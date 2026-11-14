# Prop brief: Medium Boulder (Mossy) (`boulder_medium_mossy`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a rounded natural medium boulder of grey stone with moss, built from chamfered (octagonal) masses with stepped tops, several turned 22.5 or 45 degrees (`boulderlib.boulder`); one of nine (3 sizes x 3 stones).
- **Where it goes:** battle-map dressing (fields, roadsides, hillsides, camp edges) and lobby grounds; placed by builders and map designers, rotated to any of 4 facings (the shape is irregular, so rotating gives variety).
- **Setting / House:** any battle map or lobby
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 2. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 24 px tall (a troop is 32-35 px): hip height, a troop looks over it.
- **Silhouette at 15 m:** a tall stone with lower bodies beside it and a pebble at its foot; asymmetric so it reads as natural.
- **Close-up story at 3 m:** a tall stone with lower bodies beside it, a pebble at the foot, the shoulder cut into the main stone.
- **Materials and ramps (proplib):** `GREY` stone ramp plus `MOSS` ramp `make_ramp("5F7A3A")` (the stone hand's moss): moss on the tops and in the cracks only, via `Rock`. No other materials.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the 4 ground cells are solid (the stone fills most of each); the top cell at y=1 is mostly air so it is not solid: arrows pass over the crown.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). It blocks movement and arrows by its solid cells only; no rule change.
- **References:** the stone hand's weathered stone and moss (`art/props/stone_hand`), the Knight on the card for scale; the approved forge set for density and light. No outside assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
