"""The Oathbound knight's clips: no channel pops between frames, the feet stay on the floor, and the committed Java and
JSON exports match what the clips build."""
import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import animate_unit as A                                    # noqa: E402
from unit_anim import oathbound_clips as CL, rig            # noqa: E402


class UnitAnimTest(unittest.TestCase):
    def test_clips_are_smooth_grounded_and_exported(self):
        combo, guard = CL.combo(), CL.guard()
        for c in (combo, guard):
            for bone, (step, accel) in A.smoothness(c).items():
                self.assertLessEqual(step, 25, '%s %s moves %s degrees in one frame' % (c.name, bone, step))
                self.assertLessEqual(accel, 12, '%s %s changes speed by %s degrees a frame' % (c.name, bone, accel))
            for i in range(0, int(c.length * 20) + 1):
                rot, pos = c.at(i / 20)
                lift = rig.ground_root_y(rot, {'root': (pos['root'][0], 0, pos['root'][2])})
                self.assertAlmostEqual(lift, pos['root'][1], delta=0.05, msg='%s at %.2f s: the feet leave the floor' % (c.name, i / 20))
        out = ROOT / 'art/animations/oathbound'
        self.assertEqual((out / 'oath_clips.java').read_text(encoding='utf-8'), A.java([combo, guard]))
        self.assertEqual(json.loads((out / 'oath_clips.json').read_text(encoding='utf-8')),
                         json.loads(json.dumps(A.data([combo, guard]))))


if __name__ == '__main__':
    unittest.main()
