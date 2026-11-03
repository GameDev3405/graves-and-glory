# Current handoff — 2026-11-02

## Where we are

`main` is the 0.5.0 development integration, at `dfec25d` on 2026-11-03 (local only, not pushed). It is not a published release. The Phase 0 baseline passed fast 22/22 and game 23/23 at `f1f6047`; a release verify at `01b0c77` (2026-11-03) passed, package and readback included; verify game passed at `3885d32`. Six-player, live dedicated-server, animation and map gates remain open. The merge log is `work/MERGE-QUEUE.md`, Kort's answers are in `work/ANSWERS-2026-11-0*.md` and open questions in `work/PENDING-QUESTIONS.md` (in the main checkout, `C:/Users/kortt/Melty/bannerbound`).

Approved plan: [PLAN-TO-1.0.md](PLAN-TO-1.0.md), approved 2026-10-28. The single board is [BOARD.md](BOARD.md); it is the place to read row by row state. Phase 0 is done, Phase 1 is done except the animation merge (1.5) and the 0.5.1 cut (1.10), and Phases 2 to 6 are in progress.

Merged and in `main`:

- Five Houses, thirty troops, six commander classes, House traits and abilities; in-match levels, gear tiers, Veterans/Statuses and the Armoury tab.
- Troop art for all five Houses at tiers I to III: Gilded (flagships, tiers II/III), Ironclad, Wildmoot (with the Tusk), Tidecallers (sea-blue Tideguard) and Graveborn, plus the troop rework (`c26a3d8`) and the `.bbmodel` round-trip. Each was approved by Kort on 2026-11-02.
- War Table claims, recruit, class cards, the troop sheet, Armoury, Banner and Field pages, the campaign card and the six-seat lobby; the report layout and the accessibility pass.
- Modes: Domination, Siege (185 s clock, 15 s yard hold, stage-0 cap 50%), Convoy (slowed wagon, escort 12/20) and Survival, with Siege bots, `tools/mode_gate.py` and sheet-driven feed, ledger and report lines (draft text).
- Campaign: schema 3, order lock, bonus payout, all six battles playing and paying in their own mode, victory gold, and pay by time seated (battles 2 to 3 are 2v2, 4 to 6 are 3v3). Briefings for battles 2 to 6 are drafts for Kort to edit.
- Map engine slice 1 and plans 2 to 4 (engine map slot, AI handoff, building library) and the editor files for the five maps; Map Forge and the Ironhold surface; docs `MAP-STANDARDS.md`, `ART-STYLE-GUIDE.md` and the economy check.
- Server kit (`3fa9dd1`), perf baseline, slice dry runs, QA rules (partial pay for a stopped match, ops-only `/warp`, 2 minute reconnect hold), arena autosave skipped in battle, and the teleport-sync minors.
- House balance Set A, arrow speed, and the House audit probe (no stat changes). Audio cues (`audio/cues`).
- Release tooling: strict package, Modrinth and CurseForge exports, Excalibur 1.20 bundled unmodified with credit (Kort approved), release notes draft and README refresh.

Awaiting Kort, not in `main`:

- Maps: Kort designs all five maps himself in the map engine editor (2026-11-02) and signs off each master layout. Agents do not build layouts.
- Animation rework (1.5), `work/anim-rework`: merge approved (Q62) but the branch conflicts with `main` in 10 files and its session is merging `main` in. Animation editor `work/anim-edit`: Kort poses the guard himself.
- Audio, `audio/pixabay` (`fa54d8e`): not listened to yet.
- Vanilla-trait audit branches `claude/trait-audit-573141` and `claude/audit-measure-5b5fc8`: Kort picks traits B to F after measure A.
- Text and choices: mode text in game (Q19a), briefings 2 to 6 (Q74), battle 6 mix (Q76), difficulty ramp (Q77), battle 2 bonus switch (Q78), the CurseForge app import (Q75), 1080p, 1440p and 4K checks (VIS-01).
- Server EULA (Q79): the teleport-sync live check and the dedicated-server smoke test need a server EULA accepted in a scratch folder. Agents do not accept it.

Not done:

- Six networked players on a real baseline; dedicated-server reconnect, restart and persistence on a live server.
- Kort's map designs and their adoption; the balance retake and bot round robin on them.
- Title screen, House identity set (4B.5), final art gate, 5.6 QA runs, release candidate record and final session; publish only on Kort's explicit go.

## Branch map

Most session branches are merged. A branch being an ancestor of `main` is a merge fact; it is not a reason to remove a worktree.

| Branch | Purpose |
|---|---|
| `main` | Current 0.5.0 development integration. |
| `work/anim-rework` | Animation rework, merge approved, waiting on its session. |
| `work/anim-edit` | Animation editor, awaiting Kort. |
| `audio/pixabay` | Pixabay audio set, awaiting Kort's listen. |
| `claude/trait-audit-573141`, `claude/audit-measure-5b5fc8` | Vanilla-trait audit and measurement. |
| `work/anim-pass-1`, `work/troop-models-house`, `work/war-table-troop` | Original tips preserved untouched; their rebases are archived as tags `archive/anim-rebase-2026-11-02` and `archive/models-rebase-2026-11-02`. |
| `work/escort-spec`, `work/survival-spec`, `work/briefings` | Spec and draft branches; their content is in `main` (Convoy, Survival, briefings). |
| `mode/frame` | Mode frame, merged (`d24592e`). The merge is archived as tag `archive/mode-frame-merge-2026-11-02`. |
| `heraldic-units` | External heraldic-unit reference sheets; worktree in `Documents/gg-hx`. |

Tags: `v0.4.0` marks the old `main`; `v0.5.0-dev.20261028` marks the earlier `c4f8771` record commit, not the `f1f6047` baseline tip. `archive/2026-10/*` tags preserve the tips of removed historical branches (preview, codex experiments, snapshots, art bake-offs, builder docs). The 2026-10-28 cleanup report lists the removed remote branches.

## Candidate and evidence

`docs/releases/current-candidate.json` is authoritative for the current identity, jar hash, verification paths and known issues; it predates the 2026-11-02 merges and needs a refresh before any candidate is claimed. `docs/reports/cleanup-2026-10-28.md` records the branch and documentation cleanup. The old `552a8c6` preview is historical, with a real Quartermaster clipping defect; do not label it current or six-player verified. Full runtime evidence remains in each worktree's `work/` directory and was not deleted.

## Next actions

1. Kort: the awaiting items above. Maps come first, since adoption, the bot round robin and the balance retake all wait on them.
2. Agents, without Kort, a server EULA or new maps: re-run release verify on current `main`, refresh the candidate record and release notes, re-run art QA on the tier II/III art, run the QA matrix rows that need no server, and re-run the mode gate.
3. Keep `docs/BOARD.md` current as each task moves; it is the single board.

## Constraints

- `docs/WORKFLOW.md` is policy: gameplay, save, protocol and dependency changes need Kort; no publish or release; one writer per checkout; never kill a client; use `tools/verify.py fast|game`; never edit `Spec.java`.
- Never write `eula.txt` or accept the server EULA; ask Kort.
- Keep the Pact wording and the parchment/dark wood/brass-frame direction; no brass fittings on table wood. Troop art uses 1 texel per model pixel (weapons 2), hand-authored with the `art/hand` builders (Kort 2026-10-30); simple Minecraft-style faces use 2. No pennants or standards on any troop's back.
- All local worktrees and their evidence are retained. Do not remove a worktree based on ancestry alone; archived branch tips are reachable through `archive/2026-10/*` tags.
- Historical handoffs are preserved at `docs/archive/2026-10-28-handoff-for-new-ai.md` and `docs/archive/2026-10-28-modeling-brief.md`.
