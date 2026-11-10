"""Drowned Rowboat (`drowned_boat`): 3x1x2 cells, anchor cell (1,0,0). Brief: art/props/drowned_boat/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..16, z 0..32 px. Elements stay inside -16..32 per part;
a part that cannot is stored shifted (Prop(shift={part: (dx,dy,dz)})), like the forge hood.

Rules that the card checks: whole-px faces (1 texel per model px), every colour from a proplib ramp or make_ramp,
elements within the budget in brief.md, model inside the footprint (2 px overhang at most).
Build:  python art/props/build_props.py drowned_boat      Card:  python tools/prop_card.py drowned_boat
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401  (materials: proplib.MAT keys)


def build():
    drowned_boat = [  # placeholder: replace with the real build
        E((-14, 0, 2), (30, 16, 30), "plank_x", name="placeholder"),
    ]
    return Prop("drowned_boat", {"drowned_boat": drowned_boat})
