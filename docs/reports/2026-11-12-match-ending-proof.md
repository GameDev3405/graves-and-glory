# Match ending proof index, 2026-11-12

Task 7 of `docs/superpowers/plans/2026-11-12-match-ending.md`. Branch `ui/match-ending`. Evidence lives under `work/ingame-check/` (git-ignored); job ids are checkq ids. Every game run used the real sequence timings (RESULT 6 s, PODIUM 8 s, STATS 10 s, REWARDS 6 s). Note: the checkq status says "passed (exit 0)" even when checks fail, so the verdicts below are read from each root's `evidence/local-checks.txt`.

Worker note: the brief named Opus 5.5; this task ran on Sonnet 5.5, and the commit trailer says so.

## Verdicts

| Check | Job or report | Result |
|---|---|---|
| ending-modes, final run | 20261112-173647-457-0ccc | pass: 103 lines (102 checks plus `p4-ending-modes-suite`), 0 fail |
| ending-modes, first run | 20261112-172315-460-b448 | 2 fail (test artefact, below), fixed |
| ending-sync, final run | 20261112-174804-035-3473 | pass: all 14 lines, 0 fail |
| ending-sync, attempt 1 | 20261112-172318-965-0830 | fail: the JVM gave no `ProcessHandle.commandLine()` |
| ending-sync, attempt 2 | 20261112-173647-800-b81e | fail: spawn required `--gameDir`, which Loom does not pass |
| ui-report (Phase 1 suite) | 20261112-175339-112-52a4 | pass: 49 lines, 0 fail (needs `--env GRAVES_PHASE1_CHECKS=ui-report`; the first submission 20261112-175127-544-99cd without that env reported `unknown suite` and tested nothing) |
| verify fast --offline | `work/verify/20261112-175517-27320/result.json` | pass, 70 PASS lines; log `work/ingame-check/verify-fast.txt` |
| verify game --offline | `work/verify/20261112-175840-32980/result.json` | pass, 71 PASS lines including `game: PASS`; log `work/ingame-check/verify-game.txt` |

test_where did not flake in either verify run, so no solo rerun was needed.

## ending-modes (`EndingChecks.ModesSuite`)

Submitted with `GRAVES_INSTANT_ENDING=false`. Twelve rounds, one room round each, 200 ticks of play before the end, then the end is forced as win, loss or draw (Survival has win and loss only), plus a thirteenth, natural end. Map point counts are read from the map, not assumed (every shipped map reported 3 here).

| Round | Map | Outcome | Report `line` |
|---|---|---|---|
| domination-win / -loss / -draw | Rivergate Valley | VICTORY / DEFEAT / DRAW | Gold Banner 0 / Violet Banner 0 · Rivergate Valley · 3:01 |
| domination-natural | Rivergate Valley | VICTORY | Gold Banner 2 / Violet Banner 0 · Rivergate Valley · 3:05 |
| siege-win / -loss / -draw | Ironhold Siege | VICTORY / DEFEAT / DRAW | Stages taken 0/3 · Ironhold Siege · 3:01 |
| convoy-win / -loss / -draw | Frostmarch Pass | VICTORY / DEFEAT / DRAW | Road covered 0% · Frostmarch Pass · 3:01 |
| survival-win / -loss | Hollowwood Reach | VICTORY / DEFEAT | Wave 1/7 · Hollowwood Reach · 3:01 |

Per round the suite asserts: round started, ran 200 ticks, ending begins, report `outcome` and `line` as above, the freeze (armies and the clock stop), the server step order RESULT, PODIUM, STATS, REWARDS, DONE (`ending-modes-<tag>-steps`), the client saw the same steps (`-client-steps`) and drew the result banner (`-client-banner`). The full data is in `work/ingame-check/ending-modes/evidence/ending-modes.json`.

Natural end: `winTarget` is lowered to 2 through `SheetOverride`, point 0 is held for the player's team, the mode finishes on its own score (line shows 2 / 0), and the value is restored afterwards.

First-run failures (job 172315-460-b448): `domination-draw-client-steps` and `siege-loss-client-steps`. Cause: the test skipped STATS or REWARDS after 8 ticks, shorter than the half-second snapshot cadence, so the client never saw one step. The harness now dwells 30 ticks before skipping. No game code changed.

Screenshots (all under `work/ingame-check/ending-modes/run/screenshots/`): `p4-ending-modes-<tag>-result.png` (RESULT banner, 18 ticks in) and `p4-ending-modes-<tag>-podium.png` (36 ticks into PODIUM) for each of the 12 tags: `domination-win|loss|draw|natural`, `siege-win|loss|draw`, `convoy-win|loss|draw`, `survival-win|loss` (24 files). Spot-checked by eye: the natural-end banner reads VICTORY with the right line; the siege-draw podium shows three stands with names, scores, kept counts and the MVP tag.

## Clip

`work/ingame-check/ending-sequence-clip.mp4`: 120 frames, one every 5 client ticks (4 fps equals real time), through a whole domination-win sequence (600 ticks). Source frames `work/ingame-check/ending-modes/run/screenshots/p4-ending-clip-0000.png` to `-0119.png`, joined with ffmpeg (libx264, yuv420p).

## ending-sync (`EndingChecks.SyncSuite`, `EndingJoin`)

Two real clients: the host opens the world to LAN and writes the address and room code; a second JVM (`EndJoiner`, cloned from the host's launch with its own game directory) connects, joins the room and readies; the host starts a round and finishes it after 100 ticks. Both clients log each step change with the client tick and the wall-clock ms.

Result of job 174804-035-3473 (from `work/ingame-check/ending-sync/evidence/ending-sync-host-log.txt` and `ending-sync-join-log.txt`):

| Step | Host tick | Visitor tick | Gap |
|---|---|---|---|
| RESULT | 1621 | 1622 | 1 tick, 14 ms |
| PODIUM | 1741 | 1741 | 0 ticks, 24 ms |
| STATS | 1901 | 1902 | 1 tick, 7 ms |
| REWARDS | 2101 | 1917 | visitor had skipped on, not compared |

The limit was 10 ticks; the worst gap was 1. The visitor skipped STATS and REWARDS and reached DONE at +311 ticks after the finish, while the host was still in STATS (it ran RESULT@1, PODIUM@120, STATS@280, REWARDS@480, DONE@600). At the visitor's DONE it was still in the room, and the `CampScreen` that opened was the lobby page (after 7 ticks), not home. The host sequence was unchanged by the visitor's skip.

## Not proven

- REWARDS is not compared between the two clients, because the visitor skips it. Only RESULT, PODIUM and STATS are sync-checked.
- Loss and draw rounds end by a forced finish with a chosen winner, not by playing the match out; only the Domination natural round ends on its own rules. The 200 ticks of play are mostly idle bots, so stats shown are near-empty (the Wave 1/7, Road 0% and Stages 0/3 lines show it).
- Rewards eligibility uses an elapsed-time fixture, not a real match length.
- The tick gap uses the client's world time, and the ms column is wall clock; two clients on one machine share a CPU, so this is not a network-latency measure.
- Sync was run once with one visitor; no three-client or late-join-during-ending case.
- The clip is one domination win; no clip of the loss, draw or two-client lane. The two-client lane has logs but no screenshots of the visitor window.
- The suite verdict is based on harness checks plus a few eyeballed screenshots; the other 22 result and podium screenshots were not individually viewed.
- No real bug was found in `src/main`; no gameplay code was edited. Harness changes only: `EndingChecks.java`, new `EndingJoin.java`, `Phase4Checks.java` (suite registration), `fabric.mod.json` (test-tools entrypoint), `tools/release_config.py` (keeps `EndingJoin` out of release jars).
