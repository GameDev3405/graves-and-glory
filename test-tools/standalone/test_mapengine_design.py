"""Checks tools/mapengine design file schema, validation, save/load and the `new` command. Pure: no Minecraft."""
import copy
import json
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from mapengine import cli, design  # noqa: E402
from mapengine.design import DesignError, build_problems, new_design, next_id, validate  # noqa: E402


def obj(d, **kw):
    d['objects'].append(kw)
    return d


def base(**kw):
    o = {'by': 'kort', 'state': 'accepted'}
    o.update(kw)
    return o


class DesignTest(unittest.TestCase):
    def test_new_design_is_valid(self):
        d = new_design('t', 'T')
        self.assertEqual(validate(d), [])
        self.assertEqual((d['width'], d['depth'], d['baseY'], d['objects']), (700, 500, 64, []))

    def test_bad_outlines(self):
        d = new_design('t', 'T')
        d['objects'].append(base(id='hill-1', kind='hill', outline=[[0, 0], [5, 5]], height=10, steepness=0.5))
        msgs = validate(d)
        self.assertEqual(len(msgs), 1)
        self.assertIn('hill-1', msgs[0])
        d['objects'][0]['outline'] = [[0, 0], [10, 10], [10, 0], [0, 10]]
        self.assertEqual(validate(d), [])

    def test_out_of_bounds(self):
        d = new_design('t', 'T')
        d['objects'].append(base(id='spawn-1', kind='spawn', team='dawn', x=700, z=10))
        self.assertTrue(any('spawn-1' in m for m in validate(d)))
        d = new_design('t', 'T')
        d['objects'].append(base(id='road-1', kind='road', path=[[-50, 10], [100, 10]], width=5))
        self.assertEqual(validate(d), [])

    def test_duplicate_ids(self):
        d = new_design('t', 'T')
        for _ in range(2):
            d['objects'].append(base(id='point-1', kind='point', name='A', x=10, z=10, radius=8))
        self.assertTrue(any('duplicate' in m for m in validate(d)))

    def test_value_ranges(self):
        d = new_design('t', 'T')
        d['objects'] += [
            base(id='hill-1', kind='hill', outline=[[0, 0], [9, 0], [9, 9]], height=121, steepness=0.5),
            base(id='hill-2', kind='hill', outline=[[0, 0], [9, 0], [9, 9]], height=5, steepness=1.5),
            base(id='river-1', kind='river', path=[[0, 0], [9, 9]], width=65, depth=13),
            base(id='road-1', kind='road', path=[[0, 0], [9, 9]], width=17),
            base(id='point-1', kind='point', name='A', x=10, z=10, radius=1),
            base(id='spawn-1', kind='spawn', team='red', x=10, z=10),
            base(id='zone-1', kind='zone', outline=[[0, 0], [9, 0], [9, 9]], palette='nope'),
            base(id='blob-1', kind='blob'),
        ]
        msgs = validate(d)
        for oid in ('hill-1', 'hill-2', 'river-1', 'road-1', 'point-1', 'spawn-1', 'zone-1', 'blob-1'):
            self.assertTrue(any(oid in m for m in msgs), oid)
        d['format'] = 2
        self.assertTrue(any('format' in m for m in validate(d)))

    def test_by_and_state(self):
        d = new_design('t', 'T')
        d['objects'].append(base(id='point-1', kind='point', name='A', x=1, z=1, radius=8, by='bob', state='maybe'))
        self.assertEqual(len(validate(d)), 2)

    def test_build_needs_spawns_and_points(self):
        text = ' '.join(build_problems(new_design('t', 'T')))
        for word in ('dawn', 'dusk', 'point'):
            self.assertIn(word, text)

    def test_save_load_roundtrip_stable(self):
        d = new_design('t', 'T')
        d['objects'].append(base(id='point-1', kind='point', name='A', x=10, z=10, radius=8))
        with tempfile.TemporaryDirectory() as tmp:
            p, q = pathlib.Path(tmp) / 'a.json', pathlib.Path(tmp) / 'b.json'
            design.save(p, d)
            design.save(q, d)
            self.assertEqual(p.read_bytes(), q.read_bytes())
            self.assertTrue(p.read_bytes().endswith(b'\n'))
            self.assertEqual(design.load(p), d)
            bad = copy.deepcopy(d)
            bad['format'] = 9
            p.write_text(json.dumps(bad))
            with self.assertRaises(DesignError):
                design.load(p)

    def test_next_id(self):
        d = new_design('t', 'T')
        for n in (1, 3):
            d['objects'].append(base(id=f'hill-{n}', kind='hill'))
        self.assertEqual(next_id(d, 'hill'), 'hill-4')
        self.assertEqual(next_id(d, 'river'), 'river-1')

    def test_validate_malformed_shapes(self):
        self.assertEqual(validate([]), ['design: not an object'])
        d = new_design('t', 'T')
        d['objects'] = [5, None]
        self.assertIn('object #0: not an object', validate(d))
        self.assertIn('object #1: not an object', validate(d))
        d['objects'] = 'x'
        self.assertTrue(validate(d))

    def test_cli_new(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(cli.main(['new', 'foo', 'Foo Map'], root=pathlib.Path(tmp)), 0)
            d = design.load(pathlib.Path(tmp) / 'maps' / 'foo' / 'design.json')
            self.assertEqual(d['name'], 'Foo Map')
            self.assertEqual(cli.main(['new', 'foo', 'Foo'], root=pathlib.Path(tmp)), 1)
            self.assertEqual(cli.main(['check', 'foo'], root=pathlib.Path(tmp)), 1)  # no spawns: cannot build


if __name__ == '__main__':
    unittest.main()
