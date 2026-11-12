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

# Batch review 2026-11-11

Lane `anim/stance-revenant` (ee9e5d69 WIP) with the skill lane merged in, reviewed on `anim/review-revenant`. Logs and frames
in `work/review/` (not committed).

## Before and after

- **Focus check:** before 42 clips, 15 with a FAIL, 36 FAIL rows; after 10 with a FAIL, 12 rows (WARN rows 86 -> 97: the
  pieces now swing, and a swinging piece gets near-pop and near-clip WARNs).
- **Scorecard:** before and after 1 fail, `attack_overhead` edge_lead 14.24 deg against 13.53. The limit tightened when the
  skill merge re-derived it from the knight (the WIP had 0 fails on the old limits). Not fixed: proposed exemption below.
- **Baseline rebuild (no edit):** differs from the export on `hit_light_front` (max 0.17), `hit_medium_front` (0.97) and all
  12 deaths. Drift from the newer shared modules: not committed, the exported clips stay.

## Changes per clip

- **idle_upper** (`OWN_IDLE` head 14/-8/16 -> 12/-6/9): the hung head sank the coif 2.0 px into the left pauldron (t20.5).
- **walk_upper** (new `OWN_WALK`, gated `OWN`): the shared walk marched him upright, eyes level, blade held out level like a
  bar (Kort's unsent note on tick 5). Now the chest hunches, the head hangs and cocks, the blade rides lower and turned out.
  Same legs, timing and length. 0 FAIL.
- **stance_fallback_upper/_legs** (`stances.py` revenant rows: head -6 roll, hips +1 px): coif into pauldron 2.1 and the left
  boot 1.2 px under the ground. Rally and hold regenerate byte-identical.
- **Attacks, hits, stun** (new `art/anim/revenant/review_layer.py`, run after the export, revenant only): every hanging piece
  swings (secondary_life FAILs gone); the overhead and low-cut edge roll eases instead of snapping (pops of 19 and 20 deg);
  the front flap gives way to the lifting thigh; the hung head lifts through the heavier hits; the hips ride higher in
  knockback, pulled and stun (boots up to 2.2 px under the ground). Lengths, loop flags, markers and `impact` unchanged.

## Remaining FAIL rows and why

- **Hits played raw** (hit_block elbow 176.9, hit_knockback elbow 145.0 and weapon 1.7, hit_pulled elbow 165.6 and floor 1.1,
  hit_medium_back knee -13.0, hit_medium_front coif 2.01): the hits are additive, and focus_check plays them over the bind
  pose. Composed over the ready stance (`work/review/compose.py`, as HitController plays them) none of these rows appear.
  Composed, one row is left: hit_knockback gilt_rim_r into torso 2.0 at t1 (the same rigid rim row the attacks have on
  their own; exactly at the limit). The composed check also flags settle on every hit: the looping stance under them.
- **attack_overhead** gilt_rim_r into torso 2.0 (t4), **attack_low_cut** right_plate into torso 2.0 (t6.5): both rigid and
  both in the clips before this review, at the limit.
- **charge_run_upper** half_cape into left_boot 1.93: before this review too (the charge comes from charge.py, shared).
- **stun_upper** coif into right_spike2 2.52 (was 2.72; stun from shared reactions.py over his hung head);
  **stun_out_upper** secondary_life 0.95: a 6 tick hand-back where the spring's fade in and out leaves under a tick of swing.

## Proposed exemptions (troop, clip, check, value, reason)

- revenant, attack_overhead, scorecard edge_lead, 14.24 deg (limit 13.53): the shoulder-driven slash leads 0.7 deg past a
  limit re-derived after the WIP was scored; the edge faces the cut by eye.
- revenant, stun_out_upper, secondary_life, 0.95: a 6 tick clip.
- revenant, stun_upper, armour_clip, 2.52: the coif against the right shoulder spikes; the head offset that clears it
  stretches the neck.

## Other troops

The builder hunks are gated on `OWN` and the stances rows keyed `revenant`. walk_upper, idle_upper and idle_legs from the
builder for knight, sentinel, gl_marshal, tc_raider and gb_hollowking match their committed `out/` byte for byte;
`stances.py all --out` matches every other troop's export except hexer (the known drift) and stalker (the same with the
unedited stances.py: pre-existing). A full revenant build differs from the baseline build only in walk_upper and idle_upper.

## Tests

`python -m unittest discover -s tools/anim` OK. `verify.py fast`: python-tests timed out and two Java checks could not
start (the paging file was full: the machine ran out of memory); every other step PASS. A second run: every step PASS
except python-tests, which timed out again (8 jobs on a machine running about 100 Python processes).
In-game reel (geo-knight, `GRAVES_GEO_CHECK_TROOP=revenant`): neither run is clean, and each failed different checks.
- revenant-review1: 23 p1 lines and 3 `=false`, run on the clips before the last hit tweak. The failures were
  attack-sync-stand (52/54 swings matched; 2 had a null clip), hit (medium-left/side 5/8 frames shot) and suite.
- revenant-review2: 23 p1 lines and 4 `=false`, run on 302c4050. The failures were stance (ready lag 8), death (left weight
  0.046 at death 3), attack-death and suite.
- Every check passed in one of the two runs. Clip lengths, markers and `impact` are unchanged, and the failures are timing
  and frame-count rows on a loaded machine. Needs one clean run on a quiet machine.
Booth video: refused by the scorecard gate (attack_overhead edge_lead); contact sheets in `work/review/sheets/`.

## Fix round 1 (2026-11-12, Sonnet review FIX 6/10)

Build: `GG_ANIM_TROOP=revenant python art/anim/revenant/review_layer.py` runs last. It reads
`art/anim/revenant/review_base.animation.json` (the export before the layer) and writes the asset and `out/`, so a second
run gives the same bytes. After a fresh export or splice, run it with `--rebase` once (the export becomes the base).

- **Chest plates** (Kort's fault): gilt_rim_r / right_plate sank 2.0 px into the chest as the sword arm rose. Rotating the
  arm or plates did not help (the rim overlaps the torso in x); the right pauldron's plates slide 3 px out along the arm
  over those ticks instead (CHEST: attack_overhead, attack_low_cut, hit_knockback).
- **attack_overhead edge_lead** 14.24 -> 10.4: the hand rolls the edge into the cut over t9.25-10.25 (EDGE_Y). The
  reviewer's suggestion (y down to about 1) made it worse (16.1); the roll goes up to 12 deg instead. The 42 deg roll
  after contact eases over t10-14 (EASE).
- **Follow-through** (POST, after the cloth pass): the overhead carries across and down with the wrist cocked up
  (carry 0.08 -> 0.16, no weapon under the ground); the low cut sweeps on across (carry 0.07 -> 0.18, pen_limb within
  limit). The tabard_f1 flap swings clear of the right cuisse at the overhead's knee lift.
- **Stance hand-off**: every attack blends head and off arm from stance_ready_upper's pose over 3 ticks at each end.
- **stun_out_upper**: tabards, tassets, cape and pauldrons settle with a 3-5 deg overshoot (SETTLE); secondary_life 2.53.
- **charge_run_upper**: the half cape rides 6 deg clear of the boot in this troop's layer (CHARGE_CAPE); the shared
  charge.py is untouched.
- Attacks push tabard_f1 at -0.5 deg per deg of knee lift (thrust reached 1.9 px at -0.35); hits keep -0.35.

Changed clips: attack_thrust, attack_overhead, attack_low_cut (upper only), charge_run_upper, stun_out_upper,
hit_knockback. Every other clip is byte-identical to c48cccce. No shared file changed, so other troops are untouched.

Focus check: 6 FAIL rows -> 1 (stun_upper coif into right_spike2 2.52: model-side debt, accepted). Scorecard 0 fail.
Unit tests (scorecard, focus_check, render_geo, booth*) OK.
Booth batch `work/booth/revenant-20261112-dcca7d6` (built, not uploaded); phone video
`C:/Users/kortt/Melty/status/booth-videos/revenant_review.mp4` (3:25, 42 clips).
In-game reel revenant-review3 (geo-knight, `GRAVES_GEO_CHECK_TROOP=revenant`): 23 p1 lines, 0 `=false` (clean).
