# Hound family (Moot Hound) in the Souls-like style

Kort, 2026-11-03. Branch `work/anim-rework`. References saved at `C:/Users/kortt/Melty/anim-refs/hound/` (frames in `frames/<move>/`, notes in `SOURCES.md`; not committed): "ROG Creatures: Wolf" (youtu.be/eZ6g7BfuyRo, Atlant Games) and "Gray Wolf male animation preview" (youtu.be/9ZgLlGkoNo4, gim studio).

## Why

Kort wants beasts in the same weighty Souls-like feel as the infantry (an Elden Ring wolf coils, then throws itself in one commit and recovers slowly). The hound has no clips, no clip folder and, more basically, **no troop model**, so today nothing animates it (see "Inventory today"). This spec designs the two attacks and the stances on the rig, and lists what has to exist before any of it can be seen in game.

## Goals

- Hound stances (`idle`, `guard`) and two attacks (`hound_bite`, `hound_lunge`), plus minimal hits.
- Jaws and head lead at every key (tables below); no leg through the ground, no head through the chest.
- Each move approved by Kort one at a time on the compare page.
- Be honest about the gaps: the references show a crouch and a leap, not a bite; the bite is designed here.

## Units

One troop uses family `hound` (levels `wm_hound_2` Dire Hound and `wm_hound_3` share the family and rig):

| Troop | Entity / rig | Held | Helmet / chest | Shield | HP / dmg / range | Cooldown | Ability |
|---|---|---|---|---|---|---|---|
| `wm_hound` Moot Hound | wolf, `hound` rig | `minecraft:air` | `minecraft:air` / `minecraft:air` | none | 18 / 4 / 2.2 | 20 ticks | `pounce` (onhit: x1.8 on the first hit after closing 6 blocks without attacking) |

Tags beast, cavalry, small; speed 0.36. The sheet row has no scale field. rootScale is expected to be 1.0 (root pivot y 0 in `art/rigs/hound.json`, as in the biped rig) but is not read from a model because none exists. No cape, strap or armour layer, so no cloth springs and nothing to clip except the rig itself. The tail is a rigid bone (rest rotation x 36), not a spring.

Rig facts (`art/rigs/hound.json`, px, y down, the animal faces -z; pivots in the file are relative to the parent, worked out here to world): hips `body` pivot (0,14,7), box z 0..9, y 11..17; `chest` pivot (0,14,0), box z -6..0, y 10..17; `head` pivot (0,13.5,-6), box z -9..-5, snout to z -12 (nose tip about (0,14.5,-12)); `jaw` pivot (0,15.5,-8), box z -12..-8, y 15.5..16.5; front legs pivot (+-1.5,16,-4), back legs pivot (+-1.5,16,7), 8 px long, soles at y 24; tail pivot (0,12,8). Nose to tail root: 21 px. Right side is -x.

## Inventory today

| Item | Status |
|---|---|
| Troop model `wm_hound` | **missing.** `models/troop/index.json` lists six troops (archer, gl_marshal, knight, longbow, mage, sentinel) and the rigs; `art/troops/` has no `wm_hound.json`; `TroopModels.model("wm_hound")` is null. The wolf entity therefore renders as the vanilla wolf and `TroopMotion.pose` (called from `TroopModel.setAngles`, TroopMotion.java ~line 118) never runs for it. `docs/PARKED.md:81` says wolf and the other Wildmoot entities cannot be spawned by the server yet |
| Rig model `hound` | exists (`models/troop/rig/hound.json`, `art/rigs/hound.json`; TroopModels.RIGS maps wolf, polar_bear and ravager to it) |
| Clip folders `clips/hound/common` and `clips/hound/hound` | missing: only `clips/biped/` exists. `TroopMotion.build(rig, family)` reads `ClipLibrary.folder("hound","common")` then `("hound","hound")`, both empty, so every stance falls back to the procedural layers |
| Stances idle, carry, guard, brace, wall, shield, aim, rest | missing. This plan adds `idle` and `guard`; the rest fall back to idle (`stanceChain`). No brace/wall formation for beasts (Question 4) |
| `hound_bite`, `hound_lunge` | missing. `Tactics.variants("hound")` returns the two rows; `build` marks them index -1 (no clip). `clips.py` requires a clip for each (stab and blunt are in MELEE, clips.py:36), so Task 1 adds zero-motion stubs. `_wall` not needed |
| hit_head/body/legs | missing (nothing in a `hound/common` folder). Task 3 adds a small set |
| block*, death_*, fall, raise | blocks: none (no shield). Deaths: missing, procedural fall; the biped death clips do not apply to this rig. Question 5 |
| Procedural gait | **not for this rig.** `MotionBody` writes only the biped names (`right_leg` etc.); no Java file mentions `front_right_leg`. Hound legs will not swing when it walks, only when a clip keys them. Question 2 |
| Pipeline | `tools/anim_edit.py` is hard-wired to the biped rig (`RIG = 'biped'`, `TROOP` map: sword_shield, bow, common) and `mesh(troop)` calls `build_skins.troop_project`, which fails without `art/troops/wm_hound.json`. `tools/clips.py` already reads any `art/clips/<rig>/<family>.bbmodel` |

New folders after Task 1: `clips/hound/hound/` (attacks and stances, generated from `art/clips/hound/hound.bbmodel`) and `clips/hound/common/` (hits, from `art/clips/hound/common.bbmodel`). Never hand-author the JSON.

## References

Both videos were opened and frames checked on disk (`SOURCES.md`). Findings, plainly:

- ROG Creatures: Wolf, 20.0-21.2 s (frames/hound_bite, 12 frames at 10 fps): a low stalk, front legs bent, head and shoulders lowered, jaw a little open. **There is no bite snap in this span.** It gives the `guard` pose.
- ROG Creatures: Wolf, 26.0-27.2 s (frames/hound_lunge, 12 frames, re-cut to this span): a wolf leaves the ground in a motion-blurred leap, body stretched, forelegs forward (clearest at about 26.3-26.7 s). Wind-up and landing are not readable (several wolves overlap, they are small on screen). It gives the airborne silhouette only.
- Gray Wolf preview, 19-23 s (frames/hound_idle, 40 frames, sheet `s_idle.png`): a clean standing idle (head turns, weight shifts); v1 cited 14-18 s, which showed a run first. It gives `idle`. 43-44 s: the wolf falls onto its side (frames/hound_death, 10 frames); 40-43 s is a howl.
- The bite snap, the lunge wind-up and impact are **designed from the rig**, using the Elden Ring Red Wolf of Radagon description (a lunging bite, a long recovery). The video gap is Question 1.

## Conventions

- Rotations in degrees, ModelPart convention: **+x pitches the front of a bone down** (a head at +20 points the nose 20 degrees below level; a body at +10 dips the chest). Computed from the rig: a point (0,1,-6) on the head moves from y 1.0 to y 3.0 at +20. +y turns the animal toward its RIGHT, -y left, as in `sword_shield_low_cut.json`. Root position: **+y DOWN (a crouch), -z forward** (checked: shipped `guard.json` crouch is +1.2, `MotionBody.java:124` `TY = crouch`; `sword_shield_thrust.json` root z -2.6 at the impact key). Raising the animal is a negative root y; v1 had the sign backwards.
- Pitch accumulates down the chain: `body` carries `chest`, which carries `head` (with `jaw`) and the front legs. The back legs and `root` do not carry `body`. So the head's absolute pitch = body + chest + head, a front leg's = body + chest + leg. Tables give local values and the head's absolute pitch (cum).
- Front feet sink about 0.12 px for each degree of body + chest pitch (worked out from the rig: at 12 degrees the front pivot is about 1.4 px lower). There is no foot-planting layer for the hound (MotionFeet is biped), so the clips counter-rotate the front legs (leg x = -(body + chest) keeps them vertical) and raise `root` (negative y) by half the sink; the back feet then float by that half. Allowed error 1 px, checked in the renders. Sole check (coordinator's measured sinks with the v2 root): front soles sat 1.5 px under the ground in guard, 2.8 at bite impact, 2.56 at lunge 0.20 and 2.32 at lunge 0.75. Root is now -0.8 (guard), -1.1 (bite 0.40 to 0.70 and lunge 0.75) and -1.2 (lunge 0.20). Lifting the root moves the back soles up by the same amount (0.8 to 1.2 px). **Expected: every sole within 1 px of y 24 in guard, bite 0.40/0.55 and lunge 0.20/0.75 (back soles at most 1.2 at lunge 0.20, where the back legs are folded); where the numeric check (`clips.py points`) or the render shows more, move the root toward the larger error until both fit.**
- Clips are additive over the stance; guard is the base for both attacks. First and last keys of every channel are 0.
- Root y cells in the move tables are absolute px with the guard's -0.8 included; the clip channel stores the cell minus -0.8. Negative is up.
- **Tail:** clip channels are added to the rest rotation (`tools/clips.py` header: "added to the rest rotation"; `art/rigs/hound.json` tail rest x 36). Every tail value in these tables is therefore a delta: guard +10 is absolute 46, lunge impact +30 is absolute 66. Idle 0 leaves the tail at its rest 36. The other hound bones have no rest rotation, so their table values are absolute.
- Both legs of a pair take the same value (FL = FR, BL = BR) unless a cell says otherwise.

## Stances (absolute; Kort picks the final look)

| Stance | body x | chest x | head x (cum) | jaw x | FL=FR x | BL=BR x | tail x | root y px | Weapon direction (jaws) |
|---|---|---|---|---|---|---|---|---|---|
| `idle` (GiM 19-23 s) | 0 | 0 | 0 (0) | 0 | 0 | 0 | 0 | 0 | head level, snout forward, jaw shut |
| `guard` (ROG 20.0-21.2 s stalk) | 4 | 4 | -2 (6) | 6 | -8 | 0 | 10 | -0.8 (raised) | snout forward and 6 degrees down toward the enemy's knees, jaw open 6 degrees, chest low, forelegs vertical (cum 0) |

Idle breathes: chest x +-1.5 and head y +-6 over a 3.2 s loop; tail y +-6. Guard loops 2.4 s with chest x +-1.5. Neither contains foot motion.

## Moves

Each attack is additive over `guard`. Cells show the delta, with the guard-relative absolute in brackets where it helps. Keys are approximate; check in `pose_preview.py`.

### 1. `hound_bite` (zone legs, stab; impact 0.40 s = tick 8; clip 1.2 s = 24 ticks)

Video: only the stalk at ROG 20.0-21.2 s (frames/hound_bite) supports the start. The snap is designed: cock the head up and back with the jaw wide, throw the head down at shin height with the jaw shutting at the impact key, tug, release. Retiming: the video offers no strike, so the 0.18 s snap (0.22 to 0.40) is chosen for the light 8-tick row.

| t (s) | body x | chest x | head x (cum) / y | jaw x | FL=FR x | BL=BR x | tail x | root y/z px | Weapon direction (jaws lead) |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 (6) / 0 | 0 (6) | 0 (-8) | 0 | 0 | -0.8/0 | guard: snout 6 degrees down, jaw 6 open |
| 0.22 | -3 (1) | -4 (0) | -14 (-15) / 0 | +34 (40) | +7 (-1) | 0 | +6 | -0.8/+1.2 | wind-up: head cocked back, snout 15 degrees above level, cocked at thigh height, jaw open 40 degrees ready to drive down |
| 0.40 | +6 (10) | +6 (10) | +8 (26) / 0 | -6 (0) | -12 (-20) | +10 | +16 | -1.1/-3.0 | IMPACT: head and jaw line point forward and 26 degrees down at the target's legs, nose tip about 3.7 px above the ground (shin height; computed from the rig pivots), jaw shut |
| 0.55 | +5 (9) | +5 (9) | +8 (24) / +18 | -6 (0) | -10 (-18) | +8 | +14 | -1.1/-3.5 | tug: snout still forward-down 24 degrees at the leg, head yawed 18 degrees to the right, jaws clamped |
| 0.70 | +5 (9) | +5 (9) | +8 (24) / -18 | -6 (0) | -10 (-18) | +8 | +14 | -1.1/-3.0 | tug back: head yawed 18 degrees to the left (a 36 degree swing in 0.15 s, about 240 deg/s), snout forward-down, jaws clamped |
| 0.85 | +2 (6) | +2 (6) | 0 (10) / 0 | +14 (20) | -4 (-12) | +3 | +8 | -0.8/-1.5 | release: jaws open 20 degrees, snout rising to 10 degrees down, still forward |
| 1.20 | 0 | 0 | 0 (6) / 0 | 0 (6) | 0 | 0 | 0 | -0.8/0 | guard |

Clearance: at 0.40 the nose tip is 3.7 px above the ground, so the head never reaches the floor; chest bottom (y 17 + 1.6) stays above ground; the head and chest boxes overlap only at the neck pivot (as in the rest pose).

### 2. `hound_lunge` (zone body, blunt; impact 0.40 s = tick 8; clip 1.3 s = 26 ticks)

Video: the leap at ROG 26.3-26.7 s (frames/hound_lunge), airborne with the body stretched and forelegs forward. Not readable: the wind-up and landing, which are designed here: a coil and drop, a spring, the chest and head arriving first, a heavy landing. Retiming: spring from 0.20 to 0.40 s (the video's blurred leap lasts about 0.4 s). "Head and chest lead the body": at the impact key the chest is at belt height and ahead of the hips.

| t (s) | body x | chest x | head x (cum) | jaw x | FL=FR x | BL=BR x | tail x | root y/z px | Weapon direction (head and chest lead) |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 (4) | 0 (4) | 0 (6) | 0 (6) | 0 (-8) | 0 | 0 | -0.8/0 | guard |
| 0.20 | +4 (8) | 0 (4) | -8 (2) | 0 (6) | -4 (-12) | -20 | +20 | -1.2/+2.0 | coil: chest low, head low and forward, snout level (2 degrees down) aimed at the target's belt line, jaws shut, back legs folded under the belly |
| 0.32 | -10 (-6) | -4 (0) | +6 (-2) | +8 (14) | -27 (-35) | +35 | +30 | -3.0/-3.0 | spring: chest rising, snout 2 degrees above level toward the target's chest, jaws opening, forepaws swinging forward |
| 0.40 | -12 (-8) | -6 (-2) | +8 (-4) | +14 (20) | -37 (-45) | +40 | +30 | -6.0/-6.0 | IMPACT: airborne, chest pivot about 17 px above the ground, nose about 18 px, at the target's belt height, head forward with the snout 4 degrees above level, jaws open 20 degrees, forepaws forward and up (cum -55) |
| 0.55 | -8 (-4) | -4 (0) | +6 (0) | +4 (10) | -22 (-30) | +30 | +24 | -6.5/-8.0 | carried through: head forward, snout level, forepaws forward |
| 0.75 | +2 (6) | +2 (6) | +2 (12) | 0 (6) | -4 (-12) | +10 | +10 | -1.1/-6.0 | landing: chest dips, snout 12 degrees down, forelegs reaching down (vertical) |
| 1.00 | +1 (5) | 0 (4) | 0 (7) | 0 (6) | -1 (-9) | +2 | +3 | -0.8/-3.0 | settling |
| 1.30 | 0 (4) | 0 (4) | 0 (6) | 0 (6) | 0 (-8) | 0 | 0 | -0.8/0 | guard |

Bracket values are the absolute local x (head: the cumulative pitch body + chest + head). Front legs vertical means leg local = -(body + chest); the spring and impact rows deliberately swing the forelegs forward instead (cum -41 at 0.32, -55 at 0.40).

Height check at impact (computed from the rig; root y negative = raised): chest pivot y = 14 - 7 sin 8 = 13.0. With root up 4 the chest pivot is 9.0 (15.0 px above the soles at y 24) and the nose about 8.1 (15.9 px). Target height: a 32 px biped has its chest at about 20-24 px and its belt at 14-18 px; a hound at 15.9 px reaches the belt, not the chest. Decision: the target is the **belt/lower torso, 14-18 px**, and the root is raised to -6 (from -4) so the nose is about 17.9 px and the chest pivot 17.0 px above the soles, inside that band. A chest-high leap would need root about -9 (clearly airborne); that is Kort's call. Check in the render.

## Application to units

- Moot Hound: no armour or held item, so clearance is the rig itself (head to chest at the bite, tail to back legs on the lunge). Ground contact is by the numbers above (no foot layer): the front feet sink up to 1 px, the back feet float up to 1 px in guard and the bite; during the lunge the whole animal leaves the ground on purpose.
- The `pounce` ability (x1.8 after a 6-block approach) has no visual of its own; the lunge is its natural look. A distinct pounce animation would be a change of the attack row, needs Kort.
- Dire Hound and level 3 reuse the same clips.
- Nothing here needs a gameplay, save or network change. The model, the engine gait and the pipeline gaps do need Kort (Questions 2-3).

## Decisions stated

- Carry/walk: no `carry` clip is authored now; carry falls back to idle and a walking hound has stiff legs (the engine has no quadruped gait). Default pending Question 2; a walk-cycle loop is a separate later task.
- Deaths: none authored; the procedural fall stays (the biped death clips do not fit this rig). Default pending Question 5; left for a later pass.
- Brace, wall, shield, aim, rest: no clips, fall back to idle.

## Fixed limits

- Impact ticks fixed by `Spec.ATTACKVARIANTS`: bite 8, lunge 8; zones legs and body; attacks at most 40 ticks. Changes need Kort.
- `python tools/clips.py --verify` needs a clip for every melee `ATTACKVARIANTS` id in the family bbmodel; Spec.java only through `tools/preflight.py`; afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- `MotionEngineCheck.hitOverAttack` (`test-tools/standalone/MotionEngineCheck.java` ~line 1093, driver `hitWhileAttacking` ~line 1057) runs the `sword_shield` fixture only and never plays hound clips. Timing: the attack starts at tick 20, a body hit lands at tick 25, so the window ticks 26-35 is attack ticks 6-15, inside both hound attacks (impact at tick 8). A family copy is in the plan (Tasks 4 and 5); the 15 deg/tick rule applies. There is no cloth spring on the hound.
- At most 2 Minecraft instances; no computer-use on Minecraft; no push or merge.

## Questions for Kort

1. The references show a crouch and a leap but no bite: accept the designed bite, or supply a reference (real wolf or dog footage, an Elden Ring wolf clip)?
2. Build the hound troop model (`art/troops/wm_hound.json`, `index.json`, renderer)? Without it nothing here can be seen in game. Also: the engine has no quadruped gait, so a walking hound has stiff legs. Author walk-cycle clips (a loop clip on `carry`) or allow an engine change?
3. Allow `tools/anim_edit.py` to take a rig argument (it is biped-only)? Tool change, no game change.
4. Brace or wall poses for hounds (beasts do not form up)? Default: no.
5. Hit and death clips for the hound (the biped ones do not apply): author a small set (hits in Task 3, death left for a later pass)?

## Review

v1 6.5 (7/4/8/7/8/5). Fix round 1: root y sign corrected (+y down: lunge impact -6 raised from the wrong +4, bite +0.3, guard +0.5 a crouch), height check recomputed with a stated belt-height target, frame folders re-cut to their cited spans, tug raised to 18 degrees, Task 0 default and runnable tasks, anim_edit.py and harness-safety steps and HoundHitOverCheck specifics added in the plan, carry and death decisions stated.

v2 7.0 (8/7/7/6/8/6). Fix round 2: root set to -0.8/-1.1/-1.2 for the soles within 1 px, bite 0.22 reworded to thigh height, tail stated as a delta over rest 36; plan: belt-height focus, 19-23 s idle, family check loads clips like realSwordShield with a hard-coded attack order, exact solo_hound run.sh line, full commit commands, one hit variant per zone with mid/return keys, pose_preview.py rig gap named; stale s_death.png deleted.

Final (after fix round 2): 7.5 (8/7/8/7/8/7). Known gaps for Kort: there is no wm_hound troop model and anim_edit needs a rig change (Q2, Q3); the bite and the lunge wind-up have no video, so the designed versions need a yes or real footage; the bite impact and tug put the front soles 1.1-1.4 px under the ground (raise root y to about -1.5); back soles float 1.7 px at lunge 0.20, over the stated 1.2 limit; there is no quadruped gait, so carry falls back to idle; death is the procedural fall and there is one hit per zone; HoundHitOverCheck is planned but not run; pose_preview has no hound rig; pounce has no visual of its own.
