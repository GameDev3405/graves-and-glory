"""Checks tools/mapengine terrain heightfield: hills, rivers, roads, zones. Pure: no Minecraft."""
import pathlib
import sys
import time
import unittest

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine.design import new_design, validate  # noqa: E402
from mapengine.terrain import NO_WATER, heightfield  # noqa: E402


def design(*objs, w=64, d=64):
    out = new_design('t', 'T', w, d)
    for i, o in enumerate(objs):
        o = dict(o, id=f"{o['kind']}-{i + 1}", by='kort', state='accepted')
        out['objects'].append(o)
    return out


def square(x0, z0, x1, z1):
    return [[x0, z0], [x1, z0], [x1, z1], [x0, z1]]


def hill(outline, height=20, steepness=1.0):
    return {'kind': 'hill', 'outline': outline, 'height': height, 'steepness': steepness}


class TerrainTest(unittest.TestCase):
    def test_flat_when_empty(self):
        f = heightfield(design())
        self.assertEqual(f.height.shape, (64, 64))
        self.assertEqual(f.height.dtype, np.int16)
        self.assertTrue((f.height == 0).all())
        self.assertTrue((f.water == NO_WATER).all())
        self.assertFalse(f.road.any())
        self.assertTrue((f.zone == -1).all())
        self.assertEqual(f.zones, [])

    def test_hill_peak_and_edge(self):
        sq = square(12, 12, 52, 52)
        f = heightfield(design(hill(sq, 20, 1.0)))
        self.assertEqual(f.height[32, 32], 20)
        self.assertEqual(f.height[11, 32], 0)
        self.assertEqual(f.height[32, 11], 0)
        self.assertGreater(f.height[12, 32], 0)  # first inside ring
        self.assertGreater(f.height[32, 12], 0)
        gentle = heightfield(design(hill(sq, 20, 0.0)))
        self.assertLess(gentle.height[15, 32], f.height[15, 32])

    def test_hill_order_independent_for_max(self):
        a = hill(square(8, 8, 40, 40), 15, 0.5)
        b = hill(square(24, 24, 56, 56), 25, 0.2)
        self.assertTrue((heightfield(design(a, b)).height == heightfield(design(b, a)).height).all())

    def test_hollow_uses_min(self):
        f = heightfield(design(hill(square(12, 12, 52, 52), 10, 1.0), hill(square(24, 24, 40, 40), -6, 1.0)))
        self.assertEqual(f.height[32, 32], -6)
        self.assertEqual(f.height[16, 16], 10)

    def test_river_carves_hill(self):
        river = {'kind': 'river', 'path': [[0, 32.5], [64, 32.5]], 'width': 4, 'depth': 3}
        f = heightfield(design(hill(square(12, 12, 52, 52), 20, 1.0), river))
        self.assertEqual(f.height[32, 32], -3)
        self.assertEqual(f.water[32, 32], 0)
        self.assertEqual(f.height[31, 32], -3)
        self.assertEqual(f.height[33, 32], -3)
        self.assertLessEqual(f.height[35, 32], 1)  # bank ring: dist 2.5, 0.5 past the water edge
        self.assertGreater(f.height[40, 32], 1)    # hill is intact away from the river
        self.assertEqual(f.water[40, 32], NO_WATER)

    def test_road_mask_width(self):
        # Path coordinates are block corners: z = 32.5 runs through the centre of cell row 32.
        road = {'kind': 'road', 'path': [[0, 32.5], [64, 32.5]], 'width': 5}
        f = heightfield(design(road))
        for z in range(30, 35):
            self.assertTrue(f.road[z, 10], z)
        self.assertFalse(f.road[29, 10])
        self.assertFalse(f.road[35, 10])
        self.assertTrue((f.height == 0).all())

    def test_zones_later_overwrites(self):
        z1 = {'kind': 'zone', 'outline': square(0, 0, 40, 40), 'palette': 'ironhold'}
        z2 = {'kind': 'zone', 'outline': square(20, 20, 60, 60), 'palette': 'frostmarch'}
        f = heightfield(design(z1, z2))
        self.assertEqual(f.zones, ['ironhold', 'frostmarch'])
        self.assertEqual(f.zone[10, 10], 0)
        self.assertEqual(f.zone[30, 30], 1)
        self.assertEqual(f.zone[50, 50], 1)
        self.assertEqual(f.zone[60, 5], -1)

    def test_off_canvas_and_self_crossing_do_not_crash(self):
        bow = [[-20, -20], [90, 90], [90, -20], [-20, 90]]  # bow tie, vertices far off canvas
        line = [[-50, 10], [200, 10]]
        d = design(hill(bow, 10, 0.5),
                   {'kind': 'zone', 'outline': bow, 'palette': 'default'},
                   {'kind': 'river', 'path': line, 'width': 3, 'depth': 2},
                   {'kind': 'road', 'path': [[10, -30], [10, 99]], 'width': 3})
        f = heightfield(d)
        self.assertEqual(f.height.shape, (64, 64))
        self.assertTrue(f.road[20, 10])
        self.assertEqual(f.water[10, 30], 0)

    def test_hill_across_canvas_edge_keeps_height(self):
        f = heightfield(design(hill(square(-30, -30, 30, 30), 20, 1.0)))
        self.assertEqual(f.height[0, 0], 20)  # the hill continues off the canvas, so no slope at the edge

    def test_deterministic(self):
        d = design(hill(square(10, 10, 50, 50), 18, 0.3),
                   {'kind': 'river', 'path': [[0, 20], [64, 40]], 'width': 4, 'depth': 2},
                   {'kind': 'road', 'path': [[5, 5], [60, 55]], 'width': 3})
        a, b = heightfield(d), heightfield(d)
        for name in ('height', 'water', 'road', 'zone'):
            self.assertTrue((getattr(a, name) == getattr(b, name)).all(), name)

    def test_full_size_speed(self):
        objs = []
        for i in range(6):
            cx, cz = 100 + i * 100, 100 + (i % 3) * 140
            ring = [[cx + 70, cz], [cx + 35, cz + 60], [cx - 35, cz + 60], [cx - 70, cz], [cx - 35, cz - 60], [cx + 35, cz - 60]]
            objs.append(hill(ring, 30 + i, 0.2 * i))
        objs.append({'kind': 'river', 'path': [[0, 250], [200, 220], [450, 280], [700, 250]], 'width': 8, 'depth': 3})
        objs.append({'kind': 'road', 'path': [[10, 20], [350, 250], [690, 480]], 'width': 5})
        objs.append({'kind': 'road', 'path': [[10, 480], [350, 300], [690, 20]], 'width': 4})
        d = design(*objs, w=700, d=500)
        self.assertEqual(validate(d), [])
        best = min(self._time(d) for _ in range(3))
        self.assertLess(best, 0.5, f'heightfield took {best:.3f}s')

    @staticmethod
    def _time(d):
        t = time.perf_counter()
        heightfield(d)
        return time.perf_counter() - t


if __name__ == '__main__':
    unittest.main()
