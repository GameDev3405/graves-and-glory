# Commander gear catalogue (approved by Kort 2026-11-03)

**Approved as drafted by Kort, 2026-11-03** (healer 4-piece cleanse, ranged 4-piece pierce). Source: the approved spec (`docs/superpowers/specs/2026-11-03-commander-gear-design.md`), the art guide (section 10.0 rules) and the six class actives in `Commander.java` and `sheets/design.json`. No numbers except cooldowns; balance comes later.

Art rules that apply to everything: Souls-like game art, each piece unique, 1 texel per pixel. No flag, pennant or standard on any back. Cloth parts (tabard, sash) take the side's colour; metal and leather are fixed paint. Gear is house-neutral, so it can leave the House art to the troops.

## 1. The four sets

### Bruiser: **Last Watch** (the garrison that does not leave the wall)
- **Fantasy:** a dented, stubborn wall of iron that gets hit so the troops do not.
- **2 pieces:** take less damage while enemies are around you on several sides.
- **4 pieces:** a taunt aura. Nearby enemies are drawn to you first.

| Piece | Look |
|---|---|
| Helm: Watchman's Greathelm | Squat iron bucket helm, one narrow eye slit, a cracked brow plate |
| Chest: Wallbreaker Cuirass | Slab breastplate with riveted bands and one huge pauldron |
| Legs: Gatepost Greaves | Thick plated thighs, leather straps, scuffed knee guards |
| Boots: Cairn-Treads | Heavy sabatons stacked like stones, mud-dark |

**Rarity looks:** Common: bare grey iron, plain rivets. Uncommon: a green-dyed leather strap and sash. Rare: blue-steel plates over the iron. Epic: purple inlay lines and a faint glow in the seams. Legendary: orange ember light in the cracks, plus horn-like crest and extra plate layers that change the silhouette.

### Healer: **Ashen Vigil** (the keepers who tend the fallen)
- **Fantasy:** a robed warden-priest of the lantern, who keeps the line alive.
- **2 pieces:** nearby troops slowly heal over time.
- **4 pieces:** fallen troops are revived faster, or harmful effects are cleansed (Kort picks).

| Piece | Look |
|---|---|
| Helm: Lanternbearer's Hood | Pale cowl over a half mask, a small hanging lantern charm |
| Chest: Vigilant Vestment | Layered robe over light mail, a belt of small vials |
| Legs: Candlewake Skirt | Long split robe, ash-grey wraps, scorched hem |
| Boots: Mourner's Sandals | Wrapped boots with soot-black soles and tiny bells |

**Rarity looks:** Common: plain grey-white wool. Uncommon: green thread at the hem. Rare: a blue sash and silver clasps. Epic: purple glass in the lantern and a soft glow. Legendary: warm orange light that lights the cowl from inside, with floating embers around the shoulders.

### Ranged: **Frostmarch Hunt** (hide-clad stalkers of the pass)
- **Fantasy:** a quiet hunter in hide and bone who deletes targets from far away.
- **2 pieces:** faster draw on bows and crossbows.
- **4 pieces:** shots pierce, or a volley of extra arrows (Kort picks).

| Piece | Look |
|---|---|
| Helm: Stalker's Maskhood | Hide hood, bone-plate half mask, antler nubs |
| Chest: Pass-Warden Jerkin | Layered leather with a quiver harness across the chest |
| Legs: Snowshod Leggings | Fur-trimmed leather, lashed bone guards |
| Boots: Silent Hunter's Boots | Soft tall boots with a bone buckle, frost-stained |

**Rarity looks:** Common: dull brown hide. Uncommon: green moss-dyed trim. Rare: blue-grey fur and carved bone. Epic: purple-lit rune scratches on the bone. Legendary: orange feather plume and a pale glowing eye on the mask.

### Commander: **Marchwarden** (the one the banner follows)
- **Fantasy:** the knightly leader whose presence makes a whole line march harder.
- **2 pieces:** bigger morale and order radius.
- **4 pieces:** faster formation and a stronger charge.

| Piece | Look |
|---|---|
| Helm: Marchwarden's Crown-Helm | Open-faced steel helm with a ridge crest and a thin crown band |
| Chest: Oathplate | Polished breastplate with a tabard over it, a sun-like plate on the chest |
| Legs: Kingsfield Cuisses | Plate over mail, a cloth-edged faulds skirt |
| Boots: Marching Sabatons | Clean steel boots with a gold spur |

**Rarity looks:** Common: plain steel, undyed tabard. Uncommon: green piping. Rare: blue enamel on the plates. Epic: purple gem at the crown and engraved filigree. Legendary: gold-orange trim, a glowing chest sun, and a tall crest that shows from the back of the field.

## 2. Weapons (11, about 10)

| Name | Family | Look | Behaviour |
|---|---|---|---|
| Oathkeeper's Longsword | Sword | Straight blade, plain cross guard, worn grip | Vanilla-feel melee |
| Last Watch Tower Shield (off-hand) | Shield | Tall iron slab with a dented rim | Blocks on right-click, then a short cooldown |
| Hollowfell Greatsword | Greatweapon | Black, long and heavy, notched along one edge | Slow, heavy hits with a small sweep; fills both hands |
| Ironhold Cleaver | Greatweapon | Great-axe, broad head, brass-banded haft | Same behaviour; two hands |
| Frostmarch Longbow | Bow | Tall bone-and-yew bow, lashed with sinew | Vanilla draw; draws faster |
| Rivergate Arbalest | Crossbow | Short iron stock, thick steel limbs | Slower, hits harder |
| Barrowleaf Quiver (off-hand) | Quiver | Hide quiver on the hip with feather fletching | Quiver for bow and crossbow users |
| Candlewake Staff | Healing staff | Gnarled wood, a small lantern head | Right-click heals the first friendly hit; left-click is a weak melee |
| Sunwell Crook | Healing staff | Pale shepherd's crook, a gold ring | Same behaviour |
| Marchwarden's Standard | War banner | Held pole and flag in the hand (never on the back) | Medium melee; doubles the radius of your set and relic auras while held |
| The Long Colours | War banner | Tattered hold banner, charred edge, iron spike | Same behaviour |

## 3. Relics (12)

### The 6 current class actives, as relics (today's effect, unchanged)

| Relic (was) | Effect | Cooldown |
|---|---|---|
| Ward of the Wall (Stand Fast) | Your troops within 12 blocks take 25% less damage for 8 s; you gain Resistance I | 90 s |
| Muster Seal (Muster) | Every group marches to you, 20% faster for 15 s | 120 s |
| Gravebloom Censer (Grave Bloom) | Up to 6 troops that fell nearby in the last 20 s rise as Risen for 30 s | 120 s |
| Forward Cache (Forward Supply) | Plant a 40-health supply banner far from your base; troops deploy there for 60 s while safe | 150 s |
| Hunter's Eye (Mark Prey) | Mark a foe within 40 blocks; you and your troops deal 30% more to it for 12 s | 60 s |
| Reaver's Fang (Bloodrush) | Speed II and 30% lifesteal for 10 s; each foe you kill returns a fallen troop, up to 3 | 75 s |

### 6 new relics

| Relic | Effect | Cooldown |
|---|---|---|
| Charger's Spur | Dash a short way forward | 10 s |
| Gatebreaker's Shove | Slam forward, knocking back and staggering foes in front of you | 15 s |
| Lantern of Mercy | A heal pulse on you and the troops near you | 30 s |
| Hunter's Volley | A fan of arrows from your position, in a cone | 25 s |
| Second Wind | Heal yourself over a few seconds | 45 s |
| Battle Cry | Nearby troops hit a little harder for a short time | 40 s |

## 4. Example perks (Epic and Legendary pieces only)

- **Second Chance:** once a match, the blow that would down you leaves you on a sliver of health instead.
- **Grudge:** you hit harder against whoever hurt a troop of yours.
- **Quickened Relic:** your first relic comes back sooner.
- **Heavy Hand:** every third swing of a greatweapon sweeps wider.
- **Last Light:** healing bolts also give the target a brief shield.
- **Hawkeye:** shots at a marked foe pierce through.
- **Oathkeeper:** troops close to you share some of your damage reduction.
- **Trailblazer:** your dash leaves a path that slows enemies crossing it.
