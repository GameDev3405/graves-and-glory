# Prop brief: Wall Lamp (`street_lamp_wall`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the wall-hung twin of the street lamp: an iron wall plate, a braced arm and a boxy iron lantern hanging on a short chain, with a glowing glass core; flush on a wall.
- **Where it goes:** gate and keep walls, house fronts, tavern doors, yard walls; placed at the back of its cell against a wall at any height the map wants (the lamp hangs about 14 px out).
- **Setting / House:** any
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 16 px (1 block) tall, 8 px wide, about 15 px out from the wall: a small hanging lantern mounted at head height or above (under half a Knight tall).
- **Silhouette at 15 m:** a small glowing box hanging off a wall on an arm; from the side, bracket, chain and lantern read as three separate parts.
- **Close-up story at 3 m:** riveted wall plate (2x2 rivets) with a boss; 45-degree stepped brace under a steel-topped arm; two chain links; the street lamp's lantern (shared proplib `lantern`): corner posts, amber glass on all four faces with a slow candle heart, a lit sill, pyramid cap and ring.
- **Materials and ramps (proplib):** `iron` (plate, boss, arm, brace, lantern frame), `steel` (rivets, arm top, chain link, ring); proplib `LAMP_GLASS` for the glow part `street_lamp_wall_glass` (own flicker order, `LAMP_FLICKER_WALL`); no other new ramps.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none (solid cells empty): it hangs on a wall; troops walk past and arrows are not stopped.
- **Light and effects:** one light block, level 13, at (0,0,0); no particles, no sound.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `brazier_wall` (wall-hung lit prop), `street_lamp` (same lantern); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
