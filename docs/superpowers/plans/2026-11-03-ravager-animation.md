# Ravager (tc_brute) Charge and Bite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Kraken-touched Brute a troop model on the hound rig and two authored attacks, `ravager_charge` and `ravager_bite`, with horn and jaws leading the move at every key. Kort approves each in game.

**Architecture:** Clip data on the `hound` rig, family folder `clips/hound/ravager/`, built from `art/clips/hound/ravager.bbmodel`. Attacks are additive offsets over the stance (first and last key of every channel 0); no leg is keyed (the gait runs underneath). Before any of that, the troop must be drawn by our model, not Minecraft's ravager renderer (Task 0-art), and a four-legged gait must exist (Task 0-Java).

**Shared work:** the hound plan (`docs/superpowers/plans/2026-11-03-hound-animation.md`, Task 0) and the bear plan (`2026-11-03-bear-animation.md`, Tasks 0 and 1) need the same quadruped gait and the same `anim_edit.py` rig parameter. **Whichever family lands first builds those; the others skip them** (this plan then keeps only Task 0-art for the Brute's own model and horn). Check `git log` and the other two plans before starting Task 0-Java or Task 1.

**Tech Stack:** `tools/anim_edit.py` (open/apply), `tools/clips.py`, `tools/build_skins.py`, scratchpad tools (below), `test-tools/work/t16r12/live/run.sh`, `build_compare.py` at localhost:8771.

**Spec:** `docs/superpowers/specs/2026-11-03-ravager-animation-design.md`

Scratch tools live in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (`pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`). If missing, recreate them from the knight plan (`docs/superpowers/plans/2026-11-02-knight-swings.md` Task 1).

## Global Constraints

- Attack rows are fixed: `ravager_charge` body blunt impact tick 10 (0.50 s); `ravager_bite` head stab impact tick 8 (0.40 s). Clip length at most `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 (2.0 s). Planned 1.30 s and 1.00 s. Any change needs Kort.
- Legs are not keyed in either attack. If a later pass keys them it must be an offset over the gait, with a render check that the gait does not fight the key.
- Sign conventions (spec "Conventions"): +x pitches the front down; forward is -z; root y + is down, root z - is forward; jaw +x opens. Verify against shipped clips in Task 1 before keying.
- Every key states where the horn or jaws point. A key without that is a defect.
- Clip JSON is generated from `art/clips/hound/ravager.bbmodel` via `tools/anim_edit.py open/apply`. Never hand-author JSON. Do not run `art/clips/build_clips.py`.
- `python tools/clips.py` then `--verify`; `Spec.java` changes only through `tools/preflight.py`. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. (Expected `git status` always shows these two as already modified plus this plan's files.)
- Game verification: `python tools/verify.py game --offline` (refuses while a Minecraft client is open).
- At most 2 Minecraft instances; Kort's client counts. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No eula.txt. No computer-use on Minecraft; drive clients via the harness.
- No push, publish or merge. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Ask Kort with AskUserQuestion; each attack is approved and committed on its own.

## Review Focus

1. **Horn or jaws face the move at every key**, from front, 3/4 and side at 8 angles: charge horn forward and 10 degrees down at impact (chain +10 from idle), up on the toss; bite snout at head height, jaws clamping at 0.40 s.
2. **Start and end at zero** on every keyed channel; no pop into or out of the stance.
3. **Horn into the ground / head into the hump / jaw into the chest** at the extremes (charge 0.22 and 0.62, bite 0.20).
4. **Hit over attack** with this family's own fixture (the shipped gate does not play ravager clips), 15 deg/tick limit in ticks 26-35.
5. **Legs:** no leg is keyed; the lunge is root, body, chest, head and jaw. The gait must keep running through an attack. If Task 0-Java is not done, no attack can be judged and the plan stops there.

---

### Task 0-art: the Brute's model (NEEDS KORT / COORDINATOR; owner Kort or the coordinator's art worker)

**Files:**
- Create: `art/troops/tc_brute.json` (`troop`, `rig: hound`, `bones` overrides with the ravager's pivots, boxes and scale; `parts` for the horn on `head`; schema in the `tools/build_skins.py` header, MODEL_KEYS). Skin and UVs from the vanilla `minecraft:textures/entity/illager/ravager.png` layout.
- Modify: nothing in the RIGS maps (`ravager` is already `hound` in `tools/build_skins.py` and `TroopModels.RIGS`) unless Kort picks a separate rig.
- Generated: `models/troop/tc_brute.json`, `models/troop/index.json`.

- [ ] **Step 1:** Ask Kort (AskUserQuestion) spec Questions 1-2. Expected: a written answer; stop without it.
- [ ] **Step 2:** Build the model, then `python tools/build_skins.py`. Expected: exit 0; `grep -c tc_brute src/main/resources/assets/gravesandglory/models/troop/index.json` prints `1`; a harness spawn of `tc_brute` returns a `TroopModel` (not the vanilla renderer); record `rootScale()` (formula (24 - ravager root pivot y) / (24 - hound root pivot y); expected about 2.5x for 0.85 to 2.2 blocks; then re-derive the root px keys in Tasks 2-3) and the head pivot height (used for the horn-height recheck in Task 2).

### Task 0-Java: four-legged gait (NEEDS KORT / COORDINATOR; shared with the hound and bear plans, build once)

**Files:** Modify `src/main/java/gg/gravesandglory/MotionBody.java` and `MotionFeet.java` (today they write only `right_leg`, `left_leg`, `right_shin`, `left_shin`: `MotionBody.java:96-100`, `MotionFeet.java:25`). Create a harness check in `test-tools/standalone/`.

- [ ] **Step 1:** Prove the gap with a harness: build a hound-rig `Family` (the Task 4 fixture without attacks), drive `limbAngle`/`limbSpeed` for 60 ticks, print the rotation range of `front_left_leg`, `front_right_leg`, `back_left_leg`, `back_right_leg`. Expected before the fix: all four ranges 0.0 deg. Expected after: each at least 15 deg peak to peak, diagonal pairs (front_left with back_right) in phase and the other pair in antiphase, and `front_left_leg` returns to its start within 0.5 deg over one full cycle.
- [ ] **Step 2 (forelegs):** `front_left_leg` and `front_right_leg` are children of `chest` in `art/rigs/hound.json`, so MotionFeet/MotionBody must add -(chest x) to both front legs each tick. Expected: with a chest x of +14 held and the gait stopped, the front-foot world position (rotation sum of chest + front leg) changes by under 0.5 deg; during the charge, front-foot slide over a stance phase stays under 0.5 px (contact sheet).
- [ ] **Step 3:** With an attack playing (the Task 4 driver), Expected: the four legs keep cycling (peak to peak still at least 15 deg) while the attack track is active, i.e. the attack does not take over the legs.

### Task 1: Project, tools, sign check (depends on Task 0-art step 2; first commit includes the project) (NEEDS COORDINATOR for tools/)

`tools/anim_edit.py` is biped-only: `RIG = 'biped'` (line 26), `SRC = art/clips/biped` (line 27), `semantic()` loads `art/rigs/<RIG>.json` (line 90), `cmd_open` and `cmd_apply` read `SRC / (family + '.bbmodel')` (lines 108, 141), and `TROOP` (line 29) lists sword_shield, bow, common. `open ravager` exits 2 today. Adding a TROOP entry alone is not enough; `mesh('tc_brute')` also needs `art/troops/tc_brute.json` (Task 0-art), because it builds the geometry from the troop json. (If the hound or bear plan already parameterised the rig, reuse it.)

**Files:**
- Modify: `tools/anim_edit.py`: replace the module constants by a per-family map, e.g. `FAMILY = {'sword_shield': ('biped', 'knight'), 'bow': ('biped', 'archer'), 'common': ('biped', 'knight'), 'ravager': ('hound', 'tc_brute')}`; `src_path = ROOT / 'art/clips' / rig / (family + '.bbmodel')` in `cmd_open` and `cmd_apply`; `semantic(project, rig)` loading `art/rigs/<rig>.json`.
- Create: `art/clips/hound/ravager.bbmodel`, a **fresh hound-rig project** (not a copy of the biped one: `tools/clips.py:137` rejects any animated bone not in the rig's bone list, `art/rigs/hound.json`). Command (scratch `make_rig_project.py`, create it from this):
  `python -c "import sys,json,pathlib;sys.path.insert(0,'tools');import build_skins as b;root=pathlib.Path('.').resolve();pathlib.Path('art/troops/hound_rig.json').write_text(json.dumps({'troop':'hound_rig','rig':'hound'}));b.troop_project(b.load(root),'hound_rig',dest=pathlib.Path('work/anim-edit/hound_rig.bbmodel'))"`, delete `art/troops/hound_rig.json` after. This makes groups for exactly the hound.json bones (root, body, chest, head, jaw, four legs, tail), no skin. A script then loads that project, sets `textures` to `[]` (the committed bbmodels hold the bare rig: biped has 0 textures), `name` to `ravager clips`, and `animations` to the two stub animations: for each of `ravager_charge` (length 1.30, impact 0.50) and `ravager_bite` (length 1.00, impact 0.40) a dict like the `sword_shield_thrust` animation in `git show HEAD:art/clips/biped/sword_shield.bbmodel` (`name`, `loop: once`, `length`, `snapping: 20`, an `animators` map with an `Effects` animator of `type: effect` holding one `timeline` keyframe `{script: impact}` at the impact time, and a new `uuid`). **UNVERIFIED; dry-run first** (write to `work/`, do not commit). `troop_project` reads `cat.stages[troop]` and calls `merge_bones` (`build_skins.py:3017-3018`), so an empty troop json will likely raise KeyError on the stage lookup or a merge error. Failure also looks like a project whose groups are not exactly the hound.json bones, which `tools/clips.py:137` then rejects ("animated bone not in rig"). **Stated fallback, used if the dry-run raises or `clips.py` complains:** write the groups from `art/rigs/hound.json` by hand in the same layout as the biped file (a `model` group turned 180 degrees about x around one group per bone, one element per box).
- Create stubs (zero motion): `ravager_charge`, `ravager_bite` (this is the same file; `tools/clips.py` fails if a melee stab/slash/blunt row lacks a clip and both rows are melee).
- Scratch: extend `swing_ends.py` for impact markers 0.5 (charge) and 0.4 (bite) and `pose_preview.py` with `--over idle` for the hound rig.

- [ ] **Step 1:** `python tools/anim_edit.py open ravager`. Expected: before the edit prints usage and exits 2; after the edit exits 0 and prints `anim_edit: wrote work/anim-edit/ravager-tc_brute.bbmodel (2 clips)`.
- [ ] **Step 2:** `python tools/clips.py` then `python tools/clips.py --verify`. Expected: writes `src/main/resources/assets/gravesandglory/clips/hound/ravager/ravager_charge.json` and `ravager_bite.json`; `--verify` passes (impact on tick 10 and 8, length at most 40 ticks). With one stub removed: FAIL naming the missing id. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` if they changed.
- [ ] **Step 3:** Sign check against a shipped clip: print `sword_shield_overhead.json` root z at 0.5 s. Expected: -1.6 (forward is negative); `hit_head` head x at 0.07 s. Expected: -8 (up and back is negative). Preview `chest x +10 head x +8` on the built model at 3/4. Expected: the snout dips; if it rises, flip the signs in the spec tables before keying.
- [ ] **Step 4:** Stances. Key `idle`, `carry`, `guard`, `rest` with the spec's Stances table (root y, body, chest, head, jaw; absolute chain pitch 0, 0, +5, +18). `swing_ends.py` does not apply (stances are not additive); instead `python tools/clips.py --verify` must pass the loop-closing check (a looping clip ends where it starts within 0.01). Expected: pass; preview shows idle horn level, guard horn 5 degrees down, rest head low.
- [ ] **Step 5:** Commit tools, project, stubs and stances only when Kort OKs Task 0. Message: `Add ravager clip project, stances and stub attacks` + trailer.

### Task 2: ravager_charge (reference gim_rhino 0:05.5-0:10.5 and the head lifting mid-trot from 0:11.8)

**Files:** `art/clips/hound/ravager.bbmodel` (animation `ravager_charge` only); generated `clips/hound/ravager/ravager_charge.json`.

**Provisional until spec Question 3 (barge vs ram).** Root px values are re-derived after Task 0-art (rootScale = (24 - ravager root pivot y) / (24 - hound root pivot y), about 2.5x for 0.85 to 2.2 blocks).

Key table (spec "Moves"; deltas on the idle stance; chain pitch = body x + chest x + head x; legs are not keyed):

| t | root | body x | chest x | head x | jaw x | chain | horn points |
|---|---|---|---|---|---|---|---|
| 0.00 | 0,0,0 | 0 | 0 | 0 | 0 | 0 | forward, level |
| 0.22 | 0,+1.5,+1.0 | -4 | -10 | -16 | +6 | -30 | up and forward (head lift) |
| 0.40 | 0,+1.0,-1.2 | +1 | +3 | +2 | 0 | +6 | swinging down to level |
| **0.50** | 0,+0.6,-2.4 | +2 | +5 | +3 | 0 | **+10** | **forward, 10 degrees down, at chest-to-waist height** |
| 0.62 | 0,0,-1.6 | -2 | -14 | -22 | +10 | -38 | flung up, rising and forward (toss) |
| 0.90 | 0,+0.3,-0.6 | 0 | -4 | -4 | 0 | -8 | easing level |
| 1.30 | 0 | 0 | 0 | 0 | 0 | 0 | forward, level |

- [ ] **Step 1:** Write the keys with `anim_edit.py open ravager` then `apply`, `impact` marker at 0.50. Run `swing_ends.py ravager_charge`. Expected: `OK` (first and last keys 0, impact 0.5).
- [ ] **Step 2:** `pose_preview.py ravager_charge --over idle` at 0.00, 0.22, 0.40, 0.50, 0.62, 0.90, front, 3/4, side. Expected checklist: horn angle matches the chain column within 5 deg (compute from the summed chain, not by eye); at 0.50 the horn is forward and 10 degrees down at chest-to-waist height (measure with the head pivot height from Task 0-art), not into the ground; at 0.62 the head is above the shoulder hump and the horn still points forward-up; the head does not pass through the hump or chest.
- [ ] **Step 3:** `python tools/clips.py`, `python tools/clips.py --verify`. Expected: both exit 0; `impact` on tick 10; length 26 ticks.
- [ ] **Step 4:** Render: `GRAVES_GRADE_CLIPS=ravager_charge,idle test-tools/work/t16r12/live/run.sh solo grade tc_brute`; key frames at 8 angles; add the compare page (`build_compare.py`, localhost:8771) with the rhino frames `frames/run` (gallop) and `frames/rear` (head lifts mid-trot from 0:11.8) beside it. Expected checklist: horn forward and level to 10 degrees down during 0.40-0.55; toss visibly rises after impact; no pop at the first and last frame; body reads as a heavy beast, not a hound; legs keep cycling through the swing and no foot slides (contact sheet).
- [ ] **Step 5:** Ask Kort in game (AskUserQuestion, short). On OK: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`, commit the bbmodel and JSON: `Add ravager charge animation` + trailer. Expected: `git status` shows only the two pre-existing modifications and the plan docs.

### Task 3: ravager_bite (no horned-beast reference; hound wolf bite for the snap)

**Files:** same bbmodel (animation `ravager_bite` only); generated `ravager_bite.json`.

| t | root | body x | chest x | head | jaw x | chain | jaws/horn point |
|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | forward, level, jaws shut |
| 0.20 (authored, no reference) | 0,+0.4,+0.8 | -2 | -10 | x -14 | +38 | -26 | snout up and back, jaws gaping, horn up |
| 0.32 | 0,+0.2,-1.2 | 0 | +2 | x +1 | +40 | +3 | snout forward at the target's head, jaws wide |
| **0.40** | 0,+0.4,-2.0 | +1 | +2 | x -7 | 0 | **-4** | **snout forward, a touch up, at head height, jaws clamping** |
| 0.50 | 0,0,-1.6 | +1 | 0 | x -3, y +12 | +4 | -2 | jaws held, snout swinging right |
| 0.58 | 0,0,-1.2 | 0 | 0 | x -2, y -12 | +4 | -2 | jaws held, snout swinging left |
| 0.70 | 0,0,-0.5 | -1 | -6 | x -10 | +14 | -17 | pulling up and back, jaws opening |
| 1.00 | 0 | 0 | 0 | 0 | 0 | 0 | forward, level |

- [ ] **Step 1:** Keys, `impact` marker 0.40, `swing_ends.py ravager_bite`. Expected: `OK`.
- [ ] **Step 2:** Preview at the 8 key times. Expected checklist: jaw opens +38 to +40 before 0.32 and is shut at 0.40 (a snap of 0.08 s); snout at target head height at impact; the head shake keeps the jaw clamped; the jaw never passes through the chest.
- [ ] **Step 3:** `python tools/clips.py` and `--verify`. Expected: pass; impact tick 8; length 20 ticks.
- [ ] **Step 4:** Render and compare as Task 2 with `GRAVES_GRADE_CLIPS=ravager_bite,idle`. Expected: snap visible from the side; no pop; the 12 degree shake reads from the front.
- [ ] **Step 5:** Kort approves in game, then commit `Add ravager bite animation` + trailer.

### Task 4: Family hit-over-attack check

The shipped `MotionEngineCheck.hitOverAttack` (~line 1093) and `hitWhileAttacking` (line 1057) run the `realSwordShield` fixture only (line 463; it loads `CLIPS = ".../clips/biped/"` dirs `common` and `sword_shield`, and `hitWhileAttacking` hard-wires `d.shield = true`); they never play ravager clips. Hits are deferred (spec), so this check uses the engine's procedural hit unless Kort later adds hit clips.

**Files:**
- Create: `test-tools/standalone/RavagerHitOverAttackCheck.java`, package `gg.gravesandglory`, a `main` that:
  1. loads every `*.json` in `src/main/resources/assets/gravesandglory/clips/hound/common/` (if it exists) then `.../hound/ravager/` through `Clip.parse(Files.readString(p))` into a map by `c.name()` (copy the `realSwordShield` loop at MotionEngineCheck.java:464-471 with `CLIPS` changed to `.../clips/hound/`);
  2. builds `stances` from `idle, carry, guard, rest`, `attacks` = `[ravager_charge, ravager_bite]` (the order of `Spec.ATTACKVARIANTS` for family `ravager`), `wall` empty (`new MotionEngine.Family("ravager", stances, attacks, new ArrayList<>(), hits, deaths)` with empty `hits` and `deaths`, copying the last lines of `realSwordShield`);
  3. (`MotionEngine.Family(String name, stances, attacks, wall, hits, deaths)`; `MotionInput.Attack(int index, long tick)`) for attack index 0 and 1 runs the `hitWhileAttacking` loop with a `Drive` (`MotionEngineCheck.Drive` is a static nested class; copy it or make the new class a nested one) set up as `d.stance = "guard"`, `d.shield = false`, `d.enemy = true`; attack `new MotionInput.Attack(index, d.tick())` at frame 20, `d.hit("body", 0, 1, false, false)` with `for (MotionMath.Seed seed : seeds(4, 4100))` as `MotionEngineCheck.hitOverAttack` (line 1090) does, passing `double[] keep = new double[4]` and `check(keep[1] == 1 && keep[2] == 1, ...)` at frame 25, 60 frames at `d.step(0.05)`; the step is the sum over bones and components of `|cur - prev|`; run once with and once without the hit; for each seed in the same seed set `hitOverAttack` uses (read lines 1090-1097);
  4. prints `attack <i>: keep[1]=<n> keep[2]=<n> worst step <x> deg/tick` and `check(worst <= 15)`; and a line `attack <i> alone: peak step <x> deg/tick` (no hit) per attack. Note the bite's jaw +40 to 0 in 0.08 s is about 24 deg/tick by itself, so the alone-peak may exceed 15; the 15 limit is on the hit's added difference, not the clip's own peak.
- Window: ticks 26-35 are attack ticks 6-15 (0.30-0.75 s), so both impacts (0.50 s, 0.40 s) are inside it.

- [ ] **Step 1:** Compile and run with the scratch runner: `EXTRA=test-tools/standalone/RavagerHitOverAttackCheck.java MAIN=RavagerHitOverAttackCheck bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh` (recreate `mec.sh` from the knight plan if missing: javac `--release 17` on the listed `src/main/java/gg/gravesandglory/*.java` plus the test files, then `java -cp <out>;<gson jar> gg.gravesandglory.$MAIN`). Expected: `attack 0: worst step <= 15.0 deg/tick` and `attack 1: worst step <= 15.0 deg/tick`, exit 0, and the `keep[1] == 1` line (the attack is the only track after the hit) true for both.
- [ ] **Step 2:** If one fails, lengthen the hit ease-in or shrink the procedural hit gain; re-run. Expected: both at most 15.0 without editing the attack clips.

### Task 5: All troops in the family and the game

Family = `tc_brute` only.

- [ ] **Step 1:** Final solo grade renders of `tc_brute` for both attacks at 8 angles plus a walk-to-attack chain (swing into swing, the pop flag reads "no"). Expected: horn/jaw directions as the tables, scale and `rootScale` correct (horn not in the ground), no leg slide.
- [ ] **Step 2:** `python tools/clips.py --verify`, then `python tools/verify.py game --offline`. Expected: both exit 0; no Minecraft client open. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 3:** `git status`. Expected: only the two pre-existing modifications, the untracked docs from the other family agents, and nothing of the ravager's left unstaged after its commits.
- [ ] **Step 4:** Final commit if anything is left. Hand to Kort for the game review. Do not push.
