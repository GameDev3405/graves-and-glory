# Prop brief: Giant's Chain Link (`giant_chain_link`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a short run of a giant's chain coming out of the earth: one oval link lying flat in the grass, the next standing on edge through its end and half sunk, the third diving into a mound.
- **Where it goes:** chapter 1 maps: a river bank, a field, the foot of a hill; what was chained is never said. Kort places it.
- **Setting / House:** No House. Relics of the giants, older than the Houses; nobody names the giants (lore reboot 2026-11-09). Kort's "Giant relics, small" set: worked stone and old iron, no bones.
- **Footprint (cells, x east * y up * z south):** 3 x 2 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** each link is 28 x 24 px with an 18 x 14 hole and a 5 px bar; the standing link rises 16 px, knee to thigh on a troop.
- **Silhouette at 15 m:** dark grey iron ovals linked at right angles, one flat and one on edge, running into a heap of earth: unmistakably a chain.
- **Close-up story at 3 m:** the standing link threaded through the flat one (how a chain hangs together); the third link's end going under the mound; little rust, a touch of moss on the tops.
- **Materials and ramps (proplib):** `art/props/giant_relics.py` `old_iron`: dark iron `make_ramp("4C4A4E", 0.20)` with rust blooms `make_ramp("7A4630", 0.20)` at about 0.08; moss as the stone hand; earth from `giant_bones.mound`.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells; every relic in this set is over 8 cells).
- **Collision:** the four cells x=1..2, y=0 under the standing link and the mound; the flat link at x=0 is ankle-high and stays open so troops can step over it.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). Solid cells already block movement and arrows like any wall.
- **References:** `stone_hand` (moss and earth), `helmet_grave_marker` (same iron); the forge set for density; the Knight on the card.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
