# Crossbow Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task by task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Give the crossbow family (`ic_arbalist`, `ic_ballista`) its own Souls-like clips: `aim`, `idle`, `carry`, `rest` stances and the attack `crossbow_loose` with its reload, each approved by Kort in game.

**Architecture:** One Blockbench source `art/clips/biped/crossbow.bbmodel` is compiled by `tools/clips.py` into `src/main/resources/assets/gravesandglory/clips/biped/crossbow/*.json`. A family folder overrides `common/` by name. Clips are additive offsets over the stance. Legs are unkeyed (MotionFeet plants them).

**Tech Stack:** Blockbench bbmodel, `tools/anim_edit.py`, `tools/clips.py`, scratch preview scripts, `MotionEngineCheck`, the solo grade harness `test-tools/work/t16r12/live/run.sh`.

**Spec:** `docs/superpowers/specs/2026-11-03-crossbow-animation-design.md`

---

## Global Constraints

- Docs-first: do not start until Kort has answered Questions 2 and 3 of the spec (Java bow-check fix, troop models). Tasks 1-4 need no Java. Task 6 (in-game render) needs the troop models; until then use the stand-in rule in Task 6.
- **Blocker (Ballista):** the shared clip drops the arms about 40 degrees and brings the stock to the belly (forearm flexed 45 and arm dropped 40 at 1.05), but the Ballista frame is a root part with a fixed stock. Do not render, tune or approve the Ballista until Kort answers spec Question 1 (shared arm drops vs fixed stock); report and wait.
- Stand-in model: `art/troops/` has no `ic_arbalist`; until it exists use `--troop archer` for previews (the archer model exists).
- No gameplay, save-format or protocol change. Attack row `crossbow_loose` (zone body, kind bolt, impact tick 4 = 0.20 s, medium) is fixed. Clip max 40 ticks (`BattleMath.ATTACK_CLIP_MAX_TICKS`).
- Weapon-faces-the-move rule: every key states where the crossbow points (spec Moves table). Check wind-up, impact and follow-through from front, 3/4 and side. A key whose weapon direction is wrong or unstated is a defect. Stock at the shoulder and the bolt level at the target at the loose; during the reload the crossbow points down and forward, never across the head.
- First and last keys of every keyed channel are 0. Legs unkeyed.
- Scratch tools live in `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (`pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`). If a file is missing, recreate it from Task 1 of `docs/superpowers/plans/2026-11-02-knight-swings.md`. Below, `SP` means that directory.
- After every `clips.py`, `--verify`, gradle or grade run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- At most 2 Minecraft instances. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No eula.txt. No computer-use on Minecraft. No push, publish or merge.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- References: `C:/Users/kortt/Melty/anim-refs/crossbow/` (frames `xb_raise`, `xb_reload`, `ballista_crank`; `SOURCES.md`). Never committed.

## Review Focus

1. Weapon direction at each key (table in the spec): stock at the shoulder and bolt level at the loose; muzzle down and forward during the reload.
2. The reload reads as haul, lay the bolt, lift, with a body hunch, and is done within 36 ticks.
3. No clipping at 8 angles: stock vs cheek and goggles, coat skirt vs the lowered crossbow, left hand vs forestock.
4. Hit over attack: ticks 26-35 of the crossbow case stay at or under 15 degrees per tick (the lift runs there).
5. Both troops are checked (Arbalist 0.90, Ballista frame), and the bow code does not run on the crossbow.

---

## Task 1: Scratch tools, source file and the crossbow fixture

**Files:**
- Create: `art/clips/biped/crossbow.bbmodel`
- Modify (scratch, not committed): `SP/swing_ends.py`, `SP/pose_preview.py` invocations
- Modify: `tools/anim_edit.py` (add `crossbow` to its TROOP table with default `archer`; mandatory, a tooling change that needs the coordinator's approval before it lands; switch to `ic_arbalist` once that model exists)

- [ ] **Step 1: Check the scratch tools exist**

Run: `ls "C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/"`
Expected: lists `pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`. If one is missing, recreate it from the knight plan Task 1.

- [ ] **Step 2: Point `swing_ends.py` at the crossbow**

Edit `SP/swing_ends.py` so the clip dir is `src/main/resources/assets/gravesandglory/clips/biped/crossbow/` and the IMPACT dict is `{"crossbow_loose": 0.20}`.
Expected: `python SP/swing_ends.py` prints one row (`crossbow_loose`) or reports the file missing (no clip yet).

- [ ] **Step 3: Create the source file (before anything opens it)**

`tools/anim_edit.py open` reads `art/clips/biped/<family>.bbmodel`, so it must exist first. Copy `art/clips/biped/sword_shield.bbmodel` to `art/clips/biped/crossbow.bbmodel` and delete all its animations (keep the biped rig).
Expected: `python -c "import json;d=json.load(open('art/clips/biped/crossbow.bbmodel'));print(len(d.get('animations',[])))"` prints `0`.
Note: the stand-in troop `archer` draws the **archer's bow**, not a crossbow, so previews on it show a bow in the hand until `ic_arbalist` exists; judge arm and body poses and the muzzle direction from the bow's axis, and do not add a crossbow cube to the shared archer model.

- [ ] **Step 3b: Add the TROOP entry (approval first)**

Ask the coordinator to approve the one-line `tools/anim_edit.py` change, then add `crossbow` with default troop `archer`.
Expected: `python tools/anim_edit.py open crossbow --troop archer` no longer reports an unknown family.

- [ ] **Step 3c: Open it**

Run: `python tools/anim_edit.py open crossbow --troop archer`
Expected: Blockbench opens `work/anim-edit/crossbow-archer.bbmodel`: the archer model, no animations, the bow in the hand.

- [ ] **Step 4: Verify the family wiring still passes**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: `clips.py --verify` fails once with "missing clip crossbow_loose" (the melee-kind rule applies only to melee rows; if it passes, that is also fine). Record which in the commit message. No other file changes.

- [ ] **Step 5: Commit**

```
git add art/clips/biped/crossbow.bbmodel tools/anim_edit.py
git commit -m "Add the crossbow clip source with a stand-in crossbow"
```
(with the trailer)

---

## Task 2: Stances `aim`, `idle`, `carry`, `rest`

**Files:**
- Modify: `art/clips/biped/crossbow.bbmodel`
- Generate: `src/main/resources/assets/gravesandglory/clips/biped/crossbow/{aim,idle,carry,rest}.json`

Target poses (spec Stances table; absolute pitch x unless shown, rotation degrees; legs unkeyed). The hand values are derived from muzzle above level = -(body + R arm + R forearm + R hand) - 90, a rough estimate: `pose_preview.py` is the check.

| Clip | Body | Head | R arm / forearm / hand | L arm / forearm / hand | Muzzle estimate and weapon direction |
|---|---|---|---|---|---|
| aim | 4 (y 25) | -2 (y -25, z -6) | -55 (y 10, z 12) / -85 / 46 | -85 (y -10, z -6) / -20 / 11 | level: stock at the right shoulder, bolt level at target chest height |
| idle | 3 (y 8) | 0 (y -8) | -25 (z 12) / -70 / -33 | -45 (y -10, z -6) / -60 / -23 | 35 above level: stock at the right ribs, muzzle forward-up |
| carry | 4 (y 6) | -2 (y -6) | -25 / -70 / -19 | -45 / -60 / -9 | 20 above level |
| rest | 0 | 0 | 5 (z 8) / -20 / -35 | 0 / -5 / 0 | 40 below level: hand at the hip, muzzle down-forward, not across the legs |

- [ ] **Step 1: Author the four stances in Blockbench**

Run: `python tools/anim_edit.py open crossbow --troop archer`, then set the keys with `tools/anim_edit.py apply` (one animation each, loop on, `aim` 3.0 s with 0.5 degree sway, `idle` 4.0 s, `carry` 2.4 s, `rest` 4.0 s).
Expected: four looping animations; first and last keys of each channel equal.

- [ ] **Step 2: Preview each from front, 3/4 and side**

Run (per stance): `python SP/pose_preview.py aim SP/aim.png 0 1.5 --bb art/clips/biped/crossbow.bbmodel`
Expected checklist, in the render: aim: stock touches the right shoulder, bolt line level and pointing at the target (not up, not across the head), left hand under the forestock; idle and carry: crossbow across the chest, muzzle up-forward and off to the left of the face; rest: hand at the hip (not raised toward the chest), muzzle down and forward about 40 below level, clear of the legs. Idle about 35 and carry about 20 above level. No stock in the cheek or goggles in any view.

- [ ] **Step 3: Compile and verify**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: four stance JSONs appear under `clips/biped/crossbow/`; verify passes loops close (start == end).

- [ ] **Step 4: Kort review gate, then commit**

Build the compare page (Task 5 pattern) for the stances against the Elden Ring idle/raise frames; Kort approves in game.
```
git add art/clips/biped/crossbow.bbmodel src/main/resources/assets/gravesandglory/clips/biped/crossbow
git commit -m "Pose the crossbow aim, idle, carry and rest stances"
```
(with the trailer)

---

## Task 3: Attack `crossbow_loose`

**Files:**
- Modify: `art/clips/biped/crossbow.bbmodel`
- Generate: `.../clips/biped/crossbow/crossbow_loose.json`

Keys (spec Moves table; deltas from `aim`: pitch degrees, yaw `y` and roll `z` in brackets; the root column is position in pixels, `y` = down; R hand and L hand pitch deltas are 0 at every key; time in seconds, tick = s x 20). The loose and aim are authored, no reference (spec Question 6); the reload follows Elden Ring `xb_reload` f_015 (14:07.4: crossbow at the belly, roughly level) and Ironbelly `arb_reload1` f_040 (6.0 s: level, bolt in the groove). Muzzle below level is the rough estimate `body + R arm + R forearm + R hand`; `pose_preview.py` is the check.

| t | tick | body | head | R arm | R forearm | L arm | L forearm | root (px) | Muzzle below level (estimate) / weapon direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0: stock at shoulder, bolt level at target |
| 0.10 | 2 | 0.5 | 0.5 | 1 | 0 | 1 | 0 | 0 | 1.5: cheek weld, bolt level |
| **0.20** (impact marker) | 4 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **0: bolt level at the target chest, stock at shoulder** |
| 0.25 | 5 | -3 (y -2) | -2 (y 2) | -6 | 4 | -5 | 3 | z -0.4 | -5: muzzle up about 5 |
| 0.35 | 7 | 1 | 1 | 2 | 0 | 2 | 0 | 0 | 3: level, start of the lowering |
| 0.55 | 11 | 3 (y -2) | 3 (y 1) | 11 (z -2) | -13 | 13 | 7 | y 0.2 | 1: elbows dropping, still level |
| 0.80 | 16 | 7 (y -4) | 6 (y 3) | 26 (z -4) | -29 | 29 | 16 | y 0.5 | 4: crossbow at the chest, nearly level |
| 1.05 | 21 | 10 (y -6) | 10 (y 5) | 40 (z -6) | -45 | 45 | 25 | y 0.8 | 5: haul, stock at the belly, forearms near horizontal, level to slightly down |
| 1.25 | 25 | 10 (y -4) | 10 (y 4) | 28 (z -4) | -33 | 34 | 16 | y 0.5 | 5: lay, bolt in the groove, never across the head |
| 1.55 | 31 | 6 (y -2) | 4 (y 2) | 14 | -16 | 16 | 8 | y 0.2 | 4: rising toward the target |
| 1.70 | 34 | 2 (y -1) | 2 (y 1) | 6 | -7 | 7 | 3 | y 0.1 | 1: nearly level |
| 1.80 | 36 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0: back in aim, bolt level at the target |

Near-even key spacing, no eases: each bone at most 6 degrees per tick, summed about 13.5 (lowering) and 12.5-14.5 (lift). Marker `impact` at 0.20 only. Length 1.8 s (36 ticks).

- [ ] **Step 1: Author the keys**

Run: `python tools/anim_edit.py open crossbow --troop archer`, add `crossbow_loose`, apply the table above (hands 0), set the impact marker at 0.20.
Expected: one animation, 1.8 s, marker at 0.20, no keys on legs or hands (or hands all 0).

- [ ] **Step 2: Preview the key frames**

Run: `python SP/pose_preview.py crossbow_loose SP/loose.png 0 0.2 0.25 0.55 0.8 1.05 1.25 1.55 1.8 --bb art/clips/biped/crossbow.bbmodel --over aim`
Expected checklist (front, 3/4, side and top): at 0.20 the bolt line is level and points at the target, stock on the shoulder; at 0.25 the muzzle is up about 5 and the stock stays at the shoulder; at 0.55-1.25 the whole crossbow drops from shoulder to belly with the muzzle level to about 5 below (read off the side view; the table estimate is only a guess), never across the head, free hand at the string or groove, torso hunched about 10 at 1.05; at 1.55 rising toward the shoulder; at 1.80 identical to aim. No part of the crossbow in the head, the goggles or the coat skirt. If a muzzle angle in the picture differs from the table by more than 5 degrees, change the table, not the picture.

- [ ] **Step 3: Compile and verify**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: `crossbow_loose.json` created; verify passes (impact on a tick and on the sheet tick 4, clip 36 ticks, at most 40, first and last keys 0).

- [ ] **Step 4: Check the ends**

Run: `python SP/swing_ends.py`
Expected: `crossbow_loose  impact 0.20 s = tick 4  length 1.80 s  first/last 0  OK`. If the script prints no row, fix its dir (Task 1 Step 2).

- [ ] **Step 5: Compare page against the video**

Run: `python SP/build_compare.py crossbow_loose` and open `http://localhost:8771/`.
Expected: side by side with `frames/xb_raise` and `frames/xb_reload` (reload only): the drop to the belly, the hunch and the raise match in order and posture (the muzzle stays near level, as in f_015). The aim and loose have no reference (spec Question 6): they are judged on the weapon direction only.

- [ ] **Step 6: Commit after Kort approves**

```
git add art/clips/biped/crossbow.bbmodel src/main/resources/assets/gravesandglory/clips/biped/crossbow/crossbow_loose.json
git commit -m "Shoulder, loose and reload the crossbow from the aim stance"
```
(with the trailer)

---

## Task 4: Hit over attack for the crossbow, and the inherited hits

**Files:**
- Create (scratch): `SP/window_check.py`
- Modify: `test-tools/standalone/MotionEngineCheck.java` (test tool only)

`hitOverAttack` (~line 1093) uses `realSwordShield()` attack 0 (the thrust): attack starts at frame 20, the hit lands at frame 25, and it checks frames 26-35 (with-hit step minus without-hit step at most 15 degrees per tick, summed over all bones, cloth springs included). It never plays crossbow clips, so it does not cover this family. Clip tick = frame - 20. For `crossbow_loose`, frames 26-35 are clip ticks 6-15, the **lowering**; a second hit at frame 47 has its window at frames 48-57, **clamped to ticks 28-35** because the clip ends at tick 36 (the lift).

- [ ] **Step 1: Write and run the window checker**

`hit_keys.py` only builds hit clips; it has no window options. Create `SP/window_check.py`:

```python
import json, sys
def val(keys, t):
    ks = sorted(keys, key=lambda k: k['t'])
    if t <= ks[0]['t']: return ks[0]['v']
    for a, b in zip(ks, ks[1:]):
        if t <= b['t']:
            f = (t - a['t']) / (b['t'] - a['t'])
            return [x + (y - x) * f for x, y in zip(a['v'], b['v'])]
    return ks[-1]['v']
clip = json.load(open(sys.argv[1]))
bones = {b: c['rotation'] for b, c in clip['bones'].items() if 'rotation' in c}
for w in sys.argv[2:]:
    lo, hi = map(int, w.split('-'))
    wb = ws = 0.0
    for k in range(lo, hi + 1):
        st = {b: sum(abs(p - q) for p, q in zip(val(r, k / 20), val(r, (k - 1) / 20))) for b, r in bones.items()}
        wb = max(wb, max(st.values())); ws = max(ws, sum(st.values()))
    print(f'ticks {lo}-{hi}: max per-bone step {wb:.1f}, max summed step {ws:.1f} deg/tick')
```

Run: `python SP/window_check.py src/main/resources/assets/gravesandglory/clips/biped/crossbow/crossbow_loose.json 6-15 28-35`
Expected: two lines; per-bone step at most 6 and summed step at most 15 in both (about 13.8 and 14.5 as authored). This measures the clip's own motion, a headroom proxy; the real gate (with-hit minus without-hit) is Step 3. If a window fails, spread that segment over more ticks or shrink the amplitude, then rerun Task 3 Step 3.

- [ ] **Step 2: Add a crossbow case**

Copy the sword_shield fixture into `realCrossbow()` (the crossbow stances and `crossbow_loose` from the new JSONs, attack index 0) and add two assertions to `hitOverAttack`: attack starts at frame 20, hit at frame 25, frames 26-35 at most 15 degrees per tick; and a second run with the hit at frame 47, frames 48-55 (clamped to the clip's last tick, 35 = frame 55).
Expected: the file compiles.

- [ ] **Step 3: Run the check**

Run: `bash SP/mec.sh`
Expected: prints `hit over attack (crossbow, hit@25, lowering): max X <= 15` and `(crossbow, hit@47, lift): max Y <= 15`, the sword_shield case unchanged; exit 0. If it fails, return to Step 1 and reshape the clip; do not change the check.

- [ ] **Step 4: Check inherited hits with the crossbow in hand**

Run: `bash SP/try_hits.sh crossbow`
Expected: for `hit_head`, `hit_body`, `hit_legs` (and `_2`, `_3`) from `common/` over `aim` and over the haul pose at 1.05: the crossbow does not pass through the head or torso. If it does, author `hit_*.json` family overrides at about 0.4 size (`SP/hit_keys.py`), rerun Steps 1 and 3, and commit them with this task.

- [ ] **Step 5: Commit**

```
git add test-tools/standalone/MotionEngineCheck.java src/main/resources/assets/gravesandglory/clips/biped/crossbow
git commit -m "Check a hit over the crossbow lowering and lift"
```
(with the trailer)

---

## Task 5: Java fix so the bow code does not run on the crossbow (needs approval)

Only after Kort approves Question 2 of the spec. Skip entirely otherwise and note it in the final report.

**Files:**
- Modify: `src/main/java/gg/gravesandglory/TroopMotion.java` (`bow(row)`, line 146)

- [ ] **Step 1: Narrow the check**

At `src/main/java/gg/gravesandglory/TroopMotion.java:146` change `bow(row)` from `row!=null&&row.ranged()&&!row.magic()` to `row!=null&&row.ranged()&&!row.magic()&&!"crossbow".equals(row.family())`.
Decision: do not narrow to `"bow".equals(family)`, because that would also remove the bow twist and draw from the thrower (`tc_harpooner`, family `thrower`, ranged non-magic), a behaviour change nobody asked for. Only the crossbow is excluded.
Expected: compiles; `bow(row)` is true for archer, longbow, bonebow, stalker, wm_hunter and tc_harpooner, false for ic_arbalist and ic_ballista.

- [ ] **Step 2: Run the checks**

Run: `bash SP/mec.sh; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` (the Spec checkout does not touch `TroopMotion.java`).
Expected: MotionEngineCheck passes; archers and the harpooner unchanged.

- [ ] **Step 3: Commit**

```
git add src/main/java/gg/gravesandglory/TroopMotion.java
git commit -m "Only the bow family draws a bow"
```
(with the trailer)

---

## Task 6: Render in game, both troops

**Needs** the `ic_arbalist` and `ic_ballista` troop models (spec Question 3). If they do not exist, stop and report; do not stub models here. If spec Question 1 is open, render the Arbalist only and for the Ballista write "report and wait".

**rootScale:** read it, do not guess. The loader (`TroopModel.rig`, TroopModel.java:311) computes `(24 - model root pivotY) / (24 - rig root pivotY)`; the rig's root pivot is 0 (`art/rigs/biped.json`) and the pivot in a troop json is pivotY directly (TroopModelLoader.java:216). A model json with no `bones.root.pivot` gets exactly 1.0 (knight, archer, sentinel do). `TroopMotion.rootScale` (line 298) multiplies the root's offsets by it. No log line prints it today, so compute it from the files:
`python -c "import json;p=json.load(open('art/troops/ic_arbalist.json'))['bones'].get('root',{}).get('pivot',[0,0,0])[1];print((24-p)/24)"`
Expected: prints the value (1.0 if the model has no root pivot; about 0.90 only if the model lowers the root 2.4 px). Write it into the spec's Units table and the commit message; if it is below 0.85 or above 1.0, tell the coordinator before rendering.

**Files:**
- Modify (gitignored): `test-tools/work/t16r12/live/run.sh` (add cases `solo_arbalist` and `solo_ballista` beside `solo_knight`, env `GRAVES_PHASE2_CHECKS=motion-grade`, `GRAVES_MOTION_FAMILY=crossbow`, `GRAVES_GRADE_TROOP=ic_arbalist` / `ic_ballista`, `GRAVES_GRADE_CLIPS=aim,idle,carry,rest,crossbow_loose`, `GRAVES_GRADE_OUT`, `GRAVES_GRADE_PARTS`)

- [ ] **Step 1: Render the Arbalist**

Run: `bash test-tools/work/t16r12/live/run.sh solo_arbalist` (one instance).
Expected, frames at 8 angles for aim and crossbow_loose at 0.20, 0.25, 0.55, 0.80, 1.05, 1.25, 1.55: (a) at 0.20 the bolt is level and points at the target, stock on the shoulder; (b) at 0.25 muzzle up about 5 degrees, stock still at the shoulder, left hand on the forestock; (c) at 0.55-1.25 crossbow dropping to the belly with the muzzle near level, never across the head, at 1.05 the muzzle level to about 5 below with the stock at the belly and the forearms near horizontal; (d) goggles, beard, tabard and bolt case not crossed by the crossbow at any angle; (e) coat skirt clear of the crossbow at 1.05; (f) at 1.80 the frame equals aim; (g) the root offsets at 0.55-1.25 look right for the rootScale read in the step above (feet planted, no floating).

- [ ] **Step 2: Render the Ballista (only if spec Question 1 is answered)**

Run: `bash test-tools/work/t16r12/live/run.sh solo_ballista`
Expected: same frames, plus (h) the arms stay on the frame stock at 1.05 and do not pass through the frame or wheels, (i) the wheels clear the legs as the root dips 0.8 px. If (h) or (i) fails: report and wait; do not tune around it.

- [ ] **Step 3: Compare page and sign-off**

Run: `python SP/build_compare.py crossbow_loose` with the two renders.
Expected: Kort sees video and both troops side by side and approves or lists fixes; at most two fix rounds, then record the remaining issues and stop.

---

## Task 7: Final check: all troops in the family and the verify game

- [ ] **Step 1: Verify**

Run: `python tools/clips.py && python tools/clips.py --verify; git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`
Expected: pass; the crossbow family lists `aim`, `idle`, `carry`, `rest`, `crossbow_loose` plus any hit overrides.

- [ ] **Step 2: Check both troops resolve the family**

Run: `bash SP/mec.sh`
Expected: MotionEngineCheck passes (sword_shield and crossbow cases).

- [ ] **Step 3: Verify game**

Close every Minecraft client first (the command refuses to run while one is open), then run `python tools/verify.py game --offline` (docs/SETUP.md), then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
Expected: exits 0 with the game checks passing; `git status` shows only the crossbow files, the check file and (if approved) `TroopMotion.java`.

- [ ] **Step 4: Land it**

No push or merge. Report to the coordinator: commits made, Kort's approvals, open items (Questions 1, 3, 4).
