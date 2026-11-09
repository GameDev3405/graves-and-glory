"""The texture recipe's tools (docs/art/TEXTURE-RECIPE.md): house_palette.py register, the template builder's preview
passing the palette and hue checks. unittest-compatible:
    python -m unittest discover -s tests -p test_texture_recipe.py"""
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
HP = ROOT / 'tools/art/house_palette.py'
TEMPLATE = ROOT / 'art/hand/_template/build_template.py'
sys.path.insert(0, str(ROOT / 'art/hand/common'))

MODULE = '''from handlib import ramp
STONE = ramp("1a1814", "2e2a24", "48423a", "665e52", "8a8070", "b0a692")
MOSSGLOW = ramp("0e1a12", "1a3322", "2a5236", "3e7650", "5e9a70")
_PRIVATE = ramp("000000", "ffffff")
'''


def run(*args):
    return subprocess.run([sys.executable] + [str(a) for a in args], capture_output=True, text=True, cwd=ROOT)


class Register(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.d = pathlib.Path(self.tmp.name)
        (self.d / 'newhouse.py').write_text(MODULE, encoding='utf-8')

    def tearDown(self):
        self.tmp.cleanup()

    def register(self, *extra):
        return run(HP, 'register', 'newhouse', self.d / 'newhouse.py', '--pal-dir', self.d / 'pal', '--no-show', *extra)

    def test_writes_whole_ramps_and_reads_back(self):
        r = self.register('--with', 'handlib.LEATH')
        self.assertEqual(r.returncode, 0, r.stderr)
        p = json.loads((self.d / 'pal/newhouse.json').read_text(encoding='utf-8'))
        self.assertEqual(sorted(p['ramps']), ['handlib.LEATH', 'newhouse.MOSSGLOW', 'newhouse.STONE'])
        self.assertEqual(len(p['ramps']['newhouse.STONE']['colors']), 6)
        self.assertEqual(p['ramps']['newhouse.STONE']['colors'][0], {'hex': '#1a1814', 'used_by': []})
        self.assertEqual(p['modules'], ['newhouse'])
        import palette
        old = palette.PAL_DIR
        try:
            palette.PAL_DIR, palette._cache = str(self.d / 'pal'), {}
            R = palette.require('newhouse', 'STONE', 'LEATH')
            self.assertEqual(R['STONE'][-1], (0xb0, 0xa6, 0x92, 255))
        finally:
            palette.PAL_DIR, palette._cache = old, {}

    def test_refuses_existing_unless_forced(self):
        self.assertEqual(self.register().returncode, 0)
        r = self.register()
        self.assertNotEqual(r.returncode, 0)
        self.assertIn('locked', r.stderr)
        self.assertEqual(self.register('--force').returncode, 0)

    def test_refuses_locked_house_and_bad_ramps(self):
        r = run(HP, 'register', 'graveborn', self.d / 'newhouse.py', '--no-show')
        self.assertNotEqual(r.returncode, 0)
        self.assertIn('locked', r.stderr)
        (self.d / 'bad.py').write_text('from handlib import ramp\nX = ramp("ffffff", "202020", "101010")\n', encoding='utf-8')
        r = run(HP, 'register', 'badhouse', self.d / 'bad.py', '--pal-dir', self.d / 'pal', '--no-show')
        self.assertNotEqual(r.returncode, 0)
        self.assertIn('not dark to light', r.stderr)


class Template(unittest.TestCase):
    def test_every_preset_previews_and_passes_checks(self):
        with tempfile.TemporaryDirectory() as tmp:
            for house in ('gilded', 'graveborn', 'wildmoot', 'ironclad', 'tidecallers'):
                out = pathlib.Path(tmp) / house
                r = run(TEMPLATE, 'preview', out, 'crimson', '--house=' + house)
                self.assertEqual(r.returncode, 0, r.stderr)
                for t in (1, 2, 3):
                    self.assertTrue((out / ('template_t%d.png' % t)).is_file())
                    self.assertTrue((out / ('template_t%d.geo.json' % t)).is_file())
                r = run(HP, 'check', '--dir', out, '--house', house)
                self.assertEqual(r.returncode, 0, house + r.stdout)
                r = run(ROOT / 'tools/art_hue_check.py', '--dir', out, '--house', house)
                self.assertEqual(r.returncode, 0, house + r.stdout)

    def test_gate_passes_every_preset(self):
        for house in ('gilded', 'graveborn', 'wildmoot', 'ironclad', 'tidecallers'):
            r = run(TEMPLATE, 'gate', '--house=' + house)
            self.assertEqual(r.returncode, 0, house + r.stdout)
            self.assertIn('gate PASS', r.stdout)

    def load(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location('build_template_under_test', TEMPLATE)
        m = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(m)
        return m

    def test_gate_fails_a_flat_arm(self):
        m = self.load()
        Rm = m.R('padding')
        m.arm = lambda: (lambda face, w, h: [[Rm[2]] * w for _ in range(h)])   # a flat painter: no light at all
        problems = m.gate(m.TEMPLATE)
        self.assertTrue(any('flat or lit from below' in p for p in problems), problems)

    def test_gate_fails_off_house_main_mass(self):
        m = self.load()
        m.PRIMARY_ROLES = ('plate',)   # tier I has no plate: its hood, gambeson and sleeves are then not a primary ramp
        problems = m.gate(m.TEMPLATE)
        self.assertTrue(any(p.startswith('tier 1: primary ramps cover') for p in problems), problems)

    def test_material_cues_differ(self):
        m = self.load()
        w, h = 6, 12
        grids = {name: m.fill('padding', pattern=getattr(m, name))('front', w, h) for name in ('rope_twist', 'gloss_fold')}
        grids['shell'] = m.shell('plate')('front', w, h)
        for name, g in grids.items():
            flat = [c for row in g for c in row]
            self.assertTrue(all(c is not None for c in flat), name)
            self.assertGreaterEqual(len(set(flat)), 3, name)
        self.assertNotEqual(grids['rope_twist'], grids['gloss_fold'])

    def test_never_installs(self):
        r = run(TEMPLATE, 'install')
        self.assertNotEqual(r.returncode, 0)
        self.assertFalse((ROOT / 'art/troops/template.json').exists())


if __name__ == '__main__':
    unittest.main()
