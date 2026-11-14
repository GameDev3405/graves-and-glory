"""Wall Lamp (`street_lamp_wall`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/street_lamp_wall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px. The back (south, z 16) sits flush on a wall.
An iron wall plate with rivets, a braced arm reaching out over the room, and a boxy iron lantern hanging from the arm
tip on a short chain. The lantern glass is a glow part (full-bright). Same lantern as `street_lamp`.
Build:  python art/props/build_props.py street_lamp_wall      Card:  python tools/prop_card.py street_lamp_wall
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, Anim   # noqa: F401

# candle flicker behind the glass (proplib lamp_glass): four frames blended, in an irregular order that leaves the steady frame often
LAMP_FLICKER = Anim(frames=4, frametime=2, interpolate=True, order=[0, 1, 2, 0, 3, 1, 1, 0, 2, 3, 0, 1, 2, 2])


def build():
    b = [
        # wall plate with rivets, the arm and its stepped brace
        E((5, 6, 15), (11, 16, 16), "iron", skip="south", name="wall plate"),
        E((6, 14, 14), (7, 15, 15), "steel", name="rivet"), E((9, 14, 14), (10, 15, 15), "steel", name="rivet"),
        E((6, 7, 14), (7, 8, 15), "steel", name="rivet"), E((9, 7, 14), (10, 8, 15), "steel", name="rivet"),
        E((7, 13, 3), (9, 15, 15), "iron", name="arm"),
        E((7, 11, 13), (9, 13, 15), "iron", name="brace top"), E((7, 9, 14), (9, 11, 15), "iron", name="brace low"),
        E((7, 12, 11), (9, 13, 13), "iron", name="brace tip"),
        # arm tip hook, chain, lantern: top ring, cap, corner posts, base
        E((7, 15, 3), (9, 16, 5), "iron", name="arm cap"),
        E((7, 11, 3), (9, 13, 5), "iron", name="chain"),
        E((4, 9, 1), (12, 11, 9), "iron", name="cap eaves"),
        E((6, 11, 3), (10, 12, 7), "iron", name="cap crown"),
        E((4, 2, 1), (12, 3, 9), "iron", skip="down", name="lantern base"),
        E((4, 3, 1), (6, 9, 3), "iron", name="corner nw"), E((10, 3, 1), (12, 9, 3), "iron", name="corner ne"),
        E((4, 3, 7), (6, 9, 9), "iron", name="corner sw"), E((10, 3, 7), (12, 9, 9), "iron", name="corner se"),
    ]
    g = [E((5, 3, 2), (11, 9, 8), "lamp_glass", skip="down up", name="lantern glass")]
    return Prop("street_lamp_wall", {"street_lamp_wall": b, "street_lamp_wall_glass": g}, glow=("street_lamp_wall_glass",),
                anim={"street_lamp_wall_glass": LAMP_FLICKER})
