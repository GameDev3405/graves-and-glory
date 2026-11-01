# Motion grading: sword_shield

Family `sword_shield` (engine family `sword_shield`, troop `knight`, rig `biped`).
Evidence run: `test-tools/work/grade/sword_shield/20261101-005828` (manifest.json lists every file by clip, state and kind).

Grade each rubric line 1 to 10. A state passes only when every line is 10 (R65). Write what you can see in the named
file: the frame, the angle or the unit, and the fault. A grade with no observable finding does not count.

Re-runs (M3): while every grade cell here is still `__/10`, a re-run rewrites this whole file; once any grade is filled
in, a re-run replaces only the `Evidence run:` line above and leaves the grades and findings alone.

## How the evidence was made

- Pipeline (R75): every sheet is the full TroopMotion pipeline as the game draws it (engine layers, MotionFeet, scene and death handling); clips are asked for only at the input level (stance packets, attack and hit events, scene poses, the server's Tactics.shield, real kills). No sheet is a raw clip with the engine layers off.
- Solo (R74, R68, R76, R77): 4.95 blocks from the troop at 8 angles (0 front, 90 its right), eye 0.91, level; fitted so every clip's extent from every angle fills at most 80 percent of the frame; crop [262, 107, 748, 591] of the frame (the union of the extent's projections plus 10 percent), native, no downscale. Each clip: 8 frames front and side with feet and hands crops, its key poses from 8 angles
  (0 front, 45, 90 its right, 135, 180 back, 225, 270 its left, 315), and its weapon-tip and shield-rim trails front and side.
- Marks on solo and loco shots: white ground line with height ticks every 0.5 block, soles green when planted, red when lifted
  and magenta when sunk more than half a texel (1/32 block) below the ground, each over a grey ground tick; hands yellow, weapon tip orange, shield rim cyan. The marks are computed from the drawn pose, not painted by hand.
- Locomotion (R71): the solo side camera (4.95 blocks, eye 0.91) tracking the troop each tick along x; crop [150, 107, 972, 591] (the solo crop 15 percent wider each side). Segments: walk, run, shield crawl, backpedal and a 90 degree turn in place, each from idle and back to
  idle. Planted slide and sole heights are in manifest.json `loco`, and summarised below.
- Spacing (R73): real spacing (R73): a shield troop in wall, brace, shield_move or hold stands shieldCell = max(0.70, width + 0.1) = 0.70 apart; any other max(formation 1.80, max(1.3, width + 0.3)) = 1.80. The wide grid (2 x 3) is only the clone-check overview; the wide grid is a clone-check overview only, never evidence of spacing or clipping.
- Crowd distance (R78): the formation and the wide overview from (0, 16.50 over the ground, -26.85) in front, pitched at the grid's middle: the nearest row about 31 blocks away; the formation from (0, eye 1.62, -30.50), level: the nearest row 30.5 blocks away. Cite the level shot for silhouette and the high formation shot for variation.
- Capture: 120 frames at 20 fps; events right after frame 10 (attacks and hits again after 70); strip = every 4th frame;
  dense sheets show every frame from E-10 to E+10 with the per-tick pose delta under each (R72).
- Capture timing (R81): every frame newly drawn; at most 2 world steps per capture not exactly 1 tick, none touching a dense window (event E-10 to E+10). Each capture's off-steps (frame and world ticks) are in manifest.json `checks.captureTiming`,
  and a tile whose frame ends one is labelled `STEP n TICKS`.
- Volleys (M1): every troop gets its attack or hit on the same tick, so crowd and formation shots show synchronised
  volleys. Judge variation by stance phase, amplitude, faces and seeds, not by event timing.
- Deaths (R79): crowd stills at f028 (vanilla death tint, as in game), f050 and f080 (death ticks about 40 and 70).
- Blend (M2): cite the dense sheets and the delta plots, not the GIF.

Measured: the nearest crowd troop was 30.54 blocks from the camera.

Measured: 24 close captures, 4 world steps not 1 tick in all, 5 stale frames (stance-carry f077 2 ticks, f098 0 ticks; guard-carry f015 2 ticks, f036 0 ticks).

Planted slide (R83) is measured at the corners each foot stands on, plant by plant: a box foot rocking heel to toe reads as
no slide; the old sole-centroid measure is kept in the manifest as `soleCentroidSlideBlocks` for comparison.

| Loco segment | Speed (blocks/tick) | Plants R, L | Planted slide R, L (blocks) | Largest single plant R, L | Lowest sole R, L | Body moved |
|---|---|---|---|---|---|---|
| walk | 0.169 | 8, 8 | 5.538, 3.904 | 1.222, 0.752 | -0.067, -0.067 | 8.46 |
| run | 0.264 | 9, 9 | 5.147, 3.357 | 1.344, 0.820 | -0.094, -0.093 | 10.58 |
| crawl | 0.056 | 1, 1 | 2.817, 2.834 | 2.817, 2.834 | -0.120, -0.122 | 2.79 |
| backpedal | 0.127 (assumed) | 1, 1 | 8.024, 8.002 | 8.024, 8.002 | -0.054, -0.054 | 6.35 |
| turn | 0.000 | 1, 1 | 0.533, 0.618 | 0.533, 0.618 | -0.036, -0.026 | 0.00 |

Backpedal speed is assumed: the walk speed x 0.75, as no measured backpedal speed exists yet.

| Loco segment | Foot | Each plant: start frame (server tick), frames, slide in blocks; engine plant weight mean |
|---|---|---|
| walk | right | f000 (t1) 25 fr 1.222 w1.00; f026 (t27) 5 fr 0.405 w1.00; f034 (t35) 3 fr 0.610 w1.00; f041 (t42) 4 fr 0.384 w1.00; f048 (t49) 3 fr 0.983 w1.00; f054 (t55) 5 fr 0.454 w1.00; f062 (t63) 3 fr 1.000 w1.00; f068 (t69) 44 fr 0.481 w1.00 |
| walk | left | f000 (t1) 26 fr 0.402 w1.00; f028 (t29) 2 fr 0.752 w1.00; f034 (t35) 4 fr 0.281 w1.00; f042 (t43) 2 fr 0.378 w1.00; f047 (t48) 5 fr 0.471 w1.00; f056 (t57) 2 fr 0.441 w1.00; f061 (t62) 5 fr 0.490 w1.00; f069 (t70) 43 fr 0.689 w1.00 |
| run | right | f000 (t1) 24 fr 1.344 w1.00; f025 (t26) 3 fr 0.380 w1.00; f031 (t32) 2 fr 0.780 w1.00; f035 (t36) 2 fr 0.328 w1.00; f040 (t41) 2 fr 0.810 w1.00; f044 (t45) 3 fr 0.275 w1.00; f050 (t51) 1 fr 0.000 w1.00; f054 (t55) 2 fr 0.325 w1.00; f059 (t60) 43 fr 0.905 w1.00 |
| run | left | f000 (t1) 25 fr 0.731 w1.00; f026 (t27) 2 fr 0.712 w1.00; f030 (t31) 3 fr 0.400 w1.00; f036 (t37) 1 fr 0.000 w1.00; f040 (t41) 2 fr 0.265 w1.00; f045 (t46) 2 fr 0.135 w1.00; f049 (t50) 2 fr 0.295 w1.00; f055 (t56) 1 fr 0.000 w1.00; f059 (t60) 43 fr 0.820 w1.00 |
| crawl | right | f000 (t0) 112 fr 2.817 w1.00 |
| crawl | left | f000 (t0) 112 fr 2.834 w1.00 |
| backpedal | right | f000 (t1) 111 fr 8.024 w1.00 |
| backpedal | left | f000 (t1) 111 fr 8.002 w1.00 |
| turn | right | f000 (t1) 95 fr 0.533 w1.00 |
| turn | left | f000 (t1) 95 fr 0.618 w1.00 |

| Transition | Event | Largest step (deg/tick) at frame | Largest 2nd difference | Ratio to baseline p90 | Flagged |
|---|---|---|---|---|---|
| hold-wall-brace | stance wall | 38.9 at f025 (right_hand) | 14.8 | 24.0 | yes |
| guard-carry | stance hold | 47.2 at f075 (right_hand) | 17.2 | 20.8 | yes |
| shield-up-down | raise | 21.3 at f027 (right_arm) | 6.6 | 6.9 | no |
| hit-while-attacking | attack | 121.3 at f025 (right_forearm) | 84.7 | 58.5 | yes |
| death-while-attacking | kill | 143.2 at f030 (right_hand) | 110.8 | 5.9 | yes |
| loco-walk | move to stop | 108.9 at f069 (right_leg) | 92.8 | 1.7 | no |
| loco-run | move to stop | 176.3 at f050 (right_leg) | 170.4 | 1.5 | no |
| loco-crawl | idle to move | 17.9 at f025 (right_leg) | 11.1 | 1.1 | no |
| loco-backpedal | idle to move | 73.5 at f024 (right_forearm) | 40.8 | 1.5 | no |
| loco-turn | turn starts | 16.5 at f021 (right_leg) | 28.1 | 1.5 | no |

Rubric (R65): Weight and timing; Arcs; Anticipation and follow-through; Silhouette at distance; Variation (no clones); Contact (no foot slide, no ground sinking); No clipping (body, weapon, shield, neighbour); Blend continuity (no pops).

## stance-idle / idle

Clip `idle` (stance, 4.00 s, loop, key poses extreme_a 1.50s, extreme_b 3.20s). State: server stance `none` from `hold`, stills at f030, spacing 1.80.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/stance-idle/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/idle/contact.png` (8 frames front and side)<br>`states/stance-idle/delta.png` (speed of the pose per tick) | contact f0-f7 front and side: small, slow beats (sword settles, shield arm resettles) on a held weight; the live strip holds the stance with no float |
| Arcs | 8/10 | `clips/idle/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/idle/turnaround.png` (key poses from 8 angles) | trail: the weapon tip moves on short smooth loops; turnaround: key poses differ by a few degrees, as a held stance should |
| Anticipation and follow-through | 7/10 | `clips/idle/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-idle/strip.png` (the live state) | extreme_a / extreme_b rows: right_hand and left_forearm lag the arm by 0.08-0.12 s; little anticipation, a held stance has little to anticipate |
| Silhouette at distance | 8/10 | `states/stance-idle/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-idle/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | level_zoom: board, helm and the sword point over the rim read at 30 blocks; the guard and wall line reads as a shield line |
| Variation (no clones) | 7/10 | `states/stance-idle/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-idle/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-idle/formed_f030.png` (the whole formation frame) | formed_zoom: per-unit posture, seeds and life layer vary the line, but stance phase is close across units, so neighbours look alike |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`clips/idle/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/idle/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-idle/formed_f030.png` (neighbours at real spacing)<br>`states/stance-idle/close_f030.png` (the trio at real spacing, close)<br>`states/stance-idle/dense_f010.png` (every frame round the event) | turnaround 0-315: no blade through body or board; at real spacing 0.70 the boards overlap edge over edge (wall, guard) as designed, not through a neighbour's body |
| Blend continuity (no pops) | 8/10 | `states/stance-idle/dense_f010.png` (every frame round the event)<br>`states/stance-idle/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`loco/walk/dense_idle-to-move.png` (a related transition)<br>`loco/walk/dense_move-to-stop.png` (a related transition) | dense_f010 and delta: no flagged window in the state itself |

## stance-carry / carry

Clip `carry` (stance, 2.00 s, loop, key poses extreme_a 1.10s, extreme_b 1.60s). State: server stance `run` from `none`, stills at f030, spacing 1.80.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/stance-carry/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/carry/contact.png` (8 frames front and side)<br>`states/stance-carry/delta.png` (speed of the pose per tick) | contact f0-f7 front and side: small, slow beats (sword settles, shield arm resettles) on a held weight; the live strip holds the stance with no float |
| Arcs | 8/10 | `clips/carry/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/carry/turnaround.png` (key poses from 8 angles) | trail: the weapon tip moves on short smooth loops; turnaround: key poses differ by a few degrees, as a held stance should |
| Anticipation and follow-through | 7/10 | `clips/carry/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-carry/strip.png` (the live state) | extreme_a / extreme_b rows: right_hand and left_forearm lag the arm by 0.08-0.12 s; little anticipation, a held stance has little to anticipate |
| Silhouette at distance | 8/10 | `states/stance-carry/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-carry/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | level_zoom: board, helm and the sword point over the rim read at 30 blocks; the guard and wall line reads as a shield line |
| Variation (no clones) | 7/10 | `states/stance-carry/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-carry/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-carry/formed_f030.png` (the whole formation frame) | formed_zoom: per-unit posture, seeds and life layer vary the line, but stance phase is close across units, so neighbours look alike |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/run/loco.png` (run)<br>`loco/run/loco_trace.png` (run traces)<br>`clips/carry/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/carry/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-carry/formed_f030.png` (neighbours at real spacing)<br>`states/stance-carry/close_f030.png` (the trio at real spacing, close)<br>`states/stance-carry/dense_f010.png` (every frame round the event) | turnaround 0-315: no blade through body or board; at real spacing 0.70 the boards overlap edge over edge (wall, guard) as designed, not through a neighbour's body |
| Blend continuity (no pops) | 6/10 | `states/stance-carry/dense_f010.png` (every frame round the event)<br>`states/stance-carry/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/guard-carry/dense_f020.png` (a related transition)<br>`loco/run/dense_idle-to-move.png` (a related transition)<br>`loco/run/dense_move-to-stop.png` (a related transition) | transition guard-carry flagged: 47.2 deg/tick on right_hand at f075 (the carry's hand pitch 178 against the guard's 90) |

## stance-guard / guard

Clip `guard` (stance, 2.40 s, loop, key poses extreme_a 0.75s, extreme_b 1.55s). State: server stance `hold` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/stance-guard/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/guard/contact.png` (8 frames front and side)<br>`states/stance-guard/delta.png` (speed of the pose per tick) | contact f0-f7 front and side: small, slow beats (sword settles, shield arm resettles) on a held weight; the live strip holds the stance with no float |
| Arcs | 8/10 | `clips/guard/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/guard/turnaround.png` (key poses from 8 angles) | trail: the weapon tip moves on short smooth loops; turnaround: key poses differ by a few degrees, as a held stance should |
| Anticipation and follow-through | 7/10 | `clips/guard/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-guard/strip.png` (the live state) | extreme_a / extreme_b rows: right_hand and left_forearm lag the arm by 0.08-0.12 s; little anticipation, a held stance has little to anticipate |
| Silhouette at distance | 8/10 | `states/stance-guard/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-guard/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | level_zoom: board, helm and the sword point over the rim read at 30 blocks; the guard and wall line reads as a shield line |
| Variation (no clones) | 7/10 | `states/stance-guard/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-guard/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-guard/formed_f030.png` (the whole formation frame) | formed_zoom: per-unit posture, seeds and life layer vary the line, but stance phase is close across units, so neighbours look alike |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/guard/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/guard/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-guard/formed_f030.png` (neighbours at real spacing)<br>`states/stance-guard/close_f030.png` (the trio at real spacing, close)<br>`states/stance-guard/dense_f010.png` (every frame round the event) | turnaround 0-315: no blade through body or board; at real spacing 0.70 the boards overlap edge over edge (wall, guard) as designed, not through a neighbour's body |
| Blend continuity (no pops) | 6/10 | `states/stance-guard/dense_f010.png` (every frame round the event)<br>`states/stance-guard/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/guard-carry/dense_f020.png` (a related transition)<br>`loco/crawl/dense_idle-to-move.png` (a related transition)<br>`loco/crawl/dense_move-to-stop.png` (a related transition)<br>`loco/backpedal/dense_idle-to-move.png` (a related transition)<br>`loco/backpedal/dense_move-to-stop.png` (a related transition) | transition guard-carry flagged: 47.2 deg/tick on right_hand at f075 (the carry's hand pitch 178 against the guard's 90) |

## stance-brace / brace

Clip `brace` (stance, 3.00 s, loop, key poses extreme_a 0.55s, extreme_b 2.10s). State: server stance `brace` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/stance-brace/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/brace/contact.png` (8 frames front and side)<br>`states/stance-brace/delta.png` (speed of the pose per tick) | contact f0-f7 front and side: small, slow beats (sword settles, shield arm resettles) on a held weight; the live strip holds the stance with no float |
| Arcs | 8/10 | `clips/brace/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/brace/turnaround.png` (key poses from 8 angles) | trail: the weapon tip moves on short smooth loops; turnaround: key poses differ by a few degrees, as a held stance should |
| Anticipation and follow-through | 7/10 | `clips/brace/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-brace/strip.png` (the live state) | extreme_a / extreme_b rows: right_hand and left_forearm lag the arm by 0.08-0.12 s; little anticipation, a held stance has little to anticipate |
| Silhouette at distance | 8/10 | `states/stance-brace/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-brace/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | level_zoom: board, helm and the sword point over the rim read at 30 blocks; the guard and wall line reads as a shield line |
| Variation (no clones) | 7/10 | `states/stance-brace/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-brace/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-brace/formed_f030.png` (the whole formation frame) | formed_zoom: per-unit posture, seeds and life layer vary the line, but stance phase is close across units, so neighbours look alike |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/crawl/loco.png` (shield crawl)<br>`loco/crawl/loco_trace.png` (crawl traces)<br>`clips/brace/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/brace/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-brace/formed_f030.png` (neighbours at real spacing)<br>`states/stance-brace/close_f030.png` (the trio at real spacing, close)<br>`states/stance-brace/dense_f010.png` (every frame round the event) | turnaround 0-315: no blade through body or board; at real spacing 0.70 the boards overlap edge over edge (wall, guard) as designed, not through a neighbour's body |
| Blend continuity (no pops) | 6/10 | `states/stance-brace/dense_f010.png` (every frame round the event)<br>`states/stance-brace/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hold-wall-brace/dense_f020.png` (a related transition)<br>`loco/crawl/dense_idle-to-move.png` (a related transition)<br>`loco/crawl/dense_move-to-stop.png` (a related transition)<br>`loco/backpedal/dense_idle-to-move.png` (a related transition)<br>`loco/backpedal/dense_move-to-stop.png` (a related transition) | transition hold-wall-brace flagged: 38.9 deg/tick on right_hand at f025 (the wall's hand pitch 40 against the guard's 90 blends in a few ticks) |

## stance-wall / wall

Clip `wall` (stance, 3.00 s, loop, key poses extreme_a 1.00s, extreme_b 2.50s). State: server stance `wall` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/stance-wall/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/wall/contact.png` (8 frames front and side)<br>`states/stance-wall/delta.png` (speed of the pose per tick) | contact f0-f7 front and side: small, slow beats (sword settles, shield arm resettles) on a held weight; the live strip holds the stance with no float |
| Arcs | 8/10 | `clips/wall/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/wall/turnaround.png` (key poses from 8 angles) | trail: the weapon tip moves on short smooth loops; turnaround: key poses differ by a few degrees, as a held stance should |
| Anticipation and follow-through | 7/10 | `clips/wall/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-wall/strip.png` (the live state) | extreme_a / extreme_b rows: right_hand and left_forearm lag the arm by 0.08-0.12 s; little anticipation, a held stance has little to anticipate |
| Silhouette at distance | 8/10 | `states/stance-wall/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-wall/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | level_zoom: board, helm and the sword point over the rim read at 30 blocks; the guard and wall line reads as a shield line |
| Variation (no clones) | 7/10 | `states/stance-wall/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-wall/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-wall/formed_f030.png` (the whole formation frame) | formed_zoom: per-unit posture, seeds and life layer vary the line, but stance phase is close across units, so neighbours look alike |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/crawl/loco.png` (shield crawl)<br>`loco/crawl/loco_trace.png` (crawl traces)<br>`clips/wall/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/wall/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-wall/formed_f030.png` (neighbours at real spacing)<br>`states/stance-wall/close_f030.png` (the trio at real spacing, close)<br>`states/stance-wall/dense_f010.png` (every frame round the event) | turnaround 0-315: no blade through body or board; at real spacing 0.70 the boards overlap edge over edge (wall, guard) as designed, not through a neighbour's body |
| Blend continuity (no pops) | 6/10 | `states/stance-wall/dense_f010.png` (every frame round the event)<br>`states/stance-wall/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hold-wall-brace/dense_f020.png` (a related transition) | transition hold-wall-brace flagged: 38.9 deg/tick on right_hand at f025 (the wall's hand pitch 40 against the guard's 90 blends in a few ticks) |

## stance-shield / shield

Clip `shield` (stance, 2.00 s, loop, key poses extreme_a 0.65s, extreme_b 1.45s). State: server stance `shield_move` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/stance-shield/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/shield/contact.png` (8 frames front and side)<br>`states/stance-shield/delta.png` (speed of the pose per tick) | contact f0-f7 front and side: small, slow beats (sword settles, shield arm resettles) on a held weight; the live strip holds the stance with no float |
| Arcs | 8/10 | `clips/shield/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/shield/turnaround.png` (key poses from 8 angles) | trail: the weapon tip moves on short smooth loops; turnaround: key poses differ by a few degrees, as a held stance should |
| Anticipation and follow-through | 7/10 | `clips/shield/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-shield/strip.png` (the live state) | extreme_a / extreme_b rows: right_hand and left_forearm lag the arm by 0.08-0.12 s; little anticipation, a held stance has little to anticipate |
| Silhouette at distance | 8/10 | `states/stance-shield/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-shield/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | level_zoom: board, helm and the sword point over the rim read at 30 blocks; the guard and wall line reads as a shield line |
| Variation (no clones) | 7/10 | `states/stance-shield/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-shield/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-shield/formed_f030.png` (the whole formation frame) | formed_zoom: per-unit posture, seeds and life layer vary the line, but stance phase is close across units, so neighbours look alike |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/crawl/loco.png` (shield crawl)<br>`loco/crawl/loco_trace.png` (crawl traces)<br>`clips/shield/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/shield/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-shield/formed_f030.png` (neighbours at real spacing)<br>`states/stance-shield/close_f030.png` (the trio at real spacing, close)<br>`states/stance-shield/dense_f010.png` (every frame round the event) | turnaround 0-315: no blade through body or board; at real spacing 0.70 the boards overlap edge over edge (wall, guard) as designed, not through a neighbour's body |
| Blend continuity (no pops) | 8/10 | `states/stance-shield/dense_f010.png` (every frame round the event)<br>`states/stance-shield/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/shield-up-down/dense_f020.png` (a related transition)<br>`loco/crawl/dense_idle-to-move.png` (a related transition)<br>`loco/crawl/dense_move-to-stop.png` (a related transition) | dense_f010 and delta: no flagged window in the state itself |

## attack-sword_shield_thrust / sword_shield_thrust

Clip `sword_shield_thrust` (attack, 1.40 s, one-shot, key poses windup 0.10s, impact 0.35s, follow 0.90s, recovery 1.40s). State: server stance `hold` from `hold`, stills at f017, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/attack-sword_shield_thrust/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/sword_shield_thrust/contact.png` (8 frames front and side)<br>`states/attack-sword_shield_thrust/delta.png` (speed of the pose per tick) | contact: windup 0.15 s, impact on the impact tick, then a 0.7-0.9 s settle; the body drops and lunges into the blow (root ty up to 2 px, tz -2 px) |
| Arcs | 8/10 | `clips/sword_shield_thrust/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/sword_shield_thrust/turnaround.png` (key poses from 8 angles) | trail and turnaround: the tip travels a clean arc; thrust impact front f2 is foreshortened (blade toward the camera, 15 deg inward and 14 deg down, checked in rig space); the 90 deg side shows the full reach |
| Anticipation and follow-through | 8/10 | `clips/sword_shield_thrust/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/attack-sword_shield_thrust/strip.png` (the live state) | windup row: the shoulder coils back and the shield turns in to cover; follow row: overreach, then reel back with the shield closing |
| Silhouette at distance | 8/10 | `states/attack-sword_shield_thrust/level_f017.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/attack-sword_shield_thrust/level_zoom_f017.png` (the same, the troops zoomed nearest neighbour) | level shot: the lunge and the blade read at 30 blocks; synchronised volley |
| Variation (no clones) | 6/10 | `states/attack-sword_shield_thrust/formed_zoom_f017.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/attack-sword_shield_thrust/crowd_zoom_f017.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/attack-sword_shield_thrust/formed_f017.png` (the whole formation frame) | every unit swings the same clip on the same tick (M1 volleys), so the line strikes in unison; only amplitude and seed differ |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/sword_shield_thrust/contact.png` (the feet rows of the clip) | feet rows: soles -0.11 to -0.14 at impact as the root drops while the feet stay planted with tilted shins. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 7/10 | `clips/sword_shield_thrust/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/attack-sword_shield_thrust/formed_f017.png` (neighbours at real spacing)<br>`states/attack-sword_shield_thrust/close_f017.png` (the trio at real spacing, close)<br>`states/attack-sword_shield_thrust/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f039.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f070.png` (every frame round the event) | turnaround impact: the shield swings wide and edge-on to let the point over its rim; the weapon-tip marker sits off the knight's sword tip (harness marker offset); no blade through the body seen |
| Blend continuity (no pops) | 6/10 | `states/attack-sword_shield_thrust/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f039.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f070.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition)<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | hit-while-attacking 121 deg/tick (right_forearm, f025) and death-while-attacking 143 deg/tick (f030) are flagged; the attack alone has no flagged window |

## attack-sword_shield_overhead / sword_shield_overhead

Clip `sword_shield_overhead` (attack, 1.20 s, one-shot, key poses windup 0.35s, impact 0.50s, follow 0.55s, recovery 1.20s). State: server stance `hold` from `hold`, stills at f020, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/attack-sword_shield_overhead/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/sword_shield_overhead/contact.png` (8 frames front and side)<br>`states/attack-sword_shield_overhead/delta.png` (speed of the pose per tick) | contact: windup 0.15 s, impact on the impact tick, then a 0.7-0.9 s settle; the body drops and lunges into the blow (root ty up to 2 px, tz -2 px) |
| Arcs | 8/10 | `clips/sword_shield_overhead/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/sword_shield_overhead/turnaround.png` (key poses from 8 angles) | trail and turnaround: the tip travels a clean arc; thrust impact front f2 is foreshortened (blade toward the camera, 15 deg inward and 14 deg down, checked in rig space); the 90 deg side shows the full reach |
| Anticipation and follow-through | 8/10 | `clips/sword_shield_overhead/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/attack-sword_shield_overhead/strip.png` (the live state) | windup row: the shoulder coils back and the shield turns in to cover; follow row: overreach, then reel back with the shield closing |
| Silhouette at distance | 8/10 | `states/attack-sword_shield_overhead/level_f020.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/attack-sword_shield_overhead/level_zoom_f020.png` (the same, the troops zoomed nearest neighbour) | level shot: the lunge and the blade read at 30 blocks; synchronised volley |
| Variation (no clones) | 6/10 | `states/attack-sword_shield_overhead/formed_zoom_f020.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/attack-sword_shield_overhead/crowd_zoom_f020.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/attack-sword_shield_overhead/formed_f020.png` (the whole formation frame) | every unit swings the same clip on the same tick (M1 volleys), so the line strikes in unison; only amplitude and seed differ |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/sword_shield_overhead/contact.png` (the feet rows of the clip) | feet rows: soles -0.11 to -0.14 at impact as the root drops while the feet stay planted with tilted shins. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 7/10 | `clips/sword_shield_overhead/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/attack-sword_shield_overhead/formed_f020.png` (neighbours at real spacing)<br>`states/attack-sword_shield_overhead/close_f020.png` (the trio at real spacing, close)<br>`states/attack-sword_shield_overhead/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f035.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f070.png` (every frame round the event) | turnaround impact: the shield swings wide and edge-on to let the point over its rim; the weapon-tip marker sits off the knight's sword tip (harness marker offset); no blade through the body seen |
| Blend continuity (no pops) | 6/10 | `states/attack-sword_shield_overhead/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f035.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f070.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition)<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | hit-while-attacking 121 deg/tick (right_forearm, f025) and death-while-attacking 143 deg/tick (f030) are flagged; the attack alone has no flagged window |

## attack-sword_shield_low_cut / sword_shield_low_cut

Clip `sword_shield_low_cut` (attack, 1.10 s, one-shot, key poses windup 0.10s, impact 0.40s, follow 0.90s, recovery 1.10s). State: server stance `hold` from `hold`, stills at f018, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/attack-sword_shield_low_cut/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/sword_shield_low_cut/contact.png` (8 frames front and side)<br>`states/attack-sword_shield_low_cut/delta.png` (speed of the pose per tick) | contact: windup 0.15 s, impact on the impact tick, then a 0.7-0.9 s settle; the body drops and lunges into the blow (root ty up to 2 px, tz -2 px) |
| Arcs | 8/10 | `clips/sword_shield_low_cut/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/sword_shield_low_cut/turnaround.png` (key poses from 8 angles) | trail and turnaround: the tip travels a clean arc; thrust impact front f2 is foreshortened (blade toward the camera, 15 deg inward and 14 deg down, checked in rig space); the 90 deg side shows the full reach |
| Anticipation and follow-through | 8/10 | `clips/sword_shield_low_cut/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/attack-sword_shield_low_cut/strip.png` (the live state) | windup row: the shoulder coils back and the shield turns in to cover; follow row: overreach, then reel back with the shield closing |
| Silhouette at distance | 8/10 | `states/attack-sword_shield_low_cut/level_f018.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/attack-sword_shield_low_cut/level_zoom_f018.png` (the same, the troops zoomed nearest neighbour) | level shot: the lunge and the blade read at 30 blocks; synchronised volley |
| Variation (no clones) | 6/10 | `states/attack-sword_shield_low_cut/formed_zoom_f018.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/attack-sword_shield_low_cut/crowd_zoom_f018.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/attack-sword_shield_low_cut/formed_f018.png` (the whole formation frame) | every unit swings the same clip on the same tick (M1 volleys), so the line strikes in unison; only amplitude and seed differ |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/sword_shield_low_cut/contact.png` (the feet rows of the clip) | feet rows: soles -0.11 to -0.14 at impact as the root drops while the feet stay planted with tilted shins. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 7/10 | `clips/sword_shield_low_cut/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/attack-sword_shield_low_cut/formed_f018.png` (neighbours at real spacing)<br>`states/attack-sword_shield_low_cut/close_f018.png` (the trio at real spacing, close)<br>`states/attack-sword_shield_low_cut/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f033.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f070.png` (every frame round the event) | turnaround impact: the shield swings wide and edge-on to let the point over its rim; the weapon-tip marker sits off the knight's sword tip (harness marker offset); no blade through the body seen |
| Blend continuity (no pops) | 6/10 | `states/attack-sword_shield_low_cut/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f033.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f070.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition)<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | hit-while-attacking 121 deg/tick (right_forearm, f025) and death-while-attacking 143 deg/tick (f030) are flagged; the attack alone has no flagged window |

## hit-head / hit_head

Clip `hit_head` (hit, 0.50 s, one-shot, key poses windup 0.00s, impact 0.05s, follow 0.30s, recovery 0.50s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/hit-head/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/hit_head/contact.png` (8 frames front and side)<br>`states/hit-head/delta.png` (speed of the pose per tick) | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_head/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/hit_head/turnaround.png` (key poses from 8 angles) | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_head/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-head/strip.png` (the live state) | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | `states/hit-head/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-head/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | `states/hit-head/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-head/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-head/formed_f012.png` (the whole formation frame) | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/hit_head/contact.png` (the feet rows of the clip) | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_head/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-head/formed_f012.png` (neighbours at real spacing)<br>`states/hit-head/close_f012.png` (the trio at real spacing, close)<br>`states/hit-head/dense_f010.png` (every frame round the event)<br>`states/hit-head/dense_f021.png` (every frame round the event)<br>`states/hit-head/dense_f070.png` (every frame round the event) | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | `states/hit-head/dense_f010.png` (every frame round the event)<br>`states/hit-head/dense_f021.png` (every frame round the event)<br>`states/hit-head/dense_f070.png` (every frame round the event)<br>`states/hit-head/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit-body / hit_body

Clip `hit_body` (hit, 0.55 s, one-shot, key poses windup 0.00s, impact 0.10s, follow 0.35s, recovery 0.55s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/hit-body/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/hit_body/contact.png` (8 frames front and side)<br>`states/hit-body/delta.png` (speed of the pose per tick) | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_body/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/hit_body/turnaround.png` (key poses from 8 angles) | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_body/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-body/strip.png` (the live state) | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | `states/hit-body/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-body/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | `states/hit-body/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-body/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-body/formed_f012.png` (the whole formation frame) | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/hit_body/contact.png` (the feet rows of the clip) | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_body/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-body/formed_f012.png` (neighbours at real spacing)<br>`states/hit-body/close_f012.png` (the trio at real spacing, close)<br>`states/hit-body/dense_f010.png` (every frame round the event)<br>`states/hit-body/dense_f022.png` (every frame round the event)<br>`states/hit-body/dense_f070.png` (every frame round the event) | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | `states/hit-body/dense_f010.png` (every frame round the event)<br>`states/hit-body/dense_f022.png` (every frame round the event)<br>`states/hit-body/dense_f070.png` (every frame round the event)<br>`states/hit-body/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit-legs / hit_legs

Clip `hit_legs` (hit, 0.60 s, one-shot, key poses windup 0.00s, impact 0.10s, follow 0.40s, recovery 0.60s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/hit-legs/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/hit_legs/contact.png` (8 frames front and side)<br>`states/hit-legs/delta.png` (speed of the pose per tick) | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_legs/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/hit_legs/turnaround.png` (key poses from 8 angles) | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_legs/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-legs/strip.png` (the live state) | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | `states/hit-legs/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-legs/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | `states/hit-legs/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-legs/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-legs/formed_f012.png` (the whole formation frame) | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/hit_legs/contact.png` (the feet rows of the clip) | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_legs/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-legs/formed_f012.png` (neighbours at real spacing)<br>`states/hit-legs/close_f012.png` (the trio at real spacing, close)<br>`states/hit-legs/dense_f010.png` (every frame round the event)<br>`states/hit-legs/dense_f023.png` (every frame round the event)<br>`states/hit-legs/dense_f070.png` (every frame round the event) | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | `states/hit-legs/dense_f010.png` (every frame round the event)<br>`states/hit-legs/dense_f023.png` (every frame round the event)<br>`states/hit-legs/dense_f070.png` (every frame round the event)<br>`states/hit-legs/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit-block / block

Clip `block` (hit, 0.50 s, one-shot, key poses windup 0.00s, impact 0.05s, follow 0.20s, recovery 0.50s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/hit-block/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/block/contact.png` (8 frames front and side)<br>`states/hit-block/delta.png` (speed of the pose per tick) | contact: the board takes the blow in 0.05-0.1 s, the arm gives and recovers in 0.4-0.5 s |
| Arcs | 8/10 | `clips/block/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/block/turnaround.png` (key poses from 8 angles) | trail: the shield rim jolts back and returns on one arc |
| Anticipation and follow-through | 8/10 | `clips/block/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-block/strip.png` (the live state) | the body rocks back with the board and settles forward |
| Silhouette at distance | 8/10 | `states/hit-block/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-block/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | level shot: the board jolt reads, small at 30 blocks |
| Variation (no clones) | 6/10 | `states/hit-block/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-block/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-block/formed_f012.png` (the whole formation frame) | synchronised volleys; block and block_2 alternate by tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/block/contact.png` (the feet rows of the clip) | feet rows -0.05 to -0.04, near baseline. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/block/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-block/formed_f012.png` (neighbours at real spacing)<br>`states/hit-block/close_f012.png` (the trio at real spacing, close)<br>`states/hit-block/dense_f010.png` (every frame round the event)<br>`states/hit-block/dense_f021.png` (every frame round the event)<br>`states/hit-block/dense_f070.png` (every frame round the event) | no clipping seen in the turnaround |
| Blend continuity (no pops) | 8/10 | `states/hit-block/dense_f010.png` (every frame round the event)<br>`states/hit-block/dense_f021.png` (every frame round the event)<br>`states/hit-block/dense_f070.png` (every frame round the event)<br>`states/hit-block/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/shield-up-down/dense_f020.png` (a related transition)<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | no flagged window in the block state |

## scene-fall / fall

Clip `fall` (scene, 1.20 s, one-shot, key poses windup 0.00s, impact 1.00s, follow 1.10s, recovery 1.20s). State: server stance `hold` from `hold`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/scene-fall/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/fall/contact.png` (8 frames front and side)<br>`states/scene-fall/delta.png` (speed of the pose per tick) | fall and raise keep weight in the drop and the climb |
| Arcs | 8/10 | `clips/fall/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/fall/turnaround.png` (key poses from 8 angles) | smooth arcs in the trail |
| Anticipation and follow-through | 7/10 | `clips/fall/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/scene-fall/strip.png` (the live state) | little anticipation before the drop |
| Silhouette at distance | 8/10 | `states/scene-fall/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/scene-fall/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | reads at 30 blocks |
| Variation (no clones) | 7/10 | `states/scene-fall/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/scene-fall/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/scene-fall/formed_f030.png` (the whole formation frame) | units synchronised |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/fall/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/fall/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/scene-fall/formed_f030.png` (neighbours at real spacing)<br>`states/scene-fall/close_f030.png` (the trio at real spacing, close)<br>`states/scene-fall/dense_f010.png` (every frame round the event) | no clipping seen |
| Blend continuity (no pops) | 8/10 | `states/scene-fall/dense_f010.png` (every frame round the event)<br>`states/scene-fall/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there)) | shield-up-down 21 deg/tick, not flagged |

## scene-raise / raise

Clip `raise` (scene, 1.00 s, one-shot, key poses windup 0.10s, impact 0.60s, follow 0.70s, recovery 1.00s). State: server stance `hold` from `hold`, stills at f022, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/scene-raise/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/raise/contact.png` (8 frames front and side)<br>`states/scene-raise/delta.png` (speed of the pose per tick) | fall and raise keep weight in the drop and the climb |
| Arcs | 8/10 | `clips/raise/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/raise/turnaround.png` (key poses from 8 angles) | smooth arcs in the trail |
| Anticipation and follow-through | 7/10 | `clips/raise/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/scene-raise/strip.png` (the live state) | little anticipation before the drop |
| Silhouette at distance | 8/10 | `states/scene-raise/level_f022.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/scene-raise/level_zoom_f022.png` (the same, the troops zoomed nearest neighbour) | reads at 30 blocks |
| Variation (no clones) | 7/10 | `states/scene-raise/formed_zoom_f022.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/scene-raise/crowd_zoom_f022.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/scene-raise/formed_f022.png` (the whole formation frame) | units synchronised |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/raise/contact.png` (the feet rows of the clip) | walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/raise/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/scene-raise/formed_f022.png` (neighbours at real spacing)<br>`states/scene-raise/close_f022.png` (the trio at real spacing, close)<br>`states/scene-raise/dense_f010.png` (every frame round the event) | no clipping seen |
| Blend continuity (no pops) | 8/10 | `states/scene-raise/dense_f010.png` (every frame round the event)<br>`states/scene-raise/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there)) | shield-up-down 21 deg/tick, not flagged |

## death-front / death_slash_front

Clip `death_slash_front` (death, 1.40 s, one-shot, key poses windup 0.00s, impact 1.20s, follow 1.30s, recovery 1.40s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/death-front/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_slash_front/contact.png` (8 frames front and side)<br>`states/death-front/delta.png` (speed of the pose per tick) | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_slash_front/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_slash_front/turnaround.png` (key poses from 8 angles) | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_slash_front/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-front/strip.png` (the live state) | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | `states/death-front/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-front/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-front/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-front/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-front/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-front/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | `states/death-front/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-front/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-front/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-front/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-front/formed_f028.png` (the whole formation frame) | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_slash_front/contact.png` (the feet rows of the clip) | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_slash_front/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-front/formed_f028.png` (neighbours at real spacing)<br>`states/death-front/close_f028.png` (the trio at real spacing, close)<br>`states/death-front/formed_f050.png` (neighbours at real spacing)<br>`states/death-front/close_f050.png` (the trio at real spacing, close)<br>`states/death-front/formed_f080.png` (neighbours at real spacing)<br>`states/death-front/close_f080.png` (the trio at real spacing, close)<br>`states/death-front/dense_f010.png` (every frame round the event) | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | `states/death-front/dense_f010.png` (every frame round the event)<br>`states/death-front/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death-right / death_slash_right

Clip `death_slash_right` (death, 1.40 s, one-shot, key poses windup 0.00s, impact 1.20s, follow 1.30s, recovery 1.40s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/death-right/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_slash_right/contact.png` (8 frames front and side)<br>`states/death-right/delta.png` (speed of the pose per tick) | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_slash_right/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_slash_right/turnaround.png` (key poses from 8 angles) | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_slash_right/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-right/strip.png` (the live state) | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | `states/death-right/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-right/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-right/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-right/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-right/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-right/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | `states/death-right/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-right/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-right/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-right/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-right/formed_f028.png` (the whole formation frame) | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_slash_right/contact.png` (the feet rows of the clip) | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_slash_right/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-right/formed_f028.png` (neighbours at real spacing)<br>`states/death-right/close_f028.png` (the trio at real spacing, close)<br>`states/death-right/formed_f050.png` (neighbours at real spacing)<br>`states/death-right/close_f050.png` (the trio at real spacing, close)<br>`states/death-right/formed_f080.png` (neighbours at real spacing)<br>`states/death-right/close_f080.png` (the trio at real spacing, close)<br>`states/death-right/dense_f010.png` (every frame round the event) | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | `states/death-right/dense_f010.png` (every frame round the event)<br>`states/death-right/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death-back / death_slash_back

Clip `death_slash_back` (death, 1.40 s, one-shot, key poses windup 0.15s, impact 1.20s, follow 1.30s, recovery 1.40s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/death-back/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_slash_back/contact.png` (8 frames front and side)<br>`states/death-back/delta.png` (speed of the pose per tick) | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_slash_back/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_slash_back/turnaround.png` (key poses from 8 angles) | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_slash_back/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-back/strip.png` (the live state) | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | `states/death-back/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-back/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-back/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-back/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-back/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-back/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | `states/death-back/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-back/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-back/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-back/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-back/formed_f028.png` (the whole formation frame) | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_slash_back/contact.png` (the feet rows of the clip) | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_slash_back/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-back/formed_f028.png` (neighbours at real spacing)<br>`states/death-back/close_f028.png` (the trio at real spacing, close)<br>`states/death-back/formed_f050.png` (neighbours at real spacing)<br>`states/death-back/close_f050.png` (the trio at real spacing, close)<br>`states/death-back/formed_f080.png` (neighbours at real spacing)<br>`states/death-back/close_f080.png` (the trio at real spacing, close)<br>`states/death-back/dense_f010.png` (every frame round the event) | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | `states/death-back/dense_f010.png` (every frame round the event)<br>`states/death-back/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death-left / death_slash_left

Clip `death_slash_left` (death, 1.40 s, one-shot, key poses windup 0.00s, impact 1.20s, follow 1.30s, recovery 1.40s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `states/death-left/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_slash_left/contact.png` (8 frames front and side)<br>`states/death-left/delta.png` (speed of the pose per tick) | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_slash_left/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_slash_left/turnaround.png` (key poses from 8 angles) | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_slash_left/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-left/strip.png` (the live state) | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | `states/death-left/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-left/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-left/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-left/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-left/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-left/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | `states/death-left/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-left/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-left/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-left/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-left/formed_f028.png` (the whole formation frame) | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_slash_left/contact.png` (the feet rows of the clip) | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_slash_left/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-left/formed_f028.png` (neighbours at real spacing)<br>`states/death-left/close_f028.png` (the trio at real spacing, close)<br>`states/death-left/formed_f050.png` (neighbours at real spacing)<br>`states/death-left/close_f050.png` (the trio at real spacing, close)<br>`states/death-left/formed_f080.png` (neighbours at real spacing)<br>`states/death-left/close_f080.png` (the trio at real spacing, close)<br>`states/death-left/dense_f010.png` (every frame round the event) | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | `states/death-left/dense_f010.png` (every frame round the event)<br>`states/death-left/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## block_2 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/block_2/contact.png` | contact: the board takes the blow in 0.05-0.1 s, the arm gives and recovers in 0.4-0.5 s |
| Arcs | 8/10 | `clips/block_2/trail.png`, `clips/block_2/turnaround.png` | trail: the shield rim jolts back and returns on one arc |
| Anticipation and follow-through | 8/10 | `clips/block_2/contact.png` | the body rocks back with the board and settles forward |
| Silhouette at distance | 8/10 | n/a | level shot: the board jolt reads, small at 30 blocks |
| Variation (no clones) | 6/10 | n/a | synchronised volleys; block and block_2 alternate by tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/block_2/contact.png` | feet rows -0.05 to -0.04, near baseline. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/block_2/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 8/10 | n/a | no flagged window in the block state |

## hit_body_2 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/hit_body_2/contact.png` | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_body_2/trail.png`, `clips/hit_body_2/turnaround.png` | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_body_2/contact.png` | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | n/a | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | n/a | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/hit_body_2/contact.png` | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_body_2/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | n/a | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit_body_3 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/hit_body_3/contact.png` | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_body_3/trail.png`, `clips/hit_body_3/turnaround.png` | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_body_3/contact.png` | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | n/a | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | n/a | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/hit_body_3/contact.png` | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_body_3/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | n/a | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit_head_2 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/hit_head_2/contact.png` | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_head_2/trail.png`, `clips/hit_head_2/turnaround.png` | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_head_2/contact.png` | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | n/a | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | n/a | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/hit_head_2/contact.png` | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_head_2/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | n/a | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit_head_3 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/hit_head_3/contact.png` | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_head_3/trail.png`, `clips/hit_head_3/turnaround.png` | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_head_3/contact.png` | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | n/a | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | n/a | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/hit_head_3/contact.png` | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_head_3/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | n/a | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit_legs_2 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/hit_legs_2/contact.png` | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_legs_2/trail.png`, `clips/hit_legs_2/turnaround.png` | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_legs_2/contact.png` | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | n/a | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | n/a | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/hit_legs_2/contact.png` | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_legs_2/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | n/a | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## hit_legs_3 (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/hit_legs_3/contact.png` | contact: the jolt peaks at 0.04-0.07 s and settles in 0.5-0.7 s; three variants a zone, picked by tick |
| Arcs | 8/10 | `clips/hit_legs_3/trail.png`, `clips/hit_legs_3/turnaround.png` | trail: the head snaps back and returns on one arc; the legs buckle down and rise |
| Anticipation and follow-through | 7/10 | `clips/hit_legs_3/contact.png` | no anticipation (a hit has none); head and arms lag the body in the follow-through |
| Silhouette at distance | 7/10 | n/a | level shot: head and body jolts are small at 30 blocks; the legs buckle reads |
| Variation (no clones) | 7/10 | n/a | three variants a zone vary the reaction, but volleys land on one tick |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/hit_legs_3/contact.png` | feet rows: the buckle drops the root up to 1.4 px. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/hit_legs_3/turnaround.png` | no clipping seen in the turnaround |
| Blend continuity (no pops) | 6/10 | n/a | hit-while-attacking is flagged (121 deg/tick, right_forearm, f025) |

## death (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death/trail.png`, `clips/death/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_arrow (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_arrow/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_arrow/trail.png`, `clips/death_arrow/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_arrow/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_arrow/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_arrow/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_back (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_back/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_back/trail.png`, `clips/death_back/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_back/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_back/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_back/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_blunt (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_blunt/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_blunt/trail.png`, `clips/death_blunt/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_blunt/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_blunt/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_blunt/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_left (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_left/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_left/trail.png`, `clips/death_left/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_left/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_left/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_left/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_magic (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_magic/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_magic/trail.png`, `clips/death_magic/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_magic/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_magic/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_magic/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_right (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_right/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_right/trail.png`, `clips/death_right/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_right/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_right/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_right/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

## death_stab (no live state)

No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | 8/10 | `clips/death_stab/contact.png` | contact: jolt, stagger, tip, fall, land, bounce, lie over 1.4-1.7 s; the body lands with weight and a small bounce |
| Arcs | 8/10 | `clips/death_stab/trail.png`, `clips/death_stab/turnaround.png` | side 90 frames: the body pivots about its contact point onto the ground in one arc |
| Anticipation and follow-through | 8/10 | `clips/death_stab/contact.png` | a stagger before the tip; the bounce and settle after the land |
| Silhouette at distance | 8/10 | n/a | front f4-f5 carry the vanilla red death tint, as in game; the body on the ground reads at 30 blocks |
| Variation (no clones) | 7/10 | n/a | kind and bearing pick the clip; units killed together fall the same way |
| Contact (no foot slide, no ground sinking) | 4/10 | `clips/death_stab/contact.png` | grounded so the lowest corner rests on the ground; the soles lift as the body lies. walk planted slide 5.54 / 3.90 blocks over 8 plants a foot (largest single plant 1.22), run 5.15 / 3.36; crawl and backpedal read as one long plant each (2.8 and 8.0 blocks of slide); lowest soles -0.07 walk, -0.12 crawl, so the soles sit under the line in the feet rows (magenta). Slide and sink come from the planted-feet solve and the ankle-less rig (a tilted shin tips the sole corner under), not the clip keys; open, engine-level |
| No clipping (body, weapon, shield, neighbour) | 8/10 | `clips/death_stab/turnaround.png` | sword and shield lie with the body; no limb through the ground seen in the side frames |
| Blend continuity (no pops) | 6/10 | n/a | death-while-attacking flagged: 143 deg/tick at f030 (right_hand) as the death takes over a swing |

