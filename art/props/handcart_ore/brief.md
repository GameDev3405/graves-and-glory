# Prop brief: handcart_ore

- **Use:** dressing (battle maps and camps: read at 15-40 m, silhouette first; collision is a ground-level barrier only).
- **What it is, in one line:** the handcart heaped with grey ore, copper flecks showing. Loaded variant: the same base with ore in the bed.
- **Where it goes:** army camps, supply roads and siege approaches on any battle map; builders place it, nothing spawns it.
- **Setting / House:** any; plain wood, iron and undyed cloth, no house colours.
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 3. Front faces north (the handles point north). Anchor cell (0, 0, 1).
- **Scale against a troop:** side boards about 22 px, below a troop's shoulder; the cart is 2 blocks wide, 2 long plus handles.
- **Silhouette at 15 m:** a small box between two big wheels with two handles forward and legs behind.
- **Close-up story at 3 m:** spoked wheels with iron hubs, planked side boards with iron straps, rear legs and a grip bar between the handles, the load's detail (ore).
- **Materials and ramps (proplib):** `wood_z`, `wood_x`, `plank_z`, `plank_x`, `swood_z`, `swood_x`, `wood_y`, `iron`, `leather`; new ramps via `make_ramp` in art/props/cartlib.py (canvas, hay, ball, boulder, ore, copper, burlap as the variant needs).
- **Budget:** at most 96 elements, sheet 128 px.
- **Collision:** the cart body at ground level: 2 x 2 cells (z 1-2). The handle cell (z 0) stays open: handles are thin.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** the approved forge set for density and light; the Knight on the card for scale. Shared code: art/props/cartlib.py. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
