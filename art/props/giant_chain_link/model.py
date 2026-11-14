"""Giant's Chain Link (`giant_chain_link`): 3x2x2 cells, anchor cell (1,0,0). Brief: art/props/giant_chain_link/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z 0..32 px. One part.

A short run of a giant's chain coming out of the earth: one oval link lying flat in the grass, the next link standing
on edge through its end and half sunk, and the third link's end just showing before the run dives into a mound.
Each link is an oval of 5 px square bar (two straight sides, two end bars, four 45-degree corners), 28 px long.
Dark old iron with a little rust.
Build:  python art/props/build_props.py giant_chain_link      Card:  python tools/prop_card.py giant_chain_link
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop   # noqa: F401
from giant_bones import mound, relight
from giant_relics import old_iron

S = 2          # half the straight length of a link's sides
ZC = 16        # the run's centre line (z)


def link(plane, cu, cv, a0, a1, mat, keep=None, floor=0):
    """An oval link in `plane` ("xz": lying flat, normal y; "xy": on edge, normal z), centre (cu, cv), its bar between
    a0..a1 on the normal axis. Outer 28 x 24 px, hole 18 x 14. `keep(name)` filters pieces (to bury part of a link);
    pieces are clipped at y = floor."""
    nax = "y" if plane == "xz" else "z"
    def box(u0, u1, v0, v1, b0, b1, nm, ang=None, org=None):
        if plane == "xz": frm, to = [u0, b0, v0], [u1, b1, v1]
        else: frm, to = [u0, v0, b0], [u1, v1, b1]
        rot = None
        if ang:
            o = (org[0], (b0 + b1) / 2, org[1]) if plane == "xz" else (org[0], org[1], (b0 + b1) / 2)
            rot = (nax, ang, o)
        return (nm, frm, to, rot)
    p = [box(cu - S - 4, cu + S + 4, cv - 12, cv - 7, a0, a1, "side lo"),
         box(cu - S - 4, cu + S + 4, cv + 7, cv + 12, a0, a1, "side hi"),
         box(cu - S - 12, cu - S - 7, cv - 4, cv + 4, a0, a1, "end west"),
         box(cu + S + 7, cu + S + 12, cv - 4, cv + 4, a0, a1, "end east")]
    for su in (-1, 1):
        for sv in (-1, 1):
            c = (cu + su * (S + 6.5), cv + sv * 6.5)
            ang = 45 if su * sv < 0 else -45
            if plane == "xz": ang = -ang
            p.append(box(c[0] - 4.5, c[0] + 4.5, c[1] - 2.5, c[1] + 2.5, a0 + 1, a1 - 1,
                         "corner %s%s" % ("e" if su > 0 else "w", "hi" if sv > 0 else "lo"), ang, c))
    out = []
    for nm, frm, to, rot in p:
        if keep and not keep(nm): continue
        if not rot and to[1] <= floor: continue
        if not rot: frm[1] = max(frm[1], floor)
        out.append(E([int(v) for v in frm], [int(v) for v in to], mat, name="link " + nm, rot=rot,
                     skip="down" if frm[1] == floor else ""))
    return out


def build():
    I = old_iron(seed=1, bloom=0.08, moss_h=0, top_moss=0.08, runs=0.05)
    I2 = old_iron(seed=2, bloom=0.08, moss_h=1, top_moss=0.08, runs=0.05)
    els = []
    # link A lies flat in the grass, sunk 1 px: bar y 0..4
    els += link("xz", -2, ZC, 0, 4, I)
    # link B stands on edge through A's east end, half sunk: centre 4 px above the ground, its lower half buried
    els += link("xy", 15, 4, ZC - 3, ZC + 2, I2, keep=lambda n: n not in ("side lo", "corner wlo", "corner elo"))
    # link C: only its west end shows, going under the mound with the rest of the chain
    els += link("xz", 33, ZC, 0, 4, I, keep=lambda n: n in ("end west", "corner whi", "corner wlo"))
    # the earth the run dives into, and a little heaped where B goes down
    els += mound((20, 0, 2), (32, 7, 30), layers=2, seed=4)
    els += mound((8, 0, 10), (22, 2, 22), layers=1, seed=5)
    return Prop("giant_chain_link", {"giant_chain_link": relight(els)})
