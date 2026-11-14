"""Spear Standard (`spear_standard`): 1x3x1 cells, anchor cell (0,0,0); footprint x 0..16, y 0..48, z 0..16 px. Brief: art/props/spear_standard/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z).
A war spear planted in a cairn of stones; the shaft stands on the west side so the swallow-tailed pennant (own part,
spear_standard_pennant, so a team colour can replace it) streams east, inside the cell plus 2 px.
Build:  python art/props/build_props.py spear_standard      Card:  python tools/prop_card.py spear_standard
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3   # noqa: F401

RED = make_ramp("8E1F20")      # neutral deep red cloth
CLOTH = Mat(RED, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 5 == 0 else 0)
SX = 3                          # shaft x 3..5, z 7..9


def build():
    spear = [
        # cairn: a ring of packed stones holding the shaft
        E((0, 0, 4), (9, 2, 12), "stone", skip="down", name="cairn base"),
        E((1, 2, 5), (7, 4, 11), "stone", skip="down", name="cairn top"),
        E((6, 0, 10), (11, 3, 14), "stone", skip="down", name="stone south-east"),
        E((7, 0, 2), (11, 2, 6), "stone", skip="down", name="stone north-east"),
        E((0, 0, 11), (4, 3, 15), "stone", skip="down", name="stone south-west"),
        # shaft, leather grip wrap, iron socket, head
        E((3, 0, 7), (5, 38, 9), "wood_y", skip="down", name="shaft"),
        E((2, 14, 6), (6, 22, 10), "leather", name="grip wrap"),
        E((2, 36, 6), (6, 40, 10), "iron", name="socket collar"),
        E((1, 40, 7), (7, 42, 9), "steel", name="blade base"),
        E((2, 42, 7), (6, 45, 9), "steel", name="blade"),
        E((3, 45, 7), (5, 47, 9), "steel", name="blade upper"),
        E((3, 47, 7), (5, 48, 9), "steel", name="blade tip"),
        E((3, 41, 6), (5, 46, 10), "steel", name="blade ridge"),
    ]
    # pennant: 1 px cloth streaming east from the shaft, a gold hem top and bottom, a swallow tail, a gold charge
    P = [
        E((5, 28, 8), (9, 36, 9), CLOTH, name="hoist"), E((5, 36, 8), (9, 37, 9), "gilt", name="hem top"), E((5, 27, 8), (9, 28, 9), "gilt", name="hem bottom"),
        E((9, 29, 8), (13, 35, 9), CLOTH, name="fly"), E((9, 35, 8), (13, 36, 9), "gilt", name="hem top"), E((9, 28, 8), (13, 29, 9), "gilt", name="hem bottom"),
        E((13, 33, 8), (16, 35, 9), CLOTH, name="tail upper"), E((13, 35, 8), (16, 36, 9), "gilt", name="hem top"),
        E((13, 29, 8), (16, 31, 9), CLOTH, name="tail lower"), E((13, 28, 8), (16, 29, 9), "gilt", name="hem bottom"),
        E((16, 34, 8), (18, 36, 9), "gilt", name="tail upper tip"), E((16, 28, 8), (18, 30, 9), "gilt", name="tail lower tip"),
        E((6, 30, 7), (8, 34, 8), "gilt", name="charge"),
    ]
    sh = (0, 16, 0)
    return Prop("spear_standard", {"spear_standard": spear, "spear_standard_pennant": P},
                shift={"spear_standard": sh, "spear_standard_pennant": sh})
