# Server kit report, task 3.5 (2026-11-01)

Result: kit works end to end on a local copy. Nothing was deployed to tjepkema-server or any live machine.

## What was done
- Reused the earlier kit (`work/server-kit-3-5`, commit 93aa5c0: start/stop/backup/restore/update/gate scripts for Windows and Linux, port 25580 defaults, Aikar flags, EULA never auto-accepted, README), cherry-picked onto this branch. The earlier session had only dry-run and stub-java tests.
- The mod already had a login-time `VersionGate` (protocol, version, build hash); the kit adds a jar pin (`mod-lock.json`).
- Added `ServerKitSmoke` (test-tools harness, listed in `release_config.py` forbidden list) for a one-client smoke join.
- Fixed a bug found by the real run: `update.ps1` failed ("not a readable jar") when the replacement jar had the same file name as the old one, because .NET resolved the relative path against the process directory. `Get-JarInfo` now resolves paths first.
- Added `docs/SERVER-RUNBOOK.md`.

## Local test (scratch folder, Fabric 0.16.14 launcher hash verified, Java 21 server, 4 GB heap, `online-mode=false` for the dev client only)
| Step | Result |
|---|---|
| gate before pin | refused, no `mod-lock.json` |
| gate after pin, start dry-run | OK; Aikar command printed, nothing started |
| Real boot (eula=true by Kort's yes, local only) | "Done" in 2.9 to 3.7 s, port 25580 |
| Non-whitelisted client | rejected by the version gate first (stale-build jar, see below) |
| Client with matching build, whitelisted | joined, stayed 100 ticks: PASS (also after restart) |
| Client sending wrong build hash | server log: "Refused incompatible client during login"; harness line says FAIL only because the client's disconnect text was the generic "Disconnected" |
| backup while running | refused |
| stop / backup / restart | clean save, verified zip, restart OK, playerdata persisted |
| update to different jar and back | OK after the fix; pin, backup, retired jars kept |
| restore | OK; old world kept as `.replaced-*` |

The first join was rejected because the server jar had been built before my source edits, so its source hash differed from the dev client: the gate working as intended.

## Not verified
- Linux `.sh` scripts (dry-run only). No Spark/MSPT numbers. No remote or Tailscale join, no firewall rule, no online-mode (real account) join. The "wrong build" harness case should assert on the server log or the client disconnect reason; its client-side text check is weak.
- Not done: deploy to `C:\GGMatch` (Kort's step).

Scratch server and logs: `scratchpad/srv` of this session; run logs in `work/server-kit-run-*.log`.
