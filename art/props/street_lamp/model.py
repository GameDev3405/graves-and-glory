"""Street Lamp (`street_lamp`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/street_lamp/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..48, z 0..16 px. The prop is exactly 3 cells tall, so it stays
one part per layer, each stored shifted down 16 px (stored y -16..32), like the README says for a 3-cell prop.
A stained-timber post on a stone footing with two iron bands; four stepped iron corbels carry a platform; a boxy iron
lantern (four corner posts, a pyramid cap, a ring) holds a glass core. The glass is a glow part (full-bright).
Build:  python art/props/build_props.py street_lamp      Card:  python tools/prop_card.py street_lamp
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401

GLASS = fixed("B8641A", "E0962A", "F6C448", "FFE27A", "FFF4B8")      # lantern glass, warm yellow (full-bright part)


def _glass(face, P, c, r, w, h):
    """Frosted pane: a brighter heart, a darker rim; the top and bottom faces stay mid."""
    if face in ("up", "down"): return GLASS[2]
    d = min(c, w - 1 - c, r, h - 1 - r)
    return GLASS[2] if d == 0 else GLASS[3] if d == 1 else GLASS[4]


def glass_mat():
    m = Mat(GLASS, 2, _glass, edges=False, lit=False); m.ramps = (GLASS,)
    return m


def build():
    b = [
        # stone footing and the timber post, two iron bands
        E((4, 0, 4), (12, 4, 12), "stone", skip="down", name="footing"),
        E((6, 4, 6), (10, 34, 10), "wood_y", name="post"),
        E((5, 10, 5), (11, 12, 11), "iron", name="band low"),
        E((5, 22, 5), (11, 24, 11), "iron", name="band mid"),
        # four stepped corbels under the lantern platform (north, south, east, west)
        E((7, 28, 5), (9, 30, 6), "iron", name="corbel n low"), E((7, 30, 4), (9, 32, 6), "iron", name="corbel n high"),
        E((7, 28, 10), (9, 30, 11), "iron", name="corbel s low"), E((7, 30, 10), (9, 32, 12), "iron", name="corbel s high"),
        E((10, 28, 7), (11, 30, 9), "iron", name="corbel e low"), E((10, 30, 7), (12, 32, 9), "iron", name="corbel e high"),
        E((5, 28, 7), (6, 30, 9), "iron", name="corbel w low"), E((4, 30, 7), (6, 32, 9), "iron", name="corbel w high"),
        # the platform and lantern: base plate, four corner posts, top plate, pyramid cap, ring
        E((3, 32, 3), (13, 34, 13), "iron", name="platform"),
        E((4, 34, 4), (12, 36, 12), "iron", skip="down", name="lantern base"),
        E((4, 36, 4), (6, 44, 6), "iron", name="corner nw"), E((10, 36, 4), (12, 44, 6), "iron", name="corner ne"),
        E((4, 36, 10), (6, 44, 12), "iron", name="corner sw"), E((10, 36, 10), (12, 44, 12), "iron", name="corner se"),
        E((3, 44, 3), (13, 46, 13), "iron", name="cap eaves"),
        E((5, 46, 5), (11, 47, 11), "iron", name="cap crown"),
        E((7, 47, 7), (9, 48, 9), "steel", name="cap tip"),
    ]
    g = [
        E((5, 36, 5), (11, 44, 11), glass_mat(), skip="down up", name="lantern glass"),
    ]
    return Prop("street_lamp", {"street_lamp": b, "street_lamp_glass": g}, glow=("street_lamp_glass",),
                shift={"street_lamp": (0, 16, 0), "street_lamp_glass": (0, 16, 0)})
