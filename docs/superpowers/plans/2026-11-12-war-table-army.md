# War Table Army Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 20-entry 5x4 banner with a 9 x 6 war table under an equal gold allowance, split saved gold into troop writs and gear shards, and add the `banner` formation shape, without changing anything the design does not name.

**Architecture:** One pure class `WarGrid` owns cell geometry, the compact cell string, the role placement rule and the v6 to v7 banner move; `RosterMath.validTable` is the only banner rule, used by the server, the client painter (`BannerDrop`), preflight's Java twin check and bots. The profile stores up to 5 sparse banners as compact strings; a battle `Squad.banner` becomes the dense 54-entry list of the active banner (index = `row * 9 + col`, `empty` where nothing stands), so code that reads a banner by position keeps working with a wider list. A troop with no cell takes `slot >= WarGrid.CELLS`.

**Tech Stack:** Java 17, Fabric 1.20.1, Gson 2.10.1, `sheets/design.json` -> `tools/preflight.py` -> generated `Spec.java`, standalone checks in `test-tools/standalone`, in-game suites through `tools/checkq.py`.

**Spec:** `docs/superpowers/specs/2026-11-12-war-table-army-design.md` (approved by Kort 2026-11-12). Builder rulings: `C:\Users\kortt\Melty\status\army-lead.md` (R-numbers below refer to it).

## Global Constraints

- Never hand-edit `Spec.java`: edit `sheets/design.json`, regenerate with `./gradlew compileJava` (runs `tools/preflight.py`).
- Never write `eula.txt`. Never kill a game client. In-game suites and sims only through `python tools/checkq.py submit ...` then `wait` in a background shell. No push, merge or publish. Work only in `C:\Users\kortt\Melty\bb-army`.
- Grid: `bannerCols` 9, `bannerRows` 6; row 0 is the front; cell index `row * 9 + col` (0-53).
- `musterAllowance` 640 (R1), `maxTroops` 40, `maxBanners` 5, banner name 1-16 characters, server-trimmed.
- Muster cost of a troop = `troops[].cost`. Legendary limit 1 per banner; no Epic limit (`rarities.bannerMax`: common, rare, epic 40; legendary 1).
- Profile `SCHEMA` 7. `writs` (Pact count) is renamed `pacts`. New `troopWrits`, `gearShards`. Saved `gold` removed. `Squad.gold` (in-match purse) unchanged.
- Rates: roll 1 writ, chosen troop 3 writs; a match pays writs = today's gold payout / 100 with the remainder kept in `writGold` (0-99) and carried; shards = today's gold payout; Forge gold prices become the same numbers in gear shards. Migration of saved gold: writs = gold / 100, gear shards = gold % 100.
- Forge marks merge into gear shards (Kort Q1, 2026-11-12): `forgeMarks` is removed; saved marks add 1:1 to `gearShards`; a match pays its old marks amount plus its old gold payout as gear shards; gear rolls (`forge.rollMarks`), breakdowns, repairs and rows all use `gearShards`; refusal texts say "gear shards". `GearWire` keeps its field position (it carries gear shards now).
- Formations: `line` removed, `banner` added (ids: block, banner, wedge, loose). A saved or sent `line` reads as `block`.
- Server commands: `cell:<banner>:<col>:<row>:<id|->`, `banner:new:<name>`, `banner:rename:<banner>:<name>`, `banner:copy:<banner>`, `banner:delete:<banner>`, `banner:use:<banner>`. `slot:` is removed. A refused command changes nothing and sends a notice.
- `SNAPSHOT_LIMIT` 16000 and the 32000 import cap must hold for a worst-case profile (5 banners x 40 troops).
- Text in plain words; follow existing comment style (Javadoc on every new method, as the file around it).
- Every commit ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Commit only named task files.

## Review Focus

1. An old profile whose 20-entry banner holds an Epic `^` pair or a Legendary `^^^` run: the markers vanish and the troop lands once, in the cell of its first entry (Task 2 fixture `profile-v6-wide.json`).
2. A banner that was valid at save time but is not now (troop sold off, House not fieldable, sheet cost raised): sanitize trims it from the back rows, never wipes it, and the player is told once (Task 2 test `sanitizeTrimsNotWipes`).
3. Refills while the troop's own cell is taken by a living troop of the same type: the refill waits for its own cell, no other; the queued gold is refunded at round end as today (Task 3 in-game check `refillOwnCell`).
4. A Hold with shape `banner` given to one group only while another group holds a different order: the other group does not move (Task 4 in-game check `bannerShapeSelectedOnly`).
5. A client that sends `cell:` with a banner index 5, a column 9, a negative row, an unowned or Legendary-twice troop: each is refused with a notice and the saved file is unchanged (Task 7 test `cellCommandRefusals` in `LobbyRulesCheck`).

## Audit additions (re-run 2026-11-12, full list in `2026-11-12-war-table-army-audit.md`)

Sites section 9 of the design missed, and the task that owns each:
- Task 2: fixtures `test-tools/resources/fixtures/profile-v3-veteran.json`, `test-tools/qa/profile-v2-seed.json` (gold, banner, writs); `GearMigrationCheck`; `server_migration_probe.py`.
- Task 2 / 5: the match report already sends `"pacts"` (earned this match) beside `"writs"` (held): `Campaign.java:665`, `WarTableReport.java:337`, `UiReportChecks.java:91`. Ruling R3: the held count's report key becomes `"pactsHeld"`; `"pacts"` keeps its meaning.
- Task 3: `Unit.temporary` already exists (R2); slot sites `Commander.java:37-38`, `GravesAndGlory.java:1105-1107`, `SurvivalMode.java:298`. `ModeLines.java:27` sorts its own rows, not units: unaffected. `BattleMathCheck` (`deployRing`).
- Task 4: `CommandPanel.java:38,411-444`, the `g-line` PNG icons, `sheets/design.json` formation text near line 1834, `OrderIntakeProbeFormation:50`.
- Task 5: profile gold in `Progression.java:33,109-115`, `MarchText.java:11-18`, `MarchMath.java:76-91`, `MatchRules.java:35-51`, `Campaign.java:145`, `CampScreen.java:82,135`, `GravesAndGloryClient.java:353`, `WarTableGear.java:592`, `WarTableRecruit.java:630`; checks `CampRedesignChecks`, `MarchBonusChecks`, `ForgeCheck`, `GearCommandsCheck`, harness `RewardDriver`, `SliceClient`, `SliceProbe`, `k7_rewards.py`; `forgeMarks` sites `Gear`, `GearWire`, `GearView`, `WarTableGear` (see Q1).
- Task 6: `LevelMathCheck:120` strips `^` from a `botBanners` row.
- Task 7: `WarTablePages.java` banner reads at 556-616, 765-869, 1160, 1291.
- Checked unaffected: lang file, `tools/contact_sheet.py` (its `line` is a drawing primitive), `balance_gate.py`, `progression_gate.py`, `state.py`, `tests/`.

---

### Task 1: Sheet values, `WarGrid`, the validator

**Files:**
- Modify: `sheets/design.json` (`rules` every row: add `bannerCols` 9, `bannerRows` 6, `musterAllowance` 640, `maxTroops` 40, `maxBanners` 5; `rarities.bannerMax` common/rare/epic 40, legendary 1; `economy.maxQueue` 40)
- Modify: `tools/preflight.py` (accept the new columns; check `maxTroops <= bannerCols * bannerRows`, `musterAllowance >= maxTroops * 0` sanity only)
- Create: `src/main/java/gg/gravesandglory/WarGrid.java`
- Modify: `src/main/java/gg/gravesandglory/RosterMath.java` (Catalog gains `cost`; add `validTable`)
- Modify: `tools/verify.py` (`names` list gains `WarGrid`)
- Create: `test-tools/standalone/WarGridCheck.java`

**Interfaces:**
- Produces (all `static`, pure, no Minecraft types):
  - `WarGrid.COLS`, `ROWS`, `CELLS` read from `Spec.RULES[0]` through methods `cols()`, `rows()`, `cells()`; `int index(int col, int row)`, `int col(int i)`, `int row(int i)`, `boolean inRange(int i)`.
  - `SortedMap<Integer,String> parse(String cells)` — format `"<index>:<id>,<index>:<id>"`, sorted by index; a malformed pair is skipped, a repeated index keeps the first.
  - `String format(Map<Integer,String> table)` — the inverse, sorted, `""` for empty.
  - `List<String> dense(Map<Integer,String> table)` — `cells()` entries, `RosterMath.EMPTY` where nothing stands.
  - `SortedMap<Integer,String> sparse(List<String> dense)` — inverse of `dense`, skips `EMPTY`, `MARKER`, null.
  - `int cost(Map<Integer,String> table, RosterMath.Catalog c)`, `int troops(Map<Integer,String> table, RosterMath.Catalog c)` (known ids only).
  - `SortedMap<Integer,String> place(List<String> ordered, ToIntFunction<String> rank)` — the placement rule (below).
  - `SortedMap<Integer,String> trim(SortedMap<Integer,String> table, Predicate<SortedMap<Integer,String>> ok)` — removes the highest-index troop (last row, from its right end backwards) until `ok` holds or the table is empty.
  - `record Moved(SortedMap<Integer,String> table, int trimmed)`; `Moved fromLine(List<String> line20, RosterMath.Catalog c, Predicate<SortedMap<Integer,String>> ok)` — entry `i` of an old banner to cell `(2 + i % 5, i / 5)`, markers and `empty` skipped, then `trim`.
  - `RosterMath.Catalog.cost(String id)` (0 for unknown), `Catalog` record gains `Map<String,Integer> cost` filled from `troops[].cost` in `fromSpec()`.
  - `String RosterMath.validTable(Map<Integer,String> table, Catalog c, Set<String> owned, Set<String> houses)` — null when valid, else the first failure in this order: `range`, `unknown`, `not_owned`, `house` (troop's House not in `houses`), `count` (> `maxTroops`), `cost` (> `musterAllowance`), then `<rarity>_limit` rarest first from `rarities.bannerMax`.
  - `RosterMath.allowance()`, `RosterMath.maxTroops()`, `RosterMath.maxBanners()`.

Placement rule (`place`): `rank` returns 0 for a shield troop, 1 for other melee, 2 for ranged or magic. Sort the ordered list stably by rank. Fill rows from row 0; within a row fill columns centre-out `4,3,5,2,6,1,7,0,8`; a new rank starts a new row. When more troops remain than cells, stop at `cells()`.

- [ ] **Step 1: Write `WarGridCheck`** (standalone, same harness style as `HousesCheck`): `parseFormatRoundTrip` (`"0:knight,10:archer"` -> map -> same string; `"x:knight,0:a,0:b"` -> `{0=a}`), `denseSparse` (54 entries, round trip), `indexMath` (`index(4,1)==13`, `col(13)==4`, `row(13)==1`, `inRange(54)==false`), `placeCentresByRole` (3 shield, 2 archer -> cells 4,3,5 in row 0, then 13,12 in row 1), `trimFromBack` (removes index 53 before 52 before 9), `fromLineMovesAndSkipsMarkers` (`[mage,^,knight,...]` -> mage at cell 2, knight at cell 4 (entry 2 -> col 4, row 0)), `validTableOrder` (one case per failure name, in order, including `legendary_limit` with two Legendaries and no failure for three Epics), `starterValid` (the Task 2 starter table is valid with all Commons owned), `botBannersValid` (every `Spec.BOTBANNERS` row, placed by Task 6's rule, passes with all troops owned — mark `TODO Task 6` and skip until Task 6 lands, the only allowed skip).
- [ ] **Step 2: Run** `python tools/verify.py fast --offline` — expect `WarGridCheck` compile failure.
- [ ] **Step 3: Edit the sheet, run `./gradlew compileJava`**, then implement `WarGrid` and `RosterMath` additions.
- [ ] **Step 4: Run** `python tools/verify.py fast --offline` — `WarGridCheck` passes; other checks may fail only where they assert the old `bannerMax` epic value (fix them in this task).
- [ ] **Step 5: Commit** `feat(army): war table cells, allowance and the one banner validator`.

### Task 2: Profile schema 7 and migration

**Files:**
- Modify: `Profile.java`, `ProfileMigration.java`, `LegacyDataMigration.java` (only if it touches banner or gold), `Campaign.java:137-145` (the import comparison uses `troopWrits` and owned instead of `gold`)
- Create fixtures: `test-tools/resources/fixtures/profile-v6-starter.json`, `profile-v6-wide.json` (Epic and Legendary with markers), `profile-v6-over.json` (20 entries costing more than 640 — use Legendary + Epics + Rares; if no owned set exceeds 640 with 20 entries, lower nothing and instead assert the trim path with a test-only predicate), `profile-v6-gold.json` (gold 1234, writs 2)
- Modify: `test-tools/standalone/ProfileMigrationCheck.java`, `ProfileFixtureCheck.java`, `PersistedDataCorruptionCheck.java`, `CampaignResetCheck.java`, `HousesCheck.java`

**Interfaces:**
- Consumes: Task 1 `WarGrid`, `RosterMath.validTable`.
- Produces on `Profile`:
  - `static final int SCHEMA = 7`, `MAX_BANNER_NAME = 16`.
  - `static final class Banner { String name; String cells; }` (Gson class, not a record, so old files missing fields load).
  - Fields `List<Banner> banners` (default: one banner "Banner I" holding `starterTable()`), `int activeBanner`, `int pacts`, `int troopWrits`, `int writGold` (0-99), `int gearShards`, `String notice` (one-time text, cleared by the server after it is shown; null when none).
  - Legacy read-only fields, null after sanitize so Gson never writes them: `List<String> banner`, `Integer gold`, `Integer writs`.
  - `SortedMap<Integer,String> table()` (active banner parsed), `List<String> line()` (`WarGrid.dense(table())`), `void setTable(int banner, Map<Integer,String> t)`.
  - `static SortedMap<Integer,String> starterTable()` — 12 `knight` then 8 `archer` through `WarGrid.place` (knights rank 0, archers rank 2).
  - `ProfileMigration.v6toV7(Profile p, RosterMath.Catalog c)`: if `banners` is empty, move legacy `banner` with `WarGrid.fromLine` (ok = `validTable(...)==null` against owned and every House); if `trimmed > 0` set `notice` = "Your banner cost more than the war table allows; N troop(s) were taken off the back rows."; `pacts = writs`; `troopWrits = gold / 100`, `gearShards = gold % 100` (Task 5 extends v6toV7: `gearShards += forgeMarks` and drops `forgeMarks`); null the legacy fields.
- Sanitize (replaces the `validBanner`/`repack` line): 1 to `maxBanners` banners (none -> starter), names trimmed of control characters, cut to 16, blank -> "Banner N"; each table: drop out-of-range, unknown, unowned cells, then `WarGrid.trim` until `validTable` passes against all fieldable Houses; `activeBanner` clamped; `troopWrits`, `gearShards` 0..1,000,000, `writGold` 0..99, `pacts` >= 0.
- Keep `RosterMath.validBanner`, `repack`, `starterBanner` only while v1/v2 migration needs them; delete what nothing reads at the end of Task 7.

- [ ] **Step 1: Tests first** in `ProfileMigrationCheck`: `v6StarterMoves` (12 knights at cells 2-6 row 0, 7-? per `fromLine`, archers rows 2-3; no notice), `v6WideDropsMarkers`, `v6OverTrims` (notice set, trimmed count right, result valid), `v6GoldSplit` (1234 -> writs 12, shards 34; writs 2 -> pacts 2), `v1ChainsToV7` (existing v1 fixtures end at schema 7 with a valid table), `sanitizeTrimsNotWipes`, `bannerNamesClamp` (17 chars -> 16, blank -> "Banner 2", 6 banners -> 5), `legacyFieldsNotWritten` (Gson output of a migrated profile has no `"banner"`, `"gold"`, `"writs"` keys).
- [ ] **Step 2: Run** fast verify — expect failures.
- [ ] **Step 3: Implement**; update every reader of `Profile.banner`/`gold`/`writs` that the compiler flags in shared (non-client, non-battle) code to the new accessors with no behaviour change beyond the rename; for battle and client readers use `line()` as a mechanical stand-in (Tasks 3 and 7 rework them).
- [ ] **Step 4: Run** fast verify — green.
- [ ] **Step 5: Commit** `feat(army): profile schema 7 with five war table banners, pacts and the gold split`.

### Task 3: Battle cells, deployment, refills

**Files:** `GravesAndGlory.java` (Squad.banner, Unit, `reinforce`, `deploy`/`ringPlace`, training refusal 1068-1075, queue 1100-1110, auto-reinforce 850, test squads 1410/1436/1484/1503), `Campaign.java` (`prepareRound` 339-403, bot reinforcement 744-750, 794), `Commander.java:30-41`, `SurvivalMode.java:298`, `Morale.java:34`, `BattleMath.java` (deploy helpers), `Tactics.java:494,506`, `RosterMath.houseSlots`, `Houses.java:37`, `BarracksPage.java`, `RecruitmentScreen.java`, `RecruitView.java`, `Veterans.java` (slot sites), in-game checks `Phase1Checks`, `Phase2Checks`, `BannerFieldChecks`, `HoldLineChecks`, `BotSimulation`, `DuelSimulation`, `CampaignChecks`, `MapRouteChecks`, `LevelChecks`, `GeoGaps`, `CustomGroupsProbe` where they build banners or read slots.

**Interfaces:**
- Consumes: `WarGrid`, `Profile.line()`.
- Produces: `Squad.banner` is a dense list of `WarGrid.cells()` entries. `Unit.hasCell()` = `slot >= 0 && slot < WarGrid.cells()`. A cell-less troop's slot is `WarGrid.cells() + n`. `RosterMath.houseSlots(List<String> line, Catalog)` sums `slotWeight` per House over known ids (markers no longer exist). `BattleMath.tableOffset(int cell, double colGap, double rowGap)` returns `{side, back}` = `{(col-4)*colGap, row*rowGap}`.

Rules:
- Deployment: a troop at cell `i` stands at `tableOffset(i, cellGap(row), rowDepth)` from the squad anchor, keeping today's spacing: lateral gap 2.5 (a shield troop at its wall cell, an all-shield row closes as today via `deployRow` generalised to 9 places), rows 3 apart (`deployDepths` generalised). Rows with no troop take no depth (the army stands from its first filled row). Forward reinforcement `ringPlace` uses the same offsets.
- Training: a troop may queue while its living + queued count is below its count in the banner, and living + queued of all troops < `maxTroops`; the "Army full" text changes to "Army full: your banner has no free place for this troop." `economy.maxQueue` 40.
- Refill: a trained troop takes the lowest free cell holding its id in the banner; if none is free its gold is refunded (`s.gold += t.paid()`).
- Bots: buy toward the banner counts with battle gold, capped by `maxTroops` (no weight).
- Auto-reinforce (850): trigger when fielded count < banner troop count.
- Commander temporaries and Survival waves: slot `cells() + n`; Survival keeps its spread by `n`.
- Tactics lane offsets: `(WarGrid.col(u.slot) - 4)` for celled troops, `(u.slot % 5 - 2)` for cell-less.

- [ ] **Step 1: Tests first**: standalone `BattleMathCheck` gains `tableOffsetCentres` (cell 4 -> side 0; cell 0 -> -4*gap; cell 13 -> back = rowGap), `deployRowNineWide`; `HousesCheck` gains `houseSlotsSumsWeights` (mage weight 2 -> 2 slots without markers). In-game: `BannerFieldChecks` gains `deploysAtCells` (a 9x6 banner with troops at cells 0, 8, 49: lateral spread = 8 gaps, cell 49 behind cell 0 by 5 row depths ± 0.5), `refillOwnCell` (kill the troop at cell 8, queue one; it deploys to cell 8's place), `cellessNeverRefunded` (a Grave Bloom Risen and a Survival wave troop have `!hasCell()`; queue refunds count = 0), `fortyTroops` (a 40-troop banner fields 40 units).
- [ ] **Step 2: Run** fast verify (standalone fail) and submit the changed in-game suites through checkq to record the baseline failures.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** fast verify green; `python tools/checkq.py submit client --root army-t3 --suites <changed suites>`, `wait` in background — green.
- [ ] **Step 5: Commit** `feat(army): troops deploy and refill at their war table cells`.

### Task 4: `banner` shape, `line` removed

**Files:** `sheets/design.json` (`formations`: replace `line` with `{id:"banner", label:"Banner", columns:9, spacing:2.5, wedge:false, purpose:"Stand as the war table lays the army out.", moveMultiplier:1.0, arrowBlock:0.0, shove:0.0}`; `ui.command.bottom`, `tips` tip-05 and tip-12, `campLessons.lesson-2`, `troopNotes.wm_oldhorn`, every `line` text), `OrderRules.java`, `CommandSelection.java:27,70`, `CommandMode.java:21,109-135`, `BattleHud.java:552,558`, `OrderOutlines.java`, `FormationRules.java`, `GravesAndGlory.formation(Unit)`, `src/main/resources/.../atlas.json:26-27`, `tools/ui_art/manifest.py:50`, `tools/ui_art/v4a_hud.py:481`, `tools/implement_command_ui.py:45` (dead script: leave), tests `OrderRulesCheck`, `FormationRulesCheck`, `tests/test_command_ui_sheet.py`, `tests/test_ui_art.py`, `HoldLineChecks`, `cmdui/*`.

**Interfaces:** `OrderRules.formationId(String sent)` returns the id, `"block"` for `line`, null for anything unknown (refused as today). In `formation(Unit)`: when the formation is `banner` and `u.hasCell()`, place = `BattleMath.tableOffset(u.slot, 2.5, 3)` (no `roleDepth`, no closing up; dead troops leave gaps because the offset is per cell); cell-less members stand in a row behind row `rows()`, spaced 2.5 by their order.

- [ ] **Step 1: Tests first**: `OrderRulesCheck` `lineReadsAsBlock`, `bannerKnown`; in-game `HoldLineChecks` `bannerShapePlaces` (Hold + banner at a point facing east: cell 4 at the point, cell 0 4 gaps to the left of the facing), `bannerShapeGaps` (kill cell 3; cell 2 does not move), `bannerShapeSelectedOnly`; `test_command_ui_sheet.py` expects the four ids.
- [ ] **Step 2-4:** run, implement (HUD button art: reuse the `line` icon frame relabelled until the art lane draws one; record that as a follow-up), run green (fast + changed suites via checkq).
- [ ] **Step 5: Commit** `feat(army): banner formation shape replaces line`.

### Task 5: Currencies and `starsCount`

**Files:** `Campaign.java` (roll 438-445, buy 499-500, payouts 560-564 and 604-613, tutorial 1011, progression claim 1137, March bonus 610, forClient), `Forge.java:127-167`, `MarchMath.java`/`MarchText.java` (bonus text), `MatchRules.java` (add `starsCount` rule), `Campaign.progression()`, `RecruitPages.java`, `RecruitView.java`, `CampScreen.java:82,135`, `GravesAndGloryClient.java:353`, `ArmouryTab.java:276` (check whether it is the purse), `WarTableRecruit.java`, `WarTableReport.java`, `GearView.java`, `sheets/design.json` (`recruitment.rollCost` 1, `chosenCost` 3 in writs; `rarities.chosenPrice` -> writs 3; `economyModel`), `tools/economy_model.py`, `test_economy_model.py`, `ForgeCheck`, `GearCommandsCheck`, `MatchRulesCheck`.

**Interfaces:** `RosterMath.payWrits(Profile p, int goldEquivalent)` adds `(writGold + g) / 100` to `troopWrits` and keeps the remainder; `Profile.gearShards += goldEquivalent`. Forge costs read the same sheet numbers, charged in `gearShards`, refusal text "… costs N gear shards." `MatchRules.starsCount(String mode)` default true; `Campaign.progression(...)` returns 1 when false.

- [ ] **Step 1: Tests first**: `MatchRulesCheck` `starsOffGivesOne`; `ForgeCheck` repair/expand spend shards; standalone `writsCarryRemainder` (80 then 40 -> 1 writ, writGold 20); `test_economy_model.py` time-to-first-Epic unchanged within 1%.
- [ ] **Step 2-4:** run, implement, run green.
- [ ] **Step 5: Commit** `feat(army): troop writs and gear shards replace saved gold`.

### Task 6: Bots and other army builders

**Files:** `sheets/design.json` (`botBanners.slots` -> plain ordered troop lists, `^` removed), `tools/preflight.py:308-332,464,576-590,673-674,929` (bot banner checks: known ids, no `^`, count <= `maxTroops`, cost <= `musterAllowance`, Legendary <= 1; checks `army-capacity`, `troops-commanded` restated for the table), `Campaign.botBanner`/`bannerRow`/`absent copy (174)`, `MarchMath.java:94-193`, synthetic banners in tests, `test_preflight_rules.py`, `test_preflight_houses.py`, `test_preflight_campaign.py`, `tools/where_extract.py`, `test_where.py`, `tools/balance_method.py:601`.

**Interfaces:** `WarGrid.botTable(List<String> ordered)` = `place(ordered, WarGrid::role)` where `role(String id)` reads tags from `Spec.TROOPS` (shield 0, ranged or magic 2, else 1). Every army builder (bot, campaign enemy, boss, ally, house chapter row, synthetic, absent copy) goes through `botTable` then `dense`.

- [ ] **Step 1:** un-skip `WarGridCheck.botBannersValid`; add `test_preflight_rules.py` cases (a `^` refused, 41 troops refused, cost 641 refused).
- [ ] **Step 2-4:** run, implement, run green, plus `BotSimulation` one short match via checkq to prove bots field.
- [ ] **Step 5: Commit** `feat(army): bots and campaign armies use the war table`.

### Task 7: Lobby war table page and commands

Worker: Opus medium (lobby UI).

**Files:** `Campaign.java:238-262` (commands), `BannerDrop.java` (rewrite as the paint rules), `WarTablePages.java` (banner page), `WarTableLobby.java:126`, `WarTableTroop.java:497`, `CampaignClient.java:22,96-107,125`, `GravesAndGloryClient.java:140`, `test-tools/standalone/BannerDropCheck.java`, `LobbyRulesCheck.java`, new standalone `SnapshotSizeCheck.java`, `LobbyChecks` in-game.

**Interfaces:** `BannerDrop.paint(SortedMap<Integer,String> table, int cell, String brush, Rules r)` returns `Plan(ok, why, command, result)`; `Rules(Catalog, owned, houses)`; the client never changes the table except by a server-accepted command (it may show `result` optimistically). Server applies the same `paint` and `validTable`.

Page: 9 x 6 grid (row 0 at the top, labelled "Front"), gold bar `spent / 640` and `troops / 40`, owned-troop palette (face from `TroopFaces`, rarity frame, price), banner tabs (up to 5; new, rename, copy, delete, set active). Left click or drag paints the brush; clicking a cell holding the brush's troop removes it; right click removes. Unaffordable or blocked troops greyed with the reason (`validTable` failure in words). Follow the War Table style and `docs/visual-references`.

- [ ] **Step 1: Tests first**: `BannerDropCheck` rewritten (`paintPlaces`, `samebrushRemoves`, `refusesOverCost`, `refusesSecondLegendary`, `refusesOutOfRange`); `LobbyRulesCheck.cellCommandRefusals`; `SnapshotSizeCheck` (worst profile: 5 banners x 40 cells of the longest troop id, 16-char names, a full gear inventory as the sheet allows -> `Campaign`-style JSON <= 16000 pretty or compact form, import JSON <= 32000).
- [ ] **Step 2-4:** run, implement, run green; `LobbyChecks` via checkq.
- [ ] **Step 4b: The one-time notice** (`Profile.notice`): the server shows it once when the player opens the War Table (or joins), then sets it null and saves; `LobbyRulesCheck` or an in-game check proves it is shown once and gone after a save and reload.
- [ ] **Step 5:** in-game screenshots of the page at normal and small window sizes, hover and refused states, to `work/army/screens/`.
- [ ] **Step 6: Commit** `feat(army): war table page paints the 9 x 6 banner`.

### Task 8: Remaining tests, docs, sim, load test

- Docs: `README.md`, `docs/PLAYER-README.md`, `docs/discord/start-here.md`, `docs/releases/1.0.0-notes-DRAFT.md`, `docs/QA-MATRIX.md`, `docs/NEW-UNIT.md`, `PHASE-2-BALANCE.md`; mark `docs/superpowers/specs/2026-11-09-army-economy-design.md` superseded.
- Remove `rules.squadSize` once nothing reads it (grep proves it); remove dead `validBanner`/`repack`/`MARKER` code nothing reads.
- Balance sim rerun via `checkq submit sim` with the same arguments as the last recorded run; compare win rates; report any shift.
- Load test: `BotSimulation` probe, 4 armies (2 v 2), each a 40-troop banner (`wm_hound` x 40 = 640), Domination, 10 minutes of round time; report `tickMeanMs`, `tickP95Ms`, `tickP99Ms`, `tickMaxMs`, `ticksOver50ms` against the same run with today's 20-troop bot banners.
- Final: `python tools/verify.py fast --offline` and `game --offline` green, rebase on `main`, re-run fast.
