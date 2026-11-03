# Performance targets and hotspots (board item 5.5)

Date: 2026-11-02. Branch: work/perf-targets. Author: Claude (worker). Scope: measurement and proposal only; no gameplay, save-format or protocol change.

## Summary

- Server tick cost in a 3v3 bot match is far under budget (mean 5-6 ms, p95 7-9 ms against 50 ms). Met with a large margin.
- Client FPS is well above 60 in the one battle that could be recorded (mean 178, p5 125), but that run was not at confirmed 1080p and covered one map only. Treated as provisionally met.
- Network, dedicated-server MSPT and 6-player load were not re-measured here (reasons below).
- Top hotspot: pathfinding requests issued from `Tactics.walk` (about 13% of busy server-thread time in battle, of which about 82% is the single `startMovingTo` call). Vanilla entity ticking and chunk ticking dominate the rest.

## Method

- Integrated server via `runClient -PlocalIntegration -PautoHost`, with the bot simulation (`BotSimulation`, `-Dgravesandglory.botSim=3 simMaps simRoundSeconds=240 simSize=3 simProbe=true`): 3 consecutive 240 s matches, 3v3, 114 troops, real 20 TPS, Ironhold then Rivergate maps.
- Server numbers come from `botsim/probe.csv` (tick start to end, heap after GC, loaded chunks, map build ticks).
- Client numbers come from a new unshipped probe, `test-tools/java/gg/gravesandglory/PerfIntegrated.java` (enabled by `-Dgravesandglory.perfInt=<dir>`, excluded from release by `tools/release_config.py`). It samples frames per second, frame-time p95/p99/max, heap and entity count, and spectates the battle from (0, 90, 0).
- Hotspots come from a JFR recording (`settings=profile`, `jdk.ExecutionSample`), aggregated per thread over the three battle windows (server thread) and over the whole run (render thread).
- `CampaignChecks` was disabled (the `localChecks` property removed) so it did not take over the world. Heap capped at 3 GB. options.txt: vsync off, max FPS 260, render and simulation distance 12.

Caveats that apply to everything below: another worktree's client was running on the same machine; JFR was active (small overhead); the client window was most likely the build default 1280x720, not 1920x1080 (my `--width/--height` arguments were lost when I avoided overriding `jvmArgs`). Treat client FPS as an indicative upper bound for 1080p, not a certified figure.

## Targets, measured values, status

| Area | Proposed target | Measured | Status |
|---|---|---|---|
| Server MSPT, 3v3 full troops and bots (integrated) | mean <= 20 ms, p95 <= 50 ms (plan 7.7), p99 <= 50 ms | mean 4.86 / 6.36 / 6.33 ms; p95 7.18 / 8.90 / 9.40; p99 14.16 / 10.45 / 12.45 (matches 1/2/3) | Met |
| Server worst tick | no more than 3 ticks over 50 ms per match, none during battle | max 118 / 59 / 71 ms; 5 / 2 / 1 ticks over 50 ms, mostly the round-tick-0 troop spawn (51-118 ms) | Mostly met; spawn spike is the miss |
| Server MSPT, dedicated server, 6 players | p95 <= 50 ms | not measured: a dedicated server needs the EULA, which is Kort's call; 6 real clients exceed the process limit | Not measured |
| Client FPS at 1080p, battle | >= 60 mean, p5 >= 45 (plan 7.7: 60 fps) | battle (Ironhold, spectator, 58 entities): mean 178.3, p5 125.1, min 53.2, frame p95 10.3 ms, worst frame 365 ms; camp view mean 107, p5 89 | Met in that run; not confirmed at 1080p; Rivergate battle not captured (client phase stayed "camp") |
| Client frame hitches | no frame over 250 ms in battle | worst 365 ms in battle; load phase worst 601 ms | Missed (rare, single hitches; load hitch expected) |
| Server heap | <= 1 GB after GC | 487-505 MB after GC; 3156-4038 loaded chunks; process committed about 3.4 GB with a 3 GB heap cap | Met |
| Client heap | <= 2 GB | max about 1373 MB (3 GB cap) | Met |
| Memory, whole integrated client plus server process | <= 4 GB committed | about 3.4 GB committed | Met (the 3 GB heap cap was my setting, so this is an upper estimate of need) |
| Network per player, battle (downstream) | <= 20 KB/s | not measurable on the integrated channel (no Netty buffers, counters read 0). Prior dedicated-server baseline (perf-baseline-2026-11-02): 6.7-9.9 KB/s battle, 0.03 KB/s upstream | Met by prior data; not re-measured |
| Map build (new match, cold) | <= 60 s (plan 7.7) | 24.7-27.2 s (539-546 ticks) per match; first cold build 83-87 s from the host call to arena ready including world creation | Build met; cold first-launch path is the miss |
| New world to in-world | <= 30 s | 17-19 s | Met |
| Save on shutdown | <= 3 s for a 22 MB world | about 1 s | Met |
| Lobby-return save (blocks the server thread) | <= 500 ms | 765-908 ms (from the prior baseline, not re-measured) | Missed (single hitch at the end of a match) |
| Existing-save load time | <= 15 s | not measured (extra run dropped on coordinator instruction); a 22 MB saved world exists in work/perf-int/r1/run/saves for a later check | Not measured |

## Hotspots (JFR, server thread, battle windows, percent of busy server-thread samples)

1. Vanilla entity ticking, 39.7%. Pathfinding about 10.9% and entity collisions about 15%. Scales with troop count; no mod-side bug, but pathfinding request volume is controlled by the mod (hotspot 3).
2. `ServerChunkManager.tick`, 33.4%. Random block ticks about 9-10% and `ChunkTicketManager.purge` about 6%, across 3000-4000 loaded chunks for what is a small arena. This is the cheapest large win: the loaded chunk count is far larger than the battlefield needs (simulation distance 12 is my test setting; the shipped value should be checked).
3. Mod `Battle.tick`, 20.6%. Of that, `Tactics.walk` 13.3% (686 samples in the sampled windows). Within `walk`: line 342 `startMovingTo` = 562 of 686 samples (82%); `Arena.ground` calls from lines 299 and 330 = 95 samples (14%); `getBlockState` on the waypoint-clearing check (line 333) = 11. Also `sendState` about 0.9% and `Arena.height/ground` about 2% overall.
4. Map build: `Arena.buildColumn` about 12% of whole-run server samples; it produces the 25-27 s build (reasonable, off the battle path).
5. Client render thread: `TroopRenderer.render` about 5% of samples (`TroopModel.prepare/render`, `TroopDepth.beginFrame`, `TroopModels.resolve`), then vanilla particles and chunk building. No client hotspot threatens 60 fps in this run.

## Fixes, ranked by gain over effort

Gameplay-visible or tuning changes are only listed; none were implemented.

1. Lower loaded-chunk count and random tick work for the arena (gain: up to about a third of server tick time; effort: low). Candidates: set the battle world's simulation distance and random-tick speed to the minimum the arena needs, or stop chunk tickets outside the arena. Needs a check that no crops, fire or leaf decay depend on random ticks (gameplay-adjacent, so it needs Kort's approval).
2. Raise `repathTicks` or add a destination-change threshold before `startMovingTo` in `Tactics.walk` (gain: most of the 13% walk cost, since 82% of it is that one call; effort: low). The call is already gated by `u.nextPath` and a 3-block goal-change check, so the cost is pathfinding itself. Spreading repaths across ticks (stagger `nextPath` by slot) would flatten the round-tick-0 spike and the periodic load without changing the average behavior. Troops steer slightly differently, so this is gameplay-visible and needs approval; not done.
3. Cache `Arena.ground(x, z)` in a primitive int array per arena (gain: about 2%; effort: low, invisible). Not implemented because the height map is also rewritten during map build and gate/wall changes, and I could not run a full verification matrix in the time allowed.
4. Spread the troop spawn over several ticks at round start (gain: removes the 51-118 ms spike; effort: low-medium; touches spawn timing so needs approval).
5. Move the lobby-return save off the server thread or make it incremental (gain: about 0.8 s hitch; effort: medium; save behavior, so needs approval).
6. Client: nothing urgent. If a low-end target is added, batch `TroopModel.prepare` and look at particle limits first.

## Not measured, with reasons

- Dedicated-server MSPT and real per-player network: the EULA is Kort's decision, and the integrated channel has no byte counters. The prior baseline figures are quoted above and marked as such.
- Rivergate battle FPS: the spectator camera only engaged for match 1.
- Confirmed 1080p: window size was 1280x720 by accident.
- Existing-save load time: no extra run, per coordinator instruction to finish with what was measured.
- Six-player (5 client) load: process limit of one Minecraft process.

## Files changed by this item

- `test-tools/java/gg/gravesandglory/PerfIntegrated.java` (new, test tool only, not shipped).
- `test-tools/resources/fabric.mod.json` (entrypoint registered for the test tool).
- `tools/release_config.py` (class added to the forbidden-in-release list).
- Raw data stays in the gitignored `work/perf-int/` folder of this worktree (probe.csv, client.csv, run.jfr).

## Spawn spike follow-up (branch perf/spawn-spike, 2026-11-02)

Before (main `bd05fa2b`, integrated client, bot-sim 2 x 60 s 3v3, 114 troops, probe + PerfIntegrated, raw data in gitignored `work/perf-spawn/before/`):

| Match | Map | Max tick | Ticks over 50 ms | startRound | field (bot-sim respawn) | Slow ticks (tick@ms) |
|---|---|---|---|---|---|---|
| 1 (cold JVM) | Ironhold | 128.4 ms | 10 | 105.3 ms | 16.3 ms | 0@128, 4@72, 5@89, 6@58, 7@58, then 50-55 ms at 63-70 and 159 |
| 2 (warm) | Rivergate | 33.9 ms | 0 | 14.6 ms | 10.2 ms | none |

Finding: the 51-118 ms spike is a cold-start cost, not steady per-round work. A warm spawn of 114 troops costs about 15 ms (startRound). The first round pays class loading and interpreted code for the troop classes, their goals and their kit. Path requests are already spread over 10 ticks by the slot%10 unit beat, so throttling repaths would not move the spike. Bot-sim's `field()` respawns the army in the same tick, which adds 10-16 ms that real rounds do not pay.

Change (gameplay-invisible): `Battle.prewarmSpawn` runs once per server start from `mapReady` (in the lobby, after the map build). It builds one unspawned troop of each sheet row with its kit and discards it. `reinforce` now recomputes House traits once per line instead of once per troop; the resulting traits are the same.

Lobby-return save (765-908 ms): not changed. `saveAll(true,false,false)` already skips clean chunks and does no flush. The remaining cost is main-thread NBT serialization of the chunks the round dirtied. Moving it off-thread is unsafe (chunk data is not thread-safe), and spreading it over lobby ticks needs chunk-storage mixins and changes save behaviour that Kort approved on 2026-11-02 (ArenaAutosave). Kort accepted the 0.8 s lobby-return save as is on 2026-11-03.

After (same run settings, `work/perf-spawn/after/`; prewarm logged 30 troops in 40 ms in the lobby):

| Match | Map | Max tick | Ticks over 50 ms | startRound | field | Slow ticks (tick@ms) |
|---|---|---|---|---|---|---|
| 1 (cold JVM) | Ironhold | 185.7 ms (tick 20) | 6 | 29.0 ms | 13.3 ms | 0@95, 7@57, 9@51, 20@186, 120@52, 130@57 |
| 2 (warm) | Rivergate | 86.1 ms (tick 0) | 1 | 12.9 ms | 13.0 ms | 0@86 |

Read: the prewarm cuts the cold startRound from 105 to 29 ms, and warm is unchanged (15 to 13 ms). Tick 0 is still over 50 ms. The after run shared the machine with two to three other worktrees' 2-3 GB clients (the baseline shared it with none), so tick times are inflated and the under-50 ms tick target is not shown. Tick 0 also holds the first unit ticks, entity tracking and bot-sim's second spawn, beyond startRound. Next step: re-measure on an idle machine, then attach JFR to tick 0 if it is still over 50 ms. Repath throttling was not done: walk already runs on a slot%10 beat, so path requests do not stack on tick 0.

## Tick 0 re-measure attempt (branch perf/tick0, 2026-11-03)

Machine state: no other Minecraft java.exe was running before launch (checked twice), so this was the idle-machine slot the earlier run lacked.

Probe change (test tool only, not shipped): `PerfProbe` now also runs on an integrated server with `-Dgravesandglory.perfProbeIntegrated=true` and writes `server-firstticks.csv` (map, battle tick 0-19, ms) for every battle. Battle tick 0 is the tick in which `startRound` ran (the probe's phase flips at that tick's end), so it includes startRound, bot-sim's `field()` respawn and the first unit ticks.

Run: `runClient -PlocalIntegration -PautoHost` with a gradle init script (own root `work/perf-tick0/before`, seeded options.txt, botSim 2 x 60 s 3v3 on maps 0,1, JFR `settings=profile`, 3 GB heap).

Result: not measured. The client crashed in mod initialization (`work/perf-tick0/before/run/crash-reports/crash-2026-11-03_01.29.33-client.txt`): the main mod was not among the loaded mods, so every main-mod class the test-tools entrypoints touched was missing (`NoClassDefFoundError` for `Veterans$Tally` from BotSimulation and `BuildMode` from MapForge), although `build/classes/java/main` (396 classes) and `build/resources/main/fabric.mod.json` were present. Cause not found; first suspect is the init script overriding the loom client `runDir`/`jvmArgs` in `afterEvaluate` (compare with a known-good `test-tools/isolated-checks.gradle` launch).

Before / after: before = 95 ms (cold) and 86 ms (warm) at tick 0, from the loaded-machine run above; after = none, no fix made. Tick 0 under 50 ms is still not shown. The CampNetworkChecks fresh-root check was gated on the tick-0 fix and was not run. Next: get one integrated bot-sim launch working, read `server-firstticks.csv`, then JFR tick 0 if it is over 50 ms.

### Idle re-measure (second launch, same day)

The first launch's crash came from a stale main-mod build on the classpath. Relaunched exactly as `verify.py game` does (`gradlew --no-daemon build -x test`, then `gradlew --no-daemon runClient -PlocalIntegration -I test-tools/isolated-checks.gradle`), plus a second init script that only drops the `localChecks` property and adds botSim 2 x 60 s 3v3 (maps 0,1), autoHost, probe, PerfIntegrated, 3 GB heap and JFR `settings=profile`. options.txt pre-seeded as in the 2026-11-02 runs (render and simulation distance 12, vsync off). Zero Minecraft processes were running at launch. Raw data: `work/perf-tick0/before2/` (gitignored). Prewarm: 30 troops in 25 ms.

| Match | Map | Battle max tick | Ticks over 50 ms | Tick 0 | Ticks 1-9 (ms) |
|---|---|---|---|---|---|
| 1 (cold JVM) | Ironhold | 218.1 ms (tick 0) | 11 | 218.1 ms | 49, 25, 47, 39, 76, 87, 97, 51, 19 |
| 2 (warm) | Rivergate | 66.1 ms (tick 0) | 1 | 66.1 ms | 5-10 |

Battle means 7.85 / 6.97 ms, p95 17.2 / 9.8 ms. So on an idle machine tick 0 is still over 50 ms: 218 ms cold, 66 ms warm (before = 95 / 86 ms in the loaded run; the loaded run's cold match had a different first-tick mix, and the two runs differ in launch path, so treat the pairs as indicative).

JFR, server thread, ticks around each round start: only 1 execution sample lies under `startRound`/`reinforce`/`spawn`, so the spawn itself is cheap (startRound 13-29 ms by the earlier timing). The samples in the second around tick 0 are vanilla world, chunk and entity ticking plus chunk serialization; `Structures.place` samples belong to the map build's landmark queue just before the round, not tick 0. There was one 12.9 ms young-GC pause inside match 1's tick 0 (the 259 ms `G1Old` event is a concurrent cycle, not a pause). Dozens of JIT compilations over 100 ms ran in the same second (light propagation, chunk ticking, chunk serialization, advancement criteria), and JFR does not sample the time a thread spends in interpreted or just-compiled code paths under a safepoint. Reading: the remaining cold tick 0 is JVM warm-up of vanilla code paths that first run at battle scale on that tick, not a mod-side hotspot that sampling can name. The warm 66 ms tick 0 could not be attributed in the time available.

No code fix was made (none that players could not see could be identified and verified in budget), so there is no "after" row. CampNetworkChecks fresh-root check: not run (budget). Candidates for the next pass: per-tick JFR with `jdk.ExecutionSample#period=1ms` around tick 0 of the warm match, and a check whether the round's first chunk save or entity tracking of 114 new troops falls on tick 0.
