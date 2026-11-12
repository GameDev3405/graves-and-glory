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

# Batch review 2026-11-11

Branch `anim/review-raider` (lane `anim/stance-raider` WIP 268da402 plus the skill lane). The stance redo was spliced in (stance and idle clips only), and a late layer `art/anim/knight/raider_review.py` fixes the attacks, hits and stun. Every other clip is byte-identical to the committed asset.

Correction (fix round 1): commit 50f9b616 changed 42 of the 65 clips, not 32. The ten `_legs` partners of the LIFE clips (and the gait legs) got skirt and tail keys. No death changed.

## Baseline drift (not touched)

A full rebuild of tc_raider with HEAD code differs from the committed `out/` in 31 clips:
- idle and the 12 stance builder clips: the WIP;
- attack_thrust, attack_overhead(+_legs), attack_low_cut(+_legs), hit_light_front, hit_medium_front and all 12 deaths: shared-module drift.

The revenant shows the same drift on HEAD (attacks, hit fronts, deaths). That is why only the stance and idle clips were spliced (`work/review/splice.py`, unversioned), not rebuilt.

## Stance redo (what can play: no shield and no spear, so `holdStance` gives hold; brace, wall, shield_up and ward cannot play)

| Clip | What makes it the raider's |
|---|---|
| idle | Cutlass carried low and forward, free fist on the hip; the deck-roll hip sway. |
| stance_ready | Wide, low sea legs, chest side-on, deck roll. Builder change: the free hand drops and opens out from the hilt (`OWN_UPPER` left_arm (8, 0, -14)). Feet within 0.98 px of the attacks' tick 0. |
| stance_hold | Cutlass cocked high by the helm, free hand out low, legs from the brace row. |
| stance_rally | Cutlass brandished out to the side, chest turned behind it. Slowed (`RALLY_OWN` lift 104, keep 0.6, fore -30): the scorecard's ang_speed_weapon failed at 64 deg/100ms. |
| stance_fallback | Backward shuffle turned side-on behind a low cutlass, the hips rolling foot to foot. Hip drop removed (base 0.7): the foot was 1.96 px under the floor. |

All of these were looked at in front, side and three-quarter views, near and at game16.

## Late layer `raider_review.py` (tc_raider only; edits the exported asset; idempotent: a second pass is byte-identical)

Run it after build, export, stances, reactions and charge. The full recipe is in the docstring of `art/anim/knight/build_anims.py`.
- **LIFE:** `fullbody.soft_motion` on the attacks, the medium hits, hit_block, hit_pulled, hit_knockback, stun_upper and stun_out_upper. This covers kelp_helm, kelp_left_shoulder, scarf, skirt, the hip sash `tail`, and both pauldrons.
  - stun_upper's loop runs to a steady swing and closes exactly.
  - stun_out_upper reads stun_legs but never writes the hip pieces (stun_legs owns them).
  - Hit cloth is softened over [1 2 1] three times, so there is no pop.
- **SASH:** the hip sash follows the left thigh in walk_legs, run_legs, stance_fallback_legs and the LIFE legs clips. Before, the greave cut through it.
- **ROLL** (overhead, low cut): the edge's half turn onto the cut no longer snaps in the wind-up.
  - It turns at most 11.5 deg/tick from tick 0 to 1.25 ticks before the arc, then finishes by the arc's first fast sample.
- **HAND_EASE** (overhead t11.25-13.25, low cut t8.75-11.5): the after-cut wobble through the Euler pole becomes one Hermite turn.
- **SETTLE:** the attacks ease back to their first pose over the last 3 ticks, as a rotation, not an Euler blend.

## Rows (focus check)

- **Before:** 14 clips with a FAIL, 33 FAIL rows.
- **After:** 7 clips, 11 rows.
- **Fixed:**
  - every secondary_life;
  - walk_upper and stance_fallback_upper armour_clip (greave into sash);
  - grip_roll on overhead (42.2) and low cut (46.1);
  - overhead pop (19.9) and low cut jitter (11.8);
  - settle on the three attacks;
  - the knockback greave into sash.
- **New WARNs:**
  - attack_low_cut weapon_in_body 0.85 px (belt) at t10, from the hand ease;
  - stance_wall(_up) knee_aim 54 (wall cannot play for this troop).

## Open (not fixed: fix rounds used up; pre-existing, unchanged)

- **hit_pulled:** floor 1.64 at t5.25; elbow 165.6 at t5.75 (reactions.py, common).
- **stun_upper:** floor 1.72 at t0.75; head into right_shell_barn 2.36 at t1.0 (reactions.py).
- **stun_out_upper:** floor 1.72 at t0.75 (reactions.py).
- **hit_knockback:** weapon_in_body 1.81 (right_leg) at t1.75; floor 1.5 at t10; elbow 145 at t7.5 (reactions.py).
- **hit_medium_front:** head into left_shell_barn 2.32 at t4.5 (builder hit_clips: the head tilts into the raider's shoulder shell).
- **hit_medium_back:** knee -13 at t11.5 (builder).
- **hit_block:** elbow 176.5 at t8.25 (builder).

## Tests

- **Scorecard:** 42 clips, 0 failing.
- **Unit tests:** `unittest discover -s tools/anim`: 152 OK.
- **verify fast:** every check passed except python-tests, which hit verify's 600 s step timeout (exit 124) on the overloaded machine. That suite was re-run alone: see the report.
- **In-game (checkq, root `tc_raider-review1`, geo-knight with `GRAVES_GEO_CHECK_TROOP=tc_raider`):** NOT a pass. 23 p1 lines, 6 `=false`, 4 "Can't keep up".
  - The failures are timing checks:
    - run-side: client speed spread 0.116-0.446;
    - one of 124 swings unmatched (clip null), which fails attack-sync-stand, attack-plays and attack-footwork together;
    - brace stance ramp lag 5-6 ticks against 4.
  - Every frame, hit, hit-pulled and death check passed.
  - Other lanes' reels on this machine the same night failed timing checks the same way.
  - Needs a re-run on a quiet machine.

# Fix round 1 (2026-11-12, after the independent Sonnet review, 6.5/10)

Tools at 861c69c2: hits and the stun shake-off are judged over stance_ready, as the game plays them. All fixes are in the tc_raider layer `raider_review.py`; 19 clips changed against 50f9b616, no death, every length and impact marker unchanged.

- **Build recipe:** in the docstring of `build_anims.py`. The layer is the last step. Run on the asset, it writes the same bytes to the asset and to `art/anim/tc_raider/out/`, so out/ now equals the shipped file. `tools/anim/test_raider_review.py` runs the layer twice on the shipped file and asserts byte-identical output.
- **HELM** (hit_medium_front/back/right/left, hit_block, hit_knockback, stun_upper): one measured rule.
  - First the shoulder shell slides down its arm off the helm, at most 1.5 px.
  - Then the head rolls away from it, at most 12 deg.
  - Last, in hits only, the arm gives back part of its flinch. It was not needed.
  - The target is the rest fit plus 1.5 px.
- **FLOOR** (hit_pulled, hit_knockback, stun): the hips rise by what each sample needs, to 0.4 px. The stun keeps its loop closed: a constant over the loop plus an entry bump.
- **STANCE:** stance_ready's free upper arm turns elbow-out by 19 deg. Its bracer sat 1.69 px in the chest on every tick, which the attacks inherit once they start on the stance (scorecard pen_limb, attack limit 1.39).
- **HANDOFF:** the three attacks' first and last poses are now exactly stance_ready's at one phase (the free arm, torso, head, legs and hips). They ease out over 2 ticks and back over the last 3; the right arm and the blow are untouched.
- **stun_out:** with the new tool (a stun-to-stance handover), stun_out has no FAIL and no knee_aim row. The round-0 floor and knee_aim rows came from stun_legs being added twice, so nothing was changed for them.
- **Rows:** before, at 861c69c2 on the 50f9b616 asset, 8 clips had a FAIL (10 rows). After: 0. Scorecard: 42 clips, 0 failing.
- **Open:**
  - attack_low_cut still shares the overhead's wind-up.
  - limb_in_body WARN in the stun (bracer 1.99 px), from reactions.py.
  - attack_overhead armour_clip WARN 1.86 (head into right shell, t14.5).
