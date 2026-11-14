# Prop brief: Commander's Great Pavilion (`tent_pavilion_large`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the large version of `tent_pavilion` (Kort, 2026-11-13: the small one reads much smaller than the map's block-built pavilion): the same octagonal walk-in pavilion of striped cream and madder-red canvas, stepped concave roof, scalloped valance, centre pole with gilt finial and red pennant, open door with both flaps tied back, eight guy ropes, at the block build's size.
- **Where it goes:** where a map has the block-built pavilion (compare fixture `structures/compare/tent_pavilion.nbt`, on props/maps-a), the centre of a large camp; `tent_pavilion` (9x8x9) stays as the small version for lesser captains. Builders and map designers place it. Neutral cream and dark red for now; team-colour variants come later.
- **Setting / House:** any House's war camp
- **Footprint (cells, x east * y up * z south):** 15 x 12 x 15. Front faces north (the door). Anchor cell (7, 0, 7), which holds the centre pole. The block build is 15x13x15 including its one-block ground pad, so 15x12x15 above the pad: the same footprint and height.
- **Scale against a troop:** walls 56 px (3.5 blocks, almost two troops), eave and valance 61 px, roof to 152 px, finial tip 191 px: about 5.5 troops tall at the pole. Walls 9 blocks across, valance 11 blocks across (the block build's wall ring and eave). The door is 24 px wide and 46 px high.
- **Silhouette at 15 m:** a broad striped octagonal cone with a dagged valance skirt and a pennant on top, the biggest thing in the camp.
- **Close-up story at 3 m:** the door flaps folded back and tied with hemp; a red rug with a gilt border round the pole and the commander's iron-strapped chest by the back wall; the ropes running from under the valance to their pegs.
- **Materials and ramps (proplib):** as `tent_pavilion`: `make_ramp("E0D2AC", 0.16)` canvas, `make_ramp("8C2A25", 0.20)` madder red (24 stripes by angle round the pole, red crown), `make_ramp("A8977A", 0.16)` hemp; `wood_y` pole and door posts, `swood_y` pegs, `gilt` collar and finial, `plank_x`/`swood_x`/`iron` chest.
- **Budget:** a flagship big prop (2700 cells): 27 roof rings at 3 px a step (8 faces, diagonal faces cut in two to fit a 3-cell part) and a 5-step crown, diagonal walls in 16 px pieces so the card's fill map stays true. 604 elements over 53 parts, sheet 1024 (canvas seen from outside and inside).
- Budget override: elements 920, sheet 1024
- **Collision:** solid (barriers) only where the canvas wall or the pole stands: the 23 cells the wall ring passes through at y=0..2 (closed corner to corner; the wall is 3.5 blocks tall, so y=2 is canvas too) and the pole column (7, 0..11, 7). The door cell (7, y, 3) and every interior cell are open, so a troop or player walks in through the door and round the pole. Ropes, pegs, flaps, the rug and the chest have no collision.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none; dressing.
- **References:** `tent_pavilion` (the small version, same look); the block-built pavilion it replaces (compare fixture above); the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
