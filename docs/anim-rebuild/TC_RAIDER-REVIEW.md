# Drowned Raider (tc_raider) review record (2026-11-06)

First shieldless one-handed sword unit, built alongside the revenant. It shares the knight's clip builder (`art/anim/knight/build_anims.py`, `GG_ANIM_TROOP=tc_raider`, the shieldless FREE_GUARD / FREE_STANCE / FREE_FIST branch) and was judged against STYLE.md by a screenshot reviewer over three rounds.

- **Review: FIX FIRST.** Main problems:
  - stances and the combat-ready idle lacked a free-hand role and weight;
  - the overhead load was late and weak;
  - hits were under-sold.
- **Fix round 1:**
  - a free-hand guard and stance pose;
  - a wall stance upper body;
  - stance breathing;
  - a stronger shieldless hit peak;
  - an overhead key at clip tick 2.
- **Re-review 1:** stances and idle FIXED. The overhead was still MAJOR (load not readable from the side). Hit medium was flagged for an excessive torso twist.
  - Part of the "late load / 0.3 s impact lag" came from the reel's segment-time labels, which include a 6-tick lead-in. The reel now labels attack tiles with clip time and flags the server impact tick.
- **Fix round 2:** the shieldless overhead load rocks the hips back and sits deeper (ticks 2-7.5). The free hand reaches toward the foe through the load, then snaps to the hip as the blade falls.
- **Re-review 2: SHIP WITH MINORS.**
  - Load FIXED: a held wind-up of about 0.25 s, readable from the side.
  - The blade is past the cut on the server impact tick (clip +0.50 s).
  - Hit medium twist downgraded to MINOR.
- **In-game checks:** the geo-knight suite with `GRAVES_GEO_CHECK_TROOP=tc_raider`, 22/22.

## Open minors (polish, not blocking)

- **Overhead:**
  - The blade sweeps about 150 degrees between clip +0.45 and +0.50; add an in-between near +0.475 so it can't read as a pop.
  - A small re-lift at +0.40 to +0.45 (reads as anticipation).
- **Hit medium (front):** the torso twists past the reel's roughly 30 degrees, almost side-on at 0.40 s; trim it by about a third.
- **Idle:** the weight rock is weak in the front view.
- **Shared with the revenant:**
  - death knee-buckle beat; death-left/right corpse offset;
  - run lean and left-arm pump;
  - ward blade guard line and stiff ward arm;
  - no skid step on hits.

# Tc_raider: v2 style + v4 deaths (2026-11-07)

The tc_raider now builds in the knight's v2 style, rebuilt for one hand and an empty off hand, and has the 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` and `_drop2` (both drop the cutlass). Deaths come from the shared module `art/anim/common/deaths.py`.

- **Style:** pelvis-driven drive, graded light/medium/heavy hits, doubled idle weight shift, distinct stances. The off arm is a counterweight: it flares out on the overhead wind-up, folds across the chest on the thrust and comes up on hits. Attack clip lengths and impact keyframes are unchanged (server sync).
- **Per-troop tuning:** the overhead and low cut use tc_raider-only arm and blade values (`CU` in the builder). The cutlass otherwise clipped the head and leg and reached 1.4-1.8 px below the floor.
- **Byte-identical:** knight, sentinel, gl_marshal and ic_hearthguard build identically with and without these changes.
- **Review (Sonnet, high effort): PASS.** It covered gait, stances, 3 attacks, 7 hit sheets and all 12 deaths from contact sheets. No blockers. The reviewer thought the blade in `death_right` might dip below the floor. The probe puts it at -0.06 px, so it does not.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=tc_raider`):** 22 p1 lines and 0 `=false`. The log shows 3 "Can't keep up" lines and no check failed.

## Open minors (v2 + v4, polish, not blocking)

- **Idle and walk:** the idle weight cycle and the off-arm counterswing are faint at sheet scale.
- **Stances:** Hold brace and Shield wall are almost the same pose.
- **Attacks:** the thrust, overhead and low cut share one recovery pose.
- **Overhead:** the tip passes close to the lead leg at t=18.
- **death_forward / death_right (kept weapon):** the cutlass is 0.10 and 0.06 px into the floor at its lowest.
- **death_forward drop1/drop2:** the probe flags POP? on the weapon throw (138 deg/tick at t12.5), as the knight does on its throws.
- **Not seen in game:** only the harness and the offline reel were run.
