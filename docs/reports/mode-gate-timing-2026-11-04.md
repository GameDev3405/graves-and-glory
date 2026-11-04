# Mode gate vs timing rules, 2026-11-04 (diagnosis only)

Base: main 5344c9e5. Throwaway env-var toggles (reverted, not committed). Seed SIM_LEVEL_SEED=4242 (world only; bots and combat are not seeded). Siege 6 matches, Convoy 4.

| Toggle | Siege attacker wins | Convoy checkpoints |
|---|---|---|
| main control | 0/6 | 0 |
| (a) blows land instantly | 0/6 | 0 |
| (b) no cancellation | 0/6 | 0 |
| (b) + no range check | 0/6 | 0 |
| (c) no bow shot delay | 0/6 | 0 |
| (d) no pending gate | 0/6 | 0 |
| a+b+c+d+range together | 0/6 | not run |
| arc changes off / shield raise off / wall stretch off | 0/6 each | not run |
| parent ef8d2817 control (same seed) | 4/6 | not run |

Average living dusk defenders at 90/120/150/180 s: parent 49.0/37.5/26.7/4.0, main 52.5/44.5/29.8/18.8; every toggle run lands near main.

Finding: no single timing rule, pair, or all four together restores the gate. No sign of a bug (for example cancellation on target range, or ranged troops stuck behind the melee gate); removing the range check changed nothing. Defenders die slightly slower on main from the first minute. The yard clock has almost no slack (parent yard falls at 164-180 s of 185 s), so a small global tempo loss flips every match. Likely a diffuse effect of the rework (bow cooldown +25%, shield and crawl changes, shieldwall removal, hit weight), not one rule.

Caveats: 6-match samples; the sim is non-deterministic (parent got 4/6 here versus 5/6 reported earlier).
