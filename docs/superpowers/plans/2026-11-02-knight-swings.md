# Knight Swings From the Guard (video style) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the three loose sword_shield swings (`sword_shield_overhead`, `sword_shield_low_cut`, `sword_shield_thrust`) in the video's style, thrown from and returning to the new guard. Kort approves each swing in game, with no self-clipping and no blend pops.

**Architecture:** Clip data only. An action clip is added to the stance as an offset (`MotionStack.action`, `Clip.add`). Each swing's keys are therefore deltas from the guard pose, and the first and last keys of every keyed channel must be 0, so a swing starts and ends exactly in guard. Edit the keys in `art/clips/biped/sword_shield.bbmodel`, rebuild with `tools/clips.py`, render with the motion-grade harness (clip filter), and compare against the video on the local compare page. The `*_wall` swings are not touched.

**Tech Stack:** Blockbench clip JSON (Python edits; scratchpad `guard_edit.py` pattern), `tools/clips.py`, scratchpad `pose_preview.py` (guard + swing summed), `test-tools/work/t16r12/live/run.sh`, scratchpad `build_compare.py` at localhost:8771.

**Spec:** `docs/superpowers/specs/2026-11-02-knight-video-style-design.md`

## Global Constraints

- Strike timing is approved:
  - Each `impact` marker stays where it is: overhead 0.5 s (10 ticks), low cut 0.4 s (8), thrust 0.35 s (7).
  - Clip lengths stay within `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks (2.0 s).
  - Any timing change needs Kort first.
- Formation (`wall`, `brace`, `*_wall`) is untouched.
- Fix clipping in the clip keys only. Mounts and part sizes stay unchanged. The crouch and lunge are visual only.
- `python tools/clips.py --verify` must pass. `Spec.java` changes only through `tools/preflight.py`.
- Limits on running the game and touching the machine:
  - At most 2 Minecraft instances; Kort's client counts.
  - Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`.
  - No computer-use on Minecraft. No eula.txt.
- No push, publish or merge to main. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Pose checks cover all three troops: `knight`, `sentinel` and `gl_marshal`.
- Ask Kort with AskUserQuestion, short and plain. Each swing is approved and committed on its own.

## Review Focus

1. **Start and end in guard.** Every keyed channel's first and last key is 0, or the swing pops in and out. Task 1 adds a scratch check, `swing_ends.py`, that each swing task runs.
2. **Hand-back to planting.** A swing that keys the legs takes the feet from `MotionFeet` (legShare 1). Do the step-in with a root offset only and leave the legs unkeyed, so planted feet bend the knees. Add leg keys only if Kort asks for a real step; the render's `contact` sheet must then show no foot slide.
3. **Blade through the body or a leg at the wind-up and follow-through extremes**, at all 8 angles, on the bulkier sentinel and marshal as well as the knight.
4. **Swing into swing.** Chained attacks (`transitions` part, attack-uninterrupted) must not pop: the grading pop flag reads "no" for every loose swing.
5. **Thrust blade drift:** at impact the blade points at the target, within about 2° of the body's forward line (spec: fixes the known 2–10° outward drift; 408dc109 angled it 6° outward to offset that drift; re-measure in the render).

---

### Task 1: Render, preview and end-key check (work files, not committed)

**Files:**
- Modify: `test-tools/work/t16r12/live/run.sh` (gitignored). Add a `GRAVES_GRADE_CLIPS` value per round: the swing being worked plus `guard`.
- Modify: scratchpad `pose_preview.py`. Add a `--over guard` option that samples guard at time 0 and adds the swing's offset, so a swing is previewed as it plays in game.
- Create: scratchpad `swing_ends.py <clip>...`. It loads each built clip JSON and prints FAIL for any keyed channel whose first or last key is not within 0.01 of 0, and for any `impact` marker other than 0.5, 0.4 or 0.35. Otherwise it prints OK.

- [ ] **Step 1:** Write `swing_ends.py` and run it on the three swings as they stand. Expected: it reports what the current clips do. The old offsets may not end at 0; if so, note this in the ledger.
- [ ] **Step 2:** Add `--over` to `pose_preview.py`. Preview `sword_shield_thrust --over guard` at its impact time. Expected: the summed pose matches the current in-game thrust from the last render (`guardv1c`), which confirms the offset model.

### Task 2: Overhead (video 0:22–0:26)

**Files:** `art/clips/biped/sword_shield.bbmodel` (animation `sword_shield_overhead` only); generated `sword_shield_overhead.json`.

The target, as poses summed onto the guard:
- **Wind-up (before 0.5 s):** sword raised high behind the head, body turned back on the sword side, shield still in front of the chest.
- **Impact (0.5 s):** the blade comes down across the body, hips and shoulders turn into the blow, and the root moves forward about 1–2 px (step-in, Review Focus 2).
- **Follow-through, then recovery to all zeros by 1.2 s.**

Exact angles and easing are the builder's call.

- [ ] **Step 1:** Write the keys. Keep the times inside 1.2 s and the impact marker at 0.5.
- [ ] **Step 2:** Run `python tools/clips.py`, then `--verify`, then `swing_ends.py sword_shield_overhead`. Expected: no errors, OK, and only `sword_shield_overhead.json` changed.
- [ ] **Step 3:** Use `pose_preview.py sword_shield_overhead out.png 0 0.25 0.5 0.8 1.2 --over guard` to check for the blade through the head or legs. Iterate here before rendering.
- [ ] **Step 4:** Render the knight in the background with `GRAVES_GRADE_CLIPS=sword_shield_overhead,guard`. Check the following and fix them before showing Kort:
  - Wind-up, impact and follow-through keys at all 8 angles.
  - The `transitions` pop flag for overhead reads "no".
- [ ] **Step 5:** Rebuild the compare page ("Overhead v<N>"), reload tab-5, and ask Kort to approve or say what to change. Repeat until approved.
- [ ] **Step 6:** Run `bash <scratchpad>/mec.sh` (MotionEngineCheck alone). Expected: no FAIL lines. Commit the bbmodel and `sword_shield_overhead.json`: "Swing the sword-and-shield overhead from the guard, video style".

### Task 3: Low cut (video 0:15.5–0:19)

Same steps as Task 2, for `sword_shield_low_cut`: length 1.1 s, impact 0.4.

The target is a side slash driven by a whole-body turn:
- **Wind-up:** sword drawn back low on the sword side, hips turned away.
- **Impact:** a horizontal cut at waist to knee height across the front, with the hips leading.
- **Follow-through** past the shield side, while the shield stays up.

Commit message: "Swing the sword-and-shield low cut from the guard, video style".

### Task 4: Thrust (video 0:44.5–0:49)

Same steps as Task 2, for `sword_shield_thrust`: length 1.25 s, impact 0.35.

The target is a lunge:
- **Wind-up:** sword drawn back by the hip, point forward.
- **Impact:** the arm drives straight out, the root moves forward 2–3 px, and the body leans in.
- **Recovery** back to guard.

Step 4 also measures the blade's yaw against the body's forward line at impact, from the 3/4 and top-down renders. Expected: within about 2° (Review Focus 5).

Commit message: "Lunge the sword-and-shield thrust from the guard, video style".

### Task 5: Templar and Lord Marshal check

- [ ] **Step 1:** Render `solo_sentinel`, then `solo_marshal`, one at a time, with `GRAVES_GRADE_CLIPS=sword_shield_overhead,sword_shield_low_cut,sword_shield_thrust`.
- [ ] **Step 2:** Check every swing's key poses at 8 angles. If anything clips, adjust that swing's keys, re-verify and re-render the knight. Show Kort again if the change is visible.

### Task 6: Land it

- [ ] **Step 1:** Run `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` in the background. Expected: PASS. Afterwards, `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java` if they changed.
- [ ] **Step 2:** Commit any Task 5 fixes. Write the ledger. Tell the coordinator the new head of `work/anim-rework`; if it is gone, leave a note in the shared notes folder.
- [ ] **Step 3:** Ask Kort which comes next: hits, guarded advance or taunt.
