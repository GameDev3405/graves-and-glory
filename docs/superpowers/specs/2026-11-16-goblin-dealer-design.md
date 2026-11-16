# Goblin loot dealer: design (SPEC ONLY)

Date: 2026-11-16. Status: **Approved by Kort 2026-11-16 (economy-dependent parts wait for the economy rework).** Every number below is a starting value for tuning.

> **SPEC ONLY. Do not build.** The army economy is on hold until Kort reworks its spec (`status/DECISIONS-2026-11-15-port.md`). This feature is part of that rework.

- Brief: `Melty/status/brief-lobby-npcs-goblin.md`.
- Companion: `2026-11-16-lobby-npcs-design.md`, which covers the goblin's look, animation and lines (section 4.11) and the shared NPC tech.
- Open decisions are marked **OPEN Qn**, numbered across both specs. Lines marked **(call)** are my proposals.
- All numbers are provisional and would live in `sheets/design.json`.

## 1. Intent

Kort's idea (2026-11-15): a goblin NPC in the lobby takes gold for random chests, with good or weak loot. If the player stakes extra gold, the multiplier board (the drop board) boosts the odds first. The board stays a multiplier; the chest is the actual roll.

## 2. What exists today, and the two conflicts

**Currencies (the war table army rework, built on main).**
- Persistent gold was removed from the profile. `Profile` holds:
  - `troopWrits`, for rolls and chosen troops;
  - `writGold`, a 0 to 99 remainder;
  - `gearShards`, for the Forge;
  - `pacts`.
- Gold now exists only as the in-match purse (`Squad.gold`) and as the lobby muster allowance.
- Kort's rule (2026-11-12): "writs are for the troops, gear shards are for weapons and armors, gold is for in matches and banner in lobby."
- A match pays the old-gold equivalent of about 80 to 120 (`matchGold` 80, `winGold` 40). Through `Profile.payGold` that comes to about 1 writ plus 80 to 120 shards. A gear roll costs 75 shards and a troop roll 1 writ.

**Conflict 1, the currency: decided (Q1, Kort).** "Gold buys a chest" had no lobby gold to spend. Kort's answer:
- **Silver** becomes the in-match currency. It is simple, and it is thrown away after each match. Silver is today's `Squad.gold` purse, renamed.
- **Gold** is what the player carries back to the lobby and spends at the goblin ("goblins love gold"). It is a new persistent profile field, earned at match end.
- **Flagged for the economy rework:**
  - the rename touches every place that shows or names battle gold (HUD, Armoury, barracks, sheet keys, bots);
  - the persistent gold is a save schema change;
  - how much gold a match pays, against writs and shards, is part of the rework;
  - **(call)** the lobby muster allowance, also called gold today, needs a name that does not clash with the new gold.

**The drop board** (`port1211/drop-board` 74fc4399, approved 2026-11-14, not merged).
- On a War Table or Forge roll, the server draws a slot from table A: x10 1%, x5 4%, x3 10%, x2 20%, x1 30%, mirrored. The mean is 2.30.
- The boosted tiers (rare, epic, legendary) are multiplied by **m / mean**, and the lower tiers share the rest (`DropMath.boost`).
- The result is banked at pay time, then the coin's path is replayed.
- The design rule: "the board changes what a roll feels like, not what a hundred rolls give". An x1 is a 0.43x roll.

**Conflict 2, the boost: decided (Q2, Kort).** Under the board's approved rule, staking would not improve the odds on average. Kort: the board gives a real boost only on goblin runs, and an x1 is the same as no stake. War Table and Forge rolls keep the approved board.

## 3. Design

### 3.1 Where and how a buy goes
1. **(Kort)** The goblin's shop and setup sit right next to the multiplier board, so the two stay close. **(call)** That is spot A, east of the hearth court. His cart and chests stand at the foot of the altar court.
2. Clicking him opens his shop:
   - **(Kort, Q5)** a category: armour, weapons or units. Each chest rolls from that category's pool only;
   - **(Kort)** a chest tier: small, medium or large;
   - **(Kort)** a count: 1, 3 or 5 chests;
   - **(Kort)** an optional board stake, where the player chooses how much gold to add (section 3.4).
3. The server checks and takes the whole payment in one step. It then draws the board slot (if staked) and every chest's loot, writes the results to the profile and saves, before anything is shown. After that, everything is a show: a disconnect, a skip or a match start never changes or repeats a result. This is the drop board's rule, reused.
4. **(Kort)** **With a stake:** the board drop happens first, as approved (fly in, aim, drop, flare on the slot). The camera then returns to the goblin, who walks to the chest and opens it, with the boosted odds already applied.
5. **No stake:** the goblin goes straight to the opening.
6. **(Kort)** **The opening.** The goblin plays one of three performances, chosen at random. The performance is the same whatever the count; only the number of results that pop up afterwards changes (1, 3 or 5 cards).
   - **(a) The kick.** He winds up and kicks the chest open: a big, satisfying chest kick with a hold before contact, the lid flying up, and a light burst. Original timing in the spirit of a classic adventure-game chest moment.
   - **(b) The mimic.**
     1. The chest is a mimic. He dives in and it chomps shut.
     2. It shakes and gulps, then spits him back out holding the loot.
     3. **(Kort)** The idea comes from the mimic gag in Frieren, but the art, the mimic design and the motion are original.
   - **(c) The lockpick.**
     1. He kneels and tries a lockpick. It snaps.
     2. He stares at it, pulls an oversized mallet out of his pack and smashes the chest open.
7. **(Kort)** **Skipping.** A player can skip the opening only after he has seen it once.
   - **(call)** Each of the three openings unlocks its own skip after one full viewing, so every player sees all three at least once.
   - This needs a per-profile "seen" record, which is a save schema change.
   - The board part stays skippable as approved.
8. **Watchers.** Watchers within the board's watch radius (48) see the drop and the opening. An x10, or a legendary result, sends one lobby-wide line, as the board does.

### 3.2 Tiers, counts and prices
- **(Kort)** There are three tiers: small, medium and large. Bigger tiers have a better rarity floor and better odds.
- **(Kort)** Buying in bulk is discounted. 3 chests cost about 2.8 x one chest, and 5 chests about 4.5 x one chest.

The single-chest prices and each tier's band odds (the table in 3.3) are **Q9 (decided)**. They are given in match payouts (one match pays about 80 to 120 old-gold equivalent today), because the gold a match pays is set by the economy rework.

| Tier | 1 chest (proposed) | 3 chests (x2.8) | 5 chests (x4.5) | Floor (the worst roll) | Base rare or better |
|---|---|---|---|---|---|
| Small | about 1 match (100) | 280 | 450 | a common unit or basic gear | 5% |
| Medium | about 2.5 matches (250) | 700 | 1,125 | a common unit or basic gear, less often | 7% |
| Large | about 6 matches (600) | 1,680 | 2,700 | uncommon | 9% |

### 3.3 Loot pools
**(Kort)**
- Bad loot is always usable, just weak: the worst roll is a common unit or basic gear. There is no junk.
- **(Q5)** The goblin sells armour, weapons and units. The player picks the category, and each chest rolls from that pool.
- No writs and no relics are in his chests (no strong reason to add them: writs would undercut writ rolls, and relics have no rarity to boost).
- Cosmetics are sold by a separate cosmetics NPC (lobby NPC spec, OPEN Q17), not the goblin.

**Band odds per tier** (Q9 (decided); the same bands for every category):

| Band | Small | Medium | Large |
|---|---|---|---|
| Common | 70 | 45 | 0 |
| Uncommon | 25 | 48 | 91 |
| Rare | 3.8 | 5.2 | 6.6 |
| Epic | 1.0 | 1.5 | 2.0 |
| Legendary | 0.2 | 0.3 | 0.4 |

**What each pool gives:**
- **Armour:** a helm, chest, legs or boots piece of the band's gear rarity, slot drawn evenly, with the Forge's quality roll (0 to 100). **(call)** Large-chest pieces roll quality 50 to 100.
- **Weapons:** a main or off-hand piece of the band's rarity, same rules.
- **Units:** a troop of the player's Houses at the band's rarity.
  - Troops have no uncommon rarity. **(call)** The uncommon band gives a common troop that arrives with one extra copy (a star step), so the Large floor is still better than the Small floor.
  - **(call)** The legendary band gives an epic troop with one extra copy. No Legendary troop and no Pact comes from a chest, because Legendaries come only from Pacts today.

**Notes:**
- **Units from chests are a new progression source.** Troops come only from writ rolls today, and epic units are hard to get (rolls give 2% epics, and an epic cannot be chosen). This needs the progression gate (section 4).
- **Gear from chests competes with the Forge.** The Forge sells the same pools for shards; the goblin sells them for gold, with tiers and the board. The rework sets the relative value.
- **Guardrail (checked by an odds tool like `tools/dropboard_odds.py`):** the gold returned by a batch through duplicate refunds stays at or under 60% of what the batch cost. It is checked for every tier, count and stake, so no batch can be farmed for gold.

### 3.4 The stake and the multiplier
**(Kort)**
- The stake is optional, and the player chooses how much gold to add.
- More stake gives better or more multiplier pegs.
- One board drop boosts every chest in a 3- or 5-chest batch.
- The stake is paid per batch.

**The multiplier rule (Kort, Q2): a raw multiplier for goblin runs only.**
- On slot m, each boosted band (rare, epic, legendary) has its chance multiplied by **m** (not m / mean), and the lower bands share the rest.
- So an x1 equals an unstaked chest, and a stake is never worse than no stake.
- Code: `DropMath.boostRaw` beside `boost`, using the same integer method.
- War Table and Forge rolls keep the approved mean-preserving rule.
- **Cap:** base rare-or-better x the highest multiplier must stay at or under 100%. With x10 as the top peg, that is why the Large tier stops at 9%. Preflight checks this cap for every tier and every stake.

**The stake ladder (Q10 (decided)).** Proposed: three stake levels. Each one is a slot table on the same nine runes.

| Stake | Price (proposed) | Slot table | Mean |
|---|---|---|---|
| Light | +50% of the batch price | Table A bell: x10 1, x5 4, x3 10, x2 20, x1 30 (each side) | 2.30 |
| Heavy | +100% | Table B wild: x10 2, x5 6, x3 12, x2 20, x1 20 | 2.72 |
| All-in | +200% | Table C, no x1: the centre rune becomes x2. x10 2, x5 5, x3 12, x2 20 (each side), centre x2 22 | 2.86 |

- "Better pegs" means the chance moves up the runes. "More pegs" means the All-in relabels the centre rune from x1 to x2, so every rune boosts.
- The rune numerals then have to be drawn per drop by the client. The altar's runes are fixed glow parts today, so this is an art change on the board.
- **Stake priced as a share of the batch price (recommended):** the guardrail holds at every count.
- **Alternative: fixed gold amounts.** Then a 5-chest batch gets five chests boosted for the price of one stake. That makes the 5-batch the best deal by far, and the guardrail must be checked at 5.

Medium tier, rare-or-better per chest, Light stake:

| Slot | Chance | Rare or better |
|---|---|---|
| no stake | | 7% |
| x1 | 30% | 7% |
| x2 | 40% | 14% |
| x3 | 20% | 21% |
| x5 | 8% | 35% |
| x10 | 2% | 70% |
| Light average | | 16.1% |

With the stake as a share of the batch price, rare-or-better per unit of gold against no stake is mean / (1 + stake share):
- Light: 1.53
- Heavy: 1.36
- All-in: 0.95

So the bigger stakes buy swing more than value.

### 3.5 Pity and duplicates
**Pity (Kort, 2026-11-16): a hard counter that players can see.**
- A rare-or-better is guaranteed every N chests.
- Each tier counts chests (not batches) since its last rare-or-better.
- The shop shows the counter per tier, for example "Rare or better in 7".

**N per tier (Q12 (decided)):**

| Tier | Base rare or better | N (proposed) | Chance a run reaches the guarantee without luck |
|---|---|---|---|
| Small | 5% | 25 | 0.95^24, about 29% |
| Medium | 7% | 15 | 0.93^14, about 36% |
| Large | 9% | 10 | 0.91^9, about 43% |

- Chests opened under a stake count the same as unstaked chests.
- **(call)** In a batch, the counter runs chest by chest, so the guaranteed chest can fall inside a 3- or 5-batch.
- **A forced chest** draws the rare, epic and legendary bands at their base proportions. Under a stake, only legendary is then scaled by m / mean against rare and epic, so the result can never exceed the whole.
  - Large at x10 on the Light table: 0.4 / 9 = 4.4%, x 4.35 = about 19%.
- Separate counters per tier stop a player from building pity on small chests and cashing it on a large one.
- The counters are new profile fields, so this is a save schema change.

**Duplicates (Kort, 2026-11-16): a duplicate converts back into partial gold or upgrade shards.**

The rate is **Q13 (decided)**. Proposed: a share of the single-chest price of the tier it came from.

| Band | Refund (proposed) |
|---|---|
| Common | 5% |
| Uncommon | 10% |
| Rare | 30% |
| Epic | 60% |
| Legendary | 100% |

- **(call)** Gear duplicates pay upgrade shards. Unit duplicates pay gold.
- **(call)** What counts as a duplicate:
  - **Gear:** the same item at the same rarity is already owned (the Forge's rule).
  - **Units:** a unit copy still stars its troop while it can, as recruitment does. Only a copy past max stars counts as a duplicate.
- Refunds count toward the 60% value cap in section 3.3. The worst case is Large with every chest a duplicate: about 13% of the price back, well under the cap.
- **(call)** If the inventory cannot hold the batch's gear, the shop refuses before payment, as the Forge does.

### 3.6 What vanilla (plain) clients see
**(Kort, 2026-11-16)**
- Plain players see a villager stand-in.
- They buy through a chest-style menu: tier, count and stake.
- Results show as items or a title message, with no custom animation (no altar cinematic and no opening performance).
- The server draws exactly as for modded players, so the odds and pity are the same.

**(call) Details:**
- **The stand-in:** a baby villager with the swamp skin and no profession, which reads small and green. The server holds it as a baby.
  - Fallback: an adult nitwit with the swamp skin.
- **A staked buy** shows the slot as a title ("x5!"), then the items, with the board sounds sent by name (P2 showed that sounds resolve on plain clients).
- **The menu** shows the pity counters and the odds.
- **Scope:** this is lobby work on top of the narrowed plain scope (which covers battles only at first), so it ships when the plain lobby menus do.

### 3.7 Exploits and guards

| Exploit | Guard |
|---|---|
| Changing a result after seeing it (cancel the aim, disconnect, double click, skip) | Slot and all chest results are drawn and saved at pay time (the drop board rule). The show holds no value. DropFlow already handles a double send |
| The client picks the slot or the multiplier | The server draws the slot. The aim point only seeds the replayed path, and every start point has the table's odds (DropSim) |
| Profit loops | The 60% value cap in section 3.3, for every tier, count and stake |
| Batch abuse (one stake boosting five chests) | The stake is priced as a share of the batch price (Q10), and the guardrail is checked at 5 |
| Pity shopping across tiers | Per-tier counters |
| Duplicate refunds as a profit loop | Refunds count toward the 60% value cap |
| The stake is paid but no board is shown (altar out of view, a match starts) | The multiplier is drawn whenever the stake is paid. The board is only the show, with a result card as the fallback |
| The inventory cannot hold the batch | Refuse before payment |
| Two sessions buying at once on one profile | Pay, draw and bank in one server step on the profile, as rolls do |
| The skip unlock | It is a client convenience over results that are already banked; it cannot change a result |
| Free rolls through debug or test commands | Op-only, and absent from release builds (the release leak guard) |
| Farming the currency with bots or AFK play | The earn rules of Q1's currency apply; this feature adds no new way to earn |
| Alts or trading | No trading exists, profiles are per UUID, and no loot is transferable |
| Real money, binge spending, hidden odds | Q15 (decided): never sold for money nor granted by the Founders pass; band chances and slot tables shown in the shop; **(call)** no daily cap, because the value cap and earn-only currency already limit spending |

## 4. Approval gates when built
- **Currency.** Silver renames the match purse, and gold becomes a new persistent field (a save schema change). Both are part of the economy rework (Q1).
- **Progression.** Units and gear arriving from chests are a new progression source (section 3.3).
- **Save schema.** Gold, the pity counters and the openings already seen all need new save fields.
- **Protocol.** The shop action, the batch reveal and the goblin's board variant need packet changes. The per-drop rune labels need altar art changes.
- **Sounds.** Free-only: CC0, CC-BY, or good generated audio. Kort rates them.
- **Lore.** The goblin's place (Q7). The tier names stay "small, medium, large" in this spec; flavour names wait for the lore lead, and nothing named "giant".

## 5. Decisions

**Decided by Kort (2026-11-16):**
- buying 1, 3 or 5 chests;
- three tiers;
- an optional stake that the player chooses;
- no junk;
- the three openings;
- the shop beside the board, with the board first;
- one drop per batch, with the stake paid per batch;
- bulk prices x2.8 and x4.5;
- skipping only after one viewing;
- the goblin's look (NPC spec 4.11);
- hard pity with a visible counter;
- duplicates converting to partial gold or upgrade shards;
- plain players get a villager, a chest menu and item or title results.

- **Q1:** silver in matches, gold carried to the lobby and spent at the goblin;
- **Q2:** a real boost on goblin runs only, where an x1 equals no stake;
- **Q5:** armour, weapons and units, picked by the player, with no writs, relics or cosmetics.

**Decided in the final round (Kort, 2026-11-16):**
- **Q7:** the goblin is a neutral relic-grubber.
- **Q9:** the proposed prices and band odds are the starting values.
- **Q10:** the stake ladder: Light +50%, Heavy +100%, All-in +200% of the batch price.
- **Q12:** pity at 25, 15 and 10 for small, medium and large, with a separate counter per tier.
- **Q13:** duplicate refunds of 5, 10, 30, 60 and 100% of the tier's single-chest price.
- **Q15:**
  - gold can never be bought with real money, nor come from Founders;
  - the odds are shown;
  - there is no daily cap.

**Nothing in this spec is open.** Building waits for the economy rework, which sets the silver rename, persistent gold and the gold paid per match.
