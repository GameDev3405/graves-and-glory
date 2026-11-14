"""Scarecrow (`scarecrow`): 3x3x1 cells, anchor cell (1,0,0). Brief: art/props/scarecrow/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x -16..32, y 0..48, z 0..16 px; the prop is exactly 3 cells tall, one part stored shifted
down 16 px. The post stands in the anchor cell (x 6..10).
A straw-stuffed figure on a cross pole: a faded-blue ragged coat with hanging tatters and straw at the hem, a stitched sack
head under a battered felt hat with straw poking out, straw hands, and a crow perched on the viewer's right (west) arm.
Build:  python art/props/build_props.py scarecrow      Card:  python tools/prop_card.py scarecrow
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, COAL, pat_grain   # noqa: F401
from strawlib import STRAW_X, STRAW_Y, STRAW_Y_DARK, BURLAP_R, ROPE, pat_burlap

COAT_R = make_ramp("5A6E86", 0.16)        # faded blue coat
PATCH_R = make_ramp("8A4A3A", 0.18)       # a rust-red patch
FELT_R = make_ramp("4F4338", 0.18)        # old felt hat


def pat_coat(face, P, c, r, w, h):
    """Woven rag cloth: a thread row every other px, a few dark mends."""
    x, y, z = (int(math.floor(v)) for v in P)
    u = x if face in ("north", "south", "up", "down") else z
    if y % 2 == 0 and (u + y // 2) % 2 == 0: return -1
    return -1 if h3(x, y, z, 3) < 0.05 else 0


def pat_chest(face, P, c, r, w, h):
    """Coat front with a patch of rust-red cloth on the chest."""
    x, y = int(math.floor(P[0])), int(math.floor(P[1]))
    if face == "north" and 8 <= x <= 11 and 18 <= y <= 21: return PATCH_R[2 if (x + y) % 3 else 1]
    return pat_coat(face, P, c, r, w, h)


def pat_face(face, P, c, r, w, h):
    """Sack head, 8 px wide: weave, and on the front two dark stitched eyes and a stitched mouth."""
    x, y = int(math.floor(P[0])), int(math.floor(P[1]))
    if face == "north":
        if y in (32, 33) and x in (5, 6, 9, 10): return COAL[0]
        if y == 30 and 5 <= x <= 10: return COAL[0] if x % 2 else COAL[1]
    return pat_burlap(face, P, c, r, w, h)


COAT = Mat(COAT_R, 2, pat_coat); CHEST = Mat(COAT_R, 2, pat_chest); CHEST.ramps = (PATCH_R,)
COAT.ramps = (); TATTER = Mat(COAT_R, 1, pat_coat, edges=False)
HEAD = Mat(BURLAP_R, 2, pat_face); HEAD.ramps = (COAL,)
HAT = Mat(FELT_R, 2, lambda f, P, c, r, w, h: -1 if h3(*P, 8) < 0.08 else 0)
CROW = Mat(COAL, 2, lambda f, P, c, r, w, h: 1 if f == "up" and c % 2 == 0 else 0, edges=False)


def build():
    els = [
        # the post, driven into the ground, and the cross pole
        E((6, 0, 6), (10, 14, 10), "wood_y", skip="down", name="post"),
        E((-6, 25, 7), (22, 28, 10), "wood_x", name="cross pole"),
        # straw bursting from the hem, then the coat: belly, chest, shoulder yoke
        E((3, 6, 5), (13, 11, 11), STRAW_Y, name="hem straw"),
        E((2, 7, 7), (3, 11, 10), STRAW_Y, name="hem strand"),
        E((13, 8, 6), (14, 11, 9), STRAW_Y, name="hem strand"),
        E((5, 4, 5), (7, 7, 8), STRAW_Y, name="hem strand"),
        E((9, 3, 7), (11, 7, 10), STRAW_Y_DARK, name="hem strand"),
        E((3, 11, 4), (13, 17, 12), COAT, name="belly"),
        E((2, 17, 3), (14, 25, 13), CHEST, name="chest"),
        E((3, 25, 5), (13, 29, 11), COAT, name="shoulder yoke"),
        E((2, 16, 3), (14, 17, 13), ROPE, name="belt rope"),
        # sleeves with straw forearms and straw hands
        E((-3, 23, 5), (3, 29, 11), COAT, name="sleeve"),
        E((13, 23, 5), (19, 29, 11), COAT, name="sleeve"),
        E((-6, 24, 6), (-3, 29, 10), ROPE, name="cuff tie"),
        E((19, 24, 6), (22, 29, 10), ROPE, name="cuff tie"),
        E((-11, 23, 5), (-6, 29, 11), STRAW_X, name="hand straw"),
        E((22, 23, 5), (27, 29, 11), STRAW_X, name="hand straw"),
        E((-9, 21, 6), (-7, 23, 10), STRAW_Y, name="finger straw"),
        E((24, 20, 6), (26, 23, 10), STRAW_Y, name="finger straw"),
        # ragged tatters hanging from the sleeves and the coat hem
        E((-2, 17, 7), (0, 23, 8), TATTER, name="tatter"),
        E((1, 15, 7), (3, 23, 8), TATTER, name="tatter"),
        E((14, 18, 7), (16, 23, 8), TATTER, name="tatter"),
        E((17, 14, 7), (19, 23, 8), TATTER, name="tatter"),
        E((3, 8, 3), (6, 11, 4), TATTER, name="hem rag"),
        E((9, 6, 3), (12, 11, 4), TATTER, name="hem rag"),
        # neck tie, sack head, straw under the hat, the felt hat
        E((4, 29, 4), (12, 30, 12), ROPE, name="neck rope"),
        E((4, 30, 4), (12, 37, 12), HEAD, name="head sack"),
        E((3, 35, 5), (4, 38, 10), STRAW_Y, name="hair straw"),
        E((12, 34, 6), (13, 38, 10), STRAW_Y, name="hair straw"),
        E((2, 37, 2), (14, 38, 14), HAT, name="hat brim"),
        E((4, 38, 4), (12, 44, 12), HAT, name="hat crown"),
        E((5, 44, 6), (11, 45, 10), HAT, name="hat crown top"),
        E((4, 38, 3), (12, 40, 4), ROPE, name="hat band"),
        # a crow perched on the west arm (viewer's right)
        E((-4, 29, 6), (2, 33, 10), CROW, name="crow body"),
        E((-6, 33, 6), (-2, 36, 9), CROW, name="crow head"),
        E((-8, 33, 7), (-6, 34, 8), "steel", name="crow beak"),
        E((2, 29, 7), (5, 31, 9), CROW, name="crow tail"),
    ]
    return Prop("scarecrow", {"scarecrow": els}, shift={"scarecrow": (0, 16, 0)})
