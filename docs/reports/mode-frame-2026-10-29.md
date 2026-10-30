# Mode frame (2C.1) — 2026-10-29

## Outcome

Domination now runs behind the Phase 4 mode frame. `Battle` no longer hard-codes the win score, score interval, round
length, timeout or income: they come from a `modes` sheet row through a per-round `BattleMode` object. Domination's
numbers and behaviour are unchanged; the only mode registered is `domination` and every map hosts it.

Branch `mode/frame` in `bannerbound-modes`, from `main` at `bd05fa2`. No PR, no push.

## What changed

| File | Change |
|---|---|
| `sheets/design.json` | New `modes` sheet with one `domination` row (`maps 0,1,2,3,4`, 900 s, target 300, interval 80, scales 1.0/1.0, `timeout score`, `Domination to {n}`). `rules` loses `winScore`, `scoreInterval`, `roundSeconds`. |
| `tools/preflight.py` | Validates the `modes` sheet: map indexes, timeout rule, one `{n}` at most, non-negative numbers, a scoring mode with a target, a `domination` row. Regenerates `Spec.java`. |
| `src/main/java/gg/gravesandglory/ModeMath.java` | Pure rules: `waveCount`, `ruleLine`, `howTo`, `sizeLine`, `siegeStrength`, `roles`, `ledgerText`. |
| `BattleMode.java`, `Modes.java`, `DominationMode.java` | The per-round object (setup, teardown, tick, field base and barracks, income, timeout, clock, ledger, tracks), the registry (`register`, `row`, `create`, `hosts`, `ruleLine`), and Domination's interval scoring moved into `tick`. |
| `GravesAndGlory.java` | `Battle.mode`, created each `startRound` with `setup`; `fieldBase`/`fieldBarracks` delegate; the tick calls `mode.tick` and `mode.timeoutWinner`; income, the STATE scores and the STATE clock read the mode. |
| `Campaign.java`, `Commander.java` | Lobby, respawn, deploy and forward-supply range read `b.fieldBase`; `returnLobby` and `switchMap` call `mode.teardown`. |
| `BattleHud.java`, `WarTableHome.java` | Track maximum and the field card's rule line come from the `modes` row. |
| `test-tools/standalone/ModeMathCheck.java` | 8 checks for the pure rules. |
| `tools/verify.py`, `tools/release_config.py` | `ModeMath` joins the standalone compile; `ModeMathCheck` joins the jar-leak guard. |

## Checks

- `python tools/verify.py fast --offline` — exit 0, 22/22: preflight, python tests, UI standard, UI assets, skin assets,
  java compile, 16 standalone checks (including `ModeMathCheck: 8 checks passed`), build.
- `ModeMathCheck` alone — exit 0, 8/8.
- Headless bot simulation, one match, map 0, 3v3, 120 s round, `-PsimFast=true` — exit 0. `results.csv`: dusk wins
  21 to 7 at 120 s; gold at 90 s dawn 301, dusk 767. That exercises the new scoring, timeout and income paths end to
  end. Run root: `work/mode-sim/` (results in `work/mode-sim/botsim/`).

## Deliberately not in this change

- No `battlemode:` command or lobby picker: with one mode it would be a setting with one value. It arrives with Siege.
- No mode crash marker (`Modes.mark`/`recover`): it restores map pieces a mode places, and Domination places none. It
  belongs to the first mode that does (Siege).
- No `Phase4Checks` suite and no client run: the machine's client window belongs to the modelling lane. The suite for
  `p4-modes-domination` is the next test to add when that window is free.
- `roundSeconds` is still set once at `Battle` construction (from the row) and overridden by a simulation, not reset in
  `startRound`; the mode-selection pass must set it when the mode changes.
