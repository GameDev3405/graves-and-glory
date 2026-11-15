"""Prop kit: no registry entries in prop code, vanilla effect ids only (plan task S4)."""
import json, pathlib, re, unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / 'src' / 'main' / 'java' / 'gg' / 'gravesandglory'
DEFS = ROOT / 'src' / 'main' / 'resources' / 'data'
FORBIDDEN = [
    (re.compile(r'Registry\s*\.\s*register'), 'Registry.register'),
    (re.compile(r'Registries\s*\.[^;]*register', re.S), 'Registries...register'),
    (re.compile(r'FabricBlockEntityTypeBuilder'), 'FabricBlockEntityTypeBuilder'),
    (re.compile(r'SoundEvent\s*\.\s*of\s*\(\s*(?:new\s+Identifier|Identifier\s*\.\s*of)\s*\(\s*"gravesandglory"'), 'SoundEvent.of(new Identifier / Identifier.of("gravesandglory"'),
]


def prop_sources():
    return sorted(SRC.glob('Prop*.java')) + [SRC / 'BuildHandbook.java']


class PropNoRegistry(unittest.TestCase):
    def test_props_class_exists(self):
        self.assertTrue((SRC / 'Props.java').is_file(), 'Props.java (S4) is missing')

    def test_no_registry_entries(self):
        files = prop_sources()
        self.assertTrue((SRC / 'BuildHandbook.java').is_file())
        self.assertIn(SRC / 'PropPack.java', files, 'PropPack.java (S7) is missing')
        for f in files:
            text = f.read_text(encoding='utf8')
            for pattern, name in FORBIDDEN:
                with self.subTest(file=f.name, rule=name):
                    self.assertIsNone(pattern.search(text), f'{f.name} uses {name}')

    def test_effect_ids_vanilla(self):
        defs = sorted(DEFS.glob('*/gg_props/*.json'))
        self.assertTrue(defs, 'no gg_props defs found')
        for f in defs:
            for e in json.loads(f.read_text(encoding='utf8')).get('effects', []):
                with self.subTest(file=f.name, id=e.get('id')):
                    self.assertTrue(str(e.get('id', '')).startswith('minecraft:'), f'{f.name}: effect id {e.get("id")} is not vanilla')


if __name__ == '__main__':
    unittest.main()
