"""Commander's Great Pavilion (`tent_pavilion_large`): 15x12x15 cells, anchor cell (7,0,7). Brief: art/props/tent_pavilion_large/brief.md.

The large version of `tent_pavilion` (same look, same painter and octagon method; read that model's doc), sized to the
block-built map pavilion (compare fixture structures/compare/tent_pavilion.nbt: 15x13x15 with its ground layer, tent
11 blocks across at the eave and 12 tall above the floor). Footprint x -112..128, y 0..192, z -112..128 px; centre (8, 8).
Walls apothem 72 (9 blocks across), eave apothem 86, valance 88 (11 blocks across), roof 3 px a step to y 152,
finial tip at 191. Diagonal boxes longer than a 3-cell region are cut into pieces along the face; ropes crossing the
y=48 region line are cut in two with one rotation origin.
Build:  python art/props/build_props.py tent_pavilion_large      Card:  python tools/prop_card.py tent_pavilion_large

Palette: `build(pid, pal)` takes a `Palette` (stripe ramps, hemp, optional weathering). `ORIGINAL` is this prop; the colour
variants (`tent_pavilion_large_weathered|dawn|dusk`) are thin model.py wrappers that call `build` with their own palette,
so the geometry, collision and solid cells are identical. Do not change ORIGINAL's output: it must build byte-identical.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, LINEN, GILT, WOOD, SWOOD, IRON   # noqa: F401

SIZE = (15, 12, 15)
CX = CZ = 8                     # centre of the pavilion (middle of the anchor cell)
R2 = math.sqrt(0.5)
TAN = math.tan(math.radians(22.5))
REGIONS = (-112, -64, -16, 32, 80, 128)   # x and z region bounds of the split grid (Prop.split, 3 cells a part)

CREAM = make_ramp("E0D2AC", 0.16)   # undyed canvas
RED = make_ramp("8C2A25", 0.20)     # madder-red canvas
ROPE_R = make_ramp("A8977A", 0.16)  # hemp


class Palette:
    """Colours of one pavilion: `light` / `dark` stripe ramps (roof ones default to the same), hemp, and optional `wear`:
    a dict {mud: ramp, mud_lo, mud_span (ragged mud line height px), dirt (px of grimed canvas above it), valance_dirt,
    patches: [(octagon face deg, u centre, half width, y0, y1, ramp)], roof_patches: [(face deg, u, half width, r0, r1, ramp)]}.
    Angles: 90 = north (the door face), 0 = east, 180 = west, -90 = south."""
    def __init__(s, light=CREAM, dark=RED, rope=ROPE_R, roof_light=None, roof_dark=None, wear=None):
        s.light, s.dark, s.rope, s.wear = light, dark, rope, wear
        s.roof_light, s.roof_dark = roof_light or light, roof_dark or dark

    def ramps(s):
        w = s.wear or {}
        extra = [w["mud"]] if w.get("mud") else []
        extra += [p[-1] for p in list(w.get("patches", ())) + list(w.get("roof_patches", ()))]
        return (s.light, s.dark, s.roof_light, s.roof_dark, s.rope) + tuple(extra)


ORIGINAL = Palette()
CURRENT = [ORIGINAL]      # the palette `build` is painting with (read by Canvas; the helpers below stay palette-free)

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


def sector(x, z):
    """(octagon face index k, depth along the face normal, distance along the face tangent) of a point: face k looks
    toward angle 45 k degrees (90 = north), like `stripe`."""
    ang = math.degrees(math.atan2(-(z - CZ), x - CX)); k = int(round(ang / 45.0)) % 8
    a = math.radians(45 * k); nx, nz = math.cos(a), -math.sin(a); tx, tz = math.sin(a), math.cos(a)
    return k, (x - CX) * nx + (z - CZ) * nz, (x - CX) * tx + (z - CZ) * tz, (nx, nz)


class Canvas:
    """Striped canvas painter. `rot` is the element's turn about y (so texels map to world points and normals);
    kind: wall (lit top row, dark hem), roof (flat steps, red crown), valance (stripes, dark trim rows), flap (red)."""

    def __init__(s, kind, rot=None, crown=0.0, pal=None):
        pal = pal or CURRENT[0]
        s.kind, s.rot, s.crown, s.pal = kind, rot, crown, pal
        s.ramps = pal.ramps()

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
        pal = s.pal
        if s.kind == "roof": ramp = pal.roof_dark if col == 0 else pal.roof_light
        else: ramp = pal.dark if col == 0 else pal.light
        if pal.wear: return s.weather(f, n, x, y, z, t, ramp)
        return ramp[max(0, min(4, t))]

    def weather(s, f, n, x, y, z, t, ramp):
        """Grime, mud and patches on top of the plain tone `t` (outer faces only: inside the tent stays clean)."""
        wr = s.pal.wear; clamp = lambda v: max(0, min(4, v))
        k, dn, dt, (nx, nz) = sector(x, z)
        outer = f == "up" or (f != "down" and n[0] * nx + n[2] * nz > 0.9)
        if not outer: return ramp[clamp(t)]
        if s.kind == "roof":
            for kk, u, hw, r0, r1, pr in wr.get("roof_patches", ()):
                if kk % 360 == 45 * k and abs(dt - u) < hw and r0 <= dn < r1:
                    edge = abs(dt - u) >= hw - 1 or dn < r0 + 1 or dn >= r1 - 1
                    return pr[clamp(t - (1 if edge else 0))]
            return ramp[clamp(t)]
        if f == "up": return ramp[clamp(t)]
        if s.kind == "valance":
            return ramp[clamp(t - (1 if y < wr.get("valance_dirt", 0) else 0))]
        for kk, u, hw, y0, y1, pr in wr.get("patches", ()):
            if s.kind == "wall" and kk % 360 == 45 * k and abs(dt - u) < hw and y0 <= y < y1:
                edge = abs(dt - u) >= hw - 1 or y < y0 + 1 or y >= y1 - 1
                return pr[clamp(t - (1 if edge else 0))]
        # ragged mud line: 3 px columns along the face, each its own height; grimed canvas above it
        key = (k, math.floor(dt / 3.0))
        line = wr["mud_lo"] + int(h3(key[0], key[1], 5) * wr["mud_span"])
        if y < line: return wr["mud"][clamp(t - (1 if y < 2 else 0))]
        if y < line + wr["dirt"] and h3(key[0], key[1], 7) < 0.7: return ramp[clamp(t - 1)]
        return ramp[clamp(t)]


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


def span(f, u0, u1, a, t, y0, y1, *rest, most=46, **k):
    """face_box over u0..u1, a diagonal face cut into pieces of at most `most` px (a turned box must fit one region;
    the walls use 16 px pieces so each stored box sits on the cell its canvas really crosses: the card's fill map
    reads boxes unturned)."""
    if len(f) == 1 or u1 - u0 <= most: return [face_box(f, a, t, y0, y1, u0, u1, *rest, **k)]
    n = -(-(u1 - u0) // most); cuts = [u0 + (u1 - u0) * i // n for i in range(n + 1)]
    return [face_box(f, a, t, y0, y1, cuts[i], cuts[i + 1], *rest, **k) for i in range(n)]


def ring(a, t, y0, y1, kind, name, crown=0.0, skip=""):
    h = half(a); out = []
    for f in NORM:
        ext = 0 if len(f) == 1 else 1                       # diagonal boxes run 1 px past each corner
        out += span(f, -h - ext, h + ext, a, t, y0, y1, kind, name, crown, skip=skip)
    return out


# roof: octagonal rings, 3 px a step, a concave pavilion curve from the eave (apothem 86) to the crown
ROOF_Y0, STEP, ROOF_N, EAVE = 56, 3, 32, 86
ROOF_A = [int(round(EAVE * (1 - i / 34.0) ** 1.45)) for i in range(ROOF_N)]
WALL_A, WALL_H, WALL_T = 72, 56, 3
VAL_A = 88
DOOR = 12            # the door runs u -12..12 on the north face: x -4..20, the whole of cell x=7
CROWN = 20.0         # roof canvas inside this radius is the red crown


def build(pid="tent_pavilion_large", pal=ORIGINAL):
    CURRENT[0] = pal
    els = []
    # ---- walls: canvas on seven faces and the door face's two jambs and lintel ----
    h = half(WALL_A)
    for f in NORM:
        ext = 0 if len(f) == 1 else 1
        if f == "N":
            els.append(face_box(f, WALL_A, WALL_T, 0, WALL_H, -h, -DOOR, "wall", "wall", skip="down"))
            els.append(face_box(f, WALL_A, WALL_T, 0, WALL_H, DOOR, h, "wall", "wall", skip="down"))
            els.append(face_box(f, WALL_A, WALL_T, 46, WALL_H, -DOOR, DOOR, "wall", "lintel"))
        else:
            els += span(f, -h - ext, h + ext, WALL_A, WALL_T, 0, WALL_H, "wall", "wall", skip="down", most=16)
    # door posts either side of the opening
    zw = CZ - WALL_A
    for x0 in (CX - DOOR - 4, CX + DOOR):
        els.append(E((x0, 0, zw - 1), (x0 + 4, 49, zw + 4), "wood_y", skip="down", name="door post"))
    # the two door flaps, folded back against the wall and tied (turned 45 degrees about the door edge)
    for hx, ang, x0, x1 in ((CX + DOOR, 45, CX + DOOR, CX + DOOR + 11), (CX - DOOR, -45, CX - DOOR - 11, CX - DOOR)):
        rot = ("y", ang, (hx, 0, zw))
        els.append(E((x0, 4, zw - 3), (x1, 45, zw - 1), Canvas("flap", rot), rot=rot, name="door flap"))
        tx0 = x0 + 4 if ang > 0 else x1 - 7
        els.append(E((tx0, 22, zw - 4), (tx0 + 3, 26, zw), Mat(pal.rope, 2), rot=rot, name="flap tie"))

    # ---- roof ----
    for i, a in enumerate(ROOF_A):
        y0 = ROOF_Y0 + STEP * i
        nxt = ROOF_A[i + 1] if i + 1 < ROOF_N else 0
        if a <= 10:         # the crown cap: one square box a step
            c = max(3, int(round(a * 0.9)))
            els.append(E((CX - c, y0, CZ - c), (CX + c, y0 + STEP, CZ + c), Canvas("roof", crown=999), name="crown"))
            continue
        t = a - nxt + 1
        if i == 0: t = max(t, a - (WALL_A - WALL_T))  # the eave ring closes over the wall top
        els += ring(a, t, y0, y0 + STEP, "roof", "roof", CROWN)
    top = ROOF_Y0 + STEP * ROOF_N
    # ---- valance: a striped band round the eave with three dagged tabs a face ----
    els += ring(VAL_A, 2, 46, 61, "valance", "valance")
    for f in NORM:
        for u in (-24, 0, 24):
            els.append(face_box(f, VAL_A, 2, 40, 46, u - 7, u + 7, "valance", "valance tab"))
            els.append(face_box(f, VAL_A, 2, 36, 40, u - 3, u + 3, "valance", "valance tab"))

    # ---- centre pole, gilt collar and finial, pennant ----
    els.append(E((CX - 4, 0, CZ - 4), (CX + 4, 178, CZ + 4), "wood_y", skip="down", name="centre pole"))
    els.append(E((CX - 6, top, CZ - 6), (CX + 6, top + 3, CZ + 6), "gilt", name="gilt collar"))
    els.append(E((CX - 5, 178, CZ - 5), (CX + 5, 186, CZ + 5), "gilt", name="finial"))
    els.append(E((CX - 3, 186, CZ - 3), (CX + 3, 191, CZ + 3), "gilt", name="finial tip"))
    pen = Mat(pal.dark, 2, lambda f, P, c, r, w, h: 2 if 6 <= P[1] - 160 < 9 and f in ("north", "south") else 0)
    els.append(E((CX + 4, 160, CZ - 1), (CX + 26, 176, CZ + 1), pen, name="pennant"))
    els.append(E((CX + 26, 160, CZ - 1), (CX + 36, 165, CZ + 1), pen, name="pennant tail"))
    els.append(E((CX + 26, 171, CZ - 1), (CX + 36, 176, CZ + 1), pen, name="pennant tail"))

    # ---- guy ropes (22.5 degrees off the vertical) to pegs, two a face on the four cardinal faces ----
    # Each rope is cut at y=48 (a region line) into two boxes turned about the same origin: one straight rope.
    rope = Mat(pal.rope, 2, edges=False)
    L, foot = 56, 110

    def guy(frm, to, rot):
        return [E((frm[0], 0, frm[2]), (to[0], 48, to[2]), rope, rot=rot, name="guy rope"),
                E((frm[0], 48, frm[2]), (to[0], L, to[2]), rope, rot=rot, name="guy rope")]
    for u in (-24, 24):
        xe = CX + foot; zc = CZ + u
        els += guy((xe, 0, zc - 1), (xe + 2, L, zc + 1), ("z", 22.5, (xe, 0, zc)))
        els.append(E((xe, 0, zc - 2), (xe + 4, 6, zc + 2), "swood_y", skip="down", name="peg"))
        xw = CX - foot
        els += guy((xw - 2, 0, zc - 1), (xw, L, zc + 1), ("z", -22.5, (xw, 0, zc)))
        els.append(E((xw - 4, 0, zc - 2), (xw, 6, zc + 2), "swood_y", skip="down", name="peg"))
        xc = CX + u; zs = CZ + foot
        els += guy((xc - 1, 0, zs), (xc + 1, L, zs + 2), ("x", -22.5, (xc, 0, zs)))
        els.append(E((xc - 2, 0, zs), (xc + 2, 6, zs + 4), "swood_y", skip="down", name="peg"))
        xn = CX + (u + (2 if u > 0 else -2))          # front ropes stand a little wider, clear of the door flaps
        zn = CZ - foot
        els += guy((xn - 1, 0, zn - 2), (xn + 1, L, zn), ("x", 22.5, (xn, 0, zn)))
        els.append(E((xn - 2, 0, zn - 4), (xn + 2, 6, zn), "swood_y", skip="down", name="peg"))

    # ---- inside: a red rug with a gilt border round the pole, the commander's chest at the back ----
    R_ = 34

    def rug(f, P, c, r, w, h):
        x, y, z = P
        e = min(x - (CX - R_), (CX + R_) - x, z - (CZ - R_), (CZ + R_) - z)
        if e < 3: return GILT[2]
        if e < 4: return pal.dark[1]
        return pal.dark[2] if (int(x) + int(z)) % 8 else pal.dark[1]
    rugm = Mat(pal.dark, 2, rug)
    rugm.ramps = (pal.dark, GILT)
    els.append(E((CX - R_, 0, CZ - R_), (CX + R_, 1, CZ + R_), rugm, skip="down", name="rug"))
    cz0 = 56           # by the back wall, across a cell line so no one cell is mostly chest
    els.append(E((-12, 0, cz0), (12, 9, cz0 + 12), "plank_x", skip="down", name="chest"))
    els.append(E((-13, 9, cz0 - 1), (13, 12, cz0 + 13), "swood_x", name="chest lid"))
    for x0 in (-9, 6):
        els.append(E((x0, 0, cz0 - 1), (x0 + 3, 12, cz0), "iron", skip="down", name="chest strap"))
    return Prop.split(pid, els, SIZE)
