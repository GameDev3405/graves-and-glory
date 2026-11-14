"""Milestone (`milestone`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/milestone/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..16, y 0..32, z 0..16 px.
A squared roadside marker stone, 28 px tall: a wide footing, a plinth, a tapering body and a flat overhanging cap; moss
creeps up the foot, the front carries a V notch over three number cuts. No lettering. Cells stay 1x2x1 (28 px does not fit 1x1x1).
Build:  python art/props/build_props.py milestone      Card:  python tools/prop_card.py milestone
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, STONE   # noqa: F401

MOSS = make_ramp("56733A", 0.20)


def pat_rock(face, P, c, r, w, h):
    """Weathered rough stone: sparse darker pits, moss in 2 px clumps on the lower part and on top."""
    x, y, z = (int(math.floor(v)) for v in P)
    k = h3(x // 2, y // 2, z // 2, 9)
    if (y < 9 and k < 0.40) or (face == "up" and k < 0.22):
        return MOSS[1 if k < 0.12 else 2]
    return -1 if h3(x, y, z, 5) < 0.07 else 0


def pat_carved(face, P, c, r, w, h):
    """Front face of the upper body (x 4..12, y 14..25): a V notch cut near the top and three tally strokes under it."""
    x, y = (int(math.floor(P[i])) for i in (0, 1))
    if face == "north":
        if y == 22 and 5 <= x <= 10: return -2                   # notch, widest row
        if y == 21 and 6 <= x <= 9: return -2
        if y == 20 and 7 <= x <= 8: return -2
        if 14 <= y <= 18 and x in (5, 7, 9) and h3(x, y, 0, 3) < 0.97: return -2   # number cuts
    return pat_rock(face, P, c, r, w, h)


ROCK = Mat(STONE, 2, pat_rock, top=0); ROCK.ramps = (MOSS,)
CARVED = Mat(STONE, 2, pat_carved, top=0); CARVED.ramps = (MOSS,)


def build():
    els = [
        E((2, 0, 2), (14, 4, 14), ROCK, skip="down", name="footing"),
        E((3, 4, 3), (13, 14, 13), ROCK, name="plinth"),
        E((4, 14, 4), (12, 25, 12), ROCK, faces={"north": CARVED}, name="body"),
        E((3, 25, 3), (13, 27, 13), ROCK, name="cap"),
        E((5, 27, 5), (11, 28, 11), ROCK, name="cap top"),
        E((1, 0, 5), (3, 3, 9), ROCK, skip="down", name="fallen chip"),
        E((12, 0, 1), (15, 2, 4), ROCK, skip="down", name="fallen chip"),
    ]
    return Prop("milestone", {"milestone": els})
