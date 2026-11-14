# Prop brief: Signpost, One Arm (`signpost_1`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a timber signpost: a 4x4 stained post on a rubble footing with a small finial, carrying one blank weathered arm board on the front that points east (the viewer's left) and bears a carved arrow; no lettering.
- **Where it goes:** road verges, crossroads and village edges on any battle map; lobby and camp lanes as set dressing.
- **Setting / House:** any road or crossroads
- **Footprint (cells, x east * y up * z south):** 1 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 45 px to the finial (1.3 Knights, a little over a helm); the arm board sits at 33-40 px, at a Knight's eye level, and spans 14 px.
- **Silhouette at 15 m:** a thin dark post with one flat pointed board sticking out sideways near the top, topped by a small finial.
- **Close-up story at 3 m:** the board's stepped point and engraved arrow shaft with barbs, grain and splits on the board, stone rubble around the foot.
- **Materials and ramps (proplib):** `swood_y`/`swood_x` (post, cap), a board painter on the `WOOD` ramp with a carved arrow (art/props/signlib.py), `stone`/`stone_top` (footing, rubble).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the ground cell (0,0,0) is solid (the post and footing fill about 8x8 of it, so a troop paths around it and arrows stop on it); the two upper cells stay open (the board hangs over them).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `street_lamp` (same post and footing language), the Knight on the card for scale; shared code art/props/signlib.py.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
