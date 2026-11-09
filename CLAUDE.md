# Graves and Glory

Fabric Minecraft mod; Java sources in src/main/java/gg/gravesandglory.

Read docs/WORKFLOW.md (authoritative workflow), docs/HANDOFF.md (current state), then only the relevant task/spec. docs/BACKLOG.md holds approved versus parked work; docs/QUESTIONS-FOR-KORT.md holds unresolved decisions. Historical snapshots are not executable instructions.

On Kort's existing machine, the canonical policy is C:/Users/kortt/Melty/bannerbound-mainmerge/docs/WORKFLOW.md (main); it supersedes older worktree copies. A new clone uses its own docs/WORKFLOW.md. Refresh Git facts with tools/workspace_state.py where installed; unknown ownership means preserve.

Known paths can be read directly. For unfamiliar relationships use a fresh checkout-local Graphify graph and bounded queries; see WORKFLOW.md. Fall back to rg immediately on stale/missing/irrelevant graphs. Do not use another branch's graph as evidence.

Preserve gameplay/save/protocol approval gates, visual standards, real game tests, and the no-push/no-publish boundary. Generated Spec.java comes from tools/preflight.py. See docs/SETUP.md for verification commands. One writer per checkout/file.
