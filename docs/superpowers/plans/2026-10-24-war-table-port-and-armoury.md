# War Table Port, Last Stand HUD and Armoury Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On the merged base (`phase-1-solo-core`, head 8266638) the War Table owns every House page Phase 2 built, so nothing in the shipped game is reachable only through the old camp screen:
- shard, Rare and contract claims with a House (12 chapter tiers and the contract shards are unclaimable today);
- the five Houses, rolling, the chosen buy, stars and the Pact, the duplicate roll card, Odds, the notice, the full roster and the troop sheet;
- the class page with the six classes' icons and their full text;
- the 20-seat Banner page across all five Houses, with wide troops.

The same job also lands the closing HUD work (LAST STAND card and feed line, the ability's own picture, level pips, glint and the level-3 feed line) and the Armoury tab (in-match progression Task 8), rewrites the 20 Phase 1 UI fixtures that pin Phase 1 content, and ends with Kort's screenshot gate. Everything here is client-side: no sheet, save or protocol change (see "Claims need no packet field").

**Architecture:** The War Table pages keep their structure (`WarTable`, `WarTablePages`, `WarTableRecruit`, `WarTableHome`, canvas pixels offset by `WarTable.origin()`, the 854x480 compact layout). Phase 2's pure helpers stay the single source of rules and are reused, not copied:
- `RecruitPages.action(troop, profile)` gives the buy, star or Pact action, its command, whether it is open and its tooltip.
- `Progression` (`claim`, `refusal`, `reward`) and `RosterMath` (`rollHouse`, `slotWeight`, `MARKER`) give the claim and seat rules.

New client code: `WarTableTroop.java` (the troop sheet) and `ArmouryTab.java`. The Phase 2 widgets in `CampScreen` and `CampaignLobbyScreen` are retired in Task 9, after nothing references them.

**Tech Stack:** Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Yarn 1.20.1+build.10, Loom 1.7.4, Java 17, Gradle 8.8 (offline), Python 3.12 (Pillow for the art tools).

**Binding context (read first):**
- `.superpowers/sdd/merge-prep-report.md` sections 6 and 7 (the reds this plan turns green).
- `docs/archive/QUESTIONS-FOR-KORT-before-optimization-2026-10-25.md` (historical round 34 and trial-merge answers) (Kort: "the War Table gap closes by porting Phase 2's missing pieces into the War Table: House-picker claims, troop-shop entry, class icons for the six Phase 2 classes, the full 20-slot Banner page, as one UI task after the base merge; Phase 2's old screen checks move to the War Table") and round 34 answer 4 (Armoury tab in today's barracks style now, restyled after the base merge).
- `docs/superpowers/plans/2026-10-18-ui-plan.md` Tasks 7 to 9 (UI Task 9 is Tasks 6 and 7 here), `2026-10-23-in-match-progression.md` Tasks 8, 12 and 13, `2026-10-17-phase-2-houses.md` Tasks 2 to 4, 12 and 13, and the progression spec section 5.1.
- `docs/HANDOFF.md`, `docs/WORKFLOW.md`, `docs/ART-STYLE-GUIDE.md`, `work/ui-concepts/GRADING-RUBRIC.md`.

## Kort's rulings that bind this plan

1. The Phase 2 House pages are **ported into** the War Table: shard, Rare and contract claims, House tabs, rolling, the full roster, claim house and notices, the class page. They are not kept as a second screen.
2. The 20 Phase 1 UI fixtures that pin Phase 1 content are **rewritten** (Tasks 1, 3, 4, 6), each with the page it tests.
3. The Armoury tab (progression Task 8) and progression Task 12 go on this base (Tasks 8 and 7).
4. **Icons come first.** Branch `ui-icons` (worktree `C:\Users\kortt\Melty\bannerbound-icons`) paints twelve icons: `table/icon-class-<id>` for `champion`, `reaver`, `warden`, `marshal`, `warlock`, `quartermaster`, and `hud/ability-<id>` for `stand_fast`, `bloodrush`, `mark_prey`, `rally_horn`, `grave_bloom`, `forward_supply`. This plan only wires pages to them (Tasks 2 and 6 wait on Task 0).
5. The commander ability slot **hides** for Quartermaster's Forward Supply (Task 6). The key stays bound.
6. Kort decides mod dependencies, progression and campaign rules, and any save or multiplayer-protocol break. **This plan changes none of them.** A task that finds it needs a server, sheet or packet change stops, records it in the ledger and asks.
7. **Kort's answers, 2026-10-24 (in chat):** side colour: add the four-seal row to the Banner page (Task 3); troop sheet: build it (Task 5); Armoury: today's barracks style in Task 8, War Table restyle after Kort's gate; Quartermaster slot: hidden, key stays bound. Controller defaults for the three questions he did not see (recommended options, reversible): claim default pre-selects the banner's House and names it in the reward line; the old-layout `results` and `train` pages and the match-report page stay parked; the roll reveal is the War Table's letter with the broken seal and `p2-roll-card` is rewritten to check it (Task 4).

## Claims need no packet field (smallest fix)

`Campaign.progressCommand` and `Progression.claim` already read the House or the Rare from the command string: `daily:<slot>[:<house>]`, `weekly:<house>`, `founders:<tier>[:<house or troop>]`. Phase 2's old screen sent exactly that. The War Table's contract and chapter pages send `progress:daily:<slot>`, `progress:weekly` and `progress:founders:<t>` with no suffix, so the server answers "Choose a House ..." and stores nothing (the shards went 0 to 0 in the merge run). The fix is client-only (Task 1): a "for <House>" row on Contracts and Chapter, and a Rare pick on tier 20. A packet field would be needed only if the server had to remember a default House per profile, which nothing asks for.

## What exists on the base (verified 2026-10-24)

- **Recruit page** (`WarTableRecruit`, 595 lines): the five-House banner rod (`RecruitView.houses()`), the muster roll with portraits (silhouette until recruited), the letter, `roll:<house>` on Enlist, the sealed letters, the pity line, a notice line, the Odds note. Gaps:
  - Choose sends `unlock:<id>`, which the server refuses in Phase 2 ("Troops now join by recruitment rolls"); the chosen buy `buy:<id>`, `star:<id>` and `legendary:<house>:pact|shards` are never sent.
  - The Odds note still says "No duplicates"; a duplicate roll shows the broken seal as if the troop were new, with no copy or star line.
  - There is no troop sheet (copies, star ladder, mastery, the three level lines).
- **Contracts and Chapter** (`WarTablePages`): claims send no House (lines 752 to 755 and about 881); the chapter plaques print "Gold" and "25" for every tier (about line 994), wrong for 22 of the 30 tiers (`Progression.reward(tier)` has the real text); tier 20's Rare has no pick.
- **Class plaques** (`WarTablePages` 568 to 571, 732 to 741): a name and a one-line bonus; the six Phase 2 classes draw no icon (`paintClass` skips a missing icon); the active and passive text of the sheet are not drawn.
- **Banner page**: one House (`owned()` reads `RosterMath.rollHouse(profile.banner)`), six rows, 20 seats, `army()` adds at the first empty seat without a weight check, and the seat sheet draws no wide-troop marker (`RosterMath.MARKER`, an Epic takes two slots). The side colour (`colour:<side>`) is reachable only from `CampaignLobbyScreen`, which only the harness opens.
- **HUD** (`BattleHud`): `plateState` has no LAST STAND; routing dead code remains; `ability()` draws `hud/horn` for every class; `FeedLines`, `FeedEvents` have `routing` and `rout`. `CommandMode.Group` and `state_v9` already carry `lastStand`, `full`, `strength`, `level2`, `level3` and `armouryOpen`; the Armoury block is decoded into `CampaignClient.armoury`. No pip, glint or level-3 line is drawn.
- **Armoury**: `LevelChecks.suites.put("armoury", NotBuilt)`; no tab, no key.
- **Old camp screen** (`CampScreen`, 281 lines) still carries Phase 2's widgets (`HouseTab`, `card`, `pickHouse`, `houseTabs`, `rollLabel`, `pityLine`, `pickClaimHouse`, `cycleRare`, `claimHouse`, `rare`, `openTroop`, `seenCards`, `cardLow`, `probe`) and an old-layout `troop` page. Only the harness reaches them; the six reds below drive them.

## File map

| File | Task | Role |
|---|---|---|
| `WarTablePages.java` | 1, 2, 3 | claim rows and Rare pick; class panel; the multi-House banner |
| `WarTableRecruit.java`, `RecruitView.java` | 4 | buy, star and Pact through `RecruitPages.action`; duplicate card; Odds copy; notice; full roster |
| `WarTableTroop.java` (new), `CampScreen.java` | 5, 9 | the troop sheet and its route; retire dead pages |
| `RecruitPages.java`, `CampaignClient.java`, `CampaignLobbyScreen.java` | 1 to 5 (read), 9 (prune) | helpers reused; unused ones deleted at the join |
| `BattleHud.java`, `UiMath.java`, `CommandMode.java`, `FeedLines.java`, `FeedEvents.java`, `EventFeed.java` | 6, 7 | LAST STAND, ability picture, pips, glint, level-3 line |
| `ArmouryTab.java` (new), `RecruitmentScreen.java`, `GravesAndGloryClient.java` (key binding only) | 8 | the Armoury tab and key |
| `tools/build_ui_art.py`, `tools/ui_art/*.py`, `src/main/resources/assets/gravesandglory/textures/gui/**`, `atlas.json` | 1 to 3, 6, 7 | new painters in each lane's own files; the atlas is generated |
| `test-tools/java/gg/gravesandglory/Phase1Checks.java`, `Phase2Checks.java`, `LevelChecks.java`, `UiMathCheck.java`, `test_ui_standard.py` | all | rewrites and new ids, by region (below) |
| `docs/UI-GATE.md`, `docs/PARKED.md`, `docs/PHASE-2-PLAYTEST.md` | 10 | gate page, parked items, playtest items |

No new harness class is created, so `tools/release_config.py` needs no change. If a task adds one, it goes in the `forbidden` tuple.

## Order and parallelism

```
Task 0 (controller): icons merged, worktrees, baseline
  Lane P1  ui-port-claims    worktree bannerbound-ui-p1   Task 1 claims -> Task 2 class page -> Task 3 Banner page
  Lane P2  ui-port-recruit   worktree bannerbound-ui-p2   Task 4 recruit page -> Task 5 troop sheet
  Lane H   ui-hud-last-stand worktree bannerbound-ui-hud  Task 6 LAST STAND + abilities -> Task 7 pips, glint, level-3 line
  Lane A   ui-armoury        worktree bannerbound-ui-arm  Task 8 Armoury tab
-> Task 9 join and retire the dead pages -> Task 10 regression and Kort's gate
```

- **Four lanes run in parallel with disjoint files.** Each lane starts from the Task 0 head. Within a lane the tasks are sequential, because they share files. Tasks 2 and 6 start only after Task 0 reports the icons merged; Tasks 1, 3, 4, 5, 7 (after 6) and 8 do not need them.
- **Ownership is a hard rule.** A lane that needs another lane's file stops and reports; the edit waits for Task 9.

| Lane | Owns (edits) |
|---|---|
| P1 | `WarTablePages.java`, `WarTable.java`, `WarTableHome.java` (only if a shared helper must change), `tools/ui_art/wt_claims.py` (new), `Phase1Checks.java` region `ui-pages` (about lines 3180 to 3700), `Phase2Checks.java` blocks `p2-claim-house` and `p2-class-picker` (Screens near 6455 to 6470; Commanders near 1116) |
| P2 | `WarTableRecruit.java`, `RecruitView.java`, `WarTableTroop.java`, `RecruitPages.java`, `CampScreen.java` (routing only), `tools/ui_art/wt_recruit.py` (new), `Phase1Checks.java` region `ui-recruit` (about 2700 to 3060), `Phase2Checks.java` blocks `p2-recruit-tabs` (Engine near 505 and 661), `p2-full-roster` (Migration near 6600 to 6700), `p2-roll-card`, `p2-notice-clears`, `p2-roll-card-troop` (Screens near 6336 to 6460) |
| H | `BattleHud.java`, `UiMath.java`, `CommandMode.java`, `FeedLines.java`, `FeedEvents.java`, `EventFeed.java`, `tools/ui_art/v4a_hud.py` and `hud_*.py` (new), `UiMathCheck.java`, `test_ui_standard.py`, `Phase1Checks.java` regions `ui-hud` and `ui-feed` (about 1570 to 2340) |
| A | `ArmouryTab.java`, `RecruitmentScreen.java`, `GravesAndGloryClient.java` (one key binding and its hint), `LevelChecks.java` |

- **Frozen in every lane:** all server code (`Campaign`, `GravesAndGlory`, `Veterans`, `Progression`, `Abilities`, `Commander`), `sheets/design.json`, `Spec.java`, `tools/preflight.py`, `LevelMath.java`, `RankSync.java`, `RosterMath.java`. A lane never edits them.
- **Generated files** (`atlas.json`, the packed texture PNGs) are regenerated by `python tools/build_ui_art.py` at Task 9 and never hand-merged. A lane commits its own painters and PNGs and may commit the regenerated atlas; a conflict on it at the join is resolved by regenerating.
- **Clients.** Each lane uses at most one harness client at a time, so four lanes use the four allowed across every worktree (HANDOFF). Before each launch run the "Kort is playing" process check and count the clients other sessions hold; if only three are free, lane A waits, because it has the fewest launches. Never compile in a worktree while its client runs. Each client run is 30 minutes at most. Run roots: `work/p1/<tag>` for `GRAVES_PHASE1_CHECKS`, `work/p2/<tag>` for `GRAVES_PHASE2_CHECKS`, `options.txt` seeded, as in the progression plan's "Test runs used by every task".
- **Not parallel:** Tasks 6 and 7 (same HUD files, one agent, separate commits, as UI Task 9 allows); Tasks 9 and 10.

## Global Constraints

- Minecraft 1.20.1 and Fabric only; no new dependency. No server, sheet, save or protocol change.
- **Look:** the approved War Table and Iron Ledger look is binding. No brass fittings on table wood. Nothing under 9 on `work/ui-concepts/GRADING-RUBRIC.md` passes for player-facing art; every shot is also graded as a greyscale copy. Colour-blind rule: every state (chosen House, claimed, inactive, wide seat, glint) is told by a word, a glyph or a shape, never by colour alone. `ui-ux-pro-max` is used only with `--domain ux` (readability, contrast, hit targets of at least 24 canvas px, focus order, feedback), never for style, palette or font, and never with `--design-system` or `--persist`.
- A new painted piece is mocked up first in `work/ui-concepts/wt-port/` (git-ignored) at 960x540 and 854x480 and graded 9 or better before code. Kort sees one 960-wide contact sheet per task that changes the look.
- Text: no clipped or ellipsised sheet text. A check measures every drawn line against its box (`UiMath.lines`, `font.getWidth`) at all five sizes (the `p1-ui-*-scales` sizes). Player text never says "stage", "evolve" or "Writ" (it says "Pact").
- **Derive, never pin.** A rewritten fixture reads its expected words from the same source as the game (`Progression.reward(tier)`, `Spec.TROOPS`, `Spec.FACTIONS`, `FeedLines`, `RecruitView`), so the next content change cannot turn it red again.
- Harness lives only in `test-tools/`. The working copy is CRLF: edit dense one-line Java files in place; never reformat or re-save a whole file. Never hand-edit `Spec.java`.
- Client rules (HANDOFF): at most four harness clients at once across worktrees, 30 minutes each; always pass `--offline` and `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"`.
- Commit by path with `git -c user.name=Claude -c user.email=noreply@anthropic.com commit -F <msg file under work/p1/>`. Subjects start `War Table port Task <n>:`. The message ends with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Never push, merge to main, publish or use `mcp__melty__*`.
- Run `GRAPHIFY_BACKEND=none graphify update .` before each task's commit. **Every brief carries the CLAUDE.md graphify rule:** start a code question with `graphify query "<question>"`, then read and verify the source lines it points to; if the graph is missing or silent, grep.
- Executors (tonight, Kort's rule): Sonnet 5.5 for every agent, `effort` set on every spawn, the lowest that does the job, never `max`, never unset. If Opus is lifted before a task starts, tasks marked **[design]** move to Opus `high`.
- Process: briefs, reports, review packages and the ledger live in `.superpowers/sdd/2026-10-24-war-table-port-and-armoury/`. Each task gets a Sonnet `xhigh` review of `review-A..B.diff`; fix rounds resume the implementer, then a scoped re-review. Two fix rounds at most, then the controller rules. Every decision goes into `progress.md` as `Ruling: what, why, cost if wrong`.
- Deferred ideas go to `docs/PARKED.md` (append only); open calls to `docs/QUESTIONS-FOR-KORT.md` (append only, recommended option first). Lanes do not edit those two files in parallel: they write to their own report and the controller appends at Task 10.

## Review Focus

1. **Stuck money.** After Task 1, the 12 shard tiers, the daily that completes all three, the weekly and tier 20's Rare each pay once (`p2-claim-*`). A second press on a claimed item sends nothing and shows its reason; a double click does not spend twice (`CampScreen.action`'s throttle).
2. **Choose is not `unlock:`.** After Task 4 the page never sends `unlock:` or `army:` (both refused in Phase 2): `sentCommands` is searched in every recruit check.
3. **No silent button.** Every inactive button (Choose, Star, Pact, Claim, Add) shows its reason as a word on the page or in its tooltip (`RecruitPages.Action.tip`, `Progression.refusal`).
4. **Wide troops.** An Epic placed on the banner shows both seats; Remove frees both; the page never offers an Add the server will refuse (`RosterMath.slotWeight`, `MARKER`).
5. **Frozen server.** The diff of every lane touches no file under "Frozen". A packet or sheet need is a ledger entry, not an edit.
6. **Retirement is safe.** Task 9 deletes a Phase 2 widget only when `grep` finds no reference in `src/` or `test-tools/`.

## Test runs used by every task

- **Standalone and compile:** the commands of the progression plan's "Test runs used by every task" (preflight, the javac checks including `UiMathCheck`, `"$G" compileJava compileLocalTestTools --offline`), run in the lane's worktree. Before each commit also `"$G" build --offline -x test`, `python tools/build_ui_art.py --verify` and `python test-tools/standalone/test_ui_standard.py` (expects `clean`).
- **In-game:** Phase 1 UI suites with `GRAVES_PHASE1_CHECKS=<suite>`, Phase 2 and progression suites with `GRAVES_PHASE2_CHECKS=<suite>`, each in a fresh root; read `evidence/local-checks.txt` (`grep -E "^(pass|fail) p[12]-"`), `run/logs/latest.log` and `run/screenshots`. A suite passes when every id of it is `pass`, its `-suite` roll-up line is present, the named screenshots exist, and the tester states in one sentence what each shows.
- **Screenshot grading:** side by side with the mockup, a greyscale copy, the ten rubric lines, as UI Task 8 Step 5. A screen under 9 goes back to its task with the lines that lost points.

## Verification matrix

Every red of the merge report and every new id, with the task that turns it green and the run that proves it. A roll-up is green only after all of its members are.

| Id (or family) | Today | Green at | Run | Notes |
|---|---|---|---|---|
| `p2-claim-house` (+ new `p2-claim-weekly`, `p2-claim-founders-shards`, `p2-claim-founders-rare`, `p2-claim-sends`) | red (shards 0 to 0) | Task 1 | Phase 2 `screens` | claim sends the House; server stores it |
| `p1-ui-page-contracts`, `p1-ui-page-chapter` | red (Phase 1 numbers) | Task 1 | Phase 1 `ui-pages` | derive from `Progression.reward` |
| `p2-class-picker`, `p2-class-icons`, `p2-class-text-fits` (new) | red (2 lines cut) | Task 2 | Phase 2 `commander` | roll-up `p2-commander-suite` |
| `p1-ui-page-banner`, then `p1-ui-pages-suite` (+ new `p2-banner-houses`, `-wide`, `-sends`, `-fits`) | red | Task 3 | Phase 1 `ui-pages` | the roll-up needs Tasks 1 and 3 |
| `p2-recruit-tabs`, `p2-full-roster`, `p2-roll-card`, `p2-notice-clears` (+ new `p2-recruit-choose`, `-star`, `-pact`, `-duplicate`, `-odds`) | red | Task 4 | Phase 2 `engine`, `migration`, `screens` | roll-ups `p2-engine-suite`, `p2-migration-suite`; `p2-screens-suite` needs Task 1 too |
| `p1-ui-recruit-*` (the 8 reds the first run lists, `-exception` among them) | red | Task 4 | Phase 1 `ui-recruit` | `-exception` is the harness's NPE on a rect the page no longer draws (`Phase1Checks.java` about 2960) |
| `p2-roll-card-troop` (moved), new `p2-troop-sheet`, `p2-troop-star` | green on the old page | Task 5 | Phase 2 `screens` | must stay green through Task 9 |
| `p1-ui-hud-fixture`, `-hud-suite`, `-alt-layer` | red (12/6/2 pin) | Task 6 | Phase 1 `ui-hud` | fixture states its own banner |
| `p1-ui-feed-point`, `-loss-kill`, `-rout` (to `p1-ui-feed-last-stand`), `-deploy`, `-feed-suite` | red (Phase 1 words) | Task 6 | Phase 1 `ui-feed` | words derived from `FeedLines` and `Spec` |
| `p1-ui-cards`, `-cards-edge`, `p1-ui-ability`, new `p1-ui-ability-hidden` | green on routing | Task 6 | Phase 1 `ui-hud` | LAST STAND, picture, Quartermaster hide |
| `p1-ui-pips`, `-glint`, `-feed-level3`, `-feed-level2-silent`, `-hud-footprint` | not built | Task 7 | Phase 1 `ui-hud`, `ui-feed` | footprint at the five sizes |
| Armoury: `p2-armoury-tab`, `-states`, `-field`, `-buy-click`, `-fits`, `-key-hint`, roll-up `p2-armoury-suite` | `NotBuilt` | Task 8 | Phase 2 `armoury,gear` | |
| All 20 Phase 1 UI reds | red | Tasks 1, 3, 4, 6 | `ui-hud,ui-feed,ui-recruit,ui-pages` | 3 + 5 + 8 + 4 = 20 |
| The whole tree vs the merge baselines | | Task 10 | full regression | Phase 1 core 57/0; Phase 3 135/0, 149/202/156/164/139 on maps 0 to 4, routes 49/0; campaign 39/0; selftest 11/0; levels, gear, ranks and the five House suites as in the merge run |

## Kort's gate (Task 10)

Kort sees `docs/UI-GATE.md`, a 960-wide contact sheet per changed screen with its mockup, the greyscale copy and the rubric lines (all 9 or better):
1. Contracts and Chapter with the "for <House>" row and the Rare pick: `wt-contracts-claim`, `wt-chapter-pick`.
2. The class page, six classes with icons, the Quartermaster's text complete: `wt-class-quartermaster`, `wt-class-warlock`, `wt-class-compact`.
3. The Banner page across Houses with a wide seat: `wt-banner-houses`, `wt-banner-compact`.
4. The recruit page: a new troop, a duplicate, Odds, the notice, the full roster (`wt-recruit-roll`, `wt-recruit-duplicate`, `wt-recruit-odds`, `wt-recruit-notice`, `p2-migration-full-roster-*`) and the troop sheet (`wt-troop-sheet`, `wt-troop-legendary`).
5. The HUD: `ui-hud-cards` (LAST STAND), `ui-hud-ability` and the Quartermaster hide, `ui-hud-pips`, `ui-feed-level3`.
6. The Armoury: `armoury-tab`, `armoury-field`.

Kort decides: pass or send back per screen, and the open questions at the end. 0.5.0's UI is done when he passes it and the final regression is green.

---

### Task 0: Icons merged, worktrees, baseline (controller)

**Executor:** the controller, or Sonnet 5.5 `medium`. **Worktree:** the main checkout. **Needs a client:** no.

**Files:** none edited by the controller except git state. Docs in the main checkout are dirty (`CLAUDE.md`, `HANDOFF.md`, `PARKED.md`, `QUESTIONS-FOR-KORT.md`, two plans, one untracked plan); none is touched by worktree creation.

- [ ] **Step 1: Wait for the icons.** Branch `ui-icons` reports twelve icons. Check in `C:\Users\kortt\Melty\bannerbound-icons`: `python tools/build_ui_art.py --verify` and `python test-tools/standalone/test_ui_standard.py` are clean, and `atlas.json` holds the six `table/icon-class-<id>` and six `hud/ability-<id>` ids named in ruling 4. The icons agent's own graded report holds scores of 9 or better.
- [ ] **Step 2: Land the icons on the base** (the controller's call: a local fast-forward or merge of `ui-icons` into `phase-1-solo-core`; nothing is pushed). Re-run the two checks and Phase 1 `ui-art` once; the new ids appear in its listing.
- [ ] **Step 3: Create the four worktrees** from that head: `git worktree add ../bannerbound-ui-p1 -b ui-port-claims`, `../bannerbound-ui-p2 -b ui-port-recruit`, `../bannerbound-ui-hud -b ui-hud-last-stand`, `../bannerbound-ui-arm -b ui-armoury`. Copy the git-ignored pinned dependency jars and the launch pattern from `bannerbound-merge`. In each: `"$G" compileJava compileLocalTestTools --offline` and the standalone checks pass, as the merge run's baseline.
- [ ] **Step 4: Confirm the red baseline** is the merge report's: start no client yet; the lanes' first run records it. Write the briefs (the graphify rule in each) into `.superpowers/sdd/2026-10-24-war-table-port-and-armoury/`.

**Risk:** low. The only trap is merging the icons branch with stale atlas generation: always regenerate the atlas, never merge it by hand.

---

### Task 1 (lane P1): Claims with a House, the Rare pick, chapter labels

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-p1`. **Needs a client:** yes (Phase 2 `screens`, Phase 1 `ui-pages`).

**Files:**
- Modify `WarTablePages.java`:
  - A "for <House>" row of five chips (the five `RecruitView.houses()`, a seal and the House name) on Contracts and on Chapter, shown while the next claim pays shards. It holds a client static `claimHouse`, defaulting to `RosterMath.rollHouse(profile.banner)`; the chosen chip carries a word ("for") and a rule under it, not only a colour. The reward line names the pay and the House ("3 Ironclad shards" for the daily, "10 Ironclad shards" for the weekly and a shard tier), from `Progression.reward`.
  - Contracts: `progress:daily:<slot>` for slots 0 and 1 (no House); `progress:daily:2:<claimHouse>`; `progress:weekly:<claimHouse>`.
  - Chapter: `progress:founders:<t>:<claimHouse>` for tiers in `Progression.SHARD_TIERS`; tier 20 (`Progression.RARE_TIER`) shows a Rare chip that cycles the Rares of the chosen House (`RecruitPages.rares(house)`, as the old `CampScreen.cycleRare` did: read it first) and sends `progress:founders:20:<troopId>`; gold, title and cosmetic tiers send `progress:founders:<t>`.
  - Plaque labels from `Progression.reward(tier)` in place of "Gold" and "25"; the page's claimed and not-yet states keep a word or glyph.
  - An inactive Claim shows `Progression.refusal` as its tooltip and a short word on the plaque.
- Create `tools/ui_art/wt_claims.py` (the chip and its chosen state, `table/chip-house`, `table/chip-house-on`), and register the module in `build_ui_art.py` (one import line).
- Modify `Phase1Checks.java` (`p1-ui-page-contracts`, `-chapter`) and `Phase2Checks.java` (`p2-claim-house`, plus the new ids).

**Interfaces:** consumes `Progression.claim/refusal/reward` and its tier sets, `RecruitPages.rares(house)/houseChips/contractShards`, `RosterMath.rollHouse`, `CampScreen.action`'s throttle through `WarTablePages.send`. Produces `WarTablePages.claimHouse` for Task 3's filter default.

- [ ] **Step 1: Mockup** both pages with the House row at 960x540 and 854x480; grade 9 or better.
- [ ] **Step 2: Write the checks (red first):**
  - `p2-claim-house`: a fixture with the three dailies done; pick Ironclad in the row; press Claim on contract 2. The server stores `daily:2` and Ironclad shards rise by `Progression.DAILY_SHARDS` (shards 0 to 0 today). Screenshot `wt-contracts-claim`.
  - `p2-claim-weekly`: weekly with Tidecallers picked pays `WEEKLY_SHARDS` to Tidecallers.
  - `p2-claim-founders-shards`: a tier of `SHARD_TIERS` with Wildmoot picked pays `TIER_SHARDS` to Wildmoot. Screenshot `wt-chapter-pick`.
  - `p2-claim-founders-rare`: tier 20 with a Rare picked adds that troop; a non-Rare is not offered.
  - `p2-claim-sends`: `sentCommands` holds exactly `progress:daily:2:ironclad`, `progress:weekly:tidecallers`, `progress:founders:<t>:wildmoot`, `progress:founders:20:<rare>`; slots 0 and 1 carry no House.
  - `p1-ui-page-contracts` and `-chapter` rewritten: expected plaque words and gold come from `Progression.reward(tier)` and the Phase 2 tier lists, not from "+25 gold" and the hero tier.
- [ ] **Step 3: Run `screens` and `ui-pages`; expect the new ids and `-banner` red, the two rewritten ids red for the right reason.**
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run the same suites; the ids of the matrix row are green.** `p1-ui-page-banner` and `-pages-suite` stay red until Task 3. Grade `wt-contracts-claim`, `wt-chapter-pick` and greyscale copies: 9 or better. Run the `ux` checks for hit targets and focus order.
- [ ] **Step 6: Commit** `"War Table port Task 1: claims with a House, the Rare pick, chapter labels"`.

**Risk:** medium. The claim strings are exact (a wrong suffix is a silent refusal); the chip row competes for space on the compact 854x480 pages; the default House must never claim shards into a House the player did not see (the reward line names it). Review: the five `sentCommands` strings and the claimed-once behaviour.

---

### Task 2 (lane P1): The class page with the six icons and full text

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-p1`. **Starts after Task 0 and Task 1.** **Needs a client:** yes (Phase 2 `commander`).

**Files:**
- Modify `WarTablePages.java`: the six plaques draw `table/icon-class-<id>` (no fallback: a missing icon fails the check); a panel under them shows the class in use, or the one under the pointer or focus: name, the troops it leads (`group`), Bonus, Active (name, cooldown seconds from `cooldownTicks`) and Passive, each with its full sheet text (`activeText`, `passiveText`), wrapped by `UiMath.lines`, never cut. The Quartermaster's text is the longest; the layout is sized to it at 854x480.
- Modify `Phase2Checks.java` block `p2-class-picker` (Commanders near 1116).

**Interfaces:** consumes `Spec.CLASSES`, `class:<id>`, `profile.commander`, the icon ids of ruling 4. Task 3 shares this page's right column: keep the class panel's box constants in one place (`CLASS_*`) so Task 3 does not move them.

- [ ] **Step 1: Mockup** the page with the longest class (Quartermaster) at 960x540 and 854x480; grade 9 or better.
- [ ] **Step 2: Write the checks (red first):**
  - `p2-class-picker`: for each of the six classes, press its plaque; the profile's `commander` follows within 2 state packets; the panel for that class shows its Bonus, Active and Passive text in full (every sheet string is a prefix-free join of the lines drawn; no line was dropped). Screenshots `wt-class-quartermaster`, `wt-class-warlock`.
  - `p2-class-icons`: every plaque draws its own `table/icon-class-<id>` (six of six); the picture ids match the class ids.
  - `p2-class-text-fits`: at the five sizes every text line lies inside its box and the panel never overlaps the plaques; screenshot `wt-class-compact`.
- [ ] **Step 3: Run `commander`; expect all three red** (today: "2 lines cut", no icons).
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run `commander`; `p2-class-picker`, `-icons`, `-text-fits` and `p2-commander-suite` pass.** Grade `wt-class-quartermaster`, `wt-class-warlock` and greyscale copies: 9 or better.
- [ ] **Step 6: Commit** `"War Table port Task 2: the class page, six icons, full text"`.

**Risk:** medium. The text is long and the font wider than Phase 2's (the original failure). Do not shrink the font: grow the panel or shorten the plaques. Review: the five-size fit check and the grey copies. The icons are another agent's art; a weak icon goes back to `ui-icons`, not into this task.

---

### Task 3 (lane P1): The Banner page across Houses, wide troops

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-p1`. **Needs a client:** yes (Phase 1 `ui-pages`).

**Files:**
- Modify `WarTablePages.java` (Banner section, about 537 to 640): a House filter (the same five chips as Task 1, defaulting to `claimHouse`'s default, the banner's own House) over the roll; the roll lists the owned troops of the chosen House (six rows, the wheel scrolls if a House ever has more); each row shows its slot weight (a "2 seats" word on a wide troop). The 20-seat sheet draws a wide troop across two seats and the `MARKER` cell as its second half; the seat count reads "<used>/20" by slot weight. Add is inactive, with its reason, when no run of free seats fits the troop's weight; Add takes the first run that fits; Remove frees every seat of the troop's last copy; a seat click places the armed troop through `slot:<i>:<id>`.
- Possibly (see question 1): a four-seal colour row sending `colour:<side>`.
- Modify `Phase1Checks.java` (`p1-ui-page-banner`, `-pages-suite`, region `ui-pages`).

**Interfaces:** consumes `RosterMath.slotWeight/MARKER/EMPTY/rollHouse`, the profile's `banner` list. Reads, never edits, `RosterMath`. The server repacks and validates; the page shows what the next state packet holds and does not predict it.

- [ ] **Step 1: Mockup** the page with five chips, a wide Epic seated and a House with no owned troop, at 960x540 and 854x480; grade 9 or better.
- [ ] **Step 2: Write the checks (red first):**
  - `p1-ui-page-banner` rewritten for Phase 2: 20 seats, five chips, the seat texts from the profile's `banner`.
  - `p2-banner-houses`: with a full-roster fixture each chip lists exactly that House's six troops, owned flags right; a House with none owned shows the empty-roll word.
  - `p2-banner-wide`: place an owned Epic; the seat and its right neighbour both draw it (the second as the marker); the count rises by 2; Remove empties both; Add on a full banner is inactive with its reason.
  - `p2-banner-sends`: `sentCommands` holds only `slot:<i>:<id>` strings with the right seat indices; never `army:`.
  - `p2-banner-fits`: five sizes; every seat and row inside the canvas, hit targets 24 px or more. Screenshots `wt-banner-houses`, `wt-banner-compact`.
- [ ] **Step 3: Run `ui-pages`; expect these red.**
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run `ui-pages`; `p1-ui-page-banner` and `p1-ui-pages-suite` pass (Task 1's two ids are already green).** Grade the two shots and greyscale copies: 9 or better.
- [ ] **Step 6: Commit** `"War Table port Task 3: the Banner page across Houses, wide troops"`.

**Risk:** high, the biggest task. A wide troop's seats depend on server repacking; draw from the profile, never from a local guess. Seat 20 with a wide troop and a banner of 19 are the edge cases to test. A new House row must not push the seat sheet off the 854x480 canvas.

---

### Task 4 (lane P2): The recruit page: buy, stars, Pact, duplicates, Odds, notice, roster

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-p2`. **Needs a client:** yes (Phase 1 `ui-recruit`, Phase 2 `engine,migration,screens`).

**Files:**
- Modify `WarTableRecruit.java` and `RecruitView.java`:
  - **The letter's action** is `RecruitPages.action(troopsRow, profile)`: Choose becomes "Buy <cost>" (`buy:<id>`, Common and Rare, owned or not, until full stars), "Star <n>" (`star:<id>`, owned Epic and Legendary), "Pact" or "<shards> shards" (`legendary:<house>:pact|shards`, an unowned Legendary). An unowned Epic shows the word "From letters only". An inactive action shows its `tip` as a line under the tag. `unlock:` is never sent.
  - **A duplicate roll** (the rolled troop was already owned) shows no broken seal: the letter lights the new star or copy ("Copy 2 of 3 toward star 2") and says so in its line.
  - **The Odds note** drops the stale "No duplicates" rule and states the real odds per rarity (`RosterMath.rarityRow`), the pity count (`RecruitView.pityLeft`) and "a troop you own gives a copy toward its next star".
  - **Notice:** a refusal shows once, fades, and is not replayed after the page is closed and reopened or after later state packets (`CampaignClient.shownNotice`, `noticeSerial`).
  - **Full roster:** a profile with all 30 troops shows six owned rows (portraits, not silhouettes) on each of the five banners.
- Create `tools/ui_art/wt_recruit.py` only if a new tag or glyph is needed (a "Star" and "Pact" tag in the vellum family); reuse existing tags first.
- Modify `Phase1Checks.java` region `ui-recruit` and `Phase2Checks.java` blocks listed in the file map.

**Interfaces:** consumes `RecruitPages.action`, `CampaignClient.lastRoll/rollShowing/revealUntil/revealUnit/noticeSerial`, `ROLL_FILL_MS` 1200 and `REVEAL_MS` 2400. Produces the page state Task 5's troop sheet opens from (a click on the letter's name).

- [ ] **Step 1: Mockup** the letter in its four states (Buy, Star, Pact and inactive with a reason), the duplicate card and the Odds note; grade 9 or better.
- [ ] **Step 2: Write the checks (red first):**
  - `p2-recruit-tabs`: the same v1 fixture; on the War Table page the five House banners each show their roll with the migrated profile's owned flags; Enlist's price is `RecruitView.cost()`; the purse reads 420 plus 3 times the refund (as the old check).
  - `p2-full-roster`: all 30 owned; each banner shows six owned rows. Screenshots `p2-migration-full-roster-<house>` (five).
  - `p2-roll-card`: after Enlist the letter shows the rolled troop within the roll and reveal time (`letter-troop` equals `lastRoll`), the broken seal only when new, the purse down by the cost, the server's state the same. Screenshot `wt-recruit-roll`.
  - `p2-recruit-duplicate`: a forced duplicate draw lights a star and the copy line, no broken seal. Screenshot `wt-recruit-duplicate`.
  - `p2-notice-clears`: a refusal shows once on the notice line and is gone after the fade, after a page close and reopen, and after two state packets. Screenshot `wt-recruit-notice`.
  - `p2-recruit-choose` (Buy sends `buy:<id>`, gold down by `chosenCost`), `-star` (`star:<id>`, shards down), `-pact` (`legendary:<house>:pact`, a Pact spent): each reads the server's state; none sends `unlock:`.
  - `p2-recruit-odds`: the note's lines hold the odds and the copy rule and not "No duplicates". Screenshot `wt-recruit-odds`.
  - `p1-ui-recruit-*` rewritten: the Phase 1 letter flow (Enlist, Choose, Dusk banner, broke, keys, layout, scales) restated on the five Houses by index (never "dusk"), the Phase 2 prices and the House locks.
- [ ] **Step 3: Run `ui-recruit`, then `engine,migration,screens`; expect the listed reds, the first run recording the exact `p1-ui-recruit-*` set.**
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run the same suites; the matrix row is green.** `p2-screens-suite` also needs Task 1's claim ids (merged at Task 9, or run on the join). Grade the five shots and greyscale copies: 9 or better.
- [ ] **Step 6: Commit** `"War Table port Task 4: the recruit page, buy, stars, Pact, duplicates, Odds"`.

**Risk:** high. The reveal is found by troop *name* (`revealUnit`): a duplicate must not re-break the seal; the roll and reveal timings must hold the old test's window. `Choose` changes meaning (it was a refused `unlock:`), so the cost it shows is the chosen-buy cost. A double press must not spend twice.

---

### Task 5 (lane P2): The troop sheet (the troop-shop entry)

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-p2`. **Needs a client:** yes (Phase 2 `screens`).

**Files:**
- Create `WarTableTroop.java`: opened by a click on the letter's name (and by `CampScreen` page `troop`). It shows the portrait, name, House and rarity, the star ladder with the next star's cost, copies, Mastery progress and title, and the three level lines (level names from `RecruitPages.levelNames(t)` over `LevelMath.levelName`; text from the `levelAbilities` rows' `abilities.text`, as the card does), and the same action tag as the letter (`RecruitPages.action`). Back returns to the recruit page on the same House and troop.
- Modify `CampScreen.java` (routing of page `troop` to `WarTableTroop`; nothing else).
- Create or extend `tools/ui_art/wt_recruit.py` only for a missing ladder glyph.
- Modify `Phase2Checks.java` blocks `p2-roll-card-troop` (moved from the old page) and add `p2-troop-sheet`, `p2-troop-star`.

**Interfaces:** consumes the profile's `owned` (copies, stars, mastery), `Spec.TROOPS`, `Spec.RECRUITMENT`, `RecruitPages.caption`. Does not change what a troop does; it only shows it.

- [ ] **Step 1: Mockup** the sheet for a Common (Buy), an owned Epic (Star) and an unowned Legendary (Pact) at both sizes; grade 9 or better.
- [ ] **Step 2: Write the checks (red first):**
  - `p2-troop-sheet`: opening a troop from the letter shows its three level lines, copies, stars and Mastery from the profile; Back lands on the same House and troop. Screenshots `wt-troop-sheet`, `wt-troop-legendary`.
  - `p2-troop-star`: a star-up from the sheet sends `star:<id>` and the ladder moves on the next state.
  - `p2-roll-card-troop` (moved): a buy on the sheet shows the roll card, and leaving the sheet ends the reveal (the old stale-card check).
- [ ] **Step 3: Run `screens`; expect the new ids red and `p2-roll-card-troop` red once it drives the new page.**
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run `screens` and `ui-recruit`; all pass.** Grade the shots and greyscale copies: 9 or better.
- [ ] **Step 6: Commit** `"War Table port Task 5: the troop sheet"`.

**Risk:** medium. A new screen class; the level lines must read the same row the card and `Abilities` read (progression Review Focus 5). If Kort declines the troop sheet (question 2), this task is dropped and `p2-roll-card-troop` is retired with the old page.

---

### Task 6 (lane H): LAST STAND card, feed line, the ability's own picture, the Group A and B fixtures

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-hud`. **Starts after Task 0.** **Needs a client:** yes (Phase 1 `ui-hud,ui-feed,ui-art`).

This is UI plan Task 9 (HUD part), written for the merged base, plus the 8 fixtures of Groups A and B.

**Files:**
- Modify `BattleHud.java`: `plateState` gains LASTSTAND (order FALLEN, LASTSTAND, FLASH, SELECTED, PLAIN); delete the ROUTING dead code; the LAST STAND card keeps its live look (not grey), with the stamp and the words LAST STAND in the state slot Task 8c reserved (chip and full card); FALLEN keeps today's grey art. `ability()` draws `hud/ability-<id>` for the five class actives and **draws no slot at all for `forward_supply`** (ruling 5; the key stays bound; no sound, no empty frame). `hud/horn` stays for the title screen.
- Modify `CommandMode.java` (`Group.routing` leftovers to `lastStand`, if any remain), `FeedLines.java` (`lastStand(String group)` = group label + " make a last stand"; delete `routing` and `regrouped`), `FeedEvents.java` (`rout` to `lastStand`, hooked to the server's throttled last-stand line), `EventFeed.java` (kind `laststand`, the stamp glyph, ink `0xff504b44`).
- Modify `tools/ui_art/v4a_hud.py` (and a new `hud_*.py`): the LAST STAND card art if the painter wants one (within the Iron Ledger family), the stamp glyph. The six ability pictures are the icons branch's and are not painted here.
- Modify `UiMath.java`, `UiMathCheck.java`, `test_ui_standard.py`, `Phase1Checks.java` regions `ui-hud` and `ui-feed`.

**Interfaces:** consumes `state_v9`'s `lastStand` and `full` (already decoded), `Commander.byActive`, `GravesAndGloryClient.abilityId`, the icon ids of ruling 4. Task 7 builds on this task's `hudLayout` slots.

- [ ] **Step 1: Mockup** the LAST STAND chip and full card (graded 9, with a greyscale copy; items 6 and 7 of the rubric name last stand).
- [ ] **Step 2: Rewrite the fixtures and write the checks (red first):**
  - Group A (`p1-ui-hud-fixture`, `-hud-suite`, `-alt-layer`): the fixture sets its own banner (for example 12 infantry, 6 ranged, 2 support, explicit troop ids) instead of reading the starter army, so counts and chips are stated.
  - Group B (`p1-ui-feed-point`, `-loss-kill`, `-deploy`, `-feed-suite`): expected words come from `FeedLines` and `Spec.FACTIONS` and the troop plural the game prints ("Gold took Mill Hamlet", "3 Order Squires lost", "2 Risen lost" are what it prints today; the check computes them, it does not hard-code them).
  - `p1-ui-cards`: the support group at its last stand gives `FLASH`, `SELECTED`, `LASTSTAND` at the `hudLayout` rects; the card is live, with stamp, words and a red count. Screenshot `ui-hud-cards`. `p1-ui-cards-edge`: FALLEN stays grey.
  - `p1-ui-feed-last-stand` (replaces `-feed-rout`): ten of twelve own troops die in one tick with an enemy within 16; within 100 ticks "Infantry make a last stand" appears once; no second line inside 30 s; none for a group at 25 percent with no enemy near.
  - `p1-ui-ability`: with `rally_horn` the slot draws `hud/ability-rally_horn`, never `hud/horn`; rect, seconds and sweep as before. Screenshot `ui-hud-ability`. `p1-ui-ability-hidden`: with `forward_supply` no `ability` rect is drawn and `BattleHud.rects()` has no `ability` key; the key still toggles nothing visible. Screenshot `ui-hud-ability-hidden`.
  - `UiMathCheck`: `FeedLines.lastStand("Support").equals("Support make a last stand")` and the `laststand` ink holds 4.5:1 on the slip. `test_ui_standard.py`: `BattleHud` draws the stamp with the words, and never draws `hud/horn` in the slot.
- [ ] **Step 3: Run `ui-hud,ui-feed`; expect the eight fixtures and the new ids red for the right reason.**
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run `ui-hud,ui-feed,ui-art` and Phase 1 `combat,morale,reinforce,bots,tutorial,report`; all pass** (57/0 for the core). Grade `ui-hud-cards`, `ui-hud-ability`, `ui-hud-ability-hidden` and greyscale copies: 9 or better.
- [ ] **Step 6: Commit** `"War Table port Task 6: LAST STAND, the ability's own picture, the HUD fixtures"`.

**Risk:** medium. The fixtures are the point: a banner stated in the fixture can still drift from the server's spawn; compare against `Battle` state, not the starter-army literal. Hiding the Quartermaster's slot must not break `UiMath.hudLayout`'s footprint budget (no reserved gap).

---

### Task 7 (lane H): Level pips, the glint, the level-3 feed line (progression Task 12)

**Executor:** Sonnet 5.5 `xhigh` **[design]**, same agent as Task 6, separate commit. **Worktree:** `bannerbound-ui-hud`. **Needs a client:** yes.

**Files:** as progression Task 12: `BattleHud.java` and `UiMath.java` (`hudLayout` reserves the pip and glint slot on the full card and on the chip), `CommandMode.java` (`Group.level2`, `level3`, `armouryOpen` are already read; confirm), `FeedLines.java` (`levelUp(name)` = "<level name> rises"), `FeedEvents.java` (`levelUp(b, team, levelName)` reads `Veterans.pendingFeed`; kind `levelup`, an enemy's line in the enemy colour; level 2 makes no line, `progression.feedLevel` 3), `EventFeed.java`, `tools/ui_art/v4a_hud.py` or `hud_*.py` (pip in two sizes, glint frames, feed glyph), `UiMathCheck.java`, `test_ui_standard.py`, `Phase1Checks.java`.

- [ ] **Step 1: Write the checks (red first), exactly as progression Task 12 Step 1:** `p1-ui-pips` (3 level-2 and 1 level-3 draw their pips on the chip and the full card, screenshot `ui-hud-pips`), `p1-ui-glint` (an open Armoury buy sets the group's glint, buying clears it), `p1-ui-feed-level3` (a Dawn Squire reaching level 3 gives "Knight-Captain rises" once with the `levelup` glyph; an enemy's line in the enemy colour; screenshot `ui-feed-level3`), `p1-ui-feed-level2-silent`, `p1-ui-hud-footprint` (within budget at the five sizes), `UiMathCheck` (`FeedLines.levelUp("Knight-Captain")`, ink 4.5:1), `test_ui_standard.py` (pips and glint never by colour alone: shape count and motion).
- [ ] **Step 2: Run; expect failures. Step 3: paint and implement.**
- [ ] **Step 4: Run `ui-hud,ui-feed,ui-art`, Phase 1 `combat,report`, and Phase 2 `levels,gear,ranks`; all pass.**
- [ ] **Step 5: Grade `ui-hud-pips`, `ui-feed-level3` and greyscale copies: 9 or better.**
- [ ] **Step 6: Commit** `"War Table port Task 7: Iron Ledger pips and glint, the level-3 feed line"`.

**Risk:** medium. The footprint budget is the tight constraint; the Quartermaster hide (Task 6) frees no space and must not be counted on. The glint must read in greyscale (a moving mark, not a tint).

---

### Task 8 (lane A): The Armoury tab (progression Task 8)

**Executor:** Sonnet 5.5 `xhigh` **[design]**. **Worktree:** `bannerbound-ui-arm`. **Starts after Task 0.** **Needs a client:** yes (Phase 2 `armoury,gear`).

**Files:** as progression Task 8, on the merged base.
- Create `ArmouryTab.java`: one row per banner troop type, its portrait at the best worn tiers (`ArmyCards.portrait(spec,w,a)`), four buttons (Weapon II, Weapon III, Armour II, Armour III) in four states read from `CampaignClient.armoury` (`LevelMath.Buy`, `buyState`, `refusal`): locked ("Needs a level-2 Knight"), open (the price), bought, unaffordable (greyed, price shown). A click sends `gear:<troop>:<weapon|armour>:<tier>`. Away from a buy location every button is greyed with the line "Buy at your barracks or a held point". With `progression.enabled` false the tab says "Levels are off".
- Modify `RecruitmentScreen.java`: an Armoury tab beside the barracks page; the screen opens on it with a new key binding.
- Modify `GravesAndGloryClient.java`: the key binding only (a free key, checked against R, G, V, B, M, [, ], H, LAlt and vanilla's; shown through `HintText`; recorded in the ledger).
- Modify `LevelChecks.java`: replace `NotBuilt("armoury", ...)` with the real suite.
- Look: today's barracks style (round 34 answer 4): `DarkUi` tokens, Marcellus headings, the barracks screen's own widgets. Every state has a word or a glyph; the `ux` checks cover contrast and hit targets.

- [ ] **Step 1: Mockup** the tab at 960x540 and 854x480; grade 9 or better; one 960-wide contact sheet for Kort.
- [ ] **Step 2: Write the suite (red first):**
  - `p2-armoury-tab`: a row per banner troop with four buttons in the server's states. Screenshot `armoury-tab`.
  - `p2-armoury-states`: a fixture with one button in each state; labels "Needs a level-2 Knight", "40", "Bought", "80" greyed.
  - `p2-armoury-field`: the key opens the tab in the field; every button greyed with the reason. Screenshot `armoury-field`.
  - `p2-armoury-buy-click`: a click at the barracks buys; the state updates within 2 state packets.
  - `p2-armoury-fits`: at 854x480 every button lies inside the canvas, hit targets of 20x20 or more.
  - `p2-armoury-key-hint`: a rebound key shows its real name.
- [ ] **Step 3: Run; expect failures. Step 4: implement.**
- [ ] **Step 5: Run `armoury,gear`; all pass. Grade `armoury-tab`, `armoury-field` and greyscale copies: 9 or better.**
- [ ] **Step 6: Commit** `"War Table port Task 8: the Armoury tab and key"`.

**Risk:** medium. The key binding can collide with a vanilla or Fabric binding; the tab reads, never computes, buy states (the server is the authority). The War Table restyle of this tab is not in this plan (question 3).

---

### Task 9: Join the lanes and retire the dead Phase 2 pages (controller, then one implementer)

**Executor:** controller for the merges; Sonnet 5.5 `xhigh` for the retirement. **Worktree:** the main checkout (`phase-1-solo-core`). **Needs a client:** yes.

**Files:** merges only, then in `CampScreen.java`, `CampaignLobbyScreen.java`, `RecruitPages.java`, `CampaignClient.java`: delete what nothing references; `docs/PARKED.md` stays for Task 10.

- [ ] **Step 1: Merge the four lane branches into `phase-1-solo-core`, locally, in the order P1, P2, H, A** (the controller). Disjoint files merge cleanly; the only expected conflict is `atlas.json` and the packed PNGs, resolved by `python tools/build_ui_art.py` and committed as generated. Nothing is pushed.
- [ ] **Step 2: Compile both source sets; run the standalone checks, `test_ui_standard.py`, `build_ui_art.py --verify`.**
- [ ] **Step 3: Retire the dead code.** For each of `HouseTab`, `card`, `pickHouse`, `houseTabs`, `rollLabel`, `pityLine`, `pickClaimHouse`, `cycleRare`, `claimHouse`, `rare`, `openTroop`, `seenCards`, `cardLow`, `probe`, the old-layout `troop` body, `CampaignLobbyScreen.ClassCard`, and every `RecruitPages` helper no longer called: `grep` shows no reference in `src/` or `test-tools/`, then delete. Anything still referenced is listed in the report, not deleted. `LobbyScreen`'s banner editor and the `ArmyCards.Colour` seals stay unless the grep says they are unreferenced; the `results` and `train` old-layout pages are out of scope.
- [ ] **Step 4: Run the matrix's four families together:** Phase 1 `ui-art,ui-hud,ui-feed,ui-home,ui-recruit,ui-pages,ui-leave`, Phase 2 `engine,screens,migration,commander,armoury,gear,levels,ranks`. All 20 Phase 1 reds, the six Phase 2 reds, their roll-ups and the Armoury ids pass.
- [ ] **Step 5: Commit** `"War Table port Task 9: lanes joined, the dead camp pages retired"`.

**Risk:** medium. Deleting a widget a harness stage still constructs fails at run time, not compile time for string-routed pages: run `screens` and `commander` after the deletion, not before.

---

### Task 10: Final regression and Kort's gate

**Executor:** Sonnet 5.5 `high`. **Worktree:** the main checkout. **Needs a client:** yes.

**Files:** `docs/UI-GATE.md` (a new "Merged base: the War Table port" section: each screenshot beside its mockup, the grey copy, the rubric lines, the open questions), `docs/PARKED.md` (append: the War Table look for the match-report page and the old `results` and `train` pages, the Armoury restyle, the banner colour row if not built, level-text on the troop sheet if cut), `docs/PHASE-2-PLAYTEST.md` (items: claim shards with a House; recruit by Buy, Star and Pact; read a duplicate card; place a wide troop; pick each class; see the LAST STAND card; open the Armoury), `docs/QUESTIONS-FOR-KORT.md` (append the open questions below, recommended first).

- [ ] **Step 1: Full regression** (merge baselines): Phase 1 `combat,morale,reinforce,bots,tutorial,report` (57/0); `ui-art,ui-hud,ui-feed,ui-home,ui-recruit,ui-pages,ui-leave`; Phase 2 `banners,chase,engine,commander,gilded,screens,migration,spike,gilded-art,anim,tells,depth,faces,turn,hands,levels,gear,statuses,ranks,armoury` (`t3` with `GRAVES_PHASE2_TURN=knight`), the five `levels-<house>` suites; Phase 3 `pipeline,studio,camp,houses,yard`, landmarks and supply on maps 0 to 4, routes; `-PcampaignChecks` alone; the self test. Every id passes, or the red is the known `p2-hands-shots` platform-light flake (merge report 6.3). Expect: no `p1-ui-*` and no `p2-*` red from section 6.
- [ ] **Step 2: Standalone and packaging:** `UiMathCheck`, `BattleMathCheck`, `HousesCheck`, `LevelMathCheck`, `ProfileMigrationCheck`, `test_ui_standard.py`, `build_ui_art.py --verify`, `build_skins.py --verify`, `"$G" build --offline -x test`, `python tools/package.py --allow-stopgap`; the leak check finds no harness class in the jar.
- [ ] **Step 3: Screenshot gate:** build the contact sheets, score every screen (the ten rubric lines, grey copies), send under-9 screens back to their task.
- [ ] **Step 4: Write `UI-GATE.md`, the parked items, the playtest items and the questions; run `GRAPHIFY_BACKEND=none graphify update .`; commit** `"War Table port Task 10: regression, gate page for Kort"`. Hand the branch and `docs/UI-GATE.md` to the orchestrator. Do not merge, push or publish.

**Kort's gate:** as above. Not done until he has seen the sheets.

---

## Spec gaps resolved

1. **Where the checks live.** The ids stay where they are (`p1-ui-*` in `Phase1Checks`, `p2-*` in `Phase2Checks`) and drive the War Table in place ("Phase 2's old screen checks move to the War Table"). No new harness file, so lanes edit disjoint regions of the two big files and merge by line.
2. **The 20 Phase 1 reds are rewritten with their page**, not in a separate fixture task: Contracts and Chapter in Task 1, Banner in Task 3, the recruit eight in Task 4, HUD and feed in Task 6. `p1-ui-feed-rout` becomes `p1-ui-feed-last-stand` (routing is gone).
3. **Claim House default.** The picker defaults to the banner's House and names the House in the reward line; nothing is claimed into a House the player cannot see.
4. **Choose.** It was a refused `unlock:`. The buy, star and Pact actions come from `RecruitPages.action`, so the War Table and the old page can never disagree on a rule.
5. **The old screen's rolls card** (a full-screen dim) is not ported: the War Table's reveal (the letter and the broken seal) replaces it; `p2-roll-card` checks the letter, not a dim (question 7).

## Needs Kort

Appended to `docs/QUESTIONS-FOR-KORT.md` at Task 10 (recommended option first):
1. **Side colour.** The War Table has no way to pick the banner's side colour (`colour:<side>`); only the harness-opened lobby has it, so every player flies gold. (a) Add a four-seal row to the Banner page in Task 3 (recommended); (b) gold only for 0.5.0; (c) put it on the home page.
2. **Troop sheet.** Your answer lists a "troop-shop entry". (a) Build the troop sheet (Task 5), reached from the letter (recommended, since the star ladder, Mastery and the level lines have no other screen); (b) put the actions on the letter only and retire `p2-roll-card-troop`.
3. **Armoury look.** (a) Today's barracks style in Task 8, and the War Table restyle after your gate (recommended: you see the tab working first); (b) restyle it in the War Table look inside Task 8.
4. **Quartermaster slot.** `Commander.ForwardSupply` is registered and runs on the server, though you called it not built. (a) Hide the slot as you ruled and revisit after a playtest (recommended); (b) show it now that the server active exists; (c) hide the slot and also refuse the key.
5. **Claim defaults.** (a) Pre-select the banner's House, named in the reward line (recommended, one click); (b) no default, Claim stays inactive until a House is picked.
6. **Out of scope.** The old-layout `results` and `train` camp pages and the match-report page stay as they are (parked). (a) Agree (recommended); (b) add them.
7. **Roll card.** Phase 2 showed a roll as a full-screen card over a dimmed screen. The War Table already reveals a roll as the opened letter with a broken seal (UI Task 6, your look). (a) Keep the letter as the reveal and rewrite `p2-roll-card` to check it (recommended, Task 4); (b) port the full-screen card too.
