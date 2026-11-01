# Current handoff — 2026-10-31

## Where we are

`main` is now the 0.5.0 development integration, promoted from `phase-1-solo-core` plus the M1 UI work, the banner page and the harness Narrator fix. Source identity: `c41a0e1cef708e00a3a6030e1afde5b9a4194e0a83c93937116c050887efd0e4`. `main` and the three `work/*` lanes are pushed to GitHub; the Phase 0 baseline re-verified source identity `c41a0e1` at `f1f6047`, while the `v0.5.0-dev.20261028` tag marks the earlier `c4f8771` record commit. It is not a published release; six-player, dedicated-server, visual and package gates remain open.

Approved plan: [PLAN-TO-1.0.md](PLAN-TO-1.0.md), approved 2026-10-28. Phase 0: 0.1 to 0.5 complete; 0.6 skipped by Kort 2026-10-28; 0.7 board is done (`docs/BOARD.md`). Phase 0 is done. Phase 1.1 (troop sheet, `c233dbc`) and 1.2 (Armoury, `711ae89`, `27ddf58`) are merged into `main` and accepted by Kort; 1.4 is done; 1.8 is skipped by Kort. The baseline passed fast 22/22 and game 23/23 at `f1f6047`; see `reports/phase-0-5-baseline-2026-10-28.md`.

Merged and in `main`:

- Five Houses, thirty troops, six commander classes, House traits and abilities.
- In-match levels, gear tiers, Veterans/Statuses and the Armoury tab.
- War Table claims, recruit, class cards, the banner page (House chips, wide seats, used/20) and the LAST STAND HUD/feed work.
- Builder server code (`BuildMode`, six worlds) and the phase 2/3 merges.
- The verification harness and the 2026-10-28 docs cleanup.

In progress:

- **Animation pass 1:** rebased onto `main` at `d0c8d7f` on `work/anim-rebase` (`b023fe1`). All 12 animation checks pass and 196 turn shots render, including the reverted bow carry. The clearance measurement stays 0.6 to 3.7 px (recorded for Blender pass 2), which is why `p2-turn-suite` reads false. Original `work/anim-pass-1` tip preserved; not merged or pushed.
- **House troop models:** rebased onto `main` at `18522ba` on `work/models-rebase` (`bf86e26`); fast 22/22, Gilded art 7/7 and the turn suite pass, and the cull check is re-aimed. Three troops (Hearthguard, Tusk, Tideguard) in stage-1 plate, still 8/10 against the 9 gate; Kort's look, tiers II/III, other sides and face close-ups remain. Original `work/troop-models-house` tip preserved; not merged or pushed.
- **Six-player War Table lobby (3.4, 4C.3):** merged into `main` locally at `784249a` (from `work/lobby-six`); host picks map and mode, Domination only. Not pushed.
- **Animation rework (1.5):** Phase 1 approved; T13 in progress on `work/anim-rework` (`24f18e6`, `bannerbound-anim-rework`). Latest grading runs use stub clips.
- **House art (1.6, 1.7, 2B):** Gilded sun staff installed in three tiers on `art/knight-1x` (`7bfe24e`, `595cd16`); other Houses not started.
- **Map Forge (2A.3):** `work/mapforge-2a3` (`bannerbound-mapforge`), Ironhold blockout reported at 42 of 88 sections at `fb579d9`, more since (`d9fb9cf`); toward 3.1.
- **Mode frame (2C.1):** on Freebuff's `mode/frame` (`9498dcf`), unreviewed.
- **Campaign (2C.3):** done on `work/campaign-2c3` (`9660a4f`), not merged. Bonus picks are being applied there (partly 4D.2). Iron Gate runs as Domination until Siege joins the lobby.
- **Convoy escort (4C.2):** unparked by Kort 2026-10-31; spec being drafted on `work/escort-spec`.
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
| `work/anim-pass-1` | Animation pass 1, six Order troops; original WIP tip. |
| `work/anim-rebase` | Rebased animation pass 1, animation suite green at `b023fe1`; awaits Kort's visual review and merge decision. |
| `work/troop-models-house` | House troop art, three troops at stage 1; original WIP tip. |
| `work/models-rebase` | Rebased House models, checks green at `bf86e26`; awaits Kort's visual review and merge decision. |
| `work/war-table-troop` | Original War Table troop sheet WIP tip (`30b447b`), preserved untouched. |
| `work/war-table-rebase` | Troop sheet, merged into `main` 2026-10-31 after Kort accepted it. |
| `work/armoury-restyle` | Armoury restyle, merged into `main` 2026-10-31. |
| `work/lobby-six` | Six-seat lobby, merged into `main` 2026-10-31. |
| `work/anim-rework` | Animation rework Phase 1, T13 in progress. |
| `work/mapforge-2a3` | Map Forge and the Ironhold blockout. |
| `work/campaign-2c3` | Campaign 2C.3, done, not merged. |
| `mode/frame` | Mode frame, unreviewed. |
| `work/escort-spec` | Convoy escort spec in progress. |
| `heraldic-units` | External heraldic-unit reference sheets; worktree in `Documents/gg-hx`. |

Tags: `v0.4.0` marks the old `main`; `v0.5.0-dev.20261028` marks the earlier `c4f8771` record commit, not the `f1f6047` baseline tip. `archive/2026-10/*` tags preserve the tips of removed historical branches (preview, codex experiments, snapshots, art bake-offs, builder docs). All five project branches and all 19 tags are on GitHub; a separate `claude/eloquent-fermat-lw270a` branch also exists, and removed remote branches are listed in the cleanup report.

## Candidate and evidence

`docs/releases/current-candidate.json` is authoritative for the current identity, jar hash, verification paths and known issues. `docs/reports/cleanup-2026-10-28.md` records the branch and documentation cleanup. The old `552a8c6` preview is historical, with a real Quartermaster clipping defect; do not label it current or six-player verified. Full runtime evidence remains in each worktree's `work/` directory and was not deleted.

## Next actions

1. Kort: decide on `work/campaign-2c3` and review `mode/frame`; rerun the game tier on any merged tree.
2. Finish T13 of the animation rework, then 1.5; 1.7 follows the House models' acceptance.
3. Phase 1.3: Banner and Field page polish is unblocked.
4. Keep `docs/BOARD.md` current as each task moves; it is the single board.

## Constraints

- `docs/WORKFLOW.md` is policy: gameplay, save, protocol and dependency changes need Kort; no publish or release; one writer per checkout; never kill a client; use `tools/verify.py fast|game`; never edit `Spec.java`.
- Keep the Pact wording and the parchment/dark wood/brass-frame direction; no brass fittings on table wood. New art uses 3 texels per model pixel; simple Minecraft-style faces use 2; no mass remake of accepted art.
- All 30 local worktrees and their evidence are retained. Do not remove a worktree based on ancestry alone; archived branch tips are reachable through `archive/2026-10/*` tags.
- Excalibur is approved in principle for the friends' pack (M4) and is not in this candidate.
- Historical handoffs are preserved at `docs/archive/2026-10-28-handoff-for-new-ai.md` and `docs/archive/2026-10-28-modeling-brief.md`.
