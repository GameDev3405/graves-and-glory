"""Commander's Pavilion (`tent_pavilion`): 9x8x9 cells, anchor cell (4,0,4). Brief: art/props/tent_pavilion/brief.md.

A BIG prop: model every element in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min corner
of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -64..80, y 0..128, z -64..80 px.
Prop.split cuts the elements into 3-cell regions (parts tent_pavilion_<n>); after changing which regions hold elements run
python tools/new_prop.py tent_pavilion --sync  so the def lists the same parts.
Build:  python art/props/build_props.py tent_pavilion      Card:  python tools/prop_card.py tent_pavilion

An octagonal pavilion centred on the anchor cell's middle (8, 8): striped canvas walls (apothem 44), a stepped concave
roof of octagonal rings (eave apothem 52 at y 40, crown near y 100), a scalloped valance, a centre pole with a gilt
finial and a pennant, an open door in the north wall with both flaps tied back, and eight guy ropes to pegs.

Octagon faces: the four cardinal faces are plain boxes; the four diagonal faces are boxes turned 45 degrees about y.
A turned box must sit inside one 3-cell region on x and z (the splitter cuts across y only), so `face_box` stores each
diagonal box at the nearest legal spot and picks the rotation origin that carries it onto the octagon (origins may lie
anywhere; Minecraft only bounds from/to). The canvas painter maps every texel back to the world point, so the stripes
(24 around, by angle from the pole) line up across walls, valance and roof.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, LINEN, GILT, WOOD, SWOOD, IRON   # noqa: F401

SIZE = (9, 8, 9)
CX = CZ = 8                     # centre of the pavilion (middle of the anchor cell)
R2 = math.sqrt(0.5)
TAN = math.tan(math.radians(22.5))
REGIONS = (-64, -16, 32, 80)    # x and z region bounds of the split grid (Prop.split, 3 cells a part)

CREAM = make_ramp("E0D2AC", 0.16)   # undyed canvas
RED = make_ramp("8C2A25", 0.20)     # madder-red canvas
ROPE_R = make_ramp("A8977A", 0.16)  # hemp

# outward normal (x, z) of each octagon face; the face tangent is the normal turned 90 degrees
NORM = {"N": (0, -1), "NE": (R2, -R2), "E": (1, 0), "SE": (R2, R2), "S": (0, 1), "SW": (-R2, R2), "W": (-1, 0), "NW": (-R2, -R2)}
TURN = {"NE": -45, "SW": -45, "NW": 45, "SE": 45}
FN = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def roty(ang):
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    return ((c, s), (-s, c))    # (x, z) -> (c x + s z, -s x + c z): tools/prop_sheet.rot_matrix("y")


def stripe(x, z):
    """0 = red, 1 = cream: 24 stripes round the pole, a red one centred on each cardinal face."""
    ang = math.degrees(math.atan2(-(z - CZ), x - CX))
    return int(math.floor((ang + 7.5) / 15.0)) % 2


class Canvas:
    """Striped canvas painter. `rot` is the element's turn about y (so texels map to world points and normals);
    kind: wall (lit top row, dark hem), roof (flat steps, red crown), valance (stripes, dark trim rows), flap (red)."""
    ramps = (CREAM, RED)

    def __init__(s, kind, rot=None, crown=0.0):
        s.kind, s.rot, s.crown = kind, rot, crown

    def world(s, P, n):
        if not s.rot: return P, n
        _, ang, O = s.rot; (a, b), (c, d) = roty(ang)
        dx, dz = P[0] - O[0], P[2] - O[2]
        return (O[0] + a * dx + b * dz, P[1], O[2] + c * dx + d * dz), (a * n[0] + b * n[2], n[1], c * n[0] + d * n[2])

    def __call__(s, f, P, c, r, w, h):
        (x, y, z), n = s.world(P, FN[f])
        if f == "up":
            # a tread takes its riser's tone (the facet's side of the light): the game lights tops itself, and a
            # lighter tread over every 4 px riser bands the roof into rings
            t = 2 if (x - CX) * R2 - (z - CZ) * R2 > -0.2 * math.hypot(x - CX, z - CZ) or s.kind == "wall" else 1
        elif f == "down": t = 0
        else:
            lit = n[0] * R2 - n[2] * R2 > -0.2          # facing the top-front-left (north-east) light
            t = 2 if lit else 1
            if n[0] * (x - CX) + n[2] * (z - CZ) < -0.3: t -= 1   # inside faces, out of the sun: the doorway reads dark
            if s.kind == "wall" and h >= 6:
                if r == 0 and lit: t += 1
                if r >= h - 2: t -= 1                    # mud-splashed hem
            if s.kind == "valance" and h >= 6 and (r == 0 or r == h - 1): t -= 1   # sewn trim rows on the band
        col = stripe(x, z)
        if s.kind == "flap": col = 0
        if s.kind == "roof" and math.hypot(x - CX, z - CZ) < s.crown: col = 0
        ramp = RED if col == 0 else CREAM
        return ramp[max(0, min(4, t))]


def _place(lo, n):
    """Lowest start for a box of length n near `lo` that stays inside one split region."""
    best = None
    for a, b in zip(REGIONS, REGIONS[1:]):
        if n > b - a: continue
        s = min(max(round(lo), a), b - n)
        if best is None or abs(s - lo) < abs(best - lo): best = s
    assert best is not None, "box %d px too long for a region" % n
    return best


def face_box(face, a, t, y0, y1, u0, u1, kind="wall", name=None, crown=0.0, mat=None, skip=""):
    """A box on octagon face `face`: outer side at apothem `a`, `t` px thick inward, tangent span u0..u1 px from the
    face centre, y0..y1. Cardinal faces are plain boxes; diagonal faces are turned 45 degrees (see module doc)."""
    nx, nz = NORM[face]
    if face in ("N", "E", "S", "W"):
        tx, tz = -nz, nx
        xs = sorted((CX + tx * u0, CX + tx * u1)) if tx else None
        zs = sorted((CZ + tz * u0, CZ + tz * u1)) if tz else None
        if nx: xs = sorted((CX + nx * a, CX + nx * (a - t)))
        if nz: zs = sorted((CZ + nz * a, CZ + nz * (a - t)))
        return E((xs[0], y0, zs[0]), (xs[1], y1, zs[1]), mat or Canvas(kind, crown=crown), name=name, skip=skip)
    ang = TURN[face]; tx, tz = -nz, nx
    um, d = (u0 + u1) / 2.0, a - t / 2.0
    C = (CX + nx * d + tx * um, CZ + nz * d + tz * um)     # world centre of the face box (x, z)
    L = u1 - u0
    bx = _place(C[0] - L / 2.0, L); bz = _place(C[1] - t / 2.0, t)
    B = (bx + L / 2.0, bz + t / 2.0)                        # stored (unturned) centre
    (p, q), (r_, s_) = roty(ang)
    # world = O + R (B - O)  ->  (I - R) O = C - R B
    rb = (p * B[0] + q * B[1], r_ * B[0] + s_ * B[1])
    m = ((1 - p, -q), (-r_, 1 - s_)); rhs = (C[0] - rb[0], C[1] - rb[1])
    det = m[0][0] * m[1][1] - m[0][1] * m[1][0]
    ox = (rhs[0] * m[1][1] - m[0][1] * rhs[1]) / det; oz = (m[0][0] * rhs[1] - m[1][0] * rhs[0]) / det
    rot = ("y", ang, (round(ox, 4), y0, round(oz, 4)))
    return E((bx, y0, bz), (bx + L, y1, bz + t), mat or Canvas(kind, rot, crown), rot=rot, name=name, skip=skip)


def half(a):
    return int(round(TAN * a))


def ring(a, t, y0, y1, kind, name, crown=0.0, skip=""):
    h = half(a); out = []
    for f in NORM:
        ext = 0 if len(f) == 1 else 1                       # diagonal boxes run 1 px past each corner
        out.append(face_box(f, a, t, y0, y1, -h - ext, h + ext, kind, name, crown, skip=skip))
    return out


# roof: octagonal rings, 2 px a step (4 px steps ragged the stripes at every step), a concave pavilion curve from the eave (apothem 52) to the crown
ROOF_Y0, STEP, ROOF_N, EAVE = 40, 2, 30, 52
ROOF_A = [int(round(EAVE * (1 - i / 32.0) ** 1.45)) for i in range(ROOF_N)]
WALL_A, WALL_H, WALL_T = 44, 40, 3
VAL_A = 54
DOOR = 12            # the door runs u -12..12 on the north face: x -4..20, the whole of cell x=4
CROWN = 13.0         # roof canvas inside this radius is the red crown


def build():
    els = []
    # ---- walls: canvas on seven faces and the door face's two jambs and lintel ----
    h = half(WALL_A)
    for f in NORM:
        ext = 0 if len(f) == 1 else 1
        if f == "N":
            els.append(face_box(f, WALL_A, WALL_T, 0, WALL_H, -h, -DOOR, "wall", "wall", skip="down"))
            els.append(face_box(f, WALL_A, WALL_T, 0, WALL_H, DOOR, h, "wall", "wall", skip="down"))
            els.append(face_box(f, WALL_A, WALL_T, 34, WALL_H, -DOOR, DOOR, "wall", "lintel"))
        else:
            els.append(face_box(f, WALL_A, WALL_T, 0, WALL_H, -h - ext, h + ext, "wall", "wall", skip="down"))
    # door posts either side of the opening
    zw = CZ - WALL_A
    for x0 in (CX - DOOR - 4, CX + DOOR):
        els.append(E((x0, 0, zw - 1), (x0 + 4, 37, zw + 3), "wood_y", skip="down", name="door post"))
    # the two door flaps, folded back against the wall and tied (turned 45 degrees about the door edge)
    for hx, ang, x0, x1 in ((CX + DOOR, 45, CX + DOOR, CX + DOOR + 11), (CX - DOOR, -45, CX - DOOR - 11, CX - DOOR)):
        rot = ("y", ang, (hx, 0, zw))
        els.append(E((x0, 3, zw - 3), (x1, 33, zw - 1), Canvas("flap", rot), rot=rot, name="door flap"))
        tx0 = x0 + 4 if ang > 0 else x1 - 7
        els.append(E((tx0, 16, zw - 4), (tx0 + 3, 19, zw), Mat(ROPE_R, 2), rot=rot, name="flap tie"))

    # ---- roof ----
    for i, a in enumerate(ROOF_A):
        y0 = ROOF_Y0 + STEP * i
        nxt = ROOF_A[i + 1] if i + 1 < ROOF_N else 0
        if a <= 8:          # the crown cap: one square box
            c = max(2, int(round(a * 0.9)))
            els.append(E((CX - c, y0, CZ - c), (CX + c, y0 + STEP, CZ + c), Canvas("roof", crown=99), name="crown"))
            continue
        t = a - nxt + 1
        if i == 0: t = max(t, a - (WALL_A - 2))       # the eave ring closes over the wall top
        els += ring(a, t, y0, y0 + STEP, "roof", "roof", CROWN)
    top = ROOF_Y0 + STEP * ROOF_N
    # ---- valance: a striped band round the eave with three dagged tabs a face ----
    els += ring(VAL_A, 2, 34, 45, "valance", "valance")
    for f in NORM:
        for u in (-15, 0, 15):
            els.append(face_box(f, VAL_A, 2, 30, 34, u - 5, u + 5, "valance", "valance tab"))
            els.append(face_box(f, VAL_A, 2, 27, 30, u - 2, u + 2, "valance", "valance tab"))

    # ---- centre pole, gilt collar and finial, pennant ----
    els.append(E((CX - 3, 0, CZ - 3), (CX + 3, 114, CZ + 3), "wood_y", skip="down", name="centre pole"))
    els.append(E((CX - 4, top, CZ - 4), (CX + 4, top + 2, CZ + 4), "gilt", name="gilt collar"))
    els.append(E((CX - 3, 114, CZ - 3), (CX + 3, 119, CZ + 3), "gilt", name="finial"))
    els.append(E((CX - 2, 119, CZ - 2), (CX + 2, 122, CZ + 2), "gilt", name="finial tip"))
    flag = []   # the pennant: its own moving part, flutters about the pole (docs/props/MOVING-PARTS.md)
    pen = Mat(RED, 2, lambda f, P, c, r, w, h: 2 if 3 <= P[1] - 104 < 5 and f in ("north", "south") else 0)
    flag.append(E((CX + 3, 102, CZ - 1), (CX + 17, 112, CZ + 1), pen, name="pennant"))
    flag.append(E((CX + 17, 102, CZ - 1), (CX + 23, 105, CZ + 1), pen, name="pennant tail"))
    flag.append(E((CX + 17, 109, CZ - 1), (CX + 23, 112, CZ + 1), pen, name="pennant tail"))

    # ---- guy ropes (22.5 degrees off the vertical) to pegs, two a face on the four cardinal faces ----
    rope = Mat(ROPE_R, 2, edges=False)
    L, foot = 37, 69
    for u in (-14, 14):
        # east and west: turned about z; north and south: turned about x. Origin on the foot's edge toward the tent.
        xe = CX + foot; zc = CZ + u
        els.append(E((xe, 0, zc - 1), (xe + 2, L, zc + 1), rope, rot=("z", 22.5, (xe, 0, zc)), name="guy rope"))
        els.append(E((xe, 0, zc - 2), (xe + 3, 5, zc + 1), "swood_y", skip="down", name="peg"))
        xw = CX - foot
        els.append(E((xw - 2, 0, zc - 1), (xw, L, zc + 1), rope, rot=("z", -22.5, (xw, 0, zc)), name="guy rope"))
        els.append(E((xw - 3, 0, zc - 1), (xw, 5, zc + 2), "swood_y", skip="down", name="peg"))
        xc = CX + u + (0 if u > 0 else 0)
        zs = CZ + foot
        els.append(E((xc - 1, 0, zs), (xc + 1, L, zs + 2), rope, rot=("x", -22.5, (xc, 0, zs)), name="guy rope"))
        els.append(E((xc - 2, 0, zs), (xc + 1, 5, zs + 3), "swood_y", skip="down", name="peg"))
        xn = CX + (u + (2 if u > 0 else -2))          # front ropes stand a little wider, clear of the door flaps
        zn = CZ - foot
        els.append(E((xn - 1, 0, zn - 2), (xn + 1, L, zn), rope, rot=("x", 22.5, (xn, 0, zn)), name="guy rope"))
        els.append(E((xn - 1, 0, zn - 3), (xn + 2, 5, zn), "swood_y", skip="down", name="peg"))

    # ---- inside: a red rug with a gilt border round the pole, the commander's chest at the back ----
    def rug(f, P, c, r, w, h):
        x, y, z = P
        e = min(x - (CX - 22), (CX + 22) - x, z - (CZ - 22), (CZ + 22) - z)
        if e < 2: return GILT[2]
        if e < 3: return RED[1]
        return RED[2] if (int(x) + int(z)) % 8 else RED[1]
    rugm = Mat(RED, 2, rug)
    rugm.ramps = (RED, GILT)
    els.append(E((CX - 22, 0, CZ - 22), (CX + 22, 1, CZ + 22), rugm, skip="down", name="rug"))
    cz0 = CZ + WALL_A - 2 - 12
    els.append(E((CX - 8, 0, cz0), (CX + 8, 8, cz0 + 10), "plank_x", skip="down", name="chest"))
    els.append(E((CX - 9, 8, cz0 - 1), (CX + 9, 11, cz0 + 11), "swood_x", name="chest lid"))
    for x0 in (CX - 6, CX + 4):
        els.append(E((x0, 0, cz0 - 1), (x0 + 2, 11, cz0), "iron", skip="down", name="chest strap"))
    return Prop.split("tent_pavilion", els, SIZE, moving=[("pennant", flag, (27,))])
