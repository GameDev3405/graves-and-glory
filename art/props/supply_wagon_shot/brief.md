# Prop brief: supply_wagon_shot

- **Use:** dressing (battle maps and camps: read at 15-40 m, silhouette first; collision is a ground-level barrier only).
- **What it is, in one line:** the supply wagon loaded with three big trebuchet stones and two pyramids of iron cannonballs. Loaded variant: the same base with shot in the bed.
- **Where it goes:** army camps, supply roads and siege approaches on any battle map; builders place it, nothing spawns it.
- **Setting / House:** any; plain wood, iron and undyed cloth, no house colours.
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 6. Front faces north (the shafts point north). Anchor cell (1, 0, 2).
- **Scale against a troop:** about 30 px to the side boards (a troop is 32-35 px): troop head and shoulders show over the sides; the bed is 2 blocks wide and 4 long, the shafts add 2 more blocks.
- **Silhouette at 15 m:** a long dark box on four wheels with two shafts reaching out the front, the load heaped above the sides.
- **Close-up story at 3 m:** spoked wheels with iron hubs, planked side boards with iron straps, a driver's bench and a yoke bar with legs under the shafts, the load's detail (shot).
- **Materials and ramps (proplib):** `wood_z`, `wood_x`, `plank_z`, `plank_x`, `swood_z`, `swood_x`, `wood_y`, `iron`, `leather`; new ramps via `make_ramp` in art/props/cartlib.py (canvas, hay, ball, boulder, ore, copper, burlap as the variant needs).
- **Budget:** at most 160 elements, sheet 256 px (4 spoked wheels cost about 56 elements; big props may carry an override).
- Budget override: elements 160, sheet 256
- **Collision:** the bed's footprint at ground level: 3 x 4 cells (x 0-2, z 2-5). The shaft cells (z 0-1) stay open because the shafts are thin and above head height 1; a troop paths around the bed, arrows hit it. The load fills cells above ground (y=1), which are open (WARN walk-through is intended: the bed is a ground-level barrier by design).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** the approved forge set for density and light; the Knight on the card for scale. Shared code: art/props/cartlib.py. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
