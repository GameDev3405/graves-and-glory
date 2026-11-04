"""Runs tools/preflight.py on a scratch copy of the repo with one gear cell broken at a time and asserts it refuses (no game, no Gradle).

Same harness as test_preflight_rules.py: the real Spec.java and sheets/preflight.json are never touched, and the first test runs the untouched sheet
through the copy so a refusal below is the gear rule's, not the copy's.
"""
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]

class PreflightGear(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.root = pathlib.Path(cls.tmp.name)
        (cls.root / 'tools').mkdir()
        shutil.copy(REPO / 'tools' / 'preflight.py', cls.root / 'tools' / 'preflight.py')
        (cls.root / 'sheets').mkdir()
        shutil.copy(REPO / 'build.gradle', cls.root / 'build.gradle')
        shutil.copytree(REPO / 'src' / 'main' / 'resources', cls.root / 'src' / 'main' / 'resources')
        game = cls.root / 'src' / 'main' / 'java' / 'gg' / 'gravesandglory'
        game.mkdir(parents=True)
        shutil.copy(REPO / 'src' / 'main' / 'java' / 'gg' / 'gravesandglory' / 'GravesAndGlory.java', game / 'GravesAndGlory.java')
        if (REPO / 'art' / 'troops').is_dir():
            shutil.copytree(REPO / 'art' / 'troops', cls.root / 'art' / 'troops')
        if (REPO / 'test-tools' / 'resources').is_dir():
            shutil.copytree(REPO / 'test-tools' / 'resources', cls.root / 'test-tools' / 'resources')
        cls.sheet = json.loads((REPO / 'sheets' / 'design.json').read_text())

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def run_preflight(self, mutate=None):
        data = json.loads(json.dumps(self.sheet))
        if mutate:
            mutate(data)
        (self.root / 'sheets' / 'design.json').write_text(json.dumps(data, indent=2))
        done = subprocess.run([sys.executable, '-I', str(self.root / 'tools' / 'preflight.py')], cwd=self.root, capture_output=True, text=True)
        return done.returncode, done.stdout + done.stderr

    def refuses(self, mutate, *words):
        code, out = self.run_preflight(mutate)
        self.assertNotEqual(code, 0, 'preflight accepted the broken cell')
        for word in words:
            self.assertIn(word, out)

    @staticmethod
    def row(data, sheet, rid):
        return next(r for r in data[sheet] if r['id'] == rid)

    def test_the_real_sheet_is_clean_with_gear(self):
        code, out = self.run_preflight()
        self.assertEqual(code, 0, out)
        self.assertIn('Preflight clean', out)

    def test_gear_rarities_order(self):
        self.refuses(lambda d: d['gearRarities'][1].__setitem__('id', 'rare'), 'gearRarities')

    def test_stat_ranges_rise(self):
        self.refuses(lambda d: self.row(d, 'gearRarities', 'epic').__setitem__('statMin', 9.0), 'statMin')

    def test_breakdown_rises(self):
        self.refuses(lambda d: self.row(d, 'gearRarities', 'rare').__setitem__('breakdownPct', 0.1), 'breakdownPct')

    def test_each_set_has_one_piece_per_armour_slot(self):
        self.refuses(lambda d: d['gearItems'].remove(self.row(d, 'gearItems', 'bruiser_legs')), 'bruiser', 'legs')

    def test_two_handed_only_main(self):
        self.refuses(lambda d: self.row(d, 'gearItems', 'kite_shield').__setitem__('hands', 2), 'hands')

    def test_item_id_charset(self):
        self.refuses(lambda d: self.row(d, 'gearItems', 'falchion').__setitem__('id', 'bad.id'), 'bad.id')

    def test_starter_weapons_resolve(self):
        self.refuses(lambda d: self.row(d, 'gearSets', 'bruiser').__setitem__('starterMain', 'nope'), 'starterMain')

    def test_every_class_maps_to_one_set(self):
        self.refuses(lambda d: self.row(d, 'gearSets', 'bruiser').__setitem__('classes', 'champion'), 'reaver')

    def test_every_class_has_a_relic(self):
        self.refuses(lambda d: self.row(d, 'relics', 'stand_fast').__setitem__('fromClass', '-'), 'champion')

    def test_forge_slots(self):
        self.refuses(lambda d: self.row(d, 'forge', 'forge').__setitem__('maxSlots', 115), 'maxSlots')
    def test_blacksmith_lines_required(self):
        self.refuses(lambda d: d['blacksmithLines'].clear(), 'blacksmithLines')

    def test_blacksmith_line_length(self):
        self.refuses(lambda d: d['blacksmithLines'][0].__setitem__('text', 'x' * 91), 'blacksmithLines', '90')

    # Gear combat (gear-combat plan Task 1). Rows are found by their links, never by catalogue names.
    def test_weapon_family_needs_a_row(self):
        def m(d):
            weapon = next(i for i in d['gearItems'] if i['slot'] == 'main')
            weapon['family'] = 'nofamily'
        self.refuses(m, 'nofamily')

    def test_set_bonus_set_resolves(self):
        self.refuses(lambda d: d['gearSetBonuses'][0].__setitem__('set', 'noset'), 'noset')

    def test_set_bonus_effect_known(self):
        self.refuses(lambda d: d['gearSetBonuses'][0].__setitem__('effect', 'noeffect'), 'noeffect')

    def test_set_needs_both_bonus_rows(self):
        def m(d):
            first = d['gearSetBonuses'][0]
            d['gearSetBonuses'] = [b for b in d['gearSetBonuses'] if not (b['set'] == first['set'] and b['pieces'] == 4)]
        self.refuses(m, '4-piece')

    def test_perk_item_resolves(self):
        self.refuses(lambda d: d['gearPerks'][0].__setitem__('item', 'noitem'), 'noitem')

    def test_perk_effect_known(self):
        self.refuses(lambda d: d['gearPerks'][0].__setitem__('effect', 'noeffect'), 'noeffect')

    def test_perk_is_fixed_per_item(self):
        self.refuses(lambda d: d['gearPerks'][1].__setitem__('item', d['gearPerks'][0]['item']), 'already has a perk')

    def test_relic_tuning_effect_known(self):
        self.refuses(lambda d: d['relicTuning'][0].__setitem__('effect', 'noeffect'), 'noeffect')

    def test_relic_tuning_id_resolves(self):
        self.refuses(lambda d: d['relicTuning'][0].__setitem__('id', 'norelic'), 'norelic')

    def test_every_relic_has_tuning(self):
        def m(d):
            gone = d['relicTuning'].pop()
            m.gone = gone['id']
        self.refuses(m, 'tuning rows')

    def test_gear_combat_one_row(self):
        self.refuses(lambda d: d['gearCombat'].append(dict(d['gearCombat'][0], id='second')), 'gearCombat')

    def test_ported_relics_copy_the_classes(self):
        sheet = self.sheet
        if 'classes' not in sheet:
            self.skipTest('classes removed (Task 7); the copy was checked when it was made')
        for r in sheet['relics']:
            if r['fromClass'] == '-':
                continue
            c = next(c for c in sheet['classes'] if c['id'] == r['fromClass'])
            t = next(t for t in sheet['relicTuning'] if t['id'] == r['id'])
            self.assertEqual(t['effect'], c['active'])
            for col in ('durationTicks', 'radius', 'range', 'value1', 'value2', 'value3'):
                self.assertEqual(t[col], c[col], r['id'] + '/' + col)

if __name__ == '__main__':
    unittest.main()
