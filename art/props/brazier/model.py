"""Brazier (`brazier`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/brazier/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..32, z 0..16 px.
An iron basket of glowing coals on three legs (one leg to the front, two behind), a ring brace between the legs,
a bowl of eight upright bars with the coals showing between them. The coals are a glow part (full-bright).
Build:  python art/props/build_props.py brazier      Card:  python tools/prop_card.py brazier
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, cylinder   # noqa: F401


def build():
    b = [
        # three legs (front, back-left, back-right), feet and a ring brace tying them
        E((7, 0, 2), (9, 12, 4), "iron", skip="down", name="front leg"),
        E((3, 0, 9), (5, 12, 11), "iron", skip="down", name="back leg west"),
        E((11, 0, 9), (13, 12, 11), "iron", skip="down", name="back leg east"),
        E((6, 0, 1), (10, 1, 5), "iron", skip="down", name="front foot"),
        E((2, 0, 8), (6, 1, 12), "iron", skip="down", name="back foot west"),
        E((10, 0, 8), (14, 1, 12), "iron", skip="down", name="back foot east"),
        E((5, 5, 4), (11, 6, 5), "iron", name="brace front"), E((4, 5, 5), (5, 6, 9), "iron", name="brace west"),
        E((11, 5, 5), (12, 6, 9), "iron", name="brace east"), E((5, 5, 9), (11, 6, 10), "iron", name="brace back"),
        # the basket: a base plate, a lower hoop, eight upright bars with 1 px slits, a rim hoop
        E((4, 12, 4), (12, 14, 12), "iron", name="base plate"),
        E((3, 14, 3), (13, 16, 5), "iron", name="lower hoop"), E((3, 14, 11), (13, 16, 13), "iron", name="lower hoop"),
        E((3, 14, 5), (5, 16, 11), "iron", name="lower hoop"), E((11, 14, 5), (13, 16, 11), "iron", name="lower hoop"),
        E((7, 16, 3), (9, 24, 5), "iron", name="bar front"), E((7, 16, 11), (9, 24, 13), "iron", name="bar back"),
        E((3, 16, 7), (5, 24, 9), "iron", name="bar west"), E((11, 16, 7), (13, 24, 9), "iron", name="bar east"),
        E((4, 16, 4), (6, 24, 6), "iron", name="bar nw"), E((10, 16, 4), (12, 24, 6), "iron", name="bar ne"),
        E((4, 16, 10), (6, 24, 12), "iron", name="bar sw"), E((10, 16, 10), (12, 24, 12), "iron", name="bar se"),
        E((2, 24, 2), (14, 26, 4), "iron", name="rim"), E((2, 24, 12), (14, 26, 14), "iron", name="rim"),
        E((2, 24, 4), (4, 26, 12), "iron", name="rim"), E((12, 24, 4), (14, 26, 12), "iron", name="rim"),
        # two carrying rings on the rim
        E((1, 23, 7), (2, 26, 9), "iron", name="ring west"), E((14, 23, 7), (15, 26, 9), "iron", name="ring east"),
    ]
    c = [
        E((4, 14, 4), (12, 20, 12), "coal", skip="down", name="coal bed"),
        E((5, 20, 5), (11, 22, 11), "coal", skip="down", name="coal heap"),
        E((6, 22, 6), (10, 23, 10), "coal", skip="down", name="coal crest"),
        E((7, 23, 7), (9, 24, 9), "coal", skip="down", name="ember"),
    ]
    return Prop("brazier", {"brazier": b, "brazier_coals": c}, glow=("brazier_coals",))
