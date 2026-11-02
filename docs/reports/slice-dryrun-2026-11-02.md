# Six-player slice dry run, 2026-11-02 (plan task 3.7)

Branch `work/slice-dryrun` off main `7ca3221`. Not pushed. Status: IN PROGRESS (interim commit; final results below are updated as items finish).

## Method
- Harness (committed d4f5aa6): `SliceProbe` (server, writes `state-server.json`, reads `cmd-server.txt` for `finish <team>` and `stop`) and `SliceClient` (client, reads `cmd-<user>.txt` for `connect`, `disconnect`, `send <cmd>`, `quit`; writes `state-<user>.json`). Both are test-tools only and are in the forbidden list in `tools/release_config.py`.
- Launch (works): the Loom dev server always runs in `<worktree>/run-server/` and the Gradle client in `<worktree>/run/`; an init-script `workingDir` override has no effect. Put `eula.txt` (own scratch folder only), `server.properties` (port 25588, online-mode=false, flat world) in `run-server/` and `options.txt` with `onboardAccessibility:false` in `run/`. Second client: direct `java` with the same command line and `@classpath.args`, different `--username`.
- Processes started by me: 1 server (Gradle `runServer`), 2 clients (SliceAlpha, SliceBravo). Seats 3 to 6 are bots. Kort's client 29360 was not touched.

## Checklist results (interim)

| # | Item | Result |
|---|---|---|
| 1 | Domination, 2 humans + 4 bots (3v3), round to a result | PASS (forced finish at 192 s, see below) |
| 2 | Siege on Ironhold | running |
| 3 | Rewards after a completed match | PASS for Domination |
| 4 | Return to camp after the match | PASS for Domination |
| 5a | Reconnect within 2 min gets a seat back | PASS (see below) |
| 5b | Slot released after 2 min | pending |
| 6 | Mid-match crash, partial gold | pending |
| 7 | Server restart | pending |
| 8 | Persistence of profiles | pending |

### 5a Reconnect within 2 min
Bravo sent `disconnect` at match second ~60. Server: Bravo left `online`, a bot took over the seat immediately, and `held` showed a hold on Bravo's UUID until tick 5113 (about 115 s ahead). Bravo sent `connect` about 40 s later: back in the battle world, seated on team 1, `held` cleared, scores kept running. Minor observations: the seat index moved from slot 1 to slot 5 in the list, and Bravo's client showed ready=False in the War Table roster after rejoining (cosmetic).

### 1, 3, 4 Domination on Rivergate, 2 humans + 4 bots
Room created by SliceAlpha (size 3, fill on), SliceBravo joined, both ready, 10 s countdown, map built, battle started with 6 seats. The round was ended with the probe's `finish 0` at 199 s of play (the 900 s clock was not waited out), which is past the 180 s minimum. Result: Gold Banner wins. Both clients showed the result and landed in `CampScreen` ("Back to the war council"). Rewards (server profile and client profile agree):

| Player | Outcome | Gold | XP | Shards | Renown | Matches / Wins |
|---|---|---|---|---|---|---|
| SliceAlpha (winner) | VICTORY | 120 | 150 | gilded +2 | gilded +2 | 1 / 1 |
| SliceBravo (loser, had reconnected) | BATTLE COMPLETE | 80 | 100 | gilded +1 | gilded +1 | 1 / 0 |

The reconnected player was eligible for the full reward. Caveat: the round was force-ended, not played to the clock; a natural clock or score finish was not exercised.

## Defects so far
| Rating | Finding | Reproduce |
|---|---|---|
| minor | Rejoined commander reseated at the last seat index; roster ready flag shows False after rejoin | Disconnect and reconnect a commander mid-Domination, read `state-server.json` seats and the client social roster |
| minor (tooling) | Init-script `workingDir` override ignored by Loom dev server and client | see Method |

## Needs real humans (not agent-testable)
- Six real clients at once, and real latency, jitter, packet loss over Tailscale.
- Online-mode accounts, whitelist and the kit's live `start.ps1` on `tjepkema-server`.
- Feel checks: reward screens, return-to-camp transitions, map-load frame spikes, audio, readability in a real match.
- Friends reconnecting from a real client crash or lost connection, and a real server crash (power or process loss) rather than a killed dev process.
