---
name: animate-troop
description: Author a Graves and Glory troop clip from scratch with tools/animate_troop.py - chained poses solved per tick (leg IK, arm aims with a natural grip, head look), clearance pushes that keep weapons and shields out of the body and the ground, a bake to tick keys written into art/clips/<rig>/<family>.bbmodel, and gate checks, sheets and strips for review. Use for the 2026-11 animation redo, for any new or re-keyed troop attack, guard, cast or shot, when a blade or shield clips through a troop, or when asked to check, sheet or strip a troop clip offline.
---

# Animate a troop

Read `docs/ANIMATING-TROOPS.md` first: the pipeline, conventions, recipe fields, gates, timing principles, traps and a
table of what failed and why. This file is the short procedure. `.claude/skills/troop-animation/` still owns Kort's
rules (blade direction, 1 texel per pixel, no back flags, souls-like reads, no missing clips), the in-game review loop
and the rubric; follow both.

## Boundaries

- Timing is gameplay: clip ids, lengths and impact ticks come from `sheets/design.json` attackVariants. The writer
  refuses a mismatch; never change design.json for a clip without Kort.
- Never run the old `art/clips/build_clips.py` in write mode. Never hand-edit `Spec.java` or the clip JSON; write the
  bbmodel through `--author --write`, which runs `tools/clips.py`.
- Offline sheets are not acceptance. Kort sees in-game sheets before any merge. No push, publish or merge.
- One writer per family bbmodel. Commit only the recipe, the bbmodel, the clip JSON and the named review sheets.

## Procedure

1. **Scope.** `python tools/animate_troop.py --list` gives each troop's family, weapon hand and attacks. Read the
   design row for the clip (impactTicks, length) and the family's spec if one exists.
2. **See what is there.** `--still TROOP --clip CLIP --out DIR` and `--check TROOP CLIP`.
3. **Start from the preset.** `--author TROOP CLIP --out DIR` bakes `recipes.preset` in memory, prints every gate, and
   draws a sheet at the key times and a strip around the impact. Nothing is written.
4. **Tune the recipe** in `tools/troop_anim/recipes.py` `RECIPES[troop][clip]`. Order: clipping (change aims; a small
   grid search over the key aims beats a bigger push), then rate and wrist, then ground, then the read from every view.
   Hold the timing principles: loaded wind-up hold, fast linear strike, weight at impact, slower recovery, stance at
   both ends of a once-clip.
5. **Budget.** Up to two fix rounds per failing gate, then stop and report the numbers and sheets.
6. **Write.** `--author TROOP CLIP --write` (it refuses on any gate failure), then `python tools/clips.py --verify` and
   `python tools/verify.py fast`. Only the one animation in the bbmodel changes; check `git diff --stat`.
7. **Review sheets.** `--sheet TROOP CLIP t1 t2 ... --out docs/review/anim-redo/<troop>/` and
   `--strip TROOP CLIP FROM TO --step 0.05 --out ...`. Look at all three views of every key: blade direction, pommel,
   shield edge, feet.
8. **In game.** Shoot the contact sheet and run the reviewer loop in `.claude/skills/troop-animation/`.

## Traps (each cost a rework)

- Interpolating Euler keys between far poses spins arms. Recipes interpolate aims; the bake solves every tick.
- Let elevation carry the elbow bend. A sideways bend plane rolls the upper arm 80 to 90 degrees.
- Keep the elbow bending to the same side between keys; crossing it forces a 180-degree roll.
- Long hilts need a cocked wrist: blade 20 to 35 degrees above the forearm, or the pommel sinks into the arm.
- Hold item minus forearm elevation nearly constant through the strike, or the wrist gate trips.
- Clipping hides between ticks; the push solver checks half ticks, and `--check` samples at 60 fps.
- A low follow-through puts the tip in the ground; keep the blade level after a root drop.
- A big push means the pose is wrong. Fix the pose.
- Joint limits clamp backwards elbows in game, so they look frozen, not bent; the sign lint reads the pitch before.
- The stance's own clipping and foot hover are floors, not clip defects; they are reported apart.

## Rig bones

Today's bipeds have no `chest` or `*_foot`. When Kort's waist and ankle bones land, the `chest` pose field drives the
waist and the feet roll on the ankle with no recipe edits; re-run `--check` on every committed clip, because the arms
then hang from the chest.
