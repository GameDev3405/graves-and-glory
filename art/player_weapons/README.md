# Player weapons

Ten player weapons, two for each of the five armour sets in `art/armor/`, made to the repository's weapon standard
(`docs/ART-STYLE-GUIDE.md` section 7) and the same rarity ladder as the armour.

Status: **art only, for Kort's look.** No items, renderer, recipes or gameplay change. They sit outside `art/weapons/`
on purpose: `tools/build_skins.py` builds every `.bbmodel` in that folder as a troop weapon.

| Set | Weapon | What it is | Rarity shows on | Legendary adds |
|---|---|---|---|---|
| Oathbound | **Oathblade** | Longsword: wheel pommel with a gilt sun boss, wound leather grip, straight cross with finials, a blade-root boss with the sun, a lit flat and a shade flat either side of a recessed fuller, stepped point. 24.5 px. | blade and guard steel, finials, pommel ring | sun-steel blade, ember heart in the boss, swept gilt wing quillons with ember tips, rays on the pommel and boss |
| Oathbound | **Sunward** | Heater shield: enamel field, steel rim, a gilt sun ringed round a gilt boss, planked wood back, a leather grip on brackets clear of the fist. | rim steel, the chief band across the top | four sun rays standing off the face with ember tips, crest finials, ember boss |
| Barrow Warden | **Gravereaper** | War scythe: black-wood snath with a side handle, iron socket, a bone skull at the blade root, a four-section curved blade (edge bright, spine rusted), iron butt spike. | blade iron, snath collars | grave-black iron, bone spines along the blade back with ember tips, an iron crown-collar, ember eyes in the skull |
| Barrow Warden | **Wakelight** | Lantern flail: short haft, a four-link chain (links alternate their turn like real links), an iron-barred lantern head with a bone candle inside and bone spikes. | iron, the last link | the lantern lit with ember, two more spikes, horned cap |
| Thornhide | **Tuskspear** | Boar spear: ash haft with rawhide binding, a boar-hair tuft, iron socket, bone lugs, a leaf head with a proud midrib, bone butt cap. | the iron head, the binding | charred head with ember veins, antler-tine lugs with ember tips, boar tusks at the butt |
| Thornhide | **Antlerbow** | Recurve hunting bow: four staves a limb, rawhide limb wraps, a leather grip, antler nocks with iron caps. Draw frames `pull_0` to `pull_2` flex the limbs and bring the string and a nocked arrow back. | limb wraps, nock caps | antler tines with ember tips |
| Forgewall | **Anvilfall** | War hammer: oak haft with a leather grip and brass collar, iron langet, a big square head with brass rivets, a scored striking face with brass rims, a curved beak and a top spike. | head iron, the head bands | soot-black iron, ember forge vents and a glowing face, two small smokestacks on the head |
| Forgewall | **Siegewright** | Crossbow: oak tiller with brass bands, butt, iron prod block, a two-section iron prod, stirrup, brass nut, trigger lever. Draw frames bend the prod and bring the string back to the nut; the bolt sits on the stock in `pull_2`. | prod iron, prod tip caps | a Forgeheart firebox under the tiller, an ember bolt head |
| Tidewrought | **Undertow** | Trident: kelp-wound driftwood haft, bronze crossbar with a scallop on it, a barbed centre tine and two side tines with outward barbs, kelp trailing from the bar. | bronze, crossbar bands | abyss bronze, ember points and pearl, bleached coral branches on the crossbar |
| Tidewrought | **Brinecutter** | Cutlass: a four-section curved blade widening toward the tip, a scallop-shell cup guard with barnacles, a knuckle bow, a bronze pommel. 24.2 px. | blade bronze, ricasso collar | abyss bronze, an ember edge at the tip, coral on the guard, an ember pearl in the pommel |

## Files

```
art/player_weapons/<weapon>/<weapon>.bbmodel            Common to Epic (Blockbench, format 5, free model)
art/player_weapons/<weapon>/<weapon>_legendary.bbmodel  Legendary
art/player_weapons/<weapon>/<weapon>.json               sidecar: grip, density 2, frames, draw data for the bow and crossbow
art/player_weapons/<weapon>/textures/<weapon>_<rarity>.png   64x128, 2 texels a px
art/player_weapons/<weapon>/textures/<weapon>_legendary_e.png   glow
art/player_weapons/<weapon>/textures/<weapon>_m.png, <weapon>_legendary_m.png   material maps (build_skins key colours)
art/player_weapons/review/<weapon>.png                  review sheet; review/lineup.png all ten
```

The projects follow `art/weapons/`: y up, the grip's centre at the origin (the 4 px fist hides y -2 to +2), the big
faces across z, Blockbench's south the front, one group per frame with only `rest` visible, cubes named
`<frame>/<cube>`. Every rarity of a weapon shares one UV layout.

The review sheets show front, the style guide's 45-degree view, edge, back, the 45-degree view in a 4 px fist, and a
small view. The bow and the crossbow add a draw row.

## Rebuilding and checks

```
python tools/build_player_weapons.py             # all ten, the sheets and the lineup
python tools/build_player_weapons.py anvilfall   # one
python tools/build_player_weapons.py --check     # build in memory and check only
```

Each weapon is defined in `tools/player_weapons/<set>.py`; `wengine.py` holds the cube, chain and export code and
`wpaint.py` the weapon painters (blade flats and fuller, whole-pixel grip wraps, wood grain). The build writes each
project and reads it back with the repository's own `tools/bbmodel.py`, and fails on any of these:

- a frame that is not one connected piece;
- faces that share a plane;
- a face whose UV is not exactly 2 texels a px;
- more than 16 cubes a frame (18 on Legendary), strings not counted;
- a sword over 24.5 px;
- glow below Legendary.

All ten pass.

## Rules followed and departures, for Kort

Followed: 2 texels a px on a 64x128 sheet, whole-pixel wraps at the hand, the face tone table with south as the front,
lit and shade blade flats with a fuller, wood grain as one line, tier cues on parts the fist does not cover, cube
budgets, Kort's 24.5 px sword length, and no team colour on a weapon.

Departures to rule on:
- **Glow.** Legendaries carry 10 to 66 lit texels (the Sunward's rays are the most).
- **Glow on draw weapons.** The Antlerbow's and Siegewright's Legendaries glow, though the troop sprite pipeline
  refuses glow on a draw weapon.
- **Rarity hues.** Epic violet and Legendary orange sit near team hues, as on the armour.
- **New families.** These are the first scythe, flail, spear, hammer, crossbow, trident, cutlass and shield in the
  roster. Section 7.3 says the first of each family Kort accepts becomes an approved construction.
- **Lower bow limb.** It is the upper limb mirrored, so its light falls from the lower left.
- **Lengths.** The pole weapons (scythe 34 px, spear 37.5 px, trident 38.5 px) have no length ruling; in-game they
  would be scaled by the item's display transform.
