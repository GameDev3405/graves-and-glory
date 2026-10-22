# Phase 2 design: the five Houses and the long chase

Version 1, 2026-10-17. Status: **proposal for Kort's sign-off** (the "roster
sheet" gate in Phase 2 of `docs/VISION-AND-ROADMAP.md`). Nothing here is built.
Spec sources: vision sections 2, 3, 4, 5, 8 and 11 (Phase 2). Engine facts come
from `Tactics.java`, `GravesAndGlory.java` (`Army*` subclasses, `Battle.spawn`),
`ArmyCards.java`, `Campaign.java`, `Progression.java`, `sheets/design.json`,
and the Phase 1 plan interfaces (`Morale.RoutGuard`, `Morale.GUARDS`,
`Morale.rally`, `Battle.reinforcementAnchors`, `Projectiles.Shot`). The first
three are gone: the no-routing design (`2026-10-22-no-routing-design.md`)
removed routing, and sections 1.2 to 2.5, 5, 6.1 and 7 below are edited to it.

Decisions I made as builder are marked **[B]** and listed again in section 8
so Kort can overturn them. Everything else follows a written Kort decision.

---

## 0. Engine facts this design is built around

Each row is a fact read from the current code, not an assumption.

| Fact (current code) | Consequence for the design |
|---|---|
| Troops are vanilla mob subclasses (`ArmyHusk`, `ArmySkeleton`, `ArmyWither`) whose `initGoals()` keeps only `SwimGoal`. All movement and attacks come from `Tactics.tick` (navigation, `MoveControl`, and a direct `enemy.damage(mobAttack, spec.damage * campaign.damage(u))`). | Any goal-based 1.20.1 mob can become a troop with the same three-line subclass. Brain-AI mobs are avoided (piglin brute, hoglin, zoglin, warden, camel, sniffer, goat), because their brain keeps running after `initGoals` is emptied. Flying mobs (phantom, vex, allay) are avoided because they ignore ground navigation and formations. Guardians are avoided because they cannot walk on land. |
| `Tactics` uses `spec.range() > 3` to decide "ranged" (kiting, standoff, projectile visuals). | Melee reach stays at or below 3.0 for now. Batch 0 adds a `ranged` column so large mobs can later reach 3.2 to 3.4. |
| Damage is a `mobAttack(mob)` source, so a raised vanilla shield **fully** blocks frontal melee and "arrow" damage today. | The counter table (section 3) replaces the all-or-nothing block with multipliers. It uses a new `gravesandglory:troop` damage type tagged `bypasses_shield`, and the shield maths happens in one place (`Combat.resolve`). |
| `mend` units heal and never deal damage. `blight` units deal no direct damage; they drop a delayed area hit. Both use global numbers (`spellRadius 2.5`, `spellWindupTicks 20`, `healAmount 4`, `abilityCooldownTicks 100`). | Mend and blight are reused. Their numbers move to a per-ability sheet so the Mortar and the Mist Caller can reuse blight with their own radius and windup. |
| `ArmyCards.portrait`, `Battle.spawn` and the stale-entity cleanup each switch over the entity string. | Batch 0 replaces the three switches with one `ArmyMobs` factory (entity id to constructor) and an `ArmyMob` marker interface. After that, a new base mob costs one subclass plus one line. |
| A squad has 20 slots (`rules.squadSize`). Lineups are stored per faction, and bot lineups come from the troop `count` column. | A banner becomes one 20-slot list that does not depend on the side. Bot banners get their own sheet. |
| Class bonuses are hard-coded in `Campaign.damage/health` (vanguard, marksman, arcanist). | Commander numbers move into the `classes` sheet, and the code reads them from there. |
| Phase 1 provides `Battle.reinforcementAnchors(team) -> List<Capture>` and `Projectiles.Shot.kind` ("arrow", "bolt", "mend"). It also provided `Morale.GUARDS` (a RoutGuard list) and `Morale.rally(b,s,group)`; guards and `Morale.rally` are gone (no-routing design). | No House registers a guard or calls a rally. Bulwark is read by the `flank` condition (covered flanks), Stand Fast keeps only its damage cut, the Lord Marshal's banner anchors its troops, and Muster (the Marshal's active) only gives Move orders. Forward Supply needs the anchor list widened from `Capture` to a small `Anchor` record (x, z, name, team) **[B]**. Counter rows key off `Shot.kind`. |
| Vanilla undead burn in daylight unless they wear a helmet, and zombies and husks convert in water after 600 ticks. | Every undead `Army*` subclass overrides `isAffectedByDaylight()` to return false and `canConvertInWater()` to return false. Husks turning into zombies in the river is a latent bug today; fix it in Batch 0. |

---

## 1. The five Houses

### 1.1 Colour language: team first, House second

Teams are banner colours (gold, violet, crimson, teal). Friend or foe is the
most important thing to read, so **the team colour gets the two loudest
surfaces on every troop**: a tabard or chest wrap, plus a small back pennant.
The pennant is a new EMF part on every troop, coloured by an ETF `teams=`
rule on the scoreboard team the troop joins **[B]**. The House is shown by
**silhouette and material**, never by hue alone. That keeps Tidecallers
readable on the teal team and Gilded readable on the gold team.

All skins are 64x64 in the vanilla layout, flat-shaded with 3 to 4 tones per
material, so they hold up with vanilla textures and with Faithful 32x
(Faithful changes items and blocks, not these mob skins). Excalibur stays
optional.

### 1.2 House sheets

**Graveborn**: *the dead who answered the horn.* When the old kings fell,
their graves would not close, and the Graveborn are what climbed out:
shamblers in rusted mail, bowmen of bare bone, wardens in black iron, and
casters who still remember the funeral rites and use them backwards. They
are cheap, there are many of them, and they fight hardest at the edge of the grave. Every battle they
lose adds to their ranks somewhere else.
- Silhouette: hunched or gaunt, ragged hems, no shields, a hanging lantern on
  casters. Bone white, grave-moss green, soul-cyan eye glow (emissive ETF layer).
- Base mobs: zombie, skeleton, stray, wither skeleton.

**Gilded Order**: *the oath that holds the line.* Knights, templars and
longbowmen sworn to the last living crown: plate, kite shields, plumes. They
cost the most and give the least ground. A Gilded line in shieldwall is the
wall the other Houses are built to break.
- Silhouette: upright, broad kite shield, plume or crest on the helm, long
  surcoat. White enamel and polished gold trim, steel grey.
- Base mob: husk (human shape, does not burn in daylight, already shipped as Dawn).

**Wildmoot**: *the woods have teeth.* Boar-kin hunters in hide and antler,
their hounds, the spiders they bargain with, and the great antlered bear
they follow. Fast and hard to pin down, they are lethal in the trees and
fragile in the open.
- Silhouette: snouted heads and big ears (zombified piglin model), low
  four-legged bodies (wolf, spider, bear), antlers and feathers sticking up.
  Russet fur, bark brown, bone, ochre warpaint stripes.
- Base mobs: zombified piglin, wolf, spider, polar bear.

**Ironclad**: *the hold remembers every siege.* Mountain clans of iron and
soot: hearthguards, shield-breakers, arbalists and the crews who drag
ballistae and mortars to war, led by a walking forge. They are slow and
heavy, and strongest when they stop moving.
- Silhouette: wide and square, big beards (vindicator heads read stocky),
  engines on the back. Soot-black iron, brass, ember-orange vents (emissive).
- Base mobs: vindicator, pillager, iron golem.

**Tidecallers**: *the sea wants the land back.* Drowned raiders, trident
guards and mist callers come up the rivers in fog, led by a kraken-touched
beast and a drowned admiral. They move fast along water and slow down
everything they touch.
- Silhouette: kelp trailing from limbs, coral growths on shoulders,
  tridents, hooded robes on casters. Verdigris green, barnacle white,
  deep-water navy, faint teal glow.
- Base mobs: drowned, evoker, ravager.

### 1.3 House traits (3 / 5 / 7)

**[B] Counting.** The vision says "fielding 3, 5 or 7 troops of one House".
A banner has 20 slots, so thresholds of 3/5/7 slots would let one banner run
three traits at once, and a pure banner would gain nothing. Instead, traits
count **banner slots of that House at 6 / 10 / 14** (the vision's tiers,
doubled for the 20-slot banner). A Legendary fills 4 slots and an Epic fills
2 (section 4.6), so collecting deep in one House pushes you up the tiers.
The resulting choices are: pure (one trait at III), 14+6 (III + I),
10+10 (II + II), 6+6+8 (three at I).

| House | Trait | Tier I (6 slots) | Tier II (10 slots) | Tier III (14 slots) |
|---|---|---|---|---|
| Graveborn | *Rise Again*: when a Graveborn troop would die, it may rise once at 30% max HP. It is stunned for 20 ticks (no attacks) and shows soul-fire particles. Once per troop life. Legendaries excluded. | 10% chance | 25% chance | 40% chance |
| Gilded Order | *Bulwark*: Gilded troops with the `shield` tag take less ranged damage (any `Shot.kind` arrow or bolt, and blight), whether or not the shield is raised. Covered flanks: the flank bonus (C16, C16s) does not apply to an **own** troop while a living Gilded troop of another own group stands within R blocks of it. | -15% ranged, R 5 | -30% ranged, R 8 | -45% ranged, R 12 |
| Wildmoot | *Pack Hunters*: a Wildmoot attack deals bonus damage when 3 or more own Wildmoot troops are targeting the same enemy, **or** when the attacker is more than 110 degrees off the target's facing (`BattleMath.relativeBearing`). Meeting both conditions gives the bonus once, not twice. | +10% | +20% | +30% |
| Ironclad | *Entrench*: an Ironclad troop that has moved less than 0.5 blocks in the last 60 ticks takes less damage. Siege-tag troops also gain range while entrenched. Inside an owned objective's capture radius the damage reduction is doubled. | -10% taken, +10% siege range | -15% taken, +20% siege range | -20% taken, +30% siege range |
| Tidecallers | *Riptide*: Tidecallers move faster in water or while it rains on the map, and their hits apply Slowness I. From tier II, Tidecallers ford the river: `Tactics.route` skips the bridge detour for them and their water path penalty is 0. | +15% speed, slow 20 ticks | +25% speed, slow 30 ticks, fording | +35% speed, slow 40 ticks, fording |

Trait uptime appears in the end-of-match report (vision section 6). The
active tier shows in the HUD army strip as a House glyph with I, II or III.

---

## 2. Full roster (30 troops)

How to read the tables:
- HP is the `health` column, before worn armour.
- Dmg is damage per hit before multipliers. Range is in blocks.
- CD is the attack cooldown in ticks.
- Speed is the `GENERIC_MOVEMENT_SPEED` base value that the spawn code sets
  (current troops use 0.29 to 0.34).
- Cost is round gold at the barracks.
- Train is the training time in ticks (the sheet keeps
  `trainingSeconds` = ticks / 20).
- Slot weight: Common and Rare 1, Epic 2, Legendary 4.

**Balance rule [B]:** power per *slot* rises with rarity; power per *gold*
stays flat or falls. Rarity buys density, not efficiency.

Ability ids refer to the `abilities` sheet (section 6.1). **NEW** marks a
mechanic that does not exist in the code today. There are six new
mechanics, each reused by several troops: `stance` (a conditional multiplier
row in the counter table), `pierce/cleave` (a secondary target), `onhit_slow`,
`pounce`, `aura` and `slam`. Two new engine hooks are also needed:
`fearless` (a damage bonus at low health) and the `mist` zone (a persistent blight).

### 2.1 Gilded Order (maps to Dawn)

| id | Name | Rarity | Role | Mob | HP | Dmg | Range | CD | Speed | Cost | Train | Ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `knight` | Order Squire | Common | infantry | husk | 32 | 5 | 2.8 | 25 | 0.28 | 22 | 80 | `shield` (existing) |
| `archer` | Order Archer | Common | ranged | husk | 20 | 4 | 22 | 40 | 0.30 | 24 | 80 | `steady`: +20% damage while the group order is Hold (**NEW** stance) |
| `sentinel` | Templar | Rare | infantry | husk | 46 | 6 | 2.8 | 28 | 0.25 | 48 | 140 | `shield` (existing) |
| `longbow` | Order Longbow | Rare | ranged | husk | 18 | 6 | 30 | 45 | 0.28 | 46 | 140 | `pierce`: each shot also hits the nearest enemy within 2 blocks behind the target for 50% (**NEW**) |
| `mage` | Sunweaver | Epic | support | husk | 24 | 0 | 18 | 55 | 0.29 | 70 | 180 | `mend`: heals 5 HP, cooldown 80, radius 10 (existing mechanic, per-ability numbers) |
| `gl_marshal` | Lord Marshal Aldric | Legendary | infantry | husk | 80 | 9 | 2.8 | 22 | 0.27 | 140 | 300 | `oath_banner`: on Hold, plants a banner (cooldown 600). For 300 ticks, own troops within 12 deal +10% damage and are anchored: they cannot be knocked back, pulled or slowed (a slow already on a troop is removed on entry; stuns, weaken and bleed still land). (**NEW** aura) |

| id | Beats | Beaten by | Evolve line (stage 2 / stage 3 deltas, HP/Dmg %) |
|---|---|---|---|
| `knight` | archers and arbalists (shield) | Shieldbreaker, Reaper cleave, blight | Squire / Knight (+3/+3) / Knight-Captain (+3/+3) |
| `archer` | slow infantry, Graveborn Risen | Hounds, Web Stalkers, any charge | Archer / Order Bowman (+2/+4) / Sergeant-Archer (+1/+2) |
| `sentinel` | ranged of every kind, Hounds at a wall | Shieldbreaker, Mortar, Mist Caller | Templar / Templar Knight (+4/+2) / Templar Commander (+2/+1) |
| `longbow` | packed lines (pierce), Ballista crews at range | Web Stalkers, Hounds, Mist | Longbow / Yeoman Longbow (+2/+4) / Master Longbow (+1/+2) |
| `mage` | attrition fights, Rise Again stalls | Pounce, Harpooner pull, Mortar | no evolution (Epic) |
| `gl_marshal` | slows, pulls and knockback: Tidecallers and Ironclad slams | Slam (Colossus, Old Horn), focused blight | no evolution (Legendary) |

### 2.2 Graveborn (maps to Dusk)

| id | Name | Rarity | Role | Mob | HP | Dmg | Range | CD | Speed | Cost | Train | Ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `revenant` | Risen | Common | infantry | zombie (**new subclass** `ArmyZombie`) | 26 | 4 | 2.8 | 25 | 0.25 | 14 | 60 | `fearless`: a fearless soldier at or below 30% of his max health deals +20% damage (**NEW** stance) |
| `bonebow` | Bone Archer | Common | ranged | skeleton | 18 | 4 | 22 | 40 | 0.27 | 16 | 60 | `fearless` (as above) |
| `reaper` | Grave Reaper | Rare | infantry | wither skeleton | 34 | 8 | 2.8 | 30 | 0.30 | 40 | 120 | `cleave`: each hit also strikes one other enemy within 2 blocks of the target for 50% (**NEW**, shares code with pierce) |
| `stalker` | Barrow Stalker | Rare | ranged | stray (**new subclass** `ArmyStray`) | 20 | 5 | 26 | 40 | 0.30 | 40 | 120 | `frostshot`: hits apply Slowness I for 40 ticks (**NEW** onhit_slow) |
| `hexer` | Gravecaller | Epic | support | wither skeleton | 22 | 7 | 18 | 55 | 0.27 | 60 | 160 | `blight`: radius 2.5, windup 20, cooldown 100, Slowness I for 40 ticks (existing) |
| `gb_hollowking` | The Hollow King | Legendary | infantry | wither skeleton | 70 | 10 | 3.0 | 30 | 0.26 | 120 | 300 | `dread`: passive aura, radius 10. Enemy troops inside deal -15% damage (**NEW** aura) |

| id | Beats | Beaten by | Evolve line |
|---|---|---|---|
| `revenant` | other Commons by numbers and price, long grinding fights | Cleave, Slam, Mortar, Order Archers kiting | Risen / Revenant (+3/+3) / Grave Knight (+3/+3) |
| `bonebow` | Sunweaver (focus fire), slow siege crews | Hounds, Gilded shields | Bone Archer / Barrow Archer (+2/+4) / Deadeye (+1/+2) |
| `reaper` | Commons in crowds, Risen-vs-Risen mirrors | Templar (shield), Trident Guard brace | Grave Reaper / Harvester (+3/+3) / Lord of Scythes (+3/+3) |
| `stalker` | Hounds and Web Stalkers (the slow kills pounce), cavalry-like | Templar, Mist Caller | Barrow Stalker / Frost Stalker (+2/+4) / Wight Stalker (+1/+2) |
| `hexer` | shieldwalls, clumped Commons | Hounds, Harpooners, pierce | no evolution |
| `gb_hollowking` | damage-heavy packs, above all Wildmoot's | Lord Marshal banner, Sunder, Colossus | no evolution |

### 2.3 Wildmoot (new)

| id | Name | Rarity | Role | Mob | HP | Dmg | Range | CD | Speed | Cost | Train | Ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `wm_hound` | Moot Hound | Common | cavalry-like | wolf (**new** `ArmyWolf`) | 18 | 4 | 2.2 | 20 | 0.36 | 16 | 60 | `pounce`: the first hit after closing 6+ blocks without attacking deals x1.8; cooldown 100 (**NEW**) |
| `wm_hunter` | Hide Hunter | Common | ranged | zombified piglin (**new** `ArmyBoarkin`) | 18 | 4 | 20 | 36 | 0.31 | 20 | 70 | `ambush`: +25% damage while the shooter cannot see the sky (under tree canopy or a roof) (**NEW** stance) |
| `wm_stalker` | Web Stalker | Rare | cavalry-like | spider (**new** `ArmySpider`) | 28 | 6 | 2.6 | 25 | 0.34 | 40 | 120 | `ensnare`: hits apply Slowness II for 30 ticks, cooldown 80 (**NEW** onhit_slow). Climbs walls (vanilla spider navigation). |
| `wm_tusk` | Tusk Spear | Rare | infantry (spear) | zombified piglin | 30 | 6 | 3.0 | 28 | 0.31 | 38 | 120 | `brace`: x1.3 against enemies whose group order is Charge, while its own order is Hold (**NEW** stance) |
| `wm_seer` | Bloodhowl Seer | Epic | support | zombified piglin | 24 | 3 | 14 | 40 | 0.31 | 60 | 160 | `howl`: every 300 ticks, own troops within 10 get +15% damage and +15% speed for 120 ticks (**NEW** aura) |
| `wm_oldhorn` | Old Horn, the Antlered Alpha | Legendary | cavalry-like | polar bear (**new** `ArmyBear`) | 90 | 11 | 3.0 | 30 | 0.30 | 130 | 300 | `trample`: on first contact while its group order is Charge, deals 6 to every enemy within 3 with knockback 1.2; cooldown 200 (**NEW** slam) |

| id | Beats | Beaten by | Evolve line |
|---|---|---|---|
| `wm_hound` | archers, casters, wounded troops | spears (Tusk, Trident Guard), Frost Stalker, Templar wall | Moot Hound / Dire Hound (+3/+3) / Packleader (+3/+3) |
| `wm_hunter` | anything walking through woods | open-field archers, Hounds, Mortar | Hide Hunter / Thornstalker (+2/+4) / Elder Hunter (+1/+2) |
| `wm_stalker` | ranged backlines, wall defenders (climbs) | spears, Frost Stalker, Sunweaver sustain | Web Stalker / Broodstalker (+3/+3) / Widowmaker (+3/+3) |
| `wm_tusk` | cavalry-like (spear tag), charges | archers, Mortar, Cleave | Tusk Spear / Tuskguard (+4/+2) / Boar-Lord (+2/+1) |
| `wm_seer` | long melee brawls | pierce, Barrow Stalker, Harpooners | no evolution |
| `wm_oldhorn` | archer lines, Commons in Line formation | braced spears, Shieldbreaker, Mortar | no evolution |

### 2.4 Ironclad (new)

| id | Name | Rarity | Role | Mob | HP | Dmg | Range | CD | Speed | Cost | Train | Ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `ic_hearthguard` | Hearthguard | Common | infantry | vindicator (**new** `ArmyVindicator`) | 34 | 5 | 2.8 | 28 | 0.24 | 22 | 90 | `shield` (existing; the vindicator renders an off-hand shield) |
| `ic_arbalist` | Arbalist | Common | ranged | pillager (**new** `ArmyPillager`) | 20 | 6 | 24 | 60 | 0.25 | 24 | 90 | `armour_bolt`: against `shield` targets the shield multiplier is 0.7 instead of 0.35 (**NEW** stance row) |
| `ic_breaker` | Shieldbreaker | Rare | infantry | vindicator | 38 | 8 | 2.8 | 32 | 0.25 | 44 | 140 | `sunder`: ignores the shield multiplier entirely and deals x1.4 to `shield` targets (**NEW** stance row) |
| `ic_ballista` | Ballista Crew | Rare | siege | pillager with an EMF ballista frame | 24 | 10 | 34 | 80 | 0.20 | 48 | 160 | `pierce`: also hits a second target within 4 blocks behind for 60% (**NEW**, shared) |
| `ic_mortar` | Ironhold Mortar | Epic | siege | pillager with an EMF mortar barrel | 22 | 9 | 36 (minimum 10) | 100 | 0.18 | 70 | 200 | `shell`: blight variant, radius 3.5, windup 30, cooldown 100, no slow, cannot target anything closer than 10 (existing blight with per-ability numbers and a minimum range) |
| `ic_colossus` | Brassjaw, the Forge Colossus | Legendary | infantry | iron golem (**new** `ArmyGolem`) | 120 | 12 | 3.0 | 36 | 0.22 | 150 | 340 | `forge_slam`: every hit also deals 60% to all other enemies within 2.5 with knockback 0.8 (**NEW** slam) |

| id | Beats | Beaten by | Evolve line |
|---|---|---|---|
| `ic_hearthguard` | archers, Hounds at a wall | Shieldbreaker, Mortar, Mist | Hearthguard / Ironwall (+4/+2) / Hearth-Thane (+2/+1) |
| `ic_arbalist` | Gilded shields (partly), slow infantry | Hounds, Web Stalkers (60-tick reload) | Arbalist / Steel Arbalist (+2/+4) / Bolt-Master (+1/+2) |
| `ic_breaker` | every shield troop, the Colossus | archers, Hounds, Reaper cleave | Shieldbreaker / Gatebreaker (+3/+3) / Anvilfist (+3/+3) |
| `ic_ballista` | lines, Legendaries (large), structures (Phase 4) | Hounds, Web Stalkers (climb past cover), Old Horn | Ballista Crew / Siege Crew (+3/+3) / Master Engineer (+2/+4) |
| `ic_mortar` | shieldwalls, entrenched lines, clumps | anything that gets closer than 10 | no evolution |
| `ic_colossus` | Commons, packed melee | Shieldbreaker, Mortar focus fire, kiting archers | no evolution |

### 2.5 Tidecallers (new)

| id | Name | Rarity | Role | Mob | HP | Dmg | Range | CD | Speed | Cost | Train | Ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `tc_raider` | Drowned Raider | Common | infantry | drowned (**new** `ArmyDrowned`) | 28 | 5 | 2.8 | 24 | 0.29 | 18 | 70 | `brine`: hits apply Slowness I for 20 ticks (**NEW** onhit_slow; when Riptide also applies, the longer duration wins) |
| `tc_harpooner` | Harpooner | Common | ranged | drowned (trident projectile visual) | 20 | 5 | 16 | 40 | 0.29 | 20 | 70 | `harpoon`: hits pull the target 1 block toward the shooter and apply Slowness I for 30; cooldown 60 (**NEW** onhit_slow with knockback -1.0) |
| `tc_tideguard` | Trident Guard | Rare | infantry (spear) | drowned | 34 | 7 | 3.0 | 28 | 0.28 | 42 | 130 | `brace` (shared with Tusk Spear) |
| `tc_mist` | Mist Caller | Rare | support | evoker (**new** `ArmyEvoker`) | 22 | 0 | 18 | 60 | 0.28 | 46 | 140 | `mist`: a blight variant that leaves a cloud of radius 4 for 100 ticks. Enemies inside get Slowness I, and ranged hits fired from or into the cloud deal x0.5; cooldown 160 (**NEW** zone) |
| `tc_brute` | Kraken-touched Brute | Epic | cavalry-like | ravager (**new** `ArmyRavager`) | 70 | 9 | 3.0 | 34 | 0.28 | 75 | 200 | `tentacle_sweep`: every hit also deals 70% to other enemies within 3 with knockback 1.0; at most one sweep per 60 ticks (**NEW** slam) |
| `tc_admiral` | Admiral Vael, the Drowned Tide | Legendary | infantry | drowned | 64 | 9 | 3.0 | 26 | 0.30 | 130 | 300 | `undertow`: every 200 ticks, pulls enemies within 7 three blocks toward her, deals 4 damage and applies Slowness II for 60 ticks (**NEW** slam with negative knockback) |

| id | Beats | Beaten by | Evolve line |
|---|---|---|---|
| `tc_raider` | Hounds and runners (slow), retreating troops | Templar, Cleave, Mortar | Drowned Raider / Reef Raider (+3/+3) / Tide Reaver (+3/+3) |
| `tc_harpooner` | casters and Ballista crews (pulls them out of cover) | longer-range archers, Gilded shields | Harpooner / Whaler (+2/+4) / Deep Harpooner (+1/+2) |
| `tc_tideguard` | cavalry-like, Old Horn, Brute mirror | archers, Mortar, Shieldbreaker | Trident Guard / Reef Warden (+4/+2) / Tide-Sworn (+2/+1) |
| `tc_mist` | archer and ballista lines | Hounds, Web Stalkers, Reaper | Mist Caller / Fogweaver (+3/+3) / Stormcaller (+3/+3) |
| `tc_brute` | Commons, archer lines | braced spears, Shieldbreaker focus fire | no evolution |
| `tc_admiral` | kiting archers, backline casters (pull) | Lord Marshal banner, Sunder focus fire | no evolution |

### 2.6 Evolution deltas and stage visuals

Evolve deltas are cumulative percentages over base HP and damage. Range,
speed, cooldown and abilities never change. Three patterns are used:
balanced (+3/+3, then +3/+3), tank (+4/+2, then +2/+1) and striker (+2/+4,
then +1/+2). Every pattern stays at or under +6% per stat, so stars (max
+6% for Commons and Rares, section 4.4) plus evolution never pass +12% on
any stat. Each stage also changes the troop's custom name to the stage name.
ETF and EMF use that name to swap the skin and add an overlay part (section
2.7). The evolved stage name shows on the troop card.

### 2.7 Skin and overlay plan

Mechanism **[B]**:
- One ETF skin per troop per stage, selected by the troop's custom name
  (a `name=` rule in `<mob>.properties`).
- One EMF variant model per troop, selected the same way (`models.N` plus
  `name.N` in the EMF properties).
- The team pennant and the tabard texture variant are selected by
  scoreboard team (`teams=`).
- The existing `husk.jem`, `skeleton.jem` and `wither_skeleton.jem` become
  model 1 (the default) of each mob.
- Files live in the armory pack:
  `assets/minecraft/optifine/random/entity/<path>/<mob>N.png`,
  `assets/minecraft/optifine/cem/<mob>N.jem` and `<mob>.properties`.
- Overlay textures extend `armor_overlay.png` into one 64x64 sheet per
  House (`overlay_<house>.png`) with the same region layout as today.

Every troop gets the back pennant (team colour) and one extra part at
stage 3 (shown in brackets).

| Troop | Items (weapon / helmet / chest) | ETF skin | EMF overlay parts (stage 3 extra) |
|---|---|---|---|
| Order Squire | iron_sword / iron_helmet / iron_chestplate + shield | white enamel surcoat over mail, gold trim | tabard, kite-shield boss (gold crest plume) |
| Order Archer | bow / chainmail_helmet / leather_chestplate | green-grey gambeson, gold cord | quiver on back, hood (feathered cap) |
| Templar | diamond_sword / diamond_helmet / iron_chestplate + shield | full white plate, gold cross | great helm crest, long tabard, pauldrons (cape) |
| Order Longbow | bow / leather_helmet / leather_chestplate | yeoman brown, gold sash | tall quiver, bracer (feathered hat) |
| Sunweaver | blaze_rod / golden_helmet / leather_chestplate | white robe with sun embroidery | sun halo on head (emissive), stole |
| Lord Marshal Aldric | netherite_sword / golden_helmet / diamond_chestplate + shield | gilded plate, red cloak | crown-helm, long cloak, banner pole on back |
| Risen | iron_sword / chainmail_helmet / chainmail_chestplate | grey-green rot, torn tabard | ragged hem, exposed rib plate (rusted crown) |
| Bone Archer | bow / chainmail_helmet / leather_chestplate | bone white, moss | bone quiver, skull pauldron (lantern) |
| Grave Reaper | iron_hoe / netherite_helmet / iron_chestplate | black iron, soul-cyan eyes (emissive) | scythe blade extending the hoe arm, hood (spine plates) |
| Barrow Stalker | bow / chainmail_helmet / none | stray rags recoloured frost blue | fur mantle, antler-bone headdress (frost mist strips) |
| Gravecaller | amethyst_shard / golden_helmet / leather_chestplate | burial robe, gold death mask | lantern on a chain, hood (candles on shoulders) |
| The Hollow King | netherite_sword / golden_helmet / netherite_chestplate | black and gold, hollow ribcage | broken crown, tattered royal cloak, soul fire in the chest (emissive) |
| Moot Hound | none (wolf) | dark russet, warpaint stripe | bone collar, spiked hide saddle pad (antler tufts) |
| Hide Hunter | bow / leather_helmet / leather_chestplate | boar-kin fur, ochre stripes, no rot | feather headdress, hide cloak (antler headdress) |
| Web Stalker | none (spider) | black-green chitin, white markings | silk sacs on the back, mandible bones (bone mask) |
| Tusk Spear | trident / leather_helmet / leather_chestplate | boar-kin, whitened tusks | hide shoulder mantle, spear tassel (tusk crown) |
| Bloodhowl Seer | bone / leather_helmet / leather_chestplate | grey fur, red handprints | antler crown, totem staff on back (emissive eyes) |
| Old Horn | none (polar bear) | dark brown dire bear, grey muzzle | antlers (head), bone-and-feather mantle, warpaint (emissive eyes) |
| Hearthguard | iron_axe / iron_helmet / iron_chestplate + shield | soot iron, braided beard on the vindicator face | wide square pauldrons, round shield boss (horned helm) |
| Arbalist | crossbow / chainmail_helmet / leather_chestplate | soot leather, brass goggles | bolt case on back, goggles (brass shoulder plate) |
| Shieldbreaker | netherite_axe / iron_helmet / iron_chestplate | ember-orange tattoos | anvil-plate pauldron, chain belt (forge apron) |
| Ballista Crew | crossbow / iron_helmet / leather_chestplate | soot and brass | ballista frame on root (2 arms, bolt), pair of wheels (brass crest) |
| Ironhold Mortar | none / iron_helmet / iron_chestplate | soot and ember | mortar barrel on back with an ember vent (emissive), ammo crate |
| Brassjaw | none (iron golem) | brass and soot plates, ember core | furnace grille chest (emissive), smokestacks on shoulders, rivets |
| Drowned Raider | iron_sword / chainmail_helmet / none | verdigris skin, kelp | kelp strands from the arms, coral shoulder (barnacle crown) |
| Harpooner | trident / leather_helmet / none | navy rags, rope | rope coil, harpoon quiver (shark-tooth necklace) |
| Trident Guard | trident / iron_helmet / chainmail_chestplate | barnacled mail | coral pauldrons, shell shield on back (coral crown) |
| Mist Caller | none (evoker spells) | deep navy robe, teal runes (emissive) | hood with kelp fringe, conch on belt (mist strips) |
| Kraken-touched Brute | none (ravager) | barnacle-crusted hide, teal | tentacle maw (4 boxes on the jaw), coral spines on the back |
| Admiral Vael | trident / golden_helmet / netherite_chestplate | drowned officer's coat | tricorne, long coat tails, anchor on back (emissive eyes) |

The non-humanoid mobs (wolf, spider, polar bear, iron golem, ravager) need
new JEMs. Where Fresh Animations ships an animation file for that mob, the
JEM references it from a root entry, like the existing three do. Otherwise
it keeps vanilla geometry plus attached child parts. Non-humanoids get
`minecraft:air` for helmet and chest.

---

## 3. Counter rules

All troop damage goes through one function, `Combat.resolve(attacker,
target, base, kind)` (**NEW**). It multiplies together the matching rows
below, then House traits, then commander effects. The progression bonus is
already part of `base`. The product is clamped to **[0.25, 2.5]**, and the
function then calls `target.damage` with the `gravesandglory:troop` damage
type, which bypasses the vanilla shield (armour still applies). Every row
lives in the `counters` sheet, so balance passes edit data, not code.

| # | Rule | Attacker | Target | Condition | Multiplier |
|---|---|---|---|---|---|
| C1 | Shields vs arrows | any ranged (`Shot.kind` arrow) | `shield` tag, shield raised | attacker within 90 degrees of the target's facing | x0.35 |
| C2 | Shields vs arrows (lowered) | any ranged arrow | `shield` tag, shield not raised | frontal 90 degrees | x0.75 |
| C3 | Shield vs melee | melee | `shield` tag, shield raised | frontal 90 degrees | x0.6 |
| C4 | Shield vs magic | blight, mist, bolt | `shield` | any | x1.0 (magic ignores shields) |
| C5 | Charge vs archers (impact) | melee whose group order became Charge in the last 60 ticks | target in a `ranged` group | first hit per attacker per charge | x1.5 |
| C6 | Charge vs archers (sustained) | melee, group order Charge | `ranged` group | after the impact hit | x1.2 |
| C7 | Cavalry-like vs archers | `beast` or `cavalry` tag | `ranged` group | always | x1.3 (does not stack with C5/C6; the highest applies) |
| C8 | Spears vs beasts | `spear` tag | `beast` tag | always | x1.6 |
| C9 | Beasts into braced spears | `beast` tag | `spear` tag, order Hold | frontal 90 degrees | x0.7 |
| C10 | Siege vs structures | `siege` tag | structure (Phase 4 gates and walls, the Forward Supply banner) | always | x4.0 |
| C11 | Non-siege vs structures | anything else | structure | always | x0.25 |
| C12 | Siege vs large | `siege` | `large` tag (Epic beasts, Legendaries) | always | x1.3 |
| C13 | Siege vs small and fast | `siege` | `small` tag (wolf, spider) | always | x0.6 |
| C14 | Sunder | `sunder` ability | `shield` | replaces C1 to C3 | x1.4 |
| C15 | Armour bolt | `armour_bolt` | `shield` | replaces C1 only | x0.7 |
| C16 | Flank (all Houses) | any melee | any | attacker more than 110 degrees off the target's facing, and the target is not covered by Bulwark | x1.30 (no-routing design; was x1.15; Wildmoot Pack Hunters stacks on top) |
| C16s | Flank shot | `shot` (arrow, bolt) | any | as C16 | x1.15 (no-routing design) |
| C17 | Mist cover | ranged | any | shooter or target inside a Mist cloud | x0.5 |
| C18 | Stance rows | `steady`, `ambush`, `brace` | as in section 2 | as in section 2 | x1.2 / x1.25 / x1.3 |

Tags per troop (sheet column `tags`, a comma list):
- shield: knight, sentinel, gl_marshal, ic_hearthguard
- spear: wm_tusk, tc_tideguard, tc_admiral
- beast and cavalry: wm_hound, wm_stalker, wm_oldhorn, tc_brute
- siege: ic_ballista, ic_mortar
- large: all Epics except the casters, and all Legendaries
- small: wm_hound, wm_stalker
- fearless: revenant, bonebow

Troop cards show the beats and beaten-by lines from section 2, with a
crossed-swords glyph for each counter row that applies.

---

## 4. Progression economy

### 4.1 Gold

| Source | Amount | Notes |
|---|---|---|
| Match completion (skirmish, campaign, multiplayer) | 80 | unchanged; `minimumRewardSeconds 180` still applies |
| Win | +40 | unchanged |
| Practice (1vAI quick practice) | 40 | unchanged |
| Daily contracts | 30 / 40 / 30 | unchanged (1 battle, 3 battles, capture or orders) |
| Weekly contract | 150 | unchanged (6 battles) |
| Founders chapter (re-authored) | 10 tiers x 30 = 300 total | down from 24 x 25 = 600; the other tiers become shards, titles, cosmetics and one Rare |

| Sink | Cost |
|---|---|
| House recruitment roll | 100 |
| Chosen Common or Rare (new, or a duplicate copy) | 300 |
| Evolve a Common to stage 2 / 3 | 100 / 250 gold (plus shards) |
| Evolve a Rare to stage 2 / 3 | 150 / 350 gold (plus shards) |

### 4.2 Recruitment pools

The player picks a House, and the roll draws one troop from that House by
weight. Legendaries are never in the pool. Weights per troop:

| Rarity | Weight per troop | Share of a House pool |
|---|---|---|
| Common (2) | 43 | 86% |
| Rare (2) | 6 | 12% |
| Epic (1) | 2 | 2% |

Bad-luck protection **[B]**: per House, if 39 rolls in a row gave no Epic,
the 40th roll gives the Epic. The counter resets on any Epic. Owned troops
stay in the pool; rolling one gives a duplicate (stars first, then shards).

### 4.3 Legendaries

- **Legendary Pact**: finishing campaign chapter 1 grants one Pact, which
  can be spent on any House's Legendary.
- **House Renown**: +1 Renown in a House for each eligible match in which
  that House's trait was at tier I or higher in your banner, and +1 more on a
  win. Renown levels I to V sit at 3 / 8 / 15 / 22 / 30 points and unlock
  House banner cosmetics along the way. At Renown V (30 points), the House
  Legendary can be bought for **50 House shards**.

### 4.4 Rank stars

Owning a troop is star 1. Duplicates add stars, up to 3.

| Rarity | Star 2 | Star 3 | Bonus at star 2 / 3 (HP and damage) |
|---|---|---|---|
| Common, Rare | 1 duplicate | 2 more duplicates (4 copies total) | +3% / +6% |
| Epic | 1 duplicate or 30 shards | 2 more duplicates or 60 shards | +6% / +12% |
| Legendary | 40 shards | 80 shards | +6% / +12% |

**Hard cap:** for every stat, the star bonus plus the evolution bonus is at
most +12% over the sheet base, in every mode. Commons and Rares reach the
cap at star 3 and stage 3 (6 + 6). Epics and Legendaries reach it at star 3
alone. `Campaign` computes `progressionMultiplier(stat) = min(1.12, 1 +
stars% + stage%)`, and a standalone check asserts the cap for every row.
Commander class effects and House traits are battle effects, so they sit
outside this cap.

### 4.5 Shards (per House)

| Source | Shards |
|---|---|
| Each eligible match | +1 for every House at trait tier I or higher in your banner; +1 more to your largest House on a win |
| All three daily contracts claimed | +3 to a House you choose when claiming |
| Weekly contract | +10 to a chosen House |
| Campaign bonus objective (one-time per battle) | +5 to that battle's House |
| A duplicate past star 3 (dismantled automatically) | Common 2, Rare 4, Epic 8 |
| Founders chapter tiers | 12 tiers x 10 shards (chosen House) |

| Shard sink | Cost |
|---|---|
| Evolve a Common to stage 2 / 3 | 10 / 25 |
| Evolve a Rare to stage 2 / 3 | 15 / 35 |
| Epic star 2 / 3 without duplicates | 30 / 60 |
| Legendary unlock (needs Renown V) | 50 |
| Legendary star 2 / 3 | 40 / 80 |

Stage 3 also requires Mastery I for that troop.

### 4.6 Banner rules

A banner has 20 slots. An Epic unit fills 2 slots and a Legendary fills 4
(each is still one unit). A banner can hold at most one Legendary and two
Epic units. House trait thresholds count slots.

### 4.7 Mastery (per troop)

Mastery is counted from the server-side match ledger: kills by that troop
type, and matches in which at least one troop of that type survived to the
end.

| Level | Kills | Matches survived | Reward |
|---|---|---|---|
| I | 40 | 8 | title "<Troop> Veteran"; unlocks evolve stage 3 |
| II | 150 | 25 | cosmetic variant skin (ETF name suffix " (Veteran)") |
| III | 400 | 60 | brass portrait frame and the title "<Troop> Master" |

House mastery: getting all six troops of a House to Mastery I gives the House
title ("Oathsworn", "Gravebound", "Moot-Blooded", "Hold-Kin", "Tidesworn").

### 4.8 Founders chapter, re-authored

The chapter keeps the season id `founders`, the same 30 tiers at 250 XP per
tier, the server ledger and claims, and it never expires **[B]**. Tiers
already claimed stay claimed.

| Tiers | Reward |
|---|---|
| 1, 3, 7, 9, 11, 13, 17, 19, 23, 27 | 30 gold |
| 2, 4, 6, 8, 12, 14, 16, 18, 22, 24, 26, 28 | 10 shards (chosen House) |
| 5, 10, 15, 25, 30 | titles (the existing six minus Vanguard: Scout, Pathfinder, Captain, Veteran, Bannerlord) |
| 20 | one chosen Rare troop |
| 21, 29 | banner cosmetics (pennant pattern, banner frame) |

### 4.9 Worked model: hours to a full roster at one star

Assumptions:
- A 12-minute match plus 3 minutes of camp, lobby and report gives 4 matches
  per hour, with a 50% win rate.
- 1.5 hours of play per play day, 4 play days a week.
- The player rolls one House until its Epic drops, then buys any missing
  Rare for 300 (the cheapest policy).
- Legendary #1 comes from the Pact.

Income per hour:
- matches: 4 x (80 + 0.5 x 40) = 400
- daily contracts: 100 per 1.5 h = 67
- weekly contract: 150 per 6 h = 25
- **total about 490 gold per hour** (chapter gold counted as a one-off)

Gold needed, from a Monte Carlo of 20,000 runs of the policy above
(weights 43/6/2, pity at 40): per House the mean is 2,920 gold (27.5
rolls). For all five Houses the mean is 14,700 gold, the 10th percentile
11,000, the median 14,800 and the 90th percentile 18,200. One-off offsets:
300 chapter gold and the tier-20 Rare (worth 300).

| Player | Gold needed | Hours |
|---|---|---|
| Lucky (10th percentile) | 10,400 | 21 |
| Median | 14,200 | 29 |
| Mean | 14,100 | 29 |
| Unlucky (90th percentile) | 17,600 | 36 |

Parallel gates, which do not bind at the median:
- Renown V in the four non-Pact Houses needs 4 x 20 House-matches. Fielding
  two Houses per match, that is 40 matches, or 10 hours.
- Shards for four Legendaries (200 total) arrive at about 20 per hour: 2.5
  per match, contracts, and about 100 automatic dismantles over the run at 2
  each. That is about 10 hours, and those shards compete with evolutions.

A median player therefore completes the roster in about **29 hours**, inside
the 25 to 40 target. The lucky tail at 21 hours is accepted: those players
still have evolutions, stars and mastery ahead.

Next visible goal at each point for a median player:

| Hour | What just happened | Next goal on screen |
|---|---|---|
| 0 to 2 | campaign chapter 1, 10 Commons owned, Pact spent | first Rare (300 gold or rolls) |
| 2 to 6 | 2 to 4 Rares, first stars on Commons | first Epic (pity counter shown) |
| 6 to 12 | first House complete except its Legendary, Renown III | Renown V in that House |
| 12 to 20 | second and third Houses, Legendary #2 | first stage-3 evolution |
| 20 to 29 | last Epics, Legendaries #3 to #5 | Mastery II skins, star 3 on favourites |
| 29+ | roster complete | evolutions (850 shards for all of them), Mastery III |

The model lives in a sheet (`economyModel`, NOT_SHIPPED) with these inputs,
so balance passes can rerun it. The Monte Carlo script goes in `tools/`
(Python, not shipped).

---

## 5. Commander classes

Six classes replace Vanguard, Marksman and Arcanist. Each has one active
ability (on a new commander ability key, default G **[B]**) and one passive.
The numbers live in the `classes` sheet. Times are in ticks at 20 per second.

| Class | Track | Active | Cooldown | Duration | Radius / range | Effect | Passive |
|---|---|---|---|---|---|---|---|
| Champion | hero | *Stand Fast* | 1800 (90 s) | 160 (8 s) | 12 around you | own troops in the radius take -25% damage; you get Resistance I | +4 max HP for you; own infantry within 10 of you deal +10% damage |
| Reaver | hero | *Bloodrush* | 1500 (75 s) | 200 (10 s) | self | Speed II and 30% lifesteal on your melee hits; each enemy troop you kill refunds one free reinforcement of your cheapest lost troop (max 3 per use) | +2 attack damage for you; respawn in 9 s instead of 12 |
| Warden | hero | *Mark Prey* | 1200 (60 s) | 240 (12 s) | raycast 40 | the marked enemy (troop or commander) takes +30% damage from your troops and from you, and glows for your team | own ranged troops deal +10% damage and gain +2 range |
| Marshal | general | *Muster* (id `rally_horn`) | 2400 (120 s) | 300 (15 s) | all own groups | every group gets a Move order to your position; your troops move +20% for the duration (`value1` 0.2), troops that join meanwhile too; no horn sounds in the field (only you hear the rally sound, as command feedback) | training time -20% |
| Warlock | general | *Grave Bloom* | 2400 (120 s) | 600 (30 s) | radius 8 at a point within 30 | raises up to 6 troops (from either side) that died inside the circle in the last 20 s as temporary Risen in your squad (60% HP, outside the 20-slot cap); they crumble after 30 s | own `magic` troops cost 20% less round gold |
| Quartermaster | general | *Forward Supply* | 3000 (150 s) | 1200 (60 s) | at your feet, at least 30 from your own base | places a supply banner (a structure target with 40 HP). While it stands and no enemy is within 12, it is an `Anchor` in `reinforcementAnchors` | +2 round gold per held point per income tick (3 becomes 5) |

Class picks migrate as: vanguard to Champion, marksman to Warden, arcanist
to Warlock **[B]**. New engine pieces: a commander ability key and a
cooldown tile on the HUD; a per-battle death log of (position, time, troop
id) for Grave Bloom; and the `Anchor` record for Forward Supply.

---

## 6. Data model

### 6.1 Sheet changes in `sheets/design.json`

All new sheets follow the preflight rules: every cell filled, unique ids,
references checked. Types: S string, I int, D double, B bool.

**`houses`** (new)

| Column | Type | Example |
|---|---|---|
| id | S | `gilded` |
| name | S | `Gilded Order` |
| accent | I | `14667624` (gold trim 0xDFCE68, UI only) |
| glyph | S | `gravesandglory:textures/gui/house/gilded.png` |
| troops | S | `knight,archer,sentinel,longbow,mage,gl_marshal` |
| title | S | `Oathsworn` |

**`houseTraits`** (new, one row per House per tier)

| Column | Type | Example |
|---|---|---|
| id | S | `gilded_2` |
| house | S | `gilded` |
| tier | I | 2 |
| slots | I | 10 |
| mechanic | S | `bulwark` |
| value | D | 0.30 (ranged reduction) |
| radius | D | 8.0 (covered flanks) |
| durationTicks | I | 0 |
| text | S | `Shield troops take 30% less ranged damage; your troops within 8 of a Gilded troop take no extra flank damage.` |

**`troops`** (changed). Removed columns: `faction`, `count`. Added columns:

| Column | Type | Example (`knight`) |
|---|---|---|
| house | S | `gilded` |
| rarity | S | `common` (ref `rarities`) |
| ranged | B | false (replaces the `range > 3` test) |
| tags | S | `shield` (`none` if empty) |
| evolveLine | S | `knight` (ref `evolutions`, or `none`) |
| slotWeight | I | 1 |
| pennant | B | true |

Kept columns:
- id, name, health, damage, range, speed, cooldown, magic, cost,
  trainingSeconds.
- entity, which can now be any of 15 ids: husk, zombie, skeleton, stray,
  wither_skeleton, zombified_piglin, wolf, spider, polar_bear, vindicator,
  pillager, iron_golem, drowned, evoker, ravager.
- weapon, helmet, chest (`minecraft:air` allowed).
- texture: the fallback base skin, husk only.
- group: `infantry`, `ranged`, `support`, plus new `siege` and `cavalry`
  rows in the `groups` sheet.
- ability: now a reference into `abilities`.

**`abilities`** (new)

| Column | Type | Example (`pierce_longbow`) |
|---|---|---|
| id | S | `pierce_longbow` |
| mechanic | S | `pierce` (one of shield, mend, blight, stance, pierce, cleave, onhit_slow, pounce, aura, slam, fearless, mist) |
| cooldownTicks | I | 0 |
| radius | D | 2.0 |
| windupTicks | I | 0 |
| amount | D | 0.5 (a share, a heal or flat damage, depending on the mechanic) |
| statusTicks | I | 0 |
| statusLevel | I | 0 |
| knockback | D | 0.0 (negative values pull) |
| durationTicks | I | 0 |
| minRange | D | 0.0 |
| target | S | `enemies` |
| text | S | `Shots also hit the next enemy behind the target for 50%.` |

**`counters`** (new)
- Columns: `id S, attackerTag S, targetTag S, condition S, multiplier D, replaces S, text S`.
- Example: `{"id":"C1","attackerTag":"arrow","targetTag":"shield","condition":"raised_front","multiplier":0.35,"replaces":"none","text":"Raised shields stop most arrows."}`
- Conditions come from a closed list that `Combat.resolve` reads: always,
  raised_front, lowered_front, front, charge_impact, charge, hold, flank,
  no_sky, in_mist.

**`rarities`** (new)
- Columns: `id S, rollWeight I, chosenPrice I, slotWeight I, starPct2 D, starPct3 D, dupsStar2 I, dupsStar3 I, shardStar2 I, shardStar3 I, dismantleShards I, bannerMax I`.
- Example: `{"id":"rare","rollWeight":6,"chosenPrice":300,"slotWeight":1,"starPct2":0.03,"starPct3":0.06,"dupsStar2":1,"dupsStar3":2,"shardStar2":0,"shardStar3":0,"dismantleShards":4,"bannerMax":20}`

**`evolutions`** (new, one row per line for stage 2 and one for stage 3)
- Columns: `id S, troop S, stage I, name S, healthPct D, damagePct D, shards I, gold I, requiresMastery I`.
- Percentages are cumulative.
- Example: `{"id":"knight_3","troop":"knight","stage":3,"name":"Knight-Captain","healthPct":0.06,"damagePct":0.06,"shards":25,"gold":250,"requiresMastery":1}`

**`recruitment`** (new, one row; replaces `campaign.rollCost`)
- Columns: `id S, rollCost I, chosenCost I, epicPity I, legendaryShards I, renownForLegendary I, capPct D, maxStars I`.
- Example: `{"id":"houses","rollCost":100,"chosenCost":300,"epicPity":40,"legendaryShards":50,"renownForLegendary":30,"capPct":0.12,"maxStars":3}`

**`mastery`** (new): `id S, level I, kills I, matches I, reward S`.

**`renown`** (new): `id S, level I, points I, reward S`.

**`classes`** (changed)
- Added columns: `track S, active S, cooldownTicks I, durationTicks I, radius D, range D, value1 D, value2 D, passive S, passiveValue D, activeText S, passiveText S`.
- Example: `{"id":"champion","name":"Champion","group":"infantry","bonus":"...","track":"hero","active":"stand_fast","cooldownTicks":1800,"durationTicks":160,"radius":12.0,"range":0.0,"value1":0.25,"value2":0.0,"passive":"champion","passiveValue":4.0,"activeText":"Your troops within 12 take 25% less damage for 8 s; you gain Resistance I.","passiveText":"+2 hearts; nearby infantry +10% damage."}`

**`botBanners`** (new)
- Columns: `id S, house S, difficulty S, slots S`.
- `slots` holds 20 comma-separated ids; `^` marks a slot filled by the
  Epic or Legendary before it.
- Rows: one pure banner per House plus three mixed ones, for each
  difficulty.

Other changed sheets:
- **`factions`** becomes the side sheet **[B]**. It holds rows `gold`,
  `violet`, `crimson` and `teal` with `color`, spawn and barracks columns,
  and a match picks two of them. Only the display name changes, and the
  `troops` column is dropped.
- **`campaign`**: `rollCost` moves to `recruitment`. `duplicateRefund` and
  `starterGold` (both unused) are removed.
- **`combatDiscipline`**: `spellRadius`, `spellWindupTicks`, `healAmount`,
  `healRadius` and `abilityCooldownTicks` become defaults, used only when an
  ability row says `0`.
- **`economyModel`** (new, NOT_SHIPPED): the section 4.9 inputs.

Preflight additions:
- references: troop to house, troop to rarity, troop ability to abilities,
  evolveLine to evolutions;
- Commons and Rares must have an evolve line; Epics and Legendaries must
  have `none`;
- each House has exactly one Legendary, one Epic, two Rares and two Commons;
- cap check: `starPct3 + max stage pct <= capPct`;
- slot counts in botBanners.

### 6.2 Profile schema v2

Profiles are stored as today: server-owned JSON per player, saved through
`Campaign.save`.

```json
{
  "schema": 2,
  "gold": 420, "matches": 31, "wins": 17, "tutorialDone": true,
  "commander": "champion",
  "banner": ["knight","knight","knight","knight","sentinel","sentinel","archer","archer","longbow","mage","^","gl_marshal","^","^","^","revenant","revenant","bonebow","hexer","^"],
  "bannerColour": "gold",
  "owned": {
    "knight": {"copies": 3, "stars": 2, "stage": 2, "kills": 212, "survived": 30, "mastery": 2, "variant": "veteran"},
    "mage":   {"copies": 2, "stars": 2, "stage": 1, "kills": 0,   "survived": 22, "mastery": 0, "variant": "base"}
  },
  "shards": {"gilded": 34, "graveborn": 12, "wildmoot": 0, "ironclad": 3, "tidecallers": 0},
  "renown": {"gilded": 27, "graveborn": 9,  "wildmoot": 0, "ironclad": 1, "tidecallers": 0},
  "pity":   {"gilded": 12, "graveborn": 4,  "wildmoot": 0, "ironclad": 0, "tidecallers": 0},
  "pacts": 0,
  "progression": { "...": "Progression.State unchanged, schema 1" }
}
```

In this example banner, Gilded fills 15 slots (trait tier III) and Graveborn
fills 5 (no trait).

The old fields are dropped or derived: `unlocked` becomes the keys of
`owned`, `army` counts are derived from `banner`, and the per-faction
`deployment` is dropped. The sanitizer drops unknown ids, clamps stars and
stages to 1..3 (stage is always 1 for Epics and Legendaries), and rebuilds
any banner that breaks the slot rules.

### 6.3 Migration from schema 1 (12 Dawn/Dusk troops)

The migration runs once, when `schema` is missing or 1, before the
sanitizer. It keeps a copy of the v1 file next to it (`<uuid>.v1.json`).

| v1 id (faction) | v2 id | House / rarity | Rule |
|---|---|---|---|
| knight (Dawn) | knight | Gilded / Common | owned |
| archer (Dawn) | archer | Gilded / Common | owned |
| mage (Dawn) | mage | Gilded / **Epic** | not owned; refund 300 gold (Kort, 2026-10-17) |
| sentinel (Dawn) | sentinel | Gilded / Rare | owned if unlocked |
| longbow (Dawn) | longbow | Gilded / Rare | owned if unlocked |
| oracle (Dawn) | mage | merged | if unlocked: refund 300 gold |
| revenant (Dusk) | revenant | Graveborn / Common | owned |
| bonebow (Dusk) | bonebow | Graveborn / Common | owned |
| hexer (Dusk) | hexer | Graveborn / **Epic** | not owned; refund 300 gold |
| reaper (Dusk) | reaper | Graveborn / Rare | owned if unlocked |
| stalker (Dusk) | stalker | Graveborn / Rare | owned if unlocked |
| wraith (Dusk) | hexer | merged | if unlocked: refund 300 gold |

After the table is applied:
- All Commons of every House are granted. New profiles get the same 10
  Commons and nothing else.
- Gold, matches, wins, progression and titles are copied.
- `banner` is the v1 Dawn deployment if it was a valid 20-slot line,
  otherwise the Dusk one. Ids are mapped through the table, and slot
  patterns that break the rules are repacked.
- The class is mapped as in section 5.
- Shards, renown and pity all start at 0.
- The Founders ledger and claims are left untouched.

Standalone check (no game launch), in `test-tools/standalone`:
- fixtures: a fresh v1, a fully unlocked v1, a v1 with an invalid
  deployment, and a corrupt file;
- asserts: the mapping table above, idempotence (running it twice changes
  nothing), and that no v1 file is deleted.

### 6.4 Code touch list (for the plan writer, not a plan)

- `ArmyMobs` factory and 12 new `Army*` subclasses.
- `Combat.resolve` and the troop damage type.
- `Abilities`: one switch per mechanic, with data from the sheet.
- `Houses`: trait tiers per squad computed at reinforce, and `Houses.covered`
  for the `flank` condition (Bulwark).
- The `fearless` stance in `Combat.resolve`; `dread` is a damage aura only
  (no morale hook).
- `Anchor` record widening `reinforcementAnchors`.
- Fabric `ServerLivingEntityEvents.ALLOW_DEATH` for Rise Again.
- Death log for Grave Bloom.
- `Campaign.Profile` v2 and the migration.
- Recruitment, evolve and mastery screens (`RecruitmentScreen`,
  `CampaignLobbyScreen`, `ArmyCards`).
- State packet bump: the troop table length changes, and the client rejects
  mismatches by design.
- Bot banners in the `Campaign` bot logic.
- Harness ids in `test-tools`.

---

## 7. Implementation order

Each batch ends with a bot-vs-bot harness run (Phase 1 Task 5) and a
screenshot from a running isolated client, taken by a tester subagent.
Balance gate per House batch: that House's pure banner against the current
best pure banner at equal round gold, 20 sims, with a win rate between 40
and 60%.

| Batch | Content | Screenshot must show |
|---|---|---|
| 0. Engine | Sheets and preflight rules; profile v2 and migration with its standalone check; `ArmyMobs` factory; `Combat.resolve` and the damage type; `ranged` column; abilities shell (existing shield, mend and blight moved onto it, no behaviour change); daylight and water-conversion overrides; sides decoupled from Houses; House glyph in the HUD. | The camp recruitment screen with House tabs on a migrated v1 profile: Gilded and Graveborn rows filled from the old collection, the three new Houses showing their two Commons, and the gold carried over. |
| 1. Gilded Order (first; maps to Dawn) | 6 troops; ETF/EMF variants by name; team pennant via `teams=`; `stance`, `pierce` and `aura` mechanics; Bulwark covered flanks; counter rows C1 to C6 and C14 to C16; Champion and Marshal classes; Gilded bot banners. | A Gilded shieldwall under Graveborn arrow fire with the HUD trait tile "Bulwark III", the Lord Marshal's planted banner with its particles, and distinct Squire, Templar and Longbow silhouettes in one frame. |
| 2. Graveborn (maps to Dusk) | `ArmyZombie`, `ArmyStray`; `cleave`, `onhit_slow`, `fearless`; the Hollow King's `dread` (a damage cut); Rise Again via ALLOW_DEATH; Warlock class with the death log. | A Risen standing back up with soul-fire particles mid-fight, and Grave Bloom raising temporary Risen inside its circle. |
| 3. Ironclad | `ArmyVindicator`, `ArmyPillager`, `ArmyGolem`; ballista and mortar root overlays; `slam`; the mortar's minimum range; Entrench; counter rows C10 to C13 (structures are stubbed until Phase 4, so the Forward Supply banner is the first structure target); Quartermaster with the `Anchor` record. | A ballista crew and a mortar entrenched on an objective with the range ring larger than base, Brassjaw's slam knocking back a line, and a Forward Supply banner deploying reinforcements. |
| 4. Tidecallers | `ArmyDrowned`, `ArmyEvoker`, `ArmyRavager`; the `mist` zone, `harpoon` pull and `undertow`; Riptide speed, slow, and river fording in `Tactics.route`; trident projectile visual; Warden class. | Tidecallers fording the river next to a bridge while the enemy goes around, a mist cloud over an archer line, and the Brute's sweep. |
| 5. Wildmoot (last; most new quadruped models) | `ArmyBoarkin`, `ArmyWolf`, `ArmySpider`, `ArmyBear`; `pounce`, `ambush` (sky check), `howl`; Pack Hunters with the flank bearing; Reaver class. | Moot Hounds hitting an archer line from the flank with the Pack Hunters damage numbers shown (debug overlay), a Web Stalker on a wall, and Old Horn trampling. |
| 6. The chase | Recruitment screen (House rolls, pity counter, chosen buy); evolve screen; mastery tracking and titles; renown; shard ledger; Founders re-author; end-of-match report rows (trait uptime, shards, renown); economy model sheet and Monte Carlo tool; portraits for all 30. | A recruitment roll result card with the duplicate-to-star animation; an evolve confirmation from stage 2 to 3 with the new overlay on the portrait; and the Phase 2 playtest gate (full roster collected on a test profile, mixed vs pure banner sims). |

Portraits and the pixel icon set overlap with Phase 3. Each batch includes
its House's six portraits and glyph, so cards are never blank.

---

## 8. Risks and open questions

### 8.1 Builder decisions taken in this document (Kort may overturn)

1. **Trait tiers at 6 / 10 / 14 banner slots**, not 3 / 5 / 7 troops,
   because the banner has 20 slots. Alternative: count distinct troop types
   at 2 / 4 / 6, which rewards collecting breadth instead of banner depth.
2. **Epics fill 2 slots and Legendaries fill 4**, with at most one Legendary
   and two Epics per banner. This is how "Legendaries take a lot of the
   budget" is made concrete, together with their high round-gold cost.
3. **Sunweaver (`mage`) and Gravecaller (`hexer`) become Epics.** Migrated
   players do not keep them: each of mage, hexer and (if unlocked) Oracle and
   Wraith is refunded as 300 gold and earned again through rolls (Kort's
   decision, 2026-10-17). Everyone starts the chase with Commons only.
4. **Legacy ids are kept** for the 12 old troops (`knight`, `revenant`, and
   so on), and new troops get House prefixes. This avoids breaking harness
   ids and profiles.
5. **Roll weights 43 / 6 / 2 with Epic pity at 40**, chosen so the median
   full roster lands at about 29 hours with the Phase 1 rewards unchanged.
6. **The Founders chapter keeps its id.** Claimed tiers stay claimed, and
   total chapter gold drops from 600 to 300 in exchange for shards and one
   Rare.
7. **The team colour is the loudest read** (a tabard plus a new back pennant
   on every troop, via ETF `teams=`); Houses read by silhouette and
   material.
8. **Mob substitutions against the vision table.** Phantom, guardian and
   piglin brute are not used (flying, water-only and brain AI). The Gilded
   paladin is a husk champion instead of an iron golem, and the iron golem
   becomes Ironclad's Legendary. The ravager is the Tidecallers'
   kraken-touched brute. Wildmoot's alpha is a polar bear with antlers, and
   Wildmoot's hunters are boar-kin on the zombified piglin model. Ironclad
   are vindicators and pillagers.
9. **Class migration**: vanguard to Champion, marksman to Warden, arcanist
   to Warlock. The commander active goes on key G.
10. **`reinforcementAnchors` widens to an `Anchor` record** so the Forward
    Supply banner can join the list.
11. **Batch order** after Gilded and Graveborn is Ironclad, Tidecallers,
    then Wildmoot, ordered by model risk (humanoids first).
12. **Mastery counts kills per troop type**, credited to the type of the
    killing unit. Individual troops do not persist, so they cannot be
    tracked one by one.

### 8.2 Risks

| Risk | Impact | Mitigation |
|---|---|---|
| ETF `name=` and `teams=` rules may not apply to EMF overlay textures, or EMF variant models selected by name may not combine with Fresh Animations roots. | Houses and teams look identical in game. | Batch 1 spike: one husk with two name variants and a team pennant, screenshotted before any other art is made. Fallback: a per-troop texture through a renderer override for each mob (Java, more work). |
| Quadruped mobs (wolf, spider, bear, ravager) may not respond to `MoveControl.moveTo` the way humanoids do; wolves sit, bears stand up, ravagers roar when blocked. | The Wildmoot batch stalls. | Wildmoot goes last. The subclasses override those side effects in `tickMovement` (`setSitting(false)`, no `setWarning` outside slam), and each mob gets a 2-minute bot-vs-bot run before any art. |
| Attack animations: `swingHand` does nothing on the golem, ravager and bear. | Hits look like nothing happened (pillar 1). | `Abilities` sends `world.sendEntityStatus(mob, (byte)4)` for the golem and ravager, and a `setWarning` pulse for the bear. |
| Spiders climb walls, so they will ignore Siege gates in Phase 4. | Siege balance. | Accepted as Wildmoot's siege answer; Phase 4 walls get overhangs. Flagged for Kort (question 3). |
| Large hitboxes (golem 1.4, ravager 1.95, bear 1.4) jam formations and bridges; the bridge routing offsets assume 1.3 spacing. | Pathing breaks and troops get stuck. | Slot weights of 2 to 4 already space them out; Batch 3 adds formation spacing per troop width (derived from the entity type). |
| 15 base mob types and five new entity models raise client cost in 8-player, 160-troop fights. | Frame rate. | The Phase 1 tick-cost check is rerun per batch with mixed banners. |
| Per-hit counter checks (flank bearing, sky check) run for 160 troops. | Server tick time. | Every check is O(1) per hit; the Pack Hunters target counts are cached per 10-tick Tactics pass. |
| The economy model assumes the Phase 1 reward numbers, and Phase 4 bonus objectives add income. | The roster finishes faster than 25 hours. | The model sheet is rerun every time a gold source is added. |
| Legacy players lose their two starter casters at migration (refunded as gold). | A migrated banner has two empty slots until the first rolls. | Accepted by Kort 2026-10-17; the refund covers six rolls. |

### 8.3 Questions for Kort (answered 2026-10-17)

1. Trait counting: **banner slots at 6 / 10 / 14.**
2. Migrated Sunweaver and Gravecaller: **refunded, 300 gold each** (Oracle and
   Wraith too if unlocked); earned again through rolls.
3. Spiders climbing walls in Siege: **keep it.**
4. Legendary names: **approved** (Lord Marshal Aldric, The Hollow King, Old Horn
   the Antlered Alpha, Brassjaw the Forge Colossus, Admiral Vael the Drowned Tide).
5. Wildmoot as boar-kin: **approved.**
6. Chase length: **29-hour median accepted**; Epic weight stays 2.
7. Commander active key: **H** (G is the Attack order); Kort did not object.
8. Founders tier-20 Rare for pre-migration claimers: **no retro grant.**
9. Mark Prey glow: **vanilla glow**, visible to both teams; team-only glow parked.
