# Task 1 report: structure pipeline, placer, test marker and the Phase 3 harness

Status: DONE_WITH_CONCERNS
Commit: c2d6699 (one commit, branch phase-3-built-world, on top of b50dcb4; 13 files, +779 -3). Nothing pushed.
Tests: every pipeline and studio check passes in the real client (16 pass lines, 0 fail lines in the final run); preflight clean; compile clean.

Evidence roots (all under C:/Users/kortt/Melty/bannerbound-p3/work/, git-ignored):
- p3/pipeline-151849: the final run, suites pipeline,studio. evidence/local-checks.txt, run/logs/latest.log, run/screenshots/p3-pipeline-marker.png and p3-pipeline-marker-north.png.
- p3/studio-151606: the Step 8 export run. studio-out/test/p3_marker.nbt, run/screenshots/p3-studio-test-p3_marker.png.
- p3/studio-150911: the first export attempt, which hung on the first-run accessibility screen (see Concerns, item 1). I killed its client (the Minecraft java process of that root only). It is kept as evidence.
- p1/red-run.log (Step 3 red run), p1/baseline-compile.log, p1/launch.sh (the launch wrapper I used), p1/winshot.ps1 and p1/stall-window.png (the window capture of the hang).

## What was built, per file

- .gitattributes: appended `*.nbt binary` and `*.mcfunction text eol=lf`.
- src/main/java/gg/gravesandglory/Structures.java (new, 137 lines, server side, no client imports). Exactly the brief's interface: NS, `record Placed(id,blocks,nanos,box,missing)`, `LOG` (last 256), `template(w,id)`, `rotation(deg)`, pure `toWorld` (block and double forms) and `footprint`, and `place(w,id,cx,y,cz,rotation)`. The template is read with `server.getResourceManager().getResource(gravesandglory:structures/<id>.nbt)` and `NbtIo.readCompressed`, built with `createTemplate(nbt)`, cached by id; the world's generated/ folder is never read. The cache clears on END_DATA_PACK_RELOAD and SERVER_STOPPED (registered in a static initializer). Placement uses StructurePlacementData with rotation, mirror NONE, ignore entities, no neighbour updates, IGNORE_STRUCTURE_BLOCKS, position = pivot P=((sx-1)/2,0,(sz-1)/2), origin = centre - P, flag NOTIFY_LISTENERS. A missing file prints `[Graves and Glory] structure missing: <id>` and returns missing=true with a one-block box; a bad rotation is the only thing that throws (IllegalArgumentException). Small extras: public `read(server,id)` (the raw NBT, used by the studio) and public `clearCache()`.
- src/main/java/gg/gravesandglory/Campaign.java: field `boolean leash=true;` and `leash&&` in front of the two lobby leashes (the not-running branch and the unseated-players line).
- src/main/java/gg/gravesandglory/GravesAndGlory.java: the build-tick leash is `if(campaign.leash&&(p.getZ()>-275||p.getY()<63))campaign.toLobby(p);`.
- test-tools/java/gg/gravesandglory/StructureStudio.java (new, 257 lines, ModInitializer, server-safe imports only). ORIGIN (1000,64,1000), `scripts(server)`, `size`, `build`, `save`, `export`, `stale`/`fresh`, `dump`, `force`/`release` (own set of forced chunks), `run` (executes a script text), and the commands /studio build|save|dump <id> and /studio place <id> <rotation> (at the block under the player's feet). Build sets the box grown by 2 to air and the box to structure_void in Java, then runs each line as `s.getCommandSource().withPosition(Vec3d.of(ORIGIN)).withLevel(4).withSilent()`; a failing line fails the piece naming its line number. Save is `saveFromWorld(world,ORIGIN,size,false,STRUCTURE_VOID)` + `writeNbt`. `dump` writes the size header, one setblock per non-void block with x-runs merged into fill, block entity SNBT kept (id/x/y/z stripped), air written.
- test-tools/java/gg/gravesandglory/Phase3Checks.java (new, 321 lines, client entrypoint): the scaffold (Suite with step/limit/cleanup, SUITES map one put per line, report, shot, server, view, finish, start 40 ticks after the front menu via startCamp(c,false)) and the two suites. `Pipeline`: places the marker at (1000,64,1100) rot 0, (1010) rot 90, (1020) rot 180, (1030) rot 270 and a missing piece at (1000,64,1120), then runs the eight checks of the brief plus p3-to-world, then two screenshots. `StudioChecks`: export or verify per script, then p3-studio-roundtrip.
- test-tools/resources/data/gravesandglory_studio/functions/test/p3_marker.mcfunction (new): the brief's text, byte for byte.
- test-tools/resources/data/gravesandglory/structures/test/p3_marker.nbt (new, 428 bytes, 31 blocks, DataVersion 3465): the Step 8 export, copied from p3/studio-151606/studio-out/test/p3_marker.nbt (same sha256 ef51eefe...dd29).
- test-tools/isolated-checks.gradle: five systemProperty lines (phase3Checks, phase3Maps, studioPieces, studioMode, studioOut) added between the frontHost and phase1Checks lines.
- test-tools/resources/fabric.mod.json: StructureStudio after testing.LocalIntegration in main; Phase3Checks after CampNetworkChecks in client. The test-tools mixin config and the TitleDemos entrypoint are untouched.
- test-tools/java/gg/gravesandglory/CampaignChecks.java: one new first line in onInitializeClient, the early return when gravesandglory.phase3Checks is non-empty.
- tools/release_config.py: one new tuple line after the first `forbidden = (` line: Phase3Checks, StructureStudio, Walkability, gravesandglory_studio, p3_marker.
- tools/preflight.py (+42 lines, after `errors=[]`/`cells=0`): a small NBT reader (gzip, walks the root compound, skips every unread tag) and the rule for every .nbt under src/main/resources/data/gravesandglory/structures and test-tools/resources/data/gravesandglory/structures: id must match [a-z0-9_]+(/[a-z0-9_]+)+, a script with the same id must exist under test-tools/resources/data/gravesandglory_studio/functions, DataVersion must be 3465, an unreadable file is an error.

## Deviations from the brief, and why

1. Step 11 `git add -A` was replaced by `git add` of the 13 files by explicit path (your instruction). Spec.java and sheets/preflight.json show as modified in the worktree after every build only because tools/preflight.py rewrites them as CRLF working copies (no content difference); I did not add them and restored both with `git checkout --` after the commit. The tree is clean at c2d6699.
2. The Step 3 red run failed on `leash` (3 errors) as well as on `Structures` (24 errors), because the harness `view` helper turns the leash off and the brief puts the Campaign.leash guard in Step 5. Details below.
3. One check more than the brief: `p3-to-world`. It proves the pure functions the client will call (`toWorld` in both forms, `footprint`) and `Placed.box` against the blocks actually found in the world, for the marker at 0, 90, 180 and 270 degrees. The brief places the marker at rotation 0 and 90 only; I also place it at 180 (centre 1020,64,1100) and 270 (centre 1030,64,1100), so this check and p3-no-entities (which now looks in all four boxes) have all four rotations to read.
4. One screenshot more: `p3-pipeline-marker-north.png`, from (1015,76,1080) looking south at (1015,66,1100). The north-facing banner of the unrotated marker is on the side the prescribed south camera cannot see, so the banner (and the 180/270 placements) are only visible in this picture.
5. `view(...)`: x,y,z is the camera EYE position (feet = y minus the standing eye height), because `MapTour.look` computes yaw and pitch from the eye. It is poll style: at least 40 ticks, at most 200, until the camera is within a block, the client chunk is loaded and terrain rendering is complete; it prints a `[p3] note view <name> taken after N ticks, camera there=<bool>` line, takes the shot, restores the leash. The brief says "40 ticks for chunks"; this waits for the actual condition.
6. The studio validates scripts beyond the brief: line 1 must be `# size sx sy sz` (odd sx and sz up to 48, sy 1 to 32), only `#` comments, blank lines, `fill` and `setblock` are accepted, and every `~` offset must stay inside the size. Anything else fails the piece with its line number.
7. `p3-studio-roundtrip` compares with the shipped file when there is one (the brief); before the first export there is none, so it compares with a fresh build of the script (the first export run said "equals the fresh export of the script (no shipped file yet)", the final run "equals the shipped file").
8. `Structures` clears its cache on END_DATA_PACK_RELOAD (brief) and also on SERVER_STOPPED, registered in a static initializer so nothing read for an earlier server can outlive it.
9. Preflight has two more error kinds than the brief: an id that is not `<group>/<name>` in [a-z0-9_/], and a structure file that cannot be read as gzip NBT. Both name the piece.
10. Phase3Checks closes the first-run accessibility screen itself (one line before the start condition). Not in the brief; see Concerns, item 1.
11. Phase3Checks.java and StructureStudio.java were written with CRLF working copies; I converted both to LF before staging (the repo says `*.java text eol=lf`), so `git ls-files --eol` shows i/lf w/lf for every file of the commit except .gitattributes (CRLF working copy, LF index, as before).
12. Phase 2 merge hygiene: in the four shared harness files I only added lines in place and never reordered or reformatted. The isolated-checks.gradle lines sit between the frontHost and phase1Checks lines; the fabric.mod.json entries sit after LocalIntegration (main) and after CampNetworkChecks (client); the CampaignChecks line is the new first line of onInitializeClient; the release_config.py line is a new tuple line directly after the first `forbidden = (` line.

## Red run (Step 3), before Structures and the studio existed

Command: the compile line of the instructions (`compileJava compileLocalTestTools`) with Phase3Checks and the marker script in place. Log: p1/red-run.log. Result: `BUILD FAILED in 8s`, exit 1, 27 errors: 18 `cannot find symbol` (15 `symbol: variable Structures`, 3 `symbol: variable leash`) and 9 `package Structures does not exist`. First lines:

    Preflight clean: 1261 filled cells; every row generates a Java record. Runtime verification pending.
    test-tools/java/gg/gravesandglory/Phase3Checks.java:71: error: cannot find symbol
        server(c,b->{b.campaign.leash=false;var p=...
        symbol:   variable leash
        location: variable campaign of type Campaign
    ...Phase3Checks.java:142: error: cannot find symbol
        static BlockPos cell(...){return Structures.toWorld(SIZE,cx,Y,Z,deg,lx,ly,lz);}
        symbol:   variable Structures
        location: class Pipeline
    ...Phase3Checks.java:154: error: package Structures does not exist
        Structures.Placed p0=Structures.place(w,MARKER,1000,Y,Z,0);

## Preflight (Step 9)

    $ python tools/preflight.py
    Preflight clean: 1261 filled cells; every row generates a Java record. Runtime verification pending.   (exit 0)

Rename test (script moved aside, then restored):

    structure test/p3_marker: no script test-tools/resources/data/gravesandglory_studio/functions/test/p3_marker.mcfunction   (exit 1)
    Preflight clean: ...   (exit 0 after the rename back)

I also ran the other rules once with two throw-away files (deleted again): a copy with DataVersion 3463 gave `structure test/zz_bad: DataVersion 3463, expected 3465`; a file named Bad-Name.nbt holding text gave the id error, the no-script error and `cannot read the NBT (Not a gzipped file ...)`. The compile line prints the same `Preflight clean` (it runs preflight first).

## In-game results

Runs (one client at a time; before each launch no java process had a window title, only gradle daemons; each client was closed when its suite ended, runs took 1m11s and 1m26s):
- Step 8 export: `GRAVES_PHASE3_CHECKS=studio GRAVES_STUDIO_PIECES=test/p3_marker GRAVES_STUDIO_MODE=export`, root p3/studio-151606.
- Step 10: `GRAVES_PHASE3_CHECKS=pipeline,studio`, root p3/pipeline-151849. Both used the plan's recipe (absolute GRAVES_ROADMAP_TEST_ROOT, `runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir <root>/run"`) with no hand-seeded options.txt.

Step 8, `grep -E "^(pass|fail) "` (p3/studio-151606/evidence/local-checks.txt):

    pass mod-loads
    pass client-compatibility
    pass arena-ready
    pass p3-export-test-p3_marker
    pass p3-studio-roundtrip
    pass p3-studio-suite

    [p3] p3-export-test-p3_marker=true wrote .../studio-151606/studio-out/test/p3_marker.nbt (428 bytes), size 5, 5, 5 equals the header
    [p3] p3-studio-roundtrip=true the dump of test/p3_marker (12 lines), built and saved, equals the fresh export of the script (no shipped file yet)

Step 10, `grep -E "^(pass|fail) "` (p3/pipeline-151849/evidence/local-checks.txt): 16 pass lines, 0 fail lines:

    pass mod-loads
    pass client-compatibility
    pass arena-ready
    pass p3-template-loads
    pass p3-place-exact
    pass p3-place-rotated
    pass p3-void-keeps-ground
    pass p3-block-entity
    pass p3-no-entities
    pass p3-missing-template
    pass p3-no-generated-override
    pass p3-to-world
    pass p3-pipeline-suite
    pass p3-fresh-test-p3_marker
    pass p3-studio-roundtrip
    pass p3-studio-suite

`grep "[p3]" run/logs/latest.log` (the log prefix removed):

    [p3] p3-template-loads=true template test/p3_marker read from the mod resources, size 5, 5, 5
    [p3] note placed test/p3_marker blocks=31 box=BlockBox{minX=998, minY=64, minZ=1098, maxX=1002, maxY=68, maxZ=1102} in 9.68 ms
    [p3] note placed test/p3_marker blocks=31 box=BlockBox{minX=1008, minY=64, minZ=1098, maxX=1012, maxY=68, maxZ=1102} in 0.69 ms
    [p3] note placed test/p3_marker blocks=31 box=BlockBox{minX=1018, minY=64, minZ=1098, maxX=1022, maxY=68, maxZ=1102} in 0.34 ms
    [p3] note placed test/p3_marker blocks=31 box=BlockBox{minX=1028, minY=64, minZ=1098, maxX=1032, maxY=68, maxZ=1102} in 1.42 ms
    [p3] p3-place-exact=true floor 25/25 polished_andesite, pillar stone_bricks y65-67, lantern y68, oak_stairs facing south at (1000,65,1102), white_wall_banner facing north at (1000,66,1099), box BlockBox{minX=998, minY=64, minZ=1098, maxX=1002, maxY=68, maxZ=1102}, blocks=31
    [p3] p3-place-rotated=true at 90 degrees: pillar at (1010,65..67,1100), oak_stairs facing west at (1008,65,1100), white_wall_banner facing east at (1011,66,1100), box BlockBox{minX=1008, minY=64, minZ=1098, maxX=1012, maxY=68, maxZ=1102}
    [p3] p3-void-keeps-ground=true the hay_block set at (1001,65,1101), local (3,1,3) which is structure void, is hay_block after placement
    [p3] p3-block-entity=true the banner at (1000,66,1099) holds sc yellow then bo blue
    [p3] p3-no-entities=true no entity inside the boxes of the four placed markers (setIgnoreEntities)
    [p3] p3-missing-template=true place(test/does_not_exist) returned missing=true, placed nothing and threw nothing; Structures.LOG ends with it
    [p3] p3-no-generated-override=true with a 1x1x1 gold_block file in generated/ (the vanilla manager read size 1, 1, 1) Structures.template still gives 5, 5, 5; file deleted
    [p3] p3-to-world=true toWorld (block and centre forms) and footprint match the blocks found for the marker at 0, 90, 180 and 270 degrees
    [p3] note view pipeline-marker taken after 40 ticks, camera there=true
    [p3] note view pipeline-marker-north taken after 40 ticks, camera there=true
    [p3] p3-pipeline-suite=true all checks passed
    [p3] note studio mode=verify pieces=[test/p3_marker] scripts=[test/p3_marker] out=.../pipeline-151849/studio-out
    [p3] p3-fresh-test-p3_marker=true the script of test/p3_marker rebuilds to exactly the shipped structure file
    [p3] p3-studio-roundtrip=true the dump of test/p3_marker (12 lines), built and saved, equals the shipped file
    [p3] p3-studio-suite=true all checks passed

The line `p3-no-generated-override` also shows the check is meaningful: with the 1x1x1 file in the world's generated/ folder the vanilla StructureTemplateManager read size 1, 1, 1, and Structures.template still gave 5, 5, 5. The file is deleted in a finally block.

## Screenshots (opened with the Read tool; all three exist and show their claim)

- p3/pipeline-151849/run/screenshots/p3-pipeline-marker.png: two marker platforms (5x5 polished andesite, a 3-high stone-brick pillar with a lantern on top) seen from the south. On the left one (rotation 0) the oak stairs sit at its south edge in front of the pillar and the yellow hay block stands on the cell that is structure void (local 3,1,3), still there; on the right one (rotation 90) the stairs are on the west side of the pillar and a dark-blue banner edge shows on the pillar's east side (seen edge-on, as the brief expects), and the corner of the third marker (180) enters at the far right edge.
- p3/pipeline-151849/run/screenshots/p3-pipeline-marker-north.png (the extra view): all four markers from the north, looking south, so image left is east: from left to right rotation 270 (stairs east, banner on its west side), 180 (stairs on the north edge in front of the pillar, banner hidden on the south side), 90 (banner on the east side, stairs west), 0 (white banner with the yellow and blue pattern facing the camera, hay block beside the pillar). The four turns match the check results.
- p3/studio-151606/run/screenshots/p3-studio-test-p3_marker.png: the freshly built marker from the studio camera (south-east, above): platform, pillar, lantern and stairs; the banner is hidden behind the pillar from that side (only a sliver of blue) because it faces north.

## Extra checks I ran

- Player jar: `gradle --offline -q remapJar` built build/libs/graves-and-glory-0.4.0.jar; `tools/release_config.validate_player_jar(jar, source_identity())` returns OK with the new forbidden names in place, and the jar holds only the Structures classes (Structures, Structures$Placed, Structures$Loaded): no Phase3Checks, StructureStudio, gravesandglory_studio, p3_marker or structure files. (The forbidden tuple has no missing comma; I checked it with ast, the earlier truncated print only looked like it.)
- Line endings: `git ls-files --eol` shows i/lf w/lf for all new files; the marker .nbt is binary.
- The harness still contains the test-tools mixin config entry (`gravesandglory_test_tools.mixins.json`) and the TitleDemos entrypoint.

## Concerns

1. The plan's run recipe (plan header lines 66 to 69: `mkdir -p "$GRAVES_ROADMAP_TEST_ROOT"/{run,evidence,runtime}` and runClient) does not seed `run/options.txt`. The Phase 1 ruling (global-constraints and Task 2 report of the phase-1 sdd folder) says test roots must hold `pauseOnLostFocus:false` and `onboardAccessibility:false`, otherwise the client idles on the first-run "Welcome to Minecraft / Narrator / Accessibility" screen. My first export run (p3/studio-150911) did exactly that: no log line after 15:09:33 for 3+ minutes, process responding, ~0 CPU; a capture of the window (p1/stall-window.png) showed the screen. I killed that client (only that root's java process), then made Phase3Checks press Continue itself: `AccessibilityOnboardingScreen.close()` is what the Continue button runs (sets onboardAccessibility=false, writes options.txt, opens the title screen, which the mod then swaps for FrontMenuScreen); the tick handler also sets pauseOnLostFocus=false every tick, as Phase1Checks does. Verified in two fresh roots without any seeded file (studio-151606 and pipeline-151849 both started within a few seconds). Later Phase 3 tasks therefore work with the plan's recipe as written; Phase1Checks suites still need the seeding. Worth correcting the plan header or adding the seeding line there.
2. `dump` is only proven on the marker. It replays a template as `setblock`/`fill` lines in a fixed order (y, z, x runs), and /setblock runs shape updates on neighbours, so blocks whose state is derived from their neighbours (fences, walls, glass panes, corner stairs) may come back with other connection properties than the template holds. Pieces authored as scripts are not affected for export or the `fresh` check (both compare what the same script builds), but a p3-studio-roundtrip over such a piece could fail for reasons unrelated to the pipeline. The brief fixes the round trip on the marker, which has no such blocks.
3. The `/studio build|save|dump|place` commands compile and register but no suite runs them in the game; they are thin wrappers over build, save, dump and Structures.place, which the suites do exercise. Nothing in the brief asks for a command check, so I did not add one.
4. The prescribed studio camera is south-east and above. Anything important on the north or west faces of a piece cannot be seen in `p3-studio-<group>-<name>.png` (the marker's banner is the example). Later tasks may want a second angle, as the pipeline suite has.
5. The fixtures overwrite world blocks: the pipeline clears x 990..1040, y 63..72, z 1094..1124 to air before placing, and the studio build clears the piece's box plus a margin of 2 at (1000,64,1000). That is fine in the harness world; a real arena map overlapping those rectangles would be erased by a run. The pieces float on that cleared slab in the pictures; the rounded green horizon in the shots is the distant ground seen through fog (not investigated further).
6. `gravesandglory.phase3Maps` (GRAVES_PHASE3_MAPS) is wired in isolated-checks.gradle as the brief says, but nothing reads it yet; it is for the later landmarks suite.
7. Tooling note: the shell tool of this machine collapses backslashes and chokes on large heredocs. StructureStudio's patterns therefore avoid backslash classes (digits are matched with [0-9]), and I checked the finished files for stray backslashes; the runs confirm the script parser works (header, fill, setblock, SNBT with quotes and brackets).

## Fix round 1

All seven items are fixed on `phase-3-built-world`, one commit each, on top of c2d6699. HEAD is 038811f. The worktree is clean (Spec.java and sheets/preflight.json were not added; the build rewrites them as CRLF working copies with no content change and I restored them each time). `compileJava compileLocalTestTools` and `python tools/preflight.py` (`Preflight clean: 1261 filled cells ...`) are clean at HEAD.

| Item | Review | Commit |
|---|---|---|
| 1 | Important 1: `/studio dump` read the shipped piece, never the structure block save | 0aa53d3 |
| 2 | Minor 1: dump re-sorted the cells, a vine could come before its support | 2485141 |
| 3 | Minor 2: `p3-no-generated-override` could pass vacuously | 1a48304 |
| 4 | Minor 3: `Structures.load` serialised the template again to count blocks | a66942c |
| 5 | Minor 4: preflight did not enforce the size rule on structure files | 3dac8ba |
| 6 | Minor 5: no suite ran `/studio place` (nor `save`) | 8e00a8e |
| 7 | Minor 6: `/studio build` left its chunks force-loaded | 038811f |

Runs (each a fresh isolated root under `C:/Users/kortt/Melty/bannerbound-p3/work/p1/`, `run/options.txt` seeded with `pauseOnLostFocus:false` and `onboardAccessibility:false`, started only when no titled java process was up; the root names carry a suffix after the time so the three runs can be told apart):

- `p3-fix1-160507-red`: `GRAVES_PHASE3_CHECKS=pipeline,studio`, the NEW checks against the OLD `StructureStudio.java` of c2d6699 (put back in the working tree for this one run, never committed, restored afterwards). 16 pass, 4 fail, and the fails are the three expected ones plus the suite line: `p3-studio-dump-generated`, `p3-studio-dump-order`, `p3-studio-build-releases`, `p3-studio-suite`. Evidence: `p3-fix1-160507-red/evidence/local-checks.txt`, game log copy `p3-fix1-160507-red-latest.log`.
- `p3-fix1-161433-green`: the same suites at HEAD, verify mode. 20 pass, 0 fail. Evidence: `p3-fix1-161433-green/evidence/local-checks.txt`; the `[p3]` lines in `p3-fix1-161433-green/run/logs/latest.log`; `p3-fix1-161433-green/studio-out/command-checks/test/` holds what the command checks wrote.
- `p3-fix1-162023-export`: `GRAVES_PHASE3_CHECKS=studio GRAVES_STUDIO_PIECES=test/p3_marker GRAVES_STUDIO_MODE=export`. 10 pass, 0 fail: `p3-export-test-p3_marker` wrote `p3-fix1-162023-export/studio-out/test/p3_marker.nbt` (428 bytes, size 5, 5, 5 equals the header), and its uncompressed NBT is byte for byte the shipped `test-tools/resources/data/gravesandglory/structures/test/p3_marker.nbt` (1636 bytes, same SHA-1), so the dump change did not change what an export writes. The four new checks also pass in export mode.
- `work/p1/fix1-preflight-size-rule.txt`: the negative test of item 5.
- Also rebuilt the player jar after the Structures change: `gradle --offline -q remapJar`, `release_config.validate_player_jar(jar, source_identity())` is OK, and the jar has no Phase3Checks, StructureStudio, gravesandglory_studio, p3_marker or structure test files.

### Item 1 (Important 1): `/studio dump` dumped the stale shipped piece. Commit 0aa53d3

- **Changed** (StructureStudio.java, the dump command only; `Structures.template` is untouched): the template now comes from `server.getStructureTemplateManager().getTemplate(new Identifier(Structures.NS,id))`, which reads `<world>/generated/gravesandglory/structures/<id>.nbt` first and the mod resources second. The feedback says which one was used: `dumped <id> (the generated file was used: <path>) as a script of N lines to <file>` or `dumped <id> (no generated file, so the shipped piece was used) ...`. If neither exists it throws a StudioException naming both places.
- **Check:** `p3-studio-dump-generated` (Phase3Checks, studio suite). It writes a 1x1x1 gold block override with the shared helper `writeGoldOverride` (the one `p3-no-generated-override` now uses too), drops the manager's cached template, types `studio dump test/p3_marker` through the command dispatcher from an operator source, reads the written script and requires `# size 1 1 1` as its first line and "the generated file was used" in the feedback. Then it deletes the override (in a finally), drops the cache again, dumps again and requires `# size 5 5 5` and "no generated file" in the feedback.
- **Green** (`p3-fix1-161433-green/evidence/local-checks.txt` and the `[p3]` lines of `p3-fix1-161433-green/run/logs/latest.log`): `p3-studio-dump-generated=true with a 1x1x1 file in generated/ the dump of test/p3_marker begins '# size 1 1 1' and the feedback says the generated file was used; with the file deleted it begins '# size 5 5 5' and says the shipped piece was used`.
- **Red** (`p3-fix1-160507-red/evidence/local-checks.txt`, the new checks against the studio of c2d6699): `fail p3-studio-dump-generated: with the override the script begins '# size 5 5 5', not '# size 1 1 1' (the stale shipped piece); with the override the feedback does not say the generated file was used ...`. So the check does fail on the bug the review described.
- The written scripts of the check go to `<studio-out>/command-checks/test/p3_marker.mcfunction` (see the note on command-checks below), never over the real dump output.

### Item 2 (Minor 1): dump re-sorted the cells, so a vine could be written before its support. Commit 2485141

- **Changed** (StructureStudio.dump): the `cells.sort` by y, z, x is gone. The cells are walked in the order the template stores them (full cubes, then the other blocks, then the block entities, each by y, x, z). A run of equal blocks is merged into one `fill` only between entries that follow each other in that order, along z (same x and y, z plus 1, same state, no block entity data). The marker still dumps to 12 lines (its floor is now five fills along z instead of along x).
- **Check:** `p3-studio-dump-order`. Fixture script (a Java text block): `fill` of air over the 5x3x5 box, `stone` at (1,2,1) with `vine[up=true]` at (1,1,1) under it (ceiling vine), `stone` at (3,1,3) with `vine[south=true]` at (3,1,2) (vine hung on its south neighbour). The check builds the script, saves it, dumps the template, requires every `stone` line to come before the first `vine` line in the dump text, builds the dump, requires the ceiling vine (up=true) at (1001,65,1001) and the south vine (south=true) at (1003,65,1002), saves again and requires the two saves equal.
- **Green:** `p3-studio-dump-order=true a ceiling vine and a south-hung vine survive script, save, dump (23 lines, the stones first), build, save: the two saves are equal`.
- **Red** (the old sorted dump): `fail p3-studio-dump-order: the dump writes a vine before the stone it hangs on; the ceiling vine at (1001,65,1001) is air once the dump is built; the south-hung vine at (1003,65,1002) is air once the dump is built; the saved dump differs from the fixture ...`. This is the failure the review predicted, reproduced.
- The review asked that the ruling which carries concern 2 to Task 3 prove a south-hung or ceiling vine. This check is that proof for the dump side; Task 3's ruling can cite it by id.

### Item 3 (Minor 2): `p3-no-generated-override` could pass vacuously. Commit 1a48304

- **Changed** (Phase3Checks, Pipeline): the check now fails with `fixture did not take effect: the vanilla manager read <size> ...` unless the vanilla read of the override is exactly `1, 1, 1`. The override is written by the shared `writeGoldOverride` helper.
- **Green:** `p3-no-generated-override=true with a 1x1x1 gold_block file in generated/ (the vanilla manager read size 1, 1, 1) Structures.template still gives 5, 5, 5; file deleted` (same files as above). This shows the guard's condition holds in the game. I did not run the failing branch; it is a one-line condition with the review's own wording.

### Item 4 (Minor 3): `Structures.load` serialised the whole template again to count blocks. Commit a66942c

- **Changed** (Structures.load): the count is taken from the compound as it was read, `nbt.get().getList("blocks",NbtElement.COMPOUND_TYPE).size()`, instead of `template.writeNbt(new NbtCompound()).getList(...)`. For a 48x32x48 piece the first placement no longer pays for a second serialisation. Nothing else in Structures changed.
- **Check:** `p3-place-exact` (blocks==31) and the `Structures.LOG` comparison in `p3-studio-place`, both of which read the count that `load` now takes from the file.
- **Green:** `p3-place-exact=true ... box BlockBox{minX=998, minY=64, minZ=1098, maxX=1002, maxY=68, maxZ=1102}, blocks=31` and the four `[p3] note placed test/p3_marker blocks=31 ...` lines (4.50, 0.57, 0.59 and 1.07 ms), in `p3-fix1-161433-green/run/logs/latest.log`.

### Item 5 (Minor 4): preflight did not enforce the size rule on structure files. Commit 3dac8ba

- **Changed** (tools/preflight.py): the reader that walks the root compound of each structure file (`structure_header`, formerly `structure_data_version`) now also returns the root `size` list, read as a TAG_List of three ints. The loop adds one error line per violation: `structure <id>: width <n> is even, it must be odd`, `depth <n> is even, it must be odd`, `width <n> is more than 48`, `depth <n> is more than 48`, `height <n> is more than 32`, and `size is not a list of three ints`. The existing DataVersion and id checks are unchanged.
- **Check:** negative test with throw-away files `zz_*.nbt` in `test-tools/resources/data/gravesandglory/structures/test/` (preflight reads that folder and the one under src/main), removed again: sizes 4,5,5 / 5,5,6 / 49,5,5 / 5,5,51 / 5,33,5 / 50,40,50 and a two-int list 5,5 each produced the expected lines (50,40,50 produced five), and 47,32,47 (the largest allowed) produced none. The output is in `work/p1/fix1-preflight-size-rule.txt`. After removing the files `python tools/preflight.py` prints `Preflight clean: 1261 filled cells; every row generates a Java record. Runtime verification pending.` (also run again after the last commit). The shipped marker (5,5,5) passes.

### Item 6 (Minor 5): `/studio place` and `/studio save` were never run. Commit 8e00a8e

- **Changed:** new check `p3-studio-place` plus the shared helpers `Said` (a CommandOutput that captures the feedback), `dispatch` (types a command through `server.getCommandManager().getDispatcher()` with a given source) and a stored player id. No studio code changed in this commit.
- **Check:** the player is moved to the known block (1012,70,1060) (spectator, leash off, the chunks around it forced, the slab cleared with air), `studio place test/p3_marker 90` is typed from the player's own command source (`getCommandSource()`), and the result is compared with `Structures.footprint(Pipeline.SIZE, 1012, 69, 1060, 90)`: the last `Structures.LOG` entry (31 blocks, its box), the box named in the feedback, the bounding box of the non-air blocks the world holds, the 25 floor blocks at y-1 under his feet, the pillar from where he stood up to y+2, the lantern at y+3 and the stairs at `Structures.toWorld(..., 90, 2, 1, 4)` facing west. That proves the y-1 offset and the centre-under-the-feet rule.
- **Green:** `p3-studio-place=true typed by the player standing on (1012,70,1060), 'studio place test/p3_marker 90' put the marker in BlockBox{minX=1010, minY=69, minZ=1058, maxX=1014, maxY=73, maxZ=1062} (Structures.footprint): floor 25/25 under his feet, pillar where he stood, stairs west; it said: placed test/p3_marker turned 90 with its centre under your feet: 31 blocks in 0.6 ms, box BlockBox{...}`.
- **Red:** this check passed against the old studio as well (`pass p3-studio-place` in `p3-fix1-160507-red/evidence/local-checks.txt`). That is expected: the review asked for a check of behaviour that was untested, not for a fix, and it shows the fixture and the dispatcher work. The same run is also the red/green pair for the other three checks.
- `/studio save` is covered by item 7's check (the saved file's size equals its header, and its NBT equals `StructureStudio.save`).

### Item 7 (Minor 6): `/studio build` left its chunks forced. Commit 038811f

- **Changed** (StructureStudio): every `/studio` command runs inside `act()`, which snapshots the studio's set of forced chunks first and in a finally calls the new `letGo(world, held)`: it unforces, in the world (`setChunkForced(x, z, false)`) and in the studio's own set, exactly the chunks the action forced. Chunks a running suite holds (taken before the action) are not touched. I chose to release at the end of the command rather than to skip forcing: `/studio build` works at the fixed stage (1000,64,1000), where the operator is usually not standing, so nobody else loads those chunks while it builds.
- **Check:** `p3-studio-build-releases`: first `StructureStudio.release(w)`; the check computes the chunks of the stage (the piece's box grown by 2 at ORIGIN) and requires them unforced; it types `studio build test/p3_marker`, requires the pillar at ORIGIN+(2,1,2), and requires that none of the stage chunks is in `w.getForcedChunks()` and that `StructureStudio.forced` is empty; then `studio save test/p3_marker` and the written file's size must equal the header and its NBT must equal `StructureStudio.save`.
- **Green:** `p3-studio-build-releases=true 'studio build test/p3_marker' built the piece and left none of its 1 stage chunk(s) forced (the studio holds none and the world's forced chunks hold none); 'studio save' wrote it with the size of the header`.
- **Red:** `fail p3-studio-build-releases: chunk [62, 62] is still forced after the command; the studio still holds 1 chunk(s) after the command` (`p3-fix1-160507-red/evidence/local-checks.txt`).

### Notes on the new checks

- Four new check ids in the studio suite, after `p3-studio-roundtrip`, in this order: `p3-studio-dump-generated`, `p3-studio-dump-order`, `p3-studio-place`, `p3-studio-build-releases`. They are called at the end of `roundtrip`, so they run in verify mode and in export mode, and with `GRAVES_PHASE3_CHECKS=studio` or `pipeline,studio`.
- While they run, the system property `gravesandglory.studioOut` points at `<studio-out>/command-checks` (set and restored in a finally), so the files the commands write (`test/p3_marker.mcfunction`, `test/p3_marker.nbt`) land beside the real export and dump output, never over it.
- The shared helpers added to Phase3Checks are `Said` (captures command feedback), `dispatch` (types a command through the dispatcher with a given source at permission level 4) and `writeGoldOverride` (the 1x1x1 file in generated/). No shared harness file (Campaign, GravesAndGlory, CampaignChecks, isolated-checks.gradle) was touched in this round; the four files changed are Structures.java, Phase3Checks.java, StructureStudio.java and tools/preflight.py.
- Concerns 2 and 3 of the first report are answered: the dump is now exercised on the vine cases and on the marker (2), and `/studio build`, `save`, `dump` and `place` are all typed through the dispatcher in the game (3).

### Concerns after the fixes

1. The dump order is proven on the marker and on the two vine cases whose supports are full cubes. The template stores full cubes first, so a full-cube support always comes before what hangs on it. Something hung from a block that is not a full cube (a vine under a bottom slab, say) is stored among the other blocks, ordered by y, x, z, and could still be written before its support when the support is higher. None of the shipped pieces has one and I did not test it; Task 3's authors should hang vines on full blocks.
2. The failing branch of the item 3 guard (`fixture did not take effect`) was read but not run; the run shows its condition holds (the vanilla manager reads `1, 1, 1`).
3. Compiling prints the note that `StructureStudio.java` uses a deprecated API: `BlockBox.encompass` in `stage()` at line 123, which is first-round code, unchanged by this round.
4. Every launch had to wait for a free client because other harness runs were using the machine (a titled java process up each time I first looked); no run started while one was up.
