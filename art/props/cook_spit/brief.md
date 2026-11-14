# Prop brief: Cook Spit (`cook_spit`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a camp cook fire: two forked posts either side of a small fire in a ring of stones, an iron spit across the forks with a tied roast that turns slowly.
- **Where it goes:** the cook fire of an army camp, behind the tents beside the cauldron tripod and the camp stools; builders and map designers place it.
- **Setting / House:** any House: army camps, beside the cauldron tripod and stools
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** the forks stand 27 px, a little under a troop's shoulder (a Knight is 35 px); the spit sits at 22 px, waist-high to a cook; the roast is 16 px long and 8 px thick, a haunch two hands can lift.
- **Silhouette at 15 m:** two dark upright posts with a bar across them, a brown lump turning on the bar, an orange glow under it.
- **Close-up story at 3 m:** the charred side of the roast coming round as it turns, the twine bands and the knuckle bones, the crank on the east end and the lashings on the posts.
- **Materials and ramps (proplib):** `wood_y`, `wood_x`, `wood_z`, `endgrain` (posts, logs, crank grip), `stone` (ring), `iron` (spit, crank arm), `coal` and the `HOT` ramp (embers and flames, glow); new ramps: roast `make_ramp("9A5A2E")`, charred side `make_ramp("5E3418")`, bone `make_ramp("E2D6B8")`, twine `make_ramp("B8A57E")`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** both bottom cells (0,0,0) and (1,0,0) solid: a troop walks round the fire and the posts, never through them; the upper cells are open (arrows pass over the spit) and (0,1,0) holds the light block.
- **Light and effects:** light 12 at (0,1,0). The fire is a glow part with static flame tongues (animated flames are another branch's work). Effects copied from the live-fire brazier: flame and smoke particles, a rare lava pop, a quiet campfire crackle. The roast turns once every 8 s (`rotate` about the spit axis, docs/props/MOVING-PARTS.md).
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `cauldron_tripod` (fire ring, embers, flame tongues), `brazier` (effects), `great_banner` (moving parts); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
