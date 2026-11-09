# Chapter 1 "The Oath": battle design for 1.0

Status: approved by Kort, 2026-11-09 (`kort-decisions-2026-11-09.md`), after one Sonnet-high review. Lore lead. Turns `docs/drafts/chapter-1-outline-2026-11-09.md` into six playable battles, checked against `main` at `54e5d339` (`sheets/design.json`, `Campaign.java`, `MarchMath.java`, `Modes.java`, `CampaignMaps.java`). Proposals are marked **[call]**. This document edits no code and no sheet.

Tie-breaker from Kort (2026-11-09): 1.0 is remembered for deep tactics and battles that look and feel great. **Every battle below teaches or tests one tactic**, named on its "Tactic" line.

## 1. What the game can do today, and what this design assumes

The six shipped battles (`march-1` to `march-6`) are sheet rows with map, mode, size (1 to 3 commanders a side), tier, lessons, briefing (160 characters or fewer), reason (70 or fewer), bonus and allies. They are order-locked, solo, and pay a Pact when all six are cleared.

| Need | Today | This design |
|---|---|---|
| Six battles over all five maps and all four modes | yes | kept |
| Siege on Ironhold only, player attacks | yes | battle 3 |
| Convoy on Frostmarch only, player escorts | yes | battle 4 |
| Survival on Hollowwood only, Graveborn troop waves | yes | battle 2, as the Skeleton King's dead |
| Kingsfield has five points | yes | battle 6 |
| Allied bot banners | yes (`allies`), but any row named there is removed from every tier pool (`MarchMath.java:98`), so naming `gilded_normal` would take it out of skirmish and practice | **new dedicated ally rows (S1)**, sheet only |
| Legendaries in enemy armies | every hard-tier row leads with one | battle 6 uses `wildmoot_hard` for Old Horn |
| Bonus kinds | `hold_all`, `lose_at_most`, `stage_by`, `win_within`, `never_rolled`, `kill_unit` | five of them used; new `bonusObjectives` rows needed (S2) |
| **Enemy House chosen per battle** | **no**: bots come from tier rows in sheet order by lane. Today's briefings already name enemies the code does not field, and a 3v3 hard battle pulls `graveborn_hard`, which would put the Hollow King in chapter 1 | **needs code (N1), required** |
| **The player commands the chapter's House** | **no**: the player fields their own collection banner (`Campaign.java:359`) | **decided: own banner in 1.0 (D1)** |
| Text after a battle | no: only the briefing card before it | **needs code (N2), required** for the design's seeds |
| Magic flicker on casters (the glimpur ebb) | no | **needs code (N3)**; the approved design plants it in 1.0 |
| Storybook pages, stills, stage scenes, card items, archive | no; `PLAN-TO-1.0.md:16` keeps them out of 1.0 | moved to a later story layer (section 6) |
| Map relics (stone hand, carved stone) | no relic system; Kort designs the maps | **Kort's maps (D4)** |
| "The Long March" in player text | yes, in eight places | **needs a text change (N4), required** |

## 2. The chapter on one line

The Gilded answer the Tidecallers' drowning of the coast, meet the dead of a realm the confederacy forgot, then are ordered to clear the Wildmoot from the quarry road and do it. Aldric ends wanting to read the Charter again. Hints: spells sputter, and a quarry uncovers a carved stone shape.

The player fights or fights beside four Houses: Tidecallers (battles 1 and 3), Ironclad (ally, battle 3), Wildmoot (battles 4 to 6) and their own. Graveborn troops appear in battle 2, but as the Skeleton King's bound dead, not as the rebel House; the House itself is first met in chapter 5. The design's archive items showing that the Skeleton King was a Gilded king are deferred with the story layer.

## 3. Battles

Player text: ASCII, no dashes as punctuation, no exclamation marks. Counts are in section 5.

### Battle 1: The Drowned Crossings

- **Story beat.** The tide took eleven Gilded coast towns. The Tidecaller raiders who did it hold the river crossings inland. The Order rides to answer under the Charter.
- **Map and mode.** Rivergate Valley, Domination (three points). 1v1, easy, lessons on.
- **Armies.** Player: their banner (D1). Enemy: `tidecallers_easy` (N1): Drowned Raiders, Harpooners, Trident Guards, Mist Callers. No Legendary.
- **Tactic.** Concentrate and take one point at a time. The middle crossing sits on the river, where Tidecallers in the water move faster; the player learns to take the two bank points first and fight the river point from the bank.
- **Learns or unlocks.** The four shipped lessons (follow, formation, attack and charge, barracks) and the one-time recruitment roll gold. Bonus `hold_all`: hold all three crossings at once.
- **Text.**
  - Briefing: "Tidecaller raiders hold the river crossings above the drowned coast. Take them one at a time and teach your banner to follow you."
  - Reason: "The Charter says every wrong is answered. This one first."
  - After (N2): "A Sunweaver's light sputters in her hand. Aldric: Damp powder. See that it is dry." Hint 1.

### Battle 2: The Moon Shrine Watch

- **Story beat.** At the edge of the realm the graves open. The dead wear the colours of an old levy. Aldric reads them after: Tengervald, a realm the confederacy struck from its relief list two lifetimes ago. Nobody names the Skeleton King.
- **Map and mode.** Hollowwood Reach, Survival. 2v2, easy.
- **Armies.** Player plus one allied Gilded banner (`ally_gilded_easy`, S1). Enemy: the shipped Survival director (Risen, Bone Archers, Grave Reapers, Barrow Stalkers, Gravecallers) under the moon. Waves grow by half for the second banner (`survivalPerBanner`). No code needed.
- **Tactic.** Hold a position. No enemy commander and no points: shields in front of the shrine, archers behind, and no chasing into the trees.
- **Learns or unlocks.** Defence in depth. Bonus `lose_at_most`: lose at most 8 troops.
- **Text.**
  - Briefing: "The graves of Hollowwood are opening under the moon. Hold the shrine with the Order's second banner until the dead lie still."
  - Reason: "The dead wear colours no one has worn in a lifetime."
  - After (N2): "Aldric reads the faded colours. Tengervald. Struck from the relief list two lifetimes ago. Someone is still answering for it."
- **Lore note.** These are the Skeleton King's bound dead, played by the Graveborn troops. Chapter 1 never calls them Graveborn. An art tell (intact binding sigils on the bound dead, cut cords on the rebels) would separate them later; proposal only. "Tengervald" is a placeholder name; using it in player text needs Kort's yes (D3).

### Battle 3: Smoke at Ironhold

- **Story beat.** Tidecallers have taken the Ironhold gate in revenge for the rivers the Ironclad's smelters poison. The Ironclad ask the Order for help. The river runs the wrong colour and Aldric does not ask why. This plants chapters 3 and 4 without explaining them.
- **Map and mode.** Ironhold Siege, Siege (player attacks). 2v2, normal.
- **Armies.** Player plus one allied Ironclad banner (`ally_ironclad_normal`, S1) with Ballista Crews and the Mortar. Enemy: `tidecallers_normal` (N1), with Mist Callers and a Kraken-touched Brute.
- **Tactic.** Combined arms in a siege. Mist halves the player's arrows, so the allied engines break the gate while the player's shields screen them.
- **Learns or unlocks.** Siege stages (yard, gate, keep). Bonus `stage_by`: break the gate within 360 seconds.
- **Text.**
  - Briefing: "Tidecallers hold the Ironhold gate and the Ironclad cannot take it back alone. Bring their engines up, break the gate, take the keep."
  - Reason: "The river under Ironhold runs the wrong colour. No one asks why."
  - After (N2): "The smith thanks the Order and does not meet Aldric's eye. A dying raider says one word. Admiral."

### Battle 4: The Hunters' Road

- **Story beat.** A rider from the capital: the council orders the Wildmoot grazing lands cleared so the quarry road can open. Aldric reads it twice and obeys. The chapter turns from answering a wrong to doing one.
- **Map and mode.** Frostmarch Pass, Convoy (player escorts). 2v2, normal.
- **Armies.** Player plus `ally_gilded_normal` (S1). Enemy: `wildmoot_normal` (N1): Moot Hounds, Web Stalkers, Tusk Spears, a Bloodhowl Seer.
- **Tactic.** Screen a moving objective. Any one enemy beside the wagon stops it, and hounds are the fastest troops in the game; the player keeps shields beside the wagon instead of chasing kills.
- **Learns or unlocks.** Convoy and checkpoints. Bonus `never_rolled`: the wagon never rolls back.
- **Text.**
  - Briefing: "The council orders the grazing lands cleared for the quarry road. The wagon carries the hunters. Walk beside it through the pass."
  - Reason: "Aldric read the order twice. This is the hunt again."
  - After (N2): "A boar-kin hunter in the snow: We hid for a hundred years. We stopped hiding."

### Battle 5: The Grazing Grounds

- **Story beat.** The grass is ripe and the order is to burn it. The Order takes the grounds. The fire does not catch, and a quarryman says the stone is cold in the cut.
- **Map and mode.** Hollowwood Reach, Domination (three points). 3v3, normal.
- **Armies.** Player plus `ally_gilded_normal` twice (S1). Enemy: `wildmoot_normal` on all three lanes (N1).
- **Tactic.** Squad cohesion. The Wildmoot's pack trait hits harder when three hunt one troop or strike from behind; lone squads die. The player moves squads together and guards flanks.
- **Learns or unlocks.** Bonus `lose_at_most`: lose at most 12 troops **[call: tune in the sim]**. A loss cap rewards cohesion directly.
- **Text.**
  - Briefing: "The grass is ripe and the order is to burn it. Take the grazing grounds before the herds come back for them."
  - Reason: "The quarry road needs the plain. The herds were there first."
  - After (N2): "The fire does not catch. A quarryman says the stone is cold in the cut."

### Battle 6: The Antlered Alpha

- **Story beat.** The quarry road is open. At the cut lies a carved stone shape the size of a house, half uncovered; nobody names it. The Wildmoot come to close the road, led by Old Horn. His line breaks and he withdraws, for chapter 2. Aldric asks what the stone is, is told "stone", and orders it covered.
- **Map and mode.** Kingsfield Crossroads, Domination (five points). 3v3, hard.
- **Armies.** Player plus `ally_gilded_normal` twice, one tier below (S1), so the finale is hard but not lonely. Enemy (N1): `wildmoot_hard` on lane 0, led by Old Horn, and `wildmoot_normal` on lanes 1 and 2, so only one Old Horn takes the field.
- **Tactic.** Spread or concentrate. Five points thin both armies, and Old Horn's charge slam breaks formations; the player decides where to mass and answers the Legendary with their own banner.
- **Learns or unlocks.** Clearing the chapter pays the Pact (a Legendary). Bonus `hold_all`: hold all five points at once (a new bonus id of the shipped kind). **[call]** Not `kill_unit`: that bonus can only be earned by killing him, and the story needs him alive for chapter 2. Old Horn can still fall in battle as a troop, as every Legendary can; the after line says the line breaks and he withdraws.
- **Text.**
  - Briefing: "The quarry road is open and the Wildmoot have come to close it. Old Horn leads them. Hold the five crossings and break his line."
  - Reason: "At the cut lies a carved stone the size of a house."
  - After (N2): "The Wildmoot line breaks and Old Horn withdraws. Aldric asks what the stone is. Stone, says the quarryman. Aldric orders it covered."
  - Chapter end (N2): "Aldric, alone with the Charter. The lamps flicker once. I swore to protect every person under it. I would like to read it again."

## 4. Where the hints live in 1.0

| Hint | Where | Needs |
|---|---|---|
| Spells sputter | battle 1 after line; flicker on casters in battles 4 to 6 | N2, N3 |
| The cold stone and the carved shape | battle 5 after line, battle 6 reason and after line | the shape on the Kingsfield map needs D4 |
| Tengervald struck from the relief list | battle 2 reason and after line | N2 |
| Relics on maps (a stone hand on Rivergate, a statue on Ironhold) | maps | D4 |
| Aldric wants to read the Charter again | chapter end | N2 |

Without N2 the 1.0 chapter keeps only the old colours (battle 2 reason) and the carved stone (battle 6 reason), and the design's goal that a player suspects the land is alive is not met. Without N3 the ebb seed Kort chose is missing from 1.0. Both are therefore required for the design as approved; dropping either is Kort's call (D2).

## 5. Proposed sheet rows (not applied)

| id | order | name | map | size | tier | mode | lessons | bonus | allies (S1) | enemy (N1) |
|---|---|---|---|---|---|---|---|---|---|---|
| oath-1 | 1 | The Drowned Crossings | rivergate | 1 | easy | domination | yes | hold_all 3 | none | tidecallers_easy |
| oath-2 | 2 | The Moon Shrine Watch | hollowwood | 2 | easy | survival | no | lose_at_most 8 | ally_gilded_easy | (director) |
| oath-3 | 3 | Smoke at Ironhold | ironhold | 2 | normal | siege | no | stage_by gate 360 | ally_ironclad_normal | tidecallers_normal |
| oath-4 | 4 | The Hunters' Road | frostmarch | 2 | normal | convoy | no | never_rolled | ally_gilded_normal | wildmoot_normal |
| oath-5 | 5 | The Grazing Grounds | hollowwood | 3 | normal | domination | no | lose_at_most 12 | ally_gilded_normal, ally_gilded_normal_b | wildmoot_normal x3 |
| oath-6 | 6 | The Antlered Alpha | kingsfield | 3 | hard | domination | no | hold_all 5 | ally_gilded_normal, ally_gilded_normal_b | wildmoot_hard, wildmoot_normal x2 |

- **Ids.** New ids `oath-1` to `oath-6` (Kort, D5). Old campaign progress resets: a `ProfileMigration` step clears won battles and claimed bonuses of the retired `march-*` rows, and keeps Pacts held and Legendaries bought. Keeping the old ids would have shown new battles as cleared.
- **Bonus house.** Every row pays Gilded shards, since the chapter is Gilded's. The War Table's per-House glyph on the campaign card then shows one House throughout.
- **Bonus rows.** Every battle gets a new `bonusObjectives` id (shipped kinds); none reuses a `march-*` bonus id, so old claims cannot block new bonuses.
- All briefings are 133 characters or fewer and all reasons 64 or fewer (script check).

## 6. What moves to a later story layer

The outline's book stills, stage scenes, the Charter card, the relief roll and the hunting tally need the storybook engine, which `PLAN-TO-1.0.md` keeps out of 1.0. They stay as that layer's script. When it arrives (season 2 at the earliest **[call]**), chapter 1 gains them without changing a battle.

## 7. Work needed (proposals, none approved)

Sizes are rough: S under a day, M a few days.

- **N1, enemy banner per battle (required, S to M).** A sheet field naming the enemy bot rows, read where `Campaign.java` seats team 1 (about line 341, `botBanner` about 370). Without it the chapter fields whatever tier rows come first, including the Hollow King at hard tier.
- **N2, after-battle line (required for the design, M).** One text field per battle on the result screen, plus a chapter-end line. `PLAN-TO-1.0.md:16` says no named cast in 1.0; Aldric is a shipped Legendary, not new cast, but the plan line needs Kort's amendment for him to speak.
- **N3, glimpur ebb tell (required for the design, M).** A flicker or dimmer glow and a sound on caster troops in chosen battles, no mechanical effect.
- **N4, retire "The Long March" in player text (required, S).** `CampaignClient.java:176`, `Campaign.java:309,524`, `WarTableReport.java:510,513,515`, `Progression.java:128` (check) and `README.md:138`. Code comments can stay.
- **S1, ally rows (sheet, S).** `ally_gilded_easy`, `ally_gilded_normal`, `ally_ironclad_normal`, copies of the House rows, so no skirmish pool loses a row.
- **S2, rows and bonuses (sheet, S).** The six rows above and the new bonus row.
- **D1 follow-up, chapter House banner (M).** Only if Kort picks (b).
- **Verification.** Rewrite the `p4-march-*` campaign checks for the new rows; re-run the sim gate and the bot-quality checks after the sheet changes; one full chapter run in a harness.

## 8. Decided (Kort, 2026-11-09)

- **D1.** The player's own collection banner in 1.0, with Gilded banners beside them. A fixed House banner per chapter is built before season 2, which with one-month seasons means with the 1.0 campaign code.
- **D2.** N1 to N4, S1 and S2 approved, and the `PLAN-TO-1.0.md:16` amendment letting Aldric speak in after lines.
- **D3.** Battles, names and lines approved as drafted.
- **D4.** The carved stone at a Kingsfield quarry cut, a stone hand on Rivergate and a statue on Ironhold go into Kort's map designs.
- **D5.** New ids; old campaign progress resets, held Pacts and bought Legendaries stay.

Names follow `names.md`. Chapter 1's player text uses only Tengervald, the Charter and Aldric; Wakeland, Highheart and the Concord may appear in a later story layer.
