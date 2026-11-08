"""Death-clip weapon polish: keeps a held weapon / shield / prop believable while its owner dies.

python art/anim/common/heldprop.py <troop> [<troop> ...] [--check]

A death clip keeps the weapon in the hand (the prop bone is a child of the hand bone), so the weapon's pose is whatever the arm
chain happens to give while the body falls, rolls and settles.  That produced: a bow or crossbow 5-13 px under the floor, flipping
180 degrees between two ticks, a trident or crossbow standing on its end after the body had landed, a lantern pole upright on the
ground, and so on.  This pass re-keys ONLY the hand bone's rotation (the wrist) in the death clips, and only where it has to:

  per half-tick the wrist may take any of ~700 candidate world rotations about the wrist pivot (the natural wrist, small twists, tip
  lifts, and 'lie the long axis along a compass heading, thin axis vertical').  A Viterbi pass picks the cheapest sequence:
    cost = floor penetration + penetration into the torso/legs/head + distance from the natural wrist
         + (after the body has landed) how far the long axis is from the floor and the thin axis from vertical
    transition = the weapon's world rotation per half tick beyond what the natural motion already did, much more after landing
  So a clip whose weapon is already fine keeps the natural wrist exactly, a violating stretch is bent away from the floor / body
  and then settles flat and still with the body.

The body, the arm chain, the clip names and lengths are untouched; the pass is idempotent on its own output.
A clip that keys a prop bone itself (the released sword / shield of the shared v4 deaths) is skipped here (see release_fix).
"""
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / "tools" / "anim"))
import render_geo as R  # noqa: E402

DT = 0.5                    # solver grid, ticks
T0 = 1.0                    # the game fades a death clip in over 2 ticks: from here the authored pose is shown
TMAX = 20.0
HANDS = ("left_hand", "right_hand")
SOLID = ("torso", "hips", "head", "left_leg", "right_leg", "left_shin", "right_shin", "left_foot", "right_foot")

W_FLOOR = 60.0     # per px of weapon below FLOOR_MARGIN
W_BODY = float(os.environ.get('HELDPROP_WBODY', 25.0))      # per px inside the torso / legs / head (beyond BODY_TOL)
W_DEV = 0.06       # per degree away from the natural wrist
W_FLAT = 0.16      # per degree off flat (after the body has landed)
W_STILL = 0.35     # per degree of weapon world rotation per half tick beyond the natural motion, once landed
W_MOVE = float(os.environ.get('HELDPROP_WMOVE', 0.05))     # quadratic, same, before landing
FLOOR_MARGIN = 0.12 / 16.0
BODY_TOL = 0.25 / 16.0
CAP_DEG = 38.0     # hard cap on the extra world rotation per half tick
ALLOW_AIR = float(os.environ.get('HELDPROP_AIR', 8.0))     # free rotation allowance per half tick while still falling
ALLOW_REST = 2.0     # ... once the body has landed


def geo_path(u):
    for p in (f"art/anim/{u}/out/preview/t3_rest.geo.json", f"art/anim/{u}/out/preview/t3.geo.json", f"art/anim/{u}/out/A/preview/t3.geo.json"):
        if (ROOT / p).exists():
            return ROOT / p
    raise FileNotFoundError(u)


def smooth(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


def rot_zyx(r):
    return (R._Rz(r[2]) @ R._Ry(r[1]) @ R._Rx(r[0]))[:3, :3]


def unit_cols(M):
    n = np.linalg.norm(M, axis=0)
    n[n < 1e-9] = 1.0
    return M / n


def euler_zyx(M):
    """x, y, z (radians) with M = Rz(z) Ry(y) Rx(x)."""
    y = -math.asin(max(-1.0, min(1.0, M[2, 0])))
    c = math.cos(y)
    if abs(c) < 1e-4:
        return None
    return math.atan2(M[2, 1], M[2, 2]), y, math.atan2(M[1, 0], M[0, 0])


def unwrap(a, ref):
    while a - ref > math.pi:
        a -= 2 * math.pi
    while a - ref < -math.pi:
        a += 2 * math.pi
    return a


def angle_between(A, B):
    """rotation angle (deg) between rotation stacks A (n,9) and B (m,9) -> (n,m)."""
    c = (A @ B.T - 1.0) / 2.0
    return np.degrees(np.arccos(np.clip(c, -1.0, 1.0)))


def axis_rot(axis, ang):
    """rotation matrices for unit axes (n,3), angles (n,) radians -> (n,3,3)."""
    a = axis / np.linalg.norm(axis, axis=-1, keepdims=True)
    x, y, z = a[:, 0], a[:, 1], a[:, 2]
    c, s = np.cos(ang), np.sin(ang)
    C = 1 - c
    out = np.empty((len(a), 3, 3))
    out[:, 0, 0] = c + x * x * C; out[:, 0, 1] = x * y * C - z * s; out[:, 0, 2] = x * z * C + y * s
    out[:, 1, 0] = y * x * C + z * s; out[:, 1, 1] = c + y * y * C; out[:, 1, 2] = y * z * C - x * s
    out[:, 2, 0] = z * x * C - y * s; out[:, 2, 1] = z * y * C + x * s; out[:, 2, 2] = c + z * z * C
    return out


def rot_between(a, b):
    """minimal rotation taking unit vector a to unit vectors b (n,3) -> (n,3,3)."""
    n = len(b)
    ax = np.cross(np.broadcast_to(a, (n, 3)), b)
    s = np.linalg.norm(ax, axis=1)
    c = b @ a
    ang = np.arctan2(s, c)
    bad = s < 1e-8
    if bad.any():
        alt = np.cross(a, np.array([1.0, 0, 0]) if abs(a[0]) < 0.9 else np.array([0, 1.0, 0]))
        ax[bad] = alt
    return axis_rot(ax, ang)


class Rig:
    """One troop's geometry + the poses of its death clips."""

    def __init__(self, troop, anim_path):
        self.troop = troop
        self.model = R.load_geo(str(geo_path(troop)))
        m = self.model
        self.anim_path = anim_path
        self.data = json.load(open(anim_path, encoding="utf-8"))
        self.sc = R.Scene(m, [], [], 0.0)
        self.clips = {}
        self.base = self._ready()
        self.hands = {}
        for h in HANDS:
            if h in m.bones:
                sub = set(m.descendants(h))
                cubes = [i for i, b in enumerate(m.cube_bone) if b in sub]
                if cubes:
                    self.hands[h] = (sub, cubes)
        allsub = set().union(*[s for s, _ in self.hands.values()]) if self.hands else set()
        self.solid = [i for i, b in enumerate(m.cube_bone) if b in SOLID and b not in allsub]
        self.trunk = [i for i, b in enumerate(m.cube_bone) if b in ("torso", "hips")]

    def clip(self, n):
        if n not in self.clips:
            self.clips[n] = R.load_clip(self.data, n)
        return self.clips[n]

    def sample(self, n, t):
        c = self.clip(n)
        et = c.effective_tick(t)
        out = {}
        if et is None:
            return out
        for b in c.touched():
            d = {}
            for ch in ("rotation", "position"):
                v = c.sample(b, ch, et)
                if v is not None:
                    d[ch] = np.asarray(v, dtype=float)
            out[b] = d
        return out

    def _ready(self):
        # the clips in self.data are the ones being polished; the ready pose is not a death clip, so it is never rewritten
        p = {}
        p.update(self.sample("idle_legs", 0.0))
        p.update(self.sample("idle_upper", 0.0))
        st = dict(self.sample("stance_ready_upper", 0.0))
        st.update(self.sample("stance_ready_legs", 0.0))
        out = dict(p)
        for b, d in st.items():
            out[b] = dict(d)
        return out

    def pose(self, name, t):
        dp = self.sample(name, min(t, self.clip(name).length))
        w = smooth(t / 2.0)
        out = {b: dict(d) for b, d in self.base.items()}
        for b, d in dp.items():
            o = dict(out.get(b, {}))
            for ch, v in d.items():
                a = o.get(ch, np.zeros(3))
                o[ch] = a + (v - a) * w
            out[b] = o
        return out

    def frame(self, name, t):
        P = self.pose(name, t)
        m = self.model
        self.sc._pose = lambda tick, P=P: {b: {ch: tuple(v) for ch, v in d.items()} for b, d in P.items() if b in m.bones}
        return self.sc.evaluate(t), P


def prop_points(model, fr, cubes):
    """8 corners + centre of each prop cube, world blocks (n,3)."""
    c = fr.corners[cubes]
    return np.concatenate([c.reshape(-1, 3), c.mean(axis=1)], axis=0)


def body_boxes(model, fr, idx):
    out = []
    for i in idx:
        Mc = fr.cube_M[i]
        cs = model.cube_list[i].corners
        R3 = Mc[:3, :3]
        out.append((np.linalg.inv(R3), Mc[:3, 3], cs.min(axis=0), cs.max(axis=0)))
    return out


def lattice(model, fr, cubes, n=3):
    """n^3 points per prop cube, world blocks."""
    g = np.linspace(0, 1, n)
    pts = []
    for i in cubes:
        cs = model.cube_list[i].corners
        lo, hi = cs.min(axis=0), cs.max(axis=0)
        P = np.array([[lo[0] + (hi[0] - lo[0]) * a, lo[1] + (hi[1] - lo[1]) * b, lo[2] + (hi[2] - lo[2]) * c] for a in g for b in g for c in g])
        Mc = fr.cube_M[i]
        pts.append(P @ Mc[:3, :3].T + Mc[:3, 3])
    return np.concatenate(pts, axis=0)


def body_depth(P, boxes):
    """deepest penetration (blocks) of any point of P (K,N,3) into any box -> (K,)"""
    K = P.shape[0]
    depth = np.zeros(K)
    for iR, tt, lo, hi in boxes:
        L = (P - tt) @ iR.T
        d = np.minimum(L - lo, hi - L).min(axis=-1)          # >0 inside
        depth = np.maximum(depth, np.where(d > 0, d, 0.0).max(axis=1))
    return depth


def axes_of(pts, pivot):
    """(long axis pointing away from the pivot, thin axis, planar?, elongated?) of a point cloud."""
    c = pts.mean(axis=0)
    w, v = np.linalg.eigh(np.cov((pts - c).T))
    s = np.sqrt(np.maximum(w, 1e-12))
    a = v[:, 2]
    if np.dot(a, c - pivot) < 0:
        a = -a
    return a, v[:, 0], s[0] < 0.55 * s[1], s[2] > 1.5 * s[1]


def candidates(a0, t0, planar, elong, rolls=True):
    """world rotations (K,3,3) applied about the wrist pivot; index 0 is the identity."""
    Q = [np.eye(3)[None]]
    # small twists about 14 directions
    k = np.arange(14) + 0.5
    phi = math.pi * (3 - math.sqrt(5)) * k
    z = 1 - 2 * k / 14
    r = np.sqrt(1 - z * z)
    axes = np.stack([r * np.cos(phi), z, r * np.sin(phi)], axis=1)
    for ang in (4, 9, 16, 26, 40):
        Q.append(axis_rot(axes, np.full(len(axes), math.radians(ang))))
    # tip lift / drop about the horizontal axis perpendicular to the long axis
    side = np.cross(a0, [0, 1, 0])
    if np.linalg.norm(side) > 1e-3:
        side = side / np.linalg.norm(side)
        angs = np.radians(np.arange(-60, 61, 5))
        angs = angs[np.abs(angs) > 1e-6]
        Q.append(axis_rot(np.tile(side, (len(angs), 1)), angs))
    if elong or planar:
        az = np.radians(np.arange(0, 360, 15))
        for el in (-12, 0, 12, 32, 58):
            e = math.radians(el)
            d = np.stack([np.cos(e) * np.cos(az), np.full_like(az, math.sin(e)), np.cos(e) * np.sin(az)], axis=1)
            Q1 = rot_between(a0, d)
            Qs = [Q1]
            if planar and rolls:
                t1 = Q1 @ t0
                up = np.array([0, 1.0, 0])
                for sgn in (1.0, -1.0):
                    ut = up * sgn
                    u_perp = ut - (d @ ut)[:, None] * d
                    t_perp = t1 - np.einsum("ij,ij->i", t1, d)[:, None] * d
                    nu = np.linalg.norm(u_perp, axis=1)
                    nt = np.linalg.norm(t_perp, axis=1)
                    ok = (nu > 1e-3) & (nt > 1e-3)
                    ang = np.arctan2(np.einsum("ij,ij->i", np.cross(t_perp, u_perp), d), np.einsum("ij,ij->i", t_perp, u_perp))
                    ang = np.where(ok, ang, 0.0)
                    Qs.append(axis_rot(d, ang) @ Q1)
            for q in Qs:
                Q.append(q)
                # half-way blends (identity -> q) so the sequence can ramp
                ax = np.zeros((len(q), 3))
                tr = np.clip((np.einsum("kii->k", q) - 1) / 2, -1, 1)
                ang = np.arccos(tr)
                ax[:, 0] = q[:, 2, 1] - q[:, 1, 2]
                ax[:, 1] = q[:, 0, 2] - q[:, 2, 0]
                ax[:, 2] = q[:, 1, 0] - q[:, 0, 1]
                nrm = np.linalg.norm(ax, axis=1)
                good = nrm > 1e-6
                if good.any():
                    Q.append(axis_rot(ax[good], ang[good] * 0.5))
    return np.concatenate(Q, axis=0)


def clip_violations(rig, name, hand):
    """cheap summary of the natural clip: (worst floor penetration px, worst body penetration px)."""
    sub, cubes = rig.hands[hand]
    wf = 0.0
    wb = 0.0
    for t in np.arange(T0, TMAX + 1e-6, DT):
        fr, _ = rig.frame(name, float(t))
        pts = prop_points(rig.model, fr, cubes)
        wf = max(wf, -pts[:, 1].min() * 16)
    return wf, wb



PIN_TOL = 1.5       # px: a held prop that still travels more than this after the body has landed is let go
PIN_G = 1.0         # px per tick^2: the same stylised gravity as the shared deaths
PIN_VH = float(os.environ.get('HELDPROP_PINVH', 3.5))        # px/tick: the fastest a released prop leaves the hand sideways
PIN_VDOWN = 2.5     # px/tick: ... and downwards
PIN_REST = 0.3      # bounce restitution
PIN_REACH = 26.0    # px: how far sideways it may be set down to clear the body
PIN_SETTLE = 1.0    # ticks the released prop takes to sink the last bit to the floor


PIN_TPLOW = float(os.environ.get('HELDPROP_TPLOW', 4.0))   # px: the prop is let go at the landing once it is this close to the floor
PIN_TURN = 22.0     # deg per half tick: a prop still turning faster than this once the body has landed is let go


def kabsch_deg(P, Q):
    """Angle (deg) of the best rigid rotation taking point set P to Q."""
    A = (P - P.mean(axis=0)).T @ (Q - Q.mean(axis=0))
    U, _, Vt = np.linalg.svd(A)
    Rm = Vt.T @ np.diag([1.0, 1.0, np.sign(np.linalg.det(Vt.T @ U.T))]) @ U.T
    return math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(Rm) - 1.0) / 2.0))))


def pin_children(rig, hand):
    """prop bones: the hand's direct children that carry cubes."""
    m = rig.model
    out = []
    for b, bone in m.bones.items():
        if bone.parent == hand:
            sub = set(m.descendants(b))
            cubes = [i for i, cb in enumerate(m.cube_bone) if cb in sub]
            if cubes:
                out.append((b, sub, cubes))
    return out


def _pin_euler(Rm, rest, near):
    """file degrees (x,y,z) with Rz Ry Rx (rest + file) = Rm, on the branch nearest `near` (file degrees)."""
    e = euler_zyx(Rm)
    if e is None:
        return tuple(near)
    x, y, z = e
    nx = math.radians(-near[0]) + rest[0]
    nz = math.radians(near[2]) + rest[2]
    x, z = unwrap(x, nx), unwrap(z, nz)
    return (-math.degrees(x - rest[0]), -math.degrees(y - rest[1]), math.degrees(z - rest[2]))


def rot_slerp_frac(Rm, f):
    """the rotation f of the way from identity to Rm."""
    c = (np.trace(Rm) - 1.0) / 2.0
    ang = math.acos(max(-1.0, min(1.0, c)))
    if ang < 1e-6:
        return np.eye(3)
    ax = np.array([Rm[2, 1] - Rm[1, 2], Rm[0, 2] - Rm[2, 0], Rm[1, 0] - Rm[0, 1]])
    n = np.linalg.norm(ax)
    if n < 1e-9:
        return np.eye(3)
    return axis_rot((ax / n)[None, :], np.array([ang * f]))[0]


LIFT_MARGIN = 0.1   # px: a held prop that dips below the floor while the body is still falling is raised by what it is short

def early_lift_keys(rig, hand, kids, frames, pts, ts, tp, tmax=1e9, zero=True):
    """Before the pin (or alone): where the carried prop dips under the floor, raise it in world space by exactly the
    shortfall.  Returns {bone: (keys_r, keys_p)} (empty when nothing dips)."""
    need = {}
    for i in range(min(tp, len(ts))):
        if float(ts[i]) >= tmax:
            break
        low = float(pts[i][:, 1].min()) * 16
        if low < -1e-3:
            u = min(1.0, float(ts[i]) / 2.0)
            w = max(0.25, u * u * (3.0 - 2.0 * u))       # the game fades a death clip in over 2 ticks: only w of the lift shows
            need[i] = (-low + LIFT_MARGIN) / w
    if not need:
        return {}
    m = rig.model
    idx = sorted({j for i in need for j in (i - 1, i, i + 1) if 0 <= j < min(tp, len(ts)) and float(ts[j]) < tmax})
    out = {}
    for b, sub, cu in kids:
        bone = m.bones[b]
        keys_r, keys_p = ({"0": [0.0, 0.0, 0.0]}, {"0": [0.0, 0.0, 0.0]}) if zero else ({}, {})
        near = (0.0, 0.0, 0.0)
        for i in idx:
            W = frames[i].bone_M[b].copy()
            W[1, 3] += need.get(i, 0.0) / 16.0
            L = np.linalg.inv(frames[i].bone_M[hand]) @ W
            rot = _pin_euler(unit_cols(L[:3, :3]), bone.rot, near)
            near = rot
            Rb = rot_zyx(np.asarray(bone.rot, float) + np.radians([-rot[0], -rot[1], rot[2]]))
            pv = bone.pivot / 16.0
            q = L[:3, 3] - pv + Rb @ pv
            t = float(ts[i])
            keys_r[key_t(t)] = fmt(rot)
            keys_p[key_t(t)] = fmt((-q[0] * 16.0, q[1] * 16.0, q[2] * 16.0))
        out[b] = (keys_r, keys_p)
    return out


def pin_hand(rig, name, hand, verbose=True):
    """Where the hand keeps carrying the prop (sliding / turning) after the body has landed, let the prop go instead:
    from the landing on, key the prop bone(s) so their WORLD pose stays where it lay, sunk to the floor."""
    kids = pin_children(rig, hand)
    c = rig.data["animations"][name]
    if not kids:
        return None
    m = rig.model
    ts = np.arange(T0, TMAX + 1e-6, DT)
    frames = [rig.frame(name, float(t))[0] for t in ts]
    if any(k in c["bones"] for k, _, _ in kids):
        # released gear (shared deaths): only lift it while it is still carried, i.e. before its first key
        firsts = []
        for k, _, _ in kids:
            if k in c["bones"]:
                for ch in c["bones"][k].values():
                    ts_k = [float(x) * 20.0 for x in ch.keys() if float(x) > 1e-9]
                    if ts_k:
                        firsts.append(min(ts_k))
        if not firsts:
            return None
        tmax = min(firsts) - 0.01
        allc = sorted({i for _, _, cu in kids for i in cu})
        pts = [prop_points(m, f, allc) for f in frames]
        ek = early_lift_keys(rig, hand, [k for k in kids if k[0] in c["bones"]], frames, pts, ts, len(ts), tmax=tmax, zero=False)
        if not ek:
            return None
        for b, (kr, kp) in ek.items():
            for chn, kk in (("rotation", kr), ("position", kp)):
                tgt = c["bones"][b].setdefault(chn, {})
                for tk, val in kk.items():
                    tgt[tk] = val
            for chn in ("rotation", "position"):                    # keep keys in time order
                c["bones"][b][chn] = dict(sorted(c["bones"][b][chn].items(), key=lambda kv: float(kv[0])))
        rig.clips.pop(name, None)
        if verbose:
            print(f"{rig.troop:14s} {name:14s} {hand:10s} LIFT carried gear ({len(ek)} bone) before t={tmax:.2f}", flush=True)
        return {b: (0.0, 0.0) for b in ek}
    lows = np.array([f.corners[rig.trunk].reshape(-1, 3)[:, 1].min() * 16 for f in frames])
    rest_low = float(lows[-1])
    tl = float(TMAX)
    for i, t in enumerate(ts):
        if t >= 3.0 and lows[i] <= rest_low + 1.5:
            tl = float(t)
            break
    allc = sorted({i for _, _, cu in kids for i in cu})
    pts = [prop_points(m, f, allc) for f in frames]
    dev = np.array([np.abs(p - pts[-1]).max() * 16 for p in pts])
    late = [i for i, t in enumerate(ts) if t >= tl + 2.0 and dev[i] > PIN_TOL]
    if not late:
        # a prop that is still carried through a violent turn after the landing is let go as well
        for i, t in enumerate(ts[:-1]):
            if t >= tl and kabsch_deg(pts[i], pts[i + 1]) > PIN_TURN:
                late = [i]
                break
    if not late:
        ek = early_lift_keys(rig, hand, kids, frames, pts, ts, len(ts))
        if not ek:
            return None
        for b, (kr, kp) in ek.items():
            c["bones"][b] = {"rotation": kr, "position": kp}
        rig.clips.pop(name, None)
        if verbose:
            print(f"{rig.troop:14s} {name:14s} {hand:10s} LIFT only ({len(ek)} bone)", flush=True)
        return {b: (0.0, 0.0) for b in ek}
    tp = None
    for i, t in enumerate(ts):
        if t >= 2.5 and t < tl and pts[i][:, 1].min() * 16 <= 1.5 and pts[i + 1][:, 1].min() * 16 <= 1.5:
            tp = i                               # it is already on the floor: leave it there
            break
        if t >= tl and pts[i][:, 1].min() * 16 <= PIN_TPLOW:
            tp = i
            break
        if t >= tl + 1.5:
            tp = i
            break
    Lat0 = lattice(m, frames[tp], allc, 3)
    cen0 = Lat0.mean(axis=0)
    piv0 = (frames[tp].bone_M[hand] @ np.append(m.bones[hand].pivot / 16.0, 1.0))[:3]
    a0, th0, planar0, elong0 = axes_of(Lat0, piv0)
    Rf = np.eye(3)
    if elong0:
        el = math.degrees(math.asin(min(1.0, abs(a0[1]))))
        if el > 4.0:
            h = np.cross(a0, np.array([0.0, 1.0, 0.0]))
            tgt = np.array([a0[0], 0.0, a0[2]])
            if np.linalg.norm(tgt) > 1e-6:
                Rf = rot_between(a0, (tgt / np.linalg.norm(tgt))[None, :])[0]
    elif planar0:
        el = math.degrees(math.acos(min(1.0, abs(th0[1]))))
        if el > 4.0:
            tt = th0 if th0[1] >= 0 else -th0
            Rf = rot_between(tt, np.array([[0.0, 1.0, 0.0]]))[0]
    def about(R3):
        M = np.eye(4)
        M[:3, :3] = R3
        M[:3, 3] = cen0 - R3 @ cen0
        return M
    Lf = (Lat0 - cen0) @ Rf.T + cen0
    drop = max(0.0, float(Lf[:, 1].min()) * 16)          # px the flattened prop sinks to reach the floor
    # release velocity (px / tick) from the prop's own motion, tamed
    c_now = pts[tp].mean(axis=0) * 16
    c_prev = pts[max(tp - 1, 0)].mean(axis=0) * 16
    v = (c_now - c_prev) / DT
    vh = np.array([v[0], v[2]])
    nv = float(np.linalg.norm(vh))
    if nv > PIN_VH:
        vh = vh * (PIN_VH / nv)
    vy = float(min(max(v[1], -PIN_VDOWN), 1.0))
    # ticks to reach the floor under gravity
    disc = vy * vy + 2.0 * PIN_G * drop
    tfall = (vy + math.sqrt(disc)) / PIN_G if drop > 1e-6 else 0.0
    vimp = math.sqrt(disc) if drop > 1e-6 else 0.0
    # where it lies: the smallest sideways move that keeps it out of the body for the rest of the clip
    idx = list(range(tp, len(ts), 2))
    Lat = Lf
    grid = np.arange(-PIN_REACH, PIN_REACH + 1e-6, 2.0)
    Th = max(tfall, 1.5)
    d0 = 0.5 * vh * Th
    offs = np.array([(dx + d0[0], dz + d0[1]) for dx in grid for dz in grid])
    bad = np.zeros(len(offs))
    for i in idx:
        boxes = body_boxes(m, frames[i], rig.solid)
        sink = drop if (float(ts[i]) - float(ts[tp])) >= tfall else min(drop, max(0.0, -(vy * (float(ts[i]) - float(ts[tp])) - 0.5 * PIN_G * (float(ts[i]) - float(ts[tp])) ** 2)))
        P = Lat[None, :, :] + np.stack([offs[:, 0] / 16.0, np.full(len(offs), -sink / 16.0), offs[:, 1] / 16.0], axis=1)[:, None, :]
        bad = np.maximum(bad, body_depth(P, boxes) * 16)
    cost = 40.0 * np.maximum(bad - 0.3, 0.0) + 0.15 * np.linalg.norm(offs - d0, axis=1)
    off = offs[int(np.argmin(cost))]
    res = {}
    ek = early_lift_keys(rig, hand, kids, frames, pts, ts, tp)
    for b, sub, cu in kids:
        bone = m.bones[b]
        W0 = frames[tp].bone_M[b].copy()
        keys_r, keys_p = {"0": [0.0, 0.0, 0.0]}, {"0": [0.0, 0.0, 0.0]}
        if b in ek:
            keys_r.update(ek[b][0]); keys_p.update(ek[b][1])
        near = (0.0, 0.0, 0.0)
        for i in range(tp, len(ts)):
            t = float(ts[i])
            dt = t - float(ts[tp])
            u = min(1.0, dt / Th)
            h00, h10, h01, h11 = 2*u**3 - 3*u**2 + 1, u**3 - 2*u**2 + u, -2*u**3 + 3*u**2, u**3 - u**2
            hx = h10 * vh[0] * Th + h01 * off[0]
            hz = h10 * vh[1] * Th + h01 * off[1]
            if dt >= tfall:
                y = -drop
                if vimp > 1.5:
                    vb = PIN_REST * vimp
                    d2 = dt - tfall
                    y += max(0.0, vb * d2 - 0.5 * PIN_G * d2 * d2)
            else:
                y = max(-drop, vy * dt - 0.5 * PIN_G * dt * dt)
            ur = smooth(dt / Th)
            Rt = rot_slerp_frac(Rf, ur)
            Wrot = about(Rt) @ W0
            lowp = float(((Lat0 - cen0) @ Rt.T + cen0)[:, 1].min()) * 16
            W = Wrot.copy()
            W[0, 3] += hx / 16.0
            W[2, 3] += hz / 16.0
            # y is the sink of the flat pose below its start; the turning pose is kept clear of the floor
            W[1, 3] += y / 16.0
            low_now = lowp + y
            if low_now < 0.0:
                W[1, 3] += -low_now / 16.0
            L = np.linalg.inv(frames[i].bone_M[hand]) @ W
            Rm = unit_cols(L[:3, :3])
            rot = _pin_euler(Rm, bone.rot, near)
            near = rot
            Rb = rot_zyx(np.asarray(bone.rot, float) + np.radians([-rot[0], -rot[1], rot[2]]))
            pv = bone.pivot / 16.0
            q = L[:3, 3] - pv + Rb @ pv
            keys_r[key_t(t)] = fmt(rot)
            keys_p[key_t(t)] = fmt((-q[0] * 16.0, q[1] * 16.0, q[2] * 16.0))
        c["bones"][b] = {"rotation": keys_r, "position": keys_p}
        res[b] = (float(ts[tp]), drop)
    rig.clips.pop(name, None)
    if verbose:
        print(f"{rig.troop:14s} {name:14s} {hand:10s} PIN at t={ts[tp]:.1f} (landed {tl:.1f}) sink {drop:.1f}px off ({off[0]:.0f},{off[1]:.0f}) v ({vh[0]:.1f},{vy:.1f},{vh[1]:.1f}) late-dev {dev[late].max():.1f}px", flush=True)
    return res


REFINE = 3


def candidates_axes():
    k = np.arange(14) + 0.5
    phi = math.pi * (3 - math.sqrt(5)) * k
    z = 1 - 2 * k / 14
    r = np.sqrt(1 - z * z)
    return np.stack([r * np.cos(phi), z, r * np.sin(phi)], axis=1)


def slerp_mid(A, B):
    """rotation half way between rotation matrices A and B."""
    R = B @ A.T
    tr = np.clip((np.trace(R) - 1) / 2, -1, 1)
    ang = math.acos(tr)
    ax = np.array([R[2, 1] - R[1, 2], R[0, 2] - R[2, 0], R[1, 0] - R[0, 1]])
    nr = np.linalg.norm(ax)
    if nr < 1e-8:
        return A
    return axis_rot(ax[None] / nr, np.array([ang * 0.5]))[0] @ A


def solve(rig, name, hand, log=None):
    m = rig.model
    sub, cubes = rig.hands[hand]
    ticks = np.arange(T0, TMAX + 1e-6, DT)
    n = len(ticks)
    # landing time
    tl = None
    lows = []
    cens = []
    frames = []
    for t in np.arange(0.0, TMAX + 1e-6, DT):
        fr, P = rig.frame(name, float(t))
        tr = fr.corners[rig.trunk].reshape(-1, 3)
        lows.append(tr[:, 1].min() * 16)
        cens.append(tr.mean(axis=0) * 16)
        if t >= T0:
            frames.append((fr, P))
    lows = np.array(lows)
    rest_low = float(lows[-1])
    for i, t in enumerate(np.arange(0.0, TMAX + 1e-6, DT)):
        if t >= 3.0 and lows[i] <= rest_low + 1.5:
            tl = float(t)
            break
    if tl is None:
        tl = float(TMAX)
    b = m.bones[hand]
    pre = []
    for i, (fr, P) in enumerate(frames):
        t = float(ticks[i])
        lat = lattice(m, fr, cubes, 3)
        piv = (fr.bone_M[hand] @ np.append(b.pivot / 16.0, 1.0))[:3]
        a0, t0, planar, elong = axes_of(lat, piv)
        ramp = smooth((t - tl + 0.5) / 2.0)
        pre.append(dict(lat=lat, piv=piv, a0=a0, t0=t0, planar=planar, elong=elong, ramp=ramp,
                        boxes=body_boxes(m, fr, rig.solid), Rh=unit_cols(fr.bone_M[hand][:3, :3])))

    def cost(i, Q):
        c = pre[i]
        PP = (c["lat"] - c["piv"])[None] @ np.transpose(Q, (0, 2, 1)) + c["piv"]
        low = PP[:, :, 1].min(axis=1)
        floor_pen = np.maximum(0.0, FLOOR_MARGIN - low) * 16
        floor_pen = np.where(low < 0, floor_pen, floor_pen * 0.35)
        depth = np.maximum(0.0, body_depth(PP, c["boxes"]) - BODY_TOL) * 16
        angle = np.degrees(np.arccos(np.clip((np.einsum("kii->k", Q) - 1) / 2, -1, 1)))
        a1 = Q @ c["a0"]
        t1 = Q @ c["t0"]
        elev = np.degrees(np.arcsin(np.clip(np.abs(a1[:, 1]), 0, 1))) if c["elong"] else np.zeros(len(Q))
        thin = np.degrees(np.arccos(np.clip(np.abs(t1[:, 1]), 0, 1))) if c["planar"] else np.zeros(len(Q))
        near = low * 16 < 7.0
        flat = W_FLAT * (elev + thin) * c["ramp"] * np.where(near, 1.0, 0.35)
        return W_FLOOR * floor_pen + W_BODY * depth + W_DEV * angle + flat

    def dp(Qs_list, U_list):
        V = U_list[0].copy()
        V[1:] = 1e9
        back = []
        Rf_prev = (Qs_list[0] @ pre[0]["Rh"]).reshape(len(Qs_list[0]), 9)
        for i in range(1, n):
            Rf = (Qs_list[i] @ pre[i]["Rh"]).reshape(len(Qs_list[i]), 9)
            d = angle_between(Rf_prev, Rf)
            ramp = pre[i]["ramp"]
            ex = np.maximum(0.0, d - (ALLOW_AIR * (1 - ramp) + ALLOW_REST * ramp))
            T = (W_MOVE * (1 - ramp) + W_STILL * ramp) * ex + 0.5 * W_MOVE * ex * ex * (1 - ramp)
            T = T + np.where(ex > CAP_DEG, 1e4, 0.0)
            tot = V[:, None] + T
            k = tot.argmin(axis=0)
            V = tot[k, np.arange(tot.shape[1])] + U_list[i]
            back.append(k)
            Rf_prev = Rf
        j = int(V.argmin())
        seq = [j]
        for k in reversed(back):
            j = int(k[j])
            seq.append(j)
        seq.reverse()
        return seq, float(V.min())

    Qs_list = [candidates(c["a0"], c["t0"], c["planar"], c["elong"]) for c in pre]
    U_list = [cost(i, Qs_list[i]) for i in range(n)]
    seq, best = dp(Qs_list, U_list)
    rng_axes = candidates_axes()
    for it in range(REFINE):
        Wsel = [Qs_list[i][seq[i]] @ pre[i]["Rh"] for i in range(n)]
        newQ = []
        for i in range(n):
            ex = [Qs_list[i]]
            Rh = pre[i]["Rh"]
            tg = []
            for j in (i - 2, i - 1, i + 1, i + 2):
                if 0 <= j < n:
                    tg.append(Wsel[j])
            if 0 < i < n - 1:
                tg.append(slerp_mid(Wsel[i - 1], Wsel[i + 1]))
            if tg:
                ex.append(np.array([W @ Rh.T for W in tg]))
            qi = Qs_list[i][seq[i]]
            for ang in (2.0, 5.0, 10.0):
                ex.append(axis_rot(rng_axes, np.full(len(rng_axes), math.radians(ang))) @ qi)
            newQ.append(np.concatenate(ex, axis=0))
        # keep the current selection addressable: it is inside Qs_list[i] (first block)
        old_seq = seq
        Qs_list = newQ
        U_list = [cost(i, Qs_list[i]) for i in range(n)]
        seq, best = dp(Qs_list, U_list)
    Qsel = [Qs_list[i][seq[i]] for i in range(n)]
    ctx = [(fr, P, pre[i]["ramp"]) for i, (fr, P) in enumerate(frames)]
    stats = dict(tl=tl, changed=sum(1 for q in Qsel if np.abs(q - np.eye(3)).max() > 1e-6), cost=best)
    return ticks, Qsel, ctx, stats


def to_keys(rig, hand, ticks, Qsel, ctx):
    """new clip-space (file degrees) wrist rotation for each solver tick."""
    m = rig.model
    b = m.bones[hand]
    parent = b.parent
    out = []
    prev = None
    for i, t in enumerate(ticks):
        fr, P, _ = ctx[i]
        Rp = unit_cols(fr.bone_M[parent][:3, :3]) if parent else np.eye(3)
        rot = np.asarray(b.rot, float) + np.asarray(P.get(hand, {}).get("rotation", np.zeros(3)), float)
        Rl = rot_zyx(rot)
        Mn = Rp.T @ Qsel[i] @ Rp @ Rl
        e = euler_zyx(Mn)
        if e is None:
            e = tuple(rot)
        x, y, z = e
        if prev is not None:
            x, z = unwrap(x, prev[0]), unwrap(z, prev[2])
        else:
            x, z = unwrap(x, rot[0]), unwrap(z, rot[2])
        prev = (x, y, z)
        fx, fy, fz = -math.degrees(x - b.rot[0]), -math.degrees(y - b.rot[1]), math.degrees(z - b.rot[2])
        out.append((float(t), (fx, fy, fz)))
    return out


def fmt(v):
    return [round(float(x), 3) + 0.0 for x in v]


def key_t(tick):
    return ("%.4f" % (tick * 0.05)).rstrip("0").rstrip(".")


def apply_keys(data, name, hand, keyed, changed_idx, tick_list):
    """replace the hand rotation keys over the changed span (+1 grid tick either side) with the solved ones."""
    bone = data["animations"][name]["bones"][hand]
    old = bone["rotation"]
    lo = max(0, changed_idx[0] - 1)
    hi = min(len(keyed) - 1, changed_idx[-1] + 1)
    t_lo, t_hi = keyed[lo][0] * 0.05, keyed[hi][0] * 0.05
    new = {}
    for k, v in old.items():
        tt = float(k)
        if tt < t_lo - 1e-6:
            new[k] = v
    for i in range(lo, hi + 1):
        new[key_t(keyed[i][0])] = fmt(keyed[i][1])
    for k, v in old.items():
        tt = float(k)
        if tt > t_hi + 1e-6:
            new[k] = v
    bone["rotation"] = dict(sorted(new.items(), key=lambda kv: float(kv[0])))


def fix_troop(troop, anim_path=None, write=True, verbose=True):
    anim_path = anim_path or str(ROOT / f"art/anim/{troop}/out/{troop}.animation.json")
    rig = Rig(troop, anim_path)
    names = [n for n in rig.data["animations"] if n.startswith("death_")]
    report = {}
    for name in ([] if os.environ.get("HELDPROP_ONLYPIN") == "1" else names):
        c = rig.data["animations"][name]
        for hand in rig.hands:
            sub, cubes = rig.hands[hand]
            if any(bn in c["bones"] for bn in sub if bn != hand):
                report[(name, hand)] = "skipped (props keyed)"
                continue
            if hand not in c["bones"] or "rotation" not in c["bones"][hand]:
                continue
            ticks, Qsel, ctx, stats = solve(rig, name, hand)
            keyed = to_keys(rig, hand, ticks, Qsel, ctx)
            idx = [i for i, Q in enumerate(Qsel) if np.abs(Q - np.eye(3)).max() > 1e-6]
            report[(name, hand)] = stats
            if verbose:
                print(f"{troop:14s} {name:14s} {hand:10s} tl={stats['tl']:4.1f} changed {len(idx):2d}/{len(ticks)} cost={stats['cost']:.1f}", flush=True)
            if idx:
                apply_keys(rig.data, name, hand, keyed, idx, ticks)
                rig.clips.pop(name, None)       # later hands of this clip see the new wrist
    if os.environ.get("HELDPROP_PIN", "1") == "1":
        for name in names:
            for hand in rig.hands:
                pin_hand(rig, name, hand, verbose)
    if write:
        with open(anim_path, "w", encoding="utf-8") as fh:
            json.dump(rig.data, fh, indent=2)
            fh.write("\n")
    return report


if __name__ == "__main__":
    argv = sys.argv[1:]
    scratch = None
    if "--scratch" in argv:
        k = argv.index("--scratch")
        scratch = argv[k + 1]
        del argv[k:k + 2]
    args = [a for a in argv if not a.startswith("--")]
    for u in args:
        fix_troop(u, anim_path=scratch, write="--check" not in argv)
