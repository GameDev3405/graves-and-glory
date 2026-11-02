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
          "gateSecond,keepSecond,checkpoints,progress,idleMax")


def row(i, winner="dawn", stage=3, blows=5, mode="siege", scores=("0", "0"), checkpoints="", idle=""):
    pre = [str(i), "m", "1", "1", winner, scores[0], scores[1]] + ["0"] * 17
    return ",".join(pre + [mode, str(stage), str(blows), "10", "", "", "", checkpoints, "", idle])


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


def dom(dawn_wins, draws=0, scores=("300", "120")):
    return [row(i, "dawn" if i < dawn_wins else "draw" if i < dawn_wins + draws else "dusk", mode="domination", stage="", blows="", scores=scores)
            for i in range(10)]


def convoy(escort_wins, checkpoints="2", idle="10"):
    return [row(i, "dawn" if i < escort_wins else "dusk", mode="convoy", stage="", blows="", checkpoints=checkpoints, idle=idle)
            for i in range(10)]


class DominationGateTest(unittest.TestCase):
    def test_pass_and_band(self):
        code, out, _ = run(dom(5), mode="domination")
        self.assertEqual(code, 0)
        self.assertTrue(out.startswith("mode_gate domination matches=10 dawn=50% pass\n"))
        for wins, expect in ((2, 1), (3, 0), (7, 0), (8, 1)):
            self.assertEqual(run(dom(wins), mode="domination")[0], expect, wins)

    def test_draws_count_half(self):
        self.assertEqual(run(dom(2, draws=2), mode="domination")[0], 0)  # 2 + 1 = 3 of 10
        self.assertEqual(run(dom(0, draws=5), mode="domination")[0], 1)  # 2.5 of 10

    def test_dead_match(self):
        rows = dom(5)
        rows[0] = row(0, "dawn", mode="domination", stage="", blows="", scores=("0", "0"))
        code, out, _ = run(rows, mode="domination")
        self.assertEqual(code, 1)
        self.assertIn("both scores at 0", out)


class ConvoyGateTest(unittest.TestCase):
    def test_pass(self):
        code, out, _ = run(convoy(5), mode="convoy")
        self.assertEqual(code, 0)
        self.assertTrue(out.startswith("mode_gate convoy matches=10 dawn=50% pass\n"))
        self.assertIn("mean checkpoints 2.00", out)

    def test_escort_band(self):
        for wins, expect in ((2, 1), (3, 0), (7, 0), (8, 1)):
            self.assertEqual(run(convoy(wins), mode="convoy")[0], expect, wins)

    def test_checkpoints_and_stall(self):
        code, out, _ = run(convoy(5, checkpoints="1"), mode="convoy")
        self.assertEqual(code, 1)
        self.assertIn("mean checkpoints 1.00 is under 1.5", out)
        code, out, _ = run(convoy(5, idle="61"), mode="convoy")
        self.assertEqual(code, 1)
        self.assertIn("10 match(es) stalled the wagon over 60 s (longest 61 s)", out)
        self.assertEqual(run(convoy(5, idle="60"), mode="convoy")[0], 0)

    def test_old_results_file_is_unusable(self):
        old = HEADER.replace(",checkpoints,progress,idleMax", "")
        self.assertEqual(run(convoy(5), mode="convoy", header=old)[0], 2)


if __name__ == "__main__":
    unittest.main()
