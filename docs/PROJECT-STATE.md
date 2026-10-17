# Graves and Glory: project state (consolidated 2026-10-17)

Single source of truth for where the game stands. Pulled together from the
repo, UPDATE_PLAN.md, the Codex roadmap workspace, five Claude sessions from
today (audit, audit fixes, map overhaul, Roblox comparison, work review) and
the release notes. Older notes are superseded by this file where they disagree.

## 1. What the game is

A Fabric 1.20.1 modpack (Java 17, Loom, one original mod `gravesandglory`,
formerly `bannerbound`) inspired by Roblox's *Command an Army*. The player is a
commander who fights in person and leads a 20-troop army in 15-minute
domination matches. Dark-fantasy look: Dawn (gold, knights, sun) vs Dusk
(violet, undead, moon).

Core loop today:

1. Branded title menu -> walled camp courtyard (Quartermaster, Recruiter,
   Archivist, Steward, War Gates). R opens the war council.
2. Arrange 20 deployment slots from unlocked troop types; pick a commander class
   (Vanguard / Marksman / Arcanist, flat % buffs to one troop role).
3. Stand on a ready pad (map vote, countdown, bot fill) or make a private room
   by code with a friend party (up to 4).
4. Battle on one of five 700x500 maps: hold objectives to 300 points, 15-minute
   cap, 12-second commander respawn. Orders: Move/Attack/Charge/Hold/Follow/
   Retreat; formations Compact/Line/Wedge/Loose(/Shieldwall); tactical map (M)
   with pings and fog of war. Barracks lectern buys reinforcements with
   round gold (passive 5 per 5 s, +3 per point held, +1 per kill).
5. Rewards: 80 gold per match +40 win (practice 40), 100 XP +50 win, daily and
   weekly contracts, a free 30-tier "Founders chapter" (titles + 25 gold tiers).
   100 gold buys one chosen or random new troop type (6 starters, 6 unlockable).

Factions: Dawn Knight/Ranger/Sunweaver (+ Sentinel, Longbow, Oracle); Dusk
Revenant/Bone Archer/Gravecaller (+ Reaper, Stalker, Wraith). Troops are
vanilla mob subclasses (husk/skeleton/wither skeleton) driven by `Tactics.java`.

Modes: 1vAI practice, 1v1 to 4v4 (8 humans, 160 troops), bot fill for empty
seats, a joining friend can take over a bot army.

Presentation stack (all pinned, licences recorded): Sodium, Iris, Lithium,
FerriteCore, ImmediatelyFast, ModernFix, EMF/ETF, Fresh Animations, Presence
Footsteps; shaders Complementary Reimagined/Unbound r5.5.1 + MakeUp Ultra
Fast; textures Excalibur+Clarent (default), Faithful 32x, vanilla; built-in
"armory" pack with 3D weapons and EMF armour overlays. Marcellus + Lato UI
type, charcoal/brass "dark ironwork" UI.

## 2. Where everything lives

| Thing | Location |
|---|---|
| Source of truth (git, branch master) | `C:\Users\kortt\Melty\bannerbound` |
| Second working copy, used by Claude for game runs | `C:\Users\kortt\Melty\bannerbound-test` (same HEAD, no Codex camp work) |
| Pre-rename snapshot | `C:\Users\kortt\Melty\bannerbound-pre-rename` |
| Game data tables | `sheets/design.json` -> generated `Spec.java` (never hand-edit) |
| Release artifacts (zip, mrpack, server kit) 0.3.0 to 0.3.6 | `dist/` |
| Codex roadmap, checklist, logs, release candidates | `C:\Users\kortt\Documents\Codex\2026-10-17\i-w\outputs\Graves-and-Glory-Roadmap\` |
| Codex camp/progression session outputs, screenshots | `C:\Users\kortt\Documents\Codex\2026-10-17\go-x20\outputs\` |
| UI concept art (Oct 16, reference only) | `C:\Users\kortt\Documents\Codex\2026-10-16\x20-be\outputs\*.png` |
| Claude audit report (25 findings F-01..F-25) | https://claude.ai/artifact/UJMurrH2TxPnf7GZDM5e5H |
| Map overhaul contact sheets | `work/tour/b4-map*.jpg` (not present in repo listing; `a1`, `b2` sets exist) |
| Prism test instance | `%APPDATA%\PrismLauncher\instances\graves-and-glory-0.3.3` |
| Melty runtime data | `%LOCALAPPDATA%\gravesandglory\` |
| Home server (planned official host) | tjepkema-server, i7-10700K, 96 GB, Starlink (CGNAT) |

## 3. Version and git state

- `gradle.properties` mod_version = 0.3.7 (local candidate, uncommitted).
- Git HEAD `04bec2d` = Claude's audit-fix and map commits (F-01..F-06, F-08,
  F-10, F-17, F-22). Those are committed; nothing is pushed; no remote exists.
- **Uncommitted on top of HEAD (two authors, both unsaved to git):**
  - Codex 0.3.5 -> 0.3.7: `AdmissionPolicy`, `CampScreen`, `CampWorld`,
    `Progression`, `SocialHub`, Campaign/GravesAndGlory changes, server
    templates, release notes 0.3.5/0.3.6.
  - Claude map session: `MapDressing.java` + Arena/CampaignMaps edits
    (spawn parade grounds, capture rings, cover, obelisks, bridge pillars).
- Last live-game evidence is 0.3.4 (Prism import, menu, camp, practice).
  0.3.5 to 0.3.7 have build/policy/unit checks only.
- Published state: Melty listing is 0.2.0 (release 7c68b875, published on
  Kort's instruction; it still contains harness classes and bundles
  Excalibur/Clarent/Fresh Animations whose licences may not allow that).
  0.3.0 is a held Melty draft. Nothing on Modrinth or CurseForge. Any further
  publish requires Kort's explicit go.
- Working tree compiles offline (compileJava + test tools, 2026-10-17 16:10).

## 4. Timeline (what happened in which tool)

| When | Tool | What |
|---|---|---|
| to Oct 15 | Codex (Melty kit) | Bannerbound 0.1.0 published on Melty; 0.2.0 campaign update built, tested, held |
| Oct 16 | Codex | UI redesign (battle setup, class, barracks), armory pack, EMF overlays, 0.3.0 |
| Oct 16 | Claude | Lobby/server research doc; Codex review; harness-leak finding |
| Oct 17 00:00-06:00 | Claude ("continuation") | Rename to Graves and Glory, git init, 0.3.1 migration + packaging |
| Oct 17 01:00-10:30 | Codex (roadmap i-w) | Six-milestone checklist; 0.3.1-0.3.4: migration, version gate, mrpack, branded menu, server kit, Prism launcher test, Iris key fix |
| Oct 17 ~10:00-12:50 | Claude (audit) | 25-finding audit; fixes committed; pad sizing, tutorial, economy, chosen unlocks, Kingsfield map |
| Oct 17 12:00-14:00 | Codex (go-x20) | 0.3.5 camp waiting; 0.3.6 camp courtyard, parties, rooms, contracts, Founders chapter; 0.3.7 |
| Oct 17 13:00-15:00 | Claude (maps) | Map dressing on all 5 maps, walkability scans, scores 6.5-7.5 |
| Oct 17 | Claude | Roblox comparison; "work review" launched dev client for Kort |

## 5. What is verified vs claimed

Verified in a real game (dev client or Prism):
- Menu, camp, inventory, custom room, practice battle, save/quit (0.3.4).
- All 12 river crossings on the four original maps with 20 pathfinding troops.
- Two local clients joining a running match; shaders in 4v4 entrances; intro
  camera ~118 FPS.
- Legacy Bannerbound upgrade preserves profile and look.
- 0.3.6 two-loopback-client party/room/rematch flow (Codex isolated run).
- 0.3.7 camp suite in a real isolated client (2026-10-17 17:00, worktree
  bannerbound-runs/work/verify-037): 33 checks pass (stewards, camp HUD off,
  server-confirmed recruit, party and room packets, room start with bots,
  results and rewards, daily claim and ledger, rematch, short-match no
  reward, failed-save rollback, map-prep auto-start, gate countdown and
  step-off). 14 screenshots under run/screenshots. Seen in them: tutorial
  lesson text overflows its panel; vanilla advancement toast shows in
  battle; camp courtyard is plain stone brick with text signs.

Claimed only by unit/policy tests or code reading:
- Camp waiting admission (746 assertions), contracts/chapter claims, Kingsfield
  Crossroads layout, tutorial, new economy numbers, map dressing walkability.

Never done:
- Any match with real humans beyond Kort + a second local client.
- Any Internet match; any 8-human match; balance of anything.
- Modrinth App / CurseForge install; home-server deployment.
- Visual check of EMF armour overlays in game.

Known broken or stale:
- (fixed e78b81b) Self-test and route harness updated to current code:
  self-test 8/8 pass, routes 20/20 on maps 0-3.
- Kingsfield (map 4) crashes the client when selected from the lobby
  (minimap indexes 5 points before the state packet arrives); fix planned
  in Phase 1.
- Campaign-suite checks `solo-pad-3v3` and `roadmap-suite` fail before Phase 1.
- Pre-existing road blockers: bridge railing posts, Ironhold gate walls,
  Hollowwood log pile (35 cells).
- "Overview" tour camera shot renders at ground level.
- Design sheets unused by code: hooks, landmarkStyle, recruitmentView,
  terrainStyle, duplicateRefund, starterGold.
- Baseline log noise: Excalibur pumpkin hats, Iris BIOME_PALE_GARDEN, ETF
  properties, Realms auth offline.

## 6. Open audit findings (from the 25)

Done: F-01, 02, 03, 04, 05, 06, 08 (first numbers), 10 (chosen unlocks), 17,
18, 19, 20, 22 (one new map).

Still open: F-07 server cost at 160 troops (unmeasured); F-09 reinforcement
walk time (~1 min suspected); F-11 unlocks may be strictly stronger; F-12 Dusk
has no shield equivalent, shield blocking unverified; F-13 classes are flat
buffs; F-14 attacks are hitscan, hard to read; F-15 predictable bots;
F-16 soft counter triangle; F-21 colour-only team identity; F-23 locked
troops never shown; F-24 one battle per server; F-25 texture version match.

## 7. Codex milestone status (01-MASTER-CHECKLIST)

- M1 reliable build/package/saves: mostly done; open = CurseForge export,
  official launcher installs, dependency ID inventory.
- M2 branded lobby-to-battle flow: camp and menu done; open = one visual
  style definition, loading screens, tutorial yard, army presets, roll
  presentation, accessibility.
- M3 servers/rooms/parties: parties, rooms, admission done locally; open =
  Starlink reachability, match states, battle isolation, spectators,
  proxy/routing, backend security.
- M4 secure progression: server-owned profiles + ledger + contracts +
  chapter done locally; open = economy model, more troops, results screen,
  mastery/cosmetics, reset edge tests.
- M5 public matchmaking + Internet beta: not started.
- M6 first season content: not started.

## 8. Decisions Kort has made (recorded)

- Fighting game, not management; matches only, no persistent world.
- Hand-built purposeful maps; players vote.
- No real-money rolls; no stat advantage from progression; titles/cosmetics.
- Target CurseForge + Modrinth; keep Melty working; home-host the official
  server on the 96 GB PC.
- Solo pad starts a bot-filled 3v3 (accepted; folded into F-04).
- Claude tests with screenshots itself; Kort plays occasionally.
- Nothing publishes, launches a game or closes a game without his go
  (launching now allowed via subagent when he says so).
- Never silently delete cut ideas; park them.
- UI direction (Kort's critique, Oct 16): restrained medieval command
  interface, charcoal surfaces, ivory text, brass accents, readable body font,
  "no AI slop". Concept images are direction, not assets.
- Ready pads replace "first joiner is host"; parties and pads exclusive;
  one match at a time (accepted defaults).
- CurseForge: full modpack listing, not mod-only. He has a creator account.
- Licences: Excalibur, Clarent, EFA patch may only be referenced by ID on
  CurseForge, not bundled; Modrinth cannot reference CurseForge files.

## 10. Parked ideas (never delete, not yet decided)

From the Oct 17 night session: hit-stop on commander hits, OGG sound
variants, capture-the-banner mode, flank/rear/morale, daily seeds, ping
wheel, control groups, a fourth commander class. From the audit: veteran
squads, morale and rout, medal challenges, end-of-match report, cosmetic
banners, commander active abilities, bot difficulty tiers, attack/defend
siege mode, single-player campaign of linked battles. From the Roblox
comparison: commander mounts and personal weapon, duplicate-roll unit levels.
Kingdom & Army (older Forge pack) ideas: free-for-all with alliances, war
table with fog of war, player classes Knight/Mage/Ranger/Healer/Tank.

## 9. Gap between concept art and the real game

The Oct 16 concept sheets (battle setup, barracks, class selection) show a
painterly, polished strategy UI with castles, banners and real troop art. The
real camp (Codex screenshot `camp-courtyard.png`) is a flat stone-brick yard
with vanilla text signs and the default HUD font. The UI screens use the
charcoal/brass style but the world itself has not had an art pass. This is the
single biggest "feel" gap and is not on any checklist as a concrete task.
