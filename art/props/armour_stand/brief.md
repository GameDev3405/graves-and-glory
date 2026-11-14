# Prop brief: Armour Stand (`armour_stand`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a timber cross-foot stand wearing a full dull-iron plate harness with a mail skirt, a faded tabard, a great helm with a T-slit and a faded plume; the arms hang from a crossbar whose tips show.
- **Where it goes:** Old Hold hall and armoury walls, a keep's great hall, any lobby room; a figure standing against a wall or beside a door.
- **Setting / House:** Any House; halls and armouries
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 32 px tall (2 blocks, 0.91 of the Knight at 35 px): helm top at 32, shoulders at 25, a man-scale dummy slightly shorter than the Knight.
- **Silhouette at 15 m:** a helmed, shouldered iron figure with a red plume on a small cross-foot, narrower at the feet.
- **Close-up story at 3 m:** the chest ridge and steel knee and elbow cops against dull plate; the crossbar tips poking past the hanging arms; mail skirt check and the faded tabard.
- **Materials and ramps (proplib):** `plate` (new Mat on the `iron` ramp, light-side column a tone up), `mail` (iron ramp, 1 px stagger check), `steel` (cops, ridge, gorget, nose bar), `plank_x`/`plank_z`/`wood_y`/`wood_x` (stand), `leather`, `gilt` (buckle), `soot` (slits); new ramp `PLUME = make_ramp("7A2A24")` for plume and tabard.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** both cells of the 1x2x1 footprint solid ([0,0,0] and [0,1,0]): the figure fills both; troops path around it and arrows hit it.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `weapon_rack`, `brazier` iron; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
