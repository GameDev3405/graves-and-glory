# wm_hunter review record (2026-11-06)

The third bow unit. It uses the archer's bow builder (`GG_BOW_TROOP=wm_hunter python art/anim/archer/build_bow_anims.py`) with its own character values (`CHAR["wm_hunter"]`: a shallow lean, hips sunk 1.2×, a big rock at the loose) and its own aim (volley 28°, direct 3°). Its rig comes from `art/anim/common/troop_rig.py wm_hunter` (64 bones, order bow).

- **Hunch.** The head rests pitched 20° down. The anchor solve runs on the full FK, rest rotation included, so the nock still lands on the cheek.
- **Gear.** The cape takes the archer's hood-cape lag. The quiver's band, rim and fan arrows are separate bones, so it has no lag channel.
- **Shared fixes from this unit's batch** (no change to the archer or longbow output):
  - `build_anims.py` `SHOULDER_D`: the fist IK targets shift by the troop's own left-shoulder pivot against the knight's, for rigs whose shoulders sit higher.
  - `build_bow_anims.py` recoil hip key: the extra drop applies only to troops sunk 1× or more. Before, a light troop (bonebow, 0.5×) lifted its hips there until the back leg could not reach.

## Review

- **Round 1 (Sonnet high, reel `wm_hunter_v1`): SHIP WITH MINORS.** No blockers.
  - **Shot:** the bow holds the aim line with upright limbs and no flip. The string hand reaches the cheek past the hunched snout. The loose and recoil read, and the held shot keeps its draw alive.
  - **Fan:** the feather fan swings and settles through the shot, hits and deaths.
  - **Deaths:** the bow stays in hand and rests beside the body in every fall.
  - **Locomotion:** the hooves plant with no slide or sink.
- **In-game checks** (`GRAVES_GEO_CHECK_TROOP=wm_hunter`): **run 6 PASS, all 51 checks.** Attack frames 25/25 at side and three-quarter and 12/12 far; sync 15/15 standing and 4/4 walking; impact weight 1.0; footwork 1.0; hits 7 blows × 3 views; deaths 4/4.
- **Verdict: finished** (visual review and gameplay testing both pass). Video `wm_hunter_v1`.

## Open minors (polish, not blocking)

- **Brace and hold the line.** Both read close to combat ready (same as the archer).
- **Bowstring art.** The rigid string rod sits in front of the snout at full draw. The arrow is short.
- **Death right.** The rest pose bunches the fan and bow against the torso. The bow crosses the face mid-fall.
- **Death behind.** One bow limb rests raised 30–45°.
- **hit_light_front amplitude.** It is weak.
- **Cape lag.** It was not visible from the reel's cameras, so it is unconfirmed.

## v2 feel (2026-11-07, branch anim/v2-bow)

The same Knight v2 feel as the archer (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/archer/build_bow_anims.py` with the hunter's own CHAR values. The draw, the shot and the deaths are unchanged, and the deaths are byte-identical to the previous export.

In the medium hit from the right, the bow arm opens less: `BOW_ARM_RIGHT` (-4, -8, -11). With the archer's values the bow went into the left leg and shin (10 points); it is 0 now.

- **Probe.**
  - No bow or fist in the body in any hit, idle or stance.
  - Pre-existing and unchanged: the volley has 1 bow point in the head, and the held direct shot has 1 in the left shin.
  - Floor: hits -0.29 to -0.58 px.
  - Largest hit pop: 26.0 px/t, at the heavy front peak, at the bow tip.
- **Review.** Sonnet high, 1 round, verdict SHIP WITH MINORS.
  - Stances read clearly apart and the hits are graded.
  - The bow arm never crosses the face or body. The only face overlap is the accepted front-view depth overlap at the heavy peak.
  - No floor breaks, pops or wrong facing seen.
- **Open minors.**
  - The medium hit from behind bends the torso about 70-80 degrees forward, stronger than the front heavy hit. Cap it near 50 degrees.
  - Heavy front peak leans about 55-65 degrees, past the archer's 45-55.
  - The plain idle weight shift is subtle.
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=wm_hunter`: 22 `[p1] p1-` lines, 0 `=false`.
- Video: `wm_hunter_v2feel`.

## Batch review 2026-11-11

Bow-family batch review on `anim/review-bow`. Timing unchanged: `ATTACK_TICKS` 32, `FULL_AT` 16.5, `IMPACT` 22; both attacks keep `full` 0.825 and `impact` 1.1.

- **Rows before -> after** (`work/review/wm_hunter.{before,after}.json`): clips with a FAIL 15 -> 7, FAIL rows 32 -> 11, WARN rows 36 -> 44. Every added WARN is a former FAIL (no PASS became a WARN).
- **Changed clips (16, spliced; every other clip byte-identical to HEAD).** The hit `_legs` clips came out identical, so they are unchanged.
  - `attack_direct`, `attack_volley` (+legs): Kort's archer r3 shot fitted to the hunter (`direct_shot.FIT["wm_hunter"]`): anchor 1 px further back and lower for the snout, quiver roll -16, bow canted -30 deg and raised 20 deg, head turned 34 deg off the arrow and away at the nock so the bow and string hand clear the snout; nock grip 1 px further forward so the string forearm clears the jerkin (scorecard `pen_limb` 1.52 -> 1.07, limit 1.39). The quiver channel now reaches the clip (`_own_bones`: the per-troop gear remap had dropped it). Fist through the head (3.04) and the loose pop are gone. Bow into the cap at t12.5 is 1.48-1.50, a WARN on the limit.
  - Aim stances: from the r3 full-draw frame, chest takes 85% of the pitch.
  - Hits and `hit_block`: gear swings (cloak, feather fan, quiver, tusk necklace) soft and low-passed: the feather pops (up to 31 deg) and `secondary_life` FAILs are gone.
- **Remaining FAIL rows.** `stance_rally_upper` right bracer into the snout 2.22 at t6.5 (rally is not a bow clip of this batch; second fix round not spent on it). `death_forward` shoulder 108 at t20 (pre-existing). Common modules: `hit_pulled`, `stun_upper`, `stun_out_upper`, `hit_knockback` (hoof under the ground, still gear), `charge_run_upper` (forearm in the jerkin 2.50).
- **Proposed exemptions.** None for the bow clips. The common-module rows belong to `reactions.py` / `charge.py`.
- **Tests.** Scorecard exit 0 (35 clips, 0 fail) after the nock-grip fix (it failed `pen_limb` on both attacks before it). Unit tests OK (see ARCHER-REVIEW batch section). Booth config valid.
