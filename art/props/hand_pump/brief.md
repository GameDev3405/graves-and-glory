# Prop brief: Hand Pump (`hand_pump`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a village water pump: a banded timber column on a stone base with an iron head, a spout reaching over a stone trough full of water and a long lever tilted up over the trough.
- **Where it goes:** village greens, farmyards, camps and market squares on any battle map; lobby yards as set dressing.
- **Setting / House:** any village or farmyard
- **Footprint (cells, x east * y up * z south):** 2 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 48 px (3 blocks, 1.4 Knights) at the lever tip, the head at 40 px; the trough rim is 9 px, below a Knight's knee; 2 blocks wide.
- **Silhouette at 15 m:** a tall timber column with a diagonal lever and an iron spout, standing beside a low trough: reads as a pump from far away.
- **Close-up story at 3 m:** iron bands on the column, the spout and its drop lip over the trough water (with ripple glints), the lever pins, brick courses on the base and trough.
- **Materials and ramps (proplib):** `swood_y`, `wood_x` (column, lever), `iron`, `steel` (bands, head, spout, pins), `stone`, `stone_top` (base, trough), `water` (trough).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** both ground cells (0,0,0) and (1,0,0) are solid (trough and base); the upper cells are open; a troop paths around the pump, arrows stop on it.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `quench_trough` (trough and water); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
