# Prop brief: Muster Board (`muster_board`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a standing timber notice board with a pinned map of the marches, a sealed contract held by a dagger, and loose notices and scraps.
- **Where it goes:** the Old Hold lobby hall, near the muster floor where players gather before a battle; back to a wall or free standing, front facing the players.
- **Setting / House:** the Old Hold lobby hall, a lived-in stone and timber hold where players gather before battles
- **Footprint (cells, x east * y up * z south):** 2 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 46 px, about 1.3 troops tall (a troop is 32-35 px): the roof beam is above head height so a player reads the notices at eye level (y 14..38 px) and the roof gives it a shelter shape.
- **Silhouette at 15 m:** a wide roofed board on two posts with feet: a tall rectangle with a roof overhang, light papers against dark timber.
- **Close-up story at 3 m:** (1) the map of the marches with a dashed red route ford-crossing the river to a marked target; (2) a contract with a red wax seal pinned by a dagger; (3) the ledge with an ink pot and a rolled scroll, and a crooked torn scrap.
- **Materials and ramps (proplib):** `wood_y`, `swood_x|z`, `plank_x|z`, `iron`, `steel`, `gilt`, `leather`, `linen`; new ramps via `make_ramp`: RED (9A3028, wax, route, marker), FIELD (6B7F45, map fields), ink pot (2C2A3A); paper painters use LINEN, LEATHER (ink), WATER, STONE.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the two ground cells (0,0,0) and (1,0,0) are solid: troops path around the board like a wall and arrows hit it; the cells above are left open (the board is a 2 px plank sheet and the roof is overhead).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (lobby use).
- **References:** sits beside the forge set (weapon_rack, shield_rack) for wood and iron look; the Knight for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
