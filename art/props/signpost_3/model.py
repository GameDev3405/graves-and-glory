"""Signpost, Three Arms (`signpost_3`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/signpost_3/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..16, y 0..48, z 0..16 px; exactly 3 cells tall, so one part stored shifted down 16 px.
Two front boards pointing opposite ways and a third on the back pointing east, a crossroads post.
Shared builder: art/props/signlib.py.
Build:  python art/props/build_props.py signpost_3      Card:  python tools/prop_card.py signpost_3
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from signlib import sign


def build():
    return sign("signpost_3", [("front", True, 35), ("front", False, 23), ("back", True, 15)])
