# Graves and Glory dedicated server

Run one Graves and Glory server on Minecraft 1.20.1 with Fabric. This folder is the whole server kit: the server download extracts to exactly this layout, and `tools/server-kit/` in the repository is its source. Port **25580**, whitelist on. Windows (`.ps1`) and Linux (`.sh`) scripts do the same jobs. The only thing a script downloads is the Fabric launcher (`setup`), checked against a pinned SHA-256. Nothing here touches another machine or accepts the Minecraft EULA for you.

## What is in the folder

| File | Job |
|---|---|
| `mods/` | The Graves and Glory jar and the four server dependency jars (Fabric API, Lithium, FerriteCore, ModernFix). Not in the repository folder; the server zip adds it |
| `docs/` | Licence notices and third-party sources (server zip only) |
| `setup.ps1` / `setup.sh` | Downloads the pinned Fabric 0.16.14 launcher (`fabric-server-launch.jar`) and checks its SHA-256. Starts nothing |
| `start.ps1` / `start.sh` | Checks Java, version gate, config, EULA, backs up the world, then launches with Aikar flags |
| `stop.ps1` / `stop.sh` | Clean stop (world saved). Never kills the process |
| `gate.ps1` / `gate.sh` | Version gate; `-Pin` / `--pin` records the approved jar |
| `backup.ps1` / `backup.sh` | Verified world archive in `backups/` |
| `restore.ps1` / `restore.sh` | Put a backup back; backs up first, keeps the replaced world |
| `update.ps1` / `update.sh` | Stop, back up, swap jar, pin, optional restart |
| `lib.ps1` / `lib.sh` | Shared code |
| `server.properties.default` | Defaults: port 25580, whitelist on, online mode on, RCON off, 16 slots. Copied to `server.properties` on first start only |
| `whitelist.template.json` | Empty whitelist (`[]`), copied to `whitelist.json` on first start only |
| `jvm-args.txt` | Optional, yours: extra JVM flags, one per line (not shipped) |

Every script takes `-DryRun` (`--dry-run` for `.sh`) and `-Root DIR` (`--root DIR`). A dry run does all the checks and prints what it would do; it writes and starts nothing. Scripts work in the folder they sit in.

## Set up (a person does these steps)

Extract the server zip into a new folder, for example `C:\GGServer` on Windows or `/opt/gg` on Linux. Run all commands from it. When you later extract a newer zip over it, never overwrite your own `server.properties`, `whitelist.json` or `eula.txt`; the zip does not contain those names.

1. **Java 17 or newer.** The scripts refuse older Java.
2. **Get the Fabric launcher:** `.\setup.ps1` (Linux: `./setup.sh`; Linux and macOS need `chmod +x *.sh` if your unzip dropped the permission). It downloads the pinned Fabric 0.16.14 launcher and checks its SHA-256. It will not replace a launcher that is already there.
3. **Check `mods/`.** It must hold exactly one `graves-and-glory-<version>.jar` plus the four dependency jars. The test-tool jar (`*DO-NOT-SHIP*`) and `-sources` jars are refused.
4. **Pin the jar:** `.\gate.ps1 -Pin` (or `./gate.sh --pin`). This writes `mod-lock.json` with the version, build identity and file hash. Do this only after you know this is the build your players have.
5. **EULA.** Run `.\start.ps1`. It creates `eula.txt` containing `eula=false` and stops. Read https://aka.ms/MinecraftEULA yourself. If you agree, edit `eula.txt` to `eula=true`. No script in this kit writes that line.
6. **Whitelist.** `whitelist.json` starts empty and `white-list=true`, so nobody can join yet. Start the server and type `whitelist add <name>` in its console, or add entries by hand (`{"uuid": "...", "name": "..."}`).
7. **Start:** `.\start.ps1` (Linux: `./start.sh`). Optional `-MemoryGB 8` (and `-JvmArgs`, see Extra JVM flags); the default is 6 (min and max heap equal, as Aikar advises). The first start downloads Minecraft and Fabric libraries and builds the camp, so it is slow.
8. **Reach it.** Players join `<host>:25580` with the exact same client build. Open that one port in the host firewall, or keep it on Tailscale. The default binds all interfaces; set `server-ip=` in `server.properties` to pin one address. Port 25580 stays clear of 25565-25567, 25570 and 25571, which other servers on the same machine may use.
9. **Smoke test:** join as a whitelisted player. The mod itself also refuses mismatched clients at login.

Keep `level-name=Graves and Glory` in `server.properties`: the mod only activates on a world with that internal name (or the legacy `Bannerbound`), and logs why when it stays inactive. A random existing world is not converted. One shared battlefield per server process; the default 16 slots allow up to eight battle commanders plus camp visitors.

## Daily use

- **Stop:** `.\stop.ps1` (Windows) saves and exits. On Windows `start.ps1` must be the process that launched the server; it passes `stop` to the console. Typing `stop` in its window also works. On Linux, `./stop.sh` sends SIGTERM, which runs the server's normal save and shutdown. Both wait up to 120 s and never force-kill.
- **Keep it running:** run `start.ps1` from a Task Scheduler task (at startup, restart on failure), or `start.sh` from systemd (`Restart=on-failure`, `ExecStop=/opt/gg/stop.sh`) or screen/tmux. The scripts do not loop on their own.
- **Back up:** `.\backup.ps1 -Label manual`. Output is `backups/<world>-<UTC time>-<label>.zip` (`.tar.gz` on Linux), read back and checked for `level.dat` before it counts. The server must be stopped (`-AllowRunning` overrides at your risk). Start backups (`start`) and `auto` backups keep the newest 20; every other label is kept forever. Add a scheduled `backup.ps1 -Label auto -AllowRunning` only if you accept a possibly inconsistent copy; the safe copy is the one made at each start.
- **Restore:** stop the server, then `.\restore.ps1 -Backup backups\<file>.zip`. It backs up the current world as `prerestore`, then moves it to `<world>.replaced-<time>`. Nothing is deleted. A `.zip` restores on Windows, a `.tar.gz` on Linux; convert by hand if you move between them.

## Extra JVM flags

To add JVM flags (for example `-Dgg.build=camp`), either pass them on the command line or list them in `jvm-args.txt` next to `start.ps1`:

```
.\start.ps1 -JvmArgs "-Dgg.build=camp -Xss1M"      (Linux: ./start.sh --jvm-args "-Dgg.build=camp -Xss1M")
```

`jvm-args.txt` holds one flag per line; `#` starts a comment and blank lines are ignored. File flags come first, then the command-line flags, both after the Aikar flags and before `-jar`, so a later flag wins over an Aikar default. Every flag must start with `-`; anything else stops the start. A dry run prints the final command. The kit does not ship a `jvm-args.txt`, so updating the kit never overwrites yours.

The Java version check reads the line that contains `version "N"` from `java -version`, so a `JAVA_TOOL_OPTIONS` or `JDK_JAVA_OPTIONS` banner ("Picked up JAVA_TOOL_OPTIONS: ...") ahead of it no longer breaks the check. Setting `JAVA_TOOL_OPTIONS` also works as a way to pass flags, but `jvm-args.txt` is the supported route.

## Update

When there is a new build:

```
.\update.ps1 -Jar C:\path\to\graves-and-glory-0.5.1.jar -Restart
```

It checks the new jar (mod id, Minecraft 1.20.1, build record), stops the server if it is running, makes a `preupdate` backup (abort if it fails), moves the old jar to `mods-retired/`, installs the new one as `graves-and-glory-<version>.jar`, re-pins `mod-lock.json`, and with `-Restart` starts the server again. You then join `<host>:25580` as the smoke test. The previous lock is kept as `mod-lock.json.prev`. Rollback: stop, move the retired jar back into `mods/`, restore `mod-lock.json.prev` over `mod-lock.json`.

Map artifacts live inside the world, so there is no separate map swap. To change maps, restore a world archive with `restore`.

## How the guards work

- **Version gate** (`start` and `gate`): refuses when `mods/` has no Graves and Glory jar or more than one, when the jar is not mod id `gravesandglory`, targets any Minecraft but 1.20.1, has no `gravesandglory-build.json`, or does not match `mod-lock.json` by version, source hash and file hash. A rebuilt jar needs a new pin, on purpose.
- **No world without a backup:** `start` backs up first and aborts if the backup fails. `update` and `restore` do the same. A backup never overwrites an existing archive. Restore moves the old world aside rather than deleting it.
- **EULA:** `start` refuses unless `eula.txt` says `eula=true`, and only a person writes that.

## Defaults and limits

- World folder is `Graves and Glory`. The 16 player slots and the flat-world, adventure-mode, normal-difficulty defaults are the project's server template. The whitelist is on and enforced, online mode is on, RCON and query are off.
- The default heap is 6 GB; confirm it against the host's RAM. Aikar flags are the Paper docs set, with the 12 GB and up variant used from 12 GB.
- All mod state is inside the world folder, so a world backup is a full backup. The whitelist, ops and `mod-lock.json` are small; copy them by hand if you care.
- Tested on Windows against a real 1.20.1 Fabric server and a client. The Linux `.sh` scripts have only been dry-run checked and have not booted a server.
- This is a plain server for friends. It has no public matchmaking, and local-profile imports are still trusted.
