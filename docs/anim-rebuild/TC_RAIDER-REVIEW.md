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
