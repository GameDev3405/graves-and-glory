"""Fixture test for tools/progression_gate.py (no game, no Gradle): hand-made botsim results.csv and levels.csv sets with known shares and comeback rates.

Three Houses a, b and c (banners a_normal, b_normal, c_normal), six ordered pairs of 5 matches each (30 base matches a condition):

- OFF (levels off): every unordered pair splits 5 to 5, so every House has raw win rate 50% and Bradley-Terry share 50% (ratings equal by symmetry).
  Comeback tags over the 30 base matches in order: 6 won by the side trailing at 300 s, 1 tied at 300 s, 1 shorter than 300 s, the other 22 won by the leader.
  Extra rows: one aborted round and one draw (a vs b), which change no share (a draw is half a win, 5.5 of 11 is still 50%).
  Comeback pool: 30 - 1 tie - 1 short = 28 rounds, 6 comebacks = 21.43%.
- ON_SHARE_BIG: a wins 8 of 10 against b and against c, b and c split 5 to 5. Raw 80%, 35%, 35%; Bradley-Terry shares 80%, 35%, 35% (ln 4 gap, the weak prior moves it by hundredths).
  Deltas against OFF: +30, -15, -15 points: rule 1 fails.
- ON_CLOSE: a wins 6 of 10 against b and 5 of 10 against c, b and c split 5 to 5. Raw 55%, 45%, 50%; least-squares ratings give shares about 55%, 45%, 50%.
  Deltas about +5, -5, 0: rule 1 passes.
- A comeback tag set per ON set: 6 of 28 (same as OFF: passes), 5 of 28 (17.86%, 3.57 points lower: passes), 2 of 28 (7.14%, 14.29 points lower: fails).
- Ties at 300 s are left out of the pool (the trailing side is undefined); draws and aborted rounds are left out; rounds under 300 s are dropped from the pool but still count in the shares.
"""
import csv
import importlib.util
import pathlib
import subprocess
import sys
import tempfile
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]
TOOL = REPO / 'tools' / 'progression_gate.py'

RESULT_COLUMNS = ['match', 'map', 'dawnTier', 'duskTier', 'winner', 'dawnScore', 'duskScore', 'seconds', 'banner0', 'banner1',
                  'dawnKills', 'duskKills', 'score300_0', 'score300_1']
LEVEL_COLUMNS = ['match', 'team', 'banner', 'firstLevel2', 'firstLevel3', 'endShareLevel2', 'level3Count', 'gear']

PAIRS = [('a', 'b'), ('b', 'a'), ('a', 'c'), ('c', 'a'), ('b', 'c'), ('c', 'b')]


def winners(design):
    """design: for each ordered pair (x, y), how many of its 5 matches x (the seat-0 House) wins. Returns {pair: ['x'/'y' * 5]}."""
    return {pair: [pair[0]] * design[pair] + [pair[1]] * (5 - design[pair]) for pair in PAIRS}


# x wins out of 5 in the ordered pair, chosen so the unordered pair totals are exact (the pair (x, y) plus the pair (y, x)).
EVEN = {('a', 'b'): 3, ('b', 'a'): 3, ('a', 'c'): 3, ('c', 'a'): 3, ('b', 'c'): 3, ('c', 'b'): 3}   # each unordered pair 5 to 5
BIG = {('a', 'b'): 4, ('b', 'a'): 1, ('a', 'c'): 4, ('c', 'a'): 1, ('b', 'c'): 3, ('c', 'b'): 3}    # a wins 8 of 10 vs b and vs c
CLOSE = {('a', 'b'): 3, ('b', 'a'): 2, ('a', 'c'): 3, ('c', 'a'): 3, ('b', 'c'): 3, ('c', 'b'): 3}  # a: 6 of 10 vs b, 5 of 10 vs c


def build_matches(design, comebacks, extras=False):
    """The 30 base matches in order (pair by pair), then the optional aborted and draw rows. Tags by position:
    positions 0..comebacks-1 are won by the side trailing at 300 s, position 28 ties at 300 s, position 29 lasts 250 s, the rest are won by the leader."""
    matches = []
    for pair, wins in winners(design).items():
        for w in wins:
            matches.append((pair, w))
    rows = []
    for position, (pair, w) in enumerate(matches):
        seat = 0 if w == pair[0] else 1
        seconds = 900
        win_score, lose_score = 60, 50
        if position < comebacks:      # the winner trailed at 300 s
            s300 = (40, 55) if seat == 0 else (55, 40)
        elif position == 28:          # tie at 300 s
            s300 = (50, 50)
        else:                         # the winner led at 300 s
            s300 = (55, 40) if seat == 0 else (40, 55)
        if position == 29:
            seconds = 250
        finals = (win_score, lose_score) if seat == 0 else (lose_score, win_score)
        rows.append(dict(banner0=pair[0] + '_normal', banner1=pair[1] + '_normal', winner='dawn' if seat == 0 else 'dusk',
                         dawnScore=finals[0], duskScore=finals[1], seconds=seconds, score300_0=s300[0], score300_1=s300[1]))
    if extras:
        rows.append(dict(banner0='a_normal', banner1='b_normal', winner='aborted', dawnScore=3, duskScore=1, seconds=40, score300_0=3, score300_1=1))
        rows.append(dict(banner0='a_normal', banner1='b_normal', winner='draw', dawnScore=70, duskScore=70, seconds=900, score300_0=30, score300_1=45))
    return rows


def write_run(root, name, rows, levels=None):
    """One run directory root/name/botsim with results.csv (and levels.csv). Match numbers restart at 1 in every run, as the real files do."""
    folder = pathlib.Path(root) / name / 'botsim'
    folder.mkdir(parents=True)
    with open(folder / 'results.csv', 'w', newline='', encoding='utf-8') as handle:
        out = csv.DictWriter(handle, RESULT_COLUMNS, restval='0')
        out.writeheader()
        for number, row in enumerate(rows, 1):
            out.writerow(dict(row, match=number, map=number % 5, dawnTier='normal', duskTier='normal', dawnKills=0, duskKills=0))
    if levels is not None:
        with open(folder / 'levels.csv', 'w', newline='', encoding='utf-8') as handle:
            out = csv.DictWriter(handle, LEVEL_COLUMNS, restval='')
            out.writeheader()
            for line in levels:
                out.writerow(line)
    return folder.parent


def split_runs(rows, count):
    """Cut the rows into count runs, as the real round robin cuts a condition into one server run per ordered pair."""
    size = -(-len(rows) // count)
    return [rows[i:i + size] for i in range(0, len(rows), size)]


def level_rows(rows):
    """levels.csv lines for a run: two per match. first level 2 and 3 follow the row index (see the module text of the expectations below);
    the winner's side ends at share 0.4 with 4 level-3 soldiers, the loser's at 0.2 with 2. Aborted rounds carry 999 values that must not count."""
    lines = []
    for number, row in enumerate(rows, 1):
        for team, seat in ((0, 'dawn'), (1, 'dusk')):
            index = (number - 1) * 2 + team
            if row['winner'] == 'aborted':
                lines.append(dict(match=number, team=seat, banner=row['banner%d' % team], firstLevel2=999, firstLevel3=999, endShareLevel2='0.999', level3Count=99, gear=''))
                continue
            won = row['winner'] == seat
            lines.append(dict(match=number, team=seat, banner=row['banner%d' % team], firstLevel2='' if index % 11 == 10 else 100 + 10 * (index % 5),
                              firstLevel3='' if index % 3 else 150 + 10 * ((index // 3) % 3), endShareLevel2='0.400' if won else '0.200',
                              level3Count=4 if won else 2, gear=''))
    return lines


def blank_levels(rows):
    """levels.csv of a progression-off run: no first level 2 or 3, no end share, no level-3 soldiers, no gear."""
    return [dict(match=number, team=seat, banner=row['banner%d' % team], firstLevel2='', firstLevel3='', endShareLevel2='', level3Count=0, gear='')
            for number, row in enumerate(rows, 1) for team, seat in ((0, 'dawn'), (1, 'dusk'))]


def load_module():
    spec = importlib.util.spec_from_file_location('progression_gate', TOOL)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ProgressionGate(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        root = cls.tmp.name
        cls.gate = load_module()
        cls.off = [write_run(root, 'off%d' % i, part, blank_levels(part)) for i, part in enumerate(split_runs(build_matches(EVEN, 6, extras=True), 4))]
        cls.on_share_big = [write_run(root, 'big%d' % i, part, level_rows(part)) for i, part in enumerate(split_runs(build_matches(BIG, 6), 3))]
        cls.on_close = [write_run(root, 'close%d' % i, part, level_rows(part)) for i, part in enumerate(split_runs(build_matches(CLOSE, 5), 3))]
        cls.on_with_aborted = [write_run(root, 'abo%d' % i, part, level_rows(part)) for i, part in enumerate(split_runs(build_matches(CLOSE, 5, extras=True), 3))]
        cls.on_comeback_low = [write_run(root, 'low%d' % i, part, level_rows(part)) for i, part in enumerate(split_runs(build_matches(CLOSE, 2), 3))]

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def analyse(self, on):
        return self.gate.analyse(self.off, on)

    # ---- shares
    def test_off_shares_are_even(self):
        result = self.analyse(self.on_close)
        for house in ('a', 'b', 'c'):
            self.assertAlmostEqual(result['shares'][house]['off'], 0.5, delta=0.01, msg=house)  # the draw row moves the seat term by hundredths
            self.assertAlmostEqual(result['shares'][house]['raw_off'], 0.5, places=6, msg=house)

    def test_big_share_delta_fails_rule_one(self):
        result = self.analyse(self.on_share_big)
        shares = result['shares']
        self.assertAlmostEqual(shares['a']['raw_on'], 0.80, places=6)
        self.assertAlmostEqual(shares['b']['raw_on'], 0.35, places=6)
        self.assertAlmostEqual(shares['c']['raw_on'], 0.35, places=6)
        self.assertAlmostEqual(shares['a']['on'], 0.80, delta=0.02)
        self.assertAlmostEqual(shares['b']['on'], 0.35, delta=0.02)
        self.assertAlmostEqual(shares['a']['delta'], 0.30, delta=0.02)
        self.assertAlmostEqual(shares['b']['delta'], -0.15, delta=0.02)
        self.assertFalse(result['rules']['shares'])

    def test_close_share_delta_passes_rule_one(self):
        result = self.analyse(self.on_close)
        shares = result['shares']
        self.assertAlmostEqual(shares['a']['raw_on'], 0.55, places=6)
        self.assertAlmostEqual(shares['b']['raw_on'], 0.45, places=6)
        self.assertAlmostEqual(shares['c']['raw_on'], 0.50, places=6)
        for house in ('a', 'b', 'c'):
            self.assertLess(abs(shares[house]['delta']), 0.07, house)
        self.assertAlmostEqual(shares['a']['delta'], 0.05, delta=0.03)
        self.assertTrue(result['rules']['shares'])

    def test_a_house_outside_the_band_in_both_runs_is_reported_not_failed(self):
        # a: 50% off, 80% on (inside off); b and c: 50% off, 35% on. Nobody is outside 40-60 in both runs.
        self.assertEqual(self.analyse(self.on_share_big)['outside_both'], [])
        # the big set against itself: a at 80%, b and c at 35% in both runs, so all three are reported, and the share rule (delta 0) passes
        result = self.gate.analyse(self.on_share_big, self.on_share_big)
        self.assertEqual(result['outside_both'], ['a', 'b', 'c'])
        self.assertTrue(result['rules']['shares'])

    # ---- comeback
    def test_comeback_pool_counts(self):
        result = self.analyse(self.on_close)
        off, on = result['comeback']['off'], result['comeback']['on']
        self.assertEqual((off['won'], off['n']), (6, 28))
        self.assertAlmostEqual(off['rate'], 6 / 28, places=9)
        self.assertEqual((on['won'], on['n']), (5, 28))
        self.assertAlmostEqual(on['rate'], 5 / 28, places=9)
        # 30 base rows + aborted + draw in OFF: left out are 1 tie, 1 short round, 1 draw, 1 aborted
        self.assertEqual((off['ties'], off['short'], off['draws'], off['aborted']), (1, 1, 1, 1))
        self.assertEqual((on['ties'], on['short'], on['draws'], on['aborted']), (1, 1, 0, 0))

    def test_comeback_rule_passes_at_three_and_a_half_points_lower(self):
        result = self.analyse(self.on_close)
        self.assertAlmostEqual(result['comeback']['delta'], 5 / 28 - 6 / 28, places=9)
        self.assertTrue(result['rules']['comeback'])

    def test_comeback_rule_fails_at_fourteen_points_lower(self):
        result = self.analyse(self.on_comeback_low)
        self.assertAlmostEqual(result['comeback']['on']['rate'], 2 / 28, places=9)
        self.assertAlmostEqual(result['comeback']['delta'], 2 / 28 - 6 / 28, places=9)
        self.assertFalse(result['rules']['comeback'])
        self.assertTrue(result['rules']['shares'])

    def test_comeback_rule_passes_at_equal_rates_and_shares_fail_independently(self):
        result = self.analyse(self.on_share_big)
        self.assertAlmostEqual(result['comeback']['delta'], 0.0, places=9)
        self.assertTrue(result['rules']['comeback'])
        self.assertFalse(result['rules']['shares'])

    # ---- level timings (ON only)
    def test_timings(self):
        result = self.analyse(self.on_close)
        timings = result['timings']['on']
        # first level 2 over the sides that reached it: values 100..140 in equal numbers, 1 in 11 sides blank; the median is 120
        self.assertEqual(timings['first_level2_median'], 120)
        self.assertEqual(timings['first_level3_median'], 160)
        self.assertGreater(timings['first_level2_never'], 0)
        self.assertGreater(timings['first_level3_never'], 0)
        self.assertAlmostEqual(timings['winner_end_share'], 0.4, places=9)
        self.assertAlmostEqual(timings['loser_end_share'], 0.2, places=9)
        self.assertAlmostEqual(timings['level3_per_round'], 6.0, places=9)   # 4 + 2 in each decided round
        self.assertEqual(timings['rounds'], 30)
        self.assertEqual(timings['sides'], 60)
        off = result['timings']['off']
        self.assertIsNone(off['first_level2_median'])
        self.assertIsNone(off['first_level3_median'])
        self.assertEqual(off['level3_per_round'], 0.0)

    def test_aborted_rounds_do_not_count_in_timings(self):
        timings = self.gate.analyse(self.off, self.on_with_aborted)['timings']['on']
        # the aborted round carries 999 and 99 values: it must not count (31 rounds are left: 30 base matches and the draw)
        self.assertEqual((timings['rounds'], timings['sides']), (31, 62))
        self.assertLessEqual(timings['first_level2_max'], 140)
        self.assertLessEqual(timings['first_level3_max'], 170)
        self.assertLess(timings['level3_per_round'], 7)

    def test_ratings_match_the_balance_method_fit(self):
        """The gate keeps the covariance of tools/balance_method.py bt_fit; the ratings themselves must be the same numbers."""
        import numpy as np
        spec = importlib.util.spec_from_file_location('balance_method', REPO / 'tools' / 'balance_method.py')
        method = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(method)
        rounds = self.gate.load_condition(self.on_share_big)
        names = ['a', 'b', 'c']
        mine, _, h_mine = self.gate.bt_fit([r for r in rounds if r['winner'] != 'aborted'], names)
        rows = [(r['banner0'], r['banner1'], self.gate.result_of(r), 0.0) for r in rounds if r['winner'] != 'aborted']
        theirs, _, h_theirs, _, _ = method.bt_fit(rows, names)
        self.assertTrue(np.allclose(mine, theirs, atol=1e-9), (mine, theirs))
        self.assertAlmostEqual(h_mine, h_theirs, places=9)

    # ---- the command line
    def run_cli(self, on):
        command = [sys.executable, '-I', str(TOOL), '--off'] + [str(p) for p in self.off] + ['--on'] + [str(p) for p in on]
        done = subprocess.run(command, capture_output=True, text=True)
        return done.returncode, done.stdout + done.stderr

    def test_cli_pass_and_fail_lines(self):
        code, out = self.run_cli(self.on_close)
        self.assertEqual(code, 0, out)
        self.assertIn('gate progression shares=pass comeback=pass -> pass', out)
        code, out = self.run_cli(self.on_share_big)
        self.assertEqual(code, 1, out)
        self.assertIn('gate progression shares=fail comeback=pass -> fail', out)
        code, out = self.run_cli(self.on_comeback_low)
        self.assertEqual(code, 1, out)
        self.assertIn('gate progression shares=pass comeback=fail -> fail', out)

    def test_cli_unusable_input_exits_two(self):
        done = subprocess.run([sys.executable, '-I', str(TOOL), '--off', str(self.off[0]), '--on', str(pathlib.Path(self.tmp.name) / 'missing')], capture_output=True, text=True)
        self.assertEqual(done.returncode, 2, done.stdout + done.stderr)


if __name__ == '__main__':
    unittest.main()
