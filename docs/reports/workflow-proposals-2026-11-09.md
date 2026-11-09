# Workflow proposals for Kort, 2026-11-09

From the workflow lead. Nothing here has been done; each item needs Kort's yes (relayed by the coordinator).

## Verify triage on `main` (step 1)

- **Clean `main` 54e5d339 passes verify fast: 65 of 65 steps** (run in the clean worktree `bb-workflow`, every step run even after a failure; report `bb-workflow/work/verify/20261109-144151-61056/result.json`). There are no real regressions to fix.
- **The `main` checkout fails only because of Kort's uncommitted files.** Reproduced in a throwaway worktree with a copy of his diff (the checkout itself was not touched): 2 of 549 Python tests fail, and the asset checks pass.
  - `test_audio_events.AudioEventsTest.test_every_variant_file_exists`: the five deleted `.ogg` files are still listed in `sounds.json`.
  - `test_mapengine_check.CheckTest.test_sample_builds_clean`: Kort's edit to `maps/sample/design.json` (river-1 removed and new pieces) no longer builds clean against the sample's expectations.
  - The CRLF-only `Spec.java` / `preflight.json` changes fail nothing; they only show as modified.

## P1. A separate worktree for Kort's map and asset edits

The `main` checkout (`bannerbound-mainmerge`) holds these uncommitted changes. Verify on a clean copy of `main` does not see them, so they are the reason runs in the `main` checkout fail, and a `land` fast-forward refuses any branch that touches the same paths.

| Path in the `main` checkout | What it is | Proposed destination |
|---|---|---|
| `maps/kingsfield/design.json` (+627 lines) | Kort's map engine work | Copied into `C:\Users\kortt\Melty\bb-kort-maps` (new worktree, branch `kort/maps`, cut from `main`); Kort commits it there when he is happy |
| `maps/sample/design.json` (river-1 removed, +344 lines) | Kort's map engine work | Same worktree and branch |
| 5 deleted sounds in `src/main/resources/assets/gravesandglory/sounds/` (`campaign_briefing_music_2`, `music_battle_drums_1`, `music_battle_grim_1`, `music_calm_1`, `overtime_music_1`) | Deleted, still listed in `sounds.json` and `tools/audio/PICKS.example.json` | The deletion is applied in `bb-kort-maps` (or a separate `kort/audio` branch) together with the matching `sounds.json` entries, so the asset check passes; Kort confirms these five are meant to go |
| 9 deleted pngs under `design-archive/ui-concepts/` and `docs/review/2026-11-04-attack-clips-contact.png` | Old concept stills and a review sheet | Same: deletion applied as a commit on Kort's branch if he wants them gone; otherwise restored |
| `src/main/java/gg/gravesandglory/Spec.java`, `sheets/preflight.json` | Line endings only (`git diff --ignore-cr-at-eol` is empty) | Nothing to keep: `git checkout -- <both>` restores them. See P4 for the permanent fix |
| `docs/superpowers/{plans,specs}/2026-11-09-review-booth*.md` (untracked) | Byte-identical to the committed copies on `anim/geckolib-rebuild` | The animation lead already plans to move them to the notes folder before landing |
| `docs/superpowers/{plans,specs}/2026-11-08-warlord-animations*.md` (untracked) | In no commit anywhere | Copied into the animation lead's worktree and committed there (Warlord work starts after the rebuild lands) |
| `docs/drafts/lore-bible-2026-11-09.md`, `chapter-1..5-outline-2026-11-09.md` (untracked) | In no commit; the lore lead has copies in `bannerbound-lore` | Committed on `docs/lore-lead` by the lore lead; then deleted from the `main` checkout |

Order, once Kort says yes: (1) create `bb-kort-maps` and copy the two `design.json` files and the deletions into it, then check byte equality; (2) only then restore those paths in the `main` checkout; (3) from then on the `main` checkout is integration only: `tools/land.py` fast-forwards it and nobody edits or runs a client in it. Kort runs the map engine editor from `bb-kort-maps`.

## P2. Rule conflicts (audit 3.5)

| Topic | Where | Says | Proposal |
|---|---|---|---|
| Routine effort | `WORKFLOW.md` (Kort 2026-11-07) vs global `~/.claude/CLAUDE.md` (2026-10-25) | Sonnet **high** for routine work, checks and fixes, vs Sonnet **medium** for routine implementation, tests and docs | WORKFLOW wins for Graves and Glory (it is newer and says it supersedes). Edit the global file's first bullet to add "Graves and Glory: see its WORKFLOW.md" so the two stop disagreeing |
| Stuck escalation | `WORKFLOW.md` vs global `CLAUDE.md` model ban (2026-10-28) | "xhigh is not the stuck escalation" vs "when stuck, escalate to Opus at xhigh" | One rule in both: stuck means Opus 5.5 at **high**; xhigh only when the brief names the task as needing it |
| Concurrent sessions | `WORKFLOW.md` line 19 vs Kort's decisions of 2026-11-09 | "one coordinator plus at most four helper sessions" vs one coordinator plus six leads (animation, textures, lore, props, workflow, UI) | Update WORKFLOW to "one coordinator plus six leads; leads use subagents, not sessions; nobody works in the `main` checkout" |
| Opus scope | `WORKFLOW.md` vs Kort's decisions of 2026-11-09 | "Opus medium for art, animation, UI and systems" vs "Opus medium for complicated or important work" | Replace WORKFLOW's list with Kort's 11-09 wording |
| Client ceiling | `WORKFLOW.md` (Kort 2026-11-01) vs `SETUP.md` | five harness clients vs "the existing four-client ceiling" | Five (Kort's later decision). Fix `SETUP.md`; its same paragraph also says the guard defers if *any* client is running, but `verify.py` defers only for a client from its own checkout: correct that sentence too |
| Fable ban | `WORKFLOW.md`, global `CLAUDE.md`, memory | Written three times, enforced nowhere | Add a PreToolUse hook in `tools/claude-hooks.json` that denies an `Agent` call whose `model` is `fable`, and a SessionStart check that warns when the session model is a Fable model. Small; build on your yes |

## P3. `land` and `STATE.md` adoption

`tools/land.py <branch>` (tests in `test-tools/standalone/test_land.py`) is the only way a lane reaches `main` from now on: rebase in the lane's worktree, verify fast there, fast-forward `main`, archive the lane's `work/` evidence under `<main checkout>/work/land/`, remove the worktree and branch, regenerate and commit `docs/STATE.md`. It never pushes. Each lead keeps `C:\Users\kortt\Melty\status\<lead>.md` (state, done since last, next, blocked, questions for Kort) current; `STATE.md` reads them.

## P4. `Spec.java` and `preflight.json` line-ending noise (proposal, not built)

Cause: `tools/preflight.py` lines 892 and 894 call `Path.write_text(...)` without `newline=`. On Windows that writes CRLF. `.gitattributes` says `*.java` and `*.json` are `eol=lf`, so every worktree that ran preflight shows both files as modified although `git diff --ignore-cr-at-eol` is empty. Fix: add `newline='\n'` to both calls, plus a test that runs preflight in a temp copy and asserts the outputs contain no `\r`. Existing dirty worktrees are then cleaned by each owner with `git checkout -- <both files>` (or by the next preflight run). `tools/land.py` already treats CR-only differences in these two files as noise. Two-line change; build on Kort's yes.

## P5. Faster `verify.py fast` (proposal, not built)

Measured on clean `main` 54e5d339 (2026-11-09): fast took **517 s**, all serial: Python tests 289 s, `ui-assets` 99 s, Gradle build 81 s, the 57 Java checks 34 s together, the rest under 15 s. Inside the Python tests, `test_gear_art.py` alone takes about 185 s, then `test_build_skins.py` 44 s, `test_preflight_rules.py` 35 s, `test_mapengine_check.py` 32 s. In order of payoff per effort:

1. **Make `test_gear_art.py` cheap.** It is about a third of the whole run. Find what it renders or rebuilds per test (likely the full gear art set several times) and build once per module (`setUpClass`) or compare against stored hashes. Expected saving: two to three minutes.
2. **Run independent steps in parallel.** After preflight, the Python tests (split by module across processes), the asset verifications (`ui-assets`, `skin-assets`, `clips`), the Java checks and the Gradle build do not depend on each other. Run them as concurrent subprocesses, each with its own log. With item 1 done, fast should drop from about 8.5 minutes to about 2.
3. **Gradle daemon for fast only.** `--no-daemon` costs a cold JVM and cold configuration each run. Allow the daemon for `fast` (keep `--no-daemon` for `release`), with `org.gradle.daemon.idletimeout` of 15 minutes so idle worktrees do not keep daemons alive. The client guard is unchanged.
4. **One JVM for the 57 standalone checks.** Only 34 s today, so low priority: a `CheckRunner` that calls each `*Check.main` in turn would save perhaps 20 s.
5. **`fast --changed`** (optional): when only `docs/**` or `art/**` changed since `main`, skip the Gradle build and the Java checks. `land` always runs the full fast.

Measure each change against the current timing on the same commit before keeping it.
