# Bow Animation (video style) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the bow family's stances and shots (`aim`, `bow_direct`, `bow_volley`, optionally archer hits) in the reference video's style, for archer, longbow, bonebow, stalker and wm_hunter. Kort approves each move in game, with no self-clipping, no blend pops, and the bow always facing the target.

**Architecture:** Clip data only. An action clip is added to the stance as an offset (`MotionStack.action`, `Clip.add`), so each shot's keys are deltas from `aim` and the first and last keys of every keyed channel must be 0. Edit keys in `art/clips/biped/bow.bbmodel`, rebuild with `tools/clips.py`, render with the motion-grade harness (clip filter), compare against the video on the local compare page.

**Tech Stack:** Blockbench clip JSON (Python edits), `tools/clips.py`, the scratchpad tools below (absolute paths; `SP` = `C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound-anim-rework/57c0f64e-7fc7-4247-9210-bf7cf820e5af/scratchpad`), `test-tools/work/t16r12/live/run.sh`, and the compare page at localhost:8771.

**Tools (scratchpad `SP`).** `SP/bow_ends.py <clip>...` (written for this plan; a copy of the knight's `swing_ends.py` pointed at `clips/biped/bow/` with impact 0.5 for both shots), `SP/pose_preview.py` (the knight's preview: it draws a sword and shield, so Task 1 makes a bow copy), `SP/mec.sh` (compiles and runs MotionEngineCheck alone), `SP/build_compare.py` (the compare page). The knight's `swing_ends.py` hard-codes the sword_shield folder and impact times and must not be used on bow clips.

**Spec:** `docs/superpowers/specs/2026-11-03-bow-animation-design.md`

## Global Constraints

- Strike timing is approved: `bow_direct` and `bow_volley` keep their `impact` marker at 0.5 s (tick 10), and the `draw` (0.22) and `full` (0.36) markers; `bow_draw` stays as is. Clips stay within `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks (2.0 s). Any timing change needs Kort first.
- Fix clipping in the clip keys only. Mounts and part sizes stay unchanged. A crouch is visual only (root offset, legs unkeyed so foot planting owns the feet).
- Weapon direction is mandatory at every key (wind-up, draw, full, impact, follow-through, recovery): bow upright, belly toward the target, arrow along the line of sight, string hand at the cheek or jaw at full draw; volley arrow 35-45 degrees up. A key whose bow points the wrong way is a defect even if it is clean.
- Do not edit `common/` clips (other families use them) and do not edit other families' clips.
- `python tools/clips.py --verify` must pass. `Spec.java` changes only through `tools/preflight.py`; after any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Limits on running the game and the machine: at most 2 Minecraft instances (Kort's client counts); never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no computer-use on Minecraft; no eula.txt.
- No push, publish or merge to main. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Render checks cover the two bow troops that have a TroopModel: `archer` and `longbow`. `bonebow`, `stalker` and `wm_hunter` draw with vanilla renderers and play no clips; they are not rendered here (spec, Question 5).
- Ask Kort with AskUserQuestion, short and plain. Each move is approved and committed on its own.
- **Commit order.** Always: (1) Task 2 aim, (2) Task 3 bow_direct, (3) Task 4 bow_volley. Contingent: (4) Task 5 fixes, only if the bow hit-over-attack check fails and a shot or hit must change; (5) Task 6 archer hits, only if Kort says yes to Question 1; (6) Task 7 fixes, only if the second-unit checklist fails. Task 1 and Task 8 commit nothing. The scratchpad tools are never committed.

## Review Focus

1. **Start and end in aim.** Every keyed channel's first and last key is 0, or the shot pops in and out (`SP/bow_ends.py`).
2. **Bow faces the target at every key** from front, 3/4 and side, including the follow-through and the volley's tilted-back bow; the string hand sits at the jaw at full draw.
3. **String hand and bow arm through the head, helmet, quiver or cape** at all 8 angles, on archer (chainmail helmet, leather chest) and longbow (leather helmet and chest), the two units with models.
4. **Hit over attack:** MotionEngineCheck "hit over attack" tests the sword_shield thrust only (ticks 26-35) and does not cover 24-tick bow clips; the bow-specific check (Task 5: at most 15 degrees/tick difference over the hit's 10-tick window, four seeds, over the inherited common hits as well as any new ones) must pass; shrink the hits if it does not (the knight's needed about 0.4 size).
5. **Shot into shot and aim pose:** chained shots and the aim hold show no pop (the grading pop flag reads "no"); the loose does not hide the frostshot effect origin.

---

### Task 1: Render, preview and end-key check (work files, not committed)

**Files:**
- Modify: `test-tools/work/t16r12/live/run.sh` (gitignored). `GRAVES_GRADE_CLIPS` per round: the clip being worked plus `aim`.
- Create `SP/bow_preview.py` (this is Step 2 below) from `SP/pose_preview.py`: replace the sword and shield boxes with a bow (a 2 px by 24 px box in the left hand, belly toward +z of the hand) and a thin arrow line from the right hand along the bow's facing; keep `--over aim` (sample aim at 0, add the clip's offset). Preview of the weapon direction cannot be trusted until this copy is drawn and compared with one in-game render.
- Use `python SP/bow_ends.py <clip>...` (FAIL for any keyed channel whose first or last key is over 0.01 from 0, for an `impact` marker other than 0.5, or for a length over 2.0 s).

- [ ] **Step 1:** Run `python SP/bow_ends.py bow_direct bow_volley` on the clips as they stand. Expected: the single line `OK` (checked on 2026-11-03: both clips already start and end at 0, impact 0.5, length 1.2 s). Anything else means the clips changed; record it.
- [ ] **Step 2:** Write `SP/bow_preview.py` (Files above), then `python SP/bow_preview.py bow_direct out.png 0.36 0.5 --over aim`. Expected: a front, 3/4 and side image of the full draw with the bow belly toward the target and the string hand near the jaw; it matches the first in-game render of `bow_direct` at 0.36 closely (same arm angles within a few degrees). If it does not, fix the preview before trusting it.

### Task 2: `aim` stance (video 0:56-0:57)

**Files:** `art/clips/biped/bow.bbmodel` (animation `aim` only); generated `bow/aim.json`.

Target:
- **Hold:** body side-on to the target, a slight root drop (about -1 px) for bent knees (legs unkeyed), bow arm straight at the shoulder, string hand at the cheek, head looking along the arrow. **Weapon direction:** bow upright, belly toward the target, arrow along the line of sight at chest height.
- Loop closes (first key equals last), sway 0.5-1 degree.

Channels that change (spec, aim key table): `root.position` y (about -1 pixel for the crouch; position is in pixels, target keys -0.60, -0.45, -0.60, -0.50, -0.60 at 0, 0.8, 1.7, 2.4, 3.0 s), `head.rotation` x (look along the arrow), and `right_arm` / `right_forearm` only if the string hand misses the jaw. Channels that must not change: `left_leg`, `left_shin`, `right_leg`, `body`, `left_arm` (rotation and its constant position y = 1), `left_forearm`, `left_hand`.

- [ ] **Step 1:** Edit the keys; run `python tools/clips.py`, then `python tools/clips.py --verify`. Expected: no errors, `git status` shows only `art/clips/biped/bow.bbmodel` and `bow/aim.json` changed, and `bow/aim.json` leg keys equal the old values (`git diff` shows no change on left_leg, left_shin, right_leg).
- [ ] **Step 2:** `python SP/bow_preview.py aim out.png 0 1 2` and check bow direction and clipping. Expected: bow upright, belly to the target, arrow along the line of sight at chest height, string hand within 1 px of the cheek, hand not inside the head cube. Iterate.
- [ ] **Step 3:** Render `archer` then `longbow` with `GRAVES_GRADE_CLIPS=aim` (the two units that have models); check 8 angles. Expected: no clipping through the chainmail helmet (archer) or leather helmet (longbow), no pop flag, loop closes.
- [ ] **Step 4:** Rebuild the compare page ("Aim v<N>"), ask Kort to approve. Commit bbmodel and `aim.json`: "Ground the bow aim hold, video style".

### Task 3: `bow_direct` (Kort: slight tilt, video 1:03.0-1:03.9)

**Kort's picks (2026-11-03) override this task where they conflict:** follow the spec's "Kort's picks" phase table. Order is cant 0-0.22, pull and hold 0.22-0.50, loose 0.50, follow-through to 0.62, quiver reach over the right shoulder 0.62-0.78, nock 0.78-0.98, redraw to aim 0.98-1.20. The bow is canted about 25 degrees through left_forearm/left_hand roll from 0.22 through 0.62 and stays on the target line throughout. The old reach keys at 0.10 move to 0.62-0.78. The 0.55-0.86 rows below keep their bow-arm values (on the line, tip 5-8 low); their right-arm values are replaced by the reach and nock. Extra Step 4 checks: the reach hand reaches behind the head at the quiver mouth, not through the helmet or back gear; the arrow tip is never pointing at the archer during the nock; the string looks right at 0.98-1.20 (see the spec's open item). Compare page reference: `frames/kort_slight` f_001-f_010.

**Files:** `art/clips/biped/bow.bbmodel` (animation `bow_direct` only); generated `bow_direct.json`.

Targets, as offsets from `aim` (length 1.2 s, markers 0.22 / 0.36 / 0.5 unchanged). Keys at 0, 0.10, 0.22 and 0.36 stay as in the current JSON (spec, current tables). Change the keys below (degrees x, y, z; copied from the spec's target table):

| t (s) | body | head | left_arm | left_forearm | left_hand | right_arm | right_forearm | Bow direction |
|---|---|---|---|---|---|---|---|---|
| 0.50 (loose) | 0, -8, -2 | -1, 8, 0 | -48, -7, -31 | 34, -14, 24 | 40, -36, -42 | -27, 20, -30 | 44, 3, -59 | on the target line |
| 0.55 | -2, -8, -2 | -1, 8, 0 | -41, -7, -31 | 34, -14, 24 | 40, -36, -42 | 142, 99, 112 | -28, -144, 1 | on the line, tip 5-8 degrees low |
| 0.70 | -2, -8, -2 | -1, 8, 0 | -41, -7, -31 | 34, -14, 24 | 40, -36, -42 | 142, 99, 112 | -28, -144, 1 | held, tip 5-8 degrees low |
| 0.86 | -1, -5, -1 | 0, 5, 0 | -28, -5, -21 | 23, -10, 16 | 27, -24, -28 | 57, 40, 45 | -11, -58, 0 | easing up to the stance angle |
| 1.20 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | in aim |

The old follow-through keys (left_arm -1/-15/-50, left_hand 108/-67/-104) swung the bow off the line; these replace them. Before writing, preview 0.55 and confirm that a more negative left_arm x raises the bow (the sign the dip assumes); if reversed, use x -55 at 0.55-0.70 instead of -41. Timing of the phases follows the spec's retime map (reach 0-0.10, nock 0.10-0.22, draw 0.22-0.36).

- [ ] **Step 1:** Write the keys, keeping times inside 1.2 s and the markers fixed.
- [ ] **Step 2:** `python tools/clips.py`, then `--verify`, then `python SP/bow_ends.py bow_direct`. Expected: no errors, OK, only `bow_direct.json` changed.
- [ ] **Step 3:** `python SP/bow_preview.py bow_direct out.png 0 0.15 0.36 0.5 0.62 1.2 --over aim`; check the bow direction and the string hand against the helmet. Iterate before rendering.
- [ ] **Step 4:** Render `archer` with `GRAVES_GRADE_CLIPS=bow_direct,aim`; check key poses at 8 angles and the pop flag reads "no". Repeat for `longbow` (the only other modelled bow unit).
- [ ] **Step 5:** Rebuild the compare page ("Direct v<N>"), reload, ask Kort to approve or say what to change. Repeat until approved.
- [ ] **Step 6:** Run `bash SP/mec.sh` (MotionEngineCheck alone). Expected: no FAIL lines. Note this gate tests the sword_shield only (the bow is covered by Task 5). Commit: "Draw and loose the bow direct shot from the aim, video style".

### Task 4: `bow_volley` (Kort: full tilt, video 1:05.0-1:06.4, plus the lob)

**Kort's picks override:** same phase order as Task 3. The bow cants about 75 degrees (nearly flat, `frames/kort_full` f_001-f_012) on top of the 35-45 degree lob below. The reach is from f_013-f_015. Extra check: the flat bow's lower limb clears the chest and the left forearm at the lean.

Same steps as Task 3 for `bow_volley` (1.2 s, impact 0.5). Target keys (degrees x, y, z; the arrow 35-45 degrees above the target line: left_arm pitched 30 more than the direct's, body leaning back 8). Keys at 0, 0.10 stay; the 0.22 key stays as today:

| t (s) | body | head | left_arm | left_forearm | left_hand | right_arm | right_forearm | Bow direction |
|---|---|---|---|---|---|---|---|---|
| 0.36 | -8, -8, -2 | -6, 8, 0 | -78, -12, -31 | 36, -18, 26 | 46, -31, -47 | -49, 48, -30 | 47, 19, -71 | arrow about 38 degrees up; trim left_arm x within 10 degrees to land 35-45 |
| 0.50 (loose) | -8, -8, -2 | -6, 8, 0 | -78, -12, -31 | 36, -18, 26 | 46, -31, -47 | -49, 48, -30 | 47, 19, -71 | same |
| 0.55 | -6, -8, -2 | -6, 8, 0 | -71, -12, -31 | 36, -18, 26 | 46, -31, -47 | 164, -24, 107 | -29, -136, -14 | tip 5-8 degrees below the high angle, on the line |
| 0.70 | -6, -8, -2 | -6, 8, 0 | -71, -12, -31 | 36, -18, 26 | 46, -31, -47 | 164, -24, 107 | -29, -136, -14 | held |
| 0.86 | -4, -5, -1 | -2, 5, 0 | -48, -8, -21 | 24, -12, 17 | 32, -22, -33 | 66, -10, 43 | -12, -54, -6 | tip easing down to the stance angle, not past it |
| 1.20 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | in aim |

Right_arm x rises 18 degrees at full draw so the string hand follows the arrow up; check the elbow against the helmet.

Step 4 also checks the left forearm against the chest and the helmet at the lean, on archer and longbow, and measures the arrow's elevation at 0.36 in the preview (Expected: 35-45 degrees above the target line; today's keys give a smaller lob, so raise the left arm pitch and the chest lean).

Commit message: "Lob the bow volley from the aim, video style".

### Task 5: Bow hit-over-attack check (runs whether or not hits are added)

`MotionEngineCheck.hitOverAttack` (line 1093, helper `hitWhileAttacking` at line 1057) is built on `realSwordShield` (line 463): the sword_shield family, stance `hold`, shield on, attack at tick 20, body hit at tick 25, window ticks 26-35. It never loads a bow clip, so it neither covers the bow family nor is affected by it. The bows inherit the `common/` hits, which move the arms over the shot, so this check is needed even if no bow hit is ever written.

**Files (scratchpad only, not committed):** recreate from the knight's work: `SP/mec.sh` (already compiles MotionEngineCheck and honours `EXTRA` and `MAIN`) plus a new `SP/BowHitOverCheck.java` in package `gg.gravesandglory` (copy `hitWhileAttacking` and `realSwordShield` from `test-tools/standalone/MotionEngineCheck.java`, which hold the Drive and Family setup). Run: `EXTRA=SP/BowHitOverCheck.java MAIN=BowHitOverCheck bash SP/mec.sh`.

Definition:
- **Inputs:** attacks `bow_direct` (index 0) and `bow_volley` (index 1) from `clips/biped/bow/`; hit clips `hit_body`, `hit_head`, `hit_legs` taken from `clips/biped/bow/` if present, else from `clips/biped/common/` (so the inherited ones are tested now); four seeds from `seeds(4, 4100)`; stance `aim` (not `hold`), shield false, enemy true; the attack starts at tick 20; the hit lands at attack tick k for k in {2, 5, 10, 14} (draw, full draw, impact, follow-through).
- **Per case:** the same measurement as `hitWhileAttacking` (sum over bones of the absolute change of the 3 rotation channels per 0.05 s tick), once with the hit and once without.
- **Window:** the 10 ticks from the tick after the hit lands (hit tick + 1 to hit tick + 10), the same width as the existing 26-35.
- **Limit:** at every tick of the window, with minus without is at most 15 degrees. Also the existing hit-over-attack structure checks: after the hit the attack is the only track, one overlay, overlay weight under 0.5 one tick in, ease-in at least 0.15 s.
- **Expected line:** `bow hit over attack: 3 hits x 2 shots x 4 landings x 4 seeds, worst N.N deg/tick (want 15 or less)` with N.N at most 15.0, and no `FAIL`. If it fails, name the case (shot, hit, landing tick) and shrink the hit (a bow hit set, Task 6) or leave it to Kort as a Question; do not edit `common/`.

- [ ] **Step 1:** Write `SP/BowHitOverCheck.java` per the definition; sanity check it by running the sword_shield case first and confirming the same worst value as `SP/mec.sh` prints for `hit over attack`.
- [ ] **Step 2:** Run it over the inherited `common/` hits. Expected: the line above, worst at most 15.0.
- [ ] **Step 3:** Run it again after Task 6 (if it happens) and after any clip change. Run `SP/mec.sh` too; Expected: no FAIL lines.
- [ ] **Step 4 (contingent):** If a case failed and a shot key had to change, commit that: "Keep the bow shots steady under a hit".

### Task 6 (contingent: only if Kort says yes to Question 1): archer hits

**Files:** new `hit_body`, `hit_head`, `hit_legs` (+ `_2`, `_3`) in `art/clips/biped/bow.bbmodel`; generated `bow/hit_*.json`. They override `common/` for this family only.

Target: a recoil that keeps the left arm and bow on the target line (bow: still facing the target at every key) and moves the torso and head. Commit (contingent): "Keep the bow up through archer hits".

- [ ] **Step 1:** Write the keys; `tools/clips.py`, `--verify`, and check the ends of every hit clip with a copy of `SP/bow_ends.py` that skips the impact test.
- [ ] **Step 2:** Render with `GRAVES_GRADE_CLIPS=hit_body,hit_head,hit_legs,aim`; check 8 angles; ask Kort.
- [ ] **Step 3:** Re-run Task 5's check (it now picks up the new hits). Expected: worst at most 15.0. Shrink the hits if not (the knight's needed about 0.4 size).
- [ ] **Step 4:** Commit.

### Task 7: Check the second unit and the loadouts

- [ ] **Step 1:** Render `longbow` with `GRAVES_GRADE_CLIPS=aim,bow_direct,bow_volley` (and the hits if Task 6 ran); `archer` was done in Tasks 2-4. Check every key at 8 angles. Expected checklist, all must hold: (a) bow faces the target at every key, front, 3/4 and side; (b) string hand at the jaw at 0.36 and 0.50 and not inside the leather helmet; (c) no part of the left arm or bow inside the head or chest at the volley lean; (d) the reach at 0.10 clears the cape and quiver; (e) pop flag reads no; (f) the loose snaps (string hand moves past the jaw by 0.55).
- [ ] **Step 2:** If anything fails the checklist, adjust that move's keys, re-run `python tools/clips.py --verify` and `python SP/bow_ends.py bow_direct bow_volley`, re-render `archer`. Show Kort again if the change is visible.
- [ ] **Step 3:** Check the `steady` (archer) and `pierce_longbow` (longbow) effect origins are visible at the loose. The frostshot (stalker) and ambush (wm_hunter) visuals need models first and are not checked.
- [ ] **Step 4:** If Step 2 changed any clip (contingent commit), commit the fixes: "Clear the bow clips of the longbow clipping and the volley lean". If nothing changed, say so in the ledger.

### Task 8: Land it

- [ ] **Step 1:** Run `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background. Expected: PASS. Afterwards `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` if they changed.
- [ ] **Step 2:** Confirm the Task 7 fixes are committed (if there were any) (message above). Write the ledger. Tell the coordinator the new head of `work/anim-rework`.
- [ ] **Step 3:** Ask Kort which comes next: a `carry` clip, a bow death, or the volley launch-pitch question (Question 2; protocol change, needs approval).
