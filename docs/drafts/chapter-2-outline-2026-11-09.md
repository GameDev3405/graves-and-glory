# Chapter 2 outline: "The Long Grass" (Wildmoot, Old Horn)

Status: Draft for Kort's review, updated 2026-11-09 with his decisions and the names in docs/lore/names.md. Arrives in season 2, one month after 1.0. Same ground rules as `chapter-1-outline-2026-11-09.md` and `lore-bible-2026-11-09.md`; the approved design wins any disagreement. Calls I made alone are marked **[call]**. No mechanics, rosters or map layouts change.

## What the chapter does

The player commands the Wildmoot. Chapter 1 ended on the hunt; this chapter opens on the same ground from the other side, the ashes of the grazing grounds. The first half answers the wrong done to the Wildmoot (the Gilded hunted them for centuries). The second half is the wrong they commit: raiding the Ironclad's mining caravans and, in the finale, assaulting Ironhold. Tone is loud, feral grief and vengeance. No violet, no calm.

Hints: the herds flee uphill without being driven; the grass withers where nothing burned it. Folk myth: the Great Herd Beneath (Ommeru), told by the herd-mother as a bedtime story. Nobody names the giants.

Old Horn leads. **[call]** The only speakers are Old Horn, a boar-kin hunter and the herd-mother (titled, not named); hounds, spiders and bears do not speak. Brassjaw appears in battle 6 and is carried into chapter 3.

## Shape

Six battles **[call]**, fixed difficulty, order lock, shipped modes only (Domination anywhere, Convoy on Frostmarch, Siege on Ironhold). Season 2 has no new map: the chapter reuses shipped maps, with Kingsfield and Hollowwood dressed as the withering plains over Ommeru (`docs/lore/season-framework.md`).

| # | Battle | Map | Mode | Difficulty | Enemy | Allies | Tactic |
|---|---|---|---|---|---|---|---|
| 1 | The Ashes | Hollowwood Reach | Domination | Easy | Gilded | none | Pack hunting: three on one, strike from behind |
| 2 | The Stone Cut | Kingsfield Crossroads | Domination (five points) | Normal | Gilded | none | Speed: raid the weakest point, then the next |
| 3 | The Hunting Camp | Rivergate Valley | Domination | Normal | Gilded | none | Flanking a shield line instead of meeting it |
| 4 | The Caravan Road | Frostmarch Pass | Convoy (ambush) | Normal | Ironclad | none | Ambush: stop the wagon by striking its escort |
| 5 | The Smelter Road | Rivergate Valley | Domination | Normal | Ironclad | none | Fighting armour: pick targets, avoid the heavy line |
| 6 | The Forge Colossus | Ironhold Siege | Siege (attack) | Hard | Ironclad (Brassjaw) | none | Assaulting walls without engines; answer the Legendary |

**[call]** Battle 4 needs the Wildmoot on the defence (ambush) side of Convoy. The convoy spec fixes campaign battle 4 as an escort; I assume a campaign chapter can set the side per battle. If not, battle 4 becomes a Domination on Frostmarch.

**[call]** Battle 6's bonus must not be killing Brassjaw: the engine's `kill_unit` bonus only pays on a kill, and he withdraws alive for chapter 3. Use a stage bonus (break the gate in time) or a loss cap instead.

## Pages

### Page 1: The Ashes
1. `c2_ashes` (book, three stills). The grazing grounds a week after the burning; a herd-mother counting. Caption: "The grass was the only grave they ever left us." Built in the world, never painted.
2. `c2_horn` (card, the herd). Old Horn, first words: "We hid. We were polite about it. We are done being polite."
3. `c2_myth` (card, optional). The herd-mother tells the Great Herd Beneath: a herd so large it lay down and became the hills. **[call]** It is a bedtime story and nobody takes it literally.

### Page 2: The Ashes of Hollowwood
1. `c2_b1_before` (stage, Hollowwood). The Gilded camp sits on the burnt grazing grounds.
2. `battle_1` The Ashes (Domination, Easy).
3. `c2_b1_after` (stage). The grass at the edge of the burn is dying where no fire touched it. The herd-mother: "It is leaving. Something is pulling it." Hint 1.

### Page 3: The Stone Cut
1. `c2_b2_before` (stage, Kingsfield, dawn). The quarry road the Gilded opened across Wildmoot ground. A half-uncovered stone shape sits in the cut.
2. `battle_2` The Stone Cut (Domination, Normal).
3. `c2_b2_after` (stage). The boar-kin hunter touches the stone and goes still. "It is warm." In chapter 1 the stone was cold; it is waking. Nobody explains it.

### Page 4: The Hunting Camp
1. `c2_b3_before` (stage, Rivergate). The camp the hunters ran from. A tally of Wildmoot kills on a post.
2. `battle_3` The Hunting Camp (Domination, Normal).
3. `c2_b3_after` (stage). The tally burns. Old Horn does not watch it. **[call]** This is the chapter's last scene where the Wildmoot are only wronged.

### Page 5: The Herds Move
1. `c2_herds` (card, the herd). Herds are fleeing uphill with no hunter near. Old Horn: "Even the grass runs. I do not know from what." Hint 2.
2. `c2_caravan_plan` (card, the war table). Old Horn's grief turns outward: the Ironclad carry iron through the passes; they once took the high pasture. **[call]** The raids are revenge for an older wrong the Ironclad did, kept vague.

### Page 6: The Caravan Road
1. `c2_b4_before` (stage, Frostmarch, snow). The wagons of iron come through the pass.
2. `battle_4` The Caravan Road (Convoy, Normal).
3. `c2_b4_after` (stage). Ironclad drivers lie in the snow. A boar-kin hunter: "They did nothing to us this winter." Old Horn: "They did nothing to us last winter either."

### Page 7: The Smelter Road
1. `c2_b5_before` (stage, Rivergate). The Ironclad smelter road runs along the river; the water below it is the wrong colour. **[call]** The poisoned river is seen but not explained; it is chapter 3's and chapter 4's.
2. `battle_5` The Smelter Road (Domination, Normal).
3. `c2_b5_after` (stage). A burning wagon spills redstone dust that glows and goes dark. The herd-mother: "That is the same light as the stone."

### Page 8: The Forge Colossus
1. `c2_b6_before` (stage, Ironhold, dusk). The assault on the gate. Brassjaw stands in the keep.
2. `battle_6` The Forge Colossus (Siege, Hard). Brassjaw is a Legendary in a bot banner with no boss rules.
3. `c2_b6_after` (stage). The gate breaks and Brassjaw withdraws into the mountain; if the player's banner struck him, Old Horn says so. He lives either way, for chapter 3.
4. `c2_myth_end` (book, one still, unskippable first time). Old Horn on a hill at night with the herds streaming past him uphill, the grass flattening once as if in a wind no one feels. His line: "We hid for a hundred years. We have not stopped being afraid. Now the herds are afraid of something I cannot see." Chapter 3.

## Where the hints live

| Hint | Where | Reading |
|---|---|---|
| Grass withers | `c2_b1_after`, `c2_herds` | Glimpur leaving the grass. |
| Herds flee uphill | `c2_herds`, `c2_myth_end` | The land waking. |
| The warm stone | `c2_b2_after` | A giant's body, felt before it is understood. |
| Redstone glows and goes dark | `c2_b5_after` | The same glimpur, leaving. |
| The Great Herd Beneath | `c2_myth` | Ommeru, told as a story. |

## Voice directions

Old Horn: loud, short lines, grief that sounds like anger. The boar-kin hunter: blunt, one idea at a time. The herd-mother: low, practical, old. Brassjaw: heard once, a few words, flat.

## What this chapter leaves for others

- Chapter 3 opens on the Ironclad's burning caravans from the Ironclad's side.
- Chapter 1's quarry stone gets a second look; do not explain it.
- Act II: the Wildmoot fight with no terms offered.

## Open for Kort

1. Confirm the six battles and maps.
2. Confirm the Convoy ambush side is possible in the campaign engine.
3. Confirm that Old Horn is the only named character, with the herd-mother and hunter titled.
