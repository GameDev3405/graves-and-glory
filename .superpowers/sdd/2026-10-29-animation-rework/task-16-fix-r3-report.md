# Task 16 fix r3 report (engine gates 1, 2, 3, 6)

Evidence: live sword_shield grade parts at a574205 under `test-tools/work/t16r3/` (g_loco, g_transitions, g_solo). Gate numbers come from the manifests via `test-tools/work/t16r3/gates.py`. The "before" numbers come from `test-tools/work/t16r2c`. contact_sheet.py ran on all three parts with 0 missing tiles.

| Gate | Before (r2c) | After (r3) | Result |
|---|---|---|---|
| 1 loco lowestSoleHeight (worst segment) | -0.159 | -0.013 | PASS |
| 1 solo, 27 non-death clips | not recorded | -0.013 (fall) | PASS |
| 1 solo, 20 death clips | not recorded | -0.076 to -0.265 | FAIL |
| 2 swing lift, texels | not recorded | walk 3.3, run 3.0, backpedal 3.3, crawl 3.8, shuffle 3.5 | PASS |
| 2 turn step | plant slide 0.53, no step | 2.5-texel swing, plants ≤ 0.119 | PASS |
| 2 largest per-plant slide | 1.4-4.0 | 0.117-0.143; run 0.206 (0.112 when engine-planted) | FAIL, run only |
| 2 stanceSlipRatio | 0.02-0.07 | 0.05-0.07; crawl 0.169, shuffle 0.074 | FAIL, crawl |
| 3 hit-while-attacking, f26-35 | 0.94x, not monotone | 1.15x, not monotone | FAIL |
| 6 run start, s=18 window | 45, 38, 42, 77, 157, 82 | 47, 93, 31, 114, 102, 234 | FAIL |

Gate 3: the hit is now an additive overlay (0.2 s ease-in) that leaves the attack playing; the attack's own double hump still dominates the window.

## Commits

02b0b0a soles; 66775da turn step; 5f9f292 run start (held check, unmet); cde0479 hit overlay (held check); a574205 straps before ground lift, frame-exact death fade of the ground weight, grade fields.

## Verify (fast, offline): FAIL

Only MotionLayersCheck "no pops" fails: speed 1.0 (0.93 vs bar 0.85) and 1.5 (1.29). Root TY jumps about 2 px in 1/60 s when a legs hit lands at a full run; the ground lift follows the reaction's knee bend. New since 02b0b0a. I did not loosen the check.

## Remaining risk

Death clips sink (ground pass fades out on death); the lift can pop at a run; gates 3 and 6 need clip or stride retiming; crawl slip and run per-plant slide remain above the gate.
