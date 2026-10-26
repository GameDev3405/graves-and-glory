# Task 4 report (lane P2): the recruit page, buy, stars, Pact, duplicates, Odds

**Status: DONE, with two ids still red that are not mine** (`p2-claim-house` and the `p2-screens-suite` roll-up, both waiting on lane P1's Task 1 claims). Everything else the task owns is green.

Worktree `C:\Users\kortt\Melty\bannerbound-ui-p2`, branch `ui-port-recruit`.

## Commit

- `1e79065` "War Table port Task 4: the recruit page, buy, stars, Pact, duplicates, Odds" (5 files, +757 -325): `RecruitPages.java`, `RecruitView.java`, `WarTableRecruit.java`, `Phase1Checks.java`, `Phase2Checks.java`. Committed by path. `Spec.java` and `sheets/preflight.json` showed as modified after every build (CRLF-only regeneration); I restored them with `git checkout --` and did not commit them.

## What changed

- The letter's tag (widget `choose`) sends what `RecruitPages.action(troopsRow, profile)` gives: `buy:<id>` ("Buy 300", Common and Rare, owned or not, until full stars), `star:<id>` ("Star <n>", owned Epic and Legendary), `legendary:<house>:pact` ("Pact") or `legendary:<house>:shards` ("<n> shards"). `unlock:` and `army:` are never sent (the suite fails if one is).
- An unowned Epic shows the words "From letters only"; a troop at full stars shows "Full stars" (`letter-none`). A tag that cannot be pressed shows its `tip` in up to two lines under it ("Need 300 gold. You have 50.").
- Double press: the page's `pending()` lock (the tag takes no second press until the camp's state moves or 30 ticks pass) sits on top of `CampScreen.action`'s 300 ms throttle. `p1-ui-recruit-keys` checks that one Enter on the tag sends exactly one `buy:` and the gold drops by `chosenCost` once.
- Duplicate roll: the reveal finds its troop by name, as before, but a roll whose `lastRoll.duplicate()` is true lays no broken seal. The letter lights the star it gained (animated over `ROLL_FILL_MS` 1200 ms) or the copy it counted, and writes `RecruitPages.outcome` ("Star 1 to star 2", "Copy 2 of 4 toward star 3") in place of the deploy line.
- Odds note: `RecruitView.note` states the real odds per rarity from `RosterMath.rarityRow`, the pity count, and "A troop you own gives a copy toward its next star."; "No duplicates" is gone. Enlist no longer greys out when a House is fully owned (a roll gives a copy).
- Notice: shows once on the ladder axis, fades, and is not replayed on reopen or after later state packets.
- Full roster: a profile with all 30 troops shows six owned rows (portraits, no silhouettes) on each of the five banners.
- Timings unchanged: `CampaignClient.ROLL_FILL_MS` 1200, `REVEAL_MS` 2400. `p2-roll-card` checks the letter appears within `REVEAL_MS` of the press.

## Red first

The first runs on the unchanged tree (`work/p2/results.txt`):

| run | root | result | ids |
|---|---|---|---|
| base-recruit (P1 `ui-recruit`) | `work/p2/r-baserec-225120` | pass 3 fail 8 | `p1-ui-recruit-layout`, `-layout-1080`, `-enlist`, `-choose`, `-dusk`, `-broke`, `-exception`, `-suite` |
| base-p2 (P2 `migration,screens`) | `work/p2/r-basep2-225308` | pass 15 fail 6 | `p2-roll-card`, `p2-notice-clears`, `p2-claim-house`, `p2-screens-suite`, `p2-full-roster`, `p2-migration-suite` |
| base-engine (P2 `engine`) | `work/p2/r-baseeng-225554` | pass 13 fail 2 | `p2-recruit-tabs`, `p2-engine-suite` |

These are exactly the merge report's ids. The new ids (`p2-recruit-choose`, `-star`, `-pact`, `-duplicate`, `-odds`) have no red run against the old code: they call APIs that did not exist there (`RecruitView.action`, `WarTableRecruit.texts()["choose-command"]`), so they could not compile against it. I say so rather than fake one.

## Verification (commands and results)

All with `--offline` and `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`. One harness client at a time, gate checked before each launch.

| command | result |
|---|---|
| `bash work/p2/compile.sh t4d` (`gradle compileJava compileLocalTestTools --offline -q`) | compile exit 0 |
| `bash work/p2/run.sh t4-recruit-3 PHASE1 ui-recruit t4rec3` (root `work/p2/r-t4rec3-234605`) | pass 11 fail 0: `p1-ui-recruit-layout`, `-layout-1080`, `-enlist`, `-choose`, `-dusk`, `-broke`, `-keys`, `-scales`, `-suite` all pass |
| `bash work/p2/run.sh t4-eng PHASE2 engine t4eng` (root `work/p2/r-t4eng-233424`) | pass 15 fail 0, incl. `p2-recruit-tabs`, `p2-roll-house`, `p2-engine-suite` |
| `bash work/p2/run.sh t4-ms PHASE2 migration,screens t4ms` (root `work/p2/r-t4ms-233732`) | pass 23 fail 3: `p2-notice-repeats` (my test's timing; fixed below), `p2-claim-house` (P1), `p2-screens-suite` (roll-up). Migration all green incl. `p2-full-roster`, `p2-migration-suite`; `p2-roll-card`, `p2-recruit-duplicate`, `-odds`, `-choose`, `-star`, `-pact`, `p2-notice-clears`, `-changes` pass |
| `bash work/p2/run.sh t4-scr PHASE2 screens t4scr` after the fix (root `work/p2/r-t4scr-234116`) | pass 17 fail 2: only `p2-claim-house` and `p2-screens-suite` (both need P1's Task 1 claims; `p2-notice-repeats`, `p2-troop-card`, `p2-roll-card-troop`, `p2-card-layout` pass) |
| `bash work/p2/standalone.sh` | every Java check exits 0 (UiMathCheck 179, HousesCheck 293, ProfileMigrationCheck 31, LevelMathCheck 141, BattleMathCheck 64 and the rest), `FAIL lines ... 0`, all five unittest modules OK, `test_ui_standard: clean`, `preflight: Preflight clean`, `build_ui_art: 260 textures verified`, `build_skins: 118 outputs verified` |
| `gradle build --offline -x test -q` | exit 0 |
| `python tools/build_ui_art.py --verify` | "260 textures verified" |
| `python test-tools/standalone/test_ui_standard.py` | "clean" |
| `GRAPHIFY_BACKEND=none graphify update .` | rebuilt 7365 nodes, 24025 edges |

Two test bugs found and fixed on the way (both mine, in the harness, not in the page):
1. `p1-ui-recruit-broke` and `-keys`: when the server gives all 30 troops at once, the client's own owned-set diff reveals the last new troop (the Lord Marshal) and the page selects it, so the check read the wrong letter and left the Odds note open, which then swallowed the next click. The check now selects the first troop again and closes the note.
2. `p2-notice-repeats`: the line of the refusal before still showed for a tick or two after the serial moved; it now waits for the new words.

`p2-screens-suite` and `p2-claim-house` stay red until Task 9 joins P1's claims; I did not touch them.

## Screenshots (graded)

All in `C:\Users\kortt\Melty\bannerbound\.superpowers\sdd\2026-10-24-war-table-port-and-armoury\shots-p2\task-4\`, each with a `-grey.png` copy. The 960 shots are the 960x540 page (Phase 1 shots are native; the `wt-recruit-*-960` and `migration-full-roster-*` ones are the central 960x540 crop of the harness's 1280x720 canvas, where the page sits centred). Graded by me against `work/ui-concepts/GRADING-RUBRIC.md` and read in greyscale: **9 each** (legible, one focal letter, three type sizes, the states carry words or shapes: the tag's label, the reason line, the broken seal, the lit star).

- Recruit page: `wt-recruit-page-960`, `wt-recruit-page-854`, `wt-recruit-page-1920x974`
- Letter reveal, new recruit under the broken seal: `wt-recruit-roll-960`, `wt-recruit-roll-854`, `wt-recruit-new-recruit-960`
- Duplicate lighting a star: `wt-recruit-duplicate-960`, `wt-recruit-duplicate-854`
- Odds note: `wt-recruit-odds-960`, `wt-recruit-odds-854`
- Notice: `wt-recruit-notice-960`
- Inactive tag with its reason (50 gold): `wt-recruit-broke-960`; Legendary with shards and Pact wording: `wt-recruit-legendary-960`
- Full roster, five Houses: `migration-full-roster-gilded`, `-graveborn`, `-wildmoot`, `-ironclad`, `-tidecallers`

Deviation: the plan's Step 1 asks for a mockup first. This task adds no new painted art (every part reuses the vellum tag, wax, star and note textures that exist), so I graded the real in-game shots instead.

## Needs Kort's eyes

- A new recruit's letter carries the broken seal and "Deploys in battle for N gold" but no written "joined" line; only a duplicate gets the outcome line. Say if a new recruit should also say "Joined your collection" in words.
- After a duplicate the reason line under the tag talks about the next Buy ("Adds a copy (2 of 4 toward star 3)"), directly under "Star 1 to star 2". Accurate, but two copy lines close together.
- Brassjaw's portrait on the Ironclad full-roster shot shows a pale model (an existing Legendary model issue, not this task).

## Foreign or frozen needs

None. I did not touch server code, sheets, `Spec.java`, `UiMath`, `CampaignClient`, HUD or Armoury files. Note for lane H: `UiMath` still has a stale "no duplicates" Odds helper; retire it at Task 9.
