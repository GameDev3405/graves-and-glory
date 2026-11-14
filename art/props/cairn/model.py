"""Giant's Cairn (`cairn`): 3x3x3 cells, anchor cell (1,0,1). Brief: art/props/cairn/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..48, z -16..32 px. The prop is exactly 3 cells tall, so it
stays one part stored shifted down 16 px (y -16..32).

A cairn over a giant's resting place: a long low barrow of earth, a ring of boulders each as big as a man's chest,
two more courses of smaller stones on them and a flat capstone, and on the capstone a little heap of fist-sized
stones that people still leave there.
Build:  python art/props/build_props.py cairn      Card:  python tools/prop_card.py cairn
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, blob   # noqa: F401
from giant_bones import mound
from giant_relics import stone, GSTONE, DSTONE


def build():
    A = stone(moss_h=5, top_moss=0.5, seed=1)
    B = stone(moss_h=4, top_moss=0.45, seed=2, ramp=DSTONE)
    C = stone(moss_h=0, top_moss=0.35, seed=6)
    D = stone(moss_h=0, top_moss=0.3, seed=7, ramp=DSTONE)
    els = []
    # the barrow under it, longer north-south
    els += mound((-15, 0, -16), (31, 4, 32), layers=2, seed=9, name="barrow")
    # bottom ring: five boulders round a core
    for frm, to, m, sd in (((-14, 2, -10), (4, 18, 8), A, 11), ((6, 2, -13), (26, 17, 4), B, 12),
                           ((14, 2, 4), (31, 16, 22), A, 13), ((-13, 2, 8), (5, 15, 27), B, 14),
                           ((3, 2, 14), (20, 14, 30), A, 15)):
        els += blob(frm, to, m, layers=2, step=4, seed=sd, name="boulder")
    els.append(E((-2, 2, -2), (18, 16, 18), D, skip="down", name="core stone (hidden fill)"))
    # second course
    for frm, to, m, sd in (((-8, 16, -6), (8, 28, 10), C, 21), ((6, 15, -7), (22, 27, 8), D, 22),
                           ((0, 14, 6), (18, 27, 22), C, 23)):
        els += blob(frm, to, m, layers=2, step=4, seed=sd, name="stone")
    # third course and the capstone, a little off true
    els += blob((-1, 26, -1), (17, 36, 15), D, layers=2, step=3, seed=31, name="top stone")
    els.append(E((0, 36, 0), (14, 40, 12), C, skip="down", name="capstone", rot=("y", 22.5, (0, 36, 0))))
    # the little heap of stones left by people
    els += [E((4, 40, 3), (8, 42, 6), stone(moss_h=0, top_moss=0, seed=40), skip="down", name="left stone"),
            E((8, 40, 5), (11, 43, 8), stone(moss_h=0, top_moss=0, seed=41, ramp=DSTONE), skip="down", name="left stone"),
            E((6, 42, 4), (9, 44, 7), stone(moss_h=0, top_moss=0, seed=42), skip="down", name="left stone")]
    return Prop("cairn", {"cairn": els}, shift={"cairn": (0, 16, 0)})
