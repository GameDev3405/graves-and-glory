# Spear and trident troops: animation spec

Kort, 2026-11-03 request: find good game-animation videos for each weapon, break the moves down, and apply them to the units properly. Branch `work/anim-rework`. Docs only; no clip, Java or sheet was touched. Reference folder `C:/Users/kortt/Melty/anim-refs/spear/` (not committed).

**Reference status: checked.** The main reference is Thunder Cloud Studio's "Spear Animation Demonstration" (youtu.be/Yk4NFoRhw1g, one figure on a plain background; local `anim-refs/spear/thunder-spear.mkv`). Timestamps below were extracted at 10 fps and looked at. Kort's GameDevHero videos (V1, V2) and the 9CG pack are catalogues of many tiny figures with no readable pose; the Ammar video is a dark combat montage, mood only. Details in `anim-refs/spear/SOURCES.md`. The video has no braced hold, no shield-wall line and no low sweep with a low wind-up, so those parts stay from the weapon-direction rule and the 2026-10-29 spec.

## Why

The spear family has no clips at all. Its folder `clips/biped/spear/` does not exist, and `clips/biped/common/` holds only hits, deaths, fall and raise, no stances. So today every stance and every swing of the three spear troops is the procedural fallback, and no spear attack plays (`TroopMotion.build` leaves `attacks` empty, so `MotionEngine.events` starts nothing). Kort wants the Souls-like feel the knight now has: low weight, big wind-ups, hips driving the blow. For a spear that means a levelled shaft, a point that always leads, and a braced line.

## Goals

- Three loose strikes and their braced (`_wall`) versions, a `brace` hold, `guard`, `carry` and `idle`, all authored for a two-handed spear or trident.
- On every key the weapon points where the move needs (see Moves). A pose with the point wrong is a defect even if clean.
- No clipping of the shaft through head, legs or neighbours in a brace line.
- Each move approved by Kort one at a time in game, as for the knight.

## Units

All three are family `spear`, rig `biped`, weapon `minecraft:trident`, shield `none`, no off-hand item, so the trident is held two-handed (right hand holds it; the left hand is posed onto the shaft by clip keys).

| Troop | Entity / skin | Tags | Ability | Notes |
|---|---|---|---|---|
| `wm_tusk` (Tusk Spear, Wildmoot, rare) | zombified_piglin | spear | brace (x1.3 vs charging enemies on Hold) | leather cap and chest, hp 30, speed 0.31, cooldown 28 |
| `tc_tideguard` (Trident Guard, Tidecallers, rare) | drowned | spear | brace | iron helm, chainmail, hp 34, cooldown 28; keeps `tc_tideguard` evolve line |
| `tc_admiral` (Admiral Vael, legendary) | drowned | spear, large | undertow (slam, timer 200 ticks, radius 7, pulls enemies) | netherite chest, golden helm, hp 64, slotWeight 4, cooldown 26 |

Formation: spear troops never take the shield `wall` stance. `BattleMath.holdStance` returns `brace` for a spear troop on Hold, so `tc_tideguard` and `wm_tusk` form a braced line, not a wall; the `_wall` clips play from `brace` (`MotionEngine.events`: stance wall or brace). Scale: `large` (Admiral) is the only size flag; the model scale itself is set by the troop model, which does not exist yet for these three (see Questions).

## Inventory today

| Engine slot | Clip(s) the engine looks for | Today |
|---|---|---|
| Stances idle, carry, guard, brace | `spear/<name>`, else `common/<name>` | none in either folder; chain falls to procedural (`MotionEngine.stanceChain`: brace to guard to carry to idle to procedural) |
| `shield`, `wall` | not used (no shield) | n/a |
| `aim`, `rest` | not used | n/a |
| Attacks `spear_thrust`, `spear_high_thrust`, `spear_low_sweep` | `spear/<id>` | missing; no attack clip plays |
| `<id>_wall` | `spear/<id>_wall` | missing (would fall back to the plain clip) |
| Hits `hit_head/body/legs` (+ `_2`, `_3`) | family else common | common 9 clips inherited (generic, no shield assumption) |
| Blocks | `block`, `block_head`, `block_legs` | only in `sword_shield`; none for spear (no shield, so blocks are not expected) |
| Deaths | by kind and side (`death_slash_front` etc.) | common, inherited |
| `fall`, `raise`, scenes | common | inherited |
| Weapon model | `art/weapons/<id>.json`, `TroopModels.weapon` | Branch choice: `origin/work/troop-models-house` (not merged here). It holds `art/weapons/coral_trident`, `_t2`, `_t3` (bbmodel, json, png) with game models under `models/troop/weapon/`, and the Tusk and Tideguard stage-1 troop files (`art/troops/wm_tusk.json`, `tc_tideguard.json`). This branch has none of them (`models/troop/index.json` lists archer, gl_marshal, knight, longbow, mage, sentinel). The Admiral has no model anywhere. Whether Tusk uses the Tidecallers `coral_trident` is not stated; ask the art lane. |

Attack rows (fixed): `spear_thrust` body stab impact 7 ticks (0.35 s); `spear_high_thrust` head stab 7 (0.35 s); `spear_low_sweep` legs blunt 8 (0.40 s). Max clip 40 ticks.

## References

See `anim-refs/spear/SOURCES.md` (frames in `anim-refs/spear/frames/<move>/`, 10 fps, 480 px wide, not committed).

| Video | Use |
|---|---|
| Thunder Cloud Studio, "Spear Animation Demonstration", youtu.be/Yk4NFoRhw1g, 1:05 | Main reference: `thrust` 0:10.2-0:11.4, `high_thrust` 0:11.4-0:12.6, `low_sweep` 0:07.3-0:08.5, `idle` 0:00-0:02, `carry` 0:26-0:28 |
| GameDevHero V1 and V2 (Kort's find) | Downloaded and looked at: wide catalogues of tiny labelled figures, no pose readable. Not used. |
| 9CG, "Spear Animation Pack for Unreal Engine", youtu.be/4HCB4j9gJS4 | Overhead catalogue of tiny figures. Not used. |
| Ammar, "Spear animations and combat", youtu.be/zShdlx511mM | Dark Souls-like montage, small and cluttered. Mood only. |

The Souls-like look, from the main video: a deep crouch, hands drawn back along the shaft, a hard lunge that holds the level extension about 0.2 s, then a quick settle.

## Stances

Rig bones keyed: `root` (position, px), `body`, `head`, `right_arm`, `right_forearm`, `right_hand`, `left_arm`, `left_forearm` (rotation, degrees x,y,z). Signs follow `sword_shield_thrust.json`: body x + leans forward; arm x - raises the arm forward; root z - moves forward; body y + turns the right shoulder back (wind-up side); head y counter-turns to keep the eyes on the target. Every number below is a starting value for the builder to tune in `pose_preview.py` until the **tine direction** column holds (it is the check, the angles are not). Stance tables are absolute from rest; the move tables further down are offsets over the stance.

Tine direction is stated as yaw from the unit's forward line (0 = at the target, + = to the right, - = to the left, 180 = straight back) and pitch from level (+ up, - down).

Both hands are on the shaft in every stance except `idle` and `carry`, where only the right hand holds it. Grip is measured along the shaft from the butt (shaft about 2.3 blocks): rear hand = the right hand.

| Stance | Hands | Used when | Pose and tine direction |
|---|---|---|---|
| `idle` | right hand at the balance point (1.0 m from the butt); left hand free, hanging | no enemy, still | relaxed, feet wide. Tines yaw 0, pitch -45 (point down and forward, clear of the ground; check clipping). Video 0:00-0:02 |
| `carry` | right hand 0.8 m from the butt, shaft resting on the right shoulder; left hand free, swings | marching | Tines yaw 0, pitch +60, butt down behind the hip, never across the neighbour. The video's trailed-point run (0:26-0:28) is not copied: it would spear the man behind |
| `guard` | rear (right) hand on the shaft 0.5 m from the butt at hip height; lead (left) hand 0.5 m ahead of it at waist to chest height (8 to 12 px apart) | enemy near, or Run with enemy | low and wide, knees bent, weight forward. Tines yaw 0, pitch 0 to +5, in line with both forearms at waist to chest height. Video recovery 0:12.0-0:12.6 |
| `brace` | rear hand on the shaft 0.4 m from the butt at hip height, butt behind the rear hip; lead hand 0.5 m ahead at waist height | Hold (formed line); no footage | the "hoplite" hold: tight, upright, rear foot back. Tines yaw 0, pitch -5 (0 to -10), at the charging enemy's chest; held steady |

| Stance | Time | root | body | head | R arm | R forearm | R hand | L arm | L forearm |
|---|---|---|---|---|---|---|---|---|---|
| `idle` | 0 / 1.2 / 2.4 | 0,0,0 | 0,0,0 (sway 0.5) | 0,0,0 | -12,0,6 | -10,0,0 | -8,0,0 | 2,0,-4 | 0,0,0 |
| `carry` | 0 / 0.4 / 0.8 | 0,0.4 bob,0 | 2,0,0 | 0,0,0 | -25,-8,12 | -70,0,0 | 0,0,0 | swings +-20,0,-4 | -10,0,0 |
| `guard` | 0 / 1.2 / 2.4 | 0,-2.5,-1 | 8,12,0 (sway 1.5) | -6,-12,0 | -50,-6,10 | -35,0,0 | -10,0,0 | -68,18,-6 | -20,0,0 |
| `brace` | 0 / 1.5 / 3.0 | 0,-0.5,0 | 3,6,0 | -3,-6,0 | -40,-4,6 | -25,0,0 | -6,0,0 | -62,14,-4 | -18,0,0 |

Formation rule (as for the knight): `brace` and the `_wall` strikes stay tight and upright so neighbours on the 0.7-block cell do not overlap. Loose melee (`guard` and the loose strikes) uses the low wide pose. `idle` stays relaxed. Loops close (first key equals last).

## Moves

All are action clips (offset over the stance; first and last keys of every keyed channel are 0; impact marker on the fixed tick). Times in seconds. Step-in is a root offset only; legs unkeyed so foot planting bends the knees. Video times are Thunder Cloud Studio (`thunder-spear.mkv`), looked at at 10 fps. Cells are rotations x,y,z in degrees, root in px; a bare 0 means 0,0,0. The last column is the absolute tine direction (yaw, pitch) the pose must show.

### `spear_thrust` (body, stab, impact 0.35 s, length 1.2 s)

Start and end stance: `guard`. Hands: rear on the shaft at the hip, lead 8 to 12 px ahead; at impact both arms drive straight out and the lead hand slides about 2 px toward the tines. Video: 0:10.2-0:11.4. Checked: 10.3-10.5 crouch with the spear drawn back along its own line, point forward and slightly down; 10.6-10.7 lunge; 10.8-10.9 full extension, shaft level at chest to shoulder height, in line with both arms, held about 0.2 s; 11.0 (frame f_009) is an overhead spin, not a rise, and is not used; the recovery is ours. The video goes from the start of the draw (10.3) to full extension (10.75) in about 0.45 s; scale by 0.78 so the extension lands on 0.35 s. The 0.2 s hold becomes the follow-through; the recovery (0.55 to 1.2 s) is ours: a steady pull back to guard.

| Time | Key | root | body | head | R arm | R forearm | R hand | L arm | L forearm | Tines (yaw, pitch) |
|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | start | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, +2 |
| 0.20 | wind-up: weight back, rear hand to the ribs, shaft slides back along its line | 0,-0.5,1.0 | -2,10,0 | 0,-8,0 | 40,0,6 | 25,0,0 | 0 | 4,0,0 | 0 | 0, -5 (point at the target's chest) |
| 0.30 | lunge | 0,-0.8,-1.5 | 6,-4,0 | 0,4,0 | 12,0,-2 | 8,0,0 | 0 | -6,-4,0 | -2,0,0 | 0, -2 |
| 0.35 | impact: arms drive out, hips square, lean in 6-8 | 0,-0.8,-2.8 | 8,-12,0 | 0,10,0 | -22,0,-8 | -8,0,0 | -6,0,0 | -14,-12,0 | -6,0,0 | 0 (within 2), 0 level, chest height |
| 0.50 | follow-through: held, small settle | 0,-0.8,-2.6 | 7,-10,0 | 0,9,0 | -20,0,-7 | -7,0,0 | -5,0,0 | -12,-10,0 | -5,0,0 | 0, -3 |
| 0.70 | recovery | 0,-0.3,-1.0 | 3,-4,0 | 0,3,0 | -6,0,-2 | -2,0,0 | 0 | -4,-3,0 | 0 | 0, -1 |
| 1.20 | end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, +2 |

### `spear_high_thrust` (head, stab, impact 0.35 s, length 1.2 s)

Start and end: `guard`. Hands: rear hand rises to the cheek then drives out at shoulder height, lead hand at chest then out. Video: 0:11.4-0:12.6. Checked: 11.4-11.5 fast gather; 11.6-11.8 deep lunge, shaft level at shoulder to chest height, point leading, held about 0.3 s (impact about 11.65); 11.9 spear drops; 12.0-12.6 crouched ready pose. The video is at shoulder height; ours aims at head height, so raise both hands 2 to 3 px and lean less deep. The cheek-height wind-up is ours.

| Time | Key | root | body | head | R arm | R forearm | R hand | L arm | L forearm | Tines (yaw, pitch) |
|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | start | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, +2 |
| 0.20 | wind-up: rear hand to shoulder or cheek, torso rocks back 5 | 0,-0.3,0.8 | -5,8,0 | 0,-6,0 | 14,0,4 | -30,0,0 | 0 | -8,0,0 | -6,0,0 | 0, +4 (shaft near head height) |
| 0.30 | rise and drive | 0,-0.4,-1.0 | 2,-4,0 | 0,3,0 | -18,0,-4 | -14,0,0 | 0 | -16,-6,0 | -6,0,0 | 0, +3 |
| 0.35 | impact: arms out at shoulder height | 0,-0.4,-2.0 | 4,-10,0 | 0,8,0 | -34,0,-8 | -10,0,0 | -6,0,0 | -26,-10,0 | -8,0,0 | 0 (within 2), +2, head height (the target's eye line) |
| 0.50 | follow-through: held, shoulders high | 0,-0.4,-1.8 | 4,-8,0 | 0,7,0 | -32,0,-7 | -9,0,0 | -5,0,0 | -24,-8,0 | -7,0,0 | 0, +1 |
| 0.75 | recovery: lower to guard | 0,-0.2,-0.8 | 2,-3,0 | 0,2,0 | -10,0,-2 | -3,0,0 | 0 | -8,-2,0 | -2,0,0 | 0, +1 |
| 1.20 | end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, +2 |

### `spear_low_sweep` (legs, blunt, impact 0.40 s, length 1.3 s)

Start and end: `guard`. Hands: both low at the right hip at the wind-up (rear hand at the hip, lead hand 8 to 12 px ahead, both on the shaft), then the arms sweep across the front. Video: 0:07.5-0:07.7 and 0:08.2-0:08.4 (local frames `frames/low_sweep/f_003` and `f_004`). Checked: f_003 (about 7.5) crouched, hips turned, shaft running from the hand at hip height down and forward to about knee height, point leading; f_004 (about 7.6) deeper crouch, shaft from the hand near the waist falling to the shin and the ground, arc blur around it. `f_010` (about 8.2-8.3) shows the shaft at waist to chest height with the arc over the head; that is higher than a legs-zone blow needs, so it is **not** used for height. Height chosen: shaft from the hands at waist height down to the tine third at knee height at impact (waist to knee), because the zone is `legs` and the f_003 and f_004 frames show the same fall. The video winds up overhead with a jump-spin (7.3-7.4), which we do not copy; the low cock at the right hip is ours. Do not use 6.8-7.0 (a head-height spin). Tines lead the arc (the point is in front of the shaft's travel), as in the video; this settles the old question 4. All keys are on ticks (multiples of 0.05 s). The body turn is paced at 15 degrees per tick or less: 0 to 40 in three ticks (0.15), 40 to -35 in five ticks (0.15 to 0.40, the largest step 15 per tick), so the amplitude is 40 right and 35 left, not the 45 and 40 of the first draft. The shaft, not the tines, makes contact (blunt); the outer third of the shaft crosses the target's legs.

| Time | Key | root | body | head | R arm | R forearm | R hand | L arm | L forearm | Tines (yaw, pitch) |
|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | start | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, +2 |
| 0.15 | wind-up peak: crouch, hips and shoulders turned 40 right, hands low at the right hip | 0,-3.0,0.5 | 14,40,0 | 0,-36,0 | 45,0,8 | 10,0,0 | 0 | 40,-35,0 | 0 | +120 (back-right), -10 |
| 0.25 | hips lead | 0,-3.1,0 | 15,10,0 | 0,-9,0 | 22,0,-2 | 6,0,0 | 0 | 20,-18,0 | 0 | +60, -14 |
| 0.35 | arms catch up | 0,-3.2,-0.6 | 15,-20,0 | 0,18,0 | 6,0,-14 | 0 | 0 | 4,12,0 | 0 | +10, -18 |
| 0.40 | impact: sweep, shaft waist to knee | 0,-3.2,-1.0 | 14,-35,0 | 0,32,0 | -10,0,-25 | -4,0,0 | -4,0,0 | -8,25,0 | -4,0,0 | -15, -20 (tine third at knee height) |
| 0.50 | follow-through | 0,-3.0,-0.8 | 12,-45,0 | 0,40,0 | -8,0,-30 | -4,0,0 | -4,0,0 | -6,30,0 | -4,0,0 | -45, -15 |
| 0.65 | follow-through end | 0,-2.6,-0.4 | 10,-42,0 | 0,38,0 | -6,0,-28 | -3,0,0 | 0 | -4,28,0 | -3,0,0 | -60, -10 (knee to shin) |
| 0.90 | unwind and rise | 0,-1.0,0 | 5,-12,0 | 0,10,0 | -2,0,-8 | 0 | 0 | -1,8,0 | 0 | -20, -3 |
| 1.30 | end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, +2 |

### Braced versions `spear_thrust_wall`, `spear_high_thrust_wall`, `spear_low_sweep_wall`

Same impact ticks, played from `brace` (offsets over the brace table). Hands: rear on the shaft at the hip, lead 0.5 m ahead; they only slide 1 to 2 px along the shaft. Tight and upright: no crouch, hip turn at most 6 degrees, root travel under 1 px, nothing wider than the 0.7-block cell. Length 1.2 s (thrust, high thrust) and 1.3 s (low).

Reconciling arms with the tine pitch: the shaft pivots on the lead hand, so the pitch change from the brace line (-5) is about 1.2 times D, where D = (rear arm x) - (lead arm x) in degrees (the lead arm higher than the rear raises the tines; 10 px grip spacing, 12 px arms). Every row below satisfies this within 1 degree; head counter-turns the body y at the same size; forearm and hand deltas stay at 5 degrees or less. The loose strikes above are tuned in the pose preview instead, because their rear-hand pull is mostly horizontal.

| Clip | Time | Key | root | body | head | R arm | R forearm | R hand | L arm | L forearm | D | Tines (yaw, pitch) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| thrust_wall | 0.20 | pull back along the line | 0,0,0.4 | 0,3,0 | 0,-3,0 | 10,0,3 | 4,0,0 | 0 | 10,0,0 | 2,0,0 | 0 | 0, -5 |
| | 0.35 | impact | 0,-0.2,-0.8 | 3,-3,0 | 0,3,0 | -8,0,-4 | -3,0,0 | -3,0,0 | -12,-4,0 | -4,0,0 | +4 | 0, 0 (chest height) |
| | 0.50 | hold | 0,-0.2,-0.7 | 3,-3,0 | 0,3,0 | -7,0,-4 | -3,0,0 | -3,0,0 | -9,-4,0 | -3,0,0 | +2 | 0, -2 |
| | 1.20 | end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, -5 (brace line) |
| high_thrust_wall | 0.20 | pull back, hands lift 1 px | 0,0,0.3 | -1,3,0 | 0,-3,0 | 6,0,2 | 3,0,0 | 0 | 2,0,0 | 1,0,0 | +4 | 0, 0 |
| | 0.35 | impact | 0,-0.1,-0.6 | 2,-3,0 | 0,3,0 | -20,0,-6 | -6,0,0 | -4,0,0 | -26,-6,0 | -8,0,0 | +6 | 0, +2 (head height) |
| | 0.50 | hold | 0,-0.1,-0.5 | 2,-3,0 | 0,3,0 | -18,0,-5 | -5,0,0 | -3,0,0 | -23,-5,0 | -7,0,0 | +5 | 0, +1 |
| | 1.20 | end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, -5 |
| low_sweep_wall | 0.25 | draw back along the line | 0,-0.3,0.4 | 2,4,0 | 0,-4,0 | 22,0,2 | 5,0,0 | 0 | 18,0,0 | 3,0,0 | +4 | 0, 0 |
| | 0.40 | impact: short chop forward and down, no sideways sweep | 0,-0.6,-0.9 | 6,-4,0 | 0,4,0 | -14,0,-3 | -4,0,0 | -3,0,0 | 7,-4,0 | 2,0,0 | -21 | 0, -30 (shin height) |
| | 0.65 | follow-through | 0,-0.5,-0.6 | 5,-3,0 | 0,3,0 | -12,0,-2 | -3,0,0 | -2,0,0 | 4,-3,0 | 2,0,0 | -16 | 0, -25 |
| | 1.30 | end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0, -5 |

### Other clips to author

| Clip | Purpose | Weapon direction |
|---|---|---|
| `idle`, `carry` | see Stances | see Stances |
| hits | optional spear family `hit_*`: recoil keeping the spear levelled (the inherited common hits are shield-agnostic and may drag the shaft through the body) | tines stay forward; the arms move less than the torso |
| Guarded advance (walk with an enemy near) | optional, same as the knight's | tines forward, shaft level |

Moves the video lacks: the braced hold and `_wall` strikes (no line is shown), the low sweep's low wind-up, and a column-safe carry. Source for those: the polearm "hoplite" references in the 2026-10-29 spec (Kort's fourth reference) and the weapon-direction rule.

## Application to units

- **Proportions.** The three share the biped rig (arms 12 px, hand to shoulder). Shaft is about 2.2 to 2.5 blocks of trident model; the left arm and forearm are keyed per pose onto the shaft line (hands 8 to 12 px apart along the shaft).
- **rootScale** (root offsets are multiplied by it; `TroopModel.java` lines 312-318: `(24 - troop root pivotY) / (24 - biped root pivotY)`). Read from the files on `origin/work/troop-models-house`: `art/troops/wm_tusk.json` (rig `biped`, size 128x64, `hunch` 20, no `bones` block) and `tc_tideguard.json` (rig `biped`, size 128x64, `bones` only scales both legs by 1.05, no root entry). Neither moves the root, so both pivots equal the biped's and the formula gives (24 - p) / (24 - p) = 1.0 for both. Caveat: the loader may derive the root pivot from the 1.05 leg scale (it would then be about 1.05), so Task 0 prints `rootScale()` at load and records it before any root number is trusted. Admiral: no model, no value; assumption 1.25 (`large`), replaced by the model's number when it lands. Clips are authored at 1.0; the engine scales root offsets (thrust travel 2.8 px becomes about 3.5 px at 1.25).
- **Admiral clearance** (all assumptions, no model exists): scale 1.25, torso about 10 px wide (8 px x 1.25), shoulder span about 20 px (16 x 1.25), grip spacing 10 to 15 px (8 to 12 x 1.25). Measure first: when his model exists, read his torso and arm widths and his root pivot from the model and redo these numbers before authoring against them; until then treat everything here as unchecked. Targets once measured: shaft at least 2 px clear of the chest front plane at every wind-up; lead hand at least 3 px in front of the chest; at the high-thrust wind-up the shaft at least 2 px from the cheek and the rear forearm clear of the helm brim. He has `slotWeight 4`, so he shares no 0.7-block cell; the cell rule applies to Tusk and Tideguard only.
- **Held item.** Right hand. The weapon model's grip (art lane) must put the tines along the hand's forward axis, so a forearm pointing at the target means tines at the target. This is an assumption to confirm when the trident model exists.
- **Clipping risks.** Shaft through the head and the left forearm at the high-thrust wind-up; shaft through the rear leg at the sweep wind-up; shaft through the neighbour in a 0.7-block brace line (tines extend about 1.2 blocks ahead, but the shaft's rear passes the neighbour); the Drowned and Piglin skins are bulkier at the arms than the Husk.
- **Ability visuals.** Brace has none beyond the stance (x1.3 on charging enemies). Undertow (Admiral, slam) has no clip row; if a cast pose is wanted it needs a new attack or scene clip and is a gameplay-adjacent addition: needs Kort.
- **Prerequisite.** Clips only drive troops with a `TroopModel`. Models come from `origin/work/troop-models-house`: Tusk and Tideguard stage-1 troops and the `coral_trident` weapons are there, none of it is merged into this branch, and the Admiral is not started. Until the branch is merged, clips can be authored and verified in Blockbench but not rendered in game.

## Fixed limits

- Impact ticks fixed: thrust 7, high thrust 7, low sweep 8; clip max 40 ticks. Wall variants keep the same ticks.
- `tools/clips.py --verify` must pass; `Spec.java` only through `tools/preflight.py`; restore `sheets/preflight.json` and `Spec.java` after verify.
- `MotionEngineCheck` hit checks all run the real `sword_shield` family: `hitOverAttack` (line about 1093, built on `hitWhileAttacking` at 1057), `hitOverWindow` (line 1111, held: the steps in the window rise to one peak and fall) and `wallStrikes` (line 519, which hard-codes impacts 7, 10, 8 for the sword-and-shield strikes; the spear impacts are 7, 7, 8). `hitWhileAttacking` starts the attack at tick 20 and lands the hit at tick 25, which is attack tick 5; its window, ticks 26 to 35, is therefore attack ticks 6 to 15, inside every spear clip (24 and 26 ticks). So the window is valid for the spear but the fixture never plays a spear clip. Spear-specific check (plan Task 7): a copy that builds the family from `common` plus `spear` and plays each spear attack with a common hit at attack tick 5 (as the original), 10 and 14, requiring each step at most 15 degrees per tick from the hit to the clip's last tick; plus copies of `hitOverWindow` and `wallStrikes` with the spear impacts (7, 7, 8). Spear troops carry less cloth than the knight; the drowned and piglin skins have none.
- Max 2 Minecraft instances; no computer-use on Minecraft; never touch PIDs 29360, 4580, 6048 or C:/GGMatch; no eula.txt; no push or merge.
- No gameplay, save or network change.

## Questions for Kort

1. Models: Tusk and Tideguard troop models and the `coral_trident` weapons are on `origin/work/troop-models-house`, not on this branch; the Admiral has none. Should the animation work wait for that branch to merge, or author clips blind on the biped rig and render later?
2. The three troops share one set of clips. Do you want the Admiral to get his own heavier variants later (slower, wider)? Default: shared.
3. Undertow has no cast pose (a slam from a spear). Leave as the plain thrust for now? Default: leave.

## Review

v1 6.5 (8/7/7/5/7/5)

Fix round 1: key tables (time, bone x,y,z, tine yaw and pitch) for every clip; hit-over-attack scope and a spear-specific check; both hand positions named on the shaft in every stance; low sweep tine yaw per key, waist-to-knee height cited to frames f_003 and f_004, question 4 settled; rootScale source and Admiral width and clearance numbers; plan tasks 2, 4, 5, 6 given their own Expected lines and checklists.

v2 6.2 (7/7/7/5/6/5)

Fix round 2: hit-check scope corrected (hit lands at attack tick 5, window is inside the clips; hitOverWindow and wallStrikes named; spear copies added); rootScale read from the troop files; braced tables completed and reconciled with tine pitch; Admiral numbers marked as assumptions with a measure-first step; low sweep keys snapped to ticks with the body step at 15 degrees per tick or less; trident branch and model status stated; thrust frame f_009 corrected (overhead spin).

Final (after fix round 2): 6.5 (8/7/7/5/7/5). Known gaps for Kort:
- Stub clips are missing. `tools/clips.py:396` fails until every `spear_*` id has a clip, so Task 0 must add zero-motion stubs with their impact markers.
- The brace and guard arm numbers contradict the stated tine pitch (D rule gives about +26° against the stated -5°), so they must be re-derived.
- The spear hit-over-attack check has no file, command or exit check, and `mec.sh` does not cover spear.
- The pair brace render has no `run.sh` case.
- Per-unit numbers wait for the `troop-models-house` merge. On that branch the Tideguard wears a finned cap and bronze cuirass, not an iron helm.
- The low sweep wind-up and all `_wall` keys are our own, with no footage behind them.
