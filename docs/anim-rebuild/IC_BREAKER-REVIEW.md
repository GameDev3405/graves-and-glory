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

## v2 feel (2026-11-07)

The knight's approved v2 style is ported to the greataxe builder (`art/anim/greataxe/build_greataxe_anims.py`, which reuses the spear builder's v2 tables). The breaker keeps its heavy two-handed axe and slow, committed swings. `GG_V2=0` builds the old style. Deaths are unchanged.

- **Pelvis-driven blows:** the overhead chop, side hew and haft jab each load into the back hip. The pelvis then turns and drops into the blow, and the axe follows the hips. Both fists stay on the haft.
- **Side hew recovery fixed:** the right hand's rotation channel ended more than 180 deg from its start. HEAD had this too: the end key was at z -356.9. During the in-game fade back to the stance, the axe spun a full turn. Now the recovery lifts the axe up in front on the left (t21). A geodesic key at t19 rolls it evenly back to the guard, at no more than about 13 deg per quarter tick. Any attack rotation channel whose end is more than 180 deg from its start is re-branched to an equivalent euler that is visually continuous. The swing's fastest step fell from 377.7 deg/tick (HEAD, in the recovery) to 252.9 (t11.5, the strike itself).
- **Graded hits:** a light blow is a knee flinch at 0.4 of the peak. A medium blow throws the upper body further, holds it a beat, shifts the pelvis and catches it with a recovery step. A heavy blow plays the medium clip at 1.55 times its size, so it reads as a stagger.
- **Idle:** the knight's weight cycle: breath, then a slow shift from foot to foot.
- **Stances:** ready, brace, hold the line and ward follow the spear family's v2 poses, carried by the axe.
- **Probe:** no new shaft or fist in the body, and no new pops. The counts below were already present at HEAD and the review judged them as the haft resting against the torso and arm:
  - stance_wall: 81 shaft-in-body samples (HEAD 81);
  - hit_medium_front: 3 (HEAD 5);
  - hit_medium_right: 10 (HEAD 9).
- **Byte identity:** knight, archer and gl_marshal build byte-identical to HEAD. The greataxe builder feeds only ic_breaker.
- **Review (Sonnet high, sheets, compared with knight v2): PASS WITH MINORS.** One fix round re-reviewed the side hew recovery: PASS WITH MINORS, no new blockers.
- **Harness:** `geo-knight` with ic_breaker, run twice: 22 `[p1]` checks each run. Each run failed only attack-sync-walk and the suite roll-up. Every sampled swing matched the server: walk 3/3 in both runs, stand 27/27 and 24/24. Only 3 walking swings landed in the window, one below the check's floor of 4. Run 1 logged 3 "Can't keep up" warnings, run 2 none. The original breaker rollout sampled 4. Clip lengths and impact markers are unchanged from HEAD, and the server's swing cadence does not read clip data, so the shortfall is in the duel's sample size, not the animation.
- **Minors:**
  - The axe head comes close to the helmet in the chop (t13/t15), the side hew wind-up (t13/t15), the jab (t18) and the medium right hit (t6). It does not clip.
  - The light front hit is a very small flinch.
  - Ready and brace read close. The walk is near-identical in side view. The idle weight shift is subtle next to the knight's.
  - The side hew's grip error peaks at 3.2 px.
