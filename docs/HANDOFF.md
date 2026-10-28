# Current handoff — 2026-10-28

## Update 2026-10-28
phase-1-solo-core is fast-forwarded locally to 3031161, the M1 UI integration result (reports/m1-ui-integration-2026-10-28.md). Nothing is pushed on phase-1-solo-core. The candidate record and jar are for this source (releases/current-candidate.json); six networked players, package gate and Kort's look remain open. The builder server is live (BUILDER-SERVER.md). Animation pass 1 runs in branch anim-pass-1, banner House chips in banner-task-3. A six-player test plan is drafted, not yet in the repo. SLICE-1 scoping is answered (BACKLOG.md, DECISIONS.md on builder-server); map parts wait on the builder. The sections below are the 2026-10-25 state and are partly superseded.

Read WORKFLOW.md for policy; SETUP.md for commands. Integration checkout: bannerbound, phase-1-solo-core at da669eea15c2b647a07443925a92e9c16c3453be before optimization. Optimization tooling and durable documents are recorded in a local policy/tooling commit (see git log); pre-existing gameplay work remains separate. Refresh live facts with tools/workspace_state.py; see ../workspace-state.json from the checkout root's parent.

## Current task and source state
Kort authorized execution of optimization proposal v2 and confirmed Claude idle. Optimization owns workflow, navigation, setup, verification and Graphify tooling. Gameplay source and other lanes' partial work are preserved. No gameplay merge, push or release is authorized by this task.

The base merge is ALREADY incorporated: 8266638 is an ancestor of da669ee (six class icons and six ability HUD pictures). Do not repeat the old merge or redispatch October 21 cut-off reviews from the archived handoff. Phase 3 f172667 is also incorporated. The old growth/0.6.0 proposal was withdrawn, not approved.

Current outstanding work: War Table claims/recruit/Armoury/HUD lanes and missing tier art; balance retake and integrated player verification need candidate-specific evidence. UI-port claims at 5dcf3da and recruit at 1e79065 have reports, not an authorization to merge. HUD has dirty partial work. See BACKLOG.md and registry; no inferred owners for other branches.

## Candidate and evidence
`releases/current-candidate.json` is the authoritative candidate record. Main source is not declared a release-ready playtest candidate; incomplete UI/art integration and package gates remain. Previous preview at 552a8c6 is historical, with a real Quartermaster clipping defect; do not label it current or six-player verified.

`reports/optimization-2026-10-25.md` records this task's results. Prior merged-tree baseline is in the preserved merge-prep report under `archive/local-decisions/`; its tests are historical, not certification of current dirty source. Full runtime evidence remains under the original work directories, protected from archival.

## Next three actions
1. Read the optimization report and current verification result; resolve any tooling failure before using its tier as a gate.
2. At the next product planning checkpoint, agree the polished six-player slice and claim an explicit checkout/owned files. Audit outstanding lane changes without silently merging them.
3. Validate an identified integrated candidate through staged client/server, interactive/visual, persistence and eventual six-networked-player tests. Bots alone are insufficient.

## Constraints and questions
Keep the existing Pact wording, medieval parchment/wood/brass-frame direction, no brass fittings on table wood, and selected reference features in DECISIONS.md. New art uses 3 texels/model pixel; simple Minecraft-style faces use 2; no mass remake of accepted art. Media remains parked until maps/units are ready.

See QUESTIONS-FOR-KORT.md. Ownership of inactive worktrees remains unknown—preserve. Checkpoint bulk-file omissions remain in place. Never remove a worktree based only on ancestry.
