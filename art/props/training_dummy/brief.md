# Prop brief: Training Dummy (`training_dummy`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a yard practice dummy: a post on a cross foot with a crossbar for arms, a straw-stuffed burlap sack body tied with rope, a stitched sack head and a battered round wooden shield hung from one shoulder.
- **Where it goes:** any House's drill yard and the Old Hold training ground; battle-map camps and lobby yards as set dressing.
- **Setting / House:** any House's drill yard
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 32 px (2 blocks), 3 px shorter than a Knight (35 px); crossbar arms 16 px wide, so a troop stands beside it at about its own height.
- **Silhouette at 15 m:** a straw-yellow man shape with outstretched arms and a round shield on one side, topped by a tuft; yellow against dark ground reads as a dummy at once.
- **Close-up story at 3 m:** straw strands spilling from the hem, wrists, shoulders and a torn chest patch; rope ties at waist and neck; the head sack has stitched eyes and mouth; the shield is gouged and iron-rimmed with a boss.
- **Materials and ramps (proplib):** `wood_x`/`wood_y`/`plank_x`/`plank_z` (post, bar, foot), `iron` (boss, rim); shared `art/props/strawlib.py`: straw (fixed 5-tone warm yellow ramp, strand pattern), burlap (`make_ramp("8A7860")`, weave), rope (`make_ramp("8E7448")`); shield uses `WOOD` with an `IRON` rim.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the ground cell (0,0,0) is solid, so troops path around it and arrows stop on it; the upper cell is open (head and arms are above head height of nothing a troop needs to pass).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** stake_barricade and weapon_rack for wood and rope; the Knight on the card for scale; no outside assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
