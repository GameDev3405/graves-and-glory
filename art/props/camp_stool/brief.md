# Prop brief: Camp Stool (`camp_stool`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a small folding X-frame camp stool: two pairs of crossed legs bolted together, a slung leather seat across the tops.
- **Where it goes:** around camp fires, in front of tents, beside the map table. Builders and map designers place it; groups of two or three read best.
- **Setting / House:** any House.
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0). About 14 x 11 x 12 px: a stool sits in the middle of its cell.
- **Scale against a troop:** seat at about 11 px, a third of a Knight (35 px), knee height.
- **Silhouette at 15 m:** a small dark X under a flat brown seat; reads as a stool, never as an obstacle.
- **Close-up story at 3 m:** the iron pivot bolt where the legs cross, the leather seat sagging between its rails, wooden foot runners.
- **Materials and ramps (proplib):** `wood_y`, `swood_x`, `leather`, `iron`; nothing new.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px.
- **Collision:** none (`solid` is empty): a stool is walk-through, troops path across it, arrows pass. The collision map shows no solid cell.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `muster_board` leather and iron; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
