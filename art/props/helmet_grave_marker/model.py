"""Giant's Helmet Grave (`helmet_grave_marker`): 3x2x3 cells, anchor cell (1,0,1). Brief: art/props/helmet_grave_marker/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z -16..32 px. One part.

A giant's iron helmet set crown-up on the earth as a grave marker: a round dome in thin octagonal rings on an iron brow
band, a comb along the crown, a face plate with one dark visor slit crossed by a long nasal, dark iron with a
little rust. It took a blow once: the east side of the dome is dented flat. People leave a few stones and a posy.
Build:  python art/props/build_props.py helmet_grave_marker      Card:  python tools/prop_card.py helmet_grave_marker
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat   # noqa: F401
from giant_bones import mound, octagon
from giant_relics import old_iron, stone, DARK, POPPY, STEM, DSTONE

C = (8, 8)


def build():
    H = old_iron(seed=5, bloom=0.08, moss_h=2, top_moss=0.08, top=0, runs=0.05)   # flat-lit ledges
    B = old_iron(seed=6, bloom=0.08, moss_h=4, runs=0.05)
    els = []
    els += octagon(C, 20, "y", 0, 5, B, name="brow band", skip="down")
    # the dome: a flattened round crown (superellipse, power 2.5, top at y 30) cut in thin octagonal rings. Each ring's
    # diagonals sit 1 px lower than its straight boxes (their feet hidden in the wider ring below), so even a 2 px ring
    # has solid corners and no shared plane: a smooth dome, not a stepped hive.
    edges = (5, 13, 17, 20, 23, 25, 27, 29, 30)
    for y0, y1 in zip(edges, edges[1:]):
        t = ((y0 + y1) / 2 - 5) / 25
        R = max(4, round(19 * (1 - t ** 2.5) ** 0.4))
        L = int(round(R * math.tan(math.radians(22.5))))
        cu, cv = C
        dent = 2 if 19 <= y0 <= 23 else 0      # the east side beaten flat across the middle rings
        nm = "dome, dented flat on the east" if dent else "dome"
        els += [E((cu - R, y0, cv - L), (cu + R - dent, y1, cv + L), H, name=nm, skip="down"),
                E((cu - L, y0, cv + L), (cu + L, y1, cv + R), H, name="dome", skip="down"),
                E((cu - L, y0, cv - R), (cu + L, y1, cv - L), H, name="dome", skip="down")]
        for ang in (45, -45):
            els.append(E((cu - R + dent // 2, y0 - 1, cv - L), (cu + R - dent // 2, y1 - 1, cv + L), H, name="dome",
                         skip="down", rot=("y", ang, (cu, (y0 + y1 - 2) / 2, cv))))
    els.append(E((6, 27, -1), (10, 32, 17), B, skip="down", name="comb"))
    # face plate on the flat north face of the dome only (x 0..16), so no edge stands proud of the diagonals:
    # a single dark visor slit, the nasal crossing it
    D = Mat(DARK, 2, edges=False, lit=False)
    F = old_iron(seed=9, bloom=0.08, runs=0.05)
    # each plate is cut to the flat face of the ring behind it and stands 1 px proud, so no edge sticks out past the
    # dome's diagonals as a fin: cheek and slit on the lowest ring (face z -11), brow on the next (face z -10)
    els += [E((0, 5, -12), (16, 10, -11), F, skip="south", name="cheek plate"),
            E((1, 13, -11), (15, 17, -10), F, skip="south", name="brow plate"),
            E((0, 10, -12), (2, 12, -11), F, skip="south", name="slit end"),
            E((14, 10, -12), (16, 12, -11), F, skip="south", name="slit end"),
            E((7, 3, -13), (9, 16, -10), F, skip="south", name="nasal"),
            E((2, 10, -12), (14, 12, -11), D, skip="south east west up down", name="visor slit (the dark inside)")]
    # earth heaped round the band, a few stones and a posy left at the front
    els += mound((-16, 0, -16), (32, 3, 32), layers=1, seed=7)
    S = stone(moss_h=0, top_moss=0.2, seed=8, ramp=DSTONE)
    els += [E((-10, 0, -15), (-6, 3, -11), S, skip="down", name="left stone"),
            E((-6, 0, -16), (-3, 2, -13), stone(moss_h=0, top_moss=0, seed=9), skip="down", name="left stone"),
            E((20, 0, -15), (24, 3, -12), S, skip="down", name="left stone")]
    els += [E((10, 0, -16), (16, 1, -14), Mat(STEM, 2, edges=False), skip="down", name="posy stems"),
            E((14, 0, -16), (17, 2, -13), Mat(POPPY, 2, edges=False), skip="down", name="poppies")]
    return Prop("helmet_grave_marker", {"helmet_grave_marker": els})
