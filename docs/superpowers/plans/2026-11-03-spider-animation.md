# Spider (Web Stalker) Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Web Stalker (`wm_stalker`, rig `spider`, family `spider`) its stances `idle`, `guard`, `carry`, the attacks `spider_bite` and `spider_lunge`, hits and a death, with the fangs and front legs always facing the target on every key, no clipping and no blend pops. Kort approves each clip in game.

**Architecture:** Clip data only: new folder `src/main/resources/assets/gravesandglory/clips/spider/spider/` (the engine reads `clips/<rig>/common/` then `clips/<rig>/<family>/`; there is no `spider/common`). Source of truth is a new `art/clips/spider/spider.bbmodel` (one animation per clip). An action clip is an offset over the stance, added to the rig's rest pose; the first and last keys of every keyed channel are 0; the step is a root offset only. **The spider has no troop model, so nothing plays until Task 0 is done by the coordinator and Kort**: today `wm_stalker` draws as the vanilla spider.

**Spec:** `docs/superpowers/specs/2026-11-03-spider-animation-design.md` (rig, sign conventions, key tables).

## Global Constraints

- Impact markers fixed: `spider_bite` at 0.40 s (tick 8, body, stab), `spider_lunge` at 0.40 s (tick 8, body, blunt). Clips at most 40 ticks (planned 1.0 s and 1.1 s). Any timing change needs Kort first.
- Weapon direction (docs/ANIMATION-EDITING.md): every key states where the fangs and front legs point (spec tables): the fangs lead at the target's chest on the bite, the front legs and body lead on the lunge. A clean pose with them aimed at the ground or away fails. Check wind-up, impact, follow-through, recovery from front, 3/4 and side.
- `python tools/clips.py` then `python tools/clips.py --verify` must pass. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- No gameplay, save or network change; no Java or sheet edits (Questions 1 to 4 in the spec are Java, tooling or art and need Kort or the coordinator).
- Clip JSON is generated from the bbmodel; edit through `tools/anim_edit.py open/apply`, never hand-author JSON. `clips.py` fails if the family lacks a clip for any MELEE attack id: until both `spider_bite` and `spider_lunge` exist, plan a zero-motion stub (impact marker 0.40, first and last key 0) for the other.
- At most 2 Minecraft instances; Kort's client counts. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No computer-use on Minecraft. No eula.txt.
- No push, publish or merge. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Ask Kort with AskUserQuestion, short and plain; each clip is approved and committed on its own.
- Ground clearance: the spider has no foot planting, so every planted tip (legs 2, 3, 4) must be within 0.5 px of the rest-pose sole (y 23.71, `clips.points(rig, {})`) on every key; `TroopModel.rotation` is Rz·Ry·Rx, so no hand formula: use the solved rolls in the spec tables and re-measure with `clips.points` in the pose preview. Death is exempt (root y solved to the ground).
- Expected `git status` throughout: the already-modified `sheets/preflight.json` and `Spec.java`, the already-untracked files listed at the start, plus only the files each task names.
- Scratch tools are in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (`pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`); recreate from the knight plan if missing. `pose_preview.py` reads `art/rigs/biped.json` and a sword_shield bbmodel (line 17 and 23) and draws a sword and shield: for the spider copy it to `pose_preview_spider.py` with the rig path `art/rigs/spider.json`, the bbmodel `art/clips/spider/spider.bbmodel`, the colour map keyed by spider bones and no sword or shield.

## Review Focus

1. **Fangs and front legs point as the spec says on every key** (spec tables): coil with the face up, snap forward and about 30 deg above level at the bite's impact, front legs 31 deg above level (the bite is an exaggerated rear-up and does not aim at the chest), 17 deg above level on the lunge (inside the 15 to 20 deg chest line), each tip 25 deg outside the facing; judge the bite by head and thorax heading and fang-box travel (more than 3 px forward and up between 0.30 and 0.40), not fang angles; check at 8 angles.
2. **No clipping:** front legs never cross the head (total yaw at most 65), the abdomen never into the thorax, planted legs 2-4 within 0.5 px of the rest sole (y 23.71) on every key (spec Ground clearance rule, solved with `clips.points`); fix in the clip keys only.
3. **Start and end in the stance:** every keyed channel's first and last key is 0 (`swing_ends.py`); both attack impacts on tick 8 and the sheet tick.
4. **Sign conventions** from the spec (thorax pitch, leg yaw and roll, fang pitch) confirmed on the first preview and render, not assumed.
5. **Hit over attack:** the stock gate never plays this family; the spider copy (Task 7) must stay at or under 15 degrees per tick in the window.

---

### Task 0: Preconditions (blocking, coordinator and Kort)

**Files (coordinator):** `art/troops/wm_stalker.json`, `art/troops/wm_stalker.bbmodel` (and faces png), generated `src/main/resources/assets/gravesandglory/models/troop/wm_stalker.json` and `index.json`, `tools/anim_edit.py`, `art/clips/spider/spider.bbmodel` (needs coordinator), the stub clips `spider_bite` and `spider_lunge` (needs coordinator), `test-tools/work/t16r12/live/run.sh`.

- [ ] **Step 1:** Kort answers spec Question 1 (renderer swap) and Question 2 (walk). Without a yes on 1, stop: no clip can show in game.
- [ ] **Step 2:** Add the troop model (**coordinator**): the source is `art/troops/wm_stalker.json` plus `art/troops/wm_stalker.bbmodel` (rig `spider`, texture `minecraft:textures/entity/spider/spider.png`; follow `art/troops/archer.*`); then run `python tools/build_skins.py`, which writes `models/troop/wm_stalker.json` and adds the `index.json` entry; then `python tools/preflight.py` (check at `tools/preflight.py` line 216: the index `troops` list must equal the `art/troops` ids). Check: `grep -c wm_stalker src/main/resources/assets/gravesandglory/models/troop/index.json`. Expected: 1, the model JSON exists, and preflight prints no `models/troop/index.json` error. Afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` if preflight rewrote them.
- [ ] **Step 3:** Tooling (**coordinator**): `tools/anim_edit.py` hard-codes `RIG = 'biped'` and `SRC = ROOT / 'art/clips' / RIG` (lines 26-27), `semantic()` loads `art/rigs/biped.json` (line 90) and `apply` copies to `art/clips/biped`. Generalise RIG and SRC so they come from the family (the spider family uses rig `spider`, source `art/clips/spider`), add `'spider': 'wm_stalker'` to `TROOP` (line 29), and create `art/clips/spider/spider.bbmodel` from the real `art/rigs/spider.json` (one group per bone, the display group `model`), not from `git show HEAD:art/clips/biped/sword_shield.bbmodel`, whose groups are biped bones. Check: `python tools/anim_edit.py open spider`. Expected: exits 0 and writes a file under `work/anim-edit/` with the eight legs, thorax, head, fangs and abdomen; the existing `open sword_shield` still exits 0.
- [ ] **Step 4:** Render harness (**coordinator**): add to `test-tools/work/t16r12/live/run.sh` beside `solo_knight`: `solo_stalker) go solo_stalker GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=spider GRAVES_GRADE_TROOP=wm_stalker GRAVES_GRADE_CLIPS=spider_bite,spider_lunge,guard GRAVES_GRADE_OUT=$B/solo_stalker/grade GRAVES_GRADE_PARTS=solo ;;`
- [ ] **Step 5:** Recreate the scratch tools if missing (see Global Constraints). Expected: `ls` shows them and `python pose_preview_spider.py --help` exits 0.

### Task 1: Stance `guard` (and the sign check)

**Files:** `art/clips/spider/spider.bbmodel` (`guard`); generated `clips/spider/spider/guard.json`. **Needs coordinator (Task 0 Step 3):** `spider.bbmodel` itself (built from `art/rigs/spider.json`) and the two stub clips `spider_bite`, `spider_lunge` (zero motion, impact marker 0.40, length 1.0 and 1.1 s) that let `clips.py` pass until Tasks 3 and 4; the worker authors only `guard` here.

Target and direction: loop 2.0 s, spec `guard` row: root y +1.8, thorax x -4, head x -6, fangs x -10 (abs chain -20: tips hang 20 deg forward of straight down, 70 deg below level), legs 1 left yaw +14 roll -14 (mirror right), leg 2 yaw +5 roll -5.3, leg 3 roll -10.2, leg 4 roll -17.6 (solved: planted tips within 0.02 px of the rest sole 23.71; L1 2.6 px above), abdomen x +6; breathing +-1 deg; first key equals last.

- [ ] **Step 1:** Write the keys from the spec. Before relying on any other number, apply one probe in Blockbench or the preview: thorax x -20 (expect nose up), left leg 1 yaw +20 (expect the tip forward), roll -20 (expect it lifts), fangs x -40 (expect tips forward). Expected: all four as the spec's sign list; record the actual signs in the ledger. **If any differs, fix the sign list in the spec before continuing.**
- [ ] **Step 2:** `python tools/clips.py` then `python tools/clips.py --verify`. Expected: no errors; only `guard.json` and the two stubs added.
- [ ] **Step 3:** `python pose_preview_spider.py guard out.png 0 --over guard` front, 3/4, side. Expected checklist: the front legs forward and raised, the face toward the target, fangs hanging 20 deg forward of down; the front legs clear the head from the top; **ground clearance: the lowest tip of each of legs 2, 3, 4 (both sides) is within 0.5 px of the rest sole 23.71 (print `clips.points` lows at keys 0 and 1.0 and compare with the rest pose; solver: 0.02), legs 1 at or above it**; also run it for `idle`.
- [ ] **Step 4:** Render `GRAVES_GRADE_CLIPS=guard bash test-tools/work/t16r12/live/run.sh st1 solo_stalker`. Expected checklist: the Web Stalker draws as the custom model (not vanilla); the stance loop closes; no leg in the head or through the ground at 8 angles; the feet do not hover more than 0.5 px.
- [ ] **Step 5:** Compare page "Guard v1" (`build_compare.py`, localhost:8771): the render only (front, 3/4, side); there is no video stance frame to compare against (the footage is first-person). Ask Kort; commit: "Add the Web Stalker guard stance".
- [ ] **Step 6 (engine smoke test, scratch, no repo change; moved here from Task 0):** with `guard.json` and the stubs in place, build a Family from `clips/spider/spider`, call `MotionEngine.pose` for 200 frames on the spider model with idle, guard and an attack. Expected: no exception, no NaN; bones missing from the spider (`right_arm`, `right_leg`) are skipped. If an exception or a non-zero bone appears, report to the coordinator (spec Engine gaps item 3) and stop.

### Task 2: `idle` and `carry`

**Files:** `spider.bbmodel` (`idle`, `carry`); `idle.json`, `carry.json`. Values from the spec Stances table.

- **`idle`** (loop 2.6 s, absolute values): crouched wait, root y +1.0, thorax x +2, leg 1 roll -12.4 (yaw 0), leg 2 roll -5.4, leg 3 roll -3.6, leg 4 roll -4.4 (planted tips within 0.01 px of the rest sole 23.71, solved), abdomen breath +-2, thorax yaw +-1 (no thorax roll), one-leg feeler twitch (leg 1 yaw +-4) every 1.3 s. Fangs hang (chain -4: nearly straight down), head faces forward.
- **`carry`** (loop 0.6 s, pending Question 2): alternating tetrapod gait, legs L1,R2,L3,R4 then R1,L2,R3,L4; swing yaw +-14, mid-swing lift roll -10 (right legs mirrored; a swinging leg is exempt); planted legs hold idle's rolls; no root bob (a 0.8 px bob sinks tips 0.8 px); thorax roll +-1.5 following the planted side (0.42 px at most). Fangs hang.

- [ ] **Step 1:** Write the keys; loops close (first key equals last key).
- [ ] **Step 2:** `python tools/clips.py`, `--verify`. Expected: no errors; `idle.json`, `carry.json` added.
- [ ] **Step 3:** Pose preview of both over `guard`: planted tips within 0.5 px of the rest sole at every key. Render `GRAVES_GRADE_CLIPS=idle,carry` (add the loco part if Question 2 chose the clip route). Expected: legs move in alternation, each leg swings on its own side without crossing a neighbour, the loop closes, no leg through the ground, body bob visible; if the legs stand still while the spider moves, record Question 2 and stop.
- [ ] **Step 4:** Compare page; ask Kort; commit: "Add the Web Stalker idle and carry".

### Task 3: `spider_bite` (spec Moves, video 0:23.9-0:24.5)

**Files:** `spider.bbmodel` (`spider_bite`); `spider_bite.json`. Length 1.0 s, impact 0.40.

Video mapping (one, piecewise): 23.9 to 24.1 s = clip 0.00 to 0.20 (1x); 24.1 to 24.3 = 0.20 to 0.30 (2x); 24.3 to 24.5 = 0.30 to 0.40 (2x). Frames `f_010` to `f_016` (`f_012` and `f_016` are silhouette only). Deltas over guard (guard abs: thorax -4, head -6, fangs -10, chain -20); chain = thorax + head + fangs absolute, 0 = straight down, negative = tips forward, -90 = level. Leg rolls are the solved ones (spec; mirror right); rear yaws stay 0.

**Chest line.** The target's chest is about 15 to 20 deg above level from the thorax. The bite does **not** aim at it: it is an exaggerated rear-up (front legs 31 deg above level at impact), stated as such in the spec; Question 3 asks Kort.
- **Start (0.00):** guard; fangs 20 deg forward of down, front legs forward.
- **Coil and rear-up (0.20):** root (0,+0.5,+2.0); thorax -16 (abs -20), head -6 (abs -12), fangs +60 (abs +50), chain +18; L1 yaw +4 roll -40; L2 yaw +4 roll +12.2; L3 roll -10.0; L4 roll -21.7; abdomen +8; **fangs cocked 18 deg back of straight down under the chin (not forward), face lifted 32 deg, front legs raised overhead and splayed**.
- **Peak (0.30):** root (0,0,+1.0); thorax -22 (abs -26), head -10 (abs -16), fangs +62 (abs +52), chain +10; L1 yaw +4 roll -50; L2 roll +18.2; L3 roll -10.4; L4 roll -26.3; **face lifted 42 deg, fangs cocked 10 deg back of down (loaded), front legs high**.
- **Impact (0.40, fang snap):** root (0,+0.5,-4.0); thorax -28 (abs -32), head -14 (abs -20), fangs -58 (abs -68), chain -120; L1 yaw +6 roll -30; L2 roll +23.0; L3 roll -15.5; L4 roll -37.6; abdomen +10; **face nose-up 52 deg, fangs forward 30 deg above level, front legs forward 31 deg above level (10 to 15 deg above the chest line), each tip 25 deg outside the facing (50 deg V); tolerance front legs within 8 deg of 31 deg, lateral 30 deg per leg**.
- **Hold (0.55):** root (0,+0.8,-2.5); thorax -20, head -8, fangs -22 (chain -70); L1 yaw +2 roll -14; L2 roll +10.6; L3 roll -13.3; L4 roll -28.8; **fangs 20 deg below level, easing down**.
- **Recovery (0.75):** root (0,+0.4,-0.8); thorax -6, head -2, fangs -4 (chain -32); L1 roll -4; L2 roll +1.6; L3 roll -4.5; L4 roll -9.1; **fangs 58 deg below level, falling toward guard**. **End (1.00):** all 0, guard.

- [ ] **Step 1:** Write the keys from the spec table; impact marker 0.40; every keyed channel 0 at both ends.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `python swing_ends.py spider_bite`. Expected: no errors, OK, only `spider_bite.json` changed, impact on tick 8 and on the sheet tick.
- [ ] **Step 3:** `python pose_preview_spider.py spider_bite out.png 0 0.2 0.3 0.4 0.55 0.75 1.0 --over guard`. Expected: at 0.2 the face is up, the fangs tucked back under the chin and the front legs overhead; at 0.3 the same, face higher; at 0.4 face nose-up 52 deg, front legs forward about 31 deg above level (measured, within 8 deg) and each tip about 25 deg outside the facing; no front leg crosses the head; the abdomen clears the thorax; **legs 2, 3, 4 (both sides) within 0.5 px of the rest sole 23.71 at all seven keys** (the solver gives 0.02; print `clips.points` lows per key).
- [ ] **Step 4:** Render `GRAVES_GRADE_CLIPS=spider_bite,guard bash test-tools/work/t16r12/live/run.sh sb1 solo_stalker`; 8 angles at 0.2, 0.4, 0.55. Judge the bite by the head and thorax heading (face nose-up 32 deg at 0.2, 52 at 0.4) and by the fang-box travel between 0.3 and 0.4 (computed: 4.5 px forward and 3.6 px up in the world; each must be more than 3 px in the render), not by fang angles (the 2 px fangs are unreadable). Expected: rear legs slide at most 5 px along the ground; pop flag "no" for chained bites; the bite does not look like a nod.
- [ ] **Step 5:** Compare page "Bite v1" with `frames/rear_bite/f_010,f_012,f_014,f_016` beside the render (note on the page: the video shows the rear-up and leg strike only, f_012 and f_016 are silhouette only, the fang snap is ours); ask Kort; iterate.
- [ ] **Step 6:** `mec.sh` (no FAIL lines; the stock gate does not play this family, see Task 7); commit: "Add the Web Stalker bite".

### Task 4: `spider_lunge` (spec Moves, video 0:19.6-0:20.4)

**Files:** `spider.bbmodel` (`spider_lunge`); `spider_lunge.json`. Length 1.1 s, impact 0.40.

Video mapping (one, piecewise): 19.6 to 19.8 s = clip 0.00 to 0.20 (1x); 19.8 to 19.9 = 0.20 to 0.30 (1x); 19.9 to 20.2 = 0.30 to 0.40 (3x). Frames `f_001` to `f_007` (`f_004` is silhouette only). Deltas over guard; chain as in Task 3. Chest line: the target is 1.5 to 2 blocks away with its chest 1.2 to 1.4 blocks up, so about 15 to 20 deg above level from the thorax; the lunge aims at it. Leg rolls are the solved ones (spec).
- **Start (0.00):** guard.
- **Coil (0.20):** root (0,+3.0,+3.0); thorax +8 (abs +4), head +4 (abs -2), fangs +20 (abs +10), chain +12; L1 yaw -8 roll -21.5; L2 yaw -5 roll -17.5; L3 roll -9.5; L4 roll -7.7; abdomen +14; **fangs tucked 12 deg back of down, front legs folded back under the chin**.
- **Launch (0.30):** root (0,-3.5,-7.0); thorax -8 (abs -12), head -4, fangs -30 (chain -62); L1 yaw +6 roll -20; L2 yaw +14 roll -22; L3 yaw -10 roll -12; L4 yaw -14 roll -12; **airborne (lowest tips 4.3 px above the sole): front legs forward and level (1 deg above), fangs 28 deg below level**.
- **Impact (0.40):** root (0,+1.0,-12.0); thorax -14 (abs -18), head -6 (abs -12), fangs -40 (abs -50), chain -80; L1 yaw +6 roll -30; L2 yaw +18 roll +24.0; L3 roll -11.4; L4 roll -22.2; abdomen +12; **front legs forward 17 deg above level, inside the 15 to 20 deg chest line (tolerance 15 deg elevation), each tip 25 deg outside the facing, fangs forward 10 deg below level**.
- **Skid (0.55):** root (0,+1.8,-9.0); thorax -4, head -2, fangs -10 (chain -36); L1 yaw +2 roll -8; L2 yaw +10 roll -2.3; L3 roll -9.8; L4 roll -14.6; **front legs braking forward, fangs 54 deg below level**.
- **Settle (0.80):** root (0,+1.0,-3.0); thorax +2; L1 yaw -14 roll -4.8; L2 roll -5.8; L3 roll -3.6; L4 roll -3.5; **facing the target, fangs about 18 deg forward of down**. **End (1.10):** all 0, guard.

- [ ] **Step 1:** Write the keys; marker 0.40; ends 0.
- [ ] **Step 2:** `python tools/clips.py`, `--verify`, `swing_ends.py spider_lunge`. Expected: no errors, OK, only `spider_lunge.json` changed, impact tick 8.
- [ ] **Step 3:** `pose_preview_spider.py spider_lunge out.png 0 0.2 0.3 0.4 0.55 0.8 1.1 --over guard`. Expected: at 0.4 the front legs reach forward about 17 deg above level and each tip about 25 deg outside the facing; legs 2, 3, 4 (both sides) within 0.5 px of the rest sole 23.71 at 0.0, 0.2, 0.4, 0.55, 0.8, 1.1 (solver: 0.02; the 0.3 key is airborne: nothing needed); L1 at or above the sole (0.4 px above at 0.2 and 0.8); body low at 0.2.
- [ ] **Step 4:** Render `GRAVES_GRADE_CLIPS=spider_lunge,guard bash test-tools/work/t16r12/live/run.sh sl1 solo_stalker`; 8 angles at 0.2, 0.3, 0.4, 0.55. Expected: the root offset reads as a pounce of about 0.75 block, front legs aimed at the chest-to-belly within 15 deg at the impact frame, no leg through the ground at the landing; the spider ends in guard; if the cull box clips the model at the root offset, record it for the coordinator (the engine takes the root reach from the clip).
- [ ] **Step 5:** Compare page "Lunge v1" with `frames/lunge/f_001,f_004,f_007` beside the render (`f_004` is silhouette only); ask Kort (Question 4: root offset only); iterate.
- [ ] **Step 6:** `mec.sh`; commit: "Add the Web Stalker lunge".

### Task 5: Hits

**Files:** `spider.bbmodel`; `hit_body.json`, `hit_head.json`, `hit_legs.json`. Decision: no `_2` or `_n` variants in v1 (same in the spec); add mirrored `_2` clips only if Kort asks. Deltas over guard (guard abs: thorax -4, head -6, fangs -10, chain -20). The leg rolls are the solved ones (`clips.points`, mirror right): legs 2, 3, 4 within 0.02 px of the rest sole (23.71) at every key.

| Clip | Time (s) | Bones (deltas) | Abs thorax / head / fangs, chain | Fang and front-leg direction |
|---|---|---|---|---|
| `hit_body` | 0.10 | root z +2.0; thorax +10; fangs +20; L1 yaw -8 roll -5.6; L2 roll -4.9; L3 roll +4.8; L4 roll +13.7 | +6 / -6 / +10, +10 | fangs tucked 10 deg back of down, forward legs recoiling (L1 0.4 px above the sole) |
| | 0.25 | root z +0.8; thorax +4; fangs +8; L1 yaw -3; L2 roll -2.1; L3 roll +1.8; L4 roll +5.0 | 0 / -6 / -2, -8 | recovering (L1 1.3 px above) |
| | 0.50 | 0 | guard | toward the target |
| `hit_head` | 0.08 | thorax -8; head -26; fangs +30; L2 roll +5.0; L3 roll -3.6; L4 roll -8.9; abdomen +4 | -12 / -32 / +20, -24 | face up 44 deg, fangs 24 deg forward of down (L1 4.5 px above) |
| | 0.25 | thorax -3; head -10; fangs +12; L2 roll +1.8; L3 roll -1.2; L4 roll -3.5 | -7 / -16 / +2, -21 | face dropping back |
| | 0.50 | 0 | guard | toward the target |
| `hit_legs` | 0.10 | root y +2.5; thorax +6; L1 yaw -4 roll -15.0; L2 roll -14.4; L3 roll -8.3; L4 roll -7.3 | +2 / -6 / -10, -14 | body dropped 2.5 px, forward legs folded in (0.4 px above the sole), fangs 14 deg forward of down, facing the target |
| | 0.25 | root y +1.0; L1 roll +1.0; L2 roll -4.8; L3 roll -4.4; L4 roll -5.8 | -4 / -6 / -10, -20 | rising (L1 1.5 px above) |
| | 0.50 | 0 | guard | toward the target |

- [ ] **Step 1:** Write the keys; first and last 0 on every keyed channel; `clips.py` and `--verify`. Expected: no errors; three clips added.
- [ ] **Step 2:** Pose preview at each key. Expected: legs 2, 3, 4 (both sides) within 0.5 px of the rest sole 23.71 at every key (the solver says 0.02, `hit_legs` 0.10 included: L2 -14.4, L3 -8.3, L4 -7.3); L1 at or above the sole; then render the hits over guard (`GRAVES_GRADE_CLIPS=hit_body,hit_head,hit_legs`). Expected: each reads as a flinch, no leg through the ground on `hit_legs`, fangs not drawn through the head on `hit_head`.
- [ ] **Step 3:** Ask Kort; commit: "Add the Web Stalker hit reactions".

### Task 6: Death

**Files:** `spider.bbmodel` (`death`); `death.json`. 1.4 s. Death is exempt from the planted-tip rule (the body tips over); the root y of each key is solved so the lowest point of the model is on the rest sole (23.71), checked by `clips.py --verify` (within 0.1 px).

| Time (s) | Bones (deltas over guard, position px) | Abs thorax / head / fangs, chain | Fang direction |
|---|---|---|---|
| 0.00 | 0 | -4 / -6 / -10, -20 | guard |
| 0.30 | thorax roll +12; root y -3.3 (solved; leans onto the left legs, the right legs lift 6 to 7 px); legs at guard; fangs +10 | -4 / -6 / 0, -10 | back toward hanging |
| 0.70 | thorax roll +90; root y -6.4 (solved); all legs to roll +20 total (deltas L1 +34, L2 +25.3, L3 +30.2, L4 +37.6; mirror right); fangs +20 | -4 / -6 / +10, 0 | hanging down |
| 1.10 | thorax roll +180; root y +2.1 (solved); all legs to roll +50 total (deltas L1 +64, L2 +55.3, L3 +60.2, L4 +67.6; mirror right); fangs +30 | -4 / -6 / +20, +10 | 10 deg back of down (pointing up), legs curled |
| 1.40 | hold the 1.10 pose | same | same |

- [ ] **Step 1:** Write the keys, then `python tools/clips.py --verify`. Expected: the death-on-the-ground check passes (the lowest point within 0.1 px of the ground); if it fails at a key, move that key's root y in 0.1 steps until it passes.
- [ ] **Step 2:** Render `GRAVES_GRADE_CLIPS=death`. Expected: the spider rolls onto its back, legs curl, no leg or abdomen through the ground; the lying body does not rock after 1.4 s.
- [ ] **Step 3:** Ask Kort; commit: "Add the Web Stalker death".

### Task 7: Family hit-over-attack check

`MotionEngineCheck.hitOverAttack` (line 1093) runs `realSwordShield` only; `hitWhileAttacking` (line 1057) starts the attack at tick 20 and a hit at tick 25 and measures the window ticks 26 to 35 (attack ticks 6 to 15). It hard-wires `d.stance="hold"; d.shield=true` and the sword_shield family, so it never plays the spider's clips. Both spider attacks put their impact (tick 8) inside that window. The step it measures is, per frame, the sum over all bones of |change of rx|+|ry|+|rz| (`Pose.snapshot()`), with the hit minus without.

**Files:** `test-tools/standalone/SpiderHitCheck.java` (new, `package gg.gravesandglory;`; the coordinator must accept a file in the repo, otherwise keep it in the scratchpad and pass the same path to `EXTRA`).

`Drive`, `check`, `seeds` and `realSwordShield` are package-private statics of `MotionEngineCheck`, not public API. `mec.sh` also compiles `MotionEngineCheck.java`, but the new file copies what it needs so it stays self-contained:

- [ ] **Step 1:** Write `SpiderHitCheck.java` (class `SpiderHitCheck` with `main`), in package `gg.gravesandglory`:
  - `static final String CLIPS = "src/main/resources/assets/gravesandglory/clips/spider/";`
  - `check(boolean, String)` and `seeds(int n, long at)` copied from `MotionEngineCheck` (lines 29 and 92).
  - A small `Drive` copied from `MotionEngineCheck.Drive` (line 143): fields `time=1000`, `stance`, `enemy`, `shield`, `attack`, `hit`, `seed`; `in()` builds `new MotionInput(time, 0, 0, 0, 0, 0, stance, 0f, shield, enemy, false, 0, attack, hit, seed, null)`; `tick()` is `(long) Math.floor(time * 20)`; `step(dt)` adds dt to `time`; `hit(zone)` sets `new MotionInput.Hit("slash", zone, 0f, 1, false, false, tick())`.
  - `realSpider(Map<String, Clip> all)` copied from `realSwordShield` (line 463): load the single folder `CLIPS` with `Files.list(Path.of(CLIPS)).sorted()` and `Clip.parse(Files.readString(p))` into `all` (there is no `spider/common`); stances from `idle`, `carry`, `guard`; attacks `List.of(all.get("spider_bite"), all.get("spider_lunge"))` (the `Attack` variant 0 is the bite, 1 the lunge); the same `hit_(head|body|legs)` regexes and `deathKey` loop; `wall` = the attacks themselves (no `_wall` clips); `return new MotionEngine.Family("spider", stances, attacks, wall, hits, deaths);`.
  - The stance: `d.stance = ""; d.enemy = true; d.shield = false;` (`MotionEngine.stanceKey`: an empty stance with an enemy and no movement gives `"guard"`; `"hold"` with `shield=true` gives the shield pose, which the spider does not have, and `"hold"` with `shield=false` also gives guard).
  - `hitWhileAttacking(seed, variant, zone, hit, keep)` as in `MotionEngineCheck` (60 frames at dt 0.05, `d.attack = new MotionInput.Attack(variant, d.tick())` at frame 20, `d.hit(zone)` at frame 25, `MotionEngine.pose(d.in(), st, fam, out)`, `out.snapshot()`, the same sum of |dr| over all bones, `keep` filled at frame 25: track count, whether the only track is the attack, overlay count, overlay ease-in).
  - `main`: for variant in {0 (`spider_bite`), 1 (`spider_lunge`)}, zone in {`body`, `head`, `legs`} and the 4 seeds of `seeds(4, 4100)`: `with` and `without` arrays; `check(keep[1] == 1 && keep[2] == 1, ...)` (the attack stays the only track and one overlay is added), `check(ease(0.05 / keep[3]) < 0.5 && keep[3] >= 0.15, ...)`, then `worst = max(worst, with[f] - without[f])` for `f` 26 to 35. Print `PASS <attack> <zone> worst=<n>` per attack-zone pair (6 lines), then `SpiderHitCheck OK worst=<n> deg/tick`; `check(worst <= 15, ...)`; exit 1 if any check throws.
- [ ] **Step 2:** Run exactly: `MAIN=SpiderHitCheck EXTRA=test-tools/standalone/SpiderHitCheck.java bash C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/mec.sh`. **Expected:** 24 runs (2 attacks x 3 zones x 4 seeds) with no `AssertionError`; six `PASS` lines; every worst (hit minus without, summed over bones, ticks 26 to 35) at most 15.0 deg per tick; the attack is the only track after the hit; the last line `SpiderHitCheck OK worst=<n> deg/tick` with n at most 15.0; exit 0.
- [ ] **Step 3:** If over 15, shrink the offending hit's amplitude (root z, thorax x or fangs x, with the solved leg rolls scaled by the same factor so the tips stay within 0.5 px of the sole) by 20 percent steps, re-run Step 2, re-render. Expected: all pass; record the worst step in the ledger.
- [ ] **Step 4:** Commit the checker and any amplitude changes: "Add the Web Stalker hit-over-attack check".

### Task 8: All clips for the unit and verify game

- [ ] **Step 1:** Render `solo_stalker` with `GRAVES_GRADE_CLIPS=spider_bite,spider_lunge,guard,idle,carry,hit_body,hit_head,hit_legs,death`. Expected: every key at 8 angles shows the fangs and front legs pointing as the spec says, no clipping of legs, head or abdomen; fix in the clip keys only, re-verify, re-render.
- [ ] **Step 2:** `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background (it refuses while a Minecraft client is open). Expected: PASS. Afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 3:** Commit any fixes, write the ledger, tell the coordinator the new head of `work/anim-rework`.
- [ ] **Step 4:** Ask Kort which comes next: the `ensnare` web tell (Question 5), a spider gait layer, or the next family.
