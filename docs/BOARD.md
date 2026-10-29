# Board: PLAN-TO-1.0

2026-10-29. The single visible state for the 1.0 plan. One row per task in the active phases; later phases get one row until they start. Every row names an owner, a state and the evidence that settles it. If this board and another document disagree, fix both in the same commit.

States: **Done**, **In progress**, **Awaiting Kort** (work finished, visual or merge decision pending), **Blocked** (a named task must land first), **Skipped**, **Not started**.

Owners are decisions, not file ownership. `Agent` means implementation; `Kort` means a decision, visual gate or scheduling.

## Phase 0: approve and freeze

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 0.1 | Approve the plan and the seven decisions | Kort | Done 2026-10-28 | `PLAN-TO-1.0.md`, `DECISIONS.md` |
| 0.2 | Freeze the 1.0 scope and exclusions | Kort | Done 2026-10-28 | `PLAN-TO-1.0.md` section 0.2 |
| 0.3 | Confirm the slice map (Ironhold) | Kort | Done 2026-10-28 | `DECISIONS.md`, plan 7.1 |
| 0.4 | Confirm the adoption format and AI build authority | Kort | Done 2026-10-28 | Plan 7.2 and 7.3 |
| 0.5 | Re-run fast and game on `main`; record the baseline | Agent | Done 2026-10-28 | `reports/phase-0-5-baseline-2026-10-28.md`; fast 22/22, game 23/23 at `f1f6047` |
| 0.6 | Book the six-player baseline date | Kort | Skipped 2026-10-28 | `SIX-PLAYER-TEST-PLAN.md` section 0; no date held |
| 0.7 | Publish a single visible board | Agent | Done 2026-10-29 | This file |

## Phase 1: close 0.5.x

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 1.1 | War Table troop sheet: rebase, compile, layout checks, visual review, merge or park | Agent + Kort | Awaiting Kort | `work/war-table-rebase` at `a423cc8`: fast/game 23/23, Phase 2 screens pass; report on that branch. Not merged or pushed |
| 1.2 | Armoury restyle (War Table Task 10 look) | Agent + Kort | Blocked on 1.1 and the look | `BACKLOG.md` ARMOURY-LOOK |
| 1.3 | Banner and Field page polish | Agent | Blocked on 1.1 | Not started |
| 1.4 | Harness Narrator parity | Agent | Done 2026-10-29 | `reports/phase-1-4-harness-narrator-2026-10-29.md`; 22/22 Phase 2 checks in a fresh root |
| 1.5 | Animation pass 1: rebase, finish six Order troops, re-render, record exceptions | Agent + Kort | Awaiting Kort | `work/anim-rebase` at `b023fe1`: 12/12 animation checks, 196 turn shots, clearance 0.6 to 3.7 px recorded; report on that branch. Not merged or pushed |
| 1.6 | Merge the House troop models: rebase, fix checks, baseline, turn and death suites | Agent + Kort | Awaiting Kort | `work/models-rebase` at `bf86e26`: fast 22/22, Gilded art 7/7, turn suite; 8/10 baseline recorded. Not merged or pushed |
| 1.7 | Art production line: per-House generator, tiers II and III, faces, portraits, PBR | Agent | Blocked on 1.6 acceptance | Not started |
| 1.8 | Six-player network baseline | Kort + friends + Agent | Skipped by Kort 2026-10-28 | No date; method kept in `SIX-PLAYER-TEST-PLAN.md` |
| 1.9 | Fix blockers and majors from 1.8 | Agent | Blocked on 1.8 | Not started |
| 1.10 | Cut 0.5.1: fast, game, record, tag (no release) | Agent | Blocked on 1.1 to 1.7 and 1.9 | Not started |

## Phases 2 to 6

Not started. Each row points at the task table that defines its work.

| Phase | State | Task table |
|---|---|---|
| 2. Production lines: Map Forge, art line, mode frame | Not started (can start now in parallel) | Plan section 3, 2A to 2C |
| 3. Six-player slice | Not started | Plan section 3, tasks 3.1 to 3.9 |
| 4. Content completion | Not started | Plan section 3, tasks 4A to 4F |
| 5. Balance, performance, hardening | Not started | Plan section 3, tasks 5.1 to 5.8 |
| 6. 1.0 release | Not started | Plan section 3, tasks 6.1 to 6.6 |

## What is actually true today

Three kinds of claim, kept apart on purpose.

| Kind | State |
|---|---|
| Verified by a run | Baseline fast 22/22 and game 23/23 at `f1f6047`; Phase 2 screens 22/22 on `work/war-table-rebase`; House-model fast 22/22, Gilded art 7/7 and turn suite at `bf86e26`; animation 12/12 and 196 turn shots at `b023fe1` |
| Visually accepted | None of the three rebased art lanes. Earlier UI work is accepted; the House troop art is 8/10 against a 9 gate |
| Not done at all | Six networked players, dedicated-server reconnect and restart, strict package, balance retake, Siege, Survival, Banner, the six-battle campaign, map adoption, Excalibur in the pack |

## Where the authoritative records live

| Record | File |
|---|---|
| Candidate identity, jar hash, known issues | `docs/releases/current-candidate.json` |
| Approved scope and decisions | `docs/PLAN-TO-1.0.md`, `docs/DECISIONS.md` |
| Policy | `docs/WORKFLOW.md` |
| Setup and verification commands | `docs/SETUP.md` |
| Open questions for Kort | `docs/QUESTIONS-FOR-KORT.md` |
| Six-player method | `docs/SIX-PLAYER-TEST-PLAN.md` |
| Verification run data | `work/verify/<timestamp-pid>/result.json` in each checkout |
