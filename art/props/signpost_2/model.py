"""Signpost, Two Arms (`signpost_2`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/signpost_2/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..16, y 0..48, z 0..16 px; exactly 3 cells tall, so one part stored shifted down 16 px.
The one-arm post with a second board on the front pointing the other way (west). Shared builder: art/props/signlib.py.
Build:  python art/props/build_props.py signpost_2      Card:  python tools/prop_card.py signpost_2
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from signlib import sign


def build():
    return sign("signpost_2", [("front", True, 34), ("front", False, 26)])
