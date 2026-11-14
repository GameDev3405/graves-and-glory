# Prop brief: Stocks (`stocks`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** village stocks: two stout posts on ground sills with a lower and a hinged upper board between them holding two ankle holes, iron hinge straps at one end, a small hasp and padlock at the other, a dark gap behind the holes, and a low bench behind.
- **Where it goes:** village greens, market squares and gatehouse yards on any battle map; lobby streets as set dressing.
- **Setting / House:** any village green
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 32 px tall (0.9 Knight), 2 blocks wide; the boards sit at 12-23 px, ankle-to-calf height on a seated troop.
- **Silhouette at 15 m:** two tall posts with a board frame between them at knee height and a low bench behind: a gate-like shape.
- **Close-up story at 3 m:** two 5x6 px ankle holes between the boards, iron hinge straps and a hasp and padlock, post caps, the bench behind.
- **Materials and ramps (proplib):** `swood_y`/`swood_x` (posts, caps), `wood_x`, `wood_y`, `plank_x`, `plank_z` (boards, bench, sills), `iron`, `steel` (hinges, hasp, padlock).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** both ground cells (0,0,0) and (1,0,0) are solid: the posts, boards and bench fill them (a troop paths around, arrows stop).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `weapon_rack` and `market_stall` for timber and iron; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
