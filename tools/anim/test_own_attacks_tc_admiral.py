"""Admiral Vael's own attacks (art/anim/tc_admiral/build_own_attacks.py): the gameplay values they must keep, the guard at
both ends, and the shipped clips equal clips().

The fixed values (length, loop, impact marker, first and last key of every bone) are those of the family attacks the own
moves replaced, read from the exported file at 8e1cd793 into own_attacks_tc_admiral_fixed.json."""
import json
import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
EXPORTED = os.path.join(REPO, "src/main/resources/assets/gravesandglory/animations/troop/tc_admiral.animation.json")
FIXED = json.load(open(os.path.join(HERE, "own_attacks_tc_admiral_fixed.json"), encoding="utf-8"))["clips"]


def _vec(v):
    return v.get("vector", v) if isinstance(v, dict) else v


def ends(channel):
    if isinstance(channel, dict) and "vector" not in channel:
        ts = sorted(channel, key=float)
        return _vec(channel[ts[0]]), _vec(channel[ts[-1]])
    return _vec(channel), _vec(channel)


def gap(a, b, rotation):
    d = a - b
    return (d + 180.0) % 360.0 - 180.0 if rotation else d


class OwnAttacks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.shipped = json.load(open(EXPORTED, encoding="utf-8"))["animations"]

    def test_length_loop_and_impact_are_the_fixed_values(self):
        for name, fx in FIXED.items():
            clip = self.shipped[name]
            self.assertAlmostEqual(clip["animation_length"], fx["animation_length"], places=4, msg=name)
            self.assertEqual(clip.get("loop"), fx["loop"], name)
            self.assertEqual(clip.get("timeline"), fx["timeline"], name)

    def test_guard_at_both_ends(self):
        for name, fx in FIXED.items():
            bones = self.shipped[name]["bones"]
            for key, want in fx["first"].items():
                bone, ch = key.rsplit(".", 1)
                self.assertIn(bone, bones, "%s: %s not keyed" % (name, bone))
                self.assertIn(ch, bones[bone], "%s: %s not keyed" % (name, key))
                a, b = ends(bones[bone][ch])
                for end, got in (("first", a), ("last", b)):
                    want_end = fx[end][key]
                    for x, y in zip(got, want_end):
                        self.assertAlmostEqual(gap(x, y, ch == "rotation"), 0.0, delta=0.05,
                                               msg="%s %s %s key" % (name, key, end))
            for bone, chans in bones.items():     # bones the family clip did not key: same pose at both ends
                for ch, v in chans.items():
                    if bone + "." + ch in fx["first"]:
                        continue
                    a, b = ends(v)
                    for x, y in zip(a, b):
                        self.assertAlmostEqual(gap(x, y, ch == "rotation"), 0.0, delta=0.25, msg="%s %s.%s" % (name, bone, ch))

    def test_shipped_clips_equal_clips(self):
        # in its own process: the spear builder imports the knight's build_anims for this troop, and another test may
        # already hold the knight's copy in sys.modules
        with tempfile.TemporaryDirectory() as tmp:
            out = os.path.join(tmp, "own.json")
            code = ("import json, sys; sys.path.insert(0, %r); import build_own_attacks as own; "
                    "json.dump(own.clips(), open(%r, 'w', encoding='utf-8'))"
                    % (os.path.join(REPO, "art", "anim", "tc_admiral"), out))
            env = dict(os.environ, GG_SPEAR_TROOP="tc_admiral")
            env.pop("GG_ANIM_TROOP", None)
            subprocess.run([sys.executable, "-c", code], cwd=REPO, env=env, check=True,
                           stdout=subprocess.DEVNULL)
            built = json.load(open(out, encoding="utf-8"))
        self.assertEqual(sorted(built), sorted(FIXED))
        for name, clip in built.items():
            self.assertEqual(json.loads(json.dumps(clip)), self.shipped[name], name + ": rebuild and --deploy the admiral")


if __name__ == "__main__":
    unittest.main()
