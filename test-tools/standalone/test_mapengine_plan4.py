"""Checks map engine Plan 4, the building library: index, schema, pad, stamp, checker faults and the AI handoff."""
import copy
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import build as mb  # noqa: E402
from mapengine import buildings, design, handoff, library  # noqa: E402
from mapengine import check as mc  # noqa: E402
from mapengine.terrain import heightfield  # noqa: E402
from mapforge.structure import Structure, rotate_state, to_world  # noqa: E402

HOUSE = 'kit/house_a'  # 15 x 13, a real door on the east side (local cell 13, 6)


def obj(kind, i, by='kort', state='accepted', **kw):
    return dict(kw, id=f'{kind}-{i}', kind=kind, by=by, state=state)


def base(*extra):
    d = design.new_design('t4', 'T4', 128, 64)
    d['objects'] = [obj('spawn', 1, team='dawn', x=5, z=5), obj('spawn', 2, team='dusk', x=120, z=5),
                    obj('point', 1, name='A', x=64, z=55, radius=6),
                    obj('hill', 1, outline=[[30, 20], [50, 20], [50, 40], [30, 40]], height=3, steepness=0.2)]
    d['objects'] += list(extra)
    return d


def house(i, x, z, rotation=0, **kw):
    return obj('building', i, piece=HOUSE, x=x, z=z, rotation=rotation, **kw)


def codes(d, f=None):
    return sorted(c for c, _, _ in buildings.issues(d, f or heightfield(d)))


class Library(unittest.TestCase):
    def test_index_current(self):
        self.assertEqual(library.INDEX_PATH.read_bytes(), library.index_bytes(library.generate()),
                         'library.json is stale: run python tools/mapengine library')

    def test_entries(self):
        lib = library.load()
        self.assertEqual(len(lib), len(list(library.PIECES.rglob('*.nbt'))))
        e = lib[HOUSE]
        self.assertEqual((e['group'], e['footprint'], e['door'], e['doorSpan']), ('kit', [15, 13], 'east', [6, 6]))
        self.assertTrue(all(v['door'] in library.DOORS for v in lib.values()))


class Schema(unittest.TestCase):
    def test_building_schema(self):
        self.assertEqual(design.validate(base(house(1, 80, 30))), [])
        bad = base(obj('building', 1, piece='kit/nope', x=80, z=30, rotation=45),
                   obj('building', 2, piece=HOUSE, x=80.5, z=30, rotation=0))
        msgs = ' '.join(design.validate(bad))
        for want in ('building-1: piece', 'building-1: rotation', 'building-2: x and z'):
            self.assertIn(want, msgs)


class Footprint(unittest.TestCase):
    def test_box_and_door_turn(self):
        self.assertEqual(buildings.box(house(1, 80, 30)), (73, 24, 87, 36))
        self.assertEqual(buildings.box(house(1, 80, 30, 90)), (74, 23, 86, 37))
        self.assertEqual(buildings.door(house(1, 80, 30)), ('east', [(88, 30)]))
        self.assertEqual(buildings.door(house(1, 80, 30, 90))[0], 'south')
        self.assertEqual(buildings.door(house(1, 80, 30, 90))[1], [(80, 38)])

    def test_pad_levels_footprint(self):
        d = base(house(1, 40, 30))  # on the hill's ramp
        f = heightfield(d)
        x0, z0, x1, z1 = buildings.box(d['objects'][-1])
        self.assertEqual(set(f.height[z0:z1 + 1, x0:x1 + 1].ravel().tolist()), {int(f.height[30, 40])})


class Checker(unittest.TestCase):
    def test_clean(self):
        self.assertEqual(codes(base(house(1, 80, 30), house(2, 100, 30, 180))), [])

    def test_faults(self):
        d = base(house(1, 80, 30),
                 house(2, 95, 30),          # its west edge covers house-1's door front: overlap free, door blocked
                 house(3, 120, 30),         # runs off the east edge
                 house(4, 101, 30),         # overlaps house-2
                 house(5, 6, 6))            # the dawn spawn stands inside it
        c = codes(d)
        self.assertIn('door_blocked', c)
        self.assertIn('building_bounds', c)
        self.assertIn('building_overlap', c)
        self.assertIn('building_point', c)
        blocked = [i for k, i, _ in buildings.issues(d, heightfield(d)) if k == 'door_blocked']
        self.assertIn('building-1', blocked)


class Build(unittest.TestCase):
    def test_stamp_rotated_and_deterministic(self):
        d = base(house(1, 80, 30, 90))
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            m = mb.build(d, a)
            mb.build(copy.deepcopy(d), b)
            for t in m['tiles']:
                self.assertEqual((pathlib.Path(a) / t['file']).read_bytes(), (pathlib.Path(b) / t['file']).read_bytes())
            self.assertEqual((pathlib.Path(a) / 'map.json').read_bytes(), (pathlib.Path(b) / 'map.json').read_bytes())
            self.assertEqual([(p['piece'], p['rotation']) for p in m['buildings']], [(HOUSE, 90)])
            piece = Structure.load(library.piece_path(HOUSE))
            local = (13, 1, 6)
            state = next(s for p, s, _ in piece.cells() if p == local)
            wx, _, wz = to_world(piece.size, (80, 0, 30), 90, local)
            floor = m['buildings'][0]['y'] - d['baseY']
            t = next(t for t in m['tiles'] if t['x'] <= wx < t['x'] + t['size'][0] and t['z'] <= wz < t['z'] + t['size'][2])
            tile = Structure.load(pathlib.Path(a) / t['file'])
            want = (wx - t['x'], floor + 1 - mb.FLOOR, wz - t['z'])
            got = [tile.palette[i] for p, i, _ in tile.blocks if tuple(p) == want]
            self.assertEqual(got, [rotate_state(state, 90)])
            self.assertIn('facing=north', got[0])  # west turned clockwise
            issues = mc.check(d, a)
            self.assertEqual([i for i in issues if i.code in ('columns', 'moved', 'stale') or i.code.startswith(('building', 'door'))], [])


class Handoff(unittest.TestCase):
    def test_export_lists_library_and_buildings(self):
        d = base(house(1, 80, 30))
        with tempfile.TemporaryDirectory() as out:
            handoff.export(d, out)
            brief = (pathlib.Path(out) / 'BRIEF.md').read_text(encoding='utf-8')
        self.assertIn('`kit/house_a` | 15 x 13', brief)
        self.assertIn('`building-1` building', brief)

    def test_import_locks_buildings(self):
        d = base(house(1, 80, 30))
        draft = copy.deepcopy(d)
        draft['objects'][-1]['rotation'] = 180
        draft['objects'].append(house(2, 100, 30, by='kort', state='accepted'))
        merged, rep = handoff.merge(d, draft)
        self.assertEqual(rep['added'], ['building-2'])
        b = {o['id']: o for o in merged['objects']}
        self.assertEqual(b['building-1']['rotation'], 0)
        self.assertEqual((b['building-2']['by'], b['building-2']['state']), ('ai', 'proposed'))
        self.assertTrue(any(r.startswith('building-1: locked') for r in rep['refused']))


if __name__ == '__main__':
    unittest.main()
