"""Tall Brazier (`brazier_tall`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/brazier_tall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..48, z 0..16 px.
A big iron fire bowl (rim at 34 px, a Knight's shoulder) on a stone pedestal with an iron-banded column and four iron
claws under the bowl. The fire is a glow part (full-bright) with an animated texture of its own (proplib Anim, the
brazier's live fire): breathing embers and two crossed pairs of flame planes (one pair turned 45 degrees) rising
16 px above the rim, so it reads by day and from every side. The prop is 3 cells tall, so both parts are stored shifted down 16 px (y -16..32).
Built in the style of `brazier` (art/props/brazier/model.py), which stays as it is.
Build:  python art/props/build_props.py brazier_tall      Card:  python tools/prop_card.py brazier_tall
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, HOT, IRON, pat_iron, pat_brick, Anim, FIRE_ANIM, flames   # noqa: F401

# dark slate iron and stone so the stand reads as dark metal against deepslate; the rim keeps a pale iron highlight
DIRON = Mat(make_ramp("3A3D45"), 2, pat_iron)
DSTONE = Mat(make_ramp("4A4C52"), 2, pat_brick, top=0)
DSTONE_TOP = Mat(DSTONE.ramp, 2, lambda f, P, c, r, w, h: (-1 if h3(*P, 41) < 0.05 else 0), top=0)
RIM = Mat(IRON, 3, pat_iron)

def build():
    b = [
        # stone pedestal: a plinth, a step, a square column with two iron bands
        E((1, 0, 1), (15, 4, 15), DSTONE, skip="down", name="plinth", faces={"up": DSTONE_TOP}),
        E((2, 4, 2), (14, 6, 14), DSTONE, skip="down", name="plinth step", faces={"up": DSTONE_TOP}),
        E((4, 6, 4), (12, 21, 12), DSTONE, skip="down up", name="column"),
        E((3, 8, 3), (13, 10, 13), DIRON, name="column band"),
        E((3, 17, 3), (13, 19, 13), DIRON, name="column band"),
        # iron cap and four claws holding the bowl
        E((3, 21, 3), (13, 23, 13), DIRON, name="cap"),
        E((0, 22, 6), (2, 27, 10), DIRON, name="claw west"), E((14, 22, 6), (16, 27, 10), DIRON, name="claw east"),   # 1 px proud of
        E((6, 22, 0), (10, 27, 2), DIRON, name="claw front"), E((6, 22, 14), (10, 27, 16), DIRON, name="claw back"),   # the bowl course (no z-fight)
        # the bowl: a base, a lower course, four walls, a heavy rim, two carrying rings
        E((2, 23, 2), (14, 25, 14), DIRON, name="bowl base"),
        E((1, 25, 1), (15, 28, 15), DIRON, skip="up", name="bowl course"),
        E((0, 28, 0), (16, 32, 2), DIRON, name="bowl wall"), E((0, 28, 14), (16, 32, 16), DIRON, name="bowl wall"),
        E((0, 28, 2), (2, 32, 14), DIRON, name="bowl wall"), E((14, 28, 2), (16, 32, 14), DIRON, name="bowl wall"),
        E((-1, 32, -1), (17, 34, 2), RIM, name="rim"), E((-1, 32, 14), (17, 34, 17), RIM, name="rim"),
        E((-1, 32, 2), (2, 34, 14), RIM, name="rim"), E((14, 32, 2), (17, 34, 14), RIM, name="rim"),
        E((-1, 28, 7), (0, 32, 9), DIRON, name="ring west"), E((16, 28, 7), (17, 32, 9), DIRON, name="ring east"),
    ]
    f = [
        E((2, 28, 2), (14, 32, 14), "embers", skip="down", name="coal bed"),
        E((3, 32, 3), (13, 34, 13), "embers", skip="down", name="coal heap"),
        E((5, 34, 5), (11, 35, 11), "embers", skip="down", name="coal crest"),
    ] + flames(8, 8, 32, 48, 6) + [   # live fire: the planes stand at x 8 / z 8 and on the diagonals, never on an ember face
        E((2, 32, 8), (14, 45, 8), "flame", rot=("y", 45, (8, 32, 8)), name="flame diag a"),
        E((8, 32, 2), (8, 45, 14), "flame", rot=("y", 45, (8, 32, 8)), name="flame diag b"),
    ]
    return Prop("brazier_tall", {"brazier_tall": b, "brazier_tall_fire": f}, glow=("brazier_tall_fire",),
                anim={"brazier_tall_fire": Anim(**FIRE_ANIM)},
                shift={"brazier_tall": (0, 16, 0), "brazier_tall_fire": (0, 16, 0)})
