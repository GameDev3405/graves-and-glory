# Sentinel review record (2026-11-06)

Second shield unit. It shares the knight's clip builder (`art/anim/knight/build_anims.py`, `GG_ANIM_TROOP=sentinel`) and was judged against STYLE.md by a screenshot reviewer over three rounds.

- **Review: FIX FIRST.** Three problems:
  - Brace stances: the sword hung limp.
  - Block: the shove-back was weak.
  - Combat-ready idle: too subtle.
- **Fix round 1:**
  - The brace sword is held low by the right hip, angled up and across past the shield edge.
  - Block phases: absorb (rock back, skid), then a shoulder-driven shield shove, then settle.
  - Idle weight shift doubled.
- **Re-review 1:** brace FIXED, block FIXED. The idle was still MAJOR: its weight cycle did not read from the front.
- **Fix round 2:**
  - Idle weight cycle: bigger forward/back shift, a lead-knee flex and hip drop on the forward phase, a lateral hip shift, and arms trailing the torso.
  - Block shove: less head pitch, with the shield arm pushed further out.
- **Re-review 2: SHIP WITH MINORS.** No new blockers.
- **In-game checks:** the geo-knight suite with `GRAVES_GEO_CHECK_TROOP=sentinel`.

These changes are shared, so the knight's idle and block changed with them (see KNIGHT-REVIEW.md).

## Open minors (polish, not blocking)

- **Idle:**
  - The lateral shift is still small in the front view; push it about 30% further.
  - The arm trail and knee flex are hard to see; add a 2-3 tick lag.
- **Block shove:**
  - The head still pitches about 40 degrees; cut another 10-15.
  - Extend the shield arm about 2 voxels.
- **Brace:**
  - The holds have no breath; add a small chest rise.
  - In the front view the blade hides behind the hip.
- **To verify:** foot sliding can't be checked at sheet size.
