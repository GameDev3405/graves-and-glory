# Caster Animation (staff and palm casting) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the caster family a Souls-like `idle` (also `rest`), a staff-aimed `aim`, a low battle-mage `guard`, and a rebuilt `caster_cast` whose staff points at the target at the release. Kort approves each move in game.

**Architecture:** Clip data only. Stances (`idle`, `aim`, `guard`) are absolute poses; the action clip `caster_cast` is added to the stance as an offset (`MotionStack.action`, `Clip.add`), so its keys are deltas and the first and last keys of every keyed channel are 0. The new idle, guard and aim key the root and upper body only (no leg keys), so they step with the root. Edit keys in `art/clips/biped/caster.bbmodel`, rebuild with `tools/clips.py`, render with the motion-grade harness, compare on the local compare page.

**Tech Stack:** Blockbench clip JSON, `tools/clips.py`, the scratchpad scripts below, `test-tools/work/t16r12/live/run.sh`, compare page at localhost:8771.

**Spec:** `docs/superpowers/specs/2026-11-03-caster-animation-design.md` (the Key tables there are the numbers to build; `phi` and the head-elevation table define "pointing at the target").

## Script paths

S = `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad`

- `S/pose_preview.py` (static pose renders): `python S/pose_preview.py <clip> <out.png> <times...> [--over aim|guard]`
- `S/swing_ends.py` (first and last keys are 0, impact marker): `python S/swing_ends.py <clip>...`
- `S/mec.sh` (runs MotionEngineCheck): `bash S/mec.sh`
- `S/build_compare.py` (compare page at localhost:8771): `python S/build_compare.py`

## Global Constraints

- Strike timing is approved: `caster_cast` impact marker stays at 0.5 s (10 ticks); clip length stays at or under `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks (2.0 s), currently 1.2 s. Any timing change needs Kort first.
- No new `Spec.ATTACKVARIANTS` row, no gameplay, save-format or protocol change without Kort.
- Fix clipping in the clip keys only. Mounts and part sizes stay. The crouch is visual only.
- `python tools/clips.py --verify` must pass. `Spec.java` changes only through `tools/preflight.py`. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Only the mage has a troop model (`TroopMotion.java:143` returns no clip row without one). Hexer, wm_seer and tc_mist render as vanilla and cannot show clips. Rendering and approval use `mage`. Modelling the other three is not in this plan (spec Questions 1).
- Limits on running the game: at most 2 Minecraft instances (Kort's client counts); never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft.
- No push, publish or merge to main. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Ask Kort with AskUserQuestion, short and plain. Each move is approved and committed on its own.
- Weapon direction (docs/ANIMATION-EDITING.md): every key states where the staff head or focus points, and the render is checked from front, 3/4 and side. A clean pose with the staff pointing the wrong way is a defect.

## How "pointing at the target" is measured

All angle tables are provisional until Task 1 confirms the `sun_staff` head end (fallback: subtract 180 from every `right_hand` x).

Staff head elevation `e = 90 - phi`, where `phi` = body.x + arm.x + forearm.x + hand.x (spec, Weapon direction). In the render, the side view shows the true pitch: mark the head and butt ends, measure the staff line against the horizontal. In the front and 3/4 views the staff is foreshortened, so check that the staff line points toward the target side and its screen angle from the target-direction line is within 15 degrees. Pass means all three views agree within 15 degrees of the table's value.

## Review Focus

1. **Staff head aims at the target at the impact key** (0.5 s): head elevation within 15 degrees of level in front, 3/4 and side views, over both `aim` and `guard`. Also the wind-up (head up, within 10 degrees of vertical) and follow-through (head drops at most 5 degrees below its impact value).
2. **Start and end in the stance.** First and last key of every keyed channel is 0 in `caster_cast`; no pop at the blend from `aim` or `guard` and back.
3. **Staff and robe clipping** at the extremes at all 8 angles: staff through the cowl or head at the wind-up, the lower end through the skirt, shin or right hand in idle and guard, free hand through the mantle.
4. **Idle to aim to cast to guard blends** read clearly: no snap, idle stays calm.
5. **Hit over cast** at 15 degrees per tick or less, from the caster-specific check (Task 5), not only the existing gate.

---

### Task 1: Tooling, staff orientation and numbers check (work files, not committed)

**Files:**
- Modify: `test-tools/work/t16r12/live/run.sh` (gitignored): `GRAVES_GRADE_CLIPS` per round (clip under work plus `idle,aim,guard`).
- Modify: `S/pose_preview.py`: `--over aim|guard` samples that stance at time 0 and adds the action offsets.
- Check: `S/swing_ends.py caster_cast`.

- [ ] **Step 1:** Run `python S/swing_ends.py caster_cast` on the current clip. Expected: OK (the keys already end at 0; impact 0.5).
- [ ] **Step 2:** Render the mage in the current pose and the current `caster_cast` impact key (solo grade, 8 angles). Find which end of `sun_staff` is the head. Expected: ledger note "head at +Y (short end) / -Y". If -Y, subtract 180 from every `right_hand` x in the spec tables before building, and tell Kort (Questions 3). Also check the body yaw sign (negative = staff side toward the target).
- [ ] **Step 3:** Build the stance clips from the spec tables in a scratch copy and render `guard`, `aim` and the cast impact in `pose_preview.py`. Expected: the measured head elevation is within 15 degrees of the table's value (idle -45, guard -8, aim 0, cast impact -4 over aim and -12 over guard, wind-up 81 over aim and 89 over guard). If a table angle fails, correct the table's hand x until it passes and record the change in the ledger.

### Task 2: Idle (also `rest`) (video C idle 0:08-0:28, frames/riko_idle)

**Files:** `art/clips/biped/caster.bbmodel` (new animation `idle`, loop); generated `clips/biped/caster/idle.json`.

Build the spec's idle table: closed 4 s loop, staff low at the right hip with the head forward and 45 degrees down as in Riko's idle (phi = 135), free hand open at the thigh, breath on body, head and root only. No leg keys.

- [ ] **Step 1:** Write the animation, first key = last key, `loop` true.
- [ ] **Step 2:** `python tools/clips.py`, then `--verify`. Expected: no errors; only `idle.json` is added; "loops close" passes.
- [ ] **Step 3:** `python S/pose_preview.py idle out.png 0 1.1 2.2 3.2` at 8 angles. Expected: staff head elevation -45 degrees, within 15 (head forward and down, 30 to 60 below horizontal) at every angle, lower end clear of the leg and robe.
- [ ] **Step 4:** Render the mage with `GRAVES_GRADE_CLIPS=idle`; confirm in the render that `rest` (empty order) and idle use it (rest falls back through carry to idle). Check front, 3/4, side. Expected: calm, staff low and diagonal like C, no clipping, no foot sliding (root-only step).
- [ ] **Step 5:** Rebuild the compare page (`python S/build_compare.py`, video C idle beside the game), ask Kort to approve or say what to change.
- [ ] **Step 6:** `bash S/mec.sh`. Expected: no FAIL lines. Commit the bbmodel and `idle.json`: "Hold the caster's staff upright in the idle, video style" with the trailer.

### Task 3: Aim (staff level on the enemy: engine rule, no reference; open palm from A 0:30.5-0:32.5, frames/palm_cast)

**Files:** `art/clips/biped/caster.bbmodel` (animation `aim`); generated `clips/biped/caster/aim.json`.

Build the spec's aim table (authored for aimPitch 0; `MotionStack` adds aimPitch to both arms' RX).

- [ ] **Step 1:** Write the animation, closed 3 s loop. No leg keys.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`. Expected: no errors; only `aim.json` added.
- [ ] **Step 3:** `pose_preview.py aim` at 8 angles. Expected: staff head elevation within 15 degrees of level (spec: 0) in front, 3/4 and side views, pointing at the enemy; no staff through the cowl, the free arm or the body.
- [ ] **Step 4:** Render the mage as a ranged unit with an enemy in reach (stance `ranged_aim`) with and without aimPitch (enemy below, 30 degrees). Expected: the head drops by the aimPitch angle, within 5 degrees, and the staff stays on the enemy.
- [ ] **Step 5:** Compare page (A's palm cast beside the game for the free hand only), ask Kort.
- [ ] **Step 6:** `bash S/mec.sh`, commit: "Aim the caster's staff at the enemy, video style".

### Task 4: Guard (video A 0:15.75-0:19.0, frames/battle_stance)

**Files:** `art/clips/biped/caster.bbmodel` (animation `guard`); generated `clips/biped/caster/guard.json`.

Build the spec's guard table: crouch through the root offset (-2 px), body turned 25 degrees, staff across the body at the hip with the head forward and 8 degrees down, free hand at the chest. Used on hold orders and run or fall_back with an enemy. No leg keys.

- [ ] **Step 1:** Write the animation, closed 3 s loop.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`. Expected: no errors; only `guard.json` added.
- [ ] **Step 3:** `pose_preview.py guard` at 8 angles. Expected: staff head elevation -8 degrees (within 15 of that) forward at every angle; no staff through the shin, skirt or hand.
- [ ] **Step 4:** Render with `GRAVES_GRADE_CLIPS=idle,aim,guard` on a hold order, including the idle-to-guard blend. Expected: the switch is visible and does not snap; no clipping. If `carry` does not use the idle angle in game, add a `carry` clip with the idle staff pose (note in the ledger).
- [ ] **Step 5:** Compare page (frames/battle_stance beside the game), ask Kort. Spec Question 2: if Kort wants an upright guard, reduce the crouch and turn here.
- [ ] **Step 6:** `bash S/mec.sh`, commit: "Crouch the caster into a battle-mage guard, video style".

### Task 5: Cast (gather: video C raise 229-233 s and B 0:12.9-0:14.3; palm: A 0:30.5-0:32.5; staff level at release: engine rule, no reference) and caster hit check

**Files:** `art/clips/biped/caster.bbmodel` (animation `caster_cast` only); generated `caster_cast.json`; a caster hit-over-attack check (see Step 7).

Build the spec's `caster_cast` table (deltas, 1.2 s, impact 0.50 s, over `aim`; also valid over `guard`).

- [ ] **Step 1:** Write the keys at 0, 0.28, 0.50, 0.62, 0.85, 1.20. Keep the impact marker at 0.5. Legs unkeyed.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `python S/swing_ends.py caster_cast`. Expected: no errors, OK, only `caster_cast.json` changed, impact on tick 10.
- [ ] **Step 3:** `python S/pose_preview.py caster_cast out.png 0 0.28 0.5 0.62 0.85 1.2 --over aim`, then again with `--over guard`, at front, 3/4 and side. Expected: at 0.5 the staff head elevation is within 15 degrees of level in all three views, over both stances (spec: -4 over aim, -12 over guard); at 0.28 the head is up (elevation 81 over aim, 89 over guard, within 10 of those) and the tip at least 1 px clear of the cowl at the sides and front (spec Clearance); at 0.62 the head is at most 5 degrees below its 0.5 value.
- [ ] **Step 4:** Render the mage with `GRAVES_GRADE_CLIPS=idle,aim,guard,caster_cast` at 8 angles. Check Review Focus 1-3 and that the `transitions` pop flag reads "no".
- [ ] **Step 5:** Compare page ("Cast v<N>"), videos C and B (gather) and A (palm) beside the game, slowed, key poses clickable. Ask Kort to approve or say what to change. Repeat until approved.
- [ ] **Step 6:** `bash S/mec.sh`. Expected: no FAIL lines. Commit: "Cast the caster's staff at the target, video style".
- [ ] **Step 7 (caster hit over cast).** `MotionEngineCheck.hitOverAttack` (test-tools/standalone/MotionEngineCheck.java, line 1093) uses `hitWhileAttacking`, which builds the family with `realSwordShield(...)`: it is the sword-and-shield fixture only and does not cover the caster. Add a caster-specific check in the standalone harness (a `realCaster` family loaded from `clips/biped/caster` with the common hits; a hold-or-aim drive with an enemy; the cast at tick 20; a `body` hit at tick 25 and none; per-tick summed steps for ticks 0 to 59 over the mage's bones including robe, mantle, cowl and cord springs). Expected: in ticks 26 to 35 the step with the hit minus the step without is at most 15 degrees per tick, for 4 seeds, and the cast keeps playing (one track, overlay weight under 0.5 one tick in). Run it over a `body`, `head` and `legs` hit. If it fails, shrink the cast's body and root keys, not the common hits. The check lives in the harness (test-tools, not gameplay); commit it with: "Check the caster's hit over cast".

### Task 6: Final checks and wrap-up

- [ ] **Step 1:** Run `python tools/clips.py --verify`. Expected: pass (impact on a tick and on the sheet tick, attack at most 40 ticks, death on the ground, loops close).
- [ ] **Step 2:** Render the whole mage family in game: idle, aim, guard, cast, a hit over a cast (hit_body), a death. Expected: no pops, no clipping, staff head direction right at every key.
- [ ] **Step 3:** For hexer, wm_seer and tc_mist, record in the ledger that they have no troop model (vanilla render), so the clips do not apply; do not change them. If Kort asks for models, create a follow-up plan using the spec's Application to units.
- [ ] **Step 4:** `bash S/mec.sh` and `python tools/verify.py game` are run by the coordinator before the merge. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` and confirm `git status` shows only the committed caster changes.
- [ ] **Step 5:** Hand the ledger and the open questions (spec) to the coordinator. No push.
