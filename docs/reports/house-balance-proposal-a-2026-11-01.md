# HOUSE-BALANCE: Proposal A result

2026-11-01, branch work/balance-duels. Follow-up to house-balance-vanilla-audit-2026-10-31.md (Proposal A). Measurement only: no sheet value changed on disk.

**Method.** DuelSimulation, plan units-r4 (720 fights, map 0, `-PsimFast=true`, `-PduelSeed=7`, world seed 4242), current main. Four runs, same seed. Traits toggled with `-PsimOverride` (in-memory SheetOverride): Rise Again off = `houseTraits.graveborn_3.value=0`; Bulwark neutral = `gilded_3.value=0` plus `gilded_3.radius=0` (no ranged cut, no flank cover). No harness code added. Caps ended 2 to 4 fights per run.

**House win rate** (cross-House fights; n = 224 to 260 per cell, so about +/-3 points standard error)

| House | Baseline | Rise Again off | Bulwark neutral | Both |
|---|---|---|---|---|
| Graveborn | 71.9 | 67.3 | 74.6 | 71.9 |
| Tidecallers | 58.0 | 57.1 | 56.7 | 58.0 |
| Ironclad | 33.8 | 36.9 | 35.8 | 37.3 |
| Gilded | 35.3 | 37.9 | 31.2 | 31.2 |

**Pairings** (row's win %, n = 64 to 100; baseline / Rise off / Bulwark off / both)

- Graveborn v Gilded 71.2 / 67.5 / 80.0 / 76.2
- Graveborn v Ironclad 76.0 / 69.0 / 76.0 / 72.0
- Graveborn v Tidecallers 67.5 / 65.0 / 67.5 / 67.5
- Gilded v Ironclad 46.2 / 45.0 / 40.0 / 40.0
- Gilded v Tidecallers 29.7 / 35.9 / 34.4 / 29.7
- Ironclad v Tidecallers 26.2 in all four

**Finding.** The two traits do not cause the gap. Rise Again off costs Graveborn 4.6 points, leaving it at 67.3, still 7 above the 60 band edge. Neutralising Bulwark does not lift Gilded; it falls 4.1 (sentinel 35.3 to 17.6), so Bulwark helps Gilded a little. Both off: Graveborn 71.9, Gilded 31.2, no movement beyond noise. The imbalance is in the sheets. Baseline here differs from the 10-29 report (Graveborn 81.2, Gilded 26.8) because main has changed since.

**Reaper (sheet).** 39 HP, 9 damage per 26 ticks (6.92 DPS), speed 0.33, cost 40, against knight 28 HP, 4.00 DPS, 0.28, cost 22. Reaper has 2.4 times the knight's HP x DPS for 1.8 times the cost, and wins 94.1 (98.5 with Rise off, 100 with both off). Revenant costs 14 (knight 22) for 29 HP and 3.64 DPS: 92.6 win.

**Proposals** (untested; each needs Kort approval and a rerun of this plan)

1. Reaper damage 9 to 7 (DPS 5.38; HP x DPS falls to 2.0 times the knight's).
2. Revenant cost 14 to 19.
3. Gilded longbow (7.4 win, 18 HP, cost 46): cost 46 to 34.

Raw data: work/method/{base,norise,nobulwark,both}-*/duel/duels.csv (untracked).

## Follow-up: proposals simulated (same setup, in-memory overrides only)

Plan units-r4 for (a); for cost changes the plan was regenerated with equal gold at the new cost (the unchanged plan regenerates identically). Cross-House win %, n = 224 to 260 (about +/-3):

| Run | Graveborn | Tidecallers | Ironclad | Gilded | Reaper | Revenant | Longbow |
|---|---|---|---|---|---|---|---|
| Baseline | 71.9 | 58.0 | 33.8 | 35.3 | 94.1 | 92.6 | 7.4 |
| (a) Reaper damage 7 | 72.3 | 57.6 | 33.1 | 36.2 | 88.2 | 92.6 | 11.8 |
| (b) Revenant cost 19 | 71.2 | 58.0 | 33.8 | 36.2 | 98.5 | 80.9 | 10.3 |
| (c) Longbow cost 34 | 71.7 | 55.4 | 33.1 | 39.1 | 95.6 | 94.1 | 25.0 |
| (d) a+b+c | 68.8 | 58.9 | 32.3 | 39.7 | 94.1 | 80.9 | 25.0 |
| (e) d + Reaper health 30 | 66.2 | 59.4 | 34.6 | 39.7 | 79.4 | 82.4 | 23.5 |

The combined set (d) still leaves Graveborn at 68.8 and Ironclad at 32.3 outside 40-60; Gilded (39.7) is on the edge. Reaper damage alone barely moves the Reaper (94.1 to 88.2): its HP and speed carry it.

**One more tested tweak (e): Reaper health 39 to 30** on top of (d). Reaper falls to 79.4 and Graveborn to 66.2; Ironclad (34.6) and Gilded (39.7) stay put. It helps but does not finish the job: Graveborn is still 6 above the band, and Ironclad is still 5 below it, so Ironclad needs its own pass (its ranged and breaker troops win 22 to 41). Untested next step: Bonebow (85 win) damage or cost.
