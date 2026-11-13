"""The campaign's preflight rules (chapter 1 "The Oath", build brief A and B): ally rows, enemies per battle, after lines, the chapter
sheet (banner, season, chapter end), and the skirmish and practice bot pools, which the chapter must leave exactly as they were.
Each rule has a negative case run on a copy of preflight.py in a temporary tree, as test_preflight_houses does; the real Spec.java and
sheets/preflight.json are never written."""
import json, pathlib, shutil, subprocess, sys, tempfile, unittest

REAL = pathlib.Path(__file__).resolve().parents[2]

# The bot pools of every tier before chapter 1 (main at a7811c4d): Campaign.fieldableBotBanners, in sheet order. Skirmish and practice
# draw from these; the chapter's ally rows and the retired march-6 ally rows must never appear here.
POOLS_BEFORE = {
    'easy': ['gilded_easy', 'graveborn_easy', 'wildmoot_easy', 'ironclad_easy', 'tidecallers_easy', 'mix_gilded_graveborn_easy', 'mix_ironclad_tidecallers_easy', 'mix_wildmoot_gilded_easy'],
    'normal': ['gilded_normal', 'graveborn_normal', 'wildmoot_normal', 'ironclad_normal', 'tidecallers_normal', 'mix_gilded_graveborn_normal', 'mix_ironclad_tidecallers_normal', 'mix_wildmoot_gilded_normal'],
    'hard': ['gilded_hard', 'graveborn_hard', 'wildmoot_hard', 'ironclad_hard', 'tidecallers_hard', 'mix_gilded_graveborn_hard', 'mix_ironclad_tidecallers_hard', 'mix_wildmoot_gilded_hard'],
}


def pools(data):
    """Campaign.fieldableBotBanners for each tier: rows of the tier whose troops are all of fieldable Houses, less the rows a battle names as
    allies or a chapter names as its House banner (MarchMath.reserved)."""
    fieldable = {h['id'] for h in data['houses'] if h['fieldable']}
    house_of = {t['id']: t['house'] for t in data['troops']}
    reserved = {a for r in data['campaignBattles'] if r['allies'] != 'none' for a in r['allies'].split(',')}
    reserved |= {c['bannerRow'] for c in data.get('campaignChapters', []) if c['bannerRow'] != 'none'}
    return {tier: [b['id'] for b in data['botBanners'] if b['difficulty'] == tier and b['id'] not in reserved
                   and all(s == '' or house_of.get(s) in fieldable for s in b['slots'].split(','))]
            for tier in POOLS_BEFORE}


class PreflightCampaign(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = pathlib.Path(tempfile.mkdtemp(prefix='preflight-campaign-'))
        (cls.tmp / 'tools').mkdir()
        shutil.copy(REAL / 'tools' / 'preflight.py', cls.tmp / 'tools' / 'preflight.py')
        shutil.copytree(REAL / 'sheets', cls.tmp / 'sheets')
        shutil.copytree(REAL / 'src' / 'main' / 'resources', cls.tmp / 'src' / 'main' / 'resources')
        shutil.copytree(REAL / 'test-tools' / 'resources', cls.tmp / 'test-tools' / 'resources')
        shutil.copy(REAL / 'build.gradle', cls.tmp / 'build.gradle')
        game = cls.tmp / 'src' / 'main' / 'java' / 'gg' / 'gravesandglory'
        game.mkdir(parents=True, exist_ok=True)
        shutil.copy(REAL / 'src' / 'main' / 'java' / 'gg' / 'gravesandglory' / 'GravesAndGlory.java', game / 'GravesAndGlory.java')
        if (REAL / 'art' / 'troops').is_dir():
            shutil.copytree(REAL / 'art' / 'troops', cls.tmp / 'art' / 'troops')
        cls.design = json.loads((REAL / 'sheets' / 'design.json').read_text())

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.tmp, ignore_errors=True)

    def run_with(self, mutate):
        data = json.loads(json.dumps(self.design))
        mutate(data)
        (self.tmp / 'sheets' / 'design.json').write_text(json.dumps(data))
        r = subprocess.run([sys.executable, str(self.tmp / 'tools' / 'preflight.py')], capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    def refused(self, mutate, needle):
        code, out = self.run_with(mutate)
        self.assertNotEqual(code, 0, 'preflight passed a sheet it should refuse')
        self.assertIn(needle, out)

    @staticmethod
    def battle(data, id):
        return next(r for r in data['campaignBattles'] if r['id'] == id)

    @staticmethod
    def chapter(data, n):
        return next(c for c in data['campaignChapters'] if c['chapter'] == n)

    # --- the shipped sheet -------------------------------------------------------------------------------------------------------------
    def test_clean_sheet_passes(self):
        code, out = self.run_with(lambda d: None)
        self.assertEqual(code, 0, out)

    def test_bot_pools_unchanged(self):
        self.assertEqual(pools(self.design), POOLS_BEFORE)

    def test_chapter_one_rows(self):
        rows = self.design['campaignBattles']
        self.assertEqual([r['id'] for r in rows], ['oath-%d' % i for i in range(1, 7)])
        self.assertFalse(any(b['id'].startswith('b') and b['id'][1:2].isdigit() for b in self.design['bonusObjectives']), 'no march-* bonus id is reused')
        ids = {b['id'] for b in self.design['botBanners']}
        self.assertFalse(ids & {'mix_gilded_ironclad_wildmoot_normal', 'mix_tidecallers_graveborn_normal'}, 'the retired march-6 ally rows are gone')
        self.assertTrue({'ally_gilded_easy', 'ally_gilded_normal', 'ally_gilded_normal_b', 'ally_ironclad_normal'} <= ids)
        self.assertTrue(all(r['house'] == 'gilded' and r['chapter'] == 1 for r in rows))
        self.assertEqual([r['ebb'] for r in rows], [False, False, False, True, True, True])
        self.assertEqual(self.battle(self.design, 'oath-6')['enemies'], 'wildmoot_hard,wildmoot_normal,wildmoot_normal')
        kinds = {b['id']: b['kind'] for b in self.design['bonusObjectives']}
        self.assertEqual(kinds[self.battle(self.design, 'oath-6')['bonus']], 'hold_all')
        c = self.chapter(self.design, 1)
        self.assertEqual((c['name'], c['banner'], c['season'], c['house']), ('The Oath', 'own', 1, 'gilded'))

    # --- ally rows (A2, A3) ------------------------------------------------------------------------------------------------------------
    def test_ally_id_shape(self):
        def bad(d):
            row = next(b for b in d['botBanners'] if b['id'] == 'ally_gilded_normal_b')
            row['id'] = 'ally_gilded_normal_c'
            self.battle(d, 'oath-5')['allies'] = 'ally_gilded_normal,ally_gilded_normal_c'
            self.battle(d, 'oath-6')['allies'] = 'ally_gilded_normal,ally_gilded_normal_c'
        self.refused(bad, 'botBanners/ally_gilded_normal_c: id must be')

    def test_ally_row_only_in_allies(self):
        self.refused(lambda d: self.battle(d, 'oath-2').update(allies='ally_gilded_normal'),'botBanners/ally_gilded_easy: an ally row must be named in some battle\'s allies')

    def test_ally_row_never_an_enemy(self):
        self.refused(lambda d: self.battle(d, 'oath-1').update(enemies='ally_gilded_easy'), 'campaignBattles/oath-1: enemies ally_gilded_easy is an ally row')

    # --- enemies (N1) ------------------------------------------------------------------------------------------------------------------
    def test_enemies_resolve(self):
        self.refused(lambda d: self.battle(d, 'oath-1').update(enemies='tidecallers_medium'), 'campaignBattles/oath-1: enemies tidecallers_medium is not a botBanners id')

    def test_enemies_count_matches_size(self):
        self.refused(lambda d: self.battle(d, 'oath-3').update(enemies='tidecallers_normal'), 'campaignBattles/oath-3: enemies names 1 rows; it needs 2, one per enemy seat (size)')

    def test_enemies_none_allowed(self):
        code, out = self.run_with(lambda d: self.battle(d, 'oath-1').update(enemies='none'))
        self.assertEqual(code, 0, out)

    # --- after and chapterEnd (N2) -----------------------------------------------------------------------------------------------------
    def test_after_rules(self):
        cases = [('x' * 161, '161 characters'), ('Café lights.', 'ASCII'), ('Hold fast!', 'exclamation'),
                 ('Hold - fast.', 'dash'), ('Hold -- fast.', 'dash'), ('Hold fast -', 'dash')]
        for text, needle in cases:
            with self.subTest(text=text):
                self.refused(lambda d: self.battle(d, 'oath-2').update(after=text), needle)

    def test_after_allows_keys_and_hyphenated_words(self):
        code, out = self.run_with(lambda d: self.battle(d, 'oath-2').update(after='Press {key:menu} to read the boar-kin oath.'))
        self.assertEqual(code, 0, out)

    def test_chapter_end_rules(self):
        self.refused(lambda d: self.chapter(d, 1).update(chapterEnd='The lamps flicker!'), 'campaignChapters/oath/chapterEnd')

    # --- chapters (G1, G2) -------------------------------------------------------------------------------------------------------------
    def test_every_battle_chapter_has_a_row(self):
        self.refused(lambda d: self.battle(d, 'oath-6').update(chapter=2), 'campaignBattles/oath-6: chapter 2 has no campaignChapters row')

    def test_banner_kind(self):
        self.refused(lambda d: self.chapter(d, 1).update(banner='mine'), 'campaignChapters/oath: banner must be own or house')

    def test_house_banner_needs_its_row(self):
        self.refused(lambda d: self.chapter(d, 1).update(banner='house'), 'campaignChapters/oath: a house banner names a botBanners row of its House')
        self.refused(lambda d: self.chapter(d, 1).update(banner='house', bannerRow='wildmoot_normal'), 'campaignChapters/oath: a house banner names a botBanners row of its House')

    def test_house_banner_row_is_reserved(self):
        def house(d):
            d['botBanners'].append(dict(next(b for b in d['botBanners'] if b['id'] == 'gilded_normal'), id='ally_gilded_hard'))
            d['botBanners'][-1]['difficulty'] = 'hard'
            self.chapter(d, 1).update(banner='house', bannerRow='ally_gilded_hard')
        code, out = self.run_with(house)
        self.assertEqual(code, 0, out)

    def test_seasons_climb_with_chapters(self):
        def two(d):
            d['campaignChapters'].append(dict(self.chapter(d, 1), id='second', chapter=2, season=0))
            self.battle(d, 'oath-6')['chapter'] = 2
        self.refused(two, 'campaignChapters/second: season 0')

    def test_own_banner_has_no_row(self):
        self.refused(lambda d: self.chapter(d, 1).update(bannerRow='gilded_normal'), 'campaignChapters/oath: an own banner names no bannerRow')


if __name__ == '__main__':
    unittest.main()
