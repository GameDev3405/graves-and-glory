# Phase 3: The Built World Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the moss courtyard with a hand-built war camp, replace every generic battlefield building except bases and bridges with an authored landmark whose cover and sight lines are tested, and remove every known road blocker so twenty knights cross all fifteen crossings of the five maps.

**Architecture:** Pieces are vanilla structure files in `src/main/resources/data/gravesandglory/structures/<group>/<name>.nbt`, authored as `.mcfunction` command scripts in `test-tools/` and exported by a harness-only `StructureStudio`. A new server class `Structures` reads them from the mod's resources (never from a world's `generated/` folder), places them rotated about the centre block, and queues one landmark per tick after the existing column build. Placement data lives in seven new sheets (`pieces`, `landmarks`, `landmarkLines`, `camp`, `campPieces`, `campStations`, `campLessons`). `CampaignMaps.select` turns each landmark row into a `kind=landmark` structure row, so terrain blending, dressing, decoration and the minimap keep working. `CampWorld` builds a procedural terrain island once per camp hash, then places camp pieces, NPCs, labels, troop displays, lesson boards and the war table. A harness suite `Phase3Checks` (`p3-*` ids) and a shared `Walkability` scan prove placement, routes and lines.

**Tech Stack:** Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Yarn 1.20.1+build.10, Loom 1.7.4, Java 17, Gradle 8.8 (offline), Python 3 (`tools/preflight.py`), ffmpeg (`C:\ffmpeg\ffmpeg-8.1-full_build\bin\ffmpeg`, contact sheets).

**Spec:** `docs/superpowers/specs/2026-10-17-built-world-design.md`; later scope sources: `docs/superpowers/specs/2026-10-22-supply-houses-design.md` section 12.1 (Task 8b, supply houses) and Kort's map vision of 2026-10-22 (`kort-map-vision-2026-10-22.md` in the SDD workspace). `docs/VISION-AND-ROADMAP.md` section 3 (pillars) and section 12 (workflow) bind every task; sections 9, 11 (Phase 3) and 13 set the scope. Prerequisites: Phase 1 merged (`Profile.campaignCleared`, `campaign:march-1`, `Spec.CAMPAIGNBATTLES`); Phase 2 Task 3 merged before Task 4 (`ArmyMobs`, the `houses` sheet, the recruit page with House tabs). If Phase 2 Task 3 is not merged when Task 3 ends, do Task 5 before Task 4.

## Global Constraints

- Minecraft 1.20.1 and Fabric only; no new dependencies; nothing is registered as worldgen.
- Never hand-edit `Spec.java`. Change `sheets/design.json`, then `python tools/preflight.py` (it also runs on every `compileJava`). Every row of a sheet has the same columns and every cell is filled; a column is `double` only if its first row's value has a decimal point; an empty sheet is an error. So a sheet is added in the task that has its first rows, and a row that names a `.nbt` is added only after that file is exported.
- Pillar 3 binds: authored detail lives only in structure files. Java builds terrain, the existing forests, point rings and flags, the flags and brazier state of the supply houses (Task 8b), war-table carpets and entities; no new `Arena.box` decoration.
- Pillar 5 binds: nothing is done until a running game shows it by log line and screenshot. The tester opens every named screenshot and states in one sentence what it shows; a screenshot that does not show the claim fails the step even if its check passed.
- Section 12 binds, updated to Kort's 2026-10-17 model rule (Opus 5.5 and Sonnet 5.5 only, both at max effort; no Haiku): piece scripts and layout judgement go to Opus 5.5 max, Java to Sonnet 5.5 max, runs, screenshots and contact sheets to a Sonnet 5.5 max tester. Every build task (2 to 8) ends with its named screenshots and the `routes` suite.
- The harness lives only in `test-tools/`. Add `Phase3Checks`, `StructureStudio`, `Walkability`, `gravesandglory_studio` and `p3_marker` to the `forbidden` tuple in `tools/release_config.py`.
- Working copy is CRLF (`core.autocrlf=true`); edit dense lines in place and never reformat a file. `.gitattributes` gains `*.nbt binary` and `*.mcfunction text eol=lf` (Task 1).
- Structure files: DataVersion 3465; odd width and depth, each at most 48; height at most 32; ids `<group>/<name>` in `[a-z0-9_/]`. A piece whose footprint changes gets a new name, never a silent resize.
- Scripts: line 1 is `# size <sx> <sy> <sz>`; every other line is a `#` comment or a `fill`/`setblock` with `~` coordinates from the minimum corner (local 0,0,0); each `fill` covers at most 32,768 blocks. Cells no line sets stay structure void and keep the ground; write `air` where a piece must clear. Local y 0 is the ground layer (world y = the row's elevation or `y`); "feet N" means a player stands with feet at local y N, on a solid block at N-1; "eye" points sit 1.6 above the feet.
- No launch while Kort plays: before every launch run `powershell -NoProfile -Command "Get-Process java,javaw -ErrorAction SilentlyContinue | Select-Object Id,StartTime,MainWindowTitle"`; if a window title starts with `Minecraft` and the tester did not start it, stop and report. Testers may launch isolated clients when he is not playing.
- Isolated runs only, always `--offline`. Branch `phase-3-built-world` from `master` after Phase 1 merges; commit locally per task; never push, publish or upload; `mod_version` is the controller's call.
- Pixel UI icons are not in this plan. Hook: tent and display labels stay text; when the separate pixel UI pass lands, `CampDisplays` draws `gravesandglory:textures/gui/house/<id>.png` beside each label, with no world change.
- Cut ideas go to `docs/PARKED.md` (append, never delete).
- Walkthrough gate (Kort, 2026-10-18): every build task (2 to 8) ends with a first-person walkthrough by a Sonnet 5.5 max tester who steers a player through the whole map or camp piece, screenshots every landmark, approach and sight line from the walker's eye, and grades the result 1 to 10 against `work/ui-concepts/GRADING-RUBRIC.md` adapted to world pieces (readability, silhouette, detail, purpose, no clipping or floating blocks, no visible seams). Below 9 is a fix round, not an approval. Nothing less than a 9 ships. Do not rush these tasks; give them more help (a second pass, a second tester) rather than less time.

## Review Focus

1. A map switch while landmarks are still queued: the queue is cleared, no piece of the old map lands on the new one, and `ready` waits for the new map's queue. (Test: `p3-lm-switch-mid-queue`, Task 6.)
2. Rotation: blockstates (stairs, banners, doors) and footprints turn with the piece and the centre block stays put. (Tests: `p3-place-rotated`, Task 1; `p3-lm-<id>-fits`.)
3. A world made by an older build rebuilds its camp (old blocks and entities gone) and its map on first load, and a second start rebuilds nothing. (Tests: `p3-camp-old-gone`, `p3-camp-rebuild-idempotent`, `p3-lm-cache`.)
4. No literal camp coordinate survives outside `CampWorld` and `CampaignMaps`; a player who leaves the island or reaches the arena returns to spawn; both pads still ready a match. (Tests: `p3-camp-leash`, `p3-camp-pads`, the grep in Task 3.)
5. Display mobs never move, burn, despawn, take damage, get cleaned up as stale units or count as troops. (Tests: `p3-houses-shaded`, `p3-houses-survive`.)
6. No landmark blocks a through-road, a deck or a ring it does not enclose. (Tests: `p3-lm-<id>-roads`, `-ring`, `p3-walk-map<N>`, routes 20/20.)

## Constraints from the current code (and the smallest change chosen)

- `CampaignMaps.select(map)` rebuilds `Spec.STRUCTURES`, `POINTS`, `TERRAIN` and the road list for one map, and `Arena.structure` switches on `kind` with no `default`. Landmarks therefore join as `StructuresRow(id,"landmark",x,z,w,d,h,palette,elevation)` rows (w and d swapped at 90 and 270) appended in `select` after the row named by `replaces` is removed. `Arena.structure` builds nothing for them, while `MapDressing.onStructure`, the decoration margin and `Arena.mapColor` see them unchanged.
- The build tick ends with `Arena.finish(world);campaign.built();ready=true;` once `buildColumn>=total`. The landmark queue goes between `finish` and `built`, one piece per tick, so `ready` and `arena-ready` wait for it.
- `Campaign.cached()`, `built()` and the constructor compare the marker `{"layout":"campaign-2","map":N}`. Task 2 changes the layout to `campaign-3`; Task 6 adds `"landmarks":"<hex>"`.
- Camp coordinates are literals in `Campaign.toLobby` (`0,66,LOBBY_Z+20`), both `Campaign.tick` leashes (`abs(x)>40`, `abs(z-LOBBY_Z)>30`, `y<63`), the build-tick leash in `GravesAndGlory` (`p.getZ()>-275||p.getY()<63`), the `CampWorld.station` boxes, `CampaignMaps.onPad` (`-11` and `8`) and the harness (`CampaignChecks` `-9.5,65,-322` twice; `CampRedesignChecks` `22,66,LOBBY_Z-7` and `-9.5,65,PAD_Z`). Task 3 replaces each with `CampWorld.inCamp/spawn/where/station` and `CampaignMaps.padCenter`. The PARTY lectern test `(3,66,LOBBY_Z+18)` stays and becomes unreachable.
- The leashes also pull a camera player back from the arena, so Task 1 adds `Campaign.leash` (default true) for harness camera work.
- The CAMP packet carries one station string (`openCamp(p,station)`, then `CampScreen.open(station)`); tents send `recruit:<house>` and `open` splits on the first `:`.
- Camp NPCs and labels carry `CampWorld.TAG` (`gravesandglory-camp-v3`), `camp-generation:<g>` and `camp:<station>`; ENTITY_LOAD discards stale generations. Task 3 moves `TAG` to `gravesandglory-camp-v4` and adds the v3 tag to the discard list.
- Command tags never reach the client, so client code finds display slots from the sheets (`CampWorld.slot` is pure), never from entity tags.
- `Battle.clearOldUnits` discards entities tagged exactly `gravesandglory` or `bannerbound`; camp entities never carry those tags.
- `MapRouteChecks` defaults to maps 0 to 3 because of the Kingsfield HUD crash fixed in 6ecbfb0; Task 2 makes the default 0 to 4. `MapTour.walkable` becomes `Walkability.scan`, shared by MapTour and Phase3Checks.
- Points: maps 0 to 3 have A at `(rx0-29,z0)`, B at `(rx1,0)`, C at `(rx2+29,z2)` with `rx=(int)riverX(z)`; Kingsfield has A, C, E on its crossings and B `(-130,100)`, D `(130,-100)` at elevation 74. Rings have radius 17; a landmark keeps out of radius+2 unless it encloses the point.

## Test runs used by every task

**Preflight:** `cd /c/Users/kortt/Melty/bannerbound && python tools/preflight.py` prints `Preflight clean`.

**Compile both source sets:**
```bash
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle)
"$G" compileJava compileLocalTestTools --offline
```

**In-game suites `<list>`** (one launch; Phase3Checks runs the comma list in order, then stops the client):
```bash
# Kort-not-playing check from Global Constraints first.
cd /c/Users/kortt/Melty/bannerbound
export GRAVES_ROADMAP_TEST_ROOT="$(cygpath -m "$PWD")/work/p3/<first-suite>-$(date +%H%M%S)"
mkdir -p "$GRAVES_ROADMAP_TEST_ROOT"/{run,evidence,runtime}
printf 'pauseOnLostFocus:false
onboardAccessibility:false
' > "$GRAVES_ROADMAP_TEST_ROOT/run/options.txt"   # a fresh root otherwise idles on the first-run accessibility screen (Phase 3 Task 1 finding; Phase3Checks also closes it)
GRAVES_PHASE3_CHECKS=<list> "$G" runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir $GRAVES_ROADMAP_TEST_ROOT/run"
grep -E "^(pass|fail) " "$GRAVES_ROADMAP_TEST_ROOT/evidence/local-checks.txt"; grep "\[p3\]" "$GRAVES_ROADMAP_TEST_ROOT/run/logs/latest.log"; ls "$GRAVES_ROADMAP_TEST_ROOT/run/screenshots"
```
Optional variables: `GRAVES_PHASE3_MAPS` limits the `landmarks` suite (default `0,1,2,3,4`; `routes` always covers all five maps); `GRAVES_STUDIO_PIECES` (comma list of `<group>/<name>`, default every script); `GRAVES_STUDIO_MODE` (`verify` by default, or `export`); `GRAVES_STUDIO_OUT` (default `$GRAVES_ROADMAP_TEST_ROOT/studio-out`). `work/` is git-ignored. Pass means: every `p3-*` line is `pass`, `pass p3-<suite>-suite` is present for each suite in the list, and every named screenshot (`p3-<name>.png`) exists and shows its claim.

**Studio export of new or changed pieces** (same launch lines, list `studio`): `GRAVES_PHASE3_CHECKS=studio GRAVES_STUDIO_MODE=export GRAVES_STUDIO_PIECES=<ids>`. Expected `pass p3-export-<group>-<name>` and `p3-studio-<group>-<name>.png` per piece. Then copy each file into place and verify:
```bash
mkdir -p src/main/resources/data/gravesandglory/structures/<group>
cp "$GRAVES_ROADMAP_TEST_ROOT/studio-out/<group>/<name>.nbt" src/main/resources/data/gravesandglory/structures/<group>/
```
A later run with `GRAVES_STUDIO_MODE` unset passes `p3-fresh-<group>-<name>` for every script.

**Route check** (the end of every build task): suite `routes`. Expected `pass p3-walk-map<N>` and `pass p3-routes-map<N>` for N = 0..4, the route harness's own lines (`pass map-<n>-setup` five times, `pass map-<n>-bridge-<k>` fifteen times, `pass all-map-navigation`), and `pass p3-routes-suite`.

---

### Task 1: Structure pipeline, placer, test marker and the Phase 3 harness

**Files:**
- Create: `src/main/java/gg/gravesandglory/Structures.java`
- Create: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (client entrypoint)
- Create: `test-tools/java/gg/gravesandglory/StructureStudio.java` (main entrypoint; registers `/studio`)
- Create: `test-tools/resources/data/gravesandglory_studio/functions/test/p3_marker.mcfunction`
- Create: `test-tools/resources/data/gravesandglory/structures/test/p3_marker.nbt` (exported in Step 8; the marker lives only in the harness jar)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (field `boolean leash=true`; both leashes run only when it is true)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (the build-tick leash also checks `campaign.leash`)
- Modify: `test-tools/isolated-checks.gradle`: `systemProperty 'gravesandglory.phase3Checks', System.getenv('GRAVES_PHASE3_CHECKS') ?: ''`, then the same for `phase3Maps` (`GRAVES_PHASE3_MAPS`, default `'0,1,2,3,4'`), `studioPieces` (`GRAVES_STUDIO_PIECES`, `''`), `studioMode` (`GRAVES_STUDIO_MODE`, `'verify'`) and `studioOut` (`GRAVES_STUDIO_OUT`, `testRoot + '/studio-out'`)
- Modify: `test-tools/resources/fabric.mod.json` (client `gg.gravesandglory.Phase3Checks`; main `gg.gravesandglory.StructureStudio`)
- Modify: `test-tools/java/gg/gravesandglory/CampaignChecks.java` (also returns early when `gravesandglory.phase3Checks` is non-empty)
- Modify: `tools/release_config.py` (the five forbidden names), `tools/preflight.py` (structure-file rules), `.gitattributes`

**Interfaces:**
- Produces `Structures` (server package, no client imports):
  - `static final String NS="gravesandglory"`; `record Placed(String id,int blocks,long nanos,BlockBox box,boolean missing)`; `static final List<Placed> LOG` (the last 256 placements).
  - `static Optional<StructureTemplate> template(ServerWorld w,String id)`: reads `data/gravesandglory/structures/<id>.nbt` through `w.getServer().getResourceManager()` and `NbtIo.readCompressed`, builds it with `w.getStructureTemplateManager().createTemplate(nbt)`, and caches it by id. The cache clears on `ServerLifecycleEvents.END_DATA_PACK_RELOAD`. A world's `generated/` folder is never read.
  - `static BlockRotation rotation(int degrees)`: 0 NONE, 90 CLOCKWISE_90, 180 CLOCKWISE_180, 270 COUNTERCLOCKWISE_90; anything else throws `IllegalArgumentException`.
  - Pure (the client calls them): `toWorld(Vec3i size,int cx,int y,int cz,int rotation,int lx,int ly,int lz) -> BlockPos` with pivot `P=((sx-1)/2,0,(sz-1)/2)` and result `(cx,y,cz)+R(l-P)`; `toWorld(...,double lx,double ly,double lz) -> Vec3d`, the same about the centre block's centre: `(cx+.5,y+ly,cz+.5)+R((lx,lz)-(P.x+.5,P.z+.5))`; `footprint(Vec3i size,int cx,int y,int cz,int rotation) -> BlockBox`.
  - `static Placed place(ServerWorld w,String id,int cx,int y,int cz,int rotation)`: origin `(cx,y,cz)-P`; `StructurePlacementData` with `setRotation(rotation(r))`, `setMirror(BlockMirror.NONE)`, `setIgnoreEntities(true)`, `setUpdateNeighbors(false)`, `addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)`, `setPosition(P)`; then `template.place(w,origin,origin,data,w.getRandom(),Block.NOTIFY_LISTENERS)`. A missing template places nothing, logs `[Graves and Glory] structure missing: <id>` and returns `missing=true`; nothing throws.
- Produces `StructureStudio` (harness): `ORIGIN=(1000,64,1000)`; `scripts()` lists every `gravesandglory_studio:<group>/<name>` function resource; `build(MinecraftServer s,String id) -> Vec3i` reads the size header, force-loads the chunks, sets the box grown by 2 to air and the box itself to `structure_void` in Java, then runs each command line with `s.getCommandSource().withPosition(Vec3d.of(ORIGIN)).withLevel(4).withSilent()` (a failing line fails the piece, naming its line number); `save(s,id,size) -> NbtCompound` is `saveFromWorld(world,ORIGIN,size,false,Blocks.STRUCTURE_VOID)` then `writeNbt`; `export(s,id) -> Path` writes `<studioOut>/<id>.nbt` (gzip); `fresh(s,id) -> boolean` rebuilds, saves and compares with the resource's NBT by `equals`; `dump(StructureTemplate t) -> String` writes a size header and one `setblock` per non-void block (runs along x merged into `fill`, block entity NBT kept). Commands: `/studio build|save|dump <id>` and `/studio place <id> <rotation>` at the player.
- Produces the `Phase3Checks` scaffold (the `Phase1Checks` pattern): `report(String id,boolean pass,String why)` (MeltyTest line plus `[p3] id=pass why`), `shot(MinecraftClient c,String name)` saving `p3-<name>.png`, `view(MinecraftClient c,String name,double x,double y,double z,double tx,double ty,double tz)` (leash off, spectator teleport with yaw and pitch from `MapTour.look`, 40 ticks for chunks, `shot`, leash restored), `server(c,f)`, `interface Suite`, a `SUITES` map, and `finish(c,suite)` reporting `p3-<suite>-suite` and stopping the client after the last suite. Start: 40 ticks after the front menu, `startCamp(c,false)`.

- [ ] **Step 1: Add to `.gitattributes`** the lines `*.nbt binary` and `*.mcfunction text eol=lf`.
- [ ] **Step 2: Write the marker script** `test/p3_marker.mcfunction` exactly:
```
# size 5 5 5
fill ~0 ~0 ~0 ~4 ~0 ~4 minecraft:polished_andesite
fill ~2 ~1 ~2 ~2 ~3 ~2 minecraft:stone_bricks
setblock ~2 ~4 ~2 minecraft:lantern
setblock ~2 ~1 ~4 minecraft:oak_stairs[facing=south]
setblock ~2 ~2 ~1 minecraft:white_wall_banner[facing=north]{Patterns:[{Pattern:"sc",Color:4},{Pattern:"bo",Color:11}]}
```
- [ ] **Step 3: Write suite `pipeline`** (it cannot compile until Step 5). Fixture: chunks force-loaded; place `test/p3_marker` at `(1000,64,1100)` rotation 0 and at `(1010,64,1100)` rotation 90. Checks:
  - `p3-template-loads`: `template(w,"test/p3_marker")` is present with size (5,5,5).
  - `p3-place-exact`: polished_andesite over (998..1002,64,1098..1102); stone_bricks at (1000,65..67,1100); lantern at (1000,68,1100); oak_stairs facing south at (1000,65,1102); white_wall_banner facing north at (1000,66,1099); `Placed.box` is (998,64,1098) to (1002,68,1102); `blocks==31`.
  - `p3-place-rotated`: pillar at (1010,65..67,1100); stairs facing west at (1008,65,1100); banner facing east at (1011,66,1100).
  - `p3-void-keeps-ground`: a hay_block set at (1001,65,1101) before placement (local 3,1,3 is void) is still there after.
  - `p3-block-entity`: the banner at (1000,66,1099) holds `sc` yellow then `bo` blue.
  - `p3-no-entities`: no entity inside either marker's box after placement.
  - `p3-missing-template`: `place(w,"test/does_not_exist",1000,64,1120,0)` returns `missing=true`, places nothing and throws nothing.
  - `p3-no-generated-override`: write a 1x1x1 gold_block template to `<world>/generated/gravesandglory/structures/test/p3_marker.nbt` and clear the cache; `template` still has size (5,5,5). Delete the file after.
  - Screenshot `pipeline-marker` from (1005,70,1108) looking at (1005,65,1100).
- [ ] **Step 4: Compile; expect `cannot find symbol` for `Structures`.**
- [ ] **Step 5: Implement `Structures`** as in Interfaces, and the `Campaign.leash` guard.
- [ ] **Step 6: Implement `StructureStudio` and suite `studio`.** Export mode: `p3-export-<group>-<name>` per requested script (file written, size equals the header) and screenshot `studio-<group>-<name>` from `ORIGIN+(sx/2+0.7d, sy+0.4d, sz/2+0.7d)` looking at `ORIGIN+(sx/2, sy/3, sz/2)`, `d=max(sx,sz)+8`. Verify mode: `p3-fresh-<group>-<name>` per script. Both modes: `p3-studio-roundtrip` (dump the marker's template, run the dump as a script, save, `equals` the resource NBT).
- [ ] **Step 7: Wire the harness files** (`isolated-checks.gradle`, `fabric.mod.json`, the `CampaignChecks` early return, `release_config.py`). Compile clean.
- [ ] **Step 8: Export the marker** (`GRAVES_STUDIO_PIECES=test/p3_marker`) and copy `studio-out/test/p3_marker.nbt` to `test-tools/resources/data/gravesandglory/structures/test/`.
- [ ] **Step 9: Add the structure-file rules to `tools/preflight.py`:** a small NBT reader (gzip, walk the root compound, skip unread tags) checks every `.nbt` under `src/main/resources/data/gravesandglory/structures/` and `test-tools/resources/data/gravesandglory/structures/`: DataVersion is 3465, and a script with the same id exists under `test-tools/resources/data/gravesandglory_studio/functions/`. Expect `Preflight clean`; rename the script for a moment and expect an error naming `test/p3_marker`.
- [ ] **Step 10: Run suites `pipeline,studio`.** Expected `pass` for the eight pipeline ids, `p3-fresh-test-p3_marker`, `p3-studio-roundtrip`, `p3-pipeline-suite` and `p3-studio-suite`. `p3-pipeline-marker.png` shows two marker pillars; the right one has its stairs to the west and its banner to the east.
- [ ] **Step 11: Commit.**
```bash
git add -A .gitattributes src/main/java/gg/gravesandglory test-tools tools
git commit -m "Phase 3: structure placer, studio export pipeline and the p3 harness"
```

---

### Task 2: Road blockers, visible and hidden, and the walk scan

Done before any landmark, so every later task is measured against a zero baseline.

**Files:**
- Create: `test-tools/java/gg/gravesandglory/Walkability.java`
- Modify: `src/main/java/gg/gravesandglory/Arena.java` (`structure` cases `bridge` and `breach`; new `clearDecks`; `finish`)
- Modify: `src/main/java/gg/gravesandglory/CampaignMaps.java` (`select`: hamlet, market, garrison, signal tower, Kingsfield customs and warehouse)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (layout `campaign-2` becomes `campaign-3` in the constructor, `cached` and `built`)
- Modify: `test-tools/java/gg/gravesandglory/MapTour.java` (`walkable` calls `Walkability.scan`; its log line gains ` through=<n> base=<n>`)
- Modify: `test-tools/java/gg/gravesandglory/MapRouteChecks.java` (default maps, class comment, per-map counter)
- Modify: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (suite `routes`)

**Interfaces:**
- Produces (harness) `Walkability.record Scan(int cells,int road,int through,int building,int base,int point,String detail)`, `static Scan scan(GravesAndGlory.Battle b)` and `scan(b, BlockBox area)` for one footprint. It visits every road, deck and capture-ring cell in the arena; a cell is blocked when a block with a collision shape sits at `Arena.ground+1` or `+2`. A blocked cell counts as `road` on a deck; as `base` inside a footprint of kind `base` or `barracks` or an id ending `_stores`; as `building` inside any other footprint, and also as `through` when a road of width 4 or more covers it and neither endpoint of that road is inside that footprint; as `point` inside a ring; otherwise as `road`. `detail` lists `kind x z block` per hit.
- Produces `MapRouteChecks.crossed` (`Map<Integer,Integer>`, 20/20 crossings per map).
- Bridge rows run along x: the deck is the planks only, rows `z..z+depth-1` by columns `x..x+width-1` (`x=s.x()-width/2`, `z=s.z()-depth/2`), top block at `ground=elevation`. `Arena.ground`'s bridge test also counts row `z+depth` and column `x+width`, so `Walkability` and `clearDecks` use the plank test; the railings go on rows `z-1` and `z+depth`. `wet(px,row)` means `river(px-1,row)||river(px,row)||river(px+1,row)`.
- Produces `Arena.clearDecks(ServerWorld w)`: for every `bridge` row, every deck cell from `elevation+1` to `elevation+12` becomes air.

- [ ] **Step 1: Write `Walkability` and suite `routes`.** For maps 0 to 4: switch and wait for `ready`; `scan`; report `p3-walk-map<N>` (pass when `road==0 && through==0`; why carries all six counts); write `detail` to `screenshots/p3-walk-map<N>-blocked.txt`; for each crossing `k`, `view` `deck-map<N>-bridge-<k>` from `(s.x()-8, 70, s.z()+s.depth()/2+10)` looking at `(s.x(),65,s.z())`. Then build a `MapRouteChecks` over maps 0 to 4, tick it until `done`, and report `p3-routes-map<N>` (pass when `crossed.get(N)==3`).
- [ ] **Step 2: Point `MapTour.walkable` at `Walkability.scan`.** In `MapRouteChecks` the default becomes `{0,1,2,3,4}`, the comment says all five maps (the Kingsfield HUD crash was fixed in 6ecbfb0), and each passing crossing runs `crossed.merge(map,1,Integer::sum)`.
- [ ] **Step 3: Baseline run of `routes` before any fix.** Expected `fail p3-walk-map<N>`; the static estimate of blocked road and deck cells is map 0: 26, map 1: 54, map 2: 26, map 3: 61, map 4: 16. Record the actual counts for the commit message.
- [ ] **Step 4: Wood railings off the deck.** In `case "bridge"`, palette `wood`, keep the plank deck and replace the post loop: on rows `z-1` and `z+depth`, for each `dx` in `0..width-1` at `px=x+dx`, place a spruce_log post from `ground-3` to `ground` when `dx%4==0`, otherwise `spruce_slab[type=top]` at `ground`; where `wet(px,row)`, a `spruce_fence` at `ground+1` with `east`/`west` true toward wet neighbours.
- [ ] **Step 5: Stone walls off the deck.** For palette `stone`, replace the wall and lantern loop: on rows `z-1` and `z+depth`, where `wet(px,row)`, stone_bricks at `ground`, `stone_brick_wall` at `ground+1` (`east`/`west` `low` toward wet neighbours, `up` true where `a%8==0`) and a lantern at `ground+2` where `a%8==0`. The abutments stay.
- [ ] **Step 6: Add `Arena.clearDecks`** and call it in `finish` right after the structures loop.
- [ ] **Step 7: Widen the Ironhold breach:** in `case "breach"`, `Math.abs(dz-s.depth()/2)>3` becomes `>(s.id().equals("breach")?5:3)`, an 11-wide gap; the curtains keep 7.
- [ ] **Step 8: Delete the Hollowwood log pile line** in `finish`: `if(CampaignMaps.selected==3)for(int row=0;row<3;row++)box(w,-90+row*3,65,-164,2,2,8,Blocks.SPRUCE_LOG);`.
- [ ] **Step 9: Move the buildings off the approach roads** in `select`: the hamlet's `hz=z-4+(k/2)*31` becomes `hz=z+12+(k/2)*15` (house lanes follow, since they start at `hx,hz`); the market `rx-65,z+25` becomes `rx-50,z+33`; the Ironhold garrison `65,135` becomes `65,147`; the Frostmarch signal_tower `57,166` becomes `62,170`; on Kingsfield only (`selected==4`) customs moves to `-45,47` and the warehouse to `45,-47`.
- [ ] **Step 10: Bump the layout marker** to `campaign-3` in all three places, so every existing world rebuilds its map once.
- [ ] **Step 11: Compile; run `routes`.** Expected the route check in full (road 0 and through 0 on every map). Deck screenshots: wood decks with a fence railing beside the deck over the water only, stone decks with walls beside them, no post or wall on any deck row; `p3-deck-map1-bridge-1` shows no ruin wall on the Ironhold deck.
- [ ] **Step 12: Commit** `"Phase 3: road blockers off decks and roads, walk scan, routes on all five maps"`, with the before and after counts in the body.

---

### Task 3: Camp base (terrain island, ridge, command tent and war table, War Gates and pads, archive, parade)

The War Gates belong here, not in a later task: retiring the courtyard removes today's pads, so the new pads arrive in the same commit.

**Files:**
- Modify: `sheets/design.json`: new sheets `pieces`, `camp`, `campPieces`, `campStations` (rows below)
- Modify: `tools/preflight.py` (rules for the four sheets)
- Modify: `src/main/java/gg/gravesandglory/CampWorld.java` (rewrite `build`; delete the courtyard loops, `pavilion` and `dress`; add `inCamp`, `spawn`, `where`, `station`, `warTable`, `tableCell`, `text`; `TAG="gravesandglory-camp-v4"`)
- Modify: `src/main/java/gg/gravesandglory/Arena.java` (extract the spruce branch of the scenery loop into `tree(ServerWorld w,int x,int y,int z,int width,int height,boolean spruce)`, behaviour unchanged)
- Modify: `src/main/java/gg/gravesandglory/CampaignMaps.java` (`LOBBY_Z=Spec.CAMP[0].originZ()`, `PAD_Z=LOBBY_Z+Spec.CAMP[0].padDz()`, `onPad` from `pad0Dx`/`pad1Dx`, new `padCenter`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`toLobby` through `CampWorld.spawn`; both leashes through `inCamp`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (build-tick leash through `inCamp`; ENTITY_LOAD also discards `gravesandglory-camp-v3`; `CampWorld.warTable(world)` right after `campaign.built()`)
- Create: `test-tools/resources/data/gravesandglory_studio/functions/camp/{ridge_stair,command_tent,war_gates,archive,parade}.mcfunction` and their exports under `src/main/resources/data/gravesandglory/structures/camp/`
- Modify: `test-tools/java/gg/gravesandglory/CampaignChecks.java` (both `-9.5,65,-322` become `CampaignMaps.padCenter(0)`)
- Modify: `test-tools/java/gg/gravesandglory/CampRedesignChecks.java` (`four-interactive-stewards` becomes `camp-npcs-match-sheet`: villagers with `TAG` equal the `campStations` rows whose `npc` is not `-`; the recruit relocation uses `CampWorld.where("recruit")`; the pad relocation uses `padCenter(0)`)
- Modify: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (suite `camp`)

**Sheets (exact rows).** `pieces` (columns `id, width, depth, height, palette, purpose`):

| id | width | depth | height | palette | purpose |
|---|---|---|---|---|---|
| camp/ridge_stair | 13 | 13 | 14 | stone | stair from the bowl to the ridge |
| camp/command_tent | 25 | 15 | 14 | wood | commander's tent and war table |
| camp/war_gates | 37 | 11 | 16 | wood | gate arch and the two ready pads |
| camp/archive | 23 | 31 | 14 | stone | Archivist and the Founders' Stone |
| camp/parade | 41 | 33 | 6 | stone | parade ground, standard, spawn plinth |

`camp` (one row): `id camp-4, originX 0, originZ -310, floorY 64, minDx -60, maxDx 60, minDz -72, maxDz 35, minY 62, spawnDx 0.5, spawnDz 18.5, spawnY 65.0, spawnYaw 180.0, spawnPitch 8.0, padDz 26, pad0Dx -11, pad1Dx 8, islandMinDx -96, islandMaxDx 96, islandMinDz -108, islandMaxDz 40, islandBottomY 44, plateauY 76, tableDx -7, tableDz -65, tableY 78`.

`campPieces` (columns `id, template, dx, dz, y, rotation, flatten, station, slotX, slotY, slotZ`; `y` is the piece floor; slots of `-1` mean none; `flatten=false` keeps the ridge bank under the stairs):

| id | template | dx | dz | y | rotation | flatten | station | slotX | slotY | slotZ |
|---|---|---|---|---|---|---|---|---|---|---|
| ridge-west | camp/ridge_stair | -18 | -44 | 64 | 0 | false | - | -1 | -1 | -1 |
| ridge-east | camp/ridge_stair | 18 | -44 | 64 | 0 | false | - | -1 | -1 | -1 |
| command | camp/command_tent | 0 | -60 | 76 | 0 | true | army | -1 | -1 | -1 |
| parade | camp/parade | 0 | 8 | 64 | 0 | true | - | -1 | -1 | -1 |
| archive | camp/archive | 46 | 6 | 64 | 0 | true | - | -1 | -1 | -1 |
| gates | camp/war_gates | 0 | 30 | 64 | 0 | true | - | -1 | -1 | -1 |

`campStations` (columns `id, page, npc, x, y, z, yaw, halfX, halfZ, label`; `x` and `z` are dx and dz; the label floats 3.7 above `y`; in labels `{key:<id>}` becomes `Text.keybind("key.gravesandglory.<id>")` and `{key:use}` becomes `Text.keybind("key.use")`):

| id | page | npc | x | y | z | yaw | halfX | halfZ | label |
|---|---|---|---|---|---|---|---|---|---|
| quartermaster | army | armorer | 10.5 | 77.0 | -57.5 | 90.0 | 1 | 1 | Quartermaster / Army & shop |
| archivist | contracts | librarian | 44.5 | 65.0 | 0.5 | 90.0 | 1 | 1 | Archivist / Daily contracts |
| founders | chapter | - | 50.5 | 65.0 | 14.5 | 0.0 | 2 | 2 | FOUNDERS CHAPTER / 30 tiers |
| steward | friends | cartographer | 4.5 | 65.0 | 22.5 | 180.0 | 1 | 1 | Steward / Parties & rooms |
| welcome | home | - | 0.5 | 65.0 | 14.5 | 0.0 | 1 | 1 | WELCOME TO BANNER CAMP / {key:menu} opens your camp guide |
| gate-west | match | - | -9.0 | 65.0 | 28.0 | 0.0 | 2 | 2 | WEST GATE / Stand here to ready |
| gate-east | match | - | 10.0 | 65.0 | 28.0 | 0.0 | 2 | 2 | EAST GATE / Stand here to ready |
| war-gates | match | - | 0.5 | 69.0 | 25.5 | 0.0 | 3 | 1 | WAR GATES / Local matches / Bots fill empty seats |
| recruiter | recruit | cleric | 0.5 | 65.0 | -36.5 | 0.0 | 1 | 1 | Recruiter / New troops |

The `recruiter` row is interim; Task 4 deletes it. NPC yaw: 0 faces south, 90 west, 180 north, -90 east.

**Interfaces:**
- Produces in `CampWorld`: `record Spot(double x,double y,double z,float yaw,float pitch)`; `static Spot spawn(Campaign.Profile p)` (the `camp` spawn; Task 5 adds the first arrival); `static boolean inCamp(double x,double y,double z)` (inside the island box, `originX+islandMinDx <= x < originX+islandMaxDx+1` and the same for z, and `y >= minY`); `static Vec3d where(String page)` (the first `campStations` row with that page, else the centre of the first `campPieces` row whose station starts with it); `static String station(BlockPos p)` (a `campStations` box `|x-sx|<=halfX`, `|z-sz|<=halfZ`, `y-2 <= p.y <= y+4`, else a `campPieces` footprint whose station is not `-`, from floor-1 to floor+12, else null); `static void warTable(ServerWorld w)` and `static BlockState tableCell(int i,int j)`; `static Text text(String label)` (gold, `{key:...}` tokens as keybind parts).
- Produces `CampaignMaps.padCenter(int team) -> Vec3d`: `(-9.0,65,-282.0)` and `(10.0,65,-282.0)`.
- Consumes `Structures.place`, `Arena.tree`, `Campaign.leash`.

**Camp build order (`CampWorld.build`, at world start):**
1. Terrain, only when `<world>/gravesandglory-camp.json` lacks the current hash (CRC32 of the `camp` and `campPieces` rows, the `camp/*` rows of `pieces`, and the string `terrain-1`). For each column of the island box, height `h` is `floorY`, plus `plateauY-floorY` where `dz<=-51` or `(plateauY-floorY)*(-38-dz)/13` where `-51<dz<-38`; plus `min(18,0.7*(-72-dz))` where `dz<-72`; plus `min(20,0.8*(|dx|-60))` where `|dx|>60`; minus `min(2,(dz-35)*0.5)` where `dz>35`; plus `2*sin(dx*.21)*cos(dz*.17)` outside the bowl (`minDx..maxDx`, `minDz..maxDz`); rounded. Then the `campPieces` rows: a cell inside a row's footprint takes that row's `y` when `flatten=true` and keeps `h` when `flatten=false`; a cell inside no footprint but inside the footprint grown by 2 of one or more `flatten=true` rows takes the highest of their `y` (so the command tent's apron stays on the plateau beside the graveyard notch). Fill stone from `islandBottomY` to `h-2`, dirt at `h-1`, grass at `h` (stone where `h>=80`), and air from `h+1` up to the `WORLD_SURFACE` heightmap top. Spruce trees by `Arena.tree` outside the bowl where `64<h<88` and `floorMod(dx*734287+dz*912931,23)==0`, width 5, height `8+floorMod(dx+dz,4)`. Log `[Graves and Glory] camp terrain <ms> ms` (or `terrain skipped`).
2. Pieces: `Structures.place(w,template,originX+dx,y,originZ+dz,rotation)` for every `campPieces` row, at every start. Log `[Graves and Glory] camp pieces <ms> ms`.
3. Entities: discard everything tagged `TAG`, `gravesandglory-camp-v3`, `gravesandglory-lobby` or `bannerbound-lobby`; spawn each station NPC (villager with the row's profession at level 5, NoAI, invulnerable, silent, persistent, the label's first part as a hidden custom name, the row's yaw, tags `TAG`, `camp-generation:<g>`, `camp:<page>`) and each label (the existing TextDisplay style, through `text`).
4. `warTable(w)`; write the hash.

**War table rule.** Cell `(i,j)`, `i` 0..14 and `j` 0..8, sits at `(originX+tableDx+i, tableY, originZ+tableDz+j)` and covers arena `x` from `-350+i*700/15` to `-350+(i+1)*700/15` and `z` from `-250+j*500/9` to `-250+(j+1)*500/9`, sampled on a 7x7 grid at cell fractions `(a+0.5)/7`. First match wins: a point centre inside the cell gives `candle[lit=true]`; any bridge sample `light_gray_carpet`; any river sample `cyan_carpet`; 7 or more road samples `brown_carpet`; 25 or more samples with `Arena.height>79` `gray_carpet`; otherwise `white_carpet` on Frostmarch and `green_carpet` elsewhere. `warTable` runs at camp build and after every map build.

**Piece briefs** (local coordinates from the minimum corner; the author's call beyond these):
- `camp/ridge_stair` 13x13x14, set into the ridge bank (`flatten=false`): a 5-wide `stone_brick_stairs[facing=north]` flight at local x 4..8, rising one block per block from feet 1 at local z 12 to feet 13 at local z 0 (world feet 65 to 77); stone-brick retaining walls at x 3 and 9 that meet the bank; air above the flight; lanterns on posts every 4 steps.
- `camp/command_tent` 25x15x14 on the plateau (floor y 76): south front open from the floor to local y 8; black and gray wool canopy on dark_oak_log poles; spruce plank floor; the war table at local x 4..20, z 1..11 with its dark oak top at local y 1 and the carpet cells (local x 5..19, z 2..10, y 2) left void; the standard pole, dark_oak_fence at local (16,1..11,14) with a `black_banner` `{bo 4, mc 4}` on top, the anchor for in-world banner cosmetics; the Quartermaster stands east of the table at local (22.5,1,9.5); lanterns on chains; no lectern.
- `camp/war_gates` 37x11x16, facing south: a timber gatehouse with its arch at local x 15..21 (dx -3..3); `minecraft:barrier` across the arch at local z 10, y 1..3; two pads at local x 7..10 and 26..29, z 1..4 (dx -11..-8 and 8..11, dz 26..29), each a 4x4 of `smooth_quartz` at y 0 in a `polished_blackstone` frame with a lantern post on each side.
- `camp/archive` 23x31x14: stone-brick hall with a deepslate tile roof and its door on the west face; bookshelves; the Founders' Stone (chiseled stone bricks around a lodestone) at local (15,1,23); no lectern.
- `camp/parade` 41x33x6: packed mud and stone-brick parade with paths to the gates, the ring and the side pieces; the parade standard on a 3x3 polished_blackstone plinth at local (20,0,2) (dark oak fence pole at y 1..4, `black_banner` `{bo 4, mc 4}` at y 5); the spawn plinth, a flush 3x3 of polished_andesite centred at local (20,0,26).

Banner notation: pattern code and colour id (0 white, 1 orange, 4 yellow, 7 gray, 9 cyan, 11 blue, 13 green, 15 black); in scripts `{Patterns:[{Pattern:"bo",Color:4},{Pattern:"mc",Color:4}]}`.

The scripts are exported before the suite is written: a studio run needs a harness that compiles.

- [ ] **Step 1: Write the five scripts** (Opus 5.5 medium) to the briefs, each with its `# size` header equal to its `pieces` row.
- [ ] **Step 2: Export them:** `GRAVES_PHASE3_CHECKS=studio GRAVES_STUDIO_MODE=export GRAVES_STUDIO_PIECES=camp/ridge_stair,camp/command_tent,camp/war_gates,camp/archive,camp/parade`. Expected five `pass p3-export-camp-<name>`. The tester states what each `p3-studio-camp-<name>.png` shows; a piece that reads as a plain box goes back to Step 1. Copy the five files to `src/main/resources/data/gravesandglory/structures/camp/`.
- [ ] **Step 3: Add the four sheets** with the rows above, and their rules to `tools/preflight.py`: `pieces` ids unique, each file exists and its NBT size equals `width, height, depth`; `campPieces` templates are `pieces` ids, rotation in {0,90,180,270}, station is `-` or matches `[a-z]+(:[a-z]+)?`, slots are all `-1` or inside the template; `campStations` ids unique, `npc` in {`-`, armorer, cleric, librarian, cartographer, weaponsmith}, `page` in {home, contracts, chapter, match, recruit, friends, army, train}. Expect `Preflight clean`.
- [ ] **Step 4: Write suite `camp`** (it compiles after Step 5):
  - `p3-camp-built`: one `Structures.LOG` entry per `campPieces` row, none missing, each `blocks>0`.
  - `p3-camp-budget`: the logged terrain time is at most 6000 ms and the pieces time at most 1500 ms (why carries both).
  - `p3-camp-spawn`: after `toLobby` the player is within 0.1 of (0.5,65,-291.5) with yaw 180, on a block with a collision shape, with air at feet and head.
  - `p3-camp-leash`: a player set at (0.5,66,-250) is back at spawn within 40 ticks; one at (-95.5,90,-310) stays; one at (0.5,60,-300) is back at spawn.
  - `p3-camp-stations`: each NPC row has one villager within 0.5 of its world position, tagged `camp:<page>`, with NoAI; each label is within 0.5 of `y+3.7`; `CampWorld.station` at each row's position returns its page.
  - `p3-camp-pages`: `openCamp(p,"army")` opens `LobbyScreen`; every other page opens `CampScreen` on that page.
  - `p3-camp-pads`: standing at `padCenter(t)` for 40 ticks puts the squad on team `t`, for both pads.
  - `p3-camp-war-table`: after the camp build and after building maps 0 and 2, every cell holds `tableCell(i,j)`; on map 0 cell (7,4) is a lit candle (point B) and every other row has a cyan or light-gray cell; on map 2 no cell is `green_carpet`.
  - `p3-camp-old-gone`: no entity tagged `gravesandglory-camp-v3`, `gravesandglory-lobby` or `bannerbound-lobby`; no concrete at y 64 in x -11..-8 and 8..11, z -324..-321 (the old pads); (5,64,-310) is not gold_block; (-39,70,-339) is air.
  - `p3-camp-rebuild-idempotent`: a second `CampWorld.build` logs `camp terrain skipped`; a CRC32 of the block states in the bowl box (y 62..90) and the count of `TAG` entities are unchanged.
  - Screenshots (`view`): `camp-arrival` (0.5,67.6,-292.0) to (0.5,67,-330); `camp-ridge` (0.5,82,-361) to (0.5,64,-300); `camp-aerial` (0.5,110,-250) to (0.5,64,-320); `camp-war-table` (0.5,84,-361) to (0.5,78,-371); `camp-gates-inside` (0.5,67,-298) to (0.5,66,-276); `camp-gates-outside` (0.5,70,-271) to (0.5,67,-290); `camp-archive` (30.5,68,-304) to (46,66,-304).
- [ ] **Step 5: Rewrite `CampWorld.build`** in the build order above; `TAG` becomes `gravesandglory-camp-v4`; delete the courtyard loops, `pavilion` and `dress`. Implement `Spot`, `spawn`, `inCamp`, `where`, `station` and `text`, and `CampaignMaps.padCenter`.
- [ ] **Step 6: Replace the camp literals** listed in "Constraints from the current code" with these calls. Then `grep -n "LOBBY_Z+\|LOBBY_Z-\|getZ()>-\|getZ()-CampaignMaps" src/main/java/gg/gravesandglory/*.java` shows only the PARTY lectern line and the `PAD_Z` definition.
- [ ] **Step 7: Add `warTable` and `tableCell`**; call `warTable` at the end of the camp build and right after `campaign.built()` in the build tick.
- [ ] **Step 8: Update `CampaignChecks` and `CampRedesignChecks`** as listed under Files.
- [ ] **Step 9: Compile; run `camp,routes`.** Expected the ten `p3-camp-*` ids, `pass p3-camp-suite` and the route check. The tester states what each of the seven screenshots shows: the arrival view has the parade, the ring area and the ridge; the war table shows Rivergate's river as a cyan line with three candles.
- [ ] **Step 10: Earlier suites still pass:** `-PcampaignChecks` with no Phase 3 variable ends with `pass roadmap-suite`; `GRAVES_CAMP_CHECKS=true` passes `camp-npcs-match-sheet`.
- [ ] **Step 11: Commit** `"Phase 3: built camp base: terrain island, ridge, command tent and war table, War Gates, archive, parade"`.

---

### Task 4: House ring (four tents, the Graveborn graveyard shrine, banners, live troop displays)

Needs Phase 2 Task 3 (`ArmyMobs`, the `houses` sheet, the recruit page with House tabs). Controller ruling 2026-10-22: those exist only on `phase-2-houses` and merging branches is Kort's call, so Task 4 runs after the base merge (or after Kort lets Phase 2 Task 3 into this branch); Tasks 5 to 9 do not wait for it. The graveyard belongs here, not with the yard: it is the Graveborn recruit station and shares the display code.

**Files:**
- Modify: `sheets/design.json` (`pieces` and `campPieces` gain five rows each; `campStations` loses `recruiter`)
- Create: `test-tools/resources/data/gravesandglory_studio/functions/camp/{tent_gilded,tent_wildmoot,tent_ironclad,tent_tidecallers,graveyard}.mcfunction` and their exports
- Modify: `src/main/java/gg/gravesandglory/CampWorld.java` (`slot`, `yaw`, `displays`, tent labels)
- Create: `src/main/java/gg/gravesandglory/CampDisplays.java` (client; `WorldRenderEvents.AFTER_ENTITIES`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (register `CampDisplays`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (the stale-unit filter that uses `ArmyMobs.isTroop` skips entities tagged `CampWorld.TAG`, so displays are never discarded as stale troops)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java` (`open` splits `recruit:<house>` on the first `:` and selects that House tab)
- Modify: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (suite `houses`)

**Sheets.** `pieces`:

| id | width | depth | height | palette | purpose |
|---|---|---|---|---|---|
| camp/tent_gilded | 13 | 11 | 12 | wood | Gilded Order recruiter tent |
| camp/tent_wildmoot | 13 | 11 | 12 | wood | Wildmoot recruiter tent |
| camp/tent_ironclad | 13 | 11 | 12 | wood | Ironclad recruiter tent |
| camp/tent_tidecallers | 13 | 11 | 12 | wood | Tidecallers recruiter tent |
| camp/graveyard | 23 | 23 | 16 | stone | Graveborn shrine and recruiter |

`campPieces`:

| id | template | dx | dz | y | rotation | flatten | station | slotX | slotY | slotZ |
|---|---|---|---|---|---|---|---|---|---|---|
| gilded | camp/tent_gilded | -26 | -6 | 64 | 270 | true | recruit:gilded | 1 | 2 | 9 |
| wildmoot | camp/tent_wildmoot | -18 | -24 | 64 | 0 | true | recruit:wildmoot | 1 | 2 | 9 |
| graveborn | camp/graveyard | 0 | -39 | 64 | 0 | true | recruit:graveborn | 6 | 1 | 19 |
| ironclad | camp/tent_ironclad | 18 | -24 | 64 | 0 | true | recruit:ironclad | 1 | 2 | 9 |
| tidecallers | camp/tent_tidecallers | 26 | -6 | 64 | 90 | true | recruit:tidecallers | 1 | 2 | 9 |

World footprints, for checks: gilded x -31..-21, z -322..-310; wildmoot x -24..-12, z -339..-329; graveborn x -11..11, z -360..-338; ironclad x 12..24, z -339..-329; tidecallers x 21..31, z -322..-310. First display slots (k=0, feet): gilded (-21.5,66,-310.5) running north, wildmoot (-22.5,66,-329.5) running east, graveborn (-4.5,65,-340.5) running east, ironclad (13.5,66,-329.5) running east, tidecallers (22.5,66,-320.5) running south.

**Tent brief** (each authored facing south): a closed tent body at local z 0..7; an open porch at z 8..10 under a roof at y 7; a plinth at y 1 along x 0..12, z 9, with the six display slots at x 1..11 step 2, z 9 (feet at y 2); above the porch roof a gable along z 8, y 8..10, carrying the House wall banner at (6,9,9) facing south; a dark oak fence pole at (0,1..6,10) with the House standing banner at (0,7,10). Banners, written as pattern and colour id:
- Gilded Order: white and yellow wool, birch, quartz, gold blocks; white banner {sc 4, bo 4}.
- Wildmoot: brown wool, spruce, bone blocks, hay, moss; brown banner {tts 0, bts 0, mr 1}.
- Ironclad: black and gray wool, iron blocks, iron bars, chains, a blast furnace, a fenced campfire (no magma); black banner {bri 7, mc 1, bo 4}.
- Tidecallers: cyan and blue wool, prismarine, a tide pool, sea lanterns (no cobwebs); blue banner {gru 9, bts 0, bo 9}.

**Graveyard brief** (23x23x16, facing south): cobbled deepslate paths, stone-brick walls, soul lanterns, podzol and coarse dirt, gravestones; at the north a polished blackstone and deepslate tile shrine whose back wall meets the ridge face; a roofed colonnade over x 4..18, z 18..20 (roof at y 5, back wall along z 17 at y 1..4) with the slots at x 6..16 step 2, z 19 (feet at y 1); the black wall banner {gru 13, sku 0, bo 15} on the back wall at (11,4,18) facing south.

**Interfaces:**
- Produces `CampWorld.slot(Spec.CampPiecesRow row,int k) -> Vec3d` (pure, so the client can call it): `Structures.toWorld(size,originX+dx,y,originZ+dz,rotation,slotX+2k,slotY,slotZ)` plus (0.5,0,0.5), with `size` from the row's `pieces` entry (`width, height, depth`).
- Produces `CampWorld.yaw(int rotation) -> float`: 0 gives 0, 90 gives 90, 180 gives 180, 270 gives -90 (a display faces its tent's front).
- Produces `CampWorld.displays(ServerWorld w)`, run in the build's entity step: for each row whose station starts with `recruit:`, the House's `troops` split on `,`; troop `k` (its `Spec.TroopsRow t`) is `ArmyMobs.create(t.entity(),w)` (on `IllegalStateException`, `ArmyMobs.portrait`), equipped as `Battle.spawn` does (extract `ArmyMobs.equip(MobEntity,Spec.TroopsRow)` if that code is inline), NoAI, invulnerable, silent, persistent, custom name the troop `name` with `setCustomNameVisible(false)` (so the troop model and skin resolve from the name, as in battle), at `slot(row,k)` with `yaw(rotation)`, tags `TAG`, `camp-generation:<g>`, `camp:recruit:<house>`, `camp-display:<troop id>`. Tent labels "<HOUSE NAME> / Recruit" (name upper case) at local (6.5,11.5,10.5), the graveyard's at (11.5,7.5,21.5), through `toWorld`.
- Produces `CampDisplays` (client): each frame, for display slots within 32 blocks of the camera, draws at `slot+(0,3.2,0)` the troop name over `<RARITY> / LOCKED`, `<RARITY> / 1 STAR` or `<RARITY> / <n> STARS` (RARITY is COMMON, RARE, EPIC or LEGENDARY), read from the client's copy of the profile's `owned` map (the one the recruit page cards read). Harness fields `int lastDrawn`, `List<String> lastTexts`.
- Consumes `ArmyMobs.create/portrait`, `Spec.HOUSES`, `Spec.TROOPS`, the recruit page's House tabs.

- [ ] **Step 1: Write the five scripts** to the briefs.
- [ ] **Step 2: Export them** (`GRAVES_STUDIO_PIECES=camp/tent_gilded,camp/tent_wildmoot,camp/tent_ironclad,camp/tent_tidecallers,camp/graveyard`); the tester states what each studio shot shows (palette, banner, porch or colonnade); copy the files.
- [ ] **Step 3: Add the rows; delete the `recruiter` row.** Expect `Preflight clean`.
- [ ] **Step 4: Write suite `houses`:**
  - `p3-houses-pieces`: all five rows in `Structures.LOG`, none missing.
  - `p3-houses-banners`: for each row, the wall banner (tents (6,9,9), graveyard (11,4,18)) and, for tents, the standing banner (0,7,10), at their `toWorld` positions, hold the House patterns in order; each wall banner faces the rotated south.
  - `p3-houses-displays`: 30 display mobs, six per House in `troops` order, each within 0.1 of `slot(row,k)`, NoAI, invulnerable, persistent, of the troop's entity type, with the hidden custom name.
  - `p3-houses-shaded`: `w.isSkyVisible(BlockPos.ofFloored(slot).up())` is false for all 30 slots.
  - `p3-houses-survive`: after 600 ticks, then a `camp:practice` start and `returnLobby`, the same 30 UUIDs are alive and none moved more than 0.1.
  - `p3-houses-opens`: `CampWorld.station` at each slot returns `recruit:<house>`; using a Graveborn display mob opens `recruit:graveborn`; `openCamp(p,"recruit:gilded")` opens `CampScreen` on the recruit page with the Gilded tab selected (screenshot `camp-recruit-gilded`).
  - `p3-houses-labels`: after each `camp-house-<id>` view, `CampDisplays.lastTexts` holds that House's six names, each over LOCKED or its star count exactly as the test profile's `owned` map says.
  - `p3-houses-recruiter-gone`: no villager tagged `camp:recruit`.
  - Screenshots: `camp-ring` (0.5,72,-291) to (0.5,66,-330); `camp-house-<id>` for the five rows, the camera `max(width,depth)+8` from the piece centre toward the ring centre (0.5,y,-315.5) at y 67.6, looking at the piece centre at y 67.
- [ ] **Step 5: Implement `slot`, `yaw`, `displays` and the tent labels.**
- [ ] **Step 6: Implement `CampDisplays` and the `open` split**; register `CampDisplays` in `GravesAndGloryClient`.
- [ ] **Step 7: Compile; run `houses,camp,routes`.** Expected the eight `p3-houses-*` ids, both suite lines, the ten camp ids and the route check. The tester states for each `camp-house-<id>` that the banner, palette and six troops show, and for `camp-ring` that four tents and the shrine face the parade.
- [ ] **Step 8: Commit** `"Phase 3: House ring: four tents, Graveborn graveyard shrine, banners and live troop displays"`.

---

### Task 5: Training yard and the tutorial hooks

**Files:**
- Modify: `sheets/design.json` (`pieces`, `campPieces` and `campStations` rows below; `camp` gains `firstDx -33.5`, `firstDz 6.5`, `firstYaw 90.0`; new sheet `campLessons`)
- Modify: `tools/preflight.py` (`campLessons` ids unique; every `{key:<id>}` names `menu, attack, follow, formation, map, rotate_left, rotate_right, confirm, cancel` or `use`)
- Create: `test-tools/resources/data/gravesandglory_studio/functions/camp/training_yard.mcfunction` and its export
- Modify: `src/main/java/gg/gravesandglory/CampWorld.java` (`lessons`, `dummies`, first arrival in `spawn`)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java` (page `train`)
- Modify: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (suite `yard`)

**Rows.** `pieces`: `camp/training_yard | 23 | 31 | 8 | wood | training yard and tutorial`. `campPieces`: `yard | camp/training_yard | -46 | 6 | 64 | 0 | true | train | -1 | -1 | -1` (world x -57..-35, z -319..-289). `campStations`: `drillmaster | train | weaponsmith | -38.5 | 65.0 | 6.5 | -90.0 | 1 | 1 | DRILLMASTER / Battle 1 and practice`.

`campLessons` (columns `id, dx, y, dz, text`; the HUD lesson copy of `GravesAndGloryClient.lesson`, with lesson 4 also naming the use key):

| id | dx | y | dz | text |
|---|---|---|---|---|
| lesson-1 | -55.5 | 67.2 | -3.5 | LESSON 1 OF 4 / Press {key:menu}, choose a group and Move, then click the ground to send them. |
| lesson-2 | -55.5 | 67.2 | 2.5 | LESSON 2 OF 4 / Press {key:formation} to change formation. Shieldwall holds a line; Wedge breaks one. |
| lesson-3 | -55.5 | 67.2 | 8.5 | LESSON 3 OF 4 / Press {key:attack} and aim at an enemy or a point to send your army in. |
| lesson-4 | -55.5 | 67.2 | 14.5 | LESSON 4 OF 4 / Recruit at your barracks lectern with {key:use}. Trained troops join your army at the front. |

**Yard brief** (23x31x8, facing south, open on the east): fenced coarse dirt and gravel; four backboards (dark oak planks) at local x 0, z 4..6, 10..12, 16..18 and 22..24, y 1..4, one behind each lesson; six dummy pads (smooth stone) along z 9 at x 4..14 step 2; weapon racks; a shade roof over x 16..20, z 13..17 for the Drillmaster.

**Interfaces:**
- `CampWorld.spawn(Profile p)` returns the first arrival `(originX+firstDx, spawnY, originZ+firstDz)` = (-33.5,65,-303.5), yaw 90, pitch 8 while `p.campaignCleared==0`, else the normal spawn.
- `CampWorld.lessons(ServerWorld w)`: one TextDisplay per row at `(originX+dx, y, originZ+dz)`, billboard center, the label style, text through `CampWorld.text`, tags `TAG` and `camp-lesson:<id>`.
- `CampWorld.dummies(ServerWorld w)`: six armor stands at (-52.5+2k, 65, -309.5), yaw -90, invulnerable, no gravity, iron helmet and leather chestplate, tags `TAG` and `camp-dummy`.
- Page `train` on `CampScreen`: heading TRAINING YARD, the `march-1` row's name and briefing, buttons START (sends `campaign:march-1`) and PRACTICE (sends `camp:practice`).

- [ ] **Step 1: Write and export the script** (`GRAVES_STUDIO_PIECES=camp/training_yard`); the tester states what the studio shot shows; copy the file.
- [ ] **Step 2: Add the rows, the `camp` columns and the `campLessons` sheet with its preflight rule.** Expect `Preflight clean`.
- [ ] **Step 3: Write suite `yard`:**
  - `p3-yard-drillmaster`: one villager tagged `camp:train` within 0.5 of (-38.5,65,-303.5); `openCamp(p,"train")` opens the train page (screenshot `camp-train-page`).
  - `p3-yard-start`: pressing START gives `running` within 900 ticks with `activeBattle` `march-1`; then `returnLobby`.
  - `p3-yard-lessons`: four TextDisplays tagged `camp-lesson:*` at their rows; each text's JSON holds the keybind `key.gravesandglory.menu`, `formation`, `attack` and `key.use` in turn; with `menu` rebound to K, the rendered lesson 1 reads `Press K,` (screenshot `camp-yard-lessons` from (-51.5,67.6,-313.5) to (-55.5,67.2,-313.5); restore the binding).
  - `p3-yard-dummies`: six armor stands tagged `camp-dummy` at their positions, invulnerable.
  - `p3-yard-first-arrival`: with `campaignCleared=0`, `toLobby` puts the player within 0.1 of (-33.5,65,-303.5) facing yaw 90 (screenshot `camp-first-arrival`, the player's view); with `campaignCleared=1`, at the normal spawn. Restore the profile.
  - Screenshot `camp-yard` from (-28.5,69,-290) to (-46,66,-304).
- [ ] **Step 4: Implement** `spawn`'s first arrival, `lessons`, `dummies` and page `train`.
- [ ] **Step 5: Compile; run `yard,camp,routes`.** Expected the five `p3-yard-*` ids, both suite lines, the camp ids and the route check; the tester sees the four boards, six dummies and the Drillmaster in `camp-yard` and reads lesson 1's key in `camp-yard-lessons`.
- [ ] **Step 6: Commit** `"Phase 3: training yard with key-named lesson boards, dummies and the battle 1 start"`.

---

### Task 6: Landmarks I: sheets, placement queue, kit pieces, Rivergate and Kingsfield

**Files:**
- Modify: `sheets/design.json` (`pieces` gains ten rows; new sheets `landmarks` and `landmarkLines`)
- Modify: `tools/preflight.py` (rules for both sheets)
- Create: scripts `kit/{house_a,house_b,market,farm,customs,warehouse,shrine,watchtower}`, `rivergate/mill`, `kingsfield/outpost` under `test-tools/resources/data/gravesandglory_studio/functions/`, and their exports under `src/main/resources/data/gravesandglory/structures/`
- Modify: `src/main/java/gg/gravesandglory/Structures.java` (`queueLandmarks`, `step`, `clearQueue`, `queued`, `hash`, `salt`)
- Modify: `src/main/java/gg/gravesandglory/CampaignMaps.java` (`select` applies the map's landmark rows)
- Modify: `src/main/java/gg/gravesandglory/Arena.java` (`height`: the structures loop skips `landmark`; a landmark pass after the road blend)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (build tick drains the queue; field `boolean placing`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (marker gains `"landmarks":"<hash>"`; `switchMap` calls `Structures.clearQueue()` and sets `placing=false`)
- Modify: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (suite `landmarks`)

**Sheets.** `pieces`:

| id | width | depth | height | palette | purpose |
|---|---|---|---|---|---|
| kit/house_a | 15 | 13 | 11 | wood | hamlet cottage, door east |
| kit/house_b | 15 | 13 | 11 | wood | hamlet two-storey house, door east |
| kit/market | 19 | 9 | 6 | wood | market stalls |
| kit/farm | 27 | 19 | 3 | wood | fenced wheat field |
| kit/customs | 19 | 15 | 12 | wood | toll house |
| kit/warehouse | 25 | 19 | 12 | stone | stone storehouse |
| kit/shrine | 23 | 19 | 15 | stone | roadside shrine |
| kit/watchtower | 11 | 11 | 17 | stone | base watchtower, door west |
| rivergate/mill | 19 | 21 | 16 | wood | mill and yard at the north crossing |
| kingsfield/outpost | 47 | 47 | 14 | wood | flank outpost enclosing a point |

`landmarks` (columns `id, map, template, x, z, rotation, elevation, replaces, encloses`; `x, z` is the centre block; `-` means none). Every map ends with four tower rows, with `p` its prefix (`rg` 0, `ih` 1, `fm` 2, `hw` 3, `kf` 4): `p-dawn-tower-n | m | kit/watchtower | -180 | -85 | 0 | 67 | dawn_tower_-85 | -`, `p-dawn-tower-s | m | kit/watchtower | -180 | 85 | 0 | 67 | dawn_tower_85 | -`, `p-dusk-tower-n | m | kit/watchtower | 180 | -85 | 180 | 67 | dusk_tower_-85 | -`, `p-dusk-tower-s | m | kit/watchtower | 180 | 85 | 180 | 67 | dusk_tower_85 | -`. The other rows of maps 0 and 4 (rotation 0, elevation 64, encloses `-` unless shown):

| id | map | template | x | z | replaces |
|---|---|---|---|---|---|
| rg-house-0 | 0 | kit/house_a | -85 | -133 | house_0 |
| rg-house-1 | 0 | kit/house_b | -111 | -133 | house_1 |
| rg-house-2 | 0 | kit/house_b | -85 | -118 | house_2 |
| rg-house-3 | 0 | kit/house_a | -111 | -118 | house_3 |
| rg-market | 0 | kit/market | -60 | -112 | market |
| rg-farm | 0 | kit/farm | -100 | -170 | farm |
| rg-mill | 0 | rivergate/mill | -28 | -175 | mill |
| rg-customs | 0 | kit/customs | -58 | 30 | customs |
| rg-warehouse | 0 | kit/warehouse | 60 | -30 | warehouse |
| rg-shrine | 0 | kit/shrine | 69 | 127 | shrine |
| kf-house-0 | 4 | kit/house_a | -84 | -148 | house_0 |
| kf-house-1 | 4 | kit/house_b | -110 | -148 | house_1 |
| kf-house-2 | 4 | kit/house_b | -84 | -133 | house_2 |
| kf-house-3 | 4 | kit/house_a | -110 | -133 | house_3 |
| kf-market | 4 | kit/market | -59 | -127 | market |
| kf-farm | 4 | kit/farm | -99 | -185 | farm |
| kf-mill | 4 | rivergate/mill | -27 | -190 | mill |
| kf-customs | 4 | kit/customs | -45 | 47 | customs |
| kf-warehouse | 4 | kit/warehouse | 45 | -47 | warehouse |
| kf-shrine | 4 | kit/shrine | 68 | 142 | shrine |
| kf-outpost-west | 4 | kingsfield/outpost | -130 | 100 | - |
| kf-outpost-east | 4 | kingsfield/outpost | 130 | -100 | - |

Exceptions: `kf-outpost-west` has elevation 74 and encloses `B`; `kf-outpost-east` has rotation 180, elevation 74 and encloses `D`. Sheet order: the map 0 rows, its towers, then map 4's, so the first sheet row is `rg-house-0`. The mills sit two blocks north of the old `mill` rows so their larger footprint keeps out of ring A.

`landmarkLines` (columns `id, template, kind, fromX, fromY, fromZ, toX, toY, toZ`; local coordinates, eye points at feet+1.6; a `toY` of -1.0 means `Arena.ground(floor(x),floor(z))+2.6` at the world end point). A cover line must be blocked, a sight line clear:

| id | template | kind | fromX | fromY | fromZ | toX | toY | toZ |
|---|---|---|---|---|---|---|---|---|
| mill-cover | rivergate/mill | cover | 11.5 | 2.6 | 17.5 | 6.5 | 2.6 | 39.5 |
| mill-sight | rivergate/mill | sight | 14.5 | 9.6 | 11.5 | 20.5 | 2.6 | 39.5 |
| outpost-cover | kingsfield/outpost | cover | 36.5 | 2.6 | 36.5 | 42.5 | 2.6 | 42.5 |
| outpost-sight | kingsfield/outpost | sight | 8.5 | 11.6 | 38.5 | -7.5 | -1.0 | 23.5 |

World values the checks must reproduce: `rg-mill` cover (-25.5,66.6,-167.5) to (-30.5,66.6,-145.5), sight (-22.5,73.6,-173.5) to (-16.5,66.6,-145.5) (the north bridge deck); `kf-mill` cover (-24.5,66.6,-182.5) to (-29.5,66.6,-160.5), sight (-21.5,73.6,-188.5) to (-15.5,66.6,-160.5); `kf-outpost-west` cover (-116.5,76.6,113.5) to (-110.5,76.6,119.5), sight (-144.5,85.6,115.5) to (-160.5,ground,100.5); `kf-outpost-east` cover (117.5,76.6,-112.5) to (111.5,76.6,-118.5), sight (145.5,85.6,-114.5) to (161.5,ground,-99.5).

**Briefs** (local coordinates; every piece faces south unless stated):
- `kit/house_a`: cobblestone plinth, oak planks in a stripped oak frame, dark oak roof, a chimney; east door at x 14, z 5..7; windows north and south.
- `kit/house_b`: two-storey spruce house; east porch with the door at x 14, z 5..7; a lean-to on the west.
- `kit/market`: three stalls under white, cyan and red awnings; walk-throughs at x 5..6 and 12..13.
- `kit/farm`: wheat (`age=7`) at y 1 on farmland at y 0; water at y 0 where `x%5==2`; an oak fence round the edge at y 1 with 3-wide gates mid-side (x 12..14 north and south, z 8..10 east and west).
- `kit/customs`: a toll house with a covered arch; doors north and south.
- `kit/warehouse`: stone-brick store, deepslate tile roof, wide doors on both long sides.
- `kit/shrine`: quartz pillars on a smooth quartz floor, a raised altar with sea lanterns, open on every side.
- `kit/watchtower` (elevation 67): stone tower; west door at x 0, z 4..6; a ladder to a crenellated top at feet 13; a lantern.
- `rivergate/mill`: the mill (stone base, oak and spruce frame) at x 8..16, z 0..12, a water wheel (spruce log axle, planks and slabs) against its east wall at x 17..18, z 3..9; doors west at x 8, z 9..11 and south at z 12, x 12..14; a loft floor at y 7 (feet 8) with a south window at x 14..15, y 9..10; an L-shaped yard (x 0..7 for z 0..20, and z 13..20 for x 0..18) inside a 2-high cobblestone wall on its outer sides, with the south yard gate at x 4..8 (the mill lane enters there).
- `kingsfield/outpost` (elevation 74; centre block (23,23)): at y 0, structure void where the arena keeps its own blocks (the ring cells between radius 15.5 and 17.5, and the centre 3x3), paving inside radius 15.5, packed mud and gravel outside 17.5; structure void at y 1..8 where `|x-23|<=3 and |z-23|<=3` (the flag); a spruce log stockade at radius 21..23, y 1..4, spruce fence on top at y 5; gates open from y 1 to 5 at every stockade cell within 3.5 of a road centreline into the point: the west road along z 23 (x 0..2, z 20..26) and the centre road from (23,23) toward (125,-77) (around (39,8)); three lookouts, 3x3 spruce towers with a ladder to a platform at y 9 (feet 10) and a parapet of spruce bottom slabs at y 10, at SE x 37..39, z 37..39, SW x 7..9, z 37..39 and NW x 7..9, z 7..9; 2-high stone-brick inner walls on the cells between radius 9 and 12 within 15 degrees of the NW, SE and SW diagonals (the NE diagonal carries the centre road).

**Interfaces:**
- `CampaignMaps.select`, after the map's rows are built and before `Spec.STRUCTURES` is assigned (the woodland loop reads it): for each `landmarks` row with `map==selected`, in sheet order, remove the row whose id equals `replaces` (unless `-`; when no row has that id and no earlier row of this map named it, log `[Graves and Glory] landmark <id>: no row <replaces>`), then add `s(id,"landmark",x,z,w,d,h,palette,elevation)` with `w, d, h, palette` from the `pieces` row (`w` and `d` swapped at 90 and 270).
- `Arena.height`: the structures loop skips `landmark` as well as `bridge`; after the road blend, for each `landmark` row, `dx=max(0,|x-s.x()|-s.width()/2.0-2)` (the same for z) and `h=blend(h,s.elevation(),hypot(dx,dz),0,10+2*(s.elevation()-64))`.
- `Structures.queueLandmarks(ServerWorld w)` queues the selected map's rows in sheet order; `step(ServerWorld w) -> boolean` places the next (`place(w,template,x,elevation,z,rotation)`) and returns true once empty; `clearQueue()`; `queued() -> int`; `hash(int map) -> String`, the CRC32 hex of the map's `landmarks` rows, their templates' file bytes, their `landmarkLines` rows and `salt`; `static String salt=""` (harness only).
- Build tick: when `buildColumn>=total && !placing`: `Arena.finish(world)`, `Structures.queueLandmarks(world)`, `placing=true`. While `placing`, each tick: `if(Structures.step(world)){campaign.built();CampWorld.warTable(world);ready=true;placing=false;message="Your war council is ready. Press R.";Diagnostics.report("arena-ready",true,null);}`.
- `Campaign`: marker `{"layout":"campaign-3","map":N,"landmarks":"<hash(N)>"}` in the constructor, `cached()` and `built()`.

- [ ] **Step 1: Write the ten scripts** to the briefs.
- [ ] **Step 2: Export them** (`GRAVES_STUDIO_PIECES` = the ten ids); the tester states what each studio shot shows; copy the files.
- [ ] **Step 3: Add the `pieces` rows, both new sheets and their preflight rules:** `landmarks` ids unique, `map` 0..4, `template` a `pieces` id, rotation in {0,90,180,270}, elevation 60..90, `replaces` `-` or a name, `encloses` `-` or one of A..E; footprints of one map do not overlap; `landmarkLines` ids unique, `template` a `pieces` id, `kind` cover or sight. Expect `Preflight clean`.
- [ ] **Step 4: Write suite `landmarks`** over the maps in `GRAVES_PHASE3_MAPS`; on each, switch and wait for `ready`, then per landmark row:
  - `p3-lm-<id>-placed`: in `LOG`, not missing; the replaced row is gone from `Spec.STRUCTURES`.
  - `p3-lm-<id>-fits`: `Placed.box` equals `footprint(size,x,elevation,z,rotation)` and lies inside |x|<=350, |z|<=250.
  - `p3-lm-<id>-clear`: every structure-void template cell at local y 1 and up is air in the world, except a reserved column: an enclosed point's flag column and, from Task 8b, the yard-centre column of a supply house (y 1 to 8).
  - `p3-lm-<id>-roads`: `Walkability.scan(b,box)` has `through==0` and no deck hit.
  - `p3-lm-<id>-ring`: no non-void template cell within 19 of a point the row does not enclose; for an enclosed point, the ring, centre and flag blocks are as the arena built them.
  - `p3-lm-<id>-gates` (rows that enclose a point): for each road with an endpoint at that point, every cell within 2 of its centreline, from radius 17.5 to the footprint edge, has no collision shape at ground+1 and +2.
  - `p3-lm-<id>-cover` and `-sight` per `landmarkLines` row of its template: world points through `toWorld`; `world.raycast` with `COLLIDER`, `FluidHandling.NONE` and the harness player. Cover passes when the hit is a block more than 0.3 before `to`; sight when it misses or hits within 0.3 of `to`; `why` names the hit block and position.
  - Per map `p3-lm-budget-map<N>`: each row's `Placed.nanos` at most 100 ms and their sum at most 1.5 s.
  - Once per run: `p3-lm-switch-mid-queue` (switch to map 4, wait for `queued()>0`, switch to map 0; `queued()==0` right after, `ready` only after map 0's queue drains, no `kf-` id in `LOG` after the second switch); `p3-lm-cache` (`campaign.cached()` true after a build, false with `salt="test"`; restore it).
  - Screenshots: `lm-map<N>-overview` from (0,175,250), yaw 180, pitch 40. `lm-<id>-ground` for the first row of each template on the map, and also `lm-<id>-high` when the template is not `kit/`: with `u` the unit vector from the centre toward the nearest point the row does not enclose, the ground camera is at centre+`u*(max(w,d)+12)` at y = max(elevation, ground there)+5, the high one at centre+`u*(max(w,d)+30)` at y elevation+30; both look at (x+0.5, elevation+4, z+0.5).
- [ ] **Step 5: Implement the `select` rows and the height pass.**
- [ ] **Step 6: Implement the queue, the build tick, `clearQueue` in `switchMap`, `hash` and the marker.**
- [ ] **Step 7: Compile; run `landmarks,routes` with `GRAVES_PHASE3_MAPS=0,4`.** Expected every `p3-lm-*` id for the 30 rows, both budgets, both once-per-run ids, `pass p3-landmarks-suite` and the route check. The tester states what `lm-rg-mill-ground`, `lm-rg-mill-high`, `lm-kf-outpost-west-high` and both overviews show: loft window toward the deck, stockade with two gates round B.
- [ ] **Step 8: Commit** `"Phase 3: landmark pipeline, kit pieces, Rivergate mill and Kingsfield outposts"`.

---

### Task 7: Ironhold landmarks (gate, keep, curtains, hay barn, garrison)

**Files:**
- Modify: `sheets/design.json` (`pieces` +5, `landmarks` +21, `landmarkLines` +4)
- Create: scripts `ironhold/{keep,gate,curtain,hay_barn,garrison}` and their exports
- Modify: `src/main/java/gg/gravesandglory/Arena.java` (delete the hay line in `finish`)

**Sheets.** `pieces`:

| id | width | depth | height | palette | purpose |
|---|---|---|---|---|---|
| ironhold/keep | 47 | 27 | 22 | stone | river keep south of B |
| ironhold/gate | 7 | 51 | 14 | stone | west gate, 11-wide passage |
| ironhold/curtain | 5 | 31 | 15 | stone | curtain section with end towers |
| ironhold/hay_barn | 25 | 19 | 13 | wood | hay barn and hay yard |
| ironhold/garrison | 31 | 23 | 15 | stone | garrison barracks and yard |

`landmarks` (map 1, elevation 64, encloses `-`; then the four `ih-` tower rows):

| id | template | x | z | rotation | replaces |
|---|---|---|---|---|---|
| ih-house-0 | kit/house_a | -75 | -118 | 0 | house_0 |
| ih-house-1 | kit/house_b | -101 | -118 | 0 | house_1 |
| ih-house-2 | kit/house_b | -75 | -103 | 0 | house_2 |
| ih-house-3 | kit/house_a | -101 | -103 | 0 | house_3 |
| ih-market | kit/market | -50 | -97 | 0 | market |
| ih-keep | ironhold/keep | 0 | 34 | 0 | ironhold |
| ih-gate | ironhold/gate | -37 | 0 | 0 | breach |
| ih-curtain-wn1 | ironhold/curtain | -72 | -43 | 0 | curtain_-1_-62 |
| ih-curtain-wn2 | ironhold/curtain | -72 | -81 | 0 | curtain_-1_-62 |
| ih-curtain-ws1 | ironhold/curtain | -72 | 43 | 0 | curtain_-1_62 |
| ih-curtain-ws2 | ironhold/curtain | -72 | 81 | 0 | curtain_-1_62 |
| ih-curtain-en1 | ironhold/curtain | 72 | -43 | 180 | curtain_1_-62 |
| ih-curtain-en2 | ironhold/curtain | 72 | -81 | 180 | curtain_1_-62 |
| ih-curtain-es1 | ironhold/curtain | 72 | 43 | 180 | curtain_1_62 |
| ih-curtain-es2 | ironhold/curtain | 72 | 81 | 180 | curtain_1_62 |
| ih-hay-barn | ironhold/hay_barn | -105 | -160 | 0 | supply_barn |
| ih-garrison | ironhold/garrison | 65 | 147 | 0 | garrison |

`landmarkLines`:

| id | template | kind | fromX | fromY | fromZ | toX | toY | toZ |
|---|---|---|---|---|---|---|---|---|
| gate-cover | ironhold/gate | cover | 7.5 | 2.6 | 9.5 | -20.5 | -1.0 | 9.5 |
| gate-sight | ironhold/gate | sight | 1.5 | 14.6 | 16.5 | -20.5 | -1.0 | 25.5 |
| keep-cover | ironhold/keep | cover | 14.5 | 2.6 | 4.5 | 26.5 | 2.6 | -20.5 |
| keep-sight | ironhold/keep | sight | 14.5 | 8.6 | 0.5 | 26.5 | 2.6 | -20.5 |

World values: `ih-gate` cover (-32.5,66.6,-15.5) to (-60.5,ground,-15.5), sight (-38.5,78.6,-8.5) to (-60.5,ground,0.5) (the west field in line with the passage); `ih-keep` cover (-8.5,66.6,25.5) and sight (-8.5,72.6,21.5), both to (3.5,66.6,0.5) on the B deck.

**Briefs:**
- `ironhold/gate` (outer face local x 0, the west): the passage at z 20..30 (world z -5..5), clear from y 1 to 6 across x 0..6; towers at z 13..19 and 31..37, 14 high, top floor at y 12 (feet 13), a parapet at y 13 whose west side is open at z 15..17 (north tower) and z 33..35 (south tower); walls 10 high along z 0..12 and 38..50; iron-bar portcullis above y 6 only. `ironhold/gate_intact` is reserved for Siege and not authored.
- `ironhold/keep` (world x -23..23, z 21..47): the north wall along z 0..2, solid to y 6, a parapet on z 0 at y 7..8 with a crenel at x 14..15, the wall-walk on top (feet 7); 5-wide land gates through it at x 7..11 and 35..39 (world x -16..-12 and 12..16), y 1..4; the north and south walls cross the river on iron bars at x 18..28, y 0..1; a timber courtyard deck at y 0 over the river; the donjon on the east bank. It is not a bridge row and the route harness does not test it.
- `ironhold/curtain` (outer face local x 0): a 12-high wall with a 2-wide wall-walk and crenels on the outer face; towers 15 high at z 0..4 and 26..30. Each side's two pieces leave the 7-wide sally gaps at world z -65..-59 and 59..65.
- `ironhold/hay_barn`: a timber barn with hay bales inside and in a fenced yard (the old hay line's job); doors east and south.
- `ironhold/garrison`: a stone barracks block with a walled yard; doors toward its lane.

- [ ] **Step 1: Write the five scripts** to the briefs.
- [ ] **Step 2: Export them**; the tester states what each studio shot shows; copy the files.
- [ ] **Step 3: Add the rows.** Expect `Preflight clean`.
- [ ] **Step 4: Delete the hay line** in `Arena.finish`: `if(CampaignMaps.selected==1)for(int row=0;row<4;row++)box(w,-109+row*4,65,-152,3,2,3,Blocks.HAY_BLOCK);`.
- [ ] **Step 5: Compile; run `landmarks,routes` with `GRAVES_PHASE3_MAPS=1`.** Expected every `p3-lm-*` id for the 21 rows, the budget, both once-per-run ids, the suite line and the route check (`p3-routes-map1` shows 3 crossings). The tester states what `lm-ih-gate-ground` (passage between two towers), `lm-ih-keep-high`, `lm-ih-curtain-wn1-ground`, `lm-ih-hay-barn-ground` and `lm-map1-overview` show.
- [ ] **Step 6: Commit** `"Phase 3: Ironhold landmarks: gate, keep, curtains, hay barn and garrison"`.

---

### Task 8: Frostmarch and Hollowwood landmarks

**Files:**
- Modify: `sheets/design.json` (`pieces` +7, `landmarks` +29, `landmarkLines` +4)
- Create: scripts `frostmarch/{lodge,refuge,signal_tower}`, `hollowwood/{chapel,sawmill,logging_camp,moon_shrine}` and their exports

**Sheets.** `pieces`:

| id | width | depth | height | palette | purpose |
|---|---|---|---|---|---|
| frostmarch/lodge | 27 | 19 | 14 | wood | caravan lodge and yard on its lane |
| frostmarch/refuge | 21 | 17 | 12 | wood | pass refuge |
| frostmarch/signal_tower | 15 | 15 | 21 | stone | signal tower with its fire |
| hollowwood/chapel | 29 | 23 | 18 | stone | broken chapel over the south lane |
| hollowwood/sawmill | 25 | 19 | 13 | wood | sawmill |
| hollowwood/logging_camp | 19 | 17 | 6 | wood | logging camp holding the old log pile |
| hollowwood/moon_shrine | 23 | 19 | 16 | stone | moon shrine |

`landmarks` (rotation 0, elevation 64, encloses `-`; each map then has its four tower rows, `fm-` on map 2 and `hw-` on map 3):

| id | map | template | x | z | replaces |
|---|---|---|---|---|---|
| fm-house-0 | 2 | kit/house_a | -83 | -143 | house_0 |
| fm-house-1 | 2 | kit/house_b | -109 | -143 | house_1 |
| fm-house-2 | 2 | kit/house_b | -83 | -128 | house_2 |
| fm-house-3 | 2 | kit/house_a | -109 | -128 | house_3 |
| fm-customs | 2 | kit/customs | -58 | 30 | customs |
| fm-warehouse | 2 | kit/warehouse | 60 | -30 | warehouse |
| fm-shrine | 2 | kit/shrine | 67 | 137 | shrine |
| fm-lodge | 2 | frostmarch/lodge | -80 | -178 | caravan_lodge |
| fm-refuge | 2 | frostmarch/refuge | 62 | 34 | pass_refuge |
| fm-signal-tower | 2 | frostmarch/signal_tower | 62 | 170 | signal_tower |
| hw-house-0 | 3 | kit/house_a | -92 | -133 | house_0 |
| hw-house-1 | 3 | kit/house_b | -118 | -133 | house_1 |
| hw-house-2 | 3 | kit/house_b | -92 | -118 | house_2 |
| hw-house-3 | 3 | kit/house_a | -118 | -118 | house_3 |
| hw-market | 3 | kit/market | -67 | -112 | market |
| hw-customs | 3 | kit/customs | -58 | 30 | customs |
| hw-warehouse | 3 | kit/warehouse | 60 | -30 | warehouse |
| hw-chapel | 3 | hollowwood/chapel | 70 | 100 | broken_chapel |
| hw-sawmill | 3 | hollowwood/sawmill | -85 | -176 | sawmill |
| hw-logging-camp | 3 | hollowwood/logging_camp | -109 | -170 | - |
| hw-moon-shrine | 3 | hollowwood/moon_shrine | 76 | 127 | shrine |

`landmarkLines`:

| id | template | kind | fromX | fromY | fromZ | toX | toY | toZ |
|---|---|---|---|---|---|---|---|---|
| lodge-cover | frostmarch/lodge | cover | 17.5 | 2.6 | 2.5 | 42.5 | -1.0 | 20.5 |
| lodge-sight | frostmarch/lodge | sight | 25.5 | 8.6 | 14.5 | 42.5 | -1.0 | 20.5 |
| chapel-cover | hollowwood/chapel | cover | 20.5 | 2.6 | 15.5 | -6.5 | -1.0 | 52.5 |
| chapel-sight | hollowwood/chapel | sight | 14.5 | 2.6 | 20.5 | -3.5 | -1.0 | 38.5 |

World values: `fm-lodge` cover (-75.5,66.6,-184.5) and sight (-67.5,72.6,-172.5), both to (-50.5,ground,-166.5) on the lodge lane; `hw-chapel` cover (76.5,66.6,104.5) to (49.5,ground,141.5), sight (70.5,66.6,109.5) to (52.5,ground,127.5) on the chapel lane.

**Briefs:**
- `frostmarch/lodge`: the hall (dark spruce and stone, snowy roofs) at x 0..16, an east door at z 9..11; the yard at x 17..26, z 0..18 inside a 2-high stone wall on its north, east and south sides; the gate at x 26, z 12..16 (the lodge lane leaves there) under a gatehouse at x 23..26, z 11..17 whose upper room has its floor at y 6 (feet 7) and an east window at x 26, y 8..9, z 14..15.
- `frostmarch/refuge`: a stone hut with a lean-to and a brazier; doors on all four sides (its lane starts at the centre).
- `frostmarch/signal_tower`: a stone tower with an inner stair to a parapet; the signal fire on netherrack, netherrack at y 19 and fire at y 20.
- `hollowwood/chapel`: a ruined deepslate chapel; the south door at x 5..9, y 1..4 (the chapel lane leaves there); in the south wall (z 22) a window at x 11..14, y 2..3 and solid wall at x 15..16 from y 1 to 6; inside, x 11..16, z 19..21, y 1..3 kept clear.
- `hollowwood/sawmill`: a log ramp and a stonecutter bench under a roof, open on the east (its lane leaves there).
- `hollowwood/logging_camp`: log stacks (the logs of the old pile), a hut and sawhorses; the old farm lane from (-107,-170) ends inside it.
- `hollowwood/moon_shrine`: calcite and polished deepslate, soul lanterns, an open court.

- [ ] **Step 1: Write the seven scripts** to the briefs.
- [ ] **Step 2: Export them**; the tester states what each studio shot shows; copy the files.
- [ ] **Step 3: Add the rows.** Expect `Preflight clean`.
- [ ] **Step 4: Check the signal fire hand-over:** `grep -n "signal_tower" src/main/java/gg/gravesandglory/MapDressing.java` shows the fire placed inside a loop over `Spec.STRUCTURES` matching the id, so with the row replaced it places nothing and the piece carries the fire. If the lookup is not a loop, guard it.
- [ ] **Step 5: Compile; run `landmarks,routes` with `GRAVES_PHASE3_MAPS=2,3`.** Expected every `p3-lm-*` id for the 29 rows, both budgets, both once-per-run ids, the suite line and the route check. The tester states what `lm-fm-lodge-ground`, `lm-fm-signal-tower-high`, `lm-hw-chapel-ground`, `lm-hw-logging-camp-ground` and both overviews show.
- [ ] **Step 6: Commit** `"Phase 3: Frostmarch and Hollowwood landmarks"`.

---

### Task 8b: Supply houses (five house pieces, spur roads, neutral flags)

The buildings of the supply-houses spec (`docs/superpowers/specs/2026-10-22-supply-houses-design.md` sections 3, 7.1, 7.2, 10 and 12.1); its rules (sections 4 to 9 and 11) are their own plan after the base merge. In Phase 3 every house stands inert: a white neutral flag, unlit braziers, no capture and no services. The suite is `supply` and its ids are `p3-house-*`, one letter away from Task 4's `p3-houses-*` (the camp's House ring): grep with the full prefix and never report one for the other. Runs follow `docs/HANDOFF.md`: at most two harness clients at once, 30 minutes each.

**Files:**
- Modify: `sheets/design.json` (`pieces` +5, `landmarks` +18, `landmarkLines` +10; new sheet `supplyHouses`, 18 rows)
- Modify: `tools/preflight.py` (rules for `supplyHouses` and for the house rows of `landmarks`)
- Create: scripts `supply/{rivergate,ironhold,frostmarch,hollowwood,kingsfield}` under `test-tools/resources/data/gravesandglory_studio/functions/`, and their exports under `src/main/resources/data/gravesandglory/structures/supply/`
- Modify: `src/main/java/gg/gravesandglory/CampaignMaps.java` (`select` lays the spur roads)
- Modify: `src/main/java/gg/gravesandglory/Arena.java` (`roadLevel` climbs to a raised house; `flag(ServerWorld,int,int,int,int)`; `finishBuild` raises the house flags)
- Modify: `src/main/java/gg/gravesandglory/Structures.java` (`hash` also reads the map's `supplyHouses` rows)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (layout `campaign-3` becomes `campaign-4` in the constructor, `cached()` and `built()`)
- Modify: `test-tools/java/gg/gravesandglory/MapRouteChecks.java` (house mode)
- Modify: `test-tools/java/gg/gravesandglory/Phase3Checks.java` (suite `supply`; the house entries in the reserved columns of `p3-lm-<id>-clear`; the walkthrough's house legs)

**Sheets.** `pieces` (one footprint for all five, spec 7.1; nothing solid above local y 6, the template keeps y 7 and 8 for the flag column):

| id | width | depth | height | palette | purpose |
|---|---|---|---|---|---|
| supply/rivergate | 31 | 37 | 9 | wood | supply house: granary and walled yard |
| supply/ironhold | 31 | 37 | 9 | stone | supply house: armoury and walled yard |
| supply/frostmarch | 31 | 37 | 9 | wood | supply house: ridge hut and walled yard |
| supply/hollowwood | 31 | 37 | 9 | stone | supply house: woodstore and walled yard |
| supply/kingsfield | 31 | 37 | 9 | stone | supply house: tithe barn and walled yard |

`landmarks`. The positions are the spec's (3.2), except the Ironhold nw/se pair, moved 15 blocks west and east (inside spec 3.3's 20) because `ih-house-3` stands 7 blocks from the nw footprint at (-125,-72). The north houses face south (rotation 0) and the south houses north (180), so each storehouse stands on the high side of its ridge or hill and each front gate faces the middle lane. A pair (nw with se, sw with ne) shares the mean of its two terrain heights (spec 3.2, rounded half up: Ironhold sw/ne (78+75)/2 gives 77), and 80 on Frostmarch (spec 3.3). The 18 rows go in one block right after `rg-house-0`, which stays the first sheet row: Task 6's landmark pass in `Arena.height` blends the rows in sheet order, so every level piece near a house is blended after it and keeps its own flat, and no level piece stands close enough (12 blocks, 18 for a tower) for its shoulder to reach into a house (preflight, Step 4).

| id | map | template | x | z | rotation | elevation | replaces | encloses |
|---|---|---|---|---|---|---|---|---|
| rg-supply-nw | 0 | supply/rivergate | -125 | -72 | 0 | 74 | - | - |
| rg-supply-sw | 0 | supply/rivergate | -125 | 72 | 180 | 74 | - | - |
| rg-supply-ne | 0 | supply/rivergate | 125 | -72 | 0 | 74 | - | - |
| rg-supply-se | 0 | supply/rivergate | 125 | 72 | 180 | 74 | - | - |
| ih-supply-nw | 1 | supply/ironhold | -140 | -72 | 0 | 76 | - | - |
| ih-supply-sw | 1 | supply/ironhold | -125 | 72 | 180 | 77 | - | - |
| ih-supply-ne | 1 | supply/ironhold | 125 | -72 | 0 | 77 | - | - |
| ih-supply-se | 1 | supply/ironhold | 140 | 72 | 180 | 76 | - | - |
| fm-supply-nw | 2 | supply/frostmarch | -125 | -72 | 0 | 80 | - | - |
| fm-supply-sw | 2 | supply/frostmarch | -125 | 72 | 180 | 80 | - | - |
| fm-supply-ne | 2 | supply/frostmarch | 125 | -72 | 0 | 80 | - | - |
| fm-supply-se | 2 | supply/frostmarch | 125 | 72 | 180 | 80 | - | - |
| hw-supply-nw | 3 | supply/hollowwood | -125 | -72 | 0 | 74 | - | - |
| hw-supply-sw | 3 | supply/hollowwood | -125 | 72 | 180 | 74 | - | - |
| hw-supply-ne | 3 | supply/hollowwood | 125 | -72 | 0 | 74 | - | - |
| hw-supply-se | 3 | supply/hollowwood | 125 | 72 | 180 | 74 | - | - |
| kf-supply-nw | 4 | supply/kingsfield | -125 | -72 | 0 | 76 | - | - |
| kf-supply-se | 4 | supply/kingsfield | 125 | 72 | 180 | 76 | - | - |

`landmarkLines` (the five pieces share one layout, so the local values repeat):

| id | template | kind | fromX | fromY | fromZ | toX | toY | toZ |
|---|---|---|---|---|---|---|---|---|
| supply-rivergate-cover | supply/rivergate | cover | 24.5 | 2.6 | 34.5 | 24.5 | -1.0 | 48.5 |
| supply-rivergate-sight | supply/rivergate | sight | 25.5 | 7.6 | 10.5 | 16.5 | -1.0 | 48.5 |
| supply-ironhold-cover | supply/ironhold | cover | 24.5 | 2.6 | 34.5 | 24.5 | -1.0 | 48.5 |
| supply-ironhold-sight | supply/ironhold | sight | 25.5 | 7.6 | 10.5 | 16.5 | -1.0 | 48.5 |
| supply-frostmarch-cover | supply/frostmarch | cover | 24.5 | 2.6 | 34.5 | 24.5 | -1.0 | 48.5 |
| supply-frostmarch-sight | supply/frostmarch | sight | 25.5 | 7.6 | 10.5 | 16.5 | -1.0 | 48.5 |
| supply-hollowwood-cover | supply/hollowwood | cover | 24.5 | 2.6 | 34.5 | 24.5 | -1.0 | 48.5 |
| supply-hollowwood-sight | supply/hollowwood | sight | 25.5 | 7.6 | 10.5 | 16.5 | -1.0 | 48.5 |
| supply-kingsfield-cover | supply/kingsfield | cover | 24.5 | 2.6 | 34.5 | 24.5 | -1.0 | 48.5 |
| supply-kingsfield-sight | supply/kingsfield | sight | 25.5 | 7.6 | 10.5 | 16.5 | -1.0 | 48.5 |

World values the checks must reproduce, for a house at (x, z) with elevation E: rotation 0 gives cover (x+9.5,E+2.6,z+16.5) to (x+9.5,ground,z+30.5) and sight (x+10.5,E+7.6,z-7.5) to (x+1.5,ground,z+30.5); rotation 180 gives cover (x-8.5,E+2.6,z-15.5) to (x-8.5,ground,z-29.5) and sight (x-9.5,E+7.6,z+8.5) to (x-0.5,ground,z-29.5). So `rg-supply-nw` cover (-115.5,76.6,-55.5) to (-115.5,ground,-41.5) and sight (-114.5,81.6,-79.5) to (-123.5,ground,-41.5) on its spur; `ih-supply-se` cover (131.5,78.6,56.5) to (131.5,ground,42.5) and sight (130.5,83.6,80.5) to (139.5,ground,42.5).

`supplyHouses` (columns `id, map, name, landmark, yardX, yardZ, spurX, spurZ, elevation`; ids and names from spec 3.4, each id equal to its `landmarks` row; the yard centre is the template's local (15,24), 6 blocks from the piece centre toward the front gate; the spur runs straight from the yard centre out of the front gate to the middle lane at z 0):

| id | map | name | landmark | yardX | yardZ | spurX | spurZ | elevation |
|---|---|---|---|---|---|---|---|---|
| rg-supply-nw | 0 | Westmill Granary | rg-supply-nw | -125 | -66 | -125 | 0 | 74 |
| rg-supply-sw | 0 | Westhill Barn | rg-supply-sw | -125 | 66 | -125 | 0 | 74 |
| rg-supply-ne | 0 | Eastmill Granary | rg-supply-ne | 125 | -66 | 125 | 0 | 74 |
| rg-supply-se | 0 | Easthill Barn | rg-supply-se | 125 | 66 | 125 | 0 | 74 |
| ih-supply-nw | 1 | West Armoury | ih-supply-nw | -140 | -66 | -140 | 0 | 76 |
| ih-supply-sw | 1 | West Quarters | ih-supply-sw | -125 | 66 | -125 | 0 | 77 |
| ih-supply-ne | 1 | East Armoury | ih-supply-ne | 125 | -66 | 125 | 0 | 77 |
| ih-supply-se | 1 | East Quarters | ih-supply-se | 140 | 66 | 140 | 0 | 76 |
| fm-supply-nw | 2 | West Ridge Hut | fm-supply-nw | -125 | -66 | -125 | 0 | 80 |
| fm-supply-sw | 2 | West Fur Store | fm-supply-sw | -125 | 66 | -125 | 0 | 80 |
| fm-supply-ne | 2 | East Ridge Hut | fm-supply-ne | 125 | -66 | 125 | 0 | 80 |
| fm-supply-se | 2 | East Fur Store | fm-supply-se | 125 | 66 | 125 | 0 | 80 |
| hw-supply-nw | 3 | West Woodstore | hw-supply-nw | -125 | -66 | -125 | 0 | 74 |
| hw-supply-sw | 3 | West Herb House | hw-supply-sw | -125 | 66 | -125 | 0 | 74 |
| hw-supply-ne | 3 | East Woodstore | hw-supply-ne | 125 | -66 | 125 | 0 | 74 |
| hw-supply-se | 3 | East Herb House | hw-supply-se | 125 | 66 | 125 | 0 | 74 |
| kf-supply-nw | 4 | West Tithe Barn | kf-supply-nw | -125 | -66 | -125 | 0 | 76 |
| kf-supply-se | 4 | East Tithe Barn | kf-supply-se | 125 | 66 | 125 | 0 | 76 |

World footprints, for checks: rotations 0 and 180 keep 31 by 37, so a house at (x, z) covers x-15..x+15 by z-18..z+18: x -140..-110 or 110..140 (Ironhold nw -155..-125, se 125..155) by z -90..-54 (north) or 54..90 (south). Front gates at x-4..x+4 on z -54 or 54; spur road cells within 3 of x. Nearest point to each yard: nw A on maps 0 to 3 (117, 128, 125 and 112 blocks), C on Kingsfield (141); sw and ne B (141); se C (117, 128, 125 and 112; Kingsfield's C at 141).

**Brief, the shared shape** (31x9x37, authored facing south; local coordinates from the minimum corner; the author's call beyond these):
- The storehouse fills the back third, x 0..30 by z 0..11, its front wall along z 11 with a door to the yard. Nothing solid stands above local y 6, so the pole Java raises (its lantern at y 7) tops the roof line (spec 7.1).
- The lookout: a platform on the storehouse roof, floor at y 5 over x 23..28, z 7..11 (feet 6), a parapet of bottom slabs at y 6 on its open sides, a ladder up from the yard at x 22, z 12, y 1..5; the eye point (25.5,7.6,10.5) stays clear.
- The yard, x 0..30 by z 12..36, walled 2 high (y 1..2) along x 0, x 30 and z 36. The front gate in the south wall at x 11..19: 9 wide, wider than the spur road's 7-cell band, open to the sky. The gate posts at x 10 and 20 carry the two braziers, `campfire[lit=false]` at (10,3,36) and (20,3,36). The side gate in the east wall at z 22..26: 5 wide, clear from y 1 to 4.
- The capture circle (spec 4): centre (15,0,24), radius 10, full blocks at y 0, the cells 9 to 10 blocks from the centre in a contrasting block so its edge reads; a 3x3 plinth at x 14..16, z 23..25, y 0. Structure void at y 1..8 where |x-15|<=3 and |z-24|<=3 (the flag column, as the outposts leave theirs). Nothing with a collision shape at y 1..2 within 10.5 of the centre, and clear ground from both gates to the circle (x 11..19 for z 35..36; z 22..26 for x 26..30).
- Outside the circle: crates and barrels, a supply cart, a forge corner (anvil, grindstone, smithing table, a weapon rack of blocks: `place` ignores entities, so no item frames or armour stands) and a covered corner of cots (beds under a lean-to). The cell (24,1..2,34), the cover line's eye, stays air.
- Every cell above y 0 that the piece does not build is written as `air`, except the flag column (structure void), so no grass, flower or dressing from the map's build survives in the yard or the gates (`p3-lm-<id>-clear`).
- The lines: the cover line runs from behind the south wall through it at x 24; the sight line runs from the lookout over the yard on the +x side of the flag column and out through the front gate opening along the spur, and nothing rises into it.
- Grim and weathered, Souls-like (Kort, 2026-10-22): sagging, uneven roof lines, patched and cracked walls, mixed worn materials, soot over the braziers; no clean box shapes. No cobwebs, powder snow or anything else that slows a walker in the yard, the gates or on the way up to the lookout.

**Dressings** (one per map on the shared shape, spec 7.1):
- `supply/rivergate` (Westmill and Eastmill Granary, Westhill and Easthill Barn): an oak frame on a cobble plinth under a hay roof, weathered spruce infill, mossy patches, coarse dirt and cobble in the yard.
- `supply/ironhold` (West and East Armoury, West and East Quarters): stone brick with cracked and mossy courses, iron bars in the windows, a deepslate tile roof, chains; the forge corner at its fullest.
- `supply/frostmarch` (West and East Ridge Hut, West and East Fur Store): dark spruce on a stone footing, snow on the roofs only, snow block and gravel in the yard; the north houses stand as a terrace cut into the ridge (spec 3.3).
- `supply/hollowwood` (West and East Woodstore, West and East Herb House): mossy cobblestone and spruce logs, moss on the roof, rooted dirt and podzol at the wall foot, soul lanterns.
- `supply/kingsfield` (West and East Tithe Barn): fieldstone (cobble, andesite, stone) under an oak tithe barn, wheat sheaves (hay bales) stacked in the yard, a gravel yard.

**Interfaces:**
- `supplyHouses` gives `Spec.SupplyHousesRow(String id,int map,String name,String landmark,int yardX,int yardZ,int spurX,int spurZ,int elevation)` and `Spec.SUPPLYHOUSES`, read by this task's code and by the rules plan (which adds its own `supply` sheet).
- `CampaignMaps.select`, before `roads=building` (the woodland loop after it keeps 10 blocks off every road): `road(h.yardX(),h.yardZ(),h.spurX(),h.spurZ(),4)` for each `supplyHouses` row with `map()==selected`. The near end lies inside the footprint, so `Walkability` files the spur's cells inside the house as `building`, never `through`, and the 9-wide front gate is wider than the road's band: `Walkability` is unchanged.
- `Arena.roadLevel(int x,int z)`: the point rule moves into `static double climb(double beyond,int elevation)` (`Y+(elevation-Y)*t*t*(3-2*t)` with `t=clamp(1-beyond/CLIMB,0,1)`) and `static boolean endsAt(CampaignMaps.Road r,int x,int z)`; a raised point keeps `beyond=hypot(x-p.x(),z-p.z())-(p.radius()+2)`. A raised house is a `supplyHouses` row of the selected map with `elevation()>Y` whose yard centre is an end of the nearest road: `beyond=hypot(dx,dz)` with `dx=max(0,|x-L.x()|-L.width()/2.0-2)` and the same for z, `L` its landmark row in `Spec.STRUCTURES` (the flat of Task 6's landmark pass). The early return when nothing is raised also counts the houses; the level is the largest of the floor and every climb; `CLIMB` stays 48.
- `Arena.flag(ServerWorld w,int x,int y,int z,int team)`; `flag(w,p,team)` becomes `flag(w,p.x(),p.elevation(),p.z(),team)`. `finishBuild`, after the points loop: `flag(w,h.yardX(),h.elevation(),h.yardZ(),-1)` for each house of the selected map. `Arena.finish` runs before `Structures.queueLandmarks` and the piece leaves the flag column void, so the flag stands; the rules plan recolours it with the same call.
- The braziers are the template's `campfire[lit=false]` at local (10,3,36) and (20,3,36), in the world `Structures.toWorld(size,x,elevation,z,rotation,10,3,36)` and `(...,20,3,36)`; nothing in this task changes them.
- `Structures.hash(int map)` also reads the map's `supplyHouses` rows; the marker becomes `{"layout":"campaign-4","map":N,"landmarks":"<hash(N)>"}` (the road code changed), so every existing world rebuilds its map once.
- Harness: the reserved columns that Task 6's `p3-lm-<id>-clear` skips gain, for the `supplyHouses` row whose `landmark` is the row's id, the cells with |x-yardX|<=3 and |z-yardZ|<=3 from elevation+1 to elevation+8 (the shape of an enclosed point's flag column); the check is otherwise unchanged.
- Harness: `MapRouteChecks(MinecraftClient c,int[] maps,boolean houses)` (the two-argument constructor keeps the crossings) and `final Map<Integer,Integer> reached`. In house mode each map's goals, in sheet order of its `supplyHouses` rows, are `house-<id>-base`, from the spawn of the faction whose half holds the house (`Spec.FACTIONS[yardX<0?0:1]`, `spawnX`, `spawnZ`), and `house-<id>-point`, from 12 blocks from the centre of the point nearest the yard, toward the yard; the goal is the yard centre. Each map starts with `map-<n>-setup`, as the crossings do. `prepare` puts the twenty knights in a 5 by 4 block about the start (`standingCell`, no x limit), order `move`, formation `block`, target `(yardX+0.5,elevation+1,yardZ+0.5)`, squad yaw toward the yard, and the spectator 16 blocks from the yard toward the start, 18 up, looking at the yard (the units' own chunk tickets keep the march loaded). A goal passes when all 20 living knights stand within 10 blocks (horizontal) of the yard centre with `getY()>elevation+0.5`, within 3600 ticks; it reports `map-<n>-house-<id>-<base|point>` (`reached k/20` and the elapsed seconds), saves `route-map-<n>-house-<id>-<base|point>.png`, and a pass runs `reached.merge(map,1,Integer::sum)`. The closing line is `all-house-navigation`, so `all-map-navigation` keeps meaning the crossings.
- Harness: in `Walkthrough.buildLegs`, a spur whose building is a house (the `landmark` of a `supplyHouses` row) becomes the leg `house-<id>`, walked from its lane end through the front gate to 4 blocks short of the yard centre, 2 blocks right of the centre line, with the pictures `house-<id>-spur` (2 blocks in, toward the gate), `house-<id>-gate` (10 blocks before the gate, toward the flag) and `house-<id>-yard` (at the end, past the flag at the storehouse); `GRAVES_WALK_ONLY=supply` selects them.
- Consumes `Structures.toWorld/footprint/place/queueLandmarks/hash` (Tasks 1 and 6), Task 6's landmark rows, height pass and `p3-lm-*` checks, `Walkability.scan`, `MapRouteChecks` and the `walkthrough` suite (Task 2), `Phase3Checks.report/shot/view`.

- [ ] **Step 1: Write the five scripts** (Opus 5.5 max, as Global Constraints assign piece scripts) to the shared brief and their dressings, each headed `# size 31 9 37`. Write `supply/rivergate` first and take it through Step 2 alone, so the shared shape is proven once before the other four reuse it.
- [ ] **Step 2: Export them** (`GRAVES_STUDIO_PIECES=supply/rivergate,supply/ironhold,supply/frostmarch,supply/hollowwood,supply/kingsfield`). Expected five `pass p3-export-supply-<map>`. The tester states what each `p3-studio-supply-<map>.png` shows (storehouse and lookout, the walled yard with both gates and the ring of the circle, the map's materials, nothing above the flag's height); a piece that reads as a plain box or a generic shed goes back to Step 1. Copy the five files to `src/main/resources/data/gravesandglory/structures/supply/`.
- [ ] **Step 3: Prove the climb on Kingsfield's outposts first** (Sonnet 5.5 max): move the point rule of `roadLevel` into `climb` and `endsAt` as in Interfaces, with no house branch yet. Compile; run `routes`. Expected the route check in full, with `pass p3-walk-map4` showing the same six counts as the last Task 8 run and `pass p3-routes-map4` (3 crossings): the outposts' roads climb as before.
- [ ] **Step 4: Add the rows and their preflight rules.** `supplyHouses`: ids unique and of the form `<prefix>-supply-<nw|sw|ne|se>` with the map's prefix; `map` 0..4; `landmark` names a `landmarks` row of the same map whose template starts `supply/` and whose elevation equals the row's and whose rotation is 0 or 180; (yardX, yardZ) is that piece's local (15,24) in the world, so inside its footprint (x, z+6 at rotation 0; x, z-6 at 180); (spurX, spurZ) lies outside the footprint, within |x|<=350 and |z|<=250; names unique per map and at most 20 characters; four rows on each of maps 0 to 3 and two on map 4; every row has a partner on its map at (-yardX, -yardZ) with the same elevation and the spur end (-spurX, -spurZ). `landmarks` (gaps between footprints are the larger of the x and z gaps): the five `supply/` pieces have one size; no other footprint of the map lies within `12+2*(e-64)` blocks of a house's footprint, `e` the other row's elevation (its shoulder plus 2); every other row of the map whose footprint lies within `12+2*(E-64)` blocks of a house's footprint, `E` the house's elevation, comes after the house in the sheet. Expect `Preflight clean`. Then, one at a time: set `rg-supply-se`'s `yardZ` to 67 and expect an error naming `rg-supply-se`; move the 18 house rows to the end of the sheet and expect an error naming `rg-supply-nw` and `rg-house-2`. Restore both.
- [ ] **Step 5: Write suite `supply`, the house mode of `MapRouteChecks`, the reserved columns and the walkthrough's house legs** (Sonnet 5.5 max). Suite `supply` covers the maps in `GRAVES_PHASE3_MAPS` (default all five); on each it switches and waits for `ready`, then per `supplyHouses` row of the map:
  - `p3-house-<id>-neutral`: `oak_fence` at (yardX, elevation+1..elevation+6, yardZ), `sea_lantern` at elevation+7, `white_wool` at (yardX+1..yardX+3, elevation+4..elevation+5, yardZ); a `campfire` with `lit=false` at both brazier cells.
  - `p3-house-<id>-yard`: every cell with (x-yardX)^2+(z-yardZ)^2<=100 has a collision shape at `elevation` and none at elevation+1 or +2, the pole cell excepted (flat and open); a flood fill over such cells inside the footprint, 4 neighbours, from beside the pole reaches the front gate opening (local z 36, x 11..19) and the side gate opening (local x 30, z 22..26), and the cell just outside each (local (15,0,37) and (31,0,24)) is such a cell too. `why` carries the bad cells and both results.
  - `p3-house-<id>-spur`: along the spur's centre line, one cell per block from (spurX, spurZ) to the front gate's outer cell (local (15,0,37)), every cell is a road cell, nothing with a collision shape stands at `Arena.ground`+1 or +2, `Arena.ground` changes by at most 1 from one cell to the next, and it equals `elevation` at the gate. `why` carries the cell count, the largest step and the ground at both ends.
  - Screenshots (`view`; local points go through `Structures.toWorld`, so their y is elevation plus the local y; where no y is given the eye is at `Arena.ground`+2.6): `<id>-spur` from the spur's lane end to the gate (local (15.5,3.0,36.5)); `<id>-gate` from local (15.5,47.5) to the yard centre at elevation+4; `<id>-yard` from local (15.5,2.6,33.5) to the storehouse front (local (15.5,3.0,11.5)); `<id>-lookout` from the sight line's start to its end; `<id>-point` from 12 blocks from the nearest point's centre toward the yard, to the yard centre at elevation+4.
  - Then a `MapRouteChecks` in house mode over this one map, ticked until `done`: `p3-house-routes-map<N>` passes when `reached.get(N)` is twice the map's house rows (8 on maps 0 to 3, 4 on Kingsfield); `why` lists each goal's count.
  Compile, then run `supply` with `GRAVES_PHASE3_MAPS=0` before Step 6: expected `fail p3-house-rg-supply-<nw|sw|ne|se>-neutral` (no flag) and `-spur` (no road) on all four; record the other results.
- [ ] **Step 6: Implement** the spur roads in `select`, the house branch of `roadLevel`, `Arena.flag(w,x,y,z,team)` and the house flags in `finishBuild`, `hash` with the `supplyHouses` rows, and the `campaign-4` marker (Sonnet 5.5 max).
- [ ] **Step 7: Compile; run**, a fresh root per launch:
  - `landmarks` in three launches, `GRAVES_PHASE3_MAPS=0,4`, `1` and `2,3` (the groups of Tasks 6 to 8). Expected, over the three, every `p3-lm-*` id for all 98 rows (each house row with `-placed`, `-fits`, `-clear`, `-roads`, `-ring`, `-cover` and `-sight`) and the five budgets; in each launch both once-per-run ids and `pass p3-landmarks-suite`.
  - `supply` once per map, `GRAVES_PHASE3_MAPS=0` to `4` (about 12 minutes for a map with four houses: eight marches of up to 3600 ticks; two maps would come close to the 30-minute limit). Expected, per map, `pass p3-house-<id>-neutral`, `-yard` and `-spur` for each of its houses, `pass p3-house-routes-map<N>`, the route harness's lines (`pass map-<n>-setup`, `pass map-<n>-house-<id>-base` and `-point` for each house, `pass all-house-navigation`) and `pass p3-supply-suite`. Note each launch's wall time; Task 9 keeps one map per launch.
  - `routes`. Expected the route check in full: 15 crossings at 20/20 and `p3-walk-map<N>` with road 0 and through 0, so the spurs are clear.
  The tester states what `lm-rg-supply-nw-ground`, `lm-ih-supply-nw-high`, `lm-fm-supply-nw-high` (the terrace cut into the ridge), `lm-kf-supply-nw-ground` and each map's overview show, and for one house per map the five views: the spur climbing to the gate with the white flag readable from the lane (about 60 blocks), the gate between its two unlit braziers, the yard with its ring and the flag above the roof line, the lookout over the spur, the house from its nearest point.
- [ ] **Step 8: Commit** by path (the files above) with `git -c user.name=Claude -c user.email=noreply@anthropic.com commit -F <msg file under work/p1/>` and the `docs/HANDOFF.md` trailer: `"Phase 3: supply houses: five house pieces, spur roads up to their yards, neutral flags"`.
- [ ] **Step 9: Walkthrough gate** (after the task review, a Sonnet 5.5 max tester): `walkthrough` with `GRAVES_WALK_ONLY=supply`, one map per launch (`GRAVES_PHASE3_MAPS=<N>`). Expected `pass p3-wt<N>-leg-house-<id>` for each house of the map, `pass` for `p3-wt<N>-stuck`, `-foreign`, `-river` and `-floating`, and `pass p3-walkthrough-suite`. The tester grades each map's houses 1 to 10 against `work/ui-concepts/GRADING-RUBRIC.md` adapted to world pieces, from the walkthrough pictures and the five `supply` views per house (spur, gate, yard, lookout, from the nearest point). Below 9 on any map is a fix round: a piece goes back to Step 1, or a pair moves up to 20 blocks with its half-turn symmetry kept (spec 3.3; on Frostmarch a terrace under 9 moves down-slope first), its rows and spur moving with it and Steps 4 and 7 run again. Each fix round commits by path as in Step 8. Nothing ships below 9.

---

### Task 9: Final regression and Kort's screenshot gate

**Files:**
- Create: `docs/PHASE-3-SCREENSHOTS.md`
- Modify: `docs/PROJECT-STATE.md`, `docs/PARKED.md` (append only)

- [ ] **Step 1: Full Phase 3 run,** in launches of at most 30 minutes, at most two harness clients at once (`docs/HANDOFF.md`), each with a fresh `GRAVES_ROADMAP_TEST_ROOT`: suites `pipeline,studio,camp,houses,yard` with `GRAVES_PHASE3_MAPS` unset; `landmarks` once per map (`GRAVES_PHASE3_MAPS=0`, then `1` to `4`); `supply` once per map (as in Task 8b, Step 7); `routes`. Expected every `p3-*` line `pass`, the eight suite lines (`landmarks` and `supply` in each of their launches), and `p3-fresh-<group>-<name>` for all 39 scripts (the marker, 11 camp pieces, 22 landmark pieces, 5 supply houses). Before the first launch, empty `work/p3/task9-shots/`, `work/p3/task9-roots.txt` and `work/p3/task9-manifest.txt`. After each launch, copy its `run/screenshots/*.png` into `work/p3/task9-shots/` with `cp -n` (a name it skips is a frame two launches both made: report it, never overwrite), append its root to `work/p3/task9-roots.txt`, and append one manifest line per copied image: file name, test root, `GRAVES_PHASE3_MAPS` (`all` when unset) and `git rev-parse --short HEAD`. The sheets: `python -c "import json;d=json.load(open('sheets/design.json'));print({k:len(d[k]) for k in ('pieces','landmarks','landmarkLines','supplyHouses')},[sum(r['map']==m for r in d['landmarks']) for m in range(5)])"` prints `{'pieces': 38, 'landmarks': 98, 'landmarkLines': 22, 'supplyHouses': 18} [18, 25, 18, 19, 18]`.
- [ ] **Step 2: Earlier suites:** `-PcampaignChecks` with no Phase 3 variable ends with `pass roadmap-suite`; `GRAVES_CAMP_CHECKS=true` passes; `GRAVES_PHASE1_CHECKS=combat,morale,reinforce,bots,tutorial,report,ui` passes; `-PselfTest` (same init script, its own test root) passes every id except those `docs/PROJECT-STATE.md` lists as stale. Each run uses a fresh `GRAVES_ROADMAP_TEST_ROOT`.
- [ ] **Step 3: Release build, not uploaded:** `"$G" build --offline -x test`, then `python tools/package.py` (its `validate_player_jar` leak check passes; the zip stays in `dist/`); `J=$(ls build/libs/graves-and-glory-*.jar | grep -v -- -sources); unzip -l "$J" | grep -c "data/gravesandglory/structures/.*\.nbt"` prints `38` (33 and the five supply houses; the marker stays in the harness jar).
- [ ] **Step 4: Contact sheets** from the Step 1 screenshots in `work/p3/task9-shots/` (frames scaled to 480x270; camp 5x4; each map 5x4, with 13, 17, 15, 17 and 15 frames on maps 0 to 4; each map's houses 5x4, the five `supply` views of each house, 20 frames on maps 0 to 3 and 10 on Kingsfield):
```bash
FF=/c/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg; S="$PWD/work/p3/task9-shots"; O="$PWD/work/p3/task9-sheets"; mkdir -p "$O"
sheet(){ n=$1; t=$2; shift 2; for f in "$@"; do echo "file '$(cygpath -m "$f")'"; done > "$O/$n.txt"; "$FF" -y -f concat -safe 0 -i "$O/$n.txt" -vf "scale=480:270,tile=$t" -frames:v 1 "$O/$n.png"; }
sheet camp 5x4 "$S"/p3-camp-*.png
for m in 0:rg 1:ih 2:fm 3:hw 4:kf; do sheet "map${m%%:*}" 5x4 "$S/p3-lm-map${m%%:*}-overview.png" "$S"/p3-lm-${m##*:}-*.png; sheet "houses${m%%:*}" 5x4 "$S"/p3-${m##*:}-supply-*.png; done
ls "$O"/*.png
```
Expected eleven sheets; the tester states what each shows.
- [ ] **Step 5: Append to `docs/PARKED.md`:** "Bases' barracks and stores road clips (2026-10-17, Phase 3; Question 6, for Phase 4)"; "`ironhold/gate_intact` for Siege (Phase 4)"; "In-world banner cosmetics on the command tent's standard pole (Phase 3 non-goal)"; "Pixel House glyphs beside the CampDisplays labels (UI pass hook)"; "Supply house rules (`docs/superpowers/specs/2026-10-22-supply-houses-design.md` sections 4 to 9 and 11): their own plan, written and run after the base merge, before 0.5.0; until then the houses are inert buildings with white (neutral) flags and unlit braziers (Phase 3 Task 8b)".
- [ ] **Step 6: Write `docs/PHASE-3-SCREENSHOTS.md`:** one section per sheet with its path and three yes/no boxes (reads as built, not generated; fits its House or map; nothing floats, clips or blocks a road, except the bases' barracks and stores, whose road clips are parked in Step 5), each `houses<N>` section (spur, gate, yard, lookout, from the nearest point) with a note that its flags are white and its braziers out because the houses stay inert until the supply-house rules plan (Step 5); then Kort's walk-through: a fresh world arrives at the yard, START battle 1, back at camp, up the ridge to the war table, the five Houses, out through the War Gates, then one skirmish each on Ironhold and Kingsfield, and a short walk on each of Rivergate, Frostmarch (up and down its terrace) and Hollowwood past their landmarks and supply houses, looking for awkward terrain and buildings that sit wrong. The gate passes only on Kort's word.
- [ ] **Step 7: Update `docs/PROJECT-STATE.md`:** Phase 3 built and waiting on the screenshot gate, with the test roots of Steps 1 and 2.
- [ ] **Step 8: Commit** `"Phase 3: regression, contact sheets and the screenshot gate checklist"`. No merge, push or upload; `mod_version` stays the controller's call.

---

## Self-review

**VISION section 9 to tasks:** camp ridge, commander's tent and war table: Task 3. House tents in a half-ring with banners and live troop displays: Task 4. Graveborn graveyard shrine: Task 4. Training yard that is the tutorial: Task 5. War Gates facing the map: Task 3. "Built once as a structure file, placed by the mod": Task 1's pipeline, used from Task 3; the camp is twelve pieces on procedural terrain rather than one file, so every piece stays editable in a vanilla structure block (48 limit; spec section 8). Landmarks with cover and sight lines: mill and outposts Task 6, gate and keep Task 7, lodge and chapel Task 8, the kit in Tasks 6 to 8. Road blockers: railings, deck walls, the Ironhold breach and gate, the log pile and the building moves in Task 2, the hay line in Task 7, the bases' clips parked (Question 6). New maps, modes, the pixel UI (one-line hook in Global Constraints) and troop skins (Phase 2) are out of scope.

**Order changes from the brief:** the road blockers moved to Task 2, so every landmark task is measured against a zero baseline; the War Gates sit in Task 3, because retiring the courtyard removes today's pads; the graveyard joins the tents in Task 4, because it is the Graveborn recruit station and shares the display code. Every build task (2 to 8) ends with named screenshots and the `routes` suite.

**Names used across tasks:** `Structures.place/template/rotation/toWorld/footprint/queueLandmarks/step/clearQueue/queued/hash/salt/LOG` and `Placed`; `StructureStudio.build/save/export/fresh/dump/ORIGIN`; `Phase3Checks.report/shot/view`; `Walkability.scan/Scan`; `MapRouteChecks.crossed`; `Arena.clearDecks/tree`; `CampWorld.TAG/Spot/spawn/inCamp/where/station/warTable/tableCell/text/slot/yaw/displays/lessons/dummies`; `CampDisplays.lastDrawn/lastTexts`; `CampaignMaps.padCenter`; `Campaign.leash`; `Battle.placing`; sheets `pieces`, `landmarks`, `landmarkLines`, `camp`, `campPieces`, `campStations`, `campLessons`.

**Counts:** 98 landmark rows (18, 25, 18, 19 and 18 on maps 0 to 4), 22 `landmarkLines` rows, 38 `pieces` rows, 18 `supplyHouses` rows, 39 scripts (the marker, 11 camp, 22 landmark, 5 supply houses), 38 shipped structure files.
