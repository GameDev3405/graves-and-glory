"""Large Boulder (mossy) (`boulder_large_mossy`): 3x3x3 cells, anchor cell (1,0,1). Brief: art/props/boulder_large_mossy/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..48, z same px. Shapes and stones live in art/props/boulderlib.py.
Build:  python art/props/build_props.py boulder_large_mossy      Card:  python tools/prop_card.py boulder_large_mossy
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import Prop
from boulderlib import boulder


def build():
    return Prop("boulder_large_mossy", {"boulder_large_mossy": boulder("large", "mossy")})
