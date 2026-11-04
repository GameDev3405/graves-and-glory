# House gear sets: five new armour sets (draft, 2026-11-04)

**Status:** draft for Kort. Kort approved the idea on 2026-11-04: five House-themed player armour sets, added as new sets beside the four approved ones in `docs/drafts/gear-catalogue-2026-11-03.md`. Nothing here changes the four approved sets. Every open choice is marked **Kort decides**.

Art rules carry over from the catalogue: Souls-like game art, each piece unique, 1 texel per pixel, no flag, pennant or standard on any back. The art for the Common and Legendary looks is already made. Rarity tiers: Common grey, Uncommon green, Rare blue, Epic violet, Legendary orange. **Epic violet and Legendary orange appear on trim only**, never as a body colour. Legendary player armour may glow more than the 16-texel troop cap (Kort-approved).

## 1. How these fit the real data model

Checked against `sheets/design.json` (`gearSets`, `gearItems`, `gearSetBonuses`, `gearPerks`) and `Gear.java`, `GearStats.java`, `GearEffects.java`, `GearCombat.java`.

- **A set** is a `gearSets` row plus 4 `gearItems` rows (helm, chest, legs, boots) and two `gearSetBonuses` rows (2 and 4 pieces). Each item has Health, Damage, Armour, Speed, Ranged, Healing and Command fields. Rarity then multiplies them (Common 1.0 to 1.1, Uncommon 1.15 to 1.3, Rare 1.35 to 1.55, Epic 1.6 to 1.85, Legendary 1.9 to 2.2). Epic and Legendary pieces carry one perk each.
- **Set-bonus mechanics that already exist (8):** `surrounded_guard`, `taunt_aura`, `heal_over_time`, `cleanse`, `draw_speed`, `pierce`, `command_radius`, `formation_charge`. **Every bonus below uses one of these.** Only the values and the pairing change. No new mechanic.
- **Perks that already exist (8):** Grudge, Second Chance, Heavy Hand, Trailblazer, Quickened Relic, Last Light, Hawkeye, Oathkeeper. The perks below are picked from those eight.
- **Stat budget:** each new piece uses the same total points as its nearest existing set, spent on the same existing fields. Numbers are Common-roll proposals only; balance is closed, so they get no sim pass. Damage is left at 0 on all armour, as in the four approved sets.
- **Starter fields:** the five sets are not starter sets, so `classes` is `-` and `starterMain` and `starterOff` are `-`. Existing profiles still map to the original four.
- **Role field:** the loadout's "role" is the id of the strongest set (`GearStats.role`, `WarTablePages.java` line 1158 compares it to the set id). The lobby seat icon reads `table/icon-class-<first class>` (`WarTableLobby.roleIcon`), so each new set needs a role icon or a fallback. Flag for the build step, not a design choice.
- **Not 4 roles, 9 sets:** "Your role is your strongest set" still holds. A House set is a role built from the same pieces, with its own twist. The gear screen's set progress line ("Oathbound 2 of 4") works as is.

## 2. The five sets

### Oathbound (Gilded Order): **the knight who holds the line's heart**
- **Fantasy:** a white-and-gold paladin who steadies the troops around them, a defender where Marchwarden is a driver.
- **2 pieces:** `command_radius`. Your command aura reaches 3 blocks further (Marchwarden gives 4).
- **4 pieces:** `heal_over_time`. Your troops within 8 blocks heal 0.5 health every second (Ashen Vigil gives 1 and does not extend the aura).
- **Nearest existing:** Marchwarden, by look and by the aura. **How it differs:** Marchwarden makes the line faster and harder-hitting; Oathbound makes it tougher and longer-lived. It has no speed or damage bonus, and its stats lean on Armour, not on a plain Command number.
- **Role in play:** the sturdy anchor of a big formation. Weaker than a dedicated healer, stronger on armour.

| Piece | Look | Common stats |
|---|---|---|
| Helm: Oathbound Greathelm | White-lacquered great helm, gold brow band, a narrow vision slit | Health 2, Armour 1, Command 1 |
| Chest: Dawnwall Cuirass | White plate with a gold sun medallion and a short cloth tabard | Health 3, Armour 3, Command 2 |
| Legs: Vowkeeper Tassets | White plate over mail, gold-edged faulds, cloth side panels | Health 2, Armour 2, Command 1 |
| Boots: Pilgrim Sabatons | Clean white greaves with gold toe-caps and a small gold spur | Health 1, Armour 1, Command 1 |

**Perks (Epic and Legendary):** helm Oathkeeper, chest Second Chance, legs Grudge, boots Quickened Relic.
**Rarity looks:** Common: plain white-grey steel, no gold. Uncommon: green cloth tabard and sash. Rare: blue enamel on the white plate, silver edging. Epic: violet gem in the brow band and violet inlay lines (trim only). Legendary: orange-gold trim, a glowing chest sun (above the 16-texel troop cap), a gold halo-ring crest above the helm.

### Barrow Warden (Graveborn): **the dead who will not step back**
- **Fantasy:** a grave-guardian in bone and rusted iron who stands in front of the fallen and draws the enemy.
- **2 pieces:** `cleanse`. Every 8 s, slow, weaken and bleed leave you and your troops within 6 blocks (Ashen Vigil gives 5 s and 8 blocks, at 4 pieces).
- **4 pieces:** `taunt_aura`. Foes within 6 blocks are drawn to fight you first (Last Watch gives 8).
- **Nearest existing:** Last Watch, for the taunt. **How it differs:** a smaller taunt with no damage reduction, backed by the weaker cleanse at 2 pieces. It draws the enemy and the cold of the barrow shrugs off what hits you, but it is a softer wall than Last Watch. Stats lean on Health, with a little Healing.
- **Role in play:** the sticky tank for a Graveborn-style swarm. It loses to a flank, which Last Watch does not.

| Piece | Look | Common stats |
|---|---|---|
| Helm: Barrow Skull-Mask | Bone skull mask riveted to a rust-red iron cap, hollow eye sockets | Health 3, Armour 2 |
| Chest: Tombplate | Rusted iron breastplate with ribs of bone laid over it, a cracked burial seal | Health 5, Armour 3, Healing 1 |
| Legs: Ossuary Greaves | Rusted plate over grave-wrap cloth, bone knee guards | Health 3, Armour 2, Healing 1 |
| Boots: Crypt-Treads | Heavy rusted boots, strips of burial linen at the ankle | Health 2, Armour 1 |

**Perks:** helm Quickened Relic, chest Second Chance, legs Oathkeeper, boots Last Light.
**Rarity looks:** Common: pale bone and flaking brown rust. Uncommon: green moss and verdigris in the cracks. Rare: blue-grey cleaned iron, polished bone. Epic: violet glow in the eye sockets and in the burial seal (trim only). Legendary: orange ember light in the eyes and ribs (glow above the troop cap), extra ribs and a bone crown that change the silhouette.

### Thornhide (Wildmoot): **the cornered beast that charges**
- **Fantasy:** a fur-clad brawler with a boar's snout and antlers, who is hardest to put down when surrounded and whose pack rushes after them.
- **2 pieces:** `surrounded_guard`. Take 20% less damage while 3 or more foes are within 4 blocks (Last Watch gives 30%).
- **4 pieces:** `formation_charge`. Troops in your command aura move 10% faster and hit 15% harder on Attack (Marchwarden gives 15% and 10%).
- **Nearest existing:** Frostmarch Hunt for the fur, bone and antlers; Last Watch and Marchwarden for the mechanics. **How it differs:** Frostmarch is a quiet archer; Thornhide is a melee brawler that wins when the fight is messy. Its charge trades speed for bite, and it needs a command aura to matter, so it stays a leader's set, not a solo set.
- **Role in play:** the loud, close-in fighter who leads the rush.

| Piece | Look | Common stats |
|---|---|---|
| Helm: Boar-Snout Mantle | Boar skull with a long snout and tusks, a fur ruff behind it | Health 2, Armour 1 |
| Chest: Thornhide Hauberk | Layered pelts over leather with bone thorns on the shoulders | Health 4, Armour 2 |
| Legs: Bramble Leggings | Fur-lined leather, lashed thorn guards and rope | Health 2, Armour 1, Speed 0.01 |
| Boots: Moot-Runner Boots | Fur-cuffed boots with hoof-like soles | Health 1, Speed 0.02 |

**Perks:** helm Grudge, chest Last Light, legs Heavy Hand, boots Trailblazer.
**Rarity looks:** Common: dull brown fur and raw hide. Uncommon: green moss-dyed rope and lacing. Rare: blue-grey winter fur and polished tusks. Epic: violet rune scratches on the tusks and bone thorns (trim only). Legendary: antlers appear on the helm (a new silhouette), the snout and eyes glow orange (above the troop cap), and the thorn shoulders grow larger.

### Forgewall (Ironclad): **the armoured turret**
- **Fantasy:** a soot-black plated gunner behind a brass grille, who pulls foes onto a fortified line and shoots through them.
- **2 pieces:** `taunt_aura`. Foes within 5 blocks are drawn to fight you first (smaller than Last Watch's 8).
- **4 pieces:** `pierce`. Your arrows pierce through 1 foe (same as Frostmarch Hunt).
- **Nearest existing:** Frostmarch Hunt for the pierce; Last Watch for the taunt. **How it differs:** Frostmarch gets its pierce with a faster draw and light armour. Forgewall has no draw bonus, wears the heaviest armour of any ranged build and draws attention first, so it shoots from the front, not from cover. Armour is its main stat, with a modest Ranged number.
- **Role in play:** the commander who stands at the wall and fires through the crowd.

| Piece | Look | Common stats |
|---|---|---|
| Helm: Grille-Box Helm | Square box helm, a brass grille over the face, rivets in rows | Health 2, Armour 2, Ranged 1 |
| Chest: Furnace Cuirass | Thick riveted plate, a brass vent plate on the chest, soot at the seams | Health 4, Armour 3, Ranged 2 |
| Legs: Boilerplate Cuisses | Overlapping riveted plates, brass hinges | Health 2, Armour 2, Ranged 1 |
| Boots: Anvil-Treads | Flat iron sabatons with a brass heel plate | Health 1, Armour 1, Ranged 1 |

**Perks:** helm Hawkeye, chest Second Chance, legs Heavy Hand, boots Quickened Relic.
**Rarity looks:** Common: dull black iron with plain rivets. Uncommon: green-dyed leather straps behind the plates. Rare: blue-steel plates and bright brass. Epic: violet light in the grille slats and seams (trim only). Legendary: orange furnace glow behind the grille and in the seams (above the troop cap), plus two small smokestacks on the shoulders that puff a thin smoke trail.

### Tidewrought (Tidecallers): **the fleet's captain who strikes and moves**
- **Fantasy:** a bronze-helmed sea commander in coral and kelp, whose line moves like a tide.
- **2 pieces:** `draw_speed`. Bows draw 15% faster (Frostmarch gives 25%).
- **4 pieces:** `formation_charge`, speed only. Troops in your command aura move 15% faster, with no damage bonus (value 2 is 0).
- **Nearest existing:** Marchwarden for the pace aura; Frostmarch Hunt for the draw speed. **How it differs:** Marchwarden's charge adds damage and Thornhide's adds bite; Tidewrought's is pure speed, so it is the tempo set. It draws a little faster than normal, not as fast as Frostmarch. Stats lean on Speed, Ranged and Command.
- **Role in play:** the quick skirmish leader who repositions the line. A weak front-liner.

| Piece | Look | Common stats |
|---|---|---|
| Helm: Tidecaller's Hoplite Helm | Bronze Greek helm with a horsehair crest of dried kelp, cheek plates | Health 1, Ranged 1, Command 1 |
| Chest: Reefscale Cuirass | Bronze muscle cuirass, coral growth on one shoulder, kelp belt | Health 2, Ranged 2, Command 2 |
| Legs: Brinewrap Greaves | Bronze greaves over wet leather wraps, small shells | Health 1, Ranged 1, Command 1, Speed 0.01 |
| Boots: Wavestep Sandals | Bronze-strapped sandals with kelp ties | Health 1, Command 1, Speed 0.02 |

**Perks:** helm Hawkeye, chest Last Light, legs Trailblazer, boots Quickened Relic.
**Rarity looks:** Common: dull green-brown bronze, no coral. Uncommon: green kelp trim and ties. Rare: blue-polished bronze and pale coral. Epic: violet pearl at the brow and violet shell inlay (trim only). Legendary: bright orange-gold bronze, glowing coral on the shoulder (above the troop cap), and a tall kelp-and-coral crest.

## 3. Summary

| Set | House | 2 pieces | 4 pieces | Nearest existing | Twist |
|---|---|---|---|---|---|
| Oathbound | Gilded Order | command radius +3 | troops heal 0.5/s within 8 | Marchwarden | Armoured aura anchor, no speed or damage |
| Barrow Warden | Graveborn | cleanse every 8 s within 6 | taunt within 6 | Last Watch | Sticky tank without damage reduction |
| Thornhide | Wildmoot | 20% less damage when surrounded | troops +10% speed, +15% damage | Frostmarch Hunt / Last Watch | Brawler that wins messy fights |
| Forgewall | Ironclad | taunt within 5 | arrows pierce 1 | Frostmarch Hunt | Armoured gunner at the wall |
| Tidewrought | Tidecallers | bows draw 15% faster | troops +15% speed | Marchwarden | Pure tempo, weak front-liner |

No new cooldowns. The only timed effect is `cleanse`, which already has its own timer in the catalogue's Ashen Vigil.

## 4. House restriction: recommendation

- **Option A (recommended): open to all.** Any commander can roll and wear any House's set. This matches the catalogue rule that gear is house-neutral and fits the goal that gear works for any House's commander. It needs no new data. The sets are themed for a House and a player who wants the full theme can still pick the matching one.
- **Option B: restricted to its House.** Only a commander of that House can equip the set. This adds a `house` column to `gearSets`, a check in `Gear.java`, and a rule for bots, and it makes the sets worth less for players of other Houses. It also risks a bad roll leaving a player with unusable gear.
- **Option C: open to all, with a small drop tilt toward your own House.** The roll weights favour the commander's own House's set. This adds a new weighting rule, which is a new system, so I do not recommend it.

**Kort decides:** A, B or C.

## 5. Open choices (Kort decides)

1. **House restriction:** A (open to all, recommended), B or C, above.
2. **Set names and piece names:** every name here is a draft.
3. **Bonus pairings:** each 2-piece and 4-piece pairing and value (for example Forgewall's 2-piece taunt at 5 blocks, or Tidewrought's speed-only charge). The alternative for Forgewall is draw speed 15% at 2 pieces, which makes it a plain Frostmarch with plate.
4. **Thornhide and Tidewrought both reuse `formation_charge`:** accept the reuse with different values, or swap Thornhide's 4-piece for a different existing mechanic.
5. **Perk assignment:** the perk on each piece is a proposal from the existing eight.
6. **Common stat numbers:** proposals only, at the same budget as the nearest set. They are not tuned and balance stays closed.
7. **Role icons:** a new icon per set, or one shared fallback icon for all five, for the lobby seat and the War Table.
8. **Legendary glow size:** how far above the 16-texel troop cap each Legendary piece may glow (Kort approved glowing more; the size per set is not set).
9. **Arrival timing:** whether the five sets ship in the 1.0 catalogue or after (this raises the armour count from 16 to 36 items, each with 5 rarity looks).
