# Thrower (Harpooner) Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Author the thrower family: stances `idle`, `carry`, `aim` and the attack `thrower_throw` for `tc_harpooner`. Kort approves each clip in game, with the harpoon always facing the move, no clipping and no blend pops.

**Architecture:** Clip data only, new folder `src/main/resources/assets/gravesandglory/clips/biped/thrower/` (overrides `common/` by name). Source of truth is a new `art/clips/biped/thrower.bbmodel`, edited through `tools/anim_edit.py open/apply`, never by hand-authoring JSON. `anim_edit.py` knows only `sword_shield`, `bow` and `common` (`TROOP`), so a `thrower` entry and the bbmodel are a **tooling change: needs the coordinator** (Task 0).

**Spec:** `docs/superpowers/specs/2026-11-03-thrower-animation-design.md`

## Global Constraints

- Impact marker fixed: `thrower_throw` at 0.40 s (tick 8), body, arrow. Clip at most 40 ticks (planned 1.1 s). Any timing change needs Kort first.
- Weapon direction (docs/ANIMATION-EDITING.md): every key states where the harpoon points; check wind-up, release, follow-through and recovery from front, 3/4 and side. For the throw the point leads toward the target at release, level and along the throwing arm. **Numeric test (Task 1 onward):** `chain_check.py` (below) must give the spec's yaw and pitch for each key within 3 degrees of yaw and 6 degrees of pitch, and the hand x at every key must be -5 or less (outside the head edge 4 and the brim edge 5.6 within 0.6). The pitches are authored WITHOUT the engine's aimPitch: in game the engine adds about -31 degrees (arm up) to both arms for any `aim` stance (`MotionStack.java:110-111`), so the shaft you see in a render is the authored pitch plus about 31.
- Signs (copy, do not guess): arm x - raises forward; body x + leans forward; body y + turns the chest right (right shoulder back); root y + is down (`guard.json` crouches +1.2); root z - is forward (`sword_shield_overhead` lunges -1.8).
- Action clips are offsets over the stance; first and last keys of every keyed channel are 0; step-ins are root offsets only.
- `python tools/clips.py` then `python tools/clips.py --verify` must pass. Counts: today (2026-11-03) `python tools/clips.py` prints `clips: wrote 62 clips` and `--verify` prints `clips: 62 clips verified`; each added clip adds 1, so expect 63 after `aim`, 65 after `idle` and `carry`, 66 after `thrower_throw`. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` (those two files and `art/clips/biped/sword_shield.bbmodel` are already modified in this worktree; leave the bbmodel and untracked files alone).
- No gameplay, save-format or network change; no Java or sheet edits in this plan. Spec questions 2 to 4 are Java and need Kort or the coordinator first.
- At most 2 Minecraft instances; Kort's client counts. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No computer-use on Minecraft. No eula.txt.
- No push, publish or merge. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Pose checks cover `tc_harpooner` (the only thrower). Ask Kort with AskUserQuestion. Each clip is approved and committed on its own.

Scratch tools (recreate from the knight plan if missing): in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/`: `pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`. Thrower tools (kept in `C:/Users/kortt/Melty/anim-refs/thrower/`, not committed, run from that folder): `chain.py` (harpoon tip from the rig chain), `final_keys.py` (reprints the spec's tip table), `step_check.py` (per-tick step and hand x), `resolve2.py` / `resolve3.py` (re-solve the arm y and hand angles), `chain_check.py`, `ThrowerHitCheck.java` and `mec_thr.sh` (Task 5).

## Review Focus

1. **Weapon direction on every key.** Level over the shoulder in `aim`, point at the target and 6 to 12 above at wind-up, point leading along the arm at release; the numeric check and 8 angles.
2. **Release arm and hand.** The release arm is nearly straight (forearm -15 absolute), the follow-through arm comes down (x from -125 to -60), the hand is turned so the "upright" harpoon lies level; the hand stays at x -5 or less and the shaft passes under the sou'wester brim ring (1.6 px below at aim and release) and clears the quiver.
3. **Start and end in the stance.** Every keyed channel's first and last key is 0 (`swing_ends.py`); `aim` loops close.
4. **Timing and visuals seen in game.** The release shows on tick 8 with the wind-up visible; no bow twist; one harpoon only (spec Questions 2 to 4).
5. **Hit over attack.** The existing MotionEngineCheck runs `sword_shield` only; the thrower copy (Task 5) stays at or under 15 degrees per tick in ticks 26 to 35.

---

### Task 0: References and preconditions (blocking)

- [x] **Step 1:** Footage obtained (2026-11-03): `awakening-spear-throw.mkv` (the throw), Thunder Cloud frames for idle. See `C:/Users/kortt/Melty/anim-refs/thrower/SOURCES.md`. `carry` has no usable video; it is authored from the rule.
- [x] **Step 2:** Frames extracted at 10 fps into `anim-refs/thrower/frames/{throw,idle,carry,guard}/` and looked at; the spec carries the checked timestamps.
- [ ] **Step 3 (needs Kort or the coordinator, before any render): the bow twist.** `TroopMotion.java:146` `bow(row)` = `ranged && !magic` is true for the thrower, so `carries(...)` twists the weapon hand (45 degrees at rest, 90 plus 90 during an attack). Every render of `aim` and `thrower_throw` shows the wrong hand until `bow` is restricted to family `bow` (spec Question 4). Check: `grep -n "static boolean bow" src/main/java/gg/gravesandglory/TroopMotion.java`. Expected today: `146:` with `row.ranged()&&!row.magic()`. If unanswered, authoring (Tasks 1 to 3) can continue on the numbers, but every render step below is blocked and must be marked "twist present" in the ledger.
- [ ] **Step 4:** Model on this branch: `grep -c tc_harpooner src/main/resources/assets/gravesandglory/models/troop/index.json`. Expected today: `0` (the model and `art/weapons/harpoon.json` are on `art/tidecallers-t1`; Question 1). Until merged, no render step runs; pose previews on the bare rig and `chain_check.py` still do.
- [ ] **Step 5 (tooling, needs the coordinator):** add the family to `tools/anim_edit.py` and create its source model.
  - **Run today (2026-11-03, this worktree), recorded:** `python tools/clips.py` printed `clips: wrote 62 clips` and exited 0, `python tools/clips.py --verify` printed `clips: 62 clips verified` and exited 0, and `git status` showed no new tracked change (still only the three pre-existing modified files). `python tools/anim_edit.py open thrower` and `python tools/anim_edit.py open thrower --troop knight` each printed the module docstring (usage, ending "Families: sword_shield (troop knight), bow (archer), common (knight); --troop picks another troop of the biped rig.") and exited **2**; `grep -n "^TROOP" tools/anim_edit.py` printed `29:TROOP = {'sword_shield': 'knight', 'bow': 'archer', 'common': 'knight'}`. So `--troop knight` alone does NOT work for an unknown family: the TROOP entry is needed first.
  - Edit: `sed -i "s/^TROOP = {'sword_shield': 'knight', /TROOP = {'thrower': 'tc_harpooner', 'sword_shield': 'knight', /" tools/anim_edit.py`. Expected: `grep -n "^TROOP" tools/anim_edit.py` shows `'thrower': 'tc_harpooner'` first. **Unverified** (not run here: the plan stage may not edit tools).
  - Source model from the committed copy (the worktree `sword_shield.bbmodel` is dirty): `git show HEAD:art/clips/biped/sword_shield.bbmodel > art/clips/biped/thrower.bbmodel`, then empty its animations so no sword clips land in the thrower folder: `python -c "import json,pathlib; p=pathlib.Path('art/clips/biped/thrower.bbmodel'); d=json.loads(p.read_text(encoding='utf-8')); d['animations']=[]; p.write_text(json.dumps(d,indent=1)+'\n',encoding='utf-8')"`. Expected: `python tools/clips.py` prints `clips: wrote 62 clips` (unchanged) and creates no file under `clips/biped/thrower/` (`thrower_throw` is kind arrow, so clips.py:36 `MELEE` does not demand a stub; a melee family would). **Unverified.**
  - Then `python tools/anim_edit.py open thrower --troop knight`. Expected (unverified): `anim_edit: wrote work/anim-edit/thrower-knight.bbmodel`. Without `--troop` the command uses `tc_harpooner` and, until `art/troops/tc_harpooner.json` exists on this branch, it should stop in `build_skins` (not run; expect an error or traceback, exit 1). After the model merge use `open thrower` and `python tools/anim_edit.py apply thrower`.
- [ ] **Step 6:** `test-tools/work/t16r12/live/run.sh` cases (coordinator, beside `solo_knight` at line 27): `solo_harpooner) go solo_harpooner GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=thrower GRAVES_GRADE_TROOP=tc_harpooner GRAVES_GRADE_CLIPS=thrower_throw,aim GRAVES_GRADE_OUT=$B/solo_harpooner/grade GRAVES_GRADE_PARTS=solo ;;` and `st_harpooner` with `GRAVES_GRADE_CLIPS=idle,carry` and `GRAVES_GRADE_PARTS=solo,loco`. Expected: `grep -c harpooner test-tools/work/t16r12/live/run.sh` prints 2 or more.
- [ ] **Step 7:** Confirm the tools: `cd C:/Users/kortt/Melty/anim-refs/thrower && python final_keys.py`. Expected: 8 lines; `aim 0.00 yaw 0.1 pitch 0.2`, `release 0.40 yaw -0.1 pitch -0.1`, `wind 0.15 yaw 0.0 pitch 5.9`, `stride 0.25 yaw 0.4 pitch 2.6`, `follow 0.55 yaw 1.4 pitch -19.7`, `recov 0.80 yaw 0.2 pitch -5.4`, `idle ... pitch -38.6`, `carry ... pitch 61.1`. Also `python step_check.py 0.25`: first line `stride 0.25 max step 21.7 deg/tick on ra_x at 0.50 s`, then `hand x worst (largest) -5.1 at 0.50 s`. Plus the scratch tools listed above exist (`ls`).

### Task 1: Stance `aim`

**Files:** `tools/anim_edit.py` and `art/clips/biped/thrower.bbmodel` (Task 0, coordinator); `art/clips/biped/thrower.bbmodel` (`aim`); generated `clips/biped/thrower/aim.json`.

Absolute values from the spec Stances table: root 0,+1.2,0; body 3,22,0 (sway 1); head -3,-22,0; right arm -100,-20,0; right forearm -70,0,0; right hand -103,2,0; left arm -75,-10,-10; left forearm -10,0,0. Loop 3.0 s, first key equals last. Weapon direction: harpoon level (authored pitch 0; in game the engine's aimPitch adds about 31 up on both arms, so the render shows about +31), point at the target at chest-to-head height, shaft 2 px outside the head edge.

- [ ] **Step 1:** In the working file (`anim_edit.py open thrower`) key those values at 0, 1.5 and 3.0 s (breath and sway at most 1 degree), save, `python tools/anim_edit.py apply thrower`. Expected: `changed aim`, then `clips: 63 clips verified`; only `thrower/aim.json` added.
- [ ] **Step 2 (numeric acceptance):** `cd C:/Users/kortt/Melty/anim-refs/thrower && python chain_check.py thrower/aim.json - 0 1.5`. Expected: yaw within 3 of 0 (derived 0.1) and pitch between -6 and +6 (derived 0.2) at both times, hand x between -5.5 and -7 (derived -6.1). If it fails, change the right hand x and y only (the solver in `chain.py`, `solve`, gives the pair for any arm values) and re-run.
- [ ] **Step 3:** `pose_preview.py aim out.png 0 --bb art/clips/biped/thrower.bbmodel --blade 0,-1,0`. Expected: shaft level (the preview does not add aimPitch) and pointing at the target in front, 3/4 and side; clears the brim tail and quiver; left arm points at the target.
- [ ] **Step 4 (blocked by Task 0 steps 3 and 4):** `GRAVES_GRADE_CLIPS=aim bash test-tools/work/t16r12/live/run.sh st1 solo_harpooner`. Expected checklist: harpoon in the right fist at 8 angles; point toward the target; the render shows the authored pitch plus about 31 up (the engine aimPitch, `MotionStack.java:110-111`); if it shows level or more than 40, record it, the first render is also the check of that value; no brim, quiver or rope clipping; no bow hand-twist.
- [ ] **Step 5:** Compare page "Aim v1" (`build_compare.py`, localhost:8771); ask Kort; `mec.sh` (no FAIL lines); commit: "Add the harpooner aim stance".

### Task 2: `idle` and `carry`

**Files:** `thrower.bbmodel` (`idle`, `carry`); `idle.json`, `carry.json`. Absolute values from the spec Stances table.

- **`idle`** (loop 2.4 s): right arm -12,0,6; forearm -10; hand 150,-10,0; left arm 2,0,-4. Weapon direction: harpoon in the right hand, **point forward and down about -39**, clear of the ground; frame `frames/idle/f_005` (the figure's right hand is the screen-left hand; point low, butt behind the shoulder).
- **`carry`** (loop 0.8 s; **authored from the rule, not from video**): body 2,0,0; right arm -25,-8,12; forearm -70; hand 120,15,0; left arm swings +-20,0,-4; forearm -10; root bob 0.4. Weapon direction: harpoon resting on the right shoulder, **point forward and up about +61, butt down behind the hip, never trailed or across the neighbour**.
- `rest`: no thrower clip; the engine falls back `rest -> carry -> idle`, so a resting unit shows `carry` (stated in the spec).

- [ ] **Step 1:** Key the values; loops close; `anim_edit.py apply thrower`. Expected: `changed idle, carry`, `clips: 65 clips verified`.
- [ ] **Step 2 (numeric):** `python chain_check.py thrower/idle.json - 0 1.2` then `python chain_check.py thrower/carry.json - 0 0.4`. Expected: idle pitch -39 plus or minus 6, carry pitch +61 plus or minus 6, yaw within 5 of 0, hand inside x -5 to -8.
- [ ] **Step 3 (blocked by Task 0 steps 3 and 4):** `GRAVES_GRADE_CLIPS=idle,carry bash test-tools/work/t16r12/live/run.sh st2 st_harpooner`. Expected: point direction as above at 8 angles; loops close; no foot slide; hat, quiver and rope coil clear.
- [ ] **Step 4:** Compare page; ask Kort; `mec.sh`; commit: "Add the harpooner idle and carry".

### Task 3: `thrower_throw` (spec Moves, Awakening video 1.9-3.2 s)

**Files:** `thrower.bbmodel` (`thrower_throw`); `thrower_throw.json`. Length 1.1 s, impact marker 0.40. The clip stores the DELTAS in the bracketed columns of the spec Moves table (absolute minus `aim`); the harpoon column is the check.

Target keys (absolute right arm, forearm, hand; harpoon tip from `chain.py`; pitches are authored, the engine adds about -31 in game):
- **0.15 wind-up:** arm -70,-30,0; forearm -60; hand -144,17,0. Harpoon yaw 0, pitch +6; torso open to 40; hand x -5.9, 1.9 px outside the head edge.
- **0.25 stride (tick 5, was 0.28):** arm -85,-10,20; forearm -55; hand -146,2,0. Harpoon 0, +3 (derived 2.6); left arm eased to -70.
- **0.40 release:** arm -125,35,10; forearm -15 (almost straight); hand -139,25,0. Harpoon 0, 0, **point leading at the target along the arm line, head height**; hand x -5.6; left arm pulled to the ribs.
- **0.55 follow-through:** arm -60,40,30; forearm -30; hand -157,41,0. The arm comes DOWN (+65 in x against the release) and across the front, hand x -5.2; hand empty; if the harpoon is still held (Question 3) it points 0, -20. **The x-only estimate fails here (-42 against -19.7); `chain_check.py` is the authority.**
- **0.80 recovery:** arm -100,10,30; forearm -40; hand -134,16,0; held harpoon 0, -5. 1.10: all deltas 0.

- [ ] **Step 1:** Key the deltas from the spec table; impact marker 0.40; `anim_edit.py apply thrower`. Expected: `changed thrower_throw`, `clips: 66 clips verified`, impact on tick 8 and on the sheet tick, length 22 ticks.
- [ ] **Step 2:** `swing_ends.py thrower_throw`. Expected: OK (every keyed channel 0 at both ends, impact at 0.4).
- [ ] **Step 3 (numeric):** `python chain_check.py thrower/aim.json thrower/thrower_throw.json 0 0.15 0.25 0.40 0.55 0.80 1.10`. Expected: yaw within 3 and pitch within 6 of 0/0 (t 0), 0/+6 (0.15), 0/+3 (0.25), 0/0 (0.40), 0/-20 (0.55), 0/-5 (0.80), 0/0 (1.10) (derived: 0.1/0.2, 0.0/5.9, 0.4/2.6, -0.1/-0.1, 1.4/-19.7, 0.2/-5.4, 0.1/0.2); **the hand x at each of those times -5 or less** (derived -6.1, -5.9, -6.1, -5.6, -5.2, -5.5, -6.1); the hand y at 0.55 greater than at 0.40 (the hand ends lower: y -2.4 to about +3.2). Add 0.25 to the time list (the stride key). If any hand x is above -5 (e.g. -4.9), raise that key's arm y by 5 and re-solve the hand with `resolve2.py <key>`.
- [ ] **Step 3b (per-tick max step):** `cd C:/Users/kortt/Melty/anim-refs/thrower && python step_check.py 0.25`. Expected: `stride 0.25 max step 21.7 deg/tick on ra_x at 0.50 s` (any channel above 25 degrees per tick fails; if you retime a key, re-run: a 0.30 stride gives 37.5 on the left arm and fails), then `hand x worst (largest) -5.1 at 0.50 s` (must be -5 or less) and `yaw range -2.2..1.4` (within 3). If you tuned the keys, edit the table at the top of `step_check.py` to match the clip and re-run.
- [ ] **Step 4:** `pose_preview.py thrower_throw out.png 0 0.15 0.25 0.40 0.55 1.10 --over aim --bb art/clips/biped/thrower.bbmodel --blade 0,-1,0`. Expected: harpoon at the target on the first four keys; the follow-through arm clears the quiver; hand x never above -5 (the head edge is 4, the brim ring 5.6).
- [ ] **Step 5 (blocked by Task 0 steps 3 and 4):** `GRAVES_GRADE_CLIPS=thrower_throw,aim bash test-tools/work/t16r12/live/run.sh th1 solo_harpooner`; 8 angles at wind-up, release, follow-through. Expected: harpoon within 3 degrees of the forward line at the release frame; the flying harpoon leaves on the release frame with the wind-up visible (if the wind-up is skipped or the harpoon leaves early, record Question 2 and attach frames); one harpoon only (else Question 3); no bow hand-twist.
- [ ] **Step 6:** Compare page "Throw v1" with `frames/throw/f_009,f_012,f_014,f_015,f_016,f_018` beside the render; ask Kort; iterate; `mec.sh` (no FAIL lines); commit: "Throw the harpoon from the aim, video style".

### Task 4: Release visuals (verification only, no Java)

- [ ] **Step 1:** In the Task 3 render, check whether the weapon hand is twisted (45 degrees at rest, 90 plus 90 in the throw). Expected today: yes unless Task 0 step 3 landed. Put frames in the ledger.
- [ ] **Step 2:** Check the item in the hand after release and the frame at 0.6 s (Question 3).
- [ ] **Step 3:** Send the coordinator the open engine items (`drawsAhead`, bow twist, hide harpoon) with the frames.

### Task 5: Hits and the thrower hit-over-attack check

`MotionEngineCheck.hitWhileAttacking` (line 1057) hard-wires `realSwordShield`, `d.stance = "hold"` and `d.shield = true`, starts the attack at tick 20 and the hit at tick 25 (attack tick 5, the gate), so its window ticks 26-35 is attack ticks 6-15, inside the 22-tick throw. The thrower copy:

- **Fixture file:** `C:/Users/kortt/Melty/anim-refs/thrower/ThrowerHitCheck.java` (copy to the scratchpad `thr/`; package `gg.gravesandglory`, never committed). It builds the family from `clips/biped/common` then `clips/biped/thrower` (same sort as `realSwordShield`), stance `ranged_aim` (stance key `aim`), `d.shield = false`, `d.enemy = true`, attack index 0 (`thrower_throw`) at tick 20, `d.hit("body", ...)` at tick 25, four seeds `seeds(4, 4100)`, gate: hit minus no-hit at most 15 degrees per tick in ticks 26-35. Extra hit ticks 28 and 34 are printed as "extra" lines only, not part of the gate.
- [ ] **Step 1:** Compile and run (own output folder so the shared `mec` dir is not clobbered): `EXTRA="<scratchpad>/thr/ThrowerHitCheck.java" MAIN=ThrowerHitCheck bash C:/Users/kortt/Melty/anim-refs/thrower/mec_thr.sh`. Expected before Task 3 lands: an `AssertionError ... NoSuchFileException ... biped\thrower` (checked 2026-11-03: it compiles and fails exactly there). Expected after: `thrower hit over attack (hit at attack tick 5, gate): adds at most N deg/tick (want 15 or less)`, two `extra` lines, then `PASS`.
- [ ] **Step 2:** If it fails or the render shows the harpoon dragged off the target line, author thrower `hit_*` (and `_2`, `_3`) at about 0.4 size that keep the right hand and the harpoon on the target line (Question 5); re-run Step 1. Otherwise record "inherited, no change".
- [ ] **Step 3:** The existing `hitOverWindow` check (line 1111) also hard-wires `realSwordShield`; add the same copy for the peak-and-fall shape only if Kort asks for it. Commit any new clips: "Add harpooner hit reactions".

### Task 6: All clips for the unit and verify game

- [ ] **Step 1:** Render `solo_harpooner` with `GRAVES_GRADE_CLIPS=thrower_throw,aim,idle,carry`. Expected: every key at 8 angles shows the harpoon as in the numeric table; no clipping of brim, quiver or rope coil; fix in the clip keys only, re-verify, re-render.
- [ ] **Step 2:** `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background (it refuses while a Minecraft client is open). Expected: PASS. Afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 3:** Expected `git status` before commit: the already-modified `sheets/preflight.json` and `Spec.java` (restored by the checkout), `art/clips/biped/sword_shield.bbmodel` (pre-existing), and only the thrower files listed above as new. Commit any fixes, write the ledger, tell the coordinator the new head of `work/anim-rework`.
- [ ] **Step 4:** Ask Kort which comes next: the engine items (Questions 2 to 4), a thrower hit set, or a visible rope or line for the ability (needs Kort).
