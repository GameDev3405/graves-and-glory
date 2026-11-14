# Prop brief: Wall Weapon Rack (`weapon_rack_wall`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** the wall-hung twin of `weapon_rack`: the same four weapons (arming sword, boar spear, hearth axe, longsword) resting on a rail frame over a plank back panel, flush on a wall.
- **Where it goes:** the yard and smithy walls of the Old Hold, barracks and gatehouse walls on any map; always at the back of its cell, against a wall.
- **Setting / House:** Any House; the yard and smithy
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 32 px (2 blocks) tall from y 2 to the spear tip at 32, as tall as a troop (35 px) and 2 blocks wide; same as the free-standing rack.
- **Silhouette at 15 m:** a wide timber frame with the spear head and axe poking above the top rail.
- **Close-up story at 3 m:** the four weapons match the free-standing rack, so the set reads as one; gilt guard on the longsword; iron pegs through the top rail.
- **Materials and ramps (proplib):** `plank_x` (back panel), `wood_y` (posts), `swood_x` (rails), `steel`, `iron`, `gilt`, `leather` (weapons); no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none (solid cells empty): it hangs on a wall 7 px deep at the back of the cell; troops walk past and arrows are not stopped.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `weapon_rack` (art/props/build_props.py): same weapons and timber tones; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
