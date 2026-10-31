"""Checks tools/mapforge (plan task 2A.3): NBT round-trip on the shipped structure files, placement maths against
Structures.java, every operation, region locks, dry-run, apply, rollback and the sample batch. Pure: no Minecraft."""
import datetime
import gzip
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapforge import nbt  # noqa: E402
from mapforge.plan import PLAN_FORMAT, PlanError, apply, compile_plan, dry_run, rollback, validate, write_undo  # noqa: E402
from mapforge.structure import DATA_VERSION, PIECES, Structure, footprint, normalize, rotate_state, to_world  # noqa: E402
from mapforge.world import AIR_CELL, Cell, World  # noqa: E402

SAMPLES = ROOT / 'tools/mapforge/samples'
NOW = datetime.datetime(2026, 10, 30, tzinfo=datetime.timezone.utc)


def flat(size=16, y=64, block='minecraft:grass_block'):
    w = World('test')
    for x in range(size):
        for z in range(size):
            w.set((x, y, z), Cell(block))
    return w


def plan(ops, locks=None, **extra):
    p = {'format': PLAN_FORMAT, 'id': 'test', 'world': 'test', 'author': 'unit', 'purpose': 'unit test',
         'locks': locks or [{'id': 'all', 'mode': 'allow', 'min': [-100, -64, -100], 'max': [100, 319, 100]}],
         'operations': ops}
    p.update(extra)
    return p


class NbtTest(unittest.TestCase):
    def test_shipped_structures_round_trip_byte_for_byte(self):
        files = sorted(PIECES.rglob('*.nbt'))
        self.assertTrue(files)
        for path in files:
            raw = gzip.decompress(path.read_bytes())
            name, root = nbt.decode(raw)
            self.assertEqual(nbt.encode(root, name), raw, path.name)
            s = Structure.load(path)
            self.assertEqual(s.data_version, DATA_VERSION)
            self.assertEqual(Structure.from_tag(s.to_tag()).blocks, s.blocks, path.name)

    def test_every_tag_kind_and_modified_utf8(self):
        root = nbt.compound({'b': nbt.byte(-3), 's': nbt.short(300), 'i': nbt.int_(-70000), 'l': nbt.long(1 << 40),
                             'f': nbt.float_(0.5), 'd': nbt.double(-2.25), 'ba': nbt.Tag(nbt.BYTE_ARRAY, b'\x01\x02'),
                             'str': nbt.string('nul\x00 and \U0001F5E1'), 'li': nbt.list_(nbt.INT, [nbt.int_(1)]),
                             'empty': nbt.list_(nbt.END, []), 'ia': nbt.int_array([1, -1]),
                             'la': nbt.Tag(nbt.LONG_ARRAY, [5, -5]), 'c': nbt.compound({'x': nbt.string('y')})})
        raw = nbt.encode(root)
        self.assertNotIn(b'\x00 and', raw)  # NUL is written as C0 80
        self.assertEqual(nbt.decode(raw)[1], root)

    def test_write_is_deterministic_and_rejects_bad_lists(self):
        with tempfile.TemporaryDirectory() as d:
            a, b = pathlib.Path(d, 'a.nbt'), pathlib.Path(d, 'b.nbt')
            s = Structure.load(PIECES / 'camp/tent_ironclad.nbt')
            s.save(a)
            s.save(b)
            self.assertEqual(a.read_bytes(), b.read_bytes())
        with self.assertRaises(ValueError):
            nbt.list_(nbt.INT, [nbt.string('x')])


class PlacementTest(unittest.TestCase):
    def test_to_world_matches_structures_java(self):
        size = (13, 12, 11)  # pivot (6, 0, 5)
        self.assertEqual(to_world(size, (100, 64, 200), 0, (6, 0, 5)), (100, 64, 200))
        self.assertEqual(to_world(size, (100, 64, 200), 90, (0, 3, 0)), (105, 67, 194))
        self.assertEqual(to_world(size, (100, 64, 200), 180, (0, 0, 0)), (106, 64, 205))
        self.assertEqual(to_world(size, (100, 64, 200), 270, (0, 0, 0)), (95, 64, 206))
        self.assertEqual(footprint(size, (0, 64, 0), 90), ((-5, 64, -6), (5, 75, 6)))

    def test_state_rotation(self):
        self.assertEqual(rotate_state('oak_stairs[facing=north,half=top]', 90), 'minecraft:oak_stairs[facing=east,half=top]')
        self.assertEqual(rotate_state('minecraft:oak_log[axis=x]', 270), 'minecraft:oak_log[axis=z]')
        self.assertEqual(rotate_state('minecraft:oak_sign[rotation=14]', 90), 'minecraft:oak_sign[rotation=2]')
        self.assertEqual(rotate_state('minecraft:oak_fence[east=false,north=true,south=false,west=true]', 90),
                         'minecraft:oak_fence[east=true,north=true,south=false,west=false]')
        self.assertEqual(rotate_state('minecraft:rail[shape=north_east]', 180), 'minecraft:rail[shape=south_west]')
        self.assertEqual(rotate_state('minecraft:stone', 90), 'minecraft:stone')
        self.assertEqual(normalize('stone_slab[waterlogged=false,type=top]'), 'minecraft:stone_slab[type=top,waterlogged=false]')


class OperationTest(unittest.TestCase):
    def test_set_fill_shapes_replace_and_clear(self):
        w = flat()
        ch = compile_plan(plan([{'op': 'set', 'pos': [1, 65, 1], 'block': 'stone'}]), w)
        self.assertEqual(ch[(1, 65, 1)][1], Cell('minecraft:stone'))
        ch = compile_plan(plan([{'op': 'fill', 'min': [0, 65, 0], 'max': [2, 67, 2], 'block': 'stone', 'shape': 'outline'}]), w)
        self.assertEqual(len(ch), 26)  # 27 minus the centre
        ch = compile_plan(plan([{'op': 'fill', 'min': [0, 64, 0], 'max': [2, 66, 2], 'block': 'stone', 'shape': 'hollow'}]), w)
        self.assertEqual(len(ch), 26)  # the shell; the centre stays air
        ch = compile_plan(plan([{'op': 'fill', 'min': [0, 63, 0], 'max': [3, 65, 0], 'block': 'gravel', 'replace': ['grass_block']}]), w)
        self.assertEqual(sorted(ch), [(x, 64, 0) for x in range(4)])
        ch = compile_plan(plan([{'op': 'clear', 'min': [0, 60, 0], 'max': [1, 70, 1]}]), w)
        self.assertEqual(len(ch), 4)
        self.assertTrue(all(after == AIR_CELL for _, after, _ in ch.values()))

    def test_later_operations_see_earlier_ones(self):
        ch = compile_plan(plan([{'op': 'set', 'pos': [0, 64, 0], 'block': 'stone'},
                                {'op': 'fill', 'min': [0, 64, 0], 'max': [1, 64, 0], 'block': 'gravel', 'replace': ['stone']}]), flat())
        self.assertEqual(ch[(0, 64, 0)][1], Cell('minecraft:gravel'))
        self.assertEqual(ch[(0, 64, 0)][2], 1)
        self.assertNotIn((1, 64, 0), ch)

    def test_terrain_stamp(self):
        op = {'op': 'terrain', 'origin': [0, 0], 'base': 64, 'floor': 62, 'surface': 'grass_block', 'under': 'dirt',
              'underDepth': 1, 'core': 'stone', 'clearTo': 66, 'heights': [[2, None, -1]]}
        ch = compile_plan(plan([op]), flat())
        col = {y: ch[(0, y, 0)][1].state for y in range(62, 67) if (0, y, 0) in ch}
        self.assertEqual(col, {62: 'minecraft:stone', 63: 'minecraft:stone', 64: 'minecraft:stone', 65: 'minecraft:dirt', 66: 'minecraft:grass_block'})
        self.assertFalse(any(p[0] == 1 for p in ch))
        self.assertEqual(ch[(2, 64, 0)][1], AIR_CELL)  # dug one below base; old grass cleared
        self.assertEqual(ch[(2, 63, 0)][1], Cell('minecraft:grass_block'))

    def test_structure_placement_keeps_void_and_copies_block_entities(self):
        s = Structure((3, 1, 3), ['minecraft:air', 'minecraft:structure_void', 'minecraft:chest[facing=north]'],
                      [((0, 0, 0), 0, None), ((1, 0, 1), 2, nbt.compound({'id': nbt.string('minecraft:chest')})),
                       ((2, 0, 2), 1, None)])
        with tempfile.TemporaryDirectory() as d:
            s.save(pathlib.Path(d, 'p.nbt'))
            w = flat(y=10)
            ch = compile_plan(plan([{'op': 'structure', 'file': 'p.nbt', 'at': [5, 10, 5], 'rotation': 90}]), w, d)
            self.assertEqual(ch[(6, 10, 4)][1], AIR_CELL)  # local (0,0,0), turned
            chest = ch[(5, 10, 5)][1]
            self.assertEqual(chest.state, 'minecraft:chest[facing=east]')
            self.assertEqual(chest.be_tag()['id'].value, 'minecraft:chest')
            self.assertNotIn((4, 10, 6), ch)  # structure void keeps the grass
            ch = compile_plan(plan([{'op': 'structure', 'file': 'p.nbt', 'at': [5, 10, 5], 'includeAir': False}]), w, d)
            self.assertNotIn((4, 10, 4), ch)

    def test_validation_messages(self):
        errors = validate({'format': 'x', 'operations': [{'op': 'nuke'}, {'op': 'fill', 'min': [0, 0, 0], 'max': [-1, 0, 0], 'block': '??'},
                                                         {'op': 'structure', 'piece': 'camp/nope', 'at': [0, 0, 0], 'rotation': 45},
                                                         {'op': 'set', 'pos': [0, 0], 'block': 'stone', 'extra': 1}]})
        text = '\n'.join(errors)
        for needle in ('format must be', 'id must be', 'at least one allow', 'op must be one of', 'min [0, 0, 0] is above',
                       'not a block state', 'no structure file', 'rotation must be', 'unknown key', 'pos must be'):
            self.assertIn(needle, text)
        with self.assertRaises(PlanError):
            compile_plan({'format': PLAN_FORMAT}, flat())


class LockApplyRollbackTest(unittest.TestCase):
    def test_locks_and_limits_refuse_without_changing_the_world(self):
        w = flat()
        before = w.digest()
        locks = [{'id': 'zone', 'mode': 'allow', 'min': [0, 60, 0], 'max': [7, 80, 7]},
                 {'id': 'hearth', 'mode': 'deny', 'min': [3, 60, 3], 'max': [4, 80, 4], 'reason': 'station'}]
        p = plan([{'op': 'fill', 'min': [0, 65, 0], 'max': [9, 65, 0], 'block': 'stone'}], locks)
        s = dry_run(p, w)
        self.assertFalse(s['ok'])
        self.assertIn('outside every allow region', s['violations'][0])
        with self.assertRaises(PlanError):
            apply(p, w)
        p = plan([{'op': 'set', 'pos': [3, 65, 3], 'block': 'stone'}], locks)
        self.assertIn('deny region hearth (station)', dry_run(p, w)['violations'][0])
        p = plan([{'op': 'set', 'pos': [1, 65, 1], 'block': 'stone'}], locks)
        self.assertIn('protect-spawn', dry_run(p, w, protect=[{'id': 'protect-spawn', 'min': [1, 0, 1], 'max': [1, 100, 1]}])['violations'][0])
        p = plan([{'op': 'fill', 'min': [0, 65, 0], 'max': [3, 65, 0], 'block': 'stone'}], locks, limits={'maxChangedBlocks': 3})
        self.assertIn('over limits.maxChangedBlocks 3', dry_run(p, w)['violations'][0])
        with self.assertRaises(PlanError):
            apply(plan([{'op': 'set', 'pos': [1, 65, 1], 'block': 'stone'}], expectWorld='0' * 64), w)
        self.assertEqual(w.digest(), before)

    def test_dry_run_does_not_modify_and_apply_matches_prediction(self):
        w = flat()
        before = w.digest()
        p = plan([{'op': 'fill', 'min': [0, 65, 0], 'max': [1, 65, 1], 'block': 'stone'},
                  {'op': 'clear', 'min': [5, 64, 5], 'max': [5, 64, 5]}])
        s = dry_run(p, w)
        self.assertEqual(w.digest(), before)
        self.assertEqual((s['changed'], s['placed'], s['removed']), (5, {'minecraft:stone': 4}, {'minecraft:grass_block': 1}))
        s2, undo = apply(p, w, now=NOW)
        self.assertEqual(w.digest(), s['worldAfter'])
        self.assertEqual(undo['worldBefore'], before)
        self.assertEqual(rollback(undo, w), 5)
        self.assertEqual(w.digest(), before)

    def test_rollback_refuses_after_a_later_edit_but_keeps_edits_elsewhere(self):
        w = flat()
        _, undo = apply(plan([{'op': 'set', 'pos': [0, 65, 0], 'block': 'stone'}]), w, now=NOW)
        w.set((9, 65, 9), Cell('minecraft:oak_planks'))  # someone else's edit, elsewhere
        w2 = w.copy()
        w2.set((0, 65, 0), Cell('minecraft:dirt'))  # someone edited the plan's block
        with self.assertRaises(PlanError):
            rollback(undo, w2)
        self.assertEqual(w2.get((0, 65, 0)), Cell('minecraft:dirt'))
        rollback(undo, w)
        self.assertEqual(w.get((0, 65, 0)), AIR_CELL)
        self.assertEqual(w.get((9, 65, 9)), Cell('minecraft:oak_planks'))
        with tempfile.TemporaryDirectory() as d:
            path = pathlib.Path(d, 'u.json')
            write_undo(path, undo)
            with self.assertRaises(PlanError):
                write_undo(path, undo)

    def test_world_file_round_trip_and_structure_export(self):
        w = World.from_structure(Structure.load(PIECES / 'camp/command_tent.nbt'), (10, 64, 10), 'camp')
        with tempfile.TemporaryDirectory() as d:
            path = pathlib.Path(d, 'w.json')
            w.save(path)
            self.assertEqual(World.load(path).digest(), w.digest())
            data = json.loads(path.read_text())
            data['blocks'][0][3] = 'minecraft:diamond_block'
            path.write_text(json.dumps(data))
            with self.assertRaises(ValueError):
                World.load(path)
            lo, hi = w.bounds()
            s = w.export_structure(lo, hi)
            s.save(pathlib.Path(d, 'e.nbt'))
            again = World.from_structure(Structure.load(pathlib.Path(d, 'e.nbt')), lo, 'camp')
            self.assertEqual(again.digest(), w.digest())


class SampleTest(unittest.TestCase):
    def test_sample_batch_is_valid_deterministic_and_reversible(self):
        p = json.loads((SAMPLES / 'camp-watchpost.json').read_text())
        protect = json.loads((SAMPLES / 'camp-protect.json').read_text())['locks']
        self.assertEqual(validate(p, SAMPLES), [])
        w = flat(64)
        before = w.digest()
        s = dry_run(p, w, SAMPLES, protect)
        self.assertTrue(s['ok'], s['violations'])
        self.assertTrue(all(op['changed'] > 0 for op in s['operations']))
        _, u1 = apply(p, w, SAMPLES, protect, now=NOW)
        _, u2 = apply(p, flat(64), SAMPLES, protect, now=NOW)
        self.assertEqual(u1, u2)
        rollback(u1, w)
        self.assertEqual(w.digest(), before)

    def test_cli_round_trip_and_live_path_refusal(self):
        with tempfile.TemporaryDirectory() as d:
            world = pathlib.Path(d, 'camp.json')
            run = lambda *a: subprocess.run([sys.executable, str(ROOT / 'tools/mapforge'), *map(str, a)], capture_output=True, text=True)
            self.assertEqual(run('flat', '--out', world, '--min', 0, 0, '--max', 63, 63, '--y', 64).returncode, 0)
            digest = World.load(world).digest()
            r = run('dry-run', SAMPLES / 'camp-watchpost.json', '--world', world)
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertIn('locks ok', r.stdout)
            self.assertEqual(World.load(world).digest(), digest)
            r = run('apply', SAMPLES / 'camp-watchpost.json', '--world', world)
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertNotEqual(World.load(world).digest(), digest)
            self.assertEqual(run('rollback', pathlib.Path(d, 'camp.camp-watchpost-sample.undo.json'), '--world', world).returncode, 0)
            self.assertEqual(World.load(world).digest(), digest)
            save = pathlib.Path(d, 'save')
            save.mkdir()
            (save / 'level.dat').write_bytes(b'')
            r = run('dry-run', SAMPLES / 'camp-watchpost.json', '--world', save / 'w.json')
            self.assertNotEqual(r.returncode, 0)
            self.assertIn('offline only', r.stderr + r.stdout)


if __name__ == '__main__':
    unittest.main()
