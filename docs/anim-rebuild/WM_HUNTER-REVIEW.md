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
