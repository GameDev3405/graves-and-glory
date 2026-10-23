"""The worked economy model of the houses design, section 4.9: hours to a full roster at one star, as a seeded Monte Carlo
driven by sheets/design.json (the NOT_SHIPPED economyModel row, with the recruitment, rarities, troops and houses sheets).
Never shipped; balance passes rerun it after editing the sheet.

  python tools/economy_model.py             run economyModel.runs players, print the tables, assert the spec 4.9 figures
  python tools/economy_model.py --seed N    the same with another seed (the asserts are the seeded run's; another seed may miss)

The policy (spec 4.9): for each House in turn the player rolls that House (recruitment.rollCost a roll) until its Epic drops,
the Epic forced on roll recruitment.epicPity, and then buys each Rare of it still missing for recruitment.chosenCost (a new
commander owns every Common already, houses design 6.2). Draws follow rarities.rollWeight per troop; Legendaries are never in a pool (the Pact and Renown give
them). There are no evolutions (in-match progression design, section 10), so the roster figures are the whole cost.

Asserts, each within 3%: per-House mean gold 2,920; five-House mean 14,700, 10th percentile 11,000, median 14,800 and 90th
percentile 18,200. Exit 1 naming the first that misses."""
import argparse
import json
import pathlib
import random
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
TOLERANCE = 0.03
EXPECTED = {'house mean': 2920, 'five-House mean': 14700, 'P10': 11000, 'median': 14800, 'P90': 18200}
TARGET_HOURS = (25, 40)   # spec 4.9: a median player completes the roster inside 25 to 40 hours
# The "next visible goal" table of spec 4.9 for a median player, with the in-match progression design's section 10 rows.
NEXT_GOALS = (
    ('0 to 2', 'campaign chapter 1, 10 Commons owned, Pact spent', 'first Rare (300 gold or rolls)'),
    ('2 to 6', '2 to 4 Rares, first stars on Commons', 'first Epic (pity counter shown)'),
    ('6 to 12', 'first House complete except its Legendary, Renown III', 'Renown V in that House'),
    ('12 to 20', 'second and third Houses, Legendary #2', 'star 3 on a favourite'),
    ('20 to 29', 'last Epics, Legendaries #3 to #5', 'Mastery II veteran trim, star 3 on favourites'),
    ('29+', 'roster complete', 'star 3 across a House, Mastery II and III'),
)


def sheets():
    return json.loads((ROOT / 'sheets/design.json').read_text(encoding='utf-8'))


def pools(data):
    """Per House, in houses order: [(troop id, rarity, weight)] of its rollable troops (every rarity with a roll weight)."""
    weight = {r['id']: r['rollWeight'] for r in data['rarities']}
    out = []
    for h in data['houses']:
        rows = [(t['id'], t['rarity'], weight[t['rarity']]) for t in data['troops'] if t['house'] == h['id'] and weight[t['rarity']] > 0]
        out.append((h['id'], rows))
    return out


def house_gold(rng, pool, roll_cost, chosen_cost, pity):
    """One House by the policy: rolls until the Epic, then a chosen buy for each Rare still missing. Returns the gold."""
    total = sum(w for _, _, w in pool)
    owned, rolls = {t for t, r, _ in pool if r == 'common'}, 0   # a new commander owns every Common (houses design 6.2)
    while True:
        rolls += 1
        if rolls >= pity:
            troop = next(t for t, r, _ in pool if r == 'epic')
        else:
            pick, troop = rng.randrange(total), None
            for t, _, w in pool:
                if pick < w:
                    troop = t
                    break
                pick -= w
        owned.add(troop)
        if any(t == troop and r == 'epic' for t, r, _ in pool):
            break
    missing = sum(1 for t, r, _ in pool if r == 'rare' and t not in owned)
    return rolls * roll_cost + missing * chosen_cost


def percentile(sorted_values, p):
    """Nearest-rank percentile of a sorted list."""
    k = max(0, min(len(sorted_values) - 1, int(round(p / 100.0 * len(sorted_values) + 0.5)) - 1))
    return sorted_values[k]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--seed', type=int, default=4909)
    args = ap.parse_args()
    data = sheets()
    m = data['economyModel'][0]
    rec = data['recruitment'][0]
    roll_cost, pity = rec['rollCost'], rec['epicPity']
    chosen = rec['chosenCost']
    houses = pools(data)
    rng = random.Random(args.seed)

    runs, per_house = [], []
    for _ in range(m['runs']):
        gold = 0
        for _, pool in houses:
            g = house_gold(rng, pool, roll_cost, chosen, pity)
            per_house.append(g)
            gold += g
        runs.append(gold)
    runs.sort()
    got = {'house mean': sum(per_house) / len(per_house), 'five-House mean': sum(runs) / len(runs),
           'P10': percentile(runs, 10), 'median': percentile(runs, 50), 'P90': percentile(runs, 90)}

    match_income = m['matchesPerHour'] * (m['matchGold'] + m['winRate'] * m['winGold'])
    daily_income = m['dailyGold'] / m['hoursPerPlayDay']
    weekly_income = m['weeklyGold'] / (m['hoursPerPlayDay'] * m['playDaysPerWeek'])
    per_hour = match_income + daily_income + weekly_income
    offsets = m['chapterGold'] + m['chapterRareValue']

    print('Economy model (houses design 4.9): %d runs, seed %d, %d Houses, roll %d gold, chosen buy %d gold, Epic pity %d'
          % (m['runs'], args.seed, len(houses), roll_cost, chosen, pity))
    print('Income per hour: matches %.0f + daily contracts %.0f + weekly contract %.0f = %.0f gold (chapter gold a one-off)'
          % (match_income, daily_income, weekly_income, per_hour))
    print('Gold for one House: mean %.0f; five Houses: mean %.0f, P10 %d, median %d, P90 %d'
          % (got['house mean'], got['five-House mean'], got['P10'], got['median'], got['P90']))
    print('One-off offsets: %d chapter gold and the tier-20 Rare worth %d' % (m['chapterGold'], m['chapterRareValue']))
    print()
    print('| Player | Gold needed | Hours |')
    print('|---|---|---|')
    hours = {}
    for label, key in (('Lucky (10th percentile)', 'P10'), ('Median', 'median'), ('Mean', 'five-House mean'), ('Unlucky (90th percentile)', 'P90')):
        need = got[key] - offsets
        hours[key] = need / per_hour
        print('| %s | %s | %d |' % (label, format(int(round(need / 100.0)) * 100, ','), round(hours[key])))
    print()
    print('| Hour | What just happened | Next goal on screen |')
    print('|---|---|---|')
    for row in NEXT_GOALS:
        print('| %s | %s | %s |' % row)
    print()

    failures = []
    for key, want in EXPECTED.items():
        ok = abs(got[key] - want) <= TOLERANCE * want
        print('assert %-16s %8.0f  want %6d within 3%%: %s' % (key, got[key], want, 'pass' if ok else 'FAIL'))
        if not ok:
            failures.append(key)
    ok = TARGET_HOURS[0] <= hours['median'] <= TARGET_HOURS[1]
    print('assert median hours     %8.1f  want %d to %d: %s' % (hours['median'], TARGET_HOURS[0], TARGET_HOURS[1], 'pass' if ok else 'FAIL'))
    if not ok:
        failures.append('median hours')
    if failures:
        print('economy_model: FAIL ' + ', '.join(failures))
        sys.exit(1)
    print('economy_model: all asserts pass')


if __name__ == '__main__':
    main()
