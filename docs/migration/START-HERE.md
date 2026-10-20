# Start here: Graves and Glory

This private GitHub repository preserves the local Minecraft modpack project and its parallel Claude/Codex work. Migration date: October 19, 2026. The original directory name, `bannerbound`, predates the game's rename.

## Current status

- Minecraft 1.20.1, Fabric, Java 17.
- `main` starts from the existing `phase-1-solo-core` commit `dde5a56` (version 0.4.0), with organization and archive documentation added.
- `preview-0.5` contains the separate 0.5.0-preview1 integration attempt. It is a preview, not a verified finished release.
- Houses, models, world building, UI and heraldic units have separate development branches. They have NOT been automatically merged by this migration.
- `snapshot/2026-10-19/*` branches preserve uncommitted working files captured during migration. They are work in progress, not tested release candidates. Active local development can continue after capture.
- Older status documents record earlier checkpoints. Check branch code and commit history before treating an old status statement as current.

## Find your work

| What you need | Location |
| --- | --- |
| Player-facing overview | [README](../../README.md) |
| Vision and roadmap | [Vision](../VISION-AND-ROADMAP.md) |
| Decisions and your answers | [Questions and answers](../QUESTIONS-FOR-KORT.md) |
| Consolidated historical checkpoint | [Project state](../PROJECT-STATE.md) |
| Detailed implementation plans | [Plans](../superpowers/plans/) |
| Design specifications | [Specs](../superpowers/specs/) |
| Story text | [Copy](../copy/) |
| Code, assets and game data | `src/`, `sheets/` |
| Development and packaging instructions | [How to edit](../../HOW-TO-EDIT.md) |
| Work log | [Update plan](../../UPDATE_PLAN.md) |
| Design mockups and artwork tools | [Design archive](../../design-archive/) |
| Earlier Codex roadmap and audit | [October 17 archive](../archive/2026-10-17-roadmap/) |
| Original local folders and snapshots | [Worktree inventory](2026-10-19-worktrees.json) |

## Branch inventory at capture

The `archive/2026-10-19/bannerbound-runs` branch preserves the clean detached test-run checkout.

| Branch | Commit |
| --- | --- |
| `archive/2026-10-19/bannerbound-runs` | `8864616` |
| `heraldic-units` | `987186ea8a13` |
| `master` | `e78b81b2fb3e` |
| `phase-1-solo-core` | `dde5a56b73f3` |
| `phase-2-houses` | `6264f6a1e2d0` |
| `phase-2-models` | `2bf40df0d62f` |
| `phase-3-built-world` | `7f9d076ef47b` |
| `preview-0.5` | `38deb85f1e62` |
| `snapshot/2026-10-19/bannerbound-p2b` | `01623c6b8e09` |
| `snapshot/2026-10-19/bannerbound-p3` | `161b7354cc8f` |
| `snapshot/2026-10-19/bannerbound-preview` | `c9782e71d97f` |
| `snapshot/2026-10-19/bannerbound-test` | `95d92502f830` |
| `snapshot/2026-10-19/bannerbound-ui` | `40a2a9c3a6b1` |
| `test-runs` | `e3ff4f21568a` |
| `ui-plan` | `ebd3d98db968` |

## What stays local

Game installations, saved test worlds, Gradle/build caches, runtime logs, downloaded dependency binaries, credentials, Claude session internals and bulk test footage are excluded. The dependency manifests and build/package tools are included. The design archive preserves UI concepts, their generators, redesign images and map-tour stills; third-party reference screenshots are excluded. Links to external services remain links, not exported conversations.

This is a source and design migration. Existing ZIP/MRPACK/server distributions remain in the local `dist` folders; this upload does not certify or publish a playable release. Full Claude conversations are not included.

## Continuing development

Use one branch/worktree per task. Read the relevant plan and owner decisions before editing. Keep snapshots as reference and integrate deliberate changes into a new development branch; do not blindly merge every snapshot. Keep generated `Spec.java` changes tied to the design sheet. Follow HOW-TO-EDIT for tests and packaging. The existing game has not been launched or rebuilt as part of this migration.
