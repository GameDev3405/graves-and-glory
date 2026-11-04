# QA: fresh install and save migration on main ebd38de3 (2026-11-04)

Branch `qa/fresh-install-1104`. Board row 5.6 said the fresh-install and migration rows had no recorded run on current main; they now do. Evidence: `docs/reports/qa-fresh-install-2026-11-04-evidence/`.

## Results

| Rows | Result | How |
|---|---|---|
| A2 | PASS | Harness client, empty root: `startup-armory-enabled`, `startup-suite`. |
| A3 | PASS | `campaignChecks`, two launches of one root, fixed user: `lobby-ready`, `restart-collection`, `restart-clean-lobby`, `restart-armory-enabled`; 80 pass, 0 fail. |
| A4, A5 | PASS | `verify.py fast` (ProfileMigrationCheck 44 checks, LegacyDataMigrationCheck 13). |
| D1 to D8, D10 to D15, E1 to E8 | PASS | Same fast run. |
| D16 to D18, D22, D23 | PASS | Scratch dedicated server, two starts, a bot join each: five v1 fixtures, a real v2 image (campaignCleared 3), the v3 veteran file and a non-JSON file. All load as schema 4 with `.v1/.v2/.v3.json` byte copies, starter gear worn, relic in slot 0; v2 gives battlesWon march-1 to 3; the second start changes no file; the corrupt file is quarantined as `.json.corrupt` and that commander gets defaults. |
| D19 | PASS | Phase 2 migration suite, four launches of one root (import once, fixtures, restart). |
| E9 | PASS | Legacy `bannerbound-profiles/` and `bannerbound-layout.json` copied byte-identical, originals kept, the legacy commander then loads as schema 4. |
| K14 | GAP | No Linux or macOS client. WSL Ubuntu has no Java; installing it means a download not approved. |
| A1, A6, A7 | Kort | Unchanged, manual. |
| D9, D20, D21, E10 | not rerun | Passed earlier on 2026-11-04b; outside this pass. |

## Findings

1. **Fixed: `verify.py fast` failed on main at `ui-assets`.** 26 healer and Long Colours grid icons were stale against the gear art merged in fc2bad30. `python tools/build_ui_art.py` regenerated them (generated files only; the gate is the test). Kort may want to glance at the healer grid icons.
2. **Flake, not fixed:** `p2-full-roster` failed once (launch 2 of four): the War Table recruit page had drawn no rows at frame 8. It passed on launches 1, 3 and 4; no migration result was affected. Worth a longer frame wait in `Phase2Checks` later.
3. A hand-made v2 file in the compact form the unit test uses (`"banner":"knight,archer"`) is treated as corrupt by the real server, because the real Gson reads `banner` as a list. Real v2 images written by the game load fine. No change made; only matters if some old build wrote strings.
4. Dev harness roots need `evidence/` to exist or `restart-fixture` fails with NoSuchFile.

## New tooling

`test-tools/qa/server_migration_probe.py` (modes `e` and `d`) and its `profile-v2-seed.json` repeat the dedicated-server rows. eula.txt was written only in the scratch root `work/qa1104/srv` and is not committed. At most one Minecraft process ran at a time; port 25690 only.
