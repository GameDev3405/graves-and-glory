# Thrower troop (Harpooner): animation spec

Kort, 2026-11-03 request: find good game-animation videos for each weapon, break the moves down, and apply them to the units properly. Branch `work/anim-rework`. Docs only; no clip, Java or sheet was touched. Reference folder `C:/Users/kortt/Melty/anim-refs/thrower/` (not committed; `SOURCES.md` there).

**Reference status: one usable video, low detail.** The only throw I found is Awakening Animations' "Blender character spear throw FINAL" (youtu.be/5VTQCulDktA, local `awakening-spear-throw.mkv`): a small grey figure, camera partly orbiting. Frames at 10 fps are in `frames/throw/` and looked at. No polished game-pack javelin or harpoon throw turned up (nikoff and Kevin Iglesias were downloaded; tiny figures, no throw I could identify). Stances reuse the spear spec's checked Thunder Cloud frames. Numbers below are starting values; the harpoon direction column (computed from the rig chain, round 1) is the check and the angles are tuned in the pose preview until it holds.

## Why

The family `thrower` has one troop, `tc_harpooner`. `clips/biped/thrower/` does not exist and `common/` has no stances or attacks, so nothing plays for it. Beyond missing clips, four engine facts make a straight clip drop-in wrong (see Inventory today): no model on this branch, the bow carry applies to it (a **precondition for any render: needs Kort or the coordinator**), the shot leaves on the clip's impact tick so the wind-up would be skipped, and the harpoon item stays in the hand after the throw. Kort wants the Souls-like feel: a loaded wind-up, a hard hip-driven release, the harpoon point leading.

## Goals

- `idle`, `carry`, `aim` stances and the `thrower_throw` attack, with the harpoon always pointing where the move needs.
- Throw impact (release) on tick 8 (0.40 s); clip at most 40 ticks. Fixed row.
- No clipping of harpoon, arm or rope coil through the sou'wester brim, quiver or head.
- Approved by Kort in game, one clip at a time.

## Units

| Troop | Entity / skin | Loadout (Spec.TROOPS) | Stats | Ability |
|---|---|---|---|---|
| `tc_harpooner` (Harpooner, Tidecallers, common), the only `thrower` | drowned, `drowned.png` | weapon `minecraft:trident`, helmet `leather_helmet`, chest `air`, shield `none`, tags none | hp 20, damage 5, range 16, speed 0.29, cooldown 40 ticks (2.0 s), slotWeight 1, ranged, not magic | `harpoon` (design.json: on hit pulls the target 1 block closer and applies Slowness I for 1.5 s, every 3 s) |

Troop model (on branch `art/tidecallers-t1`, NOT on this branch): `art/troops/tc_harpooner.json`, rig `biped`, `idleArms: hang`, weapon `harpoon` in the right hand, no bone scale overrides. Look: sou'wester with a long brim tail down the back, navy oilskin coat, rope coil over the left shoulder, a quiver of spare heads on a baldric on the back, wrist wraps. Weapon `art/weapons/harpoon.json`: grip offset 0, rotation 0, "held upright like the sun staff"; so the shaft stands vertical in the hand and every pose has to turn the right hand to lay it level.

## Inventory today

| Engine slot | Looks for | Today |
|---|---|---|
| Model | `TroopMotion.row(m)` needs `m.troop`; `models/troop/index.json` on this branch lists archer, gl_marshal, knight, longbow, mage, sentinel | **missing here**: `tc_harpooner` plays no clips at all (the vanilla drowned renderer is used) |
| Stances | `thrower/<name>` else `common/<name>` | none in either; procedural. A ranged troop firing is in the `aim` stance (the bow family has `aim.json`) |
| Attack `thrower_throw` (body, arrow, impact 8) | `thrower/thrower_throw` | missing; no attack plays |
| `_wall` | n/a | none; a ranged troop does not take wall or brace |
| Hits `hit_head/body/legs` (+`_2`,`_3`) | family else common | 9 common clips inherited. They move the arms, so a hit while aiming can drag the harpoon off line |
| Blocks | none | none expected |
| Deaths, fall, raise | common | inherited |
| Bow carry (**defect**) | `TroopMotion.bow(row)` = `ranged && !magic` (line 146) | true for the thrower, so `carries()` calls `twist()`: the weapon hand is turned 45 degrees yaw at rest and 90 pitch plus 90 roll while an attack plays (`draw()` counts any attack track). That is a bow's grip, wrong for a harpoon. Needs a Java change (restrict to family `bow`): **needs Kort / coordinator before Plan Task 1 renders anything**, because every render of `aim` or `thrower_throw` shows the twisted hand until it is fixed. Lines: `TroopMotion.java:146` (`bow(row)`), its callers in `pose()` (`carries(m,bow(row),...)`, `draw(...,bow(row),...)`) |
| Release timing (**defect**) | `Tactics.drawsAhead` is true for family `bow` only (its comment: "the crossbow and thrower families have no clips yet"); `Projectiles.loose` starts the clip `impactTicks` before now | the throw clip would start 8 ticks in the past when the harpoon leaves, so the client skips the wind-up and pops to the release pose. The fix is one condition in `Tactics.drawsAhead` (thrower too) plus its seed loose/hold: a timing change, **needs Kort** |
| Thrown harpoon visual | `Projectiles.fire` kind `trident` spawns a flying TridentEntity | the item stays in the hand after release, so two harpoons show. Needs a hide-weapon marker or held-item swap: **needs Kort / coordinator** |

Cooldown 40 ticks and clip length 22 ticks fit; the clip ends well before the next throw.

## References

| Source | Use |
|---|---|
| Awakening Animations, "Blender character spear throw FINAL", youtu.be/5VTQCulDktA | the throw: 1.9-2.3 s load and plant (f_009-f_013), 2.4-2.6 s release (f_014-f_016), 2.8-3.1 s follow-through and stumble (f_018-f_021; f_n = 1.1 + 0.1 (n-1) s, so f_021 = 3.1 s) |
| Thunder Cloud Studio, "Spear Animation Demonstration", youtu.be/Yk4NFoRhw1g | idle 0:00-0:02 (`frames/idle`), used for the idle pose; **carry (0:26-0:28) and guard (0:12.0-0:12.6) are rule only**: the frames are on disk but neither is copied (the carry frame is a run with the point forward and low; the thrower has no guard) |
| nikoff pack, Kevin Iglesias throwing | downloaded, not usable (tiny, no readable throw) |

The camera in the Awakening video orbits, so the screen direction of the spear flips between f_014 and f_015. The harpoon directions below come from the frames where it is level and the point leads (f_009-f_013) and from the weapon-direction rule, not from screen left or right.

## Stances

**How the harpoon direction is derived (round 1 fix).** Signs, checked against shipped clips: body x + leans forward; arm x - raises the arm forward (arm x -90 = level forward, -180 = straight up); body y + turns the chest to the right (right shoulder back); root y + is DOWN (shipped `guard.json` crouches with root y +1.2); root z - is forward (shipped `sword_shield_overhead` and `low_cut` lunge to -1.8 and -1.4 at impact). The harpoon's authored axis is the weapon json grip at rotation 0: the tip points out of the fist along hand-local -y, so with every bone at 0 the tip points UP the hanging arm. It points forward only after the hand is flipped about 180 (as the shipped `sword_shield/guard.json` hand x 180 does for the sword). So the tip pitch cannot be read from the sum of body, arm and forearm x alone: the hand x is what turns it. I computed the tip with the rig's own chain (`tools/bbmodel.py rotation()`, ZYX order, pivots from `art/rigs/biped.json`) in `C:/Users/kortt/Melty/anim-refs/thrower/chain.py`; `final_keys.py` there reprints every row below (run both from that folder). The yaw is measured from the unit's forward line (0 = at the target, the entity body already faces the target; + = to the unit's right), pitch + up. The x-only estimate is: tip pitch = -(body x + arm x + forearm x + hand x - 180 + 90), angles mod 360, good to a few degrees only while arm y and z are small; the script is the reference. The first draft's hand -80 gave tip pitch -18 and yaw +38 (computed): not level, not at the target. The numbers below replace it. Round 2 re-solved the hand for each key after moving arm y out (the hand sits 5.2 to 6.1 px outside the head centre at every key) and after the engine's aimPitch (about -31 on both arms, see below) was taken into account. This is a model computation, not yet a preview or in-game check.

Right-arm and hand values are absolute (stance, from rest). Left arm and head values are starting values to confirm in the preview. Harpoon columns are computed (right hand only); hand position is the hand pivot in rig px (x - is the unit's right, y - is up, z - is forward; the head spans x -4 to 4, y -8 to 0 at rest).

| Stance | Time | root | body | head | R arm | R forearm | R hand | L arm | L forearm | Harpoon tip (yaw, pitch) and hand pos |
|---|---|---|---|---|---|---|---|---|---|---|
| `idle` | 0 / 1.2 / 2.4 | 0 | 0 (sway 0.5) | 0 | -12,0,6 | -10,0,0 | 150,-10,0 | 2,0,-4 | 0 | -2, -39; hand -6.8, 9.5, -2.3 (hip height, beside the thigh) |
| `carry` | 0 / 0.4 / 0.8 | 0,0.4 bob,0 | 2,0,0 | 0 | -25,-8,12 | -70,0,0 | 120,15,0 | swings +-20,0,-4 | -10,0,0 | -3, +61; hand -5.9, 5.4, -6.0 (chest, in front) |
| `aim` | 0 / 1.5 / 3.0 | 0,+1.2,0 | 3,22,0 (sway 1) | -3,-22,0 | -100,-20,0 | -70,0,0 | -103,2,0 | -75,-10,-10 | -10,0,0 | 0, 0 (authored; the engine adds the arc, see aimPitch below); hand -6.1, -2.4, -3.4 (2.1 px outside the head edge) |

- `idle`: right hand at the balance point, left hand hanging; relaxed, feet wide; point forward and down about -40 so it clears the ground; brim tail clear of the shaft. Video frame `frames/idle/f_005` (0:00-0:02, Thunder Cloud): the figure faces the camera, so the spear hand on screen-left is the figure's RIGHT hand; the point is low to screen-left, the butt up behind the shoulder. Matches the right-hand harpoon (no mirroring needed). The video's butt-up-behind-the-shoulder is not copied: here the butt stays behind the hip so the shaft does not touch the quiver (check in the preview).
- `carry`: **authored from the rule, not from the video.** The video's carry frame `frames/carry/f_010` (0:26-0:28) is a run with the spear point held forward and low in the front hand and the butt over the shoulder; that would spear the man in front, so it is not copied. Ours: harpoon resting on the right shoulder, hand chest-high, left hand free and swinging; tip yaw 0, pitch about +60, butt down behind the hip; quiver and rope coil stay clear of the arm.
- `aim` (video 1.9-2.3 s, `frames/throw/f_009-f_013`): the spear held level over the shoulder, left arm out as a pointer, side-on stance. Tip level at the target (authored pitch 0: the engine adds the shot's arc, see aimPitch below), at chest-to-head height, 2 px outside the head edge and below the brim.
- **Why body y +22, not the bow's 90.** The shipped `bow/aim.json` turns the body 90 (and the head -90) because a bow is held side-on with both arms across it. The harpoon is thrown one-handed from the shoulder, so a 22 degree open stance is enough to hold the right shoulder back for the draw (wind-up opens it to 40) while the chest stays nearly toward the target, which also keeps the quiver on the back out of the throwing arm's way. It is a chosen value, not copied from the bow or the video (the video's hips are open further at 2.2 s); raise it if the preview looks stiff. Yaw check: body +22, arm y -20 and hand y +2 combine through the chain to a shaft yaw of +0.2 (script), so the shaft lies on the target line; the head -22 counter-turns to look at the target.
- **Right hand sign.** Hand x -103 (= +257) is derived, not guessed: it is the rotation that, with body 3, arm -100 and forearm -70, lays the shaft at yaw 0.1 and pitch 0.2 (script, round 2 re-solve for the aimPitch baseline). Numeric acceptance in the plan: the shaft within 6 degrees of level (pitch -6 to +6) and 3 degrees of yaw 0.
- **aimPitch (round 2).** MotionEngine does add it for every family: `MotionEngine.java:376` reads `in.aimPitch()` clamped to +-60 and `MotionStack.java:64` sets the aim share to 1 for any stance named `aim` (not just the bow), then `MotionStack.java:110-111` adds `aimPitch * aimShare` to the x rotation of BOTH `right_arm` and `left_arm` (positive = down, so a negative value raises the arms and the harpoon). The value is `Projectiles.aimPitch` (line 114), `BattleMath.aimPitch` = minus the launch pitch of the shot's arc. For a thrown harpoon: `volleyPeakPerBlock` 0.25 and `thrownArcScale` 0.6 (sheets/design.json lines 2427 and 2430) give peak = 0.15 x range and launch pitch atan(4 x peak / range) = atan(0.6) = **31 degrees up at every range up to the 22 block cap, so aimPitch is about -31 for a level target (a higher or lower target shifts it)**. So the real harpoon line in game is the authored pitch plus about 31: the shaft rises 31 degrees over the authored pose (arms only, body and head are not raised), matching the lofted flight of the thrown harpoon. That is why the authored aim and release pitches in this spec are 0 and the wind-up +6 (a small draw-back lift), not the +6/+8/+12 of the first draft that would have stacked on the arc to 37 to 43 degrees. The 31 degrees are added to the arm channels for the whole `aim` stance, including during the throw (the attack layer adds on top). Not measured in game: the first render must confirm it (needs the model merge). The left arm also rises 31, so its pointing is also relative to the arc.
- **Root.** Aim crouches slightly: root y +1.2 (y is down; the first draft's -1.5 would have stood the unit up).
- `rest`: **no thrower clip planned**; the engine falls back `rest -> carry -> idle` (`MotionEngine.REST`), so a resting harpooner shows the `carry` pose. No `guard`, `brace`, `wall` or `shield` either (a ranged troop with no shield; if `aim` were missing the chain `aim -> guard -> carry -> idle` falls to `carry`).
- **No `bow_draw` equivalent.** `bow_draw` is the bow family's forced/debug clip; for any other family the name resolves to the first attack (`TroopMotion` line 321), so the thrower plays `thrower_throw` there. Not needed.
- Loops close (first key equals last).

## Moves

One attack clip, `thrower_throw` (body, arrow, impact 0.40 s = tick 8, length 1.1 s = 22 ticks), start and end stance `aim`. Authored pitches exclude the engine's aimPitch (about -31 on both arms in game). The table gives every value ABSOLUTE (stance + delta) with the delta, the number written into the clip (an action clip is an offset over the stance), in brackets beside it. First and last keys of every keyed channel are 0 (the delta rows at 0.00 and 1.10 are all 0). Video: 1.9-3.1 s of the Awakening video (f_021 is 3.1 s). Checked: 1.9-2.3 s (f_009-f_013) shaft level over the shoulder and a long plant stride; 2.4 s (f_014) last frame with the harpoon in the hand; 2.5 s (f_015) the harpoon airborne, point leading. Retime: the video goes from the plant (2.2) to release (2.5) in 0.3 s; ours goes from the stride (0.25) to release (0.40) in 0.15 s, a hard throw, with a short draw-back (0.15 s) that is ours. The 0.40 s release is the fixed row. Step-in by root only (z - forward, y + down); legs left to foot planting.

Round 1 fix: the old table's right arm went from -100 to -135 with the forearm bent to -100 at the "extended" release, and the follow-through arm went UP; both were wrong and are replaced. Harpoon tip and hand position come from `final_keys.py` (right hand only).

| Time | Key | root abs [delta] | body abs [delta] | head abs [delta] | R arm abs [delta] | R forearm abs [delta] | R hand abs [delta] |
|---|---|---|---|---|---|---|---|
| 0.00 | start (`aim`) | 0,1.2,0 [0] | 3,22,0 [0] | -3,-22,0 [0] | -100,-20,0 [0] | -70,0,0 [0] | -103,2,0 [0] |
| 0.15 | wind-up | 0,1.2,0.8 [0,0,0.8] | -1,40,0 [-4,18,0] | -3,-36,0 [0,-14,0] | -70,-30,0 [30,-10,0] | -60,0,0 [10,0,0] | -144,17,0 [-41,15,0] |
| 0.25 | stride | 0,2.4,-1.0 [0,1.2,-1.0] | 9,16,0 [6,-6,0] | -3,-17,0 [0,5,0] | -85,-10,20 [15,10,20] | -55,0,0 [15,0,0] | -146,2,0 [-43,0,0] |
| 0.40 | **release (impact)** | 0,2.7,-2.2 [0,1.5,-2.2] | 15,-16,0 [12,-38,0] | -3,10,0 [0,32,0] | -125,35,10 [-25,55,10] | -15,0,0 [55,0,0] | -139,25,0 [-36,23,0] |
| 0.55 | follow-through | 0,2.7,-2.0 [0,1.5,-2.0] | 19,-30,0 [16,-52,0] | -3,18,0 [0,40,0] | -60,40,30 [40,60,30] | -30,0,0 [40,0,0] | -157,41,0 [-54,39,0] |
| 0.80 | recovery | 0,1.8,-0.8 [0,0.6,-0.8] | 9,4,0 [6,-18,0] | -3,-7,0 [0,15,0] | -100,10,30 [0,30,30] | -40,0,0 [30,0,0] | -134,16,0 [-31,14,0] |
| 1.10 | end (`aim`) | 0,1.2,0 [0] | 3,22,0 [0] | -3,-22,0 [0] | -100,-20,0 [0] | -70,0,0 [0] | -103,2,0 [0] |

| Time | L arm abs [delta] | L forearm abs [delta] | Harpoon tip (yaw, pitch); hand pos | Weapon direction and body mechanics |
|---|---|---|---|---|
| 0.00 | -75,-10,-10 [0] | -10,0,0 [0] | 0, 0; -6.1,-2.4,-3.4 | level, point at the target, over the shoulder (about +31 more in game from the engine's aimPitch) |
| 0.15 | -65,-16,-10 [10,-6,0] | -10 [0] | 0, +6; -5.9,0.7,-3.2 | shaft stays on the target line, point 6 above it; torso opens to 40 (the shoulders do the draw: the hand stays at the ear, arm yaw -30 cancels the body turn); weight back (root z +0.8); hand 1.9 px outside the head edge (round 2) |
| 0.25 | -70,-10,-10 [5,0,0] | -10 [0] | 0, +3; -6.1,0.7,-7.1 | the point drops to +3 as the hand comes forward with the hips; left arm points at the target (eased from -90, round 2); plant stride (root z -1.0, down 1.2); the stride is on tick 5, so release is 3 ticks later |
| 0.40 | -15,10,-10 [60,20,0] | -60,0,0 [-50,0,0] | 0, 0; -5.6,-2.4,-10.2 | **point leading at the target along the almost straight arm** (forearm -15, arm 125 up and forward), release at head height, arm yaw out 35 to hold the hand 1.6 px outside the head edge (round 2); left arm pulled to the ribs; hips square then through to -16; root z -2.2 |
| 0.55 | -10,10,-5 [65,20,5] | -50,0,0 [-40,0,0] | 0, -20 (only if the harpoon is still in the hand, Question 3; with the engine's +31 it would point 11 up); -5.2,3.2,-12.7 | the arm comes DOWN (arm x -125 to -60, +65 vs the release; hand drops from head to chest height, y -2.4 to +3.2) and across the front (arm yaw 40, hand yaw 41 to keep it out of the body); **the x-only estimate fails on this row** (it gives -42 against the chain's -19.7 because arm y 40 and z 30 matter), so `chain_check.py` is the authority; hand open; the thrown harpoon flies where it was aimed |
| 0.80 | -45,-5,-10 [30,5,0] | -30,0,0 [-20,0,0] | 0, -5 (if held); -5.5,-1.2,-8.0 | returning to `aim`; hidden or back in hand (Question 3) |
| 1.10 | -75,-10,-10 [0] | -10 [0] | 0, +6 | end in `aim` |

Per-tick step (round 2): the stride moved from 0.28 (between ticks) to 0.25 (tick 5), so the stride-to-release span is 3 ticks, not 2.4. Sampled every tick with linear interpolation (`step_check.py 0.25`), the largest single-channel step is 21.7 degrees per tick (right arm x, 0.40 to 0.55, the follow-through drop); body y peaks at 12 per tick (wind-up to stride); the left arm x stride-to-release step was 31 per tick at 0.28 and is now 18.3 (left arm stride eased from -90 to -70). Gate in Plan Task 3: no channel above 25 degrees per tick. A 0.30 stride (the coordinator's example) leaves only 2 ticks to release and gives 37.5 degrees per tick on the left arm, so 0.25 is used instead.

Marker: `impact` at 0.40. Hips and shoulders open 22 degrees right at the aim, wind back to 40, then drive through to -16; knees bend through the planted legs (not keyed). The left arm and head columns are tuned in the pose preview; only the right chain is derived by script. Pose check (round 2): the hand x is -5.2 to -6.1 at every key and -5.1 or less at every tick between them (`step_check.py`), i.e. at least 1.1 px outside the head edge (4), up from -4.2 in the first draft; the follow-through (-5.2) and the tick-0.50 sample (-5.1) are the tightest. Brim geometry (from the tc_harpooner build script): the crown is 9.2 wide (x +-4.6), the brim ring spans x +-5.6 and z -6.2 to +5 at y -6.2 to -4 (head-pivot space), the tail hangs down the back. The aim hand sits at y -2.4 (1.6 px below the brim underside at y -4) and x -6.1 (0.5 px outside the brim side edge at 5.6); the wind-up hand at y +0.7 is 4.7 px below the brim; the release hand (y -2.4, x -5.6) is 1.6 px below it. So the shaft passes under the brim ring with 1.6 px of margin at aim and release. This is geometry from the build script, not an in-game measurement: needs the model merge to confirm, and view cheek and brim at 8 angles.

Moves the video lacks: the draw-back and the recovery (authored from the rule); hits, blocks, deaths (inherited from `common/`).

## Application to units

- **Proportions and scale (round 2).** `rootScale()` is set in `TroopModel.rig` (lines 311-315) as (24 - the troop root's pivot y) / (24 - the rig root's pivot y). The `tc_harpooner` build script (`art/hand/tc_harpooner/build_tc_harpooner.py` on `art/tidecallers-t1`) declares `Bone("root", "")` with no pivot, and `art/rigs/biped.json` has the root pivot at 0,0,0, so the formula gives (24-0)/(24-0) = **1.0**. This is read from the files, not measured in game, and the troop json has no bone scale (`bones: []`). The model is not on this branch, so the rendered value **needs the model merge**. Biped: upper arm 6 px, forearm 6, about 12 px shoulder to hand.
- **Helmet (round 2).** The row lists `leather_helmet` (vanilla equipment id), but the troop model draws its own hat: the build script has three bones (`hat`, `brim`, `brim_tail`) tagged `helm`, and `TroopModel.prepare` (line 434) hides `helm` bones only when `helmOff` is set, which `TroopMotion.helmOff` (line 477) takes from the scene marker `helm_off`. So in normal play **the sou'wester renders**, not the leather helmet; I did not find a vanilla armour layer for a custom troop model (not verified in game, needs the model merge). `TroopModelLoader` has no helmet code of its own. The brim clearance is in the Moves section (1.6 px below the brim ring at aim and release; wind-up 4.7 px).
- **Held item.** Harpoon in the right hand, authored upright. The right hand is rotated to lay it level; check the grip stays in the fist at 8 angles. The left hand is free in `idle` and `carry` and points in `aim` and the throw.
- **Clipping risks.** The shaft at head height against the sou'wester brim tail and the quiver baldric when the arm draws back; the left arm against the rope coil on the left shoulder; the quiver against the throwing arm on the follow-through (the arm sweeps across and down the front, not the back); the shaft in `carry` against the hat. Verify at front, 3/4 and side.
- **Wall and formation.** None; the stride root travel (about 2.2 px) stays inside the cell.
- **Scale.** 1.0 by the formula above (needs the model merge to confirm); no root correction.
- **Ability.** `harpoon` pulls the target on hit, resolved server side; no clip drives it. A visible rope or line would be a gameplay-visual item: needs Kort.
- **Needs Kort:** the release timing (`Tactics.drawsAhead`), hiding the harpoon after release, the model merge. The bow-carry fix (Java) is a coordinator call.

## Fixed limits

- `thrower_throw` body, arrow, impact tick 8, weight medium. Clip at most 40 ticks. Any change needs Kort.
- First and last keys of every keyed channel are 0. `python tools/clips.py` then `--verify`; `Spec.java` only through `tools/preflight.py`.
- `MotionEngineCheck.hitOverAttack` (line 1093) loads the `sword_shield` fixture only (`realSwordShield`); it never plays a thrower clip, so it proves nothing here. Its 26-35 tick window (6 to 15 ticks after an attack that starts at tick 20) does fall inside a 22-tick throw clip, so a thrower-specific copy (Plan Task 5) is meaningful and uses the same window. The MEC gate hit lands at attack tick 5 (tick 25); other hit ticks are extras, not the gate.
- Max 2 Minecraft instances; no computer-use on Minecraft; no push, publish or merge.

## Questions for Kort

1. Merge the Tidecallers models (branch `art/tidecallers-t1`) so `tc_harpooner` has a model? Without it nothing here can be seen in game.
2. Start the throw clip a beat early (extend `Tactics.drawsAhead` to the thrower), as the bow does, so the wind-up shows and the harpoon leaves on the release? Without it the throw pops to the release pose.
3. Hide the harpoon in the hand from the release (0.40 s) to about 0.9 s (or show a spare one drawn from the quiver)? Today two harpoons would show.
4. Stop the bow hand-twist for the thrower (restrict `TroopMotion.bow` to the bow family, `TroopMotion.java:146`)? Java; blocks every render, so answer it first.
5. A thrower-owned `hit_*` set so a hit during `aim` does not drag the harpoon off line (as asked for the bow)?
6. Is a level over-the-shoulder javelin throw the right look, or a heavier two-hand overhead hurl?

## Review

v1 6.5 (7/6/5/8/7/6). v2 7.3 (8/6/8/6/8/8). Fix round 1: right-arm signs and harpoon pitch recomputed from the rig chain (absolute and delta values, aim hand x derived, follow-through arm now comes down, root y signs fixed); carry marked as authored from the rule, idle hand and mirroring checked on frame f_005; body y 22 versus the bow's 90 justified with a yaw check; bow twist made a precondition for rendering; rest and bow_draw decisions added; plan tasks 0 and 5 given exact commands, fixtures and acceptance tests.

Fix round 2 (v2 7.3, 8/6/8/6/8/8): (1) arm y moved out and the hand re-solved so the hand is at x -5.2 or less at every key (was -3.0 to -4.7) and -5.1 or less at every tick; brim geometry and clearance stated; (2) rootScale 1.0 derived from the files, sou'wester (not leather_helmet) renders, both marked needs the model merge for the in-game check; (3) the stride moved from 0.28 to tick 5 (0.25) and the left arm eased, largest step 21.7 degrees per tick, with a 25 degrees per tick gate in the plan; (4) the engine does add aimPitch for the thrower (MotionStack.java:110-111, about -31 degrees on both arms), so the authored aim and release pitches are now 0, not +6 and +8, and the hand angles re-solved; (5) references corrected: f_021 = 3.1 s, carry and guard marked rule only; (8) the follow-through row notes the x-only estimate fails there (-42 against -19.7) so chain_check is the authority. Items 6 and 7 are in the plan: real clips.py count 62 today and the first-run outputs of clips.py and anim_edit.py.

Final (after fix round 2): 7.5 (8/6/8/7/8/8). Open minor items (not fixed, two-round limit): plan Task 6 Step 1 needs the command `GRAVES_GRADE_CLIPS=thrower_throw,aim,idle,carry bash test-tools/work/t16r12/live/run.sh all1 solo_harpooner`; step_check.py run bare defaults to 0.3 and prints 27.5, so always pass 0.25; add a brim-clearance line for the 0.55 follow-through (hand x -5.2 is inside the 5.6 brim edge, about 7 px below it); add a render acceptance of +25 to +37 deg release pitch, since aimPitch lofts the throw; the thrower hit-over-attack check has not yet been run on a real clip. Known gaps for Kort: merge art/tidecallers-t1 so tc_harpooner has a model; restrict TroopMotion.bow to the bow family (Java); extend Tactics.drawsAhead to the thrower and hide the harpoon after release (Java and timing); one in-game render to confirm the +31 deg aimPitch, scale 1.0 and brim clearance.
