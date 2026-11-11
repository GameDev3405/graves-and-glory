"""The full-body kit on the knight's overhead demo: guard at both ends, impact marker, and a refusal when out of reach."""
import copy
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


if __name__ == "__main__":
    unittest.main()
