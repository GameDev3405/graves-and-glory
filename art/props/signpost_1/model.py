"""Signpost, One Arm (`signpost_1`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/signpost_1/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). Footprint x 0..16, y 0..48, z 0..16 px; exactly 3 cells tall, so one part stored shifted down 16 px.
A 4x4 stained-timber post (45 px, a little over a Knight) on a rubble footing, one blank 7 px arm board on the front
pointing east (the viewer's left) with a carved arrow. Shared builder: art/props/signlib.py.
Build:  python art/props/build_props.py signpost_1      Card:  python tools/prop_card.py signpost_1
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from signlib import sign


def build():
    return sign("signpost_1", [("front", True, 33)])
