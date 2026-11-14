"""Archery Target (`archery_target`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/archery_target/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..32, z 0..16 px.

A straw butt on a wooden stand: two legs leaning back, a rear prop leg and a crossbar; a round coiled-straw boss 4 px thick
tilted back 22.5 degrees, its front painted with cream, red and blue-black rings, three arrows stuck in it.
Build:  python art/props/build_props.py archery_target      Card:  python tools/prop_card.py archery_target
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, LINEN, COAL, WOOD, disc_cells, slab   # noqa: F401
from strawlib import STRAW_X, STRAW_Y, STRAW_R, ROPE, straw_pattern

RED = make_ramp("A8322A", 0.20)
CX, CY = 8, 17          # centre of the face in the unrotated frame
TILT = 22.5
ORG_Z = 7


def pat_face(face, P, c, r, w, h):
    """Painted rings seen on the north face; the rim of the boss and every other face stay straw."""
    x, y, z = P
    if face == "north":
        d = math.hypot(x - CX, y - CY)
        if d < 2.2: return COAL[1]
        if d < 3.8: return LINEN[3]
        if d < 5.6: return RED[2]
        if d < 6.9: return LINEN[3]
    return straw_pattern("x")(face, P, c, r, w, h)


FACE = Mat(STRAW_R, 2, pat_face, edges=False, top=0)
FACE.ramps = (LINEN, RED, COAL)
ARROW_SHAFT = Mat(make_ramp("B49A72", 0.16), 2, lambda f, P, c, r, w, h: 0, edges=False)
FLETCH = Mat(LINEN, 3, lambda f, P, c, r, w, h: -1 if (c + r) % 3 == 0 else 0, edges=False)
FLETCH_D = Mat(COAL, 3, lambda f, P, c, r, w, h: -1 if (c + r) % 3 == 0 else 0, edges=False)
FLETCH_R = Mat(RED, 2, lambda f, P, c, r, w, h: -1 if (c + r) % 3 == 0 else 0, edges=False)


def arrow(x, y, depth, fl):
    """An arrow flying north-south: tail with a single 1x3 vane at z 0..3, shaft into the boss until z = depth."""
    return [
        E((x, y, 0), (x + 1, y + 1, depth), ARROW_SHAFT, name="arrow shaft"),
        E((x, y - 1, 0), (x + 1, y + 2, 3), fl, name="arrow vane"),
    ]


def build():
    rot = lambda ox, oy, oz, a: ("x", a, (ox, oy, oz))
    els = []
    # legs leaning back with the boss, rope lashed under it; crossbar and a rear prop leg
    for x in (2, 11):
        els.append(E((x, 0, 3), (x + 3, 21, 6), "wood_y", skip="down", rot=rot(x, 0, 6, TILT), name="leg"))
        els.append(E((x - 1, 13, 2), (x + 4, 15, 7), ROPE, rot=rot(x, 0, 6, TILT), name="lashing"))
    els.append(E((5, 6, 6), (11, 9, 9), "wood_x", name="crossbar"))
    els.append(E((6, 0, 12), (10, 11, 15), "wood_y", skip="down", rot=rot(6, 0, 12, -TILT), name="prop leg"))
    # the boss
    els += slab(disc_cells(CX, CY, 7.4), "z", 3, 7, STRAW_X, name="straw boss",
                faces={"north": FACE}, rot=rot(0, 9, ORG_Z, TILT))
    # arrows
    els += arrow(11, 18, 8, FLETCH_D)
    els += arrow(4, 20, 9, FLETCH)
    els += arrow(9, 13, 5, FLETCH)
    return Prop("archery_target", {"archery_target": els})
