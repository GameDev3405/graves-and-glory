"""Checks that the map packer classifies the two map blocks by id (docs/props/MAP-BLOCKS.md): troop_fence is a wall (never a floor, cuts the edges
through it), still_water is no-entry water (never a floor, nothing stands under it, it reads back as water). Synthetic sections: no bundled map holds either block yet."""
import gzip
import pathlib
import sys
import tempfile
import unittest

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import sectionmap as sm  # noqa: E402
from mapengine.build import ground_state, water_state  # noqa: E402

FENCE, STILL, STONE, WATER = 'gravesandglory:troop_fence', 'gravesandglory:still_water', 'minecraft:stone', 'minecraft:water'
PALETTE = [STONE, FENCE, STILL, WATER]
S, F, ST, W = 0, 1, 2, 3  # palette indices: stone, fence, still_water, real water; -1 is air


def section(depth=3, width=12, height=6):
    return np.full((depth, width, height), -1, dtype=np.int16)  # [z, x, y]


class MapBlockClassification(unittest.TestCase):
    def test_columns_masks(self):
        self.assertFalse(ground_state(FENCE))
        self.assertFalse(ground_state(STILL))
        self.assertTrue(ground_state(STONE))
        self.assertTrue(water_state(STILL))
        self.assertTrue(water_state(WATER))
        self.assertFalse(water_state(FENCE))
        self.assertFalse(water_state(STONE))

    def test_kinds(self):
        self.assertEqual(sm.kind(FENCE), 3)
        self.assertEqual(sm.kind(STILL), 2)
        self.assertEqual(sm.kind('minecraft:oak_fence'), 3)
        self.assertEqual(sm.kind(STONE), 1)

    def test_fence_line_is_a_wall(self):
        for height in (1, 2):
            stack = section()
            stack[:, :, 0] = S  # a stone floor, top at y 0
            stack[:, 5, 1:1 + height] = F  # a fence line across x 5, all three rows
            floors = sm.floors_of(stack, PALETTE, 64)
            for z in range(3):
                self.assertNotIn((z, 5), floors, f'a fence column is no floor ({height} high)')
                self.assertEqual(floors[(z, 4)], [64])
                self.assertEqual(floors[(z, 6)], [64])
            dist = sm.floor_distances(floors, 12, 3, (0, 1), 64)
            self.assertNotIn((11, 1, 64), dist, 'nothing is reachable across the fence line')
            self.assertIn((4, 1, 64), dist)

    def test_fence_gap_is_a_door(self):
        stack = section()
        stack[:, :, 0] = S
        stack[0, 5, 1] = F
        stack[2, 5, 1] = F  # a one-wide gap at z 1
        floors = sm.floors_of(stack, PALETTE, 64)
        dist = sm.floor_distances(floors, 12, 3, (0, 1), 64)
        self.assertIn((11, 1, 64), dist)
        self.assertEqual(dist[(11, 1, 64)], 11)

    def test_control_without_the_fence(self):
        stack = section()
        stack[:, :, 0] = S
        floors = sm.floors_of(stack, PALETTE, 64)
        self.assertIn((11, 1, 64), sm.floor_distances(floors, 12, 3, (0, 1), 64))

    def test_still_water_is_no_entry(self):
        # a pool: stone bed (y 0), real water (y 1), still_water on it (y 2), banks of stone two high with feet at y 2
        stack = section(width=12)
        stack[:, :, 0] = S
        stack[:, :4, 1] = S
        stack[:, 8:, 1] = S
        stack[:, 4:8, 1] = W
        stack[:, 4:8, 2] = ST
        floors = sm.floors_of(stack, PALETTE, 64)
        for x in range(4, 8):
            self.assertNotIn((1, x), floors, 'the pool columns have no floor')
        dist = sm.floor_distances(floors, 12, 3, (0, 1), 65)
        self.assertNotIn((11, 1, 65), dist, 'a pool of still_water cannot be crossed')

    def test_still_water_over_dry_ground_is_no_floor(self):
        stack = section()
        stack[:, :, 0] = S
        stack[1, 5, 1] = ST  # still_water placed by mistake on dry ground
        floors = sm.floors_of(stack, PALETTE, 64)
        self.assertNotIn((1, 5), floors)
        self.assertIn((1, 4), floors)

    def test_bake_is_byte_reproducible(self):
        stack = section()
        stack[:, :, 0] = S
        stack[:, 5, 1] = F
        stack[1, 8, 1] = ST
        with tempfile.TemporaryDirectory() as d:
            a, b = pathlib.Path(d) / 'a.gz', pathlib.Path(d) / 'b.gz'
            for out in (a, b):
                sm.write_nav(out, sm.floors_of(stack, PALETTE, 64), 12, 3, 64, 'f' * 64)
            self.assertEqual(a.read_bytes(), b.read_bytes())
            body = gzip.decompress(a.read_bytes())
            self.assertTrue(body.startswith(b'NAV1'))


if __name__ == '__main__':
    unittest.main()
