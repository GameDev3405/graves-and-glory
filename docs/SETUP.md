# Setup and verification

Use Python 3.12, Java JDK 17 and the checked-in Gradle 8.8 wrapper. Minecraft 1.20.1, Fabric Loader 0.16.14, API 0.92.2+1.20.1, Yarn 1.20.1+build.10 and Loom 1.7.4 remain unchanged.

From this checkout in PowerShell:

```powershell
python -m pip install -r requirements-dev.txt
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot' # or your JDK 17
.\gradlew.bat build compileLocalTestTools -x test
python tools/verify.py fast --offline
python tools/verify.py game --offline
python tools/verify.py release --offline
```

The first build fills the Gradle cache and needs network access unless already cached. Offline verification reports missing dependencies as failures. JAVA_HOME takes priority; the runner can discover an installed Adoptium 17 or a PATH JDK 17. It does not change machine settings. Gradle distribution and wrapper JAR checksums were verified against services.gradle.org when installed; the distribution checksum remains pinned in gradle-wrapper.properties. Python dependency versions are recorded in requirements-dev.txt.

Fast runs preflight, 155 unittest cases (153 existing plus two verification-safety regressions), script checks, asset verification, 15 standalone Java checks, the mod build and harness compilation. It never launches Minecraft. Preflight regenerates Spec.java and sheets/preflight.json; hashes before/after are saved, never blindly discarded. Do not share a writing checkout with another worker.

Game runs fast then a real isolated Phase 1 combat client suite, requiring its exact completion marker. It is a smoke check, not six-player, visual or full lobby/rewards acceptance. The Windows process guard defers if any Minecraft client is running. No player process is terminated; only a timed-out process tree created by this runner is stopped. Runs live under work/verify/<timestamp-pid>/game. Runtime is capped at 30 minutes. The existing four-client ceiling remains; this conservative runner uses one client and refuses concurrent clients.

Release runs fast then strict package.py and verify_package.py, checking source fingerprints, harness exclusion, dependency pins and archive readback. It does not permit stopgap art or upload. A package gate failure is an incomplete release, even if fast passes. Supply locally pinned dependency files listed by dependencies/manifest.json through the existing acquisition process; do not change the pins to make a package pass.

Every run saves result.json, exact commands/exit codes, full logs, source identity and generated-file hashes in work/verify. Failures return nonzero with a short excerpt. Preserve candidate artifacts under unique paths before rebuilding; update releases/current-candidate.json only with checks for that exact identity. Old sheets/verification.json evidence is historical unless its identity matches.

## Graph and Claude setup

Graphify 0.9.93 is installed as the uv tool graphifyy; no automatic upgrade. `python tools/graph_state.py refresh` uses local AST extraction and no-label clustering. `python tools/graph_state.py check` verifies content hashes, including dirty/untracked code inputs and the graph. Read known files directly; bounded graph queries help unfamiliar relationships. Groovy extraction is incomplete; use direct source for Gradle files.

tools/claude-hooks.json is the portable template. Merge only its PreToolUse entry into project .claude/settings.json; preserve unrelated hooks/settings. Existing local worktrees use an absolute path to the main checkout's graph_hook.py, which reads the calling checkout from the Claude hook payload. The hook emits an advisory only for an attempted graph query with stale/missing content. It never blocks direct reads/searches. Restart/reload Claude's project settings as required by the installed client; hook-process smoke checks do not prove a currently open session reloaded settings.

Installed project skills are preserved in the checkpoint: Graphify 0.9.93; mattpocock/skills revision 5beb35b (questionnaire, writing-for-agents, domain-modeling, architecture and codebase-design); nextlevelbuilder/ui-ux-pro-max-skill 56ea9cf. The UI skill is for UX checks only: approved medieval references win, no generated design-system/palette replacement. Do not edit plugin caches. Local settings and raw ledgers stay private; durable decisions are tracked in docs.

The global Claude default is the configured `sonnet` alias. Specific cheaper/stronger availability and account entitlement must be checked at dispatch time; no paid inference was run to probe it. The existing autoCompactWindow=200000 setting is retained as a trial, not confirmed active behavior.

CI can invoke the same fast command once a Windows runner has JDK17, Python dependencies and Gradle caches. Remote activation is deferred to repository authorization; this task does not duplicate the checks in a second CI implementation.

The bundled terminal Claude CLI reports 2.1.273; the idle desktop process uses a separate 2.1.308 installation. Local help confirms model aliases and effort flags, but account entitlements and active-session compaction/reloaded hooks were not inferred from CLI presence.
