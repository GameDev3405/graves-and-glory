# Gear Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build server-authoritative commander gear data. That covers the sheet catalogue, the Profile schema 4 save with its migration, a compact gear sync, the Forge marks payout, and the blacksmith's server rules (roll with pity, duplicates, breakdown, repair, inventory expansion), with every request validated on the server.

**Architecture:** The rules are pure classes (`Gear`, `Forge`, `GearWire`, `GearCommands`), so the standalone `*Check.java` suite compiles them without Minecraft, the same way `Profile` and `MatchRules` work today. `Campaign` only glues them in. It saves through the existing save-or-undo pattern (`before` JSON, then `save`, then `unsaved`). Gear leaves the 16,000-character camp snapshot completely and goes to the client on a new S2C channel, `gear_v1`, as a compact string. Requests ride the existing ORDER channel as `gear:` and `forge:` commands.

**Tech Stack:** Java 17, Fabric 1.20.1 (Loader 0.16.14, API 0.92.2+1.20.1), Gson 2.10.1, Python 3.12 preflight and unittest.

**Spec:** `docs/superpowers/specs/2026-11-03-commander-gear-design.md`. Index: `docs/superpowers/plans/2026-11-03-commander-gear-index.md`.

**Approval gates:** Kort approved this design on 2026-11-03. That covers:
- the **save gate**: Profile schema 3 to 4, the migration, the starter grant, and refunds;
- the **protocol gate**: the new S2C `gear_v1`, and the new `gear:` and `forge:` ORDER commands. `profile_v1` and `state_v9` are unchanged;
- the **gameplay gate**: Forge marks and the blacksmith economy.

The numbers in Task 1 are placeholders, and they go back to Kort at the balance step (g). Draft item, set and relic names need Kort's approval before (c) shows them. This plan adds no UI or art. Do not push or merge.

## Global Constraints
- Spec.java comes from `python tools/preflight.py`. Never hand-edit it. Commit the regenerated `Spec.java` and `sheets/preflight.json` together with `sheets/design.json`.
- Every new pure class goes into the `names` list in `tools/verify.py:75`, or the standalone checks cannot compile it.
- Every new check is `test-tools/standalone/<Name>Check.java`, in package `gg.gravesandglory`. It has a `main` that throws `AssertionError` on the first failure and prints `<Name>Check: N checks passed`, like `ProfileMigrationCheck`. `verify.py` discovers checks automatically.
- **Quick loop:** `python tools/verify.py fast --offline`. It stops at the first failing step and prints an excerpt, and the Java checks run before the Gradle build. For Python only: `python -m unittest discover -s test-tools/standalone -p "test_preflight_gear.py"`.
- Never run a build in a checkout with a running Minecraft client. Kort might be playing, so the runner's process guard must pass first.
- Commit with explicit paths, never `git commit -a`. Kort keeps uncommitted `maps/*.json`. End each commit message with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- These rules are server-authoritative. The client never states an item's stats, rarity or ownership. It names a uid or a slot, and the server checks it against the saved profile.
- Profile writes go through `Campaign.save` (`SafeFiles.writeWithBackup`), and a failed save undoes the change (`Campaign.unsaved`).
- Spec values: 5 rarities (common, uncommon, rare, epic, legendary; borders grey, green, blue, purple, orange). 4 sets × 4 armour slots. 3 relic slots. 60 slots to start, rows of 10, at most 120. Pity N = 10, and pity does not apply to relic rolls. A relic roll costs 1.5 × the roll cost. Breakdown returns 20/35/50/70/100% of the roll cost. Wear is 10% per match, and at 0% a piece works at half stats. A loss pays 0.5 and a bot-only match pays 0.5. A piece is never lost.

## Settled calls (log for Kort; reversible)
1. **Class refund is 0.** Classes were never sold: `class:<id>` (`Campaign.java:237`) is free. "Refund the gold actually paid" therefore pays nothing, and the migration records no refund.
2. **Starter relic:** a migrated profile also unlocks the relic that replaces its old class active, and equips it in slot 1.
3. **Starter weapons:** bruiser gets a sword and a kite shield, ranged a bow and a quiver, healer a staff, and commander a banner. Mapping: champion and reaver become bruiser; warden becomes ranged; warlock becomes healer; marshal and quartermaster become commander.
4. **One quality roll per piece** (0 to 100). It scales all of the item's base stats inside the rarity's range. This keeps the save and the wire compact.
5. **Roll slots** are helm, chest, legs, boots, weapon (any main-hand or off-hand item) and relic, which matches the screen's filter tabs.
6. **Requests ride the ORDER channel** as `gear:` and `forge:` commands, like `buy:` and `roll:`. That avoids a new C2S channel. Sync is a new S2C `gear_v1` compact string: about 2 KB at 120 items, so no paging is needed.
7. **Long March battles are exempt** from the bot-only halving. Every opponent there is a bot by design, and the spec gives the campaign "the same base". Practice and bot-filled custom matches are halved.
8. **Forge marks** pay only on a match that settles (`finish`), pro-rated by the seated share, like shards. A stopped or crashed match pays no marks and causes no wear. Marks use `credited` (a win with the majority seated), as victory gold does. A draw pays as a loss.
9. **Forge prices** live in a new `forge` sheet row. The marks payout columns go on the `campaign` row. The `economy` row is the in-battle barracks and stays as it is.
10. **Refusals:** a full inventory refuses an armour or weapon roll without charging. A relic roll is refused when every relic is owned. A duplicate relic refunds 50% of the relic roll cost. An equipped piece cannot be broken down. "Repair all" runs only when the gold covers all of it.
11. **"Break down all duplicates"** keeps one piece per (item, rarity): the equipped one if any, otherwise the highest quality, then the highest durability, then the oldest uid.
12. **An imported client profile** (`Campaign.importProfile`) never brings gear. Its gear fields are cleared, and it gets the starter set.
13. The `commander` field stays in schema 4. Class removal belongs to (d).
14. There is no blacksmith proximity check, matching recruitment rolls today. `gear:` and `forge:` are refused while seated in a running battle, like every other personal command.

## Review Focus
1. **Double requests:** two `forge:roll:helm` arrive with marks for only one. The second is refused, and marks never go negative (Task 5).
2. **A forged or malformed request** (`gear:equip:999999`, `forge:breakdown:abc`, `gear:relic:7:x`, an empty slot) is refused with a notice. No exception, no change (Task 8).
3. **A crash between migration writes:** reloading a schema-3 file whose `.v3.json` backup already exists keeps the backup unchanged, ends at schema 4, and does not grant the starter set twice (Task 4).
4. **Two-hander interplay:** equipping a greatsword with a shield worn unequips the shield, and equipping a shield with a greatsword worn unequips the greatsword. Neither piece is lost (Task 3).
5. **A save file with 130 pieces** (inventory over the cap, hand-edited or from a shrunk sheet) keeps every piece but refuses new rolls. The wire still fits its limit (Tasks 4, 5 and 7).

---

### Task 1: Gear sheets and preflight rules

**Files:**
- Modify: `sheets/design.json`. Add the tables `gearRarities`, `gearSets`, `gearItems`, `relics` and `forge`, and three columns on `campaign`.
- Modify: `tools/preflight.py`. Add the gear rules before `if errors:` (line 704).
- Create: `test-tools/standalone/test_preflight_gear.py`. Copy the harness from `test_preflight_rules.py:16-60` (temp copy, `run_preflight`, `refuses`).
- Regenerated: `src/main/java/gg/gravesandglory/Spec.java` and `sheets/preflight.json`.

**Interfaces:**
- Produces records (preflight names them from the table key):
  - `Spec.GearRaritiesRow(String id, String name, String border, int rollWeight, double statMin, double statMax, double breakdownPct, int repairGold, int power)`, held in `Spec.GEARRARITIES`;
  - `Spec.GearSetsRow(String id, String name, String role, String classes, String starterMain, String starterOff)`, in `Spec.GEARSETS`;
  - `Spec.GearItemsRow(String id, String name, String slot, String set, String family, int hands, double health, double damage, double armour, double speed, double ranged, double healing, double command)`, in `Spec.GEARITEMS`;
  - `Spec.RelicsRow(String id, String name, String role, String fromClass, int cooldownTicks, String text)`, in `Spec.RELICS`;
  - `Spec.ForgeRow(String id, int rollMarks, double relicRollPct, double relicDuplicatePct, int pity, int startSlots, int rowSlots, int maxSlots, int rowGold, int rowGoldStep, int wearPct, double brokenStatPct, int maxDurability, int relicSlots, int starterQuality)`, in `Spec.FORGE`;
  - `Spec.CampaignRow` gains `int forgeMarks, double lossMarksPct, double botOnlyMarksPct`.
- Double columns must be written with a decimal point in the first row (`4.0`). Preflight's `typ()` (line 706) reads the type from row 0.

- [ ] **Step 1: Write the failing tests** in `test_preflight_gear.py`:
  - `test_the_real_sheet_is_clean_with_gear`: return code 0, and the output contains `Preflight clean`.
  - `test_gear_rarities_order`: rename row 1 to `rare`; refuses with `gearRarities`.
  - `test_stat_ranges_rise`: set `epic.statMin` above `epic.statMax`; refuses with `statMin`.
  - `test_breakdown_rises`: set `rare.breakdownPct` to 0.1; refuses with `breakdownPct`.
  - `test_each_set_has_one_piece_per_armour_slot`: delete `bruiser_legs`; refuses with `bruiser` and `legs`.
  - `test_two_handed_only_main`: give `kite_shield` hands 2; refuses with `hands`.
  - `test_item_id_charset`: an id `bad.id`; refuses with `id`.
  - `test_starter_weapons_resolve`: `bruiser.starterMain` set to `nope`; refuses with `starterMain`.
  - `test_every_class_maps_to_one_set`: remove `reaver` from `bruiser.classes`; refuses with `reaver`.
  - `test_every_class_has_a_relic`: blank `stand_fast.fromClass`; refuses with `champion`.
  - `test_forge_slots`: `maxSlots` 115 (not start plus whole rows); refuses with `maxSlots`.

- [ ] **Step 2: Run them and confirm they fail**
Run: `python -m unittest discover -s test-tools/standalone -p "test_preflight_gear.py"`
Expected: FAIL (KeyError `gearRarities`).

- [ ] **Step 3: Add the sheet rows** (the numbers are placeholders for (g); the names are drafts for Kort)
  - `gearRarities`, in this order:
    - common: Common, `#9d9d9d`, 50, 1.0/1.1, 0.2, 20, 1
    - uncommon: Uncommon, `#3fae49`, 28, 1.15/1.3, 0.35, 30, 2
    - rare: Rare, `#3d7fd6`, 15, 1.35/1.55, 0.5, 45, 3
    - epic: Epic, `#9a4fd6`, 6, 1.6/1.85, 0.7, 65, 5
    - legendary: Legendary, `#e8892b`, 1, 1.9/2.2, 1.0, 90, 8
  - `gearSets`:
    - bruiser: Warden, `champion,reaver`, `arming_sword`, `kite_shield`
    - healer: Mender, `warlock`, `mending_staff`, `-`
    - ranged: Huntsman, `warden`, `hunting_bow`, `quiver`
    - commander: Herald, `marshal,quartermaster`, `war_banner`, `-`
    - `role` equals `id`.
  - `gearItems`:
    - 16 armour rows, `<set>_<slot>` for set in (bruiser, healer, ranged, commander) and slot in (helm, chest, legs, boots), with `family` `-` and hands 1;
    - 8 main-hand rows, set `-`: `arming_sword` and `falchion` (sword, 1); `greatsword` (greatsword, 2); `great_axe` (greataxe, 2); `hunting_bow` (bow, 1); `crossbow` (crossbow, 1); `mending_staff` (staff, 1); `war_banner` (banner, 1);
    - 2 off-hand rows, set `-`: `kite_shield` (shield, 1) and `quiver` (quiver, 1);
    - each row has a draft name and base stats for its role. Bruiser armour leans to health and armour, healer to healing, ranged to ranged and speed, and commander to command.
  - `relics`, with `fromClass` and cooldownTicks copied from the `classes` row:
    - stand_fast (champion), bloodrush (reaver), mark_prey (warden), rally_horn (marshal), grave_bloom (warlock), forward_supply (quartermaster);
    - six new relics with `fromClass` `-`: dash, shield_bash, heal_pulse, volley, war_cry and iron_skin;
    - every relic gets a `role` and one-line draft text.
  - `forge`: `forge`, 75, 1.5, 0.5, 10, 60, 10, 120, 500, 250, 10, 0.5, 100, 3, 50.
  - `campaign[0]`: `forgeMarks` 100, `lossMarksPct` 0.5, `botOnlyMarksPct` 0.5.

- [ ] **Step 4: Add the preflight rules.** Each error message names the table, the row id and the column, as the existing ones do:
  - gearRarities ids are exactly common, uncommon, rare, epic, legendary, in that order;
  - `0 < statMin <= statMax`, both rising by row; `breakdownPct` in (0, 1] and rising; `power` ≥ 1 and rising; `rollWeight` > 0; `border` matches `#[0-9a-f]{6}`;
  - item ids match `[a-z0-9_]{1,24}`; slot is in helm, chest, legs, boots, main, off;
  - an armour row's set is a gearSets id, and each set has exactly one row per armour slot;
  - a weapon row's set is `-`; `hands` is 2 only on a main row; every stat ≥ 0;
  - starterMain is a main item; starterOff is `-` or an off item; and starterOff is `-` when starterMain has 2 hands;
  - when a `classes` table exists, every class id appears in exactly one `gearSets.classes`, and in exactly one `relics.fromClass`;
  - relic ids match the item charset; `cooldownTicks` > 0;
  - forge: `(maxSlots - startSlots) % rowSlots == 0` and `maxSlots >= startSlots`; `pity` ≥ 1; `relicSlots` == 3; `0 <= starterQuality <= 100`; `0 < brokenStatPct <= 1`.

- [ ] **Step 5: Run the tests and preflight**
Run: `python tools/preflight.py`, then the unittest command above.
Expected: `Preflight clean`, and every test in `test_preflight_gear.py` passes. `Spec.java` now contains `public static GearItemsRow[] GEARITEMS`.

- [ ] **Step 6: Commit**
```bash
git add sheets/design.json sheets/preflight.json tools/preflight.py test-tools/standalone/test_preflight_gear.py src/main/java/gg/gravesandglory/Spec.java
git commit -m "Add the commander gear sheets and their preflight rules"
```

### Task 2: Gear core (pieces, stats, power, starter grant)

**Files:**
- Create: `src/main/java/gg/gravesandglory/Gear.java`
- Create: `test-tools/standalone/GearCheck.java`
- Modify: `tools/verify.py:75`. Add `Gear` to `names`.

**Interfaces:**
- Consumes: the Task 1 records.
- Produces, in `final class Gear`, all package-private and static unless noted:
  - `static final class Piece { int uid; String item; String rarity; int quality; int durability; }`. This is Gson-saved, so field names are save format.
  - `List<String> ARMOUR = List.of("helm","chest","legs","boots")` and `List<String> SLOTS = List.of("helm","chest","legs","boots","main","off")`. Roll slots: `List<String> ROLL_SLOTS = List.of("helm","chest","legs","boots","weapon","relic")`.
  - `List<String> STATS = List.of("health","damage","armour","speed","ranged","healing","command")`.
  - `Spec.GearItemsRow item(String id)`, `Spec.GearRaritiesRow rarity(String id)`, `Spec.RelicsRow relic(String id)`, `Spec.GearSetsRow set(String id)`, `Spec.ForgeRow forge()`. Each lookup returns null when unknown.
  - `int rarityIndex(String rarity)`: 0 to 4, or -1.
  - `double[] stats(Piece p)`: in `STATS` order. Each value is `base × (statMin + (statMax − statMin) × quality / 100)`, times `brokenStatPct` when durability is 0.
  - `int power(Profile p)`: the sum of `gearRarities.power` over equipped pieces. `double averagePower(Collection<Profile> humans)`: 0 when empty. This is the stub for matchmaking and bots.
  - `String roleFor(String commanderClass)`: the gearSets id whose `classes` lists it, else `"bruiser"`. `String starterRelic(String commanderClass)`: the relic whose `fromClass` is that class, else the first relic in sheet order.
  - `void grantStarter(Profile p)`: adds 4 Common armour pieces of `roleFor(p.commander)`, plus starterMain and starterOff when not `-`. Each piece is at `forge.starterQuality` and `forge.maxDurability`, with uids from `p.nextGearUid++`. It equips them all, unlocks `starterRelic` and puts it in relic slot 0, then sets `p.gearPower`. It ignores the inventory cap.
  - `Piece piece(Profile p, int uid)`: null when not owned.
- Profile fields this task reads, which Task 4 adds: `List<Gear.Piece> gear`, `Map<String,Integer> equipped`, `Set<String> relics`, `List<String> relicSlots`, `int nextGearUid`, `int gearPower`. **Add these field declarations in this task**, with defaults `new ArrayList<>()`, `new LinkedHashMap<>()`, `new LinkedHashSet<>()`, `new ArrayList<>(List.of("","",""))`, `1` and `0`, so it compiles. Task 4 owns the schema, the sanitize step and the migration.

- [ ] **Step 1: Write the failing `GearCheck`**
  - `stats`: a `bruiser_helm` legendary piece at quality 100 gives `health == base × 2.2` (within 1e-9). At quality 0 it gives `base × 1.9`. At durability 0 and quality 100 it gives `base × 2.2 × 0.5`.
  - `roleFor`: `"reaver"` gives bruiser, `"warlock"` gives healer, `"marshal"` gives commander, and `"nope"` gives bruiser.
  - `grantStarter` on `new Profile()` with commander `"warden"`:
    - 6 pieces: ranged armour ×4, `hunting_bow` and `quiver`, all common, quality 50, durability 100;
    - `equipped` holds all 6 slots, each pointing at the matching uid;
    - `relics == {"mark_prey"}` and `relicSlots == ["mark_prey","",""]`;
    - uids are 1 to 6, `nextGearUid == 7`, and `gearPower == 6`.
  - `grantStarter` with `"warlock"` gives 5 pieces and no off-hand.
  - `averagePower` of two profiles with powers 6 and 10 is 8.0, and of none is 0.0.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`
Expected: the `java-compile` step fails because `Gear` is not found.

- [ ] **Step 3: Implement `Gear`** with the signatures above. Profile gains the six fields listed (no logic yet), and `verify.py` names gains `Gear`.

- [ ] **Step 4: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`
Expected: `GearCheck: PASS`. The other checks still pass.

- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/Gear.java src/main/java/gg/gravesandglory/Profile.java test-tools/standalone/GearCheck.java tools/verify.py
git commit -m "Add gear pieces, stats, gear power and the starter grant"
```

### Task 3: Loadout rules (equip, unequip, relic slots)

**Files:**
- Modify: `src/main/java/gg/gravesandglory/Gear.java`
- Create: `test-tools/standalone/GearLoadoutCheck.java`

**Interfaces:**
- Produces three methods. Each returns `""` on success, or the refusal notice. On a refusal the profile is unchanged. On success `p.gearPower` is recomputed.
  - `static String equip(Profile p, int uid)`: the piece goes to its item's slot (`main` or `off` for weapons). A 2-handed main clears `off`. An off-hand worn with a 2-handed main clears `main`. Refusals: `"You do not own that piece."`. Durability 0 is allowed.
  - `static String unequip(Profile p, String slot)`: refuses a slot not in `SLOTS` with `"No such slot."`. Clearing an empty slot succeeds.
  - `static String setRelic(Profile p, int slot, String relicId)`: slot is 0 to 2, otherwise `"No such relic slot."`. `relicId` `"-"` empties the slot. An unowned or unknown relic is refused with `"You have not unlocked that relic."`. A relic already in another slot moves (the other slot empties).

- [ ] **Step 1: Write the failing `GearLoadoutCheck`** (Review Focus 4)
  - Equip a rolled `greatsword` while the starter `arming_sword` and `kite_shield` are worn: main is the greatsword, off is absent, and the shield and sword pieces stay in `p.gear`.
  - Then equip `kite_shield`: main is absent and off is the shield.
  - Equip uid 999 is refused, and `equipped` is unchanged.
  - Equip a `bruiser_chest` while `healer_chest` is worn: only chest changes.
  - `setRelic(p,1,"stand_fast")` while it sits in slot 0 gives `["","stand_fast",""]`. `setRelic(p,3,"dash")` is refused. `setRelic(p,0,"dash")` while not owned is refused.
  - `unequip(p,"cape")` is refused. `unequip(p,"helm")` leaves no `helm` key, and gearPower drops by 1.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`. Expected: java-compile fails because `equip` is not found.

- [ ] **Step 3: Implement** `equip`, `unequip` and `setRelic`.

- [ ] **Step 4: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`. Expected: `GearLoadoutCheck: PASS`.

- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/Gear.java test-tools/standalone/GearLoadoutCheck.java
git commit -m "Add server loadout rules: equip, two-handers and relic slots"
```

### Task 4: Profile schema 4 and the 3-to-4 migration

**Files:**
- Modify: `src/main/java/gg/gravesandglory/Profile.java` (`SCHEMA` at line 17, fields, and `sanitize` at lines 47-87)
- Modify: `src/main/java/gg/gravesandglory/ProfileMigration.java` (`upgradeFile` at lines 136-168; add `v3toV4`)
- Modify: `src/main/java/gg/gravesandglory/Campaign.java` (`freshProfile` at line 106; `importProfile` at lines 118-130)
- Create: `test-tools/resources/fixtures/profile-v3-veteran.json`. This is a real schema-3 shape: gold 950, commander `marshal`, the 10 Commons, battlesWon `["m1"]`, or the first real `campaignBattles` id.
- Create: `test-tools/standalone/GearMigrationCheck.java`. It uses Gson with the same settings as `ProfileFixtureCheck.JSON`.
- Modify: `test-tools/standalone/ProfileMigrationCheck.java` and `test-tools/standalone/ProfileFixtureCheck.java`. Change their schema-3 expectations to 4: lines 46 and 55 of the fixture check, and the "already schema 3 returns null" case.

**Interfaces:**
- Consumes: `Gear.grantStarter`, `Gear.power`, `Gear.item`, `Gear.rarity`, `Gear.relic`, `Gear.forge`.
- Produces:
  - `Profile.SCHEMA = 4`.
  - New fields: `int forgeMarks`, `int gearPity`, `int inventorySize = Spec.FORGE[0].startSlots()`, plus the Task 2 fields.
  - `static final String GEAR = "gear"`: the JSON key that `Campaign.forClient` strips (Task 7).
  - `static Profile ProfileMigration.v3toV4(Profile p)`: in place. It sets forgeMarks 0, gearPity 0 and inventorySize to startSlots. It runs `Gear.grantStarter(p)` only when `p.gear` is empty, then sets schema 4. Every schema-3 field is kept unchanged. The refund is 0 (settled call 1).
  - `upgradeFile`: same signature. It returns null only when the file is at schema 4 or later. Schema 1 runs `toV2`, then `v2toV3`, then `v3toV4`. Schema 2 runs `v2toV3` then `v3toV4`. Schema 3 is parsed with `parseV2` and runs `v3toV4`. The original bytes are kept under their schema name (`.v1.json`, `.v2.json` or `.v3.json`), and each intermediate image is also kept once. An existing backup is never replaced.
  - `Profile.sanitize` begins with `if (schema < 3) v2toV3(...)` and then `if (schema < 4) ProfileMigration.v3toV4(this)`. Then it cleans gear:
    - drop null pieces, unknown items and unknown rarities, and drop repeated uids (keep the first);
    - clamp quality to 0..100 and durability to 0..maxDurability; clamp forgeMarks to 0..1,000,000 and gearPity to 0..pity;
    - set inventorySize to startSlots..maxSlots, rounded down to a whole row;
    - set nextGearUid to at least max uid + 1;
    - `equipped` keeps only entries whose uid exists and whose item's slot equals the key. A 2-handed main drops `off`;
    - `relics` keeps only known ids. `relicSlots` becomes exactly `relicSlots` entries, each `""` or an owned relic, with no repeats;
    - `gearPower = Gear.power(this)`;
    - a piece is never dropped for being over the inventory size.
  - `Campaign.freshProfile()`: `new Profile()`, then `Gear.grantStarter`, then `sanitize()`.
  - `Campaign.importProfile`: after `portable.sanitize()`, clear `gear`, `equipped`, `relics` and `relicSlots`. Set `forgeMarks` and `gearPity` to 0 and `nextGearUid` to 1, then `Gear.grantStarter(portable)` (settled call 12).

- [ ] **Step 1: Write the failing `GearMigrationCheck`**
  - **mig4-fixture:** copy `profile-v3-veteran.json` to a temp dir and run the `ProfileFixtureCheck.load` steps. Then:
    - schema is 4;
    - gold is still 950, and battlesWon and owned are unchanged;
    - commander set: 4 `commander_*` pieces plus `war_banner`, all equipped;
    - relics are `{"rally_horn"}` in slot 0;
    - `.v3.json` is a byte copy of the fixture;
    - the file on disk contains `"schema": 4`.
  - **mig4-idempotent** (Review Focus 3): a second load changes no bytes. Then write the schema-3 fixture over the file again, keeping the existing `.v3.json`, and load: the backup is unchanged and there are 5 pieces, not 10.
  - **mig4-chain:** the `profile-v1-full.json` fixture ends at schema 4, keeps `.v1.json`, `.v2.json` and `.v3.json`, and carries a starter set.
  - **sanitize-gear:**
    - an unknown item, a repeated uid, quality 140 and durability -3 come out dropped, dropped, 100 and 0;
    - a stale `equipped.helm` pointing at a chest piece is dropped;
    - `relicSlots` `["dash","dash"]` with dash owned becomes `["dash","",""]`;
    - inventorySize 77 becomes 70, and 500 becomes 120;
    - a profile with 130 pieces keeps all 130 (Review Focus 5).
  - **fresh:** `Campaign.freshProfile` cannot be compiled standalone, so check `new Profile()`, then `Gear.grantStarter`, then `sanitize()` instead: 6 bruiser starter pieces and `stand_fast`.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`. Expected: `GearMigrationCheck` fails with `schema is 4, got 3`.

- [ ] **Step 3: Implement** the Profile fields and sanitize, `v3toV4`, the `upgradeFile` chain, `freshProfile` and `importProfile`. Update the Javadoc on `Profile` and `Campaign.profile` from schema 3 to schema 4.

- [ ] **Step 4: Update the old checks** (ProfileMigrationCheck and ProfileFixtureCheck) from schema 3 to 4. Do not weaken any other assertion.

- [ ] **Step 5: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`. Expected: `GearMigrationCheck`, `ProfileMigrationCheck`, `ProfileFixtureCheck` and `PersistedDataCorruptionCheck` all pass, and the build passes.

- [ ] **Step 6: Commit**
```bash
git add src/main/java/gg/gravesandglory/Profile.java src/main/java/gg/gravesandglory/ProfileMigration.java src/main/java/gg/gravesandglory/Campaign.java test-tools/resources/fixtures/profile-v3-veteran.json test-tools/standalone/GearMigrationCheck.java test-tools/standalone/ProfileMigrationCheck.java test-tools/standalone/ProfileFixtureCheck.java
git commit -m "Profile schema 4: gear fields, the 3-to-4 migration and the starter set"
```

### Task 5: Forge rules (roll with pity, duplicates, breakdown, repair, expand)

**Files:**
- Create: `src/main/java/gg/gravesandglory/Forge.java`
- Create: `test-tools/standalone/ForgeCheck.java`
- Modify: `tools/verify.py:75`. Add `Forge`.

**Interfaces:**
- Consumes: `Gear.*` and the Profile gear fields.
- Produces, in `final class Forge`:
  - `record Rolled(String refusal, Gear.Piece piece, String relic, boolean duplicate, int refund)`. On a refusal the other fields are null, false or 0.
  - `static int rollCost(String slot)`: `rollMarks`, or `Math.round(rollMarks × relicRollPct)` for `relic`.
  - `static Rolled roll(Profile p, String slot, RandomGenerator rng)`:
    - refuses an unknown slot (`"No such forge slot."`), short marks (`"A roll costs N Forge marks."`), a full inventory for a non-relic slot (`"Your inventory is full. Break down or expand first."`), and a relic roll with every relic owned (`"You own every relic."`);
    - otherwise it charges the cost. A **relic** draw is uniform over the relics in sheet order; an owned one gives `duplicate=true` and refunds `round(relicCost × relicDuplicatePct)` to forgeMarks;
    - an **armour or weapon** draw is uniform over the items whose slot matches (`weapon` means main or off). Rarity is drawn by `rollWeight`. When `gearPity >= pity`, only epic and legendary are drawn, by their weights. Quality is `rng.nextInt(101)`, and durability is max;
    - gearPity becomes 0 on epic or legendary, otherwise +1. A relic roll never touches gearPity;
    - the new piece takes `nextGearUid++`. `duplicate` is true when another piece already has the same item and rarity.
  - `static boolean duplicate(Profile p, Gear.Piece piece)`: another piece with the same item and rarity exists.
  - `static int breakdownValue(Gear.Piece piece)`: `floor(rollMarks × breakdownPct)`.
  - `static String breakdown(Profile p, int uid)`: refuses an unowned piece and an equipped one (`"Unequip it first."`). Otherwise it removes the piece and adds its value to forgeMarks.
  - `static int breakdownDuplicates(Profile p)`: applies settled call 11 and returns the marks gained.
  - `static int repairCost(Gear.Piece piece)`: `ceil(repairGold × (max − durability) / max)`. `static String repair(Profile p, int uid)`: refuses an unowned piece, a full one (`"It needs no repair."`) and short gold. `static String repairAll(Profile p)`: repairs every damaged piece when the gold covers the total, otherwise refuses with the total named.
  - `static int expandCost(Profile p)`: `rowGold + rowGoldStep × ((inventorySize − startSlots) / rowSlots)`. `static String expand(Profile p)`: refuses at maxSlots or on short gold; otherwise it adds rowSlots.
  - `static void wear(Profile p)`: every equipped piece loses `maxDurability × wearPct / 100`, floored at 0.

- [ ] **Step 1: Write the failing `ForgeCheck`**
  - **distribution:** use `new SplittableRandom(42)` and 20,000 helm rolls with gearPity forced back to 0 before each roll. Each rarity's share is within ±1.5 percentage points of `rollWeight / 100`. Each of the 4 helms is drawn within ±2 points of 25%.
  - **pity:** use a stub `RandomGenerator` whose `nextInt(bound)` always returns 0, which always draws common. Rolls 1 to 10 are common, with gearPity rising 1 to 10. Roll 11 is epic or legendary, and gearPity is then 0.
  - **pity-relic:** a relic roll at gearPity 7 leaves it at 7.
  - **cost:** a helm roll costs 75 and a relic roll costs 113 (`round(112.5)`).
  - **double request** (Review Focus 1): with 100 marks, the first helm roll succeeds and the second is refused. Marks end at 25, and the piece count rises by exactly 1.
  - **full** (Review Focus 5): with 130 pieces and inventorySize 120, a helm roll is refused and marks are unchanged. A relic roll is still allowed.
  - **relic duplicate:** with every relic but `dash` owned, draws continue until dash comes up. A draw of an owned relic refunds 56 (`round(113 × 0.5)`).
  - **relic all owned:** refused, and marks are unchanged.
  - **breakdown:** a rare piece returns 37 (`floor(75 × 0.5)`). An equipped one is refused. An unowned uid is refused.
  - **dupes:** 3 `bruiser_helm` epics (one equipped at quality 10, others at 90 and 60) and 2 `greatsword` commons (quality 40 and 40, uids 8 and 9). The equipped helm and greatsword uid 8 are kept. Marks gain `2 × 52 + 15`.
  - **repair:** an epic at durability 30 costs `ceil(65 × 70 / 100) = 46`. `repairAll` with gold one short of the total is refused, and nothing changes.
  - **expand:** at 60 it costs 500, at 70 it costs 750, and at 120 it is refused.
  - **wear:** equipped pieces go 100 → 90, unequipped pieces stay at 100, and a piece at 5 goes to 0.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`. Expected: java-compile fails because `Forge` is not found.

- [ ] **Step 3: Implement `Forge`.** Rarity draw: `rng.nextInt(totalWeight)` walked over the rows in order, so the always-0 stub draws common.

- [ ] **Step 4: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`. Expected: `ForgeCheck: PASS`.

- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/Forge.java test-tools/standalone/ForgeCheck.java tools/verify.py
git commit -m "Add the blacksmith rules: rolls with pity, duplicates, breakdown, repair and expansion"
```

### Task 6: Forge marks payout and match wear

**Files:**
- Modify: `src/main/java/gg/gravesandglory/MatchRules.java`. Add the method after `grossGold` (line 50).
- Modify: `src/main/java/gg/gravesandglory/Campaign.java`, in `finish` at lines 553-581.
- Modify: `test-tools/standalone/MatchRulesCheck.java`

**Interfaces:**
- Produces: `public static int forgeMarks(int base, boolean won, boolean botOnly, double lossPct, double botOnlyPct)`, which returns `(int) Math.floor(base × (won ? 1 : lossPct) × (botOnly ? botOnlyPct : 1))`, and 0 when base ≤ 0.
- In `finish`, for each participant:
  - `boolean botOnly = activeBattle == null && every b.squads entry on the other team has bot == true` (settled call 7);
  - `int marks = eligible ? MatchRules.prorate(MatchRules.forgeMarks(Spec.CAMPAIGN[0].forgeMarks(), credited, botOnly, lossMarksPct, botOnlyMarksPct), seated, seconds) : 0`;
  - `p.forgeMarks += marks`, and `if (eligible) Forge.wear(p)`. Both happen before `save(id)`, so a failed save undoes them with the gold;
  - report: `report.addProperty("forgeMarks", saved ? marks : 0)`;
  - the standing line appends `" / +"+marks+" Forge marks"` when marks > 0.
- The stopped or crashed payout path (`Campaign.java:514-520`) is unchanged.

- [ ] **Step 1: Write the failing MatchRulesCheck cases**
  - `forgeMarks(100,true,false,0.5,0.5) == 100`, `(100,false,false,...) == 50`, `(100,true,true,...) == 50` and `(100,false,true,...) == 25`;
  - `(0,true,false,...) == 0`, and `(75,false,true,0.5,0.5) == 18`;
  - `prorate(forgeMarks(100,true,false,0.5,0.5), 90, 180) == 50`.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`. Expected: java-compile fails because `forgeMarks` is not found.

- [ ] **Step 3: Implement** `MatchRules.forgeMarks` and the `finish` changes.

- [ ] **Step 4: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`. Expected: `MatchRulesCheck: PASS`, and the build passes.

- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/MatchRules.java src/main/java/gg/gravesandglory/Campaign.java test-tools/standalone/MatchRulesCheck.java
git commit -m "Pay Forge marks on a settled match and wear the worn gear"
```

### Task 7: Compact gear sync (gear_v1)

**Files:**
- Create: `src/main/java/gg/gravesandglory/GearWire.java`
- Create: `test-tools/standalone/GearWireCheck.java`
- Modify: `tools/verify.py:75`. Add `GearWire`.
- Modify: `src/main/java/gg/gravesandglory/GravesAndGlory.java`. Add the identifier beside lines 39-52. Call `campaign.syncGear(p)` right after `b.writeString(campaign.snapshot(p))` in the send loop (line 1206), and send it outside the STATE buffer.
- Modify: `src/main/java/gg/gravesandglory/Campaign.java`: `forClient` (line 927), `left` (line 196), and the new `syncGear`.
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (a receiver beside line 83) and `src/main/java/gg/gravesandglory/CampaignClient.java` (a field).

**Interfaces:**
- Produces:
  - `GearWire.LIMIT = 12000`.
  - `static String encode(Profile p)`. The format is `g1|marks|size|pity|power|e0,e1,e2,e3,e4,e5|relic,relic|s0,s1,s2|uid.item.r.q.d;uid.item.r.q.d`. Equipped uids follow `Gear.SLOTS` order, with 0 meaning empty. `r` is `Gear.rarityIndex`. An empty list is an empty field.
  - `record View(int forgeMarks, int inventorySize, int pity, int power, Map<String,Integer> equipped, List<String> relics, List<String> relicSlots, List<Gear.Piece> pieces)`, and `static View decode(String s)`. Decode returns null on any malformed input and never throws.
  - `GravesAndGlory.GEAR = new Identifier("gravesandglory","gear_v1")`: S2C, one `writeString(text, GearWire.LIMIT)`.
  - `Campaign.syncGear(ServerPlayerEntity p)`: when `ServerPlayNetworking.canSend(p, GEAR)` and `GearWire.encode(profile)` differs from `gearSent.get(uuid)`, it sends and remembers. `left(id)` removes the `gearSent` entry.
  - `Campaign.forClient(Profile)` also does `j.remove(Profile.GEAR)`.
  - `CampaignClient.gear` (a `GearWire.View`, initially null) is set by the client receiver through `readString(GearWire.LIMIT)` and `decode`. Part (c) reads it.

- [ ] **Step 1: Write the failing `GearWireCheck`**
  - **round trip:** a starter profile plus 3 rolled pieces: `decode(encode(p))` matches marks, size, pity, power, equipped, relics, relicSlots, and every piece's fields.
  - **size at 120** (spec QA): 120 pieces, each using the longest `gearItems` id and uids from 100000 up, with quality 100, durability 100, all 12 relics, and marks 1,000,000. `encode(p).length() <= GearWire.LIMIT / 2`.
  - **size at 130** (Review Focus 5): the same at 130 pieces stays at or under `LIMIT`.
  - **snapshot untouched:** with Gson (as in `ProfileFixtureCheck`), the profile JSON tree with `Profile.GEAR` removed has the same length for 0 pieces and for 120 pieces. Gear adds nothing to the 16,000-character snapshot.
  - **malformed:** `decode("")`, `decode("g1|x")`, `decode("g2|...")` and a piece with a rarity index of 9 all return null.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`. Expected: java-compile fails because `GearWire` is not found.

- [ ] **Step 3: Implement** `GearWire`, then the channel, `syncGear`, the `forClient` strip and the client receiver.

- [ ] **Step 4: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`. Expected: `GearWireCheck: PASS`, and `build` passes (the client and server classes compile).

- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/GearWire.java test-tools/standalone/GearWireCheck.java tools/verify.py src/main/java/gg/gravesandglory/GravesAndGlory.java src/main/java/gg/gravesandglory/Campaign.java src/main/java/gg/gravesandglory/GravesAndGloryClient.java src/main/java/gg/gravesandglory/CampaignClient.java
git commit -m "Sync gear on gear_v1 and keep it out of the camp snapshot"
```

### Task 8: Server-validated gear and forge commands, with the real-client check

**Files:**
- Create: `src/main/java/gg/gravesandglory/GearCommands.java`
- Create: `test-tools/standalone/GearCommandsCheck.java`
- Modify: `tools/verify.py:75`. Add `GearCommands`.
- Modify: `src/main/java/gg/gravesandglory/Campaign.java`:
  - the `personal` list at line 223;
  - dispatch after line 231;
  - a new `gear(ServerPlayerEntity, Profile, String)`;
  - a `forgeResults` map, added to `snapshot()` (line 890) as `"forge"`.
- Create: `test-tools/java/gg/gravesandglory/GearChecks.java`. Register it in `Phase1Checks.SUITES` (line 41) as `"gear"`.
- Modify: `docs/HANDOFF.md` and `docs/BACKLOG.md`. Mark the gear foundation done, then (c), (d) and (f) next, per the index.

**Interfaces:**
- Produces:
  - `record Outcome(String refusal, String notice, Forge.Rolled rolled)`.
  - `static Outcome GearCommands.apply(Profile p, String cmd, RandomGenerator rng)`. It parses:
    - `gear:equip:<uid>`, `gear:unequip:<slot>`, `gear:relic:<0-2>:<relicId|->`;
    - `forge:roll:<rollSlot>`, `forge:breakdown:<uid>`, `forge:breakdown-duplicates`, `forge:repair:<uid>`, `forge:repair-all` and `forge:expand`.
  - A bad number, an unknown verb or a missing part gives the refusal `"That gear request is not valid."`. `apply` never throws, and a refusal leaves `p` unchanged. On success, `notice` is the player-facing line, for example `"Rolled: <name> (<Rarity>)"`, `"+N Forge marks"`, `"Repaired for N gold"`, or `"Inventory: N slots"`.
  - `Campaign.gear(p, pr, cmd)`:
    1. Take `before = JSON.toJson(pr)` and call `GearCommands.apply(pr, cmd, rollRng)`.
    2. On a refusal, send `notice(refusal)` and stop.
    3. Otherwise, `if (!save(id)) unsaved(id, before, "That gear change")`.
    4. Else send `notice(outcome.notice)`. For a roll, put `{n, item, rarity, relic, duplicate, refund}` in `forgeResults`, with n counting up, as `lastRoll` does.
  - Then `syncGear(p)`.
  - `gear:` and `forge:` are added to the `personal` lock list.

- [ ] **Step 1: Write the failing `GearCommandsCheck`** (Review Focus 2)
  - `gear:equip:999999`, `forge:breakdown:abc`, `gear:relic:7:x`, `gear:relic:0`, `forge:roll:`, `forge:roll:cape`, `gear:`, `forge:frob` and `gear:equip:-1`: each returns the refusal, and the profile JSON (Gson) is byte-identical before and after.
  - `forge:roll:helm` with 75 marks succeeds, and the notice starts with `Rolled: `.
  - `gear:equip:<that uid>` succeeds.
  - `forge:breakdown:<starter boots uid>` is refused while equipped.
  - `forge:expand` with 500 gold gives `inventorySize == 70`.

- [ ] **Step 2: Run it and confirm it fails**
Run: `python tools/verify.py fast --offline`. Expected: java-compile fails because `GearCommands` is not found.

- [ ] **Step 3: Implement** `GearCommands` and the `Campaign` glue.

- [ ] **Step 4: Run it and confirm it passes**
Run: `python tools/verify.py fast --offline`. Expected: `GearCommandsCheck: PASS`, and the build passes.

- [ ] **Step 5: Write the real-client suite `GearChecks`.** It is a `Phase1Checks.Suite` that drives the client through `GravesAndGloryClient.send(...)` and edits the server through `Phase1Checks.server`, as `ReportChecks` does. Report ids:
  - `p1-gear-sync`: within 100 ticks of camp, `CampaignClient.gear` is not null, with 6 equipped and `relicSlots[0]` not empty.
  - `p1-gear-roll`: the server grants 200 marks, then the client sends `forge:roll:helm`. Within 100 ticks, `CampaignClient.gear.pieces` grew by 1, marks are 125, and the profile file on disk (re-read with Gson) agrees.
  - `p1-gear-forged`: the client sends `gear:equip:999999` and `forge:breakdown:abc`. The notice is `"That gear request is not valid."`, and the server profile is unchanged.
  - `p1-gear-snapshot`: the server gives the profile 120 pieces. The next `campaign.snapshot(p)` is pretty-printed (it contains `\n`) and has no `"gear"` key, and `CampaignClient.gear.pieces.size() == 120`.
  - `p1-gear-battle-lock`: while seated in a practice battle, `forge:roll:helm` gets `"Active battle choices are locked until the match ends."`.
  - `p1-gear-suite`: the summary.
  - Cleanup puts the profile back from a JSON copy taken at the start.

- [ ] **Step 6: Run the real-client suite.** It needs no running Minecraft client and a checkout with no build in progress.
```powershell
$root = "$PWD\work\gear-game"; New-Item -ItemType Directory -Force "$root\run","$root\evidence" | Out-Null
"pauseOnLostFocus:false`nonboardAccessibility:false`nrenderDistance:8`nsimulationDistance:6`nguiScale:3" | Set-Content -Encoding utf8 "$root\run\options.txt"
$env:GRAVES_ROADMAP_TEST_ROOT = $root; $env:GRAVES_PHASE1_CHECKS = 'gear'
.\gradlew.bat --no-daemon runClient -PlocalIntegration -I test-tools/isolated-checks.gradle
```
Expected: `work/gear-game/evidence/local-checks.txt` contains `pass p1-gear-suite` and no line starting with `fail `. Then run `python tools/verify.py game --offline` and expect `status: pass`, which shows the combat smoke is not regressed.

- [ ] **Step 7: Update HANDOFF and BACKLOG, then commit**
```bash
git add src/main/java/gg/gravesandglory/GearCommands.java test-tools/standalone/GearCommandsCheck.java tools/verify.py src/main/java/gg/gravesandglory/Campaign.java test-tools/java/gg/gravesandglory/GearChecks.java test-tools/java/gg/gravesandglory/Phase1Checks.java docs/HANDOFF.md docs/BACKLOG.md
git commit -m "Validate gear and forge requests on the server, with a real-client gear suite"
```
