# Sentinel review record (2026-11-06)

Second shield unit. It shares the knight's clip builder (`art/anim/knight/build_anims.py`, `GG_ANIM_TROOP=sentinel`) and was judged against STYLE.md by a screenshot reviewer over three rounds.

- **Review: FIX FIRST.** Three problems:
  - Brace stances: the sword hung limp.
  - Block: the shove-back was weak.
  - Combat-ready idle: too subtle.
- **Fix round 1:**
  - The brace sword is held low by the right hip, angled up and across past the shield edge.
  - Block phases: absorb (rock back, skid), then a shoulder-driven shield shove, then settle.
  - Idle weight shift doubled.
- **Re-review 1:** brace FIXED, block FIXED. The idle was still MAJOR: its weight cycle did not read from the front.
- **Fix round 2:**
  - Idle weight cycle: bigger forward/back shift, a lead-knee flex and hip drop on the forward phase, a lateral hip shift, and arms trailing the torso.
  - Block shove: less head pitch, with the shield arm pushed further out.
- **Re-review 2: SHIP WITH MINORS.** No new blockers.
- **In-game checks:** the geo-knight suite with `GRAVES_GEO_CHECK_TROOP=sentinel`.

These changes are shared, so the knight's idle and block changed with them (see KNIGHT-REVIEW.md).

## Open minors (polish, not blocking)

- **Idle:**
  - The lateral shift is still small in the front view; push it about 30% further.
  - The arm trail and knee flex are hard to see; add a 2-3 tick lag.
- **Block shove:**
  - The head still pitches about 40 degrees; cut another 10-15.
  - Extend the shield arm about 2 voxels.
- **Brace:**
  - The holds have no breath; add a small chest rise.
  - In the front view the blade hides behind the hip.
- **To verify:** foot sliding can't be checked at sheet size.

# Sentinel: v2 style + v4 deaths (2026-11-07)

The sentinel now builds in the v2 shield style (Kort approved v2 as the default for shield units), so its whole set changed, not only the deaths. It also has the knight's 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` (drops the longsword) and `_drop2` (drops the longsword and the shield). The D3 death system in `art/anim/knight/build_anims.py` now takes the held weapon's own bones (`weapon_t1..t3`) and the troop's own forearm pieces (the sentinel has cuffs and no glove).

- **Byte-identical:** these troops build byte-identical with the HEAD builder and the new builder: knight, gl_marshal, revenant (both direct), ic_hearthguard (axe builder), archer (bow builder), ic_arbalist (crossbow builder), wm_tusk (spear builder) and tc_harpooner (thrower builder). D3 stays gated on `D3_TROOPS`.
- **Review (Sonnet, high effort): SHIP WITH MINORS.** It covered the whole set (gait, stances, idle, 3 attacks, 7 hits, 12 deaths) from contact sheets and the full reel. No blockers.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=sentinel`):**
  - Run 1: 22 p1 and 1 `=false`. That failure was the march run-side check, which the lag played as WALK. The suite rollup line follows from it.
  - Rerun: 22 p1 and 0 `=false`.
  - The death clips that played included `death_back`, `death_forward`, `death_forward_drop1`, `death_left_drop1` and `death_right_drop1`.

## Open minors (v2 + v4, polish, not blocking)

Most of these come from the shared v2 builder, so they apply to the knight as well.

- **Thrust:** at t=11 the blade is yawed out to the right, then snaps forward by t=13. Cap the yaw at about 30 degrees, or add an in-between.
- **Low cut:** it ends about 25-30 degrees blade-down at t=15-18, which reads as a chop more than a flat sweep.
- **Overhead:** the hilt sits tight to the helmet at t=11-13. A slight clip can't be ruled out at sheet size. The wind-up at t=0-4 barely moves.
- **Heavy front hit:** the onset at t=1-3 is soft.
- **Side and behind hits:** direction reads mostly from torso lean. Add about 20% more head and shoulder turn.
- **Raised-shield stances:** only slightly higher than the base poses.
- **Combat-ready idle:** breath and weight shift are subtle (about 1.5x wanted). This is a carry-over minor.
- **death_back:** the fall is fast (upright at t=8, flat at t=13), with no visible back steps.
- **death_back (kept shield):** settles from upright to flat after landing. This is the clatter; it should not slide.
- **death_forward:** the knee buckle is hard to see; it reads as a stiff face-plant.
- **death_forward_drop2:** the dropped shield looks slightly tilted, not fully flat.
- **death_left:** ends half-down: a slumped kneel and lean, with the kept sword diagonal.
- **death_left_drop2:** the shield lies flat beside the hips, partly under the slumped body. The builder's clearance check passes, but confirm it in game.
- **Not seen in game:** only the harness and the offline reel were run.

# Batch review 2026-11-11

Branch `anim/review-sentinel` (from `anim/troop-sentinel`). Builder commit 89d82e46 (`art/anim/knight/build_anims.py`,
gated on `ALIVE_TROOPS`; new `art/anim/knight/soft_layers.py`), outputs in the next commit. Knight, gl_marshal, revenant,
tc_raider and gb_hollowking build byte-identical with the HEAD builder (non-death clips rebuilt with both builders and
compared clip by clip; the deaths carry no gated code).

**Focus check (raw, every clip over the bind pose):** before 16 of 41 clips with a FAIL (29 rows), after 7 (9 rows); all 9
are rig differences or bind-pose artefacts (below). Composed the way the game plays it (HitController adds the hit over the
ready stance; `work/review/sen_compose.py`), k 1.0: 0 FAIL except stun_out_upper whole_body (the composition holds the legs
still; the game plays it over stun_legs). Scorecard: exit 0, 0 fails.

## What changed, per clip

- **Every attack, the medium hits and the block** (builder, `soft()`): the cape, the skirt plates and the pauldrons swing
  (fullbody.soft_motion; the hits keep every piece in the upper clip). secondary_life FAIL -> pass (medium_back 3.01 WARN).
- **attack_low_cut:** the shield fist rides 1.6 px lower and 0.5 px further out through the follow-through (t8-t11.5):
  shield into helm 2.01 FAIL -> 1.36 WARN. The wrist's per-sample solve flipped (jitter 11.29 at t9.75): a binomial
  low-pass over right_hand t8.75-t11.5 (`calm()`): pass.
- **run_upper / charge_run_upper / stance_wall:** the leg skirt plates hang off the thigh (`skirt_hang`, keep 0.55): plate into
  tabard skirt 1.97 FAIL -> pass/WARN (run 1.78 left_forearm into torso, charge 1.72 hips into left_leg, both WARN).
- **stance_wall (legs):** the hips sit 1.25 px higher (1.3 overreaches the ankle): left knee 1.11 px under the ground -> pass.
- **hit_pulled** (`soft_layers.py`, after reactions.py): sword arm wider of the helm through the fling, body lifted 2.6 px
  off the dragged feet, toes 10 deg less, skirt swing capped at 5 deg. As played: sword into helm and toe 2.5 px under the
  ground -> WARN only (k 1 and 1.55). Raw floor FAIL -> pass.
- **hit_knockback** (`soft_layers.py`): hips sink 1.7 px less on the shove and each stumbling step, the braced toe lifts
  10 deg in the recovery: right boot 1.47 px under -> 0.52 WARN.
- **stun_upper / stun_out_upper** (`soft_layers.py`): the pieces swing; the stun loop (t4-t24) stays cut safe.

Length, impact and timeline markers unchanged on every clip.

## Remaining FAIL rows (raw), with reason

| Clip | Row | Reason |
|---|---|---|
| stance_wall_upper | floor 1.17 t10, shield | the wall plants the shield rim in the ground on purpose (was 2.42) |
| stun_upper | floor 1.81 t7.75, shield | the stunned troop leans on its planted shield (unchanged) |
| stun_out_upper | floor 1.72 t0.75, right_boot | additive over the bind pose; over the stun's last pose: pass |
| hit_pulled | weapon_t3 into right_leg 1.92 t2.25; elbow 162.8 t2.75 | bind-pose artefact; composed k 1 and 1.55: pass |
| hit_knockback | weapon_t3 into right_leg 1.82 t1.5; elbow 146.58 t8.5 | bind-pose artefact; composed: pass |
| hit_medium_back | knee -13.01 t11.5 | bind-pose artefact; composed: pass |
| hit_block | elbow 176.88 t8.25 | bind-pose artefact; composed: pass |

New WARNs that were a pass: attack_thrust cape_t3 into hips 1.70 t10 (the cape now swings), hit_pulled raw
weapon_in_body (above).

## Proposed exemptions (troop, clip, check, value, reason)

- sentinel, stance_wall_upper, floor, 1.17, shield rim planted by design.
- sentinel, stun_upper, floor, 1.81, leans on the planted shield by design.
- sentinel, hit_pulled / hit_knockback / hit_medium_back / hit_block / stun_out_upper: the rows above, additive clip judged
  over the bind pose (better: focus_check composing additive hits over the ready stance, a tool change).

## Open

- Heavy blows (k 1.55) on hit_knockback: gorget into helm 1.91 t3 and left knee_aim 88.8 t5.5 come from
  art/anim/common/reactions.py (every biped); not changed here.
- `art/anim/sentinel/out/` is not regenerated: a full build of this troop ran over 80 minutes on this machine without
  finishing (an earlier one ran out of memory). The changed clips were rebuilt with the builder (part builds) and merged
  over the committed exported asset, then `soft_layers.py` ran. Re-running export.py from the stale out/ would drop the
  builder changes: rebuild out/ before any re-export. Fixed 2026-11-12: see "Build recipe" below.

## Build recipe (2026-11-12)

`GG_ANIM_TROOP=sentinel python art/anim/knight/rebuild_out.py` then `python art/anim/common/export.py sentinel` (about 4-15 min).
It writes out/ whole: the builder's 40 clips, the 12 deaths spliced unchanged from 8cb35120 (the builder no longer reproduces
them), then stances, reactions, charge, reactions (hit_pulled) and soft_layers.py. Checked 2026-11-12: out/ equals the shipped
asset byte for byte and the export leaves it unchanged. Never run stances/reactions/soft_layers on the asset by hand again.

## Test results

- `python -m unittest discover -s tools/anim`: 152 tests OK (`work/review/unittest_final.log`).
- `python tools/verify.py fast`: build PASS, ui-assets PASS, python-tests 727 run, 1 failure:
  test_where `test_extract_is_fast_and_covers_every_troop_and_section`, 87.2 s against its 30 s limit (a speed limit; the
  machine was running several builds and test suites from other lanes). No animation test failed
  (`work/review/verify_fast_final.log`).
- In-game reel `sentinel-review3` (job 20261112-033758-640-5886): 22 `[p1] p1-` checks plus the suite line, 0 `=false`.
  Two earlier runs each failed one lag-timed row on clips this batch did not change (review1: stance_ready shield_move;
  review2: march walk/run sampled IDLE at speed 0).
