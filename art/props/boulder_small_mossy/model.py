"""Small Boulder (mossy) (`boulder_small_mossy`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/boulder_small_mossy/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z same px. Shapes and stones live in art/props/boulderlib.py.
Build:  python art/props/build_props.py boulder_small_mossy      Card:  python tools/prop_card.py boulder_small_mossy
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import Prop
from boulderlib import boulder


def build():
    return Prop("boulder_small_mossy", {"boulder_small_mossy": boulder("small", "mossy")})
