# Section 9 audit re-run (branch army/war-table, read-only)

Partial: stopped early on the coordinator instruction. Paths are under src/main/java/gg/gravesandglory/ unless longer. Concepts not finished are marked "not checked".

## 1. Missed by section 9

| file:line | what it does | concept | risk |
|---|---|---|---|
| Progression.java:33,109-115 (consumer Campaign.java:1137) | Grant record carries gold; pay() builds "+N gold"; claims add it to Profile.gold. File is not in section 9 at all | profile gold / progression claims | 3 |
| MarchText.java:11-18 | bonus text "+5 shards, +40 gold" from bonus.gold() | profile gold (march bonus) | 2 |
| MarchMath.java:76-91 | Settled carries bonusGold; Campaign.java:610 adds it to p.gold (section 9 lists MarchMath 94-193 only) | profile gold | 2 |
| MatchRules.java:35-51 | partialGold, grossGold pay match gold into Profile.gold (section 9 lists only line 54) | profile gold | 2 |
| Campaign.java:145 | load check portable.gold > stored.gold on import | profile gold | 3 |
| Campaign.java:566,616,619,690 | report JSON "gold", "collection gold" standing text | profile gold payout display | 2 |
| Campaign.java:665 vs UiReportChecks.java:91 | report JSON already has "pacts" (earned this match) AND "writs" (held). Renaming writs to pacts collides with the existing key. WarTableReport.java:337 reads both | writs rename | 3 |
| WarTableReport.java:322 | reads report "gold" for the rewards row | profile gold display | 2 |
| UiReportChecks.java:44,47,91 | fake report with gold, writs, pacts | writs, gold | 2 |
| CampScreen.java:82,135 | header "N gold" and state hash use CampaignClient.profile.gold (section 9 lists 59,137) | profile gold | 2 |
| GravesAndGloryClient.java:353 | HUD hint purse CampaignClient.profile.gold (section 9 lists only 140) | profile gold | 2 |
| WarTableGear.java:592-593,150 | "Gold n" label from profile.gold; Forge expand price in gold. File not in section 9 | profile gold, Forge | 2 |
| WarTableRecruit.java:630 | purse pouch shows profile.gold (section 9: 232, 673) | profile gold | 2 |
| RecruitPages.java:287 | change hash p.shards + p.writs + p.renown (section 9 lists 37, 43) | writs | 2 |
| Forge.java:9; Gear.java:103; GearWire.java:19-22; GearView.java:236,267; WarTableGear.java:224,563; MatchRules.java:63; Campaign.java:611 | forgeMarks currency (separate from gold). The design never says whether it stays or becomes gearShards | forgeMarks | 2 |
| CommandPanel.java:38,411-444 | SHAPE_NAMES {"Line","Wall","Wedge","Loose"} and tray built from CommandSelection.SHAPES | line formation | 2 |
| resources/assets/gravesandglory/textures/gui/hud/g-line.png, g-line-faded.png | line glyph art (atlas.json is listed, the PNGs are not) | line formation | 1 |
| sheets/design.json:1834 | check evidence string "block, line, wedge and loose" | line | 1 |
| WarTablePages.java:556,567,573-616,765-869,967,1160,1291 | banner reads: copies/used/filled/shownLine, drag, houseSlots (1160), bannerMax (598). Section 9 lists 18,43,46,236,373,602,646,672,709-717,1078 | .banner, MARKER, EMPTY | 2 |
| RecruitmentScreen.java:56,58,60 | bannerTroops, listed, weight read CampaignClient.banner and slotWeight | .banner, slotWeight | 2 |
| CampRedesignChecks.java:34,36,39,40 (test-tools/java/gg/gravesandglory) | asserts pr.gold after roll/enlist | profile gold | 2 |
| MarchBonusChecks.java:38,43,107 | sq.banner bans and pr.gold | .banner, gold | 2 |
| RewardDriver.java:48; SliceClient.java:82; SliceProbe.java:65 | harness JSON dumps profile.gold | profile gold | 2 |
| GearChecks.java, GearScreenChecks.java | forgeMarks assertions (GearChecks also SNAPSHOT_LIMIT) | forgeMarks, SNAPSHOT_LIMIT | 1 |
| OrderIntakeProbeFormation.java:50 | issues order with formation "line" | line | 2 |
| RankNetworkChecks.java:81 | uses Phase2Checks.banner("knight*6,...") helper | banner helper | 1 |
| test-tools/standalone/BattleMathCheck.java:222,231,235 | calls BattleMath.deployRing | deployRing | 2 |
| test-tools/standalone/LevelMathCheck.java:120 | splits a botBanners row slots string and removes "^" | MARKER in botBanners | 2 |
| test-tools/standalone/GearMigrationCheck.java:49,51 | asserts p.gold == 950 and p.banner unchanged | gold, banner | 2 |
| ForgeCheck.java:24; GearCommandsCheck.java:20,64 | seed p.gold for repair tests | profile gold | 2 |
| test-tools/resources/fixtures/profile-v3-veteran.json | gold, banner, writs, writChapters. Section 9 names only profile-v1-*.json. Read by CampaignResetCheck:103 | fixtures | 3 |
| test-tools/qa/profile-v2-seed.json | same fields | fixtures | 2 |
| test-tools/qa/k7_rewards.py; server_migration_probe.py:103,146 | read profile gold from saves | profile gold | 2 |
| test-tools/java/.../SheetOverride.java:10,14 | SAFE set lets a test override botBanners | botBanners | 1 |
| sheets/design.json 2341,2354,3101-3145,4990-4992,5146-5196,10669-10670,11385-11433 | startingGold (squad), refundGold (squad), Forge repairGold, campaign completion/victory/practice gold, bonus objective gold, rollCost/chosenCost, Founders tier gold. Campaign, forge, bonusObjectives and progression rows not named in section 9 | gold | 2 |
| tools/expand_design.py | one maxQueue hit | maxQueue | 1 |

Corrections to section 9 itself:
- Unit.temporary already exists (GravesAndGlory.java:328, set at Commander.java:40, read in about 30 places in Houses, Morale, Veterans, Tactics, Abilities, Combat, Statuses). The work is to stop Commander.temporary using slot = squadSize (Commander.java:37-38), the refund path (GravesAndGlory.java:1105-1107) and SurvivalMode.java:298 (slot++ % squadSize). It is not a new field.
- ModeLines.java:27 is NOT the unit slot. It sorts modeLedger sheet rows by their own slot column. Drop it.
- Campaign.progression() and 13 calls in WarTablePages are different from RosterMath.starPct (star bonus). Only the first is in scope for 4.7.

## 2. Per concept (src files unless noted)

- squadSize: BarracksPage, Campaign, Commander, GravesAndGlory(7), Morale, Profile, RecruitmentScreen(2), RosterMath(5), SurvivalMode, Spec (generated); sheets/design.json:1266; tools preflight.py, where_extract.py, add_deployment.py; tests Phase1Checks, Phase2Checks(3), test_where.py. All in section 9.
- bannerSize: RosterMath(4); Phase2Checks, LobbyChecks(2). Listed.
- validBanner: Profile(2), ProfileMigration, RosterMath; CampaignChecks, BannerFieldChecks, Phase2Checks, HousesCheck(6), BannerDropCheck, ProfileMigrationCheck; tools/preflight.py. Listed.
- repack: BannerDrop(4), Campaign(2), ProfileMigration(2), Profile(2), RosterMath, WarTablePages; BannerDropCheck, ProfileMigrationCheck, HousesCheck, BannerFieldChecks, Phase2Checks. Listed. Docs: NEW-UNIT, QA-MATRIX(3), docs/graph-labels.json (generated, not in section 9).
- starterBanner: CampaignClient, Campaign(2), Profile, ProfileMigration(2), RosterMath; CustomGroupsProbe, LobbyChecks, Phase2Checks, HousesCheck, ProfileMigrationCheck. Listed.
- MARKER "^" / EMPTY "empty": RosterMath (many), BannerDrop, WarTablePages (own EMPTY at line 50), Morale:34, Campaign:256,388, ProfileMigration:98-105; sheet botBanners slots strings use "^" (10744-10830); LevelMathCheck:120 (MISS), LobbyRulesCheck, BannerFieldChecks, BannerDropCheck. Other MARKER hits (Modes, BuildMode, CampWorld, CommandPanel OWN_MARKER, Phase3Checks) are unrelated.
- houseSlots: Campaign:612, Houses:45, RosterMath(2), WarTablePages:1160 (MISS line); HousesCheck, Phase2Checks.
- slotWeight: RosterMath(4), Campaign(2), BarracksPage(2), Houses, GravesAndGlory(3), RecruitmentScreen(3), WarTablePages; sheet (34); tools/preflight.py(3); Phase2Checks(5), HousesCheck.
- bannerMax: RosterMath(7), BannerDrop:97, WarTablePages:598; sheet rarities(4); preflight.py(3).
- .banner field access: Campaign (many), GravesAndGlory (888,892,1070,1105,1163-64,1200,1437-38,1464,1476,1502-03), WarTablePages (many), RecruitmentScreen:56,58, CampScreen:59, CommandSelection:70, Veterans:269, Houses:45, Profile:31, ProfileMigration:76. MarchMath:183 c.banner() is a chapter house flag, unrelated.
- "slot:" command: Campaign:253-262 (server), BannerDrop:6,27,29 (client). Two sites only.
- .slot on units: Audio:152, Tactics:305,411,413,494,506,536,633-639, GravesAndGlory:1039,1104-1107,1197,1213,1410,1438,1484,1503, Commander:36-38. All listed.
- temporary: field exists, see corrections.
- deployRing: BattleMath:583, GravesAndGlory:1166, BattleMathCheck (MISS).
- % 5 and / 5 banner geometry: BattleMath:581-593, GravesAndGlory:1204-1214,1410,1438,1484,1503, Tactics:494,506. Arena:292, CampaignMaps:95, ConvoyMode, SurvivalMode timers are unrelated.
- "line" formation: BattleMath:254 (fallback), BattleHud:552,558, CommandSelection:27, CommandPanel:38 (MISS); sheet 977, 1834 (MISS), 2379; atlas.json 26-27 and the PNGs (MISS); tools ui_art manifest.py:50, v4a_hud.py:481, implement_command_ui.py:45; tests OrderRulesCheck:19-21, cmdui/*, CommandPlaytest, CampaignChecks:104, CustomGroupsProbe:77, GeoGaps:286, OrderIntakeProbeFormation:50 (MISS). Lang files: no hit.
- writs: Profile:37,82; Campaign:530,534,652,665; RecruitPages:43,287; WarTableReport:337,535-536; Phase2Checks, CampaignResetCheck, ProfileMigrationCheck, UiReportChecks (MISS); fixtures profile-v3-veteran.json, qa/profile-v2-seed.json (MISS).
- writChapters: Profile(3), ProfileMigration(3), Campaign(3), MarchMath:128 (comment); Phase2Checks, CampaignResetCheck, ProfileMigrationCheck.
- profile gold: see section 3.
- forgeMarks: Profile, Campaign, Forge(7), Gear, GearView, GearWire, WarTableGear, MatchRules, ProfileMigration, sheet campaign.forgeMarks:4999; many Gear and Forge checks. Relation to gearShards undecided in the design.
- botBanners: Campaign(4), MarchMath(6), sheet:10733, tools preflight.py(14), balance_method.py, test_preflight_campaign.py(10), BotSimulation, DuelSimulation, Phase2Checks, SheetOverride (MISS), MarchMathCheck.
- SNAPSHOT_LIMIT: Campaign:16-17,1053-1057, GravesAndGloryClient:140, Phase1Checks(7), GearChecks(2, MISS).
- maxQueue: GravesAndGlory:1080,1096, sheet:2346, tools/expand_design.py (MISS), Phase2Checks(3), MotionChecks (unrelated local variable).
- golden: only golden-angle uses (Tactics:413, BattleEffects:26) plus unrelated golden tests.
- progression()/starPct: Campaign(3 progression()), WarTablePages(13), RosterMath starPct, sheet starPct keys, WarTableTroop:497.
- % 10 / % 2 uses: Tactics:305,411,639, Audio:152 (use Unit.slot), keep working on a cell index.
- docs (README.md, docs/*.md): not checked beyond count greps (hits in QA-MATRIX, NEW-UNIT, PHASE-2-BALANCE).
- tools/*.py: preflight.py, economy_model.py, balance_method.py, where_extract.py, expand_design.py seen. balance_gate.py, progression_gate.py, contact_sheet.py, state.py matched a loose pattern: not checked.
- sheets/preflight.json, sheets/verification.json: not checked.
- src/main/resources lang and other json: only the unrelated key.gravesandglory.banner seen; rest not checked.
- tests/ directory (test_command_ui_sheet.py, test_ui_art.py): not checked.

## 3. Profile gold vs squad gold in src

Squad gold (in-match purse, unchanged by the design):
- GravesAndGlory.java:226 Squad.gold = startingGold, :230 earn(), :850-1107 training and refund (s.gold), :1466-1469 test.
- Campaign.java:750 bot buys (s.gold); Veterans.java:322,330,370-371,412 gear buys (s.gold); SurvivalMode:228; ConvoyMode:278; MatchStats goldEarned; BattleMode:76; BattleMath:755.
- Client: GravesAndGloryClient.java:37 static int gold is the squad purse mirror. Used by BattleHud:372-386, ArmouryTab:276,368, RecruitmentScreen:65,72, BarracksPage:226,263.
- LevelMath gold parameters (buyState, refusal, botPicks) take a squad purse value.

Saved Profile.gold (to remove):
- Profile.java:27 field, :79 clamp. ProfileMigration.java:19,61,73 (v1 import, refund).
- Campaign.java: 145 (import compare), 438,445 (roll), 499-500 (chosen buy), 564 (partial pay), 607-610 (finish earned + bonus), 1011 (tutorial grant), 1137 (progression claim).
- Forge.java:139-165 (repair, repair all, add row).
- Client mirror reads: CampScreen:82,135; GravesAndGloryClient:353; RecruitPages:37-38; WarTableRecruit:232,630,673; WarTableGear:592.
- Paid-amount display (report JSON "gold", not a purse): Campaign:616, WarTableReport:322, UiReportChecks.
- Sheet gold feeding Profile.gold: campaign completionGold/victoryGold/practiceGold, recruitment rollCost/chosenCost, forge repairGold, bonusObjectives gold, progression tier gold.
- Tests touching Profile.gold: Phase2Checks:5633+, CampRedesignChecks, MarchBonusChecks, ForgeCheck, GearCommandsCheck, GearMigrationCheck, ProfileMigrationCheck(80,90), RewardDriver, SliceClient, SliceProbe, k7_rewards.py, server_migration_probe.py.
