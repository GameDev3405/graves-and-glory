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

# Sentinel: v2 style + v4 deaths (2026-11-07)

The sentinel now builds in the v2 shield style (Kort approved v2 as the default for shield units), so its whole set changed, not only the deaths. It also has the knight's 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` (drops the longsword) and `_drop2` (drops the longsword and the shield). The D3 death system in `art/anim/knight/build_anims.py` now takes the held weapon's own bones (`weapon_t1..t3`) and the troop's own forearm pieces (the sentinel has cuffs and no glove).

- **Byte-identical:** these troops build byte-identical with the HEAD builder and the new builder: knight, gl_marshal, revenant (both direct), ic_hearthguard (axe builder), archer (bow builder), ic_arbalist (crossbow builder), wm_tusk (spear builder) and tc_harpooner (thrower builder). D3 stays gated on `D3_TROOPS`.
- **Review (Sonnet, high effort): SHIP WITH MINORS.** It covered the whole set (gait, stances, idle, 3 attacks, 7 hits, 12 deaths) from contact sheets and the full reel. No blockers.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=sentinel`):**
  - Run 1: 22 p1 and 1 `=false`. That failure was the march run-side check, which the lag played as WALK. The suite rollup line follows from it.
  - Rerun: 22 p1 and 0 `=false`.
  - The death clips that played included `death_back`, `death_forward`, `death_forward_drop1`, `death_left_drop1` and `death_right_drop1`.

## Open minors (v2 + v4, polish, not blocking)

Most of these come from the shared v2 builder, so they apply to the knight as well.

- **Thrust:** at t=11 the blade is yawed out to the right, then snaps forward by t=13. Cap the yaw at about 30 degrees, or add an in-between.
- **Low cut:** it ends about 25-30 degrees blade-down at t=15-18, which reads as a chop more than a flat sweep.
- **Overhead:** the hilt sits tight to the helmet at t=11-13. A slight clip can't be ruled out at sheet size. The wind-up at t=0-4 barely moves.
- **Heavy front hit:** the onset at t=1-3 is soft.
- **Side and behind hits:** direction reads mostly from torso lean. Add about 20% more head and shoulder turn.
- **Raised-shield stances:** only slightly higher than the base poses.
- **Combat-ready idle:** breath and weight shift are subtle (about 1.5x wanted). This is a carry-over minor.
- **death_back:** the fall is fast (upright at t=8, flat at t=13), with no visible back steps.
- **death_back (kept shield):** settles from upright to flat after landing. This is the clatter; it should not slide.
- **death_forward:** the knee buckle is hard to see; it reads as a stiff face-plant.
- **death_forward_drop2:** the dropped shield looks slightly tilted, not fully flat.
- **death_left:** ends half-down: a slumped kneel and lean, with the kept sword diagonal.
- **death_left_drop2:** the shield lies flat beside the hips, partly under the slumped body. The builder's clearance check passes, but confirm it in game.
- **Not seen in game:** only the harness and the offline reel were run.
