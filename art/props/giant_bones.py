"""Shared ground for the giant bone set (giant_rib, giant_vertebra, giant_skull_fragment, giant_knuckle).

Old ivory bone, weathered: long fibre cracks that follow each bone box (face-local, so a leaning box keeps its grain),
rare cross cracks and pits, lichen patches on the upper surfaces, moss and an earth stain where the bone meets the ground.
`bone_chain` lays a curved bone as a chain of boxes turned in 22.5-degree steps, so a rib reads as a smooth bend and not
as a staircase.
"""
import math
from proplib import E, Mat, make_ramp, h3, LIT, blob

IVORY = make_ramp("BDB092", 0.17)    # old ivory: pale, warm, a little grey
MARROW = make_ramp("9C8466", 0.18)   # the spongy core of a cut or broken bone
MOSS = make_ramp("5F7A3A", 0.18)     # same moss as the stone hand: one world
LICHEN = make_ramp("8E9256", 0.16)   # grey-green lichen crust
DIRT = make_ramp("6E5038", 0.18)     # churned earth (stone hand's)


def _tone(ramp, face, d=0, base=2):
    return ramp[max(0, min(4, base + LIT[face] + d))]


def ground_and_lichen(face, P, moss_h, seed):
    """Moss up to `moss_h` px (ragged), a 2 px earth stain above it, lichen patches higher up; None for clean bone."""
    x, y, z = P
    line = moss_h + int(h3(math.floor(x / 2), math.floor(z / 2), 9 + seed) * 4)
    if y < line:
        return _tone(MOSS, face, 1 if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 11) < 0.25 else 0)
    if y < line + 2: return -1
    if face != "down":
        cell = (math.floor(x / 4), math.floor(y / 3), math.floor(z / 4))
        if h3(*cell, 31 + seed) < (0.10 if face == "up" else 0.03) and h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 33) < 0.6:
            return _tone(LICHEN, face, 1 if h3(x, y, z, 35) < 0.2 else 0)
    return None


def bone_pattern(moss_h=4, seed=0, grain=True):
    """Pattern for bone faces. `moss_h` px: below it (ragged) moss grows, just above it the bone is stained by earth.
    grain=False (skull plates): no long fibre cracks; a wandering suture line and pits instead."""
    def p(face, P, c, r, w, h):
        g = ground_and_lichen(face, P, moss_h, seed)
        if g is not None: return g
        fi = "nsewud".index(face[0])
        if not grain:
            x, y, z = P
            # a suture: a jagged line wandering round the dome at about y 33, and one over the crown along x = 8
            if face != "up" and abs(y - (33 + (math.floor((x + z) / 3) % 3) - 1)) < 0.6: return -1
            if face == "up" and abs(x - (8 + (math.floor(z / 3) % 3) - 1)) < 0.6: return -1
            if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 13 + seed) < 0.03: return -1
            return 0
        # face-local: `a` runs along the box's long side, `b` across it
        if w >= h: a, b, L = c, r, w
        else: a, b, L = r, c, h
        # long fibre cracks along the bone: some across-positions carry a dashed crack
        if b > 0 and h3(b, fi, 3 + seed) < 0.16 and h3(b, math.floor(a / 7), 5 + seed) < 0.55: return -1
        # rare cross crack, jagged, a full tone darker
        if L >= 12 and h3(math.floor(a / 11), 7 + seed, fi) < 0.12 and (a % 11) == (2 + (b // 2) % 2): return -2
        # pits: 2x2, sparse
        if h3(math.floor(c / 2), math.floor(r / 2), fi, 13 + seed) < 0.022: return -1
        return 0
    return p


def bone(moss_h=4, seed=0, base=2, edges=False, grain=True):
    m = Mat(IVORY, base, bone_pattern(moss_h, seed, grain), top=1, edges=edges)
    m.ramps = (MOSS, LICHEN)
    return m


def disc_face(cu, cv, radius, moss_h=4, seed=0, plane="xy"):
    """The flat end of a bone drum (a vertebra's articular face): growth rings about (cu, cv) in `plane`, a lit rim,
    a dark dimple in the middle, two radial cracks."""
    iu, iv = "xyz".index(plane[0]), "xyz".index(plane[1])
    def p(face, P, c, r, w, h):
        g = ground_and_lichen(face, P, moss_h, seed)
        if g is not None: return g
        du, dv = P[iu] - cu, P[iv] - cv; d = math.hypot(du, dv)
        if d > radius - 1.5: return 1
        if d < 2.2: return -2
        ang = math.degrees(math.atan2(dv, du)) % 360
        if d > 4 and any(abs(ang - a0) < 30 / d for a0 in (60 + seed * 40, 200 + seed * 25)): return -2
        if int(d) in (5, 9): return -1
        return 0
    m = Mat(IVORY, 2, p, edges=False, top=1)
    m.ramps = (MOSS, LICHEN)
    return m


def pat_marrow(face, P, c, r, w, h):
    """A clean cut through a bone: a pale 2 px cortical rim around a dark spongy core (2 px cells, no speckle)."""
    d = min(c, r, w - 1 - c, h - 1 - r)
    if d < 1: return IVORY[3]
    if d < 2: return MARROW[1]
    return MARROW[1 if h3(math.floor(c / 2), math.floor(r / 2), 21) < 0.35 else 2]

MARROW_FACE = Mat(MARROW, 2, pat_marrow, edges=False, lit=False)
MARROW_FACE.ramps = (IVORY,)


def pat_break(face, P, c, r, w, h):
    """A big old break through thick bone: spongy core in 2 px cells, world-aligned, no rim (a rim reads as a frame)."""
    x, y, z = P
    k = h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 23)
    return -1 if k < 0.3 else (1 if k > 0.85 else 0)

BREAK_FACE = Mat(MARROW, 3, pat_break, edges=False)


def pat_earth(face, P, c, r, w, h):
    x, y, z = P
    return -1 if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 17) < 0.14 else 0

def pat_moss(face, P, c, r, w, h):
    x, y, z = P
    k = h3(math.floor(x / 2), math.floor(z / 2), 29)
    return 1 if k < 0.14 else (-1 if k > 0.9 else 0)

EARTH = Mat(DIRT, 2, pat_earth)
MOSSY = Mat(MOSS, 2, pat_moss, top=0)


def mound(frm, to, layers=2, seed=0, name="earth mound"):
    """A low earth mound (blob) with moss on its top faces."""
    out = blob(frm, to, EARTH, layers=layers, seed=seed, name=name)
    for e in out: e.faces = {"up": MOSSY}
    return out


# ---- light on turned boxes -----------------------------------------------------------------------------------------
_NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
_LIGHT = {"up": 1, "down": -2, "north": 0, "east": 0, "south": -1, "west": -1}


def _turn(v, axis, ang):
    a = math.radians(ang); c, s = math.cos(a), math.sin(a); x, y, z = v
    if axis == "x": return (x, c * y - s * z, s * y + c * z)
    if axis == "y": return (c * x + s * z, y, -s * x + c * z)
    return (c * x - s * y, s * x + c * y, z)


class Turned:
    """A material on a rotated box, lit by where each face really points. Mat bakes light by face name (top +1, west
    -1 ...); on a box turned 45 degrees its "west" face may look at the sky. This paints each face with the light of
    its turned normal (weighted over the axes it leans between), keeping the face's own name when the light agrees."""
    def __init__(s, mat, axis, ang):
        s.mat, s.ramp, s.ramps = mat, mat.ramp, getattr(mat, "ramps", ())
        s.remap = {}
        for f, n in _NORMAL.items():
            t = _turn(n, axis, ang)
            l = (max(0, t[1]) * 1 + max(0, -t[1]) * -2 + max(0, t[2]) * -1 + max(0, -t[0]) * -1)
            want = int(math.floor(l + 0.5))
            s.remap[f] = f if _LIGHT[f] == want else {1: "up", 0: "north", -1: "south", -2: "down"}[max(-2, min(1, want))]
    def __call__(s, face, *a):
        return s.mat(s.remap[face], *a)


def relight(els):
    """Give every rotated element (and its face overrides) light from its real facing. Call on a prop's final list."""
    for e in els:
        if not e.rot or abs(e.rot[1]) < 30: continue        # 22.5-degree leans keep their faces' own light
        if not isinstance(e.mat, Turned) and hasattr(e.mat, "ramp"): e.mat = Turned(e.mat, e.rot[0], e.rot[1])
        e.faces = {k: (Turned(v, e.rot[0], e.rot[1]) if hasattr(v, "ramp") and not isinstance(v, Turned) else v)
                   for k, v in e.faces.items()}
    return els


# ---- round bones: octagons instead of 1 px stairs -------------------------------------------------------------------
_PL = {"z": (0, 1, 2), "x": (2, 1, 0), "y": (0, 2, 1)}   # axis -> (u, v, a) indices


def _box(axis, u0, u1, v0, v1, a0, a1):
    ui, vi, ai = _PL[axis]; frm, to = [0, 0, 0], [0, 0, 0]
    frm[ui], to[ui], frm[vi], to[vi], frm[ai], to[ai] = u0, u1, v0, v1, a0, a1
    return frm, to


def octagon(center, R, axis, a0, a1, mat, name="drum", faces=None, skip=""):
    """A solid octagonal drum along `axis` (inradius R, whole-px centre (u, v) in the axis' plane): a wide box, the two
    arms of a tall box, and two 45-degree boxes for the diagonals, those 1 px shorter at each end so no two end faces
    share a plane (no z-fighting). Four or five elements, and it reads round at any distance."""
    cu, cv = center; L = int(round(R * math.tan(math.radians(22.5))))
    ui, vi, ai = _PL[axis]; org = [0, 0, 0]; org[ui], org[vi], org[ai] = cu, cv, (a0 + a1) / 2
    out = [E(*_box(axis, cu - R, cu + R, cv - L, cv + L, a0, a1), mat, name=name, faces=faces, skip=skip),
           E(*_box(axis, cu - L, cu + L, cv + L, cv + R, a0, a1), mat, name=name, faces=faces, skip=skip),
           E(*_box(axis, cu - L, cu + L, cv - R, cv - L, a0, a1), mat, name=name, faces=faces, skip=skip)]
    for ang, frm_to in ((45, _box(axis, cu - R, cu + R, cv - L, cv + L, a0 + 1, a1 - 1)),
                        (-45, _box(axis, cu - R, cu + R, cv - L, cv + L, a0 + 1, a1 - 1))):
        out.append(E(*frm_to, mat, name=name, faces=faces, skip=skip, rot=(axis, ang, tuple(org))))
    return out


def octa_chain(start, steps, zc, mat, bevel=4, **kw):
    """A curved bone with an octagonal section: `steps` (theta, length, width) as bone_chain, each step drawn as a cross
    of two boxes, width x (width - bevel) and (width - bevel) x width, so the corners read bevelled. Returns (els, end)."""
    a, end = bone_chain(start, [(th, L, w, w - bevel) for th, L, w in steps], 2, zc - 1, zc + 1, mat, **kw)
    b, _ = bone_chain(start, [(th, L, w - bevel, w) for th, L, w in steps], 2, zc - 1, zc + 1, mat, **kw)
    return a + b, end


def octagon_cut(center, R, a0, a1, mat, xmax, name="drum", faces=None, cut_faces=None):
    """An octagonal drum along y (as `octagon(..., "y", ...)`) broken off east of x = `xmax`: the straight boxes are
    cut there (the cut face takes `cut_faces`) and the diagonals keep only their west halves."""
    cu, cv = center; L = int(round(R * math.tan(math.radians(22.5)))); out = []
    fc = dict(faces or {}); cf = dict(fc); cf.update(cut_faces or {})
    for x0, x1, z0, z1 in ((cu - R, cu + R, cv - L, cv + L), (cu - L, cu + L, cv + L, cv + R), (cu - L, cu + L, cv - R, cv - L)):
        if x0 >= xmax: continue
        out.append(E((x0, a0, z0), (min(x1, xmax), a1, z1), mat, name=name, faces=cf if x1 > xmax else fc))
    for ang in (45, -45):
        out.append(E((cu - R, a0 + 1, cv - L), (cu, a1 - 1, cv + L), mat, name=name, faces=fc, rot=("y", ang, (cu, (a0 + a1) / 2, cv))))
    return out


def octagon_ring(center, R, t, a0, a1, mat, drop=(), name="ring", faces=None):
    """An octagonal ring along z (an eye socket's rim): outer inradius R, bars `t` thick. Bars: 0 top, 1 top-east,
    2 east, 3 bottom-east, 4 bottom, 5 bottom-west, 6 west, 7 top-west (odd = the 45-degree bars); `drop` leaves bars out (a broken rim). Diagonal bars are 1 px shallower at each end (no shared planes)."""
    cu, cv = center; L = int(round(R * math.tan(math.radians(22.5)))); org = (cu, cv, (a0 + a1) / 2)
    top = ((cu - L, cv + R - t, a0), (cu + L, cv + R, a1)); bot = ((cu - L, cv - R, a0), (cu + L, cv - R + t, a1))
    inset = lambda b: ((b[0][0], b[0][1], a0 + 1), (b[1][0], b[1][1], a1 - 1))
    bars = {0: (top, None), 2: (((cu + R - t, cv - L, a0), (cu + R, cv + L, a1)), None), 4: (bot, None),
            6: (((cu - R, cv - L, a0), (cu - R + t, cv + L, a1)), None),
            1: (inset(top), -45), 7: (inset(top), 45), 3: (inset(bot), 45), 5: (inset(bot), -45)}
    out = []
    for i in range(8):
        if i in drop: continue
        (frm, to), ang = bars[i]
        out.append(E(frm, to, mat, name=name, faces=faces, rot=("z", ang, org) if ang else None))
    return out


def octagon_arch(center, R, t, a0, a1, mat, foot, name="arch"):
    """The upper half of an octagonal ring along z (an arch over a hole): outer inradius R, bars `t` thick, the two
    side bars running down to y = `foot`."""
    cu, cv = center; L = int(round(R * math.tan(math.radians(22.5))))
    out = [E((cu - L, cv + R - t, a0), (cu + L, cv + R, a1), mat, name=name),
           E((cu + R - t, foot, a0), (cu + R, cv + L, a1), mat, name=name),
           E((cu - R, foot, a0), (cu - R + t, cv + L, a1), mat, name=name)]
    for ang in (45, -45):
        out.append(E((cu - L, cv + R - t, a0 + 1), (cu + L, cv + R, a1 - 1), mat, name=name, rot=("z", ang, (cu, cv, (a0 + a1) / 2))))
    return out


# ---- curved bones -------------------------------------------------------------------------------------------------
REGION = 48

def _rot2(ang, v):
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    return (c * v[0] - s * v[1], s * v[0] + c * v[1])


def bone_chain(start, steps, t, z0, z1, mat, ov=3, name="bone", faces=None, end_faces=None, skip_first="down", end_ov=1):
    """A curved bone in the x-y plane: a chain of boxes from `start` (x, y: centre of the first box's base), each
    step (theta, length): theta degrees from straight up, leaning east (+x): 0, 22.5, 45, 67.5 or 90 (or the same
    negative, leaning west). `t` is the bone's thickness in the plane (even), z0..z1 its depth. Boxes overlap `ov` px at
    each joint so a bend never opens a gap. Rotated boxes are stored where they stay inside one 3-cell region on y (the
    big-prop splitter cuts only across a box's rotation axis) and turned about an origin that puts them on the line.
    Returns (elements, end point)."""
    P = (float(start[0]), float(start[1])); out = []; n = len(steps)
    zc, t0 = (z0 + z1) / 2, t
    for i, step in enumerate(steps):
        th, L = step[0], step[1]
        # optional per-step (theta, length, thickness, depth): a tapering bone, depth centred on the same mid-plane
        t = step[2] if len(step) > 2 else t0
        if len(step) > 3: z0, z1 = int(zc - step[3] / 2), int(zc + step[3] / 2)
        d = (math.sin(math.radians(th)), math.cos(math.radians(th)))
        last = i == n - 1
        o0 = 0 if i == 0 else ov; o1 = 0 if last else (end_ov if i == n - 2 else ov)
        if abs(th) <= 45:   # a y-long box turned by -theta
            ang = -th
            def box(bx, by): return (bx - t // 2, by - o0, z0), (bx + t // 2, by + L + o1, z1)
        else:               # an x-long box (pointing +x or -x) turned toward the vertical
            sgn = 1 if th > 0 else -1
            ang = sgn * (90 - abs(th))
            if sgn > 0:
                def box(bx, by): return (bx - o0, by - t // 2, z0), (bx + L + o1, by + t // 2, z1)
            else:
                def box(bx, by): return (bx - L - o1, by - t // 2, z0), (bx + o0, by + t // 2, z1)
        sk = skip_first if i == 0 else ""
        fc = dict(faces or {})
        if last and end_faces: fc.update(end_faces)
        if ang == 0:
            bx, by = round(P[0]), round(P[1]); P = (bx, by)
            frm, to = box(bx, by)
            out.append(E(frm, to, mat, skip=sk, name=name, faces=fc))
        else:
            bx, by = round(P[0]), round(P[1])
            frm, to = box(bx, by)
            # keep the unrotated box inside one 48 px region on y
            r0 = math.floor(frm[1] / REGION) * REGION
            if to[1] > r0 + REGION:
                up = r0 + REGION - frm[1]; down = to[1] - (r0 + REGION)
                by = by + up if up <= down else by - down
                frm, to = box(bx, by)
            # origin O with R(B - O) + O = P  ->  (I - R) O = P - R B
            a = math.radians(ang); c, s = math.cos(a), math.sin(a)
            RB = (c * bx - s * by, s * bx + c * by)
            rx, ry = P[0] - RB[0], P[1] - RB[1]
            m00, m01, m10, m11 = 1 - c, s, -s, 1 - c
            det = m00 * m11 - m01 * m10
            ox = (rx * m11 - m01 * ry) / det; oy = (m00 * ry - m10 * rx) / det
            out.append(E(frm, to, mat, skip=sk, name=name, faces=fc, rot=("z", ang, (round(ox, 4), round(oy, 4), (z0 + z1) / 2))))
        P = (P[0] + d[0] * L, P[1] + d[1] * L)
    return out, P
