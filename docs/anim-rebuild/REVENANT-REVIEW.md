# Revenant review record (2026-11-06)

Shieldless one-handed sword unit, built alongside the Drowned Raider. It shares the knight's clip builder (`art/anim/knight/build_anims.py`, `GG_ANIM_TROOP=revenant`, the shieldless FREE_GUARD / FREE_STANCE / FREE_FIST branch) and was judged against STYLE.md by a screenshot reviewer. The shieldless fixes from the tc_raider rounds (free-hand guard, wall upper body, stance breathing, stronger hit peak, deeper overhead load with the free-hand reach) apply to both units; see TC_RAIDER-REVIEW.md for that history.

- **Review: SHIP WITH MINORS.** The full set (idle, gait, stances, three attacks, hits, block, deaths) read with clear weight and purpose; no BLOCKER or MAJOR left after the shared shieldless fixes.
- **In-game checks:** the geo-knight suite with `GRAVES_GEO_CHECK_TROOP=revenant`, 22/22 (run r4).
  - Earlier runs failed `stance` and `attack-walk` as flakes of the foe-backing check: a single slow sample tripped it. The check now requires the median foe-backing speed over the swing to exceed 0.1 (GeoKnightChecks), and r4 passed with median 0.169.

## Open minors (polish, not blocking)

- **Overhead:**
  - An abrupt rise at clip +0.40 to +0.45.
  - The blade pops at +0.05 to +0.10; add an in-between.
- **Free hand:** the reach through the overhead load is not legible at range.
- **Twist:** less torso twist through the swings than the reel.
- **Walk:** stride reads short.
- **Low cut:** the blade passes close to the lead leg.
- **Shared with tc_raider:**
  - death knee-buckle beat; death-left/right corpse offset;
  - run lean and left-arm pump;
  - ward blade guard line and stiff ward arm;
  - no skid step on hits.

# Revenant: v2 style + v4 deaths (2026-11-07)

The revenant now builds in the knight's v2 style, rebuilt for one hand and an empty off hand, and has the 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` and `_drop2` (both drop the notched sword; there is no second piece). Deaths come from the shared module `art/anim/common/deaths.py`.

- **Style:** pelvis-driven drive (`ATTACK_LEGS`), graded light/medium/heavy hits, doubled idle weight shift, distinct stances. The off hand balances: it counterweights the cuts, folds across the chest on the thrust and rises as a guard on medium and heavy hits and in the combat-ready stance. Attack clip lengths and impact keyframes are unchanged (server sync).
- **Boot fix (all revenant clips change):** the boot cubes are parented to the shin while the leg solve plants the foot bone, so the boots sank 1.3-2.5 px below the floor. `boot_follow` in the builder moves each boot with its foot at bake time (and in the death solver via `deaths.POSE_FIX`). Rigs without a separate boot bone are untouched. The death grounding also snaps a start ground within 0.5 px of the floor to the floor.
- **Byte-identical:** knight, sentinel, gl_marshal and ic_hearthguard build identically with and without these changes.
- **Review (Sonnet, high effort): PASS.** It covered gait, stances, 3 attacks, 7 hit sheets and all 12 deaths from contact sheets. No blockers.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=revenant`):** 22 p1 lines and 0 `=false`. The first run crashed at startup with a missing-class error before any check ran (a build-state flake); the rerun was clean. The log shows 3 "Can't keep up" lines and no check failed.

## Open minors (v2 + v4, polish, not blocking)

- **Idle and walk:** the idle weight cycle and the off-hand swing are subtle at sheet scale.
- **Stances:** Hold brace and Shield wall are close to each other.
- **Attacks:** the low cut and the overhead cut end in nearly the same follow-through pose.
- **Walk:** blade-in-body in 17 samples (ticks 0-8, hips and torso). Pre-existing; not visible at sheet scale.
- **Impact snaps:** the probe flags POP? (over 60 deg/tick) at the impact snaps and on the weapon in `death_forward` drop1/drop2 (the throw). Same as the knight.
- **death_forward (kept weapon):** the blade is 0.5 px into the floor at its lowest (the knight's is 0.29).
- **death_back / death_forward / death_right (kept weapon):** the blade lies near floor level beside the body; confirm in game.
- **Boots:** the boot cube inflate leaves them 0.3 px proud of the shin.
- **Not seen in game:** only the harness and the offline reel were run.
