# Board: PLAN-TO-1.0

2026-10-31. The single visible state for the 1.0 plan. One row per task in the active phases; later phases get one row until they start. Every row names an owner, a state and the evidence that settles it. If this board and another document disagree, fix both in the same commit.

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

Phase 0 is done.

## Phase 1: close 0.5.x

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 1.1 | War Table troop sheet: rebase, compile, layout checks, visual review, merge or park | Agent + Kort | Done 2026-10-31 | Merged to `main` at `c233dbc` after Kort accepted it; the six-seat lobby followed at `784249a` |
| 1.2 | Armoury restyle (War Table Task 10 look) | Agent + Kort | Done 2026-10-31 | Restyled to match the troop sheet; `711ae89` merged at `27ddf58` |
| 1.3 | Banner and Field page polish | Agent | Not started | 1.1 is merged, so this is unblocked |
| 1.4 | Harness Narrator parity | Agent | Done 2026-10-29 | `reports/phase-1-4-harness-narrator-2026-10-29.md`; 22/22 Phase 2 checks in a fresh root |
| 1.5 | Animation pass 1: rebase, finish six Order troops, re-render, record exceptions | Agent + Kort | In progress | `work/anim-rebase` at `b023fe1` (12/12 checks, 196 turn shots) is the earlier pass, not merged. The anim-rework Phase 1 plan is approved and T13 is in progress on `work/anim-rework` (`24f18e6`); the latest grading runs use stub clips |
| 1.6 | Merge the House troop models: rebase, fix checks, baseline, turn and death suites | Agent + Kort | In progress | `work/models-rebase` at `bf86e26` (8/10 baseline) is not merged. Gilded sun staff, three tiers, installed on `art/knight-1x` (`7bfe24e`, `595cd16`); bows and troops checked in game. Partly covers 2B |
| 1.7 | Art production line: per-House generator, tiers II and III, faces, portraits, PBR | Agent | In progress | Partly done through the Gilded sun staff (`7bfe24e`); other Houses not started |
| 1.8 | Six-player network baseline | Kort + friends + Agent | Skipped by Kort 2026-10-28 | No date; method kept in `SIX-PLAYER-TEST-PLAN.md` |
| 1.9 | Fix blockers and majors from 1.8 | Agent | Blocked on 1.8 | Not started |
| 1.10 | Cut 0.5.1: fast, game, record, tag (no release) | Agent | Blocked on 1.5 to 1.7 and 1.9 | Not started |

## Phases 2 to 6

One row per started task; the rest of each phase has not started. Each row points at the plan task that defines its work.

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 2A.3 | Map Forge tools | Agent | In progress | `work/mapforge-2a3` (`bannerbound-mapforge`): Ironhold blockout reported at 42 of 88 sections at `fb579d9`, more since (`d9fb9cf`). Feeds 3.1 |
| 2B | House art (see 1.6, 1.7) | Agent + Kort | In progress | Gilded sun staff installed, `7bfe24e`, `595cd16` on `art/knight-1x` |
| 2C.1 | Mode frame | Agent | Awaiting review | On Freebuff's `mode/frame` (`9498dcf`); not reviewed, not merged |
| 2C.3 | Campaign schema 3, `MarchMath`, order lock, bonus payout | Agent | Awaiting Kort | Done on `work/campaign-2c3` (`9660a4f`), not merged. Bonus picks are being applied there and partly cover 4D.2. Iron Gate runs as Domination until Siege joins the lobby |
| 3.4 | Six-player lobby | Agent | Done 2026-10-31 | Merged to `main` at `784249a` |
| 4C.2 | Convoy escort mode | Agent | In progress | Unparked by Kort 2026-10-31; spec being drafted on `work/escort-spec` |
| 4C.3 | Host picks map and mode | Agent | Done 2026-10-31 | In `784249a`; Domination only |
| 4F.1, 4F.6 | War Table pages, 1440p layouts | Agent | In progress | War Table pages drawn at 3x at 1440p (`30e3f3f`); partly done |
| 5.1 | Balance design review | Kort + Agent | Backlogged | HOUSE-BALANCE in `BACKLOG.md` (`ed48af0`); vanilla-trait audit first |

| Phase | State | Task table |
|---|---|---|
| 2. Production lines: Map Forge, art line, mode frame | In progress | Plan section 3, 2A to 2C |
| 3. Six-player slice | In progress (3.4 done) | Plan section 3, tasks 3.1 to 3.9 |
| 4. Content completion | In progress (4C.3 done) | Plan section 3, tasks 4A to 4F |
| 5. Balance, performance, hardening | Not started (5.1 backlogged) | Plan section 3, tasks 5.1 to 5.8 |
| 6. 1.0 release | Not started | Plan section 3, tasks 6.1 to 6.6 |

## What is actually true today

Three kinds of claim, kept apart on purpose.

| Kind | State |
|---|---|
| Verified by a run | Baseline fast 22/22 and game 23/23 at `f1f6047`; Phase 2 screens 22/22 on `work/war-table-rebase`; House-model fast 22/22, Gilded art 7/7 and turn suite at `bf86e26`; animation 12/12 and 196 turn shots at `b023fe1` |
| Visually accepted | The War Table troop sheet and Armoury (Kort, 2026-10-31). Not the rebased animation or House-model lanes; the House troop art is 8/10 against a 9 gate |
| Not done at all | Six networked players on a real baseline, dedicated-server reconnect and restart, strict package, balance retake, Siege, Survival, Banner, the six-battle campaign merged, map adoption, Excalibur in the pack |

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
