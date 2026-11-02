# Six-player test plan (SLICE-1, SLICE-03)

2026-10-28. Approved as the Phase 1 baseline method; not scheduled or run. Kort skipped the 3v3 baseline on 2026-10-28 ("skip the 6 player game") and directed work to continue on the roadmap. Baseline first, then acceptance. Sources read: `docs/WORKFLOW.md`, `docs/SETUP.md`, `docs/SLICE-1-CHECKPOINT.md`, `docs/DECISIONS.md` (SLICE-1 scoping answers, Excalibur), `docs/BUILDER-SERVER.md`, `docs/ROADMAP-v2.md` (M6), `tools/builder-server/start-build-server.ps1`, `tools/export_server.py`. [A] = assumption. No server was run and nothing was deployed.

## 0. Scheduling status

Kort skipped the six-player baseline on 2026-10-28 and directed work to continue on the roadmap. No date is held. The confirmation checklist and invite draft below are retained for when the session is rescheduled.

| Field | Value |
|---|---|
| Status | Skipped by Kort on 2026-10-28; no date held |
| Server | tjepkema-server, `C:\GGMatch`, port 25580 |
| Players | Kort plus five friends |

Confirmation checklist:
- [ ] Five friends confirm the two-hour window.
- [ ] Candidate jar and friends' pack are built and recorded.
- [ ] Tailscale reachability checked from each machine.
- [ ] Whitelist has all six Minecraft names.
- [ ] `C:\GGMatch` is deployed on port 25580; builder on 25570 untouched.

Invite draft (update the date and time before sending):

> Graves and Glory 3v3 baseline - [date and time to be confirmed]. I need five players for a two-hour test: lobby, three matches, rewards, one reconnect and a restart. Please reply if you can make it; I will send the pack and Tailscale details once we have five.

## 1. Scope and rules
- 3v3 humans on two teams, six real clients. Bots fill empty seats only. Headless bots never count as players (WORKFLOW.md, DECISIONS.md).
- Clients: Kort's PC plus friends' machines, all joining tjepkema-server (Tailscale, Windows 11, Java 17, 100.72.202.38).
- Working targets approved 2026-10-28: 60 fps on each client and server MSPT p95 under 50 ms; confirm after the Phase 3 baseline.
- Never touch ports 25565-25567, 25571 (Kingdom) or 25570 (builder). Match server uses **25580** [A, any free port].
- Never stop Kort's game or the builder server. `tools/verify.py game` is only a one-client smoke check and does not count here (SETUP.md).
- Version stays 0.5.x. Friends' build may use stopgap tier art; the strict package gate applies only to public release.

## 2. Prerequisites
| Item | Owner | Detail |
|---|---|---|
| Candidate jar | Claude | Built from a recorded commit; `tools/verify.py fast --offline` passes first. Record jar SHA-256 and the identity in `releases/current-candidate.json`. |
| Modpack for friends | Kort | Same candidate jar, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Minecraft 1.20.1, Java 17. [A] Install by importing a pack zip made with `tools/package.py`, not hand-copied jars. |
| Excalibur | Kort | Bundled unmodified (CC BY-NC-ND 3.0); credit Maffhew in the pack notes. Compare file hash with the original. |
| Tailscale | Kort + each friend | Friend's machine reaches the tailnet; `ping 100.72.202.38` works. |
| Whitelist | Kort | All six Minecraft names in the match server's `whitelist.json`; ops for Kort only. |
| Hardware notes | each person | CPU, GPU, RAM, Minecraft heap, resolution, wired or wifi, recorded in the run log. |

## 3. Deploying a candidate (builder untouched)
The builder lives in `C:\GGBuild` (task `GGBuilderServer`, port 25570). Leave it running.
1. Create a separate folder `C:\GGMatch`. Copy the Fabric launcher, `mods\` (candidate jar plus the server dependencies `tools/export_server.py` lists), `server.properties`. Kort supplies `eula.txt` himself.
2. Set `server-port=25580`, `white-list=true`, `max-players=6` [A], `level-name=gg-match`. Never reuse a `gg-build-*` save. Do not pass `-Dgg.build` (that mode creates no Battle).
3. [A] Add a firewall rule for 25580, Tailscale only, mirroring the builder rule.
4. Start `start-server.ps1` (from the export kit) in a visible window, not a boot task. Heap 6 GB [A], since the builder uses 6 GB on the same host.
5. Before each upgrade: `stop` in console, zip `gg-match`, keep the old jar under a unique name, then swap the jar.
6. Check from Kort's PC: `Test-NetConnection 100.72.202.38 -Port 25580`.

## 4. Run order
1. **Lobby:** six clients join, choose sides 3v3, House and class. Empty seats show bots, clearly marked.
2. **Match:** standard capture scenario on the slice battle map, full length, with orders, captures, deaths, spawns.
3. **Rewards:** end screen; rewards granted once and saved.
4. **Return:** all go back to the lobby/camp and start a second match without a restart.
5. Three matches. Match 1 is the baseline (no targets applied); matches 2-3 check against the agreed targets.

## 5. Measurements and logging
| Metric | How | Where |
|---|---|---|
| Client fps | F3 min/avg per phase (lobby, mid-fight, peak fight), screenshot each minute | player log sheet |
| Client memory | F3 memory line at start, mid, end | player log sheet |
| Ping | tab list/F3 at start and peak | player log sheet |
| Server tick | `/tick query` [A] or spark [A, not in pack]; MSPT avg and p95 per phase | `C:\GGMatch\logs`, copied |
| Server memory/CPU | [A] script sampling `Get-Process java` every 30 s | run folder |
| Bandwidth | [A] Tailscale/adapter byte counters before and after, MB per minute per phase | run folder |
| Errors | server `latest.log`, `crash-reports`, client `latest.log` | run folder |

Run folder `work/six-player/<date>-<jar-sha8>/`: `run.md` (who, hardware, times), logs named by player, samples. Do not commit bulk logs; summarize in docs only.

## 6. Reconnect and restart tests
- **Reconnect:** mid-match one player quits and rejoins within 2 minutes (the slot is then released); another drops hard (kill client). Expect same side, House, class; no duplicate unit or reward; no server error.
- **Seats:** [A] a leaving player's seat goes to a bot; a rejoin reclaims it.
- **Restart:** clean `stop` in lobby, and mid-match. [A] a mid-match restart may end the match; lobby/profile state must survive. Any other outcome needs Kort's call.
- **Crash:** kill only the `C:\GGMatch` java process, never the builder's; check world integrity after restart.

## 7. Persistence checks
After match 3 and after every restart compare with pre-restart screenshots/files: House and class choice, rewards, profile and unlocks, map state. The world loads without regenerating, and a backup exists from step 3.5.

## 8. Defect report template
```
ID: D-<nn>  Reporter:  Time:  Jar SHA8:
Phase: lobby | match | rewards | return | reconnect | restart
Severity: blocker | major | minor | cosmetic
Did / expected / happened:
Repro: always | sometimes | once   Players affected:
Evidence: screenshot / log path / clip
Hardware: CPU / GPU / RAM / wifi or wired
```
Blocker = crash, desync, lost rewards, unplayable. Claude triages into BACKLOG; at most two automatic fix rounds (WORKFLOW.md).

## 9. Pass / fail
| # | Check | Pass |
|---|---|---|
| 1 | Six humans in one match | all stay connected to the end; no bot counted |
| 2 | Loop | lobby, match, rewards, return, second match |
| 3 | Client fps | at or above the agreed target on every machine |
| 4 | Server tick | MSPT p95 under 50 ms (working target, confirm after Phase 3) |
| 5 | Reconnect | side, House, class restored; no duplicates |
| 6 | Restart | lobby restart keeps persistence; mid-match as decided |
| 7 | Persistence | all section 7 fields intact |
| 8 | Isolation | builder up on 25570; 25565-25567/25571 untouched |
| 9 | Defects | no open blocker or major |
| 10 | Visual | Kort's look at threshold 9, Excalibur on and off |

## 10. Who does what
- **Kort:** schedule about 2 hours [A]; run the server; supply `eula.txt`; whitelist; gather friends' logs; final visual judgment.
- **Each friend:** install the pack, join Tailscale, confirm ping, fill the log sheet, file defects with the template, send `latest.log`.
- **Claude:** build and record the candidate; write deploy and sampling scripts under `tools/` for Kort's review; analyze logs; triage defects; update BACKLOG and HANDOFF.
