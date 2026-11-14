"""Giant's Rib (`giant_rib`): 3x6x1 cells, anchor cell (1,0,0). Brief: art/props/giant_rib/brief.md.

A BIG prop: model every element in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min corner
of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -16..32, y 0..96, z 0..16 px.
Prop.split cuts the elements into 3-cell regions (parts giant_rib_<n>); after changing which regions hold elements run
python tools/new_prop.py giant_rib --sync  so the def lists the same parts. Shape helpers: wheel, cylinder, blob, curve.
Build:  python art/props/build_props.py giant_rib      Card:  python tools/prop_card.py giant_rib

One rib of a giant, sunk in the earth at its foot, rising and bending east in even 22.5-degree turns to a clean flat
cut at x = 32. The section is octagonal (two crossed boxes a step) and tapers from 16 px at the foot to 12 at the cut.
Chaining: a second rib facing the other way (rotated 180) with its anchor 3 cells east of this one meets that cut face
to face, and the pair stands as a round arch 6 cells tall over a 4-cell gap. A line of pairs is the Rivergate ribcage.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, wheel, cylinder, blob, curve   # noqa: F401
from giant_bones import bone, octa_chain, mound, relight, MARROW_FACE

SIZE = (3, 6, 1)
ZC = 8                  # the rib's mid-plane (the cell's centre, so a turned rib lines up with this one)
START = (-8, 0)
# (turn, length, width): one 22.5-degree turn a step, the steps shortening evenly so the bend tightens smoothly
TURNS = [(0, 38, 16), (22.5, 29, 14), (45, 21, 14), (67.5, 10, 12)]


def build():
    B = bone(moss_h=7, seed=1)
    # the last, level step runs from wherever the turns end to exactly x = 32 (the cut the next rib meets)
    x = START[0] + sum(L * math.sin(math.radians(th)) for th, L, _ in TURNS)
    steps = TURNS + [(90, 32 - round(x), 12)]
    els, end = octa_chain(START, steps, ZC, B, name="rib", end_faces={"east": MARROW_FACE})
    # earth heaped around the foot, mossy on top, and a bone shard broken off long ago
    els += mound((-16, 0, -2), (12, 8, 18), layers=3, seed=3)
    els.append(E((8, 0, 3), (18, 3, 9), bone(moss_h=1, seed=2), skip="down", name="bone shard", rot=("y", 22.5, (8, 0, 3))))
    return Prop.split("giant_rib", relight(els), SIZE)
