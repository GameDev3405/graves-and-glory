# Crew Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task by task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Give the crew family (`ic_mortar`) its own Souls-like clips: `aim`, `idle`, `carry`, `rest` stances and the attack `crew_fire` (pull, shot at tick 8, kick, reload), each approved by Kort in game.

**Architecture:** One Blockbench source `art/clips/biped/crew.bbmodel` is compiled by `tools/clips.py` into `src/main/resources/assets/gravesandglory/clips/biped/crew/*.json`. A family folder overrides `common/` by name. Clips are additive offsets over the stance. Legs are unkeyed (MotionFeet plants them).

**Tech Stack:** Blockbench bbmodel, `tools/anim_edit.py`, `tools/clips.py`, scratch preview scripts, `MotionEngineCheck`, the solo grade harness `test-tools/work/t16r12/live/run.sh`.

**Spec:** `docs/superpowers/specs/2026-11-03-crew-animation-design.md`

---

## Global Constraints

- **Blocker:** spec Question 3 (when the swing plays against the 30-tick shell windup) must be answered before Task 3: it decides whether the reload belongs in the clip, so do not author `crew_fire` before it.
- Docs-first: do not start until Kort has answered Questions 1, 3 and 6 of the spec (barrel geometry, swing timing against the windup, the missing model). **Tasks 3-6 are unrunnable until `art/troops/ic_mortar.json` exists and Questions 1, 3 and 6 are answered**; Tasks 1 and 2 can run on the archer stand-in. Tasks 1-4 need no Java. Task 6 (in game) needs `art/troops/ic_mortar.json`.
- `ic_mortar` has no troop model: until it exists use `--troop archer` as the stand-in for previews (the archer's bow is in the hand and the barrel is absent; judge hand reach from the numbers and the weapon direction column).
- No gameplay, save-format or protocol change. Attack row `crew_fire` (zone body, kind blunt, impact tick 8, heavy) is fixed. Clip max 40 ticks (`BattleMath.ATTACK_CLIP_MAX_TICKS`).
- Weapon-faces-the-move rule: every key states the barrel (a root part, fixed 45 up-forward), where the hands are on it, and which way the shell points (spec Moves table). A key without a weapon direction is a defect. Check wind-up, impact and follow-through from front, 3/4 and side.
- First and last keys of every keyed channel are 0. Legs unkeyed.
- Sign convention: arm pitch negative raises the arm forward; forearm negative bends; body pitch positive leans forward; head pitch negative looks up; body yaw positive turns the left side forward (shipped `sword_shield_low_cut.json` draws back right with body y +34). Check against a shipped clip, never guess.
- Clip JSON is generated from the bbmodel; edit through `tools/anim_edit.py open` / `apply`, never hand-author JSON. Do not run `art/clips/build_clips.py`.
- Scratch tools live in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (`pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `hit_keys.py`; `window_check.py` is created in Task 4). If one is missing, recreate it from Task 1 of `docs/superpowers/plans/2026-11-02-knight-swings.md`. Below, `SP` means that directory.
- After every `clips.py`, `--verify`, gradle or grade run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. Expected `git status` before any task already shows `M art/clips/biped/sword_shield.bbmodel`, ` M sheets/preflight.json`, ` M Spec.java` and the untracked sword_shield hit JSONs and docs; only the files a task names are new.
- At most 2 Minecraft instances. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No eula.txt. No computer-use on Minecraft. No push, publish or merge.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- References: `C:/Users/kortt/Melty/anim-refs/crew/` (frames `ballista_crank`, `SOURCES.md`). Never committed.

## Review Focus

1. Weapon direction at each key (spec table): barrel fixed 45 up-forward, hands on the lanyard and the strap (reach 8 px to the hand pivot), the shell gripped by its nose and pointing down and back along the forearm at the muzzle.
2. The reload reads as stoop to the crate, lift the shell, drop it in, hand away, within 40 ticks.
3. No clipping at 8 angles: hand and shell vs barrel mouth and cap, apron vs stooping forearm, crate vs R arm, shell vs head.
4. Hit over attack: the crew fixture stays at or under 15 degrees per tick in ticks 26-35 (hit at frame 25) and 48-55 (hit at frame 47).
5. The bow code does not run on the mortar (Task 5), and the barrel kick is honest about being a root dip.

---

## Task 1: Source file with a stub `crew_fire`, and the anim_edit entry

**Files:**
- Create: `art/clips/biped/crew.bbmodel`
- Modify: `tools/anim_edit.py` (add `'crew': 'archer'` to the TROOP table at line 29; a tooling change that needs the coordinator's approval first; it also adds `crew` to the default family list at line 137; switch the default to `ic_mortar` once that model exists)
- Modify (scratch): `SP/swing_ends.py`

- [ ] **Step 1: Check the scratch tools exist**

Run: `ls "C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/"`
Expected: lists `pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `hit_keys.py`. If one is missing, recreate it from the knight plan Task 1.

- [ ] **Step 2: Point `swing_ends.py` at the crew family**

`SP/swing_ends.py` hard-codes the sword_shield folder `D` and the sword_shield `IMPACT` dict, and takes clip names as arguments. Edit it: `D` = `.../clips/biped/crew/` and `IMPACT = {"crew_fire": 0.40}` (a crew entry, the sword_shield entries may stay beside it).
Expected: `python SP/swing_ends.py crew_fire` raises FileNotFoundError until the stub exists (Step 5); after Step 5 it prints `OK`.

- [ ] **Step 3: Create the source file**

The worktree copy of `sword_shield.bbmodel` is dirty, so take the committed one: `git show HEAD:art/clips/biped/sword_shield.bbmodel > art/clips/biped/crew.bbmodel`, then delete all its animations (keep the biped rig).
Expected: `python -c "import json;d=json.load(open('art/clips/biped/crew.bbmodel'));print(len(d.get('animations',[])))"` prints `0`.

- [ ] **Step 4: The TROOP entry (approval first)**

Ask the coordinator to approve the one-line `tools/anim_edit.py` change, then add `crew` with default troop `archer`.
Expected: `python tools/anim_edit.py open crew --troop archer` no longer prints the usage text.

- [ ] **Step 5: Stub clip so `clips.py` can pass**

`clips.py` (line 395) fails a family project that lacks a clip for a melee (stab, slash, blunt) attackVariants id of that family, and `crew_fire` is blunt. Open the file with `python tools/anim_edit.py open crew --troop archer`, add an animation `crew_fire`, length 2.0 s, no bone keys, an `impact` marker at 0.40 s (tick 8; **required**, `--verify` fails a clip without it), then `python tools/anim_edit.py apply crew`.
Expected: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` passes, and `crew/crew_fire.json` exists with impact tick 8 and length 40 ticks. Expected stub: a zero-key clip (no bone channels, only the impact marker on tick 8). Without the stub it fails with `attackVariants 'crew_fire' (blunt) of family crew has no clip`.

- [ ] **Step 6: Commit**

```
git add art/clips/biped/crew.bbmodel tools/anim_edit.py src/main/resources/assets/gravesandglory/clips/biped/crew
git commit -m "Add the crew clip source with a stub crew_fire"
```
(with the trailer)

---

## Task 2: Stances `aim`, `idle`, `carry`, `rest`

**Files:**
- Modify: `art/clips/biped/crew.bbmodel`
- Generate: `src/main/resources/assets/gravesandglory/clips/biped/crew/{aim,idle,carry,rest}.json`

Target poses (spec Stances table; absolute degrees; pitch first, yaw y and roll z in brackets; legs unkeyed):

| Clip | Body | Head | R arm / forearm | L arm / forearm | Weapon direction |
|---|---|---|---|---|---|
| aim | 4 (y 6) | -14 (y -6) | -80 (z 10) / -95 | -68 (y -22, z -6) / -96 | barrel 45 up-forward over the right shoulder; R hand pivot about 4.5 ahead of and 3 above the shoulder on the lanyard cord, L hand pivot about 5 ahead and 2 above on the grip strap (arm 4 px + forearm 4 px, so reach 8 px to the hand pivot), the cord and strap hang 3-4 px from the barrel underside |
| idle | 2 (y 4) | -3 (y -4) | -12 (z 8) / -35 | -40 (y -15) / -70 | barrel slung 45 up-forward; L hand on the sling at the left chest, R hand on the apron belt |
| carry | 5 (y 3) | -2 | -12 / -35 | -40 / -70 | as idle, leaned under the weight, sway damped |
| rest | 3 | 0 | 3 (z 6) / -8 | 3 / -10 | barrel slung 45; hands at the thighs, off the barrel |

- [ ] **Step 1: Author the four stances**

Run: `python tools/anim_edit.py open crew --troop archer`, set the keys, `python tools/anim_edit.py apply crew`. Loop on; `aim` 3.0 s with 0.4 degree sway, `idle` 4.0 s, `carry` 2.4 s, `rest` 4.0 s.
Expected: four looping animations in the project; first and last keys of each channel equal.

- [ ] **Step 2: Preview each from front, 3/4 and side**

Run (per stance): `python SP/pose_preview.py aim SP/aim.png 0 1.5 --bb art/clips/biped/crew.bbmodel`
Expected checklist: aim: R hand pivot about 4.5 px in front of and 3 above the shoulder (on the cord), L hand pivot about 5 in front of and 2 above (on the grip), the two hands 0.5 px apart, R outside and L inside the barrel, head tipped up about 14 toward where the barrel mouth would be, torso leaned 4 with the left side forward 6; idle and carry: L hand at the left chest, R hand low at the belt, neither above the shoulder; rest: both hands at the thighs, arms hanging. In no view does a hand or forearm pass through the head or torso. The barrel, cord and strap are absent on the stand-in, and `--over aim` samples aim at time 0 only: judge hand positions against the numbers in the spec reach check.

- [ ] **Step 3: Compile and verify**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: four stance JSONs appear under `clips/biped/crew/`; verify passes; loops close (start == end).

- [ ] **Step 4: Kort review gate, then commit**

Build the compare page (Task 3 Step 5 pattern) for the stances beside `frames/ballista_crank/f_020` (upright) and f_060 (hunched, head down) for posture only. Kort approves in game.
```
git add art/clips/biped/crew.bbmodel src/main/resources/assets/gravesandglory/clips/biped/crew
git commit -m "Pose the mortar crew aim, idle, carry and rest stances"
```
(with the trailer)

---

## Task 3: Attack `crew_fire`

**Files:**
- Modify: `art/clips/biped/crew.bbmodel`
- Generate: `.../clips/biped/crew/crew_fire.json` (replaces the stub)

Keys (spec Moves table; deltas from `aim`; pitch degrees, yaw `y`, roll `z` in brackets; root column is position in pixels, `y` = down; hands 0). Time in seconds, tick = s x 20. Authored, no reference, except the stoop and head-down (ballista frames f001, f050-f070). R hand positions use 4 px + 4 px segments (reach 8 px to the hand pivot) from the aim stance R -80 / -95. Root signs: y positive is down, z positive is back (shipped lunges go forward with z negative, guard crouches with y +1.2), so the recoil is z +0.6.

| t | tick | body | head | R arm | R forearm | L arm | L forearm | root (px) | Weapon direction at this key (R hand = ahead, above the shoulder pivot, px) |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Barrel 45 up-forward; R hand (4.5, 3.0) on the lanyard cord, L hand (5.0, 2.0) on the grip strap |
| 0.15 | 3 | 1 | -1 | 8 | 8 | -1 | 0 | 0 | Barrel unchanged; R hand (5.4, 2.0), cord drawing down; L hand on the grip |
| 0.30 | 6 | 2 | 0 | 16 | 16 | -2 | 0 | 0 | Barrel unchanged; R hand (6.1, 0.8), cord taut (hand 2 px down, 1.6 px out); L hand on the grip |
| **0.40** | **8 impact** | 1 | -1 | 12 | 12 | -1 | 0 | 0 | **Shell leaves along the barrel axis, 45 up-forward toward the target arc; R hand (5.8, 1.5) springing back, cord released** |
| 0.50 | 10 | -2 (y -1) | -3 (y 1) | 6 | 6 | 5 | 2 | y 0.6, z 0.6 | Recoil: barrel 45 (root part), body jolts back, root nudged back (z positive; the shipped lunges go forward with z negative) and down; R hand (5.1, 2.5) flying back, L hand pushed down |
| 0.65 | 13 | 1 | 0 | 2 | 2 | -1 | 1 | y 0.3, z 0.3 | Settled, smoke at the mouth; barrel 45; R hand (4.8, 2.7) |
| 0.95 | 19 | 7 (y -5) | 5 | 30 | 29 | -7 | 0 | y 0.5 | Stooped to the ammo crate, held in front for now (Question 1): R hand (6.4, -2.1) on the shell; barrel 45; L hand stays on the grip (arm -7 cancels the body lean, within about 2 px) |
| 1.15 | 23 | 5 | 2 | -20 | 36 | -5 | 0 | y 0.3 | Shell lifted past the right shoulder: R hand (6.0, 3.5) gripping the shell by its nose, fins out past the fingertips, so the shell points nose down and back along the forearm (forearm world -150, 30 off vertical; the bore is 45) |
| 1.35 | 27 | 1 | -6 | -45 | 60 | -1 | 0 | 0 | Shell at the mouth: R hand (5.2, 5.6) is 0.5 px from the mouth (5, 6); shell nose down and back along the forearm (world -155, 25 off vertical, 20 steeper than the bore, so the hand tips it in); head up 20 total into it |
| 1.45 | 29 | 0 | -6 | -47 | 60 | 0 | 0 | 0 | Shell released into the bore, nose first; R hand (4.9, 5.9) letting go |
| 1.65 | 33 | 1 | -3 | -25 | 40 | -1 | 0 | 0 | R palm slaps the shell tail at the mouth: R hand (5.6, 4.3); barrel loaded |
| 1.85 | 37 | 1 | -1 | -8 | 12 | -1 | 0 | 0 | Hand dropping to the cord, R hand (4.9, 3.4); barrel 45 |
| 2.00 | 40 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Back in aim: R hand (4.5, 3.0) on the cord, L (5.0, 2.0) on the grip, barrel 45 |

Marker `impact` at 0.40 only. Length 2.0 s (40 ticks).

- [ ] **Step 1: Author the keys**

Run: `python tools/anim_edit.py open crew --troop archer`, replace the stub's body with the table (hands 0), keep the `impact` marker at 0.40, `python tools/anim_edit.py apply crew`.
Expected: one `crew_fire` animation, 2.0 s, marker at 0.40, no keys on legs or hands (or hands all 0).

- [ ] **Step 2: Preview the key frames**

Run: `python SP/pose_preview.py crew_fire SP/fire.png 0 0.3 0.4 0.5 0.95 1.15 1.35 1.65 2.0 --bb art/clips/biped/crew.bbmodel --over aim` (`--over aim` samples the aim stance at time 0 only; no barrel, cord or shell is drawn, so those checks are numeric)
Expected checklist (front, 3/4, side, top; hand positions are ahead, above the shoulder pivot, in px, from the spec table): at 0.30 R hand (6.1, 0.8), 2 px down and 1.6 out from aim; at 0.40 R hand (5.8, 1.5); at 0.50 body jolted back about 3 and head up; at 0.95 stooped 11 total, head down 5, R hand (6.4, -2.1) in front of the body at the crate position (crate in front pending Question 1); at 1.15 R hand (6.0, 3.5) past the right shoulder, forearm pointing up-forward about 30 off vertical; at 1.35 R hand (5.2, 5.6) about 0.5 px from where the mouth would be (5, 6), head tipped up 20 total (confirm in pose_preview; if short, change the arm number); at 1.65 hand (5.6, 4.3); at 2.00 identical to aim. No hand, forearm or shell in the head or the iron cap at any angle. If a hand position in the picture differs from the table by more than 1 px, change the table, not the picture.

- [ ] **Step 3: Compile and verify**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: `crew_fire.json` regenerated; verify passes (impact on a tick and on the sheet tick 8, clip 40 ticks, at most 40, first and last keys 0).

- [ ] **Step 4: Check the ends**

Run: `python SP/swing_ends.py crew_fire` (needs the crew entry from Task 1 Step 2)
Expected: `OK` (the script prints only FAIL lines or OK; it checks the impact at 0.40, length at most 2.0 and every first and last key within 0.01 of 0).

- [ ] **Step 5: Compare page**

Run: `python SP/build_compare.py crew_fire` and open `http://localhost:8771/`.
Expected: beside `frames/ballista_crank` for posture only (the stoop at f001/f050, head down at f060-f070). Loading, pull and shot have no footage and are judged on the weapon direction column only.

- [ ] **Step 6: Commit after Kort approves**

```
git add art/clips/biped/crew.bbmodel src/main/resources/assets/gravesandglory/clips/biped/crew/crew_fire.json
git commit -m "Pull, fire and reload the mortar from the aim stance"
```
(with the trailer)

---

## Task 4: Hit over attack for the crew, and the inherited hits

**Files:**
- Create (scratch): `SP/window_check.py` (create from the crossbow plan Task 4 Step 1, `docs/superpowers/plans/2026-11-03-crossbow-animation.md`; it must sum |x|+|y|+|z| per bone, then over bones, as the engine check does)
- Modify: `test-tools/standalone/MotionEngineCheck.java` (test tool only)

`MotionEngineCheck.hitOverAttack` (~line 1093) calls `hitWhileAttacking` (~line 1057), which hard-wires `realSwordShield`, `d.shield=true`, stance `hold`, attack index 0 (the thrust). It never plays crew clips, so it does not cover this family. Clip tick = frame - 20.

- [ ] **Step 1: Window check of the clip's own motion**

Run: `python SP/window_check.py src/main/resources/assets/gravesandglory/clips/biped/crew/crew_fire.json 6-15 28-35`
Expected: two lines; ticks 6-15: max per-bone step 4.7 and max summed step 13.5 (ticks 9-10); ticks 28-35: 7.0 (R forearm 33 to 37) and 11.75 (tick 30). Pass bounds: per-bone at most 8, summed at most 15 in both. Ticks 20-27 sum up to 17.2 but lie in neither window. This is a headroom proxy; the real gate is Step 3. If a window fails, spread that segment over more ticks or shrink the amplitude and rerun Task 3 Step 3.

- [ ] **Step 2: Add a crew case**

In `test-tools/standalone/MotionEngineCheck.java`, next to `realSwordShield` (find it with `grep -n "realSwordShield" test-tools/standalone/MotionEngineCheck.java`), add `realCrew()`: the crew stances and `crew_fire` from the new JSONs (attack index 0). Next to `hitWhileAttacking` (~line 1057) add `hitWhileAttackingCrew(seed, hitFrame, keep)`, a copy that uses `realCrew()`, `d.shield = false`, `d.stance = "aim"`, `d.enemy = true`, starts the attack at frame 20 and puts the body hit at `hitFrame` instead of 25. The existing `keep` capture is at `f == 25`; the crew helper captures at `f == hitFrame`, a separate capture for frame 47, and the same asserts (attack is the only track, one overlay, ease-in at least 0.15) run for both. In `hitOverAttack` (~line 1093) add two blocks after the existing loop: `hitFrame = 25` (assert frames 26-35 at most 15 deg/tick) and `hitFrame = 47` (assert frames 48-55 at most 15 deg/tick).
Expected: the file compiles (`bash SP/mec.sh` builds it).

- [ ] **Step 3: Run the check**

Run: `bash SP/mec.sh`
Expected: prints `hit over attack (crew, hit@25): max X <= 15` and `(crew, hit@47): max Y <= 15`, the sword_shield case unchanged; exit 0. If it fails, reshape the clip (smaller recoil first, as the knight hits needed about 0.4 size); do not change the check.

- [ ] **Step 4: Check inherited hits with the stance**

`pose_preview.py` reads the clip and `--over` from one bbmodel, and the inherited `hit_*` clips live in `art/clips/biped/common.bbmodel`. Build a scratch merge: a short python that loads `art/clips/biped/crew.bbmodel` and `common.bbmodel`, appends common's animations whose name starts with `hit_` to crew's `animations`, and writes `SP/crew_hits.bbmodel`. Then, per clip (`hit_head`, `hit_body`, `hit_legs`, and `_2`, `_3`): `python SP/pose_preview.py hit_body SP/hb.png 0 0.1 0.2 0.3 --bb SP/crew_hits.bbmodel --over aim`.
Expected: no hand or forearm passes through the head or torso over `aim`. Note that `--over aim` samples aim at time 0 and draws no barrel: it does not show the hit over the stoop (0.95) or the lifted pose (1.35); those are covered numerically by Task 4 Step 3. If a hit fails, author `hit_*.json` family overrides at about 0.4 size (`SP/hit_keys.py`), rerun Steps 1 and 3, commit them with this task.

- [ ] **Step 5: Commit**

```
git add test-tools/standalone/MotionEngineCheck.java src/main/resources/assets/gravesandglory/clips/biped/crew
git commit -m "Check a hit over the mortar pull, kick and reload"
```
(with the trailer)

---

## Task 5: Java fix so the bow code does not run on the mortar (needs approval)

Only after Kort approves Question 5. Skip otherwise and note it in the final report. Make it one change with the crossbow plan's Task 5 (whichever lands first edits the same line).

**Files:**
- Modify: `src/main/java/gg/gravesandglory/TroopMotion.java` (`bow(row)`, line 146)

- [ ] **Step 1: Narrow the check**

Change `bow(row)` to `row!=null&&row.ranged()&&!row.magic()&&!"crossbow".equals(row.family())&&!"crew".equals(row.family())`. Do not narrow to `"bow".equals(family)`: that would also remove the bow twist from the thrower (`tc_harpooner`).
Expected: compiles; `bow(row)` is true for archer, longbow, bonebow, stalker, wm_hunter, tc_harpooner and false for ic_arbalist, ic_ballista and ic_mortar.

- [ ] **Step 2: Run the checks**

Run: `bash SP/mec.sh; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: MotionEngineCheck passes; archers and the harpooner unchanged.

- [ ] **Step 3: Commit**

```
git add src/main/java/gg/gravesandglory/TroopMotion.java
git commit -m "Only the bow family draws a bow"
```
(with the trailer)

---

## Task 6: Render in game

**Needs** `art/troops/ic_mortar.json` (spec Question 6) including the `mortar_barrel` root part. If it does not exist, stop and report; do not stub a model here.

**rootScale:** read it, do not guess: `python -c "import json;p=json.load(open('art/troops/ic_mortar.json'))['bones'].get('root',{}).get('pivot',[0,0,0])[1];print((24-p)/24)"`
Expected: prints the value (1.0 with no root pivot; about 0.90 only if the model lowers the root 2.4 px). Write it into the spec's Units table and the commit message; if below 0.85 or above 1.0, tell the coordinator first.

**Files:**
- Modify (gitignored): `test-tools/work/t16r12/live/run.sh` (add case `solo_mortar`: `GRAVES_PHASE2_CHECKS=motion-grade`, `GRAVES_MOTION_FAMILY=crew`, `GRAVES_GRADE_TROOP=ic_mortar`, `GRAVES_GRADE_CLIPS=aim,idle,carry,rest,crew_fire`, `GRAVES_GRADE_OUT`, `GRAVES_GRADE_PARTS=solo`)

- [ ] **Step 1: Render the Mortar**

Run: `bash test-tools/work/t16r12/live/run.sh solo_mortar` (one instance).
Expected, frames at 8 angles for aim and `crew_fire` at 0.30, 0.40, 0.50, 0.95, 1.15, 1.35, 1.65: (a) at 0.30 the right hand on the taut lanyard (hand 2 px down from aim), the barrel at 45 up-forward with the left hand on the strap; (b) at 0.40 the mouth flaring at the target arc, the hand released; (c) at 0.50 the body jolted back with the barrel still at 45 (root dip only); (d) at 0.95 the stoop, hand at the crate in front, head down, left hand still on the strap; (e) at 1.35 the shell at the mouth, gripped by the nose, pointing down and back along the forearm, the head tipped up; (f) no hand, forearm or shell through the barrel, the cap, the head, the apron, the crate or the tabard at any angle; (g) the mouth glow visible at aim and at 1.35; (h) idle, carry, rest read as a barrel slung with the hands clear of it.

- [ ] **Step 2: Compare page and sign-off**

Run: `python SP/build_compare.py crew_fire` with the render.
Expected: Kort sees the render beside the ballista stoop frames and approves or lists fixes; at most two fix rounds, then record the remaining issues and stop.

---

## Task 7: Final check

- [ ] **Step 1: Verify**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: pass; the crew family lists `aim`, `idle`, `carry`, `rest`, `crew_fire` plus any hit overrides.

- [ ] **Step 2: Mechanics check**

Run: `bash SP/mec.sh`
Expected: MotionEngineCheck passes (sword_shield and crew cases).

- [ ] **Step 3: Verify game**

Close every Minecraft client first, then run `python tools/verify.py game --offline` (docs/SETUP.md), then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
Expected: exits 0; `git status` shows only the crew files, `tools/anim_edit.py`, the check file and (if approved) `TroopMotion.java`, beside the already-modified preflight.json, Spec.java and sword_shield files.

- [ ] **Step 4: Land it**

No push or merge. Report to the coordinator: commits made, Kort's approvals, open items (Questions 1, 3, 5, 6).
