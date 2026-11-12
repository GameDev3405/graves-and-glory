# Match Ending Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the instant teleport at match end with a server-timed sequence: freeze plus VICTORY/DEFEAT/DRAW, a top-three podium on a shared stage, a stats screen, a rewards count-up, then back to the room.

**Architecture:** A pure `MatchEnding` class holds the rules (timings, match score, MVP, podium pick, per-player step with skip). `Campaign.finish` keeps paying and saving exactly as today, then starts the ending instead of calling `returnLobby`; the battle tick is gated off while it runs. The server publishes each player's step in the camp snapshot and the enriched report in `result`; the client draws the banner, the podium (client-side figures on a server-built stage) and two screens that are also reused by the War Table report.

**Tech Stack:** Java 17, Fabric 1.20.1 (Yarn 1.20.1+build.10), GeckoLib troops, Gson snapshots, `sheets/design.json` → `Spec.java` via `tools/preflight.py`, standalone `*Check.java` (no Minecraft types) and real-client harness suites run through `tools/checkq.py`.

**Spec:** `docs/superpowers/specs/2026-11-12-match-ending-design.md` (approved by Kort 2026-11-12). Mockups: `docs/superpowers/specs/match-ending-mockups.html`.

## Global Constraints

- Timings: result 6 s, podium 8 s, stats 10 s, rewards 6 s (+0.4 s per reward row past six, rewards step capped at 10 s). Skip keys: `{key:menu}` or Escape; hint text exactly "{key:menu} or Escape to skip".
- Score: `10*kills + 30*commanderDowns + 1*objectiveSeconds + round(100*kept/(kept+lost))` (0 bonus when kept+lost is 0). `kills` here is the army's enemy kills (today's `SquadStats.kills`). MVP ties: more kills, more objective seconds, winning side, earlier seat.
- All timings and weights come from a new one-row sheet `ending` in `sheets/design.json` (`resultSeconds` 6, `podiumSeconds` 8, `statsSeconds` 10, `rewardsSeconds` 6, `rewardsMaxSeconds` 10, `rewardRowTenths` 4, `killWeight` 10, `commanderWeight` 30, `objectiveWeight` 1, `survivalMax` 100). Never hand-edit `Spec.java`; run `python tools/preflight.py` / `./gradlew compileJava`.
- Reward amounts and the order of paying/saving in `Campaign.finish` do not change. Rewards are saved once, at step 1.
- Outcome words: `VICTORY`, `DEFEAT`, `DRAW` (the report's loss outcome changes from "BATTLE COMPLETE" to "DEFEAT").
- Production default runs the sequence. The harness default is instant (old behaviour) through system property `gravesandglory.instantEnding` (absent = false); `test-tools/isolated-checks.gradle` sets it from env `GRAVES_INSTANT_ENDING`, default `'true'`, so every existing suite is unchanged.
- No save schema change. No dependency change. No edits to the battle HUD or troop pages (UI lead). Visual direction: parchment, dark wood, brass frame; no brass on table wood.
- Before editing `Campaign.finish`, `WarTableReport` or `MatchRules`, message the army lead (local_63f4c13a-5f5b-4587-af60-b1d088854b6d); keep edits there minimal.
- Never write `eula.txt`; never kill a game client; at most five clients machine-wide; game runs only through `tools/checkq.py`.
- One writer per checkout: workers run one at a time in `C:\Users\kortt\Melty\bb-ending`.

## Review Focus

1. A timeout or a second win condition firing during the ending (the round clock passes `roundSeconds` while `running` is still true) must not call `finish` again or pay twice. Pinned in Task 3 (`ending-no-double-finish`).
2. A player who disconnects mid-ending, or joins mid-ending, must end in camp with their reward and the report, not stranded at the stage. Pinned in Task 3 (`ending-rejoin`).
3. A commander dead (spectator, respawn pending) at the end must get the full sequence and must not be respawned into the frozen field. Pinned in Task 3 (`ending-respawn-pending`).
4. The server stopping during the ending (host quits) must not lose the reward or leave units in the world: `voidMatch` must not re-pay, teardown must run. Pinned in Task 3 (`ending-stop`).
5. A 4v4 report with the new fields must still fit the camp snapshot (`{}` fallback would blank the client). Pinned in Task 3 (`ending-snapshot-size`).

---

### Task 1: Pure rules (`MatchEnding`) and the `ending` sheet

**Files:**
- Create: `src/main/java/gg/gravesandglory/MatchEnding.java`
- Create: `test-tools/standalone/MatchEndingCheck.java`
- Modify: `sheets/design.json` (new `ending` sheet), `tools/preflight.py` (validation line beside the `campaign` checks ~459), `tools/verify.py:81` (add `MatchEnding` to `names`)

**Interfaces:**
- Produces (no Minecraft types; all static):
  - `enum Step { RESULT, PODIUM, STATS, REWARDS, DONE }`
  - `record Timing(int resultTicks, int podiumTicks, int statsTicks, int rewardsTicks)`; `static Timing timing(Spec.EndingRow row, int rewardRows)` (rewards = min(max, base + tenths*(rows-6)/10 s) when rows > 6).
  - `static Step step(long elapsedTicks, Timing t)`; `static long ticksLeft(long elapsedTicks, Timing t)` (in the current step; 0 at DONE).
  - `static Step playerStep(Step clock, Step skippedTo)` = the later of the two.
  - `static Step next(Step s)` (DONE stays DONE).
  - `record Entry(String id, int team, boolean bot, int seat, int kills, int commanderDowns, int objectiveSeconds, int kept, int lost)`
  - `static int score(Entry e, Spec.EndingRow w)`
  - `static List<Entry> ranked(List<Entry> entries, int winner, Spec.EndingRow w)` (score desc, then the MVP tie-break; winner -1 = draw skips that rule).
  - `static int mvpIndex(List<Entry> entries, int winner, Spec.EndingRow w)` (index into `entries`, -1 for an empty list; unlike `MatchStats.mvp`, a 0-score match still crowns the first ranked).
  - `static List<Entry> podium(List<Entry> ranked)` (first min(3, size)).
  - `static List<String> squadTroops(Map<String,Integer> troopKills, List<String> bannerOrder, int max)` (troop ids by kills desc, ties banner order, then fill from banner order without repeats, up to max=3).
  - `enum Headline { KILLS, OBJECTIVE, KEPT }`; `static Headline headline(Entry e, Spec.EndingRow w)` (the term contributing most to the score; ties in enum order). `static String headlineText(Entry e, Headline h)` → "38 kills", "4:12 on points", "17 of 20 kept".
  - `static String kd(int kills, int deaths)` → "6 / 1" style; `static String ratio(int kills, int deaths)` → "6.0", deaths 0 → kills as "6.0".

- [ ] **Step 1: Add the sheet.** In `sheets/design.json` add `"ending": [{"id":"default","resultSeconds":6,"podiumSeconds":8,"statsSeconds":10,"rewardsSeconds":6,"rewardsMaxSeconds":10,"rewardRowTenths":4,"killWeight":10,"commanderWeight":30,"objectiveWeight":1,"survivalMax":100}]`. In `tools/preflight.py` add: every value a whole number ≥ 0, `rewardsMaxSeconds >= rewardsSeconds`. Run `python tools/preflight.py`; expected: exit 0 and `Spec.java` gains `EndingRow` and `ENDING`.
- [ ] **Step 2: Write `MatchEndingCheck`** in the style of `MatchRulesCheck` (static `check`, `main`, prints the count). Assertions, using `Spec.ENDING[0]` as `w`:
  - `score(entry(kills=38, downs=1, obj=185, kept=17, lost=9))` = 380+30+185+65 = 660; `score` with kept=0, lost=0 has 0 survival bonus; kept=5, lost=0 gives +100.
  - Ranking: equal score → more kills first; equal score and kills → more objective first; then the winner's side first; then lower seat. With winner -1 the side rule is skipped (seat decides).
  - `podium` of 0, 1, 2, 3, 8 entries has 0, 1, 2, 3, 3 rows; bots rank like humans.
  - `squadTroops({archer:5, knight:5, pike:1}, [knight, archer, pike, squire], 3)` = [knight, archer, pike]; `squadTroops({}, [knight, archer], 3)` = [knight, archer]; empty banner → [].
  - `timing(w, 6).rewardsTicks()` = 120; `timing(w, 9)` = 120 + 24 = 144 ticks; `timing(w, 40)` capped at 200.
  - `step(0)` RESULT; `step(119)` RESULT; `step(120)` PODIUM; `step(279)` PODIUM; `step(280)` STATS; `step(480)` REWARDS; `step(600)` DONE (6 rows). `ticksLeft(121)` = 159.
  - `playerStep(RESULT, STATS)` = STATS; `playerStep(REWARDS, PODIUM)` = REWARDS; `next(REWARDS)` = DONE; `next(DONE)` = DONE.
  - `kd(6,1)` "6 / 1"; `ratio(6,0)` "6.0"; `ratio(7,2)` "3.5"; `headlineText` for 252 objective seconds = "4:12 on points".
- [ ] **Step 3: Run it to see it fail.** Run `python tools/verify.py fast --offline` (or the javac line from verify.py with `MatchEnding` added). Expected: compile failure, `MatchEnding` missing.
- [ ] **Step 4: Implement `MatchEnding`** to the interfaces above.
- [ ] **Step 5: Run `python tools/verify.py fast --offline`.** Expected: `MatchEndingCheck` passes, everything else as on main.
- [ ] **Step 6: Commit** `feat(ending): match ending rules and the ending sheet`.

### Task 2: New match statistics

**Files:**
- Modify: `src/main/java/gg/gravesandglory/MatchStats.java` (`SquadStats` fields), `GravesAndGlory.java` (kill credit ~177-178, `allowCommanderDeath` ~1320-1330, damage hook ~1290-1310, `captureTick` ~1270-1285, training `addLast` ~1082/1098, `reinforce` ~1196, `objectiveSeconds` ~1281), `Campaign.java` (bot buys ~750, `progressEvent` 1145), `Veterans.java` (`buy` success), `OrderRules.java` (~141), `BattleMode.java` (+ `SiegeMode`, `ConvoyMode`, `SurvivalMode` overrides)
- Create: `test-tools/standalone/MatchStatsSourceCheck.java` (source checks, as `MatchRulesCheck` reads source)

**Interfaces:**
- Produces on `MatchStats.SquadStats` (all `public int` unless said): `commanderKills` (enemy troops killed by the commander's own blow or shot: attacker is the squad's `ServerPlayerEntity`), `commanderDowns` (enemy commanders downed and credited to this army: the `side` squad in `allowCommanderDeath`), `deaths` (this commander downed), `damageDealt` (rounded sum of damage the commander's own blows and shots dealt to enemy troops and commanders, after mitigation), `captures` (points this army took: credited to every seated squad of the capturing team with a troop or commander on the point at the capture), `goldSpent`, `trained`, `upgrades`, `orders`, `kept` (set at step 1: this army's living non-temporary troops), `public final Map<String,Integer> fielded` (per troop id: every spawn or refill of that type this round).
- Produces on `BattleMode`: `public boolean onObjective(GravesAndGlory.Battle b, GravesAndGlory.Squad s, net.minecraft.util.math.Vec3d pos)`; default = within any capture point's radius when `capturesPoints(b)` (today's rule). Siege: within the keep point or the active breach stage's site. Convoy: within the wagon's escort/block radius. Survival: within the shrine radius. The once-a-second objective count (today at 1281 for points only) calls this for every seated squad's troops and commander, one second per squad per second.

- [ ] **Step 1: Write `MatchStatsSourceCheck`**: reflection on `MatchStats.SquadStats` for each field name above; source checks that `GravesAndGlory.java` increments `commanderKills`, `commanderDowns`, `deaths`, `damageDealt`, `captures`, `trained`, `goldSpent`, `fielded`; `Veterans.java` increments `upgrades`; `OrderRules.java` increments `orders`; `SiegeMode`, `ConvoyMode`, `SurvivalMode` override `onObjective`. Add the class name to nothing (it reads source); add `MatchStats` already compiled.
- [ ] **Step 2: Run fast; expected FAIL** on the first missing field.
- [ ] **Step 3: Add the fields and hooks.** Gold spent counts the price paid by humans and bots (training, Armoury gear, refills that cost gold); refunds subtract. `trained` counts each troop that finishes training (not queued). `kept` is filled by Task 3.
- [ ] **Step 4: Run `python tools/verify.py fast --offline`.** Expected: all pass.
- [ ] **Step 5: Commit** `feat(ending): track commander, economy, capture and objective stats per army`.

### Task 3: Server ending flow, report and protocol

**Files:**
- Modify: `Campaign.java` (`finish`, new `beginEnding`, `endingTick`, `endEnding`, `voidMatch`, `joined`, `handle` for `ending:skip`, `reportSquads`, report fields, snapshot `ending`), `GravesAndGlory.java` (tick gate at 838-841, round-timeout at ~868, respawn loop ~826, damage hook, `allowCommanderDeath`), `OrderRules.java`, `Commander.java`, `Veterans.java` (refuse while ending), `test-tools/isolated-checks.gradle` (instant property)
- Create: `test-tools/java/gg/gravesandglory/EndingChecks.java` (harness suite `ending`, wired where `Phase4Checks` dispatches suites)

**Interfaces:**
- Consumes: Task 1 (`MatchEnding.*`, `Spec.ENDING[0]`), Task 2 (stats), Task 4 (`PodiumStage.viewSpot()` returns `{x,y,z,yaw,pitch}`; until Task 4 lands, a stub returning the camp spawn).
- Produces (server):
  - `Campaign` fields: `long endingStart = -1` (−1 = no ending), `int endingWinner`, `MatchEnding.Timing endingTiming`, `Map<UUID, MatchEnding.Step> skipped`, `boolean ending()` = `endingStart >= 0`.
  - `finish(int winner)`: returns at once when `ending()`; otherwise pays exactly as today, then `beginEnding(winner)` unless `instantEnding()`, in which case `returnLobby()` as today.
  - `static boolean instantEnding()` = `Boolean.getBoolean("gravesandglory.instantEnding")`.
  - `beginEnding`: sets kept on every seated squad, `setAiDisabled(true)` and zero velocity on every unit mob, records start, plays `Audio.ending` as today. `b.running` stays true.
  - `endingTick()` (from `Campaign.tick` while `ending()`): per participant computes `playerStep(step(elapsed), skipped)`; on entering PODIUM teleports that player to `PodiumStage.viewSpot()` in SPECTATOR; on DONE `toLobby(p)` for that player; when every online participant is DONE, or the clock is DONE, `endEnding()` = `endingStart=-1` then `returnLobby()`.
  - `voidMatch` during the ending: no partial pay (already settled), just `endEnding()`.
  - `joined` during the ending: player goes to camp (`toLobby`), no step.
  - Command `ending:skip` from a participant: `skipped.put(id, next(current step))`.
- Produces (wire, consumed by Tasks 5 and 6):
  - Camp snapshot field `ending` (only for a participant whose step is not DONE): `{"step":"RESULT|PODIUM|STATS|REWARDS","left":<ticks left in step>,"total":<ticks of step>,"outcome":"VICTORY|DEFEAT|DRAW","line":"<final score line>","stage":[x,y,z]}` (`stage` = the podium plinth row centre from `PodiumStage.centre()`).
  - Report (`results` JSON) additions: `outcome` uses `DEFEAT` for a loss; `line` (same score line); each `squads` row gains `uuid`, `house`, `score`, `ck` (commanderKills), `cd` (deaths), `downs`, `kept`, `mvp` (bool), `rank` (1-based); `podium`: up to three `{rank,name,uuid,bot,team,house,score,headline,gear:[item ids HEAD,CHEST,LEGS,FEET,MAINHAND,OFFHAND, "" for none],troops:[≤3 troop ids]}`; `you`: `{"commander":{"kills","deaths","damage"},"army":{"kills","lost","kept","best","troops":[{"id","fielded","kills","lost"}]},"objectives":{"captures","holdSeconds"},"economy":{"earned","spent","trained","upgrades","orders"}}`; `rewards`: ordered `[{"label","amount","reason"}]` built from what was actually saved (rows with amount 0 omitted; gold, Forge marks, XP, each House's shards and Renown, March bonus, Pacts, titles); `claimable`: `{"contracts":n,"tiers":n}` from `Progression.claimable`.
  - Score line: Domination/points modes "Dawn 300 / Dusk 214"; other modes the mode's first ledger entry in its format; always followed by " · <map> · <m:ss>".
  - Harness gradle: `systemProperty 'gravesandglory.instantEnding', System.getenv('GRAVES_INSTANT_ENDING') ?: 'true'`.

- [ ] **Step 1: Message the army lead** that `finish` gets a split (pay block untouched) and a `rewards` list in the report; ask for any conflict. Proceed unless they object.
- [ ] **Step 2: Write `EndingChecks`** (suite `ending`, run with `GRAVES_INSTANT_ENDING=false`), each a reported check, using the `b.roundStarted=b.time-181*20; joinedAt` fixture the other suites use, starting practice via `camp:practice`:
  - `ending-steps`: `finish(team)` → `ending()` true, snapshot `ending.step` RESULT, outcome VICTORY; after 120 ticks PODIUM and player within 3 blocks of `viewSpot`; after 280 STATS; after 480 REWARDS; after 600 (+row extension) player in camp, `b.running` false, units empty.
  - `ending-freeze`: every unit's position after 40 ticks of RESULT within 0.05 of its position at `finish`; no unit health changes; the player takes no damage from a forced hit; an order command is refused.
  - `ending-paid-once`: profile gold/XP after the full sequence equals the value right after `finish`.
  - `ending-no-double-finish`: force `b.roundStarted` so the clock is out during RESULT; `results` id and gold unchanged after 40 ticks.
  - `ending-skip`: send `ending:skip` four times from the client → player in camp before the clock's DONE, `endEnding` ran (solo).
  - `ending-defeat` and `ending-draw`: outcome DEFEAT for `finish(1-team)`, DRAW for `finish(-1)`; report `outcome` matches.
  - `ending-report`: report has `podium` (≥1 row, rank 1 has `mvp` true in squads), `you`, `rewards` (gold row present), `line`.
  - `ending-respawn-pending`: put the player in a respawn just before `finish`; he is not respawned during RESULT and reaches PODIUM.
  - `ending-rejoin`: `participants` member removed and re-`joined` during STATS lands in camp.
  - `ending-stop`: `voidMatch(STOPPED)` during STATS: gold unchanged, units gone.
  - `ending-snapshot-size`: a 4v4 match (`mode:4`, fill) report + snapshot serialises under the snapshot limit the server uses (find the constant where it falls back to `{}`).
- [ ] **Step 3: Submit and confirm it fails**: `python tools/checkq.py submit client --root ending-red --suites ending --env GRAVES_INSTANT_ENDING=false`, then `python tools/checkq.py wait <id>` in the background. Expected: failures (no ending).
- [ ] **Step 4: Implement** the interfaces above. Gate in `GravesAndGlory.tick`: battle block at 841 runs only when `!campaign.ending()`; the round-timeout `finish` and `mode.tick` are inside that gate; respawns wait while ending; damage hook and `allowCommanderDeath` refuse damage while ending; `OrderRules`, `Commander` (abilities, relics) and `Veterans.buy`/training refuse with no notice while ending.
- [ ] **Step 5: Run fast, then the suite** (`--suites ending --env GRAVES_INSTANT_ENDING=false`), then the existing suites that call `finish` with the default instant property: `campaign`, `camp-network` and `camp-redesign` suite names as `Phase4Checks`/`CampaignChecks` define them. Expected: all pass.
- [ ] **Step 6: Commit** `feat(ending): server ending sequence, freeze, skip and the enriched report`.

### Task 4: The podium stage

**Files:**
- Create: `src/main/java/gg/gravesandglory/PodiumStage.java`, `test-tools/standalone/PodiumStageCheck.java`
- Modify: the server start path where `CampWorld` builds the camp (build the stage after it, idempotent); `tools/verify.py` names (only if a pure part is split out: `PodiumStage` must then keep Minecraft types out of the pure part, e.g. `PodiumLayout`)

**Interfaces:**
- Produces: `PodiumLayout` (pure): `static int[] origin()` (stage centre block x,y,z), `static double[] viewSpot()` ({x,y,z,yaw,pitch}, 12 blocks in front of the centre plinth, eye level with plinth 1's top), `static double[][] plinths()` (three standing points: rank 1 centre top, rank 2 left, rank 3 right, as seen from the view spot), `static double[] centre()`; `PodiumStage.build(ServerWorld)` places the blocks (idempotent: a marker block checked first) and `PodiumStage.ensure(ServerWorld)` force-loads its chunks during an ending.
- Stage: dark stone dais (deepslate tiles / polished blackstone), three oak plinths heights 3, 2, 1 blocks with a stripped-oak top, a backdrop wall 15 wide and 8 high, two hanging banners (replaced per match by the client's side colours is not required: use neutral black banners), lanterns. Night look comes from the client camera (Task 5), not world time.

- [ ] **Step 1: Write `PodiumStageCheck`**: origin outside the arena world (`|x| > worldHalfX + 32` or `|z| > worldHalfZ + 32`, from `Spec.RULES[0]`) and at least 120 blocks from the camp origin (`Spec.CAMP[0]`), but within 300 so the move is short; `viewSpot` faces the centre plinth (yaw points at `plinths()[0]` within 2°); plinth 1 higher than 2 higher than 3; all three within the view's 50° horizontal field from 12 blocks.
- [ ] **Step 2: Run fast; expected FAIL.**
- [ ] **Step 3: Implement** `PodiumLayout` and `PodiumStage`; wire `build` after the camp build, `ensure` from `Campaign.beginEnding`.
- [ ] **Step 4: Run fast.** Expected pass. Then a harness screenshot: add an `ending-stage` check to `EndingChecks` that teleports the client to `viewSpot` and saves a screenshot to evidence.
- [ ] **Step 5: Commit** `feat(ending): the shared podium stage`.

### Task 5: Client sequence: banner, freeze look, podium scene

**Files:**
- Create: `src/main/java/gg/gravesandglory/EndingClient.java`
- Modify: `CampaignClient.java` (`accept`: read `ending`; open nothing during the ending; on step DONE open `CampScreen("lobby")` when `social` shows a room, else `"home"`, and mark `shownResult`), `GravesAndGloryClient.java` (skip key hook beside the intro skip; HUD render hook; client tick), `GeoTroopRenderer.java` / `TroopRenderer.java` / commander animation (animation clock pause while `EndingClient.frozen()`), camera mixin path used by `CampaignClient.sampleCamera`

**Interfaces:**
- Consumes: snapshot `ending` and report `podium`/`line` (Task 3).
- Produces: `EndingClient.step()` (current `MatchEnding.Step` or null), `EndingClient.frozen()`, `EndingClient.skip(MinecraftClient)` (sends `ending:skip`), `EndingClient.hud(DrawContext, int w, int h)`.
- RESULT: banner word in Cinzel-like title font (`DarkUi.title`) at ~11% of screen width height, colour VICTORY gold `DarkUi.GOLD`, DEFEAT the defeat red used by `AudioCues`/HUD, DRAW pale; lands with a 6-tick scale-in; brass rule under it; `line` under that; hint at the bottom.
- PODIUM: camera entity fixed at `viewSpot`, 8 s push-in of 3 blocks (smoothstep), letterbox bars as the entrance's; figures spawned client-side on enter and removed on exit: rank 1 at `plinths()[0]` etc., commander = `OtherClientPlayerEntity` with the player's `GameProfile` (bots: a fixed profile with the default skin), equipped with the `gear` ids as item stacks so `CommanderGearFeature` draws them; troops = client-side troop entities of each `troops` id with that rank's team, placed in an arc 1.2 blocks around the commander, facing the camera, animation paused at idle. Name plates on the HUD under each plinth via world-to-screen projection: MVP chip on rank 1, name, "<score> · <HEADLINE>", "<House> · <Dawn|Dusk>".
- Freeze: while `frozen()` (step RESULT onward), troop and commander-figure animation time stops at the value it had when the step began.

- [ ] **Step 1: Add client harness checks to `EndingChecks`**: `ending-client-banner` (HUD reports banner drawn with the right word for each outcome; screenshot), `ending-client-podium` (in PODIUM the camera entity is not the player, ≥1 figure exists in the client world near the stage, screenshot), `ending-client-skip` (pressing the menu key binding during RESULT reaches PODIUM within 10 ticks), `ending-client-frozen` (a troop's animation clock value equal across 20 ticks of RESULT).
- [ ] **Step 2: Run the suite; expected FAIL** on the new checks.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the suite and look at the screenshots** at normal and small window sizes (`docs/visual-references` rules). Expected: checks pass; banner legible at gui scale 2 and 3; plates do not overlap at 3 commanders.
- [ ] **Step 5: Commit** `feat(ending): result banner, freeze and the podium scene`.

### Task 6: Stats and rewards screens, and the report

**Files:**
- Create: `src/main/java/gg/gravesandglory/EndingPanels.java` (drawing), `EndingScreen.java` (a `Screen` for STATS and REWARDS during the ending)
- Modify: `WarTableReport.java` (new face `you` with label "You" after "Battle"; the Battle face's commanders table uses `EndingPanels.scoreboard`; the note's rewards block uses `EndingPanels.rewards` in its static form), `CampScreen.java` only if needed to route keys, `ReportSections.java` untouched

**Interfaces:**
- Consumes: report `squads` (new columns), `you`, `rewards`, `claimable`, `mode` ledger (Task 3).
- Produces: `EndingPanels.scoreboard(DrawContext, JsonObject report, int x, int y, int w)` (columns Commander, Score, K / D, Army kills, Lost, On points; your team first, a gap row between teams, your row highlighted, MVP chip); `EndingPanels.groups(DrawContext, JsonObject report, int x, int y, int w, int h)` (Commander, Army, Objectives, Economy and orders as in the mockup; Objectives shows mode rows from `ReportSections` for Siege/Convoy/Survival); `EndingPanels.rewards(DrawContext, JsonObject report, int x, int y, int w, float progress)` (rows appear one at a time and count up over `progress` 0..1; `progress` 1 draws everything final; total line; claim strip "Ready to claim: N contracts · chapter tier N" with nothing drawn when both are 0).
- `EndingScreen`: opened by `EndingClient` on STATS, swaps to rewards on REWARDS, closes on DONE; Escape or the menu key = skip (never closes the game menu); background dims the world; paper and frame as `WarTableReport` paints them (same textures).

- [ ] **Step 1: Add harness checks to `EndingChecks`**: `ending-client-stats` (screen open in STATS, every scoreboard row drawn, screenshot), `ending-client-rewards` (row count drawn equals report rewards size by the end of the step, total drawn, screenshot), `ending-client-room` (after DONE with a room the open screen is the `lobby` page; without, `home`), `ending-report-you` (War Table "Last battle" → face You draws the four groups, screenshot).
- [ ] **Step 2: Run; expected FAIL.**
- [ ] **Step 3: Implement.** Text fits at the War Table's compact page and gui scale 3 (use `UiMath` fitting helpers the report uses).
- [ ] **Step 4: Run the suite and look at the screenshots** at normal and small sizes. Expected: pass, no clipped text.
- [ ] **Step 5: Commit** `feat(ending): stats and rewards screens, report You face`.

### Task 7: Proof

**Files:**
- Modify: `test-tools/java/gg/gravesandglory/EndingChecks.java` (mode matrix), status file `C:\Users\kortt\Melty\status\ending-lead.md`
- Create: `docs/reports/2026-11-12-match-ending-proof.md` (evidence index)

- [ ] **Step 1: Mode matrix** in `EndingChecks` (suite `ending-modes`): for Domination (map 0), Siege (map 1), Convoy (map 2), Survival (map 3), start a room round in that mode (`social:` commands as `Phase4Checks` does), let it run 200 ticks, then end it as win, loss and draw (`finish(team)`, `finish(1-team)`, `finish(-1)`; Survival has no draw: win and loss only) and assert `ending-steps` order, freeze and report `line` for each. Screenshot each RESULT and PODIUM.
- [ ] **Step 2: Real ending without forcing**: one Domination round with the score limit lowered through `SheetOverride` so the mode ends itself; assert the sequence runs.
- [ ] **Step 3: Two clients**: host plus a second local client (`tools/launch_second_client.py`, the `CampNetworkChecks` pattern) in one room; both log step changes with tick stamps; check both reach each step within 10 ticks of each other; the visitor skips to DONE and lands in the room while the host is still in STATS; host's sequence unchanged.
- [ ] **Step 4: `python tools/verify.py fast --offline` and `game --offline`**, both green; save outputs.
- [ ] **Step 5: Clip**: record the full sequence (frames saved by the harness every 5 ticks, joined with ffmpeg if installed, else a contact sheet via `tools/contact_sheet.py`).
- [ ] **Step 6: Commit** `test(ending): mode matrix, two-client sync and proof index`.

### Task 8: Review, rebase, report

- [ ] **Step 1:** One fresh Sonnet 5.5 high review of `main..ui/match-ending` against the spec and this plan (`docs/PROCEDURES.md` review brief). At most two fix rounds.
- [ ] **Step 2:** `git rebase main` (expect conflicts in `Campaign.java`/`WarTableReport.java` if the army branch landed; keep both sides' intent), re-run fast and the `ending` suite.
- [ ] **Step 3:** Status file and final report: commits, checks with output, evidence paths, what is not proven.
