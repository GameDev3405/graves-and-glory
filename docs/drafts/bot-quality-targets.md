# DRAFT: bot quality targets for 1.0 (for Kort to edit)

Status: DRAFT, 2026-11-01. Numbers are proposals, not measured results.

Tooling: `tools/balance_sim.sh` (headless bot rounds, writes `botsim/results.csv`), `tools/balance_gate.py` (two seat-swapped runs, pass band 40 to 60), `tools/mode_gate.py <csv> <mode>` (siege checker only today). results.csv columns today: match, map, dawnTier, duskTier, winner, dawnScore, duskScore, seconds, banner0, banner1, dawnKills, duskKills, dawnWiped, duskWiped, dawnRetreats, duskRetreats, fights, meanFightSeconds, flank hits and shots, score300, mode, stage, gateBlows, gateDamage, yardSecond, gateSecond, keepSecond.

Each cell: at least 40 rounds, seat-swapped, rounds with winner=aborted left out.

## Win-rate bands (equal tier and banner, seat-swapped)
| Mode | Target | Measure |
|---|---|---|
| Domination | 40 to 60 percent per map and per House pair | `balance_gate.py` on two runs; draws count half |
| Siege | Attackers win 30 to 70 percent; no match stuck at stage 0 | `mode_gate.py <csv> siege` (exists) |
| Survival | Bots clear the Normal wave set in 50 to 80 percent of rounds | winner column plus a new survival checker in `mode_gate.py` |
| Banner | Raiding side wins 40 to 60 percent; at least 1 capture per 3 matches | winner column plus a new capture column |
| Difficulty ladder | Hard beats Normal 65 to 85 percent; Normal beats Easy 60 to 80 percent | mixed `-PsimDawn` and `-PsimDusk` tiers, win rate by `dawnTier` vs `duskTier` |

## Behaviour targets
| Target | Number | Measure |
|---|---|---|
| Max idle time | No army idle (no move, fight or objective progress) over 20 s; mean idle under 5 percent of round time | NEW `idleSeconds` column in `BotSimulation`; proxy today: at least 1.5 `fights` per 5 minutes |
| Objective focus | At least 70 percent of army time within 12 blocks of a capture point, banner or gate target | NEW `objectiveShare` column; proxy today: `score300` above 25 percent of available points, and `gateBlows` above 0 once `stage` is at least 1 (exists) |
| Formation hold | Group members within 6 blocks of the group centre at least 85 percent of the time before first contact | NEW `formationHold` column, sampled each second; no proxy today |
| Retreat sanity | At most 2 retreats per army per round | `dawnRetreats`, `duskRetreats` (exist) |
| Flank use | At least one flank hit in 50 percent of rounds on open maps | `dawnFlankHits`, `duskFlankHits` (exist) |
| Round length | Median at most 90 percent of the round cap; at most 5 percent hit the cap | `seconds` (exists) |
| Aborts | 0 per 40 rounds | `aborted=N` on the summary line (exists) |
| Path failures | 0 bots stuck over 30 s per map | map bot-path check (plan item 4A.1) plus the idle column once added |

## Not built yet
Three columns (`idleSeconds`, `objectiveShare`, `formationHold`) and survival and banner checkers in `tools/mode_gate.py`. That is test-tool work in `test-tools/java/gg/gravesandglory/BotSimulation.java`, but tuning bot behaviour to hit the numbers needs Kort's approval.
