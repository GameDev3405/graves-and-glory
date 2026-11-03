# QA matrix run, 2026-11-03 (plan 5.6)

Branch `work/qa-matrix-runs`, base `main` bd05fa2b. Tester: Sonnet worker. Per-row states are in the "Run 2026-11-03" column of `docs/QA-MATRIX.md`. Evidence files (copies of the harness `local-checks.txt` and the verify result) are in `docs/reports/qa-matrix-run-2026-11-03-evidence/`; full logs are under the gitignored `work/q3/`.

## Totals (137 rows)
Updated after the dedicated-server pass (below): 86 PASS, 2 FAIL (C9, C11), 29 Kort (needs Kort, friends, real network or visual sign-off), 20 not run (no agent-runnable check exists, or a fixture was needed).
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
| C8 status probe | PASS (probe script stale) | status JSON sane (1.20.1, protocol 763, 0/16); `server_status_probe.py` exits 1 because it asserts `players.max == 8`, kit default is 16 |
| C9 join 127.0.0.1 | FAIL (expectation) | join works (`QaClient joined the game`, harness PASS); `gravesandglory-profiles/<uuid>.json` is not created by the join, only on the first collection change (`Campaign.save`) |
| C10 stop | PASS (with finding) | `stop.ps1` saved all dimensions, exit 0; first attempt sent `?stop` (BOM) under a redirected stdin and the server kept running; fine after setting a BOM-free `[Console]::InputEncoding` |
| C11 other level-name | FAIL (GAP) | mod inactive, no `Battle`, no crash, but no log line says why |
| E9 legacy profiles and layout | PASS | `bannerbound-profiles/` and `bannerbound-layout.json` copied byte-identical (sha256), originals kept; failure branch not run live (AUTO covers it) |
| F2 old protocol, F3 old version, F4 build hash | PASS | live clients refused with "requires Graves and Glory 0.5.0 (build 2e255add)"; server logs "Refused incompatible client", no exception. The harness `old` case changes the version string, not the protocol number |
| F5 malformed / missing handshake | PASS | `malformed=true` ("Invalid modpack compatibility response ..."), `missing=true`; no server error |
| J2 build mode, non-op | PASS | `-Dgg.build=camp`; a non-op protocol bot got `command.unknown.command` for `/backup`, `/map`, `/warp` (also absent from its command tree); no op control run |
| J9 RCON | PASS | kit default `enable-rcon=false`; no RCON log line, nothing listening on 25575 |

Findings (not fixed, per scope):
- C9: matrix wording expects the profile file after a bare join; the code writes it lazily. Either the row text or the join path should change (Kort/owner call).
- C11: no operator log line when the level name is not `Graves and Glory`/`Bannerbound` (README says activation depends on it).
- C10: `start.ps1` forwards `stop` through `ProcessStartInfo.RedirectStandardInput`; with a UTF-8 console input encoding the server received `?stop`. Likely a BOM; seen only under a redirected (non-interactive) host, not checked in a real console window.
- C8: `test-tools/standalone/server_status_probe.py` hard-codes `max == 8`.
- First `Done` came with one "Can't keep up" (73 ticks) during the camp build, as the runbook already notes.
