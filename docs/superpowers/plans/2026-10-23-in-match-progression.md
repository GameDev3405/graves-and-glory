> Note (2026-11-07): model and effort routing in this plan is superseded by docs/WORKFLOW.md.

# In-Match Progression (Levels and Bought Gear) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Soldiers level from kills during a match (level 1 when trained or rebought, level 2 at 3 kill credits, level 3 at 8). Kill credit goes to the killer, his helpers and their healers, and a victim counts its own level. Each level gives +5% or +10% health and damage, opens the next gear tier and changes the troop's ability: level 2 makes its own ability stronger, level 3 adds a second one. Each army buys Weapon II and III and Armour II and III per troop type with battle gold, in a new Armoury tab. Every tier has its own look, rank chevrons show over veterans of both teams, and the Iron Ledger cards and event feed report veterans. Between-match evolutions are gone. Nothing from levels or gear carries to the next match.

**Architecture:** The rules are pure classes with no Minecraft imports, proved by a javac-only check:
- `LevelMath` covers thresholds, the bounty, multipliers, worn tiers, buy states, prices and the bots' buy order.
- `CreditLog` holds the 10-second assist and support logs.

Server code goes through a few classes:
- one facade, `Veterans` (kill credit, level ups, gear, the Armoury state, bot buys);
- one status engine, `Statuses` (weaken, bleed, mark, single-target stun, cleanse);
- one sync channel, `RankSync` (`rank_v1`: level, weapon tier and armour tier per soldier).

Damage keeps one path. `Combat.resolve` multiplies the soldier's level and weapon factor onto the blow's base, beside stars, and reads `weaken` and `mark` as multipliers. The troop renderer reads `RankSync` to pick the armour skin, the weapon model and the chevrons.

**Tech Stack:** Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Yarn 1.20.1+build.10, Loom 1.7.4, Java 17, Gradle 8.8 (offline), Python 3.12 (Pillow for the art tools).

**Spec:** `docs/superpowers/specs/2026-10-22-in-match-progression-design.md` (approved by Kort, round 26). It is binding in all sections, and section numbers below ("design 3.2") refer to it. Context:
- `docs/superpowers/plans/2026-10-17-phase-2-houses.md`: this plan uses its names, sheets and suites. Its Tasks 11 to 13 and art steps already took the design's section 11.3 edits.
- `docs/superpowers/plans/2026-10-18-ui-plan.md`: Tasks 2, 3 and 8c and the closing Task 9.
- `docs/superpowers/specs/2026-10-21-art-pipeline-v2-design.md` and `2026-10-21-troop-depth-design.md`.
- `docs/HANDOFF.md`.

**Starts when:** Phase 2 Task 13 has committed on `phase-2-houses` (one writer per worktree; Task 13 is editing `Phase2Checks.java`, `CampScreen.java` and `tools/preflight.py` now). Phase 2 Tasks 10 to 12 are complete. This plan finishes before the 0.5.0 base merge, except Task 12, which runs on the merged base (design 11.4). 0.5.0 ships only after this plan **and** after every troop has its own weapon I to III and armour I to III art (design 4.4). That art comes from Phase 2's art steps, not from this plan.

## What exists today (phase-2-houses, efff4cc)

- **Built:** stars (Common and Rare `starPct2`/`starPct3` already 0.06/0.12), shards, Renown and Mastery (`mastery_1.reward` is already `title:Veteran`). There are the veteran trim overlays (`<troop>_veteran.png`, accepted at 8.5 for the six Gilded troops), and the troop page shows level names and three ability lines from `troopNotes.level2`/`level3` text. No evolve command or screen was ever built.
- **Still on the evolution model, removed by Task 1:**
  - the `evolutions` sheet (40 rows with gold, shard, percent and mastery columns);
  - `Profile.Owned.stage`, `Profile.lastStage` and the sanitizer's stage clamp;
  - `Campaign.progression` (star plus stage), `Campaign.stage`, `Unit.stage`;
  - `ArmyMobs.stageName`/`named`, `ArmyCards.portrait(spec, stage)`, the `CampScreen` recruit card's stage;
  - `TroopModels.resolve`, which reads the stage from the custom name and from `Spec.EVOLUTIONS`;
  - the stage spawns in `Phase2Checks` (`p2-gilded-stages` and others).
- **Kill credit today:** one `Unit.lastHitBy` squad, `lastHitAt` and `lastHitTroop`, set in `Combat.resolve` and in `Battle`'s damage handler (`GravesAndGlory.java` near line 996). They are read in `AFTER_DEATH` (near line 113) for gold, `stats.kills` and `stats.troopKills` (Mastery).
- **Networking:**
  - `state_v8` (written in `Battle.sendState`, read in `GravesAndGloryClient`);
  - `stance_v1` (sent per troop on START_TRACKING, the pattern `rank_v1` copies);
  - commands go over `order_v4` to `Battle.command`, which routes `train:`, `disband:` and `cancel-training` to `manage`.
- **Bots:** they buy in `Campaign.tick` every 100 ticks (`economy`) at `Commander.cost`, and their tier rows are `Spec.BOTCOMMAND`.

## File map

| File | Task | Role |
|---|---|---|
| `sheets/design.json`, `tools/preflight.py` (and generated `Spec.java`) | 1, 11 | the new sheets and rules; tuning in 11 only |
| `src/main/java/gg/gravesandglory/LevelMath.java` (new, pure) | 2 | thresholds, bounty, multipliers, worn tiers, buy states, prices, bot buy order, level names |
| `src/main/java/gg/gravesandglory/CreditLog.java` (new, pure) | 2 | assist and support logs, the credited set |
| `test-tools/standalone/LevelMathCheck.java` (new) | 2 | standalone checks `lm-*` |
| `src/main/java/gg/gravesandglory/Veterans.java` (new) | 3, 4, 5 | the server facade |
| `src/main/java/gg/gravesandglory/Statuses.java` (new) | 3, 6 | the status engine |
| `src/main/java/gg/gravesandglory/RankSync.java` (new) | 3 | `rank_v1`, server send and client store |
| `src/main/java/gg/gravesandglory/ArmouryTab.java` (new, client) | 8 | the Armoury tab |
| `src/main/java/gg/gravesandglory/TroopChevrons.java` (new, client) | 9 | the chevron billboard |
| `GravesAndGlory.java` | 1, 3, 4, 5 | Unit fields, spawn, death credit, state packet, `gear:` routing |
| `Campaign.java`, `Profile.java`, `RosterMath.java`, `ArmyMobs.java`, `RecruitPages.java` | 1 (Campaign also 5) | evolution teardown; bot gear buys |
| `MatchStats.java`, `CampScreen.java` | 1, 5 | the report `levels` block and its rows |
| `Abilities.java`, `Combat.java`, `CounterMath.java`, `Houses.java`, `Commander.java`, `HouseAttacks.java` | 1 and 3 (small), 6, 7 | per-level rows, new mechanics, the damage factor |
| `RecruitmentScreen.java`, `GravesAndGloryClient.java` | 3, 8 | the Armoury tab and key; the state read |
| `TroopModels.java`, `TroopModel.java`, `TroopModelLoader.java`, `TroopRenderer.java`, `TroopFeatures.java`, `ArmyCards.java` | 1 (small), 9 | tier switching, stopgap, chevrons, portraits |
| `tools/build_skins.py`, `test-tools/standalone/test_build_skins.py`, `tools/package.py`, `tools/release_config.py` | 1, 9 | the `weaponTiers` part key, the stopgap report, the release refusal |
| `test-tools/java/gg/gravesandglory/LevelChecks.java` (new) | 3, 4, 5, 8 | suites `levels`, `gear`, `armoury` |
| `test-tools/java/gg/gravesandglory/AbilityLevelChecks.java` (new) | 3, 6, 7 | suites `statuses`, `levels-<house>` |
| `test-tools/java/gg/gravesandglory/RankChecks.java` (new) | 3, 9 | suite `ranks` |
| `test-tools/java/gg/gravesandglory/Phase2Checks.java` | 1, 3, 9 | the stage checks become armour-tier checks; the suite registry |
| `test-tools/java/gg/gravesandglory/BotSimulation.java`, `tools/progression_gate.py` (new) | 5, 11 | sim flags and logs; gate maths |
| ui-plan code: `BattleHud.java`, `UiMath.java`, `CommandMode.java`, `FeedLines.java`, `FeedEvents.java`, `EventFeed.java`, `tools/build_ui_art.py`, `tools/ui_art/`, `Phase1Checks.java`, `UiMathCheck.java`, `test_ui_standard.py` | 12 | pips, glint, the level-3 feed line |
| `docs/PHASE-2-BALANCE.md`, `docs/PHASE-2-PLAYTEST.md`, `HOW-TO-EDIT.md`, `docs/PARKED.md` | 9, 11, 13 | logs and docs |

## Order and parallelism

```
Task 1 data and teardown -> Task 2 pure rules -> Task 3 seams
   Lane A (bannerbound-p2, phase-2-houses):  Task 4 credit and levels -> Task 5 gear and bots -> Task 8 Armoury tab
   Lane B (bannerbound-p2-abil, progression-abilities):  Task 6 statuses and new mechanics -> Task 7 the 60 level rows
   Lane C (bannerbound-p2-rank, progression-ranks):  Task 9 tiers, stopgap, chevrons, release check
-> Task 10 join and regression -> Task 11 balance gate (headless)
-> [base merge, Kort's call] -> Task 12 HUD pips, glint, feed line (merged base) -> Task 13 regression and Kort's gate
```

- **Strictly sequential:** Tasks 1 to 3, because each changes `sheets/design.json`, `Spec.java` or the seams every lane builds on. Tasks 10 to 13 are sequential too.
- **Three lanes in parallel after Task 3, in separate worktrees, with no file in common.** Task 3 lays the seams that make this true:
  - the `Unit` fields;
  - `Veterans`' hook methods and `Combat`'s factor line;
  - the credit hook calls in `Abilities`;
  - the `Statuses.tick`/`clear` calls;
  - `RankSync` both ends;
  - the `state_v9` layout;
  - the suite registry lines and three harness skeletons.

  The ownership below is a hard rule for the lanes. A lane that needs another lane's file stops and reports. The edit then waits for Task 10.

  | Lane | Owns |
  |---|---|
  | A | `GravesAndGlory.java`, `Campaign.java`, `Veterans.java`, `MatchStats.java`, `CampScreen.java`, `RecruitmentScreen.java`, `ArmouryTab.java`, `GravesAndGloryClient.java`, `BotSimulation.java`, `LevelChecks.java` |
  | B | `Abilities.java`, `Combat.java`, `CounterMath.java`, `Houses.java`, `Commander.java`, `HouseAttacks.java`, `Statuses.java`, `test-tools/standalone/HousesCheck.java`, `AbilityLevelChecks.java` |
  | C | `TroopModels.java`, `TroopModel.java`, `TroopModelLoader.java`, `TroopRenderer.java`, `TroopFeatures.java`, `TroopChevrons.java`, `ArmyCards.java`, `tools/build_skins.py`, `test_build_skins.py`, `tools/package.py`, `RankChecks.java`, `Phase2Checks.java` |

  `sheets/design.json`, `Spec.java`, `tools/preflight.py`, `LevelMath.java`, `CreditLog.java` and `RankSync.java` are frozen during the lanes. A lane that finds a sheet value wrong records it for Task 10 or 11 and does not edit it.
- New worktrees: `git worktree add ../bannerbound-p2-abil -b progression-abilities <Task 3 head>` (and `-rank`, `progression-ranks`). Copy the git-ignored pinned dependency jars and the latest run-root launch pattern from `bannerbound-p2`, as `bannerbound-gate9` did. Task 10 merges both branches into `phase-2-houses` locally, as `phase-2-models` was merged. Nothing is pushed.
- **Clients:** each lane uses at most one harness client at a time. All three lanes together use three of the four allowed across every worktree (HANDOFF), so check what the UI and Phase 3 sessions hold before launching. Never compile in a worktree while its client runs. Task 11's bot sims are headless and do not count.
- **Not parallel:** Task 12 and UI plan Task 9 (they share `BattleHud`, `FeedLines`, `FeedEvents`, `EventFeed`, `UiMath` and `Phase1Checks`). Run them back to back by one agent, as UI Task 9 allows ("the two closing tasks may be one"), with separate commits.

## Changes Kort decides (saves and multiplayer protocol)

Each of these is listed for Kort (CLAUDE.md: changes that break saves or the multiplayer protocol are his decision). Question 2 of round 34 asks him to confirm them.
1. **Saves (Task 1):**
   - `Profile.Owned.stage` is no longer written, and the sanitizer ignores a stored `stage` field (design 11.1, approved).
   - No profile loses data, since no player ever evolved, and there is no migration version bump.
   - A 0.5.0 profile read by 0.5.0-preview1 gives stage 1 everywhere, which is what every profile holds today.
2. **Match report file (Task 5):** the report JSON gains a `levels` block. This is additive: `CampScreen` reads older reports without it.
3. **Protocol (Task 3):**
   - `state_v8` becomes `state_v9`. It gains, per group, the living count at level 2 and at level 3 and an `armouryOpen` bit; and, per army, the Armoury block: `armouryHere`, then per banner troop type the troop id and four buy-state bytes.
   - A new channel, `rank_v1` (entity id, level, weapon tier, armour tier), is sent on START_TRACKING and on every change, like `stance_v1`.
   - A new command string, `gear:<troop>:<weapon|armour>:<tier>`, goes over the existing `order_v4` (no layout change).
   - Host and clients must run the same build, and a 0.5.0-preview1 client cannot join a build from this plan.
   - At the base merge, the UI branch's `state_v7` reader takes the `state_v9` layout. That is a merge note for the base-merge agent.

## Global Constraints

- Minecraft 1.20.1 and Fabric only; no new dependencies.
- Never hand-edit `Spec.java`. Change `sheets/design.json`, then run `python tools/preflight.py`, which must print `Preflight clean`.
  - Every row of a sheet has the same columns, and every cell is filled.
  - A column is `double` only if the first row's value is written with a decimal point. The `abilities` sheet's first row is `none`, so new double columns are written `0.0` there.
- Every number of design sections 3, 4, 6 and 9 lives in a sheet. The only code constants are fixed effect kinds (Slowness I and II) and presentation timings: the flash length, and the chevron's size and lift.
- The counter table stays the one place multipliers meet (design 2):
  - `weaken`, `mark`, `thickhide`, the new conditions and the level-2 stance and guard rows are counter rows or factors inside `Combat.resolve`.
  - The level-and-weapon factor multiplies the blow's base, as stars do (`Abilities.baseHit`), so it sits outside the [0.25, 2.5] clamp, and `HitLog` records it.
- Progression cap: stars alone stay within +12% (`recruitment.capPct`). Levels and gear are battle effects outside the cap, like House traits (design 2).
- Player-facing text never says "stage", "evolve" or "Writ" (it says "Pact"); internal ids (`troops.evolveLine`, `writ*`) keep their names. Preflight enforces this on sheet text from Task 1.
- Art and screens: nothing under 9 passes on `work/ui-concepts/GRADING-RUBRIC.md`. Weapons pass at 8.5, then Kort sees the weapon sheet (round 30). No brass fittings on table wood. The `ui-ux-pro-max` skill is used only for its `--domain ux` checks, never for style, palette or font.
- Harness lives only in `test-tools/`. Add `LevelMathCheck`, `LevelChecks`, `AbilityLevelChecks` and `RankChecks` to the `forbidden` tuple in `tools/release_config.py` (Task 1).
- The working copy is CRLF. Edit dense one-line Java files in place, and never reformat or re-save a whole file.
- Client rules (HANDOFF):
  - at most four harness clients at once across all worktrees, one per agent, 30 minutes each;
  - never compile while a client runs;
  - run the "Kort is playing" process check before each launch;
  - run roots go under `work/p2/` with `options.txt` seeded, copying the latest run root's launch pattern;
  - always pass `--offline` and `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"`.
- Commit by path with `git -c user.name=Claude -c user.email=noreply@anthropic.com commit -F <msg file under work/p1/>`. Subjects start `In-match progression Task <n>:`, and the trailer names the model that did the work. Never push, merge to main, publish or use `mcp__melty__*`.
- Run `GRAPHIFY_BACKEND=none graphify update .` before each task's commit. Every brief carries the CLAUDE.md graphify rule: start a code question with `graphify query`, then verify in the source.
- Executors follow HANDOFF's token rules, as named on each task:
  - Opus at `xhigh` or `high` for combat, design, the model pipeline and large reviews;
  - Sonnet for mechanical builds, suite runs, testers, doc edits, re-reviews and small-diff reviews;
  - `medium` effort for purely mechanical agents;
  - never `max`, never Haiku.
- Process: as in HANDOFF.
  - Each brief, report, review package and the ledger live in `.superpowers/sdd/2026-10-23-in-match-progression/`.
  - Each task gets an Opus review of `review-A..B.diff`. Fix rounds resume the implementer, and a scoped Sonnet re-review follows.
  - Two fix rounds at most, then the controller rules.
  - Every decision goes into `progress.md` as `Ruling: what — why — cost if wrong`.
- Deferred ideas go to `docs/PARKED.md` (append only). Open calls go to `docs/QUESTIONS-FOR-KORT.md` (append only), with the recommended option first.

## Review Focus

1. **Credit edges.** These cases are tested by `p2-credit-*` and `p2-level-*` (Task 4):
   - a shot lands after its archer died;
   - a kill comes while the killer's helper is already dead;
   - a Rise Again blow (not a kill, ruled 2026-10-21);
   - a temporary Risen kills, or is killed;
   - an allied army's helper;
   - the commander lands the killing blow;
   - a structure is destroyed;
   - 9 credits arrive at once, so a soldier goes from level 1 to 3 in one blow.

   Mastery still counts killing blows per type only (`p2-mastery-unchanged`).
2. **Health on level up and armour wear.** Only the added health is healed. Stars multiply on top, and a fully geared level-3 soldier is 1.3 x 1.3 = 1.69 of a fresh one (`p2-gear-stats`). A risen Graveborn keeps level, credits and gear (`p2-rise-keeps-level`). A rebought soldier starts at level 1 and wears tier I (`p2-rebuy-level-1`, `p2-gear-rebuy-tier1`).
3. **Round boundaries.** At `startRound`, `returnLobby` and `switchMap`, these are cleared:
   - every `CreditLog` entry;
   - every status;
   - every Armoury opening and buy;
   - every `RankSync` client entry;
   - the pending level-3 feed lines.

   Tests: `p2-gear-round-reset`, `p2-status-clear-round`, `p2-rank-clear`.
4. **The sync.** A client that joins a battle in progress gets every tracked soldier's rank before it draws one (`p2-rank-late-join`, the `stance_v1` late-registration path). Portraits never read a stale rank.
5. **No side paths.** Every new damage multiplier shows in `Combat.HitLog`. The ability on a troop card and in `Abilities` come from the same `levelAbilities` row (`p2-level-row-select`, `p2-card-level-lines`).

## Test runs used by every task

**Standalone pure checks (seconds, no game):**
```bash
cd /c/Users/kortt/Melty/bannerbound-p2 && python tools/preflight.py
J="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot/bin"; S=src/main/java/gg/gravesandglory
"$J/javac" --release 17 -d build/progression-checks $S/{Spec,BattleMath,MatchStats,HintText,Progression,Profile,ProfileMigration,RosterMath,CounterMath,LevelMath,CreditLog}.java \
  test-tools/standalone/{BattleMathCheck,HousesCheck,ProfileMigrationCheck,LevelMathCheck}.java \
  && for c in BattleMathCheck HousesCheck ProfileMigrationCheck LevelMathCheck; do "$J/java" -cp build/progression-checks gg.gravesandglory.$c || exit 1; done
```
Pass output: `<Check>: <n> checks passed` for each. Also `python test-tools/standalone/test_build_skins.py` and `python tools/build_skins.py --verify` whenever a task touches art tools or models.

**Compile both source sets:** `export JAVA_HOME=...; G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle | head -1); "$G" compileJava compileLocalTestTools --offline`. Before a commit, also run `"$G" build --offline -x test`.

**In-game suite `<suite>`:** as in the Phase 2 plan, with `GRAVES_PHASE2_CHECKS=<suite>` and a fresh root under `work/p2/`. The new suites register in `Phase2Checks.SUITES` and print through its helpers, so the same greps apply:
```bash
grep -E "^(pass|fail) p2-" "$GRAVES_ROADMAP_TEST_ROOT/evidence/local-checks.txt"; grep "\[p2\]" "$GRAVES_ROADMAP_TEST_ROOT/run/logs/latest.log"; ls "$GRAVES_ROADMAP_TEST_ROOT/run/screenshots"
```
A suite passes when:
- every `p2-*` id of the suite is `pass`;
- `pass p2-<suite>-suite` is present;
- the named screenshots exist;
- the tester states in one sentence what each screenshot shows.

**Bot run (`<a>` vs `<b>`, progression on or off):** headless, two processes of 10 matches each, one per seat, maps 0 to 4 twice:
```bash
"$G" runServer --offline -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=900 \
  -PsimProgression=<on|off> -PsimBanner0=<a> -PsimBanner1=<b> --init-script test-tools/isolated-server.gradle
```
- `-PsimProgression` (Task 5) overrides `progression.enabled` for the sim. `results.csv` gains `score300_0`, `score300_1`, and `levels.csv` is written beside it (Task 5).
- `-PsimFast=true` and the round robin (`tools/balance_method.py`, `tools/balance_sim.sh`) apply only once those tools are on `phase-2-houses` (they are uncommitted in `bannerbound-method` today); Task 11 depends on Kort's answer to round 34 question 1.

---

### Task 1: Sheets, preflight rules and the evolution teardown

**Executor:** Sonnet high (mechanical, wide). **Needs a client:** yes, for the regression of the touched suites in Step 7. Everything else is static.

This task changes all the data up front, so no lane edits a sheet. It also removes `Spec.EVOLUTIONS` and keeps every caller compiling at level 1: levels exist after it, but nothing raises them yet.

**Files:**
- Modify `sheets/design.json`:
  - **`progression`** (new, one row): `id main, enabled true, assistSeconds 10, commanderBounty 3, chevronRange 24.0, feedLevel 3`.
  - **`levels`** (new, three rows): `id, level, kills, healthPct, damagePct`: `l1 1 0 0.0 0.0`, `l2 2 3 0.05 0.05`, `l3 3 8 0.10 0.10`.
  - **`gearTiers`** (new, three rows): `id, tier, weaponPct, armourPct`: `t1 1 0.0 0.0`, `t2 2 0.10 0.10`, `t3 3 0.20 0.20`.
  - **`rarities`**: new int columns `gearPrice2`, `gearPrice3`: common 40/80, rare 60/120, epic 80/160, legendary 100/200. The star columns are already 0.06/0.12.
  - **`evolutions` becomes `levelNames`**: columns `id, troop, level, name`, the same 40 rows (ids `<troop>_<level>`), with the gold, shard, percent and mastery columns gone. `troops.evolveLine` keeps its name.
  - **`abilities`**:
    - New columns: `status` (String: `none`, `slow`, `weaken`, `bleed`, `knockback`, `stun`), `taken` (double, N11), `cleanse` (boolean, N13), `rangedPenalty` (boolean, default true, N12) and `tags` (String, `|`-joined counter tags the row grants, default `none`).
    - The mechanics `onhit_slow` become `onhit_status` with status `slow` (frostshot, ensnare, brine, harpoon).
    - 60 new rows, `<troop>_l2` and `<troop>_l3`, one per troop per level, with the values of design 6.2. A level-2 value is today's row plus the design's step ("if the base row is retuned, the step is kept"): fearless today is 0.20/0.30, so its level-2 row is 0.30/0.40; dread today has no `amount2` (Task 8b removed it), so its level-2 row changes radius and amount only.
    - The `text` of each row is the design's ability sentence, at most 90 characters.
    - The mechanic of each level-2 row equals its base's mechanic.
    - Level-3 rows use existing mechanics (`pierce`, `cleave`, `pounce`, `aura`, `stance`, `onhit_status`) or the new ones: `onkill_heal`, `raise`, `rise`, `mark`.
  - **`levelAbilities`** (new, 60 rows): `id, troop, level, ability`.
  - **`counters`**: the new rows below, and the conditions `target_slowed_or_wounded` (design N9) and `target_wounded` (the Templar's *Unbroken*, which needs the wounded half alone; see Spec gaps 3).

    | id | attackerTag | targetTag | condition | multiplier | replaces | for |
    |---|---|---|---|---|---|---|
    | C1g | arrow | guard2 | raised_front | 0.30 | C1 | level-2 *Better guard* (Squire, Templar, Hearthguard) |
    | C3g | melee | guard2 | raised_front | 0.5 | C3 | the same |
    | C15b | bodkin | shield | raised_front | 0.6 | C1 | Order Archer level 3 |
    | C15v | armour_bolt2 | shield | raised_front | 0.85 | C1\|C15 | Arbalist level 2 |
    | C14v | sunder2 | shield | always | 1.6 | C1\|C2\|C3\|C14 | Shieldbreaker level 2 |
    | C18a2 | steady2 | any | hold | 1.3 | C18a | Order Archer level 2 |
    | C18b2 | ambush2 | any | no_sky | 1.35 | C18b | Hide Hunter level 2 |
    | C18c2 | brace2 | charging | hold | 1.5 | C18c | Tusk Spear and Trident Guard level 2 |
    | C18d | bracewall | charging | hold | 1.3 | none | Hearthguard level 3 |
    | C19 | shot | thickhide | always | 0.75 | none | Old Horn and Brute level 3 (design N10) |
    | C20 | rundown | any | target_slowed_or_wounded | 1.3 | none | Moot Hound level 3 |
    | C21 | any | unbroken | target_wounded | 0.8 | none | Templar level 3 |

  - **`botCommand`**: new columns `gearMaxTier` (easy 2, normal 3, hard 3), `gearReserveTroops` (8, 5, 2) and `gearPriority` (`banner`, `role`, `open`).
  - **`troopNotes`**: drop `level2` and `level3`. Card ability lines come from the `levelAbilities` rows' `abilities.text`, so the card and the game read one row.
- Modify `tools/preflight.py`:
  - drop the evolution rules;
  - every Common and Rare has `levelNames` rows for levels 2 and 3;
  - every troop has exactly one level-2 and one level-3 `levelAbilities` row;
  - a level-2 ability has its base ability's mechanic;
  - `levels.kills` rise strictly from 0;
  - the percentages do not fall;
  - gear prices are positive, with tier III above tier II;
  - `starPct3 <= capPct`;
  - every `abilities.tags` tag appears in some counter row;
  - no `text`, `name` or `troopNotes` cell contains "stage", "evolve" or "Writ" (case-insensitive);
  - `progression.feedLevel` is 2 or 3.
- Modify `Profile.java`: drop `Owned.stage` and `lastStage`. The sanitizer ignores a stored `stage` field.
- Modify `RosterMath.java`: `progressionMultiplier(starPct, cap)`, stars only.
- Modify `Campaign.java`: `progression` reads stars only; drop `stage()`.
- Modify `GravesAndGlory.java`:
  - `Unit.stage` becomes `int level=1, credits=0, weaponTier=1, armourTier=1`;
  - `spawn` names by `ArmyMobs.levelName(spec,1)` (plus the veteran suffix as today).
- Modify `ArmyMobs.java`: `stageName` becomes `levelName(TroopsRow,int level)`, reading `levelNames`; `named` does the reverse lookup over level names.
- Modify `ArmyCards.java`: `portrait(spec, int weaponTier, int armourTier)` (the Task 9 API). Until Task 9 it maps `armourTier` to today's stage skin and ignores `weaponTier`.
- Modify `CampScreen.java`: the recruit `Card` loses `stage`, and its title is the level-1 name.
- Modify `RecruitPages.java`: `levelNames(t)` reads `levelNames`; `abilityLines(t)` reads the base row plus the two `levelAbilities` rows' text.
- Modify `TroopModels.java`: `resolve` maps a level name to its level and uses it as the armour tier. This is a stopgap until Task 9 reads `RankSync`.
- Modify `Abilities.java`: `case "onhit_slow"` becomes `case "onhit_status"`, with status `slow` behaving exactly as before.
- Modify `test-tools/standalone/{HousesCheck,ProfileMigrationCheck}.java`: the evolution assertions become `levelNames`/`levelAbilities` assertions, and a stored stage is ignored.
- Modify `test-tools/java/gg/gravesandglory/Phase2Checks.java`: the stage spawns and `p2-gilded-stages` set the level name on the spawned troop instead of a profile stage. Task 9 makes them armour-tier checks.
- Modify `tools/release_config.py` (forbidden names).

**Interfaces:**
- Produces: `Spec.PROGRESSION`, `LEVELS`, `GEARTIERS`, `LEVELNAMES`, `LEVELABILITIES`; the new `abilities`, `counters`, `rarities` and `botCommand` columns; `ArmyMobs.levelName`; `ArmyCards.portrait(spec,w,a)`; the `Unit` fields `level`, `credits`, `weaponTier`, `armourTier`.
- Consumes: design sections 6.2, 8 and 11.1.

- [ ] **Step 1: Add the failing assertions.**
  - `HousesCheck`: the five new sheets have their shapes; `levelNames` has 40 rows; `levelAbilities` has 60 rows, two per troop; every level-2 mechanic matches its base; C19 and C1g exist; no `onhit_slow` mechanic is left; `knight` level 3 is named "Knight-Captain"; the `gilded_normal` banner's full gear cost is 1,680 (design 4.3).
  - `ProfileMigrationCheck`: a profile JSON holding `"stage":3` loads, and saving it writes no `stage`.
- [ ] **Step 2: Run the standalone checks; they fail** (no such sheets).
- [ ] **Step 3: Edit the sheets and preflight; run `python tools/preflight.py` until `Preflight clean`.** Then run the deliberate-failure check: break one level-2 mechanic and one gear price in a scratch copy, confirm preflight names both, and restore.
- [ ] **Step 4: Apply the teardown edits above; compile both source sets.**
- [ ] **Step 5: Run the standalone checks; all pass.**
- [ ] **Step 6: Grep check:** `grep -rn "EVOLUTIONS\|stageName\|\.stage\b" src/main/java` finds nothing outside comments, and `grep -rin "evolve\|stage " src/main/resources/assets/gravesandglory/lang` finds no player text.
- [ ] **Step 7: Regression of what the teardown touched (client):** suites `gilded,gilded-art,chase,screens,migration` and `graveborn,tidecallers,wildmoot` (`onhit_status`); all pass. The tester confirms that `chase-troop-card` still shows three level names and three ability lines.
- [ ] **Step 8: Commit** `"In-match progression Task 1: progression sheets, level names, level ability rows, evolution teardown"`.

**Review:** Opus high. Review focus: every level-2 value equals today's row plus the design step; no player text says stage or evolve; `onhit_status` slow matches `onhit_slow` to the tick; the profile round trip.

---

### Task 2: Pure rules: LevelMath and CreditLog

**Executor:** Opus high (combat rules, pure). **Needs a client:** no; static only.

**Files:**
- Create `src/main/java/gg/gravesandglory/LevelMath.java` and `CreditLog.java` (pure, `Spec` only).
- Create `test-tools/standalone/LevelMathCheck.java`.

**Interfaces (Produces, pure):**
- `LevelMath.levelFor(int credits) -> int`: 1 to 3, from `Spec.LEVELS`.
- `LevelMath.bounty(int victimLevel, boolean commander, boolean temporary) -> int`: a commander counts `progression.commanderBounty`, and a temporary troop counts 1.
- `LevelMath.healthFactor(int level, int armourTier)` and `damageFactor(int level, int weaponTier)`: `1 + levelPct + tierPct`, both doubles.
- `LevelMath.worn(int level, int boughtTier) -> int`: `max(1, min(level, boughtTier))`.
- `enum Buy { LOCKED, OPEN, BOUGHT, UNAFFORDABLE }`.
- `LevelMath.buyState(int tier, int highestLevelSeen, int boughtTier, int gold, int price) -> Buy`. Tier II opens at a seen level of 2. Tier III opens at a seen level of 3 and needs tier II bought on that track.
- `LevelMath.refusal(int tier, int highestLevelSeen, int boughtTier, int gold, int price, boolean here) -> String`: "" when the buy is allowed, else the player copy.
  - "Needs a level-2 <name>"; the caller passes the name in an overload `refusal(..., String levelName)`.
  - "Buy Weapon II first", "Already bought", "Not enough gold".
  - "Buy at your barracks or a held point".
- `LevelMath.price(String rarity, int tier) -> int` from `rarities.gearPrice2/3`.
- `record Option(String troop, String track, int tier, int price)`.
- `LevelMath.botPicks(Spec.BotCommandRow row, List<TypeState> types, int gold, int cheapestTroopPrice) -> List<Option>`. `TypeState` is `(troop, group, rarity, highestLevelSeen, weaponBought, armourBought, livingAtLevel2Plus)`. The order follows design 9.1:
  - Easy: banner order.
  - Normal: ranged and siege take the weapon first, infantry and cavalry the armour first, support the weapon first.
  - Hard: every open buy, starting with the type with the most living soldiers at level 2 or higher.
  - Each pick keeps `gearReserveTroops x cheapestTroopPrice` gold, and none goes above `gearMaxTier`.
- `LevelMath.levelName(Spec.TroopsRow t, int level) -> String`: `ArmyMobs.levelName` delegates here. Epics and Legendaries keep their own name.
- `CreditLog`, with units as entity ids and times as battle ticks:
  - `hit(int victim, int helper, int helperTeam, long t)` and `support(int target, int supporter, int team, long t)`;
  - `credited(int victim, int killer, int killerTeam, long t, long window) -> Set<Integer>`: the killer (when he is a troop), every helper of the killer's team in the window, and every supporter of the killer or of one of those helpers in the window;
  - `trim(long t, long window)`, `forget(int unit)`, `clear()`.

- [ ] **Step 1: Write `LevelMathCheck` with failing ids:**
  - `lm-thresholds`: 0→1, 2→1, 3→2, 7→2, 8→3, 99→3.
  - `lm-carry`: 0 plus 9 credits gives level 3 and two level ups.
  - `lm-bounty`: level 1 gives 1, level 3 gives 3, a commander gives 3, a temporary troop gives 1.
  - `lm-factor`: level 3 with tier III is 1.30 per stat, and 1.69 in product.
  - `lm-worn`: a level-3 soldier with tier II bought wears II; a level-1 soldier with III bought wears I.
  - `lm-buy-states`: all four states; tier III is LOCKED without tier II, even at level 3; an opened buy stays open when the highest level seen stays at 2.
  - `lm-prices`: the table of design 4.3, and `gilded_normal`'s full cost of 1,680.
  - `lm-refusal`: each of the copy strings above. No copy says "stage" or "evolve".
  - `lm-bot-easy`, `lm-bot-normal`, `lm-bot-hard`: fixed fixtures for each difficulty, covering order, max tier and reserve.
  - `lm-credit-*`: killer only; a helper inside 10 s and one outside; a supporter of a helper; a supporter of nobody; an allied army's helper (same team, credited); an enemy-team "helper" (not credited); `forget` drops a dead helper's entries, but a kill inside his window still credits the living ones; `trim` drops old entries.
- [ ] **Step 2: Run; fail.**
- [ ] **Step 3: Implement both classes.**
- [ ] **Step 4: Run all standalone checks; all pass.** Record the counts.
- [ ] **Step 5: Commit** `"In-match progression Task 2: LevelMath and CreditLog pure rules"`.

**Review:** Opus high. Focus: the credit set rules of design 3.2 against the fixtures; the buy-state table; that `botPicks` never spends into the reserve.

---

### Task 3: Seams: the Veterans facade, the damage factor, credit hooks, state_v9, rank_v1, harness skeletons

> **Merge note (2026-10-23, integration trial):** on the merged base, ui-plan's per-group `fullCount` (int) and `strength` (float) sit right after `lastStand`. Write the new `level2`, `level3` and `armouryOpen` fields AFTER `strength` (or append them after the commander ability), never between `lastStand` and `fullCount`. The UI reader takes the same order.

**Executor:** Opus high (protocol and the damage path). **Needs a client:** yes, for Step 7's regression. Steps 1 to 6 are static.

The seams let the three lanes work on disjoint files. Behaviour after this task: the damage factor is 1.0 for every soldier at level 1 with tier I, the logs fill but nothing reads them, `rank_v1` carries 1/1/1, and the `state_v9` fields are zero.

**Files:**
- Create `Veterans.java`:
  - `helped(Battle, Unit helper, LivingEntity victim)` and `supported(Battle, Unit supporter, LivingEntity target)` write to `Battle.credit`, a new `CreditLog` field, and ignore temporary troops and players.
  - `damageFactor(Unit)` returns `LevelMath.damageFactor(u.level, u.weaponTier)`.
  - `outputFactor(Unit)` is the same, for heal amount and cloud duration.
  - `clear(Battle)`.
  - `armouryState(Squad)` returns an empty view until Task 5.
- Create `Statuses.java`: `tick(Battle)`, `clear(Battle)` and `factorOut(Battle, LivingEntity attacker)`, which returns 1.0 until Task 6.
- Create `RankSync.java`:
  - the channel `rank_v1`;
  - `changed(Battle, Unit)` sends to every tracking player;
  - the START_TRACKING send and late-registration resend copy `stance_v1`'s;
  - client side, `registerClient()` stores `Rank(level, weaponTier, armourTier)` by entity id and offers `RankSync.client(Entity) -> Rank` (null when unknown) and `setLocal(Entity, Rank)` for client-only portrait mobs; `clear()` empties it on disconnect and round reset.
- Modify `GravesAndGlory.java`:
  - add `Battle.credit`;
  - call `Veterans.clear`, `Statuses.clear` and `RankSync` clears at `startRound`/`returnLobby`/`switchMap`;
  - call `Statuses.tick(this)` in the battle tick;
  - the damage handler (near line 996) calls `Veterans.helped(b, striker, entity)` for every troop blow;
  - `STATE` becomes `state_v9`, with the reserved fields of "Changes Kort decides" item 3 written as zeros and empties;
  - register `RankSync` on both sides.
- Modify `GravesAndGloryClient.java`: read `state_v9` into `CommandMode`'s group records (`level2`, `level3`, `armouryOpen`) and `CampaignClient.armoury` (a new record), and call `RankSync.registerClient()`.
- Modify `Combat.java`, one factor:
  - `base * Veterans.damageFactor(attacker)` before the product;
  - `Statuses.factorOut(b, attacker.mob)` inside the product, beside the auras;
  - `HitLog` gains `veteran` and `status`.
- Modify `Abilities.java`, hook calls and two empty seams only:
  - `Veterans.helped` in `slow(...)`, `pull(...)` and `mistTick`, for each enemy held in a cloud;
  - `Veterans.supported` in `mend`, `aura` (the oath banner) and `howl`, for each own troop it reaches;
  - no-op `rowsChanged(Unit)` and `onKill(Battle, Unit)`, which lane A calls from Task 4 on and lane B fills in Task 6.
- Create `test-tools/java/gg/gravesandglory/{LevelChecks,AbilityLevelChecks,RankChecks}.java`. Each suite class (`levels`, `gear`, `armoury`, `statuses`, `levels-gilded` to `levels-tidecallers`, `ranks`) reports `fail p2-<suite>-not-built` until its task.
- Modify `Phase2Checks.java`: register the new suites in `SUITES` (one line).

**Interfaces:**
- Produces:
  - for lane A: `Veterans` (owned by lane A from here), `Battle.credit`, the `state_v9` writer and reader;
  - for lane B: `Statuses` (owned by lane B from here), the `Combat` factor line, the hook calls B must keep when it rewrites `slow`, `mend` and `mistTick`;
  - for lane C: `RankSync.client(Entity)`.
- `state_v9` layout, appended after the `state_v8` fields:
  - per group: `int level2, int level3, boolean armouryOpen`, written right after each group's `lastStand` boolean;
  - after the commander ability: `boolean armouryHere`, `byte n`, then per type `String troop, byte weapon2, byte weapon3, byte armour2, byte armour3`, each byte a `LevelMath.Buy` ordinal.

- [ ] **Step 1: Write `LevelMathCheck` additions:** `lm-state-roundtrip` writes and reads the `state_v9` Armoury block through a pure encoder in `LevelMath` (`encodeArmoury`/`decodeArmoury`), so the layout is tested without a game.
- [ ] **Step 2: Run; fail.**
- [ ] **Step 3: Implement the seams.**
- [ ] **Step 4: Compile; run the standalone checks; all pass.**
- [ ] **Step 5: `grep -n "state_v8" src`** finds only comments.
- [ ] **Step 6: Confirm** that a `Combat.HitLog` written in a standalone test or a log line holds `veteran == 1.0` and `status == 1.0`.
- [ ] **Step 7: Regression (client):** suites `banners,engine,commander,gilded,graveborn,ironclad,tidecallers,wildmoot` and Phase 1 `combat,reinforce,bots`. All pass, unchanged, since every factor is 1.0. The new suites' `not-built` fails are expected and recorded.
- [ ] **Step 8: Commit** `"In-match progression Task 3: veterans facade, damage factor, credit hooks, state_v9, rank_v1, harness seams"`. Then create the two lane worktrees from this commit.

**Review:** Opus high. Focus: the protocol layout (one place, documented in the packet comments); the late-join path; that the factor sits on the base, outside the clamp, and that `Statuses` sits inside; no behaviour change at level 1.

---

### Task 4 (lane A): Kill credit and levels

> **Review notes (tasks-1-3-review.md, 2026-10-23), binding for this task:**
> - `Veterans.onDeath` must run ABOVE `if(victim.temporary)continue;` in AFTER_DEATH (GravesAndGlory ~:121), or `p2-temporary-no-credit` ("a Risen killed counts 1") fails.
> - A player commander's death never reaches AFTER_DEATH (`allowDamage` ~:996-999 returns false and exits before the striker loop). `onCommanderDeath` must find the striker itself in that branch and forget the player's credit log there (the Reaver respawns in 9 s, under the 10 s window).
> - Find the killer through `b.units`, never by a dead unit's id (`CreditLog.trim` drops dead ids once nothing names them).
> - Also carry from Phase 2 Task 13: `Campaign.notices` entries expire server-side after 10 s (no protocol change), so a dismissed client notice cannot come back.
> - Record in the report: the rank_v1 CLEAR packet (id -1) and the `Mist.caster`/`Banner.by` components are Task 3 additions (plan bookkeeping).

**Executor:** Opus xhigh (combat). **Worktree:** `bannerbound-p2`. **Needs a client:** yes (suite `levels`).

**Files:**
- Modify `Veterans.java`:
  - `onDeath(Battle, Unit victim, Entity attacker, Squad winner)` computes `LevelMath.bounty` and credits `CreditLog.credited` plus the killing blow;
  - `credit(Battle, Unit, int)` raises `credits` and applies level ups;
  - `levelUp(Battle, Unit, int from, int to)` does the health step (max and current rise by the delta), the custom name (`levelName` plus the veteran suffix), the particles in the side colour and a sound (both heard and seen within about 24 blocks), `RankSync.changed`, a re-read of the soldier's ability rows (`Abilities.rowsChanged(u)`, the Task 3 seam Task 6 fills) and the pending feed line;
  - the killing blow's soldier also gets `Abilities.onKill(b, killer)` (the Task 3 seam; *Reap* in Task 6);
  - `pendingFeed`: a list of `(team, levelName)` records per round, read by Task 12, one line per soldier reaching `progression.feedLevel`;
  - the `progression.enabled` guard and the sim override.
- Modify `GravesAndGlory.java`:
  - `AFTER_DEATH` calls `Veterans.onDeath` above the temporary-victim skip (`if(victim.temporary)continue;`), so before the existing gold and Mastery lines, which are unchanged (the binding review note above; done so in c3282e1);
  - an enemy commander's death (a player or a bot commander body) also credits his killers through `Veterans.onCommanderDeath` at `commanderBounty`;
  - the Rise Again `ALLOW_DEATH` false path credits nothing.
- Modify `LevelChecks.java` (suite `levels`).

**Interfaces:**
- Consumes: `LevelMath`, `CreditLog`, `Battle.credit`, `RankSync.changed`.
- Produces: `Veterans.onDeath`, `levelUp`, `pendingFeed`, `Unit.level` and `credits` live. The calls to `Abilities.rowsChanged` and `onKill` reach Task 3's no-ops until Task 10 joins lane B's bodies.

- [ ] **Step 1: Write suite `levels`:**
  - `p2-credit-killer`: one Dawn Squire kills a level-1 Dusk Squire; credits 1, level 1.
  - `p2-credit-assist`: a Dawn archer hits the victim at t-5 s and a Squire kills; both get 1. A hit at t-11 s gets nothing.
  - `p2-credit-slow`: a Barrow Stalker's frostshot slows the victim, a Squire kills, and the Stalker gets 1.
  - `p2-credit-zone`: an enemy held in a Mist Caller's cloud dies, and the caller gets 1.
  - `p2-credit-support`: a Sunweaver mends the killer at t-4 s and gets 1. A Sunweaver who mended nobody involved gets 0.
  - `p2-credit-allied`: two Dawn armies; the helper from the second army gets credit.
  - `p2-bounty`: killing a level-3 victim with two helpers gives each of the three 3.
  - `p2-level-skip`: a soldier at 0 credits gets 9; he is level 3, his health stepped twice, and the log shows two level ups and one feed line.
  - `p2-commander-bounty`: a bot commander body killed gives its killers 3 each.
  - `p2-commander-never-levels`: a player commander's killing blow gives him nothing, while his helpers get theirs.
  - `p2-temporary-no-credit`: a Grave Bloom Risen kills and earns nothing. A Risen killed counts 1.
  - `p2-structure-no-kill`: destroying a Forward Supply banner gives no credit.
  - `p2-mastery-unchanged`: `troopKills` counts killing blows only; assists add nothing.
  - `p2-level-up`: at 3 credits a Squire shows the custom name "Knight", max health x1.05, and current health raised by the delta only (a soldier at 10 of 28 goes to 11.4).
  - `p2-level-name-veteran`: a Mastery II Squire at level 3 is "Knight-Captain (Veteran)".
  - `p2-level-flash`: particles in the side colour and a sound event logged at the soldier. Screenshot `levels-flash`, mid-flash at about 8 blocks.
  - `p2-rise-keeps-level`: a level-2 Graveborn rises through Rise Again with level, credits, name and rank unchanged, and the rising blow gives no credit.
  - `p2-rebuy-level-1`: a level-3 soldier dies, his slot is retrained, and the new one is level 1 with the level-1 name.
  - `p2-progression-off`: with `progression.enabled` false (the sim override), nobody gains credit.
  - `p2-rank-sent`: each level up calls `RankSync.changed`, logged with the new level.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Compile; run suites `levels`, `chase` (Mastery) and `graveborn` (Rise Again, Grave Bloom); all pass.** The tester states what `levels-flash` shows.
- [ ] **Step 5: Commit** `"In-match progression Task 4: kill credit with assists, supports and the veteran bounty; levels and level ups"`.

**Review:** Opus xhigh. Review focus 1 and 2.

---

### Task 5 (lane A): Gear, the buy command, the Armoury state, bot buying, the report and sim logs

**Executor:** Opus high. **Worktree:** `bannerbound-p2`. **Needs a client:** yes (suite `gear`). The sim flag check is headless.

**Files:**
- Modify `Veterans.java`:
  - per-army `Armoury` (highest level seen per type, bought tier per track per type);
  - `buy(Battle, ServerPlayerEntity, Squad, String cmd)` for `gear:<troop>:<weapon|armour>:<tier>`, checked with `LevelMath.refusal`. Location: `atBarracks` or within reach of `reinforcementAnchors(team)`; bots skip the location rule.
  - `wear(Battle, Squad, troop)`, applied on buy and on each level up: armour gives a health step like a level up, weapon gives the factor, then `RankSync.changed`;
  - `armouryState(Squad)` for `state_v9`;
  - `botBuy(Battle, Squad)` through `LevelMath.botPicks`.
- Modify `GravesAndGlory.java`:
  - `Battle.command` routes `gear:` to `Veterans.buy`;
  - `sendState` fills the `state_v9` fields;
  - training and rebuys leave gear untouched (a rebought soldier wears tier I until he levels).
- Modify `Campaign.java`:
  - in `tick()`'s economy block, after the banner refill, call `Veterans.botBuy`;
  - in `finish`, add the report `levels` block: per troop type, `highestLevel`, `level3Count`, `weapon` and `armour` (the bought tiers).
- Modify `CampScreen.java`: the results page lists one line per type: level names reached and tiers bought, in the existing row style.
- Modify `MatchStats.java`: `LevelRow(troop, highest, level3s, weapon, armour)`, pure, and add it to the standalone compile.
- Modify `test-tools/java/gg/gravesandglory/BotSimulation.java`:
  - `-PsimProgression=on|off`;
  - `results.csv` gains `score300_0`, `score300_1` (the score at 300 s);
  - `levels.csv`: per match and side, the seconds of the first level 2 and the first level 3, the share of living soldiers at level 2 or higher at the end, the level-3 count, and the gear bought with its second.
- Modify `LevelChecks.java` (suite `gear`).

**Interfaces:**
- Consumes: Task 4's level ups; `LevelMath` buy states, prices and `botPicks`.
- Produces: the `gear:` command; the `state_v9` Armoury data; the report `levels`; the sim flags and logs (Task 11).

- [ ] **Step 1: Write suite `gear`:**
  - `p2-gear-locked`: before any level 2, `gear:knight:weapon:2` is refused with "Needs a level-2 Knight". Gold is unchanged.
  - `p2-gear-open-l2`: once one Squire reaches level 2, Weapon II opens and Weapon III stays locked.
  - `p2-gear-iii-needs-ii`: with a level-3 Squire, Armour III is refused until Armour II is bought.
  - `p2-gear-open-stays`: the level-2 Squire dies, and the buy stays open.
  - `p2-gear-wear-now`: Armour II bought; every level-2+ Squire gains +10% max health at once (current health by the delta), and level-1 Squires do not.
  - `p2-gear-wear-later`: a Squire reaching level 2 after the buy wears it the moment he levels. A Squire who levelled before the buy keeps tier I until the buy.
  - `p2-gear-rebuy-tier1`: a retrained Squire wears tier I, and no gold is charged.
  - `p2-gear-price`: Common 40/80 and Epic 80/160 are taken from gold. A Warlock's discount does not apply.
  - `p2-gear-no-refund`: there is no command that sells back.
  - `p2-gear-location`: away from the barracks and from any anchor, the buy is refused with "Buy at your barracks or a held point". At the barracks, and at a held forward point, it is accepted.
  - `p2-gear-allies-separate`: two Dawn armies; army 1's buy does not change army 2's Armoury.
  - `p2-gear-stats`: a level-3 Squire with Weapon III and Armour III has max health and `HitLog` base x1.30 each (1.69 in product), with stars multiplying on top for an owned star-3 troop.
  - `p2-gear-round-reset`: a new round empties every Armoury.
  - `p2-armoury-state`: the client's `CampaignClient.armoury` matches the server's for each type and track (packet round trip).
  - `p2-bot-gear-easy`, `-normal`, `-hard`: with fixed gold and levels, each bot buys what `LevelMath.botPicks` says, in order, and keeps its reserve. Easy never buys tier III.
  - `p2-report-levels`: the finished report holds `levels` rows. Screenshot `gear-report`.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Compile; run suites `gear`, `levels`, `chase` and `screens`, and Phase 1 `reinforce,bots,report`; all pass.**
- [ ] **Step 5: Sim smoke (headless):** one match `gilded_normal` vs `graveborn_normal`, 900 s, `-PsimProgression=on`. `levels.csv` has two rows and `results.csv` the 300 s scores. Then the same with `off`, where no level rises.
- [ ] **Step 6: Commit** `"In-match progression Task 5: gear tiers, the gear command, the Armoury state, bot gear buys, report levels, sim logs"`.

**Review:** Opus high. Focus: the opening rule and `worn` against design 4.2; health steps on wear; the location rule against `reinforcementAnchors`; the bot reserve.

---

### Task 6 (lane B): Statuses and the new mechanics N1 to N13, per-level ability rows

> **Note from Task 1 (2026-10-23):** `CounterMath.winners` keeps the largest multiplier per replace group, so C1g (0.30) and C3g (0.5) cannot beat C1 (0.35) and C3 (0.6). Task 6 must change that rule (and pin it in HousesCheck).

> **Review notes (tasks-1-3-review.md, 2026-10-23), binding for this task:**
> - **I1 ruling:** lane B owns the SHAPES (not the values) of five level-3 rows that the design says are "the same ability also does X" but the sheet models as a second independent cast: `mage_l3` (mend also cleanses; today a zero mend falls back to a full heal), `wm_seer_l3` (howl also cleanses; today it resets `howlAmount`/`howlSpeed` and wipes the level-2 +20%), `ic_mortar_l3` (shell leaves burning ground; today a separate mist cast), `tc_mist_l3` (cloud also weakens; today a second cloud), `hexer_l3` and `reaper_l3` (effects on a kill; trigger `onhit` today, use the onKill seam). Reshape them as modifiers of the level-2 row (flags), adjust `tools/preflight.py` rules for those rows only, keep every value. `mistTick` reads only the sheet's first mist row (`Abilities.java` ~:664, :696): make Firepot bleed and Chill fog weaken work.
> - **I2:** second-row state (`knight_l3` cooldown 100, `tc_raider_l3` pounce, `ic_mortar_l3`) lives in a lane B store (e.g. `IdentityHashMap<Unit,...>` in `Abilities` or `Statuses`, emptied from `Statuses.clear`), never a new field on `Unit` (lane A's file).
> - **I3:** add credit hooks: `Veterans.helped` where `Statuses.apply` lays weaken, bleed, mark, stun or knockback and per zone tick for Firepot and Chill fog; `Veterans.supported` for Furnace heart (`ic_colossus_l3`), Admiral's call (`tc_admiral_l3`) and the cleanses; a bleed tick with no live striker credits the recorded source. Check ids `p2-credit-status`, `p2-credit-aura`.

**Executor:** Opus xhigh (combat). **Worktree:** `bannerbound-p2-abil`. **Needs a client:** yes (suite `statuses`).

**Files:**
- Modify `Statuses.java`:
  - `weaken` (x(1 - 0.15) on the target's outgoing hits, read by `factorOut`); the 0.15 is a sheet value on the weaken rows (`amount`);
  - `bleed`: 1 damage every 20 ticks through the troop damage source, outside `resolve`, so undead are not immune. The bleeder is credited through `Veterans.helped`;
  - `mark` (N8): it reuses the Mark Prey state shape without the glow, and `Commander`'s factor path reads it for the marker's army only;
  - the single-target stun (N4);
  - `cleanse(entity)` (N13);
  - the same kind of status does not stack on one target: the longer remaining duration wins.

  Statuses apply to troops and commanders alike, per round 34 question 3 (recommended option). If Kort picks "troops only", `Statuses.apply` returns early for players.
- Modify `Abilities.java`:
  - `row(Unit)` replaces `row(Spec.TroopsRow)` at every call: the level-2 row when `u.level >= 2`, else the base;
  - `second(Unit)` gives the level-3 row when `u.level >= 3`, else none;
  - `tick` and `onHit` dispatch both rows;
  - `rowsChanged(Unit)` refreshes the unit's added counter tags (`abilities.tags`) and timers;
  - `onhit_status` gains `weaken`, `bleed`, `knockback` and `stun`;
  - `onkill_heal` (N5) is the body of `Abilities.onKill(b, u)` (Task 3's seam, which lane A's kill path already calls). In this lane the suite calls it directly;
  - `raise` (N6) uses `Commander`'s Grave Bloom temporary-troop code, at most 2 at a time, for 15 s;
  - `mist` zones may carry `status` and skip the ranged penalty (N12), and the Firepot is a burning-ground zone;
  - `mend` and `howl` call `Statuses.cleanse` when `cleanse` is set (N13);
  - the aura `taken` lowers damage own troops take (N11);
  - weapon tier scales the heal amount (Sunweaver) and the cloud duration (Mist Caller) through `Veterans.outputFactor`;
  - keep every Task 3 credit hook.
- Modify `Combat.java`: `stance` reads `Abilities.row(u)` (fearless at level 2); the "Bodkin" and guard rows act through the tags.
- Modify `CounterMath.java`: the conditions `target_slowed_or_wounded` and `target_wounded`, with `Hit` gaining `targetSlowed` and `targetWounded`.
- Modify `test-tools/standalone/HousesCheck.java` for those conditions.
- Modify `Houses.java`: `rise` (N7). A Hollow King at level 3 may rise once at 50%, with its own chance added to the trait's where both apply, the Legendary exclusion lifted for this row, and the same keep-level path.
- Modify `Commander.java`: `mark` read beside Mark Prey (marker's army only).
- Modify `HouseAttacks.java`, only where a Tidecallers or Ironclad level row needs its attack path (harpoon pull length, brine).
- Modify `AbilityLevelChecks.java` (suite `statuses`).

**Interfaces:**
- Consumes: the `Unit.level` field; `Veterans.helped`/`supported`/`outputFactor`; the Task 1 rows.
- Produces: `Abilities.row(Unit)`, `second(Unit)`, `rowsChanged`, `onKill`; `Statuses.apply(b, from, target, status, ticks, level)`; the two counter conditions.

- [ ] **Step 1: Write suite `statuses`** (the harness sets `u.level` and calls `Abilities.rowsChanged`):
  - `p2-status-slow-as-before`: frostshot at level 1 gives exactly today's Slowness and ticks.
  - `p2-status-weaken`: a weakened Squire's next `HitLog` has `status` 0.85.
  - `p2-status-bleed`, `p2-status-bleed-undead`: a Risen bleeding for 80 ticks loses 4.
  - `p2-status-nostack`: a 40-tick bleed over a 60-tick one keeps 60.
  - `p2-status-knockback`: Heavy bolt 0.6.
  - `p2-stun-single`: Shield bash stuns only the target for 20 ticks, every 100 ticks.
  - `p2-onkill-heal`: Reap heals 25% of max health on a killing blow.
  - `p2-raise`: Death knell raises at most 2 temporary Risen for 15 s, and they vanish at round end.
  - `p2-rise-l3`: the Hollow King rises once at 50% at level 3, never at level 2.
  - `p2-mark`: marked targets take x1.10 from the marker's army and x1.0 from an allied army.
  - `p2-cond-slowed-wounded`: Run them down x1.3 on a slowed or a 30%-health target, x1.0 otherwise.
  - `p2-thickhide`: an arrow on a level-3 Old Horn gets C19 0.75 in `HitLog.rows`.
  - `p2-aura-taken`: Furnace heart x0.9 taken within 6.
  - `p2-zone-status`: Chill fog weakens inside the cloud; Firepot bleeds inside the ring.
  - `p2-cleanse`: Dawnlight and Rousing howl remove slow, weaken and bleed.
  - `p2-output-tier`: a Sunweaver with weapon tier III heals 5 x 1.2 (base row, level 1); a Mist Caller with tier III has a cloud of duration x1.2.
  - `p2-level-row-select`: a level-2 Longbow's pierce is 0.7 and reaches 3; at level 3 the second row (slow) also fires.
  - `p2-status-clear-round`: a new round clears every status.
  - `p2-commander-status`: a player commander hit by Marrow arrows is weakened (per question 3).
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Compile; run the standalone checks and suites `statuses,engine,gilded,graveborn,ironclad,tidecallers,wildmoot`; all pass.**
- [ ] **Step 5: Commit** `"In-match progression Task 6: statuses, the thirteen small mechanics, per-level ability rows"`.

**Review:** Opus xhigh. Focus: no level-3 row copies a sibling's signature (design 1, anti-goals); bleed credit and immunity; nothing outside the counter table except the base factor.

---

### Task 7 (lane B): The 60 level rows, House by House

**Executor:** Sonnet xhigh (mechanical once Task 6 exists; one commit per House allowed). **Worktree:** `bannerbound-p2-abil`. **Needs a client:** yes (five suites).

**Files:**
- Modify `AbilityLevelChecks.java` (suites `levels-gilded`, `levels-graveborn`, `levels-wildmoot`, `levels-ironclad`, `levels-tidecallers`).
- Modify `Abilities.java`, `Statuses.java` and `HouseAttacks.java` only for defects these checks find.

**Interfaces:**
- Consumes: Task 6.
- Produces: one proof per row of design 6.2.

- [ ] **Step 1: Write the five suites:** one `p2-l2-<troop>` and one `p2-l3-<troop>` per troop, 60 ids. Each spawns the troop at that level against a fixed target, does one action and asserts the row's number in `Combat.LAST` or in the status. Examples:
  - `p2-l2-knight`: C1g 0.30 on an arrow against a raised shield.
  - `p2-l3-gl_marshal`: a cleave at 50% within 2.
  - `p2-l2-wm_seer`: Howl 0.20 with cooldown 240.
  - `p2-l3-tc_admiral`: +10% within 10.

  One screenshot per House, `levels-<house>`, shows a level-3 troop of that House using its second ability.
- [ ] **Step 2: Run; expect failures where a row's path is missing.**
- [ ] **Step 3: Fix each failure in the mechanic, never in the sheet.** A sheet value that looks wrong is recorded for Task 11.
- [ ] **Step 4: Run all five suites plus `statuses`; all pass.** The tester describes each House shot.
- [ ] **Step 5: Commit** (one per House or one in all) `"In-match progression Task 7: level 2 and level 3 rows proved for <House>"`.

**Review:** Sonnet high (small diffs per House), then one Opus high pass over the whole lane before Task 10.

---

### Task 8 (lane A): The Armoury tab

**Executor:** Opus high (UI design). **Worktree:** `bannerbound-p2`. **Needs a client:** yes (suite `armoury`, screenshots graded).

**Files:**
- Create `ArmouryTab.java`:
  - one row per banner troop type, with the portrait at its best worn tiers (`ArmyCards.portrait(spec,w,a)`);
  - four buttons (Weapon II, Weapon III, Armour II, Armour III) in four states: locked (the needed level name), open (the price), bought, unaffordable (greyed, the price shown);
  - clicking sends `gear:`;
  - away from a buy location every button is greyed, with the reason line "Buy at your barracks or a held point";
  - with `progression.enabled` false the tab says "Levels are off".
- Modify `RecruitmentScreen.java`: an Armoury tab beside the barracks page, and the screen opens on the Armoury tab with a new key binding. The executor picks a free key, checks it against every existing binding, records it in the ledger, and shows the binding through `HintText`.
- Modify `GravesAndGloryClient.java`: the key binding.
- Modify `LevelChecks.java` (suite `armoury`).
- Mockup first: `work/ui-concepts/armoury/` (git-ignored), built from today's barracks screen, then graded.

**Look:** per round 34 question 4 (recommended option): DarkUi tokens, Marcellus headings and the barracks screen's own widgets, as the screen it sits on, within the War Table and Iron Ledger family. No brass fittings on wood. The `ui-ux-pro-max` `--domain ux` checks are run for contrast, hit targets, feedback and colour-blind states: every state is told apart by a word or a glyph, never by colour alone.

- [ ] **Step 1: Mockup the tab at 960x540 and 854x480; grade it against the rubric; 9 or better before code.** Kort sees one 960-wide contact sheet.
- [ ] **Step 2: Write suite `armoury`:**
  - `p2-armoury-tab`: the tab shows a row per type with four buttons in the states the server reports. Screenshot `armoury-tab`.
  - `p2-armoury-states`: a fixture has one button in each state; the labels are "Needs a level-2 Knight", "40", "Bought", and "80" greyed.
  - `p2-armoury-field`: the key opens the tab in the field; the buttons are greyed with the reason. Screenshot `armoury-field`.
  - `p2-armoury-buy-click`: a click at the barracks buys, and the state updates within 2 state packets.
  - `p2-armoury-fits`: at 854x480 every button lies inside the canvas, with hit targets of at least 20x20 design px.
  - `p2-armoury-key-hint`: a rebound key shows its real name.
- [ ] **Step 3: Run; expect failures.**
- [ ] **Step 4: Implement.**
- [ ] **Step 5: Run suites `armoury,gear`; all pass. Grade `armoury-tab`, `armoury-field` and a greyscale copy of each: 9 or better.** A lower score goes back to Step 4.
- [ ] **Step 6: Commit** `"In-match progression Task 8: the Armoury tab and key"`.

**Review:** Opus high. Rubric scores in the review.

---

### Task 9 (lane C): Tier switching, the art stopgap, chevrons, portraits and the release check

**Executor:** Opus high (model pipeline). **Worktree:** `bannerbound-p2-rank`. **Needs a client:** yes (suite `ranks`, screenshots graded). The tool checks are static.

**Files:**
- Modify `TroopModels.java`:
  - `resolve` takes the armour tier, weapon tier and level from `RankSync.client(e)`. The name now only says which troop it is, plus the veteran suffix.
  - Until the first `rank_v1` arrives, the troop draws at 1/1/1. `TroopModels.resolve` no longer reads the level from the name.
- Modify `TroopModel.java` and `TroopModelLoader.java`:
  - the `stages` part key means armour tier, and `skins` keys are armour tiers;
  - a new part key `weaponTiers` shows weapon parts for troops with no held weapon (Moot Hound, Web Stalker, Old Horn, Brassjaw, the Brute, the Mist Caller, the Mortar, and the Ballista Crew's frame parts);
  - the veteran trim draws over whichever armour tier is worn.
- Modify `TroopRenderer.java` and `TroopFeatures.java`: the weapon model picks `art/weapons/<weapon>_t<n>`, with tier I staying `<weapon>`.
  - **Stopgap:** a missing tier draws as the highest tier below it that exists, for skins, weapons and part sets.
- Create `TroopChevrons.java`:
  - one chevron over a level-2 soldier and two over a level-3, for both teams, as a small billboard above the head in the side's colour;
  - drawn only within `progression.chevronRange`, and not affected by the troop-depth Visuals switch;
  - texture `assets/gravesandglory/textures/entity/troop/chevron.png`, painted to the style guide.
- Modify `ArmyCards.java`: `portrait(spec, w, a)` sets the portrait entity's rank with `RankSync.setLocal`. It is cached per id, tiers and colour.
- Modify `tools/build_skins.py`:
  - `weaponTiers` in the part schema;
  - weapon tiers built from `art/weapons/<weapon>_t2|_t3` when present;
  - `--tiers` prints, per troop, which armour tiers and weapon tiers exist and which fall back (the stopgap list).
- Modify `test-tools/standalone/test_build_skins.py`: schema and stopgap-list cases.
- Modify `tools/package.py`: it refuses while the stopgap list is not empty, printing the list, unless `--allow-stopgap` is given (round 34 question 6). A stopgap package's file name carries `-stopgap`.
- Modify `Phase2Checks.java`: `p2-gilded-stages` and the other stage spawns become `p2-gilded-armour-tiers` and armour-tier checks through `RankSync`.
- Modify `RankChecks.java` (suite `ranks`).
- Docs: in the Phase 2 plan's art steps, replace "the file naming is the art step's (open: see the in-match progression design)" with the naming above (`<weapon>_t2`, `<weapon>_t3`, the `weaponTiers` part key; armour tiers are the `_s1` to `_s3` skins). In `HOW-TO-EDIT.md`, the tier art section.

**Interfaces:**
- Consumes: `RankSync.client`.
- Produces: the naming and part keys the art steps paint to; `build_skins.py --tiers`; the release refusal.

- [ ] **Step 1: Write suite `ranks` and the tool tests:**
  - `p2-rank-sync`: the server sets a Squire to 3/2/3; within 2 ticks the client's `RankSync.client` reads it.
  - `p2-rank-late-join`: a client registering late gets every tracked soldier's rank.
  - `p2-rank-clear`: a new round empties the client store.
  - `p2-armour-tier-skin`: a Squire at armour tiers 1, 2 and 3 binds `_s1`, `_s2` and `_s3`.
  - `p2-weapon-tier-model`: tier 2 draws `<weapon>_t2` when it exists, and tier I's model when it does not (the stopgap).
  - `p2-weapon-parts-tier`: Old Horn's `weaponTiers` parts switch.
  - `p2-tier-stopgap`: an Epic with only `_s1` draws `_s1` at armour III. The chevrons still show the level.
  - `p2-chevrons-both-teams`: a level-2 Dawn and a level-3 Dusk soldier draw one and two chevrons in their side colours. Screenshot `ranks-chevrons` at about 12 blocks.
  - `p2-chevron-range`: no chevron beyond 24 blocks.
  - `p2-chevron-depth-off`: with Visuals off the chevrons still draw.
  - `p2-veteran-over-tier`: a Mastery II Squire at armour III draws the trim over `_s3`.
  - `p2-portrait-tiers`: the portrait at 2/3 draws the tier-3 skin and the tier-2 weapon.
  - `p2-gilded-armour-tiers`: the converted stage check. Screenshot `ranks-tiers`: the Squire at 1/1, 2/2 and 3/3 side by side.
  - `test_build_skins.py`: `--tiers` lists a troop with a missing `_s3` as a stopgap. `package.py` refuses with the list, and accepts with `--allow-stopgap`, marking the name.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Implement. Paint `chevron.png`.**
- [ ] **Step 4: Compile; run suites `ranks,gilded-art,depth,anim,tells` and the tool tests; all pass.**
- [ ] **Step 5: Grade `ranks-chevrons` and `ranks-tiers` (and greyscale copies): 9 or better.** The chevron must read at a glance at 12 and 24 blocks, and never as a vanilla item or a hotbar icon.
- [ ] **Step 6: Commit** `"In-match progression Task 9: armour and weapon tiers drawn from rank_v1, art stopgap, chevrons, tier release check"`.

**Review:** Opus high. Focus: review focus 4; no stopgap leak into a release package; the draw cost with chevrons (the depth suite's tick-cost camera within its budget).

---

### Task 10: Join the lanes and run the regression

> **Binding notes from the Task 7 review (controller, 2026-10-23):** (1) Add one check where a real killing blow crosses a level: a troop at one kill from level 2 or 3 lands the blow that kills, and the new row applies on that blow (the M1 ruling in the comment above `Combat.onHit`; Reap's `p2-l3-reaper` calls `onKill` directly today). (2) Lane B's bleed numbers `BLEED_DAMAGE` (1) and `BLEED_EVERY` (20 ticks) in `Statuses.java` become `combatDiscipline` cells (for example `bleedDamage`, `bleedEveryTicks`) through the sheet and `tools/preflight.py`; `HURT_HALF` and `BLEED_GRACE` stay in code. (3) `tools/preflight.py` is frozen in `lane-rules.md`; lane B's M6 rule (a level-3 row may not trigger on cast, timer, hold or charge_contact) is accepted as the one exception.

> **Binding notes from the Task 5 re-review (controller, 2026-10-23):** (4) `Veterans.refusal` caps the gear tier at 3 because `LevelMath.price` throws above it; keep the cap, or make `price` return the tier III price and cover `LevelMath.botPicks` too (it can still throw with a four-row sheet). (5) Run the takeover suite and the Phase 1 suites after the merge (the Task 5 fix round did not). (6) The results page bottom block overflows into the buttons in a 2v2 or bigger match with an 8-type, 4-House banner at the default 1080p canvas (818x460; pre-existing): record it for Task 12 and the merged base, do not fix here. (7) Carry into Task 11: drop rounds under 300 s (`results.csv` has a `seconds` column) and check the `[levels] kill` lines for helper-credit farming (first level 3 lands 9 to 19 s after the first level 2, 32 to 35 level-3 soldiers a side).

**Executor:** Sonnet high (merge and runs, rules fixed below; Sonnet xhigh on the night of 2026-10-23 by Kort's no-Opus rule). **Worktree:** `bannerbound-p2`. **Needs a client:** yes.

**Files:** merge commits only, plus the edits the lanes deferred: the items in each lane's report marked "needs another lane's file".

- [ ] **Step 1: Merge `progression-abilities`, then `progression-ranks`, into `phase-2-houses` locally.**
  - Resolution rule: keep both sides' behaviour, since the lanes own disjoint files. Any conflict means a lane broke the ownership rule: stop and report it to the controller.
  - After the merge, lane A's calls to `Abilities.rowsChanged` and `onKill` reach lane B's bodies. Rerun `p2-onkill-heal` and `p2-level-row-select` through a real level up (Step 3 covers it).
- [ ] **Step 2: Compile; run the standalone checks and `build_skins.py --verify`.**
- [ ] **Step 3: Full regression, one launch each:**
  - Phase 2: `banners,engine,commander,spike,gilded-art,anim,depth,tells,gilded,graveborn,graveborn-art,ironclad,ironclad-art,tidecallers,tidecallers-art,wildmoot,wildmoot-art,chase,screens,migration`;
  - this plan's suites: `levels,gear,armoury,statuses,levels-gilded,levels-graveborn,levels-wildmoot,levels-ironclad,levels-tidecallers,ranks`;
  - Phase 1: `combat,morale,reinforce,bots,tutorial,report,ui`;
  - then `-PcampaignChecks` alone.

  Expected: every id passes. Known flakes (`p2-blight-gravecaller`, the anim platform light) get one rerun, recorded.
- [ ] **Step 4: Two-client check:** a LAN host and a guest who joins mid-battle. The guest sees the host's level-2 chevron and correct tiers (`p2-rank-late-join` in the real network path), and the guest's Armoury tab shows the guest's own army.
- [ ] **Step 5: Commit the merge and any join fixes** `"In-match progression Task 10: lanes joined, regression"`. Run `graphify update .`.

**Review:** Sonnet high (merge diff), with an Opus high whole-plan review of `review-<Task 3 head>..<Task 10 head>.diff` before Task 11.

---

### Task 11: Balance gate (design 9.2), headless

**Executor:** Sonnet high for the runs and the log. Tuning diagnosis, when a check fails, goes to Opus high. **Worktree:** `bannerbound-p2`, or a detached gate worktree (`bannerbound-gate-prog`) so Task 12 prep can continue. **Needs a client:** no; headless bot sims only.

**Method:** depends on round 34 question 1.
- **Recommended (round robin, Kort's round 32 method):** with `tools/balance_method.py` and `tools/balance_sim.sh` merged onto `phase-2-houses` first (their own commit):
  - run every ordered pair of the five `<house>_normal` banners, 10 matches per seat, maps 0 to 4 twice, `-PsimRoundSeconds=900 -PsimFast=true`, once with `-PsimProgression=off` (baseline) and once `on`;
  - Bradley-Terry share per House per condition, with intervals;
  - plus one real-time 900 s pair per condition (the current best pair) to catch fast-tick drift.
- **Spec as written:** each pure banner against the current best pure banner, 20 matches (10 per seat), 900 s real time, off then on.

**Files:**
- Create `tools/progression_gate.py`, which reads `results.csv` and `levels.csv` from both conditions and prints:
  - per House, the share on and off and the delta;
  - the pooled comeback rate (decided matches won by the side trailing at 300 s), on and off;
  - the level timings (median second of the first level 2 and the first level 3 per side; the end share at level 2 or higher; level 3s per round);
  - pass or fail per rule.
- Modify `docs/PHASE-2-BALANCE.md`: a section "In-match progression gate".
- Modify `sheets/design.json`: only on a fail, only the levers of design 9.2, in order (thresholds, then gear prices, then gear percentages, then level bonuses).

**Rules (design 9.2):**
- Pass: every House's share with levels on is within 10 points of its own baseline share. The pooled comeback rate on is no more than 10 points below off.
- Report: a House inside 40 to 60% in both runs is balanced. One outside goes to the Phase 2 end balance pass, not to this plan.
- The level target is a tuning aim: first level 2 at 2 to 3 minutes; about a third of the winner's living soldiers at level 2 or higher at the end; a few level 3s per round.
- Difficulty ladder (Easy, Normal, Hard with gear buying on): report only.
- Stop rule: two tuning rounds. Whatever still fails goes to `docs/QUESTIONS-FOR-KORT.md` with the numbers, and the plan reports instead of looping. A catch-up gold rule is not tuned here; it is a question.

- [ ] **Step 1: Write `tools/progression_gate.py` and a fixture test** (two hand-made csv sets with known shares and comeback rates); run it; it passes.
- [ ] **Step 2: Run the baseline (off) and the levels (on) conditions in the background, two servers at a time.**
- [ ] **Step 3: Run `progression_gate.py`; log every line in `docs/PHASE-2-BALANCE.md`.**
- [ ] **Step 4: On a fail, tune round 1, then rerun the failing condition; tune round 2 if needed; then apply the stop rule.** Log each change as `row.column old -> new` with the reason.
- [ ] **Step 5: Run the standalone checks and preflight after any sheet change; commit** `"In-match progression Task 11: progression gate, <pass|report>"`.

**Review:** Opus high reads the gate log and any tuning. Kort gets the numbers in the round 34 follow-up if the stop rule fired.

---

### Task 12: Iron Ledger pips and glint, the level-3 feed line (merged base, closing HUD task)

**Executor:** Opus high (small HUD art graded 9). It runs back to back with UI plan Task 9 by the same agent, in separate commits. **Worktree:** the merged base, after the base merge (Kort's call). **Needs a client:** yes.

**When.** After `phase-2-houses` (with Tasks 1 to 11) and `ui-plan` have merged into the base, and before 0.5.0 ships (design 11.4). The base-merge agent takes the `state_v9` layout from `phase-2-houses` and points the UI branch's HUD reader at it; that is a merge note, not this task.

**Files:**
- Modify `BattleHud.java` and `UiMath.java` (`hudLayout`):
  - **Pips:** per group, how many living soldiers are at level 2 and at level 3, on the full card and on Task 8c's one-line chip, in a slot `hudLayout` reserves, so the footprint budget (`p1-ui-hud-footprint`) still passes.
  - **Glint:** on a group's card or chip when `armouryOpen` is true (an Armoury buy for a troop of that group is open and not yet bought).
- Modify `CommandMode.java`: `Group` gains `level2`, `level3` and `armouryOpen` from `state_v9` (if the merge has not added them already).
- Modify `FeedLines.java`: `levelUp(String levelName)` gives "<level name> rises" ("Knight-Captain rises").
- Modify `FeedEvents.java`: `levelUp(b, team, levelName)` reads `Veterans.pendingFeed` each tick. The kind is `levelup`, with a new glyph; an enemy's line is drawn in the enemy colour. Level 2 makes no line (`progression.feedLevel` 3).
- Modify `EventFeed.java`: the new kind.
- Modify `tools/build_ui_art.py` and `tools/ui_art/` (painters): the pip (two sizes: level 2 and level 3), the glint frames and the feed glyph, in the Iron Ledger family.
- Modify `UiMathCheck.java` and `test_ui_standard.py`.
- Modify `Phase1Checks.java` (suites `ui-hud`, `ui-feed`).
- Modify `docs/PARKED.md` if any HUD item is cut.

**Colour-blind rule:** pips and the glint are told apart by shape (one or two marks; a moving glint), never by colour alone. The feed line carries its glyph.

- [ ] **Step 1: Write the checks:**
  - `p1-ui-pips`: a group with 3 level-2 and 1 level-3 soldiers draws those pips at the `hudLayout` rects, on the chip and on the full card. Screenshot `ui-hud-pips`.
  - `p1-ui-glint`: opening a buy for an infantry troop sets the infantry card's glint, and buying clears it.
  - `p1-ui-feed-level3`: a Dawn Squire reaching level 3 gives "Knight-Captain rises" once with the `levelup` glyph. An enemy soldier's line is in the enemy colour.
  - `p1-ui-feed-level2-silent`: level 2 gives no line.
  - `p1-ui-hud-footprint`: still within budget at the five sizes.
  - `UiMathCheck`: `FeedLines.levelUp("Knight-Captain").equals("Knight-Captain rises")`, and the `levelup` ink holds 4.5:1 on the slip.
  - `test_ui_standard.py`: the pips and the glint never draw by colour alone.
- [ ] **Step 2: Run; expect failures.**
- [ ] **Step 3: Paint the art; implement.**
- [ ] **Step 4: Run suites `ui-hud,ui-feed,ui-art` and Phase 1 `combat,report`, and this plan's `levels,gear,ranks`; all pass.**
- [ ] **Step 5: Grade `ui-hud-pips` and a feed shot with the level-3 line (and greyscale copies): 9 or better.**
- [ ] **Step 6: Commit** `"In-match progression Task 12: Iron Ledger pips and glint, the level-3 feed line"`.

**Review:** Opus high.

---

### Task 13: Final regression and Kort's gate

**Executor:** Sonnet high. **Worktree:** the merged base. **Needs a client:** yes.

**Files:**
- Modify `docs/PHASE-2-PLAYTEST.md`:
  - Session 4 gains "a match played for levels and gear": see a first level 2 and the chevron; buy Weapon II at the barracks; try a buy in the field (greyed, with its reason); see an enemy Knight-Captain's feed line; read the report's levels rows. Each item gets a yes/no box.
  - Free-text questions: "Did a comeback feel possible?" and "Could you tell enemy veterans apart?".
  - Appendix: the Task 11 gate lines.
- Modify `HOW-TO-EDIT.md`: the progression sheets, the level rows and the tier art naming.
- Modify `docs/PARKED.md`, for any cut items.

- [ ] **Step 1: Full regression on the merged base:** Task 10's list plus the UI suites `ui-art,ui-hud,ui-feed,ui-home,ui-recruit,ui-pages,ui-leave`, then `-PcampaignChecks` alone. Every id passes.
- [ ] **Step 2: Standalone checks, `test_ui_standard.py`, `build_ui_art.py --verify`, `build_skins.py --verify` and `--tiers`.** Then `"$G" build --offline -x test` and `python tools/package.py --allow-stopgap` (a local, marked build).
  - The leak check finds no `LevelMathCheck`, `LevelChecks`, `AbilityLevelChecks`, `RankChecks`, `Phase2Checks` or `BotSimulation` in the jar.
  - Record the `--tiers` stopgap list: it is the 0.5.0 art gate. `python tools/package.py` without the flag must refuse while the list is not empty.
- [ ] **Step 3: Write the playtest additions and update `HOW-TO-EDIT.md`.**
- [ ] **Step 4: Commit** `"In-match progression Task 13: regression, playtest gate for levels and gear"`. Hand the branch, the playtest doc and the stopgap list to the orchestrator. Do not merge or push.

**Kort's gate:** Session 4 of the playtest. 0.5.0 ships when the playtest passes, the Task 11 gate passed or Kort ruled on its report, and the stopgap list is empty (every troop has weapon I to III and armour I to III art, through the art steps and Kort's art-reference review).

---

## Spec gaps resolved

1. **The data comes first, in one task.** Every row and column of design section 8 is written in Task 1, including all 60 ability rows, so the parallel lanes never edit `sheets/design.json`. Tuning happens only in Task 11.
2. **Level-2 stance and guard rows are counter rows.** Stance multipliers live only in `counters` (Phase 2 rule). So *Steady*, *Ambush*, *Brace*, *Armour bolt*, *Sunder* and *Better guard* at level 2 are new rows keyed on a tag the level-2 ability grants (`abilities.tags`), with `replaces` naming the base row. *Bodkin points* (C15b), *Brace the wall* (C18d), *Run them down* (C20) and *Thick hide* and *Barnacle hide* (C19) work the same way.
3. **The Templar's *Unbroken*** ("at or below 30% he takes 20% less") needs the wounded half of N9 alone. So the counter conditions gain `target_wounded` beside `target_slowed_or_wounded`. Both read `combatDiscipline.woundedShare`.
4. **The level and weapon factor sits on the base, outside the clamp,** like stars. Design 4.1 says stars, traits, commander effects and counter rows multiply on top of one level-plus-gear multiplier. `weaken` and `mark` sit inside the clamped product with the auras. `HitLog` records both.
5. **Bleed** ticks outside `Combat.resolve` (1 damage, troop damage source, no counters, no on-hit), and the bleeder is credited through the assist log.
6. **Card text and game read one row.** `troopNotes.level2`/`level3` (Task 12 of Phase 2) are dropped. Card lines come from the `levelAbilities` rows' `abilities.text`.
7. **Tier art naming** (the Phase 2 art steps left it open):
   - weapons are `art/weapons/<weapon>_t2` and `_t3`, and tier I keeps `<weapon>`;
   - armour tiers are the `_s1` to `_s3` skins, and the `stages` part key means armour tier;
   - weapon-part sets use a new `weaponTiers` part key, which covers the Ballista Crew's frame parts.
8. **Release check:** `tools/package.py` refuses while any troop draws a stopgap tier, unless `--allow-stopgap` is given, which marks the file name (round 34 question 6). `build_skins.py --tiers` prints the list.
9. **The level-3 feed line before the HUD exists.** On `phase-2-houses`, `Veterans.pendingFeed` records each line, as Task 8b's last-stand hook does, and Task 12 sends it through `FeedEvents` on the merged base.
10. **The level-up flash is server-side** (particles and a sound at the soldier), so every client sees it with no extra packet. Chevrons are client-side from `rank_v1`.
11. **Bots' rebuy reserve** is `gearReserveTroops x` the cheapest banner troop at the price the bot really pays (`Commander.cost`, class discount included). Gear itself never takes the discount (design 4.2).
12. **The sim switch.** `progression.enabled` stays the sheet's value for play. `-PsimProgression` overrides it inside `BotSimulation` only, for the gate's baseline.

## Needs Kort

Round 34 of `docs/QUESTIONS-FOR-KORT.md` (appended 2026-10-23):
1. The gate's method: round robin (round 32) or the spec's 20-match gate.
2. Confirming the save and protocol changes listed above.
3. Whether statuses also hit player commanders.
4. The Armoury tab's look on `phase-2-houses`.
5. Whether the Phase 2 end balance pass runs with levels on.
6. Packaging builds while tier art is missing.

Until he answers, tasks build the recommended option, and each answer changes only the step named in its question.
