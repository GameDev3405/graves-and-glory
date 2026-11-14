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

## Model fix 2026-11-13: rags in three bones

Kort (2026-11-13): "Hollow King rags (split into more than one bone)".

**Model** (`art/hand/gb_hollowking/build_gb_hollowking.py`):
- The one `rags` bone (three flat strips on a pivot at the body origin) is now three bones under `body`. Each one is hinged on
  the belt line at its own strip's top: `rags_r` over the right thigh (3x12 px), `rags_c` between the thighs (3x14), and
  `rags_l` over the left thigh (3x10).
- Every strip keeps its box, UV and place at rest; the world boxes and the textures are byte-identical. Only the pivots
  moved. There is no mid joint: the strips reach only 1-5 px past the knee, so a joint there would bend a tiny flap.

**Clips** (`art/anim/gb_hollowking/hk_polish.py`, `fix_rags`):
- Before: one turn plus a fold (scale) for the whole skirt, set by the forward thigh.
- Now: each group is a damped spring toward plumb (period 0.5 s, damping 0.45). It is clamped to the range in which it
  clears the legs and the ground, and it is carried by a thigh that pushes it. So each strip trails its own thigh by a
  beat in the gaits, sways and settles after attacks and hits, and hangs to gravity in the deaths. There is no scale.
- One-shot clips limit a push to 6 deg a sample, so a landing body lifts the strip a few samples early instead of in one
  frame. This cleared a pop FAIL on the forward deaths.
- The rag fix now runs after the thigh fix, so it sees the final legs. The cuisse and plate fixes no longer measure
  against the cloth.
- Every clip that keyed `rags` now keys the three bones instead: the gaits, attacks, stances, hits, deaths and rise, plus
  the late clips (hold, fallback, stun, knockback, pulled).
- Timing: `animation_length`, `loop` and `timeline` are equal to HEAD on all 67 asset clips and all 54 out/ clips.

**One other bone changed:** `run_legs` `left_cuisse` position.
- At HEAD it slid down to 2.5 px to clear the old single rag board.
- That board is gone, and the cuisse now measures only the body and legs, so it slides 1.75 px, like the right one.
- No other bone changed in any clip.

**Rows** (focus check):
- Before: 44 clips, 0 FAIL, 109 WARN rows.
- After: 44 clips, 0 FAIL, 109 WARN rows.
- The forward deaths and fake_death now show their worst floor row as `rags_c` at 2.48 px. That is a fall, so it is not
  gated (before, the worst was `weapon_t3` at 1.81 px or a boot at 1.55 px). The cloth lies on the ground there.
- Scorecard: exit 0, with the same 4 EXEMPT rows on rise.
- Exemptions: there was no rags entry, so none was removed.

**Rag tip at hand-overs** (px, hips frame, worst strip):

| hand-over | HEAD | now |
|---|---|---|
| idle -> ready first frames | 4.28 | 2.36 |
| idle -> walk first frames | 5.79 | 6.84 |
| idle -> run first frames | 7.22 | 7.71 |
| ready -> walk first frames | 5.03 | 4.55 |
| ready -> run first frames | 5.16 | 5.41 |
| walk -> run first frames | 2.02 | 1.21 |
| in game, idle -> walk at matchPhase | 4.87 | 3.52 |
| in game, walk <-> run at the same phase, worst | 3.03 | 8.61 |

The 2 px target is not met. Each strip now lies on its own thigh, and the thighs themselves jump:
- The right thigh is at -61 deg at walk t0 and at -26 deg in idle.
- At the same phase, walk and run differ by up to 49 deg at the thigh, and the knee jumps up to 5.8 px.

A strip that follows its leg jumps with that leg. Meeting 2 px would need the strip to pass through the thigh, or a
change to the gaits (other bones). In game, the start from idle or ready is blended over startBlend ticks; walk <-> run
is not blended.

**Renders:** `work/modelfix/render/before` and `after`; sheet `work/modelfix/gb_hollowking_sheet.jpg`.

**What I saw:**
- The rest pose is unchanged from the front.
- From the side, the strips still read as thin edge-on lines. Each one now trails its own thigh in the walk and run.
- At game16 the rags read as a dark fringe at the waist, as before.
- Deaths: the strips lie on the ground with the body.

**Tests:**
- `test_hk_polish.py`: OK.
- A second `hk_polish.py` run changes no byte.

**Not checked:**
- The in-game reel, verify and Gradle (not allowed by this brief).

## Model rebuild 2026-11-13 (cape)

Kort: "the hollow kings cape looks weird that needs fixed". Shown today's cape and three options (knee cape, heel cape,
knee cape with capelet); he picked A, the knee cape, and said nothing else bothers him. Builder only
(`art/hand/gb_hollowking/build_gb_hollowking.py`); no clip was re-keyed.

Changes (everything else, bone names, parents, pivots and limb lengths, is unchanged):
- Removed `cloak_a/b/c` (5x25-27x1 panels from the collar to the ground) and `train_a/b/c` (trains lying on the floor).
- New `collar` on body at (0,-10,3.8), rot (-12,0,0), carrying a 5x2x2 rolled fold of team cloth. troop_rig hangs it in a
  new `cape` container (pivot world (0,24,3.8)), which is the bone the idle, walk, run, attack, death, fake_death and rise
  clips already key. Before this the `cape` keys did nothing (no such bone) and the cloak never moved in idle, walk or run.
- New `cloak_a` (3x13x1, flare z +4), `cloak_b` (4x15x1), `cloak_c` (3x12x1, flare z -4) under `collar`, torn hem, team
  cloth, to the back of the knee. The names are kept so the attack and hit keys on cloak_a/b/c still drive them.
- Final cape chain: torso > cape > collar > cloak_a, cloak_b, cloak_c.

Proof:
- focus_check: 44 clips, 0 FAIL (unchanged from before). WARN: secondary_life cloak_a 3.86 and 3.21 (as before), settle on cape.
- scorecard: 0 failing.
- verify fast: PASS.
- Cape clearance (work/hk_model/probe.py, every quarter tick): idle/walk/run legs +0.63, arms -0.15, sword +1.9, hem
  +7.3 px over the feet. Worst of all clips: cloak_a into right_pauldron -1.6 (attack_overhead_cleave t14.5), cloak_b into
  left_foot_gear -1.7 (hit_knockback t3), cloak_a into belt -0.85 (attack_side_cut t15). Before: legs -2.48, sword -0.64,
  hem 8.87 px through the ground.
- Open for animation: the cloak_a/b/c and cape keys were tuned for the old cape; those last 1-2 px are the cape's motion.

## Cape motion 2026-11-14

Kort's cape note, second half: the knee cape (model rebuild 2026-11-13) had no keys of its own. New
`art/anim/gb_hollowking/hk_cape.py` solves cloak_a/b/c as three damped springs hung from the collar (each panel its own
period 0.52-0.64 s and damping, so they lag the torso and each other), driven by gravity less the hinge's acceleration,
a head wind with gait speed and, in holds, a slow breath. Every quarter-tick sample is then cleared off the body: the
nearest turn that leaves the panel out of the armour, and where a plate sweeps the panel's top (no turn clears that) a
small eased slide of the hinge (position keys, at most 2.5 px). `hk_polish.py` stage `cape` (after soft, before seam)
keys it on every clip: BUILT and LATE lists, `fix_cape`. Only `cape`, `cloak_a/b/c` keys are written; `cape` is held at
rest in absolute clips and left out of additive ones. No body key, model, rig, Java or other troop touched.

- Idle and the stances (ready, brace, brace_up, wall, wall_up, shield_up, ward, hold, fallback): a 2.6 s breath sway,
  out 2.8 deg and side 1.6 deg, the three panels a third of a cycle apart; clean loop seams (four solver passes).
- Walk, run, charge_run_upper: trail back with speed (walk about +7, run +16, charge +21 deg out), the stride's beat
  twice a cycle out/in and once side to side, panels a step apart (b 0.7, c 1.3 rad later). Fallback trails forward a
  little (walking backward).
- Attacks: start from the ready stance's cape, follow the swing (peaks after the torso) and ease back to their first
  pose over the last 4 ticks (settle check). Rally is solved the same way.
- Hits, knockback, pulled, stun_out: solved on the stance they are added to, played with it from its start (as the
  checks and the HitController play them); the written key is what the stance does not give, zero at both ends.
- Deaths, fake_death, rise: gravity with the ground 1.2 px below the floor line allowed; the cape falls with the body
  and lies on it (forward, left, right) or under it (back).

Contacts (work/hk_model probe, signed px, every quarter tick; negative is inside):
- cloak_a into right_pauldron, attack_overhead_cleave: -1.60 -> arms -0.36 (t0.5, the stance's own graze).
- cloak_b into left_foot_gear, hit_knockback: -1.70 -> legs -0.42 (t9, cloak_a into right_foot_gear).
- cloak_a into belt, attack_side_cut: -0.85 -> waist -0.32 (t0, hip_l, the rest fit).
- Worst of all non-death clips: arms -0.60 (rally t0.5), legs -0.42, waist -0.32, weapon +1.45, back +1.49, hem +4.0
  over the feet (before: arms -1.60, legs -1.70, waist -0.85).

Checks: scorecard 44 clips 0 failing (4 rise exemptions as before); focus_check 44 clips 0 FAIL; unittest discover
tools/anim OK; test_hk_polish OK; a second hk_polish run is a no-op. Focus rows new on the cape: pop cloak_c 9.4
(hit_knockback t13.5) and 10.0 (hit_medium_right t7.75), WARN (limit 14). Gone: settle cape (cleave, side cut),
secondary_life cloak_a (medium_back, pulled).

Build: `python art/anim/gb_hollowking/hk_build.py` runs the whole chain (build_anims -> export -> stances -> reactions
-> charge -> hk_polish late) and checks that out/ (54 clips) and the asset (67) agree. export.py alone drops the 13
clips stances/reactions/charge add to the asset only.

Open: the two pop WARNs (a panel meeting a plate in one quarter tick); a hit added to the stance at another phase than
its start can lie up to the stance's breath (about 8 deg) further in or out; hit_pulled_legs' rags and cuisse keys moved
up to 3.75 deg because those late stages measure the whole pose, cape included.
