# Chapter 1 copy: The Long March, I: The Road North

Player-facing strings for campaign chapter 1, written 2026-10-18 from the Phase 4 spec (`docs/superpowers/specs/2026-10-18-phase-4-modes-campaign-design.md`, sections 8, 10 and 11) and the Houses spec. Nothing here is in a sheet or in code yet. Status: draft for Kort's review; the spec's own words are approved, the new lines are not. "s10" below means spec section 10.

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

How these were written:

- Verbatim from the spec: battle names, briefings, reasons, bonus lines, the epilogue, the Siege how-to lines and the button label. Everything else is new.
- Feed lines fit the feed slip: measured in the default 8 px font (advances read from the 1.20.1 `ascii.png`) against its 140 px text window (UI plan, Task 3), width in px after each line. Five of the spec's example lines did not fit; see "Fit changes" at the end.
- `{team}` is the acting side's short name, shown as Dawn and Dusk as in the spec. Measured with the Phase 2 side names too (Gold, Violet, Crimson, Teal), only the Siege yard line overflows, and only for Crimson (143 px).
- How-to lines are under 60 characters (count in brackets) and carry no tunable numbers, except Siege's own "four times".
- Defeat lines are true for a draw as well as a loss, so one line serves both. Victory and defeat lines are plain notices with the emblem glyph.
- ASCII only, no dashes as punctuation, no exclamation marks. "Chapter" is the Founders plaque's word, so no on-screen string here uses it.

## The six battles

Map, mode, difficulty, banners and bonus are from s10 and s11. The heading, briefing, reason and bonus are the spec's words; the tag, victory and defeat lines are new.

### 1. The Training Field

Map: Rivergate Valley (map 0). Mode: Domination. Difficulty: Easy. Allies: none. Enemy: `march1_turncoats` (10 Order Squires, 6 Order Archers, four empty slots). Bonus: `hold_all`, Gilded shards.

- Briefing: Turncoat knights hold the river road. Take the crossings and teach your banner to follow you.
- Reason: The road west must open before the march begins.
- Bonus: Hold all three crossings at the same moment.
- Bonus tag: hold all three (kept line 130 px, lost line 128 px)
- Victory feed: `The road west is open.` (114 px)
- Defeat feed: `The road west stays shut.` (132 px)
- Note: Replaces the live sheet's briefing, which still says "Dusk raiders". The four lessons are unchanged and stay in code (`GravesAndGloryClient.lessonText`).

### 2. The Moon Shrine

Map: Hollowwood Reach (map 3). Mode: Survival. Difficulty: Easy. Allies: none. Enemy: the "moon" waves at Easy scale (`waveSet` moon). Bonus: `lose_at_most 8`, Graveborn shards.

- Briefing: The old graves of Hollowwood are opening. Hold the shrine until the last of the dead lies still.
- Reason: The march camps at the moon shrine tonight; there is nowhere else to sleep.
- Bonus: Lose no more than 8 troops.
- Bonus tag: few losses (kept line 115 px, lost line 113 px)
- Victory feed: `The dead lie still.` (85 px)
- Defeat feed: `The dead hold the shrine.` (129 px)

### 3. Ironhold Retaken

Map: Ironhold Siege (map 1). Mode: Siege (attack). Difficulty: Normal. Allies: `ironclad_normal` (its mortar and four ballistas). Enemy: `graveborn_easy`, `graveborn_normal`, defending. Bonus: `stage_by gate 360 s`, Ironclad shards.

- Briefing: The dead took Ironhold while its smiths slept. The Ironclad bring the engines; you bring the swords. Take the yard, break the gate, seize the keep.
- Reason: The wagons cannot pass while the dead hold Ironhold.
- Bonus: Break the gate inside six minutes.
- Bonus tag: gate in time (kept line 117 px, lost line 115 px)
- Victory feed: `Ironhold is retaken.` (100 px)
- Defeat feed: `The wagons turn back.` (113 px)

### 4. Raiders on the River

Map: Rivergate Valley (map 0). Mode: Banner. Difficulty: Normal. Allies: `ironclad_easy` (takes the guard role, so you raid). Enemy: `tidecallers_easy`, `tidecallers_normal`. Bonus: `carry_self`, Tidecallers shards.

- Briefing: Tidecaller raiders have come up the river for the march's banner. Guard yours and carry theirs home.
- Reason: Without its banner the march is just a crowd.
- Bonus: Carry their banner home yourself.
- Bonus tag: you carry it (kept line 123 px, lost line 121 px)
- Victory feed: `The raiders go home empty.` (138 px)
- Defeat feed: `The raiders hold the river.` (137 px)

### 5. The Frozen Pass

Map: Frostmarch Pass (map 2). Mode: Domination. Difficulty: Normal. Allies: `ironclad_normal`. Enemy: `wildmoot_normal`, `wildmoot_normal`. Bonus: `win_within 600 s`, Wildmoot shards.

- Briefing: The Wildmoot let no host through Frostmarch that has not bled for it. Take the pass before the snow closes it.
- Reason: The pass is the only way north before the snow.
- Bonus: Take the pass inside ten minutes.
- Bonus tag: win in time (kept line 109 px, lost line 107 px)
- Victory feed: `The pass is yours.` (94 px)
- Defeat feed: `The snow closes the pass.` (133 px)

### 6. Kingsfield

Map: Kingsfield Crossroads (map 4, five points). Mode: Domination. Difficulty: Hard. Allies: `ironclad_normal`, `wildmoot_normal`. Enemy: `graveborn_hard` (holds the Hollow King), `mix_gilded_graveborn_normal` (the turncoats returned), `graveborn_normal`. Bonus: `kill_unit gb_hollowking`, Graveborn shards.

- Briefing: The Hollow King's host has reached Kingsfield. The Houses you met stand with you now. Break his host and the road is yours.
- Reason: Every road meets at Kingsfield, and so does the Hollow King.
- Bonus: Strike down the Hollow King with your own banner.
- Bonus tag: strike the King (kept line 134 px, lost line 132 px)
- Victory feed: `The Hollow host is broken.` (131 px)
- Defeat feed: `The Hollow host holds.` (111 px)

## Modes

The how-to lines go on the briefing card under the reason (s8); the player's side picks the set. A feed line shows the same words to both sides. The glyph is the seal for our side and the struck seal for theirs, in the team's ink; "emblem" is the neutral glyph.

### Siege (Ironhold)

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

| Part | Copy |
|---|---|
| Epilogue (`chapters.epilogue`) | Kingsfield holds. When the field was searched, the Hollow King's crown was gone. |
| Title | Legendary Writ |
| Body | The road north is behind you. This writ calls one Legendary to your banner: Lord Marshal Aldric, the Hollow King, Brassjaw, Admiral Vael or Old Horn. |
| Note | Choose now, or keep the Writ for later. It is spent once, on one Legendary. |
| Button | Choose your Legendary |

## Camp guide blurb (40 words)

The dead do not stay buried, and the road north runs through them. Six battles, five Houses and four ways to fight lie between Rivergate and Kingsfield. Win each battle to open the next. The last pays a Legendary Writ.

## Fit changes against the spec's section 8 examples

| Spec line | px | Used | px |
|---|---|---|---|
| The portcullis drops in 10 s | 142 | The portcullis falls in 10 s | 135 |
| The dead rise at the chapel | 141 | Dead rise at the chapel | 119 |
| Wave 3 laid to rest, +80 gold | 144 | Wave 3 at rest, +80 gold | 123 |
| The dead are taking the shrine | 157 | The shrine is being taken | 127 |
| Bonus kept: break the gate inside six minutes | 226 | Bonus kept: gate in time | 117 |

Not changed: "Dawn took the Outer Yard", "The Iron Gate is breached", "Dawn has Dusk's banner", "Dusk's banner is home" and "Dawn scores, 1 to 0" fit as written.
