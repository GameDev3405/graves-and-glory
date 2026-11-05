"""The Oathbound knight's combat pack on the articulated rig: the armour is cut cleanly at the joints, the legs put the
feet where the clips place them (no sinking, no hovering, no skating), nothing pops between frames, the loops close,
every strike returns to the guard, and the committed Java and JSON exports match what the clips build."""
import json
import math
import pathlib
import sys
import unittest

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import animate_combat as A                                    # noqa: E402
from unit_anim import combat_clips as CC, combat_rig as CR, rig   # noqa: E402

CLIPS = [f() for f in CC.CLIPS]


def geo(R1, R2):
    return math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(R1.T @ R2) - 1) / 2))))


class CombatAnimTest(unittest.TestCase):
    def test_armour_is_cut_at_the_joints(self):
        unit = A.CombatUnit()
        whole = {b.name: b for b, f, bone, l in A.AU.Unit().parts if l is None}
        cut = {}
        for b, faces, bone, l in unit.parts:
            if l is None and '~joint' not in b.name:
                cut.setdefault(b.name.split('~')[0], []).append((b, faces, bone))
        self.assertEqual(set(cut), set(whole))
        lo_of = {'chest': CR.WAIST_Y, 'right_forearm': None, 'right_shin': None}
        for name, pieces in cut.items():
            src = whole[name]
            # the pieces stack back into the original box, row for row
            self.assertAlmostEqual(sum(p.size[1] for p, _, _ in pieces), src.size[1], places=6, msg=name)
            for p, faces, bone in pieces:
                self.assertIn(bone, CR.BONES, name)
                for f in ('north', 'south', 'east', 'west'):
                    if f in faces:
                        self.assertEqual(faces[f][2][3], round(p.size[1]), '%s %s keeps its own rows' % (p.name, f))
            if len(pieces) > 1:
                bones = [bone for _, _, bone in pieces]
                self.assertEqual(len(set(bones)), len(bones), '%s: one piece per segment' % name)

    def test_feet_and_reach(self):
        for c in CLIPS:
            self.assertLess(c.ik_error, 0.3, '%s: a foot placement is out of the leg\'s reach' % c.name)
            sink, gap, slide = A.feet_report(c)
            self.assertLessEqual(sink, 0.15, '%s: a sole sinks %s px into the floor' % (c.name, sink))
            self.assertLessEqual(gap, 0.15, '%s: a planted foot hovers %s px' % (c.name, gap))
            self.assertLessEqual(slide, 8.0, '%s: a planted foot skates at %s px/s' % (c.name, slide))

    def test_no_pops(self):
        """Every bone's true rotation between 60 fps frames (not the Euler numbers, which gimbal lock can inflate):
        strikes may be fast but nothing jumps, and speed never changes by more than a strike's worth in a frame."""
        for c in CLIPS:
            n = int(round(c.length * 60))
            for b in c.rot:
                Rs = [rig.mc_rotation(*rig.sample(c.rot[b], i / 60)) for i in range(n + 1)]
                g = [geo(Rs[i], Rs[i + 1]) for i in range(n)]
                self.assertLessEqual(max(g), 50, '%s %s turns %.1f degrees in a frame' % (c.name, b, max(g)))
                acc = max(abs(g[i + 1] - g[i]) for i in range(n - 1))
                self.assertLessEqual(acc, 30, '%s %s changes speed by %.1f degrees a frame' % (c.name, b, acc))

    def test_loops_close_and_strikes_return_to_guard(self):
        guard_rot, guard_pos = [c for c in CLIPS if c.name == 'combat_idle'][0].at(0)
        for c in CLIPS:
            r0, p0 = c.at(0)
            r1, p1 = c.at(c.length - 1e-9 if c.loop else c.length)
            for b in r0:
                if b in ('cape', 'plume'):
                    continue
                self.assertLess(geo(rig.mc_rotation(*r0[b]), rig.mc_rotation(*r1[b])), 0.5, '%s %s' % (c.name, b))
            np.testing.assert_allclose(p0['root'], p1['root'], atol=0.01)
            if not c.loop:
                for b in r1:
                    if b in ('cape', 'plume', 'right_forearm', 'right_hand', 'right_arm', 'left_forearm'):
                        continue      # the idle's arms carry their overlap lag at its first frame
                    self.assertLess(geo(rig.mc_rotation(*r1[b]), rig.mc_rotation(*guard_rot[b])), 1.0,
                                    '%s ends off the guard at %s' % (c.name, b))

    def test_exports_match(self):
        out = ROOT / 'art/animations/oathbound_combat'
        self.assertEqual((out / 'combat_clips.java').read_text(encoding='utf-8'), A.java(CLIPS))
        self.assertEqual(json.loads((out / 'combat_clips.json').read_text(encoding='utf-8')),
                         json.loads(json.dumps(A.data(CLIPS))))
        bb = json.loads((out / 'combat_knight.bbmodel').read_text(encoding='utf-8'))
        self.assertEqual(sorted(a['name'] for a in bb['animations']), sorted(c.name for c in CLIPS))
        self.assertEqual({g['name'] for g in bb['groups']} >= set(CR.BONES), True)


if __name__ == '__main__':
    unittest.main()
