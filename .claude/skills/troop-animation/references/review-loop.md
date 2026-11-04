# The review loop

After every animation pass, a fresh reviewer studies the in-game contact sheet and the clip data, grades it honestly,
and lists exact fixes. The author fixes, reshoots and asks for a re-review of the changed areas. This is how the
2026-11-04 review found 18 missing attacks and every backwards elbow while all checks were green.

## The loop

1. **Pass.** The author (Opus) keys or fixes the clips, runs `anim_edit apply`, `clips.py --verify`,
   `verify.py fast`, then shoots the in-game sheet through the harness (pipeline.md section 8).
2. **Review.** Spawn a fresh reviewer subagent (Sonnet 5.5, `claude-sonnet-5-5`, medium effort, per Kort 2026-11-04; never the author's own context,
   never a Fable model) with `reviewer-prompt.md`. It reads the sheet images at full size and the clip JSON, and
   writes the report to a file the brief names, for example `work/anim-review-<family>-<date>-r<N>.md`.
3. **Triage.** The author reads the report. Every finding is fixed, or disputed with evidence (a frame, a key value).
   No finding is silently dropped. Anything that is a gameplay, timing or art-direction decision goes to Kort.
4. **Fix round.** Batch every concrete defect into one fix round. Reshoot the affected troops and moves.
5. **Re-review.** A new reviewer (fresh context) re-grades only the changed areas and the unresolved findings, with
   the previous report as input.
6. **Stop rule (WORKFLOW.md).** At most two automatic fix rounds. If any move is still below the gate after round
   two, stop: mark the work incomplete or blocked, keep the sheets and reports, list the remaining defects with grades,
   name what would unblock it (a stronger model, Kort's ruling, a rig change), and move to independent approved work.
   Never pass a broken result to satisfy the cap, and never lower a grade's bar to finish.
7. **Report to Kort** with the final sheet and the grade table. Kort accepts; agents do not.

## Rubric

Grade each troop on each move: **wind-up, impact, follow-through, idle, walk** (plus guard, carry, hit or death when
the pass touched them). Ranged troops: nock or wind-up, full draw or cast as impact, loose and recovery as
follow-through. Each grade needs its findings; a letter alone is not a review.

| Grade | Meaning | Rubric score |
|---|---|---|
| A | Ship. Reads instantly at game distance, Souls-like weight, blade correct on every key, joints clean, no clipping. Nothing a player would notice. | 9-10 |
| B | Good, with small polish items (a key 1-2 px off, a slightly soft hold). Not a ship blocker for internal assets; fix before player-facing acceptance. | 8-8.9 |
| C | Works but reads weak or wrong in one respect: floaty, no hold, unclear silhouette, minor clipping. Must be fixed. | 6-7.9 |
| D | Clearly broken in one respect: blade pointing the wrong way on a key, backward elbow or knee, visible joint gap, floating, jog walk. | 4-5.9 |
| F | Missing, substitute frames, broken in several respects, or unreadable. | below 4 |

**Gate:** player-facing animation needs every graded cell at A (9, WORKFLOW.md's player-facing threshold). Internal
assets need B+ (8.5). One D or F in any cell fails the troop regardless of the average.

### Automatic grade caps (any one applies)

| Defect | Max grade |
|---|---|
| Clip missing, NO CLIP, or a substitute pose shown | F |
| Blade or weapon points the wrong way for the move on any key | D |
| Forearm rx above 0 or shin rx below 0 in the clip data, or a visibly backward joint | D |
| Joint notch or gear floating off a bent limb, visible at sheet size | C |
| Weapon through the head, face or torso | C |
| No anticipation hold (catmull-rom through the top), or impact rises instead of sinking | C |
| Walk trailing shin folds past about 45 degrees (jog look), or hips locked | C |
| Body floating above its shadow | D |
| Back flag, pennant or standard | F (art rule) |
| Fractional box sizes or stretched texels introduced | C |
| Timing (impactTicks, length, cooldown) changed without Kort's approval | F until approved |

### What the reviewer checks per move

- **Wind-up:** cocked and loaded (elbow flexed, weapon drawn back for the move), held 150-250 ms, readable telegraph.
- **Impact:** blade or point on the target line at the zone named (head, body, legs), weight down (root +y, knees
  loaded), front foot planted, impact on the tick the sheet names.
- **Follow-through:** blade continues along its arc past the target, a short held low position, quick return, no wrist
  flip.
- **Idle:** breathing visible but not jittery, weapon held logically, grounded, no clipping, House silhouette clear.
- **Walk:** heavy plant, swing knee 20-25 degrees, visible hip sway and bob, weapon carried sensibly, no foot slide.
- **Across all:** 1 texel per pixel, no back flags, joints closed, consistent camera, same result at front, 3/4 and side.

## Report format (the reviewer writes exactly this)

```
# Animation review <family or House>, <date>, round <N>
Inputs: <sheet paths>, <clip files>, commit <hash>. Read only.

## Verdict
<PASS | FIX | ESCALATE>, one sentence. Gate: <A for player-facing | B+ internal>.

## Grades
| Troop | Wind-up | Impact | Follow-through | Idle | Walk | Overall |
|---|---|---|---|---|---|---|
| knight | D | C | B | A | C | D |

## Findings (worst first)
### F1 <short title> (grade cap: D)
- Where: clips/biped/sword_shield/sword_shield_overhead.json, right_forearm key t 0.50 (+38); sheet review-gilded.jpg col 2.
- What: <what is seen, and the number that causes it>.
- Fix: <exact target values or change>, check in <view>.

## Not checked
<what the evidence could not show, and why>

## Since last round (re-reviews only)
<each previous finding: fixed / not fixed / regressed, with evidence>
```

Honesty rules for the reviewer: grade what is on the sheet, not what the author says it does. Say "not checked" when
the evidence cannot show it. Do not round up because the work improved. Do not soften. Name every problem once, with
its fix. If the sheet itself is misleading (substitute frames, mismatched cameras, wrong clip), that is finding F1.
