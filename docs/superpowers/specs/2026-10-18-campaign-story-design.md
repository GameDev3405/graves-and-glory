# Campaign story design: The Long March

Status: draft v1 for Kort, 2026-10-18. Written from Kort's direction the same evening: grim with heroic aspects, dramatic and emotional; players must get attached to the characters and understand them; the lore deep and intricate, everything with a purpose, expandable for later chapters; the campaign is the story side of the game and multiplayer is the memory-making side. Kort agreed: companions fall in battle and sit out the next one wounded, and only the script kills anyone. Kort can record voice lines.

This document fixes the world, the cast, the plot of chapter 1 and the shape of chapters 2 to 5, and the systems that tell the story in the game (cutscenes, dialog cards, companions as units, voice). It argues from the approved specs and never contradicts them: the Houses spec (`2026-10-17-houses-design.md`), the Phase 4 spec (`2026-10-18-phase-4-modes-campaign-design.md`), the built world (`2026-10-17-built-world-design.md`) and the UI spec (`2026-10-18-ui-design.md`). The chapter 1 copy draft (`docs/copy/chapter-1.md`) is rewritten from this document once Kort approves it.

Questions for Kort are collected in section 12. Calls I made alone are marked **[call]**.

---

## 1. Intent and why

The campaign exists to make the player care. Skirmish and the server make memories with friends; the campaign makes the player understand four people and a dead king, and then asks them to fight anyway. Every battle in the chapter already exists in the Phase 4 spec with its map, mode, sides and bonus. This document gives each one a reason that hurts, a person who carries it, and a scene before and after.

Rules the story keeps:

- **Grim, with heroism paid for.** The march is losing. The Road North is a retreat dressed as an advance. Every heroic beat costs someone something the player can see.
- **The systems are the story.** Morale, routs, graves, the shrine where the dead are recruited, the banner you carry: the story points at what the game already does instead of inventing things the fights do not earn.
- **Attachment through presence.** Companions are real units with names over their heads, they speak between battles, and they react to what actually happened. Nobody important is a cutscene-only character.
- **Everything has a purpose.** Every name, place and object below is tied to a game system or a later chapter. Nothing is set dressing.
- **Expandable.** Chapter 1 answers one question (who the Hollow King is) and opens three (where the crown went, what the Tide wants, whether the graves can ever close). Chapters 2 to 5 are outlined so chapter 1's choices do not paint them in.

## 2. Goals, non-goals, anti-goals

Goals:
- A player who finishes chapter 1 can name the four companions, say what each one wants, and say why the Hollow King had to fall.
- A player who loses a companion in battle feels it in camp, and the wound costs them the next fight.
- The lore bible (section 4) can carry five chapters without rewriting chapter 1.
- Scenes are data, so copy and staging change without Java.

Non-goals:
- Branching plot. Chapter 1 is one road. Choices change lines and who stands where, not the ending. **[call]** A branching campaign would double the writing and the harness for a first chapter nobody has played yet.
- Voiced player. The commander is silent.
- Pre-rendered video in the game. Minecraft ships no video codec; the trailer is a separate, parked piece of work.
- Chapters 2 to 5 in detail. Section 9 fixes their questions and cast, not their battles.

Anti-goals (a failure even if it works):
- Companions who are safe and know it. If falling has no cost, nobody cares.
- Lore that lives only in a document. If the player cannot find it in the game, it is not in the game.
- Melodrama. Lines are short, plain, and said by people who are tired. Nobody makes a speech longer than three cards.
- A twist that makes earlier scenes a lie. Rereading chapter 1 after the reveal must make it better, not wrong.

## 3. Constraints

- The Phase 4 battle table binds: six battles, their maps, modes, sides, bonuses and the Houses met in order (Gilded, Graveborn, Ironclad, Tidecallers, Wildmoot, then Kingsfield). The epilogue line "Kingsfield holds. When the field was searched, the Hollow King's crown was gone." binds.
- The Houses spec binds the five Houses, their one-line natures and their five Legendaries (Lord Marshal Aldric, the Hollow King, Old Horn, Brassjaw, Admiral Vael). Legendaries stay recruitable by Writ and Renown; the story explains why a Legendary answers a writ (section 4.5) and never makes one unrecruitable.
- The built world binds the places: Rivergate (mill, crossings, shrine), Ironhold (gate, keep, curtains, barn, garrison), Frostmarch (lodge, refuge, signal tower), Hollowwood (chapel, sawmill, logging camp, moon shrine), Kingsfield (five points, outposts, mill), and Banner Camp (ridge, command tent, war table, War Gates, archive with the Founders' Stone, House ring with the graveyard shrine, training yard).
- The UI spec binds the look: vellum cards, the Iron Ledger, the War Table. Dialog cards are UI-kit vellum with a portrait, not a new style.
- Text: ASCII, no dashes as punctuation, no exclamation marks. Feed lines fit 140 px. Card lines wrap inside the card.
- One client at a time, the harness in `test-tools/`, scenes verified by log line and screenshot (Pillar 5).

## 4. The lore bible

### 4.1 The Barrowlands

The land has no king and has not had one for nineteen years. It is called the Barrowlands because its kings are buried in it: every king since the first lies under a mound at Kingsfield, where every road meets, and the mounds are the realm's only true border. A king is crowned on the mounds and buried under them. The crown is the Barrow Crown, one crown passed living hand to living hand for four hundred years, and the Gilded Order's oath is sworn to it: *to the last living crown*. Those four words are the whole story.

The five Houses are not nations. They are what the people of the Barrowlands became when the crown fell: the Order kept the oath, the mountain clans shut their gates, the woods closed, the coast drowned, and the dead got up.

### 4.2 The Fall at Kingsfield (nineteen years ago)

The Tide came up the rivers in a fog that did not lift for a month. King Osric, the last Barrow King, met Admiral Vael's host at Kingsfield with the Order and the clans, and he was losing. On the third day Lord Marshal Aldric blew the Barrow Horn.

The Horn is older than the crown. It calls the buried kings and their hosts to the standard in the hour the realm falls, and it has one rule, carved on the Founders' Stone that now stands in Banner Camp's archive: *the living crown calls them, and the living crown sends them home*. The dead rose from the mounds and broke the Tide in a night. Then the king died. Nobody agrees how: a Tidecaller trident, a fall from his horse, his own heart. He died with the crown on his head and the Horn's call still sounding, and there was no living crown to send the dead home.

So they stayed. The buried kings went back to their mounds and would not close them. The hosts wandered. Osric rose on the fourth morning with the crown still on him, hollow behind the eyes, and asked his marshal where his army was. The Order broke in half over the answer.

### 4.3 The Hollow King

The Hollow King is King Osric. He is not a villain and he is not mad. He is a dead man who still believes he is king, because he died with the crown on and the oath says the crown is what makes a king. He is coming home to Kingsfield to hold court. Everywhere he walks, the ground remembers the Horn, and the dead within hearing get up and follow him: the old graves of Hollowwood, the smiths of Ironhold, the drowned in the river. He does not raise them on purpose. He is the call, walking.

He keeps his writs. Every commission he signed alive, he honours dead, which is why the Gilded turncoats serve him and why his first words to the player at Kingsfield are about the paper in their hand (section 6, battle 6).

The crown on his head is the real Barrow Crown. When he falls at Kingsfield, it is gone when the field is searched. Chapter 1 does not say who took it. Section 9 does.

### 4.4 The Order's split: oathkeepers and turncoats

The Order swore *to the last living crown*. When Osric rose, half the Order read it one way (the king is dead, the oath waits for the next living hand) and half read it the other (the crown lives on his head, so he is the last living crown, and the oath is to him). The second half are called turncoats by the first, and they call themselves the Crown's Own. They are not traitors in their own eyes. They are the Order's conscience taken literally, which is worse.

Lord Marshal Aldric leads the oathkeepers from the west and does not march with the player. He blew the Horn, he knows what the Stone says, and he is looking for a living hand to put the crown in. He is a Legendary the player can own, and in chapter 1 he is a name in two letters and a rumour.

### 4.5 Writs, banners and why Legendaries answer

A writ is a royal commission: the crown's seal on a paper that names a captain and gives them a standard. A banner (the game's deployment) is the company raised under a writ. The five Legendaries answer writs because every one of them is bound to the crown's seal, by oath (Aldric), by blood (Osric), by old bargain (Old Horn and the woods), by forge-mark (Brassjaw bears the seal on his chest) or by the thing the Tide wants (Vael). When the player spends a Legendary Writ, the story says the seal on it was recognised. **[call]** This is why the chapter reward is a Writ and not gold.

The player's company is the Bannerbound: the survivors of Kingsfield's fall and everyone they picked up since, bound to one standard by a writ Osric signed alive on the second day of the Fall. The writ names no captain any more; the name has been scraped off and rewritten twice. The player is the third name. The standard is the march's banner, and it is what the Tide comes for at Rivergate (battle 4): the Horn's rule is about the living crown *at the standard*, so whoever holds the Bannerbound standard at Kingsfield can finish the rite, or stop it.

### 4.6 The Horn's rule, in full

Carved on the Founders' Stone, read by the Archivist in camp when asked (a Banner Camp scene from Phase 3's archive piece):

> The living crown calls them.
> The living crown sends them home.
> One hand, one crown, one standard, on the field of kings.

Three conditions, one place. Chapter 1 takes the crown away. Chapter 2 is about the standard. Chapter 5 is about the hand.

### 4.7 The Houses in the story

- **Gilded Order:** the oath, and what it costs to keep one literally. Their chapter 1 face is the Harrow family (section 5).
- **Graveborn:** grief that got up and kept walking. They are cheap and many because the Barrowlands have been dying for nineteen years. Every battle the player loses adds to their ranks "somewhere else"; the shrine in camp is where the player's own dead come back, which the story treats as exactly as terrible as it sounds.
- **Ironclad:** memory. "The hold remembers every siege." Ironhold's smiths slept because the clans sealed the hold against the Horn's call and the call came through the stone anyway.
- **Wildmoot:** toll. The woods let nothing through that has not bled for it, because the woods are where the realm's unburied dead were left, and the woods are keeping them.
- **Tidecallers:** the sea wants the land back. Chapter 1 shows raiders; chapter 2 shows why. Vael was at Kingsfield the night the dead rose, and the Tide learned what the Horn can do.

### 4.8 Names

Places are the shipped maps. People are new. The naming rule: one or two syllables for the living, plain English roots, no apostrophes; the dead keep the names they had. Legendaries keep their spec names.

## 5. The cast

Four companions, met along the road in House order, plus one voice from the dead and one enemy the player knows by name. Each companion has a want, a wound, a battle where their story turns, and a hook for a later chapter. Each is a real unit in the player's banner from the battle they join, with a fixed troop type from the Houses spec so the harness can place and count them.

| Name | House | Joins | Unit (Houses spec) | Wants | Wound | Turns in | Later hook |
|---|---|---|---|---|---|---|---|
| Ser Maud Harrow | Gilded Order | Battle 1 | Order Sergeant (Rare infantry) | To keep the oath to someone alive | Her brother Tomas leads the turncoats | Battles 1 and 6 | Tomas lives (ch. 2) |
| Wren | Graveborn | Battle 2 | Bone Archer (Common ranged) | To remember how she died | She was the Bannerbound's scout; she died at the Fall | Battle 2, then the shrine | Remembers the Horn (ch. 5) |
| Dunmar Coalbright | Ironclad | Battle 3 | Hearthguard (Rare infantry) | To open his own gate | He sealed Ironhold and the call came anyway | Battle 3 | Brassjaw's forge-mark (ch. 4) |
| Ashka of the Thornwood | Wildmoot | After battle 5 | Hide Hunter (Common ranged) | To know why the woods' dead are walking | The pass took her brother as toll, years ago, and now he walks | Battles 5 and 6 | Old Horn's bargain (ch. 3) |
| The Archivist | none (camp) | Camp | not a unit | To finish the record | He wrote the Founders' Stone's copy and got one word wrong | Camp scenes | Which word (ch. 5) |
| Ser Tomas Harrow | Gilded, turncoat | Enemy, battles 1 and 6 | Order Sergeant, enemy banner | To serve the last living crown | He believes it | Battle 6 | Taken alive (ch. 2) |

Unit mapping is **[call]**; the Houses spec's Rare names are used where the spec has them and the Phase 2 Task 2 roster decides the exact ids at plan time.

### 5.1 Ser Maud Harrow

Forties, Order sergeant, a scar through one eyebrow from the Fall. She kept the oath the plain way: the king is dead, the crown waits, she serves the standard until there is a hand to give it to. She is the march's spine and she knows the player is the third name on the writ, because she scraped the second one off herself. She is dry, not warm; she shows care by counting the player's troops after every battle out loud.

Her brother Tomas took the other reading. They have not spoken in nineteen years and she will not say his name until battle 6 makes her.

- **Battle 1, The Training Field.** The turncoats on the river road are Tomas's company, though he is not there. Maud teaches the player the game's first lessons (the existing tutorial boards stay) and her first card after the win is: *"Those were Order. Remember that when you count them."*
- **Battle 6, Kingsfield.** Tomas's banner is among the enemy. The script puts him on the player's side of the field. If Maud is standing (not wounded), the scene after the battle is hers: she finds him fallen, and does not kill him. If she is wounded, the player finds him and she says it from the stretcher. Either way he is taken alive.

### 5.2 Wren

A Bone Archer who was the Bannerbound's scout before she was the Bannerbound's dead. She died at the Fall and answered the Horn with everyone else, and she has been walking since, following the only thing she still recognises: the standard. At the Moon Shrine (battle 2) she is the last of the dead to lie still, and she lies still on purpose, in front of the player, and then gets up again and follows them out. She does not remember dying. She remembers the player's banner. She is the voice at the graveyard shrine in camp (where Graveborn are recruited, by Phase 3's rule), and every recruit the player takes there, she introduces by name if she knew them.

Her humour is the dry kind the dead are allowed. She is the chapter's heart because she is the only one who cannot be wounded in the usual sense: when she falls in battle, the script treats it as the Graveborn trait (she rises once), and if she falls twice she sits out the next battle "resting", which she finds funny.

- **Battle 2, The Moon Shrine.** The reason line "there is nowhere else to sleep" is hers; the scene before the battle is Maud choosing the shrine because the dead do not come up through consecrated ground, and the scene after is Wren lying still, then standing. Her first card: *"You kept the banner. Good. I could not find anything else."*
- **The shrine, every visit.** One line per visit, in order, and she remembers one more thing each time (section 7.4). By the end of chapter 1 she remembers that the Horn was blown by someone she saw.

### 5.3 Dunmar Coalbright

Ironclad hearthguard, gate-warden of Ironhold. When the Horn's call reached the mountains the clans sealed the holds: every door shut, the forges banked, the smiths to bed under stone. He sealed the great gate himself. The call came through the stone anyway, and the smiths got up dead. He was outside the gate when it happened, checking the seals, which is the only reason he is alive, and he has not forgiven himself for being thorough.

He is slow to speak, exact when he does, and he counts in engines. He built the gate the player must break in battle 3, which is why the bonus is "inside six minutes": he knows exactly how long it takes to break his own work with his own ballistas.

- **Battle 3, Ironhold Retaken.** The scene before is the Ironclad bringing the engines and Dunmar refusing to look at the gate; the scene after is in the keep, where he finds the smiths' graves still open and asks the player, the only time he asks anything, to leave them that way until there is a crown to send them home. The game's rule that Siege reserves `ironhold/gate_intact` is the story's rule too: after this battle the gate stands broken on the map, and the camp's Ironclad tent remembers it.

### 5.4 Ashka of the Thornwood

Wildmoot hunter from the Thornwood north of Frostmarch. The pass has always taken a toll in blood, because the woods are where the Barrowlands left its unburied, and the woods keep them quiet. Her brother was the toll, eleven years ago, when their clan crossed south. Now the woods' dead are walking, which means the woods have stopped keeping them, and she wants to know why. She joins after the pass because the Wildmoot's law is that whoever pays the toll in full may take one hunter north with them, and the player's march paid it.

She is the only companion who likes the player straight away, because they bled honestly. She is also the only one who has seen the Hollow King's host from the trees, and her report before Kingsfield is how the player learns how large it is.

- **Battle 5, The Frozen Pass.** The Wildmoot fight the player at the pass because the law says so; the scene after is the toll paid and Ashka stepping out of the trees. The bonus "inside ten minutes" is the snow: her line is *"The snow does not take sides. It takes the slow."*
- **Battle 6, Kingsfield.** Her Wildmoot kin stand with the player, as the Phase 4 sides say.

### 5.5 The Archivist

Banner Camp's keeper of the record, in the archive by the Founders' Stone (a Phase 3 piece). Old, precise, and wrong once: when he copied the Stone's rule into the camp's book, nineteen years ago, by lantern light, he wrote one word wrong, and the march has been reading his copy ever since. Which word is the chapter 5 question. In chapter 1 he reads the rule aloud when asked, tells the player what a writ is, and keeps the roll of the dead that Wren adds to.

### 5.6 Ser Tomas Harrow

Maud's younger brother, sergeant of the Crown's Own. He is not on the field in battle 1 (his company is; he is north with the king). He is at Kingsfield. He believes, completely, that the Hollow King is the last living crown and that his sister broke her oath. He is right about the words and wrong about everything else, and the story never calls him a coward. He falls at Kingsfield and lives. Chapter 2 has him in the march's custody, which is a problem for Maud and a gift for the player, because he knows where the crown went.

## 6. Chapter 1, "The Road North": the plot

The Phase 4 table is the spine; this section adds the why, the scenes and the companion beats. Scene ids are the ones the scene files use (section 7.2).

**Opening, the Writ (storybook, `c1_open`).** Four panels. The Fall in one image (fog, the mounds, the Horn). The king rising. The scraped writ with a third name being written on it: the player's. The standard going up over Banner Camp. The Writ card from the chapter 1 copy is the last panel's text. Maud's voice over the last panel: *"The road north opens at the river. Teach them to follow you first."*

**Battle 1, The Training Field (Rivergate, Domination, Easy).** The road west must open before the march begins. Turncoat Order hold the crossings. Maud teaches, the four lesson boards teach, and the player learns the enemy is their own House from the other side. After: Maud's count, and her line about remembering they were Order. Hook: a turncoat's dropped writ-copy with the king's seal, which Maud takes and will not explain.

**Battle 2, The Moon Shrine (Hollowwood, Survival, Easy).** The march camps at the moon shrine because consecrated ground is the only ground the dead do not come up through, and the old graves of Hollowwood are opening because the Hollow King's host passed this way a week ago. Hold until the last of the dead lies still. The last of them is Wren. After: she stands, she follows, she joins. Camp: the graveyard shrine opens for recruiting; Wren is its voice.

**Battle 3, Ironhold Retaken (Ironhold, Siege attack, Normal).** The wagons cannot pass while the dead hold Ironhold. The Ironclad bring the engines; Dunmar brings the guilt. Take the yard, break the gate, seize the keep. After: the keep, the open graves, Dunmar's one request, and he joins. Camp: the Ironclad tent, the broken gate remembered.

**Between 3 and 4, the letter (camp card, `c1_letter_aldric`).** A rider from the west with a letter from Lord Marshal Aldric to "whoever holds the Bannerbound standard": the king's host is making for Kingsfield; the Tide are on the rivers again; hold the standard, do not let it reach the field in the wrong hand. Maud reads it and says the Marshal blew the Horn. It is the first time the player hears what the Horn is. The Archivist can be asked for the rule after this card.

**Battle 4, Raiders on the River (Rivergate, Banner, Normal).** Without its banner the march is just a crowd. The Tide come up the river for the standard, because the standard is one of the three things the Horn's rule needs on the field of kings. Guard yours, carry theirs home. After: Vael's raiders go home empty, and the player keeps a Tidecaller banner that is not a banner at all but a drowned man's shroud with the seal on it. Chapter 2 hook. Maud: *"They did not come for gold."*

**Battle 5, The Frozen Pass (Frostmarch, Domination, Normal).** The pass is the only way north before the snow, and the Wildmoot take their toll from any host that crosses, because the woods keep the unburied quiet and the toll is what the woods are owed. Take the pass before the snow closes it. After: the toll is paid in the dead the player lost (the game's real losses are counted in the scene by the script), Ashka steps out of the trees, and she joins. Her report: the king's host is at Kingsfield already, and it is larger than the march.

**Before Kingsfield, the council (camp scene, `c1_council`).** All four at the war table. Each says one thing. Maud: Tomas is there. Wren: she remembers a horn. Dunmar: the engines can reach the centre bridge from the raised outposts. Ashka: the king stands at the mounds and does not move. The player gives no speech. The scene ends on the standard.

**Battle 6, Kingsfield (Kingsfield, Domination five points, Hard).** Every road meets at Kingsfield and so does the Hollow King. The Houses you met stand with you. Break his host.

- The king's scene before the battle (`c1_king_before`): Osric at the mounds, crowned, hollow. He asks for the writ. He reads his own seal. He says the player's name, because it is on the paper, and tells them the commission stands: carry the standard to the field of kings. *"You have. Well done. Now give it to me."* The player does not. That is the whole scene.
- In the battle, the bonus "strike down the Hollow King with your own banner" is the story's demand too: the writ holder's banner, by the player's hand.
- Tomas's banner is on the field. The script places him near the centre bridge and keeps him from dying (section 7.5).
- After (`c1_king_after`): the king down, the crown gone when the field is searched. Maud and Tomas (section 5.1). Wren looks at the king's face and says she saw him die the first time. Dunmar's engines are silent. Ashka is already looking north.

**Epilogue (storybook, `c1_close`).** The spec's line, over the field at dawn: *"Kingsfield holds. When the field was searched, the Hollow King's crown was gone."* Then the Writ screen: choose your Legendary. The story's reading: the seal on the paper was recognised by someone.

### 6.1 The climax, and why it lands

The player spends five battles learning the oath, the Horn, the writ and the standard through four people who each carry one of them. At Kingsfield the dead king holds up the paper that makes the player who they are, and asks for the thing they have carried the whole road. Saying no costs the king. Saying yes would end the march. There is no third choice, and the game does not pretend there is: the battle starts when the card closes.

Then the crown is gone, and the rite cannot be finished by anyone, and the dead stay up. Chapter 1 ends worse than it began, with four people who would follow the player anywhere, which is the point.

## 7. How the story is told in the game

### 7.1 Three kinds of scene

1. **Storybook** (`panel` scenes): layered pixel-art panels drawn with the UI kit's exporter, 2 to 4 layers each (sky, far, near, figures), slow pan or parallax per layer, text typed underneath on a vellum strip, optional voice. Used for the chapter opening and close. Four to six panels each. Drawn by the same pipeline as the UI mockups (`tools/ui_art`), exported into the atlas or as standalone PNGs.
2. **In-engine** (`stage` scenes): the game world is the set. The scene script moves the camera along keyframes (the title demos' camera hook, `TitleDemoCameraMixin`, generalised and moved out of the test tools), places actors (real units, posed and frozen or walking a path), sets time, weather, particles and sound, and shows dialog cards. Used before and after battles. Played on the battle's own map, so a scene can show the gate it is about to break.
3. **Camp cards** (`card` scenes): dialog cards at the war table, the shrine, a tent or the archive, with no camera move. The player walks up and presses the use key; the card plays. Used between battles and for every optional line.

All three are skippable with one key at any time, which also skips the typing. A skipped scene is marked seen. Seen scenes can be replayed from the Chapter screen (UI plan Task 7's Chapter page gains a scenes list; **[call]**).

### 7.2 Scene files

Scenes are text, one file per scene, under `src/main/resources/data/gravesandglory/scenes/<chapter>/<id>.scene`, loaded as a resource (not a sheet: sheets are rectangular and a scene is a sequence). Line 1 is `# scene <id> <kind>` with kind `panel`, `stage` or `card`. Every other line is a comment or one command. The format follows the structure scripts' rule: one line, one thing, no nesting.

```
# scene c1_king_before stage
map 4
time 23000
weather clear
fog 0.6
actor king gb_hollowking at 350 71 250 facing 180 pose stand
actor maud companion:maud at 346 70 262 facing 0
camera 340 74 270 to 352 72 258 over 160
wait 20
card king "You carry my seal." voice c1_k01
card king "I signed that on the second day. I remember the hand." voice c1_k02
card maud "Do not answer him." voice c1_m31
camera hold 352 72 258 look king
card king "You have carried the standard to the field of kings. Well done. Now give it to me." voice c1_k03
fade out 20
end
```

Commands (the builder adds nothing not listed; new commands come back through this document):
- `map <n>` picks the battle map for a stage scene (the scene runs on it before the battle builds its lobby). `camp` picks the camp.
- `time`, `weather`, `fog` set the sky.
- `actor <id> <unit or companion:<name>> at x y z facing <yaw> [pose <name>]`: a real entity, frozen unless `walk`.
- `walk <actor> to x y z over <ticks>`; `pose <actor> <name>` (poses are the title demos' arm poses plus `kneel`, `fallen`, `stand`; **[call]** five poses, no custom animation in chapter 1).
- `camera x y z to x y z over <ticks>`, `camera hold x y z look <actor>`, `camera player` (back to the player).
- `card <speaker> "<text>" [voice <id>]`: a vellum dialog card with the speaker's portrait; the player advances with the use key or after the typing finishes plus 60 ticks. Text wraps inside the card; a card holds at most three lines.
- `panel <png> [pan dx dy over <ticks>] [layers n]`: storybook only.
- `sound <id>`, `particles <type> at x y z count n`, `wait <ticks>`, `fade in|out <ticks>`, `end`.
- `if companion:<name> standing|wounded` ... `else` ... `endif`: the only branch, so scenes can change who speaks a line.

### 7.3 Dialog cards

The UI kit's vellum card with a 32x32 pixel portrait at the left, the speaker's name in the title font, the line in the body font, typed at 2 characters per tick with a soft quill sound, and a small "continue" glyph. Portraits are pixel art, one per speaker (six in chapter 1), drawn by the designer pipeline and graded against the UI rubric like any other art. Cards never cover the Iron Ledger's seal row. In battle, the only cards are the king's two scenes; everything else is before or after.

### 7.4 Companions as units

- A companion is a named troop in the player's banner with a fixed slot the player cannot sell or replace; the slot shows the portrait on the War Table banner page.
- In battle the companion is a normal unit of its type with the name over its head in the side colour, counted by the group it is in. Orders work on it like any troop.
- **Falling.** At 0 HP a companion does not die. It goes to a `fallen` pose where it stood, is untargetable, and a feed line says who fell. After the battle the companion is `wounded` for the next battle: its slot is empty, its portrait greyed, and its camp lines are the wounded set (one or two lines, from the stretcher). Wren rises once per battle by the Graveborn trait and is "resting", never wounded, which her lines joke about.
- **Only the script kills.** No companion dies in chapter 1. Tomas is script-protected at Kingsfield (section 7.5).
- **Reaction lines.** After each battle the camp picks lines from what happened: a companion who fell, a rout of the player's group, a bonus earned or missed, losses above a threshold. One line per fact, at most three per visit, in a fixed priority. These are the attachment engine; they are written per companion per battle (section 8 sizes it).
- **Shrine lines.** Wren's shrine line advances once per visit after each battle, six in chapter 1, and does not depend on anything else, so a player who never loses still hears her remember.

### 7.5 Script protection

A unit with the `protected` tag (companions, Tomas at Kingsfield, the king until the battle starts) takes damage normally but cannot go below 1 HP from any source; at 1 HP it falls (companion rule) or, for Tomas, kneels and is ignored by bots. The king is not protected in the battle itself: the bonus requires his death by the player's banner, and his `lastHitBy` is already tracked for it.

### 7.6 Voice

Every `card` line has a `voice` id. A voice line is an `.ogg` under `assets/gravesandglory/sounds/voice/<chapter>/<id>.ogg`, listed in `sounds.json`. If the file exists it plays and the typing matches its length; if not, the card types silently. Subtitles are always the card text, so nothing depends on audio. Kort records after the script is locked (section 11); until then every card is silent and complete.

Recording guide, so the lines are written for a voice: lines are under 20 words, one breath, written to be said tired. Six speakers in chapter 1; Kort can voice all of them with a note per character (section 8 carries the notes).

### 7.7 Where the lore lives in the game

- The Founders' Stone in the archive: the rule, readable.
- The Archivist: the roll of the dead, what a writ is, what the Horn is (after the letter).
- The War Table's Chapter page: the chapter's scenes, replayable, and the companions' portraits with their one-line wants.
- The shrine: Wren.
- The tents: each companion's camp lines.
- Item text: the scraped writ (the chapter's Writ card), the turncoat's writ-copy, the drowned shroud. Three objects, each a hook.

## 8. Writing the copy

Volume for chapter 1, sized so Kort can read it in one sitting and record it in two:

| Piece | Count | Words each |
|---|---|---|
| Storybook panels (open 4, close 2) | 6 | under 30 |
| Stage scenes (before and after six battles, plus the king's two, which are among them) | 12 | 3 to 6 cards |
| Camp cards (letter, council, four joins) | 6 | 2 to 4 cards |
| Reaction lines (4 companions x 6 battles x up to 4 facts) | about 80 | under 20 |
| Wounded lines (4 companions x 2) | 8 | under 20 |
| Shrine lines (Wren) | 6 | under 25 |
| Archivist | 5 | under 40 |
| Object text | 3 | under 40 |

About 180 lines, roughly 2,500 words. Every line carries a speaker, a scene id, a voice id and a one-line direction for the voice.

Voice notes: Maud, level and low, never raises it; Wren, light, a little late on every line, as if listening; Dunmar, slow, every word placed; Ashka, quick and warm; the Archivist, precise, old; the Hollow King, kind, which is the horror of it; Tomas, certain.

## 9. Chapters 2 to 5: the shape

Each chapter meets one House on its own ground, answers one question, and opens the next. The cast grows by one per chapter. Nothing below binds battles or maps; new maps are their own design.

| Chapter | Name | House ground | Question answered | Who joins | Who it hurts |
|---|---|---|---|---|---|
| 2 | The Drowned Coast | Tidecallers | Where the crown went: Vael took it, to drown it, because a crown under the sea can never send the dead home and the Tide wants the dead to stay up (the dead drown; the drowned are Tide) | Tomas, in custody, then not | Maud |
| 3 | The Thornwood | Wildmoot | Why the woods stopped keeping the dead: Old Horn's bargain was with a living crown, and it lapsed | Ashka's brother, walking | Ashka |
| 4 | The Deep Hold | Ironclad | What the seal on Brassjaw means: the clans forged the crown, and can forge a hand for it | Brassjaw, by the player's writ | Dunmar |
| 5 | The Barrow Horn | Kingsfield again | The Archivist's wrong word, and whose hand: the rule says *one hand*, the copy says *a living hand*; the Stone never said living | Nobody. Wren remembers the hand on the Horn was hers | Everyone |

Chapter 5's ending is the only one fixed now, because chapter 1's scenes are written toward it: the Horn can be blown again by the hand that blew it, and Wren is dead, and the rule never said living. The player's choice at the end of the saga is whether to let her. **[call]** Everything in chapter 1 that Wren remembers is written so this is true without lying.

Aldric is in every chapter as a letter and in chapter 5 in person, on the mounds, with nothing to say for himself.

## 10. Harness and gates

- A `scenes` suite in the harness plays every chapter 1 scene headless with the typing skipped, checks each `card` draws (text, speaker, rect), each `camera` reaches its end point, each `actor` is placed and posed, and takes one screenshot per scene at its last card.
- A `companions` suite: a companion falls and is untargetable; the next battle's slot is empty and greyed; the wounded line plays; Wren rises once and is "resting"; Tomas at 1 HP kneels; a reaction line plays for a fall, a rout and a bonus, in priority order.
- A `story-voice` check: every `voice` id in the scene files is either in `sounds.json` or in a listed "silent" set, and no id is in both.
- Kort's gate: he plays chapter 1 start to finish in one sitting with the scenes on, and says, for each companion, what they wanted. If he cannot for any one of them, that companion's lines are rewritten before the chapter ships.

## 11. Order of work

1. Kort approves this document (section 12 answered). Then the chapter 1 copy is rewritten from it: every line with its scene id, speaker, voice id and direction, in `docs/copy/chapter-1.md`.
2. The Phase 4 plan (modes and chapter 1) is written with the story in it: scene loader, dialog cards, companions, protection, the three suites. Scenes are data from day one.
3. Portraits and storybook panels go through the designer pipeline and the UI rubric, after the unit identity pass so companions look like their Houses.
4. Kort records voice after the copy is locked and the scenes play silently in the game, so the timing is known.
5. Chapter 2's design starts after Kort's chapter 1 gate.

## 12. Questions for Kort

1. **The Hollow King is King Osric, the last king, risen with the crown on.** He is kind, not evil, and he asks the player to hand him the standard. Yes or no.
2. **Wren blew the Horn.** Not Aldric: she was the hand on it, on his order, and she died for it, and chapter 5 turns on her remembering. Chapter 1 only hints. This is the saga's biggest secret; say no now if you want a different one, because every Wren line is written toward it.
3. **Companions join one by one** (Maud at 1, Wren at 2, Dunmar at 3, Ashka after 5) instead of all four from battle 1. The road builds the company. Yes or no.
4. **Tomas lives** and is in the march's custody for chapter 2. Yes or no.
5. **The player is "the Bannerbound", the third name on a scraped writ.** The company's name is the game's old name. Yes or no.
6. **Voice:** you voice all six chapter 1 speakers, with the notes in section 8, or you voice some and the rest stay subtitled. Which.
7. **Scene replay on the Chapter page** and skip with one key: fine, or should scenes be unskippable the first time.

---

## Calls made in this draft

- No branching plot in chapter 1; one `if standing|wounded` branch in scene files only.
- Five actor poses, no custom animation; the title demos' camera and pose hooks are generalised rather than a new system.
- Scenes as resource text files, not sheets.
- Companions' unit types from the Houses spec's Rare and Common rows; exact ids at plan time.
- Protection at 1 HP for companions and Tomas; the king unprotected in the battle.
- Chapter 5's ending fixed now so chapter 1 can be written toward it.
- The realm's name, the Barrowlands; the king's name, Osric; the company's name, the Bannerbound.
