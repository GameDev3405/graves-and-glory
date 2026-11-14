# Prop brief: Giant's Cairn (`cairn`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a cairn of big stones over a giant's resting place: a ring of chest-high boulders, two smaller courses, a flat capstone and a little heap of fist-sized stones people still leave on top.
- **Where it goes:** chapter 1 maps, on rises and at the end of old roads; near `rune_stone`. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 3. Front faces north. Anchor cell (1, 0, 1).
- **Scale against a troop:** 44 px, 1.26 troops tall, 46 px across: each boulder of the bottom ring is as big as a man's chest; the people's stones on top are fist-sized, for scale.
- **Silhouette at 15 m:** a broad stepped pile narrowing to a flat capstone: a cairn, not a boulder and not a wall.
- **Close-up story at 3 m:** the people's little heap of stones on the capstone; moss on every top face and at the foot; the stones of two tones, laid by hand.
- **Materials and ramps (proplib):** `art/props/giant_relics.py`: the stone hand's weathered stone `make_ramp("8A8478", 0.20)`, a darker field stone `make_ramp("6E6B66", 0.20)`, moss `make_ramp("5F7A3A")`, lichen `make_ramp("8E9256")`, earth from `giant_bones.mound`. Boulders are `proplib.blob` (step 3-4 px, two layers) so each reads as one rough stone.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** y=0: every cell but the two east corners (they hold only the barrow's edge); y=1: the centre column. Troops walk round it; it stops arrows like a rock.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `stone_hand`, `rune_stone`, the boulder set (`boulder_*`) for rock style; the forge set for density; the Knight on the card.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
