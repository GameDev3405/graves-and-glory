# Prop brief: Giant's Rune Stone (`rune_stone`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a rough standing stone of the giants, three blocks tall, with two giant-scale runes carved down its face, each rune as tall as a man's forearm, holding a faint cold light.
- **Where it goes:** chapter 1 maps: a field edge, a crossing or a hilltop, off the lanes; alone or near `cairn`. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 2 x 3 x 2. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 47 px, 1.34 troops tall; its face is 20 px wide. A man stands beside it and the runes start at his waist and end above his head: writing made for bigger readers.
- **Silhouette at 15 m:** one tall, rough upright slab tapering on its west side, chipped on its east shoulder, with a broken crown: a menhir, not a gravestone (no frame, no rounded top).
- **Close-up story at 3 m:** the two runes (a forked stave above a hooked stave) as 2 px strokes 1 px proud of the face, two-tone with a darker core so they glow dimly; moss climbing the foot; a chip of the crown lying in the grass.
- **Materials and ramps (proplib):** `art/props/giant_relics.py`: the stone hand's weathered stone `make_ramp("8A8478", 0.20)`, a darker field stone `make_ramp("6E6B66", 0.20)`, moss `make_ramp("5F7A3A")`, lichen `make_ramp("8E9256")`, earth from `giant_bones.mound`; rune light `make_ramp("4E8A7E", 0.16)` (dim cold teal, full-bright part, base tone 1 with a darker core so it stays faint at night).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** the four bottom cells (y=0): the stone's foot fills them; troops walk round it, arrows at waist height hit it. Above y=0 the slab is too thin (about a quarter of each cell) to need barriers.
- **Light and effects:** the runes are a glow part (`rune_stone_runes`, full-bright) so they read faintly at night; no light block, no particles, no sound: stillness is the point.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `stone_hand` (same stone and moss), `cairn`; the forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
