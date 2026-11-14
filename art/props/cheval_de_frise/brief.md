# Prop brief: Cheval de Frise (`cheval_de_frise`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** gameplay (gameplay object (cover, barricade, siege piece, objective): collision IS the design; any rule change needs Kort).
- **What it is, in one line:** a sharpened-stake barrier: a heavy log with pairs of crossed stakes driven through it, points out on both sides, set across a camp edge or road.
- **Where it goes:** camp edges and approaches, gaps between tents and palisades, beside `stake_barricade`. Builders and map designers place it; nothing in the game spawns it.
- **Setting / House:** any House; plain field carpentry.
- **Footprint (cells, x east * y up * z south):** 3 x 1 x 1. Front faces north. Anchor cell (1, 0, 0). 48 x 16 x 16 px.
- **Scale against a troop:** about 15 px tall, under half a Knight (35 px): a troop sees over it; the sharpened points are at hip height.
- **Silhouette at 15 m:** a row of pale crossed-stake X shapes on a dark log, spiky in both directions.
- **Close-up story at 3 m:** the fresh pale heartwood of each point; iron straps binding the log; the stakes crossing through the log in X pairs.
- **Materials and ramps (proplib):** `stump` and `endgrain` (log), `wood_y` (stakes), `iron` (straps); new ramp `make_ramp("B49A72", 0.16)` for the cut points, as the stake barricade.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px.
- **Collision:** all 3 cells solid, like `stake_barricade`: troops must go around, arrows hit it. The model fills each cell only partly (an X of stakes around a log), which is intended; the collision map may warn about low fill.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none proposed beyond the stake barricade's (collision only); Kort decides if any.
- **References:** `stake_barricade` for the stakes, points and collision; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
