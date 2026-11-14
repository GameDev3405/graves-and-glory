"""The Templar's own attacks (art/anim/sentinel/build_own_attacks.py): the fixed gameplay and the shipped clips.

Each clip keeps the family's length (22 ticks), loop flag and impact marker (thrust 7, overhead 10, low cut 8), starts and
ends on the guard the family's clips start and end on, and the shipped files (out/ and the exported animation) hold exactly
what clips() builds. The builder imports build_anims with GG_ANIM_TROOP=sentinel, and other tests in this process import it
as the knight, so the clips are built in a child process.
"""
import json
import os
import subprocess
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
SHIPPED = (os.path.join(REPO, "art", "anim", "sentinel", "out", "sentinel.animation.json"),
           os.path.join(REPO, "src", "main", "resources", "assets", "gravesandglory", "animations", "troop",
                        "sentinel.animation.json"))
IMPACT = {"attack_thrust": 7, "attack_overhead": 10, "attack_low_cut": 8}
TICKS = 22
TPS = 20.0

CHILD = r"""
import json, os, sys
sys.path.insert(0, os.path.join(sys.argv[1], "art", "anim", "sentinel"))
import build_own_attacks as O
B = O.B
n = B.ATTACK_TICKS * B.SUB
guard = {}
for name in ("thrust", "overhead", "low_cut"):
    legs = B.attack_legs(B.ATTACK_LEGS[name])      # the family's own footwork: its first and last samples are its ends
    guard["attack_%s_legs" % name] = {b: [list(v[0]) for v in (legs[0][b], legs[n][b])]
                                      for b in legs[0] if b != "hips"}
    guard["attack_%s_legs" % name]["hips"] = [list(legs[0]["hips"][0]), list(legs[n]["hips"][0])]
up = {b: list(B.GUARD[b]) for b in B.UPPER_BONES}
up["right_forearm"] = list(B.GUARD["right_forearm"])
print(json.dumps({"clips": O.clips(), "guard_upper": up, "guard_legs": guard, "ticks": B.ATTACK_TICKS,
                  "impact": B.IMPACT}))
"""


def keys(clip, bone, channel="rotation"):
    ks = clip["bones"][bone][channel]
    ts = sorted(ks, key=float)
    val = lambda v: v["vector"] if isinstance(v, dict) else v  # noqa: E731
    return [val(ks[t]) for t in ts], [float(t) for t in ts]


class OwnAttacks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        env = dict(os.environ, GG_ANIM_TROOP="sentinel")
        r = subprocess.run([sys.executable, "-c", CHILD, REPO], env=env, capture_output=True, text=True,
                           encoding="utf-8", timeout=900)
        if r.returncode:
            raise RuntimeError(r.stderr[-2000:])
        cls.built = json.loads(r.stdout.strip().splitlines()[-1])
        cls.clips = cls.built["clips"]

    def test_six_clips(self):
        self.assertEqual(sorted(self.clips), sorted([n + s for n in IMPACT for s in ("", "_legs")]))

    def test_fixed_timing(self):
        self.assertEqual(self.built["ticks"], TICKS)
        self.assertEqual({"attack_" + k: v for k, v in self.built["impact"].items()}, IMPACT)
        for name, tick in IMPACT.items():
            for suffix in ("", "_legs"):
                clip = self.clips[name + suffix]
                self.assertFalse(clip["loop"], name + suffix)
                self.assertAlmostEqual(clip["animation_length"], TICKS / TPS, places=4, msg=name + suffix)
            marks = self.clips[name]["timeline"]
            self.assertEqual(list(marks.values()), ["impact"], name)
            self.assertAlmostEqual(float(list(marks)[0]) * TPS, tick, places=3, msg=name)

    def test_guard_at_both_ends(self):
        for name in IMPACT:
            up = self.clips[name]
            for bone, g in self.built["guard_upper"].items():
                vals, ts = keys(up, bone)
                self.assertAlmostEqual(ts[0], 0.0, places=4)
                self.assertAlmostEqual(ts[-1], TICKS / TPS, places=4)
                for v in (vals[0], vals[-1]):
                    for a, b in zip(v, g):
                        self.assertAlmostEqual(a, b, delta=0.01, msg="%s %s" % (name, bone))
            legs = self.clips[name + "_legs"]
            for bone, (first, last) in self.built["guard_legs"][name + "_legs"].items():
                vals, _ = keys(legs, bone)
                for v, g in ((vals[0], first), (vals[-1], last)):
                    for a, b in zip(v, g):
                        self.assertAlmostEqual(a, b, delta=0.01, msg="%s_legs %s" % (name, bone))
            pinned = set(self.built["guard_upper"]) | set(self.built["guard_legs"][name + "_legs"])
            for clip in (up, legs):          # every other keyed piece (cape, plates, skirts) comes home too
                for bone, ch in clip["bones"].items():
                    if bone in pinned:       # held to the family's own ends above (its legs end 0.03 deg off its start)
                        continue
                    for channel in ch:
                        vals, _ = keys(clip, bone, channel)
                        for a, b in zip(vals[0], vals[-1]):
                            self.assertAlmostEqual(a, b, delta=0.01, msg="%s %s %s" % (name, bone, channel))

    def test_shipped_files_hold_the_built_clips(self):
        for path in SHIPPED:
            with open(path, encoding="utf-8") as fh:
                shipped = json.load(fh)["animations"]
            for name, clip in self.clips.items():
                self.assertEqual(shipped[name], clip, "%s in %s" % (name, path))


if __name__ == "__main__":
    unittest.main()
