# Prop brief: Milestone (`milestone`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a squared roadside marker stone: a wide mossy footing, a plinth, a body with a V notch and three number cuts on the front, and a flat overhanging cap; no lettering.
- **Where it goes:** road sides and crossroads on any battle map; lobby paths as set dressing, one every so often along a road.
- **Setting / House:** any road
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 28 px tall (0.8 Knight), 12 px wide at the foot: a troop sees it at thigh-to-waist height, as a waymarker should.
- **Silhouette at 15 m:** a grey square-shouldered post with a flat cap and green moss at its foot, readable as a waymarker (not a gravestone) against a verge.
- **Close-up story at 3 m:** the V notch and the three number cuts, 2 px clumps of moss creeping up the foot and on top, two fallen chips.
- **Materials and ramps (proplib):** `STONE` ramp with a rough-stone painter (pits, moss in clumps) and a carved front painter in `art/props/milestone/model.py`; moss is a new `make_ramp("56733A")`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the ground cell (0,0,0) is solid (the stone fills about 12x12 of it); the upper cell is open.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `stone_hand` and `boulder_small_mossy` for stone and moss; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
