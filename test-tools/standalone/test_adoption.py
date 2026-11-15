"""Checks map adoption files (plan task 2A.5, tools/mapforge/runs.py and adopt.py): gg-mapforge-runs/1 round-trip and
determinism, tamper detection, diff/apply round-trip, batch locks, and the bake refusals. Pure: no Minecraft."""
import gzip
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapforge import adopt  # noqa: E402
from mapforge.runs import AIR, Runs, RunsError, apply, diff, from_world, to_world, validate  # noqa: E402
from mapforge.world import Cell, World  # noqa: E402

HEADER = {'map': 'ironhold', 'stamp': 'abc123'}


def base_world():
    w = World('t')
    for x in range(4):
        for z in range(4):
            for y in range(60, 63):
                w.set((x, y, z), Cell('minecraft:stone'))
            w.set((x, 63, z), Cell('minecraft:grass_block[snowy=false]'))
    return w


def capture(w):
    return from_world(w, 'capture', dict(HEADER), box=((0, 60, 0), (3, 66, 3)))


class RunsFile(unittest.TestCase):
    def test_round_trip_and_determinism(self):
        c = capture(base_world())
        data = c.to_bytes()
        self.assertEqual(data, capture(base_world()).to_bytes(), 'same world, same bytes')
        back = Runs.from_bytes(data)
        self.assertEqual(back.columns, c.columns)
        self.assertEqual(back.digest(), c.digest())
        self.assertEqual(back.cells(), 16 * 7)
        self.assertEqual(back.get(1, 63, 1), 'minecraft:grass_block[snowy=false]')
        self.assertEqual(back.get(1, 65, 1), AIR)

    def test_tampered_body_is_refused(self):
        data = capture(base_world()).to_bytes()
        text = gzip.decompress(data).decode()
        bad = gzip.compress(text.replace('minecraft:stone', 'minecraft:dirt').encode(), mtime=0)
        with self.assertRaises(RunsError):
            Runs.from_bytes(bad)

    def test_world_round_trip(self):
        w = base_world()
        self.assertEqual(to_world(capture(w), World).blocks, w.blocks)


class OverlayRoundTrip(unittest.TestCase):
    def test_diff_then_apply_gives_final(self):
        w = base_world()
        base = capture(w)
        w.set((2, 64, 2), Cell('minecraft:oak_planks'))
        w.set((0, 63, 0), Cell('minecraft:dirt'))
        final = capture(w)
        overlay = diff(base, final, dict(HEADER))
        self.assertEqual(overlay.cells(), 2)
        self.assertEqual(overlay.header['baseDigest'], base.digest_over(overlay))
        self.assertEqual(apply(overlay, base).digest(), final.digest())
        self.assertEqual(adopt.check_overlay(Runs.from_bytes(overlay.to_bytes()), base), [])

    def test_ticked_soil_still_matches(self):
        w = base_world()
        base = capture(w)
        w.set((0, 63, 0), Cell('minecraft:oak_planks'))
        overlay = diff(base, capture(w), dict(HEADER))
        ticked = base_world()
        ticked.set((0, 63, 0), Cell('minecraft:dirt'))
        self.assertEqual(capture(ticked).digest_over(overlay), overlay.header['baseDigest'])

    def test_flowing_fluid_settles_to_air_but_sources_do_not(self):
        from mapforge.runs import settle
        self.assertEqual(settle('minecraft:water[level=3]'), AIR)
        self.assertEqual(settle('minecraft:lava[level=12]'), AIR)
        self.assertEqual(settle('minecraft:water[level=0]'), 'minecraft:water[level=0]')
        self.assertEqual(settle('minecraft:oak_planks'), 'minecraft:oak_planks')

    def test_overlay_against_another_base_is_refused(self):
        w = base_world()
        base = capture(w)
        w.set((2, 64, 2), Cell('minecraft:oak_planks'))
        overlay = diff(base, capture(w), dict(HEADER))
        other = base_world()
        other.set((2, 64, 2), Cell('minecraft:cobblestone'))
        errors = adopt.check_overlay(overlay, capture(other))
        self.assertTrue(any('baseDigest' in e for e in errors), errors)
        restamped = Runs('capture', dict(HEADER, stamp='changed'))
        restamped.columns = base.columns
        self.assertTrue(any('stamp' in e for e in adopt.check_overlay(overlay, restamped)))


class Batch(unittest.TestCase):
    def test_batch_keeps_protected_cells(self):
        manifest = {'protect': [{'id': 'cap', 'center': [1, 1], 'radius': 0, 'y': [60, 70]}]}
        b = adopt.compile_batch(base_world(), 'ironhold', manifest, top=66, keep_protected=True, keep_columns=[(3, 3, 0)])
        self.assertIsNone(b.get(1, 63, 1), 'capture cylinder left alone')
        self.assertIsNone(b.get(3, 62, 3), 'kept column left alone')
        self.assertEqual(b.get(0, 66, 0), AIR, 'air up to the top')
        self.assertEqual([l['id'] for l in b.header['locks']['deny']], ['cap', 'keep-3-3'])
        self.assertEqual(Runs.from_bytes(b.to_bytes()).kind, 'batch')


class Bake(unittest.TestCase):
    def setUp(self):
        self.tmp = pathlib.Path(tempfile.mkdtemp())
        w = base_world()
        self.base = capture(w)
        w.set((2, 64, 2), Cell('minecraft:oak_planks'))
        self.overlay = diff(self.base, capture(w), dict(HEADER))
        self.base.write(self.tmp / 'base.runs.gz')
        self.overlay.write(self.tmp / 'o.runs.gz')
        adopt.TARGETS = {'dev': self.tmp / 'dev', 'release': self.tmp / 'release'}

    def test_dev_bake_stamps_mod_version(self):
        self.assertEqual(adopt.main(['bake', str(self.tmp / 'o.runs.gz'), '--base', str(self.tmp / 'base.runs.gz'), '--target', 'dev']), 0)
        baked = Runs.read(self.tmp / 'dev/ironhold.runs.gz')
        self.assertEqual(baked.header['modVersion'], adopt.mod_version())
        self.assertEqual(baked.digest(), self.overlay.digest())
        self.assertEqual(validate(self.tmp / 'dev/ironhold.runs.gz')[0], [])

    def test_release_bake_needs_approval(self):
        self.assertEqual(adopt.main(['bake', str(self.tmp / 'o.runs.gz'), '--base', str(self.tmp / 'base.runs.gz'), '--target', 'release']), 2)
        self.assertFalse((self.tmp / 'release').exists())

    def test_stamp_mismatch_is_refused(self):
        other = Runs('capture', dict(HEADER, stamp='new-generator'))
        other.columns = self.base.columns
        other.write(self.tmp / 'other.runs.gz')
        self.assertEqual(adopt.main(['bake', str(self.tmp / 'o.runs.gz'), '--base', str(self.tmp / 'other.runs.gz'), '--target', 'dev']), 2)


class DataVersionCarried(unittest.TestCase):
    def test_diff_carries_the_source_data_version(self):
        w = base_world()
        base = from_world(w, 'capture', {'map': 't', 'dataVersion': 3955}, box=((0, 60, 0), (3, 66, 3)))
        w.set((1, 61, 1), Cell('minecraft:dirt'))
        final = from_world(w, 'capture', {'map': 't', 'dataVersion': 3955}, box=((0, 60, 0), (3, 66, 3)))
        self.assertEqual(diff(base, final).header['dataVersion'], 3955)

    def test_writers_default_to_pre_port_3465(self):
        c = capture(base_world())
        self.assertEqual(c.header['dataVersion'], 3465)
        self.assertEqual(diff(c, capture(base_world())).header['dataVersion'], 3465)
        self.assertEqual(Runs.from_bytes(c.to_bytes()).header['dataVersion'], 3465)


if __name__ == '__main__':
    unittest.main()
