# Historical snapshot — not current instructions

Preserved 2026-10-25. Use docs/WORKFLOW.md and docs/HANDOFF.md for current directions.

# How to run Graves and Glory sessions (token budget rules)

Read this first, then `docs/HANDOFF.md`. Updated 2026-10-25 after the token audit. This is the canonical Graves and Glory token policy, including for existing worktrees. It overrides older token-economy memories and generic plugin defaults (including Superpowers review loops). Quality, scope and shipping restrictions remain in force.

## Models

| Role | Model | Effort |
|---|---|---|
| Coordinator and routine work | Sonnet 5.5 | medium |
| Routine implementation, testers, packaging, doc edits, small reviews, re-reviews | Sonnet 5.5 | medium; high for substantive debugging |
| Art creation and grading | Sonnet 5.5 | extra high; existing quality thresholds unchanged |
| Hard problems: combat design, model pipeline, a task stuck after 2 fix rounds, large risky reviews | Fable 5.1 | high |

- No Haiku. No Codex.
- Always set model and effort explicitly when dispatching an agent. Omitted values can inherit the caller. Never default to max effort.
- Start with Sonnet. The standing ban on spawning or resuming Opus workers remains until Kort explicitly lifts it. Fable is an escalation option only where separately allowed; do not treat this policy as lifting the Sonnet-only restriction. If a blocker needs a prohibited model, report it instead of silently switching.

## One bounded deliverable per worker

- Split a phase into coherent, independently verifiable deliverables. Each worker completes one deliverable, writes a short handoff, then stops; do not keep it alive across an entire development stream.
- Before stopping: update `docs/HANDOFF.md` with the new state, commit it, and tell Kort the phase is done.
- Start a fresh worker for the next deliverable using the handoff, relevant source paths and acceptance checks. Resume an existing worker only for a closely related fix. A coordinator also starts fresh at phase boundaries.
- Around 100-150k context tokens, assess at the next natural milestone whether a handoff or targeted compaction would remove obsolete history. Preserve decisions, exact file paths, tests, failures and remaining work. The configured 200k compaction window is a trial; do not repeatedly restart work that still needs its context.
- Long sessions are the biggest cost: every turn re-reads the whole conversation.

## Orchestrator habits

1. **Four agents at most at a time.** Keep the number of plans in flight small. When the cap hits, running agents die and their partial work is wasted.
2. **Delegate selectively.** Do small, low-risk edits directly. Delegate substantial independent tasks and noisy research with narrow briefs, concrete acceptance checks and a stop condition. Avoid duplicate workers investigating the same question.
3. **Files, not pastes.** Briefs, reports and review packages live under `.superpowers/sdd/<plan>/`. Agents return a short status, not the full report.
4. **Reviews scaled to risk.** Use one focused Sonnet review for ordinary tasks and scoped re-review of fixes. Broad final review is for large or risky changes. Respect all model restrictions; a generic plugin cannot authorize a prohibited model.
5. **Two fix rounds max.** This overrides the five-round Superpowers default. After two, record minor residual findings; stop automatic iteration on unresolved blockers and report the specific issue and proposed next step. Escalate only to an allowed model. Never waive a required test or mark a blocked deliverable complete to meet the round limit.
6. **Art grading.** Player-facing art still needs 9 on the rubric. Internal-only assets (test stills, harness renders) pass at 8.5. One self-review per art brief. Submit one 960-wide contact sheet and concrete observed defects. Allow at most two fix rounds; if the quality gate still fails, park the asset for Kort's decision rather than lowering the bar or looping on a numerical score. Art effort stays xhigh; mechanical packaging and test execution use medium.
7. **Batch messages to parked agents.** Message one only when there's work for it.
8. **Questions go to `docs/QUESTIONS-FOR-KORT.md`.** Don't stop work to ask. Kort answers as multiple choice when he asks what needs his attention.
9. **Graph before grep.** Code questions start with `graphify query "<question>"` (or `path` / `explain`) against that worktree's `graphify-out/graph.json`, then read only the lines it points to. Every brief that explores code carries this rule and the graph path. After code changes an agent runs `graphify update .` (local, free).

## Unchanged rules

Everything in `docs/HANDOFF.md` still applies: client limits, commit rules, never push or publish, never `mcp__melty__*`, send each numbered build to Kort when it's ready.
