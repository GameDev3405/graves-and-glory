# Hound family animation implementation plan

> For agents: use superpowers:subagent-driven-development or superpowers:executing-plans, task by task. Steps use `- [ ]`.

**Goal:** Hound stances, hits and two Souls-like attacks for the Moot Hound (`wm_hound`).
**Spec:** `docs/superpowers/specs/2026-11-03-hound-animation-design.md` (key tables are the source of truth; the weapon-direction column must hold at every key; +x pitches the front of a bone down; +y turns right; root +y DOWN (crouch), negative y raises, -z forward).
**Tech:** clips live in `art/clips/hound/hound.bbmodel` (family) and `art/clips/hound/common.bbmodel` (hits), edited with `anim_edit.py` once Task 0 allows it; the JSON under `src/main/resources/assets/gravesandglory/clips/hound/{hound,common}/` is generated, never hand-authored. Compare page on localhost:8771.

## Global Constraints

- Branch `work/anim-rework`. No push, merge or publish. Do not edit Spec.java or `sheets/` except the build-time check. Java, tools and art changes only where a task names them and Kort has approved them (Task 0).
- Fixed impact ticks: bite 8, lunge 8; clips at most 40 ticks. Changes need Kort.
- Action clips are additive over `guard`; first and last keys of every channel are 0.
- After `clips.py`, `--verify`, `anim_edit.py apply` or gradle: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Real loadout: wolf entity, `hound` rig, rootScale expected 1.0 (read it from the model once it exists), no held item, no armour, no shield, no cape.
- Scratch tools (pose_preview.py, swing_ends.py, mec.sh, build_compare.py, try_hits.sh, hit_keys.py) live in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/`. If missing, recreate them from the knight plan (`2026-11-02-knight-swings.md`). `pose_preview.py` and `swing_ends.py` are biped-aware: check they accept the `hound` rig first (Task 1).
- At most 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft. Kort approves each move on the compare page before the next.
- Expected git status throughout includes the already-modified `sheets/preflight.json` and `src/main/java/gg/gravesandglory/Spec.java` (restore them before each commit as above).
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

Jaws and head direction per key (spec tables); front and back feet within 1 px of the ground outside the lunge; hit-over-attack numbers; the model and pipeline prerequisites (Task 0); the lunge's impact height at the target's belt (14-18 px); every sole within 1 px of y 24.

## Hit-over-attack, family check

`MotionEngineCheck.hitOverAttack` (`test-tools/standalone/MotionEngineCheck.java` ~line 1093) calls `hitWhileAttacking` (~line 1057), which runs the `sword_shield` fixture only (`realSwordShield`, `d.shield = true`, stance `hold`); it never plays hound clips, so the existing gate proves nothing for this family. Add `test-tools/standalone/HoundHitOverCheck.java` (package `gg.gravesandglory`) with these pieces: **stance** `guard` (the base the attacks are additive over; `d.stance = "guard"`, `d.shield = false`); **family constructor** `realHound(Map<String,Clip>)`, a copy of `realSwordShield` (MotionEngineCheck.java:463). `mec.sh` cannot compile `ClipLibrary` or `Tactics` (they import Minecraft/Fabric), so it loads the JSON as `realSwordShield` does, with `Files.list` over `src/main/resources/assets/gravesandglory/clips/hound/common` and `.../hound/hound` and `Clip.parse`; it builds the stances map from idle/guard and the attacks from the hard-coded order `[hound_bite, hound_lunge]` (index 0 bite, 1 lunge; this matches `Tactics.variants("hound")` but is not read from it), hits by the `hit_(head|body|legs)(_n)?` regex, no deaths, then calls the `MotionEngine.Family` record (`MotionEngine.java:93`); **Drive analogue** `Drive(MotionMath.Seed)` as in `MotionEngineCheck` (line 143) with `attack` set to the attack index and `hit` to the body hit, no `shield`, no `aim`; **check helper** `check(boolean ok, String message)` (line 29) wrapped around a copy of `hitWhileAttacking`'s loop, run for both moves. Timing as the original: the attack starts at tick 20, a body hit lands at tick 25, so ticks 26-35 are attack ticks 6-15. That window holds the bite's strike (impact at attack tick 8) and the lunge's spring and impact. Run: `EXTRA=test-tools/standalone/HoundHitOverCheck.java MAIN=HoundHitOverCheck bash <scratchpad>/mec.sh`. Rule: step with the hit minus step without at most 15 deg/tick (steps sum all bones' channel changes, so the tail and four legs count). Expected output: no `FAIL` line, and `hit over attack hound bite: max <x> deg/tick` and `hit over attack hound lunge: max <x> deg/tick` each with x under 15.0 (the lines are printed by the new check; the existing sword_shield lines are unchanged). Add the check only when the move's real clip exists, never for stubs.

---

## Task 0: Prerequisites (blocked on Kort, spec Questions 2 and 3)

**Default: blocked until Kort answers.** Nothing below can be rendered or opened without these. Runnable now without Kort: reviewing the spec key tables, recalculating geometry with `pose_preview.py` if it accepts the hound rig, and drafting `HoundHitOverCheck.java` (not run). Every other task (1 to 6) is blocked, since the pipeline cannot make a hound working file.

- [ ] A hound troop model: `art/troops/wm_hound.json` (+ skin art), `models/troop/index.json` entry, built with `tools/build_skins.py`; the wolf entity must render through it. Expected: `TroopModels.model("wm_hound")` is non-null and `TroopMotion.pose` runs for a wolf (a debug log or the grade harness shows the bones moving).
- [ ] `tools/anim_edit.py` takes a rig (see Task 1). Expected: `python tools/anim_edit.py open hound --troop wm_hound` writes a work file with the wolf mesh and exits 0.
- [ ] Kort's answer on walk legs (Question 2): author a `carry` walk-cycle loop, or approve an engine quadruped gait. Without one the walking hound has stiff legs; the attacks are unaffected. Until answered: no carry clip.

## Task 1: Scaffold with stub attacks

`clips.py` fails if the family bbmodel lacks a clip for any melee `ATTACKVARIANTS` id (stab and blunt are in MELEE, clips.py:36), so stubs go in first.

**Files:**
- Create `art/clips/hound/hound.bbmodel`.
- Modify `tools/anim_edit.py`: **needs coordinator** (a tool change outside the animation scope). `RIG = 'biped'`, `SRC = art/clips/biped` and the `TROOP` map (`sword_shield: knight, bow: archer, common: knight`) are hard-coded. Required: take the rig from the family (or an argument) and add `'hound': 'wm_hound'` and a hound `common` entry to `TROOP`.
- Build the bbmodel from the hound rig: start from the committed biped working file, not the dirty worktree copy (`git show HEAD:art/clips/biped/sword_shield.bbmodel > <scratch>/base.bbmodel`), replace its bones and mesh with the hound rig from `art/rigs/hound.json` (bone names root, body, chest, head, jaw, front_left_leg, front_right_leg, back_left_leg, back_right_leg, tail, with the pivots in the spec) and drop all its animations; then place it as `art/clips/hound/hound.bbmodel`. `anim_edit.py open hound --troop wm_hound` should do this itself once the tool change above exists; if the tool can only open an existing file, use the manual route.

- [ ] Blocked on the `anim_edit.py` change (coordinator). `python tools/anim_edit.py open hound --troop wm_hound`; in the work file create two zero-motion stub clips `hound_bite` (1.2 s, impact marker 0.40 s) and `hound_lunge` (1.3 s, marker 0.40 s), with the marker name and loop settings of `sword_shield_thrust.json` (marker `impact`, loop false). Each has only 0-valued first and last keys on `root`.
- [ ] `python tools/anim_edit.py apply`. Expected: exit 0, verify passes (impact on ticks 8 and 8, attacks at most 40 ticks), `clips/hound/hound/` holds `hound_bite.json` and `hound_lunge.json`.
- [ ] `pose_preview.py` is biped-only: it needs a `--rig hound` option (coordinator or scratch-tool work, not part of this plan). Until then the alternative check is numeric: `tools/clips.py` `points(rig, pose)` (line 316, forward kinematics over every pivot and box corner on `art/rigs/hound.json`), fed the key poses from the spec tables, for sole y, nose y and head-through-chest overlaps. Expected: the rest pose gives soles at y 24 and the nose tip at z -12. Every later pose-preview step uses whichever of the two exists.
- [ ] Harness safety: feed the hound rig through `MotionBody`, `MotionFeet.ground` and `TroopMotion.limits` in a scratch harness (copy the `mec.sh` driver, rig `hound`, `idle` stance, 200 ticks walking and one attack). These are biped-named code paths. Expected: no exception, `limits` skips missing bones without a write, `MotionBody` and `MotionFeet` write no hound bone (every hound bone is 0 or the clip value; a nonzero value outside a keyed clip is a failure, stop and report). If a bone is written, the clips cannot be trusted; report to the coordinator.
- [ ] Restore generated files: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. Expected: `git status` shows only hound files, docs and the pre-existing modifications.
- [ ] `git add art/clips/hound/hound.bbmodel tools/anim_edit.py src/main/resources/assets/gravesandglory/clips/hound/` then commit:

```
git commit -m "Scaffold the hound clip family with stub attacks" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Task 2: Stances (idle, guard)

- [ ] In the work file add `idle` (3.2 s, loop: chest x +-1.5, head y +-6, tail y +-6) and `guard` (2.4 s, loop: chest x +-1.5) from the spec Stances table (absolute rotations, catmullrom). Expected: guard at t=0 has body x 4, chest x 4, head x -2, jaw x 6, front legs x -8, tail x +10 (a delta over the tail's rest 36, absolute 46), root y -0.8 (raised); idle all 0; first key equals last on every loop.
- [ ] `anim_edit.py apply`; restore generated files. Expected: verify passes, loops close (difference under 0.01).
- [ ] `pose_preview.py` guard, front, side and 3/4. Expected: snout 6 degrees below level pointing forward, jaw slightly open, chest low, forelegs vertical (cum 0), front soles within 1 px of y 24 (every sole within 1 px of y 24). Compare with frames/hound_bite (ROG 20.0-21.2 s): the stalk crouch reads similarly.
- [ ] Compare idle with frames/hound_idle (GiM 19-23 s). Expected: head level, relaxed.
- [ ] Rest, aim, brace, wall, shield, carry: no clips; stance chain falls back to idle (expected: no missing-clip warning in the sim). 
- [ ] Commit:

```
git add art/clips/hound/hound.bbmodel src/main/resources/assets/gravesandglory/clips/hound/hound/ && git commit -m "Pose the hound idle and guard" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- art/clips/hound/hound.bbmodel src/main/resources/assets/gravesandglory/clips/hound/hound/
```

## Task 3: Hit reactions (hits in `art/clips/hound/common.bbmodel`)

Needed by the family hit-over-attack check and so a struck hound reacts (spec Question 5). Scale: about 0.4 of the knight's hit size (spec "Fixed limits").

- [ ] Create `common.bbmodel` for the hound rig and add one variant per zone for now (`hit_body`, `hit_head`, `hit_legs`; no `_2`/`_3`; 0.5 s each, no loop). Keys at 0, 0.10 (peak), 0.25 (mid), 0.5 (return, 0); values are deltas (the tail adds to its rest 36). `hit_body`: peak chest x -5, head x -6, jaw +4, root z +1.0, tail +10, back legs +3; mid chest -2, head -2, root z +0.4, tail +4, back legs +1. `hit_head`: peak head x -10, head y +8, jaw +10, tail +6, back legs 0; mid head x -4, y +3, jaw +4, tail +2. `hit_legs`: peak body x +4, front legs -6, back legs -4 (hind legs buckle), root y +0.6 (down), tail -6; mid body +2, front legs -3, back legs -2, root y +0.3, tail -3. First and last keys 0. Expected: `apply` passes; `clips/hound/common/` holds three files (the hit name pattern is `hit_(head|body|legs)(_n)?`).
- [ ] Render `GRAVES_GRADE_CLIPS=hit_body,hit_head,hit_legs`. Expected: reactions read as flinches in the body direction, no foot slides more than 1 px, snout never below the soles.
- [ ] Commit:

```
git add art/clips/hound/common.bbmodel src/main/resources/assets/gravesandglory/clips/hound/common/ && git commit -m "Add small hound hit reactions" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- art/clips/hound/common.bbmodel src/main/resources/assets/gravesandglory/clips/hound/common/
```

## Task 4: `hound_bite` (impact 8, 24 ticks)

- [ ] Replace the bite stub with spec move 1: keys at 0, 0.22, 0.40, 0.55, 0.70, 0.85, 1.20 s, first and last 0, impact marker at 0.40 s. Expected: at 0.40 body 6, chest 6, head 8 deltas (cum 26), jaw -6 (shut), root y -1.1 z -3.0 (clip channel -0.3 over guard's -0.8); every sole within 1 px of y 24 at 0.40 and 0.55. Tug: head y +18 at 0.55 and -18 at 0.70.
- [ ] `anim_edit.py apply`; restore generated files. Expected: attack 24 ticks, impact on tick 8 and on the sheet tick.
- [ ] `swing_ends.py`/`pose_preview.py` at 0.22, 0.40, 0.55 and 0.70 from front, 3/4 and side. Expected, jaws leading: 0.22 snout 15 degrees above level with the jaw open 40 degrees; 0.40 snout line 26 degrees below level, pointing forward at shin height, tip about 3.7 px above the sole line, jaw shut; 0.55 and 0.70 head yawed 18 degrees right then 18 degrees left, snout still forward-down; no head box through the chest or the floor.
- [ ] Family hit-over-attack check (section above) for the bite with `hit_body`. Expected: maximum under 15.0 deg/tick over attack ticks 6-15.
- [ ] Render: `GRAVES_GRADE_CLIPS=hound_bite,guard bash test-tools/work/t16r12/live/run.sh solo grade wm_hound` (needs the Task 0 model; add to `test-tools/work/t16r12/live/run.sh` if absent: `solo_hound) go solo_hound GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=hound GRAVES_GRADE_TROOP=wm_hound GRAVES_GRADE_CLIPS=${CLIPS:-hound_bite,guard} GRAVES_GRADE_OUT=$B/solo_hound/grade GRAVES_GRADE_PARTS=solo ;;`; run with `CLIPS=hound_lunge,guard` for the lunge), key frames at 8 angles, then `python <scratchpad>/build_compare.py` and open localhost:8771. Expected checklist: the head cocks up and back, snaps down toward the leg height, jaw visibly shuts on the impact frame, shakes once each way, releases and settles to guard; feet within 1 px of the ground; no limb through the body. Stop and wait for Kort's approval.
- [ ] Commit:

```
git add art/clips/hound/hound.bbmodel src/main/resources/assets/gravesandglory/clips/hound/hound/ test-tools/standalone/HoundHitOverCheck.java && git commit -m "Snap the hound bite at shin height from the stalk" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- art/clips/hound/hound.bbmodel src/main/resources/assets/gravesandglory/clips/hound/hound/ test-tools/standalone/HoundHitOverCheck.java
```

## Task 5: `hound_lunge` (impact 8, 26 ticks)

- [ ] Replace the lunge stub with spec move 2: keys at 0, 0.20, 0.32, 0.40, 0.55, 0.75, 1.00, 1.30 s, first and last 0, impact marker at 0.40 s. Expected at 0.40: body -12, chest -6, head +8 (head cum -4), jaw +14, front legs -37, back legs +40, root y -6.0 (raised) z -6.0; 0.55 root y -6.5 z -8.0.
- [ ] `anim_edit.py apply`; restore generated files. Expected: attack 26 ticks, impact on tick 8.
- [ ] Pose preview at 0.20, 0.32, 0.40, 0.55, 0.75 from front, 3/4 and side. Expected, head and chest leading: 0.20 chest low, snout level, back legs folded; 0.40 airborne, chest pivot about 17 px above the soles, nose about 18 px, snout 4 degrees above level pointing forward at belt height (14-18 px), jaws open 20 degrees, forepaws forward and up; the head and chest are ahead of the hips (root z -6); 0.75 forelegs vertical on landing. Compare the silhouette with frames/hound_lunge (ROG 26.3-26.7 s).
- [ ] Family hit-over-attack check for the lunge with `hit_body`. Expected: maximum under 15.0 deg/tick over attack ticks 6-15. If it fails, shrink the hits (not the lunge) as on the knight.
- [ ] Render and compare as in Task 4 with `CLIPS=hound_lunge,guard` (also needs the Task 0 model). Expected checklist: a visible coil, a spring that lifts the chest first, the head and chest arriving at the target's belt height (14-18 px), a heavy landing and recovery; no foot through the ground after landing. Stop and wait for Kort's approval.
- [ ] Commit:

```
git add art/clips/hound/hound.bbmodel src/main/resources/assets/gravesandglory/clips/hound/hound/ test-tools/standalone/HoundHitOverCheck.java && git commit -m "Throw the hound lunge chest first from the coil" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- art/clips/hound/hound.bbmodel src/main/resources/assets/gravesandglory/clips/hound/hound/ test-tools/standalone/HoundHitOverCheck.java
```

## Task 6: Land it

- [ ] `python tools/clips.py --verify`, then the family hit-over-attack check across both moves, then `python tools/verify.py game --offline` (refuses while a Minecraft client is open). Expected: exit 0, each move under 15.0 deg/tick, no new failure.
- [ ] Check every hound troop (`wm_hound`, and its level troops `wm_hound_2`, `wm_hound_3` use the same family): render solo for each. Expected: the same clips play, feet within 1 px, no clipping.
- [ ] Restore generated files; `git status` shows only hound files, docs and the pre-existing modifications.
- [ ] Update `docs/HANDOFF.md` hound line (done moves, open questions). No push.
- [ ] Commit:

```
git add docs/HANDOFF.md && git commit -m "Land the hound family clips" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- docs/HANDOFF.md
```
