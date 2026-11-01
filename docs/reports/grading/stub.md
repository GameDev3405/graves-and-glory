# Motion grading: stub

Family `stub` (engine family `sword_shield`, troop `knight`, rig `biped`), synthetic stub clips.
Evidence run: `test-tools/work/grade/stub/20261031-204518` (manifest.json lists every file by clip, state and kind).

Grade each rubric line 1 to 10. A state passes only when every line is 10 (R65). Write what you can see in the named
file: the frame, the angle or the unit, and the fault. A grade with no observable finding does not count.

Re-runs (M3): while every grade cell here is still `__/10`, a re-run rewrites this whole file; once any grade is filled
in, a re-run replaces only the `Evidence run:` line above and leaves the grades and findings alone.

## How the evidence was made

- Pipeline (R75): every sheet is the full TroopMotion pipeline as the game draws it (engine layers, MotionFeet, scene and death handling); clips are asked for only at the input level (stance packets, attack and hit events, scene poses, the server's Tactics.shield, real kills). No sheet is a raw clip with the engine layers off.
- Solo (R74, R68, R76, R77): 3.44 blocks from the troop at 8 angles (0 front, 90 its right), eye 1.14, level; fitted so every clip's extent from every angle fills at most 80 percent of the frame; crop [234, 78, 812, 622] of the frame (the union of the extent's projections plus 10 percent), native, no downscale. Each clip: 8 frames front and side with feet and hands crops, its key poses from 8 angles
  (0 front, 45, 90 its right, 135, 180 back, 225, 270 its left, 315), and its weapon-tip and shield-rim trails front and side.
- Marks on solo and loco shots: white ground line with height ticks every 0.5 block, soles green when planted, red when lifted
  and magenta when sunk more than half a texel (1/32 block) below the ground, each over a grey ground tick; hands yellow, weapon tip orange, shield rim cyan. The marks are computed from the drawn pose, not painted by hand.
- Locomotion (R71): the solo side camera (3.44 blocks, eye 1.14) tracking the troop each tick along x; crop [112, 78, 1056, 622] (the solo crop 15 percent wider each side). Segments: walk, run, shield crawl, backpedal and a 90 degree turn in place, each from idle and back to
  idle. Planted slide and sole heights are in manifest.json `loco`, and summarised below.
- Spacing (R73): real spacing (R73): a shield troop in wall, brace, shield_move or hold stands shieldCell = max(0.70, width + 0.1) = 0.70 apart; any other max(formation 1.80, max(1.3, width + 0.3)) = 1.80. The wide grid (2 x 3) is only the clone-check overview; the wide grid is a clone-check overview only, never evidence of spacing or clipping.
- Crowd distance (R78): the formation and the wide overview from (0, 16.50 over the ground, -26.85) in front, pitched at the grid's middle: the nearest row about 31 blocks away; the formation from (0, eye 1.62, -30.50), level: the nearest row 30.5 blocks away. Cite the level shot for silhouette and the high formation shot for variation.
- Capture: 120 frames at 20 fps; events right after frame 10 (attacks and hits again after 70); strip = every 4th frame;
  dense sheets show every frame from E-10 to E+10 with the per-tick pose delta under each (R72).
- Volleys (M1): every troop gets its attack or hit on the same tick, so crowd and formation shots show synchronised
  volleys. Judge variation by stance phase, amplitude, faces and seeds, not by event timing.
- Deaths (R79): crowd stills at f028 (vanilla death tint, as in game), f050 and f080 (death ticks about 40 and 70).
- Blend (M2): cite the dense sheets and the delta plots, not the GIF.

Measured: the nearest crowd troop was 30.54 blocks from the camera.

| Loco segment | Planted slide R, L (blocks) | Largest single plant R, L | Lowest sole R, L | Body moved |
|---|---|---|---|---|
| walk | 5.802, 5.757 | 1.201, 1.506 | -0.067, -0.067 | 8.46 |
| run | 3.757, 3.517 | 1.226, 1.664 | -0.094, -0.089 | 10.58 |
| crawl | 2.951, 3.171 | 2.951, 3.171 | -0.108, -0.108 | 2.79 |
| backpedal | 6.924, 6.878 | 6.924, 6.878 | -0.054, -0.054 | 6.35 |
| turn | 1.073, 1.728 | 1.073, 1.728 | -0.024, -0.020 | 0.00 |

| Transition | Event | Largest step (deg/tick) at frame | Largest 2nd difference | x baseline p90 | Flagged |
|---|---|---|---|---|---|
| hold-wall-brace | stance wall | 41.9 at f025 (right_arm) | 16.7 | 27.9 | yes |
| guard-carry | stance hold | 33.1 at f074 (left_shin) | 22.0 | 12.9 | yes |
| shield-up-down | raise | 38.6 at f027 (right_arm) | 25.7 | 16.1 | yes |
| hit-while-attacking | hit body | 232.4 at f026 (right_arm) | 296.1 | 79.4 | yes |
| death-while-attacking | kill | 244.1 at f026 (right_arm) | 345.3 | 161.5 | yes |
| loco-walk | move to stop | 108.5 at f068 (right_leg) | 77.8 | 1.5 | no |
| loco-run | move to stop | 173.5 at f049 (right_leg) | 166.5 | 1.5 | no |
| loco-crawl | idle to move | 29.3 at f023 (left_shin) | 26.6 | 3.2 | yes |
| loco-backpedal | move to stop | 47.3 at f055 (right_leg) | 50.7 | 2.7 | no |
| loco-turn | turn starts | 13.8 at f020 (left_leg) | 24.6 | 1.2 | no |

Rubric (R65): Weight and timing; Arcs; Anticipation and follow-through; Silhouette at distance; Variation (no clones); Contact (no foot slide, no ground sinking); No clipping (body, weapon, shield, neighbour); Blend continuity (no pops).

## stance-idle / idle

Clip `idle` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `none` from `hold`, stills at f030, spacing 1.80.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-idle/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/idle/contact.png` (8 frames front and side)<br>`states/stance-idle/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/idle/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/idle/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/idle/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-idle/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-idle/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-idle/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-idle/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-idle/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-idle/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`clips/idle/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/idle/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-idle/formed_f030.png` (neighbours at real spacing)<br>`states/stance-idle/close_f030.png` (the trio at real spacing, close)<br>`states/stance-idle/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-idle/dense_f010.png` (every frame round the event)<br>`states/stance-idle/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`loco/walk/dense_idle-to-move.png` (a related transition)<br>`loco/walk/dense_move-to-stop.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## stance-carry / carry

Clip `carry` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `run` from `none`, stills at f030, spacing 1.80.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-carry/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/carry/contact.png` (8 frames front and side)<br>`states/stance-carry/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/carry/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/carry/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/carry/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-carry/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-carry/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-carry/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-carry/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-carry/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-carry/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/run/loco.png` (run)<br>`loco/run/loco_trace.png` (run traces)<br>`clips/carry/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/carry/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-carry/formed_f030.png` (neighbours at real spacing)<br>`states/stance-carry/close_f030.png` (the trio at real spacing, close)<br>`states/stance-carry/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-carry/dense_f010.png` (every frame round the event)<br>`states/stance-carry/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/guard-carry/dense_f020.png` (a related transition)<br>`loco/walk/dense_idle-to-move.png` (a related transition)<br>`loco/walk/dense_move-to-stop.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## stance-guard / guard

Clip `guard` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `hold` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-guard/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/guard/contact.png` (8 frames front and side)<br>`states/stance-guard/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/guard/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/guard/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/guard/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-guard/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-guard/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-guard/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-guard/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-guard/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-guard/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/guard/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/guard/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-guard/formed_f030.png` (neighbours at real spacing)<br>`states/stance-guard/close_f030.png` (the trio at real spacing, close)<br>`states/stance-guard/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-guard/dense_f010.png` (every frame round the event)<br>`states/stance-guard/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/guard-carry/dense_f020.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## stance-brace / brace

Clip `brace` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `brace` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-brace/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/brace/contact.png` (8 frames front and side)<br>`states/stance-brace/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/brace/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/brace/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/brace/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-brace/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-brace/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-brace/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-brace/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-brace/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-brace/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/crawl/loco.png` (shield crawl)<br>`loco/crawl/loco_trace.png` (crawl traces)<br>`clips/brace/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/brace/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-brace/formed_f030.png` (neighbours at real spacing)<br>`states/stance-brace/close_f030.png` (the trio at real spacing, close)<br>`states/stance-brace/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-brace/dense_f010.png` (every frame round the event)<br>`states/stance-brace/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hold-wall-brace/dense_f020.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## stance-wall / wall

Clip `wall` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `wall` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-wall/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/wall/contact.png` (8 frames front and side)<br>`states/stance-wall/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/wall/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/wall/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/wall/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-wall/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-wall/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-wall/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-wall/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-wall/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-wall/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/crawl/loco.png` (shield crawl)<br>`loco/crawl/loco_trace.png` (crawl traces)<br>`clips/wall/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/wall/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-wall/formed_f030.png` (neighbours at real spacing)<br>`states/stance-wall/close_f030.png` (the trio at real spacing, close)<br>`states/stance-wall/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-wall/dense_f010.png` (every frame round the event)<br>`states/stance-wall/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hold-wall-brace/dense_f020.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## stance-shield / shield

Clip `shield` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `shield_move` from `none`, stills at f030, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-shield/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/shield/contact.png` (8 frames front and side)<br>`states/stance-shield/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/shield/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/shield/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/shield/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-shield/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-shield/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-shield/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-shield/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-shield/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-shield/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/crawl/loco.png` (shield crawl)<br>`loco/crawl/loco_trace.png` (crawl traces)<br>`clips/shield/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/shield/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-shield/formed_f030.png` (neighbours at real spacing)<br>`states/stance-shield/close_f030.png` (the trio at real spacing, close)<br>`states/stance-shield/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-shield/dense_f010.png` (every frame round the event)<br>`states/stance-shield/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/shield-up-down/dense_f020.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## stance-aim / aim

Clip `aim` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `ranged_aim` from `none`, stills at f030, spacing 1.80.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-aim/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/aim/contact.png` (8 frames front and side)<br>`states/stance-aim/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/aim/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/aim/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/aim/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-aim/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-aim/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-aim/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-aim/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-aim/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-aim/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`clips/aim/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/aim/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-aim/formed_f030.png` (neighbours at real spacing)<br>`states/stance-aim/close_f030.png` (the trio at real spacing, close)<br>`states/stance-aim/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-aim/dense_f010.png` (every frame round the event)<br>`states/stance-aim/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there)) | _to fill: frame or angle, what is seen_ |

## stance-rest / rest

Clip `rest` (stance, 2.00 s, loop, key poses extreme_a 0.00s, extreme_b 1.00s). State: server stance `ranged_rest` from `none`, stills at f030, spacing 1.80.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-rest/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/rest/contact.png` (8 frames front and side)<br>`states/stance-rest/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/rest/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/rest/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/rest/contact.png` (key-pose rows: extreme_a, extreme_b)<br>`states/stance-rest/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-rest/level_f030.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/stance-rest/level_zoom_f030.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-rest/formed_zoom_f030.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/stance-rest/crowd_zoom_f030.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/stance-rest/formed_f030.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`clips/rest/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/rest/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/stance-rest/formed_f030.png` (neighbours at real spacing)<br>`states/stance-rest/close_f030.png` (the trio at real spacing, close)<br>`states/stance-rest/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-rest/dense_f010.png` (every frame round the event)<br>`states/stance-rest/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there)) | _to fill: frame or angle, what is seen_ |

## attack-sword_shield_thrust / sword_shield_thrust

Clip `sword_shield_thrust` (attack, 0.95 s, one-shot, key poses windup 0.00s, impact 0.35s, follow 0.65s, recovery 0.95s). State: server stance `hold` from `hold`, stills at f017, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/attack-sword_shield_thrust/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/sword_shield_thrust/contact.png` (8 frames front and side)<br>`states/attack-sword_shield_thrust/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/sword_shield_thrust/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/sword_shield_thrust/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/sword_shield_thrust/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/attack-sword_shield_thrust/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/attack-sword_shield_thrust/level_f017.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/attack-sword_shield_thrust/level_zoom_f017.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/attack-sword_shield_thrust/formed_zoom_f017.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/attack-sword_shield_thrust/crowd_zoom_f017.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/attack-sword_shield_thrust/formed_f017.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/sword_shield_thrust/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/sword_shield_thrust/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/attack-sword_shield_thrust/formed_f017.png` (neighbours at real spacing)<br>`states/attack-sword_shield_thrust/close_f017.png` (the trio at real spacing, close)<br>`states/attack-sword_shield_thrust/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f030.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/attack-sword_shield_thrust/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f030.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/dense_f070.png` (every frame round the event)<br>`states/attack-sword_shield_thrust/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition)<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## attack-sword_shield_overhead / sword_shield_overhead

Clip `sword_shield_overhead` (attack, 1.10 s, one-shot, key poses windup 0.25s, impact 0.50s, follow 0.80s, recovery 1.10s). State: server stance `hold` from `hold`, stills at f020, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/attack-sword_shield_overhead/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/sword_shield_overhead/contact.png` (8 frames front and side)<br>`states/attack-sword_shield_overhead/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/sword_shield_overhead/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/sword_shield_overhead/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/sword_shield_overhead/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/attack-sword_shield_overhead/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/attack-sword_shield_overhead/level_f020.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/attack-sword_shield_overhead/level_zoom_f020.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/attack-sword_shield_overhead/formed_zoom_f020.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/attack-sword_shield_overhead/crowd_zoom_f020.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/attack-sword_shield_overhead/formed_f020.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/sword_shield_overhead/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/sword_shield_overhead/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/attack-sword_shield_overhead/formed_f020.png` (neighbours at real spacing)<br>`states/attack-sword_shield_overhead/close_f020.png` (the trio at real spacing, close)<br>`states/attack-sword_shield_overhead/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f033.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/attack-sword_shield_overhead/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f033.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/dense_f070.png` (every frame round the event)<br>`states/attack-sword_shield_overhead/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition)<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## attack-sword_shield_low_cut / sword_shield_low_cut

Clip `sword_shield_low_cut` (attack, 1.00 s, one-shot, key poses windup 0.20s, impact 0.40s, follow 0.70s, recovery 1.00s). State: server stance `hold` from `hold`, stills at f018, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/attack-sword_shield_low_cut/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/sword_shield_low_cut/contact.png` (8 frames front and side)<br>`states/attack-sword_shield_low_cut/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/sword_shield_low_cut/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/sword_shield_low_cut/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/sword_shield_low_cut/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/attack-sword_shield_low_cut/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/attack-sword_shield_low_cut/level_f018.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/attack-sword_shield_low_cut/level_zoom_f018.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/attack-sword_shield_low_cut/formed_zoom_f018.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/attack-sword_shield_low_cut/crowd_zoom_f018.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/attack-sword_shield_low_cut/formed_f018.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/sword_shield_low_cut/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/sword_shield_low_cut/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/attack-sword_shield_low_cut/formed_f018.png` (neighbours at real spacing)<br>`states/attack-sword_shield_low_cut/close_f018.png` (the trio at real spacing, close)<br>`states/attack-sword_shield_low_cut/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f031.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/attack-sword_shield_low_cut/dense_f010.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f031.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/dense_f070.png` (every frame round the event)<br>`states/attack-sword_shield_low_cut/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition)<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## hit-head / hit_head

Clip `hit_head` (hit, 0.50 s, one-shot, key poses windup 0.00s, impact 0.10s, follow 0.30s, recovery 0.50s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-head/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/hit_head/contact.png` (8 frames front and side)<br>`states/hit-head/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/hit_head/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/hit_head/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/hit_head/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-head/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-head/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-head/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-head/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-head/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-head/formed_f012.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/hit_head/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/hit_head/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-head/formed_f012.png` (neighbours at real spacing)<br>`states/hit-head/close_f012.png` (the trio at real spacing, close)<br>`states/hit-head/dense_f010.png` (every frame round the event)<br>`states/hit-head/dense_f021.png` (every frame round the event)<br>`states/hit-head/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-head/dense_f010.png` (every frame round the event)<br>`states/hit-head/dense_f021.png` (every frame round the event)<br>`states/hit-head/dense_f070.png` (every frame round the event)<br>`states/hit-head/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## hit-body / hit_body

Clip `hit_body` (hit, 0.50 s, one-shot, key poses windup 0.00s, impact 0.10s, follow 0.30s, recovery 0.50s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-body/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/hit_body/contact.png` (8 frames front and side)<br>`states/hit-body/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/hit_body/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/hit_body/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/hit_body/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-body/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-body/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-body/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-body/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-body/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-body/formed_f012.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/hit_body/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/hit_body/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-body/formed_f012.png` (neighbours at real spacing)<br>`states/hit-body/close_f012.png` (the trio at real spacing, close)<br>`states/hit-body/dense_f010.png` (every frame round the event)<br>`states/hit-body/dense_f021.png` (every frame round the event)<br>`states/hit-body/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-body/dense_f010.png` (every frame round the event)<br>`states/hit-body/dense_f021.png` (every frame round the event)<br>`states/hit-body/dense_f070.png` (every frame round the event)<br>`states/hit-body/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## hit-legs / hit_legs

Clip `hit_legs` (hit, 0.50 s, one-shot, key poses windup 0.00s, impact 0.10s, follow 0.30s, recovery 0.50s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-legs/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/hit_legs/contact.png` (8 frames front and side)<br>`states/hit-legs/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/hit_legs/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/hit_legs/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/hit_legs/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-legs/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-legs/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-legs/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-legs/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-legs/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-legs/formed_f012.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/hit_legs/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/hit_legs/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-legs/formed_f012.png` (neighbours at real spacing)<br>`states/hit-legs/close_f012.png` (the trio at real spacing, close)<br>`states/hit-legs/dense_f010.png` (every frame round the event)<br>`states/hit-legs/dense_f021.png` (every frame round the event)<br>`states/hit-legs/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-legs/dense_f010.png` (every frame round the event)<br>`states/hit-legs/dense_f021.png` (every frame round the event)<br>`states/hit-legs/dense_f070.png` (every frame round the event)<br>`states/hit-legs/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## hit-block / block

Clip `block` (hit, 0.50 s, one-shot, key poses windup 0.00s, impact 0.10s, follow 0.30s, recovery 0.50s). State: server stance `hold` from `hold`, stills at f012, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-block/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/block/contact.png` (8 frames front and side)<br>`states/hit-block/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/block/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/block/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/block/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/hit-block/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-block/level_f012.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/hit-block/level_zoom_f012.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-block/formed_zoom_f012.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/hit-block/crowd_zoom_f012.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/hit-block/formed_f012.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/block/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/block/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/hit-block/formed_f012.png` (neighbours at real spacing)<br>`states/hit-block/close_f012.png` (the trio at real spacing, close)<br>`states/hit-block/dense_f010.png` (every frame round the event)<br>`states/hit-block/dense_f021.png` (every frame round the event)<br>`states/hit-block/dense_f070.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-block/dense_f010.png` (every frame round the event)<br>`states/hit-block/dense_f021.png` (every frame round the event)<br>`states/hit-block/dense_f070.png` (every frame round the event)<br>`states/hit-block/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/shield-up-down/dense_f020.png` (a related transition)<br>`transitions/hit-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## scene-fall / fall

Clip `fall` (scene, 1.50 s, one-shot, key poses windup 0.00s, impact 0.90s, follow 1.20s, recovery 1.50s). State: server stance `hold` from `hold`, stills at f028, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/scene-fall/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/fall/contact.png` (8 frames front and side)<br>`states/scene-fall/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/fall/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/fall/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/fall/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/scene-fall/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/scene-fall/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/scene-fall/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/scene-fall/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/scene-fall/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/scene-fall/formed_f028.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/fall/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/fall/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/scene-fall/formed_f028.png` (neighbours at real spacing)<br>`states/scene-fall/close_f028.png` (the trio at real spacing, close)<br>`states/scene-fall/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/scene-fall/dense_f010.png` (every frame round the event)<br>`states/scene-fall/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there)) | _to fill: frame or angle, what is seen_ |

## scene-raise / raise

Clip `raise` (scene, 1.50 s, one-shot, key poses windup 0.00s, impact 0.90s, follow 1.20s, recovery 1.50s). State: server stance `hold` from `hold`, stills at f028, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/scene-raise/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/raise/contact.png` (8 frames front and side)<br>`states/scene-raise/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/raise/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/raise/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/raise/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/scene-raise/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/scene-raise/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/scene-raise/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/scene-raise/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/scene-raise/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/scene-raise/formed_f028.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/raise/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/raise/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/scene-raise/formed_f028.png` (neighbours at real spacing)<br>`states/scene-raise/close_f028.png` (the trio at real spacing, close)<br>`states/scene-raise/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/scene-raise/dense_f010.png` (every frame round the event)<br>`states/scene-raise/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there)) | _to fill: frame or angle, what is seen_ |

## death-front / death

Clip `death` (death, 1.50 s, one-shot, key poses windup 0.00s, impact 0.90s, follow 1.20s, recovery 1.50s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-front/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death/contact.png` (8 frames front and side)<br>`states/death-front/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-front/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-front/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-front/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-front/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-front/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-front/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-front/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-front/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-front/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-front/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-front/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-front/formed_f028.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-front/formed_f028.png` (neighbours at real spacing)<br>`states/death-front/close_f028.png` (the trio at real spacing, close)<br>`states/death-front/formed_f050.png` (neighbours at real spacing)<br>`states/death-front/close_f050.png` (the trio at real spacing, close)<br>`states/death-front/formed_f080.png` (neighbours at real spacing)<br>`states/death-front/close_f080.png` (the trio at real spacing, close)<br>`states/death-front/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-front/dense_f010.png` (every frame round the event)<br>`states/death-front/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## death-right / death_right

Clip `death_right` (death, 1.50 s, one-shot, key poses windup 0.00s, impact 0.90s, follow 1.20s, recovery 1.50s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-right/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_right/contact.png` (8 frames front and side)<br>`states/death-right/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death_right/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_right/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death_right/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-right/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-right/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-right/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-right/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-right/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-right/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-right/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-right/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-right/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-right/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-right/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-right/formed_f028.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_right/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death_right/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-right/formed_f028.png` (neighbours at real spacing)<br>`states/death-right/close_f028.png` (the trio at real spacing, close)<br>`states/death-right/formed_f050.png` (neighbours at real spacing)<br>`states/death-right/close_f050.png` (the trio at real spacing, close)<br>`states/death-right/formed_f080.png` (neighbours at real spacing)<br>`states/death-right/close_f080.png` (the trio at real spacing, close)<br>`states/death-right/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-right/dense_f010.png` (every frame round the event)<br>`states/death-right/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## death-back / death_back

Clip `death_back` (death, 1.50 s, one-shot, key poses windup 0.00s, impact 0.90s, follow 1.20s, recovery 1.50s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-back/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_back/contact.png` (8 frames front and side)<br>`states/death-back/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death_back/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_back/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death_back/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-back/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-back/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-back/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-back/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-back/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-back/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-back/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-back/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-back/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-back/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-back/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-back/formed_f028.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_back/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death_back/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-back/formed_f028.png` (neighbours at real spacing)<br>`states/death-back/close_f028.png` (the trio at real spacing, close)<br>`states/death-back/formed_f050.png` (neighbours at real spacing)<br>`states/death-back/close_f050.png` (the trio at real spacing, close)<br>`states/death-back/formed_f080.png` (neighbours at real spacing)<br>`states/death-back/close_f080.png` (the trio at real spacing, close)<br>`states/death-back/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-back/dense_f010.png` (every frame round the event)<br>`states/death-back/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

## death-left / death_left

Clip `death_left` (death, 1.50 s, one-shot, key poses windup 0.00s, impact 0.90s, follow 1.20s, recovery 1.50s). State: server stance `hold` from `hold`, stills at f028, f050, f080, spacing 0.70.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-left/strip.png` (timing of the live state at 20 fps; event after f010)<br>`clips/death_left/contact.png` (8 frames front and side)<br>`states/death-left/delta.png` (speed of the pose per tick) | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death_left/trail.png` (weapon-tip and shield-rim paths, front and side)<br>`clips/death_left/turnaround.png` (key poses from 8 angles) | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death_left/contact.png` (key-pose rows: windup, impact, follow, recovery)<br>`states/death-left/strip.png` (the live state) | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-left/level_f028.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-left/level_zoom_f028.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-left/level_f050.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-left/level_zoom_f050.png` (the same, the troops zoomed nearest neighbour)<br>`states/death-left/level_f080.png` (level shot, eye height 1.62, nearest troop at least 30 blocks)<br>`states/death-left/level_zoom_f080.png` (the same, the troops zoomed nearest neighbour) | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-left/formed_zoom_f028.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-left/formed_zoom_f050.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-left/formed_zoom_f080.png` (the formation at real spacing, from 30 blocks up high, zoomed)<br>`states/death-left/crowd_zoom_f028.png` (the wide overview grid, a clone check only (spacing exaggerated))<br>`states/death-left/formed_f028.png` (the whole formation frame) | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `loco/walk/loco.png` (walk: frames and feet crops)<br>`loco/walk/loco_trace.png` (walk: foot traces; manifest loco[].plantedSlideBlocks)<br>`loco/turn/loco_trace.png` (turn in place)<br>`loco/backpedal/loco.png` (backpedal)<br>`loco/backpedal/loco_trace.png` (backpedal traces)<br>`clips/death_left/contact.png` (the feet rows of the clip) | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death_left/turnaround.png` (body, weapon and shield from 8 angles at each key pose)<br>`states/death-left/formed_f028.png` (neighbours at real spacing)<br>`states/death-left/close_f028.png` (the trio at real spacing, close)<br>`states/death-left/formed_f050.png` (neighbours at real spacing)<br>`states/death-left/close_f050.png` (the trio at real spacing, close)<br>`states/death-left/formed_f080.png` (neighbours at real spacing)<br>`states/death-left/close_f080.png` (the trio at real spacing, close)<br>`states/death-left/dense_f010.png` (every frame round the event) | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-left/dense_f010.png` (every frame round the event)<br>`states/death-left/delta.png` (per-tick pose delta; manifest states[].delta.windows (flag rule there))<br>`transitions/death-while-attacking/dense_f025.png` (a related transition) | _to fill: frame or angle, what is seen_ |

