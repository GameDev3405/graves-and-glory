# Prop brief: Brazier (`brazier`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** an iron fire basket of glowing coals on three legs: eight upright bars, rim hoop, carrying rings, no flame; harmless.
- **Where it goes:** gate yards, camps, the Old Hold courtyard and hall corners; any battle map as light dressing near a gateway or Domination point.
- **Setting / House:** Any House; yards and camps
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 26 px tall (0.74 of the Knight): hip height rim at about 24 px; the glow cell above it is the upper half of the 1x2x1 footprint.
- **Silhouette at 15 m:** a dark tripod stand under a flared basket with an orange glow in it.
- **Close-up story at 3 m:** coals glowing between the bars (a glow part, full-bright, so it reads at night); rim carrying rings; a ring brace between the legs.
- **Materials and ramps (proplib):** `iron` (stand, basket); `coal` (the glow part `brazier_coals`, hot gaps from the `HOT` ramp); no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** lower cell only (one barrier at y=0): the legs and basket base fill it; the upper cell holds only the open bowl and is left walkable (a troop can still not hop it from the front: the barrier is 1 block high). Arrows at head height pass.
- **Light and effects:** light level 13 on the upper cell (0,1,0); no particles, no fire, no sound: harmless.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** the forge coals (`coal` material) for the glow look; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
