# Impact-frame melee: balance re-baseline (Task 5)

2026-10-30. Animation rework Task 5 (branch work/anim-rework), with its fix round 1. A melee troop's blow now lands on its swing's impact frame (attackVariants.impactTicks, 7 to 12 ticks after the swing). It lands only if the attacker is alive and has not been stunned since that swing, and the target is alive and within reach plus combatDiscipline.meleeReachGrace (0.5). Ranged and caster attacks are unchanged. Fix round 1 judges a Charge's impact on the swing tick (Ruling R29), allows one pending blow per troop, and draws the swing variant from a per-battle random. Fix round 2 judges Hold with the Charge on the swing tick as well, and reseeds that random at the start of every round. This report compares DuelSimulation before and after. **Nothing was retuned.**

This version replaces the first one. That one listed the Stalker as melee (it is ranged), miscounted the plan, and measured the code before fix round 1. The grace-0.5 results below now measure the fix round 2 code; the grace sweep still uses the fix round 1 runs (see Method).

## Verdict for Kort

Melee got weaker and ranged/caster troops got stronger, well past the 5-point flag.

- **Troops.** 11 of 18 troops moved more than 5 win-rate points.
- **Pairings.** 35 of 153 non-mirror pairings moved more than 5 points; 7 of those are melee against melee.
- **Ratings.** The rating changes are significant: 17 of 18 types sit outside +/-1.96 SE (chi-square p = 0.000).
- **Melee down.** All 9 melee troops lost rating (the Sentinel least, -0.005). The biggest drops are tc_raider (-19.1 win-rate points), tc_tideguard (-17.6), reaper (-10.3) and tc_brute (-10.3).
- **Ranged and caster up.** All 9 gained rating. The biggest gains are hexer (+17.6 win-rate points), longbow (+11.8), ic_arbalist (+10.3) and archer (+7.4). tc_harpooner and bonebow each gained +5.9 win-rate points.
- **Fights got longer.** The mean fight went from 34.6 s to 38.3 s of game time, and the mean wipe from 27.7 s to 31.8 s.

There are two likely causes. Neither was measured per fight, because the duel CSV does not record misses.

1. A target that moves more than 0.5 blocks out of reach during the 7 to 12 tick wind-up is now missed. Ranged troops back off (rangedRetreatDistance), and knockback from other blows pushes targets.
2. A melee attacker killed during its wind-up now deals nothing.

Both are the intended behaviour of option A. Whether to retune is Kort's call. The grace sweep below shows how much of the shift the reach grace alone can win back.

## meleeReachGrace sweep (data for Kort; the sheet stays at 0.5)

Each column is a full 720-fight run against the same BEFORE run, with only combatDiscipline.meleeReachGrace changed. The sweep ran on the fix round 1 code and was not redone for fix round 2 (the brief asked only for the 0.5 baseline again). Round 2's per-round reseed moves the 0.5 numbers by noise only (Method, Seeded randomness), so the round 1 rows stay comparable with each other; the round 2 row is the committed baseline. The change was made through a temporary sheet override, and the sheet was restored to 0.5 before the commit. A rating shift is B-A from `balance_method.py compare`, with its z score; win pts is the change in non-mirror win rate (n = 68 fights per troop).

| grace | troops >5 win pts | pairings >5 pts (of 153; melee vs melee) | mean melee rating shift | mean ranged/caster rating shift | types within +/-1.96 | mean fight s (BASE 34.58) | mean wipe s (BASE 27.65) |
|---|---|---|---|---|---|---|---|
| 0.5 (committed, fix round 2 code) | 11 | 35 (7) | -0.111 | +0.111 | 1 of 18 | 38.33 | 31.84 |
| 0.5 (fix round 1 code) | 10 | 30 (8) | -0.108 | +0.108 | 2 of 18 | 38.19 | 31.42 |
| 1.0 | 6 | 17 (2) | -0.060 | +0.060 | 2 of 18 | 36.56 | 30.20 |
| 1.5 | 3 | 21 (5) | -0.041 | +0.041 | 9 of 18 | 36.16 | 29.63 |
| 2.0 | 2 | 22 (5) | -0.028 | +0.028 | 10 of 18 | 36.05 | 29.65 |

The ratings sum to zero, and there are 9 melee and 9 ranged/caster types, so the two mean shifts are always equal and opposite.

Per troop (all four columns fix round 1 code):

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
- **After.** The fix round 2 code as committed (grace 0.5): work/method/t5r2-after-170256/duel/duels.csv.
- **Fix round 1 run.** The fix round 1 code at grace 0.5, the sweep's 0.5 column: work/method/t5r1-after-155143/duel/duels.csv.
- **Grace runs.** The fix round 1 code with grace 1.0, 1.5 and 2.0: work/method/t5r1-grace10-155929, t5r1-grace15-160703 and t5r1-grace20-161424.
- **Superseded run.** The first AFTER run, before fix round 1, is work/method/t5-after-141732. Its overall picture was the same: 31 pairings moved, a mean melee rating shift of -0.108, and a mean fight of 38.4 s.
- **Win rate.** A troop's share of its non-mirror fights, with a draw counted as half: n = 68 fights per troop (17 opponents x 4).
- **Pairing shift.** The change in the A side's win rate over a pairing's 4 non-mirror fights, so each step is 25 points. A pairing counts as moved when that change is above 5 points.
- **Time to kill.** The mean game seconds of the fights a troop won by wipe.
- **Ratings.** From `tools/balance_method.py compare --a BEFORE --b AFTER`. These ratings use every fight, including the mirror and calibration fights.
- **Seeded randomness.** Both runs use the same seeds. The swing variant comes from the battle's own random (Battle.rolls), not from the world's random. Since fix round 2 it is reseeded at the start of every round from the world seed and the round's number (BattleMath.roundSeed), so a fight's swing variants no longer depend on how many draws the fights before it took. Any change in a fight's timing still changes which later random draws each event gets, so a single 4-fight pairing carries real noise (25-point steps). The reseed alone, fix round 1 against fix round 2 at grace 0.5, is noise: 17 of 18 types within +/-1.96 (chi-square p = 0.459), and 9 of 153 cells with |z| > 1.96 (7.7 expected, p = 0.357). The per-troop and rating shifts against BEFORE are well beyond it.

## Hurt cooldown (vanilla i-frames) and delayed blows

A blow that lands on its impact frame meets the target's vanilla hurt cooldown as it stands on that tick, exactly as an instant blow on that tick did at BASE.

- **Inside the cooldown's first half** (timeUntilRegen above 10), a blow no larger than the hit that started the cooldown does nothing.
- **A larger blow** deals only its excess over that hit, through armour, and does not restart the cooldown.

hl-impact checks both cases live: a 20-point hit 3 ticks before a 5.00 blow leaves the dummy untouched, and a 1-point hit then lets 3.52 through (want 3.52). Nothing in Task 5 zeroes the cooldown for a delayed blow. The wind-up only shifts the tick on which vanilla's rule is applied.

## Per troop, grace 0.5 (all non-mirror fights)
| troop | kind | n | win% before | win% after | shift (pts) | mean wipe s before | after | shift s |
|---|---|---|---|---|---|---|---|---|
| hexer | ranged/caster | 68 | 29.4 | 47.1 | +17.6 **>5** | 40.0 | 52.1 | +12.2 |
| longbow | ranged/caster | 68 | 25.0 | 36.8 | +11.8 **>5** | 30.4 | 29.9 | -0.5 |
| ic_arbalist | ranged/caster | 68 | 54.4 | 64.7 | +10.3 **>5** | 40.3 | 39.4 | -0.9 |
| archer | ranged/caster | 68 | 52.9 | 60.3 | +7.4 **>5** | 30.1 | 35.6 | +5.5 |
| tc_harpooner | ranged/caster | 68 | 80.9 | 86.8 | +5.9 **>5** | 35.1 | 36.3 | +1.1 |
| bonebow | ranged/caster | 68 | 94.1 | 100.0 | +5.9 **>5** | 37.0 | 37.6 | +0.6 |
| ic_ballista | ranged/caster | 68 | 44.1 | 48.5 | +4.4 | 20.9 | 22.7 | +1.8 |
| stalker | ranged/caster | 68 | 52.9 | 55.9 | +2.9 | 24.9 | 27.7 | +2.8 |
| ic_breaker | melee | 68 | 23.5 | 26.5 | +2.9 | 22.0 | 24.6 | +2.6 |
| ic_mortar | ranged/caster | 68 | 25.0 | 26.5 | +1.5 | 19.1 | 18.1 | -1.0 |
| ic_hearthguard | melee | 68 | 32.4 | 32.4 | +0.0 | 25.6 | 31.6 | +6.0 |
| knight | melee | 68 | 27.9 | 26.5 | -1.5 | 28.0 | 35.3 | +7.4 |
| sentinel | melee | 68 | 10.3 | 7.4 | -2.9 | 41.7 | 28.4 | -13.4 |
| revenant | melee | 68 | 91.2 | 82.4 | -8.8 **>5** | 26.0 | 33.0 | +7.1 |
| reaper | melee | 68 | 97.1 | 86.8 | -10.3 **>5** | 17.8 | 27.2 | +9.4 |
| tc_brute | melee | 68 | 55.9 | 45.6 | -10.3 **>5** | 28.3 | 25.8 | -2.6 |
| tc_tideguard | melee | 68 | 39.7 | 22.1 | -17.6 **>5** | 22.2 | 26.9 | +4.7 |
| tc_raider | melee | 68 | 63.2 | 44.1 | -19.1 **>5** | 23.5 | 30.2 | +6.7 |

## Per pairing, grace 0.5: every pairing whose A-side win% moved (4 fights each, so 25-point steps)
| pairing (A~B) | win% A before | after | shift | mean s before | after | mean margin before | after |
|---|---|---|---|---|---|---|---|
| archer~tc_raider | 0.0 | 100.0 | +100.0 | 33.0 | 61.8 | -0.394 | +0.084 |
| longbow~tc_tideguard | 0.0 | 100.0 | +100.0 | 24.9 | 32.2 | -0.348 | +0.430 |
| longbow~tc_brute | 0.0 | 75.0 | +75.0 | 29.0 | 33.0 | -0.389 | +0.186 |
| bonebow~reaper | 25.0 | 100.0 | +75.0 | 29.2 | 30.2 | -0.048 | +0.588 |
| reaper~tc_harpooner | 100.0 | 25.0 | -75.0 | 26.8 | 42.0 | +0.333 | -0.151 |
| hexer~tc_raider | 0.0 | 75.0 | +75.0 | 27.8 | 54.4 | -0.450 | +0.189 |
| hexer~tc_tideguard | 0.0 | 75.0 | +75.0 | 29.5 | 61.1 | -0.513 | +0.182 |
| hexer~tc_brute | 0.0 | 75.0 | +75.0 | 69.2 | 80.1 | -0.434 | +0.090 |
| ic_arbalist~tc_brute | 25.0 | 100.0 | +75.0 | 29.5 | 31.1 | -0.212 | +0.364 |
| knight~ic_hearthguard (melee vs melee) | 50.0 | 0.0 | -50.0 | 32.5 | 34.6 | -0.005 | -0.174 |
| knight~tc_raider (melee vs melee) | 25.0 | 75.0 | +50.0 | 21.9 | 29.9 | -0.062 | +0.047 |
| archer~ic_arbalist | 0.0 | 50.0 | +50.0 | 30.1 | 33.0 | -0.154 | +0.053 |
| sentinel~hexer | 75.0 | 25.0 | -50.0 | 112.6 | 120.0 | +0.191 | -0.261 |
| revenant~hexer | 100.0 | 50.0 | -50.0 | 34.2 | 86.4 | +0.344 | +0.040 |
| revenant~tc_harpooner | 100.0 | 50.0 | -50.0 | 40.9 | 65.1 | +0.396 | -0.051 |
| stalker~tc_raider | 0.0 | 50.0 | +50.0 | 28.6 | 48.1 | -0.477 | +0.035 |
| stalker~tc_tideguard | 50.0 | 100.0 | +50.0 | 29.1 | 32.5 | +0.058 | +0.476 |
| hexer~ic_arbalist | 75.0 | 25.0 | -50.0 | 25.2 | 24.1 | +0.007 | -0.133 |
| ic_arbalist~tc_raider | 0.0 | 50.0 | +50.0 | 26.1 | 57.4 | -0.440 | -0.013 |
| tc_tideguard~tc_brute (melee vs melee) | 100.0 | 50.0 | -50.0 | 18.3 | 20.9 | +0.115 | +0.060 |
| knight~hexer | 100.0 | 75.0 | -25.0 | 50.7 | 96.0 | +0.545 | +0.202 |
| archer~reaper | 0.0 | 25.0 | +25.0 | 19.2 | 57.5 | -0.613 | -0.058 |
| archer~hexer | 25.0 | 0.0 | -25.0 | 20.9 | 17.4 | -0.203 | -0.236 |
| archer~ic_ballista | 100.0 | 75.0 | -25.0 | 27.1 | 30.7 | +0.146 | +0.106 |
| longbow~hexer | 0.0 | 25.0 | +25.0 | 22.9 | 21.1 | -0.177 | -0.075 |
| revenant~bonebow | 25.0 | 0.0 | -25.0 | 45.7 | 38.1 | -0.050 | -0.413 |
| revenant~reaper (melee vs melee) | 25.0 | 0.0 | -25.0 | 27.2 | 33.3 | -0.046 | -0.071 |
| reaper~ic_arbalist | 100.0 | 75.0 | -25.0 | 16.9 | 37.4 | +0.647 | +0.333 |
| stalker~ic_arbalist | 25.0 | 0.0 | -25.0 | 22.2 | 21.2 | -0.239 | -0.280 |
| stalker~ic_ballista | 50.0 | 25.0 | -25.0 | 22.6 | 23.4 | -0.098 | -0.072 |
| ic_hearthguard~ic_breaker (melee vs melee) | 75.0 | 50.0 | -25.0 | 25.1 | 30.3 | +0.128 | -0.052 |
| ic_hearthguard~tc_brute (melee vs melee) | 25.0 | 0.0 | -25.0 | 26.5 | 29.4 | -0.078 | -0.201 |
| ic_breaker~tc_tideguard (melee vs melee) | 75.0 | 100.0 | +25.0 | 20.6 | 23.0 | +0.157 | +0.168 |
| ic_ballista~tc_brute | 0.0 | 25.0 | +25.0 | 25.0 | 30.5 | -0.238 | -0.031 |
| ic_mortar~tc_harpooner | 25.0 | 50.0 | +25.0 | 16.6 | 16.3 | +0.016 | +0.062 |

pairings compared 153; moved more than 5 points: 35 (melee vs melee: 7)
mean fight before 34.58 s, after 38.33 s; mean wipe fight before 27.65 s (n 666), after 31.84 s (n 667); ends before wipe 666, cap 54; after wipe 667, cap 53

## Ratings, grace 0.5 (balance_method.py compare, A = before, B = after)

```
run A: 720 of 720 fights; run B: 720 of 720 fights; 189 shared pairings; 18 types in both
type                   A       B     B-A     SE      z
knight            -0.211  -0.291  -0.080  0.019  -4.25 ***
archer            +0.065  +0.188  +0.123  0.016  +7.91 ***
sentinel          -0.503  -0.508  -0.005  0.014  -0.36
longbow           -0.194  -0.044  +0.150  0.020  +7.40 ***
revenant          +0.417  +0.297  -0.120  0.013  -9.04 ***
bonebow           +0.528  +0.623  +0.094  0.017  +5.49 ***
reaper            +0.483  +0.305  -0.178  0.018  -9.62 ***
stalker           +0.041  +0.138  +0.097  0.030  +3.20 ***
hexer             -0.229  -0.010  +0.219  0.030  +7.37 ***
ic_hearthguard    -0.329  -0.389  -0.061  0.010  -5.99 ***
ic_arbalist       +0.104  +0.215  +0.111  0.017  +6.49 ***
ic_breaker        -0.287  -0.370  -0.083  0.014  -5.95 ***
ic_ballista       +0.051  +0.109  +0.059  0.023  +2.60 *
ic_mortar         -0.306  -0.260  +0.045  0.016  +2.78 *
tc_raider         +0.143  -0.028  -0.171  0.021  -8.03 ***
tc_harpooner      +0.284  +0.381  +0.097  0.019  +5.06 ***
tc_tideguard      -0.116  -0.282  -0.166  0.021  -7.92 ***
tc_brute          +0.057  -0.074  -0.131  0.028  -4.68 ***
types: 1 of 18 within +/-1.96; sum z^2 = 707.1 on 18 df (chi-square p = 0.000)
(the ratings sum to 0, so a change to one type also moves the others by about 1/(k-1) of it; --focus re-anchors; the cells below have no such coupling)
cell noise: per-fight SD pooled within pairing and side 0.123 (612 df)
cells: 153 cells, mean change +0.075, 44 with |z| > 1.96 (expected 7.7; binomial p of that many or more = 0.000), sum z^2 = 1006.6 on 153 df (p = 0.000)
```
