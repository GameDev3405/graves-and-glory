"""Giant's Vertebra (`giant_vertebra`): 3x3x2 cells, anchor cell (1,0,0). Brief: art/props/giant_vertebra/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..48, z 0..32 px. One part, stored shifted down 16 px (y -16..32).

One vertebra of a giant's spine, standing in the earth: the round body (centrum) sunk in a mound with its ringed joint face
to the front, the arch over the spinal canal rising to a
sloped ridge, the spine leaning back from it at 45 degrees and a thick wing (transverse process) to each side, the
west one snapped short. Round shapes are octagons (two straight and two 45-degree boxes), not 1 px stairs.
Build:  python art/props/build_props.py giant_vertebra      Card:  python tools/prop_card.py giant_vertebra
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401
from giant_bones import bone, disc_face, mound, octagon, relight, MARROW_FACE

CX, CY, R = 8, 12, 12      # centrum: centre (x, y) and inradius; its lowest edge sits on the floor, the mound buries it


def spine(x0, x1, base, length, thick, ang, mat, uy=16):
    """A blade along y, `thick` deep in z, turned `ang` about x so it leans back, its foot centred on `base` (y, z).
    The unrotated box sits at y uy..uy+length (inside the part frame) and the rotation origin is solved so the turned
    box lands with its foot on `base`: (I - R) O = base - R foot."""
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    fy, fz = uy, base[1]                                   # unrotated foot centre
    ry, rz = c * fy - s * fz, s * fy + c * fz              # R . foot
    ty, tz = base[0] - ry, base[1] - rz
    det = (1 - c) ** 2 + s * s
    oy = ((1 - c) * ty - s * tz) / det; oz = (s * ty + (1 - c) * tz) / det
    return E((x0, uy, fz - thick / 2), (x1, uy + length, fz + thick / 2), mat, name="spinous process",
             rot=("x", ang, (8, round(oy, 3), round(oz, 3))))


def build():
    B = bone(moss_h=6, seed=4)
    disc = disc_face(CX, CY, R, moss_h=6, seed=1)
    els = []
    # centrum: an octagonal drum along z, its front (north) face the ringed joint face
    els += octagon((CX, CY), R, "z", 5, 30, B, name="centrum", faces={"north": disc})
    # neural arch over the canal: two pedicles standing on the back half of the centrum, and two laminae sloping up
    # from them to a ridge (a steep roof, not a lid)
    els.append(E((-2, CY + 6, 11), (3, 34, 26), B, name="pedicle"))
    els.append(E((13, CY + 6, 11), (18, 34, 26), B, name="pedicle"))
    els.append(E((-2, 32, 11), (9, 36, 26), B, name="lamina", rot=("z", 22.5, (-2, 32, 18.5))))
    els.append(E((7, 32, 11), (18, 36, 26), B, name="lamina", rot=("z", -22.5, (18, 32, 18.5))))
    # the canal's back wall in shadow, seen through the arch
    els.append(E((3, 20, 24), (13, 34, 25), bone(moss_h=0, seed=4, base=1), skip="up down east west south", name="canal back"))
    # spine (spinous process): one narrow blade rising off the back of the ridge and leaning back (south) at 45 degrees
    els.append(spine(6, 10, (35, 19), 14, 5, 45, B))
    # transverse processes, thick and heavy: the east wing whole and rising, the west one snapped short (a clean break)
    els.append(E((17, 21, 13), (31, 31, 23), B, name="transverse process", rot=("z", 22.5, (17, 21, 18))))
    els.append(E((26, 25, 14), (32, 34, 22), B, name="transverse tip", rot=("z", 22.5, (17, 21, 18))))
    els.append(E((-7, 21, 13), (-1, 31, 23), B, name="broken transverse process", rot=("z", -22.5, (-1, 21, 18)),
                 faces={"west": MARROW_FACE}))
    # the earth it sinks into, and a chip of the broken wing lying in the moss
    els += mound((-16, 0, 0), (32, 5, 32), layers=2, seed=5)
    els.append(E((-14, 0, 3), (-7, 3, 8), bone(moss_h=1, seed=6), skip="down", name="bone chip", rot=("y", -22.5, (-14, 0, 3))))
    return Prop("giant_vertebra", {"giant_vertebra": relight(els)}, shift={"giant_vertebra": (0, 16, 0)})
