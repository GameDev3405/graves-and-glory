# Phase 4: Modes and Campaign Chapter 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn Domination into one of four battle modes (Siege on Ironhold, Survival on Hollowwood, Banner on Rivergate), open them through a Skirmish and Practice picker, and ship campaign chapter 1, "The Road North": six battles fought with the player's own banner, told through a storybook of in-game footage, data-driven scenes, dialog cards and four companions who fight, fall and talk in camp, ending in a Legendary Pact.

**Architecture:** One `BattleMode` object per round owns setup, teardown, field bases, anchors, scoring, end, morale shares, bot plans and the ledger; `DominationMode` is today's code moved behind it, and every mode reads its numbers from new sheets (`modes`, `modeSites`, `modeRules`, `waves`, `waveGroups`). Campaign battles seat the player's banner plus the battle's bot banners, pay normal rewards, open in order and track one bonus each, saved atomically with gold in profile schema 3. The story is data too: scene files under `data/gravesandglory/scenes/c1/`, parsed by the pure `SceneScript`, run per player by the server's `ScenePlayer` and drawn by the client's `SceneScreen` and `DialogCard` over a new `scene_v1` channel; the storybook, camp lines, companions and voice read the story sheets through pure classes (`StoryBook`, `StoryLines`, `StoryNames`) checked by standalone `javac` runs. Every scene image is in-game footage (Kort's ruling, 2026-10-18 late): book panels are sets placed on a stage area and shot under a drifting camera, stage scenes play on the battle maps, thumbnails are frames the harness captures, and portraits are the characters' own live models, drawn through Phase 2's troop models.

**Tech Stack:** Minecraft 1.20.1, Fabric Loader 0.16.14 (bundles MixinExtras), Fabric API 0.92.2+1.20.1, Yarn 1.20.1+build.10, Loom 1.7.4, Java 17, Gradle 8.8 (offline); Python 3.12.2 with Pillow and numpy (`tools/ui_art`, `tools/preflight.py`, `tools/economy_model.py`, `tools/write_voice_sounds.py`, `tools/mode_gate.py`); ffmpeg `C:\ffmpeg\ffmpeg-8.1-full_build\bin\ffmpeg` (contact sheets, the test voice fixture).

**Spec:** `docs/superpowers/specs/2026-10-18-phase-4-modes-campaign-design.md` (modes, Skirmish, the six battles, data, save state, harness), and `docs/superpowers/specs/2026-10-18-campaign-story-design.md` (v2, approved 2026-10-18 late; it is newer and wins every conflict: cutscenes and voice are in, boss mechanics stay out, the storybook replaces the march page, the War Table plaque reads "The March", companions are campaign-only in a company row of up to four beside the 20-slot banner). Every player-facing string of chapter 1 is transcribed from `docs/copy/chapter-1.md`, never written here and never edited by a task. The Houses spec (`2026-10-17-houses-design.md`), the built-world spec (`2026-10-17-built-world-design.md`) and the UI spec (`2026-10-18-ui-design.md`) bind as those specs say.

**Base:** branch `phase-4-modes-campaign`, cut from the merge of `phase-1-solo-core`, `phase-2-houses` (worktree `C:\Users\kortt\Melty\bannerbound-p2`) and `ui-plan` (worktree `C:\Users\kortt\Melty\bannerbound-ui`), each complete to its plan. Phase 2 here is its rewritten plan, in which troops have their own models, loader and renderer (its Tasks 5, 5b and 5c), and EMF, ETF and Fresh Animations leave the modpack. That merge's STATE channel is `state_v9`, the union of Phase 2's `state_v8` and the UI plan's `state_v7`; this plan keeps its layout. The Phase 4 spec also assumes Phase 3 (`phase-3-built-world`, worktree `C:\Users\kortt\Melty\bannerbound-p3`): Tasks 4 to 27 need its pieces, landmarks, camp and `Structures` (Task 11 seats Siege, Survival and Banner battles, which need Tasks 5 to 7); merge it before Task 4. Tasks 1 to 3 run without it. Step 1 of every task compiles that base.

## Global Constraints

- Minecraft 1.20.1 and Fabric only; no new dependency, no loader change, no new entity renderer (troops and companions draw through Phase 2's troop renderer), no new map. Pillow and numpy are dev tools and never enter the jar.
- Never hand-edit `src/main/java/gg/gravesandglory/Spec.java`. Change `sheets/design.json`, then `python tools/preflight.py` (also run by every `compileJava`). Every row of a sheet has the same columns and every cell is filled; a column is `double` only if the first row's value has a decimal point (`1.0`, not `1`); an empty sheet is an error, so a sheet is added in the task that has its first rows. Lists inside a cell are comma lists; `none` or `-` means empty, as each sheet below says.
- Every game number lives in a sheet. Numbers the specs mark "(draft value)" are drafts here too and are tuned only through the sims (Task 9), the march run (Tasks 11 and 27) and Kort's gate. Presentation-only constants stay named in code: `SiegeMode.BLOW_CONTEST_TICKS = 60` (the seal stays contested 3 s after a gate blow), `StorybookScreen.WIND_TICKS = 40`, `DialogCard.CARET_MS = 500`, `SceneScreen.FADE_STEP` (one alpha step per tick).
- The harness lives only in `test-tools/`. Add `Phase4Checks`, `ModeMathCheck`, `MarchMathCheck`, `StoryCheck` and `t_voice` to the `forbidden` tuple in `tools/release_config.py`.
- Working copy is CRLF (`core.autocrlf=true`). Edit dense one-line Java in place; never reformat a file or re-save it with other line endings. Add lines to the shared harness files (`test-tools/isolated-checks.gradle`, `test-tools/resources/fabric.mod.json`, `tools/release_config.py`); never reorder them.
- Commit locally per task on `phase-4-modes-campaign`, adding files by explicit path (never `git add -A`), each message ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Never push, publish or upload.
- No launch while Kort plays. Before every launch run `powershell -NoProfile -Command "Get-Process java,javaw -ErrorAction SilentlyContinue | Select-Object Id,StartTime,MainWindowTitle"` and read each java command line: a java process with `--gameDir` outside `C:\Users\kortt\Melty\*\work\` is Kort's, so wait; otherwise at two harness clients, wait (Kort's ruling, 2026-10-18: two harness clients may run at once, never while Kort plays). One client per agent.
- Isolated runs only, always `--offline`: a fresh `GRAVES_ROADMAP_TEST_ROOT` under `C:/Users/kortt/Melty/bannerbound/work/p4/` with `run/options.txt` seeded (recipe below). Never edit anything under `work/`; creating a fresh run root under `work/p4/` is the only write there.
- Pillar 5 binds: nothing is done until a running game shows it by log line and screenshot. The tester opens every named screenshot and states in one sentence what it shows; a screenshot that does not show the claim fails the step even if its check passed.
- Art gate: every scene image is real in-game footage, and nothing is painted (Kort's ruling, 2026-10-18 late). Panels are sets on the stage area, thumbnails are captured frames, and portraits are rendered from live models. Every book leaf, card, chit and kit piece, every set's grading frame in both looks (Task 18), every thumbnail and every portrait is graded 1 to 10 against `work/ui-concepts/GRADING-RUBRIC.md` by a Sonnet 5.5 max tester before Kort sees it, at the detail Kort set for the War Table (two to three times the first round's pixel detail). Below 9 is a fix round. Walkthrough gate (Phase 3's rule): every world piece (Task 4) is walked first-person by a Sonnet 5.5 max tester and graded on the same rubric adapted to world pieces; below 9 is a fix round.
- Executors: Opus 5.5 max or Sonnet 5.5 max only, as named on each task. Testers (runs, screenshots, grades, walkthroughs) are Sonnet 5.5 max.
- Text: ASCII, no dashes as punctuation, no exclamation marks, no modern idiom. Feed lines fit 140 px. A dialog card wraps to at most three lines, an item card to at most five. "Chapter" stays the Founders plaque's word; on screen the campaign is "The Long March, I: The Road North". The game mode is `battleMode` in code; "mode" appears only on screen. A string that does not fit is reported back to the copy, never rewritten by the task.
- The paper is "the Pact" or "a Pact" in every player-facing string, never "Writ". Internal names keep the merged code's: the Pact count is `Profile.writs` and its chapters `Profile.writChapters`, and the Pact command is `legendary:<house>:pact`.
- Packet layouts: STATE stays `state_v9` (mode values ride in its two scores, its point list and its time left; the rest rides in the snapshot JSON it already carries every 10 ticks); `feed_v1` is unchanged (mode notices use kind `notice`, which draws the viewer's emblem); the story adds channel `gravesandglory:scene_v1`. Any later layout change bumps the channel id; host and client must run the same build.
- Scene ids, voice ids and speaker initials are the copy's (voice ids `c1_` + initial + two digits). Cut or deferred ideas go to `docs/PARKED.md` (append, never delete).

## Review Focus

1. A round that ends abnormally in the middle of a mode (every player leaves, `switchMap`, the server stops, or `finish` while the portcullis countdown runs or a banner is carried): the gate, wards, flags, world clock, carrier helmet, glow and speed are restored, no countdown fires later, and the next Domination round on that map plays normally. (Tests: `p4-mode-teardown-mid` and `p4-siege-crash`, Task 5; `p4-banner-teardown-head`, Task 7.)
2. A scene interrupted by a disconnect, Escape, a server stop or a map switch: the camera, HUD and game mode come back, actors and scene props are gone, the scene stays unread so it plays again, and a pending battle or the deferred report still follows. (Test: `p4-scene-interrupt`, Task 17.)
3. Copy that does not fit: a card that wraps to four lines, a long speaker label, a feed line over 140 px (the Crimson yard line measures 143), at GUI Scale 1 to 3 and the 854x480 window. (Tests: `p4-feed-fit`, Task 3; `p4-scenes-fit`, Task 16.)
4. A profile that played before the story existed (schema 2 with `campaignCleared` 3, or holding the Pact that Phase 2's one-battle chapter granted): it migrates once, keeps its Pact (the Pact count and `writChapters` untouched), opens the book on page 1 with won battles and their before-scenes read, catches up through the unread after-scenes before the next battle, and no companion appears before the scene that brings them. (Tests: `p4-migrate-v2`, Task 10; `p4-book-catchup`, Task 13; `p4-book-migrated`, Task 24.)
5. Input during a scene: a rebound use key, an unbound skip key, held movement keys, Escape, mouse clicks and the number keys on the choice chits. The real binding is shown, use hurries the typing and then advances, skip is refused on a first reading and allowed on a replay, the player does not move, and Escape opens the pause menu and returns to the same card. (Test: `p4-scene-keys`, Task 16.)

## Constraints from the merged code (and the smallest change chosen)

- `Battle.tick` scores `+1` per owned point when `time%R.scoreInterval()==0`, finishes at `R.winScore()` and times out at `R.roundSeconds()`; `Battle.roundSeconds` starts from `R.roundSeconds()`; `BattleHud` reads `R.winScore()` for the tracks and `WarTableHome` draws `"Domination to "+R.winScore()`. Task 2 moves the three columns to `modes` and routes every read through `b.mode` (server) or the snapshot's `battleMode` (client).
- `Battle.base(t)` is fixed per team from `Spec.FACTIONS` rows 0 and 1. The sheet holds x -108 and 108, but `CampaignMaps.select` rewrites both rows for every battle map: spawn (-283,0) and barracks (-286,32) for team 0, spawn (283,0) and barracks (286,-32) for team 1. Respawn (`toBase`), the lineup (`reinforce`), deploy, `deployCentre`, the retreat assignment and `Morale`'s rout home read `base(t)`; barracks access (the `accessRadius` test) and the barracks teleport read the row's `barracksX/Z`. Task 2 adds `Battle.fieldBase(int team)` and `Battle.fieldBarracks(int team)` (the mode's answers) and switches those call sites; `base(t)` stays for squad creation, camp and legacy code.
- `Morale` reads one row (`M=Spec.MORALE[0]`) and keeps `RoutGuard { boolean holds(Battle b,Squad s,String group); }` guards in `GUARDS`; Phase 2's `routShareFor` picks the share. Task 2 makes the row the mode's (`modes.morale`) and the share `b.mode.routShare(...)`, lowest wins.
- `Campaign.soloStart` seats a battle as practice (one bot, practice gold, every troop); `prepareRound` creates `practice?1:size` bots per team with tier `activeBattle.difficulty()`, and `botBanner(s)` picks a `botBanners` row of that tier by lane. Task 11 adds `Squad.fixedBanner` so each campaign seat fields exactly its row.
- `Campaign.finish(int winner)` computes the reward, raises `campaignCleared`, saves, builds the report and calls `returnLobby()`, in that order. Tasks 11, 17, 25 and 26 extend it in place; an after-scene defers only `returnLobby()`.
- `campaign:<id>` starts any row in any order behind `soloRefusal(MARCH_SOLO)`. Task 11 adds the order lock, Task 17 the before-scene.
- `GravesAndGlory.openCamp(ServerPlayerEntity p,String station)` (static) returns early for a commander seated in a running battle, then sends one station string and the client opens that page (`army`, the war table in the command tent, opens the lobby; Phase 3 stations come from `campStations` and `campPieces`, tents send `recruit:<house>`, the graveyard shrine `recruit:graveborn`, the archivist `contracts`, the Founders' Stone `chapter`). Task 16 calls `Story.atStation(p,station)` first and skips the page when it returns true; Task 26 adds the camp lines.
- `CampaignClient` runs the 120-tick entrance (`cinematic()`, `sampleCamera(partial)`) and draws `briefing(...)` only for campaign battles. `EntranceCameraMixin` (Camera.update TAIL) and `CinematicHudMixin` live in main; scenes generalise both, and nothing in main may call `TitleDemos` (test-tools). Phase 2 Task 5b deletes `BipedAnimationMixin`: every troop rig, illagers included, takes a scene pose through `TroopAnimator.setScenePose(int entityId,String pose)` (`sword_up`, `kneel`, `fallen`, `stand`, `helm_off`, `point`, `sit`, `raise`), which moves into place over 0.4 to 0.8 s and holds, so this plan adds no pose mixin. Troops draw no vanilla held item (Phase 2 Needs Kort 10), so scenes place props as item displays.
- Command tags never reach the client, and custom names stay troop names, because `TroopModels.resolve` reads them to give each troop its own model (Phase 2 Task 5). Portraits on cards, the book and the banner page are live models rendered into the portrait rect (`ArmyMobs.portrait`, `Portrait.draw`), never textures. The client learns companions, labels and poses only from the snapshot's `company` array and `scene_v1` pose ops, keyed by entity id. The Phase 2 interfaces this plan consumes are `ArmyMobs.create`/`portrait`/`stageName`, `ArmyCards.portrait`, `TroopModels.resolve`/`portraitTall`, `TroopAnimator.setScenePose`, `Battle.joinSide` and `Battle.shield`; each task's Interfaces block names the ones it uses.
- `tools/preflight.py` requires `botBanners` ids of the form `<house>_<difficulty>` or `mix_<house>_<house>_<difficulty>`, difficulties that are `botCommand` ids, slots that are troops or `^`, and slot weights summing to 20. Task 10 adds two narrow exceptions for the campaign-only row.
- `Profile` is schema 2 (pure; `sanitize` sets `schema=2`); `ProfileMigration.upgradeFile` upgrades only schema 1 and keeps `<uuid>.v1.json`. Task 10 adds the schema 2 to 3 step beside it.
- `FeedLines.bannerDown(name)` already means a commander's banner fell, so Banner mode's "down" line is `FeedLines.bannerLying(owner)`. `FeedLines.style(kind,...)` draws the viewer's emblem for any unlisted kind, so mode notices use kind `notice`.
- `Structures.place(w,template,x,elevation,z,rotation)` (Phase 3) writes a piece centred on (x,z) and saves nothing; Task 5 adds `placeSaving` and `restore`. Phase 3's `landmarks` suite derives its `p3-lm-<id>-*` checks from every `landmarks` row, so a new row is tested by running that suite.
- `ArmySupplyBanner` (Phase 2) is the structure-target pattern: an armor stand implementing `ArmyMob`, tag `structure`, HP in a field, `damage` subtracting the resolved amount, with counters C10 x4.0 for siege troops and C11 x0.25 for the rest.

## Files at a glance

| File | Responsibility | Task |
|---|---|---|
| `ModeMath.java` (pure) | wave counts, rout shares, ledger text, rule and size lines, Banner roles | 1 |
| `BattleMode.java`, `Modes.java`, `DominationMode.java` | the per-round mode object, the registry and crash marker, Domination moved behind it | 2 |
| pieces `ironhold/gate_intact`, `ironhold/ward`, `kingsfield/mounds`; the archive's Stone letters | the world pieces Siege and the story need | 4 |
| `SiegeMode.java`, `GateTarget.java` | Siege stages, the gate target, wards, bot plans | 5 |
| `SurvivalMode.java` | waves, the shrine, the director squad | 6 |
| `BannerMode.java` | stands, carriers, drops, roles | 7 |
| `tools/mode_gate.py` | the mode and march gates over the sim CSVs | 9, 11 |
| `MarchMath.java` and `StoryState.java` (pure), `Bonus.java` | order lock, cleared, chapter done, finish bookkeeping; the story state saved inside the profile; the six bonus trackers | 10, 11 |
| `StoryBook.java`, `StoryLines.java`, `StoryNames.java` (pure) | book states, line choice, labels, the familial-rule count | 13 |
| `SceneScript.java` (pure), `scenes/c1/*.scene`, `tools/write_voice_sounds.py` | the scene file parser, the twenty scene files, the voice layout | 14 |
| `tools/ui_art/storybook/` | the storybook's UI kit | 15 |
| `Story.java`, `ScenePlayer.java`, `SceneScreen.java`, `DialogCard.java`, `StoryVoice.java`, `OggLength.java` (pure) | server story glue and one running scene per player; the client's scene screen, cards with live portraits, and voice | 16 |
| `SceneMath.java` (pure), `SceneCamera.java`, `SceneSky.java`, `ScenePoses.java`, `MarchCard.java`; mixins `SceneSkyMixin`, `SceneFogMixin` | stage scenes: camera, time and weather, fog, poses, letterbox, the march card | 17 |
| `StageArea.java`, suite `story-capture`, `build_ui_art.py --thumbs` | the stage area for panels, frame capture, thumbnails | 18 |
| `structures/story/*.nbt`, their studio scripts and `storySets` rows | the sets, staged and graded from their frames | 19 to 23 |
| `StorybookScreen.java` | the book and the March plaque | 24 |
| `Company.java`, `CompanyNames.java`; mixin `CompanyLabelMixin` | companions in battle, protection, the fall, overhead labels | 25 |
| `StoryCamp.java` | the viewer's own companions in camp, as client-only entities, so no other player sees them | 26 |
| `Phase4Checks.java`, `ModeMathCheck.java`, `MarchMathCheck.java`, `StoryCheck.java` (test-tools) | suites and standalone checks | 1 onward |

## Test runs used by every task

**Preflight:** `cd /c/Users/kortt/Melty/bannerbound && python tools/preflight.py` prints `Preflight clean`.

**Compile both source sets** (Step 1 of every task, against the base, and again after each change):
```bash
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle)
"$G" compileJava compileLocalTestTools --offline
```
Pass: `BUILD SUCCESSFUL`.

**Standalone pure checks** (seconds, no game):
```bash
cd /c/Users/kortt/Melty/bannerbound
J="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot/bin"; S=src/main/java/gg/gravesandglory
"$J/javac" --release 17 -d build/p4-checks $S/Spec.java $S/ModeMath.java test-tools/standalone/ModeMathCheck.java && "$J/java" -cp build/p4-checks gg.gravesandglory.ModeMathCheck
"$J/javac" --release 17 -d build/p4-checks $S/Spec.java $S/MarchMath.java test-tools/standalone/MarchMathCheck.java && "$J/java" -cp build/p4-checks gg.gravesandglory.MarchMathCheck
"$J/javac" --release 17 -d build/p4-checks $S/{Spec,BattleMath,RosterMath,CounterMath,Progression,Profile,ProfileMigration,StoryState,MarchMath}.java test-tools/standalone/{HousesCheck,ProfileMigrationCheck}.java && "$J/java" -cp build/p4-checks gg.gravesandglory.ProfileMigrationCheck
"$J/javac" --release 17 -d build/p4-checks $S/{Spec,StoryState,StoryBook,StoryLines,StoryNames,SceneScript,OggLength,SceneMath,MarchMath}.java test-tools/standalone/StoryCheck.java && "$J/java" -cp build/p4-checks gg.gravesandglory.StoryCheck src/main/resources docs/copy/chapter-1.md test-tools/resources
```
Pass: `<Check>: <n> checks passed`; StoryCheck also prints `story-names: clean`, `story-voice: <v> voiced, <u> unrecorded, <s> silent, clean` and `story-copy: <n> lines match`. (Voice is recorded after the copy is locked, story 12.4, so a voiced line without its `.ogg` yet is "unrecorded", not an error; `sounds.json` lists only files that exist.) ProfileMigrationCheck needs `HousesCheck` (its fixture catalog) and that class's pure dependencies. A failure throws `AssertionError` naming the check. A task that grows a check's class set updates its line here in the same commit.

**Art export check:** `python tools/build_ui_art.py --verify` prints `build_ui_art: <n> textures verified`.

**In-game suite `<suite>`** (one launch; the suite stops the client itself):
```bash
# Pre-launch process check from Global Constraints first.
export GRAVES_ROADMAP_TEST_ROOT="C:/Users/kortt/Melty/bannerbound/work/p4/<suite>-$(date +%H%M%S)"
mkdir -p "$GRAVES_ROADMAP_TEST_ROOT"/{run,evidence,runtime}
printf "pauseOnLostFocus:false\nonboardAccessibility:false\n" > "$GRAVES_ROADMAP_TEST_ROOT/run/options.txt"
GRAVES_PHASE4_CHECKS=<suite> "$G" runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir $GRAVES_ROADMAP_TEST_ROOT/run"
grep -E "^(pass|fail) " "$GRAVES_ROADMAP_TEST_ROOT/evidence/local-checks.txt"; grep "\[p4\]" "$GRAVES_ROADMAP_TEST_ROOT/run/logs/latest.log"; ls "$GRAVES_ROADMAP_TEST_ROOT/run/screenshots"
```
Pass: every id the step names prints `pass`, `pass p4-<suite>-suite` is present, the named screenshots exist, and the tester states one sentence per screenshot. Several suites may run in one launch as a comma list. Phase 1, 2 and 3 suites run the same way with `GRAVES_PHASE1_CHECKS`, `GRAVES_PHASE2_CHECKS` and `GRAVES_PHASE3_CHECKS`.

**Bot runs:** Phase 2's recipe (`"$G" runServer --offline -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=<m> -PsimRoundSeconds=<s> -PsimBanner0=<a> -PsimBanner1=<b> --init-script test-tools/isolated-server.gradle`), with Task 9's `-PsimMode=<mode>` and Task 11's `-PsimMarch=<battle id>`.

---

### Task 1: ModeMath, the pure mode rules

**Executor:** Sonnet 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/ModeMath.java` (pure: no Minecraft imports)
- Create: `test-tools/standalone/ModeMathCheck.java` (the `UiMathCheck` shape: `check(boolean,String)`, a counter, the summary line)
- Modify: `tools/release_config.py` (`forbidden` gains `'Phase4Checks', 'ModeMathCheck', 'MarchMathCheck', 'StoryCheck', 't_voice'`, appended)

**Interfaces:**
- Produces (all `static` in `ModeMath`):
  - `int waveCount(int base,int banners,double perBanner,double tierScale)` = `ceil(base*(1+perBanner*(max(1,banners)-1))*tierScale - 1e-9)` (spec 6: counts round up).
  - `double routShare(double houseShare,double wallShare,boolean insideWalls)`: `insideWalls ? min(houseShare,wallShare) : houseShare`. `houseShare` is Phase 2's `Morale.routShareFor` result, so fearless 0.12 stays lowest and the walls beat dread's 0.40.
  - `String ruleLine(String template,int n)`: `{n}` replaced by n.
  - `List<String> howTo(String cell)`: split on `|`, trimmed, empty parts dropped, at most 3.
  - `String sizeLine(String battleMode,int size,String tierName,String side)` (spec 9): Domination and Banner `n+" v "+n+", "+tier+(n==1?" bot":" bots")`, the UI plan's line; Survival `n+(n==1?" banner, ":" banners, ")+tier+" waves"`, where n counts every defending banner; Siege `n+" v "+n+", "+side+", "+tier+(n==1?" bot":" bots")`.
  - `int siegeStrength(int stage,double progress,double hp,double maxHp)`: stages 0 and 2 `round(100*(1-progress))`, stage 1 `round(100*hp/maxHp)`, stage 3 (fallen) 0.
  - `List<String> roles(int banners,int humans,double raidShare)` (spec 7): one role per banner, humans first. Raiders = `max(1,round(banners*raidShare))`; indexes below `humans` are `raider` whatever the count; the raid slots humans leave go to the next bots; then one `guard`; the rest `holder`. One banner with no human is `split`.
  - `String ledgerText(String format,int value,int max)`: `fraction` gives `value+"/"+max`, `percent` gives `value+"%"`, `plain` gives `""+value`.

- [ ] **Step 1: Compile both source sets** against the base; expect `BUILD SUCCESSFUL`.
- [ ] **Step 2: Write `ModeMathCheck`:**
```java
check(waveCount(8,1,0.5,1.0)==8 && waveCount(8,4,0.5,1.0)==20 && waveCount(8,0,0.5,1.0)==8, "banners: x1 plus 0.5 per extra banner");
check(waveCount(8,1,0.5,0.75)==6 && waveCount(10,1,0.5,0.75)==8 && waveCount(3,1,0.5,1.25)==4 && waveCount(16,4,0.5,1.25)==50, "tier scale, rounded up");
check(routShare(0.25,0.15,true)==0.15 && routShare(0.25,0.15,false)==0.25 && routShare(0.12,0.15,true)==0.12 && routShare(0.40,0.15,true)==0.15, "lowest share wins inside the walls");
check(ruleLine("Domination to {n}",300).equals("Domination to 300") && ruleLine("Siege: yard, gate, keep",3).equals("Siege: yard, gate, keep") && ruleLine("Survival: {n} waves",7).equals("Survival: 7 waves"), "rule lines");
check(howTo(" a | b |  | c | d ").equals(List.of("a","b","c")) && howTo("one").equals(List.of("one")), "how-to cells");
check(sizeLine("domination",2,"Normal","none").equals("2 v 2, Normal bots") && sizeLine("domination",1,"Easy","none").equals("1 v 1, Easy bot") && sizeLine("survival",2,"Normal","none").equals("2 banners, Normal waves") && sizeLine("survival",1,"Easy","none").equals("1 banner, Easy waves") && sizeLine("siege",2,"Normal","attack").equals("2 v 2, attack, Normal bots") && sizeLine("banner",3,"Hard","none").equals("3 v 3, Hard bots"), "size lines");
check(siegeStrength(0,0.36,0,0)==64 && siegeStrength(1,0,400,1600)==25 && siegeStrength(1,0,1600,1600)==100 && siegeStrength(2,1.0,0,0)==0 && siegeStrength(3,0,0,0)==0, "siege strength");
check(roles(2,1,0.5).equals(List.of("raider","guard")) && roles(1,0,0.5).equals(List.of("split")) && roles(3,0,0.34).equals(List.of("raider","guard","holder")) && roles(4,2,0.5).equals(List.of("raider","raider","guard","holder")) && roles(1,1,0.5).equals(List.of("raider")) && roles(2,2,0.5).equals(List.of("raider","raider")), "banner roles, humans first");
check(ledgerText("fraction",1,3).equals("1/3") && ledgerText("percent",64,100).equals("64%") && ledgerText("plain",12,40).equals("12"), "ledger text");
```
- [ ] **Step 3: Run the ModeMathCheck line** (Test runs); expect FAIL: `cannot find symbol: class ModeMath`.
- [ ] **Step 4: Implement `ModeMath`** with the signatures above.
- [ ] **Step 5: Run it; expect `ModeMathCheck: 9 checks passed`.** Compile both source sets.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/ModeMath.java test-tools/standalone/ModeMathCheck.java tools/release_config.py
git commit -m "Phase 4: ModeMath, the pure mode rules"
```

---

### Task 2: The mode frame, with Domination behind it

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{BattleMode,Modes,DominationMode}.java`, `test-tools/java/gg/gravesandglory/Phase4Checks.java`
- Modify: `sheets/design.json`: new `modes` and `modeRules`; `rules` loses `winScore`, `scoreInterval` and `roundSeconds`; `morale` gains column `wallRoutShare` and row `siege`; `botCommand` gains five columns
- Modify: `tools/preflight.py` (the `modes` rules in Step 4)
- Modify: `GravesAndGlory.java` (`Battle.mode`, `fieldBase`, `fieldBarracks`, the tick, `sendState`, the call sites named in "Constraints from the merged code"), `Morale.java`, `Campaign.java`, `Arena.java` (`unflag`), and every other reader of the three removed columns (`grep -rn "winScore()\|scoreInterval()\|roundSeconds()" src test-tools`); client readers switch to `Modes.row("domination")` only so they compile (Task 3 draws from the snapshot)
- Modify: `test-tools/isolated-checks.gradle` (`systemProperty 'gravesandglory.phase4Checks', System.getenv('GRAVES_PHASE4_CHECKS') ?: ''`, after the phase 2 line), `test-tools/resources/fabric.mod.json` (client entrypoint `gg.gravesandglory.Phase4Checks`, after `Phase2Checks`)

**Rows.** `modes` (columns `id, name, maps, roundSeconds, winTarget, scoreInterval, passiveScale, pointScale, morale, timeout, ruleLine, howTo, howToDefend`; `maps` is a string in every row, `"1"` and not `1`):

| id | name | maps | roundSeconds | winTarget | scoreInterval | passiveScale | pointScale | morale | timeout | ruleLine |
|---|---|---|---|---|---|---|---|---|---|---|
| domination | Domination | 0,1,2,3,4 | 900 | 300 | 80 | 1.0 | 1.0 | domination | score | Domination to {n} |
| siege | Siege | 1 | 600 | 3 | 0 | 1.0 | 0.0 | siege | team1 | Siege: yard, gate, keep |
| survival | Survival | 3 | 0 | 0 | 0 | 0.0 | 0.0 | domination | team0 | Survival: {n} waves |
| banner | Banner | 0 | 720 | 3 | 0 | 1.0 | 1.0 | domination | score | Banner: first to {n} |

`howTo` and `howToDefend` are transcribed from the copy's Modes section, the three lines joined with ` | `: Siege and Banner as their two columns read; Domination's one set in both; Survival's `howTo` column in both (assumption: the copy offers two Survival sets and says to copy one over the other). `modeRules`, one row: `id default, siegeStageBonus 180, portcullisSeconds 10, breachShare 0.25, survivalPerBanner 0.5, riseWarning 5, maxDeadAlive 60, carrierSpeed 0.9, pickupRadius 1.5, standRadius 3.0, dropReturnSeconds 20`. `morale`: `wallRoutShare` 0.25 on `domination`; row `siege` copies `domination` with `wallRoutShare` 0.15. `botCommand` (easy, normal, hard): `useStations` false, true, true; `raidShare` 0.34, 0.5, 0.5; `chaseRange` 40.0, 80.0, 120.0; `waveScale` 0.75, 1.0, 1.25; `waveTraitTier` 0, 1, 2.

**Interfaces:**
- Consumes: Task 1 `ModeMath`; Phase 2 `Morale.routShareFor(Battle,Squad,String)`, `Morale.RoutGuard`, `Battle.reinforcementAnchors(int)->List<Anchor>`, `record Anchor(double x,double z,String name,int team)`; `Arena.flag(ServerWorld,Spec.PointsRow,int)`.
- Produces `abstract class BattleMode` (field `final Spec.ModesRow row`). The defaults are Domination's behaviour, so `DominationMode` overrides only `tick`:
  - `Spec.MoraleRow morale()`: the `morale` row named by `row.morale()`.
  - `void setup(Battle b)` after `prepareRound` and before the first `reinforce`; `void teardown(Battle b)`, idempotent, undoing everything `setup` and the round changed; `void tick(Battle b)` on each running tick after `captureTick`.
  - `Vec3d fieldBase(Battle b,int team)` (default `Battle.base(team)`): respawn, the lineup, routs and the deploy fallback. `Vec3d fieldBarracks(Battle b,int team)` (default the faction row's `barracksX/Z` at y 65): barracks access and the barracks teleport. `Vec3d lineup(Battle b,int team)` (default `fieldBase`): the point `reinforce`'s row formula treats as the base at round start.
  - `List<Anchor> anchors(Battle b,int team)` (default `b.reinforcementAnchors(team)`); `List<Capture> seals(Battle b)` (STATE's point list and the M map; default `b.points`); `boolean capturable(Battle b,Capture c)` (default true; `captureTick` skips a point that is not).
  - `int income(Battle b,Squad s)` = `(int)(E.passiveGold()*row.passiveScale() + min(E.objectiveIncomeCap(), owned*E.pointGold()*row.pointScale()))`, `E=Spec.ECONOMY[0]`.
  - `int timeoutWinner(Battle b)`: `score` gives the higher score or -1, `team0` 0, `team1` 1.
  - `int clock(Battle b)` (seconds shown; default `roundSeconds` minus elapsed); `int[] ledger(Battle b)` (STATE's two score fields; default the scores); `int[] max(Battle b)` (default `{winTarget,winTarget}`); `String[] formats()` (default `{"plain","plain"}`); `int count(Battle b)` (the report's stage, wave or capture count; default 0); `int ruleN()` (the `{n}`; default `winTarget`).
  - `double routShare(Battle b,Squad s,String group,Vec3d centre,double houseShare)` (default `houseShare`); `boolean holds(Battle b,Squad s,String group)` (default false; one `Morale.RoutGuard` registered once at init asks `battle.mode.holds`).
  - `void botPlan(Battle b,Squad s,Spec.BotCommandRow tier)`: runs right after `Campaign.botDecide` and may replace `s.target` and `s.assignments`; default nothing.
  - `String howToSide(Battle b,Squad s)`: `attack` or `defend`; default `attack`.
  - `JsonObject snapshot(Battle b,Squad viewer)` = `{"id","name","rule","howTo":[...],"howToDefend":[...],"side","max":[a,b],"format":[f0,f1]}`, where `rule` is `ModeMath.ruleLine(row.ruleLine(),ruleN())` and the lists are `ModeMath.howTo` of the cells; modes add keys of their own.
- Produces `Modes`: `register(String id,Function<Spec.ModesRow,BattleMode> factory)`, `boolean registered(String id)`, `Spec.ModesRow row(String id)` (throws `IllegalArgumentException` naming an unknown id), `BattleMode create(String id)`, `boolean hosts(String id,int map)`. Task 2 registers `domination`; Tasks 5 to 7 register theirs. The crash marker is `<world save>/gravesandglory/mode-marker.json`, `{"mode","map","time","flagsDown","saved":[]}`, written by `mark(Battle b,long worldTime,boolean flagsDown,List<String> saved)` (atomic move, as profiles are saved), deleted by `unmark(Battle b)`, and read by `recover(Battle b)` once the campaign's map is ready (built or cached): when the marker's map is the built map it puts the world time back, puts the flags back with `Arena.flag(w,p,-1)` for every point of the map and deletes the marker (Task 5 adds the saved pieces); otherwise it deletes the marker and its saved files, since a rebuild replaced the arena.
- Produces `Arena.unflag(ServerWorld w,Spec.PointsRow p)`: air over every block `flag` places.
- Produces in `Battle`: `public BattleMode mode` (a `DominationMode` outside rounds). `startRound` sets `mode=Modes.create(campaign.battleMode)`, calls `setup`, and sets `roundSeconds=mode.row.roundSeconds()` unless `BotSimulation` overrides it. `returnLobby`, `switchMap` and `SERVER_STOPPING` call `teardown` and reset it. `Vec3d fieldBase(int team)` and `Vec3d fieldBarracks(int team)` delegate to the mode. The tick: Domination's interval scoring moves to `DominationMode.tick` (`time%row.scoreInterval()==0`, finish at `row.winTarget()`); income is `mode.income`; the time-out is `if(running&&mode.row.roundSeconds()>0&&time-roundStarted>=roundSeconds*20L)campaign.finish(mode.timeoutWinner(this))`; `sendState` writes `mode.ledger`, `mode.seals` and `mode.clock`.
- Produces in `Morale`: `static Spec.MoraleRow M(Battle b)` (= `b.mode.morale()`) replaces the static row; the rout test uses `b.mode.routShare(b,s,group,centre,routShareFor(b,s,group))`; routing groups run to `b.fieldBase(s.team)`.
- Produces in `Campaign`: `String battleMode="domination"`, a lobby setting kept across rounds like `map`; command `battlemode:<id>`, refused with a notice and no change unless `Modes.registered(id)` and `Modes.hosts(id,map)`. The notice reads `<mode name> is not played on <map name>.` (an engineering line the copy lacks; reported back to the copy). `map:<n>` keeps the mode only if it hosts the new map, else resets it to `domination`. The snapshot key `battleMode` is `b.mode.snapshot(b,viewer)` during a round, else the lobby mode's `snapshot` from a fresh instance.
- Produces the `Phase4Checks` scaffold (the `Phase2Checks` shape): `interface Suite { void step(MinecraftClient c,int tick); }`, `SUITES`, `report(String id,boolean pass,String why)` (the MeltyTest line and `[p4] <id>=<pass|fail> <why>`), `shot(MinecraftClient c,String name)` saving `p4-<name>.png`, `server(MinecraftClient c)`, `finish(MinecraftClient c)` printing `pass p4-<suite>-suite` when every id the suite reported passed.

- [ ] **Step 1: Compile both source sets** against the base.
- [ ] **Step 2: Write suite `modes`:**
  - `p4-modes-refuse`: on map 0, `battlemode:siege` leaves `Campaign.battleMode` at `domination` and sends the notice; `battlemode:nosuch` is refused the same way; `battlemode:domination` is accepted.
  - `p4-modes-json`: the lobby snapshot's `battleMode` has id `domination`, rule `Domination to 300`, three `howTo` lines equal to `ModeMath.howTo` of the sheet cell, `howToDefend` equal to `howTo`, max `[300,300]` and format `["plain","plain"]`.
  - `p4-modes-domination`: a practice round on map 0 with scores forced to 299 and 0 and point A owned by team 0 finishes for team 0 at the next 80-tick interval. A round with `roundSeconds` 30 and scores 5 to 3 finishes for team 0 at 600 ticks, and one at 4 to 4 is a draw. With no point owned, a squad earns 5 gold per 100 ticks.
  - `p4-modes-morale`: `Morale.M(b).id()` is `domination`; a knight group at 20% strength routs, and it runs to `b.fieldBase(team)`, which equals `Battle.base(team)` in Domination.
  - `p4-mode-marker`: during a round the marker exists with mode `domination` and map 0, and after `returnLobby` it is gone. A marker written by hand with `flagsDown` true and time 18000 is recovered by `Modes.recover`: the world time reads 18000, point A's flag blocks are back, and the file is gone.
- [ ] **Step 3: Compile; expect `cannot find symbol` for `BattleMode` and `Modes`.**
- [ ] **Step 4: Add the sheets and their preflight rules:** every `modes.maps` entry is a map index; `morale` names a `morale` id; `timeout` is `score`, `team0` or `team1`; `howTo` and `howToDefend` hold one to three parts; `ruleLine` holds at most one `{n}`. Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement `BattleMode`, `Modes`, `DominationMode` and `Arena.unflag`,** switch the call sites, and remove every read of the three `rules` columns.
- [ ] **Step 6: Wire the harness files, compile, and run suite `modes`;** then Phase 1 suites `combat,morale,reinforce,report` and Phase 2 suite `engine`, each with its own variable. Expected: the five `p4-modes` ids pass with `pass p4-modes-suite`, and every Phase 1 and Phase 2 id passes as before.
- [ ] **Step 7: Commit** (add each other file the grep found by its path).
```bash
git add sheets/design.json tools/preflight.py src/main/java/gg/gravesandglory/{BattleMode,Modes,DominationMode,GravesAndGlory,Morale,Campaign,Arena,Spec}.java test-tools/java/gg/gravesandglory/Phase4Checks.java test-tools/isolated-checks.gradle test-tools/resources/fabric.mod.json
git commit -m "Phase 4: the battle mode frame, Domination moved behind it, the modes sheet"
```

---

### Task 3: Mode feed lines, the ledger and the briefing how-to lines

**Executor:** Sonnet 5.5 max.

**Files:**
- Modify: `src/main/java/gg/gravesandglory/{FeedLines,FeedEvents,BattleHud,CampaignClient,WarTableHome}.java`
- Modify: `test-tools/java/gg/gravesandglory/Phase4Checks.java` (suite `hud`)

**Interfaces:**
- Consumes: Task 2's snapshot `battleMode` and STATE fields; `ModeMath.howTo`, `ledgerText`; UI `UiMath.trackFill`, `UiMath.trimWords`, `FeedLines.style`, `FeedLines.teamShort`.
- Produces in `FeedLines` (pure; each line transcribed from the copy's Modes tables, the "Line" column): `yardTaken(String team)`, `portcullis(int seconds)`, `gateBreached()`, `deadRise(String site)` ("Dead rise at " plus the site name), `waveAtRest(int wave,int gold)`, `shrineTaken()`, `bannerTaken(String team,String other)`, `bannerHome(String owner)`, `bannerLying(String owner)`, `bannerScore(String team,int a,int b)`, and `bonus(boolean kept,String tag)` from the copy's "Campaign bonus".
- Produces `FeedEvents.mode(Battle b,String kind,int team,String text)`: sent to every player in the round. Kind `point` draws the acting team's seal (struck for the other side) and is used for the copy's "seal / struck seal" and "struck seal" rows; kind `notice` draws the viewer's emblem and is used for the "emblem" rows. Survival's struck-seal lines are sent with team 1, the dead.
- Produces `CampaignClient.battleMode` (`JsonObject`, never null; Domination's lobby object until the first snapshot) and `CampaignClient.howTo()->List<String>` (the `howTo` or `howToDefend` list by the snapshot's `side`). `briefing(...)` draws the how-to lines in `body` `SEPIA` under the reason, one per row. A round of any mode but Domination outside the campaign draws the same card with the mode's name as its title and the how-to lines alone.
- `BattleHud` draws side i's number as `ModeMath.ledgerText(format[i],value,max[i])` and its track as `UiMath.trackFill(value,max[i])`; the field card's first line is the snapshot's `rule`.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `hud` (this task's ids):**
  - `p4-feed-fit`: with the real font, every line above at its widest values measures at most 140 px: teams Dawn, Dusk, Gold, Violet, Crimson and Teal (as `teamShort` gives them) in both team slots, sites `the chapel`, `the ford`, `the deep wood`, `the track`, waves 1 to 7 with gold 0 to 100, scores 0 to 3, both bonus forms with the tag `strike the King`. The one exception is `yardTaken("Crimson")` (143 px, the copy's known overflow): its drawn text after `UiMath.trimWords(text,140,...)` is at most 140 px, and the suite logs `[p4] feed-overflow yardTaken Crimson <px>`. Any other line over 140 fails and is reported to the copy.
  - `p4-feed-lines`: each new line sent once through `FeedEvents.mode` reaches the feed. A `point` line from team 0 shows `hud/fgv-seal-dawn` to a team 0 viewer and the struck seal to a team 1 viewer; a `notice` shows the viewer's emblem. Screenshots `p4-feed-yard`, `p4-feed-rise`, `p4-feed-wave`, `p4-feed-banner`, `p4-feed-score`, `p4-feed-bonus`.
  - `p4-briefing-howto`: battle `march-1`'s intro card draws its briefing, its reason and the three Domination how-to lines in that order, all inside the card. Screenshot `p4-briefing-domination`.
  - `p4-field-rule`: the field card reads `Domination to 300`; after `battlemode:banner` it reads `Banner: first to 3`.
  - `p4-hud-domination`: in a Domination round the ledger draws both scores and fills the tracks as before. Screenshot `p4-hud-domination`.
- [ ] **Step 3: Run suite `hud`; expect failures** (missing functions, no how-to lines on the card).
- [ ] **Step 4: Implement** the copy functions, `FeedEvents.mode`, the snapshot read, the ledger, the briefing lines and the field card line.
- [ ] **Step 5: Run suite `hud`; expect the five ids and `pass p4-hud-suite`.** The tester states what each screenshot shows. The Crimson overflow goes into the task report, for the copy.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{FeedLines,FeedEvents,BattleHud,CampaignClient,WarTableHome}.java test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: mode feed lines, the ledger from the snapshot, briefing how-to lines"
```

---

### Task 4: World pieces for Siege and the story (walkthrough gate)

Phase 3 is merged before this task.

**Executor:** Sonnet 5.5 max.

**Files:**
- Create (studio scripts, Phase 3 pipeline): `test-tools/resources/data/gravesandglory_studio/functions/ironhold/{gate_intact,ward}.mcfunction`, `.../kingsfield/mounds.mcfunction`
- Create (exports): `src/main/resources/data/gravesandglory/structures/ironhold/{gate_intact,ward}.nbt`, `.../kingsfield/mounds.nbt`
- Modify: `test-tools/resources/data/gravesandglory_studio/functions/camp/archive.mcfunction` and its export `structures/camp/archive.nbt`
- Modify: `sheets/design.json` (`pieces` rows for the new pieces; `landmarks` row `kf-mounds`; new sheet `modeSites` with its ward rows)
- Modify: `tools/preflight.py` (`modeSites`: ids unique; `mode` a `modes` id; `map` an index the mode hosts; `kind` one of `stage, gate, ward, base, station, walls, standoff, shrine, rise, stand`; `piece` a `pieces` id or `-`; rotation in {0,90,180,270})
- Modify: `test-tools/java/gg/gravesandglory/Walkability.java` (`reach`), `Phase4Checks.java` (suite `world`)
- Create: `docs/PHASE-4-GATES.md` (every Phase 4 grade, run and gate result lands here, one section per gate)

**Pieces:**
- `ironhold/gate_intact`: the `ironhold/gate` footprint (7 wide, 51 deep, 14 high) with the portcullis down the full height of the passage and the doors shut; no passage cell (world x -40..-34, z -5..5) is walkable.
- `ironhold/ward`: a wall in the curtains' stone, at least 4 high, with no gap a troop fits through. Its `modeSites` rows (columns `id, mode, map, kind, name, team, stage, x, y, z, radius, seconds, hp, piece, rotation`; `x, y, z, radius` are doubles; `name` and `piece` are `-` when unused; `team` and `stage` -1) are `siege-ward-n` and `siege-ward-s`, kind `ward`, mode `siege`, map 1, y 64.0, with the centre and rotation the builder measures. They place it from the gate wall's north end (world z -25) to the river bank and from its south end (z 25) to the keep's west wall (x -23). The spec places it twice; if one length cannot close both gaps, the south row uses a shorter copy `ironhold/ward_south`. If the flood test below finds another way in from the east bank (over the B deck, or through the keep's east land gate at world x 12..16), further ward rows close each opening; a shorter copy or an extra row is recorded in `PHASE-4-GATES.md` and the task report, since the spec names two.
- `kingsfield/mounds`: a ring of five to seven barrows of earth and stone, the first king's mound highest (its top at least 10 above the ground, with a path to it), and at the foot of one mound a small carved stone with one sign reading the copy's child's stone text. Row `kf-mounds` (map 4, rotation 0, elevation 64, `replaces -`, `encloses -`) sits beside the Kings Bridge (C) in one of the two quadrants beside it that hold no landmark, its footprint 6 or more from every capture ring and off every road.
- `camp/archive`: the Founders' Stone gains its own letters as wall signs on its face: the copy's "The Stone and the copy" Stone column, in capitals, wrapped per sign line.

**Interfaces:**
- Consumes: Phase 3 `StructureStudio` (`GRAVES_STUDIO_MODE=export`, `GRAVES_STUDIO_PIECES=<group>/<name>`), `Structures.place` and `template`, the `pieces` and `landmarks` preflight rules, suites `landmarks`, `routes` and `camp`.
- Produces: templates `ironhold/gate_intact`, `ironhold/ward` (and `ironhold/ward_south` if needed), `kingsfield/mounds`; landmark `kf-mounds` (Task 14's scene files read its centre from the sheet); sheet `modeSites` with record `Spec.ModeSitesRow` and its ward rows (Task 5 adds the rest).
- Produces (harness) `Walkability.reach(ServerWorld w,BlockPos from,BlockBox bounds)->Set<Long>`: a flood fill over dry walkable cells (solid, non-water ground with no collision shape at ground+1 and ground+2, steps of at most one block), keyed `BlockPos.asLong(x,0,z)`.

- [ ] **Step 1: Compile the merged base** and run Phase 3 suite `routes`; record the zero baseline.
- [ ] **Step 2: Write suite `world`:**
  - `p4-world-gate-intact`: `Structures.template(w,"ironhold/gate_intact")` has the size of `ironhold/gate`; placed at (-37,64,0) rotation 0, no cell at world x -40..-34, z -5..5 is walkable.
  - `p4-world-wards`: on map 1 with `gate_intact` and every `modeSites` row of kind `ward` placed, `reach` from (-283, ground, 0) over the whole arena finds no cell in the inner yard (x -33..-6, z -24..20) and none within 10 of the keep court (-8,34). With `ironhold/gate` back in place it reaches the keep court through the passage.
  - `p4-world-stone`: the archive's Stone carries signs whose joined text equals the copy's Stone column in capitals.
  - Screenshots `p4-world-gate-intact`, `p4-world-wards` (from (-80,80,0) looking at (-37,66,0)), `p4-world-mounds` (from the Kings Bridge looking at the highest mound), `p4-world-stone`.
- [ ] **Step 3: Run suite `world`; expect failures** (templates missing).
- [ ] **Step 4: Author the scripts, export them through the studio,** and add the `pieces` rows (sizes equal to the exports), `kf-mounds`, and `modeSites` with its rules and ward rows. Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Run suite `world` and Phase 3 suites `landmarks,routes,camp`** (`GRAVES_PHASE3_MAPS=1,4`). Expected: the four `p4-world` ids and `pass p4-world-suite`; every `p3-lm-kf-mounds-*` id; `p3-walk-map1` and `p3-walk-map4` at zero; the camp suite unchanged.
- [ ] **Step 6: Walkthrough gate.** A Sonnet 5.5 max tester walks each piece first-person (the shut gate from the west field and from the inner yard, both wards, the mounds and the child's stone, the Stone in the archive) and grades each on the rubric adapted to world pieces. Below 9 is a fix round. Record the grades in `docs/PHASE-4-GATES.md`.
- [ ] **Step 7: Commit.**
```bash
git add test-tools/resources/data/gravesandglory_studio/functions/ironhold test-tools/resources/data/gravesandglory_studio/functions/kingsfield test-tools/resources/data/gravesandglory_studio/functions/camp/archive.mcfunction src/main/resources/data/gravesandglory/structures/ironhold src/main/resources/data/gravesandglory/structures/kingsfield src/main/resources/data/gravesandglory/structures/camp/archive.nbt sheets/design.json tools/preflight.py src/main/java/gg/gravesandglory/Spec.java test-tools/java/gg/gravesandglory/{Walkability,Phase4Checks}.java docs/PHASE-4-GATES.md
git commit -m "Phase 4: shut gate, wards, the kings' mounds and the Stone's letters"
```

---

### Task 5: Siege on Ironhold

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{SiegeMode,GateTarget}.java`
- Modify: `Structures.java` (`placeSaving`, `restore`, `Saved`), `Modes.java` (register `siege`; `recover` restores saved pieces), `GravesAndGlory.java` (`GateTarget` in `Battle.structures`), `sheets/design.json` (the Siege `modeSites` rows), `Phase4Checks.java` (suite `siege`)

**Rows** (`modeSites`, mode `siege`, map 1; draft values from spec 5; `-` and -1 where unused):

| id | kind | name | team | stage | x | y | z | radius | seconds | hp | piece |
|---|---|---|---|---|---|---|---|---|---|---|---|
| siege-yard | stage | Outer Yard | 1 | 0 | -55.0 | 64.0 | 0.0 | 15.0 | 20 | 0 | - |
| siege-gate | gate | Iron Gate | 1 | 1 | -37.0 | 64.0 | 0.0 | 0.0 | 0 | 1600 | ironhold/gate_intact |
| siege-keep | stage | The Keep | 1 | 2 | -8.0 | 64.0 | 34.0 | 10.0 | 30 | 0 | - |
| siege-donjon | base | donjon | 1 | -1 | 12.0 | 65.0 | 34.0 | 0.0 | 0 | 0 | - |
| siege-tower-n | station | north tower | 1 | -1 | -38.5 | 77.0 | -8.5 | 0.0 | 0 | 0 | - |
| siege-tower-s | station | south tower | 1 | -1 | -38.5 | 77.0 | 8.5 | 0.0 | 0 | 0 | - |
| siege-wallwalk | station | wall-walk | 1 | -1 | -8.5 | 71.0 | 21.5 | 0.0 | 0 | 0 | - |
| siege-walls-gate | walls | - | 1 | -1 | -37.0 | 64.0 | 0.0 | 14.0 | 0 | 0 | - |
| siege-walls-keep | walls | - | 1 | -1 | 0.0 | 64.0 | 34.0 | 25.0 | 0 | 0 | - |
| siege-standoff | standoff | - | 0 | -1 | -62.0 | 64.0 | 0.0 | 0.0 | 0 | 0 | - |

Rotation is 0 in every row. The donjon row is the walkable cell of the keep's east court nearest (12,34), measured by the builder; the station rows are the gate towers' tops (feet 77) and the keep's north wall-walk (feet 71) from Phase 3's sight lines.

**Interfaces:**
- Consumes: Task 2 `BattleMode` and `Modes`; Task 4 templates and ward rows; Phase 2 `ArmySupplyBanner` (the structure-target pattern), counters C10 and C11, `Battle.structures`; Phase 3 `Structures.place`, `template`, `footprint`.
- Produces in `Structures`: `record Saved(String tag,Path file,BlockPos origin,Vec3i size)` with `JsonObject toJson()` and `static Saved fromJson(JsonObject)`; `placeSaving(ServerWorld w,String id,int cx,int y,int cz,int rotation,String tag)->Saved` saves the blocks of the footprint (`StructureTemplate.saveFromWorld`, no entities) to `<world save>/gravesandglory/mode-saved/<tag>.nbt`, then places the piece; `restore(ServerWorld w,Saved s)->boolean` places the saved blocks back at `origin` and deletes the file.
- Produces `GateTarget extends ArmorStandEntity implements ArmyMob`: `ArmySupplyBanner`'s pattern (tag `structure`, HP in a field, `damage` subtracting the resolved amount) with HP from the gate row, team 1, invisible and without gravity at the passage's west face (-40.5,65,0.5). Blows from team 1 change nothing. It joins `Battle.structures` when the portcullis drops and leaves at 0 HP or teardown.
- Produces `SiegeMode extends BattleMode`, registered as `siege`:
  - `static final int BLOW_CONTEST_TICKS = 60`; `int stage()` (0 yard, 1 gate, 2 keep, 3 taken); `long fellAt(int stage)` (ticks after `roundStarted` when that stage fell, -1 while it stands); `int gateHp()`.
  - `setup`: places every ward row with `placeSaving`, takes the map's flags down with `Arena.unflag`, builds `Capture`s for the yard, the gate and the keep (ids A, B and C, owner 1), and writes the marker with the ward saves.
  - `tick`: attackers alone on the current stage's ring advance it by Domination's capture rule for `seconds`; the keep advances only in stage 2. When the yard falls: `roundSeconds += siegeStageBonus`, feed `yardTaken` (point, team 0) and `portcullis(portcullisSeconds)` (notice), and the drop is due `portcullisSeconds*20` ticks later. At the drop: every unit and player inside the passage box (x -40..-34, z -5..5) moves to the nearer side (x -42.5 or -31.5, same z), `placeSaving` puts `gate_intact` over the open gate, the `GateTarget` spawns and the marker is rewritten. At 0 HP: `restore` brings the open gate back as the breach, `roundSeconds += siegeStageBonus`, feed `gateBreached` (point, team 0), stage 2. When the keep falls, `campaign.finish(0)`.
  - `fieldBase`: team 1 the donjon row; team 0 `Battle.base(0)` until the yard falls, the yard after it, the gate's east mouth (-31.5,65,0.5) after the breach. `fieldBarracks`: team 1 the donjon; team 0 the default. `lineup`: team 1 the yard centre.
  - `anchors`: team 1 the yard while it stands with no attacker within 20 of it, else the donjon; team 0 none until the yard falls, then the yard. `seals`: A, B and C, each the defenders' seal while standing, contested while contested and for `BLOW_CONTEST_TICKS` after each gate blow, the attackers' seal once fallen. `capturable`: false for every map point.
  - `routShare`: for team 1 while `stage()<2`, `ModeMath.routShare(houseShare,morale().wallRoutShare(),inside)`, inside meaning within a `walls` row's radius of its centre; otherwise `houseShare`.
  - `timeoutWinner` 1 (no draws); `ledger` `{stagesTaken, ModeMath.siegeStrength(...)}`; `max` `{3,100}`; `formats` `{"fraction","percent"}`; `count` stages taken; `howToSide` team 0 `attack`, team 1 `defend`; `snapshot` adds `"stage"`.
  - `botPlan` (spec 5's table; Easy uses no stations and sends its `siege` group with the infantry): attackers take the yard, then their `siege` group holds the standoff facing the gate while infantry attack the `GateTarget` and ranged hold at the yard, then everything attacks the keep. Defenders hold the yard with infantry and put ranged on the tower stations; when the portcullis warning sounds every defender group in the yard targets (-31.5,65,0.5); in stage 1 infantry wait there and move to the breach (-33.5,65,0.5) once `gateHp()<=breachShare*hp`; in stage 2 all hold the keep, ranged on the wall-walk.
  - `teardown`: cancels a due drop, discards the `GateTarget`, restores the gate and the wards, puts the flags back, unmarks.
- Produces in `Modes.recover`: every `saved` entry of the marker is restored before the flags.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `siege`** (each id starts a fresh practice Siege round on map 1 unless it says otherwise):
  - `p4-siege-setup`: both ward saves exist under `mode-saved/`; a cell at (-37,65,0) in the passage is walkable (the open gate); the flag blocks of every map point are air; `stage()==0`; the marker reads mode `siege` with two `saved` entries; STATE's ledger reads 0 and 100 and its clock 600.
  - `p4-siege-yard`: team 0 units alone on the yard ring: after 20 s `stage()==1`, `roundSeconds` grew by 180 and the feed got `yardTaken` then `portcullis(10)`; 200 ticks later no passage cell is walkable and `gateHp()==1600`.
  - `p4-siege-passage`: a team 0 knight at (-37,65,0) and a team 1 knight at (-35,65,2) when the drop fires end at x -42.5 and -31.5 (the nearer side, same z), alive and not inside a block.
  - `p4-siege-counters`: one resolved blow on the `GateTarget` from a team 0 ballista removes the resolved damage times C10's value read from the `counters` sheet (4.0), one from a knight times C11's (0.25); a team 1 blow leaves `gateHp()` unchanged. After each team 0 blow seal B reads contested for 60 ticks, then the defenders' seal.
  - `p4-siege-breach`: with `gateHp()` forced to 1, one blow: the passage is walkable again, `stage()==2`, `roundSeconds` grew by 180 again, the feed got `gateBreached`, the `GateTarget` is gone from `Battle.structures`, and `b.fieldBase(0)` is (-31.5,65,0.5).
  - `p4-siege-end`: attackers alone on the keep ring in stage 2 for 30 s finish the round for team 0, and the report JSON has `battleMode` `siege` and count 3. A round with `roundSeconds` forced to 30 and the yard standing finishes for team 1; so does one forced to a 0 to 0 time-out after the yard fell (no draws).
  - `p4-siege-morale`: a team 1 knight group inside the gate's walls ring routs at 14% strength and holds at 16%; outside every ring it routs at 24%; inside after the breach it routs at 24%; a fearless group (Phase 2's 0.12) inside holds at 13%.
  - `p4-siege-stations`: with the Normal tier, a team 1 Bone Archer (`bonebow`) sent to each station row reaches within 1.5 of it in at most 600 ticks; a miss fails and names the station. The fix for a miss is a stair inside the piece, made under the walkthrough gate and recorded in `docs/PHASE-4-GATES.md`.
  - `p4-siege-bots`: a 2 v 2 Normal bot round for 3600 ticks: before the yard falls attacker groups target the yard; in stage 1 the attackers' `siege` group targets the standoff and their infantry the `GateTarget`; defender ranged groups hold the station rows; at the warning every defender group in the yard targets (-31.5,65,0.5). The suite logs `[p4] siege-bots stage=<n> at=<tick>` at each fall.
  - `p4-siege-json`: the snapshot's `battleMode` has id `siege`, rule `Siege: yard, gate, keep`, `stage`, max `[3,100]`, format `["fraction","percent"]`, side `attack` for a team 0 viewer and `defend` for team 1, and both how-to lists from the sheet.
  - `p4-mode-teardown-mid`: three rounds. (a) `returnLobby` during the portcullis countdown: no drop fires in the next 300 ticks, the passage is walkable, the ward cells hold their saved blocks, the flags are up and the marker is gone. (b) `switchMap(0)` with the gate shut and the `GateTarget` alive: the same, and the target is discarded. (c) After (a), a Domination round on map 1 captures point A in its normal seconds.
  - `p4-siege-crash`: with the gate shut and both wards up, the suite replaces `b.mode` with a fresh `DominationMode` without `teardown` (the crash), ends the round, and calls `Modes.recover(b)`: the gate is open, the wards gone, the flags up, the marker and the saved files deleted.
  - Screenshots `p4-siege-gate-fire` (a ballista striking the shut gate), `p4-siege-breach` (the open gate with attackers in the passage), `p4-siege-ledger` (the ledger reading `1/3` and a percent).
- [ ] **Step 3: Run suite `siege`; expect failures** (`battlemode:siege` refused: no mode registered).
- [ ] **Step 4: Add the Siege `modeSites` rows.** Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement** `Structures.Saved`, `placeSaving` and `restore`, `GateTarget`, `SiegeMode`, the `recover` addition, and the registration.
- [ ] **Step 6: Run suites `siege,modes`, then Phase 3 suite `routes` with `GRAVES_PHASE3_MAPS=1`.** Expected: every id above and `pass p4-siege-suite`, `pass p4-modes-suite`, and `p3-walk-map1` at zero (the arena is as Phase 3 left it). The tester states what each screenshot shows.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{SiegeMode,GateTarget,Structures,Modes,GravesAndGlory,Spec}.java sheets/design.json test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: Siege on Ironhold: stages, the gate target, wards and bot plans"
```

---

### Task 6: Survival on Hollowwood

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/SurvivalMode.java`
- Modify: `sheets/design.json` (new `waves` and `waveGroups`; the Survival `modeSites` rows), `tools/preflight.py`, `BattleMode.java` (`scripted`, `seatsTeam`), `Campaign.java` (seating, `botDecide`, buying and income skip a scripted squad), `GravesAndGlory.java` (the lineup skips it), `Modes.java` (register), `Phase4Checks.java` (suite `survival`)

**Rows** (draft values, spec 6). `waves` (`id, set, wave, seconds, gold`): `moon-1` to `moon-7`, set `moon`, wave 1 to 7, seconds 90, 90, 100, 100, 110, 120, 150, gold 60, 60, 80, 80, 100, 100, 0.

`waveGroups` (`id, wave, troop, count, site`; `site` a `modeSites` id; at most 6 per group, at least two sites per wave, all four in wave 7):

| wave | groups (troop count site) |
|---|---|
| moon-1 | revenant 4 chapel; revenant 4 ford |
| moon-2 | revenant 4 chapel; revenant 4 wood; bonebow 2 chapel; bonebow 2 wood |
| moon-3 | revenant 5 ford; revenant 5 track; bonebow 3 ford; bonebow 3 track |
| moon-4 | revenant 5 chapel; revenant 5 wood; bonebow 3 chapel; bonebow 3 wood; reaper 3 ford |
| moon-5 | revenant 6 ford; revenant 6 track; bonebow 3 chapel; bonebow 3 wood; stalker 2 chapel; stalker 2 wood |
| moon-6 | revenant 6 chapel; revenant 6 wood; bonebow 4 ford; bonebow 4 track; reaper 2 chapel; reaper 2 wood; hexer 1 track |
| moon-7 | revenant 4 at each site; bonebow 2 at each site; reaper 2 chapel; reaper 2 ford; stalker 2 wood; stalker 2 track; hexer 1 chapel; hexer 1 track |

Group ids are `<wave>-<n>` in table order; the sites are `surv-rise-<name>` below.

`modeSites` (mode `survival`, map 3; rotation 0, piece `-`; positions are drafts the builder moves onto walkable ground 50 to 90 from the shrine, as `p4-surv-sites` checks):

| id | kind | name | team | x | y | z | radius | seconds |
|---|---|---|---|---|---|---|---|---|
| surv-shrine | shrine | the shrine | 0 | 76.0 | 64.0 | 127.0 | 10.0 | 20 |
| surv-court | base | the court | 0 | 76.0 | 65.0 | 127.0 | 0.0 | 0 |
| surv-rise-chapel | rise | the chapel | 1 | 70.0 | 64.0 | 55.0 | 4.0 | 0 |
| surv-rise-ford | rise | the ford | 1 | 17.0 | 64.0 | 145.0 | 4.0 | 0 |
| surv-rise-wood | rise | the deep wood | 1 | 150.0 | 64.0 | 120.0 | 4.0 | 0 |
| surv-rise-track | rise | the track | 1 | 80.0 | 64.0 | 200.0 | 4.0 | 0 |

The site names are the copy's (Modes, Survival). `stage` -1 and `hp` 0 in every row; the court row is the court's walkable cell nearest the shrine centre, as measured.

**Interfaces:**
- Consumes: Task 1 `waveCount`; Task 2 `BattleMode`, `Modes`, `Arena.unflag`; Task 3 `FeedLines.deadRise`, `waveAtRest`, `shrineTaken`, `FeedEvents.mode`; `Battle.spawn(Squad s,Spec.TroopsRow spec,int slot,Vec3d pos)->Unit`; Phase 2's Rise Again (`Squad.traits` keyed `graveborn`); `Phase2Checks.tickCost(Battle,List<String>)`.
- Produces in `BattleMode`: `boolean scripted(Squad s)` (default false): a squad the mode drives alone, which `botDecide`, buying, income, the lineup and the lobby rows skip; `boolean seatsTeam(int team)` (default true): `prepareRound` seats no banner on a team it refuses.
- Produces `SurvivalMode extends BattleMode`, registered as `survival`:
  - `static final UUID DIRECTOR = UUID.nameUUIDFromBytes("gravesandglory:director".getBytes(StandardCharsets.UTF_8))`; `Squad director()` (team 1, `bot`, no commander, no banner, `traits.put("graveborn",tier.waveTraitTier())` when above 0); `scripted` is true for it alone and `seatsTeam(1)` false.
  - `String waveSet` (default `moon`; Task 11 sets it from the battle row); `int wave()` (0 before the first rising), `int waves()`, `int standing()` (living director units), `int risenThisWave()`, `int cleared()`.
  - `setup`: saves the world time into the marker and sets it to 18000; takes down every map point's flag; builds the shrine `Capture` (id A, owner 0, the row's radius and seconds); raises team 0's banner pole at the court with `Arena.flag`; wave 1's warnings start at once, so it rises `riseWarning` s into the round.
  - `tick`: `riseWarning*20` ticks before a group rises, soul fire (`ParticleTypes.SOUL_FIRE_FLAME`) and `SoundEvents.PARTICLE_SOUL_ESCAPE` at its site and, once per site per wave, `FeedEvents.mode(b,"point",1,FeedLines.deadRise(site.name()))`. A group rises as `ModeMath.waveCount(count,banners,survivalPerBanner,tier.waveScale())` units of its troop scattered within the site radius, never more than `maxDeadAlive` alive (the rest queue and rise at their site as others fall); the director's group assignments attack-move to the shrine. A wave is cleared when none of its units lives or waits: each team 0 squad `earn`s the wave gold and the feed gets `waveAtRest(n,gold)` (point, team 0). The next wave's warnings start when the timer leaves `riseWarning` s, cleared or not. The shrine: the dead alone on its ring take it in its seconds, with `shrineTaken()` (point, team 1) when a take begins; taken is `finish(1)`. The last wave cleared, or its timer out with the shrine held, is `finish(0)`.
  - `income` is 0 (both scales 0); kill gold stays. `fieldBase` and `fieldBarracks` for team 0 the court; `anchors` none; `seals` the shrine alone; `capturable` false for map points; `holds` true for the director; `clock` the seconds to the next wave (in the last, its time left); `ledger {wave(),standing()}`, `max {waves(),risenThisWave()}`, `formats {"fraction","plain"}`; `count` cleared waves; `ruleN` `waves()`; `snapshot` adds `wave`, `waves` and `nextIn`.
  - `teardown`: discards every director unit and queued group, puts the world time back, removes the pole with `Arena.unflag`, puts the map flags back, unmarks.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `survival`** (practice Survival rounds on map 3, one human banner unless stated):
  - `p4-surv-sheet`: per `moon` wave the `waveGroups` totals per troop equal spec 6's table (1: revenant 8; 2: revenant 8, bonebow 4; 3: 10, 6; 4: 10, 6, reaper 3; 5: 12, 6, stalker 4; 6: 12, 8, reaper 4, hexer 1; 7: 16, 8, reaper 4, stalker 4, hexer 2); no group holds more than 6; every wave uses two or more sites and wave 7 all four.
  - `p4-surv-sites`: `Walkability.reach` from the court reaches every rise row's cell, and each lies 50 to 90 from the shrine centre.
  - `p4-surv-rise`: at setup the world time reads 18000 and the round's team 1 holds no seated banner; 100 ticks before wave 1 rises, soul fire burns at the chapel and the ford and the feed holds `Dead rise at the chapel` and `Dead rise at the ford`; the risen units stand within 4 of their site. Screenshot `p4-surv-rising`.
  - `p4-surv-counts`: wave 1 rises 8 Risen at one Normal banner, 20 at four Normal banners, 6 at one Easy and 10 at one Hard (per group, rounded up). Wave 7 at four Hard banners never has more than 60 dead alive; killing 10 lets 10 more rise within 40 ticks.
  - `p4-surv-gold`: with no kills, team 0 gold is unchanged over 400 ticks; the harness kills wave 1, and every team 0 squad gains 60 and the feed holds `Wave 1 at rest, +60 gold`; wave 2 rises 90 s after wave 1 did, whether wave 1 was cleared or not.
  - `p4-surv-norout`: a director group at 5% strength holds 400 ticks with `routedAt` empty.
  - `p4-surv-end`: the dead alone on the ring take the shrine in 20 s and the round finishes for team 1, with `The shrine is being taken` in the feed when the take began; a defender stepping onto the ring contests it and the progress stops. The last wave killed finishes for team 0; a round with wave 7's timer run out and the shrine held finishes for team 0. Each report has `battleMode` `survival` and the cleared count.
  - `p4-surv-barracks`: the team 0 pole stands at the court; the player at the court opens the barracks (the `accessRadius` test at `fieldBarracks(0)`), buys a troop and it deploys at the court.
  - `p4-surv-teardown`: after `returnLobby` mid-wave the world time is what it was before setup, the pole is gone, the map flags are up, no director unit lives and the marker is gone.
  - `p4-surv-tickcost`: `Phase2Checks.tickCost` over wave 7 at four banners, beside a 4 v 4 Domination round on map 3, is logged as `[p4] survival-tickcost survival=<ms> domination=<ms>`; the id passes when both measured (the numbers go to the gate).
  - `p4-surv-json`: the snapshot's `battleMode` has rule `Survival: 7 waves`, `wave`, `waves` 7, `nextIn`, max `[7,<risen>]` and format `["fraction","plain"]`. Screenshot `p4-surv-ledger` (the ledger reading `3/7` and the standing count, at night).
- [ ] **Step 3: Run suite `survival`; expect failures** (`battlemode:survival` refused).
- [ ] **Step 4: Add `waves`, `waveGroups` and the Survival `modeSites` rows, with preflight rules:** `waveGroups.wave` is a `waves` id, `troop` a troop id, `site` a `modeSites` row of kind `rise`, `count` 1 to 6; each `waves` set numbers its waves from 1 without gaps. Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement** `scripted`, `seatsTeam` and their call sites, and `SurvivalMode`; register it.
- [ ] **Step 6: Run suites `survival,modes`.** Expected: every id above, `pass p4-survival-suite` and `pass p4-modes-suite`. The tester states what each screenshot shows.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{SurvivalMode,BattleMode,Campaign,GravesAndGlory,Modes,Spec}.java sheets/design.json tools/preflight.py test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: Survival on Hollowwood: waves, rise sites and the shrine"
```

---

### Task 7: Banner on Rivergate

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/BannerMode.java`
- Modify: `BattleMode.java` (`exempt`), `Tactics.java` and `Combat.java` (skip an exempt unit's orders and attacks), `Morale.java` (a routing group leaves an exempt unit alone), `GravesAndGlory.java` (`AttackEntityCallback` refuses a carrying commander), `CampaignClient.java` (no sprint while the snapshot says `carrying`), `MapScreen.java` (the carried emblem), `Modes.java` (register), `sheets/design.json` (two `stand` rows), `Phase4Checks.java` (suite `banner`)

**Rows** (`modeSites`, mode `banner`, map 0, kind `stand`; the centres of points A and C as `CampaignMaps.select` places them; the builder may move Dusk's into the shrine court only on a Banner sim seat bias, Task 9):

| id | name | team | x | y | z | radius |
|---|---|---|---|---|---|---|
| banner-stand-dawn | Mill Hamlet | 0 | -39.0 | 64.0 | -145.0 | 3.0 |
| banner-stand-dusk | River Shrine | 1 | 39.0 | 64.0 | 145.0 | 3.0 |

**Interfaces:**
- Consumes: Task 1 `ModeMath.roles`; Task 2 `BattleMode`, `Modes`, `Arena.flag` and `unflag`; Task 3 `FeedLines.bannerTaken`, `bannerHome`, `bannerLying`, `bannerScore`; `Tactics.route`; `modeRules` `carrierSpeed`, `pickupRadius`, `standRadius`, `dropReturnSeconds`; `botCommand.chaseRange`, `raidShare`.
- Produces in `BattleMode`: `boolean exempt(Battle b,Unit u)` (default false): `Tactics` gives the unit no group order, `Combat` starts no attack for it, and a routing group leaves it alone.
- Produces `BannerMode extends BattleMode`, registered as `banner`:
  - `enum State { HOME, CARRIED, DOWN }`; `final class Flag { int team; State state; Vec3d at; Unit carrier; UUID player; long downAt; ItemStack savedHead; boolean savedGlow; }`; `Flag flag(int team)`; `int captures(int team)`.
  - `setup`: takes down the flags of A and C, places each team's standing banner (`Blocks.CYAN_BANNER` for Dawn, `Blocks.PURPLE_BANNER` for Dusk) at its stand, marks.
  - `tick`: an enemy troop or commander within `pickupRadius` of a flag that is HOME or DOWN takes it: CARRIED, the banner block removed, the carrier's head slot saved and given the banner item, glowing on, and a movement speed modifier (`UUID CARRIER_SPEED`, `MULTIPLY_TOTAL`, `carrierSpeed-1`); feed `bannerTaken(teamShort(carrier),teamShort(owner))` (point, carrier team). A troop carrier walks to its own stand by `Tactics.route`; a commander carrier goes where the player goes. A carrier within `standRadius` of its own stand scores: `captures+1`, the flag HOME, feed `bannerScore(team,mine,theirs)` (point); 3 captures `finish(team)`. A carrier's death drops the flag at its feet as a standing banner block (DOWN, `downAt`), feed `bannerLying(owner)` (notice). The owner's troop or commander within `pickupRadius` of a DOWN flag sends it home at once, as does `dropReturnSeconds` untouched; feed `bannerHome(owner)` (notice).
  - `exempt`: true for a troop carrier. `holds`: false (only the carrier is guarded, by `exempt`). `routShare` default; morale row `domination`.
  - `anchors`: B only, as Domination's anchors give it; `seals`: the Dawn stand, B, the Dusk stand, where a stand reads its owner's seal while HOME, the carrier team's while CARRIED and contested while DOWN; `capturable`: B alone.
  - `ledger {captures(0),captures(1)}`, `max {3,3}`, `formats {"plain","plain"}`, `count` the winner's captures; `timeoutWinner` by `score`.
  - `howToSide`: `attack` for a human (humans raid first) and for a bot raider, `defend` for a guard or holder.
  - `snapshot` adds `banners`: `[{"team","state","x","z","carrierTeam"}]`, and `carrying` true when the viewer's commander carries.
  - `botPlan`: roles from `ModeMath.roles(banners,humans,tier.raidShare())` per team, recomputed at each decision. A raider targets the enemy stand, or the dropped enemy flag when DOWN; a guard holds its own stand; a holder holds B; `split` sends infantry to raid and ranged to guard. When a team-mate carries, every bot banner within `chaseRange` of the carrier targets the carrier; when its own flag is CARRIED or DOWN, every bot banner within `chaseRange` targets that carrier or that flag.
  - `teardown`: every carrier's head slot and glow back and the speed modifier removed, every banner block (stands and dropped) removed, A and C flags back, unmarks.
- Produces (client): `MapScreen` draws, for each `banners` entry in state CARRIED, the carrier team's emblem at `x,z`; `CampaignClient` cancels sprint each tick while `carrying`.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `banner`** (practice Banner rounds on map 0):
  - `p4-banner-setup`: a cyan banner block stands at the Dawn stand and a purple one at the Dusk stand; the flag blocks of A and C are air and B's stand; B captures in its seconds and A and C never; STATE's ledger reads 0 and 0 and its clock 720.
  - `p4-banner-pickup-troop`: a team 1 knight moved within 1.5 of the Dawn stand: the flag is CARRIED, its head slot holds a cyan banner, it glows, its movement speed attribute is 0.9 of its base, the rest of its group keeps its orders, it lands no hit in 200 ticks beside an enemy, its distance to the Dusk stand falls over those ticks, and the feed holds `Dusk has Dawn's banner`. Screenshot `p4-banner-carry`.
  - `p4-banner-pickup-commander`: the player walks onto the Dusk stand: attacking an enemy troop leaves its health unchanged, sprint held 40 ticks never sets `isSprinting()`, and the snapshot's `carrying` is true.
  - `p4-banner-norout`: the carrier's group forced to 10% strength routs; the carrier keeps walking to its stand.
  - `p4-banner-drop`: the carrier killed at a point P leaves a standing banner block at P (DOWN) and the feed holds `Dawn's banner is down`; an enemy touch picks it up again; after a second drop the owner's touch sends it home at once with `Dawn's banner is home`; a third drop left alone is home after 20 s. Screenshot `p4-banner-down`.
  - `p4-banner-score`: a carrier within 3 of its own stand scores: captures 1, the flag home, `Dusk scores, 1 to 0`. With both flags carried, each carrier still scores at its own stand.
  - `p4-banner-end`: 3 captures finish for that team; a time-out at 2 to 1 goes to the leader, and at 1 to 1 it is a draw.
  - `p4-banner-bots`: a 2 v 2 Normal bot round: each team's roles are `raider, guard`; within 2400 ticks a bot raider picks up the enemy flag and carries it 40 blocks or more; while its own flag is carried, a bot banner within 80 of the carrier targets within 4 of it. The suite logs `[p4] banner-bots pickup=<tick> carried=<blocks>`.
  - `p4-banner-map`: while a flag is carried the snapshot's `banners` entry reads CARRIED at the carrier's x and z, and `MapScreen` draws the carrier team's emblem there. Screenshot `p4-banner-map`.
  - `p4-banner-teardown-head`: `returnLobby` while a knight carries, and `switchMap(1)` while the player carries: the player's head slot holds what it held before the pickup, the player does not glow, no `CARRIER_SPEED` modifier remains, no banner block remains at either stand or any drop, A's and C's flags stand, and a following Domination round on map 0 captures A in its normal seconds.
  - `p4-banner-json`: the snapshot's `battleMode` has rule `Banner: first to 3`, max `[3,3]`, format `["plain","plain"]`, side `attack` for the human viewer. Screenshot `p4-banner-ledger`.
- [ ] **Step 3: Run suite `banner`; expect failures** (`battlemode:banner` refused).
- [ ] **Step 4: Add the two `stand` rows.** Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement** `exempt` and its three call sites, `BannerMode`, the attack refusal, the client sprint stop and the map emblem; register the mode.
- [ ] **Step 6: Run suites `banner,modes`.** Expected: every id above, `pass p4-banner-suite` and `pass p4-modes-suite`. The tester states what each screenshot shows.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{BannerMode,BattleMode,Tactics,Combat,Morale,GravesAndGlory,CampaignClient,MapScreen,Modes,Spec}.java sheets/design.json test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: Banner on Rivergate: stands, carriers, drops and raider roles"
```

---

### Task 8: The Skirmish and Practice picker

**Executor:** Sonnet 5.5 max.

**Files:**
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (lobby settings `enemyHouse` and `siegeSide`; commands `enemy:`, `side:`, `skirmish:start`; solo commanders may send `map:` and `mode:`; seating by mode; `botBanner` by enemy House)
- Modify: `WarTablePages.java` (the field page), `WarTableHome.java` (the size line; the Skirmish Ride), `CampaignClient.java` (`enemyHouse`, `siegeSide` from the snapshot)
- Modify: `Phase4Checks.java` (suite `skirmish`)

**Interfaces:**
- Consumes: Task 1 `ModeMath.sizeLine`; Task 2 `battlemode:<id>`, `Modes.row`, `Modes.hosts`; Task 6 `seatsTeam`; UI Task 7 `WarTablePages.open("field")` and its `table/sheet-field`; `Campaign.soloRefusal`, `soloStart`, `botBanner`, `fieldableBotBanners`.
- Produces in `Campaign`:
  - Lobby settings under the `difficulty:` rule (anyone outside a custom lobby, only the leader inside one, locked while a battle runs): `String enemyHouse="any"` set by `enemy:<house id>`, `enemy:mixed` or `enemy:any`; `String siegeSide="attack"` set by `side:attack` or `side:defend`, refused with no change unless `battleMode` is `siege`.
  - `map:<n>` and `mode:<1..4>` are also accepted from a commander for whom `soloRefusal` is empty and who is in no custom lobby, so the picker works solo. `switchMap` sets `map` at once, so a `map:` followed by `battlemode:` in the same tick is accepted.
  - `skirmish:start`: behind `soloRefusal` (the rule line "Skirmish is solo here. Use a private room to play with your party.", an engineering line reported to the copy), a solo round with `practice=false` at the lobby's mode, map, difficulty, enemy, size and side. It pays normal rewards (completion 80 and victory 40 after 180 s, and Phase 2's shards and renown), and the barracks sells only owned troops.
  - `camp:practice` keeps practice rules (size 1, any troop, practice gold) and now plays the lobby's mode, map, difficulty, enemy and side.
  - Seating: Siege with `siegeSide` `defend` seats the human on team 1; Survival seats every human on team 0 with `size-1` allied bots and no team 1 banner; the other modes seat as today.
  - `botBanner(s)` for an enemy-side bot: `enemyHouse` names a House, so the row `<house>_<tier>`; `mixed` picks a `mix_*_<tier>` row by lane; `any` keeps today's pick. A House row that is not fieldable falls back to today's pick.
  - The snapshot gains `enemyHouse` and `siegeSide`.
- Produces in the field page (inside `table/sheet-field`, rows of `btn-vellum-*` buttons with `heading` labels, the chosen button pressed):
  - Row 1, the mode: one button per registered `modes` row, labelled with `modes.name`. It sends `map:<first map of the mode>` when the current map is not hosted, then `battlemode:<id>`.
  - Row 2, the maps the mode hosts: Vote as today on the war gates, else `map:<i>`.
  - Row 3, difficulty as today.
  - Row 4, the enemy: the five House names (`RosterMath.houseName`) and "Mixed", sending `enemy:<id>`.
  - Row 5, the size: 1 to 4, sending `mode:<n>`. In Survival it reads 0 to 3 allies and sends `mode:<allies+1>`.
  - Row 6, only in Siege: "Attack" and "Defend", sending `side:<id>`.
- Produces in `WarTableHome`: the field card's second line is `ModeMath.sizeLine(battleMode id,n,tier,siegeSide)`, where Survival's n counts the human plus allies. With Skirmish chosen and no party or room, Ride sends `skirmish:start`; with a party or room it sends `camp:gates` as before. The label stays "Ride to the war gates".

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `skirmish`:**
  - `p4-skirmish-picker`: on the field page each mode button makes the snapshot's `battleMode.id` that mode, moving the map to 1 for Siege, 3 for Survival and 0 for Banner. The map row lists only the mode's maps, and the side row shows only for Siege. At (960,540,1), (1280,720,3) and (854,480,2) every box lies inside the canvas and no two overlap. Screenshot `p4-field-picker` (Siege chosen).
  - `p4-skirmish-size-line`: the home field card reads `2 v 2, Normal bots` for Domination at size 2, `2 banners, Normal waves` for Survival with one ally, and `2 v 2, attack, Normal bots` for Siege.
  - `p4-skirmish-start-domination`, `-siege`, `-survival`, `-banner`: `skirmish:start` starts a round with `practice` false, that mode and its map, team sizes by the mode's seating, and bots at the chosen tier. A forced win after 181 s pays 120 gold (completion 80 and victory 40) plus what `finish` pays outside practice; in Survival the win is the held shrine.
  - `p4-skirmish-enemy`: `enemy:tidecallers` at Normal gives every enemy-side bot the `tidecallers_normal` slots; `enemy:mixed` gives a `mix_*_normal` row; `enemy:any` keeps today's pick.
  - `p4-skirmish-defend`: Siege with `side:defend` seats the player on team 1, the snapshot side reads `defend`, and the briefing draws the `howToDefend` lines. `side:defend` under Domination is refused and `siegeSide` stays `attack`.
  - `p4-skirmish-own-banner`: in a skirmish round, buying a troop the profile does not own is refused with Phase 2's notice.
  - `p4-skirmish-practice`: Banner chosen, `camp:practice` starts a Banner round on map 0 with `practice` true, size 1 and practice gold.
  - `p4-skirmish-room`: a private room led by the test player with `battlemode:survival` on map 3 starts a Survival round with the player on team 0.
- [ ] **Step 3: Run suite `skirmish`; expect failures** (unknown commands, no mode row).
- [ ] **Step 4: Implement** the settings, the commands, the seating, the bot banner pick, the field page rows and the home's size line and Ride.
- [ ] **Step 5: Run suites `skirmish,hud,modes` and UI suite `ui-pages`** (`GRAVES_PHASE1_CHECKS=ui-pages`). Expected: every id above, `pass p4-skirmish-suite`, and the UI ids as before (`p1-ui-page-field` still sets the difficulty). The tester states what the screenshot shows.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{Campaign,WarTablePages,WarTableHome,CampaignClient}.java test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: the Skirmish and Practice picker: mode, map, enemy, size and side"
```

---

### Task 9: Mode bot runs and the mode gate

**Executor:** Sonnet 5.5 max.

**Files:**
- Modify: `test-tools/java/gg/gravesandglory/BotSimulation.java` (`simMode`; new CSV columns), `build.gradle` (the sim property list gains `simMode`)
- Create: `tools/mode_gate.py`, `tools/test_mode_gate.py` (unittest, fixture CSVs written by the test)
- Modify: `docs/PHASE-4-GATES.md` (a "Mode runs" section)

**Interfaces:**
- Consumes: Tasks 5 to 7 (`SiegeMode.stage()`, `gateHp()`; `SurvivalMode.wave()`, `cleared()`; `BannerMode.captures(int)`, `Flag`); `Campaign.battleMode`; `tools/balance_gate.py`'s CSV reading and exit codes.
- Produces: `-PsimMode=<id>` (system property `gravesandglory.simMode`, default `domination`). Each match sets `Campaign.battleMode` before `startRound`; `-PsimMaps` must list only maps the mode hosts, else the run throws `IllegalArgumentException` naming the map. Survival seats `simSize` banners on team 0 (default 1 here) and none on team 1. `results.csv` appends, after `banner1`: `battleMode, stage, wave, captures0, captures1, gateBlows, carrierStill` (Siege: stages taken and team 0 blows on the gate; Survival: the wave reached; Banner: each side's captures and the longest time in seconds any carrier stood within 1 block of one spot; 0 where a column does not apply).
- Produces `python tools/mode_gate.py <results.csv> <mode>`: prints one line `mode_gate <mode> <stats> dawn=<P>% pass|fail` and exits 0 pass, 1 fail, 2 unusable input (balance_gate's convention). Aborted rounds are left out and counted as ` aborted=N`. The rules (spec 13, draft values): `siege` passes when attackers win 3 to 7 of 10 and `gateBlows>0` in every match; `survival` when `wave>=4` in at least 8 of 10 and team 0 wins 2 to 8 of 10; `banner` when `captures0+captures1>=1` in at least 8 of 10 and `carrierStill<30` in every match. `dawn` is team 0's share of decided rounds, logged for the seat bias.

- [ ] **Step 1: Write `tools/test_mode_gate.py`:** `test_siege_pass` (10 rows, 5 attacker wins, every `gateBlows` 1 or more: exit 0), `test_siege_untouched_gate` (one row with `gateBlows` 0: exit 1), `test_survival_bounds` (9 wins: exit 1; 8 wins with 8 rows at wave 4 or more: exit 0), `test_banner_still_carrier` (one row with `carrierStill` 30: exit 1), `test_aborted_left_out` (an aborted row is not counted and the line ends ` aborted=1`), `test_bad_input` (a missing column: exit 2).
- [ ] **Step 2: Run** `python -m unittest tools/test_mode_gate.py -v`; **expect FAIL** (no module `mode_gate`).
- [ ] **Step 3: Implement `tools/mode_gate.py`** and the `BotSimulation` and `build.gradle` changes.
- [ ] **Step 4: Run the unit test; expect `OK`.** Compile both source sets.
- [ ] **Step 5: Run the three mode runs** (Bot runs recipe, 10 matches each, Normal against Normal on Phase 2 `botBanners` rows, each mode with the Houses its march battle sets against each other):
  - Siege: `-PsimMode=siege -PsimMaps=1 -PsimRoundSeconds=600 -PsimBanner0=ironclad_normal -PsimBanner1=graveborn_normal`;
  - Survival: `-PsimMode=survival -PsimMaps=3 -PsimSize=1 -PsimBanner0=gilded_normal`;
  - Banner: `-PsimMode=banner -PsimMaps=0 -PsimRoundSeconds=720 -PsimBanner0=gilded_normal -PsimBanner1=tidecallers_normal`.

  Then `python tools/mode_gate.py <root>/botsim/results.csv <mode>` for each. Expected: three `pass` lines. A fail is tuned only through the draft values the spec names for that mode, one change at a time and logged in `PHASE-4-GATES.md`; a Banner `dawn` share outside 40 to 60 is the trigger for moving the Dusk stand into the shrine court (Task 7's row), rerun after the move.
- [ ] **Step 6: Append the three `[botsim]` summaries and gate lines to `docs/PHASE-4-GATES.md`, then commit** (with `sheets/design.json` and `Spec.java` when a draft value was tuned).
```bash
git add test-tools/java/gg/gravesandglory/BotSimulation.java build.gradle tools/mode_gate.py tools/test_mode_gate.py docs/PHASE-4-GATES.md
git commit -m "Phase 4: mode bot runs and the mode gate"
```

---

### Task 10: The march data, MarchMath and profile schema 3

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{MarchMath,StoryState}.java` (pure), `test-tools/standalone/MarchMathCheck.java`
- Modify: `sheets/design.json` (`campaignBattles` rebuilt; new `bonusObjectives` and `chapters`; `botBanners` gains seven rows), `tools/preflight.py`
- Modify: `Profile.java` (schema 3), `ProfileMigration.java` (2 to 3), `test-tools/standalone/ProfileMigrationCheck.java` (its Test runs line already lists `StoryState` and `MarchMath`), `Campaign.java` (readers of the removed `size`; `forClient`)

**Rows.** `campaignBattles` columns, in order: `id, chapter, order, name, map, mode, house, allyBanners, enemyBanners, difficulty, side, waveSet, bonus, talkOrder, lessons, briefing, reason, victory, defeat` (`size` goes). Map, mode, House, banners, difficulty, side and wave set are spec 11's table; `name`, `briefing`, `reason`, `victory` and `defeat` are transcribed from the copy's "The six battles"; `lessons` is true on `march-1` only; `bonus` is `b<n>_<kind>` below; `talkOrder` is the copy's reaction-line order: `maud`; `maud`; `wren,maud`; `maud,dunmar,wren`; `dunmar,wren,maud`; `ashka,maud,dunmar,wren`.

`bonusObjectives` (`id, kind, value, target, shards, gold, text, tag`; `text` and `tag` transcribed from the copy, the bonus line and the bonus tag):

| id | kind | value | target |
|---|---|---|---|
| b1_hold_all | hold_all | 0 | none |
| b2_lose_at_most | lose_at_most | 8 | none |
| b3_stage_by | stage_by | 360 | gate |
| b4_carry_self | carry_self | 0 | none |
| b5_win_within | win_within | 600 | none |
| b6_kill_unit | kill_unit | 0 | gb_hollowking |

`shards` 5 and `gold` 40 (draft value) in every row; the shards go to the battle row's `house`.

`chapters` (`id, name, pact, epilogue, pactText, blurb`): id 1, name `The Road North`, pact true; `epilogue`, `pactText` (the copy's Pact card title, body and note joined with ` | `; the copy calls the column `writText`) and `blurb` (the camp guide blurb) transcribed from the copy.

`botBanners` (`id, house, difficulty, slots`): `march1_turncoats`, gilded, easy: 10 `knight`, 6 `archer`, 4 `empty`. The march run's seat banners (draft values, from spec 10's "By then" lines; house gilded, difficulty normal): `march_by1` and `march_by2` the starter banner (12 `knight`, 8 `archer`); `march_by3` knight 8, archer 4, revenant 4, bonebow 4; `march_by4` knight 6, archer 4, revenant 4, ic_hearthguard 3, ic_arbalist 3; `march_by5` knight 6, archer 2, revenant 4, ic_hearthguard 3, ic_arbalist 3, tc_raider 2; `march_by6` sentinel 1, knight 5, archer 2, revenant 4, ic_hearthguard 3, ic_arbalist 3, wm_hound 2.

**Interfaces:**
- Produces `MarchMath` (pure, all `static`; `rows` is `Spec.CAMPAIGNBATTLES`):
  - `int cleared(Set<String> won,Spec.CampaignBattlesRow[] rows)`: the highest chapter 1 order whose every lower order is won, 0 for none.
  - `Spec.CampaignBattlesRow lockedBy(Spec.CampaignBattlesRow row,Set<String> won,Spec.CampaignBattlesRow[] rows)`: the lowest-order unwon row of the same chapter with a lower order, or null when the row is open.
  - `boolean chapterDone(int chapter,Set<String> won,Spec.CampaignBattlesRow[] rows)`; `Spec.CampaignBattlesRow nextUnwon(Set<String> won,Spec.CampaignBattlesRow[] rows)` (null when all are won).
  - `Spec.CampaignBattlesRow ride(Set<String> won,String lastChosen,Spec.CampaignBattlesRow[] rows)`: `nextUnwon`, or once all are won the row `lastChosen` names, else the last row.
  - `int[] teamSizes(Spec.CampaignBattlesRow row)`: `{1+allies, enemies}` by the banner lists (`none` is empty); Survival gives `{1,0}`.
  - `record Settled(boolean firstWin,int cleared,boolean bonusPaid,int bonusShards,int bonusGold,String bonusHouse,boolean chapterDone,boolean pactGranted)`.
  - `Settled settle(Spec.CampaignBattlesRow row,Spec.BonusObjectivesRow bonus,boolean won,boolean bonusKept,Set<String> battlesWon,Set<String> bonusClaimed,Set<Integer> pactChapters,Spec.CampaignBattlesRow[] rows)`: spec 12 steps 1 to 3, adding to the three sets it is given. A loss changes nothing; a bonus pays only when `won && bonusKept` and its id is unclaimed; the Pact is granted when the chapter is done and `pactChapters` lacks it. The caller copies the sets first and puts the copies back when the save fails.
- Produces `StoryState` (pure; saved as the profile's `story`): `Set<String> read` (scene ids whose first reading finished); `Map<String,String> wounded`, `Map<String,String> resting` (companion to the battle id whose finish heals it, `next` when no battle was unwon); `List<String> shrineHeard`, `int shrineDue`; `Set<String> woundedHeard`, `pellSaid`, `recruitHeard`, `rollHeard`; `Map<String,String> choices` (choice id to `warm` or `plain`); `List<String> pending` (scene ids due on the next arrival); `Facts last` with `record Facts(String battle,boolean won,List<String> fell,boolean routed,String bonus,int losses,int count,int bannerSize,boolean killed)` (`bonus` is `kept`, `missed` or `none`; `killed` is the `kill_unit` target's death by the player's squad); `int warm()`; `void sanitize()` (nulls become empty).
- Produces in `Profile`: `int schema=3`; `Set<String> battlesWon`, `Set<String> bonusClaimed`; `StoryState story`. `sanitize` sets schema 3, repairs nulls, drops ids no `campaignBattles` or `bonusObjectives` row knows, calls `story.sanitize()`, and derives `campaignCleared = MarchMath.cleared(battlesWon,Spec.CAMPAIGNBATTLES)`. The Pact count (`writs` in the Phase 2 code, `pacts` in the Houses spec 6.2) and `writChapters` keep the merged code's names.
- Produces in `ProfileMigration`: `static Profile v2toV3(Profile p,Spec.CampaignBattlesRow[] rows)`: `battlesWon` gets chapter 1 rows with `order <= campaignCleared`; `bonusClaimed` empty; `story.read` gets the before-scene of each won battle (`c1_b<n>_before` and, for `march-6`, `c1_king_before`); the Pact count and `writChapters` untouched. `upgradeFile` reads the file's `schema` first: schema 1 runs today's step then this one; schema 2 runs this one, keeping `<uuid>.v2.json` once beside the v1 backup; `sanitize` runs last.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write `MarchMathCheck`** (rows from `Spec.CAMPAIGNBATTLES` and `Spec.BONUSOBJECTIVES`):
```java
check(cleared(Set.of(),R)==0 && cleared(Set.of("march-1","march-2"),R)==2 && cleared(Set.of("march-1","march-3"),R)==1, "cleared is the unbroken run from order 1");
check(lockedBy(row("march-3"),Set.of("march-1"),R).id().equals("march-2") && lockedBy(row("march-2"),Set.of("march-1"),R)==null && lockedBy(row("march-1"),Set.of(),R)==null, "the order lock names the first unwon lower battle");
check(nextUnwon(Set.of("march-1"),R).id().equals("march-2") && nextUnwon(all(),R)==null && ride(all(),"march-4",R).id().equals("march-4") && ride(all(),"",R).id().equals("march-6"), "next and ride");
check(Arrays.equals(teamSizes(row("march-2")),new int[]{1,0}) && Arrays.equals(teamSizes(row("march-3")),new int[]{2,2}) && Arrays.equals(teamSizes(row("march-6")),new int[]{3,3}) && Arrays.equals(teamSizes(row("march-1")),new int[]{1,1}), "team sizes from the banner lists");
// settle: first win with the bonus, then a replay, then a loss, then the chapter
check(s1.firstWin() && s1.bonusPaid() && s1.bonusShards()==5 && s1.bonusGold()==40 && s1.bonusHouse().equals("gilded") && won.contains("march-1") && claimed.contains("b1_hold_all"), "first win pays the bonus once");
check(!s2.firstWin() && !s2.bonusPaid() && s2.cleared()==1, "a replay pays no bonus");
check(!s3.bonusPaid() && !won.contains("march-2") && claimed.size()==1, "a loss with the bonus kept records nothing");
check(s6.chapterDone() && s6.pactGranted() && pacts.equals(Set.of(1)) && !s7.pactGranted(), "the Pact once per chapter");
check(!s8.pactGranted(), "a profile whose writChapters already holds 1 gets no second Pact");
```
  And in `ProfileMigrationCheck`, `p4-migrate-v2`: a schema 2 fixture with `campaignCleared` 1, the Pact count 1 and `writChapters` [1] becomes schema 3 with `battlesWon` [march-1], `bonusClaimed` [], the Pact count 1, `writChapters` [1], `story.read` [c1_b1_before] and `campaignCleared` 1; a fixture with `campaignCleared` 3 gets march-1 to march-3 and their three before-scenes; `upgradeFile` leaves exactly one `<uuid>.v2.json` after two runs; a schema 1 fixture goes to 3 with both backups. `p4-story-json`: a `StoryState` with every field set survives `toJson` and `fromJson`, and `warm()` counts two warm choices of three.
- [ ] **Step 3: Run the MarchMathCheck and ProfileMigrationCheck lines; expect FAIL** (`cannot find symbol: class MarchMath`).
- [ ] **Step 4: Add the rows, with preflight rules:** `campaignBattles.mode` is a `modes` id that hosts the row's `map`; `difficulty` a `botCommand` id; every banner a `botBanners` id or `none`; `waveSet` a `waves` set when the mode is `survival`, else `none`; `side` is `attack` or `defend` for Siege and `none` otherwise; `bonus` a `bonusObjectives` id; `talkOrder` lists companion ids; `bonusObjectives.kind` is in the closed list of spec 11. `botBanners` ids starting `march` are exempt from the `<house>_<difficulty>` id form and the fieldable check and may hold `empty`; their weights still sum to 20; `Campaign.fieldableBotBanners` skips them. Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement** `MarchMath`, `StoryState`, the `Profile` fields and `sanitize`, `v2toV3` and the `upgradeFile` order; switch every reader of `campaignBattles.size` to `MarchMath.teamSizes`; `forClient` sends `battlesWon`, `bonusClaimed` and `story`.
- [ ] **Step 6: Run both standalone lines; expect `MarchMathCheck: 9 checks passed` and ProfileMigrationCheck's count with `p4-migrate-v2` and `p4-story-json` among them.** Compile both source sets, then run Phase 1 suite `tutorial` and Phase 2 suite `migration`: both pass as before, the tutorial now against `march1_turncoats`.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{MarchMath,StoryState,Profile,ProfileMigration,Campaign,Spec}.java test-tools/standalone/{MarchMathCheck,ProfileMigrationCheck}.java sheets/design.json tools/preflight.py
git commit -m "Phase 4: march data, MarchMath, StoryState and profile schema 3"
```

---

### Task 11: Campaign battles as real battles, with bonuses and the march run

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/Bonus.java`
- Modify: `GravesAndGlory.java` (`Squad.fixedBanner`; the tracker in `Battle`), `Campaign.java` (`campaign:` order lock and seating, `bannerFor`, `finish`, the report JSON, the victory and defeat notices), `SurvivalMode.java` (`waveSet` from the row), `test-tools/java/gg/gravesandglory/BotSimulation.java` (`simMarch`), `build.gradle` (`simMarch`), `tools/mode_gate.py` and its test (`march`), `Phase4Checks.java` (suites `march` and `save`), `docs/PHASE-4-GATES.md`

**Interfaces:**
- Consumes: Task 10 `MarchMath`, `Profile.battlesWon`, `bonusClaimed`, the rows; Tasks 2 and 5 to 7 (`Campaign.battleMode`, `SiegeMode.fellAt`, `BannerMode` scoring, `SurvivalMode.waveSet`); Task 3 `FeedLines.bonus`, `FeedEvents.mode`; `Unit.lastHitBy`; Phase 1 `Campaign.finish`, `save`, the free roll on battle 1.
- Produces in `Squad`: `String fixedBanner` (a `botBanners` id or null); `Campaign.bannerFor(s)` returns that row's slots when it is set.
- Produces in `Campaign`:
  - `campaign:<id>` refuses with `Win <name> first.` (spec 10's line, `name` from `MarchMath.lockedBy`) and starts nothing.
  - A campaign battle is no longer practice: `practice=false`, the player's own banner and owned troops only. `battleMode` is the row's mode. The player sits on team 0 (every chapter 1 Siege is `attack`), one bot banner per `allyBanners` entry joins team 0 and one per `enemyBanners` entry team 1, each with `fixedBanner` set and tier `row.difficulty()`. Survival takes the row's `waveSet`. Lessons and the one-time free roll stay on `march-1`.
  - `finish(int winner)` for a campaign battle, before `returnLobby`, in spec 12's order: the tracker's last update (`b.bonus.end(b,player,won)`); `MarchMath.settle`; the bonus shards to the row's House and the bonus gold (not bound by the 180 s rule); the Pact count +1 when `pactGranted`; the normal match rewards; then one `save`. If the save fails, the sets, gold, shards and the Pact count are put back. Then the feed gets `FeedLines.bonus(kept,tag)` if the tracker had not already sent it, and the row's `victory` (a win) or `defeat` (a loss or draw) as a `notice` line.
  - The report JSON gains `battleMode`, `count` (`mode.count(b)`), `bonus` (`{"id","kept","shards","gold"}`, shards and gold 0 when not paid) and `chapter` (`{"done","pact"}`).
- Produces `Bonus` (all trackers on the round; `Battle.bonus` is null outside campaign battles):
  - `enum State { OPEN, KEPT, LOST }`; `abstract static class Tracker { final Spec.BonusObjectivesRow row; State state=State.OPEN; abstract void tick(Battle b,Squad player); void end(Battle b,Squad player,boolean won); }`; `static Tracker create(Spec.BonusObjectivesRow row)`.
  - `hold_all`: KEPT the first tick team 0 owns every map point; at the end OPEN becomes LOST.
  - `lose_at_most`: LOST when the player's squad has lost more than `value` troops; at a win OPEN becomes KEPT.
  - `stage_by`: KEPT when `SiegeMode.fellAt(1)` is at most `value*20`; LOST once `value` s pass with the gate standing.
  - `carry_self`: KEPT when the player's commander scores a capture carrying; at the end OPEN becomes LOST.
  - `win_within`: LOST once `value` s pass; KEPT at a win inside it.
  - `kill_unit`: KEPT when a unit of troop `target` dies with `lastHitBy` the player's squad; LOST when it dies otherwise; at the end OPEN becomes LOST.
  - When a tracker settles mid-round the feed gets `FeedLines.bonus(state==KEPT,row.tag())` once (a `notice`). KEPT pays only when the round is won; on a loss the line has already been shown and nothing is paid.
- Produces the march run: `-PsimMarch=<battle id>` (property `gravesandglory.simMarch`) runs that battle's row with a Normal bot in the player's seat fielding `march_by<order>`, 5 matches (draft value), the CSV as Task 9's; `mode_gate.py <csv> march` passes when team 0 wins at least 2 of the decided rounds.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suites `march` and `save`** (a fresh profile per id; the harness forces wins, losses and events through the round state):
  - `p4-march-1-start` to `p4-march-6-start`: `campaign:<id>`, with every lower battle won, starts the row's map and mode; each seat's banner equals its `fixedBanner` row's slots (the player's equals his profile banner); every bot's tier is the row's difficulty; the player is on team 0 with the row's team sizes (`MarchMath.teamSizes`); `march-2` runs the `moon` set at Easy scale; `practice` is false.
  - `p4-march-bonus-hold_all`, `-lose_at_most`, `-stage_by`, `-carry_self`, `-win_within`, `-kill_unit`: each kind driven once to KEPT and once to LOST in its own battle. A kept bonus on a win pays 5 shards to the row's House and 40 gold, and `bonusClaimed` gains the id; the feed shows `Bonus kept: <tag>` or `Bonus lost: <tag>` once. `kill_unit` is KEPT only when the King's last hit is the player's squad, and LOST when an allied bot's is.
  - `p4-march-kept-loss`: a KEPT tracker on a lost round pays nothing and claims nothing.
  - `p4-march-lock`: with only `march-1` won, `campaign:march-3` sets no pending battle and the notice reads `Win The Moon Shrine first.`
  - `p4-march-replay`: replaying a won battle pays the normal gold and no bonus, and `battlesWon` is unchanged.
  - `p4-march-pact`: winning `march-6` with the other five won adds one to the Pact count and `writChapters` gains 1; a replay grants nothing more; a profile that already held chapter 1 in `writChapters` gets no Pact.
  - `p4-march-feed`: a win sends the row's `victory` line and a loss or draw its `defeat` line, as notices.
  - `p4-march-report`: the report JSON has `battleMode`, `count`, `bonus` with id, kept, shards and gold, and `chapter` with done and pact.
  - `p4-save-order`: when `returnLobby` begins, the profile file on disk already holds the new `battlesWon`, `bonusClaimed` and gold.
  - `p4-save-rollback`: with a directory planted at `<uuid>.tmp` so the write fails, the three fields and the gold in memory equal their values before `finish`.
  - `p4-save-relog`: after a win the suite drops the cached profile, and the profile read back from disk shows the three fields.
  - In `tools/test_mode_gate.py`: `test_march_two_of_five` (2 team 0 wins of 5: exit 0; 1 of 5: exit 1).
- [ ] **Step 3: Run suites `march,save`; expect failures** (no lock, practice seating).
- [ ] **Step 4: Implement** `fixedBanner` and `bannerFor`, the seating, the lock, `Bonus`, the `finish` order, the report keys and the notices.
- [ ] **Step 5: Run suites `march,save` and Phase 1 suites `tutorial,report`.** Expected: every id above, `pass p4-march-suite`, `pass p4-save-suite`, and the Phase 1 ids as before.
- [ ] **Step 6: Run the march run** (Bot runs recipe, `-PsimMarch=march-<n>` for n 1 to 6, 5 matches each; Siege and Banner with their mode's round seconds), then `python tools/mode_gate.py <root>/botsim/results.csv march` for each. Expected: six `pass` lines. A battle under 2 of 5 is tuned through its draft values (tier, ally banner, bonus value, or the `march_by` row), one change at a time, logged in `docs/PHASE-4-GATES.md`.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{Bonus,GravesAndGlory,Campaign,SurvivalMode,Spec}.java sheets/design.json test-tools/java/gg/gravesandglory/{BotSimulation,Phase4Checks}.java build.gradle tools/mode_gate.py tools/test_mode_gate.py docs/PHASE-4-GATES.md
git commit -m "Phase 4: campaign battles seat their banners, track their bonus and save in order"
```

---

### Task 12: The War Table's campaign card, the briefing bonus line and the Pact report

**Executor:** Sonnet 5.5 max.

**Files:**
- Modify: `src/main/java/gg/gravesandglory/{CampaignClient,WarTableHome,CampScreen,WarTableRecruit}.java`, `Campaign.java` (the report's `chapter.first`)
- Modify: `sheets/design.json` (`economyModel.chapterGold` 1060, draft value: the free roll's 100 plus six battles at 80, 40 and the 40 bonus), then `python tools/economy_model.py`
- Modify: `Phase4Checks.java` (suite `report`; suite `hud`'s `p4-feed-fit` grows), `docs/PHASE-4-GATES.md`

**Interfaces:**
- Consumes: Task 3 `CampaignClient.battleMode`, `howTo()`; Task 10 `MarchMath.ride`, `teamSizes`, `chapters` row (`epilogue`, `pactText`); Task 11's report keys and `Campaign.finish`; Phase 2's Legendary unlock on the recruit page and its Pact command (`legendary:<house>:pact` in the Phase 2 plan).
- Produces:
  - `briefing(...)` draws a campaign battle's bonus line (`bonusObjectives.text`) after the how-to lines, one blank row between, in `body` `SEPIA`.
  - The Campaign field card: the map of `MarchMath.ride(won,CampaignClient.lastChosen,rows)`, its mode's rule (`ModeMath.ruleLine`) and `ModeMath.sizeLine(mode,sizes[0],tier,side)`; the Ride plate reads `"Ride to "+row.name()` and sends `campaign:<id>`. `CampaignClient.lastChosen` (String, session only) is the battle the book last played.
  - The report JSON's `chapter` gains `first` (the chapter became done in this finish). The `results` page, when `first`, draws the chapter's `epilogue`; when `pact`, the Pact card under it (the `pactText` parts: title as `heading`, body and note as `body`, wrapped to the card) and a `btn-vellum` "Choose your Legendary" that opens the recruit page in its Legendary unlock view. Closing either screen keeps the Pact.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `report`, and grow `p4-feed-fit`:**
  - `p4-briefing-bonus`: `march-3`'s card draws its name, briefing, reason, the three Siege `howTo` lines and `Break the gate inside six minutes.` in that order, every line inside the card. Screenshot `p4-briefing-siege`.
  - `p4-field-campaign`: with `march-1` won, the home's Campaign card shows Hollowwood Reach, `Survival: 7 waves` and `1 banner, Easy waves`, and Ride reads `Ride to The Moon Shrine` and sends `campaign:march-2`. With all six won and `lastChosen` `march-4`, Ride reads `Ride to Raiders on the River`.
  - `p4-report-pact`: after the win that completes the chapter, the `results` page draws the epilogue, the card titled `Legendary Pact` with its body and note, and the button. Screenshot `p4-report-pact`. The button opens the recruit page's Legendary view with the five Legendaries offered; choosing Brassjaw sends the Pact command and the Pact count falls by one. A second run that closes the view without choosing keeps the count.
  - `p4-report-epilogue-only`: a profile whose `writChapters` already held 1 sees the epilogue with no card and no button.
  - `p4-feed-fit` (suite `hud`) adds every `campaignBattles.victory` and `defeat` and both bonus forms of every `bonusObjectives.tag`, each at most 140 px.
- [ ] **Step 3: Run suites `report,hud`; expect failures** (no bonus line, no Pact card).
- [ ] **Step 4: Implement** the bonus line, the Campaign card, the Ride plate, `lastChosen`, the report key and the `results` page block.
- [ ] **Step 5: Run suites `report,hud` and UI suite `ui-home`** (`GRAVES_PHASE1_CHECKS=ui-home`). Expected: every id above, `pass p4-report-suite`, `pass p4-hud-suite`, and the UI ids as before. The tester states what each screenshot shows.
- [ ] **Step 6: Rerun the economy model** with `chapterGold` 1060: `python tools/economy_model.py` passes the assertions Phase 2 set; its printout goes to `docs/PHASE-4-GATES.md` under "Economy". An assertion that fails is reported to Kort with the printout, not tuned here.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{CampaignClient,WarTableHome,CampScreen,WarTableRecruit,Campaign,Spec}.java sheets/design.json test-tools/java/gg/gravesandglory/Phase4Checks.java docs/PHASE-4-GATES.md
git commit -m "Phase 4: campaign field card, briefing bonus line and the Pact report"
```

---

### Task 13: The story sheets and the pure story classes

**Executor:** Sonnet 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{StoryBook,StoryLines,StoryNames}.java` (pure), `test-tools/standalone/StoryCheck.java`
- Modify: `sheets/design.json` (new `storyPages`, `storyLines`, `graveRoll`, `objects`, `speakers`), `tools/preflight.py`

**Rows** (every player-facing cell transcribed from the copy, never written; `-` where unused):
- `storyPages` (`id, chapter, page, order, kind, title, optional, station, battle, when, pageTitle, passage`): the copy's 26 scenes in its page order, `id` the scene id (`battle_<n>` for a battle), `title` the copy's quoted title, `pageTitle` and `passage` on each page's first row. `kind` is `book`, `stage`, `card` or `battle`; `when` is `before` for `c1_b<n>_before` and `c1_king_before`, `battle` for `battle_<n>`, `after` for `c1_b<n>_after` and `c1_king_after`, `auto` for `c1_crown`, `-` otherwise; `battle` is `march-<n>` on those rows. `optional` is true for `c1_archive` only. `station` on the card rows: `army` for `c1_third_name`, `c1_letter` and `c1_council` (the war table), `contracts` for `c1_archive` and `c1_rule` (the Archivist), `recruit:graveborn` for `c1_shrine_2`.
- `speakers` (`id, initial, label, name, namedAt, portrait, troop, joins, leafPage, want, station`), from the copy's Voices and company tables:

| id | initial | label | name | namedAt | portrait | troop | joins | leafPage | station |
|---|---|---|---|---|---|---|---|---|---|
| maud | m | The sergeant | Maud | c1_third_name:2 | troop:sentinel | sentinel | c1_third_name | 1 | recruit:gilded |
| wren | w | The archer | Wren | c1_b2_after:2 | troop:bonebow | bonebow | c1_b2_after | 3 | recruit:graveborn |
| dunmar | d | The gate-warden | Dunmar | c1_letter:5 | troop:ic_breaker | ic_breaker | c1_b3_after | 4 | recruit:ironclad |
| ashka | a | The hunter | Ashka | c1_council:4 | troop:wm_hunter | wm_hunter | c1_b5_after | 7 | recruit:wildmoot |
| pell | p | The Archivist | - | - | npc:contracts | - | - | 0 | contracts |
| tomas | t | Her brother | Tomas | c1_king_after:6 | troop:sentinel | - | - | 0 | - |
| king | k | The Hollow King | - | - | troop:gb_hollowking | - | - | 0 | - |
| edric | e | - | - | - | - | - | - | 0 | - |
| record | r | - | - | - | - | - | - | 0 | - |
| raider | x | A raider | - | - | - | - | - | 0 | - |

  `want` is the copy's company-table want for the four companions, `-` for the rest. The labels and names are the copy's. `portrait` names the live model a card renders (Kort's ruling: portraits come from the character's own model, rendered into the portrait rect as the War Table frame renders the commander): `troop:<id>` or `npc:<camp station>`. A card's portrait mirrors `helm_off` once the speaker's own actor is posed so (Task 16), which turns the King's portrait into Edric's face.
- `storyLines` (`id, companion, trigger, priority, text, warm, plain, open`; `id` is the voice id, story 7.3's `voice` column): the copy's Reaction lines (triggers `b<n>_fell`, `b<n>_fell:<companion>`, `b<n>_rout`, `b<n>_bonus_lost`, `b<n>_losses`, `b<n>_bonus_kept`, priority 1 to 5 as given), the Replay set (`replay_*`), the Wounded and resting lines (`wounded`, `resting`, priority 0), Wren's memory rows 2 to 6 (`shrine_2` to `shrine_6`, priority 0; rows 1 and 7 are the cards of `c1_shrine_2` and `c1_crown`, so each voice id lives in one place), the Recruit lines (`recruit`), the Archivist's cards (`pell_<topic>`, priority the card's order in its topic) and Ashka's choices (`choice_<n>` with the two chits in `warm` and `plain`; `choice_<n>:warm` and `choice_<n>:plain` for her answers). `open` is `always`, or `+`-joined conditions: `read:<scene>`, `page:<n>`, `again:<scene>` (that scene read and this topic's earlier rule line said), `unwon:<battle>`. The Archivist's topics open as the copy's Open column says (`always`, `read:c1_letter`, `again:c1_rule`, `page:3`); the choices are `read:c1_b5_after`, `read:c1_council` and `page:8+unwon:march-6`.
- `graveRoll` (`id, troopType, name, line, voice`): the copy's 12 rows as `roll_<n>`; `troopType` `revenant` for Risen, `bonebow` for Bone Archer, `reaper` and `stalker` for the copy's stand-ins (both are the Graveborn troop ids).
- `objects` (`id, text`): `pact` (the scraped pact), `pactcopy` (the turncoat's pact-copy), `shroud`, `roll`, `childstone`, and `stone` (the copy's three lines as the Archivist reads them, joined with ` | `).

**Interfaces:**
- Consumes: Task 10 `StoryState` (with `Facts`), `MarchMath`, `Profile.battlesWon`.
- Produces `StoryBook` (pure, `static`; `P` is `Spec.STORYPAGES` in book order, page then order):
  - `enum State { READ, NEXT, OPEN, SEALED }`.
  - `String next(StoryState s,Set<String> won,Spec.StoryPagesRow[] P)`: the first row that is neither optional nor read, where a battle row is read once its battle is won and a before-scene of a won battle counts as read; null when the chapter is read through.
  - `State state(Spec.StoryPagesRow row,StoryState s,Set<String> won,Spec.StoryPagesRow[] P)`: READ when read; NEXT when it is `next`; OPEN for an unread optional row on a page at or before the current one; SEALED otherwise.
  - `int currentPage(...)` (the page of `next`, else the last page); `String beforeScene(String battle,Spec.StoryPagesRow[] P)` and `afterScene(...)` (null when none); `List<String> company(StoryState s,Spec.SpeakersRow[] S)` (companions whose `joins` scene is read, in sheet order).
- Produces `StoryLines` (pure, `static`):
  - `List<Spec.StoryLinesRow> reaction(StoryState.Facts f,List<String> present,Spec.CampaignBattlesRow battle,Map<String,String> joinedAt,Spec.StoryLinesRow[] L)`: the copy's rules. Facts in priority order (a fall, the player's group routed, the bonus missed on a win, losses above a third of `bannerSize`, the bonus kept); at most three lines. A fall gives the named companion's `b<n>_fell:<fallen>` line first (skipped when that companion was absent or fell), then the fallen companion's own `b<n>_fell`. Every other fact goes to the present, unfallen companion who has spoken least this visit, ties by `talkOrder`. A companion in a replay of a battle fought before it joined (`joinedAt`) uses the `replay_*` rows. Wren's fell line plays after one fall; after two, her `resting` line replaces it.
  - `Spec.StoryLinesRow wounded(String companion,StoryState s,Spec.StoryLinesRow[] L)` (the next `wounded` or `resting` row in turn, cycling); `Spec.StoryLinesRow shrine(StoryState s,Spec.StoryLinesRow[] L)` (the next of `shrine_2` to `shrine_6` when `c1_shrine_2` is read and `shrineDue>0`, else null).
  - `Object recruit(String troop,StoryState s,Spec.GraveRollRow[] R,Spec.StoryLinesRow[] L)`: the next unheard `graveRoll` row of that troop type, else the next `recruit` line in turn; null for a troop the roll does not know.
  - `List<Spec.StoryLinesRow> pell(StoryState s,int page,Spec.StoryLinesRow[] L)` (the cards of the next unsaid open topic, else empty); `Spec.StoryLinesRow choice(StoryState s,int page,Set<String> won,Spec.StoryLinesRow[] L)` (the next unanswered open choice, else null); `boolean open(String cell,StoryState s,int page,Set<String> won,Set<String> said)`.
- Produces `StoryNames` (pure): `static String label(String speaker,String sceneId,int card,Spec.SpeakersRow[] S,Spec.StoryPagesRow[] P)`: `label` before the `namedAt` card in book order and `name` from it on, so a replayed early scene shows the label it had (assumption); `static final List<String> NOUNS` (Maud, Wren, Dunmar, Ashka, Tomas, Edric, Hesper, the Hollow King, and the lore's other names that must stay unsaid in chapter 1: Pell, Osric, Elin, Halden Rook, Tarn); `static final List<String> REALM` (Barrowlands and the five map names); `record Finding(int page,String what)`; `static List<Finding> check(Map<Integer,List<String>> textByPage,Map<Integer,List<String>> cardsAndCaptionsByPage)`: a page may introduce one noun not seen on an earlier page, page 8 two (Kort's ruling: Edric and Tomas), and no REALM word may appear in a card or caption. Object words, the Houses, roles, nicknames, Danta and the company's "Hollow" are not counted (the copy's reading).
- Produces `StoryCheck` (standalone, args `<resources> <copy> <test resources>`): its own `check(boolean,String)` lines, then `story-names`, `story-voice` and `story-copy` over the sheets now and the scene files from Task 14.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write `StoryCheck`:**
  - `p4-book-fresh`: a fresh state's `next` is `c1_fall`, page 1, `c1_archive` OPEN and every page 2 row SEALED; the company is empty.
  - `p4-book-catchup`: the state Task 10's `v2toV3` makes for `campaignCleared` 3 has `next` in turn `c1_fall`, `c1_third_name`, `c1_b1_after`, `c1_b2_after`, `c1_b3_after`, `c1_letter`, `c1_rule`, `c1_shrine_2`, `c1_b4_before`, `battle_4` as each is marked read; Maud enters the company only once `c1_third_name` is read, Wren after `c1_b2_after`, Dunmar after `c1_b3_after`, and Ashka not at all.
  - `p4-lines-reaction`: battle 4 won, Maud fell, the group routed, the bonus missed: `c1_d14`, `c1_m54`, then the rout line to the least-spoken present companion by `talkOrder` (Wren's `c1_w26`); three lines, never four. Battle 3 lost with the bonus missed gives no bonus-missed line. Battle 1 replayed with Wren present gives her `replay_*` lines. Wren fallen twice gives `c1_w46` or `c1_w47`, not `c1_w18`.
  - `p4-lines-shrine`: no row before `c1_shrine_2` is read; then one row per due return in order `shrine_2` to `shrine_6`; a state with five dues hears rows 2 to 6 in order before `c1_crown`.
  - `p4-lines-pell`: topic 4 waits for `c1_letter`, topic 6 for page 3, topic 5 for `c1_rule` and a second asking; each use gives the next unsaid open topic.
  - `p4-lines-choice`: choice 3 is open on page 8 only while `march-6` is unwon; `warm()` counts the warm picks.
  - `p4-names-labels`: `label("maud","c1_third_name",1,...)` is `The sergeant` and card 4 `Maud`; `label("tomas","c1_king_before",...)` is `Her brother`.
  - `p4-names-rule`: a fixture page with two new nouns is a finding, page 8's Edric and Tomas are not, and `Rivergate` in a caption is a finding.
  - `story-names: clean` and `story-copy: <n> lines match` over the sheets (every `storyLines`, `graveRoll`, `objects`, `speakers.want`, `storyPages` title, page title and passage cell equals its line in the copy).
- [ ] **Step 3: Run StoryCheck with the reduced class set** (Tasks 14, 16 and 17 add `SceneScript`, `OggLength` and `SceneMath` back to the Test runs line):
```bash
"$J/javac" --release 17 -d build/p4-checks $S/{Spec,StoryState,StoryBook,StoryLines,StoryNames,MarchMath}.java test-tools/standalone/StoryCheck.java && "$J/java" -cp build/p4-checks gg.gravesandglory.StoryCheck src/main/resources docs/copy/chapter-1.md test-tools/resources
```
  Expect FAIL: `cannot find symbol: class StoryBook`.
- [ ] **Step 4: Add the five sheets, with preflight rules:** `storyPages` pages run from 1 without gaps and orders within a page from 1; `kind` and `when` in their lists; a `battle` cell names a `campaignBattles` id; a card row has a `station`; `speakers.joins` and `namedAt` name `storyPages` ids (`namedAt` as `<id>:<card>`); `storyLines.companion` is a `speakers` id; `trigger` matches one of the forms above; `open` parses; `graveRoll.troopType` is a troop id. Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement** `StoryBook`, `StoryLines`, `StoryNames` and the sheet half of `StoryCheck`.
- [ ] **Step 6: Run StoryCheck (reduced set); expect `StoryCheck: 8 checks passed`, `story-names: clean` and `story-copy: <n> lines match`.** A copy line that does not fit a sheet rule goes back to the copy. Compile both source sets.
- [ ] **Step 7: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{StoryBook,StoryLines,StoryNames,Spec}.java test-tools/standalone/StoryCheck.java sheets/design.json tools/preflight.py
git commit -m "Phase 4: story sheets, StoryBook, StoryLines and StoryNames"
```

---

### Task 14: SceneScript, the scene files and the voice layout

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/SceneScript.java` (pure)
- Create: `src/main/resources/data/gravesandglory/scenes/c1/<id>.scene` for the copy's 20 scenes (every `storyPages` row whose kind is not `battle`), and `scenes/c1/silent.txt` (the copy's silent set, one id a line)
- Create: `tools/write_voice_sounds.py`, `tools/test_write_voice_sounds.py` (unittest)
- Create: `test-tools/resources/data/gravesandglory/scenes/test/{ok,nested_if,voiced_count,unknown,panel_in_stage,no_end}.scene`
- Modify: `test-tools/standalone/StoryCheck.java` (the scene half), `tools/preflight.py`

**The command set.** Story 7.3's list, plus six additions that Kort's footage ruling (2026-10-18 late) needs. The additions are marked "added" and are reported back to the story spec, because the spec says new commands come back through it.
- Line 1 is `# scene <id> <kind>`, where kind is `panel`, `stage` or `card`. Every other line is blank, a `#` comment, or one command.
- World: `map <n>`, `camp`, `time <0..23999>`.
  - `weather clear|rain|thunder|snow` (`snow` added, for page 7).
  - `fog <density 0..1> [<rrggbb>]` (the colour is added, for the Fall's fog and the sea).
- Actors:
  - `actor <id> <troop id | companion:<speaker> | commander> at x y z facing <yaw> [team 0|1] [pose <p>]`.
    - `team` is added: it gives the side cloth. Without it the actor wears linen, as portraits do.
    - `commander` is added: the player's own body stands there, because the copy stages the commander in four scenes. A scene has at most one.
  - `walk <actor> to x y z over <ticks>`.
  - `pose <actor> <p>`, where `p` is one of Phase 2 Task 5b's scene poses: `sword_up`, `kneel`, `fallen`, `stand`, `helm_off`, `point`, `sit` or `raise`. Story 7.3 counts six poses, but Phase 2's set has eight and no `swing`, so the eight are the list (reported back).
  - `prop <id> <item id> at x y z [facing <yaw>] [scale <s>]` (added). It places a vanilla `item_display`, because a troop draws no held item (Phase 2 Task 5, Needs Kort 10). Uses: the horn, the pact on the table, the dropped pact-copy, the crown.
- Camera:
  - `camera x y z to x y z over <ticks> [look x y z | look <actor>]` (`look` added on the moving form; without it the camera faces along its path).
  - `camera hold x y z look <actor>`, `camera player`.
- Panels and pieces:
  - `panel <set> [drift dx dy dz over <ticks>]` and `caption "<text>" [voice <id>]`, both in panel scenes only.
  - `structure <set>`, in stage scenes. `<set>` is a `storySets` id (Task 17), so the copy's `structure ironhold/gate_intact` becomes a row that places that piece.
- Cards: `card <speaker> "<text>" [voice <id>]`, `card item:<object id> "<text>"`.
- Effects and flow: `sound <id>`, `particles <type> at x y z count <n>`, `wait <ticks>`, `fade in|out <ticks>`, `end`.
- Branches: `if companion:<speaker> standing|wounded|resting`, `if match killed:<troop>|bonus|won`, `else`, `endif`.
- Coordinates: in a panel scene every coordinate is relative to the set's centre block. In a stage scene they are world coordinates on the scene's map.

**Interfaces:**
- Consumes: Task 13's sheets and `StoryCheck`; the scene pose names of Phase 2 Task 5b (`TroopAnimator.setScenePose(int entityId,String pose)` takes them).
- Produces `SceneScript` (pure):
  - `record Op(int line,String cmd,List<String> args,int jump)`.
    - `cmd` is the command word. The camera forms are `camera`, `camera_hold` and `camera_player`.
    - `args` is the remaining tokens. Quoted text is one argument, without its quotes.
    - `jump`: for an `if`, the index after its `else` or `endif`; for an `else`, the index after its `endif`; otherwise -1.
  - `record Scene(String id,String kind,List<Op> ops)`; `static final class ParseError extends Exception { final int line; }`.
  - `static Scene parse(String id,List<String> lines,Set<String> silent) throws ParseError`. Each error names its 1-based line:
    - a bad header, or a header id that is not the file's;
    - an unknown command or a wrong argument count;
    - a pose outside the eight, or a second `commander` actor;
    - a nested `if`, an `else` or `endif` without its `if`, or an `if` with no `endif`;
    - `{count}`, `{losses}` or `{bonus}` in a voiced card or caption whose voice id is not in `silent`;
    - `panel` or `caption` outside a panel scene;
    - `map`, `camp`, `camera`, `actor`, `prop`, `walk` or `structure` in a card scene;
    - a last command that is not `end`.
  - `static List<String> texts(Scene s)`: every card, caption and item text, in order.
  - `static List<String[]> voiced(Scene s)`: `{speaker, voice id}` for each voiced card and caption. A caption's speaker is the speaker whose initial its voice id carries.

**The scene files** are dialogue skeletons. Each holds:
- its header;
- every card and caption of the copy's table in order, with its voice id, its text transcribed exactly;
- the copy's card-level branches (`c1_king_after` cards 6a and 6b under `if match killed:gb_hollowking`);
- `end`.

Staging (map, time, actors, camera, sets, panel lines) is added by Tasks 19 to 23. A skeleton plays as cards over the player's own view.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write the failing tests.**
  - StoryCheck gains:
    - `p4-scene-parse`: every `storyPages` row whose kind is not `battle` has a file that parses. Its header kind is `panel` for `book` and otherwise equals the row's kind. `c1_king_after`'s `if` jumps past card 6a to the `else`.
    - `p4-scene-errors`: each error fixture under `scenes/test/` fails at its known line. `ok.scene` holds one line of every command form and parses with one op per command line.
    - `story-voice: <v> voiced, <u> unrecorded, <s> silent, clean` over the scene files and the `storyLines` and `graveRoll` voice columns. Clean means:
      - no id is both in `sounds.json` and in `silent.txt`;
      - every `voice.` entry of `sounds.json` has its file;
      - every silent id is used;
      - every voice id's initial is its speaker's (`speakers.initial`).
    - `story-copy` and `story-names` also read `SceneScript.texts` of every scene file.
  - `tools/test_write_voice_sounds.py`:
    - `test_adds_present`: a fixture `voice/c1/c1_m10.ogg` gives the entry `voice.c1_m10` with `{"sounds":[{"name":"gravesandglory:voice/c1/c1_m10","stream":true}]}`.
    - `test_drops_missing`: an entry with no file is removed.
    - `test_keeps_others`: every non-voice entry is kept, in the same order.
- [ ] **Step 3: Run StoryCheck** with the Test runs line (`SceneScript` added; `OggLength` is still missing until Task 16, so drop it from the line for now), and run `python -m unittest tools/test_write_voice_sounds.py -v`. **Expect FAIL** (`SceneScript` not defined; no module `write_voice_sounds`).
- [ ] **Step 4: Implement** `SceneScript`, the 20 skeletons, `silent.txt` and `write_voice_sounds.py`. Then add the preflight rule: every non-battle `storyPages` id has `scenes/c1/<id>.scene`, and no scene file lacks a row.
- [ ] **Step 5: Run.** Expect:
  - `StoryCheck: 10 checks passed`;
  - `story-voice: <v> voiced, <v> unrecorded, 9 silent, clean`;
  - `story-names: clean` and `story-copy: <n> lines match`;
  - unittest `OK` and `Preflight clean`.

  A line that does not parse as the copy writes it goes back to the copy.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/SceneScript.java src/main/resources/data/gravesandglory/scenes tools/write_voice_sounds.py tools/test_write_voice_sounds.py tools/preflight.py test-tools/resources/data/gravesandglory/scenes test-tools/standalone/StoryCheck.java
git commit -m "Phase 4: SceneScript and the chapter 1 scene skeletons"
```

---

### Task 15: The storybook's UI kit

**Executor:** Sonnet 5.5 max.

Under Kort's ruling, this task draws only the book and its chrome. It draws no portrait, no panel and no thumbnail: portraits are live models (Task 16), and panels and thumbnails are in-game frames (Tasks 18 to 23).

**Files:**
- Create: `tools/ui_art/storybook/{sb_common,sb_book,sb_table,sb_props,sb_ui}.py`.
  - Copy these byte for byte from `work/ui-concepts/storybook/` (round 1 as approved, NOTES.md rulings 1 to 9).
  - `sb_common.py`'s path setup is the one line that changes, pointed at `tools/ui_art`.
  - `sb_portraits`, `sb_river`, `sb_world` and `sb_scenes` are not copied: their pictures are placeholders.
- Create: `tools/ui_art/storybook/sb_extra.py` (the pieces the mockups lack, in the same painters' style)
- Modify: `tools/ui_art/manifest.py` (`MOCK` gains the five modules' `ASSETS`; `story/` entries)
- Create (exported, committed): `src/main/resources/assets/gravesandglory/textures/gui/story/*.png`, and `textures/gui/atlas.json` regenerated

**Textures** (folder `story/`):
- Every asset the four copied painters register, under its own name. Among them:
  - the table and book: `table-sb-closed`, `table-sb-open`, `rail-sb`, `book-closed`, `book-open`, the candle, glow, inkwell, leaves, ribbon and motes;
  - the cards and strips: `card-404x104`, `card-400x394`, `caption-strip`, `bar-top`, `bar-bot`;
  - the frames and marks: `mini-46`, `mini-46-grey`, `mini-66`, `moon`, `voice-mark`, `quill-tick`;
  - the plates: `plate-next`, `plate-read`, `plate-sealed`, `glow-next`;
  - the rest: `glyph-dom`, `clasp-closed`, `clasp-open`, `mouse-use`, the key caps, the tags, `plaque-144x32`, `plaque-152x32`, `tab-brass-112x28`, `rule-orn`, `rule-row`, `seal-hollow`.
  - Multi-frame pieces keep their frame counts: the candle 4, the tumbling leaf 4, and `glow-next` 2 (2 s up and 2 s down).
- From `sb_extra.py`:
  - `card-item-404x128`: the item card, five 17 px lines at a 356 px measure.
  - `chit-404x28` and `chit-404x28-hot`: Ashka's reply chits, with a `kv-30x18` key cap at (10,5).
  - `bandage`: 12x10, for a wounded miniature's top right corner.
  - `glyph-siege`, `glyph-surv`, `glyph-banner`: drawn as `glyph-dom` is.
  - `icon-book`: the March plaque's 16x16 icon.
  - `pips-<n>-<i>` for n 2 and 5 and every i.
  - `cap-<letter>`: the drop capital, one per distinct first letter of the eight leaf passages (`storyPages.passage`).

**Interfaces:**
- Consumes: the UI plan's exporter (`build_ui_art.py`, `--verify`, `atlas.json`), `UiTex`, `UiKitSheet` and suite `ui-art`.
- Produces: the `story/<name>` atlas names above, used by Tasks 16, 17, 18 and 24.

- [ ] **Step 1: Compile both source sets.** Run `python tools/build_ui_art.py --verify`; record n.
- [ ] **Step 2: Add the `story/` entries to `manifest.py`.** Run `--verify`; **expect FAIL** naming the first `story/` PNG missing.
- [ ] **Step 3: Copy the painters, write `sb_extra.py` and export.** Run `python tools/build_ui_art.py`, then `--verify`. **Expect** `build_ui_art: <n + story count> textures verified`.
- [ ] **Step 4: Run UI suite `ui-art`.** Run it with `GRAVES_PHASE1_CHECKS=ui-art`, at 1920x1080 and GUI scale 2.
  - **Expect** `pass p1-ui-atlas`, `pass p1-ui-kit-sheet` and `pass p1-ui-art-suite`.
  - The kit sheet now shows every `story/` texture crisp at 1 px per art pixel, with no missing texture.
- [ ] **Step 5: Grade.**
  - A Sonnet 5.5 max tester grades the kit sheet's `story/` part against `work/ui-concepts/GRADING-RUBRIC.md`, beside the round 1 mockups. The tester checks that each copied texture matches its mockup crop pixel for pixel.
  - The tester grades the `sb_extra.py` pieces at the War Table detail.
  - Below 9 is a fix round on `sb_extra.py` only. A copied painter is never edited here; a fault in one goes back to the designer.
- [ ] **Step 6: Commit.**
```bash
git add tools/ui_art/storybook tools/ui_art/manifest.py src/main/resources/assets/gravesandglory/textures/gui/story src/main/resources/assets/gravesandglory/textures/gui/atlas.json
git commit -m "Phase 4: storybook UI kit (book, leaves, cards, chits, glyphs)"
```

---

### Task 16: The scene runtime, dialog cards and card scenes

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{Story,ScenePlayer,SceneScreen,DialogCard,StoryVoice,OggLength}.java` (`OggLength` pure)
- Create: `test-tools/resources/assets/gravesandglory/sounds/voice/c1/t_voice.ogg`. It is 2.0 s at 440 Hz: `ffmpeg -f lavfi -i "sine=frequency=440:duration=2" -c:a libvorbis -q:a 2 t_voice.ogg`.
- Create: `test-tools/resources/assets/gravesandglory/sounds.json` (`voice.t_voice` only).
- Create: `test-tools/resources/data/gravesandglory/scenes/test/{test_keys,test_voice}.scene`.
- Modify:
  - `GravesAndGlory.java`: channel `SCENE = new Identifier("gravesandglory","scene_v1")`, one JSON string a packet; `openCamp` calls `Story.atStation(p,station)` first.
  - `Campaign.java`: `story:` commands go to `Story.command`.
  - `GravesAndGloryClient.java`: the receiver, the skip binding and the reopen hook.
  - `CinematicHudMixin.java`: `InGameHud.render` is also cancelled while `SceneScreen.active()`.
  - `lang/en_us.json`: `key.gravesandglory.skip` is "Skip", the approved UI label.
  - `Phase4Checks.java` (suite `scenes`), `test-tools/standalone/StoryCheck.java` (`p4-ogg-length`).

**Interfaces:**
- Consumes:
  - Task 13: `StoryBook.next`/`state`, `StoryNames.label`, the `speakers` and `objects` rows.
  - Task 14: `SceneScript.parse`.
  - Task 10: `StoryState`, saved inside the profile with `Campaign.save`.
  - The UI plan: `UiCanvas`, `UiTex`, `DarkUi.heading`/`body`, `Portrait.draw(DrawContext,LivingEntity,int x,int y,int window,int tall)` and key hints from bindings.
  - Phase 2 Task 5: `ArmyMobs.portrait(String entity,World w)`, `ArmyMobs.stageName(Spec.TroopsRow,int)` (the custom name `TroopModels.resolve` reads to pick the model), `TroopModels.portraitTall(String troop)`.
  - Phase 2 Task 5b: `TroopAnimator.setScenePose(int entityId,String pose)`.
- Produces (server):
  - `Story.command(ServerPlayerEntity p,String cmd)->boolean`, for `story:play:<id>`, `story:next`, `story:skip` and `story:choice:warm|plain`. Tasks 17 and 24 add `story:arrived`, `story:march` and `story:book`; Task 26 adds `story:camp` and `story:talk:<companion>`.
  - `Story.atStation(ServerPlayerEntity p,String station)->boolean`: it plays the NEXT card scene whose `storyPages.station` is this station, and returns true. Task 26 adds camp lines.
  - `Story.play(ServerPlayerEntity p,String sceneId,Runnable follow)`, `Story.running(UUID)->ScenePlayer`, and `Story.cond(ServerPlayerEntity p,List<String> ifArgs)->boolean`:
    - `companion:<s> standing|wounded|resting` reads `StoryState.wounded`/`resting`;
    - `match killed:<troop>|bonus|won` reads `StoryState.last` (`killed`, `bonus` `kept`, `won`).
  - `ScenePlayer` runs one scene per player.
    - It steps the ops each tick until a blocking op: `card`, `caption` and `item` wait for `story:next`; `wait` and `fade` count ticks.
    - `if`/`else` jump by `Op.jump`.
    - `{count}`, `{losses}` and `{bonus}` come from `StoryState.last` (`bonus` is the battle's `bonusObjectives.tag`).
    - Labels come from `StoryNames.label`.
    - On a first reading, `end` adds the id to `StoryState.read` and saves; then `follow` runs.
    - `story:skip` is refused on a first reading (nothing changes) and otherwise jumps to `end`. A sealed scene's `story:play` does nothing.
- Produces (`scene_v1` ops, server to client; Task 17 adds `camera`, `sky`, `fog`, `pose` and `march`):
  - `{"op":"open","id","kind","title","first"}`;
  - `{"op":"card","speaker","label","text","voice","portrait","actor","chits"}`, where `portrait` is the `speakers.portrait` cell, `actor` the speaker's actor entity id or -1, and `chits` `[warm,plain]` or absent;
  - `{"op":"item","text"}`, `{"op":"caption","text","voice","panel","panels"}`, `{"op":"fade","dir","ticks"}` and `{"op":"close"}`.
- Produces (client):
  - `SceneScreen`: `shouldPause()` false, no background, `static boolean active()`, and `static boolean hideChrome` (a harness hook: it draws no card or strip, for frame captures).
    - Use (the `use` binding as bound) and a left click hurry the typing, then send `story:next`.
    - Skip, `key.gravesandglory.skip` (default Backspace), sends `story:skip`. On a first reading it is drawn under `clasp-closed` with "First reading", as in mockup 03. When unbound it is not drawn.
    - The hotbar 1 and 2 bindings pick a chit.
    - Escape opens `GameMenuScreen`. The reopen hook puts the same `SceneScreen` back, same card and same typed count, when the game menu closes.
  - `DialogCard` (canvas px, mockup 04):
    - the card is `story/card-404x104` at (278,376), with `mini-66` at (14,19) and the portrait window 60 at (17,22);
    - the label is `heading` ink at (96,14), followed by `voice-mark` for a voiced line;
    - text is `body` at (96,39) with 17 px lines: at most 3 lines at a 294 px measure, and the third line at most 198, because it shares its row with Continue;
    - Continue (the use binding's glyph and "Continue") sits at (298,72) once typed;
    - a speaker with portrait `-` has no frame, and the text starts at x 24;
    - the item card is `card-item-404x128`: 5 lines at a 356 px measure from (24,18);
    - with chits, the card rises to y 340, and the chits are `chit-404x28` at (278,450) and (278,482), each with its hotbar key cap and its text at (48,6), with a 346 px measure;
    - typing is 2 characters a tick (`DialogCard.CPT = 2`), or stretched to the voice's length when the voice exists, with the caret blinking at `CARET_MS`;
    - `static List<String> wrap(String text,int measure,int lastMeasure)`.
  - Portraits are live models drawn with `Portrait.draw(g,e,x,y,60,tall)`:
    - `troop:<id>` is a fresh, uncached `ArmyMobs.portrait(row.entity(),world)` named `ArmyMobs.stageName(row,1)` (so `TroopModels.resolve` gives it its own model), one per speaker per scene, with `tall` from `TroopModels.portraitTall`;
    - `npc:contracts` is a client `VillagerEntity` with the librarian profession at level 5, as the camp's Archivist;
    - when a `pose` op sets `helm_off` on the speaker's own actor, the card calls `TroopAnimator.setScenePose(portrait.getId(),"helm_off")`;
    - every portrait pose clears when the scene closes.
  - `StoryVoice.play(String id)->int`: it plays `gravesandglory:voice.<id>` when the sound exists and returns its length in ticks (`OggLength`), or 0.
  - `OggLength.seconds(InputStream ogg)->double`: the last page's granule position over the identification header's sample rate.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write the failing tests.**
  - StoryCheck gains `p4-ogg-length`: `t_voice.ogg` reads 2.0 s, within 0.05.
  - Suite `scenes`, played at 1920x1080 and GUI scale 2:
    - `p4-scene-card`: with `c1_third_name` NEXT, the war table opens `SceneScreen`, not the lobby page.
      - Card 1's label is `The sergeant`, and card 2's is `Maud`. Text types 2 characters a tick.
      - At `end` the id is in `StoryState.read`, the profile is saved, and the next use of the table opens the lobby page.
      - Screenshot `p4-card-third-name`.
    - `p4-card-portrait`:
      - Maud's card records a portrait draw of a `sentinel` named by `stageName`;
      - `c1_archive`'s card records a librarian villager;
      - a raider card draws no frame.
      - Screenshot `p4-card-portrait`.
    - `p4-scene-voice`: `test_voice` (voice `t_voice`) types over 40 ticks, within 2, and plays the sound. A voiced id with no file types at 2 a tick, in silence.
    - `p4-scene-keys` (Review Focus 5), on `test_keys`:
      - with use rebound to F, the Continue hint shows F, and F completes the line and then advances;
      - a left click advances the same way;
      - skip on the first reading leaves the op index unchanged and draws the clasp; once the scene is read, a replay's skip reaches `end`;
      - with skip unbound, no skip cap is drawn;
      - holding W for 40 ticks leaves the player's position unchanged;
      - Escape opens `GameMenuScreen`, and "Back to Game" returns to the same card with its typed count;
      - on a chit card, hotbar 2 sends `story:choice:plain`.
    - `p4-scenes-fit` (Review Focus 3), at GUI scale 1, 2 and 3 and at 854x480:
      - every card text of every scene file and of `storyLines` and `graveRoll` wraps within 3 lines (the third at 198 px);
      - every item text within 5 lines;
      - every caption within 3 lines at 556 px;
      - every label plus the voice mark within 294 px;
      - every chit within 346 px;
      - a failure names the voice id, and the line goes back to the copy.
- [ ] **Step 3: Run StoryCheck and suite `scenes`; expect FAIL** (`OggLength` and `SceneScreen` not defined).
- [ ] **Step 4: Implement** the classes, the channel, the commands, `atStation`, the hooks and the binding.
- [ ] **Step 5: Run StoryCheck (the Test runs line, now with `OggLength`) and suite `scenes`.**
  - **Expect** `StoryCheck: 11 checks passed`, the five ids, and `pass p4-scenes-suite`.
  - The tester grades `p4-card-third-name` and `p4-card-portrait` against the rubric. Below 9 is a fix round.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{Story,ScenePlayer,SceneScreen,DialogCard,StoryVoice,OggLength,GravesAndGlory,Campaign,GravesAndGloryClient}.java src/main/java/gg/gravesandglory/mixin/CinematicHudMixin.java src/main/resources/assets/gravesandglory/lang/en_us.json test-tools/resources/assets/gravesandglory test-tools/resources/data/gravesandglory/scenes/test test-tools/java/gg/gravesandglory/Phase4Checks.java test-tools/standalone/StoryCheck.java
git commit -m "Phase 4: scene runtime, dialog cards with live portraits, card scenes"
```

---

### Task 17: Stage scenes: camera, actors, sky, fog, hand-offs and interruption

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{SceneMath,SceneCamera,SceneSky,ScenePoses,MarchCard}.java` (`SceneMath` pure)
- Create: `src/main/java/gg/gravesandglory/mixin/{SceneSkyMixin,SceneFogMixin}.java`, registered in `gravesandglory.mixins.json` (client). There is no illager mixin: Phase 2's `TroopAnimator` covers every rig.
- Create: `test-tools/resources/data/gravesandglory/scenes/test/test_stage.scene`
- Modify:
  - `ScenePlayer.java`, `Story.java`, `SceneScreen.java`, `EntranceCameraMixin.java` (`SceneCamera` is sampled first);
  - `Campaign.java`:
    - the pending start plays the before-scene;
    - `finish` defers `returnLobby`;
    - `Campaign.leash` skips a player in a scene;
    - `switchMap` aborts scenes;
  - `GravesAndGlory.java`: DISCONNECT and SERVER_STOPPING abort; SERVER_STARTED recovers.
- Modify: `sheets/design.json` (new `storySets`, first row `c1_b3_gate`), `tools/preflight.py`, `Phase4Checks.java`, `StoryCheck.java` (`p4-camera-math`)

**Sheet `storySets`** (`id, kind, scene, piece, map, x, y, z, rotation, camX, camY, camZ, lookX, lookY, lookZ`):
- `kind` is one of:
  - `dressing`: `structure` places `piece` on `map` at (x,y,z) and `rotation`;
  - `panel`: Task 18; the piece goes on the stage area, with the camera at `cam` looking at `look`, both relative to the set's centre;
  - `thumb`: Task 18; a capture camera only, piece `none`, map `-1` for the camp.
- First row: `c1_b3_gate`, `dressing`, scene `c1_b3_before`, piece `ironhold/gate_intact`, map 1, (-37,64,0), rotation 0, and every camera cell `0.0`.
- Preflight rules:
  - `kind` is in the list;
  - `scene` is a `storyPages` id;
  - `piece` is a `pieces` id, or `none` for a thumb;
  - `map` is a map index, or -1 on a thumb;
  - `rotation` is 0, 90, 180 or 270;
  - every `structure` and `panel` argument in a scene file names a row of that kind and scene.

**Interfaces:**
- Consumes:
  - Task 16: `ScenePlayer`, `Story.play`, `SceneScreen`, the ops.
  - Task 5: `Structures.placeSaving(...)->Saved`, `restore`, `Saved.toJson`/`fromJson`.
  - Phase 2 Task 5: `ArmyMobs.create(String,World)`, `ArmyMobs.stageName`, `Battle.joinSide(MobEntity,int team)`, `Battle.shield(Spec.TroopsRow,String dye)`.
  - Phase 2 Task 5b: `TroopAnimator.setScenePose(int,String)`, whose poses move into place over 0.4 to 0.8 s and hold.
  - Phase 1: `Campaign.soloStart`, `returnLobby`, `toLobby`, `switchMap`, the pending battle, `soloRefusal(MARCH_SOLO)`.
- Produces `SceneMath` (pure):
  - `static double ease(double t)`: `t*t*(3-2t)`;
  - `static double[] at(double[] from,double[] to,double t,boolean ease)`;
  - `static float[] look(double[] eye,double[] target)`: `{yaw,pitch}` in Minecraft's convention (yaw 0 faces +z, -90 faces +x, and a positive pitch looks down).
- Produces (server, in `ScenePlayer`):
  - **The player.** At the start the player's position, rotation and game mode are saved. The player turns spectator and moves to the scene's first camera point.
    - A scene with a `commander` actor is different: the player stays in adventure at that actor's point and facing, frozen by `SceneScreen`. The client draws the local player as in third person (`SceneCamera` sets the camera's third-person flag). The scene waits for `story:arrived` (sent by the client once the chunks within 2 of that point are loaded) or 100 ticks. At the end the player is put back, unless the scene hands to a battle.
  - **Sky and fog.** `time`, `weather` and `fog` send `{"op":"sky","time","weather"}` and `{"op":"fog","density","colour"}`. The server's world clock and weather never change.
  - **Actors.**
    - `actor` spawns `ArmyMobs.create(row.entity(),w)` named `ArmyMobs.stageName(row,1)`, with AI off, invulnerable, silent and persistent, and command tag `gg_scene` only, so no unit code sees it.
    - `team` calls `Battle.joinSide`. A troop with a shield gets `Battle.shield` in that side's dye.
    - `companion:<s>` is that speaker's `troop`.
    - `walk` moves the actor along its line each tick, facing its travel.
    - `pose` sends `{"op":"pose","entity","pose"}`.
    - `prop` spawns an `item_display` tagged `gg_scene`. A `prop` line naming a placed prop moves that prop instead.
  - **Effects.** `structure` calls `placeSaving` with tag `scene-<uuid>-<set>`. `sound` plays to the player at the camera, and `particles` goes through `ServerWorld.spawnParticles(player,...)`.
  - **Camera ops:**
    - `{"op":"camera","from","to","ticks","look","entity","ease":true}`;
    - `{"op":"camera","hold","entity"}`;
    - `{"op":"camera","player":true}`.
  - **The marker.** `<world>/gravesandglory/scene-marker.json` holds every live save and actor UUID. `Story.abort(UUID player,String why)` and `Story.recover(MinecraftServer)` use it.
- Produces (hand-offs, in `Campaign` and `Story`):
  - **Before a battle.**
    - When the pending battle's map is ready and its before-scene is unread, the scene plays first.
    - At its end the client holds the last frame and shows the `MarchCard` (`{"op":"march","battle"}`).
    - `story:march` runs today's start: `soloStart` after the same map and solo checks.
    - `story:book` sends the player to camp with nothing started; Task 24 reopens the book there.
    - A read before-scene replayed from the book returns to the book at its end (assumption).
  - **After a battle.** On a battle's first win with an unread after-scene, `finish` keeps its order up to `returnLobby()`. The scene plays on the map, then `returnLobby()` runs, so the report opens after it.
  - **Abort.** `Story.abort` is called on a disconnect, a server stop, `switchMap` and `startRound`. It:
    - discards `gg_scene` entities and restores the scene's saves;
    - puts the player back unless they are gone;
    - sends `close`;
    - leaves the scene unread;
    - runs the follow-up: an after-scene's `returnLobby()` always, and a before-scene's pending start, which rechecks the map and the solo rule.

    `recover` at server start restores every save in the marker, discards its actors and deletes the file.
- Produces (client):
  - **`SceneCamera`:** `active()`, `set(JsonObject op)` and `sample(float partial)`, with `x,y,z,yaw,pitch`.
    - A move eases its position with `SceneMath` and faces its `look`, its actor's eye, or its travel.
    - `hold` tracks the actor's eye every frame.
  - **`SceneSky` with `SceneSkyMixin`:**
    - `ClientWorld.Properties.getTimeOfDay` returns the scene's time;
    - `World.getRainGradient` and `getThunderGradient` return the scene weather's 0 or 1;
    - for `snow`, the precipitation drawn at the camera is snow.
  - **`SceneFogMixin`:**
    - `BackgroundRenderer.applyFog` (TAIL) sets the fog end to `max(8, viewDistance*(1-density))` and the start to `0.15*end`;
    - `BackgroundRenderer.render` (TAIL) sets the clear and fog colour when a colour is given.
  - **`ScenePoses.apply(int entityId,String pose)`** calls `TroopAnimator.setScenePose` and records the id. `clearAll()` runs on `close`.
  - **`SceneScreen` for stage scenes:**
    - `story/bar-top` at (0,0) and `bar-bot` at (0,492), sliding in over 10 ticks;
    - the title in `heading` gilt at (24,15);
    - the skip box at (743,12), as in mockup 04;
    - fades step by `FADE_STEP`.
  - **`MarchCard.draw(DrawContext g,Spec.CampaignBattlesRow row,List<String> company)`**, mockup 05:
    - `card-400x394` at (280,73);
    - the name in `focal`, centred at x 200, top 22;
    - the reason in `body` sepia, top 54;
    - `rule-orn` at (140,78);
    - the battle's thumbnail plate (Task 18) at (30,94);
    - the map name in `heading` at (188,102); the mode name (`modes.name`) and the difficulty at (188,128) and (188,148);
    - "The company" at top 194, and the company row: 46 px frames 70 apart, the first at x `200-(46n+24(n-1))/2`, top 216, names centred at top 266, the wounded and resting greyed;
    - "March" on `btn-brass-240x40` at (80,298), with an Enter cap;
    - "Back to the book" on `tag-164x28` at (118,350), with an Esc cap.

    While the card shows, Enter sends `story:march` and Escape sends `story:book`. Strings come from the rows and NOTES.md ruling 7.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write the failing tests.**
  - `p4-camera-math` (StoryCheck):
    - `ease(0.25)==0.15625`;
    - `at` at t 1 is `to`;
    - `look` from the origin to (1,0,0) is yaw -90, and to (0,-1,1) is pitch 45.
  - Suite `scenes` gains `p4-stage-fixture`. `test_stage.scene` (map 1, time 18000, rain, fog 0.6 2a3a4a, actor `knight` team 1 posed `kneel`, a prop, a move with `look`, a hold, `structure c1_b3_gate`) plays:
    - the client's time reads 18000 while the server's is unchanged;
    - rain is 1, and the fog end and colour match;
    - the actor has AI off and tag `gg_scene`, and its kneel pose is recorded;
    - the camera ends the move within 0.1 block and 1 degree;
    - the gate cells are unwalkable during the scene and walkable after;
    - the player is spectator during it and back in adventure at their camp spot after.
    - Screenshot `p4-stage-fixture`.
  - `p4-before-hands-off`: `march-3` with its before-scene unread.
    - The scene plays, then the march card draws every field. Enter starts a Siege round, and the scene is read.
    - A second run's Escape leaves the player in camp with no round running.
    - Screenshot `p4-march-card`.
  - `p4-after-defers`: `march-1`'s first win plays `c1_b1_after` before `returnLobby()`, and the report opens after it. A later win returns at once.
  - `p4-scene-interrupt` (Review Focus 2), during `test_stage`:
    - (a) the disconnect handler's abort;
    - (b) `switchMap(2)`;
    - (c) a planted marker with a save and an actor UUID, recovered at the next start.

    After each:
    - the camera, sky and fog overrides are off and the HUD is back;
    - the game mode and position are restored;
    - no `gg_scene` entity is left, and the open gate is back;
    - the scene is unread.

    An interrupted after-scene still runs `returnLobby()` and the report opens. An interrupted before-scene gives today's "The battlefield changed" notice.
- [ ] **Step 3: Run StoryCheck and suite `scenes`; expect FAIL.**
- [ ] **Step 4: Implement** the classes, the mixins, the sheet, and the `Campaign`, `Story` and harness changes.
- [ ] **Step 5: Run StoryCheck (the Test runs line, with `SceneMath`) and suites `scenes,siege,march`.**
  - **Expect** `StoryCheck: 12 checks passed`, every `scenes` id, and `pass p4-scenes-suite`.
  - Task 5's and Task 11's ids pass as before.
  - The tester states one sentence for each screenshot and grades `p4-march-card`.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{SceneMath,SceneCamera,SceneSky,ScenePoses,MarchCard,ScenePlayer,Story,SceneScreen,Campaign,GravesAndGlory,Spec}.java src/main/java/gg/gravesandglory/mixin/{SceneSkyMixin,SceneFogMixin,EntranceCameraMixin}.java src/main/resources/gravesandglory.mixins.json sheets/design.json tools/preflight.py test-tools/resources/data/gravesandglory/scenes/test/test_stage.scene test-tools/java/gg/gravesandglory/Phase4Checks.java test-tools/standalone/StoryCheck.java
git commit -m "Phase 4: stage scenes (camera, actors, sky, fog), march card, hand-offs, abort"
```

---

### Task 18: Book panels, the stage area and frame capture

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/StageArea.java`
- Create: `test-tools/resources/data/gravesandglory/scenes/test/test_panel.scene`
- Modify:
  - `ScenePlayer.java`, `SceneScreen.java`, `SceneCamera.java` (a linear drift), `Story.java` (`recover` also clears the stage);
  - `DarkUi.java`: `WAX = 0xff8a2b1f`, the mockups' wax red, for the rubricated initial;
  - `sheets/design.json`: twelve `thumb` rows;
  - `tools/build_ui_art.py`: `--thumbs <dir>`, and thumbnails in `--verify`;
  - `Phase4Checks.java`: suite `story-capture`, and two ids in `scenes`;
  - `test-tools/isolated-checks.gradle`: `p4Capture` from `GRAVES_P4_CAPTURE`, default `''`, and `p4Look` from `GRAVES_P4_LOOK`, `off` or `reimagined`, default `off`.
- Create (captured, committed): `src/main/resources/assets/gravesandglory/textures/gui/story/thumb/<id>.png` for the six card scenes and the six battles.

**Interfaces:**
- Consumes:
  - Task 17: `storySets`, `SceneCamera`, `ScenePlayer`'s player handling, `story:arrived`, the marker, `SceneScreen.hideChrome`.
  - Phase 3: `Structures.place(ServerWorld,String,int,int,int,int)->Placed` and `Placed.box`.
  - The UI plan: `Phase1Checks.window(c,w,h,scale)` and the capture suites' mod, shader and resource pack copy; `Presentation.selectShader`.
- Produces `StageArea` (server):
  - `static final BlockPos CENTRE = new BlockPos(0,64,3000)`. It is flat-world air, 2,700 blocks past the camp (z -310) and every map.
  - `static Structures.Placed place(ServerWorld w,Spec.StorySetsRow row)`: clears the last set's box (grown by 1) to air, places `row.piece()` centred on `CENTRE` at `row.rotation()`, forces the box's chunks while a scene uses it, and writes the box to `<world>/gravesandglory/stage.json`.
  - `static void clear(ServerWorld w)`.
  - `static Vec3d camera(Spec.StorySetsRow row)` and `look(Spec.StorySetsRow row)`: `CENTRE` plus the row's offsets.
  - `static final Map<String,Spec.StorySetsRow> FIXTURES`, a harness hook consulted after `Spec.STORYSETS`.
- Produces `panel <set> [drift dx dy dz over <ticks>]` in `ScenePlayer`, in order:
  1. after the first panel, `fade out 10`;
  2. `StageArea.place`;
  3. the player moves to `camera(row)`;
  4. wait for `story:arrived`;
  5. a camera op from `camera(row)` to `camera(row)+drift` over the ticks, looking at `look(row)`, with `ease` false;
  6. `fade in 10`.

  In a panel scene, `actor`, `prop`, `walk`, `particles` and `camera` coordinates are offsets from `CENTRE`.
- Produces (client, mockup 03):
  - the title in `heading` gilt at (24,18), and `story/pips-<n>-<i>` at (24,42);
  - `caption-strip` at (180,438), with the caption typed in `heading` ink at (22,15), 556 wide, its first letter in `WAX`;
  - the skip box at (743,14), and Continue on `tag-140x28` at (807,484);
  - no letterbox.
- Produces the capture tooling:
  - **Suite `story-capture`** takes, for each id in `p4Capture`, a 1280x720 frame with the HUD hidden and `SceneScreen.hideChrome` set, saved as `p4-thumb-<id>.png`:
    - a book scene: its first panel, 40 ticks after the fade in;
    - a stage scene: its last card;
    - a card or battle scene: its `thumb` row's camera.

    With `p4Look` `reimagined` it first calls `Presentation.selectShader(1)` and waits 400 ticks, as the title capture does. For every panel and every stage scene's last card it also saves the set's grading frame, `p4-set-<scene>-<n>-<look>.png`.
  - **`python tools/build_ui_art.py --thumbs <screenshots dir>`** centre-crops each `p4-thumb-<id>.png` to 16:9 and resizes it with Lanczos to 256x144 at `textures/gui/story/thumb/<id>.png`.
    - `--verify` fails on any thumbnail that is not 256x144 or whose id is not a `storyPages` id, and prints `<k> of 26 thumbnails`.
    - Thumbnails are drawn with `DrawContext.drawTexture` from 256x144 into 128x72 canvas px, outside the atlas.

**`thumb` rows** (twelve, piece `none`):
- `c1_third_name`, `c1_letter` and `c1_council` at the command tent's war table, each framed differently;
- `c1_archive` and `c1_rule` at the archive's Stone;
- `c1_shrine_2` at the graveyard shrine;
- `battle_1` to `battle_6` on their maps (`campaignBattles.map`).

Camp rows use map -1 with camp coordinates. A battle's camera is a high three-quarter view that shows at least two of its points and its mode's site: the gate for Siege, the shrine for Survival, both stands for Banner. The executor chooses the numbers, and the tester's sentence checks them.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write the failing tests** (suite `scenes`).
  - `p4-stage-area`: fixture sets A and B (`StageArea.FIXTURES`, both on the Phase 3 marker piece and one larger test piece).
    - Placing A then B leaves no block of A outside B's box, and `stage.json` names B's box.
    - A planted `stage.json` is cleared at the next start.
    - `CENTRE` lies at least 2000 blocks from the camp and from every map's built bounds.
  - `p4-panel-fixture`: `test_panel.scene` plays two panels with drift (2,0,0) over 100 ticks.
    - Each panel's camera starts at `camera(row)` and ends 2 blocks along x, within 0.1.
    - The strip types the caption with its first letter in `WAX`, and the pips show 1 of 2, then 2 of 2.
    - The player is spectator at the stage area during the scene, the leash never fires, and the player is back at their camp spot after.
    - Screenshot `p4-panel-fixture`.
  - `p4-capture-thumbs`:
    - suite `story-capture` with `GRAVES_P4_CAPTURE` set to the twelve thumb ids writes twelve 1280x720 frames;
    - `--thumbs` writes twelve 256x144 PNGs;
    - `--verify` prints `12 of 26 thumbnails`.
- [ ] **Step 3: Run suite `scenes`; expect FAIL** (`StageArea` not defined).
- [ ] **Step 4: Implement** `StageArea`, `panel`, the panel chrome, the drift, the capture suite, `--thumbs` and the twelve rows.
- [ ] **Step 5: Run suite `scenes`, then `story-capture` twice** (`GRAVES_P4_LOOK` `off`, then `reimagined`, with the UI plan's mod and shader copy into the run root).
  - Run `--thumbs` on the `reimagined` frames; they are the committed thumbnails, as the title panorama was taken with shaders on.
  - **Expect:**
    - every `scenes` id passes, with `pass p4-scenes-suite`;
    - `pass p4-story-capture-suite`;
    - `build_ui_art: <n> textures verified` and `12 of 26 thumbnails`.
  - The tester states what each thumbnail shows, at plate size and at full size, and grades the twelve against the rubric. Below 9 is a fix round on the row's camera.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{StageArea,ScenePlayer,SceneScreen,SceneCamera,Story,DarkUi,Spec}.java sheets/design.json tools/build_ui_art.py src/main/resources/assets/gravesandglory/textures/gui/story/thumb test-tools/resources/data/gravesandglory/scenes/test/test_panel.scene test-tools/java/gg/gravesandglory/Phase4Checks.java test-tools/isolated-checks.gradle
git commit -m "Phase 4: book panels on the stage area, frame capture, card and battle thumbnails"
```

---

### Task 19: Sets for `c1_fall`, the five panels of the Fall

**Executor:** Opus 5.5 max. The tester is Sonnet 5.5 max.

Tasks 19 to 23 are content work: they build sets in the world under Kort's footage ruling. They need Phase 2's own models for every troop on a set (Phase 2 Tasks 5 to 10, the unit identity pass the story spec's order of work puts first). A troop still drawn by its vanilla renderer fails the grade.

**The set recipe** (Tasks 19 to 23 follow it for each set):
1. **Author the piece.**
   - Write the piece as a Phase 3 studio script: `test-tools/resources/data/gravesandglory_studio/functions/story/<set>.mcfunction`, with the size header and one command a line.
   - Export it with `GRAVES_PHASE3_CHECKS=studio GRAVES_STUDIO_MODE=export GRAVES_STUDIO_PIECES=story/<set>` and copy it to `src/main/resources/data/gravesandglory/structures/story/<set>.nbt`.
   - Add its `pieces` row (size equal to the export) and its `storySets` row.
   - A panel piece carries its own ground and backdrop out to the edge of its camera's view.
2. **Stage the scene.** Add the lines to the scene file before, between and after its cards: `map` or `panel`, `time`, `weather`, `fog`, `actor`, `prop`, `pose`, `walk`, `camera`, `structure`, `sound`, `particles`, `wait` and `fade`. The brief is the copy's place line and staging note for the scene.
   - Times: day 6000, dusk 12500, night 18000, dawn 23300.
   - No card, caption or item text changes.
   - No legible words appear on a set except object text the copy gives.
   - What the commands cannot show goes back to the copy as a staging question; the builder adds no command.
3. **Check the scene.** Run suite `scenes` with `GRAVES_P4_SCENES=<ids>`.
4. **Capture it.** Run `story-capture` for the ids in both looks (`GRAVES_P4_LOOK` `off` and `reimagined`).
   - The tester quotes the claim (the copy's place and staging note) and states in one sentence how each grading frame meets it.
   - The tester grades each frame against `work/ui-concepts/GRADING-RUBRIC.md`, adapted to world shots as the walkthrough gate adapts it to pieces.
   - Below 9 in either look is a fix round on the set or its staging.
   - Then `--thumbs` makes the scene's thumbnail from its `reimagined` frame.
5. **Commit** the scripts, pieces, rows, scene file and thumbnail.

**Files:**
- Create: `story/c1_fall_1` to `story/c1_fall_5` (scripts and `.nbt`), `textures/gui/story/thumb/c1_fall.png`
- Modify: `scenes/c1/c1_fall.scene`, `sheets/design.json` (five `pieces` rows, five `panel` rows), `Phase4Checks.java` (the per-scene ids, this task only)

**Interfaces:**
- Consumes: Task 18 (`panel`, `StageArea`, `story-capture`, `--thumbs`); Task 17 (actors, props, poses, sky, fog); Phase 3's `StructureStudio`; Phase 2's `TroopAnimator` poses, through the scene's `pose` lines.
- Produces, in suite `scenes`, `p4-scene-<id>` for each id in `GRAVES_P4_SCENES` (default none; Task 27 lists all twenty). Each plays the scene headless with typing hurried and checks:
  - each card draws its text and label inside its rect;
  - each camera op reaches its end point;
  - each actor and prop stands at its point, and each pose is recorded;
  - each `structure` is placed during the scene and restored after;
  - each `panel` places its set and reaches its camera point;
  - a stage scene has `map`, `time` and at least one `camera`, and a panel scene has a `panel` before its first caption.

  Each takes screenshot `p4-scene-<id>` at the last card.

**The five panels** (the story spec's section 6 and the copy's panel table; no names on screen; the climber is a silhouette):
1. `c1_fall_1`, a river in fog: fog with colour, and the Tide coming up the river in it.
2. `c1_fall_2`, a table with the pact: the pact as a prop, with the seal set to it.
3. `c1_fall_3`, the highest mound under the moon: a small figure climbing it against the sky, holding something in both hands. It is small because it is far.
4. `c1_fall_4`, the mounds against the sea: the kings and their hosts standing up out of the mounds, and the Tide breaking.
5. `c1_fall_5`, the dead king and the lieutenant: a lieutenant lifting a crown prop from a dead king's head.

Each panel's drift is slow: under 3 blocks over its caption.

- [ ] **Step 1: Compile both source sets.** Run suite `scenes` with `GRAVES_P4_SCENES=c1_fall`. **Expect FAIL** (`p4-scene-c1_fall`: no panel before the first caption).
- [ ] **Step 2: Build and stage the five sets** by the recipe. Run preflight; expect `Preflight clean`.
- [ ] **Step 3: Run suite `scenes` with `GRAVES_P4_SCENES=c1_fall`.** Expect `pass p4-scene-c1_fall` and `pass p4-scenes-suite`.
- [ ] **Step 4: Capture and grade** by the recipe: ten grading frames, each 9 or better, and `13 of 26 thumbnails`.
- [ ] **Step 5: Commit.**
```bash
git add test-tools/resources/data/gravesandglory_studio/functions/story src/main/resources/data/gravesandglory/structures/story src/main/resources/data/gravesandglory/scenes/c1/c1_fall.scene sheets/design.json src/main/java/gg/gravesandglory/Spec.java src/main/resources/assets/gravesandglory/textures/gui/story/thumb/c1_fall.png test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: the Fall's five panel sets, staged and graded"
```

---

### Task 20: Sets for `c1_crown`, the two panels of the Highest Mound

**Executor:** Opus 5.5 max. The tester is Sonnet 5.5 max.

**Files:**
- Create: `story/c1_crown_1` and `story/c1_crown_2` (scripts and `.nbt`), `textures/gui/story/thumb/c1_crown.png`
- Modify: `scenes/c1/c1_crown.scene`, `sheets/design.json` (two `pieces` rows, two `panel` rows)

**Interfaces:**
- Consumes: the set recipe and `p4-scene-<id>` (Task 19); Task 4's `kingsfield/mounds`. A script may place it with the vanilla `place template gravesandglory:kingsfield/mounds` line, so the panels' mounds match the map's.

**The two panels** (the copy's `c1_crown` note):
1. `c1_crown_1`, dawn on the first king's mound, the highest: Wren (`companion:wren`) small on its top, holding a crown prop in both hands in the `raise` pose, the way she once held a horn.
2. `c1_crown_2`, the mounds open behind her, all of them: an open-mound variant of the set, the dead standing in them. The scene then fades to black (`fade out 40`) and ends with no words on the black.

- [ ] **Step 1: Compile both source sets.** Run suite `scenes` with `GRAVES_P4_SCENES=c1_crown`. **Expect FAIL** (no panel before the first caption).
- [ ] **Step 2: Build and stage both sets** by the recipe. Run preflight; expect `Preflight clean`.
- [ ] **Step 3: Run suite `scenes` with `GRAVES_P4_SCENES=c1_crown`.** Expect `pass p4-scene-c1_crown`. The last screenshot is black.
- [ ] **Step 4: Capture and grade** by the recipe: four grading frames, each 9 or better, and `14 of 26 thumbnails`.
- [ ] **Step 5: Commit.**
```bash
git add test-tools/resources/data/gravesandglory_studio/functions/story src/main/resources/data/gravesandglory/structures/story src/main/resources/data/gravesandglory/scenes/c1/c1_crown.scene sheets/design.json src/main/java/gg/gravesandglory/Spec.java src/main/resources/assets/gravesandglory/textures/gui/story/thumb/c1_crown.png
git commit -m "Phase 4: the Highest Mound's two panel sets, staged and graded"
```

---

### Task 21: Sets for Rivergate's four stage scenes

**Executor:** Opus 5.5 max. The tester is Sonnet 5.5 max.

**Files:**
- Create: the dressing scripts and pieces `story/<scene>_<name>` that the scenes need (at least `c1_b1_after_fire`, `c1_b4_before_barges` and `c1_b4_after_fire`)
- Create: `textures/gui/story/thumb/{c1_b1_before,c1_b1_after,c1_b4_before,c1_b4_after}.png`
- Modify: `scenes/c1/{c1_b1_before,c1_b1_after,c1_b4_before,c1_b4_after}.scene`, `sheets/design.json` (`pieces` rows and `dressing` rows on map 0)

**Interfaces:**
- Consumes: the set recipe and `p4-scene-<id>` (Task 19); Task 17's actors (`companion:`, `commander`, `team`), props, poses, `structure` and the hand-offs; Task 7's Rivergate stands (Mill Hamlet at (-39,-145)) for the mill.

**The four scenes** (map 0; each brief is the copy's place line and staging note):
- `c1_b1_before`, day:
  - Maud and the commander on the south bank;
  - the turncoats' colours on the three crossings, as `knight` actors on `team 1`, whose pennants carry the colour.
  - It hands to `battle_1`, and its last frame is mockup 05's held frame.
- `c1_b1_after`, dusk:
  - the fire on the north bank, with Maud seated, counting, and the company eating;
  - sentries and lanterns on the crossing;
  - the pact-copy prop by the fire. Maud picks it up: a second `prop` line with the same id moves it to her hand.
- `c1_b4_before`: heavy coloured fog on the water, the mill wheel standing in it, and grain barges untouched at the bank. It hands to `battle_4`.
- `c1_b4_after`:
  - card 1 at the fire by the mill wheel;
  - the taken shroud, shown by its item card;
  - a dying raider (`tc_raider`, `team 1`) against the wheel, in the `sit` pose.

- [ ] **Step 1: Compile both source sets.** Run suite `scenes` with `GRAVES_P4_SCENES=c1_b1_before,c1_b1_after,c1_b4_before,c1_b4_after`. **Expect FAIL** (no `map`, `time` or `camera`).
- [ ] **Step 2: Build and stage** by the recipe. Every `dressing` piece is restored after its scene. Run preflight; expect `Preflight clean`.
- [ ] **Step 3: Run suite `scenes` for the four ids, then suites `march,banner`.**
  - **Expect** the four `p4-scene-*` ids and `pass p4-scenes-suite`.
  - Battles 1 and 4 still build and play on a clean map after their scenes; Task 7's and Task 11's ids pass.
- [ ] **Step 4: Capture and grade** by the recipe: each grading frame 9 or better in both looks, and `18 of 26 thumbnails`.
- [ ] **Step 5: Commit.**
```bash
git add test-tools/resources/data/gravesandglory_studio/functions/story src/main/resources/data/gravesandglory/structures/story src/main/resources/data/gravesandglory/scenes/c1 sheets/design.json src/main/java/gg/gravesandglory/Spec.java src/main/resources/assets/gravesandglory/textures/gui/story/thumb
git commit -m "Phase 4: Rivergate's stage sets (battles 1 and 4), staged and graded"
```

---

### Task 22: Sets for Hollowwood's and Ironhold's four stage scenes

**Executor:** Opus 5.5 max. The tester is Sonnet 5.5 max.

**Files:**
- Create: the dressing scripts and pieces `story/<scene>_<name>` the scenes need (at least `c1_b2_before_graves`, `c1_b2_after_fire`, `c1_b3_after_fire` and `c1_b3_after_graves`)
- Create: `textures/gui/story/thumb/{c1_b2_before,c1_b2_after,c1_b3_before,c1_b3_after}.png`
- Modify: `scenes/c1/{c1_b2_before,c1_b2_after,c1_b3_before,c1_b3_after}.scene`, `sheets/design.json` (`pieces` rows and `dressing` rows on maps 3 and 1)

**Interfaces:**
- Consumes:
  - the set recipe and `p4-scene-<id>` (Task 19);
  - Task 17's `c1_b3_gate` row;
  - Hollowwood's shrine and chapel landmarks (`hw-moon-shrine` (76,127), `hw-chapel` (70,100));
  - Task 5's Siege gate at (-37,64,0);
  - Phase 2's Ironclad troops for the engines and `ic_breaker` for the gate-warden.

**The four scenes** (each brief is the copy's place line and staging note):
- `c1_b2_before`, map 3, night: open graves along the track and the shrine lit. It hands to `battle_2`.
- `c1_b2_after`, map 3, dawn, then the fire:
  - no dead stand when the scene opens;
  - the last of the dead (Wren, `companion:wren`) lies still in front of the commander (`fallen`), then stands (`stand`);
  - cards 4 to 6 are at the fire by the shrine.
- `c1_b3_before`, map 1, day:
  - `structure c1_b3_gate` stands;
  - the Ironclad engines in a row;
  - the gate-warden (`dunmar`, his troop) stands with his back to the gate.
  - It hands to `battle_3`.
- `c1_b3_after`, map 1:
  - card 1 at the fire in the keep yard;
  - then the smiths' graves, open, with one stone he reads and stops at. The stone carries no legible name.
  - The gate stands broken, as the battle left it.

- [ ] **Step 1: Compile both source sets.** Run suite `scenes` for the four ids. **Expect FAIL** (no `map`, `time` or `camera`).
- [ ] **Step 2: Build and stage** by the recipe. Run preflight; expect `Preflight clean`.
- [ ] **Step 3: Run suite `scenes` for the four ids, then suites `survival,siege,march`.**
  - **Expect** the four `p4-scene-*` ids and `pass p4-scenes-suite`.
  - Battles 2 and 3 still play on clean maps after their scenes: `p4-siege-setup` finds the open gate, not `gate_intact`.
- [ ] **Step 4: Capture and grade** by the recipe: each grading frame 9 or better in both looks, and `22 of 26 thumbnails`.
- [ ] **Step 5: Commit.**
```bash
git add test-tools/resources/data/gravesandglory_studio/functions/story src/main/resources/data/gravesandglory/structures/story src/main/resources/data/gravesandglory/scenes/c1 sheets/design.json src/main/java/gg/gravesandglory/Spec.java src/main/resources/assets/gravesandglory/textures/gui/story/thumb
git commit -m "Phase 4: Hollowwood's and Ironhold's stage sets (battles 2 and 3), staged and graded"
```

---

### Task 23: Sets for Frostmarch's and Kingsfield's four stage scenes

**Executor:** Opus 5.5 max. The tester is Sonnet 5.5 max.

**Files:**
- Create: the dressing scripts and pieces `story/<scene>_<name>` the scenes need (at least `c1_b5_before_lodge` and `c1_b5_after_fire`, unless Frostmarch already has a lodge, and `c1_king_after_fire`)
- Create: `textures/gui/story/thumb/{c1_b5_before,c1_b5_after,c1_king_before,c1_king_after}.png`
- Modify: `scenes/c1/{c1_b5_before,c1_b5_after,c1_king_before,c1_king_after}.scene`, `sheets/design.json` (`pieces` rows and `dressing` rows on maps 2 and 4)

**Interfaces:**
- Consumes:
  - the set recipe and `p4-scene-<id>` (Task 19);
  - Task 4's `kf-mounds` landmark, with the child's stone;
  - Task 16's portrait mirror of `helm_off`;
  - Task 17's `if` branches over `StoryState`;
  - Phase 2's `gb_hollowking` model, whose `helm` parts hide in `helm_off`.

**The four scenes** (each brief is the copy's place line and staging note):
- `c1_b5_before`, map 2, `weather snow`: snow falling, the lodge at the foot of the pass, and the treeline above it full of watchers (Wildmoot actors, `team 1`, still) who do not come down. It hands to `battle_5`.
- `c1_b5_after`, map 2, snow and a fire:
  - card 1 at the fire;
  - after card 1, Ashka (`companion:ashka`) walks out of the trees;
  - before card 3, `camera hold ... look ashka` while she looks at the standard a moment too long.
- `c1_king_before`, map 4, night:
  - the Hollow King (`king`, `gb_hollowking`, `team 1`), helmed and crowned, on the mound by the centre bridge;
  - her brother (`tomas`, `sentinel`, `team 1`) a step behind;
  - Maud beside the commander.
  - The battle starts when the last card closes (the march card follows).
- `c1_king_after`, map 4, dawn coming, the company's fire at the foot of the mound. Before card 1, the copy's branches:
  - `if match killed:gb_hollowking`, the King is `fallen` on the mound; `else` he `kneel`s, then falls;
  - `if companion:maud standing`, she kneels by him; `wounded`, two bearers `walk` her to him;
  - `pose king helm_off`, so his card portrait shows Edric's face from card 1 on;
  - after card 5, her brother is found `fallen` where the script put him, and Ashka's kin take him alive;
  - Dunmar's engines stand silent, and Ashka looks at the mounds;
  - the camera passes the child's stone, and nobody reads it.

- [ ] **Step 1: Compile both source sets.** Run suite `scenes` for the four ids. **Expect FAIL** (no `map`, `time` or `camera`).
- [ ] **Step 2: Build and stage** by the recipe. Run preflight; expect `Preflight clean`.
- [ ] **Step 3: Run suite `scenes` for the four ids,** with `c1_king_after` played four times: killed and not killed, each with Maud standing and with Maud wounded (`StoryState` fixtures).
  - **Expect** the four `p4-scene-*` ids for every branch, and the King's card portrait recorded with `helm_off` from card 1.
  - Then run suite `march`; battles 5 and 6 still play on clean maps.
- [ ] **Step 4: Capture and grade** by the recipe. Each branch of `c1_king_after` has its own grading frame. Each frame is 9 or better in both looks, and `--verify` prints `26 of 26 thumbnails`.
- [ ] **Step 5: Commit.**
```bash
git add test-tools/resources/data/gravesandglory_studio/functions/story src/main/resources/data/gravesandglory/structures/story src/main/resources/data/gravesandglory/scenes/c1 sheets/design.json src/main/java/gg/gravesandglory/Spec.java src/main/resources/assets/gravesandglory/textures/gui/story/thumb
git commit -m "Phase 4: Frostmarch's and Kingsfield's stage sets (battles 5 and 6), staged and graded"
```

---

### Task 24: The storybook screen and the March plaque

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/StorybookScreen.java`
- Modify:
  - `WarTableHome.java`: the March plaque, and the Campaign card's Change;
  - `CampaignClient.java`: `story()`, `StoryState` read from the profile JSON that Task 10's `forClient` sends;
  - `Story.java`: `story:book`, and the `{"op":"book"}` op;
  - `SceneScreen.java`: a book scene or card scene played from the book returns to the book;
  - `Phase4Checks.java`: suite `storybook`;
  - the UI plan's home layout check (`p1-ui-home-layout`).

**Interfaces:**
- Consumes:
  - Task 13: `StoryBook.state`/`next`/`currentPage`/`company`, `StoryNames.label`, and the `storyPages` and `speakers` rows.
  - Task 15: `story/` textures. Task 17: `MarchCard`. Task 18: the thumbnails.
  - The UI plan: `WarTable.plaque(int x,int width,String icon,String label,String key,int keyX,int badge,PressAction a)`, `Portrait.draw` and `UiTex`.
  - Phase 2 Task 5: `ArmyCards.portrait(Spec.TroopsRow,int stage)` and `TroopModels.portraitTall`.
- Produces:
  - `StorybookScreen(int page)`: `open()` shows the table with the book closed, and `opened` is set after `WIND_TICKS` or on Enter. Plus `static int page()`, `static Map<String,int[]> rects` (harness) and `static Map<String,StoryBook.State> drawnStates` (harness).
  - `{"op":"book"}`: the client opens `StorybookScreen` on the current page.
  - `story:book`: the march card's Back. It sends the player to camp with nothing started, then sends `book`.

**Layout** (canvas px; mockups 01 and 02 with NOTES.md rulings 1 to 9; strings are the rows' and NOTES.md's approved UI labels):
- **Closed.**
  - `table-sb-closed`, `rail-sb`, and `book-closed` at (330,80).
  - On the cover: "The Long March" (`focal` gilt, centred x 486, top 129), `seal-hollow` at (461,239), "I" (top 327) and "The Road North" (`heading`, top 365).
  - The candle frames at (198,82), the inkwell at (700,330), and the leaves.
  - The rail's "War Table" on `plaque-144x32` at x 56, with Esc.
  - "Open the book" with an Enter cap at (400,503).
  - After `WIND_TICKS` the cover and three leaves turn, four frames each, and the book settles on `currentPage`. On a first open that is page 1.
- **Spread.**
  - `table-sb-open`, and `book-open` at (36,72).
  - The rail adds "Back a page" on `plaque-152x32` at x 212, with the left-arrow cap.
  - **The left leaf:**
    - the running head, centred x 230, top 89. It reads "I: The Road North", the title line's ASCII form, in place of the mockup's middle dot (reported back);
    - the page title in `focal`, centred x 230, top 118;
    - the passage in `body`, at (70,154), 320 wide, under `cap-<letter>`;
    - the company rows: at (66, 272+52i), 328x46, in join order, with nothing drawn for a companion not yet joined. Each row has `mini-46` with the live model in its 40 px window inset 3 (`Portrait.draw`, `ArmyCards.portrait(row,1)`), the label in `heading` at (52,5), and the want in `body` sepia at (52,25).
    - A wounded companion: `mini-46-grey`, the portrait under `RenderSystem.setShaderColor(0.55f,0.55f,0.55f,1f)`, and `bandage` at (34,-2).
    - A resting companion: grey too, `moon` at (37,-3), and "Resting" at (100,7).
  - **The right leaf:**
    - "Page N", centred x 614, top 89;
    - the page's rows as plates at (448, 118+128i), or 118+96i when a page has four;
    - in each plate, the thumbnail drawn 256x144 into the 128x72 window of its plate frame, the kind ("Book", "Stage", "Card" or "Battle") at (156,14), and the title in `heading` at (156,32);
    - by state:
      - READ: `plate-read`, `quill-tick` and "Read";
      - NEXT and OPEN: `plate-next`, `glow-next` breathing, and the `tab-brass-112x28` with Enter and "Play";
      - SEALED: `plate-sealed` and "Sealed until the battle is won.", faded;
    - a battle plate adds its mode glyph (`glyph-dom`, `glyph-siege`, `glyph-surv` or `glyph-banner`) on a tab over the thumbnail.
- **Turning** (ruling 6, back only). The lifted left corner, the left arrow, "Back a page" and the wheel up all turn back a page. The right arrow and the wheel down turn forward, but never past `currentPage`. Past the current page the leaves stay tied and sealed. A turn is four frames.
- **Acting.**
  - Clicking a READ, NEXT or OPEN plate, or pressing Enter on the NEXT one, sends `story:play:<id>`. A battle plate instead opens `MarchCard` over the book under `SWEEP_VEIL`, and its March sends `campaign:<id>`.
  - A SEALED plate sends nothing.
  - When `c1_crown` is in `StoryState.pending`, opening the book plays it before the spread. The book's other trigger, the return to camp, is Task 26's.
- **The War Table home.**
  - "The March" is a rail plaque: `WarTable.plaque(784,112,"story/icon-book","The March",null,0,0,...)`. It opens the book.
  - The Campaign card's Change also opens the book.
  - The camp guide blurb (`chapters.blurb`) is not placed. The copy says the spec is silent, so it is parked for Kort (Task 27).

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `storybook`** at 1920x1080, GUI scale 2:
  - `p4-book-open`: on a fresh profile, the March plaque shows the closed book and, after 40 ticks, the spread on page 1. Enter opens it at once. Screenshots `p4-book-closed` and `p4-book-spread`.
  - `p4-book-states`, over three fixtures (fresh; `march-1` won with `c1_b1_after` read; `march-5` won with Maud wounded and Wren resting):
    - `drawnStates` matches `StoryBook.state` for every row of the current page, and each state draws its pieces;
    - page 7 holds four plates at 96 spacing;
    - Maud's row is grey with the bandage, and Wren's has the moon and "Resting";
    - one screenshot per fixture.
  - `p4-book-play`:
    - a NEXT before-scene plays and hands to the march card;
    - a SEALED plate sends nothing;
    - a replay of a read scene accepts skip;
    - a battle plate's March sends `campaign:march-2`;
    - Back on a march card ends in the open book.
  - `p4-book-turn`: the arrows, the wheel and the corner turn back. Forward stops at the current page, each turn is four frames, and the forward block draws tied.
  - `p4-book-migrated` (Review Focus 4): a schema 2 profile with `campaignCleared` 3, migrated by Task 10.
    - The book opens on page 1 with `c1_fall` NEXT and an empty company.
    - After `c1_fall` and `c1_third_name`, page 2 is current: `battle_1` is READ and `c1_b1_after` is NEXT, and the company holds Maud alone.
    - The Pact count and `writChapters` are unchanged.
  - `p4-book-crown`: with `c1_crown` pending, the book plays it first.
  - `p1-ui-home-layout` gains the March plaque's box (784,12,112,28).
- [ ] **Step 3: Run suites `storybook` and UI `ui-home`; expect FAIL** (no plaque, no screen).
- [ ] **Step 4: Implement** `StorybookScreen`, the plaque, Change, `story()`, `story:book` and the return to the book.
- [ ] **Step 5: Run suites `storybook,scenes` and UI `ui-home,ui-pages`.**
  - **Expect** every id, `pass p4-storybook-suite` and the UI suites as before.
  - The tester grades `p4-book-closed`, `p4-book-spread` and the three state screenshots against the rubric, beside mockups 01 and 02. Below 9 is a fix round.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{StorybookScreen,WarTableHome,CampaignClient,Story,SceneScreen}.java test-tools/java/gg/gravesandglory/{Phase4Checks,Phase1Checks}.java
git commit -m "Phase 4: the storybook screen and the March plaque"
```

---

### Task 25: Companions as units, protection and the fall

**Executor:** Opus 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{Company,CompanyNames}.java`, `src/main/java/gg/gravesandglory/mixin/CompanyLabelMixin.java`
- Modify:
  - `GravesAndGlory.java`: `Unit.company` and `Unit.protectedUnit`; the `ALLOW_DEATH` floor; the snapshot `company` array;
  - `Campaign.java`: seating; `finish` fills `StoryState.last`, `wounded` and `resting` before the one save;
  - `Tactics.java`: targeting skips an untargetable unit;
  - `Houses.java`: `recompute` skips company units;
  - `FeedLines.java`: `fallen`;
  - `WarTablePages.java`: the company row on the banner page;
  - `gravesandglory.mixins.json`, and `Phase4Checks.java` (suite `companions`).

**Interfaces:**
- Consumes:
  - Task 11: campaign seating, `Squad.fixedBanner`, `finish`.
  - Task 10: `StoryState` (`wounded`, `resting`, `Facts`). Task 13: `StoryBook.company`, `speakers.troop` and `label`.
  - `Battle.spawn(Squad,Spec.TroopsRow,int slot,Vec3d)`.
  - Phase 2 Task 7: Graveborn's Rise Again rule, for Wren's one rise.
  - Phase 2 Task 5b: `TroopAnimator.setScenePose(int,String)`, for `fallen` and `kneel`.
  - `Unit.lastHitBy`.
- Produces (server):
  - `Company.field(Battle b,Squad player,Spec.CampaignBattlesRow row)`: at round start in a campaign battle only, it spawns each companion of `StoryBook.company` who is not wounded or resting. Each spawns through `Battle.spawn` with its `speakers.troop`, in slots 20 to 23 by join order, at the player's deploy point.
    - These units carry `Unit.company=<speaker id>` and `protectedUnit=true`.
    - They never enter reinforce, House trait tiers, stars or evolution, and they cost nothing.
    - Skirmish, Survival co-op and every multiplayer match field none.
  - **The floor.** A `protectedUnit` takes damage normally and never drops below 1 HP: `ServerLivingEntityEvents.ALLOW_DEATH` returns false and sets 1 HP. At 1 HP:
    - a companion falls: AI off, untargetable, state `fallen`, and the feed gets `FeedLines.fallen(label)` = `label+" has fallen"` (kind `loss`; a string not in the copy, reported back);
    - Wren's first fall in a battle rises by the Rise Again rule. Her second leaves her `resting` for the rest of the battle;
    - Tomas (the `sentinel` in slot 8 of `mix_gilded_graveborn_normal`, protected only in `march-6`) kneels, is untargetable, and his own AI stops;
    - the Hollow King is never protected.
  - **`finish` for a campaign battle** fills `StoryState.last` with `Facts(battle,won,fell,routed,bonus,losses,count,bannerSize,killed)`:
    - `routed`: the player's group routed;
    - `killed`: `gb_hollowking` died with `lastHitBy` the player's squad;
    - `fell`: companions in fall order.

    Each fallen companion but Wren becomes `wounded` until the finish of `MarchMath.nextUnwon` (or `next` when none is unwon), and Wren after a second fall becomes `resting` the same way. Any wound whose battle just finished heals first.
  - **The snapshot** gains `company`: `[{"id","entity","label","state","team"}]`, with `state` `standing`, `fallen`, `kneeling` or `resting`. Tomas appears in it with label `""`.
- Produces (client):
  - `CompanyNames.of(int entityId)->Entry` (`id`, `label`, `state`, `team`), from each snapshot. On a change to `fallen`, `kneeling` or `resting` it calls `TroopAnimator.setScenePose(entity,"fallen"|"kneel"|"fallen")`. Back to `standing` (Wren's rise) it sets `stand`.
  - `CompanyLabelMixin` (`EntityRenderer.renderLabelIfPresent`): it draws a company entry's label over the head in its side colour (`DarkUi.teamColor`). The custom name stays the troop name, because `TroopModels.resolve` reads it.
  - The banner page: inside `sheet-banner`, under the seats, "The company" (the approved label) in `heading` at (60,284), and up to four `mini-46` frames at (60+56i,304). Each frame holds the live model, greyed with `bandage` or `moon` as the book draws it.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `companions`:**
  - `p4-comp-field`: `march-4` with Maud, Wren and Dunmar in the company fields the player's 20 slots plus three company units in slots 20 to 22.
    - `Houses` tiers equal those of the banner alone.
    - No reinforce wave respawns a company unit.
    - `skirmish:start` and `camp:practice` rounds field none.
  - `p4-comp-fall`: Maud driven to 0 HP stays at 1 HP in `fallen`.
    - No enemy targets her for 200 ticks.
    - The feed shows `Maud has fallen`, and the label draws over her.
    - Screenshot `p4-comp-fallen`.
  - `p4-comp-wounded`: after that battle, `wounded` names `march-5`.
    - A replay of `march-2` fields no Maud, and her rows grey in the book and on the banner page.
    - Finishing `march-5`, won or lost, heals her.
    - Screenshot `p4-comp-banner-row`.
  - `p4-comp-wren`: Wren's first fall rises; her second leaves her `resting`, never `wounded`.
  - `p4-comp-tomas`: in `march-6`, Tomas at 1 HP kneels, and no bot targets him for 200 ticks. The Hollow King killed by the player's banner sets `killed` true.
  - `p4-comp-facts`: `last` after a fixture match (a fall, a rout, the bonus missed on a win, 9 losses of 20) holds those facts and `losses` 9. A fixture card holding `{losses}` then types `9`.
- [ ] **Step 3: Run suite `companions`; expect FAIL** (no company units).
- [ ] **Step 4: Implement** `Company`, the floor, the fall, the healing in `finish`, the snapshot array, `CompanyNames`, the mixin and the banner row.
- [ ] **Step 5: Run suites `companions,march,save`, and Phase 2 `graveborn,gilded`.**
  - **Expect** every id, `pass p4-companions-suite`, and the Phase 2 ids as before. The Rise Again rule is unchanged for troops.
  - The tester states one sentence for each screenshot.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{Company,CompanyNames,GravesAndGlory,Campaign,Tactics,Houses,FeedLines,WarTablePages}.java src/main/java/gg/gravesandglory/mixin/CompanyLabelMixin.java src/main/resources/gravesandglory.mixins.json test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: companions as protected campaign units; falls, wounds and Tomas"
```

---

### Task 26: The camp story: arrival lines, tents, the shrine, the Archivist and the Stone

**Executor:** Sonnet 5.5 max.

**Files:**
- Create: `src/main/java/gg/gravesandglory/StoryCamp.java` (client)
- Modify:
  - `Story.java`: `Line`, `line`, `dueArrival`, `story:camp`, `story:talk:<companion>`, the station lines in `atStation`, `recruited`, and two first-reading marks;
  - `StoryNames.java`: the state label; `Company.java`: `lastFielded`;
  - `SceneScreen.java`: a line scene opened over a screen puts that screen back; `DialogCard.java`: `wrap` starts a new line at each ` | `;
  - `CampScreen.java`: closing a campaign report sends `story:camp`;
  - `Campaign.java`: `finish` calls `Story.dueArrival`; the return after a campaign battle raises `shrineDue`; a Graveborn `roll` or `buy` calls `Story.recruited`;
  - `GravesAndGloryClient.java`: `StoryCamp` and its `UseEntityCallback`;
  - `Phase4Checks.java`: suite `campstory`.

**Interfaces:**
- Consumes:
  - Task 13: `StoryLines.reaction`/`wounded`/`shrine`/`recruit`/`pell`/`choice`, `StoryBook.company`/`currentPage`, the `speakers` (`station`, `joins`, `namedAt`), `graveRoll` and `objects` rows.
  - Task 16: `Story.play`, `ScenePlayer` (substitutions, labels, voice), `SceneScreen`, the `card` op's `chits`, `story:choice:warm|plain`.
  - Task 24: `CampaignClient.story()`. Task 25: `StoryState.last`, `wounded`, `resting`; `Company.field`.
  - Phase 3: `CampWorld.station(BlockPos)`, `slot(Spec.CampPiecesRow,int)`, `yaw(int)`, `inCamp`, `Spot`; the `campPieces` rows whose station is `recruit:<house>` (the graveyard's is `recruit:graveborn`); the stations `contracts` (the Archivist) and `chapter` (the Founders' Stone).
  - Phase 2: `roll:<house>`, `buy:<troop>`, `Campaign.rollRng`.
  - Phase 2 Task 5: `ArmyMobs.portrait(String,World)` and `ArmyMobs.stageName(Spec.TroopsRow,int)` (the name `TroopModels.resolve` reads, so each companion wears its own model). Phase 2 Task 5b: `TroopAnimator.setScenePose(int,String)`.
- Produces (server):
  - `record Story.Line(String speaker,String text,String voice,String choice)`:
    - `Line.of(Spec.StoryLinesRow)` (`choice` is the trigger of a `choice_<n>` row, else null);
    - `Line.of(Spec.GraveRollRow)` (speaker `wren`);
    - a speaker `item:<id>` draws the item card.
  - `Story.line(ServerPlayerEntity p,List<Story.Line> lines,Runnable follow)`: an unsaved scene of one card a line, with Task 16's substitutions and voice, labelled by `StoryNames.label(speaker,state,S)`.
    - A choice line draws its row's `warm` and `plain` as chits and waits for `story:choice:<pick>`; the pick stores `StoryState.choices` and plays the `choice_<n>:<pick>` row next.
    - Each row counts as heard when its card opens (`woundedHeard`, `shrineHeard` with `shrineDue` less one, `pellSaid`, `recruitHeard`, `rollHeard`). The end saves once.
    - Skip ends a line scene, and a skipped choice stays open.
  - `StoryNames.label(String speaker,StoryState s,Spec.SpeakersRow[] S)`: `name` once the `namedAt` scene is read, else `label`.
  - Two marks on a scene's first reading:
    - every Archivist topic whose cards the scene holds in full joins `pellSaid` (`c1_archive` and `c1_rule` hold topic 2);
    - the book's following `auto` row joins `pending` (`c1_crown`, after `c1_king_after`).
  - The `returnLobby` after a campaign battle's `finish` raises `shrineDue` by one and saves, when Wren is in `StoryBook.company`. It runs after the after-scene, so the return from `march-2` counts and a straight run banks five.
  - `Story.dueArrival(UUID)`: `finish` calls it for a campaign battle, and the harness for fixtures.
  - `story:camp` with an arrival due plays, in order:
    1. up to three `StoryLines.reaction` lines over `last`, with `present` = `Company.lastFielded(uuid)` and `joinedAt` the `battle` cell of each speaker's `joins` row;
    2. when `c1_crown` is pending: `c1_shrine_2` if unread, every due memory row in order, then `c1_crown`.

    Without an arrival due it does nothing.
  - `story:talk:<companion>` (ignored while a scene runs) plays the first of:
    - the next `wounded` or `resting` line, while wounded or resting;
    - Ashka's open choice;
    - Wren's next due memory row.

    With nothing to say it opens the companion's `speakers.station` page.
  - `atStation` additions. Each line scene ends on the station's page:
    - `contracts`: an OPEN unread card scene of the station (`c1_archive`) plays as Task 16's do. Otherwise the next Archivist topic (`StoryLines.pell` at `currentPage`) plays, its cards in one line scene.
    - `recruit:graveborn`: Wren's next due memory row.
    - `chapter`: the `stone` item card on every use, its three parts on their own lines.
  - `Story.recruited(ServerPlayerEntity p,String troop)`: after a successful Graveborn `roll` or `buy`, made while the player stands at `recruit:graveborn` with Wren in the company, it plays `StoryLines.recruit` over the recruit page.
  - `Company.lastFielded(UUID)->List<String>`: the speakers that `Company.field` last fielded for that player.
- Produces (client):
  - `StoryCamp`: while the player is in camp (`CampWorld.inCamp`) and `CampaignClient.story()` is known, it adds one client-only entity per companion of `StoryBook.company`:
    - each is `ArmyMobs.portrait(row.entity(),world)` for the speaker's troop row, named `ArmyMobs.stageName(row,1)`, with NoAI and no gravity;
    - each is added with `ClientWorld.addEntity` under ids from -100000 down, at `spot(speakers.station)`, facing `CampWorld.yaw(rotation)`;
    - they are rebuilt on each profile update and removed on leaving camp. The server world holds none of them, so no other player sees them.
  - `static CampWorld.Spot spot(String station)` (pure): the `campPieces` row with that station. The spot is 2.5 blocks in front of `slot(row,5)` along the row's facing, with the feet at the row's `y`+1.
  - **The stretcher.** A wounded or resting companion lies at its spot in `fallen` (`TroopAnimator.setScenePose`) on a stretcher.
    - The stretcher is a `white_wool` block drawn in `WorldRenderEvents.AFTER_ENTITIES` with `renderBlockAsEntity`.
    - It is scaled to 0.9 x 0.15 x 1.9, 0.25 above the ground, along the facing.
  - **The use.** `UseEntityCallback`: a use on one of these returns `ActionResult.CONSUME`, so no packet is sent, and sends `story:talk:<companion>`. `static String of(int entityId)` serves the harness.

- [ ] **Step 1: Compile both source sets.**
- [ ] **Step 2: Write suite `campstory`.** The harness sets `last`, `lastFielded` and the story state, then calls `dueArrival`.
  - `p4-camp-arrival`: `march-4` won with Dunmar and Wren fielded (Maud wounded), Wren fallen once, a rout and the bonus missed.
    - Closing the report plays `c1_w24`, `c1_d15` and `c1_d16` in that order, in one line scene: a fall, a rout and the bonus. Maud's line about Wren is skipped, since she was absent.
    - A second `story:camp` plays nothing.
    - With Maud, Dunmar and Wren fielded, Maud fallen, a rout and the bonus missed, it plays `c1_d14`, `c1_m54` and `c1_w26`, and never a fourth.
    - Screenshot `p4-camp-reaction`.
  - `p4-camp-losses`: `march-1` won with the bonus kept and 9 losses of 20 plays `c1_m41`, typed as `9 lost on the river road, to Order blades. Count them. Then count them again.`, then `c1_m42`.
  - `p4-camp-wounded`:
    - Maud wounded lies fallen on the stretcher at the Gilded tent. Three uses play `c1_m73`, `c1_m74` and `c1_m73`, each labelled `Maud`.
    - Wren resting plays `c1_w46`, then `c1_w47`.
    - Dunmar wounded before `c1_letter` is read is labelled `The gate-warden`.
    - Screenshot `p4-camp-stretcher`.
  - `p4-camp-shrine`: with `c1_shrine_2` read and `shrineDue` 2:
    - using Wren plays `c1_w48`;
    - using the shrine plays `c1_w49`, then opens the recruit page on the Graveborn tab;
    - a third use of Wren opens that page with no line.

    A return from `march-1` without Wren leaves `shrineDue` alone. The return from `march-2`'s first win, after `c1_b2_after`, raises it by one.
  - `p4-camp-crown`:
    - reading `c1_king_after` puts `c1_crown` in `pending`;
    - with rows 2 to 4 heard and `shrineDue` 2, the arrival plays the reaction lines, `c1_w51`, `c1_w52`, then `c1_crown`, whose skip is refused;
    - `pending` is empty after, and the book then opens without it.
  - `p4-camp-choice`: on page 7 with `c1_b5_after` read, talking to Ashka shows `c1_a06` with the chits `Nobody in my banner is the worst.` and `It was the quickest way through.`
    - Hotbar 1 plays `c1_a07` and stores `choice_1` as `warm`.
    - A skipped choice stays open and is asked at the next talk.
    - Screenshot `p4-camp-choice`.
  - `p4-camp-pell`: with `c1_archive`, `c1_letter` and `c1_rule` read on page 5:
    - five uses of the Archivist play topics 1, 3, 4, 5 and 6 in that order, each ending on the contracts page (topic 2 is held by both scenes);
    - a sixth use opens the page at once.

    With `c1_archive` unread, the first use plays it.
  - `p4-camp-stone`: using the Founders' Stone shows the item card with `The crown calls them.`, `The crown sends them home.` and `A living hand, one crown, one standard, on the field of kings.` on their own lines, within five lines. The Founders page follows. Screenshot `p4-camp-stone`, with the Stone's drawn letters behind the card.
  - `p4-camp-recruit`: with Wren in the company, at the shrine:
    - a `roll:graveborn` fixed by `Campaign.rollRng` to a revenant plays `c1_w57` over the recruit page, and the page is back after it;
    - the next revenant plays `c1_w58`, and after all four revenant rows a revenant plays `c1_w53`.

    A Graveborn roll from the War Table plays nothing, and so does one without Wren.
  - `p4-camp-company`: with all four in the company, each stands at `spot` on a solid block with two free blocks above. None exists in a battle, and the server world holds none of them. They go on leaving camp and return with the player. Screenshot `p4-camp-company`.
- [ ] **Step 3: Run suite `campstory`; expect FAIL** (`story:camp` unknown).
- [ ] **Step 4: Implement** `Line`, `line`, the label, the marks, `shrineDue`, the arrival, the talk, the station lines, `recruited`, `lastFielded` and `StoryCamp`.
- [ ] **Step 5: Run suites `campstory,companions,scenes,storybook` and the StoryCheck line.**
  - **Expect** every id, `pass p4-campstory-suite`, the other suites as before, and `StoryCheck: 12 checks passed`.
  - The tester grades `p4-camp-stretcher`, `p4-camp-choice` and `p4-camp-company` against the rubric. Below 9 is a fix round.
- [ ] **Step 6: Commit.**
```bash
git add src/main/java/gg/gravesandglory/{StoryCamp,Story,StoryNames,Company,SceneScreen,DialogCard,CampScreen,Campaign,GravesAndGloryClient}.java test-tools/java/gg/gravesandglory/Phase4Checks.java
git commit -m "Phase 4: the camp story: arrival lines, the stretcher, the shrine, the Archivist and the Stone"
```

---

### Task 27: Final regression, contact sheets and Kort's gate

**Executor:** Sonnet 5.5 max.

**Files:**
- Create: `docs/PHASE-4-PLAYTEST.md`
- Modify: `docs/PHASE-4-GATES.md` (sections "Final regression", "Screenshot gate" and "Kort's gate"), `docs/PROJECT-STATE.md`, `docs/PARKED.md` (append only)

**Interfaces:**
- Consumes: every suite, check id and gate above; Phase 1's, the UI plan's, Phase 2's and Phase 3's final regression lists, as their plans' last tasks name them.

- [ ] **Step 1: Pure checks.**
  - Preflight prints `Preflight clean`. The four standalone lines of the Test runs pass, with `StoryCheck: 12 checks passed`, `story-names: clean`, `story-voice: <v> voiced, <u> unrecorded, <s> silent, clean` and `story-copy: <n> lines match`.
  - `python -m unittest tools/test_mode_gate.py tools/test_write_voice_sounds.py -v` prints `OK`.
  - `python tools/build_ui_art.py --verify` prints `26 of 26 thumbnails`.
  - `python tools/economy_model.py` passes as in Task 12.
- [ ] **Step 2: Every Phase 4 suite,** each launch on a fresh root:
  - `modes,hud,world,siege,survival,banner,skirmish`;
  - `march,save,report,companions,campstory`;
  - `scenes,storybook` with `GRAVES_P4_SCENES` set to the twenty scene ids of `storyPages` (every row whose kind is not `battle`);
  - `story-capture` with `GRAVES_P4_LOOK=reimagined` (as Task 18 runs it) and `GRAVES_P4_CAPTURE` set to the fourteen book and stage scene ids.

  Expected: every `p4-*` id `pass`, the fifteen suite lines, the twenty `p4-scene-<id>`, and a `p4-set-<scene>-<n>-reimagined` frame for every panel and stage set.
- [ ] **Step 3: Earlier suites, each on a fresh root:**
  - Phase 3's `pipeline,studio,camp,houses,yard,landmarks,routes`, with `GRAVES_PHASE3_MAPS` unset;
  - Phase 2's final list;
  - Phase 1's `combat,morale,reinforce,bots,tutorial,report,ui`, and the UI plan's suites;
  - `-PcampaignChecks` with no variable, ending with `pass roadmap-suite`.

  Expected: every id as its plan last left it.
- [ ] **Step 4: Bot runs on the final build.** Task 9's three mode runs and Task 11's march run, as those tasks give them. Expected: three `pass` lines and six `pass` lines, appended to `PHASE-4-GATES.md` under "Final regression". A fail is tuned as those tasks say and the step rerun.
- [ ] **Step 5: Release build, not uploaded.**
  - Run `"$G" build --offline -x test`, then `python tools/package.py`. Its leak check passes: no `Phase4Checks`, `ModeMathCheck`, `MarchMathCheck`, `StoryCheck` or `t_voice` in the jar. The zip stays in `dist/`.
  - `J=$(ls build/libs/graves-and-glory-*.jar | grep -v -- -sources); unzip -l "$J" | grep -c "data/gravesandglory/scenes/c1/.*\.scene"` prints `20`.
- [ ] **Step 6: Contact sheets** from the Step 2 screenshots, with Phase 3's `sheet` function (frames scaled to 480x270): `modes` 4x4 from `p4-hud-*`, `p4-siege-*`, `p4-surv-*`, `p4-banner-*` and `p4-skirmish-*`; `scenes` 5x4 from `p4-scene-*`; `sets` 5x4 from the `p4-set-*-reimagined` frames; `book` 4x3 from `p4-book-*` and `p4-card-*`; `camp` 4x3 from `p4-comp-*` and `p4-camp-*`. Expected five sheets.
- [ ] **Step 7: Screenshot gate.** A Sonnet 5.5 max tester reviews the five sheets and every grade in `PHASE-4-GATES.md`.
  - Every graded image stands at 9 or better, and each sheet gets one sentence on what it shows.
  - An image under 9 goes back to its task as a fix round before Step 8.
- [ ] **Step 8: Write `docs/PHASE-4-PLAYTEST.md`** (one page).
  - **Setup:** this build, a fresh profile, scenes on, voice silent.
  - **The session:** Kort plays chapter 1 start to finish in one sitting, about 90 to 120 minutes (draft value): the book from page 1, all six battles, the camp between them and `c1_crown`.
  - **Yes or no boxes:** each mode played once (Survival at the shrine, Siege at the gate, Banner at the river); a companion fell and was wounded; Wren's memory rows played; the Pact card granted the Legendary.
  - **The questions, which are the pass mark:**
    - what Maud, Wren, Dunmar and Ashka each wanted, and who the Hollow King was (story 11: a part he cannot answer is rewritten before the chapter ships);
    - whether he wants chapter 2 (Phase 4 spec 13).
  - **Appendix:** the five sheets and every gate line.
- [ ] **Step 9: Append to `docs/PARKED.md`:**
  - "The camp guide blurb's placement (the copy's 40 words in `chapters.blurb`; neither spec places it) (Phase 4, for Kort)";
  - "Chapter 1 voice recording (story 7.9 and 12.4; who voices whom is Kort's call)";
  - "A `swing` scene pose (story 7.3 lists it; Phase 2 Task 5b has none)";
  - "A turning mill wheel for `c1_b4_after` (no command or piece moves it)";
  - "Scene fog under shader packs (the override may not show)".
- [ ] **Step 10: Update `docs/PROJECT-STATE.md`:** Phase 4 built and waiting on Kort's gate, with the test roots of Steps 2 and 3.
- [ ] **Step 11: Commit.**
```bash
git add docs/PHASE-4-PLAYTEST.md docs/PHASE-4-GATES.md docs/PROJECT-STATE.md docs/PARKED.md
git commit -m "Phase 4: regression, contact sheets and Kort's gate"
```
  There is no merge, push or upload, and `mod_version` stays the controller's call. Hand the branch and `docs/PHASE-4-PLAYTEST.md` to the orchestrator.

---

## Self-review

**1. Spec coverage.**
- **The Phase 4 spec:**
  - section 4, the mode frame: Task 2, on Task 1's pure rules;
  - section 5, Siege: Tasks 4 and 5. Section 6, Survival: Task 6. Section 7, Banner: Task 7;
  - section 8, feed lines and the briefing card: Tasks 3 and 12. Section 9, Skirmish: Task 8;
  - section 10, the six battles: Tasks 10 to 12. The march page becomes the book (Task 24), as the story spec rules;
  - section 11, data: each sheet lands with its first rows, in Tasks 2, 4 to 7, 10, 13 and 17;
  - section 12, save state: Tasks 10, 11, 25 and 26;
  - section 13, harness and gates: Tasks 2, 9, 11 and 27.
- **The story spec:**
  - 7.1, the storybook: Tasks 15, 18 and 24;
  - 7.2, the three kinds of scene: Tasks 16 (card), 17 (stage) and 18 (book);
  - 7.3, scene files: Task 14, with six commands added for the footage ruling (reported);
  - 7.4, dialog cards: Task 16, with mockup ruling 3's 60 px live portrait in place of 32 px pixel art (reported, since 7.4 is not yet rewritten);
  - 7.5, companions: Task 25, and their reaction lines Task 26;
  - 7.6, the shrine: Tasks 13 and 26. 7.7, the chits: Tasks 16 and 26. 7.8, protection: Task 25. 7.9, voice: Tasks 14 and 16;
  - 7.10, where the lore lives: Task 4 (the Stone's letters), Task 16 (the item cards) and Task 26 (the Stone's reading, the Archivist, the shrine, the tents);
  - section 11, the suites and checks: Tasks 13, 14, 16, 19 and 24 to 26;
  - 12.3, the sets built in the world and graded from their frames: Tasks 18 to 23, on Phase 2's own troop models.
- **Kort's rulings:**
  - the footage ruling: Tasks 17 to 23 and the Art gate;
  - the Pact's naming: the Global Constraints;
  - Tomas named on page 8: Task 13's `StoryNames`;
  - the Phase 2 interface change: the merged-code constraints and every Interfaces block from Task 14 on.
- **Parked by Task 27, not built here:** the blurb's placement, voice recording, a `swing` pose, a turning mill wheel, and fog under shader packs.

**2. Step scan.** Every test step names its ids with the copy's voice ids and the sheets' values. Every code step names its file, its signature and the values the specs pin. Every run step names its command or suite list and the output that passes. The only code bodies are the two pure checks' assertion lists (Tasks 1 and 10). A string that does not fit goes back to the copy; no step writes copy.

**3. Type consistency.** These are the names used across tasks, checked against their defining task:
- the modes: `ModeMath`, `BattleMode`, `Modes.registered`, `Battle.mode`/`fieldBase`/`fieldBarracks`, `Campaign.battleMode`, `CampaignClient.battleMode`/`howTo`/`lastChosen`/`story()`;
- the march: `FeedLines.bannerLying`/`bonus`/`fallen`, `Structures.placeSaving`/`restore`, `Walkability.reach`, `MarchMath.cleared`/`lockedBy`/`nextUnwon`/`ride`/`teamSizes`/`settle`, `Squad.fixedBanner`, `Bonus`;
- the story state: `StoryState` and `Facts`; `StoryBook.State`/`next`/`state`/`currentPage`/`company`; `StoryLines.reaction`/`wounded`/`shrine`/`recruit`/`pell`/`choice`; `StoryNames.label` (both forms);
- the scenes: `SceneScript.Op`/`Scene`/`parse`; `Story.command`/`atStation`/`play`/`running`/`cond`/`abort`/`recover`/`line`/`Line`/`dueArrival`/`recruited`; `SceneScreen.active`/`hideChrome`; `DialogCard.CPT`/`wrap`; `StoryVoice.play`; `OggLength.seconds`; `SceneMath`; `StageArea`; `MarchCard`;
- the book and the company: `StorybookScreen`; `Company.field`/`lastFielded`; `CompanyNames.of`; `StoryCamp.spot`/`of`;
- the sheets `modes`, `modeSites`, `modeRules`, `waves`, `waveGroups`, `campaignBattles`, `bonusObjectives`, `chapters`, `storyPages`, `speakers`, `storyLines`, `graveRoll`, `objects` and `storySets`, and the channel `scene_v1`;
- Phase 2: `ArmyMobs.create`/`portrait`/`stageName`, `ArmyCards.portrait`, `TroopModels.resolve`/`portraitTall`, `TroopAnimator.setScenePose(int,String)` with its eight poses, `Battle.joinSide`, `Battle.shield`.

**4. Review Focus.** Each of the five lines has its test in the task that owns the code:
- 1: `p4-mode-teardown-mid` and `p4-siege-crash` (Task 5), and `p4-banner-teardown-head` (Task 7);
- 2: `p4-scene-interrupt` (Task 17);
- 3: `p4-feed-fit` (Task 3) and `p4-scenes-fit` (Task 16);
- 4: `p4-migrate-v2` (Task 10), `p4-book-catchup` (Task 13) and `p4-book-migrated` (Task 24);
- 5: `p4-scene-keys` (Task 16).

**5. Proportion.** The plan runs to about 1.8 times the two specs together, for 27 tasks. It does not restate the copy: it names voice ids and quotes a line only where an assertion needs the exact text. Its code blocks are run commands, commits and the two assertion lists.
