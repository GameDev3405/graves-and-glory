# Graves and Glory dedicated server kit (task 3.5, update loop 2A.8)

Plain files for running one Graves and Glory server on Minecraft 1.20.1 with Fabric. Port **25580**. Windows (`.ps1`) and Linux (`.sh`) twins do the same jobs. Nothing here downloads anything, touches a remote machine, or accepts the Minecraft EULA for you.

## What is in the kit

| File | Job |
|---|---|
| `start.ps1` / `start.sh` | Checks Java, version gate, config, EULA, backs up the world, then launches with Aikar flags |
| `stop.ps1` / `stop.sh` | Clean stop (world saved). Never kills the process |
| `gate.ps1` / `gate.sh` | Version gate; `-Pin` / `--pin` records the approved jar |
| `backup.ps1` / `backup.sh` | Verified world archive in `backups/` |
| `restore.ps1` / `restore.sh` | Put a backup back; backs up first, keeps the replaced world |
| `update.ps1` / `update.sh` | Stop, back up, swap jar, pin, optional restart |
| `lib.ps1` / `lib.sh` | Shared code |
| `server.properties.default` | Defaults: port 25580, whitelist on, online mode on, RCON off, 16 slots |
| `whitelist.template.json` | Empty whitelist (`[]`) |

Every script takes `-DryRun` (`--dry-run` for `.sh`) and `-Root DIR` (`--root DIR`). A dry run does all the checks and prints what it would do; it writes and starts nothing. Scripts work in the folder they sit in, so copy the whole kit into the server folder.

## Install (a person does these steps)

Pick a folder, for example `C:\GGServer` on Windows or `/opt/gg` on Linux. Run all commands from it.

1. **Java 17 or newer.** Java 21 is what the research notes used. The scripts refuse older Java.
2. **Copy this kit** (everything in `tools/server-kit/`) into the folder. Do not overwrite a live `server.properties`, `whitelist.json` or `eula.txt` when you copy a newer kit later; the kit does not ship those names.
3. **Get the Fabric launcher and the server mods.** Two ways:
   - Run `python tools/export_server.py` in the repo (needs a release build and the package step), extract `dist/*-server.zip` into the folder, and run its `setup-server.ps1`. It downloads the pinned Fabric 0.16.14 launcher and checks its SHA-256. Then **delete the extracted `server.properties`** (it binds localhost:25565) and the extracted `start-server.ps1`, and use this kit's scripts instead. That zip also carries the four server dependency jars in `mods/`.
   - Or install Fabric Loader 0.16.14 for 1.20.1 yourself so that `fabric-server-launch.jar` is in the folder, and put the mod jar and Fabric API 0.92.2+1.20.1 in `mods/`.
4. **Put exactly one `graves-and-glory-<version>.jar` in `mods/`.** The test-tool jar (`*DO-NOT-SHIP*`) and `-sources` jars are refused.
5. **Pin the jar:** `.\gate.ps1 -Pin` (or `./gate.sh --pin`). This writes `mod-lock.json` with the version, build identity and file hash. Do this only after you know this is the build your players have.
6. **EULA.** Run `.\start.ps1`. It creates `eula.txt` containing `eula=false` and stops. Read https://aka.ms/MinecraftEULA yourself. If you agree, edit `eula.txt` to `eula=true`. No script in this kit writes that line.
7. **Whitelist.** `whitelist.json` starts empty and `white-list=true`, so nobody can join yet. Start the server and type `whitelist add <name>` in its console, or add entries by hand (`{"uuid": "...", "name": "..."}`).
8. **Start:** `.\start.ps1` (Linux: `./start.sh`). Optional `-MemoryGB 8`; the default is 6 (min and max heap equal, as Aikar advises). First start downloads Minecraft and Fabric libraries and builds the camp, so it is slow.
9. **Reach it.** Players join `<host>:25580`. Open that one port in the host firewall, or keep it on Tailscale. The builder server uses 25570 and the Kingdom servers 25565-25567 and 25571, so 25580 does not collide. The default binds all interfaces; set `server-ip=` in `server.properties` to pin one address.
10. **Smoke test:** join as a whitelisted player with the matching client build. The mod itself also refuses mismatched clients at login.

## Daily use

- **Stop:** `.\stop.ps1` (Windows) saves and exits. On Windows `start.ps1` must be the process that launched the server; it passes `stop` to the console. Typing `stop` in its window also works. On Linux, `./stop.sh` sends SIGTERM, which runs the server's normal save and shutdown. Both wait up to 120 s and never force-kill.
- **Keep it running:** run `start.ps1` from a Task Scheduler task (at startup, restart on failure), or `start.sh` from systemd (`Restart=on-failure`, `ExecStop=/opt/gg/stop.sh`) or screen/tmux. The scripts do not loop on their own.
- **Back up:** `.\backup.ps1 -Label manual`. Output is `backups/<world>-<UTC time>-<label>.zip` (`.tar.gz` on Linux), read back and checked for `level.dat` before it counts. The server must be stopped (`-AllowRunning` overrides at your risk). Start backups (`start`) and `auto` backups keep the newest 20; every other label is kept forever. Add a scheduled `backup.ps1 -Label auto -AllowRunning` only if you accept a possibly inconsistent copy; the safe copy is the one made at each start.
- **Restore:** stop the server, then `.\restore.ps1 -Backup backups\<file>.zip`. It backs up the current world as `prerestore`, then moves it to `<world>.replaced-<time>`. Nothing is deleted. A `.zip` restores on Windows, a `.tar.gz` on Linux; convert by hand if you move between them.

## Update loop (2A.8)

After each candidate build:

```
.\update.ps1 -Jar C:\path\to\graves-and-glory-0.5.1.jar -Restart
```

It checks the new jar (mod id, Minecraft 1.20.1, build record), stops the server if it is running, makes a `preupdate` backup (abort if it fails), moves the old jar to `mods-retired/`, installs the new one as `graves-and-glory-<version>.jar`, re-pins `mod-lock.json`, and with `-Restart` starts the server again. You then join `<host>:25580` as the smoke test. The previous lock is kept as `mod-lock.json.prev`. Rollback: stop, move the retired jar back into `mods/`, restore `mod-lock.json.prev` over `mod-lock.json`.

Map artifacts live inside the world, so there is no separate map swap. To change maps, restore a world archive with `restore`.

## How the guards work

- **Version gate** (`start` and `gate`): refuses when `mods/` has no Graves and Glory jar or more than one, when the jar is not mod id `gravesandglory`, targets any Minecraft but 1.20.1, has no `gravesandglory-build.json`, or does not match `mod-lock.json` by version, source hash and file hash. A rebuilt jar needs a new pin, on purpose.
- **No world without a backup:** `start` backs up first and aborts if the backup fails. `update` and `restore` do the same. A backup never overwrites an existing archive. Restore moves the old world aside rather than deleting it.
- **EULA:** `start` refuses unless `eula.txt` says `eula=true`, and only a person writes that.

## Assumptions made while building this (change freely)

- World folder is `Graves and Glory` (the project's existing template and activation check use that name).
- 6 GB default heap from the research notes; confirm it against the host's RAM. Aikar flags are the Paper docs set, with the 12 GB and up variant used from 12 GB.
- 16 player slots and the flat-world, adventure-mode, normal-difficulty defaults copy the project's server template.
- No server-side performance mods (Lithium and the rest in the research notes) are included; they need a licence and compatibility check first.
- Whitelist is on and enforced. Online mode is on; RCON and query are off.
- All mod state is inside the world folder, so a world backup is a full backup. Whitelist, ops and `mod-lock.json` are small; copy them by hand if you care.
- Tested here only with dry runs, a real 0.5.0 jar from the main checkout's build, a synthetic 0.5.1 jar, and a stub `java` for the Windows start/stop path. It has not run a real Minecraft server, and nothing was installed on tjepkema-server.
