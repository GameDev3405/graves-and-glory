# Bear (Old Horn) Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Old Horn (`wm_oldhorn`, family `bear`) a rig, a model and two attack clips, `bear_maul` (head slash, impact tick 10) and `bear_bite` (body stab, impact tick 8), in a Souls-like weighty style, plus the trample rear. Claws and jaws face the target at every key.

**Architecture:** Today the bear draws through the vanilla renderer (no TroopModel), so `TroopMotion` never runs for it. Task 0 is new rig, model, skin and engine work (not clip work) and must exist first. After that it is clip data only: action clips are offsets over the stance (`Clip.add`), so each clip's first and last keys are 0 on every keyed channel.

**Tech Stack:** Blockbench clip projects via `tools/anim_edit.py`, `tools/clips.py`, `tools/preflight.py`, `test-tools/work/t16r12/live/run.sh`, scratchpad tools at `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (`pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`; recreate from the knight plan if missing).

**Spec:** `docs/superpowers/specs/2026-11-03-bear-animation-design.md`

## Global Constraints

- Impact markers are fixed: `bear_maul` 0.50 s (tick 10), `bear_bite` 0.40 s (tick 8). Clip lengths: maul 1.50 s (30 ticks = the cooldown), bite 1.40 s (28 ticks). Attack clips at most `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks (2.0 s). Any change needs Kort.
- Weapon faces the move (docs/ANIMATION-EDITING.md "Weapon direction"): every key's claw or jaw direction is stated in the spec table and checked in the render at front, 3/4 and side.
- `python tools/clips.py` then `--verify`. After any verify/gradle: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. Spec.java only through `tools/preflight.py`.
- Limits: at most 2 Minecraft instances (Kort's client counts); never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft; `python tools/verify.py game --offline` refuses while a Minecraft client is open.
- No push, publish or merge. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Commit once per approved move, never the already-modified worktree files.
- Expected `git status` throughout includes the already-modified `art/clips/biped/sword_shield.bbmodel`, `sheets/preflight.json`, `src/main/java/gg/gravesandglory/Spec.java` and the existing untracked docs; stage only this plan's own files.
- Ask Kort with AskUserQuestion, short and plain. Each move is approved on its own.

## Review Focus

1. Claws/jaw direction at every key: maul claws drawn back at the Load (sum +10), up-forward (-150) at the apex, 28 deg below level and aimed at the head at impact (yaw -6); bite snout 32-34 deg below level and claws level (-96 to -100) at lunge and impact, jaw closed at the impact tick.
2. Start and end in guard: first and last key of every keyed channel is 0 (`swing_ends.py`).
3. Raised paw and antlers: the maul's apex must not pass the paw through the head or antlers (all 8 angles).
4. Impact ticks: maul 10, bite 8, on the sheet tick (`clips.py --verify`); lengths 30 and 28 ticks; every key on the 0.05 s grid.
5. Hit over attack: at most 15 deg/tick extra in attack ticks 6-15 (Task 3 fixture, bear-specific; MotionEngineCheck's own fixture is sword_shield only).

## Task 0: What must exist first (NEW rig, model, skin and engine work, not clip work; needs Kort / coordinator)

Plainly: Old Horn has no rig, no model, no skin and no clips, and the engine has no four-legged gait and no `isWarning` hook. Nothing below is animation authoring; each piece is art or Java work that must be decided and assigned before any clip task starts. Hound, ravager and spider are in the same position. **One shared quadruped Task 0 is built once, not once per family:** see `2026-11-03-hound-animation.md` Task 0 (Kort's walk-leg question 2) and `2026-11-03-ravager-animation.md` Task 0 (its 0b gait check); the spider plan likewise. The bear plan owns 0a, 0b and 0d; 0c is the shared item.

| Piece | What | Owner | Status |
|---|---|---|---|
| 0a | Rig `art/rigs/bear.json` (bones root, body, chest, head, jaw, front_left_arm/forearm/paw, front_right_arm/forearm/paw, tail, back_left_leg, back_right_leg; `hands` to the paws) and the skin: `art/troops/wm_oldhorn.json` with antlers on `head` and the polar bear texture. **Pass/fail check, in px (spec "Rig decision"):** shoulder pivot 17-19 above the ground; head centre 19-21 and not lower than the shoulder; arm + forearm + paw 16.5-19 and within 1 of the shoulder height; head box 8 or more tall. The hound rig (shoulder about 10, leg 8, head about 13) fails all of these, so it is not reused | Kort decides the look (needs Kort); coordinator assigns the art work | blocked |
| 0b | Wiring, exact edits: `TroopModels.java` line 83, `Map.entry("polar_bear","hound")` becomes `Map.entry("polar_bear","bear")` (wolf and ravager stay `hound`); `tools/build_skins.py` line 142, `'polar_bear': 'hound'` becomes `'polar_bear': 'bear'`, and the rig table near line 157 gets a `'bear'` entry with the 14 bones of 0a; model in `models/troop/`, entry `wm_oldhorn` in `models/troop/index.json`, rig in `models/troop/rig/bear.json` | coordinator (Java/tools, after 0a) | blocked on 0a |
| 0c | Quadruped gait layer or `carry` walk loop (`MotionBody`/`MotionFeet` write only right_leg/left_leg today); must read leg bone names per rig (`front_*_arm`/`back_*_leg` for the bear) | coordinator and Kort (hound plan Question 2); shared with hound, ravager, spider; built once | blocked, shared |
| 0d | `TroopMotion` plays a `rear` clip while the entity's synced `isWarning` flag is set (client-only, no protocol change) | coordinator | blocked on 0b |

- [ ] **Step 1:** Report to the coordinator: "wm_oldhorn draws as the vanilla polar bear; there is no rig, model, skin or clip. 0a needs Kort; 0b-0d are coordinator work; clip Tasks 1-7 wait." Expected: each piece is accepted, assigned or declined. If Kort keeps the vanilla bear, stop and mark Tasks 1-7 cancelled.
- [ ] **Step 2 (model-only checks, no clips needed; run after 0a and 0b):** (a) `grep -c wm_oldhorn src/main/resources/assets/gravesandglory/models/troop/index.json`, expected `1`. (b) `test -f art/rigs/bear.json && test -f src/main/resources/assets/gravesandglory/models/troop/rig/bear.json && echo ok`, expected `ok`. (c) the px dimension check of 0a against `art/rigs/bear.json`, expected all four values in range. (d) Model-only render with the Task 1 `run.sh` target (add the target first, Task 1 step 1b): `bash test-tools/work/t16r12/live/run.sh solo_oldhorn`, which with no clips yet draws the bear's rest pose, expected: frames written, showing the antlered `bear` model on four planted paws, not the vanilla polar bear. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. If (d) still shows the vanilla bear, 0b did not take; stop.
- [ ] **Step 3 (clip-dependent load check, runs after Task 2 when the stances exist):** `python tools/clips.py --verify` passes with `bear_maul` and `bear_bite` read from `clips/bear/bear/`, and the Task 2 stance render shows `guard` and `idle` playing (the bear stands in the guard crouch, root y +1.2, not the rest pose). Expected: both hold; otherwise the clip folder is not loaded and nothing after Task 2 may proceed.

## Task 1: Tooling and stub clips (coordinator work for the tool edit; after Task 0 step 2 a and b)

**Files:**
- Modify (**needs coordinator**): `tools/anim_edit.py`. Today `RIG = 'biped'` (line 26) and `SRC = ROOT / 'art/clips' / RIG` (line 27) are hard-coded, `TROOP` (line 29) is `{'sword_shield': 'knight', 'bow': 'archer', 'common': 'knight'}`, and `semantic(project)` (line 90) loads `art/rigs/biped.json` through `RIG`. Required refactor (the same one the hound and ravager plans need; do it once): a `FAMILY_RIG` map (`sword_shield`, `bow`, `common`, `caster` to `biped`; `bear` to `bear`; hound and ravager when their plans land), a helper `rig_of(family)`, every use of `RIG`/`SRC`/`WORK` taking the rig from the family (`art/clips/<rig>/<family>.bbmodel`, `clips/<rig>/<family>/`), `semantic(project, rig)` loading `art/rigs/<rig>.json`, and `'bear': 'wm_oldhorn'` in `TROOP`. The interface is `python tools/anim_edit.py open bear --troop wm_oldhorn`, as in the hound plan. There is no `new` mode.
- Create (**needs coordinator**, same recipe as the hound and ravager plans): `art/clips/bear/bear.bbmodel`. Start from the committed biped file, not the dirty worktree copy: `git show HEAD:art/clips/biped/sword_shield.bbmodel > <scratch>/base.bbmodel`; replace its bones and mesh with the 14 `bear` rig bones from `art/rigs/bear.json` (names, pivots, boxes), drop every biped animation, and place the result as `art/clips/bear/bear.bbmodel`. Once the refactor exists `open bear --troop wm_oldhorn` should do this itself; if the tool can only open an existing file, use the manual route.
- Modify: `test-tools/work/t16r12/live/run.sh` (**needs coordinator**, one case line next to `solo_knight`): `solo_oldhorn) go solo_oldhorn GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=bear GRAVES_GRADE_TROOP=wm_oldhorn GRAVES_GRADE_CLIPS=${CLIPS:-guard,bear_maul} GRAVES_GRADE_OUT=$B/solo_oldhorn/grade GRAVES_GRADE_PARTS=solo ;;`, the form the hound plan uses for `solo_hound`. Run a clip with `CLIPS=guard,bear_bite bash test-tools/work/t16r12/live/run.sh solo_oldhorn`. Every render command in this plan is this target with a `CLIPS` value.
- Create (generated): `src/main/resources/assets/gravesandglory/clips/bear/bear/*.json`.

`tools/clips.py` fails if a family project lacks a clip for any MELEE (stab/slash/blunt) attackVariants id, so both attacks get stubs (zero motion, `impact` marker on the row's tick) until their task replaces them. The hit clips get real authoring in Task 3, before the attacks.

- [ ] **Step 1:** Make the refactor and the project, with stub clips `bear_maul` (length 1.5, impact 0.5) and `bear_bite` (length 1.4, impact 0.4), run `python tools/clips.py`. Expected: writes `clips/bear/bear/bear_maul.json` and `bear_bite.json`, exit 0.
- [ ] **Step 1b:** Add the `solo_oldhorn` line to `run.sh`. Expected: `grep -c solo_oldhorn test-tools/work/t16r12/live/run.sh` prints `1`, and `CLIPS=guard bash test-tools/work/t16r12/live/run.sh solo_oldhorn` (after Task 0) ends with `GRAVES_GRADE_OUT` frames under `solo_oldhorn/grade` and no `FAIL` line.
- [ ] **Step 2:** `python tools/clips.py --verify`. Expected: pass (impacts on ticks 10 and 8; lengths 30 and 28 ticks, under 40). Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 3:** `python tools/anim_edit.py open bear --troop wm_oldhorn`. Expected: prints the working file path, exit 0 (not 2), and the file lists the `bear` rig bone names (front_right_forearm among them), not biped bones.
- [ ] **Step 4:** Recreate or locate the scratchpad `swing_ends.py` and `pose_preview.py` (check they accept the `bear` rig); run `swing_ends.py bear_maul bear_bite`. Expected: both stubs PASS (all zeros) and impact markers 0.5 and 0.4.
- [ ] **Step 5:** Commit (tools + project + stub JSON): `Add the bear clip project and stubs` plus the trailer.

## Task 2: Stances (guard, idle, carry/walk, rest)

**Files:** `art/clips/bear/bear.bbmodel` (animations `idle`, `guard`, `carry`, `rest`); generated JSON.

Target: `guard`: root y +1.2, body 0, chest x +4, head x +10, jaw +2, front arms 0 (paws planted wide). `idle`: same with chest breath x +-1.5 over 3 s, loops. `carry`: four-leg walk loop (needs Task 0c). `rest`: root y +4, chest +6, head +26, arms +4 (the spec's rest table). Claw direction: paws planted, claws down. Block, shield, aim, wall and brace are not authored (no shield, no ranged weapon).

- [ ] **Step 1:** Edit through `python tools/anim_edit.py open bear --troop wm_oldhorn`, `apply`. Expected: `apply` prints verify pass; loops close within 0.01.
- [ ] **Step 2:** Render: `CLIPS=guard,idle,carry bash test-tools/work/t16r12/live/run.sh solo_oldhorn` (key frames at 8 angles). Expected checklist: four paws on the ground, no leg through the body, antlers clear of the shoulders, head low, no pop at loop seam.
- [ ] **Step 3:** Run Task 0 step 3. Expected: pass.
- [ ] **Step 4:** Commit: `Add the bear stances`.

## Task 3: Hits, and the hit-over-attack check (before the attacks)

The hits are needed by the check that Tasks 4 and 5 run, so they are authored here.

**Files:** `art/clips/bear/bear.bbmodel` (`hit_head`, `hit_body`, `hit_legs`); create `test-tools/standalone/BearHitCheck.java`.

Hit keys (spec "Other clips"): `hit_head`: 0.10 head x -10, head y +6, jaw +10, chest -2, root z +0.8; `hit_body`: 0.10 chest x -4, body x -3, root z +1.0; `hit_legs`: 0.10 body x +3, both arms x -4, root y +0.6; each is 0 at t 0 and 0.50, no loop.

- [ ] **Step 1:** Author the three hits, `apply`. Expected: verify passes; `clips/bear/bear/` holds `hit_head.json`, `hit_body.json`, `hit_legs.json` beside the attack stubs. `swing_ends.py hit_head hit_body hit_legs`: PASS.
- [ ] **Step 2 (write the fixture):** `BearHitCheck.java` (package `gg.gravesandglory`, `public final class BearHitCheck` with a `main`) is a copy of `MotionEngineCheck.hitWhileAttacking` (line 1057) and `hitOverAttack` (line 1093), which hard-wire `realSwordShield` and `d.shield = true`. **Family loading:** copy `realSwordShield` (MotionEngineCheck.java:463) as `realBear`: a constant `BEAR = "src/main/resources/assets/gravesandglory/clips/bear/"` (the `CLIPS` constant's role), loop `for (String dir : List.of("bear"))` over `Files.list(Path.of(BEAR + dir))` with `Clip.parse(Files.readString(p))` and `all.put(c.name(), c)`, exactly as the original does for `common` and `sword_shield`. Then the stances map from `idle, carry, guard, brace, wall, shield, aim, rest` that exist, and **the attack list hard-coded** as `List.of("bear_maul", "bear_bite")` (index 0 maul, 1 bite). The hits and deaths blocks and `new MotionEngine.Family("bear", stances, attacks, wall, hits, deaths)` are unchanged from the original. **`ClipLibrary` and `Tactics` are not used**: they import Fabric/Minecraft classes, and `mec.sh` compiles only the pure-Java engine files, so they cannot be compiled there. **Stance:** the original holds stance `"hold"` with `d.shield = true` (`MotionEngine.stanceKey` maps it to `shield`); the bear uses `"hold"` with `d.shield = false`, which maps to `guard`, the stance the attacks are additive over. **Drive and helpers:** `MotionEngineCheck.Drive`, `check` and `seeds` are package-private in the same package and `mec.sh` compiles `MotionEngineCheck` too, so `BearHitCheck` calls them as `MotionEngineCheck.Drive` etc. instead of copying. **Timing:** as the original, the attack starts at tick 20 (`d.attack = new MotionInput.Attack(index, d.tick())`) and a body hit lands at tick 25, so ticks 26-35 are attack ticks 6-15 = 0.30-0.80 s. That window holds the whole maul strike, the 58 deg body swing over the 2 ticks 0.40-0.50 (spec: the load the check must survive), and the bite's lunge, impact and jaw close. It runs both attacks over 4 seeds (`seeds(4, 4100)`).
- [ ] **Step 3 (run it):** `MAIN=BearHitCheck EXTRA=test-tools/standalone/BearHitCheck.java bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh` (`mec.sh` ignores arguments; `MAIN` and `EXTRA` are its env vars; it prints the last 30 lines). Expected, one line per attack, from `main`: `PASS bear hit over attack bear_maul (the attack keeps playing; the hit adds at most X deg/tick)` and `PASS bear hit over attack bear_bite (the attack keeps playing; the hit adds at most X deg/tick)`, with X at most 15.0 and no `FAIL` line. (The `PASS ... (...)` text is the format of `MotionEngineCheck`'s `run` lines; `BearHitCheck.main` prints it for each attack.) Against the Task 1 stubs (all zero) X measures the hit alone and must already pass; the real numbers are re-checked in Tasks 4 and 5. If X exceeds 15, shrink the hit clips, not the attacks.
- [ ] **Step 4:** Run the unchanged `MotionEngineCheck`: `bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh` with no env vars. Expected: its existing PASS lines, including `PASS hit over attack (the attack keeps playing; the hit adds at most X deg/tick)` for sword_shield, unchanged.
- [ ] **Step 5:** Commit: `Add the bear hits and the hit-over-attack check`.

## Task 4: `bear_maul` (video: Elden Ring Runebear 27.0-30.5 s)

**Files:** `art/clips/bear/bear.bbmodel` (animation `bear_maul` only); generated `bear_maul.json`.

Target poses (deltas over guard; weapon = right-paw claws; world pitch 0 = down, -90 = forward level, -180 = up). The spec table is the source of truth; this is the same list, every key on the 0.05 s grid:

| t (s) | Pose | Claws/jaw direction |
|---|---|---|
| 0.00 | all 0 | guard, claws down |
| 0.15 | body -8 y +6, chest -6, head -4 jaw +10, right arm +20 y +6 z -8, forearm 0, paw -6, left arm +6, root y +1.8 z +1.0 | sum +10: claws down, 10 deg BEHIND vertical, paw drawn back and out |
| 0.35 | body -50 y +10, chest -12, head -12 jaw +30, right arm -88 y +14 z -22, forearm -4, paw -10, left arm -50, root y -1.0 z +0.8 | sum -150: claws up and forward, 60 deg above level, jaw open |
| 0.40 | body -52 y +10, chest -12, head -12 jaw +30, right arm -90 y +14 z -22, forearm -4, paw -10, left arm -52, root y -1.0 z +0.8 | sum -154: the apex hold, drifting 4 deg |
| 0.50 (impact) | body +6 y -8, chest +8 y +6, head +12 jaw 0, right arm -70 y -4 z +4, forearm -10, paw -2, left arm -20, root y +0.5 z -2.0 | sum -62: claws 28 deg below level; yaw -6, aimed at the target's centre line and head |
| 0.70 | body +12 y -8, chest +14, head +14, right arm -20 y -14 z +8, forearm -6, left arm -6, root y +1.8 z -2.4 | sum +4: claws down past the centre line |
| 1.10 | body +3 y -2, chest +4, head +6 jaw +2, right arm -6, left arm -2, root y +1.4 z -0.8 | claws near down |
| 1.50 | all 0 | guard (30 ticks) |

Speeds per tick (claw sum): load to apex 40 deg/tick over 4 ticks; strike 0.40-0.50 is **92 deg over 2 ticks = 46 deg/tick**, with the body alone swinging **+58 deg (-52 to +6) in the same 2 ticks**; this body swing is the load the hit-over-attack check must survive. Easing: catmullrom. Legs unkeyed (feet planted); step is root z only.

- [ ] **Step 1:** `python tools/anim_edit.py open bear --troop wm_oldhorn`, key the table, `python tools/anim_edit.py apply`. Expected: verify passes, impact 0.5 s on tick 10, length 1.50 s (30 ticks).
- [ ] **Step 2:** `python C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/swing_ends.py bear_maul`. Expected: PASS, first and last keys of every keyed channel are 0.
- [ ] **Step 3:** Preview the summed pose: `pose_preview.py bear_maul --over guard` at t = 0.15, 0.35, 0.50, and sum body + chest + arm + forearm by hand. Expected: +10 +- 5 at 0.15, -150 +- 5 at 0.35, -62 +- 5 at 0.50, and net yaw -6 +- 3 at 0.50. If the sums disagree with the stated direction, the keys are wrong, not the text.
- [ ] **Step 4:** Render: `CLIPS=guard,bear_maul bash test-tools/work/t16r12/live/run.sh solo_oldhorn`; frames at the 5 keys and 8 angles. Expected checklist: (a) Load: paw drawn back and out, claws down and slightly behind; (b) apex: claws up-forward with the palm toward the target, not behind or sideways; (c) impact: claws on a line to head height and aimed at the target's centre line, not into the ground, the sky or 20 deg off to the left; (d) raised paw clear of head and antlers at all 8 angles; (e) back legs planted, hips hinge, no foot slide; (f) no pop at start or end.
- [ ] **Step 5:** Run the hit check (Task 3 step 3 command). Expected: `PASS bear hit over attack bear_maul (...at most X deg/tick)` with X at most 15.0 across the 58 deg body swing; if over, shrink the hits, not the maul.
- [ ] **Step 6:** Build the compare page (`build_compare.py`, serve on localhost:8771 from `anim-refs/bear/compare/`): video frames from `frames/maul_overhead/` left, game right, front/side/3-4, keys clickable. Expected: Kort sees both and approves in AskUserQuestion; fix and re-render on rejection (two fix rounds, then record and escalate).
- [ ] **Step 7:** `python tools/clips.py --verify`, then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. Expected: pass, tree back to the expected status.
- [ ] **Step 8:** Commit after approval: `Rebuild bear_maul as a rear-up claw slam` plus the trailer.

## Task 5: `bear_bite` (video: Elden Ring Runebear 43.0-46.0 s; jaw timing authored)

**Files:** `art/clips/bear/bear.bbmodel` (animation `bear_bite` only); generated `bear_bite.json`.

Both arms get the same x; the left z is the mirror of the right. Spec table is the source of truth; every key is on the 0.05 s grid.

| t (s) | Pose | Snout/claw direction |
|---|---|---|
| 0.00 | all 0 | guard |
| 0.15 | body -4, chest -4, head -14 jaw +30, arms +4 / forearm 0, root y +2.0 z +1.2 | snout -8 (up, level), jaw open, at the target's chest; claws 0 (down) |
| 0.30 | body +6, chest +8, head +4 jaw +34, arms -100 / forearm -14, root y +1.0 z -2.4 | snout +32 below level, jaws wide; claws -96 (forward, level) |
| 0.40 (impact) | body +8, chest +10, head +2 jaw 0 (closed), arms -104 / forearm -18, root y +0.8 z -3.4 | snout +34 into the torso, jaw CLOSED at the impact tick; claws -100 (forward, level, either side) |
| 0.45 | as 0.40 with jaw -1 | jaw held closed on the target |
| 0.55 | as 0.40 with body y +4, head y +12 | shake A, snout on the target |
| 0.60 | as 0.40 with body y -4, head y -12 | shake B |
| 0.75 | as 0.40 with body y +1, head y +4 | counter-key, the shake settles |
| 0.90 | body +3, chest +4, head -12 jaw +14, arms -60 / forearm -8, root y +1.4 z -1.0 | snout +9 lifting out, jaw opens; claws -57 (33 deg below level) |
| 1.10 | body +1.5, chest +2, head -4 jaw +4, arms -8, root y +1.4 z -0.4 | snout +13.5 (nose still down, easing up); claws -0.5 (down) |
| 1.40 | all 0 | guard (28 ticks) |

- [ ] **Step 1:** Key via `anim_edit.py open bear --troop wm_oldhorn` / `apply`. Expected: verify passes, impact 0.4 s on tick 8, length 1.40 s (28 ticks).
- [ ] **Step 2:** `swing_ends.py bear_bite`. Expected: PASS.
- [ ] **Step 3:** `pose_preview.py bear_bite --over guard` at 0.30 and 0.40; sum body + chest + head for the snout and body + chest + arm + forearm for the claws. Expected: snout +32 and +34 (+-4), claws -96 and -100 (+-5), and the jaw delta is 0 at 0.40 (not +12).
- [ ] **Step 4:** Render: `CLIPS=guard,bear_bite bash test-tools/work/t16r12/live/run.sh solo_oldhorn`. Expected checklist: (a) jaw open and snout toward the target at 0.30, fully closed at the impact tick 0.40; (b) forepaws forward and level, claws not behind the body and not pointing at the ground at the impact; (c) head and jaw clear of chest and antlers at body +8; (d) the shake keeps the snout on the target; (e) hips low, not floating; (f) no pop at start or end.
- [ ] **Step 5:** Run the hit check (Task 3 step 3 command). Expected: `PASS bear hit over attack bear_bite (...at most X deg/tick)`, X at most 15.0.
- [ ] **Step 6:** Compare page from `frames/bite_lunge/`, Kort approval. Expected: approved or fix round (max two). Also ask Kort question 4 (clearer bite reference).
- [ ] **Step 7:** Verify as Task 4 step 7, commit: `Rebuild bear_bite as a low pounce`.

## Task 6: Trample rear and death (after the attacks are approved; `rear` needs Task 0d)

**Files:** `art/clips/bear/bear.bbmodel` (`rear`, `death`); `TroopMotion` for the `isWarning` trigger (0d).

Keys: the spec tables. `rear` (1.00 s = 20 ticks): 0.30 body -50, chest -12, head -12 jaw +30, both arms -88 / forearm -4 (z right -22, left +22), root y -1.0 z +0.8; 0.50 body -52, head -14, arms -90; 0.70 as 0.30; 1.00 zeros. Claws up and forward (-150) through the hold. `death`: spec table (roll 90 at 1.60 s, final root offsets solved by the ground check); claws forward-down (sums -32 then -48), jaw open, nothing pointing at the sky.

- [ ] **Step 1:** Author, `apply`. Expected: verify passes; `death` does not loop and lies on the ground (every pivot and box corner within 0.1 px).
- [ ] **Step 2:** Render the trample in game: `ArmyBear.rear` for 20 ticks. Expected checklist: the bear stands up in about 0.3 s, holds with claws up and forward, drops in about 0.3 s, back legs planted.
- [ ] **Step 3:** Commit: `Add the bear rear and death`.

## Task 7: Final checks

- [ ] **Step 1:** `python tools/clips.py --verify` then reset `sheets/preflight.json` and `Spec.java` via `git checkout --`. Expected: pass; `git status` shows only this plan's commits plus the already-modified files.
- [ ] **Step 2:** `python tools/verify.py game --offline` (no Minecraft client open). Expected: pass; reset the same two files after.
- [ ] **Step 3:** Check the only troop of the family, `wm_oldhorn`, at 8 angles on maul, bite, guard, idle, rear, death: claws or jaws face the target at every key; antlers clear; no clipping. Expected: written pass/fail list in the ledger.
- [ ] **Step 4:** Save a short handoff and retire the worker. Expected: no push, no merge; the coordinator merges.
