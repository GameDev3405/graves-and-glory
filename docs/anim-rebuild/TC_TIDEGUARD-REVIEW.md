# tc_tideguard review record (2026-11-06)

The second spear unit, the Tide Court tideguard. It uses the shared spear builder: `GG_SPEAR_TROOP=tc_tideguard python art/anim/spear/build_spear_anims.py`. Its rig comes from `art/anim/common/troop_rig.py tc_tideguard`. It has the same clip set as wm_tusk: thrust, high thrust and low sweep, each with a `_legs` clip, plus locomotion, stances, hits, block and four deaths.

- **Round 1 fix: rear fist out of the torso.** At thrust contact the butt of the shaft passed through the torso. Each troop now has a `rear_x` offset for the rear (right) fist; tideguard uses -0.6 px. The build log shows 0 samples with the shaft in the body in all three attacks.
- **Round 1 fix: death spear lookahead.** In the deaths the wrist could flip the spear to the other side between adjacent keys. Troops can opt in to `death_lookahead`, which makes `flat_weapon` choose each wrist turn with the next half tick in view (from t3.0). With it, the spear lifts, swings and lies flat with no flip or spin.
- **Shared builder changes in this commit:**
  - The knight `death_clip` gains `SOFT` (bone-name prefixes of hanging hair or kelp, which never hold up the falling body) and a ground settle for those strands.
  - `troop_rig` gains a per-troop `HANG` table to re-hang body gear on a leg bone.
  - Both default to empty and only affect tc_admiral (next unit). Tideguard's clips are unchanged by them.

## Review

- **Round 1 (Sonnet high, reel `reel_tide_v1`): SHIP WITH MINORS.** The butt clipped through the torso at thrust contact, and the spear flipped in the deaths. Both are fixed above.
- **Round 2 (Sonnet high, reel `reel_tide_v2`, plus dense side strips of each attack): SHIP WITH MINORS. No blockers.**
  - All four deaths: the spear swings smoothly and rests on the ground beside the body, with no flip or spin.
  - Thrust: a held load with a coil (t296–299), fast contact (t300–302), a lunge with a hip drop and torso turn, an overshoot, then a settle back to guard. The butt exits behind the hip and no longer cuts the torso.
  - Regression check of loco, stances, hits, ready idle, high thrust and low sweep: no pops and no weapon through the body.
  - That was the second fix round, so the remaining items are recorded below.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false (attack sync 34/34 standing, 4/4 walking).
- **Verdict: finished.** Video: `tc_tideguard_v2.mp4`.

## Open minors (polish, not blocking)

- **Behind death** (t631+, side view): the spear tip leans on the arm, slightly above the ground, rather than lying fully flat.
- **Right death** (t735+, front view): the butt end lightly crosses the lower torso.
- **Left death** (t677, front view): arm and spear overlap for a moment in the pile.
- **Low sweep** (t368→t369): a quick drop of the spear to horizontal. It reads as a fast strike, not a pop.
- At contact the thrust and the low sweep both end near horizontal, so they look somewhat alike.
