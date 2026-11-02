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

Gate 3: the hit now plays as an additive overlay over 0.2 s and no longer restarts the attack. The window is still dominated by the attack's own double hump.

## Commits

- 02b0b0a: ground the soles; carry the stride sweep.
- 66775da: turn step.
- 5f9f292: run start, plus a held check (not met).
- cde0479: hit overlay, plus a held window check.
- a574205:
  - straps spring before the ground lift (MotionClothCheck);
  - the death fade of the ground weight no longer depends on frame rate (MotionFrameCheck);
  - MotionGrade records swing lift and solo sole height.

## Verify (`python tools/verify.py fast --offline`)

FAIL. Every check passes except MotionLayersCheck "no pops", which fails at speed 1.0 (ratio 0.93 against a 0.85 bar) and at speed 1.5 (ratio 1.29). The pop is root TY, about 2 px in a 1/60 s frame. It comes from a legs hit while running at full speed: the ground lift follows the reaction's knee bend.

This failure is new since 02b0b0a. Exempting the ground lift on the stride's own pose did not clear it, so the edit was reverted. I did not loosen the check.

## Remaining risk

- Death clips still sink the soles, because the ground pass fades out on death.
- The ground lift can pop root TY during a leg-hit at a run. It needs a rate-aware lift, or a reaction that bends the knees without lowering the soles.
- Gates 3 and 6 need clip or stride retiming, not more blending.
- Crawl slip and run per-plant slide are still above the gate.
