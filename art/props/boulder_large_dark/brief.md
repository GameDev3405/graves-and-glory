# Prop brief: Large Boulder (Dark) (`boulder_large_dark`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a rounded natural large boulder of dark basalt, built from chamfered (octagonal) masses with stepped tops, several turned 22.5 or 45 degrees (`boulderlib.boulder`); one of nine (3 sizes x 3 stones).
- **Where it goes:** battle-map dressing (fields, roadsides, hillsides, camp edges) and lobby grounds; placed by builders and map designers, rotated to any of 4 facings (the shape is irregular, so rotating gives variety).
- **Setting / House:** any battle map or lobby
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 3. Front faces north. Anchor cell (1, 0, 1).
- **Scale against a troop:** 28 px tall, 3 blocks wide (a troop is 32-35 px): a low wide outcrop a troop can see over.
- **Silhouette at 15 m:** a low wide outcrop of chamfered masses, the highest toward the back, with rubble at one corner (no cap).
- **Close-up story at 3 m:** masses of different heights, rubble at the foot; cracks (moss-filled on the mossy stone).
- **Materials and ramps (proplib):** `DARK` ramp `make_ramp("55535C")`, basalt / old slag, violet-leaning shadows, via `Rock`. No other materials.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** all 9 ground cells solid, plus the 3 y=1 cells the highest masses fill (it is 1.75 blocks tall). The collision map matches the model.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). It blocks movement and arrows by its solid cells only; no rule change.
- **References:** the stone hand's weathered stone and moss (`art/props/stone_hand`), the Knight on the card for scale; the approved forge set for density and light. No outside assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
