# Prop brief: Soldier's Tent (`tent_soldier`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a soldier's ridge tent of undyed canvas: low walls, a sagging roof, a ridge pole, guy ropes to pegs, and a front door flap rolled up and tied open.
- **Where it goes:** battle maps: army camps behind a front line, a Domination or Siege camp, a roadside bivouac. Builders and map designers place it.
- **Setting / House:** any House; neutral undyed canvas, team colours come later.
- **Footprint (cells, x east * y up * z south):** 5 x 4 x 7. Front faces north. Anchor cell (2, 0, 3).
- **Scale against a troop:** ridge at 48 px, pole top at 51 (1.45 troops tall); 3 cells wide (48 px) plus rope room; a troop walks in through a 18 x 32 px door at the front.
- **Silhouette at 15 m:** a pale triangular-roofed canvas shape with a dark door opening on the north end and a ridge pole, ropes fanning to pegs.
- **Close-up story at 3 m:** the rolled flap tied with two ropes above the door; a bedroll and groundsheet seen through the door; seams in the canvas and pegged guy ropes.
- **Materials and ramps (proplib):** new ramps via make_ramp: canvas BCB08E, hemp rope 8E7448, groundsheet 5E5240, wool 6A5B4C; `wood_y`, `wood_z` for posts and pole.
- **Budget:** a big prop cut into 12 regions: the splitter multiplies long elements.

Budget override: elements 140, sheet 256
- **Collision:** full barrier cubes on the tent body cells only (x 1..3, z 1..5, y 0..1); none on the rope and peg cells; ropes are walk-through. The canvas cells are hollow, so the collision map warns about low fill: the tent reads solid from outside.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; stake_barricade for rotated slabs; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
