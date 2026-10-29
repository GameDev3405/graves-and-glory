# Current handoff — 2026-10-28

## Where we are

`main` is now the 0.5.0 development integration, promoted from `phase-1-solo-core` plus the M1 UI work, the banner page and the harness Narrator fix. Source identity: `c41a0e1cef708e00a3a6030e1afde5b9a4194e0a83c93937116c050887efd0e4`. `main` and the three `work/*` lanes are pushed to GitHub; the Phase 0 baseline re-verified source identity `c41a0e1` at `f1f6047`, while the `v0.5.0-dev.20261028` tag marks the earlier `c4f8771` record commit. It is not a published release; six-player, dedicated-server, visual and package gates remain open.

Approved plan: [PLAN-TO-1.0.md](PLAN-TO-1.0.md), approved 2026-10-28. Phase 0 is in progress: 0.1 to 0.5 are complete; 0.6 six-player date and 0.7 task board remain. The baseline passed fast 22/22 and game 23/23 at `f1f6047`; see `reports/phase-0-5-baseline-2026-10-28.md`.

Merged and in `main`:

- Five Houses, thirty troops, six commander classes, House traits and abilities.
- In-match levels, gear tiers, Veterans/Statuses and the Armoury tab.
- War Table claims, recruit, class cards, the banner page (House chips, wide seats, used/20) and the LAST STAND HUD/feed work.
- Builder server code (`BuildMode`, six worlds) and the phase 2/3 merges.
- The verification harness and the 2026-10-28 docs cleanup.

In progress:

- **War Table troop sheet (Task 5):** `work/war-table-troop`, a 553-line `WarTableTroop` plus recruit-page wiring and layout checks. It was built after the recruit commit but never committed, so it was absent from every branch and from M1. Preserved as WIP; not merged, rebased or verified.
- **Animation pass 1:** `work/anim-pass-1`, six Order troops. Weapon clearance fixed; no clip reached 9; the bow carry revert is unrendered and `p2-turn-shots` has not been re-run.
- **House troop models:** `work/troop-models-house`, three troops (Hearthguard, Tusk, Tideguard) in stage-1 plate. Back flags removed; team colour now reads through cloth and House trim. Kort's look, tiers II/III, other sides and face close-ups remain.
- **Armoury restyle:** waits on Kort's War Table Task 10 look; the tab is currently a plain dark panel.

Not done:

- Six networked players, dedicated-server reconnect/restart and persistence.
- Strict package gate: 26 troops lack complete own tier art.
- Balance retake, which waits on map adoption; map/lobby adoption waits on the builder.
- Phase 4 modes/campaign; media is parked.

## Branch map

| Branch | Purpose |
|---|---|
| `main` | Current 0.5.0 development integration. |
| `work/anim-pass-1` | Animation pass 1, code clips for the six troop classes. |
| `work/troop-models-house` | House troop art, three troops at stage 1. |
| `work/war-table-troop` | War Table troop sheet (Task 5), WIP and unmerged. |
| `heraldic-units` | External heraldic-unit reference sheets; worktree in `Documents/gg-hx`. |

Tags: `v0.4.0` marks the old `main`; `v0.5.0-dev.20261028` marks the earlier `c4f8771` record commit, not the `f1f6047` baseline tip. `archive/2026-10/*` tags preserve the tips of removed historical branches (preview, codex experiments, snapshots, art bake-offs, builder docs). All five project branches and all 19 tags are on GitHub; a separate `claude/eloquent-fermat-lw270a` branch also exists, and removed remote branches are listed in the cleanup report.

## Candidate and evidence

`docs/releases/current-candidate.json` is authoritative for the current identity, jar hash, verification paths and known issues. `docs/reports/cleanup-2026-10-28.md` records the branch and documentation cleanup. The old `552a8c6` preview is historical, with a real Quartermaster clipping defect; do not label it current or six-player verified. Full runtime evidence remains in each worktree's `work/` directory and was not deleted.

## Next actions

1. Phase 0.6: book the six-player baseline date with five friends.
2. Phase 0.7: publish the single Phase 0/1 task board with owners, state and evidence links.
3. Then start Phase 1: rebase the War Table, House-model and animation lanes, run the six-player network baseline, and cut 0.5.1.

## Constraints

- `docs/WORKFLOW.md` is policy: gameplay, save, protocol and dependency changes need Kort; no publish or release; one writer per checkout; never kill a client; use `tools/verify.py fast|game`; never edit `Spec.java`.
- Keep the Pact wording and the parchment/dark wood/brass-frame direction; no brass fittings on table wood. New art uses 3 texels per model pixel; simple Minecraft-style faces use 2; no mass remake of accepted art.
- All 30 local worktrees and their evidence are retained. Do not remove a worktree based on ancestry alone; archived branch tips are reachable through `archive/2026-10/*` tags.
- Excalibur is approved in principle for the friends' pack (M4) and is not in this candidate.
- Historical handoffs are preserved at `docs/archive/2026-10-28-handoff-for-new-ai.md` and `docs/archive/2026-10-28-modeling-brief.md`.
