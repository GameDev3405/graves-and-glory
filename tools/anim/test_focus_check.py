"""Focus check on the shipped knight: the approved clips pass, and each of Kort's four complaints is caught when it is
put into a clip on purpose."""
import copy
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import focus_check as fc  # noqa: E402
import render_geo as rg  # noqa: E402
import scorecard as sc  # noqa: E402

REPO = sc.REPO
GEO = "%s/geo/troop/knight.geo.json" % sc.ASSETS
ANIM = sc._read(os.path.join(REPO, "%s/animations/troop/knight.animation.json" % sc.ASSETS))
RIG = sc.Rig(rg.load_geo(os.path.join(REPO, GEO)), sc.rig_rules_for(REPO, GEO), "knight")
DESIGN = sc.Design(REPO)
CLIP, LEGS = "attack_overhead", "attack_overhead_legs"


def run(data, name=CLIP, base=(LEGS,)):
    res = fc.check_clip(RIG, data, name, base, variant=DESIGN.variant("knight", name))
    return {r["check"]: r for r in res["results"]}


def mutate(bone, channel, fn, clip=CLIP):
    """A copy of the knight's animations with fn(seconds, [x, y, z]) -> [x, y, z] applied to one channel's keys."""
    data = copy.deepcopy(ANIM)
    keys = data["animations"][clip]["bones"][bone][channel]
    for k in list(keys):
        keys[k] = fn(float(k), list(keys[k]))
    return data


class Shipped(unittest.TestCase):
    def test_every_knight_clip_is_clean_or_exempt(self):
        clips = fc.check_troop(REPO, "knight")
        self.assertGreaterEqual(len(clips), 40)
        bad = [(c["name"], r["check"]) for c in clips for r in c["results"] if r["status"] == "FAIL"]
        self.assertEqual(bad, [])

    def test_exemptions_name_real_clips_and_checks(self):
        for (troop, clip), checks in fc.load_exemptions().items():
            self.assertIn(clip, ANIM["animations"] if troop == "knight" else [clip])
            for c in checks:
                self.assertIn(c, fc.LIMITS)

    def test_a_check_that_does_not_apply_is_na_not_pass(self):
        r = run(ANIM, "idle_upper", ("idle_legs",))
        self.assertEqual(r["edge_lead"]["status"], "n/a")
        self.assertEqual(r["phases"]["status"], "n/a")


class Caught(unittest.TestCase):
    def fails(self, data, check, **kw):
        r = run(data, **kw)[check]
        self.assertEqual(r["status"], "FAIL", r)
        return r

    def test_base_clip_passes(self):
        bad = [k for k, r in run(ANIM).items() if r["status"] == "FAIL"]
        self.assertEqual(bad, [])

    def test_blade_turned_flat_in_the_cut(self):                    # FACE: "the blade turned flat against the target"
        hand = RIG.full.bones[RIG.blade_bone].parent
        data = mutate(hand, "rotation", lambda t, v: [v[0], v[1] + (90 if 0.38 < t < 0.52 else 0), v[2]])
        r = run(data)
        self.assertTrue(r["edge_lead"]["status"] == "FAIL" or r["roll_spin"]["status"] == "FAIL", (r["edge_lead"],
                                                                                                 r["roll_spin"]))

    def test_frozen_clip_is_stale(self):                            # STALE: nothing moves
        data = copy.deepcopy(ANIM)
        for name in (CLIP, LEGS):
            for bone in data["animations"][name]["bones"].values():
                for ch in ("rotation", "position"):
                    if isinstance(bone.get(ch), dict):
                        first = bone[ch][min(bone[ch], key=float)]
                        bone[ch] = {k: first for k in bone[ch]}
        r = run(data)
        self.assertEqual(r["whole_body"]["status"], "FAIL", r["whole_body"])
        self.assertEqual(r["phases"]["status"], "FAIL", r["phases"])

    def test_arms_only_swing_is_not_whole_body(self):               # STALE: legs and hips stand still under the cut
        data = copy.deepcopy(ANIM)
        for bone in data["animations"][LEGS]["bones"].values():
            for ch in ("rotation", "position"):
                if isinstance(bone.get(ch), dict):
                    first = bone[ch][min(bone[ch], key=float)]
                    bone[ch] = {k: first for k in bone[ch]}
        self.fails(data, "whole_body")

    def test_knee_bent_backward(self):                              # LIMB
        data = mutate("left_shin", "rotation", lambda t, v: [-25.0, v[1], v[2]], clip=LEGS)
        self.fails(data, "knee")

    def test_one_frame_pop(self):                                   # LIMB: a glitch frame
        keys = sorted(float(k) for k in ANIM["animations"][CLIP]["bones"]["right_arm"]["rotation"])
        at = keys[len(keys) // 3]
        data = mutate("right_arm", "rotation", lambda t, v: [v[0] + (40 if t == at else 0), v[1], v[2]])
        r = run(data)
        self.assertTrue(r["pop"]["status"] == "FAIL" or r["jitter"]["status"] == "FAIL", (r["pop"], r["jitter"]))

    def test_sword_arm_swung_across_the_body(self):                 # CLIP
        data = mutate("right_arm", "rotation", lambda t, v: [v[0], v[1], v[2] + 60.0])
        self.fails(data, "weapon_in_body")
        self.fails(data, "limb_in_body")

    def test_blade_folded_into_the_body(self):                      # CLIP
        hand = RIG.full.bones[RIG.blade_bone].parent
        data = mutate(hand, "rotation", lambda t, v: [v[0] - 90.0, v[1], v[2]])
        self.fails(data, "weapon_in_body")

    def test_clip_that_does_not_come_home(self):                    # LIMB: snaps when the layer blends out
        last = max(float(k) for k in ANIM["animations"][CLIP]["bones"]["torso"]["rotation"])
        data = mutate("torso", "rotation", lambda t, v: [v[0] + (20 if t == last else 0), v[1], v[2]])
        self.fails(data, "settle")


if __name__ == "__main__":
    unittest.main()
