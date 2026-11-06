# ic_breaker review record (2026-11-06)

The Iron Clans Shieldbreaker, the first greataxe unit: a long two-handed axe (`greataxe`, sunder). Its builder reuses the spear builder's two-handed solve, footing, carry, hits and falls, and adds the greataxe's own guard, stances and swings: `python art/anim/greataxe/build_greataxe_anims.py` (`GG_GREATAXE_TROOP` picks the troop; the default is ic_breaker). Its rig comes from `art/anim/common/troop_rig.py ic_breaker`.

- **Guard:** the axe is held across the body at the high port. The right fist is low before the belly, the head is up by the right shoulder with the bit toward the foe, and the left fist is up the haft. From here the chop needs only a heave and the hew a coil.
- **Attacks:** inside a 28-tick clip under the 32-tick cooldown, each with an `_legs` clip:
  - overhead_chop (impact 12)
  - side_hew (impact 12)
  - haft_jab (impact 7)

  The timeline `impact` marker sits on the server's impact tick.
- **haft_jab is a cross-check:** the haft is driven level across the chest into the foe. It reads in the front and three-quarter views. In the side view it is seen end-on.
- **Bit:** every key names the bit direction, so the bit leads each swing. The chop swings in the sagittal plane and the hew level.
- **Grip:**
  - `GREATAXE_PROBE=1` prints each swing's grip error, axe lowest point and shaft-in-body samples.
  - Results: chop 1.95 px (none in body), hew 3.17 px (one sample), jab 1.44 px (none). The axe stays at least 2 px above the soles.
- **Round 1 fix: haft_jab recovery.** After the check the haft swung back out forward, which read as a second, spear-like thrust. The recovery now pulls in and lifts back toward the carry.
- **Round 1 fix: overhead_chop.** A mid-arc key at atk 11.5 splits the drop across the impact tick.

## Review

- **Round 1 (Sonnet high, reel `reel_brk_v1`): FIX NEEDED.** The haft_jab recovery read as a second thrust. The fixes above.
- **Round 2 (Sonnet high, reel `reel_brk_v2`): SHIP WITH MINORS.** The cross-check reads as one strike; the body lunges and drops on impact, then the axe returns to the carry with no second thrust. No new clipping.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. All 22 standing swings and 4 walking swings matched the server impact.
- **Verdict: finished.** Video: `ic_breaker_v2.mp4`.

## Open minors (polish, not blocking)

- **overhead_chop** (t305→t306): the drop still reads as a one-frame snap at 20 Hz sampling. The atk 11.5 key falls between sampled ticks; in game, the renderer's partial-tick interpolation shows it.
- **overhead_chop** follow-through (atk 20): a slight lift before the settle.
- **side_hew:**
  - The overshoot is hidden in the side view.
  - The butt brushes the right flank at t11.25.
  - The grip error is 3.2 px at t11.
  - The gather turns the bit between t18 and t20.
- Walk and stances read weakly at reel scale.
- Hit folds are deep.
- Death floor contact was judged at low confidence.
- The reel crops the head at the top of frame during the chop wind-up.
