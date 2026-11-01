# HOUSE-BALANCE: vanilla-trait audit

2026-10-31, branch work/house-audit (cut from main). Analysis only: nothing was tuned, no client was started, nothing was run in game. Vanilla numbers below are Minecraft 1.20.1 values from memory and the mod's own code comments; they were not re-read from a decompiled jar, so confirm any number before acting on it. Every item that touches gameplay is marked "needs Kort approval".

Reference run (docs/reports/2026-10-29-impact-frame-balance.md, on work/anim-rework; found via `git log --all`, e.g. commit 4d3230a): grace 1.0, 720 fights, seed 7. House win rates: Graveborn 81.2, Tidecallers 54.0, Ironclad 35.4, Gilded 26.8. Band 40-60.

## 1. How a vanilla mob reaches combat in this mod

Read from `ArmyMobs.create`, `GravesAndGlory.Battle.spawn`, the `Army*` classes, `Combat.resolve`, `Tactics` and `Projectiles`.

- Each troop is a real vanilla mob subclass (`ArmyHusk`, `ArmyWither`, ...) with `initGoals` cut to a SwimGoal. Tactics moves and swings it. Vanilla melee and ranged goals never run.
- The sheet sets max health, movement speed and follow range. `ArmyMobs.create` zeroes base ARMOR. Gear armour from the sheet's helmet and chest columns is real vanilla armour and is kept (section 4).
- Damage never goes through `tryAttack`. `Combat.resolve` calls `target.damage(source(TROOP), amount)` with the mod's own damage type (`gravesandglory:troop`, listed in the `bypasses_shield` tag). So vanilla on-hit effects (husk Hunger, wither skeleton Wither) never fire, the attacker's ATTACK_DAMAGE and ATTACK_KNOCKBACK attributes are unused, and vanilla shield blocking is bypassed (shields act only through the counters table).
- Because the damage goes through `LivingEntity.damage`, the target's own vanilla defences DO apply: armour and toughness, knockback (fixed 0.4 strength, scaled by the target's KNOCKBACK_RESISTANCE), the 20-tick hurt invulnerability, and the status-effect immunities.
- Melee reach is `squaredDistanceTo` (feet to feet) against `range + meleeReachGrace` (Tactics line 129, `Abilities.range`), not hitbox edges. Ranged and magic hits are queued and always land (`Projectiles.fire`), so no arrow collides with a hitbox. Splashes are centre-distance too (`Abilities.splash`).
- Mobs have no natural regeneration. Healing is only the mod's own `heal()` calls (mend, Houses, Commander), which ignore the undead Regeneration immunity.

## 2. Per-troop table

KBR = base KNOCKBACK_RESISTANCE of the vanilla mob; each netherite gear piece adds 0.1. Hitbox is width x height in blocks.

| Troop | House | Base mob (class) | KBR | Hitbox | Group | Vanilla immunities | Vanilla on-hit | Daylight burn | Overridden by the mod? |
|---|---|---|---|---|---|---|---|---|---|
| knight, archer, sentinel, longbow, mage, gl_marshal | Gilded | Husk (ArmyHusk) | 0 | 0.6 x 1.95 | undead | Poison, Regeneration | Hunger | yes | Burn and water conversion: yes. On-hit: never reached. KBR, hitbox, immunities: no |
| revenant | Graveborn | Zombie (ArmyZombie) | 0 | 0.6 x 1.95 | undead | Poison, Regeneration | none | yes | Burn and Drowned conversion: yes. Rest: no |
| bonebow | Graveborn | Skeleton (ArmySkeleton) | 0 | 0.6 x 1.99 | undead | Poison, Regeneration | none | yes | Burn: yes. Shot type: `updateAttackType` emptied |
| reaper, hexer, gb_hollowking | Graveborn | Wither Skeleton (ArmyWither) | 0 (+0.1 reaper helm, +0.1 hollowking chest) | 0.7 x 2.4 | undead | Poison, Regeneration, Wither effect, fire and lava | Wither 10 s | no (fire immune) | Burn flag set anyway. On-hit: never reached. Hitbox, immunities: no |
| stalker | Graveborn | Stray (ArmyStray) | 0 | 0.6 x 1.99 | undead | Poison, Regeneration, freeze | Slowness arrow | yes | Burn: yes. No vanilla arrow ever lands |
| ic_hearthguard, ic_breaker | Ironclad | Vindicator (ArmyVindicator) | 0 | 0.6 x 1.95 | illager | none | none | no | n/a (goals cut) |
| ic_arbalist, ic_ballista, ic_mortar | Ironclad | Pillager (ArmyPillager) | 0 | 0.6 x 1.95 | illager | none | none | no | n/a |
| ic_colossus | Ironclad | Iron Golem (ArmyGolem) | 1.0 | 1.4 x 2.7 | none | Drowning immune | none | no | No: full knockback immunity leaks |
| tc_raider, tc_harpooner, tc_tideguard, tc_admiral | Tidecallers | Drowned (ArmyDrowned) | 0 (+0.1 admiral chest) | 0.6 x 1.95 | undead | Poison, Regeneration | none | yes | Burn and conversion: yes. Rest: no |
| tc_mist | Tidecallers | Evoker (ArmyEvoker) | 0 | 0.6 x 1.95 | illager | none | none | no | n/a |
| tc_brute | Tidecallers | Ravager (ArmyRavager) | 0.75 | 1.95 x 2.2 | illager | none | none | no | KBR leaks. Its own tick speed pull is overridden |
| wm_hunter, wm_tusk, wm_seer | Wildmoot | Zombified Piglin (ArmyBoarkin) | 0 | 0.6 x 1.95 | undead | Poison, Regeneration, fire and lava | none | no | Burn, conversion, anger: yes |
| wm_hound | Wildmoot | Wolf (ArmyWolf) | 0 | 0.6 x 0.85 | none | none | none | no | Half-damage rule from non-players: yes |
| wm_stalker | Wildmoot | Spider (ArmySpider) | 0 | 1.4 x 0.9 | arthropod | Poison, Wither effect | none | no | No (wall climbing is vanilla) |
| wm_oldhorn | Wildmoot | Polar Bear (ArmyBear) | 0 | 1.4 x 1.4 | none | none | none | no | n/a |

Facts that hold for every troop:

- Attack cooldown: not vanilla. The sheet's `cooldown` column drives `nextAttack` (Tactics line 133). The vanilla 20-tick hurt invulnerability still applies on the target, but every melee cooldown is 22 ticks or longer, so it only bites on same-tick double hits (cleave, slam, bleed). `Abilities.trample` resets it explicitly.
- Natural regeneration: none for any mob.
- Zombie-family reinforcements (hard difficulty plus mob spawning): `ZombieEntity.damage` can spawn them, but spawning goes through `spawnEntity`, which skips `initialize`, so the reinforcement attribute stays 0. Low risk.
- Enchantments: none (`unbreakable()` only sets the Unbreakable tag), so group-based enchant damage does nothing.
- Counters table: `undead` is not a tag in the sheet or Java (only one comment in Statuses.java), so entity group never reaches the counters.

## 3. What the mod leaves un-neutralised

1. Knockback resistance: Colossus 1.0, Brute 0.75, netherite pieces 0.1 each (Reaper, Hollowking, Admiral). Everyone else is 0.
2. Hitbox: Wither Skeleton 0.7 x 2.4 (Reaper, Hexer, Hollowking), Ravager 1.95 x 2.2, Golem 1.4 x 2.7. These change body collision, crowding, path width and `canSee` line of sight (taller eye), not melee reach and not ranged hits.
3. Undead immunities (Poison, Regeneration, Wither): the mod uses no Poison or Regeneration effect, so nothing is lost today. Slowness, the one effect the mod does apply, works on undead.
4. Wither Skeleton fire immunity: the mod lays no fire damage (the Firepot is a mod bleed in `Statuses`), so no effect today.
5. Gear armour (helmet and chest columns): not a mob trait, but the same class of hidden lever (section 4).

## 4. Do the leaks explain the outliers?

No. Every husk, zombie, skeleton and drowned troop shares KBR 0 and the same undead rules, so those leaks cannot separate Graveborn (81.2) from Gilded (26.8): Gilded's husks are the same mob family as Graveborn's zombies.

Effective HP of melee troops under the vanilla armour formula against a 6-damage hit (sheet health, gear armour only; my arithmetic, not a measured run):

| Troop | HP | Armour pts | Toughness | Reduction at 6 dmg | Effective HP | Sheet DPS |
|---|---|---|---|---|---|---|
| knight (Gilded) | 28 | 8 | 0 | 20.0% | 35.0 | 4.00 |
| sentinel (Gilded) | 40 | 9 | 2 | 26.4% | 54.3 | 4.29 |
| revenant (Graveborn) | 29 | 7 | 0 | 16.0% | 34.5 | 3.64 |
| reaper (Graveborn) | 39 | 9 | 3 | 27.3% | 53.6 | 6.92 |
| hearthguard (Ironclad) | 34 | 8 | 0 | 20.0% | 42.5 | 3.57 |
| tc_raider (Tidecallers) | 28 | 2 | 0 | 1.6% | 28.5 | 4.17 |

Gilded and Graveborn infantry match on effective HP. The Reaper (DPS 6.9, speed 0.33, 39 HP) is a sheet outlier, not a mob leak.

## 5. Ranked suspects

Ranked by plausibility of explaining the gap.

1. Graveborn Rise Again (mod trait, not vanilla). At banner tier III (the simulation default) every fallen non-Legendary troop has a 45% chance to stand again at 30% health. In wipe-to-the-last-troop duels that is a large extra life for Graveborn. Probably the largest Graveborn driver; the size is an estimate and unmeasured.
2. Gilded Bulwark does nothing in melee duels (mod trait). It reduces only arrow, bolt and blight hits on shield troops; Gilded's best melee is a 28 HP, 4 DPS knight. Probably the largest Gilded drag, but a design choice, not a leak.
3. Graveborn sheet stats: Reaper DPS 6.9 at speed 0.33, Bonebow 98.5% and Revenant 86.8% troop win rates, per the reference report.
4. Wither Skeleton hitbox (0.7 x 2.4, taller eye): second order; may favour Reaper, Hexer and Hollowking slightly through crowding and line of sight. Unmeasured.
5. Knockback resistance: Colossus and Brute only, plus a small netherite 0.1 on Reaper and Hollowking. A target knocked beyond reach plus grace dodges an impact-frame blow, so resistance is a small edge for those troops. It cannot explain a House-wide gap.
6. Undead immunities, fire immunity, vanilla on-hit effects: no effect today. Listed so a future ability author knows.

## 6. Proposed changes (none applied)

| # | Change | Touches gameplay? |
|---|---|---|
| A | Run duels with Rise Again forced off (`Houses.forceRise=false`) and with Bulwark neutralised, to separate trait effects from sheet stats before touching anything. A test configuration only. | No |
| B | Zero KNOCKBACK_RESISTANCE (base and gear modifiers) in `ArmyMobs.create`, beside the armour zero, so only the sheet decides who is shoved. | Yes, needs Kort approval (Colossus and Brute become pushable) |
| C | Normalise biped hitboxes (mainly the wither skeleton 0.7 x 2.4). | Yes, needs Kort approval (also touches renderer and model alignment) |
| D | Override `canHaveStatusEffect` on undead classes so a future Poison or Regeneration ability works on every House. | Needs Kort approval when such an ability is added; no effect today |
| E | Retune the Graveborn sheet (Reaper, Bonebow, Revenant) and the Gilded sheet or Bulwark value. | Yes, needs Kort approval; only after A |
| F | Confirm zombie reinforcements stay 0 on hard with mob spawning on. | No gameplay change if confirmed |

Recommendation: run A first. If Graveborn falls toward the band with Rise Again off and Gilded rises with Bulwark neutralised, the gap is two traits and sheet stats, and B to D are not needed for this task.

## 7. DuelSimulation

`test-tools/java/gg/gravesandglory/DuelSimulation.java` (unshipped, run with `runServer -PduelSim=<plan>`) spawns real server mobs through the same `Battle.spawn` and `ArmyMobs.create` path as a match, in one open cell with no points, driven by the Normal bot's Attack or Charge order every 100 ticks. It does not model vanilla traits itself: knockback resistance, hitbox, undead rules, gear armour and the `Combat` damage path are the real game's. Consequences:

- Every leak above is already present in the 720-fight results; nothing is modelled twice and nothing is missing.
- House traits default to tier III (`traits=banner`), which puts Rise Again and Bulwark at 45% in the reference numbers. `traits=live` or a lower-tier plan isolates them.
- Fights end on a wipe or a 120 s cap; the cap ended 52 of 720 in the reference run.
- There is no per-trait toggle for knockback resistance or hitbox. To test B or C, edit `ArmyMobs.create` on a scratch branch.

## 8. Unverified

- Vanilla 1.20.1 constants (KBR, hitboxes, immunities) are from memory.
- No duel run was made for this report; the effective-HP table is arithmetic.
- The Rise Again effect size is an estimate.
