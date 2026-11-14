# Prop brief: Tool Rack (`tool_rack`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a wall board of smithing and camp tools: a sledge, a pair of tongs, a cross-peen hammer, a shovel and a two-man crosscut saw lying on the foot shelf, hung from iron pegs on a top rail.
- **Where it goes:** Old Hold smithy and yard walls, camp stores, gatehouses; always at the back of its cell against a wall.
- **Setting / House:** Any House; smithy and stores
- **Footprint (cells, x east * y up * z south):** 2 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 32 px (2 blocks) tall and wide; tools run 16-22 px long, roughly forearm-to-leg length, readable beside the Knight (35 px).
- **Silhouette at 15 m:** a dark board with grey tool heads hanging in a row under the rail and a long grey saw along the bottom.
- **Close-up story at 3 m:** the saw's teeth and wooden handles, the steel strike faces of the sledge, the pivot of the tongs, the T grip of the shovel; iron pegs on the rail.
- **Materials and ramps (proplib):** `dark_plank` (new Mat on the `SWOOD` ramp, planks, tone 1 so the hafts stand out), `swood_x`, `wood_y`, `wood_x`, `iron`, `steel`, `leather`; no new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none (solid cells empty): hangs on a wall at the back of the cell (shelf 6 px deep); troops walk past and arrows are not stopped.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `weapon_rack_wall` (same wall-board build); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
