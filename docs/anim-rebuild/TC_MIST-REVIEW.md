# tc_mist review record (2026-11-06)

The Mist Caller (tc_mist), the Tidecallers caster: a driftwood rod with a conch at the tip in the right fist, a kelp hood, a mist cloak and a sash. Its clips come from the caster builder (`art/anim/caster/build_caster_anims.py`, `GG_CAST_TROOP=tc_mist`). The mage's and hexer's built output is byte-identical; every tc_mist change branches on the troop.

- **Clips:** guard, idle, walk and run carry, stances, hits and deaths, plus `attack_cast` (36 ticks) and `attack_cast_legs`. The timeline `impact` marker sits on the server's release tick (10).
- **Cast:** a backhand fan sweep that spreads the fog. It is not the mage's overhead thrust or the hexer's underhand swing. From the guard (the rod up before the right shoulder, the conch to the right of the face) the body turns and sinks from tick 0. The right shoulder comes forward as the rod is drawn across the chest and back past the left side, and the conch tips up so it shows past the shoulder. The wind keeps tightening through the held load (ticks 4.5-8.4). The hips then open and the rod sweeps flat out through the front. On tick 10 it is levelled at the foe, and the free hand is flung wide to the left (the arms part the mist). The sweep overshoots out to the right at 11.8, the off hand eases back by tick 16, and the root is on guard by tick 30. Probe: fist error at most 0.389 px, 0 samples with the rod in the body.
- **Stances:** brace (Hold position) is a deep crouch, with the rod low and level before the lead knee. Wall (Hold the line) is upright, with the rod raised high and the conch clear of the face, and the off hand beside it. Ward (fall back) holds the rod crosswise before the chest as a bar, with the off hand up.
- **Carry:** walk and run carry the rod butt 13.9 px and 12.9 px above the ground, with 0 samples in the body.
- **Deaths:** the shared caster falls, untweaked. Rest heights (min-y, px): left torso 0.84, hips 3.93; right torso 0.97, hips 4.14.
- **Gameplay (Kort approved, 2026-11-06, cosmetic only):** the mist caller's attack is its mist ability, which sent no attack event. `Abilities.castsAhead` now covers a caster whose ability mechanic is blight or mist (the hexer and the mist caller). The mist caller announces the cast 10 ticks early from `Tactics` on the beat before a cast is due, and `Abilities` sends the matching `loose` when the mist is released. Damage, ticks, targeting and cooldowns are unchanged.
- **Checks:** `GeoKnightChecks` renamed the `BLIGHT` flag to `SPELL` (ranged, magic caster, blight or mist). It runs the ranged duel with frames over the 36-tick cast clip and releases through `Tactics.shoot` with the ability cooldown cleared. `GeoTroopRenderer` lists tc_mist.

## Review

- **Round 1 (Sonnet high, reel `tc_mist/reel`, video `tc_mist_v1.mp4`): SHIP WITH MINORS.** No blockers. No clipping or pops, and all four deaths lie on the floor. Minors: the off hand was not flung wide at the release; the rod hid behind the torso in side view during the load; the anticipation started late, with a 1-tick snap into the release; the ward was hard to read; the wall fists covered the face and the conch floated; the run's trailing arm read as a pole; the guard rod was small and overlapped by the off hand.
- **Round 2 (Sonnet high, reel `tc_mist/reel2`, video `tc_mist_v2.mp4`): SHIP WITH MINORS.**
  - Fixed: the rod shows past the shoulder through the load; the turn and sink start at tick 0, and the drive into the release is smooth; the wall rod is high with the conch clear of the face.
  - Improved: the off hand is flung wide (fully by tick 12); the ward rod is more horizontal and forward; the guard rod is lifted clear of the off hand.
  - No new clipping, floor contact or spins.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false.
  - 35 of 35 cast frames caught in the side and three-quarter views, and 17 of 17 far.
  - All 9 standing and 4 walking casts matched the server release.
  - Weight at the release is 1.0, and 0 between casts.
  - Stances, hits and deaths pass in 3 views.
- **Verdict: finished.** Video: `tc_mist_v2.mp4`.

## Open minors (polish, not blocking)

- **Cast release:** the off-hand fling is modest on the impact frame itself and is fully wide only by tick 12.
- **Cast, front view:** at the release the rod hand sits low, so the rod crosses the lower body. The side view reads well.
- **Ward:** the rod still tilts a little diagonally in the side view rather than reading as a clean horizontal bar.
- **Guard:** the rod is small in the front view.
- **Run:** the trailing free arm reads like a stray pole. This is the shared gait carry, and was not reopened.
- **Shared with the mage, not reopened:** the heavy front hit folds about 80 degrees with a rebound (knight hit keys); the deaths have a long static end; there is a walk and run seam pop; the run has little forward lean.
- The early announce needs the mist ability to be off cooldown on the beat before the cast. The harness checks the announce and release timing through `Tactics.shoot`, not the live mist targeting itself.
