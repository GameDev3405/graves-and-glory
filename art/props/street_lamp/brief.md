# Prop brief: Street Lamp (`street_lamp`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a medieval street lamp: a stained-timber post on a small stone footing, four iron corbels carrying a platform, and a boxy iron lantern with a glowing glass core under a pyramid cap; not Victorian, no scrollwork.
- **Where it goes:** town streets, the Old Hold courtyard and gate approaches, camp lanes; battle-map dressing along roads, gatehouse yards and Domination points that need a night light.
- **Setting / House:** any
- **Footprint (cells, x east * y up * z south):** 1 x 3 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 48 px (3 blocks) tall, 1.4 Knights (35 px): the lantern sits at 36-44 px, just above a raised Knight's helm, so it lights a lane from head height up; the post is 4 px thick like a fence post.
- **Silhouette at 15 m:** a thin dark post topped by a wider boxy lantern with a warm yellow glow and a small pyramid roof.
- **Close-up story at 3 m:** four iron corbels under the lantern platform; two iron bands on the post; lantern corner posts with the glass seen between them; stone footing.
- **Materials and ramps (proplib):** `wood_y` (post), `stone` (footing), `iron` (bands, corbels, lantern frame and roof), `steel` (rivets); one new fixed warm yellow ramp `GLASS` for the glow part `street_lamp_glass` (lantern glass, full-bright).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the single ground cell (0,0,0) is solid: the post and footing fill about 4x4 of it (an arrow or a troop bumps the post); the two upper cells stay open (light cell at y=2 holds no block).
- **Light and effects:** one light block, level 14, at (0,2,0) (the lantern cell); no particles, no sound.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `brazier` (lit prop with a glow part), `brazier_wall`; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
