# Player armour sets

Five new player armour sets, one per House, built to sit next to Kort's first set (the crimson knight with the great
helm, sash and tabard). Each set has the four vanilla slots (helmet, chest, legs, boots), every rarity, every team
colour, and a Legendary form with its own extra geometry.

Status: **art only, for Kort's look.** Nothing here is wired into the game: there are no items, no renderer and no
change to gameplay, saves or protocol. Kort's first set is not in this repository, so these follow it from the review
sheet image: same rarity ladder, same sheet layout, same 1 texel per model px.

| Set | House | Silhouette, read from six blocks | Team cloth | Legendary |
|---|---|---|---|---|
| **Oathbound** | Gilded Order | Upright white enamel knight. Keeled armet with a single sight and breaths on the right cheek, gilt brow pivots, team horsehair plume falling behind the helm. Gilt sun on the breastplate, broad three-piece pauldrons with gilt rims, gilt-cuffed vambraces with steel gauntlet lames, couters. Sword belt with a gilt buckle, knee-length surcoat skirt split per leg, poleyns, sabatons with toe caps. | plume, cape, surcoat skirt | **Dawnward**: ivory enamel, seven-ray gilt sun halo behind the head with ember tips, stepped gilt wings rising from the pauldrons, ember heart in the breast sun, taller plume, ankle cape with the sun embroidered on it. |
| **Barrow Warden** | Graveborn | Hunched and ragged, lopsided. Team grave-hood open at the face over a cracked bone skull mask; rusted sallet underneath. Bone sternum and ribs strapped over a darkened, rust-bloomed cuirass. A bone skull pauldron looking outward from the right shoulder, a bare rusted spaulder on the left. Bone splints on the forearms. Chain belt with a tiny skull buckle, grave-moss rag, mail chausses, bone kneecaps, iron boots with bone toe-claws. | hood and hood tail, mantle, torn back-drape | **Lich-warden**: grave-black iron, broken iron crown (one spike snapped) with an ember gem, ember in the mask's sockets and in the skull pauldron's, ember between the ribs, horns on the skull, spikes on the spaulder, drape to the ankle. |
| **Thornhide** | Wildmoot | Hunter in hide and fur. Fur hood over a dark hide mask with ochre clay warpaint (a broken brow stripe, claw stripes under the right eye), a bone boar snout with tusks, pricked ears, a three-feather fan on the left. Hide lamellar laced in rawhide, fang-and-claw necklace, ragged fur collar, a lamellar pad on the right shoulder and a whole pelt with its forepaw and claws on the left. Lamellar bracers with crossed lacing, wrapped hands. Bone-toggle belt, wrapped shins, fur-cuffed wrapped boots. | cloak, loincloth with the tusk chevron | **Antlerking**: charred hide, branching antlers with ember tips, ember warpaint and pupils, longer tusks, bone spikes on the pad, longer cloak. |
| **Forgewall** | Ironclad | Wide and square. Flat-topped box helm with a brass face grille, a projecting riveted brow and riveted cheek plates. L-section slab pauldrons that square the shoulders. Riveted cuirass with a side hinge and a brass furnace-grille plackart; gorget. Big square gauntlets with brass knuckle bars. Leather belt with a brass buckle, knee-length scale skirt split per leg with a brass hem, knee plates, square boots with toe caps and side buckles. | apron and back tabard, both with the anvil mark | **Forgeheart**: soot-black iron, ember-lit helm and chest grilles, ember vents in the pauldron plates, twin brass-banded smokestacks with ember mouths rising behind the shoulders. |
| **Tidewrought** | Tidecallers | Drowned marine. Corinthian-cut bronze helm (T opening, swept cheek pieces) under a fin crest of bronze spines webbed in team cloth, the crest running on down the nape. Bronze scale coat with a scallop-shell pectoral, scallop-shell pauldrons crusted with barnacles, scale sleeves and bronze vambraces, kelp trailing from the forearms. Team sash knotted at the left hip, navy breeches, scale tassets, bronze greaves with shell knee cops, bronze boots with calf fins. Verdigris pools in the lowest laps. | fin crest webbing, sea-cloak with a wave-cut hem, sash | **Abyssal**: abyss-dark bronze, taller crest with an ember spine, ember eyes, an ember pearl at the shell's hinge, bleached coral branching from both shoulders with ember polyps, longer kelp and cloak. |

## Rarity

The same ladder as the first set, so a player reads rarity the same way on every set:

- **Common**: the set's own metal; trim bands in a plain material (bare steel, rusted bronze, rawhide, polished steel, a
  bronze rim).
- **Uncommon**: the trim bands turn green.
- **Rare**: the metal is blued; the trim turns steel blue.
- **Epic**: the metal is blackened toward violet; the trim turns violet.
- **Legendary**: the set's own dark or precious metal, ember-orange trim, a few glowing (`_e`) texels and extra
  geometry.

Only the `plate` and `accent` materials change with rarity. Bone, fur, shell, brass, gilt, leather and cloth stay the
same at every tier, so the House reads at every rarity. The trim bands (`accent`) are placed where they read from a
distance: helm brow bands, pauldron and lame edges, waist bands, lacing, knee and boot cuffs, and crest spines.

## Files

```
art/armor/<set>/<set>.bbmodel              Common to Epic geometry (Blockbench, free model), Common/crimson embedded
art/armor/<set>/<set>_legendary.bbmodel    Legendary geometry, Legendary/crimson embedded
art/armor/<set>/textures/<set>_<rarity>_<team>.png   5 rarities x 4 teams (crimson, teal, gold, violet), 128x64
art/armor/<set>/textures/<set>_legendary_e.png       the glow mask (emissive texels only)
art/armor/review/<set>.png                 the review sheet: 7 rows x 5 views + a thumbnail
art/armor/review/lineup.png                all five sets, Common and Legendary, side by side
```

Every rarity and team of a set shares one UV layout, so swapping the texture swaps the look. The Legendary project uses
the same layout; its extra cubes have their own space in it.

The projects use Blockbench's own space: y up, feet at y 0, the player faces north (-z), and the player's right arm is at
+x. Bones are grouped as the vanilla player model (`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`) at
the vanilla pivots, and each bone holds one group per slot (`helmet`, `chest`, `legs`, `boots`). The sash, apron,
loincloth, rag and tabard pieces hang from `body` like the first set's tabard. Skirts that would clip through a walking
leg are split per leg.

## Rebuilding

The sets are defined in code, so every detail is reviewable and reproducible:

```
python tools/build_player_armor.py              # every set, the textures, projects, sheets and the lineup
python tools/build_player_armor.py thornhide    # one set
python tools/build_player_armor.py --check      # paint and check only (writes nothing)
```

`tools/player_armor/<set>.py` holds a set: its palette and every box with its painters, with comments for each detail.
`tools/player_armor/paint.py` holds the material patterns (plate, mail, scales, lamellar, cloth, leather, fur, bone,
rust, patina, barnacles, trims and rivets). `tools/player_armor/engine.py` holds the boxes, the atlas, the Blockbench
export and the turntable renderer used for the sheets. The build is deterministic: marks are seeded per face.

The build reports, and exits non-zero on, any two faces that share a plane and would z-fight. That covers the armour against itself and against
the player's own body.

## Style rules followed

From `docs/ART-STYLE-GUIDE.md`: 5-tone hue-shifted ramps per material, light from the top left (front and the
viewer's-left side lit, the far side dark), bevel highlights on top-left edges and shadows on bottom-right edges, one
pattern per material, marks placed irregularly (never on a fixed period), no pure black or white, no gradients or noise,
whole-px boxes at 1 texel per px, and the team-tint saturation clamp on every non-team ramp. Team colour appears only
on cloth.

Known departures, for Kort to rule on:

- **Epic violet and Legendary orange** sit within 45 degrees of the violet and gold/crimson team hues, which the tint
  rule forbids for troop skins. The first set already uses them as rarity signals, so they are kept, unclamped, on the
  trim bands only.
- **Glow count.** The `_e` mask holds 29 to 61 lit texels per Legendary across all faces of the texture (Barrow Warden
  29, Forgewall 30, Tidewrought 46, Oathbound 49, Thornhide 61). The troop cap is 16, and no single view shows
  every lit texel. Trim them if the cap should apply to player armour too.
- **Team marks**: Forgewall's anvil and Thornhide's tusk chevron are drawn in the team cloth's own dark tones, not in
  another colour.
