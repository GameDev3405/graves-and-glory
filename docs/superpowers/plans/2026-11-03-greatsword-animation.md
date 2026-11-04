# Greatsword Animation (The Hollow King) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `greatsword` family clips for `gb_hollowking`: the `guard` stance, `idle` and `carry`, and the three attacks `greatsword_overhead_cleave`, `greatsword_side_cut` and `greatsword_thrust`, with the weapon facing the move at every key. Kort approves each move in game.

**Architecture:** Clip data only. A family folder `src/main/resources/assets/gravesandglory/clips/biped/greatsword/` overrides `clips/biped/common/` by name (`TroopMotion.build`). An action clip is an offset over the stance (`MotionStack.action`, `Clip.add`): every swing's keys are deltas from `guard`, and the first and last key of every keyed channel is 0. Edit the keys in a new `art/clips/biped/greatsword.bbmodel` (copy the structure of `sword_shield.bbmodel`; never edit the committed bare-rig files directly, use `open` per `docs/ANIMATION-EDITING.md`), rebuild with `tools/clips.py`, render with the motion-grade harness, compare against the video.

**Spec:** `docs/superpowers/specs/2026-11-03-greatsword-animation-design.md` (the key tables below are copied from it; if they differ, the spec wins). Video: `C:/Users/kortt/Melty/anim-refs/greatsword/` (`SOURCES.md`).

## Global Constraints

- Strike timing is fixed (`Spec.ATTACKVARIANTS`): cleave impact 0.60 s (12 ticks), side cut 0.60 s (12), thrust 0.35 s (7). Clips at most 40 ticks (2.0 s). Any timing change needs Kort.
- No gameplay, network, protocol or sheet change. `Spec.java` only through `tools/preflight.py`.
- Every key of every attack states and shows the blade direction (rule: `docs/ANIMATION-EDITING.md`, "Weapon direction"). A key without a direction is a defect.
- Root-only steps; legs left to foot planting (`MotionFeet`, legShare 1) unless Kort asks for a real step.
- `python tools/clips.py --verify` must pass; the greatsword hit-over-attack check (Task 6A) must pass: ticks 26-35, step with hit minus step without at most 15 deg per tick, cloth springs count. The stock MotionEngineCheck "hit over attack" runs only the sword_shield fixture and proves nothing for this family.
- After any verify or gradle: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Limits: at most 2 Minecraft instances (Kort's client counts); never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft. No push, publish or merge to main.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. One commit per approved move.
- Ask Kort with AskUserQuestion, short and plain; each move approved and committed on its own.

## Scripts and paths

Scratchpad (work files, never committed): `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (below `$S`). It holds `swing_ends.py`, `pose_preview.py`, `build_compare.py`, `mec.sh`, `guard_edit.py`. **If a script is missing there, recreate it from the knight plan** (`docs/superpowers/plans/2026-11-02-knight-swings.md`, which says what each one does) before starting. `test-tools/work/t16r12/live/run.sh` is gitignored; if it lacks the `solo_gs` and `solo_gs_stand` cases of Task 0, add them.

## Review Focus

1. **Blade direction at every key.** Wind-up, impact, follow-through and recovery are checked from front, 3/4 and side against the spec's table; a clean pose with the blade the wrong way is a defect.
2. **Start and end in guard.** First and last key of every keyed channel is 0 (`swing_ends.py`); the left hand stays on the grip at every key.
3. **Long-blade clipping at 8 angles:** blade through the head or crown at the cleave top, into the ground at the cleave follow-through, through the right thigh in the side cut, through the cloak or pennant on the turn (clearance budget in the spec's Units section).
4. **Greatsword hit-over-attack (Task 6A)**, a hit window per clip, with the cloak and pennant springs: shrink hit sizes only if it fails, and say so.
5. **Thrust blade yaw** within about 2 deg of the body's forward line at impact (3/4 and top-down renders), and the draw per-tick step check (no bone over 25 deg per tick, the single cap for all three attacks).

---

### Task 0: Gate on the model (Kort) and name the render command

The Hollow King has no model, skin or `grave_sword` (`models/troop/` holds six Gilded troops). Two paths; ask Kort (spec Question 1) which one.

- [ ] **Step 1:** Ask Kort with AskUserQuestion: build now and grade on a stand-in, or wait for the Graveborn model. Do not start Task 2 until answered. Task 1 may proceed.
- [ ] **Step 2 (stand-in path):** add to `test-tools/work/t16r12/live/run.sh` (gitignored), before the closing `esac`, these two cases. The stand-in is the Marshal (`gl_marshal`, the legendary-size biped) with the greatsword family forced on it:

```
 solo_gs_stand) go solo_gs_stand GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=greatsword GRAVES_GRADE_TROOP=gl_marshal GRAVES_GRADE_CLIPS=${GS_CLIPS:-guard,idle,carry,greatsword_overhead_cleave,greatsword_side_cut,greatsword_thrust} GRAVES_GRADE_OUT=$B/solo_gs_stand/grade GRAVES_GRADE_PARTS=solo,transitions ;;
 solo_gs) go solo_gs GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=greatsword GRAVES_GRADE_TROOP=gb_hollowking GRAVES_GRADE_CLIPS=${GS_CLIPS:-guard,idle,carry,greatsword_overhead_cleave,greatsword_side_cut,greatsword_thrust} GRAVES_GRADE_OUT=$B/solo_gs/grade GRAVES_GRADE_PARTS=solo,transitions ;;
```

  Run one clip set: `GS_CLIPS=greatsword_overhead_cleave,guard bash test-tools/work/t16r12/live/run.sh gs1 solo_gs_stand`. Expected checklist: log prints `== solo_gs_stand start`, then `== solo_gs_stand try 1 end ... rc=0`, then `ALLDONE`; `test-tools/work/t16r12/live/gs1/solo_gs_stand/grade/` holds one key-frame PNG set per clip and a `transitions` report; no `NoClassDefFoundError` (the script retries once); at most 2 clients running.
- [ ] **Step 2b (stand-in Expected):** the grade must be read as angles only. Expected line to confirm from the run output and its render: troop `gl_marshal`, held item `minecraft:netherite_sword` in the right hand (Spec row; `models/troop/gl_marshal.json` has `hand: right`), body at the Marshal's own scale, **not** the Hollow King's [1.2, 1.15, 1.15] (that is unverified until his model exists). The Marshal's Spec shield tag (`shield,large`) may draw a shield on the left arm; if it does, note it as a stand-in artifact and judge the two-hand grip from `pose_preview.py`. The stand-in grade validates the blade angles, direction at each key and gross clearances only; it does not validate hand reach, blade length, crown or cloak clearance, or scale, which wait for the real model.
- [ ] **Step 3 (wait path):** if Kort picks the wait path, **the in-game grade is blocked**: Tasks 2 to 5 stop after `pose_preview.py` and `clips.py --verify` and report "grade blocked until the Hollow King model exists"; use `solo_gs` once `models/troop/gb_hollowking.json` exists.
- Check before any run: no `eula.txt` written, PIDs 29360, 4580, 6048 and `C:/GGMatch` untouched; `Get-CimInstance Win32_Process` shows at most 1 other game client.

### Task 1: Tooling (work files, not committed)

**Files:** `$S/swing_ends.py`, `$S/pose_preview.py`, `$S/build_compare.py`, `$S/mec.sh`; `tools/anim_edit.py` (one line); new `art/clips/biped/greatsword.bbmodel`.

- [ ] **Step 1:** Confirm or recreate `$S/swing_ends.py` and `$S/pose_preview.py`. Run: `python "$S/swing_ends.py" sword_shield_overhead` from the worktree. Expected: it prints OK for the knight clip (first and last keys 0, impact 0.50 noted), which proves the script works. It must FAIL for any keyed channel whose first or last key is not within 0.01 of 0 and for an `impact` marker other than 0.60 (cleave, side cut) or 0.35 (thrust); add the expected markers as arguments.
- [ ] **Step 2:** Register the family in the editor. `tools/anim_edit.py` knows only `sword_shield`, `bow` and `common` (its `TROOP` map and `art/clips/biped/<family>.bbmodel`), so `open greatsword` prints the usage text and exits 2 today. Add `'greatsword': 'gl_marshal'` to `TROOP` (stand-in mesh; change to `'gb_hollowking'` once the model exists), and create the source by copying the bare-rig `art/clips/biped/common.bbmodel` to `art/clips/biped/greatsword.bbmodel` with `animations` emptied: `python -c "import json,pathlib; p=json.loads(pathlib.Path('art/clips/biped/common.bbmodel').read_text(encoding='utf-8')); p['animations']=[]; pathlib.Path('art/clips/biped/greatsword.bbmodel').write_text(json.dumps(p,indent=1)+'\n',encoding='utf-8')"`. Check `tools/clips.py` accepts a bbmodel with no animations; if it does not, create one empty `guard` animation instead.
- [ ] **Step 3:** Open the working file. Exact command, from `C:/Users/kortt/Melty/bannerbound-anim-rework`: `python tools/anim_edit.py open greatsword --troop gl_marshal --launch`. Expected: prints `anim_edit: wrote work/anim-edit/greatsword-gl_marshal.bbmodel (0 clips)` (or 1), then the path; Blockbench opens it with the Marshal mesh and the rig bones listed in the spec (root, body, head, arms, forearms, hands, legs, shins). If it says "holds edits not yet applied; kept", the file has earlier edits: do not pass `--fresh` without checking they are saved.
- [ ] **Step 4:** After each clip is keyed and saved: `python tools/anim_edit.py apply greatsword`. Expected: it copies the animations into `art/clips/biped/greatsword.bbmodel`, runs `python tools/clips.py` and `python tools/clips.py --verify`, exit 0; new files only under `src/main/resources/assets/gravesandglory/clips/biped/greatsword/`. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.

- [ ] **Step 5 (the one cap):** create `$S/step_check.py <clip>...`: load the clip JSON, sample every keyed bone at 20 ticks per second with `clips.sample`, and print the largest single-bone step in degrees per tick and the bone, for rotation channels. FAIL over 25. Expected: `python "$S/step_check.py" sword_shield_overhead` prints a number (the knight drop is about 22) and OK; the same 25 limit applies to `greatsword_overhead_cleave`, `greatsword_side_cut` and `greatsword_thrust` (authored legs: cleave R arm about 22, side cut body about 21, thrust R hand 24). Run it in Step 2 of Tasks 3 to 5.

### Task 2: Stances `guard`, `idle`, `carry`

**Files:** `art/clips/biped/greatsword.bbmodel` (animations `guard`, `idle`, `carry`); generated `guard.json`, `idle.json`, `carry.json`.

Key table (absolute rotations in degrees [x, y, z]; root in px; **blade direction in the last column**; numbers are first-pass, the blade column is the acceptance):

| Clip | t (s) | body | head | R arm | R forearm | L arm | L forearm | hands R / L | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|---|
| `guard` (2.4 s loop) | 0.00 and 2.40 | [8, -8, 0] | [-6, 8, 0] | [-125, -10, -20] | [-95, 0, 0] | [-60, 25, 10] | [-70, 0, 0] | [0,0,0] / [0,0,0] | [0, 1.0, 0] | Up and back over the right shoulder, about 50 deg above horizontal, tip behind and above the shoulder |
| | 1.20 (breath) | [9.5, -8, 0] | [-7.5, 8, 0] | [-127, -10, -20] | [-96, 0, 0] | [-61, 25, 10] | [-71, 0, 0] | same | [0, 1.1, 0] | same, tip bobs about 1 deg |
| `idle` (3.0 s loop) | 0.00 and 3.00 | [2, 0, 0] | [0, 0, 0] | [-30, -4, 6] | [-30, 0, 0] | [-34, 6, -4] | [-36, 0, 0] | [0,0,0] / [0,0,0] | [0, 0, 0] | Point down, a step ahead of the right foot, 3 px off the floor, hands stacked on the pommel at the belt |
| | 1.50 (breath) | [3, 0, 0] | [-1, 0, 0] | [-31, -4, 6] | [-31, 0, 0] | [-35, 6, -4] | [-37, 0, 0] | same | [0, 0.1, 0] | same |
| `carry` (2.0 s loop) | 0.00 and 2.00 | [4, -6, 0] | [-3, 6, 0] | [-100, -5, -15] | [-100, 0, 0] | [-45, 15, 5] | [-40, 0, 0] | [0,0,0] / [0,0,0] | [0, 0.4, 0] | Resting on the right shoulder, edge up, tip up and forward about 30 deg above horizontal |
| | 1.00 (sway) | [4, -3, 0] | [-3, 3, 0] | [-102, -5, -15] | [-100, 0, 0] | [-47, 15, 5] | [-40, 0, 0] | same | [0, 0.5, 0] | same, tip sways about 2 deg |

Legs in `guard`: knight stagger (left_leg [-40,-6,-7], left_shin 41, right_leg [30,8,2], right_shin 14) widened 4 deg; `idle` and `carry` leave legs to the walk and foot planting.

- [ ] **Step 1:** Key the three loops from the table (closed loops: first and last key equal). Run `python tools/anim_edit.py apply greatsword`. Expected: exit 0, only the three files added.
- [ ] **Step 2:** `python "$S/pose_preview.py" guard "$S/gs_guard.png" --bb art/clips/biped/greatsword.bbmodel` (and `idle`, `carry`; it draws front, 3/4 and side; `--blade x,y,z` sets the blade vector, defaulting to the knight sword, so pass the blade direction of the pose being checked). Expected: guard blade up and back over the right shoulder about 50 deg above horizontal; idle point down a step ahead of the right foot, 3 px off the floor; carry resting on the right shoulder, tip up and forward about 30 deg; left hand on the grip in all three.
- [ ] **Step 3:** Render: `GS_CLIPS=guard,idle,carry bash test-tools/work/t16r12/live/run.sh gs2 solo_gs_stand` (or `solo_gs` once the model exists). Expected checklist: rc=0, ALLDONE; no clip through head, legs, cloak or floor at 8 angles; compare page below shows idle and carry next to `frames/idle` and `frames/carry`.
- [ ] **Step 4:** Ask Kort to approve; commit `art/clips/biped/greatsword.bbmodel`, `tools/anim_edit.py` and the three JSONs: "Give the Hollow King a high guard, rest and shoulder carry".

### Task 3: Overhead cleave (video 0:00.7-0:01.0)

**Files:** `art/clips/biped/greatsword.bbmodel` (animation `greatsword_overhead_cleave`, length 1.8 s, impact 0.60); generated JSON.

Key table (deltas over `guard`; first and last key zero):

| t (s) | body | head | R arm | R forearm | L arm | L forearm | R hand / L hand | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | 0 / 0 | [0,0,0] | Guard: up and back over the right shoulder |
| 0.20 lift | [-6,0,0] | [3,0,0] | [-25,0,0] | [-10,0,0] | [-20,0,0] | [-10,0,0] | -10 / 0 | [0,0.2,0.4] | Rising toward vertical, tip behind the head |
| 0.36 top (hang) | [-15,4,0] | [8,0,0] | [-50,0,-6] | [-20,0,0] | [-45,0,0] | [-25,0,0] | -30 / 0 | [0,0.4,0.8] | Vertical to tilted back behind the head, edge toward the enemy, tip above and behind the skull and crown (at least 6 px clear) |
| 0.48 mid-drop | [6.5,0,0] | [-2,0,0] | [2.5,0,2] | [5,0,0] | [0,0,0] | [2,0,0] | -35 / -10 | [0,-0.2,-0.8] | Halfway down, blade about 45 deg from vertical, edge forward, tip in front of the face line |
| 0.60 impact | [28,-4,0] | [-12,0,0] | [55,0,10] | [30,0,0] | [45,0,0] | [30,0,0] | -40 / -20 | [0,-0.8,-2.4] | Down, edge first, about 45 deg forward and down, tip at chest height in front of the target, on the front line, both hands at the belt |
| 0.75 follow | [20,-2,0] | [-8,0,0] | [70,0,8] | [34,0,0] | [55,0,0] | [30,0,0] | -30 / -10 | [0,-1.0,-2.8] | Low in front, point at least 3 px above the floor, hands at the thigh |
| 1.20 recover | [6,0,0] | [-2,0,0] | [20,0,0] | [8,0,0] | [15,0,0] | [8,0,0] | -8 / 0 | [0,-0.3,-0.8] | Rising back toward guard, tip coming up and back |
| 1.80 | zeros | zeros | zeros | zeros | zeros | zeros | 0 / 0 | [0,0,0] | Guard |

- [ ] **Step 1:** Key it (catmullrom). Keep the impact marker at 0.60 and the length at 1.8 s.
- [ ] **Step 2:** `python tools/anim_edit.py apply greatsword`, then `python "$S/swing_ends.py" greatsword_overhead_cleave`. Expected: exit 0, OK, only `greatsword_overhead_cleave.json` is new or changed. Then `python "$S/step_check.py" greatsword_overhead_cleave`: expected at most 25 deg per tick (about 22).
- [ ] **Step 3:** `python "$S/pose_preview.py" greatsword_overhead_cleave "$S/gs_cleave.png" 0 0.2 0.36 0.48 0.6 0.75 1.2 --bb art/clips/biped/greatsword.bbmodel --over guard`. Expected: the blade direction of each row of the table, tip clear of head and crown by 6 px and of the floor by 3 px.
- [ ] **Step 4:** Render: `GS_CLIPS=greatsword_overhead_cleave,guard bash test-tools/work/t16r12/live/run.sh gs3 solo_gs_stand`. Expected: rc=0; key frames at 8 angles; the `transitions` report says no pop; no clip through head, crown, legs, cloak or ground.
- [ ] **Step 5:** Compare page. Make the greatsword copy of the script once: `cp "$S/build_compare.py" "$S/build_compare_gs.py"` and edit its `REF` to `C:/Users/kortt/Melty/anim-refs/greatsword/frames`, `OUT` to `C:/Users/kortt/Melty/anim-refs/greatsword/compare`, and `MOVES` to `('overhead_cleave','Overhead cleave (video 0:00.7)',['overhead_cleave'],['greatsword_overhead_cleave'],None)`, `('side_cut','Side cut (video 1.0)',['side_cut'],['greatsword_side_cut'],None)`, `('shoulder','Guard (video 2.2)',['shoulder_ready'],['guard'],None)`, `('carry','Carry (RamsterZ 6 s)',['carry'],['carry'],None)`, `('idle','Idle (RamsterZ 14 s)',['idle'],['idle'],None)`; and the thrust row written out in full: `('thrust', 'Thrust (reference-free: no video, game only)', [], ['greatsword_thrust'], None)` (empty ref list; if the script indexes `refs[0]`, skip the ref column for that row and keep the label). Run it again after Task 5 so the row has game frames. Run `python "$S/build_compare_gs.py" test-tools/work/t16r12/live/gs3/solo_gs_stand "Greatsword cleave"`, then serve it: `python -m http.server 8771 --directory C:/Users/kortt/Melty/anim-refs/greatsword/compare` (background) and open `http://localhost:8771/index.html`. Expected checklist: page loads with the cleave row showing 7 video frames left and the game frames right at front, 3/4 and side; the blade in each game frame points the way the table's direction column says; no console 404. Ask Kort to approve.
- [ ] **Step 6:** Run the greatsword hit-over-attack check (Task 6A) for this clip once the check exists; before it exists, run `bash "$S/mec.sh"` and report that it only covers the knight. Commit bbmodel and JSON: "Cleave the Hollow King's greatsword overhead from the guard".

### Task 4: Side cut (video 0:01.0-0:01.5; flat sweep departs from the video, spec Question 7)

Same steps as Task 3, for `greatsword_side_cut` (1.7 s, impact 0.60), clip set `greatsword_side_cut,guard`, run tag `gs4`, row `side_cut` in the compare page.

Key table (deltas over `guard`):

| t (s) | body | head | R arm | R forearm | L arm | L forearm | R hand / L hand | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | 0 / 0 | [0,0,0] | Guard: up and back over the right shoulder |
| 0.15 | [2,-12,0] | [0,10,0] | [20,-15,-20] | [10,0,0] | [15,-10,-10] | [8,0,0] | -10 / 0 | [0,0.2,0.3] | Dropping to level on his right, point behind, edge toward the enemy |
| 0.36 top (hang) | [4,-45,0] | [0,38,0] | [45,-25,-40] | [25,0,0] | [40,-20,-25] | [20,0,0] | -25 / -10 | [0,0.4,0.9] | Flat and level at chest height, point behind him (3 px clear of the cloak), edge toward the enemy |
| 0.48 mid-sweep | [6,5,0] | [-1,-3,0] | [27,0,-12] | [42,0,0] | [30,-2,-7] | [35,0,0] | -17 / -5 | [0.5,-0.1,-0.6] | Level, crossing the body line at chest height, edge leading |
| 0.60 impact | [8,55,0] | [-2,-45,0] | [10,25,15] | [60,0,0] | [20,15,10] | [50,0,0] | -10 / 0 | [1.0,-0.6,-1.5] | Level across the front at chest height, **edge leading**, sweeping toward his left past the target, arms extended, hips lead |
| 0.80 follow | [6,60,0] | [-2,-50,0] | [8,20,12] | [50,0,0] | [16,12,8] | [40,0,0] | -8 / 0 | [1.2,-0.8,-1.2] | Level, point past his left side and slightly up, head looking back at the target |
| 1.20 recover | [2,15,0] | [0,-12,0] | [3,5,3] | [12,0,0] | [4,3,2] | [10,0,0] | -2 / 0 | [0.3,-0.2,-0.3] | Rising over the left shoulder and drifting back toward guard |
| 1.70 | zeros | zeros | zeros | zeros | zeros | zeros | 0 / 0 | [0,0,0] | Guard |

Extra Step 4 checks: blade through the right thigh at follow-through, and through the cloak and pennant at the wind-up top (3 px clear). Commit: "Cut the Hollow King's greatsword across from the guard".

### Task 5: Thrust (reference-free; Kort sign-off gate)

Same steps as Task 3, for `greatsword_thrust` (1.2 s, impact 0.35), clip set `greatsword_thrust,guard`, run tag `gs5`. **Reference-free:** a second search found no readable greatsword thrust video (spec Moves). No video row to compare; the compare page shows the game only. **Gate: do not commit the thrust until Kort has watched it in game at front, 3/4 and side and said yes** (AskUserQuestion); a no means re-key from his notes, not a commit.

Key table (deltas over `guard`):

| t (s) | body | head | R arm | R forearm | L arm | L forearm | R hand / L hand | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | 0 / 0 | [0,0,0] | Guard: up and back over the right shoulder |
| 0.05 | [0.8,-3,0] | [0,2,0] | [6,0,-1] | [4,0,0] | [4,-2,0] | [3,0,0] | -12 / -3 | [0,0.1,0.2] | Starting to swing down, tip still behind |
| 0.10 | [2.4,-9,0] | [-1,7,0] | [18,0,-3] | [12,0,0] | [12,-6,0] | [9,0,0] | -36 / -9 | [0,0.2,0.5] | About halfway: tip swinging down and forward |
| 0.15 draw | [4,-15,0] | [-2,12,0] | [30,0,-5] | [20,0,0] | [20,-10,0] | [15,0,0] | -60 / -15 | [0,0.3,0.8] | Level at the right hip, point forward at the target, elbows tucked, forearms level with the blade |
| 0.35 impact | [12,6,0] | [-8,-4,0] | [35,0,0] | [95,0,0] | [-20,0,0] | [45,0,0] | -15 / -5 | [0,0.7,-3.5] | Level, in line with both forearms, point at the target's chest height, within about 2 deg of the body's forward line, hands at chest |
| 0.50 follow | [10,5,0] | [-6,-3,0] | [30,0,0] | [80,0,0] | [-17,0,0] | [38,0,0] | -13 / -4 | [0,0.6,-3.0] | Extension held a beat, point forward and level |
| 0.80 recover | [4,2,0] | [-2,-1,0] | [10,0,0] | [30,0,0] | [-6,0,0] | [14,0,0] | -4 / -1 | [0,0.2,-1.0] | Pulling back, point rising |
| 1.20 | zeros | zeros | zeros | zeros | zeros | zeros | 0 / 0 | [0,0,0] | Guard |

- [ ] **Extra step A (per-tick draw check):** run a short Python over `greatsword_thrust.json` sampling the keyed bones every 0.05 s from 0.00 to 0.15 (catmullrom as the engine does, or just the keys 0, 0.05, 0.10, 0.15 since they sit on tick boundaries). Expected checklist: step 1 (0.00 to 0.05) R hand -12; step 2 -24; step 3 -24 deg; largest single-bone step at most 25 deg per tick (the one cap; `step_check.py` agrees). Over the cap: add a key at 0.20 or ask Kort to move the impact. Also check the drive 0.15 to 0.35 (4 ticks) at most 25 deg per tick per bone (expected R forearm about 19).
- [ ] **Extra step B (yaw):** blade yaw versus the body's forward line from the 3/4 and top-down renders. Expected: within about 2 deg at impact.

Commit: "Drive the Hollow King's greatsword thrust from the guard".

### Task 6A: Greatsword hit-over-attack check

MotionEngineCheck's "hit over attack" (`hitOverAttack`, about line 1093, via `hitWhileAttacking`) loads `realSwordShield` (`common` and `sword_shield`) and always plays attack index 0, so it never touches the greatsword. Add a greatsword version in a scratchpad copy first (no edit to the shared check without Kort).

Hit windows, one set per clip (attack starts at absolute tick 20 as in `hitWhileAttacking`, a hit lands at the tick shown, the compared window is the 10 ticks after it; clip ticks are 20 per second). The v1 stock window 26-35 is only the first row's; the thrust (24 ticks) runs 20 to 44 absolute, so 26-35 is inside it, but its recovery needs its own window.

| Clip | Part tested | Hit at tick | Window (ticks) | Clip part covered |
|---|---|---|---|---|
| cleave (36 ticks, impact at clip tick 12) | impact | 30 | 31-40 | drop and impact (clip ticks 11-20) |
| cleave | recovery | 35 | 36-45 | follow-through into recovery (clip ticks 16-25; recovery starts 0.75 s = tick 15) |
| side cut (34 ticks, impact 12) | impact | 30 | 31-40 | sweep and impact |
| side cut | recovery | 36 | 37-46 | follow-through into recovery (recovery starts 0.80 s = tick 16) |
| thrust (24 ticks, impact 7) | impact | 25 | 26-35 | draw end, drive and impact (clip ticks 6-15; the stock window) |
| thrust | recovery | 30 | 31-40 | extension into pull-back (recovery starts 0.50 s = tick 10) |

Pass for every row: with-hit step minus without-hit step at most 15 deg per tick, cloth springs counted.

- [ ] **Step 1:** Copy `test-tools/standalone/MotionEngineCheck.java` to `$S/GreatswordHitCheck.java` (class `GreatswordHitCheck`, package `gg.gravesandglory`). Add `realFamily(String name, List<String> attacks)` (clone of `realSwordShield` with the dir list `common`, `greatsword`, the stance list, and the attack ids `greatsword_overhead_cleave`, `greatsword_side_cut`, `greatsword_thrust`); parameterise `hitWhileAttacking(seed, hit, keep, attackIndex)` so `d.attack = new MotionInput.Attack(attackIndex, d.tick())`, `d.shield = false`; and make `main` call `hitOverAttack` once per attack index 0, 1, 2 (hit tick and window per the table above). Include the cloak and pennant springs the same way the stock check does (Drive with the seeds).
- [ ] **Step 2:** Run: `MAIN=GreatswordHitCheck EXTRA="$S/GreatswordHitCheck.java" bash "$S/mec.sh"` (`mec.sh` compiles the engine sources and prints the last 30 lines). Expected checklist: six lines, one per table row, each "hit over attack: ... the hit adds at most N deg/tick" with N at most 15; no FAIL lines; "checks passed". If a clip exceeds 15, shrink the hit size (the knight needed about 0.4) or the swing's peak step, and record which, as the knight plan did.
- [ ] **Step 3:** Report the numbers to the coordinator. Ask Kort before promoting the check into `MotionEngineCheck.java` (a shared test file).

### Task 6: Final check and land

- [ ] **Step 1:** Render the whole family: `bash test-tools/work/t16r12/live/run.sh gs6 solo_gs_stand` (default clip set: guard, idle, carry and the three attacks; use `solo_gs` once `gb_hollowking` has a model). **If the model is not there and Kort did not accept the stand-in, the in-game grade is blocked: say so and stop here.** Expected checklist: rc=0; every key's blade direction at 8 angles against the spec; the three attacks chained (`transitions`) show no pop; hits and deaths inherited from `common/` still look right with the blade held. **Decision (fix round 2):** hits stay inherited from `common/` and no block is built in this plan; greatsword-specific hits and a block are a separate backlog item (spec Question 4), not a step here.
- [ ] **Step 2:** `grep '"greatsword"' src/main/java/gg/gravesandglory/Spec.java`: expected one TROOPS row (`gb_hollowking`).
- [ ] **Step 3:** In the background: `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline`. Expected: PASS. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 4:** Commit any fixes, write the ledger, tell the coordinator the new head of `work/anim-rework`.
- [ ] **Step 5:** Add one line to `docs/BACKLOG.md` (parked): "Greatsword-specific hits and block, taunt"; ask Kort what comes next.
