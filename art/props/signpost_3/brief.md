# Prop brief: Signpost, Three Arms (`signpost_3`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the same timber signpost with three blank arm boards: two on the front pointing opposite ways (east and west) and a third lower one on the back pointing east, each with a carved arrow; no lettering.
- **Where it goes:** main crossroads and village greens on any battle map; lobby and camp lanes as set dressing.
- **Setting / House:** any road or crossroads
- **Footprint (cells, x east * y up * z south):** 1 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 45 px to the finial (1.3 Knights); boards at 35-42, 23-30 and 15-22 px (the front pair spread well apart so it reads differently from signpost_2), 14 px long each.
- **Silhouette at 15 m:** a post bristling with three pointed boards at different heights: the most signposted thing in the lane.
- **Close-up story at 3 m:** three stepped points with engraved arrows, the lowest board on the back where only a walk-around finds it, grain and splits, the rubble footing.
- **Materials and ramps (proplib):** same as signpost_1: `swood_*`, the carved board painter on `WOOD` (art/props/signlib.py), `stone`/`stone_top`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the ground cell (0,0,0) is solid (post and footing fill about 8x8); the two upper cells stay open.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `signpost_1`, `signpost_2` and `street_lamp`; the Knight on the card for scale; shared code art/props/signlib.py.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
