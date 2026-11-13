"""Checks tools/economy_model.py reads its one-off gold and Rare value from sheets/design.json, not from hand-set numbers."""
import copy
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
import economy_model as em


class EconomyModelSheetTest(unittest.TestCase):
    def setUp(self):
        self.data = em.sheets()

    def test_model_row_has_no_hand_set_chapter_values(self):
        self.assertNotIn('chapterGold', self.data['economyModel'][0])
        self.assertNotIn('chapterRareValue', self.data['economyModel'][0])

    def test_chapter_gold_is_wins_plus_bonuses_from_the_sheet(self):
        camp = self.data['campaign'][0]
        battles = [b for b in self.data['campaignBattles'] if b['chapter'] == 1]
        bonus = {b['id']: b['gold'] for b in self.data['bonusObjectives']}
        base, extra = em.chapter_gold(self.data)
        self.assertEqual(base, len(battles) * (camp['completionGold'] + camp['victoryGold']))
        self.assertEqual(extra, sum(bonus[b['bonus']] for b in battles))
        self.assertGreater(extra, 0)

    def test_chapter_gold_follows_sheet_edits(self):
        edited = copy.deepcopy(self.data)
        base, extra = em.chapter_gold(self.data)
        edited['campaign'][0]['victoryGold'] += 10
        for b in edited['bonusObjectives']:
            if b['id'] == 'o6_hold_all':
                b['gold'] += 25
        n = sum(1 for b in edited['campaignBattles'] if b['chapter'] == 1)
        self.assertEqual(em.chapter_gold(edited), (base + 10 * n, extra + 25))

    def test_rare_value_is_the_rare_chosen_price(self):
        edited = copy.deepcopy(self.data)
        for r in edited['rarities']:
            if r['id'] == 'rare':
                r['chosenPrice'] = 4
        self.assertEqual(em.chapter_rare_value(edited), 4 * em.WRIT_GOLD)   # the sheet price is in writs; the model counts gold-equivalents

    def test_prices_are_in_writs(self):
        rec = self.data['recruitment'][0]
        self.assertEqual((rec['rollCost'], rec['chosenCost']), (1, 3))
        self.assertTrue(all(r['chosenPrice'] in (0, 3) for r in self.data['rarities']))

    def test_time_to_first_epic_unchanged_by_the_writ_prices(self):
        # Before the war table army rework the sheet priced a roll at 100 gold and a chosen buy at 300; the seeded run's median hours were 27.27.
        # The new prices are the same numbers in writs (100 gold each), so the median must stay within 1 percent.
        _, _, _, hours, _, _ = em.simulate(self.data, 4909)
        self.assertAlmostEqual(hours['median'], 27.27, delta=0.01 * 27.27)

    def test_band_is_29_hours_within_12_percent(self):
        self.assertAlmostEqual(em.TARGET_HOURS[0], 25.52)
        self.assertAlmostEqual(em.TARGET_HOURS[1], 32.48)


if __name__ == '__main__':
    unittest.main()
