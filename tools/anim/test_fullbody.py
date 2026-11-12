"""The full-body kit on the knight's overhead demo: guard at both ends, impact marker, and a refusal when out of reach."""
import copy
import math
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(REPO, "art", "anim", "common"))
sys.path.insert(0, os.path.join(REPO, "art", "anim", "knight"))
import fullbody  # noqa: E402
import build_overhead_demo as demo  # noqa: E402


def ends(clip, bone, channel="rotation"):
    keys = clip["bones"][bone][channel]
    ts = sorted(keys, key=float)
    return keys[ts[0]], keys[ts[-1]]


class Kit(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.clips = fullbody.clips("attack_overhead", demo.OVERHEAD, report=False)

    def test_two_clips_with_the_design_timing(self):
        self.assertEqual(sorted(self.clips), ["attack_overhead", "attack_overhead_legs"])
        up = self.clips["attack_overhead"]
        self.assertAlmostEqual(up["animation_length"], fullbody.B.ATTACK_TICKS / fullbody.B.TPS, places=3)
        self.assertEqual(list(up["timeline"].values()), ["impact"])
        self.assertAlmostEqual(float(list(up["timeline"])[0]) * fullbody.B.TPS, fullbody.B.IMPACT["overhead"], places=3)

    def test_starts_and_ends_on_the_same_pose(self):
        for name, clip in self.clips.items():
            for bone, ch in clip["bones"].items():
                for channel in ("rotation", "position"):
                    if isinstance(ch.get(channel), dict):
                        a, b = ends(clip, bone, channel)
                        for x, y in zip(a, b):
                            # whole turns apart is the same pose: the game takes the nearest triple when it blends
                            # (AttackController.near)
                            gap = (x - y + 180.0) % 360.0 - 180.0 if channel == "rotation" else x - y
                            self.assertAlmostEqual(gap, 0.0, delta=0.25, msg="%s %s %s" % (name, bone, channel))

    def test_matches_the_exported_file(self):
        import json
        path = os.path.join(REPO, "src/main/resources/assets/gravesandglory/animations/troop/knight.animation.json")
        shipped = json.load(open(path, encoding="utf-8"))["animations"]
        for name, clip in self.clips.items():
            self.assertEqual(json.loads(json.dumps(clip)), shipped[name], name + ": run build_overhead_demo.py")

    def test_a_hand_out_of_reach_is_refused_with_its_tick(self):
        spec = copy.deepcopy(demo.OVERHEAD)
        spec["reach"] = [(t, (0.0, 0.0, 14.0)) if t == 10 else (t, r) for t, r in spec["reach"]]
        with self.assertRaises(Exception) as cm:
            fullbody.solve(spec)
        self.assertIn("tick", str(cm.exception))


def swing(clip, bone):
    """Largest spread of any rotation axis over the bone's keys, degrees."""
    keys = list(clip["bones"][bone]["rotation"].values())
    return max(max(k[i] for k in keys) - min(k[i] for k in keys) for i in range(3))


class WholeUnit(unittest.TestCase):
    """Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor that way it feels alive"."""

    @classmethod
    def setUpClass(cls):
        cls.clips = fullbody.clips("attack_overhead", demo.OVERHEAD, report=False)

    def test_soft_parts_are_found_on_the_knight(self):
        parts = fullbody.soft_parts_for()
        self.assertTrue({"cape", "left_skirt", "right_skirt"} <= set(parts), sorted(parts))
        self.assertNotIn("crest", parts)        # "remove the wobble of the red strip on his head" (Kort, round 3)
        self.assertEqual(parts["cape"]["period"], 7.0)                       # SOFT_TUNED wins over the measured value
        self.assertIn("left_skirt_plate", parts["left_skirt"]["bones"])     # same hinge: one piece
        self.assertEqual(parts["left_skirt"].get("clip"), "legs")
        self.assertNotIn("left_pauldron", parts)                             # a plate, not a spring

    def test_soft_parts_are_found_on_another_troop(self):
        geo = os.path.join(REPO, "src/main/resources/assets/gravesandglory/geo/troop/wm_tusk.geo.json")
        parts = fullbody.soft_parts_for(model=fullbody.rg.load_geo(geo), troop="wm_tusk")
        moved = {b for c in parts.values() for b in c["bones"]}
        self.assertTrue({"kilt", "trophy", "tooth_necklace", "hip_skull"} <= moved, sorted(moved))
        for c in parts.values():
            self.assertGreater(c["period"], 3.0)

    def test_shoulder_plates_follow_part_of_the_arm(self):
        self.assertEqual(fullbody.plates_for(), {"right_pauldron": ("right_arm", fullbody.PLATE_FOLLOW),
                                                 "left_pauldron": ("left_arm", fullbody.PLATE_FOLLOW)})

    def test_every_piece_moves_in_the_demo(self):
        up, legs = self.clips["attack_overhead"], self.clips["attack_overhead_legs"]
        for bone in ("cape", "right_pauldron", "left_pauldron"):
            self.assertGreaterEqual(swing(up, bone), 4.0, bone)
        self.assertNotIn("crest", up["bones"])          # the crest rides the helm
        for bone in ("right_skirt", "left_skirt"):
            self.assertGreaterEqual(swing(legs, bone), 4.0, bone)

    def test_cape_never_swings_into_the_back(self):
        keys = self.clips["attack_overhead"]["bones"]["cape"]["rotation"]
        rest = keys[min(keys, key=float)][0]
        self.assertGreaterEqual(min(k[0] for k in keys.values()), rest - 0.01)   # lim x = (0, 34): outward only

    def test_a_piece_sways_while_the_body_holds_still(self):
        """Reviews of 2026-11-11: quiver, skirts and trophies "pinned" through a full draw and a held lunge."""
        still = fullbody.solve(demo.OVERHEAD)
        up0, legs0 = dict(still[0][0]), dict(still[1][0])
        n = 12 * fullbody.SUB
        up, legs = fullbody.soft_motion([dict(up0) for _ in range(n)], [dict(legs0) for _ in range(n)])
        for bone, src in (("cape", up), ("left_skirt", legs)):
            xs = [src[i][bone][0] for i in range(n)]
            spread = max(max(x[k] for x in xs) - min(x[k] for x in xs) for k in range(3))
            self.assertGreater(spread, 0.4, bone)       # alive
            self.assertLess(spread, 6.0, bone)          # a breath, not a wind
        self.assertEqual(up[0]["cape"][0], up[-1]["cape"][0])      # and it starts and ends where the stance has it

    def test_two_pieces_never_move_as_a_pair(self):
        geo = os.path.join(REPO, "src/main/resources/assets/gravesandglory/geo/troop/wm_tusk.geo.json")
        parts = fullbody.soft_parts_for(model=fullbody.rg.load_geo(geo), troop="wm_tusk")
        self.assertNotEqual(parts["hip_skull"]["period"], parts["tooth_necklace"]["period"])
        self.assertNotEqual(fullbody._name_frac("hip_skull", "phase"), fullbody._name_frac("tooth_necklace", "phase"))
        self.assertEqual(fullbody._name_frac("cape"), fullbody._name_frac("cape"))       # the same on every build

    def test_a_shoulder_plate_stays_on_its_arm(self):
        """Kort, round 2 tick 10 (the plate hung 70 degrees behind a raised arm): "why is wrist/arm above shoulder?"."""
        keys = self.clips["attack_overhead"]["bones"]["right_pauldron"]["rotation"]
        arm = self.clips["attack_overhead"]["bones"]["right_arm"]["rotation"]
        self.assertGreater(swing(self.clips["attack_overhead"], "right_arm"), 60.0)      # the arm really swings
        lag = max(math.degrees(math.acos(max(-1.0, min(1.0, (fullbody.np.trace(fullbody._rot_of(v)) - 1.0) / 2.0))))
                  for v in keys.values())
        self.assertLessEqual(lag, fullbody.PLATE_LAG + 0.05)
        self.assertGreater(lag, 4.0)                    # and it still moves against the arm
        self.assertTrue(arm)

    def test_a_shoulder_plate_gives_way_to_the_helm(self):
        """Reviewers of rounds 1 and 2 saw the pauldron ride the arm onto the helm's edge: it slides down its own arm."""
        keys = self.clips["attack_overhead"]["bones"]["right_pauldron"]
        self.assertIn("position", keys)
        slide = {float(t): math.sqrt(sum(v * v for v in p)) for t, p in keys["position"].items()}
        self.assertGreater(max(slide.values()), 0.5)
        self.assertLessEqual(max(slide.values()), fullbody.PLATE_SLIDE + 1e-3)
        for p in keys["position"].values():             # along the arm only: the plate never leaves the limb sideways
            self.assertLess(math.hypot(p[0], p[2]), 0.05 + 0.02 * abs(p[1]), p)
        self.assertLess(slide[min(slide)], 1e-3)        # nothing at the first key
        self.assertLess(slide[max(slide)], 1e-3)        # nor at the last

    def test_sink_measures_one_cube_inside_another(self):
        def box(lo, hi):
            x0, y0, z0 = lo
            x1, y1, z1 = hi      # render_geo's corner order: BLB, BRB, TLB, TRB, TLF, TRF, BLF, BRF
            return fullbody.np.array([[x0, y0, z0], [x0, y0, z1], [x0, y1, z0], [x0, y1, z1],
                                      [x1, y1, z0], [x1, y1, z1], [x1, y0, z0], [x1, y0, z1]], dtype=float) / 16.0
        a = box((0, 0, 0), (4, 4, 4))
        self.assertAlmostEqual(fullbody._sink([a], [box((3, 0, 0), (7, 4, 4))]), 1.0, places=5)
        self.assertEqual(fullbody._sink([a], [box((5, 0, 0), (9, 4, 4))]), 0.0)


if __name__ == "__main__":
    unittest.main()
