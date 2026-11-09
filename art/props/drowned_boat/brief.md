# Prop brief: Drowned Rowboat (`drowned_boat`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a small wooden rowboat, stove in and half sunk in mud at a river crossing, listing on its side, one oar snapped, left by raiders who came up the river.
- **Where it goes:** Rivergate Valley, chapter 1 battle 1 (the drowned coast): on banks and shallows by the crossings, off the main lanes. Kort places it.
- **Setting / House:** Rivergate crossings, chapter 1
- **Footprint (cells, x east * y up * z south):** 3 x 1 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** about 10-14 px high (the raised gunwale), so it sits below a troop's knees-to-waist: a boat that seats four, about 2.5 blocks long.
- **Silhouette at 15 m:** the pointed hull of a boat tilted on its side, bow toward the front (north): a long curve of planks, clearly a boat and clearly wrecked.
- **Close-up story at 3 m:** (1) the hole stove in the hull, broken plank ends showing; (2) the snapped oar; (3) mud and river water pooled in the low side.
- **Materials and ramps (proplib):** builder's choice from `MAT` (`plank_*`, `wood_*`, `swood_*`, `water`, `iron` for nails), plus a new mud ramp via `make_ramp` (name it here when you add it). Weathered and wet, darker than the forge's timber.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** all 6 bottom cells solid (the scaffold's default): troops walk around the hull, arrows at chest height pass over it. Keep the model in the bottom layer (y 0..16).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; `stone_hand` (Rivergate's other prop) for its weathered, mossy outdoor look; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
