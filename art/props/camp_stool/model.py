"""Camp Stool (`camp_stool`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/camp_stool/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px.
A folding X-frame stool: two pairs of legs crossed at 45 degrees (seen from the front), an iron pivot bolt, wooden foot
runners and a slung leather seat. Walk-through (no solid cell).
Build:  python art/props/build_props.py camp_stool      Card:  python tools/prop_card.py camp_stool
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3   # noqa: F401


def build():
    s = [
        # foot runners the legs stand on
        E((1, 0, 3), (4, 2, 13), "swood_z", skip="down", name="foot runner west"),
        E((12, 0, 3), (15, 2, 13), "swood_z", skip="down", name="foot runner east"),
    ]
    # two crossed leg pairs (front at z 3..7, back at z 9..13); the pair's two legs lie side by side in z
    for z in (3, 9):
        s.append(E((2, 1, z), (4, 15, z + 2), "wood_y", rot=("z", -45, (4, 1, z)), name="leg a"))
        s.append(E((12, 1, z + 2), (14, 15, z + 4), "wood_y", rot=("z", 45, (12, 1, z + 2)), name="leg b"))
        s.append(E((7, 4, z - 1 if z == 3 else z + 4), (9, 6, z if z == 3 else z + 5), "iron", name="pivot bolt"))
    s += [
        # seat rails and the slung leather seat
        E((1, 10, 2), (4, 12, 14), "swood_z", name="seat rail west"),
        E((12, 10, 2), (15, 12, 14), "swood_z", name="seat rail east"),
        E((4, 11, 3), (12, 12, 13), "leather", name="leather seat"),
        E((4, 10, 4), (12, 11, 12), "leather", skip="up", name="seat sag"),
    ]
    return Prop("camp_stool", {"camp_stool": s})
