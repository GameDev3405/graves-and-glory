"""Training Dummy (`training_dummy`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/training_dummy/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..32, z 0..16 px.

A yard dummy: a post on a cross foot, a crossbar for arms, a burlap sack body stuffed with straw (straw shows at the hem,
the wrists, the neck and through a torn patch), rope ties at the waist and neck, a stitched sack head, and a battered
round wooden shield hung from the shoulder on the viewer's left (east).
Build:  python art/props/build_props.py training_dummy      Card:  python tools/prop_card.py training_dummy
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, IRON as IRON_R, disc_cells, slab   # noqa: F401
from proplib import pat_plank
from strawlib import STRAW_Y, STRAW_X, BURLAP, BURLAP_R, ROPE, pat_burlap


def pat_face(face, P, c, r, w, h):
    """Sack head: weave, and on the front (north) two dark eyes and a stitched mouth. Column 0 is the viewer's left."""
    if face == "north":
        if r == 1 and c in (1, 4): return -2
        if r == 3 and 1 <= c <= 4 and c % 2 == 1: return -2
    return pat_burlap(face, P, c, r, w, h)


HEAD = Mat(BURLAP_R, 2, pat_face)


def pat_shield(face, P, c, r, w, h):
    """Planks hanging upright, battered: dark gouges, a few pale scuffs."""
    x, y, z = P
    base = pat_plank("y", 3)(face, P, c, r, w, h)
    k = h3(int(x) // 2, int(y) // 2, 29)
    if k < 0.12: return base - 1
    if k > 0.93: return base + 1
    return base


def pat_shield_rim(face, P, c, r, w, h):
    """Front of the shield: an iron-banded rim ring, planks inside."""
    x, y, z = P
    if face == "north" and math.hypot(x - 11.5, y - 11.5) > 4.5: return IRON_R[2 if h3(x, y, 3) < 0.7 else 1]
    return pat_shield(face, P, c, r, w, h)


SHIELD = Mat(WOOD, 2, pat_shield_rim)
SHIELD.ramps = (IRON_R,)


def build():
    els = [
        # cross foot of two planks, the post standing through it
        E((1, 0, 6), (15, 3, 10), "plank_x", skip="down", name="foot plank"),
        E((6, 0, 1), (10, 3, 15), "plank_z", skip="down", name="foot plank"),
        E((6, 3, 6), (10, 10, 10), "wood_y", name="post"),
        # straw spilling from the hem of the sack
        E((4, 6, 5), (12, 10, 11), STRAW_Y, name="hem straw"),
        E((3, 6, 6), (4, 10, 9), STRAW_Y, name="hem strand"),
        E((12, 7, 7), (13, 10, 10), STRAW_Y, name="hem strand"),
        E((9, 4, 4), (12, 7, 6), STRAW_Y, name="hem strand"),
        E((5, 5, 9), (7, 7, 12), STRAW_Y, name="hem strand"),
        # body sack: belly, waist rope, chest, straw bulging at the shoulders
        E((3, 10, 4), (13, 14, 12), BURLAP, name="belly sack"),
        E((2, 14, 3), (14, 16, 13), ROPE, name="waist rope"),
        E((3, 16, 4), (13, 21, 12), BURLAP, name="chest sack"),
        E((2, 21, 3), (14, 23, 13), STRAW_Y, name="shoulder straw"),
        # torn patch on the chest, the straw showing
        E((5, 16, 3), (9, 20, 4), STRAW_Y, name="torn patch"),
        # crossbar arms through the chest, straw bundles lashed at the ends
        E((0, 19, 7), (16, 22, 10), "wood_x", name="crossbar"),
        E((0, 17, 5), (3, 20, 11), STRAW_X, name="hand straw"),
        E((13, 17, 5), (16, 20, 11), STRAW_X, name="hand straw"),
        E((3, 18, 6), (4, 23, 11), ROPE, name="arm tie"),
        E((12, 18, 6), (13, 23, 11), ROPE, name="arm tie"),
        # neck tie and the sack head with its gathered top
        E((5, 23, 5), (11, 24, 11), ROPE, name="neck rope"),
        E((5, 24, 5), (11, 29, 11), HEAD, name="head sack"),
        E((6, 29, 6), (10, 30, 10), ROPE, name="top tie"),
        E((6, 30, 6), (7, 32, 7), STRAW_Y, name="top straw"),
        E((8, 30, 8), (10, 32, 9), STRAW_Y, name="top straw"),
        # rope strap over the shoulder holding the shield on the east side
        E((12, 14, 3), (13, 21, 5), ROPE, name="shield strap"),
        E((14, 14, 3), (15, 21, 5), ROPE, name="shield strap"),
    ]
    els = [e for e in els if e]
    els += slab(disc_cells(11.5, 11.5, 5.6), "z", 1, 3, SHIELD, name="shield")
    els.append(E((10, 10, 0), (13, 13, 1), "iron", name="shield boss"))
    return Prop("training_dummy", {"training_dummy": els})
