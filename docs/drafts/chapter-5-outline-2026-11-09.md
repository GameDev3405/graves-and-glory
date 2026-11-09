# Chapter 5 outline: "The Rest" (Graveborn, the Hollow King)

Status: Draft for Kort's review, updated 2026-11-09 with his decisions and the names in docs/lore/names.md. Arrives in season 5, the end of Act I. Same ground rules as `chapter-1-outline-2026-11-09.md` and `lore-bible-2026-11-09.md`; the approved design wins any disagreement. Calls I made alone are marked **[call]**. No House mechanics or playable rosters change; the Bound Host's NPC roster is new content for season 5.

## What the chapter does

The player commands the Graveborn, rebels from the Skeleton King's army who broke the binding this year. They are distrusted by every House because they were once part of the king's army and because of what they did while bound. The chapter is about owning that, which is why it closes the campaign. It tells the Skeleton King's story (his realm was dying and the Concord looked away), shows the Graveborn's two wrongs as memories, and ends on the one fight every House must join.

Hints: the binding frays, and the rebellion itself is the hint. Folk myth: the Rest (Ysmorren), the belief that the dead are owed a rest they were denied. The finale ends with the spire's light, the first time the game says the land is alive.

The Hollow King leads, a mocking title the rebels gave their leader, the Skeleton King's defected general. **[call]** The only named character is the Hollow King; the Skeleton King (Halvard of Tengervald in the bible) stays unnamed in player text and appears in battle 6. Speakers are the Hollow King and a bound soldier (titled).

## Shape

Six battles **[call]**, fixed difficulty, order lock, shipped modes only. The enemy in battles 1, 5 and 6 is the Bound Host, the Skeleton King's army, which arrives in season 5 as an NPC enemy faction with its own roster. Allied banners in battle 6 are Wildmoot and Tidecallers, the two Houses the Graveborn wronged, who join anyway. Season 5 may ship a new map, Tengervald (Halvard's dying realm and its barrows, over Ysmorren). **[call]** It can replace the reused map for one battle, best battle 5, where the Host comes to rebind them.

| # | Battle | Map | Mode | Difficulty | Enemy | Allies | Tactic |
|---|---|---|---|---|---|---|---|
| 1 | The First Free Night | Hollowwood Reach | Survival | Easy | the Bound Host (waves) | none | Holding against numbers that do not tire |
| 2 | The Barrow Road | Kingsfield Crossroads | Domination (five points) | Normal | Gilded | none | Attrition: trading slow troops well |
| 3 | The Razed Dens | Frostmarch Pass | Domination | Normal | Wildmoot (memory) | none | Holding a line against pack hunters |
| 4 | The Sunk Fleet | Rivergate Valley | Domination | Normal | Tidecallers (memory) | none | Fighting at the water's edge |
| 5 | The Rebinding | Hollowwood Reach, or Tengervald **[call]** | Domination | Normal | the Bound Host | Wildmoot | Fighting beside an ally you do not command |
| 6 | The King's Host | Kingsfield Crossroads | Domination (five points) | Hard | the Bound Host | Wildmoot, Tidecallers | Coordinating three Houses across five points |

**[call]** Battle 6's bonus must not be killing the Skeleton King: the engine's `kill_unit` bonus only pays on a kill, and he withdraws alive for Acts II and III. Use hold all five points instead.

**[call]** Battles 3 and 4 are memories: the Hollow King tells them in a stage scene before each, and the player commands the Graveborn as the bound army. The scene frames them as wrongs, and the Graveborn say so aloud.

## Pages

### Page 1: The First Free Night
1. `c5_free` (book, three stills). The dead standing in the dark, thinking for the first time in a century. Caption: "We were bound for a hundred and fifty years. We woke on the same night."
2. `c5_hollow` (card, the camp). The Hollow King, first words: "They call me the Hollow King. They are not wrong. I was hollow a long time."
3. `c5_rest` (card, optional, a bound soldier). The Rest: the dead are owed a rest. **[call]** The soldier does not know why the binding broke.

### Page 2: The First Free Night (battle)
1. `c5_b1_before` (stage, Hollowwood, night). The Bound Host's recalled dead rise to rebind the rebels.
2. `battle_1` The First Free Night (Survival, Easy).
3. `c5_b1_after` (stage). A bound soldier's hand stops mid-grip. "It is pulling at me, from somewhere. Not the king. Something older." Hint.

### Page 3: The Barrow Road
1. `c5_b2_before` (stage, Kingsfield). A Gilded patrol shoots on sight. "We are not what you remember."
2. `battle_2` The Barrow Road (Domination, Normal).
3. `c5_b2_after` (stage). Aldric's patrol captain, titled, spits: "You wore his colours." The Hollow King: "Yes."

### Page 4: The King's Story
1. `c5_king` (card, the war table). The Hollow King tells how the Skeleton King came to bind the dead: his realm was dying, the Concord looked away, he bound the dead to defend the people who were left. "He was not wrong at the start. He was wrong for a century after." **[call]** This is where the player first hears the Skeleton King's motive.
2. `c5_archive_levy` (card item). The Tengervald levy roll, the same one struck from the relief list in chapter 1.

### Page 5: The Razed Dens
1. `c5_b3_before` (stage, Frostmarch). The Hollow King: "I remember this. I do not remember choosing it."
2. `battle_3` The Razed Dens (Domination, Normal; memory).
3. `c5_b3_after` (stage). The Hollow King kneels at a den: "We did this. It was in our hands."

### Page 6: The Sunk Fleet
1. `c5_b4_before` (stage, Rivergate). The Tidecaller ships at anchor.
2. `battle_4` The Sunk Fleet (Domination, Normal; memory).
3. `c5_b4_after` (stage). Water closes over a Tidecaller hull. A bound soldier: "We cannot even say we are sorry in a way that matters."

### Page 7: The Rebinding
1. `c5_b5_before` (stage, Hollowwood). The Bound Host comes to rebind them. A Wildmoot herd crosses the field uninvited, and the hunter beside them nods once.
2. `battle_5` The Rebinding (Domination, Normal; Wildmoot ally).
3. `c5_b5_after` (stage). The Houses have not forgiven anything; they are standing at the same fire. **[call]** The ring stays open.

### Page 8: The King's Host
1. `c5_b6_before` (stage, Kingsfield, night). The Bound Host in its whole length; the Skeleton King himself at the head, tired and certain. He says one thing to the Hollow King: "You could have stayed."
2. `battle_6` The King's Host (Domination, five points, Hard; Wildmoot and Tidecaller allies).
3. `c5_b6_after` (stage). The army breaks. The Skeleton King withdraws; if the player's banner strikes him, the Hollow King says "Not yet." **[call]** He lives either way, for Acts II and III. The Houses stand in the field's silence, and none of them touches another.
4. `c5_spire` (book, two stills, unskippable first time). The sky over Highheart far to the east: a column of violet-white light rises and holds. Every House turns to look. The Hollow King: "The dead asked to rest. Something has heard." The land under the Houses' feet rises and falls once like a breath. End of Act I.

## Where the hints live

| Hint | Where | Reading |
|---|---|---|
| The binding frays | `c5_b1_after`, `c5_free` | The spire's first pulse. |
| The pull on the dead | `c5_b1_after` | The spire, felt before it is named. |
| The Rest | `c5_rest` | Ysmorren. |
| The spire's light | `c5_spire` | The first time the game says the land is alive. |
| The land breathes | `c5_spire` | The giants waking. |

## Voice directions

The Hollow King: dry, tired, honest. A bound soldier: flat, slow. The Skeleton King: gentle, certain, kind, which is the horror. Aldric's patrol captain: bitter.

## What this chapter leaves for others

- Act II: the Skeleton King keeps hunting the Houses; the giants arrive and Orruvan wakes under Highheart.
- The rebels' ownership of what they did is the seed of the Graveborn's Act III turn.

## Open for Kort

1. Confirm the six battles, maps, and the two allied banners.
2. Confirm that memory battles (3 and 4) play as the Graveborn's own wrongs.
3. Confirm the Skeleton King's one-line scene and the spire's last still.
