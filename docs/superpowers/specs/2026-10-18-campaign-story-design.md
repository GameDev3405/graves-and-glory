# Campaign story design: The Long March

Status: v2 approved by Kort, 2026-10-18 late (section 13 answered: Ashka is the heir and the love interest; Maud leaves with Tomas; the saga ends on the player's choice in chapter 5, complete in itself but open to a sixth chapter; the choice chits stay; the Stone's words stay readable). v1 was written the same evening and Kort answered it: the Hollow King is not King Osric; Wren blew the Horn; companions join one by one; Tomas lives; the company is the Hollow; scenes cannot be skipped the first time; voice is decided later (write dialog and captions now). Kort also asked for love interests, sorrow and despair, a plot twist, a cliffhanger at the end of every chapter, and depth: everything planned, everything with a purpose, expandable for later chapters. He approved the storybook frame: the book lies on the war table, the wind opens it to the player's page, each page holds two to four scenes, each chapter holds some number of pages, the player plays the next scene or flips back to replay old ones, and a scene hands straight to its match. He asked that the storybook be given real art and detail; the web demo was a flow mockup only.

Kort's direction, in his words: grim with heroic aspects, dramatic and emotional; players must get attached to the characters and really understand them; the campaign is the story side of the game, and commanding an army with friends on a server is the memory-making side.

This document fixes the world, the cast, the plot of chapter 1 page by page, the shape of chapters 2 to 5 with their twists and cliffhangers, and the systems that tell the story in the game. It argues from the approved specs and never contradicts them: the Houses spec (`2026-10-17-houses-design.md`), the Phase 4 spec (`2026-10-18-phase-4-modes-campaign-design.md`), the built world (`2026-10-17-built-world-design.md`) and the UI spec (`2026-10-18-ui-design.md`). The chapter 1 copy (`docs/copy/chapter-1.md`) is rewritten from this document once Kort approves it.

Calls I made alone are marked **[call]**. Questions for Kort are in section 13.

---

## 1. Intent and why

The campaign exists to make the player care. Skirmish and the server make memories with friends; the campaign makes the player understand four people and a dead captain, and then asks them to fight anyway. Every battle in chapter 1 already exists in the Phase 4 spec with its map, mode, sides and bonus. This document gives each one a reason that hurts, a person who carries it, and a scene before and after.

Rules the story keeps:

- **Grim, with heroism paid for.** The march is losing. The Road North is a retreat dressed as an advance. Every heroic beat costs someone something the player can see.
- **The systems are the story.** Morale, routs, graves, the shrine where the dead are recruited, the standard the player carries: the story points at what the game already does instead of inventing things the fights do not earn.
- **Attachment through presence.** Companions are real units with names over their heads, they speak between battles, and they react to what actually happened. Nobody important is a cutscene-only character.
- **Everything has a purpose.** Every name, place and object below is tied to a game system, a scene, or a later chapter. Nothing is set dressing.
- **Expandable, and planned.** Chapter 1 answers one question (who the Hollow King is) and opens four (where the crown went, what the Tide wants, who the heir is, whether the graves can close). Chapters 2 to 5 are plotted so chapter 1's scenes are written toward them, and rereading chapter 1 after any reveal makes it better, not wrong.
- **A cliffhanger ends every chapter**, on the last page, as a scene the player cannot skip the first time.

## 2. Goals, non-goals, anti-goals

Goals:
- A player who finishes chapter 1 can name the four companions, say what each one wants, say who the Hollow King was and why he had to fall, and wants to open chapter 2 tonight.
- A player who loses a companion in battle feels it in camp, and the wound costs them the next fight.
- The lore bible carries five chapters without rewriting chapter 1.
- Scenes are data, so copy and staging change without Java.

Non-goals:
- Branching plot. Chapter 1 is one road. Choices change lines and who stands where, not the ending. **[call]** A branching campaign would double the writing and the harness for a first chapter nobody has played yet. Chapter 5 has the saga's one real choice.
- Voiced player. The commander is silent.
- Pre-rendered video in the game.
- Chapters 2 to 5 in battle-by-battle detail. Section 9 fixes their plots, casts, twists and cliffhangers, not their maps and modes.

Anti-goals (a failure even if it works):
- Companions who are safe and know it.
- Lore that lives only in a document. If the player cannot find it in the game, it is not in the game.
- Melodrama. Lines are short and said by tired people. Nobody makes a speech longer than three cards.
- A twist that makes earlier scenes a lie.
- A storybook that looks like a menu. It is a painted object on a table.

## 3. Constraints

- Kort's 2026-10-18 direction amends one line of the approved Phase 4 spec: its anti-goal "no boss mechanics, cutscenes or voice" is lifted for cutscenes and voice. Boss mechanics stay out: the Hollow King has no rules of his own in the battle. The Phase 4 plan is written from both documents.
- The Phase 4 battle table binds: six battles, their maps, modes, sides, bonuses and the Houses met in order (Gilded, Graveborn, Ironclad, Tidecallers, Wildmoot, then Kingsfield). The epilogue line "Kingsfield holds. When the field was searched, the Hollow King's crown was gone." binds and is the last line before the cliffhanger page.
- The Houses spec binds the five Houses and their five Legendaries. Legendaries stay recruitable by Pact and Renown; section 4.6 says why a Legendary answers a pact.
- The built world binds the places: Rivergate, Ironhold, Frostmarch, Hollowwood, Kingsfield (five points: two fords, the centre bridge, two raised outposts), and Banner Camp (command tent, war table, War Gates, archive with the Founders' Stone, House ring with the graveyard shrine, training yard).
- The story needs one landmark the built world does not list yet: the kings' mounds at Kingsfield, a ring of barrows beside the centre bridge with the first king's mound highest. It is added to Phase 3's Kingsfield landmark task **[call]**; `c1_king_before` and `c1_crown` wait for it.
- The UI spec binds the look: vellum, brass, iron, the Iron Ledger, the War Table. The storybook and its cards are drawn from the same kit, at the same detail Kort demanded for the mockups (two to three times the pixel detail of the first round), and graded against `work/ui-concepts/GRADING-RUBRIC.md` before Kort sees them.
- Text: ASCII, no dashes as punctuation, no exclamation marks. Feed lines fit 140 px. Card lines wrap inside the card, at most three lines.
- One client at a time, the harness in `test-tools/`, scenes verified by log line and screenshot (Pillar 5).

## 4. The lore bible

### 4.1 The Barrowlands

The land has no king and has not had one for nineteen years. It is called the Barrowlands because its kings are buried in it: every king since the first lies under a mound at Kingsfield, where every road meets, and the mounds are the realm's only true border. A king is crowned on the mounds and buried under them. The crown is the Barrow Crown, one crown passed living hand to living hand for four hundred years, and the Gilded Order's oath is sworn to it: *to the last living crown*. Those four words are the whole story.

The five Houses are not nations. They are what the people of the Barrowlands became when the crown fell: the Order kept the oath, the mountain clans shut their gates, the woods closed, the coast drowned, and the dead got up.

### 4.2 The Horn's rule

Carved on the Founders' Stone, which now stands in Banner Camp's archive, in letters the Archivist copied by lantern light nineteen years ago:

> The crown calls them.
> The crown sends them home.
> One hand, one crown, one standard, on the field of kings.

The Archivist's copy, the one every living person has read since, says *a living hand*. The Stone says *one hand*. Nobody has compared them. This is the saga's keystone, paid off in chapter 5 (section 9).

The Barrow Horn is older than the crown. It calls the buried kings and their hosts to the standard in the hour the realm falls. The one who blows it is the first to die and the first to rise: the Horn takes its breath back. Nobody living knows this part, because nobody who blew it is living.

### 4.3 The Fall at Kingsfield, nineteen years ago (day by day)

- **Day 1.** The Tide comes up the rivers in a fog that does not lift. Admiral Vael's host lands at Kingsfield. King Osric meets it with the Order under Lord Marshal Aldric, the clans, and the woods' hunters. Osric is losing.
- **Day 2.** Osric sends his only child, Elin, nine years old, north toward Frostmarch with a guard of twelve. He signs a pact naming Ser Halden Rook captain of the standard's guard, "to carry the standard to the field of kings and hold it until a living hand takes the crown". This is the Hollow's pact. Halden Rook is its first name.
- **Day 3.** Aldric orders the Barrow Horn blown from the first king's mound, the highest. The guard cannot reach it through the Tide. The company's scout can. Her name is Wren. She blows it. The dead of the mounds stand up and break the Tide in a night. Wren is found at the foot of the mound in the morning with the Horn in her hands and no breath in her. Halden Rook dies on the mound-top holding the standard over her.
- **Day 4.** The Tide is broken. Osric is dead on the field, crown on, from a Tidecaller trident. The dead kings walk back to their mounds and do not close them. Their hosts do not go home. The Horn is still sounding, faintly, under everything. Edric Vane, Rook's lieutenant, takes the crown off the dead king's head so that there is a crown to put on a living hand, writes his own name under Rook's on the pact, and marches the standard's guard north after the heir. They call themselves the Hollow from that morning. Wren rises on the fifth day and follows the standard, because it is the only thing she still recognises.
- **The nineteen years.** Elin's guard is never found. The Hollow search north, then west, then north again, and the realm comes apart behind them. The dead do not lie down, but they wander without purpose, and the living learn to bury under consecrated ground. The Order breaks over the oath (section 4.5). Vael takes the coast. The clans seal the holds. The woods close.
- **Two years ago.** Edric brings the Hollow back to Kingsfield with the crown and the standard. He has read the Archivist's copy a thousand times: a living hand, one crown, one standard, the field of kings. He has all three, he thinks, and no heir. He puts the crown on himself at the mounds, to send the dead home. The crown takes him, and it cracks on his head, a split from the brow to the back that never closes. What stands up is hollow behind the eyes and kind in the voice, and every dead thing within hearing turns toward it. The Hollow scatter. Maud Harrow, his sergeant and his lover, carries the standard out of Kingsfield with eleven people, believes him dead under the mounds, and scrapes his name off the pact with her own knife on the road.
- **This spring.** Maud writes the third name on the pact: the player's. Banner Camp is where the Hollow have gathered what is left. The Road North is the march back to the field of kings, to hold the standard there until Aldric brings a hand.

### 4.4 The Hollow King

The Hollow King is Edric Vane, crowned at the mounds two years ago by his own hand. He is not a villain and he is not mad. He is a dead man who believes he is king, because the oath says the crown makes the king and the crown is on his head. He is coming home to Kingsfield to hold court, and he is bringing every dead thing that ever answered the Horn, because they turn toward the crown like a field toward the sun. Everywhere he walks, the ground remembers the Horn and the dead get up: the old graves of Hollowwood, the smiths of Ironhold, the drowned in the river. He does not raise them on purpose. He is the call, walking.

He keeps pacts. Every commission the crown ever sealed, he honours, which is why the Gilded turncoats serve him and why his first words to the player at Kingsfield are about the paper in their hand. He does not remember Maud. That is the horror and the sorrow of him: kindness with nothing behind it.

The Houses spec calls him the Hollow King and makes him the Graveborn Legendary. The story keeps both. When a player spends a Pact on him in Phase 2's recruit page, the story's reading is that the seal on the pact was his own.

The crown on his head is the real Barrow Crown, broken (the Houses spec's broken crown on the model is this crack). A broken crown can still call the dead; it cannot send them home. That is chapter 4. When he falls at Kingsfield, it is gone when the field is searched. The last page of chapter 1 says who has it.

### 4.5 The Order's split: oathkeepers and the Crown's Own

The Order swore *to the last living crown*. When the crown walked out of Kingsfield on a dead man's head, half the Order read it one way (the king is dead, the oath waits for the next living hand) and half read it the other (the crown is on a head that speaks and gives orders, so it is the last living crown, and the oath is to it). The second half are called turncoats by the first, and they call themselves the Crown's Own. They are not traitors in their own eyes. They are the Order's conscience taken literally, which is worse.

Lord Marshal Aldric leads the oathkeepers from the west and does not march with the player. He ordered the Horn blown, he has read the Stone with his own eyes, and he is looking for a living hand. In chapter 1 he is a name in one letter. In chapter 5 he is on the mounds with nothing to say for himself.

### 4.6 Pacts, the standard, and why Legendaries answer

A pact is a royal commission: the crown's seal on a paper that names a captain and gives them a standard. A banner (the game's deployment) is the company raised under a pact. The Hollow's pact has three names on it, two scraped: Halden Rook, Edric Vane, and the player.

The standard is the Hollow's banner pole and cloth, the thing the player carries into every battle, and the third of the Horn's three things. Whoever holds it on the field of kings with a crown and a hand can finish the rite, or stop it. That is why the Tide comes for it at Rivergate (battle 4), why Aldric's letter says hold it, and why the Hollow King asks for it.

The five Legendaries answer pacts because every one of them is bound to the crown's seal: by oath (Aldric), by the crown itself (the Hollow King), by old bargain (Old Horn and the woods), by forge-mark (Brassjaw bears the seal on his chest; the clans forged the crown), or by hunger for it (Vael). When the player spends a Legendary Pact, the story says the seal was recognised. **[call]** This is why the chapter reward is a Pact and not gold.

### 4.7 The Houses in the story

- **Gilded Order:** the oath, and what it costs to keep one literally. Their faces are the Harrows (section 5).
- **Graveborn:** grief that got up and kept walking. They are cheap and many because the Barrowlands have been dying for nineteen years, and every battle they lose adds to their ranks somewhere else. The shrine in camp is where the player's own dead come back, which the story treats as exactly as terrible as it sounds.
- **Ironclad:** memory. "The hold remembers every siege." Ironhold's smiths rose at the Fall, when the call came through the sealed stone; the clans held the upper hold against their own dead for seventeen years. Two years ago the Hollow King's passing woke the rest, and the dead took the whole hold and its gate while the last living smiths slept. That is the "took Ironhold" of the Phase 4 briefing. The clans forged the Barrow Crown four hundred years ago; the forge-mark on Brassjaw is the crown's seal (chapter 4).
- **Wildmoot:** toll. The woods let nothing through that has not bled for it, because the woods are where the realm's unburied dead were left, and the woods are keeping them quiet under Old Horn's bargain with the crown. The bargain lapsed with the crown (chapter 3).
- **Tidecallers:** the sea wants the land back. Chapter 1 shows raiders; chapter 2 shows why: the drowned are the Tide, and a crown under the sea keeps every dead thing up forever.

### 4.8 Names

Places are the shipped maps. The player is **Danta Hollow** (Kort, 2026-10-18 late), a named hero; the profile name still labels saves and lobbies, the story calls him Danta. The company is **the Hollow** ("the Hollow" never "the Hollow", which is retired): his troops, named for his line. The enemy keeps the title the Hollow King, and the echo is the saga's hidden meaning (Kort, 2026-10-18 late: approved): see section 4.9. Chapter 1's copy notices it once and explains nothing. People: one or two syllables for the living, plain English roots, no apostrophes; the dead keep the names they had. Legendaries keep their spec names.

**The familial rule (Kort, 2026-10-18 late, approved).** The player should carry people, not a gazetteer. In dialog and captions:

- One new proper noun per page at most. A page that introduces a name says everything else on it by role or by relationship.
- Names belong to those who stand beside the player: the four companions, Tomas, and Edric Vane. Everyone else is a role: the old king, the Marshal, the Archivist (Pell gives his name once, himself), the first captain, the Admiral, "the child they sent north". Hesper is named once, by Dunmar; otherwise "his wife".
- No realm or region names in a character's mouth: "the land", "the mounds", "the crown", "the Horn", "the standard", "the paper with your name on it", "the river", "the coast", "the wood", "the hold", "the field of kings". The map names stay in match cards and the battle table; a native may say their own place once. "Barrowlands", "Barrow Crown" and "Barrow Horn" are the bible's words, for this document and the chapter titles, not the copy's.
- Each companion is introduced by relationship before by name: what they are to the player or to each other in their first line, and the name lands a beat later from someone else's mouth.
- After every battle, a fire scene: the company talking to each other, with recurring nicknames for each other and the player that the copy fixes once and keeps.
- The Houses keep their names; they are the game's words. The company name is one word everywhere, so a rename is one replacement.

The `story-voice` check counts proper nouns per page against this rule (section 11).

### 4.9 The Hollow, and its kings (the saga's hidden meaning)

"Hollow" is not Danta's family name. It is the pact's. The company has been the Hollow since the first pact was sealed, and every captain the pact names takes the company's name the way a ship's captain takes the ship: Halden Rook signed *Rook, of the Hollow*; Edric Vane signed the same; Maud wrote *Danta Hollow* this spring because that is how a pact is written, and Danta has never known another name because the camp raised him. He does not know this when chapter 1 opens. Neither does the player.

So "the Hollow King" was never a word for empty eyes. It is a title: the captain of the Hollow who takes the crown for himself. Edric is only the latest. The ring of mounds at Kingsfield holds the Hollow's kings, and the first mound belongs to the first captain who did it, four hundred years ago, before the Houses had names. The crown calls them and cannot send them home because a Hollow who crowns himself is the wrong hand, every time: the Horn's rule wants one hand, and the captain's hand is already on the standard.

The twist is told one piece per chapter, never as a lecture, and chapter 1 plants only the echo:

| Chapter | What lands | Who says it |
|---|---|---|
| 1 | The echo, noticed and dropped. *"Hollow. Like him."* *"Like the company. Eat."* | Wren, Maud, at the first fire Wren shares (`c1_b2_after`, page 3; she joins after battle 2) |
| 2 | The roll of captains in the archive, every name signed *of the Hollow*. Danta's name is the paper's, not his blood's; he does not know who he was before the pact | Pell, then Danta alone with the page |
| 3 | The mounds have a word for the captain who wears the crown for the company, and Edric was not the first | Ashka, who learned it from the clan's dead |
| 4 | The cost of the reforged crown: whoever carries it to the field as the Hollow becomes its king | Dunmar, at the forge |
| 5 | The first mound opens and the first Hollow King stands up. The title was always waiting for Danta. He is the first captain of the Hollow who refuses it, and the Horn goes to the hand | The field |

Rules for the copy: the word "Hollow" is said by the company about itself with pride and by the Crown's Own as an accusation, and the player should not be able to tell which is older until chapter 2. Nobody explains the title before chapter 3. Danta's own name on the pact is shown, not read aloud, on page 1 (the Pact card) so the player sees the form *Danta, of the Hollow* before anyone says what it means. The `story-names` check treats "Hollow" and "the Hollow" as the player's own name, not a new noun.

## 5. The cast

Four companions, met along the road in House order, one voice from the dead, one keeper of the record, and one enemy the player knows by name. Each companion has a want, a wound, a love, a battle where their story turns, and a hook for a later chapter. Each is a real unit in the player's banner from the battle they join, with a fixed troop type from the Houses spec so the harness can place and count them.

| Name | House | Joins | Unit (Houses spec) | Wants | Wound | Turns in | Later |
|---|---|---|---|---|---|---|---|
| Ser Maud Harrow | Gilded Order | Battle 1 | Templar (Gilded Rare infantry) | A living hand for the crown, so the dead can rest and Edric with them | She loved Edric and believes she buried him; her brother Tomas serves the thing he became | 1, 6 | Leaves with Tomas (ch. 2) |
| Wren | Graveborn | Battle 2 | Bone Archer (Common ranged) | To remember how she died | She blew the Horn; she does not know it yet | 2, the shrine, the last page | Takes the crown (ch. 2); the hand (ch. 5) |
| Dunmar Coalbright | Ironclad | Battle 3 | Shieldbreaker (Ironclad Rare infantry) | To open his own gate and let the dead out, not in | He sealed Ironhold; his wife Hesper was inside | 3 | The forge (ch. 4) |
| Ashka of the Thornwood | Wildmoot | After battle 5 | Hide Hunter (Common ranged) | To know why the woods' dead are walking | Her brother Tarn was the toll, eleven years ago, and now he walks | 5, 6 | The heir (ch. 3) |
| Pell, the Archivist | none (camp) | Camp | not a unit | To finish the record | He copied one word wrong, nineteen years ago | Camp | The word (ch. 5) |
| Ser Tomas Harrow | Gilded, Crown's Own | Enemy, 1 and 6 | Templar, enemy banner | To serve the last living crown | He believes it | 6 | Custody, then not (ch. 2) |
| Edric Vane, the Hollow King | Graveborn Legendary | Enemy, 6 | gb_hollowking | The standard | He put the crown on | 6 | Rest (ch. 5) |

### 5.1 Ser Maud Harrow

Forties, Order sergeant, a scar through one eyebrow from the Fall. She has kept the oath the plain way for nineteen years: the king is dead, the crown waits, she serves the standard until there is a hand to give it to. She was Edric's sergeant for seventeen of those years and his lover for twelve, and she does not say so, because the Hollow who are left all know and the new ones do not need to. She is the march's spine. She shows care by counting the player's troops after every battle out loud, and she teaches the player to command because Edric taught her.

She believes Edric died under the mounds two years ago. She scraped his name off the pact on the road out of Kingsfield so that the pact could take another, and she thinks that was the hardest thing she has done. It was not. Battle 6 is.

Her brother Tomas took the other reading of the oath. They have not spoken in nineteen years and she will not say his name until page 8 makes her.

- **Page 2, The River Road.** The turncoats on the crossings are Tomas's company, though he is not there. Maud teaches the player the first lessons (the tutorial boards stay), and her card after the win is: *"Those were Order. Remember that when you count them."*
- **Page 5, The Letter.** She reads Aldric's letter and says, for the first time, that the Horn was blown on his order, and that she was there. She does not say who blew it. She does not know.
- **Page 8, Kingsfield.** The king falls. Maud lifts the helm. The after-battle scene is hers whatever state she is in (section 7.5): standing, she kneels by him; wounded, she is carried to him. She finds Tomas fallen nearby and does not kill him. Either way he is taken alive.

### 5.2 Wren

A Bone Archer who was the Hollow's scout before she was the Hollow's dead. She blew the Horn on day 3 of the Fall and died doing it, rose on the fifth day, and has walked since, following the only thing she still recognises: the standard. She does not remember dying. She does not remember the Horn. She remembers the player's banner, Maud's voice, and that she was good at climbing. At the Moon Shrine (battle 2) she is the last of the dead to lie still, and she lies still on purpose, in front of the player, and then gets up again and follows them out.

She is the voice at the graveyard shrine in camp, where Graveborn are recruited, and every recruit the player takes there she introduces by name if she knew them, which she often did. Her humour is the dry kind the dead are allowed. She is the chapter's heart because she is the only one who cannot be wounded in the usual sense: the script stands her up once per battle after she falls (a companion rule, not the Graveborn trait, which needs Graveborn slots she does not count toward), and if she falls twice she sits out the next battle "resting", which she finds funny.

She remembers one more thing each time the player visits the shrine (section 7.6), and by the last page of chapter 1 she remembers everything, which is the cliffhanger.

- **Page 3, The Moon Shrine.** *"You kept the banner. Good. I could not find anything else of ours."*
- **Page 8, the last page.** The field is searched and the crown is gone. At dawn she is standing on the first king's mound with it in her hands. *"I remember now."* The mounds begin to open behind her. The page turns black.

### 5.3 Dunmar Coalbright

Ironclad shieldbreaker, gate-warden of Ironhold. When the Horn's call reached the mountains the clans sealed the holds: every door shut, the forges banked, the smiths to bed under stone. He sealed the great gate himself, from outside, because the gate-warden seals from outside and checks the seals before he comes in by the postern. The call came through the stone before he reached the postern, and the smiths got up dead. His wife Hesper, master of the lower forge, was among them. He has been outside the gate for nineteen years. He has not forgiven himself for being thorough.

He is slow to speak, exact when he does, and he counts in engines. He built the gate the player must break in battle 3, which is why the bonus is "inside six minutes": he knows exactly how long it takes to break his own work with his own ballistas. He has never gone in.

- **Page 4, Ironhold.** Before: the Ironclad bring the engines and he will not look at the gate. After, in the keep: the smiths' graves are open and one of them is Hesper's, and she is not in it, and he asks the player, the only time he asks anything, to leave the graves open until there is a crown to send them home. He joins, and the Ironclad tent in camp gains his lines. The map's rule that the gate stands broken after this battle is the story's rule too.
- **Chapter 4** is his: the lower forge, Hesper, and what the clans forged.

### 5.4 Ashka of the Thornwood

Wildmoot hunter from the Thornwood north of Frostmarch. The pass has always taken a toll in blood, because the woods are where the Barrowlands left its unburied and the woods keep them quiet. Her brother Tarn was the toll eleven years ago, when their clan crossed south. Now the woods' dead are walking, which means the woods have stopped keeping them, and she wants to know why. She joins after the pass because the Wildmoot's law is that whoever pays the toll in full may take one hunter north with them, and the player's march paid it.

She is the only companion who likes the player straight away, because they bled honestly, and she is the one the player can grow close to across the chapters, slowly, in lines the player chooses to answer or not (section 7.7). She is also the one who has seen the Hollow King's host from the trees, and her report before Kingsfield is how the player learns how large it is.

She does not know she is Osric's daughter. Her clan found a child of nine with a dead guard of twelve in the snow north of Frostmarch, nineteen years ago, and raised her as their own, and gave her a brother, and the brother died at the pass keeping the toll off her. Chapter 3 turns on it. Chapter 1 plants it in three places and says nothing (section 6, pages 7 and 8).

- **Page 7, The Frozen Pass.** *"The snow does not take sides. It takes the slow."*
- **Page 8, Kingsfield.** Her kin stand with the player, as the Phase 4 sides say. She looks at the mounds longer than anyone and does not know why.

### 5.5 Pell, the Archivist

Banner Camp's keeper of the record, in the archive by the Founders' Stone. Old, precise, and wrong once: when he copied the Stone's rule into the camp's book, nineteen years ago, by lantern light, with the Tide a mile off, he wrote *a living hand* where the Stone says *one hand*, because that is what everyone had always said it meant. The march has read his copy ever since. Edric read it. In chapter 1 Pell reads the rule aloud when asked (from the copy), tells the player what a pact is, keeps the roll of the dead that Wren adds to, and says once, if the player asks twice, that he has never checked the copy against the Stone because he is sure of his hand.

### 5.6 Ser Tomas Harrow

Maud's younger brother, sergeant of the Crown's Own. He is not on the field in battle 1 (his company is; he is north with the king). He is at Kingsfield. He believes, completely, that the Hollow King is the last living crown and that his sister broke her oath. He is right about the words and wrong about everything else, and the story never calls him a coward. He falls at Kingsfield and lives. Chapter 2 has him in the march's custody, which is a problem for Maud and a gift for the player, because he saw who took the crown.

### 5.7 Edric Vane

The Hollow King, and before that the second name on the pact. In chapter 1 he is seen twice: on the mounds before battle 6, helmed, asking for the standard; and after, when Maud lifts the helm. One line is his alive, in the storybook opening, on day 4: *"Then I will carry it until there is a hand."* Everything he says dead is kind and about the paper.

## 6. Chapter 1, "The Road North": the pages

The chapter is eight pages. A page is a spread: the left leaf holds the page's title, a short passage, and the company's state (portraits, wounded greyed); the right leaf holds the page's scenes in order, two to four, each a small painted thumbnail with its name and kind. Scene kinds (a battle scene's thumbnail is also the page's way to replay the match): **book** (storybook panels), **stage** (in-engine, before or after a battle), **card** (camp dialog), **battle** (the match itself, shown as a scene so the page reads as a sequence). The Phase 4 table binds the six battles; this section says what surrounds them.

Scene ids are the ones the scene files use.

### Page 1: The Pact
1. `c1_fall` (book, 5 panels). The Fall, day by day: the fog on the rivers; the pact signed; a small figure climbing the highest mound with a horn; the dead standing up out of the mounds against the sea; a lieutenant lifting a crown from a dead king's head, and his line: *"Then I will carry it until there is a hand."* No names on screen except Osric's. The climber is a silhouette.
2. `c1_third_name` (card, camp, Maud at the war table). Two names scraped, hers is the knife, the third is the player's. *"It says the standard goes north. It does not say why. Nobody who knew is alive."* Then the Pact card from the chapter 1 copy.
3. `c1_archive` (card, optional, Pell at the Stone). What a pact is. The rule, read from the copy. The roll of the dead, open on the first page: Halden Rook, then a long list.

### Page 2: The River Road
1. `c1_b1_before` (stage, Rivergate). Order on the crossings. Maud: our Order, the other reading of the oath. Take the bridges one at a time.
2. `battle_1` The Training Field (Rivergate, Domination, Easy; lessons and the free roll stay).
3. `c1_b1_after` (stage, the north crossing). Maud counts. *"Those were Order. Remember that when you count them."* A dropped pact-copy with the king's seal; she takes it and will not explain.

### Page 3: The Moon Shrine
1. `c1_b2_before` (stage, Hollowwood, night). The old graves are opening; he came through here a week ago. Consecrated ground; the dead do not come up through it, they will come at it. *"There is nowhere else to sleep."*
2. `battle_2` The Moon Shrine (Survival, Easy).
3. `c1_b2_after` (stage, the shrine). Whatever the waves left standing lies down as the scene opens (the scene stages it; a timer win leaves no one standing in the story). The last of the dead lies still in front of the player. Then stands. Wren. *"You kept the banner. Good. I could not find anything else of ours."* Maud's one word. Wren joins and takes her place at the graveyard shrine in camp, which recruits Graveborn as it always has (Phase 3's rule; nothing is locked).

### Page 4: Ironhold
1. `c1_b3_before` (stage, Ironhold, the west field). The Ironclad bring the engines. Dunmar will not look at the gate. The brief's briefing lines. Six minutes is how long his gate holds against his ballistas; he says so.
2. `battle_3` Ironhold Retaken (Siege attack, Normal).
3. `c1_b3_after` (stage, the keep, the smiths' graves). Open graves, one with a name he reads and stops. Hesper. *"Leave them open. Until there is a crown to send them home. I will not have them put back twice."* He joins.

### Page 5: The Letter
1. `c1_letter` (card, camp, the war table). A rider from the west: Aldric's letter to whoever holds the Hollow standard. The king's host makes for Kingsfield; the Tide are on the rivers again; the heir's trail is found in the north, hold the standard, do not let it reach the field in the wrong hand. Maud: the Marshal ordered the Horn blown; she was there; she does not know who climbed. First time the player hears the word Horn.
2. `c1_rule` (card, the archive). Pell reads the rule from the copy, now that it means something. *A living hand, one crown, one standard, on the field of kings.* If asked again: he has never checked it against the Stone.
3. `c1_shrine_2` (card, the shrine). Wren remembers that she was good at climbing. She does not know why that matters.

### Page 6: Raiders on the River
1. `c1_b4_before` (stage, Rivergate, fog). The Tide come up the river for the standard. Maud: *"They did not come for gold."* Guard yours, carry theirs home.
2. `battle_4` Raiders on the River (Banner, Normal).
3. `c1_b4_after` (stage, the mill). The raiders' banner is a drowned man's shroud with the crown's seal sewn on it. Vael's name, said once by a dying raider: the Admiral wants the crown under the water, where no hand can take it. Chapter 2's hook, in one line.

### Page 7: The Frozen Pass
1. `c1_b5_before` (stage, Frostmarch, the lodge). The pass takes a toll from every host. The Wildmoot will not be talked past. Take the pass before the snow closes it.
2. `battle_5` The Frozen Pass (Domination, Normal).
3. `c1_b5_after` (stage, the pass, snow). The toll is counted in the dead the player actually lost (the scene reads the match's losses). Ashka steps out of the trees: the law gives the player one hunter. She looks at the standard a moment too long. *"The snow does not take sides. It takes the slow."* She joins. Plant 1: she says her clan found her in this snow.
4. `c1_council` (card, camp, the war table, all four). Each says one thing. Maud: Tomas is there. Wren: she remembers a horn. Dunmar: the engines can reach the centre bridge from the raised outposts. Ashka: he was standing at the mounds when she left the trees, and the host is larger than the march. The player gives no speech. The scene ends on the standard.

### Page 8: Kingsfield
1. `c1_king_before` (stage, the mounds, night). The Hollow King, helmed, crowned. He asks for the pact. He reads his own seal. He says Danta's name, because it is on the paper. *"You have carried the standard to the field of kings. Well done. Now give it to me."* Maud: *"Do not answer him."* The player does not. The battle starts when the card closes.
2. `battle_6` Kingsfield (Domination, five points, Hard). Tomas's banner is on the field, script-protected. The bonus, strike down the Hollow King with your own banner, is the story's demand too. The battle has no boss rules: the king is a Legendary in a bot banner and the bots play him as they play any Legendary. When the match ends in the player's favour, however it ended (points, or his death by anyone), the after-scene stages the same fact: the host broke and the king is down on the mound. If the player's banner killed him, one of Maud's lines says so (`if match killed:gb_hollowking`); if he was alive at the end, the scene shows him on his knees on the mound as the dead stop answering him, and he falls. Tomas kneels where the script puts him at the end, whatever his HP.
3. `c1_king_after` (stage, the mounds, dawn coming). The king down. Maud lifts the helm. Edric. He does not know her. He says the paper's name again, kindly, and stops. Maud's three lines, the longest she ever says. Tomas is found fallen and taken alive. Wren looks at Edric's face: *"I saw him lift it. The first morning. I was on the ground and I saw him lift it."* Dunmar's engines are silent. Ashka is looking at the mounds. Plant 2: one mound has a child's name on it that nobody reads aloud. Then the end-of-match report, as Phase 4 says: the spec's epilogue line, *"Kingsfield holds. When the field was searched, the Hollow King's crown was gone."*, and the "Choose your Legendary" button. The book's last scene waits behind it.
4. `c1_crown` (book, 2 panels, unskippable the first time, the cliffhanger; it plays when the player next opens the book or returns to camp, whichever is first). Dawn. The first king's mound, the highest. Wren on top of it with the crown in both hands, the way she once held a horn. *"I remember now."* The mounds open behind her, all of them. Black. Chapter 2.

### 6.1 The plot twist and why it lands

The player spends seven pages learning the oath, the Horn, the pact and the standard through four people who each carry one of them, and watching Maud not say a name. On page 8 the dead king holds up the paper that makes the player who they are and asks for the thing they have carried the whole road. The player refuses and strikes him down with their own banner, which the game rewards. Then Maud lifts the helm, and the player learns they have killed the man who wrote the pact, the man Maud loved, the second name under their own, who did exactly what the player is doing and ended like this. Nothing earlier was a lie: Maud's silence, the scraped name, the king's kindness, Wren's "I saw him lift it", the Pact reward itself (the seal recognised its own captain's hand).

Then the crown is gone, and the last page shows who has it, and why the mounds are opening, and the chapter ends with the dead kings standing up again and Wren remembering. Chapter 1 ends worse than it began, with four people who would follow the player anywhere, which is the point.

### 6.2 Sorrow and despair, where they live

- Maud at the helm (page 8), and every earlier scene where she counts troops and does not say his name.
- Dunmar at Hesper's open grave (page 4), and his refusal to let the dead be put back twice.
- Wren introducing the player's own fallen by name at the shrine, every visit.
- The toll at the pass counted in the player's real losses (page 7).
- The Pact reward, which the player now understands was Edric's seal.
- The last page: the dead the player fought all chapter were never the enemy; the Horn was.

## 7. How the story is told in the game

### 7.1 The storybook

The storybook is the campaign's whole front end, replacing the march page's battle list in the UI plan's Task 7 **[call]**. The War Table's plaque for it keeps the Phase 4 spec's naming rule ("Chapter" is the Founders plaque's word), so the plaque reads "The March" and opens the book.

- **The table.** The book lies closed on the war table, a painted leather cover with the chapter's numeral in a wax seal. Opening the Chapter plaque shows the table; after a beat, wind lifts the cover and the pages, and the book settles open on the player's current page (the first page with an unread scene). The wind is three to four leaves turning, not a shuffle. On a first ever open it settles on page 1.
- **The spread.** Left leaf: the page's title, a short passage in the body font with a drop capital, and the company (portraits, wounded greyed with a small bandage glyph, Wren "resting" with a small moon). Right leaf: the scenes, two to four, each a painted thumbnail (a small still of the scene's own stage or panel), its name in the title font, its kind in small caps, and a state: read (a small quill mark), next (the thumbnail lit, a soft glow that breathes), sealed (a wax seal over the thumbnail, dimmed). A battle scene's thumbnail shows the map and mode glyph.
- **Turning.** Corners turn pages, as do the arrow keys and the scroll wheel. The player can turn back to any read page and replay any read scene. Sealed scenes do nothing. Chapters not yet reached show as a thick sealed section at the back.
- **Playing.** Clicking the next scene plays it. A stage or book scene fades from the table into the scene and back; a battle scene fades into the match lobby with the briefing card (Phase 4 spec section 8). A before-the-battle scene hands straight to its battle scene when it ends, so the player never returns to the table between them.
- **Skipping.** A scene cannot be skipped the first time (Kort's rule). The skip key is drawn greyed with "first reading" the first time; on replay it skips to the end. Typing can always be hurried with the use key.
- **Art.** The book, the leaves, the thumbnails, the portraits and the panels are painted through the designer pipeline (`tools/ui_art`) at the detail Kort set for the War Table (two to three times the first round's pixel detail), with the brass, vellum and iron tokens of the UI kit, and graded against the UI rubric before Kort grades them. The web demo of 2026-10-18 is the flow only.

### 7.2 Three kinds of scene

1. **Book** (`panel` scenes): layered pixel-art panels, two to four layers each (sky, far, near, figures), slow pan or parallax per layer, a caption line in the title font, text typed on a vellum strip, optional voice. Used for the chapter opening and the cliffhanger. Painted by the designer pipeline.
2. **Stage** (`stage` scenes): the game world is the set. The scene script moves the camera along keyframes (the title demos' camera hook, generalised and moved out of the test tools), places actors (real units, posed and frozen or walking a path), sets time, weather, particles and sound, and shows dialog cards. Played on the battle's own map before it builds its lobby, so a scene shows the gate it is about to break.
3. **Card** (`card` scenes): dialog cards at the war table, the shrine, a tent or the archive, with no camera move; the player walks up and presses the use key, or opens them from the book.

### 7.3 Scene files

Scenes are text, one file per scene, under `src/main/resources/data/gravesandglory/scenes/<chapter>/<id>.scene`, loaded as a resource. Line 1 is `# scene <id> <kind>` with kind `panel`, `stage` or `card`. Every other line is a comment or one command. One line, one thing, no nesting. The chapter's page layout is a sheet (`storyPages`: chapter, page, order, sceneId, kind, title) so the book is data too.

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
- `map <n>` or `camp`; `time`, `weather`, `fog`.
- `actor <id> <unit or companion:<name>> at x y z facing <yaw> [pose <name>]`; `walk <actor> to x y z over <ticks>`; `pose <actor> <name>` (poses: the title demos' arm poses plus `kneel`, `fallen`, `stand`, `helm_off`; **[call]** six poses, no custom animation in chapter 1).
- `camera x y z to x y z over <ticks>`, `camera hold x y z look <actor>`, `camera player`.
- `card item:<id> "<text>"`: an object's text on the vellum card, no portrait, no voice (the scraped pact, the roll page, the shroud). **[call, from the copy]**
- `card <speaker> "<text>" [voice <id>]`: at most three wrapped lines. Text may hold `{count}` (troops standing after the match), `{losses}` (troops lost in the match) and `{bonus}` (the match's bonus word); a voiced line never carries a substitution. The player is named (Danta), so lines that say his name are plain text and can be voiced.
- `panel <png> [pan dx dy over <ticks>] [layers n]` (book only); `caption "<text>"`.
- `structure <id>`: places a structure piece for a stage scene (page 4's before-scene shows `ironhold/gate_intact` standing, as Siege will).
- `sound <id>`, `particles <type> at x y z count n`, `wait <ticks>`, `fade in|out <ticks>`, `end`.
- `if companion:<name> standing|wounded|resting` and `if match killed:<unit> | bonus | won` ... `else` ... `endif`: the only branches.

Lines that are not scenes (reaction, wounded, shrine, Pell, the grave roll) are table-shaped and live in sheets, not scene files: `storyLines` (companion, trigger, priority, text, voice; choice rows add `warm` and `plain` for the chits) and `graveRoll` (troopType, name, line, voice). `storyPages` carries, on a page's first row, `pageTitle` and `passage` (the leaf text) and a `want` per companion for the left leaf **[call, from the copy]**. Object text (the pact, the Stone, the shroud) lives in an `objects` sheet (id, text). The `story-voice` check scans both the scene files and the sheets.

### 7.4 Dialog cards

The UI kit's vellum card with a 32x32 pixel portrait at the left, the speaker's name in the title font, the line in the body font, typed at two characters per tick with a soft quill sound, and a small continue glyph. Portraits are pixel art, one per speaker (eight in chapter 1: Maud, Wren, Dunmar, Ashka, Pell, Tomas, the Hollow King helmed, Edric), drawn by the designer pipeline and graded like any other art. Cards never cover the Iron Ledger's seal row. In battle the only cards are the king's two scenes; everything else is before or after.

### 7.5 Companions as units

- Companions exist only in campaign battles. They never appear in Skirmish, co-op Survival or any multiplayer match, and their slots are not part of the 20-slot banner: a campaign battle fields the player's banner plus a company row of up to four, drawn as portraits on the War Table banner page and on the book's left leaf. Company troops count toward no House trait tier, take no stars or evolution, and cost nothing; the story loans them. **[call]** This keeps the Houses spec's banner maths and Phase 4's "buys only owned troops" untouched, and battle 1's full starter banner stays as verified.
- In battle the companion is a normal unit of its type with the name over its head in the side colour, counted by the group it is in. Orders work on it like any troop.
- **Falling.** At 0 HP a companion does not die. It goes to a `fallen` pose where it stood, is untargetable, and a feed line says who fell. After the battle the companion is `wounded` until the player next finishes the chapter's next unwon battle, win or lose: in every campaign battle launched meanwhile, replays included, its slot is empty and its portrait greyed in the book and on the table, and its camp lines are the wounded set. Replaying an old battle therefore does not heal anyone. Companions who joined later do appear in replays of earlier battles (the replay is the player's, not the story's). Reaction lines play after every campaign battle; before-scenes play once, on first reading; after-scenes play once, on the first win. Wren is stood up once per battle by the script after she falls and is "resting" after a second fall, never wounded, which her lines joke about.
- **Only the script kills.** No companion dies in chapter 1. Tomas is script-protected at Kingsfield (section 7.8).
- **Reaction lines.** After each battle the camp picks lines from what happened: a companion who fell, a rout of the player's group, a bonus earned or missed, losses above a threshold. One line per fact, at most three per visit, in a fixed priority. Written per companion per battle (section 8 sizes it).

### 7.6 The shrine and Wren's memory

Wren's memory advances once per return to camp after a chapter battle, win or lose, six in chapter 1 (the first is `c1_shrine_2`), and does not depend on anything else, so a player who never loses still hears all six before `c1_crown`. **[call]** The six, in order: she was good at climbing; the fog had a sound; someone gave her something to carry; it was heavy and it was not a bow; she was higher than everyone; she was cold before she was anything else. Page 8's last scene is the seventh.

When the player recruits a Graveborn troop at the shrine, Wren says a line from a sheet (`graveRoll`: troop type, name, line): she "knew" the kind of dead that troop is, and gives it a byname from the roll (the ferryman, the bridge guard; the familial rule keeps proper names to the company). Bynames are flavour in a sheet, not saved per troop; the game's troops stay types with stars, as the Houses spec says. Pell's roll of the dead is the same sheet read in the archive.

### 7.7 Love, and what the player can do about it

Three loves, three ways:
- **Maud and Edric** is over before the chapter starts and the player only learns it on page 8. It is told in silences: she never says his name, she counts troops the way he taught her, the second scrape on the pact is deeper than the first.
- **Dunmar and Hesper** is a grave. He says her name once, at it, and once more in chapter 4.
- **Ashka and the player** is the one that can grow. From page 7 on, some of her camp cards end with a choice of two replies for the silent commander, drawn as two small vellum chits (the game's first and only choice UI in chapter 1 **[call]**): one warm, one plain. Neither is wrong. The game counts the warm ones. Nothing in chapter 1 depends on the count; chapter 3 reads it. The player who never picks warm still gets her whole story.

### 7.8 Script protection

A unit with the `protected` tag (companions, Tomas at Kingsfield) takes damage normally but cannot go below 1 HP from any source; at 1 HP it falls (companion rule) or, for Tomas, kneels and is ignored by bots. The Hollow King is not protected in the battle: the bonus requires his death by the player's banner, and `lastHitBy` already tracks it.

### 7.9 Voice

Every `card` line has a `voice` id. A voice line is an `.ogg` under `assets/gravesandglory/sounds/voice/<chapter>/<id>.ogg`, listed in `sounds.json`. If the file exists it plays and the typing matches its length; if not, the card types silently. Subtitles are always the card text, so nothing depends on audio. Kort has not decided who voices whom (himself, friends, possibly synthesised); the copy is written now with a one-line direction per speaker so any of those can record later, and every card is complete silent.

### 7.10 Where the lore lives in the game

- The Founders' Stone in the archive: the rule, readable, from the copy. (The Stone's own letters are drawn on the piece. A player who reads the piece's text and the copy's text can find the difference in chapter 1. Nobody in the game points it out until chapter 5.)
- Pell: the roll of the dead, what a pact is, what the Horn is (after the letter).
- The storybook (the War Table's March plaque): every scene, replayable, and the company's portraits with their one-line wants.
- The shrine: Wren.
- The tents: each companion's camp lines.
- Item text: the scraped pact, the turncoat's pact-copy, the drowned shroud. Three objects, each a hook.

## 8. Writing the copy

Rulings from the copy, 2026-10-18 late: Tomas is "her brother" until Kingsfield, speaks once as that in `c1_king_before`, and Maud says his name in `c1_king_after` after Edric's, so page 8 is the one page with two new names (Kort overturned the copy's call to wait for chapter 2); the bonus-missed reaction line plays only on a win without the bonus; plant 3 (Ashka as the heir) sits in her second choice card, a tent card the player may never open, which is acceptable because plants 1 and 2 carry the twist; `c1_king_after` stages Tomas's capture without a card; page 4's leaf is "The Gate" and page 8's "The Field of Kings"; battle 6's feed lines are "The King's host is broken." and "The King's host holds." The copy is at `docs/copy/chapter-1.md`.

Volume for chapter 1, sized so Kort can read it in one sitting:

| Piece | Count | Words each |
|---|---|---|
| Book panels (fall 5, crown 2) | 7 | under 30 |
| Stage scenes (12, two per battle) | 12 | 3 to 6 cards |
| Camp cards (third name, archive, letter, rule, shrine 2, council) | 6 | 2 to 4 cards |
| Reaction lines (4 companions x 6 battles x up to 4 facts) | about 80 | under 20 |
| Wounded lines (3 companions x 2, Wren resting x 2) | 8 | under 20 |
| Shrine lines (Wren, 6 plus the grave roll) | 6 + roll | under 25 |
| Ashka's choice cards (pages 7 and 8) | 3, two replies each | under 20 |
| Pell | 6 | under 40 |
| Object text | 3 | under 40 |

About 200 lines, roughly 2,800 words. Every line carries a speaker, a scene id, a voice id and a one-line direction for the voice.

Voice directions: the Record (the book's captions), plain and unhurried, a reader not a player; a raider of the Tide (`c1_b4_after`), drowned and failing, sure of the Admiral; Maud, level and low, never raises it; Wren, light, a little late on every line, as if listening; Dunmar, slow, every word placed; Ashka, quick and warm; Pell, precise, old; the Hollow King, kind, which is the horror of it; Edric alive (one line), young and certain; Tomas, certain.

## 9. Chapters 2 to 5: the plan

Each chapter meets one House on its own ground, answers one question, turns on one twist, and ends on one cliffhanger. The cast grows or breaks by one per chapter. Nothing below binds battles or maps; new maps are their own design. Everything below binds chapter 1's writing, because chapter 1 is written toward it.

### Chapter 2: The Drowned Coast (Tidecallers)
- **Opens on the cliffhanger.** Wren is gone from the camp with the crown. The mounds at Kingsfield stand open and empty; the dead kings have walked. Tomas, in custody, says he saw her take it and knows where the dead are going: the sea.
- **Question answered.** Where the crown went, and what the Tide wants. Wren remembers blowing the Horn and remembers what the crown did to Edric; she is carrying it to the coast to drown it so that no hand can ever wear it. Vael wants exactly that, because a crown under the sea sends nobody home and the drowned are his.
- **The chase.** Down the rivers to the coast through Tidecaller ground, Tomas guiding, Maud not speaking to him, then speaking.
- **The name (section 4.9).** Before the coast, the archive: Pell lays the roll of captains open, and every name on it is signed *of the Hollow*, Rook and Edric among them. Danta's surname is the pact's. The sorrow beat of the chapter is Danta alone with the page, asking Maud what he was called before, and Maud not knowing.
- **Twist.** Vael was at Kingsfield the night the Horn was blown, on the mound, and he carried Wren's body down. He has been trying to reach the Horn-blower for nineteen years. He does not want to drown the crown. He wants the hand.
- **Cliffhanger.** The player takes the crown back from the shore. Maud puts her sword down, takes Tomas's hand, and walks west with him to find the heir herself, because the Marshal's letters have stopped and somebody must. The Hollow lose their sergeant. The last page: Aldric's final letter, delivered a month late, one line: *The heir is dead. I found the guard.*

### Chapter 3: The Thornwood (Wildmoot)
- **Question answered.** Why the woods stopped keeping the dead: Old Horn's bargain was with the crown, and it lapsed when the crown walked. The woods will keep their dead again for a crown that is worn, which is why the Wildmoot want the player's crown as much as the Tide did.
- **Twist.** The guard Aldric found was Elin's. The child was not among them. Ashka's clan found a child in that snow. Ashka is Osric's daughter: the living hand. Tarn died keeping the toll off her because the clan knew what she was and never told her. The warm count from chapter 1 decides how she hears it from the player (not whether).
- **The name (section 4.9).** Ashka, who grew up among a clan that talks to its dead, tells Danta what the mounds call the captain who wears the crown for the company, and that Edric was not the first. She is the one who says the words "Hollow King" as a title, not a name, and the player hears the chapter 1 echo land.
- **Cliffhanger.** Old Horn kneels to her at the heart of the Thornwood, every hunter with him. Ashka looks at the crown in the player's hands and runs. The last page: Aldric's riders at the edge of the woods, come for the heir, with Tomas and Maud among them.

### Chapter 4: The Deep Hold (Ironclad)
- **Question answered.** What the forge-mark on Brassjaw means: the clans forged the Barrow Crown four hundred years ago, and a crown can be reforged for a hand that will not take it, if the forge is lit by the one who banked it. Dunmar goes in through his own gate.
- **Twist.** Hesper is in the lower forge, dead and working, and she has kept the forge lit for nineteen years waiting for him to open the door, because the hold remembers every siege and so does she. The reforging needs the crown melted, which frees Edric's rest and every dead thing's; Dunmar asks the player not to, because it also frees her.
- **The name (section 4.9).** Dunmar names the reforged crown's cost at the forge: whoever carries it to the field as the Hollow becomes its king, and the mound ring has room. The forge choice below is the player's first look at that door; the chapter does not let him walk through it yet.
- **Cliffhanger.** The player chooses the forge (the saga's second choice, binary, remembered): the crown is reforged or it is not. Either way Dunmar stays in the hold. The last page: Aldric has Ashka, and is marching her to Kingsfield to crown her on the mounds, with the Crown's Own at his back, because the oath says the last living crown and he has decided she is it.

### Chapter 5: The Barrow Horn (Kingsfield)
- **Question answered.** Pell's word. The player, or Pell, finally reads the Stone against the copy: *one hand*, not *a living hand*. The rule never needed a living king. It needed the hand that called them: the hand on the Horn. Aldric's crowning of Ashka on the mounds does nothing, and he knows it on the mound-top, and says nothing.
- **Twist.** Wren has known since the last page of chapter 1. She took the crown to the sea not to drown it but to find the Horn, which Vael kept. She has it. She is the hand. She is dead, and the rule never said living.
- **The first mound (section 4.9).** When Aldric's crowning of Ashka fails, the first mound opens and the first Hollow King stands up: a captain four hundred years dead, the oldest form of the broken crown on his head, and he turns to Danta as to an heir, because the title was always waiting for him. The reforged or unreforged crown is in Danta's hands. He is the first captain of the Hollow who refuses it. That refusal is staged, not chosen: the player has already made the saga's choices, and this is who Danta is. The Horn goes to the hand.
- **The ending, the saga's one real choice.** Wren on the first king's mound with the Horn, as she was. If she blows it, the dead go home: the kings, the hosts, Edric, Hesper, every Graveborn the player recruited at the shrine, and Wren. If she does not, the Barrowlands keep their dead and their captain keeps her. The player decides, and the game remembers, and chapter 5 ends either way on a quiet field at dawn with the standard planted on the mound.
- **The close.** No cliffhanger. The saga ends whole: the standard planted, the company accounted for, every companion's want answered one way or the other. The last page is a page of the roll of the dead in Pell's hand, with *Danta, of the Hollow* written at the bottom as captain, the first name on the roll of captains with no mound beside it, and under it a blank line. That blank line is the door a sixth chapter walks through (Kort: complete, but able to be added onto), and the mound that did not open is the ground it stands on. The field the dead left, or the field they kept, is the ground a chapter 6 would start from.

Aldric is in every chapter as a letter and in chapters 3 to 5 in person. Vael, Old Horn and Brassjaw are each met on their own ground, and each is recruitable by pact after their chapter, as the Houses spec allows.

## 10. The timeline, in one table

| When | What | Who knows it in chapter 1 |
|---|---|---|
| 400 years ago | The clans forge the Barrow Crown; the first king is crowned and buried at Kingsfield; the Horn's rule is carved | Pell (the copy), nobody (the Stone) |
| Year 0, day 1 | The Tide lands at Kingsfield | Everyone |
| Day 2 | Elin sent north; the pact signed, Halden Rook its first name | Maud, Pell |
| Day 3 | Aldric orders the Horn blown; Wren climbs and blows it; she dies; Rook dies over her | Maud (the order, not the hand); Wren (by the last page) |
| Day 4 | Osric dies crowned; Edric lifts the crown, writes his name, marches the Hollow north; Vael carries Wren's body down | Maud; Wren (the lifting); Vael |
| Day 5 | Wren rises and follows the standard | Maud, Wren |
| Year 0 to 2 | The search for Elin; the guard found dead in the snow by Aldric, years later; the child raised by the Thornwood clan | Aldric (the guard); the clan |
| Year 8 | Tarn dies at the pass keeping the toll off Ashka | The clan |
| Year 17 | Edric crowns himself at the mounds; becomes the Hollow King; Maud scrapes his name | Maud |
| Year 19, spring | Danta Hollow's name on the pact; Banner Camp | Everyone |
| Year 19, the chapter | The Road North | |

## 11. Harness and gates

- A `scenes` suite plays every chapter 1 scene headless with the typing hurried, checks each `card` draws (text, speaker, rect), each `camera` reaches its end point, each `actor` is placed and posed, each `panel` loads its layers, and takes one screenshot per scene at its last card.
- A `storybook` suite: the book opens on the current page; read, next and sealed states draw correctly for three progress fixtures; a before-the-battle scene hands to its battle; a sealed scene does nothing; skip is refused on first reading and allowed on replay; the wounded portrait greys.
- A `companions` suite: a companion falls and is untargetable; the next battle's slot is empty and greyed; the wounded line plays; Wren rises once and is "resting"; Tomas at 1 HP kneels; a reaction line plays for a fall, a rout and a bonus, in priority order; `losses` substitutes the real count.
- A `story-voice` check: every `voice` id in the scene files is either in `sounds.json` or in a listed "silent" set, and no id is in both.
- A `story-names` check (the familial rule, section 4.8): a listed set of proper nouns (cast, places, the Horn, the crown) is counted per storybook page across scene files and sheets; a page introduces at most one noun not seen on an earlier page (chapter 1 page 8 is allowed two, Edric and Tomas, by Kort's ruling), and the realm names never appear in a `card` or `caption`.
- Art gates: every book leaf, thumbnail, portrait and panel is graded against the UI rubric before Kort sees it, as the mockups were.
- Kort's gate: he plays chapter 1 start to finish in one sitting with the scenes on, and says, for each companion, what they wanted, and who the Hollow King was. If he cannot for any one of them, that part is rewritten before the chapter ships.

## 12. Order of work

1. Kort approves this document (section 13 answered). Then the chapter 1 copy is rewritten from it: every line with its scene id, speaker, voice id and direction, in `docs/copy/chapter-1.md`.
2. The Phase 4 plan (modes and chapter 1) is written with the story in it: the storybook, the scene loader, dialog cards, companions, protection, the choice chits, the four suites. Scenes are data from day one. The UI plan's Task 7 Chapter page becomes the book.
3. The storybook art (book, leaves, thumbnails, eight portraits, seven panels) goes through the designer pipeline and the UI rubric, after the unit identity pass so companions look like their Houses.
4. Voice is recorded after the copy is locked and the scenes play silently in the game, so the timing is known. Who voices whom is Kort's call later.
5. Chapter 2's design starts after Kort's chapter 1 gate.

## 13. Questions for Kort (answered 2026-10-18 late)

1. **Ashka is the heir** (chapter 3's twist) and the player's slow love interest. Both, one, or neither. **Kort: both.**
2. **Maud leaves with Tomas** at the end of chapter 2 and comes back on the wrong side in chapter 3. Yes or no. **Kort: yes.**
3. **The saga ends on the player's choice** (chapter 5: Wren blows the Horn or does not), with no chapter 6 planned. Yes, or plan a sixth. **Kort: end it there, make it feel complete, but leave it able to be added onto.**
4. **Choice chits** for Ashka's cards (two replies, warm or plain) are the only choice UI in chapter 1. Fine, or no choices at all until later. **Kort: fine.**
5. **The Stone's real words are visible in the game from chapter 1** for a player who reads carefully. Fine, or hide it until chapter 5. **Kort: fine.**

---

## Calls made in this draft

- No branching plot in chapter 1; one `if standing|wounded|resting` branch in scene files; Ashka's warm count is the only state carried forward.
- Six actor poses, no custom animation; the title demos' camera and pose hooks generalised rather than a new system.
- Scenes as resource text files; the page layout as a sheet.
- The storybook replaces the UI plan Task 7 march page's list; the plaque reads "The March".
- Companions' unit types from the Houses spec's Rare and Common rows; exact ids at plan time.
- Protection at 1 HP for companions and Tomas; the Hollow King unprotected and without boss rules in the battle; the after-scene stages his fall whatever ended the match.
- Companions are campaign-only, a company row beside the 20-slot banner, loaned by the story.
- The Barrow Crown is broken (the model's broken crown); chapter 4 reforges it.
- The kings' mounds are a new Kingsfield landmark for Phase 3.
- The realm's name (the Barrowlands), the king (Osric), the heir (Elin), the first captain (Halden Rook), the Archivist (Pell), Dunmar's wife (Hesper), Ashka's brother (Tarn).
- Chapters 2 to 5 plotted now, with twists and cliffhangers, so chapter 1 is written toward them.
