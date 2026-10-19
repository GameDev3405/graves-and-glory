# Phase 4: modes and campaign chapter 1, design

## Questions for Kort

1. Banner mode is "carry the enemy banner home", and the supply wagon escort, the vision's other Banner variant, goes to PARKED. [Yes; the carry reuses touch pickup and the rout override, while a wagon needs a moving vehicle and a road path on Rivergate]
2. Campaign battles 3 to 6 bring allied bot banners, the Ironclad from battle 3 and the Wildmoot as well in battle 6, so the chapter grows from 1 v 1 to 3 v 3. [Yes; battle 3 needs ballistas a new player does not own, and the growing host is the chapter's arc]
3. Each campaign battle has one fixed difficulty, with no difficulty picker. [Yes; a picker triples the tuning and the bonus checks, and the banners carry the curve]
4. Skirmish's "any banner" means you choose the enemy's House and always field your own banner. [Yes; Practice is where you try troops you do not own]
5. The Hollow King in battle 6 is a normal Legendary troop in the enemy banner, with no boss rules of his own. [Yes; boss rules can wait for chapter 2]
6. A profile that already got the chapter 1 Pact from the one-battle Phase 2 chapter keeps it and gets no second Pact for finishing all six. [Yes; nothing is taken back, and the gate run uses a fresh profile]
7. A solo Skirmish starts straight from the picker (mode, map, difficulty, enemy House, size, Siege side) and does not go through the war-gates pads; the pads stay for parties and Phase 5, and the Ride plate keeps its name. [Yes; pad team, pad size and map votes would otherwise fight the picker, and it settles who owns the settings when two players share a server]

Status: approved by Kort 2026-10-18 (all seven questions answered yes; the escort variant is parked and will be revisited). Plan to be written once Phase 2's code names exist. Sources: the vision (binding), the approved Houses spec, the built-world spec and the UI spec. Phase 4 starts after Phases 2 and 3 merge and assumes their plans as written. Every number invented here is marked "(draft value)"; numbers taken from a sheet or another spec are not.

## 1. Intent and why

Phase 4 turns one mode into four and gives the solo player a story to march through. The vision asks for Siege on Ironhold, Survival on Hollowwood and Banner on Rivergate, a Skirmish on your own terms, and a six-battle chapter 1 whose end grants a Legendary. Kort's gate is plain: he finishes chapter 1 in one sitting and wants chapter 2.

How it serves the pillars:
- Readable drama: each mode has one thing to watch, a gate's strength, the next wave or a banner in someone's hands, and it shows on the existing ledger bar.
- Your banner is yours: campaign battles field the player's own banner and pay normal rewards. Today they run as practice.
- Built, not generated: every mode plays on the Phase 3 landmarks. Siege adds only the reserved intact gate and one short ward wall piece.
- Solo is a real game: the campaign and Skirmish need no friends, and Survival also seats up to four.
- Honest numbers: every new number lives in a sheet, and the economy model is rerun for the new gold.

## 2. Goals, non-goals, anti-goals

Goals
1. Siege, Survival and Banner, each with a win condition, a clock, scoring, reinforcement and morale rules, bot play at all three tiers and a ledger bar.
2. Skirmish: any mode on the maps that host it, any difficulty, any enemy House, your own banner, normal rewards.
3. Campaign chapter 1, "The Road North": six battles in the Houses order, each with a briefing card, a reason, one bonus objective and a fixed difficulty. Finishing it grants the Houses spec's Legendary Pact.
4. Campaign battles seat the player's own banner, honour the battle's allies and enemies, pay normal rewards and open in order.
5. Battles won, bonuses claimed and the chapter claim are saved in the same atomic save as gold.
6. In-game checks per mode and per battle, one bot-vs-bot run per mode, and Kort's one-sitting gate.

Non-goals
- New maps. The purpose-built fortress and woodland shrine are Phase 6, and "More maps beyond five" stays parked.
- Siege, Survival or Banner on other maps. The `modes` sheet lists each mode's maps, so a later map is a row.
- Chapters 2 and 3, which the vision calls content for later, not promises.
- Campaign with friends. Campaign battles stay solo, as the existing MARCH_SOLO refusal says; friends are Phase 5.
- Fixing the bot tier ladder, which Kort parked until the Phase 3 maps. Modes add behaviour on top of the tiers as they are.
- The bases' barracks and store road clips that Phase 3 parked for Phase 4. They are a world fix, not a mode; the Phase 4 plan can carry them as a separate task, and nothing here depends on them.

Anti-goals
- No new dependency, new entity renderer or new map.
- No boss mechanics, cutscenes or voice. The briefing card, the feed and the report carry the story.
- No difficulty curve that rests on the tier label alone. The Phase 1 sims show the tiers are not monotonic, so the curve comes from banners and headcounts.
- No loaned banners in Skirmish or the campaign.
- No second HUD. Every mode uses the ledger bar, the seals and the feed, and anything new is named as new.
- Parked ideas stay parked: medal challenges, daily seeds and veteran squads. Two parked items return on purpose: capture-the-banner, which PARKED already marks as the roadmap's Banner, and the +5 shard bonus objective that Phase 2 deferred. Bonus objectives are one per campaign battle and paid once; they are not the parked medal challenges.

## 3. Constraints and what the code does today

Rules that bind this design:
- Minecraft 1.20.1 with Fabric; no new dependencies.
- Numbers live in sheets/design.json. Spec.java is generated by tools/preflight.py and never hand-edited. Every cell is filled, every row has the same columns, and a double needs a decimal point in the first row.
- One static Battle per server, so one round and one mode at a time.
- Harness code lives only in test-tools/, and its class names join the forbidden tuple in tools/release_config.py.
- A packet layout change bumps its channel id. STATE is state_v6; the UI plan takes v7 and adds feed_v1, and the Phase 2 plan names state_v8.

What the code assumes today, all of it Domination:
- `Battle.tick` scores owned points every `scoreInterval` up to `winScore`; a time-out goes to the higher score, and a tie is a draw.
- `Battle.base(t)`, the barracks and the respawn spot are fixed per team from `Spec.FACTIONS`. `Morale` sends routing groups to `base(team)` and reads one global row, `Spec.MORALE[0]`.
- `Campaign.botDecide` picks a capture point and an order, with per-group assignments for shieldwalls. Nothing knows a gate, a wave or a banner.
- `Tactics` targets enemy units and players. Phase 2 adds structure targets, `ArmySupplyBanner` and `Battle.structures`, with counter C10 at x4.0 for siege troops and C11 at x0.25 for the rest.
- `campaign:<id>` starts any battle in any order, and `soloStart` seats it as practice: one bot, practice gold, every troop unlocked, `campaignBattles.size` ignored. `campaignCleared` is the highest order won.
- "Mode" already means lobby size in the snapshot and the `mode:` command, and the order sheets in the UI spec. This spec calls the game mode `battleMode` in code; "mode" appears only on screen.

From Phases 2 and 3 this design relies on: all five Houses fieldable; `botBanners` rows `<house>_<difficulty>` and the mixed rows; the `Anchor` record and `reinforcementAnchors`; structure targets; profile schema 2 with `pacts`, `writChapters` and `campaignCleared`, migrated by the pure `ProfileMigration`; the landmark pipeline, one piece per tick at most 100 ms each; and the reserved `ironhold/gate_intact`.

## 4. The mode frame

Each mode answers the same questions, so the code routes them through one per-round mode object rather than special cases:

| Hook | Domination today | What a mode may change |
|---|---|---|
| Setup and teardown | points and flags | mode pieces, mode sites, flags taken down and put back |
| Field base per team | the team base | where the lineup forms, players respawn, troops are bought and routing groups run |
| Anchors | points held 10 s with no enemy within 20 | which sites deploy bought troops |
| Scoring | +1 per owned point per interval | its own counters |
| End | 300, then the higher score or a draw | its own win, and who wins at time-out |
| Morale | the `domination` row | its own `morale` row, plus guards for carriers and waves |
| Bots | point choice | a target and per-group assignments; each tier's retreat, rally, charge and shieldwall logic stays |
| Ledger | scores, tracks, clock, seals | what each of them shows |

Shared rules:
- Teardown restores the map: mode pieces out, Domination flags back, the world clock back. A server that stops mid-round restores the map on its next start from a saved "mode pieces placed" marker.
- Mode state is round state: startRound creates it, finish reads it, returnLobby drops it.
- The chosen mode is a lobby setting like `map` and `difficulty`, set by a new `battlemode:<id>` command. A mode the chosen map does not host is refused with a notice.
- The intro briefing card, today's 6 s skippable card, gains the mode's rule lines, so the first play of a mode explains itself.
- Network: mode state rides in what STATE already sends, the two scores, the point list with owner, progress, challenger and contested, and the time left, plus a `battleMode` object in the snapshot JSON with the mode id, rule lines and the two track maxima. The binary layout of state_v6 does not change, so there is no channel bump. If the builder adds binary fields, the channel takes the next free id after v8.

## 5. Siege on Ironhold

Sides: Dawn attacks from the west and Dusk defends, because the gate faces west. In Skirmish the player picks a side; the campaign battle is an attack.

Stages, in order. A stage that falls stays fallen.

| Stage | Where | Falls when |
|---|---|---|
| A Outer Yard | a capture ring between the west curtains at x -72 and the gate at x -37, centred near x -55, z 0 (draft value), radius 15 (draft value) | attackers hold it uncontested for 20 s (draft value) |
| B Iron Gate | a structure target in the gate passage with 1600 HP (draft value) | its HP reaches 0 |
| C The Keep | a ring in the keep's west court, centred near x -8, z 34 (draft value), radius 10 (draft value) | attackers hold it uncontested for 30 s (draft value) |

The flow:
1. The round starts with the gate open, as Phase 3 built it, and the keep barred: it cannot be captured until the gate falls. The defenders' lineup forms in the yard, the attackers' at their base.
2. When the yard falls, the attackers gain 180 s (draft value), the feed warns that the portcullis drops, and 10 s (draft value) later `ironhold/gate_intact` replaces the open gate. Units in the passage are first moved to the nearer side. Defenders still outside are cut off; that is the price of losing the yard.
3. The gate takes damage only from attackers and only in stage B, through the Phase 2 counters. 1600 HP is about a minute of two ballistas and a mortar, or three minutes of eight swords, judged from the Houses spec's damage and cooldowns. At 0 HP the open gate piece returns as the breach, the attackers gain another 180 s (draft value), and the keep unbars.
4. Attackers win by taking the keep. Defenders win when the attacker clock runs out. There are no draws.

Clock: 600 s (draft value) at the start plus the two bonuses, so with these draft values a full siege runs 10 to 16 minutes.

Pieces and landmarks:
- `ironhold/gate_intact`, reserved by Phase 3, is authored now: the `ironhold/gate` footprint with the portcullis down and the doors shut.
- New piece `ironhold/ward`, a short Siege-only wall placed twice: from the gate wall's north end to the river bank, and from its south end to the keep. The passage becomes the only dry way east on the west bank. The wards go in at setup, come out at teardown and take no damage.
- The gate tower tops at feet 13 and the keep's north wall-walk at feet 7 are defender ranged stations. The tower tops see the west field and the yard; the wall-walk sees the B deck. The donjon on the east bank is the defenders' field base for respawn, barracks and routs.
- The curtain line bounds the yard: its central opening is the front door and the sally gaps are the flanks.
- Web Stalkers climb the gate and the wards, as Kort decided. Because the keep stays barred, climbing never skips a stage, so no overhangs are added and the Houses spec's overhang note is dropped. Tidecallers fording past a ward are their House's siege answer in the same way.
- The Domination flags at A, B and C come down for the round.

Reinforcement and respawn:
- Defenders deploy at the yard while it is an anchor, otherwise at the donjon. Defender players respawn at the donjon.
- Attackers deploy at their base until the yard falls, then at the yard. Attacker players respawn at the last stage taken, or at base before that.

Morale:
- While the gate stands, a defender group whose centre is inside the walls routs at 15% strength (draft value) instead of 25%. Inside means within 14 blocks (draft value) of the gate or 25 blocks (draft value) of the keep's centre. Once the gate falls, the walls stop counting.
- When several rout shares apply, the lowest wins; the Graveborn's `fearless` 12% is already lower.
- Routing defenders run to the donjon, attackers to their last stage or their base. A routing group that a shut gate cuts off cannot get home, and its rout ends on its timer.

Bots add these plans to each tier's own logic:

| Stage | Attackers | Defenders |
|---|---|---|
| A | attack the yard | infantry hold the yard; ranged on the tower stations |
| B | siege troops hold a standoff spot about 25 blocks (draft value) west of the gate and strike it; infantry attack the passage; ranged shoot the towers | infantry wait behind the passage; ranged on the towers; at 25% gate HP (draft value) the infantry move to the breach |
| C | attack the keep | all hold the keep; ranged on the wall-walk |

Easy uses no stations and sends its siege troops in with the infantry; Normal and Hard use stations and the standoff spot. New `botCommand` column `useStations`: easy false, normal true, hard true. When the portcullis warning sounds, defender bots in the yard retreat through the gate.

Ledger:
- Left, the attackers: stages taken, drawn "1/3", track `trackFill(stages, 3)`.
- Right, the defenders: the current stage's strength, drawn "64%". For the yard and the keep it is 100% minus capture progress; for the gate it is the HP share. Track `trackFill(strength, 100)`. New: this puts capture progress on the bar, which the UI spec keeps off it in Domination, because in Siege the stage is the whole tempo of the match.
- Clock: the attackers' time left.
- Seals A, B and C use the existing sprites: the holder's seal; contested while contested, and for 3 s (draft value) after each blow to the gate, kept as a named presentation constant rather than a sheet number; and the attackers' seal once a stage falls.

## 6. Survival on Hollowwood

Sides: every human and allied bot defends on team 0, one to four banners. Team 1 is the wave director; it has no commander and buys nothing.

The shrine is a capture ring in the court of `hw-moon-shrine`, the piece placed at x 76, z 127, with radius 10 (draft value), owned by the defenders. The court is also the defenders' field base: lineup, respawn, barracks and rout destination. New: the field barracks is marked by the team's banner on a pole like a point flag, with no new building. Point C at the crossing is not used.

Waves:
- A wave set is rows in `waves` and `waveGroups`. Each wave has a timer and wave gold; each group names a troop, a count and a rise site.
- Each rise site shows soul fire and plays a sound 5 s (draft value) before its group rises, and the feed names the site.
- The next wave rises when the current timer runs out, cleared or not. Clearing early buys a longer breather to spend gold.
- A wave is cleared when all its troops are dead; every defending banner then receives the wave gold.
- Counts scale by defending banners, times 1 plus 0.5 (draft value) for each banner after the first, and by tier through the new `botCommand` column `waveScale`: easy 0.75, normal 1.0, hard 1.25 (draft values). Counts round up.
- No more than 60 dead (draft value) stand on the field at once; the rest rise as others fall. This also bounds tick cost.
- The dead carry the Graveborn Rise Again trait at the new `botCommand` column `waveTraitTier`: easy none, normal tier I, hard tier II (draft values).

The "moon" set, used by the campaign and as the Skirmish default. Every number in this table is a draft value.

| Wave | Timer | Gold | The dead |
|---|---|---|---|
| 1 | 90 s | 60 | 8 Risen |
| 2 | 90 s | 60 | 8 Risen, 4 Bone Archers |
| 3 | 100 s | 80 | 10 Risen, 6 Bone Archers |
| 4 | 100 s | 80 | 10 Risen, 6 Bone Archers, 3 Grave Reapers |
| 5 | 110 s | 100 | 12 Risen, 6 Bone Archers, 4 Barrow Stalkers |
| 6 | 120 s | 100 | 12 Risen, 8 Bone Archers, 4 Grave Reapers, 1 Gravecaller |
| 7 | 150 s | 0 | 16 Risen, 8 Bone Archers, 4 Grave Reapers, 4 Barrow Stalkers, 2 Gravecallers |

End: the defenders win when the last wave is cleared, or when its timer runs out with the shrine still theirs. They lose if the dead take the shrine, which takes 20 s (draft value) of dead on the ring and no defender on it. There are no draws.

Gold: wave gold replaces passive and point income, both scaled to 0. Kill gold stays, and starting gold is the normal 160.

Landmarks: four rise sites 50 to 90 blocks (draft value) from the court, on walkable ground, so each group is seen before it arrives: behind the ruined chapel to the north, the ford at the C crossing to the west, the deep wood to the east and the south track. The chapel's cover and sight lines look onto the C crossing, so a group posted there covers the ford's rising, while the north group comes out from behind the chapel itself. The round runs at night: the world clock goes to midnight at setup and back at teardown, and the shrine's soul lanterns light the court.

Morale: the living use the Domination row and rout to the court, so a rout near the shrine is short. Every wave squad carries a RoutGuard for its whole life; the dead never break.

Bots:
- Wave squads attack-move to the shrine and fight what they meet. They ignore `retreatShare` and charge by their tier's rules.
- Allied defender bots hold the shrine ring and spend their wave gold. Hard braces its shieldwall against archers as usual.

Ledger:
- Left, the defenders: the wave, drawn "3/7", track `trackFill(wave, waves)`.
- Right, the dead: how many stand, drawn "12", track `trackFill(standing, risen this wave)`.
- Clock: time to the next wave; in the last wave, its time left.
- One seal, the shrine: the defenders' seal, contested when both sides stand on the ring, the enemy seal while the dead take it. Capture progress stays in the world, as the UI spec rules.

## 7. Banner on Rivergate

Sides: Dawn and Dusk, as in Domination.

Stands and points:
- Dawn's banner stands at A, Mill Hamlet, and Dusk's at C, River Shrine. Neither is a capture point in this mode.
- B, the Trade Crossing, stays a normal point with point income and forward reinforcement but no score. It is the only forward anchor.

Rules:
- Any enemy troop or commander within 1.5 blocks (draft value) of a banner, at its stand or on the ground, picks it up.
- The carrier wears the banner on its head in the owner's colour, glows, never routs and cannot attack. It moves at x0.9 speed (draft value), and a commander cannot sprint while carrying.
- A troop carrier leaves its group's orders and walks to its own stand by `Tactics.route`; the rest of its group keeps its orders. A commander carrier goes where the player goes.
- When a carrier dies, the banner drops where it fell. An enemy touch picks it up again, the owner's touch sends it home at once, and after 20 s untouched (draft value) it returns home.
- A carrier within 3 blocks (draft value) of its own stand scores 1, and the banner returns home.
- Scoring does not need your own banner at home, so a double steal is a race, not a stalemate.
- First to 3 captures (draft value) wins. At 12 minutes (draft value) the higher count wins, and a tie is a draw.

Landmarks:
- Dawn's stand sits in the mill hamlet. The mill loft sees the north bridge deck, and the houses and market give guards cover.
- Dusk's stand sits by the River Shrine and the `rg-shrine` piece. The builder may move it into the shrine court if the Banner sim shows a seat bias, since the hamlet already favours Dawn.
- The customs house and the warehouse cover both approaches to B, where escorts and chasers meet.
- A carry is about 300 blocks and crosses the river once. Tidecallers at trait tier II ford anywhere, which is their Banner answer.

Morale: the Domination row; only the carrier is guarded.

Bots:
- At each decision every bot banner holds a role: raider, which attacks the enemy stand or the dropped enemy banner; guard, which holds its own stand; or holder, which holds B.
- Of a team's banners, the new `botCommand` column `raidShare` sets how many raid, rounded and at least one, with humans counted as raiders first: easy 0.34, normal 0.5, hard 0.5 (draft values). Of the bot banners left, the first guards and the rest hold B. A lone bot banner with no human splits: infantry raid, ranged guard.
- When a team-mate carries, every bot banner within `chaseRange` escorts the carrier. When its own banner is gone, every bot banner within `chaseRange` chases the carrier or the dropped banner. New column `chaseRange`: easy 40, normal 80, hard 120 blocks (draft values).

Ledger:
- Captures per side as plain numbers, tracks `trackFill(captures, 3)`, and the clock as in Domination.
- Seals: a stand shows its owner's seal while the banner is home, the carrier team's seal while it is carried, and contested while it lies on the ground. B is a normal seal.
- New: on the M map the carried banner shows as the carrier team's emblem.

## 8. Feed lines and the briefing card

No new glyphs. Every mode event follows the UI spec's rule that the seal means ours and the struck seal means theirs, in the team's ink; neutral news uses the emblem. The new copy goes through the UI plan's feed_v1; if new kinds change that packet's layout, it bumps.

| Mode | Event | Glyph | Copy |
|---|---|---|---|
| Siege | yard falls | seal or struck seal | "Dawn took the Outer Yard" |
| Siege | gate closing | emblem | "The portcullis drops in 10 s" |
| Siege | gate down | seal or struck seal | "The Iron Gate is breached" |
| Survival | a group rises | struck seal | "The dead rise at the chapel" |
| Survival | wave cleared | seal | "Wave 3 laid to rest, +80 gold" |
| Survival | shrine threatened | struck seal | "The dead are taking the shrine" |
| Banner | banner taken | seal or struck seal | "Dawn has Dusk's banner" |
| Banner | banner home or down | emblem | "Dusk's banner is home" |
| Banner | capture | seal or struck seal | "Dawn scores, 1 to 0" |
| Campaign | bonus kept or lost | emblem | "Bonus kept: break the gate inside six minutes" |

The briefing card in CampaignClient keeps its name, briefing and reason, and gains the mode's rule lines from `modes.howTo` or `howToDefend`, plus the bonus line for campaign battles. New: the War Table shows the same card on the march page, laid out with the 4.4 kit and no new ink drawing.

## 9. Skirmish

Setup sits behind the Skirmish sheet's Change button, in the 4.4 kit's field picker:
- mode first;
- map, from the mode's `maps`; Siege, Survival and Banner have one map each in Phase 4;
- difficulty;
- enemy House: one of the five or Mixed, which picks the `botBanners` row `<house>_<difficulty>` or a mixed row;
- size, as the lobby sizes today; in Survival it is allied banners only, zero to three;
- for Siege only, attack or defend.

Other rules:
- The field card's rules line comes from `modes.ruleLine` instead of the hard-coded "Domination to 300". The size line reads "2 v 2, Normal bots", "2 banners, Normal waves" or "2 v 2, attack, Normal bots".
- You field your own banner, and the barracks sells only troops you own.
- Rewards are normal: completion 80 and victory 40 after 180 s, plus the Houses spec's shards and renown. A held shrine counts as a win.
- Every mode is open from the start, because the vision says any mode.
- Practice gets the same mode picker and keeps its own rules: any troop, practice gold.
- Rooms pass the chosen mode through. Survival seats every human on team 0, which makes it co-op; the rest of friends play is Phase 5.

## 10. Campaign chapter 1, "The Road North"

How campaign battles run from Phase 4, replacing practice seating:
- The player fields their own banner and buys only owned troops.
- The battle seats one bot banner per entry in its `allyBanners` and `enemyBanners`, all at the battle's tier.
- It pays normal rewards and the Houses spec's match shards.
- A battle opens only when every lower-order battle in its chapter is won; otherwise `campaign:<id>` refuses with "Win <name> first".
- Won battles can be replayed for normal rewards. A bonus pays once, only on a win, and is not bound by the 180 s reward rule.
- Battle 1 keeps its lessons and its one-time free roll.

The order is the Houses order, and each new mode arrives with the House the vision ties to it:

| # | Battle | Map | Mode | House met | Sides | Difficulty |
|---|---|---|---|---|---|---|
| 1 | The Training Field | Rivergate | Domination | Gilded Order | you v 1 | Easy |
| 2 | The Moon Shrine | Hollowwood | Survival | Graveborn | you v the waves | Easy |
| 3 | Ironhold Retaken | Ironhold | Siege, attack | Ironclad | 2 v 2 | Normal |
| 4 | Raiders on the River | Rivergate | Banner | Tidecallers | 2 v 2 | Normal |
| 5 | The Frozen Pass | Frostmarch | Domination | Wildmoot | 2 v 2 | Normal |
| 6 | Kingsfield | Kingsfield | Domination, five points | none new | 3 v 3 | Hard |

Each bonus pays 5 shards to the battle's House, as the Houses spec says, and 40 gold (draft value), once. The "By then" gold assumes every earlier battle was won with its bonus, using that draft bonus gold.

**1. The Training Field.** The existing tutorial, verified as F-17.
- Reason, unchanged: "The road west must open before the march begins."
- Briefing: "Turncoat knights hold the river road. Take the crossings and teach your banner to follow you."
- Sides: you against the new `botBanners` row `march1_turncoats`: 10 Order Squires, 6 Order Archers and four empty slots (draft values). The first House you meet is your own, from the other side.
- Bonus `hold_all`: "Hold all three crossings at the same moment." Gilded shards.
- By then: the starter banner of 12 Order Squires and 8 Order Archers, all ten Commons, no gold.

**2. The Moon Shrine.**
- Reason: "The march camps at the moon shrine tonight; there is nowhere else to sleep."
- Briefing: "The old graves of Hollowwood are opening. Hold the shrine until the last of the dead lies still."
- Sides: you alone against the "moon" waves at Easy scale.
- Bonus `lose_at_most` 8 (draft value): "Lose no more than 8 troops." Graveborn shards.
- By then: up to 260 gold, about two rolls, from the free roll, battle 1's pay and its bonus; Gilded shards. The Risen and Bone Archers you are about to fight are already yours to field.

**3. Ironhold Retaken.**
- Reason: "The wagons cannot pass while the dead hold Ironhold."
- Briefing: "The dead took Ironhold while its smiths slept. The Ironclad bring the engines; you bring the swords. Take the yard, break the gate, seize the keep."
- Sides: you and `ironclad_normal`, with its mortar and four ballistas, against `graveborn_easy` and `graveborn_normal` defending.
- Bonus `stage_by` gate 360 s (draft value): "Break the gate inside six minutes." Ironclad shards.
- By then: up to 420 gold; Graveborn shards; Survival met.

**4. Raiders on the River.**
- Reason: "Without its banner the march is just a crowd."
- Briefing: "Tidecaller raiders have come up the river for the march's banner. Guard yours and carry theirs home."
- Sides: you and `ironclad_easy` against `tidecallers_easy` and `tidecallers_normal`. The Ironclad take the guard role, so you raid.
- Bonus `carry_self`: "Carry their banner home yourself." Tidecallers shards.
- By then: up to 580 gold; Ironclad shards; Siege met. The Hearthguard and Arbalists you own can now stand in your banner.

**5. The Frozen Pass.**
- Reason: "The pass is the only way north before the snow."
- Briefing: "The Wildmoot let no host through Frostmarch that has not bled for it. Take the pass before the snow closes it."
- Sides: you and `ironclad_normal` against two `wildmoot_normal`.
- Bonus `win_within` 600 s (draft value): "Take the pass inside ten minutes." Wildmoot shards.
- By then: up to 740 gold; Tidecallers shards; Banner met.

**6. Kingsfield.**
- Reason: "Every road meets at Kingsfield, and so does the Hollow King."
- Briefing: "The Hollow King's host has reached Kingsfield. The Houses you met stand with you now. Break his host and the road is yours."
- Sides: you, `ironclad_normal` and `wildmoot_normal` against `graveborn_hard`, which holds the Hollow King, `mix_gilded_graveborn_normal`, the turncoats returned, and `graveborn_normal`.
- Bonus `kill_unit` gb_hollowking: "Strike down the Hollow King with your own banner." The killing blow must come from your squad, read from `lastHitBy`. Graveborn shards.
- By then: up to 900 gold, about nine rolls, so a median player has about one Rare at 12% a roll or has bought one for 300; Wildmoot shards; every House and every mode met.
- No new House: battle 6 is the payoff. Its `house` names the Graveborn so the bonus shards have a home.

The chapter reward:
- When battle 6 is won and every chapter 1 battle is in `battlesWon`, finish grants one Legendary Pact through the Phase 2 rule: `pacts` +1 and `writChapters` gains 1, once per profile.
- Which Legendary: the player's choice of the five, as the approved Houses spec decides: Lord Marshal Aldric, the Hollow King, Brassjaw, Admiral Vael or Old Horn.
- How: the end-of-match report shows the chapter's epilogue and a "Choose your Legendary" button. It opens the recruit page's Pact view, which spends the Pact with Phase 2's `legendary:<house>:pact`. Closing the screen keeps the Pact for later.
- Epilogue: "Kingsfield holds. When the field was searched, the Hollow King's crown was gone." It promises nothing.

The War Table:
- The Campaign sheet's Ride plate reads "Ride to" and the next unwon battle, as today. Once the chapter is done, Ride replays the battle last chosen.
- New: the march page, a 4.4 kit page behind the Campaign sheet's Change button. It shows six battle plaques, won ones with a wax seal, the next in brass and later ones in iron, each with its House glyph, mode name and bonus state, and the Pact plaque at the foot. Choosing a plaque shows its briefing card.
- On screen the campaign reads "The Long March, I: The Road North". The word "Chapter" stays the Founders plaque's.

## 11. Data

New and changed sheets in sheets/design.json:

| Sheet | Status | Columns |
|---|---|---|
| `modes` | new, 4 rows | id, name, maps, roundSeconds, winTarget, scoreInterval, passiveScale, pointScale, morale, timeout, ruleLine, howTo, howToDefend |
| `modeSites` | new | id, mode, map, kind, name, team, stage, x, y, z, radius, seconds, hp, piece |
| `modeRules` | new, 1 row | siegeStageBonus, portcullisSeconds, breachShare, survivalPerBanner, riseWarning, maxDeadAlive, carrierSpeed, pickupRadius, standRadius, dropReturnSeconds |
| `waves` | new | id, set, wave, seconds, gold |
| `waveGroups` | new | id, wave, troop, count, site |
| `bonusObjectives` | new, 6 rows | id, kind, value, target, shards, gold, text |
| `chapters` | new, 1 row | id, name, pact, epilogue |
| `campaignBattles` | changed, 6 rows | `size` goes; adds mode, house, allyBanners, enemyBanners, side, waveSet, bonus |
| `botBanners` | one new row | `march1_turncoats` |
| `morale` | new column and row | column wallRoutShare; row `siege` |
| `botCommand` | new columns | useStations, raidShare, chaseRange, waveScale, waveTraitTier |
| `rules` | changed | winScore, scoreInterval and roundSeconds move to `modes` |

`modes` rows; passiveScale and pointScale multiply the economy sheet's passive and point gold:

| id | maps | roundSeconds | winTarget | scoreInterval | passive, point | morale | timeout |
|---|---|---|---|---|---|---|---|
| domination | 0,1,2,3,4 | 900 | 300 | 80 | 1.0, 1.0 | domination | score |
| siege | 1 | 600 (draft value) | 3, the stages | 0 | 1.0, 0.0 | siege | team1 |
| survival | 3 | 0, set by the waves | 0, set by the waves | 0 | 0.0, 0.0 | domination | team0 |
| banner | 0 | 720 (draft value) | 3 (draft value) | 0 | 1.0, 1.0 | domination | score |

`ruleLine` holds one `{n}` filled with winTarget or the wave count: "Domination to {n}", "Siege: yard, gate, keep", "Survival: {n} waves", "Banner: first to {n}". `howTo` and `howToDefend` hold up to three lines split by "|". Siege's `howTo` reads "Take the Outer Yard. | Break the gate; siege troops strike it four times harder. | Seize the keep before the clock runs out." and its `howToDefend` reads "Hold the yard, then the gate, then the keep. | The walls steady your troops while the gate stands. | When the clock runs out, Ironhold is yours." The other modes repeat `howTo` in `howToDefend`.

`modeSites` kinds are `stage`, `gate`, `ward`, `base`, `station`, `walls`, `standoff`, `shrine`, `rise` and `stand`: the sites of sections 5 to 7, about twelve rows for Siege, six for Survival and three for Banner. The gate row carries the HP and the intact piece; ward rows carry their piece.

`modeRules`, one row, with the values set in sections 5 to 7: siegeStageBonus 180, portcullisSeconds 10, breachShare 0.25, survivalPerBanner 0.5, riseWarning 5, maxDeadAlive 60, carrierSpeed 0.9, pickupRadius 1.5, standRadius 3.0, dropReturnSeconds 20 (all draft values).

`morale`: wallRoutShare is 0.25 on the `domination` row, equal to its routShare, and 0.15 (draft value) on the new `siege` row, which otherwise copies `domination`.

`botCommand` new columns, for easy, normal and hard: useStations false, true, true; raidShare 0.34, 0.5, 0.5; chaseRange 40, 80, 120; waveScale 0.75, 1.0, 1.25; waveTraitTier 0, 1, 2 (all draft values, as in sections 5 to 7).

`campaignBattles` rows. Order 1 to 6 and chapter 1; the banner cells are comma lists, and "none" means empty:

| id | map | mode | house | allyBanners | enemyBanners | difficulty | side | waveSet |
|---|---|---|---|---|---|---|---|---|
| march-1 | 0 | domination | gilded | none | march1_turncoats | easy | none | none |
| march-2 | 3 | survival | graveborn | none | none | easy | none | moon |
| march-3 | 1 | siege | ironclad | ironclad_normal | graveborn_easy, graveborn_normal | normal | attack | none |
| march-4 | 0 | banner | tidecallers | ironclad_easy | tidecallers_easy, tidecallers_normal | normal | none | none |
| march-5 | 2 | domination | wildmoot | ironclad_normal | wildmoot_normal, wildmoot_normal | normal | none | none |
| march-6 | 4 | domination | graveborn | ironclad_normal, wildmoot_normal | graveborn_hard, mix_gilded_graveborn_normal, graveborn_normal | hard | none | none |

`bonusObjectives`: one row per battle with the kind and value from section 10. `target` is "gate" for `stage_by`, "gb_hollowking" for `kill_unit` and "none" otherwise; shards are 5, from the Houses spec; gold is 40 (draft value). The closed list of kinds is `hold_all`, `lose_at_most`, `stage_by`, `carry_self`, `win_within` and `kill_unit`. Each kind is one tracker on the round state; a later chapter adds a kind by adding a tracker.

`chapters`: id 1, name "The Road North", pact true, and the epilogue from section 10.

New preflight checks:
- every `modes.maps` entry is a map index;
- every `campaignBattles` mode exists and hosts the battle's map, its difficulty is a `botCommand` id, its banners are `botBanners` ids, its waveSet exists when the mode is survival, and its bonus exists;
- every `waveGroups.site` is a rise site, and every `modeSites` piece is a known template.

These close the parked Long March leftovers on unchecked difficulty and map, and removing `size` closes the third.

The economy model sheet is rerun with the bonus gold and the campaign's normal rewards, as the Houses spec requires for any new gold source. Chapter 1 now pays up to 1060 gold to a player who wins every battle and bonus, so the model's "first Rare" moves inside the chapter.

## 12. Save state

Profile schema 3:

| Field | Status | Meaning |
|---|---|---|
| `battlesWon` | new, a set of strings | `campaignBattles` ids won at least once |
| `bonusClaimed` | new, a set of strings | `bonusObjectives` ids already paid |
| `writChapters` | from Phase 2 | chapters whose Pact was granted: the chapter claim |
| `campaignCleared` | from Phase 1 | kept and now derived: the highest order with every lower order won; the Ride plate and CampWorld.spawn still read it |

At finish, before returnLobby, in this order:
1. On a win in a campaign battle, add its id to `battlesWon` and recompute `campaignCleared`.
2. Read the round's bonus tracker. If the bonus was kept and its id is not in `bonusClaimed`, add the id, the shards to the battle's House and the bonus gold.
3. If every row of the chapter is in `battlesWon` and `writChapters` lacks the chapter, grant the Pact.
4. Write it all in the one atomic save that already carries gold, with the Phase 1 .bak and atomic move. A failed save rolls all of it back in memory.
5. The report JSON gains `battleMode`, the stage, wave or capture count, `bonus` with id, kept, shards and gold, and `chapter` with done and pact.
6. returnLobby then clears `activeBattle` and the mode state, as it does today.

A round that ends without finish, because everyone left or the server stopped, records nothing, as today.

Migration from schema 2 to 3, in `ProfileMigration` with its standalone check:
- `battlesWon` gets the ids of chapter rows whose order is at most `campaignCleared`;
- `bonusClaimed` starts empty, since no bonus existed before;
- `pacts` and `writChapters` stay as they are, which is question 6;
- the v2 file is kept once as `<uuid>.v2.json`, as Phase 2 keeps v1.

## 13. Harness and gates

Phase4Checks lives in test-tools/, runs with GRAVES_PHASE4_CHECKS=<suite>, follows the Phase1Checks shape of report, shot, server, skirmish, Suite and finish, and joins the release_config forbidden tuple.

| Suite | Checks |
|---|---|
| modes | a mode on a map it does not host is refused; the field card line comes from the sheet; the `battleMode` JSON per mode; Domination unchanged, with the Phase 1 suites passing |
| siege | setup with wards, open gate and barred keep; the yard falls with the time bonus, the warning and `gate_intact` 10 s later, and a unit in the passage is moved out; a ballista strikes the gate at x4.0 and a knight at x0.25, and 0 HP opens the breach and the keep; keep taken goes to team 0 and time-out to team 1; wall morale at 15% inside, 25% outside and 25% after the breach; a Bone Archer reaches every station within 600 ticks (draft value); teardown and a simulated crash both restore the gate, wards and flags; screenshots of the shut gate under fire, the breach and the ledger |
| survival | groups rise at their sites after the warning; counts for one and four banners and for Easy and Hard match the formula, and the alive cap holds; wave gold on clear and no passive income; the dead never rout; shrine taken is a loss, the last wave cleared or timed out is a win; the shrine barracks sells; night at setup and day after; `tickCost` at four banners logged beside a 4 v 4 Domination round; screenshots of a rising and the ledger |
| banner | pickup by a troop and by a commander; the carrier glows, wears the banner, never routs, never attacks and moves at x0.9; drop on death, owner touch returns, and the 20 s return; scoring at your own stand, also with both banners out; 3 captures win and a tied time-out draws; a bot raider picks up and carries, and guards chase within `chaseRange`; screenshots |
| skirmish | one solo start per mode with normal rewards; the enemy House picks the right `botBanners` row; Siege defend seats the player on team 1; Practice keeps practice gold |
| march | `p4-march-1-start` to `p4-march-6-start` check map, mode, banners, tier, side and waves; each battle's bonus kind driven once to kept and once to lost; `p4-march-lock` refuses battle 3 before battle 2 is won; a replay pays normal gold and no bonus; battle 6 with all won grants the Pact once, and a replay grants nothing; the p1-tutorial suite still passes with the new enemy row |
| save | the profile file on disk holds `battlesWon`, `bonusClaimed` and the gold before returnLobby runs, and a forced save failure rolls back all three; a relog shows them; a schema 2 fixture with campaignCleared 1 and writChapters 1 migrates to `battlesWon` march-1 with the Pact untouched and one backup |
| hud | ledger screenshots per mode, and one per new feed line |

Bot-vs-bot: BotSimulation gains `gravesandglory.simMode`, and results.csv gains the stage reached, the wave reached and the captures. One run per mode, 10 matches each (draft value), Normal against Normal on the Phase 2 `simBanner` rows:
- Siege: attackers win 3 to 7 of 10 (draft value), and no match ends with the gate untouched.
- Survival: one Normal bot banner defends, reaches wave 4 in at least 8 of 10 (draft value) and holds the shrine in 2 to 8 of 10 (draft value).
- Banner: at least one capture in 8 of 10 (draft value), and no carrier stands still for 30 s (draft value).
- Each run logs the Dawn seat's share, because Siege and Banner inherit the known seat bias.

The march run: for each battle, a Normal bot plays in the player's seat with the banner its "By then" line expects, and must win at least 2 of 5 (draft value), so no battle reaches Kort unwinnable.

Kort's gate: a tester subagent reviews the screenshot set first. Then Kort plays a fresh profile through all six battles in one sitting, about 90 to 120 minutes (draft value), and says whether he wants chapter 2. That answer is the pass mark.

## 14. Left to the builder

- The shape of the mode object, and where the bonus trackers live.
- Exact site coordinates within the rules above: the yard ring between curtains and gate, the keep ring in the west court, the donjon base, ward placement where the gate wall's ends meet dry ground, stations that mob navigation can reach, the rise sites, and the Dusk stand beside or inside the shrine court.
- The look of `gate_intact` and the ward, through the Phase 3 pipeline and its studio screenshots.
- How units in the passage are moved when the portcullis drops, and how the crash marker is stored.
- Whether siege troops need a "strike the structure" assignment, or the Phase 2 structure targeting already does it.
- The exact snapshot keys, the feed wording within the lines above, the march page layout within the 4.4 kit, and the order of the Skirmish pickers.
- The wave group rows behind the moon table, and every draft value, tuned through the sims and the gate.

## Calls made in this draft

- The battle order is the Houses order, and each new mode arrives with the House the vision ties to it: Survival with the Graveborn, Siege with the Ironclad, and Banner with the Tidecallers on the river map.
- Battle 1's enemy becomes Commons-only Gilded turncoats, so the Graveborn first appear as the rising dead, and its briefing drops "Dusk raiders".
- Campaign battles stop running as practice: own banner, normal rewards, allies and enemies seated, an order lock, replays allowed, and a bonus paid once and only on a win.
- Bonus objectives pay the Houses spec's 5 shards and 40 gold (draft value), because the vision lists bonus objectives as a gold source; the economy model is rerun.
- Siege: the gate stands open during the yard stage and shuts when the yard falls; two ward walls; stages lock; time bonuses; no draws; no overhangs.
- Survival: wave gold replaces passive and point income; the barracks sits at the shrine; the round runs at night; the dead never rout; it is built and tested for one to four banners.
- Banner: stands at A and C, B is the only anchor, and scoring does not need your own banner at home.
- The game mode is `battleMode` in code with a `battlemode:` command. Mode state rides in the snapshot JSON and the existing score and point fields, so state_v6 keeps its layout.
- winScore, scoreInterval and roundSeconds move from `rules` to `modes`.
- Skirmish opens every mode from the start and offers both Siege sides, and Practice gets the mode picker.
- On screen the campaign is "The Long March, I: The Road North"; "Chapter" stays the Founders plaque.
- The campaign stays solo.
- Practice fields troops you do not own through an uncapped practice barracks: your own banner is the starting lineup, every troop is buyable at star 1, stage 1, and nothing is saved to the profile. Unowned troops are never written into the saved banner, because repack would strip them. (Builder check, 2026-10-18.)
