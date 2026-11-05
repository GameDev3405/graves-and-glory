# Scythe Family Animations (Grave Reaper) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Grave Reaper a stance set and two heavy scythe attacks (`scythe_reap`, `scythe_overhead_hook`) with Souls-like weight, the weapon facing the move on every key. Kort approves each move in game.

**Architecture:** Clip data only, in a new family folder `clips/biped/scythe/`. Attack clips are additive offsets over `guard` (first and last key of every keyed channel is 0). Source is `art/clips/biped/scythe.bbmodel`, edited through `tools/anim_edit.py open/apply` (never hand-author JSON), built with `tools/clips.py`.

**Spec:** `docs/superpowers/specs/2026-11-03-scythe-animation-design.md` (key tables with numbers and a weapon direction on every key live there).

**Scratch tools** (recreate from `docs/superpowers/plans/2026-11-02-knight-swings.md` if missing), all under `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/`: `pose_preview.py`, `swing_ends.py`, `mec.sh`, `build_compare.py`, `try_hits.sh`, `hit_keys.py`. `pose_preview.py` draws a sword and shield only and `swing_ends.py` hard-codes the sword_shield folder and its IMPACT dict; both need a scythe branch (Task 0).

## Global Constraints

- Attack rows are fixed: `scythe_reap` (legs, slash, impact tick 12, heavy) and `scythe_overhead_hook` (head, slash, impact tick 12, heavy). Marker `impact` at 0.60 s. Clip length 1.3 s (26 ticks, equal to the 26-tick cooldown), never above `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40. Any change needs Kort.
- No gameplay, network or timing change. A model, weapon part or `_wall` variant is an art/scope decision for Kort.
- `python tools/clips.py` then `--verify` must pass. `Spec.java` only through `tools/preflight.py`. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- `clips.py` fails if a family bbmodel lacks a clip for any MELEE (stab/slash/blunt) ATTACKVARIANTS id (clips.py:36). Only the two scythe ids belong to this family; the bbmodel needs only those (plus stances).
- Expected `git status` throughout includes the already-modified `sheets/preflight.json` and `Spec.java` (and the dirty `art/clips/biped/sword_shield.bbmodel`); do not stage them.
- Max 2 Minecraft instances (Kort's client counts). Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No eula.txt. No computer-use on Minecraft. No push, publish or merge.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Each approved move is committed on its own. Ask Kort with AskUserQuestion, short and plain.

## Review Focus

1. **Weapon faces the move:** every key's haft/blade direction matches the spec table; blade edge leads at impact; check the summed pitch (guard body x + arm x + forearm x) against the stated direction.
2. **Start and end in guard:** every keyed channel's first and last key is 0 (`swing_ends.py`).
3. **Clipping:** haft/blade/arms through the netherite helmet, chest or legs at the overhead wind-up and the reap follow-through, at all 8 angles; and the wide reap against 0.7-block neighbours.
4. **Swing into swing:** clip length equals the 26-tick cooldown, so a chained swing starts the tick the previous ends; no pop.
5. **Hits over the attack:** the family-specific hit-over-attack check (Task 4) must stay at or under 15 deg/tick; the stock gate does not cover this family.

---

### Task 0: Prerequisites (needs coordinator and Kort)

**Files:** `tools/anim_edit.py` (line 29), new `art/clips/biped/scythe.bbmodel`, `art/troops/reaper.bbmodel` + `reaper.json`, `models/troop/index.json`, `test-tools/work/t16r12/live/run.sh` (gitignored).

- [ ] **Step 1 (needs Kort, Questions 1):** Confirm a reaper TroopModel with a scythe weapon part will be built. Without one `TroopMotion.row()` returns null (TroopMotion.java:143) and no clip plays. Expected: Kort says yes and the art task exists; otherwise stop and leave the specs as paper.
- [ ] **Step 2 (needs coordinator):** Create the bbmodel with `git show HEAD:art/clips/biped/sword_shield.bbmodel > art/clips/biped/scythe.bbmodel` (the worktree copy is dirty). `anim_edit.py` exits 2 when the family is not in `TROOP` (line 103), even with `--troop`, so a `TROOP` entry is needed in both orders, and that tooling edit needs the coordinator. Stand-in order (before the reaper model): add `'scythe': 'knight'` to `TROOP` (line 29) and run `python tools/anim_edit.py open scythe`; Expected: exit 0 and `work/anim-edit/scythe-knight.bbmodel` appears, on the knight's body with no scythe in hand, so keys written this way are re-judged on the reaper once its model exists. After the reaper model exists (`art/troops/reaper.bbmodel` or a reaper json for `build_skins.troop_project`), change the entry to `'scythe': 'reaper'`; Expected: exit 0 and `work/anim-edit/scythe-reaper.bbmodel` appears.
- [ ] **Step 3:** Rename/strip the copied bbmodel to the scythe clips: stub `scythe_reap` and `scythe_overhead_hook` (zero motion, `impact` marker at 0.60), remove the sword_shield clips. Run `python tools/clips.py` then `--verify`. Expected: both stubs build, no errors; only `clips/biped/scythe/*.json` appear as new files.
- [ ] **Step 4:** Add `solo_reaper` to `run.sh`, copying the `solo_knight` case with the reaper troop. Expected: `bash test-tools/work/t16r12/live/run.sh t0 solo_reaper` (background) produces frames with the Reaper standing in `guard` under `GRAVES_GRADE_CLIPS=guard`.
- [ ] **Step 5:** Extend `swing_ends.py` with an IMPACT dict `{scythe_reap: 0.6, scythe_overhead_hook: 0.6}` and the `scythe` folder, and give `pose_preview.py` a `--weapon scythe` mode (haft line plus blade tip). Expected: `swing_ends.py scythe_reap scythe_overhead_hook` prints OK for the stubs.

### Task 1: Stances (guard, idle, carry)

**Files:** `art/clips/biped/scythe.bbmodel`; generated `guard.json`, `idle.json`, `carry.json` in `clips/biped/scythe/`.

Targets (spec, Stances): guard with the haft about 30 deg above horizontal (spec sum -121, 31 above), pointing forward within 20 deg of the target line, blade end up and forward of the head; idle with the butt grounded beside the right foot and the haft within 5 deg of vertical; carry with the haft on the right shoulder, blade end up and back. Copy signs from the shipped `clips/biped/sword_shield/guard.json` (it crouches at root y +1.2..+1.35, y positive is down).

- [ ] **Step 1:** Write the keys from the spec tables via `anim_edit.py`. Expected: `clips.py --verify` passes; loops close (last key equals first).
- [ ] **Step 2:** `pose_preview.py guard --weapon scythe` at 0 and 1.5 s. Expected: blade tip forward of the head, never across the face; the summed chain 6 - 62 - 50 - 15 (body, arm, forearm, hand) = -121 gives the haft 31 deg above horizontal, matching the spec's "about 30"; the idle sum -180 gives a vertical haft; the carry sum -200 gives the haft back and up 70 deg above horizontal. If a preview disagrees with a sum, change the numbers or the text.
- [ ] **Step 3:** Render `solo_reaper` with `GRAVES_GRADE_CLIPS=guard,idle,carry`. Expected checklist at 8 angles: knees bent; both hands on the haft (left hand not floating); blade clear of the helmet; idle butt not sunk in the ground; carry blade end behind the head.
- [ ] **Step 4:** Rebuild the compare page (`build_compare.py`, localhost:8771) against `frames/guard_hold`, `frames/idle_stand`, ask Kort. Expected: Kort approves or lists changes; repeat.
- [ ] **Step 5:** Commit bbmodel and the three JSONs with the message "Pose the Grave Reaper stances with the scythe, video style", ending with the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Expected: `git show --stat` lists only those four files.

### Task 2: Reap (`scythe_reap`, video Wolff 0:04.0-0:04.9)

**Files:** `art/clips/biped/scythe.bbmodel` (animation `scythe_reap` only); generated `scythe_reap.json`.

Keys: the spec `scythe_reap` table (0.00, 0.20, 0.50, 0.60, 0.80, 1.05, 1.30 s). Weapon: wind-up haft behind the right hip 15 deg below horizontal (sum -75) with the blade end out and back; impact haft 30 deg below horizontal (sum -60), blade tip about 0.3 m above the floor (hand 0.89 m, tip 1.2 m along the haft), concave edge leading; follow-through haft rising on the left; recovery returns to the guard diagonal. Weight moves back at the gather (root z +0.8, y +0.8, y positive is down) and forward at impact (root z -1.8, y +0.6, the shipped maximum for z; flag it for the render because the reap is heavy). Pitch sums from the spec: wind-up S = -75 (15 deg below horizontal), impact S = -60 (30 below), follow-through S = -151 (61 above).

- [ ] **Step 1:** Write the keys; marker `impact` at 0.60; length 1.3. Expected: `clips.py` builds only `scythe_reap.json`.
- [ ] **Step 2:** Run `clips.py --verify` and `swing_ends.py scythe_reap`. Expected: verify passes (impact on tick 12 and on the sheet tick, length 26 <= 40 ticks), `swing_ends.py` prints OK.
- [ ] **Step 3:** `pose_preview.py scythe_reap out.png 0 0.2 0.5 0.6 0.8 1.05 --over guard --weapon scythe`. Expected checklist: at 0.50 the blade end is away from the target and clear of the helmet by 4 px, haft about 15 deg below horizontal (sum -75); at 0.60 the haft is about 30 deg below horizontal (sum -60) with the blade tip at about 0.3 m; at 0.80 the haft is about 61 deg above horizontal on the left (sum -151). Fix numbers or text where they disagree.
- [ ] **Step 4:** Render the Reaper in the background with `GRAVES_GRADE_CLIPS=scythe_reap,guard`. Expected checklist at 8 angles at keys 0.50, 0.60, 0.80: blade edge leads at 0.60; haft and arms do not enter the head, chest or legs; reach pass threshold: the blade tip within 0.3 m of the 3.0 range at impact (the spec calc predicts about 1.5 m, so this fails unless the weapon model is longer; report the measured number and raise it as Questions 6); the `transitions` pop flag reads "no".
- [ ] **Step 5:** Compare page "Reap v<N>" against `frames/attack_a`, ask Kort, repeat until approved.
- [ ] **Step 6:** `bash <scratchpad>/mec.sh`. Expected: no FAIL lines (this only runs the sword_shield fixture, see Task 4). Commit with the message "Swing the Grave Reaper's low reap from the guard, video style", ending with the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

### Task 3: Overhead hook (`scythe_overhead_hook`, video Wolff 0:09.8-0:10.2)

**Files:** same bbmodel (animation `scythe_overhead_hook`); generated `scythe_overhead_hook.json`.

Keys: the spec `scythe_overhead_hook` table. Weapon: wind-up haft above the head tilted back about 35 deg past vertical (sum -215, 125 from forward) with the tip hooked down, 4 px off the nape; impact haft 10 deg below horizontal pointing forward (sum -80, wrist flick hand +72), blade tip at about 1.5 m, head height (hand 1.72 m, tip 1.2 m along the haft), concave edge facing the head; follow-through haft 65 deg below horizontal (sum -25), blade end at knee height; recovery back to guard. The reference camera pans here (Spec References), so judge the hook in game, not frame for frame.

- [ ] **Step 1:** Write the keys; marker 0.60; length 1.3. Expected: `clips.py` builds only the hook JSON.
- [ ] **Step 2:** `clips.py --verify` and `swing_ends.py scythe_overhead_hook`. Expected: verify passes, OK.
- [ ] **Step 3:** Preview at 0 0.2 0.5 0.6 0.8 1.05 `--over guard`. Expected checklist: 0.50 haft above the head not through the helmet or nape, sum -121 - 94 = -215; 0.60 sum -121 + 41 = -80, so 10 deg below horizontal, pointing forward, tip at about 1.5 m; 0.80 sum -121 + 96 = -25, so 65 below, blade end near knee height. Root signs follow shipped sword_shield_overhead: y +0.4 at the wind-up, y -0.8 and z -1.8 at impact.
- [ ] **Step 4:** Render with `GRAVES_GRADE_CLIPS=scythe_overhead_hook,guard`. Expected checklist at 8 angles: no blade through the helmet at the wind-up; the blade tip reaches 1.4-1.9 m height; reach pass threshold: tip within 0.3 m of 3.0 at impact (calc predicts about 1.7 m, same note as Task 2); pop flag "no".
- [ ] **Step 5:** Compare page "Hook v<N>" (against `frames/attack_c`), ask Kort, repeat until approved.
- [ ] **Step 6:** `mec.sh`. Expected: no FAIL lines. Commit with the message "Hook the Grave Reaper's overhead swing from the guard, video style", ending with the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

### Task 4: Family-specific hit-over-attack check (scratch, not committed)

`MotionEngineCheck.hitOverAttack` (line 1093, helper `hitWhileAttacking` line 1057) runs the `realSwordShield` fixture only (family sword_shield, `d.stance = "hold"`, `d.shield = true`, attack index 0, attack at tick 20, body hit at tick 25) and never plays scythe clips, so it does not cover this family.

**Files:** new scratch copy `test-tools/standalone/MotionEngineCheckScythe.java` (not committed), made by `sed 's/MotionEngineCheck/MotionEngineCheckScythe/g' test-tools/standalone/MotionEngineCheck.java`, then edited: in `realSwordShield` change `List.of("common", "sword_shield")` to `List.of("common", "scythe")`, the family name to `"scythe"`, and the attack list to `List.of("scythe_reap", "scythe_overhead_hook")`; in `hitWhileAttacking` set `d.shield = false` (stance stays `"hold"`, the engine's loose-melee key; `guard` is the clip name, not a stance string) and parametrize `hitWhileAttacking` with an attack index (it hard-codes `new MotionInput.Attack(0, d.tick())`), calling it from `hitOverAttack` for index 0 and again for index 1.

- [ ] **Step 1:** Run `MAIN=MotionEngineCheckScythe EXTRA=test-tools/standalone/MotionEngineCheckScythe.java bash <scratchpad>/mec.sh` (mec.sh compiles the stock check plus `${EXTRA}` and runs `gg.gravesandglory.${MAIN}`). Expected: it compiles with no error lines. The copy also runs the thrust- and shield-specific checks (`hitOverWindow` and the checks near lines 496 and 520), which may FAIL for a scythe family; ignore those and read only the "hit over attack" line.
- [ ] **Step 2:** The attack starts at tick 20 and the hit at tick 25, so the window ticks 26-35 are attack ticks 6-15 (0.30-0.75 s), which includes the impact at tick 12. Expected: the output has the line "hit over attack: the attack keeps playing; the hit adds at most X deg/tick" with X at most 15.0 for attack index 0 and for index 1; no FAIL on that line (other lines are ignored, see Step 1).
- [ ] **Step 3:** If X exceeds 15, add scythe-owned `hit_*` clips (Questions 2, needs Kort) or reduce the body offset; use `hit_keys.py` and `try_hits.sh` for the numbers. Expected: the rerun prints X at most 15.0 and `python tools/clips.py --verify` still passes.

### Task 5 (BLOCKED on Kort's answer to Questions 3): wall variants

**Files:** bbmodel, generated `scythe_reap_wall.json`, `scythe_overhead_hook_wall.json`.

- [ ] **Step 0:** Do not start until Kort answers Questions 3. The spec has no key table for the wall variants, because the deltas depend on the `wall`/`brace` hold that does not exist yet; write that table (with weapon direction and pitch sum on every key) into the spec first and have it reviewed. Expected: a table in the spec and Kort's yes.
- [ ] **Step 1:** Add the two clips (spec: yaw and abduction scaled by 0.35, blade end within 0.9 m of the body line, impact 0.60, length 1.3) and a tight `wall` and `brace` hold. Weapon direction on every key per the spec. Expected: `clips.py --verify` passes; `MotionEngine` picks the wall variants when the stance is wall or brace.
- [ ] **Step 2:** Render three Reapers in a 0.7-block row. Expected: no blade through the neighbours at any key; the pop flag "no".
- [ ] **Step 3:** Ask Kort. Commit with the message "Add tight wall variants for the Grave Reaper's swings", ending with the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

### Task 6: Land it

- [ ] **Step 1:** Run `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background (it refuses while a Minecraft client is open). Expected: PASS. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 2:** Render `solo_reaper` at L1, L2 and L3 (netherite helmet and iron chest in all) with `GRAVES_GRADE_CLIPS=guard,idle,carry,scythe_reap,scythe_overhead_hook`. Expected: every swing's key poses at 8 angles show no clipping on any level; `cleave_reaper` visual (second target) reads on the reap.
- [ ] **Step 3:** Fix any finding in the clip keys, re-verify, re-ask Kort for any visible change, and commit with the trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Write the ledger and tell the coordinator the new head of `work/anim-rework`. Expected: `git log --oneline` shows one commit per approved move since the specs, each with the trailer; `git status` shows only the already-modified `sheets/preflight.json`, `Spec.java` and `art/clips/biped/sword_shield.bbmodel`, plus the untracked docs (`docs/superpowers/plans/2026-11-03-thrower-animation.md`, the scythe and thrower specs and the scythe plan) and `src/main/resources/assets/gravesandglory/clips/biped/sword_shield/hit_*.json`; nothing is pushed.
