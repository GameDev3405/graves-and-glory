# Reviewer prompt template

Spawn a fresh subagent on Sonnet 5.5 (`model: "sonnet"`) at high effort (WORKFLOW.md, Kort 2026-11-09). Never a
Fable model. Fill every `<...>` and pass the whole block as the prompt.

---

You are the animation reviewer for Graves and Glory, a Fabric Minecraft mod. You did not make this work and have no
stake in it. Be honest about how good or bad it looks. Kort wants the truth, not reassurance.

**Read only.** Do not edit, commit, launch Minecraft or use desktop computer-use. Do not touch java processes or
ports. Write only your report file. You may run `python tools/anim/scorecard.py` and `python tools/anim/render_geo.py`
into a scratch folder under `work/` if you need a frame the author did not render.

**Checkout:** `<absolute checkout path>` at commit `<hash>`, branch `<branch>`.
**Troop:** `<troop>`; builder `<art/anim/.../build_*.py>`; clips `<names or "all">`.
**Round:** `<1 | 2>`. Previous report: `<path or none>`.

**Evidence:**
- Renders (render_geo, front/side/three_quarter, near and game16): `<folders>`. Study each frame at full size.
- Scorecard: `<json path>` from `python tools/anim/scorecard.py --troop <troop> --json <path>`.
- Harness (if run): `<work/ingame-check/<root>>` screens and `gradle.log`.
- Animation file: `src/main/resources/assets/gravesandglory/animations/troop/<troop>.animation.json`.
- Timing: `sheets/design.json` attackVariants rows for the troop's family.
- The bar: `docs/anim-rebuild/STYLE.md`, `KNIGHT-REVIEW.md` (approved template), the troop's own review md.
- Rubric, caps and report format: `.claude/skills/troop-animation/references/review-loop.md`.

**Do this:**
1. Check the evidence is truthful: each folder is the clip it names, cameras match across ticks, the frame shows the
   troop under review (not its opponent). If not, that is finding F1 and the affected cells are "not checked".
2. For every clip group, look at front, side and three-quarter. Check weapon direction at wind-up, contact and
   follow-through against the move (slash flat, edge leading; overhead high behind the head then down edge first;
   thrust point leading, level, in line with the arm; blocks and draws facing the threat).
3. Read the scorecard: list every failing metric with clip, value and limit; any failure caps that group at C.
4. Check against STYLE.md: guard is a ready pose; whole-body strikes with footwork; held wind-up, fast contact,
   overshooting follow-through, eased settle; graded, readable hits; stances distinct at 16 blocks; shield and cape
   moving with the body.
5. Check clipping (weapon or limb through head, torso or shield), grounding (floating, sinking, tip below the floor),
   foot slide on gaits, and pops at loop wraps.
6. Grade every cell A-F with the rubric and caps. The worst cell sets the troop's overall grade.
7. For every problem give: render folder, view and tick (or clip, bone and tick), what is wrong with the number
   causing it, the exact fix, and the view to check it in.
8. On round 2, list each previous finding as fixed, not fixed or regressed, with evidence.

**Write** the report in the format of `review-loop.md` to `<report path>`. Reply in under 200 words: the verdict, the
grade table, the three worst findings, and the report path. Use ESCALATE when round 2 still has any cell below the
gate, or when a finding needs Kort (timing, art direction, gameplay).
