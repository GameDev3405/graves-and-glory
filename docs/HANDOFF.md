# Current handoff — 2026-11-02

## Where we are

`main` is now the 0.5.0 development integration, promoted from `phase-1-solo-core` plus the M1 UI work, the banner page and the harness Narrator fix. Source identity: `c41a0e1cef708e00a3a6030e1afde5b9a4194e0a83c93937116c050887efd0e4`. `main` and the three `work/*` lanes are pushed to GitHub; the Phase 0 baseline re-verified source identity `c41a0e1` at `f1f6047`, while the `v0.5.0-dev.20261028` tag marks the earlier `c4f8771` record commit. It is not a published release; six-player, dedicated-server, visual and package gates remain open.

Approved plan: [PLAN-TO-1.0.md](PLAN-TO-1.0.md), approved 2026-10-28. Phase 0: 0.1 to 0.5 complete; 0.6 skipped by Kort 2026-10-28; 0.7 board is done (`docs/BOARD.md`). Phase 0 is done. Phase 1.1 (troop sheet, `c233dbc`) and 1.2 (Armoury, `711ae89`, `27ddf58`) are merged into `main` and accepted by Kort; 1.4 is done; 1.8 is skipped by Kort. The baseline passed fast 22/22 and game 23/23 at `f1f6047`; see `reports/phase-0-5-baseline-2026-10-28.md`.

Merged and in `main` (local only, not pushed):

- Five Houses, thirty troops, six commander classes, House traits and abilities; in-match levels, gear tiers, Veterans/Statuses and the Armoury tab.
- War Table claims, recruit, class cards, the troop sheet and the six-seat lobby (`784249a`). The lobby offers Domination, Siege and Convoy.
- Phase 1.3 Banner and Field pages with drag and drop (`cc88186`).
- Troop rework (`c26a3d8`): six troops keep their original art with the upright weapon grip fix; Ballista and Mortar get new art.
- Siege (clock 185 s) with the archer climb fix (`1f14562`), Convoy, the mode frame, Siege bots and `tools/mode_gate.py` (`4fa1872`).
- Campaign 2C.3 (`8e39637`), the audio cues (`audio/cues`) and the server-rules merges.
- Docs only, merged 2026-11-02: `docs/MAP-STANDARDS.md` (2A.7), `docs/ART-STYLE-GUIDE.md` (2B.2), `docs/reports/economy-check-2026-10-30.md` (5.4).
- The verification harness and the 2026-10-28 docs cleanup.

Awaiting Kort, not merged:

- Map engine slice 1, on `claude/map-engine-slice-1-65134c`.
- Audio (Pixabay), on `audio/pixabay` in `bannerbound-audio`.
- House balance retune, on `work/house-audit` (`967e980`; handoff `81bbc32`).
- Animation editor, on `work/anim-edit` in `bannerbound-anim-edit`.
- Animation rework (1.5), on `work/anim-rework`; House troop models.

In progress:

- **Archer bow rework.**
- **House art (1.6, 1.7, 2B):** Gilded sun staff installed in three tiers on `art/knight-1x` (`7bfe24e`, `595cd16`); Ironclad 1x under 2B.3; other Houses not started.
- **Dedicated server kit (3.5):** `claude/server-kit-4fed75`.
- **War Table scale (4F.1, 4F.6):** pages drawn at 3x at 1440p; partly done.
- **Balance (5.1):** HOUSE-BALANCE backlogged, vanilla-trait audit first (`ed48af0`). Phase 6 not started.

Not done:

- Six networked players, dedicated-server reconnect/restart and persistence.
- Strict package gate: 26 troops lack complete own tier art.
- Balance retake, which waits on map adoption; map/lobby adoption waits on the builder.
- Phase 4 modes/campaign; media is parked.

## Branch map

| Branch | Purpose |
|---|---|
| `main` | Current 0.5.0 development integration. |
| `work/anim-pass-1` | Animation pass 1 original tip. The rebase is archived as tag `archive/anim-rebase-2026-11-02`. |
| `work/troop-models-house` | House troop art original tip. The rebase is archived as tag `archive/models-rebase-2026-11-02` (8/10 at `bf86e26`). |
| `work/war-table-troop` | Original War Table troop sheet WIP tip (`30b447b`), preserved untouched. |
| `work/war-table-rebase` | Troop sheet, merged into `main` 2026-10-31 after Kort accepted it. |
| `work/armoury-restyle` | Armoury restyle, merged into `main` 2026-10-31. |
| `work/lobby-six` | Six-seat lobby, merged into `main` 2026-10-31. |
| `work/anim-rework` | Animation rework, awaiting Kort's merge decision. |
| `work/mapforge-2a3` | Map Forge and the Ironhold blockout. |
| `work/campaign-2c3` | Campaign 2C.3, merged into `main` (`8e39637`). |
| `mode/frame` | Mode frame, merged (`d24592e`). The merge is archived as tag `archive/mode-frame-merge-2026-11-02`. |
| `work/escort-spec` | Convoy escort spec; Convoy itself is merged. |
| `heraldic-units` | External heraldic-unit reference sheets; worktree in `Documents/gg-hx`. |

Tags: `v0.4.0` marks the old `main`; `v0.5.0-dev.20261028` marks the earlier `c4f8771` record commit, not the `f1f6047` baseline tip. `archive/2026-10/*` tags preserve the tips of removed historical branches (preview, codex experiments, snapshots, art bake-offs, builder docs). All five project branches and all 19 tags are on GitHub; a separate `claude/eloquent-fermat-lw270a` branch also exists, and removed remote branches are listed in the cleanup report.

## Candidate and evidence

`docs/releases/current-candidate.json` is authoritative for the current identity, jar hash, verification paths and known issues. `docs/reports/cleanup-2026-10-28.md` records the branch and documentation cleanup. The old `552a8c6` preview is historical, with a real Quartermaster clipping defect; do not label it current or six-player verified. Full runtime evidence remains in each worktree's `work/` directory and was not deleted.

## Next actions

1. Kort: decide on the awaiting items above; rerun the game tier on any merged tree.
2. Finish the archer bow rework; 1.7 follows the House models' acceptance.
3. Keep `docs/BOARD.md` current as each task moves; it is the single board.

## Constraints

- `docs/WORKFLOW.md` is policy: gameplay, save, protocol and dependency changes need Kort; no publish or release; one writer per checkout; never kill a client; use `tools/verify.py fast|game`; never edit `Spec.java`.
- Keep the Pact wording and the parchment/dark wood/brass-frame direction; no brass fittings on table wood. Troop art uses 1 texel per model pixel (weapons 2), hand-authored with the `art/hand` builders (Kort 2026-10-30; the old 3-texel generator art is retired, and Kort approved rebuilding Hearthguard, Tusk and Tideguard at 1 texel on 2026-11-01); simple Minecraft-style faces use 2.
- All 30 local worktrees and their evidence are retained. Do not remove a worktree based on ancestry alone; archived branch tips are reachable through `archive/2026-10/*` tags.
- Excalibur is approved in principle for the friends' pack (M4) and is not in this candidate.
- Historical handoffs are preserved at `docs/archive/2026-10-28-handoff-for-new-ai.md` and `docs/archive/2026-10-28-modeling-brief.md`.
