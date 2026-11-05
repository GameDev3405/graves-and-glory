---
name: animate-troop
description: Animate a Graves and Glory troop with its held weapons, using Fresh Animations-style articulation (waist, elbows, knees, ankles), solved legs and arms, clipping control, and a preview reel and exports. Use when asked to animate a unit or troop, add or fix combat clips (idle, advance, attack combo, block, hit), fix weapons clipping through a body, or render animation previews or Blockbench animations.
---

# Animate a troop

Read `docs/ANIMATING-TROOPS.md` first. It is the full workflow, with the conventions, the numbers and a table of what
failed and why. This file is the short procedure.

## Boundaries

- Animations are **preview art**. Wiring clips into the game or changing the troop model (new bones) is
  gameplay-visible: Kort decides. Record it in `docs/QUESTIONS-FOR-KORT.md` and `docs/BACKLOG.md`.
- Don't hand-edit generated exports (`*.java`, `*.json`, `*.bbmodel`). Regenerate them.
- Commit on the session branch. If `git push` is denied, ask the user; don't work around it.

## Procedure

1. **Set up the unit and its show.** Copy `tools/animate_archer.py`: a unit class (armour from
   `tools/player_armor/<set>.py`, cut with `split_parts`; items with grip transforms; `drawables_at`, `export_parts`,
   `clipper`) and a `Show` for `animate_combat.main`. Check the armour-cut prefixes (`combat_rig.WHOLE`), which
   armour pieces are follow bones (springs), and the item grip boxes and allowed contacts in the clipper.
   **Measure the proportions first** (reach about 8 px, head and collar size, item length): they decide which
   poses can exist (see section 11 of the guide).
2. **Block the guard** first. Pose fields: `root`, `body`, `chest`, `sword`/`shield_arm` as *direction vectors in the
   chest's frame* (+x the troop's right, +y up, −z forward), `shield` and `look` facings, and foot placements
   `r_place`/`l_place` = (out, z, lift, toe_out, heel). Render it with
   `python tools/animate_combat.py --sheet <clip> <t> --out <scratch dir>` (three large views plus clipping volumes),
   and adjust until it reads.
3. **Write each clip as a chained timeline** (`seq(GUARD, [(t, label, changes), ...])`) with named arm presets. Timing:
   anticipation held a beat, strikes of 0.06 to 0.08 s, recoveries at least 0.25 s, the back foot driving every strike,
   heel-first steps, overlap through `lags`. Once-clips start and end on `GUARD`; loops close.
4. **Build** with `combat_clips.build(name, length, loop, timeline, lags=...)`. It keys the torso, solves legs, head,
   arms and the shield wrist, bakes every 0.025 s, runs clearance and simulates the cape and plume.
5. **Check clipping at the source**, items *and* forearms (`clearance.LimbClipper`). Fix poses, not the solver: search
   the key poses first (wide random search, then local), then the beats (`pose_search.py`: still poses, then
   `search_window` for transitions). If nothing in the search space gets clean, change the design, not the numbers.
   Hands placed by IK (`reach_arm`) for bows, two-handed weapons and anything held at a point.
6. **Measure**: `python tools/animate_combat.py --check`. It reports IK error, feet (sink, hover, skate), true-rotation
   smoothness and clipping. Gates: reach < 0.3 px; sink and hover ≤ 0.15 px; skate ≤ 8 px/s; ≤ 50 degrees a frame;
   jerk ≤ 30; items ≤ 1.5 px³. Use the geodesic rotation, not Euler steps.
7. **Look**: `--sheet <clip>` at every beat (blades must never point at the camera across the face), and
   `--strip <clip> <from> <to>` around each strike for arc continuity.
8. **Render and export**: `python tools/animate_combat.py`, about 4 min on all cores. Then run
   `python -m unittest discover -s test-tools/standalone -p 'test_*.py'`, since the export tests compare against the
   committed files.
9. **Document**: the set's `art/animations/<set>/README.md` (clips, how it moves, numbers, files, decisions for Kort),
   plus the BACKLOG and QUESTIONS rows.

## Traps (each cost a rework)

- Never interpolate arm Euler keys between far-apart poses. Interpolate aims and bake.
- Use a natural grip for blades. Keyed edge vectors and motion-led edges spin the wrist.
- Keep the elbow bending to the same side between consecutive poses; crossing it forces a 180-degree upper-arm roll.
- Catmull-Rom can't hold a planted foot (it over-shoots between equal keys). Placements use PCHIP.
- Elbow pivot just behind the arm's centre (z 0.5), not the back edge; otherwise the pommel sinks into the upper arm.
- The shield's aim must include the item's rest rotation.
- A clearance push is held early; if it causes new clipping, fix the pose that needs it rather than raising the push.
- Fade clearance pushes at once-clip ends, or the clip won't start and end on the guard.
- Never blend Euler numbers (settles, transitions): slerp rotations, then `closest_euler`.
- IK in yaw and pitch has a pole at an arm hanging straight down; solve in tangent space with continuity and twist
  terms.
- Test at 60 fps, the rate the game draws: 30 fps sampling hides peaks.
- A searched fix stored by beat time is lost when the beat is retimed: move its key in the poses file too.
