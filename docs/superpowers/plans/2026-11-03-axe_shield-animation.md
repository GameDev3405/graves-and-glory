# Hearthguard Axe-and-Shield Animation (video style) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the `axe_shield` family (troop `ic_hearthguard`) an authored stance set and its two attacks, `axe_shield_overhead_chop` and `axe_shield_hook_hew`, plus `_wall` variants, in the knight's video style. Kort approves each move in game. No self-clipping, no blend pops, and the axe faces the move on every key.

**Architecture:** Clip data only. New folder `clips/biped/axe_shield/` overrides `clips/biped/common/` by name (`TroopMotion.build`). Stances are copied from `clips/biped/sword_shield/` and the right arm is changed to hold the axe. Attacks are additive offsets over the stance (`MotionStack.action`, `Clip.add`): first and last key of every keyed channel is 0. Sources live in a new `art/clips/biped/axe_shield.bbmodel` (`tools/clips.py` globs `art/clips/biped/*/*.bbmodel`), built with `tools/clips.py`.

**Spec:** `docs/superpowers/specs/2026-11-03-axe_shield-animation-design.md`

**References (local, never committed):** `C:/Users/kortt/Melty/anim-refs/axe_shield/` (`SOURCES.md`, `frames/`). RBStudio https://youtu.be/8jiz-5jf15U, Hammerhead https://youtu.be/KB-SWLjRgzs, MythforgeGames https://www.youtube.com/watch?v=y8Qez-K8VhE, Graves SHIELD https://www.youtube.com/watch?v=MJkHBwryr7Q.

## Global Constraints

- Impact markers are fixed: chop 0.5 s (tick 10), hew 0.4 s (tick 8), also in the `_wall` clips. Clip length at most 40 ticks (2.0 s). Any timing change needs Kort first.
- The weapon faces the move on every key (docs/ANIMATION-EDITING.md "Weapon direction"). A key without a stated weapon direction is a defect; a pose that is clean but points wrong fails review.
- Fix clipping in clip keys only; mounts and part sizes stay. The crouch and lunge are visual only. Step-ins are root offsets only; legs are left to `MotionFeet`.
- No gameplay, save-format or network change. A shield-pull effect for the hook is gameplay and needs Kort.
- `python tools/clips.py --verify` must pass. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Limits: at most 2 Minecraft instances (Kort's counts); never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft.
- No push, publish or merge. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Ask Kort with AskUserQuestion, short and plain. Each move is approved and committed on its own.
- Scratch tools (recreate from `docs/superpowers/plans/2026-11-02-knight-swings.md` Task 1 if missing), in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/`: `pose_preview.py` (`--over guard`), `swing_ends.py`, `mec.sh`, `build_compare.py`, `compare_template.html`.

## Review Focus

1. **Weapon direction on every key** of both attacks, read in the 8-angle render: chop bit edge-first coming down from above and behind the head; hew bit edge leading, haft roughly horizontal, beard turned toward the body at the follow-through.
2. **Start and end in the stance.** Every keyed channel's first and last key is 0 (`swing_ends.py`); the folded-arms idle opens correctly when a clip holds an arm (`TroopMotion.folds`).
3. **Self-clipping on the broad, short body:** axe through helm, crest or horns (wind-up), braided beard (hew pull), slab pauldrons (raised arm), shield (hew cross), lead knee and scale-coat skirt (chop follow-through), at all 8 angles and at tiers I to III.
4. **Formation:** `_wall` clips stay inside the 0.7-block cell (root travel under 1 px, no axe tip out of it).
5. **Hit over attack:** the repo check plays only the sword_shield thrust and its window (ticks 26 to 35) is empty for our 24- and 22-tick clips; Task 1 adds a scratch run with the axe clips and the hit in the recovery (ticks 14 to 24, at most 15 deg/tick). Cloth springs (tabard, skirt) count.

---

### Task 0: Preconditions (no commit)

- [ ] **Step 1:** Run `git status --short`. Expected: the sword_shield `hit_*` files may still be uncommitted; if so, do not copy hits yet (Task 6). The working copy of `art/clips/biped/sword_shield.bbmodel` also has uncommitted edits, so never copy it from disk (Step 2 uses HEAD).
- [ ] **Step 2 (TOOLING CHANGE, needs coordinator approval before it is made):** Known gap: `tools/anim_edit.py` line 29 is `TROOP = {'sword_shield': 'knight', 'bow': 'archer', 'common': 'knight'}`, so `axe_shield` cannot be opened. Ask the coordinator, then: (a) add `'axe_shield': 'ic_hearthguard'` to `TROOP`; (b) `git show HEAD:art/clips/biped/sword_shield.bbmodel > art/clips/biped/axe_shield.bbmodel` (committed rig and animation layout, not the dirty working copy), then in Blockbench delete the sword animations from the copy and keep only the six stance animations Task 2 builds. `tools/clips.py` already globs `art/clips/biped/*/*.bbmodel`. Expected: `python tools/clips.py` runs without error; `git diff --stat` shows only `tools/anim_edit.py` and the new bbmodel. Commit on its own: "Let the clip tools open the axe_shield family". If approval is withheld, STOP.
- [ ] **Step 3:** Confirm the references exist: `ls C:/Users/kortt/Melty/anim-refs/axe_shield/` shows the four videos, `SOURCES.md`, `frames/hook_hew_LA1`, `frames/overhead_chop_HA_a`, `frames/overhead_chop_HA_b`, `frames/myth_chop`. Expected: all present.

### Task 1: Check scripts (work files, not committed)

Scratch dir `S` = `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad`.

- [ ] **Step 1:** `swing_ends.py` accepts the impact markers 0.5 (chop) and 0.4 (hew) for this family (add them to its allowed list). Run on the new clips after each task. Expected: OK.
- [ ] **Step 2:** The repo's `hitOverAttack` (`test-tools/standalone/MotionEngineCheck.java` line 1093) builds its family from `realSwordShield` and plays the sword_shield thrust; its window is ticks 26 to 35, empty for our 24- and 22-tick clips. Do not edit it. Make a scratch copy `S/axe/AxeHitOverAttack.java` (package `gg.gravesandglory`; a copy of `hitOverAttack` and `hitWhileAttacking` with the family built from `common` + `axe_shield`, the attack set to `axe_shield_overhead_chop` then `axe_shield_hook_hew`, the hit landing at tick 13, and the compared window ticks 14 to 24). Run: `EXTRA=S/axe/AxeHitOverAttack.java MAIN=AxeHitOverAttack bash S/mec.sh` (mec.sh compiles the engine classes plus `MotionEngineCheck.java`, adds `$EXTRA`, and runs `$MAIN`). Expected: for each clip a line `worst <= 15.0` (hit minus no-hit step, deg/tick) and no FAIL. Note in the ledger that the window was moved and why.
- [ ] **Step 3:** In `test-tools/work/t16r12/live/run.sh` (gitignored) set `GRAVES_GRADE_CLIPS` per round to the move being worked plus `guard`.
- [ ] **Step 4 (pose_preview and the axe):** `pose_preview.py` does not draw the axe. It draws a 16 px sword line from the right hand along `--blade`, in the hand frame. The axe has the same mount as the swords (`hearth_axe`, tools/paint_house_troops.py line 1174; registry grip [90,45,90], "held like the swords"; haft along weapon +Y, bit on -X), so the haft line is `--blade 0,-1,-1`, the default. Read the printed `R hand [x,y,z]` and `tip [x,y,z]`: haft vector = tip minus hand, model space (x right = shield side, y down, z back; forward = -z). The bit and beard directions are not drawn; check them in the render. Haft check columns below are (fwd, down, x) of that unit vector: fwd = -z, down = + below horizontal, x = + toward the shield side.

### Task 2: Stances (guard, wall, brace, shield, idle, carry), one commit

**Files:** `art/clips/biped/axe_shield.bbmodel` (new); generated JSON in `src/main/resources/assets/gravesandglory/clips/biped/axe_shield/`.

Start: the sword_shield `guard`, `wall`, `brace`, `shield`, `idle`, `carry` (body, shield arm, legs, root unchanged). The right arm changes to hold the axe. The sword's hand roll [180,169,0] would swing the haft across the body, so the right_hand values below were solved so the haft points as the check column says. Absolute stance poses (not offsets):

| Stance | body | right_arm | right_forearm | right_hand | left side (copied) | root | Haft check (fwd, down, x) | Weapon direction |
|---|---|---|---|---|---|---|---|---|
| `guard` (loop 2.4 s) | [14,18,0] | [-16,-19,16] | [-37,0,0] | [162,169,4] | left_arm [-3.4,21.6,-1.6], left_forearm [-52,0,0] | [0,1.2,0] | (.88, .48, .01) | haft forward-down about 29 deg below horizontal, bit forward, beard down |
| `wall` | [5,20,0] | [-11,-25,20] | [-31,0,0] | [158,168,1] | copy wall | copy | (.79, .62, .01) | haft forward-down about 38 deg, bit forward; tip inside the 0.7-block cell |
| `brace` | [8,16,0] | [-13,-21,17] | [-33,0,0] | [159,168,4] | copy brace | copy | (.83, .55, .01) | haft forward-down about 33 deg, bit forward |
| `shield` | [4,18,0] | [-11,-24,20] | [-31,0,0] | [157,168,1] | copy shield | copy | (.79, .62, .02) | axe low at the hip, haft forward-down about 38 deg, bit forward |
| `idle` (loop 4.0 s) | [1,3,0] (the sword's) | [2,-36,26] | [-19,0,0] | [147,166,-9] | copy sword_shield idle (left_arm [-32,0,0]; both arms keyed so `folds` opens the arms; Kort question 3) | copy | (.25, .97, .02) | haft nearly straight down along the thigh, bit forward |
| `carry` (loop 2.0 s) | [3,4,0] (the sword's) | [-48,-1,1] at 0 and 2.0, [-53,-1,2] at 1.0 | [-125,0,0] | [175,181,2] at 0 and 2.0, [182,181,3] at 1.0 | copy carry (left_arm [-32,0,0], left_forearm [-13,0,0]) | copy | (-.50, -.87, .00) | axe resting on the right shoulder: starts from the sword carry (arm -50, forearm -128), haft pointing up and back about 60 deg above horizontal, bit forward away from the beard and helm |

Note: arm and forearm x are collinear, so how the sum is split between them is arbitrary; the sums above are what matter. The values come from a search over arm, forearm x and hand against the haft target, computed with the rig pivots and the hand frame (haft = [0,-1/sqrt2,-1/sqrt2] in the hand); `pose_preview.py` confirms the check column. The carry mid key shifts the arm and hand by the sword carry's own +5 and +7.

**Authoring (Tasks 2 to 4):** `python tools/anim_edit.py open axe_shield` (Expected: prints the path `work/anim-edit/axe_shield-ic_hearthguard.bbmodel`, no error); edit the named animations in Blockbench and save; `python tools/anim_edit.py apply axe_shield` (Expected: the animations are copied into `art/clips/biped/axe_shield.bbmodel`, then `tools/clips.py` and `--verify` run and pass; only `clips/biped/axe_shield/*.json` change). Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.

- [ ] **Step 1:** Run the authoring flow for the six stance animations from the table. Keep loops closing. Expected: `apply` reports no error and six new JSON files exist.
- [ ] **Step 2:** `python tools/clips.py`, then `python tools/clips.py --verify`. Expected: no errors; new JSON only in `clips/biped/axe_shield/`.
- [ ] **Step 3:** `pose_preview.py` for each stance at 8 angles (Task 1 Step 4 for reading the haft). Expected: each haft check matches the table within 0.05; axe never in the thigh, beard or shield.
- [ ] **Step 4:** Render with `GRAVES_GRADE_CLIPS=guard,wall,brace,shield,idle,carry` via `bash test-tools/work/t16r12/live/run.sh solo grade`. Expected checklist: (a) guard crouch reads low with legs scale 0.8 and no knee-through-skirt; (b) wall and brace fit the 0.7-block cell; (c) idle arms open, not folded (or Kort keeps folded: question 3); (d) carry axe clear of the helm and horns; (e) every stance's haft and bit as in the table; (f) no blend pop entering or leaving guard.
- [ ] **Step 5:** Rebuild the compare page ("Axe stances v<N>"; video left, game right), ask Kort to approve. Commit: "Give the Hearthguard its axe-and-shield stances, video style". Trailer as above.

### Task 3: axe_shield_overhead_chop (video RBStudio 1:49.0-1:50.4 and 1:51.2-1:52.6; Mythforge 15.4-16.1 s)

**Files:** the family bbmodel (animation `axe_shield_overhead_chop`); generated `axe_shield_overhead_chop.json`. Length 1.2 s, impact marker 0.5.

Key table: rotation offsets [x,y,z] in degrees over `guard` (guard haft is (.88, .48, .01)); root position in px. The haft check is computed from the absolute pose (guard + offset) with the right_hand terms included, not from arm and forearm alone. Arm convention: more negative `right_arm` x raises the arm forward and up. The weapon direction is the requirement; `pose_preview.py --over guard` confirms the check column.

| Time (s) | body | head | right_arm | right_forearm | right_hand | left_arm | root | Haft check (fwd, down, x) | Weapon direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | (.88, .48, .01) | guard: haft forward-down, bit forward |
| 0.15 | [-6,-6,0] | [3,3,0] | [-22,5,-10] | [-21,0,0] | [20,8,6] | [-3,0,0] | [0,-0.2,-0.3] | (.72, -.69, .01) | axe rising past the hip, haft swinging up through forward, bit forward |
| 0.28 (wind-up peak) | [-12,-10,0] | [6,6,0] | [-40,-4,-16] | [-40,0,0] | [39,4,-4] | [-6,0,0] | [0,-0.4,-0.5] | (-.30, -.95, .01) | haft near vertical, tilted back about 18 deg, axe head above the hand, bit edge forward and up (falls edge first); never bit toward the back of the neck |
| 0.50 (impact) | [20,-12,0] | [-10,-4,0] | [-1,6,1] | [-1,0,0] | [-1,3,5] | [-4,0,0] | [0,-1.0,1.5] | (.71, .71, .01) | arm forward and down at the target's head height, haft 45 deg below horizontal pointing forward-down, bit edge leading forward-down |
| 0.70 (follow-through) | [26,-16,0] | [-8,-6,0] | [2,5,2] | [2,0,0] | [-4,2,6] | [-2,0,0] | [0,-1.2,1.8] | (.50, .87, .01) | axe low in front of the lead leg, haft about 60 deg below horizontal, bit edge down; clear of the shin and skirt |
| 1.20 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | (.88, .48, .01) | back in guard, haft forward-down, bit forward |

Body mechanics: weight sinks and rocks back on the wind-up, hips drop and the torso folds into the blow (step-in by root only, 1 to 2 px, legs left to `MotionFeet`), shield stays up in front of the chest. Retiming: the video's peak-to-low takes about 0.4 s; ours drops in 0.22 s and recovers in 0.5 s.

- [ ] **Step 1:** Author per the Authoring block (Task 2): `open axe_shield`, write the keys above in `axe_shield_overhead_chop` (impact marker at 0.5), `apply axe_shield`. Expected: no error, only the chop JSON added.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `swing_ends.py axe_shield_overhead_chop`. Expected: no errors, OK.
- [ ] **Step 3:** `pose_preview.py axe_shield_overhead_chop out.png 0 0.15 0.28 0.5 0.7 1.2 --over guard`. Expected: each haft vector (tip minus hand) matches the check column within 0.05; at 0.28 the axe head is over the hand, not through the crest or horns; at 0.7 the axe is clear of the lead knee.
- [ ] **Step 4:** Render with `GRAVES_GRADE_CLIPS=axe_shield_overhead_chop,guard`. Expected checklist: weapon direction on the 5 keys as the table at 8 angles; bit edge read in the render; no axe/beard/pauldron/shield/skirt clipping; tier I, II, III axe (tier 3 crown spike clear of the helm); `transitions` pop flag "no".
- [ ] **Step 5:** Rebuild compare ("Axe chop v<N>"), ask Kort. Repeat until approved.
- [ ] **Step 6:** `EXTRA=S/axe/AxeHitOverAttack.java MAIN=AxeHitOverAttack bash S/mec.sh` and `bash S/mec.sh`. Expected: no FAIL; chop `worst <= 15.0`. Commit: "Swing the Hearthguard axe overhead from the guard, video style".

### Task 4: axe_shield_hook_hew (video RBStudio 0:30.0-0:31.2, LA1)

Same steps as Task 3 (Authoring block, pose_preview against the check column, render, compare, `mec.sh` with `worst <= 15.0` for the hew). Length 1.1 s, impact marker 0.4. Offsets over `guard`. The right_hand terms are what keep the haft level and forward at impact (the arm alone would send it across the body).

| Time (s) | body | head | right_arm | right_forearm | right_hand | left_arm | root | Haft check (fwd, down, x) | Weapon direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | (.88, .48, .01) | guard |
| 0.20 (wind-up) | [4,-28,0] | [0,14,0] | [41,4,23] | [41,0,0] | [-43,0,9] | [-3,0,0] | [0,-0.3,-0.3] | (-.49, .05, -.87) | axe drawn out on the axe side, haft roughly horizontal pointing back and out to the axe side, bit edge facing the shield side, beard down |
| 0.40 (impact) | [8,22,0] | [0,-12,0] | [-10,-17,-3] | [-10,0,0] | [13,-9,-14] | [-4,0,0] | [0,-0.5,0.8] | (.95, .01, .32) | arm sweeps level across the front at belly to chest height; haft points forward, level and slightly across toward the shield side; bit edge toward the travel direction |
| 0.58 (follow-through, hook) | [10,32,0] | [0,-18,0] | [0,-48,2] | [0,0,0] | [8,-28,-36] | [-2,0,0] | [0,-0.5,0.6] | (.36, .02, .93) | haft roughly horizontal across the chest, beard turned inward toward the body, hand drawn toward the belly; axe clear of the shield board and the braided beard |
| 1.10 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | (.88, .48, .01) | axe back to the hip, haft forward-down, bit forward |

Body mechanics: hips and shoulders turn away then lead the cut; shield stays forward; root step 0.8 px forward at impact; legs left to planting. Retiming: video wind-up to cross is about 0.2 s to 0.4 s, near 1:1; extra time goes to the pull and recovery. Render checklist adds: the beard (hook) turns toward the body at 0.58; the axe does not pass through the braided beard or the shield rim at any of the 8 angles. Commit: "Hook-hew the Hearthguard axe across the body, video style".

### Task 5: `_wall` variants

- [ ] **Step 1:** Author both `_wall` clips (Authoring block) as offsets over `wall` (haft (.79, .62, .01)). Swing about 50 percent, root travel under 1 px on every key (the 0.7-block cell). Impact markers unchanged (0.5, 0.4).

`axe_shield_overhead_chop_wall` (length 1.0 s):

| Time (s) | body | right_arm | right_forearm | right_hand | root | Haft check (fwd, down) | Weapon direction |
|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | (.79, .62) | wall hold: haft forward-down, bit forward |
| 0.28 | [-6,-4,0] | [-42,-8,-25] | [-43,0,0] | [43,4,-5] | [0,-0.2,-0.2] | (-.29, -.96) | haft near vertical tilted back about 17 deg, bit edge forward and up; head clear of the helm and the rank behind |
| 0.50 (impact) | [14,-8,0] | [-2,4,1] | [-2,0,0] | [1,2,5] | [0,-0.4,0.6] | (.70, .71) | haft 45 deg below horizontal, bit edge leading forward-down; tip does not pass the neighbour line |
| 0.68 | [18,-10,0] | [2,3,3] | [2,0,0] | [-3,1,3] | [0,-0.5,0.7] | (.50, .87) | haft about 60 deg below horizontal, bit edge down, in front of the lead knee |
| 1.00 | 0 | 0 | 0 | 0 | 0 | (.79, .62) | back to the wall hold |

`axe_shield_hook_hew_wall` (length 0.9 s):

| Time (s) | body | right_arm | right_forearm | right_hand | root | Haft check (fwd, down, x) | Weapon direction |
|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | (.79, .62, .01) | wall hold |
| 0.20 | [2,-18,0] | [34,1,28] | [34,0,0] | [-37,-1,13] | [0,-0.1,-0.2] | (-.49, .04, -.87) | haft about horizontal pointing back-right, bit edge facing the shield side |
| 0.40 (impact) | [4,16,0] | [-13,-10,-8] | [-12,0,0] | [15,-5,-11] | [0,-0.2,0.5] | (.94, .01, .33) | haft forward, level, slightly across; bit edge toward the shield side |
| 0.55 | [6,24,0] | [-7,-40,-6] | [-6,0,0] | [14,-24,-34] | [0,-0.2,0.4] | (.36, .02, .93) | haft level across the chest, beard turned toward the body; elbow stays inside the cell |
| 0.90 | 0 | 0 | 0 | 0 | 0 | (.79, .62, .01) | back to the wall hold |

Largest root travel: 0.7 px (chop, 0.68 s) and 0.5 px (hew); both under 1 px.
- [ ] **Step 2:** `clips.py`, `--verify`, `swing_ends.py`. Render `GRAVES_GRADE_CLIPS=axe_shield_overhead_chop_wall,axe_shield_hook_hew_wall,wall,brace`. Expected: nothing leaves the 0.7-block cell (no axe tip past the neighbour's shoulder line); no blend pop wall to swing to wall.
- [ ] **Step 3:** Compare page, Kort approves, commit: "Add wall variants of the Hearthguard axe swings". (Skip if Kort's question 4 says no.)

### Task 6: Hits and blocks

- [ ] **Step 1:** Precondition: the sword_shield `hit_*` rework is committed (Task 0). If not, skip and note it in the ledger. Copy `hit_head/body/legs` (+ `_2`, `_3`) and `block`, `block_2`, `block_head`, `block_legs` into `clips/biped/axe_shield/`. The real sword values are used as-is: applied over the axe stances they keep the haft forward-down (computed below). Offsets over the active stance, key times as in the sword clips (each returns to 0 at the clip length; the shield side is copied):

| Clip (length) | body (key time: value) | right_arm | right_forearm | Stance | Haft over guard (fwd, down, x) | Haft over wall (fwd, down) | Weapon direction |
|---|---|---|---|---|---|---|---|
| `block` (0.5 s) | 0.06: [-5,6,0] | 0.09: [3,0,2] | none | guard or wall | (.88, .47, -.12) | (.79, .60) | haft forward-down, bit forward, axe tucked behind the shield rim |
| `block_2` (0.6 s) | 0.07: [-7,9,-3] | 0.11: [6,0,5] | 0.12: [-6,0,0] | guard or wall | (.90, .41, -.15) | (.81, .56) | as `block`, never crossing the shield |
| `block_head` (0.55 s) | 0.06: [1,5,0]; 0.16: [3,6,0] | 0.10: [3,0,3]; 0.20: [4,0,4] | none | guard or wall | (.79, .60, -.10) | (.68, .72) | haft forward-down, axe at the hip clear of the raised shield arm |
| `block_legs` (0.6 s) | 0.06: [14,4,0] | 0.10: [-6,0,8] | none | guard or wall | (.78, .63, -.01) | (.66, .75) | haft forward-down about 40 to 50 deg, bit forward, away from the shin; clear of the lowered shield |
| `hit_*` (all) | copied | copied | copied | active stance | read with pose_preview | | haft stays forward-down (fwd above 0.3), not through the torso, bit forward; the recoil never swings the axe head into the beard. Edit the right arm only if this check fails |

Expected (render `GRAVES_GRADE_CLIPS=block,block_2,block_head,block_legs` at 8 angles): the haft stays forward-down at 25 to 50 deg below horizontal as the table, the bit faces forward, and the axe never passes through the shield, the beard or the leg; first and last keys 0.
- [ ] **Step 2:** `clips.py`, `--verify`; render `GRAVES_GRADE_CLIPS=hit_body,hit_head,hit_legs,block`. Expected: shield stays up, axe stays low and clear; Task 1 hit-over-attack copy `worst <= 15.0`. Commit: "Give the Hearthguard its hits and blocks".

### Task 7: Hearthguard check at all levels, then land it

- [ ] **Step 1:** Render `solo_hearthguard` at levels 1, 2 and 3 (tier I to III axe, stage 3 helm horns) with `GRAVES_GRADE_CLIPS=<all axe_shield clips>`, one instance at a time. Expected: every key pose at 8 angles clear of the horns and crest, and all weapon directions as in the tables.
- [ ] **Step 2:** Run `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background. Expected: PASS. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 3:** Commit any fixes, write the ledger, tell the coordinator the new head of `work/anim-rework`; if it is gone, leave a note in the shared notes folder. Ask Kort which family comes next.
