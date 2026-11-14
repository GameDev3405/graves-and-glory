# Prop brief: Giant's Helmet Grave (`helmet_grave_marker`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a giant's dented iron helmet set crown-up on the earth as a grave marker, its face plate looking north, a few stones and a posy left in front.
- **Where it goes:** chapter 1 maps: a field or a hill crest, in the open where it can be seen; often near `cairn` or `rune_stone`. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 3 x 2 x 3. Front faces north. Anchor cell (1, 0, 1).
- **Scale against a troop:** 32 px tall, as tall as a troop (0.91 x the Knight), 40 px across: a man stands beside it and it reaches his head; the visor slit is wider than his shoulders.
- **Silhouette at 15 m:** a broad round dome of thin stacked rings with a comb along the crown and a dark visor slit over a nasal on the front: a helmet, not a hut or a skull.
- **Close-up story at 3 m:** the visor slit and the nasal; the dent flattening its east side; the stones and the posy of poppies laid in front.
- **Materials and ramps (proplib):** `art/props/giant_relics.py` `old_iron`: dark iron `make_ramp("4C4A4E", 0.20)` with rust and moss blooms at about 0.08 (dome, band, face plate, comb); a near-black ramp for the inside seen through the visor slit; moss, lichen, earth as the stone hand; stones from `make_ramp("6E6B66")`; posy as `offering_bowl`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** y=0: the centre and its four sides (the dome fills them); y=1: the east-west row through the centre (the dome's upper half). Corners stay open: only the octagon's cut edge and the mound are there.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `giant_chain_link` (same iron), `stone_hand`; the forge set for density; the Knight on the card.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
