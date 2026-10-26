# Optimization execution report — 2026-10-25

Implemented the approved workspace/workflow optimization. Gameplay content, branches and release status were not silently advanced. The six-player slice is the next planning milestone, not a completed product feature. Five-deliverable/full-cycle efficiency measurements are future observations.

## Preservation and ownership
- Inventoried 26 registered worktrees (25 inside Melty; Documents/gg-hx is external and unchanged). Main integration base is phase-1-solo-core, originally da669eea15c2b647a07443925a92e9c16c3453be. Kort confirmed Claude idle. Other task ownership remains unknown—preserve.
- Checkpoint: C:/Users/kortt/Melty/backups/optimization-2026-10-25. Git bundle verified; all 23 distinct worktree heads available in a restored clone. Main dirty patch applied there and restored CLAUDE.md matched its original SHA-256 byte for byte.
- 55,273 selected local files archived and every archived member hash-verified. An actual ignored PNG was extracted and hash-verified separately. local-files.tar.gz is 21,918,347,507 bytes. Full paths/hashes/sizes and restoration instructions are in that checkpoint.
- 95,464 bulk files remain explicitly retained in place, including caches/runtime assets excluded from this checkpoint. This is not a complete backup of every ignored byte. No checkout or bulk evidence was deleted. Unknown ownership and incomplete bulk archival prevent safe cleanup; zero disk space reclaimed.
- Pre-edit policies/memories are also preserved under backups/optimization-policy-2026-10-25 with manifest/restore instructions. Some later worktree snapshots contain the new policy; those separate originals preserve the before-state. Small hook-settings backups live under each modified checkout's work/optimization/settings-backups.
- minecraft/bannerbound-pre-rename are legacy directories, bannerbound-diag is diagnostic, and flop-formation is separate; these non-Git directories were left in place.

## Installed changes
- One canonical WORKFLOW.md; concise HANDOFF, active BACKLOG, current questions, durable decisions, efficiency log and setup instructions. Historical handoff/questions/policies are preserved, clearly non-current. Selected local-only task reports are copied with provenance hashes.
- Local worktree entry points point to the canonical policy; older handoffs are labelled historical. Global/project Claude memory summaries no longer demand incompatible model bans or mandatory maximum art effort. Installed plugin caches were untouched.
- Sonnet alias remains the default; cheaper mechanical and stronger escalation models are permitted when actually available. One coordinator/two-helper trial, bounded briefs, two automatic correction rounds, planning checkpoints and unattended question parking are now the policy. No new recurring automation or automatic restart was installed.
- Approved UI/unit references copied into docs/visual-references with verified hashes. No new artwork or gameplay tuning was performed.
- Gradle 8.8 wrapper, pinned distribution checksum and verified wrapper JAR; pinned tested Python dependencies. Java 17/Fabric/Minecraft versions unchanged. .jem files are pinned to LF after a fresh Windows checkout exposed a fingerprint discrepancy from CRLF conversion. Their model contents are unchanged.
- tools/verify.py exposes fast/game/release tiers, source/generated hashes, full logs and exit codes. It refuses compilation during a running Minecraft client and only stops its own process tree on timeout. Strict release gates remain.
- Graphify 0.9.93 retained. Local AST-only graph rebuilt; no semantic/model calls. Content freshness includes dirty inputs; advisory hooks guard attempted stale graph queries without forcing queries before direct reads. Project-owned hook installer preserves unrelated settings.

## Validation
| Check | Outcome / evidence |
|---|---|
| Main fast tier | PASS: work/verify/20261025-195233-27672/result.json; 153 existing Python tests, 15 Java checks, assets, mod and harness build. |
| Main real Minecraft game tier | PASS: work/verify/20261025-195709-34528/result.json; isolated combat suite, 12 pass lines, no failures; source identity unchanged during run. |
| Fresh checkout | 155 Python tests, all 15 Java checks, assets and build passed. Initial run exposed .jem CRLF fingerprint mismatch; corrected with LF attributes and rerun, final result recorded below. |
| Failure propagation | Deliberately malformed design JSON in disposable checkout returned 1, saved preflight traceback and stopped; original bytes restored. work/optimization/intentional-failure.log. |
| Player/client protection | Concurrent-client probe returned 1 before compilation while our smoke client ran; no process terminated. work/optimization/concurrent-client-guard.log. |
| False-pass regression tests | Two new tests reject empty/partial/failing game evidence and verify missing graph is advisory only for graph queries. |
| Graph | 242 indexed source paths existed; two sampled declarations/line numbers matched. Dirty untracked probe made freshness fail; removal restored freshness. Configured hook command tested on fresh/stale states. work/optimization/graph-validation.log. |
| Strict package | Correctly FAILED: 26 troops still lack complete own tier art. work/optimization/release-gate.log. No stopgap bypass or publish. |
| Recovery | Bundle restore, dirty patch byte match, all archive member hashes, ignored PNG extraction verified; checkpoint JSON manifests retain details. |

Development JAR source fingerprint: 859a2a93b7de5dcca1a1da80d83704550f0f21c629fe47ed72e3022ff9ac2c14. Exact preserved JAR and SHA-256 are in releases/current-candidate.json. This is not a packaged, visually accepted or six-player-certified release. Historical test results do not silently transfer to an altered candidate.

## Limits and next questions
Groovy/Gradle AST extraction reports three syntax limitations; read those build files directly. Graph queries can return ambiguity/broad neighbors: narrow once or read known source; do not expand into repeated large queries. Other worktree graphs are not certified fresh by the main refresh.

Configured hooks were tested as processes, not inside a newly reloaded Claude session. Desktop Claude and terminal CLI versions differ (2.1.308 process versus 2.1.273 CLI); model aliases/effort flags exist, but stronger/cheaper account entitlement was not tested with paid requests. The retained 200k compaction setting remains an unverified trial.

No fresh visual acceptance, full lobby-to-reward loop, dedicated-server six-client test, reconnect/restart test or bot-quality baseline is claimed. These remain candidate-specific gates in the next milestone. Full weekly savings cannot be measured in this implementation session.

Questions: define slice team splits/content counts, map/lobby direction, performance/bot targets and six-client hardware at the next planning checkpoint. Resolve worktree ownership and bulk-evidence retention before deletion. Continue using existing approved visual exceptions; do not re-ask them. See QUESTIONS-FOR-KORT.md.

Local changes are committed only for this optimization's owned files. No push, gameplay merge, external message or publication occurred. Pre-existing dirty gameplay/planning files and other worktree policy updates remain preserved separately.

Final fresh-checkout verification: PASS, 155 Python tests, all 15 standalone Java checks, asset verification, mod and harness build. Evidence: `work/optimization/fresh-checkout/work/verify/20261025-205037-2532/result.json`. Fresh checkout and main both identify source `859a2a93b7de5dcca1a1da80d83704550f0f21c629fe47ed72e3022ff9ac2c14`. The final runner avoids running skin unit tests twice because unittest discovery already covers them.
