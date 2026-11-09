# Workspace cleanup (Kort grilling, 2026-11-09)

Scope: Graves and Glory only (`C:/Users/kortt/Melty`). `flop-formation`, `minecraft`, `stickman`, `hyperframes`, `schematica-trial`, `minecraft-builds`, `Documents/gg-hx` and Prism instances are out of scope. Measured total: about 111 GB.

## Rules Kort set

- Graves and Glory only.
- Rebuildable files (`build/`, `.gradle/`, `run/`, `__pycache__`, logs) are deleted without listing, except in worktrees in use and in the live servers.
- Old worktree folders are deleted as they are; their uncommitted edits are not snapshotted (Kort: "Delete as-is").
- Every removed branch gets an `archive/cleanup-2026-11/<branch>` tag first, then the branch is deleted. A fresh `all-refs` bundle goes in `backups/cleanup-2026-11-09/` before any branch is deleted.
- Prune now. The reorganize (folder moves) waits until the animation rebuild merges and nothing runs in the folders.
- Repo docs are tidied too, on a branch, merged after Kort's yes. Moving a doc never changes its content.

## Never touched

- Running and in-use: `bannerbound/.claude/worktrees/anim-*` (the animation rebuild's lanes), `gg-build-server` (home builder server), the camp server, Kort's client.
- Kept: `bannerbound-mainmerge` (main; Kort's uncommitted files stay), `bannerbound` (base checkout, holds `.git`), `bannerbound-oldhold`, `bb-qa-fix-ingame`, `bannerbound-rc051` (0.5.1 release candidate, Kort's pick), `bb-builder-release` (live builder deploy artifacts), `bannerbound-anim-edit` (one tooling commit, merge any time), `bannerbound-reel` and `bannerbound-mainmerge/work/reel` (Kort: keep the reel media), `backups/`, `anim-refs/`, `reference/`, `lobby-reference/`, `weapon-art-review/`, `equipment-art-handoff/`, `unit-placeholders/`, `gg-units/`, `audio-demo/`, `Documents/gg-hx`.

## Delete (approved groups)

| Group | Folders | About |
|---|---|---|
| 1 Finished, in main | `bannerbound-audio`, `-audio-hooks`, `-audiolayers`, `-auditfix`, `-autosave`, `-bbmodel`, `-excal`, `-flaky`, `-gatefix`, `-housearmour`, `-polish`, `-qarules`, `-gate-prog`, `-p2`, `-p2b`, `-p3`, `-fresh-combat`, `-t5base`, `-test`, `-method`, `bb-indium-fix`, `bb-mapforge-fix`, `bb-port-audit`, `bb-prop-art`, `builder-autoupdate`, `prop-kit-systems`, `bb-docs-plan-121`, `bb-docs-plan-network`, `bb-docs-refresh` | 13 GB |
| 2 Broken copies (no git link) | `bannerbound-audio-r2`, `-audio-picks`, `-diag`, `-modegate`, `-qa1104`, `-tidecallers`, `-tidehue`, `-wildmoot`, `-wildmoot2`, `-pre-rename`, `bb-fix-client-state`, `bb-docs-player-combat-plan`, `bb-docs-netops` | 3.7 GB |
| 3 Superseded animation | `bannerbound-animredo` and its 8 `-g-*`, `-gait`, `-rig`, `-tools` siblings, `-anim-rework`, `-anim`, `-spike-freebuff` | 6.4 GB |
| 4 Old drafts and experiments | `bannerbound-art-opus`, `-art-sonnet`, `-models`, `-units`, `-ironclad-base`, `-proportions`, `-ui-hud`, `-ui-p2`, `-preview`, `-briefings`, `-escort`, `-survival`, `-coord`, `-serverkit`, `-siegehook`, `-modes`, `-review`, `-house-audit`, `-skilltest`, `-runs`, `-shoot` | 7.9 GB |
| 5 Old agent worktrees | `bannerbound/.claude/worktrees/agent-*` (7), `audit-measure-*`, `server-kit-*`, plus the missing `agent-a793919e…` entry (`git worktree prune`) | 3 GB |
| 6 Media (all but the reel) | `bannerbound-media2` (worktree), `bannerbound/work/media`, `post-media`, `media-new` | 27 GB |
| 7 Run data and scratch | `bannerbound/work/p1`, `title-slowmo`, `title-demos`, `sb`, `optimization`, `join-1104-scratch`, loose root files (`*.log`, `*.out`, `rc2smoke-p1.sh`, `inv2.py`, `anim-inventory.tmp.txt`) | 5.5 GB |
| 8 Throwaways | `bb-forgewall-fire`, `prop-kit-spike`, `bannerbound-lobby`, `bannerbound-rc2smoke` | 2.5 GB |
| Junk | `build/`, `.gradle/`, `run/`, `__pycache__` in every kept, idle worktree | about 5 GB |

Expected result: about 70 to 75 GB freed, 111 GB down to about 40 GB, most of it the in-use animation lanes, main and the base checkout.

Branches: each deleted worktree's branch is tagged and deleted. Branches without a worktree that are ancestors of `main`, or listed as superseded (`art/anim-redo*`, `work/anim-rework`, `work/anim-pass-1`, `spike/freebuff-anim`), are tagged and deleted too. `main`, active `anim/*`, `feat/oldhold-wiring`, `qa/*`, `release/*`, `work/anim-edit`, `prep/*` and `docs/*` still open stay.

## Later: reorganize (after the animation rebuild merges)

```
Melty/
  graves-and-glory/
    main/                 (bannerbound-mainmerge, moved with `git worktree move`)
    repo/                 (bannerbound base checkout)
    worktrees/<task>/     (every active worktree)
    servers/              (gg-build-server, kinetic kit)
    reference/            (anim-refs, lobby-reference, weapon/equipment reviews, unit-placeholders, gg-units, combat reel)
    media/                (reel media)
    backups/
  flop-formation/, stickman/, hyperframes/, ... (unchanged)
```

Moving means updating every path that names the old folders: `CLAUDE.md`, `START-HERE.md`, `WORKFLOW.md`, `workspace-state.json`, the memory files, the Prism pre-launch command (it uses a URL, not a path) and the builder-server scripts (`C:\GGBuild` is outside Melty and is unaffected).

## Docs tidy (branch `docs/cleanup-1109`)

- Superseded plans, specs and reports move to `docs/archive/<year-month>/`, with a one-line index of what moved.
- New `START-HERE.md` and `HANDOFF.md`, and `workspace-state.json` regenerated after the prune.
- Merged after Kort's yes.

## Verification

- `git worktree list` has no missing entries; `git fsck --connectivity-only` passes; every deleted branch has its tag.
- `verify.py fast` passes in `bannerbound-mainmerge` afterwards.
- Before and after sizes are recorded here.

## Prune results (2026-11-09)

- Backup: `backups/cleanup-2026-11-09/all-refs.bundle` (361 MB, verified) and `refs-before.txt` (303 refs).
- Removed 127 folders (groups 1 to 8, including 29 orphan `agent-*` folders with no git link), loose root logs and scratch files, and `build/`, `.gradle/`, `run/`, `__pycache__` in the idle kept worktrees. Main's build files were left alone.
- Free space on C: went from 172.5 GB to 248.2 GB: about 75.7 GB freed.
- Worktrees: 107 down to 30 (main, base checkout, `gg-hx`, the 22 animation-rebuild lanes, anim-edit, oldhold, rc051, reel, builder-release, qa-fix-ingame).
- Branches: 223 deleted, each first tagged `archive/cleanup-2026-11/<branch>` (232 tags including detached heads). 44 branches remain.
- `git fsck --connectivity-only` passes; `git worktree list` has no prunable entries. `verify.py fast --offline` in main: 547 of 549 pass. The 2 failures come from Kort's uncommitted edits in main (deleted `music_calm_1.ogg`, changed `maps/sample/design.json`), not from the prune.
- Left on purpose (not merged and not in an approved group): `anim/attack-clips`, `anim/joints-on-main`, `art/gilded-proportions`, `claude/audit-measure-5b5fc8`, `claude/ironclad-art-a-f8b99d`, `claude/ironclad-art-b-197667`, `claude/trait-audit-573141`, `measure/main-2026-11-02`, `media/shoot-2`, `ui/title-slowmo`, `work/house-audit`, and the `qa/*` and `release/*` branches.
