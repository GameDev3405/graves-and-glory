# Phase 2: The Five Houses Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Dawn and Dusk with five Houses of six troops each (30 troops, four rarities). Add House traits, a data-driven counter table, six commander classes, and the long chase: House rolls with pity, stars, evolutions, shards, renown, mastery and the re-authored Founders chapter. Every v1 profile migrates without loss.

**Architecture:** All new numbers live in new `sheets/design.json` sheets, read through the generated `Spec`. Every arithmetic rule sits in a pure class with no Minecraft imports (`RosterMath`, `CounterMath`, `Profile`, `ProfileMigration`, `Progression`), proved by javac-only standalone checks. Server code gets one entity factory (`ArmyMobs`), one damage path (`Combat.resolve`), one ability dispatcher (`Abilities`), one trait engine (`Houses`) and one commander engine (`Commander`). Each House lands as its own batch with its art, its mechanics, an in-game `Phase2Checks` suite with screenshots, and a bot-vs-bot balance gate.

**Tech Stack:** Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Yarn 1.20.1+build.10, Loom 1.7.4, Java 17, Gradle 8.8 (offline), Python 3. Entity Texture Features 7.2.6 and Entity Model Features 3.3.12 are already pinned in `dependencies/manifest.json`, as is Fresh Animations 1.10.4.

**Spec:** `docs/superpowers/specs/2026-10-17-houses-design.md` (binding, all sections). Context: `docs/VISION-AND-ROADMAP.md` sections 4, 5 and 8; `HOW-TO-EDIT.md`; the Phase 1 plan `docs/superpowers/plans/2026-10-17-phase-1-solo-core.md`. Phase 1 is assumed merged, and this plan uses its names exactly.

## Global Constraints

- Minecraft 1.20.1 and Fabric only; no new dependencies. Skins and models use the pinned ETF 7.2.6, EMF 3.3.12 and Fresh Animations 1.10.4.
- Never hand-edit `Spec.java`. Change `sheets/design.json`, then run `python tools/preflight.py`. Every row of a sheet has the same columns and every cell is filled. A column is `double` only if the first row's value has a decimal point.
- Game numbers live in sheets (spec 6.1). No class, trait, counter, rarity or reward number is hard-coded in Java. The only code constants are the spec's fixed effect kinds (Resistance I, Speed II) and presentation timings.
- The harness lives only in `test-tools/`. Add `Phase2Checks`, `HousesCheck` and `ProfileMigrationCheck` to the `forbidden` tuple in `tools/release_config.py`.
- The working copy uses CRLF line endings. Edit dense one-line Java files in place; never re-save a whole file with different line endings or reformat it.
- The ten surviving v1 troops keep their legacy ids. New troops use House prefixes (`gl_`, `gb_`, `wm_`, `ic_`, `tc_`). The `oracle` and `wraith` rows are removed.
- Mob rule (spec section 0): no brain-AI mobs (piglin brute, hoglin, zoglin, warden, camel, sniffer, goat), no flying mobs, no guardians. Every `Army*` subclass keeps only `SwimGoal` and returns `false` from `shouldSave`. Every undead one also returns `false` from `isAffectedByDaylight()` and `canConvertInWater()`.
- Hard progression cap: star bonus plus evolution bonus is at most +12% per stat over the sheet base, in every mode. Class effects and House traits are battle effects and sit outside the cap.
- The counter product is clamped to [0.25, 2.5] (`combatDiscipline.multiplierMin/Max`). Troop damage uses the damage type `gravesandglory:troop`, tagged `minecraft:bypasses_shield`; armour still applies.
- Skins: 64x64 vanilla layout, flat-shaded, 3 to 4 tones per material. The team colour is the loudest read (tabard plus back pennant); Houses read by silhouette and material. Every unit's look (purpose, silhouette, palette, armour and weapon, mark, tell, build basis, portrait crop) is fixed by `docs/superpowers/specs/2026-10-17-unit-identity.md` (approved by Kort 2026-10-18); Tasks 5 to 10 build to that spec, and where it differs from Houses spec 2.7 the unit identity spec wins. Its five approved rules: no gold or diamond on troops; painted shields and CustomModelData weapons via one sheet column; Tusk Spear braces on Charge; one team-colour cloth per Legendary; fix the husk and boar-kin arm pose in Task 5 before any Gilded art.
- Balance gate per House batch: the House's `<house>_normal` bot banner against the current best pure normal banner. 20 matches (10 per side), Normal bots, maps 0 to 4. The combined win rate must be between 40% and 60%.
- Launch rules are unchanged from Phase 1: no launch while Kort is playing (run the process check first); use an isolated, disposable `GRAVES_ROADMAP_TEST_ROOT` or `GRAVES_ROADMAP_SERVER_ROOT` under the scratchpad; always pass `--offline`; `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"`.
- Never publish, upload or push. Commit locally per task on branch `phase-2-houses`, cut from `main` after Phase 1 is merged.
- Packet layout changes bump the channel id. Phase 2 moves `state_v5` to `state_v6` once (Task 3) with every Phase 2 field reserved, so later tasks do not change the layout.
- UI uses the `DarkUi` tokens (charcoal, ivory, brass) and Marcellus headings. Team colour is drawn only through `DarkUi.teamMark/teamText/teamColor`. Key names come only through `HintText.expand` or `GravesAndGloryClient.hint`. Copy tone: grim but heroic, short, plain.
- Deferred ideas go to `docs/PARKED.md` (append only).

## Review Focus

1. **Side-team cleanup.** A troop dies, rises (Rise Again), is raised by Grave Bloom, or the round ends or the map switches. Its scoreboard side team (`gg_<side>`) membership is removed with the entity, so ETF `teams=` never leaks and the scoreboard does not grow across rounds. (Test: `p2-teams-clean`, Task 5.)
2. **Awkward v1 profiles.** Cases: removed ids (`oracle`, `wraith`); a deployment that is too short or names locked troops; a v1 client cache imported on a first LAN visit. Each is migrated exactly once, the server copy wins, and `<uuid>.v1.json` is kept and never deleted. (Tests: `mig-*` in `ProfileMigrationCheck`, Task 1; `p2-migrate-import`, Task 13.)
3. **Temporary troops at round end.** Temporary Grave Bloom Risen and risen Graveborn never count toward the 20-slot reinforce check and never earn mastery "survived". They vanish at `startRound`, `returnLobby` and `switchMap`, and the death log is cleared. (Test: `p2-bloom-cleanup`, Task 7.)
4. **Supply banner lost before deploy.** A Forward Supply banner is destroyed, or an enemy arrives within 12, between purchase and deploy. The troop deploys at the next valid anchor or at the barracks, decided at spawn time. (Test: `p2-supply-lost`, Task 8.)
5. **Large and four-legged mobs.** Large hitboxes (golem 1.4, ravager 1.95, bear 1.4) and quadrupeds on bridges and in formations cross the river by bridge (or ford, at Tidecallers tier II) within the route time budget. They do not sit, rear or roar outside their abilities. (Tests: `p2-ironclad-bridge`, `p2-tidecallers-bridge`, `p2-wolf-stands`, `p2-bear-warning`.)

## Test runs used by every task

**Standalone pure checks (seconds, no game):**
```bash
cd /c/Users/kortt/Melty/bannerbound && python tools/preflight.py
J="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot/bin"; S=src/main/java/gg/gravesandglory
"$J/javac" --release 17 -d build/phase2-checks $S/{Spec,BattleMath,MatchStats,HintText,Progression,Profile,ProfileMigration,RosterMath,CounterMath}.java \
  test-tools/standalone/{BattleMathCheck,HousesCheck,ProfileMigrationCheck}.java \
  && for c in BattleMathCheck HousesCheck ProfileMigrationCheck; do "$J/java" -cp build/phase2-checks gg.gravesandglory.$c || exit 1; done
```
Pass output: `BattleMathCheck: <n> checks passed`, `HousesCheck: <n> checks passed`, `ProfileMigrationCheck: <n> checks passed`. The style follows `BattleMathCheck`: `check(boolean, String)`, a counter, a summary line, and an `AssertionError` naming the failed check.

**Compile both source sets:** `export JAVA_HOME=...; G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle); "$G" compileJava compileLocalTestTools --offline`

**In-game suite `<suite>`:** run the Phase 1 command with `GRAVES_PHASE2_CHECKS=<suite>` in place of `GRAVES_PHASE1_CHECKS` and root `<scratchpad>/p2-<suite>-$(date +%H%M%S)`, then:
```bash
grep -E "^(pass|fail) p2-" "$GRAVES_ROADMAP_TEST_ROOT/evidence/local-checks.txt"; grep "\[p2\]" "$GRAVES_ROADMAP_TEST_ROOT/run/logs/latest.log"; ls "$GRAVES_ROADMAP_TEST_ROOT/run/screenshots"
```
A suite passes when:
- every `p2-*` id of the suite is `pass`;
- `pass p2-<suite>-suite` is present;
- the named screenshots exist;
- the tester opens each screenshot and states in one sentence what it shows. A screenshot that does not show the claimed thing is a fail.

**Bot balance run (`<a>` vs `<b>`):** two background processes of 10 matches each, 300 s rounds.
```bash
# process 1, port 25601: <a> on team 0      # process 2, port 25602: <a> on team 1
"$G" runServer --offline -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300 \
  -PsimBanner0=<a> -PsimBanner1=<b> --init-script test-tools/isolated-server.gradle
python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv <a>
```
- Pass: `gate <a> vs <b> win=<P>% pass` with 40 <= P <= 60.
- Append the two `[botsim]` lines and the gate line to `docs/PHASE-2-BALANCE.md`.
- On a fail, tune only that House's rows, keeping each number within 15% of the spec value. Rerun, and log every change.
- The "current best pure banner" is the pure normal banner with the highest gate win rate logged so far.

---

### Task 1: House data sheets and pure rules

This task is additive only: new sheets and pure classes. No game code reads them yet.

**Files:**
- Create: `src/main/java/gg/gravesandglory/{RosterMath,CounterMath,Profile,ProfileMigration}.java` (pure, no Minecraft imports)
- Create: `test-tools/standalone/HousesCheck.java`, `test-tools/standalone/ProfileMigrationCheck.java`
- Modify: `sheets/design.json` (the new sheets below; `combatDiscipline` gains `multiplierMin: 0.25` and `multiplierMax: 2.5`)
- Modify: `tools/preflight.py` (shape and cap rules), `tools/release_config.py` (forbidden names)

**Sheets (exact rows):**

`houses` has columns `id, name, accent, glyph, troops, title, fieldable`. The glyph is `gravesandglory:textures/gui/house/<id>.png`. The `troops` order is Common infantry, Common ranged, Rare, Rare, Epic, Legendary.

| id | name | accent | troops | title | fieldable |
|---|---|---|---|---|---|
| gilded | Gilded Order | 14667368 | knight,archer,sentinel,longbow,mage,gl_marshal | Oathsworn | true |
| graveborn | Graveborn | 10338206 | revenant,bonebow,reaper,stalker,hexer,gb_hollowking | Gravebound | true |
| wildmoot | Wildmoot | 13206842 | wm_hound,wm_hunter,wm_stalker,wm_tusk,wm_seer,wm_oldhorn | Moot-Blooded | false |
| ironclad | Ironclad | 14708794 | ic_hearthguard,ic_arbalist,ic_breaker,ic_ballista,ic_mortar,ic_colossus | Hold-Kin | false |
| tidecallers | Tidecallers | 5220506 | tc_raider,tc_harpooner,tc_tideguard,tc_mist,tc_brute,tc_admiral | Tidesworn | false |

`houseTraits` has columns `id, house, tier, slots, mechanic, value, value2, radius, durationTicks, text`: 15 rows with ids `<house>_<tier>` and slots 6/10/14. The `text` is the spec 1.3 sentence for that tier.

| House | mechanic | value (I/II/III) | value2 | radius | durationTicks |
|---|---|---|---|---|---|
| graveborn | `rise_again` | 0.10 / 0.25 / 0.40 (chance) | 0.30 (HP share) | 0.0 | 20 (stun) |
| gilded | `bulwark` | 0.15 / 0.30 / 0.45 (ranged reduction) | 0.0 | 5.0 / 8.0 / 12.0 | 0 |
| wildmoot | `pack_hunters` | 0.10 / 0.20 / 0.30 (bonus) | 3.0 (attackers on one target) | 0.0 | 0 |
| ironclad | `entrench` | 0.10 / 0.15 / 0.20 (taken) | 0.10 / 0.20 / 0.30 (siege range) | 0.5 (stillness blocks) | 60 |
| tidecallers | `riptide` | 0.15 / 0.25 / 0.35 (speed) | 0.0 / 1.0 / 1.0 (fording) | 0.0 | 20 / 30 / 40 (Slowness I ticks) |

`abilities` has columns `id, mechanic, trigger, cooldownTicks, radius, windupTicks, amount, amount2, statusTicks, statusLevel, knockback, durationTicks, minRange, target, text`.
- `trigger` is one of `passive, onhit, cast, hold, timer, charge_contact`.
- `statusLevel` is the amplifier (0 = I).
- A negative `knockback` is a pull in blocks; a positive one is the vanilla `takeKnockback` strength.
- Stance multipliers live only in `counters`, so stance rows have `amount 0.0`.
- Unlisted cells are 0 or 0.0, and `target` is `enemies` unless noted.
- `text` is the section 2 ability sentence, at most 90 characters.

| id | mechanic | trigger | values |
|---|---|---|---|
| none | none | passive | - |
| shield | shield | passive | - |
| steady, ambush, brace, armour_bolt, sunder | stance | passive | - |
| pierce_longbow | pierce | onhit | radius 2.0, amount 0.5 |
| mend_sunweaver | mend | cast | cd 80, radius 10.0, amount 5.0, target allies |
| oath_banner | aura | hold | cd 600, radius 12.0, amount 0.10, duration 300, target allies |
| fearless | fearless | passive | amount 0.12 (rout share), amount2 0.5 (living share) |
| cleave_reaper | cleave | onhit | radius 2.0, amount 0.5 |
| frostshot | onhit_slow | onhit | status 40, level 0 |
| blight_gravecaller | blight | cast | cd 100, radius 2.5, windup 20, status 40, level 0 |
| dread | aura | passive | radius 10.0, amount 0.15, amount2 0.40 |
| pounce | pounce | onhit | cd 100, radius 6.0 (closing distance), amount 1.8 |
| ensnare | onhit_slow | onhit | cd 80, status 30, level 1 |
| howl | aura | timer | cd 300, radius 10.0, amount 0.15, amount2 0.15, duration 120, target allies |
| trample | slam | charge_contact | cd 200, radius 3.0, amount 6.0 (flat), knockback 1.2 |
| pierce_ballista | pierce | onhit | radius 4.0, amount 0.6 |
| shell | blight | cast | cd 100, radius 3.5, windup 30, minRange 10.0 |
| forge_slam | slam | onhit | radius 2.5, amount 0.6 (share), knockback 0.8 |
| brine | onhit_slow | onhit | status 20, level 0 |
| harpoon | onhit_slow | onhit | cd 60, status 30, level 0, knockback -1.0 |
| mist | mist | cast | cd 160, radius 4.0, windup 20, amount 0.5, status 20, level 0, duration 100 |
| tentacle_sweep | slam | onhit | cd 60, radius 3.0, amount 0.7, knockback 1.0 |
| undertow | slam | timer | cd 200, radius 7.0, amount 4.0 (flat), status 60, level 1, knockback -3.0 |

`counters` has columns `id, attackerTag, targetTag, condition, multiplier, replaces, text`.
- A tag is a troop tag, an ability id, or one of the dynamic tags `melee, arrow, shot (arrow or bolt), magic (blight, bolt, mist), nonsiege, ranged (the target troop has ranged=true), charging, holding, structure, any`. `a+b` means both.
- `replaces` is `none` or a `|` list. A matched row and the matched rows it lists form one group that contributes only its largest multiplier.

| id | attacker | target | condition | multiplier | replaces |
|---|---|---|---|---|---|
| C1 | arrow | shield | raised_front | 0.35 | none |
| C2 | arrow | shield | lowered_front | 0.75 | none |
| C3 | melee | shield | raised_front | 0.6 | none |
| C4 | magic | shield | always | 1.0 | none |
| C5 | melee | ranged | charge_impact | 1.5 | C6 |
| C6 | melee | ranged | charge | 1.2 | none |
| C7 | cavalry | ranged | always | 1.3 | C5\|C6 |
| C8 | spear | beast | always | 1.6 | none |
| C9 | beast | spear+holding | front | 0.7 | none |
| C10 | siege | structure | always | 4.0 | none |
| C11 | nonsiege | structure | always | 0.25 | none |
| C12 | siege | large | always | 1.3 | none |
| C13 | siege | small | always | 0.6 | none |
| C14 | sunder | shield | always | 1.4 | C1\|C2\|C3 |
| C15 | armour_bolt | shield | raised_front | 0.7 | C1 |
| C16 | melee | any | flank | 1.15 | none |
| C17 | shot | any | in_mist | 0.5 | none |
| C18a | steady | any | hold | 1.2 | none |
| C18b | ambush | any | no_sky | 1.25 | none |
| C18c | brace | charging | hold | 1.3 | none |

`rarities` uses the spec 6.1 columns:

| id | rollWeight | chosenPrice | slotWeight | starPct2 | starPct3 | dupsStar2 | dupsStar3 | shardStar2 | shardStar3 | dismantleShards | bannerMax |
|---|---|---|---|---|---|---|---|---|---|---|---|
| common | 43 | 300 | 1 | 0.03 | 0.06 | 1 | 2 | 0 | 0 | 2 | 20 |
| rare | 6 | 300 | 1 | 0.03 | 0.06 | 1 | 2 | 0 | 0 | 4 | 20 |
| epic | 2 | 0 | 2 | 0.06 | 0.12 | 1 | 2 | 30 | 60 | 8 | 2 |
| legendary | 0 | 0 | 4 | 0.06 | 0.12 | 0 | 0 | 40 | 80 | 0 | 1 |

`evolutions` has columns `id, troop, stage, name, healthPct, damagePct, shards, gold, requiresMastery`: 40 rows with ids `<troop>_2` and `<troop>_3`.
- Patterns (cumulative HP/damage): balanced 0.03/0.03 then 0.06/0.06; tank 0.04/0.02 then 0.06/0.03; striker 0.02/0.04 then 0.03/0.06.
- Costs: Common 10 shards and 100 gold, then 25 and 250; Rare 15 and 150, then 35 and 350. `requiresMastery` is 0 for stage 2 and 1 for stage 3.

| troop | rarity | pattern | stage 2 name | stage 3 name |
|---|---|---|---|---|
| knight | Common | balanced | Knight | Knight-Captain |
| archer | Common | striker | Order Bowman | Sergeant-Archer |
| revenant | Common | balanced | Revenant | Grave Knight |
| bonebow | Common | striker | Barrow Archer | Deadeye |
| wm_hound | Common | balanced | Dire Hound | Packleader |
| wm_hunter | Common | striker | Thornstalker | Elder Hunter |
| ic_hearthguard | Common | tank | Ironwall | Hearth-Thane |
| ic_arbalist | Common | striker | Steel Arbalist | Bolt-Master |
| tc_raider | Common | balanced | Reef Raider | Tide Reaver |
| tc_harpooner | Common | striker | Whaler | Deep Harpooner |
| sentinel | Rare | tank | Templar Knight | Templar Commander |
| longbow | Rare | striker | Yeoman Longbow | Master Longbow |
| reaper | Rare | balanced | Harvester | Lord of Scythes |
| stalker | Rare | striker | Frost Stalker | Wight Stalker |
| wm_stalker | Rare | balanced | Broodstalker | Widowmaker |
| wm_tusk | Rare | tank | Tuskguard | Boar-Lord |
| ic_breaker | Rare | balanced | Gatebreaker | Anvilfist |
| ic_ballista | Rare | striker (see Spec gaps) | Siege Crew | Master Engineer |
| tc_tideguard | Rare | tank | Reef Warden | Tide-Sworn |
| tc_mist | Rare | balanced | Fogweaver | Stormcaller |

`recruitment` has one row: `{"id":"houses","rollCost":100,"chosenCost":300,"epicPity":40,"legendaryShards":50,"renownForLegendary":30,"capPct":0.12,"maxStars":3}`.

`mastery` has columns `id, level, kills, matches, reward`:

| id | level | kills | matches | reward |
|---|---|---|---|---|
| mastery_1 | 1 | 40 | 8 | `title:Veteran;evolve:3` |
| mastery_2 | 2 | 150 | 25 | `skin:veteran` |
| mastery_3 | 3 | 400 | 60 | `frame:brass;title:Master` |

`renown` has columns `id, level, points, reward`: rows `renown_1` to `renown_5` at 3/8/15/22/30 points, with rewards `cosmetic:pennant`, `cosmetic:trim`, `cosmetic:frame`, `cosmetic:standard` and `legendary`.

`botBanners` has columns `id, house, difficulty, slots`: 24 rows. In the patterns, C1, C2, R1, R2, E and L are taken in order from `houses.troops`.
- Pure rows `<house>_<difficulty>`:
  - easy: `C1x8, C2x6, R1x3, R2x3`
  - normal: `E,^, C1x6, C2x4, R1x4, R2x4`
  - hard: `L,^,^,^, E,^, C1x5, C2x3, R1x3, R2x3`
- Mixed rows `mix_gilded_graveborn_*`, `mix_ironclad_tidecallers_*` and `mix_wildmoot_gilded_*` (the `house` column holds the first House) have two 10-slot halves:
  - easy: `C1x5, C2x3, R1, R2` in each half
  - normal: `E,^, C1x4, C2x2, R1, R2` in each half
  - hard: first half `L,^,^,^, C1x2, C2x2, R1, R2`; second half as normal

**Interfaces:**
- Produces (pure): `RosterMath.Catalog(Map<String,String> house, Map<String,String> rarity, Map<String,Integer> slotWeight)` with `rarityOf(id)`, `houseOf(id)` and `weight(id)` (`^` and `empty` weigh 0). Task 2 adds `Catalog.fromSpec()`.
- Produces `RosterMath` (banners and traits):
  - `houseSlots(List<String> banner, Catalog c) -> Map<String,Integer>`: a `^` counts for the unit before it.
  - `traitTier(int slots, int[] thresholds) -> int`: 0 to 3.
  - `validBanner(List<String> banner, Catalog c, Set<String> owned) -> String`: null when valid, else one of `size, unknown, not_owned, orphan_marker, missing_marker, legendary_limit, epic_limit`.
  - `repack(List<String> entries, Catalog c, Set<String> owned) -> List<String>`: walks entries in order. It skips `empty`, markers, unknown ids and unowned ids, plus a Legendary after the first and an Epic after the second. It appends each remaining id with `weight-1` markers if it fits, then pads with `empty` to 20.
  - `starterBanner() -> List<String>`: knight x12, archer x8.
- Produces `RosterMath` (rolls, stars and the cap):
  - `record Roll(String troop, int pityAfter)`.
  - `roll(List<String> commons, List<String> rares, String epic, int wCommon, int wRare, int wEpic, int pity, int epicPity, java.util.random.RandomGenerator rng) -> Roll`: the Epic is forced when `pity+1 >= epicPity`; `pityAfter` is 0 on an Epic, else `pity+1`.
  - `starFromCopies(int copies, Spec.RaritiesRow r) -> int`.
  - `addCopy(Profile.Owned o, Spec.RaritiesRow r) -> int`: adds a copy and returns the dismantled shards (`dismantleShards`) once stars are already at `maxStars`.
  - `starPct(int stars, Spec.RaritiesRow r) -> double`.
  - `progressionMultiplier(double starPct, double stagePct, double capPct) -> double` = `min(1+capPct, 1+starPct+stagePct)`.
- Produces `RosterMath` (rewards):
  - `renownLevel(int points, int[] thresholds) -> int`.
  - `record Rewards(Map<String,Integer> shards, Map<String,Integer> renown)`.
  - `matchRewards(Map<String,Integer> bannerSlots, int[] thresholds, List<String> houseOrder, boolean won) -> Rewards`.
  - `masteryLevel(int kills, int matches, Spec.MasteryRow[] rows) -> int`: both thresholds are required.
- Produces `CounterMath`:
  - `record Hit(Set<String> attacker, Set<String> target, double bearing, boolean raised, boolean chargeImpact, boolean charging, boolean holding, boolean noSky, boolean inMist)`. `bearing` is Phase 1's `BattleMath.relativeBearing(targetYaw, attacker-target)`.
  - `matches(Spec.CountersRow r, Hit h) -> boolean`: front means bearing <= 90; flank means bearing > 110.
  - `counters(Hit h, Spec.CountersRow[] rows) -> double` and `applied(Hit h, Spec.CountersRow[] rows) -> List<String>`.
  - `total(double counters, double traits, double auras, double commander, double min, double max) -> double`.
  - `trait(Spec.HouseTraitsRow[] rows, String house, int tier) -> Spec.HouseTraitsRow`: null for tier 0.
  - `bulwark(Spec.HouseTraitsRow t, boolean shieldTag, String kind) -> double`, where the reduced kinds are `arrow, bolt, blight`.
  - `packHunters(Spec.HouseTraitsRow t, int sameTarget, double bearing) -> double`, `entrench(Spec.HouseTraitsRow t, boolean still, boolean onOwnedPoint) -> double` and `riptideSpeed(Spec.HouseTraitsRow t, boolean wetOrRain) -> double`.
- Produces `Profile` (top-level, pure; it replaces `Campaign.Profile` in Task 2):
  - Fields: those of spec 6.2, plus `int campaignCleared` (Phase 1), `Set<Integer> writChapters` and `Set<String> cosmetics`.
  - `static final class Owned { int copies=1, stars=1, stage=1, kills, survived, mastery; String variant="base"; }`.
  - `sanitize(RosterMath.Catalog c, Set<String> classIds, Set<String> sideIds, List<String> houses)`: drops unknown ids; clamps stars and stages to 1..3 (stage 1 for Epic and Legendary); repacks a banner that fails `validBanner`; maps an unknown class to `champion` and an unknown colour to `gold`; keeps all five House keys at 0 or more in shards, renown and pity.
- Produces `ProfileMigration`:
  - `V1` with fields `schema, gold, matches, wins, commander, tutorialDone, unlocked, army, deployment, progression, campaignCleared`.
  - `ID_MAP` and `CLASS_MAP`.
  - `toV2(V1 v1, RosterMath.Catalog c) -> Profile`.
  - `upgradeFile(Path file, Function<String,V1> parse, Function<Profile,String> write, RosterMath.Catalog c) -> Profile`: returns null when `schema >= 2`. Otherwise it writes `<uuid>.v1.json` once, then the new file via a temp file and a move. A parse failure throws `IllegalStateException` and touches nothing.

- [ ] **Step 1: Write the failing `HousesCheck`.** Build a fixture `Catalog` of all 30 ids (house, rarity and weight per spec section 2). Assertions:
```java
// counters (Spec.COUNTERS), hit(...) builds a Hit
check(eq(counters(hit("arrow","shield",10,true)),0.35) && eq(counters(hit("arrow","shield",10,false)),0.75) && eq(counters(hit("arrow","shield",150,true)),1.0), "C1/C2 front only");
check(eq(counters(hit("melee","shield",10,true)),0.6) && eq(counters(hit("melee","shield",130,true)),1.15), "C3 front, C16 flank");
check(eq(counters(hit("melee,sunder","shield",10,true)),1.4) && eq(counters(hit("arrow,armour_bolt","shield",10,true)),0.7) && eq(counters(hit("magic","shield",10,true)),1.0), "C14 C15 C4");
check(eq(counters(impact("melee","ranged")),1.5) && eq(counters(charging("melee","ranged")),1.2) && eq(counters(impact("melee,beast,cavalry","ranged")),1.5) && eq(counters(hit("melee,beast,cavalry","ranged",10,false)),1.3), "C5-C7 highest applies");
check(eq(counters(hit("melee,spear","beast",10,false)),1.6) && eq(counters(hit("melee,beast","spear,holding",10,false)),0.7) && eq(counters(hit("melee,beast","spear,holding",120,false)),1.15), "C8 C9");
check(eq(counters(hit("siege","structure",0,false)),4.0) && eq(counters(hit("nonsiege","structure",0,false)),0.25) && eq(counters(hit("siege","large",0,false)),1.3) && eq(counters(hit("siege","small",0,false)),0.6), "C10-C13");
check(eq(counters(mist("arrow,shot")),0.5) && eq(counters(holdHit("arrow,shot,steady","any")),1.2) && eq(counters(noSky("arrow,shot,ambush")),1.25) && eq(counters(holdHit("melee,brace","charging")),1.3), "C17 C18");
check(total(0.1,1,1,1,.25,2.5)==0.25 && total(2,1.5,1,1,.25,2.5)==2.5 && eq(total(1.4,0.85,1,0.75,.25,2.5),0.8925), "clamp");
// traits
check(traitTier(5,T)==0 && traitTier(6,T)==1 && traitTier(10,T)==2 && traitTier(14,T)==3 && traitTier(20,T)==3, "tiers 6/10/14");
check(houseSlots(SPEC_EXAMPLE_BANNER,cat).equals(Map.of("gilded",15,"graveborn",5)), "spec 6.2 banner: Gilded 15, Graveborn 5");
check(eq(bulwark(trait("gilded",3),true,"arrow"),0.55) && eq(bulwark(trait("gilded",3),true,"blight"),0.55) && bulwark(trait("gilded",3),false,"arrow")==1 && bulwark(trait("gilded",3),true,"melee")==1, "bulwark");
check(eq(packHunters(trait("wildmoot",2),3,0),1.2) && eq(packHunters(trait("wildmoot",2),2,120),1.2) && eq(packHunters(trait("wildmoot",2),3,120),1.2) && packHunters(trait("wildmoot",2),2,90)==1, "pack hunters once");
check(eq(entrench(trait("ironclad",1),true,false),0.9) && eq(entrench(trait("ironclad",1),true,true),0.8) && entrench(trait("ironclad",1),false,true)==1, "entrench doubled on points");
// cap, stars
for(var e:Spec.EVOLUTIONS) check(cat.rarityOf(e.troop()).matches("common|rare") && rarity("common").starPct3()+Math.max(e.healthPct(),e.damagePct()) <= Spec.RECRUITMENT[0].capPct()+1e-9, "cap "+e.id());
check(progressionMultiplier(.06,.06,.12)==1.12 && progressionMultiplier(.12,.06,.12)==1.12 && eq(progressionMultiplier(.03,0,.12),1.03), "cap clamps");
check(starFromCopies(1,C)==1 && starFromCopies(2,C)==2 && starFromCopies(3,C)==2 && starFromCopies(4,C)==3, "4 copies for star 3");
check(addCopyAt(4,"common")==2 && addCopyAt(4,"rare")==4 && addCopyAt(4,"epic")==8 && addCopyAt(1,"common")==0, "dismantle past star 3");
// rolls (shares() runs 200,000 rolls with new SplittableRandom(7) and epicPity = Integer.MAX_VALUE)
var sh=shares(200_000, new SplittableRandom(7), Integer.MAX_VALUE);
check(Math.abs(sh.common()-.86)<=.005 && Math.abs(sh.rare()-.12)<=.004 && Math.abs(sh.epic()-.02)<=.002, "weights 43/6/2");
check(firstEpicWithZeroRng()==40 && pityAfterEpic()==0, "pity: 40th roll gives the Epic, then resets");
check(noLegendaryIn(10_000), "Legendaries never roll");
// banners
check(validBanner(SPEC_EXAMPLE_BANNER,cat,ALL)==null && "missing_marker".equals(validBanner(V1_DAWN_DEFAULT,cat,ALL)), "validation");
check(repack(V1_DAWN_DEFAULT,cat,ALL).equals(EXPECTED_DAWN_REPACK), "v1 Dawn line repacks to knight x12, archer x6, mage, ^");
check(legendaries(repack(TWO_LEGENDARIES,cat,ALL))==1 && epics(repack(THREE_EPICS,cat,ALL))==2 && repack(THREE_EPICS,cat,ALL).size()==20, "limits and size");
// renown, rewards, mastery
check(renownLevel(2,R)==0 && renownLevel(3,R)==1 && renownLevel(29,R)==4 && renownLevel(30,R)==5, "renown 3/8/15/22/30");
var w=matchRewards(Map.of("gilded",14,"graveborn",6),T,HOUSES,true); check(w.shards().equals(Map.of("gilded",2,"graveborn",1)) && w.renown().equals(Map.of("gilded",2,"graveborn",2)), "win rewards");
var l=matchRewards(Map.of("gilded",14,"graveborn",5),T,HOUSES,false); check(l.shards().equals(Map.of("gilded",1)) && l.renown().equals(Map.of("gilded",1)), "loss rewards, tier 0 earns nothing");
check(masteryLevel(39,8,M)==0 && masteryLevel(40,8,M)==1 && masteryLevel(150,24,M)==1 && masteryLevel(400,60,M)==3, "mastery needs both");
```
- [ ] **Step 2: Write the failing `ProfileMigrationCheck`.** Build the fixtures in Java and use parse/write stubs (no Gson):
  - `mig-fresh` (v1 defaults: Dawn knight x12, archer x6, mage x2; Dusk revenant x12, bonebow x6, hexer x2; gold 420; vanguard):
    - `owned` keys are exactly the 10 Commons, all at star 1 and stage 1 (`mage` and `hexer` are refunded, not owned; Kort's decision 2026-10-17).
    - banner = knight x12, archer x6, `empty` x2; commander `champion`.
    - gold = v1 gold + 600 (300 each for the `mage` and `hexer` starters); matches, wins and campaignCleared are copied.
    - shards, renown and pity each have five House keys at 0; `schema==2`.
  - `mig-full` (all 12 v1 ids unlocked): adds sentinel, longbow, reaper and stalker. There is no `mage`, `hexer`, `oracle` or `wraith` key; gold = v1 gold + 1200 (300 for each of those four).
  - `mig-invalid-dawn` (a Dawn line of 7): banner = revenant x12, bonebow x6, `empty` x2.
  - `mig-both-invalid`: banner = `starterBanner()`.
  - `mig-class`: marksman maps to warden, arcanist to warlock, unknown to champion.
  - `mig-progression`: xp, claims and titles (including `Vanguard`) are copied unchanged.
  - `mig-file`: in a temp dir, `upgradeFile` writes `<id>.v1.json` byte-equal to the original, and a new `<id>.json` containing `"schema":2`.
  - `mig-idempotent`: a second call returns null and both files are byte-unchanged.
  - `mig-corrupt`: parse throws, giving `IllegalStateException`. The original is unchanged and there is no `.v1.json` and no `.tmp`.
- [ ] **Step 3: Run the standalone checks; expect a compile failure** (missing classes and Spec records).
- [ ] **Step 4: Add the sheets above and the `combatDiscipline` columns.** Preflight additions:
  - shape rules for the new sheets;
  - the cap rule `starPct3 + max(healthPct, damagePct) <= capPct` for every evolution row (in this task the rarity is looked up from `houses` membership);
  - every `botBanners.slots` has 20 entries, the weights sum to 20, and markers follow their unit;
  - `counters.condition` is in the closed list, and `abilities.mechanic` and `trigger` are in theirs.

  Run preflight; expect `Preflight clean`.
- [ ] **Step 5: Implement `RosterMath`, `CounterMath`, `Profile` and `ProfileMigration`** per the Interfaces. `ProfileMigration.ID_MAP` is the spec 6.3 table. `toV2`:
  - grants every Common of every House (from the catalog) plus the mapped v1 ids;
  - does not grant `mage` or `hexer`; instead adds `refundGold` (sheet `economy`, 300) to gold for each of `mage`, `hexer`, `oracle`, `wraith` that the v1 profile had unlocked (`mage` and `hexer` always, as starters);
  - takes the Dawn deployment if it is 20 entries of known Dawn ids or `empty` with at least one troop, else the Dusk one, else `starterBanner()`;
  - repacks the result through `validBanner` and `repack`.
- [ ] **Step 6: Run the standalone checks; expect all three summary lines.**
- [ ] **Step 7: Add `HousesCheck` and `ProfileMigrationCheck` to `forbidden`, then commit** `"Phase 2: House sheets, counter/roster maths, profile v2 and migration (pure)"`.

---

### Task 2: Troop roster, profile v2 and banner sides live

**Files:**
- Modify: `sheets/design.json`:
  - `troops` is rewritten (below), and `factions` becomes the side sheet.
  - `groups` gains `{"id":"cavalry","label":"Cavalry","role":"flank"}` and `{"id":"siege","label":"Siege","role":"siege"}`; `commandInterface.selection` gains `cavalry,siege`.
  - `campaign` drops `rollCost`, `duplicateRefund` and `starterGold`.
  - `classes` is rewritten with the Task 4 values, written now so the migrated class ids resolve.
- Modify: `tools/preflight.py`:
  - drop the faction and count rules;
  - add the troop references to house, rarity, ability and evolveLine;
  - Commons and Rares need an evolve line, and Epics and Legendaries need `none`;
  - each fieldable House has exactly 2 Common, 2 Rare, 1 Epic and 1 Legendary, and every House has at most that;
  - botBanners ids resolve.
- Modify: `src/main/java/gg/gravesandglory/Campaign.java`: `Profile` moves out; load, import and save; `banner`, `botBanner`, `damage`, `health`, `chooseSides`; the `slot:`, `colour:` and `roll` commands; `army:` and `unlock:` are refused.
- Modify: `GravesAndGlory.java` (`reinforce`, `Squad.banner`, `Battle.sides`, `side`, training limits).
- Modify: `CampaignClient.java`, `CampaignLobbyScreen.java` (banner editor tab), `ArmyCards.java`, `RecruitmentScreen.java`, `CampScreen.java`.
- Modify: `GravesAndGloryClient.java` (husk override keyed on House `gilded`) and `DarkUi.java` (side colours, glyphs `sun, moon, star, wave`).
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (side colours, banners) and `test-tools/standalone/test_ui_standard.py` (new colour literals).
- Create: `test-tools/java/gg/gravesandglory/Phase2Checks.java` (scaffold and suite `banners`).
- Modify: `test-tools/isolated-checks.gradle` (`gravesandglory.phase2Checks` from `GRAVES_PHASE2_CHECKS`) and `test-tools/resources/fabric.mod.json`.
- Modify: `CampaignChecks.java` and `Phase1Checks.java` so they return early when `gravesandglory.phase2Checks` is non-empty.
- Create: `test-tools/resources/fixtures/profile-v1-partial.json` (a v1 file: Dawn default line, sentinel and wraith unlocked, gold 420, marksman).

**Troop rows.** Columns: `id, house, rarity, name, entity, group, health, damage, range, ranged, speed, cooldown, cost, trainingSeconds (double), magic, weapon, helmet, chest, tags, ability, evolveLine, slotWeight, pennant (true), texture`.
- `ranged` is true when range > 3.
- `slotWeight` is 1/1/2/4 by rarity.
- `evolveLine` is the troop's own id for Commons and Rares, else `none`.
- `magic` is true only for mage, hexer, wm_seer and tc_mist.
- `texture` is `gravesandglory:textures/entity/dawn_guard.png` for husks, else `minecraft:textures/entity/<vanilla path>.png`.
- Items are `minecraft:` ids, and `air` means `minecraft:air`.

| id | rar | name | entity | group | HP | Dmg | Rng | Spd | CD | Cost | Train s | weapon / helmet / chest | tags | ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| knight | C | Order Squire | husk | infantry | 32 | 5 | 2.8 | 0.28 | 25 | 22 | 4.0 | iron_sword / iron_helmet / iron_chestplate | shield | shield |
| archer | C | Order Archer | husk | ranged | 20 | 4 | 22.0 | 0.30 | 40 | 24 | 4.0 | bow / chainmail_helmet / leather_chestplate | none | steady |
| sentinel | R | Templar | husk | infantry | 46 | 6 | 2.8 | 0.25 | 28 | 48 | 7.0 | diamond_sword / diamond_helmet / iron_chestplate | shield | shield |
| longbow | R | Order Longbow | husk | ranged | 18 | 6 | 30.0 | 0.28 | 45 | 46 | 7.0 | bow / leather_helmet / leather_chestplate | none | pierce_longbow |
| mage | E | Sunweaver | husk | support | 24 | 0 | 18.0 | 0.29 | 55 | 70 | 9.0 | blaze_rod / golden_helmet / leather_chestplate | none | mend_sunweaver |
| gl_marshal | L | Lord Marshal Aldric | husk | infantry | 80 | 9 | 2.8 | 0.27 | 22 | 140 | 15.0 | netherite_sword / golden_helmet / diamond_chestplate | shield,large | oath_banner |
| revenant | C | Risen | wither_skeleton* | infantry | 26 | 4 | 2.8 | 0.25 | 25 | 14 | 3.0 | iron_sword / chainmail_helmet / chainmail_chestplate | fearless | fearless |
| bonebow | C | Bone Archer | skeleton | ranged | 18 | 4 | 22.0 | 0.27 | 40 | 16 | 3.0 | bow / chainmail_helmet / leather_chestplate | fearless | fearless |
| reaper | R | Grave Reaper | wither_skeleton | infantry | 34 | 8 | 2.8 | 0.30 | 30 | 40 | 6.0 | iron_hoe / netherite_helmet / iron_chestplate | none | cleave_reaper |
| stalker | R | Barrow Stalker | skeleton* | ranged | 20 | 5 | 26.0 | 0.30 | 40 | 40 | 6.0 | bow / chainmail_helmet / air | none | frostshot |
| hexer | E | Gravecaller | wither_skeleton | support | 22 | 7 | 18.0 | 0.27 | 55 | 60 | 8.0 | amethyst_shard / golden_helmet / leather_chestplate | none | blight_gravecaller |
| gb_hollowking | L | The Hollow King | wither_skeleton | infantry | 70 | 10 | 3.0 | 0.26 | 30 | 120 | 15.0 | netherite_sword / golden_helmet / netherite_chestplate | large | dread |
| wm_hound | C | Moot Hound | wolf | cavalry | 18 | 4 | 2.2 | 0.36 | 20 | 16 | 3.0 | air / air / air | beast,cavalry,small | pounce |
| wm_hunter | C | Hide Hunter | zombified_piglin | ranged | 18 | 4 | 20.0 | 0.31 | 36 | 20 | 3.5 | bow / leather_helmet / leather_chestplate | none | ambush |
| wm_stalker | R | Web Stalker | spider | cavalry | 28 | 6 | 2.6 | 0.34 | 25 | 40 | 6.0 | air / air / air | beast,cavalry,small | ensnare |
| wm_tusk | R | Tusk Spear | zombified_piglin | infantry | 30 | 6 | 3.0 | 0.31 | 28 | 38 | 6.0 | trident / leather_helmet / leather_chestplate | spear | brace |
| wm_seer | E | Bloodhowl Seer | zombified_piglin | support | 24 | 3 | 14.0 | 0.31 | 40 | 60 | 8.0 | bone / leather_helmet / leather_chestplate | none | howl |
| wm_oldhorn | L | Old Horn, the Antlered Alpha | polar_bear | cavalry | 90 | 11 | 3.0 | 0.30 | 30 | 130 | 15.0 | air / air / air | beast,cavalry,large | trample |
| ic_hearthguard | C | Hearthguard | vindicator | infantry | 34 | 5 | 2.8 | 0.24 | 28 | 22 | 4.5 | iron_axe / iron_helmet / iron_chestplate | shield | shield |
| ic_arbalist | C | Arbalist | pillager | ranged | 20 | 6 | 24.0 | 0.25 | 60 | 24 | 4.5 | crossbow / chainmail_helmet / leather_chestplate | none | armour_bolt |
| ic_breaker | R | Shieldbreaker | vindicator | infantry | 38 | 8 | 2.8 | 0.25 | 32 | 44 | 7.0 | netherite_axe / iron_helmet / iron_chestplate | none | sunder |
| ic_ballista | R | Ballista Crew | pillager | siege | 24 | 10 | 34.0 | 0.20 | 80 | 48 | 8.0 | crossbow / iron_helmet / leather_chestplate | siege | pierce_ballista |
| ic_mortar | E | Ironhold Mortar | pillager | siege | 22 | 9 | 36.0 | 0.18 | 100 | 70 | 10.0 | air / iron_helmet / iron_chestplate | siege,large | shell |
| ic_colossus | L | Brassjaw, the Forge Colossus | iron_golem | infantry | 120 | 12 | 3.0 | 0.22 | 36 | 150 | 17.0 | air / air / air | fearless,large | forge_slam |
| tc_raider | C | Drowned Raider | drowned | infantry | 28 | 5 | 2.8 | 0.29 | 24 | 18 | 3.5 | iron_sword / chainmail_helmet / air | none | brine |
| tc_harpooner | C | Harpooner | drowned | ranged | 20 | 5 | 16.0 | 0.29 | 40 | 20 | 3.5 | trident / leather_helmet / air | none | harpoon |
| tc_tideguard | R | Trident Guard | drowned | infantry | 34 | 7 | 3.0 | 0.28 | 28 | 42 | 6.5 | trident / iron_helmet / chainmail_chestplate | spear | brace |
| tc_mist | R | Mist Caller | evoker | support | 22 | 0 | 18.0 | 0.28 | 60 | 46 | 7.0 | air / air / air | none | mist |
| tc_brute | E | Kraken-touched Brute | ravager | cavalry | 70 | 9 | 3.0 | 0.28 | 34 | 75 | 10.0 | air / air / air | beast,cavalry,large | tentacle_sweep |
| tc_admiral | L | Admiral Vael, the Drowned Tide | drowned | infantry | 64 | 9 | 3.0 | 0.30 | 26 | 130 | 15.0 | trident / golden_helmet / netherite_chestplate | spear,large | undertow |

\* `revenant` stays `wither_skeleton` and `stalker` stays `skeleton` until Task 7 adds `ArmyZombie` and `ArmyStray` and switches them to `zombie` and `stray`.

**Side rows** (`factions` columns `id, name, color, glyph, spawnX, spawnZ, barracksX, barracksZ`):

| id | name | color | glyph | spawnX | spawnZ | barracksX | barracksZ |
|---|---|---|---|---|---|---|---|
| gold | Gold Banner | 14922561 | sun | -108 | 0 | -108 | -24 |
| violet | Violet Banner | 11566335 | moon | 108 | 0 | 108 | 24 |
| crimson | Crimson Banner | 14701130 | star | -108 | 0 | -108 | -24 |
| teal | Teal Banner | 4179904 | wave | 108 | 0 | 108 | 24 |

Positions are always read by team index from rows 0 and 1. Colour, name and glyph are read through `Battle.side(team)`.

**Interfaces:**
- Consumes: the Task 1 classes; Phase 1 `Campaign.botDecide(Squad, Spec.BotCommandRow)`, `Campaign.tier`, `Squad.tier`, `DarkUi.teamMark/teamText/teamColor` and `Phase1Checks.server/skirmish`.
- Produces (profile and sides):
  - `RosterMath.Catalog.fromSpec()`.
  - The top-level `Profile`, used by `Campaign.profile(UUID) -> Profile`.
  - `Battle.sides` (`int[2]`, default `{0,1}`) and `Battle.side(int team) -> Spec.FactionsRow`.
  - `Campaign.chooseSides()`, called in `prepareRound`: team 0 takes its first human's `bannerColour`; team 1 takes its first human's colour if different, else the first free row in sheet order; bots take free rows.
  - Client `CampaignClient.sides` (`String[2]`, from the snapshot `"sides"`).
- Produces (banners and stats):
  - `Squad.banner` (`List<String>`, set in `prepareRound`).
  - `Campaign.botBanner(Squad s) -> List<String>`: picks from rows with `difficulty == s.tier` whose Houses are all fieldable, at index `s.lane % n`. The harness may assign `s.banner` directly.
  - `Campaign.damage(Unit u)` and `Campaign.health(Squad s, Spec.TroopsRow t)` now return only `progressionMultiplier` (bots get 1.0).
  - Commands `slot:<i>:<id>` (sets the slot via `repack`; refuses unowned troops) and `colour:<side>`.
- Barracks training rule: a troop id can be trained only while its living plus queued units are below its count in `s.banner`. The 20-slot check counts slot weights.
- Produces (harness) `Phase2Checks`:
  - `report(String id, boolean pass, String why)`: a MeltyTest line plus `[p2] id=pass why`.
  - `shot(MinecraftClient c, String name)`: saves `p2-<name>.png`.
  - `finish(MinecraftClient c, String suite)`: reports `p2-<suite>-suite`.
  - `interface Suite { void step(MinecraftClient c, int tick); }`, registered by name.
  - It reuses `Phase1Checks.server(...)` and `Phase1Checks.skirmish(...)`, and adds `skirmishBanners(Battle b, ServerPlayerEntity p, int map, String banner0, String banner1)`.

- [ ] **Step 1: Write suite `banners`:**
  - `p2-migrate-live`: copy `fixtures/profile-v1-partial.json` into the test profile dir as `<player uuid>.json`, drop the cached profile, and call `campaign.profile(uuid)`. Expect: owned has sentinel, mage at star 1, hexer with copies 2 and stars 2 (wraith merged), and the 10 Commons; commander `warden`; `<uuid>.v1.json` exists and equals the fixture.
  - `p2-sides`: send `colour:crimson` and start a 1v1 skirmish. Expect `b.side(team).id()=="crimson"`, the bot side `gold`, and `BattleEffects.lastFlashColor == 14701130` after a melee hit by the player's troop.
  - `p2-bot-banners`: in a Normal 3v3 practice, every bot `banner` is a `*_normal` row of a fieldable House. After reinforce, each bot has the units its banner lists (an Epic counted once, its `^` slot empty).
  - `p2-banner-edit`: `slot:19:mage` keeps the banner valid (repacked, `validBanner == null`); `slot:0:gl_marshal` (unowned) leaves the banner unchanged.
  - `p2-train-limit`: with banner knight x12, archer x8 and 12 knights alive, `train:knight` is refused, and so is `train:sentinel` (not in the banner).
- [ ] **Step 2: Run suite `banners`; expect failures** (commands and fields missing).
- [ ] **Step 3: Rewrite `troops`, `factions`, `groups`, `commandInterface`, `campaign` and `classes` (Task 4 rows), and update `preflight.py`.** Expect preflight clean. Add the `HousesCheck` assertion that `Catalog.fromSpec()` equals the fixture catalog.
- [ ] **Step 4: Move `Profile` out of `Campaign`.** `profile(UUID)` reads the file and calls `ProfileMigration.upgradeFile(file, t->JSON.fromJson(t,V1.class), JSON::toJson, Catalog.fromSpec())`; when that returns null it parses `Profile`; then it runs `sanitize`. `importProfile` migrates a v1 portable copy with `toV2` before the existing allow/ignore rule (the server copy still wins). `CampaignClient` parses `Profile.class`.
- [ ] **Step 5: Use banners in battle.**
  - `reinforce` walks `s.banner`, skipping `empty` and `^`; the slot index is the position.
  - Bot buying in `Campaign.tick` counts against `s.banner`, and `full` in `botDecide` is the number of units in `s.banner`.
  - The `Profile()` default is the 10 Commons and `starterBanner()`.
- [ ] **Step 6: Add sides.**
  - Add `Battle.sides/side`, `chooseSides`, the `colour:` command and the snapshot `"sides"`.
  - Replace every colour or name read of `Spec.FACTIONS[team]` (grep for them) with `side(team)`.
  - `DarkUi.teamMark` draws the side row's glyph, and `teamColor` returns its colour (from `CampaignClient.sides`).
- [ ] **Step 7: Adapt the UI.**
  - The lobby "Army collection" tab becomes the 20-slot banner editor, with a House filter row. It shows owned troops only; Epic and Legendary cards show their slot width.
  - `ArmyCards.Recruit` tints cards by `houses.accent`.
  - The barracks `RecruitmentScreen` lists the unique ids of the player's own banner.
  - The camp `recruit` page keeps its layout until Task 3.
  - Update `Phase1Checks` (side colours, `gilded_normal` for the synthetic shield bot) and `test_ui_standard.py`.
- [ ] **Step 8: Compile, then run the standalone checks, suite `banners`, and Phase 1 suites `combat,morale,bots`.** Expected: all pass.
- [ ] **Step 9: Commit** `"Phase 2: 30-troop roster sheet, profile v2 migration on load, banners and banner sides"`.

---

### Task 3: Combat engine (batch 0 close-out)

**Files:**
- Create: `src/main/java/gg/gravesandglory/{ArmyMobs,Combat,Abilities,Houses}.java`.
- Create: `src/main/resources/data/gravesandglory/damage_type/troop.json` (`{"message_id":"gravesandglory.troop","exhaustion":0.1,"scaling":"never"}`) and `src/main/resources/data/minecraft/tags/damage_type/bypasses_shield.json` (`{"replace":false,"values":["gravesandglory:troop"]}`).
- Create: `src/main/resources/assets/gravesandglory/textures/gui/house/{gilded,graveborn,wildmoot,ironclad,tidecallers}.png`: 16x16, made with the pixel-art skill (crowned shield, cracked tombstone, antler, anvil, wave crest).
- Create: `tools/balance_gate.py`, `docs/PHASE-2-BALANCE.md`.
- Modify: `GravesAndGlory.java`:
  - `ArmyHusk`, `ArmySkeleton` and `ArmyWither` implement `ArmyMob` and gain the daylight and water overrides;
  - spawning and stale cleanup go through `ArmyMobs`;
  - new `Unit` fields; `STATE` becomes `state_v6`.
- Modify: `Tactics.java`: `ranged()` replaces every `range()>3`; the attack branch goes through `Abilities`; the shield is raised based on the `shield` tag.
- Modify: `Projectiles.java` (landing calls `Combat.resolve`) and `ArmyCards.java` (`ArmyMobs.portrait`).
- Modify: `CommandMode.java` and `GravesAndGloryClient.java` (HUD House strip), `CampScreen.java` (`recruit` page with House tabs), `Campaign.java` (`roll:<house>`).
- Modify: `test-tools/java/.../BotSimulation.java` and `build.gradle` (`simBanner0/1`), and `Phase2Checks.java` (suite `engine`).

**Interfaces:**
- Consumes: Phase 1 `Projectiles.Shot/fire/tick`, `Unit.lastHitBy`, `BattleMath.relativeBearing` and `MatchStats.SquadStats`.
- Produces (entities):
  - `interface ArmyMob {}`.
  - `ArmyMobs.create(String entity, World w) -> MobEntity` (throws `IllegalStateException` for an unregistered id), `ArmyMobs.register(String entity, Function<World,MobEntity> f)`, `ArmyMobs.portrait(String entity, World w) -> MobEntity` (any vanilla id via `Registries.ENTITY_TYPE`) and `ArmyMobs.isTroop(Entity e) -> boolean`.
  - Each House registers its own subclasses in its task.
- Produces (damage):
  - `Combat.resolve(Battle b, Unit attacker, LivingEntity target, float base, String kind, boolean secondary) -> float`, with kinds `melee, arrow, bolt, blight, slam`. It builds a `CounterMath.Hit` from the attacker's tags, its ability id and the dynamic tags, multiplies `counters x Houses.traitFactor x Abilities.auraFactor x Commander.factor`, clamps the product, and applies it with `Combat.source(b, attacker.mob)` of type `gravesandglory:troop`. It sets `lastHitBy` and returns the amount applied. A base of 0 never calls `damage`.
  - `Combat.LAST`: an `ArrayDeque<HitLog>` of 64 entries, with `record HitLog(int attacker, int target, float base, double counters, double traits, double auras, double commander, double total, List<String> rows)`.
- Produces (abilities):
  - `Abilities.row(Spec.TroopsRow t) -> Spec.AbilitiesRow`.
  - `Abilities.tick(Battle b, Unit u, LivingEntity enemy, long time)` handles cast and aura triggers.
  - `Abilities.attack(Battle b, Unit u, LivingEntity enemy, long time)` replaces the direct `enemy.damage` in `Tactics`.
  - `Abilities.onHit(Battle b, Unit u, LivingEntity target, float dealt, String kind)` runs on-hit mechanics; it is a no-op for `secondary` hits.
  - `Abilities.range(Battle b, Unit u) -> double` and `Abilities.auraFactor(Battle b, Unit attacker, LivingEntity target) -> double`.
  - In this task only `shield`, `mend` and `blight` behave; other mechanics stay no-ops until their House task. Numbers come from the ability row, with `combatDiscipline` defaults where a row says 0.
- Produces (traits):
  - `Houses.recompute(Squad s)`, called at reinforce, on spawn and on death. It sets `Squad.traits` (`Map<String,Integer>`, House to tier) from living units' slot weights.
  - `Houses.bannerTiers(Squad s) -> Map<String,Integer>`, from `s.banner`, used for rewards.
  - `Houses.traitFactor(Battle b, Unit attacker, LivingEntity target, String kind) -> double`: 1.0 until the House tasks fill it in.
  - `Squad.stats.traitSeconds` (`Map<String,Integer>`): +1 per second for each House at tier 1 or higher.
- Produces (state fields):
  - `Unit` gains `Set<String> tags`, `int stage`, `boolean temporary`, `long expiresAt`, `long stunnedUntil`, `boolean rose`, `double travelSinceAttack` and `long chargeHitAt`.
  - `Squad` gains `Map<String,Long> chargedAt`, set where Phase 1 plays the charge horn.
  - `state_v6` layout (written now, filled later), after the Phase 1 fields: the side ids for teams 0 and 1 (2 strings); the own squad's trait count (byte), then `(house string, tier byte)` pairs; the commander `active` id (string), `readyInTicks` (int) and `activeForTicks` (int).
- Produces (rolls):
  - Command `roll:<house>` (fieldable or not). It requires gold >= `rollCost`, uses `RosterMath.roll` with `pity[house]` and `addCopy`, adds shards from any dismantle, and refunds on a failed save.
  - Harness hook `Campaign.rollRng` (`RandomGenerator`, defaulting to a `world.random` adapter).
- Produces (simulation):
  - `BotSimulation` properties `gravesandglory.simBanner0/1` set `s.banner` for every bot of team 0 or team 1.
  - `results.csv` gains the trailing columns `banner0,banner1`, and the summary line gains `banner0=<id> banner1=<id>`.
  - `tools/balance_gate.py <csv1> <csv2> <bannerId>` prints `gate <a> vs <b> win=<P>% pass|fail`.

- [ ] **Step 1: Write suite `engine`:**
  - `p2-armymobs`: `ArmyMobs.create` for husk, skeleton and wither_skeleton returns an `ArmyMob` whose goal selector holds only `SwimGoal`; `create("wolf",...)` throws.
  - `p2-no-burn`: at noon on map 0, a husk, a skeleton and a wither skeleton without helmets stand in the sun for 200 ticks and are never on fire.
  - `p2-no-convert`: an `ArmyHusk` stays in water for 700 ticks and is still an `ArmyHusk`.
  - `p2-shield-counter`: a Graveborn bonebow shoots a raised-shield knight from the front. The `Combat.LAST` entry has `counters == 0.35` and the knight lost health (the shield was bypassed). From behind, `counters == 1.0`.
  - `p2-damage-type`: that hit's source type id is `gravesandglory:troop`.
  - `p2-abilities-shell`: a Sunweaver heals a hurt knight by 5 within 100 ticks. A Gravecaller blight hits enemies within 2.5 of its centre after 20 ticks and applies Slowness I for 40.
  - `p2-hud-house`: the client receives traits for the player's starter banner (`gilded` tier 3). Screenshot `engine-hud` shows the House strip glyph with III.
  - `p2-roll-house`: with gold 100, `roll:wildmoot` leaves gold 0, one wildmoot id owned (or a copy added) and `pity.wildmoot` updated, and the save succeeds.
  - `p2-recruit-tabs`: load `profile-v1-partial.json` as in `p2-migrate-live` and open the camp page `recruit`. Expect five House tabs:
    - the Gilded tab shows knight, archer, sentinel and mage owned;
    - Graveborn shows revenant, bonebow and hexer (star 2);
    - Wildmoot, Ironclad and Tidecallers show their two Commons owned;
    - gold 420 is shown.

    Screenshots: `engine-recruit-gilded`, `engine-recruit-graveborn`, `engine-recruit-wildmoot`.
  - `p2-tick-cost`, a reusable fixture `tickCost(Battle b, List<String> banners)`: 8 bot squads (160 troops) for 600 ticks. Log `[p2] tick-cost avg=<ms>`; pass when `getTickTime() < 40`.
- [ ] **Step 2: Run suite `engine`; expect failures.**
- [ ] **Step 3: Add `ArmyMobs`, `ArmyMob` and the daylight and water overrides, and replace the three entity switches with the factory.** The switches are in `Battle.spawn`, `ArmyCards.portrait` and the stale-entity filter. Spawn sets `Unit.tags`, adds the off-hand shield when the tags contain `shield`, and sets the custom name to the stage name (stage 1 is the troop name).
- [ ] **Step 4: Add the damage type and `Combat.resolve`.** `Tactics` and `Projectiles` route all troop damage through it: `arrow` and damaging `bolt` shots resolve on landing, and 0-amount bolts stay visual.
- [ ] **Step 5: Add the `Abilities` shell and the `ranged` column** (replacing every `spec.range()>3`). Add `Houses.recompute` and `traitSeconds`. Add `state_v6` and the HUD House strip: `DarkUi.houseMark(DrawContext g, String house, int tier, int x, int y)` draws the 16x16 glyph plus a Roman numeral next to the group panels.
- [ ] **Step 6: Build the camp `recruit` page with House tabs, and add `roll:<house>`.**
  - A tab row of five glyph tabs.
  - A grid of the House's six cards. Each card shows the portrait from `ArmyMobs.portrait` with the stage name, the rarity label, stars as pips and the stage; unowned troops show LOCKED.
  - Primary button `ROLL <HOUSE> / 100 GOLD`.
  - Pity line `Epic guaranteed within <epicPity-pity> rolls`.
- [ ] **Step 7: Add banners to BotSimulation, and add `tools/balance_gate.py`.**
- [ ] **Step 8: Compile; run the standalone checks and suite `engine`.** Expected: all `p2-engine` ids pass, and the tester confirms the three recruit screenshots show the migrated rows and gold 420.
- [ ] **Step 9: No-regression sim.** Do a balance run of `gilded_normal` vs `graveborn_normal`. There is no gate in this task; log the result as the Phase 2 baseline in `docs/PHASE-2-BALANCE.md`. Run Phase 1 suites `combat,morale,reinforce,report` again; all pass.
- [ ] **Step 10: Commit** `"Phase 2 batch 0: ArmyMobs, Combat.resolve with troop damage type, abilities shell, House HUD and recruitment tabs"`.

---

### Task 4: Commander class framework and passives

**Files:**
- Create: `src/main/java/gg/gravesandglory/Commander.java`.
- Modify: `sheets/design.json` `classes` (values below; `group` and `bonus` are kept, with `bonus` = passiveText).
- Modify: `Campaign.java` (class command; bot classes `Spec.CLASSES[i % 6]`).
- Modify: `GravesAndGlory.java` (`ability` command, respawn seconds, player hits in `allowDamage` go through `Commander`, point gold).
- Modify: `Tactics.java` (range bonus via `Abilities.range`).
- Modify: `GravesAndGloryClient.java` (key `key.gravesandglory.ability`, default H; hint id `ability`; HUD cooldown tile).
- Modify: `CampaignLobbyScreen.java` (class tab: six cards under HERO and GENERAL headings, with active and passive text).
- Modify: `lang/en_us.json` and `Phase2Checks.java` (suite `commander`).

**Classes rows.** Columns: `id, name, group, bonus, track, active, cooldownTicks, durationTicks, radius, range, value1, value2, value3, passive, passiveValue, passiveValue2, passiveRadius, activeText, passiveText`.

| id | track | active | cd | dur | radius | range | value1 | value2 | value3 | passiveValue | passiveValue2 | passiveRadius |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| champion | hero | stand_fast | 1800 | 160 | 12.0 | 0.0 | 0.25 (taken) | 0.0 | 0.0 | 4.0 (HP) | 0.10 (infantry dmg) | 10.0 |
| reaver | hero | bloodrush | 1500 | 200 | 0.0 | 0.0 | 0.30 (lifesteal) | 3.0 (refunds) | 0.0 | 2.0 (attack) | 9.0 (respawn s) | 0.0 |
| warden | hero | mark_prey | 1200 | 240 | 0.0 | 40.0 | 0.30 | 0.0 | 0.0 | 0.10 (ranged dmg) | 2.0 (range) | 0.0 |
| marshal | general | rally_horn | 2400 | 300 | 0.0 | 0.0 | 0.10 (speed) | 400.0 (steady ticks) | 0.0 | 0.20 (training) | 0.0 | 0.0 |
| warlock | general | grave_bloom | 2400 | 600 | 8.0 | 30.0 | 0.60 (HP share) | 6.0 (max) | 400.0 (death window) | 0.20 (magic cost) | 0.0 | 0.0 |
| quartermaster | general | forward_supply | 3000 | 1200 | 12.0 (enemy-free) | 30.0 (from base) | 40.0 (banner HP) | 0.0 | 0.0 | 2.0 (point gold) | 0.0 | 0.0 |

`group` values: champion infantry, reaver infantry, warden ranged, marshal all, warlock support, quartermaster all. `activeText` and `passiveText` are the spec section 5 effect and passive cells, at most 90 characters each.

**Interfaces:**
- Produces (activation):
  - `Commander.row(Squad s) -> Spec.ClassesRow`.
  - `interface Commander.Active { boolean start(Battle b, ServerPlayerEntity p, Squad s, Spec.ClassesRow c); void tick(Battle b, Squad s, Spec.ClassesRow c); void end(Battle b, Squad s); }`.
  - `Commander.ACTIVES` (`Map<String,Active>`), filled by the House tasks.
  - `Commander.activate(Battle b, ServerPlayerEntity p)`: refuses while `time < s.abilityReadyAt` or while the player is unseated. An unregistered active shows the notice "This command is not ready in this build."
  - `Commander.tick(Battle b)`.
- Produces (effects):
  - `Commander.factor(Battle b, Unit attacker, LivingEntity target) -> double`, on the attacker side: Champion gives infantry within `passiveRadius` of the player x(1+0.10); Warden gives ranged troops x1.10; Mark Prey gives x1.30 on the marked entity.
  - `Commander.takenFactor(Battle b, LivingEntity target) -> double`: Stand Fast gives x0.75.
  - `Commander.rangeBonus(Squad s, Unit u) -> double`, `Commander.trainingFactor(Squad s) -> double`, `Commander.costFactor(Squad s, Spec.TroopsRow t) -> double` (cost rounded with `Math.round`), `Commander.pointGoldBonus(Squad s) -> int` and `Commander.respawnSeconds(Squad s) -> int`.
  - `Commander.applyPlayer(ServerPlayerEntity p, Squad s)`: attribute modifiers for Champion (+4 max HP) and Reaver (+2 attack), removed on class change and in the lobby.
- `Squad` gains `long abilityReadyAt, abilityUntil` and `Object abilityData`. `Combat.resolve` multiplies `Commander.factor x Commander.takenFactor`.

- [ ] **Step 1: Write suite `commander`:**
  - `p2-class-passives`: for each class, set it via `class:<id>` and start a 1v1 skirmish:
    - champion: player max HP is 24; a knight within 10 of the player has `commander == 1.10` in `Combat.LAST`, and one at 20 blocks has 1.0;
    - reaver: the respawn countdown is 9 s;
    - warden: an archer's `Abilities.range` is 24 and its `commander` factor is 1.10;
    - marshal: knight training of 80 ticks becomes 64;
    - warlock: the Sunweaver's round cost is 56;
    - quartermaster: gold per held point per income tick is 5.
  - `p2-ability-key`: the default binding of `key.gravesandglory.ability` is H and differs from every other mod binding, and `hint("ability")` returns `H`. With no registered active, pressing it gives the notice and does not start a cooldown. The HUD tile shows the class active's name (screenshot `commander-tile`).
  - `p2-class-picker`: the camp class tab shows six cards in two tracks (screenshot `commander-classes`).
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement `Commander`, the key and tile, and the class tab.**
- [ ] **Step 4: Replace the hard-coded class branches** in `Campaign.damage/health` and `Tactics` (the marksman cooldown) with `Commander`. A grep confirms no `vanguard|marksman|arcanist` literal remains outside `ProfileMigration.CLASS_MAP`.
- [ ] **Step 5: Compile; run suites `commander` and `engine`; all pass. Commit** `"Phase 2: six commander classes framework, passives from the classes sheet, ability key H"`.

---

### Task 5: Skin pipeline spike, team pennants and Gilded art

**Files:**
- Create: `tools/build_skins.py`. It reads the `design.json` troops and evolutions plus the `art/` sources, writes the pack files below with deterministic numbering, and prints `skins: <n> textures, <m> models`.
- Create, for the six Gilded troops (pixel-art skill, 64x64, spec 2.7 rows):
  - `art/troops/<troop>_s<stage>.png`, plus an optional `_e.png` emissive layer;
  - `art/troops/<troop>.jem`, and `art/troops/<troop>_s3.jem` for the stage-3 extra part;
  - `art/pennant/pennant_<side>.png` (4).
- Generated and committed:
  - `src/main/resources/resourcepacks/armory/assets/minecraft/optifine/random/entity/zombie/husk{N}.png` and `husk.properties`;
  - `.../optifine/cem/husk{N}.jem` and `.../optifine/cem/husk.properties`;
  - `assets/gravesandglory/textures/entity/pennant.png`, with `assets/gravesandglory/optifine/random/entity/pennant.properties`.

  `husk.jem` stays model 1.
- Modify: `GravesAndGlory.java`: `Battle.joinSide(MobEntity mob, int team)` and `Battle.clearSides()` use scoreboard teams `gg_<side>`; a troop leaves its team on discard or death.
- Modify: `GravesAndGloryClient.java`: the husk renderer override applies only when `entity_texture_features` is not loaded.
- Modify: `Phase2Checks.java` (suites `spike` and `gilded-art`).

**Rules:** `build_skins.py` writes:
- in `<mob>.properties` (ETF), for every troop and stage: `textures.N=N` and `name.N=regex:^<escaped stage name>( \(Veteran\))?$`;
- in the EMF `<mob>.properties`, for every troop: `models.N=N` with a regex over stages 1 and 2, and a second model over stage 3.

Every variant model attaches `gg_pennant` (box `[-2,-14,2,4,8,1]` on `body`, texture `gravesandglory:textures/entity/pennant`) and keeps the Fresh Animations root entry (`<mob>_animations.jpm`) where one ships. `pennant.properties` maps `teams.N=gg_<side>` to the four side pennants.

**Interfaces:**
- Produces: `Battle.joinSide(MobEntity mob, int team)`, which adds the mob's UUID score holder to `gg_<side(team).id()>`, creating the team on first use.
- Produces: `Battle.clearSides()`, which empties all four teams in `startRound`, `returnLobby` and `switchMap`.
- Produces: `ArmyCards.portrait` sets the stage custom name, so cards show the variant.

- [ ] **Step 1: Make the spike art only.** Use placeholder flat-colour skins for `knight` stage 1 and `sentinel` stage 1, one variant JEM with the pennant, and four pennants. Run `python tools/build_skins.py`.
- [ ] **Step 2: Write suite `spike`.** Place two knights and two Templars (one of each per team, gold vs violet) 3 blocks apart in front of the camera at noon.
  - `p2-spike-names`: the custom names equal `Order Squire` and `Templar`.
  - `p2-spike-teams`: each mob is on `gg_gold` or `gg_violet`.
  - Screenshots: `spike-name-skin` (two different skins), `spike-name-model` (pennant box visible on both) and `spike-team-pennant` (gold and violet pennants differ).
  - `p2-teams-clean`: kill all four, then `returnLobby`; no `gg_*` team has members.
- [ ] **Step 3: Run suite `spike`. Gate:** the tester states for each screenshot whether the variant shows.
  - If all three show, continue.
  - If one fails, stop and report to the orchestrator. The fallback (spec 8.2) is a client lookup `TroopSkins.texture(MobEntity e) -> Identifier` by custom name and team, used by renderer overrides for the affected mob, and built only for the failing mechanism.
- [ ] **Step 4: Make the Gilded art.** Final skins for the six troops at each of their stages (Commons and Rares 3 stages, Epic and Legendary 1); the emissive Sunweaver halo; the six models plus three stage-3 models, per spec 2.7. Rerun `build_skins.py`.
- [ ] **Step 5: Write suite `gilded-art`:**
  - `p2-gilded-lineup`: spawn the six Gilded troops in a row for each team (stage 1); screenshot `gilded-lineup`.
  - `p2-gilded-stages`: spawn knight stages 1, 2 and 3; screenshot `gilded-stages`.
  - `p2-gilded-cards`: open the recruit page Gilded tab; screenshot `gilded-cards`.
- [ ] **Step 6: Run suites `spike,gilded-art`.** The tester confirms that Squire, Archer, Templar, Longbow, Sunweaver and Aldric read as distinct silhouettes, that the team pennants differ, and that stage 3 shows the extra part. Commit `"Phase 2: ETF/EMF troop skin pipeline, team pennants, Gilded Order art"`.

---

### Task 6: Gilded Order mechanics, Champion and Marshal (batch 1)

**Files:**
- Modify: `Abilities.java` (`stance` handled by counters, `pierce`, and `aura` for `oath_banner`).
- Modify: `Houses.java` (Bulwark factor and RoutGuard).
- Modify: `Commander.java` (register `stand_fast` and `rally_horn`).
- `Morale.java` needs no change: guards register through `Morale.GUARDS`.
- Modify: `Phase2Checks.java` (suite `gilded`).

**Interfaces:**
- Consumes: Phase 1 `Morale.GUARDS`, `Morale.RoutGuard`, `Morale.rally(Battle b, Squad s, String group)`, `Squad.steadyUntil` and `BattleEffects.horn(b, at, "rally")`.
- Produces:
  - `Abilities.secondary(Battle b, Unit u, LivingEntity primary, double radius, double share, boolean behind, String kind)`: hits the nearest other enemy within `radius` of the primary; with `behind`, only within the cone continuing the shot direction. Pierce and cleave share it.
  - `Abilities.Banner` record (`team, Vec3d at, long until, double radius`), kept in `Battle.banners`.
  - RoutGuards `Houses.BULWARK_GUARD` (holds an own group whose centre is within the tier radius of a living Gilded troop), `Commander.STAND_FAST_GUARD` and `Abilities.OATH_GUARD`, registered once in `onInitialize`.
- Rules:
  - `steady` applies when the attacker group's effective order is `hold`.
  - `oath_banner` triggers when the group order changes to `hold` and the cooldown is ready. It places a banner block plus particles for 300 ticks, gives own troops within 12 x1.10 (`auraFactor`), registers the guard, and calls `Morale.rally` on routing own groups in its radius.
  - Stand Fast: own troops within 12 of the player cannot rout and take x0.75; the player gets Resistance I for 160 ticks.
  - Rally Horn: `Morale.rally` on every routing own group; every group gets a `move` assignment to the player; `steadyUntil += 400`; a +10% speed attribute modifier for 300 ticks; the rally horn sound plays.

- [ ] **Step 1: Write suite `gilded`:**
  - `p2-gilded-steady`: an archer on `hold` vs on `attack` at the same target shows `Combat.LAST` counters of 1.2 vs 1.0.
  - `p2-gilded-pierce`: a longbow shot at a target with a second enemy 1.5 blocks behind it on the shot line. The second enemy takes 50% of the primary's applied damage; an enemy beside the target (not behind) takes nothing.
  - `p2-gilded-mend`: a Sunweaver heals 5, with an 80-tick cooldown.
  - `p2-gilded-oath`: Aldric's group is ordered to `hold`.
    - A banner appears.
    - Within 12, own troops show `auras == 1.10`.
    - A routing own group in the radius regroups within 20 ticks.
    - No second banner appears within 600 ticks.
  - `p2-bulwark`: the player's banner has 14 Gilded slots.
    - A shield troop takes `traits == 0.55` from an arrow and from blight, and 1.0 from melee.
    - A weakened Gilded group with a Gilded troop within 12 of its centre does not rout.
    - At tier I (radius 5), with the Gilded troop 8 blocks away, the group routs.
  - `p2-counters-gilded`: synthetic rows (husks carrying the `sunder` and `armour_bolt` abilities and the `beast,cavalry` tags) give:
    - C3 0.6, C4 1.0, C14 1.4 and C15 0.7;
    - C5 1.5 on the first hit after a charge order, then C6 1.2;
    - C16 1.15 from behind.
  - `p2-champion-standfast` and `p2-marshal-rallyhorn`: the rules above, with the cooldown tile counting down from 90 s and 120 s.
  - `p2-gilded-showcase`: on map 0, a Gilded shieldwall (`gilded_hard`) under fire from Graveborn archers, with Aldric's banner planted. Screenshot `gilded-showcase` shows the HUD "Bulwark III", the banner with rally particles, and Squire, Templar and Longbow.
  - `tickCost` with `gilded_hard` x4 and `graveborn_hard` x4.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement `pierce` and `secondary`.**
- [ ] **Step 4: Implement the `oath_banner` aura and `Battle.banners`** (cleared in `startRound`).
- [ ] **Step 5: Implement Bulwark** (`traitFactor` and the guard).
- [ ] **Step 6: Register Stand Fast and Rally Horn.**
- [ ] **Step 7: Compile; run suite `gilded` and suites `engine,commander`; all pass.** The tester confirms `p2-gilded-showcase.png`.
- [ ] **Step 8: Balance gate:** `gilded_normal` vs `graveborn_normal`, 40-60%.
- [ ] **Step 9: Commit** `"Phase 2 batch 1: Gilded Order abilities, Bulwark, Champion and Marshal actives"`, with the gate line in the body.

---

### Task 7: Graveborn (batch 2)

**Files:**
- Modify: `GravesAndGlory.java` (`ArmyZombie` and `ArmyStray` registered in `ArmyMobs`; the `ALLOW_DEATH` handler; `Battle.deaths`).
- Modify: `sheets/design.json` (`revenant.entity = zombie`, `stalker.entity = stray`).
- Modify: `Abilities.java` (`cleave`, `onhit_slow`, `dread`), `Morale.java` (`routShareFor`), `Houses.java` (Rise Again), `Commander.java` (`grave_bloom`) and `ArmyCards.java`.
- Art and generated pack files for zombie, skeleton, stray and wither_skeleton (via `build_skins.py`).
- Modify: `Phase2Checks.java` (suite `graveborn`).

**Interfaces:**
- Produces `Morale.routShareFor(Battle b, Squad s, String group) -> double`, used in Phase 1's `shouldRout` call:
  - 0.12 (fearless) when at least 50% of living members carry `fearless`;
  - else 0.40 when the group centre is within 10 of a living enemy `dread` unit;
  - else the sheet's `routShare`;
  - fearless wins over dread.
- Produces `Houses.allowDeath(LivingEntity e, DamageSource src) -> boolean`. A troop can rise if it is Graveborn, not a Legendary, not `temporary` and not already `rose`. It rises when a roll is below the `value` for the squad's Graveborn tier. Rising sets health to `value2 x max`, sets `rose=true` and `stunnedUntil = time + 20`, shows soul-fire particles, and returns false. Harness hook: `Houses.forceRise` (`Boolean`; null means random).
- Produces `record Death(Vec3d at, long time, String troop, int team)` and `Battle.deaths` (pruned to the last 400 ticks, cleared in `startRound`).
- Produces Grave Bloom: up to 6 deaths inside 8 blocks of a raycast point within 30 become `revenant` units in the caster's squad, with `temporary=true`, 60% HP, slot 20 or higher, and `expiresAt = time + 600`.
- Rules:
  - Cleave strikes one other enemy within 2 of the target for 50% (`secondary`, not behind).
  - `onhit_slow` applies Slowness (`statusLevel`, `statusTicks`); if Slowness is already present, the longer duration is kept.
  - Dread gives enemy troops within 10 an `auraFactor` of x0.85.
  - `Tactics` skips attacks while `time < stunnedUntil`.
  - Temporary units are excluded from `reinforce` occupancy, mastery and the report's `lost`. They are discarded at `expiresAt` and in `startRound`, `returnLobby` and `switchMap`.

- [ ] **Step 1: Write suite `graveborn`:**
  - `p2-graveborn-mobs`: `ArmyZombie` and `ArmyStray` have only `SwimGoal` and do not burn at noon; the zombie does not convert in water over 700 ticks.
  - `p2-cleave`, `p2-frostshot` (Slowness I for 40 ticks), `p2-blight-gravecaller` (radius 2.5, windup 20).
  - `p2-fearless-share`: a group of 10 Risen at 15% strength does not rout, and routs at 10%; a knight group at 15% routs.
  - `p2-dread`: enemy knights within 10 of the Hollow King deal `auras == 0.85`. An enemy group at 35% within 10 routs; Risen at 35% beside him do not (fearless wins).
  - `p2-rise-again`: with `forceRise=true`, a Risen killed by a knight stands up at 30% HP, makes no attacks for 20 ticks and shows soul particles. Killed again, it dies. The Hollow King never rises. Screenshot `graveborn-rise`.
  - `p2-grave-bloom`: a warlock player with six troops dead inside 8 within the last 20 s raises six temporary Risen at 60% HP into the squad. After 600 ticks they are gone. Screenshot `graveborn-bloom`.
  - `p2-bloom-cleanup`: raise two temporary Risen and one risen Graveborn, then call `campaign.finish(-1)`. No temporary unit remains, `battle.deaths` is empty, the report's `lost` excludes temporary units, and the `gg_*` teams are empty.
  - `p2-graveborn-lineup` (six troops, both teams; screenshot `graveborn-lineup`), `p2-graveborn-stages` (Risen stages 1 to 3), and `tickCost` with Graveborn banners.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Add the subclasses and switch the sheet entities.** Test both mobs in a 2-minute bot run (`-PbotSim=1 -PsimRoundSeconds=120`, banners `graveborn_easy` on both sides); the log must show no stuck or burning troops.
- [ ] **Step 4: Make the Graveborn art** (pixel-art skill; spec 2.7 rows; soul-cyan emissive eyes) and run `build_skins.py`.
- [ ] **Step 5: Implement cleave, `onhit_slow`, dread and `routShareFor`.**
- [ ] **Step 6: Implement Rise Again via `ServerLivingEntityEvents.ALLOW_DEATH`, and the death log.**
- [ ] **Step 7: Register `grave_bloom`.**
- [ ] **Step 8: Compile; run suite `graveborn` plus `gilded,engine`; all pass.** The tester confirms the rise and bloom screenshots.
- [ ] **Step 9: Balance gate:** `graveborn_normal` vs the current best, 40-60%.
- [ ] **Step 10: Commit** `"Phase 2 batch 2: Graveborn, Rise Again, dread and fearless rout shares, Warlock Grave Bloom"`.

---

### Task 8: Ironclad (batch 3)

**Files:**
- Modify: `GravesAndGlory.java` (`ArmyVindicator`, `ArmyPillager`, `ArmyGolem`; the `Anchor` record; `reinforcementAnchors` and `deployNote` widened; formation spacing per entity width).
- Modify: `sheets/design.json` (`houses` row `ironclad.fieldable = true`).
- Modify: `Abilities.java` (`slam`, the `shell` minimum range, attack animation statuses), `Houses.java` (Entrench), `Commander.java` (`forward_supply`), `Tactics.java` (targeting structures) and `CommandMode.java` (the same spacing rule as the server).
- Art for vindicator, pillager and iron_golem, with the ballista frame and mortar barrel as EMF root parts.
- Modify: `Phase2Checks.java` (suite `ironclad`).

**Interfaces:**
- Consumes: Phase 1 `Battle.reinforcementAnchors(int team) -> List<Capture>`, `deployPoint(Squad, int)` and `deployNote(Squad, Capture)`.
- Produces:
  - `record Anchor(double x, double z, String name, int team)` with `Anchor.of(Capture c)`.
  - `Battle.reinforcementAnchors(int team) -> List<Anchor>`: the Phase 1 points plus standing supply banners with no enemy within 12.
  - `Battle.deployNote(Squad s, Anchor a) -> String`.
  - `ArmySupplyBanner extends ArmorStandEntity implements ArmyMob`: a structure target with 40 HP held in a field and tag `structure`. Its `damage` subtracts the resolved amount, and it is discarded at 0.
  - The `Battle.structures` list.
  - `Battle.spacing(Unit u) -> double` = `max(1.3, entity width + 0.3)`, used by formation offsets and by the `Tactics.route` bridge offsets.
  - `Houses.still(Unit u) -> boolean`: true when the unit moved less than 0.5 over the last 60 ticks, sampled per Tactics pass.
- Rules:
  - An on-hit slam deals its `amount` share to every other enemy within `radius`, with knockback.
  - `shell` cannot target an enemy closer than 10 (the `Abilities.range` minimum); mortars retreat to standoff distance like other ranged units.
  - Golem and ravager attacks send `world.sendEntityStatus(mob, (byte)4)`.
  - Entrench: still Ironclad troops take the `entrench(...)` factor, and siege-tag troops get range x(1+value2).
  - C10 and C11 apply when the target has the tag `structure`.
  - Forward Supply places a banner at the player's feet, and only at least 30 from their own base.

- [ ] **Step 1: Write suite `ironclad`:**
  - `p2-ironclad-mobs`: goals only; no illager raid or celebrate behaviour; the pillager never takes the crossbow-charging pose.
  - `p2-sunder` (C14 1.4 vs a raised Templar), `p2-armour-bolt` (C15 0.7), `p2-ballista-pierce` (a second target within 4 behind takes 60%), `p2-mortar-minrange` (an enemy at 8 is never shelled; one at 20 is), `p2-forge-slam` (three enemies within 2.5 take 60% and are knocked back).
  - `p2-entrench`: a hearthguard still for 60 ticks takes `traits 0.9` (tier I banner), 0.8 on an owned point, and 1.0 while moving. Ballista range goes from 34 to 37.4 at tier I.
  - `p2-counters-siege`: ballista vs supply banner 4.0; knight vs supply banner 0.25; ballista vs Brassjaw 1.3; ballista vs a Moot Hound row (synthetic, small) 0.6.
  - `p2-supply-banner`: a quartermaster places a banner 40 blocks from base; a trained troop deploys there, and `deployNote` names "Forward Supply". Inside 30 blocks of base, placing is refused.
  - `p2-supply-lost`: if the banner is destroyed (or an enemy is placed within 12) after purchase, the troop deploys at the barracks.
  - `p2-ironclad-bridge`: Brassjaw, ordered across the river on map 0, reaches the far bank within 900 ticks via the bridge.
  - `p2-ironclad-showcase`: a ballista and a mortar entrenched on an owned point, with the range ring drawn larger than base (the `CommandMode` ring uses `Abilities.range`); Brassjaw's slam on a line; a supply banner deploying. Screenshot `ironclad-showcase`. Also lineup and stages screenshots, and `tickCost`.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Add the subclasses, registration and attack statuses, and do a 2-minute bot spike run.**
- [ ] **Step 4: Make the Ironclad art and run `build_skins.py`.**
- [ ] **Step 5: Implement `slam`, `shell` and spacing.**
- [ ] **Step 6: Implement Entrench.**
- [ ] **Step 7: Implement `Anchor`, the supply banner and `forward_supply`.**
- [ ] **Step 8: Compile; run suite `ironclad` plus `graveborn,gilded`; all pass.**
- [ ] **Step 9: Balance gate:** `ironclad_normal` vs the current best.
- [ ] **Step 10: Commit** `"Phase 2 batch 3: Ironclad, Entrench, siege counters, Quartermaster Forward Supply"`.

---

### Task 9: Tidecallers (batch 4)

**Files:**
- Modify: `GravesAndGlory.java` (`ArmyDrowned`, `ArmyEvoker`, `ArmyRavager`).
- Modify: `sheets/design.json` (`houses.tidecallers.fieldable = true`).
- Modify: `Abilities.java` (the `mist` zone, harpoon pull, `undertow`, `tentacle_sweep`, `brace`) and `Houses.java` (Riptide).
- Modify: `Tactics.java`: for fording units, `route` and `walk` skip the bridge detour and the river snap, and the water path penalty is 0.
- Modify: `Projectiles.java` (trident visual for `harpoon` shooters) and `Commander.java` (`mark_prey`).
- Art for drowned, evoker and ravager.
- Modify: `Phase2Checks.java` (suite `tidecallers`).

**Interfaces:**
- Produces:
  - `record Mist(int team, Vec3d at, double radius, long until)`, kept in `Battle.mists` (cleared in `startRound`).
  - `Abilities.inMist(Battle b, Vec3d p) -> boolean`, which feeds `Hit.inMist` for the shooter or the target.
  - `Houses.fords(Unit u) -> boolean`: true for Tidecallers when the squad's tier is 2 or higher.
  - `Abilities.visual(Unit u) -> String`: `arrow`, `trident` or `bolt`.
- Rules:
  - Mist: a cloud of radius 4 for 100 ticks. Enemies inside get Slowness I (20 ticks, refreshed), and ranged hits fired from or into the cloud get C17.
  - Harpoon hits pull the target 1 block toward the shooter.
  - Undertow: every 200 ticks, pulls enemies within 7 three blocks toward the Admiral, deals 4, and applies Slowness II for 60.
  - Sweep: deals 70% to other enemies within 3, at most once per 60 ticks.
  - Brace works through counter C18c.
  - Riptide: speed x(1+value) while `isTouchingWater()` or `world.isRaining()`. Riptide hits apply Slowness I for `durationTicks`; when brine also applies, the longer duration wins.
  - Warden Mark Prey: a raycast of 40 picks an enemy troop or commander, which takes x1.30 from the warden's troops and the warden for 240 ticks. The mark uses the vanilla `GLOWING` effect, so both teams see it. A team-only glow would need a client renderer, which `state_v6` does not support; see Spec gaps.

- [ ] **Step 1: Write suite `tidecallers`:**
  - `p2-tidecallers-mobs`: goals only; the drowned never throws tridents on its own; the evoker never summons; the ravager never roars outside its sweep.
  - `p2-mist-zone`: an archer shooting into the cloud shows `counters 0.5`, and enemies inside are slowed.
  - `p2-harpoon-pull`: the target moves at least 0.8 toward the shooter.
  - `p2-undertow` and `p2-sweep`.
  - `p2-brine-longest`: with Riptide tier I (20) vs brine (20), the slow lasts 20; at tier III it lasts 40.
  - `p2-riptide-speed`: at tier I, a drowned in water moves 15% faster than on land over 100 ticks (within 5%).
  - `p2-riptide-ford`: with `tidecallers_hard` (tier III), a raider ordered across the river on map 0 crosses at least 20 blocks from any bridge, while a knight goes via the bridge. Screenshot `tidecallers-ford`.
  - `p2-tidecallers-bridge`: the Brute crosses by bridge at tier 0 within 900 ticks.
  - `p2-warden-mark`: a marked knight takes `commander 1.30` from the warden's troops and 1.0 from another squad.
  - `p2-tidecallers-showcase`: mist over an archer line and the Brute's sweep, screenshotted. Also lineup and stages screenshots, and `tickCost`.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Add the subclasses and registration, and do a 2-minute bot spike run.**
- [ ] **Step 4: Make the Tidecallers art** with `build_skins.py`, keeping the drowned outer layer.
- [ ] **Step 5: Implement mist, harpoon, undertow, sweep, brace and the trident visual.**
- [ ] **Step 6: Implement Riptide and fording.**
- [ ] **Step 7: Implement `mark_prey`.**
- [ ] **Step 8: Compile; run suite `tidecallers` plus `ironclad,gilded`; all pass.**
- [ ] **Step 9: Balance gate:** `tidecallers_normal` vs the current best.
- [ ] **Step 10: Commit** `"Phase 2 batch 4: Tidecallers, mist, Riptide fording, Warden Mark Prey"`.

---

### Task 10: Wildmoot (batch 5)

**Files:**
- Modify: `GravesAndGlory.java`, adding four subclasses:
  - `ArmyBoarkin` (zombified piglin, never angered);
  - `ArmyWolf` (`setSitting(false)` each tick, anger time 0);
  - `ArmySpider` (vanilla climbing navigation kept);
  - `ArmyBear` (no `setWarning` outside trample; a `setWarning` pulse serves as its attack animation).
- Modify: `sheets/design.json` (`houses.wildmoot.fieldable = true`).
- Modify: `Abilities.java` (`pounce`, `howl`, `trample`), `Houses.java` (Pack Hunters with `Battle.targeters`) and `Commander.java` (`bloodrush`).
- Art for zombified_piglin, wolf, spider and polar_bear. The polar bear gets a new JEM with vanilla geometry plus parts.
- Modify: `Phase2Checks.java` (suite `wildmoot`).

**Interfaces:**
- Produces:
  - `Battle.targeters` (`Map<Integer,Integer>`): target entity id to the count of living Wildmoot troops targeting it, rebuilt when `time % 10 == 0` before units tick.
  - `Abilities.noSky(Unit u) -> boolean` (`!world.isSkyVisible(blockPos.up())`), which feeds `Hit.noSky`.
  - Pounce uses `Unit.travelSinceAttack >= 6` and its cooldown, and applies x1.8 through `auraFactor`.
  - Bloodrush: Speed II for 200 ticks and 30% lifesteal on the player's melee hits (in `allowDamage`). Each enemy troop the player kills queues one free `Training` of the cheapest troop the squad lost this round, at most 3 per use.
- Rules:
  - Howl: every 300 ticks, own troops within 10 get +15% damage (`auraFactor`) and +15% speed for 120 ticks.
  - Trample: on first contact while the group order is `charge`, deals 6 to every enemy within 3 with knockback 1.2; cooldown 200.
  - Pack Hunters uses `packHunters(...)`, with `sameTarget = targeters[target]` and the bearing from the target's facing.

- [ ] **Step 1: Write suite `wildmoot`:**
  - `p2-wolf-stands`: moving and fighting for 600 ticks, the wolf is never sitting and never angry at players.
  - `p2-bear-warning`: the warning pose shows only during trample.
  - `p2-spider-wall`: a spider reaches a target behind a 3-high wall by climbing within 200 ticks. Screenshot `wildmoot-wall`.
  - `p2-pounce`: the first hit after 6 or more blocks is x1.8; the next is not.
  - `p2-ambush`: a hunter under a 1-block roof gets 1.25; in the open, 1.0.
  - `p2-ensnare` (Slowness II for 30), `p2-howl` and `p2-trample`.
  - `p2-pack-hunters`: with a tier II banner, three hounds on one archer get `traits 1.2`; two hounds from the rear get 1.2; two in front get 1.0. Screenshot `wildmoot-flank`, with the harness damage-number overlay (text displays from `Combat.LAST`, spawned only by `Phase2Checks`).
  - `p2-reaver-bloodrush`: speed applies, lifesteal heals the player, and refunds are queued, capped at 3.
  - `p2-wildmoot-showcase`: Moot Hounds hitting an archer line from the flank, a Web Stalker on a wall, and Old Horn trampling, screenshotted. Also lineup and stages screenshots, and `tickCost`.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Add the subclasses and registration, and give each mob a 2-minute bot run before any art.** The log must show no sitting, rearing or stuck troops.
- [ ] **Step 4: Make the Wildmoot art** (quadruped JEMs; the polar bear has no Fresh Animations root).
- [ ] **Step 5: Implement pounce, ambush, ensnare, howl and trample.**
- [ ] **Step 6: Implement Pack Hunters and `targeters`.**
- [ ] **Step 7: Implement `bloodrush`.**
- [ ] **Step 8: Compile; run suite `wildmoot` plus `tidecallers,graveborn`; all pass.**
- [ ] **Step 9: Balance gate:** `wildmoot_normal` vs the current best.
- [ ] **Step 10: Commit** `"Phase 2 batch 5: Wildmoot, Pack Hunters, Reaver Bloodrush"`.

---

### Task 11: The chase, server side (batch 6a)

**Files:**
- Modify: `src/main/java/gg/gravesandglory/Progression.java` (Founders re-author, House-aware claims).
- Modify: `Campaign.java` (commands, `finish` rewards and ledger, writs).
- Modify: `MatchStats.java` (report rows) and `GravesAndGlory.java` (kill credit per troop type).
- Modify: `test-tools/standalone/HousesCheck.java` and `Phase2Checks.java` (suite `chase`).
- Modify: `docs/PARKED.md`: campaign bonus-objective shards wait for bonus objectives to exist.

**Interfaces:**
- Produces (pure) `Progression.record Grant(int gold, int xp, int shards, String house, String troop, String cosmetic, String message)`.
- Produces (pure) `Progression.claim(State s, String key)`. It accepts `daily:<slot>:<house>`, `weekly:<house>` and `founders:<tier>[:<house or troop>]`, and stores the key without the suffix.
- Founders tier table:

  | Tiers | Reward |
  |---|---|
  | 1, 3, 7, 9, 11, 13, 17, 19, 23, 27 | 30 gold |
  | 2, 4, 6, 8, 12, 14, 16, 18, 22, 24, 26, 28 | 10 shards to the named House |
  | 5, 10, 15, 25, 30 | titles Scout, Pathfinder, Captain, Veteran, Bannerlord |
  | 20 | the named Rare |
  | 21 | cosmetic `pennant_pattern` |
  | 29 | cosmetic `banner_frame` |

- Contract rewards: the daily claim that completes all three gives +3 shards to its House. The weekly gives 150 gold, 300 XP and +10 shards.
- Produces (server commands), each refunding on a failed save like `roll`:
  - `buy:<troop>`: a Common or Rare for `chosenCost`, as a new troop or a duplicate copy.
  - `evolve:<troop>`: applies the next `evolutions` row, paying shards of the troop's House and gold, and checking `requiresMastery`.
  - `star:<troop>`: an Epic or Legendary star paid in shards.
  - `legendary:<house>:writ` and `legendary:<house>:shards` (the latter needs renown >= 30 and 50 shards).
  - `progress:<claim key>`.
- `finish` additions, in the same save as gold:
  - shards and renown from `RosterMath.matchRewards(Houses.bannerTiers...)`;
  - per troop type: `kills`, credited to the killing unit's type via `lastHitBy` and the shooter's spec; and `survived`, +1 per type with a living non-temporary unit at the end;
  - `mastery` from `masteryLevel`, granting the titles `"<Troop> Veteran"` and `"<Troop> Master"`, and the House title once all six troops reach Mastery I;
  - a Writ when every `campaignBattles` row of a chapter is cleared and `writChapters` lacks that chapter.
- The report JSON gains `"houses": [{"house","tier","traitSeconds","shards","renown"}]`, and the `CampScreen` results page lists them.

- [ ] **Step 1: Add failing `HousesCheck` assertions for `Progression`:**
  - `founders:1` gives 30 gold; `founders:2:gilded` gives 10 shards to gilded; `founders:2` without a House gives 0 and "Choose a House".
  - `founders:20:longbow` grants longbow; `founders:20:mage` (Epic) is refused.
  - `founders:21` gives the cosmetic `pennant_pattern`; `founders:30` gives the title Bannerlord.
  - Total gold over all 30 tiers is 300.
  - A state holding the old claim `founders:2` returns "Already claimed".
  - The third daily claim with `:ironclad` gives 3 shards; the first two give 0.
- [ ] **Step 2: Run; fail.**
- [ ] **Step 3: Implement the `Progression` changes.**
- [ ] **Step 4: Run; pass.**
- [ ] **Step 5: Write suite `chase`:**
  - `p2-buy-chosen`: 300 gold buys sentinel; a second buy adds a copy and star 2.
  - `p2-evolve`: knight stage 2 costs 10 gilded shards and 100 gold. Stage 3 is refused without Mastery I and allowed after setting 40 kills and 8 survived. A newly spawned knight's custom name is then `Knight-Captain`.
  - `p2-cap`: a star 3, stage 3 knight spawns with max HP 32 x 1.12.
  - `p2-star-shards`: an Epic reaches star 2 for 30 shards.
  - `p2-legendary-writ` and `p2-legendary-renown`.
  - `p2-match-rewards`: finish a won match with banner Gilded 14 / Graveborn 6. Expect shards +2/+1 and renown +2/+2; kills are recorded by type; temporary units are excluded.
  - `p2-report-houses`: the report rows are present (screenshot `chase-report`).
  - `p2-founders`: claims work through `progress:` commands.
- [ ] **Step 6: Run; expect failures.**
- [ ] **Step 7: Implement the commands, rewards, ledger, writs and report rows.**
- [ ] **Step 8: Compile; run suite `chase` and the standalone checks; all pass.**
- [ ] **Step 9: Commit** `"Phase 2: chosen buys, evolutions, stars, shards, renown, mastery and the re-authored Founders chapter"`.

---

### Task 12: Progression and recruitment screens, economy model (batch 6b)

**Files:**
- Modify: `CampScreen.java`:
  - the `recruit` page gains the roll result card, chosen buy and Legendary unlock;
  - a new `troop` page shows a card with stars, stage, mastery bars, and an evolve button with confirmation;
  - the `contracts` and `chapter` pages gain a House selector for claims.
- Modify: `ArmyCards.java` (card counter lines and crossed-swords glyphs) and `CampaignClient.java`.
- Modify: `sheets/design.json`:
  - new sheet `troopNotes` (`id, beats, beatenBy`): 30 rows holding the spec section 2 "Beats" and "Beaten by" cells;
  - new NOT_SHIPPED sheet `economyModel`, one row: `matchesPerHour 4.0, winRate 0.5, hoursPerPlayDay 1.5, playDaysPerWeek 4, matchGold 80, winGold 40, dailyGold 100, weeklyGold 150, chapterGold 300, chapterRareValue 300, runs 20000`;
  - add `economyModel` to `NOT_SHIPPED` in `tools/preflight.py`.
- Create: `tools/economy_model.py`: a Monte Carlo of spec 4.9 driven by the sheet; it prints and asserts.
- Create: `art/troops/<troop>_veteran.png` (30, pixel-art skill, brass-trimmed variants), regenerated by `build_skins.py`.
- Modify: `Phase2Checks.java` (suite `screens`).

**Interfaces:**
- Consumes: the Task 11 commands; `CounterMath.applied` for the card glyphs (counter rows whose attacker or target tag matches the troop).
- Produces: client `CampaignClient.lastRoll` (`{troop, duplicate, starsBefore, starsAfter, shards}`), from the snapshot after a roll.
- Produces: a 1.2 s reveal animation in which pips fill from `starsBefore` to `starsAfter`, skippable with the existing "Skip reveal".
- `tools/economy_model.py` asserts (seeded):
  - per-House gold mean 2,920, within 3%;
  - five-House mean 14,700, P10 11,000, median 14,800 and P90 18,200, each within 3%.

  It also prints the hours table of spec 4.9.

- [ ] **Step 1: Write suite `screens`:**
  - `p2-roll-card`: a forced duplicate roll of an owned Common at star 1 shows a DUPLICATE card with star 1 going to 2. Screenshot `chase-roll-dup`, mid-animation.
  - `p2-evolve-screen`: the confirm dialog from stage 2 to 3 shows both stage names, the costs, and the stage 3 portrait with its extra part. Screenshot `chase-evolve`.
  - `p2-troop-card`: the Templar card shows the beats and beaten-by lines, glyphs for C1 and C3, and the mastery bars. Screenshot `chase-troop-card`.
  - `p2-claim-house`: a claim on the contracts page sends `progress:daily:2:<selected house>`.
  - `p2-veteran-skin`: a Mastery II knight spawns as `Order Squire (Veteran)`. Screenshot `chase-veteran`.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement the pages and card changes.**
- [ ] **Step 4: Make the veteran skins and run `build_skins.py`.**
- [ ] **Step 5: Add the `economyModel` sheet and `tools/economy_model.py`, then run it.** Expect all asserts to pass and the hours table to print.
- [ ] **Step 6: Compile; run suites `screens` and `chase`; all pass.** The tester confirms the four screenshots. Commit `"Phase 2: recruitment, evolve and troop card screens, veteran skins, economy model"`.

---

### Task 13: Migration in game, regression and Kort's playtest gate

**Files:**
- Create: `docs/PHASE-2-PLAYTEST.md`.
- Create: `test-tools/resources/fixtures/profile-v1-{fresh,full,invalid}.json` and `profile-v1-cache.json` (a client cache copy).
- Modify: `docs/PROJECT-STATE.md`, `docs/PHASE-2-BALANCE.md`, `gradle.properties` (`mod_version=0.5.0`) and `docs/RELEASE-0.5.0.txt`.
- Modify: `HOW-TO-EDIT.md` sections 3 to 5: the new classes, sheets and skin pipeline, and the rule that no class number is hard-coded.
- Modify: `tools/preflight.py`: the composition rule becomes "exactly" for every House, now that all are fieldable.
- Modify: `Phase2Checks.java` (suite `migration`).

**Interfaces:**
- Consumes: every suite and check id above.

- [ ] **Step 1: Write suite `migration`:**
  - `p2-migrate-fixtures`: each fixture, loaded through `profile()`, matches its `ProfileMigrationCheck` expectations, and `.v1.json` is present.
  - `p2-migrate-restart`: on a second launch with the same root, the files are byte-unchanged and the profile is equal.
  - `p2-migrate-import`: on a first LAN visit with `profile-v1-cache.json`, the cache is imported and migrated. A second import with higher gold is ignored.
  - `p2-full-roster`: a test profile owning all 30 troops at star 1 shows every House tab complete. Screenshot `migration-full-roster`.
- [ ] **Step 2: Run suite `migration` twice on one root** (the restart case); all pass.
- [ ] **Step 3: Full regression, in this order:**
  - one launch with `GRAVES_PHASE2_CHECKS=banners,engine,commander,spike,gilded-art,gilded,graveborn,ironclad,tidecallers,wildmoot,chase,screens,migration`;
  - then Phase 1 `combat,morale,reinforce,bots,tutorial,report,ui`;
  - then `-PcampaignChecks` without either variable.

  Expected: every id passes.
- [ ] **Step 4: Mixed vs pure sims:** `mix_gilded_graveborn_normal` vs `gilded_normal`, and `mix_ironclad_tidecallers_normal` vs `tidecallers_normal`, 20 matches each, run as for the gate. There is no gate: log both, and flag any result outside 40-60% in the playtest doc.
- [ ] **Step 5: Standalone checks and packaging.** Run the three standalone checks, `test_ui_standard.py` and the `HOW-TO-EDIT.md` section 9 checks. Then run `"$G" build --offline -x test` and `python tools/package.py`; the leak check must find no `Phase2Checks`, `HousesCheck`, `ProfileMigrationCheck` or `BotSimulation` in the jar. Output stays in local `dist/` only.
- [ ] **Step 6: Write `docs/PHASE-2-PLAYTEST.md`** (one page). Setup: the 0.5.0 build, with his existing camp so he sees the migration.
  - Session 1: his migrated collection on the recruit page, and a few House rolls.
  - Session 2: a pure Gilded skirmish on Normal (shieldwall, Bulwark tile, Aldric's banner).
  - Session 3: a mixed banner of his choice vs Hard bots of a new House.
  - Session 4: one evolve, and the report's House rows.

  Each item gets a yes/no box. Free-text questions: "Did it feel good?", "Which House read worst on the field?", "Is the chase pace right?". Appendix: every gate line and the economy model table.
- [ ] **Step 7: Update `PROJECT-STATE.md` and `HOW-TO-EDIT.md`, then commit** `"Phase 2: migration verified in game, regression, 0.5.0, playtest gate"`. Do not merge or push; hand the branch and `docs/PHASE-2-PLAYTEST.md` to the orchestrator.

---

## Spec gaps resolved

1. **Commander classes are split.** The framework, key, tile and all six passives come in Task 4, before Gilded. Each active lands with the House batch the spec names: Champion and Marshal in Task 6, Warlock in 7, Quartermaster in 8, Warden in 9, Reaver in 10. This is needed because the batch 2 and 3 screenshots require Grave Bloom and Forward Supply. Between Task 2 and Task 4 there are no class bonuses.
2. **The commander key is H, not G.** G is already the Attack order key (`key("attack", GLFW_KEY_G)`). See Needs Kort 7.
3. **Houses are staged.** A new `houses.fieldable` column keeps Wildmoot, Ironclad and Tidecallers out of banners and bot banners until their batch. Their rows, abilities, evolutions and portraits exist from Task 2, so the batch 0 screen can show their Commons. The preflight composition rule is "at most" until Task 13 makes it "exactly".
4. **Extra sheet columns were needed**, because the spec's columns could not hold the spec's own numbers (for example the Rise Again 30% HP, the dread 40% rout share and the Champion's 10-block aura). Added: `houseTraits.value2`; `abilities.trigger` and `amount2`; `classes.value3`, `passiveValue2` and `passiveRadius`; `factions.glyph`; `combatDiscipline.multiplierMin/Max`; and a new sheet `troopNotes` for card text.
5. **`trainingSeconds` becomes a double**, because the spec's 70, 90 and 130-tick training times give 3.5, 4.5 and 6.5 seconds.
6. **The Ballista evolve line changed.** The spec gives +3/+3 then +2/+4, a cumulative +7% damage, which is over the +6% evolution budget and fails the cap check. It now uses the striker pattern (+2/+4 then +1/+2), with the names kept.
7. **Counter semantics:**
   - `replaces`: a matched row and the matched rows it lists contribute only their largest multiplier. This covers C5/C6/C7 "highest applies", C14 and C15.
   - "Frontal" means a bearing of 90 degrees or less from the target's facing, like the vanilla shield; flank means more than 110.
   - Dynamic tags (`melee`, `arrow`, `shot`, `magic`, `ranged`, `charging`, `holding`, `structure`, `nonsiege`, `any`) and `a+b` targets keep the condition list closed.
   - Stance multipliers live only in `counters`.
8. **Trait tiers in battle count living troops' slots.** They are recomputed on spawn and death, so uptime means something. Rewards (shards, renown) use the banner's tiers. "Large" follows the spec literally: `ic_mortar` and `tc_brute` plus the five Legendaries.
9. **Fearless beats dread** when both apply, since the Graveborn "do not run". Both change only the base rout share, not Phase 1's flanked share.
10. **Sides:** positions stay by team index (rows 0 and 1), and colours come from `Battle.side(team)`. The glyphs are sun, moon, star and wave. The gold accent int uses the hex `0xDFCE68` = 14667368; the spec's 14667624 is actually `0xDFCF68`.
11. **Bot banners** follow a fixed pattern per difficulty: easy is Commons and Rares; normal adds an Epic; hard adds a Legendary. Bots have no stars (multiplier 1.0). Barracks training is limited to the banner's composition.
12. **Migration details:**
    - A v1 line is valid when it has 20 entries of known ids for its side and at least one troop.
    - When neither line is valid, the banner is `starterBanner()` (knight x12, archer x8), which is also the new-profile banner.
    - Phase 1's `campaignCleared` is copied.
    - Founders claims stay untouched, so a player who claimed tier 20 before migration keeps the Vanguard title and does not get the tier-20 Rare.
13. **Profile I/O stays pure.** `ProfileMigration` takes parse and write functions, so the standalone check needs no Gson.
14. **Skin numbering is generated** by `tools/build_skins.py` from the `art/` sources, so ETF and EMF indices never collide by hand. Names match by `regex:` over the stage names, with an optional ` (Veteran)` suffix.
15. **Mastery levels need both thresholds** (kills and matches survived). Claims that grant shards carry the House in the key, and the House selector is on the contracts and chapter pages.
16. **Deferred:**
    - the +5 shard campaign bonus objective, since no bonus objectives exist yet (parked);
    - in-world rendering of renown and banner cosmetics: unlocks are recorded and listed, and rendering is Phase 3 art;
    - a team-only glow for Mark Prey. The plan uses the vanilla Glowing effect, which both teams can see; a team-only glow needs a client renderer (parked).
17. **Writ timing:** the Writ is granted when every `campaignBattles` row of chapter 1 is cleared. In Phase 2 chapter 1 has only battle 1, which matches the spec's hour 0 to 2 model.

## Needs Kort

From spec section 8.3:
1. ANSWERED 2026-10-17: banner slots at 6 / 10 / 14.
2. ANSWERED 2026-10-17: refund 300 gold each (Sunweaver, Gravecaller, and Oracle/Wraith if unlocked); they are earned again through rolls. The migration fixtures above reflect this.
3. ANSWERED 2026-10-17: keep climbing.
4. ANSWERED 2026-10-17: names approved as listed.
5. ANSWERED 2026-10-17: boar-kin approved.
6. ANSWERED 2026-10-17: 29-hour median accepted; Epic weight stays 2.
7. Commander active on G: is that key free in your controls, or should it be another one? (Planning found that G is the Attack order key; the plan uses H.)

Raised while planning:

8. ANSWERED 2026-10-17: no retro grant.
9. ANSWERED 2026-10-17: vanilla glow; team-only glow stays parked.
