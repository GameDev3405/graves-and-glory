# Economy check, plan task 5.4 (2026-10-30)

Report only. No game rules, values or code were changed. Kort decides on any change.

- Source: worktree `C:/Users/kortt/Melty/bannerbound-econ`, branch `work/economy-check-5-4`, base `main` at `bd05fa2`. Values are the committed `sheets/design.json` of that commit (the uncommitted edits in the other checkout were not used).
- Command: `python tools/economy_model.py` (seed 4909, 20,000 runs), exit 0, plus a sensitivity script run outside the repo that imports the model's own functions unchanged.
- Model status: not broken and not stale against the sheet. It reads `economyModel`, `recruitment`, `rarities`, `troops` and `houses` live, and all six asserts pass (spec 4.9 figures within 3%).

## Verdicts

| Target | Result | Margin |
|---|---|---|
| 29 hour median to the full roster | Pass | Model 28.7 h (target 29, band 25 to 40): 0.3 h under the point target, 3.7 h above the band floor, 11.3 h below the ceiling |
| +12 percent cap | Pass, zero headroom | Star 3 is +12% for all four rarities, `recruitment.capPct` 0.12 |
| Campaign rewards | Miss (not built yet) | 1 of 6 battles in `campaignBattles`; no bonus-objective reward fields anywhere |
| Pact pace | Not measurable on this sheet; projected borderline | Pact lands after 1 win today; the planned six-battle chapter projects 1.5 to 3 h against a 0 to 2 h goal |

## 1. 29 hour median: pass

Model output (seed 4909):

- Income per hour: matches 400 (4 matches x (80 + 0.5 x 40)) + daily contracts 67 + weekly contract 25 = 492 gold.
- Gold to a full roster: House mean 2,938; five Houses mean 14,690, P10 11,000, median 14,700, P90 18,200.
- Offsets: 300 Founders gold and the tier-20 Rare (worth 300).
- Hours: lucky (P10) 21, median 29 (28.7), mean 29, unlucky (P90) 36.
- Seed spread: median 28.7 to 29.1 h over 10 other seeds.

Sheet values that drive the number (`economyModel`, `recruitment`, `rarities`):

- `rollCost` 100, `chosenCost` 300, `epicPity` 40; `rollWeight` 43 / 6 / 2 for Common / Rare / Epic. These set the gold side (about 27.5 rolls per House).
- `matchesPerHour` 4, `winRate` 0.5, `matchGold` 80, `winGold` 40, `dailyGold` 100, `weeklyGold` 150, `hoursPerPlayDay` 1.5, `playDaysPerWeek` 4. These set the income side.

Sensitivity (median hours; P90 in brackets). The 25 to 40 h band holds for 2.6 to 4.7 matches per hour:

| Change | Income | Median (P90) |
|---|---|---|
| Baseline | 492 | 28.7 (36) |
| 3 matches/h | 392 | 36.0 (45) |
| 3.5 matches/h | 442 | 31.9 (40) |
| 5 matches/h | 592 | 23.8 (30), below the band |
| Win rate 0.3 / 0.7 | 460 / 524 | 30.7 / 26.9 |
| No contracts | 400 | 35.2 (44) |

Parallel gates (not in the Monte Carlo; recomputed from the sheet and `RosterMath.matchRewards`):

- Renown V needs 30 points. A match pays 1 Renown to each House with 6 or more slots in the banner (`houseTraits` tier 1), plus 1 on a win (`campaign.collection` completion 1, victory 1). At 50% wins that is 1.5 per House-match, so 20 House-matches per House. Collecting one House at a time (about 5.8 h, 23 matches, per House) stays ahead of it. Fielding a single House per match for all four non-Pact Houses takes 20 h, still inside 29 h. At 30% wins it is 23 h.
- Shards: 50 per Legendary, 200 for four. Supply is about 10 per hour from matches (2 Houses fielded), 3.7 per hour from contracts, 120 from Founders shard tiers, plus dismantles of duplicates past star 3. Not binding.
- Founders offsets: 125 average XP a match plus contract XP is about 700 XP/h, so tier 20 (5,000 XP) lands near hour 7 and tier 27 near hour 10. Both offsets arrive well before hour 29.

## 2. +12 percent cap: pass, with no headroom

- `rarities.*.starPct2` 0.06, `starPct3` 0.12 for common, rare, epic and legendary; `recruitment.capPct` 0.12. Preflight and `HousesCheck` already assert `starPct3 <= capPct`. `Campaign` applies `min(1 + capPct, 1 + starPct)` from stars alone.
- No evolutions exist in the sheet; Mastery rewards are titles, skin and frame only (no stats).
- Raising any `starPct3`, or lowering `capPct`, fails preflight. A deliberate retune has to move both together.
- Observation for Kort, not a miss: battle levels (`levels`: +5% / +10% health and damage) and gear (`gearTiers`: +10% / +20% weapon and armour) sit outside the cap by the in-match progression design. Stacked with star 3, a level-3 soldier in tier III gear has up to 1.12 x 1.30 = 1.456, about +46% health and damage over base inside a battle. `VISION-AND-ROADMAP.md` still says stars and evolutions "never exceed about +12% ... in every mode". The in-match spec (section 2) and the Houses spec 4.4 explicitly exempt battle effects, so the code follows the later spec. Tasks 5.1 and 5.2 may want to confirm that is the intended reading.

## 3. Campaign rewards: miss (content not yet authored)

Sheet today:

- `campaign.collection`: `completionGold` 80, `victoryGold` 40, `practiceGold` 40, `minimumRewardSeconds` 180, shards 1 and 1, Renown 1 and 1. Campaign pays exactly the skirmish rate; there is no premium for campaign play.
- `campaignBattles` has one row (`march-1`, The Training Field). Plan 4D.1 calls for six.
- No bonus-objective reward exists: no field in `campaignBattles`, and nothing in `Campaign.java` or `RosterMath.java`. Houses spec 4.5 and plan 4D.3 call for a one-time +5 shards to the battle's House per bonus objective (30 shards across six battles), plus bonus Renown.

Effect on the economy: a cleared six-battle chapter pays about 720 gold (6 x (80 + 40) if all won), the same as six skirmishes, so the model's income assumption is neutral to campaign play. The missing 30 bonus shards are about 12 matches of shard income and do not move the 29 h figure. The campaign's economic weight is the Pact, below.

## 4. Pact pace: not measurable yet, projected borderline

- Mechanics: `RosterMath.pactChapters` pays one Pact when every `campaignBattles` row of a chapter has `order <= campaignCleared`. With one row, the Pact is granted on the first win of `march-1`, about 15 minutes in. That is faster than the plan and not representative.
- Planned six battles at 15 minutes per attempt (the model's 4 matches/h includes 3 minutes of camp): 1.5 h at a 100% clear rate, 1.67 h at 90%, 2.0 h at 75%, 2.5 h at 60%, 3.0 h at 50%. The goal in the 4.9 table is "campaign chapter 1 ... Pact spent" inside hours 0 to 2. At 20 minutes an attempt (Siege and Survival are likely longer than a 12 minute Domination match) the same goal needs a clear rate near 100%.
- Value of the Pact: it replaces 50 shards plus Renown V (about 20 House-matches, roughly 5 h of play in that House) for one Legendary. It adds no gold, so the Monte Carlo and the 29 h median do not depend on it.
- Sheet values that will drive it: `campaignBattles` row count, `size`, `difficulty`, `map`, and mode lengths. The bot difficulty of the early battles sets the clear rate.

## Findings and suggested directions (Kort decides)

1. The 29 h median passes and is stable across seeds, but it rests on 4 matches per hour. The band breaks below 2.6 or above 4.7 matches per hour. Measure real match plus camp time once Siege and Survival exist (5.2 and the six-player test) and rerun; no sheet change is suggested now.
2. Campaign economics are unfinished: bonus-objective shards and Renown have no sheet fields or code, and only 1 of 6 battles exists. Direction: when 4D.1 and 4D.3 are built, add the bonus fields to `campaignBattles` and decide whether campaign wins should pay above the skirmish rate (today they do not).
3. The model is correct but not linked to the game values for several inputs. `economyModel.matchGold` and `winGold` duplicate `campaign.collection.completionGold` and `victoryGold`; daily gold (30 / 40 / 30), weekly gold 150, the 3 and 10 contract shards, Founders gold tiers and the tier-20 Rare are hard-coded in `Progression.java`, not in `design.json`. They currently agree, but a retune of one will not move the other. Direction: move them to the sheet in the 5.3 pass (this also fits the plan's "no inline numbers" rule), or add a model assert that compares them.
4. Doc drift, not a model fault: Houses spec 4.9 still lists evolutions among the later goals; the sheet has none, and the model's goal table is already updated.
5. Cap stacking in battle (up to about +46% with levels and gear) differs from the vision text; see section 2.
