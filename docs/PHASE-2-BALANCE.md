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

`gilded_normal` (82.5% against `graveborn_normal` in the Task 8b gate on the no-routing game, the baseline from here on; 52.5% in the balance fix of 2026-10-22 with routing; 85% in the Task 7 gate at the spec numbers; 100% in the Task 6 gate, report-only, before the Graveborn mechanics; 92.5% in the Task 3 baseline after fix round 1). The first Task 3 baseline (`gilded_normal` 95%) was measured with the husk's +2 armour on the Gilded side and is superseded by it. Update this line whenever a logged gate gives another pure normal banner a higher win rate. (`ironclad_normal` read 60% against `gilded_normal` in the Task 8b gate, 75% in the balance fix with routing and 45% in the Task 8 gate, so Gilded stays the bar.)

## Results

### Phase 2 Task 8b gates: the no-routing game (2026-10-22; `graveborn_normal` vs `gilded_normal` 17.5%, fail, one tuning round below; `ironclad_normal` vs `gilded_normal` 60%, report-only, inside the band)

Code: the Task 8b tree on `cdcf095` (the commits of this task), at the design's own numbers (`docs/superpowers/specs/2026-10-22-no-routing-design.md`, section 8). The controller's ruling for 8b: the `graveborn_normal` pairing must pass (40 <= P <= 60 over 20 decided matches; if not, one tuning round on the design's own numbers; if that still fails, stop and report), and the `ironclad_normal` pairing is report-only. (The design's section 10 names the gates the other way round; the ruling was followed.) Both reruns are the new baselines; the routing-game results stay below as the routing reference: `graveborn_normal` vs `gilded_normal` 47.5% (the balance fix of 2026-10-22, at the same troop rows as here) and `ironclad_normal` vs `gilded_normal` 75% (the same entry; 45% in the Task 8 gate before the Squire and Templar cuts).

What reaches these fights that the routing game did not have: no group ever breaks (a beaten group fights on; a bot army still leaves a fight only by its own Retreat order at `retreatShare`, and now arrives at its base and holds there until `rallyShare`); flank blows x1.30 (C16, was 1.15) and flank shots x1.15 (C16s, new); Bulwark's rout guard became covered flanks (no flank row against a troop within 5, 8 or 12 of a Gilded troop of another group); `fearless` is +20% damage at or below 30% health (was a 12% rout share); `dread` keeps its damage cut only. No horn sounds in the field and nothing a bot does makes a sound.

Both gates use the recipe unchanged (two processes of 10 matches, `-PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300`, a fresh server root each, nothing else running). Ports 25651 and 25652 for gate 1 (roots `work/p2/t8b-sim1-143548` and `work/p2/t8b-sim2-143608`, servers up 14:36 to 15:31), 25653 and 25654 for gate 2 (`work/p2/t8b-sim3-153227` and `work/p2/t8b-sim4-153247`, 15:32 to 16:28); every round ran its full 300 s and none aborted. The `[botsim] health` lines (90 a process) show no burning troop in any of the 360 samples and at most 2 troops stuck for 30 s in 355 of them (gate 1: two samples with 4, both match 1 at 270 s; gate 2: three samples above 2).

`BotSimulation` now writes the design's fight stats to `results.csv` (columns `dawnKills, duskKills, dawnWiped, duskWiped, dawnRetreats, duskRetreats, fights, meanFightSeconds`; `work/p2/t8b/fightstats.py` sums them): kills per side are the other side's `stats.lost` (a death by any cause); a group is wiped out when a bot army's group goes from living regular troops to none (temporaries excluded; a group refilled from the barracks can be wiped again); a retreat is the bot's own Retreat order (`Campaign.botRetreating`); a fight opens at the first blow logged while none is open and closes at the next wipe or retreat (or the round's end), so the mean fight length runs from first contact to one side's group wiped or retreating.

#### Gate 1: `graveborn_normal` vs `gilded_normal` (must pass, 40 <= P <= 60)

```
work/p2/t8b-sim1-143548/botsim/results.csv: graveborn_normal on dawn: won=1 lost=9 drawn=0 aborted=0
work/p2/t8b-sim2-143608/botsim/results.csv: graveborn_normal on dusk: won=2 lost=7 drawn=1 aborted=0
  map 0 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=1 drawn=1 aborted=0
  map 2 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=1 lost=1 drawn=0 aborted=0
rounds=20 decided=20 won=3 lost=16 drawn=1
gate graveborn_normal vs gilded_normal win=17.5% fail
```

- `graveborn_normal` won 3 of 20 (1 of 10 from Dawn's seat, 2 of 10 from Dusk's), lost 16 and drew 1: P = 17.5%, fail. Against the routing baseline (47.5%) it moved 30 points down, back to where the Task 7 gate stood with routing at the spec numbers (15%) before the balance fix.
- Mean final score 69.8 to 90.8 for Graveborn (a gap of -21.0; the balance fix read +7.0).
- Fight stats (means over the 20 matches, seat-pooled): Graveborn killed 61.2 troops a match and lost 69.5; 3.65 of its groups were wiped out a match and it wiped 3.40 of Gilded's; the bots ordered 0.05 (Graveborn) and 0.50 (Gilded) retreats a match; 8.35 fights a match of 26.5 s mean length (the per-match means run 17.8 to 45.2 s). Kingsfield (map 4) fights are half the size (about 30 kills a side).
- Why (a diagnosis from the mechanics, not an ablation): in a bot melee most blows land on someone's flank or rear. `gilded_normal` is tier III, so with its archers and longbows within 12 of the line nearly every Gilded troop is covered and takes no flank row, while every Graveborn troop takes x1.30 from a blow and x1.15 from a shot behind it. In the routing game C16 (1.15) applied to both sides alike and Bulwark's guard only stopped routs, which `gilded_normal` rarely did anyway; so the design hands Gilded a one-sided flank multiplier that the fearless damage bonus (a Risen at 30% is one or two blows from death) does not pay back.

#### Gate 2: `ironclad_normal` vs `gilded_normal` (report-only)

```
work/p2/t8b-sim3-153227/botsim/results.csv: ironclad_normal on dawn: won=6 lost=4 drawn=0 aborted=0
work/p2/t8b-sim4-153247/botsim/results.csv: ironclad_normal on dusk: won=6 lost=4 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=12 lost=8 drawn=0
gate ironclad_normal vs gilded_normal win=60% pass
```

- `ironclad_normal` won 12 of 20 (6 of 10 from each seat), lost 8, no draws: P = 60%, inside the band on its upper edge. Against the routing baseline (75%) it moved 15 points down; the Task 8 gate with routing at the spec Gilded rows read 45%.
- Mean final score 75.2 to 76.5 (a gap of -1.3).
- Fight stats: Ironclad killed 75.6 a match and lost 39.1; it wiped 4.35 Gilded groups a match and lost 0.60 of its own; retreats 0.00 (Ironclad) and 0.15 (Gilded); 6.0 fights a match of 32.2 s mean length (15.3 to 70.3). Ironclad wins the killing two to one and the points by one match in twenty: the Gilded groups die on the point and refill from the barracks while the Ironclad line is slow to move onto it. With routing they ran from the point at 25% and Ironclad took it; fighting to the last stand is what moved this number down.

#### Tuning round (ruling 1: the design's own numbers only, each within 15% of its value)

Round 1 (2026-10-22), the one round the ruling allows, on top of the design's numbers. The levers that bear on this pairing moved and no other (the bots have no commander, so Stand Fast and Muster cannot matter; `graveborn_normal` fields no Hollow King, so dread cannot): C16 1.30 to 1.105 (-15%), C16s 1.15 to 1.05 (-8.7%: a shot into a flank must still land harder for its text to hold), the Bulwark cover radii 5, 8 and 12 to 4.25, 6.8 and 10.2 (-15%; the `gilded_1..3` texts said the new numbers), `fearless` amount 0.20 to 0.23 (+15%) and amount2 0.30 to 0.345 (+15%; text "Below 34.5% health he deals 23% more damage."). The direction: the flank rows are the one-sided edge (a tier III Gilded troop is covered, a Graveborn troop never), so both come down as far as the rule allows, the cover shrinks, and the dead hit harder and sooner. Ports 25655 and 25656, roots `work/p2/t8b-sim5-163529` and `work/p2/t8b-sim6-163549`, servers up 16:35 to 17:31; all 20 rounds ran their 300 s; no burning troop and no sample with more than 2 stuck troops in the 180.

```
work/p2/t8b-sim5-163529/botsim/results.csv: graveborn_normal on dawn: won=3 lost=7 drawn=0 aborted=0
work/p2/t8b-sim6-163549/botsim/results.csv: graveborn_normal on dusk: won=3 lost=6 drawn=1 aborted=0
  map 0 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 2 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=1 drawn=1 aborted=0
  map 4 (dusk): won=1 lost=1 drawn=0 aborted=0
rounds=20 decided=20 won=6 lost=13 drawn=1
gate graveborn_normal vs gilded_normal win=32.5% fail
```

- `graveborn_normal` won 6 of 20 (3 from each seat), lost 13 and drew 1: P = 32.5%, fail. The round moved it 15 points up from 17.5% and left it 7.5 under the band. Mean final score 73.7 to 86.8 (a gap of -13.1, from -21.0).
- Fight stats: Graveborn killed 64.3 a match and lost 66.5 (from 61.2 and 69.5); 3.55 of its groups were wiped out a match and it wiped 3.35 (from 3.65 and 3.40); retreats 0.00 (Graveborn) and 0.40 (Gilded); 8.15 fights a match of 26.9 s mean length (16.7 to 35.8).
- Result: the tuning round fails. By the ruling it is not committed: the sheet stays at the design's numbers (`Spec.java` with it), and the pairing goes to Kort in `docs/QUESTIONS-FOR-KORT.md` with these numbers while Task 9 starts anyway (the design's stop rule). What the round says: the four levers together moved it 15 points (within noise of about +/-10 at n = 20), so no single lever can be credited; the covered-flank mechanism the diagnosis names is plausible but this data does not rank it. The balance pass's first step is a one-lever ablation (C16 alone at 1.105, 20 rounds, then the cover radii alone), and `results.csv` now carries per-side C16 and C16s counts (`dawnFlankHits`, `duskFlankHits`, `dawnFlankShots`, `duskFlankShots`, from `MatchStats.SquadStats`, counted in `Combat.resolve`) so the asymmetry is measured rather than inferred. With C16 at 1.105 Gilded still wins two of three, so a part of the gap is the no-routing game itself, where `fearless` was worth more as a 12% rout share against Gilded's 25% than as +20% at the edge of death, on top of the Bulwark ranged cut the balance fix already paid for. Closing it stays with Phase 2's balance pass, which may move the troop rows 8b's rule did not allow.

How much longer and bloodier fights got against the routing game: the old logs cannot say. The routing-game `results.csv` had none of the fight-stat columns and `gold.csv` samples living troops per team every 30 s only (losses are not kills: reinforcements refill between samples), so the kill, wipe, retreat and fight-length numbers above are the first baseline for the next gates; Tasks 9 and 10 compare against them.

### Phase 2 balance fix: Gilded Squire 28 and Templar 40, `graveborn_normal` vs `gilded_normal` passes (2026-10-22, 47.5%; `ironclad_normal` vs `gilded_normal` reads 75%, report-only)

Code: `phase-2-houses` at `bed3fec` with the two Gilded rows of the tuning log below changed (the commit that carries this entry). The Task 7 gate had failed at 0 of 28 with the Graveborn numbers within 15% of spec, and the options went to the controller. The controller ruled that Gilded's rows may move, on the diagnosis of 2026-10-21 (`.superpowers/sdd/2026-10-17-phase-2-houses/balance-diagnosis.md` in the main checkout). Its cause, in short: a ranged troop shoots the nearest enemy, which is the enemy front, and the Gilded front is ten shield troops under Bulwark III, so 85% of Graveborn's arrows land on a Squire or a Templar at about 28% of their base damage (counters C1 or C2 times Bulwark x0.55, clamped at 0.25), while no Gilded arrow meets a shield. Graveborn loses the first clash and never recovers the points. Cutting the two Gilded infantry rows to the lowest whole numbers within the 15% limit is the smallest change that brought a 20-match gate into the band (46% pooled over 40 matches in the diagnosis); the Graveborn rows stay at the round 2 numbers.

Both gates use the plan's recipe unchanged (two processes of 10 matches, `-PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300`, a fresh server root each, nothing else running). Ports 25641 and 25642 for gate 1, 25643 and 25644 for gate 2; every round ran its full 300 s.

#### Gate 1: `graveborn_normal` vs `gilded_normal` (blocking, 40 <= P <= 60)

Roots `work/p2/bf/g1a-004759` (`graveborn_normal` on team 0) and `work/p2/bf/g1b-004810` (`gilded_normal` on team 0).

```
[botsim] matches=10 dawn(normal)=7 dusk(normal)=3 draws=0 dawnWin=70% meanGoldEarnedAtEnd dawn=1592 dusk=1445 banner0=graveborn_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=7 dusk(normal)=2 draws=1 dawnWin=70% meanGoldEarnedAtEnd dawn=1558 dusk=1512 banner0=gilded_normal banner1=graveborn_normal
```

```
work/p2/bf/g1a-004759/botsim/results.csv: graveborn_normal on dawn: won=7 lost=3 drawn=0 aborted=0
work/p2/bf/g1b-004810/botsim/results.csv: graveborn_normal on dusk: won=2 lost=7 drawn=1 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=1 drawn=1 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=2 drawn=0 aborted=0
rounds=20 decided=20 won=9 lost=10 drawn=1
gate graveborn_normal vs gilded_normal win=47.5% pass
```

- Mean final score 84.8 to 77.8 for Graveborn (a gap of +7.0): 90.9 to 69.6 with Graveborn on Dawn, 78.7 to 86.1 on Dusk. Graveborn won no map-4 round from either seat; the Dawn seat won 14 of the 20 rounds whichever House held it (the diagnosis' own confirming gate showed the Dusk seat winning 15 of 20), so the seat edge swings from run to run and only the seat-swapped total means anything.
- The diagnosis' confirming gate of the same two rows read 47.5% as well (9 won, 10 lost, 1 drawn, gap -2); the two runs pool to 18 won, 20 lost, 2 drawn of 40.

#### Gate 2: `ironclad_normal` vs `gilded_normal` (report-only)

Roots `work/p2/bf/g2a-014428` (`ironclad_normal` on team 0) and `work/p2/bf/g2b-014429` (`gilded_normal` on team 0).

```
[botsim] matches=10 dawn(normal)=7 dusk(normal)=3 draws=0 dawnWin=70% meanGoldEarnedAtEnd dawn=1489 dusk=1431 banner0=ironclad_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=2 dusk(normal)=8 draws=0 dawnWin=20% meanGoldEarnedAtEnd dawn=1428 dusk=1553 banner0=gilded_normal banner1=ironclad_normal
```

```
work/p2/bf/g2a-014428/botsim/results.csv: ironclad_normal on dawn: won=7 lost=3 drawn=0 aborted=0
work/p2/bf/g2b-014429/botsim/results.csv: ironclad_normal on dusk: won=8 lost=2 drawn=0 aborted=0
rounds=20 decided=20 won=15 lost=5 drawn=0
gate ironclad_normal vs gilded_normal win=75% fail
```

- Mean final score 79.3 to 71.2 for Ironclad (a gap of +8.1): 74.8 to 71.4 on Dawn, 83.8 to 71.0 on Dusk. No tuning was done (report-only by ruling).
- The Task 8 gate read 45% at the spec Gilded numbers. The same pairing now reads 75%: the Squire and Templar cuts (-12.5% and -13%) soften the Gilded front, which is what the Ironclad line (sunder x1.4 and armour bolt x0.7 against a raised shield, siege) attacks; that reading was not isolated by an ablation. With 20 matches the standard error is about 10 points, so the swing is real in direction and rough in size. The Ironclad batch's own balance pass has to start from this number, not from 45%.
- Because `ironclad_normal` now beats `gilded_normal` by more than `gilded_normal` beats `graveborn_normal`, the "current best pure banner" above needs a decision from the controller; it is left as it stood.

### Phase 2 Task 8 gate: `ironclad_normal` vs `gilded_normal` (2026-10-21, report-only, 45% at the spec numbers: inside the band)

Code: the Task 8 tree (committed as `f0febad`), on `842d2c7`. The gate is report-only by ruling (the gates of Tasks 8 to 10 do not block until the balance pass), so nothing was tuned; it would have passed anyway. What reaches this fight:

- The `ironclad_normal` banner: an Ironhold Mortar (2 slots), 6 Hearthguards, 4 Arbalists, 4 Shieldbreakers and 4 Ballista Crew, 20 slots, which is tier III of Entrench (14 slots).
- Entrench: a troop that has stood still for 60 ticks takes 20% less damage (40% less on a point its team holds), and a still Mortar or Ballista Crew shoots 30% further (the Ballista Crew's 34 becomes 44.2, the Mortar's 36 becomes 46.8).
- The siege troops engage at their reach rather than at the order's radius (24 on attack, 10 on hold), and the Mortar casts nothing at an enemy within 10 blocks and walks away from one.
- The counters of the siege and the Ironclad weapons against the Gilded line: the Shieldbreaker's sunder x1.4 against a raised shield (C14), the Arbalist's armour bolt x0.7 against one (C15), siege against large x1.3 and against small x0.6 (C12, C13).
- Wide bodies in formation (Brassjaw's cell) and the siege troops standing behind the archers.

What does not reach it: Brassjaw (the hard banner only), the Quartermaster's Forward Supply (bots never use actives) and the structure counters (no structure stands in a bot match).

The gate rule is 20 matches and 40 <= P <= 60; all 20 ran. Process 1 (port 25621, `ironclad_normal` on team 0) and process 2 (port 25622, `gilded_normal` on team 0) each held its own server root; every round was a full 300 s; servers up 20:59 to 21:55. The `[botsim] health` lines (90 samples a process) show no burning troop in any of the 180, and at most 2 troops that stood still for 30 s with no target, no rout and no Hold order in 178 of them; two samples had more (process 1: match 2 at 210 s with 5 and match 7 at 210 s with 4), each followed 30 s later by one with none or fewer.

```
[botsim] matches=10 dawn(normal)=3 dusk(normal)=7 draws=0 dawnWin=30% meanGoldEarnedAtEnd dawn=1424 dusk=1512 banner0=ironclad_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=4 dusk(normal)=6 draws=0 dawnWin=40% meanGoldEarnedAtEnd dawn=1481 dusk=1500 banner0=gilded_normal banner1=ironclad_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv ironclad_normal --detail`; the roots are `work/p2/t8-sim1-205946` and `work/p2/t8-sim2-210007`):

```
work/p2/t8-sim1-205946/botsim/results.csv: ironclad_normal on dawn: won=3 lost=7 drawn=0 aborted=0
work/p2/t8-sim2-210007/botsim/results.csv: ironclad_normal on dusk: won=6 lost=4 drawn=0 aborted=0
  map 0 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 1 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 4 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 3 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=9 lost=11 drawn=0
gate ironclad_normal vs gilded_normal win=45% pass
```

What the numbers say (the last gold sample of a round is at 270 s):

- `ironclad_normal` won 9 of 20 (3 of 10 from Dawn's seat, 6 of 10 from Dusk's). Across both processes the Dusk seat won 13 of the 20 matches whichever House sat in it, so in this pairing the seat matters more than the banner; the gate's ten matches per seat cancel it.
- Mean final score 70.8 to 80.2; mean gold earned 1225 to 1366; troops alive at 270 s 55.2 to 44.5 of 57. Ironclad loses the objective race and wins the attrition: it ends each round with 10 more troops than Gilded and the lower score.
- By the second, the mean of the 20 rounds (gold earned, troops alive; Ironclad first):

```
30 gold IC/GL 72 72 living IC/GL 57.0 57.0 n 20
60 gold IC/GL 162 162 living IC/GL 57.0 57.0 n 20
90 gold IC/GL 257 273 living IC/GL 57.0 57.0 n 20
120 gold IC/GL 360 464 living IC/GL 56.8 55.6 n 20
150 gold IC/GL 475 692 living IC/GL 51.8 48.6 n 20
180 gold IC/GL 617 907 living IC/GL 50.8 42.7 n 20
210 gold IC/GL 788 1095 living IC/GL 54.2 42.1 n 20
240 gold IC/GL 994 1241 living IC/GL 56.4 44.7 n 20
270 gold IC/GL 1225 1366 living IC/GL 55.2 44.5 n 20
```

  Gold is level to 90 s (257 to 273) and Gilded leads from 120 s (360 to 464, 475 to 692 at 150 s, 617 to 907 at 180 s); Ironclad is ahead on troops from 150 s (51.8 to 48.6) and the lead widens to 50.8 against 42.7 at 180 s and 54.2 against 42.1 at 210 s. Why Ironclad earns less gold while losing fewer troops (slower troops reaching the points, or fewer kills paid) is not measured here.
- By count the banners are close (`ironclad_normal` is 20 slots with the Mortar's two; `gilded_normal` 19 troops with 552 hit points and 10 shield bearers).

Result: the gate passes at the spec numbers (P = 45%). `gilded_normal` stays the current best pure banner (85% against `graveborn_normal` in Task 7).

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

Phase 2 Tasks 3, 6 and 8 changed no sheet numbers (the Task 6 and Task 8 gates are report-only by ruling; the Task 8 gate passed at the spec numbers, 45%). Task 8b changed the rows its design names (section 8 of the no-routing design; its first commit lists them) and nothing else; its one tuning round is logged under its gate entry and was not committed (it read 32.5%, still under the band). Task 7 changed these Graveborn rows in `sheets/design.json`, each within 15% of its spec value; the percentages are against the spec value, and a whole-number column (health, damage, cooldown) moves in whole numbers.

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

Round 3 (2026-10-22), the Gilded rows, on top of round 2. Controller ruling: Gilded's rows may move. Cause: a ranged troop shoots the nearest enemy, which is the shield front, and Bulwark III (x0.55) on top of the shield counters leaves Graveborn's arrows about 28% of their base damage (diagnosis section 2, `.superpowers/sdd/2026-10-17-phase-2-houses/balance-diagnosis.md`). Each is a health cut to the lowest whole number within the 15% limit (27.2 and 39.1 are the floors):

- Order Squire (`knight`): health 32 to 28 (-12.5%).
- Templar (`sentinel`): health 46 to 40 (-13.0%).
- Result: gate 1, `graveborn_normal` vs `gilded_normal`, 9 won, 10 lost, 1 drawn of 20, P = 47.5%, pass (mean final score 84.8 to 77.8). Gate 2, `ironclad_normal` vs `gilded_normal`, report-only, 15 of 20, P = 75% (45% before the cut).

Not changed in round 3: every Graveborn row (they stay at the round 2 numbers), the other Gilded rows (the Order Archer, Order Longbow and Sunweaver health stay at spec; the diagnosis' five-row set that also cut them is the fallback if a later gate drifts under 40%), costs, counters and traits. No check hardcodes the Squire's or the Templar's health: `HousesCheck`, `BattleMathCheck` and `Phase2Checks` read the rows through `Campaign.troop(...)` and `Spec`, so none was changed.
