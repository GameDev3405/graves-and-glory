"""Compare fixtures (tools/compare_templates.py): the id mapping, the 12 test-only templates, and the release jar guard.
Needs no maps checkout: it reads the committed fixtures; `python tools/compare_templates.py --check` re-derives them."""
import json, pathlib, sys, tempfile, unittest, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import compare_templates as ct
from mapforge.structure import Structure
from release_config import validate_player_jar

SHIPPED = ROOT / 'src' / 'main' / 'resources' / 'data' / 'gravesandglory' / 'structure'


class CompareFixtures(unittest.TestCase):
    def test_variants_map_to_the_base_build(self):
        want = {'trebuchet': 'trebuchet', 'boulder_large_mossy': 'boulder', 'boulder_small_dark': 'boulder', 'supply_wagon': 'supply_wagon_hay',
                'supply_wagon_hay': 'supply_wagon_hay', 'supply_wagon_barrels': 'supply_wagon_hay', 'supply_wagon_covered': 'supply_wagon_hay',
                'supply_wagon_shot': 'supply_wagon_shot', 'market_stall': 'market_stall', 'market_stall_fish': 'market_stall',
                'giant_bone_arch': 'giant_rib', 'giant_ribs': 'giant_rib', 'smoke_vent_thick': 'smoke_vent', 'tent_soldier': 'tent_soldier',
                'tent_pavilion': 'tent_pavilion', 'weapon_rack': 'weapon_rack', 'brazier': 'brazier', 'portcullis': 'portcullis',
                'handcart': None, 'handcart_ore': None, 'brazier_wall': None, 'portcullis_raised': None, 'anvil': None, 'forge': None}
        for pid, fixture in want.items():
            self.assertEqual(ct.fixture_for(pid), fixture, pid)
        paired, alone = ct.pairs_for(['boulder_small_grey', 'handcart', 'portcullis_raised', 'brazier'])
        self.assertEqual(paired, [('boulder_small_grey', 'compare/boulder'), ('brazier', 'compare/brazier')])
        self.assertEqual(alone, ['handcart', 'portcullis_raised'])

    def test_twelve_fixtures_are_real_and_not_shipped(self):
        self.assertEqual(len(ct.FIXTURES), 12)
        for name in ct.FIXTURES:
            path = ct.OUT / (name + '.nbt')
            self.assertTrue(path.is_file(), path)
            s = Structure.load(path)
            self.assertGreater(len(s.blocks), 20, name)
            self.assertTrue(any(y > 0 for (_, y, _), _, _ in s.blocks), name + ' has nothing above its ground patch')
            self.assertFalse((SHIPPED / ct.GROUP / (name + '.nbt')).exists(), name + ' must not be in the shipped structures')
        self.assertFalse((SHIPPED / ct.GROUP).exists())

    def test_prop_shot_finds_fixtures(self):
        import prop_shot
        self.assertEqual(prop_shot.compare_pairs(['boulder_small_grey=compare/boulder'], ['boulder_small_grey']), [('boulder_small_grey', 'compare/boulder')])
        self.assertEqual(prop_shot.compare_pairs(['x=compare/nothing'], ['x']), [])

    def test_release_guard_refuses_the_fixtures(self):
        expected = {'modId': 'gravesandglory', 'version': '0.3.1', 'sourceSha256': 'expected'}
        with tempfile.TemporaryDirectory() as root:
            p = pathlib.Path(root) / 'player.jar'
            with zipfile.ZipFile(p, 'w') as z:
                z.writestr('fabric.mod.json', json.dumps({'id': 'gravesandglory', 'version': '0.3.1', 'entrypoints': {'main': ['a.B'], 'client': ['a.C']}}))
                z.writestr('gravesandglory-build.json', json.dumps(expected))
                z.writestr('a/B.class', b'x'); z.writestr('a/C.class', b'x')
                z.writestr('data/gravesandglory/structure/compare/trebuchet.nbt', b'x')
            with self.assertRaises(RuntimeError):
                validate_player_jar(p, expected)


if __name__ == '__main__': unittest.main(verbosity=2)
