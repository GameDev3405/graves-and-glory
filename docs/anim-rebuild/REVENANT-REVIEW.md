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
