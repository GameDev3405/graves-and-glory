# Ravager Family Animation (tc_brute) Design

Date: 2026-11-03. Status: draft for Kort. Docs only; nothing built, nothing committed.

## Why

The Kraken-touched Brute is the Tidecallers' beast. Kort wants every troop's animation to follow a good game reference, to read as Souls-like beast combat (a long gathering, a committed lunge, a heavy toss), and, above all, to **face the move**: a creature's weapon is its head, horn and jaws, and they must lead the attack.

First finding: **the ravager has no animation today and cannot get one by adding clips.** It has no troop model, so it is drawn by Minecraft's own ravager renderer and `TroopMotion` never runs for it. Task 0 of the plan (a model and a four-legged gait) has to exist before any clip can be seen in game. That work needs Kort or the coordinator.

## Goals

1. Two attack clips, `ravager_charge` (body, blunt, impact tick 10) and `ravager_bite` (head, stab, impact tick 8), with the horn or jaws stated at every key.
2. A troop model for `tc_brute` on the hound rig so the engine can play them.
3. Keep the fixed attack rows (zone, kind, impact tick, weight) and the 40-tick clip cap.

## Units

| Troop | Entity / family | Stats (Spec.TROOPS) | Loadout | Ability |
|---|---|---|---|---|
| `tc_brute` Kraken-touched Brute, Tidecallers, epic, group cavalry, tags beast,cavalry,large | `ravager` / family `ravager` | health 70, damage 9, range 3.0, speed 0.28, cooldown 34 ticks (1.7 s), cost 75, slotWeight 2 | weapon, helmet, chest `minecraft:air`; shield none; pennant false; texture `minecraft:textures/entity/illager/ravager.png` | `tentacle_sweep` (L2: on-hit slam, 85% to others within 3, once per 3 s); L3 `thickhide` stance (shots x0.75) |

It is the only troop of the family. A cooldown of 34 ticks with clips of 26 and 20 ticks leaves the clip finished before the next swing starts.

## Inventory today

- **Rig:** `tc_brute` (entity `ravager`) maps to rig `hound` in `TroopModels.RIGS` and `tools/build_skins.py RIGS`. `art/rigs/hound.json` is the wolf layout (bones `root, body, chest, head, jaw, front_left_leg, front_right_leg, tail, back_left_leg, back_right_leg`). Its note says the ravager gets "its own boxes", but none exist.
- **Troop model:** none. `art/troops/` and `models/troop/index.json` list archer, gl_marshal, knight, longbow, mage, sentinel only. `TroopModels` says a troop whose House has no model "draws through its vanilla renderer"; `TroopMotion.pose` returns early when there is no family (`row(m)` null / no `TroopModel`). So no clip, procedural or authored, is played for the ravager.
- **What the player sees:** Minecraft's own `RavagerEntity` (`GravesAndGlory.ArmyRavager`, SwimGoal only, so it never roars, stuns or charges by itself). `Abilities.animate` (read) sends entity status 4 to a `RavagerEntity` or `IronGolemEntity` mob on each swing and counts it in `BattleEffects.attackStatuses`; it exists because a troop has no vanilla attack goal. What the vanilla renderer does on status 4 I did not read. Once the Brute has our model, status 4 drives nothing visible (our swing comes from `MotionEvents.sendAttack`), so it can stay as a harmless counter.
- **Clip folders:** there is no `clips/hound/` at all (only `clips/biped/{common,bow,caster,sword_shield}`). `art/clips/` has only biped bbmodels. So for the ravager every stance, attack, `_wall`, hit, block and death is **missing**; there are no scenes or carries.
- **Engine gait is biped-named.** `MotionBody` and `MotionFeet` drive `right_leg/left_leg/right_shin/left_shin`; they never touch `front_left_leg` etc. I found no quadruped gait path. A hound-rig troop on the engine would stand with frozen legs. Task 0 must settle this (needs Kort/coordinator; it is Java).
- **Server side already works:** `Tactics.swing` picks a variant from `variants("ravager")` (2 rows) and calls `MotionEvents.sendAttack`, so once a clip exists under `clips/hound/ravager/` the index lines up (`TroopMotion.build`).

## Shared work with the hound and bear families

`docs/superpowers/specs/2026-11-03-hound-animation-design.md` and `2026-11-03-bear-animation-design.md` (plans of the same date) need the same three things: a quadruped troop model on the hound rig (the bear plan proposes its own `bear` rig with elbows and paws), a four-legged gait in `MotionBody`/`MotionFeet`, and `tools/anim_edit.py` taking a rig. **Whichever family lands first builds the shared Task 0; the others skip it.** The ravager keeps the hound rig, as the hound plan does, and adds only its own troop json, horn part and bone box overrides. The gait and the anim_edit rig parameter are written once. This plan's Task 0-Java and Task 1 equal the hound plan's Task 0 and the bear plan's Tasks 0 and 1.

## References

See `C:/Users/kortt/Melty/anim-refs/ravager/SOURCES.md`. Honest result: no verified Souls-like or marketplace video of a horned quadruped charge and bite exists in what I could reach.

| Source | Use |
|---|---|
| gim studio "Rhinoceros animation preview" https://www.youtube.com/watch?v=vJAlnB0O3KM (local `gim_rhino.mkv`, frame-checked) | head-down gallop 0:05.5-0:10.5 (head low, horn forward); from 0:11.8 the head lifts with the forelegs planted and the horn skyward, held to 0:14 (the legs keep stepping, so it is a head lift mid-trot, not a rear-up; checked on `sheet_rear.png`, 6 frames per second from 0:11.0); head-low horn sweep 0:14-0:27 |
| Riko Giant Monster Animset https://www.youtube.com/watch?v=19wwArriGXI | rejected: biped |
| Wolf bite refs in `anim-refs/hound/` (Kort's earlier set) | proposed for the bite's jaw snap; not frame-checked by this spec |

Souls-like feel comes from pacing, not from a clip: long anticipation (gather the head), a committed lunge, a heavy follow-through, a slow recovery. Grozthar (UE 5.6, four-legged charge) is the best asset to look at if Kort wants a true reference; unverified.

## Stances

The engine reads `idle, carry, guard, brace, wall, shield, aim, rest` from `clips/hound/ravager/` over `clips/hound/common/` (both empty today). The Brute is melee cavalry with no formation or bow, so only four are keyed; `brace`, `wall`, `shield` and `aim` stay unkeyed (procedural idle). Stances are absolute offsets from the model's rest pose; attack clips add on top. **Chain pitch = body x + chest x + head x; 0 = snout level, horn straight forward; + = down.**

| Stance | root y px | body x | chest x | head x | jaw x | chain pitch (absolute) | Horn / jaws |
|---|---|---|---|---|---|---|---|
| `idle` (loop; breathing keys of +-1 deg on chest, closing to the start) | 0 | 0 | 0 | 0 | 0 | 0 | horn forward, level; jaws shut |
| `carry` (copy of idle) | 0 | 0 | 0 | 0 | 0 | 0 | as idle |
| `guard` (hold: head lowered, ready) | +0.6 | 0 | +2 | +3 | 0 | +5 | horn forward, 5 degrees down |
| `rest` | +0.4 | 0 | +8 | +10 | 0 | +18 | head low, horn forward-down, jaws shut |

Absolute chain pitch at the attack keys (stance + delta): charge wind-up -30 from idle, -25 from guard; impact +10 / +15; toss -38 / -33. Bite wind-up -26 / -21; bite impact -4 / +1.

## Conventions (checked against shipped clips)

- Rotation +x pitches the part's front down (shipped `hit_head` is head x -8 = snout/chin tips back). Forward is -z. So chain pitch = body x + chest x + head x; **positive = snout/horn aimed down, negative = up**.
- Root: y positive is down (crouch), z negative is forward (shipped `sword_shield_overhead` lunges -1.6 at impact).
- Hound rig has no neck: `chest` (forequarters, child of `body`, pivot [0,0,-7]) acts as the neck; `head` pivot [0,-0.5,-6] under `chest`; `jaw` under `head`. `jaw` +x opens the mouth (derived: the jaw's front is at -z, so +x swings it down; verify in the first preview).
- Leg x +: foot swings back; x -: foot swings forward (same derivation, verify).
- The horn is not a bone: it is a part on `head` (to build in Task 0), so it follows the head's pitch.
- All numbers are first guesses on a model that does not exist yet. Re-derive weapon direction from the summed chain after Task 0 and the first preview.

## Moves

Attack clips are additive offsets on the stance; first and last key of every channel is 0.

### ravager_charge (body, blunt, impact 0.50 s = tick 10, length 1.30 s = 26 ticks)

**Provisional until Question 3 (barge vs ram) is answered.**

Video: gim_rhino 0:05.5-0:10.5 (head-low gallop: the approach and impact posture) and the head lifting mid-trot at 0:11.8 (horn skyward) for the wind-up and the toss. The video has no attack, so the gather and the toss are authored from those postures: the wind-up lifts the head over 0.22 s, the drop to the lunge takes 0.28 s, the toss rises over 0.12 s after impact.

**Legs are not keyed** in this clip (root, body, chest, head and jaw only). The four-legged gait from Task 0-Java keeps running underneath and `MotionFeet` planting handles the feet; the gait must not pause for an attack (the biped's legShare handoff is the model to copy). With no leg keyed, nothing fights the gait. **Forelegs:** in `art/rigs/hound.json` `front_left_leg` and `front_right_leg` are children of `chest`, so every chest x key below also tilts the forelegs and would slide the feet. Task 0-Java must cancel it: it adds a counter-rotation of -(chest x) to both front legs, every tick, on top of the gait (the charge then counter-rotates +10, -3, -5, +14, +4 at 0.22, 0.40, 0.50, 0.62, 0.90; the bite -(its chest x) the same way). If Task 0-Java cannot do it, key those front-leg counter values in the clip instead and drop the 'legs are not keyed' line.

| t (s) | root pos (x,y,z px) | body x | chest x | head x | jaw x | chain pitch | Horn / jaws point |
|---|---|---|---|---|---|---|---|
| 0.00 | 0,0,0 | 0 | 0 | 0 | 0 | 0 | horn forward, level |
| 0.22 wind-up | 0,+1.5,+1.0 | -4 | -10 | -16 | +6 | -30 | horn up and forward, 30 degrees above level (head lifted as the rhino at 0:12) |
| 0.40 drop | 0,+1.0,-1.2 | +1 | +3 | +2 | 0 | +6 | horn swinging down to level, still forward |
| **0.50 impact** | 0,+0.6,-2.4 | +2 | +5 | +3 | 0 | **+10** | **horn forward, 10 degrees down, aimed at chest-to-waist height of a 1.8-block troop (recheck the height in the preview once the head pivot is known)** |
| 0.62 follow-through toss | 0,0,-1.6 | -2 | -14 | -22 | +10 | -38 | head flung up, horn rising and forward; the toss carries the target up |
| 0.90 | 0,+0.3,-0.6 | 0 | -4 | -4 | 0 | -8 | horn forward, easing level |
| 1.30 | 0 | 0 | 0 | 0 | 0 | 0 | horn forward, level |

Retiming: the video's gallop to head lift takes about 1.5 s; here it is compressed so the horn arrives on tick 10.

### ravager_bite (head, stab, impact 0.40 s = tick 8, length 1.00 s = 20 ticks)

No horned-beast reference; the mechanic is a snake-like neck strike. Proposed source: `anim-refs/hound/` wolf bite plus the vanilla ravager's mouth (large, hinged lower jaw).

| t (s) | root pos | body | chest | head | jaw | chain pitch | Horn / jaws point |
|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | forward, level, jaw closed |
| 0.20 wind-up (authored, no reference) | 0,+0.4,+0.8 | -2 | -10 | -14 | +38 | -26 | snout up and back, jaws gaping, horn up |
| 0.32 lunge | 0,+0.2,-1.2 | 0 | +2 | +1 | +40 | +3 | snout forward at the target's head, jaws wide, horn forward |
| **0.40 impact** | 0,+0.4,-2.0 | +1 | +2 | -7 | 0 (snap shut) | **-4** | **snout forward and a touch up, at the target's head height, jaws clamping** |
| 0.50 shake | 0,0,-1.6 | +1 | 0 | head y +12 (right), pitch -3 | +4 | -2 | jaws held on the target, snout swinging 12 degrees right |
| 0.58 shake back | 0,0,-1.2 | 0 | 0 | head x -2, y -12 (left) | +4 | -2 | snout swinging 12 degrees left |
| 0.70 release | 0,0,-0.5 | -1 | -6 | -10 | +14 | -17 | snout pulling up and back, jaws opening |
| 1.00 | 0 | 0 | 0 | 0 | 0 | 0 | forward, level |

Sign check for the shake: +y turns the head right (knight convention); verify with the first preview.

### Hits, blocks, death

Not part of the fixed rows. Proposed as a second pass: `hit_head`, `hit_body`, `hit_legs` (small recoils of chest and head, the head x values of shipped `hit_head` x 1.5 on the larger beast, kept under the 15 deg/tick hit-over-attack limit) and a death clip (collapse sideways onto the knees then flop, ends on the ground, rhino 0:42-0:53 as loose reference, not frame-checked). **Deferred in writing: no task in the plan authors them.** Until they exist the engine falls back to its procedural hit and death. Question 4 asks Kort.

## Application to units

- **Proportions.** Hound rig is wolf-sized (about 0.85 blocks tall); the vanilla ravager is 1.95 wide and 2.2 tall. `art/troops/tc_brute.json` must override `bones` (pivots, boxes, scale) to the ravager's: big head and horn, hump-backed body. `TroopModel.rootScale` is set from the rig's root pivot ((24 - root pivot y) / (24 - rig's)); Formula: rootScale = (24 - root pivot y of the ravager rig) / (24 - root pivot y of hound.json); for a model about 2.2 blocks tall on a 0.85-block rig the expected value is about 2.5x (2.2 / 0.85 = 2.6; confirm against the baked model). **All root keys in this spec (px) are provisional and must be re-derived after Task 0-art**; at 2.5x the charge lunge of -2.4 px reads as -6 px. Root offsets in px are scaled by it, so re-check the lunge's -2.4 px after Task 0.
- **Held item:** none (weapon air, no shield, no helmet, no chest). Nothing to orient in a hand; the horn is a head part. There are no hands (`hands` right/left both `jaw`).
- **Clipping risks:** horn through the ground at the impact (chain +10 from idle, +15 from guard) on a low head (check the contact sheet); head through the shoulder hump at the toss (-38); jaw through the chest at the bite wind-up. No cloth, no shield, no cape.
- **Formation variants:** none (`_wall` not needed; it is cavalry).
- **Legs:** neither attack keys a leg bone; the gait and planting run through them (see the charge section).
- **Ability visuals:** `tentacle_sweep` is a server slam with its own effect; no clip needed. `thickhide` is a passive stance, no pose.
- **Needs Kort / coordinator (not done here):** the troop model and horn art (Task 0), the engine's four-legged gait (Java), keeping or dropping vanilla's roar/stun (the Army mob never uses them).

## Fixed limits

- Rows: `ravager_charge` body blunt 10 heavy; `ravager_bite` head stab 8 medium. Cap 40 ticks. Any change needs Kort.
- Clips are generated from `art/clips/hound/ravager.bbmodel` through `tools/anim_edit.py`; no hand-authored JSON; `Spec.java` only from `tools/preflight.py`.
- `MotionEngineCheck` "hit over attack" runs the sword_shield fixture only and never plays a ravager clip. The plan adds a family-specific check. The 26-35 tick window applies: the attack starts at tick 20 and the hit at 25, so ticks 26-35 are attack ticks 6-15, 0.30-0.75 s, which covers both impacts.

## Questions for Kort

1. May the Brute get a real troop model on the hound rig (Task 0), or should it get its own `ravager` rig with a neck and horn bone? (Recommendation: hound rig plus a head part for the horn.)
2. Is a Java change for a four-legged gait in scope?
3. Charge style: head-down ram and upward toss (as written), or a shoulder barge?
4. Should hit and death clips be in this pass?

## Review

v1 6.0 (7/6/7/6/6/4). Fix round 1: anim_edit needs a rig parameter and depends on the art; hound-rig bbmodel command given; Task 0 split into art and Java gait with Expected lines; hound and bear cross-referenced; legs unkeyed in text and tables; bite 0.58 head x -2; charge impact reduced to +10; rhino reference reworded as a head lift; status 4 read; Task 4 given exact commands; hits and death deferred in writing; stances keyed with absolute pitches.
v2 6.5 (7/6/7/6/7/6). Fix round 2: forelegs counter-rotate against chest x (Task 0-Java cancels it, foot-slide Expected added); rhino ref is a head lift mid-trot; hound-rig command marked unverified with fallback; rootScale formula and about 2.5x stated, root keys provisional; Task 4 prints keep[1]/keep[2], seeds(4, 4100), per-attack peaks; charge table provisional until Q3; bite 0.20 labelled authored.

Final (after fix round 2): 6.5 (7/6/7/6/7/6). Known gaps for Kort: the Brute has no troop model, and the hound-rig base is unconfirmed (Q1); there is no quadruped gait (Java, Q2, shared with hound and bear); root px and head height wait on the real rootScale; the charge is provisional on barge vs ram (Q3), and the wind-up, toss and bite are authored without video; hits and death are deferred, so the hit-over-attack check is weak; the bite foreleg counter-rotation is not listed as numbers; jaw and shake signs are unverified on a model; the hound-rig bbmodel command is unverified and the plan falls back to hand-built groups.
