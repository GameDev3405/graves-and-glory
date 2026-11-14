# Prop brief: Small Boulder (Dark) (`boulder_small_dark`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a rounded natural small boulder of dark basalt, built from chamfered (octagonal) masses with stepped tops, several turned 22.5 or 45 degrees (`boulderlib.boulder`); one of nine (3 sizes x 3 stones).
- **Where it goes:** battle-map dressing (fields, roadsides, hillsides, camp edges) and lobby grounds; placed by builders and map designers, rotated to any of 4 facings (the shape is irregular, so rotating gives variety).
- **Setting / House:** any battle map or lobby
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** about 10 px tall (a third of a troop), knee height: a troop steps around it, an arrow flies over it.
- **Silhouette at 15 m:** a low cluster of a tall stone, a wide low body and pebbles; reads as a lone field stone, not a block.
- **Close-up story at 3 m:** a few hairline cracks and one bedding line; chipped edges.
- **Materials and ramps (proplib):** `DARK` ramp `make_ramp("55535C")`, basalt / old slag, violet-leaning shadows, via `Rock`. No other materials.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** 1 cell at ground level: solid (a troop walks around it; its top is low but the cell is the smallest barrier).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). It blocks movement and arrows by its solid cells only; no rule change.
- **References:** the stone hand's weathered stone and moss (`art/props/stone_hand`), the Knight on the card for scale; the approved forge set for density and light. No outside assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
