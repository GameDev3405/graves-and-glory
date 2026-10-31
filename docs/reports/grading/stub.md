# Motion grading: stub

Family `stub` (engine family `sword_shield`, troop `knight`, rig `biped`), synthetic stub clips.
Evidence run: `test-tools/work/grade/stub/20261031-171747` (manifest.json lists every file by clip, state and kind).

Grade each rubric line 1 to 10. A clip/state passes only when every line is 10 (R65). Write what you can see in
the named file: the frame, the angle or the unit, and the fault. A grade with no observable finding does not count.

Camera (fixed, family-agnostic; distances scale with the troop's height, scale 1.00):

- Solo: 4.00 blocks from the troop, eye at height 1.10, level (pitch 0); front = angle 0 (facing it), side = angle 90 (its right); turnaround angles 0-315 step 45; tile = centre square of the frame, full height, scaled to 600x600.
- Close (trio): 5.50 blocks from the trio's middle, 30 degrees toward its right, eye 1.60, pitched 6.2 degrees down to height 1.00; still = centre 0.656 x 0.75 of the frame; capture frames scaled to 560x360.
- Crowd (20 units, distinct seeds): 30 blocks from the grid's middle (eye 16.0, 21.5 blocks in front of the front row), pitched 30 degrees down at it so every row shows over the one before; 5 by 4 grid, columns 2.0 and rows 3.0 blocks apart, all facing the camera; full frame; crowd_zoom = centre 0.2 of the frame at 4x nearest neighbour.
- Capture: 120 frames at 20 fps; events right after frame 10 (attacks and hits again after 70); strip = every 4th frame.

Rubric (R65): Weight and timing; Arcs; Anticipation and follow-through; Silhouette at distance; Variation (no clones); Contact (no foot slide, no ground sinking); No clipping (body, weapon, shield, neighbour); Blend continuity (no pops).

## idle / stance-idle

Clip `idle` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-idle`: server stance `none` from `hold`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-idle/strip.png`, `clips/idle/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/idle/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/idle/contact.png`, `states/stance-idle/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-idle/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-idle/crowd_zoom.png`, `states/stance-idle/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-idle/strip.png`, `clips/idle/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/idle/turnaround.png`, `states/stance-idle/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-idle/strip.png`, `states/stance-idle/capture.gif` | _to fill: frame or angle, what is seen_ |

## carry / stance-carry

Clip `carry` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-carry`: server stance `run` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-carry/strip.png`, `clips/carry/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/carry/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/carry/contact.png`, `states/stance-carry/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-carry/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-carry/crowd_zoom.png`, `states/stance-carry/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-carry/strip.png`, `clips/carry/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/carry/turnaround.png`, `states/stance-carry/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-carry/strip.png`, `states/stance-carry/capture.gif` | _to fill: frame or angle, what is seen_ |

## guard / stance-guard

Clip `guard` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-guard`: server stance `hold` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-guard/strip.png`, `clips/guard/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/guard/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/guard/contact.png`, `states/stance-guard/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-guard/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-guard/crowd_zoom.png`, `states/stance-guard/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-guard/strip.png`, `clips/guard/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/guard/turnaround.png`, `states/stance-guard/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-guard/strip.png`, `states/stance-guard/capture.gif` | _to fill: frame or angle, what is seen_ |

## brace / stance-brace

Clip `brace` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-brace`: server stance `brace` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-brace/strip.png`, `clips/brace/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/brace/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/brace/contact.png`, `states/stance-brace/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-brace/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-brace/crowd_zoom.png`, `states/stance-brace/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-brace/strip.png`, `clips/brace/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/brace/turnaround.png`, `states/stance-brace/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-brace/strip.png`, `states/stance-brace/capture.gif` | _to fill: frame or angle, what is seen_ |

## wall / stance-wall

Clip `wall` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-wall`: server stance `wall` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-wall/strip.png`, `clips/wall/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/wall/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/wall/contact.png`, `states/stance-wall/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-wall/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-wall/crowd_zoom.png`, `states/stance-wall/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-wall/strip.png`, `clips/wall/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/wall/turnaround.png`, `states/stance-wall/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-wall/strip.png`, `states/stance-wall/capture.gif` | _to fill: frame or angle, what is seen_ |

## shield / stance-shield

Clip `shield` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-shield`: server stance `shield_move` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-shield/strip.png`, `clips/shield/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/shield/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/shield/contact.png`, `states/stance-shield/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-shield/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-shield/crowd_zoom.png`, `states/stance-shield/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-shield/strip.png`, `clips/shield/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/shield/turnaround.png`, `states/stance-shield/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-shield/strip.png`, `states/stance-shield/capture.gif` | _to fill: frame or angle, what is seen_ |

## aim / stance-aim

Clip `aim` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-aim`: server stance `ranged_aim` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-aim/strip.png`, `clips/aim/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/aim/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/aim/contact.png`, `states/stance-aim/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-aim/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-aim/crowd_zoom.png`, `states/stance-aim/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-aim/strip.png`, `clips/aim/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/aim/turnaround.png`, `states/stance-aim/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-aim/strip.png`, `states/stance-aim/capture.gif` | _to fill: frame or angle, what is seen_ |

## rest / stance-rest

Clip `rest` (stance, 2.00 s, loop, key 0.00 s by largest summed rotation). State `stance-rest`: server stance `ranged_rest` from `none`, crowd still at frame 30.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/stance-rest/strip.png`, `clips/rest/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/rest/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/rest/contact.png`, `states/stance-rest/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/stance-rest/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/stance-rest/crowd_zoom.png`, `states/stance-rest/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/stance-rest/strip.png`, `clips/rest/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/rest/turnaround.png`, `states/stance-rest/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/stance-rest/strip.png`, `states/stance-rest/capture.gif` | _to fill: frame or angle, what is seen_ |

## sword_shield_thrust / attack-sword_shield_thrust

Clip `sword_shield_thrust` (attack, 0.95 s, one-shot, key 0.35 s by impact marker). State `attack-sword_shield_thrust`: server stance `hold` from `hold`, crowd still at frame 17.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/attack-sword_shield_thrust/strip.png`, `clips/sword_shield_thrust/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/sword_shield_thrust/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/sword_shield_thrust/contact.png`, `states/attack-sword_shield_thrust/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/attack-sword_shield_thrust/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/attack-sword_shield_thrust/crowd_zoom.png`, `states/attack-sword_shield_thrust/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/attack-sword_shield_thrust/strip.png`, `clips/sword_shield_thrust/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/sword_shield_thrust/turnaround.png`, `states/attack-sword_shield_thrust/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/attack-sword_shield_thrust/strip.png`, `states/attack-sword_shield_thrust/capture.gif` | _to fill: frame or angle, what is seen_ |

## sword_shield_overhead / attack-sword_shield_overhead

Clip `sword_shield_overhead` (attack, 1.10 s, one-shot, key 0.50 s by impact marker). State `attack-sword_shield_overhead`: server stance `hold` from `hold`, crowd still at frame 20.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/attack-sword_shield_overhead/strip.png`, `clips/sword_shield_overhead/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/sword_shield_overhead/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/sword_shield_overhead/contact.png`, `states/attack-sword_shield_overhead/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/attack-sword_shield_overhead/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/attack-sword_shield_overhead/crowd_zoom.png`, `states/attack-sword_shield_overhead/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/attack-sword_shield_overhead/strip.png`, `clips/sword_shield_overhead/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/sword_shield_overhead/turnaround.png`, `states/attack-sword_shield_overhead/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/attack-sword_shield_overhead/strip.png`, `states/attack-sword_shield_overhead/capture.gif` | _to fill: frame or angle, what is seen_ |

## sword_shield_low_cut / attack-sword_shield_low_cut

Clip `sword_shield_low_cut` (attack, 1.00 s, one-shot, key 0.40 s by impact marker). State `attack-sword_shield_low_cut`: server stance `hold` from `hold`, crowd still at frame 18.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/attack-sword_shield_low_cut/strip.png`, `clips/sword_shield_low_cut/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/sword_shield_low_cut/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/sword_shield_low_cut/contact.png`, `states/attack-sword_shield_low_cut/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/attack-sword_shield_low_cut/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/attack-sword_shield_low_cut/crowd_zoom.png`, `states/attack-sword_shield_low_cut/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/attack-sword_shield_low_cut/strip.png`, `clips/sword_shield_low_cut/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/sword_shield_low_cut/turnaround.png`, `states/attack-sword_shield_low_cut/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/attack-sword_shield_low_cut/strip.png`, `states/attack-sword_shield_low_cut/capture.gif` | _to fill: frame or angle, what is seen_ |

## hit_head / hit-head

Clip `hit_head` (hit, 0.50 s, one-shot, key 0.10 s by largest summed rotation). State `hit-head`: server stance `hold` from `hold`, crowd still at frame 12.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-head/strip.png`, `clips/hit_head/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/hit_head/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/hit_head/contact.png`, `states/hit-head/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-head/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-head/crowd_zoom.png`, `states/hit-head/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/hit-head/strip.png`, `clips/hit_head/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/hit_head/turnaround.png`, `states/hit-head/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-head/strip.png`, `states/hit-head/capture.gif` | _to fill: frame or angle, what is seen_ |

## hit_body / hit-body

Clip `hit_body` (hit, 0.50 s, one-shot, key 0.10 s by largest summed rotation). State `hit-body`: server stance `hold` from `hold`, crowd still at frame 12.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-body/strip.png`, `clips/hit_body/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/hit_body/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/hit_body/contact.png`, `states/hit-body/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-body/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-body/crowd_zoom.png`, `states/hit-body/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/hit-body/strip.png`, `clips/hit_body/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/hit_body/turnaround.png`, `states/hit-body/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-body/strip.png`, `states/hit-body/capture.gif` | _to fill: frame or angle, what is seen_ |

## hit_legs / hit-legs

Clip `hit_legs` (hit, 0.50 s, one-shot, key 0.10 s by largest summed rotation). State `hit-legs`: server stance `hold` from `hold`, crowd still at frame 12.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-legs/strip.png`, `clips/hit_legs/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/hit_legs/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/hit_legs/contact.png`, `states/hit-legs/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-legs/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-legs/crowd_zoom.png`, `states/hit-legs/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/hit-legs/strip.png`, `clips/hit_legs/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/hit_legs/turnaround.png`, `states/hit-legs/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-legs/strip.png`, `states/hit-legs/capture.gif` | _to fill: frame or angle, what is seen_ |

## block / hit-block

Clip `block` (hit, 0.50 s, one-shot, key 0.10 s by largest summed rotation). State `hit-block`: server stance `hold` from `hold`, crowd still at frame 12.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/hit-block/strip.png`, `clips/block/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/block/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/block/contact.png`, `states/hit-block/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/hit-block/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/hit-block/crowd_zoom.png`, `states/hit-block/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/hit-block/strip.png`, `clips/block/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/block/turnaround.png`, `states/hit-block/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/hit-block/strip.png`, `states/hit-block/capture.gif` | _to fill: frame or angle, what is seen_ |

## fall / scene-fall

Clip `fall` (scene, 1.50 s, one-shot, key 0.90 s by largest summed rotation). State `scene-fall`: server stance `hold` from `hold`, crowd still at frame 28.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/scene-fall/strip.png`, `clips/fall/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/fall/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/fall/contact.png`, `states/scene-fall/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/scene-fall/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/scene-fall/crowd_zoom.png`, `states/scene-fall/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/scene-fall/strip.png`, `clips/fall/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/fall/turnaround.png`, `states/scene-fall/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/scene-fall/strip.png`, `states/scene-fall/capture.gif` | _to fill: frame or angle, what is seen_ |

## raise / scene-raise

Clip `raise` (scene, 1.50 s, one-shot, key 0.90 s by largest summed rotation). State `scene-raise`: server stance `hold` from `hold`, crowd still at frame 28.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/scene-raise/strip.png`, `clips/raise/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/raise/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/raise/contact.png`, `states/scene-raise/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/scene-raise/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/scene-raise/crowd_zoom.png`, `states/scene-raise/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/scene-raise/strip.png`, `clips/raise/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/raise/turnaround.png`, `states/scene-raise/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/scene-raise/strip.png`, `states/scene-raise/capture.gif` | _to fill: frame or angle, what is seen_ |

## death / death-front

Clip `death` (death, 1.50 s, one-shot, key 0.90 s by largest summed rotation). State `death-front`: server stance `hold` from `hold`, crowd still at frame 28.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-front/strip.png`, `clips/death/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death/contact.png`, `states/death-front/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-front/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-front/crowd_zoom.png`, `states/death-front/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/death-front/strip.png`, `clips/death/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death/turnaround.png`, `states/death-front/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-front/strip.png`, `states/death-front/capture.gif` | _to fill: frame or angle, what is seen_ |

## death_right / death-right

Clip `death_right` (death, 1.50 s, one-shot, key 0.90 s by largest summed rotation). State `death-right`: server stance `hold` from `hold`, crowd still at frame 28.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-right/strip.png`, `clips/death_right/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death_right/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death_right/contact.png`, `states/death-right/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-right/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-right/crowd_zoom.png`, `states/death-right/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/death-right/strip.png`, `clips/death_right/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death_right/turnaround.png`, `states/death-right/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-right/strip.png`, `states/death-right/capture.gif` | _to fill: frame or angle, what is seen_ |

## death_back / death-back

Clip `death_back` (death, 1.50 s, one-shot, key 0.90 s by largest summed rotation). State `death-back`: server stance `hold` from `hold`, crowd still at frame 28.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-back/strip.png`, `clips/death_back/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death_back/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death_back/contact.png`, `states/death-back/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-back/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-back/crowd_zoom.png`, `states/death-back/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/death-back/strip.png`, `clips/death_back/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death_back/turnaround.png`, `states/death-back/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-back/strip.png`, `states/death-back/capture.gif` | _to fill: frame or angle, what is seen_ |

## death_left / death-left

Clip `death_left` (death, 1.50 s, one-shot, key 0.90 s by largest summed rotation). State `death-left`: server stance `hold` from `hold`, crowd still at frame 28.

| Rubric line | Grade | Evidence file | Observable findings |
|---|---|---|---|
| Weight and timing | __/10 | `states/death-left/strip.png`, `clips/death_left/contact.png` | _to fill: frame or angle, what is seen_ |
| Arcs | __/10 | `clips/death_left/contact.png` | _to fill: frame or angle, what is seen_ |
| Anticipation and follow-through | __/10 | `clips/death_left/contact.png`, `states/death-left/strip.png` | _to fill: frame or angle, what is seen_ |
| Silhouette at distance | __/10 | `states/death-left/crowd.png` | _to fill: frame or angle, what is seen_ |
| Variation (no clones) | __/10 | `states/death-left/crowd_zoom.png`, `states/death-left/crowd.png` | _to fill: frame or angle, what is seen_ |
| Contact (no foot slide, no ground sinking) | __/10 | `states/death-left/strip.png`, `clips/death_left/contact.png` | _to fill: frame or angle, what is seen_ |
| No clipping (body, weapon, shield, neighbour) | __/10 | `clips/death_left/turnaround.png`, `states/death-left/close.png` | _to fill: frame or angle, what is seen_ |
| Blend continuity (no pops) | __/10 | `states/death-left/strip.png`, `states/death-left/capture.gif` | _to fill: frame or angle, what is seen_ |

