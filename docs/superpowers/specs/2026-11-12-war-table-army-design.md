# War table army rework: design

Status: outline approved by Kort 2026-11-12 (question rounds). This document awaits his approval before the build starts.
Replaces `2026-11-09-army-economy-design.md` (never built). Mockup: https://claude.ai/artifact/JZRhLQ29TCdPU5GJLrmbvU
Target: 1.20.1 now; the 1.21.1 port carries it across.

## 1. Intent

Kort: "i dont like the fact we have a 20 banner limit ui screen in lobby where you can only place units in a 5 by 4 lineup." He wants to keep the banner idea (place troops in slots, the layout is how they stand) and lose the fixed size and the fixed count. It must suit ranked PvP, online servers and later expansion.

## 2. Goals, non-goals, anti-goals

Goals
- A 9 wide x 6 deep war table (54 cells, gaps allowed) replaces the 20-entry 5x4 banner.
- An equal gold allowance replaces the 20 slot-weight cap. Kort: "everyone has the same amount of money but how you use it depends on you."
- Three currencies with one job each. Kort: "writs are for the troops, gear shards are for weapons and armors, gold is for in matches and banner in lobby."
- The table layout matters in battle: it sets deployment, and a `banner` formation shape re-forms it.
- No behaviour change for anything not named here. Every site in section 9 is handled or shown unaffected.

Non-goals (not in this build)
- A ranked mode or matchmaking. Only the rule switch that ranked will use.
- Wings (left, centre, right thirds) as command groups. Deferred by Kort.
- Round structure (best of 5, pick phase), carried survivors, slot fees. Dropped with the old spec.
- Rebalancing troop prices. Existing `troops[].cost` is used as is.

Anti-goals
- Any way for one player to field more gold worth of army than another in the same match.
- Old saves losing owned troops, stars, gear or progress.
- A client that can send a banner the server has not checked.

## 3. Decided by Kort

| Topic | Decision |
|---|---|
| Grid | War table 9 x 6, gaps allowed |
| Limit | Equal gold allowance; a mixed army is about 20 troops; the server refuses more than 40 troops |
| Owning | Owning a troop type lets you field as many as you can afford |
| Troop writs | Earned at the end of rounds; pay for everything that gets a troop (rolls and chosen troops) |
| Gear shards | Pay for weapons and armour (the Forge) |
| Gold | In-match purse (as today) and the lobby allowance. No longer a saved currency |
| Refills | In battle you refill what you bought; costs battle gold as today |
| Stars | Give stats in casual play, ignored in ranked |
| Formations | `line` is removed; `banner` takes its place (block, banner, wedge, loose) |
| Screen polish | Paint placement; up to 5 saved banners; troop face, rarity frame and price on each slot |
| Old saves | Banner moves onto the table; if it costs more than the allowance, trim from the back rows and tell the player |

## 4. Design

### 4.1 Banner data
- A banner is a sparse map from cell (col 0-8, row 0-5; row 0 is the front) to troop id. Empty cells are not stored.
- A profile holds 1 to 5 named banners and the index of the active one. Names: 1 to 16 characters, server-trimmed.
- A banner is valid when: every cell is in range; every troop is owned and fieldable for the player's House; total muster cost <= allowance; troop count <= `maxTroops`; Legendary count <= 1 (see 4.2).
- One validator in `RosterMath` is the only rule. The client mirror (`BannerDrop`) calls the same shared code, not a copy.

### 4.2 Allowance
- New sheet values in `rules`: `banner.cols` 9, `banner.rows` 6, `musterAllowance`, `maxTroops` 40, `maxBanners` 5. `squadSize` is removed once nothing reads it.
- Muster cost of a troop = its existing `troops[].cost`.
- `musterAllowance` is set so the current default starter banner costs about 95% of it (about 600; the build fixes the number from the sheet and the balance sim confirms a 16-cost horde stays at or under 40).
- `rarities.slotWeight` stays, but only as House weight (House trait tiers, shard and Renown rewards). It no longer limits the army.
- `rarities.bannerMax`: the Epic limit is dropped (price limits it). Legendary stays at 1 per banner. *My call:* a second Legendary at 120-150 gold would be affordable and they are built as one-per-army units.

### 4.3 Currencies
- `troopWrits` (new profile field; the existing `writs` field is the Legendary Pact count and is renamed `pacts` in code and save). Earned at match end where persistent gold is paid today (`Campaign.finish`, `payPartial`, progression claims, tutorial grant). Spent on rolls and chosen troops.
- `gearShards` (new profile field). Earned at match end. Spent in the Forge in place of gold.
- Persistent `gold` is removed from the profile. `Squad.gold` (the in-match purse) is unchanged.
- Rates (*my call, to be checked by `tools/economy_model.py` so time-to-unlock stays as today*): 1 roll = 1 writ, 1 chosen troop = 3 writs; a match pays writs equal to today's gold / 100 with the remainder carried as a fraction in the profile; Forge prices become shards at the same numbers as today's gold prices, and a match pays shards equal to today's gold payout.
- Migration of saved gold: writs = gold / 100 (rounded down), gear shards = the rest. Nothing is lost.

### 4.4 Lobby screen
- War table page shows the 9 x 6 grid, the gold bar (spent / allowance, troop count / 40), a troop palette of owned troops with face, rarity frame and price, and banner tabs (up to 5: new, rename, copy, delete, set active).
- Paint: pick a troop, click or drag across cells to place; click a placed troop with the same brush to remove; right-click removes. A troop you cannot afford is greyed with the reason.
- Server commands replace `slot:i:id`: `cell:<banner>:<col>:<row>:<id|->`, `banner:new|rename|copy|delete|use:...`. Each is validated; a refused command leaves the banner as it was and returns a notice.

### 4.5 Battle
- Deployment: each troop deploys at its cell's offset from the squad anchor, using the existing cell spacing; the table is centred on column 4. `deployRing` and the 5-wide spawn math are replaced.
- `Unit.slot` becomes the cell index `row * 9 + col` (0-53). A new `Unit.temporary` flag replaces the `slot >= squadSize` convention (Commander, Survival, the refund path).
- Tactics lane offsets use `col - 4`; the `% 2`, `% 10` and golden-angle uses keep working on the cell index.
- Refills: a dead troop can be retrained into its own cell only, for battle gold, as today. `maxQueue` follows `maxTroops`.
- `banner` shape: on Hold with shape `banner`, each living troop's place is its cell offset from the aim point, rotated by the aim facing, same spacing as deployment. Dead troops leave gaps (no closing up). It applies to the selected groups only; troops outside the selection keep their orders. Temporary troops (no cell) fall in behind the last row.
- `line` is removed from the sheet, HUD, atlas, tips and lessons. Unknown or saved `line` loads as `block`.

### 4.6 Bots and other army builders
- `botBanners` rows become cell layouts. *My call:* the sheet keeps an ordered troop list per row and a placement rule fills the table by role (shields front, ranged back, centred), so designers do not hand-place 54 cells. Every bot banner must pass the same validator at preflight.
- Campaign enemies, bosses, allies (`MarchMath`), synthetic test banners and the absent-seat copy use the same placement rule.
- Bot reinforcement buying uses battle gold and the 40 troop limit in place of weight.
- Survival waves and Commander temporary troops set `temporary` and take no cell.

### 4.7 Stars and ranked
- New match rule `starsCount` (default true). `Campaign.progression()` returns 1 when it is false. Nothing sets it to false yet; a check proves the switch works.

### 4.8 Saves and network
- Profile schema 7. Migration from 6: banner entry `i` goes to cell (col `2 + i % 5`, row `i / 5`); if it fails the validator on cost, remove troops from the last row backwards until it passes and set a one-time notice; `writs` to `pacts`; gold split per 4.3. Older schemas chain through 6 as today.
- Banners are sent as compact strings (cell index and troop id pairs). The build measures a worst-case profile (5 full banners) against `SNAPSHOT_LIMIT` 16000 and the 32000 import cap and adds a check that fails if either is exceeded.

## 5. Left to the builder
- Exact `musterAllowance`, earn rates and shard prices, within the rules in 4.2 and 4.3.
- Screen layout and art of the war table page, following the existing war table style.
- Internal data structures and packet formats.

## 6. Proof required before landing
- Every standalone check, Python test, and in-game suite in section 9 updated and green; `verify` green.
- New checks: validator (cost, count, Legendary, range, ownership); migration fixtures (v1 to v7, over-allowance trim, gold split); snapshot size worst case; `banner` shape places and gaps; temporary troops never take a cell or a refund; bots pass the validator; `starsCount` off gives multiplier 1.
- Balance sim rerun (bots field the same troops as before, so results should not move; report if they do).
- A four-player battle with 40 troops each run for server tick time, reported to Kort.
- In-game screenshots of the war table page for Kort.

## 7. Risks
- 40 troops per player is double today's load. If tick time is not acceptable the limit drops; that is Kort's decision with the numbers.
- The in-flight animation and bug sweep branches touch `GravesAndGlory.java` and checks; rebase before landing.

## 8. Order of work (suggested)
1. Sheet values, validator, profile schema 7 and migration, with standalone checks.
2. Battle: cell index, temporary flag, deployment, tactics lanes, refills.
3. `banner` shape, `line` removal.
4. Currencies.
5. Bots and other builders, preflight rules.
6. Lobby screen.
7. Remaining tests, docs, tips, balance sim, load test.

## 9. Everything affected (audit 2026-11-12, main at 3f732f0e)

Paths are under `src/main/java/gg/gravesandglory/` unless longer. Line numbers drift; treat them as pointers and re-grep.

Banner list and size
- `RosterMath.java` 52, 68, 99; `Profile.java` 19, 30-37, 76-79, 97; `ProfileMigration.java` 61-99; `LegacyDataMigration.java`
- `Campaign.java` 17, 136, 174, 247-262, 348-403, 437-445, 498-500, 530-534, 560-564, 604-613, 652, 665, 744-750, 794, 826-829, 1011-1012, 1049-1057, 1115, 1137
- `GravesAndGlory.java` 131-142, 178, 217, 226, 259, 284, 355, 850, 888, 892, 961, 1033-1048, 1070-1112, 1148-1166, 1196-1215, 1226, 1410, 1437-1438, 1464, 1476, 1484, 1502-1503
- `WarTablePages.java` 18, 43, 46, 236, 373, 602, 646, 672, 709-717, 1078; `WarTableLobby.java` 126; `WarTableRecruit.java` 232, 673; `WarTableTroop.java` 497; `WarTableReport.java` 337, 535
- `BarracksPage.java` 171, 231, 262; `RecruitmentScreen.java` 65, 71; `RecruitPages.java` 37, 43; `RecruitView.java` 53-56; `CampScreen.java` 59, 137; `CampaignClient.java` 22, 96-107, 125; `GravesAndGloryClient.java` 140; `BannerDrop.java` (whole file)
- `Veterans.java` 269, 330, 401; `Houses.java` 37, 45; `Morale.java` 34, 36; `Commander.java` 35-38; `SurvivalMode.java` 228, 282, 298; `MarchMath.java` 94-193; `Forge.java` 139-165; `MatchRules.java` 54; `Abilities.java` 199, 236, 342, 463, 1036

Slot math
- `Tactics.java` 305, 411-413, 494, 506, 639; `Audio.java` 152; `BattleMath.java` 253-254, 349, 371-390, 583-607; `ModeLines.java` 27 (confirm whether this is the same slot)

Formations
- `CommandSelection.java` 27, 70; `CommandMode.java` 21, 109-135; `BattleHud.java` 552, 558; `OrderRules.java` 58-73; `OrderOutlines`, `FormationRules`; `atlas.json` 26-27
- `tools/ui_art/manifest.py` 50, `v4a_hud.py` 481, `implement_command_ui.py` 45

Sheet and generator (never hand-edit `Spec.java`; edit `sheets/design.json`, run `./gradlew compileJava`)
- `sheets/design.json`: `rules`, `rarities`, `recruitment`, `economy`, `formations` (977), `botBanners`, `economyModel`, `ui.command.bottom`, `tips` (tip-05, tip-12), `campLessons.lesson-2`, `troopNotes.wm_oldhorn`, checks `army-capacity`, `troops-commanded`
- `tools/preflight.py` 308-332, 464, 576-590, 673-674, 929; `tools/economy_model.py`; `tools/balance_method.py` 601; `tools/balance_sim.sh`; `tools/where_extract.py`; dead scripts `tools/add_deployment.py`, `implement_groups.py`, `refine_landmarks.py` (leave or delete, do not fix)

Tests and checks
- `test-tools/java/gg/gravesandglory/`: `Phase1Checks` 805, 859, 1027, 1457, 1551, 2143; `Phase2Checks` 348-349, 1450-1473, 2621, 3231, 3272, 4047, 6278; `CampaignChecks` 89-104; `MapRouteChecks` 72, 118-156; `LobbyChecks` 65, 206, 372; `BannerFieldChecks` 235, 264-268; `HoldLineChecks` 464, 500+, 714; `LevelChecks` 414; `BotSimulation` 162; `DuelSimulation`; `CampaignDevControl` 45; `cmdui/*`; `CommandPlaytest`; `RevisionChecks`; `GeoGaps` 286; `CustomGroupsProbe` 77
- `test-tools/standalone/`: `BannerDropCheck`, `ProfileFixtureCheck`, `ProfileMigrationCheck`, `PersistedDataCorruptionCheck`, `CampaignResetCheck`, `HousesCheck`, `OrderRulesCheck`, `LobbyRulesCheck`, `MatchRulesCheck`, `test_where.py`, `test_preflight_rules.py`, `test_preflight_houses.py`, `test_preflight_campaign.py`, `test_economy_model.py`; fixtures `test-tools/resources/fixtures/profile-v1-*.json`
- `tests/test_command_ui_sheet.py`, `tests/test_ui_art.py`

Docs
- `README.md`, `docs/PLAYER-README.md`, `docs/discord/start-here.md`, `docs/releases/1.0.0-notes-DRAFT.md`, `docs/QA-MATRIX.md`, `docs/NEW-UNIT.md`, `PHASE-2-BALANCE.md`, `docs/superpowers/specs/2026-11-09-army-economy-design.md` (mark superseded)

Highest bug risks found by the audit
1. `slot >= squadSize` marks temporary troops and drives the refund path; real cells 20-53 would collide. Fixed by the `temporary` flag (4.5).
2. `validBanner` requires an exact size and `Profile.sanitize` repacks anything else, wiping old banners without the schema 7 migration (4.8).
3. Snapshot falls back to `{}` over 16000 characters (4.8).
4. `writs` name collision (4.3).
5. Preflight fixes bot banners at 20 entries and weight 20 (4.6).
