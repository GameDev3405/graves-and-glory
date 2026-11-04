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
| 2 | Reconnect hold expires after 2 min | not run | PASS (release at exactly 2400 ticks); 1 major, 1 minor defect |
| 3 | Mid-match client crash, partial gold | not run | FAIL: crashed player got 0 gold (partial pay exists only for server stop or crash) |
| 4 | Server restart: lobby, parties, profiles | not run | PASS parties and profiles; open room not kept (minor) |
| 5 | Rewards persist across reconnect and restart | not run | PASS |

### R2-1 Siege on Ironhold, natural finish
Room by SliceAlpha (size 3, map 1, mode siege, fill on), SliceBravo joined, both ready, 10 s countdown, battle with 6 seats (2 humans, 4 bots). Not force-finished: the round ran past its 185 s clock (round length later read 545 s) and ended on its own at 371 s of play with score 3 to 0. Gold Banner (Alpha, team 0) won. Both clients landed on `CampScreen` ("Gold Banner wins! Back to the war council."), result saved and eligible. Server and client profiles agree:

| Player | Outcome | Gold | XP | Shards | Renown | Matches / Wins |
|---|---|---|---|---|---|---|
| SliceAlpha (winner) | VICTORY | 120 | 150 | gilded +2 | gilded +2 | 1 / 1 |
| SliceBravo (loser) | BATTLE COMPLETE | 80 | 100 | gilded +1 | gilded +1 | 1 / 0 |

Observation (minor, unclear if intended): the probe's `roundSeconds` read 185 at battle start and 545 by second 329, so the siege round length grows during play (probably an extension when a point is taken); the 185 s figure in round 1 was only the initial value.

### R2-2 Reconnect hold expiring after 2 min (Domination, Rivergate)
SliceBravo sent a clean `disconnect` about 20 s into the round. Server: a bot took the seat at once and `held` showed Bravo's UUID with release tick 15390; at 120.0 s after the disconnect (2400 ticks) `held` became empty, matching the 2 min rule. PASS. After the release Bravo reconnected (at 183 s of play): he was put back into the battle world and given a seat again, which means a released slot does not lock a player out.

Defects found here:
- major: the post-expiry rejoiner was seated on team 0 (the opposite side) while the lobby roster still lists him on team 1. Server seats before: Alpha 0, bot 0, bot 0, bot 1 x3; after: Alpha 0, bot 0, bot 1 x3, Bravo 0. Within the 2 min hold he gets his own team back (round 1 5a). Reproduce: disconnect past 2 min, reconnect during the same round, read `seats` in `state-server.json` against the social roster.
- minor: that player was credited the full winner reward (120 gold, 150 XP, 2 shards) although he was away for about 160 of 213 s. Round ended with the probe's `finish 0` at 213 s (past the 180 s minimum), so a natural finish was not exercised in this round.

### R2-3 Mid-match client crash (Domination, Rivergate)
SliceBravo's own client process was force-killed (`Stop-Process -Force`, no clean disconnect) at 46 s of play. Server: a bot took the seat at once and a 2 min hold was set, same as a clean disconnect. Alpha played on; the round was ended with `finish 0` at 193 s (Bravo never returned; the hold was still open at tick 20798). Result: Alpha VICTORY, 120 gold. **Bravo received nothing**: his server profile stayed at 200 gold, 2 matches, 1 win (no partial gold, no match counted). FAIL against the expectation "partial gold pro-rated by time played".

Reading `Campaign.java` (no change made): `MatchRules.partialGold` pro-rating is only wired to an interrupted match (server stop, or a server crash through the progress checkpoint), not to a player who drops out of a match that finishes normally. That reading explains the result; whether it is intended is a design call.

Defect: major. A player whose client crashes and who does not return in the same round gets no gold, XP, shards or match count for about 46 s played. Reproduce: two humans in a Domination match, force-kill one client at about 45 s, let the other human win or finish the round, read the dropped player's profile in `state-server.json`.

### R2-4 Server restart (clean stop, then start, same world folder)
Before: Alpha and Bravo in a party (Alpha leader), Alpha's open room (siege, map 1, size 3) with Bravo joined, profiles Alpha 360 gold / 3 matches / 3 wins / 450 XP / gilded 6, Bravo 200 gold / 2 matches / 1 win / 250 XP / gilded 3. The probe `stop` ended the server cleanly; both clients dropped to `DisconnectedScreen`. After a fresh start and `connect` from both clients:
- Party: intact (same leader, both members). `gravesandglory-parties.json` on disk. PASS.
- Profiles: identical gold, matches, wins, XP, shards and Renown on server and client. PASS (see item 5).
- The open room: gone. After the restart both clients have `room=""` and the lobby settings are back to defaults (domination, map 0, size 2). Minor, likely by design since rooms are in memory, but the brief expected the lobby to stay.
Verdict: PASS for parties and profiles, with the room loss recorded as a minor defect.

### R2-5 Rewards persist across reconnect and restart
Reconnect: Bravo's profile after the 2 min hold reconnect and again after the force-killed client was relaunched matched the server (200 gold). Alpha's rewards (120 gold per win, +2 shards) survived the restart exactly (360 gold, 450 XP, gilded 6 on disk, server and client). Profile files are in `run-server/Graves and Glory/gravesandglory-test-profiles/` with a `.bak` copy of the previous save. PASS.

### R2-extra Domination round to a natural finish
Alpha, Bravo (party, both seated on team 0 with one bot) against 3 bots on Rivergate. No forced finish: team 1 reached the 300 score limit at 493 s of play (scores 11 to 300), well before the 900 s clock, so the clock end itself was still not exercised. Violet Banner won; both clients got BATTLE COMPLETE, 80 gold, 100 XP, gilded +1 and landed on `CampScreen`. Server and client profiles agree (Alpha 440 gold / 4 matches, Bravo 280 gold / 3 matches). PASS for a natural score finish. Note: two humans in a party were put on the same team, so a party always fights together (expected, not a defect).

### Round 2 counts
PASS: 1 (Siege), 2 (hold expiry), 4 (parties, profiles), 5 (persistence), plus the natural Domination score finish. FAIL: 3 (client crash pays nothing). Blockers: none. Majors: 2 (crash pays 0; post-expiry rejoin seats the wrong team). Minors: room lost on restart, full reward for a player absent 75 percent of the round, siege round length grows during play. Not run: the server-crash checkpoint path of partial gold (a hard kill of the server mid-match). All processes stopped at the end; PIDs 29360, 4580, 6048 untouched.
