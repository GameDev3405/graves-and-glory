# Commander gear: plan index

Spec: `docs/superpowers/specs/2026-11-03-commander-gear-design.md` (Kort approved 2026-11-03). The spec builds in parts (a) to (g). Each part gets its own plan, written when the part before it lands, so it can name the real interfaces. Gates: Kort's approval covers the direction of the save (v4), protocol, gameplay and balance-reopen gates. The numbers and the art still go back to him at their own reviews.

| Order | Plan | Spec parts | Needs | Status |
|---|---|---|---|---|
| 1 | `2026-11-03-gear-foundation.md` | (a) data, save v4, gear sync; (b) Forge marks payout and blacksmith server logic | nothing | written |
| 2 | `gear-screen` (future) | (c) gear screen, blacksmith NPC station in `campStations` and `CampWorld.npc`, the Forge tab and reveal, tooltips, the key that opens it in camp | 1 | to write |
| 3 | `gear-combat` (future) | (d) `equipPlayer` uses the equipped gear, gear stats, set bonuses and perks replace `Commander.applyPlayer`, relics on keys 1 to 3 through the class-active key path, class removal, and the audit of every `profile.commander` reader | 1 | to write |
| 4 | `gear-bots` (future) | (e) bot loadouts at the lobby's average human gear power (`Gear.averagePower`), and bot relic use | 1, 3 | to write |
| 5 | `gear-art` (future, own reviews) | (f) 4 sets x 4 pieces x 5 rarity looks, 5 weapon families, about 12 relic icons, grid icons, the commander render, and the greatweapon, staff and banner clips (the weapon faces the move) | item and relic ids from plan 1, Task 1 | to write |
| 6 | `gear-balance` (future, with Kort) | (g) hero duels (Legendary beats 3, loses to 5; Common beats 1, loses to 3), sheet numbers, campaign tuning for a Rare commander | 3, 4, enough of 5 | to write |

## What can run in parallel
- **Art (f)** runs beside everything else from the start. It needs only the stable ids from plan 1, Task 1. Kort approves the draft names before the art is final.
- **Screen (c) and combat (d)** both start once plan 1 is merged, and can run at the same time in separate worktrees. Both touch `Campaign.java` (c: station and screen hooks; d: `equipPlayer` and class removal). Keep one writer per file and merge one before the other rebases.
- **Bots (e)** waits for (d), because relic use needs the relic effects.
- **Balance (g)** comes last.
- **Matchmaking** (ranked and unranked) is its own brainstorm and spec, outside this index. It reads `Profile.gearPower` and `Gear.averagePower` from plan 1. Update PLAN-TO-1.0.md lines 20, 36 and 392 when that spec is written (spec, Dependencies 3).

## Text and docs (folded into the parts)
Item, perk and relic text, and the blacksmith lines and tooltips, go in (c). The how-to-play page and the Discord roadmap post go in (c), after (d) settles the relic text. QA rows for class removal go in (d).
