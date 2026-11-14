"""Hand Pump (`hand_pump`): 2x3x1 cells, anchor cell (0,0,0). Brief: art/props/hand_pump/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..32, y 0..48, z 0..16 px; exactly 3 cells tall, one part stored shifted down 16 px.
A village water pump: a stone-block base on the east (viewer's left) with a banded timber column, an iron pump head, a spout
reaching west over a stone trough full of water, and a long lever tilted up over the trough.
Build:  python art/props/build_props.py hand_pump      Card:  python tools/prop_card.py hand_pump
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3   # noqa: F401


def build():
    els = [
        # stone trough, west half: floor, four walls, water inside
        E((1, 0, 3), (17, 3, 13), "stone", skip="down", name="trough floor"),
        E((1, 3, 3), (17, 9, 5), "stone", name="trough wall n"),
        E((1, 3, 11), (17, 9, 13), "stone", name="trough wall s"),
        E((1, 3, 5), (3, 9, 11), "stone", name="trough wall w"),
        E((3, 3, 5), (15, 7, 11), "water", skip="north south east west down", name="trough water"),
        # stone base the pump stands on
        E((17, 0, 2), (31, 10, 14), "stone", skip="down", name="pump base"),
        E((16, 10, 1), (32, 12, 15), "stone_top", name="pump coping"),
        # timber column with two iron bands, iron head, spout and a drop lip
        E((21, 12, 5), (27, 34, 11), "swood_y", name="column"),
        E((20, 16, 4), (28, 18, 12), "iron", name="band low"),
        E((20, 27, 4), (28, 29, 12), "iron", name="band high"),
        E((20, 34, 4), (28, 40, 12), "iron", name="pump head"),
        E((22, 40, 6), (26, 41, 10), "steel", name="head cap"),
        E((14, 26, 6), (21, 30, 10), "iron", name="spout"),
        E((14, 22, 6), (17, 26, 10), "iron", name="spout lip"),
        # the lever: pinned at the head, long arm over the trough tilted up, a short tail behind
        E((23, 36, 3), (26, 39, 4), "steel", name="lever pin"),
        E((23, 36, 12), (26, 39, 13), "steel", name="lever pin"),
        E((5, 37, 5), (26, 40, 7), "wood_x", rot=("z", -22.5, (26, 37, 5)), name="lever arm"),
        E((5, 37, 9), (26, 40, 11), "wood_x", rot=("z", -22.5, (26, 37, 9)), name="lever arm"),
        E((26, 36, 5), (30, 39, 11), "wood_x", name="lever tail"),
        E((29, 31, 7), (30, 36, 9), "iron", name="lever link"),
    ]
    return Prop("hand_pump", {"hand_pump": els}, shift={"hand_pump": (0, 16, 0)})
