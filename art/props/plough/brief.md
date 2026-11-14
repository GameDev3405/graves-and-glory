# Prop brief: Plough (`plough`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a wooden ox-plough left standing in a field: an iron-pointed share on a runner, a landside plank and a stepped mouldboard, an iron coulter slanting to a long beam propped on a stake with an iron hitch bracket, and two stilts rising to a grip.
- **Where it goes:** ploughed fields, farm edges and village outskirts on any battle map; lobby yards as set dressing.
- **Setting / House:** any field
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 3. Front faces north. Anchor cell (0, 0, 1).
- **Scale against a troop:** 29 px tall at the grip (waist height on a Knight), 48 px long and 14 px wide: three blocks long, a plough fit for a pair of oxen.
- **Silhouette at 15 m:** a long low timber frame with a slanted pair of handles at one end and a pointed iron nose at the other.
- **Close-up story at 3 m:** the iron share and coulter, the stepped mouldboard, the landside plank, the hitch bracket at the beam tip, the stake propping the beam.
- **Materials and ramps (proplib):** `wood_z`, `swood_y`, `swood_z`, `plank_z`, `wood_y`, `wood_x` (frame), `iron`, `steel` (share, coulter, bracket).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the two rear cells (0,0,1) and (0,0,2) are solid (runner, landside, standards and stilts); the front cell (0,0,0) is open (a thin beam and stake), so troops can pass along the beam.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `handcart` and `stake_barricade` for wood; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
