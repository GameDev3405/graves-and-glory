# Prop brief: Signpost, Two Arms (`signpost_2`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the same timber signpost with two blank arm boards on the front, one pointing east (the viewer's left) and one pointing west, each with a carved arrow; a fork marker, no lettering.
- **Where it goes:** road forks and crossroads on any battle map; lobby and camp lanes as set dressing.
- **Setting / House:** any road or crossroads
- **Footprint (cells, x east * y up * z south):** 1 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 45 px to the finial (1.3 Knights); boards at 34-41 px and 26-33 px, 14 px long each.
- **Silhouette at 15 m:** a post with two flat pointed boards sticking out in opposite directions near the top, so it reads as a direction post at once.
- **Close-up story at 3 m:** the stepped points and engraved arrows, grain and splits on both boards, the rubble footing.
- **Materials and ramps (proplib):** same as signpost_1: `swood_*`, the carved board painter on `WOOD` (art/props/signlib.py), `stone`/`stone_top`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the ground cell (0,0,0) is solid (post and footing fill about 8x8); the two upper cells stay open.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `signpost_1` and `street_lamp`; the Knight on the card for scale; shared code art/props/signlib.py.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
