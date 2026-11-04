# Gear Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build part (c): the gear screen (layout A from the approved mockup), the blacksmith camp station with its Forge tab and roll reveal, tooltips, and the key that opens the screen anywhere in camp.

**Architecture:** All view logic (filters, sorts, compare arrows, tooltip lines, set progress, stat totals, grid scrolling, relic-slot targeting) lives in a pure class `GearView` that reads `GearWire.View` and the foundation's `Gear`/`Forge` lookups, so the standalone `*Check.java` suite tests it without Minecraft. A new War Table page class `WarTableGear` (page `gear`, tabs Armoury and Forge) draws it in canvas pixels, the same way `WarTableTroop` does, and records its drawn rects for the harness. Every click sends a foundation `gear:` or `forge:` ORDER command; the client never decides an outcome.

**Tech Stack:** Java 17, Fabric 1.20.1 (Loader 0.16.14, API 0.92.2+1.20.1), Python 3.12 preflight.

**Spec:** `docs/superpowers/specs/2026-11-03-commander-gear-design.md` and `docs/superpowers/specs/2026-11-03-commander-gear-mockup.html` (option A). Index: `docs/superpowers/plans/2026-11-03-commander-gear-index.md`. Foundation: `docs/superpowers/plans/2026-11-03-gear-foundation.md` (must be merged first).

## Global Constraints
- Spec.java comes from `python tools/preflight.py`; never hand-edit it. Commit the regenerated `Spec.java` and `sheets/preflight.json` with `sheets/design.json`.
- New pure classes go in the `names` list in `tools/verify.py:75`. New standalone checks are `test-tools/standalone/<Name>Check.java`, package `gg.gravesandglory`, printing `<Name>Check: N checks passed`.
- Quick loop: `python tools/verify.py fast --offline`. Never build in a checkout with a running Minecraft client.
- Commit with explicit paths, never `git commit -a`; leave `maps/*/design.json` uncommitted. End commits with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Do not push.
- **No catalogue data in code.** Item, set, rarity, relic and perk names, colours and numbers come from `Spec.GEARITEMS`, `Spec.GEARSETS`, `Spec.GEARRARITIES`, `Spec.RELICS`, `Spec.FORGE` (foundation Task 1) and the combat plan's tables. Tests build expectations from the sheet rows, never from literal catalogue names. The draft names (`docs/drafts/gear-catalogue-2026-11-03.md`) need Kort's approval before release; changing them is a sheet edit only.
- Layout A: top bar "Armoury" badge with Esc chip, Forge marks and gold on the right, gold rope divider. Left parchment card: Stats column (Health, Damage, Armour, Speed, Ranged, Healing, Command), commander in a gold frame, "Worn" row of 6 slots (Helm, Chest, Legs, Boots, Main hand, Off hand) with "Set: <name> N of 4", "Relics" row of 3 slots labelled keys 1, 2, 3. Right card: "Inventory N of M", filter tabs All, Helms, Chests, Legs, Boots, Weapons, Relics, a sort dropdown (Rarity, Newest, Slot, Set), an 8-column grid of rarity-bordered slots, and the hover tooltip.
- Rarity border colours are `gearRarities.border`. Click or right-click equips; shift-click breaks down, with a confirmation for Epic and above (rarity index ≥ 3).
- The screen never opens during a battle (`GravesAndGloryClient.battleActive()`).
- Player-facing UI acceptance: rubric 9, checked at 1920x1080, 2560x1440 and 3840x2160, plus hover, selected, disabled and refusal states (WORKFLOW Quality).

## Settled calls (log for Kort; reversible)
1. **Title "Armoury"**, as the approved mockup shows. `ArmouryTab` (troops, key U in battle) keeps its name; Kort may prefer "Gear" to avoid the clash. It is one string constant.
2. **Open key:** a new binding `key.gravesandglory.gear`, default **I**, in camp only. The blacksmith station opens the same page on its Forge tab.
3. **Blacksmith station:** `campStations` row `blacksmith`, page `forge`, npc `weaponsmith`, placed 6 blocks south of the quartermaster (x 10.5, y 77.0, z -51.5, yaw 90, half 1/1), label `Blacksmith / Forge & repairs`. The spot is checked in game and moved if not standable.
4. **Stats column shows the summed gear stats** (`Gear.stats` over worn pieces), one decimal, `—` for 0. Effective in-battle values (after the combat plan's conversion) can replace them later; that is a display change only.
5. **Grid shows every piece**, worn ones with an "E" mark; the counter counts all pieces (`pieces.size()` of `inventorySize`), matching the foundation's cap. Clicking a worn piece in the grid does nothing; clicking a worn slot on the left card unequips it.
6. **Relics:** clicking a relic in the grid puts it in the selected relic slot, else the first empty slot, else slot 0. Clicking a worn relic slot selects it; right-clicking it empties it. Relics cannot be broken down.
7. **Repair:** the Forge tab has "Repair all (N gold)" only. Per-piece repair stays a server command with no button.
8. **Epic+ breakdown confirmation** is a confirm strip on the right card (Break down / Keep), not a modal.
9. **Blacksmith lines:** a new sheet table `blacksmithLines(id, text)` with 3 draft lines; the Forge tab shows one, picked by `screenOpenCount % lines`.
10. **Commander preview and perk lines** wait for the combat plan's Task 1 (`gearLooks`, `gearPerks`, `gearSetBonuses` and `GearLooks.stack`). Until then the frame draws the client player as is and the tooltip has no perk line. These are Task 5 here.

## Review Focus
1. **A gear view that has not arrived** (`CampaignClient.gear == null`, first second in camp or an old server): the page draws "Loading your gear…" and every action button is inactive; no null pointer (Task 3).
2. **A sheet that changed under a save:** a piece whose item or rarity id is gone from the sheet is skipped in the grid and the tooltip, not drawn as a crash (Task 1, `GearView.visible`).
3. **120 pieces on a small window:** the grid scrolls by whole rows and the scroll offset clamps after a breakdown shrinks the list (Task 1, `scrollClamp`; Task 4 shot at 1080p with 120 pieces).
4. **The tooltip at the grid's bottom-right corner** flips left and up so it stays 12 px inside the canvas (Task 3 rect check).
5. **Rapid double clicks on Roll:** the second click inside 300 ms is dropped by `CampScreen.action`, and the reveal shows only when the snapshot's `forge.n` rises (Task 4).

---

### Task 1: GearView, the pure view logic

**Files:**
- Create: `src/main/java/gg/gravesandglory/GearView.java`
- Create: `test-tools/standalone/GearViewCheck.java`
- Modify: `tools/verify.py:75` (add `GearView`)

**Interfaces:**
- Consumes: `GearWire.View`, `Gear.Piece`, `Gear.SLOTS`, `Gear.ARMOUR`, `Gear.STATS`, `Gear.item`, `Gear.rarity`, `Gear.rarityIndex`, `Gear.relic`, `Gear.set`, `Gear.stats`, `Forge.rollCost`, `Forge.breakdownValue`, `Forge.breakdownDuplicates`, `Forge.repairCost`, `Forge.expandCost`, `Forge.duplicate`.
- Produces, in `final class GearView` (static, package-private):
  - `List<String> FILTERS = List.of("all","helm","chest","legs","boots","weapon","relic")`, `List<String> SORTS = List.of("rarity","newest","slot","set")`, `int COLUMNS = 8`.
  - `List<Gear.Piece> visible(GearWire.View v, String filter, String sort)`: pieces whose item and rarity resolve; `weapon` means slot main or off. Sorts: rarity (index desc, then item name, then uid), newest (uid desc), slot (`Gear.SLOTS` order, then rarity desc, then uid), set (gearSets sheet order, weapons last, then `Gear.SLOTS` order, then uid).
  - `List<String> visibleRelics(GearWire.View v)`: owned relics in sheet order.
  - `double[] totals(GearWire.View v)`: sum of `Gear.stats` over worn pieces, in `STATS` order.
  - `String statText(double value)`: `—` for 0, else one decimal with a trailing `.0` dropped.
  - `String setProgress(GearWire.View v)`: `"Set: <set name> N of 4"` for the set with most worn armour pieces (sheet order breaks ties), `""` with none worn.
  - `String counter(GearWire.View v)`: `"N of M"`.
  - `boolean worn(GearWire.View v, int uid)`.
  - `List<String> tooltip(GearWire.View v, Gear.Piece p)`: lines in order: name; `"<Rarity> <slot word> · <Set name> set"` (a weapon uses its family word and no set); each nonzero stat as `"+X <stat>"` joined by ` · `; the compare line(s); `"Durability N%"` or `"Broken: half stats until repaired"` at 0; `"Duplicate"` when `Forge.duplicate`; the footer `"Click: equip · Shift-click: break down"` (footer `"Worn"` for a worn piece).
  - `List<String> compare(GearWire.View v, Gear.Piece p)`: for each stat that differs from the piece worn in that item's slot, `"▲ +X <stat> vs worn"` or `"▼ -X <stat> vs worn"`; `["Nothing worn there"]` when the slot is empty; empty for a worn piece.
  - `boolean confirmBreakdown(Gear.Piece p)`: rarity index ≥ 3.
  - `int relicTarget(GearWire.View v, int selected)`: selected (0 to 2) if ≥ 0, else first empty slot, else 0.
  - `int rows(int count)`, `int scrollClamp(int offsetRows, int count, int visibleRows)`.
  - `Profile asProfile(GearWire.View v)`: a throwaway `Profile` holding the view's gear fields, for `Forge` previews.
  - `int duplicatesValue(GearWire.View v)`: `Forge.breakdownDuplicates(asProfile(v))`.
  - `int repairAllCost(GearWire.View v)`: sum of `Forge.repairCost` over damaged pieces.
  - `String rollRefusal(GearWire.View v, String slot)`: the client-side guess that greys the Roll button: short marks, full inventory (non-relic), or every relic owned; `""` when allowed. The server still decides.

- [ ] **Step 1: Write the failing `GearViewCheck`.** Build views from `Gear.grantStarter` on a `new Profile()` plus rolled pieces made from `Spec.GEARITEMS` rows (never literal names). Cases:
  - `visible` with filter `helm` returns only helm-slot items; `weapon` returns main and off; a piece with an item id not in the sheet is skipped.
  - Sort rarity: a legendary sorts before an epic; equal rarity sorts by name then uid. Newest: uid 9 before uid 3.
  - `totals` of the starter set equals the sum of `Gear.stats` of the 4 to 6 worn pieces (1e-9). A worn piece at durability 0 contributes half.
  - `statText(0) == "—"`, `statText(12.0) == "12"`, `statText(9.54) == "9.5"`.
  - `setProgress` of a starter profile is `"Set: " + set.name() + " 4 of 4"`; with 2 armour pieces of another set swapped in it names the set with more pieces.
  - `compare` of a higher-rarity copy of the worn helm gives only `▲` lines; of a lower one only `▼`; of a worn piece, empty; with the slot empty, `["Nothing worn there"]`.
  - `tooltip` of a durability-0 piece contains `Broken: half stats until repaired`; of a second copy of an owned (item, rarity), contains `Duplicate`.
  - `confirmBreakdown` is false for index 0 to 2 and true for 3 and 4.
  - `relicTarget` with slots `["x","",""]` and selected -1 gives 1; with all full gives 0; selected 2 gives 2.
  - `scrollClamp(10, 30, 4) == 0` (30 pieces = 4 rows), `scrollClamp(3, 120, 6) == 3`, `scrollClamp(20, 120, 6) == 9`.
  - `rollRefusal` with marks below `Forge.rollCost("helm")` is non-empty; at 130 pieces and size 120, `helm` is refused and `relic` is not.
  - `duplicatesValue` matches `Forge.breakdownDuplicates` on an equal `Profile`.

- [ ] **Step 2: Run it and confirm it fails.** Run `python tools/verify.py fast --offline`. Expected: java-compile fails, `GearView` not found.

- [ ] **Step 3: Implement `GearView`** with the signatures above. Stat words come from `Gear.STATS`; slot words from a small map over `Gear.SLOTS` (helm, chest, legs, boots, main hand, off hand).

- [ ] **Step 4: Run it and confirm it passes.** Expected: `GearViewCheck: PASS`; other checks unchanged.

- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/GearView.java test-tools/standalone/GearViewCheck.java tools/verify.py
git commit -m "Add the gear screen's view rules: filters, sorts, compare and tooltips"
```

### Task 2: Blacksmith station, the gear key and the camp page hook

**Files:**
- Modify: `sheets/design.json` (`campStations` row `blacksmith`; new table `blacksmithLines`), regenerated `Spec.java` and `sheets/preflight.json`
- Modify: `tools/preflight.py` (rule: `blacksmithLines` has at least 1 row, text non-empty and ≤ 90 characters)
- Modify: `test-tools/standalone/test_preflight_gear.py` (add `test_blacksmith_lines_required`)
- Modify: `src/main/java/gg/gravesandglory/GravesAndGloryClient.java` (binding `gear`, default `GLFW_KEY_I`; in the END tick, `while(gear.wasPressed())if(!battleActive())client.setScreen(new CampScreen("gear"));`)
- Modify: `src/main/java/gg/gravesandglory/CampScreen.java` (`alias`: `forge` → `gear` with the Forge tab; a `WarTableGear gear` field managed like `troopSheet`; `watched()` adds the gear key so it closes the page)
- Modify: `src/main/java/gg/gravesandglory/WarTable.java` (`PAGES` gains `gear`, using `HOME_TABLE` until its own table art exists)
- Modify: `src/main/resources/assets/gravesandglory/lang/en_us.json` (`key.gravesandglory.gear`: "Open gear")

**Interfaces:**
- Produces: `CampScreen.page("gear")` opens `WarTableGear` on tab `armoury`; `CampScreen.open("forge")` (the station) opens it on tab `forge`. `WarTableGear(CampScreen screen, String tab)` with `init()`, `tick()`, `render(DrawContext)`, `mouseClicked`, `mouseScrolled`, `keyPressed`, and `Map<String,int[]> rects()` (the boxes last drawn). Task 3 fills the body; this task adds a stub that draws the frame and top bar only.
- `Spec.BlacksmithLinesRow(String id, String text)` in `Spec.BLACKSMITHLINES`.

- [ ] **Step 1: Write the failing preflight test** `test_blacksmith_lines_required`: delete every `blacksmithLines` row; refuses with `blacksmithLines`. Run `python -m unittest discover -s test-tools/standalone -p "test_preflight_gear.py"`. Expected: FAIL (KeyError).
- [ ] **Step 2: Add the sheet rows** (settled calls 3 and 9; three draft lines for Kort, for example "Bring me marks and I'll bring you steel."), the preflight rule, the binding, the alias, the page hook and the stub page.
- [ ] **Step 3: Run** `python tools/preflight.py` (expect `Preflight clean`), the unittest command (expect pass), then `python tools/verify.py fast --offline` (expect pass, build included).
- [ ] **Step 4: Commit**
```bash
git add sheets/design.json sheets/preflight.json tools/preflight.py test-tools/standalone/test_preflight_gear.py src/main/java/gg/gravesandglory/Spec.java src/main/java/gg/gravesandglory/GravesAndGloryClient.java src/main/java/gg/gravesandglory/CampScreen.java src/main/java/gg/gravesandglory/WarTable.java src/main/java/gg/gravesandglory/WarTableGear.java src/main/resources/assets/gravesandglory/lang/en_us.json
git commit -m "Add the blacksmith station, the gear key and the gear page hook"
```

### Task 3: The Armoury tab (layout A)

**Files:**
- Modify: `src/main/java/gg/gravesandglory/WarTableGear.java`

**Interfaces:**
- Consumes: `GearView.*` (Task 1), `CampaignClient.gear` (foundation Task 7), `CampaignClient.profile.gold`, `CampScreen.action(String)`, `UiButton`, `DarkUi`, `UiTex`, `WarTable.column`.
- Produces: rect keys recorded each frame in `rects()`: `stats`, `frame`, `worn-<slot>` for each `Gear.SLOTS`, `relic-0..2`, `counter`, `tab-<filter>`, `sort`, `grid`, `cell-<i>` for visible cells, `tooltip` (while hovering), `confirm` (while open). Task 4 and the harness read them.
- Commands sent, all through `screen.action`: `gear:equip:<uid>` (click or right-click a grid piece not worn), `gear:unequip:<slot>` (click a worn slot), `gear:relic:<slot>:<relicId>` (click a relic in the Relics filter, slot from `GearView.relicTarget`), `gear:relic:<slot>:-` (right-click a worn relic slot), `forge:breakdown:<uid>` (shift-click; Epic+ only after the confirm strip's Break down).

- [ ] **Step 1: Lay out the 960x540 page** to match mockup A's proportions: left card about 400 px wide, right card the rest, 44 px slots with 6 px gaps, 8 grid columns, visible grid rows as many as fit above the card's bottom padding. Grid scroll by whole rows on the mouse wheel, clamped by `GearView.scrollClamp`. Tooltip at the hovered cell's right, flipped left and up to stay 12 px inside the canvas (Review Focus 4).
- [ ] **Step 2: Draw states:** loading (Review Focus 1), hover, selected relic slot (light outline), worn "E" mark, duplicate badge, broken piece (dimmed icon and red durability), confirm strip, and the latest server notice (`CampaignClient.notice`) under the grid. Item icons are rarity-coloured placeholders until the art track (f) supplies `UiTex` icons; draw `UiTex` when present.
- [ ] **Step 3: Build** with `python tools/verify.py fast --offline`. Expected: pass.
- [ ] **Step 4: Commit**
```bash
git add src/main/java/gg/gravesandglory/WarTableGear.java
git commit -m "Draw the gear screen's Armoury tab: stats, worn slots, relics and the inventory grid"
```

### Task 4: The Forge tab, the reveal, and the real-client screen suite

**Files:**
- Modify: `src/main/java/gg/gravesandglory/WarTableGear.java`
- Modify: `src/main/java/gg/gravesandglory/CampaignClient.java` (parse the snapshot's `forge` object like `lastRoll`: `static ForgeRoll forgeRoll` with `n, item, rarity, relic, duplicate, refund`)
- Create: `test-tools/java/gg/gravesandglory/GearScreenChecks.java`; register `"ui-gear"` in `Phase1Checks.SUITES` (line 41)
- Create: `docs/visual-references/06-gear-screen-layout-a.png` (a screenshot of mockup option A) and its row in `docs/visual-references/reference-manifest.json`, in the existing entry format

**Interfaces:**
- Forge tab content: six slot buttons over `Gear.ROLL_SLOTS`, the selected slot's cost (`Forge.rollCost`), a Roll button (`forge:roll:<slot>`, inactive with `GearView.rollRefusal` as its hover text), the pity line `"Epic or better within N rolls"` (`Spec.FORGE[0].pity() - view.pity()`, hidden for the relic slot), "Break down all duplicates (+N marks)" with a confirm (`forge:breakdown-duplicates`, inactive at 0), "Repair all (N gold)" (`forge:repair-all`, inactive at 0), "Add a row (N gold)" (`forge:expand`, inactive at max), and one blacksmith line.
- Reveal: when `forgeRoll.n` rises while the tab is open, a card shows for 1.5 s: rarity border fading in, then the name, the rarity, `Duplicate` or `+N Forge marks refunded`. Any click skips it.
- Rect keys added: `forge-slot-<slot>`, `roll`, `pity`, `dupes`, `repair-all`, `expand`, `reveal`.

- [ ] **Step 1: Write the real-client suite `GearScreenChecks`** (pattern: `LobbyChecks`, `window(c,w,h,0,true)` and `shot(c,name)`). Report ids:
  - `p1-gear-ui-open`: in camp the gear key opens page `gear`; the key again closes it; during a practice battle it does not open.
  - `p1-gear-ui-station`: `CampScreen.open("forge")` opens the Forge tab.
  - `p1-gear-ui-rects-<w>`: at 1920x1080, 2560x1440 and 3840x2160 every rect in `rects()` lies 12 px inside the canvas, and no two of `stats`, `frame`, `grid`, worn and relic slots overlap. Shots `gear-<w>-armoury`, `gear-<w>-forge`.
  - `p1-gear-ui-equip`: clicking a rolled helm in the grid makes `CampaignClient.gear.equipped().get("helm")` its uid within 100 ticks.
  - `p1-gear-ui-confirm`: shift-click on an Epic piece (server-granted) opens `confirm` and sends nothing until Break down; on a Common piece it sends at once.
  - `p1-gear-ui-roll`: with 200 marks granted, Roll on helm shows `reveal` within 100 ticks and the piece count rises by 1; two clicks 100 ms apart roll once (Review Focus 5).
  - `p1-gear-ui-120`: with 120 pieces granted at 1920x1080, scrolling to the end shows the last row; a shot `gear-1080-120`; after a breakdown from the last row the offset is clamped (Review Focus 3).
  - `p1-gear-ui-tooltip-corner`: hovering the bottom-right visible cell keeps `tooltip` inside the canvas (Review Focus 4).
  - `p1-gear-ui-suite`: the summary. Cleanup restores the profile from a JSON copy taken at the start.
- [ ] **Step 2: Implement the Forge tab, the reveal and the `forgeRoll` parse.**
- [ ] **Step 3: Run the suite** (no Minecraft client running, no build in progress):
```powershell
$root = "$PWD\work\gear-ui"; New-Item -ItemType Directory -Force "$root\run","$root\evidence" | Out-Null
"pauseOnLostFocus:false`nonboardAccessibility:false`nrenderDistance:8`nsimulationDistance:6`nguiScale:0" | Set-Content -Encoding utf8 "$root\run\options.txt"
$env:GRAVES_ROADMAP_TEST_ROOT = $root; $env:GRAVES_PHASE1_CHECKS = 'ui-gear'
.\gradlew.bat --no-daemon runClient -PlocalIntegration -I test-tools/isolated-checks.gradle
```
Expected: `work/gear-ui/evidence/local-checks.txt` contains `pass p1-gear-ui-suite` and no `fail ` line.
- [ ] **Step 4: Visual review.** Compare the shots with `06-gear-screen-layout-a.png` and the troop sheet references at normal and small sizes. Record findings (clipping, unreadable text, misaligned slots) and fix them; at most two correction rounds, then report what remains. Rubric 9.
- [ ] **Step 5: Commit**
```bash
git add src/main/java/gg/gravesandglory/WarTableGear.java src/main/java/gg/gravesandglory/CampaignClient.java test-tools/java/gg/gravesandglory/GearScreenChecks.java test-tools/java/gg/gravesandglory/Phase1Checks.java docs/visual-references/06-gear-screen-layout-a.png docs/visual-references/reference-manifest.json
git commit -m "Add the Forge tab and roll reveal, with the real-client gear screen suite"
```

### Task 5: Commander preview, perk and set-bonus text, and player docs (after combat Task 1)

Runs once `docs/superpowers/plans/2026-11-03-gear-combat.md` Task 1 is merged; rebase onto it first.

**Files:**
- Modify: `src/main/java/gg/gravesandglory/WarTableGear.java`, `src/main/java/gg/gravesandglory/GearView.java`, `test-tools/standalone/GearViewCheck.java`, `test-tools/java/gg/gravesandglory/GearScreenChecks.java`
- Modify: `docs/discord/start-here.md` (lines 6 and 16: commander gear instead of six classes), `docs/discord/roadmap.md` (the gear entry); Create: `docs/discord/how-to-play.md` (controls including the gear key and relic keys 1 to 3, roles and sets, the blacksmith loop). Drafts only; Kort posts them.
- Modify: `docs/HANDOFF.md`, `docs/BACKLOG.md`

**Interfaces:**
- Consumes: `GearLooks.stack(Gear.Piece)`, `Spec.GEARPERKS`, `Spec.GEARSETBONUSES`, `GearStats.perkFor(String item, String rarity)` (combat Task 1).
- Produces: `GearView.tooltip` gains the perk line `"<Perk name>: <text>"` after the stats for Epic and Legendary pieces; the Worn header's hover shows the set's 2- and 4-piece bonus texts with the reached ones bright. The frame draws a client-side preview entity (`OtherClientPlayerEntity` with the client player's skin) wearing `GearLooks.stack` of each worn piece, via `InventoryScreen.drawEntity`, following the mouse like the vanilla inventory.

- [ ] **Step 1: Add the failing `GearViewCheck` cases:** an Epic piece whose item has a `gearPerks` row has that row's name in its tooltip; a Rare piece of the same item does not; set-bonus lines for a 2-of-4 set mark the 2-piece row reached and the 4-piece row not.
- [ ] **Step 2: Run** `python tools/verify.py fast --offline`; expected FAIL on the perk line.
- [ ] **Step 3: Implement** the tooltip and set lines and the preview; add `p1-gear-ui-preview` to `GearScreenChecks` (equipping a helm changes the preview's head stack within 100 ticks; a shot `gear-1440-preview`).
- [ ] **Step 4: Run** fast (expect pass) and the `ui-gear` suite as in Task 4 (expect `pass p1-gear-ui-suite`).
- [ ] **Step 5: Write the docs drafts**, then update HANDOFF and BACKLOG (part (c) done; names still awaiting Kort).
- [ ] **Step 6: Commit**
```bash
git add src/main/java/gg/gravesandglory/WarTableGear.java src/main/java/gg/gravesandglory/GearView.java test-tools/standalone/GearViewCheck.java test-tools/java/gg/gravesandglory/GearScreenChecks.java docs/discord/start-here.md docs/discord/roadmap.md docs/discord/how-to-play.md docs/HANDOFF.md docs/BACKLOG.md
git commit -m "Show the commander preview, perk and set texts, and draft the gear player docs"
```
