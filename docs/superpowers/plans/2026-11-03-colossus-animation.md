# Colossus Animation Implementation Plan

> For agentic workers: do one task at a time, one writer per checkout. Spec: `docs/superpowers/specs/2026-11-03-colossus-animation-design.md` (key tables are the targets). Refs: `C:/Users/kortt/Melty/anim-refs/colossus/` (not committed).

**Goal:** Give `ic_colossus` (rig `colossus`, family `colossus`) stances idle/guard, attacks `colossus_slam` and `colossus_backhand`, and three small hits, in the Souls-like golem style.

**Architecture:** Clips are additive offsets over the stance. JSON is generated from `art/clips/colossus/<family>.bbmodel` by `tools/clips.py`, edited only through `tools/anim_edit.py open/apply`. `TroopMotion.build("colossus", "colossus")` merges `clips/colossus/common` with `clips/colossus/colossus`.

## Global Constraints

- Docs only was the spec run; this plan edits clips, tools and tests. Never hand-author clip JSON.
- Attack rows fixed: `colossus_slam` head blunt impact tick 12, `colossus_backhand` body blunt tick 8. Clip length at most 40 ticks. Changes need Kort.
- First and last keys of every keyed channel are 0. Key only `root`, `body`, `head`, `right_arm`, `left_arm` (no forearms or shins on this rig).
- Weapon direction rule: every key states where the fists/knuckles point (spec tables). A key without a direction is a defect. Check signs against shipped `sword_shield` clips: +y turns right, root y+ is down, root z- is forward.
- After any verify/gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`. Expected `git status` always includes the already-modified `art/clips/biped/sword_shield.bbmodel`, `sheets/preflight.json`, `Spec.java` plus the untracked doc/hit files from other families; do not stage them.
- Max 2 Minecraft instances. Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No eula.txt. No computer-use on Minecraft. No push, merge or publish.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Scratch tools (recreate from the knight plan if missing): `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad/` (`pose_preview.py`, `mec.sh`, `build_compare.py`, `colossus_fist.py`).

## Review Focus

1. Do the fists lead at every key, and does the weapon-direction column follow from the summed pitch (body x + arm x)?
2. Slam impact lands on tick 12 (0.60 s) with the fists descending; backhand on tick 8 (0.40 s) with the back of the hand leading.
3. No arm through the head or chest, no fist below the ground at the follow-through.
4. Hits stay small and pass the hit-over-attack check (at most 15 deg/tick in the window).
5. Nothing claimed beyond what is verified: gait on the golem legs and the status-4 double play are checked in Task 1.

## Command reference (used by the tasks)

- `$S` = `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad`. Repo root = `C:/Users/kortt/Melty/bannerbound-anim-rework`.
- Edit clips: `python tools/anim_edit.py open colossus` (edit in Blockbench), then `python tools/anim_edit.py apply colossus` (hits: `apply common`; TROOP is keyed by file stem, see Task 1 Step 1). Generates the JSON via `tools/clips.py`.
- Render: add the case lines below to `test-tools/work/t16r12/live/run.sh` (Task 1 Step 5), then run `bash test-tools/work/t16r12/live/run.sh <tag> <case>` (waits for a free client slot, at most 2 clients):
  `solo_colossus) go solo_colossus GRAVES_PHASE2_CHECKS=motion-grade GRAVES_MOTION_FAMILY=colossus GRAVES_GRADE_TROOP=ic_colossus GRAVES_GRADE_CLIPS=colossus_slam,colossus_backhand GRAVES_GRADE_OUT=$B/solo_colossus/grade GRAVES_GRADE_PARTS=solo ;;`
  `st_colossus`: the same line with `GRAVES_GRADE_CLIPS=idle,guard`. `solo_chits`: `GRAVES_GRADE_CLIPS=hit_body,hit_head,hit_legs`. Sheets land in `test-tools/work/t16r12/live/<tag>/<case>/grade/<timestamp>/`.
- Compare: copy `$S/build_compare.py` to `$S/build_compare_colossus.py`, set `REF` to `C:/Users/kortt/Melty/anim-refs/colossus/frames`, `OUT` to `C:/Users/kortt/Melty/anim-refs/colossus/compare`, and `MOVES` to the colossus rows (slam vs `slam_front02`, idle vs `idle_stance`, hits vs `hit_shoulder`, backhand with no reference). Run `python $S/build_compare_colossus.py <grade-run-dir> "Colossus v<N>"`, then `python -m http.server 8771 --directory C:/Users/kortt/Melty/anim-refs/colossus/compare` and open `http://localhost:8771/`.
- Check: `EXTRA="test-tools/standalone/ColossusMotionCheck.java" MAIN=ColossusMotionCheck bash $S/mec.sh` (mec.sh also compiles `MotionEngineCheck.java`, so the new class reuses its package-private `Drive`, `seeds`, `check`).

## Task 1: Troop model and pipeline (needs coordinator)

Nothing shows in game until `ic_colossus` has a troop model. Blocked until the coordinator and Kort settle spec Question 2 (look).

**Files:** Modify `tools/anim_edit.py`, `test-tools/work/t16r12/live/run.sh`. Create `art/troops/ic_colossus.json`, skins via `tools/build_skins.py`, an entry in `src/main/resources/assets/gravesandglory/models/troop/index.json`, `art/clips/colossus/colossus.bbmodel`, `art/clips/colossus/common.bbmodel`.

- [ ] Step 1 (coordinator work): refactor `tools/anim_edit.py`. Today `RIG = 'biped'` and `SRC = ROOT / 'art/clips' / RIG` (lines 26-27) are module constants and `semantic(project)` (line ~90) loads `art/rigs/biped.json`. Change `TROOP` to be keyed by (rig, file stem) so the stem always equals `art/clips/<rig>/<stem>.bbmodel`: `('biped','sword_shield'): 'knight'`, `('biped','bow'): 'archer'`, `('biped','common'): 'knight'`, `('colossus','colossus'): 'ic_colossus'`, `('colossus','common'): 'ic_colossus'`. The command line takes `[rig/]stem` (`open colossus` and `open colossus/common`; a bare stem that exists for one rig only, such as `sword_shield`, resolves to it; `common` is ambiguous and needs `biped/common` or `colossus/common`). `working()` names files `<rig>-<stem>-<troop>.bbmodel` and `cmd_apply` globs the same pattern, so the colossus and biped `common` files never collide. Make `SRC` a function `src(rig)` returning `ROOT/'art/clips'/rig`, make `semantic(project, rig)` load `art/rigs/<rig>.json`, and thread `rig` through `cmd_open`, `cmd_apply` and `cmd_preview` (`TROOP.get(family, family)` becomes the tuple lookup). Expected: `python tools/anim_edit.py apply sword_shield` changes no generated JSON (`git status --short src/main/resources/assets/gravesandglory/clips/biped` shows only the already-untracked hit files); `python tools/anim_edit.py open colossus` exits 0 (today: exit 2) once Step 3 has seeded the bbmodel.
- [ ] Step 2: add the troop model and index entry, then `python tools/build_skins.py`. Expected: exit 0 and `ic_colossus` in index.json.
- [ ] Step 3: seed the source files, because `cmd_open` calls `load(SRC/(stem+'.bbmodel'))` before `mesh()` and fails when the file is missing. With a one-off script (kept in `$S/seed_colossus.py`, not committed) call `anim_edit.mesh('ic_colossus')` (it builds the project from `art/troops/ic_colossus.json` through `build_skins.troop_project`, so Step 2 must be done), set `project['animations'] = []`, and `save(...)` it to both `art/clips/colossus/colossus.bbmodel` and `art/clips/colossus/common.bbmodel`. Groups are present (body, head, right_arm, left_arm, right_leg, left_leg), there are no forearm or shin groups, and `cmd_apply` later remaps edited animations onto these source groups by name. Expected: `python tools/anim_edit.py open colossus` prints `wrote work/anim-edit/colossus-colossus-ic_colossus.bbmodel (0 clips)` (file name per Step 1) and exits 0; `python tools/clips.py` accepts both files (no bone-name errors) and its only failures are the missing clips for `colossus_slam` and `colossus_backhand`.
- [ ] Step 4: add zero-motion stubs for both attacks with impact markers at 0.60 s and 0.40 s, then `python tools/anim_edit.py apply colossus`. Expected: `python tools/clips.py` exits 0 and `python tools/clips.py --verify` passes (impact ticks 12 and 8).
- [ ] Step 5: add the `solo_colossus`, `st_colossus` and `solo_chits` case lines (Command reference) to run.sh, then `bash test-tools/work/t16r12/live/run.sh colossus0 solo_colossus`. Expected: sheets exist; read them for (a) the procedural walk on the golem legs, (b) whether the vanilla status-4 swing plays together with the clip. If status 4 doubles the swing, report to the coordinator; do not patch Java here.
- [ ] Step 6: commit `Add ic_colossus troop model and colossus clip pipeline`, trailer as above.

## Task 2: ColossusMotionCheck (hit-over-attack for this family)

`MotionEngineCheck.hitOverAttack` (~1093) and `hitWhileAttacking` (~1057) use only `realSwordShield` with `d.shield = true`; they never play colossus clips. Attack starts at tick 20, hit at tick 25, so the window ticks 26-35 = attack ticks 6-15, covering both impacts (12 and 8).

**Files:** Create `test-tools/standalone/ColossusMotionCheck.java` (package `gg.gravesandglory`).

- [ ] Step 1: write the class. Family builder `realColossus(Map<String,Clip> all)`: copy `realSwordShield` (MotionEngineCheck.java:463) with `CLIPS = "src/main/resources/assets/gravesandglory/clips/colossus/"`, dirs `List.of("common","colossus")`, loading with `Files.list(Path.of(...))` sorted and `Clip.parse(Files.readString(p))`; stances from `idle, carry, guard, brace, wall, shield, aim, rest` that exist; attacks in order `colossus_slam` (index 0), `colossus_backhand` (index 1); the same hit and death regexes; finish with `new MotionEngine.Family("colossus", stances, attacks, wall, hits, deaths)`. It does not use `ClipLibrary`, `Tactics`, `TroopMotion` or `TroopModel` (mec.sh cannot compile them), so `row(m)` being null for a rig-only model never arises: the engine runs on the Family directly. Fixture: `Drive d = new Drive(seed); d.stance = "hold"; d.shield = false; d.enemy = true;` (the stance key is then `guard`, MotionEngine.stanceKey lines 457-464), attack at f == 20 via `new MotionInput.Attack(index, d.tick())`, body hit at f == 25, 60 ticks with and without the hit, both attack indexes, 4 seeds (`MotionEngineCheck.seeds(4, 4100)`).
- [ ] Step 2: Task 1 Step 4 already made the zero-motion stubs, so run it right after writing the class (Task 1 done, Tasks 3-6 not). Expected: compiles; the two attack clips exist but are empty, so every hit-over-attack check passes trivially (X.X about 0) and the run proves only that the fixture loads the family; the stances `idle`/`guard` are absent until Task 3, so the engine falls back to its procedural stance and the output line carries no failure. Any `NullPointerException` or `AssertionError` here is a fixture bug.
- [ ] Step 3: rerun after each of Tasks 4, 5 and 6 (these are the real gates). Expected last line: `colossus hit over attack: the attack keeps playing; the hit adds at most X.X deg/tick` with X.X at most 15 over attack ticks 6-15, for the slam and the backhand, and no `check` failure lines. Before Task 6 the hits do not exist, so the hit overlay is empty: Task 6 Step 2 is the first run that tests a hit.
- [ ] Step 4: commit `Add colossus hit-over-attack check`.

## Task 3: Stances idle and guard

**Files:** `art/clips/colossus/colossus.bbmodel`, generated `clips/colossus/colossus/{idle,guard}.json`.

Numbers (absolute rig-file pose, degrees; arm abs pitch = body x + arm x):

| Clip | Length / keys | body | head | right_arm | left_arm | root | Fists |
|---|---|---|---|---|---|---|---|
| `idle` loop | 4.0 s, keys 0 / 2 / 4 | x +4 / +5 / +4 (breathing), y sway -3 / +3 / -3 | x -3 | x -5 / -11 / -5, y -2 | x -5 / -11 / -5, y +2 | 0 | bars about 82 below horizontal (abs -4), fists about 1 px ahead of the hips, knuckles down |
| `guard` loop | 2.4 s, keys 0 / 1.2 / 2.4 | x +10 +- 1.5 (11.5 at 1.2) | x -8 | x -35, y -6 | x -35, y +6 | y +0.8 | bars 65 below horizontal (abs -25), fists 14 px ahead of the hips and 5.8 px above the ground, knuckles forward-down |

- [ ] Step 1: `python tools/anim_edit.py open colossus`, key both loops, `python tools/anim_edit.py apply colossus`. Expected: `python tools/clips.py --verify` passes and both loops close (first key equals last).
- [ ] Step 2: `bash test-tools/work/t16r12/live/run.sh colossus1 st_colossus`. Expected checklist: arms hang forward; no arm in the chest; feet planted; head level; guard visibly crouched forward versus idle; guard fists above the ground.
- [ ] Step 3: `python $S/build_compare_colossus.py test-tools/work/t16r12/live/colossus1/st_colossus/grade "Colossus stances"` and serve (Command reference). Expected: idle (ref `idle_stance`) next to guard at `http://localhost:8771/`.
- [ ] Step 4: commit `Colossus idle and guard stances`.

## Task 4: colossus_slam

Keys (spec "colossus_slam keys", both arms identical, deltas from guard): 0.00 all 0 (th -25, knuckles down-forward); 0.16 body -10, head +6, arms -80, root y -0.3 z +0.4 (th -115, knuckles forward-up); 0.30 body -22, head +10, arms -130, root y -0.6 z +0.8 (th -177, bars straight up, knuckles to the sky); 0.34 identical to 0.30 (top hold, same knuckles to the sky); 0.45 body +4, head 0, arms -90, root y +0.6 z -0.6 (th -111, descending); 0.60 IMPACT body +22, head -14, arms -72, root y +1.8 z -2.0 (th -75, bars 15 below horizontal, knuckles leading, fists about 19 px above the ground at absolute root y +2.6); 0.78 body +28, head -18, arms -35, root y +1.6 z -2.4 (th -32, fists about 2.1 px above the ground at absolute root y +2.4, knuckles down); 1.20 body +12, head -6, arms 0 (abs -35, th -13), root y +0.4 z -1.6 (fists about 1.9 px above the ground); 1.60 all 0.

- [ ] Step 1: key it in `python tools/anim_edit.py open colossus`, set the impact marker at 0.60 s, `python tools/anim_edit.py apply colossus`. Expected: `python tools/clips.py --verify` passes (impact tick 12, length 32 ticks, at most 40).
- [ ] Step 2: `bash test-tools/work/t16r12/live/run.sh colossus2 solo_colossus`. Expected checklist: fists straight up at 0.30 and 0.34 and at least 5 px clear of the head; fists descending at 0.60, about 19 px above the ground, knuckles leading; ground clearance greater than 0 px at 0.78 (about 2.1 px) and at 1.20 (about 1.9 px): check the side view and confirm no fist or sole below the ground plane (root deltas stack on guard's +0.8; if it touches, lower the 0.78 root delta to +1.2); knuckles lead at every key.
- [ ] Step 3: `python $S/build_compare_colossus.py test-tools/work/t16r12/live/colossus2/solo_colossus/grade "Colossus slam"` and serve on 8771. Expected: Kort approves or lists changes; do not proceed on rejection.
- [ ] Step 4: `EXTRA="test-tools/standalone/ColossusMotionCheck.java" MAIN=ColossusMotionCheck bash $S/mec.sh`. Expected: slam section at most 15 deg/tick, attack ticks 6-15.
- [ ] Step 5: after approval commit `Colossus slam`.

## Task 5: colossus_backhand (designed, no reference)

No video shows a backhand; keys come from the rig (spec Q1 still asks Kort for a link). Keys (deltas from guard; right-arm abs th = body x (10 + d) + arm x (-35 + d); absolute world yaw = body y + (-6 + arm y delta), giving -58 at 0.20, -23 at 0.30, +13 at 0.40, +50 at 0.55, +14 at 0.85; root deltas stack on guard's +0.8): 0.20 load body x +4, y -42, head y +30, R arm x -49 y -10, L arm x +20, root y +0.8 z +0.6 (th -70, bar 20 below horizontal across the front at absolute yaw -58, fist about 19.5 px above the ground, back of the hand to the right); 0.30 body x +3 y -14, head y +12, R arm x -48 y -3, L +22, root y +0.9 z -0.3; 0.40 IMPACT body x +2 y +14, head y -6, R arm x -47 y +5, L +25, root y +1.0 z -1.2 (absolute yaw +13: arm forward-right, back of the hand and outer face leading, fist about 19.5 px above the ground); 0.55 body x +4 y +46, head y -20, R arm x -45 y +10, L +30, root y +1.0 z -1.6 (absolute yaw +50, arm out right); 0.85 body x +2 y +20, head y -8, R arm x -30, L +10, root y +0.5 z -0.8; 1.30 all 0.

- [ ] Step 1: key it, impact marker 0.40 s, `python tools/anim_edit.py apply colossus`. Expected: `python tools/clips.py --verify` passes (tick 8, length 26 ticks).
- [ ] Step 2: `bash test-tools/work/t16r12/live/run.sh colossus3 solo_colossus`. Expected checklist: arm clears the chest at 0.20 (else R arm y -6); the arm sweeps flat in plan; back of the hand leads; fist about 19.5 px above the ground at impact; sweep direction matches `sword_shield_low_cut.json` signs (+y turns right).
- [ ] Step 3: `python $S/build_compare_colossus.py test-tools/work/t16r12/live/colossus3/solo_colossus/grade "Colossus backhand"` and serve on 8771. Expected: Kort approval.
- [ ] Step 4: run the check (Task 4 Step 4 command). Expected: backhand section at most 15 deg/tick, attack ticks 6-15.
- [ ] Step 5: commit `Colossus backhand` after approval.

## Task 6: Hits

**Files:** `art/clips/colossus/common.bbmodel` (file stem `common`, matching the TROOP key `('colossus','common')`), generated `clips/colossus/common/hit_{body,head,legs}.json`. Key tables are in the spec ("Hits"): all 0.45 s, mid at 0.05, settle at 0.25, end 0. body (body x +5 / +1.5, head x +4 / +1, root z +0.4 / +0.1); head (body x -3 / -1, head x -8 / -2.5); legs (body x +4 / +1.2, root y +0.8 / +0.25). Arms unkeyed.

- [ ] Step 1: `python tools/anim_edit.py open colossus/common`, key them, `python tools/anim_edit.py apply colossus/common`. Expected: `python tools/clips.py --verify` passes.
- [ ] Step 2: `EXTRA="test-tools/standalone/ColossusMotionCheck.java" MAIN=ColossusMotionCheck bash $S/mec.sh`. Expected: hit-over-attack at most 15 deg/tick with the hits present; if not, scale the hit amplitudes down (the knight needed about 0.4) and rerun.
- [ ] Step 3: `bash test-tools/work/t16r12/live/run.sh colossus4 solo_chits`. Expected: one frame per hit; the golem flinches and the arms do not swing.
- [ ] Step 4: commit `Colossus hit reactions`.

## Task 7: Final checks for the family

Family troops: `Spec.TROOPS` has the single row `ic_colossus`; its level ids `ic_colossus_2` and `ic_colossus_3` (`Spec.LEVELABILITIES`) use the same model and clips.

- [ ] Step 1: `python tools/clips.py` then `python tools/clips.py --verify`. Expected: exit 0.
- [ ] Step 2: `bash $S/mec.sh` (existing check) and the Colossus check (Command reference). Expected: both pass, old check unchanged.
- [ ] Step 3: close all Minecraft clients, then `python tools/verify.py game --offline`. Expected: pass.
- [ ] Step 4: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`, then `git status --short`. Expected: only the files listed in Global Constraints.
- [ ] Step 5: update `docs/HANDOFF.md` with the colossus state and open questions (spec Questions 1-5). Commit `Colossus animation family complete`.
