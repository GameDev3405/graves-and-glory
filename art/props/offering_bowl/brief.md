# Prop brief: Giant's Offering Bowl (`offering_bowl`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a giant's huge carved stone bowl on a short stem, its rim man-high, with small offerings people still leave: tallow candles, coins, a posy.
- **Where it goes:** chapter 1 maps: a village edge, a crossroads shrine, a quiet clearing; it doubles as a shrine piece in the Old Hold. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 3. Front faces north. Anchor cell (1, 0, 1).
- **Scale against a troop:** rim at 34 px, so a troop's eyes are just under it (1 troop tall to the rim, 40 px with the rim candles); the bowl is 42 px across. A bowl made for giant hands, used by men as an altar.
- **Silhouette at 15 m:** a wide octagonal bowl on a narrow stem: a giant's chalice, the dark water just showing over the rim from above.
- **Close-up story at 3 m:** candles on the plinth, on the ground and two on the rim with wax run down the wall; three coins and a posy of poppies and tansy on the plinth; a carved zigzag band round the bowl.
- **Materials and ramps (proplib):** `art/props/giant_relics.py`: the stone hand's weathered stone `make_ramp("8A8478", 0.20)`, a darker field stone `make_ramp("6E6B66", 0.20)`, moss `make_ramp("5F7A3A")`, lichen `make_ramp("8E9256")`, earth from `giant_bones.mound`; old rainwater `make_ramp("3E5552", 0.18)` (dark green, so it reads as standing water and not a fountain); candle wax from proplib `LINEN`, flames proplib `HOT` (glow part), coins `GILT`; posy `make_ramp("A8402F")` poppy, `make_ramp("C9AE48")` tansy, `make_ramp("4E6A30")` stems.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** the plus of cells under the bowl at y=0 and y=1 (centre and four sides): the bowl overhangs them at head height, so troops path round its full width; the four corners stay open.
- **Light and effects:** candle flames are a glow part (`offering_bowl_flames`); one light block, level 7, at (1, 2, 0) above the front rim, so the candles light the ground a little at night. No particles, no sound.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `rune_stone`, `cairn` (same stone); `brazier` for the glow part; the forge set for density; the Knight on the card.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
