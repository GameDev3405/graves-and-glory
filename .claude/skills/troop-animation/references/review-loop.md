# The review loop

After every animation pass, a fresh reviewer studies the renders, the scorecard and the harness evidence, grades
honestly and lists exact fixes. The author fixes, re-renders and asks for a re-review of the changed areas. Only a
passed review goes to Kort's review booth.

## The loop

1. **Pass.** The author edits the builder, builds, exports, runs the scorecard (exit 0) and renders the changed
   clips: front, side, three-quarter, near and `game16` (pipeline.md sections 4-5).
2. **Review.** Spawn a fresh reviewer subagent, Sonnet 5.5 at high effort (WORKFLOW.md; never the author's context,
   never a Fable model), with `reviewer-prompt.md`. It writes its report to the path the brief names, for example
   `work/anim-review-<troop>-<date>-r<N>.md`.
3. **Triage.** Every finding is fixed, or disputed with evidence (a frame, a probe number). Nothing is silently
   dropped. Timing, gameplay and art-direction questions go to Kort.
4. **Fix round.** Batch every concrete defect into one round. Rebuild, re-export, re-score, re-render.
5. **Re-review.** A new reviewer re-grades only the changed areas and the open findings, with the previous report.
6. **Stop rule.** At most two review fix rounds. Booth fix rounds (after Kort rejects clips) are counted separately,
   two at most, per `docs/anim-rebuild/REVIEW-BOOTH.md`. If anything is still below the gate, stop: record the remaining minors in
   `docs/anim-rebuild/<TROOP>-REVIEW.md`, escalate blockers with evidence and what would unblock them (Opus high,
   Kort's ruling, a rig change), and move to independent work. Never lower the bar to finish.
7. **Harness, then booth.** Run the geo-knight suite (pipeline.md section 6), commit, and submit the booth batch.
   Kort accepts; agents do not.

## Rubric

Grade each clip group the pass touched: **guard/idle, gait (walk, run), stances, each attack (wind-up, contact,
follow-through), block, hits, deaths.** Ranged: nock or wind-up, full draw or cast as contact, loose and recovery as
follow-through. Each grade needs findings; a letter alone is not a review.

| Grade | Meaning |
|---|---|
| A | Ship. Reads at 16 blocks, whole-body weight per STYLE.md, weapon correct every frame, no clipping. |
| B | Good, small polish items (a key slightly soft, a minor overlap in one view). |
| C | Works but reads weak or wrong in one respect: floaty, no held wind-up, unclear silhouette, minor clipping. |
| D | Clearly broken in one respect: weapon pointing the wrong way, hyperextended joint, floating, foot skate, jog walk. |
| F | Missing, wrong clip shown, broken in several respects, or unreadable. |

**Gate:** player-facing animation needs A in every graded cell. One D or F fails the troop regardless of average.

**Verdicts:** SHIP = every cell A. SHIP WITH MINORS = every cell A or B, with each B item recorded under open minors
in `docs/anim-rebuild/<TROOP>-REVIEW.md`. FIX = any C, D or F in round 1 or 2. ESCALATE = any cell still below B after
round 2, or a finding that needs Kort.

### Automatic grade caps

| Defect | Max grade |
|---|---|
| Clip missing or a different clip shown | F |
| Any scorecard metric failing | C (and the booth refuses it) |
| Weapon points the wrong way for the move on any frame | D |
| Hyperextended elbow or knee (scorecard `elbow_bend`/`knee_bend` below 0, or visible) | D |
| Weapon through head or torso, or tip below the floor | C |
| No held wind-up, or contact rises instead of sinking | C |
| Body floats above or sinks below the ground | D |
| Stances indistinguishable at 16 blocks | C |
| Back flag, pennant or standard | F |
| Impact tick, clip length or cooldown changed without Kort | F until approved |

## Report format (the reviewer writes exactly this)

```
# Animation review <troop>, <date>, round <N>
Inputs: <render folders>, <scorecard json>, <harness root or none>, commit <hash>. Read only.

## Verdict
<SHIP | SHIP WITH MINORS | FIX | ESCALATE>, one sentence.

## Grades
| Clip group | Grade | Worst finding |
|---|---|---|
| attack_overhead | C | F2 |

## Findings (worst first)
### F1 <short title> (grade cap: D)
- Where: <render folder>/<view>, tick <t>; clip <name>, bone <bone>.
- What: <what is seen, with the number that causes it>.
- Fix: <exact change in the builder or target value>, check in <view>.

## Not checked
<what the evidence could not show, and why>

## Since last round (re-reviews only)
<each previous finding: fixed / not fixed / regressed, with evidence>
```

Honesty rules: grade what the frames show, not what the author says. Say "not checked" when the evidence cannot show
it. Do not round up because the work improved. Name every problem once, with its fix. Misleading evidence (wrong
clip, mismatched cameras, the wrong troop in frame) is finding F1.
