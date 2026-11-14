"""Stocks (`stocks`): 2x2x1 cells, anchor cell (0,0,0). Brief: art/props/stocks/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..32, y 0..32, z 0..16 px.
Village stocks: two stout posts on ground sills, a pair of boards (lower fixed, upper hinged) with two ankle holes between
them, an iron hinge at the west end (viewer's right) and a hasp and padlock at the east end, and a low bench behind for the prisoner.
Build:  python art/props/build_props.py stocks      Card:  python tools/prop_card.py stocks
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3   # noqa: F401


def build():
    els = [
        # ground sills and the two posts with capped tops
        E((1, 0, 3), (7, 3, 13), "plank_z", skip="down", name="sill"),
        E((25, 0, 3), (31, 3, 13), "plank_z", skip="down", name="sill"),
        E((2, 3, 5), (6, 30, 11), "swood_y", name="post"),
        E((26, 3, 5), (30, 30, 11), "swood_y", name="post"),
        E((1, 30, 4), (7, 32, 12), "swood_x", name="post cap"),
        E((25, 30, 4), (31, 32, 12), "swood_x", name="post cap"),
        # lower board with two ankle notches, upper board with matching notches: two 5x6 px holes
        E((6, 12, 6), (26, 14, 10), "wood_x", name="lower board"),
        E((6, 14, 6), (10, 17, 10), "wood_x", name="lower board"),
        E((15, 14, 6), (17, 17, 10), "wood_x", name="lower board"),
        E((22, 14, 6), (26, 17, 10), "wood_x", name="lower board"),
        E((6, 17, 6), (10, 20, 10), "wood_x", name="upper board"),
        E((15, 17, 6), (17, 20, 10), "wood_x", name="upper board"),
        E((22, 17, 6), (26, 20, 10), "wood_x", name="upper board"),
        E((6, 20, 6), (26, 23, 10), "wood_x", name="upper board"),
        # hinge straps at the west end (viewer's right), hasp and padlock at the east end (viewer's left)
        E((6, 13, 5), (13, 15, 6), "iron", name="hinge strap"),
        E((6, 20, 5), (13, 22, 6), "iron", name="hinge strap"),
        E((23, 14, 5), (25, 22, 6), "iron", name="hasp strap"),
        E((23, 10, 4), (26, 14, 5), "steel", name="padlock"),
        # low bench behind on two legs
        E((6, 8, 11), (26, 10, 16), "plank_x", name="bench seat"),
        E((8, 0, 12), (11, 8, 15), "wood_y", skip="down", name="bench leg"),
        E((21, 0, 12), (24, 8, 15), "wood_y", skip="down", name="bench leg"),
    ]
    return Prop("stocks", {"stocks": els})
