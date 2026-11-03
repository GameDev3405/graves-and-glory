# QA matrix run, 2026-11-03 (plan 5.6)

Branch `work/qa-matrix-runs`, base `main` bd05fa2b. Tester: Sonnet worker. Per-row states are in the "Run 2026-11-03" column of `docs/QA-MATRIX.md`. Evidence files (copies of the harness `local-checks.txt` and the verify result) are in `docs/reports/qa-matrix-run-2026-11-03-evidence/`; full logs are under the gitignored `work/q3/`.

## Totals (137 rows)
78 PASS, 1 FAIL, 34 Kort (needs Kort, friends, real network or visual sign-off), 24 not run (no agent-runnable check exists, or a dedicated server/EULA/fixture was needed).
A PASS on an AUTO-only row means the standalone or Python check passed in `verify.py fast`; "live" halves of mixed rows were not run (noted in the column).

## Runs
| Run | Method | Result |
|---|---|---|
| Fast tier | `JAVA_HOME=... python tools/verify.py fast --offline` | All PASS: preflight, python-tests, ui checks, java-compile, all `*Check.java`, gradle build. `verify-fast-result.json` |
| Single client, 2 launches | `runClient -PcampaignChecks -PlocalIntegration`, fixed user, fresh root | launch 1 and 2: no fail lines; `lobby-ready`, `restart-collection` (gold 777), `restart-clean-lobby`, `restart-armory-enabled`, `pad-ready`, `pad-step-off`, `campaign-suite`, `roadmap-suite` all pass. `camp-launch2-local-checks.txt` |
| Startup only | `GRAVES_ROADMAP_STARTUP_ONLY=true` | `startup-armory-enabled`, `startup-suite` pass. `startup-local-checks.txt` |
| Two clients (LAN host + loopback joiner, both harness) | `CampNetworkChecks`, options.txt seeded | host and joiner: 0 fail. invite/party, disconnect-holds-army, reconnect-reclaims-army, rematch x2, restart party/room, two-member disk persistence, full-match waits in camp, visitor no report/reward, two waiting rooms. `two-client-*-local-checks.txt` |
| Phase 4 | `GRAVES_PHASE4_CHECKS=convoy,march-modes,domination` | convoy suite and march-modes (march 2 to 4) pass; `domination` fails |

## Failures
- K10: `GRAVES_PHASE4_CHECKS=domination` reports `fail p4-domination-suite: unknown suite`; the matrix names a harness suite that does not exist (repro: `GRAVES_PHASE4_CHECKS=domination gradlew runClient -PlocalIntegration -I test-tools/isolated-checks.gradle`). Convoy and march modes pass, so this is a missing/misnamed harness case, not a gameplay defect.

## Not run (reasons)
- Dedicated server rows (C7-C11, E9, F5, F2-F4 harness cases, J2, J9): need a dedicated server and Kort's `eula.txt`; AUTO halves of F2-F4 pass.
- E10, D17-D21, H6, H7, I8, I10-I13, K4, K7 (beyond visitor), K11, K12 look checks: no fixture or no automated case (matrix LATER/GAP).
- B1, B3, A8: release-tier packaging; not part of this run.

## Notes
- A fresh root hangs on the accessibility onboarding screen for `CampNetworkChecks` (it waits for the front menu). Seeding `options.txt` with `onboardAccessibility:false` and `pauseOnLostFocus:false` fixes it; `CampaignChecks` and `Phase4Checks` already handle it. Not fixed (test tool left unchanged).
- The host client does not self-stop after the joiner finishes; it was killed. All clients started here were closed; the other worktree's client was not touched.
- Preflight regenerated `Spec.java` and `sheets/preflight.json` (line endings only); reverted, not committed.
