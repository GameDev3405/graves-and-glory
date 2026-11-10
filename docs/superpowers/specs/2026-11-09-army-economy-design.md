# Army size and match economy: design

Date: 2026-11-09. Author: the coordinator session, from two rounds of questions with Kort.
Status: draft for Kort's approval. Nothing here is built.

Lines marked **(Kort)** are his decisions. Lines marked **(my call)** are mine and are his to overrule.

## 1. Intent

Today every commander marches the same saved banner of 20 slots, free, into a single round. Army building happens before the match and never changes.

Kort wants army size to be "a real strategic choice": a few strong units or many cheap ones, bought with gold, decided again every round, and coordinated with teammates. A hard limit of 20 goes. Fielding more than 20 stays possible but "very, very difficult", and the way to afford it is to win the map. **(Kort)**

The model is a round-based competitive match (his example: Rainbow Six Siege): a short buy phase, a round, then the next buy phase with whatever gold you hold.

This serves what 1.0 is to be remembered for: deep tactics. The economy turns every round into a decision (spend, save, protect what is alive) and gives the snowball a cost: a bigger army is a bigger loss.

## 2. Goals, non-goals, anti-goals

Goals
- A match is best of 5 rounds with an economy that runs across rounds. **(Kort)**
- A pick phase before every round where each commander buys an army with gold. **(Kort)**
- No visible limit on army size; price and training time climb past 20 slots. **(Kort)**
- One system in every mode: player versus player, 3v3, bots, campaign. **(Kort)**
- Everything numeric lives in `sheets/design.json`, so seasons can retune without code.
- Built so ranked play and unit bans can be added in a later season without redoing the pick phase. **(Kort: bans later)**

Non-goals for 1.0
- Ranked queue, unit bans, matchmaking by army value.
- Trading or gifting gold between teammates. **(Kort: no)**
- A persistent army between matches. Losses matter inside a match only. **(Kort)**
- Fixing the existing Domination and Convoy imbalance (separate, still open).

Anti-goals (this fails even if it works)
- A match that is decided after round 2 because the loser can no longer field an army.
- A pick phase that is slow or fiddly: it must be finishable in a few clicks with a preset.
- A server that lags because someone reached 40 units.
- Bots that cannot play the economy, so solo and campaign play feels different from playing people.

## 3. The match

### 3.1 Structure
- A match is best of 5: the first side to win 3 rounds wins. **(Kort)**
- Each round: pick phase (45 seconds **(Kort)**), the 6-second round intro as today, then the round under the mode's normal rules.
- A round ends by the mode's own win condition or its timer. The mode decides the round winner exactly as it decides the battle winner today. Today every mode ends the battle by calling `campaign.finish`, which pays rewards and returns everyone to the lobby; that splits into "round end" (score, round gold, next pick phase) and "match end" (rewards, lobby). Persistent rewards are paid once, at match end.
- Round length: today's `roundSeconds` are per battle: Domination 900 s, Convoy 420 s, Siege 185 s. Siege already fits. **(my call)** Each mode gets a `matchRoundSeconds` value aimed at a match of 20 to 30 minutes; for Domination that means about 4 to 5 minutes instead of 15, which changes how it plays and needs Kort's eye (section 10).
- Sides: Domination is symmetric, so sides stay. Siege (one side always attacks) and Convoy (one side escorts) are not, so in those modes the roles swap every round **(my call)**; who takes which role in a fifth round is open (section 10).
- Draws: a drawn round (possible in Domination) gives nobody a round win and both sides the loss gold. If no side has 3 wins after 5 rounds, the side with more round wins takes the match; if still level, one extra round is played **(my call)**.
- Survival is outside this match system: it has no opposing commander and no clock. It stays a single run with one pick phase at the start; the soft 20 and the price rules apply, and wave gold works as today **(my call)**.
- Teams can be up to 4 commanders (`playersPerTeam`), so everything here must hold for 4v4, not only 3v3.

### 3.2 Gold
One currency: battle gold, the existing in-battle gold. It exists only during a match and is separate from the persistent gold in the profile.

- Start: every commander begins the match with `startGold`. It must buy "a full mixed army with gold left" **(Kort)**. With today's costs a mixed 20-slot army (1 Legendary, 2 Epics, 4 Rares, 8 Commons) is about 600, so the starting value is **700 (my call, to be tuned)**.
- `startGold` replaces today's `startingGold` (160), which is handed out again at every round start; gold is now given once, at match start.
- During a round: income as today (passive trickle, held points, kills). Winning the map is how you get the funding. **(Kort)** Measured today this is roughly 410 to 450 gold per commander in a 5-minute round, which is large next to the round bonuses, so the income rates themselves are part of the tuning.
- End of round: the winner gets `roundWinGold`, the loser gets a smaller `roundLossGold`. **(Kort: small loss bonus.)** Starting values **200 and 120 (my call)**.
- Gold carries over between rounds. **(Kort)** It is lost when the match ends. Bot commanders and their gold persist for the whole match (today they are removed at every round boundary).
- Other things that spend battle gold keep working unchanged: Armoury purchases during a round.
- No transfers between teammates. **(Kort)**

### 3.3 The army
- Survivors of a round carry into the next round at no cost. **(Kort)** They return at full health **(my call)**. Whether they also keep the levels and Armoury tiers they earned is open (section 10); today those are wiped at every round start.
- Dead units are gone. To have them again you buy them again.
- So from round 2 on, the pick phase is mostly about replacing losses and adding to what lived.
- Rarity caps stay: at most 1 Legendary and 2 Epics alive or queued per commander. **(Kort)** Today these caps exist only as limits on the saved banner (`rarities.bannerMax`); they become limits on the living army.
- A commander can buy any troop they own **(Kort)**, within the House rules a room applies today (the fieldable-House filter and the room's House pick stay as they are **(my call)**).
- Order on the field: today a unit's `slot` is its square in the 5-wide banner grid and drives formation rows, shield walls and Hold places. It becomes the unit's position in the commander's army order: survivors keep their order, new units are added at the end, and the pick screen lets the commander reorder. Rows stay 5 wide; armies past 35 slots need the lineup point moved back so deep rows do not start behind it.
- House traits that count slots of a House (thresholds 6, 10, 14) count the living army at round start, with the thresholds unchanged **(my call)**.

### 3.4 Price and the soft 20
- Army size is counted in slots, as today: Legendary 4, Epic 2, others 1 (`slotWeight`). **(Kort)**
- A unit always costs its own `cost`. On top of that, every slot past `softSlots` (20) carries a slot fee that grows with each slot: the fee for slot `20 + k` is `overSlotGold × overSlotBase ^ (k - 1)`.
- Buying a unit that takes the army from `n` slots to `n + w` slots costs its `cost` plus the fees of the slots `n + 1` to `n + w` that are past 20. So the total paid for an army is the sum of its units' costs plus the fees for its slots past 20, whatever order it was bought in. Un-buying refunds exactly what that purchase cost, and when units die the next purchase pays the fee for the slots it actually fills.
- Starting values **(my call, to be tuned)**: `overSlotGold` 30, `overSlotBase` 1.5. That makes slot 21 cost 45 extra, slot 23 about 100, slot 25 about 230 (about 590 in fees to reach 25) and slot 30 about 1,700 (about 5,100 in fees to reach 30).
- Worked example: with 19 slots, an Epic (cost 60, 2 slots) fills slots 20 and 21 and costs 60 + 45 = 105. With 20 slots it fills 21 and 22 and costs 60 + 45 + 67 = 172.
- Slower training: a unit's training time is multiplied by `overTrainBase ^ k`, where `20 + k` is the highest slot it fills; starting value **1.15 (my call)**. The barracks queue stays one unit at a time, as today.
- In the pick phase, units within the first 20 slots are on the field when the round starts. Units past 20 are put in the commander's barracks queue at round start and arrive as they finish training, without the commander having to stand at the barracks **(my call)**. That is how "slower to train" shows in the pick phase.
- There is no visible maximum. **(Kort)** The fees are tuned so that an army past about 30 slots is practically unaffordable. A hidden safety limit, `hardSlots`, exists only to protect the server: starting value **40 slots per commander (my call)**, set properly by the performance test in section 8. The UI never shows it; the buy button simply reads "too costly" there.

Tuning targets for the sims (section 8), not promises:
- Round 1: a mixed army fields about 20 slots with gold left; an all-cheap army reaches about 21 to 23 slots. **(Kort: a cheap army gets "like 20+ units".)**
- Reaching 25 slots needs roughly two won rounds with light losses.
- 30 slots or more happens in fewer than 1 in 100 matches.
- A side that lost rounds 1 and 2 can still field at least 16 slots in round 3. The snowball is wanted **(Kort)**, and so is the small loss bonus **(Kort)**; this target is where the two meet, and if the sims cannot hit it the loss gold or the income rates change, not the rule.

### 3.5 During a round
- The barracks still trains: new units of any owned troop, and replacements. **(Kort)** Today the barracks refuses any troop beyond its count in the saved banner and silently refunds any unit past slot 20; both rules go.
- Prices and training times use the same fees, counted on living plus queued slots. The queue length limit (`maxQueue`, 20) stays.
- Gold not spent is saved for the next pick phase. That is the core decision.
- Disbanding refunds nothing, as today.

## 4. The pick phase

- Opens for every commander at the start of each round, 45 seconds. **(Kort)**
- Shows: your gold, your carried survivors, every troop you own with its current price (already including the over-20 multiplier), your slot count, and a Ready button.
- Presets: the saved banner becomes a preset. **(Kort)** A profile holds exactly one banner today; it gets up to 5 named presets **(my call)**. One click loads a preset: it buys, in the preset's order, everything not already in your army that you can afford, and skips the rest with a note.
- Undo: anything bought in this pick phase can be un-bought for a full refund until you press Ready or the timer ends. **(my call)** Carried survivors cannot be sold.
- Teammates: a side panel shows each teammate's current picks and slot count, updating live, so the team can coordinate. **(Kort)** No chat system is added; players use what Minecraft has.
- Enemy picks are hidden until the round starts. **(Kort)** The server must not send enemy picks to clients before then. Bought units are therefore not spawned in the world during the pick phase (Minecraft would send nearby entities to enemy clients); they spawn when the phase ends. Pick data travels in its own versioned packet, sent per side; the existing camp snapshot is size-limited and is not used for it.
- When every commander is Ready, the phase ends early. When the timer ends, whatever is bought is the army. A commander who bought nothing in round 1 gets their first preset loaded automatically so nobody starts with an empty field **(my call)**.
- Deployment: units appear at the side's spawn area in the order bought. Formation and placement tools work as today.
- The phase is one step in a small sequence owned by the match: `[ban step (empty in 1.0)] -> pick -> deploy -> round`. The empty ban step is the hook for the later season.

## 5. Bots

- A bot commander goes through the same pick phase with the same gold and prices. No free units, no hidden gold.
- It must make three decisions a person makes: what mix to buy (quantity or quality, and what its teammates already have), whether to spend or save this round, and what to reinforce mid-round.
- Difficulty changes how good those decisions are, not the gold.
- The AI lead owns how; this spec only fixes the rule that bots use the real economy. Its design (branch `ai/lead`) gets a section for this.

## 6. Campaign

- Campaign battles use the same system. **(Kort)** Each battle row sets the player's `startGold`, the number of rounds (1 is allowed, so early battles stay short), and the enemy's budget or fixed army.
- Battle 1 keeps a fixed army and no pick phase, and the pick phase is introduced as a taught step in an early battle **(my call)**.
- Chapter 1 landed on 2026-11-09 with fixed banners. Its six battles need budgets and a re-check of the bonus objectives and the ebb. The lore lead confirms no story line depends on "your twenty".
- Rewards after a campaign battle are unchanged (persistent gold, shards, Renown).

## 7. What changes in the game

Rules and data (`sheets/design.json`, generated `Spec.java`)
- `rules.squadSize` becomes `softSlots`. New keys: `startGold`, `roundWinGold`, `roundLossGold`, `overPriceBase`, `overTrainBase`, `hardSlots`, `roundsToWin`, `pickSeconds`, and `matchRoundSeconds` per mode.
- Every place that reads `squadSize` as a hard limit (recruit and reinforcement checks, the training refund, deployment, `RosterMath.bannerSize`, Morale ceilings, Commander, SurvivalMode, the Armoury rows, campaign bot buying) changes to the soft rule. Temporary units (Risen and similar) are indexed after the commander's real army today, at slots at or above `squadSize`; they need their own index range that does not collide with a 30-slot army.
- The fixed troop count per House (2/2/1/1 by rarity, enforced in preflight) is removed: Houses can grow. **(Kort)** Recruitment odds must stay correct when a House has more troops of a rarity. Nobody loses a troop.

Match flow
- A match object that owns rounds, scores, per-commander gold and surviving units, and the phase sequence. Today `startRound` builds everything fresh; it must instead build from the carried army.
- Leaving and rejoining mid-match: a commander who disconnects keeps their gold and army for the match; a bot takes the seat until they return **(my call)**.

Saves
- A profile stores one banner as a fixed 20-entry list today, and repacks anything else at load. Presets become up to 5 named lists of any length up to `hardSlots` worth of slots. The old banner migrates to the first preset unchanged. This is a new profile schema (today's is 6) and a save-format change, and needs Kort's explicit go when it is built.
- A preset is only a shopping list. It never guarantees an army.

Screens
- New: the pick phase screen with the teammate panel.
- Changed: the barracks training list (prices with the multiplier), the army list and HUD (must scroll past 20), the War Table banner page (now a preset editor without a hard 20), the campaign lobby grid, the recruitment label "ARMY n / 20 SLOTS", the round and match scoreboard, an end-of-round summary (gold earned, units lost).
- The UI lead owns the designs; the pick screen needs a mockup for Kort before it is built.

Commands and formations
- Selection, groups and formations must work with 30 or more units for one commander.

Tests
- In-game self-tests and fixtures that assert exactly 20 troops per side or 80 living troops are rewritten around budgets.
- New tests: price curve, carried survivors, round gold, preset loading, hidden enemy picks (a client of side A never receives side B's picks), the rarity caps under the new rules, save migration from a 20-entry banner.

## 8. Performance and balance

- Measured today: 3v3 bot battles at 5 to 6 ms per tick against a 50 ms budget, with pathfinding and collisions scaling with troop count. A dedicated server with six players was never measured.
- Before any curve is final: run 3v3 on a dedicated test server (through `tools/balance_sim.sh` and the check queue) with every commander at 20, 30 and 40 slots. `hardSlots` is set where the server keeps at least half its tick budget free.
- Performance must also be measured at 4v4 (eight commanders), since teams can be four. Balance sims: cheap-swarm against elite armies at equal gold; how often the round-1 winner wins the match (target: clearly more than half, clearly less than always; starting target 60 to 70 percent); how often armies pass 25 and 30 slots.
- Every troop's `cost` is now the main balance lever and gets one pass.

## 9. Order of building (for the plan, not a schedule)

1. Rules and data: the new keys, the soft rule, temporary-unit indexing, Houses can grow.
2. The match object: rounds, score, carried armies, round gold. Playable with today's banner as the round-1 army.
3. Pick phase on the server, driven by commands, with bots buying by a simple rule.
4. Pick phase screen and the changed screens.
5. Presets and the save migration.
6. Bots playing the economy properly (AI lead).
7. Campaign budgets.
8. Performance test, sims, tuning.

When: **my recommendation** is to build after the 1.21.1 port, so it is built once. Kort has not decided this.

## 10. Open for Kort

1. Round length for Domination in a best-of-5 match (section 3.1): about 4 to 5 minutes instead of 15.
2. Build now on 1.20.1, or after the port.
3. Siege and Convoy: roles swap each round (my call). Who attacks or escorts in a fifth round?
4. Do survivors keep the levels and Armoury tiers they earned in earlier rounds? Keeping them rewards protecting units and strengthens the snowball; wiping them is today's behaviour.
5. "Any troop you own" stays within today's House rules for a room (my call). Say so if you meant every troop regardless of House.
6. The starting numbers in sections 3.2 and 3.4 are mine; he may want to set the feel himself (how much gold is left in round 1, how fast the slot fee climbs).
7. Campaign bonus objectives that count fallen troops ("lose fewer than 8") were written for an army of 20; they need new numbers under budgets.

## 11. Left to the builder

Class and packet layout, how the match object is stored, the exact migration code, UI layout within the UI lead's mockup, and the sim harness changes, as long as the rules above hold and every number comes from the sheet.
