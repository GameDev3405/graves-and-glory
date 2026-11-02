import contextlib
import io
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'tools'))
import mode_gate  # noqa: E402

HEADER = ("match,map,dawnTier,duskTier,winner,dawnScore,duskScore,seconds,banner0,banner1,dawnKills,duskKills,"
          "dawnWiped,duskWiped,dawnRetreats,duskRetreats,fights,meanFightSeconds,dawnFlankHits,duskFlankHits,"
          "dawnFlankShots,duskFlankShots,score300_0,score300_1,mode,stage,gateBlows,gateDamage,yardSecond,"
          "gateSecond,keepSecond")


def row(i, winner="dawn", stage=3, blows=5, mode="siege"):
    pre = [str(i), "m", "1", "1", winner] + ["0"] * 19
    return ",".join(pre + [mode, str(stage), str(blows), "10", "", "", ""])


def run(rows, mode="siege", header=HEADER):
    with tempfile.TemporaryDirectory() as d:
        p = os.path.join(d, "results.csv")
        with open(p, "w", newline="") as f:
            f.write("\n".join([header] + rows) + "\n")
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = mode_gate.main(["mode_gate.py", p, mode])
        return code, out.getvalue(), err.getvalue()


def ten(dawn_wins, **kw):
    return [row(i, "dawn" if i < dawn_wins else "dusk", **kw) for i in range(10)]


class ModeGateTest(unittest.TestCase):
    def test_siege_pass(self):
        code, out, _ = run(ten(5))
        self.assertEqual(code, 0)
        self.assertTrue(out.startswith("mode_gate siege matches=10 dawn=50% pass\n"))
        self.assertIn("stages 0=0 1=0 2=0 3=10", out)

    def test_siege_untouched_gate(self):
        rows = ten(5)
        rows[0] = row(0, "dawn", stage=2, blows=0)
        code, out, _ = run(rows)
        self.assertEqual(code, 1)
        self.assertIn("gateBlows=0", out)

    def test_siege_win_band(self):
        for wins, expect in ((2, 1), (3, 0), (7, 0), (8, 1)):
            self.assertEqual(run(ten(wins))[0], expect, wins)

    def test_aborted_left_out(self):
        rows = ten(5) + [row(10, "aborted", stage=0, blows=0), row(11, "aborted", stage=0, blows=0)]
        code, out, _ = run(rows)
        self.assertEqual(code, 0)
        self.assertTrue(out.startswith("mode_gate siege matches=10 dawn=50% pass aborted=2\n"))

    def test_stuck_in_yard(self):
        rows = ten(5)
        rows[0] = row(0, "dusk", stage=0, blows=0)
        code, out, _ = run(rows)
        self.assertEqual(code, 1)
        self.assertIn("never got past the yard", out)
        self.assertNotIn("gateBlows=0", out)

    def test_other_mode_rows_skipped(self):
        code, out, _ = run(ten(5) + [row(10, "dusk", mode="duel")])
        self.assertEqual(code, 0)
        self.assertIn("matches=10", out)

    def test_bad_input(self):
        self.assertEqual(mode_gate.main(["x", os.path.join(tempfile.gettempdir(), "nope_missing.csv"), "siege"]), 2)
        old_header = HEADER.split(",mode,")[0]
        self.assertEqual(run(["1,m,1,1,dawn"], header=old_header)[0], 2)
        self.assertEqual(run(ten(5), mode="bogus")[0], 2)
        self.assertEqual(run([row(0, "aborted")])[0], 2)


if __name__ == "__main__":
    unittest.main()
