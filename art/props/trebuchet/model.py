"""Trebuchet (`trebuchet`): 7x13x18 cells, anchor cell (3,0,8). Brief: art/props/trebuchet/brief.md.

A BIG prop: model every element in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min corner
of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -48..64, y 0..208, z -128..160 px.
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

SIZE = (7, 13, 18)

# hemp rope: a warm straw-brown, twisted (world-aligned diagonal lay lines)
HEMP_R = make_ramp("A48A58", 0.18)
HEMP = Mat(HEMP_R, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0] + P[1] + P[2]) % 3 == 0 else 0, edges=False)
# counterweight stones and the shot: pale field stone, a few chips
FSTONE_R = make_ramp("8A8478", 0.20)
FSTONE = Mat(FSTONE_R, 2, lambda f, P, c, r, w, h: -1 if h3(*P, 23) < 0.10 else 0)

ARM_SLOTS = (90, 91, 92, 93, 94, 95)   # CMD indices past the 90 regions (split block 140..149): the arm and the box
# are moving parts, packed by proplib.pack_group
BOX_SLOTS = (96, 97, 98)
AXLE = (140, -12)                # (y, z) of the arm's pivot; the axle runs along x
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
    """One side's frame on a 12 px sill: the A (two 12 px legs leaning in to the bearing), an X of braces inside the A
    under a tie beam, a raking strut on each end from the sill up to the A, the bearing, the winch post.
    x0 = the sill's west x. Leg centre lines: front z = -60 + 0.414 h, back z = 36 - 0.414 h (h = y - 24)."""
    x1 = x0 + 12
    els = [
        E((x0, 10, -124), (x1, 24, 156), "swood_z", faces={"north": "endgrain", "south": "endgrain"}, name="sill"),
        E((x0 - 1, 9, -122), (x1 + 1, 25, -117), "iron", name="sill end band"),
        E((x0 - 1, 9, 149), (x1 + 1, 25, 154), "iron", name="sill end band"),
        # the A: two legs leaning in to the bearing, iron shoes at the feet
        E((x0, 24, -66), (x1, 150, -54), "wood_y", rot=("x", 22.5, (x0, 24, -60)), name="front leg"),
        E((x0, 24, 30), (x1, 150, 42), "wood_y", rot=("x", -22.5, (x0, 24, 36)), name="back leg"),
        # iron collars where the legs are stored in two regions: the joint sits on the collar's lower edge
        E((x0 - 1, 48, -67), (x1 + 1, 52, -53), "iron", rot=("x", 22.5, (x0, 24, -60)), name="leg collar"),
        E((x0 - 1, 48, 29), (x1 + 1, 52, 43), "iron", rot=("x", -22.5, (x0, 24, 36)), name="leg collar"),
        E((x0 - 1, 96, -67), (x1 + 1, 100, -53), "iron", rot=("x", 22.5, (x0, 24, -60)), name="leg collar"),
        E((x0 - 1, 96, 29), (x1 + 1, 100, 43), "iron", rot=("x", -22.5, (x0, 24, 36)), name="leg collar"),
        E((x0 - 1, 24, -68), (x1 + 1, 30, -52), "iron", name="leg shoe"),
        E((x0 - 1, 24, 28), (x1 + 1, 30, 44), "iron", name="leg shoe"),
        # the X inside the A: each brace runs from one leg's foot to the other leg at the tie beam (the back brace is
        # 1 px thinner each side so the two never share a face where they cross)
        E((x0 + 2, 24, -64), (x1 - 2, 120, -56), "wood_y", rot=("x", 45, (x0 + 2, 24, -60)), name="cross brace"),
        E((x0 + 3, 24, 32), (x1 - 3, 120, 40), "wood_y", rot=("x", -45, (x0 + 3, 24, 36)), name="cross brace"),
        E((x0 + 1, 86, -38), (x1 - 1, 94, 14), "wood_z", name="tie beam"),
        # raking struts: the rear one from the sill to the bearing, the front one to the front leg
        E((x0 + 2, 24, -120), (x1 - 2, 160, -112), "wood_y", rot=("x", 45, (x0 + 2, 24, -116)), name="front raker"),
        E((x0 + 2, 24, 100), (x1 - 2, 188, 108), "wood_y", rot=("x", -45, (x0 + 2, 24, 104)), name="rear raker"),
        E((x0 - 1, 24, -122), (x1 + 1, 29, -110), "iron", name="raker shoe"),
        E((x0 - 1, 24, 98), (x1 + 1, 29, 110), "iron", name="raker shoe"),
        # the bearing block the axle turns in, strapped with iron
        E((x0, 130, -22), (x1, 152, -2), "swood_y", faces={"up": "endgrain"}, name="bearing"),
        E((x0 - 1, 146, -23), (x1 + 1, 150, -1), "iron", name="bearing strap"),
        E((x0 - 1, 132, -23), (x1 + 1, 135, -1), "iron", name="bearing strap"),
        # winch post at the back
        E((x0, 24, 110), (x1, 62, 122), "swood_y", faces={"up": "endgrain"}, name="winch post"),
        E((x0 - 1, 34, 109), (x1 + 1, 38, 123), "iron", name="winch post strap"),
    ]
    return els


def handspikes(xa, xb):
    """The winch's handspike star on one end of the drum axle: four bars through an iron boss."""
    c = (50, 116)
    org = ((xa + xb) // 2, c[0], c[1])
    els = [
        E((xa, 32, 114), (xb, 68, 118), "wood_y", name="handspike"),
        E((xa, 48, 98), (xb, 52, 134), "wood_z", name="handspike"),
        E((xa, 34, 114), (xb, 66, 118), "wood_y", rot=("x", 45, org), name="handspike"),
        E((xa, 34, 114), (xb, 66, 118), "wood_y", rot=("x", -45, org), name="handspike"),
        E((xa - 1, 46, 112), (xb + 1, 54, 120), "iron", name="winch boss"),
    ]
    return els


def build():
    els = []
    # ---- the bed: two sills on four wheels, cross beams, a plank deck with the sling trough --------------------
    els += side(-40) + side(44)
    els += [
        E((-28, 12, -124), (44, 16, 156), "plank_z", name="deck"),
        E((-28, 16, -124), (44, 24, -114), "wood_x", name="front cross beam"),
        E((-28, 16, 146), (44, 24, 156), "wood_x", name="rear cross beam"),
        E((-28, 16, -66), (44, 23, -54), "wood_x", name="cross beam"),
        E((-28, 16, 30), (44, 23, 42), "wood_x", name="cross beam"),
        E((-2, 16, -54), (1, 20, 30), "swood_z", name="trough rail"),
        E((15, 16, -54), (18, 20, 30), "swood_z", name="trough rail"),
        # cross ties between the A-frames, low enough to clear the arm and the hanging box
        E((-28, 52, -53), (44, 60, -41), "wood_x", name="front cross tie"),
        E((-28, 52, 17), (44, 60, 29), "wood_x", name="back cross tie"),
        E((-28, 98, -2), (44, 106, 10), "wood_x", name="upper back tie"),
    ]
    for zc in (-104, 136):
        for xc in (-44, 60):
            els += wheel((xc, 17, zc), 17, 6, "x", "wood_x", spokes=8, rim=4, hub=5, hub_mat="iron", name="wheel")
        els.append(E((-41, 14, zc - 3), (57, 20, zc + 3), "iron", name="wheel axle"))

    # ---- axle across the bearings ------------------------------------------------------------------------------
    els += [
        E((-42, 136, -16), (58, 144, -8), "iron", name="axle"),
        E((-44, 134, -18), (-42, 146, -6), "iron", name="axle cap"),
        E((58, 134, -18), (60, 146, -6), "iron", name="axle cap"),
    ]

    # ---- the throwing arm, upright here, turned as one about the axle (a px along the arm = y 140 - a) --------
    # One dark beam tapering from the hub to the tip; the taper steps fall on the region joints (y 48, 96).
    # It sits inside the middle x region (x -16..32), so nothing along it is cut lengthwise.
    arm = [
        E((1, 96, -19), (15, 202, -5), "swood_y", faces={"up": "endgrain"}, name="arm"),
        E((2, 48, -18), (14, 96, -6), "swood_y", name="arm"),
        E((3, 8, -17), (13, 48, -7), "swood_y", faces={"down": "endgrain"}, name="arm tip"),
        E((0, 128, -20), (1, 152, -4), "iron", name="hub plate"),
        E((15, 128, -20), (16, 152, -4), "iron", name="hub plate"),
        E((0, 172, -20), (16, 176, -4), "iron", name="arm band"),
        E((1, 66, -19), (15, 70, -5), "iron", name="arm band"),
        E((2, 10, -18), (14, 14, -6), "iron", name="tip band"),
        E((6, 2, -14), (10, 8, -10), "iron", name="sling hook"),
        E((-14, 192, -15), (30, 198, -9), "iron", name="weight pin"),
    ]
    # The arm is a moving part (docs/props/MOVING-PARTS.md): an idle creak about the axle. The pull rope and the sling's
    # rising cord hang on its hook, so they turn with it (the rope's drum end runs on into the wound rope, hidden there).
    moving_arm = [E(e.frm, e.to, e.mat, skip=" ".join(sorted(e.skip)), rot=ARM, name=e.name, faces=e.faces) for e in arm]

    # ---- weight box hanging from the pin (pin centre about y 179, z -51): plank walls, ballast on top ----------
    # It hangs free: it follows the arm and swings a little on the pin (its own moving part).
    box = [
        E((-12, 92, -75), (28, 136, -71), "plank_x", name="box wall"),
        E((-12, 92, -31), (28, 136, -27), "plank_x", name="box wall"),
        E((-12, 92, -71), (-8, 136, -31), "plank_z", name="box wall"),
        E((24, 92, -71), (28, 136, -31), "plank_z", name="box wall"),
        E((-8, 92, -71), (24, 96, -31), "plank_x", skip="up", name="box floor"),
        E((-8, 131, -71), (24, 132, -31), FSTONE, skip="down", name="ballast"),
        E((-14, 136, -77), (30, 140, -71), "swood_x", name="box rim"),
        E((-14, 136, -31), (30, 140, -25), "swood_x", name="box rim"),
        E((-14, 136, -71), (-8, 140, -31), "swood_z", name="box rim"),
        E((24, 136, -71), (30, 140, -31), "swood_z", name="box rim"),
        E((-14, 98, -77), (30, 102, -25), "iron", name="box band"),
        E((-14, 120, -77), (30, 124, -25), "iron", name="box band"),
        E((-6, 132, -53), (-2, 181, -49), "iron", name="hanger"),
        E((18, 132, -53), (22, 181, -49), "iron", name="hanger"),
        # hemp lashings down the side walls, seen from the side
        E((-15, 96, -63), (-12, 140, -60), HEMP, name="box lashing"), E((-15, 96, -42), (-12, 140, -39), HEMP, name="box lashing"),
        E((28, 96, -63), (31, 140, -60), HEMP, name="box lashing"), E((28, 96, -42), (31, 140, -39), HEMP, name="box lashing"),
        # ballast stones heaped in the box, standing proud of the rim
        E((-7, 132, -70), (4, 145, -60), FSTONE, skip="down", name="ballast stone"),
        E((5, 132, -69), (14, 144, -58), FSTONE, skip="down", name="ballast stone"),
        E((15, 132, -62), (23, 146, -52), FSTONE, skip="down", name="ballast stone"),
        E((-7, 132, -56), (5, 138, -46), FSTONE, skip="down", name="ballast stone"),
        E((7, 132, -47), (17, 145, -37), FSTONE, skip="down", name="ballast stone"),
        E((17, 132, -45), (23, 137, -33), FSTONE, skip="down", name="ballast stone"),
        E((-7, 132, -42), (4, 143, -32), FSTONE, skip="down", name="ballast stone"),
    ]

    # ---- winch at the back: drum on the posts, hemp wound on it, handspikes on both ends -----------------------
    els += [
        E((-28, 43, 111), (44, 57, 121), "wood_x", name="winch drum"),
        E((-28, 45, 109), (44, 55, 123), "wood_x", name="winch drum"),
        E((0, 42, 110), (16, 58, 122), HEMP, name="wound rope"),
        E((0, 44, 108), (16, 56, 124), HEMP, name="wound rope"),
        E((-47, 48, 114), (63, 52, 118), "iron", name="winch axle"),
    ]
    els += handspikes(-46, -42) + handspikes(58, 62)
    # the pull rope from the arm's hook to the drum
    moving_arm.append(E((6, 45, 85), (9, 48, 112), HEMP, name="pull rope"))

    # ---- sling: from the hook down to the trough, forward to the pouch with its stone --------------------------
    moving_arm.append(E((6, 16, 71), (10, 47, 74), HEMP, rot=("x", 22.5, (6, 16, 71)), name="sling cord"))
    els.append(E((6, 16, -34), (10, 18, 74), HEMP, name="sling cord"))
    els.append(E((2, 16, -52), (14, 21, -34), "leather", name="sling pouch"))
    els += [E((4, 20, -49), (12, 30, -37), FSTONE, name="shot"), E((3, 21, -48), (13, 29, -38), FSTONE, name="shot")]

    els = cut(els)
    return Prop.split("trebuchet", els, SIZE, moving=[("arm", moving_arm, ARM_SLOTS), ("box", box, BOX_SLOTS)])
