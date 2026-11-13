"""tools/where.py and tools/where_extract.py: the overlay answers troop ids (any troop), design-sheet ids (any section) and rules keys; generic ids are
skipped and said so; the rg fallback answers words nothing else knows; Audio.java's arm()/meleeCue() still have the shape the Python mirror encodes."""
import pathlib, re, subprocess, sys, time, unittest
ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOLS = ROOT/'tools'
sys.path.insert(0, str(TOOLS))
import where_extract  # noqa: E402

def where(*args):
    t = time.time()
    r = subprocess.run([sys.executable, str(TOOLS/'where.py'), *args], cwd=ROOT, capture_output=True, encoding='utf-8', errors='replace')
    return r.stdout, time.time()-t

class Where(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # always rebuild: the overlay must match this checkout's tools and sheet
        cls.t = time.time()
        r = subprocess.run([sys.executable, str(TOOLS/'where_extract.py')], cwd=ROOT, capture_output=True, encoding='utf-8', errors='replace')
        cls.extract_secs = time.time()-cls.t
        cls.extract_out = r.stdout
        assert r.returncode == 0, r.stderr
        where('warmup')   # builds the AST index cache if it is missing

    def test_extract_is_fast_and_covers_every_troop_and_section(self):
        self.assertLess(self.extract_secs, 30)
        import json
        o = json.loads((ROOT/'work'/'where'/'overlay.json').read_text(encoding='utf-8'))
        troops = [r['id'] for r in json.loads((ROOT/'sheets'/'design.json').read_text(encoding='utf-8'))['troops']]
        self.assertEqual(o['meta']['troops'], troops)
        have = {n['label'] for n in o['nodes'] if n.get('ent') == 'troop'}
        self.assertEqual(have, set(troops))
        self.assertIn('abilities', o['meta']['sections'])
        self.assertTrue(o['meta']['skipped'])
        self.assertIn('skipped', self.extract_out)

    def test_rules_key_returns_spec_line_and_java_reader(self):
        out, secs = where('maxTroops')
        self.assertRegex(out, r'Spec\.java:\d+')
        self.assertRegex(out, r'sheets/design\.json:\d+')
        self.assertRegex(out, r'src/main/java/\S+\.java:\d+\s+reads rules\.maxTroops')
        self.assertLess(secs, 3)

    def test_sample_troop_returns_design_row_and_animation_file(self):
        out, secs = where('tc_raider')
        self.assertRegex(out, r'sheets/design\.json:\d+\s+design\.json troops row tc_raider')
        self.assertRegex(out, r'\S+tc_raider\.animation\.json')
        self.assertLess(secs, 3)

    def test_other_troop_returns_row_spec_model_both_texture_folders_and_builder(self):
        out, secs = where('gl_marshal')
        self.assertRegex(out, r'sheets/design\.json:\d+\s+design\.json troops row gl_marshal')
        self.assertRegex(out, r'Spec\.java:\d+\s+Spec\.TROOPS generated row')
        self.assertRegex(out, r'\S+gl_marshal\.(geo\.json|bbmodel):\d+\s')
        self.assertRegex(out, r'textures/entity/troop/gl_marshal/\s+legacy troop textures')
        self.assertRegex(out, r'textures/entity/troop/gl_marshal_geo/\s+GeckoLib troop textures')
        self.assertRegex(out, r'art/anim/knight/build_anims\.py:\d+\s+builder script')
        self.assertLess(secs, 3)

    def test_builder_import_edge_is_recorded(self):
        out, _ = where('ic_breaker')
        self.assertRegex(out, r'build_axe_anims\.py:\d+\s+build_axe_anims\.py imports art/anim/knight/build_anims\.py')
        self.assertIn('family axe', out)   # the troop names its family; the family entity carries the attackVariants rows
        fam, _ = where('axe')
        self.assertRegex(fam, r'design\.json attackVariants row axe_\w+ for family axe')

    def test_ability_id_returns_sheet_row_spec_row_and_a_reader(self):
        out, secs = where('brine')
        self.assertRegex(out, r'sheets/design\.json:\d+\s+design\.json abilities row brine')
        self.assertRegex(out, r'Spec\.java:\d+\s+Spec\.ABILITIES\[\d+\] generated row for brine')
        self.assertRegex(out, r'src/main/java/\S+\.java:\d+\s+mentions brine')
        self.assertLess(secs, 3)

    def test_generic_id_is_skipped_and_says_so(self):
        out, _ = where('block')
        self.assertRegex(out, r'skipped as too generic \(\d+ text matches')
        self.assertNotIn('(formations):', out)   # no overlay entity for it
        self.assertRegex(out, r'sheets/design\.json:\d+ \(formations\)')   # but the sheet row is named

    def test_second_troop_and_max_cap(self):
        out, _ = where('ic_hearthguard', '--max', '5')
        lines = [l for l in out.splitlines() if re.match(r'\s+\S+:\d+\s', l) or re.match(r'\s+\S+/\s', l)]
        self.assertTrue(0 < len(lines) <= 5, lines)
        self.assertIn('design.json', out)

    def test_unknown_word_falls_back_to_rg(self):
        out, _ = where('Slowness')
        self.assertIn('rg fallback', out)
        self.assertRegex(out, r'sheets/design\.json:\d+')

    def test_nothing_anywhere_says_so(self):
        out, _ = where('qzx' + 'wvy' + 'plugh')
        self.assertIn('no match', out)

    def test_audio_java_arm_and_meleecue_keep_the_shape_the_mirror_encodes(self):
        # The sound chain is a Python mirror of Audio.arm() and Audio.meleeCue(). If either changes shape this fails: update
        # where_extract.audio_arm()/sound_links() and ARM_SHAPE/MELEE_SHAPE together.
        self.assertEqual(where_extract.audio_shape_problems(), [])

    def test_shape_guard_detects_drift(self):
        text = where_extract.rd(where_extract.AUDIO)
        self.assertEqual(where_extract.java_body(text, 'public static Arm arm('), where_extract.ARM_SHAPE)
        changed = text.replace('Arm.HEAVY : Arm.BLADE', 'Arm.HEAVY : Arm.NONE')
        self.assertNotEqual(where_extract.java_body(changed, 'public static Arm arm('), where_extract.ARM_SHAPE)

    def test_audio_mirror_agrees_with_the_known_arms(self):
        rows = {r['id']: r for r in where_extract.json.loads(where_extract.rd(where_extract.DESIGN))['troops']}
        self.assertEqual(where_extract.audio_arm(rows['ic_breaker']), 'HEAVY')
        self.assertEqual(where_extract.audio_arm(rows['knight']), 'BLADE')
        self.assertEqual(where_extract.audio_arm(rows['archer']), 'BOW')
        self.assertEqual(where_extract.audio_arm(rows['mage']), 'MAGIC')
        self.assertEqual(where_extract.audio_arm(rows['ic_colossus']), 'HEAVY')

if __name__ == '__main__':
    unittest.main()
