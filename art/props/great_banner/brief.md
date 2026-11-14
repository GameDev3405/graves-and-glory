# Prop brief: Great Banner (`great_banner`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a tall war standard: a timber pole on a stone footing with a crossbar, a long swallow-tailed cloth banner (deep red, gold sun device and gold trim) hung from the bar, and a gilt spearhead finial; the cloth is its own part so it can be recoloured per team.
- **Where it goes:** camps and army spawn points, gate yards, Domination points, the Old Hold courtyard and hall; battle-map dressing where a rallying point needs a landmark visible across the field.
- **Setting / House:** neutral house colours for now (deep red cloth, gold device); team-colour cloths come later
- **Footprint (cells, x east * y up * z south):** 3 x 6 x 1. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** 96 px (6 blocks) tall, 2.7 Knights (35 px): the cloth is 28 px wide by 48 px tall (hangs from y 80 to y 32, so a troop can just walk under its lower edge), crossbar 40 px wide.
- **Silhouette at 15 m:** a tall pole with a gilt spearhead, a crossbar and a long red banner with a forked tail and a gold sun in it.
- **Close-up story at 3 m:** gold trim on the cloth border and swallowtail; gold sun device with rays; iron bands on the pole; gilt end caps on the crossbar; stepped stone footing.
- **Materials and ramps (proplib):** `wood_y` (pole), `wood_x` (crossbar), `stone` (footing), `iron` (bands, spear socket), `gilt` (caps, spearhead); new ramp `RED` = `make_ramp("8E1F26")` for the cloth field, with `GILT` for trim and device, painted by one `ClothPaint` painter on the cloth part only.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** only the pole's ground cell (1,0,0) is solid (the footing and pole fill most of it); the cells either side are open and the cloth hangs from y 32 up, above head height, so troops walk beneath it; arrows pass the cloth.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (not a gameplay prop).
- **References:** `tent_soldier` (canvas colour via make_ramp, big prop), `shield_rack` shields (house charge colours); the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
