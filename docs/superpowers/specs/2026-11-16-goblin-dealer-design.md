# Goblin loot dealer: design (draft, SPEC ONLY)

Date: 2026-11-16. Status: DRAFT. Kort answered the first questions of the grill on 2026-11-16 (relayed by the coordinator). Those answers are marked **(Kort)**. The other decisions are still open.

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

**Conflict 1, the currency (OPEN Q1).** "Gold buys a chest" has no lobby gold to spend today. Kort's answers keep saying "gold" (the stake, and duplicate refunds of "partial gold"), so the spec's working assumption is a persistent goblin gold earned at match end. That still needs his confirmation, and it is a save schema change.

**The drop board** (`port1211/drop-board` 74fc4399, approved 2026-11-14, not merged).
- On a War Table or Forge roll, the server draws a slot from table A: x10 1%, x5 4%, x3 10%, x2 20%, x1 30%, mirrored. The mean is 2.30.
- The boosted tiers (rare, epic, legendary) are multiplied by **m / mean**, and the lower tiers share the rest (`DropMath.boost`).
- The result is banked at pay time, then the coin's path is replayed.
- The design rule: "the board changes what a roll feels like, not what a hundred rolls give". An x1 is a 0.43x roll.

**Conflict 2, the boost (OPEN Q2).** Kort's answers (stake buys "better or more multiplier pegs") assume that staking really improves the odds. Under the board's approved rule it does not on average. The goblin needs its own multiplier rule.

## 3. Design

### 3.1 Where and how a buy goes
1. **(Kort)** The goblin's shop and setup sit right next to the multiplier board, so the two stay close. **(call)** That is spot A, east of the hearth court. His cart and chests stand at the foot of the altar court.
2. Clicking him opens his shop:
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

The single-chest prices and each tier's band odds (the table in 3.3) are **OPEN Q9**. They are given in match payouts (one match pays about 80 to 120 old-gold equivalent), so they survive whichever currency Q1 picks.

| Tier | 1 chest (proposed) | 3 chests (x2.8) | 5 chests (x4.5) | Floor (the worst roll) | Base rare or better |
|---|---|---|---|---|---|
| Small | about 1 match (100) | 280 | 450 | a common unit or basic gear | 5% |
| Medium | about 2.5 matches (250) | 700 | 1,125 | a common unit or basic gear, less often | 7% |
| Large | about 6 matches (600) | 1,680 | 2,700 | uncommon | 9% |

### 3.3 Loot table sketch
- **(Kort)** Bad loot is always usable, just weak: the worst roll is a common unit or basic gear. There is no junk.
- The contents of each band are **OPEN Q5**.

| Band | Small | Medium | Large | What it holds (sketch) |
|---|---|---|---|---|
| Common | 70 | 45 | 0 | A common unit (one copy of an owned or new common troop of the player's Houses), or a common gear piece |
| Uncommon | 25 | 48 | 91 | An uncommon gear piece or 1 troop writ |
| Rare | 3.8 | 5.2 | 6.6 | A rare unit, a rare gear piece, 3 troop writs, or a random relic |
| Epic | 1.0 | 1.5 | 2.0 | An epic gear piece, an epic unit, or a chest-only cosmetic (epic) |
| Legendary | 0.2 | 0.3 | 0.4 | A legendary gear piece, or a chest-only cosmetic (legendary) |

**Notes:**
- **Units from chests are a new progression source.** Troops come only from writ rolls today. Epic units in particular are hard to get: rolls give 2% epics, and an epic cannot be chosen. So units in a chest need the progression gate (section 4).
- **(call)** No Legendary troop and no Pact comes from a chest, because Legendaries come only from Pacts today.
- **Guardrail (checked by an odds tool like `tools/dropboard_odds.py`):** the expected value of a batch, counted in its own currency, stays at or under 60% of what the batch cost. It is checked for every tier, every count and every stake, so no batch can be farmed for profit. The value counts writ and shard payouts and the breakdown value of gear.

### 3.4 The stake and the multiplier
**(Kort)**
- The stake is optional, and the player chooses how much gold to add.
- More stake gives better or more multiplier pegs.
- One board drop boosts every chest in a 3- or 5-chest batch.
- The stake is paid per batch.

**The multiplier rule (OPEN Q2). Recommended: a raw multiplier for the goblin only.**
- On slot m, each boosted band (rare, epic, legendary) has its chance multiplied by **m** (not m / mean), and the lower bands share the rest.
- So an x1 equals an unstaked chest, and a stake is never worse than no stake.
- Code: `DropMath.boostRaw` beside `boost`, using the same integer method.
- War Table and Forge rolls keep the approved mean-preserving rule.
- **Cap:** base rare-or-better x the highest multiplier must stay at or under 100%. With x10 as the top peg, that is why the Large tier stops at 9%. Preflight checks this cap for every tier and every stake.

**The stake ladder (OPEN Q10).** Proposed: three stake levels. Each one is a slot table on the same nine runes.

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

**N per tier (OPEN Q12):**

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

The rate is **OPEN Q13**. Proposed: a share of the single-chest price of the tier it came from.

| Band | Refund (proposed) |
|---|---|
| Common | 5% |
| Uncommon | 10% |
| Rare | 30% |
| Epic | 60% |
| Legendary | 100% |

- **(call)** Gear duplicates pay upgrade shards. Unit, relic and cosmetic duplicates pay gold.
- **(call)** What counts as a duplicate:
  - **Gear:** the same item at the same rarity is already owned (the Forge's rule).
  - **Units:** a unit copy still stars its troop while it can, as recruitment does. Only a copy past max stars counts as a duplicate.
  - **Relics and cosmetics:** already owned.
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
| Real money, binge spending, hidden odds | OPEN Q15: never sold for money nor granted by the Founders pass; band chances and slot tables shown in the shop; **(call)** no daily cap, because the value cap and earn-only currency already limit spending |

## 4. Approval gates when built
- **Currency.** A new or changed currency is a save schema change and part of the economy rework (Q1).
- **Progression.** Units, relics and epic gear arriving from chests are a new progression source (section 3.3).
- **Save schema.** Pity counters, the openings already seen, and owned cosmetics all need new save fields.
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

**Open: numbers.**
- **OPEN Q9:** single-chest prices, and each tier's band odds (3.2, 3.3)
- **OPEN Q10:** the stake range and how it is priced (3.4)
- **OPEN Q12:** N per tier (3.5)
- **OPEN Q13:** the duplicate refund rate (3.5)

**Open: still not numbers.** Ask these, or confirm the working assumption:
- **OPEN Q1:** the currency (working assumption: a persistent goblin gold)
- **OPEN Q2:** a raw multiplier for the goblin, against the board's approved mean-preserving rule
- **OPEN Q5:** what each band holds (units, gear, writs, relics, cosmetics)
- **OPEN Q7:** the goblin's place in the lore (his look is decided)
- **OPEN Q15:** never for real money, odds shown, no daily cap

Q6 and Q11 are decided for the goblin. Q6 for the staff NPCs, and Q3, Q4, Q8, Q14 and Q16, are in the lobby NPC spec.
