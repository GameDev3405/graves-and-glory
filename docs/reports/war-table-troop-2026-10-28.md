# War Table troop sheet — 2026-10-28

Status: implementation and checks complete on `work/war-table-rebase`; green at `a423cc8`. **Not merged or pushed.** Kort's visual acceptance is the remaining gate.

## Source

| Item | Result |
|---|---|
| Base | `main` @ `1f6c799` (Phase 0.5 baseline) |
| Original lane | `work/war-table-troop` @ `30b447b` (one WIP commit, 36 behind `main`) |
| Rebased lane | `work/war-table-rebase` @ `a423cc8` — WIP `ee3a247` plus checks `a423cc8` |
| Source identity | `951e2133b939c6b35cc8198ab7e15af86ac0a3d2c04e439c61b0e20567ba8a4c` |
| Worktree | clean at the certification run; `dirty: false` in the report |

The rebase applied with no conflicts. The original `work/war-table-troop` branch and the `bannerbound-ui-p2` worktree were left untouched.

## What the lane contains

- `WarTableTroop.java` (553 lines): the troop sheet as two papers. The letter carries the live model, name, House and rarity, stars, deploy cost, the recruit action tag and its reason. The sheet has two faces: **Troop** (star ladder, copies or shards held, Mastery title/next level and bars, three battle levels with names and ability lines) and **Matchups** (beats, beaten by, counter lines with favour/against glyphs).
- `CampScreen`: `page troop`, `troopSheet`, render/tick/key routing and `openTroop`.
- `WarTableRecruit`: the letter's name is a `sheet` widget that opens the sheet; `back()` returns to the recruit page on the same House and troop.
- `Phase1Checks`: the recruit page's layout counts include the new `sheet` widget.
- `Phase2Checks`: `p2-troop-card` now reads both faces from the sheet's drawn state; `p2-roll-card-troop` now expects the buy reveal on the letter instead of the removed roll-card overlay. `WarTableTroop.CounterDraw` records every drawn counter line because a row can appear both for what the troop deals and what it takes.

## Verification

| Check | Result | Evidence |
|---|---|---|
| Fast tier | 22 checks, exit 0 | `work/verify/20261028-235021-65908/result.json` (23 checks including game) |
| Game tier | 23 checks, exit 0, `dirty: false` | same report; 12/12 game evidence lines including `p1-combat-suite` |
| Phase 2 `screens` | all checks pass, `p2-screens-suite=true` | `work/phase2/screens-20261028-234651/gradle.log` |

The game tier used JDK 17 and the documented `work/verify-bin/pwsh.exe` shim. Preflight produced line-ending-only changes in `Spec.java` and `sheets/preflight.json`; the generated content was unchanged and the files were restored.

The `screens` run was made on the same working-tree content committed as `a423cc8`; it includes `p2-troop-card=true` and `p2-roll-card-troop=true`. It also passes the existing recruit, notice, claims, veteran and restore checks.

## Visual inspection

Screenshots from the passing run:

- `work/phase2/screens-20261028-234651/run/screenshots/p2-chase-troop-card.png` — Troop face at 1280x720, GUI 3.
- `work/phase2/screens-20261028-234651/run/screenshots/p2-chase-troop-matchups.png` — Matchups face.

Both papers sit inside the table, the tabs read clearly, the star ladder and Mastery bars align, the three level lines are readable, and the six counter lines fit the sheet. The Templar's two C3 lines are both drawn: one outgoing (`Deals x0.6`) and one incoming (`Takes x0.6`).

Not inspected: 1440p/4K, the 854x480 compact page, hover/focus/disabled states, the sheet's notice line, and the long-text/error paths. Kort's look acceptance has not been given.

## Not tested

- Six networked players, dedicated-server reconnect/restart and persistence.
- Strict package gate; it still refuses troops with incomplete tier art.
- The full lobby/rewards loop.

## Decision

The lane is ready for Kort's visual review. It is deliberately not merged: Phase 1.1 says merge or park, and the approved workflow does not treat a green harness run as visual acceptance. After sign-off, merge `work/war-table-rebase` into `main` and rerun the game tier on the merged tree. Otherwise park the branch with this report.
