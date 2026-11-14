"""Giant's Skull Fragment (`giant_skull_fragment`): 3x3x3 cells, anchor cell (1,0,1). Brief: art/props/giant_skull_fragment/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..48, z -16..32 px. One part, stored shifted down 16 px.

The upper skull of a giant, sunk to its teeth: two round eye sockets under a heavy brow, the nose hole between, a row of
upper teeth in the earth, the dome of the cranium behind. The dome is one octagonal drum and three rings, each 2 px
narrower; the east socket's rim and that side of the dome are broken away in steps, the break showing the bone's
spongy core and the dark hollow inside. Moss lines the socket floors.
Sockets are octagonal rings (straight and 45-degree bars), not 1 px stairs.
Build:  python art/props/build_props.py giant_skull_fragment      Card:  python tools/prop_card.py giant_skull_fragment
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, curve   # noqa: F401
from giant_bones import bone, mound, octagon, octagon_cut, octagon_ring, relight, MARROW_FACE, BREAK_FACE, MARROW, MOSSY

FZ = -14                  # front plane of the face (the socket rims)
EYES = ((-3, 25), (19, 25))   # socket centres (x, y), west and east (the east is the viewer's left from the front)
ER = 9                    # socket rim outer inradius


def build():
    B = bone(moss_h=5, seed=7)
    dark = Mat(MARROW, 0, None, edges=False, lit=False)          # deep shadow inside the skull
    els = []
    # cranium: one dome, a deep drum and three rings each stepping in 2 px, broken away on the east: each ring stops
    # further west than the one below (a stepped, jagged break), the cut faces and the drum's exposed top show the core
    D = bone(moss_h=5, seed=7, grain=False)
    els += octagon((8, 12), 20, "y", 0, 24, D, name="cranium", skip="down", faces={"up": BREAK_FACE})
    for (r, y0, y1, xmax) in ((18, 24, 30, 17), (16, 30, 36, 13), (14, 36, 41, 9)):
        els += octagon_cut((8, 12), r, y0, y1, D, xmax, name="dome", cut_faces={"east": BREAK_FACE})
    els.append(E((18, 24, -2), (26, 25, 24), dark, skip="down east west north south", name="hollow of the broken dome"))
    # face plate between and round the sockets, set 3 px behind the rims
    els.append(E((-14, 10, FZ + 3), (30, 34, FZ + 8), B, name="face"))
    # eye sockets: a dark octagon deep inside each ring; the east rim lost its outer bars (broken)
    for i, (cx, cy) in enumerate(EYES):
        els.append(E((cx - ER + 2, cy - ER + 2, FZ + 2), (cx + ER - 2, cy + ER - 2, FZ + 3), dark, skip="up down east west south",
                     name="socket shadow"))
        els.append(E((cx - 5, cy - ER + 2, FZ + 1), (cx + 5, cy - ER + 4, FZ + 3), MOSSY, name="moss in the socket"))
        els += octagon_ring((cx, cy), ER, 3, FZ, FZ + 4, B, drop=(1, 2) if i else (), name="socket rim")
    # nose hole: a dark diamond under and between the sockets, with a bone bridge above it
    els.append(E((5, 12, FZ + 2), (11, 18, FZ + 3), dark, name="nose hole", rot=("z", 45, (8, 15, FZ + 2.5))))
    els.append(E((4, 18, FZ + 1), (12, 25, FZ + 4), B, name="nose bridge"))
    # cheekbones under the sockets, and the upper teeth in the earth
    els.append(E((-14, 9, FZ + 1), (4, 14, FZ + 6), B, name="cheekbone"))
    els.append(E((12, 9, FZ + 1), (29, 14, FZ + 6), B, name="cheekbone"))
    els.append(E((-8, 0, FZ + 3), (24, 10, FZ + 9), B, skip="down", name="upper jaw"))
    for k, x in enumerate((-7, -1, 5, 11, 17)):
        els.append(E((x, 0, FZ + 1), (x + 5, 11 - 2 * (k % 2), FZ + 5), bone(moss_h=3, seed=10 + k, base=3), skip="down", name="tooth"))
    # brow ridge: one heavy bar over both sockets, dipping over the nose (a curve through three points)
    els += curve([(-15, 33, FZ - 1), (8, 31, FZ - 1), (27, 33, FZ - 1)], (6, 9), B, along="x", name="brow ridge")
    # earth around it
    els += mound((-16, 0, -16), (32, 5, 32), layers=2, seed=8)
    return Prop("giant_skull_fragment", {"giant_skull_fragment": relight(els)}, shift={"giant_skull_fragment": (0, 16, 0)})
