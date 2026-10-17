# Phase 1: Solo Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a solo skirmish in Graves and Glory read and feel right: arrows you can see, lines that break and run, troops that reinforce near the fight, bots at three difficulties with measured balance, a verified first campaign battle, an end-of-match report, and a HUD that does not depend on colour or on default keys.

**Architecture:** Server-authoritative changes go into the existing `Battle` (in `GravesAndGlory.java`), `Tactics`, `Campaign` and `BattleEffects`, plus two new server classes (`Projectiles`, `Morale`). Every decision that can be pure arithmetic goes into pure classes with no Minecraft imports (`BattleMath`, `MatchStats`, `HintText`), so a standalone `javac` check exercises it in seconds. In-game behaviour is proved by a new isolated harness suite (`test-tools/.../Phase1Checks.java`, `p1-*` check ids) and bot balance by a new dedicated-server harness (`BotSimulation.java`). Numbers live in `sheets/design.json`.

**Tech Stack:** Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Yarn 1.20.1+build.10, Loom 1.7.4, Java 17, Gradle 8.8 (offline), Python 3 (`tools/preflight.py`, audit scripts), ffmpeg (`C:\ffmpeg\ffmpeg-8.1-full_build\bin\ffmpeg`, for horn cues).

**Spec:** `docs/VISION-AND-ROADMAP.md`, section 11 "Phase 1: the solo core feels right", informed by sections 3 (pillars), 5 (commander) and 6 (battles). Current state: `docs/PROJECT-STATE.md` (open findings F-09, F-14, F-15, F-21, F-24). Repo rules: `HOW-TO-EDIT.md`.

## Global Constraints

- Minecraft 1.20.1 and Fabric only; no new dependencies, no loader change.
- Never hand-edit `src/main/java/gg/gravesandglory/Spec.java`. Change `sheets/design.json`, then `python tools/preflight.py` (also runs on every `compileJava`). Every row of a sheet has the same columns and every cell is filled. A column is `double` only if the first row's value is written with a decimal point (`2.0`, not `2`).
- Game data numbers go in sheets: `combatDiscipline`, `economy`, `botCommand`, `rules`, and the new sheets `morale` and `campaignBattles` named below. Presentation-only timings (HUD flash length) may stay as named constants in client code.
- The harness lives only in `test-tools/`. Every new harness class name is added to the `forbidden` tuple in `tools/release_config.py` (`Phase1Checks`, `BotSimulation`, `BattleMathCheck`).
- Working copy is CRLF (`core.autocrlf=true`). Edit in place; never re-save a whole dense one-line Java file with different line endings or reformat it.
- "Readable drama" and "Honest numbers" (spec section 3): nothing is done until a running game shows it by screenshot or log line; balance claims need bot-vs-bot data.
- No game launch while Kort is playing. Before every launch run `powershell -NoProfile -Command "Get-Process java,javaw -ErrorAction SilentlyContinue | Select-Object Id,StartTime,MainWindowTitle"`; if any window title starts with `Minecraft` and the tester did not start it, stop and report. Standing permission: tester subagents may launch isolated test clients when Kort is not playing.
- Isolated runs only: a fresh disposable `GRAVES_ROADMAP_TEST_ROOT` (or `GRAVES_ROADMAP_SERVER_ROOT`) under the session scratchpad; always `--offline`; `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"`; gradle is `$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle)`.
- Never publish, upload or push. Commit locally per task on a feature branch (`phase-1-solo-core`), never on `master`/`main` directly.
- Teams in Phase 1 are still Dawn (team 0) and Dusk (team 1), coloured by `factions.color`. Houses, banner colours, shards and commander actives are Phase 2; Phase 1 only leaves hook points for them.
- UI language: charcoal surfaces, ivory text, brass accents (`DarkUi` tokens), Marcellus headings, body font for body text, "no AI slop". Copy tone: grim but heroic, short, plain.
- Cut or deferred ideas go to `docs/PARKED.md` (append, never delete).
- Packet layout changes bump the channel id (`state_v4` -> `state_v5`); host and client must run the same build.

## Review Focus

1. A target dies, is discarded, or the round ends or the map switches while an arrow is in flight: no damage is applied, nothing throws, and no `gravesandglory-shot` arrow entity is left in the world. (Test: `p1-shot-orphan`, Task 1.)
2. A routing group cannot reach its base (river, blocker, already standing at base, enemy camped on it): the rout still ends by `routMaxSeconds`, the group does not flip between rout and regroup every second, and an order the player gave during the rout takes effect on regroup. (Tests: `p1-rout-ends`, `p1-rout-order-kept`, Task 2.)
3. The forward point flips, becomes contested, or has an enemy next to it between purchase and deploy: the troop deploys at base, decided at spawn time, never into an enemy blob. (Test: `p1-forward-contested`, Task 3.)
4. Kingsfield Crossroads has five points, not three, and maps switch mid-session (`CampaignMaps.select` rewrites `Spec.POINTS`): bot point choice, forward anchors and report objective arrays all size from `battle.points`. (Tests: `p1-bots-least-defended` and `p1-report-five-points` run on map 4.)
5. A player has rebound R/G/V/B/M or left a key unbound: every hint shows the actual binding name, including server-sent notices and the camp sign. (Test: `p1-hints-rebound`, Task 8.)

## Constraints from the current code (and the smallest change chosen)

- **One `Battle` per server (F-24).** `GravesAndGlory.battle` is a static singleton. The bot simulation therefore runs one match at a time per server process; parallel runs use separate roots and `server-port` values. No refactor to multiple battles in Phase 1.
- **`CampaignMaps.select` rewrites static `Spec.POINTS`/`STRUCTURES`/`TERRAIN`/`SCENERY`.** New code reads the current map's points only through `battle.points` (rebuilt by `Campaign.switchMap`), never caches `Spec.POINTS`, and fixtures wait for `battle.ready` after any `switchMap`.
- **Spec rows are immutable records.** Harness toggles (forward reinforcement on/off, round length) are plain `Battle` fields initialised from the sheet, so a check can flip them without rewriting `Spec` arrays.
- **Bot logic is one inline loop in `Campaign.tick`** gated by `b.time%100`. It is extracted into `Campaign.botDecide(Squad, Spec.BotCommandRow)` so tiers, fixtures and the simulation call the same code.
- **Ranged damage is applied in `Tactics.tick` the instant a unit fires**, with an instant particle line. The hit decision stays there (reliable hitscan); only its application moves to a delayed `Projectiles` queue.
- **Kill credit is found by matching `source.getAttacker()` to a live `Unit`**, so a shot landing after its archer died earns nobody. `Unit.lastHitBy` (a `Squad`) fixes this for kill gold, report kills and the sim.
- **A "squad" in this code is one commander's whole 20-troop army; it has three groups (infantry, ranged, support).** Morale works per group.
- **`CampaignChecks` scripts by tick number** and returns early for other suites. `Phase1Checks` follows the `CampRedesignChecks` pattern (own env var, own world start) and `CampaignChecks` gains one more early-return condition.

## Test runs used by every task

**Standalone pure checks (seconds, no game):**
```bash
cd /c/Users/kortt/Melty/bannerbound && python tools/preflight.py
J="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot/bin"
"$J/javac" --release 17 -d build/phase1-checks src/main/java/gg/gravesandglory/{Spec,BattleMath}.java test-tools/standalone/BattleMathCheck.java \
  && "$J/java" -cp build/phase1-checks gg.gravesandglory.BattleMathCheck
```
Later tasks extend the brace list with `MatchStats` (Task 7) and `HintText` (Task 8). Pass output: `BattleMathCheck: <n> checks passed`. A failure throws `AssertionError` naming the check.

**Compile both source sets:**
```bash
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle)
"$G" compileJava compileLocalTestTools --offline
```

**In-game suite `<suite>` (one launch; the suite stops the client itself):**
```bash
# Kort-not-playing check from Global Constraints first.
export GRAVES_ROADMAP_TEST_ROOT="<scratchpad>/p1-<suite>-$(date +%H%M%S)"
mkdir -p "$GRAVES_ROADMAP_TEST_ROOT"/{run,evidence,runtime}
GRAVES_PHASE1_CHECKS=<suite> "$G" runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir $GRAVES_ROADMAP_TEST_ROOT/run"
grep -E "^(pass|fail) " "$GRAVES_ROADMAP_TEST_ROOT/evidence/local-checks.txt"; grep "\[p1\]" "$GRAVES_ROADMAP_TEST_ROOT/run/logs/latest.log"; ls "$GRAVES_ROADMAP_TEST_ROOT/run/screenshots"
```
Pass means: every `p1-*` line for the suite is `pass`, the line `pass p1-<suite>-suite` is present, and the named screenshots exist. The tester opens each screenshot and states in one sentence what it shows; a screenshot that does not show the claimed thing is a fail even if the check passed.

---

### Task 1: Combat readability (visible projectiles, team impact flashes, HUD health flash)

Includes the shared harness scaffold, since this is the first task that needs it.

**Files:**
- Create: `src/main/java/gg/gravesandglory/BattleMath.java` (pure, no Minecraft imports)
- Create: `src/main/java/gg/gravesandglory/Projectiles.java`
- Create: `test-tools/standalone/BattleMathCheck.java`
- Create: `test-tools/java/gg/gravesandglory/Phase1Checks.java`
- Modify: `src/main/java/gg/gravesandglory/Tactics.java` (ranged branch of `tick`, the `enemy.damage` and particle-line lines; mend heal)
- Modify: `src/main/java/gg/gravesandglory/BattleEffects.java` (`strike`, new `flash`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`Unit`, `Battle` fields, `Battle.tick`, `clearOldUnits`, `startRound`, `AFTER_DEATH` handler)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`returnLobby`, `switchMap` call `Projectiles.clear`)
- Modify: `src/main/java/gg/gravesandglory/CommandMode.java` (`hud` group panel, new flash state)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (STATE receiver calls `CommandMode.noteGroups`)
- Modify: `sheets/design.json` sheet `combatDiscipline`: add columns `arrowSpeed: 2.2`, `boltSpeed: 1.2`, `projectileMinTicks: 2`, `projectileMaxTicks: 14`, `impactParticles: 8`
- Modify: `test-tools/isolated-checks.gradle` (add `systemProperty 'gravesandglory.phase1Checks', System.getenv('GRAVES_PHASE1_CHECKS') ?: ''`)
- Modify: `test-tools/resources/fabric.mod.json` (client entrypoint `gg.gravesandglory.Phase1Checks`)
- Modify: `test-tools/java/gg/gravesandglory/CampaignChecks.java` (early return when `gravesandglory.phase1Checks` is non-empty)
- Modify: `tools/release_config.py` (forbidden: `Phase1Checks`, `BattleMathCheck`)

**Interfaces:**
- Produces (pure): `BattleMath.travelTicks(double distance, double blocksPerTick, int minTicks, int maxTicks) -> int`; `BattleMath.tookLoss(float prevHealth, int prevCount, float health, int count) -> boolean` (true when count fell or health fell by at least 0.01).
- Produces (server): `Projectiles.record Shot(GravesAndGlory.Unit shooter, GravesAndGlory.Squad squad, LivingEntity target, String kind, float amount, Vec3d from, long fired, long lands, Entity visual)`; kinds `"arrow"` (damage), `"bolt"` (blight, visual only), `"mend"` (heal). `Projectiles.fire(Battle b, Unit u, LivingEntity target, String kind, float amount)`, `Projectiles.tick(Battle b)`, `Projectiles.clear(Battle b)`, `Projectiles.inFlight(Battle b) -> int`. `Battle.shots` is `final List<Projectiles.Shot>`.
- Produces: `GravesAndGlory.Unit.lastHitBy` (`Squad`, set whenever this unit takes battle damage); `BattleEffects.flash(Battle b, Vec3d at, int team)`; harness counters `BattleEffects.flashes` (int) and `BattleEffects.lastFlashColor` (int RGB).
- Produces (client): `CommandMode.noteGroups(List<Group> before, List<Group> after, long nowMs)`, `CommandMode.flashing(String groupId) -> boolean`, constant `CommandMode.FLASH_MS = 450`.
- Produces (harness): `Phase1Checks` with `report(String id, boolean pass, String why)` (MeltyTest line plus `[p1] id=pass why` on stdout), `shot(MinecraftClient c, String name)` (saves `p1-<name>.png`), `server(MinecraftClient c, Consumer<GravesAndGlory.Battle> f)`, `skirmish(GravesAndGlory.Battle b, ServerPlayerEntity p, int map, int size, String difficulty)` (practice-style start: waits for `ready` after `switchMap` if needed, `startRound`, then `campaign.introUntil=b.time`), `interface Suite { void step(MinecraftClient c, int tick); }`, and `finish(MinecraftClient c, String suite)` which reports `p1-<suite>-suite` (true when no earlier failure) and then `c.scheduleStop()` once the last suite in the comma list is done. Suites register by name in a `Map<String,Supplier<Suite>>`.

- [ ] **Step 1: Write the failing standalone check** `test-tools/standalone/BattleMathCheck.java` (same style as `AdmissionPolicyCheck`: `check(boolean, String)`, counter, summary line):

```java
check(BattleMath.travelTicks(22, 2.2, 2, 14) == 10, "arrow at max range lands in 10 ticks");
check(BattleMath.travelTicks(1, 2.2, 2, 14) == 2, "point-blank shots still travel the minimum");
check(BattleMath.travelTicks(400, 1.2, 2, 14) == 14, "travel is capped");
check(BattleMath.tookLoss(.80f, 12, .78f, 12) && BattleMath.tookLoss(.8f, 12, .9f, 11), "health drop or a death flashes");
check(!BattleMath.tookLoss(.80f, 12, .805f, 12) && !BattleMath.tookLoss(.5f, 6, .7f, 6), "noise and healing do not flash");
```

- [ ] **Step 2: Run it; expect a compile failure** ("cannot find symbol BattleMath").

- [ ] **Step 3: Implement `BattleMath.travelTicks` and `tookLoss`** (round `distance/blocksPerTick` to nearest, clamp to `[minTicks,maxTicks]`).

- [ ] **Step 4: Run the standalone check; expect `BattleMathCheck: 5 checks passed`.**

- [ ] **Step 5: Add the sheet columns** to `combatDiscipline` (values above), run `python tools/preflight.py`, expect `Preflight clean`.

- [ ] **Step 6: Write the in-game suite `combat`** in `Phase1Checks` (plus the scaffold above). Fixture: `skirmish(b,p,0,1,"normal")`, then `b.prepareVisualSquads(true)` (existing two facing squads), player teleported 12 blocks to the side at y+6 looking at the midpoint. Checks:
  - `p1-arrow-delay`: on the first `Shot` of kind `arrow`, the target's health at tick `fired` equals its health at fire time, and at `lands+1` it is lower (or the target is dead).
  - `p1-arrow-visible`: while that shot is in flight, an `ArrowEntity` with command tag `gravesandglory-shot` exists within 3 blocks of the straight line `from`->target; screenshot `combat-volley` taken mid-flight.
  - `p1-arrow-cleanup`: 40 ticks after the volley stops, no entity with tag `gravesandglory-shot` remains.
  - `p1-shot-orphan`: fire a shot, discard its target before `lands`, then call `campaign.finish(-1)` with another shot in flight: no exception, `Projectiles.inFlight(b)==0`, no tagged arrow remains.
  - `p1-impact-team-colour`: after a Dusk unit's arrow lands, `BattleEffects.flashes` grew and `lastFlashColor == Spec.FACTIONS[1].color()`; same for a Dawn melee hit with `Spec.FACTIONS[0].color()`.
  - `p1-hud-flash`: damage one of the player's own infantry by 4 on the server; within 30 client ticks `CommandMode.flashing("infantry")` is true; screenshot `combat-hud-flash`.
  - `p1-tick-cost`: seat eight bot squads (160 troops) in a fight around the centre point for 600 ticks; log `[p1] tick-cost avg=<ms>`; pass when `server.getTickTime() < 40`.

- [ ] **Step 7: Implement `Projectiles`.** `fire` computes `lands = time + travelTicks(distance, kind arrow ? arrowSpeed : boltSpeed, projectileMinTicks, projectileMaxTicks)`. For `arrow` it spawns a vanilla `ArrowEntity` at the shooter's eye with `setNoGravity(true)`, `setNoClip(true)`, `pickupType=DISALLOWED`, velocity `(target - from)/travelTicks`, tag `gravesandglory-shot` (no-clip arrows never collide, so they carry no damage). For `bolt`/`mend` there is no entity; `tick` draws a team-tinted `DustParticleEffect` orb (plus `WITCH` for Dusk, `ENCHANT` for Dawn) at the interpolated position each tick. On `time >= lands`: discard the visual; if the target is alive, not removed and in `b.world`, apply `amount` with `mobAttack(shooter.mob)` for `arrow`, `heal` for `mend`; set `target`'s `Unit.lastHitBy = squad`; call `BattleEffects.flash` with the shooter's team. `clear` discards visuals and empties the list. A blight bolt's `lands` is capped at the unit's `windupUntil` so the bolt arrives when the spell bursts.

- [ ] **Step 8: Wire it in.** In `Tactics.tick`, for `range()>3` replace the immediate `enemy.damage` and the 10-step particle line with `Projectiles.fire(...,"arrow",amount)` (keep the shoot sound); blight start fires a `bolt` at `spellCenter`; mend calls `fire(...,"mend",healAmount*damage)` instead of healing at once. `Battle.tick` calls `Projectiles.tick(this)` inside the running/after-intro block; `startRound`, `Campaign.returnLobby` and `Campaign.switchMap` call `Projectiles.clear`. `clearOldUnits` also discards entities tagged `gravesandglory-shot`. `BattleEffects.strike` (melee) calls `flash` at the victim with the attacker's team. `flash` spawns `impactParticles` dust particles in `Spec.FACTIONS[team].color()` (size 1.4) plus a small crit puff, and updates the counters. `AFTER_DEATH` falls back to `victim.lastHitBy` when no live attacker unit matches.

- [ ] **Step 9: HUD flash.** `CommandMode.hud` draws a 194x3 health bar under each group line (fill = group health); while `flashing(id)` the bar and the panel border use the danger token `0xffd9634c` (Task 8 moves it into `DarkUi`). The STATE receiver calls `noteGroups(old, new, Util.getMeasuringTimeMs())` before replacing `CommandMode.groups`.

- [ ] **Step 10: Compile; then run the standalone check again (still passes).**

- [ ] **Step 11: Run suite `combat` in game.** Expected: `pass` for `p1-arrow-delay`, `p1-arrow-visible`, `p1-arrow-cleanup`, `p1-shot-orphan`, `p1-impact-team-colour`, `p1-hud-flash`, `p1-tick-cost`, `p1-combat-suite`; screenshots `p1-combat-volley.png` (arrows visibly mid-air between two lines, coloured puffs at impacts) and `p1-combat-hud-flash.png` (infantry bar in the danger colour).

- [ ] **Step 12: Append to `docs/PARKED.md`:** "Display-entity arrows with interpolation, and arrow arcs (2026-10-17, Phase 1 plan; no-clip arrows chosen as the smallest reliable visual)."

- [ ] **Step 13: Commit.**
```bash
git add -A sheets/design.json src/main/java/gg/gravesandglory test-tools tools/release_config.py docs/PARKED.md
git commit -m "Phase 1: visible delayed projectiles, team impact flashes, HUD health flash (F-14)"
```

---

### Task 2: Morale, rout and horns

**Files:**
- Create: `src/main/java/gg/gravesandglory/Morale.java`
- Modify: `src/main/java/gg/gravesandglory/BattleMath.java` (rout maths)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`Squad` fields, `Battle.assignment`, `Battle.tick`, `reinforce`, `tactical`, ORDER handling, `sendState` group block, `STATE` id -> `state_v5`, sound registration loop)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (bot charge transition plays the horn)
- Modify: `src/main/java/gg/gravesandglory/BattleEffects.java` (`horn`)
- Modify: `src/main/java/gg/gravesandglory/CommandMode.java` (`Group` gains `boolean routing`; HUD shows `ROUTING`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (STATE reader)
- Modify: `tools/create_battle_audio.py` (three new cues; name list), regenerated `src/main/resources/assets/gravesandglory/sounds/horn_{charge,rout,rally}.ogg`, `sounds.json`, `lang/en_us.json`
- Modify: `sheets/design.json`: new sheet `morale`, one row `{"id":"domination","routShare":0.25,"flankedRoutShare":0.6,"flankRadius":8.0,"flankArcDegrees":110.0,"flankMinAttackers":3,"engageCheckRadius":16.0,"routMinSeconds":6,"routMaxSeconds":15,"routSafeRadius":12.0,"regroupImmunitySeconds":20,"hornCooldownSeconds":8,"hornVolume":1.6}`
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (suite `morale`)

**Interfaces:**
- Consumes: `Phase1Checks` scaffold (Task 1).
- Produces (pure): `BattleMath.strength(double livingHealth, double fullHealth) -> double` (clamped 0..1; `fullHealth<=0` gives 1); `BattleMath.relativeBearing(double facingYawDeg, double dx, double dz) -> double` degrees 0..180 between Minecraft forward `(-sin yaw, cos yaw)` and `(dx,dz)`; `BattleMath.flanked(double facingYawDeg, double[] dx, double[] dz, double arcDeg, int minAttackers) -> boolean`; `BattleMath.shouldRout(double strength, boolean flanked, double routShare, double flankedRoutShare) -> boolean`.
- Produces (server): `Morale.tick(Battle b)` (every 20 ticks); `Morale.routing(Squad s, String group) -> boolean`; `Morale.interface RoutGuard { boolean holds(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group); }` and `Morale.GUARDS` (`CopyOnWriteArrayList<RoutGuard>`, empty in Phase 1; Phase 2's Gilded *Bulwark* and Champion *Stand Fast* register here); `Morale.rally(Battle b, Squad s, String group)` (ends a rout now, plays rally horn; Phase 2's Marshal *Rally Horn* calls it). `Squad` gains `Map<String,Long> routedAt`, `Map<String,Long> steadyUntil`, `Map<String,Double> fullHealth`.
- Produces: `BattleEffects.horn(Battle b, Vec3d at, String kind)` with kinds `charge`, `rout`, `rally`, rate-limited per squad by `hornCooldownSeconds`; harness counter `BattleEffects.horns` (`Map<String,Integer>`).
- Produces: `CommandMode.Group(..., boolean fighting, boolean routing)`; channel `gravesandglory:state_v5`.

**Rules (decisions the implementer cannot make alone):**
- Strength of a group = sum of living members' health / `fullHealth[group]`, where `fullHealth` is set in `reinforce` from the squad's lineup (`spec.health() * campaign.health(s,spec)` per slot of that group) and never lowered mid-round.
- A group is checked only when an enemy unit or commander is within `engageCheckRadius` of its centre. Flanked = at least `flankMinAttackers` enemy units within `flankRadius` of the centre whose current target is a member, at a bearing more than `flankArcDegrees` from the group's assignment yaw.
- Rout when `shouldRout` and no `RoutGuard` holds and `time >= steadyUntil[group]`. While routing, `Battle.assignment(u)` returns `new Assignment("retreat","loose",base(team),yaw)` without touching `s.assignments`, so player orders given during the rout are stored and apply on regroup.
- Regroup when (`routMinSeconds` passed and no enemy within `routSafeRadius`) or the centre is within 10 blocks of base or `routMaxSeconds` passed. Then `steadyUntil = time + regroupImmunitySeconds*20` and the rally horn plays.
- Horns: `charge` when a group's effective order changes to `charge` (player ORDER/tactical or bot); `rout` at rout start; `rally` at regroup. Played with `world.playSound(null, centre, horn_<kind>, SoundCategory.HOSTILE, hornVolume, pitch)`; Dusk pitch 0.85, Dawn 1.0.

- [ ] **Step 1: Add failing standalone checks** to `BattleMathCheck`:

```java
check(Math.abs(BattleMath.strength(60, 360) - 1/6.0) < 1e-9 && BattleMath.strength(400, 360) == 1 && BattleMath.strength(5, 0) == 1, "strength is health-weighted and clamped");
check(BattleMath.relativeBearing(0, 0, 1) < 1 && Math.abs(BattleMath.relativeBearing(0, 0, -1) - 180) < 1 && Math.abs(BattleMath.relativeBearing(0, -1, 0) - 90) < 1, "yaw 0 faces +z");
double[] rx={0,.5,-.5}, rz={-5,-5,-5}, fx={0,.5,-.5}, fz={5,5,5};
check(BattleMath.flanked(0, rx, rz, 110, 3) && !BattleMath.flanked(0, fx, fz, 110, 3) && !BattleMath.flanked(0, new double[]{0,0}, new double[]{-5,-5}, 110, 3), "three rear attackers flank; front or two do not");
check(BattleMath.shouldRout(.2, false, .25, .6) && BattleMath.shouldRout(.5, true, .25, .6), "weak routs; flanked half-strength routs");
check(!BattleMath.shouldRout(.5, false, .25, .6) && !BattleMath.shouldRout(.9, true, .25, .6), "half-strength unflanked and flanked fresh hold");
```

- [ ] **Step 2: Run; expect compile failure on the new methods.**
- [ ] **Step 3: Implement the four `BattleMath` methods.**
- [ ] **Step 4: Run; expect `BattleMathCheck: 10 checks passed`.**
- [ ] **Step 5: Add the `morale` sheet; run preflight; expect clean.**
- [ ] **Step 6: Horn cues.** Add to `tools/create_battle_audio.py` three `cue(...)` calls using the existing `'horn'` and `'drum'` kinds (charge: rising two-note call, 1.6 s; rout: falling minor third, 1.8 s; rally: three-note ascending call, 1.4 s) and extend the `sounds.json`/lang name list with `horn_charge`, `horn_rout`, `horn_rally` (subtitles "Charge horn", "Retreat horn", "Rally horn"). Run `python tools/create_battle_audio.py`; expect `Created ... Ogg sound cues` and three new `.ogg` files. Register the three ids in the `onInitialize` sound loop.
- [ ] **Step 7: Write in-game suite `morale`.** Fixture on map 0: player squad (Dawn, normal lineup) placed 60 blocks from Dusk base facing yaw 90; one Dusk synthetic squad `hold` 6 blocks in front.
  - `p1-rout-weak`: `kill()` 10 of 12 knights; within 40 ticks `Morale.routing(s,"infantry")`, `BattleEffects.horns.get("rout")` grew, and after 60 more ticks the knights' mean distance to base fell by at least 4 blocks; screenshot `morale-rout`.
  - `p1-rout-order-kept`: during that rout, call `b.tactical(p,"infantry","hold","block",x,z,yaw,-1)`; after regroup the infantry assignment is that `hold`.
  - `p1-rout-ends`: from rout start, routing is false by `routMaxSeconds*20+40` ticks even with the enemy squad teleported next to the base, and the rally horn counter grew; within the following `regroupImmunitySeconds` the group does not rout again.
  - `p1-flank-rout`: fresh group, kill 6 of 12 knights (half strength, no rout yet: assert not routing after 40 ticks); place 4 enemy infantry 5 blocks behind (relative to yaw) with `setTarget` on knights; routing within 40 ticks.
  - `p1-rout-guard`: register a `RoutGuard` returning true, repeat the weak setup; no rout; unregister.
  - `p1-charge-horn`: issue `tactical(...,"charge",...)` at a visible enemy; charge horn counter grew; a second charge within `hornCooldownSeconds` does not grow it.
  - `p1-hud-routing`: the client `CommandMode.groups` entry for infantry has `routing()==true` during the rout; screenshot `morale-hud`.
- [ ] **Step 8: Implement `Morale` and wiring** per the Rules above; `sendState` writes `routing` after `fighting` and the client reads it; bump `STATE` to `state_v5`; HUD second line reads `ROUTING` in the warn colour while routing.
- [ ] **Step 9: Compile; run suite `morale`.** Expected `pass` for the seven ids and `p1-morale-suite`; screenshots `p1-morale-rout.png` (knights running back past the camera, away from the enemy) and `p1-morale-hud.png` (infantry panel reading ROUTING). Log shows `[p1] p1-charge-horn=true`.
- [ ] **Step 10: Commit** `"Phase 1: morale, rout and regroup with horns; RoutGuard hook for Phase 2 traits"`.

---

### Task 3: Forward reinforcement from held objectives (F-09)

**Files:**
- Modify: `src/main/java/gg/gravesandglory/BattleMath.java` (`nearestAnchor`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`Capture.ownedSince`, `captureTick` sets it, `Battle.forwardReinforcement` field, `reinforcementAnchors`, `deployPoint`, `trainingTick` uses it, deploy message)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (lesson 4 copy, see Task 6)
- Modify: `sheets/design.json` sheet `economy`: add `forwardReinforcement: true`, `forwardHoldSeconds: 10`, `forwardSafeRadius: 20.0`, `forwardSpawnOffset: 8.0`
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (suite `reinforce`)

**Interfaces:**
- Produces (pure): `BattleMath.nearestAnchor(double tx, double tz, double bx, double bz, double[] ax, double[] az) -> int`: index of the anchor nearest the target `(tx,tz)`, or `-1` when none is strictly nearer to the target than the base `(bx,bz)`.
- Produces (server): `Battle.forwardReinforcement` (`boolean`, initialised from `Spec.ECONOMY[0].forwardReinforcement()`); `Battle.reinforcementAnchors(int team) -> List<Capture>`: points owned by `team` for at least `forwardHoldSeconds`, not contested, no enemy unit or commander within `forwardSafeRadius`. Phase 2's Quartermaster *Forward Supply* banner adds its anchor here. `Battle.deployPoint(Squad s, int slot) -> Vec3d`: chosen at spawn time; anchor centre moved `forwardSpawnOffset` toward own base plus the existing `(slot%5-2)*2, (slot/5-2)*2` ring, ground-snapped; falls back to the current base spot if the result is in the river (`Arena.river` and ground < 64). Target used for the choice: the infantry assignment target if present, else `s.target`. `Battle.deployNote(Squad s, Capture anchor) -> String` builds the deploy message (null anchor means barracks).

- [ ] **Step 1: Add failing standalone checks:**

```java
double[] ax={0,-40}, az={0,-140};
check(BattleMath.nearestAnchor(40, 0, -108, 0, ax, az) == 0, "centre point is nearer the front than base");
check(BattleMath.nearestAnchor(-100, 0, -108, 0, ax, az) == -1, "base wins when the fight is at home");
check(BattleMath.nearestAnchor(40, 0, -108, 0, new double[0], new double[0]) == -1, "no anchors means base");
```
- [ ] **Step 2: Run; expect compile failure. Step 3: implement. Step 4: run; expect 13 checks passed.**
- [ ] **Step 5: Add the `economy` columns; preflight clean.**
- [ ] **Step 6: Write in-game suite `reinforce`** (map 0, `skirmish(b,p,0,1,"normal")`, bot squad set to `hold` at its base). Player squad ordered `move` to `points.get(1)` centre + 40 blocks toward Dusk (x+40). Measure helper: disband one infantry, push `new Training(knightSpec, b.time)` onto `s.training`, then count ticks from the new `Unit` appearing until it is within `arrivalTolerance+3` of `b.formation(unit)`; timeout 2400 ticks.
  - `p1-reinforce-walk`: run the helper with `b.forwardReinforcement=false` (before) and again with it `true` after setting `points.get(1).owner=team`, `ownedSince=time-forwardHoldSeconds*20-1` (after). Log `[p1] reinforce-walk before=<s>s after=<s>s`. Pass when `after < before` and `after <= 20`.
  - `p1-forward-contested`: set the point `contested=true` and repeat one spawn: the troop appears within 30 blocks of `Battle.base(team)`.
  - `p1-forward-enemy-near`: point owned, uncontested, one enemy unit placed 10 blocks from it: spawn at base.
  - `p1-forward-message`: `b.deployNote(s, anchor)` for the anchored spawn contains the point's name, and `b.deployNote(s, null)` contains "barracks".
  - Screenshot `reinforce-forward` from above the centre point at the anchored spawn tick.
- [ ] **Step 7: Implement** `ownedSince` (set when `captureTick` changes `owner`), `reinforcementAnchors`, `deployPoint`, and replace the `base(s.team).add(...)` spawn in `trainingTick` with `deployPoint(s,slot)`. Message text comes from `Battle.deployNote(Squad s, Capture anchor) -> String`: `"<troop> trained: deploying at <point name>"`, or `"<troop> trained: deploying from barracks"` when `anchor` is null.
- [ ] **Step 8: Compile; run suite `reinforce`.** Expected `pass` for the four ids and `p1-reinforce-suite`; the log line with both times (expected before about 30 to 60 s, after under 20 s); `p1-reinforce-forward.png` shows a fresh knight standing at the captured centre point flag.
- [ ] **Step 9: Commit** `"Phase 1: forward reinforcement from held objectives, walk measured (F-09)"`, with the before/after times in the message body.

---

### Task 4: Bot difficulty tiers (Easy / Normal / Hard) (F-15)

**Files:**
- Modify: `sheets/design.json` sheet `botCommand`: replace the single row with three rows (Normal keeps today's numbers so Normal behaviour is unchanged):

| id | retreatShare | rallyShare | chargeRatio | decisionTicks | flankLeastDefended | defenderWeight | shieldwallVsArchers | archerThreatCount | archerThreatRange | chargeDistance |
|---|---|---|---|---|---|---|---|---|---|---|
| easy | 0.35 | 0.7 | 2.5 | 160 | false | 0.0 | false | 3 | 30.0 | 0.0 |
| normal | 0.25 | 0.6 | 1.5 | 100 | false | 0.0 | false | 3 | 30.0 | 0.0 |
| hard | 0.2 | 0.55 | 1.3 | 60 | true | 25.0 | true | 3 | 30.0 | 18.0 |

- Modify: `src/main/java/gg/gravesandglory/BattleMath.java` (`leastDefended`, `timedCharge`, `shieldwallVsArchers`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`difficulty` field, `difficulty:` command, `tier(Squad)`, extract `botDecide`, snapshot field)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`Squad.tier` String)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java` (home page "Bots: <tier>" button) and `CampaignLobbyScreen.java` (lead controls button), `CampaignClient.java` (`difficulty` from snapshot)
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (suite `bots`)

**Interfaces:**
- Produces (pure): `BattleMath.leastDefended(double cx, double cz, double[] px, double[] pz, int[] owner, int[] defenders, int team, double defenderWeight) -> int` (minimises `distance + defenders*defenderWeight` over points not owned by `team`; if all are owned, over all points); `BattleMath.timedCharge(double nearestEnemyDistance, long living, long enemiesNear, double chargeRatio, double chargeDistance) -> boolean` (`living >= enemiesNear*chargeRatio` and, when `chargeDistance > 0`, `nearestEnemyDistance <= chargeDistance`); `BattleMath.shieldwallVsArchers(int rangedThreats, int meleeThreats, int threshold, boolean hasShields) -> boolean` (`hasShields && rangedThreats >= threshold && meleeThreats == 0`).
- Produces (server): `Campaign.difficulty` (`String`, default `"normal"`, server-wide because there is one battle per server); command `difficulty:<easy|normal|hard>` (refused while `b.running`, like other global choices); `Squad.tier` set from `difficulty` in `prepareRound` for every bot; `Campaign.tier(Squad s) -> Spec.BotCommandRow` (row by id, fallback normal); `Campaign.botDecide(GravesAndGlory.Squad s, Spec.BotCommandRow tier)` = today's per-bot loop body plus the Hard branches. `Campaign.tick` runs it per bot when `b.time % tier.decisionTicks() == 0`. Snapshot gains `"difficulty"`.
- Hard branches: point choice uses `leastDefended` with `defenders[i]` = entries in `b.sightings.get(s.team)` within `2*radius` of point `i` (bots see only what their team saw), over all of `b.points` (fixes `lane%3` ignoring Kingsfield flanks); charge only when `timedCharge` with distance from the army centre to the nearest remembered enemy; when `shieldwallVsArchers` (shield troops alive in infantry; ranged threats = remembered enemy ranged units within `archerThreatRange` of centre; melee threats = enemy infantry within 8) put `s.assignments.put("infantry", new Assignment(order,"shieldwall",target,yaw))`. Dusk has no shield troop (F-12, open), so Dusk Hard bots never shieldwall in Phase 1.

- [ ] **Step 1: Add failing standalone checks:**

```java
double[] px={-44,0,44}, pz={-43,0,43}; int[] own={-1,-1,-1};
check(BattleMath.leastDefended(0,0,px,pz,own,new int[]{0,9,0},1,25) != 1, "nine defenders push hard bots off the centre");
check(BattleMath.leastDefended(0,0,px,pz,own,new int[]{0,0,0},1,25) == 1, "undefended nearest point wins");
check(BattleMath.leastDefended(0,0,px,pz,new int[]{1,1,1},new int[]{0,0,0},1,25) == 1, "all owned falls back to all points");
check(!BattleMath.timedCharge(60,20,8,1.3,18) && BattleMath.timedCharge(15,20,8,1.3,18) && BattleMath.timedCharge(60,20,8,1.5,0), "hard waits for range; normal ignores distance");
check(BattleMath.shieldwallVsArchers(4,0,3,true) && !BattleMath.shieldwallVsArchers(4,1,3,true) && !BattleMath.shieldwallVsArchers(4,0,3,false), "shieldwall only vs archers, only with shields");
```
- [ ] **Step 2: Run; fail. Step 3: implement. Step 4: run; expect 18 checks passed.**
- [ ] **Step 5: Replace the `botCommand` rows; preflight clean. Replace every `Spec.BOTCOMMAND[0]` with `tier(s)`.**
- [ ] **Step 6: Write in-game suite `bots`** (map 4, Kingsfield, five points). Fixtures call `b.campaign.botDecide(bot, row)` directly on a synthetic Dawn bot squad (so the Dawn shield troop exists) with `tier="hard"`.
  - `p1-bots-least-defended`: plant nine Dusk sightings on point index 1 and none on the flank points; Hard target is not point 1 and is one of indices 0..4 (log which); Normal on the same state keeps its lane rule.
  - `p1-bots-shieldwall`: spawn six Dusk archers 20 blocks in front, none melee within 8; after `botDecide`, infantry assignment formation is `shieldwall`; 40 ticks later at least 6 knights `isUsingItem()`; screenshot `bots-shieldwall`.
  - `p1-bots-charge-timing`: enemies 60 blocks away gives order `attack`; moved to 15 blocks gives `charge` (and the charge horn counter grows).
  - `p1-bots-tiers`: `difficulty:hard` command while not running sets `campaign.difficulty`; starting a solo pad 3v3 gives all five bots `tier=="hard"`; the command is refused while running; the camp home screenshot `bots-difficulty` shows "Bots: Hard".
- [ ] **Step 7: Implement** the extraction, tiers, command, snapshot and both buttons (cycle easy -> normal -> hard).
- [ ] **Step 8: Compile; run suite `bots`.** Expected `pass` for the four ids and `p1-bots-suite`; `p1-bots-shieldwall.png` shows knights with raised shields facing archers. Also run `-PcampaignChecks` without `GRAVES_PHASE1_CHECKS` (existing suite) and confirm `bot-objectives` and `bot-replacements` still pass (Normal unchanged).
- [ ] **Step 9: Commit** `"Phase 1: Easy/Normal/Hard bot tiers; Hard flanks, shieldwalls and times charges (F-15)"`.

---

### Task 5: Headless bot-vs-bot simulation harness

**Files:**
- Create: `test-tools/java/gg/gravesandglory/BotSimulation.java` (server `ModInitializer`)
- Create: `src/main/java/gg/gravesandglory/MatchStats.java` (pure; first field set)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`Squad.stats`, gold income and kill gold add to `stats.goldEarned`, `Battle.roundSeconds` field replacing `R.roundSeconds()` reads in `tick` and `sendState`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`lastWinner` set in `finish`)
- Modify: `build.gradle` (a `botSim` block, below the `selfTest` block)
- Modify: `test-tools/resources/fabric.mod.json` (main entrypoint `gg.gravesandglory.BotSimulation`)
- Modify: `tools/release_config.py` (forbidden: `BotSimulation`)

**Interfaces:**
- Produces (pure): `MatchStats.SquadStats` (mutable: `int goldEarned`; Task 7 adds more); `MatchStats.winRate(int wins, int matches) -> double` (0 when `matches==0`).
- Produces (server): `Squad.stats` (`MatchStats.SquadStats`, reset in `startRound`); `Battle.roundSeconds` (`int`, initialised from `R.roundSeconds()`); `Campaign.lastWinner` (`int`, -1 draw).
- Produces (harness): system properties `gravesandglory.botSim` (matches N), `gravesandglory.simDawn`, `gravesandglory.simDusk` (tier ids), `gravesandglory.simMaps` (comma list, cycled), `gravesandglory.simSize` (default 3), `gravesandglory.simRoundSeconds` (default 300). Output in `<server root>/botsim/`: `results.csv` header `match,map,dawnTier,duskTier,winner,dawnScore,duskScore,seconds`; `gold.csv` header `match,second,team,goldEarned,goldBank,living` sampled every 30 s; final stdout line `[botsim] matches=N dawn(<tier>)=W1 dusk(<tier>)=W2 draws=D dawnWin=P% meanGoldEarnedAtEnd dawn=G1 dusk=G2`.
- `build.gradle` block: when `project.hasProperty('botSim')`: `loom.runs.server.vmArg` for `-Dgravesandglory.localIntegration=true`, `-Dfabric.addMods=<test tools jar>`, and each `-Dgravesandglory.sim*` from the matching `-P` property; `tasks.named('runServer'){ dependsOn localTestToolsJar }`.

- [ ] **Step 1: Add failing standalone checks** (extend the compile list with `MatchStats`):

```java
check(MatchStats.winRate(7,10) == .7 && MatchStats.winRate(0,0) == 0, "win rate");
var st=new MatchStats.SquadStats(); st.goldEarned+=5; check(st.goldEarned == 5, "stats accumulate");
```
- [ ] **Step 2: Run; fail. Step 3: implement `MatchStats` minimal. Step 4: run; expect 20 checks passed.**
- [ ] **Step 5: Implement `BotSimulation`.** State machine on `END_SERVER_TICK`, only when `gravesandglory.botSim` is set: wait `battle.ready`; for match `i`, `switchMap(maps[i%len])` when needed and wait `ready`; set `campaign.size=simSize, fill=true, practice=false, difficulty=simDawn`, `battle.roundSeconds=simRoundSeconds`, `battle.startRound()`, then set `tier` of team-1 bots to `simDusk`; sample every 600 ticks; when `running` turns false write the result row; after N matches print the summary and `server.stop(false)`. No players are needed: units tick in the chunks `updateUnitChunks` forces.
- [ ] **Step 6: Smoke run** (2 matches, 120 s rounds) on a fresh root; copy Kort's existing `run-server/eula.txt` and `run-server/server.properties` into it (do not write a new EULA acceptance); set `server-port=25601`:
```bash
export GRAVES_ROADMAP_SERVER_ROOT="<scratchpad>/botsim-smoke"; mkdir -p "$GRAVES_ROADMAP_SERVER_ROOT"
cp run-server/eula.txt run-server/server.properties "$GRAVES_ROADMAP_SERVER_ROOT/"; sed -i 's/^server-port=.*/server-port=25601/' "$GRAVES_ROADMAP_SERVER_ROOT/server.properties"
"$G" runServer --offline -PbotSim=2 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0 -PsimRoundSeconds=120 --init-script test-tools/isolated-server.gradle
```
Expected: two rows in `results.csv`, at least 8 rows in `gold.csv`, the `[botsim]` summary line, and the server exits by itself.
- [ ] **Step 7: Balance runs** (background, two processes on ports 25601/25602, maps `0,1,2,3,4`, 300 s rounds, 10 matches each): pairings hard-vs-normal, normal-vs-hard, normal-vs-easy, normal-vs-normal. Acceptance: Hard wins at least 60% across both side assignments combined; Normal beats Easy at least 60%; Normal-vs-Normal Dawn win rate within 35% to 65%. If a pairing misses, tune only `botCommand` sheet values and rerun that pairing; record each run's summary line.
- [ ] **Step 8: Append to `docs/PARKED.md`:** "Fast-tick mixin for the bot simulation (run matches faster than real time) (2026-10-17, Phase 1 plan; real-time runs in parallel processes chosen instead)."
- [ ] **Step 9: Commit** `"Phase 1: headless bot-vs-bot simulation harness"`, with the four `[botsim]` summary lines in the body.

---

### Task 6: Tutorial verified in game and reframed as campaign battle 1

The F-17 tutorial (commit 5b9603b) has four server-advanced lessons (`Campaign.tutorialStep/tutorialDid`, client `GravesAndGloryClient.lesson`) shown in a player's first ever match. It has never been seen running. This task proves it runs, fixes what the first run shows, and moves it into "The Long March, battle 1".

**Files:**
- Modify: `sheets/design.json`: new sheet `campaignBattles`, one row `{"id":"march-1","chapter":1,"order":1,"name":"The Training Field","map":0,"size":1,"difficulty":"easy","lessons":true,"briefing":"Dusk raiders hold the river road. Take the crossings and teach your banner to follow you.","reason":"The road west must open before the march begins."}`
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`activeBattle`, `campaign:<id>` command, `pendingBattle` start after map build, `tutorialStep` condition, `Profile.campaignCleared`, `finish` marks clear on victory, snapshot `"battle"` object)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java` (home: primary "THE LONG MARCH / BATTLE 1" while `campaignCleared==0`, otherwise a normal button; "Practice" stays as a skirmish vs one bot at the chosen difficulty)
- Modify: `src/main/java/gg/gravesandglory/CampaignClient.java` (`battle` briefing fields; briefing card in `hud` during the intro)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (`lesson` heading `"THE LONG MARCH / BATTLE 1 / LESSON n OF 4"`; lesson 4 copy: `"Recruit at your barracks lectern. Trained troops march out from the held point nearest the fight."` with the use-key name)
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (suite `tutorial`)

**Interfaces:**
- Consumes: `Campaign.difficulty` and `Squad.tier` (Task 4); `deployPoint` (Task 3, lesson 4 wording).
- Produces: `Campaign.activeBattle` (`Spec.CampaignBattlesRow` or null; cleared in `returnLobby`); command `campaign:march-1` (solo only, refused while running, same party rules as `camp:practice`; switches map then starts when `ready`); `tutorialStep(id)` returns a lesson only when `activeBattle != null && activeBattle.lessons()` and the player is a participant (lessons repeat on replay; the gold reward stays once-only via `tutorialDone`). `Profile.campaignCleared` (`int`, highest `order` won). Snapshot `"battle": {"id","name","briefing","reason"}` (empty object when none).
- The first-ever-match trigger is removed: pads and rooms never show lessons; new commanders are pointed to battle 1 by the camp guide's primary button.

- [ ] **Step 1: Verification run of the current tutorial first** (before changing code): write suite `tutorial` steps that start practice, issue a `tactical` move, a formation change, an attack, and `command(p,"barracks")`, each 60 ticks apart after the intro, asserting `CampaignClient.tutorial` goes 0,1,2,3,-1 and gold rises by `rollCost`; screenshot each lesson (`tutorial-lesson-1`..`4`). Run it on today's code. Record what passes and fails; fix only genuine defects found (use superpowers:systematic-debugging), commit those fixes separately: `"F-17: fix <defect> found in first in-game tutorial run"`.
- [ ] **Step 2: Extend suite `tutorial` for the reframe:**
  - `p1-tutorial-start`: `b.command(p,"campaign:march-1")`; within 900 ticks `b.running`, `campaign.size==1`, `campaign.map==0`, the one bot has `tier=="easy"`, `CampaignClient.battle` name is "The Training Field"; screenshot `tutorial-briefing` during the intro shows the briefing card.
  - `p1-tutorial-lessons`: the four lessons advance in order from real commands (as in Step 1); screenshots `tutorial-lesson-1`..`4`.
  - `p1-tutorial-reward-once`: gold rises by `rollCost` once; a replay of `campaign:march-1` shows lessons again but pays nothing.
  - `p1-tutorial-clear`: `campaign.finish(0)` marks `campaignCleared==1`; the camp home shows battle 1 as a normal button (screenshot `tutorial-camp-after`).
  - `p1-tutorial-not-on-pads`: a solo pad 3v3 for a fresh profile shows no lesson (`CampaignClient.tutorial==-1`).
- [ ] **Step 3: Run suite `tutorial`; expect the five new ids to fail** (command unknown).
- [ ] **Step 4: Add the sheet; implement** the command, `pendingBattle`, briefing, camp buttons, copy and `tutorialStep` change.
- [ ] **Step 5: Compile; run suite `tutorial`.** Expected `pass` for the five ids and `p1-tutorial-suite`; screenshots show the briefing card and each lesson naming the bound key.
- [ ] **Step 6: Commit** `"Phase 1: tutorial becomes The Long March battle 1 (F-17 verified in game)"`.

---

### Task 7: End-of-match report screen

**Files:**
- Modify: `src/main/java/gg/gravesandglory/MatchStats.java` (`SquadStats` gains `kills`, `lost`, `objectiveSeconds`; `teamPointSeconds`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`AFTER_DEATH` counts `kills` for the killer squad, including commander kills via `squads.get(attacker)`, and `lost` for the victim squad; `captureTick` adds 1 s of `objectiveSeconds` to each squad with a unit or its commander inside any point radius, and 1 s to `Battle.pointSeconds[team][point]` for each owned point; `int[][] pointSeconds` sized from `points.size()` in `startRound`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`finish` adds `"squads"` and `"points"` arrays to each participant's report)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java` (`results` page table)
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (suite `report`)

**Interfaces:**
- Produces (pure): `MatchStats.SquadStats { int kills, lost, goldEarned, objectiveSeconds; }`; `MatchStats.mvp(List<SquadStats> rows) -> int` (index with most kills, ties broken by objectiveSeconds, -1 when empty).
- Produces: report JSON (inside the existing `results` object, read by `CampaignClient.result`): `"squads": [{"name","team","bot","tier","you","kills","lost","gold","objective"}]` (all seated squads of both teams, own team first; bot names `"<tier> bot"`), `"points": [{"name","dawn","dusk"}]` (seconds held per team, one entry per `battle.points` element, so five on Kingsfield).
- `results` page layout: outcome headline (existing), then a table with columns COMMANDER / KILLS / LOST / GOLD / ON POINTS, one row per squad, your row in brass, the MVP row marked with a crest; under it one line per point "<name>  Dawn <s>s / Dusk <s>s"; existing reward lines stay below. Fits 760x460 scaled; rows trim with `trimToWidth`. House trait uptime (spec section 6) is Phase 2 and is not shown.

- [ ] **Step 1: Add failing standalone checks:**

```java
var a=new MatchStats.SquadStats(); a.kills=4; a.objectiveSeconds=10; var b2=new MatchStats.SquadStats(); b2.kills=4; b2.objectiveSeconds=30;
check(MatchStats.mvp(List.of(a,b2)) == 1 && MatchStats.mvp(List.of()) == -1, "mvp by kills then objective time");
```
- [ ] **Step 2: Run; fail. Step 3: implement. Step 4: run; expect 21 checks passed.**
- [ ] **Step 5: Write in-game suite `report`** on map 4 (`skirmish(b,p,4,1,"normal")`):
  - Kill two enemy units with commander damage (`damage(playerAttack(p), 99)`), `kill()` one own unit, own and stand on point 0 for 60 ticks, wait one income interval, then `campaign.finish(team)`.
  - `p1-report-fields`: the player's report has `squads` of size `b.squads` seated count; own row `kills>=2`, `lost>=1`, `gold>=passiveGold`, `objective>=2`.
  - `p1-report-five-points`: `points` has 5 entries and point 0 shows at least 2 s for the player's team.
  - `p1-report-screen`: within 60 client ticks the current screen is `CampScreen` on page `results`; screenshot `report`.
  - `p1-report-late-kill`: an arrow from a unit that died before landing still credits its squad (`kills` grew), proving `lastHitBy`.
- [ ] **Step 6: Implement** the counters, JSON and table.
- [ ] **Step 7: Compile; run suite `report`.** Expected `pass` for the four ids and `p1-report-suite`; `p1-report.png` shows the table with readable numbers and five point lines.
- [ ] **Step 8: Commit** `"Phase 1: end-of-match report with kills, losses, gold and objective time"`.

---

### Task 8: HUD and camp UI cleanup, colour-blind glyphs, key-bound hints (F-21)

**Files:**
- Create: `src/main/java/gg/gravesandglory/HintText.java` (pure)
- Create: `test-tools/standalone/test_ui_standard.py`
- Modify: `src/main/java/gg/gravesandglory/DarkUi.java` (tokens, `teamMark`, `teamText`, `panel`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (drop `GOLD/INK/MUTED/WHITE` and `factionColor` in favour of `DarkUi`; `renderHud`, `campHud`, `lesson`, `map`, `keyHints`, `MapScreen` legend; `hint(String id)` resolver)
- Modify: `src/main/java/gg/gravesandglory/CommandMode.java` (`hud` text from bindings: confirm = `options.attackKey`/`confirmOrder`, cancel = `options.useKey`/`cancelOrder`)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java`, `CampaignLobbyScreen.java`, `CampaignClient.java` (notices and `message` pass through `HintText.expand`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java`, `GravesAndGlory.java` (server strings use `{key:menu}` tokens), `CampWorld.java` (camp label uses `Text.keybind("key.gravesandglory.menu")` via a `label` overload taking `Text`)
- Modify: `tools/release_config.py` only if a new harness name appears (none expected)
- Modify: `test-tools/java/gg/gravesandglory/Phase1Checks.java` (suite `ui`)

**Interfaces:**
- Produces (pure): `HintText.expand(String template, java.util.function.Function<String,String> keyName) -> String` replaces every `{key:<id>}` token with `keyName.apply(id)`; unknown or malformed tokens are left as-is.
- Produces (client): `GravesAndGloryClient.hint(String id) -> String` (ids `menu, attack, follow, formation, map, rotate_left, rotate_right, confirm, cancel, use, hit`; `use`/`hit` map to `options.useKey`/`options.attackKey`; returns `getBoundKeyLocalizedText().getString()`, which already reads "Not bound" style text for unbound keys); `GravesAndGloryClient.campHint() -> String` (the camp bottom hint, for the check).
- Produces (UI): `DarkUi` tokens `GOLD, EDGE, PAPER, MUTED` (existing) plus `INK=0xf019171d`, `WARN=0xffff9955`, `DANGER=0xffd9634c`, `DAWN=0xff57d9ee`, `DUSK=0xffc58bff`, `NEUTRAL=0xffc5cad1`; `DarkUi.teamColor(int team) -> int`; `DarkUi.teamMark(DrawContext g, int team, int x, int y)` draws a 7x7 glyph: Dawn = sun (filled disc with four rays), Dusk = crescent moon, neutral = hollow diamond; `DarkUi.teamText(DrawContext g, int team, String text, int x, int y)` = glyph then text in the team colour; `DarkUi.panel(DrawContext g, int x, int y, int w, int h)` = `frame` plus a 1 px brass top rule. Rule: team colour may only be drawn through `teamMark`/`teamText`/`teamColor` next to a glyph; enemy sightings on maps draw an "x" glyph, own markers a filled dot, allies a ring.

- [ ] **Step 1: Add failing standalone checks** (compile list gains `HintText`):

```java
java.util.function.Function<String,String> keys = id -> id.equals("menu") ? "K" : "Not bound";
check(HintText.expand("{key:menu} opens your war council", keys).equals("K opens your war council"), "token replaced");
check(HintText.expand("Map {key:map}", keys).equals("Map Not bound") && HintText.expand("{key:menu", keys).equals("{key:menu"), "unbound and malformed");
```
- [ ] **Step 2: Write `test-tools/standalone/test_ui_standard.py`** (exit 1 with file:snippet on any finding). Asserts over `src/main/java/gg/gravesandglory/*.java`:
  - team colour literals `0xff57d9ee`, `0xffc58bff`, `0xff70a8a4`, `0xff9e77ba`, `0xff6ddfe9` and the name `factionColor(` appear only in `DarkUi.java`;
  - no string literal matches `Press [A-Z]\b`, `\b[A-Z] opens`, `Right-click`, `right-click`, `Click:`, `"M /`, `cyan ally`, `Gold yours` outside `HintText` tokens;
  - in `GravesAndGloryClient.java`, `CommandMode.java`, `CampScreen.java`, `CampaignClient.java`, no ARGB literal `0x[0-9a-fA-F]{8}` except in an allowlist of the map terrain fill (`0x66303c39`, `0xff34473b`) and inside `FantasyButton` (button states, kept);
  - every `drawTextWithShadow(... factionColor` or `DarkUi.teamColor(` call site is inside a statement that also calls `teamMark` or is `teamText` itself.
- [ ] **Step 3: Run both; expect the standalone Java check to fail to compile and `python test-tools/standalone/test_ui_standard.py` to list current findings** (`M / BATTLEFIELD`, `Click: confirm`, the "R opens" notices, `factionColor` uses, raw HUD colours).
- [ ] **Step 4: Implement** `HintText`, `DarkUi` additions, and the sweep: score panel `teamText` for DAWN/DUSK; point status line and minimap point markers with `teamMark`; capture bars with the challenger glyph; map legend "● yours  ○ ally  x enemy" drawn as glyphs; HUD panels via `DarkUi.panel`; body text via `DarkUi.label`; report table team column via `teamMark` (Task 7 rows); camp hint from `hint("menu")` and `hint("use")`; command-mode hint from `hint("confirm")`/`hint("cancel")` and the mouse bindings; server strings `"{key:menu} opens your war council."`, `"Your war council is ready. Press {key:menu}."`.
- [ ] **Step 5: Run both static checks; expect `BattleMathCheck: 23 checks passed` and `test_ui_standard: clean`.**
- [ ] **Step 6: Write in-game suite `ui`:**
  - `p1-hints-rebound`: bind `menu` to K and `map` to unbound (`setBoundKey`, `KeyBinding.updateKeysByCode()`); `keyHints()` contains `K`; `campHint()` contains `K`; the camp welcome notice rendered by the client contains `K`; the HUD map card label does not contain `M /`; restore defaults after.
  - `p1-hud-glyphs`: screenshot `ui-battle-hud` in a running skirmish with one point per team owned and one neutral; screenshot `ui-map` with the M map open; screenshot `ui-camp-home` and `ui-report`.
  - Greyscale review (tester, not code): convert `p1-ui-battle-hud.png` with `ffmpeg -i p1-ui-battle-hud.png -vf hue=s=0 p1-ui-battle-hud-grey.png` and state in words whether every point's owner and both scores can be told apart by glyph alone.
- [ ] **Step 7: Compile; run suite `ui`.** Expected `pass p1-hints-rebound`, `pass p1-hud-glyphs`, `pass p1-ui-suite`; the four screenshots show sun/moon glyphs beside every Dawn/Dusk colour and no default-key text; the greyscale copy stays readable.
- [ ] **Step 8: Commit** `"Phase 1: charcoal/brass HUD and camp sweep, team glyphs, key-bound hints (F-21)"`.

---

### Task 9: Regression and Kort's playtest gate

**Files:**
- Create: `docs/PHASE-1-PLAYTEST.md` (checklist Kort reads before his three skirmishes)
- Modify: `docs/PROJECT-STATE.md` (sections 5 and 6: F-09, F-14, F-15, F-17, F-21 status with evidence paths; F-24 unchanged)
- Modify: `gradle.properties` (`mod_version=0.4.0`, spec section 10: 0.4.x is the solo rework); keep `melty.json` `components[].fileName` aligned if `melty.json` still exists after Phase 0

**Interfaces:**
- Consumes: every suite and check id above.

- [ ] **Step 1: Full regression, one launch:** `GRAVES_PHASE1_CHECKS=combat,morale,reinforce,bots,tutorial,report,ui` (suites run in order, each starting from `campaign.returnLobby()`). Expected: every `p1-*` id above passes and each `p1-<suite>-suite` line is present.
- [ ] **Step 2: Existing suites:** `-PcampaignChecks` without `GRAVES_PHASE1_CHECKS` ends with `pass roadmap-suite`; `-PselfTest` passes every id except the ones `PROJECT-STATE.md` lists as stale (`troops-commanded`, `hill-navigation`) unless Phase 0 already repaired them, in which case all pass.
- [ ] **Step 3: Standalone and packaging checks:** the standalone Java check, `python test-tools/standalone/test_ui_standard.py`, the existing standalone checks from `HOW-TO-EDIT.md` section 9, then `"$G" build --offline -x test` and `python tools/package.py` (leak check passes: no `Phase1Checks`, `BotSimulation` or `BattleMathCheck` in the player jar). Packaging writes to `dist/` locally only; nothing is uploaded.
- [ ] **Step 4: Write `docs/PHASE-1-PLAYTEST.md`.** One page. Setup line (install the 0.4.0 build, fresh camp is fine). Then three skirmishes, each with what Kort should see and a yes/no box:
  - Skirmish 1, campaign battle 1 "The Training Field" on Easy: briefing card at the start; four lessons naming his own keys; arrows visibly flying and coloured puffs where they land; his group bar flashing when his troops take hits; at least one enemy group breaking and running with a horn; recruits marching out of the centre crossing once he holds it, not from base; the report screen after the battle with his kills, losses, gold and time on points.
  - Skirmish 2, solo ready pad 3v3 on Normal, his map vote: his own line routing at least once when outnumbered or hit from behind, regrouping with the rally horn, and his orders given during the rout taking effect after; bots that come back with paid replacements; the match readable from the HUD by glyphs, not only colour.
  - Skirmish 3, solo pad 3v3 on Hard on Kingsfield Crossroads: Hard bots taking an undefended flank point; Dawn Hard bots raising a shieldwall against his archers; bot charges arriving close, not from across the map; the report naming an MVP.
  - Closing questions (free text): "Did it feel good?", "Which fight was hardest to read?", "Anything that felt unfair?". Phase gate passes only when Kort says it feels good; his notes become Phase 1 fixes or `PARKED.md` lines.
  - Appendix: the four `[botsim]` summary lines and the `reinforce-walk` before/after times, so he sees the numbers behind "Hard is harder" and "the walk is shorter".
- [ ] **Step 5: Update `docs/PROJECT-STATE.md`** sections 5 and 6 with what is now verified in game and where the evidence is (test root paths, screenshot names).
- [ ] **Step 6: Commit** `"Phase 1: regression, 0.4.0 version, playtest gate checklist"`. Do not merge or push; hand the branch and `docs/PHASE-1-PLAYTEST.md` to the orchestrator for Kort's gate.

---

## Self-review notes

- Spec coverage: projectiles and impact flashes (Task 1); morale, rout, horns (Task 2); forward reinforcement and walk measurement (Task 3); three bot tiers with the three Hard behaviours (Task 4); bot-vs-bot data (Task 5); tutorial verified and reframed (Task 6); report with kills, objective time, gold, troops lost (Task 7); HUD/camp cleanup, glyphs, key hints (Task 8); Kort's gate (Task 9). Deferred with reason: House trait uptime in the report (Houses are Phase 2), shard bonus objective for battle 1 (shards are Phase 2/4), counters on troop cards (spec section 6 lists it, roadmap places it in Phase 2's "House traits and counters on the field").
- Check count in `BattleMathCheck` runs 5, 10, 13, 18, 20, 21, 23 across Tasks 1, 2, 3, 4, 5, 7, 8.
- Names used across tasks: `Projectiles.fire/tick/clear/inFlight`, `Unit.lastHitBy`, `BattleEffects.flash/horn`, `Morale.tick/routing/rally/GUARDS`, `Battle.forwardReinforcement/reinforcementAnchors/deployPoint/deployNote/roundSeconds/pointSeconds`, `Capture.ownedSince`, `Campaign.difficulty/tier/botDecide/lastWinner/activeBattle`, `Squad.tier/stats/routedAt/steadyUntil/fullHealth`, `MatchStats.SquadStats/winRate/mvp`, `HintText.expand`, `DarkUi.teamMark/teamText/teamColor/panel`.
