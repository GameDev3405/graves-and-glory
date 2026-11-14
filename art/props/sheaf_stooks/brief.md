# Prop brief: Sheaf Stooks (`sheaf_stooks`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** three harvest stooks in a small cluster: each is four wheat sheaves stood up leaning together, bound with a rope band, with a spread cap sheaf of drooping ears on top; a few loose stalks lie on the stubble.
- **Where it goes:** harvested fields and farm edges on any battle map; lobby fields as set dressing.
- **Setting / House:** any harvest field
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 2. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 26 px to the cap top (0.74 Knight), each stook about 14 px across at the foot: knee-to-waist height for a troop.
- **Silhouette at 15 m:** three gold peaked haystack shapes, wide at the foot and capped, against dark stubble.
- **Close-up story at 3 m:** the four crossed sheaves with strand pattern, the rope band, the cap's drooping ears, loose stalks.
- **Materials and ramps (proplib):** shared `art/props/strawlib.py` straw (strand pattern) and rope; wheat ears a fixed paler gold ramp declared in the model.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** non-solid: `solid` is empty so troops path straight through the cluster; it is ground dressing, not cover (cover would be a gameplay change for Kort).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `training_dummy`, `archery_target` (same straw); `supply_wagon_hay`; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
