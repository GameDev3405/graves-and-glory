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

Fast runs preflight, the Python unittest suite, script checks, asset verification, 25 standalone Java checks, the mod build and harness compilation. It never launches Minecraft. Preflight regenerates Spec.java and sheets/preflight.json; hashes before/after are saved, never blindly discarded. Do not share a writing checkout with another worker.

Game runs fast then a real isolated Phase 1 combat client suite, requiring its exact completion marker. It is a smoke check, not six-player, visual or full lobby/rewards acceptance. The Windows process guard defers if a Minecraft client from this checkout is running (clients of other checkouts do not block it). No player process is terminated; only a timed-out process tree created by this runner is stopped. Runs live under work/verify/<timestamp-pid>/game. Runtime is capped at 30 minutes. The five-client ceiling across all worktrees remains (Kort 2026-11-01, confirmed 2026-11-09); this conservative runner uses one client and refuses concurrent clients.

Release runs fast then strict package.py and verify_package.py, checking source fingerprints, harness exclusion, dependency pins and archive readback. It does not permit stopgap art or upload. A package gate failure is an incomplete release, even if fast passes. Supply locally pinned dependency files listed by dependencies/manifest.json through the existing acquisition process; do not change the pins to make a package pass.

Every run saves result.json, exact commands/exit codes, full logs, source identity and generated-file hashes in work/verify. Failures return nonzero with a short excerpt. Preserve candidate artifacts under unique paths before rebuilding; update releases/current-candidate.json only with checks for that exact identity. Old sheets/verification.json evidence is historical unless its identity matches.

## Shared in-game check queue

Lanes run client suites and balance sims through `tools/checkq.py`, never by calling `tools/ingame_check_run.sh` or `tools/balance_sim.sh` directly. From the lane worktree: `python tools/checkq.py submit client --root <name> --suites <list>` (add `--env NAME=VALUE` for harness settings such as `GRAVES_PHASE1_CHECKS=geo-knight`) or `python tools/checkq.py submit sim --name <name> -- <gradle -P arguments>`. Submit prints the job id and its result path and starts the queue runner if none is alive. Then start `python tools/checkq.py wait <id>` as a background shell command (not a polling model turn): it exits with the job's exit code (124 timeout, 125 cancelled, 126 queue error, 3 unknown job) and the result is copied into the lane's own run root as `checkq-result.json` and `checkq.log` (`work/ingame-check/<root>/` or the sim's `work/method/<name>-<HHMMSS>/`). `python tools/checkq.py status` lists waiting, running and recent jobs in plain words (`--json` for tools); `cancel <id>` removes a waiting job or stops a running one. The runner starts jobs first come, first served while all Minecraft Java processes on the machine (Kort's own game, sim servers and clients started outside the queue each take a slot) plus queue jobs stay at or below five, one job per worktree, a sim on a free port of 25680-25699. It stops only the process tree it started for a job (30-minute limit or cancel) and never kills any other process; a timeout kill takes the job's own process tree, and if Gradle joined an existing daemon the Minecraft process may survive, so after a `timeout` result check `status` and the process list, and close a leftover client by hand only if it is from your own worktree. The queue lives in `C:/Users/kortt/Melty/.checkq` (`CHECKQ_DIR` overrides).

## Kort's inbox

One private page lists everything that waits on Kort, in priority order, about an hour a day. Each lead puts every such item on its own line in its status file (`status/<lead>.md`) in this exact form, and deletes the line once Kort answers: `- kort: <one plain sentence> | open: <https link, path or none> | min: <minutes> | prio: <1 now, 2 this week, 3 whenever>`. Missing minutes count as 5 and a missing priority as 2. A status file with no such line shows its old `questions for Kort` text as one item (marked old-style note). Booth batches without a `verdict.json` are picked up from every worktree automatically. Run `python tools/inbox.py` (options `--status-dir`, `--repo`, `--out`, `--budget 60`, `--json`); it writes `work/inbox/index.html` and prints a one-line summary. Publish/refresh: a session runs the command and republishes the file with the Artifact tool to the same URL (the URL is recorded in docs/HANDOFF.md once published).

Adding a new troop: the stage-by-stage checklist with its commands and checks is docs/NEW-UNIT.md.

## Graph and Claude setup

The viewer uses readable code-group names from docs/graph-labels.json. Names are matched to reviewed symbol anchors, so cluster renumbering does not rename a group to an unrelated subsystem. Newly discovered groups fall back to their actual file/symbol names. tools/graph_state.py refresh applies these names and regenerates graph.html after local clustering, with no external naming model. The names describe inferred code groups, not strict gameplay architecture boundaries.

Graphify 0.9.93 is installed as the uv tool graphifyy; no automatic upgrade. `python tools/graph_state.py refresh` uses local AST extraction and no-label clustering. `python tools/graph_state.py check` verifies content hashes, including dirty/untracked code inputs and the graph. Read known files directly; bounded graph queries help unfamiliar relationships. Groovy extraction is incomplete; use direct source for Gradle files.

tools/claude-hooks.json is the portable template. Merge its PreToolUse and SessionStart entries into project .claude/settings.json; preserve unrelated hooks/settings. The `Agent|Task|Workflow` and SessionStart entries run tools/model_guard.py, which enforces Kort's Fable ban: it denies a subagent or workflow that names a Fable model and warns a session that starts on Fable (a hook cannot change the session's model). It also checks the `model:` of the agent file a `subagent_type` names. It cannot see a mid-session `/model` switch (subagents without a `model` then inherit it) or a workflow that sets the model through a variable; `|| exit 0` keeps a checkout without the script from blocking Agent calls. A worktree's settings may point at the main checkout's graph_hook.py by absolute path; the hook picks the checkout whose graph is read from the query's `--graph` argument, else a leading `cd`, else the session cwd (walked up to its git root), so a session in a subfolder is not misreported as stale (test-tools/standalone/test_graph_hook.py). The hook emits an advisory only for an attempted graph query with stale/missing content. It never blocks direct reads/searches. Restart/reload Claude's project settings as required by the installed client; hook-process smoke checks do not prove a currently open session reloaded settings.

Installed project skills are preserved in the checkpoint: Graphify 0.9.93; mattpocock/skills revision 5beb35b (questionnaire, writing-for-agents, domain-modeling, architecture and codebase-design); nextlevelbuilder/ui-ux-pro-max-skill 56ea9cf. The UI skill is for UX checks only: approved medieval references win, no generated design-system/palette replacement. Do not edit plugin caches. Local settings and raw ledgers stay private; durable decisions are tracked in docs.

The global Claude default is the configured `sonnet` alias. Specific cheaper/stronger availability and account entitlement must be checked at dispatch time; no paid inference was run to probe it. The existing autoCompactWindow=200000 setting is retained as a trial, not confirmed active behavior.

CI can invoke the same fast command once a Windows runner has JDK17, Python dependencies and Gradle caches. Remote activation is deferred to repository authorization; this task does not duplicate the checks in a second CI implementation.

The bundled terminal Claude CLI reports 2.1.273; the idle desktop process uses a separate 2.1.308 installation. Local help confirms model aliases and effort flags, but account entitlements and active-session compaction/reloaded hooks were not inferred from CLI presence.
