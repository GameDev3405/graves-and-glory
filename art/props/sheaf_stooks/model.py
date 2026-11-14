"""Sheaf Stooks (`sheaf_stooks`): 2x2x2 cells, anchor cell (0,0,0). Brief: art/props/sheaf_stooks/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..32, y 0..32, z 0..32 px.
Three stooks, each four wheat sheaves stood up leaning together (wide at the foot, crossing near the top), a rope band
around the middle and a spread cap sheaf on top whose ears droop; a few loose stalks lie on the stubble.
Non-solid: troops walk through them.
Build:  python art/props/build_props.py sheaf_stooks      Card:  python tools/prop_card.py sheaf_stooks
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3   # noqa: F401
from strawlib import STRAW_X, STRAW_Y, STRAW_Y_DARK, STRAW_R, ROPE, straw_pattern

# wheat ears: a paler, redder gold than the straw stalks
EAR_R = ((0xB0, 0x82, 0x34, 255), (0xC8, 0x9A, 0x40, 255), (0xDA, 0xB2, 0x56, 255), (0xE8, 0xC8, 0x72, 255), (0xF4, 0xDE, 0x9C, 255))
EARS = Mat(list(EAR_R), 2, lambda f, P, c, r, w, h: -1 if (int(P[0]) + int(P[2]) * 2 + int(P[1])) % 3 == 0 else 0, edges=False, top=0)
EARS.ramps = (list(EAR_R),)
TILT = 22.5


def stook(cx, cz, hgt=18, name="stook"):
    """One stook centred on (cx, cz): four sheaves 4x4 px, bases 5 px from the centre, leaning inward."""
    els = []
    r = 5
    # north and south sheaves lean about the x axis, east and west about the z axis
    els.append(E((cx - 2, 0, cz - r - 2), (cx + 2, hgt, cz - r + 2), STRAW_Y, skip="down", rot=("x", TILT, (cx - 2, 0, cz - r + 2)), name=name + " sheaf n"))
    els.append(E((cx - 2, 0, cz + r - 2), (cx + 2, hgt, cz + r + 2), STRAW_Y_DARK, skip="down", rot=("x", -TILT, (cx - 2, 0, cz + r - 2)), name=name + " sheaf s"))
    els.append(E((cx - r - 2, 0, cz - 2), (cx - r + 2, hgt, cz + 2), STRAW_Y, skip="down", rot=("z", -TILT, (cx - r + 2, 0, cz - 2)), name=name + " sheaf w"))
    els.append(E((cx + r - 2, 0, cz - 2), (cx + r + 2, hgt, cz + 2), STRAW_Y_DARK, skip="down", rot=("z", TILT, (cx + r - 2, 0, cz - 2)), name=name + " sheaf e"))
    els.append(E((cx - 4, 11, cz - 4), (cx + 4, 13, cz + 4), ROPE, name=name + " band"))
    els.append(E((cx - 5, hgt, cz - 5), (cx + 5, hgt + 3, cz + 5), EARS, name=name + " cap"))
    els.append(E((cx - 3, hgt + 3, cz - 3), (cx + 3, hgt + 6, cz + 3), EARS, name=name + " cap top"))
    return els


def build():
    els = []
    els += stook(8, 8, 18, "stook a")
    els += stook(23, 9, 20, "stook b")
    els += stook(15, 24, 19, "stook c")
    els += [
        E((26, 0, 21), (31, 1, 23), STRAW_X, skip="down", name="loose stalks"),
        E((1, 0, 19), (6, 1, 21), STRAW_X, skip="down", name="loose stalks"),
        E((2, 0, 24), (8, 1, 26), STRAW_X, skip="down", name="loose stalks"),
    ]
    return Prop("sheaf_stooks", {"sheaf_stooks": els})
