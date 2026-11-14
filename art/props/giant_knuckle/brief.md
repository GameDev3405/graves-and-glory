# Prop brief: Giant's Knuckle (`giant_knuckle`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the knuckle end of a giant's finger bone breaking the ground: one fat round condyle knob half sunk in the earth, the shaft snapped off short as a thick stub rising east out of it, the rest of the shaft lying in the moss.
- **Where it goes:** chapter 1 maps, Rivergate first: scattered on banks, field edges and by crossings, often near `stone_hand` (a hint of a buried hand). Part of the giant bone set (`giant_rib`, `giant_vertebra`, `giant_skull_fragment`).
- **Setting / House:** no House. The remains of the giants, older than the Houses (lore reboot 2026-11-09; nobody names the giants).
- **Footprint (cells, x east * y up * z south):** 3 x 2 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** 29 px, nearly a troop's height; the knob is 24 px across and 30 px deep, the shaft 12 px thick. One end of one finger bone as tall as a man.
- **Silhouette at 15 m:** one round knob with a short broken stub pointing up and east: a bone, not a rock. The knob is an octagonal drum with a boss on each face; the stub is octagonal in section and leans at 67.5 degrees (rotated boxes, not stairs).
- **Close-up story at 3 m:** the stub's end showing the dark marrow core, with a thin splinter of its upper wall running on past the break; the fallen shaft lying in the moss, its break toward the stub; moss climbing the knob where it enters the earth.
- **Materials and ramps (proplib):** the set's ramps in `art/props/giant_bones.py` (old ivory `make_ramp("BDB092", 0.17)`, marrow `make_ramp("9C8466", 0.18)`, lichen `make_ramp("8E9256", 0.16)`, the stone hand's moss and earth).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** where bone is solid at ground level: the knob (x = 0 and x = 1, both z, y = 0). The stub is above head height over the open east cells. The card's walk-through WARN on (2, 0, 1) is the fallen shaft, 7 px tall: walkable, so no barrier. The 6 px mound is walkable.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `stone_hand`, `giant_rib` (same remains, same moss and earth); the forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
