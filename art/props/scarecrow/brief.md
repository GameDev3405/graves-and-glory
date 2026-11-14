# Prop brief: Scarecrow (`scarecrow`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a straw-stuffed scarecrow on a cross pole: a faded-blue ragged coat with a rust patch and hanging tatters, straw at the hem and hands, a stitched sack head under a battered felt hat and a crow perched on one arm.
- **Where it goes:** fields, farmyards and hay-meadow edges on any battle map; lobby yards as set dressing.
- **Setting / House:** any field
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 1. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** 45 px to the hat top (1.3 Knights); arms span 37 px, so a troop standing beside it is at about the figure's shoulder height.
- **Silhouette at 15 m:** a tall man shape with arms straight out, a wide hat and a dark crow on one arm, against the crop line.
- **Close-up story at 3 m:** tatters under the sleeves, the rust-red chest patch, stitched eyes and mouth, straw poking from hat, hem and hands, rope belt and cuffs, the crow.
- **Materials and ramps (proplib):** `wood_y`/`wood_x` (post, pole), shared `art/props/strawlib.py` straw, burlap and rope, new ramps `make_ramp("5A6E86")` coat, `"8A4A3A"` patch, `"4F4338"` hat; crow on `COAL`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** only the post cell (1,0,0) is solid (the post is 4x4 px); the side cells (the arms, at 23-29 px) are open, so troops and arrows pass beside it.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `training_dummy` (straw, sack head), `archery_target`; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
