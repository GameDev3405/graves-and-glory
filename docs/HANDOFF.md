# Current handoff: 2026-11-07

## Where we are

`main` is the 0.5.1 development integration at `2fc30f50` (2026-11-07), checked out in `C:/Users/kortt/Melty/bannerbound-mainmerge`. It is local only, not pushed, and not a published release. `mod_version` is 0.5.1; the 0.5.1 candidate (`release/0.5.1`, `c99fe907`, rc2 notes `52fa325a`) was cut on 2026-11-04 and not published. The candidate record `docs/releases/current-candidate.json` is at `bb2d85c4` and needs a refresh before any candidate is claimed.

Approved plan: [PLAN-TO-1.0.md](PLAN-TO-1.0.md) (approved 2026-10-28). The 1.0 plan with the hosted network (Phase 7) is on branch `docs/plan-1.0-network` and is not in `main` until Kort merges it. The single board is [BOARD.md](BOARD.md). Policy is [WORKFLOW.md](WORKFLOW.md), including Kort's model rules of 2026-11-07.

Merged and in `main`:

- Five Houses, thirty troops (all with their own models), House traits and abilities, in-match levels, Veterans and Statuses, the Armoury tab.
- Troop art for all five Houses at tiers I to III, the House identity set and recolour, hand-authored with the `art/hand` builders. Elbows and knees for biped troops (`37494090`, 2026-11-04). Attack clips for every troop family (`1be70ff9`), checked in game for all 30 troops with fixes D1 to D6 (`1d3f2edd`, QA polish, Kort signed off 2026-11-04).
- Commander gear, plans 1 to 3: foundation and save v4 (`a38e4218`), the Arsenal screen and Blacksmith (`9c13ed6d`), gear combat with relics on keys 1 to 3 (`82ace819`). The commander classes are removed. Gear art and the House armour sets are merged.
- Command UI (`e771d5f1`, 2026-11-05): left-edge order panel, formations, custom numbered groups, charge with overhead camera. Excalibur look kept off weapons (`6442b1af`, 2026-11-05).
- Audit fixes (2026-11-07): `fix/ui-polish` (`1c440dd5`) and `fix/client-state` (`2fc30f50`), including the deleted `EntranceOverlayMixin`.
- War Table pages and the six-seat lobby; Domination, Siege, Convoy and Survival; campaign battles 1 to 6; bots; map engine; the dedicated server kit and server download; title screen; audio cues (Kort's picks, layers, order sounds); release tooling and notes.

Awaiting Kort, not in `main`:

- `docs/plan-1.0-network`: the hosted network, matchmaking and Old Hold hub designs (Kort's 2026-11-07 grilling sessions), plus the PLAN-TO-1.0, BACKLOG, DECISIONS and PARKED updates. Awaits his review and merge.
- `feat/oldhold-wiring` (two commits ahead of `main`): the Old Hold lobby (concept C, Q129) and the showcase pieces. Awaits his in-game walk. No hub feature work starts before the layout is locked. `feat/lobby-oldhold` holds an unreviewed WIP commit of lobby terrain, preserved only.
- `qa/fix-ingame` is a QA harness branch. It is never merged.
- The Axiom and Ledger mod download for the builder server (`BUILDER-SERVER.md`) awaits Kort's yes.
- Lobby and map sign-offs: Kort designs all five maps in the map engine editor and signs off each master layout. Agents do not build layouts.
- Knight v2 and v4 deaths: approved 2026-11-07 as the template for other troops (`anim/geckolib-rebuild`, `781c12ee`, not in `main`). Which units reuse which parts is still open with Kort.
- Text and choices: mode text in game (Q19a), briefings 2 to 6 (Q74), the CurseForge app import (Q75), 1080p, 1440p and 4K checks (VIS-01), audio polish (Q123).
- Server EULA: see Constraints.

Not done:

- Six networked players on a real baseline; dedicated-server reconnect, restart and persistence on a live server; everything in Phase 7.
- Kort's map designs and their adoption; the balance retake and bot round robin on them.
- Final art gate (4B.7), 5.6 QA re-run on current `main`, release candidate record refresh and final session; publish only on Kort's explicit go.

## Branch map

Most session branches are merged. A branch being an ancestor of `main` is a merge fact; it is not a reason to remove a worktree.

| Branch | Purpose |
|---|---|
| `main` | Current 0.5.1 development integration. |
| `work/anim-rework` | Original jointed-troop commits, superseded by `37494090`. Keep; do not merge. |
| `docs/plan-1.0-network` | 1.0 hosted network plan, awaiting Kort's review and merge. |
| `feat/oldhold-wiring`, `feat/lobby-oldhold` | Old Hold lobby; awaiting Kort's in-game walk. |
| `anim/geckolib-rebuild` | Knight v2 and v4 deaths, approved 2026-11-07. |
| `qa/fix-ingame` | QA harness, never merged. |
| `work/anim-edit` | Animation editor, awaiting Kort. |
| `audio/pixabay` | Pixabay audio set, awaiting Kort's listen. |
| `claude/trait-audit-573141`, `claude/audit-measure-5b5fc8` | Vanilla-trait audit and measurement. |
| `work/anim-pass-1`, `work/troop-models-house`, `work/war-table-troop` | Original tips preserved untouched; their rebases are archived as tags `archive/anim-rebase-2026-11-02` and `archive/models-rebase-2026-11-02`. |
| `work/escort-spec`, `work/survival-spec`, `work/briefings` | Spec and draft branches; their content is in `main` (Convoy, Survival, briefings). |
| `mode/frame` | Mode frame, merged (`d24592e`). The merge is archived as tag `archive/mode-frame-merge-2026-11-02`. |
| `heraldic-units` | External heraldic-unit reference sheets; worktree in `Documents/gg-hx`. |

Tags: `v0.4.0` marks the old `main`; `v0.5.0-dev.20261028` marks the earlier `c4f8771` record commit, not the `f1f6047` baseline tip. `archive/2026-10/*` tags preserve the tips of removed historical branches (preview, codex experiments, snapshots, art bake-offs, builder docs). The 2026-10-28 cleanup report lists the removed remote branches.

## Candidate and evidence

`docs/releases/current-candidate.json` is authoritative for the current identity, jar hash, verification paths and known issues; it predates the 2026-11-05 to 2026-11-07 merges and needs a refresh before any candidate is claimed. `docs/reports/cleanup-2026-10-28.md` records the branch and documentation cleanup. The old `552a8c6` preview is historical, with a real Quartermaster clipping defect; do not label it current or six-player verified. Full runtime evidence remains in each worktree's `work/` directory and was not deleted.

## Next actions

1. Kort: review and merge `docs/plan-1.0-network`; walk the Old Hold in game (`feat/oldhold-wiring`) and lock the layout; say yes or no to the Axiom and Ledger download; sign off lobby and map layouts as he designs them. Network hub features (task 7.5) and the rest of Phase 7 wait on the walk and the merge.
2. Agents, without Kort, a server EULA or new maps: refresh the candidate record and release notes on current `main` (`2fc30f50`), re-run release verify and the QA rows that need no server, re-run the mode gate, then roll the knight template to the next units once Kort says which.
3. Keep `docs/BOARD.md` current as each task moves; it is the single board.

## Constraints

- `docs/WORKFLOW.md` is policy: gameplay, save, protocol and dependency changes need Kort; no publish or release; one writer per checkout; never kill a client; use `tools/verify.py fast|game`; never edit `Spec.java`.
- Never write `eula.txt` or accept the server EULA; ask Kort.
- Keep the Pact wording and the parchment/dark wood/brass-frame direction; no brass fittings on table wood. Troop art uses 1 texel per model pixel (weapons 2), hand-authored with the `art/hand` builders (Kort 2026-10-30); simple Minecraft-style faces use 2. No pennants or standards on any troop's back.
- All local worktrees and their evidence are retained. Do not remove a worktree based on ancestry alone; archived branch tips are reachable through `archive/2026-10/*` tags.
- Historical handoffs are preserved at `docs/archive/2026-10-28-handoff-for-new-ai.md` and `docs/archive/2026-10-28-modeling-brief.md`.
