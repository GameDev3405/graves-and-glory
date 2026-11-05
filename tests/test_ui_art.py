"""Command panel art (command UI Task 8): the pennant, pole, pills, tray, order icons and formation shapes that
CommandPanel draws, exported by tools/build_ui_art.py into textures/gui/command/. unittest-compatible:
    python -m unittest discover -s tests -p test_ui_art.py"""
import json
import pathlib
import re
import unittest

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
GUI = ROOT / 'src/main/resources/assets/gravesandglory/textures/gui'
CMD = GUI / 'command'
PAINTER = ROOT / 'tools/ui_art/cmd_panel.py'

ICONS = ('attack', 'charge', 'follow', 'retreat', 'formation')
SHAPES = ('line', 'block', 'wedge', 'loose')
SIZES = {'pennant': (64, 24), 'pennant_trim': (64, 24)}
SIZES.update({'icon_' + i: (16, 16) for i in ICONS})
SIZES.update({'shape_' + s: (24, 24) for s in SHAPES})
OTHERS = ('pole', 'pill', 'tray')


class CommandTextures(unittest.TestCase):
    def atlas(self):
        return json.loads((GUI / 'atlas.json').read_text(encoding='utf-8'))['textures']

    def test_command_textures(self):
        atlas = self.atlas()
        for name in list(SIZES) + list(OTHERS):
            path = CMD / (name + '.png')
            self.assertTrue(path.is_file(), path)
            im = Image.open(path)
            entry = atlas.get('command/' + name)
            self.assertIsNotNone(entry, 'atlas.json lacks command/' + name)
            self.assertEqual(im.size, (entry['w'], entry['h']), name)   # the size the builder wrote
            if name in SIZES:
                self.assertEqual(im.size, SIZES[name], name)
            self.assertTrue(any(a for a in im.convert('RGBA').getchannel('A').getdata()), name + ' is empty')

    def test_one_texel_per_pixel(self):
        """painted at GUI scale 1: the painter never resizes or scales (no resize, no scale factor, no float scaling)"""
        src = PAINTER.read_text(encoding='utf-8')
        self.assertIsNone(re.search(r'\.resize\(|\.thumbnail\(|scale\s*=|Image\.(?:NEAREST|BILINEAR|LANCZOS)', src))

    def test_pennant_body_greyscale(self):
        """the body is tinted per House at draw time, so every painted pixel is a grey"""
        im = Image.open(CMD / 'pennant.png').convert('RGBA')
        grey = [(r, g, b) for (r, g, b, a) in im.getdata() if a]
        self.assertGreater(len(grey), 64 * 24 // 2)
        for (r, g, b) in grey:
            self.assertTrue(r == g == b, (r, g, b))

    def test_trim_follows_pennant(self):
        """the gold trim lies on the pennant's own pixels, so the two line up when drawn at the same spot"""
        body = Image.open(CMD / 'pennant.png').convert('RGBA')
        trim = Image.open(CMD / 'pennant_trim.png').convert('RGBA')
        bp, tp = body.load(), trim.load()
        painted = [(x, y) for y in range(24) for x in range(64) if tp[x, y][3]]
        self.assertTrue(painted)
        for (x, y) in painted:
            self.assertTrue(bp[x, y][3], (x, y))
            r, g, b, _ = tp[x, y]
            self.assertTrue(r > b, 'trim pixel (%d, %d) is not gold' % (x, y))

    def test_icons_differ(self):
        """no two order icons or two formation shapes share their silhouette"""
        for group, prefix in ((ICONS, 'icon_'), (SHAPES, 'shape_')):
            masks = {}
            for n in group:
                im = Image.open(CMD / (prefix + n + '.png')).convert('RGBA')
                masks[n] = tuple(1 if a else 0 for a in im.getchannel('A').getdata())
            self.assertEqual(len(set(masks.values())), len(group), prefix)


if __name__ == '__main__':
    unittest.main()
