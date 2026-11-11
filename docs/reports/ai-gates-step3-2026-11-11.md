# Bot AI step 3: balance gates on the current code (2026-11-11)

Measurement only, no gameplay code changed. Branch ai/lead at e75a1371, `-PsimFast=true`, `GRAVES_BOTSIM_PROBE=1`, 10 matches per run, Domination on the 300 s sim clock with maps 0-4 cycled twice (Rivergate, Ironhold, Frostmarch, Hollowwood, Kingsfield), Convoy on map 2 with the 420 s clock. Roots are `work/method/ai4-{dom,domswap,dommirror,convoy}-*` (not committed). Win rates carry a 95% Wilson interval; with n=10 an interval is about 50 points wide.

## Domination: wins (map pairs are Dawn-Dusk, two matches each)

| Run | Dawn | Dusk | House split | Rivergate | Ironhold | Frostmarch | Hollowwood | Kingsfield |
|---|---|---|---|---|---|---|---|---|
| 1. Gilded (Dawn) v Graveborn (Dusk) | 5 | 5 | Gilded 5, Graveborn 5 | 2-0 | 0-2 | 1-1 | 2-0 | 0-2 |
| 2. Graveborn (Dawn) v Gilded (Dusk) | 4 | 6 | Gilded 6, Graveborn 4 | 0-2 | 0-2 | 1-1 | 1-1 | 2-0 |
| 3. Gilded mirror | 2 | 8 | n/a | 0-2 | 0-2 | 1-1 | 0-2 | 1-1 |

Pooled: Gilded v Graveborn 11-9 (55%, 34-74%); Dusk 19 of 30 (63%, 46-78%); Dusk off Ironhold 13 of 24 (54%, 35-72%); Ironhold Dusk 6 of 6 (14 of 14 with the 8 of 8 on 2026-11-09). Gilded by map, runs 1+2: Rivergate 4/4, Hollowwood 3/4, Ironhold 2/4 (always the Dusk seat), Frostmarch 2/4, Kingsfield 0/4.

## bot_quality headlines

| Run | trickle | idle (not Hold) | Hold idle | blind picks | objective share | tick p99 (mean) |
|---|---|---|---|---|---|---|
| 1 dom | 20.5% | 8.2% | 5.8% | 2385/31517 (7.6%) | 78.6% | 20.0 ms (4.86) |
| 2 domswap | 22.6% | 8.1% | 7.1% | 2735/32956 (8.3%) | 75.6% | 9.3 ms (3.00) |
| 3 mirror | 22.9% | 6.5% | 4.3% | 3078/42441 (7.3%) | 77.7% | 7.6 ms (2.29) |
| 4 convoy | 16.0% | 20.5% | 0.0% | 285/70320 (0.4%) | 88.1% | 4.4 ms (1.20) |

Trickle (target under 10%), idle (under 5%) and blind picks (0) fail in every run; objective share passes. Run 1's 20 ms p99 is a single worst match (run 2 and 3 on the same code give 9.3 and 7.6 ms), so read tick p99 as noisy, likely host load from other worktrees; I did not check. Run-to-run trickle spread here is 20.5-22.9% (2.4 points) on 10-match runs, smaller than the 8 points seen at 4 matches.

## Convoy (Dawn escorts; Dusk defends)

Escort won 0 of 10 (0-28%), 0 checkpoints in 10 of 10, as before. Escort distance (max road distance, blocks): mean 191, best 240 of the roughly 262 to checkpoint 1, 8 of 10 matches over 168 (2026-11-09: mean 153, best 231, 5 of 10 over 150). Wagon contested share (5 s samples with a defender at the wagon): mean 31% (range 18-61%), down from 46% (20-66%). Pushed share about 78%. Escort kills are 120-130 against 30-59 in 9 of 10 matches, so the escort wins the fights and still does not arrive.

## Verdict

- **Domination: not a House problem.** Gilded beat Graveborn 11 of 20, not 16 of 16, and the winner follows the map and seat, not the House: Gilded took Rivergate 4/4 and Graveborn took Kingsfield 4/4 whichever seat each held. The 2026-11-09 sweep was largely a pre-step-2 bot effect.
- **Ironhold is a spawn/map problem, confirmed.** Dusk won every Ironhold match in all three runs (6/6, 14/14 over both days), with either House and in the mirror, matching the roughly 150 block spawn gap. Off Ironhold Dusk is 54%, a fair split. Fix the spawn, then re-run.
- **Mirror: fails, mainly from Ironhold.** Dusk won 8 of 10 (49-94%); the earlier pass was 4-4 with 2 draws. Excluding Ironhold Dusk still leads 6 of 8. Two-sided binomial p is about 0.11 for 8/10, so a bot seat bias is possible but not shown; it needs 20+ off-Ironhold mirror matches. The bots are still weak (trickle 21-23%, 7-8% blind picks) but this does not make them uneven by seat.
- **Convoy: still blocked on timing.** Checkpoint 1 needs about 349 s of pushing (262 blocks at 0.75 blocks/s) in 420 s, so the wagon can be contested at most about 17% of the time. The lowest contested share seen is 18% and that match peaked at 240 of 262 blocks, 22 short. Bots improved (distance up 25%, contested down 15 points) but the mean contested share of 31% is almost double the ceiling. Lever for the balance owner: a first checkpoint near 200 blocks, push speed near 1.0 blocks/s, or about 500 s of clock.

## Numbers to act on

Move Ironhold Dusk spawn about 150 blocks back toward the points (or Dawn forward). Do not change House stats on this evidence: Gilded 55% (34-74%). Convoy needs a timing lever from Kort, not bot work first.
