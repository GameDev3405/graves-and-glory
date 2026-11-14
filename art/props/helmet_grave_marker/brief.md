# Prop brief: Giant's Helmet Grave (`helmet_grave_marker`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a giant's barrel great helm, fallen and tipped onto its east side, half sunk in heaped earth, its face looking north; a broken giant's sword blade stands upright beside it as the grave marker, with a few stones and a posy in front.
- **Where it goes:** chapter 1 maps: a field or a hill crest, in the open where it can be seen; often near `cairn` or `rune_stone`. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 3 x 2 x 3. Front faces north. Anchor cell (1, 0, 1).
- **Scale against a troop:** 33 px tall, as tall as a troop (0.94 x the Knight); the drum is 30 px across: the cross-shaped eye slit is wider than a man's shoulders.
- **Silhouette at 15 m:** a flat-topped, straight-sided iron drum with chamfered corners leaning over at 22.5 degrees, a blade standing beside it: a fallen great helm, not a hut or a hive.
- **Close-up story at 3 m:** the cross-shaped eye slit and two breath slots on the face plate; the riveted brow band; earth heaped over the raised west foot; the broken sword blade with its dark fuller; the stones and the posy.
- **Materials and ramps (proplib):** `art/props/giant_relics.py` `old_iron`: dark iron `make_ramp("4C4A4E", 0.20)` with rust blooms about 0.06 and almost no moss (drum, band, rivets, face plate, blade); a near-black ramp for the slits and the fuller; earth from `giant_bones.mound`; stones from `make_ramp("6E6B66")`; posy as `offering_bowl`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** y=0: the centre, its four sides and the south-west cell (helm, heaped earth, the blade); y=1: the plus of the centre and its four sides (the tipped drum). The other corners stay open.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `giant_chain_link` (same iron), `stone_hand`; the forge set for density; the Knight on the card.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
