# Prop brief: Wall Brazier (`brazier_wall`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a small coal basket hung on an iron bracket from a wall plate, coals glowing between its bars; no flame, harmless.
- **Where it goes:** gateway arches, halls and corridor walls on any map or the Old Hold, at head height above ground (hung at the cell, usually placed one block above the floor).
- **Setting / House:** Any House; gateways and halls
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 16 px tall, one block (0.46 of the Knight), 14 px deep out from the wall.
- **Silhouette at 15 m:** an orange point of light on a wall with a dark bracket under it.
- **Close-up story at 3 m:** riveted wall plate and stepped brace; coals glow between the bars; carrying hoop rim.
- **Materials and ramps (proplib):** `iron`, `steel` (rivets); `coal` (the glow part `brazier_wall_coals`); no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none (solid cells empty): a wall fitting that a troop would walk past.
- **Light and effects:** light level 11 on its own cell (0,0,0); no particles, no fire, no sound.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `brazier` (same basket, same coals); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
