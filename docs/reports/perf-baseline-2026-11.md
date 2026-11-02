# Performance baseline with real clients, 2026-11 (plan task 3.6)

Branch `work/perf-baseline-clients` off main `3fa9dd1`. The name `work/perf-baseline` was already taken by the bot-only baseline (checked out in `bannerbound-perf`, in main as `docs/reports/perf-baseline-2026-11-02.md`). Not pushed.

Single run, 2026-11-02: one dedicated dev server, two real harness clients (host and guest, in one War Table room) and four bot seats, on two maps one after the other. Each battle had 2 humans plus bot seats and was measured for 3000 server ticks (150 s). Map index 1 is Ironhold, map index 0 is Rivergate.

## Results

| Measure | Ironhold | Rivergate |
|---|---|---|
| Map build (wall s / ticks) | 27.3 s / 546 | 27.0 s / 539 (first build after boot: 51.0 s / 538) |
| Server tick, battle: mean | 7.58 ms | 8.27 ms |
| Server tick, battle: p50 / p95 / p99 | 6.82 / 10.65 / 24.68 ms | 7.87 / 11.51 / 15.78 ms |
| Server tick, battle: max | 75.2 ms | 99.6 ms |
| Ticks over 50 ms (of 3000) | 9 | 2 |
| Server tick, camp (pre-battle): mean / p95 / max | 4.51 / 8.65 / 34.4 ms (243 ticks) | 7.20 / 17.69 / 55.7 ms (841 ticks, includes the first build window; max 460 ms) |
| Server heap used, battle max | 1784 MB (3 GB max) | 1815 MB (3 GB max) |
| Loaded chunks / entities (server) | 3794 / up to 127 | 4116 / up to 125 |
| Host client fps, battle: mean / p5 / min | 173 / 106 / 95 | 174 / 117 / 98 |
| Host client frame p95 / worst | 14.0 ms / 30 ms | 13.5 ms / 427 ms |
| Guest client fps, battle: mean / p5 / min | 238 / 131 / 103 | 354 / 170 / 98 |
| Guest client frame p95 / worst | 11.2 ms / 259 ms | 9.0 ms / 182 ms |
| Client heap max (of 2 GB) | host 633 MB, guest 594 MB | host 633 MB, guest 590 MB |
| Bandwidth, battle, server to client | host 6.7 KB/s, guest 8.4 KB/s | host 6.7 KB/s, guest 9.9 KB/s |
| Bandwidth, client to server | 0.03 KB/s | 0.03 KB/s |
| Camp-phase bandwidth, server to client | 4.6 to 19.8 KB/s | 10.8 to 10.9 KB/s |
| Entities in view on client (battle) | 59 | 59 |

Process memory (Windows working set, whole run, max): server 2486 MB (private 3640 MB), host client 1432 MB (private 2400 MB), guest client 1423 MB (private 2409 MB). CPU seconds at the end: server 460, host 505, guest 582.

Reading the numbers:

- Server ticks sit at 7.6 to 8.3 ms mean against a 50 ms budget, p95 about 11 ms. This is above the bot-only baseline (p95 4.5 to 8.3 ms) because two real clients in a running match add chunk tracking and entity packets, and the server shares a machine with the clients. The 50 ms line was crossed 9 times on Ironhold and twice on Rivergate in 3000 ticks, worst 100 ms; no multi-second stalls during battles.
- Map build is paced by columns per tick, so it takes about 540 ticks (27 s at 20 TPS) on both maps, matching the earlier baseline. The 51 s first build is cold JVM and cold chunk generation right after boot; the camp-phase max of 460 ms on Rivergate and the 668 ms first battle window on Ironhold are the build-to-battle transition, not steady-state battle.
- Client frame rate is comfortable (uncapped, p5 above 100 fps on this machine). The worst-frame spikes (427 ms host, 1.4 s guest during the Rivergate camp) are world load and map-switch moments, not battle frames. Frame spikes while a map loads into a running client are the one visible roughness.
- Bandwidth is tiny, about 7 to 10 KB/s in battle, so it is not a constraint at this player count.

## Method

- Server: Loom `runServer` in the worktree with `-Dfabric.addMods` pointing to the local-integration test jar and `-Dgravesandglory.localIntegration=true`, 3 GB heap with the kit's Aikar flags. `PerfProbe` (test-tools, never shipped; added to the forbidden list in `tools/release_config.py`) records server tick START to END with `System.nanoTime`, build duration from map change to lobby ready, and every 100 ticks heap, loaded chunks and entities.
- Clients: two Loom `runClient` instances driven by `PerfClient` through the harness (no desktop computer use). The host creates a War Table room, the guest joins by code, they set the map and size, ready up (10 s countdown) and wait out the battle. Per second: frame count and frame times from `WorldRenderEvents.START`, JVM heap, Netty byte counters (inbound and outbound, added at the head of the pipeline), entity count, ping.
- Process memory: `work/sample.ps1` sampled working set, private bytes and CPU every 5 s for the server and both clients.
- Summary: `tools/perf_summary.py <run dir>` turns the CSVs (server-windows, server-battles, server-builds, client-*, procs) into the numbers above. Percentiles are over tick or per-second window samples as noted in the script.
- Kit check: `gate.ps1 -Pin` and `start.ps1 -DryRun` from `tools/server-kit` were run on the built jar and passed (version gate OK, Aikar command printed, EULA refused until set).

## Deviations from the plan

- The plan says six clients. The cap is 5 Minecraft processes in total and Kort's review client (PID 29360) is one of them. This run used 3 of mine (1 server + 2 harness clients) and 4 bot seats. Real-client results therefore cover 2 human seats, not 6; per-client cost can be read directly from the table, but server cost with 6 humans is an extrapolation and was not measured.
- The server ran as the Loom dev `runServer`, not through the kit's `start.ps1`, because `fabric-server-launch.jar` is not on this machine and downloading it was not authorised. The kit scripts were only dry-run.
- No Spark (not available offline). Tick times come from `PerfProbe`.
- The EULA was set only inside the scratch server folder under the worktree. No live server, `C:/GGMatch` or `tjepkema-server` was touched.

## Not measured

- Six real clients, or any run with more than 2 humans.
- The kit's real launcher path (`start.ps1` live), JVM startup time and the Fabric launcher's own overhead.
- Network conditions: everything ran on loopback, so latency, jitter and loss are zero (ping column is 0). Bandwidth is bytes at the Netty layer, not NIC counters.
- GPU time and low-end hardware: fps comes from one desktop machine, uncapped, with the three Minecraft processes and Kort's client sharing CPU.
- Long matches (only 150 s battles) and repeated map switches for leak detection; heap max is used heap, not live set after GC.
- Per-system tick cost (no Spark profile), so the 7 to 8 ms is not attributed to a subsystem.
- Spectators, campaign mode and convoy battles; only the standard lobby skirmish path.
