# Spear and Trident Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Author the spear family: stances (`idle`, `carry`, `guard`, `brace`), the three strikes (`spear_thrust`, `spear_high_thrust`, `spear_low_sweep`) and their `_wall` braced versions, for `wm_tusk`, `tc_tideguard` and `tc_admiral`. Kort approves each move in game, with the point of the weapon always facing the move, no clipping and no blend pops.

**Architecture:** Clip data only, new folder `src/main/resources/assets/gravesandglory/clips/biped/spear/` (overrides `common/` by name; no engine change). Source of truth is a new `art/clips/biped/spear.bbmodel`, created the way `sword_shield.bbmodel` was (via `tools/anim_edit.py open spear`; if `open` cannot create a new family, stop and ask the coordinator: that is a tooling change). An action clip is an offset over the stance: first and last keys of every keyed channel are 0. Step-ins are root offsets only. Render with the motion-grade harness; compare against the footage on the local compare page.

**Spec:** `docs/superpowers/specs/2026-11-03-spear-animation-design.md`

## Global Constraints

- Impact markers fixed: `spear_thrust` 0.35 s (tick 7), `spear_high_thrust` 0.35 s (7), `spear_low_sweep` 0.40 s (8); `_wall` variants the same. Clips at most 40 ticks (2.0 s). Any timing change needs Kort first.
- Weapon direction (docs/ANIMATION-EDITING.md): every key of every attack states where the weapon points; check wind-up, impact, follow-through and recovery from front, 3/4 and side. A clean pose with the point wrong fails.
- `python tools/clips.py` then `python tools/clips.py --verify` must pass. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- No gameplay, save-format or network change. No Java or sheet edits.
- At most 2 Minecraft instances; Kort's client counts. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No computer-use on Minecraft. No eula.txt.
- No push, publish or merge. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Pose checks cover `wm_tusk`, `tc_tideguard`, `tc_admiral`.
- Ask Kort with AskUserQuestion, short and plain. Each move is approved and committed on its own.
- **Blocking preconditions (Task 0):** troop and trident models from `origin/work/troop-models-house` (Tusk, Tideguard, `coral_trident`; Admiral has none), the `anim_edit` tooling change, and the run.sh cases (see Task 0).

## Review Focus

1. **Weapon direction on every key.** Tines forward and level for thrusts (head height for the high thrust, chest for the thrust), shaft flat at shin height for the sweep; verify on all three troops at 8 angles.
2. **Both hands on the shaft.** The left arm is keyed onto the shaft line in every pose; the shaft passes neither through the left forearm, the head nor the rear leg.
3. **Start and end in the stance.** Every keyed channel's first and last key is 0 (scratch `swing_ends.py`), and the grading pop flag reads "no" for chained attacks.
4. **Brace line.** `_wall` clips stay inside the 0.7-block cell: root travel under 1 px, no sweep past the cell, shaft tip not through a neighbour.
5. **Hit over attack.** The existing MotionEngineCheck fixture (attack at tick 20, hit at 25 = attack tick 5, window ticks 26 to 35 = attack ticks 6 to 15) is valid for the spear window, but it plays `sword_shield` only, as do `hitOverWindow` and `wallStrikes` (impacts 7, 10, 8). The spear copies in Task 7 must stay at or under 15 degrees per tick.

---

### Task 0: References and preconditions (blocking)

- [x] **Step 1:** Footage obtained (done 2026-11-03): `thunder-spear.mkv` is the main reference; the others are catalogues. See `C:/Users/kortt/Melty/anim-refs/spear/SOURCES.md`.
- [x] **Step 2:** Frames extracted (10 fps, 480 wide) into `anim-refs/spear/frames/<move>/` and looked at; the spec carries the checked timestamps (thrust 0:10.2-0:11.4, high thrust 0:11.4-0:12.6, low sweep 0:07.5-0:07.7 and 0:08.2-0:08.4, idle 0:00-0:02, carry 0:26-0:28). The braced hold has no footage and stays rule-based.
- [ ] **Step 3:** Confirm the models (spec question 1). They live on `origin/work/troop-models-house`: `art/troops/wm_tusk.json`, `tc_tideguard.json`, `art/weapons/coral_trident*` and `models/troop/weapon/coral_trident*.json`; this branch has none, the Admiral has none. Check that the trident grip puts the tines on the hand's forward axis. Then print `rootScale()` for Tusk and Tideguard at model load and record it (expected 1.0; if the loader derives it from Tideguard's 1.05 leg scale it will read about 1.05, and root numbers then scale by it). For the Admiral, measure his torso and shoulder width and root pivot from his model before using the spec's 1.25 and 10/20 px assumptions. Expected: numbers recorded in the ledger. If the models cannot be had, author clips on the bare rig only and stop before any render step; report.
- [ ] **Step 4 (tooling change, needs the coordinator's approval first):** `python tools/anim_edit.py open spear` fails today: `TROOP` in `tools/anim_edit.py` (line 29: sword_shield, bow, common) has no `spear` entry and there is no `art/clips/biped/spear.bbmodel`. The change is one `TROOP` entry (`'spear': 'wm_tusk'`) plus a copy of `art/clips/biped/sword_shield.bbmodel` to `spear.bbmodel` with its clips emptied. Expected: after approval, `open spear` exits 0 and opens a `spear` family with the rig and no clips; without approval, stop and report. Then write scratch tools (outside the repo) in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/`: `mec.sh`, `swing_ends.py` and `pose_preview.py`; recreate them from the knight plan (`docs/superpowers/plans/2026-11-02-knight-swings.md`) if missing. `swing_ends.py <clip>...` (FAIL for any keyed channel whose first or last key is not within 0.01 of 0, or an `impact` marker not at 0.35, 0.35, 0.4, or any key time not a multiple of 0.05), `pose_preview.py ... --over brace|guard` (stance at time 0 plus the clip offset).
- [ ] **Step 5 (test-tool change, tell the coordinator):** `test-tools/work/t16r12/live/run.sh` has no `solo_tusk`, `solo_tideguard` or `solo_admiral` case (it has solo_knight, solo_sentinel, solo_khits). Add one per troop modelled on `solo_knight`: `GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=spear GRAVES_GRADE_TROOP=<wm_tusk|tc_tideguard|tc_admiral> GRAVES_GRADE_CLIPS=<clip list> GRAVES_GRADE_OUT=$B/solo_<name>/grade GRAVES_GRADE_PARTS=solo`. Expected: `bash run.sh <tag> solo_tusk` runs and writes `$B/solo_tusk/grade`; `solo_admiral` stays blocked until his model exists.

### Task 1: Stances `guard` and `brace`

**Files:** `art/clips/biped/spear.bbmodel` (clips `guard`, `brace`); generated `clips/biped/spear/guard.json`, `brace.json`.

Target poses and weapon direction:
- **`guard`** (loop 2.4 s): low, knees bent, weight forward; spear levelled at the enemy, tines forward at waist to chest height, shaft level, in line with the forearms, left hand on the shaft ahead, rear hand on the shaft at hip height (0.5 m from the butt), lead hand on the shaft 8 to 12 px ahead. Breath and sway of at most 1 to 2 degrees.
- **`brace`** (loop 3 s): upright and tight for the 0.7-block cell; tines forward and slightly down (0 to 10 degrees) at the charging enemy's chest, rear foot back, rear hand on the shaft at hip height (0.4 m from the butt), lead hand 0.5 m ahead at waist height.
- Loop closes (first key equals last).

- [ ] **Step 1:** Write the keys from the spec's Stances table (absolute values, loops close).
- [ ] **Step 2:** `python tools/clips.py`, then `--verify`. Expected: no errors; only the two new JSON files are added.
- [ ] **Step 3:** `pose_preview.py` front, 3/4, side. Check the shaft does not cross the head or leg, and that two troops at 0.7-block spacing do not overlap (brace). Hand-height check: rear hand on the shaft at hip height and lead hand on the shaft at waist to chest height (guard) or waist height (brace), 8 to 12 px apart, both gripping the shaft (no hand off it); tine pitch at the table value (guard 0 to +5, brace -5).
- [ ] **Step 4:** Render each troop: `GRAVES_GRADE_CLIPS=guard,brace bash test-tools/work/t16r12/live/run.sh <tag> solo_tusk` (then `solo_tideguard`, `solo_admiral`). Expected: stance reads clearly at 8 angles; compare page "Stances v1".
- [ ] **Step 5:** Ask Kort. `bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh` (no FAIL lines). Commit: "Add the spear guard and brace stances".

### Task 2: `idle` and `carry`

**Files:** `spear.bbmodel` (clips `idle`, `carry`); `idle.json`, `carry.json`. Values from the spec's Stances table.

Targets:
- **`idle`** (loop 2.4 s): relaxed, right hand on the shaft at the balance point (1.0 m from the butt), left hand free and hanging; tines yaw 0, pitch -45, point clear of the ground. Video 0:00-0:02.
- **`carry`** (loop 0.8 s): right hand 0.8 m from the butt, shaft on the right shoulder; left hand free and swinging +-20 degrees; tines yaw 0, pitch +60, butt behind the hip, never across the neighbour. The video's trailed-point run (0:26-0:28) is not copied.

- [ ] **Step 1:** Write the keys; loops close.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`. Expected: no errors; only `idle.json` and `carry.json` added.
- [ ] **Step 3:** `pose_preview.py idle|carry` front, 3/4, side. Checklist: idle tines pitch about -45 and the tip clear of the ground; carry tines about +60 and the butt behind the hip; the left hand is free in both.
- [ ] **Step 4:** Render each troop with `GRAVES_GRADE_CLIPS=idle,carry,guard`. Expected: idle to guard switch has no pop; the carry loop closes with no foot slide; in a column of two at 0.7-block spacing the carried tines do not touch the man ahead or behind; the Admiral's shoulder-carried shaft clears his helm by 2 px.
- [ ] **Step 5:** Ask Kort; `bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh` (no FAIL lines). Commit: "Add the spear idle and carry".

### Task 3: `spear_thrust` (spec Moves, Thunder Cloud video 0:10.2-0:11.4)

**Files:** `spear.bbmodel` (`spear_thrust`); `spear_thrust.json`. Length 1.2 s, impact 0.35.

Target (weapon direction at every key):
- **Wind-up (0.00 to 0.20):** weight back, rear hand to the ribs, lead hand on the shaft; tines forward and level at target chest height.
- **Impact (0.35):** arms drive out, root forward 2 to 3 px, lean in 6 to 8 degrees; tines lead on the forward line within 2 degrees, shaft level and in line with both forearms.
- **Follow-through (0.45 to 0.55):** held; tines forward at chest height, under 5 degrees of settle.
- **Recovery (0.55 to 1.2):** back to guard; tines forward, shaft level.

- [ ] **Step 1:** Write the keys; times inside 1.2 s, impact 0.35.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `swing_ends.py spear_thrust`. Expected: no errors, OK, only `spear_thrust.json` added.
- [ ] **Step 3:** `pose_preview.py spear_thrust out.png 0 0.2 0.35 0.5 1.2 --over guard`. Check the point direction on every key and the left hand on the shaft.
- [ ] **Step 4:** Render per troop with `GRAVES_GRADE_CLIPS=spear_thrust,guard`; 8 angles at wind-up, impact, follow-through; measure the blade-line yaw at impact (within 2 degrees of the forward line); pop flag "no". Expected: lines above hold.
- [ ] **Step 5:** Compare page "Thrust v<N>"; ask Kort; iterate.
- [ ] **Step 6:** `bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh`; commit: "Thrust the spear from the guard, video style".

### Task 4: `spear_high_thrust` (video 0:11.4-0:12.6)

**Files:** `spear.bbmodel` (`spear_high_thrust`); `spear_high_thrust.json`. Length 1.2 s, impact 0.35. Key table in the spec.

Targets (weapon direction at every key):
- **Wind-up (0 to 0.20):** rear hand at shoulder or cheek, lead hand at chest; tines yaw 0, pitch +4, shaft near head height.
- **Impact (0.35):** arms out at shoulder height, root forward 2 px; tines yaw 0 within 2, pitch +2, at head height (eye line), shaft in line with the forearms.
- **Follow-through (0.45 to 0.55):** held at head height. **Recovery (to 1.2):** lower to guard, tines forward.

- [ ] **Step 1:** Write the keys from the spec table; impact marker 0.35.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `swing_ends.py spear_high_thrust`. Expected: no errors, OK, only `spear_high_thrust.json` added.
- [ ] **Step 3:** `pose_preview.py spear_high_thrust out.png 0 0.2 0.35 0.5 1.2 --over guard`. Checklist: tine height at impact is head height on all three troops (Admiral included); point on the forward line; the shaft is clear of the head and left forearm at 0.20; the shaft is level in side view.
- [ ] **Step 4:** Render per troop with `GRAVES_GRADE_CLIPS=spear_high_thrust,guard`; 8 angles at wind-up, impact, follow-through. Expected: point leads at head height at impact; blade-line yaw within 2 degrees; pop flag "no".
- [ ] **Step 5:** Compare page "High thrust v<N>"; ask Kort; iterate. Then `mec.sh`.

Commit: "High-thrust the spear from the guard, video style".

### Task 5: `spear_low_sweep` (video 0:07.5-0:07.7, 0:08.2-0:08.4; frames f_003, f_004)

**Files:** `spear.bbmodel` (`spear_low_sweep`); `spear_low_sweep.json`. Length 1.3 s, impact 0.40. Key table in the spec.

Targets (tine yaw, pitch; yaw + is right, - is left):
- **Wind-up peak (0.15):** crouch (root down about 3 px), hips and shoulders turned 40 right, hands low at the right hip; tines yaw +120 (back-right), pitch -10.
- **0.25 and 0.35:** tines yaw +60 then +10, pitch -14 then -18. Body turn at most 15 degrees per tick (0 to 40 in three ticks, 40 to -35 in five ticks).
- **Impact (0.40):** shaft from the waist-height hands down to knee height at the tine third (waist to knee, per f_003 and f_004; f_010 is higher and not used); tines yaw -15, pitch -20; the outer third of the shaft crosses the target's legs.
- **Follow-through (0.50 to 0.65):** tines yaw -45 then -60, pitch -15 then -10. **Recovery:** unwind, rise, tines back to guard.

- [ ] **Step 1:** Write the keys from the spec table; impact marker 0.40.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `swing_ends.py spear_low_sweep`. Expected: no errors, OK (every key on a 0.05 tick, body step at most 15 degrees per tick), only `spear_low_sweep.json` added.
- [ ] **Step 3:** `pose_preview.py spear_low_sweep out.png 0 0.15 0.25 0.35 0.4 0.5 0.65 1.3 --over guard`. Checklist: tine yaw at each key matches the table within 10 degrees; the shaft never clips the rear leg or the left forearm at 0.25; both hands on the shaft at every key; impact height waist to knee.
- [ ] **Step 4:** Render per troop with `GRAVES_GRADE_CLIPS=spear_low_sweep,guard`; 8 angles at wind-up, impact, follow-through. Expected: the shaft sweeps flat from back-right across the front to the left, never over the head; the `contact` sheet shows no foot slide (step is root-only); pop flag "no".
- [ ] **Step 5:** Compare page "Low sweep v<N>"; ask Kort; iterate. Then `mec.sh`.

Commit: "Sweep the spear low from the guard, video style".

### Task 6: Braced `_wall` strikes

**Files:** `spear_thrust_wall`, `spear_high_thrust_wall`, `spear_low_sweep_wall` (bbmodel and JSON). Same ticks and impact markers as Tasks 3 to 5 (7, 7, 8). Key tables in the spec.

Targets (tine yaw 0 on every key; pitch from the table):
- **thrust_wall:** wind-up pitch -5; impact pitch 0 at chest height; hold -2; end back to the brace line (-5).
- **high_thrust_wall:** wind-up pitch 0; impact pitch +2 at head height; hold +1; end -5.
- **low_sweep_wall:** wind-up pitch 0 (drawn back along the line); impact a short forward-down chop, pitch -30 at shin height, shaft not swept sideways; follow-through -25; end -5.
- All three: root travel under 1 px, hip turn at most 6 degrees, nothing past the 0.7-block cell, hands on the shaft (slide 1 to 2 px).

- [ ] **Step 1:** Write the three clips; `python tools/clips.py`, `--verify`, `swing_ends.py` on all three. Expected: no errors, all OK, three JSON files added.
- [ ] **Step 2:** `pose_preview.py <id>_wall out.png ... --over brace` for each. Checklist: yaw 0 within 2 degrees at every key; pitch within 5 degrees of the table; root z never beyond -1 px; hip yaw under 6 degrees.
- [ ] **Step 3:** Render two troops side by side at 0.7-block spacing in `brace` with `GRAVES_GRADE_CLIPS=<ids>_wall,brace` (Tusk and Tideguard only; the Admiral has `slotWeight 4`). Expected: no shaft through a neighbour, no overlap, nothing leaves the cell, pop flag "no".
- [ ] **Step 4:** Compare page; Kort approves; `mec.sh`; commit: "Add the braced spear strikes".

### Task 7: Hits and the spear hit-over-attack checks

The existing `MotionEngineCheck` hit checks (`hitOverAttack`, `hitOverWindow`, `wallStrikes`) run only `sword_shield`; `hitWhileAttacking` starts the attack at tick 20 and hits at tick 25 (attack tick 5), so its window (ticks 26 to 35) is attack ticks 6 to 15, inside every spear clip, but no spear clip is ever played.

- [ ] **Step 1:** Write scratch copies (outside the repo, in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/`): `hitWhileAttacking` with a Family built from `common` plus `spear` (attacks `spear_thrust`, `spear_high_thrust`, `spear_low_sweep`, plus the `_wall` clips on a brace stance), common `hit_head/body/legs` landing at attack tick 5 (as the original), 10 and 14, each step at most 15 degrees per tick from the hit to the clip's last tick (24 or 26), four seeds; a copy of `hitOverWindow` (steps rise to one peak and fall in the window) and a copy of `wallStrikes` with the spear impacts 7, 7, 8. Expected: PASS for all three attacks and all three hit tick cases. Editing `MotionEngineCheck.java` itself needs the coordinator's approval (Java edit).
- [ ] **Step 2:** Render `hit_head`, `hit_body`, `hit_legs` (common) over `guard` and `brace` for the three troops. If the shaft drags through the body or Step 1 fails, author spear `hit_*` (and `_2`, `_3`) that keep the tines forward, shrinking amplitude to about 0.4. If they pass, record "inherited, no change" in the ledger.
- [ ] **Step 3:** Commit any new clips: "Add spear hit reactions".

### Task 8: All troops and verify game

- [ ] **Step 1:** Render `solo_tusk`, `solo_tideguard`, `solo_admiral` (one at a time) with `GRAVES_GRADE_CLIPS` set to all spear clips. Check every strike key at 8 angles, and the Admiral's bulk. Expected: no clipping; fix in the clip keys only, re-verify, re-render.
- [ ] **Step 2:** `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background. Expected: PASS. Afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 3:** Commit any fixes, write the ledger, tell the coordinator the new head of `work/anim-rework`.
- [ ] **Step 4:** Ask Kort which comes next: guarded advance, hits, or an Undertow cast pose (needs Kort).
