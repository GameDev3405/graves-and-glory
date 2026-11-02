# Match server runbook (task 3.5)

Kit: `tools/server-kit/` (full detail in its `README.md`). Target for the six-player slice test: `C:\GGMatch` on tjepkema-server, port **25580**, Tailscale only. Do not touch the builder (25570) or Kingdom ports (25565-25567, 25571). Deploying is Kort's step.

## One-time setup (PowerShell, in `C:\GGMatch`)
1. Copy everything in `tools/server-kit/` into the folder.
2. Put in the folder: `fabric-server-launch.jar` (Fabric 0.16.14 for 1.20.1, sha256 `856a5f9f...cf113`, from `tools/export_server.py`'s `setup-server.ps1` or meta.fabricmc.net), and in `mods\`: exactly one `graves-and-glory-<version>.jar` plus `fabric-api-0.92.2+1.20.1.jar`.
3. `.\gate.ps1 -Pin` after confirming that jar is the build the players have (it writes `mod-lock.json`).
4. `.\start.ps1 -MemoryGB 6` once. It creates `server.properties` (port 25580, whitelist on, online-mode on, RCON off, 16 slots), an empty `whitelist.json`, and `eula.txt` with `eula=false`. Kort reads the EULA and sets `eula=true` himself.
5. Whitelist the six names (`whitelist add <name>` in the server window, or edit `whitelist.json`). Ops: Kort only. Set `max-players=6` if desired (kit default 16 for visitors).
6. Firewall: allow 25580 on the Tailscale interface only, mirroring the builder rule.

## Running a session
- Start: `.\start.ps1 -MemoryGB 6` in a visible window. It checks Java 17+, the version gate, EULA, takes a verified world backup, then launches with Aikar flags.
- Stop: `.\stop.ps1` (clean save; never kills). Typing `stop` in the window also works.
- Backup: `.\backup.ps1 -Label manual` (server stopped). Every start also backs up first. Restore: `.\restore.ps1 -Backup backups\<file>.zip` (old world kept as `.replaced-<time>`).
- New build: `.\update.ps1 -Jar <path>` (stops, backs up, retires the old jar, installs and re-pins). Rollback: stop, move the jar from `mods-retired\` back, restore `mod-lock.json.prev`.
- Tick data for the MSPT target: `/tick query` is not in 1.20.1; use the server log's "Can't keep up" lines or add Spark after Kort approves it.

## Version gate (two layers)
1. Server side kit: `start`/`gate` refuse a missing, duplicate, wrong-Minecraft or unpinned jar.
2. In the mod (`VersionGate`): at login the client sends protocol, version and source build hash; a mismatch is disconnected with "Install the same modpack release". Friends must run the same build as the pinned jar.

## Before the 3.7 test
Run the smoke join from the kit's report once on the real host, then have each friend join with the exact modpack build.
