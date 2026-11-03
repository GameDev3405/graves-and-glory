# Dedicated-server runtime checks, 2026-11-02

Branch `work/server-runtime` off `main` (`bd05fa2b`). Not pushed. No code changed. Kort's Q79 ruling: `eula=true` written only in a fresh scratch server folder (`<session scratchpad>/srv`, outside the repo and not `C:/GGMatch`), used for these two tests only. Processes started by me: 1 Minecraft server (java 21, 3 GB, via the kit's `start.ps1`) and 1 harness client at a time (Gradle `runClient -PlocalIntegration`, usernames SmokeAlpha / SliceAlpha). Other clients (46544 at start) and PIDs 29360, 4580, 6048 were not touched. All my processes were stopped at the end.

Build under test: `graves-and-glory-0.5.0.jar`, build `583bb576fd31` (main `bd05fa2b`).

## Test 1: server kit smoke (board 3.5 runtime) - PASS, with one deviation

Deviation: `tools/export_server.py` could not run. It needs `tools/package.py` first, which stops with `FileNotFoundError: dependencies/resourcepacks/Excalibur_V1.20.zip` (the pinned Excalibur pack is not in any local `dependencies/`; getting it is a download I did not make). So `dist/*-server.zip` was not produced. I assembled the same server contents by hand, as `export_server.py` and the kit README (install option 2) define them: `mods/` = the built jar + `fabric-api-0.92.2+1.20.1` + lithium, ferritecore, modernfix (the 3 other `server: required` rows of `dependencies/platform-lock.json`; fabric-api from the Gradle cache), plus `tools/server-kit/*`. The Fabric launcher was fetched from the pinned URL in `export_server.py`; sha256 `856a5f9f...cf113` matched. The export zip's own `setup-server.ps1`/`SERVER-README.txt` path is therefore still untested.

| Step | Result |
|---|---|
| `gate.ps1` before pin | refused: "No mod-lock.json" |
| `gate.ps1 -Pin` | pinned 0.5.0, build 583bb576fd31 |
| `start.ps1 -DryRun` | gate OK, Aikar command printed, nothing started |
| `start.ps1` without eula | created `eula.txt` (false), stopped with the EULA message |
| `eula=true`, `online-mode=false`, `server-ip=127.0.0.1`, whitelist of 2 offline UUIDs | scratch folder only |
| Real boot | mods loaded incl. `gravesandglory 0.5.0`; `Done (2.252s)` (29 s total with first-run downloads); camp terrain 1840 ms, pieces 377 ms; one "Can't keep up! 63 ticks behind" after the camp build (first boot, seen before) |
| Client join (`ServerKitSmoke`, case `ok`) | `[server-kit-smoke] PASS ok in world as SmokeAlpha`; server: `SmokeAlpha[/127.0.0.1:60362] logged in ... joined the game` ... `left the game` |
| `stop.ps1` | "Stopping java pid ... Stopped", all dimensions saved, `server.pid` removed (done twice) |
| `update.ps1 -Jar` with a different jar (same file name) | new jar OK, `preupdate` backup verified, old jar to `mods-retired/`, re-pinned, gate OK |

Not verified: Linux `.sh` twins, online-mode login, remote join, the export zip.

## Test 2: teleport-sync live check for `09d2bac` - PASS (fix confirmed, bug reproduced without it)

Scenario (from `docs/reports/slice-dryrun-2026-11-02.md`): a human enters a battle, which relocates him camp -> arena. Driven with `SliceClient` (cmd files): `connect`, `send social:room`, `send social:ready`, wait for the 10 s countdown and the battle. Real packaged-style server (no probe), flat world, Domination map 0, one human + bot fill. Same steps and server folder in both runs; the only difference is `TeleportSync.afterTeleport`.

| Run | Server jar | `moved too quickly` lines at battle start |
|---|---|---|
| Fixed (as in main) | build 583bb576fd31 | **0** (login `-107.5,66,0.5`, camp `-32.5,65,-303.5`, battle `-283,65,0`, 15 s in battle, no warning) |
| Control: `afterTeleport` body disabled (temporary local edit, reverted, not committed) | build 11c9024ac4cf | **1**: `[22:45:57] WARN: SliceAlpha moved too quickly! -250.5,1.0,303.5` at battle start |

The control line is the camp->arena jump (delta about 250 x 303 blocks), so the check measures the right thing. The fix removes it. One sample per run; log excerpts are above.

Not exercised here: the `/warp` path and `BuildMode` relocate (the other two call sites), a return to camp after a finished match (needs a ~3 minute round), the rejoin ready-flag half of the commit (covered by `TeleportRelocateCheck`).

## Notes and tooling

- First client connect in the control run dropped at login with the generic "Disconnected" and no server-side error (22:40:03); a second `connect` joined at once. Not seen elsewhere (4 other joins fine). Likely a client-side timing race right after the server restart; unreproduced.
- `SmokeAlpha`'s `serverSmokeOut` file was not written because I passed an MSYS path (`/c/...`) to the JVM; the console line `[server-kit-smoke] PASS` is the evidence. Use a Windows path (`C:/...`).
- Evidence (ignored `work/server-runtime/`): `console-fixed.log`, `console-nosync.log`, per-run `gradle.log` and `slice/client-SliceAlpha.log`.
- `tools/verify.py fast` not run: no source, tool or sheet change was committed (Gradle's preflight regenerated `Spec.java`/`preflight.json` with line-ending-only differences; restored).
