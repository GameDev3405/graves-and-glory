# How to edit Graves and Glory

Guide for people and AI helpers changing this project. Project root: `C:\Users\kortt\Melty\bannerbound`. Minecraft 1.21.1, Fabric Loader 0.19.5, Fabric API 0.116.18+1.21.1, Yarn mappings 1.21.1+build.3, Loom 1.17.21, Java 21 target (1.21.1 port branch).

## 1. Rules to follow

- A release is a plain zip, a Modrinth `.mrpack` and a server kit (section 6). Publishing, uploading or submitting any of them anywhere (Modrinth, CurseForge, GitHub, a launcher or marketplace) needs Kort's go.
- Do not hand-edit `src/main/java/gg/gravesandglory/Spec.java`. It is generated from `sheets/design.json` on every build. Change the sheet, then rebuild.
- Harness clients may be launched (Kort lifted the no-Minecraft rule on 2026-10-23), under the client rules in `docs/HANDOFF.md`: at most four at once across all worktrees, one per agent, 30 minutes each, never compile while your own client runs, and if a java process runs with a `--gameDir` outside `C:\Users\kortt\Melty\*\work\`, Kort is playing: wait. Never send input to, or close, a process you did not start.
- Do not run `tools/prepare_preview_checks.py` or `tools/revise_command_design.py` for a release. They rewrite `design.json` and, for the first one, harness sources under `src`.
- The player jar must contain no test harness. The harness lives only in `test-tools/`.
- The modpack ships no third-party resource packs (Kort, 2026-10-18): do not add one to `dependencies/`, the exports or the code. Its look is the built-in armory pack and the mod's own assets. History: Excalibur, Clarent, the Excalibur Fresh Animations patch, Fresh Animations and Faithful 32x were bundled until that date.

## 2. Where things are

| Path | What it is |
| --- | --- |
| `src/main/java/gg/gravesandglory/` | All game code (see section 3) |
| `src/main/java/gg/gravesandglory/mixin/` | Mixins into vanilla classes; registered in `src/main/resources/gravesandglory.mixins.json` |
| `gradle.properties` | Authoritative mod_version; Gradle expands fabric.mod.json and embeds the source fingerprint |
| `src/main/resources/fabric.mod.json` | Mod metadata template and entrypoints |
| `src/main/resources/assets/gravesandglory/` | Mod textures, sounds (`sounds.json`), language file (`lang/en_us.json`) |
| `src/main/resources/resourcepacks/armory/` | Built-in resource pack for weapons and armour overlays (section 5) |
| `sheets/design.json` | Game data tables: troops, factions, orders, formations, points, structures, rules, maps, classes and more |
| `sheets/preflight.json` | Report written by the preflight script on every build |
| `sheets/verification.json` | Verification status notes. Not read by the generator |
| `tools/preflight.py` | Validates `design.json` and regenerates `Spec.java` |
| `tools/package.py` | Builds the player zip in `dist/` and checks the jar for harness leaks |
| `tools/verify_package.py`, `tools/export_modrinth.py`, `tools/verify_modrinth.py`, `tools/export_server.py` | Read back the zip; build and read back the `.mrpack`; build the server kit (section 6) |
| `dependencies/` | Pinned third-party mods and shader packs (`manifest.json`), notices and source archives |
| `test-tools/java/gg/gravesandglory/` | Development test harness. Not in the player jar |
| `melty.json` | Legacy Melty publish metadata. Kept for now but unused: packaging and verification never read it, so it can go stale or be deleted |
| `README.md` | Player-facing description. Keep it accurate |
| `docs/archive/UPDATE_PLAN.md` | Archived working log up to 0.3.7; current state is `docs/HANDOFF.md` |
| `build.gradle` | Build config, version, Loom run configs, harness tasks |
| `dist/` | Packaged output. Old builds are in `dist/superseded/`; `bannerbound-0.2.0.zip` there still contains harness classes; do not reuse it |

## 3. Source files

The game is `src/main/java/gg/gravesandglory/`. A number a rule needs is read from `Spec` (the generated tables) or passed in by the caller; the classes below name the sheets they read.

**Match and server**
- `GravesAndGlory.java`: server entry point and battle logic: commands, troop orders and formations, barracks, respawn, packets, stale-soldier cleanup, `Battle.spawn` (a troop is a plain vanilla mob named by its troop and stage).
- `Campaign.java`: a player's camp and match flow: profiles (`profile(UUID)` upgrades a schema 1 file in place), rolls, chosen buys, stars and shards, Pacts, rewards, the first-visit cache import, bot logic each tick. `CampaignMaps.java` and `Arena.java`: the five battlefields; `CampWorld.java`: the camp courtyard; `MapDressing.java`: dressing.
- `Combat.java`: all troop damage. One factor from the counters rows, the House traits, the auras and the commander effects, clamped to the `combatDiscipline` range. `CounterMath.java` is the pure half (the standalone `HousesCheck` compiles it).
- `Abilities.java` and `HouseAttacks.java`: troop abilities and each House's own attack, read from the `abilities` sheet by mechanic id. `Houses.java`: trait tiers in battle, the mechanics of the five Houses. `Morale.java`: group standing (nothing routs; the LAST STAND). `Projectiles.java`, `Tactics.java`, `CommandMode.java`, `BattleEffects.java`, `PackControls.java`: shots, orders, the command UI and cues.
- `Commander.java`: the six commander classes. The code knows only effect kinds, named by the `classes` sheet's `passive` and `active` columns; **no class number is hard-coded**: cooldowns, durations, radii, ranges and every value come from the class row, and its `activeText` and `passiveText` are display only.

**Data and profile (pure, no Minecraft types, compiled by the standalone checks)**
- `Profile.java`: a commander's saved collection, schema 2 (banner, owned troops with stars, stage and mastery, shards, Renown, Epic pity). `ProfileMigration.java`: the one-time v1 upgrade (parse and write functions are passed in). `RosterMath.java`: banners, trait tiers, rolls and pity, stars, the progression cap. `MatchStats.java`, `BattleMath.java`, `Progression.java`, `SocialHub.java`.
- `Spec.java`: GENERATED, never hand-edited.

**Camp screens**
- `CampScreen.java`: the camp pages. `RecruitPages.java`: the recruit cards' actions, the roll result card and the troop page. `CampaignClient.java`: the client's copy of the snapshot and the roll reveal. `ArmyCards.java`, `DarkUi.java`, `RecruitmentScreen.java`, `CampaignLobbyScreen.java`, `PresentationScreen.java`, `FrontMenuScreen.java`.

**Troop models (client)**
- `TroopModelLoader.java` loads `models/troop/` on every resource reload into `TroopModels.java`; `TroopModel.java` shows and hides a troop's parts by stage, fold and helm; `TroopRenderer.java` and `TroopRenderers.java` draw it (never vanilla armour, held items or the elytra); `TroopFeatures.java` draws its layers (outer, veteran trim, team cloth, glow, weapon, shield); `TroopMesh.java` and `TroopDepth.java` do the relief; `TroopAnimator.java` poses a troop from `TroopClips.java` (`BipedClips`, `ColossusClips`, `HoundClips`, `SpiderClips`: 18 clips a rig) with `TroopTells.java` and the House attack clips.
- `Diagnostics.java`: an optional hook loaded through `ServiceLoader`; the player jar has no provider, so it does nothing in play.
- `Presentation.java`: your local visual preferences: the shader choice (through Iris) and the hit-accent strength. It has no resource-pack logic; Fabric enables the built-in armory pack itself.

## 4. Changing game data (the usual edit)

1. Edit `sheets/design.json`. Each sheet is a list of rows with the same columns. Every cell must be filled.
2. Run the build. `compileJava` depends on `tools/preflight.py`, which:
   - checks for empty sheets, duplicate ids, inconsistent columns and unfilled cells;
   - checks references: troop to faction, troop to group, command selections and orders, UI formation references, and troop texture files;
   - rewrites `Spec.java` with one record and one table per sheet.
3. If preflight prints errors, it stops the build. Fix the sheet, not the output.

Sheets listed in `NOT_SHIPPED` inside `tools/preflight.py` (currently `checks`, `revisionChecks`, `playtestMilestone`) are validated but not turned into Java records. They are project process notes. Add a sheet there only if it must not ship as game data.

Some sheet values are display text only (a class's `activeText` and `passiveText`, an ability's `text`, a row's `text`). The numbers are not: no class number, ability number or counter multiplier is hard-coded, so change the sheet and the card text together, and the code follows. A new number the code needs gets a column, not a literal. Pure rules (`RosterMath`, `CounterMath`, `ProfileMigration`) take their numbers from `Spec` or from their caller, which is what lets the standalone checks pin them without Minecraft.

The Phase 2 sheets: `houses` (five, with `fieldable`, all true), `troops` (30), `troopNotes` (card text), `evolutions`, `mastery`, `renown`, `rarities`, `recruitment`, `economy` and `economyModel` (the numbers `tools/economy_model.py` simulates), `abilities`, `counters`, `houseTraits`, `classes`, `combatDiscipline`, `botBanners` and `campaignBattles`. `tools/preflight.py` checks them, and a House must hold exactly its composition (every House has the same rarity shape, since all five are fieldable).

The animation sheets (2026-10-29 animation rework): every `troops` row names its weapon `family`, and `attackVariants` lists each family's attacks (`id`, `family`, `zone`, `kind`, `impactTicks`, `weight`) in the order the engine cycles them. Preflight checks that every family is known and used, that each troop's family has rows, and that each `impactTicks` lands below the cooldown of every troop in the family. A variant's `kind` is the animation's hit kind (`stab`, `slash`, `blunt`, `arrow`, `bolt`, `magic`), not the combat kind: here `bolt` is a crossbow bolt, while in `CounterMath.kindTags` and `attackKind` (and so in counter rules and Combat) `bolt` is a caster's magic bolt and a crossbow counts as `arrow`.

### Adding a troop

1. Add a row to `troops` (id, House, rarity, entity, stats, ability, tags) and its id to its House's `troops` list in sheet order; add its `troopNotes` row and its `evolutions` rows. Preflight names every missing cell and reference.
2. A troop is drawn from its custom name, so its level names must be unique across all troops (preflight checks it).
3. Give it art (section 5): `art/troops/<id>.json` and its skins. Without art the troop draws as its bare rig.
4. If it uses a new ability mechanic, add the mechanic in `Abilities.java` or `HouseAttacks.java` and the row in `abilities`; the numbers stay in the row.
5. The client rejects a state packet whose troop count differs from the table length, so host and players must run the same build.

## 5. Troop models, skins and the armory overlay

**Troops draw from their own models.** `tools/build_skins.py` turns the sources in `art/` (rigs, parts, weapons, troops, team cloth) into `src/main/resources/assets/gravesandglory/models/troop/` (`index.json`, one file per troop, rig and weapon) and `textures/entity/troop/` (one skin per armour tier with its relief layer, the Veteran trim, glow and outer layers, weapon sheets, the rank chevron, and one team cloth per side). The client picks a troop's model from its custom name (a troop or level name, with an optional ` (Veteran)` suffix), its armour skin and weapon from its rank_v1 rank (`RankSync`: level, weapon tier, armour tier), and its cloth from its `gg_<side>` team, so a side colour is data (the `factions` sheet), never a texture edit. Never hand-edit a generated file.

- `python tools/build_skins.py` exports; `--verify` rebuilds into a temp dir and compares (it must pass before a commit that touches `art/` or the generated models); `--template <troop>` writes the sheet to paint over.
- `tools/preflight.py` checks that `models/troop/index.json` lists exactly the `art/troops` ids.
- Where each part of the art pipeline lives, its sizes and the rules (relief, two texels a model pixel on weapons, no vanilla armour on a troop) is in `docs/superpowers/specs/2026-10-17-unit-identity.md` (and `docs/ART-STYLE-GUIDE.md` on the base branch); this file only says how to rebuild.
- Phase 2 painted skins for the six Gilded troops; the other 24 draw as their bare rigs until each House's art half. That is why every troop must work without art.
- The armory pack is registered `DEFAULT_ENABLED` in `GravesAndGloryClient.java`, so Fabric enables it on a first launch. It is the only resource pack the modpack carries (all third-party packs were dropped, Kort, 2026-10-18); a player who disables it in Minecraft's resource-pack screen keeps it off.

**Tier art (in-match progression design 4.4).** A troop's armour and weapon each have tiers I to III (the `gearTiers` sheet), set apart from its level.
- Armour: `art/troops/<id>_s1.png` to `_s3.png`, one skin per armour tier. A skin for a tier no gear tier has fails the build.
- Weapon: `art/weapons/<weapon>.json` is tier I; `<weapon>_t2.json` and `<weapon>_t3.json` (with their sheets) are tiers II and III. A troop always names its tier I weapon; a `_t<n>` weapon without its tier I base fails the build.
- Weapon parts (a troop without a held weapon, such as a horn or a banner): a part gets a `"weaponTiers": [..]` key, the weapon tiers it shows at, like `stages` (the armour tiers) for armour. A weapon tier of parts counts as drawn only when it is whole: every bone that carries a tier I weapon part carries a part of that tier too, else the troop draws tier I there and stays on the stopgap list.
- A missing tier draws as the highest tier below it that exists (the stopgap). `python tools/build_skins.py --tiers` lists every troop's tiers and the stopgaps.
- The rank chevron is `art/marks/chevron.png` (17x7, grey, full alpha or none); the client tints it by the side colour.

**Armory overlay (players and vanilla mobs only).** The armory resource pack under `src/main/resources/resourcepacks/armory/` still carries Entity Model Features overlays (`player.jem`, `player_slim.jem`, and `husk`, `skeleton` and `wither_skeleton` `.jem` files) and 30 vanilla weapon item overrides. Troops no longer use them. EMF, ETF and Fresh Animations and the husk, skeleton and wither skeleton `.jem` files leave in the release step (Needs Kort 13); until then they stay pinned, for the presentation modpack only. Changing a JEM needs a game restart or resource reload to see.

## 6. Packaging a release

A release is a plain zip, a Modrinth `.mrpack` and a server kit. There is no launcher or marketplace recipe step. Publishing any of them anywhere needs Kort's go.

```
gradle build --offline -x test        (from a shell with Gradle 8.8 on PATH, or the full gradle.bat path)
python tools/package.py               -> dist/graves-and-glory-<version>.zip and dist/package-manifest.json
python tools/verify_package.py        -> reads back the zip
python tools/export_modrinth.py       -> dist/graves-and-glory-<version>.mrpack
python tools/verify_modrinth.py       -> reads back the .mrpack
python tools/export_server.py         -> dist/graves-and-glory-<version>-server.zip (tools/server-kit/ at the root, plus mods/ and docs/)
```

- `gradle.properties` supplies the single `mod_version` value (currently `0.5.1`). Gradle expands it into `fabric.mod.json` and the archive names. `docs/RELEASE-<version>.txt` must exist for that version, or packaging fails. Packaging does not read `melty.json`.
- `generateReleaseIdentity` embeds a normalized source fingerprint in `gravesandglory-build.json`. Packaging rejects a stale JAR. The final ZIP uses a staging file so a packaging failure cannot replace an earlier archive.
- Run `python tools/verify_package.py` after packaging to read back every entry and verify its hash, the inner JAR and the third-party pins.
- Author permission evidence used by packaging is tracked in `dependencies/author-permissions.json`, rather than the ignored `evidence/` directory. The file is empty now: no dependency needs author terms.
- If old classes survive a rebuild, run a clean Gradle build. Do not reuse a prior remapped JAR.
- `tools/package.py` fails the build if any harness name appears in the player jar. It also builds `dist/package-manifest.json` with hashes, credits and every third-party notice.
- Only `dependencies/manifest.json` entries are included, and each file is checked against its pinned SHA-256.
- `tools/package.py` refuses while any troop draws a stopgap tier (`python tools/build_skins.py --tiers` lists them). `--allow-stopgap` builds anyway for local use and names the zip `graves-and-glory-<version>-stopgap.zip`; never publish that file. `tools/export_modrinth.py` and `tools/export_server.py` run the same check: with `--allow-stopgap` they take only the `-stopgap` player archive and write `-stopgap.mrpack` and `-stopgap-server.zip`; without it they refuse, and they refuse a package manifest whose archive does not carry their own mark.

## 7. Test harness (development only)

- Source: `test-tools/java/gg/gravesandglory/` (campaign checks, route checks, visual checks, and `MeltyTest`, the shared check-report hook).
- Built into `build/localTestTools/local-integration-DO-NOT-SHIP.jar` by the `localTestToolsJar` task.
- Used by `gradle runClient --offline -PcampaignChecks`. Never include that jar in a release.
- Phase 2 suites run with `GRAVES_PHASE2_CHECKS=<suite>[,...]` (`banners`, `engine`, `commander`, `gilded`, `spike`, `gilded-art`, `graveborn`, `ironclad`, `tidecallers`, `wildmoot`, `chase`, `anim`, `turn`, `tells`, `depth`, `screens`, `migration`) and Phase 1 suites with `GRAVES_PHASE1_CHECKS=<suite>[,...]`, through `test-tools/isolated-checks.gradle`. Suite `migration` is meant to be run twice on one run root (the second launch checks the files the first left). A launch has a 30 minute cap, so split a long list. Bot simulations are headless: `gradle runServer --offline -PbotSim=10 ... --init-script test-tools/isolated-server.gradle` (see `docs/PHASE-2-BALANCE.md`).
- Command UI suites (`test-tools/java/gg/gravesandglory/cmdui/`, `<Name>Suite`, loaded by `CommandUiChecks`) run with `GRAVES_COMMAND_UI_CHECKS=<Suite>[,...]` (or `-Dgravesandglory.commandUiChecks`): `Smoke`, `OrderIntake`, `Keys`, `OrderBehaviour`, `FormationRules`, `Panel` (also writes the panel screenshots to `work/cmdui-shots/`), `Feedback`, `ChargeCamera`, `BotOrders`, `CustomGroups` and `EndToEnd` (one battle with the real keys, then a disconnect and a clean rejoin). Run them from Git Bash with `GRAVES_ROADMAP_TEST_ROOT=<absolute root under work/> GRAVES_COMMAND_UI_CHECKS=... GRAVES_ROADMAP_USER=CmdUi ./gradlew.bat runClient -PlocalIntegration -I test-tools/isolated-checks.gradle`; results are `<root>/run/command-ui/results.txt` (`PASS|FAIL <check> <detail>`, each suite ends with a `cu-<name>-suite` line). The panel's code is `CommandPanel.java` (drawing, hit rects), `CommandSelection.java` (selection, tray, order keys), `CommandMode.java` (aim and send) and `ChargeCamera.java`; `UiMath.java` holds the HUD layout the panel shares with `BattleHud`.
- The Melty test recipe is retired. `MeltyTest` keeps its legacy name, but every harness check reports through `MeltyTest.report`, driven by `-Dgravesandglory.localChecks` in development runs, so it stays. Its `%LOCALAPPDATA%\Melty\test` branch is the retirable part, and renaming the class touches about a dozen harness files. The leak check in `release_config.py` still lists `MeltyTest` by name for that reason. In-game regression and launcher installation for 0.3.1 remain pending.

## 8. Known gaps and open decisions

- `dist/superseded/bannerbound-0.2.0.zip` is kept for reference only. It still contains harness classes. Do not publish it.
- Release 7c68b875 (0.2.0, in review) matches that zip. Replacing it needs Kort's explicit approval.
- Several design sheets are unused by code: `hooks`, `landmarkStyle`, `recruitmentView`, `terrainStyle`, and `CampaignRow` fields `duplicateRefund` and `starterGold`.
- No third-party resource packs ship (Kort, 2026-10-18). Excalibur's skeleton and wither-skeleton geometry was CC-BY-NC-ND, so do not copy any third-party pack's model files into Graves and Glory.
- Player 3D depth is an overlay, not a new model. Its in-game look has not been verified.
- Batches 1-4 (respawn countdown, key strip, result headline and PLAY AGAIN, bot retreat and charge) compile and package. Their in-game behaviour is not yet verified. Balance of bot retreat and charge needs human playtests.
- `botCommand` columns `archerThreatCount` and `archerThreatRange` are unused by code since the Shieldwall formation was removed (2026-10-29). They are parked, not removed, because another branch edits the same rows; see PARKED.md.
- `tools/prepare_preview_checks.py` and `tools/implement_experience.py` reference removed hooks. Do not run them.
- `CommandPlaytest` writes to `evidence/revision-02` (relative to the run folder) and creates that folder itself when it is missing.

## 9. Rename migration and standalone checks (0.3.1)

`LegacyDataMigration` copies old collection/profile JSON and presentation settings without overwriting current files. It copies an inactive `saves/Bannerbound` into `saves/Graves and Glory` only if no current world exists, excludes session locks, retains the old save, and refuses locked or ambiguous saves. The server accepts the legacy level name in the copied level data. A unique incomplete staging copy is cleaned after failure.

Never run migration experiments on a live player save. Back up after normal shutdown before deployment. The helper is exercised against temporary filesystem fixtures without launching Minecraft:

```
javac --release 21 -d build/migration-checks src/main/java/gg/gravesandglory/LegacyDataMigration.java test-tools/standalone/LegacyDataMigrationCheck.java
java -cp build/migration-checks gg.gravesandglory.LegacyDataMigrationCheck
python test-tools/standalone/test_release_integrity.py
```

These standalone checks stay under `test-tools/standalone`, outside the player and development Fabric source sets. They do not replace a real installation, save-upgrade, restart or multiplayer test.

For isolated game regressions, `test-tools/isolated-checks.gradle` requires
GRAVES_ROADMAP_TEST_ROOT and routes the run directory, runtime and check output
there. Use it with `-PcampaignChecks --init-script test-tools/isolated-checks.gradle`
and matching `--gameDir` arguments. Optional variables: GRAVES_ROADMAP_MIGRATION
(true for controlled legacy fixtures), GRAVES_ROADMAP_STARTUP_ONLY (true for a
short startup check). These are
development controls, never production settings. Obtain launch authorization
and prepare only disposable data before using them.

## 10. Normal launcher menu and isolated tests (0.3.3)

FrontMenuScreen is the normal entry point. Never restore automatic title-screen world creation. Melty properties are optional and invitations require an explicit action. Practice must not open a LAN listener; friends hosting may do so. Public battles remain disabled until a real service is configured.

The isolated Gradle init scripts set Loom's runDir, because Loom overwrites JavaExec workingDir inside exec(). Always use a disposable run folder and explicit gameDir for client tests. Existing player windows/processes must remain untouched. FrontMenuChecks lives only in test-tools and drives real menu buttons for validation.

