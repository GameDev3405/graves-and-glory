"""Tall Brazier (`brazier_tall`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/brazier_tall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..48, z 0..16 px.
A big iron fire bowl (rim at 34 px, a Knight's shoulder) on a stone pedestal with an iron-banded column and four iron
claws under the bowl. The fire is a glow part (full-bright): a coal bed with yellow and orange flame tongues rising
above the rim, so it reads by day. The prop is 3 cells tall, so both parts are stored shifted down 16 px (y -16..32).
Built in the style of `brazier` (art/props/brazier/model.py), which stays as it is.
Build:  python art/props/build_props.py brazier_tall      Card:  python tools/prop_card.py brazier_tall
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, HOT   # noqa: F401  (materials: proplib.MAT keys)

# flames: the coals' HOT ramp, unlit (a glow part). Orange tongues darken a step at their roots; the yellow core is
# the brightest tone with an orange foot.
FLAME = Mat(HOT, 3, lambda f, P, c, r, w, h: -1 if f not in ("up", "down") and r >= h - 2 else 0, edges=False, lit=False)
CORE = Mat(HOT, 4, lambda f, P, c, r, w, h: -1 if f not in ("up", "down") and h > 3 and r >= h - 1 else 0, edges=False, lit=False)


def build():
    b = [
        # stone pedestal: a plinth, a step, a square column with two iron bands
        E((1, 0, 1), (15, 4, 15), "stone", skip="down", name="plinth", faces={"up": "stone_top"}),
        E((2, 4, 2), (14, 6, 14), "stone", skip="down", name="plinth step", faces={"up": "stone_top"}),
        E((4, 6, 4), (12, 21, 12), "stone", skip="down up", name="column"),
        E((3, 8, 3), (13, 10, 13), "iron", name="column band"),
        E((3, 17, 3), (13, 19, 13), "iron", name="column band"),
        # iron cap and four claws holding the bowl
        E((3, 21, 3), (13, 23, 13), "iron", name="cap"),
        E((1, 22, 6), (3, 27, 10), "iron", name="claw west"), E((13, 22, 6), (15, 27, 10), "iron", name="claw east"),
        E((6, 22, 1), (10, 27, 3), "iron", name="claw front"), E((6, 22, 13), (10, 27, 15), "iron", name="claw back"),
        # the bowl: a base, a lower course, four walls, a heavy rim, two carrying rings
        E((2, 23, 2), (14, 25, 14), "iron", name="bowl base"),
        E((1, 25, 1), (15, 28, 15), "iron", skip="up", name="bowl course"),
        E((0, 28, 0), (16, 32, 2), "iron", name="bowl wall"), E((0, 28, 14), (16, 32, 16), "iron", name="bowl wall"),
        E((0, 28, 2), (2, 32, 14), "iron", name="bowl wall"), E((14, 28, 2), (16, 32, 14), "iron", name="bowl wall"),
        E((-1, 32, -1), (17, 34, 2), "iron", name="rim"), E((-1, 32, 14), (17, 34, 17), "iron", name="rim"),
        E((-1, 32, 2), (2, 34, 14), "iron", name="rim"), E((14, 32, 2), (17, 34, 14), "iron", name="rim"),
        E((-1, 28, 7), (0, 32, 9), "iron", name="ring west"), E((16, 28, 7), (17, 32, 9), "iron", name="ring east"),
    ]
    f = [
        E((2, 28, 2), (14, 32, 14), "coal", skip="down", name="coal bed"),
        E((3, 32, 3), (13, 34, 13), "coal", skip="down", name="coal heap"),
        # flame: a yellow core rising to a tip, orange tongues round it, an orange crown
        E((4, 34, 4), (12, 39, 12), CORE, skip="down", name="flame core"),
        E((6, 39, 6), (10, 45, 10), CORE, skip="down", name="flame tip"),
        E((7, 45, 7), (9, 48, 9), FLAME, skip="down", name="flame crown"),
        E((2, 34, 2), (6, 41, 6), FLAME, skip="down", name="tongue nw"),
        E((10, 34, 10), (14, 42, 14), FLAME, skip="down", name="tongue se"),
        E((10, 34, 2), (14, 38, 6), FLAME, skip="down", name="tongue ne"),
        E((2, 34, 10), (6, 37, 14), FLAME, skip="down", name="tongue sw"),
        E((7, 34, 2), (10, 36, 4), FLAME, skip="down", name="tongue front"),
        E((11, 39, 5), (13, 43, 7), FLAME, skip="down", name="lick"),
    ]
    return Prop("brazier_tall", {"brazier_tall": b, "brazier_tall_fire": f}, glow=("brazier_tall_fire",),
                shift={"brazier_tall": (0, 16, 0), "brazier_tall_fire": (0, 16, 0)})
