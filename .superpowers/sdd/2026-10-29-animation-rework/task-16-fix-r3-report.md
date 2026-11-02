# Task 16 fix r3 report (engine gates 1, 2, 3, 6)

Evidence: live sword_shield grade parts under `test-tools/work/t16r3/`: g_loco and g_transitions at a574205, g_solo2 at d0404cb (the earlier g_solo at a574205 is kept as the before for deaths). Gate numbers come from the manifests via `test-tools/work/t16r3/gates.py`. The "before" numbers come from `test-tools/work/t16r2c`. contact_sheet.py ran on every part with 0 missing tiles.

| Gate | Before (r2c) | After (r3) | Result |
|---|---|---|---|
| 1 loco lowestSoleHeight (worst segment) | -0.159 | -0.013 | PASS |
| 1 solo, 27 non-death clips | not recorded | -0.013 (fall) | PASS |
| 1 solo, 20 death clips | -0.076 to -0.265 (a574205) | -0.015 to +0.004 | PASS |
| 2 swing lift, texels | not recorded | walk 3.3, run 3.0, backpedal 3.3, crawl 3.8, shuffle 3.5 | PASS |
| 2 turn step | plant slide 0.53, no step | 2.5-texel swing, plants ≤ 0.119 | PASS |
| 2 largest per-plant slide | 1.4-4.0 | 0.117-0.143; run 0.206 (0.112 when engine-planted) | FAIL, run only |
| 2 stanceSlipRatio | 0.02-0.07 | 0.05-0.07; crawl 0.169, shuffle 0.074 | FAIL, crawl |
| 3 hit-while-attacking, f26-35 | 0.94x, not monotone | 1.15x, not monotone | FAIL |
| 6 run start, s=18 window | 45, 38, 42, 77, 157, 82 | 47, 93, 31, 114, 102, 234 | FAIL |

Gate 3: the hit is now an additive overlay (0.2 s ease-in) that leaves the attack playing; the attack's own double hump still dominates the window.

## Commits

02b0b0a soles; 66775da turn step; 5f9f292 run start (held check, unmet); cde0479 hit overlay (held check); a574205 straps before ground lift, grade fields; 6a9bfa6 ground pass fades out without the ground input (the layer pops: it lifted the hips off the unsolved gait's soles); d7bbcad death fall stays grounded, and from the clip's end the lift eases under the torso's clearance so the corpse keeps its authored pose; d0404cb LifeCheck turn test drives the ground input (the step needs it since 66775da; thresholds unchanged).

## Verify (fast, offline): PASS at d0404cb

MotionLayersCheck pops fixed at the cause, not loosened. corpseGround now runs with and without the ground input (384 bodies).

## Remaining risk

Ten corpses rest with a knight boot toe 0.85-1.0 px under the ground (standalone diag, after the clip ends). The knight's boots are larger than clip_rig's shins; their end-pose legs need art keys. Items 3-5 (run plant slide, crawl slip, gates 3 and 6) are not attempted this round.
