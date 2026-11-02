# Mode frame (2C.1) — 2026-10-29

## Outcome

Domination now runs behind the Phase 4 mode frame. `Battle` no longer hard-codes the win score, score interval, round
length, timeout or income: they come from a `modes` sheet row through a per-round `BattleMode` object. Domination's
numbers and behaviour are unchanged; the only mode registered is `domination` and every map hosts it.

The lobby also carries the mode now: `battlemode:<id>` sets it (refused with a notice unless a registered mode hosts the
map), a map switch resets it to Domination when the new map does not, the camp snapshot carries the mode block (id, rule,
three how-to lines, side, tracks and their formats), and a round leaves a crash marker under the world save that the
next map ready reads back.

Branch `mode/frame` in `bannerbound-modes`, from `main` at `bd05fa2`. No PR, no push.

## What changed

| File | Change |
|---|---|
| `sheets/design.json` | New `modes` sheet with one `domination` row (`maps 0,1,2,3,4`, 900 s, target 300, interval 80, scales 1.0/1.0, `timeout score`, `Domination to {n}`, three `howTo` lines in `howTo` and `howToDefend`). `rules` loses `winScore`, `scoreInterval`, `roundSeconds`. |
| `tools/preflight.py` | Validates the `modes` sheet: map indexes, timeout rule, one `{n}` at most, non-negative numbers, a scoring mode with a target, a `domination` row, one to three `howTo` and `howToDefend` parts. Regenerates `Spec.java`. |
| `src/main/java/gg/gravesandglory/ModeMath.java` | Pure rules: `waveCount`, `ruleLine`, `howTo`, `sizeLine`, `siegeStrength`, `roles`, `ledgerText`. |
| `BattleMode.java`, `Modes.java`, `DominationMode.java` | The per-round object (setup, teardown, tick, field base and barracks, income, timeout, clock, ledger, tracks, the snapshot block), the registry (`register`, `row`, `create`, `hosts`, `ruleLine`), the crash marker (`mark`, `unmark`, `recover`), and Domination's interval scoring moved into `tick`. |
| `Campaign.java` | `battleMode` lobby setting and the leader-gated `battlemode:<id>` command; `map:`/`switchMap` resets a mode the new map does not host; the snapshot's `battleMode` block; `returnLobby` and `switchMap` remove the marker. |
| `test-tools/java/gg/gravesandglory/Phase4Checks.java` | New Phase 4 harness and suite `modes`: `p4-modes-refuse`, `p4-modes-json`, `p4-modes-domination`, `p4-mode-marker`. |
| `BotSimulation.java`, `DuelSimulation.java` | Set their own `roundSeconds` after `startRound`, which now sets the row's clock. |
| `GravesAndGlory.java` | `Battle.mode`, created each `startRound` with `setup`; `fieldBase`/`fieldBarracks` delegate; the tick calls `mode.tick` and `mode.timeoutWinner`; income, the STATE scores and the STATE clock read the mode; `Battle.mapReady()` runs recovery and opens the camp on both the built and cached paths. |
| `Campaign.java`, `Commander.java` | Lobby, respawn, deploy and forward-supply range read `b.fieldBase`; `returnLobby` and `switchMap` call `mode.teardown`. |
| `BattleHud.java`, `WarTableHome.java` | Track maximum and the field card's rule line come from the `modes` row. |
| `test-tools/standalone/ModeMathCheck.java` | 8 checks for the pure rules. |
| `tools/verify.py`, `tools/release_config.py` | `ModeMath` joins the standalone compile; `ModeMathCheck` joins the jar-leak guard. |

## Checks

- `python tools/verify.py fast --offline` — exit 0, all 23 logged steps PASS: preflight, python tests, UI standard, UI
  assets, skin assets, java compile, 16 standalone checks (including `ModeMathCheck: 8 checks passed`), build. Report:
  `work/verify/20261029-213423-46076/result.json`. Two earlier attempts cleared the art steps and then aborted at the
  Java guard when a modelling-lane client started; the passing run began at 21:29:41 on the final tree.
- `ModeMathCheck` alone — exit 0, 8/8.
- Headless bot simulation, one match, map 0, 3v3, 120 s round, `-PsimFast=true` — exit 0. `results.csv`: dusk wins
  21 to 7 at 120 s; gold at 90 s dawn 301, dusk 767. That exercises the new scoring, timeout and income paths end to
  end. Run root: `work/mode-sim/` (results in `work/mode-sim/botsim/`).
- Real client, suite `modes`, all five ids, on a reused cached run root `work/p4-modes4/` with a marker seeded into the
  save before launch — exit 0, `BUILD SUCCESSFUL in 51s`. Startup log: `[Graves and Glory] Cached map ready.` then
  `[Graves and Glory] Recovered Rivergate Valley from an interrupted round.` Evidence `pass p4-modes-refuse`,
  `pass p4-modes-json`, `pass p4-modes-domination`, `pass p4-mode-marker`, `pass p4-modes-suite`. Reported:
  `battlemode:siege` and `battlemode:nosuch` refused with a notice and no change, `battlemode:domination` accepted;
  the lobby block read id `domination`, rule `Domination to 300`, three how-to lines matching the sheet (the two cells
  are identical for Domination), max `[300,300]`, format `[plain, plain]`; 299 to 0 with A held finished for team 0 at
  the next interval, 5 ticks after arming, score 300 to 0; 30 s at 5 to 3 finished for team 0 after 600 ticks; 30 s
  at 4 to 4 drew after 600 ticks; no point owned earned 5 gold on the 100-tick income tick. The marker check read
  `cached-ready hook: time 18000=true, point A flag back=true, file removed=true, rebuilt-map saved file deleted=true,
  working-directory file kept=true`. Log `work/p4-modes4/client.log`, evidence
  `work/p4-modes4/evidence/local-checks.txt`; the earlier runs are archived beside them (`*-run1.*`, `*-run2.*`).

## Review fixes

- Recovery ran only from `Campaign.built()`, which the cached path never reaches: a crash on an unchanged map left the
  marker unread. The ready step is now `Battle.mapReady()` (recover, then open the camp), called by the constructor when
  `campaign.cached()` and by the build path after `Arena.finish`; the constructor's 3000 time and `CampaignMaps.lobby`
  run before it, so a recovered time stands. The seeded-marker run recovered at startup on the cached path.
- `Modes.recover` deleted a saved file by a bare filename, which resolved against the working directory. It now resolves
  the name inside the marker's own folder; the check proves the folder file is deleted and a same-named working-folder
  file survives.
- `battlemode:` now takes the same `leader(p)` guard as `map:`, `mode:` and `fill`.
- `p4-mode-marker` drives `mapReady()` rather than `Modes.recover`; `p4-modes-json` compares `howToDefend` with its own
  column. The two cells hold the same three lines for Domination, so the data cannot distinguish them today.
- `p4-modes-refuse` still cannot reach the map-host branch because no second mode is registered. That branch is
  untested, not proven; it lands with Siege.

## Deliberately not in this change

- Only `domination` is registered and only it has a sheet row. `battlemode:siege` is therefore refused as an unknown
  mode, and the `<mode> is not played on <map>` branch cannot fire until a second mode registers; `p4-modes-refuse`
  asserts the refusal and no change, and the map branch lands with Siege (Task 5).
- No lobby picker for the mode: the command is the lobby setting, as the plan specifies; a client control waits for the
  modes that need one.
- The marker's `saved` list is written empty and recovery deletes the files it names; the mode that places map pieces
  (Siege) fills it in Task 5. No picker, no UI, no other mode.
