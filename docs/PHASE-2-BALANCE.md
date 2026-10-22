# Phase 2 balance log

Every House batch of Phase 2 ends with a bot-vs-bot balance gate. This file holds the gate rules, the run recipe, the result of every run and the tuning log. The plan is `docs/superpowers/plans/2026-10-17-phase-2-houses.md` (section "Bot balance run").

## The gate

- A House's `<house>_normal` bot banner (`botBanners` sheet) plays the current best pure normal banner (below). Each batch compares two pure banners.
- 20 matches, 10 per seat: process 1 puts the House's banner on team 0 and process 2 on team 1, so the seat (Dawn's or Dusk's base) cancels out. Normal bots on both seats (`-PsimDawn=normal -PsimDusk=normal`), maps 0 to 4 (`-PsimMaps=0,1,2,3,4`), 300 s rounds.
- P is the House banner's share of the decided rounds it won over the 20 matches; a draw counts half and an aborted round is left out (`tools/balance_gate.py` names how many). Pass: 40 <= P <= 60.
- On a fail, tune only that House's rows in `sheets/design.json`, keeping each number within 15% of its spec value, run `python tools/preflight.py`, rerun both processes and log every change in the tuning log below.
- The current best pure banner is the pure normal banner with the highest gate win rate logged so far.
- Phase 2 Task 3 has no gate: it logs the baseline only.

## Run recipe

Two background processes at once, on ports 25601 and 25602, each with its own fresh server root (a copy of `eula.txt` and `server.properties`, `level-name=Graves and Glory`, with the process's `server-port`). A run takes about an hour in real time (ten matches of 5 minutes of round time plus the arena build, per process).

```bash
G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle | head -1)
export GRAVES_ROADMAP_SERVER_ROOT=<root1>   # then <root2>, with server-port 25602
"$G" runServer --offline -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300 \
  -PsimBanner0=<a> -PsimBanner1=<b> --init-script test-tools/isolated-server.gradle   # process 2: -PsimBanner0=<b> -PsimBanner1=<a>
python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv <a> [--detail]
```

The gate prints `gate <a> vs <b> win=<P>% pass|fail` (exit status 0 pass, 1 fail, 2 unusable input), preceded by a line `aborted=<N>` of its own when any round was aborted; `--detail` adds the per-run and per-map tallies. Each run's `botsim/results.csv` names the banners fielded in its `banner0` and `banner1` columns, and the `[botsim]` summary line ends with `banner0=<id> banner1=<id>`.

## Current best pure banner

`gilded_normal` (85% against `graveborn_normal` in the Task 7 gate, with the Graveborn mechanics built and at the spec numbers; 100% in the Task 6 gate, report-only, before them; 92.5% in the Task 3 baseline after fix round 1, where `graveborn_normal` had 7.5%). The first Task 3 baseline (`gilded_normal` 95%) was measured with the husk's +2 armour on the Gilded side and is superseded by it. Update this line whenever a logged gate gives another pure normal banner a higher win rate.

## Results

### Phase 2 Task 7 gate: `graveborn_normal` vs `gilded_normal` (2026-10-21, fails; two tuning rounds, stopped)

Code: the Task 7 tree (committed as `3324fa3`), on `f52c15a`. The commit carries the round 2 numbers; round 0 ran the same mechanics at the spec numbers (the sheet rows differ only as the tuning log lists, and the soul-fire particles and a rally-horn centre that ignores temporary troops came after it; neither touches a fight). What reaches this fight that the Task 6 gate (before Task 7) did not have:

- Rise Again. The 20 slots of `graveborn_normal` reach tier III (14 slots): each Graveborn troop that is not Legendary rises once, at 30% health and stunned for 20 ticks, with a 40% chance at spec (45% from round 2).
- Fearless. The Risen and the Bonebow carry it: a group at least half fearless breaks at 12% strength instead of 25% (the infantry group is 6 Risen of 10, the ranged group 4 Bonebow of 8).
- The Grave Reaper's cleave (half the blow to the nearest other enemy within 2) and the Barrow Stalker's frostshot (Slowness I for 2 s). The Gravecaller's blight was in already.
- The Risen is a zombie and the Barrow Stalker a stray (Army mobs: no daylight burn, no water conversion).

What does not reach it: the Hollow King's dread (the hard banner only) and Grave Bloom (bots never use actives).

The gate rule is 20 matches and 40 <= P <= 60. Round 0 ran all 20 at the spec numbers. Rounds 1 and 2 were stopped after 14 decided rounds each (7 per seat, all lost): with 6 rounds left the best possible share is 6 of 20 = 30%, under the band, so the rest could not change the verdict. Each process held its own server root and port (25601, 25602); every round was a full 300 s. The `[botsim] health` lines (a sample every 30 s) show no burning troop in any of the 455 samples (180, 141 and 134 per round) and, in 454 of them, at most 2 troops that stood still for the 30 s with no target, no rout and no Hold order; one sample had 15 (round 1, match 5, 180 s, map 4) and the next one, 30 s later, had none.

#### Round 0: the spec numbers (all 20 matches)

Process 1 (port 25601, `graveborn_normal` on team 0) and process 2 (port 25602, `gilded_normal` on team 0), servers up 16:10 to 17:05:

```
[botsim] matches=10 dawn(normal)=3 dusk(normal)=7 draws=0 dawnWin=30% meanGoldEarnedAtEnd dawn=1306 dusk=1639 banner0=graveborn_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=10 dusk(normal)=0 draws=0 dawnWin=100% meanGoldEarnedAtEnd dawn=1686 dusk=1251 banner0=gilded_normal banner1=graveborn_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv graveborn_normal --detail`; the roots are `work/p2/t7-sim1-160953` and `work/p2/t7-sim2-161013`):

```
work/p2/t7-sim1-160953/botsim/results.csv: graveborn_normal on dawn: won=3 lost=7 drawn=0 aborted=0
work/p2/t7-sim2-161013/botsim/results.csv: graveborn_normal on dusk: won=0 lost=10 drawn=0 aborted=0
  map 0 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=2 drawn=0 aborted=0
rounds=20 decided=20 won=3 lost=17 drawn=0
gate graveborn_normal vs gilded_normal win=15% fail
```

What the numbers say (the last gold sample of a round is at 270 s):

- `graveborn_normal` won 3 of 20 (3 of 10 from Dawn's seat, 0 of 10 from Dusk's), so Gilded still leads from either seat; in the Task 6 gate Graveborn won none of 20, so the Graveborn mechanics moved it from 0% to 15%.
- Mean final score 47.8 to 99.8 (Task 6: 45.7 to 105.0); mean gold earned 1107 to 1466; troops alive at 270 s 48.9 to 45.6 of 57.
- By the second, the mean of the 20 rounds: gold is level to 120 s (402 to 409); at 150 s Graveborn has 46.7 troops left against Gilded's 52.5 and at 180 s 48.7 against 53.4; gold is 690 to 802 at 180 s and 957 to 1244 at 240 s. Gilded's losses pass Graveborn's only from 210 s (48.2 to 52.0 alive), when the points are already Gilded's. The gap is the first clash and the points it decides.
- By count and hit points the two banners are close (`graveborn_normal` is 466 hit points in 19 troops, `gilded_normal` 552 hit points in 19 troops with 10 shield bearers). Which part of the first clash Graveborn loses is not measured here; the Gilded shield bearers (x0.55 from arrows and blight, counters C1 to C3 on the front) face 9 of Graveborn's 19 troops that fire arrows or blight, and the Gilded longbow (range 30) outranges every Graveborn archer. That is a reading of the sheet, not a result of the run.

#### Round 1 (tuned, stopped at 14 decided rounds)

The changes are in the tuning log. Roots `work/p2/t7-sim1-171824` and `work/p2/t7-sim2-171844`.

```
work/p2/t7-sim1-171824/botsim/results.csv: graveborn_normal on dawn: won=0 lost=7 drawn=0 aborted=0
work/p2/t7-sim2-171844/botsim/results.csv: graveborn_normal on dusk: won=0 lost=7 drawn=0 aborted=0
  map 0 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=1 drawn=0 aborted=0
rounds=14 decided=14 won=0 lost=14 drawn=0
gate graveborn_normal vs gilded_normal win=0% fail
```

- Mean final score 68.4 to 90.8; mean gold earned 1234 to 1435; troops alive at 270 s 52.0 to 43.0. At 120 s Graveborn is ahead on gold (503 to 384) and at 150 s it has 45.3 troops left against 55.1: it reaches the points first and still loses the first clash.

#### Round 2 (tuned further, stopped at 14 decided rounds)

Roots `work/p2/t7-sim1-181133` and `work/p2/t7-sim2-181153`.

```
work/p2/t7-sim1-181133/botsim/results.csv: graveborn_normal on dawn: won=0 lost=7 drawn=0 aborted=0
work/p2/t7-sim2-181153/botsim/results.csv: graveborn_normal on dusk: won=0 lost=7 drawn=0 aborted=0
  map 0 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=1 drawn=0 aborted=0
rounds=14 decided=14 won=0 lost=14 drawn=0
gate graveborn_normal vs gilded_normal win=0% fail
```

- Mean final score 71.8 to 87.8; mean gold earned 1255 to 1423; troops alive at 270 s 51.6 to 39.5; at 150 s 46.5 against 54.1 alive.
- The gap in final score fell from 52 points (round 0) to 22 (round 1) and 16 (round 2), and Graveborn ends each round with more troops than Gilded, but it won no round of the 28 tuned ones. The closest rounds were on map 4: 97 to 101 with Graveborn on Dawn and 89 to 105 with it on Dusk.

Result: the gate fails. Two tuning rounds within 15% of the spec numbers did not bring `graveborn_normal` into the band, so the tuning stops here (the plan's rule), and the Graveborn numbers stay as round 2 left them (the tuning log lists every spec value, so a revert is mechanical). The Task 7 report puts the options to Kort.

### Phase 2 Task 6 gate: `gilded_normal` vs `graveborn_normal` (2026-10-19, report-only)

Code: the Task 6 tree (committed as `6264f6a`), on `786bb2b`. What reaches this fight:

- The Order Longbow's pierce: each arrow also hits the nearest enemy within 2 blocks behind its target for half the shot.
- Gilded Bulwark. `gilded_normal`'s 20 Gilded slots reach tier III:
  - its 10 shield bearers take x0.55 from arrows, bolts and blight;
  - a group within 12 blocks of a Gilded troop of another group of its army does not rout.
- The Stand Fast and Oath Banner rout guards are registered too, but they act only while their active or banner stands.

What does not reach it:

- The Oath Banner: `gilded_normal` has no Lord Marshal.
- The Champion's and the Marshal's actives: bots never use actives.

Report-only, by the controller's ruling in progress.md ("Task 6 gates after Tasks 4 and 7 land"): nothing was tuned.

Process 1 (port 25601, `gilded_normal` on team 0, `graveborn_normal` on team 1), server up 07:59:41 to 08:54:56. Every round ran its full 300 s.

```
[botsim] matches=10 dawn(normal)=10 dusk(normal)=0 draws=0 dawnWin=100% meanGoldEarnedAtEnd dawn=1755 dusk=1238 banner0=gilded_normal banner1=graveborn_normal
```

Process 2 (port 25602, `graveborn_normal` on team 0, `gilded_normal` on team 1), server up 08:00:08 to 08:56:14.

```
[botsim] matches=10 dawn(normal)=0 dusk(normal)=10 draws=0 dawnWin=0% meanGoldEarnedAtEnd dawn=1240 dusk=1763 banner0=graveborn_normal banner1=gilded_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv gilded_normal --detail`; the roots are `work/p2/task6-sim1-075914` and `work/p2/task6-sim2-075935`):

```
work/p2/task6-sim1-075914/botsim/results.csv: gilded_normal on dawn: won=10 lost=0 drawn=0 aborted=0
work/p2/task6-sim2-075935/botsim/results.csv: gilded_normal on dusk: won=10 lost=0 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 3 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=20 lost=0 drawn=0
gate gilded_normal vs graveborn_normal win=100% fail
```

The gate fails at 100%, outside the 40 to 60 band. That is 7.5 points above the Task 3 baseline after fix round 1 (92.5%): that baseline's one loss and one draw are wins here, one and a half rounds of 20. It stays report-only until the Graveborn batch (Task 7) lands; Graveborn still fights without its own mechanics.

What the numbers say (from the two runs' `results.csv` and `gold.csv`; the last gold sample of a round is at 270 s):

- `gilded_normal` won 20 of 20, 10 from each seat. Every close round was on map 4: 100 to 95 and 108 to 83 from Dawn's seat, 100 to 93 and 101 to 100 from Dusk's. On maps 0 to 3 the margin was 58 to 103 points.
- Mean final score 114.0 to 42.6 (Task 3 after fix round 1: 100.8 to 47.8). Mean gold earned 1549 to 1071 (1470 to 1118).
- Troops still alive at the 270 s sample, out of 57: Gilded 48.5 (from 47.1), Graveborn 50.0 (from 49.9). Pierce and Bulwark barely moved who survives, so the gap is still about holding points, not kills.
- Graveborn's fearless, cleave, frostshot, Rise Again and Dread arrive with Task 7. Its live damage here is the Bone Archers' and Barrow Stalkers' arrows and the Gravecaller's blight, which is exactly what Bulwark cuts on the ten Gilded shield bearers.

### Phase 2 Task 3 baseline after fix round 1: `gilded_normal` vs `graveborn_normal` (2026-10-19, no gate)

Code: `0533219` plus fix round 1 (`418797c`). The one change that reaches the fight is that a troop's armour is now its worn gear only: no Gilded troop carries the husk's built-in armour of 2 any more, and no troop of any House takes armour from its mob type. The other ten review fixes touch the sheet (one new `combatDiscipline` column, the charge-impact window, with the same value), the camp UI, the harness and the docs, not the rules. Report only: nothing was tuned.

Process 1 (port 25601, `gilded_normal` on team 0, `graveborn_normal` on team 1), server up 00:19:39 to 01:14:34. Every round ran its full 300 s.

```
[botsim] matches=10 dawn(normal)=9 dusk(normal)=0 draws=1 dawnWin=90% meanGoldEarnedAtEnd dawn=1677 dusk=1278 banner0=gilded_normal banner1=graveborn_normal
```

Process 2 (port 25602, `graveborn_normal` on team 0, `gilded_normal` on team 1), server up 00:20:00 to 01:14:56.

```
[botsim] matches=10 dawn(normal)=1 dusk(normal)=9 draws=0 dawnWin=10% meanGoldEarnedAtEnd dawn=1292 dusk=1666 banner0=graveborn_normal banner1=gilded_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv gilded_normal --detail`; the roots are `work/p2/task3-fix1-sim1-001855` and `work/p2/task3-fix1-sim2-001916`):

```
work/p2/task3-fix1-sim1-001855/botsim/results.csv: gilded_normal on dawn: won=9 lost=0 drawn=1 aborted=0
work/p2/task3-fix1-sim2-001916/botsim/results.csv: gilded_normal on dusk: won=9 lost=1 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=0 drawn=1 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=18 lost=1 drawn=1
gate gilded_normal vs graveborn_normal win=92.5% fail
```

The baseline is outside the 40 to 60 band. Task 3 has no gate, so nothing was tuned; the Gilded batch (plan Task 6, Step 8) gates `gilded_normal` against `graveborn_normal` again, and the Graveborn batch (Task 7, Step 9) gates `graveborn_normal` against the current best. It is 2.5 points below the first baseline's 95%, which was measured with the husk's +2 armour on the Gilded side. The difference is one win turned into a draw (half a round of 20), well inside the noise of 20 rounds, so removing that armour did not by itself close the gap.

What the numbers say (from the two runs' `results.csv` and `gold.csv`; the last gold sample of a round is at 270 s):

- `gilded_normal` won 18 of 20 (9 of 10 from Dawn's seat, 9 of 10 from Dusk's), 1 drawn. The rounds it lost: map 2 (graveborn_normal on Dawn, 75 to 71).
- Mean final score 100.8 to 47.8 (first baseline 105.0 to 45.7); mean gold earned 1470 to 1118 (1487 to 1106); troops still alive at the 270 s sample 47.1 to 49.9 of 57 (49.9 to 48.5 in the first baseline): Gilded had lost more troops than in the first baseline and Graveborn fewer, and the outcome did not move with it.

### Phase 2 Task 3 baseline, first run: `gilded_normal` vs `graveborn_normal` (2026-10-18, no gate; measured with the husk's +2 armour on every Gilded troop)

Code: base `01f9f5c` plus the Task 3 combat engine (ArmyMobs, Combat.resolve with the troop damage type, abilities shell, Houses, state_v8). In this build the counters table (the shield rows C1 to C3, the flank row and the stance rows), mend and blight act; every trait, aura and commander factor is 1 and every other ability is a no-op until its House batch. The Order Archer's `steady` is live through counters row C18a (x1.2 on its own hits while its group holds; `gilded_normal` fields four archers). Every Gilded troop was a husk and carried the husk's built-in armour of 2 on top of its sheet gear, which no sheet holds and no Graveborn troop (skeletons have none) carried: this baseline was measured with that +2 on the Gilded side. Fix round 1 removed it (a troop's armour is its worn gear only); the corrected baseline is the section above.

Process 1 (port 25601, `gilded_normal` on team 0, `graveborn_normal` on team 1), server up 21:57:44 to 22:52:10. Every round ran its full 300 s.

```
[botsim] matches=10 dawn(normal)=10 dusk(normal)=0 draws=0 dawnWin=100% meanGoldEarnedAtEnd dawn=1698 dusk=1262 banner0=gilded_normal banner1=graveborn_normal
```

Process 2 (port 25602, `graveborn_normal` on team 0, `gilded_normal` on team 1), server up 21:59:29 to 22:53:58.

```
[botsim] matches=10 dawn(normal)=1 dusk(normal)=9 draws=0 dawnWin=10% meanGoldEarnedAtEnd dawn=1278 dusk=1702 banner0=graveborn_normal banner1=gilded_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv gilded_normal --detail`; the roots are `work/p2/task3-sim1-215714` and `work/p2/task3-sim2-215859`):

```
work/p2/task3-sim1-215714/botsim/results.csv: gilded_normal on dawn: won=10 lost=0 drawn=0 aborted=0
work/p2/task3-sim2-215859/botsim/results.csv: gilded_normal on dusk: won=9 lost=1 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 3 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 4 (dusk): won=1 lost=1 drawn=0 aborted=0
rounds=20 decided=20 won=19 lost=1 drawn=0
gate gilded_normal vs graveborn_normal win=95% fail
```

The baseline is outside the 40 to 60 band. Task 3 has no gate, so nothing was tuned; the Gilded batch (plan Task 6, Step 8) gates `gilded_normal` against `graveborn_normal` again, and the Graveborn batch (Task 7, Step 9) gates `graveborn_normal` against the current best.

What the numbers say (from the two runs' `results.csv` and `gold.csv`; the last gold sample of a round is at 270 s):

- `gilded_normal` won 19 of 20 (10 of 10 from Dawn's seat, 9 of 10 from Dusk's), so the seat does not explain it. The rounds it lost: map 4 (graveborn_normal on Dawn, 104 to 96).
- Mean final score 105.0 to 45.7; mean gold earned 1487 to 1106; troops still alive at the 270 s sample 49.9 to 48.5 of 57 (three armies of 19 on a side). The fights barely thin either side, so the gap is objective score, not kills.
- The two banners as the sheet has them: `gilded_normal` is 19 troops with 552 hit points and 10 shield bearers (counters C1 to C3 cut arrows and melee from the front); `graveborn_normal` is 19 troops with 466 hit points and 0 shield bearers. Graveborn's fearless, cleave, frostshot, Rise Again and Dread are not built until the Graveborn batch, so its one live mechanic is the Gravecaller's blight; Gilded's live ones are the shield rows, the Order Archer's `steady` stance (C18a) and the Sunweaver's mend. The Gilded armour was also 2 higher than its gear says (above), roughly 9 to 11% less damage taken from every troop blow.

## Tuning log

Phase 2 Tasks 3 and 6 changed no sheet numbers (the Task 6 gate is report-only by ruling). Task 7 changed these Graveborn rows in `sheets/design.json`, each within 15% of its spec value; the percentages are against the spec value, and a whole-number column (health, damage, cooldown) moves in whole numbers.

Round 1 (2026-10-21), the five troops of `graveborn_normal`:

- Risen (revenant): health 26 to 29 (+11.5%), cooldown 25 to 22 ticks (-12%), speed 0.25 to 0.28 (+12%).
- Bonebow: health 18 to 20 (+11.1%), cooldown 40 to 35 (-12.5%), speed 0.27 to 0.30 (+11.1%).
- Grave Reaper: health 34 to 39 (+14.7%), damage 8 to 9 (+12.5%), cooldown 30 to 26 (-13.3%), speed 0.30 to 0.33 (+10%).
- Barrow Stalker: health 20 to 22 (+10%), cooldown 40 to 35 (-12.5%), speed 0.30 to 0.33 (+10%).
- Gravecaller (hexer): health 22 to 25 (+13.6%), damage 7 to 8 (+14.3%), cooldown 55 to 48 (-12.7%), speed 0.27 to 0.30 (+11.1%).
- Result: 0 of 14, mean final score 68.4 to 90.8 (round 0: 47.8 to 99.8).

Round 2 (2026-10-21), on top of round 1:

- Reach: Risen and Grave Reaper 2.8 to 3.0 (+7.1%; preflight keeps a melee reach at 3 or less), Bonebow 22 to 25 (+13.6%), Barrow Stalker 26 to 29 (+11.5%), Gravecaller 18 to 20 (+11.1%).
- Training time: Risen and Bonebow 3.0 to 2.6 s (-13.3%), Grave Reaper and Barrow Stalker 6.0 to 5.2 s (-13.3%), Gravecaller 8.0 to 7.0 s (-12.5%).
- Rise Again chance (`graveborn_1`, `graveborn_2`, `graveborn_3`): 10% to 11% (+10%), 25% to 28% (+12%), 40% to 45% (+12.5%); the row texts say the new numbers.
- Cleave (`cleave_reaper`): 50% to 57% of the blow (+14%); the row text says it.
- Result: 0 of 14, mean final score 71.8 to 87.8.

Not changed: the Hollow King (not in the normal banner), costs, the fearless, dread, frostshot and blight rows, and the Gilded rows. The Task 7 tests that read a Graveborn number (the Rise Again chances and the cleave share) follow the tuned sheet.
