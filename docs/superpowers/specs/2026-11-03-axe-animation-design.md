# Axe family (Shieldbreaker) in the Souls-like style

Kort, 2026-11-03. Branch `work/anim-rework`. Reference: "Elden Ring - Full Greataxe Moveset Showcase" (youtu.be/KVRG_kIZlfs), saved at `C:/Users/kortt/Melty/anim-refs/axe/` (frames in `frames/<move>/`, notes in `SOURCES.md`; not committed).

## Why

Kort wants heavy, weighty Souls-like swings. The axe family has no clips today, so the Shieldbreaker plays the procedural fallback. The video's greataxe fighter crouches wide, winds the head far back, slams it in one commit and holds the crouch before recovering.

## Goals

- Axe stances and three attacks (`axe_overhead_chop`, `axe_side_hew`, `axe_haft_jab`) in the video's weight.
- Weapon direction right at every key (tables below). No axe through head, legs or body.
- Each move approved by Kort one at a time on the compare page.

## Units

One troop uses family `axe`:

| Troop | Model / rig | Held | Helmet / chest | Shield | HP / dmg / range | Cooldown | Ability |
|---|---|---|---|---|---|---|---|
| `ic_breaker` Shieldbreaker | vindicator, biped rig | `minecraft:netherite_axe` | iron helmet, iron chestplate | none | 38 / 8 / 2.8 | 32 ticks | `sunder` (passive, x1.4 vs shield troops) |

rootScale is 1.0 (inferred, not read from a vindicator model file): `TroopModels.java:83` maps `vindicator` to the `biped` rig, and `TroopModel.rig()` (~line 311) computes rootScale as (24 - own root pivot y) / (24 - rig root pivot y); both root pivots are y 0 in `art/rigs/biped.json`, so 24/24. No cape, strap or tabard, so no cloth springs.

## Inventory today

| Clip | Status |
|---|---|
| Stances idle, carry, guard | missing (common has none): procedural fallback. This plan adds them. |
| Stances rest, aim | not needed (no bow, no rest pose for melee); `MotionEngine.stanceChain` falls back to idle |
| Stances brace, wall, shield | not needed (no shield, no wall formation); they fall back to guard/idle. Needs Kort only if axe men should form up |
| `axe_overhead_chop`, `axe_side_hew`, `axe_haft_jab` | missing: procedural fallback. `clips.py` requires one clip per `ATTACKVARIANTS` id in the family bbmodel, so Task 1 adds zero-motion stubs with the impact marker; `_wall` not needed |
| hit_head/body/legs (+_2,_3) | inherited from `biped/common` |
| block* | none (no shield) |
| death_*, fall, raise | inherited from `biped/common` |

New folder: `clips/biped/axe/`, generated from `art/clips/biped/axe.bbmodel`. Clips are edited through `python tools/anim_edit.py open axe` and `apply`, never by hand-writing the JSON.

## References

The video gives the overhead chop and a sideways sweep (Executioner's Greataxe, 2H heavy). It has no haft/butt jab, and no jab reference exists on disk. Every cited timestamp has frames on disk.

## Conventions

- Right-hander. The model's right side is -x: `right_arm` pivots at x -5, `left_arm` at x +5 (`art/rigs/biped.json`). "Right" and "left" below are the fighter's own sides.
- Degrees. x = pitch (negative lifts an arm forward/up, 0 hangs, -90 is horizontal forward), y = yaw, z = roll. **+y turns toward the fighter's RIGHT, -y toward the LEFT**, as in the shipped `sword_shield_low_cut.json` (draw back right: body y +34, right_arm y +18; sweep left: y -32 and -34).
- Tables list bones body, head, right_arm, right_forearm, right_hand, left_arm, left_forearm, left_hand, root (px). Attack rows are deltas over the guard with the absolute pitch in brackets for the arm chain (guard + delta). First and last keys of every channel are 0.
- Haft pitch below horizontal (approximation for the two-handed grip) = 90 + (right_arm abs x + right_forearm abs x). Guard: -55 + -35 = -90, so the haft is near horizontal.
- Values are approximate starting points; check in `pose_preview.py`.

## Item assumptions (measure step)

The vanilla netherite axe item is 16 px long with the head about 6 px across at the far end. Assumed grip: right hand 3 px from the butt, left hand 9 px from the butt, so the head sits about 7 px beyond the upper hand, with 3 px of bit half-width. This is an assumption: Task 2 measures the head size and grip positions from a `pose_preview.py` render of the guard and corrects the clearance figures below.

## Stances (absolute poses; Kort picks the final look)

| Stance | body x/y | head x/y | right_arm x/y | right_forearm x | right_hand x | left_arm x/y | left_forearm x | left_hand x | root y px | Weapon direction |
|---|---|---|---|---|---|---|---|---|---|---|
| `guard` (video 5:29.3-5:29.8 crouch) | 16/12 | -14/-10 | -55/20 | -35 | 10 | -60/-25 | -40 | 10 | -3 | haft near horizontal across the chest (formula 0 deg), bit to the right and slightly up, edge forward |
| `idle` | 2/0 | 0/0 | -30/8 | -75 | 0 | -35/-10 | -70 | 0 | 0 | axe resting on the right shoulder, haft pointing up-back, bit behind the head, edge outward |
| `carry` (march) | 4/0 | 0/0 | -30/8 | -75 | 0 | -35/-10 | -70 | 0 | 0 | as idle; the march layers add the sway |

Guard breathes up to 3 deg on body x over its loop (length 2.4 s, loop). Legs: root y -3 px is the crouch; knee bend is left to the planting layer. The video's wider, lower crouch (about -6 px) is a known difference, accepted until Kort asks for more.

## Moves

### 1. `axe_overhead_chop` (impact 0.6 s = 12 ticks, heavy; clip 1.8 s = 36 ticks)

Video 5:19.0-5:22.0 (frames/axe_overhead_chop). Video impact 5:20.0 (1.0 s in); retimed to 0.6 s: wind-up 0.48 s, strike 0.12 s.

Arm cells are delta (absolute).

| t (s) | Video t | body x/y | head x | r_arm x/y | r_forearm x | r_hand x | l_arm x/y | l_forearm x | l_hand x | root y/z px | Weapon direction |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 5:19.0 | 0/0 | 0 | 0 (-55)/0 | 0 (-35) | 0 (10) | 0 (-60)/0 | 0 (-40) | 0 (10) | 0/0 | guard: haft near horizontal across the chest |
| 0.30 | 5:19.5 | -8/12 | -6 | -60 (-115)/14 | 0 (-35) | -15 (-5) | -55 (-115)/-14 | 0 (-40) | -15 (-5) | -1/0 | haft rising behind the right shoulder, bit up and back (pitch 60 deg past horizontal) |
| 0.48 | 5:19.8 | -14/16 | -10 | -95 (-150)/16 | +5 (-30) | -20 (-10) | -90 (-150)/-16 | +10 (-30) | -20 (-10) | -1/0 | peak: arm + forearm = -180, haft vertical (within 15 deg) behind the head, bit up, edge facing forward |
| 0.60 | 5:20.0 | 22/-4 | 8 | +15 (-40)/0 | +35 (0) | -10 (0) | +15 (-45)/0 | +40 (0) | -10 (0) | -3/+2 | IMPACT: arm + forearm = -40, haft 50 deg (+-5) below horizontal, bit edge first at target head height, pointing forward-down |
| 0.80 | 5:20.2 | 28/-2 | 10 | +30 (-25)/0 | +35 (0) | -10 (0) | +30 (-30)/0 | +40 (0) | -10 (0) | -4/+2 | held low: haft about 65 deg below horizontal, bit near the ground in front, edge down |
| 1.30 | 5:20.8 | 18/0 | 5 | +20 (-35)/0 | +20 (-15) | -5 (5) | +20 (-40)/0 | +20 (-20) | -5 (5) | -3/+1 | crouch hold, bit low in front |
| 1.80 | 5:21.5 | 0/0 | 0 | 0 (-55)/0 | 0 (-35) | 0 (10) | 0 (-60)/0 | 0 (-40) | 0 (10) | 0/0 | guard |

Clearance (rig px; bit half-width 3 px, assumed above): at the peak the haft centreline stays at least 9 px right of the head centreline, so the bit edge (6 px) clears the helmet half-width (5 px) by 1 px; the vindicator nose sticks out about 2 px in front of the face (not in the biped rig file; unverified), so on the downstroke 0.48-0.60 s the bit stays at least 2 px in front of the nose line; at impact the haft is at least 3 px in front of the chest face (chestplate layer included).

### 2. `axe_side_hew` (impact 0.6 s, heavy; 1.8 s)

Video 5:29.3-5:32.3 (frames/axe_side_hew): crouch 5:29.3-5:29.8, haft level out to the side 5:30.2-5:31.0, sweep, impact and dust at **5:31.5**. The clip maps 5:29.3-5:31.5 (2.2 s) onto 0-0.6 s, a 4x (about 3.7x) compression, so the clip is a quick flat sweep, not the video's slow loop-and-slam. Per ANIMATION-EDITING.md: drawn back level on the fighter's RIGHT, then swept across the front to the LEFT, edge leading. The video's wind-up is on the left and loops over the head; we mirror the side and drop the loop. The impact is lifted on purpose from the video's low strike near the ground to waist height, because the hew's zone is body, not legs. After impact the clip plays near real time (0.85 s = 5:31.8, 1.30 s = 5:32.3).

Hands stay at hip height throughout (arm abs -15, forearm abs -50), so the haft stays level at the waist.

| t (s) | Video t | body x/y | head y | r_arm x/y | r_forearm x | r_hand x | l_arm x/y | l_forearm x | l_hand x | root y/z px | Weapon direction |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 5:29.3 | 0/0 | 0 | 0 (-55)/0 | 0 (-35) | 0 (10) | 0 (-60)/0 | 0 (-40) | 0 (10) | 0/0 | guard |
| 0.35 | 5:30.6 | 4/45 | 12 | +40 (-15)/45 | -15 (-50) | -10 (0) | +45 (-15)/35 | -10 (-50) | -10 (0) | -1/-1 | wind-up: fighter turned right; haft level at the waist, bit out on the right behind the body, edge toward the swing |
| 0.50 | 5:31.1 | 4/55 | 14 | +40 (-15)/55 | -15 (-50) | -10 (0) | +45 (-15)/42 | -10 (-50) | -10 (0) | -1/-1 | peak: haft horizontal (within 10 deg), bit trailing far right |
| 0.60 | 5:31.5 | 8/-30 | -8 | +40 (-15)/-40 | -15 (-50) | -10 (0) | +45 (-15)/-30 | -10 (-50) | -10 (0) | -2/+2 | IMPACT: hands at the hip, haft horizontal (+-10 deg) at waist height across the front, bit edge leading to the left |
| 0.85 | 5:31.8 | 14/-45 | -12 | +40 (-15)/-55 | -15 (-50) | -10 (0) | +45 (-15)/-45 | -10 (-50) | -10 (0) | -3/+2 | follow-through: haft horizontal pointing left, bit past the target, edge forward |
| 1.30 | 5:32.3 | 8/-15 | -5 | +20 (-35)/-20 | -8 (-43) | -5 (5) | +20 (-40)/-15 | -5 (-45) | -5 (5) | -2/+1 | haft coming back across the chest, bit up |
| 1.80 | n/a | 0/0 | 0 | 0 (-55)/0 | 0 (-35) | 0 (10) | 0 (-60)/0 | 0 (-40) | 0 (10) | 0/0 | guard |

Clearance: at the 0.5 s peak the bit trails 12+ px behind the right hip and at least 2 px clear of the body's back; across the front the haft passes at least 3 px in front of the chest face; at 0.85 s the bit points left past the left forearm by at least 2 px.

### 3. `axe_haft_jab` (impact 0.35 s = 7 ticks, light, blunt; 1.1 s) - NEEDS KORT (tied to Question 1)

Video: none, and no other reference on disk; the pose below is designed here, not copied. frames/axe_haft_jab is a rejected candidate (a ground sweep).

A vanilla netherite axe in the hand has the head on the end away from the grip and about 2 px of handle beyond the fist. "Butt leading" therefore needs the item flipped in the grip: the hands roll the item so the head points back over the shoulder (right hand x +150). Even then only about 2 px of handle leads. Do not build it on the vanilla item until Kort answers Question 1; with a great-axe model the butt is 20+ px past the grip. Rule: no key in this clip may have the axe head leading. Task 1 holds a zero-motion stub so the build passes meanwhile.

| t (s) | body x/y | head x | r_arm x/y | r_forearm x | r_hand x | l_arm x/y | l_forearm x | l_hand x | root y/z px | Weapon direction |
|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0/0 | 0 | 0 (-55)/0 | 0 (-35) | 0 (10) | 0 (-60)/0 | 0 (-40) | 0 (10) | 0/0 | guard |
| 0.18 | -4/-10 | 0 | +15 (-40)/-8 | -25 (-60) | +140 (150) | +5 (-55)/6 | -20 (-60) | +10 (20) | 0/-1 | haft drawn back level at the hip, butt end pointing at the target, axe head back over the shoulder (item flipped by the hand roll) |
| 0.35 | 8/0 | 2 | +25 (-30)/0 | +30 (-5) | +140 (150) | +30 (-30)/0 | +35 (-5) | +10 (20) | -2/+3 | IMPACT: arm + forearm = -35, so the haft is within 8 deg of level at chest height; driven straight at the target, butt leading, yaw within 2 deg of the body's forward line |
| 0.55 | 10/0 | 3 | +30 (-25)/0 | +35 (0) | +140 (150) | +35 (-25)/0 | +40 (0) | +10 (20) | -2/+3 | held, butt still forward |
| 1.10 | 0/0 | 0 | 0 (-55)/0 | 0 (-35) | 0 (10) | 0 (-60)/0 | 0 (-40) | 0 (10) | 0/0 | guard |

## Application to units

- Shieldbreaker: see the clearance lines under each move. The held item is the vanilla netherite axe at one-handed size, so the two-handed grip looks like a hatchet with the left hand on the haft (assumption above). Needs Kort: a custom two-handed great-axe model (art/weapons, like `arming_sword.bbmodel`) before the poses can be judged fairly.
- Clipping is checked in guard, chop impact, hew wind-up and jab impact against the iron helmet and chestplate.
- No ability visual (sunder is passive).

## Fixed limits

- Impact ticks fixed by `Spec.ATTACKVARIANTS`: chop 12, hew 12, jab 7; clips at most 40 ticks. Changes need Kort.
- `python tools/clips.py --verify` needs a clip for every `ATTACKVARIANTS` id in the family bbmodel; Spec.java only through `tools/preflight.py`; afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- `MotionEngineCheck.hitOverAttack` (`test-tools/standalone/MotionEngineCheck.java` ~line 1093, driver `hitWhileAttacking` ~line 1057) runs the `sword_shield` fixture only. Its timing: the attack starts at tick 20, a body hit lands at tick 25, so the checked ticks 26-35 are attack ticks 6-15. The family copy (plan Tasks 3-5) is build-time work, not tonight's.
- At most 2 Minecraft instances; no computer-use on Minecraft; no push or merge.

## Questions for Kort

1. Build a custom two-handed great-axe model for the Shieldbreaker (art work), or keep the vanilla netherite axe? The haft jab depends on it.
2. The video has no haft jab: supply a reference, or keep the designed jab?
3. Should the Shieldbreaker get wall/brace poses?
4. Accept the shallower crouch (root -3 px) versus the video's wider one?

## Review

v1 6.3 (8/6/6/6/7/5). Fix round 1: hew y signs, jab needs-Kort, hand columns, stance tables, rootScale and clearance, numeric bounds, rest/aim/brace/wall, hit-over check moved to attack tasks.

v2 6.3 (8/6/6/7/7/4). Fix round 2: y sign convention flipped to +y = right per sword_shield_low_cut and hew columns flipped; stub clips for ATTACKVARIANTS and anim_edit workflow; absolute pitches reconciled (chop impact haft 50 deg, hew hands at hip, chop peak agrees with plan); hit-over window rewritten as attack ticks 6-15 with fixture, command and Expected; jab spear-reference claim removed; hew impact 5:31.5 with 4x compression; axe head size and grip assumed with a measure step; crouch legs accepted as a known difference.

Final (after fix round 2): 6.5 (8/6/6/7/7/5). Known gaps for Kort: (1) `anim_edit.py open axe` cannot run until `axe: ic_breaker` is added to TROOP and an axe.bbmodel exists (tooling change, coordinator); (2) haft pitch numbers contradict the claimed level hew and jab (forearm needs about -75 hew, -60 jab impact, body pitch not added); (3) root signs wrong: crouch must be root y positive and impact lunge root z negative, per shipped guard and low_cut; (4) Expected git status omits the pre-existing preflight/Spec/sword_shield changes and the template should come from HEAD; (5) realAxe MEC fixture underspecified (shield=false, hold, attack order, Expected line); (6) chop arm yaws split the two hands on the haft. Fix all six at build time before any key is authored.
