"""Scorecard metrics on a small synthetic biped (fixtures/score_rig.geo.json) with the shipped limits, and the knight."""
import io
import contextlib
import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import render_geo as rg  # noqa: E402
import scorecard as sc  # noqa: E402

RIG = sc.Rig(rg.load_geo(os.path.join(HERE, "fixtures", "score_rig.geo.json")))
LIMITS = sc.load_limits()


def anim(bones, length=1.0, loop=False, name="c"):
    return {"animations": {name: {"animation_length": length, "loop": loop, "bones": bones}}}


def rg_json(name):
    with open(os.path.join(HERE, "fixtures", name), encoding="utf-8") as f:
        return json.load(f)


def rot(keys):
    """{seconds: [x, y, z]} -> a rotation channel."""
    return {"rotation": {str(k): v for k, v in keys.items()}}


def status(bones, metric, name="c", kind=None, length=1.0, loop=False, variant=None):
    meas = sc.measure(RIG, anim(bones, length, loop, name), name, kind=kind, variant=variant)
    row = next(m for m in sc.judge(meas, LIMITS) if m["id"] == metric)
    return row["status"], row


class MetricTest(unittest.TestCase):
    def check(self, good, bad, metric, **kw):
        s, row = status(good, metric, **kw)
        self.assertEqual(s, "pass", row)
        s, row = status(bad, metric, **kw)
        self.assertEqual(s, "fail", row)
        self.assertIsNotNone(row["limit"])

    def test_elbow_hyperextension_and_overfold(self):
        good = {"right_forearm": rot({0: [-60, 0, 0]}), "left_forearm": rot({0: [-60, 0, 0]})}
        self.check(good, {"right_forearm": rot({0: [40, 0, 0]})}, "elbow_bend", kind="attack")
        self.check(good, {"right_forearm": rot({0: [-150, 0, 0]})}, "elbow_bend", kind="attack")

    def test_knee_range(self):
        good = {"right_shin": rot({0: [50, 0, 0]}), "left_shin": rot({0: [50, 0, 0]})}
        self.check(good, {"right_shin": rot({0: [-30, 0, 0]}), "left_shin": rot({0: [50, 0, 0]})}, "knee_bend",
                   kind="attack")

    def test_angular_speed_per_group(self):
        for bone, group in (("right_arm", "arms"), ("right_leg", "legs"), ("torso", "torso")):
            good = {bone: rot({0: [0, 0, 0], 1.0: [2, 0, 0]})}
            bad = {bone: rot({0: [0, 0, 0], 0.1: [-170, 0, 0], 1.0: [-170, 0, 0]})}
            self.check(good, bad, "ang_speed_" + group, kind="stance")

    def test_weapon_speed_is_world_rotation(self):
        good = {"right_arm": rot({0: [0, 0, 0], 1.0: [-10, 0, 0]})}
        bad = {"right_arm": rot({0: [0, 0, 0], 0.1: [-160, 0, 0], 1.0: [-160, 0, 0]})}
        self.check(good, bad, "ang_speed_weapon", kind="stance")

    def test_foot_slide(self):
        good = {"right_foot": {"position": {"0": [0, 0, 0], "1.0": [0, 0, 0]}}}
        bad = {"right_foot": {"position": {"0": [0, 0, 0], "1.0": [0, 0, 8]}}}
        self.check(good, bad, "foot_slide", kind="stance")

    def test_weapon_penetration(self):
        good = {"right_arm": rot({0: [0, 0, 0]})}
        bad = {"weapon_t3": {"position": {"0": [6, 6, 0]}}}     # the blade run through the chest
        self.check(good, bad, "pen_weapon", kind="stance")

    def test_limb_penetration(self):
        good = {"right_forearm": rot({0: [-60, 0, 0]})}
        bad = {"right_forearm": {"position": {"0": [5, 4, 0]}}}
        self.check(good, bad, "pen_limb", kind="stance")

    def test_edge_lead(self):
        v = {"kind": "slash", "impactTicks": 5}
        # a sideways arc moves the tip across the blade along the edge (local x); a forward arc moves it flat
        good = {"right_arm": rot({0: [-90, 0, 0], 0.2: [-90, 0, 0], 0.3: [-90, 0, 60], 1.0: [-90, 0, 60]})}
        bad = {"right_arm": rot({0: [0, 0, 0], 0.2: [0, 0, 0], 0.3: [-60, 0, 0], 1.0: [-60, 0, 0]})}
        self.check(good, bad, "edge_lead", name="attack_x", variant=v)

    def test_impact_tick(self):
        v = {"kind": "stab", "impactTicks": 5}
        good = {"right_arm": rot({0: [0, 0, 0], 0.2: [0, 0, 0], 0.3: [-90, 0, 0], 1.0: [-90, 0, 0]})}
        bad = {"right_arm": rot({0: [0, 0, 0], 0.7: [0, 0, 0], 0.8: [-90, 0, 0], 1.0: [-90, 0, 0]})}
        self.check(good, bad, "impact_tick", name="attack_x", variant=v)
        _, row = status(good, "impact_tick", name="attack_x", variant=v)
        self.assertEqual((row["value"], row["design"]), (5.0, 5.0))

    def test_loop_continuity(self):
        good = {"right_arm": rot({0: [0, 0, 0], 0.5: [-2, 0, 0], 1.0: [0, 0, 0]})}
        pop = {"right_arm": rot({0: [0, 0, 0], 1.0: [-30, 0, 0]})}
        kink = {"right_arm": rot({0: [0, 0, 0], 0.1: [-10, 0, 0], 0.9: [-10, 0, 0], 1.0: [0, 0, 0]})}
        self.check(good, pop, "loop_pose", kind="stance", loop=True)
        self.check(good, kink, "loop_vel", kind="stance", loop=True)
        pos_pop = {"right_arm": {"position": {"0": [0, 0, 0], "1.0": [0, 3, 0]}}}
        self.check(good, pos_pop, "loop_pos", kind="stance", loop=True)

    def test_not_applicable_is_not_pass(self):
        meas = sc.measure(RIG, anim({"right_arm": rot({0: [0, 0, 0]})}, name="stance_x"), "stance_x")
        rows = {m["id"]: m for m in sc.judge(meas, LIMITS)}
        for m in ("edge_lead", "impact_tick", "loop_pose", "loop_vel"):
            self.assertEqual(rows[m]["status"], "n/a", m)
            self.assertTrue(rows[m]["note"])
        plain = sc.Rig(rg.load_geo(os.path.join(HERE, "fixtures", "rig.geo.json")))
        meas = sc.measure(plain, anim({"arm_left": rot({0: [0, 0, 0]})}), "c")
        rows = {m["id"]: m for m in sc.judge(meas, LIMITS)}
        for m in ("elbow_bend", "knee_bend", "ang_speed_weapon", "pen_weapon", "pen_limb"):
            self.assertEqual(rows[m]["status"], "n/a", m)


class NamingTest(unittest.TestCase):
    def test_kinds(self):
        self.assertEqual([sc.clip_kind(n) for n in ("attack_thrust", "walk_upper", "idle_legs", "stance_wall_up_upper",
                                                     "hit_block", "stun_out_upper", "death_left_drop1", "howl",
                                                     "stance_fallback_upper")],
                         ["attack", "gait", "stance", "stance", "hit", "hit", "death", "other", "fallback"])

    def test_units_pair_legs(self):
        names = ["walk_legs", "walk_upper", "stance_brace_legs", "stance_brace_up_upper", "stance_ready_legs",
                 "stance_ward_upper", "run_legs", "charge_run_upper", "attack_thrust", "attack_thrust_legs", "odd_legs"]
        u = {n: b for n, b, _ in sc.units(names)}
        self.assertEqual(u, {"walk_upper": ["walk_legs"], "stance_brace_up_upper": ["stance_brace_legs"],
                             "stance_ward_upper": ["stance_ready_legs"], "charge_run_upper": ["run_legs"],
                             "attack_thrust": ["attack_thrust_legs"], "odd_legs": []})

    def test_point_depth(self):
        import numpy as np
        box = np.array([[x, y, z] for x in (0, 4) for y in (0, 4) for z in (0, 4)], float)[None]
        I = np.eye(3)[None]
        self.assertAlmostEqual(sc.point_depth(np.array([[2.0, 2.0, 1.5], [9.0, 9.0, 9.0]]), box, I), 1.5)
        self.assertEqual(sc.point_depth(np.array([[4.0, 2.0, 2.0]]), box, I), 0.0)


class KnightTest(unittest.TestCase):
    """The approved knight passes every check with the shipped limits, and the shipped limits are its derivation."""

    @classmethod
    def setUpClass(cls):
        cls.meas = sc.measure_troop(sc.REPO, sc.KNIGHT)

    def test_knight_passes(self):
        scored = [sc.score(m, LIMITS) for m in self.meas]
        self.assertEqual(sc.failures(scored), [])
        self.assertGreaterEqual(len(scored), 40)
        applied = {m["id"] for c in scored for m in c["metrics"] if m["status"] == "pass"}
        self.assertEqual(applied, set(sc.METRICS))      # every metric is exercised by some knight clip

    def test_shipped_limits_match_derivation(self):
        fresh = sc.derive(measured=self.meas)
        self.assertEqual(fresh["metrics"], LIMITS["metrics"])


class CliTest(unittest.TestCase):
    def run_cli(self, bones):
        with tempfile.TemporaryDirectory() as tmp:
            for rel, data in (("geo/troop/rigx.geo.json", rg_json("score_rig.geo.json")),
                              ("animations/troop/rigx.animation.json", anim(bones, name="pose_x"))):
                path = os.path.join(tmp, *sc.ASSETS.split("/"), *rel.split("/"))
                os.makedirs(os.path.dirname(path))
                with open(path, "w", encoding="utf-8") as f:
                    json.dump(data, f)
            out, err = io.StringIO(), io.StringIO()
            res = os.path.join(tmp, "out", "r.json")
            with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
                code = sc.main(["--repo", tmp, "--troop", "rigx", "--json", res])
            with open(res, encoding="utf-8") as f:
                return code, out.getvalue(), json.load(f)

    def test_exit_status_and_json(self):
        code, out, res = self.run_cli({"right_arm": rot({0: [0, 0, 0], 1.0: [-2, 0, 0]})})
        self.assertEqual(code, 0, out)
        self.assertTrue(res["troops"]["rigx"][0]["pass"])
        code, out, res = self.run_cli({"right_arm": rot({0: [0, 0, 0], 0.1: [-170, 0, 0], 1.0: [-170, 0, 0]})})
        self.assertEqual(code, 1)
        self.assertIn("FAIL pose_x: ang_speed_arms", out)
        self.assertFalse(res["troops"]["rigx"][0]["pass"])


if __name__ == "__main__":
    unittest.main()
