# Prop brief: Smoke Vent (thick) (`smoke_vent_thick`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** an effect-only prop (invisible, no model): thick pale-grey smoke for a busy chimney, a bakehouse or a big hearth.
- **Where it goes:** on top of a chimney, forge hood or hearth, or in the ground at a vent; placed by builders and map designers. Effects run within 24 blocks of a player.
- **Setting / House:** any.
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** invisible; smoke rises from 0.2-0.4 blocks above the cell floor, so place it at the chimney mouth.
- **Silhouette at 15 m:** the smoke column itself: campfire_signal_smoke plus campfire_cosy_smoke, a dense tall column.
- **Close-up story at 3 m:** the smoke drifts and thins; no sound.
- **Materials and ramps (proplib):** none (no elements).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none: `solid` is empty, so troops and arrows pass through.
- **Light and effects:** vanilla particles only: campfire_signal_smoke plus campfire_cosy_smoke, a dense tall column. No fire, no light, no sound.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** the forge def's smoke effect (`large_smoke` from the hood) for the black vent; vanilla campfire smoke for the others.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
