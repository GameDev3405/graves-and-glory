# Gear and troop upkeep polish: design

Date: 2026-11-10. Author: polish lead. Status: approved in outline by Kort on 2026-11-10 (audit items G2 to G7 and T2 to T7, `docs/reports/polish-audit-2026-11-10.md`); his answers are marked "Kort". Evidence for every "today" statement is in the audit. G1, T1, T3 and the text fixes are already built on `polish/safe-fixes` and are not repeated here.

Timing: everything here changes rules or the profile save, except where marked "screen only". The save changes are made in the same migration as the army economy spec's presets (`2026-11-09-army-economy-design.md`), after the port, so the profile schema changes once.

## Part A: gear

### A1. Durability is removed (G6, Kort)

- Pieces no longer wear. `Forge.wear`, the durability bar on icons, the Durability tooltip line, the broken state, "Repair all" and the `forge:repair` actions go.
- Save: `Piece.durability` is dropped on load. `repairGold` and `maxDurability` leave the sheet.
- Gold that used to go to repairs has no replacement sink here. If collection gold piles up after this, that is a tuning matter for the Forge prices, not a reason to keep the chore.

### A2. Gear power gets a meaning (G2, Kort: all four)

Gear power stays the sum of the worn pieces' rarity power (`gearRarities.power`, 1 to 5; six slots, so 6 to 30). Relics do not count. It is used in four places:

1. **Bots gear up to match the room.** At match start each bot commander is given a full worn set whose power is the average of the human commanders' power in the room, rounded down, built from the set that fits the bot's role, plus relics by its role (one relic per 10 power, so 0 to 3). Bot difficulty shifts it: Easy −4, Normal 0, Hard +2, clamped to 6 to 30. This replaces "bots wear nothing" (`GearCombat.java:54-57`). Bot use of relics is the AI lead's (their report lists that bots never use them); until that lands bots wear relics without firing them.
2. **Campaign battles recommend a power.** Each row in `campaignBattles` gets `recommendedPower`. The campaign sheet on the home page and the battle's card show it beside the player's own, coloured: at or above is plain, 1 to 4 below is amber ("hard"), 5 or more below is red ("very hard"). It is advice only and never blocks entry. Campaign bots are geared to the recommended power, not to the player, so the Forge gives a real edge between chapters. Starting values: chapter 1 battles 6, 8, 10; each later chapter +4, capped at 28.
3. **Shown on room seats.** Each seat in the war council shows the commander's gear power as a small number on the seat plate.
4. **Team balance.** When the host presses the existing shuffle or auto-fill, humans are dealt to sides so the two sides' total power is as close as possible; ties are broken at random. Manual seat picks are never overridden.

The Arsenal's "Gear power" keeps its place and gains a tooltip listing these four uses.

### A3. Gear sources (G3, Kort: two steps)

1. **Relic rolls never repeat.** A relic roll picks only from relics the player does not own. When all twelve are owned the relic roll button is replaced by "All relics owned". The 50% duplicate refund rule goes.
2. **Scrap, and what it buys.** Breaking down a piece gives Scrap (a new profile number) as well as the marks it gives today: Common 1, Uncommon 2, Rare 4, Epic 8, Legendary 16. Scrap buys two things at the Forge:
   - **Aimed roll**: a roll that picks only from one set the player chooses. Costs the normal marks plus 6 Scrap.
   - **Reforge**: raise one owned piece one rarity, keeping its item and re-rolling its quality within the new rarity's range. Costs Scrap by the rarity reached: Uncommon 4, Rare 10, Epic 24, Legendary 60. Reforging to Epic or above does not move the pity counter.
   The numbers are starting values for the balance pass; the intent is that a Legendary of a chosen set takes about three times as many matches by Scrap alone as by luck plus Scrap.

Save: `Profile.scrap`. Sheet: `scrapByRarity`, `aimedRollScrap`, `reforgeScrap`.

### A4. Keeping gear safe and organised (G4, Kort: three free loadouts)

- **Lock**: a toggle on any piece (key L on hover, or the lock corner). A locked piece cannot be broken down, alone or by "Break down all duplicates".
- **Confirm**: breaking down any Uncommon or better piece asks once, naming the piece. Commons break at once as today.
- **Loadouts**: three per profile. Each stores six worn pieces and three relics by piece id, and a name of up to 16 characters (default "Loadout I" to "III"). The Arsenal shows three tabs over the Worn row; clicking a tab wears that loadout. Changing what is worn changes the open loadout. A piece can be in more than one loadout. A piece in any loadout counts as locked.
- **Swap notice**: when wearing a piece removes another (an off-hand with a two-hander), a line under Worn says so for 3 s.
- **Starter kit notice**: when nothing is worn, the Worn row shows "You will fight in the starter kit" in place of empty slots' silence.
- **Speed and perks**: done in the text pass (speed as a percent). Added here, screen only: Common to Rare pieces show their perk greyed with "from Epic".

Save: `Piece.locked`, `Profile.loadouts` (three), `Profile.activeLoadout`.

### A5. Finding the Arsenal, and gear between rounds (G5, Kort)

- Screen only, can be built before the port: Arsenal joins the War Table navigation (Camp, Recruit, Banner, Arsenal, Contracts, Chapter, Friends).
- In the economy spec's pick phase a commander may switch to another of their three loadouts, and nothing else (Kort). The pick screen shows the three loadout names with their power. The swap takes effect when the round starts. The UI lead owns the pick screen; this adds one row to it.

### A6. Stored but unused (G7)

- **Cosmetics** (`Profile.cosmetics`): kept in the save, left undrawn. Drawing them is art work with no design yet; listed as a follow-up, not built here.
- **Relic role label**: removed from the relic icon and tooltip. Bots use `role` to pick relics (A2), so the column stays in the sheet.
- **House sets**: stay untied to Houses. The names are flavour; no rule is added.
- **Every new player starts as a bruiser**: on first entering the Arsenal a new player is offered the three starter kits (bruiser, ranged, support) and keeps one. Players who already have a profile keep what they have.

## Part B: troop upkeep

### B1. Recruit from anywhere (T2, Kort)

- The recruit screen (U) trains, cancels and disbands from anywhere on the field. `accessRadius` no longer gates those actions. Troops appear at the barracks and march to the held point nearest the fight, as today, so distance still costs time.
- The barracks lectern still opens the same screen.
- Refusals that are silent today get a line: queue full ("The barracks queue is full"), nothing to disband ("None of those left to disband").
- Disband: the player picks the troop on its row; the screen asks once ("Disband one Knight? No gold is returned."). `disbandRefund` stays 0.
- A cancelled order's refund shows as "+N gold" on the HUD gold readout.
- Under the economy spec most buying happens in the pick phase; this rule covers buying during a round.

### B2. One name for each way troops grow (T4, Kort)

| Name | What it is | Lasts |
|---|---|---|
| Stars | duplicates of a troop raise it to two and three stars | forever |
| Rank | levels a troop earns by fighting in a match | the match (carried between rounds by the economy spec) |
| Armoury | upgrades bought for a troop type in a match | the match |
| Mastery | the account's long record with a troop type, with titles | forever |

- "Veteran" is no longer the name of a system. Where it is a Rank's own name (a troop at Rank 2 may be called a veteran) it may stay as that rank's title. `Veterans.java` is renamed in the port's cleanup, not before.
- Every player-facing string is moved to these four words. The troop page shows the four as four rows, each with one line on what it gives and how long it lasts.
- Epic and Legendary troops get Rank titles (lore lead writes them).
- Mastery rewards that are not wired (`Campaign.java:636-637`): each is either wired or removed from the Mastery page in the same change, so the page never promises what is not paid. The list goes to Kort with the build.

Mostly text and screens; the Mastery wiring is rules.

### B3. LAST STAND is removed (T5, Kort)

The label leaves the group card. `Morale.java`'s last-stand state and any sound tied to it are removed with it. No morale rule is added. The no-routing rule of 2026-10-22 is unchanged.

### B4. Healing inside a round (T6)

- A troop that has neither dealt nor taken damage for 8 s and stands within 12 blocks of a point its side holds, or of its barracks, recovers 2% of its full health per second.
- Menders keep their own, faster healing; the two do not stack (the larger applies).
- Between rounds the economy spec already returns survivors at full health.
- `automaticReinforcements` and `reinforceSeconds` are deleted from the sheet and the code.

Values: `restSeconds` 8, `restRadius` 12, `restHealPct` 0.02. This is a troop rule, so it is landed with the AI lead.

### B5. Small items (T7)

- The recruit row's ability line shows the troop's own ability text from the sheet, cut to one line.
- A status glossary: hovering a status icon on a group card, or opening the Field page's new "Statuses" list, gives one line per status.
- Banner seats and copies: today owning one copy lets a troop fill any number of banner slots (up to its rarity's `bannerMax`). This spec does not change it; it is listed for Kort as a question because it decides how much collecting matters.

## What it touches

| Part | Change | Owner |
|---|---|---|
| `Gear`, `Forge`, `GearCombat`, `WarTableGear`, `GearView`, `Profile` | durability out, Scrap, aimed roll, reforge, locks, loadouts | polish lead builds, UI lead reviews screens |
| `Campaign` (bots' gear, team balance), `campaignBattles` sheet, `WarTableLobby`, `WarTableHome` | gear power's four uses | polish lead builds, AI lead reviews bot gear |
| `GravesAndGlory` barracks actions, `RecruitmentScreen` | recruit anywhere, refusals, disband | polish lead |
| troop page, strings, `Morale`, `BattleHud` card | names, LAST STAND out | polish lead, lore lead for titles |
| `Tactics` or unit tick | rest healing | AI lead |
| profile save | `scrap`, `locked`, `loadouts`, `activeLoadout`; `durability` dropped | one migration, with the economy spec's presets |

## Order of building

1. Before the port, screen only: Arsenal in the navigation; greyed perks; swap and starter-kit notices; the four names on existing screens; recruit row ability line.
2. After the port, with the preset migration: durability out, locks, loadouts, Scrap with aimed roll and reforge, relic rolls that never repeat.
3. Gear power's four uses (bots' gear first, since practice needs it most).
4. Recruit from anywhere, disband, rest healing, LAST STAND out, Mastery wiring.

## How it is checked

- `gear`, `gear-combat`, `ui-gear` suites extended for locks, loadouts, aimed roll, reforge and the absence of durability.
- A save-migration check: an old profile with worn, damaged gear loads with the same pieces, no durability, three loadouts with the first holding what was worn.
- Bot simulation runs at power 6, 18 and 30 to see that geared bots do not swing the win rate by more than the difficulty setting intends.
- `reinforce` and `ui-recruit` suites for recruiting away from the barracks.

## Open points for Kort

- Should owning more copies of a troop be needed to field more of it on the banner (B5)?
- The starting Scrap prices and recommended powers are guesses to be tuned in play.
