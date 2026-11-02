# Performance baseline with real clients, 2026-11 (plan task 3.6) - WIP draft

Branch `work/perf-baseline-clients` off main `3fa9dd1` (the name `work/perf-baseline` is already taken by the bot-only baseline, checked out in `bannerbound-perf`, and is in main as `docs/reports/perf-baseline-2026-11-02.md`). Not pushed.

## Status

Tooling written and compiled: `PerfProbe` (server tick, build time, heap, chunks per 100 ticks) and `PerfClient` (harness client: joins, forms a War Table room, readies per map, samples frame times, heap and network bytes per second). Results below are filled in as runs finish.

## Deviations from the plan

- Plan says six clients. Cap is 5 Minecraft processes in total and Kort's review client (PID 29360) is one. This run uses 1 server + 2 harness clients (3 of mine) and 4 bot seats.
- Server is the Loom dev `runServer` with the kit's heap and Aikar flags (3 GB), not the kit's `start.ps1`: the Fabric launcher `fabric-server-launch.jar` is not on this machine and downloading it was not authorised. The kit's `gate.ps1 -Pin` and `start.ps1 -DryRun` were run on the real built jar and passed (version gate OK, Aikar command printed, EULA refused until set).
- Spark is not available offline; tick times come from `PerfProbe` (START to END of the server tick).
- EULA was set only inside the scratch folder `work/perf-srv/run`.

## Results

(pending)
