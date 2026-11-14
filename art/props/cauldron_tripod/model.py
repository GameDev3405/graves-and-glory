"""Cauldron Tripod (`cauldron_tripod`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/cauldron_tripod/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..32, z 0..16 px.
Rework 2026-11-14 (Kort: "heavy rework"): a true tripod. Three thin stained legs on stone pads, wide stance (front feet in
the two front corners, the back foot at the back middle), meet high in a rope lashing (band and knot) with their tips poking 4 px out above.
A ring under the lashing, a chain and S-hook carry the bail of a dark octagonal iron pot (rim lip, ears, stew with
bubbles) hung low over an open fire: an ash disc, a ring of eight low stones, an ember bed and two crossed pairs of
flame planes (one pair turned 45 degrees) that show between the legs and under the pot from every side. Embers, flames
and the pot's hot underside are a glow part (cauldron_tripod_embers, full-bright) with an animated texture (proplib Anim).
Build:  python art/props/build_props.py cauldron_tripod      Card:  python tools/prop_card.py cauldron_tripod
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOT, SWOOD, Anim, FIRE_ANIM, flames, pat_iron, pat_grain   # noqa: F401

MAT["rope"] = Mat(make_ramp("7A6038", 0.20), 2, edges=False, pattern=lambda f, P, c, r, w, h: -1 if int(P[1]) % 2 else 0)
MAT["stew"] = Mat(make_ramp("5B4630"), 2, edges=False, pattern=lambda f, P, c, r, w, h: 2 if h3(*P, 71) < 0.10 else (1 if h3(*P, 72) < 0.18 else 0))
MAT["pot_iron"] = Mat(make_ramp("3C3F47"), 2, pat_iron)                      # near-black cauldron iron, darkest thing on the prop
MAT["pot_soot"] = Mat(make_ramp("2C2A2E"), 2, pat_iron)                      # soot band on the belly
MAT["ash"] = Mat(make_ramp("4A4543", 0.18), 2, edges=False, pattern=lambda f, P, c, r, w, h: -1 if h3(*P, 75) < 0.25 else 0)
MAT["leg_wood"] = Mat(SWOOD, 2, pat_grain("y"), edges=False)                 # stained legs: no seam per step, so a leg reads as one pole
MAT["pot_hot"] = Mat(HOT, 1, edges=False, lit=False)                         # the pot's underside, lit by the fire (glow part)

FEET = (("leg front west", (1.0, 1.0), (6.5, 7.0)), ("leg front east", (15.0, 1.0), (9.5, 7.0)), ("leg back", (8.0, 15.0), (8.0, 9.5)))


def legs():
    """Three thin legs in 3 px steps (2x2, the foot step 3x3) from y 2 to the tips at y 32, each leaning from its foot to
    its own point under the lashing (offset from the centre so the three never share a face)."""
    out = []
    for name, (fx, fz), (tx, tz) in FEET:
        for k in range(10):
            y0 = 2 + 3 * k
            t = (y0 + 1.5 - 2) / 30.0
            cx, cz = fx + (tx - fx) * t, fz + (tz - fz) * t
            n = 3 if k == 0 else 2
            x1, z1 = math.floor(cx - n / 2 + 0.5), math.floor(cz - n / 2 + 0.5)
            x1, z1 = max(0, min(16 - n, x1)), max(0, min(16 - n, z1))
            out.append(E((x1, y0, z1), (x1 + n, y0 + 3, z1 + n), "leg_wood", name=name))
    return out


def build():
    t = [E((2, 0, 2), (14, 1, 14), "ash", skip="down", name="ash disc")]
    # eight low fire stones around the ash, soot-topped, plus a stone pad under each leg foot
    for x1, z1, x2, z2, h in ((4, 1, 7, 4, 2), (9, 1, 12, 4, 3), (12, 4, 15, 7, 2), (12, 9, 15, 12, 3),
                              (10, 12, 13, 15, 2), (3, 12, 6, 15, 3), (1, 9, 4, 12, 2), (1, 4, 4, 7, 3)):
        t.append(E((x1, 0, z1), (x2, h, z2), "stone", skip="down", faces={"up": "ash"}, name="fire stone"))
    t += [E((0, 0, 0), (3, 2, 3), "stone", skip="down", name="foot pad"), E((13, 0, 0), (16, 2, 3), "stone", skip="down", name="foot pad"),
          E((6, 0, 13), (10, 2, 16), "stone", skip="down", name="foot pad")]
    t += legs()
    # lashing: one rope band (a second band read as a hat at 15 m) and a knot on its front, the hanging ring under them, the chain (alternating links) and an S-hook
    t += [E((4, 27, 4), (12, 28, 12), "rope", name="lashing"), E((5, 28, 4), (11, 29, 5), "rope", name="lashing knot"),
          E((7, 25, 7), (9, 27, 9), "iron", name="hanging ring")]
    for i, y in enumerate(range(19, 25, 2)):
        t.append(E((7, y, 7), (9, y + 2, 9), "steel" if i % 2 else "iron", name="chain link"))
    t.append(E((6, 18, 7), (10, 19, 9), "iron", name="s-hook"))
    # bail: two posts up from the ears, a flat top the S-hook catches
    t += [E((2, 15, 7), (3, 17, 9), "iron", name="bail post"), E((13, 15, 7), (14, 17, 9), "iron", name="bail post"),
          E((2, 17, 7), (14, 18, 9), "iron", name="bail")]
    # the pot: soot belly, octagonal body (three boxes that never overlap), a proud rim lip of four bars, ears, stew
    t += [E((5, 9, 5), (11, 11, 11), "pot_soot", name="belly"),
          E((4, 11, 5), (12, 13, 11), "pot_iron", name="body"),
          E((5, 11, 4), (11, 13, 5), "pot_iron", name="body"), E((5, 11, 11), (11, 13, 12), "pot_iron", name="body"),
          E((3, 13, 4), (5, 15, 12), "pot_iron", name="rim"), E((11, 13, 4), (13, 15, 12), "pot_iron", name="rim"),
          E((5, 13, 3), (11, 15, 5), "pot_iron", name="rim"), E((5, 13, 11), (11, 15, 13), "pot_iron", name="rim"),
          E((2, 13, 7), (3, 15, 9), "pot_iron", name="ear"), E((13, 13, 7), (14, 15, 9), "pot_iron", name="ear"),
          E((5, 13, 5), (11, 14, 11), "stew", skip="down", name="stew")]
    # glow part: ember bed and heap, the pot's fire-lit underside, two crossed flame pairs (one turned 45 degrees). The
    # flame planes stand at x 8 / z 8 (and on the diagonals), never in the plane of an ember face.
    embers = [
        E((4, 0, 4), (12, 2, 12), "embers", skip="down", name="ember bed"),
        E((5, 2, 5), (11, 4, 11), "embers", skip="down", name="ember heap"),
        E((5, 8, 5), (11, 9, 11), "pot_hot", skip="up", name="pot underside"),
    ] + flames(8, 8, 2, 12, 5) + [
        E((3, 2, 8), (13, 11, 8), "flame", rot=("y", 45, (8, 2, 8)), name="flame diag a"),
        E((8, 2, 3), (8, 11, 13), "flame", rot=("y", 45, (8, 2, 8)), name="flame diag b"),
    ]
    return Prop("cauldron_tripod", {"cauldron_tripod": t, "cauldron_tripod_embers": embers}, glow=("cauldron_tripod_embers",),
                anim={"cauldron_tripod_embers": Anim(**FIRE_ANIM)})
