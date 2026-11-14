"""Trebuchet (`trebuchet`): 5x9x13 cells, anchor cell (2,0,6). Brief: art/props/trebuchet/brief.md.

A BIG prop: model every element in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min corner
of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -32..48, y 0..144, z -96..112 px.
Prop.split cuts the elements into 3-cell regions (parts trebuchet_<n>); after changing which regions hold elements run
python tools/new_prop.py trebuchet --sync  so the def lists the same parts. Shape helpers: wheel, cylinder, blob, curve.
Build:  python art/props/build_props.py trebuchet      Card:  python tools/prop_card.py trebuchet

A counterweight trebuchet on a wheeled bed, cocked to throw north: the arm is wound down to the winch at the back, the
weight box hangs high at the front, the sling lies forward along the trough with a stone in the pouch.

Long rotated timbers (A-frame legs, the arm) are cut here at the region boundaries across their length (`cut`): every
piece keeps the same rotation and origin, so they render as one beam, and each piece sits inside one region as the
splitter needs.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, wheel, cylinder, blob, curve   # noqa: F401
from proplib import part_grid, FNEG, FPOS, pat_grain

SIZE = (5, 9, 13)

# hemp rope: a warm straw-brown, twisted (world-aligned diagonal lay lines)
HEMP_R = make_ramp("A48A58", 0.18)
HEMP = Mat(HEMP_R, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0] + P[1] + P[2]) % 3 == 0 else 0, edges=False)
# counterweight stones and the shot: pale field stone, a few chips
FSTONE_R = make_ramp("8A8478", 0.20)
FSTONE = Mat(FSTONE_R, 2, lambda f, P, c, r, w, h: -1 if h3(*P, 23) < 0.10 else 0)

AXLE = (100, -12)                # (y, z) of the arm's pivot; the axle runs along x
# The arm is modelled standing upright (long end down, short end up) and turned -45 degrees about the axle: cocked,
# the long end wound down to the winch at the back, the short end and the weight box raised at the front. Upright, the
# unrotated boxes spread over a column of cells instead of piling into the cells by the axle.
ARM = ("x", -45, (8, AXLE[0], AXLE[1]))


def cut(els):
    """Cut rotated elements at the region boundaries on the axes other than their rotation axis (same rotation, same
    origin, so the pieces still render as one beam); unrotated elements are left to the splitter."""
    start, n = part_grid(SIZE)
    out = []
    for e in els:
        if not e.rot: out.append(e); continue
        ax = "xyz".index(e.rot[0]); pieces = [e]
        for i in range(3):
            if i == ax: continue
            bounds = [start[i] + 48 * k for k in range(1, n[i])]
            nxt = []
            for p in pieces:
                cs = [b for b in bounds if p.frm[i] < b < p.to[i]]
                if not cs: nxt.append(p); continue
                edges = [p.frm[i]] + cs + [p.to[i]]
                for a, b in zip(edges, edges[1:]):
                    f, t = list(p.frm), list(p.to); f[i], t[i] = a, b
                    sk = set(p.skip)
                    if a > p.frm[i]: sk.add(FNEG[i])
                    if b < p.to[i]: sk.add(FPOS[i])
                    nxt.append(E(f, t, p.mat, skip=" ".join(sorted(sk)), rot=p.rot, shade=p.shade, name=p.name,
                                 faces=p.faces, whole=p.whole or (list(e.frm), list(e.to))))
            pieces = nxt
        out += pieces
    return out


def side(x0):
    """One side's frame: sill, A-frame legs, tie beam, bearing at the axle. x0 = the sill's west x (8 px wide)."""
    x1 = x0 + 8
    els = [
        E((x0, 8, -94), (x1, 18, 110), "swood_z", faces={"north": "endgrain", "south": "endgrain"}, name="sill"),
        E((x0 - 1, 7, -93), (x1 + 1, 19, -89), "iron", name="sill end band"),
        E((x0 - 1, 7, 105), (x1 + 1, 19, 109), "iron", name="sill end band"),
        # the A: two legs leaning in to the bearing, iron shoes at the feet
        E((x0, 18, -50), (x1, 104, -42), "wood_y", rot=("x", 22.5, (x0, 18, -46)), name="front leg"),
        E((x0, 18, 18), (x1, 104, 26), "wood_y", rot=("x", -22.5, (x0, 18, 22)), name="back leg"),
        # iron collars where the legs are stored in two regions: the joint sits on the collar's lower edge
        E((x0 - 1, 48, -51), (x1 + 1, 51, -41), "iron", rot=("x", 22.5, (x0, 18, -46)), name="leg collar"),
        E((x0 - 1, 48, 17), (x1 + 1, 51, 27), "iron", rot=("x", -22.5, (x0, 18, 22)), name="leg collar"),
        E((x0 - 1, 96, -51), (x1 + 1, 99, -41), "iron", rot=("x", 22.5, (x0, 18, -46)), name="leg collar"),
        E((x0 - 1, 96, 17), (x1 + 1, 99, 27), "iron", rot=("x", -22.5, (x0, 18, 22)), name="leg collar"),
        E((x0 - 1, 18, -53), (x1 + 1, 22, -40), "iron", name="leg shoe"),
        E((x0 - 1, 18, 16), (x1 + 1, 22, 29), "iron", name="leg shoe"),
        E((x0 + 1, 56, -34), (x1 - 1, 62, 10), "wood_z", name="tie beam"),
        # the bearing block the axle turns in, strapped with iron
        E((x0, 94, -19), (x1, 107, -5), "swood_y", faces={"up": "endgrain"}, name="bearing"),
        E((x0 - 1, 103, -20), (x1 + 1, 106, -4), "iron", name="bearing strap"),
        # winch post at the back
        E((x0, 18, 76), (x1, 44, 84), "swood_y", faces={"up": "endgrain"}, name="winch post"),
        E((x0 - 1, 26, 75), (x1 + 1, 29, 85), "iron", name="winch post strap"),
    ]
    return els


def handspikes(xa, xb):
    """The winch's handspike star on one end of the drum axle: four bars through an iron boss."""
    c = (36, 80)
    org = ((xa + xb) // 2, c[0], c[1])
    els = [
        E((xa, 24, 78), (xb, 48, 82), "wood_y", name="handspike"),
        E((xa, 34, 68), (xb, 38, 92), "wood_z", name="handspike"),
        E((xa, 26, 78), (xb, 46, 82), "wood_y", rot=("x", 45, org), name="handspike"),
        E((xa, 26, 78), (xb, 46, 82), "wood_y", rot=("x", -45, org), name="handspike"),
        E((xa - 1, 33, 77), (xb + 1, 39, 83), "iron", name="winch boss"),
    ]
    return els


def build():
    els = []
    # ---- the bed: two sills on four wheels, cross beams, a plank deck with the sling trough --------------------
    els += side(-24) + side(32)
    els += [
        E((-16, 8, -94), (32, 11, 110), "plank_z", name="deck"),
        E((-16, 11, -94), (32, 17, -86), "wood_x", faces={"north": "wood_x"}, name="front cross beam"),
        E((-16, 11, 102), (32, 17, 110), "wood_x", name="rear cross beam"),
        E((-16, 11, -50), (32, 16, -42), "wood_x", name="cross beam"),
        E((-16, 11, 18), (32, 16, 26), "wood_x", name="cross beam"),
        E((0, 11, -42), (2, 14, 18), "swood_z", name="trough rail"),
        E((14, 11, -42), (16, 14, 18), "swood_z", name="trough rail"),
        # cross ties between the A-frames, low enough to clear the arm
        E((-16, 36, -42), (32, 42, -34), "wood_x", name="front cross tie"),
        E((-16, 36, 10), (32, 42, 18), "wood_x", name="back cross tie"),
    ]
    for zc in (-78, 96):
        for xc in (-28, 44):
            els += wheel((xc, 12, zc), 12, 4, "x", "wood_x", spokes=4, rim=3, hub=3, hub_mat="iron", name="wheel")
        els.append(E((-27, 10, zc - 2), (-24, 14, zc + 2), "iron", name="axle stub"))
        els.append(E((40, 10, zc - 2), (43, 14, zc + 2), "iron", name="axle stub"))

    # ---- axle across the bearings ------------------------------------------------------------------------------
    els += [
        E((-26, 97, -15), (42, 103, -9), "iron", name="axle"),
        E((-28, 96, -16), (-26, 104, -8), "iron", name="axle cap"),
        E((42, 96, -16), (44, 104, -8), "iron", name="axle cap"),
    ]

    # ---- the throwing arm, upright here, turned as one about the axle (a px along the arm = y 100 - a) --------
    # One dark beam tapering from the hub to the tip, stored in three regions on y; the taper steps fall exactly on
    # the region joints (y 48, 96) so no flat joint shows. It sits wholly east of the x region boundary (x 8): its
    # centre is x 13, 5 px east of the frame's centre, so nothing along it is cut lengthwise.
    arm = [
        E((8, 96, -17), (18, 144, -7), "swood_y", faces={"up": "endgrain"}, name="arm"),
        E((9, 48, -16), (17, 96, -8), "swood_y", name="arm"),
        E((10, 6, -15), (16, 48, -9), "swood_y", faces={"down": "endgrain"}, name="arm tip"),
        E((7, 92, -18), (8, 108, -6), "iron", name="hub plate"),
        E((18, 92, -18), (19, 108, -6), "iron", name="hub plate"),
        E((9, 8, -16), (17, 11, -8), "iron", name="tip band"),
        E((12, 2, -13), (14, 6, -11), "iron", name="sling hook"),
        E((-1, 138, -14), (27, 142, -10), "iron", name="weight pin"),
    ]
    els += [E(e.frm, e.to, e.mat, skip=" ".join(sorted(e.skip)), rot=ARM, name=e.name, faces=e.faces) for e in arm]

    # ---- weight box hanging from the pin (pin centre about y 128.3, z -40.3): plank walls, ballast on top --------
    els += [
        E((-1, 64, -51), (27, 94, -48), "plank_x", name="box wall"),
        E((-1, 64, -32), (27, 94, -29), "plank_x", name="box wall"),
        E((-1, 64, -48), (2, 94, -32), "plank_z", name="box wall"),
        E((24, 64, -48), (27, 94, -32), "plank_z", name="box wall"),
        E((2, 64, -48), (24, 67, -32), "plank_x", skip="up", name="box floor"),
        E((2, 91, -48), (24, 92, -32), FSTONE, skip="down", name="ballast"),
        E((-2, 94, -52), (28, 97, -49), "swood_x", name="box rim"),
        E((-2, 94, -31), (28, 97, -28), "swood_x", name="box rim"),
        E((-2, 94, -49), (2, 97, -31), "swood_z", name="box rim"),
        E((24, 94, -49), (28, 97, -31), "swood_z", name="box rim"),
        E((-2, 68, -52), (28, 71, -28), "iron", name="box band"),
        E((-2, 84, -52), (28, 87, -28), "iron", name="box band"),
        E((2, 92, -42), (5, 129, -38), "iron", name="hanger"),
        E((21, 92, -42), (24, 129, -38), "iron", name="hanger"),
        # ballast stones heaped in the box
        E((3, 92, -47), (10, 98, -40), FSTONE, skip="down", name="ballast stone"),
        E((11, 92, -46), (17, 97, -38), FSTONE, skip="down", name="ballast stone"),
        E((16, 92, -39), (23, 99, -33), FSTONE, skip="down", name="ballast stone"),
        E((4, 92, -37), (12, 96, -33), FSTONE, skip="down", name="ballast stone"),
    ]

    # ---- winch at the back: drum on the posts, hemp wound on it, handspikes on both ends -----------------------
    els += [
        E((-16, 31, 77), (32, 41, 83), "wood_x", name="winch drum"),
        E((-16, 33, 75), (32, 39, 85), "wood_x", name="winch drum"),
        E((8, 30, 76), (22, 42, 84), HEMP, name="wound rope"),
        E((8, 32, 74), (22, 40, 86), HEMP, name="wound rope"),
        E((-30, 34, 78), (46, 38, 82), "iron", name="winch axle"),
    ]
    els += handspikes(-31, -28) + handspikes(44, 47)
    # the pull rope from the arm's hook to the drum
    els.append(E((12, 29, 58), (14, 31, 75), HEMP, rot=("x", -22.5, (12, 30, 58)), name="pull rope"))

    # ---- sling: from the hook down to the trough, forward to the pouch with its stone --------------------------
    els.append(E((11, 11, 50), (15, 32, 53), HEMP, rot=("x", 22.5, (11, 11, 50)), name="sling cord"))
    els.append(E((11, 11, -24), (15, 13, 53), HEMP, name="sling cord"))
    els.append(E((8, 11, -38), (18, 15, -24), "leather", name="sling pouch"))
    els += [E((10, 14, -35), (16, 21, -27), FSTONE, name="shot"), E((9, 15, -34), (17, 20, -28), FSTONE, name="shot")]

    els = cut(els)
    return Prop.split("trebuchet", els, SIZE)
