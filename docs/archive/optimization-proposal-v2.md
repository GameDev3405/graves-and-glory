# Melty optimization proposal — revised for Kort's goals

October 25, 2026. For review before implementation. No Melty source, instructions, settings, graphs, or worktrees were changed while preparing this revision. Reference images were copied into this deliverable folder and verified with SHA-256 hashes.

This proposal supersedes conflicting policy recommendations in the earlier 18-step plan. That plan remains the detailed checklist for inventory, backup, build tooling, and archival safeguards. In particular, its permanent no-Opus rule, mandatory xhigh art effort, and request-for-input stopping behavior must not be carried forward unchanged: Kort has authorized proposing new model routing and autonomous work rules. Those changes are not yet installed.

## Agreed goals

- Make the existing $200 subscription last close to its weekly reset, compared with the current roughly two days. Do not impose daily allowances or reserve a daily interactive quota.
- Accept slower delivery to reduce consumption while preserving functional quality and visual polish.
- Support one coordinating chat working day and night through an approved backlog, with helpers when they improve the outcome efficiently.
- Ask questions before planning, ask newly discovered questions after planning and before implementation, and provide completion reports with remaining questions.
- During approved unattended work, decide reversible implementation details, log assumptions, park significant unresolved design choices, and continue independent approved tasks. Do not manufacture extra work just to remain busy.
- Preserve actual gameplay tests and visual inspection. Overnight Minecraft and local-server tests are allowed unless Kort says he is gaming; then continue work that does not require launching Minecraft. Do not terminate his game.
- Aim for a complete, expandable 1.0.0: campaign, cooperative and competitive multiplayer, parties, strong bots, distinctive maps, units and abilities, polished interfaces, and a walkable lobby with NPCs, shops, and upgrade stations.
- First validate a smaller, heavily polished playtest for **six simultaneous players**, with self-hosted dedicated-server support. Exact team splits and content quantities remain design choices, not inferred requirements.

## Visual references selected

I inspected ten screenshots across recruitment, faction variants, unit lineup, chapter rewards, contracts, troop details, and the in-game HUD. These selections are proposed standards, not a claim that every screen in the folder has been reviewed or accepted.

### Primary UI reference: recruitment

![Recruitment visual reference](C:/Users/kortt/Documents/Codex/2026-10-25/wha/outputs/visual-references/01-recruitment-ui.png)

Preserve the dark wood setting, warm parchment, brass/gold frames, stitched faction banners, wax-seal accents, deliberate three-column layout, and framed unit portraits. The main actions have distinct places and the materials form a coherent medieval theme.

Do not treat all details as finished: portraits need useful framing, typography must remain readable, and smaller windows need their own layout checks. Locked silhouettes are a state to verify, not automatically missing artwork.

### Unit-art reference: veteran lineup

![Veteran unit reference](C:/Users/kortt/Documents/Codex/2026-10-25/wha/outputs/visual-references/02-veteran-units.png)

Preserve recognizable roles, distinct silhouettes, coherent faction colors, modeled equipment, and visible rank detail. Use this as a reference for consistency and detail; it does not certify animation, readability in combat, other factions, or gameplay balance.

### Supporting evidence

- `visual-references/03-recruitment-result.png`: recruitment-result state and consistent decorative treatment.
- `visual-references/04-small-window-check.png`: small-window layout regression reference; tiny text is a concern, not a target to copy.
- `visual-references/05-hud-review.png`: useful existing parchment elements, but vanilla hearts/armor/hotbar and custom elements need a coherent readability review. This is a review baseline, not the final HUD standard.
- Chapter/contracts screens inspected have consistent materials but small text and extensive empty space. Use their material language selectively, not their exact scale as the universal template.
- The older dark troop-detail panel is useful for its information coverage; its appearance differs from the selected parchment direction.

The source paths and hashes are in `visual-references/reference-manifest.json`. Once approved, implementation should preserve references in a durable project location before any source worktree is archived. These screenshots do not establish a complete map/lobby design standard; obtain a focused design decision when that milestone is planned.

## Step-by-step implementation proposal

### 1. Preserve and establish the current working state

Refresh all worktree states, active writers, unique commits, dirty/untracked files, and important ignored assets. Verify recoverable checkpoints, including a sample restore, before cleanup. Mark unclear ownership as preserve. Keep concurrent Claude work from colliding with optimization edits.

**Acceptance:** every active checkout has a purpose/owner; unfinished and ignored work has a recovery path. No deletion based only on branch ancestry.

### 2. Reconcile the instructions and current handoff

Create one authoritative modpack workflow. Keep short entry-point instructions and a concise current handoff; archive historical prose without losing decisions. Update active worktree pointers and stale memory summaries deliberately. Replace unlimited-agent, blanket maximum-effort, and incompatible model rules with the approved policy below. Do not edit installed plugin caches as the durable solution.

**Acceptance:** a fresh session identifies the real current task, checkout, constraints, and candidate without reading the entire project history. Old “next actions” cannot silently restart finished work.

### 3. Organize navigation and durable records

Add a small workspace registry with Git-derived facts and manually maintained task ownership. Separate active backlog, current questions, accepted decisions, historical task reports, and bulk run evidence. Use existing documents where possible rather than building a second management system.

Each completed task gets a short record: outcome, exact source/artifact identity, changed areas, tests, evidence locations, unresolved defects, and questions. Preserve detailed logs on disk; only load relevant excerpts into Claude.

**Acceptance:** workers can locate the right information without directory-wide exploration, and a clean clone retains essential decisions.

### 4. Repair Graphify freshness and consistency

The installed version is 0.9.93. The main graph records commit `c18e7e2`, while the reviewed checkout is `da669ee`; four manifest inputs no longer exist. Several worktrees have newer graphs but lack the main checkout's local hook file. Recorded commands show actual use, but do not establish success or savings.

1. Inspect installed command behavior, hook resolution, exclusions, and update failures before changing configuration. Do not automatically upgrade a working dependency.
2. Refresh the main graph using confirmed local code-only behavior with no semantic model pass. Validate sample results against current source.
3. Give each active worker an explicit checkout and graph path. Never treat another branch's graph as current evidence for its branch.
4. Add a cheap freshness check against source changes, including dirty inputs. A graph build commit alone cannot detect uncommitted changes.
5. Refresh after meaningful source batches and integration changes, not after every small edit or screenshot.
6. Prefer bounded queries for unfamiliar relationships. Read known file locations directly when that is cheaper. Fall back promptly if the graph is stale, irrelevant, or fails.
7. Inspect actual indexed content before expanding exclusions. Keep build outputs, test worlds, archives and unrelated assets from polluting navigation where applicable.
8. Keep semantic indexing of extensive docs/images off until a small representative experiment demonstrates value and is included in an approved plan.

**Acceptance:** sample tasks retrieve correct current paths with compact output; hooks work in the actual Claude environment; failures are visible and do not cause query loops. No promise of a specific token-saving percentage.

### 5. Use task-based model routing and bounded workers

Proposed default: Sonnet for coordination and normal implementation; a cheaper available model for narrow mechanical tasks with clear checks; stronger models/reasoning for difficult diagnosis, architecture, or an important unresolved quality problem. Verify account/model availability during implementation rather than encoding speculative model IDs.

Use ordinary effort first where adequate. Art still requires strong visual evidence; mandatory maximum reasoning on every art action is not itself a quality guarantee. Escalate based on a concrete failure or difficult decision. Model changes never remove acceptance tests.

Start with one coordinator and at most two helpers for genuinely independent substantial tasks; this is a trial default, not a concurrency target. Use short briefs with outcome, owned files, required references, checks, stop condition, and return format. Avoid passing entire histories or giant plans. Retire workers after bounded deliverables; resume only for closely related fixes.

**Acceptance:** fewer large worker histories and duplicated investigations, with no increase in defects or rework. Adjust concurrency from observed results.

### 6. Bound reviews while preserving quality

Run the task's functional checks and inspect meaningful visual changes. Give concrete findings: clipping, unreadable labels, failed persistence, missing feedback, incorrect state, or broken controls. Avoid repeated subjective scoring as the sole reason for another agent round.

Use up to two automatic correction rounds as the initial default. If material issues remain, mark the task blocked/incomplete, save the evidence and next action, and move to other approved backlog work overnight. A bounded review is not permission to accept a broken result. Broader integration tests occur at milestones or when changes invalidate earlier results.

**Acceptance:** reports distinguish implemented, tested, visually inspected, and accepted. Required tests stay intact; redundant full-branch re-review is reduced.

### 7. Give the coordinator a sustainable unattended loop

Select the next approved unblocked task, issue a bounded brief or implement directly, verify, summarize, and update the backlog. Keep the working context focused on current tasks and a short current-state record. Evaluate the existing 200k compaction trial against observed behavior; do not assume the setting works or enforce a restart mid-investigation.

Proceed until usage is exhausted or all approved tasks are complete/blocked. No daily ration, idle polling, speculative scope expansion, or repeated retries against an unchanged failure. On available usage, resume from the durable handoff through the supported execution environment; do not promise that a currently running chat can automatically restart after a limit. Scheduling or resume infrastructure is a separate implementation choice.

While Kort is available, use the requested planning/question checkpoints. Overnight, questions affecting gameplay, art direction, saves, or release scope are logged and their dependent work parked. Completion reports include questions and evidence, not just “done.”

**Acceptance:** unattended work makes bounded progress and leaves a readable morning report without requiring live answers for every minor decision.

### 8. Make verification cheap to invoke and reliable

Create the reproducible setup and fast/game/release verification entry points described in the original plan. Keep source fingerprints, exit codes, full saved logs, and concise failure excerpts. Do not launch the game from fast mode. Retain migration, persistence, networking, and package checks where relevant.

Visual checks compare selected reference features at normal and small window sizes, covering hover/focus/selected/disabled/error states as applicable. Screenshots demonstrate appearance; interactive tests demonstrate controls, transitions, timing and feel. Preserve both.

**Acceptance:** one documented command runs each tier, results refer to the tested source, and repeated diagnosis of routine setup problems is minimized.

### 9. Define and validate the polished six-player slice

Propose a focused initial slice: one coherent lobby-to-match-to-rewards-and-return loop, a short campaign segment, a polished competitive scenario, a manageable subset of distinctive units/abilities, and the required UI. Exact map/unit counts and team splits will be decided in that milestone's planning checkpoint. This does not remove the broader 1.0.0 roadmap.

Verify party formation, six concurrent players, server-authoritative state, cooperative and competitive flows, join/leave/reconnect, rewards, persistence across restart, and documented self-hosted dedicated-server setup. Exercise bots and representative combat load; agree on measurable performance and bot-quality targets from an initial baseline.

Use staged client/server tests before the full six-player validation. Six local clients are not assumed feasible on this PC; if hardware cannot represent the target faithfully, record the gap and arrange an external/multi-machine playtest. Bots alone do not certify six networked clients. Keep screenshot evidence tied to the actual candidate.

**Acceptance:** a precisely identified build supports the agreed slice; failures are explicit. Obtain player feedback before scaling content. Maintain sensible subsystem boundaries and versioned persistence/protocol decisions for later expansion without a speculative rewrite.

### 10. Archive clutter and measure actual efficiency

After preservation and ownership checks, archive inactive worktrees and superseded run outputs. Retain accepted visual references, unresolved-failure evidence, release artifacts, and restoration manifests. Verify extraction before removing originals and repair links. Less disk clutter helps navigation; disk savings are not themselves token savings.

Measure the next five comparable deliverables and the next full usage cycle: accepted results, worker/model usage, context size, repeated reads, review rounds, defects/rework, and when the weekly limit is reached. Deduplicate usage records and distinguish API-equivalent estimates from subscription billing. Do not build a reporting bureaucracy or daily allowance system.

**Acceptance:** more verified work per allowance without worse quality. If efficient continuous operation still consumes the allowance early, state that honestly and discuss the remaining throughput/model tradeoff rather than promising a folder fix can guarantee seven days.

## Execution and review gate

Recommended implementation order: steps 1–4 together, then 5–8, then archive eligible clutter and measure. Step 9 is the subsequent product milestone to plan in detail; workspace optimization does not silently authorize implementing the entire game roadmap. Incremental architecture improvements happen alongside relevant tasks, not before all productive work.

Before implementation, Kort reviews this proposal, including the model-policy replacement, worker defaults, visual selections, and six-player target. Ask any newly discovered material questions after that review; avoid re-asking settled preferences. Publishing, external messages, purchases, and unrelated irreversible actions are not authorized by this proposal.

The only changes made for this deliverable are this proposal and verified reference copies in the Codex outputs folder. Implementation requires the user's next approval.
