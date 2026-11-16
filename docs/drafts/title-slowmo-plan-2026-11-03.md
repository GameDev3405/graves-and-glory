# Title screen slow motion: plan and prototype (board 4F.3, 2026-11-03)

Kort's direction (2026-11-03): the title screen shows slow-motion shots of epic battlefield fights, such as arrow volleys flying and lines clashing.

Status: prototype capture only. Nothing is shipped and the title screen is unchanged. Branch `ui/title-slowmo`, test-tools only.

## 1. Approach

**Recommended: pre-rendered loops.** Film bot-sim battles with the harness camera at slowed game time, cut 6 to 8 second loops, ship them as a compressed frame sequence, and draw the current frame behind `FrontMenuScreen`.

| | pre-rendered loops (recommended) | live in-engine replay |
|---|---|---|
| look | shader-quality frames, framed and trimmed by hand, the same every launch | only the player's own settings (most players have no shaders); camera and crowd vary from run to run |
| title-screen cost | one 960x540 texture upload per shown frame (24 per second), decoded on a worker thread; well under 1 ms per frame | loads a world and an integrated server behind the menu: 10 to 20 s extra start-up, 1 to 2 GB more memory, a full world render every frame |
| jar size | +10 to 25 MB (see the budget) | about 0 (maps and models are already in the jar) |
| new code | small: a frame reader and one draw call | large: a replay recorder and format, a headless world on the title screen, camera scripting in the shipped mod, and safe teardown when the player clicks Play |
| risk | low; the existing gradient-and-battlements art stays as the fallback | high: world loading on the title screen interacts with the launch auto-join, the save and the Play button, and needs protocol and save review |
| upkeep | reshoot when models or maps change (the harness reruns in about 10 minutes per scene) | follows the art automatically |

The live replay only wins on jar size and on keeping up with art changes. Its start-up time, memory and title-screen risk are too high for a menu backdrop.

## 2. How the slow motion is made (prototype, done)

The media harness from `media-shoot-harness` (FilmRig, MediaShoot, `Film*Mixin`) was ported onto main under `test-tools/` (never in releases). Fixes for API drift: rout state no longer lives on `Squad`, and `chargeHorn` is gone. Neither is needed for silent film takes.

New in this branch: **true slow time, not frame stretching.**
- `FilmSlowServerMixin` multiplies the integrated server's 50 ms tick slot by `FilmRig.slow`, so the bots, arrows and swings run 4 times slower.
- `FilmSlowClockMixin` gives the client's `RenderTickCounter` a continuous virtual clock at 1/`slow` speed, so client ticks slow down too and tick interpolation (`tickDelta`) fills the gaps. A 60 fps capture then has 60 truly different frames per second of footage: arrows travel smoothly instead of stepping.
- A take event switches slow time on at recorded frame `GRAVES_FILM_SLOW_AT` (80, about 1.3 s after the charge order), just before contact. The end of the take resets it to 1. Controls: `GRAVES_FILM_SLOW` (factor, 4) and `GRAVES_FILM_SLOW_AT`.
- The camera path runs on recorded seconds, so the camera moves at a calm real speed over the slowed fight.

This replaces shoot 2's c11 approach (120 fps capture played at half speed). That needed a GPU holding 120 fps and gave only 2x.

Run: `bash <scratchpad>/shoot.sh` (the same as `docs/media/tools/run-suite.sh film` with `GRAVES_FILM_MODE=shoot GRAVES_FILM_ONLY=rivergate GRAVES_FILM_SHOTS=t1,t2,t3 GRAVES_FILM_W=1920 GRAVES_FILM_H=1080`).

## 3. Shot list

Test shots (prototype, Rivergate Valley point m0p2, columns staging, no shaders):

| key | shot | camera |
|---|---|---|
| t1 | arrow volley | low beside our rear (archer) squad, looking up the line of fire over the column toward the clash; slow push forward |
| t2 | shield-line clash | low side view across the meeting point, slow lateral drift |
| t3 | wide clash | high slow orbit over both columns meeting |

For the final set, one loop per map, picked from the dry objectives in the water scan (MEDIA-WORK-LOG.md), with shaders on:
1. Rivergate Valley: volley over the river meadow (t1).
2. Ironhold Siege: shield wall at the gate, low (t2).
3. Frostmarch Pass: wide orbit in snow (t3).
4. Hollowwood Reach: cavalry or charge through the trees at twilight, tracking.
5. Kingsfield Crossroads: the commander on his hill with the clash behind him (c1 crane, slowed).

Rotate one loop per launch, or cross-fade between loops every 8 s (open choice 2).

## 4. File size and performance budget

Measured on the test shots (a 6 s slowed stretch from each clip, cut 1.4 s in):

| clip | 960x540 JPEG q5, 24 fps, 144 frames | 1280x720 H.264 CRF 22 (reference only) |
|---|---|---|
| t1 volley (calm, wide) | 6.7 MB | 6.3 MB |
| t2 clash (busy, close) | 9.2 MB | 9.4 MB |
| t3 wide orbit | 12.6 MB | 12.2 MB |

Budget: **at most 12 MB per loop and 25 MB in total** in the jar (about 2 loops, or 3 at 20 fps or 6 s). Rain and fast camera moves cost the most. Fine weather and slower moves should bring a loop to about 7 MB. H.264 is no smaller here and would need a native decoder (a new dependency of 30 MB or more), so JPEG frames are the format.

Performance on the title screen: decode the next frame on a daemon thread, at most 2 frames ahead (about 4 MB of RGBA), and upload into one `NativeImageBackedTexture` on the render thread (one `glTexSubImage2D` of 960x540, about 0.3 ms). Release the texture and the thread when the player leaves the menu screens. No world, no server, no extra memory once in game.

## 5. How the title screen plays it

1. Assets: `assets/gravesandglory/title/<loop>/frames.bin`, a pack of concatenated JPEG frames with an index of offsets (one file per loop, read with `ResourceManager`). JPEG is decoded with `javax.imageio`, which is in the JDK, so no new dependency. PNG frames would be about 8 times larger.
2. `TitleBackdrop` (client, new): owns the texture, the worker and the clock. The frame index is `(elapsed * 24) mod frameCount`; it skips frames rather than slowing down if decoding falls behind.
3. `FrontMenuScreen.render`: draw the backdrop cover-scaled to the window (crop the edges, never letterbox) in place of the gradient and battlements. Then add a dark vignette and a lower-third gradient so the gold title, buttons and version stay readable (contrast is checked at 9 per the rubric). Keep the current procedural art as the fallback when the asset is missing or fails to decode.
4. Loops are cut so the last frame matches the first (trimmed to the camera's start and end) or cross-faded over 12 frames.
5. An accessibility toggle (reduce motion) shows a single still.

## 6. Open choices for Kort

1. **Shaders in the footage?** With Complementary the frames look much better than most players' own game. Recommended: yes, since this is a menu backdrop and not gameplay.
2. **One loop per launch, or a rotating montage** of 3 to 5 loops with cross-fades? A montage costs about 3 to 5 times the jar size.
3. **Size ceiling:** 960x540 at about 13 MB per loop, or 1280x720 at about twice that (sharper on large monitors).
4. **Slow factor:** 4x (prototype) or stronger. 6x to 8x needs a longer take per loop but costs nothing extra to make.
5. **When to shoot the final set:** media work is paused until models and maps are final (Kort, 2026-10-22). Recommended: ship the player only once the models and maps are final, and leave the procedural backdrop until then.

## Evidence

Test shoot, 2026-11-03: one harness client in an isolated run root, Rivergate m0p2, 1920x1080, slow 4x from frame 80, no shaders. Exit 0, client closed.

Files (gitignored `work/`): `work/title-slowmo/clips/rivergate-t{1-volley,2-clash,3-wide}.mp4` (plus `.json` frame stats), contact sheets in `work/title-slowmo/sheets/`, and size-test frames and 720p cuts in `work/title-slowmo/loop-test/`.

Findings:
- **t2 clash works:** the lines meet head-on in the foreground, with hit flashes, shields and swords. It is the strongest candidate.
- **t1 volley misses:** the low camera ends behind a grass rise, the army is small on the horizon, and no arrows read. Fix: raise it to about 3 blocks, frame from beside the archers looking along the arc, check that archers are in range during the slowed stretch, and preview first (`GRAVES_FILM_PREVIEW`).
- **t3 wide:** usable as a wide.
- The preset uses **rain**. Final shots should be clear or golden hour.
- **Frame pacing:** the GPU was shared with other clients and averaged 42 to 47 ms per frame instead of 16.7. The slowed clock follows real time, so motion is continuous but the on-screen slowdown was about 1.5x, not 4x. For the final shoot, run alone, or switch the clock to a fixed step of (1000/fps)/slow ms per rendered frame. That makes it an offline render that cannot be affected by frame rate.
- Not yet checked: shaders, loop seams, and the in-menu player (none is built).
