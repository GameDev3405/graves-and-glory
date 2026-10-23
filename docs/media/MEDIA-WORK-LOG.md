# Media work log, 2026-10-21 to 2026-10-22

**Status: on hold.** Kort asked to stop media work until the unit models and the maps are fully fleshed out (2026-10-22). Nothing is running. The harness, the scripts and the first deliverables are all kept so the shoot can restart with one command.

## What was asked, in order

1. Find 5 to 7 good Graves and Glory images and any gameplay video for an Instagram reel. Result: 6 stills from the archive and three older 16:9 clips (`clips/bannerbound-dark-fantasy.mp4`, `clips/dark-fantasy-battle.mp4`, `clips/smooth-entrance.mp4`). Kort did not like them: HUD showing, clips choppy and grainy.
2. Stage battles and film them properly: HUD hidden, better film quality, complementary shaders on, planned shots (commander on a hill, panoramas, drone moves). Result: shoot 1, the "lines" shoot, delivered in `work/media/` (gitignored).
3. Remake the clips with the troops not fighting in the water, armies stacked in columns so they charge together and clash on the objective, and add more transitions, fly-overs and shots. Result: the staging and 12 shot types are coded and compile (shoot 2, "columns"). It was only previewed on one scene, and that preview was not good enough to film for real (see "Shoot 2 status"). No shoot 2 media was delivered.

## Build and where things live

| thing | place |
|---|---|
| game build used | `phase-2-houses` (cdcf095) in a scratch git worktree `C:/Users/kortt/Melty/bannerbound-shoot`, Phase 1 maps |
| harness source | branch `media-shoot-harness` of this repo: `433a887` (shoot 1), `3dd36cc` (shoot 2). Test-tools only, never in releases. Check it out onto a newer build to reshoot. |
| scripts | `docs/media/tools/` (copies from the session scratchpad): `run-suite.sh`, `sheet.py`, `stitch.py`, `assemble.py`, `deliver.sh`, `deliver2.sh` |
| shoot 1 output | `work/media/` (README.md there has the folder map and the frame-time table) |
| shoot 2 preview | `work/media/v2-columns/PREVIEW-kingsfield-stills.jpg` only |
| shoot plan | `docs/media/SHOOT-PLAN-2.md`; the older parked list is `docs/media/SHOTLIST.md` |

Game stack: Minecraft 1.20.1, Fabric Loader 0.16.14, Yarn 1.20.1+build.10, Java 17, Gradle 8.8 offline. Iris 1.7.6 and Sodium with Complementary Reimagined r5.5.1 as the shader (selected by `config/iris.properties`).

## How it was made

**Running the game.** `docs/media/tools/run-suite.sh film KEY=VALUE ...` starts the client through `runClient` with `--init-script test-tools/isolated-checks.gradle` in an isolated game folder (`GRAVES_ROADMAP_TEST_ROOT`), with `GRAVES_PHASE1_CHECKS=film`. The script has the session's scratchpad paths in it; change `ROOT` to run it elsewhere. The first launch stalled on the accessibility onboarding screen until an `options.txt` was written. Compile with `gradle compileJava compileLocalTestTools --offline -q -PcampaignChecks --init-script test-tools/isolated-checks.gradle` and the same `JAVA_HOME` and `GRAVES_ROADMAP_TEST_ROOT`.

**Harness.** `MediaShoot` is a `Phase1Checks.Suite` plus `ServerWatch`, registered by a `ClientModInitializer` listed in `test-tools/resources/fabric.mod.json` (suite name `film`). It builds a skirmish on a map, stages the armies, raises a hill under the commander and films a list of takes through `FilmRig`. Four mixins (`Film*Mixin`, listed in `gravesandglory_test_tools.mixins.json`) give a free camera (`Camera.update`), the field of view (`GameRenderer.getFov`), the end-of-frame hook (`MinecraftClient.render` TAIL) and the HUD cancel (`InGameHud.render` HEAD). The HUD is hidden by cancelling its render; the first-person hand is skipped by the game when `hudHidden` is true, so the option stays false and only the render is cancelled. The player is forced to the classic Steve skin.

**Capture quality.** Every rendered game frame is read back inside the game and piped to ffmpeg, not screen-recorded. The readback is a three-slot PBO ring (asynchronous `glGetTexImage` into pixel pack buffers, harvested two frames later). The first version read back synchronously and dropped to 48 fps; the ring brought it to the 60 fps cap. Frames go raw RGBA to ffmpeg (vertical flip, bt709 tv range, `h264_nvenc` preset p5, high quality VBR, CQ 15) at 2560x1440. One game frame is one file frame. 90 warm frames render before recording so shadows, temporal filtering and chunk meshes settle. Each clip writes a `.json` with average, p95, p99 and max frame time and a list of slow frames. Vsync is off and the frame cap is the take's fps.

**Camera.** All camera moves are scripted per frame (`path.at(seconds)`), so they are exactly repeatable: `rail` (Catmull-Rom position and look-at splines with a trapezoid ease), `orbitEx` (radius, height, look point and fov all drift), `chase`, `hold`, plus `follow` (a smoothed chase camera on the army's hero, tracked from the server each tick). `aim` returns nine values including the look target so the pitch is recomputed after the clearance lift that keeps the camera out of terrain. Right-arm poses (sword raise) are `arm(...)` keyframes.

**Staging.** Hill of real blocks under the commander (with a path, grass and a lantern); the sun is put low on the commander's facing side (time -1 gives 11900 or 300 depending on the side) so the army is backlit from the hill; `rear()` penalises a facing that has buildings or cliffs behind the hill; `obstructions()` counts blocks in the view; `clearView()` removes trees along the battle axis. Armies muster in "hold", then `release()` orders a charge and blows the horn on a chosen recorded frame. The commander wears the scene's armour (gold, iron, diamond or netherite) with a matching sword.

**Panoramas.** First-person stills every 15 degrees from -90 to +90 (vfov 50, hand off), stitched by `stitch.py` into a 180 degree cylindrical panorama with feathered blending (pure rotation, no feature matching), then 4:5 carousel tiles by `assemble.py`.

**Delivery.** `deliver.sh` (shoot 1) and `deliver2.sh` (shoot 2, also writes 720p previews and handles slow motion) make 1080p60 H.264 CRF 14, 9:16 centre crops (810x1440 cropped from the 1440p frame, scaled to 1080x1920, CRF 15) and 720p previews for chat. `sheet.py` makes contact sheets.

**Problems met and fixed.**

| problem | fix |
|---|---|
| onboarding screen stalled the first launch | wrote `options.txt` |
| 48 fps from synchronous readback | PBO ring |
| ffmpeg start-up hitch in the first frames | 90 warm frames |
| hero shot mis-framed after the camera lift | recompute pitch from the look target |
| camera inside walls and houses | `rear()` facing penalty |
| dive ended inside the objective banner | end point offset |
| no first-person hand | `hudHidden` false plus the HUD-cancel mixin |
| Hollowwood too dark | twilight time 12350 and gamma 100 |
| clips with stalls (kingsfield-c1, ironhold-c6, hollowwood-c1 and c2) | reshot |

## Shoot 1 result (delivered)

Five scenes, each with 8 stills, 6 clips (crane, orbit, fly-over, dive, follow, first-person run) and a 180 degree panorama; 30 clips at 1440p60 with an average frame time of 16.8 to 17.4 ms. Known flaws: first-person clips are blocked by a body or wall for a moment (kingsfield-c6, rivergate-c6, ironhold-c6, hollowwood-c6), one-frame stalls of up to 46 ms, classic Steve skin, Hollowwood is twilight at boosted brightness, 9:16 is a plain centre crop, no audio.

**The scene names in shoot 1 were wrong for two scenes.** The harness called map 0 "kingsfield" and map 3 "rivergate". The real map order (`Spec.MAPS`) is 0 Rivergate Valley, 1 Ironhold Siege, 2 Frostmarch Pass, 3 Hollowwood Reach, 4 Kingsfield Crossroads. So the footage called `kingsfield-*` was shot on Rivergate Valley and the footage called `rivergate-*` (rain, dawn) on Hollowwood Reach. `ironhold`, `frostmarch` and `hollowwood` were right. Shoot 2 uses the true names.

**The troops fought in the water in shoot 1 because the armies were one long line.** Each army was five squads side by side, about 87 blocks wide, and four of the five objectives chosen were river crossings.

## Shoot 2 status (columns, coded, not yet good)

**Staging.** The squads of one army now stack one behind another along the charge, about 12 troops abreast and 2 to 3 ranks per squad, so each column is about 27 blocks long and 19 wide. The hero's squad leads and archers go last. Both columns start 12 blocks from the objective and meet on it. The commander's hill sits behind his own column at an automatic distance. Facing is chosen by scanning the whole battlefield rectangle for water, buildings, trees and steep steps, as well as the view and the rear of the hill. Environment variables: `GRAVES_FILM_COLS` (12), `GRAVES_FILM_SPACING` (rank spacing x10, 18), `GRAVES_FILM_GAP` (12), `GRAVES_FILM_DIST` (0 means automatic), `GRAVES_FILM_OUT` (media folder).

**Water scan** (`GRAVES_FILM_MODE=fields`: every map and objective, no filming; cost is 10 per water cell, 2 per blocked cell, 1 per steep step, lower is better):

| objective | cost | water cells | | objective | cost | water cells |
|---|---|---|---|---|---|---|
| m0p0 | 172 | 0 | | m3p0 | 36 | 0 |
| m0p1 | 119 | 9 | | m3p1 | 163 | 13 |
| m0p2 | 32 | 0 | | m3p2 | 56 | 1 |
| m1p0 | 32 | 1 | | m4p0 | 356 | 33 |
| m1p1 | 1190 | 12 | | m4p1 | 33 | 0 |
| m1p2 | 36 | 1 | | m4p2 | 119 | 9 |
| m2p0 | 22 | 0 | | m4p3 | 63 | 0 |
| m2p1 | 141 | 11 | | m4p4 | 350 | 33 |
| m2p2 | 106 | 0 | | | | |

So the old choices (p1 of maps 0 to 3) were the river crossings. The shoot 2 presets use rivergate m0p2, ironhold m1p0, frostmarch m2p0, hollowwood m3p0 and kingsfield m4p1.

**New shots** (all coded; stills s8 to s11 and clips c7 to c12): vanguard (low, looking back from the front of the column at the commander on his hill), top-down tactical view, flank view of both columns, top-down descent (c7), glide over the heads of the charging army (c8), orbit of the clash (c9), side tracking shot beside the hero (c10), slow-motion clash captured at 120 fps and played at half speed (c11, `deliver2.sh` with `SLOWMO_SS` to trim), reverse fly-over from the enemy side (c12). The older shots c1 to c6 were adapted to the column geometry.

**The one preview** (Kingsfield objective m4p1, `PREVIEW-kingsfield-stills.jpg`; clips were previewed as every 45th frame and not reviewed) showed:
- the columns do form and stand on dry ground;
- m4p1 is a walled circular outpost on a raised hill, a poor stage: the fight is inside a stone ring and the hill dominates every shot;
- several new camera positions are placed badly for that terrain: s4 (enemy view) is inside a tree, s5 (clash) is against a wall, s9 (vanguard) points at dirt, s11 (flank) is behind a hill and a tree;
- the top-down (s10) and crane (s6) show the layout well.

Not yet checked: the other four scenes, the 120 fps slow motion (does the GPU hold 120 fps at 1440p with shaders), whether rear squads charge and arrive as a wave, and whether the columns hold their shape in hold.

## When resuming

1. Wait for the unit models and maps to be final, then pick the build and check out `media-shoot-harness` onto it.
2. Rerun the water scan (`GRAVES_FILM_MODE=fields`) on the final maps; pick one dry, open objective per map. Prefer open ground; avoid outposts and the walled keep.
3. Preview each scene (`GRAVES_FILM_MODE=shoot GRAVES_FILM_ONLY=<scene> GRAVES_FILM_PREVIEW=45`, which writes a PNG every 45 frames instead of a video), fix camera positions per scene, then shoot for real without `PREVIEW`, then `deliver2.sh <media dir>`.
4. Things to do with final assets: real commander skin instead of Steve, a fix for the first-person clips (blocked lens), per-clip 9:16 framing, audio or music, a rough cut.
5. Other controls: `GRAVES_FILM_ONLY` (scene ids), `GRAVES_FILM_SHOTS` (keys s1 to s11, p1, c1 to c12), `GRAVES_FILM_TIME`, `GRAVES_FILM_GAMMA` (percent), `GRAVES_FILM_SHADER`, `GRAVES_FILM_W/H/FPS`, `GRAVES_FILM_NOREC`.

## Not committed

The harness is committed on `media-shoot-harness` only. `docs/media/MEDIA-WORK-LOG.md`, `docs/media/SHOOT-PLAN-2.md` and `docs/media/tools/` are uncommitted files on `phase-1-solo-core`; `work/media/` is gitignored.
