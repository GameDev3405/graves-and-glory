"""Cauldron Tripod (`cauldron_tripod`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/cauldron_tripod/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..32, z 0..16 px.
Three stepped timber poles (two in front, one behind) lean in to a rope lashing over the middle; an iron cauldron with a bail hangs from the lashing on a chain over a ring of fire stones. The embers are a glow
part (cauldron_tripod_embers, full-bright).
Build:  python art/props/build_props.py cauldron_tripod      Card:  python tools/prop_card.py cauldron_tripod
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOT   # noqa: F401

MAT["rope"] = Mat(make_ramp("7A6038", 0.20), 2, edges=False, pattern=lambda f, P, c, r, w, h: -1 if int(P[1]) % 2 else 0)
MAT["stew"] = Mat(make_ramp("5B4630"), 2, edges=False, pattern=lambda f, P, c, r, w, h: 1 if h3(*P, 71) < 0.12 else 0)
FLAME = Mat(HOT, 3, edges=False, lit=False)
MAT["pot"] = Mat(make_ramp("6E7380"), 2, pattern=lambda f, P, c, r, w, h: -1 if h3(*P, 73) < 0.06 else 0)


def build():
    t = []
    # fire ring: eight rough stones of different heights
    for x1, z1, x2, z2, h in ((3, 2, 7, 5, 2), (9, 2, 13, 5, 3), (11, 5, 14, 9, 3), (11, 9, 14, 12, 4),
                              (8, 11, 12, 14, 3), (4, 11, 8, 14, 4), (2, 8, 5, 11, 3), (2, 4, 5, 8, 4)):
        t.append(E((x1, 0, z1), (x2, h, z2), "stone", skip="down", name="ring stone"))
    # the three poles: stepped (3 px square, 4 px high steps) so each leans from its foot on the cell edge to the lashing over
    # the middle; one leg to the front, two behind. Axis-aligned steps keep the pot's space free between them.
    for name, (bx, bz) in (("pole front west", (15.4, 3.75)), ("pole front east", (0.6, 3.75)), ("pole back", (8.0, 16.0))):
        for k in range(7):
            u = (4 * k + 2) / 26.0
            cx, cz = bx + (8 - bx) * u, bz + (8 - bz) * u
            n = 3 if k < 2 else 2                      # the poles thin toward the top
            x1, z1 = round(cx - n / 2), round(cz - n / 2)
            t.append(E((x1, 4 * k, z1), (x1 + n, 4 * k + 4, z1 + n), "wood_y", skip="down" if k == 0 else "", name=name))
    # lashing where they meet, a ring, the chain, the bail
    t.append(E((6, 23, 6), (10, 27, 10), "rope", name="lashing"))
    t.append(E((7, 27, 7), (9, 29, 9), "iron", name="hanging ring"))
    t.append(E((7, 15, 7), (9, 23, 9), "iron", name="chain"))
    t.append(E((4, 13, 7), (12, 15, 9), "iron", name="bail"))
    t.append(E((4, 10, 7), (6, 13, 9), "iron", name="bail ear west"))
    t.append(E((10, 10, 7), (12, 13, 9), "iron", name="bail ear east"))
    # the cauldron: belly, body, a hollow rim of four bars, stew under it
    t.append(E((5, 5, 5), (11, 7, 11), "pot", name="belly"))
    t.append(E((4, 7, 4), (12, 10, 12), "pot", name="body"))
    t.append(E((4, 10, 4), (12, 12, 6), "pot", name="rim north"))
    t.append(E((4, 10, 10), (12, 12, 12), "pot", name="rim south"))
    t.append(E((4, 10, 6), (6, 12, 10), "pot", name="rim west"))
    t.append(E((10, 10, 6), (12, 12, 10), "pot", name="rim east"))
    t.append(E((6, 10, 6), (10, 11, 10), "stew", skip="down", name="stew"))
    # embers (glow part)
    embers = [
        E((3, 0, 3), (13, 4, 13), "coal", skip="down", name="ember bed"),
        E((5, 4, 5), (11, 5, 11), "coal", skip="down", name="ember heap"),
        # flame tongues licking up the pot's sides, between the stones
        E((6, 4, 3), (8, 7, 5), FLAME, name="flame front"), E((9, 4, 3), (10, 6, 5), FLAME, name="flame front small"),
        E((3, 4, 6), (5, 7, 8), FLAME, name="flame west"), E((11, 4, 8), (13, 7, 10), FLAME, name="flame east"),
    ]
    return Prop("cauldron_tripod", {"cauldron_tripod": t, "cauldron_tripod_embers": embers}, glow=("cauldron_tripod_embers",))
