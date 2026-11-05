"""The Thornhide archer on the articulated rig: the feet stay planted, nothing pops between frames, the loops close and
the strikes end on ready, both hands reach their targets (the bow's grip, the arrow's nock on the string), the bow,
arrow and quiver stay out of the body, the forearms stay out of the hard armour, and the committed exports match."""
import json
import math
import pathlib
import sys
import unittest

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import animate_archer as AA                                                  # noqa: E402
import animate_combat as AC                                                  # noqa: E402
from unit_anim import accessories as ACC, archer_clips as XC, clearance as CL, rig   # noqa: E402

CLIPS = [f() for f in XC.CLIPS]
UNIT = AA.ArcherUnit()

# gates, from what this armour allows at Minecraft proportions (see art/animations/thornhide_archer/README.md)
ITEMS_MAX = 7.5        # px^3 of the bow, arrow or quiver inside the body on any 60 fps frame (only the bow
                       # swinging up onto the line passes 1.5: its lower tip brushes the lead shin for a moment)
LIMBS_MAX = 12.5       # px^3 of forearm deeper than 1.1 px in the hard armour (the fur ruff gives), any 60 fps frame
REACH_MAX = 0.35       # px between a hand and its target


def geo(R1, R2):
    return math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(R1.T @ R2) - 1) / 2))))


class ArcherAnimTest(unittest.TestCase):
    def test_feet(self):
        for c in CLIPS:
            self.assertLess(c.ik_error, 0.3, c.name)
            sink, gap, slide = AC.feet_report(c)
            self.assertLessEqual(sink, 0.15, '%s: a sole sinks %s px' % (c.name, sink))
            self.assertLessEqual(gap, 0.15, '%s: a planted foot hovers %s px' % (c.name, gap))
            self.assertLessEqual(slide, 8.0, '%s: a planted foot skates at %s px/s' % (c.name, slide))

    def test_hands_reach(self):
        for c in CLIPS:
            self.assertLessEqual(c.reach_error, REACH_MAX, '%s: a hand misses its target by %.2f px' % (c.name, c.reach_error))

    def test_no_pops(self):
        for c in CLIPS:
            n = int(round(c.length * 60))
            for b in c.rot:
                Rs = [rig.mc_rotation(*rig.sample(c.rot[b], i / 60)) for i in range(n + 1)]
                g = [geo(Rs[i], Rs[i + 1]) for i in range(n)]
                self.assertLessEqual(max(g), 50, '%s %s turns %.1f degrees in a frame' % (c.name, b, max(g)))
                acc = max(abs(g[i + 1] - g[i]) for i in range(n - 1))
                self.assertLessEqual(acc, 30, '%s %s changes speed by %.1f degrees a frame' % (c.name, b, acc))

    def test_loops_close_and_clips_end_on_ready(self):
        ready, _ = [c for c in CLIPS if c.name == 'archer_idle'][0].at(0)
        for c in CLIPS:
            r0, p0 = c.at(0)
            r1, p1 = c.at(c.length - 1e-9 if c.loop else c.length)
            for b in r0:
                if b in ('cape', 'plume', 'loin'):
                    continue
                self.assertLess(geo(rig.mc_rotation(*r0[b]), rig.mc_rotation(*r1[b])), 0.5, '%s %s' % (c.name, b))
            if not c.loop:
                for b in ('body', 'chest', 'head', 'right_leg', 'left_leg'):
                    self.assertLess(geo(rig.mc_rotation(*r1[b]), rig.mc_rotation(*ready[b])), 1.0, '%s %s' % (c.name, b))

    def test_items_and_forearms_stay_out_of_the_body(self):
        items = UNIT.clipper()
        limbs = CL.LimbClipper([(b, bone) for b, f, bone, l in UNIT.parts if l is None], bones=XC.BONES,
                               soft=XC.LIMB_SOFT)
        for c in CLIPS:
            for i in range(int(round(c.length * 60)) + 1):
                pose = AC.clip_pose(c, i / 60)
                for item, v in items.volumes(pose).items():
                    self.assertLessEqual(v, ITEMS_MAX, '%s at %.2f s: %.1f px^3 of the %s inside the body' %
                                         (c.name, i / 60, v, item))
                v = limbs.volume(pose)
                self.assertLessEqual(v, LIMBS_MAX, '%s at %.2f s: %.1f px^3 of forearm in the armour' % (c.name, i / 60, v))

    def test_the_string_follows_the_hand_and_the_arrow_flies(self):
        shoot = [c for c in CLIPS if c.name == 'archer_shoot'][0]
        draws = [d for _, d in shoot.draws]
        self.assertAlmostEqual(min(draws), -6.1, places=2)          # full draw, a hair of expansion
        self.assertGreater(max(draws), ACC.BRACE + 0.3)            # the string snaps past brace after the loose
        self.assertIsNotNone(XC.flight(shoot, shoot.loose + 0.05))
        self.assertFalse(XC.arrow_visible(shoot, shoot.loose + 0.05))

    def test_exports_match(self):
        out = ROOT / 'art/animations/thornhide_archer'
        self.assertEqual((out / 'archer_clips.java').read_text(encoding='utf-8'), AC.java(CLIPS, AA.SHOW))
        self.assertEqual(json.loads((out / 'archer_clips.json').read_text(encoding='utf-8')),
                         json.loads(json.dumps(AC.data(CLIPS, AA.data_extra))))


if __name__ == '__main__':
    unittest.main()
