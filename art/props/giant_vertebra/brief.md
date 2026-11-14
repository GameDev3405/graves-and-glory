# Prop brief: Giant's Vertebra (`giant_vertebra`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** one vertebra of a giant's spine standing in a mossy mound, its round ringed joint face to the front, the arch over the spinal canal rising to a sloped ridge, a narrow spine leaning back from it at 45 degrees and a thick wing to each side, the west wing snapped short.
- **Where it goes:** chapter 1 maps, Rivergate first: near the rib arch (a spine running away from the ribcage) and alone on banks and field edges. Part of the giant bone set (`giant_rib`, `giant_skull_fragment`, `giant_knuckle`).
- **Setting / House:** no House. The remains of the giants, older than the Houses (lore reboot 2026-11-09; nobody names the giants).
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** 47 px, 1.3 troops tall and 3 blocks wide; the drum alone is as tall as a troop's chest. A bone from a spine that would run the length of a field.
- **Silhouette at 15 m:** the classic vertebra: a round drum, the canal's arch on top of it closing in a two-slope ridge (a steep roof, not a flat cap), two heavy wings (10 px thick) and a tilted spine. The drum is an octagon (straight and 45-degree boxes), not 1 px stairs.
- **Close-up story at 3 m:** the joint face's growth rings, dark dimple and two radial cracks; the snapped west wing showing its spongy core, with a chip of it lying in the moss; lichen crusting the tops.
- **Materials and ramps (proplib):** the set's ramps in `art/props/giant_bones.py` (old ivory `make_ramp("BDB092", 0.17)`, marrow `make_ramp("9C8466", 0.18)`, lichen `make_ramp("8E9256", 0.16)`, the stone hand's moss and earth).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the middle column (x = 1, both z) at y 0-1, where the drum and arch stand on the ground, plus (1, 2, 1) where the ridge and spine fill it. The drum reaches 5 px into the side cells and the mound is 5 px: walkable. The card's walk-through WARN on the four side ground cells is the overlap count of the octagon boxes (each octagon is several overlapping boxes); the real bone there is a 4 px sliver.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `giant_rib` and `stone_hand` (same remains, same moss and earth); the forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
