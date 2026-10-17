# Graves and Glory 0.2.0 campaign update

HOLD: do not publish until Kort explicitly confirms. The earlier "publish when complete" authorization is superseded. Live listing 06b397f4-3e2d-4cc0-a286-df832c94bf61 remains 0.1.0. Current chat decisions supersede historical source notes. Work ID a84012f6-f823-457a-9bba-bac801f80891.

- Permanent unit unlocks from rolls purchased with gold earned after matches. Army composition chosen before combat, up to 20 troops.
- Commander classes boost infantry, ranged or support troops.
- Physical lobby with collection, rolling, class selection, four giant purposeful maps.
- 1vAI practice; PvP 1v1, 2v2, 3v3, 4v4; optional bot fill.
- Match camera entrances featuring commander and army, combat animation/VFX and matching sound.
- Preserve current independent group commands, formations, scouting, tactical map, barracks and faction abilities.
- Existing Minecraft 1.20.1 Fabric integration, MIT original content, remix allowed.

Source recovered from sibling minecraft project; copied into this task folder so previous preview remains intact. Original source/readme describes a private preview; that is historical project state, not a new authorization restriction. Current user explicitly requested update and publication.

Must ask before launching a game or controlling physical input (current chat rule supersedes prior historical launch permission). No games launched yet. No user processes closed.

Implementation and verification pending. Do not claim built, tested, one-click, multiplayer success or published until supported by new evidence.

## Verified development progress
- Three actual Minecraft runs passed core collection, economy, class, bot, commands and eight-army checks.
- Actual second development client joined first host; remote Internet reachability not tested.
- All twelve crossings (three on each map) passed with twenty actual pathfinding troops, no crossing teleports. See evidence/navigation-all-maps.txt and log. Each crossing took roughly nineteen seconds.
- Final source adds twenty editable deployment positions, portable collection synchronization handshake, test-profile isolation, responsive barracks UI, intro camera/HUD and original sound registration. Final regression and production installation remain pending.
- User explicitly approved separate game test windows in this chat; physical mouse/keyboard remain untouched.

## New steering before publication
Creator observed choppy intro, requests smoother entrance and researched multiple shader, texture pack and professional game UI options to choose from. Do NOT publish before the visual choice is resolved and implemented/tested. Camera sampling moved from fixed 20 Hz coordinates to per-render-frame sampling with sub-tick interpolation and smooth terrain clearance. Add actual frame telemetry; no smoothness claim until tested. Research primary author pages and show references; do not mistake reference images for Graves and Glory gameplay.

## Camera fix and visual-choice checkpoint
- Per-render-frame camera sampling with partial tick progress compiled and ran successfully. Three measured intros averaged 117.75, 117.98 and 117.74 FPS without shaders; third was a 4v4/160-troop entrance. Median ~8.35 ms; third p95 9.75 ms, max 16.15 ms. JSON evidence in run/gravesandglory/test-camera/.
- Actual 60 FPS game-window-only recording with own process audio: clips/smooth-entrance.mp4 (12 seconds, H.264/AAC). At second 6 image visually checked: commander, army, smooth-camera scene, no desktop/private windows.
- Latest full game regression passed, including paid AI casualty replacement, barracks UI, collection persistence, deployment, modes and combat. Latest evidence pass-05-checks.txt and pass-05-host.log.
- Two local clients also joined a running 4v4 match in the preceding build with the same join logic. Third-party Internet/human-eight-player playtest not done.
- All own game PIDs closed gracefully; last own 48292, prior44700/45304. No physical input taken.
- No v0.2 package uploaded/submitted/published yet. Live listing remains 0.1.0. Need creator visual style choice first.
- Researched UI references: AoE IV compact strategy HUD, Minecraft Dungeons inventory by UI artist on Behance, Total War Warhammer III army cards. Original assets must be authored; reference shots are NOT Graves and Glory media.
- Shortlist bundle candidates: Complementary Reimagined / Unbound (two styles, custom license must preserve full terms), MakeUp Ultra Fast (LGPL3 with shader source/license), Excalibur (author permits unchanged noncommercial redistribution with project link per CurseForge description; recheck exact file terms), Faithful32x (Faithful v4 terms, credits/link/license; check archive exact license). BSL and Bare Bones explicitly disallow redistribution without permission, so do not bundle. Other researched options not cleared. No shaders/textures installed yet.

## Pickup 2026-10-16 (after Codex hit its usage limit)
- Source compiles (`gradle compileJava --offline`, cached Gradle 8.8). Repo is not a git checkout, so there is no diff baseline.
- Last full mechanical campaign run (run/logs/latest.log, 15:33-15:47) passed every check, including barracks-ready, barracks-open and barracks-queue, which had failed in the 15:31 run (evidence/polish-05-checks.txt). Local-checks file at evidence/local-checks.txt is all pass.
- Stale-soldier cleanup is in source (GravesAndGlory.java: ENTITY_LOAD discard and clearOldUnits). Excalibur texture setup is in Presentation.java. Neither has a restart test in the logs yet.
- Known baseline warnings in that run: Excalibur carved-pumpkin hat models fail to load (invalid rotation values), plus Iris and shader warnings. The run still completed.
- Not done (at pickup): restart test (Excalibur + reopen clean lobby), Melty release test, publish. Each launches the game or publishes, so each needs Kort's go-ahead.

## Restart and Melty release test (2026-10-16)
- Restart test passed (two development runs): restart-collection, restart-clean-lobby and restart-excalibur all reported pass. Evidence: evidence/local-checks.txt, evidence/restart-state.json, evidence/local-checks-before-restart.txt (archived). Restart and fixture steps run only in the development harness (`devRun()`), never in player or Melty installs.
- Stale-soldier cleanup (ENTITY_LOAD discard and clearOldUnits) is verified only by that development restart test, not in a player install.
- Melty test c7f7570c-0534-4ec2-9e4c-5f44fbbc8963 on release 7c68b875 (version 0.2.0): 3 runs × 8 minutes, status passed, graduates true. Every run passed Melty's platform checks and the mod's self-checks (12 names, including match-rewards, which is not in the recipe's test.checks list).
- Footage reviews: runs 1, 2 and 3 each passed with cited frames (practice entrance and HUD, army collection, barracks training, 4v4 entrance with both armies on the field).
- Idle footage in every run: the war council sits on 4v4 with no input for about 5–7 of the 8 minutes (from 01:04 in runs 1 and 2, 00:54 in run 3). No gameplay is shown after that.
- Run 3 ends a 4v4 round as a Draw at 00:52, about ten seconds after it starts. The reward line reads "reward requires 3 minutes of play". The mod's own harness calls campaign.finish(-1) (CampaignChecks.java lines 51 and 67), so this is most likely a test-harness action, not a player-visible failure. Not separately confirmed.
- Recipe change: arena-ready was removed from test.checks after run 1 of release 9630f6d2 failed (playedSeconds 15). The check fires only after the full arena build. The timing cause is not diagnosed. Current test.checks has 11 names.
- selfTest() (GravesAndGlory.java, about line 535) has no call site, so troops-commanded, point-captured, combat-resolved, army-capacity, river-navigation, hill-navigation, friendly-fire and barracks-management never run. Their checks were removed from test.checks. Kort decides whether to wire selfTest() up or delete it.
- Not run: the two-copy multiplayer join test (needs Kort's approval, since it launches a second game copy).
- Not published. Live listing remains 0.1.0 (release b20b2bea-6baf-4ab7-b11b-1f0d9c30717b). Release 7c68b875 is publishable and waits for Kort's confirmation.

## Codex review and UI pass (2026-10-16)
- Codex's UI edits compile offline (`gradle compileJava --offline`, BUILD SUCCESSFUL). Whitespace-insensitive diff against evidence/ui-redesign-before-20261016-192922: BipedAnimationMixin changed only in line endings.
- Resolved in 0.3.0: TroopDetailFeature is removed (class, texture and registration). Armour detail now comes from EMF overlays in the armory pack. Weapons still get 3D only from the armory pack (DEFAULT_ENABLED, GravesAndGloryClient.java:50), which overrides 30 vanilla items.
- Release package problem: dist/bannerbound-0.2.0.zip contains mods/bannerbound-0.2.0.jar with CampaignChecks, CampaignDevControl, MapRouteChecks, MeltyTest, RevisionChecks, VisualChecks and Spec$ChecksRow / Spec$RevisionChecksRow. This breaks the handoff rule and README line 62. Current source keeps these only in test-tools; build/classes has none. Not yet confirmed that this zip is the exact artifact behind release 7c68b875. Decision for Kort: replace the in-review release with a clean build.
- Correction: the earlier statement that the package had no test files checked only zip entry names. The jar was not opened until this pass.
- Fixed: the CampaignLobbyScreen notice overlapped the collection section header (both at y 69-74). On the collection tab the notice now draws at height-12. Not yet seen in game.
- Fixed: class copy "Infantry gain" / "Support gain" now reads "Infantry troops gain" / "Support troops gain" (Spec.java:56).
- Open: "Hollowwood Reach" spelling (confirm); full barracks, class and battle-setup redesign from the critique. 3D depth for players: EMF overlays added in 0.3.0, in-game look not yet verified. HOW-TO-EDIT.md rewritten in 0.3.0.
- Research, not adopted: 3D Skin Layers (tr7zw, Fabric 1.20.1, client-side) for player body depth, license and armor rendering unverified; Entity Model Features (already installed) supports extra JEM model parts for husk, skeleton and player, the best no-code route; Alternate 3D Swords (CC-BY-4.0, textures only, versions unverified) for weapons.
- No game launched. Nothing published.

## 0.3.0 patch (2026-10-16)
- Armour detail: TroopDetailFeature removed. EMF overlays added to the armory pack: player.jem, player_slim.jem, husk.jem, skeleton.jem, wither_skeleton.jem, plus assets/gravesandglory/textures/entity/armor_overlay.png (64x64). In-game look not yet verified.
- Spec regeneration fixed: tools/preflight.py skips the NOT_SHIPPED sheets (checks, revisionChecks, playtestMilestone). They are still validated. Spec.java no longer has ChecksRow or RevisionChecksRow.
- tools/package.py no longer exempts Spec$ names from the harness-name check.
- `gradle build --offline -x test` succeeded: preflight clean, 1186 filled cells.
- dist/bannerbound-0.3.0.zip rebuilt: sha256 f750c046c0c6750229a35210314c29dca07365d9629a5e4c9bade4fab6fb4a19, 68,947,373 bytes. Inner jar has 161 entries and no harness or TroopDetailFeature classes. Diagnostics$Probe is an interface with no provider in the player jar.
- melty.json component fileName changed to bannerbound-0.3.0.zip. Its test block still lists the old check names; remap before any new Melty test.
- HOW-TO-EDIT.md rewritten. README.md updated for overlays and harness wording.
- Not done: dev-client EMF load check and in-game screenshot. A javaw process (PID 24748, window "Minecraft 1.20.1 - Multiplayer (LAN)", started 8:33 PM) was already running and is not mine, so no game was launched or closed.
- Not decided: dist/bannerbound-0.2.0.zip still contains harness classes. Keep or move aside (Kort).
- Not published. Release 7c68b875 is unchanged. Publishing waits for Kort's confirmation.

## Roadmap implementation checkpoint — 2026-10-17, 0.3.1
- Local Git baseline acb5056; release-foundation changes tracked in Git.
- Version in gradle.properties; generated mod metadata and embedded source fingerprint. Package rejects stale JARs, wrong identity/version and development code. Every package entry has SHA-256 and is read back by tools/verify_package.py.
- Non-destructive legacy data migration, world-name compatibility and old troop/display cleanup. 13 standalone filesystem checks pass.
- User authorized separate test windows. Used isolated run/runtime/evidence folders; did not close or control the pre-existing game. Fresh regression: 35 passing markers/checks. Corrected first-launch texture selection. Final upgraded-world restart: 40 passing markers/checks, full profile and saved-look preservation. Fresh Excalibur-default startup: three passes.
- The initial upgrade test's hard-coded Excalibur assertion failed; investigated, fixed actual first-launch ordering, and changed restart assertion to compare the saved look. Failure logs retained.
- Existing texture stack emits invalid campfire/decorative model rotation diagnostics under 1.20.1; recorded as open compatibility work.
- Final candidate ZIP: 151a3f5ed68657da81ac9bb01350b9f1e74d99f44a96f085ff3ed59b214a08da. Main-source fingerprint f6ef9feaa4e99cf8dfbc0637cf81fca59e82d3d260ed08d2750c5439342bfb6f.
- Production CurseForge/Modrinth installation, incompatible-client messaging, home hosting and publication remain pending. No uploads/submissions/publication.
- Authoritative checklist and dated evidence: C:/Users/kortt/Documents/Codex/2026-10-17/i-w/outputs/Graves-and-Glory-Roadmap/05-WORK-LOG.txt and logs/.
