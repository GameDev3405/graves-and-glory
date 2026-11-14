"""Wall Brazier (`brazier_wall`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/brazier_wall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px. The back (south, z 16) sits flush on a wall.
An iron bracket on a wall plate holds a small bars-and-hoops basket of glowing coals out over the room.
Build:  python art/props/build_props.py brazier_wall      Card:  python tools/prop_card.py brazier_wall
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, Anim, FIRE_ANIM, flames   # noqa: F401


def build():
    b = [
        # wall plate with rivets, the arm and its stepped brace
        E((5, 1, 15), (11, 11, 16), "iron", skip="south", name="wall plate"),
        E((6, 9, 14), (7, 10, 15), "steel", name="rivet"), E((9, 9, 14), (10, 10, 15), "steel", name="rivet"),
        E((6, 3, 14), (7, 4, 15), "steel", name="rivet"), E((9, 3, 14), (10, 4, 15), "steel", name="rivet"),
        E((7, 8, 7), (9, 10, 15), "iron", name="arm"),
        E((7, 5, 12), (9, 7, 15), "iron", name="brace top"), E((7, 3, 13), (9, 5, 15), "iron", name="brace low"),
        E((7, 6, 10), (9, 8, 12), "iron", name="brace stay"),
        # the basket: a base plate on the arm end, eight upright bars with 1 px slits, a rim hoop
        E((4, 10, 3), (12, 11, 11), "iron", name="base plate"),
        E((7, 11, 2), (9, 15, 4), "iron", name="bar front"), E((7, 11, 10), (9, 15, 12), "iron", name="bar back"),
        E((3, 11, 6), (5, 15, 8), "iron", name="bar west"), E((11, 11, 6), (13, 15, 8), "iron", name="bar east"),
        E((4, 11, 3), (6, 15, 5), "iron", name="bar nw"), E((10, 11, 3), (12, 15, 5), "iron", name="bar ne"),
        E((4, 11, 9), (6, 15, 11), "iron", name="bar sw"), E((10, 11, 9), (12, 15, 11), "iron", name="bar se"),
        E((3, 15, 2), (13, 16, 4), "iron", name="rim"), E((3, 15, 10), (13, 16, 12), "iron", name="rim"),
        E((3, 15, 4), (5, 16, 10), "iron", name="rim"), E((11, 15, 4), (13, 16, 10), "iron", name="rim"),
    ]
    c = [
        E((4, 11, 3), (12, 14, 11), "embers", skip="down", name="coal bed"),
        E((5, 14, 4), (11, 15, 10), "embers", skip="down", name="coal heap"),
        E((6, 15, 5), (10, 16, 9), "embers", skip="down", name="coal crest"),
    ] + flames(8, 7, 12, 18, 3)   # live fire: short tongues over the rim (the footprint allows 2 px above the cell)
    return Prop("brazier_wall", {"brazier_wall": b, "brazier_wall_coals": c}, glow=("brazier_wall_coals",),
                anim={"brazier_wall_coals": Anim(**FIRE_ANIM)})
