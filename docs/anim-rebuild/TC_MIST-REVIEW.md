# tc_mist review record (2026-11-07, caster v2)

The Mist Caller (tc_mist), the Tidecallers caster: a driftwood rod (about 9 px) with a conch at the tip in the right fist, a kelp hood, a mist cloak and a sash. Its clips come from the caster builder (`art/anim/caster/build_caster_anims.py`, `GG_CAST_TROOP=tc_mist`). It was redone on 2026-11-07 in caster style v2, following the wm_seer pilot (`WM_SEER-REVIEW.md`). The mage's, hexer's and wm_seer's built output is byte-identical; every tc_mist change branches on the troop (`ROBED` covers wm_seer and tc_mist).

## v2 (2026-11-07)

- **Robe:** the skirt and hem are rebuilt as four hanging panels under the hips (front, back and two sides), each in three stepped tiers (`troop_rig.ROBE["tc_mist"]["tiers"]`, `robe_tiers`). Each tier is 1 px thick and steps out 1 px, so the robe flares as before.
  - The panels pivot at the waist (geo 12.5). The hem sits at geo 1.5, and one row of the top tier moved to the waist so the hem clears the floor on the upright v2 hips.
  - The legs keep their bones but lose their boxes, so only the feet show (4x2x4, from the shins' bottom rows). The art is reused, and only the structure changes.
  - The builder's panel table and hem (`PANELS`, `HEM_Y` 1.5, `FEET` `_foot`) are tc_mist's own; the seer's are unchanged.
- **Cast (point and shoot):** the seer's keys and ticks, with the rod's own aims. The off hand rests at the hip and never joins the cast.
  - **Ticks 0-4:** the body turns side-on and the rod hand comes up past the chest.
  - **Ticks 4-9:** the arm extends straight with the elbow locked, and the rod holds steady along it through the charge.
  - **Tick 9.4:** the wrist cocks the conch up.
  - **Tick 10 (the impact marker):** the wrist flicks it down at the foe and the mist leaves the conch.
  - **After the release:** a small recoil, then back to guard.
  - **Probe:** fist error at most 0.037 px, 0 samples with the rod in the body at the key ticks.
- **Stances:**
  - **Brace:** upright, both arms up, the rod overhead crosswise. No crouch.
  - **Wall:** the rod pointed up and out at the foe.
  - **Ward:** the rod held out low across the body, turned away.
- **Carry:** the rod butt is 14.3 px (walk) and 13.5 px (run) above the ground, with 0 samples in the body. The gaits are the seer's short gliding walk and run.
- **Deaths:** the seer's fall tweaks except `death_forward` (`DEATH_TWEAK["tc_mist"]`). The robe folds flat with the body. Rest heights (min-y, px):

  | Fall | Rest heights |
  |---|---|
  | Back | torso 0.0, head 1.23, hips 1.35 |
  | Forward | hips 0.0, arms 0.46, torso 0.55 |
  | Left | hips 0.0, arms 1.88, torso 4.43 |
  | Right | hips 0.0, arms 1.88, torso 4.43 |

- **Robe probe** (the seer's poke-through probe): the worst leg-line margin is 0.75 px (at least 0 is the pass), the deepest foot poke 0.01 px (under 0.25), and the lowest live hem 0.15 px above the floor; fails: none. The death hems stay above the floor (lowest 0.01 px).
- **Tip origin (cosmetic only, as for the seer):** `data/gravesandglory/cast_tip/tc_mist.json` has offset (-0.213, 1.259, 1.383) blocks for the t2 rod, with t1 z 1.29 and t3 z 1.538. `Projectiles.bolt` already starts at `castFrom(u)`. Damage, ticks, targeting and cooldowns are unchanged.
- **Review (Sonnet high, reel `tc_mist_v3/reel`, video `tc_mist_v3.mp4`): SHIP WITH MINORS.** No blockers.
  - The reviewer flagged pale slivers below the hem in the run and walk. These are the 1 px side panels seen edge-on as they swing, not shins: the rig has no leg or shin boxes, and the probe passes.
  - Minors: at the release the rod reads more raised than level in the front view (seer timing); in the front view the off hand sits at chest height rather than at the hip; the brace rod hides behind the head from the front.
- **In-game checks (v2 clips): PASS, 22 of 22, 0 false** (`harness2.log`).
  - The first run failed one check: brace close view, knight 0 at weight 0.98 at k30, during "Can't keep up" server lag. That weight is set by `StanceController`'s 2-tick ramp, not by the clip. The rerun passed.
  - Cast frames: 35 of 35 in the side and three-quarter views, and 17 of 17 far.
  - 9 of 9 standing and 4 of 4 walking casts matched the server release.
  - The tip log reads `cast tip tc_mist: from (-277.287, 66.259, 6.117) eye (-277.5, 66.658, 7.5) body yaw 180`, so the mist leaves the conch rather than the eyes.

## v1 record (2026-11-06, superseded)

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
