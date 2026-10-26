# Melty workspace optimization plan

Prepared October 25, 2026, from the workspace review. This is a proposed implementation plan; none of these steps has been executed as part of this request.

**Goal:** let a fresh Claude session quickly identify the right checkout, current task, rules, and verified build; reduce repeated context loading and review loops; preserve all unfinished work and useful evidence.

**Execution order:** preserve → reconcile instructions → organize active work → make verification repeatable → establish a candidate → archive → measure. Code refactoring comes last and remains incremental.

## Starting point and boundaries

The reviewed root is `C:/Users/kortt/Melty`. The main checkout is `C:/Users/kortt/Melty/bannerbound`, referred to below as **PROJECT**. At review time it was on `phase-1-solo-core` at `da669ee`. Recheck that before implementing: this is a working project, not a frozen snapshot.

The inventory found approximately 184,000 files, 70.22 GiB, 25 Git checkouts inside Melty, and ten checkouts with changes. The separate registered worktree at `C:/Users/kortt/Documents/gg-hx` is a dependency to account for, not a folder to modify or archive automatically.

Already applied in the preceding task: Sonnet default, a 200k compaction setting, global token guidance, updated main WORKFLOW.md, and several memory corrections. Preserve and evaluate those changes rather than applying them twice. Main HANDOFF.md and older worktree policies still need reconciliation.

This plan does not authorize publishing, pushing, merging gameplay branches, changing gameplay requirements, lowering art quality gates, or lifting the Opus-worker restriction. Existing approvals continue to govern those actions. Preparation, documentation and reversible tooling can proceed when implementation is requested; the genuinely destructive steps have explicit preservation gates below.

## Phase 1 — Preserve the current state

### 1. Refresh the inventory and identify active writers

1. Re-run `git worktree list --porcelain` from PROJECT.
2. For each checkout, record its resolved path, branch or detached HEAD, exact commit, staged/unstaged/untracked changes, and unique commits relative to the current integration base.
3. Identify active Claude tasks and builds from available session information. Do not assume a quiet process or clean Git status means a checkout is unused.
4. Inspect the substantive unfinished work first: `bannerbound-method`, `bannerbound-ui-hud`, `bannerbound-ui-p2`, and the main checkout. Recheck art and UI branches with unique commits.
5. Agree on one editor/owner for each checkout during the optimization. If another task is writing there, defer overlapping edits; do not interrupt it silently.

**Deliverable:** a refreshed inventory, used as the initial worktree registry in Step 5.

**Done when:** every registered checkout has a known disposition or is explicitly marked “unknown—preserve.”

### 2. Create a recoverable checkpoint

1. Record Git refs and create a repository bundle for committed history, including detached commits that need explicit preservation refs. Do not move existing branches.
2. Save staged and unstaged binary-capable patches separately for dirty checkouts.
3. Preserve untracked source and selected ignored assets, ledgers, settings, and evidence separately; a Git bundle or patch does not include them.
4. Use an approved backup location with enough free space. Do not blindly duplicate the entire 70 GiB tree onto the same nearly full drive.
5. Create a manifest containing original paths, hashes, sizes, and restoration instructions. Keep credentials out of shareable reports and Git archives.
6. Verify the bundle and restore a representative dirty-file checkpoint plus a selected ignored asset into a temporary directory. Confirm hashes.

**Deliverable:** verified checkpoint and restoration manifest.

**Done when:** committed, dirty, untracked and selected ignored content each have a demonstrated recovery path. No cleanup before this gate.

## Phase 2 — Establish one current set of instructions

### 3. Rewrite HANDOFF.md around current facts

**Modify:** `PROJECT/docs/HANDOFF.md`.

1. Preserve its current contents as a dated historical document under `docs/archive/`.
2. Compare its claims with Git ancestry, the latest task ledgers, actual files, and build evidence. Mark uncertain claims as uncertain.
3. Remove obsolete “next” instructions for merges and tasks already completed. Do not execute them merely because the handoff lists them.
4. Replace repeated model/effort/process rules with a reference to `docs/WORKFLOW.md`.
5. Keep the current handoff to roughly 1–2 pages: current base, active tasks, next three actions, blockers, latest candidate, and relevant evidence links.
6. Separate “implemented,” “reviewed,” “tested on this commit,” and “accepted by Kort.” One does not imply the others.

**Done when:** a fresh reader can identify the next real task without reconstructing the October 19–24 history. The file no longer directs unlimited agents or Opus-max reviews.

### 4. Remove conflicting policy copies

**Inspect:** global Claude instructions, PROJECT/CLAUDE.md, `docs/WORKFLOW.md`, project memories, active worktree copies, and workflow skill instructions.

1. Keep global preferences in the global Claude file; keep modpack-specific policy in PROJECT/docs/WORKFLOW.md.
2. Keep CLAUDE.md short: project identity, navigation, essential constraints, and a pointer to the detailed workflow.
3. Resolve the two-versus-five review-round conflict explicitly at project scope. Do not edit installed plugin caches as the permanent solution; updates can overwrite them.
4. Remove or mark superseded historical instructions in active entry-point documents. Historical archives can retain old wording if clearly labeled non-current.
5. Bring active worktrees onto the same policy using deliberate file-level updates or an appropriate policy-only commit. Do not reset, rebase, or merge gameplay code just to synchronize instructions.
6. Preserve the no-Opus-worker rule and xhigh art requirements. Routine effort defaults must not accidentally override those exceptions.
7. Keep the local absolute canonical pointer for existing worktrees during transition. Document a portable repo-relative route for future clones so another machine does not depend on Kort’s C: drive.
8. Update stale memory descriptions as well as their bodies; summaries can steer a session before the full memory is opened.

**Done when:** every active checkout points to the same current policy and no active instruction simultaneously requires an incompatible model, effort, or iteration limit.

## Phase 3 — Make active work easy to find

### 5. Build a small worktree registry

**Add:** a machine-readable local registry, for example `C:/Users/kortt/Melty/workspace-state.json`, and a short `C:/Users/kortt/Melty/START-HERE.md` linking to it and PROJECT.

1. Record each path, branch, task/owner, base commit, status, unique commits, dirty-state summary, and evidence location.
2. Use a small status vocabulary: active, blocked, completed-awaiting-integration, integrated-awaiting-archive, historical, unknown.
3. Derive Git facts from Git. Keep only owner/task intent as manual fields; avoid a second hand-maintained commit ledger.
4. Explicitly label `minecraft`, `bannerbound-pre-rename`, `bannerbound-diag`, and `flop-formation` as legacy, diagnostic, or separate projects as appropriate.
5. Identify which checkout is the integration base and which build is suitable for playtesting. Do not infer either from names such as “preview” or “main.”

**Done when:** a worker can select its checkout and owned files from one place. The registry never treats “merged” as equivalent to “safe to delete.”

### 6. Separate active questions from historical decisions

**Modify:** `PROJECT/docs/QUESTIONS-FOR-KORT.md` and relevant archive/index links.

1. Move answered historical questions into a dated archive, retaining original answers and context.
2. Keep only unresolved questions in the active file.
3. Give each question an ID, affected task, recommended answer, and whether it blocks progress.
4. Put settled behavior into the relevant maintained spec; use links rather than repeating it throughout several ledgers.
5. Update HANDOFF.md to name only current blockers. Essential questions must remain visible even when other independent work continues.

**Done when:** an answered question cannot easily be mistaken for a new decision request, and every actual blocker has a clear owner and next action.

### 7. Preserve essential local decisions in the repository

1. Review `.superpowers/sdd/*/progress.md` for accepted decisions and completed-task summaries that exist nowhere tracked.
2. Move only those durable summaries into the existing specs, handoff or a compact tracked decisions file.
3. Preserve commit ranges, acceptance status, exceptions, evidence paths and hashes where useful.
4. Leave raw conversations, large diffs, worlds and bulk captures outside Git.
5. Record versions and sources of required Claude skills/hooks in setup documentation. Decide which small project-owned configuration files should be tracked selectively; do not unignore all of `.claude/` or commit machine credentials.

**Done when:** a clean clone can explain the current design and work status without depending on a hidden ledger. Raw evidence still has a separate recovery path.

## Phase 4 — Reduce the cost of each task

### 8. Standardize brief worker assignments

**Modify:** the project's existing brief convention, rather than introducing another planning framework.

Use this compact structure:

```text
Outcome:
Checkout and starting commit:
Owned files / boundaries:
Required decisions and exact reference sections:
Acceptance checks:
Model and effort; applicable restrictions:
Stop condition / maximum revision rounds:
Return: result, changed files, commit if authorized, tests, blockers, report path.
```

1. Let the main agent handle small, low-risk edits directly.
2. Delegate substantial independent tasks or noisy investigations.
3. Give each worker one bounded deliverable; retire it after a useful handoff.
4. Resume it for a related fix, not an unrelated milestone.
5. Batch actionable feedback. Do not wake a parked worker for acknowledgments.
6. Keep the existing concurrency ceiling as a ceiling, not a target that must stay full.

**Done when:** the next three routine tasks use short briefs and do not need whole-plan dumps or the full parent history.

### 9. Make verification and review proportional

1. For routine work, use existing targeted checks plus one focused review where review is warranted.
2. Re-review changed areas and unresolved findings, not the full historical branch each time.
3. Stop automatic iteration after two fix rounds. Record minor residuals; escalate or block on material unresolved issues without declaring the work complete.
4. For art, preserve quality thresholds and xhigh effort, but request observable corrections and one contact sheet per round.
5. If art still fails after the bounded rounds, present the concrete tradeoff to Kort. A model's numerical score is not a reason to keep generating indefinitely.
6. Retain stronger checks for save formats, protocol changes, rewards and other high-impact behavior.

**Done when:** the review package states what was checked, what changed, and why another pass is necessary if requested.

### 10. Evaluate the smaller-context trial

1. Verify that the installed Claude version recognizes the current compaction settings and that project/session overrides do not silently supersede them.
2. Keep the current 200k setting as an experiment, not an assumed universal optimum.
3. At natural milestones around 100–150k context, assess whether a short handoff or targeted compaction would remove obsolete history.
4. Preserve decisions, exact paths, tests, unresolved failures and next steps. Do not force a restart mid-investigation solely to hit a number.
5. Compare usage and rework across similar deliverables. If lost context creates repeated investigation, revise the threshold or brief rather than tightening it further.

**Done when:** changes are judged by usage per accepted deliverable and defect/rework rate—not cache-hit percentage alone.

## Phase 5 — Make builds and verification reproducible

### 11. Add reproducible setup and one fast verification entry point

**Modify/add:** project setup instructions, a pinned Gradle wrapper, and a small verification runner under `PROJECT/tools/`.

1. Add wrapper files for the project's established Gradle version, verifying the distribution integrity. Do not upgrade Minecraft, Fabric, Loom or Java as part of this task.
2. Document Java 17 discovery and required Python packages with supported versions. Avoid relying exclusively on one cached Gradle directory or absolute JDK path.
3. Provide one fast command that runs schema/preflight validation, the existing Python tests, standalone Java checks and compilation in an explicit order.
4. Preserve process exit codes. Save full logs to files; return concise failure excerpts and a short summary.
5. Explain that preflight generates Spec.java and preflight.json. Prefer an isolated verification checkout/snapshot when another worker owns those files.
6. Compare generated files before/after so expected regeneration is distinguishable from accidental changes. Do not discard them blindly.
7. Add an offline mode where dependencies already exist, with a clear missing-dependency error.

**Done when:** the command succeeds from a fresh isolated checkout using documented prerequisites, and an intentionally failing check produces a nonzero exit and useful diagnosis.

### 12. Separate fast checks, game checks and release checks

1. Define three explicit modes: fast, game, and release.
2. Fast mode must not launch Minecraft or publish anything.
3. Game mode must preserve the existing player-session and harness-client limits, use isolated run directories, and identify the tested source state.
4. Release mode must validate the package fingerprint, test-harness exclusion, dependency manifest and artifact readback.
5. Keep saves/migration and multiplayer tests explicit where relevant; a passing unit suite does not replace them.
6. Once the local fast runner is stable, prepare CI using that runner. Remote activation follows existing repository permissions; do not duplicate the checks in a separate implementation.

**Done when:** a “pass” names its test tier and source identity. The existing 153-test result remains a baseline for the reviewed snapshot, not a permanent certification.

### 13. Create a current candidate manifest

**Add:** a small structured record such as `PROJECT/docs/releases/current-candidate.json`; raw output stays in ignored artifact folders.

1. Identify the intended integrated candidate and finish or explicitly exclude remaining active UI/art changes through the normal approval process.
2. Record version, exact commit, dirty-state indicator or source fingerprint, artifact path, SHA-256, build time, verification commands/results, known issues and acceptance status.
3. Mark the old `sheets/verification.json` as historical or migrate its role so it cannot claim the current release was tested using legacy results.
4. Point HANDOFF.md and the workspace entry page to the candidate record.
5. Keep prior candidate records and packages for rollback. Do not overwrite a known-good artifact with a different build under an indistinguishable identity.

**Done when:** “Which build should I play?” has one unambiguous answer, and its checks refer to that exact build.

## Phase 6 — Validate the integrated experience

### 14. Perform one bounded end-to-end playtest

1. Install the candidate into an isolated launcher profile.
2. Launch, enter camp, recruit/equip, start a match, issue commands, finish, receive rewards, save, exit and return.
3. Check that the expected troops/UI are present and that persistence works.
4. Record usability problems separately from implementation defects and art preferences.
5. Repeat a focused multiplayer/save-migration check when candidate changes require it.
6. Treat balance simulation results as build-specific evidence. Recheck the integrated build where routing/combat changes invalidate old conclusions; do not rerun every expensive simulation without a reason.

**Done when:** blockers are known and tied to an exact artifact, and Kort has a useful candidate to judge. This is a bounded validation pass, not permission to redesign the camp or expand the campaign.

## Phase 7 — Reduce clutter after preservation

### 15. Archive finished worktrees deliberately

1. Start with integrated, inactive checkouts that have no unique work after the refreshed inventory.
2. Check staged, unstaged, untracked and ignored contents; include external references from ledgers and docs.
3. Preserve unique commits, selected local files and evidence with a tested archive manifest.
4. Update registry entries and links to the archive location before removing the checkout.
5. Use Git's worktree removal mechanism only once its safety checks pass. Do not force-remove dirty checkouts to make the inventory look clean.
6. Keep detached/dirty diagnostic work and active UI/art branches until they have an explicit disposition.

**Done when:** every removed checkout can be restored with its important contents, and no active task or document relies on its old location.

### 16. Apply an evidence retention policy

1. Classify output into required fixtures, accepted evidence, unresolved-failure evidence, reproducible intermediates, and superseded captures/worlds.
2. Keep accepted contact sheets, regression cases, final artifacts and the data needed to substantiate important claims.
3. Archive old run worlds and repeated media takes first; these dominate the disk footprint.
4. Verify archive hashes and sample extraction before deleting originals. Confirm resolved paths are within the intended workspace; avoid cross-shell deletion scripts.
5. Rewrite evidence links or retain an index mapping old paths to archives.
6. Measure actual reclaimed space. Do not impose a promised GiB reduction before retention decisions are made.

**Done when:** active folders are easier to navigate, accepted evidence is still findable, and storage reductions have not destroyed reproducibility.

## Phase 8 — Maintain the improvement

### 17. Refactor only the next maintenance hotspot

1. Choose the hotspot affected by the next real feature or bug; do not start a blanket architecture rewrite.
2. Candidates include packet handling in GravesAndGlory.java, persistence/match responsibilities in Campaign.java, and page implementations in WarTablePages.java.
3. Establish the relevant existing tests, extract one responsibility without changing behavior, and run the appropriate checks.
4. Format only touched handwritten code consistently. Leave generated Spec.java under its generator.
5. Keep each refactor reviewable and separately reversible from gameplay changes.

**Done when:** a future task can modify a smaller, clearer area with unchanged behavior and passing checks.

### 18. Measure a small sample and settle the defaults

For the next five comparable deliverables, record only:

- task and accepted outcome;
- model/effort and number of worker runs;
- available token/cost metrics and typical context size;
- fix/review rounds;
- defects or rework after acceptance.

Use a short entry in an existing task report or a small CSV; avoid creating a new reporting bureaucracy. Compare equivalent work, not a documentation edit with a major combat feature. Do not sum duplicated transcript records or equate API-equivalent dollars with subscription charges.

**Done when:** defaults produce less repeated reading/review without degrading accepted quality. Keep useful changes; revert counterproductive ones from the checkpoint. No recurring automation is required unless explicitly requested.

## Suggested implementation batches

| Batch | Steps | Outcome | Gate before moving on |
|---|---|---|---|
| A: Current truth | 1–4 | Recoverable state and consistent handoff/policy | No active instruction contradictions |
| B: Active work | 5–10 | Clear checkout ownership and bounded workers | Three routine tasks can use concise briefs |
| C: Reproducibility | 11–13 | Repeatable checks and identifiable candidate | Tested artifact has a recorded identity |
| D: Playability | 14 | Useful integrated feedback | Blockers tied to the candidate |
| E: Cleanup | 15–16 | Fewer active folders and retained evidence | Restore checks pass before deletion |
| F: Incremental improvement | 17–18 | Lower maintenance/usage cost | Quality and rework remain acceptable |

For a short first implementation session, complete Batch A. It addresses the strongest verified cause of wasted agent work without touching gameplay code or deleting anything.

## Completion criteria for the workspace optimization

- A fresh session can find the current task, correct checkout, policy and candidate from a short entry path.
- No active instruction file directs obsolete work or conflicts with current model restrictions.
- Every retained worktree has a purpose; every archived one has a verified recovery record.
- Durable project decisions are available from the repository; raw evidence has a separate preservation plan.
- Fast verification is repeatable and does not unexpectedly launch the game or publish.
- Candidate verification is tied to exact source/artifact identities.
- Workers have bounded tasks and review loops; smaller-context settings are evaluated rather than assumed beneficial.
- The mod's accepted quality requirements, saves, unfinished work and existing permissions remain preserved.
