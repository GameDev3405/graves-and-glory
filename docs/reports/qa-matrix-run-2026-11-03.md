# QA matrix run, 2026-11-03 (plan 5.6)

Branch `work/qa-matrix-runs`, base `main` bd05fa2b. Tester: Sonnet worker. Per-row states are in the "Run 2026-11-03" column of `docs/QA-MATRIX.md`. Evidence files (copies of the harness `local-checks.txt` and the verify result) are in `docs/reports/qa-matrix-run-2026-11-03-evidence/`; full logs are under the gitignored `work/q3/`.

## Totals (137 rows)
Updated after the D20/H7/K11 follow-up: 96 PASS, 2 FAIL (C9, I10), 29 Kort, 10 not run. Before it (E10/D21/K4 follow-up): 93 PASS, 2 FAIL (C9, I10), 29 Kort (needs Kort, friends, real network or visual sign-off), 13 not run.
Before that follow-up (robustness pass): 90 PASS, 2 FAIL, 29 Kort, 16 not run (no agent-runnable check, or turn budget).
After the dedicated-server pass (below): 87 PASS, 1 FAIL (C9), 29 Kort (needs Kort, friends, real network or visual sign-off), 20 not run (no agent-runnable check exists, or a fixture was needed).
Before that pass: 78 PASS, 1 FAIL (K10, since resolved to PASS), 34 Kort, 24 not run. The pass moved C7, C8, C10, E9, F5, J2, J9 to PASS, C9 and C11 to FAIL, and completed the live halves of F2-F4 (already PASS).
A PASS on an AUTO-only row means the standalone or Python check passed in `verify.py fast`; "live" halves of mixed rows were not run (noted in the column).

## Runs
| Run | Method | Result |
|---|---|---|
| Fast tier | `JAVA_HOME=... python tools/verify.py fast --offline` | All PASS: preflight, python-tests, ui checks, java-compile, all `*Check.java`, gradle build. `verify-fast-result.json` |
| Single client, 2 launches | `runClient -PcampaignChecks -PlocalIntegration`, fixed user, fresh root | launch 1 and 2: no fail lines; `lobby-ready`, `restart-collection` (gold 777), `restart-clean-lobby`, `restart-armory-enabled`, `pad-ready`, `pad-step-off`, `campaign-suite`, `roadmap-suite` all pass. `camp-launch2-local-checks.txt` |
| Startup only | `GRAVES_ROADMAP_STARTUP_ONLY=true` | `startup-armory-enabled`, `startup-suite` pass. `startup-local-checks.txt` |
| Two clients (LAN host + loopback joiner, both harness) | `CampNetworkChecks`, options.txt seeded | host and joiner: 0 fail. invite/party, disconnect-holds-army, reconnect-reclaims-army, rematch x2, restart party/room, two-member disk persistence, full-match waits in camp, visitor no report/reward, two waiting rooms. `two-client-*-local-checks.txt` |
| Phase 4 | `GRAVES_PHASE4_CHECKS=convoy,march-modes,modes` (Domination) | convoy suite and march-modes (march 2 to 4) pass; `modes` (Domination) passes 2026-11-03 |

## Failures
- K10: RESOLVED - the suite is named `modes` (no `domination` suite exists); `GRAVES_PHASE4_CHECKS=modes` PASS (`p4-modes-domination`, `p4-modes-suite`). The matrix row was fixed.

## Not run (reasons)
- Dedicated server rows: now run, see "Dedicated-server pass" below.
- E10, D17-D21, H6, H7, I8, I10-I13, K4, K7 (beyond visitor), K11, K12 look checks: no fixture or no automated case (matrix LATER/GAP).
- B1, B3, A8: release-tier packaging; not part of this run. Covered afterwards: `verify.py release --offline` PASS at `01b0c77` (2026-11-03; strict package, package readback, inventory and pins).

## Notes
- A fresh root hangs on the accessibility onboarding screen for `CampNetworkChecks` (it waits for the front menu). Seeding `options.txt` with `onboardAccessibility:false` and `pauseOnLostFocus:false` fixes it; `CampaignChecks` and `Phase4Checks` already handle it. Fixed: tools isolated-checks.gradle now seeds options.txt on a fresh root.
- The host client does not self-stop after the joiner finishes; it was killed. All clients started here were closed; the other worktree's client was not touched.
- Preflight regenerated `Spec.java` and `sheets/preflight.json` (line endings only); reverted, not committed.

## Dedicated-server pass (branch `work/qa-server-rows`, base `main` fd983e7b, tester: Sonnet worker)
Kort approved (Q88, 2026-11-03) accepting the EULA only in a throwaway scratch server under the worktree; it was deleted afterwards. Server: kit scripts (`tools/server-kit`) plus the freshly built 0.5.0 jar, fabric-api, lithium, ferritecore, modernfix and the pinned Fabric launcher (sha256 matched), port 25640, online-mode and whitelist off (scratch only), Java 17. Clients: dev harness clients (`runClient -PlocalIntegration`, `--quickPlayMultiplayer`), one at a time, so at most two Minecraft processes. Every process started here was closed. Details and log excerpts: `qa-matrix-run-2026-11-03-evidence/server-rows-evidence.txt`.

| Row | Result | One-line evidence |
|---|---|---|
| C7 first start | PASS | libraries downloaded, world created, `Done (2.224s)`, camp built, no stack trace |
| C8 status probe | PASS (probe fixed) | status JSON sane (1.20.1, protocol 763, 0/16); `server_status_probe.py` exits 1 because it asserts `players.max == 8`, kit default is 16 |
| C9 join 127.0.0.1 | FAIL (expectation) | join works (`QaClient joined the game`, harness PASS); `gravesandglory-profiles/<uuid>.json` is not created by the join, only on the first collection change (`Campaign.save`) |
| C10 stop | PASS (live re-check 2026-11-03, `fix/c10-stdin-bom` eb8b0ef0) | BOM host: (a) stdin open, `stop.ps1`: "Stop requested", "Stopping the server", "All dimensions are saved", "Server exited with code 0", start.ps1 exit code 0 12 s after the request (save included); (c) stdin closed: exit 0 in 8 s; (b) BOM-prefixed `stop` piped, stdin left open: exit 0 in 5 s, no `Unknown`/`<--[HERE]`. Fix chain: 1358504d (BOM), 005832ee (background reader), eb8b0ef0 (exit after java). Interactive-console typing path (typing `stop` in a real console window) is not covered by a redirected-stdin run: needs a real console window, Kort row. |
| C11 other level-name | PASS (live re-check 2026-11-03) | level-name `Vanilla Probe`: `Done (2.253s)`, INFO `Graves and Glory is inactive: level-name "Vanilla Probe" is not "Graves and Glory" or "Bannerbound"...`, no crash |
| E9 legacy profiles and layout | PASS | `bannerbound-profiles/` and `bannerbound-layout.json` copied byte-identical (sha256), originals kept; failure branch not run live (AUTO covers it) |
| F2 old protocol, F3 old version, F4 build hash | PASS | live clients refused with "requires Graves and Glory 0.5.0 (build 2e255add)"; server logs "Refused incompatible client", no exception. The harness `old` case changes the version string, not the protocol number |
| F5 malformed / missing handshake | PASS | `malformed=true` ("Invalid modpack compatibility response ..."), `missing=true`; no server error |
| J2 build mode, non-op | PASS | `-Dgg.build=camp`; a non-op protocol bot got `command.unknown.command` for `/backup`, `/map`, `/warp` (also absent from its command tree); no op control run |
| J9 RCON | PASS | kit default `enable-rcon=false`; no RCON log line, nothing listening on 25575 |

Findings (not fixed, per scope):
- C9: matrix wording expects the profile file after a bare join; the code writes it lazily. Either the row text or the join path should change (Kort/owner call).
- C11 (fixed; live re-check PASS 2026-11-03): `GravesAndGlory` now logs one INFO line at server start naming the expected level names. Was: no operator log line when the level name is not `Graves and Glory`/`Bannerbound` (README says activation depends on it).
- C10 (fix incomplete: live re-check still sends a BOM-prefixed stop; BOM source not found, start.ps1 StreamWriter looks BOM-free): `start.ps1` now writes to the child's stdin through a BOM-free UTF-8 writer. Was: `start.ps1` forwards `stop` through `ProcessStartInfo.RedirectStandardInput`; with a UTF-8 console input encoding the server received `?stop`. Likely a BOM; seen only under a redirected (non-interactive) host, not checked in a real console window.
- C8 (fixed): the probe reads `max-players` from `tools/server-kit/server.properties.default` (16 if unreadable). Was: `test-tools/standalone/server_status_probe.py` hard-codes `max == 8`.
- First `Done` came with one "Can't keep up" (73 ticks) during the camp build, as the runbook already notes.

## Robustness pass (branch `work/qa-robustness-rows`, base `main` 9ded8364, tester: Sonnet worker)
Same setup as the dedicated-server pass: a new throwaway scratch server inside the worktree (EULA accepted there only, Kort Q88), the kit scripts, the built jar, protocol-763 offline bot clients driven by script plus RCON, never computer-use. At most one java process ran; it was stopped with `stop.ps1` and the scratch folder was deleted. Evidence: `qa-matrix-run-2026-11-03-evidence/robustness-rows-evidence.txt`. Fixtures: `test-tools/resources/fixtures/profile-v1-*.json`.

| Row | Result | One-line evidence |
|---|---|---|
| D17 v1 file, restart, join | PASS | schema 3 profile (gold 1500 + 1200 refund for four retired troops, matches 31, wins 17), `.v1.json` byte copy and `.v2.json` beside it; no separate v2 start was run |
| D18 one corrupt file | PASS (note) | the corrupt commander joins with defaults and his file is kept as `.corrupt`; the two good commanders load intact. The row says "fails": by design he is not refused |
| I10 corrupt parties file | FAIL (expectation) | server starts, uses default parties, keeps `gravesandglory-parties.json.corrupt`; row text expects a startup stop |
| I11 delete layout marker | PASS | restart clean; `camp:practice` rebuilt the arena and rewrote the marker 2 s later; no exception. A join alone does not rewrite it |
| D21 save fails | PASS (follow-up, branch `work/qa-rows-e10-d21-k4`) | `.tmp` blocked by a directory; `roll` -> notice "Collection could not be saved; check disk space.", gold back to 2700, profile sha256 unchanged, `.bak` and `.v1`/`.v2` intact |
| E10 legacy world name | PASS (follow-up) | `level-name=Bannerbound`: camp built, `gravesandglory-camp.json`, `camp:practice` created a battle |
| K4 host leaves | PASS (follow-up) | `social:exit` by the host passes the seat to the guest; a disconnected host keeps the seat during the reconnect reservation ("Waiting for disconnected members to reconnect") |
| D20 oversized profile | PASS (branch `qa/d20-h7-k11`) | oversized `profile_v1` drops only that client, server survives, no profile file changes; valid-size big-gold profiles are ignored |
| H7 rejoin after the match | PASS (branch `qa/d20-h7-k11`) | full practice round ended at 928 s, reward paid once (gold 40), rejoin leaves the profile byte-identical |
| K11 entity cleanup | PASS (branch `qa/d20-h7-k11`) | RCON counts: 39 husks in the round, 0 after; 0 after a stop and restart; camp decoration unchanged. Evidence: `qa-matrix-run-2026-11-03-evidence/qa3-d20-h7-k11-evidence.txt` |

Findings (not fixed, per scope):
- I10: the row text and the code disagree (the code falls back to defaults and quarantines the file, as D18). Kort/owner call: change the row or make startup stop. The same stale expectation applies to D9.
- Preflight regenerated `Spec.java` and `sheets/preflight.json` (line endings only); reverted, not committed.
- Follow-up note: a roll sent right after join was processed during server lag and showed nothing; waiting about 8 s after the snapshot made it reproducible. The v1 profile migration at join wrote the profile despite the blocked `.tmp`, so the D21 baseline hash was taken after the join. A scratch world folder must hold a real world (level.dat) before `start.ps1` runs its backup step.
