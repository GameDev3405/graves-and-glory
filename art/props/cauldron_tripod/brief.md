# Prop brief: Cauldron Tripod (`cauldron_tripod`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** an iron cauldron hung on a chain from a lashed timber tripod over a ring of fire stones with glowing embers.
- **Where it goes:** the cook fire of an army camp: behind the tents, beside the stools and map table. Builders and map designers place it.
- **Setting / House:** any House.
- **Footprint (cells, x east * y up * z south):** 1 x 2 x 1. Front faces north. Anchor cell (0, 0, 0). The tripod stands about 28 px, the cauldron hangs between 5 and 17 px.
- **Scale against a troop:** pole tips at about 28 px, a little under a Knight (35 px); the cauldron is 12 px wide, a pot a cook carries in two arms.
- **Silhouette at 15 m:** a clear tripod (three thin stained legs meeting at a rope lashing) with a dark round pot hanging low in a ring of warm fire.
- **Close-up story at 3 m:** leg tips poking above two rope bands; a ring, chain and S-hook carrying the pot's bail; an octagonal near-black pot with rim lip, ears and stew with bubbles; flames licking the pot from every side; an ash disc, eight low soot-topped stones and stone foot pads.
- **Materials and ramps (proplib):** stained wood `SWOOD` (legs, seamless per step), `stone` (stones, pads), new ramps: `rope` 7A6038, `stew` 5B4630, `pot_iron` 3C3F47, `pot_soot` 2C2A2E, `ash` 4A4543; `iron`/`steel` (chain, hook, bail); glow part `cauldron_tripod_embers`: `embers`, `flame` (two crossed pairs, one at 45 degrees), the pot's fire-lit underside (`HOT`).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px.
- **Collision:** the one cell (0,0,0) solid: a troop paths around the fire. The upper cell (0,1,0) is open (troops and arrows pass above the stones) and holds the light block.
- **Light and effects:** light level 12 at cell (0, 1, 0). The embers are a glow part (`cauldron_tripod_embers`, full-bright). No particles.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `brazier` (coals and glow part); `stake_barricade` for timber; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
