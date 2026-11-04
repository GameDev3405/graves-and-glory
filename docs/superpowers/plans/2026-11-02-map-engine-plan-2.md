# Map Engine Plan 2: the mod reads engine output

**Goal:** a design built by `python tools/mapengine build <id>` becomes a playable battlefield in the mod, deterministically; after the mod places it, an in-mod check compares spawns, points, distances and walkability against the design; a headless dedicated-server harness proves it on `maps/sample`.

**Spec:** `docs/superpowers/specs/2026-11-01-map-engine-design.md`. **Base:** `main` `def8e74`, branch `work/map-engine-plan-2`.

## Boundaries (Kort's gates)

- **Opt-in only.** The engine map is used only when the JVM property `-Dgravesandglory.engineMap=<dir with map.json>` is set. It replaces one map slot, `-Dgravesandglory.engineMapSlot=<index>` (default 1, Ironhold), for that JVM. With the property absent, nothing changes: no existing map, packet, save format or `Spec.java` change. `MapAdoption.GENERATOR` is not bumped (no generated block changes).
- The slot's Spec rows (points, structures, terrain, scenery, landmarks, supply houses) are swapped in memory, the same way `CampaignMaps.select` already swaps them. Only Domination is supported on an engine slot in this plan; siege and convoy keep their hand-written geometry and are refused for the slot.
- **Multiplayer limit (open question for Kort):** remote clients rebuild Spec tables locally, so they need the same property and files. Sending the map over the network is a packet change and needs Kort.
- The layout cache marker hash (`Structures.hash`) includes the engine design hash on an engine slot, so a cached world never mixes maps.

## Coordinates

Design cell (x, z) on the 700 by 500 canvas maps to world (x - 350, z - 250). Design `baseY` 64 is world y 64. A canvas of any other size is refused with a message. The world margin (to `worldHalfX` 366, `worldHalfZ` 266) repeats the nearest edge column. Barriers stand 6 high at |x| = 350 or |z| = 250, as on the generated maps.

## Task 1: engine output for the mod (Python)

- `build.build` also writes `columns.bin`: little-endian int16, two planes [z][x] of world y, solid top then water top, `-32768` for none.
- `map.json` gains `width`, `depth`, `origin` [-350, -250], `roads` (polylines with width) and `routes`: one row per spawn and point, `{"spawn", "point", "straight", "walk"}`; `straight` is the design distance, `walk` the 4-neighbour walking steps over the built columns (`MAX_STEP` 1, `MAX_WADE` 1) or null.
- New `mapengine/walk.py`: `distances(solid, water, start) -> int32 [z, x]` (-1 unreachable), shared by build and check.
- `check.check` adds codes `columns` (tiles differ from `columns.bin`), `distance` (walk differs from the manifest) and `detour` (walk over 2.0 times straight).
- Tests (`test-tools/standalone/test_mapengine_plan2.py`): `test_columns_match_tiles`, `test_routes_present_and_reachable`, `test_build_bytes_deterministic` (map.json, columns.bin, every tile), `test_detour_flagged` (a wall of hill), `test_small_fixture_current` (rebuilding the committed fixture design gives the committed `test-tools/resources/fixtures/engine-map/` bytes).
- `cli build` prints the build time.

## Task 2: `EngineMap` reader and check (pure Java, standalone checked)

- `src/main/java/gg/gravesandglory/EngineMap.java`, no Minecraft imports: `static EngineMap read(Path dir)`; `int solid(int wx,int wz)`, `int water(int wx,int wz)` (edge-clamped); `boolean wet(wx,wz)`; `double riverX(int wz)` (mean wet x of the row, or -100000); records `Point(name,x,z,y,radius)`, `Spawn(id,team,x,z,y)`, `Route(spawn,point,straight,walk)`; `static int[] distances(int[] solid,int[] water,int w,int d,int sx,int sz)`; `static List<String> check(EngineMap m,int[] solid,int[] water)` returning issue lines: `columns`, `moved`, `in_water`, `unreachable`, `distance`, `detour`.
- `test-tools/standalone/EngineMapCheck.java` on the committed fixture: Java distances equal the manifest's `walk`; a moved spawn, a raised column and a blocked route are each reported; the clean grid gives no issues. `EngineMap` joins the `names` list in `tools/verify.py`.

## Task 3: the mod builds it (`EngineMaps.java`)

- `EngineMaps.on(int map)`, `apply(int map)` (called at the end of `CampaignMaps.select`): Spec.POINTS from the manifest (id and name = design name, capture 8 s, elevation = ground), FACTIONS spawn x/z and barracks 3 blocks behind, STRUCTURES/TERRAIN/SCENERY empty, roads from the manifest; landmark and supply-house rows of the slot are dropped once at activation.
- Hooks, each a one-line `if(EngineMaps.on(...))`: `Arena.height/ground/river/riverX`, `CampaignMaps.spawnX`, `Structures.hash`, `Modes.hosts` (Domination only), the build loop in `GravesAndGlory.Battle.tick`.
- `EngineMaps.step(ServerWorld)`: per tick, up to a 40 ms budget, reads one tile (`NbtIo.readCompressed`, `NbtHelper.toBlockState`), clears each column above its top, writes changed blocks with flags 2|16; then margin, barriers, point pads and rings on the local ground, neutral flags; then reads the world back and runs `EngineMap.check`. Report: `engine-map-check.txt` in the server root plus log lines with the build time; `Diagnostics.report("engine-map-check", ok, why)`.
- The world reader skips air, barrier, fences and wool (flag parts) when finding the solid top.

## Task 4: headless harness on the sample

- `test-tools/java/gg/gravesandglory/EngineMapProbe.java` (main entrypoint, `-Dgravesandglory.engineMapProbe=<out>`): waits for the arena, copies the check result, and lets BotSimulation play.
- Run: own scratch server root, `runServer --offline -PbotSim=1 -PsimMaps=1 -PsimRoundSeconds=120 -PsimFast=true -PengineMap=<work/mapengine/sample>`, `--init-script test-tools/isolated-server.gradle`. Pass: check has no issues, the round completes with a points score in `botsim/results.csv`. Record engine build time and mod placement time.

## Task 5: Kort's walk

- `HOW-TO` lines in this plan's report: build with the engine, then `gradlew runClient -PengineMap=<dir>` and pick Ironhold in the lobby. Single player only (see the multiplayer question).
