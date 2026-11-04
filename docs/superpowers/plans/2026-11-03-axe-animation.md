# Axe family animation implementation plan

> For agents: use superpowers:subagent-driven-development or superpowers:executing-plans, task by task. Steps use `- [ ]`.

**Goal:** Axe stances and three Souls-like attacks for the Shieldbreaker (`ic_breaker`).
**Spec:** `docs/superpowers/specs/2026-11-03-axe-animation-design.md` (key tables are the source of truth; the weapon-direction column must hold at every key; +y turns right).
**Tech:** clips live in `art/clips/biped/axe.bbmodel` and are edited with `python tools/anim_edit.py open axe` / `apply`; the JSON under `src/main/resources/assets/gravesandglory/clips/biped/axe/` is generated, never hand-authored. Compare page on localhost:8771.

## Global Constraints

- Branch `work/anim-rework`. No push, merge or publish. Do not edit Java, Spec.java or `sheets/` except the build-time check below.
- Fixed impact ticks: chop 12, hew 12, jab 7; clips at most 40 ticks. Changes need Kort.
- Action clips are additive over the guard; first and last keys of every channel are 0.
- After `clips.py`, `--verify`, `anim_edit.py apply` or gradle: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Real loadout: vindicator, biped rig, rootScale 1.0 (spec "Units"), vanilla `minecraft:netherite_axe`, iron helmet and chestplate, no shield, no cape.
- Scratch tools (pose_preview.py, swing_ends.py, mec.sh, build_compare.py, try_hits.sh, hit_keys.py) live in the session scratchpad. If missing, recreate them from the knight plan (`2026-11-02-knight-swings.md`).
- At most 2 Minecraft instances, no computer-use on Minecraft, no eula.txt. Kort approves each move on the compare page before the next.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

Weapon direction per key (spec tables); no axe through helmet, nose or chestplate; hit-over-attack numbers; the jab needs Kort's answer on the great-axe model (spec Question 1).

## Hit-over-attack, family check (build-time work, not tonight's spec work)

`MotionEngineCheck.hitOverAttack` (`test-tools/standalone/MotionEngineCheck.java` ~line 1093) calls `hitWhileAttacking` (~line 1057), which runs only `realSwordShield`. Add a family copy: `realAxe` (axe family) and `hitOverAttackAxe`, with the attack id picked per move. Timing, same as the original: the attack starts at tick 20, a body hit lands at tick 25, so the checked window ticks 26-35 equals attack ticks 6-15 of the clip. That covers the strike for all three moves (chop and hew impact at attack tick 12; jab impact at attack tick 7, so for the jab the window starts just before the impact and runs through the hold). Command: `bash <scratchpad>/mec.sh` (MotionEngineCheck alone; recreate from the knight plan if missing). Rule: step with the hit minus the step without at most 15 deg/tick. Expected: no FAIL line, and the "hit over attack axe" line prints a maximum under 15.0 for each finished move. The check is added only when the move's real clip exists (Tasks 3-5), never in Task 1, because stub clips would prove nothing.

---

## Task 1: Scaffold with stub attacks

`clips.py` fails if the family bbmodel lacks a clip for any `ATTACKVARIANTS` id, so stubs go in first.

**Files:** create `art/clips/biped/axe.bbmodel` (copy `sword_shield.bbmodel`'s rig and strip its clips; if `anim_edit.py open axe` offers to create the family, use that).

- [ ] Run `python tools/anim_edit.py open axe --troop ic_breaker`; in the work file create three zero-motion stub clips `axe_overhead_chop` (1.8 s, impact marker 0.6 s), `axe_side_hew` (1.8 s, marker 0.6 s), `axe_haft_jab` (1.1 s, marker 0.35 s), with the same marker name and loop settings as `sword_shield_overhead`. Each has only 0-valued first and last keys.
- [ ] `python tools/anim_edit.py apply`. Expected: it exits 0, verify passes (impact on ticks 12, 12, 7; attacks at most 40 ticks), and `clips/biped/axe/` holds the three stub JSON files.
- [ ] Restore generated files: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. Expected: `git status` shows only axe files.
- [ ] `git add art/clips/biped/axe.bbmodel src/main/resources/assets/gravesandglory/clips/biped/axe/` then commit:

```
git commit -m "Scaffold the axe clip family with stub attacks" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Task 2: Stances (guard, idle, carry)

- [ ] In the open work file (`anim_edit.py open axe`) add `guard` (length 2.4 s, loop), `idle` and `carry` from the spec Stances table (absolute rotations, catmullrom, guard breathing at most 3 deg on body x). Expected: guard right_arm x -55 and left_arm x -60 at t=0; idle and carry right_forearm x -75; first key equals last key on every loop.
- [ ] Measure step: render the guard with `pose_preview.py` and measure the bit size and hand positions on the vanilla axe; update the spec's "Item assumptions" and the clearance lines if they differ by more than 1 px.
- [ ] `anim_edit.py apply`; restore generated files. Expected: verify passes, loops close (difference 0).
- [ ] pose_preview.py guard front and side. Expected: haft near horizontal across the chest (arm + forearm = -90), bit to the right; at least 2 px clear of the nose and 3 px in front of the chest face. Idle: axe on the right shoulder, bit behind the head.
- [ ] Rest and aim: no clips; confirm the stance chain falls back to idle (expected: no missing-clip warning in the sim). Brace and wall: not authored, fall back to guard.
- [ ] Commit "Pose the axe guard, idle and carry".

## Task 3: `axe_overhead_chop` (impact 12, 36 ticks)

- [ ] In the work file replace the chop stub with spec move 1 (hand bones included, deltas over the guard). Expected: keys at 0, 0.30, 0.48, 0.60, 0.80, 1.30, 1.80 s; first and last keys 0; impact marker at 0.60 s.
- [ ] `anim_edit.py apply`; restore generated files. Expected: attack 36 ticks (at most 40), impact on tick 12.
- [ ] swing_ends.py. Expected: at 0.48 s right_arm abs -150 and right_forearm abs -30 (sum -180), haft within 15 deg of vertical, bit up behind the head; at 0.60 s right_arm abs -40 and right_forearm abs 0 (sum -40), haft 50 deg (+-5) below horizontal, edge first.
- [ ] Clearance with the measured bit size: at 0.48 s the haft is at least 9 px right of the head centreline; at the 0.60 s impact at least 3 px in front of the chest face; the bit stays 2 px in front of the nose on the downstroke.
- [ ] Family hit-over-attack check (section above) for chop with hit_body. Expected: the maximum over attack ticks 6-15 is under 15.0 deg/tick.
- [ ] Render with `GRAVES_GRADE_CLIPS` (run.sh) and open the compare page. Expected: silhouette matches the video frames 5:19.0-5:22.0. Stop and wait for Kort's approval.
- [ ] Commit "Swing the axe overhead chop from the guard, video style".

## Task 4: `axe_side_hew` (impact 12, 36 ticks)

- [ ] Replace the hew stub with spec move 2 (+y is right): wind-up on the fighter's RIGHT (body y +45 at 0.35 s, +55 at 0.50 s), impact and follow-through to the LEFT (body y -30 at 0.60 s, -45 at 0.85 s). Expected: first and last keys 0; impact marker at 0.60 s.
- [ ] `anim_edit.py apply`; restore generated files. Expected: pass, 36 ticks, impact on tick 12.
- [ ] swing_ends.py. Expected: at 0.50 and 0.60 s right_arm abs -15 and right_forearm abs -50 (hands at the hip, not the shoulder); haft horizontal (+-10 deg) at waist height; edge leading to the left; the bit never behind the body at impact.
- [ ] Clearance: bit at least 2 px clear of the body's back at 0.50 s; haft at least 3 px in front of the chest at impact; bit past the left forearm by at least 2 px at 0.85 s.
- [ ] Family hit-over-attack check for hew with hit_body. Expected: maximum under 15.0 deg/tick over attack ticks 6-15.
- [ ] Compare page against video frames 5:29.3-5:32.3 (hew_zoom.png). Expected: matches the spec's flat sweep (deliberately not the video's slam): wind-up right, sweep left at the waist. Wait for Kort.
- [ ] Commit "Swing the axe side hew from the guard, video style".

## Task 5: `axe_haft_jab` (impact 7, 22 ticks) - blocked on Kort

Do not replace the stub until Kort answers spec Question 1 (custom two-handed great-axe model or vanilla axe) and Question 2 (a reference or the designed pose). There is no jab reference on disk. With the vanilla axe, stop and report; the stub keeps the build green.

- [ ] If the great-axe model exists: replace the stub with spec move 3 including the hand-roll columns. Expected: impact marker at 0.35 s (tick 7); first and last keys 0; at impact arm + forearm = -35 (haft within 8 deg of level), butt leading, yaw within 2 deg of forward; no key with the axe head leading.
- [ ] `anim_edit.py apply`; restore generated files. Expected: pass, 22 ticks.
- [ ] Family hit-over-attack check for the jab with hit_body. Expected: maximum under 15.0 deg/tick over attack ticks 6-15 (the clip is 22 ticks, so the whole window is inside it, starting at the impact tick).
- [ ] Compare page. Expected: the butt visibly leads at impact. Wait for Kort.
- [ ] Commit "Jab the axe haft from the guard".

## Task 6: Land it

- [ ] `python tools/clips.py --verify`, then the family hit-over-attack check across all finished moves. Expected: exit 0, each move under 15.0 deg/tick.
- [ ] Restore generated files; `git status` shows only axe clip files and docs.
- [ ] Update `docs/HANDOFF.md` axe line (done moves, jab status). No push.
- [ ] Commit "Land the axe family clips".
