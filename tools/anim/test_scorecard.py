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


def status(bones, metric, name="c", kind=None, length=1.0, loop=False, variant=None, family=None, speed=None,
           limits=None):
    meas = sc.measure(RIG, anim(bones, length, loop, name), name, kind=kind, variant=variant, family=family,
                      speed=speed)
    row = next(m for m in sc.judge(meas, limits or LIMITS) if m["id"] == metric)
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

    def test_hyperextension_cap_holds_for_every_family(self):
        bent_back = {"right_forearm": rot({0: [10, 0, 0]})}          # the elbow bent 10 deg the wrong way
        for fam in (None, "bow", "spear", "crossbow"):
            s, row = status(bent_back, "elbow_bend", kind="stance", family=fam)
            self.assertEqual(s, "fail", (fam, row))
            self.assertGreaterEqual(row["limit"][0], sc.HYPER)
        lim = {"metrics": {"elbow_bend": {"limit": {"stance": [-3.0, 60.0]}, "floor_lo": sc.HYPER}},
               "families": {"bow": {"elbow_bend": {"stance": [-40.0, 140.0], "reason": "test"}}}}
        self.assertEqual(sc.limit_for(lim, "elbow_bend", "stance", "bow"), [sc.HYPER, 140.0])
        self.assertEqual(sc.limit_for(lim, "elbow_bend", "stance", "sword"), [-3.0, 60.0])

    def test_family_allowance_only_widens(self):
        lim = {"metrics": {"ang_speed_arms": {"limit": {"attack": 100.0}}},
               "families": {"bow": {"ang_speed_arms": {"attack": 150.0, "reason": "draw"}},
                            "axe": {"ang_speed_arms": {"all": 50.0, "reason": "test"}}}}
        self.assertEqual(sc.limit_for(lim, "ang_speed_arms", "attack", "bow"), 150.0)
        self.assertEqual(sc.limit_for(lim, "ang_speed_arms", "attack", "axe"), 100.0)
        for fam, why in LIMITS["families"].items():
            for metric, per in why.items():
                self.assertTrue(per.get("reason"), (fam, metric))

    def test_knee_range(self):
        good = {"right_shin": rot({0: [50, 0, 0]}), "left_shin": rot({0: [50, 0, 0]})}
        self.check(good, {"right_shin": rot({0: [-30, 0, 0]}), "left_shin": rot({0: [50, 0, 0]})}, "knee_bend",
                   kind="attack")

    def test_angular_speed_per_group(self):
        for bone, group in (("right_arm", "arms"), ("right_leg", "legs"), ("torso", "torso")):
            good = {bone: rot({0: [0, 0, 0], 1.0: [2, 0, 0]})}
            bad = {bone: rot({0: [0, 0, 0], 0.1: [-170, 0, 0], 1.0: [-170, 0, 0]})}
            self.check(good, bad, "ang_speed_" + group, kind="stance")
        # the floor: a stance shift of 6 deg in 2 ticks is no fault, though the still knight never moves that fast
        s, row = status({"right_leg": rot({0: [0, 0, 0], 0.1: [6, 0, 0], 1.0: [6, 0, 0]})}, "ang_speed_legs",
                        kind="stance")
        self.assertEqual(s, "pass", row)
        self.assertGreaterEqual(row["limit"], sc.SPEED_FLOOR)

    def test_weapon_speed_is_world_rotation(self):
        good = {"right_arm": rot({0: [0, 0, 0], 1.0: [-10, 0, 0]})}
        bad = {"right_arm": rot({0: [0, 0, 0], 0.1: [-160, 0, 0], 1.0: [-160, 0, 0]})}
        self.check(good, bad, "ang_speed_weapon", kind="stance")

    def test_foot_slide(self):
        good = {"right_foot": {"position": {"0": [0, 0, 0], "1.0": [0, 0, 0]}}}
        bad = {"right_foot": {"position": {"0": [0, 0, 0], "1.0": [0, 0, 8]}}}
        self.check(good, bad, "foot_slide", kind="stance")
        # a gait's root speed comes from the troop's booth config; without one the slide cannot be judged
        s, row = status(good, "foot_slide", name="walk_upper")
        self.assertEqual(s, "n/a", row)
        self.assertIn("booth", row["note"])
        s, row = status(good, "foot_slide", name="walk_upper", speed=0.16)
        self.assertNotEqual(s, "n/a", row)

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
        thrust = lambda a, b: {"right_arm": {"position": {"0": [0, 0, 0], str(a): [0, 0, 0], str(b): [0, 0, -6],  # noqa: E731
                                                          "1.0": [0, 0, -6]}}}      # gecko -z: toward the foe
        good, bad = thrust(0.2, 0.3), thrust(0.7, 0.8)
        self.check(good, bad, "impact_tick", name="attack_x", variant=v)
        _, row = status(good, "impact_tick", name="attack_x", variant=v)
        self.assertEqual((row["value"], row["design"]), (5.0, 5.0))
        # a faster, longer wind-up (the hand drawn back, away from the foe) is not the contact
        windup = {"right_arm": {"position": {"0": [0, 0, 0], "0.05": [0, 0, 10], "0.2": [0, 0, 10], "0.3": [0, 0, 4],
                                             "1.0": [0, 0, 4]}}}
        s, row = status(windup, "impact_tick", name="attack_x", variant=v)
        self.assertEqual((s, row["value"]), ("pass", 5.0), row)

    def test_loop_continuity(self):
        good = {"right_arm": rot({0: [0, 0, 0], 0.5: [-2, 0, 0], 1.0: [0, 0, 0]})}
        pop = {"right_arm": rot({0: [0, 0, 0], 1.0: [-30, 0, 0]})}
        kink = {"right_arm": rot({0: [0, 0, 0], 0.1: [-10, 0, 0], 0.9: [-10, 0, 0], 1.0: [0, 0, 0]})}
        self.check(good, pop, "loop_pose", kind="stance", loop=True)
        self.check(good, kink, "loop_vel", kind="stance", loop=True)
        # a breathing loop passes; a quarter-tick snap to another pose at the wrap (ic_breaker's idle: 20 deg) fails,
        # a 2 deg one is under the floor
        breathe = {"right_arm": rot({0: [0, 0, 0], 1.0: [-3, 0, 0], 2.0: [0, 0, 0]}),
                   "torso": rot({0: [0, 0, 0], 1.0: [2, 0, 0], 2.0: [0, 0, 0]})}
        self.assertEqual(status(breathe, "loop_vel", kind="stance", loop=True, length=2.0)[0], "pass")
        for snap, want in ((20, "fail"), (2, "pass")):
            blip = {"right_arm": rot({0: [-snap, 0, 0], 0.0125: [0, 0, 0], 2.9875: [0, 0, 0], 3.0: [-snap, 0, 0]})}
            s, row = status(blip, "loop_vel", kind="stance", loop=True, length=3.0)
            self.assertEqual(s, want, row)
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
        for m in ("elbow_bend", "knee_bend", "ang_speed_weapon", "pen_weapon", "pen_limb", "ang_speed_arms",
                  "foot_slide"):
            self.assertEqual(rows[m]["status"], "n/a", m)
        self.assertIn("biped", rows["ang_speed_arms"]["note"])


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

    def test_entry_depth_goes_past_the_centre(self):
        import numpy as np
        box = np.array([[x, y, z] for x in (0, 8) for y in (0, 8) for z in (0, 4)], float)[None]
        I = np.eye(3)[None]
        behind = np.array([[4.0, 4.0, -3.0]])
        # in through the back face (the root is behind): 3.5 px deep, where nearest-face depth stops at 0.5
        self.assertAlmostEqual(sc.entry_depth(np.array([[4.0, 4.0, 3.5]]), behind, box, I), 3.5)
        self.assertAlmostEqual(sc.point_depth(np.array([[4.0, 4.0, 3.5]]), box, I), 0.5)
        # grazing the side while the root is behind reads only as deep as it is past the side
        self.assertAlmostEqual(sc.entry_depth(np.array([[0.25, 4.0, 3.5]]), behind, box, I), 0.25)

    def test_breaker_edge_is_the_axe_bit_side(self):
        """ic_breaker's EDGE_SIGN comes from its geo: the wide bit cubes sit on the weapon bone's -x of the haft."""
        import numpy as np
        geo = "%s/geo/troop/ic_breaker.geo.json" % sc.ASSETS
        rig = sc.Rig(rg.load_geo(os.path.join(sc.REPO, geo)), sc.rig_rules_for(sc.REPO, geo), "ic_breaker")
        cl, cb = rig.model.cube_list, rig.model.cube_bone
        own = [i for i, b in enumerate(cb) if b == rig.blade_bone]
        haft = max(own, key=lambda i: np.ptp(cl[i].corners[:, 1]))
        hx = cl[haft].corners[:, 0].mean()
        head = [cl[i].corners for i in own
                if cl[i].corners[:, 1].min() > 1.4 and abs(cl[i].corners[:, 0].mean() - hx) > 0.05]
        lean = sum(np.ptp(c[:, 0]) * np.ptp(c[:, 1]) * (c[:, 0].mean() - hx) for c in head)
        self.assertLess(lean, 0)
        self.assertEqual(sc.EDGE_SIGN["ic_breaker"], -1.0)


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
        self.assertEqual(fresh["families"], LIMITS["families"])
        self.assertEqual(json.dumps(fresh, sort_keys=True), json.dumps(sc.derive(measured=self.meas), sort_keys=True))
        self.assertNotIn("date", fresh["derived_from"])         # deterministic: no date drift


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



class RendererTroopsTest(unittest.TestCase):
    def test_reads_approved_and_preview_sets(self):
        with tempfile.TemporaryDirectory() as repo:
            src = os.path.join(repo, sc.RENDERER)
            os.makedirs(os.path.dirname(src))
            with open(src, "w", encoding="utf-8") as f:
                f.write('Set<String> APPROVED = Set.of("knight");\nSet<String> PREVIEW = Set.of("archer", "ghost");\n')
            anims = os.path.join(repo, sc.ASSETS, "animations", "troop")
            os.makedirs(anims)
            for t in ("knight", "archer"):
                open(os.path.join(anims, t + ".animation.json"), "w").close()
            self.assertEqual(sc.renderer_troops(repo), ["knight", "archer"])
            with open(src, "w", encoding="utf-8") as f:
                f.write("class Nothing {}\n")
            with self.assertRaises(sc.ScoreError):
                sc.renderer_troops(repo)


if __name__ == "__main__":
    unittest.main()
