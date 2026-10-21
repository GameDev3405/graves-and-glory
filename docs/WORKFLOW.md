# How to run Graves and Glory sessions (token budget rules)

Read this first, then `docs/HANDOFF.md`. These rules come from Kort (2026-10-21) after the weekly cap ran out too fast. They override older token-economy notes where they conflict.

## Models

| Role | Model | Effort |
|---|---|---|
| Orchestrator (this session) | Opus 5.5 | extra high |
| Implementation, testers, packaging, doc edits, small reviews, re-reviews | Sonnet 5.5 | medium (high for tricky integration) |
| Hard problems: combat design, model pipeline, a task stuck after 2 fix rounds, large risky reviews | Fable 5.1 | high |

- No Haiku. No Codex.
- Always set the model explicitly when dispatching an agent. An omitted model inherits Opus.
- Start with Sonnet or Opus. Escalate to Fable only when stuck, never by default.

## One session per phase

- Each session finishes one phase (or one plan), then stops.
- Before stopping: update `docs/HANDOFF.md` with the new state, commit it, and tell Kort the phase is done.
- Kort opens a fresh session for the next phase. The handoff carries the state, not chat history.
- Long sessions are the biggest cost: every turn re-reads the whole conversation.

## Orchestrator habits

1. **Four agents at most at a time.** Keep the number of plans in flight small. When the cap hits, running agents die and their partial work is wasted.
2. **Delegate, don't do.** The orchestrator routes, reads reports and rules. It doesn't write code or read big diffs itself.
3. **Files, not pastes.** Briefs, reports and review packages live under `.superpowers/sdd/<plan>/`. Agents return a short status, not the full report.
4. **Reviews scaled to risk.** Sonnet reviews ordinary tasks. Opus or Fable only for large or risky diffs.
5. **Two fix rounds max.** After two, park leftover minor findings in the ledger with a ruling and move on. Escalate to Fable only for an Important finding still open.
6. **Art grading.** Player-facing art still needs 9 on the rubric. Internal-only assets (test stills, harness renders) pass at 8.5. One self-review per art brief.
7. **Batch messages to parked agents.** Message one only when there's work for it.
8. **Questions go to `docs/QUESTIONS-FOR-KORT.md`.** Don't stop work to ask. Kort answers as multiple choice when he asks what needs his attention.

## Unchanged rules

Everything in `docs/HANDOFF.md` still applies: client limits, commit rules, never push or publish, never `mcp__melty__*`, send each numbered build to Kort when it's ready.
