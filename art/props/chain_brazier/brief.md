# Prop brief: Chain Brazier (`chain_brazier`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a deep iron fire bowl with a banded belly and a rim hoop hanging by a stepped bail and two interlocked chain links from a ceiling plate, a static flame over a glowing coal bed; vanilla flame and smoke particles and a crackle.
- **Where it goes:** Old Hold great hall and keep halls: hang it under a ceiling 4 blocks above the floor (the ceiling plate is the top of the footprint).
- **Setting / House:** Any House; halls and keeps
- **Footprint (cells, x east * y up * z south):** 1 x 4 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 64 px (4 blocks) tall: the bowl hangs from y 33 to 46, so a troop (32 px) walks under it with a block of head room; the bowl is 14 px (0.9 block) wide.
- **Silhouette at 15 m:** a dark chain and arched bail dropping to a flared bowl with an orange flame in it.
- **Close-up story at 3 m:** coal bed glowing between rim and flame; steel band on the bowl belly; links of the chain interlocked at different angles; the bail steps over the fire.
- **Materials and ramps (proplib):** `iron`, `steel`; `coal` for the coals part; flame boxes use `Mat(HOT, 3|4|2)` (the forge fire ramp, full-bright). No new ramps. Parts: `chain_brazier` (iron), `chain_brazier_coals` (glow), `chain_brazier_flame` (glow, its own part so a live animated flame can replace the static one later).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** one barrier at [0,2,0], the bowl cell (arrows stop at the fire; a troop walks under it, the barrier is 2 blocks up); cells y=0 and y=1 are open air and walkable.
- **Light and effects:** light level 14 on [0,3,0] (the cell above the bowl; light cells are air only, so not the barrier cell). Effects copy the forge fire: `minecraft:flame` (2 every 16 ticks, chance 0.5), `minecraft:large_smoke` (1 every 12 ticks) and a `block.campfire.crackle` sound every 40 ticks at 0.7 volume.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `brazier`, `brazier_tall` (iron, coals look), `forge` (effects block); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
