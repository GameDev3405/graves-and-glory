# HOUSE-BALANCE: simulated search

2026-11-01, branch work/balance-duels. Nothing shipped: every change is an in-memory SheetOverride on current main. Cross-House win rate per House, 720 fights per run (units-r4 layout, 4 reps; cost changes regenerate the plan at equal gold). Standard error about 3 points per House.

**Search.** 11 coarse and refining sets on seed 7. Graveborn-only nerfs left Ironclad near 36; cheaper Ironclad and Gilded units plus Sentinel cost fixed it. Reaper damage 9 to 7 is needed (without it the Reaper stays at 94).

## Candidate sets (all within +/-25%)

| Unit | Stat | Old to new | Set A | Set B |
|---|---|---|---|---|
| reaper | health | 39 to 30 | x | x |
| reaper | damage | 9 to 7 | x | x |
| revenant | health | 29 to 23 | x | x |
| revenant | cost | 14 to 17 | x | x |
| bonebow | cost | 16 to 20 | x | x |
| longbow | cost | 46 to 35 | x | x |
| sentinel | cost | 48 to 40 | x | x |
| ic_breaker | cost | 44 to 34 | x | x |
| ic_mortar | cost | 70 to 55 | x | |
| ic_ballista | cost | 48 to 38 | x | |

Set A: 10 changes, 8 units (more margin). Set B: 8 changes, 7 units (smallest in band, Ironclad on the edge).

## Win rates (Graveborn / Tidecallers / Ironclad / Gilded)

| Run | Graveborn | Tidecallers | Ironclad | Gilded |
|---|---|---|---|---|
| Baseline seed 7 | 71.9 | 58.0 | 33.8 | 35.3 |
| A, seed 7 | 58.1 | 53.1 | 45.4 | 42.9 |
| B, seed 7 | 58.1 | 55.4 | 40.4 | 46.4 |
| Baseline seed 11 | 72.7 | 55.4 | 34.6 | 36.2 |
| A, seed 11 | 59.4 | 54.5 | 46.2 | 39.1 |
| B, seed 11 | 64.2 | 57.6 | 40.0 | 37.5 |
| Baseline plan 2 (seed 7) | 71.2 | 55.8 | 35.0 | 37.1 |
| A, plan 2 | 56.9 | 49.1 | 47.7 | 45.5 |
| B, plan 2 | 58.5 | 54.9 | 40.4 | 46.4 |

Plan 2 = 120 to 240 gold per side.

**Result.** Set A is in 40-60 in 11 of 12 House cells across both seeds and plans; its one miss is Gilded 39.1 on seed 11, inside noise. Set B fails on seed 11 (Graveborn 64.2). Recommend Set A.

**Units above 85%** (baseline: reaper 94.1, revenant 92.6, bonebow 85.3): none under Set A in any run (max 83.8, bonebow). Set B touches 85 to 87 (reaper, bonebow) in one run each. Longbow, hexer and ic_mortar stay weak (20 to 30).
