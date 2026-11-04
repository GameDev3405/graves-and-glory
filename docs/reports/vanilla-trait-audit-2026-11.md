# Vanilla-trait audit, post Set A and arrow-speed change

2026-11-01, branch claude/trait-audit-573141 (cut from main 19c7b4d). Read-only for game code: no stat or rule changed, no Minecraft run made. Builds on `house-balance-vanilla-audit-2026-10-31.md` (mob classes, immunities, hitboxes, knockback resistance; all still true, not repeated). New here: what that report got wrong or did not cover, and the state after Set A and arrows 1.0 / bolts 1.4 / max flight 32.

**Current sim win rates** (orchestrator handoff, 11-01): Graveborn 58.5, Tidecallers 54.9, Ironclad 45.8, Gilded 40.2 (band 40-60). Gilded sits on the edge.

**Method.** Code reading (file:line below), vanilla 1.20.1 formulas from memory (armour: `dmg * (1 - clamp(A - dmg/(2+T/4), A/5, 20)/25)`; hurt cooldown: `timeUntilRegen` 20, and inside the last 10 ticks only the excess over the previous hit lands; knockback 0.4 per accepted hit), and `vanilla-trait-audit-model-2026-11.py` in this folder (arithmetic tables plus a crude blob-fight model; `python vanilla-trait-audit-model-2026-11.py table` from the repo root). The blob model ignores range, speed, abilities and counters, so its win rates are meaningless and are not quoted; only its "damage lost to hurt cooldown" share is used. Vanilla constants were not re-checked against a decompiled jar.

## Headline

1. **The previous audit under-read the hurt cooldown.** It said the 20-tick invulnerability "only bites on same-tick double hits". It is per victim, not per attacker: any hit on a troop within 10 ticks of another accepted hit (from anyone) deals only the excess over that hit, and nothing if it is not larger. Nothing in the mod resets it except the Colossus slam (`Abilities.java:415`); the bleed code already knows the rule (`Statuses.java:38,142,178-181`). In the crude blob model 14-24% of attempted damage is lost to it (Gilded 14-15%, Ironclad 17-18%, Graveborn 16-19%, Tidecallers 22-24%): the cap bites hardest on many cheap hitters and on volleys.
2. **Vanilla armour is a live, undeclared defence layer on every hit**, ranged, magic and bleed included (the `troop` damage type is only in `bypasses_shield`, not `bypasses_armor`). Armour is the worn helmet and chest (`GravesAndGlory.java:994-998`) after the mob's own armour is zeroed (`ArmyMobs.java:56`); the sheet's health is "before armour" by design, but cards show no armour number. Armour cuts small hits much harder than big ones (4 damage into 8 armour loses 24%; 9 loses 14%).
3. **Gilded's low rate is not caused by a vanilla-interaction bug.** Vanilla armour is a small drag on its small-hit offence. Details at the end.

## Table

Severity: High = can move a House several points or contradicts the sheet's intent; Med = real but second order or unmeasured; Low = no effect today.

| # | Trait / stat | Intended effect | What vanilla does | Evidence | Sev |
|---|---|---|---|---|---|
| 1 | Troop melee and arrow damage (`damage`, `cooldown`) | Each blow lands at base x counters x traits (`Combat.resolve`); focus fire adds up | Per-victim hurt cooldown: a blow inside 10 ticks of an accepted hit deals only the excess. Knight 5 dmg per 25 ticks, three attackers on one target mostly overlap. 14-24% of attempted damage lost in the blob model | `Combat.java:91`; reset only at `Abilities.java:415`; rule acknowledged at `Statuses.java:38`; model script | High (unmeasured in game) |
| 2 | Armour (helmet + chest gear) | Sheet: "HP is before worn armour"; counters table is the intended modifier | Armour applies after the mod's multiplier on every hit. Hit into target: Knight 5 into Hearthguard A8 = 3.9 (78%); into Reaper A9 T3 = 3.6 (71%); Archer 4 into A8 = 3.0 (76%); Longbow pierce 3 into A8 = 2.2 (74%); Reaper 7 into Knight = 5.7 (82%); Breaker 8 into Knight = 6.7 (84%); Mortar 9 into Knight = 7.7 (86%). Effective HP at a 5-damage hit: Hearthguard 44 (cost 22), Knight 36 (22), Revenant 28 (17), Raider 28 (18), Longbow 19 (35), Mage 27 (70); Wildmoot, Colossus, Brute, Oldhorn at raw HP | `ArmyMobs.java:56`, `GravesAndGlory.java:994-998`, `data/minecraft/tags/damage_type/bypasses_shield.json`, model `table` | High (hidden cost lever) |
| 3 | Armour vs bleed (1.0 per tick) | 1.0 | 0.70 into A8, 0.61 into A10 T3 (Hollowking, Admiral) | `Statuses.java:180` | Low |
| 4 | Gilded Bulwark (-45% ranged on shield troops) with shield counter C1 | Shield troops resist arrows | Three layers multiply: C1 0.35 x Bulwark 0.55 x armour. A 4-damage arrow into a Knight (A8) lands about 0.54 | `Houses.java:62`, `Combat.java:82-85`, counters C1 | Med (Gilded shield troops near-immune to arrows; hides what Bulwark is worth) |
| 5 | Magic (Hexer, Seer, Mist, Mage; C4 "magic ignores shields") | Bypass shields | Armour still applies: Hexer 8 into Sentinel (A9 T2) = 6.1 (77%). Magic does not ignore armour | same as 2 | Med |
| 6 | Projectile flight (arrow 1.0, bolt 1.4, cap 32, min 2) | Visible lobbed volleys, damage unchanged | Damage is queued and always lands at the landing tick; no vanilla arrow collision, so speed changes latency only. Flight at standing range: Archer 22 ticks (55% of its 40-tick cooldown), Longbow 30 (67%), Bonebow 25 (71%), Stalker 29 (83%), Hunter 20 (56%), Arbalist 24 (40%), Ballista 32 (40%), Mortar 32 (32%), Hexer 14 (29%). A shot is dropped if its target dies or is removed before landing | `Projectiles.java:34-36,54-55,63,80-90`; `BattleMath.travelTicks`; `design.json` combatDiscipline | High for archer-heavy lineups (mod queue, not vanilla physics; made worse by the speed change) |
| 7 | Shots beyond the 32-tick cap | n/a | Ballista 34 and Mortar 36 (up to ~44-47 with Entrench siege range) exceed 32 blocks at speed 1.0, so flight clamps to 32 ticks and long shots fly faster than "1.0". Visual only | `Projectiles.java:35`, `BattleMathCheck.java` | Low |
| 8 | Hit knockback | Not in sheet | Every accepted hit, arrows and excess hits included, knocks the victim 0.4 away from the attacker, so volleys push troops back and slow charges. Scaled by KBR: Colossus 1.0, Brute 0.75, netherite 0.1 each. Only anchored troops cancel it | `Combat.java:91-92`, prior audit s3 | Med |
| 9 | Melee reach (`range` + grace) | Reach in blocks | Feet to feet; hitbox size does not extend reach. Knockback can push a victim beyond `range + grace` between swings | `Tactics.java:129-135` | Low |
| 10 | Shield blocking | Counters table only | Vanilla block bypassed by `bypasses_shield`; counters use `isUsingItem` at the moment a blow resolves, so a shot's flight time now matters (shield state at landing, not at firing) | `Combat.java:63`, `bypasses_shield.json` | Low-Med |
| 11 | Wolf damage (Hound) | Normal damage | Vanilla halves non-player hits; the mod doubles first so net 1.0. Bleed with no attacker is not halved. No leak | `GravesAndGlory.java:447` | Low |
| 12 | Gear tier `armourPct` | "Armour tier" | Only multiplies max health, never the armour attribute | `LevelMath.java:27-29` | Low (naming) |
| 13 | Water and river (Tidecallers ford) | Tidecallers faster in water | No code for drowning or submerged state of other Houses; vanilla land mobs take drowning damage after about 15 s submerged and swim slowly. May be part of the intended river edge, never designed as one | no `setAir` or `isSubmerged` in `src/main/java` | Low (unverified in play) |
| 14 | On-hit effects, immunities, fire, hitboxes, KBR, daylight burn, natural regen | see prior audit | Unchanged and still correct. Mobs are persistent, so no despawn | prior audit; `GravesAndGlory.java:987` | Low |

## Set A and the arrow change

- **Set A did not touch gear, so armour still carries the same weight.** Reaper at 30 HP keeps A9 T3 (about 42 effective against 5-damage hits), Revenant 23 HP A7 is about 28. Effective HP per gold of the cheap armoured infantry (Hearthguard 1.98, Revenant 1.65, Knight 1.63, Raider 1.58) is two to three times the ranged and support units (Longbow 0.55, Mage 0.38, Hexer 0.46, Seer 0.43, Mist 0.48, Mortar 0.51). The spread is not on the cards.
- **The arrow change moved ranged efficiency in a way earlier sims could not show.** Each ranged troop now has 25-83% of its cooldown in flight, and a shot whose target dies first is lost, so overkill waste grew (Bonebow, Stalker, Longbow, Archer most). The post-change rates above already include it; Graveborn and Gilded both lean on ranged.

## Is Gilded's low rate a vanilla interaction?

Mostly no.

1. Gilded husks share KBR, hitbox and immunities with Graveborn's undead (prior audit), so mob class cannot separate them. Neutralising Bulwark lowered Gilded about 4 points (`house-balance-proposal-a-2026-11-01.md`), so the trait helps.
2. Armour mildly works against Gilded's offence. Knight 5, Archer 4 and Longbow 6 (pierce 3) are small hits; into Ironclad, Reaper or Admiral armour they lose 22-30%, while Gilded's own armour (Knight A8, Sentinel A9 T2) only offsets the incoming side. Longbow (35 gold, A4, 18 HP) and Mage (70 gold) are the lowest effective HP per gold in the game. A sheet matter that vanilla armour amplifies, not a bug.
3. Baseline pairings (pre-Set A): Gilded loses worst to Tidecallers (29.7) and Graveborn (28.8). Tidecallers' Raider and Harpooner carry A2 and A1, so armour does not explain the weakest pairing; Riptide slow and Brace spear counters likely do (not vanilla).
4. The hurt cooldown costs Gilded least (14-15% in the model): fewer, larger hitters.

Conclusion: no single vanilla interaction explains Gilded at 40.2. Proposals A and B would size armour and hurt cooldown in the real engine.

## Proposed fixes, for Kort to approve (none applied)

| ID | Proposal | Gameplay change? | Why |
|---|---|---|---|
| A | Measure first: scratch test flags (not shipped) that zero `timeUntilRegen` before each `Combat.resolve` hit, and that add `troop` to `bypasses_armor`; rerun units-r4 (720 fights, seed 7, Set A) for each | No (test only) | Sizes items 1, 2 and 5 in the real engine instead of a blob model |
| B | If A shows a skew, decide the hurt-cooldown rule: (i) set `timeUntilRegen = 0` before a troop blow so each blow lands in full, or (ii) keep it and document it as the cap on focus fire | Yes, needs approval | Sheet damage overstates focus fire by up to 14-24% |
| C | Decide armour: (i) put `troop` in `bypasses_armor` and fold armour into health, (ii) keep it and show an armour number on cards, (iii) leave as is | Yes, needs approval | Armour is the largest hidden lever on effective HP per gold (item 2) |
| D | Put Hexer, Seer, Mist and Mage magic (and the Mortar shell) through armour, or exempt them, by an explicit rule | Yes, needs approval | Magic currently takes the 23% armour cut (item 5) |
| E | Zero KNOCKBACK_RESISTANCE beside the armour zero (prior audit item B) and decide whether arrows knock back | Yes, needs approval | Item 8 |
| F | Retarget or refund a shot whose target died in flight (`Projectiles.java:54-55`) | Yes, needs approval | Item 6; its cost grew with the arrow change |
| G | Show armour and effective HP on the troop card and in balance proposals | No (display) | Item 2 |

Items 7 and 9-14 need no action now.

## Caveats

- No Minecraft or Gradle run was made (other sessions were using the machine). Everything quantitative is arithmetic or a crude model; the 14-24% hurt-cooldown share is a model figure, not a measurement.
- Vanilla 1.20.1 constants are from memory.
- Gilded pairing numbers are pre-Set A; the House rates are post-arrow.
