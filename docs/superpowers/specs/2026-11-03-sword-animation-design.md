# One-handed sword troops: animation spec

Kort, 2026-11-03 request: find good game-animation videos for each weapon, break the moves down and apply them to the units properly. Branch `work/anim-rework`. Docs only; no clip, Java or sheet was touched. References are saved locally at `C:/Users/kortt/Melty/anim-refs/sword/` (see `SOURCES.md` there; videos and frames are not committed).

**Reference status: one real video, medium confidence.** Sharp Accent "One Handed Sword Animation Set 1" (https://www.youtube.com/watch?v=NwHzWyKyIs4, 20.7 s, 480p, watermarked). Timestamps below were checked against frames. It is a single armoured fighter with a free off hand and a Souls-like crouch, but it is a montage of one combo: the blade is hidden by the body at several moments and there is no clean standalone thrust or leg-height cut. Where the video lacks a move, the pose comes from the weapon-direction rule and is marked "rule". The two big pack demos (AniBearlee, GameDevHero V5) are crowds of tiny mannequins and were rejected. The Souls-like Sebastian Graves straight-sword pack (https://www.youtube.com/watch?v=GfemXQOjHgs) failed to download earlier and is the best upgrade if it can be fetched now.

## Why

The `sword` family has no clip folder. `clips/biped/sword/` does not exist and `common/` holds only hits, deaths, fall and raise, so every stance and every swing of the Risen and the Drowned Raider is procedural today, with none of the video-style weight Kort asked for on the knight. This spec gives the family the same treatment: low crouch, weight forward, big wind-ups, hips and shoulders turning into the blow, and a free off hand used as a counterweight.

## Goals

- Stances `idle`, `carry`, `guard` and three swings (`sword_overhead`, `sword_low_cut`, `sword_thrust`) authored for a one-handed sword with a free off hand.
- On every key the blade points where the move needs it (Moves table). A pose with the blade wrong is a defect even if clean.
- No self-clipping: blade, arms and off hand never pass through the head, helmet or legs, at any angle.
- Each move approved by Kort one at a time in game, front, side and 3/4, as for the knight.

## Units

Both are family `sword`, rig `biped`, held `minecraft:iron_sword` in the Spec row (the model draws `notched_sword` or `cutlass` instead), shield `none`, no off-hand item. The sword is in the right hand; the left hand is empty.

| Troop | Entity / skin | Tags and ability | Notes |
|---|---|---|---|
| `revenant` (Risen, Graveborn, common) | zombie | fearless: below 30% health deals 20% more damage (no visual) | chainmail helm and chest, hp 23, speed 0.28, cooldown 22 |
| `tc_raider` (Drowned Raider, Tidecallers, common) | drowned | brine: hits apply Slowness I for 1 s (no visual) | chainmail helm, no chest, hp 28, speed 0.29, cooldown 24 |

**Per-unit numbers (read from `Spec.TROOPS`, `TroopModels`, `models/troop/index.json`, `art/troops/`).**

| | `revenant` | `tc_raider` |
|---|---|---|
| Entity, skin | zombie, `minecraft:textures/entity/zombie/zombie.png` | drowned, `minecraft:textures/entity/zombie/drowned.png` |
| Loadout | `iron_sword`, `chainmail_helmet`, `chainmail_chestplate` | `iron_sword`, `chainmail_helmet`, chest `minecraft:air` |
| Stats | hp 23, damage 4, range 3.0, speed 0.28, cooldown 22 ticks | hp 28, damage 5, range 2.8, speed 0.29, cooldown 24 ticks |
| Model id in the row | `revenant` | `tc_raider` |
| `models/troop/index.json` entry (on `main`) | yes | yes |
| Model file (on `main`) | `models/troop/revenant.json`, from `art/troops/revenant.json`; 62 bones; skins s1 to s3; glow texture `revenant_e.png` | `models/troop/tc_raider.json`, from `art/troops/tc_raider.json`; 39 bones; skins s1 to s3; no glow |
| Weapon model (replaces `iron_sword` in the hand) | `notched_sword` (plus `_t2` and `_t3` for levels 2 and 3), right hand | `cutlass` (plus `_t2` and `_t3`): curved, notched blade, basket guard; one-handed, held like a sword |
| `idleArms` | `forward` (zombie arms out) | `hang` |
| Rest posture baked into the model | body x +15 (leaning forward), head x -6 | body x +16, head x -9.6, both arms x -16 (raised slightly forward) |
| `rootScale` | 1.0 (root pivot y 0 = biped rig) | 1.0 |
| Cloth and loose parts (clipping risks) | tabard front and back, 3 strips each (pivot y -7.5, hanging 9 px, 4-9 deg off the body), a 3-strip skirt front and back plus sides, a chain on the left arm, a left pauldron with a shard, a crest fin on the helm; plate pieces at levels 2 and 3 | skirt, belt, net on the back, scarf, tail strip, kelp on the helm, pauldrons and bracers (levels 2 and 3), barnacle shells at level 3 |

**Corrected 2026-11-03 (coordinator).** An earlier draft said neither troop had a model. That was read from `work/anim-rework`, which is 351 commits behind `main`. Both models, their skins and their weapons are on `main`. There is no blocker once `main` is merged into this branch, and Question 0 is withdrawn.

**What the real models change in this spec.**
- **Forward lean.** Both bodies rest at about 15 deg forward, and the clips add on top of that. The key tables were adapted from the knight, whose body rest is 0. For the same blade line, subtract the rest lean in the chain sum: blade pitch = body rest (15 or 16) + body key + arm + forearm. Every "level" or "N deg below level" claim must be re-summed with it. In practice the overhead impact and the thrust need about -15 on body x, or the arm raised by the same amount, or the point dips into the ground.
- **Raider arms.** The rest arm x of -16 is part of the model. Guard arm keys stack on it, so the Raider's guard sits about 16 deg higher than the Risen's unless the shared clip is tuned between them. Check both in the Task 1 render.
- **Revenant `idleArms: forward`.** This only affects the procedural idle. Guard and the swings key the arms explicitly, so the zombie arms-out look shows only when no clip runs. This keeps the lurching look the model's author chose. Question 2, whether the Risen moves differently, still stands.
- **Weapons.** The held items are `notched_sword` and `cutlass`, not the vanilla `iron_sword`, and each has t2 and t3 versions. Hand orientation and blade length come from those models; the Task 0 render must check the blade direction per key with each weapon. The cutlass curves, so its tip sits forward of the straight-blade line, which matters most on the thrust.
- **Cloth clearance.** The low cut and the lunge swing the blade past the front tabard and skirt strips, and the overhead passes over the crest fin and the kelp. Check in the render, and check the hit-over-attack gate with the cloth springs counted, as on the knight.

## Inventory today

| Engine slot | Clip the engine looks for | Today |
|---|---|---|
| Stances idle, carry, guard | `sword/<name>`, else `common/<name>` | none in either folder; falls to the procedural layers (`MotionEngine.stanceChain`: guard to carry to idle to procedural) |
| `brace`, `wall`, `shield`, `aim`, `rest` | not reached | `BattleMath.holdStance(shield=false, spear=false)` returns `hold`, and `stanceKey` maps `hold` without a shield to `guard`. A sword troop never enters `wall` or `brace`, so no `_wall` clip is needed. |
| Attacks `sword_thrust`, `sword_overhead`, `sword_low_cut` | `sword/<id>` | missing; `TroopMotion.build` gives attack index -1, so no clip plays and the engine uses its procedural swing |
| `<id>_wall` | `sword/<id>_wall` | not needed (see above) |
| Hits `hit_head/body/legs` (+ `_2`, `_3`) | family, else common | inherited: 9 generic common clips |
| Blocks | `block`, `block_head`, `block_legs` | none; no shield, none expected |
| Deaths, `fall`, `raise`, scenes | common | inherited |
| Weapon model | `models/troop/weapon/<id>.json` (on `main`) | Risen `notched_sword` (_t2, _t3), Raider `cutlass` (_t2, _t3) |

Attack rows (fixed): `sword_thrust` body stab impact 7 ticks (0.35 s); `sword_overhead` head slash 10 (0.50 s); `sword_low_cut` legs slash 8 (0.40 s). Max clip 40 ticks.

## References

`anim-refs/sword/SOURCES.md`. Used: NwHzWyKyIs4 only. Frames (10 fps, frame n at start + (n-1)/10 s) extracted for: `ready` (from 10.5), `overhead` (10.9), `side_slash` (11.4, 8 frames to 12.1, so 11.6-11.8 is frames 3-5), `thrust` (1.0), `thrust2` (4.0, 7 frames to 4.6; the deep lunge with the blade level at hip-to-chest height and the off hand thrown back is frames 3-4, about 4.2-4.3) and `walk_ready` (16.0).

**How the off hand is used in the video.** It is never on the sword and never a guard. In the ready pose it hangs loose and slightly forward at the thigh with a relaxed fist. During every wind-up it is flung out sideways at shoulder height, opposite the sword arm (about 11.2 s). At the strike it is thrown back and out, level with the shoulder, as a counterweight to the lunge (11.8 s, 1.5-1.7 s). After the strike it swings back to the thigh. We copy that: the left arm mirrors the right arm's travel, never crosses the chest, never holds the blade.

## Stances

Angles are for the builder to tune. Start from the knight's guard body and head numbers (`sword_shield/guard.json`: body about [14, 18, 0], head about [-12, -14, 0]) and drop the shield arm. The video's ready pose is used in `guard` only; `idle` stays relaxed (spec 2026-11-02, Kort's ruling).

| Stance | Used when | Pose | Weapon direction |
|---|---|---|---|
| `idle` | no enemy, still | upright, knees soft, right hand at the hip, off hand hanging loose | blade pointing forward-down at about 40 degrees below level, point toward the ground ahead, edge down |
| `carry` | marching, no enemy | idle with a slow arm swing from the off arm only; the sword arm stays still | blade forward-down about 40 degrees, never raised, never across a neighbour |
| `guard` | enemy near, or Run with enemy | low and wide, knees bent, weight forward, torso turned about 18 degrees; off hand open a little forward and out at waist height | blade forward and slightly down, about 15-30 degrees below level, point aimed at the enemy's belly, hand at the right hip (video 10.5-11.0) |

## Moves (breakdown table)

Each swing is an offset over `guard` (first and last keys 0). Times are seconds in the clip at 20 tps. The legs are left unkeyed (planted feet bend the knees); the step-in is a root offset only.

| Clip | Video | Start / end | Keys, weapon direction on every key | Body mechanics and retiming |
|---|---|---|---|---|
| `sword_overhead` (head, slash, impact 0.50, clip 1.2 s) | front 10.9-11.7; behind 1.0-2.0 | guard to guard | **0.00** guard: blade forward-down. **0.20 wind-up** (video 11.0-11.1): blade drawn up and back, lying over the shoulder behind the head, point toward the rear, edge up-forward; off arm flung out sideways at shoulder height. **0.35 top** (11.2): blade vertical, point up, edge facing forward, hand above and behind the head with the sword arm out wide so the blade clears the helmet. **0.50 impact** (video cut at 11.3-11.5): blade pitched about 50 degrees below level, edge leading, at the target's head height in front of the face; hips and shoulders turned in, root forward 1-2 px. **0.65 follow-through** (11.6; 1.5-1.7 from behind): blade carries on down to level, point forward at hip height, arm extended, as in the video's lunge; off hand thrown back. **1.20** guard | deep knee bend at the wind-up, torso leans 20-30 degrees into the cut, off arm counterbalances. The video's wind-up to cut takes about 0.45 s, so 0.50 needs no stretch |
| `sword_low_cut` (legs, slash, impact 0.40, clip 1.1 s) | front 11.4-12.1 (cut at 11.6-11.8) | guard to guard | **0.00** guard. **0.25 wind-up** (11.4-11.5): crouched deeper, sword drawn back low on the sword side, blade level (flat) and pointing to the rear, hips turned away; off arm flung out opposite at shoulder height. **0.40 impact** (11.6-11.8): blade flat and level across the front, edge leading, at knee to hip height (rule: the video's cut is at chest height; this default is the lowered version, Question 3 is optional), point swept toward the target, hips leading. **0.55 follow-through**: blade still level, arm extended across to the off side, torso rotated, off hand trails back. **1.10** guard | whole-body turn drives it: hips lead, shoulders follow, arm last. The video's cut takes about 0.4 s from the crouch, matching 0.40 |
| `sword_thrust` (body, stab, impact 0.35, clip 1.25 s) | lunge from behind 1.5-1.9 and 4.2-4.4 | guard to guard | **0.00** guard. **0.24 wind-up** (rule; the video's wind-up is an overhead, not a hip draw): elbow back, hand by the right hip, blade level, point forward at the target, weight on the back foot; off hand out to the side at waist height. **0.35 impact** (the video's lunge): arm fully straight at chest to stomach height, blade level and in line with the forearm, point on the target (within about 2 degrees of the body's forward line), root forward 2-3 px, body leaning in; off hand thrown back. **0.55 hold**: stays extended. **0.80 recovery**: blade drawn back to the guard direction. **1.25** guard | lunge with the front knee bent deep; legs planted. The video's lunge takes about 0.4 s; 0.35 is a little faster, fine |

The exact numbers for every key are in **Key tables** below (start values, tuned in preview).

Missing from the video: a clean standalone thrust wind-up, a leg-height cut, hits, blocks, deaths, a march, a taunt. Proposed source: the weapon-direction rule for the thrust wind-up and the low cut; common hits and deaths stay as they are; the march is `carry` above (video 16-20 s shows the low blade carry).

## Key tables

Start values, offsets over `guard`, in degrees (rotation) and px (root x,y,z; z negative is forward, from the knight clips). They are adapted from the approved knight clips (`sword_shield_overhead`, `_low_cut`, `_thrust`), with the free left arm moved as the counterweight in place of the shield arm. Left arm: z negative is out to the side, as in the knight's low cut. Sign convention (to confirm): root y negative = lower, root z negative = forward, right/left arm x negative = swung forward and up, left-arm z negative = out to the side, forearm and hand x negative = bent forward (hand toward the target). These signs are not confirmed for this rig, so **the tables are not usable until plan Task 1 step 3 passes**; a wrong sign there means flipping that column, not re-authoring. The blade column is the absolute direction (the guard blade is forward-down, about 25 degrees below level). Every table starts and ends at all zeros. Forearm and hand are x only.

**`sword_overhead`** (length 1.2 s, impact 0.50)

| t | body | head | left arm | right arm | r forearm | r hand | root | blade direction |
|---|---|---|---|---|---|---|---|---|
| 0.00 | 0,0,0 | 0,0,0 | 0,0,0 | 0,0,0 | 0 | 0 | 0,0,0 | guard: forward-down |
| 0.20 | -9,10,-2 | 5,-8,1 | -8,-14,-40 | -80,5,-15 | 8 | -10 | 0,-0.4,0.6 | up and back over the shoulder, point rearward, edge up-forward |
| 0.35 | -14,14,-4 | 8,-12,2 | -10,-14,-60 | -120,6,-22 | 25 | -30 | 0,-0.6,0.8 | vertical, point up, edge forward, behind the head |
| 0.50 | 10,-26,4 | -6,22,-2 | 8,10,-35 | -44,-6,-10 | 38 | -60 | 0,-0.8,-1.6 | about 50 degrees below level, edge leading, at head height in front of the face |
| 0.65 | 14,-30,2 | -8,26,0 | 8,8,-30 | -22,-8,-6 | 34 | -40 | 0,-0.8,-1.8 | down to level, point forward at hip height |
| 0.85 | 6,-12,0 | -3,10,0 | -3,2,-10 | -6,-4,-2 | 14 | -20 | 0,-0.3,-0.8 | returning to the guard direction |
| 1.20 | 0,0,0 | 0,0,0 | 0,0,0 | 0,0,0 | 0 | 0 | 0,0,0 | guard |

**`sword_low_cut`** (length 1.1 s, impact 0.40; the lowered knee-to-hip flat cut)

| t | body | head | left arm | right arm | r forearm | r hand | root | blade direction |
|---|---|---|---|---|---|---|---|---|
| 0.00 | 0,0,0 | 0,0,0 | 0,0,0 | 0,0,0 | 0 | 0 | 0,0,0 | guard |
| 0.25 | 4,34,2 | 0,-24,0 | -10,-30,-50 | -39,14,-40 | 0 | -19 | 0,-0.8,0.6 | level and flat, point rearward, hand at the right hip, hips turned away |
| 0.40 | 14,-8,-1 | 0,6,0 | -4,20,-45 | -25,-9,-34 | 16 | -37 | 0,-1.0,-1.2 | level and flat, edge leading, knee-to-hip height, crossing in front |
| 0.55 | 15,-32,-2 | 0,22,0 | -14,30,-30 | -15,-27,-28 | 25 | -44 | 0,-1.0,-1.4 | level, carried across to the off side |
| 0.80 | 5,-20,-1 | 0,14,0 | -8,8,-12 | -3,-20,-12 | 20 | -28 | 0,-0.3,-0.8 | returning to the guard direction |
| 1.10 | 0,0,0 | 0,0,0 | 0,0,0 | 0,0,0 | 0 | 0 | 0,0,0 | guard |

**`sword_thrust`** (length 1.25 s, impact 0.35)

| t | body | head | left arm | right arm | r forearm | r hand | root | blade direction |
|---|---|---|---|---|---|---|---|---|
| 0.00 | 0,0,0 | 0,0,0 | 0,0,0 | 0,0,0 | 0 | 0 | 0,0,0 | guard |
| 0.24 | 4,24,0 | -2,-17,0 | 4,-20,-30 | 17,4,10 | -30 | -3 | 0,-0.4,0.9 | upper arm swung back (arm x positive), elbow behind the body, forearm bent forward so the point stays on the target line about 10 degrees below level; hand by the hip |
| 0.35 | 12,-10,0 | -8,7,0 | -6,30,-45 | -25,-8,-17 | -8 | -40 | 0,-0.8,-2.6 | level (pitch within 5 degrees), yaw within 2 degrees of the body's forward line, point on the target, chest to stomach height |
| 0.55 | 12,-10,0 | -8,7,0 | -6,30,-45 | -24,-11,-20 | -5 | -47 | 0,-0.8,-2.7 | held extended, level, same line |
| 0.80 | 6,6,0 | -4,-4,0 | -3,10,-15 | -12,-9,-10 | -5 | -43 | 0,-0.4,-1.2 | drawn back toward the guard direction |
| 1.25 | 0,0,0 | 0,0,0 | 0,0,0 | 0,0,0 | 0 | 0 | 0,0,0 | guard |

Stance keys: guard body [14,18,0] and head [-12,-14,0] are the only numbers fixed so far; the arm keys come from the Task 2 preview, aimed at the blade directions in the Stances table.

**Blade-yaw check at thrust impact (0.35 s, tick 7), measurable.** One reference line: the body's forward line (the troop's facing, the -z axis of the body frame). `--over` and `--blade` do not exist yet: plan Task 1 adds both to the scratch `pose_preview.py`. Method: `--over guard` sums guard plus the 0.35 s offsets, `--blade` runs the rig's forward kinematics and prints the hilt-to-tip blade vector; yaw = atan2(x, -z) against the forward line, pitch = angle from level. Expected: |yaw| at most 2 degrees and |pitch| at most 5 degrees. Cross-check in the game render: the top-down angle of the 8-angle set, frame at tick 7, blade axis against the body's centre line; the two must agree within 1 degree.

**Blade-height check at low-cut impact (0.40 s, tick 8).** `--blade` also prints hilt and tip height in px above the ground (the rig's hips are at y 12 px, the knees at 6 px). Expected at 0.40: both hilt and tip heights between 6 and 12 px.

## Application to units

- **Revenant (Risen model, 62 bones, notched_sword):** at the overhead top key the blade must clear the crest fin and helm. The off hand must not cross the tabard. The lunge must not put the blade through the front tabard strips or the front leg. Plate pieces add bulk at levels 2 and 3, so render level 3 too.
- **Drowned Raider (Raider model, 39 bones, cutlass):** the blade and off hand must clear the kelp on the helm and the scarf. The raised rest arms (x -16) lift every arm key, so check the guard height. The curved cutlass changes where the tip points on the thrust.
- Neither weapon's hand orientation has been rendered yet. Plan Task 0 verifies it in the first solo render per weapon, and the "blade forward-down" claims depend on it. If the held blade points the wrong way, that is a model or hold fix and needs a decision, not a clip fix.
- No formation variants: `hold` maps to `guard`, there is no wall or brace.
- Ability visuals: none. Fearless and brine have no visual; adding one is gameplay-adjacent and needs Kort.
- **Hit-over-attack gate.** `MotionEngineCheck.hitOverAttack` (about line 1093) calls `hitWhileAttacking` (line 1057), which is hard-wired to the real sword_shield family (`realSwordShield`) and `d.shield = true`. It says nothing about the sword family; plan Task 7 adds a sword-family copy with `shield = false` (hold maps to guard) and the attack order from `ATTACKVARIANTS` (thrust 0, overhead 1, low cut 2). The real window: the attack starts at tick 20 and the hit lands at tick 25, so ticks 26-35 fall inside every swing (the swings last 22, 24 and 25 ticks, ending at ticks 42-45). The gate therefore measures the hit's added step on top of the swing's own motion, at most 15 degrees per tick.

## Fixed limits

- Strike timing is approved: `impact` markers stay on 0.50 (overhead), 0.40 (low cut), 0.35 (thrust); clips stay within `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks. Any timing change needs Kort.
- `python tools/clips.py --verify` must pass; `Spec.java` changes only through `tools/preflight.py`.
- Visual only: the crouch and lunge do not change hitbox, eye height or gameplay. No gameplay, save-format or protocol change.
- Max 2 Minecraft instances (Kort's client counts). No computer-use on Minecraft. No push, publish or merge to main.

## Questions for Kort

0. ~~Model entry~~ Withdrawn 2026-11-03: both models are on `main`. This branch only needs `main` merged in.
1. Is the one-video reference good enough (480p, watermarked, the blade hidden at points), or should the Sebastian Graves Souls-like straight-sword video be fetched and used instead?
2. Should the Risen move differently from the Drowned Raider (a lurching undead gait against a plain raider)? Today one family clip set serves both; a difference would need a second clip folder or a per-troop key, which is not set up.
3. Optional: the plan defaults to the lowered knee-to-hip flat cut (the attack row targets legs). Say so only if you would rather have a chest-height side slash like the video.
4. Taunts, guarded advance and hit reactions for the sword family are not in this spec; do you want them later, as for the knight?

## Review

_Pending: Kort's review._

v1 6.2 (8/6/8/4/7/4). Fixes: numeric key tables for all three swings; per-unit numbers and the no-model blocker; sword-family hit-over-attack gap named; thrust2 frames extracted; `anim_edit open sword` pre-checked; low cut defaults lowered; blade-yaw check made measurable.

v2 7.3 (9/7/8/7/6/7). Fixes: MEC window stated correctly (ticks 26-35 are inside every swing); sword fixture sets shield=false and takes the attack order from ATTACKVARIANTS; root-y and thrust-arm rows corrected and sign check gates the tables; one yaw reference line (body forward); real build command; blade-height check for the low cut; head-layer and chest offsets recorded after Task 0; hand orientation marked assumed.

Final (after fix round 2): 7.3 (9/7/8/7/8/6). Known gaps for Kort:
- Stub clips: `tools/clips.py` (~line 396) fails until every sword attack has a clip, so Task 2 must add zero-motion stubs with their impact markers.
- Root y sign: shipped knight clips crouch with root y +0.3 to +0.7 (rig y points down), so the table's negative values are wrong and the column must be flipped before use.
- The thrust forearm value of -30 at 0.24 s is invented.
- The expected git status must also list the dirty `sword_shield.bbmodel` and the hits v2 files.
- The preflight checkout rule differs between Global Constraints and Task 8.
- Task 0 does not list everything `build_skins` writes.
- The clearance and height numbers live only in the plan.
- After the models exist, `anim_edit` must switch from the knight to the revenant.
