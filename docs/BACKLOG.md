# Active backlog

Only explicitly approved unblocked items may run unattended. The 2026-10-28 cleanup promoted `main` and reconciled the docs. Kort approved the 1.0 plan on 2026-10-28; Phase 0 has started, but no Phase 1 lane work is authorized until Phase 0 exits.

| ID | State | Outcome / next action |
|---|---|---|
| OPT-1 | Done | Workflow, navigation, verification and Graphify tooling are in place; see the optimization report for the evidence. |
| CLEANUP-1 | Done 2026-10-28 | `main` promoted to the 0.5.0 integration; README, HANDOFF, BACKLOG, ROADMAP and candidate record reconciled; old remote branches archived and removed. See `reports/cleanup-2026-10-28.md`. |
| PLAN-1.0 | Approved 2026-10-28; Phase 1 in progress | Full-vision 1.0 plan and seven decisions approved (`docs/PLAN-TO-1.0.md`, `docs/DECISIONS.md`). Phase 0: 0.1 to 0.5 complete; 0.6 skipped by Kort 2026-10-28; 0.7 board open. Phase 1.1 is green on `work/war-table-rebase` and waits on Kort's visual review. Phase 1.4 is done and checked. Next: 1.5 and 1.6 rebases, then 1.3. Baseline is `docs/reports/phase-0-5-baseline-2026-10-28.md`. |
| OPT-2 | Deferred by Kort 2026-10-28 | All 30 worktrees and their run evidence are retained. No worktree or bulk run data was deleted; revisit only with explicit per-folder approval. |
| OPT-3 | Awaiting observations | Measure the next five comparable accepted deliverables and the next full usage cycle in `EFFICIENCY.md`. |
| SLICE-1 | Scope and plan approved 2026-10-28; Phase 0 in progress | 3v3 humans plus bots, one House per player, six classes, a short campaign run, one capture scenario and a six-client baseline first. Slice map is Ironhold; map adoption and AI build authority are decided in PLAN-TO-1.0.md. Do not implement map/lobby work until Phase 0 exits. |
| UI-INTEGRATION | Done | Claims, recruit, Armoury, class cards, banner House chips and the HUD LAST STAND/feed work are in `main`. |
| TASK-5 | Rebased; green; awaits Kort | The War Table troop sheet, recruit-page wiring and checks are rebased onto `main` at `1f6c799` on `work/war-table-rebase` (`a423cc8`). Fast/game 23/23 and the Phase 2 `screens` suite pass. Not merged or pushed; visual acceptance and the merge decision are Kort's. See the report on that branch. |
| ARMOURY-LOOK | Waits for Kort | The Armoury tab is a plain dark panel. Its War Table restyle waits for Kort's Task 10 look. |
| ANIM-1 | WIP branch `work/anim-pass-1`; Phase 1.5 | Animation pass 1 for the six Order troop classes. Weapon clipping improved but no clip reached 9; bow revert is unrendered. Next: rebase onto `main` and re-render the contact sheets. |
| MODELS-1 | WIP branch `work/troop-models-house`; Phase 1.6 | Three House troops in stage-1 plate, flags removed. Kort's look, tiers II/III, other sides and face close-ups remain. Next: rebase onto `main`, fix the cull and chest-panel checks, run the turn and death suites. |
| BALANCE | Candidate and map dependent | Run the levels-on round robin after the adopted maps exist; the prior AI baseline caveat remains. |
| MEDIA | Parked by Kort | Resume only after maps, lobby and units are polished and scope is confirmed. |

Report template: outcome; source commit + dirty fingerprint; artifact hash; changed areas; checks and exits; evidence; defects; questions/assumptions; implementation/test/visual/acceptance status. A complete implementation with missing required validation remains incomplete overall.
