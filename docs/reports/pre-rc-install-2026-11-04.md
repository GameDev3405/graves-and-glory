# Pre-RC install dry run, 2026-11-04 (board row 6.3)

Not a release. Nothing tagged, pushed or published. Branch `qa/pre-rc-install` from main `22d682f4`. Scratch version string `1.0.0-prerc1`, set only in the worktree's `gradle.properties` plus an untracked `docs/RELEASE-1.0.0-prerc1.txt` (neither committed). Source identity `037c36bb...`. Outputs: `dist/` of this worktree (gitignored; main's `dist/` untouched). Scratch runs, logs and the exact server/launcher folders: `work/prerc1/` (gitignored).

## Artifacts
| File | Bytes | SHA-256 (first 8) |
|---|---|---|
| graves-and-glory-1.0.0-prerc1.zip (player pack) | 63,767,345 | 31b4d5c3 |
| graves-and-glory-1.0.0-prerc1.mrpack | 33,006,711 | f3c3b589 |
| graves-and-glory-1.0.0-prerc1-curseforge.zip | 48,839,331 | 4affbb54 |
| graves-and-glory-1.0.0-prerc1-server.zip | 36,245,157 | 8b734a94 |
| mod jar | 19,699,925 | |

Build: `gradlew build -x test` 1 m 4 s; `package.py`, `verify_package.py` (63 entries), `export_modrinth.py`, `verify_modrinth.py` (14 downloads), `export_curseforge.py`, `verify_curseforge.py`, `export_server.py` (57 entries read back) all passed, in that order. Tier gate: "stopgap none".

## What ran
- Fresh-profile mrpack install (the 0.5.0 launcher-test scripts; vanilla 1.20.1 + Fabric Loader 0.16.14 runtime reused from that test, instance folder empty): 14 of 14 downloads verified by size, sha1, sha512; 49 override files; server-overrides ignored. Boot (offline user, Java 17): title screen in 25 s, 72 mods incl. `gravesandglory 1.0.0-prerc1`, `resourcePacks` has `gravesandglory:armory`, Excalibur zip present in `resourcepacks/` (not auto-enabled, as in 0.5.0). Only expected noise: offline auth 401, Realms, skinlayers class WARN. Log: `work/prerc1/launcher/boot-latest.log`. Process killed by exact PID.
- Server kit extracted to a scratch folder: C1 (start before setup), C3 (setup twice), C5 (no EULA), C6 (`-MemoryGB 1`) behave as the matrix expects; C2 downloaded the pinned Fabric launcher, hash verified. EULA written only in that scratch folder; port 25661, online-mode=false for the scratch run. Server reached "Done (2.034s)" (22 s total load), camp built, 4 server dependency jars plus the mod loaded, no stack traces.
- `profile_on_join.py` against the kit server with the packaged jar: all 8 checks PASS (schema 4, 10 Commons, 6 starter gear worn, relic in slot 0, rejoin unchanged).
- Real client join: harness client (`ConnectOnStart`, case `match`, same source identity) logged in to the kit server and stayed connected until its harness exit (server log `JoinKit joined the game`). Harness pass lines were not written for this case (expected for `match`).
- Dev-client harness, same source identity, one client at a time: `GRAVES_PHASE1_CHECKS=ui-gear` (Arsenal opens and closes with the gear key, etc.) 15 pass, 0 fail; default Phase 1 `combat` suite (the `verify.py game` suite: a battle starts and runs) 39 pass, 0 fail. Evidence: `work/prerc1/hc-ui-gear/evidence/local-checks.txt`, `work/prerc1/hc-combat/evidence/local-checks.txt`.
- VERIFY_PLACEHOLDER

The packaged jar excludes every harness class, so Arsenal and battle checks cannot run against it; they ran in a dev client built from the same source identity.

## Findings
1. **Credits gap (fixed).** Four Pixabay music tracks and the combat, magic, clock and drum cues ship in the jar, but `THIRD_PARTY.md` said "original sounds: Kort" and the pack did not carry `docs/AUDIO.md` (the only place the per-file author credits live). `package.py` now credits Pixabay and bundles `docs/AUDIO.md`; `package_policy.py` now fails a strict package without both. Test: `test_pixabay_audio_credit_is_required`. Note `docs/AUDIO.md` is an internal doc that ends up in all archives; consider a trimmed `AUDIO-CREDITS.md` before the RC (Kort's call).
2. **Jar size jump.** 0.5.0 jar 12.83 MB to 19.70 MB (+53%). About 6.0 MB is the four music tracks (calm 2.7, drums 1.4, grim 1.0, epic 0.7) plus 0.1 MB of short cues; the rest is textures/models/classes. Expected, not a defect. Player zip 63.8 MB.
3. **Version and release notes.** `mod_version` is still 0.5.0, and `package.py` hard-requires `docs/RELEASE-<version>.txt`. No `docs/RELEASE-1.0.0.txt` exists (only `docs/releases/1.0.0-notes-DRAFT.md`), so a 1.0.0 package would stop with a bare FileNotFoundError. The RC needs the version bump and a player-facing RELEASE-1.0.0.txt. Not changed here.
4. **Stale bundled README.** `docs/README.md` in every archive is the repo README: its first lines say `main` is the "0.5.0 development integration", "not a published release", title "Graves and Glory 0.5.0 (Houses development integration)", with 0.3 and 0.4 candidate history. Needs a player-facing rewrite before the RC. `docs/UI_REDESIGN.md` (internal design note) is also bundled.
5. **Server kit is two products.** The exported kit has `setup-server.ps1`, `start-server.ps1`, `server.properties` (localhost:25565, online-mode=true, no whitelist) and a `SERVER-README.txt` that still says "local dedicated-server candidate", "Java 21 used in verification" and mentions Starlink. The richer `tools/server-kit/` (start/stop/backup/restore/update/gate, port 25580, whitelist) is not in the zip; its README tells the operator to extract the export, delete `server.properties` and `start-server.ps1`, and copy the other scripts over. Port 25565 is also the port Kort's Kingdom servers use. Decision for Kort: ship the second kit in the zip, or leave the manual overlay.
6. **Not portable to another machine or clean clone.** (a) `dependencies/platform-lock.json` stores the Fabric API `localPath` as an absolute `C:\Users\kortt\.gradle\...` path, so `export_modrinth.py` and `export_server.py` only work on this user's Gradle cache. (b) `dependencies/{mods,shaders,resourcepacks,sources}` are gitignored; the main checkout `bannerbound` has no `resourcepacks/` (Excalibur) at all, so a release built there fails until the files are copied in (I copied them from sibling worktrees). Left as is; release must be built where the files exist.
7. **mrpack text.** The index summary still says "Local release candidate." and the CurseForge zip mod fetch still needs an API key (Q75), so a real CurseForge import is still Kort's step. Modrinth download hosts all passed the allowlist.
8. Not tested: shaders on/off (A7), Linux/macOS (K14), real launcher (Prism/Modrinth app) import, CurseForge import, a second client joining the kit.

## Fixed in this branch
`tools/package.py`, `tools/package_policy.py`, `test-tools/standalone/test_package_policy.py` (finding 1).

## Cleanup
Started and stopped: one mrpack client (PID 13424), one kit server (PID 28372 under 61444), harness gradle clients. Java PIDs 17380, 29360, 4580, 6048 and ports 25565-25567, 25571 untouched. No desktop computer-use.
