"""Giant's Helmet Grave (`helmet_grave_marker`): 3x2x3 cells, anchor cell (1,0,1). Brief: art/props/helmet_grave_marker/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z -16..32 px. One part.

A giant's barrel great helm, fallen and tipped 22.5 degrees onto its east side, the earth heaped over its west foot:
a flat-topped, straight-sided drum with a chamfered (eight-sided) section, a riveted band round the brow, a face plate
with a cross-shaped eye slit and breath slots, all looking north. A broken giant's sword blade stands upright in the
earth beside it as the grave marker; a few stones and a posy are left in front. Dark old iron, very little rust.
Build:  python art/props/build_props.py helmet_grave_marker      Card:  python tools/prop_card.py helmet_grave_marker
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat   # noqa: F401
from giant_bones import mound
from giant_relics import old_iron, stone, DARK, POPPY, STEM, DSTONE

CX, CZ = 8, 8          # the helm's axis (x, z) before it tips
OY = 6                 # the helm's foot (local y 0) and the pivot it tips about: its east edge just touches the ground
TIP = ("z", -22.5)     # leaning over onto its east side


def helm(frm, to, mat, name, skip=""):
    """A box in the upright helm's frame, tipped with the whole helm about its foot."""
    return E(frm, to, mat, name=name, skip=skip, rot=(TIP[0], TIP[1], (CX, OY, CZ)))


def build():
    H = old_iron(seed=5, bloom=0.06, moss_h=0, top_moss=0.03, top=1, runs=0.04)
    B = old_iron(seed=6, bloom=0.06, moss_h=0, runs=0.04)
    F = old_iron(seed=9, bloom=0.05, runs=0.03)
    D = Mat(DARK, 2, edges=False, lit=False)
    y0, y1 = OY, OY + 23                               # the drum: 23 px tall, flat top
    els = []
    # the drum: a cross of two boxes plus a square, so its section is a chamfered square (four wide flat faces,
    # four narrow corner faces). Tops step 1 px (the long east-west box highest) so no two tops share a plane.
    els += [helm((CX - 15, y0, CZ - 9), (CX + 15, y1, CZ + 9), H, "drum", skip="down"),
            helm((CX - 9, y0, CZ - 15), (CX + 9, y1 - 1, CZ + 15), H, "drum", skip="down"),
            helm((CX - 13, y0, CZ - 13), (CX + 13, y1 - 2, CZ + 13), H, "drum", skip="down")]
    # riveted brow band, 1 px proud, just under the top
    b0, b1 = y1 - 7, y1 - 4
    els += [helm((CX - 16, b0, CZ - 9), (CX + 16, b1, CZ + 9), B, "brow band", skip="up down"),
            helm((CX - 9, b0, CZ - 16), (CX + 9, b1, CZ + 16), B, "brow band", skip="up down"),
            helm((CX - 14, b0, CZ - 14), (CX + 14, b1, CZ + 14), B, "brow band", skip="up down")]
    rz = CZ - 17
    for rx in (CX - 7, CX - 1, CX + 5):
        els.append(helm((rx, b0 + 1, rz), (rx + 2, b1 - 1, CZ - 16), B, "rivet", skip="south"))
    for rzz in (CZ - 4, CZ + 3):
        els.append(helm((CX + 16, b0 + 1, rzz), (CX + 17, b1 - 1, rzz + 2), B, "rivet", skip="west"))
    # the face plate on the north face (z = CZ - 15), 1 px proud, with a cross-shaped eye slit and two breath slots
    z0, z1 = CZ - 16, CZ - 15
    u = lambda a, b, c, d, m, nm: helm((CX + a, y0 + b - 3, z0), (CX + c, y0 + d - 3, z1), m, nm, skip="south")
    L, R = -9, 9                                       # the plate spans the north face, x CX-9..CX+9
    els += [u(L, 17, -1, 19, F, "face plate"), u(1, 17, R, 19, F, "face plate"),       # over the eye slit
            u(L, 14, -7, 17, F, "face plate"), u(7, 14, R, 17, F, "face plate"),       # slit ends
            u(L, 3, -1, 14, F, "face plate"), u(-1, 3, 1, 7, F, "face plate"),         # cheek, chin under the slit
            u(1, 3, R, 6, F, "face plate"), u(1, 6, 3, 14, F, "face plate"), u(7, 6, R, 14, F, "face plate"),
            u(3, 8, 7, 9, F, "face plate"), u(3, 11, 7, 14, F, "face plate")]
    els += [u(-7, 14, 7, 17, D, "eye slit"), u(-1, 7, 1, 14, D, "eye slit, the long stroke"),
            u(-1, 17, 1, 19, D, "eye slit, the short stroke"),
            u(3, 6, 7, 8, D, "breath slot"), u(3, 9, 7, 11, D, "breath slot")]
    # earth heaped over the helm's raised west foot and round its east edge: half sunk
    els += mound((-16, 0, -12), (14, 5, 30), layers=1, seed=7)
    els += mound((-14, 5, -8), (6, 9, 26), layers=1, seed=11)
    els += mound((-12, 9, -4), (-2, 12, 20), layers=1, seed=12)
    els += mound((18, 0, -8), (32, 3, 24), layers=1, seed=13)
    # a broken giant's sword blade stuck upright in the earth west of the helm: the grave marker itself
    S = old_iron(seed=3, bloom=0.05, moss_h=3, runs=0.0)
    els += [E((-15, 0, 22), (-12, 22, 30), S, skip="down", name="sword blade"),
            E((-14, 0, 25), (-13, 20, 27), D, skip="down east west", name="blade fuller"),
            E((-15, 22, 22), (-12, 26, 27), S, name="blade, broken off")]
    # a few stones and a posy left in front
    St = stone(moss_h=0, top_moss=0.2, seed=8, ramp=DSTONE)
    els += [E((-12, 0, -15), (-8, 3, -11), St, skip="down", name="left stone"),
            E((24, 0, -15), (28, 3, -12), St, skip="down", name="left stone"),
            E((4, 0, -16), (10, 1, -14), Mat(STEM, 2, edges=False), skip="down", name="posy stems"),
            E((8, 0, -16), (11, 2, -13), Mat(POPPY, 2, edges=False), skip="down", name="poppies")]
    return Prop("helmet_grave_marker", {"helmet_grave_marker": els})
