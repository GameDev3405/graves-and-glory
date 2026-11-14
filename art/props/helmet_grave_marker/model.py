"""Giant's Helmet Grave (`helmet_grave_marker`): 3x2x3 cells, anchor cell (1,0,1). Brief: art/props/helmet_grave_marker/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z -16..32 px. One part.

A giant's iron helmet set crown-up on the earth as a grave marker: a round dome in octagonal tiers on a rusted brow
band, a comb along the crown, a spectacle face plate with two dark eyes and a long nasal, dark iron gone to rust
in blooms. It took a blow once: the east side of the dome is dented flat and split. People leave a few stones and a posy.
Build:  python art/props/build_props.py helmet_grave_marker      Card:  python tools/prop_card.py helmet_grave_marker
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat   # noqa: F401
from giant_bones import mound, octagon
from giant_relics import rust, old_iron, stone, DARK, POPPY, STEM, DSTONE

C = (8, 8)


def build():
    H = old_iron(seed=5, bloom=0.16, moss_h=2, top_moss=0.1, top=0)   # flat-lit ledges: a dome, not a pine cone
    B = rust(seed=6, iron=0.12, moss_h=4)
    els = []
    els += octagon(C, 20, "y", 0, 5, B, name="brow band", skip="down")
    els += octagon(C, 19, "y", 5, 14, H, name="skull wall")
    els += octagon(C, 18, "y", 14, 18, H, name="dome")
    els += octagon(C, 16, "y", 18, 22, H, name="dome")
    t = octagon(C, 13, "y", 22, 26, H, name="dome")
    t[0].to[0] -= 3; t[0].name = "dome, dented flat on the east"
    els += t
    els += octagon(C, 10, "y", 26, 29, H, name="dome")
    els += octagon(C, 6, "y", 29, 31, H, name="crown")
    els.append(E((6, 27, -3), (10, 32, 19), B, skip="down", name="comb"))
    # the split in the dented side
    D = Mat(DARK, 2, edges=False, lit=False)
    els.append(E((26, 14, 5), (27, 18, 8), D, skip="west", name="split"))
    # spectacle face plate on the front, two eyes and a nasal
    z0, z1 = -13, -11
    F = old_iron(seed=9, bloom=0.18)
    els += [E((-2, 3, z0), (18, 8, z1), F, skip="south", name="cheek plate"),
            E((-2, 14, z0), (18, 18, z1), F, skip="south", name="brow plate"),
            E((-2, 8, z0), (1, 14, z1), F, skip="south", name="eye rim"),
            E((15, 8, z0), (18, 14, z1), F, skip="south", name="eye rim"),
            E((7, 1, z0 - 1), (9, 17, z1), F, skip="south", name="nasal"),
            E((1, 8, z0 + 1), (15, 14, z1), D, skip="south east west up down", name="eyes (the dark inside)")]
    # earth heaped round the band, a few stones and a posy left at the front
    els += mound((-16, 0, -16), (32, 4, 32), layers=2, seed=7)
    S = stone(moss_h=0, top_moss=0.2, seed=8, ramp=DSTONE)
    els += [E((-10, 0, -15), (-6, 3, -11), S, skip="down", name="left stone"),
            E((-6, 0, -16), (-3, 2, -13), stone(moss_h=0, top_moss=0, seed=9), skip="down", name="left stone"),
            E((20, 0, -15), (24, 3, -12), S, skip="down", name="left stone")]
    els += [E((10, 0, -16), (16, 1, -14), Mat(STEM, 2, edges=False), skip="down", name="posy stems"),
            E((14, 0, -16), (17, 2, -13), Mat(POPPY, 2, edges=False), skip="down", name="poppies")]
    return Prop("helmet_grave_marker", {"helmet_grave_marker": els})
