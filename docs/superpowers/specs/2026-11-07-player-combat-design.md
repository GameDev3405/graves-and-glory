# Player combat: the Warlord

2026-11-07. Status: draft for Kort's approval. Comes from a question-by-question interview with Kort (the "grill") on 2026-11-07.

## 1. Why

Players should change the outcome of a battle by fighting, not only by giving orders. Today a player who wades in barely matters:

- their hits are weak against troops;
- they have too little health and die too easily;
- the fighting feels like vanilla Minecraft.

What exists today, for reference:

- Vanilla melee. Gear adds health, attack, armour, speed and attack-speed modifiers (`GearCombat`, `GearStats`, one point = 1.0 each).
- Three relics on keys 1 to 3.
- A downed player stands at their base as a spectator for 12 s (`respawnSeconds`). The killer's side gets the commander bounty (3).
- Troops have about 20 to 40 health and deal about 4 to 5 damage a hit. So a player with 20 base health dies in about a second inside a squad of 8.

Kort's best moment is a mix of a duel and turning the tide. In practice:

- An enemy player in the middle of the battle is a real threat who will cut down your units.
- So you go deal with them yourself, or ride to a failing flank and save it with your own sword and the troops following you.

## 2. Goals (Kort's answers)

1. **Warlord power.** A player who plays well can beat a full squad of 8 footmen on their own. Only enemy players and elite troops are a real threat to them.
2. **Vanilla, but heavier.** Keep Minecraft's clicking: no new block, parry, dodge or combo system. Add weight through damage, cleave, knockback, stagger, hit feedback and health. This also suits plain (no-mod) players after the 1.21.1 port.
3. **The tide is turned by kills, followers and gear.** There is no morale and no routing. A player helps by killing, by the units following them, and by the abilities their armour, weapon and relics give.
4. **Death matters through command.** While a player is down, their army is leaderless: troops keep their last order and cannot get new ones. This already holds: `OrderRules` refuses orders while the player is respawning. This design keeps it and makes it visible.
5. **Players must answer players.** Leaving an enemy warlord alone lets them shred your units. This pressure comes from goal 1, not from a separate hunting system. Kort did not pick the "warlord hunt" extras (enemy glow or compass marker, bonus damage against players).

## 3. Anti-goals (what would make it a failure even if it works)

- **Troops stop mattering.** Battles turn into player brawls with armies as scenery.
- **Unfair for new players.** Geared veterans one-shot newcomers, who feel helpless.
- **Bot battles too easy.** In solo, the player steamrolls bot armies alone.

Kort was not worried about "hiding at the back wins", so making front-line fighting mandatory is not a goal.

## 4. Non-goals

- A new melee system: directional attacks, parry timing, dodge rolls or combos.
- Morale, routing or rally auras.
- Enemy-player markers, glow or bonus damage against players.
- Changing relics. Their existing abilities count as part of a warlord's kit.

## 5. The design

### 5.1 Weighty hits (Kort picked)

- **Cleave.** A fully charged swing (vanilla attack cooldown at least 0.9) hits the target and up to 2 more enemy troops in a short arc in front. The extra targets take reduced damage.
- **Stagger and knockback.** A fully charged hit on a troop knocks it back and briefly staggers it: it cannot attack during the stagger.
  - The server side reuses the existing stun (`Unit.stunnedUntil`, as shield bash uses it), so it can be built before `anim/v2-stances` merges.
  - Only the client look (hit_v1 knockback flag bit 4, the "stunned" stance clips) comes from `anim/v2-stances`.
  - Repeated staggers on the same troop have diminishing returns, so a player cannot stun-lock a whole line.
  - Elites and warlords are never staggered. They take the knockback only.
- **Cleave and procs.** Only the main target's hit triggers on-hit perks, lifesteal and relic triggers. The extra cleave targets just take damage.
- **Feedback.**
  - A very short hit-stop on a fully charged hit.
  - A heavier hit sound, with a kill sound on top.
  - Particles that scale with damage.
  - Plain clients get the sound and particles; the hit-stop only needs the mod.
- **A troop-only damage factor.** A multiplier on player damage to army troops. Player-against-player damage stays at gear numbers, so goal 1 does not turn PvP into one-shots.

### 5.2 Warlord body (Kort picked)

- **A bigger health pool.** Warlords get a base health well above vanilla's 20. It is a battle-only modifier, like the gear modifiers, with a fixed id so it never stacks.
- **Troop-hit reduction.** Damage from army troops to a player is reduced by a factor, so 8 footmen take several seconds to bring a warlord down, not one. Damage from players is not reduced.
- **Formations stay deadly** (to protect against "troops stop mattering"). The reduction is weaker or skipped for:
  - troops in the existing `brace` or `wall` stance, hitting the warlord from their front arc;
  - focused archer volleys (several ranged hits within a short window);
  - elite troops: epic rarity and above, as a tuning row.

  Charging a set line alone stays a mistake. Catching troops on the move, flanking or in the open is where the warlord shines.

### 5.3 Fair fights (Kort picked)

- **Gear normalisation in battle.** Inside a battle a player's gear stats are capped to the match's power band. The cap either flattens the top end or scales everyone toward a band average (builder's call, section 7). A newcomer's warlord is still a warlord. Gear keeps its identity: perks, set bonuses and relics still differ. Only raw health, damage and armour are pulled together.
- **A warlord for the bots.** In solo and on bot sides, the bot commander takes the field as a warlord at the match's difficulty.
  - It is a new player-shaped commander body (Kort, 2026-11-07), not a promoted troop.
  - It uses the same health, hit, cleave and troop-hit rules as a player.
  - It is downed and respawns like a player. While it is down, its army is leaderless.
  - Killing it pays the commander bounty. That gives solo play a player-shaped threat, so the player cannot steamroll freely (protects against "bot battles too easy").
  - At low difficulty the bot warlord stays near its army's back.
  - At high difficulty it hunts flanks and the player.

### 5.4 Leaderless army (existing, made visible)

- While a player is down, a HUD line and an action-bar note say that the army is leaderless and show the seconds left.
- The respawn time itself (12 s) is unchanged; whether it should grow is a later balance question.
- Allies already get a feed line when their warlord falls (`FeedEvents.commander`, sent to the fallen player's own team). That stays as it is; enemies get no new message.

## 6. Constraints

- **Gameplay and protocol gates.** Damage, health and AI changes are gameplay changes and need Kort's approval at the plan stage. Any new packet fields go through the existing protocol checks.
- **Plain clients.** Everything except hit-stop must work for a vanilla 1.21.1 client: server-side attributes, sounds and particles only.
- **Timing.** The 1.21.1 port waits for the animation rebuild. The stagger's server side does not wait for `anim/v2-stances`; only its client clips do. Building this before the port means porting it too, so the order (before or after the port) is Kort's call, raised once the plan is ready.
- **Data-driven.** All numbers live in `sheets/design.json` tuning rows, read through `R`. `Spec.java` is generated by `tools/preflight.py` and is never hand-edited.
- **Bot sims.** The balance must be checked with bot sims and a real playtest. Pass checks:
  - a warlord beats 8 footmen in the open in at least most attempts;
  - a warlord who charges a braced spear line alone loses;
  - a newcomer warlord against a veteran warlord wins a fair share of fights.

## 7. Left to the builder

- Exact numbers: the troop damage factor, base health, the troop-hit reduction, cleave targets and falloff, stagger length and its diminishing returns.
  - Starting points come from measuring today's gear totals.
  - The 8-footmen test is the yardstick.
- How the gear cap works (flatten the top or scale toward the band). Prefer the simpler one that passes the newcomer test.
- How the bot warlord picks its targets. Its behaviour hangs off the existing `BOTCOMMAND` difficulty rows (`Squad.tier`), adding to the current bot commander, not replacing it.
- Tuning order: fix the gear-cap band first, then tune base health and troop-hit reduction against the 8-footmen test. The reduction is a flat factor.
- The exact HUD wording for the leaderless state.

## 8. Calls I made (not Kort's answers)

- Cleave only on fully charged swings, up to 2 extra targets.
- Stagger needs diminishing returns.
- The damage factor applies to troops only; player-against-player damage stays at gear numbers.
- Troop-hit reduction is weaker against formations, volleys and elites.
- Gear normalisation keeps perks, sets and relics, and pulls only health, damage and armour together.
- The bot warlord is the answer to "bot battles too easy".
- The leaderless state needs a HUD line; the 12 s respawn is unchanged; no new enemy feed line.
- Stagger reuses the existing server stun; elites and warlords are immune to it.
- Only the main target triggers on-hit perks and relics.
- "Formation" means the existing `brace` and `wall` stances; elite means epic and above.
- The bot warlord builds on the `BOTCOMMAND` difficulty rows.

Note: `Morale` in the code is information-only and makes nothing rout, so it does not conflict with goal 3.
