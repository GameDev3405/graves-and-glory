# QA matrix (plan item 5.6)

2026-11-01. Base `main` 5a7cdbb. First pass 2026-11-02 (no-client rows) is recorded in `docs/reports/qa-matrix-2026-11-02.md`; check tags below updated for rows it found already covered. The rest was docs only: nothing was run; no client or server was started and no EULA was touched. Rows come from the code and tools as they stand: `tools/package.py`, `tools/verify_package.py`, `tools/export_server.py`, `tools/export_modrinth.py`, `tools/verify.py`, `test-tools/`, `docs/SIX-PLAYER-TEST-PLAN.md`, `Campaign.java`, `GravesAndGlory.java`, `VersionGate.java`, `BuildMode.java`.

## How to read it

Check column values:

| Tag | Meaning |
|---|---|
| AUTO `Name` | Existing standalone Java check or Python unit test. Runs inside `python tools/verify.py fast --offline` (the runner globs `test-tools/standalone/*Check.java`, 20 files today). No Minecraft. |
| HARN `suite` | Existing real-client harness check under `test-tools/java/gg/gravesandglory/`, run with `gradlew runClient -PlocalIntegration -I test-tools/isolated-checks.gradle` (or `isolated-server.gradle`) and the `GRAVES_*` variable named. Needs a Minecraft client process but no human. Only the Phase 1 combat suite is wired into `verify.py game`; the rest are run by hand. |
| LATER | Not covered; could be automated without new features. Named in the gap list. |
| MANUAL | Needs Kort or friends (real machines, Tailscale, human judgment). |
| GAP | The plan or docs expect a behavior that is missing or unverified in the code. |

Result column to fill in on each pass: Pass / Fail / Blocked, tester, jar SHA-256 first 8, date. A row is green when its check passed on the candidate jar named in `releases/current-candidate.json`.

Rules that apply to every row: never write or accept `eula.txt` (Kort supplies it); never touch ports 25565-25567, 25570 (builder), 25571; never stop Kort's game or the builder; test on a copy of any save; headless bots never count as players.

Run 2026-11-03 (`work/qa-matrix-runs`, base `main` bd05fa2b), after the dedicated-server pass (`work/qa-server-rows`): 103 PASS (E10, D21, K4, D20, H7, K11, A8, B1, B3, C12, D19, I13, J6 added 2026-11-03), 2 FAIL (C9, I10), 30 Kort (K12 relabelled), 1 GAP (K14), 1 not run (K7). Detail: `docs/reports/qa-matrix-run-2026-11-03.md`.

---

## A. Fresh install, client

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| A1 | Import the player pack (`dist/graves-and-glory-<ver>.zip` or the `.mrpack`) into a clean launcher profile: Minecraft 1.20.1, Fabric Loader 0.16.14, Java 17. Start the game. | Title screen is the Graves and Glory front menu; no crash; `latest.log` has no mod-load error. Pack holds the mod jar, Fabric API 0.92.2+1.20.1 and the pinned shader/resource files only. | MANUAL (friends). Pack contents: AUTO `verify_package.py` | Kort |
| A2 | First launch with an empty game directory. | Built-in armory pack is enabled without the player touching Resource Packs. | HARN `startup-armory-enabled` (`GRAVES_ROADMAP_STARTUP_ONLY=true`) | PASS - HARN startup-armory-enabled, startup-suite |
| A3 | Open Play (single-player private world) on the fresh install. | Camp loads, commander is in the camp, no stale husks, a new commander gets a default collection. | HARN `lobby-ready`, `restart-clean-lobby` (`CampaignChecks`) | PASS - HARN lobby-ready, restart-clean-lobby |
| A4 | New commander with no profile file. | Schema 3; the 10 Commons owned at star 1; starter banner; commander `champion`; banner colour gold; gold 0; no battle won; no bonus claimed. | AUTO `ProfileMigrationCheck` (`profile-default`, `fresh-save`) | PASS - AUTO fast verify |
| A5 | Fresh install: no `bannerbound/` data and no `Bannerbound` world. | No migration side effects, no error. | AUTO `LegacyDataMigrationCheck` (fresh install row) | PASS - AUTO fast verify |
| A6 | Launch with the pack but without Fabric API, or with a wrong Minecraft or Loader version. | Launcher or Fabric refuses with its own dependency message; no half-loaded game. | MANUAL | Kort |
| A7 | Install alongside shaders (Complementary Reimagined/Unbound, MakeUp Ultra Fast) and with them off. | Game starts and plays both ways; Excalibur on and off for the visual pass. | MANUAL (Kort, SIX-PLAYER-TEST-PLAN pass 10) | Kort |
| A8 | Player pack credits: open `docs/THIRD_PARTY.md` and `docs/third-party/` in the pack. | Every bundled mod/shader/resource has its notice; Excalibur credits Maffhew; hash matches the original. | AUTO `verify_package.py` (inventory and pins). Credit wording: MANUAL | PASS 2026-11-03: `verify_package.py` inventory and pins PASS; THIRD_PARTY lists every bundled item; Excalibur credits Maffhew with the permission wording; its zip hash equals the pin (ccf01766c56a). Evidence: `qa-matrix-run-2026-11-03-evidence/qa4-rest-evidence.txt` |

## B. Release package and server kit build

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| B1 | `python tools/package.py` on a build with no stopgap tier art. | Writes `dist/graves-and-glory-<ver>.zip` and `dist/package-manifest.json`. | AUTO `verify.py release` | PASS 2026-11-03: `verify.py release --offline` strict package PASS; wrote `dist/graves-and-glory-0.5.0.zip` and `dist/package-manifest.json` |
| B2 | Same while any troop still draws a stopgap tier (26 troops today per HANDOFF). | Refuses with the tier gate message. `--allow-stopgap` writes a `-stopgap` named file only; never for public release. | AUTO `test_build_skins.py` for the gate [coverage of the refusal path not re-read]; current refusal is expected, not a bug | PASS - AUTO python-tests in fast verify |
| B3 | `python tools/verify_package.py` after B1. | PASS: hash matches the manifest, no duplicate entries, inventory equals manifest, every path under `mods/ docs/ shaderpacks/ resourcepacks/`, inner jar identity valid, dependency pins match. | AUTO `verify_package.py` | PASS 2026-11-03: `verify_package.py` PASS (63 entries, build 0.5.0, hash c092a973...) |
| B4 | Rebuild the jar from changed source, then package without rebuilding. | Stale build is rejected by source fingerprint. | AUTO `test_release_integrity.py` (`test_identity_and_stale_build_rejection`) | PASS - AUTO python-tests in fast verify |
| B5 | Look inside the jar for harness classes or service providers. | None; every harness class is refused. | AUTO `test_release_integrity.py` (`test_harness_and_service_provider_rejection`, `test_every_harness_class_is_refused`) | PASS - AUTO python-tests in fast verify |
| B6 | `.mrpack` index validation: unsafe paths, duplicates, unapproved downloads, missing or malformed hashes. | Each is rejected. | AUTO `test_modrinth_export.py` | PASS - AUTO python-tests in fast verify |
| B7 | `python tools/export_server.py` after B1 and a `.mrpack`. | Writes `dist/graves-and-glory-<ver>-server.zip` and `dist/server-manifest.json`; readback of every entry matches; archive set equals the allowlist. | AUTO `test_export_server.py` | PASS - AUTO python-tests in fast verify |
| B8 | List the server kit. | Contains: the mod jar, the four required server dependencies from `dependencies/platform-lock.json` (sha512 checked), `docs/` notices, `server.properties`, `setup-server.ps1`, `start-server.ps1`, `SERVER-README.txt`. Contains no `eula.txt`, no saves, no profiles, no credentials, no Fabric launcher, no rendering/shader mods. | AUTO `test_export_server.py` | PASS - AUTO python-tests in fast verify |
| B9 | `server.properties` in the kit. | `level-name=Graves and Glory`, `online-mode=true`, `max-players=16`, `server-ip=127.0.0.1`, `server-port=25565`, `enable-rcon=false`, `gamemode=adventure`, `spawn-protection=0`, flat world with no structures. | AUTO `test_export_server.py` | PASS - AUTO python-tests in fast verify |
| B10 | Tamper with one server dependency file, then export. | Export fails with "Stale server dependency". | AUTO `test_export_server.py` (`test_tampered_dependency_stops_the_export`) | PASS - AUTO python-tests in fast verify |

## C. Server kit on a fresh machine (headless)

Use a new empty folder and a free port. Kort supplies `eula.txt` himself; the tester never creates it. Rows that need EULA acceptance are marked.

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| C1 | Extract the kit, run `start-server.ps1` before `setup-server.ps1`. | Throws "Run setup-server.ps1 first."; nothing starts. | MANUAL (LATER as a script check of the `.ps1` text) | Kort |
| C2 | Run `setup-server.ps1`. | Downloads the pinned Fabric 0.16.14 launcher, SHA-256 `856a5f9f...` verified, moved into place. Does not start the server and accepts no terms. | MANUAL (needs network) | Kort |
| C3 | Run `setup-server.ps1` again with the launcher present. | Throws "Launcher already exists"; the existing launcher is untouched. | MANUAL | Kort |
| C4 | Corrupt the downloaded file and re-run setup (or simulate a hash mismatch). | Throws "Fabric download hash mismatch. Do not run it."; the staged file is never promoted. | MANUAL | Kort |
| C5 | Launcher present, no `eula.txt`; run `start-server.ps1`. | Throws "Review the Minecraft server EULA yourself..."; server does not run. | MANUAL | Kort |
| C6 | `-MemoryGB 1` and `-MemoryGB 64`. | Parameter validation rejects values outside 2-32. | MANUAL | Kort |
| C7 | With Kort's `eula.txt`: first start. | Minecraft and Fabric libraries download; world `Graves and Glory` is created; "Done" appears; the first battlefield is built afterward (startup tick-lag warnings are expected); no stack trace. | MANUAL (Kort). Startup path HARN `GRAVES_ROADMAP_SERVER_ROOT` + `isolated-server.gradle` (headless dedicated server run) | PASS - server pass 2026-11-03 |
| C8 | `python test-tools/standalone/server_status_probe.py <port>` against the running server. | Returns the status JSON; version and player count are sane. | MANUAL; the probe exists but is local-only and not wired into any check | PASS - status JSON sane; probe script asserts max==8 (stale) |
| C9 | Join from the same machine, 127.0.0.1:25565, with the same pack. | Client reaches the camp; the server log shows the commander profile created under `<world>/gravesandglory-profiles/<uuid>.json`. | MANUAL; networked path HARN `CampNetworkChecks` (`GRAVES_NETWORK_HOST`, `host-suite`, `invite-accept-roundtrip`) | FAIL - join works; profile file not written on bare join |
| C10 | Type `stop` in the console. | Clean save and exit; no error; battle state cleaned (`Abilities.serverStopping`, banners and clouds removed). | MANUAL; clean stop with a client HARN `host-stop-disconnects-cleanly` | PASS (live re-check 2026-11-03, `fix/c10-stdin-bom` eb8b0ef0) - BOM host with stdin open, stdin closed, and a BOM-prefixed piped `stop`: save, "All dimensions are saved", java gone and start.ps1 exited 0 within 5-12 s of the request, no unknown command. Interactive-console typing path (typing `stop` in a real console window) is not covered by a redirected-stdin run: needs a real console window, Kort row. |
| C11 | Start a server whose `level-name` is neither `Graves and Glory` nor `Bannerbound`. | The mod stays inactive (no `Battle` is created); vanilla world, no crash. Confirm there is a log line telling the operator why. | MANUAL; GAP if no log line (README says activation depends on the internal level name) | FAIL - inactive, no crash, no log line (GAP) |
| C12 | Kit as shipped: `max-players=16` versus the 4v4 + visitors rule. | Sixteen connections allowed; at most eight battle commanders; the rest wait in camp. | AUTO `AdmissionPolicyCheck`, `LobbyRulesCheck` (limits); the `server.properties` value by B9 | PASS 2026-11-03: `AdmissionPolicyCheck` and `LobbyRulesCheck` PASS in the release run; kit and Modrinth `server.properties` both `max-players=16`; the scratch server reported "max of 16 players" |
| C13 | Reachability from another PC (Tailscale), non-local `server-ip`. | `Test-NetConnection <ip> -Port <port>` succeeds; builder 25570 untouched. | MANUAL (Kort, SIX-PLAYER-TEST-PLAN section 3) | Kort |
| C14 | Kit docs: `SERVER-README.txt` claims versus real behavior. | Every sentence still true (heap defaults 2G/8G, the EULA sentence, the CURRENT LIMITS text). | MANUAL | Kort |

---

## D. Profile migration, v1 and v2 to v3

Profiles are the server's `<uuid>.json`, read once per commander by `Campaign.profile()` through `ProfileMigration.upgradeFile`. Original bytes are kept as `<uuid>.v1.json` or `<uuid>.v2.json`. Fixtures: `test-tools/resources/fixtures/profile-v1-{fresh,full,invalid,partial,cache}.json`.

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| D1 | v1 fresh profile (starter Commons only). | Owned = the 10 Commons at star 1; banner `knight x12, archer x6, empty x2`; commander `champion`; gold = v1 gold + 600 (mage and hexer refunded even if not listed); matches, wins, campaignCleared, tutorialDone copied; five House keys at 0; schema 2 then 3. | AUTO `ProfileMigrationCheck` (`mig-fresh`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D2 | v1 full profile. | Adds sentinel, longbow, reaper, stalker; no mage/hexer/oracle/wraith; gold + 1200. | AUTO `ProfileMigrationCheck` (`mig-full`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D3 | v1 with an invalid Dawn line (7 slots); both lines invalid; wrong-side line; no line. | Falls back to the Dusk line repacked, or the starter banner. | AUTO `ProfileMigrationCheck` (`mig-invalid-dawn`, `mig-both-invalid`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D4 | v1 class and id mapping (marksman, arcanist, vanguard, paladin, null; `ID_MAP`). | Classes map to warden, warlock, champion; unowned mapped ids dropped by repack. | AUTO `ProfileMigrationCheck` (`mig-map`, class map) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D5 | v1 progression (xp, claims, titles, title). | Carried across exactly. | AUTO `ProfileMigrationCheck` (`mig-progression`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D6 | v1 file on disk, then load. | `<uuid>.v1.json` is a byte copy of the original; `<uuid>.json` is schema 3, written through a temp file, no `.tmp` left. | AUTO `ProfileMigrationCheck` (`mig-file`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D7 | Load the same profile a second time. | Returns no migration; both files unchanged. | AUTO `ProfileMigrationCheck` (`mig-idempotent`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D8 | A `<uuid>.v1.json` already exists. | The existing backup is never overwritten. | AUTO `ProfileMigrationCheck` (`mig-backup-once`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D9 | Corrupt v1 file (unparseable JSON). | Throws IllegalStateException; original untouched; no backup, no `.tmp`. The server surfaces "Commander profile could not be read. Original and .bak preserved". | AUTO `ProfileMigrationCheck` (`mig-corrupt`). Server message text: MANUAL | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D10 | `<uuid>.tmp` cannot be written (locked or read-only). | The original `<uuid>.json` is left unchanged. | AUTO `ProfileMigrationCheck` (`mig-tmp-fail`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D11 | v2 file with `campaignCleared` 1, 3, 6. | Schema 3; `battlesWon` = march-1..N; `bonusClaimed` empty; gold, banner, writs, writChapters kept; `<uuid>.v2.json` is a byte copy. | AUTO `ProfileMigrationCheck` (`mig-v2`, `v2toV3`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D12 | Load a schema 3 file. | No migration; both files unchanged. | AUTO `ProfileMigrationCheck` (`mig-v2-idempotent`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D13 | A schema 1 file runs on to schema 3 in one load; the backup keeps schema 1 bytes. | `.v1.json` keeps the schema 1 bytes; a `.v2.json` also holds the schema 2 image. | AUTO `ProfileMigrationCheck` (`mig-file` schema 1 row) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D14 | Hand-edited schema 3 file: unknown ids, bad stars, mastery, banner, class, colour, negative gold, missing sets. | `sanitize()` clamps or drops each; invalid banner repacked; missing `battlesWon`/`bonusClaimed` rebuilt empty. | AUTO `ProfileMigrationCheck` (`sanitize` rows) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D15 | Write, read back, write. | Byte-identical JSON round trip. | AUTO `ProfileMigrationCheck` (`round-trip`) | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D16 | Fixtures `profile-v1-partial.json` and `profile-v1-cache.json`. | Migrate to a valid schema 3 profile. | AUTO `ProfileFixtureCheck` (all five fixtures, real Gson); live path HARN `Phase2Checks` | PASS - AUTO ProfileMigrationCheck / ProfileFixtureCheck |
| D17 | Real server restart on a host with a v1 or v2 file in `gravesandglory-profiles/`, then join as that commander. | The collection shows the migrated profile; `.v1.json`/`.v2.json` present beside it; no data lost in the client UI. | LATER (HARN `GRAVES_ROADMAP_MIGRATION=true` covers the legacy-folder path only, not v1/v2 file content end to end). Final eyes: MANUAL | PASS (robustness pass 2026-11-03): DaveV1 v1 file migrated to schema 3 (gold 1500 + 1200 refund for 4 retired troops, matches 31, wins 17), `.v1.json` byte copy and `.v2.json` beside it; v1 only, no separate v2 start |
| D18 | Two commanders, one with a corrupt file. | Corrupt one fails; the other loads and plays; no cross-contamination. | LATER | PASS (robustness pass 2026-11-03): the corrupt commander joins with defaults and the file is kept as `.corrupt` (no join failure, by design); the other two load intact |
| D19 | Client sends a portable profile copy (`importProfile`) on first visit to a single-player or LAN world with no record. | Imported once, upgraded the same way as a server file. Over 16000 characters is ignored. | LATER (no standalone check on `importAllowed`) | PASS 2026-11-03: HARN `GRAVES_PHASE2_CHECKS=migration` on one real client: `p2-migrate-import` true (first visit imported and migrated, gold 1800, saved as schema 3; a second import with 99999 gold ignored, file byte-unchanged). The over-16000 cap is the `readString(16000)` decoder limit, shown live by D20 |
| D20 | Same on a dedicated server. | Never accepted; the server's file is the only source of truth. | LATER (code: `importAllowed.remove(id) && !server.isDedicated()`) | PASS (scratch dedicated server 2026-11-03, `work/qa3`, jar 800c5d8e): oversized `profile_v1` (20022 chars) -> that client alone is dropped with DecoderException (20022 > 16000), server keeps running (RCON answers, later bots join); no profile file changed (sha256 of the existing commander's `.json`, `.v1`, `.v2` identical before/after; no file created for a fresh commander). A valid-size profile with gold 999999 from an existing and from a fresh commander: log "Ignored the collection sent by ...", no file written or changed. Note: the 16000 cap is enforced by `readString(16000)` in the receiver, so the in-handler `length>16000` check is never reached |
| D21 | Save fails (disk full or read-only directory). | Player is told "Collection could not be saved; check disk space."; old file and `.bak` intact. | LATER | PASS (scratch server 2026-11-03, `work/qa-rows-e10-d21-k4`): `.tmp` path blocked by a directory, bot `roll` -> player notice "Collection could not be saved; check disk space.", gold rolled back (2700), profile sha256 unchanged (a68003bb...), `.bak` and `.v1`/`.v2` intact; server logs AccessDeniedException |
| D22 | Run a migrated profile through a save, quit, relaunch. | Nothing changes (no repeated migration, no reward grants twice). | HARN `restart-collection` (`CampaignChecks`) | PASS - HARN restart-collection (gold 777; no v1 fixture this run) |

## E. Legacy Bannerbound data (rename)

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| E1 | Copy `bannerbound/collections`, unlocks and deployment to `gravesandglory/`. | Byte-identical; original kept. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E2 | Both directories hold the same commander. | The current copy wins. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E3 | Test collections. | Never copied by production migration; development isolation retained. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E4 | Repeat the migration after progress. | Progress is not rolled back. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E5 | Visual preference file (`presentation.json`). | Preserved. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E6 | Legacy `Bannerbound` world next to no current world. | Copied to `Graves and Glory`; regions copied; original retained; `session.lock` not copied. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E7 | The legacy world is open (locked) while migrating. | Refused; nothing copied. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E8 | Incomplete current world next to a legacy world. | Refused; recovery needed, no overwrite. | AUTO `LegacyDataMigrationCheck` | PASS - AUTO LegacyDataMigrationCheck |
| E9 | Dedicated server with `bannerbound-profiles/` and `bannerbound-layout.json` in the world. | Profiles copied to `gravesandglory-profiles/`; layout marker copied; originals kept; failure keeps originals and stops with a clear message. | HARN `legacy-profile-upgrade`, `legacy-layout-upgrade` (`GRAVES_ROADMAP_MIGRATION`) | PASS - server pass 2026-11-03 |
| E10 | A legacy-named world (`Bannerbound`) still activates the mod. | `Battle` is created for both level names. | HARN `legacy-world-upgrade` | PASS (scratch server 2026-11-03): `level-name=Bannerbound` start: camp built, `gravesandglory-camp.json` written; bot joined, `camp:practice` created the battle (snapshot has a `battle` entry); no exception |
| E11 | Migration error at client start (storage or permissions). | Front menu shows "Could not migrate old collection/settings. Your originals are unchanged." | MANUAL | Kort |

## F. Version gate and compatibility

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| F1 | Matching release, protocol and build. | Admitted. | AUTO `CompatibilityPolicyCheck` | PASS - AUTO CompatibilityPolicyCheck (harness cases not run) |
| F2 | Old protocol. | Refused. | AUTO `CompatibilityPolicyCheck`; HARN `GRAVES_ROADMAP_COMPAT_CASE=old` | PASS - AUTO CompatibilityPolicyCheck (live harness case also PASS, server pass 2026-11-03) |
| F3 | Old version string. | Refused. | AUTO `CompatibilityPolicyCheck`; HARN `old` | PASS - AUTO CompatibilityPolicyCheck (live harness case also PASS, server pass 2026-11-03) |
| F4 | Same version, different build hash. | Refused. | AUTO `CompatibilityPolicyCheck`; HARN `build` | PASS - AUTO CompatibilityPolicyCheck (live harness case also PASS, server pass 2026-11-03) |
| F5 | Malformed handshake; no handshake reply. | Disconnect with a message; no server error. | HARN `GRAVES_ROADMAP_COMPAT_CASE=malformed`, `missing` | PASS - malformed and missing, server pass 2026-11-03 |
| F6 | Message text on refusal. | Names the required version and the short build id; says "Install the same modpack release". | AUTO `CompatibilityPolicyCheck` | PASS - AUTO CompatibilityPolicyCheck (harness cases not run) |
| F7 | A vanilla or Fabric-only client joins a Graves and Glory server. | Refused with the required-mod message at login, before play. | MANUAL (GAP: no automated case for a client with no mod) | Kort |
| F8 | A new client joins a server built from a different commit of the same version. | Refused (F4 case end to end). | MANUAL (candidate vs. friend pack mix-up is the most likely real failure) | Kort |

---

## G. Lobby, admission and seats

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| G1 | Open the lobby; join by room code; sides stay level; 3v3 fills at six; a seventh is refused; host widens to 4v4; ninth refused. | As listed. | AUTO `LobbyRulesCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G2 | Host-only actions (map, mode, bot fill, hand the host seat); refusal messages. | Non-host is refused; host change clears every ready. | AUTO `LobbyRulesCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G3 | Ten-second countdown: all ready starts it; anyone standing down, a settings change, or a busy battlefield stops or holds it; it fires once. | As listed. | AUTO `LobbyRulesCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G4 | Bot fill on: one human counts down; off with a short room: "Fill both teams or enable bots."; only a host toggles it. | As listed. | AUTO `LobbyRulesCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G5 | House pick that fields no troop; House banner derivation. | Cannot ready; banner is never mutated. | AUTO `LobbyRulesCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G6 | Full human match plus a visitor. | Visitor waits in camp; no extra seat, no extra army. | AUTO `AdmissionPolicyCheck`; HARN `full-match-waits-in-camp`, `full-match-no-extra-seat` | PASS - HARN two-client CampNetworkChecks |
| G7 | Open seat plus a visitor. | Camp visitors never consume a vacant team seat. | AUTO `AdmissionPolicyCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G8 | Repeated arrivals up to and past capacity (each team size). | Team capacity never exceeded; arrivals past capacity wait. | AUTO `AdmissionPolicyCheck` | PASS - AUTO LobbyRulesCheck / AdmissionPolicyCheck |
| G9 | Visitor results. | No match report, no reward, not a participant. | HARN `visitor-no-match-report`, `visitor-no-reward`, `visitor-not-participant` | PASS - HARN two-client CampNetworkChecks |
| G10 | Two waiting rooms at once. | Independent; other rooms unchanged. | HARN `two-live-waiting-rooms`, `other-room-unchanged`, `visitor-room-independent` | PASS - HARN two-client CampNetworkChecks |
| G11 | Invite, accept, party stays together, party returns with results. | As listed. | HARN `invite-accept-roundtrip`, `whole-party-active`, `friend-returns-with-party`, `host-party-results` | PASS - HARN two-client CampNetworkChecks |
| G12 | Rematch after a match without a restart. | Second match starts; party kept. | HARN `friend-rematch`, `host-rematch`, `party-after-two-matches` | PASS - HARN two-client CampNetworkChecks |

## H. Reconnect

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| H1 | Commander quits mid-battle. | A bot holds the army (`Battle.leave`, `campaign.vacated`); team is not a commander short. | HARN `disconnect-holds-army` | PASS - HARN two-client CampNetworkChecks |
| H2 | The same commander rejoins. | Reclaims the held army: same side, House, class; no second army; no duplicate unit or reward. | AUTO `AdmissionPolicyCheck` (reservation priority); HARN `reconnect-reclaims-army`, `friend-reconnects-to-battle` | PASS - HARN two-client CampNetworkChecks |
| H3 | A different visitor arrives while a seat is held for someone else. | Cannot steal the reserved army; stale reservation cannot replace an opponent. | AUTO `AdmissionPolicyCheck` | PASS - AUTO AdmissionPolicyCheck / MatchRulesCheck |
| H4 | Practice battle: visitor, reconnect, stale reservation. | Visitor waits; owner reclaims. | AUTO `AdmissionPolicyCheck` | PASS - AUTO AdmissionPolicyCheck / MatchRulesCheck |
| H5 | Party member drops and returns. | Party kept. | HARN `disconnect-keeps-party` | PASS - HARN two-client CampNetworkChecks |
| H6 | Quit by the menu versus hard kill of the client process (Task Manager). | Same outcome as H1/H2; server logs no exception. | MANUAL (SIX-PLAYER-TEST-PLAN section 6); LATER for the hard kill with two harness clients | Kort |
| H7 | Rejoin after the match ended. | Back in camp, collection intact, no reward twice. | LATER | PASS (scratch server 2026-11-03): bot commander started `camp:practice`, the round ended by itself after 928 s (900 s clock plus intro); profile written once: gold 40, matches 1, shards gilded 1. Disconnect and rejoin 13 s later: back in camp, state_v9 snapshots received, profile file sha256 identical (0a6a2136...), no second reward; same after a server stop and restart. No exception in the log |
| H8 | Rejoin time limit: the slot is held 2 minutes (Kort 2026-11-02). | Rejoin inside 2 minutes reclaims the army (a bot takes command after 60 s). After 2 minutes the reservation is released; the army stays with its bot and any joiner, the owner included, may take the seat. | AUTO `MatchRulesCheck` (rule and wiring); HARN case LATER | PASS - AUTO AdmissionPolicyCheck / MatchRulesCheck |
| H9 | Host (LAN/single-player) quits. | Guests are disconnected cleanly; no hang. | HARN `host-stop-disconnects-cleanly` | PASS - HARN two-client CampNetworkChecks |
| H10 | Network drop (cable or Tailscale down) rather than a clean quit. | Server times the client out, then H1; rejoin works. | MANUAL | Kort |
| H11 | Same account joins twice. | Second login is refused by vanilla; no duplicate commander. | MANUAL | Kort |

## I. Restart, crash and persistence

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| I1 | Clean `stop` in the lobby/camp; start; join. | Collection, loadout, commander, House, rewards, unlocks identical to before. | HARN `restart-collection` (`CampaignChecks`) | PASS - HARN restart-collection, restart-clean-lobby |
| I2 | After restart: no leftover hostile mobs from the earlier session. | Zero husks tagged `gravesandglory`. | HARN `restart-clean-lobby` | PASS - HARN restart-collection, restart-clean-lobby |
| I3 | After restart: parties and rooms. | Party retained; rooms cleared. | HARN `server-restart-party`, `restart-party-retained`, `restart-room-cleared`, `server-restart-no-rooms` | PASS - HARN two-client restart/disk party checks |
| I4 | Parties written to disk. | Two members survive on disk; file readable. | HARN `two-member-disk-persistence`, `disk-read` | PASS - HARN two-client restart/disk party checks |
| I5 | Map cache marker (`gravesandglory-layout.json`): restart with an unchanged map. | Arena is not rebuilt. | AUTO `LayoutMarkerCheck` | PASS - AUTO LayoutMarkerCheck / MatchRulesCheck (live stop/crash not run) |
| I6 | Change the landmarks or margin revision. | Arena rebuilds once on next start. | AUTO `LayoutMarkerCheck` | PASS - AUTO LayoutMarkerCheck / MatchRulesCheck (live stop/crash not run) |
| I7 | Clean `stop` mid-match. | Server stops without error; planted banners and clouds are gone; no match record is resumed. Each seated player gets the participation gold pro-rated by the share played, no win bonus (Kort 2026-11-02). | AUTO `MatchRulesCheck` (rule and wiring); MANUAL for the live stop | PASS - AUTO LayoutMarkerCheck / MatchRulesCheck (live stop/crash not run) |
| I8 | Kill only the server java process (crash) in camp; restart. | World loads without regenerating; profile loads. Saves go through a temp file, an atomic move and a `.bak`, so a torn write must not occur. | MANUAL (Kort, never the builder); LATER for the torn-write case | Kort |
| I9 | Crash mid-match; restart. | Camp state sane; no ghost units; the next start pays the same pro-rated share from the last 30 s checkpoint (`match-progress.json`), no win bonus (Kort 2026-11-02). | AUTO `MatchRulesCheck` (wiring); MANUAL for the live crash | PASS - AUTO LayoutMarkerCheck / MatchRulesCheck (live stop/crash not run) |
| I10 | Corrupt `gravesandglory-parties.json`; start. | Startup stops with "Party data could not be read; original and .bak are preserved"; originals untouched. | LATER | FAIL (expectation; robustness pass 2026-11-03): the server starts with default parties and keeps the damaged file as `gravesandglory-parties.json.corrupt`; it does not stop. No data lost; the row text predates the SafeFiles fallback (Kort call: change the row or the behaviour) |
| I11 | Delete `gravesandglory-layout.json`. | Arena rebuilds; no crash. | LATER | PASS (robustness pass 2026-11-03): marker deleted, restarted; `camp:practice` rebuilt the arena and rewrote the marker 2 s later, no exception |
| I12 | Back up the world, restore on a second folder, start. | Same profiles and maps. | MANUAL (SIX-PLAYER-TEST-PLAN section 3, step 5) | Kort |
| I13 | Disk full during profile save. | See D21. | LATER | PASS 2026-11-03: live on the scratch server with the `.tmp` path blocked: notice "Collection could not be saved; check disk space.", profile hash unchanged, `.bak` and `.v1/.v2` intact, a roll after unblocking saved (gold 2600) |
| I14 | Long soak: three matches, restart, reconnect, restart. | Section 7 fields intact (House, class, rewards, profile, unlocks, map state). | MANUAL (SIX-PLAYER-TEST-PLAN sections 6 and 7) | Kort |

## J. Permissions and abuse

The mod registers no player commands outside build mode. In `BuildMode` (only when started with `-Dgg.build=<camp|map id>`): `/warp`, `/backup` and `/map` need op level 2 (`/warp` also needs level 2 (Kort 2026-11-02)).

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| J1 | Kit `server.properties` and a normal match server. | No `-Dgg.build`; no build commands exist; no player has op unless Kort grants it. | MANUAL (also B9 for the file) | Kort |
| J2 | Build mode: non-op runs `/backup` and `/map`. | Refused. | LATER (needs a build-mode server) | PASS - non-op /backup /map /warp refused (protocol bot) |
| J3 | Build mode: non-op runs `/warp`. | Refused: `/warp` needs permission level 2 (Kort 2026-11-02; already true). | AUTO `MatchRulesCheck`; builder server only | PASS - AUTO MatchRulesCheck / LobbyRulesCheck |
| J4 | Non-whitelisted player on a whitelisted server (`white-list=true`, `enforce-whitelist=true` on the builder). | Refused by vanilla. | MANUAL | Kort |
| J5 | Non-host sends host-only lobby actions (map, mode, bot fill, hand-over). | Refused with the "Only an idle room host" messages. | AUTO `LobbyRulesCheck` | PASS - AUTO MatchRulesCheck / LobbyRulesCheck |
| J6 | Packet abuse: oversized or malformed profile (over 16000), tactical packet with bad group/formation strings, order from a commander for another army. | Ignored or clamped; no exception; no state change. | LATER (profile size and import gate by D19/D20; `tactical` handler has no direct check) | PASS 2026-11-03 (with note): live round, 40+ bad packets (unknown group/formation/action, empty and control-char strings, NaN/inf/1e308 x, z and yaw, out-of-arena points, bogus or negative target ids, 159 entity ids, 24 malformed orders, an unseated visitor's orders and tactical): no accept text, no state change, no exception, valid order still accepted afterwards. Note: strings over the length cap (16/48/128) are rejected by the netty decoder; the sender is dropped and a DecoderException is logged, the server and others are unaffected (same as D20) |
| J7 | Offline-mode or cracked client. | Kit has `online-mode=true`; offline client refused. | MANUAL | Kort |
| J8 | Operator takes damage rules: an op in creative changes blocks in camp. | Spawn protection 0 and adventure mode make this a vanilla operator concern; no mod behavior to test beyond adventure default. | MANUAL (low priority) | Kort |
| J9 | RCON. | Off (`enable-rcon=false`). | LATER (B9) | PASS - enable-rcon=false, no RCON listener |

## K. Edge cases

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| K1 | Ten or more players on a 16-slot server during a 4v4. | Eight commanders, the rest in camp, none in battle. | AUTO `AdmissionPolicyCheck`, `LobbyRulesCheck` | PASS - AUTO AdmissionPolicyCheck / LobbyRulesCheck |
| K2 | One human with bots filling five seats; headless bots not counted as players. | Match starts; no bot reward or record. | AUTO `LobbyRulesCheck` (bot fill); bot rewards: GAP, no named check | PASS - AUTO AdmissionPolicyCheck / LobbyRulesCheck |
| K3 | Leave and join during the ten-second countdown. | Countdown stops or restarts as G3. | AUTO `LobbyRulesCheck` | PASS - AUTO AdmissionPolicyCheck / LobbyRulesCheck |
| K4 | Host leaves the room. | Host seat passes or room closes without a stuck lobby. | LATER (only `handHost` rules are checked) | PASS (scratch server 2026-11-03): host `social:exit` -> room stays and the host seat passes to the guest (`host:true`); after the guest (host again) disconnected, the room stays with "Waiting for disconnected members to reconnect" (reconnect reservation, by design), no stuck error |
| K5 | Shrink the room below its commanders; move a party to a full side. | Refused with a message. | AUTO `LobbyRulesCheck` | PASS - AUTO AdmissionPolicyCheck / LobbyRulesCheck |
| K6 | Same House picked by both sides; mixed banners. | Allowed per design; House banner derived per side. | AUTO `LobbyRulesCheck` (`houseBanner`) | PASS - AUTO AdmissionPolicyCheck / LobbyRulesCheck |
| K7 | Rewards: win, loss, quit, disconnect, visitor, rematch. | Granted once per participant, saved, none for visitors. | HARN `visitor-no-reward`; the rest LATER | NOT RUN - a live win, loss, quit, disconnect and rematch cannot be forced without a `finish()` driver, and pro-rata rewards need rounds of at least 3 minutes seated (6 scenarios); only `visitor-no-reward` and the campaign harness cover parts |
| K8 | Campaign: clear a battle twice. | `battlesWon` is a set; no double bonus. | AUTO `ProfileMigrationCheck` (round trip, sanitize) for data; play: HARN `CampaignChecks` | PASS - AUTO ProfileMigrationCheck + HARN CampaignChecks |
| K9 | Pad: step on, countdown, step off. | Ready state clears. | HARN `pad-ready`, `pad-step-off` | PASS - HARN pad-ready, pad-step-off |
| K10 | Modes: Domination, Siege, Convoy, Long March battles 2 and 4. | Each mode plays one full round. | HARN `GRAVES_PHASE4_CHECKS=convoy`, `march-modes`, `modes` (Domination is the `modes` suite, check `p4-modes-domination`) | PASS - convoy, march-modes and modes (Domination, `p4-modes-domination`) pass; the earlier FAIL was a wrong suite name in the matrix |
| K11 | Chunks and entity cleanup after many rounds. | No leftover entities or banners after a round or server stop. | HARN `restart-clean-lobby`; round boundary: LATER | PASS (scratch server 2026-11-03, RCON `execute if entity`): before round 0 non-player entities; in camp 2 (camp villager, text display); during the round 39 husks (troops) steady for 15 min; right after the round 2 (husks 0); commander gone 0. Server stop then restart: 0 non-player entities with nobody online, 0 husks with a commander in camp, camp decoration (4 villagers, 6 armor stands, 13 text displays = 23) identical before and after the restart, so nothing accumulates |
| K12 | Window, GUI scale and resolution extremes on the front menu and War Table. | Layout readable at 1440p and at GUI scale 2. | HARN `GRAVES_ROADMAP_FRONT_CHECKS=true`, AUTO `UiMathCheck`; looks: MANUAL (Kort) | Kort (visual): readability at 1440p and GUI scale 2 is a look; `UiMathCheck` already PASSes the numbers. Relabelled 2026-11-03, not run |
| K13 | Low-end client (low RAM or integrated GPU) with and without shaders. | Playable; record fps against the 60 fps target. | MANUAL (plan 5.5 owns the numbers) | Kort |
| K14 | Play with the pack on Linux or macOS. | Unverified; setup scripts are PowerShell only. | GAP (server kit is Windows only; clients are untested elsewhere) | GAP: no Linux or macOS machine and the setup scripts are PowerShell only. Relabelled 2026-11-03, not run |

## L. Six-player acceptance (hook to the existing plan)

Full details are in `docs/SIX-PLAYER-TEST-PLAN.md` (Kort skipped the 3v3 baseline on 2026-10-28; item 5.7 reschedules it). This matrix adds only the links.

| ID | Steps | Expected | Check | Run 2026-11-03 |
|---|---|---|---|---|
| L1 | Six humans, three matches, lobby to rewards to rematch. | Plan section 9 rows 1-2 pass. | MANUAL (Kort + friends) | Kort |
| L2 | Reconnect and restart rows of the plan (section 6) using H and I rows above. | Plan rows 5-7 pass. | MANUAL | Kort |
| L3 | Isolation. | Builder up on 25570; 25565-25567 and 25571 untouched. | MANUAL | Kort |
| L4 | Defect list at the end. | No open blocker or major (plan 5.6 exit: "QA matrix green"). | MANUAL | Kort |

---

## Counts

Rows: A 8, B 10, C 14, D 22, E 11, F 8, G 12, H 11, I 14, J 9, K 14, L 4 = 137.

By primary tag: 80 rows are covered by an existing AUTO or HARN check, 17 are LATER, 36 are MANUAL, and 9 rows carry a GAP note (the tags overlap, so these do not sum to 137; four rows are mixed or note-only).

## Gap list: rows with no automated check that should get one

Ordered by value for the 1.0 exit.

1. **Server kit contents (B7 to B10, C1 to C6).** Nothing tests `tools/export_server.py`. A Python test with fixture files should assert: the allowlist, no `eula.txt` anywhere, `start-server.ps1` refuses without `eula=true` and without the launcher, the pinned URL and SHA-256 in `setup-server.ps1`, and the `server.properties` values. This also protects the rule that the kit never supplies an EULA.
2. **Migration end to end on a real server (D16 to D18).** Standalone checks prove the transforms; no harness run loads a v1 or v2 file from `gravesandglory-profiles/` on a server restart. Add a fixture-driven harness run using the existing `profile-v1-*.json` files and a hand-made v2 file. Also confirm the `partial` and `cache` fixtures are exercised.
3. **Dedicated import gate and size limit (D19, D20, J6).** `importAllowed` and the 16000-character limit have no check. Extract the decision into a small pure function and add a standalone check.
4. **Interrupted match rules (I7, I9, H8).** Ruled 2026-11-02 (partial reward by time played; 2-minute slot hold) and covered by `MatchRulesCheck`. A harness case that stops or kills a live match is still open.
5. **Hard failure and torn writes (H6, I8, I10, I11, D21).** Profile save is atomic with a `.bak`, but no check kills the process mid-save, fills the disk, or corrupts `gravesandglory-parties.json` and asserts the start-up message.
6. **No-mod client at login (F7)** and **wrong level name warning (C11).** Add a harness case or a log assertion on a headless server whose `level-name` is wrong.
7. **Reward and bot rules (K2, K7).** Reward granted once per participant across win, loss, quit and rematch; bots never rewarded. Only the visitor side is covered.
8. **Build-mode commands (J2, J3).** `/warp` is ops only (ruled 2026-11-02, `MatchRulesCheck`); J2 still needs a build-mode server run.
9. **Server status probe (C8)** is not part of any check; wire it to `verify.py` against a headless harness server only if that run is made routine.
10. **Doc drift:** `docs/SETUP.md` says 15 standalone checks; there are 20. `SERVER-README.txt` and `SIX-PLAYER-TEST-PLAN.md` give different max-players (16 versus 6 [A]); decide which the release recommends.

## Questions for Kort

All three answered by Kort on 2026-11-02, plus the Iris and Sodium kit notices (kept):

1. I7, I9: a stopped or crashed match pays a partial reward, the participation gold pro-rated by the share played, no win bonus.
2. J3: `/warp` is ops only (level 2 or above).
3. H8: a disconnected player's slot is held 2 minutes, then released.
4. The server kit keeps the Iris and Sodium notices and `iris-source.zip`.
