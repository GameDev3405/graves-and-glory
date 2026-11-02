"""Checks tools/mapengine block builder: structure tiles and map.json."""
import json
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import build as mb  # noqa: E402
from mapengine.design import new_design  # noqa: E402
from mapforge.structure import Structure  # noqa: E402


def make(*objs, w=128, d=64):
    out = new_design('t', 'T', w, d)
    for i, o in enumerate(objs):
        out['objects'].append(dict(o, id=f"{o['kind']}-{i + 1}", by='kort', state='accepted'))
    return out


def full(*objs, **kw):
    return make({'kind': 'spawn', 'team': 'dawn', 'x': 5, 'z': 5},
                {'kind': 'spawn', 'team': 'dusk', 'x': 120, 'z': 5},
                {'kind': 'point', 'name': 'A', 'x': 64, 'z': 10, 'radius': 8}, *objs, **kw)


def hill(sq, h=10):
    return {'kind': 'hill', 'outline': sq, 'height': h, 'steepness': 1.0}


def blocks_at(out, tile, x, z):
    s = Structure.load(out / 'tiles' / tile)
    return {p[1]: s.palette[i] for p, i, _ in s.blocks if p[0] == x and p[2] == z}, s


class BuildTest(unittest.TestCase):
    def build(self, d):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        out = pathlib.Path(tmp.name)
        return out, mb.build(d, out)

    def test_build_needs_spawns_and_points(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(mb.BuildError) as cm:
                mb.build(make(), pathlib.Path(tmp))
            self.assertIn('dusk', ' '.join(cm.exception.problems))

    def test_tiles_and_manifest(self):
        out, m = self.build(full(hill([[10, 10], [50, 10], [50, 50], [10, 50]], 10)))
        self.assertEqual(len(m['tiles']), 2)
        for t in m['tiles']:
            self.assertTrue((out / t['file']).exists())
            self.assertEqual(t['size'][0], 64)
            self.assertEqual(t['size'][2], 64)
        self.assertEqual(json.loads((out / 'map.json').read_text()), m)
        self.assertEqual(m['floorY'], 48)
        self.assertEqual(m['tiles'][1]['x'], 64)
        pt = m['points'][0]
        self.assertEqual(pt['y'], 64 + 0 + 1)  # flat at the point cell
        self.assertEqual(len(m['design_sha256']), 64)

    def test_column_blocks(self):
        out, m = self.build(full(hill([[10, 10], [50, 10], [50, 50], [10, 50]], 10),
                                 {'kind': 'river', 'path': [[0, 58.5], [128, 58.5]], 'width': 4, 'depth': 3}))
        col, s = blocks_at(out, 't_0_0.nbt', 30, 30)
        self.assertEqual(col[10 - mb.FLOOR], 'minecraft:grass_block')
        self.assertNotIn(10 - mb.FLOOR + 1, col)
        self.assertEqual(col[10 - mb.FLOOR - 1], 'minecraft:dirt')
        self.assertEqual(col[10 - mb.FLOOR - 4], 'minecraft:stone')
        self.assertEqual(col[0], 'minecraft:stone')
        col, _ = blocks_at(out, 't_0_0.nbt', 30, 58)
        self.assertEqual(col[-3 - mb.FLOOR], 'minecraft:gravel')
        self.assertEqual(col[-3 - mb.FLOOR + 1], 'minecraft:water')
        self.assertEqual(col[0 - mb.FLOOR], 'minecraft:water')
        self.assertEqual(s.size[1], m['tiles'][0]['size'][1])

    def test_road(self):
        out, _ = self.build(full({'kind': 'road', 'path': [[0, 40.5], [128, 40.5]], 'width': 3}))
        col, _ = blocks_at(out, 't_0_0.nbt', 20, 40)
        self.assertEqual(col[-mb.FLOOR], 'minecraft:dirt_path')

    def test_zone_palette(self):
        z = {'kind': 'zone', 'outline': [[0, 0], [30, 0], [30, 30], [0, 30]], 'palette': 'ironhold'}
        out, _ = self.build(full(z))
        col, _ = blocks_at(out, 't_0_0.nbt', 15, 15)
        self.assertEqual(col[-mb.FLOOR], 'minecraft:stone')
        col, _ = blocks_at(out, 't_0_0.nbt', 40, 40)
        self.assertEqual(col[-mb.FLOOR], 'minecraft:grass_block')

    def test_same_design_same_bytes(self):
        d = full(hill([[10, 10], [50, 10], [50, 50], [10, 50]], 10))
        a, _ = self.build(d)
        b, _ = self.build(d)
        for rel in ('map.json', 'tiles/t_0_0.nbt', 'tiles/t_64_0.nbt'):
            self.assertEqual((a / rel).read_bytes(), (b / rel).read_bytes(), rel)

    def test_fast_writer_matches_mapforge(self):
        import numpy as np
        arr = np.full((3, 4, 5), -1, dtype=np.int16)
        arr[0, 0, :3], arr[1, 2, 1], arr[2, 3, 4] = 7, 2, 7
        states = [f'minecraft:s{i}' for i in range(9)]
        with tempfile.TemporaryDirectory() as tmp:
            fast = pathlib.Path(tmp) / 'a.nbt'
            mb._write_tile(fast, arr, states, 5)
            s = Structure.load(fast)
            ref = pathlib.Path(tmp) / 'b.nbt'
            s.save(ref)
            self.assertEqual(fast.read_bytes(), ref.read_bytes())
            self.assertEqual(s.palette, ['minecraft:s7', 'minecraft:s2'])
            self.assertEqual(len(s.blocks), 5)

    def test_tiles_roundtrip_mapforge(self):
        out, m = self.build(full())
        s = Structure.load(out / m['tiles'][0]['file'])
        self.assertEqual(s.data_version, 3465)
        self.assertEqual(s.size, tuple(m['tiles'][0]['size']))


if __name__ == '__main__':
    unittest.main()
