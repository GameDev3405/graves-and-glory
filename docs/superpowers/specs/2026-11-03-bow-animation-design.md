# Bow troops: animation spec

Kort, 2026-11-03 (request: find good game animation videos, break the moves down, apply them to the units properly). Branch `work/anim-rework`. References saved at `C:/Users/kortt/Melty/anim-refs/bow/` (SOURCES.md, `frames/<move>/` at 10 fps, `sheets/`; not committed).

## Why

Kort wants Souls-like weight, not historical or Minecraft-mod looks, and the weapon must always face the way the move needs (`docs/ANIMATION-EDITING.md`, "Weapon direction"). The bow family is small: two attack clips that differ only in arm and head pitch, one aim hold, one idle, and a draw timeline. Archers are the only troops with a long hold (aim) between shots, so the draw, hold and loose carry the look. The reference shows a low, grounded archer who plants, draws to the jaw, holds, looses with a hard string-hand snap, and reaches back over the shoulder for the next arrow.

## Goals

- The five bow troops draw, hold and loose like the reference: bow upright and facing the target, arrow along the line of sight, string hand at the cheek or jaw at full draw, a visible snap on release, a reach to the quiver before the next draw.
- No self-clipping: the bow, string hand and arm never pass through the head, helmet or body, at any angle, on the biped rig that all five share (see Application to units for which troops actually run these clips).
- Each move is approved by Kort one at a time, judged in game on the compare page next to the video.

## Units

Troops with `family = bow` (Spec.TROOPS). Range, cooldown (ticks), ability and loadout (helmet, chest) read from Spec.TROOPS; `rootScale()` read from `TroopModel` (field `rootK`, set from the rig by TroopModelLoader).

| id | name | house | entity | own TroopModel | helmet | chest | range | cooldown | ability |
|---|---|---|---|---|---|---|---|---|---|
| archer | Order Archer | Gilded | husk | yes (`models/troop/archer.json`, weapon `order_bow`) | chainmail | leather | 22 | 40 | steady |
| longbow | Order Longbow | Gilded | husk | yes (`longbow.json`, weapon `longbow`) | leather | leather | 30 | 45 | pierce_longbow |
| bonebow | Bone Archer | Graveborn | skeleton | no, vanilla renderer | chainmail | leather | 25 | 35 | fearless |
| stalker | Barrow Stalker | Graveborn | stray | no, vanilla renderer | chainmail | none (air) | 29 | 35 | frostshot |
| wm_hunter | Hide Hunter | Wildmoot | zombified_piglin | no, vanilla renderer | leather | leather | 20 | 36 | ambush |

**What the clips reach.** The clips drive a `TroopModel`. Only archer and longbow have one, so they are the only two units where a clip edit shows today. bonebow, stalker and wm_hunter draw with the vanilla skeleton, stray and piglin renderers and play no clip until a model is built for them (not in this scope; Questions 5). Their listed armor is vanilla armor layers on the vanilla body, so none of the clip-clipping checks below apply to them in game yet. The earlier draft's claim of thinner, longer-limbed skeleton and stray rigs was wrong: every biped troop shares `art/rigs/biped.json`, and there is no separate thin rig.

All have slotWeight 1 and no shield. Every cooldown is longer than the 24-tick clip, so shots never overlap.

## Inventory today

Rig: biped only. Folder `clips/biped/bow/` overrides `clips/biped/common/` by name (TroopMotion.build).

| Engine slot | Clip | Owner | Notes |
|---|---|---|---|
| stance `idle` | `bow/idle` | family | 4 s loop, bow lowered in the left hand at ease |
| stance `aim` (stanceKey `ranged_aim`) | `bow/aim` | family | 3 s loop, body yaw 90 (side-on), bow arm out at the target, right hand at the string |
| stance `rest` (`ranged_rest`) | none | missing | procedural fallback |
| stances `carry`, `guard`, `brace`, `wall`, `shield` | none | missing | procedural; bow troops on the move with an enemy use `guard`, without one `carry` (stanceKey `run`) |
| attack `bow_volley` (body, arrow, impact 10) | `bow/bow_volley` | family | 1.2 s, markers draw 0.22, full 0.36, impact 0.5 |
| attack `bow_direct` (body, arrow, impact 10) | `bow/bow_direct` | family | same shape as volley, flatter arm and head pitch |
| `bow_draw` | `bow/bow_draw` | family | 1.2 s pull timeline Weapon.frameAt reads (BOW_FULL 0.65, BOW_LOOSE 0.8); the name also aliases to the first attack |
| `*_wall` attacks | none | n/a | no formation hold for ranged troops |
| hits `hit_head/body/legs` (+ `_2`, `_3`) | `common/` | inherited | shared with every biped family |
| blocks | none | missing | archers do not block |
| deaths (`death*`, `death_arrow*`...), `fall`, `raise` | `common/` | inherited | shared |
| scenes | none family-owned | - | scene poses use `common/` clips |

Attack and hit clips are additive offsets over the stance (MotionStack.action); first and last key of every keyed channel is 0. Both attack clips key body, head, left arm/forearm/hand and right arm/forearm only; legs and root are unkeyed, so feet come from foot planting. The engine pitches only the head toward an aim; the bow arm's elevation comes from the clip.

## References

1. Essential Bow & Arrow Animation Pack, space.bar. anim (https://www.youtube.com/watch?v=FEWzL31g3Eo), `essential-bow-pack.mp4`. Main source: one shot cycle at 0:30.5-0:32.6 (`frames/shot_cycle`, 21 frames, f_001 = 30.5 s, 10 fps) and a side-on draw at 0:56-1:02 (`frames/aim_hold`, 60 frames, f_001 = 56.0 s). A run with the bow carried low at 0:12-0:20 (`frames/idle_ready`, plain run frames) is the carry reference. Frames are on disk under `C:/Users/kortt/Melty/anim-refs/bow/frames/`; contact sheets are in `sheets/`.
2. Horizon Archery System Test v1, Dark Tower Int. (https://www.youtube.com/watch?v=6MW9t4hZkik), `souls-archery-test.mp4`. Mood only (armoured Souls-like archer, bow vertical). Too dark and blurred to cite timestamps.

Not found: a standing idle (the 0:44-0:54 stretch was looked at; 51-54 s is extracted to `frames/idle_stand` and shows a crouch, roll and dodge, and 44-50 s is aim and crouch only, so no timestamp is cited for idle), a video with a clear high-angle volley, hit reactions or deaths for archers (searches returned mostly first-person packs and tutorials). Proposed sources: the volley is the direct draw with the bow pitched up; hits and deaths stay on `common/` unless Kort wants bow-specific ones (Questions).

## Stances

| Stance | Video | Target |
|---|---|---|
| `aim` | 0:56-0:57 (`frames/aim_hold` f_001-f_011, full draw hold, side-on) | Body side-on, feet planted wide, bow arm straight at shoulder height, bow upright (limbs vertical, belly toward the target), arrow along the line of sight, string hand at the cheek. Exactly which channels change is in the aim key table below. Weapon: bow faces the target. |
| `idle` | none (no standing idle in the video, see References) | Kept as today (`bow/idle`, 4 s, bow lowered in the left hand). Clipping check only; no re-animation without a reference. |
| `carry` (march) | 0:12-0:20 (`frames/idle_ready`, a run with the bow low in the left hand; carry only) | Optional family clip: bow low in the left hand, tip down and forward, right hand free. Procedural today; leave unless Kort asks. |
| `rest` | none | Procedural fallback stays. |

## Moves

Timing is fixed by the attack rows: impact at tick 10 (0.5 s) for both, clip at most 40 ticks. Today's clips are 1.2 s (24 ticks); the `draw`, `full` and `impact` markers (0.22, 0.36, 0.5) drive the bow sprite's pull frames and must not move. Weapon direction is stated at every key. "Target line" is the line from the archer's eye to the target.

### Kort's picks (2026-11-03): these override the rows, retime map and targets below where they conflict

Kort broke down the Essential Bow pack (FEWzL31g3Eo): normal draw 1:00-1:01, **slight tilt 1:03-1:04**, full tilt (far long shot) 1:05-1:06. Frames: `frames/kort_normal` (60-62 s), `frames/kort_slight` (63-65 s), `frames/kort_full` (65-67 s); f_n = start + 0.1 (n-1) s; sheets `sheets/kort_slight.png`, `sheets/kort_full.png`.

1. **`bow_direct` uses only the slight tilt.** Checked in `kort_slight` f_001-f_010 (63.0-63.9): full draw with the bow canted about 25 degrees off vertical (top limb leaning away from the string hand, toward the target side), string hand at the jaw; loose at about 63.1 (f_002); hand opens and holds by the face 63.2-63.6 (f_003-f_007); right arm rises up and back over the right shoulder 63.7-63.8 (f_008-f_009); arrow drawn out of the back quiver 63.9 (f_010). From 64.0 the video switches to a flat-bow style; not used. The normal and full-tilt windows are reference only, for angles the slight-tilt window hides.
2. **`bow_volley` uses the full tilt.** `kort_full` f_001-f_012 (65.0-66.1): bow rolled nearly flat (about 70-80 degrees off vertical), arrow level; loose, then the same over-the-shoulder reach at 66.2-66.4 (f_013-f_015). The game volley still needs its lob, so the volley is the full cant plus the 35-45 degree upward arrow angle from the volley target table. The lob on top of the full cant is my design (the video aims level); Kort judges the render.
3. **Every shot reaches to the back quiver right after release** (Kort's pick). Applies to all bow units for now (archer, longbow, bonebow, stalker, wm_hunter); models are checked per unit as they are built.
4. **New clip order** (replaces the old reach-first order, which nocked before the draw). The clip starts and ends in `aim` (arrow nocked, string hand at the cheek), so the markers stay put:

| Phase | Clip time (s) | Video (slight tilt, direct) | Pose | Weapon direction |
|---|---|---|---|---|
| cant and settle | 0.00-0.22 | 63.0 (f_001) | Bow rolls from upright to the cant (direct about 25, volley about 75 degrees, through left_forearm/left_hand roll); volley also lifts the bow arm and leans the chest back. | Arrow stays on the target line (direct) or climbs to 35-45 up (volley) while the limbs roll; belly toward the target. |
| final pull, hold | 0.22-0.50 | 63.0-63.1 (f_001-f_002) | String hand settles at the jaw; small tremor. Markers draw 0.22, full 0.36. | Canted bow, arrow along the line of sight. |
| loose | 0.50 | 63.1 (f_002) | String hand opens and snaps back past the jaw. | Bow holds its cant and line; tip dips at most 8 degrees. |
| follow-through | 0.50-0.62 | 63.2-63.6 (f_003-f_007), compressed | Open hand by the face, bow arm still. | Bow on the line, cant held. |
| quiver reach | 0.62-0.78 | 63.7-63.9 (f_008-f_010) | Right arm up and back over the right shoulder, hand behind the head at the quiver mouth, then draws the arrow up and out. Right arm pitch about -160 to -170 absolute at the top (check against helmet and back gear). | Bow stays on the line, cant easing back toward upright. |
| nock | 0.78-0.98 | `frames/shot_cycle` 31.9-32.1 (f_015-f_017); the slight-tilt window cuts before the nock | Arrow brought forward over the shoulder and laid on the string at the bow hand. | Arrow tip forward along the target line as soon as it clears the shoulder; never pointing at the archer's head. |
| redraw to aim | 0.98-1.20 | `frames/kort_normal` for the draw path | String hand back to the cheek; bow back upright; all deltas reach 0. | Bow faces the target, upright (aim). |

Open for the render: the bow's pull frames are marker-driven (slack after 0.5) while the hand redraws at 0.98-1.20; if the string looks wrong at the clip end, lengthen the clip toward 40 ticks or ask about a second pull timeline (needs Kort if it touches the sheet or Java).

| Clip | Video | Stance | Key | Time (s) | Pose | Weapon direction |
|---|---|---|---|---|---|---|
| `bow_direct` | 0:30.5-0:32.6 (shot cycle, retimed) | aim to aim | nock | 0.00-0.20 | Right arm reaches back over the shoulder to the quiver and brings the arrow to the string (video 31.7-32.1). Body unchanged. | Bow stays up in the left hand, belly to the target, tip up, no pitch change. Arrow points at the target as soon as it is laid on. |
| | | | draw | 0.22-0.36 | Right hand pulls to the jaw, elbow back at shoulder height, forearm in line with the arrow; body turns 8 degrees into the draw (video 31.1-31.2); head turns to look along the arrow. | Bow limbs vertical, bow arm straight, arrow level along the target line at chest height. |
| | | | full / hold | 0.36-0.50 | String at cheek or jaw, held with a tiny tremor. | Same: bow faces the target, arrow along the line of sight. |
| | | | impact (loose) | 0.50 | String hand opens and snaps straight back past the jaw (video 31.5). | Bow does not swing off the target; tips forward at most 8 degrees on the loose. |
| | | | follow-through | 0.55-0.70 | Hand open behind the ear, body recoils 2 degrees back, bow arm holds about 0.15 s (video 31.5-31.6). Deltas in the target table. | Bow still on the target line, tip 5-8 degrees low; no arrow on the string. |
| | | | recovery | 0.86-1.20 | Eases to all zeros (aim). | Bow returns to belly-to-target, upright. |
| `bow_volley` | none; the direct draw with the bow pitched up (0:56-0:58 shape) | aim to aim | nock, draw | 0.00-0.36 | As direct, but the bow arm rises and the chest leans back so the arrow climbs. | Bow tilted back so the arrow points 35-45 degrees above the target line, belly still toward the target, string drawn to the jaw. |
| | | | full / impact | 0.36 / 0.50 | Head tips back up to 6 degrees to sight along the arrow. | Arrow angled up toward the lob; limbs tipped back with it. |
| | | | follow-through, recovery | 0.55-1.20 | Recoil as direct, then eases to zero. | Bow tip drops from the high angle to the stance angle, not past it. |
| `aim` hold | 0:56-0:57 (frames/aim_hold f_001-f_011) | aim loop | hold | 0-3 | Breath-sway only, 0.5-1 degree. | Bow faces the target, arrow along the line of sight. |
| `hit_body/head/legs` | none | aim or idle | - | - | Inherited from common (see Application). | Bow stays on the target line through the recoil. |

### Retime map (video shot cycle to clip order)

The video cycle runs loose, reach to the quiver, nock, draw, hold (frames in `frames/shot_cycle`; f_n = 30.5 + 0.1 (n-1) s). The clip must start in aim, so it opens on the reach and nock, then draws, holds and looses, then ends in the follow-through and eases back to aim. The video's loose at the start of its cycle becomes the clip's loose at 0.5 s.

| Clip phase | Clip time (s) | Video range (s) | Frames | Video span to clip span |
|---|---|---|---|---|
| quiver reach | 0.00-0.10 | 31.7-31.8 | f_013-f_014 | 0.2 s to 0.10 s (x0.5) |
| nock (arrow to the string) | 0.10-0.22 | 31.9-32.1 | f_015-f_017 | 0.3 s to 0.12 s (x0.4) |
| draw | 0.22-0.36 | 30.9-31.3 | f_005-f_009 | 0.4 s to 0.14 s (x0.35) |
| hold | 0.36-0.50 | 31.3-31.5 | f_009-f_011 | 0.2 s to 0.14 s |
| loose | 0.50 | 31.5 | f_011 | impact tick fixed |
| follow-through | 0.55-0.70 | 31.5-31.6 | f_011-f_012 | 0.1 s stretched to 0.15 s (the pose is held) |
| recovery | 0.70-1.20 | none | none | eases to zero over 0.5 s |

Each video frame is used once: the quiver reach (f_013-f_014) and the follow-through (f_011-f_012) no longer overlap. The draw and hold ranges come before the reach in the video, so they are used out of video order on purpose.

**Camera of `aim_hold`.** Frames `frames/aim_hold/f_001-f_060` (e.g. f_005) are a 3/4 view from in front of and slightly to the side of the archer: the chest turned about 60 to 70 degrees from the camera, the bow arm reaching toward the lower right, the string-hand side away from the camera. The clip's `aim` body yaw is 90 (square side-on). So the video is less side-on than the clip. Use the video for the arm, hand, bow and head pose; do not copy its body turn. The body keys of `aim` stay at yaw 90 (the compare page should show the clip from a 3/4 camera about 20-30 degrees off square side-on to match the video).

### Key tables (current JSON values as the starting point)

Rotation degrees x/y/z from `clips/biped/bow/*.json` on this branch. `aim` is a stance (absolute values); the shots are additive offsets over `aim` (deltas). Bones not listed are unkeyed. Shot values between listed times interpolate linearly; aim uses catmull-rom. The right arm's reach at 0.10 is an existing part of the nock: it swings the arm to y 157 over the shoulder and back by 0.22.

**`aim`** (length 3.0 s, loop, no markers). Bow direction at every key: bow faces the target, arrow along the line of sight.

| t (s) | body | head | left_arm | left_forearm | left_hand | right_arm | right_forearm | root pos | left_arm pos |
|---|---|---|---|---|---|---|---|---|---|
| 0.0 | 0, 90, -10 | -3, -90, 0 | -5.1, -112.8, 20 | -100.2, -41.9, 0 | -38.6, -17, 36.8 | -97.8, -121.1, 22.9 | -0.1, 42.1, 13.9 | 0, 0.40, 0 | 0, 1, 0 |
| 0.8 | 0.4, 90.6, -10 | same | -6.3, -112.8, 20 | same | same | -98.8, -121.1, 22.9 | same | 0, 0.55, 0 | same |
| 1.7 | -0.3, 89.6, -10 | same | -4.5, -112.8, 20 | same | same | -97.2, -121.1, 22.9 | same | 0, 0.40, 0 | same |
| 2.4 | 0.2, 90.3, -10 | same | -5.5, -112.8, 20 | same | same | -98.2, -121.1, 22.9 | same | 0, 0.50, 0 | same |
| 3.0 | 0, 90, -10 | -3, -90, 0 | -5.1, -112.8, 20 | -100.2, -41.9, 0 | -38.6, -17, 36.8 | -97.8, -121.1, 22.9 | -0.1, 42.1, 13.9 | 0, 0.40, 0 | 0, 1, 0 |

Legs and shins in `aim` are constant: left_leg -4, 0, -4; left_shin 4, 0, 0; right_leg 3, 0, 4.

Channels that change in `aim`: only (a) `root.position` y, lowered by about 1 pixel (position is in pixels, per `docs/ANIMATION-EDITING.md`; the target keys become -0.60, -0.45, -0.60, -0.50, -0.60 at 0, 0.8, 1.7, 2.4, 3.0 s; the sway is kept; if the preview shows the archer rising instead, flip the sign), (b) `head.rotation` x, to look along the arrow (about -3 to -1; the preview decides), and (c) `right_arm` and `right_forearm` only if the preview shows the string hand missing the jaw. Channels that do not change: the leg and shin keys (`left_leg`, `left_shin`, `right_leg`) keep their exact current values, as do `body`, `left_arm` (rotation and its constant position y = 1), `left_forearm` and `left_hand`.

**`bow_direct`** (1.2 s, not looped, markers 0.22 / 0.36 / 0.50). Deltas over aim.

| t (s) | body | head | left_arm | left_forearm | right_arm | right_forearm | Bow direction |
|---|---|---|---|---|---|---|---|
| 0.00 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | in aim: faces the target |
| 0.10 | 0, 0, 0 | 0, 0, 0 | -6, -2, -2 | -1, -1, 0 | 3, 157, -11 | -56, 29, -17 | unchanged; right arm reaches over the shoulder |
| 0.22 | 0, 0, 0 | 0, 0, 0 | -13, -4, -4 | -2, -2, -1 | -10, 30, -10 | 42, 8, -38 | draw starts; faces the target |
| 0.36 | 0, -8, -2 | -1, 8, 0 | -48, -7, -31 | 34, -14, 24 | -27, 20, -30 | 44, 3, -59 | full: faces the target, arrow along the sight |
| 0.50 | 0, -8, -2 | -1, 8, 0 | -48, -7, -31 | 34, -14, 24 | -27, 20, -30 | 44, 3, -59 | loose |
| 0.55 | -4, -10, -2 | -1, 10, 0 | -1, -15, -50 | 34, -14, 24 | 142, 99, 112 | -28, -144, 1 | current follow-through: contradicts the rule (the left arm swings to -1 pitch / -50 roll and the hand rolls to 108/-67/-104); replaced by the target table below |
| 0.70 | -4, -10, -2 | -1, 10, 0 | -1, -15, -50 | 34, -14, 24 | 142, 99, 112 | -28, -144, 1 | same (current; replaced) |
| 0.86 | -2, -4, -1 | 0, 4, 0 | 0, -6, -20 | 14, -6, 10 | 57, 40, 45 | -11, -58, 0 | easing back |
| 1.20 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | in aim |

Also keyed, `left_hand` x/y/z: 0 at 0.0 and 0.15, -21, -16, 13 at 0.22, 41, -36, -42 at 0.36 to 0.50, 108, -67, -104 at 0.55 to 0.70, 43, -27, -41 at 0.86, 0 at 1.2. The 108/-67/-104 hand roll at the follow-through is the likely weapon-direction defect (the bow's grip turns well off the target line); the render step checks it first.

**`bow_volley`** (1.2 s, markers 0.22 / 0.36 / 0.50). Deltas over aim.

| t (s) | body | head | left_arm | left_forearm | right_arm | right_forearm | Bow direction |
|---|---|---|---|---|---|---|---|
| 0.00 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | in aim |
| 0.10 | 0, 0, 0 | 0, 0, 0 | -5, -2, -2 | -1, -1, 0 | 3, 157, -11 | -56, 29, -17 | unchanged |
| 0.22 | 0, 0, 0 | 0, 0, 0 | -11, -4, -4 | -1, -2, -1 | -8, 34, -10 | 40, 10, -39 | draw starts |
| 0.36 | 0, -8, -2 | -6, 8, 0 | -60, -17, -28 | 36, -18, 26 | -31, 48, -30 | 47, 19, -71 | full: the arrow should point 35-45 degrees up; the left arm pitch is only 12 more than the direct's, so measure the real angle in the preview |
| 0.50 | 0, -8, -2 | -6, 8, 0 | -60, -17, -28 | 36, -18, 26 | -31, 48, -30 | 47, 19, -71 | loose |
| 0.55-0.70 | -4, -10, -2 | -6, 10, 0 | -17, -24, -43 | 36, -18, 26 | 164, -24, 107 | -29, -136, -14 | current follow-through (replaced by the target table) |
| 0.86 | -2, -4, -1 | -2, 4, 0 | -7, -10, -17 | 15, -7, 10 | 66, -10, 43 | -12, -54, -6 | easing |
| 1.20 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | in aim |

The volley's `left_hand` roll follows the direct's shape (not re-listed). Today's volley differs from the direct by about 12 degrees more left_arm pitch and 5 degrees more head pitch, so it is not yet a clear 35-45 degree lob.

**Target tables (what the new keys should be; deltas over aim, degrees).** Only the rows that differ from the current JSON are given; the keys at 0, 0.10, 0.22 stay as in the current tables, and so do the first and last keys (0). Where a value is the full-draw value it is copied so the bow cannot swing off the target line. Rotation sign convention assumed from the draw: a more negative left_arm x raises the bow (-13 at 0.22 to -48 at 0.36 as the bow rises to the target line); the preview confirms the sign before any key is written, and if it is reversed the dip below flips.

**`bow_direct` targets**

| t (s) | body | head | left_arm | left_forearm | left_hand | right_arm | right_forearm | Bow direction |
|---|---|---|---|---|---|---|---|---|
| 0.36 | 0, -8, -2 | -1, 8, 0 | -48, -7, -31 | 34, -14, 24 | 41, -36, -42 | -27, 20, -30 | 44, 3, -59 | unchanged: faces the target, arrow along the sight |
| 0.50 (loose) | 0, -8, -2 | -1, 8, 0 | -48, -7, -31 | 34, -14, 24 | 40, -36, -42 | -27, 20, -30 | 44, 3, -59 | unchanged: on the target line |
| 0.55 | -2, -8, -2 | -1, 8, 0 | -41, -7, -31 | 34, -14, 24 | 40, -36, -42 | 142, 99, 112 | -28, -144, 1 | on the target line, tip 5-8 degrees low (left_arm x +7 from full draw; body pitch -2) |
| 0.70 | -2, -8, -2 | -1, 8, 0 | -41, -7, -31 | 34, -14, 24 | 40, -36, -42 | 142, 99, 112 | -28, -144, 1 | held: tip 5-8 degrees low |
| 0.86 | -1, -5, -1 | 0, 5, 0 | -28, -5, -21 | 23, -10, 16 | 27, -24, -28 | 57, 40, 45 | -11, -58, 0 | tip easing from low back to the stance angle, never above it |

Changes from the current JSON: left_arm and left_hand and the body and head at 0.55 and 0.70 (the bow now stays on the line instead of swinging to -1/-15/-50 and 108/-67/-104); left_arm, left_forearm, left_hand, body and head at 0.86 follow the held pose at 68 percent. The right arm values at 0.55-0.70 are kept (the authored string-hand snap) but must be checked for the hand passing behind the ear without entering the helmet. Their 142 degree pitch is the largest key in the clip.

**`bow_volley` targets** (arrow 35-45 degrees above the target line; body leans back 8, left arm pitched up 30 more than the direct's)

| t (s) | body | head | left_arm | left_forearm | left_hand | right_arm | right_forearm | Bow direction |
|---|---|---|---|---|---|---|---|---|
| 0.22 | 0, 0, 0 | 0, 0, 0 | -11, -4, -4 | -1, -2, -1 | -19, -16, 7 | -8, 34, -10 | 40, 10, -39 | draw starts, as today |
| 0.36 | -8, -8, -2 | -6, 8, 0 | -78, -12, -31 | 36, -18, 26 | 46, -31, -47 | -49, 48, -30 | 47, 19, -71 | full: arrow 35-45 degrees up (arm 30 above the direct's, body lean 8: about 38); the preview measures it and trims left_arm x within 10 degrees |
| 0.50 (loose) | -8, -8, -2 | -6, 8, 0 | -78, -12, -31 | 36, -18, 26 | 46, -31, -47 | -49, 48, -30 | 47, 19, -71 | same angle held |
| 0.55 | -6, -8, -2 | -6, 8, 0 | -71, -12, -31 | 36, -18, 26 | 46, -31, -47 | 164, -24, 107 | -29, -136, -14 | tip 5-8 degrees below the high angle, still on the line |
| 0.70 | -6, -8, -2 | -6, 8, 0 | -71, -12, -31 | 36, -18, 26 | 46, -31, -47 | 164, -24, 107 | -29, -136, -14 | held |
| 0.86 | -4, -5, -1 | -2, 5, 0 | -48, -8, -21 | 24, -12, 17 | 32, -22, -33 | 66, -10, 43 | -12, -54, -6 | tip easing down to the stance angle, never below it |

The right_arm x at full draw rises 18 degrees (-31 to -49) so the string hand follows the arrow up; check the elbow against the helmet. The left arm roll and yaw are the current values changed by 5 degrees at most, so the follow-through keeps the same plane as the draw.

Body mechanics, from the video: the archer stands braced with knees bent and weight low; hips and shoulders turn into the draw and snap back on the loose; the string-hand arm is the fast part, the bow arm barely moves. Legs are not keyed (foot planting owns them); a crouch is a root offset of about -1 px. The video cycle takes about 1.8 s; the retime map above compresses it to today's 1.2 s with the loose on tick 10. The video lacks: volley arc, hits, deaths.

## Application to units

- **Proportions (per unit).** `rootScale()` is 1.0 for archer and longbow: `TroopModel.rig()` (`TroopModel.java:312-315`) sets `rootK = (24 - this root pivot y) / (24 - the rig's root pivot y)`, the getter is `TroopModel.java:318`, and both `models/troop/archer.json` and `longbow.json` declare `"rig": "biped"`, so the two pivots are the same rig's and the ratio is 1 (derived from the code and the model files, not measured in game). Root offsets and arm reach need no correction. Arm geometry from `biped.json`: the upper arm pivots at the shoulder and is 6 px long, the forearm another 6 px, so shoulder-to-hand is about 12 px (0.75 block); the head is an 8 px cube sitting on the neck at the same height as the shoulder line. The string hand must end up at the jaw (about 5 to 6 px below the head's centre, 4 px in front of the face plane). The same geometry applies to archer and longbow; they differ only in headwear (chainmail vs leather) and the weapon model, not in the rig. bonebow, stalker and wm_hunter have no model, so no per-unit reach or jaw-reach number exists for them; if a model is built they would also use `biped.json` at their own `rootScale()`, to be read then.
- **Held item and grip.** The bows are cube-model weapons (`models/troop/weapon/order_bow.json` for archer, `longbow.json` for longbow), not sprites. Their pull frames are driven by the clip markers (draw 0.22, full 0.36, impact 0.5) and by the weapon model's pull times [0.15, 0.32, 0.48]. The bow turns 45 degrees at rest (`BOW_REST_YAW`) and upright into the plane of the shot as it draws (TroopMotion). It faces the target only if the left forearm and hand keys are right; check front, side and 3/4. There is no arrow model; the string hand's position implies it.
- **Clipping risks (per loadout).** archer: chainmail helmet and leather chest, so the string hand at the jaw can enter the helmet's cheek plate; longbow: leather helmet and chest, a lower profile, so the first one to clip is the elbow against the cape or quiver on the reach; both: bow arm through the head turn during draw, quiver or cape against the reach back over the shoulder (nock key), left forearm through the chest during the volley lean. The bonebow (chainmail and leather chest), stalker (chainmail, no chest) and wm_hunter (leather and leather chest) lists apply only if models are built for them.
- **Wall and formation.** None. Ranged troops keep their spacing; a wider stance must stay inside the unit's cell.
- **Scale.** `rootScale()` is 1.0 for both modelled units, so the aim crouch (root offset) needs no scaling.
- **Ability visuals.** Steady, pierce_longbow, fearless, frostshot (frost arrow), ambush are Spec abilities. Their visuals were not read and are not clip-driven as far as this spec found. The frostshot (stalker) and ambush (wm_hunter) visuals matter only once those two have models; for archer and longbow check steady and pierce_longbow in the render.
- **Hits while aiming.** Hits are additive over the stance and `common/` hits move the arms; during `aim` that can drag the bow off the target for a moment. Fix by a family-owned hit set (Questions), not by editing `common/`, which other families share.
- **Needs Kort:** nothing in this spec changes gameplay, network or timing. Items under Questions 2 would.

## Fixed limits

- Attack rows are fixed: `bow_volley` and `bow_direct`, body, arrow, impact tick 10; clip at most 40 ticks. Any change needs Kort.
- `draw`, `full` and `impact` markers stay at 0.22, 0.36, 0.5; the `bow_draw` timeline stays (BOW_FULL 0.65, BOW_LOOSE 0.8).
- First and last keys of every keyed channel are 0.
- `hit over attack` (MotionEngineCheck.hitOverAttack, line 1093, helper `hitWhileAttacking` line 1057): the fixture is hard-coded to `realSwordShield` (the sword_shield family, stance `hold`, shield on); the attack starts at tick 20, a body hit lands at tick 25, and the window ticks 26-35 must stay within 15 degrees/tick of the same attack without the hit (four seeds). It never loads a bow clip, so the existing gate says nothing about the bow family whether or not bow hits exist. The plan adds its own task (Task 5) that runs a bow-specific version over the inherited `common/` hits as well as any new ones.
- `python tools/clips.py` then `--verify`; `Spec.java` only through `tools/preflight.py`.
- Max 2 Minecraft instances; no computer-use on Minecraft; no push, publish or merge to main.

## Questions for Kort

1. Should archers get their own `hit_*` clips that keep the bow arm on the target (recommended), or stay on the shared `common/` hits?
2. Should the volley's upward angle follow the server's real launch pitch (the old design said the bow angle matches the arrow)? Today the clip fixes it. A real pitch is a protocol change and needs your approval.
3. Is a new `carry` clip (bow low in the hand, tip down) wanted, or does the procedural march stay?
4. Is a bow-specific death (shot while aiming) needed, or does the common set do?
5. Should bonebow, stalker and wm_hunter get their own TroopModel (so they play these clips), or stay on the vanilla renderers? Today they do not play the bow clips at all.

## Review

v1 graded 6.5/10: coverage 8, reference 6, weapon 7, per-unit 5, limits 8, executable 5.

Fixed in round 1:
- Added a key table per clip for `aim`, `bow_direct` and `bow_volley` from the current JSON, with the bow direction per key, and flagged the follow-through hand roll and the weak volley lob as the first things to check.
- Corrected the unit facts from `Spec.TROOPS`, `TroopModel` and `biped.json`: only archer and longbow have models; the loadouts are listed per unit; the bows are cube-model weapons, not sprites; there is no separate thin skeleton or stray rig; `rootScale()` is 1.0 and the arm is about 12 px.
- Stated that `MotionEngineCheck.hitOverAttack` tests the sword_shield thrust only and its tick 26-35 window lies after a 24-tick bow clip, and added a bow-specific hit-over-attack step (at most 15 degrees/tick) to the plan.
- Added the retime map from the video's shot cycle to the clip order, with frames on disk.
- Cited only timestamps with frames on disk; the 0:12-0:20 range is a run (carry only), and there is no standing idle reference.
- Stated exactly which `aim` channels change (root y, head pitch, possibly the right arm) and that the leg keys stay.

v2 7.3 (8.5/7/6.5/8/8/5.5). Fixes: target key tables (per-key deltas, follow-through on the target line with the tip 5-8 degrees low, volley left arm pitched for 35-45 degrees up) copied into plan Tasks 3-4 and the 0.55-0.70 contradiction removed; retime map split into reach and nock and follow-through (no frame reused); `aim_hold` camera stated against the clip's body yaw 90; hit-over-attack made its own task that runs without new hits and checks the common hits (the earlier claim that the window lies after a 24-tick clip is corrected: the fixture hits at tick 25 of a sword fixture and never loads a bow clip); commit order marked with the contingent commits; `idle_ready` trimmed to the plain run frames and the empty `volley_up` folder deleted; `rootScale` cited at `TroopModel.java:312-318`; the `left_arm` position y = 1 channel added to the aim table, with the crouch units in pixels.

Final (after fix round 2): 7.8 (8.5/7.5/7.5/8/8/7.5). Known gaps for Kort: retime takes the draw from 30.9-31.5 s (before the quiver reach; 32.2-32.6 s keeps video order, and the camera changes view after shot_cycle f_008); tip drop and the volley's 38° elevation rest on an unchecked arm sign, so Task 3's sign check blocks; the 0.50-0.55 string-hand snap (169° and 213° in one tick) needs a per-tick preview; the "current" tables round the JSON and omit sub-keys 0.255/0.29/0.325 (keep them); only archer and longbow play clips, and the scratch tools must be recreated from the knight's if missing; aim_hold view and hold times differ slightly from SOURCES.md (the spec is the authority).
