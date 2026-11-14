"""Lord Marshal Aldric's own attacks (art/anim/gl_marshal/build_own_attacks.py): the gameplay timing is the present clips',
each clip starts and ends on the guard, and the shipped file carries exactly what clips() builds."""
import json
import os
import subprocess
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
SHIPPED = os.path.join(REPO, "src", "main", "resources", "assets", "gravesandglory", "animations", "troop",
                       "gl_marshal.animation.json")
OUT = os.path.join(REPO, "art", "anim", "gl_marshal", "out", "gl_marshal.animation.json")
# Fixed by gameplay (the present clips, 2026-11-13): 22 ticks, play once, the impact marker in seconds.
IMPACT = {"attack_thrust": "0.35", "attack_overhead": "0.5", "attack_low_cut": "0.4"}
PRESENT_AT = "c1f97231"      # the last commit with the family's attack clips (the guard they start and end on)
NAMES = sorted(n + s for n in IMPACT for s in ("", "_legs"))
# clips() in its own process: build_anims reads GG_ANIM_TROOP once, at import, and other tests import it for the knight.
BUILD = ("import json, sys; sys.path.insert(0, 'art/anim/gl_marshal'); import build_own_attacks as O; "
         "sys.stdout.write(json.dumps(O.clips()))")


def ends(channel):
    if not isinstance(channel, dict):
        return channel, channel
    ts = sorted(channel, key=float)
    return channel[ts[0]], channel[ts[-1]]


def vec(v):
    while isinstance(v, dict):
        v = v.get("vector", v.get("post"))
    return [float(x) for x in v]


class OwnAttacks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with open(SHIPPED, encoding="utf-8") as fh:
            cls.shipped = json.load(fh)["animations"]
        env = dict(os.environ, GG_ANIM_TROOP="gl_marshal")
        run = subprocess.run([sys.executable, "-c", BUILD], cwd=REPO, env=env, capture_output=True, text=True,
                             encoding="utf-8", check=True)
        cls.built = json.loads(run.stdout)

    def test_six_clips(self):
        self.assertEqual(sorted(self.built), NAMES)

    def test_length_loop_and_impact_are_the_fixed_values(self):
        for name in NAMES:
            clip = self.built[name]
            self.assertAlmostEqual(clip["animation_length"], 1.1, places=6, msg=name)
            self.assertFalse(clip.get("loop", False), name)
            if name in IMPACT:
                self.assertEqual(clip["timeline"], {IMPACT[name]: "impact"}, name)
            else:
                self.assertNotIn("timeline", clip, name)

    def test_guard_at_both_ends(self):
        # The first and last key of every channel equal the present (family) clip's, which hand over to the stances.
        # The family's legs bake leaves its own two ends up to 0.14 deg apart (a foot's euler), so the ends are compared
        # with the present clip, not with each other.
        rel = os.path.relpath(SHIPPED, REPO).replace(os.sep, "/")
        old = json.loads(subprocess.run(["git", "show", "%s:%s" % (PRESENT_AT, rel)], cwd=REPO, check=True,
                                        capture_output=True, text=True, encoding="utf-8").stdout)["animations"]
        for name in NAMES:
            bones = self.built[name]["bones"]
            self.assertEqual(sorted(bones), sorted(old[name]["bones"]), name)
            for bone, chans in bones.items():
                for ch, keys in chans.items():
                    rest = [1.0, 1.0, 1.0] if ch == "scale" else [0.0, 0.0, 0.0]     # a channel the old clip does not key
                    for i, (x, y) in enumerate(zip(ends(keys), ends(old[name]["bones"][bone].get(ch, rest)))):
                        for a, b in zip(vec(x), vec(y)):
                            self.assertAlmostEqual(a, b, delta=0.01, msg="%s %s %s %s" % (name, bone, ch,
                                                                                          ("first", "last")[i]))

    def test_shipped_clip_equals_clips(self):
        with open(OUT, encoding="utf-8") as fh:
            out = json.load(fh)["animations"]
        for name in NAMES:
            self.assertEqual(self.shipped[name], self.built[name], name)
            self.assertEqual(out[name], self.built[name], name)


if __name__ == "__main__":
    unittest.main()
