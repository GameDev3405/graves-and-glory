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
            if b['id'] == 'b6_kill_hollowking':
                b['gold'] += 25
        n = sum(1 for b in edited['campaignBattles'] if b['chapter'] == 1)
        self.assertEqual(em.chapter_gold(edited), (base + 10 * n, extra + 25))

    def test_rare_value_is_the_rare_chosen_price(self):
        edited = copy.deepcopy(self.data)
        for r in edited['rarities']:
            if r['id'] == 'rare':
                r['chosenPrice'] = 450
        self.assertEqual(em.chapter_rare_value(edited), 450)

    def test_band_is_29_hours_within_12_percent(self):
        self.assertAlmostEqual(em.TARGET_HOURS[0], 25.52)
        self.assertAlmostEqual(em.TARGET_HOURS[1], 32.48)


if __name__ == '__main__':
    unittest.main()
