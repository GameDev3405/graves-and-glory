# Prop brief: Giant's Chain Link (`giant_chain_link`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** one link of a giant's chain, rusted through, half sunk in the earth, with the next link's crown just breaking the ground inside its east end.
- **Where it goes:** chapter 1 maps: a river bank, a field, the foot of a hill; what was chained is never said. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 3 x 2 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** the visible arch is 30 px, nearly a troop; the loop is 44 px long and its bar 6 px square: a link a man could step through if it were not buried.
- **Silhouette at 15 m:** a low octagonal arch of dark red-brown iron standing on edge out of the ground, a smaller arch at right angles inside one end: a chain, not a gate.
- **Close-up story at 3 m:** the second link at right angles (how a chain hangs together); rust blooms over dark iron and rust runs down the sides; moss on the crown and earth heaped at both feet.
- **Materials and ramps (proplib):** `art/props/giant_relics.py`: rust `make_ramp("7A4630", 0.20)` over dark iron `make_ramp("4C4A4E", 0.20)`, moss and lichen as the stone hand; earth from `giant_bones.mound`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** the north row at y=0 (the loop stands in it): troops cannot pass under a 24 px arch, so they walk round. The south row (only the low mound and the next link's 19 px crown at x=2) stays open.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `stone_hand` (moss and earth), `helmet_grave_marker` (same iron); the forge set for density; the Knight on the card.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
