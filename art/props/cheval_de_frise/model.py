"""Cheval de Frise (`cheval_de_frise`): 3x1x1 cells, anchor cell (1,0,0). Brief: art/props/cheval_de_frise/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..16, z 0..16 px.
A heavy log with six pairs of 4 px stakes of stakes driven through it crossed at 45 degrees (an X seen from the end), a pale sharpened
point on every end, three iron straps binding the log. Collision: all three cells solid, like stake_barricade.
Build:  python art/props/build_props.py cheval_de_frise      Card:  python tools/prop_card.py cheval_de_frise
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3   # noqa: F401
from proplib import pat_grain

HEART = make_ramp("B49A72", 0.16)   # freshly cut heartwood on the sharpened points (as stake_barricade)
POINT = Mat(HEART, 2, pat_grain("y"))


def build():
    els = [
        E((-15, 5, 5), (31, 11, 11), "stump", faces={"east": "endgrain", "west": "endgrain"}, name="log"),
    ]
    for x0 in (-14, -6, 2, 10, 18, 26):
        for ang in (45, -45):
            org = (x0 + 2, 8, 8)
            els.append(E((x0, 3, 6), (x0 + 4, 13, 10), "wood_y", rot=("x", ang, org), name="stake"))
            els.append(E((x0 + 1, 13, 7), (x0 + 3, 17, 9), POINT, rot=("x", ang, org), name="point"))
            els.append(E((x0 + 1, -1, 7), (x0 + 3, 3, 9), POINT, rot=("x", ang, org), name="point"))
    for x in (-9, 7, 23):
        els.append(E((x, 4, 4), (x + 2, 12, 12), "iron", name="strap"))
    return Prop("cheval_de_frise", {"cheval_de_frise": els})
