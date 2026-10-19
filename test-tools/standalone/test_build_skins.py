"""Checks tools/build_skins.py (Task 5) against fixture sources in a temp dir, without launching the game."""
import contextlib
import copy
import io
import json
import pathlib
import re
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
import build_skins
from PIL import Image

ASSETS = 'src/main/resources/assets/gravesandglory'
GOLD, VIOLET, CRIMSON, TEAL = 14922561, 11566335, 14701130, 4179904
GREY = {1.0: (160, 160, 160), 0.8: (128, 128, 128), 0.6: (96, 96, 96)}   # the template's three greys
WHITE, POLE = (243, 239, 228), (90, 70, 50)


def box(origin, size, uv, mirror=False):
    return {'origin': origin, 'size': size, 'uv': uv, 'mirror': mirror}


BIPED = {'rig': 'biped', 'size': [64, 64], 'outer': 0.25, 'hands': {'right': 'right_hand', 'left': 'left_hand'}, 'bones': [
    {'name': 'root', 'parent': '', 'pivot': [0, 0, 0], 'boxes': []},
    {'name': 'body', 'parent': 'root', 'pivot': [0, 12, 0], 'boxes': [box([-4, -12, -2], [8, 12, 4], [16, 16])]},
    {'name': 'head', 'parent': 'body', 'pivot': [0, -12, 0], 'boxes': [box([-4, -8, -4], [8, 8, 8], [0, 0])]},
    {'name': 'right_arm', 'parent': 'body', 'pivot': [-5, -10, 0], 'boxes': [box([-3, -2, -2], [4, 12, 4], [40, 16])]},
    {'name': 'right_hand', 'parent': 'right_arm', 'pivot': [-1, 10, 0], 'boxes': []},
    {'name': 'left_arm', 'parent': 'body', 'pivot': [5, -10, 0], 'boxes': [box([-1, -2, -2], [4, 12, 4], [32, 48])]},
    {'name': 'left_hand', 'parent': 'left_arm', 'pivot': [1, 10, 0], 'boxes': []},
    {'name': 'arms_folded', 'parent': 'body', 'pivot': [0, -9, -1], 'rotation': [-43, 0, 0], 'boxes': [box([-4, 2, -2], [8, 4, 4], [40, 40])]},
    {'name': 'right_leg', 'parent': 'root', 'pivot': [-1.9, 12, 0], 'boxes': [box([-2, 0, -2], [4, 12, 4], [0, 16])]},
    {'name': 'left_leg', 'parent': 'root', 'pivot': [1.9, 12, 0], 'boxes': [box([-2, 0, -2], [4, 12, 4], [16, 48])]}]}
HOUND = {'rig': 'hound', 'size': [64, 64], 'outer': 0.25, 'hands': {'right': 'jaw', 'left': 'jaw'}, 'bones': [
    {'name': 'root', 'parent': '', 'pivot': [0, 0, 0], 'boxes': []},
    {'name': 'body', 'parent': 'root', 'pivot': [0, 14, 4], 'boxes': [box([-3, -3, -4], [6, 6, 8], [0, 0])]},
    {'name': 'chest', 'parent': 'body', 'pivot': [0, 0, -4], 'boxes': [box([-3, -3, -6], [6, 6, 6], [0, 14])]},
    {'name': 'head', 'parent': 'chest', 'pivot': [0, -2, -6], 'boxes': [box([-2, -3, -4], [4, 4, 4], [28, 0])]},
    {'name': 'jaw', 'parent': 'head', 'pivot': [0, 1, -4], 'boxes': [box([-1, 0, -3], [2, 1, 3], [44, 0])]},
    {'name': 'front_left_leg', 'parent': 'chest', 'pivot': [2, 3, -3], 'boxes': [box([-1, 0, -1], [2, 7, 2], [0, 26])]},
    {'name': 'front_right_leg', 'parent': 'chest', 'pivot': [-2, 3, -3], 'boxes': [box([-1, 0, -1], [2, 7, 2], [8, 26])]},
    {'name': 'tail', 'parent': 'body', 'pivot': [0, -2, 4], 'boxes': [box([-1, 0, 0], [2, 2, 6], [16, 26])]},
    {'name': 'back_left_leg', 'parent': 'root', 'pivot': [2, 17, 7], 'boxes': [box([-1, 0, -1], [2, 7, 2], [32, 26])]},
    {'name': 'back_right_leg', 'parent': 'root', 'pivot': [-2, 17, 7], 'boxes': [box([-1, 0, -1], [2, 7, 2], [40, 26])]}]}
PARTS = {
    'cap': {'part': 'cap', 'bone': 'head', 'team': False, 'tag': 'helm', 'boxes': [box([-4, -9, -4], [8, 2, 8], [0, 0])]},
    'skirt': {'part': 'skirt', 'bone': 'body', 'team': False, 'tag': '', 'boxes': [box([-4, 0, -2], [8, 4, 4], [0, 0])]},
    'helm': {'part': 'helm', 'bone': 'head', 'team': False, 'tag': 'helm', 'boxes': [box([-4, -9, -4], [8, 3, 8], [0, 0])]},
    'pennant': {'part': 'pennant', 'bone': 'body', 'team': True, 'tag': '', 'boxes': [box([-2, -26, 3], [4, 8, 1], [0, 0])]},
}
SWORD = {'weapon': 'sword', 'size': [16, 16], 'boxes': [box([-0.5, -10, -0.5], [1, 12, 1], [0, 0])],
         'grip': {'offset': [0, 0, 0], 'rotation': [90, 0, 0]}}
TROOPS = {
    'knight': {'troop': 'knight', 'rig': 'biped', 'parts': [{'part': 'cap', 'uv': [32, 0]}, {'part': 'skirt', 'uv': [0, 32]}, {'part': 'pennant'}],
               'skins': {'1': 'knight_s1.png'}, 'weapon': 'sword', 'hand': 'right', 'portrait': 'standard'},
    'sentinel': {'troop': 'sentinel', 'rig': 'biped', 'parts': [{'part': 'helm', 'uv': [32, 0]}, {'part': 'pennant'}],
                 'skins': {'1': 'sentinel_s1.png'}, 'weapon': 'sword', 'hand': 'right', 'portrait': 'tall 2'},
    'mage': {'troop': 'mage', 'rig': 'biped', 'parts': [{'part': 'pennant'}], 'skins': {'1': 'mage_s1.png'},
             'weapon': 'none', 'hand': 'right', 'portrait': 'tall 6'},
}
SHEET = {
    'factions': [{'id': 'gold', 'color': GOLD, 'glyph': 'sun'}, {'id': 'violet', 'color': VIOLET, 'glyph': 'moon'},
                 {'id': 'crimson', 'color': CRIMSON, 'glyph': 'star'}, {'id': 'teal', 'color': TEAL, 'glyph': 'wave'}],
    'troops': [{'id': 'knight', 'entity': 'husk', 'rarity': 'common', 'evolveLine': 'knight'},
               {'id': 'sentinel', 'entity': 'husk', 'rarity': 'rare', 'evolveLine': 'sentinel'},
               {'id': 'mage', 'entity': 'husk', 'rarity': 'epic', 'evolveLine': 'none'},
               {'id': 'hound', 'entity': 'wolf', 'rarity': 'common', 'evolveLine': 'hound'}],
    'evolutions': [{'id': 'knight_2', 'troop': 'knight', 'stage': 2}, {'id': 'knight_3', 'troop': 'knight', 'stage': 3},
                   {'id': 'sentinel_2', 'troop': 'sentinel', 'stage': 2}, {'id': 'sentinel_3', 'troop': 'sentinel', 'stage': 3}],
}
GLYPH_AT = [(2, 3), (3, 3)]   # the sun mask's pixels, on the pennant field
FIELD = {(x, y): (1.0 if y < 4 else 0.8 if y < 7 else 0.6) for x in range(1, 9) for y in range(1, 9)}
CHARGE_AT, POLE_AT = (5, 5), (0, 0)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1), encoding='utf-8')


def skin(path, size=(64, 64), paint=(200, 196, 186, 255), extra=()):
    im = Image.new('RGBA', size, paint)
    for xy, colour in extra:
        im.putpixel(xy, colour)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def glow(path, lit, size=(64, 64)):
    im = Image.new('RGBA', size, (0, 0, 0, 0))
    for i in range(lit):
        im.putpixel((i % 8, i // 8), (255, 240, 200, 255))
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def fixture(root, troops=None, parts=None, rigs=None, sheet=None):
    """A minimal source tree: the sheet, two rigs, four parts, one weapon, three troops, the cloth template and glyphs."""
    root = pathlib.Path(root)
    write_json(root / 'sheets/design.json', sheet or SHEET)
    for rig in rigs or (BIPED, HOUND):
        write_json(root / 'art/rigs' / (rig['rig'] + '.json'), rig)
    for part in (parts or PARTS).values():
        write_json(root / 'art/parts' / (part['part'] + '.json'), part)
    write_json(root / 'art/weapons/sword.json', SWORD)
    skin(root / 'art/weapons/sword.png', (16, 16), (124, 133, 145, 255))
    for t in (troops or TROOPS).values():
        write_json(root / 'art/troops' / (t['troop'] + '.json'), t)
        for f in t['skins'].values():
            skin(root / 'art/troops' / f)
    cloth = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for xy, k in FIELD.items():
        cloth.putpixel(xy, GREY[k] + (255,))
    cloth.putpixel(CHARGE_AT, WHITE + (255,))
    cloth.putpixel(POLE_AT, POLE + (255,))
    (root / 'art/team').mkdir(parents=True, exist_ok=True)
    cloth.save(root / 'art/team/team_cloth.png')
    for g in ('sun', 'moon', 'star', 'wave'):
        mask = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for xy in (GLYPH_AT if g == 'sun' else [(6, 2)]):
            mask.putpixel(xy, (255, 255, 255, 255))
        mask.save(root / 'art/team' / ('glyph_%s.png' % g))
    return root


def export_assets(root):
    return build_skins.export(pathlib.Path(root), pathlib.Path(root) / ASSETS)


def find(model, name):
    return next(b for b in model['bones'] if b['name'] == name)


def absolute_bottom(model, name):
    """The lowest y of a bone's boxes in model space; the fixture legs have no rotation on their chain."""
    bones = {b['name']: b for b in model['bones']}
    y, b = 0.0, bones[name]
    chain = b
    while chain:
        y += chain['pivot'][1]
        chain = bones.get(chain['parent'])
    return max(y + bx['origin'][1] + bx['size'][1] + bx['dilation'][1] for bx in b['boxes'])


class BuildSkinsChecks(unittest.TestCase):
    def fails(self, fn, *args):
        """Runs fn, which must exit 1 through build_skins.fail; returns what it printed."""
        out = io.StringIO()
        with contextlib.redirect_stdout(out), self.assertRaises(SystemExit) as caught:
            fn(*args)
        self.assertEqual(caught.exception.code, 1, out.getvalue())
        return out.getvalue()

    def export(self, root):
        with contextlib.redirect_stdout(io.StringIO()):
            return export_assets(root)

    def bake(self, troops):
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp, troops=troops))
            return {t: build_skins.bake(cat, t) for t in troops}

    def with_knight(self, **changes):
        troops = copy.deepcopy(TROOPS)
        troops['knight'].update(changes)
        return troops

    def test_bake_scale_about_pivot(self):
        model = self.bake(self.with_knight(bones={'body': {'scale': [1.2, 1, 1]}}))['knight']
        body = find(model, 'body')['boxes'][0]
        self.assertAlmostEqual(body['origin'][0], -4.0)
        self.assertEqual(body['size'][0], 8)
        self.assertAlmostEqual(body['dilation'][0], 0.8)
        self.assertAlmostEqual(find(model, 'right_arm')['pivot'][0], -6.0)
        self.assertAlmostEqual(find(model, 'left_arm')['pivot'][0], 6.0)
        self.assertAlmostEqual(find(model, 'head')['pivot'][0], 0.0)

    def test_feet_on_ground(self):
        legs = {'right_leg': {'scale': [1, 1.15, 1]}, 'left_leg': {'scale': [1, 1.15, 1]}}
        model = self.bake(self.with_knight(bones=legs))['knight']
        for leg in ('right_leg', 'left_leg'):
            self.assertAlmostEqual(absolute_bottom(model, leg), 24.0)
        self.assertAlmostEqual(find(model, 'root')['pivot'][1], -1.8)
        plain = self.bake(TROOPS)['knight']
        self.assertAlmostEqual(absolute_bottom(plain, 'right_leg'), 24.0)

    def test_hunch(self):
        model = self.bake(self.with_knight(hunch=20))['knight']
        self.assertAlmostEqual(find(model, 'body')['rotation'][0], 20)
        for bone in ('head', 'right_arm', 'left_arm'):
            self.assertAlmostEqual(find(model, bone)['rotation'][0], -20)

    def test_uv_overlap(self):
        troops = self.with_knight(parts=[{'part': 'cap', 'uv': [32, 0]}, {'part': 'skirt', 'uv': [32, 0]}, {'part': 'pennant'}])
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, fixture(tmp, troops=troops))
        self.assertIn('knight', said)
        self.assertIn('cap', said)
        self.assertIn('skirt', said)

    def test_bad_names(self):
        cases = {
            'quadruped': self.with_knight(rig='quadruped'),
            'tail': self.with_knight(bones={'tail': {'scale': 1.1}}),
            'wings': self.with_knight(parts=[{'part': 'wings', 'uv': [32, 0]}]),
            'hound': self.with_knight(rig='hound'),
            'head': self.with_knight(parts=[{'part': 'cap', 'name': 'head', 'uv': [32, 0]}]),
        }
        cases['skirt'] = self.with_knight(parts=[{'part': 'skirt', 'uv': [0, 32]}, {'part': 'skirt', 'uv': [0, 40]}])
        for name, troops in cases.items():
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                said = self.fails(export_assets, fixture(tmp, troops=troops))
                self.assertIn('knight', said)
                self.assertIn(name, said)

    def test_vanilla_keys(self):
        vanilla = {'parent', 'elements', 'textures', 'display', 'overrides', 'gui_light', 'ambientocclusion'}
        allowed = {'format', 'troop', 'rig', 'size', 'bones', 'skins', 'veteran', 'glow', 'glowFrom', 'outer', 'weapon',
                   'hand', 'idleArms', 'portrait'}
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            self.export(root)
            models = sorted((root / ASSETS / 'models/troop').rglob('*.json'))
            self.assertTrue(models)
            for path in models:
                data = json.loads(path.read_text(encoding='utf-8'))
                with self.subTest(path=path.name):
                    self.assertIsInstance(data, dict)
                    self.assertFalse(vanilla & set(data), sorted(vanilla & set(data)))
                    if path.parent.name == 'troop' and path.name != 'index.json':
                        self.assertLessEqual(set(data), allowed)

    def test_skin_names(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            glow(root / 'art/troops/mage_e.png', 4)
            self.export(root)
            textures = root / ASSETS / 'textures/entity/troop'
            names = [p.name for p in textures.glob('*.png') if not p.name.startswith('team_cloth_')]
            self.assertIn('mage_e.png', names)
            for name in names:
                self.assertRegex(name, r'^[a-z0-9_]+_(s\d+|veteran|e|o)\.png$')
            skin(root / 'art/troops/knight.png')
            said = self.fails(export_assets, root)
            self.assertIn('knight.png', said)

    def test_glow_cap(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            glow(root / 'art/troops/knight_e.png', 7)
            said = self.fails(export_assets, root)
            self.assertIn('knight', said)
            self.assertIn('glow', said)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            glow(root / 'art/troops/mage_e.png', 16)
            self.export(root)
            self.assertTrue((root / ASSETS / 'textures/entity/troop/mage_e.png').is_file())

    def test_tint_rule(self):
        for colour, ok in (((0xE3, 0xB3, 0x41, 255), False), ((0x96, 0x78, 0x3A, 255), True)):
            with self.subTest(colour=colour), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                skin(root / 'art/troops/knight_s1.png', extra=[((10, 10), colour)])
                if ok:
                    self.export(root)
                else:
                    said = self.fails(export_assets, root)
                    self.assertIn('knight', said)
                    self.assertIn('knight_s1.png', said)

    def test_team_cloth(self):
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp))
            gold, linen = build_skins.cloth(cat, 'gold').convert('RGBA'), build_skins.cloth(cat, None).convert('RGBA')
            company = build_skins.cloth(cat, 'company').convert('RGBA')
        rgb = ((GOLD >> 16) & 255, (GOLD >> 8) & 255, GOLD & 255)
        for xy, k in FIELD.items():
            if xy in GLYPH_AT or xy == CHARGE_AT:
                continue
            want = tuple(int(c * k + 0.5) for c in rgb)
            self.assertEqual(gold.getpixel(xy)[:3], want, (xy, k))
            self.assertEqual(linen.getpixel(xy)[:3], tuple(int(c * k + 0.5) for c in (0xCF, 0xC6, 0xB0)), (xy, k))
            self.assertEqual(company.getpixel(xy)[:3], tuple(int(c * k + 0.5) for c in (0x2A, 0x37, 0x47)), (xy, k))
        self.assertEqual(gold.getpixel((2, 2))[:3], rgb)   # grey x1.0 is the side colour itself
        for xy in GLYPH_AT:
            self.assertEqual(gold.getpixel(xy)[:3], build_skins.GLYPH)
            self.assertEqual(linen.getpixel(xy)[:3], (0xCF, 0xC6, 0xB0))
            self.assertEqual(company.getpixel(xy)[:3], (0x2A, 0x37, 0x47))   # the company's cloth carries no glyph
        self.assertEqual(gold.getpixel(CHARGE_AT)[:3], WHITE)
        self.assertEqual(gold.getpixel(POLE_AT)[:3], POLE)
        self.assertEqual(linen.getpixel(CHARGE_AT)[:3], WHITE)
        self.assertEqual(gold.getpixel((15, 15))[3], 0)

    def test_index_and_stale(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            index = self.export(root)
            on_disk = json.loads((root / ASSETS / 'models/troop/index.json').read_text(encoding='utf-8'))
            self.assertEqual(index, on_disk)
            self.assertEqual(index['troops'], ['knight', 'mage', 'sentinel'])
            self.assertEqual(index['rigs'], ['biped', 'hound'])
            self.assertEqual(index['weapons'], ['sword'])
            written = sorted(p.relative_to(root / ASSETS).as_posix() for p in (root / ASSETS).rglob('*') if p.is_file())
            self.assertEqual(sorted(index['outputs'] + ['models/troop/index.json']), written)
            for side in ('gold', 'violet', 'crimson', 'teal', 'linen', 'company'):
                self.assertIn('textures/entity/troop/team_cloth_%s.png' % side, index['outputs'])
            self.assertIn('models/troop/sentinel.json', index['outputs'])
            (root / 'art/troops/sentinel.json').unlink()
            (root / 'art/troops/sentinel_s1.png').unlink()
            index = self.export(root)
            self.assertEqual(index['troops'], ['knight', 'mage'])
            self.assertFalse((root / ASSETS / 'models/troop/sentinel.json').exists())
            self.assertFalse((root / ASSETS / 'textures/entity/troop/sentinel_s1.png').exists())
            self.assertNotIn('models/troop/sentinel.json', index['outputs'])
            self.assertTrue((root / ASSETS / 'models/troop/knight.json').is_file())
            raw = (root / ASSETS / 'models/troop/index.json').read_bytes()
            self.assertNotIn(b'\r', raw)
            self.assertTrue(raw.endswith(b'\n'))

    def test_verify(self):
        for target in ('models/troop/knight.json', 'textures/entity/troop/knight_s1.png'):
            with self.subTest(target=target), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                self.export(root)
                with contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(build_skins.verify(root), 0)
                path = root / ASSETS / target
                data = bytearray(path.read_bytes())
                data[len(data) // 2] ^= 0x01
                path.write_bytes(bytes(data))
                said = self.fails(build_skins.verify, root)
                self.assertIn(target, said)

    def test_verify_pixels(self):
        """--verify compares a PNG by its pixels: the same pixels in other bytes pass, and export leaves them alone; one
        changed pixel in a source fails, naming the output and the pixel."""
        target = 'textures/entity/troop/knight_s1.png'
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            self.export(root)
            path = root / ASSETS / target
            before = path.read_bytes()
            with Image.open(path) as im:
                same = im.copy()
            same.save(path, 'PNG', compress_level=1)
            recoded = path.read_bytes()
            self.assertNotEqual(recoded, before)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.verify(root), 0)
            self.export(root)
            self.assertEqual(path.read_bytes(), recoded)
            source = root / 'art/troops/knight_s1.png'
            with Image.open(source) as im:
                changed = im.copy()
            changed.putpixel((3, 2), (90, 90, 90, 255))
            changed.save(source)
            said = self.fails(build_skins.verify, root)
            self.assertIn(target, said)
            self.assertIn('pixel (3, 2)', said)


if __name__ == '__main__':
    unittest.main(verbosity=2)
