# wm_hound review record (2026-11-07)

The Moot Hound (group F) gets its first GeckoLib model and a full v2 animation set in the Knight v2 spirit.

**Rig.** `python art/anim/wm_hound/build_rig.py` re-hangs the hand art (`art/hand/wm_hound`) on a 47-bone quadruped skeleton: root > body (rump) > chest > head > jaw, ears, fangs and head gear. Every leg has three bones (leg, shin, foot) and every source box keeps its absolute place, so the texture is unchanged. The hand art's rest pitch is negated going in, so the head and tail read as drawn.

**Clips.** `python art/anim/wm_hound/build_wm_hound_anims.py` (about 5 minutes) writes 37 clips, and `art/anim/common/export.py wm_hound` exports them. The legs are solved by numerical least-squares IK (`hound_ik.py`) against the combined pose.

| Clips | Length | Notes |
|---|---|---|
| idle / walk / run `_legs` and `_upper` | idle 60t, walk 6t, run 5t | Walk is a trot (GaitController ref .16). Run is a rotary gallop (ref .26). |
| stance ready / brace / wall `_legs` and `_upper`; `stance_ward_upper` | 40t | Ward plays over the backward walk. |
| `attack_bite`, `attack_lunge`, each with `_legs` | 16t | Impact at t8 (`"0.4"`), matching Spec hound_bite and hound_lunge. |
| `hit_{light,medium}_{front,back,right,left}`, each with `_legs` | light 9t, medium 16t | Additive. Heavy is medium x1.55. |
| `death_{back,forward,left,right}` | 20t, hold on last frame | |

The only renderer change adds `wm_hound` to `GeoTroopRenderer.TROOPS`. There is no gameplay change and no test-tool change.

- **Ground clearance.** The muzzle, fangs, collars and the tier III throat skull went through the floor in ready, brace, the attacks and idle (down to -4.6 px).
  - `head_clear` pitches the head up, then the chest, just enough to keep the head gear 0.3 px above the floor. It runs before the legs are solved.
  - `pendant` swings the throat skull back against the throat, keeping it 0.5 px above the floor.
  - Every clip's lowest point is now -0.4 px or better, and only feet and leg wraps reach it.
- **Foot slide** (render_geo probe). The walk's worst drift is 0.08 blocks, on the front feet during touchdown. The run's is 0.064.
- **Leg reach.** The worst IK misses are walk 0.12 px, wall 0.05 px, and lunge 1.0 px, which falls in the airborne phase and is harmless.
- **Round 1 fixes:**
  - **Bite** now has whole-body drive. The haunches sink and the head draws back on the wind-up. At impact the chest drives forward and down, the head thrusts out 2.4 px, and the gape holds wide from t5.2 to t7.5.
  - **Deaths** are one continuous collapse, from t3 to t10, ending in an overshoot, a 0.7 px bounce and a settle. Each direction now has its own final pose:

    | Death | Final pose |
    |---|---|
    | back | on its right side, head thrown back, legs stretched |
    | forward | half over, slumped on its chest, chin on the floor |
    | left | on its left side, curled |
    | right | 80% over onto its right side, sprawled |

  - **Hold the line** (`stance_wall`) is now a sentry: chest up, head high, ears pricked forward, tail up and stiff, a wider square stance.
  - **Medium front hit** is about 25% bigger.
- **Harness fix.** The bite was lengthened from 14 to 16 ticks by stretching its recovery. The frame check expects every tick of each attack variant up to the longest clip, so a 14-tick bite always left `attack-frames` at 29 of 30.

## Review

- **Round 1 (Sonnet high, reel `wm_hound_r1`, contact sheets): SHIP WITH MINORS, no blockers.**
  - Deaths stayed near-standing too long, rolled quickly, and had no settle.
  - The back, left and right death finals looked alike.
  - The bite was head-and-jaw only.
  - Hold the line looked close to idle.
  - Heavy and medium front hits were hard to tell apart.
  - The deaths, bite and hold-the-line items were fixed above.
- **In-game checks run 1** (round 1 clips): the client crashed at init with NoClassDefFoundError from the test-tools entrypoint (BuildMode). This was a first-launch flake; nothing was changed.
- **In-game checks run 2** (round 1 clips): 22 lines, 4 false.
  - `march-run-side` failed during lag: "Can't keep up" twice, the server running up to 113 ticks behind, and client speed min 0.181.
  - `attack-frames-side` and `attack-frames-threequarter` were 29 of 30.
  - `knight-suite` failed as a result of the above.
- **Round 2 (Sonnet high, reel `wm_hound_r2`): SHIP WITH MINORS, no blockers.** The deaths, bite and hold-the-line minors are resolved.
  - Deaths now collapse in one continuous motion, then overshoot and settle, and the finals are distinct.
  - The bite reads as a body drive.
  - Hold the line is distinct from idle, ready and brace.
- **In-game checks run 3** (round 2 clips): 22 lines, 3 false. `march-run` passed. `attack-frames` was still 29 of 30 in the side and three-quarter views, which showed it was the clip length rather than lag. The bite was lengthened to 16 ticks to fix it.
- **In-game checks run 4** (final clips): PASS, 22 of 22 checks, 0 false. The server logged "Can't keep up" 3 times, and the checks still passed. `attack-frames` was 30 of 30 side and three-quarter and 14 of 14 far. Attack sync matched the server: 14 of 14 standing, 4 of 4 walking.
- **Verdict: finished (SHIP WITH MINORS).** Round 2 was the last fix round. The bite's recovery was lengthened after review, and its strike is unchanged. Video: `wm_hound_v2.mp4` (front, side and three-quarter views).

## Open minors (polish, not blocking)

- **Hit grading:** the medium-to-heavy step is small. Peak frame-to-frame motion is light 12.5, medium 17.3 and heavy 20.8. Heavy needs more of its own (a deeper hip drop and a longer recovery), or the medium front boost should be reverted.
- **Death lead-in:** the first few ticks are near-static before the sink. The sink could start about tick 4.
- **Back death final:** the legs are stretched toward the camera, so from the side it can read as sitting up.
- **Lunge launch** moves about 35 px of screen in one tick, at t325 to t326 of reel r1, and the landing is short.
- **Run:** the front legs tuck behind the chest armor, and the knee guard can read as a floating cube mid-gallop.
- **Walk foot slide:** the front feet drift up to 0.08 blocks during touchdown.

## Batch review 2026-11-11

Branch `anim/review-hound` (from `anim/fix-singles`). This round changed only the builder; the rig, model and Java are unchanged. Working files are in `work/review/` and frames in `work/render/`.

### Focus check, before and after

Before: 9 of 22 clips had a FAIL, 17 FAIL rows. After: 8 clips, 11 FAIL rows. Scorecard: 0 fail before and after (lunge pen_limb 1.27, limit 1.391).

| Clip | Check | Before | After |
|---|---|---|---|
| attack_bite | armour_clip (chest into skullcap) | FAIL 1.99 t4.5 | pass 1.44 |
| attack_bite | settle (jaw) | FAIL 5.84 | pass 0.0 |
| attack_lunge | armour_clip (chest into skullcap) | FAIL 1.98 t3 | pass 1.46 |
| attack_lunge | settle (jaw) | FAIL 4.5 | pass 0.0 |
| attack_lunge | pop (throat_skull) | FAIL 47.4 t8.75 | WARN 12.6 front_right_shin t5.25 (was already there, hidden behind the 47) |
| death_back | pop (back_right_foot) | FAIL 43.8 t7.25 | pass 6.7 |
| bite, lunge, 4 medium hits, hit_pulled | secondary_life | FAIL 0.0 (antler_tuft) | FAIL 0.0: proposed exemption below |
| hit_medium_back | floor | FAIL 1.03 t4.25 | unchanged: a check artifact (below) |
| hit_medium_right / left | floor | FAIL 1.46 t2.5 | unchanged: a check artifact (below) |
| stance_ward_upper | floor (tooth_collar) | FAIL 1.0 t27.5 | unchanged: a booth-pairing artifact (below) |

### Changes per clip, and why

- **Hanging gear (bite, lunge, the four medium hits, hit_pulled).** The tier III throat skull and the tier II/III tooth collar now hang on a damped spring behind the head. Each swings back as the head drives and settles with its own slow sway. Both fade in over 1 tick and out over 2, so each clip still starts and ends on its stance. The throat skull still rides up the throat where the floor needs it (`pendant(keep=True)`, limited to 22 degrees a tick), which removes the 47-degree lunge pop.
- **Antler crown (antler_tuft_left/right): not keyed.** On the model these are 8-px branching bone antlers grown from the skull (hand art: "branching antler crown"). They are bone, not cloth or a pendant, so they follow the crest rule from knight round 3 and ride the head. The ears and tail were already keyed in every action clip.
- **Bite and lunge.** The ready blend now returns to the clip's first pose, so jaw and tail end where they began (settle). The gape opens from 0 and the tail starts at the ready yaw. In the bite the neck lifts and draws in during the windup (`NECK_LIFT`), and the lunge gather draws the neck out (`LUNGE_NECK`), so the chest stays clear of the skullcap. Lifting the lunge neck as well pitched the chest onto the front-leg wraps (scorecard pen_limb 1.46 over 1.391), so the lunge only draws it out. The lunge hind-foot push keeps its toe roll into the hop, removing a 37-degree back_left_shin snap at t7 that sat behind the old pop.
- **Deaths.** The leg solver no longer retries the textbook bend in deaths. In death_back the retry flipped the back right leg between two near-equal solutions at t7.25 (a 44-degree pop). death_forward moves at most 8.9 degrees (front_left_leg t7). death_left and death_right are unchanged in value; only float formatting differs.
- **Unchanged:** idle, walk, run, every stance, the light hits and every `_legs` clip of the hits are byte-identical. Every clip keeps its length, `impact` marker (0.4) and timeline.

By eye (side, front and three-quarter frames in `work/render/fin_attack_bite`, `fin_attack_lunge`):
- **Bite:** the haunches sink t4-5 while the head draws back and the jaw opens wide. The chest drives forward t7-8, the jaw shuts on t8 (impact), and the head shakes at t9.
- **Lunge:** gathers t2-5, pushes off t6, is airborne t7-8 with the jaw shutting at t8, and lands at t9.

### Rig differences that the checks do not handle (with frames)

- **The hit floor rows are a check artifact.** The focus check plays the additive hit deltas over the rest pose. The game adds them over the stance (HitController). Composed as the game plays them, the worst feet are:

  | Clip | Worst foot |
  |---|---|
  | front | 0.09 px |
  | back | 0.09 px |
  | right / left | 0.53 / 0.44 px |
  | pulled | 0.12 px |

  Frames: `work/render/chk_hml_front` (check) versus `work/render/abs_hml_front` (game) at t2.5. The game-composed hits are in `work/render/abs_after_*`.
- **The ward floor row is a pairing artifact.** `booth.wm_hound.json` pairs `stance_ward_upper` with `stance_ready_legs`. The game plays the ward with gait legs (StanceController: idle_legs or walk_legs). Over idle_legs the tooth collar's floor maximum is 0.15 px. Frames: `work/render/ward_over_ready` versus `ward_over_idle` at t27.5.

### Proposed exemptions and fixes outside this lane

- wm_hound, attack_bite / attack_lunge / hit_medium_front / hit_medium_back / hit_medium_right / hit_medium_left / hit_pulled, secondary_life, 0.0. Reason: antler_tuft_* is rigid bone antler, a crest, not hanging gear. The hanging pieces (throat skull, tooth collar, ears, tail) move.
- focus_check (shared tool): play `hit_*` clips over their stance pose, as HitController does. That removes the hound's three medium-hit floor FAILs.
- `booth.wm_hound.json`: pair `stance_ward_upper` with `idle_legs` or `walk_legs`, not `stance_ready_legs`.

### n/a rows (the check does not look at the hound)

- **Not a biped:** limb_in_body, elbow, elbow_cross, knee, knee_aim, feet_cross and shoulder. There is no joint-direction check on four legs; only the IK bounds keep the knees right.
- **No design.json variant:** phases, at_foe, edge_lead and point_lead. Bite and lunge timing is checked only by the in-game attack-sync run and by eye.
- **No weapon:** weapon_in_*, grip_roll, roll_spin, wrist, prop_in_body and guard_up. The jaw and fangs are not treated as a weapon.
- **Not run on the hound:** head_turn and whole_body (n/a on all 22 clips).
- **Partial:**
  - secondary_life looks only at the tail and antler_tuft; ears, throat skull, tooth collar and collar are never seen.
  - settle and dead_hold run only on action clips, and loop_life only on loops.
  - armour_clip is n/a on falls.

### Open items

- Lunge front_right_shin pop WARN 12.6 at t5.25 (pre-existing, the front leg folding for the push).
- Pre-existing WARNs on clips this round did not touch: idle ear pop 8.3, walk leg pop 9.0, run foot jitter 7.1, light-hit floor 0.66, hit_light_back dead_hold 3.75.
- hit_pulled armour_clip WARN 1.83 and floor WARN 0.9 are as before. Composed over the stance, its feet are at 0.12 px.
- The round 2 open minors above still stand.

### Tests

- `python -m unittest discover -s tools/anim`: 152 tests OK (`work/review/unittest.log`; 1622 s on a heavily loaded machine).
- Scorecard: 22 clips, 0 fail. Focus check: 11 FAIL rows, all explained above.
- `python tools/verify.py fast`: every step PASS (preflight, ui, skin, clips, java-compile, every Java check, build) except python-tests, which hit verify's 600 s step limit (exit 124) with about 76 Python processes from other sessions on the machine (`work/review/verify_fast.log`).
  - Run alone (`tools/parallel_tests.py test-tools/standalone`), every module passed except two timing tests (`work/review/python_tests_rerun.log`).
  - `test_checkq ... test_timeout_stops_only_its_own_tree` passed on retry.
  - `test_where ... test_extract_is_fast...` took 142 s against its 30 s limit, twice. This is load; the test does not read the hound.
- **In-game reel** `wm_hound-review1` (checkq 20261112-012617-176-bd36, geo-knight, troop wm_hound): 23 `[p1]` lines, 3 false. Server lag: "Can't keep up" 4 times, up to 520 ticks behind.
  - `attack-frames` was 30 of 30 side and three-quarter and 14 of 14 far. `attack-sync` was 20 of 20 standing and 4 of 4 walking. `attack-plays`, `footwork`, `attack-walk`, `attack-death`, `hit` (7 blows x 3 views), `hit-pulled` and `death` were all true.
  - False: `march-run-threequarter`. WALK was shown while the speed ramped from 0.316 to 0.338 under lag. run_upper and walk_upper are byte-identical to the last passing run.
  - False: `stance`.
    - Wall lag of 7 ticks under lag.
    - Every ward row: the harness wants `StanceController.target("fall_back", false, true)` with every clip assumed present, which is `stance_fallback_upper/legs`. The hound has no fallback clips, so the game correctly plays `stance_ward_upper` (shown at weight 1.00). This is a harness artifact in test-tools; the stance clips are unchanged.
  - False: `knight-suite`, as a result of the two above.
