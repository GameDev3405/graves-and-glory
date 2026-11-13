# Engine-map pathing: troops stuck on walls (plan, no gameplay code changed)

2026-11-13, branch ai/pathing-plan (base main 77e910f9). Kort after the New Kingsfield playtest: "bot units easily get stuck running into the walls over and over just sitting there useless the entire game". Screenshots 12 (gatehouse arch) and 13 (wall) are in status/playtest-2026-11-13.

## Summary

The lead is confirmed but is the smaller part. Engine maps have no route data, yet New Kingsfield has no river, so missing bridges are not why troops jam there. Three causes stack: (A) troops are placed on roofs and wall tops, (B) the pathfinder cannot see past a wall, and each grange's inner gate is sealed, (C) the stuck recovery mostly fails. Recommendation: standing-surface fix, a route field baked from the map's own floors, and a stall ladder inside `Tactics.walk`.

## 1. Root causes and baseline

Baseline: `checkq sim -PbotSim=2 -PsimMaps=5 -PsimMode=domination`: two 300 s matches, three bot armies a side, run three times (work/method/kf-stuck-011213, kf-probe-012736, kf-trace-014029). "No progress" comes from the 10 s unit samples (`domination-units.csv`): no target, over 5 blocks from its slot, moved 2.5 blocks or less. The sim has bot squads only; player squads share the same `Tactics.walk` (screenshots 12 and 13 are players).

| Measure (two matches) | New Kingsfield | New Rivergate (maps lead's run) | Old Kingsfield (slot 4) |
|---|---|---|---|
| Troop-time with no progress 10 s / 30 s / 60 s / 120 s | 44% / 39-40% / 32-34% / 21-24% | not sampled | 16% / 13% / 9% / 3% |
| Distinct troops stuck 30 s or more | 177-180 of about 228 | | 107 of about 210 |
| Longest stretch; median of 30 s+ stretches | 280 s; 60-70 s | | 140 s; 50 s |
| `stuck30` (AiMetrics) | 168, 176, 171 | 65 | 65 |
| Idle share not on Hold (target under 5%) | 29-33% | 18.8% | 13.2% |
| Longest idle group stretch (target 20 s) | 290 s | 383 s | 118 s |
| Objective share (target 70%) | 32% | 65.7% | 64.3% |

Where, over 30 s+ stretches (mirrored, all on the Attack order with no target): 46% just inside a grange inner wall (|x| 100-112, |z| under 30), 31% in the inner gate tunnel (|x| 100-108, z -3..+3), 15% in the base yards at (+-190, +-26), 6% in the open at the far grange wall (|x| 78-98). Orders: the Hand (0,0), Mill Rise and Tithe Barn (0,+-114).

**Cause A, standing surface (new).** Deployment and refill place troops at `Arena.ground(x,z)+1` (`GravesAndGlory.java:1143,1163,1220`). On an engine map that is `EngineMap.solid` (`EngineMap.java:81`), the top solid block of the column, which under a roof or on a wall is the roof. At second 10, 29 of 114 troops in the base yards (25%) stand at y 67-72 on roofs and wall tops against ground 64-65; 7 a match never come down. `Tactics.walk` has the same flaw: its waypoint and keep-out-of-walls search (`Tactics.java:615,646,652`) also use the top block, so a waypoint can land on a gatehouse roof or wall top.

**Cause B, no route knowledge.** `Tactics.route` (`:482`) does nothing on engine maps: Kingsfield has no river (`riverX` is -100000) and Rivergate's bridge list is empty (rivergate-wire.md, not re-run). Troops walk to a point 16 blocks along the straight line (`:620`) with the vanilla pathfinder at follow range 40, about 640 nodes, a disc of roughly 14 blocks. A wall longer than that yields a partial path ending at the wall face. Worse, both granges' inner (pit-side) walls have no opening: `tools/pathing_scan.py ... line -102 -34 -102 34` finds ground-open cells only at |z| 28-34, outside the wall. A 3-thick plug of stone brick at x -103..-101, y 65-69 seals the arch in screenshot 12, though the plan says gates are open. The only way on is a 3-wide side door at (-127, +-27) and then 274 steps to the Hand (straight line 215); a troop at the dead end is over 25 blocks from it.

**Cause C, recovery fails.** Route trace (`GRAVES_ROUTE_TRACE`): 3,890 sidesteps and 1,278 wide detour searches in two matches. 1,014 searches (79%) found no path, mean 6.7 ms, worst 40 ms (machine shared with other jobs). 2,165 sidesteps ran in the 15 s cooldown after a miss (`Wedge.java:26`), 1,014 had no path to look at, and one wedge repeated 63 times. The 2,560-cell search (`Wedge.FOLLOW_RANGE` 160) reaches about 28 blocks, less than the way out. Also `walk` zeroes `stuckTicks` on any move over 0.17 blocks (`Tactics.java:621`), so a jittering troop never counts as stuck; the `pinned` test (`:593`) only enables `stepHop` (`:625`), never the sidestep or detour (`:626`).

Formation slots have no reachability check (`GravesAndGlory.java:1038`), which feeds A and B.

## 2. Route waypoints from map data

The packer already holds the data: `tools/sectionmap.py floors_of` finds every floor of every column (ground under a roof, wall walk, deck) and its BFS made the `routes` in map.json. The mod's `EngineMap` keeps only the top block, which is why an arch reads as a wall.

| Option | For | Against |
|---|---|---|
| 1. Hand-listed gates and bridges | trivial | goes stale; one list per map |
| 2. Bigger vanilla search | no new data | cost grows with area; 2,560 cells reach 28 blocks |
| 3. Pack-time portal list (narrow passages on the floors graph) in map.json, read by `route` like bridges | no run-time search | arbitrary orders need a goal-to-portal step |
| 4. Pack-time floors file (`nav.bin.gz`); at load a 4-block graph (about 14,000 cells) and cached distance fields per goal | any goal, player orders too; gates, bridges, side doors alike; rebuilt only when the map is | new pure-Java class, about 300 lines |

Data-side prototype (`tools/pathing_scan.py`, test-only): `route dawn C` gives the true route, turns (-215,0) (-127,2) (-127,26) (-70,29) (-68,7) (-3,6) (0,0), narrowest passage 3 blocks. It also runs on Rivergate (`route dawn B`: 332 steps). No hand lists.

## 3. Stuck detector with recovery (bots and players)

Every order ends in `Tactics.walk` (via `toSlot`, enemy chase, `backOff`), so one detector there covers both. Replace `:621-641` with a rolling window: walking, and over 3 s neither 1.5 blocks moved nor 1 block of route-field distance gained, means stalled (jitter no longer resets it; a troop whose neighbour ahead on the route is moving is queueing, not stalled). Ladder: 3 s drop the cached path and take the next field point; 6 s sidestep toward the open side the field gives; 10 s the existing wide detour, started from the field point; 15 s regroup (slot repair, section 4); 30 s with no player within 24 blocks, snap to the nearest standable cell on the field path. Counters per rung for the harness. Player squads get the same ladder; the snap is skipped while a player watches.

## 4. Walls and arches narrower than a formation

After army/war-table a banner is 9 columns at 2.5 blocks (about 20 wide, up to 40 troops); gates are 6 wide, side doors 3. Slot repair: a slot behind a wall or on a roof moves to the nearest standable cell on the troop's side, by field distance. File-through: within 12 blocks of a portal narrower than the formation, troops follow the field in lanes (rank mod portal width over body width), generalising the bridge lane offset (`Tactics.java:491`); past it they walk to their slots again; the front waits a bounded time instead of pushing.

## 5. Cost on the tick budget

Measured (kf-probe, shared machine, so pessimistic): server tick mean 15.7 and 12.0 ms, p99 32.9 and 29.6 ms, server average 5.5 ms; harness targets 4 and 8 ms. A BFS over the 576 x 384 columns takes 2-5 ms warm (47 ms cold) in a scratch Java benchmark; with floors perhaps 1.5 times. Plan: seven static fields (5 points, 2 spawns) built while the map loads, under 100 ms; on-demand fields on the 4-block graph, about 0.3 ms; per-troop query a table lookup; the detector a few array operations per 10-tick beat. Net change should be negative: the failing wide searches cost 6.7 ms each, about 640 a match.

## 6. Recommendation, proof, order, risks

| Step | Size |
|---|---|
| 0. Standing surface from the floors for deploy, refill, waypoints (with the maps lead's `Arena.standing`); ask maps about the sealed gates | S |
| 1. `nav.bin.gz` in `sectionmap.py`; pure-Java `NavField` with a standalone check; `route` reads it | M |
| 2. Stall detector and ladder in `walk`, counters | M |
| 3. Slot repair and file-through (after war-table lands) | M |
| 4. `AiMetrics` stuck10 and per-location rows; gates in `bot_quality.py` | S |

Proof, mean of two sims on New Kingsfield: no-progress 30 s share 39% to 2% or less; 10 s share 44% to 6% or less; no stretch over 60 s; `stuck30` 170 to 4 or less; idle share under 5%; longest idle under 20 s; objective share 70% or more; tick mean within +0.5 ms and p99 within +2 ms of baseline. Controls: Rivergate and old Kingsfield do not regress. In game: Kort's repro (squad ordered through the inner gate), a fresh troop on a roof, a squad filing through the 3-wide door, screenshots.

Risks: nav stale after a map edit (key it to `design_sha256`); Siege walls gates for the round (rebuild that field once); one shared field funnels everyone into one lane; the snap is visible; sealing may be intended.

Army/war-table: it changes the lane lines in `route` (`:491`, `:506`) and `formation`/`deploy`; land it first, then rebase and re-measure, since 40-troop banners will jam more.

Questions for Kort: (1) Are the grange inner gates meant to be sealed? (2) May a troop stuck 30 s snap out unseen, or must it only walk? (3) Do Rivergate's quay corners belong in this work?
