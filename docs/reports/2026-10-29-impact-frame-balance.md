# Impact-frame melee: balance re-baseline (Task 5)

2026-10-30. Animation rework Task 5 (branch work/anim-rework), with its fix round 1. A melee troop's blow now lands on its swing's impact frame (attackVariants.impactTicks, 7 to 12 ticks after the swing). It lands only if the attacker is alive and has not been stunned since that swing, and the target is alive and within reach plus combatDiscipline.meleeReachGrace (0.5). Ranged and caster attacks are unchanged. Fix round 1 judges a Charge's impact on the swing tick (Ruling R29), allows one pending blow per troop, and draws the swing variant from a per-battle random. This report compares DuelSimulation before and after. **Nothing was retuned.**

This version replaces the first one. That one listed the Stalker as melee (it is ranged), miscounted the plan, and measured the code before fix round 1.

## Verdict for Kort

Melee got weaker and ranged/caster troops got stronger, well past the 5-point flag.

- **Troops.** 10 of 18 troops moved more than 5 win-rate points.
- **Pairings.** 30 of 153 non-mirror pairings moved more than 5 points; 8 of those are melee against melee.
- **Ratings.** The rating changes are significant: 16 of 18 types sit outside +/-1.96 SE (chi-square p = 0.000).
- **Melee down.** All 9 melee troops lost rating except the Sentinel (+0.002). The biggest drops are tc_brute (-16.2 win points), tc_raider (-16.2), tc_tideguard (-14.7) and reaper (-11.8).
- **Ranged and caster up.** All 9 gained rating. The biggest gains are hexer (+22.1), ic_arbalist (+13.2) and longbow (+8.8). archer, ic_ballista and bonebow each gained +5.9.
- **Fights got longer.** The mean fight went from 34.6 s to 38.2 s of game time, and the mean wipe from 27.7 s to 31.4 s.

There are two likely causes. Neither was measured per fight, because the duel CSV does not record misses.

1. A target that moves more than 0.5 blocks out of reach during the 7 to 12 tick wind-up is now missed. Ranged troops back off (rangedRetreatDistance), and knockback from other blows pushes targets.
2. A melee attacker killed during its wind-up now deals nothing.

Both are the intended behaviour of option A. Whether to retune is Kort's call. The grace sweep below shows how much of the shift the reach grace alone can win back.

## meleeReachGrace sweep (data for Kort; the sheet stays at 0.5)

Each column is a full 720-fight run against the same BEFORE run, with only combatDiscipline.meleeReachGrace changed. The change was made through a temporary sheet override, and the sheet was restored to 0.5 before the commit. A rating shift is B-A from `balance_method.py compare`, with its z score; win pts is the change in non-mirror win rate (n = 68 fights per troop).

| grace | troops >5 win pts | pairings >5 pts (of 153; melee vs melee) | mean melee rating shift | mean ranged/caster rating shift | types within +/-1.96 | mean fight s (BASE 34.58) | mean wipe s (BASE 27.65) |
|---|---|---|---|---|---|---|---|
| 0.5 (committed) | 10 | 30 (8) | -0.108 | +0.108 | 2 of 18 | 38.19 | 31.42 |
| 1.0 | 6 | 17 (2) | -0.060 | +0.060 | 2 of 18 | 36.56 | 30.20 |
| 1.5 | 3 | 21 (5) | -0.041 | +0.041 | 9 of 18 | 36.16 | 29.63 |
| 2.0 | 2 | 22 (5) | -0.028 | +0.028 | 10 of 18 | 36.05 | 29.65 |

The ratings sum to zero, and there are 9 melee and 9 ranged/caster types, so the two mean shifts are always equal and opposite.

Per troop:

| troop | kind | 0.5 rating shift (z) | win pts | 1.0 rating shift (z) | win pts | 1.5 rating shift (z) | win pts | 2.0 rating shift (z) | win pts |
|---|---|---|---|---|---|---|---|---|---|
| knight | melee | -0.094 (-4.2) | -4.4 | -0.043 (-2.0) | +0.0 | -0.035 (-1.5) | -1.5 | -0.024 (-1.2) | -1.5 |
| sentinel | melee | +0.002 (+0.1) | -1.5 | +0.034 (+2.7) | +0.0 | +0.021 (+1.6) | +0.0 | +0.021 (+1.2) | +0.7 |
| revenant | melee | -0.121 (-10.1) | -2.9 | -0.076 (-7.7) | -1.5 | -0.078 (-8.3) | -2.9 | -0.062 (-7.2) | -1.5 |
| reaper | melee | -0.176 (-8.1) | -11.8 | -0.108 (-6.1) | -5.9 | -0.076 (-4.5) | -2.9 | -0.047 (-3.1) | -4.4 |
| ic_hearthguard | melee | -0.059 (-5.1) | -2.9 | -0.047 (-3.9) | -4.4 | -0.027 (-2.3) | -1.5 | -0.017 (-1.4) | -1.5 |
| ic_breaker | melee | -0.085 (-5.4) | +2.9 | -0.031 (-2.4) | +4.4 | -0.007 (-0.4) | +4.4 | +0.004 (+0.3) | +4.4 |
| tc_raider | melee | -0.141 (-10.5) | -16.2 | -0.088 (-5.9) | -5.9 | -0.078 (-5.0) | -4.4 | -0.081 (-5.8) | -1.5 |
| tc_tideguard | melee | -0.129 (-6.7) | -14.7 | -0.078 (-3.9) | -7.4 | -0.036 (-1.7) | -2.9 | -0.059 (-2.8) | -10.3 |
| tc_brute | melee | -0.166 (-7.0) | -16.2 | -0.104 (-3.7) | -4.4 | -0.049 (-2.1) | -5.9 | +0.013 (+0.5) | +1.5 |
| archer | ranged/caster | +0.125 (+7.4) | +5.9 | +0.083 (+4.8) | +0.0 | +0.056 (+3.2) | +1.5 | +0.032 (+2.0) | -2.9 |
| bonebow | ranged/caster | +0.091 (+5.4) | +5.9 | +0.076 (+4.6) | +5.9 | +0.050 (+2.8) | +5.9 | +0.039 (+2.3) | +5.9 |
| hexer | ranged/caster | +0.224 (+8.6) | +22.1 | +0.067 (+2.3) | +2.9 | +0.045 (+1.9) | -1.5 | +0.011 (+0.4) | +0.7 |
| ic_arbalist | ranged/caster | +0.130 (+7.0) | +13.2 | +0.059 (+3.5) | +5.9 | +0.060 (+3.6) | +5.9 | +0.044 (+2.5) | +4.4 |
| ic_ballista | ranged/caster | +0.074 (+3.2) | +5.9 | +0.033 (+1.4) | +0.0 | +0.026 (+1.1) | +2.9 | +0.023 (+1.1) | +2.9 |
| ic_mortar | ranged/caster | +0.015 (+0.9) | +0.0 | +0.008 (+0.5) | +0.0 | +0.008 (+0.5) | +1.5 | +0.028 (+1.6) | +1.5 |
| longbow | ranged/caster | +0.114 (+5.9) | +8.8 | +0.062 (+3.3) | +2.9 | +0.032 (+1.5) | +1.5 | +0.044 (+2.7) | +4.4 |
| stalker | ranged/caster | +0.099 (+3.3) | +1.5 | +0.096 (+3.1) | +5.9 | +0.051 (+1.6) | +0.0 | +0.011 (+0.4) | -1.5 |
| tc_harpooner | ranged/caster | +0.098 (+5.1) | +4.4 | +0.058 (+3.4) | +1.5 | +0.037 (+2.3) | +0.0 | +0.020 (+1.3) | -1.5 |

Even at 2.0, melee stays below BASE on average. The Tidecallers' melee troops respond least evenly: tc_tideguard is still -10.3 win points at 2.0.

## Method

- **Simulation.** DuelSimulation with the standard unit plan work/method/plans/units-r4.txt, at equal gold per side. It has 18 types:
  - 9 melee: knight, sentinel, revenant, reaper, ic_hearthguard, ic_breaker, tc_raider, tc_tideguard, tc_brute.
  - 9 ranged or caster: archer, longbow, bonebow, stalker, hexer, ic_arbalist, ic_ballista, ic_mortar, tc_harpooner.
- **Fights.** The plan has 189 pairing ids and 720 fights:
  - 153 non-mirror pairings, 4 fights each, twice from each side: 612 fights.
  - 18 mirror pairings at equal counts, 4 fights each: 72 fights.
  - 18 calibration pairings (a type against itself at unequal counts), 2 fights each: 36 fights.
  - That is 171 x 4 + 18 x 2 = 720.
- **Settings.** Map 0, -PsimFast=true, -PduelSeed=7, world seed SIM_LEVEL_SEED=4242. The command was `SIM_LEVEL_SEED=4242 tools/balance_sim.sh <name> 25711 -PduelSim=.../units-r4.txt -PduelMap=0 -PsimFast=true -PduelSeed=7`. The runs went one at a time, and nothing was compiled while one ran.
- **Before.** BASE f10d4bf, with no Task 5 change: work/method/t5-before-135102/duel/duels.csv.
- **After.** The fix round 1 code as committed (grace 0.5): work/method/t5r1-after-155143/duel/duels.csv.
- **Grace runs.** The same code with grace 1.0, 1.5 and 2.0: work/method/t5r1-grace10-155929, t5r1-grace15-160703 and t5r1-grace20-161424.
- **Superseded run.** The first AFTER run, before fix round 1, is work/method/t5-after-141732. Its overall picture was the same: 31 pairings moved, a mean melee rating shift of -0.108, and a mean fight of 38.4 s.
- **Win rate.** A troop's share of its non-mirror fights, with a draw counted as half: n = 68 fights per troop (17 opponents x 4).
- **Pairing shift.** The change in the A side's win rate over a pairing's 4 non-mirror fights, so each step is 25 points. A pairing counts as moved when that change is above 5 points.
- **Time to kill.** The mean game seconds of the fights a troop won by wipe.
- **Ratings.** From `tools/balance_method.py compare --a BEFORE --b AFTER`. These ratings use every fight, including the mirror and calibration fights.
- **Seeded randomness.** Both runs use the same seeds. The swing variant now comes from the battle's own random (Battle.rolls, seeded from the world seed), not from the world's random. However, any change in a fight's timing still changes which later random draws each event gets. A single 4-fight pairing therefore carries real noise (25-point steps). The per-troop and rating shifts are well beyond it.

## Hurt cooldown (vanilla i-frames) and delayed blows

A blow that lands on its impact frame meets the target's vanilla hurt cooldown as it stands on that tick, exactly as an instant blow on that tick did at BASE.

- **Inside the cooldown's first half** (timeUntilRegen above 10), a blow no larger than the hit that started the cooldown does nothing.
- **A larger blow** deals only its excess over that hit, through armour, and does not restart the cooldown.

hl-impact checks both cases live: a 20-point hit 3 ticks before a 5.00 blow leaves the dummy untouched, and a 1-point hit then lets 3.52 through (want 3.52). Nothing in Task 5 zeroes the cooldown for a delayed blow. The wind-up only shifts the tick on which vanilla's rule is applied.

## Per troop, grace 0.5 (all non-mirror fights)
| troop | kind | n | win% before | win% after | shift (pts) | mean wipe s before | after | shift s |
|---|---|---|---|---|---|---|---|---|
| hexer | ranged/caster | 68 | 29.4 | 51.5 | +22.1 **>5** | 40.0 | 55.5 | +15.5 |
| ic_arbalist | ranged/caster | 68 | 54.4 | 67.6 | +13.2 **>5** | 40.3 | 39.1 | -1.3 |
| longbow | ranged/caster | 68 | 25.0 | 33.8 | +8.8 **>5** | 30.4 | 31.6 | +1.2 |
| archer | ranged/caster | 68 | 52.9 | 58.8 | +5.9 **>5** | 30.1 | 32.4 | +2.3 |
| ic_ballista | ranged/caster | 68 | 44.1 | 50.0 | +5.9 **>5** | 20.9 | 21.9 | +1.1 |
| bonebow | ranged/caster | 68 | 94.1 | 100.0 | +5.9 **>5** | 37.0 | 36.5 | -0.5 |
| tc_harpooner | ranged/caster | 68 | 80.9 | 85.3 | +4.4 | 35.1 | 35.2 | +0.0 |
| ic_breaker | melee | 68 | 23.5 | 26.5 | +2.9 | 22.0 | 24.6 | +2.6 |
| stalker | ranged/caster | 68 | 52.9 | 54.4 | +1.5 | 24.9 | 25.8 | +1.0 |
| ic_mortar | ranged/caster | 68 | 25.0 | 25.0 | +0.0 | 19.1 | 19.3 | +0.3 |
| sentinel | melee | 68 | 10.3 | 8.8 | -1.5 | 41.7 | 37.7 | -4.0 |
| revenant | melee | 68 | 91.2 | 88.2 | -2.9 | 26.0 | 34.2 | +8.2 |
| ic_hearthguard | melee | 68 | 32.4 | 29.4 | -2.9 | 25.6 | 31.6 | +6.0 |
| knight | melee | 68 | 27.9 | 23.5 | -4.4 | 28.0 | 28.6 | +0.7 |
| reaper | melee | 68 | 97.1 | 85.3 | -11.8 **>5** | 17.8 | 26.4 | +8.6 |
| tc_tideguard | melee | 68 | 39.7 | 25.0 | -14.7 **>5** | 22.2 | 25.5 | +3.3 |
| tc_raider | melee | 68 | 63.2 | 47.1 | -16.2 **>5** | 23.5 | 27.7 | +4.2 |
| tc_brute | melee | 68 | 55.9 | 39.7 | -16.2 **>5** | 28.3 | 26.5 | -1.8 |

## Per pairing, grace 0.5: every pairing whose A-side win% moved (4 fights each, so 25-point steps)
| pairing (A~B) | win% A before | after | shift | mean s before | after | mean margin before | after |
|---|---|---|---|---|---|---|---|
| archer~tc_raider | 0.0 | 100.0 | +100.0 | 33.0 | 45.6 | -0.394 | +0.215 |
| hexer~tc_raider | 0.0 | 100.0 | +100.0 | 27.8 | 68.6 | -0.450 | +0.146 |
| ic_ballista~tc_brute | 0.0 | 100.0 | +100.0 | 25.0 | 29.4 | -0.238 | +0.190 |
| knight~hexer | 100.0 | 25.0 | -75.0 | 50.7 | 107.8 | +0.545 | -0.118 |
| longbow~tc_tideguard | 0.0 | 75.0 | +75.0 | 24.9 | 36.3 | -0.348 | +0.184 |
| longbow~tc_brute | 0.0 | 75.0 | +75.0 | 29.0 | 36.8 | -0.389 | +0.106 |
| bonebow~reaper | 25.0 | 100.0 | +75.0 | 29.2 | 28.2 | -0.048 | +0.611 |
| hexer~tc_tideguard | 0.0 | 75.0 | +75.0 | 29.5 | 77.2 | -0.513 | +0.082 |
| hexer~tc_brute | 0.0 | 75.0 | +75.0 | 69.2 | 85.9 | -0.434 | +0.292 |
| ic_arbalist~tc_raider | 0.0 | 75.0 | +75.0 | 26.1 | 51.2 | -0.440 | +0.130 |
| ic_arbalist~tc_brute | 25.0 | 100.0 | +75.0 | 29.5 | 31.7 | -0.212 | +0.381 |
| reaper~tc_harpooner | 100.0 | 50.0 | -50.0 | 26.8 | 43.4 | +0.333 | -0.100 |
| stalker~tc_tideguard | 50.0 | 100.0 | +50.0 | 29.1 | 34.1 | +0.058 | +0.390 |
| knight~ic_hearthguard (melee vs melee) | 50.0 | 25.0 | -25.0 | 32.5 | 37.6 | -0.005 | -0.048 |
| knight~tc_raider (melee vs melee) | 25.0 | 50.0 | +25.0 | 21.9 | 27.1 | -0.062 | -0.035 |
| archer~reaper | 0.0 | 25.0 | +25.0 | 19.2 | 43.7 | -0.613 | -0.175 |
| archer~hexer | 25.0 | 0.0 | -25.0 | 20.9 | 22.2 | -0.203 | -0.217 |
| sentinel~hexer | 75.0 | 50.0 | -25.0 | 112.6 | 109.4 | +0.191 | -0.185 |
| revenant~bonebow | 25.0 | 0.0 | -25.0 | 45.7 | 39.8 | -0.050 | -0.457 |
| revenant~reaper (melee vs melee) | 25.0 | 50.0 | +25.0 | 27.2 | 31.3 | -0.046 | -0.040 |
| revenant~hexer | 100.0 | 75.0 | -25.0 | 34.2 | 68.4 | +0.344 | +0.039 |
| revenant~tc_harpooner | 100.0 | 75.0 | -25.0 | 40.9 | 66.6 | +0.396 | +0.136 |
| reaper~ic_arbalist | 100.0 | 75.0 | -25.0 | 16.9 | 37.8 | +0.647 | +0.260 |
| stalker~ic_arbalist | 25.0 | 0.0 | -25.0 | 22.2 | 22.3 | -0.239 | -0.254 |
| hexer~ic_arbalist | 75.0 | 50.0 | -25.0 | 25.2 | 26.3 | +0.007 | -0.007 |
| ic_hearthguard~ic_breaker (melee vs melee) | 75.0 | 50.0 | -25.0 | 25.1 | 31.4 | +0.128 | +0.018 |
| ic_hearthguard~tc_raider (melee vs melee) | 100.0 | 75.0 | -25.0 | 24.8 | 29.1 | +0.186 | +0.027 |
| ic_hearthguard~tc_brute (melee vs melee) | 25.0 | 0.0 | -25.0 | 26.5 | 27.9 | -0.078 | -0.189 |
| ic_breaker~tc_tideguard (melee vs melee) | 75.0 | 100.0 | +25.0 | 20.6 | 22.9 | +0.157 | +0.177 |
| tc_tideguard~tc_brute (melee vs melee) | 100.0 | 75.0 | -25.0 | 18.3 | 22.1 | +0.115 | +0.043 |

pairings compared 153; moved more than 5 points: 30 (melee vs melee: 8)
mean fight before 34.58 s, after 38.19 s; mean wipe fight before 27.65 s (n 666), after 31.42 s (n 665); ends before wipe 666, cap 54; after wipe 665, cap 55

## Ratings, grace 0.5 (balance_method.py compare, A = before, B = after)

```
run A: 720 of 720 fights; run B: 720 of 720 fights; 189 shared pairings; 18 types in both
type                   A       B     B-A     SE      z
knight            -0.211  -0.305  -0.094  0.023  -4.15 ***
archer            +0.065  +0.190  +0.125  0.017  +7.37 ***
sentinel          -0.503  -0.501  +0.002  0.013  +0.15
longbow           -0.194  -0.080  +0.114  0.019  +5.91 ***
revenant          +0.417  +0.296  -0.121  0.012 -10.08 ***
bonebow           +0.528  +0.620  +0.091  0.017  +5.37 ***
reaper            +0.483  +0.307  -0.176  0.022  -8.14 ***
stalker           +0.041  +0.140  +0.099  0.030  +3.32 ***
hexer             -0.229  -0.005  +0.224  0.026  +8.62 ***
ic_hearthguard    -0.329  -0.388  -0.059  0.012  -5.08 ***
ic_arbalist       +0.104  +0.235  +0.130  0.019  +6.97 ***
ic_breaker        -0.287  -0.371  -0.085  0.016  -5.44 ***
ic_ballista       +0.051  +0.125  +0.074  0.023  +3.16 ***
ic_mortar         -0.306  -0.291  +0.015  0.016  +0.90
tc_raider         +0.143  +0.002  -0.141  0.013 -10.49 ***
tc_harpooner      +0.284  +0.382  +0.098  0.019  +5.09 ***
tc_tideguard      -0.116  -0.245  -0.129  0.019  -6.70 ***
tc_brute          +0.057  -0.109  -0.166  0.024  -7.03 ***
types: 2 of 18 within +/-1.96; sum z^2 = 733.3 on 18 df (chi-square p = 0.000)
(the ratings sum to 0, so a change to one type also moves the others by about 1/(k-1) of it; --focus re-anchors; the cells below have no such coupling)
cell noise: per-fight SD pooled within pairing and side 0.120 (612 df)
cells: 153 cells, mean change +0.071, 42 with |z| > 1.96 (expected 7.7; binomial p of that many or more = 0.000), sum z^2 = 989.6 on 153 df (p = 0.000)
```
