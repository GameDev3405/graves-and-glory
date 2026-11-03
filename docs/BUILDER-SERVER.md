# Builder server

Creative server for the map/lobby builder. Hosted on tjepkema-server (`C:\GGBuild`), Tailscale only: `100.72.202.38:25570`. Join with the Graves and Glory modpack. Whitelist is on: add Minecraft names to `C:\GGBuild\whitelist.json` (and `ops.json` for `/map`), then restart the `GGBuilderServer` scheduled task. Current names: QuakeMoney42 and awesome34 (both whitelisted and op).

## For the builder
- `/warp` lists places in the current world; `/warp <name>` teleports (`spawn`, `center`, `point-a`, landmarks, barracks, stations).
- `/map` lists worlds; `/map <name>` switches (camp, rivergate, ironhold, frostmarch, hollowwood, kingsfield). Needs op. The server saves, restarts, and you reconnect after about 30 seconds.
- Creative, peaceful, always daytime, no mobs, no units, no match. Only structures exist.
- Your builds are saved: each world keeps its own save, generated once, never regenerated. Automatic backup every 30 minutes while someone is online and on stop; `/backup` (op) forces one.

## How it works
`BuildMode` activates only with `-Dgg.build=<world>` (no Battle is created). `start-build-server.ps1` loops: it points `level-name` at `gg-build-<world>`, runs the server, and relaunches on the world named in `next-world.txt` (written by `/map`). `gg-build.json` in each save marks it generated and names its world; a mismatched save stops the server instead of being overwritten.

Backups: `C:\GGBuild\backups\<world>\<timestamp>.zip`; keep 48 hours, then one per day for 14 days.

## Operations
- Start/stop: scheduled task `GGBuilderServer` (runs at machine boot as SYSTEM, hidden, so the server is up whenever the PC is on). The script relaunches after a crash; a clean `/stop` ends it. To stop for good, stop the task and kill the `java` process with `gg.build` in its command line.
- Update the mod: stop, replace `C:\GGBuild\mods\graves-and-glory-0.5.0.jar`, start. World saves are untouched.
- Firewall rule `Graves and Glory builder 25570 (Tailscale only)`; the Kingdom servers (25565-25567, 25571) are not touched.
- When building is done, the finished worlds become the real maps/lobby (separate task; copy the saves).

## Builder tools
Approved 2026-11-03 for the builder server and a builder-only client pack. They are not part of the game release (`dependencies/manifest.json`, release packs and `tools/package.py` are untouched). Exact files, versions and hashes: `tools/builder-server/builder-mods.json`.
- **Axiom** (editor: selections, copy/paste, gizmos, big brushes). Needed on the server and on the client. Builders install the builder pack, `dist/graves-and-glory-0.5.0-builder.mrpack`, made by `python tools/builder-server/make_builder_pack.py` from the finished game pack (it adds only the client mods and never rebuilds or publishes anything). Without the builder pack Axiom does not appear in game.
- **WorldEdit** (server side; the client copy is optional). Wand is the wooden axe: left click sets position 1, right click sets position 2. Then `//set <block>`, `//replace`, `//copy`, `//paste`, `//undo`, `//redo`, `//rotate`, `//schem save <name>`.
- **Ledger** (server side, needs Fabric Language Kotlin). Logs every block change by player. `/ledger inspect` toggles click inspection of a block. `/ledger search <filters>` finds actions (for example `source:QuakeMoney42 time:1d`). `/ledger rollback <filters>` undoes them (same filters, for example `/ledger rollback source:awesome34 time:2h range:30`); `/ledger restore <filters>` re-applies a rollback. Search first, then roll back. Ledger needs op.
- Server jars to copy into `C:\GGBuild\mods` next to the game jar and Fabric API: `Axiom-6.1.5-for-MC1.20.1.jar`, `worldedit-mod-7.2.15.jar`, `ledger-1.2.8.jar`, `fabric-language-kotlin-1.10.20+kotlin.1.9.24.jar`. Stop the server first. The pins are the newest builds that run on Fabric Loader 0.16.14 and Fabric API 0.92.2.
