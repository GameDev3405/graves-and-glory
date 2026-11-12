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

## Batch review 2026-11-11 (branch anim/review-caster)

Kort's four focus points, checked with `tools/anim/focus_check.py`; additive clips also judged on the guard
(`work/review/compose.py`). The family-wide part of the fix (life pass, reaction layers, front blow, stun loop) is in
MAGE-REVIEW.md, same date.

- **Baseline:** the rebuild at the lane commit matched `out/` clip for clip (CRLF only): no drift.
- **Rows:** lead's first run 20 clips with a FAIL (36 rows). Before, composed: 16 clips (27). After, composed: 3 clips
  (3 rows); raw 15 clips (17), the extra 14 bind-pose artefacts (wrist 180 at t0 on every additive clip, knockback and
  pulled elbows). Scorecard 35 clips, 0 failing.
- **Changes:**
  - **Life pass** on the cast, mist, every hit, stun, stun_out, knockback, pulled: the kelp hood, pauldrons, sash tail and the
    robe panels swing (still in every action clip before). The sash tail the guard keys in its upper clip stays in the hit's
    upper clip.
  - **attack_cast (whole_body 0.51):** the hips load back as the body turns, carry over the gliding right foot and drive into
    the release (they travelled 0.5 px; the step was 1.4).
  - **Blows:** the guard already holds the free arm 40 deg out; a blow now adds 0.4 of its outward swing on top of it (the arm
    went past the head: head into the pauldron 2.4-2.75, shoulder 115-129 in the medium back and left blows).
  - **Floor:** knockback (1.06 px under) and pulled (1.28): hips lifted at the tick. The rally's foot (1.75 under) passes
    with the stance layers rebuilt from the re-keyed clips.
  - **Stances:** brace rod fist a px out and the off arm 25 deg round (right bell 2.7 in the hood); ward off arm 12 deg off
    the mantle (1.95). Rally (stances.py layer): the rod arm 25 forward and 5 out (bell in the mantle plate 2.48; further
    out put the forearm in the head, scorecard pen_limb 1.98); aim_up arm 14 out.
  - **Stun loop:** the first tick eases out of the lap's last pose (the kelp hood stood 23 deg apart across the wrap,
    scorecard loop_pose).
- **Remaining FAIL rows (open, two fix rounds used):** the bell sleeves against the hood and mantle with an arm raised:
  brace hood into the left bell 2.62 (t29.5), shield_up hood into the left bell 2.44 (t3.5, unchanged), mist mantle into the
  right bell 2.13 (t13, unchanged). Rounds tried: brace off arm z -25/-50/0 (-50 best), shield_up z -5/-22/+12 (original
  best), the mist sweep keys moved and put back. The bell is 2 px wider than the arm and the hood 1 px over the shoulder.
- **Looked at:** all ticks, three views: attack_cast, hit_medium_left (`work/review/frames/all_tc_*.jpg`).
- **Tests:** unit tests and verify fast as in MAGE-REVIEW.md (the same builder; `test_where` timing under machine load).
  In game: geo-knight suite through checkq, root `tc_mist-review1`: 22 `p1-geo-tc_mist` lines and the suite line all true,
  0 `=false`.

### Re-check 2026-11-12 (tools merged at c0cb68a0: hits and stun_out judged over the stance)

- The new focus check plays every hit over `stance_ready_*` (as `work/review/compose.py` did) and `stun_out_upper` over the
  stun handing over to the stance in 8 ticks. The one row it found that the old tool hid, on all four casters:
  `stun_out_upper` secondary_life (robe panels, keyed by the legs clips, hung still over the handover: mage 1.76,
  hexer 0.95, tc_mist 1.42, wm_seer 1.05). Open, named: the stun_out life pass is now driven by the stun legs blending into the
  stance legs (`_handover_legs`), which swings the upper cloth, but the robe panels belong to `stun_legs` /
  `stance_ready_legs`, and the scorecard plays `stun_out_upper` with `stun_legs` and needs disjoint bones (exit 2 when
  the panels were keyed in `stun_out_upper`). Fixing it needs the panels' swing in the legs clips (shared reactions.py /
  stances.py) or a scorecard that plays stun_out over the handover; both are outside this lane.
- The layer pass (`GG_CAST_REVIEW=layers`, documented in the builder's "Batch review" block) rebuilds every layer fresh
  from reactions.py / stances.py before its own pass: running it twice gives the same bytes (checked on mage).
- `out/` holds the builder's 40 clips; the exported asset holds those 40 byte-identical plus the 17 shared layers
  (reactions.py, stances.py, charge.py), as before this review.
- Rule 6: hold (stances.py, gear `staff`/`focus`) holds the caster's focus forward; brace, wall, ward and shield_up are this
  builder's caster poses. The cast's `impact` stays tick 10, the release key (the bolt leaves).
