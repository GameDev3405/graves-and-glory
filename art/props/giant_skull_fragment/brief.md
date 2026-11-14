# Prop brief: Giant's Skull Fragment (`giant_skull_fragment`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** the upper skull of a giant sunk to its teeth: two round eye sockets under a heavy brow, the nose hole between, upper teeth in the earth, the domed cranium behind, its east side broken away.
- **Where it goes:** chapter 1 maps, Rivergate: on a rise near the rib arch, the ribcage's head; one per map at most, it is the set's centrepiece. Part of the giant bone set (`giant_rib`, `giant_vertebra`, `giant_knuckle`).
- **Setting / House:** no House. The remains of the giants, older than the Houses (lore reboot 2026-11-09; nobody names the giants).
- **Footprint (cells, x east * y up * z south):** 3 x 3 x 3. Front faces north. Anchor cell (1, 0, 1).
- **Scale against a troop:** 41 px, 1.2 troops tall; each eye socket (18 px) is wider than a troop's shoulders and each tooth stands to a troop's knee. The rest of the head is under the ground.
- **Silhouette at 15 m:** a skull face: two dark round sockets and a dark nose diamond in a pale dome. Sockets are octagonal rings (straight and 45-degree bars); the dome is one octagonal drum and three rings over it, each 2 px narrower (a smooth dome, not stacked tiers), its back plain bone with no vertical grain so it reads round, not fluted.
- **Close-up story at 3 m:** moss growing on the socket floors; the dome's east side broken away in steps (each ring stops further west than the one below), the cut faces showing the spongy core and a dark hollow inside the broken top; the row of teeth half in the earth, uneven.
- **Materials and ramps (proplib):** the set's ramps in `art/props/giant_bones.py` (old ivory `make_ramp("BDB092", 0.17)`, marrow `make_ramp("9C8466", 0.18)` for the socket shadow and the break, lichen `make_ramp("8E9256", 0.16)`, the stone hand's moss and earth).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the skull is one mass: every ground cell (y = 0) is solid, the cranium cells at y = 1 (all but the two front corners, where only the face edge is) and the crown's (1, 2, 1) and (1, 2, 2). Troops go round it, arrows hit it. The 5 px mound round it is walkable.
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing). As solid cells it already gives cover like any wall.
- **References:** `giant_rib`, `stone_hand` (same remains, same moss and earth); the forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
