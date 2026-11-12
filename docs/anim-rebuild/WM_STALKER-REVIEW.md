# wm_stalker review record (2026-11-09)

The Web Stalker (Wildmoot rare cavalry beast, group F, ability `ensnare`) gets its first GeckoLib model and a full v2 animation set, following the wm_hound template.

**Rig.** `python art/anim/wm_stalker/build_rig.py` re-hangs the hand art (`art/hand/wm_stalker`, 256x128, three tiers) on a 50-bone spider skeleton:

- root > body > thorax > head > palps, fangs (tiers II and III fang sets), mandibles
- abdomen > sacs and sacs_ii
- eight legs of two bones each: femur and tibia

Every source box keeps its absolute place, so proportions and texture UVs are unchanged. The hand art's rest x and z rotations are negated going in. Output: `out/wm_stalker.geo.json`, the rig, 18 tier/colour textures, and Blockbench-compatible preview geos `out/preview/t{1,2,3}.geo.json`.

**Clips.** `python art/anim/wm_stalker/build_wm_stalker_anims.py` writes 46 clips, and `art/anim/common/export.py wm_stalker` exports them.

- Legs are solved by least-squares IK (`stalker_ik.py`) against the combined body pose.
- Each femur solves pitch, yaw and roll and each tibia solves bend (4 DOF), with soft knee-up and anti-gimbal terms.
- `--deaths`, `--fallback` and `--ensnare` re-splice only those clips into the existing output.

| Clips | Length | Notes |
|---|---|---|
| `idle_legs` / `idle_upper` | 60t loop | Abdomen breathing bob and sway, mandible and palp twitches, head flicks. |
| `walk_legs` / `walk_upper` | 6t loop | Alternating tetrapod: L1 R2 L3 R4 against R1 L2 R3 L4, duty 0.52, body low and level. |
| `run_legs` / `run_upper` | 5t loop | Faster, lower skitter (duty 0.42), with more abdomen sway. |
| stance ready / brace / wall `_legs` and `_upper` | 40t loop | Ready raises the forelegs in threat. Brace sinks wide. Wall is a high, square sentry. |
| `stance_fallback_legs` / `_upper` | 6t loop | Backward tetrapod at 0.7x walk speed, with a guard pose. |
| `stance_ward_upper` | 40t loop | Guard pose (upper only). |
| `attack_bite` (+ `_legs`) | 18t | Rears up with forelegs high, slams down, and pins with the fangs. Impact at t8 (`"0.4"`). |
| `attack_lunge` (+ `_legs`) | 18t | Coils, then leaps 5.5 px forward with the forelegs reaching. Impact at t8. |
| `ensnare` (+ `_legs`) | 18t | Rears higher than the bite with both front pairs up and wide, abdomen curled under. It holds that coil, trembling, from t5 to t7 (the spit beat). At t8 the forelegs fling forward and wide like a net, the second pair slams out sideways, and the abdomen whips up. It holds the cast with reeling tugs until t11. Impact at t8. |
| `hit_{light,medium}_{front,back,left,right}` (+ `_legs`) | light 9t, medium 16t | Additive deltas against ready. The controller grades heavy from medium. |
| `hit_knockback` (+ `_legs`) | 20t | Lifted and pitched back, legs scrabbling, then it lands. |
| `stun_legs` / `stun_upper`, `stun_out_upper` | 24t loop, 6t | Legs drawn in, body sagging, slow twitch. Snaps back to ready. |
| `death_{back,forward,left,right}` | 20t, hold on last frame | back: flips onto its back, legs curled over the belly. forward: the legs give and every claw skids out (IK-planted) as it sinks flat onto its belly, fangs and mandibles splayed. left / right: rolls onto that side, legs drawn in (right curls tighter). Each lands, bounces and settles. |

**Server timing.** The spider family's `spider_bite` and `spider_lunge` both have delay 8, and the troop's cooldown is 25.

- Each attack clip's impact marker is at t8, and the harness's attack-sync check measured it at the server's damage tick: 14 of 14 standing and 4 of 4 walking.
- Ensnare is an `onhit_status` ability with an 80-tick cooldown. The server sends no client event for it.

## Code changes (presentation only)

- `GeoTroopRenderer.TROOPS` gains `wm_stalker`.
- `AttackController` gains an ensnare hook, which is client-side and presentation only:
  - It applies to a troop whose ability row is `onhit_status` with a cooldown.
  - The first swing plays the clip named after the ability (`ensnare`) instead of the swing clip. After that, ensnare plays whenever at least the ability's cooldown has passed since the last ensnare shown.
  - This mirrors the server's on-hit status cadence. It is an approximation: a miss, a block, or a troop first seen mid-fight can put it out of step with the real slow.
  - Only the latest swing, or one still inside the clip's length, may claim the ensnare. Older swings a troop keeps when it comes back into view do not decide.
  - The rule is generic: any `onhit_status` row with a cooldown (today also tc_harpooner's `harpoon`) gets its ability clip the same way if its clip file has one.
  - There is no gameplay, server or test-tool change. A server ensnare-ready flag is a separate question for Kort.

## Fixes during authoring

- **Leg reach.** The first pass missed by 2.6 to 7 px because the rest tips sat too close to the body and the femur roll hit gimbal lock. The fixes:
  - wider rest tips
  - the 4-DOF femur solve
  - foreleg targets moved forward
  - less leg-2 spread in ensnare
- **Worst IK misses now.** Every clip is under 0.25 px except `attack_lunge`. Its hind legs miss by 1.7 px mid-leap, at full extension, while airborne.
- **Ground clearance.** In ensnare and knockback the sacs went through the floor (down to -4.4 px). `floor_clear` now lets the abdomen pitch up by up to 60°, and the body lifts more.
  - The lowest body point is now 0.30 px above the floor, and the legs reach -0.07 px.
- **death_forward stood on its legs.** The legs now splay and roll out (rx 6), and the body settles onto the floor.
- **Harness fix.** The stance check expects `stance_fallback_upper` and `_legs` while backing under the ward stance. The fallback clips were added.

## Fix round 1 (Sonnet review: fix first, narrow)

- **Floating corpses (must-fix).**
  - **Symptom:** every corpse's lowest edge sat 0.1 to 0.2 block over its shadow in game, and death_forward stood on stilt legs.
  - **Cause:** `lowest()` counted every cube in the one shared geo, including tier-only gear, so each corpse was lifted until tier III gear touched the floor. Tier I and II draw none of the crest, mandibles or spurs, so they hung 5 to 6 px over the floor (back, right) or 3 to 4.7 px (forward). The offline previews used the tier III geo, which hid it.
  - **Fix (general).** `death_clip` now:
    - lays the corpse on the parts every tier draws (`CORE`, read from the rig's stage rules);
    - lets higher-tier armour sink at most 1 px;
    - turns the head and abdomen just enough to keep tier gear (fangs, mandibles, crest, trophy) off the floor, choosing the direction once from the final pose so the keys stay continuous.
  - The builder prints each death's final lowest point per tier:

    | Death | Tier I | Tier II | Tier III |
    |---|---|---|---|
    | back | 0.00 | -0.26 (ridge plates) | -0.26 |
    | forward | 0.00 | 0.00 | 0.00 |
    | left | 0.00 | 0.00 | 0.00 (spur -1.5) |
    | right | 0.00 | 0.00 | 0.00 |
- **death_forward is now a real slump.** The body sinks onto its belly (0.3 px), and the claws skid out along the floor, IK-planted at 0.0, so the knees splay. The fangs, mandibles and palps fold forward off the dirt, so the body stays level.
- **Corpses lie over their shadow.** A roll turns about the root on the floor and carried the body 3 px sideways, so the root now recentres the body's x/z over where it stood. A fall onto the back keeps 2 px of backward travel.
- **Variety.** death_right now rolls onto its right side (rz -96) with the legs curled tight. It no longer ends on its back like death_back.
- **Ensnare readability.** See the clip table. IK misses are under 0.1 px, and the lowest body point is -0.33 px (the sacs graze the floor at the coil).
- **AttackController.** Swings that are not the latest and are older than the ensnare clip no longer decide it (see Code changes).

## In-game checks

Command: geo-knight phase-1 checks with `GRAVES_GEO_CHECK_TROOP=wm_stalker`.

- **Run r1:** stalled at resource load until the 1800 s timeout, with no `[p1]` lines. This was a launch flake; the rerun used a fresh root.
- **Run r2:** 22 lines. `stance=false` on ward/fall_back because the fallback clips were missing. Fixed above.
- **Run r3 (final clips): PASS, 22 of 22, 0 false.**
  - `attack-frames`: 34 of 34 side and three-quarter, 16 of 16 far
  - `attack-sync-stand` 14 of 14, `attack-sync-walk` 4 of 4
  - march walk and run, stance, hit and death all true
  - `knight-suite` true
- **wm_hound regression run:** 22 lines. Only `stance=false` (ward/fall_back) and the `knight-suite` failure that follows from it.
  - The cause is that wm_hound has no `stance_fallback` clips. It was built before the fallback stance check existed, so this is pre-existing and not touched here.
- **Fix round 1, run f1** (deaths and ensnare fixed, before recentring): PASS, 22 of 22, 0 false. Sync was 14 of 14 standing and 4 of 4 walking.
- **Fix round 1, run f2** (final clips): PASS, 22 of 22, 0 false.
  - `attack-frames` 34 of 34 side and three-quarter, 16 of 16 far
  - sync 13 of 13 standing and 4 of 4 walking
  - stance, hit, death and `knight-suite` all true
  - The final in-game death frames show each corpse on its shadow.
- **Video capture run v1** (`GRAVES_GEO_CAPTURE=video`): frames for idle, stances and aim-sweep.

## Open minors

- The lunge's hind legs miss by 1.7 px in the airborne phase.
- The ensnare hook is approximate (see above).
- There is no `hit_pulled`; the hound does not have one either.
- There is no `hit_block`, because the stalker has no shield.
- The Blockbench MCP was unavailable (ECONNREFUSED), so all work went through the Python builder and render_geo previews.
- Round 1 (Sonnet high) came back fix-first; this record covers its fixes. Round 2 is with the orchestrator.
- Tier III spurs dig 1.5 px into the floor in death_left, and tier II/III ridge plates sink 0.26 px in death_back.

## Port to main (2026-11-09): scorecard loop seams

On main the scorecard failed three looping clips on `loop_vel` (walk_upper 72.8 vs 51.2 deg/tick on left_leg_1_low,
run_upper 178.5 vs 121.6 on left_leg_1, stun_upper 23.7 vs 19.5 on left_leg_3_low). Fixes in the builder, no timing change:
walk and run start a third and a fifth of a stride in (`WALK_T0` 2, `RUN_T0` 1 tick) so the wrap no longer lands on a
claw's touchdown or lift-off; the run's sac bounce phase moves 0.42 -> 0.36 of a stride; the stun snap starts at 0.75
on frame 0, as the knight's does (the controller loops ticks 4..24, so tick 0 plays once). Only walk, run and stun
changed; scorecard 26 clips, 0 failing. Not yet re-rendered or re-reviewed. The old branch's AttackController ensnare
hook was not ported (gameplay code).

## Batch review 2026-11-11

Batch wm_stalker-20261111-ddde785 (27 clips), branch `anim/review-wm_stalker` cut from `anim/troop-wm_stalker`. Baseline
rebuild: no drift. Content is identical; only `hit_pulled`'s position in the file differs, because it was spliced in
last. A full build now keeps the committed clip order.

### Rows before and after (focus_check)

- **Before:** 27 clips, 20 with a FAIL, 43 FAIL rows.
- **After:** 27 clips, 11 with a FAIL, 12 FAIL rows.
- **Scorecard:** 27 clips, 0 failing.
- **What remains:**
  - **`floor` on every additive clip** (8 hits, knockback, hit_pulled; stun_out `sacs` 2.74 at t0.75). A check artefact, see below.
  - **run_upper:** `armour_clip` 2.01 (abdomen into right_leg_4_low, t3) and `jitter` 13.74 (right_leg_4, t3). Open, see below.

**Rig difference: `floor` on additive clips.** render_geo's Scene cannot layer clips: bones must be disjoint and values
replace. So focus_check and the booth play an additive hit (deltas over ready) over its own `_legs` delta, on the
rest pose of the hand art. The game never shows that pose.

`work/review/abs_check.py` (scratch) rebuilds these clips as the game plays them (ready + delta; stun_out over the stun
loop) and checks those. Every `floor` row clears. Proof frames for hit_medium_right t1.75:
`work/review/r0/hmr_asplayedbycheck/` (claws sink) against `work/review/r0/hmr_asgame/` (claws on the floor).

**As played in game, after:**
- 9 of 11 clips are clean.
- **hit_medium_back:** `armour_clip` 1.98, left_leg_1_guard into left_leg_2_spur, t3.
- **hit_medium_left:** `armour_clip` 1.95, right_leg_3_low into right_leg_4_guard, t2.

Both are leg-on-leg, and both are open.

**Checks that do not look at the spider (n/a every clip):**
- weapon_in_body, weapon_in_arm, weapon_in_prop, prop_in_body, limb_in_body
- whole_body, phases, edge_lead, point_lead, at_foe, guard_up
- roll_spin, grip_roll, wrist, elbow, knee, knee_aim, shoulder, elbow_cross, feet_cross
- head_turn (no torso bone)

What applies to the spider: floor, armour_clip, dead_hold, loop_life, secondary_life, pop, jitter, settle.
- `armour_clip` exempts a limb's own segments only for two-token names like `right_leg`, so it does not exempt the
  spider's own leg segments.
- Foot slide is not measured by focus_check.

### Changes, by clip (builder `build_wm_stalker_anims.py`, `stalker_ik.py`)

- **Leg solve (`STEADY` clips: walk, idle, stance_fallback, stance_wall, bite, lunge, ensnare, stun, 7 hits, knockback,
  hit_pulled):** `solve_leg` gains `keep` and `avoid`.
  - `keep` (0.04 per deg) holds the leg's spare degree of freedom near the previous sample. The femur flicked
    8-14 deg while the claw stayed put (jitter).
  - `avoid` keeps the fore legs out of the head and mask, and the hind legs out of the abdomen and sacs. The limit is
    each pair's rest fit + 1.4 px, measured on focus_check's lattice. This fixes the armour_clip rows: lunge t4
    forelegs' spur and guard through the head, abdomen into the hind claws, guard into sacs_ii.
  - `run` is left out: with `keep` its loop seam failed scorecard `loop_vel` (28.9 vs 21.8), so it keeps its
    committed solve, byte for byte.
- **Trophy (secondary_life "still: trophy"):**
  - Attacks, hits, knockback and pulled: a damped spring lags the body, thorax and abdomen pitch and roll. The trophy
    turns about its own base.
  - stun and stun_out: a periodic sway.
- **attack_bite, attack_lunge (settle 2.82 fangs):** they ease back to `ready_pose(0)` from t13.5 and t14, and start on
  it.
- **stance_wall (pop 14.1, t24):** the wall step's onset is eased (`damp` times a half-tick smoothstep).
- **ensnare:**
  - backcloth into ridge_plates 2.15 at t8: the abdomen rides back up to 1.2 px as it whips.
  - Abdomen pop 23 at t7.75: the curl unwinds from 7.0 and lands at 8.25 instead of 8.0. On the floor the abdomen dipped,
    then whipped 29 deg in a quarter tick.
  - Foreleg pop 15-35 at t7.75-9.25: the fling target moves from (7.5, 4, 17) to (7, 6, 15). The old target was out of
    reach (IK miss 1.05 px), so the reel's half-pixel tugs swung the femur.
- **hit_medium x4 and knockback (backcloth into ridge_plates 2.0-2.2, as played):** `pedicel_clear` slides the abdomen
  back up to 1.6 px, only as far as needed.
- **death_back:**
  - Abdomen pop 69.7 at t6.5: the head and abdomen folds ease in over 2 ticks of look-ahead (`FOLD_EASE`), instead of
    snapping when the trophy meets the floor.
  - left_leg_4 branch flip (pop 32, t5.25): `keep` only (`KEEP_ONLY`).
- **hit_pulled: decision.** The front pitches **down** toward the line; that reads right for a spider dragged by a
  harpoon. The clip had the abdomen flung up, which read as rearing. Now the abdomen trails low and bobs up as it
  settles, and the docstring says so.
- **Unchanged and byte-identical:** idle_upper, walk_upper, run (both), stance_ready, stance_brace, stance_ward,
  stance_wall_upper, stance_fallback_upper, hit_light_back, death_forward, death_left, death_right. A rebuild of
  hit_light_back, death_forward and stance_ready reproduces them exactly.

### Open

- **run_upper:** `armour_clip` 2.01 and `jitter` 13.74 at t3 (committed state).
- **hit_medium_back and hit_medium_left: leg-on-leg armour 1.98 and 1.95, as played.** I tried a neighbour-leg avoid
  term; its build crashed, and it was reverted at the fix-round cap.
- **Proposed exemption:** wm_stalker, all additive hit clips, hit_pulled, hit_knockback and stun_out_upper, check
  `floor`, values 1.1-4.9 px. Reason: the check plays additive deltas over the rest pose. As played in game, they
  clear.
- **render_geo** shows the rest pose on the final tick of play-once clips; the committed clips do this too.

### Tests

- `python -m unittest discover -s tools/anim`: 152 tests OK.
- `verify.py fast`:
  - Every step PASS except `python-tests`, which hit the step's 600 s timeout (exit 124) on a machine at 100% CPU.
  - Run untimed, 727 tests: 1 failure, `test_where` extract speed (96.9 s vs 30 s), load-related.
  - Logs: `work/review/verify_fast.log`, `work/review/python_tests_untimed.log`.
- **In-game, geo-knight through checkq, root `wm_stalker-review1b`:** 23 `[p1]` lines, 6 `=false`. The server logged
  "Can't keep up", up to 436 ticks behind.
  - march-walk-side (0 samples), march-walk-front, march-run-front
  - stance: ward lag 7. The ward clip is unchanged.
  - hit: heavy-front side frames 6/8
  - knight-suite
  - attack-frames 34/34/16, sync 14/14 and 4/4, hit-pulled and death all true.
  - Not a pass. Rerun on a quieter machine.
