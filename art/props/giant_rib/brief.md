# Prop brief: Giant's Rib (`giant_rib`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** one rib of a giant, its foot sunk in a mossy mound, rising straight and bending east in even 22.5-degree turns to a clean flat cut; octagonal in section, tapering toward the cut.
- **Where it goes:** chapter 1, Rivergate: a line of rib pairs makes the map's landmark arch over the river (the ribcage). Single ribs also stand alone on banks and field edges. Part of the giant bone set (`giant_vertebra`, `giant_skull_fragment`, `giant_knuckle`).
- **Setting / House:** no House. The remains of the giants, older than the Houses (lore reboot 2026-11-09: the land is a body; nobody names the giants).
- **Footprint (cells, x east * y up * z south):** 3 x 6 x 1. Front faces north. Anchor cell (1, 0, 0).
- **Chaining (the arch):** the top end is a flat vertical cut at x = 32 (east face of the cell column x = 2), y about 83..95, z 2..14 (12 px octagon), symmetric about the cell's centre in z. Place a second rib turned 180 degrees with its anchor 3 cells east of the first (same z): the two cuts meet face to face and the pair stands as one round arch, 6 cells tall over a 4-cell gap at the ground. Repeat pairs along the river (1 cell or more apart) for the ribcage.
- **Scale against a troop:** 96 px, so 2.7 troops (6 blocks) tall; the bone is an octagon 16 px across at the foot, tapering to 12 px at the cut. Huge next to a Knight, and an arch of two is tall enough to walk and ride under.
- **Silhouette at 15 m:** a pale curved tusk-like bone; paired, a great arch. Bends in even 22.5-degree turns (straight, then 22.5, 45, 67.5 and level, each step shorter than the last) with an octagonal, tapering section (two crossed boxes a step), so it reads as a round curved bone, never a bent square pillar or a staircase.
- **Close-up story at 3 m:** the cut end shows a pale rim round the dark spongy core; long weathering cracks run along the bone and grey-green lichen crusts its upper faces; moss and an earth stain climb its foot, and a shard broken off long ago lies in the moss.
- **Materials and ramps (proplib):** shared set ramps in `art/props/giant_bones.py`: `make_ramp("BDB092", 0.17)` old ivory (pale, warm, a little grey, so it reads as bone and not as stone or vanilla bone block), `make_ramp("9C8466", 0.18)` marrow core, `make_ramp("8E9256", 0.16)` lichen, and the stone hand's moss `make_ramp("5F7A3A", 0.18)` and earth `make_ramp("6E5038", 0.18)` (one world with the stone hand).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the rib's foot column (x = 0, y 0-2): troops walk round the foot and arrows hit it. Everything above is over head height and the mound (up to 8 px) is walkable, so the arch's span stays open; troops pass under it. The card's walk-through WARN on high cells comes from the leaning boxes being counted unrotated (cell fill ignores rotation); they are above head height.
- **Light and effects:** none. Old bone; stillness is the point.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells block movement and arrows like any wall.
- **References:** `stone_hand` (the same giants' remains, same moss and earth); the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
