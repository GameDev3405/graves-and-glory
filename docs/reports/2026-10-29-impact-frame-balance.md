# Impact-frame melee: balance re-baseline (Task 5)

2026-10-30. Animation rework Task 5 (branch work/anim-rework): a melee troop's blow now lands on its swing's impact frame (attackVariants.impactTicks, 7 to 12 ticks after the swing), and only if the attacker is alive and not stunned since the swing, and the target is alive and within reach plus combatDiscipline.meleeReachGrace (0.5). Ranged and caster attacks are unchanged. This report compares DuelSimulation before and after. **Nothing was retuned.**

## Verdict for Kort

Melee got weaker and ranged/caster troops got stronger, well past the 5-point flag. 13 of 18 troops moved more than 5 win-rate points. 31 of 153 pairings moved more than 5 points, and 9 of those were melee against melee. The rating changes are significant: 15 of 18 types sit outside +/-1.96 SE, with chi-square p = 0.000.

- **Melee down.** The biggest drops are tc_raider (-16.2), tc_tideguard (-14.7), tc_brute (-13.2), reaper (-11.8), revenant (-7.4) and ic_hearthguard (-5.9).
- **Ranged and caster up.** The biggest gains are ic_arbalist and hexer (+14.7 each), longbow (+10.3) and tc_harpooner (+8.8). archer, ic_ballista and bonebow each gained +5.9.
- **Stalker is the exception.** It is the one melee troop whose rating rose (+0.105 rating, +2.9 win points).
- **Fights got longer.** The mean fight went from 34.6 s to 38.4 s of game time. The mean wipe went from 27.7 s to 31.3 s.

There are two likely causes, but neither was measured per fight; the duel CSV does not record misses:

1. A target that moves more than 0.5 blocks out of reach during the 7 to 12 tick wind-up is now missed. Ranged troops back off (rangedRetreatDistance), and knockback from other blows pushes targets.
2. A melee attacker killed during its wind-up now deals nothing.

Both are the intended behaviour of option A. Whether to retune is Kort's call.

## Method

- **Simulation.** DuelSimulation with the standard unit plan work/method/plans/units-r4.txt: 18 types and 189 pairings at equal gold. Each pairing is fought 4 times, twice from each side, for 720 fights.
- **Settings.** Map 0, -PsimFast=true, -PduelSeed=7, world seed SIM_LEVEL_SEED=4242. The command was `tools/balance_sim.sh <name> 25711 -PduelSim=.../units-r4.txt -PduelMap=0 -PsimFast=true -PduelSeed=7`.
- **Before.** BASE f10d4bf, the Task 1 to 4 code with no Task 5 change. Its classes were compiled before the first Task 5 source edit: the run's Gradle `compileJava` was UP-TO-DATE when it launched at 13:51, and the first Task 5 edit came at 13:52. Nothing was recompiled while it ran. Its results are in work/method/t5-before-135102/duel/duels.csv.
- **After.** The Task 5 code as committed. Its results are in work/method/t5-after-141732/duel/duels.csv.
- **Win rate.** A troop's share of its non-mirror fights, with a draw counted as half. n = 68 fights per troop, 4 per pairing.
- **Time to kill.** The mean game seconds of the fights a troop won by wipe.
- **Ratings.** From `tools/balance_method.py compare`.
- **Seeded randomness.** The swing variant pick draws from the world RNG, so the random stream differs after the change. Single 4-fight pairings carry that noise (12.5-point steps). The per-troop and rating shifts are well beyond it.

## Per troop (all non-mirror fights)
| troop | melee | n | win% before | win% after | shift (pts) | mean wipe s before | after | shift s |
|---|---|---|---|---|---|---|---|---|
| ic_arbalist |  | 68 | 54.4 | 69.1 | +14.7 **>5** | 40.3 | 41.0 | +0.6 |
| hexer |  | 68 | 29.4 | 44.1 | +14.7 **>5** | 40.0 | 55.9 | +16.0 |
| longbow |  | 68 | 25.0 | 35.3 | +10.3 **>5** | 30.4 | 33.7 | +3.3 |
| tc_harpooner |  | 68 | 80.9 | 89.7 | +8.8 **>5** | 35.1 | 36.1 | +1.0 |
| archer |  | 68 | 52.9 | 58.8 | +5.9 **>5** | 30.1 | 31.5 | +1.4 |
| ic_ballista |  | 68 | 44.1 | 50.0 | +5.9 **>5** | 20.9 | 21.5 | +0.6 |
| bonebow |  | 68 | 94.1 | 100.0 | +5.9 **>5** | 37.0 | 35.4 | -1.6 |
| stalker | yes | 68 | 52.9 | 55.9 | +2.9 | 24.9 | 29.3 | +4.4 |
| ic_breaker | yes | 68 | 23.5 | 26.5 | +2.9 | 22.0 | 24.4 | +2.4 |
| ic_mortar |  | 68 | 25.0 | 26.5 | +1.5 | 19.1 | 20.6 | +1.5 |
| knight | yes | 68 | 27.9 | 26.5 | -1.5 | 28.0 | 30.3 | +2.4 |
| sentinel | yes | 68 | 10.3 | 7.4 | -2.9 | 41.7 | 40.0 | -1.7 |
| ic_hearthguard | yes | 68 | 32.4 | 26.5 | -5.9 **>5** | 25.6 | 29.9 | +4.3 |
| revenant | yes | 68 | 91.2 | 83.8 | -7.4 **>5** | 26.0 | 31.5 | +5.5 |
| reaper | yes | 68 | 97.1 | 85.3 | -11.8 **>5** | 17.8 | 26.8 | +9.0 |
| tc_brute | yes | 68 | 55.9 | 42.6 | -13.2 **>5** | 28.3 | 26.6 | -1.7 |
| tc_tideguard | yes | 68 | 39.7 | 25.0 | -14.7 **>5** | 22.2 | 24.7 | +2.5 |
| tc_raider | yes | 68 | 63.2 | 47.1 | -16.2 **>5** | 23.5 | 27.3 | +3.8 |

## Per pairing: every pairing whose A-side win% moved (4 fights each, so 12.5-point steps)
| pairing (A~B) | win% A before | after | shift | mean s before | after | mean margin before | after |
|---|---|---|---|---|---|---|---|
| archer~tc_raider | 0.0 | 100.0 | +100.0 | 33.0 | 49.0 | -0.394 | +0.239 |
| longbow~tc_tideguard | 0.0 | 100.0 | +100.0 | 24.9 | 34.7 | -0.348 | +0.331 |
| ic_arbalist~tc_raider | 0.0 | 100.0 | +100.0 | 26.1 | 55.0 | -0.440 | +0.140 |
| longbow~tc_brute | 0.0 | 75.0 | +75.0 | 29.0 | 42.5 | -0.389 | +0.305 |
| revenant~tc_harpooner | 100.0 | 25.0 | -75.0 | 40.9 | 61.0 | +0.396 | -0.137 |
| bonebow~reaper | 25.0 | 100.0 | +75.0 | 29.2 | 28.1 | -0.048 | +0.612 |
| reaper~tc_harpooner | 100.0 | 25.0 | -75.0 | 26.8 | 39.3 | +0.333 | -0.131 |
| hexer~tc_tideguard | 0.0 | 75.0 | +75.0 | 29.5 | 76.5 | -0.513 | +0.272 |
| hexer~tc_brute | 0.0 | 75.0 | +75.0 | 69.2 | 94.8 | -0.434 | +0.250 |
| ic_arbalist~tc_brute | 25.0 | 100.0 | +75.0 | 29.5 | 35.6 | -0.212 | +0.317 |
| sentinel~hexer | 75.0 | 25.0 | -50.0 | 112.6 | 107.1 | +0.191 | -0.174 |
| revenant~hexer | 100.0 | 50.0 | -50.0 | 34.2 | 90.3 | +0.344 | -0.038 |
| stalker~ic_ballista | 50.0 | 0.0 | -50.0 | 22.6 | 22.7 | -0.098 | -0.131 |
| stalker~tc_raider | 0.0 | 50.0 | +50.0 | 28.6 | 51.7 | -0.477 | -0.050 |
| stalker~tc_tideguard | 50.0 | 100.0 | +50.0 | 29.1 | 40.0 | +0.058 | +0.288 |
| ic_hearthguard~ic_breaker | 75.0 | 25.0 | -50.0 | 25.1 | 29.5 | +0.128 | -0.023 |
| ic_ballista~tc_brute | 0.0 | 50.0 | +50.0 | 25.0 | 26.7 | -0.238 | +0.056 |
| knight~hexer | 100.0 | 75.0 | -25.0 | 50.7 | 115.8 | +0.545 | -0.002 |
| knight~ic_hearthguard | 50.0 | 25.0 | -25.0 | 32.5 | 34.0 | -0.005 | -0.104 |
| knight~tc_raider | 25.0 | 50.0 | +25.0 | 21.9 | 26.3 | -0.062 | -0.038 |
| revenant~bonebow | 25.0 | 0.0 | -25.0 | 45.7 | 41.9 | -0.050 | -0.386 |
| revenant~reaper | 25.0 | 50.0 | +25.0 | 27.2 | 31.2 | -0.046 | -0.041 |
| reaper~ic_arbalist | 100.0 | 75.0 | -25.0 | 16.9 | 40.5 | +0.647 | +0.251 |
| stalker~hexer | 75.0 | 100.0 | +25.0 | 17.9 | 22.3 | +0.102 | +0.212 |
| stalker~ic_arbalist | 25.0 | 0.0 | -25.0 | 22.2 | 24.3 | -0.239 | -0.201 |
| hexer~ic_arbalist | 75.0 | 50.0 | -25.0 | 25.2 | 27.2 | +0.007 | +0.013 |
| hexer~tc_raider | 0.0 | 25.0 | +25.0 | 27.8 | 48.9 | -0.450 | -0.165 |
| ic_hearthguard~ic_mortar | 100.0 | 75.0 | -25.0 | 29.3 | 34.4 | +0.288 | +0.115 |
| ic_hearthguard~tc_raider | 100.0 | 75.0 | -25.0 | 24.8 | 30.6 | +0.186 | +0.085 |
| ic_hearthguard~tc_brute | 25.0 | 0.0 | -25.0 | 26.5 | 26.8 | -0.078 | -0.268 |
| tc_tideguard~tc_brute | 100.0 | 75.0 | -25.0 | 18.3 | 20.6 | +0.115 | +0.139 |

pairings compared 153; moved more than 5 points: 31 (melee vs melee: 9)
mean wipe fight before 27.65 s (n 666), after 31.26 s (n 662); ends before Counter({'wipe': 666, 'cap': 54}) after Counter({'wipe': 662, 'cap': 58})

## Ratings (balance_method.py compare, A = before, B = after)

```
run A: 720 of 720 fights; run B: 720 of 720 fights; 189 shared pairings; 18 types in both
type                   A       B     B-A     SE      z
knight            -0.211  -0.294  -0.083  0.024  -3.49 ***
archer            +0.065  +0.184  +0.119  0.017  +6.95 ***
sentinel          -0.503  -0.502  +0.001  0.017  +0.06
longbow           -0.194  -0.057  +0.137  0.023  +5.93 ***
revenant          +0.417  +0.289  -0.128  0.012 -10.92 ***
bonebow           +0.528  +0.616  +0.087  0.017  +5.05 ***
reaper            +0.483  +0.301  -0.182  0.020  -9.04 ***
stalker           +0.041  +0.146  +0.105  0.030  +3.46 ***
hexer             -0.229  -0.004  +0.225  0.030  +7.48 ***
ic_hearthguard    -0.329  -0.401  -0.072  0.012  -6.07 ***
ic_arbalist       +0.104  +0.224  +0.120  0.018  +6.67 ***
ic_breaker        -0.287  -0.358  -0.071  0.017  -4.07 ***
ic_ballista       +0.051  +0.097  +0.046  0.024  +1.94
ic_mortar         -0.306  -0.289  +0.017  0.017  +0.99
tc_raider         +0.143  -0.016  -0.159  0.019  -8.56 ***
tc_harpooner      +0.284  +0.400  +0.116  0.018  +6.26 ***
tc_tideguard      -0.116  -0.239  -0.123  0.020  -6.10 ***
tc_brute          +0.057  -0.097  -0.154  0.028  -5.47 ***
types: 3 of 18 within +/-1.96; sum z^2 = 672.3 on 18 df (chi-square p = 0.000)
(the ratings sum to 0, so a change to one type also moves the others by about 1/(k-1) of it; --focus re-anchors; the cells below have no such coupling)
cell noise: per-fight SD pooled within pairing and side 0.129 (612 df)
cells: 153 cells, mean change +0.069, 41 with |z| > 1.96 (expected 7.7; binomial p of that many or more = 0.000), sum z^2 = 902.2 on 153 df (p = 0.000)
```
