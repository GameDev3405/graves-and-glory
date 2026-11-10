# Chapter 3 outline: "The Forge Below" (Ironclad, Brassjaw)

Status: Approved by Kort on 2026-11-10 (mana is called glimpur only). Updated 2026-11-09 with his decisions and the names in docs/lore/names.md. Arrives in season 3. Same ground rules as `chapter-1-outline-2026-11-09.md` and `lore-bible-2026-11-09.md`; the approved design wins any disagreement. Calls I made alone are marked **[call]**. No mechanics, rosters or map layouts change.

## What the chapter does

The player commands the Ironclad, forge people who are half machine. The first half answers the Wildmoot's raids on their caravans. The second half is the wrong they commit: their smelters have poisoned the Tidecallers' rivers, and the chapter ends with the Ironclad pressing downriver to keep their furnaces fed. The Ironclad are the giants' nearest kin, and the chapter never says so. Tone is blunt and practical; everyone is tired.

Hints: redstone veins pulse toward the mountains; the forges lose heat. Folk myth: the Makers (Kharuun), whom the Ironclad believe built the redstone. Nobody names the giants.

Brassjaw leads. **[call]** Speakers are Brassjaw and a smith (titled, not named). Admiral Vael appears in battle 6 and is carried into chapter 4.

## Shape

Six battles **[call]**, fixed difficulty, order lock, shipped modes only. Season 3 has no new map: the chapter reuses shipped maps, with Ironhold and Frostmarch dressed with pulsing redstone veins over Kharuun (`docs/lore/season-framework.md`).

| # | Battle | Map | Mode | Difficulty | Enemy | Allies | Tactic |
|---|---|---|---|---|---|---|---|
| 1 | The Burned Caravans | Frostmarch Pass | Convoy (escort) | Easy | Wildmoot | none | Escort against fast raiders: a slow wall beside the wagon |
| 2 | The Supply Yard | Ironhold Siege | Domination | Normal | Wildmoot | none | Holding ground with heavy troops and engines |
| 3 | The Dens | Hollowwood Reach | Domination | Normal | Wildmoot | none | Pushing into cover without being flanked |
| 4 | The Settling Weirs | Rivergate Valley | Domination | Normal | Tidecallers | none | Denying the water: hold the banks, not the river |
| 5 | The Slag Run | Kingsfield Crossroads | Domination (five points) | Normal | Tidecallers | none | Range against mist: when to close instead of shoot |
| 6 | The Drowned Tide | Rivergate Valley | Domination (three points) | Hard | Tidecallers (Vael) | none | All arms against the Legendary on her own ground |

**[call]** Battle 6 plays as three points: Rivergate has three, and five points belongs to Kingsfield, which chapter 3 already uses in battle 5. The river is Vael's ground.

**[call]** Battle 6's bonus must not be killing Vael: the engine's `kill_unit` bonus only pays on a kill, and she withdraws alive for chapter 4. Use hold all points or a loss cap instead.

## Pages

### Page 1: The Burned Caravans
1. `c3_caravans` (book, three stills). Ironclad wagons burning in the snow. Caption: "The mountain gave us the ore. We gave it back as iron. Nobody asked us to be thanked."
2. `c3_brassjaw` (card, the forge). Brassjaw, first words: "We dig. We build. We are not at war until someone makes it so."
3. `c3_makers` (card, optional, the smith). The Makers: the people who built the redstone before we did. **[call]** The smith tells it as a trade legend, not belief.

### Page 2: The Burned Caravans (battle)
1. `c3_b1_before` (stage, Frostmarch). The wagons will go again.
2. `battle_1` The Burned Caravans (Convoy escort, Easy).
3. `c3_b1_after` (stage). The Wildmoot retreat. The smith holds a piece of the wagon's redstone and it pulses once, toward the mountains. Hint 1.

### Page 3: The Supply Yard
1. `c3_b2_before` (stage, Ironhold). The yard is full and the forges are low. "The coal is good. The fire is not."
2. `battle_2` The Supply Yard (Domination, Normal).
3. `c3_b2_after` (stage). A forge-wall vein glows and fades. The smith: "Ask the mine." Hint 2.

### Page 4: The Dens
1. `c3_b3_before` (stage, Hollowwood). The Wildmoot dens that fed the raids.
2. `battle_3` The Dens (Domination, Normal).
3. `c3_b3_after` (stage). Brassjaw: "This ends it. It does not make it right." **[call]** The last scene where the Ironclad are only wronged.

### Page 5: The River Below
1. `c3_river` (card, the war table). Word from the downriver: the Tidecallers have stopped the smelter water. Brassjaw: "We kept the furnaces fed. We did not ask what the river thought."
2. `c3_archive_slag` (card item). A smelter log: "Slag to the river, as ever." The last entry is in a different hand and says "The fish are gone." **[call]** Chapter 4's wrong, from this side.

### Page 6: The Settling Weirs
1. `c3_b4_before` (stage, Rivergate). Weirs to hold back the Tidecaller assault.
2. `battle_4` The Settling Weirs (Domination, Normal).
3. `c3_b4_after` (stage). A Tidecaller prisoner: "You made the water sick. It does not forgive that."

### Page 7: The Slag Run
1. `c3_b5_before` (stage, Kingsfield). The slag works run to the river road.
2. `battle_5` The Slag Run (Domination, Normal).
3. `c3_b5_after` (stage). The works are saved and the river below runs thick and slow. Brassjaw looks at it too long.

### Page 8: The Drowned Tide
1. `c3_b6_before` (stage, Rivergate, fog). Admiral Vael waits on the water.
2. `battle_6` The Drowned Tide (Domination, three points, Hard). Vael is a Legendary in a bot banner with no boss rules.
3. `c3_b6_after` (stage). The Tidecaller line breaks and Vael withdraws into the river; if the player's banner struck her, Brassjaw says so. She lives either way, for chapter 4.
4. `c3_forge_end` (book, one still, unskippable first time). The Colossus's chest-plate redstone glows in time with a distant pulse, which then stops. His line: "The veins pulse. They pulse toward the mountain. Something is pulling." Chapter 4.

## Where the hints live

| Hint | Where | Reading |
|---|---|---|
| Redstone pulses toward the mountains | `c3_b1_after`, `c3_forge_end` | Glimpur leaving, drawn to Thruggsen. |
| Forges lose heat | `c3_b2_before`, `c3_b2_after` | The spire pulling glimpur home. |
| The Makers | `c3_makers` | Kharuun, told as a trade legend. |
| The river runs thick | `c3_b5_after` | A second, human, harm, seen before it is called one. |

## Voice directions

Brassjaw: blunt, slow, a man who builds. The smith: dry, practical, tired. A Tidecaller prisoner: fluid, bitter.

## What this chapter leaves for others

- Chapter 4 opens on the poisoned river from the Tidecallers' side.
- Act II: the Ironclad, hoping to be treated as kin, are refused.

## Open for Kort

1. Confirm the six battles and maps.
2. Confirm that Brassjaw is the only named character, with the smith titled.
3. The redstone palette pass for Forgewall.
