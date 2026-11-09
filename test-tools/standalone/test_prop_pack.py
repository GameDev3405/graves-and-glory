"""Prop server resource pack (prop kit plan task S2, tools/build_prop_pack.py): placeholders when art is missing, art used when present,
paper keeps its vanilla look, CMD clash and element range refusals, byte-reproducible zips, pack_format 15. Builds only into temp dirs."""
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOL = ROOT / 'tools' / 'build_prop_pack.py'
DEFS = ROOT / 'src' / 'main' / 'resources' / 'data' / 'gravesandglory' / 'gg_props'
PNG = bytes.fromhex('89504e470d0a1a0a0000000d4948445200000001000000010806000000'
                    '1f15c4890000000d4944415478da63f8cfc0f01f0005000201a3e1e2a10000000049454e44ae426082')


def part_models():
    out = []
    for f in sorted(DEFS.glob('*.json')):
        d = json.loads(f.read_text(encoding='utf8'))
        out += [(p['cmd'], p['model'], d['id']) for p in d['parts']]
    return sorted(out)


class PropPack(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = pathlib.Path(self.tmp.name)
        self.empty_art = self.dir / 'no-art'
        self.no_zip = self.dir / 'missing-excalibur.zip'

    def tearDown(self):
        self.tmp.cleanup()

    def build(self, out='out', art=None, excalibur=None):
        out = self.dir / out
        cmd = [sys.executable, str(TOOL), '--out', str(out), '--art', str(art or self.empty_art),
               '--excalibur', str(excalibur or self.no_zip)]
        r = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
        return r, out

    def pack(self, out):
        sha = (out / 'gg-props-latest.sha1').read_text().strip()
        self.assertRegex(sha, r'^[0-9a-f]{40}$')
        z = out / ('gg-props-' + sha[:8] + '.zip')
        self.assertTrue(z.exists(), z)
        return zipfile.ZipFile(z)

    def art_fixture(self, element_from=(0, 0, 0)):
        art = self.dir / 'art'
        (art / 'anvil').mkdir(parents=True)
        model = {'textures': {'0': 'anvil', 'particle': 'anvil'},
                 'elements': [{'from': list(element_from), 'to': [16, 10, 16],
                               'faces': {'up': {'uv': [0, 0, 16, 16], 'texture': '#0'}}}]}
        (art / 'anvil' / 'anvil.json').write_text(json.dumps(model))
        (art / 'anvil' / 'anvil.png').write_bytes(PNG)
        return art

    def test_placeholder_when_art_missing(self):
        r, out = self.build()
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        parts = part_models()
        self.assertIn('placeholders: %d' % len(parts), r.stdout)
        with self.pack(out) as z:
            names = set(z.namelist())
            paper = json.loads(z.read('assets/minecraft/models/item/paper.json'))
            overrides = paper['overrides']
            self.assertEqual([o['predicate']['custom_model_data'] for o in overrides], [c for c, _, _ in parts])
            self.assertEqual([o['model'] for o in overrides], ['gravesandglory:prop/' + m for _, m, _ in parts])
            for _, m, pid in parts:
                self.assertIn('assets/gravesandglory/models/prop/%s.json' % m, names)
                self.assertIn('assets/gravesandglory/textures/prop/%s.png' % pid, names)
                model = json.loads(z.read('assets/gravesandglory/models/prop/%s.json' % m))
                self.assertTrue(model['elements'], m)
                self.assertEqual(model['textures']['0'], 'gravesandglory:prop/' + pid)
                for e in model['elements']:
                    for v in e['from'] + e['to']:
                        self.assertTrue(-16 <= v <= 32, (m, e))

    def test_uses_art_when_present(self):
        r, out = self.build(art=self.art_fixture())
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIn('placeholders: %d' % (len(part_models()) - 1), r.stdout)
        with self.pack(out) as z:
            model = json.loads(z.read('assets/gravesandglory/models/prop/anvil.json'))
            self.assertEqual(model['textures'], {'0': 'gravesandglory:prop/anvil', 'particle': 'gravesandglory:prop/anvil'})
            self.assertEqual(model['elements'][0]['to'], [16, 10, 16])
            self.assertEqual(z.read('assets/gravesandglory/textures/prop/anvil.png'), PNG)

    def test_paper_keeps_vanilla_look(self):
        r, out = self.build()
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        with self.pack(out) as z:
            paper = json.loads(z.read('assets/minecraft/models/item/paper.json'))
            self.assertEqual(paper['parent'], 'minecraft:item/generated')
            self.assertEqual(paper['textures'], {'layer0': 'minecraft:item/paper'})
            self.assertFalse([n for n in z.namelist() if n.startswith('assets/minecraft/textures')])

    def test_clash_exits_2(self):
        clash = self.dir / 'excalibur.zip'
        with zipfile.ZipFile(clash, 'w') as z:
            z.writestr('assets/minecraft/models/item/paper.json', json.dumps(
                {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/paper'},
                 'overrides': [{'predicate': {'custom_model_data': 12}, 'model': 'x:a'},
                               {'predicate': {'custom_model_data': 7310005}, 'model': 'x:b'}]}))
        r, out = self.build(excalibur=clash)
        self.assertEqual(r.returncode, 2, r.stdout + r.stderr)
        self.assertIn('7310005', r.stdout + r.stderr)
        self.assertFalse((out / 'gg-props-latest.sha1').exists())

    def test_reproducible(self):
        r1, a = self.build('a')
        r2, b = self.build('b')
        self.assertEqual((r1.returncode, r2.returncode), (0, 0))
        sha = (a / 'gg-props-latest.sha1').read_text().strip()
        self.assertEqual(sha, (b / 'gg-props-latest.sha1').read_text().strip())
        name = 'gg-props-' + sha[:8] + '.zip'
        self.assertEqual((a / name).read_bytes(), (b / name).read_bytes())
        with zipfile.ZipFile(a / name) as z:
            self.assertEqual({i.date_time for i in z.infolist()}, {(1980, 1, 1, 0, 0, 0)})
            self.assertEqual(z.namelist(), sorted(z.namelist()))

    def test_pack_format_15(self):
        r, out = self.build()
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        with self.pack(out) as z:
            self.assertEqual(json.loads(z.read('pack.mcmeta'))['pack']['pack_format'], 15)

    def test_block_atlas_stitches_prop_textures(self):
        """1.20.1 stitches only textures/block and textures/item into the block atlas; textures/prop needs an atlas source."""
        r, out = self.build()
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        with self.pack(out) as z:
            atlas = json.loads(z.read('assets/minecraft/atlases/blocks.json'))
            self.assertEqual(atlas, {'sources': [{'type': 'directory', 'source': 'prop', 'prefix': 'prop/'}]})

    def test_element_range(self):
        r, out = self.build(art=self.art_fixture(element_from=(-20, 0, 0)))
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn('anvil', r.stdout + r.stderr)
        self.assertFalse((out / 'gg-props-latest.sha1').exists())


if __name__ == '__main__':
    unittest.main()
