# Builder server

Creative server for the map/lobby builder. Hosted on tjepkema-server (`C:\GGBuild`), Tailscale only: `100.72.202.38:25570`. Join with the Graves and Glory modpack. Whitelist is on: add Minecraft names to `C:\GGBuild\whitelist.json` (and `ops.json` for `/map`), then restart the `GGBuilderServer` scheduled task. Current names: QuakeMoney42 and awesome34 (both whitelisted and op).

## For the builder
- `/warp` lists places in the current world; `/warp <name>` teleports (`spawn`, `center`, `point-a`, landmarks, barracks, stations).
- `/map` lists worlds; `/map <name>` switches (camp, lobby, rivergate, ironhold, frostmarch, hollowwood, kingsfield). Needs op. The server saves, restarts, and you reconnect after about 30 seconds.
- `/map lobby` is the blank canvas for hand-building the new camp (the Last Watch, plan 2A.6, Kort 2026-11-02): flat grass, 192 x 192 blocks (x and z -96 to 95, ground at y 64, stone down to y 32), no structures. Each planned area is only a 1-block concrete outline in the ground with signs saying what goes there and what the station does. Warps: `spawn`, `hearth-court`, `war-table`, `party-lectern`, `tent-ironclad`, `tent-gilded`, `tent-graveborn`, `tent-wildmoot`, `tent-tidecallers`, `graveyard`, `armoury`, `training-yard`, `pad-dawn`, `pad-dusk`, `assembly-bay-1`, `assembly-bay-2`, `war-gates`. North is -z: War Gates at the south edge, hearth court with the war table and party lectern at the centre, House tents in a row to the north, graveyard beyond them, armoury and training yard on the west, team pads and assembly bays between spawn and gates; the east side is free. Outlines and signs are only guides: break, move or build over them. The layout is in `LobbyCanvas.java`.
- Creative, peaceful, always daytime, no mobs, no units, no match. Only structures exist.
- Your builds are saved: each world keeps its own save, generated once, never regenerated. Automatic backup every 30 minutes while someone is online and on stop; `/backup` (op) forces one.

## How it works
`BuildMode` activates only with `-Dgg.build=<world>` (no Battle is created). `start-build-server.ps1` loops: it points `level-name` at `gg-build-<world>`, runs the server, and relaunches on the world named in `next-world.txt` (written by `/map`). `gg-build.json` in each save marks it generated and names its world; a mismatched save stops the server instead of being overwritten.

The hardened supervisor, boot task, backup schedule, offsite copy and health check are in `tools/builder-server/README.md`.

Backups: `C:\GGBuild\backups\<world>\<timestamp>.zip`; keep 48 hours, then one per day for 14 days.

## Operations
- Start/stop: scheduled task `GGBuilderServer` (runs at machine boot as SYSTEM, hidden, so the server is up whenever the PC is on). The script relaunches after a crash; a clean `/stop` ends it. To stop for good, stop the task and kill the `java` process with `gg.build` in its command line.
- Update the mod: stop, replace `C:\GGBuild\mods\graves-and-glory-0.5.0.jar`, start. World saves are untouched.
- Firewall rule `Graves and Glory builder 25570 (Tailscale only)`; the Kingdom servers (25565-25567, 25571) are not touched.
- When building is done, the finished worlds become the real maps/lobby (separate task; copy the saves).
