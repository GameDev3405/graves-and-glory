# Hollow King review record

The Hollow King (`gb_hollowking`, boss, 44 focus units) is built by the knight's builder
(`art/anim/knight/build_anims.py`, `GG_ANIM_TROOP=gb_hollowking`), then `export.py`, `stances.py`, `reactions.py` and
`charge.py`. His `rise` keeps Kort's timing exemption.

## Batch review 2026-11-11

Branch `anim/review-hollowking`, cut from `anim/fix-hollowking`. One HK-gated hunk in `build_anims.py` (`if HK:`) calls the
new `art/anim/gb_hollowking/hk_polish.py` on the built document. After the late chain,
`python art/anim/gb_hollowking/hk_polish.py` polishes the late clips (stances hold/fallback, stun, stun_out, knockback,
pulled) in the exported file. Every fix is a least-amount constraint, eased in and out. Clip lengths, impact markers and
timeline markers are unchanged.

### Baseline drift (no edit of mine)

- **gb_hollowking `out/`:** death_right and its drop1/drop2 (limb_clear lifts left_arm up to 9.1 deg), and rise. The
  committed exported asset already equals the rebuild for these, so `out/` was stale. The game is unchanged.
- **revenant and tc_raider `out/`:** a plain rebuild on this branch changes 18 and 20 clips (attacks, light/medium front
  hits, all deaths; tc_raider also idle_upper and attack_thrust). The HK hook never ran for them (`HK = TROOP ==
  "gb_hollowking"`; no hk_polish line in their logs). These are shared-module drift. Their files were restored to HEAD and are
  not in this commit.
- **knight, sentinel, gl_marshal:** baseline rebuilds died of memory (numpy `_ArrayMemoryError` at 1 MB with the machine's
  commit charge exhausted). Not re-run, by the coordinator's one-heavy-job rule. Their files are untouched (`git status`).

### What changed and why (29 exported clips)

- **Rags** (walk, run, the 3 attacks, stance ready/brace/wall/hold/fallback, stun, medium hits, pulled):
  - **Problem:** the three rag strips are one rigid bone hinged at the hips. Every forward thigh went straight through
    them, at 2.2-2.5 px.
  - **Fix:** every sample turns them forward by the least angle that keeps them on the front of the legs (at most 60
    deg), and they bunch (scale about the hinge, down to 0.55) where turning alone cannot clear a 60-80 deg thigh.
  - **Motion:** a soft rise and a slower fall, periodic in loops. Additive hit legs carry only the delta over the stance's
    own turn.
- **Soft motion** (3 attacks, 4 medium hits, block, pulled, knockback, stun, stun_out): cloak_a/b/c and both pauldrons
  stood still. They now run through `fullbody.soft_motion`, posed over the stance the game shows; the tines follow their
  pauldron.
  - **Stun loop:** the game loops ticks 4-24, so tick 24 returns to where tick 4 is. The pauldron slide also eases from
    tick 0 to tick 4, so the clip's own 0-24 wrap closes (scorecard loop_pos).
- **Floor:**
  - **Clips and depths:** run t1 (knee 1.34 px), stance_wall t9.5 (1.53), stance_fallback t4.25 (1.18), knockback t11,
    pulled t1.
  - **Fix:** the hips are lifted at most 1.7 px and planted legs re-solved to keep the boot. A boot under the ground is
    set on it.
  - **Easing:** each leg's re-solve fades over 6 samples as its boot leaves the ground, so there is no shin snap.
    Additive clips ramp in from the stance.
- **Cuisse** (run, side cut, cleave, pulled): thigh plates sank into the hip block at high lift, 1.9-2.3 px, hidden
  before by the rags rows. They slide down their own thigh 1.25-2.5 px.
- **Thigh** (run): the knee lifted to 81 deg and drove the thigh into the hip block (2.11 px). It now lifts 15.5 deg less
  at the peak (about 65 deg), with the shin's world angle kept. The stride is heavier and the timing is unchanged.
- **Weapon:**
  - **Cleave:** the blade went 5.3 px under the ground in the follow-through. The arm is lifted at most 13.2 deg over
    t11.5-19.25.
  - **Stun:** 4.3 px under the ground; the arm is lifted 8 deg.
  - **stun_out:** 1.5 px under the ground; the arm is lifted 2.9 deg.
- **Edge** (cleave): edge_lead was 13.94 (14.14 after the arm lift), against a limit of 13.53. The sword hand rolls 2 deg
  about y over 3 ticks each side of the impact, giving 12.40.
- **Head** (knockback): the head was thrown back into spine_fan_c (2.0 px). It tips back 14 deg less at most, over t0-9.25.

### Rows before and after

| | before | after |
|---|---|---|
| focus, exported asset | 25 clips with a FAIL, 52 rows | 7 clips, 13 rows |
| focus, composed (additive hits over the stance, as the game shows them) | 9 clips with a FAIL | 1 clip, 1 row |
| scorecard | exit 1 (cleave edge_lead 13.94) | exit 0 (4 rise rows EXEMPT) |

- **The 13 exported FAIL rows:**
  - **Clips:** knockback, pulled, medium front/back/right/left and block.
  - **Rows:** feet and blade under the ground, elbows and a knee bent backward, and the head in a pauldron.
  - **Why they are not real:** they are all on additive clips that the check plays on the bind pose. Composed over
    stance_ready (`work/review/compose.py`), none of them is there.
- **Remaining composed FAIL:** hit_knockback, armour_clip 1.91 at t10, hip_r into right_shin.
  - **Pre-existing:** identical in HEAD, previously hidden behind the rags row.
  - **What it is:** the folded right knee of the landing crouch sits under the hip plate, 0.01 px over the 1.9 line.
  - **Status:** open (fix rounds used).
- **New WARNs that were a pass before:**
  - **hit_pulled pop 9.57, t0.5, right_pauldron:** the plate answers the pull's jerk.
  - **run_upper limb_in_body 1.14, t6.25, right_foot_gear into left_foot_gear:** from the hips lift and re-solve.
- **stun_out_upper (composed):** floor WARN 0.72 at t0.75, right_foot_gear. The lead is changing how the tool pairs
  stun_out with stun_legs.

### Proposed exemptions and out-of-boundary findings

- **Exemptions:** gb_hollowking, hit_knockback / hit_pulled / hit_medium_* / hit_block, CLIP floor, limb_in_body,
  weapon_in_body, armour_clip and LIMB elbow/knee, exported values as above.
  - **Reason:** focus_check and the scorecard judge an additive clip on the bind pose, not on the stance it plays over.
  - **Better fix:** compose additive clips in the tool, instead of exempting them.
- **Model:** the rags are one rigid bone across both thighs. Splitting them per thigh, or adding a hinge, would let them
  trail each leg instead of bunching.
- **Model:** the hip block and cuisse geometry overlap at thigh lifts past 60 deg. The cuisse slide and thigh fix are
  workarounds.

### Tests

- **Unit tests:** test_scorecard, test_focus_check, test_render_geo and the six test_booth* files, all OK.
- **Earlier full runs, same code minus round-three easing:** `python -m unittest discover -s tools/anim` gave 162 OK.
  `tools/verify.py fast` gave 727 tests, one timing failure: test_where's extract took 145 s against a 30 s limit on a
  loaded machine (log `work/verify/20261112-044315-50584`).
- **Reel** (geo-knight via `GRAVES_PHASE1_CHECKS`, `GRAVES_GEO_CHECK_TROOP=gb_hollowking`, root `gb_hollowking-review2`;
  `review1` was submitted with `--suites geo-knight`, which only sets the phase 4 list, and ran nothing):
  - **Result:** 23 troop checks, 22 true, 1 false, plus the suite line false because of it.
  - **The false row:** `p1-geo-gb_hollowking-stance`, one knight in one view (ready/close, knight 0) at weight 0.98 rather
    than over 0.99 at k30. Every lag was 1-2 ticks, and the other 11 stance cases passed.
  - **Likely cause:** the blend weight comes from `StanceController` (Java), not from clip data. The server logged three
    "Can't keep up" lines (up to 160 ticks behind) during the run.
  - **Status:** not re-run; one reel per troop.

## Fix round 1 (2026-11-12)

The independent review (FIX, 7/10) found four faults, all on HK files only. Nothing else changed; rise, lengths and markers
are identical.

- **Knockback hip plate:** the folded right knee sat under hip_r (1.94 px at t10).
  - **Fix:** the plate (its own bone under the hips) flares outward about z by the least angle, at most 18.3 deg at t10.25,
    eased in and out over t6-15.25. It is keyed in `hit_knockback_legs`. The knee, boot and hit are unchanged.
  - **Result:** no hip_r row (the WARN line is 1.7).
- **Stun loop, rags:** the rags were at -34.2 deg at tick 4 and -38.4 at tick 24.
  - **Fix:** the last 4 ticks of the rag turn and fold now blend onto the curve's own run into tick 4, so speed matches at
    the wrap. The entry eases from tick 24's value, so the clip's own 0-24 wrap also closes.
  - **Every other channel:** the stun legs and upper are keyed equal at ticks 4 and 24 (the largest change was 0.01 deg).
    Every channel of both clips is now equal at the two ticks.
- **Idempotence:** each fix measures the clip it is given, so a second pass moved clips again.
  - **Fix:** `polish()` now records per stage a hash of every clip it changed (`art/anim/gb_hollowking/out/hk_polish.state.json`).
    It does nothing when the document already holds that output, and it refuses a document that holds only part of it.
  - **Test:** `art/anim/gb_hollowking/test_hk_polish.py` checks that a second BUILT or LATE pass changes no byte and that a
    partly polished document is refused.
- **out/ against the asset:**
  - **Cause:** the builder's own limb_clear reads the build preview geo (`out/preview/t1.geo.json`). Stances.py reads the
    deployed geo, where the cleave's left bracer is 1.86 px in the torso.
  - **Fix:** the BUILT stage now ends with the same limb_clear call on the deployed geo and rig. Stances.py's later pass is
    then a no-op.
  - **Result:** `out/` equals the asset on all 54 shared clips.
- **Idle rags:** `idle_legs` is now in the rags plan (turn only, full length), and idle_upper loses its 1.87 rags row.
  - **What I saw:** idle turns the rags 9-12.5 deg at full length, and ready turns them 23-31 deg at full length. Walk,
    run, brace and stun turn them 33-60 deg at 0.55-0.69 length.
  - **Hand-over:** from idle the gait controller blends into walk by hand (`GaitController.blendFrom`). I did not see that
    blend in game, so whether the length change reads as a pop is not verified.
- **Rags from the side:** they still read as thin edge-on strips. This is a model limit and Kort's eye call; it is not
  changed.

**Gate:**
- Focus check: 44 clips, 0 FAIL.
- Scorecard: exit 0, with the 4 rise rows EXEMPT.
- The allowed 9 unit-test files: all OK.
