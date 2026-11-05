# Knight Guard Stance (video style) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the sword_shield `guard` stance as the video's low, shield-forward ready pose, with no self-clipping, approved by Kort in game.

**Architecture:** Clip data only. Edit the `guard` keys in `art/clips/biped/sword_shield.bbmodel`, rebuild the clip JSON with `tools/clips.py`, render with the motion-grade harness, and compare against the video on the local compare page. `idle` is only checked for clipping. No Java changes.

**Tech Stack:** Blockbench clip JSON (Python edits), `tools/clips.py`, Fabric motion-grade harness (`test-tools/work/t16r12/live/run.sh`), ffmpeg frames, and the scratchpad compare page at localhost:8771.

**Spec:** `docs/superpowers/specs/2026-11-02-knight-video-style-design.md`

## Global Constraints

- Formation (`wall`, `brace`, `*_wall`) stays tight and upright; clipping fixes only.
- `idle` stays relaxed and upright; the video ready pose is used in `guard` only.
- Fix clipping in the clip keys only. Shield and sword mounts and part sizes stay unchanged.
- The crouch is visual only. Hitbox, eye height and gameplay stay unchanged.
- `python tools/clips.py --verify` must pass. `Spec.java` changes only through `tools/preflight.py`.
- At most 2 Minecraft instances (Kort's client counts). Never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`. No computer-use on Minecraft. No eula.txt.
- No push, publish or merge to main. Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Pose checks use all three sword_shield troops: `knight` (Order Squire), `sentinel` (Templar) and `gl_marshal` (Lord Marshal Aldric).

## Review Focus

1. **Guard to swing blend:** the swings are still upright until their own milestones, so going from guard into a swing must not pop or pass the blade through the body. Task 2 renders the `transitions` part.
2. **Guard while moving:** stance `hold` at the crawl pace (the `guard-shuffle` loco segment) uses the new pose with legs walking. The crouch must not make feet sink or float. Task 1 adds that segment to the render.
3. **Shield against the head at the extreme of the loop:** the breathing keys move the shield arm. Check `k_extreme_a` and `k_extreme_b` at all 8 angles, not only frame 0.
4. **Bulkier troops:** the Templar and Lord Marshal may have bigger shields or pauldrons that clip where the knight does not. Task 3 covers them.
5. **Loop seam:** the last guard key must equal the first (clips.py checks within 0.01). Re-run `--verify` after every edit.

---

### Task 1: Render and compare setup (work files, not committed)

**Files:**
- Modify: `test-tools/work/t16r12/live/run.sh` (gitignored), `solo_knight` case
- Modify: scratchpad `build_compare.py`

- [ ] **Step 1:** In `run.sh`, add `transitions` to the `solo_knight` parts list (`GRAVES_GRADE_PARTS=solo,loco,transitions`) and set `GRAVES_GRADE_LOCO=walk,guard-shuffle`. Add `solo_sentinel` and `solo_marshal` cases that are the same apart from `GRAVES_GRADE_TROOP=sentinel` / `gl_marshal` and their output dirs, with parts `solo` only.
- [ ] **Step 2:** In `build_compare.py`, autocrop each game frame to the troop. Use one PIL bounding box per clip and angle: the union of non-background pixels with a 12 px margin. Then upscale with nearest-neighbour so the troop fills the view. Add `guard-shuffle` loco to the `idle` move's game list.
- [ ] **Step 3:** Rebuild the page from the existing run (`.../20261102-221153`). Expected: the guard and idle frames show the troop filling the frame. The guard-shuffle tab is empty until the next render.

### Task 2: Guard pose

**Files:**
- Modify: `art/clips/biped/sword_shield.bbmodel` (animation `guard` only)
- Generated: `src/main/resources/assets/gravesandglory/clips/biped/sword_shield/guard.json`

Starting keys (today): `root` position y 0.6, `body` (3, 12, 0), legs barely bent, `left_arm` (-22, 18, 0), `right_arm` (-65, 0, 20), `right_forearm` -110. Length 2.4 s, looping.

Target, from the video at 0:06–0:10:
- **Root:** dropped (y down is positive).
- **Body:** pitched forward and turned so the shield side leads.
- **Legs:** both thighs flexed forward and shins bent back; a wide stance with the shield-side foot forward.
- **Shield:** raised in front of the chest with its top under the chin.
- **Sword:** held low by the hip, blade angled forward and up.
- **Breathing:** small, a few degrees on the body and arms.

Exact angles are the builder's call.

- [ ] **Step 1:** Write the guard keys with a short Python edit of the bbmodel (same key times; channel values only, keeping first key equal to last key).
- [ ] **Step 2:** Run `python tools/clips.py` then `python tools/clips.py --verify`. Expected: no errors, and only `guard.json` changes under `clips/biped/sword_shield/` (`git status`).
- [ ] **Step 3:** Render in the background: `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" bash test-tools/work/t16r12/live/run.sh <tag> solo_knight`. Expected: about 30 min. Frames appear under `now/solo_knight/grade/sword_shield/<stamp>/clips/guard/`. A late `p2-motion-grade-timeout` at the sheet stage is known and harmless if the frames exist.
- [ ] **Step 4:** Check the render yourself before showing it:
  - `guard` `k_extreme_a`/`k_extreme_b` at all 8 angles: no shield through the head or body, and no blade through a leg.
  - `idle` at the same angles: no clipping.
  - `guard-shuffle` loco: feet stay on the ground.
  - The guard-to-swing transitions: no blade through the body.
  If any check fails, go back to Step 1.
- [ ] **Step 5:** Rebuild the compare page (`python build_compare.py <run dir> "Guard v<N>"`), reload tab-5, and ask Kort with AskUserQuestion: approve, or what to change. Repeat from Step 1 until approved.

### Task 3: Templar and Lord Marshal check

- [ ] **Step 1:** Render `solo_sentinel`, then `solo_marshal`, one at a time (2-instance limit).
- [ ] **Step 2:** Check `guard` and `idle` on both, as in Task 2 Step 4. If either clips, adjust the guard keys so all three troops are clean, re-run `clips.py --verify`, and re-render the knight. Show Kort again if the pose changed visibly.

### Task 4: Land it

- [ ] **Step 1:** `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py game --offline` (background). Expected: pass. Then `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- [ ] **Step 2:** Commit `art/clips/biped/sword_shield.bbmodel` and `guard.json`, with a message like "Lower the sword-and-shield guard into the video's shield-forward crouch".
- [ ] **Step 3:** Ask Kort (AskUserQuestion) which move comes next: overhead, low cut, thrust, hits, guarded advance or taunt. Each gets its own plan, written from the spec's moves table, once he picks.
- [ ] **Step 4:** Message the coordinator (`local_3774e262-685f-4a49-9dac-390d66d09f46`) with the new head of `work/anim-rework`.
