# Worker procedures for the three most common task types

These procedures cover the three most common worker tasks. Counted over 1,091 dispatches up to 2026-11-09:
- code review: 187;
- implementing one plan task: 155;
- animation visual review: 126.

Each procedure gives:
- the brief to fill in;
- the mechanical checks the worker must run;
- a fixed report format.

With all three, Sonnet 5.5 high does the work. Opus is reserved for the escalations named below.

Policy wins over this page: docs/WORKFLOW.md. Never use a Fable model.

These procedures have not been measured against Opus yet; see "Measuring" at the end.

## 1. Code review (Sonnet 5.5 high, read only)

**Brief:** fill every `<...>`.
- Checkout `<path>`. Diff `git diff <base>...<branch>` (`<n>` commits).
- What it is: `<two sentences>`.
- The approved brief or spec: `<path>`.
- Budget: under `<80k>` tokens. Read the diff, then open only the files it touches.

**Mechanical checks.** Run these first and quote their results:
1. `git diff --stat <base>...<branch>`. Flag any file outside the brief's scope, and any file another lead owns (see `status/*.md`).
2. Run the test files the diff adds or changes, and give the count and time.
3. `git diff <base>...<branch> -- src/main/java/gg/gravesandglory/Spec.java sheets/preflight.json`. A change here is allowed only as a separate regenerate commit (docs/NEW-UNIT.md, Hard rules).
4. `git grep -n -i "fable"` in the diff, plus `git push` and `--force`. Flag any new use.

**Then judge.**
- Correctness against the brief: every new branch, exit code and error path has a test, or say which one doesn't.
- Safety rules: never kill a process the code didn't start; no writes outside the lane; no push.
- Docs match the code.
- Don't restyle working code. Don't repeat what the tests already prove.

**Report.** 250 words or fewer, in exactly this shape:

```
Verdict: <no blockers | N blockers>. Checks: <tests n passed in s>; <scope ok | files>; <Spec ok | note>.
BLOCKER
- <file:line>: <defect>. Fails when <concrete input or state>. Fix: <one line>.
SHOULD-FIX
- ...
NIT
- ...
```

A finding without file:line and a failure case is not reported.

**Severity:**
- BLOCKER: wrong behaviour, data loss, a safety-rule break, or a failing check.
- SHOULD-FIX: a real gap that won't bite today.
- NIT: everything else.

**Escalate** to Opus 5.5 high only if a blocker's cause is unclear after the review, or two reviewers contradict each other.

## 2. Implementing one plan task (Sonnet 5.5 high; Opus 5.5 medium only when the plan marks the task as design)

**Brief:**
- The task text, copied from the plan. Don't make the worker read the whole plan.
- The files to touch.
- The checkout and branch.
- The acceptance tests.
- The budget, default under 150k.

**Steps:**
1. Read only the named files and the tests next to them. Use `python tools/where.py <id>` for troop, asset and sheet ids, and rg for code.
2. Write the failing test first, run it, and see it fail for the expected reason.
3. Implement until it passes. Run the test file, then start `python tools/verify.py fast` in the background (it takes about 2 minutes).
4. Make one commit, ending with the repo's Co-Authored-By line. Don't land and don't push.

**Stop rules.** Report instead of continuing if:
- the same failure survives two fix attempts;
- the task needs a file outside the named set;
- the budget would be exceeded;
- the brief conflicts with the code.

**Report.** 150 words or fewer, in exactly this shape:

```
Commit <sha> on <branch>. Tests: <file> <n> passed in <s>; verify fast: <pass | fail: first failing lane>.
Done: <one line per acceptance item>.
Deviations: <none | what and why>.
Unsure: <none | items>.
```

## 3. Animation visual review (Sonnet 5.5 high)

Use `.claude/skills/troop-animation/references/reviewer-prompt.md` as the brief. Fill every `<...>`, including the scorecard JSON path and the contact-sheet renders.

Its rubric, grade caps and report format are in `references/review-loop.md`. The animation lead owns those files: propose changes there through the lead, don't edit them.

**Mechanical checks, before any judging:**
- The scorecard JSON exists and is newer than the animation json.
- Every clip in the batch has front, side and three-quarter renders.
- Every failing scorecard metric is listed, with clip, value and limit, and caps that clip group as the rubric says.

**Report:** the review-loop.md format, written to the report path. The reply is 200 words or fewer: the verdict, the failing groups, and the report path.

**Escalate** to Opus 5.5 high only when two Sonnet reviews of the same batch disagree on a group's grade. Kort's booth verdict always overrides both.

## Measuring

The planned blind comparison (Sonnet high re-runs of earlier Opus results, with a blind Sonnet-high grader) does not fit a small budget. A fresh agent costs about 57k tokens before it reads anything (lookup trial, 2026-11-10), so one measured pair costs about 150–200k per task type. The only recent Opus code-review baseline is a 182 KB branch diff.

Measure in normal work instead, at no extra cost. For two weeks, the lead records one line per use of these procedures in `status/procedure-log.md`:

```
<date> <type> <model> <tokens> <findings or result> <accepted by lead: y/n> <later bug traced to a miss: y/n>
```

Revisit the routing if:
- Sonnet's accepted-finding rate drops clearly below the old Opus reviews;
- or a missed blocker reaches main.
