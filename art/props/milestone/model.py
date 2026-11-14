"""Milestone (`milestone`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/milestone/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..16, y 0..32, z 0..16 px.
A squat roadside stone, 22 px tall: a wide footing, a body, stepped shoulders and a rounded top; moss creeps up the
foot, the front carries a carved ring with a cross and a row of tally cuts. No lettering.
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
    """Front face of the body (x 3..13, y 5..16): a carved ring with a cross in it, tally cuts below the ring."""
    x, y = (int(math.floor(P[i])) for i in (0, 1))
    if face == "north":
        d = math.hypot(x + 0.5 - 8, y + 0.5 - 11.5)
        if 2.4 <= d < 3.5: return -2
        if x in (7, 8) and 9 <= y <= 13 and d < 2.4: return -2 if x == 7 else -1
        if y == 11 and 6 <= x <= 9 and d < 2.4: return -2
        if y in (6, 7) and x in (5, 7, 9, 11): return -2          # tally cuts
    return pat_rock(face, P, c, r, w, h)


ROCK = Mat(STONE, 2, pat_rock, top=0); ROCK.ramps = (MOSS,)
CARVED = Mat(STONE, 2, pat_carved, top=0); CARVED.ramps = (MOSS,)


def build():
    els = [
        E((2, 0, 2), (14, 5, 14), ROCK, skip="down", name="footing"),
        E((3, 5, 3), (13, 15, 13), ROCK, faces={"north": CARVED}, name="body"),
        E((4, 15, 4), (12, 18, 12), ROCK, name="shoulder"),
        E((5, 18, 5), (11, 20, 11), ROCK, name="crown"),
        E((6, 20, 6), (10, 22, 10), ROCK, name="top"),
        E((1, 0, 5), (3, 3, 9), ROCK, skip="down", name="fallen chip"),
        E((12, 0, 1), (15, 2, 4), ROCK, skip="down", name="fallen chip"),
    ]
    return Prop("milestone", {"milestone": els})
