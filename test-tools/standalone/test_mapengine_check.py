"""Checks tools/mapengine checker: built output against the design."""
import copy
import json
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import build as mb  # noqa: E402
from mapengine import check as mc  # noqa: E402
from mapengine import design  # noqa: E402
from mapengine.design import new_design  # noqa: E402


def make(*objs, w=128, d=64, point=(64, 32)):
    out = new_design('t', 'T', w, d)
    full = [{'kind': 'spawn', 'team': 'dawn', 'x': 6, 'z': 6},
            {'kind': 'spawn', 'team': 'dusk', 'x': 120, 'z': 6},
            {'kind': 'point', 'name': 'A', 'x': point[0], 'z': point[1], 'radius': 4}] + list(objs)
    for i, o in enumerate(full):
        out['objects'].append(dict(o, id=f"{o['kind']}-{i + 1}", by='kort', state='accepted'))
    return out


def built(d):
    tmp = tempfile.TemporaryDirectory()
    mb.build(d, tmp.name)
    return tmp


def codes(issues):
    return sorted((i.code, i.object_id) for i in issues)


class CheckTest(unittest.TestCase):
    def test_sample_builds_clean(self):
        d = design.load(ROOT / 'maps' / 'sample' / 'design.json')
        with tempfile.TemporaryDirectory() as tmp:
            mb.build(d, tmp)
            self.assertEqual(mc.check(d, tmp), [])

    def test_two_spawns_on_one_team_not_moved(self):
        d = make({'kind': 'spawn', 'team': 'dawn', 'x': 10, 'z': 40})
        with built(d) as tmp:
            self.assertEqual(mc.check(d, tmp), [])


    def test_flat_is_clean(self):
        d = make()
        with built(d) as tmp:
            self.assertEqual(mc.check(d, tmp), [])

    def test_point_in_water_flagged(self):
        d = make({'kind': 'river', 'path': [[64, 0], [64, 64]], 'width': 6, 'depth': 3})
        with built(d) as tmp:
            self.assertIn(('in_water', 'point-3'), codes(mc.check(d, tmp)))

    def test_cliff_ring_unreachable(self):
        d = make({'kind': 'hill', 'outline': [[50, 20], [78, 20], [78, 44], [50, 44]], 'height': 20, 'steepness': 1.0})
        with built(d) as tmp:
            issues = mc.check(d, tmp)
        self.assertEqual(codes(issues), [('unreachable', 'point-3')])
        self.assertIn('dawn', issues[0].message)

    def test_gentle_hill_reachable(self):
        d = make({'kind': 'hill', 'outline': [[30, 10], [100, 10], [100, 54], [30, 54]], 'height': 10, 'steepness': 0.0})
        with built(d) as tmp:
            self.assertEqual(mc.check(d, tmp), [])

    def test_stale_after_edit(self):
        d = make()
        with built(d) as tmp:
            d2 = copy.deepcopy(d)
            d2['objects'][2]['x'] = 70
            codes_ = codes(mc.check(d2, tmp))
            self.assertIn(('stale', 't'), codes_)

    def test_moved_detects_tampered_manifest(self):
        d = make()
        with built(d) as tmp:
            p = pathlib.Path(tmp) / 'map.json'
            m = json.loads(p.read_text())
            m['points'][0]['x'] += 3
            p.write_text(json.dumps(m))
            self.assertEqual(codes(mc.check(d, tmp)), [('moved', 'point-3')])

    def test_radius_clipped(self):
        d = make(point=(2, 32))
        with built(d) as tmp:
            self.assertEqual(codes(mc.check(d, tmp)), [('radius_clipped', 'point-3')])


if __name__ == '__main__':
    unittest.main()
