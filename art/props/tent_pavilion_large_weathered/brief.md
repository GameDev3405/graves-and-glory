# Prop brief: Commander's Great Pavilion (Weathered) (`tent_pavilion_large_weathered`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** Colour variant of `tent_pavilion_large` (approved by Kort, 2026-11-13); the original stays unchanged. Geometry, collision and solid cells are identical; `art/props/tent_pavilion_large/model.py` is a thin wrapper that calls the source model with a `Palette`. Dull, sun-faded canvas for the scorched Ironhold siege valley: stripes muted to faded oxblood and unbleached linen (the roof, which takes the most sun, paler still), a ragged mud line and grime along the hem and valance tabs, and three patches on the walls and two on the roof. Reads as a campaign-worn command tent, not a circus tent.
- **Where it goes:** where a map has the block-built pavilion (compare fixture `structures/compare/tent_pavilion.nbt`, on props/maps-a), the centre of a large camp; `tent_pavilion` (9x8x9) stays as the small version for lesser captains. Builders and map designers place it. Neutral cream and dark red for now; team-colour variants come later.
- **Setting / House:** Ironhold (scorched siege valley) and any other siege camp
- **Footprint (cells, x east * y up * z south):** 15 x 12 x 15. Front faces north (the door). Anchor cell (7, 0, 7), which holds the centre pole. The block build is 15x13x15 including its one-block ground pad, so 15x12x15 above the pad: the same footprint and height.
- **Scale against a troop:** walls 56 px (3.5 blocks, almost two troops), eave and valance 61 px, roof to 152 px, finial tip 191 px: about 5.5 troops tall at the pole. Walls 9 blocks across, valance 11 blocks across (the block build's wall ring and eave). The door is 24 px wide and 46 px high.
- **Silhouette at 15 m:** a broad striped octagonal cone with a dagged valance skirt and a pennant on top, the biggest thing in the camp.
- **Close-up story at 3 m:** the door flaps folded back and tied with hemp; a red rug with a gilt border round the pole and the commander's iron-strapped chest by the back wall; the ropes running from under the valance to their pegs.
- **Materials and ramps (proplib):** palette ramps via `make_ramp`: faded oxblood 7A3F3A, unbleached linen CBBF9C, roof 86524A / D2C8A8, hemp 8E8068, mud 6A5841, patches CDC3A2 (new canvas) and 5F594A (tarred); wood, gilt, iron as the source.
- **Budget:** a flagship big prop (2700 cells): 27 roof rings at 3 px a step (8 faces, diagonal faces cut in two to fit a 3-cell part) and a 5-step crown, diagonal walls in 16 px pieces so the card's fill map stays true. 604 elements over 53 parts, sheet 1024 (canvas seen from outside and inside).
- Budget override: elements 920, sheet 1024
- **Collision:** solid (barriers) only where the canvas wall or the pole stands: the 23 cells the wall ring passes through at y=0..2 (closed corner to corner; the wall is 3.5 blocks tall, so y=2 is canvas too) and the pole column (7, 0..11, 7). The door cell (7, y, 3) and every interior cell are open, so a troop or player walks in through the door and round the pole. Ropes, pegs, flaps, the rug and the chest have no collision.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none; dressing.
- **References:** `tent_pavilion` (the small version, same look); the block-built pavilion it replaces (compare fixture above); the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light. No outside game's assets.
- **Done means:** the card passes every automatic check, the geometry, collision and solid cells equal `tent_pavilion_large`, and a fresh Sonnet-high reviewer passes the card against this brief.
