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

The gate prints `gate <a> vs <b> win=<P>% pass|fail` (exit status 0 pass, 1 fail, 2 unusable input); `--detail` adds the per-run and per-map tallies. Each run's `botsim/results.csv` names the banners fielded in its `banner0` and `banner1` columns, and the `[botsim]` summary line ends with `banner0=<id> banner1=<id>`.

## Current best pure banner

`gilded_normal` (95% against `graveborn_normal` in the Task 3 baseline; `graveborn_normal` 5%). Update this line whenever a logged gate gives another pure normal banner a higher win rate.

## Results

### Phase 2 Task 3 baseline: `gilded_normal` vs `graveborn_normal` (2026-10-18, no gate)

Code: base `01f9f5c` plus the Task 3 combat engine (ArmyMobs, Combat.resolve with the troop damage type, abilities shell, Houses, state_v8). In this build the counters table, the shield, mend and blight act; every trait, aura and commander factor is 1 and every other ability is a no-op until its House batch.

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
- The two banners as the sheet has them: `gilded_normal` is 19 troops with 552 hit points and 10 shield bearers (counters C1 to C3 cut arrows and melee from the front); `graveborn_normal` is 19 troops with 466 hit points and 0 shield bearers. Graveborn's fearless, cleave, frostshot, Rise Again and Dread are not built until the Graveborn batch, so its one live mechanic is the Gravecaller's blight.

## Tuning log

None yet: Phase 2 Task 3 changes no sheet numbers.
