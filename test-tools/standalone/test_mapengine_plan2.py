"""Checks map engine Plan 2 output for the mod: columns.bin, routes, determinism and the committed fixture."""
import json
import pathlib
import sys
import tempfile
import unittest

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import build as mb  # noqa: E402
from mapengine import check as mc  # noqa: E402
from mapengine import design  # noqa: E402

FIXTURE = ROOT / 'test-tools' / 'resources' / 'fixtures' / 'engine-map'


def fixture_design():
    return design.load(FIXTURE / 'design.json')


def files(d):
    return sorted(p.relative_to(d).as_posix() for p in pathlib.Path(d).rglob('*') if p.is_file())


class Plan2Test(unittest.TestCase):
    def test_columns_match_tiles(self):
        d = fixture_design()
        with tempfile.TemporaryDirectory() as tmp:
            m = mb.build(d, tmp)
            solid, water = mc.read_columns(tmp, m, d['width'], d['depth'])
            cols = mc.read_columns_bin(tmp, d['width'], d['depth'])
            f = m['floorY']
            self.assertTrue(np.array_equal(cols[0], np.where(solid == mc.NONE, mc.NONE, solid + f)))
            self.assertTrue(np.array_equal(cols[1], np.where(water == mc.NONE, mc.NONE, water + f)))
            self.assertTrue((cols[1] != mc.NONE).any(), 'fixture has a river')
            self.assertEqual(mc.check(d, tmp), [])

    def test_routes_present_and_reachable(self):
        d = fixture_design()
        with tempfile.TemporaryDirectory() as tmp:
            m = mb.build(d, tmp)
            self.assertEqual(len(m['routes']), len(m['spawns']) * len(m['points']))
            for r in m['routes']:
                self.assertIsNotNone(r['walk'], r)
                self.assertGreaterEqual(r['walk'], int(r['straight']) - 1, r)
            self.assertEqual(m['origin'], [-(d['width'] // 2), -(d['depth'] // 2)])
            self.assertEqual(len(m['roads']), 1)

    def test_build_bytes_deterministic(self):
        d = fixture_design()
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            mb.build(d, a)
            mb.build(d, b)
            self.assertEqual(files(a), files(b))
            for rel in files(a):
                self.assertEqual((pathlib.Path(a) / rel).read_bytes(), (pathlib.Path(b) / rel).read_bytes(), rel)

    def test_detour_and_distance_flagged(self):
        d = fixture_design()
        with tempfile.TemporaryDirectory() as tmp:
            mb.build(d, tmp)
            m = json.loads((pathlib.Path(tmp) / 'map.json').read_text(encoding='utf-8'))
            m['routes'][0]['walk'] += 5
            m['routes'][1]['straight'] = 1
            (pathlib.Path(tmp) / 'map.json').write_bytes(mb.canonical_bytes(m))
            codes = sorted(i.code for i in mc.check(d, tmp))
            self.assertIn('distance', codes)
            (pathlib.Path(tmp) / 'columns.bin').write_bytes(b'\x00\x00')
            self.assertIn('columns', [i.code for i in mc.check(d, tmp)])

    def test_detour_wall(self):
        d = fixture_design()
        # a cliff wall that leaves only a narrow gap at the far north edge
        d['objects'].append({'id': 'hill-9', 'kind': 'hill', 'by': 'kort', 'state': 'accepted', 'height': 20,
                             'steepness': 1, 'outline': [[50, 10], [58, 10], [58, 76], [50, 76]]})
        with tempfile.TemporaryDirectory() as tmp:
            mb.build(d, tmp)
            self.assertNotIn('detour', [i.code for i in mc.check(d, tmp)])  # about 1.7 times straight
            old, mc.DETOUR = mc.DETOUR, 1.5
            try:
                self.assertIn('detour', [i.code for i in mc.check(d, tmp)])
            finally:
                mc.DETOUR = old
        d['objects'][-1]['outline'] = [[50, -4], [58, -4], [58, 76], [50, 76]]  # no gap
        with tempfile.TemporaryDirectory() as tmp:
            mb.build(d, tmp)
            self.assertIn('unreachable', [i.code for i in mc.check(d, tmp)])

    def test_small_fixture_current(self):
        with tempfile.TemporaryDirectory() as tmp:
            mb.build(fixture_design(), tmp)
            want = [f for f in files(FIXTURE) if f != 'design.json']
            self.assertEqual(files(tmp), want)
            for rel in want:
                self.assertEqual((pathlib.Path(tmp) / rel).read_bytes(), (FIXTURE / rel).read_bytes(),
                                 rel + ' is stale: rerun python tools/mapengine build on the fixture')


if __name__ == '__main__':
    unittest.main()
