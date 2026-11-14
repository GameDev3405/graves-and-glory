"""Giant's Chain Link (`giant_chain_link`): 3x2x2 cells, anchor cell (1,0,0). Brief: art/props/giant_chain_link/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z 0..32 px. One part.

One link of a giant's chain, rusted through, half sunk in the earth: an oval loop of square iron bar standing on edge,
its lower half under the ground, and the next link's crown just breaking the earth inside its east end.
Build:  python art/props/build_props.py giant_chain_link      Card:  python tools/prop_card.py giant_chain_link
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop   # noqa: F401
from giant_bones import mound, relight
from giant_relics import rust

Z0, Z1 = 6, 14          # the loop's bar depth (z)


def build():
    R = rust(seed=1, iron=0.16, moss_h=3, top_moss=0.25)
    R2 = rust(seed=2, iron=0.2, moss_h=4)
    loop = [
        E((-1, 24, Z0), (17, 30, Z1), R, name="crown bar"),
        E((-14, 17, Z0 + 1), (4, 23, Z1 - 1), R, rot=("z", 45, (-5, 20, 10)), name="corner"),
        E((12, 17, Z0 + 1), (30, 23, Z1 - 1), R, rot=("z", -45, (21, 20, 10)), name="corner"),
        E((-14, 0, Z0), (-8, 17, Z1), R, skip="down", name="west side"), E((24, 0, Z0), (30, 17, Z1), R, skip="down", name="east side"),
    ]
    # the next link, at right angles, coming up through the loop's east end: only its crown shows
    nxt = [
        E((17, 13, 8), (23, 19, 18), R2, name="next link crown"),
        E((18, 7, 0), (22, 13, 12), R2, rot=("x", -45, (20, 10, 6)), name="next link corner"),
        E((18, 7, 14), (22, 13, 26), R2, rot=("x", 45, (20, 10, 20)), name="next link corner"),
        E((17, 0, 1), (23, 8, 7), R2, skip="down", name="next link foot"), E((17, 0, 19), (23, 8, 25), R2, skip="down", name="next link foot"),
    ]
    els = relight(loop + nxt)
    # the earth it sank into: heaped at the feet and filling the loop
    els += mound((-16, 0, 2), (2, 5, 22), layers=2, seed=3)
    els += mound((14, 0, 0), (32, 5, 26), layers=2, seed=4)
    els += mound((-4, 0, 4), (20, 3, 18), layers=1, seed=5)
    return Prop("giant_chain_link", {"giant_chain_link": els})
