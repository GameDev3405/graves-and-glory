# Chapter 1 copy: The Long March, I: The Road North

Player-facing strings and the full script for campaign chapter 1. The battle strings were written 2026-10-18 from the Phase 4 spec (`docs/superpowers/specs/2026-10-18-phase-4-modes-campaign-design.md`, sections 8, 10 and 11) and the Houses spec. The script was written the same day from the approved campaign story design v2 (`docs/superpowers/specs/2026-10-18-campaign-story-design.md`). Nothing here is in a sheet, a scene file or code yet. Status: draft for Kort's review; the spec's own words are approved, the new lines are not. "s10" below means Phase 4 spec section 10; "story s6" means the story design's section 6.

## Where each string goes

| String | Sheet | Column | Basis |
|---|---|---|---|
| Battle name | `campaignBattles` | `name` | s10, s11 |
| Briefing | `campaignBattles` | `briefing` | s8, s10 |
| Reason | `campaignBattles` | `reason` | s8, s10 |
| Bonus line | `bonusObjectives` | `text` | s11 |
| Bonus feed tag (new column) | `bonusObjectives` | `tag` | spec silent |
| Victory feed line (new column) | `campaignBattles` | `victory` | spec silent |
| Defeat feed line (new column) | `campaignBattles` | `defeat` | spec silent |
| How to, attack, 3 lines | `modes` | `howTo`, lines split by a vertical bar | s11 |
| How to, defend, 3 lines | `modes` | `howToDefend`, same form | s11 |
| Rise site names | `modeSites` | `name`, rows of kind `rise` | s11 |
| Mode and bonus feed lines | none: `FeedLines` over `feed_v1` | n/a | s8 names the lines, no sheet |
| Epilogue | `chapters` | `epilogue` | s10, s11 |
| Writ card: title, body, note (new column) | `chapters` | `writText`, 3 lines split by a vertical bar | spec silent |
| Button "Choose your Legendary" | code (recruit page, Writ view) | n/a | s10 |
| Camp guide blurb (new column) | `chapters` | `blurb` | spec silent |
| Scene name on its thumbnail | `storyPages` | `title` | story s7.1, s7.3 |
| Page title and leaf passage (new columns) | `storyPages` | `pageTitle`, `passage`, on the page's first row | story s6, s7.1; columns story silent |
| Company wants on the left leaf (new column) | `storyPages` or a companion sheet | `want` | story s7.10; sheet not named |
| Card and caption lines | scene files, `scenes/c1/<id>.scene` | `card`, `caption` | story s7.3 |
| Item cards inside scenes | scene files | `card` with an item in place of a speaker | story s6 page 1; command not in the list |
| Reaction, wounded, resting, shrine, recruit, Pell and choice lines | `storyLines` | `companion`, `trigger`, `priority`, `text`, `voice` | story s7.3, s7.5 |
| Choice chits (new columns) | `storyLines` | `warm`, `plain` on the choice row | story s7.7; columns story silent |
| Grave roll | `graveRoll` | `troopType`, `name`, `line`, plus `voice` (new column) | story s7.6 |
| Object text | item text; sheet not named | n/a | story s7.10 |
| The Stone's own letters | the archive's Founders' Stone piece, drawn | n/a | story s4.2, s7.10 |

How these were written:

- Verbatim from the spec: battle names, briefings, reasons, bonus lines, the epilogue, the Siege how-to lines and the button label. They are marked "spec, verbatim" where they appear. Everything else is new.
- Feed lines fit the feed slip: measured in the default 8 px font (advances read from the 1.20.1 `ascii.png`) against its 140 px text window (UI plan, Task 3), width in px after each line. Five of the spec's example lines did not fit; see "Fit changes" below.
- `{team}` is the acting side's short name, shown as Dawn and Dusk as in the spec. Measured with the Phase 2 side names too (Gold, Violet, Crimson, Teal), only the Siege yard line overflows, and only for Crimson (143 px).
- How-to lines are under 60 characters (count in brackets) and carry no tunable numbers, except Siege's own "four times".
- Defeat lines are true for a draw as well as a loss, so one line serves both. Victory and defeat lines are plain notices with the emblem glyph.
- Script cards: under 20 words each, one breath, said by tired people, at most three wrapped lines in the vellum card, about 100 characters at most. Book captions under 30 words. Leaf passages under 60 words. Object text under 40 words. Ashka's chits under 12 words. The builder measures card wrap; any card that wraps to four lines comes back here.
- Nobody speaks more than three cards in a row. The commander never speaks except through Ashka's chits. Maud does not say Edric's name before page 8. Nobody says the word Horn before `c1_letter` on page 5, and Wren does not say it before `c1_council` on page 7. The Hollow King's lines are kind and about the paper.
- The commander is Danta Hollow (he, him): "Danta" in dialog, "Hollow" when the speaker is formal or hostile. No line uses `{player}`; a line that addresses him says Danta and is voiced. A line that carries `{count}`, `{losses}` or `{bonus}` is typed, not voiced, as story s7.3 rules; its voice id is reserved and listed in the silent set at the end.
- The familial rule (Kort, 2026-10-18): at most one new proper noun per storybook page. Names only for the people who stand beside the commander: Maud, Wren, Dunmar, Ashka, Tomas and Edric. Everyone else is a role: the old king, the Marshal, the Archivist, the first captain, the Admiral, the child they sent north; Dunmar's wife is named once, by him. No realm or region names in dialog or captions, and the map names stay on the match cards and the battle table. Each companion's first line says what they are to the company, and the name lands a beat later from someone else's mouth; until then their card shows their role. After every battle there is a fire, and the company's nicknames for each other are set once and kept (see "Names and nicknames"). The Houses keep their names.
- The company is "the Hollow", always in that form, never a variant. "Hollow" alone is also Danta's surname and the formal or hostile way to address him, so a rename of the company searches "the Hollow" and checks each bare "Hollow" by hand, skipping "Hollow King", "Hollowwood" and `gb_hollowking`. "Danta" and the company's "Hollow" are the commander's own name and do not count as new nouns.
- ASCII only, no dashes as punctuation, no exclamation marks, no modern idiom. "Chapter" is the Founders plaque's word, so no on-screen string here uses it.

## The six battles

Map, mode, difficulty, banners and bonus are from s10 and s11. The heading, briefing, reason and bonus are the spec's words, verbatim; the tag, victory and defeat lines are new. The scenes around each battle are in "The script" below.

### 1. The Training Field

Map: Rivergate Valley (map 0). Mode: Domination. Difficulty: Easy. Allies: none. Enemy: `march1_turncoats` (10 Order Squires, 6 Order Archers, four empty slots). Bonus: `hold_all`, Gilded shards.

- Briefing (spec, verbatim): Turncoat knights hold the river road. Take the crossings and teach your banner to follow you.
- Reason (spec, verbatim): The road west must open before the march begins.
- Bonus (spec, verbatim): Hold all three crossings at the same moment.
- Bonus tag: hold all three (kept line 130 px, lost line 128 px)
- Victory feed: `The road west is open.` (114 px)
- Defeat feed: `The road west stays shut.` (132 px)
- Story: page 2. `c1_b1_before` hands to this battle; `c1_b1_after` plays on the first win.
- Note: Replaces the live sheet's briefing, which still says "Dusk raiders". The four lessons are unchanged and stay in code (`GravesAndGloryClient.lessonText`).

### 2. The Moon Shrine

Map: Hollowwood Reach (map 3). Mode: Survival. Difficulty: Easy. Allies: none. Enemy: the "moon" waves at Easy scale (`waveSet` moon). Bonus: `lose_at_most 8`, Graveborn shards.

- Briefing (spec, verbatim): The old graves of Hollowwood are opening. Hold the shrine until the last of the dead lies still.
- Reason (spec, verbatim): The march camps at the moon shrine tonight; there is nowhere else to sleep.
- Bonus (spec, verbatim): Lose no more than 8 troops.
- Bonus tag: few losses (kept line 115 px, lost line 113 px)
- Victory feed: `The dead lie still.` (85 px)
- Defeat feed: `The dead hold the shrine.` (129 px)
- Story: page 3. `c1_b2_before` hands to this battle; `c1_b2_after` plays on the first win.

### 3. Ironhold Retaken

Map: Ironhold Siege (map 1). Mode: Siege (attack). Difficulty: Normal. Allies: `ironclad_normal` (its mortar and four ballistas). Enemy: `graveborn_easy`, `graveborn_normal`, defending. Bonus: `stage_by gate 360 s`, Ironclad shards.

- Briefing (spec, verbatim): The dead took Ironhold while its smiths slept. The Ironclad bring the engines; you bring the swords. Take the yard, break the gate, seize the keep.
- Reason (spec, verbatim): The wagons cannot pass while the dead hold Ironhold.
- Bonus (spec, verbatim): Break the gate inside six minutes.
- Bonus tag: gate in time (kept line 117 px, lost line 115 px)
- Victory feed: `Ironhold is retaken.` (100 px)
- Defeat feed: `The wagons turn back.` (113 px)
- Story: page 4. `c1_b3_before` hands to this battle; `c1_b3_after` plays on the first win. Dunmar's "six minutes" in `c1_b3_before` is the bonus's draft value; if `stage_by` is tuned, his line changes with it.

### 4. Raiders on the River

Map: Rivergate Valley (map 0). Mode: Banner. Difficulty: Normal. Allies: `ironclad_easy` (takes the guard role, so you raid). Enemy: `tidecallers_easy`, `tidecallers_normal`. Bonus: `carry_self`, Tidecallers shards.

- Briefing (spec, verbatim): Tidecaller raiders have come up the river for the march's banner. Guard yours and carry theirs home.
- Reason (spec, verbatim): Without its banner the march is just a crowd.
- Bonus (spec, verbatim): Carry their banner home yourself.
- Bonus tag: you carry it (kept line 123 px, lost line 121 px)
- Victory feed: `The raiders go home empty.` (138 px)
- Defeat feed: `The raiders hold the river.` (137 px)
- Story: page 6. `c1_b4_before` hands to this battle; `c1_b4_after` plays on the first win.

### 5. The Frozen Pass

Map: Frostmarch Pass (map 2). Mode: Domination. Difficulty: Normal. Allies: `ironclad_normal`. Enemy: `wildmoot_normal`, `wildmoot_normal`. Bonus: `win_within 600 s`, Wildmoot shards.

- Briefing (spec, verbatim): The Wildmoot let no host through Frostmarch that has not bled for it. Take the pass before the snow closes it.
- Reason (spec, verbatim): The pass is the only way north before the snow.
- Bonus (spec, verbatim): Take the pass inside ten minutes.
- Bonus tag: win in time (kept line 109 px, lost line 107 px)
- Victory feed: `The pass is yours.` (94 px)
- Defeat feed: `The snow closes the pass.` (133 px)
- Story: page 7. `c1_b5_before` hands to this battle; `c1_b5_after` plays on the first win and reads the match's losses.

### 6. Kingsfield

Map: Kingsfield Crossroads (map 4, five points). Mode: Domination. Difficulty: Hard. Allies: `ironclad_normal`, `wildmoot_normal`. Enemy: `graveborn_hard` (holds the Hollow King), `mix_gilded_graveborn_normal` (the turncoats returned), `graveborn_normal`. Bonus: `kill_unit gb_hollowking`, Graveborn shards.

- Briefing (spec, verbatim): The Hollow King's host has reached Kingsfield. The Houses you met stand with you now. Break his host and the road is yours.
- Reason (spec, verbatim): Every road meets at Kingsfield, and so does the Hollow King.
- Bonus (spec, verbatim): Strike down the Hollow King with your own banner.
- Bonus tag: strike the King (kept line 134 px, lost line 132 px)
- Victory feed: `The King's host is broken.` (129 px)
- Defeat feed: `The King's host holds.` (109 px)
- Note: The earlier lines said "The Hollow host". With the company named the Hollow, "The Hollow host holds" on a defeat would read as ours, so the feed says "the King's", as the bonus tag does.
- Story: page 8. `c1_king_before` hands to this battle; on the first win `c1_king_after` plays, then the end-of-match report with the epilogue and the Writ card, then `c1_crown` the next time the book opens or the player returns to camp.

## Modes

The how-to lines go on the briefing card under the reason (s8); the player's side picks the set. A feed line shows the same words to both sides. The glyph is the seal for our side and the struck seal for theirs, in the team's ink; "emblem" is the neutral glyph.

### Siege (Ironhold)

The `howTo` (attack) lines are the spec's own, verbatim. The `howToDefend` lines are new.

| # | `howTo` (attack) | `howToDefend` |
|---|---|---|
| 1 | Take the Outer Yard. (20) | Hold the yard, then the gate, then the keep. (44) |
| 2 | Break the gate; siege troops strike it four times harder. (57) | The walls steady your troops while the gate stands. (51) |
| 3 | Seize the keep before the clock runs out. (41) | When the clock runs out, Ironhold is yours. (43) |

| Event | Glyph | Line |
|---|---|---|
| Yard falls | seal / struck seal | `{team} took the Outer Yard` (129 px as Dawn) |
| Gate closing | emblem | `The portcullis falls in {n} s` (135 px; n is `portcullisSeconds`) |
| Gate down | seal / struck seal | `The Iron Gate is breached` (134 px) |

### Survival (Hollowwood)

The human side always defends here, so both sets read as the defender's card. The spec repeats `howTo` in `howToDefend` for modes with no side choice; copy one set over the other to follow it.

| # | `howTo` (attack) | `howToDefend` |
|---|---|---|
| 1 | Hold the shrine as wave after wave of the dead comes. (53) | Keep someone at the shrine. If the dead take it, you lose. (58) |
| 2 | Soul fire marks where each group will rise. (43) | The next wave rises on its timer, cleared or not. (49) |
| 3 | Every cleared wave pays gold. Spend it at your barracks. (56) | Clear a wave early for a longer breather. (41) |

| Event | Glyph | Line |
|---|---|---|
| Group rises, north site | struck seal | `Dead rise at the chapel` (119 px) |
| Group rises, west site | struck seal | `Dead rise at the ford` (109 px) |
| Group rises, east site | struck seal | `Dead rise at the deep wood` (138 px) |
| Group rises, south site | struck seal | `Dead rise at the track` (113 px) |
| Wave cleared | seal | `Wave {n} at rest, +{gold} gold` (129 px at wave 6, +100) |
| Shrine threatened | struck seal | `The shrine is being taken` (127 px) |

The four site names ("the chapel" and so on) go in `modeSites.name`; the line is "Dead rise at " plus the name.

### Banner (Rivergate)

`howTo` is the raider's card and `howToDefend` the guard's.

| # | `howTo` (attack) | `howToDefend` |
|---|---|---|
| 1 | Walk up to the enemy banner to pick it up. (42) | Guard your stand: an enemy's touch picks up the banner. (55) |
| 2 | Carry it to your own stand; the carrier cannot fight. (53) | Cut down the carrier; the banner drops where they fall. (55) |
| 3 | Bring it home before they bring yours. (38) | Touch your fallen banner to send it home at once. (49) |

| Event | Glyph | Line |
|---|---|---|
| Banner taken | seal / struck seal | `{team} has {other}'s banner` (121 px as "Dawn has Dusk's banner") |
| Banner home | emblem | `{owner}'s banner is home` (111 px as "Dusk's banner is home") |
| Banner down | emblem | `{owner}'s banner is down` (111 px as "Dusk's banner is down") |
| Capture | seal / struck seal | `{team} scores, {a} to {b}` (100 px as "Dawn scores, 1 to 0") |

### Campaign bonus (all six battles)

Emblem glyph: `Bonus kept: {tag}` and `Bonus lost: {tag}`, with the tag from each battle above. The widest is "Bonus kept: strike the King" at 134 px. The full sentence stays in `bonusObjectives.text` for the card and the report.

### Domination (battles 1, 5 and 6)

| # | `howTo` and `howToDefend` (the spec repeats one in the other) |
|---|---|
| 1 | Send troops onto a point to take it; hold it to score. (54) |
| 2 | Recruits march out from the held point nearest the fight. (57) |
| 3 | Reach the target score first, or lead when time ends. (53) |

## The Writ (reward card)

Shown on the end-of-match report when battle 6 grants the Writ: the epilogue, then the card. A profile that already holds the chapter 1 Writ (spec question 6) is granted none, so it sees the epilogue alone; the spec does not say, this is my reading.

This is the reward card, and it stays as written. The Hollow's own writ, with its two scraped names and "Danta, of the Hollow" under them, is a different paper: an item, under "Object text" below. Story s6 page 1 says `c1_third_name` shows "the Writ card from the chapter 1 copy"; this copy reads that as the scraped writ's item card, because the reward card belongs to the end of the chapter. The reward card's body names the five Legendaries as the recruit page does; it is the game's interface, not a character's voice, so the familial rule does not reach it.

| Part | Copy |
|---|---|
| Epilogue (`chapters.epilogue`, spec, verbatim) | Kingsfield holds. When the field was searched, the Hollow King's crown was gone. |
| Title | Legendary Writ |
| Body | The road north is behind you. This writ calls one Legendary to your banner: Lord Marshal Aldric, the Hollow King, Brassjaw, Admiral Vael or Old Horn. |
| Note | Choose now, or keep the Writ for later. It is spent once, on one Legendary. |
| Button (spec, verbatim) | Choose your Legendary |

## Camp guide blurb (40 words)

The dead do not stay buried, and the Hollow's road north runs through them. Six battles, five Houses and four ways to fight lie between Rivergate and Kingsfield. Win one to open the next. The last pays a Legendary Writ.

## Fit changes against the spec's section 8 examples

| Spec line | px | Used | px |
|---|---|---|---|
| The portcullis drops in 10 s | 142 | The portcullis falls in 10 s | 135 |
| The dead rise at the chapel | 141 | Dead rise at the chapel | 119 |
| Wave 3 laid to rest, +80 gold | 144 | Wave 3 at rest, +80 gold | 123 |
| The dead are taking the shrine | 157 | The shrine is being taken | 127 |
| Bonus kept: break the gate inside six minutes | 226 | Bonus kept: gate in time | 117 |

Not changed: "Dawn took the Outer Yard", "The Iron Gate is breached", "Dawn has Dusk's banner", "Dusk's banner is home" and "Dawn scores, 1 to 0" fit as written.

## The script

Written from story s5 to s8, with Kort's familial rule and the names Danta Hollow and the Hollow applied. Every line carries a scene or sheet id, a speaker, a voice id, and its speaker's direction from the table below. Voice ids are `c1_<initial><nn>`. The ids in the story design's sample scene (`c1_m31` for "Do not answer him.") are superseded by these.

### Voices

| Speaker | Id | Card label | Portrait | Direction |
|---|---|---|---|---|
| Ser Maud Harrow | m | "The sergeant" until the Archivist names her in `c1_third_name`, then "Maud" | Maud | Level and low, never raises it. (story s8) |
| Wren | w | "The archer" for her first card, then "Wren" | Wren | Light, a little late on every line, as if listening. (story s8) |
| Dunmar Coalbright | d | "The gate-warden" until Maud names him in `c1_letter`, then "Dunmar" | Dunmar | Slow, every word placed. (story s8) |
| Ashka of the Thornwood | a | "The hunter" until Maud names her in `c1_council`, then "Ashka" | Ashka | Quick and warm. (story s8) |
| Pell | p | "The Archivist", always; he does not give his name in chapter 1 | Pell | Precise, old. (story s8) |
| Ser Tomas Harrow | t | "Her brother"; nobody says his name in chapter 1 | Tomas | Certain. (story s8) |
| Edric Vane, the Hollow King | k | "The Hollow King" | the Hollow King helmed; Edric's face once the helm is off | Kind, which is the horror of it. (story s8) |
| Edric alive | e | none (a book caption) | none | Young and certain. (story s8) |
| The Record | r | none (book captions) | none | Not in story s8. Suggested: plain and unhurried, a reader, not a player. |
| A raider of the Tide | x | "A raider" | none (not among the eight) | Not in story s8. Suggested: drowned and failing, sure of the Admiral. |
| Danta Hollow, the commander | none | none | none | Silent. He speaks only through Ashka's chits, which are not voiced. |

### How to read the script

- Each scene: id, kind, place and its thumbnail name, then its cards in order with number, speaker, voice id, line, and a note where the line needs one. A card scene has no camera move. Staging notes are for the builder and never show on screen.
- An "item" card shows an object's text on the vellum card with no portrait and no voice. The story s7.3 command list has no item card yet; the proposal is `card item:<id> "<text>"`, which goes back through the story design before the builder adds it.
- Branches are the story's own (`if match killed:gb_hollowking`, `if companion:<name> standing|wounded|resting`); a card under a branch has its own voice id.
- "Typed" marks a line with a substitution: typed, not voiced, its id reserved in the silent set.
- Every card is under 20 words and every caption under 30; word counts are in brackets where a line runs close.

### Names and nicknames

Where each name lands, one new name a page. Counted as the familial rule counts: the commander's own name, the company, the Houses, roles, object words (the crown, the Horn, the Stone, the standard) and nicknames are not new names.

| Page | New name | Said by, where |
|---|---|---|
| 1 | Maud | the Archivist, `c1_third_name` card 2 |
| 2 | the Hollow King | Maud, `c1_b1_before` card 2 |
| 3 | Wren | Maud, `c1_b2_after` card 2 |
| 4 | Hesper, his wife, the only time she is named | Dunmar, `c1_b3_after` card 3 |
| 5 | Dunmar | Maud, `c1_letter` card 5 |
| 6 | none | the Admiral is a role |
| 7 | Ashka | Maud, `c1_council` card 4 |
| 8 | Edric | Maud, `c1_king_after` card 2 |

Tomas is "my brother" and "their sergeant" in chapter 1. His name would be page 8's second new name, so it waits for chapter 2. Pell stays "the Archivist"; he may give his name himself, once, later.

Nicknames, set once and kept in every line after:

| Who | Calls | Set in | Why |
|---|---|---|---|
| Maud | Danta "Third" | `c1_third_name` card 4 | the third name on the writ |
| Wren | Maud "Grey" | `c1_b2_after` card 3 | "You have gone grey" |
| Dunmar | Danta "Hollow" | `c1_b3_before` card 3 | Dunmar is formal |
| Wren | Dunmar "Tidy" | `c1_b3_before` card 4 | he built the gate and the engines to break it |
| Wren | Danta "Banner" | `c1_b3_after` card 1 | the banner is what she knows him by |
| Maud | Wren "Bird" | `c1_b4_before` card 5 | a wren is a bird |
| Ashka | Danta "Honest" | `c1_b5_after` card 3 | he bled honestly at the pass |
| Wren | Ashka "Quick" | `c1_b5_after` card 4 | the snow takes the slow |

The rest use plain words: Maud says Dunmar and Ashka; Dunmar says "the sergeant" and Wren; Ashka picks up Grey and Tidy from Wren. "Hollow" is the company's name, said with pride by its own and as an accusation by the Crown's Own; the script never marks the difference.

### The echo

Bound by Kort, one exchange, and nobody remarks on it again in chapter 1: Wren, "Hollow. Like him." Maud, "Like the company. Eat." The brief puts it at the first fire after battle 1, but Wren is not with the company until battle 2 is won (story s6 page 3). It is in `c1_b2_after`, the first fire Wren shares, on page 3.

### The company on the left leaf

The leaf shows each companion who has joined, under the label their cards use, with one line of want (the Record's text, not voiced). Wounded portraits grey with the bandage glyph; Wren resting shows the small moon.

| Companion | On the leaf from | Want |
|---|---|---|
| Maud | page 1 | A living hand for the crown, so the dead can rest. |
| Wren | page 3 | To remember how she died. |
| Dunmar | page 4 | To open his own gate, and let the dead out. |
| Ashka | page 7 | To know why the woods' dead are walking. |

Page titles are the story's, except page 4 (story: "Ironhold") and page 8 (story: "Kingsfield"), which are map names; the familial rule keeps those to the match cards and the battle table, so those leaves read "The Gate" and "The Field of Kings". Battle scenes keep their battle names on the thumbnails. Every leaf passage is written to open on a drop capital.

### Page 1: The Writ

Leaf passage (57 words):

> Nineteen years the land has had no king. The dead do not lie down, and the living bury under consecrated ground and keep their doors shut. In the camp the Hollow have gathered what is left of them: a standard, a sergeant, and a writ with two names scraped from it and a third written this spring.

#### `c1_fall` (book, 5 panels): "The Fall"

No names on screen. The climber is a silhouette. Panel 5's caption is the lieutenant's own line, shown without a name.

| Panel | Speaker | Voice | Caption |
|---|---|---|---|
| 1, fog on the rivers | the Record | `c1_r01` | Day one. A fog came up the rivers and did not lift, and the Tide came in it to the field of kings. (23) |
| 2, the writ signed | the Record | `c1_r02` | Day two. The old king sent his only child north, and set his seal to a writ: carry the standard to the field of kings, and hold it. (27) |
| 3, the climber on the highest mound | the Record | `c1_r03` | Day three. The guard could not reach the highest mound through the Tide. Someone small climbed it alone, with something held in both hands. (24) |
| 4, the dead stand up against the sea | the Record | `c1_r04` | That night the kings came up out of their mounds, and every host they ever led, and broke the Tide. At dawn the old king lay dead, crowned. (28) |
| 5, a lieutenant lifts the crown | Edric alive | `c1_e01` | Then I will carry it until there is a hand. |

#### `c1_third_name` (card, camp, the war table): "The Third Name"

Maud at the war table. The Archivist brings the writ to her.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m01` | I am your sergeant, Danta. I was sergeant to the two names before yours. | Label "The sergeant". Her first line: what she is to him. |
| 2 | the Archivist | `c1_p01` | Here is the writ, Maud. The ink on the third name is dry. | Her name lands. Page 1's new name. |
| 3 | item | none | The scraped writ (see Object text). | Shows "Danta, of the Hollow". |
| 4 | Maud | `c1_m02` | Two names above yours, both scraped. The second was my knife. That makes you Third. | Sets "Third". |
| 5 | Maud | `c1_m03` | It says the standard goes north. It does not say why. Nobody who knew is alive. | Story s6's line. |
| 6 | Maud | `c1_m04` | The Hollow carry it until there is a living hand for the crown. Then the dead can rest. | Her want. The company's name, said with pride. (18) |

#### `c1_archive` (card, optional, the archive at the Stone): "The Archive"

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | the Archivist | `c1_p02` | A writ is the crown's seal on paper. It names a captain and gives them a standard. | Pell sheet line 1, first card. |
| 2 | the Archivist | `c1_p03` | The rule is carved on the Stone. I read it from the book. The crown calls them. | He reads the copy, not the Stone. |
| 3 | the Archivist | `c1_p04` | The crown sends them home. A living hand, one crown, one standard, on the field of kings. | The copy's words. |
| 4 | item | none | The roll of the dead, its first page (see Object text). | |
| 5 | the Archivist | `c1_p05` | I write them as they come. The road has not let me stop. | |

### Page 2: The River Road

Leaf passage (54 words):

> Three crossings carry the road west over the river, and the Order holds all three. Not the Order that keeps the oath: the other half, who swore it to the crown and follow the crown wherever it walks. They wear the same colours. Some of them were taught by the same sergeant.

#### `c1_b1_before` (stage, the crossings, day): "Order on the Crossings"

Maud and the commander on the south bank; the turncoats' colours on the three crossings. Hands to `battle_1` when the last card closes.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m05` | Order on the crossings. Our own colours. The same oath as mine, read the other way. | |
| 2 | Maud | `c1_m06` | They serve the Hollow King now. They swear a crown on a dead head is living enough. | Page 2's new name. |
| 3 | Maud | `c1_m07` | Take the bridges one at a time, Third. Your banner follows you. Make it worth following. | Echoes the briefing; the lessons follow. |

#### `battle_1` (battle): "The Training Field"

The briefing card, spec strings verbatim (see "The six battles"). The lessons and the free roll stay.

#### `c1_b1_after` (stage, the north crossing, a fire at dusk): "The North Crossing"

The fire: Maud counting, the company eating. A dropped writ-copy lies by the fire; Maud picks it up.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m08` | {count} standing. I will count them after every fight. Somebody taught me to. | Typed. "Somebody" is Edric. |
| 2 | Maud | `c1_m09` | Those were Order. Remember that when you count them. | Story s6's line. |
| 3 | item | none | The turncoat's writ-copy (see Object text). | |
| 4 | Maud | `c1_m10` | Leave that with me. It is nothing you need tonight. Eat something, Third. | She keeps it and does not explain. |

### Page 3: The Moon Shrine

Leaf passage (50 words):

> The wood here is old ground, and its graves are older. A week before the march came, something walked through, and the graves opened behind it. The moon shrine stands on consecrated stone. The dead cannot come up through it. They can come at it, all night, from four sides.

#### `c1_b2_before` (stage, the shrine in the wood, night): "Nowhere Else to Sleep"

Open graves along the track; the shrine lit. Hands to `battle_2`.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m11` | He came through here a week ago. The graves opened behind him, the way they do. | "He" is the Hollow King. |
| 2 | Maud | `c1_m12` | The shrine is consecrated. They cannot come up through it. They will come at it. | |
| 3 | Maud | `c1_m13` | Keep the fires low and the banner close, Third. There is nowhere else to sleep. | |

#### `battle_2` (battle): "The Moon Shrine"

The briefing card, spec strings verbatim.

#### `c1_b2_after` (stage, the shrine at dawn, then the fire): "The Last of the Dead"

Whatever the waves left standing lies down as the scene opens; a timer win leaves no one standing in the story. The last of the dead lies still in front of the commander, then stands. Cards 4 to 6 are at the fire by the shrine. Wren joins and takes her place at the graveyard shrine in camp.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Wren | `c1_w01` | You kept the banner. Good. I could not find anything else of ours. | Label "The archer". Story s6's line; "ours" says what she is to them. |
| 2 | Maud | `c1_m14` | Wren. | Maud's one word. Page 3's new name. |
| 3 | Wren | `c1_w02` | You have gone grey, Grey. I have gone further. | Sets "Grey". |
| 4 | Maud | `c1_m15` | This is Danta Hollow. His name is third on the paper now. | Formal, so "Hollow". |
| 5 | Wren | `c1_w03` | Hollow. Like him. | Bound line. The only remark on the echo. |
| 6 | Maud | `c1_m16` | Like the company. Eat. | Bound line. |

### Page 4: The Gate

Leaf passage (56 words):

> The hold's great gate was sealed from outside nineteen years ago, by the man whose work it was to seal it. The smiths slept under the stone, and the call came through the stone. Two years ago the dead took the whole hold. The Ironclad have brought engines to break the gate. One of them built it.

#### `c1_b3_before` (stage, the west field before the gate): "The Engines"

`structure ironhold/gate_intact` stands. The Ironclad engines in a row; their gate-warden stands with his back to the gate. Hands to `battle_3`.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m17` | The Ironclad brought the engines. Their gate-warden has not looked at the gate once. | |
| 2 | Dunmar | `c1_d01` | I am the gate-warden of this hold. The dead took it while its smiths slept. | Label "The gate-warden". His first line: what he is. Echoes the briefing. |
| 3 | Dunmar | `c1_d02` | Six minutes. That is how long my gate holds against my own ballistas, Hollow. I measured it. | The bonus's draft value. Sets his "Hollow". |
| 4 | Wren | `c1_w04` | He built the gate, and the engines to break it. Tidy. | Sets "Tidy". |
| 5 | Dunmar | `c1_d03` | Take the yard. Break the gate. Seize the keep. I will be at the engines, facing them. | Facing the engines, not the gate. |

#### `battle_3` (battle): "Ironhold Retaken"

The briefing card, spec strings verbatim. The Siege how-to lines are the spec's own.

#### `c1_b3_after` (stage, the keep: a fire in the yard, then the smiths' graves): "The Smiths' Graves"

Card 1 at the fire in the keep yard. Then the graves, open; one stone with a name he reads and stops at. The gate stands broken after this battle, as the map's rule says. Dunmar joins; the Ironclad tent in camp gains his lines.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Wren | `c1_w05` | We are inside, Tidy. Banner broke your gate for you. You can look now. | Sets "Banner". He has never been in. |
| 2 | Dunmar | `c1_d04` | The smiths' graves. I sealed the hold on them. The gate-warden seals from outside. | |
| 3 | Dunmar | `c1_d05` | Hesper. | His wife. The only time her name is said. Page 4's new name. |
| 4 | Wren | `c1_w06` | She is not in there. She got up, like me. That is not the same as gone. | Chapter 4's hook. |
| 5 | Dunmar | `c1_d06` | Leave them open, Hollow. Until there is a crown to send them home. | The only time he asks anything. |
| 6 | Dunmar | `c1_d07` | I will not have them put back twice. I will march with you. | He joins. |

### Page 5: The Letter

Leaf passage (50 words):

> A rider came in from the west on a lamed horse, with one letter for whoever holds the standard of the Hollow. It is in the Marshal's hand. It says where the dead are going and where a trail was found, and it says one thing twice: hold the standard.

#### `c1_letter` (card, camp, the war table): "The Letter"

Maud reads the Marshal's letter aloud; Wren and Dunmar at the table. Wren says nothing in this scene. The first time the commander hears the word Horn.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m18` | From the Marshal, to whoever holds our standard. The Hollow King's host makes for the field of kings. | Reading. (18) |
| 2 | Maud | `c1_m19` | The Tide is on the rivers again. He has found the trail of the child they sent north. | Reading. (18) |
| 3 | Maud | `c1_m20` | Hold the standard. He writes that twice. Do not let it reach the field in the wrong hand. | (18) |
| 4 | Dunmar | `c1_d08` | The wrong hand. He does not say whose. | Label "The gate-warden". |
| 5 | Maud | `c1_m21` | He would not know, Dunmar. He gives orders. He ordered the Horn blown, on the third day. | His name lands. Page 5's new name. The first "Horn". |
| 6 | Maud | `c1_m22` | From the highest mound. I was there. I never knew who climbed. They did not come down. | Wren is at the table. |

#### `c1_rule` (card, the archive): "The Rule"

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | the Archivist | `c1_p06` | So the Marshal has written. Then hear the rule again. It means something now. | |
| 2 | the Archivist | `c1_p03` | The rule is carved on the Stone. I read it from the book. The crown calls them. | Same card as `c1_archive` 2. |
| 3 | the Archivist | `c1_p04` | The crown sends them home. A living hand, one crown, one standard, on the field of kings. | Same card as `c1_archive` 3. |

Asked again, he says Pell sheet line 5 (`c1_p11`), that he has never checked it against the Stone.

#### `c1_shrine_2` (card, the graveyard shrine): "Climbing"

Wren's first memory. This scene is row 1 of her six shrine lines; the shrine sheet carries the same card.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Wren | `c1_w07` | I remembered something, Banner. I was good at climbing. Walls, trees, anything with a top. | Shrine row 1. |
| 2 | Wren | `c1_w08` | I do not know why that matters. It feels as if it matters. | |
| 3 | Wren | `c1_w09` | I would like to remember how I died. It seems a thing a person ought to know. | Her want. |

### Page 6: Raiders on the River

Leaf passage (48 words):

> The Tide came up the rivers once before, in a fog that did not lift. Now the fog is on the river again, and raiders in it. They pass the mill and the granary and take nothing. They have come for one thing, and the Hollow carry it.

#### `c1_b4_before` (stage, the river in fog): "Fog on the River"

Heavy fog on the water, the mill wheel turning in it, grain barges untouched at the bank. Hands to `battle_4`.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m23` | Fog on the river. It came like this the first time. | The Fall. |
| 2 | Dunmar | `c1_d09` | They left the grain barges whole. Raiders do not leave grain. | |
| 3 | Maud | `c1_m24` | They did not come for gold. | Story s6's line. |
| 4 | Wren | `c1_w10` | They want our standard. That is the one thing I would get up for. | |
| 5 | Maud | `c1_m25` | Then guard it, Bird. Third, carry theirs home. And keep your own count. The fog will not. | Sets "Bird". (17) |

#### `battle_4` (battle): "Raiders on the River"

The briefing card, spec strings verbatim.

#### `c1_b4_after` (stage, the mill, a fire at the wheel): "The Shroud"

Card 1 at the fire. The raiders' banner, taken: a drowned man's shroud with the crown's seal sewn on it. A dying raider lies against the wheel. The Admiral is named by his rank only.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Wren | `c1_w11` | Tidy, you are dripping. Sit by the fire before you rust. | Dunmar came up out of the ford. |
| 2 | item | none | The drowned shroud (see Object text). | |
| 3 | Dunmar | `c1_d10` | The seal is sewn true. Whoever sewed it had seen the real one, close. | |
| 4 | a raider | `c1_x01` | The Admiral will have the crown under the water. Where no hand can ever take it. | Chapter 2's hook. Said once. (15) |
| 5 | Wren | `c1_w12` | The water will have him back by morning. That is what they want for everyone. | |
| 6 | Maud | `c1_m26` | {count} standing. Keep the shroud. Someone will want to know where it came from. | Typed. |

### Page 7: The Frozen Pass

Leaf passage (50 words):

> The pass takes a toll from every host that crosses north, and the Wildmoot keep the count. The toll is not paid in coin. The snow is coming down the pass, and behind the march the dead are coming up the road. There is one way north, and it bleeds.

#### `c1_b5_before` (stage, the lodge at the foot of the pass): "The Toll"

Snow falling; the treeline above the lodge full of watchers who do not come down. Hands to `battle_5`.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m27` | Every host that crosses pays the pass in blood. The hunters decide how much. | |
| 2 | Dunmar | `c1_d11` | They will not be talked past. My clan tried, the year the holds shut. | |
| 3 | Wren | `c1_w13` | Snow in front, the dead behind. I have been in worse places, Banner. I think. I forget. | (16) |
| 4 | Maud | `c1_m28` | Take the pass before the snow closes it. Pay what it asks. No more. | |

#### `battle_5` (battle): "The Frozen Pass"

The briefing card, spec strings verbatim.

#### `c1_b5_after` (stage, the pass in snow, a fire): "One Hunter"

Card 1 at the fire: the toll is counted in the troops the commander actually lost (the scene reads the match's losses). After card 1 a hunter steps out of the trees. Before card 3 she looks at the standard a moment too long. She joins.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m29` | {losses} lost. That is the toll, then. I counted every one. | Typed. |
| 2 | Ashka | `c1_a01` | So did the woods. Paid in full, and honestly. The law gives you one hunter north. | Label "The hunter". |
| 3 | Ashka | `c1_a02` | That is me, Honest. Your hunter, for the road. The snow does not take sides. It takes the slow. | What she is to him; sets "Honest"; story s6's line. (19) |
| 4 | Wren | `c1_w14` | She likes you already, Banner. Quick, this one. | Sets "Quick". |
| 5 | Ashka | `c1_a03` | I like anyone who bleeds honestly. I also want to know why the woods' dead are walking. | Her want. (17) |
| 6 | Ashka | `c1_a04` | My clan found me in this snow, they say. I was small. Come. It is getting dark. | Plant 1 of 3. |

Ashka's first choice card (see "Ashka's choice cards") waits at her tent after this scene.

#### `c1_council` (card, camp, the war table, all four): "The Council"

Each says one thing. The commander gives no speech. The scene ends on the standard: the last card closes and the view rests on it, with no card.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | Maud | `c1_m30` | The Crown's Own will be on that field. Their sergeant is my brother. | She does not say his name. |
| 2 | Wren | `c1_w15` | I remember a horn. Somebody was blowing it, far off. Or close. It was very loud. | Her first "horn". (16) |
| 3 | Dunmar | `c1_d12` | The engines can reach the centre bridge from the raised outposts. I will take the outposts. | (15) |
| 4 | Maud | `c1_m31` | Ashka. Tell them what you told me. | Her name lands. Page 7's new name. |
| 5 | Ashka | `c1_a05` | He was standing at the mounds when I left the trees. His host is larger than ours. | (17) |

Ashka's second choice card waits at her tent after this scene.

### Page 8: The Field of Kings

Leaf passage (55 words):

> Every road meets at the field of kings, where the kings lie under their mounds and the first king's mound stands highest. The Hollow King has come home to hold court, and every dead thing that ever answered the Horn walks with him. Danta has carried the standard the whole road. Here the road ends.

Ashka's third choice card waits at her tent while this page is current and Kingsfield is not yet won.

#### `c1_king_before` (stage, the mounds, night): "The Mounds"

The Hollow King, helmed and crowned, on the mound by the centre bridge; her brother a step behind him in the Crown's Own colours; Maud beside the commander. The commander does not answer. The battle starts when the last card closes.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | the Hollow King | `c1_k01` | You carry a writ under my seal. I would know it anywhere. | He asks for the paper. |
| 2 | the Hollow King | `c1_k02` | I signed that on the second day. I remember the hand. | He reads his own seal. |
| 3 | the Hollow King | `c1_k03` | Danta. Written under a name someone has scraped. It does not matter. The seal is whole. | Voiced: the name is fixed, so no substitution. The scraped name is his own. (16) |
| 4 | Tomas | `c1_t01` | Kneel, Hollow. And you, Maud. You swore to the last living crown, and there he stands. | Label "Her brother". "Hollow" as an accusation. (16) |
| 5 | Maud | `c1_m32` | Do not answer him. | Story s6's line. |
| 6 | the Hollow King | `c1_k04` | You have carried the standard to the field of kings. Well done. Now give it to me. | Story s6's line. (17) |

#### `battle_6` (battle): "Kingsfield"

The briefing card, spec strings verbatim. Her brother's banner is on the field, script-protected. The Hollow King has no boss rules; the bots play him as any Legendary.

#### `c1_king_after` (stage, the mounds, dawn coming, the company's fire at the foot of the mound): "Dawn Coming"

Staging, before card 1: `if match killed:gb_hollowking` he is already down on the mound; `else` he is on his knees on the mound as the dead stop answering him, and he falls. `if companion:maud standing` she kneels by him; `wounded` she is carried to him. She lifts the helm (`pose helm_off`): Edric's face. He does not know her. After card 5, her brother is found fallen where the script put him and taken alive by Ashka's kin. Dunmar's engines are silent. Ashka is looking at the mounds. Plant 2 of 3: the camera passes a small stone at the foot of one mound, carved for a child (see Object text), and nobody reads it aloud.

| # | Speaker | Voice | Line | Note |
|---|---|---|---|---|
| 1 | the Hollow King | `c1_k05` | Danta. That is the name on the paper. You carried it well. | Edric's portrait. He says it kindly, and stops. |
| 2 | Maud | `c1_m33` | Edric. I scraped your name off the writ with my own knife. I thought you were under the mounds. | Page 8's new name. The longest she ever says begins. (19) |
| 3 | Maud | `c1_m34` | You said you would carry it until there was a hand. Then you put it on. | His line from `c1_fall`. (16) |
| 4 | Maud | `c1_m35` | Twelve years, and you do not know my face. That is all right. I know yours. | (16) |
| 5 | Wren | `c1_w16` | I saw him lift it. The first morning. I was on the ground and I saw him lift it. | Story s6's line. (19) |
| 6a | Maud | `c1_m36` | Your banner struck him, Third. Bind my brother, and do not kill him. I have counted enough. | `if match killed:gb_hollowking`. (17) |
| 6b | Maud | `c1_m37` | Bind my brother, and do not kill him. I have counted enough. | `else`. |

Then the end-of-match report, as Phase 4 says: the epilogue (spec, verbatim) "Kingsfield holds. When the field was searched, the Hollow King's crown was gone.", the Writ card, and the "Choose your Legendary" button (spec, verbatim). The book's last scene waits behind it.

#### `c1_crown` (book, 2 panels, unskippable the first time): "The Highest Mound"

Plays when the commander next opens the book or returns to camp, whichever is first. Wren holds the crown in both hands, the way she once held a horn. Then the mounds open behind her, all of them, and the page turns black. No words on the black: "Chapter" stays the Founders plaque's word.

| Panel | Speaker | Voice | Caption |
|---|---|---|---|
| 1, dawn, the first king's mound | the Record | `c1_r05` | Dawn on the field of kings. On the first king's mound, the highest, someone small stood with the crown in both hands. (22) |
| 2, the mounds open behind her | Wren | `c1_w17` | I remember now. |

## Reaction lines

Sheet `storyLines`, played in camp after every campaign battle, replays included (story s7.5). How the camp picks them:

- Facts, in priority order: 1 a companion fell; 2 the commander's group routed; 3 the bonus missed; 4 losses above a third of the banner; 5 the bonus earned. One line per fact, at most three a visit, highest priority first.
- A companion speaks only about a battle they were in. A companion who fell says their own fell line on the next visit. The line about that fall is said by the companion the table names, if they were in the battle and did not fall; otherwise it is skipped.
- Every other fact goes to the companion who was in the battle, did not fall, and has spoken least this visit; ties go to the battle's order, given under each heading. With one companion present, she says them all.
- The bonus missed plays only on a win without the bonus; a loss says it already.
- Wren's fell line plays after one fall, when the script has stood her up. After a second fall she is resting, and a resting line plays in its place.
- Losses: troops lost in the match above a third of the banner's slots. Maud's losses lines carry `{losses}` and are typed.
- Triggers: `b<n>_fell`, `b<n>_fell:<name>` for the line about another's fall, `b<n>_rout`, `b<n>_bonus_lost`, `b<n>_losses`, `b<n>_bonus_kept`. Priority 1 to 5 as above. A companion in a replay of a battle fought before they joined uses the replay set.

### Battle 1, The Training Field: Maud. Order: Maud.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Maud | `b1_fell` | 1 | `c1_m38` | A squire put me down. Our Order trains them well. I should know. I trained some. |
| Maud | `b1_rout` | 2 | `c1_m39` | Your line ran at the crossing, Third. They will run again unless they see you stay. |
| Maud | `b1_bonus_lost` | 3 | `c1_m40` | You never held all three at once. A crossing you do not hold is a door left open. |
| Maud | `b1_losses` | 4 | `c1_m41` | {losses} lost on the river road, to Order blades. Count them. Then count them again. (typed) |
| Maud | `b1_bonus_kept` | 5 | `c1_m42` | All three crossings at once. The banner followed you. That is the first lesson, and the hardest. |

### Battle 2, The Moon Shrine: Maud. Order: Maud.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Maud | `b2_fell` | 1 | `c1_m43` | The dead do not tire. I do. Next time keep someone at the shrine beside me. |
| Maud | `b2_rout` | 2 | `c1_m44` | They broke at the shrine steps. Dead men out of the dark will do that, once. |
| Maud | `b2_bonus_lost` | 3 | `c1_m45` | Too many of ours lie at that shrine. Some of them will not lie there long. |
| Maud | `b2_losses` | 4 | `c1_m46` | {losses} of ours at the shrine. Sleep near the banner tonight, not near the graves. (typed) |
| Maud | `b2_bonus_kept` | 5 | `c1_m47` | Few lost, and the shrine held. You kept them close, Third. Keep doing that. |

### Battle 3, Ironhold Retaken: Maud, Wren. Order: Wren, Maud.

Dunmar is an ally here, not yet a companion, so the lines call him "the warden" or Tidy.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Maud | `b3_fell` | 1 | `c1_m48` | The gate and I both went down. Only the gate is staying down. |
| Maud | `b3_fell:wren` | 1 | `c1_m49` | Wren went down at the gate and got up again. Do not count on that. |
| Maud | `b3_rout` | 2 | `c1_m50` | They ran in the yard. Walls make men think of running. Teach them otherwise, Third. |
| Maud | `b3_bonus_lost` | 3 | `c1_m51` | The gate held longer than it should have. The warden said six minutes. He meant it. |
| Maud | `b3_losses` | 4 | `c1_m52` | {losses} lost under the hold's walls. The dead fight harder behind stone. Remember the price. (typed) |
| Maud | `b3_bonus_kept` | 5 | `c1_m53` | Gate down inside six minutes. The warden nodded. I have not seen him do that before. |
| Wren | `b3_fell` | 1 | `c1_w18` | I went down in the yard and got up again. It is becoming a habit. |
| Wren | `b3_fell:maud` | 1 | `c1_w19` | Grey went down in the yard. She was cross about it. That is a good sign. |
| Wren | `b3_rout` | 2 | `c1_w20` | Our lot ran in the yard. I would have run too, if I still had the knack. |
| Wren | `b3_bonus_lost` | 3 | `c1_w21` | The gate took too long. Tidy counted every minute under his breath. I listened. |
| Wren | `b3_losses` | 4 | `c1_w22` | We lost a great many in that yard, Banner. I will know some of them at the shrine. |
| Wren | `b3_bonus_kept` | 5 | `c1_w23` | The gate came down quick. Tidy builds well. Even the breaking. |

### Battle 4, Raiders on the River: Maud, Wren, Dunmar. Order: Maud, Dunmar, Wren.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Maud | `b4_fell` | 1 | `c1_m54` | They went for the standard and I was standing in the way. I meant to be. |
| Maud | `b4_fell:wren` | 1 | `c1_m55` | Bird went into the river and walked out again. The raiders saw. They did not like it. |
| Maud | `b4_rout` | 2 | `c1_m56` | Your line broke in the fog, Third. A line that runs is only a crowd with weapons. |
| Maud | `b4_bonus_lost` | 3 | `c1_m57` | Someone else carried their banner home. Next time carry it yourself. They should see you do it. |
| Maud | `b4_losses` | 4 | `c1_m58` | {losses} lost at the fords and the mill. The Tide counts in the water. I count here. (typed) |
| Maud | `b4_bonus_kept` | 5 | `c1_m59` | You carried it home yourself. They saw. That is worth more than the banner. |
| Wren | `b4_fell` | 1 | `c1_w24` | A raider pulled me into the river. I came out. The river was surprised. |
| Wren | `b4_fell:dunmar` | 1 | `c1_w25` | Tidy went down at the ford. He fell very carefully. I did not know you could. |
| Wren | `b4_rout` | 2 | `c1_w26` | Our lot ran from the fog. It is only weather. I have stood in worse. |
| Wren | `b4_bonus_lost` | 3 | `c1_w27` | Somebody else carried their banner home. Next time you carry it, Banner. I will watch your back. |
| Wren | `b4_losses` | 4 | `c1_w28` | The river took a lot of ours. It will give them back. That is what worries me. |
| Wren | `b4_bonus_kept` | 5 | `c1_w29` | You carried their banner home yourself, with a raider on you the whole way. I watched. |
| Dunmar | `b4_fell` | 1 | `c1_d13` | Trident in the leg. I will be slow. Slower than before. That is quite slow. |
| Dunmar | `b4_fell:maud` | 1 | `c1_d14` | The sergeant fell by the stand. She did not let go of it until she was down. |
| Dunmar | `b4_rout` | 2 | `c1_d15` | They ran. A line that runs cannot be mended in the field, Hollow. Only before it. |
| Dunmar | `b4_bonus_lost` | 3 | `c1_d16` | Their banner came home, but not in your hand. A thing done by halves is done twice. |
| Dunmar | `b4_losses` | 4 | `c1_d17` | Too many lost. I count them like bolts. You do not get bolts back. |
| Dunmar | `b4_bonus_kept` | 5 | `c1_d18` | You carried it yourself. Good. A captain should know the weight. |

### Battle 5, The Frozen Pass: Maud, Wren, Dunmar. Order: Dunmar, Wren, Maud.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Maud | `b5_fell` | 1 | `c1_m60` | The snow put me down before the hunters did. I am getting old for passes. |
| Maud | `b5_fell:wren` | 1 | `c1_m61` | Bird fell in the drifts and got up white. The hunters stopped shooting to look. |
| Maud | `b5_fell:dunmar` | 1 | `c1_m62` | Dunmar went down on the slope. He got up on one knee and kept counting. |
| Maud | `b5_rout` | 2 | `c1_m63` | They ran in the snow. Running in snow is how the pass takes its toll. |
| Maud | `b5_bonus_lost` | 3 | `c1_m64` | Too slow through the pass, Third. The snow does not wait for anyone. Nor will the dead. |
| Maud | `b5_losses` | 4 | `c1_m65` | {losses} paid at the pass. The hunters counted them. So did I. (typed) |
| Maud | `b5_bonus_kept` | 5 | `c1_m66` | Through the pass before the snow had made up its mind. Well done. |
| Wren | `b5_fell` | 1 | `c1_w30` | The cold put me down. I did not feel it. I think I have felt it before. |
| Wren | `b5_rout` | 2 | `c1_w31` | Our lot ran back down the pass. Downhill is easier, I am told. |
| Wren | `b5_bonus_lost` | 3 | `c1_w32` | Too slow, Banner. The snow nearly had us. It would have kept us, too. |
| Wren | `b5_losses` | 4 | `c1_w33` | The pass took a lot of ours. The hunters will not bury them. I asked. |
| Wren | `b5_bonus_kept` | 5 | `c1_w34` | Fast through the pass. The hunters were impressed, Banner. I could tell. They stopped shooting. |
| Dunmar | `b5_fell` | 1 | `c1_d19` | An arrow found the gap in my plate. I made that plate. I will close the gap. |
| Dunmar | `b5_fell:maud` | 1 | `c1_d20` | The sergeant fell in the snow and would not be carried. She was carried. |
| Dunmar | `b5_rout` | 2 | `c1_d21` | They broke on the slope. Snow underfoot, arrows from above. I do not blame them. I mark it. |
| Dunmar | `b5_bonus_lost` | 3 | `c1_d22` | We were slow through the pass. The snow kept the difference. |
| Dunmar | `b5_losses` | 4 | `c1_d23` | The toll was heavy. The hunters keep an honest count. So do I. |
| Dunmar | `b5_bonus_kept` | 5 | `c1_d24` | Through the pass in good time, Hollow. The engines kept up. I did not think they would. |

### Battle 6, Kingsfield: Maud, Wren, Dunmar, Ashka. Order: Ashka, Maud, Dunmar, Wren.

A lost battle 6 comes before the helm is lifted, so no line here names him or says who he was.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Maud | `b6_fell` | 1 | `c1_m67` | I went down by the centre bridge. On this field again. It is a hard field. |
| Maud | `b6_fell:ashka` | 1 | `c1_m68` | Ashka fell by the fords. She was up and arguing before we reached her. |
| Maud | `b6_rout` | 2 | `c1_m69` | They ran on the field of kings. I have seen that before. I hoped not to again. |
| Maud | `b6_bonus_lost` | 3 | `c1_m70` | It was not your banner that put him down. Better that than mine. |
| Maud | `b6_losses` | 4 | `c1_m71` | {losses} lost on the field of kings. They will not stay under it. Nothing does. (typed) |
| Maud | `b6_bonus_kept` | 5 | `c1_m72` | Your banner struck the blow, Third. I will not thank you for it. I do not blame you. |
| Wren | `b6_fell` | 1 | `c1_w35` | I fell on the mounds. The ground was warm. It should not be warm. |
| Wren | `b6_fell:dunmar` | 1 | `c1_w36` | Tidy fell at the outposts. He lay there giving orders to the engines. They obeyed. |
| Wren | `b6_rout` | 2 | `c1_w37` | Our lot ran from the mounds. Quick's kin held, and they are not even ours. |
| Wren | `b6_bonus_lost` | 3 | `c1_w38` | He went down, but not to your banner. I am not sure which is worse. |
| Wren | `b6_losses` | 4 | `c1_w39` | So many of ours on the field of kings. I will know their names by morning. |
| Wren | `b6_bonus_kept` | 5 | `c1_w40` | Your banner put him down, Banner. I watched. I have watched that mound a long time. |
| Dunmar | `b6_fell` | 1 | `c1_d25` | Down by the bridge. My engines kept firing without me. I taught them that. |
| Dunmar | `b6_fell:wren` | 1 | `c1_d26` | Wren went down by the mounds and lay there longer than she needed to. |
| Dunmar | `b6_rout` | 2 | `c1_d27` | They broke at the fords. Even good stone breaks, if it is struck enough. |
| Dunmar | `b6_bonus_lost` | 3 | `c1_d28` | Not your banner's blow, Hollow. A thing like that should be done by the one who carries the writ. |
| Dunmar | `b6_losses` | 4 | `c1_d29` | Heavy losses. The field of kings takes its toll the way the pass does. Without asking. |
| Dunmar | `b6_bonus_kept` | 5 | `c1_d30` | Your banner's blow, and clean. I have seen smiths strike worse on a good day. |
| Ashka | `b6_fell` | 1 | `c1_a15` | They had me on the slope by the fords. I was slow. Do not tell the snow. |
| Ashka | `b6_fell:maud` | 1 | `c1_a16` | Grey fell at the bridge. She would not let me carry her. I carried her. |
| Ashka | `b6_rout` | 2 | `c1_a17` | Your line ran, Honest. My kin did not, so you owe them a drink and an apology. |
| Ashka | `b6_bonus_lost` | 3 | `c1_a18` | He fell to some other hand. The woods will say that matters. I am not sure it does. |
| Ashka | `b6_losses` | 4 | `c1_a19` | You paid a heavy toll here. Heavier than the pass. The woods would call that honest. |
| Ashka | `b6_bonus_kept` | 5 | `c1_a20` | You struck him down yourself, Honest. My kin saw. They will sing it badly for a year. |

### Replay set: a companion in a battle fought before they joined

Wren in replays of battles 1 and 2; Dunmar in replays of 1 to 3; Ashka in replays of 1 to 5. No line about another's fall is written for these; it is skipped.

| Speaker | Trigger | Pri | Voice | Line |
|---|---|---|---|---|
| Wren | `replay_fell` | 1 | `c1_w41` | I fell on a road I did not walk with you the first time. It is still a road. |
| Wren | `replay_rout` | 2 | `c1_w42` | They ran on old ground. I was not with you the first time. I wish I had been. |
| Wren | `replay_bonus_lost` | 3 | `c1_w43` | You missed something on this field, Banner. I would not know what. I was not here then. |
| Wren | `replay_losses` | 4 | `c1_w44` | A lot of ours, for an old fight. The shrine will be busy. |
| Wren | `replay_bonus_kept` | 5 | `c1_w45` | Done better than the first time, I expect. Grey will not say. |
| Dunmar | `replay_fell` | 1 | `c1_d31` | I fell on ground you had already won. Mark it as a lesson. I have. |
| Dunmar | `replay_rout` | 2 | `c1_d32` | They ran on a field they had already taken. Fear keeps no ledger. |
| Dunmar | `replay_bonus_lost` | 3 | `c1_d33` | Not done to the mark, Hollow. A field won twice should be won better. |
| Dunmar | `replay_losses` | 4 | `c1_d34` | Heavy losses, for a field already won. I would count them twice. |
| Dunmar | `replay_bonus_kept` | 5 | `c1_d35` | Done to the mark. I like a thing done properly, even the second time. |
| Ashka | `replay_fell` | 1 | `c1_a21` | Down on a field I never walked. The woods are telling me to pay attention. |
| Ashka | `replay_rout` | 2 | `c1_a22` | They ran. Then they are still learning. So am I. Again, tomorrow. |
| Ashka | `replay_bonus_lost` | 3 | `c1_a23` | Something was missed, Honest. I do not know these fields. You do. Show me. |
| Ashka | `replay_losses` | 4 | `c1_a24` | A heavy toll for an old road. You paid it twice. The woods would not ask that. |
| Ashka | `replay_bonus_kept` | 5 | `c1_a25` | Well fought. I was not here the first time. I am glad I saw this one. |

## Wounded and resting lines

Sheet `storyLines`. A wounded companion's slot is empty and their portrait greyed until the chapter's next unwon battle is finished (story s7.5); at their stretcher in camp they say these in turn. Wren is never wounded; after a second fall in one battle she is resting.

| Speaker | Trigger | Voice | Line |
|---|---|---|---|
| Maud | `wounded` | `c1_m73` | Count them for me, Third. I cannot see the lines from here. Count them out loud. |
| Maud | `wounded` | `c1_m74` | I have had worse, on the field of kings. Do not stand there. Go and win. |
| Dunmar | `wounded` | `c1_d36` | Leg is broken. The engines are not. Use the engines, Hollow. |
| Dunmar | `wounded` | `c1_d37` | I waited outside a gate for nineteen years. I can wait outside a fight. |
| Ashka | `wounded` | `c1_a26` | Do not look like that, Honest. The woods have put me down harder than this. |
| Ashka | `wounded` | `c1_a27` | Go on. I will be quick again by the next one. Quicker than you. |
| Wren | `resting` | `c1_w46` | I am resting, Banner. Grey says it is good for me. I have not rested in nineteen years. |
| Wren | `resting` | `c1_w47` | Two falls in one fight. Even I have to lie down sometimes. Wake me for the next one. |

## The shrine

### Wren's memory

Sheet `storyLines`, trigger `shrine_<n>`, in story s7.6's order. Row 1 is the `c1_shrine_2` card itself. After that scene has been read, each shrine visit after a battle (won, lost or replayed) advances one row, whatever happened in it. The seventh is `c1_crown`.

| Row | Voice | Line | Story s7.6 |
|---|---|---|---|
| 1 | `c1_w07` | I remembered something, Banner. I was good at climbing. Walls, trees, anything with a top. | she was good at climbing |
| 2 | `c1_w48` | The fog had a sound. Not the sea. Lower than the sea. I can nearly hear it now. | the fog had a sound |
| 3 | `c1_w49` | Someone gave me something to carry. Put it in my hands. Said do not drop it. | someone gave her something to carry |
| 4 | `c1_w50` | It was heavy, the thing I carried. It was not a bow. I would remember a bow. | it was heavy and it was not a bow |
| 5 | `c1_w51` | I was higher than everyone. I could see the whole field, and the sea in the fog. | she was higher than everyone |
| 6 | `c1_w52` | I was cold before I was anything else. Before I was this. Cold first, then this. | she was cold before she was anything else |
| 7 | `c1_w17` | I remember now. | `c1_crown`, the last page |

### Recruit lines

Sheet `storyLines`, trigger `recruit`. When the commander recruits a Graveborn troop at the shrine, Wren gives it the next byname from the grave roll for its type. Once a type's rows have all been heard, she says these in turn instead.

| Voice | Line |
|---|---|
| `c1_w53` | I do not know this one, Banner. They know the standard, though. That is enough. |
| `c1_w54` | Another one for the road. Be kind to them. They did not ask for this. |
| `c1_w55` | Up you get. That is Banner. Follow the standard. It is easy. I promise. |
| `c1_w56` | They will not remember much. Neither do I. We manage. |

### The grave roll

Sheet `graveRoll`, Wren's voice at the shrine. The `name` column holds a byname, not a name: the familial rule keeps proper names to the company. The Archivist's roll in the archive reads the same rows as text. "Risen" and "Bone Archer" are the Phase 4 spec's names; "reaper" and "stalker" are plain stand-ins for the Houses spec's warden and caster kinds, to be swapped for its ids at plan time.

| # | Troop type | Byname | Voice | Line |
|---|---|---|---|---|
| 1 | Risen | the ferryman | `c1_w57` | He kept the ferry on the river. He still waits by the water. |
| 2 | Risen | the salt seller | `c1_w58` | She sold salt on the river road. She counts badly now. So do I. |
| 3 | Risen | the ploughman | `c1_w59` | He walked behind a plough. He still walks in straight lines. |
| 4 | Risen | the drowned boy | `c1_w60` | He drowned at the north ford, the first year. He has dried out since. |
| 5 | Bone Archer | our old bowman | `c1_w61` | Our best shot, before me. Better than me. Do not tell him. |
| 6 | Bone Archer | the stringer | `c1_w62` | She taught me to string a bow. I forgot how. She did not. |
| 7 | Bone Archer | the crow boy | `c1_w63` | He shot crows off the barley. He still hates crows. They know. |
| 8 | reaper | the shrine keeper | `c1_w64` | He kept the moon shrine thirty years. He is keeping it still. |
| 9 | reaper | the bridge guard | `c1_w65` | She held the mill bridge the night of the fog. She holds it yet. |
| 10 | reaper | the bailiff | `c1_w66` | He was a bailiff. He still asks everyone their business. Tell him. |
| 11 | stalker | the hedge mother | `c1_w67` | She said prayers over the dead. Now she says them over you. |
| 12 | stalker | the archive's clerk | `c1_w68` | He was a clerk in the archive once. The Archivist will not speak of him. |

## Ashka's choice cards

Sheet `storyLines`, trigger `choice_<n>`: her line, then two chits drawn as small vellum slips, then her answer to the one picked. The chits are the commander's only words in chapter 1 and are not voiced. Neither is wrong. The game counts the warm picks; nothing in chapter 1 reads the count (story s7.7).

### Choice 1 (page 7, her tent, after `c1_b5_after`)

| Part | Speaker | Voice | Line |
|---|---|---|---|
| Her line | Ashka | `c1_a06` | Most hosts send their worst up the pass first, to pay the toll cheap. Why did you not? |
| Warm chit | Danta | none | Nobody in my banner is the worst. |
| Plain chit | Danta | none | It was the quickest way through. |
| After warm | Ashka | `c1_a07` | Then they are lucky in their captain, Honest. Do not grow proud on it. |
| After plain | Ashka | `c1_a08` | The snow likes a quick host. So do I, as it happens. |

### Choice 2 (page 7, her tent, after `c1_council`)

Plant 3 of 3: the seal on a cloak in the snow. She says no more than this.

| Part | Speaker | Voice | Line |
|---|---|---|---|
| Her line | Ashka | `c1_a09` | That seal on your writ. I have seen it before, on a cloak, in the snow. I was small. |
| Warm chit | Danta | none | Tell me about the snow. |
| Plain chit | Danta | none | That seal is on a great many things. |
| After warm | Ashka | `c1_a10` | Cold. A cloak. My brother pulling me out of it. He paid the toll, later. Now he walks. |
| After plain | Ashka | `c1_a11` | It is. The snow puts things in a head. It took my brother, eleven years ago. |

### Choice 3 (page 8, her tent, before Kingsfield is won)

| Part | Speaker | Voice | Line |
|---|---|---|---|
| Her line | Ashka | `c1_a12` | I stopped counting his host at the third ridge. Are you afraid, Honest? |
| Warm chit | Danta | none | Yes. Stand near me on the field. |
| Plain chit | Danta | none | It changes nothing. We fight. |
| After warm | Ashka | `c1_a13` | I will. Somebody has to keep the slow ones out of the snow. |
| After plain | Ashka | `c1_a14` | Good. Fear is for after. I will be on the left, if you look for me. |

## The Archivist

Sheet `storyLines`, companion `pell`. Six lines of one or two cards, each line under 40 words. Each use of the archive plays the next line he has not yet said, among those open. He is "the Archivist" on every card.

| # | Topic | Open | Voice | Card |
|---|---|---|---|---|
| 1 | What a writ is | always | `c1_p02` | A writ is the crown's seal on paper. It names a captain and gives them a standard. |
| 1 | | | `c1_p07` | Yours names three. Two are scraped. The seal does not mind. The seal is what matters. |
| 2 | The rule, from the copy | always | `c1_p03` | The rule is carved on the Stone. I read it from the book. The crown calls them. |
| 2 | | | `c1_p04` | The crown sends them home. A living hand, one crown, one standard, on the field of kings. |
| 3 | The roll of the dead | always | `c1_p08` | The roll of the dead. The first captain at the top, then everyone the road has had since. |
| 3 | | | `c1_p05` | I write them as they come. The road has not let me stop. |
| 4 | The Horn | after `c1_letter` | `c1_p09` | The Horn is older than the crown. It calls the buried kings when the land falls. |
| 4 | | | `c1_p10` | Who blew it, the book does not say. I was copying. I did not look up. |
| 5 | The Stone | after `c1_rule`, when the rule is asked for again | `c1_p11` | Against the Stone? I never have. I copied it myself, by lantern, and I am sure of my hand. |
| 6 | The first captain | from page 3 | `c1_p12` | The first captain was first on your writ and on my roll. He died holding the standard over someone. |
| 6 | | | `c1_p13` | Over whom, nobody wrote down. I have always meant to ask. |

The scenes use some of these cards: `c1_archive` (`c1_p02` to `c1_p05`) and `c1_rule` (`c1_p03`, `c1_p04`). `c1_p01` and `c1_p06` belong to their scenes only.

## Object text

Item text, read by the commander, never spoken. The sheet is not named in the story design.

| Object | Where | Text |
|---|---|---|
| The scraped writ | `c1_third_name` card 3; the writ in the archive | The crown's seal. To the captain of the standard's guard: carry the standard to the field of kings and hold it until a living hand takes the crown. Two names scraped, the second deeper. Then: Danta, of the Hollow. (39) |
| The turncoat's writ-copy | `c1_b1_after` card 3 | A fair copy, the wax still soft. By the crown's seal, a company of the Crown's Own is raised to serve the last living crown. On the back, in a firm hand: She will come this way. (37) |
| The drowned shroud | `c1_b4_after` card 2 | A burial shroud, sea-grey, stiff with salt. Someone has sewn the crown's seal on it in gold thread, small and exact. It is still wet. It does not dry. (29) |
| The roll of the dead, first page | `c1_archive` card 4 | The roll of the dead. At the top, in a careful hand: the captain of the standard's guard, fallen on the third day. Below him, names, page after page. (29) |
| The child's stone, drawn on the piece | `c1_king_after`, Plant 2 | FOR THE CHILD WE SENT NORTH |

The first name on the writ was scraped by the second captain when he wrote his own under it; the second, Edric's, by Maud's knife, deeper. The hand on the back of the writ-copy is her brother's; Maud knows it and says nothing. The writ's own form for the commander is "Danta, of the Hollow".

## The Stone and the copy

The Founders' Stone's own letters are drawn on the piece in the archive, in carved capitals; they are shown here in the copy's case so the difference stands out. The piece's readable text on use, the Archivist's book and every card carry the copy. Nobody in chapter 1 points out the difference.

| Line | The Stone, drawn on the piece | The copy, as the Archivist reads it |
|---|---|---|
| 1 | The crown calls them. | The crown calls them. |
| 2 | The crown sends them home. | The crown sends them home. |
| 3 | One hand, one crown, one standard, on the field of kings. | A living hand, one crown, one standard, on the field of kings. |

The difference is the one word "living" (with the copy's "A" where the Stone has "One"). The copy's reading is also in the writ's own text, "until a living hand takes the crown", and in Maud's want, `c1_m04`.

## The silent set

For the `story-voice` check (story s11): these ids are typed lines with a substitution and never get an `.ogg`. Every other id in this file is a voiced line that types silently until its `.ogg` exists (story s7.9). Item cards, chits and leaf text carry no voice id.

`c1_m08`, `c1_m26`, `c1_m29`, `c1_m41`, `c1_m46`, `c1_m52`, `c1_m58`, `c1_m65`, `c1_m71` (nine).

## Silence choices

Where the story design and the briefs are silent, this copy chose. The draft this file replaces had no section of this name; these twelve are new with the script, for Kort to keep or overturn.

1. A bonus-missed line plays only on a win without the bonus; a loss already says it.
2. Each fact goes to the companion who was there, did not fall, and has spoken least this visit, ties by the battle's order, so one visit is not one voice.
3. A companion in a replay of a battle fought before they joined says the replay set, so nobody is silent in a replay and nobody remembers a fight they missed.
4. The line about another's fall has one named speaker per battle and is skipped if that speaker was absent or fell.
5. Wren's fell line plays after one fall; after two, a resting line plays in its place.
6. `c1_shrine_2` is row 1 of Wren's memory, and the shrine's own rows start at 2, one per battle after that scene. A straight run with no replays reaches row 3 or 4 before `c1_crown`; the six are all heard only by a commander who replays or loses.
7. Item cards inside scenes (the writ, the writ-copy, the roll, the shroud) need a card with no portrait; `card item:<id>` is proposed, since the command list has none.
8. The dying raider speaks with no portrait and the voice initial x; the brief's list of initials has none for him.
9. Her brother speaks once, in `c1_king_before`, so his portrait has a line, and his name waits for chapter 2 under the one-name-a-page rule. Maud's "your banner" line rides on her last card of `c1_king_after`, so the scene stays at six cards.
10. The child's name on the mound is a role, "FOR THE CHILD WE SENT NORTH", by the familial rule. Plant 3, which story s6 does not place, is the cloak in the snow in Ashka's second choice card.
11. The grave roll's names are bynames, and the Archivist does not give his name in chapter 1, both by the familial rule.
12. The echo sits at the first fire Wren shares, on page 3. The leaves of pages 4 and 8 are titled "The Gate" and "The Field of Kings", and battle 6's feed says "the King's host", so neither a map name nor "the Hollow host" reaches a leaf or the feed.
