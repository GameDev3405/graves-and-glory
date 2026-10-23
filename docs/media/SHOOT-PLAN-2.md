# Media shoot 2: film plan (2026-10-22)

Supersedes the Oct 16 clips and the parked `SHOTLIST.md` for the Instagram reel. Kort's brief: no HUD until it is polished, battles worth filming, a commander on a hill directing his army, panoramas, drone shots over the field, Complementary shaders, smooth and clean film (the Oct 16 clips were choppy and grainy).

## Why the old clips looked bad
- Screen-recorded the window at about 4.5 Mbps: the shader's noise and shadow detail smear into grain at that rate.
- The window was rendered at an unlocked frame rate and sampled by the recorder on a separate clock, so frames repeat and skip (judder).
- Camera moves were sampled at 20 Hz in the old intros.

## Capture spec
- Build: scratch copy of `phase-2-houses` (`bannerbound-shoot`, detached), the newest units: Gilded Order, Graveborn, Ironclad, Tidecallers. Maps are the Phase 1 maps (the Phase 3 built-world branch is not merged there), so shots favour terrain and units over the map dressing.
- Game rendered at 2560x1440, frame rate capped at 60, shaders Complementary Reimagined (Unbound for the moodiest scenes), HUD, hand and crosshair hidden, no toasts or chat.
- Every rendered frame is read back inside the game and piped to ffmpeg (no screen recorder), so one frame in is one frame out. Frame times are logged per clip; a clip with any frame over 20 ms is rejected and re-shot.
- Camera: keyframed paths (look-at spline, eased, sampled per frame, terrain clearance), not 20 Hz positions.
- Masters are near-lossless 1440p60 (NVENC, CQ 10). Delivery: 1080p60 H.264 CRF 14, plus a 9:16 crop for Reels and 4:5 stills for the feed.

## Armies and heroes (hard banners carry the legendary unit)
Gilded Order (Lord Marshal Aldric), Graveborn (The Hollow King), Ironclad (Brassjaw, the Forge Colossus; ballistas, mortars), Tidecallers (Admiral Vael; Drowned, Kraken-touched Brute, Mist Callers). Wildmoot (Old Horn, wolves, spiders) is not fieldable yet; a bonus if it stages cleanly.

## Scenes (one battle story each)
1. **The Bridge at Kingsfield**: Gilded vs Graveborn at the central bridge, sunset. The signature shot of the game.
2. **The Breach at Ironhold**: Ironclad siege crews and Brassjaw against a Gilded line at the fortress gate, dusk, ballista and mortar fire.
3. **Drowned Tide at Rivergate**: Tidecallers wade the river against a Gilded bank line, dawn, rain and mist.
4. **Night March at Hollowwood**: the Graveborn horde with the Hollow King through lantern light and marsh mist, night.
5. **Cold Dawn at Frostmarch**: snow ridge, low sun, Ironclad against Tidecallers.

## Stills
- A1 commander black against the sunset, low camera (scene 1).
- A2 commander pointing, over his shoulder, the army below (1).
- A3 the two lines before the clash, wide and symmetrical (1, 4).
- A4 the clash from above at three-quarter, arrows in the air (1, 2).
- A5 hero portrait, low and close: Hollow King, Brassjaw, Admiral Vael.
- A6 night march in lantern light (4).
- A7 ballista battery firing (2).
- A8 the wade in rain (3).
- P1 and P2: 180 degree panoramas from the commander's hill (golden hour, night), stitched from per-frame captures.

## Clips (8 to 15 s)
- C1 crane reveal: the commander's face, rise and pull back to the whole battle.
- C2 orbit: a slow circle of the commander as he raises his arm and the line charges.
- C3 drone fly-over: from behind the Gilded line, over the clash and out.
- C4 drone dive: high over the field, diving into the melee.
- C5 low tracking beside a charging line.
- C6 gameplay POV, HUD hidden: first person, walking with the line into the clash, sword swings.
- C7 siege fly-by past the ballista battery.
- C8 night push-in through the marching ranks.
- C9 rain sweep across the river wade.

## Order of work
1. Scout every map and objective at golden hour (one still each) and choose the best stands.
2. Stills for scenes 1 to 4, then panoramas.
3. Clips C1 to C4 on scene 1, tuned until frame times are clean; then the rest.
4. Contact sheet and delivery encodes in `work/media/`.

## Results (2026-10-22)

Shot from the `phase-2-houses` scratch copy on the Phase 1 maps, Complementary Reimagined on, HUD hidden, in-game per-frame capture at 2560x1440 and 60 fps (average frame time 16.8 to 17.4 ms on all 30 clips).
Five scenes (kingsfield, ironhold, rivergate, hollowwood, frostmarch), each with 8 stills, 6 clips (crane, orbit, fly-over, dive, follow, POV) and a 180 degree panorama.
Everything is in `work/media/` (gitignored); `work/media/README.md` has the folder map, frame-time table and known soft spots (POV clips blocked by a body or wall for a moment, one-frame stalls up to 46 ms, Hollowwood is twilight at boosted brightness).
Harness: local branch `media-shoot-harness` in the scratch worktree `bannerbound-shoot` (commit 433a887, not pushed). Re-run on a newer build by checking that branch's `test-tools/` out onto it.

On hold since 2026-10-22 (Kort): no more media work until the unit models and maps are final. The full log, including the column re-staging for shoot 2 and how to restart, is `docs/media/MEDIA-WORK-LOG.md`.
