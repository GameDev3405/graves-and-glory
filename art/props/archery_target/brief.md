# Prop brief: Archery Target (`archery_target`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a straw butt on a wooden stand: a round coiled-straw boss tilted back 22.5 degrees on two leaning legs with a rear prop leg and a crossbar, the face painted with cream, red and blue-black rings, three arrows stuck in it.
- **Where it goes:** any House's drill yard and archery range; camps and lobby yards as set dressing, lined up opposite the training dummy.
- **Setting / House:** any House's drill yard
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** face 15 px across (about 0.95 blocks) centred at 18 px, top near 27 px against a Knight's 35 px; a troop stands in front and sees the whole face.
- **Silhouette at 15 m:** a round pale target on a leaning A-shaped stand with bright concentric rings that read as 'archery butt' at once.
- **Close-up story at 3 m:** coiled straw rim with strands, the painted rings, three arrows (dark, cream and cream fletching) at different depths, rope lashings where the legs meet the boss.
- **Materials and ramps (proplib):** `wood_y`/`wood_x` (legs, crossbar), shared `art/props/strawlib.py` straw (fixed warm yellow ramp) and rope; rings from `LINEN` (cream), `COAL` (centre), and a new `make_ramp("A8322A")` red; arrow shafts `make_ramp("B49A72")`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the ground cell (0,0,0) is solid so troops path around it and arrows stop on it; the boss and legs fill the upper cell only partly.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** stake_barricade for wood and lashing; the Knight on the card for scale; no outside assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
