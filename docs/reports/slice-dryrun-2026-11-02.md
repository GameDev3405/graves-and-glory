# Six-player slice dry run, 2026-11-02 (plan task 3.7)

Branch `work/slice-dryrun` off main `7ca3221`. Not pushed. Status: FINAL, wrapped up early at the coordinator's instruction. Items not listed as PASS were not run to a result.

## Method
- Harness (committed d4f5aa6): `SliceProbe` (server, writes `state-server.json`, reads `cmd-server.txt` for `finish <team>` and `stop`) and `SliceClient` (client, reads `cmd-<user>.txt` for `connect`, `disconnect`, `send <cmd>`, `quit`; writes `state-<user>.json`). Both are test-tools only and are in the forbidden list in `tools/release_config.py`.
- Launch (works): the Loom dev server always runs in `<worktree>/run-server/` and the Gradle client in `<worktree>/run/`; an init-script `workingDir` override has no effect. Put `eula.txt` (own scratch folder only), `server.properties` (port 25588, online-mode=false, flat world) in `run-server/` and `options.txt` with `onboardAccessibility:false` in `run/`. Second client: direct `java` with the same command line and `@classpath.args`, different `--username`.
- Processes started by me: 1 server (Gradle `runServer`), 2 clients (SliceAlpha, SliceBravo). Seats 3 to 6 are bots. Kort's client 29360 was not touched.

## Checklist results (interim)

| # | Item | Result |
|---|---|---|
| 1 | Domination, 2 humans + 4 bots (3v3), round to a result | PASS (forced finish at 192 s, see below) |
| 2 | Siege on Ironhold | PARTIAL: lobby, map build and battle start with 6 seats pass (map 1, 185 s round); stopped at about 30 s of play, no result, no rewards observed |
| 3 | Rewards after a completed match | PASS for Domination |
| 4 | Return to camp after the match | PASS for Domination |
| 5a | Reconnect within 2 min gets a seat back | PASS (see below) |
| 5b | Slot released after 2 min | not run to a result: Bravo disconnected in the Siege round, hold set (release tick 9353), stopped before expiry |
| 6 | Mid-match crash, partial gold | not run |
| 7 | Server restart | not run |
| 8 | Persistence of profiles | not run |

### 5a Reconnect within 2 min
Bravo sent `disconnect` at match second ~60. Server: Bravo left `online`, a bot took over the seat immediately, and `held` showed a hold on Bravo's UUID until tick 5113 (about 115 s ahead). Bravo sent `connect` about 40 s later: back in the battle world, seated on team 1, `held` cleared, scores kept running. Minor observations: the seat index moved from slot 1 to slot 5 in the list, and Bravo's client showed ready=False in the War Table roster after rejoining (cosmetic).

### 1, 3, 4 Domination on Rivergate, 2 humans + 4 bots
Room created by SliceAlpha (size 3, fill on), SliceBravo joined, both ready, 10 s countdown, map built, battle started with 6 seats. The round was ended with the probe's `finish 0` at 199 s of play (the 900 s clock was not waited out), which is past the 180 s minimum. Result: Gold Banner wins. Both clients showed the result and landed in `CampScreen` ("Back to the war council"). Rewards (server profile and client profile agree):

| Player | Outcome | Gold | XP | Shards | Renown | Matches / Wins |
|---|---|---|---|---|---|---|
| SliceAlpha (winner) | VICTORY | 120 | 150 | gilded +2 | gilded +2 | 1 / 1 |
| SliceBravo (loser, had reconnected) | BATTLE COMPLETE | 80 | 100 | gilded +1 | gilded +1 | 1 / 0 |

The reconnected player was eligible for the full reward. Caveat: the round was force-ended, not played to the clock; a natural clock or score finish was not exercised.

### Counts
PASS: 4 (Domination round, Domination rewards, Domination return to camp, reconnect within 2 min). PARTIAL: 1 (Siege start). NOT RUN: 4 (hold expiry, crash, server restart, persistence). FAIL: 0. Blockers: none found. Note: rewards were read from the live server profiles; no restart was done to prove they were saved to disk.

## Defects so far
| Rating | Finding | Reproduce |
|---|---|---|
| minor | Rejoined commander reseated at the last seat index; roster ready flag shows False after rejoin | Disconnect and reconnect a commander mid-Domination, read `state-server.json` seats and the client social roster |
| minor | Server log shows repeated 'moved too quickly' warnings for both humans at battle start on Ironhold and Rivergate (teleport into the map) | Start any room, read the server log at battle start |
| minor (tooling) | Init-script `workingDir` override ignored by Loom dev server and client | see Method |

## Needs real humans (not agent-testable)
- Six real clients at once, and real latency, jitter, packet loss over Tailscale.
- Online-mode accounts, whitelist and the kit's live `start.ps1` on `tjepkema-server`.
- Feel checks: reward screens, return-to-camp transitions, map-load frame spikes, audio, readability in a real match.
- Friends reconnecting from a real client crash or lost connection, and a real server crash (power or process loss) rather than a killed dev process.

## Round 2 (branch `work/slice-dryrun-2` off main `65f2bcc`, agent run, not pushed)

Same launch setup as round 1 (server in `<worktree>/run-server/`, Gradle client in `<worktree>/run/`, second client by direct `java` with a copy of the Loom classpath args file). Scratch scripts and probe output are outside the repo. Processes: 1 server, 2 clients (SliceAlpha, SliceBravo).

| # | Item | Round 1 | Round 2 |
|---|---|---|---|
| 1 | Siege on Ironhold, full round, rewards | PARTIAL | PASS: see below |

### R2-1 Siege on Ironhold, natural finish
Room by SliceAlpha (size 3, map 1, mode siege, fill on), SliceBravo joined, both ready, 10 s countdown, battle with 6 seats (2 humans, 4 bots). Not force-finished: the round ran past its 185 s clock (round length later read 545 s) and ended on its own at 371 s of play with score 3 to 0. Gold Banner (Alpha, team 0) won. Both clients landed on `CampScreen` ("Gold Banner wins! Back to the war council."), result saved and eligible. Server and client profiles agree:

| Player | Outcome | Gold | XP | Shards | Renown | Matches / Wins |
|---|---|---|---|---|---|---|
| SliceAlpha (winner) | VICTORY | 120 | 150 | gilded +2 | gilded +2 | 1 / 1 |
| SliceBravo (loser) | BATTLE COMPLETE | 80 | 100 | gilded +1 | gilded +1 | 1 / 0 |

Observation (minor, unclear if intended): the probe's `roundSeconds` read 185 at battle start and 545 by second 329, so the siege round length grows during play (probably an extension when a point is taken); the 185 s figure in round 1 was only the initial value.
