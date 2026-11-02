# Six-player slice dry run, 2026-11-02 (plan task 3.7)

Branch `work/slice-dryrun` off main `7ca3221`. Not pushed. **Result: the dry run did not reach any checklist item.** The harness was built and compiled, but both launch attempts failed on a setup problem (below) and the run was stopped at the coordinator's instruction. Every checklist item is therefore "not run". No gameplay defect was observed or recorded.

## What was built (committed, d4f5aa6)
- `test-tools/java/gg/gravesandglory/SliceProbe.java` (server entrypoint, `-Dgravesandglory.sliceDir=<dir>`): writes `state-server.json` once a second (pid, phase, mode, map, round time, scores, seats with bot and seated flags, held slots, every loaded profile's gold, matches, wins, XP, shards, Renown). Polls `cmd-server.txt` for `finish <team>` (end the round) and `stop` (clean server stop). Read-only otherwise.
- `test-tools/java/gg/gravesandglory/SliceClient.java` (client entrypoint, `-Dgravesandglory.sliceClient=host:port -Dgravesandglory.sliceDir=<dir>`): a remote-controlled harness client. It takes `connect`, `disconnect`, `send <social or order command>` and `quit` from `cmd-<user>.txt` and writes `state-<user>.json` (connection, camp or battle phase, War Table room, profile summary, last result report).
- Both are registered in `test-tools/resources/fabric.mod.json` and added to the forbidden list in `tools/release_config.py`. `gradlew localTestToolsJar --offline` builds. No shipped code changed.
- Uncommitted, gitignored scratch (under `work/`): init scripts, `ctl.py` (state summary and command writer), start scripts. They follow the 3.6 method (Loom `runServer` / `runClient` with `-Dfabric.addMods` pointing at the local-integration jar).

## Checklist results

| # | Item | Result |
|---|---|---|
| 1 | Domination, 2 humans + 4 bots (3v3), full round to a result | not run |
| 2 | Siege on Ironhold, full round | not run |
| 3 | Rewards: gold, win bonus, Renown, shards, XP after a completed match; profile saved | not run |
| 4 | Return to camp after the match (both modes) | not run |
| 5 | Reconnect within 2 min gets the army back; slot released after 2 min | not run |
| 6 | Mid-match crash (kill own client or server): partial gold pro-rated by time played | not run |
| 7 | Server restart: lobby, parties and profiles intact | not run |
| 8 | Persistence of rewards across reconnect and restart | not run |

Counts: 0 pass, 0 fail, 8 not run.

## What happened
1. Server launch (`runServer` with an init script setting `workingDir` to `work/slice-srv/run`): Gradle ran the server in the default `run-server` folder instead, found no `server.properties`/EULA there and exited ("You need to agree to the EULA"), so the probe never started. The same init-script approach was recorded as working in the 3.6 report; the cause here was not found (a debug print was queued but the shell guard refused the command before it ran).
2. Client launch (same init-script method): the client also appears to have run in the default `run/` folder, not `work/slice-cli1`, so my `options.txt` was ignored and it stopped on `AccessibilityOnboardingScreen`. `SliceClient` loaded and wrote its state file, which is the only run evidence (`work/slice-out/client-SliceAlpha.log`). I asked it to quit through the harness; it exited cleanly.
3. Processes: I started one Gradle client tree (wrapper 65304, daemon 51256, client 49628). All ended on their own after the harness `quit`. A direct `Stop-Process` was refused by the permission system and was not needed. Kort's review client (29360) and the other Java processes already present (4580, 6048) were not touched. No server of mine is running. Nothing was killed, so the crash steps (6) were never attempted.

## Defect list
No game defects found (nothing was exercised). Tooling findings:

| Rating | Finding | Reproduce |
|---|---|---|
| minor | Init-script `workingDir` override did not apply for `runServer` and `runClient` in this checkout, so scratch run folders (EULA, options, properties) are ignored | `gradlew runServer --offline -Dslice.root=<root> -I work/slice-init-server.gradle` (script in `work/`, gitignored) and watch the working folder |
| minor | The agent shell guard refuses compound commands that mention git or another worktree path, which slowed scripted setup | n/a, process note |

## Next steps for whoever reruns this
- Fix the launch first: run the server and clients with an explicit `java` command line (the 3.6 run kept `cmdline.txt` and `classpath.args` per client and reused them), or set `workingDir` in a small repo-local Gradle property, then confirm the server log shows `Done` and `state-server.json` appears.
- Drive the lobby with `SliceClient` commands: `send social:lobby`, `social:size:3`, `social:map:1`, `social:mode:siege`, `social:ready`; Domination first (any map, 300 s clock), then Siege (Ironhold only, about 320 to 370 s in bot runs). The 3-minute minimum play for rewards (`minimumRewardSeconds`) means a forced early `finish` pays nothing; let rounds run to the clock.
- Crash test: kill only the server or client PID that the probe or client state file reports. After restart, the server log should show "interrupted by a crash" and pay `partialGold` from the 30-second checkpoint (`match-progress.json`).
- Reconnect: `disconnect`, wait under 120 s, `connect`; then repeat past 120 s and expect "slot ... is released" in the server log.

## Needs real humans (not agent-testable)
- Six real clients at once, and real latency, jitter, packet loss over Tailscale.
- Online-mode accounts, whitelist and the kit's live `start.ps1` on `tjepkema-server`.
- Feel checks: reward screens, return-to-camp transitions, map-load frame spikes, audio, readability in a real match.
- Friends reconnecting from a real client crash or lost connection, and a real server crash (power or process loss) rather than a killed dev process.
