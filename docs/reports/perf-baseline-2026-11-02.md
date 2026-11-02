# Performance baseline under bot load, 2026-11-02 (plan tasks 3.6 and 5.5)

Branch `work/perf-baseline` off main `ef1c1a6`. Not pushed. Only test tooling changed (`BotSimulation.java`, probe columns); no game code.

## Result against the 7.7 working targets

| Target (PLAN-TO-1.0 7.7) | Measured | Verdict |
|---|---|---|
| Server MSPT p95 under 50 ms | p95 4.5 to 8.3 ms per match (mean of matches 5.7 Ironhold, 5.8 Rivergate Domination, 6.2 Ironhold Siege); p99 6.5 to 13.1 ms | Passes with a wide margin, with one caveat below |
| Map build under 60 s | 25.2 to 27.3 s at real 20 TPS (546 ticks Ironhold, 539 Rivergate; n=8) | Passes (about 45% of the target) |
| 60 fps on each six-player machine | Not measurable here (no clients) | Open |

The caveat is a single tick of 480 to 750 ms in every unthrottled match that spans a 6000-tick server boundary (about one per five minutes of server time). Its position walks by about 547 ticks from match to match, which fits the vanilla autosave interval of 6000 server ticks. That is an inference; I did not read the save path. At 3 ticks over 50 ms per ~6000, it does not move p95 or p99, but players would feel a half-second stall every five minutes of a six-player match. If Kort wants it gone, that is a game-code question (autosave or save flush policy for the arena world), which I did not touch.

## Numbers

Load: 3v3 bot armies (size 3), Normal against Normal, gilded vs graveborn, 114 troops at start (peak 114). Domination 300 s (6000 round ticks), Siege to its natural end (3700 to 7400 ticks). Values are the mean over matches of each match's figure, with min to max in brackets. Ticks are server tick bodies (START to END of `MinecraftServer.tick`) over the round.

| Run | Matches | MSPT mean | p50 | p95 | p99 | max tick | Heap after GC (MB) | Process commit (MB) | Loaded chunks |
|---|---|---|---|---|---|---|---|---|---|
| Ironhold Domination, unthrottled | 10 | 2.9 (2.3-4.2) | n/a | 5.7 (4.5-8.2) | 8.8 (6.5-12.9) | 606 (autosave, see above) | 459 (445-479) | 2360 (2039-4075) | 2507 |
| Rivergate Domination, unthrottled | 10 | 3.0 (2.6-3.7) | n/a | 5.8 (5.1-7.1) | 9.2 (7.9-10.5) | 620 | 456 (447-467) | 2094 (2004-2219) | 2962 |
| Ironhold Siege, unthrottled | 8 | 3.1 (2.7-3.8) | 2.6 | 6.2 (5.0-8.2) | 9.6 (7.8-13.1) | 170 (36-738) | 435 (426-454) | 2080 (1940-2724) | 2252 |
| Ironhold Domination, real 20 TPS | 1 | 5.2 | 4.7 | 8.3 | 12.1 | 50.2 | 455 | 2356 | 2466 |
| Rivergate Domination, real 20 TPS | 1 | 5.0 | 4.6 | 7.8 | 12.8 | 50.8 | 465 | 2068 | 2938 |

The first two rows pool two 10-match runs (`perfA` and `perfB`, maps alternating 1,0). Real 20 TPS runs cost 5 minutes each, so only one match per map; they show unthrottled MSPT is optimistic by about 2 ms mean and 2.5 ms at p95 (cooler JIT and clock when the loop idles), not by an order of magnitude. Use the real-time rows as the conservative baseline.

Setup (map switch to arena ready, then round start), real 20 TPS:

| Map | Build wall (s) | Build ticks | Build tick mean / max (ms) | `startRound` | `field` |
|---|---|---|---|---|---|
| Ironhold | 25.2 to 27.3 (n=4) | 546 | 4.6 to 5.8 mean, max 36 to 46 | 5 to 56 ms | 2 to 17 ms |
| Rivergate | 26.9 to 27.0 (n=4) | 539 | 4.6 to 4.8 mean, max 37 to 48 | 4 to 7 ms | 1 to 4 ms |

The build is paced by the fixed columns-per-tick rate, not by CPU, so it is about 27 s at 20 TPS on any machine that keeps up (tick mean 5 ms). Unthrottled, the same build takes 5 to 7 s with a 500 to 800 ms autosave tick. Ironhold Siege `startRound` is 32 to 134 ms (mean 75). Round-start first ticks run 50 to 250 ms once per match (troop spawn).

Memory: live set after a forced GC is stable at 430 to 480 MB across all 28 matches with no upward drift (match 1 and match 10 are the same within 10 MB). Heap before GC reaches 0.5 to 1.9 GB because the JVM max is 7968 MB on this machine; that is allocator headroom, not need. Process commit charge is about 2.0 to 2.4 GB (first match higher, up to 4.1 GB). Threads 41, loaded classes about 20 000.

## Method

- Servers: `tools/balance_sim.sh`, headless `runServer -PbotSim`, offline, fresh scratch roots under `work/method/perf{A,B,C,D,E}-*` (each with its own `eula.txt`; `work/` is gitignored). Ports 25691 to 25695, one server at a time so runs did not compete for CPU. The machine has 24 logical processors; other Java processes (not mine) were running and were left alone.
- Probe: the existing `GRAVES_BOTSIM_PROBE=1` aid in `BotSimulation` (tick times, heap after forced GC, GC counts, entities, collections) plus new columns written to `botsim/probe.csv`: `committedMb`, `buildMs`, `buildTickCount`, `buildTickMeanMs`, `buildTickP95Ms`, `buildTickMaxMs`, `startRoundMs`, `fieldMs`, `unitsPeak`, `slowTicks` (index@ms of every tick over 50 ms, in round order). Build time is wall time from `Campaign.switchMap` to `Battle.ready`, so it needs a map change; each alternating-map match does one. A same-map repeat records no build.
- Unthrottled runs use `-PsimFast=true` (next tick starts as soon as the last ends), so MSPT is pure tick cost. Real-time runs omit it. Domination runs: `-PbotSim=10 -PsimMaps=1,0 -PsimRoundSeconds=300`. Siege: `-PbotSim=8 -PsimMode=siege -PsimMaps=1`. Build timing: `-PbotSim=6 -PsimMaps=1,0 -PsimRoundSeconds=30`, real time. Map 1 is Ironhold, map 0 Rivergate.
- Probe percentiles use the per-match sorted tick list (nearest rank).

## What six human clients would change (not measurable here)

- Client frame rate (the 60 fps target) and client memory: no client ran.
- Network: per-player chunk and entity tracking, state packets (`state_v9` per player), and join-time chunk streaming. The sim has no connections, so server networking cost is zero in these numbers.
- Real players near the front line keep different chunks loaded than bot troops alone, and players' own entities, projectiles and abilities add tick cost. Entity counts here are 20 to 65 non-troop entities after cleanup.
- Join and lobby hitches: building the arena with players waiting, and the autosave stall with players connected.
- Human burst play (mass ability use, clumped fights) differs from Normal-tier bots.
- Longer sessions: the longest run was 10 matches (about 7 minutes of wall time); a multi-hour soak is separate (the probe supports one).

## Reproduce

`GRAVES_BOTSIM_PROBE=1 tools/balance_sim.sh perf 25691 -PbotSim=10 -PsimMaps=1,0 -PsimRoundSeconds=300 -PsimFast=true`, then read `work/method/perf-*/botsim/probe.csv`.
