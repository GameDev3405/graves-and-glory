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


def rig_of(troop):
    geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, troop)
    return sc.Rig(rg.load_geo(os.path.join(REPO, geo)), sc.rig_rules_for(REPO, geo), troop)


def freeze(data, clips=(CLIP, LEGS)):
    """Every channel of the clips held on its first key."""
    for name in clips:
        for bone in data["animations"][name]["bones"].values():
            for ch in ("rotation", "position"):
                if isinstance(bone.get(ch), dict):
                    first = bone[ch][min(bone[ch], key=float)]
                    bone[ch] = {k: first for k in bone[ch]}
    return data


class Graded(unittest.TestCase):
    """Kort's grades of knight-attack_overhead-r1 (2026-11-11, score 7), each as a mutation the checks must catch, and
    the rules added for a two-handed troop (the Warmaw tusk)."""

    def test_weapon_spun_in_the_hand_in_the_wind_up(self):          # tick 5: "weapons spins around in hand"
        hand = RIG.full.bones[RIG.blade_bone].parent
        data = mutate(hand, "rotation", lambda t, v: [v[0], v[1], v[2] + (70.0 if 0.2 < t < 0.3 else 0.0)])
        r = run(data)["grip_roll"]
        self.assertEqual(r["status"], "FAIL", r)

    def test_cape_crest_and_plates_standing_still(self):            # "i want the whole unit to be animated"
        alive = fc.alive_pieces(RIG.full)
        data = copy.deepcopy(ANIM)
        for name in (CLIP, LEGS):
            for bone in [b for b in data["animations"][name]["bones"] if b in alive]:
                del data["animations"][name]["bones"][bone]
        r = run(data)["secondary_life"]
        self.assertEqual(r["status"], "FAIL", r)
        for piece in ("cape", "crest", "left_pauldron"):
            self.assertIn(piece, r["detail"])

    def test_the_demo_moves_every_piece(self):
        r = run(ANIM)["secondary_life"]
        self.assertEqual(r["status"], "pass", r)
        self.assertGreaterEqual(r["value"], fc.LIMITS["secondary_life"][0])

    def test_arm_sunk_in_the_chest_mid_clip(self):                  # ticks 7, 8: "textures on its chest are glitching"
        data = mutate("left_arm", "rotation", lambda t, v: [v[0], v[1], v[2] + (30.0 if 0.3 < t < 0.6 else 0.0)])
        r = run(data)["armour_clip"]
        self.assertEqual(r["status"], "FAIL", r)
        self.assertNotIn("stance", r["detail"])

    def test_clipping_the_stance_already_has_is_reported_against_the_stance(self):
        data = freeze(mutate("left_arm", "rotation", lambda t, v: [v[0], v[1], v[2] + 45.0]))
        r = run(data)["armour_clip"]
        self.assertEqual(r["status"], "WARN", r)
        self.assertIn("fix the stance", r["detail"])

    def test_alive_pieces_by_troop(self):
        self.assertTrue({"cape", "crest", "left_pauldron", "right_pauldron"} <= set(fc.alive_pieces(RIG.full)))
        tusk = set(fc.alive_pieces(rig_of("wm_tusk").full))
        self.assertTrue({"kilt", "trophy", "tooth_necklace", "hip_skull", "pauldron", "left_pauldron"} <= tusk, tusk)
        for bone in ("torso", "head", "right_forearm", "weapon"):
            self.assertNotIn(bone, tusk)

    def test_limb_root(self):
        tusk = rig_of("wm_tusk").full
        self.assertEqual(fc._limb_root(tusk, "right_shin_guard"), "right_leg")
        self.assertEqual(fc._limb_root(tusk, "left_knee"), "left_leg")
        self.assertEqual(fc._limb_root(RIG.full, "right_forearm"), "right_arm")
        self.assertIsNone(fc._limb_root(RIG.full, "torso"))

    def second_grip(self, troop, clip, legs):
        rig = RIG if troop == "knight" else rig_of(troop)
        anim = ANIM if troop == "knight" else sc._read(os.path.join(REPO, "%s/animations/troop/%s.animation.json"
                                                                    % (sc.ASSETS, troop)))
        fr = rg.Scene(rig.model, None, [rg.load_clip(anim, legs), rg.load_clip(anim, clip)], 0.0).evaluate(0.0)
        P = fc.Parts(rig)
        side = "left" if P.holder.get("weapon") == "right" else "right"
        return fc._second_grip(rig, fr, P, P.idx(["weapon"]), side)

    def test_second_hand_on_a_shaft_is_a_grip(self):                # a spear in two fists is not "weapon in the arm"
        grip = self.second_grip("wm_tusk", "attack_thrust", "attack_thrust_legs")
        self.assertIsNotNone(grip)
        self.assertAlmostEqual(grip[1] * 16.0, fc.GRIP_FREE)

    def test_shield_hand_is_not_a_grip(self):
        self.assertIsNone(self.second_grip("knight", CLIP, LEGS))


class FitPairs(unittest.TestCase):
    """The row armour_clip names one pair; fit_pairs lists them all (round 2: a pauldron 1.88 px in the helm was hidden
    behind an arm 1.88 px in the torso)."""

    def test_lists_every_pair_naming_the_part(self):
        import fit_pairs
        worst, per_tick = fit_pairs.pairs("knight", CLIP, part="pauldron", t0=3.0, t1=6.0)
        self.assertTrue(worst)
        for pair, (gain, tick, depth) in worst.items():
            self.assertTrue("pauldron" in pair[0] or "pauldron" in pair[1], pair)
            self.assertTrue(3.0 <= tick <= 6.0)
            self.assertLessEqual(gain, depth + 1e-9)        # what the clip adds is never more than the depth itself
        self.assertEqual(sorted(per_tick), [3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0])
        helm = [g for pair, (g, _, _) in worst.items() if "helm_great" in pair]
        self.assertTrue(helm and max(helm) < 1.5, helm)     # the plate gives way to the helm (fullbody._plates_yield)


if __name__ == "__main__":
    unittest.main()
