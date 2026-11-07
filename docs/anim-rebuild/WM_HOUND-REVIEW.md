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
