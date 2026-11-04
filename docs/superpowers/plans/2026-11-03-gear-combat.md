# Gear Combat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build part (d): the commander fights with the gear they wear. `equipPlayer` builds the kit from the equipped pieces, gear stats, set bonuses and perks replace `Commander.applyPlayer`, relics fire on keys 1 to 3 through the old class-active path, classes are removed, and every `profile.commander` reader is audited.

**Architecture:** Numbers live in the sheet. Pure classes (`GearStats`, `GearEffects`, `RelicSlots`) turn a profile's worn pieces into a `Loadout` (stat conversions, set counts, active bonuses and perks) and hold relic cooldowns, so standalone checks cover the rules. `GearCombat` applies a `Loadout` to the player (fixed-UUID attribute modifiers, the kit) and is the only Minecraft-facing gear class; `RelicEffects` holds the effect bodies, with the six old class actives ported from `Commander.ACTIVES`. Existing damage code in `GravesAndGlory.allowDamage` keeps its rules and gains hook calls only.

**Tech Stack:** Java 17, Fabric 1.20.1 (Loader 0.16.14, API 0.92.2+1.20.1, Mixin), Python 3.12 preflight.

**Spec:** `docs/superpowers/specs/2026-11-03-commander-gear-design.md`. Index: `docs/superpowers/plans/2026-11-03-commander-gear-index.md`. Foundation: `docs/superpowers/plans/2026-11-03-gear-foundation.md` (merged first). Task 1 here also unblocks the screen plan's Task 5.

## Global Constraints
- Spec.java comes from `python tools/preflight.py`; never hand-edit it. Commit the regenerated `Spec.java` and `sheets/preflight.json` with `sheets/design.json`.
- Do not change the foundation's records or interfaces (`Spec.GearItemsRow`, `Gear`, `Forge`, `GearWire`, the `gear:`/`forge:` commands). New data goes in new tables.
- **No catalogue data in code.** Families, set bonuses, perks, relic tuning and stat conversions come from the sheet; code knows only effect ids (the vocabulary below). Tests read rows, never literal catalogue names or numbers. Draft numbers belong to Kort's balance review (part g); the house balance gate stays closed (`balance-closed`), so this plan sets no tuning beyond copying today's class numbers into relic rows.
- New pure classes go in `tools/verify.py:75`; new standalone checks are `test-tools/standalone/<Name>Check.java` printing `<Name>Check: N checks passed`. Real-client suites go in `Phase1Checks.SUITES` (`test-tools/java/gg/gravesandglory/Phase1Checks.java:41`).
- Protocol: `state_v9` keeps its layout (relic slot 0 rides in the old ability field). A new additive S2C channel `relics_v1` carries all three slots. The `ability` ORDER command is replaced by `relic:<0-2>`.
- Commit with explicit paths, never `git commit -a`; leave `maps/*/design.json` uncommitted. End commits with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Do not push.
- Weapons: the blade faces the move (`weapon-faces-the-move`); the stand-in vanilla items carry no back flags (`no-back-flags`). Art is part (f).
- Never build in a checkout with a running client. Real game tests run per WORKFLOW with `GRAVES_ROADMAP_TEST_ROOT`, as foundation Task 8 Step 6.

## Effect vocabulary (code knows these ids; the sheet picks and tunes them)
- **Relic effects:** `stand_fast`, `rally_horn`, `grave_bloom`, `forward_supply`, `mark_prey`, `bloodrush` (ported), and new `dash`, `shove`, `heal_pulse`, `volley`, `self_heal`, `war_cry`, `iron_skin`.
- **Set-bonus effects:** `surrounded_guard`, `taunt_aura`, `heal_over_time`, `cleanse`, `draw_speed`, `pierce`, `command_radius`, `formation_charge`.
- **Perk effects:** `second_chance`, `grudge`, `quickened_relic`, `heavy_hand`, `last_light`, `hawkeye`, `oathkeeper`, `trailblazer`.

## Settled calls (log for Kort; reversible)
1. **Relic keys take the hotbar keys 1 to 3 in battle.** The server pins the selected slot to 0 (the main weapon) during battle, so 1 to 3 are free. Key H and the `ability` binding are removed.
2. **Perks are fixed per item** (`gearPerks.item`) and switch on at Epic and Legendary only.
3. **Healer 4-piece is `cleanse`**, ranged 4-piece is `pierce` (the draft offered alternatives).
4. **The foundation's `iron_skin` relic and the draft's Second Wind both exist:** `iron_skin` is a relic effect; Second Wind is the `second_chance` perk.
5. **Greatweapon sweep** hooks the player-hit block of `allowDamage`; the shield cooldown uses a mixin on `PlayerEntity.damageShield`; bow draw speed uses a `@ModifyVariable` on `BowItem.onStoppedUsing`.
6. **Class passives end** (cost and training factor 1, range and point-gold bonus 0, respawn from `R.respawnSeconds`); their value moves into gear. This is a gameplay change already inside Kort's approved direction; part (g) retunes.
7. **`Commander.java` keeps its name** (squad commander plumbing); new effect code is in `RelicEffects.java`.

## Review Focus
1. **A player with no gear view or an empty loadout** (old save mid-migration, bot seat): `equipPlayer` falls back to the starter kit from `Gear.grantStarter` on a copy and never to an empty hand (Task 3).
2. **Modifier stacking across respawns and battles:** fixed UUIDs are removed before re-adding, so health and speed do not climb (Task 3 check `p1-gear-combat-respawn`).
3. **Damage order in `allowDamage`:** the old bloodrush, stand-fast and commander-downed rules give the same numbers as before for the ported relics (Task 5 check copies the old Phase2 numbers).
4. **Hotbar keys 1 to 3 outside battle** still change slot; in battle they never change slot and never reach vanilla (Task 5).
5. **Every `profile.commander` and `Spec.CLASSES` reader** is gone or rewired; `rg "CLASSES|classesRow|Commander.row|applyPlayer|\"class:\""` returns nothing outside migration (Task 7).

---

### Task 1: Combat sheets and `GearLooks`

**Files:**
- Modify: `sheets/design.json` (tables below), `tools/preflight.py` (rules), regenerated `Spec.java` and `sheets/preflight.json`
- Modify: `test-tools/standalone/test_preflight_gear.py`
- Create: `src/main/java/gg/gravesandglory/GearLooks.java`

**Interfaces:**
- Produces sheet records (field order is the contract):
  - `GearFamiliesRow(id, item, attackSpeed, damageMult, drawMult, sweepRadius, sweepShare, cooldownTicks, range, healBase, auraMult)`: one row per weapon family in `gearItems.family`, plus one per armour slot (vanilla stand-in `item` only).
  - `GearSetBonusesRow(id, set, pieces, effect, value1, value2, radius, text)`: `set` in `gearSets.id`, `pieces` 2 or 4, `effect` in the set vocabulary.
  - `GearPerksRow(id, name, item, effect, value1, value2, text)`: `item` in `gearItems.id`, `effect` in the perk vocabulary.
  - `RelicTuningRow(id, effect, durationTicks, radius, range, value1, value2, value3)`: `id` in `relics.id`, `effect` in the relic vocabulary. The six ported rows copy today's `classes` values exactly.
  - `GearCombatRow(id, healthPerPoint, damagePerPoint, armourPerPoint, speedPerPoint, rangedPerPoint, healingPerPoint, commandPerPoint, surroundedCount, surroundedRadius, commandRadius, tauntShare)`: one row `default`.
- `GearLooks.stack(Gear.Piece p)` returns the family or slot stand-in `ItemStack` with the item's name, the rarity colour (`gearRarities.border`), NBT `gg_uid`, `Unbreakable`, `HideFlags` 2 and an empty `AttributeModifiers` list. `GearLooks.uid(ItemStack)` reads it back (-1 if absent).
- Preflight refuses: unknown family, set, item, relic or effect id; a set without both 2- and 4-piece rows; a relic in `relics` without a tuning row; a perk on an item whose slot is not in `Gear.SLOTS`; any `gearCombat` count other than one row.

- [ ] **Step 1: Write failing preflight tests** (one per refusal above). Run `python -m unittest discover -s test-tools/standalone -p "test_preflight_gear.py"`. Expected: FAIL.
- [ ] **Step 2: Add the tables with draft rows** for every family, set, relic and perk named in `docs/drafts/gear-catalogue-2026-11-03.md`; mark the table notes "draft, Kort's balance review (g)". Add the rules.
- [ ] **Step 3: Implement `GearLooks`.**
- [ ] **Step 4: Run** `python tools/preflight.py` (expect `Preflight clean`), the unittest command (pass), `python tools/verify.py fast --offline` (pass).
- [ ] **Step 5: Commit**
```bash
git add sheets/design.json sheets/preflight.json tools/preflight.py test-tools/standalone/test_preflight_gear.py src/main/java/gg/gravesandglory/Spec.java src/main/java/gg/gravesandglory/GearLooks.java
git commit -m "Add the combat gear tables and the stand-in item looks"
```

### Task 2: `GearStats` and `GearEffects`, the pure rules

**Files:**
- Create: `src/main/java/gg/gravesandglory/GearStats.java`, `src/main/java/gg/gravesandglory/GearEffects.java`
- Create: `test-tools/standalone/GearStatsCheck.java`, `test-tools/standalone/GearEffectsCheck.java`
- Modify: `tools/verify.py:75`

**Interfaces:**
- `GearStats.Loadout(double[] totals, Map<String,Integer> setCounts, List<Spec.GearSetBonusesRow> bonuses, List<Spec.GearPerksRow> perks, String role, String mainFamily, String offFamily, List<String> relics)`.
- `static Loadout of(Profile p)`: totals = sum of `Gear.stats` over worn pieces; set counts over worn armour; bonuses = rows with `pieces <= count`; perks = rows for worn Epic+ items; role = `Gear.roleFor` with sheet-order ties; relics = `p.relics` padded to 3.
- `static Loadout empty()`; `static Spec.GearPerksRow perkFor(String item, String rarity)` (null below Epic).
- Conversions: `healthBonus(L)`, `attackBonus(L)`, `armourBonus(L)`, `speedBonus(L)`, `rangedFactor(L)`, `healingFactor(L)`, `commandBonus(L)`, each `total * gearCombat.<stat>PerPoint` (factors are `1 + ...`).
- `boolean has(L, String effect)`, `double value1(L, String effect)` (sum when stacked), `double radius(L, String effect)`.
- `GearEffects` (pure): `double surroundedFactor(L, int enemiesNear)` (applies when `enemiesNear >= surroundedCount`); `double tauntShare(L)`; `double auraRadius(L, double base, boolean bannerMain)` (`× family auraMult` for banner); `boolean secondChance(L, boolean usedThisMatch)`; `double relicCooldownFactor(L)` (`quickened_relic`); `double grudgeFactor(L, boolean attackerHitMeLast)`; `double lastLightFactor(L, float healthFraction)`; `double hawkeyeFactor(L, double distance)`.

- [ ] **Step 1: Write the failing checks.** Build profiles with `Gear.grantStarter` and pieces from sheet rows. Cases: totals equal summed `Gear.stats`; a 2-of-4 set gives only the 2-piece rows; 4-of-4 gives both; a Rare piece gives no perk, the Epic copy gives its row; role matches `Gear.roleFor`; each conversion equals total × sheet factor (1e-9); `surroundedFactor` is 1 below the count; `secondChance` false once used; `auraRadius` doubles only with the banner family's `auraMult` row; `empty()` gives all factors 1 and bonuses 0.
- [ ] **Step 2: Run** `python tools/verify.py fast --offline`; expected FAIL (classes missing).
- [ ] **Step 3: Implement.** Read every number from `Spec.GEARCOMBAT[0]`, `Spec.GEARSETBONUSES`, `Spec.GEARPERKS`, `Spec.GEARFAMILIES`.
- [ ] **Step 4: Run again.** Expected `GearStatsCheck` and `GearEffectsCheck` pass.
- [ ] **Step 5: Commit** the 5 paths: `git commit -m "Turn worn gear into a loadout of stats, set bonuses and perks"`.

### Task 3: The kit (`equipPlayer` and `GearCombat.apply`)

**Files:**
- Create: `src/main/java/gg/gravesandglory/GearCombat.java`
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`equipPlayer` at :813; the three squad seat sites at :776, :780 and `Campaign:321`)
- Modify: `src/main/java/gg/gravesandglory/Commander.java` (`Squad` gains `GearStats.Loadout gear` and `String[] relics`; `applyPlayer` is deleted)
- Create: `test-tools/java/gg/gravesandglory/GearCombatChecks.java`; register `"gear-combat"` in `Phase1Checks.SUITES`

**Interfaces:**
- `GearCombat.apply(ServerPlayerEntity pl, GearStats.Loadout L)`: removes then adds fixed-UUID modifiers for max health, attack damage, armour, movement speed and attack speed (from `gearFamilies.attackSpeed`), then heals to max.
- `GearCombat.kit(ServerPlayerEntity pl, Profile p)`: clears the inventory; hotbar slot 0 = `GearLooks.stack(main)`; off-hand = `GearLooks.stack(off)`, or arrows when the off-hand family is a quiver and the main is a bow (otherwise arrows in slot 9 for a bow); armour slots from the worn armour pieces; selected slot 0.
- `equipPlayer` calls `kit` then `apply` with `GearStats.of(profile)`; seat sites set `squad.gear` and `squad.relics`. A bot seat uses `Loadout.empty()` until part (e).
- During battle, an END_SERVER_TICK keeps each commander's `selectedSlot` at 0.

- [ ] **Step 1: Write the real-client checks:** `p1-gear-combat-kit` (after practice-battle start the main hand's `GearLooks.uid` equals the equipped main uid; armour matches); `p1-gear-combat-stats` (max health equals base + `healthBonus`); `p1-gear-combat-respawn` (after two downs, max health is unchanged, Review Focus 2); `p1-gear-combat-empty` (a profile with nothing equipped gets the starter kit, Review Focus 1).
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Run** fast (pass), then the suite with `GRAVES_PHASE1_CHECKS=gear-combat` as foundation Task 8 Step 6. Expected `pass p1-gear-combat-suite`, no `fail ` line.
- [ ] **Step 4: Commit** the listed paths: `git commit -m "Arm the commander from the gear they wear"`.

### Task 4: Weapon behaviours

**Files:**
- Create: `src/main/java/gg/gravesandglory/mixin/ShieldCooldownMixin.java` (`PlayerEntity.damageShield`), `src/main/java/gg/gravesandglory/mixin/BowDrawMixin.java` (`@ModifyVariable` on `BowItem.onStoppedUsing`'s use-time)
- Modify: `src/main/resources/gravesandglory.mixins.json`
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (`allowDamage` player-hit block at :1121: sweep hook; `UseItemCallback` for the staff), `src/main/java/gg/gravesandglory/GearCombat.java`
- Modify: `test-tools/java/gg/gravesandglory/GearCombatChecks.java`

**Interfaces:**
- `GearCombat.onPlayerHit(ServerPlayerEntity pl, LivingEntity target, float amount)`: greatweapon sweep deals `sweepShare × amount` to other enemies within `sweepRadius`, once per swing.
- Shield: a block puts the shield on `cooldownTicks` item cooldown.
- Bow: draw progress × family `drawMult` × `draw_speed` bonus.
- Staff: use raycasts `range` to a friendly troop or player and heals `healBase × healingFactor`, then item cooldown `cooldownTicks`.
- Banner: aura radius via `GearEffects.auraRadius` wherever the commander aura radius is read today (`Commander.factor`).
- `playerFactor` multiplies in `rangedFactor` (projectiles), `grudgeFactor`, `hawkeyeFactor` and `lastLightFactor`.

- [ ] **Step 1: Add the failing checks:** `p1-gear-combat-sweep` (two dummies within radius both lose health from one hit; one outside does not), `p1-gear-combat-shield` (a blocked hit leaves the shield on cooldown), `p1-gear-combat-bow` (with a `draw_speed` loadout an arrow fired after N ticks is faster than without), `p1-gear-combat-staff` (a hurt troop in range gains `healBase` health), `p1-gear-combat-banner` (aura reaches a troop at base radius × `auraMult` - 0.5).
- [ ] **Step 2: Implement**, then run fast and the suite. Expected all pass.
- [ ] **Step 3: Commit** the listed paths: `git commit -m "Give each weapon family its sweep, block, draw, heal or aura"`.

### Task 5: Relic slots, keys 1 to 3, `relics_v1` and the HUD (ported effects)

**Files:**
- Create: `src/main/java/gg/gravesandglory/RelicSlots.java`, `test-tools/standalone/RelicSlotsCheck.java`, `src/main/java/gg/gravesandglory/RelicEffects.java`
- Modify: `src/main/java/gg/gravesandglory/Commander.java` (`ACTIVES` move to `RelicEffects`; `Active.start` takes a `Spec.RelicTuningRow` instead of a `ClassesRow`; Squad's `abilityReadyAt`/`abilityUntil`/`abilityData` become a `RelicSlots`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java` (ORDER `relic:<n>` replaces `ability` at :829; state_v9 ability field from slot 0 at :1210; new S2C `relics_v1` sent each second and on change)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (remove the `ability` binding; in START_CLIENT_TICK during battle, consume `options.hotbarKeys[0..2]` and send `relic:<n>`), `src/main/java/gg/gravesandglory/BattleHud.java` (:519-524, three slots), `src/main/java/gg/gravesandglory/UiMath.java` (:702, :839 layouts for three slots)
- Modify: `test-tools/java/gg/gravesandglory/Phase2Checks.java`, `AbilityLevelChecks.java`, `DuelSimulation.java` (drive relics instead of classes), `GearCombatChecks.java`
- Modify: `tools/verify.py:75`

**Interfaces:**
- `RelicSlots` (pure): `long[] readyAt, until; double[] data; boolean tryStart(int slot, long now, int cooldownTicks, int durationTicks)`; `int readyIn(int slot, long now)`; `boolean active(int slot, long now)`.
- `relics_v1` payload: 3 × (`String id`, `int readyInTicks`, `int activeTicks`).
- Cooldown = `relics.cooldownTicks × GearEffects.relicCooldownFactor`.

- [ ] **Step 1: Write `RelicSlotsCheck`** (start once, refused while cooling, independent slots, `readyIn` counts down) and the real-client checks `p1-gear-combat-relic-keys` (key 2 in battle sends `relic:1` and the selected slot stays 0; outside battle key 2 selects slot 1, Review Focus 4) and `p1-gear-combat-ported` (each ported effect gives the same numbers the old Phase2 class checks asserted, Review Focus 3).
- [ ] **Step 2: Run** fast; expected FAIL.
- [ ] **Step 3: Port and wire.** Move the six bodies unchanged except reading `RelicTuningRow` fields where they read `ClassesRow` fields (`durationTicks`, `radius`, `range`, `value1..3`).
- [ ] **Step 4: Run** fast, then `gear-combat`, `phase2` and the ability-level suites. Expected all pass; the HUD shots at 1920x1080 and 2560x1440 show three slots without overlap (rubric 9).
- [ ] **Step 5: Commit** the listed paths: `git commit -m "Fire relics on keys 1 to 3 with a three-slot HUD"`.

### Task 6: New relic, set-bonus and perk effects

**Files:**
- Modify: `src/main/java/gg/gravesandglory/RelicEffects.java`, `GearCombat.java`, `GravesAndGlory.java` (`allowDamage` hooks: `takenFactor` × `surroundedFactor`; commander-downed branch checks `secondChance`), `Tactics.java` (:105-110: troops near a `taunt_aura` commander pick them with `tauntShare`), `Combat.java` (`ENTITY_LOAD` sets arrow pierce level for `pierce`)
- Modify: `test-tools/java/gg/gravesandglory/GearCombatChecks.java`

**Interfaces:**
- Relic bodies for `dash`, `shove`, `heal_pulse`, `volley`, `self_heal`, `war_cry`, `iron_skin`, each reading only its tuning row.
- Set bonuses `heal_over_time`, `cleanse`, `command_radius`, `formation_charge` tick from `GearCombat.tick(Battle)` once per second.
- Perks `heavy_hand` (knockback), `oathkeeper` (troop morale or damage in command radius), `trailblazer` (speed on roads or out of combat), per their `gearPerks` text.

- [ ] **Step 1: Add one failing real-client check per effect id** (`p1-gear-combat-<effect>`), each asserting the sheet value moves the measured number (health, distance, damage taken, target choice, pierce level).
- [ ] **Step 2: Implement**, run fast and the suite. Expected all pass.
- [ ] **Step 3: Commit** the listed paths: `git commit -m "Add the new relics, set bonuses and perks"`.

### Task 7: Class removal, the reader audit and QA rows

**Files:**
- Modify: `sheets/design.json` (delete `classes`; keep the legacy class ids as `gearSets.classes`), `tools/preflight.py`, regenerated `Spec.java` and `sheets/preflight.json`
- Modify: `Campaign.java` (:175, :223, :237 delete `class:`, :321, :701 bot cost factor 1, :997 lobby row sends role), `CampaignLobbyScreen.java` (class cards removed), `WarTableLobby.java` (:100, :166-168, :296, :431), `WarTablePages.java` (:666-702, :1158-1166), `RecruitmentScreen.java` (:88, :90, :102), `Veterans.java` (:406), `Abilities.java` (:552 range bonus 0), `BattleMode.java` (:80 point gold 0), `Combat.java` (:106 commander factor from the loadout), `Houses.java` (:220 grave_bloom from its relic tuning row), `GravesAndGlory.java` (:973 forward_supply from its tuning row; respawn from `R.respawnSeconds`), `Profile.java` (:95 sanitize commander against `gearSets.classes`), `ProfileMigration.java` (:66 `CLASS_MAP` kept for old saves)
- Modify: `test-tools/java/gg/gravesandglory/LevelChecks.java`, `LobbyChecks.java`, `CampaignChecks.java`, `test-tools/standalone/HousesCheck.java`, `ProfileMigrationCheck.java`
- Modify: `src/main/resources/assets/gravesandglory/lang/en_us.json`, `docs/QA-MATRIX.md` (rows for each `p1-gear-combat-*` id), `docs/HANDOFF.md`, `docs/BACKLOG.md`

- [ ] **Step 1: Audit.** Run `rg -n "CLASSES|ClassesRow|Commander\.row|byActive|covers\(|applyPlayer|trainingFactor|costFactor|rangeBonus|pointGoldBonus|respawnSeconds|\"class:\"|\.commander\b" src test-tools sheets`. Each hit is deleted, rewired to the loadout or relic tuning, or kept with a one-line reason in the commit body (migration only).
- [ ] **Step 2: Text audit.** `rg -n -i "class|marshal|warlock|warden|quartermaster|reaver|champion" sheets/design.json src/main/resources docs/discord docs/QA-MATRIX.md`; rewrite player-facing class text to gear and relics.
- [ ] **Step 3: Run** `python tools/preflight.py`, `python tools/verify.py fast --offline`, then the `gear-combat`, `phase2`, lobby and campaign suites, then `python tools/verify.py game --offline`. Expected all pass; Review Focus 5's `rg` returns only `ProfileMigration` hits.
- [ ] **Step 4: Visual check** of the lobby and War Table pages that lost class cards at 1920x1080 and 2560x1440 (rubric 9): no empty gaps, role shown where class was.
- [ ] **Step 5: Commit** the listed paths: `git commit -m "Remove classes and rewire every commander reader to gear and relics"`.
