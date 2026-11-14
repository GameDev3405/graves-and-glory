"""Plough (`plough`): 1x2x3 cells, anchor cell (0,0,1). Brief: art/props/plough/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..16, y 0..32, z -16..32 px.
A wooden ox-plough left in the field: an iron-pointed share on a runner, a landside plank on the east and a stepped
mouldboard on the west, an iron coulter slanting forward to the beam, a long beam to the front propped on a stake with an
iron hitch bracket, and two stilts rising to a grip behind (waist high on a Knight).
Build:  python art/props/build_props.py plough      Card:  python tools/prop_card.py plough
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, pat_grain   # noqa: F401


def pale(axis):
    """Sun-bleached, hand-worn timber one tone lighter than `wood_*`, with pale wear streaks where hands and traces rub,
    so the plough keeps a readable silhouette at night."""
    g = pat_grain(axis)
    def pat(face, P, c, r, w, h):
        d = g(face, P, c, r, w, h)
        x, y, z = (int(v // 1) for v in P)
        return d + (1 if h3(x // 2, y, z // 3, 23) < 0.22 else 0)
    return Mat(WOOD, 3, pat)


PALE_Z, PALE_Y, PALE_X = pale("z"), pale("y"), pale("x")


def build():
    els = [
        # runner on the ground with the iron share at its front
        E((5, 0, 4), (11, 3, 26), PALE_Z, skip="down", name="slade"),
        E((5, 0, 0), (11, 3, 4), "iron", skip="down", name="share"),
        E((7, 0, -2), (9, 2, 0), "iron", skip="down", name="share point"),
        # landside plank (east) and the stepped mouldboard (west)
        E((11, 0, 8), (13, 10, 24), "plank_z", skip="down", name="landside"),
        E((3, 3, 8), (5, 8, 22), "plank_z", name="mouldboard low"),
        E((1, 8, 10), (3, 14, 22), "plank_z", name="mouldboard high"),
        E((3, 8, 10), (5, 10, 22), "iron", name="mouldboard strap"),
        # standards up to the beam and the long beam to the front, propped on a stake
        E((6, 3, 10), (10, 10, 14), PALE_Y, name="standard front"),
        E((6, 3, 20), (10, 10, 24), PALE_Y, name="standard rear"),
        E((6, 10, -14), (10, 14, 26), PALE_Z, name="beam"),
        E((7, 0, -12), (9, 10, -10), "wood_y", skip="down", name="prop stake"),
        E((7, 10, -16), (9, 14, -14), "iron", name="hitch bracket"),
        # coulter slanting forward-down from the beam
        E((7, 0, 9), (9, 15, 11), "steel", rot=("x", 22.5, (7, 0, 11)), name="coulter"),
        # stilts rising back from the runner's heel, a rung and the grip
        E((3, 3, 17), (5, 29, 20), PALE_Y, rot=("x", 22.5, (3, 3, 20)), name="stilt"),
        E((11, 3, 17), (13, 29, 20), PALE_Y, rot=("x", 22.5, (11, 3, 20)), name="stilt"),
        E((3, 12, 21), (13, 14, 25), PALE_X, name="rung"),
        E((2, 26, 27), (14, 29, 31), PALE_X, name="grip"),
    ]
    return Prop("plough", {"plough": els})
