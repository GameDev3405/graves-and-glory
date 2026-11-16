# Goblin loot dealer: design (draft, SPEC ONLY)

Date: 2026-11-16. Status: DRAFT for the grill-me interview with Kort. Nothing here is approved or built.

> **SPEC ONLY. Do not build.** The army economy is on hold until Kort reworks its spec (`status/DECISIONS-2026-11-15-port.md`). This feature is part of that rework.

- Brief: `Melty/status/brief-lobby-npcs-goblin.md`.
- Companion: `2026-11-16-lobby-npcs-design.md`, which covers the goblin's look, animation and lines (section 4.11) and the shared NPC tech.
- Open decisions are marked **OPEN Qn**, numbered across both specs. Lines marked **(call)** are my proposals.
- All numbers are provisional and would live in `sheets/design.json`.

## 1. Intent (Kort's idea, 2026-11-15)

A goblin NPC in the lobby takes gold for a random chest, which can hold good or bad loot. If the player pays extra, the goblin takes him to the multiplier board (the drop board), which boosts his odds. The board stays a multiplier; the chest is the actual roll.

## 2. What exists today, and the two conflicts

**Currencies (the war table army rework, built on main).**
- Persistent gold was removed from the profile. `Profile` holds `troopWrits` (rolls and chosen troops), `writGold` (a 0 to 99 remainder), `gearShards` (the Forge) and `pacts`. Gold now exists only as the in-match purse (`Squad.gold`) and as the lobby muster allowance.
- Kort's rule (2026-11-12): "writs are for the troops, gear shards are for weapons and armors, gold is for in matches and banner in lobby."
- A match pays the old-gold equivalent of about 80 to 120 (`matchGold` 80, `winGold` 40). Through `Profile.payGold` that comes to about 1 writ plus 80 to 120 shards. A gear roll costs 75 shards; a troop roll costs 1 writ.

**Conflict 1, the currency (OPEN Q1).** "Gold buys a chest" has no lobby gold to spend today.

**The drop board** (`port1211/drop-board` 74fc4399, approved 2026-11-14, not merged):
1. On a War Table or Forge roll, the server draws a slot from table A: x10 1%, x5 4%, x3 10%, x2 20%, x1 30%, mirrored. The mean is 2.30.
2. The boosted tiers (rare, epic, legendary) are multiplied by **m / mean**, and the lower tiers share what is left (`DropMath.boost`).
3. The result is banked at pay time, then the coin's path is replayed.
4. The design rule is "the board changes what a roll feels like, not what a hundred rolls give". An x1 is a 0.43x roll.

**Conflict 2, the boost (OPEN Q2).** A mean-preserving board boosts nothing on average. Paying extra for it would buy only the swing and the show, not "better odds".

## 3. Proposed design

### 3.1 Flow
1. The goblin stands at the drop board court (spot A, east of the hearth court) **(call)**. The altar is his "old stone".
2. Clicking him opens his shop: three chests (section 3.2), each with two buttons:
   - **Buy:** the price.
   - **Buy and go to the board:** the price plus the board fee.
3. **Buy:** the server checks and takes the payment, then draws the loot at the chest's base table. It saves the profile, then shows the result. The goblin slides the chest forward, the lid pops, the loot rises in its rarity colour, and a sting plays (no board).
4. **Buy and go to the board:**
   - The server takes the price plus the fee, draws the slot from the board table and draws the loot with that slot's multiplier (section 3.4). It saves, then plays the board sequence as approved: fly in, aim, drop, flare.
   - The reveal is the chest opening in the slot instead of a troop rising.
   - The goblin scurries to the altar and beckons (cosmetic).
5. Everything after the save is a show. A disconnect, a skip or a match start never changes or repeats the result. This is the drop board's rule, reused.

### 3.2 Tiers and prices (OPEN Q9)
Prices are given in **match payouts** (one match pays about 80 to 120 old-gold equivalent), so they survive whichever currency Q1 picks.

| Tier | Price | Feel | Base rare-or-better |
|---|---|---|---|
| Rag Sack | about 1 match (100) | Cheap, mostly junk, a small chance of something | 6% |
| Iron-bound Chest | about 2.5 matches (250) | The standard buy | 9% |
| Giant's Coffer (name OPEN in Q7; "giant" may not appear) | about 6 matches (600) | Expensive; its bad results are still useful | 10% |

**(call)** A higher tier raises the floor (what a bust or common result is worth), not the rare-or-better share. The board raises the odds. That keeps one lever per idea, and it is forced anyway: see the cap in section 3.4.

### 3.3 Loot table sketch (OPEN Q5 for contents, OPEN Q11 for bad loot)
Bands carry rarity colours. The boost applies to the boosted set {rare, epic, legendary}; the rest share the remainder.

| Band | Rag Sack | Iron-bound Chest | Giant's Coffer | What it holds (sketch) |
|---|---|---|---|---|
| Bust (bad) | 35 | 20 | 10 | A goblin junk trinket (a bent spoon, a lucky rock, a cosmetic of no value) or 25% of the price back |
| Common | 40 | 40 | 30 | A common gear piece; or shards worth about 40% of a gear roll |
| Uncommon | 19 | 31 | 50 | An uncommon gear piece; 1 troop writ; or a chest-only cosmetic (common) |
| Rare | 4.5 | 6.5 | 7 | A rare gear piece; 3 troop writs; or a random relic |
| Epic | 1.2 | 2 | 2.4 | An epic gear piece; a chosen-troop token (any non-Legendary troop); or a chest-only cosmetic (epic) |
| Legendary | 0.3 | 0.5 | 0.6 | A legendary gear piece; a Pact; or a chest-only cosmetic (legendary) |

**Within each band by tier:**
- The Sack draws the lowest option of each band.
- The Coffer's common and uncommon gear arrives at quality 75 to 100, against 0 to 100 elsewhere.

**Guardrail (checked by a tool like `tools/dropboard_odds.py`):** the expected value of a chest, counted in its own currency, must stay at or under 60% of its price, with and without the board. The count includes refunds, shard and writ payouts, and the breakdown value of gear. So no chest can be farmed for profit.

### 3.4 How the board's multiplier applies (OPEN Q2, OPEN Q10)
**Recommended: a raw multiplier for the goblin only.**
- On slot m, each boosted band's chance is multiplied by **m** (not m / mean), and the lower bands share the rest in their old proportions.
- An x1 equals the plain chest exactly, so paying extra is never worse than not paying. The average rare-or-better is 2.30 x base.
- Code: a `DropMath.boostRaw` beside `boost`, with the same integer method, and `chanceSum` in place of `meanK`.
- War Table and Forge rolls keep the approved mean-preserving rule unchanged.
- The slot table is the same table A, so the board, its odds proof and its physics are reused as they are.

**Cap:** base rare-or-better x 10 must stay at or under 100%. That is why no tier's base exceeds 10%. Preflight checks it, as it checks the board's cap today.

Iron-bound Chest, rare-or-better by slot:

| Slot | Chance | Rare or better |
|---|---|---|
| no board | | 9% |
| x1 | 30% | 9% |
| x2 | 40% | 18% |
| x3 | 20% | 27% |
| x5 | 8% | 45% |
| x10 | 2% | 90% |
| board average | | 20.7% |

**Board fee (OPEN Q10).** Rare-or-better per unit of currency against a plain chest = 2.30 / (1 + fee ratio).

| Fee | Rare-or-better per gold against plain | Effect |
|---|---|---|
| +100% (recommended) | 1.15 x | The board is a slightly better deal and a bigger gamble. The plain chest stays the cheap, low-swing buy |
| +130% | 1.00 x | Break-even: the board sells only the swing |
| +50% | 1.53 x | Nobody buys a plain chest again |

**Alternatives for Q2:**
- (b) Keep the mean-preserving board. The fee then buys only the swing and the show, and an x1 is worse than not paying.
- (c) The fee buys a better base table, and the board stays mean-preserving on top of it.

### 3.5 Pity and duplicates (OPEN Q12, OPEN Q13)
**Pity (recommended: per tier).**
- Each tier counts chests opened since its last rare-or-better.
  - Iron-bound Chest: 15 misses force rare-or-better (with a 9% base, about 1 in 4 runs ever hits it).
  - Giant's Coffer: 10 misses.
  - Rag Sack: no pity.
- Board pulls count the same as plain pulls.
- A forced pull draws within the boosted bands at their base proportions. On the board, the multiplier then shifts epic and legendary against rare by m / mean, the drop board's gear-pity rule. So pity stays fair and the slot still matters.
- Counters per tier stop a player from building pity on cheap sacks and cashing it on a coffer.
- New profile fields mean a **save schema change** and need Kort's explicit go when built.

**Duplicates (recommended: the existing rules for each kind of loot).**
- **Gear:** duplicates are kept and flagged, as in the Forge; breakdown gives shards.
  - If the inventory is full, the shop refuses before payment, as the Forge does.
  - **(call)** An alternative: a full inventory turns gear into its breakdown shards.
- **Troops (tokens):** stars, then shards past max stars, as recruitment does.
- **Relics:** the relic duplicate refund (`relicDuplicatePct`).
- **Chest-only cosmetics:** never duplicated. An owned cosmetic re-draws within its band; when the band's cosmetics are all owned, it pays shards.

### 3.6 What vanilla (plain) clients see (OPEN Q6)
The narrowed plain scope leaves the lobby out of the first plain release.
- **Recommended at first:**
  - the goblin shows as a villager: baby, swamp skin, no profession, so it reads small and green;
  - a click gives one chat line: "The goblin deals only with the Graves and Glory modpack", plus the download link.
- **Later (P4):**
  - a chest menu with the three chests and two buttons each;
  - the server draws the same way;
  - the result shows as a title ("x5!" then the item) and a chat line, with board sounds sent by name (P2.2 showed sounds resolve on plain clients);
  - no altar cinematic, since the board is client-drawn;
  - odds identical to modded clients.
- **Fallback alternatives:**
  - an adult nitwit, swamp skin;
  - a wandering trader, which reads as a dealer but is not a villager and drags llama and trade expectations.
- **Risk:** a baby villager ages. The server must hold its age at baby, or use the adult nitwit instead.

### 3.7 Exploits and guards
| Exploit | Guard |
|---|---|
| Changing the result after seeing it (cancel the aim, disconnect, double click, skip) | Drawn and saved at pay time (drop board rule); the pending show holds no value; double-send already handled by DropFlow |
| Client picks the slot or the multiplier | The server draws the slot; the aim point only seeds the replayed path; every start point has the table's odds (DropSim) |
| Profit loops (chest pays more currency than it costs) | The 60% value cap in 3.3, checked in preflight or by an odds tool for every tier, with and without the board |
| Pity shopping (cheap sacks build pity, then a coffer cashes it) | Pity counters per tier |
| Fee paid but no board shown (altar out of view, match starts) | The multiplier is drawn whenever the fee is paid; the board is only the show, with a result card as fallback (drop board rule) |
| Inventory full | Refuse before payment |
| Two sessions buying at once on one profile | Pay, draw and bank in one server step on the profile, as rolls do |
| Debug or test commands that roll for free | Op-only, and absent from release builds (the release leak guard covers the drop-board suite today) |
| Farming the currency with bots or AFK matches | Q1 decides the currency, and its earn rules carry the existing match payout guards; no new earn source here |
| Alts or trading | No trading exists; profiles are per UUID; nothing in a chest is transferable |
| Real money | OPEN Q15: chests, and the currency that buys them, are never sold for money nor granted by the Founders pass |
| Odds hidden | OPEN Q15: the shop shows each tier's band chances and the board table |

## 4. Approval gates this would hit when built
- A new or changed currency: save schema, and part of the economy rework (Q1).
- Pity counters and owned cosmetics: save schema.
- The shop packets and the board's goblin variant: protocol (new client action and reveal type).
- Sounds: free-only, CC0 or CC-BY or good generated audio; Kort rates them.
- Lore: the goblin's place (Q7), and the tier names (no "giant" if the lore lead says the giants stay unnamed).

## 5. Open decisions in this spec
Each question is in the order the grill asks it:
- **OPEN Q1:** the currency (section 2)
- **OPEN Q2:** what the extra payment buys (3.4)
- **OPEN Q5:** what chests hold (3.3)
- **OPEN Q6:** plain clients (3.6; shared)
- **OPEN Q7:** the goblin in the lore (shared)
- **OPEN Q9:** tiers and prices (3.2)
- **OPEN Q10:** the board fee (3.4)
- **OPEN Q11:** bad loot (3.3)
- **OPEN Q12:** pity (3.5)
- **OPEN Q13:** duplicates (3.5)
- **OPEN Q15:** money, odds disclosure and limits (3.7)

Q3, Q4, Q8, Q14 and Q16 are in the lobby NPC spec.
