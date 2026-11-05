# Troop animation plan (hybrid) - 2026-10-28

Decision (Kort): hybrid. Code-authored clips for the everyday states; hand-keyed Blender clips only for signature attacks and the charge. First pass covers all 6 classes and the core states; formation and charge come second. Implementation starts after SLICE-1 scoping; this is the plan only. Excalibur is the look; Fresh Animations is not used.

> **2026-11-05:** the workflow used to animate troops so far (rigs, solvers, clipping control, checks, and what failed)
> is in `docs/ANIMATING-TROOPS.md`. The Oathbound combat pack there was made Fresh Animations-style, on request, for
> combat, with a new articulated rig. That conflicts with the line above. Settle which look holds, and update this plan
> (QUESTIONS-FOR-KORT ANIM-02).

## What already exists (do not redo)
`TroopClips`/`BipedClips`/`HoundClips`/`SpiderClips`/`ColossusClips` hold 18 code-authored keyframe clips per rig: idle, walk, run, attack, hit, death, shield_up, bow_draw, cast, kneel, point, sit, raise, fall, stand, brace, hold, fall_back. `TroopAnimator` blends them by state and stance (server `stance_v1` packet), `TroopTells` adds idle tells and per-House attack variants, mirroring handles left-handed troops. So the "procedural base" half of the hybrid is mostly built.

## Gaps
1. One biped `attack` clip (plus House variants): no clearly different sword, axe, spear/thrust, trident and mace swings per class.
2. No dedicated `charge` run (leaning, weapon forward) distinct from `run`; the order exists server-side (`order=charge`).
3. Formation: `hold` and `brace` exist, but no marching-in-formation walk or rank-aware stance.

## Split
- Code (procedural): per-weapon attack variants for the six classes where a pose-and-swing curve is enough; charge as a leaned `run` variant; formation march as a `walk` variant; any missing hit/death polish.
- Blender (hand-keyed): the signature attacks (the one or two per class that define it) and the charge, if the code version looks flat. Needs a decision on import: Blockbench/Blender -> the mod's `Animation` data, which `TroopClips` currently builds only in code.

## First pass (after SLICE-1 scoping)
Per class: audit which of the 18 clips already read well; add attack variants; add `charge` and the formation walk; capture contact-sheet stills with the existing harness (`shotTime`) for review. Exit: six classes, each with distinct attack, charge and formation walk, accepted visually by Kort.

## Open
- Who keys the Blender clips (Claude, Kort or the builder), and the import path.
- Tier art gaps (26 troops) are separate and unchanged.
