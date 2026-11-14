"""Cook Spit (`cook_spit`): 2x2x1 cells, anchor cell (0,0,0). Brief: art/props/cook_spit/brief.md.
Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..32, z 0..16 px.
Two forked posts stand either side of a small fire in a ring of stones; an iron spit rests in the forks, a tied roast on it,
a crank on the east end. Three parts:
  cook_spit        posts, forks, lashings, ring stones, logs (static)
  cook_spit_fire   embers and static flame tongues (glow; animated flames are another branch's work)
  cook_spit_roast  spit, roast, twine, knuckle bones, crank: turns with the `rotate` motion kind about the spit axis
                   (docs/props/MOVING-PARTS.md). Its entity sits ON the axle (def offset [8, 14, 0], axle at x, y 22, z 8),
                   so the client's slerp alone turns it, with no translation drift.
Clearance: the rod (2x2, corners 1.4 px from the axle) turns inside a 4 px fork gap above the crotch at y 20; the twine
bands (10x10, corners 7.1 px out) clear the flame tips (y 14) by 0.9 px; the knuckles stop 1 px short of the forks.
Build:  python art/props/build_props.py cook_spit      Card:  python tools/prop_card.py cook_spit
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOT   # noqa: F401  (materials: proplib.MAT keys)

AXLE = (22, 8)                     # (y, z) of the spit axis; it runs along x
ROAST_OFFSET = (8, 14, 0)          # the roast part's entity origin = (8, 8, 8) + offset = (16, 22, 8): on the axle

ROAST = Mat(make_ramp("9A5A2E", 0.20), 2, lambda f, P, c, r, w, h: -1 if h3(*P, 41) < 0.10 else 0)
CRISP = Mat(make_ramp("5E3418", 0.20), 2, lambda f, P, c, r, w, h: 1 if h3(*P, 43) < 0.12 else 0)   # the charred side
BONE = Mat(make_ramp("E2D6B8", 0.16), 2)
TWINE = Mat(make_ramp("B8A57E", 0.16), 2, edges=False, pattern=lambda f, P, c, r, w, h: -1 if int(P[1] + P[2]) % 2 else 0)
FLAME = Mat(HOT, 3, edges=False, lit=False)
FLAME_TIP = Mat(HOT, 4, edges=False, lit=False)


def post(x0):
    """A forked post: a 4x6 stake to the crotch at y 20, two prongs to y 27 with a 4 px gap for the spit, a lashing."""
    return [
        E((x0, 0, 5), (x0 + 4, 20, 11), "wood_y", skip="down", name="post"),
        E((x0, 20, 4), (x0 + 4, 27, 6), "wood_y", faces={"up": "endgrain"}, name="fork prong"),
        E((x0, 20, 10), (x0 + 4, 27, 12), "wood_y", faces={"up": "endgrain"}, name="fork prong"),
        E((x0 - 1, 15, 4), (x0 + 5, 18, 12), TWINE, name="post lashing"),
    ]


def build():
    base = post(2) + post(26)
    # ring of rough stones round the fire, logs crossed in it
    for x1, z1, x2, z2, h in ((9, 2, 12, 5, 3), (13, 1, 17, 4, 2), (18, 2, 22, 5, 3), (21, 6, 24, 10, 3),
                              (18, 11, 22, 14, 3), (13, 12, 17, 15, 2), (9, 11, 12, 14, 3), (8, 6, 11, 10, 3)):
        base.append(E((x1, 0, z1), (x2, h, z2), "stone", skip="down", name="ring stone"))
    base.append(E((11, 0, 7), (21, 3, 9), "wood_x", faces={"east": "endgrain", "west": "endgrain"}, skip="down", name="log"))
    base.append(E((15, 0, 4), (17, 2, 12), "wood_z", faces={"north": "endgrain", "south": "endgrain"}, skip="down", name="log"))

    fire = [
        E((11, 0, 4), (21, 1, 12), "coal", skip="down", name="ember bed"),
        E((14, 2, 6), (18, 8, 10), FLAME, skip="down", name="flame"),
        E((15, 8, 7), (17, 12, 9), FLAME_TIP, name="flame"),
        E((15, 12, 8), (16, 14, 9), FLAME_TIP, name="flame tip"),
        E((11, 1, 5), (13, 6, 7), FLAME, skip="down", name="flame tongue"),
        E((19, 1, 9), (21, 7, 11), FLAME, skip="down", name="flame tongue"),
        E((12, 1, 10), (14, 5, 12), FLAME, skip="down", name="flame tongue"),
        E((18, 1, 4), (20, 5, 6), FLAME, skip="down", name="flame tongue"),
    ]

    roast = [
        E((1, 21, 7), (31, 23, 9), "iron", name="spit"),
        E((31, 21, 7), (33, 29, 9), "iron", name="crank arm"),
        E((32, 27, 7), (34, 29, 11), "wood_z", name="crank grip"),
        E((11, 18, 4), (21, 26, 12), ROAST, faces={"up": CRISP}, name="roast"),
        E((9, 19, 5), (11, 25, 11), ROAST, faces={"up": CRISP}, name="roast end"),
        E((21, 19, 5), (23, 25, 11), ROAST, faces={"up": CRISP}, name="roast end"),
        E((13, 17, 3), (14, 27, 13), TWINE, name="twine"),
        E((18, 17, 3), (19, 27, 13), TWINE, name="twine"),
        E((7, 20, 6), (9, 24, 10), BONE, name="knuckle bone"),
        E((23, 20, 6), (25, 24, 10), BONE, name="knuckle bone"),
    ]
    return Prop("cook_spit", {"cook_spit": base, "cook_spit_fire": fire, "cook_spit_roast": roast},
                glow=("cook_spit_fire",), shift={"cook_spit_roast": ROAST_OFFSET}, offsets=True)
