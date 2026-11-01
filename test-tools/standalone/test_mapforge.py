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
from mapforge.plan import PLAN_FORMAT, PlanError, apply, compile_plan, dry_run, format_summary, rollback, validate, write_undo  # noqa: E402
from mapforge.structure import DATA_VERSION, PIECES, Structure, footprint, normalize, rotate_state, to_world  # noqa: E402
from mapforge.world import AIR_CELL, Cell, World  # noqa: E402
from mapforge import render  # noqa: E402
from mapforge import manifest as mf  # noqa: E402
from mapforge.colours import UNKNOWN, Palette  # noqa: E402

try:
    import PIL  # noqa: F401
    HAVE_PIL = True
except ImportError:
    HAVE_PIL = False

SAMPLES = ROOT / 'tools/mapforge/samples'
NOW = datetime.datetime(2026, 10, 30, tzinfo=datetime.timezone.utc)


def flat(size=16, y=64, block='minecraft:grass_block'):
    w = World('test')
    for x in range(size):
        for z in range(size):
            w.set((x, y, z), Cell(block))
    return w


def sample_map():
    return json.loads((SAMPLES / 'camp-map.json').read_text())


def plan(ops, locks=None, **extra):
    p = {'format': PLAN_FORMAT, 'id': 'test', 'world': 'test', 'author': 'unit', 'purpose': 'unit test',
         'locks': locks if locks is not None else [{'id': 'all', 'mode': 'allow', 'min': [-100, -64, -100], 'max': [100, 319, 100]}],
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

    def test_terrain_floors_build_each_column_from_its_own_height(self):
        op = {'op': 'terrain', 'origin': [0, 0], 'base': 64, 'floor': 60, 'surface': 'grass_block', 'under': 'dirt',
              'underDepth': 1, 'core': 'stone', 'heights': [[2, 2, 0]], 'floors': [[63, None, 62]]}
        ch = compile_plan(plan([op]), World('empty'))
        col = lambda x: sorted(y for (cx, y, _) in ch if cx == x)
        self.assertEqual(col(0), [63, 64, 65, 66])  # a shell: its own floor, not 60..62
        self.assertEqual(col(1), list(range(60, 67)))  # null floor: the op's floor
        self.assertEqual(col(2), [62, 63, 64])
        self.assertEqual(ch[(0, 66, 0)][1], Cell('minecraft:grass_block'))
        self.assertTrue(any('floors must have the same rows' in e for e in validate(plan([dict(op, floors=[[63, 62]])]))))
        self.assertTrue(any('every floor is an integer' in e for e in validate(plan([dict(op, floors=[[63, 'x', 62]])]))))

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
        m = sample_map()
        m['id'] = 'test'
        m['protect'] = [{'id': 'protect-spawn', 'kind': 'spawn', 'min': [1, 0, 1], 'max': [1, 100, 1], 'reason': 'spawn'}]
        self.assertIn('protect-spawn', dry_run(p, w, manifest=m)['violations'][0])
        p = plan([{'op': 'fill', 'min': [0, 65, 0], 'max': [3, 65, 0], 'block': 'stone'}], locks, limits={'maxChangedBlocks': 3})
        self.assertIn('over limits.maxChangedBlocks 3', dry_run(p, w)['violations'][0])
        self.assertIn('expectWorld is gone', validate(plan([{'op': 'set', 'pos': [1, 65, 1], 'block': 'stone'}], expectWorld='0' * 64))[0])
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


def ground(size=24, top=64, depth=5):
    w = World('test')
    for x in range(size):
        for z in range(size):
            for y in range(top - depth, top + 1):
                w.set((x, y, z), Cell('minecraft:grass_block' if y == top else 'minecraft:dirt'))
    return w


def tops(world, changes=None, scan=(40, 120)):
    """Ground top per column (air and water are not ground) of world with changes applied."""
    def get(pos):
        return changes[pos][1] if changes and pos in changes else world.get(pos)
    out = {}
    for x, _, z in list(world.blocks) + list(changes or {}):
        if (x, z) not in out:
            out[(x, z)] = next((y for y in range(scan[1], scan[0] - 1, -1)
                                if get((x, y, z)).state not in ('minecraft:air', 'minecraft:water')), None)
    return out


class TerrainHelperTest(unittest.TestCase):
    """noise, erode, breakup, wall, path and river (tools/mapforge/terrain.py)."""

    def test_helper_validation(self):
        bad = [({'op': 'noise', 'min': [0, 0], 'max': [3, 3], 'scale': 4}, 'noise needs amplitude'),
               ({'op': 'noise', 'min': [0, 0], 'max': [3, 3], 'amplitude': 1, 'scale': 0}, 'scale must be positive'),
               ({'op': 'erode', 'min': [5, 0], 'max': [3, 3]}, 'is above max'),
               ({'op': 'erode', 'min': [0, 0], 'max': [3, 3], 'mode': 'wind'}, 'mode must be thermal or smooth'),
               ({'op': 'breakup', 'min': [0, 0], 'max': [3, 3], 'replace': ['grass_block'], 'mix': [['gravel', 0]]}, 'mix'),
               ({'op': 'breakup', 'min': [0, 0], 'max': [3, 3], 'replace': ['grass_block'], 'mix': 'gravel',
                 'near': ['water']}, 'near needs width'),
               ({'op': 'wall', 'points': [[0, 0], [5, 0]], 'floor': 60, 'block': 'stone'}, 'give top'),
               ({'op': 'wall', 'points': [[0, 0]], 'top': 70, 'floor': 60, 'block': 'stone'}, 'at least two'),
               ({'op': 'path', 'points': [[0, 0], [5, 0]], 'width': 3, 'surface': 'dirt_path', 'colour': 1},
                "unknown key 'colour'"),
               ({'op': 'river', 'points': [[0, 0], [5, 0]], 'width': 3, 'water': 60, 'floor': 61}, 'floor is above water'),
               ({'op': 'river', 'points': [[0, 0], [5, 0]], 'width': 3, 'water': 60, 'floor': 55, 'exclude': [[0, 0]]},
                'exclude must be')]
        for op, needle in bad:
            errors = validate(plan([op]))
            self.assertTrue(any(needle in e for e in errors), (op, errors))
        good = {'op': 'river', 'points': [[0, 0, 4], [5, 0.5, 6]], 'width': 3, 'water': 60, 'floor': 55,
                'bankSurface': [['mud', 2], ['coarse_dirt', 1]], 'seed': 'x'}
        self.assertEqual(validate(plan([good])), [])
        sunk = {'op': 'noise', 'min': [0, 0], 'max': [3, 3], 'amplitude': 1, 'scale': 4, 'offset': -0.25}
        self.assertEqual(validate(plan([sunk])), [], 'a negative noise offset is a valid bias')

    def test_noise_is_seeded_clipped_clamped_and_fades_at_its_edge(self):
        w = ground()
        op = {'op': 'noise', 'min': [2, 2], 'max': [21, 21], 'amplitude': 4, 'scale': 8, 'octaves': 3, 'seed': 7,
              'falloff': 4, 'clamp': [62, 66]}
        a = compile_plan(plan([op]), w)
        self.assertEqual(a, compile_plan(plan([op]), w))
        self.assertNotEqual(a, compile_plan(plan([dict(op, seed=8)]), w))
        self.assertTrue(a)
        self.assertTrue(all(2 <= x <= 21 and 2 <= z <= 21 for x, _, z in a))
        t = tops(w, a)
        self.assertTrue(all(62 <= t[(x, z)] <= 66 for x in range(2, 22) for z in range(2, 22)))
        self.assertGreaterEqual(len({t[c] for c in t}), 3)  # not one flat level
        self.assertLessEqual(max(abs(t[(x, 2)] - 64) for x in range(2, 22)), 1)  # faded at the clip edge
        col = [y for y in range(50, 75) if (a[(10, y, 10)][1] if (10, y, 10) in a else w.get((10, y, 10))) != AIR_CELL]
        self.assertEqual(col, list(range(59, t[(10, 10)] + 1)))  # a raise or dig keeps the column solid

    def test_noise_only_and_exclude_skip_columns(self):
        w = ground()
        for z in range(24):
            w.set((5, 64, z), Cell('minecraft:stone_bricks'))
        op = {'op': 'noise', 'min': [0, 0], 'max': [23, 23], 'amplitude': 5, 'scale': 6, 'seed': 3,
              'only': ['grass_block'], 'exclude': [{'min': [10, 10], 'max': [23, 23]}]}
        ch = compile_plan(plan([op]), w)
        self.assertTrue(ch)
        self.assertFalse(any(x == 5 for x, _, _ in ch))
        self.assertFalse(any(x >= 10 and z >= 10 for x, _, z in ch))

    def test_erode_thermal_limits_slope_and_smooth_relaxes(self):
        w = ground()
        for y in range(65, 75):
            w.set((12, y, 12), Cell('minecraft:stone'))
        op = {'op': 'erode', 'min': [4, 4], 'max': [20, 20], 'iterations': 30, 'talus': 1.0, 'rate': 0.5,
              'jitter': 0.3, 'seed': 1}
        ch = compile_plan(plan([op]), w)
        t = tops(w, ch)
        self.assertLess(t[(12, 12)], 74)
        self.assertGreater(t[(13, 12)], 64)  # material slid onto the neighbours
        self.assertTrue(all(4 <= x <= 20 and 4 <= z <= 20 for x, _, z in ch))
        self.assertLessEqual(max(abs(t[(x, 12)] - t[(x + 1, 12)]) for x in range(4, 20)), 3)
        self.assertEqual(ch, compile_plan(plan([op]), w))
        sm = compile_plan(plan([dict(op, mode='smooth', iterations=3)]), w)
        self.assertLess(tops(w, sm)[(12, 12)], 74)
        self.assertEqual(compile_plan(plan([dict(op, only=['dirt'])]), w), {})

    def test_breakup_mixes_surfaces_near_a_boundary_and_on_faces(self):
        w = ground()
        for z in range(24):
            for y in (63, 64):
                w.set((12, y, z), Cell('minecraft:water'))
        op = {'op': 'breakup', 'min': [0, 0], 'max': [23, 23], 'replace': ['grass_block'],
              'mix': [['coarse_dirt', 1], ['gravel', 1], ['mud', 1]], 'density': 1.0, 'scale': 1,
              'near': ['water'], 'width': 3, 'seed': 4}
        ch = compile_plan(plan([op]), w)
        self.assertTrue(ch)
        self.assertTrue(all(abs(x - 12) <= 3 and y == 64 for x, y, _ in ch))
        self.assertGreater(len({after.state for _, after, _ in ch.values()}), 1)
        patches = compile_plan(plan([{'op': 'breakup', 'min': [0, 0], 'max': [23, 23], 'replace': ['grass_block'],
                                      'mix': 'coarse_dirt', 'density': 0.4, 'scale': 3, 'seed': 9}]), w)
        self.assertTrue(0 < len(patches) < 23 * 24)
        faces = compile_plan(plan([{'op': 'breakup', 'min': [0, 0], 'max': [23, 23], 'replace': ['dirt'],
                                    'mix': 'stone', 'density': 1.0, 'scale': 1, 'depth': 4, 'exposed': True}]), w)
        self.assertTrue(faces)
        inner = {(x, y) for x, y, z in faces if 0 < x < 23 and 0 < z < 23}  # the world's own edges are faces too
        self.assertEqual(inner, {(11, 63), (13, 63), (12, 62)})
        self.assertNotIn((5, 63, 5), faces)  # a buried block, not a face

    def test_wall_follows_points_with_thickness_top_and_merlons(self):
        w = ground()
        op = {'op': 'wall', 'points': [[2, 4, 70], [20, 4, 74]], 'thickness': 3, 'floor': 60, 'block': 'stone_bricks',
              'merlons': True}
        ch = compile_plan(plan([op]), w)
        self.assertEqual({z for _, _, z in ch}, {3, 4, 5})
        self.assertEqual(ch[(2, 70, 4)][1], Cell('minecraft:stone_bricks'))
        self.assertNotIn((2, 71, 4), ch)
        self.assertIn((20, 74, 4), ch)
        self.assertIn((3, 71, 3), ch)  # merlon on the edge
        self.assertNotIn((3, 71, 4), ch)
        bent = compile_plan(plan([{'op': 'wall', 'points': [[2, 2], [12, 2], [12, 12]], 'thickness': 1, 'top': 66,
                                   'floor': 65, 'block': 'stone'}]), w)
        self.assertEqual({(x, z) for x, _, z in bent}, {(x, 2) for x in range(2, 13)} | {(12, z) for z in range(2, 13)})

    def test_path_grades_a_causeway_and_blends_its_edges(self):
        w = ground()
        op = {'op': 'path', 'points': [[0, 12], [23, 12]], 'width': 3, 'falloff': 3, 'y': 68, 'surface': 'dirt_path',
              'under': 'cobblestone', 'edge': [['coarse_dirt', 1], ['gravel', 1]], 'edgeDensity': 1.0}
        ch = compile_plan(plan([op]), w)
        t = tops(w, ch)
        row = [t[(10, z)] for z in range(7, 18)]
        self.assertEqual((row[0], row[4], row[5], row[6], row[10]), (64, 68, 68, 68, 64))
        self.assertTrue(all(a <= b for a, b in zip(row[:5], row[1:6])))  # eases up to the core
        self.assertTrue(any(64 < h < 68 for h in row))
        self.assertEqual(ch[(10, 68, 12)][1], Cell('minecraft:dirt_path'))
        self.assertEqual(ch[(10, 66, 12)][1], Cell('minecraft:cobblestone'))
        road = compile_plan(plan([{k: v for k, v in op.items() if k not in ('y', 'falloff')}]), w)
        self.assertTrue(all(y == 64 for _, y, _ in road))
        self.assertEqual({z for _, _, z in road}, {11, 12, 13})

    def test_river_cuts_a_channel_with_cliff_or_sloped_banks(self):
        w = ground(depth=8)
        op = {'op': 'river', 'points': [[0, 12], [23, 12]], 'width': 7, 'depth': 3, 'water': 61, 'floor': 56,
              'bed': 'gravel'}
        ch = compile_plan(plan([op]), w)

        def get(pos):
            return ch[pos][1] if pos in ch else w.get(pos)
        self.assertEqual(get((5, 61, 12)), Cell('minecraft:water'))
        self.assertEqual(get((5, 64, 12)), AIR_CELL)
        self.assertEqual(get((5, 58, 12)), Cell('minecraft:gravel'))  # bed 3 below the water at the centre
        self.assertEqual(get((5, 59, 12)), Cell('minecraft:water'))
        self.assertEqual((get((5, 59, 9)), get((5, 60, 9))), (Cell('minecraft:gravel'), Cell('minecraft:water')))  # shallower at the edge
        self.assertEqual({z for _, _, z in ch}, set(range(9, 16)))  # a cut cliff: no bank outside the channel
        sloped = compile_plan(plan([dict(op, bank=3, bankSurface='mud', faces=True)]), w)
        t = tops(w, sloped)
        self.assertEqual([t[(5, z)] for z in (16, 17, 18, 19)], [62, 63, 64, 64])
        self.assertEqual(sloped[(5, 62, 16)][1], Cell('minecraft:mud'))
        clipped = compile_plan(plan([dict(op, min=[0, 0], max=[9, 23])]), w)
        self.assertTrue(clipped and all(x <= 9 for x, _, _ in clipped))
        wide = compile_plan(plan([dict(op, points=[[0, 12, 3], [23, 12, 11]])]), w)
        self.assertEqual({z for x, _, z in wide if x == 0}, set(range(11, 14)))

    def test_seal_closes_cliff_faces_in_hollow_columns_and_gaps_finds_them(self):
        from mapforge import gaps
        w = World('hollow')
        for x in range(6):
            top = 66 if x < 3 else 62  # a plateau of shell columns beside a low floor
            for y in (range(top - 1, top + 1) if x < 3 else [top]):
                w.set((x, y, 0), Cell('minecraft:grass_block' if y == top else 'minecraft:dirt'))
        self.assertEqual([p for p in gaps.find_gaps(w) if p[0] == 2], [(2, 63, 0), (2, 64, 0)])
        op = {'op': 'seal', 'min': [0, 0], 'max': [5, 0]}
        self.assertEqual(validate(plan([op])), [])
        self.assertTrue(any('seal needs min' in e for e in validate(plan([{'op': 'seal'}]))))
        ch = compile_plan(plan([op]), w)
        self.assertEqual({p for p in ch if p[0] == 2}, {(2, 63, 0), (2, 64, 0)})
        self.assertEqual(ch[(2, 64, 0)][1], Cell('minecraft:dirt'))  # the block under the column's top
        self.assertFalse(any(p[0] != 2 for p in ch))  # the low floor and the far columns are untouched
        after = w.copy()
        for pos, (_, cell, _) in ch.items():
            after.set(pos, cell)
        self.assertEqual(gaps.find_gaps(after), [])

    def test_helpers_obey_locks_protect_dry_run_and_rollback(self):
        w = ground()
        before = w.digest()
        ops = [{'op': 'noise', 'min': [0, 0], 'max': [23, 23], 'amplitude': 3, 'scale': 6, 'seed': 2},
               {'op': 'river', 'points': [[0, 12], [23, 12]], 'width': 5, 'water': 62, 'floor': 58, 'bank': 2},
               {'op': 'path', 'points': [[12, 0], [12, 23]], 'width': 3, 'falloff': 2, 'y': 65, 'surface': 'dirt_path'},
               {'op': 'wall', 'points': [[2, 2], [2, 20]], 'thickness': 2, 'top': 70, 'floor': 60, 'block': 'stone_bricks'},
               {'op': 'erode', 'min': [0, 0], 'max': [23, 23], 'iterations': 2, 'talus': 0.5},
               {'op': 'breakup', 'min': [0, 0], 'max': [23, 23], 'replace': ['grass_block'], 'mix': 'coarse_dirt',
                'density': 0.3}]
        locks = [{'id': 'zone', 'mode': 'allow', 'min': [0, 40, 0], 'max': [23, 100, 23]},
                 {'id': 'post', 'mode': 'deny', 'min': [12, 40, 12], 'max': [12, 100, 12], 'reason': 'station'}]
        s = dry_run(plan(ops, locks), w)
        self.assertFalse(s['ok'])
        self.assertTrue(any('deny region post (station)' in v for v in s['violations']))
        with self.assertRaises(PlanError):
            apply(plan(ops, locks), w)
        self.assertEqual(w.digest(), before)
        m = sample_map()
        m['protect'] = [{'id': 'ring', 'kind': 'capture', 'center': [5, 12], 'radius': 2, 'y': [55, 64], 'reason': 'ring'}]
        self.assertTrue(any('ring' in v for v in dry_run(plan(ops), w, manifest=m)['violations']))
        s = dry_run(plan(ops), w)
        self.assertTrue(s['ok'], s['violations'])
        self.assertEqual([o['op'] for o in s['operations']], ['noise', 'river', 'path', 'wall', 'erode', 'breakup'])
        self.assertTrue(all(o['changed'] > 0 and o['columns'] > 0 for o in s['operations']), s['operations'])
        self.assertIn('columns', format_summary(s))
        self.assertEqual(w.digest(), before)
        _, undo = apply(plan(ops), w, now=NOW)
        self.assertEqual(w.digest(), s['worldAfter'])
        self.assertEqual(rollback(undo, w), s['changed'])
        self.assertEqual(w.digest(), before)


class SampleTest(unittest.TestCase):
    def test_sample_batch_is_valid_deterministic_and_reversible(self):
        p = json.loads((SAMPLES / 'camp-watchpost.json').read_text())
        m = sample_map()
        self.assertEqual(validate(p, SAMPLES), [])
        self.assertEqual(mf.validate_manifest(m), [])
        w = flat(64)
        before = w.digest()
        s = dry_run(p, w, SAMPLES, m)
        self.assertTrue(s['ok'], s['violations'])
        self.assertTrue(all(op['changed'] > 0 for op in s['operations']))
        _, u1 = apply(p, w, SAMPLES, m, now=NOW)
        _, u2 = apply(p, flat(64), SAMPLES, m, now=NOW)
        self.assertEqual(u1, u2)
        self.assertEqual((u1['section'], u1['pass']), ('s0', 'blockout'))
        self.assertEqual(u1['sectionAfter'], mf.section_digests(m, w, {'s0'})['s0'])
        self.assertNotEqual(u1['sectionBefore'], u1['sectionAfter'])
        rollback(u1, w)
        self.assertEqual(w.digest(), before)

    def test_cli_round_trip_and_live_path_refusal(self):
        with tempfile.TemporaryDirectory() as d:
            world = pathlib.Path(d, 'camp.json')
            run = lambda *a: subprocess.run([sys.executable, str(ROOT / 'tools/mapforge'), *map(str, a)], capture_output=True, text=True)
            self.assertEqual(run('flat', '--out', world, '--min', 0, 0, '--max', 63, 63, '--y', 64).returncode, 0)
            digest = World.load(world).digest()
            r = run('dry-run', SAMPLES / 'camp-watchpost.json', '--world', world)
            self.assertEqual(r.returncode, 2)
            self.assertIn('--map', r.stdout)
            r = run('dry-run', SAMPLES / 'camp-watchpost.json', '--world', world, '--map', SAMPLES / 'camp-map.json')
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertIn('locks ok', r.stdout)
            self.assertEqual(World.load(world).digest(), digest)
            r = run('apply', SAMPLES / 'camp-watchpost.json', '--world', world, '--map', SAMPLES / 'camp-map.json')
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


class ManifestTest(unittest.TestCase):
    def sectioned(self, ops, **extra):
        p = plan(ops, locks=[], section='s0', world='camp', **{'pass': 'blockout'})
        p.update(extra)
        return p

    def test_manifest_validation(self):
        m = sample_map()
        self.assertEqual(mf.validate_manifest(m), [])
        bad = sample_map()
        bad['sections'][1]['bounds']['min'][0] = 60
        self.assertTrue(any('overlap' in e for e in mf.validate_manifest(bad)))
        bad = sample_map()
        bad['sections'][1]['bounds']['max'][0] = 200
        self.assertTrue(any('outside the map bounds' in e for e in mf.validate_manifest(bad)))
        bad = sample_map()
        bad['sections'][0]['status'] = 'painted'
        bad['protect'][2]['radius'] = 0
        bad['protect'][0]['kind'] = 'tree'
        errors = mf.validate_manifest(bad)
        self.assertTrue(any('status must be' in e for e in errors))
        self.assertTrue(any('cylinder' in e for e in errors))
        self.assertTrue(any('kind must be' in e for e in errors))
        self.assertTrue(mf.validate_manifest({'format': 'x'}))

    def test_section_bounds_are_the_allow_region_and_cut_plan_locks(self):
        m, w = sample_map(), flat(64)
        ok = self.sectioned([{'op': 'set', 'pos': [5, 65, 5], 'block': 'stone'}])
        self.assertEqual(validate(ok), [])
        self.assertTrue(dry_run(ok, w, manifest=m)['ok'])
        out = self.sectioned([{'op': 'set', 'pos': [70, 65, 5], 'block': 'stone'}])
        self.assertIn('outside every allow region', dry_run(out, w, manifest=m)['violations'][0])
        # the plan's own allow lock reaches past the section: only the overlap counts
        wide = self.sectioned([{'op': 'set', 'pos': [70, 65, 5], 'block': 'stone'}],
                              locks=[{'id': 'wide', 'mode': 'allow', 'min': [0, 0, 0], 'max': [127, 100, 63]}])
        self.assertIn('outside every allow region', dry_run(wide, w, manifest=m)['violations'][0])
        narrow = self.sectioned([{'op': 'set', 'pos': [40, 65, 5], 'block': 'stone'}],
                                locks=[{'id': 'n', 'mode': 'allow', 'min': [0, 0, 0], 'max': [10, 100, 10]}])
        self.assertIn('outside every allow region', dry_run(narrow, w, manifest=m)['violations'][0])
        away = self.sectioned([{'op': 'set', 'pos': [5, 65, 5], 'block': 'stone'}],
                              locks=[{'id': 'e', 'mode': 'allow', 'min': [70, 0, 0], 'max': [80, 100, 10]}])
        self.assertTrue(any('none of the plan' in v for v in dry_run(away, w, manifest=m)['violations']))
        self.assertIn('--map', dry_run(ok, w)['violations'][0])
        other = dict(ok, world='ironhold')
        self.assertTrue(any('is map' in v for v in dry_run(other, w, manifest=m)['violations']))
        unknown = dict(ok, section='zz')
        self.assertTrue(any('no section' in v for v in dry_run(unknown, w, manifest=m)['violations']))

    def test_protect_boxes_and_cylinders(self):
        m, w = sample_map(), flat(128)
        m['sections'][1]['status'] = 'todo'
        hit = plan([{'op': 'set', 'pos': [99, 65, 34], 'block': 'stone'}], locks=[], section='s1', world='camp',
                   **{'pass': 'blockout'})
        self.assertIn('deny region flag', dry_run(hit, w, manifest=m)['violations'][0])
        corner = dict(hit, operations=[{'op': 'set', 'pos': [101, 65, 37], 'block': 'stone'}])  # 5,5 from centre > r6
        self.assertTrue(dry_run(corner, w, manifest=m)['ok'])
        above = dict(hit, operations=[{'op': 'set', 'pos': [96, 68, 32], 'block': 'stone'}])
        self.assertTrue(dry_run(above, w, manifest=m)['ok'])
        station = self.sectioned([{'op': 'set', 'pos': [22, 65, 52], 'block': 'stone'}])
        self.assertIn('war-table', dry_run(station, w, manifest=m)['violations'][0])
        # sectionless plans still get the manifest's protect locks
        legacy = plan([{'op': 'set', 'pos': [22, 65, 52], 'block': 'stone'}], world='camp')
        self.assertIn('war-table', dry_run(legacy, w, manifest=m)['violations'][0])

    def test_pass_order_reopen_and_expect_section(self):
        m, w = sample_map(), flat(64)
        p = self.sectioned([{'op': 'set', 'pos': [5, 65, 5], 'block': 'stone'}], **{'pass': 'natural'})
        self.assertTrue(any('next pass (blockout)' in v for v in dry_run(p, w, manifest=m)['violations']))
        p['reopen'] = 'redo the ridge'
        self.assertTrue(dry_run(p, w, manifest=m)['ok'])
        self.assertIn('pass and reopen belong', validate(plan([{'op': 'set', 'pos': [0, 65, 0], 'block': 'stone'}], reopen='x'))[0])
        digest = mf.section_digests(m, w, {'s0'})['s0']
        p = self.sectioned([{'op': 'set', 'pos': [5, 65, 5], 'block': 'stone'}], expectSection=digest)
        self.assertTrue(dry_run(p, w, manifest=m)['ok'])
        w2 = w.copy()
        w2.set((80, 64, 5), Cell('minecraft:dirt'))  # another section changes: still fine
        self.assertTrue(dry_run(p, w2, manifest=m)['ok'])
        w2.set((6, 64, 6), Cell('minecraft:dirt'))  # this section changes: refused
        with self.assertRaises(PlanError) as e:
            apply(p, w2, manifest=m)
        self.assertIn('section s0 digest', e.exception.errors[0])
        self.assertEqual(w2.get((5, 65, 5)), AIR_CELL)

    def test_sign_status_and_drift(self):
        m, w = sample_map(), flat(64)
        with self.assertRaises(mf.ManifestError):
            mf.sign(m, 's0', 'structure', w, 'agent')  # blockout comes first
        r = mf.sign(m, 's0', 'blockout', w, 'agent', 'changes', 'walls too boxy', now=NOW)
        self.assertEqual((r['verdict'], m['sections'][0]['status']), ('changes', 'todo'))
        r = mf.sign(m, 's0', 'blockout', w, 'agent', images=['a.png'], plans=['p.json'], now=NOW)
        self.assertEqual((m['sections'][0]['status'], r['at'], r['images']), ('blockout', '2026-10-30T00:00:00Z', ['a.png']))
        self.assertEqual(mf.validate_manifest(m), [])
        r = mf.sign(m, 's0', 'blockout', w, 'agent', notes='re-review after a reopen plan', now=NOW)
        self.assertEqual((r['verdict'], m['sections'][0]['status'], len(m['sections'][0]['reviews'])), ('pass', 'blockout', 3))
        with self.assertRaises(mf.ManifestError):
            mf.sign(m, 's0', 'natural', w, 'agent')  # structure comes first
        rows = {r['id']: r for r in mf.status_rows(m, w)}
        self.assertFalse(rows['s0']['drift'])
        self.assertIsNone(rows['s1']['drift'])
        w.set((3, 70, 3), Cell('minecraft:stone'))
        self.assertTrue({r['id']: r for r in mf.status_rows(m, w)}['s0']['drift'])
        self.assertIn('DRIFT', mf.format_status(m, mf.status_rows(m, w)))
        with self.assertRaises(mf.ManifestError):
            mf.sign_map(m, w, 'Kort')
        for s in m['sections']:
            s['status'] = 'done'
        self.assertEqual(mf.sign_map(m, w, 'Kort', now=NOW)['world'], w.digest())

    def test_cli_status_sign_and_render_section(self):
        with tempfile.TemporaryDirectory() as d:
            d = pathlib.Path(d)
            run = lambda *a: subprocess.run([sys.executable, str(ROOT / 'tools/mapforge'), *map(str, a)], capture_output=True, text=True)
            world, man = d / 'camp.json', d / 'camp-map.json'
            man.write_text((SAMPLES / 'camp-map.json').read_text())
            self.assertEqual(run('flat', '--out', world, '--min', 0, 0, '--max', 127, 63, '--y', 64).returncode, 0)
            r = run('apply', SAMPLES / 'camp-watchpost.json', '--world', world, '--map', man)
            self.assertEqual(r.returncode, 0, r.stderr)
            r = run('sign', man, 's0', '--pass', 'blockout', '--world', world, '--by', 'unit', '--notes', 'ok')
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertIn('status blockout', r.stdout)
            r = run('sign', man, 's0', '--pass', 'natural', '--world', world, '--by', 'unit')
            self.assertEqual(r.returncode, 2)  # structure comes first
            r = run('status', man, '--world', world, '--digests')
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertIn('s0     blockout', r.stdout)
            self.assertIn('digest s1 ', r.stdout)
            self.assertNotIn('DRIFT', r.stdout)
            r = run('sign', man, '--whole', '--world', world, '--by', 'Kort')
            self.assertEqual(r.returncode, 2)
            if HAVE_PIL:
                r = run('render-section', man, 's0', '--world', world, '--out-dir', d / 'img')
                self.assertEqual(r.returncode, 0, r.stderr)
                self.assertTrue((d / 'img/camp-s0-top.png').exists() and (d / 'img/camp-s0-iso.png').exists())


class ColourTableTest(unittest.TestCase):
    def test_table_covers_every_block_in_the_shipped_structures(self):
        palette = Palette()
        names = {normalize(st).split('[')[0] for path in PIECES.rglob('*.nbt') for st in Structure.load(path).palette}
        missing = sorted(n for n in names if n not in palette and n not in render.INVISIBLE)
        self.assertEqual(missing, [])
        self.assertGreater(len(palette.blocks), 900)
        self.assertEqual(palette.get('gravesandglory:nonsense')[0], UNKNOWN)
        self.assertEqual(palette.unknown, {'gravesandglory:nonsense': 1})

    def test_state_properties_shape_the_look(self):
        looks = render.Looks()
        log_y, log_x = looks.get('minecraft:oak_log[axis=y]'), looks.get('minecraft:oak_log[axis=x]')
        self.assertEqual((log_y[0], log_y[1]), (log_x[1], log_x[0]))  # on its side the bark is on top
        self.assertEqual(looks.get('minecraft:stone_slab[type=bottom]')[2][1:5:3], (0, 8))
        self.assertEqual(looks.get('minecraft:stone_slab[type=top]')[2][1:5:3], (8, 16))
        self.assertTrue(looks.get('minecraft:stone_slab[type=double]')[3])
        self.assertEqual(looks.get('minecraft:snow[layers=3]')[2][4], 6)
        self.assertIsNone(looks.get('minecraft:barrier'))


@unittest.skipUnless(HAVE_PIL, 'render needs Pillow')
class RenderTest(unittest.TestCase):
    CAPTION = 8 + 18 * 2

    def test_topdown_colour_grid_crop_and_height_shading(self):
        w = flat(32)
        w.set((20, 65, 20), Cell('minecraft:grass_block'))
        grass = Palette().get('minecraft:grass_block')[0]
        img = render.render(w, 'top', scale=4)
        margin = 30
        self.assertEqual(img.height, 32 * 4 + margin + self.CAPTION)  # the caption may widen a small image
        px = lambda x, z: img.getpixel((margin + x * 4 + 2, self.CAPTION + margin + z * 4 + 2))
        self.assertEqual(px(5, 5), grass)
        self.assertGreater(sum(px(20, 20)), sum(px(5, 5)))  # higher and lit from the north-west
        self.assertNotEqual(img.getpixel((margin + 16 * 4, self.CAPTION + margin + 5 * 4 + 1)), grass)  # grid line x=16
        crop = render.render(w, 'top', lo=(8, 8), hi=(15, 11), scale=2, grid=False)
        self.assertEqual(crop.size[1] - self.CAPTION, 4 * 2)

    def test_unknown_blocks_are_magenta_and_reported(self):
        w = flat(4)
        w.set((1, 64, 1), Cell('gravesandglory:mystery_block'))
        palette = Palette()
        img = render.render(w, 'top', scale=8, grid=False, palette=palette)
        self.assertEqual(img.getpixel((1 * 8 + 4, self.CAPTION + 1 * 8 + 4)), UNKNOWN)
        lo, hi = render.crop_box([w])
        self.assertEqual(render.unknown_blocks(w, lo, hi, palette), {'gravesandglory:mystery_block': 1})

    def test_isometric_top_face_and_size(self):
        w = World('one')
        w.set((0, 64, 0), Cell('minecraft:stone'))
        img = render.render(w, 'iso', scale=8)
        self.assertEqual(img.getpixel((20, 16 + self.CAPTION)), Palette().get('minecraft:stone')[0])
        tower = flat(8)
        for y in range(65, 75):
            tower.set((3, y, 3), Cell('minecraft:stone_bricks'))
        self.assertGreater(render.render(tower, 'iso', scale=4).height, render.render(flat(8), 'iso', scale=4).height)

    def test_diff_kinds_and_undo_rebuild(self):
        before = flat(8)
        after = before.copy()
        after.set((1, 65, 1), Cell('minecraft:stone'))       # added, surface rose
        after.set((2, 64, 2), AIR_CELL)                      # removed, surface fell
        after.set((3, 64, 3), Cell('minecraft:gravel'))      # changed, same height
        lo, hi = render.crop_box([before, after])
        cells = render.diff_cells(before, after, lo, hi)
        self.assertEqual(cells, {(1, 65, 1): 'added', (2, 64, 2): 'removed', (3, 64, 3): 'changed'})
        cols = render.column_kinds(before, after, cells, render.Looks(), lo, hi)
        self.assertEqual(cols, {(1, 1): 'added', (2, 2): 'removed', (3, 3): 'changed'})
        img = render.render(after, 'top', scale=8, grid=False, before=before)
        self.assertEqual(img.getpixel((2 * 8 + 4, self.CAPTION + 18 + 2 * 8 + 4)),
                         render._mix(render.BACKGROUND, render.KINDS['removed'], 0.62))
        render.render(after, 'iso', scale=4, before=before)
        w = flat(8)
        _, undo = apply(plan([{'op': 'fill', 'min': [0, 64, 0], 'max': [2, 64, 2], 'block': 'stone'}]), w, now=NOW)
        b, a = render.worlds_from_undo(w, undo)
        self.assertEqual((b.digest(), a.digest()), (undo['worldBefore'], undo['worldAfter']))
        rollback(undo, w)
        b2, a2 = render.worlds_from_undo(w, undo)  # works from either side of the change
        self.assertEqual((b2.digest(), a2.digest()), (undo['worldBefore'], undo['worldAfter']))

    def test_cli_render_apply_render_and_refusals(self):
        with tempfile.TemporaryDirectory() as d:
            d = pathlib.Path(d)
            run = lambda *a: subprocess.run([sys.executable, str(ROOT / 'tools/mapforge'), *map(str, a)], capture_output=True, text=True)
            world = d / 'camp.json'
            self.assertEqual(run('flat', '--out', world, '--min', 0, 0, '--max', 63, 63, '--y', 64).returncode, 0)
            r = run('apply', SAMPLES / 'camp-watchpost.json', '--world', world, '--render', '--map', SAMPLES / 'camp-map.json')
            self.assertEqual(r.returncode, 0, r.stderr)
            stem = 'camp.camp-watchpost-sample.undo'
            for suffix in ('before-top', 'after-top', 'diff-top', 'before-iso', 'after-iso', 'diff-iso'):
                self.assertTrue((d / f'{stem}.{suffix}.png').exists(), suffix)
            for extra in ((), ('--view', 'iso'), ('--undo', d / f'{stem}.json'), ('--min', 0, 60, 0, '--max', 20, 70, 20)):
                r = run('render', world, '--out', d / 'r.png', '--report-unknown', *extra)
                self.assertEqual(r.returncode, 0, r.stderr)
                self.assertIn('unknown blocks (magenta): none', r.stdout)
            self.assertNotEqual(run('render', world, '--out', d / 'r.jpg').returncode, 0)
            save = d / 'save'
            save.mkdir()
            (save / 'level.dat').write_bytes(b'')
            r = run('render', world, '--out', save / 'r.png')
            self.assertNotEqual(r.returncode, 0)
            self.assertIn('offline only', r.stderr + r.stdout)
            self.assertNotEqual(run('render', world, '--out', d / 'r.png', '--min', 1, '--max', 2).returncode, 0)


class PillowMissingTest(unittest.TestCase):
    def test_clear_error_without_pillow(self):
        saved = {k: v for k, v in sys.modules.items() if k == 'PIL' or k.startswith('PIL.')}
        try:
            for k in saved:
                del sys.modules[k]
            sys.modules['PIL'] = None
            with self.assertRaisesRegex(render.RenderError, 'needs Pillow'):
                render.require_pillow()
        finally:
            del sys.modules['PIL']
            sys.modules.update(saved)


if __name__ == '__main__':
    unittest.main()
