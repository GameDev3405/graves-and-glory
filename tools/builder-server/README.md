# Builder server operator guide

Plan task 2A.1. The builder server is the canonical editable world (see `docs/BUILDER-SERVER.md` for what builders see). This folder is the operations kit. Copy it into the server folder (`C:\GGBuild`) next to `fabric-server-launch.jar`, `mods/` and the worlds. Needs Windows PowerShell 5.1 or newer, Java 17+ and Python 3 on `PATH`.

## Files

| File | What it does |
|---|---|
| `start-build-server.ps1` | Supervisor loop: checks, backs up, starts the one save `gg-build` (all seven worlds as dimensions), restarts after a crash |
| `install-boot-task.ps1` | Registers the `GGBuilderServer` boot task (elevated PowerShell; `-WhatIf` previews) |
| `health-check.ps1` | Prints health; exit 0 OK, 1 WARN, 2 DOWN |
| `backup-now.ps1` | Manual backup; `-Offsite` also copies offsite |
| `builder_ops.py` | The logic behind all of the above (backup, prune, offsite, rotate-logs, stamp, health, preflight) |

## First start

1. The owner reads and accepts the Minecraft EULA by hand: run the server once, open `eula.txt`, set `eula=true`. The scripts never create or edit it. Without it `start-build-server.ps1` logs the reason and exits with code 3 instead of looping.
2. `powershell -File start-build-server.ps1` in a window to watch it, or install the boot task:
   `powershell -File install-boot-task.ps1` (elevated), then `Start-ScheduledTask -TaskName GGBuilderServer`.
3. Whitelist and ops (Kort 2026-11-03): `white-list=false`, `enforce-whitelist=false`, `online-mode=true`, `op-permission-level=4`. The mod makes every player op on their first join and records them in `gg-build-opped.txt`, so a later `deop <name>` sticks. `server-ip=` is blank so the server listens on every interface: the playit tunnel (boot task `GGPlayit`) forwards the public address `jensen-index.tun.ply.gg` to `127.0.0.1:25570`, and Tailscale still reaches it.

## What the supervisor does

- **Before every start:** preflight (launcher jar and EULA), log cleanup, a `-prestart` backup (skipped if the world has not changed since the newest backup), a `version-stamp.json` (world, start time, hash and size of every mod jar and the launcher), an offsite copy.
- **Crash restart with backoff:** a nonzero exit restarts after 5 s, then 10, 20 ... up to 300 s. A run of 10 minutes or more resets the backoff. After 10 failed starts in a row it stops with exit code 2 so a broken jar does not loop forever. Tune with `-BackoffStartSeconds`, `-BackoffMaxSeconds`, `-HealthyAfterSeconds`, `-MaxQuickFailures`.
- **One save:** the server always runs `level-name=gg-build` with `-Dgg.build=all`; `/map` moves players between dimensions in game, so the supervisor never switches worlds. A leftover `next-world.txt` from the old mod is deleted. A clean `/stop` (exit 0) ends the supervisor. Exit 0 still counts as a crash, and restarts with the backoff, when the run left a new file in `crash-reports/` or a tick-loop crash line in `logs/latest.log` (checked in PowerShell, so it works without Python; dry run: `start-build-server.ps1 -CheckCrashedSince <unix seconds> -CheckDir <server dir>`).
- **While running:** every `-BackupIntervalMinutes` (default 60) it runs the scheduled backup, the offsite copy and log cleanup.
- Log of everything the supervisor does: `logs/supervisor.log` (rotated at 2 MB, 5 kept). Server log: `logs/latest.log`, rotated by Minecraft; `.log.gz` files older than 14 days are deleted.

## Backups

- Location: `backups/gg-build/<yyyyMMdd-HHmmss>[-reason].zip`, entries relative to the world folder (same layout as the in-game backup, so one restore recipe covers all).
- Sources:
  - The mod itself zips after `save-all` every 30 minutes while there are changes, on stop, and on `/backup` (op). These are always consistent.
  - `-prestart`: taken while the server is stopped (consistent). Refused if the world is in use.
  - `-scheduled`: the supervisor's timer, only when the world changed since the newest backup and that backup is older than the interval.
  - `-live`: a scheduled backup taken while the server runs. Best effort: Minecraft keeps writing, so one chunk could be torn. The mod's own backups are the consistent ones; prefer them for restores.
  - `-manual`: `backup-now.ps1`.
- Retention (the mod's rule, applied to every suffix): everything under 48 hours, then the newest of each day until 14 days, then deleted. Half-written `.part` files are cleaned up.
- **Offsite copy:** put a folder path (a synced cloud folder or an external drive) in `offsite.txt`, or set `GG_OFFSITE`, or run `builder_ops.py offsite --to <path>`. New zips are copied there under the same names and the same retention applies there only. Unconfigured means skipped with a log line, so set it before relying on it.
- **Restore:** stop the server (or the task), move the damaged `gg-build` folder aside (do not delete it), unzip the chosen zip into a fresh `gg-build` folder, start. Each dimension's `gg-build.json` marker comes back with the zip, so no canvas is repainted.

## Health check

`powershell -File health-check.ps1` prints and writes `health.json`:

```
status: OK   world: gg-build   port: 25570
up: yes   version: 1.20.1   players: 0/10
tps/mspt: n/a (0 overload warnings in recent log, worst 0 ms behind)
last backup: 20261101-194100-prestart.zip (0 min ago)
started: 2026-11-01T19:41:08   mods: graves-and-glory-0.5.0.jar 37f29dc4
```

- Up/down is a real Minecraft status ping to `server-ip:server-port` from `server.properties`.
- **TPS/MSPT are not available:** Minecraft 1.20.1 has no `/tick` command and the server has no RCON. The proxy reported is the count and worst size of "Can't keep up" warnings in the recent `latest.log`. Reading real TPS would need a small mod-side exporter, which is not part of this task.
- WARN: newest backup older than 24 hours, no backup for the current world, or 5 or more overload warnings. DOWN: no answer.
- Read-only: it never starts, stops or signals anything.

## Prop resource pack

Off by default. To send the prop pack, add Java flags before `-jar` in `start-build-server.ps1` (it does not pass them yet): `-Dgg.propPack.file=<zip, or a folder with gg-props-latest.sha1>`, plus either `-Dgg.propPack.port=<free port>` (the server serves the zip itself; open that port like the game port) or an external host, and `-Dgg.propPack.url=<public URL, ending in / for a folder>`. `-Dgg.propPack.required=false` lets players decline. Update: rebuild with `python tools/build_prop_pack.py`, copy the zip and `gg-props-latest.sha1` into the pack folder (upload the zip too when hosted elsewhere), then RCON `prop pack reload`. No restart. Details and hosting options: `docs/BUILDER-SERVER.md`, "Prop resource pack".

## Stopping and updating

- Stop for good: stop the `GGBuilderServer` task (or close the supervisor window), then stop the Java process that the supervisor started; its PID is in `server.pid`. Do not stop other Java processes on this PC (the Kingdom servers and any game clients are separate).
- Mod update: stop, take `backup-now.ps1 -Offsite`, replace the jar in `mods/`, start. The next start rewrites `version-stamp.json`, so the health output shows the new hash. The scripted update loop is task 2A.8.
- The firewall rule and Tailscale-only address are described in `docs/BUILDER-SERVER.md`.

## Not covered here

Real TPS/MSPT, whitelist automation (needs player UUIDs), and the scripted jar-and-overlay swap (2A.8). Tests for the logic: `python -m unittest test-tools/standalone/test_builder_ops.py`; they use temporary folders and a fake status server, never a real world.
