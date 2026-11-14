"""Coplanar faces between troop cubes (z-fighting): the finder the check (tools/zfight_check.py) and the generators share.

Two faces of different cubes that lie in one plane, face the same way and overlap draw at the same depth, so the game cannot
pick one and the two textures flicker in stripes (Kort 2026-11-14: a Gilded shin while the knee bends, a shield's edge).

  find(model)          every such pair in the rest pose and with each jointed limb bent (BENDS): a face pair fails when the
                       faces point the same way, their planes are within EPS (0.005 px) and they overlap by more than
                       MIN_AREA (0.25 px2), unless the overlap is hidden inside a third cube
  settle(bones, emit)  the generators' tie-break: moves one cube of each pair by SEP (0.03 px) of inflate until nothing in
                       the emitted model is nearer than SOLVE_EPS. Inflate grows or shrinks a box about its own centre
                       and leaves its UVs, so the texture and the shape are unchanged (offsets stay under 0.05 px)

A model is read from a troop file of either kind:
  Model.old(json)              models/troop/<troop>.json (tools/build_skins.py; y down, pivots relative to the parent)
  Model.geo(geo_json, rig)     a GeckoLib geo (y up, absolute pivots) with its troop_rig visibility rules
Bones tagged weapon or draw are left out (weapons have their own frame check in build_skins). A bone shows when its own
stages / weaponTiers / variants allow it and its parent shows; each tier x face combination is a visibility set.
"""
import math

EPS = 0.005        # px: planes nearer than this are one plane (the check)
SOLVE_EPS = 0.015  # px: the generators keep every same-way face pair at least this far apart (settle; most end SEP apart)
MIN_AREA = 0.25    # px2: overlaps up to this (an edge, a corner) do not show
SEP = 0.03         # px of inflate settle moves a cube by: resolves in a 24-bit depth buffer to about 40 blocks
BENDS = (40, 75)   # degrees each jointed limb (a forearm or shin) is bent by, besides the rest pose (not 90: an
                   # axis-aligned bend lines a carried shield up with the torso by coincidence, a pose no clip holds)
NUDGE = 0.01       # px a face sample point is moved out along its normal for the hidden test
SKIP_TAGS = ("weapon", "draw")


# ---- small vector helpers ------------------------------------------------------------------------
def _rot(r):
    rx, ry, rz = (math.radians(v) for v in r)
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    # Rz * Ry * Rx (ModelPart.rotationZYX and Bedrock's order)
    return ((cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx),
            (sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx),
            (-sy, cy * sx, cy * cx))


def _mv(M, v): return (M[0][0] * v[0] + M[0][1] * v[1] + M[0][2] * v[2], M[1][0] * v[0] + M[1][1] * v[1] + M[1][2] * v[2],
                       M[2][0] * v[0] + M[2][1] * v[1] + M[2][2] * v[2])
def _sub(a, b): return (a[0] - b[0], a[1] - b[1], a[2] - b[2])
def _add(a, b): return (a[0] + b[0], a[1] + b[1], a[2] + b[2])
def _dot(a, b): return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
def _cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def _norm(a):
    L = math.sqrt(_dot(a, a))
    return tuple(v / L for v in a)


# ---- the model -------------------------------------------------------------------------------------
class Cube:
    __slots__ = ("key", "bone", "origin", "size", "inflate", "pivot", "rot")

    def __init__(s, key, bone, origin, size, inflate, pivot=None, rot=None):
        s.key, s.bone, s.origin, s.size = key, bone, tuple(origin), tuple(size)
        s.inflate = tuple(inflate) if isinstance(inflate, (list, tuple)) else (inflate or 0.0,) * 3
        s.pivot, s.rot = pivot, rot


class Model:
    """bones: {name: dict(parent, pivot (absolute), rot, bend (+1/-1/0: which way the joint folds))}; cubes: [Cube];
    sets: [frozenset of shown bone names] (one a tier x face combination, duplicates merged)."""

    def __init__(s, troop, kind, bones, cubes, sets):
        s.troop, s.kind, s.bones, s.cubes, s.sets = troop, kind, bones, cubes, sets

    @staticmethod
    def old(d):
        bones, cubes, rules = {}, [], {}
        for b in d["bones"]:
            pp = bones[b["parent"]]["pivot"] if b.get("parent") else (0, 0, 0)
            ap = _add(pp, b["pivot"])
            lim = b.get("limit")
            bend = (1 if lim[1] > 0 else -1) if lim else _bend_by_name(b["name"])
            bones[b["name"]] = dict(parent=b.get("parent") or None, pivot=ap, rot=tuple(b.get("rotation") or (0, 0, 0)), bend=bend)
            rules[b["name"]] = (b.get("stages") or [], b.get("weaponTiers") or [], b.get("variants"), b.get("tag", ""))
            for i, x in enumerate(b.get("boxes", [])):
                cubes.append(Cube("%s#%d" % (b["name"], i), b["name"], _add(ap, x["origin"]), x["size"], x.get("dilation", 0)))
        tiers = sorted(int(k) for k in d.get("skins", {"1": 0})) or [1]
        faces = (d.get("faces") or {}).get("count", 1)
        return Model(d.get("troop", "?"), "old", bones, cubes, _sets(bones, rules, tiers, faces))

    @staticmethod
    def geo(g, rig=None, troop=None):
        g = g["minecraft:geometry"][0] if "minecraft:geometry" in g else g
        rig = rig or {}
        R = rig.get("bones", {})
        bones, cubes, rules = {}, [], {}
        for b in g["bones"]:
            n = b["name"]
            # the geo's y is up: a handlib knee that folds by +rx folds by -rx here (troop_rig.geo negates x)
            bones[n] = dict(parent=b.get("parent"), pivot=tuple(b.get("pivot", (0, 0, 0))), rot=tuple(b.get("rotation") or (0, 0, 0)),
                            bend=-_bend_by_name(n))
            r = R.get(n) or {}
            rules[n] = (r.get("stages") or [], [], r.get("variants"), r.get("tag", ""))
            for i, c in enumerate(b.get("cubes", [])):
                cubes.append(Cube("%s#%d" % (n, i), n, c["origin"], c["size"], c.get("inflate", 0),
                                  tuple(c["pivot"]) if "rotation" in c else None, tuple(c["rotation"]) if "rotation" in c else None))
        tiers = rig.get("tiers") or [1, 2, 3]
        return Model(troop or rig.get("troop", "?"), "geo", bones, cubes, _sets(bones, rules, tiers, rig.get("faces", 1) or 1))


def _bend_by_name(n):
    return 1 if n.endswith("_shin") else -1 if n.endswith("_forearm") else 0


def _sets(bones, rules, tiers, faces):
    out = []
    for t in tiers:
        for f in range(max(1, faces)):
            shown = set()
            for n in bones:   # parents come first in both formats
                st, wt, var, tag = rules[n]
                p = bones[n]["parent"]
                if tag in SKIP_TAGS or (p and p not in shown):
                    continue
                if st and t not in st or wt and t not in wt or var is not None and f not in var:
                    continue
                shown.add(n)
            fs = frozenset(shown)
            if fs not in out:
                out.append(fs)
    return out


# ---- posing and faces ----------------------------------------------------------------------------------
def poses(model):
    """[(label, {bone: extra x rotation})]: the rest pose and each BENDS angle with every jointed limb folded its way."""
    joints = [n for n, b in model.bones.items() if b["bend"] and any(c.bone == n for c in model.cubes)]
    out = [("rest", {})]
    if joints:
        for a in BENDS:
            out.append(("bent%d" % a, {n: model.bones[n]["bend"] * a for n in joints}))
    return out


def _mm(A, B):
    return tuple(tuple(sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)) for i in range(3))


_I = ((1, 0, 0), (0, 1, 0), (0, 0, 1))


def _world(model, extra):
    """{cube key: (corners dict, linear part)} with the bone chain's rotations (and the pose's extra) applied."""
    chains = {}

    def chain(n):
        out = []
        while n:
            b = model.bones[n]
            r = b["rot"]
            if n in extra:
                r = (r[0] + extra[n], r[1], r[2])
            if any(r):
                out.append((b["pivot"], _rot(r)))
            n = b["parent"]
        return out

    res = {}
    for c in model.cubes:
        if c.bone not in chains:
            chains[c.bone] = chain(c.bone)
        ch = chains[c.bone]
        lo = [c.origin[i] - c.inflate[i] for i in range(3)]
        hi = [c.origin[i] + c.size[i] + c.inflate[i] for i in range(3)]
        cr = _rot(c.rot) if c.rot and any(c.rot) else None
        L = cr or _I
        for _, M in ch:
            L = _mm(M, L)
        C = {}
        for sx in (0, 1):
            for sy in (0, 1):
                for sz in (0, 1):
                    p = ((lo, hi)[sx][0], (lo, hi)[sy][1], (lo, hi)[sz][2])
                    if cr:
                        p = _add(_mv(cr, _sub(p, c.pivot)), c.pivot)
                    for piv, M in ch:
                        p = _add(_mv(M, _sub(p, piv)), piv)
                    C[(sx, sy, sz)] = p
        res[c.key] = (C, L)
    return res


FACE_NAMES = ("-x", "+x", "-y", "+y", "-z", "+z")


def _faces(c, C, L):
    """[(label, normal, plane offset, corner points)] of the six faces; a face's normal is its side's axis (ModelPart draws each
    face outward along its own axis, whatever the box's size), so a flat box shows its plane twice, once each way."""
    ax = [(L[0][a], L[1][a], L[2][a]) for a in range(3)]
    out = []
    for a in range(3):
        o1, o2 = [b for b in range(3) if b != a]
        for side in (0, 1):
            def k(u, v):
                t = [0, 0, 0]
                t[a], t[o1], t[o2] = side, u, v
                return tuple(t)
            pts = (C[k(0, 0)], C[k(1, 0)], C[k(1, 1)], C[k(0, 1)])
            n = ax[a] if side else tuple(-x for x in ax[a])
            out.append((FACE_NAMES[2 * a + side], n, _dot(n, pts[0]), pts))
    return out


def _basis(n):
    u = _norm(_cross(n, (1, 0, 0) if abs(n[0]) < 0.9 else (0, 1, 0)))
    return u, _cross(n, u)


def _poly(pts, u, v):
    p = [(_dot(q, u), _dot(q, v)) for q in pts]
    s = sum(p[i][0] * p[(i + 1) % 4][1] - p[(i + 1) % 4][0] * p[i][1] for i in range(4))
    return p if s >= 0 else p[::-1]


def _clip(subj, clipp):
    out = subj
    for i in range(len(clipp)):
        a, b = clipp[i], clipp[(i + 1) % len(clipp)]
        inp, out = out, []
        if not inp:
            break
        def inside(p): return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-12
        def cut(p, q):
            x1, y1, x2, y2, x3, y3, x4, y4 = p[0], p[1], q[0], q[1], a[0], a[1], b[0], b[1]
            den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
            if abs(den) < 1e-15:
                return q
            t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
            return (x1 + t * (x2 - x1), y1 + t * (y2 - y1))
        s = inp[-1]
        for e in inp:
            if inside(e):
                if not inside(s):
                    out.append(cut(s, e))
                out.append(e)
            elif inside(s):
                out.append(cut(s, e))
            s = e
    return out


def _grow(p, g):
    """A convex polygon with each corner pushed g out along both its edges (a rectangle grows g a side)."""
    out = []
    for i, q in enumerate(p):
        a, b = p[i - 1], p[(i + 1) % len(p)]
        dx, dy = 0.0, 0.0
        for r in (a, b):
            ex, ey = q[0] - r[0], q[1] - r[1]
            L = math.hypot(ex, ey)
            if L > 1e-9:
                dx, dy = dx + g * ex / L, dy + g * ey / L
        out.append((q[0] + dx, q[1] + dy))
    return out


def _area(p):
    return abs(sum(p[i][0] * p[(i + 1) % len(p)][1] - p[(i + 1) % len(p)][0] * p[i][1] for i in range(len(p)))) / 2


def _inside(p, C, c):
    """Whether point p lies inside solid cube (corners C) by more than 0.002 px on every axis."""
    if any(c.size[i] + 2 * c.inflate[i] <= 0.004 for i in range(3)):
        return False
    o = C[(0, 0, 0)]
    d = _sub(p, o)
    for i, k in enumerate(((1, 0, 0), (0, 1, 0), (0, 0, 1))):
        e = _sub(C[k], o)
        L = math.sqrt(_dot(e, e))
        t = _dot(d, e) / L
        if t < 0.002 or t > L - 0.002:
            return False
    return True


class Hit:
    """a and b: cube keys (bone#index), fa and fb their faces; sd: a's plane less b's along their shared outward normal."""
    __slots__ = ("a", "fa", "b", "fb", "area", "gap", "pose", "sd", "garea")

    def __init__(s, a, fa, b, fb, area, gap, pose, sd=0.0):
        s.a, s.fa, s.b, s.fb, s.area, s.gap, s.pose, s.sd, s.garea = a, fa, b, fb, area, gap, pose, sd, area

    def pair(s): return (s.a, s.b)

    def line(s, troop="", kind=""):
        return "%s %-14s %-22s %-3s | %-22s %-3s area %6.2f gap %.3f %s" % (
            kind, troop, s.a, s.fa, s.b, s.fb, s.area, s.gap, s.pose)


def find(model, eps=EPS, min_area=MIN_AREA, pose_list=None, hidden_too=False, grow=0.0):
    """Every visible coplanar same-way face pair of different cubes, over the poses: [Hit], one per cube pair and face pair
    (the largest overlap over the poses), in a stable order. hidden_too: keep pairs hidden inside a third cube as well.
    grow: also measure the overlap with both faces grown by this much a side (Hit.garea) and keep a pair when that one is
    over min_area (settle: two abutting cubes inflated alike overlap along their shared edge)."""
    order = {c.key: i for i, c in enumerate(model.cubes)}
    masks = {}
    for i, s in enumerate(model.sets):
        for c in model.cubes:
            if c.bone in s:
                masks[c.key] = masks.get(c.key, 0) | (1 << i)
    live = [c for c in model.cubes if c.key in masks]
    best = {}
    for label, extra in (pose_list or poses(model)):
        W = _world(model, extra)
        boxes = {}
        for c in live:
            C = W[c.key][0]
            boxes[c.key] = ([min(p[j] for p in C.values()) for j in range(3)], [max(p[j] for p in C.values()) for j in range(3)])
        # faces bucketed by direction and plane offset: only faces in one bucket or the next can be coplanar
        buckets = {}
        allf = {}
        for c in live:
            C, L = W[c.key]
            fs = _faces(c, C, L)
            allf[c.key] = (C, fs)
            for f in fs:
                nk = tuple(round(x, 3) + 0.0 for x in f[1])
                buckets.setdefault((nk, math.floor(f[2] / (2 * eps + 1e-9))), []).append((c, f))
        seen = set()
        for (nk, dk), items in buckets.items():
            cand = list(items)
            nxt = buckets.get((nk, dk + 1))
            if nxt:
                cand += nxt
            for i in range(len(items)):
                ca, fa = items[i]
                for j in range(i + 1, len(cand)):
                    cb, fb = cand[j]
                    if ca is cb:
                        continue
                    if not masks[ca.key] & masks[cb.key]:
                        continue
                    gap = abs(fa[2] - fb[2])
                    if gap > eps or _dot(fa[1], fb[1]) < 0.99999:
                        continue
                    ka, kb = (ca, fa), (cb, fb)
                    if order[cb.key] < order[ca.key]:
                        ka, kb = kb, ka
                    sig = (ka[0].key, ka[1][0], kb[0].key, kb[1][0])
                    if sig in seen:
                        continue
                    seen.add(sig)
                    (la, ha), (lb, hb) = boxes[ca.key], boxes[cb.key]
                    if any(ha[k] < lb[k] - eps or hb[k] < la[k] - eps for k in range(3)):
                        continue
                    n = ka[1][1]
                    u, v = _basis(n)
                    pa, pb = _poly(ka[1][3], u, v), _poly(kb[1][3], u, v)
                    ov = _clip(pa, pb)
                    ar = _area(ov) if len(ov) >= 3 else 0.0
                    gar = ar
                    if grow:
                        og = _clip(_grow(pa, grow), _grow(pb, grow))
                        gar = _area(og) if len(og) >= 3 else 0.0
                        if gar <= min_area:
                            continue
                        if len(ov) < 3:
                            ov = og
                    elif len(ov) < 3 or ar <= min_area:
                        continue
                    common = masks[ca.key] & masks[cb.key]
                    blockers = () if hidden_too else [c for c in live if c is not ca and c is not cb and masks[c.key] & common == common]
                    if not hidden_too and _hidden(ov, n, ka[1][2], u, v, blockers, allf, boxes):
                        continue
                    prev = best.get(sig)
                    if prev is None or (ar, gar) > (prev.area + 1e-9, prev.garea + 1e-9):
                        best[sig] = Hit(sig[0], sig[1], sig[2], sig[3], ar, gap, label, ka[1][2] - kb[1][2])
                        best[sig].garea = gar
    return sorted(best.values(), key=lambda h: (order[h.a], order[h.b], h.fa, h.fb))


def _hidden(ov, n, d0, u, v, blockers, allf, boxes):
    cx = sum(p[0] for p in ov) / len(ov)
    cy = sum(p[1] for p in ov) / len(ov)
    pts = [(cx, cy)] + [((cx + q[0]) / 2, (cy + q[1]) / 2) for q in ov]
    for x, y in pts:
        p = tuple(n[j] * (d0 + NUDGE) + u[j] * x + v[j] * y for j in range(3))
        covered = False
        for c in blockers:
            lo, hi = boxes[c.key]
            if any(p[k] < lo[k] or p[k] > hi[k] for k in range(3)):
                continue
            if _inside(p, allf[c.key][0], c):
                covered = True
                break
        if not covered:
            return False
    return True


# ---- the generators' tie-break -------------------------------------------------------------------------------
# the inflate steps settle may add to a cube, tried in this order: SEP either way first, so most pairs end SEP apart; a group of
# four or more cubes on one plane (two flat skirt panels and two greaves) also needs the half and one and a half steps. All
# stay under 0.05 px.
STEPS = (0.0, SEP, -SEP, 1.5 * SEP, -1.5 * SEP, 0.5 * SEP, -0.5 * SEP)


def settle(bones, emit, protect=(), budget=200000):
    """Moves cubes of the bones (handlib Bone / Box: box.dil is the inflate) apart until emit() -> Model has no same-way faces
    nearer than SOLVE_EPS. Each cube takes one of STEPS (0, +SEP, -SEP of inflate on its authored value; a shrink only where
    the box keeps its size, so a flat panel only grows; the finer STEPS only where those cannot do). Inflate moves every face of
    a box out along its own normal, so a same-way pair parts by the difference of their steps, and a pair farther than
    SOLVE_EPS plus twice the largest step can never come near.

    So: emit at the authored inflate, take every pair within that reach, and for each group of cubes linked by them that holds
    a near pair, search the steps (largest cube first, each trying STEPS in order) for the first choice that leaves every pair
    at least SOLVE_EPS apart: the big pieces stay put and the trim moves. A group the search cannot part within its budget
    keeps the best greedy choice. Deterministic. protect: cube keys that keep their authored inflate. Returns the pairs still
    nearer than SOLVE_EPS (the check then fails on them, unless exempt)."""
    boxes = {}
    for bn in bones:
        for i, b in enumerate(bn.boxes):
            boxes["%s#%d" % (bn.name, i)] = b
    base = {k: b.dil for k, b in boxes.items()}
    model = emit()
    reach = SOLVE_EPS + 2 * max(abs(v) for v in STEPS) + 0.001
    # hidden pairs and abutting ones too: a step can bring a hidden overlap out past the cube that hid it, and two cubes that
    # meet edge to edge on one plane overlap along that edge once both grow alike
    grow = max(abs(v) for v in STEPS)
    pairs = [h for h in find(model, eps=reach, hidden_too=True, grow=grow) if h.a in boxes and h.b in boxes]
    if not any(h.gap < SOLVE_EPS and h.area > MIN_AREA for h in pairs):
        return []
    order = {c.key: i for i, c in enumerate(model.cubes)}
    size = {c.key: 2 * (c.size[0] * c.size[1] + c.size[1] * c.size[2] + c.size[0] * c.size[2]) for c in model.cubes}
    adj = {}
    for h in pairs:
        adj.setdefault(h.a, []).append((h.b, h, 1))
        adj.setdefault(h.b, []).append((h.a, h, -1))

    def ok(h, sign, lk, lo):
        d = h.sd + sign * (STEPS[lk] - STEPS[lo])
        if abs(d) >= SOLVE_EPS - 1e-6:
            return True
        if h.area > MIN_AREA:
            return False
        # a sliver or an edge: its overlap grows with what both steps add (to garea at grow a side each)
        add = max(0.0, STEPS[lk] + STEPS[lo])
        return h.area + (h.garea - h.area) * add / (2 * grow) <= MIN_AREA

    level, done = {}, set()
    for start in sorted({k for h in pairs if h.gap < SOLVE_EPS and h.area > MIN_AREA for k in (h.a, h.b)}, key=order.get):
        if start in done:
            continue
        comp, todo = set(), [start]
        while todo:
            k = todo.pop()
            if k in comp:
                continue
            comp.add(k)
            todo += [o for o, _, _ in adj[k] if o not in comp]
        done |= comp
        seq = sorted(comp, key=lambda k: (-size[k], order[k]))
        opts = {k: [i for i in range(len(STEPS)) if i == 0 or (k not in protect and _fits(boxes[k], base[k], STEPS[i]))] for k in seq}
        got, steps = {}, [0]

        def dfs(i):
            if i == len(seq):
                return True
            steps[0] += 1
            if steps[0] > budget:
                return False
            k = seq[i]
            for lv in opts[k]:
                if all(ok(h, sg, lv, got[o]) for o, h, sg in adj[k] if o in got):
                    got[k] = lv
                    if dfs(i + 1):
                        return True
                    del got[k]
            return False

        if not dfs(0):   # greedy: each cube, in turn, the step that breaks the fewest pairs with those before it
            got = {}
            for k in seq:
                got[k] = min(opts[k], key=lambda lv: sum(not ok(h, sg, lv, got[o]) for o, h, sg in adj[k] if o in got))
        level.update(got)
    for k, lv in level.items():
        boxes[k].dil = round(base[k] + STEPS[lv], 4)
    return [h for h in find(emit(), eps=SOLVE_EPS) if h.a in boxes and h.b in boxes]


MAX_OFFSET = 0.05   # px: no box moves this far from its authored shape, counting a generator's own lift (handlib Box.lift)


def _fits(box, base, step):
    """Whether a box keeps its shape with this step on its inflate: every side over 0.2 px, a flat panel never shrunk, and
    the step plus any anti z-fighting lift the generator gave it (Box.lift: a shield rim's RIM_LIFT, a limb's inset) under
    MAX_OFFSET."""
    dil = base + step
    return (abs(getattr(box, "lift", 0.0) + step) < MAX_OFFSET - 1e-9
            and all(s + 2 * dil > 0.2 or s == 0 and dil >= 0 for s in box.size))
