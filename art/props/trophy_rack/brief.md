# Prop brief: Trophy Rack (`trophy_rack`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a wall trophy board of captured arms: a spear and a sword crossed behind a battered enemy helm (dented dome, rust, snapped crest), a dark-green and an oxblood captured round shield below.
- **Where it goes:** Old Hold hall and gatehouse walls, barracks and keeps; always at the back of its cell against a wall.
- **Setting / House:** Any House; halls and gatehouses
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 32 px (2 blocks) tall and wide, 1 px short of troop height 35; the helm is about man-head size scaled up 1.5x so it reads as a trophy.
- **Silhouette at 15 m:** a framed timber board with a stepped crown, a grey helm in the middle and two round shields at the lower corners.
- **Close-up story at 3 m:** the dented helm with rust patches and a snapped crest; the oxblood shield's pale cross and the green shield's pale slash (enemy colours); the sword's gilt pommel behind the helm.
- **Materials and ramps (proplib):** `plank_x`, `wood_y`, `swood_x` (board), `iron`, `steel`, `soot`, `leather`, `gilt`; new ramps `ENEMY = make_ramp("3B4D38")`, `ENEMY2 = make_ramp("5E2A2A")` (captured shields) and `RUST = make_ramp("8A4B2A")`; patterns for the shield faces return `LINEN` texels.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none (solid cells empty): hangs on a wall 10 px deep at the back of the cell; troops walk past and arrows are not stopped.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `weapon_rack_wall` (same wall-board build and timber); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
