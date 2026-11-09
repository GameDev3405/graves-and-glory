# Chapter 1 outline: "The Oath" (Gilded Order, Lord Marshal Aldric)

Status: Draft for Kort's review, updated 2026-11-09 with his decisions and the names in docs/lore/names.md. Follows `docs/drafts/lore-bible-2026-11-09.md` and the approved design `docs/superpowers/specs/2026-11-09-lore-reboot-design.md`. Calls I made alone are marked **[call]**. Nothing here changes mechanics, rosters or map layouts.

**Superseded in part.** The battle table and the battle-level text here are superseded by `docs/lore/chapter-1-battles.md` (approved by Kort, 2026-11-09), which ships in 1.0; the table below now mirrors it. The pages, stills and scenes are kept as the script for a later story layer (`PLAN-TO-1.0.md` keeps the storybook out of 1.0). Where the two differ, `chapter-1-battles.md` wins.

## What the chapter does

The player commands the Gilded Order, the Concord's army, sworn to the Charter. The chapter runs the ring in one direction: the first half answers the wrong done to the Gilded (the Tidecallers drowned their coast towns), the second half is the wrong the Gilded commit (the hunt of the Wildmoot). The player ends the chapter having done the thing the Charter was meant to forbid, without anyone calling it that.

Hints: spells sputter on Gilded casters; a quarry uncovers a carved stone shape. Nobody names the giants. Characters blame sabotage and bad stone.

Aldric is the lead and speaks for the silent commander's House. The only named character is Aldric; supporting figures are titled, not named (the Quartermaster, the Charter-reader, a quarryman, a Tidecaller raider). The Wildmoot's Old Horn appears in battle 6 and is carried into chapter 2.

## Shape

Six battles, fixed difficulty and an order lock, ids `oath-1` to `oath-6`. Maps are the five shipped maps, reused where needed. Kort designs and signs off all map layouts; the relics (a carved stone at a Kingsfield quarry cut, a stone hand on Rivergate, a statue on Ironhold) go into his map designs. In 1.0 the player fields their own collection banner, with Gilded banners beside them.

| # | Battle | Map | Mode | Difficulty | Enemy | Allies | Tactic |
|---|---|---|---|---|---|---|---|
| 1 | The Drowned Crossings | Rivergate Valley | Domination (three points), 1v1 | Easy | Tidecallers | none | Concentrate: one point at a time, fight the river from the bank |
| 2 | The Moon Shrine Watch | Hollowwood Reach | Survival, 2v2 | Easy | the bound dead (waves) | Gilded | Hold a position: shields front, archers behind, no chasing |
| 3 | Smoke at Ironhold | Ironhold Siege | Siege (attack), 2v2 | Normal | Tidecallers (hold the gate) | Ironclad (engines) | Combined arms: screen the engines that break the gate |
| 4 | The Hunters' Road | Frostmarch Pass | Convoy (escort), 2v2 | Normal | Wildmoot | Gilded | Screen a moving objective: stay by the wagon |
| 5 | The Grazing Grounds | Hollowwood Reach | Domination (three points), 3v3 | Normal | Wildmoot | Gilded x2 | Squad cohesion against pack hunters |
| 6 | The Antlered Alpha | Kingsfield Crossroads | Domination (five points), 3v3 | Hard | Wildmoot (Old Horn) | Gilded x2 | Spread or concentrate; answer the Legendary's charge |

Bonuses (from `chapter-1-battles.md`): hold all three, lose at most 8, break the gate within 360 seconds, the wagon never rolls back, lose at most 12, hold all five. Battle 6's bonus is hold all five, not killing Old Horn: the `kill_unit` bonus only pays on a kill, and he withdraws alive for chapter 2.

Ironclad fight beside the Gilded in battle 3 because the Tidecallers hold the Ironhold gate in revenge for the poisoned rivers, which plants chapter 3 and chapter 4 without explaining them.

Modes follow the shipped bindings: Siege on Ironhold, Survival on Hollowwood, Convoy on Frostmarch, Domination anywhere. Survival's waves are the existing sheet (the dead rising at the moon shrine), so battle 2's enemy is the Skeleton King's bound dead, not Tidecallers, and never called Graveborn. Nobody names the king; that is the point of the page.

## Pages

### Page 1: The Drowned Coast
1. `c1_coast` (book, three stills). The coast towns under the tide; the Charter on a table; Aldric reading a roll of names. Caption: "The tide took eleven towns. The Charter promised to answer it." Built as sets in the world, shot through the game camera, never painted.
2. `c1_charter` (card item, archive). The Concord Charter's one clause. Draft text: "The realms bind themselves, each to each. Every person under this Charter shall be kept from harm. Every wrong done to a person under this Charter shall be answered." Beneath it: "The Charter does not say who is a person." Nobody in the game points at that line until chapter 2.
3. `c1_roll` (card, optional, the Charter-reader). The roll of the coast towns. Tengervald is struck from the relief list, in plain ink, by an old hand. First hint that the Concord looked away.

### Page 2: The River Road
1. `c1_b1_before` (stage, Rivergate). Aldric: "We take the crossings one at a time. We count what we find." The Tidecaller raiders hold the bridges.
2. `battle_1` The Drowned Crossings (Domination, Easy; lessons and the free roll stay).
3. `c1_b1_after` (stage). A Gilded mage's bolt sputters and dies in her hand. Aldric: "Damp powder. See that it is dry." Hint 1.

### Page 3: The Moon Shrine Watch
1. `c1_b2_before` (stage, Hollowwood, night). The graves at the edge of the realm are opening. The Charter-reader says the dead wear a levy's colours no one has worn in a lifetime.
2. `battle_2` The Moon Shrine Watch (Survival, Easy).
3. `c1_b2_after` (stage). The last of the dead lies still. Aldric reads the faded colours: Tengervald. "Tengervald was struck from the relief list a lifetime ago. Someone is still answering for it." Nobody says who. This is the Skeleton King's first mark in the game.

### Page 4: Smoke at Ironhold
1. `c1_b3_before` (stage, Ironhold). The Ironclad send engines. Their smelters are burning; the river below runs wrong colours. Aldric does not ask why. **[call]** This is chapter 3's wrong, seen from outside.
2. `battle_3` Smoke at Ironhold (Siege, Normal; Ironclad allies).
3. `c1_b3_after` (stage, the gate). The Ironclad smith thanks the Order and does not meet Aldric's eye. A trace of redstone glows in the wall and goes dark. A dying Tidecaller raider says one name, once: the Admiral. **[call]** That is chapter 4's hook.

### Page 5: The Order From Highheart
1. `c1_orders` (card, the war table). A rider from Highheart: the council orders the Wildmoot grazing lands cleared so the quarry road can open. Aldric reads it twice. "This is the hunt again." He does not refuse. **[call]** The player can only continue; no choice UI.
2. `c1_archive_hunt` (card item, archive). A hunting tally of centuries. The last line is in Aldric's own hand.

### Page 6: The Hunters' Road
1. `c1_b4_before` (stage, Frostmarch). The wagons are loaded with hunters. Aldric: "Keep them to the road."
2. `battle_4` The Hunters' Road (Convoy, Normal).
3. `c1_b4_after` (stage, snow). The first Wildmoot dead the player has seen. A boar-kin hunter speaks: "We hid for a hundred years. We stopped hiding."

### Page 7: The Grazing Grounds
1. `c1_b5_before` (stage, Hollowwood, dusk). The grass is ripe; the order is to burn it.
2. `battle_5` The Grazing Grounds (Domination, Normal).
3. `c1_b5_after` (stage, smoke). The herds are gone; the fire does not catch. A quarryman, off to one side, says the stone is cold in the cut.

### Page 8: The Antlered Alpha
1. `c1_b6_before` (stage, Kingsfield, dawn). The quarry road is open. At the cut, a carved stone shape the size of a house sits half uncovered. Nobody names it. It is the carved stone at the Kingsfield quarry cut in Kort's map design.
2. `battle_6` The Antlered Alpha (Domination, five points, Hard). Old Horn is a Legendary in a bot banner with no boss rules.
3. `c1_b6_after` (stage). The Wildmoot line breaks and Old Horn withdraws; if the player's banner struck him, Aldric says so. He lives either way, for chapter 2. Aldric asks the quarryman what the stone is. "Stone." Aldric orders it covered.
4. `c1_charter_end` (book, one still, unskippable first time). Aldric alone in Highheart with the Charter. The lamps flicker once, and no one remarks. His line: "I swore to protect every person under the Charter. I would like to read it again." Chapter 2.

## Where the hints live

| Hint | Where | Reading |
|---|---|---|
| Spells sputter | `c1_b1_after`, then a visual and audio flicker on magic troops in battles 4 to 6 | Glimpur is leaving. Characters blame damp powder. |
| The carved stone shape | `c1_b6_before`, `c1_b6_after` | Orruvan's body, under the quarry road to Highheart. |
| Tengervald struck from the relief list | `c1_roll`, `c1_b2_after` | The Concord looked away from the Skeleton King's realm. |
| The Charter's gap | `c1_charter`, `c1_charter_end` | The Houses were never counted. |
| A relic on a map | A stone hand on Rivergate, a statue on Ironhold | The land is a body. |

## Voice directions

Aldric: formal, level, dutiful; a man who obeys the foundation even when it is hollow. The Quartermaster: dry. The Charter-reader: precise, old. A Tidecaller raider: drowned and failing, sure of the Admiral. A boar-kin hunter: loud, short lines. Old Horn: grief, not triumph.

## What this chapter leaves for others

- Chapter 2 opens on the hunt from the Wildmoot's side, starting at the grazing grounds.
- Chapter 3 opens on the Ironclad's smelters and the dead Wildmoot caravans.
- Chapter 4 shows the drowning of the coast towns from the Tidecallers' side.
- Act II: Aldric is the likeliest to accept the giants' terms. His line "I would like to read it again" is the seed.

## Open for Kort

None. The battles, the one named character, the Charter's wording and the map relics were decided on 2026-11-09 (`docs/lore/kort-decisions-2026-11-09.md`, `docs/lore/chapter-1-battles.md`).
