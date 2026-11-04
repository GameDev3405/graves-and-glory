# Builder server

Creative server for the map/lobby builder. Hosted on tjepkema-server (`C:\GGBuild`). Public address `jensen-index.tun.ply.gg` through the playit tunnel (boot task `GGPlayit`, forwarding to `127.0.0.1:25570`); on Tailscale also `100.72.202.38:25570`. The server listens on all interfaces (`server-ip=` blank). Join with the Graves and Glory modpack. Whitelist is off (Kort 2026-11-03): anyone with the address and a real Minecraft account can join (`online-mode=true`), and every player becomes op (level 4) on their first join.

## For the builder
- All seven worlds are loaded at once: `camp`, `lobby`, `rivergate`, `ironhold`, `frostmarch`, `hollowwood`, `kingsfield`. `/map` lists them and says which one you are in; `/map <name>` takes only you to that world's arrival point, instantly. You log back in where you left.
- `/warp` lists the places of the world you are in; `/warp <name>` teleports there.
- Every world is a blank flat canvas: grass at y 64, dirt below, stone down to y 32, nothing under that. Planned spots are only 1-block concrete outlines set into the ground with oak signs naming them. Outlines and signs are guides: break, move or build over them. Each canvas is painted once, the first time the server starts, and never again.
- `lobby` and `camp`: the Last Watch camp plan (plan 2A.6, Kort 2026-11-02), 192 x 192 (x and z -96 to 95, a light gray edge line). Warps: `spawn`, `hearth-court`, `war-table`, `party-lectern`, `tent-ironclad`, `tent-gilded`, `tent-graveborn`, `tent-wildmoot`, `tent-tidecallers`, `graveyard`, `armoury`, `training-yard`, `pad-dawn`, `pad-dusk`, `assembly-bay-1`, `assembly-bay-2`, `war-gates`. North is -z. The layout is in `LobbyCanvas.java`.
- Each map: the size a battle builds today (light gray edge at x ±366, z ±266; white playable border at x ±350, z ±250, where the battle's barrier wall stands). Outlined from the map tables (`CampaignMaps.select`, the landmarks and supply-house sheets): both teams' gates (brown), barracks (black) and spawns (light blue Dawn, blue Dusk), capture points (red circles of their capture radius), lane crossings (cyan), supply houses (yellow), and every road and lane as a lime line. Warps: `center`, `gate-dawn`, `barracks-dawn`, `spawn-dawn`, the same for `dusk`, `point-a`..., `lane-north`, `lane-middle`, `lane-south`, `house-<id>`. The layout is in `BuildWorlds.java`.
- Creative, peaceful, always daytime, no mobs, no units, no match. Only what you build exists.
- Your builds are saved: vanilla autosave every 5 minutes, a backup zip every 30 minutes while someone is online, on stop, and on `/backup` (op).

## How it works
`BuildMode` activates only with `-Dgg.build` (no Battle is created). It registers the built-in data pack `resourcepacks/gg_build`, which adds seven flat dimensions `gravesandglory:build_<world>` to the one save `gg-build`. On first start each canvas is painted with its outlines and signs and gets a marker `gg-build.json` in its own dimension folder (`gg-build/dimensions/gravesandglory/build_<world>/`); a marked canvas is never painted again. The overworld is unused: anyone who lands there (a new player, a respawn) is sent to the lobby. `gg-build-opped.txt` in the server folder lists the players already auto-opped, so a later `deop` sticks. `start-build-server.ps1` runs the server on `gg-build` and relaunches after a crash.

The hardened supervisor, boot task, backup schedule, offsite copy and health check are in `tools/builder-server/README.md`.

Backups: `C:\GGBuild\backups\gg-build\<timestamp>.zip` (the whole save, every dimension); keep 48 hours, then one per day for 14 days. The old per-world folders `gg-build-<world>` and `backups\<world>` from before 2026-11-03 are left as they were.

## Operations
- Start/stop: scheduled task `GGBuilderServer` (runs at machine boot as SYSTEM, hidden, so the server is up whenever the PC is on). The script relaunches after a crash; a clean `/stop` (or an RCON `stop`) ends it, and `Start-ScheduledTask GGBuilderServer` starts it again. Prefer the admin scripts below over killing `java`.
- Admin console (added 2026-11-03): RCON listens on 25575 and is for this PC only. Two Windows Firewall block rules (IPv4 and IPv6, `Graves and Glory RCON 25575 ...`) drop everything but loopback, and 25575 is not in the playit tunnel. The password is in `C:\GGBuild\server.properties` and `C:\GGBuild\admin\rcon.secret`; both, and `admin\`, are readable by SYSTEM and Administrators only. Never paste it anywhere. Run these from an admin PowerShell on the server PC, or over ssh as `powershell -File C:\GGBuild\admin\<script>.ps1`:
  - `rcon.ps1 <command words>`: run any server command (for example `rcon.ps1 whitelist add Name`) and print the reply.
  - `players.ps1`: who is online (`list`).
  - `save.ps1`: `save-all flush`, returns when the world is on disk.
  - `stop.ps1`: saves, sends `stop`, waits for java and the supervisor to exit. The supervisor does not restart after it (it sees `admin\stop.flag`). Exits 2 and does nothing if players are online; `-Force` overrides.
  - `restart.ps1`: `stop.ps1` then `Start-ScheduledTask GGBuilderServer`, and waits until RCON answers. Takes about 30 seconds. Same player guard and `-Force`.
- Update the mod: stop, replace the jar in `C:\GGBuild\mods\`, start. World saves are untouched.
- Firewall rule `Graves and Glory builder 25570 (Tailscale only)`; the playit agent connects out, so it needs no inbound rule; the Kingdom servers (25565-25567, 25571) are not touched.
- When building is done, the finished canvases become the real maps/lobby (separate task; copy the dimension region folders).

## Deployed 2026-11-03

- Running main `bb81fd86` (jar sha1 `3703bd7d`). The old seven `gg-build-*` worlds are zipped in `C:\GGBuild\backups\pre-wipe-20261103\` and kept in `C:\GGBuild\old-worlds\` (Kort chose a blank start).
- playit: the official v1.2.0 `playitd.exe` (`C:\GGBuild\playit\v1.2.0`, signed by Developed Methods LLC) runs as the boot task `GGPlayit` with `--secret-path C:\GGBuild\playit\playit-secret.txt`; log `C:\GGBuild\playit\playitd.log`. The old v0.15.26 CLI could not route the anycast tunnel. Tunnels are made on the playit.gg site, not from the CLI.
- Python: the server PC's Python is a per-user install (kortt, 3.15.2), not on the SYSTEM path, so the supervisor now looks for it itself (`Resolve-Python` in `start-build-server.ps1`). Checked 2026-11-03 under SYSTEM via `restart.ps1`: log rotation, backup, version stamp and offsite copy all ran OK. An all-users install on PATH would still be sturdier. The mod's own backups (every 30 minutes, on stop, `/backup`) and autosave do not need Python.
- Player packs matching this jar: `dist/graves-and-glory-0.5.0.mrpack` and `-curseforge.zip` built from the same commit.

## Builder tools
Approved 2026-11-03 for the builder server and a builder-only client pack. They are not part of the game release (`dependencies/manifest.json`, release packs and `tools/package.py` are untouched). Exact files, versions and hashes: `tools/builder-server/builder-mods.json`.
- **Axiom** (editor: selections, copy/paste, gizmos, big brushes). Needed on the server and on the client. Builders install the builder pack, `dist/graves-and-glory-0.5.0-builder.mrpack`, made by `python tools/builder-server/make_builder_pack.py` from the finished game pack (it adds only the client mods and never rebuilds or publishes anything). Without the builder pack Axiom does not appear in game.
- **Distant Horizons 3.3.3-1.20.1** (client only; far-away terrain for builders). It is in the builder pack marked client required and server unsupported, so nothing goes on the server. Not yet tried in game. Entry in `tools/builder-server/builder-mods.json`.
- **WorldEdit** (server side; the client copy is optional). Wand is the wooden axe: left click sets position 1, right click sets position 2. Then `//set <block>`, `//replace`, `//copy`, `//paste`, `//undo`, `//redo`, `//rotate`, `//schem save <name>`.
- **Ledger** (server side, needs Fabric Language Kotlin). Logs every block change by player. `/ledger inspect` toggles click inspection of a block. `/ledger search <filters>` finds actions (for example `source:QuakeMoney42 time:1d`). `/ledger rollback <filters>` undoes them (same filters, for example `/ledger rollback source:awesome34 time:2h range:30`); `/ledger restore <filters>` re-applies a rollback. Search first, then roll back. Ledger needs op.
- Server jars to copy into `C:\GGBuild\mods` next to the game jar and Fabric API: `Axiom-6.1.5-for-MC1.20.1.jar`, `worldedit-mod-7.2.15.jar`, `ledger-1.2.8.jar`, `fabric-language-kotlin-1.10.20+kotlin.1.9.24.jar`. Stop the server first. The pins are the newest builds that run on Fabric Loader 0.16.14 and Fabric API 0.92.2.
