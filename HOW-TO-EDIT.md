# How to edit Graves and Glory

Guide for people and AI helpers changing this project. Project root: `C:\Users\kortt\Melty\bannerbound`. Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Yarn mappings 1.20.1+build.10, Loom 1.7.4, Java 17 target.

## 1. Rules to follow

- A release is a plain zip, a Modrinth `.mrpack` and a server kit (section 6). Publishing, uploading or submitting any of them anywhere (Modrinth, CurseForge, GitHub, a launcher or marketplace) needs Kort's go.
- Do not hand-edit `src/main/java/gg/gravesandglory/Spec.java`. It is generated from `sheets/design.json` on every build. Change the sheet, then rebuild.
- Do not launch the game, send mouse or keyboard input, or close game processes you did not start. Kort may be playing; check first.
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
| `UPDATE_PLAN.md` | Working log: status, evidence, decisions, open items |
| `build.gradle` | Build config, version, Loom run configs, harness tasks |
| `dist/` | Packaged output. Old builds are in `dist/superseded/`; `bannerbound-0.2.0.zip` there still contains harness classes; do not reuse it |

## 3. Source files

- `GravesAndGlory.java`: server entry point and battle logic. Commands, troop orders and formations on the server, barracks, respawn, packet sending, stale-soldier cleanup. It also calls the optional diagnostics hooks.
- `GravesAndGloryClient.java`: client entry point. Registers the armory resource pack (`DEFAULT_ENABLED`), the husk renderer override for dawn troops, the battle state receiver, HUD drawing (`renderHud`), the tactical map and command preview.
- `Spec.java`: GENERATED. One record and one static table per sheet. Read it, do not write it.
- `Campaign.java`: match flow for a player's campaign. Rolls, rewards, bot logic each tick, class numbers (the numbers that class bonuses use are in code, not read from the text).
- `CampaignClient.java`: client side of the campaign. Collection file location and per-frame sampling.
- `CampaignLobbyScreen.java`: the physical lobby UI: collection, rolls, army and class selection, enter-battle.
- `CampaignMaps.java`: the five battlefields and their objectives.
- `Arena.java`: builds the battlefield, structures and bridges, sets time of day per map.
- `Tactics.java`: unit abilities (shield, mend, blight), class-specific behaviour, orders.
- `CommandMode.java`: battlefield command mode: targeting, facing and the formation preview math. It duplicates the server's formation math, so change both together.
- `BattleEffects.java`: sound and particle cues in battle.
- `RecruitmentScreen.java`: the recruitment and troop ability text.
- `ArmyCards.java`: army card display.
- `DarkUi.java`: shared UI drawing helpers and colours.
- `Presentation.java`: your local visual preferences: the shader choice (through Iris) and the hit-accent strength. It has no resource-pack logic; Fabric enables the built-in armory pack itself.
- `PresentationScreen.java`: the graphics and effects settings screen.
- `Diagnostics.java`: an optional hook. It loads a `Probe` through `ServiceLoader`. The player jar has no provider, so it does nothing in play.

## 4. Changing game data (the usual edit)

1. Edit `sheets/design.json`. Each sheet is a list of rows with the same columns. Every cell must be filled.
2. Run the build. `compileJava` depends on `tools/preflight.py`, which:
   - checks for empty sheets, duplicate ids, inconsistent columns and unfilled cells;
   - checks references: troop to faction, troop to group, command selections and orders, UI formation references, and troop texture files;
   - rewrites `Spec.java` with one record and one table per sheet.
3. If preflight prints errors, it stops the build. Fix the sheet, not the output.

Sheets listed in `NOT_SHIPPED` inside `tools/preflight.py` (currently `checks`, `revisionChecks`, `playtestMilestone`) are validated but not turned into Java records. They are project process notes. Add a sheet there only if it must not ship as game data.

Some sheet values are display text only. For example, the class bonus strings in `classes` are not parsed; the numbers are hard-coded in `Campaign.java` and `Tactics.java`. When you change a class number, change the code and the text together.

### Adding a troop type (existing mob)

1. Add a row to `troops` with its id, faction, group, count, range, magic flag and texture (`gravesandglory:textures/...` must exist under `src/main/resources/assets/gravesandglory/`).
2. Add its id to the faction's `troops` list and keep the squad count matching `rules.squadSize`.
3. If it uses an ability, add the case in `Tactics.java` and the text in `RecruitmentScreen.java`.
4. The client rejects a state packet whose troop count differs from the table length, so host and players must run the same build.

Adding a new mob class is larger work: it touches the entity mapping in `GravesAndGlory.java` and `ArmyCards.java`.

## 5. Weapon and armour overlays (Entity Model Features)

Visual detail on player and troop bodies comes from Entity Model Features (EMF) overlay files in the armory pack. No Java rendering code is involved.

- Files: `src/main/resources/resourcepacks/armory/assets/minecraft/optifine/cem/`
  - `player.jem`, `player_slim.jem`: tabard, buckle, belt, shoulder pauldrons for the normal and slim arms.
  - `husk.jem`, `skeleton.jem`, `wither_skeleton.jem`: crest, tabard and the two pauldrons on the vanilla models; they reference no animation files.
- Texture: `assets/gravesandglory/textures/entity/armor_overlay.png`, 64 by 64. Regions: pauldron (0,0), crest (0,8), tabard (20,8), buckle (34,8), belt (0,24).
- Weapon items: 30 vanilla item model overrides under `assets/minecraft/models/item/`.
- Pack enabling: the armory pack is registered `DEFAULT_ENABLED` in `GravesAndGloryClient.java`, so Fabric enables it on a first launch. It is the only resource pack the modpack carries; a player who disables it in Minecraft's resource-pack screen keeps it off.

JEM format notes (EMF 3.3.12):
- Top level: `textureSize` and `models`.
- `part`: the vanilla part to attach to (`body`, `right_arm`, `left_arm`, `head`, or `root`).
- `attach: true` makes the box a child of the vanilla part and uses raw vanilla coordinates. `attach: false` replaces the part.
- `invertAxis` flips translate, rotate and box coordinates on the named axes. Do not combine it with attach children unless you have checked the flip math.
- `boxes[].coordinates` is `[x, y, z, width, height, depth]`, with `textureOffset` for box UVs or per-face `uv` values.
- `sizeAdd` grows the box so it sits over the body without z-fighting.
- `texture` forms: `"texture"` (same folder as the model), `"./x"`, `"~/x"`, `"folder/x"` (under `assets/minecraft/`), or `"mod:folder/x"` (`assets/mod/folder/x.png`).
- Arm sizes: vanilla humanoid arms are 4 wide; skeleton arms are 2 wide; slim arms are 3 wide. The arm boxes in the four JEMs follow those sizes.

The overlays apply to every husk, skeleton and wither skeleton in a Graves and Glory world, including ones that are not troops. One neutral armour set is used, so faction tints are not applied to these overlays. Changing a JEM needs a game restart or resource reload to see; in-game look has not been verified in this build.

## 6. Packaging a release

A release is a plain zip, a Modrinth `.mrpack` and a server kit. There is no launcher or marketplace recipe step. Publishing any of them anywhere needs Kort's go.

```
gradle build --offline -x test        (from a shell with Gradle 8.8 on PATH, or the full gradle.bat path)
python tools/package.py               -> dist/graves-and-glory-<version>.zip and dist/package-manifest.json
python tools/verify_package.py        -> reads back the zip
python tools/export_modrinth.py       -> dist/graves-and-glory-<version>.mrpack
python tools/verify_modrinth.py       -> reads back the .mrpack
python tools/export_server.py         -> dist/graves-and-glory-<version>-server.zip
```

- `gradle.properties` supplies the single `mod_version` value (currently `0.4.0`). Gradle expands it into `fabric.mod.json` and the archive names. `docs/RELEASE-<version>.txt` must exist for that version, or packaging fails. Packaging does not read `melty.json`.
- `generateReleaseIdentity` embeds a normalized source fingerprint in `gravesandglory-build.json`. Packaging rejects a stale JAR. The final ZIP uses a staging file so a packaging failure cannot replace an earlier archive.
- Run `python tools/verify_package.py` after packaging to read back every entry and verify its hash, the inner JAR and the third-party pins.
- Author permission evidence used by packaging is tracked in `dependencies/author-permissions.json`, rather than the ignored `evidence/` directory. The file is empty now: no dependency needs author terms.
- If old classes survive a rebuild, run a clean Gradle build. Do not reuse a prior remapped JAR.
- `tools/package.py` fails the build if any harness name appears in the player jar. It also builds `dist/package-manifest.json` with hashes, credits and every third-party notice.
- Only `dependencies/manifest.json` entries are included, and each file is checked against its pinned SHA-256.

## 7. Test harness (development only)

- Source: `test-tools/java/gg/gravesandglory/` (campaign checks, route checks, visual checks, and `MeltyTest`, the shared check-report hook).
- Built into `build/localTestTools/local-integration-DO-NOT-SHIP.jar` by the `localTestToolsJar` task.
- Used by `gradle runClient --offline -PcampaignChecks`. Never include that jar in a release.
- The Melty test recipe is retired. `MeltyTest` keeps its legacy name, but every harness check reports through `MeltyTest.report`, driven by `-Dgravesandglory.localChecks` in development runs, so it stays. Its `%LOCALAPPDATA%\Melty\test` branch is the retirable part, and renaming the class touches about a dozen harness files. The leak check in `release_config.py` still lists `MeltyTest` by name for that reason. In-game regression and launcher installation for 0.3.1 remain pending.

## 8. Known gaps and open decisions

- `dist/superseded/bannerbound-0.2.0.zip` is kept for reference only. It still contains harness classes. Do not publish it.
- Release 7c68b875 (0.2.0, in review) matches that zip. Replacing it needs Kort's explicit approval.
- Several design sheets are unused by code: `hooks`, `landmarkStyle`, `recruitmentView`, `terrainStyle`, and `CampaignRow` fields `duplicateRefund` and `starterGold`.
- No third-party resource packs ship (Kort, 2026-10-18). Excalibur's skeleton and wither-skeleton geometry was CC-BY-NC-ND, so do not copy any third-party pack's model files into Graves and Glory.
- Player 3D depth is an overlay, not a new model. Its in-game look has not been verified.
- Batches 1-4 (respawn countdown, key strip, result headline and PLAY AGAIN, Shieldwall, bot retreat and charge) compile and package. Their in-game behaviour is not yet verified. Balance of bot retreat and charge needs human playtests.
- `tools/prepare_preview_checks.py` and `tools/implement_experience.py` reference removed hooks. Do not run them.
- `CommandPlaytest` writes to `evidence/revision-02` (relative to the run folder) and creates that folder itself when it is missing.

## 9. Rename migration and standalone checks (0.3.1)

`LegacyDataMigration` copies old collection/profile JSON and presentation settings without overwriting current files. It copies an inactive `saves/Bannerbound` into `saves/Graves and Glory` only if no current world exists, excludes session locks, retains the old save, and refuses locked or ambiguous saves. The server accepts the legacy level name in the copied level data. A unique incomplete staging copy is cleaned after failure.

Never run migration experiments on a live player save. Back up after normal shutdown before deployment. The helper is exercised against temporary filesystem fixtures without launching Minecraft:

```
javac --release 17 -d build/migration-checks src/main/java/gg/gravesandglory/LegacyDataMigration.java test-tools/standalone/LegacyDataMigrationCheck.java
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

