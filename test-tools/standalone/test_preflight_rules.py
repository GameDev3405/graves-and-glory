"""Runs tools/preflight.py on a scratch copy of the repo with one sheet cell broken at a time and asserts it refuses (no game, no Gradle).

The copy holds only what preflight reads (the sheet, the assets, the troop art json, the studio scripts, GravesAndGlory.java, build.gradle), so the real Spec.java and
sheets/preflight.json are never touched. The first test runs the untouched sheet through the same copy, so a refusal below is the rule's, not the copy's.
"""
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]

class PreflightRules(unittest.TestCase):
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
        # Phase 3's structure rules read the studio scripts (test-tools/resources/data/gravesandglory_studio): the copy holds them too (merge-prep)
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

    def bot(self, data, tier='normal'):
        return next(b for b in data['botCommand'] if b['id'] == tier)

    def test_the_real_sheet_is_clean_in_the_copy(self):
        code, out = self.run_preflight()
        self.assertEqual(code, 0, out)
        self.assertIn('Preflight clean', out)

    def refuses(self, mutate, *words):
        code, out = self.run_preflight(mutate)
        self.assertNotEqual(code, 0, 'preflight accepted the broken cell')
        for word in words:
            self.assertIn(word, out)

    def test_gear_priority_must_be_banner_role_or_open(self):
        for bad in ('rolle', 'Role', 'none', 'banner '):
            with self.subTest(priority=bad):
                self.refuses(lambda d, bad=bad: self.bot(d).__setitem__('gearPriority', bad), 'botCommand/normal', 'gearPriority')

    def test_gear_reserve_troops_may_not_be_negative(self):
        for bad in (-1, -3):
            with self.subTest(reserve=bad):
                self.refuses(lambda d, bad=bad: self.bot(d, 'hard').__setitem__('gearReserveTroops', bad), 'botCommand/hard', 'gearReserveTroops')

    def test_gear_reserve_troops_is_a_whole_number(self):
        self.refuses(lambda d: self.bot(d, 'easy').__setitem__('gearReserveTroops', 2.5), 'botCommand/easy', 'gearReserveTroops')

    def test_gear_max_tier_runs_one_to_three(self):
        for bad in (0, 4, -1):
            with self.subTest(tier=bad):
                self.refuses(lambda d, bad=bad: self.bot(d, 'easy').__setitem__('gearMaxTier', bad), 'botCommand/easy', 'gearMaxTier')

    def test_every_difficulty_is_checked(self):
        for tier in ('easy', 'normal', 'hard'):
            with self.subTest(tier=tier):
                self.refuses(lambda d, tier=tier: self.bot(d, tier).__setitem__('gearPriority', 'x'), 'botCommand/' + tier)

    # ---- the animation rework: troop families, attack variants, Hold's wall (Shieldwall is gone) ----
    def troop(self, data, tid):
        return next(t for t in data['troops'] if t['id'] == tid)

    def test_family_must_be_known(self):
        self.refuses(lambda d: self.troop(d, 'knight').__setitem__('family', 'lance'), 'knight', 'family', 'lance')

    def test_melee_family_needs_variants(self):
        def drop(d):
            d['attackVariants'] = [r for r in d['attackVariants'] if r['family'] != 'sword_shield']
        self.refuses(drop, 'sword_shield', 'attackVariants')

    def test_variant_impact_ticks_positive(self):
        for bad in (0, -1, 2.5):
            with self.subTest(impactTicks=bad):
                self.refuses(lambda d, bad=bad: d['attackVariants'][0].__setitem__('impactTicks', bad), 'attackVariants', 'impactTicks')

    def test_variant_family_must_be_used_by_a_troop(self):
        # Vacate a known family: the harpooner joins the bows, so the thrower rows belong to no troop.
        self.refuses(lambda d: self.troop(d, 'tc_harpooner').__setitem__('family', 'bow'), 'thrower_throw', 'used by no troop')

    def test_variant_family_must_be_known(self):
        def orphan(d):
            row = dict(d['attackVariants'][0])
            row['id'] = 'lance_thrust'
            row['family'] = 'lance'
            d['attackVariants'].append(row)
        self.refuses(orphan, 'lance_thrust', 'family')

    def test_shieldwall_formation_rejected(self):
        def wall(d):
            if not any(f['id'] == 'shieldwall' for f in d['formations']):
                row = dict(d['formations'][0])
                row['id'] = 'shieldwall'
                row['label'] = 'Shieldwall'
                d['formations'].append(row)
        self.refuses(wall, 'shieldwall', 'Hold')

    def test_combat_discipline_new_bounds(self):
        bad = {'shieldWallCell': (0.5, 1.31), 'crawlShare': (0.0, 1.0), 'fireMemoryTicks': (0,), 'volleyPeakPerBlock': (0.0, -0.1),
               'volleyPeakMax': (0.0,), 'crossbowArcScale': (0.0, 1.01), 'thrownArcScale': (0.0, 1.01), 'meleeReachGrace': (-0.01,),
               'corpseLieTicks': (-1,), 'corpseFadeTicks': (0,), 'arcTravelScale': (0.99, 2.01), 'heavyHitShare': (0.10, 0.05)}
        for col, values in bad.items():
            for value in values:
                with self.subTest(column=col, value=value):
                    self.refuses(lambda d, col=col, value=value: d['combatDiscipline'][0].__setitem__(col, value), 'combatDiscipline', col)
        # mediumHitShare's own bounds (heavyHitShare raised out of the way so only the medium bound is broken)
        for value in (0.0, 1.0):
            with self.subTest(column='mediumHitShare', value=value):
                def medium(d, value=value):
                    d['combatDiscipline'][0]['mediumHitShare'] = value
                    d['combatDiscipline'][0]['heavyHitShare'] = 0.99
                self.refuses(medium, 'combatDiscipline', 'mediumHitShare')
        # a tick column must be a whole number
        for col in ('fireMemoryTicks', 'corpseLieTicks', 'corpseFadeTicks'):
            with self.subTest(column=col, value='non-int'):
                self.refuses(lambda d, col=col: d['combatDiscipline'][0].__setitem__(col, 40.5), 'combatDiscipline', col, 'whole number')
        # a missing column
        for col in ('shieldWallCell', 'heavyHitShare'):
            with self.subTest(column=col, value='missing'):
                self.refuses(lambda d, col=col: d['combatDiscipline'][0].pop(col), 'combatDiscipline', 'missing', col)

if __name__ == '__main__':
    unittest.main(verbosity=2)
