"""The hanging lantern's swing (Kort 2026-11-09, on the gravecaller's blight: "the lantern is really stiff, give it some physics,
some life, don't make it so stiff"), baked into every clip of a troop whose held part hangs (troop_rig.SWING: the hexer).

troop_rig splits each hanging part: the crook stays on <part> in the fist, the lantern (chain, cap, glass, base, drop) hangs from
<part>_hang, a bone at the crook's tip. This pass keys <part>_hang in every clip that moves the arm, from the motion the clip gives
the hang point: a damped pendulum, simulated in world space and baked to rotation keys.

  the lantern   a mass L px below the hang point (L: the hang point to the lantern's centre), drawn toward where the clip carries
                it rigidly (its authored hang, so every authored pose and its clearance stay the rest of the swing) by a spring
                of F0 Hz, damped ZETA; the hang point's world acceleration (hand, forearm, torso, hips, all of it) throws it
                about by GAIN of what a free pendulum would feel, so it lags a fast move, overshoots and settles
  the cap       the swing is soft-capped at CAP degrees (tanh), so it reads alive, not floppy
  loops         simulated until steady (the periodic response), so the loop closes; clips a second or longer also get a
                breath of hand sway (SWAY degrees at a loop harmonic), so even a still guard keeps the lantern moving
  once clips    start at rest on the clip's first pose (the controller fades them in from whatever the lantern did before)
  additive      the hit layer and the stun shake-off add their keys to the pose under them (HitController.add, StanceController
                .addClip): their swing is the response to the clip's offsets over the guard and is faded to zero by the end
  deaths        the swing fades out by the time the dead hand lets the lantern go (its tier-3 part's first drop key), or the end
  clearance     at every sample the swung lantern is tested against the body's boxes and the floor; where it would sink in
                deeper than the rigid lantern does, the swing there is scaled back (smoothly) until it does not

Clips are matched with the clip that plays beside them in game (an upper with its _legs, a stance upper with the gait's legs
when it has none of its own), so the hips' bob and the stride throw the lantern too. Legs clips get no keys: the upper owns the
lantern (one writer per bone and layer).

    python art/anim/common/hang_swing.py <troop> [animation.json ...]   (default: the troop's out/ and asset clip files)
    python art/anim/common/hang_swing.py <troop> --report                 (amplitudes per clip, nothing written)

Re-key after any splice into the troop's clip file: build_caster_anims.py and stances.merge (stances, charge, reactions) call
key_doc themselves.
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / "tools" / "anim"))
sys.path.insert(0, str(HERE))
import render_geo as rg  # noqa: E402

ASSET_ANIMS = ROOT / "src/main/resources/assets/gravesandglory/animations/troop"
TPS, SUB = 20.0, 4          # samples per tick, as the builders bake
SUBSTEPS = 12               # integration steps per sample
F0 = 1.3                    # Hz: the lantern's swing on its chain (a 5 px pendulum alone would be ~1 Hz; the bail stiffens it)
ZETA = 0.24                 # damping ratio: a fast move overshoots about 45% and settles in two or three swings
DROOP = 0.4                 # how far the settled lantern hangs toward straight down from its rigid hang (1 = plumb)
GAIN = 0.22                 # share of the hang point's acceleration the lantern feels (1 = a free pendulum: far too floppy)
CAP = 36.0                  # degrees: the soft cap on the swing (tanh), absolute clips
CAP_ADD = 22.0              # degrees: the cap for additive clips (the heavy hit scales them by 1.55)
SWAY = (2.6, 1.8)           # degrees of hand sway (pitch, roll) breathed into loops of a second or more
SWAY_MIN_LEN = 1.0          # seconds
SETTLE = 6.0                # seconds of loop run-in before the kept cycle
MARGIN = 0.15               # px: allowed extra sinking (over the rigid lantern) into a body box
ROT_TOL = 0.05              # degrees: thinning tolerance of the baked keys


def additive(name):
    return name.startswith("hit_") or name == "stun_out_upper"


def is_legs(name):
    return name.endswith("_legs")


class Troop:
    def __init__(self, tid):
        sys.path.insert(0, str(HERE))
        import troop_rig
        self.tid = tid
        self.spec = troop_rig.SWING[tid]
        self.geo_path = HERE.parent / tid / "out" / f"{tid}.geo.json"
        self.model = rg.load_geo(self.geo_path)
        self.hangs = [p + "_hang" for p in self.spec["parts"]]
        missing = [h for h in self.hangs if h not in self.model.bones]
        if missing:
            raise SystemExit(f"{self.geo_path} has no {', '.join(missing)}: run troop_rig.py {tid} first")
        self.ref = self.hangs[-1]                     # the top tier's lantern: the most boxes (its drop under the base)
        self.part_ref = self.spec["parts"][-1]
        b = self.model.bones[self.ref]
        self.pivot = b.pivot / 16.0                   # gecko space, blocks
        pts = []
        for c in b.cubes:                             # the lantern's probe points: each box's corners, edge middles and centre
            lo, hi = c.corners.min(0), c.corners.max(0)
            for fx in (0, 0.5, 1):
                for fy in (0, 0.5, 1):
                    for fz in (0, 0.5, 1):
                        pts.append(lo + (hi - lo) * np.array([fx, fy, fz]))
        self.probe = np.array(pts)                    # gecko space, blocks (rest)
        vol = [np.prod(c.corners.max(0) - c.corners.min(0)) for c in b.cubes]
        cen = sum(v * (c.corners.max(0) + c.corners.min(0)) / 2 for v, c in zip(vol, b.cubes)) / sum(vol)
        self.arm = cen - self.pivot                   # gecko space, blocks: the hang point to the lantern's centre
        self.L = float(np.linalg.norm(self.arm)) * 16.0
        self.down = self.arm / np.linalg.norm(self.arm)
        # the body's boxes the lantern must stay out of: everything but the hanging parts themselves and the crook's tiers
        skip = set(self.spec["parts"]) | set(self.hangs)
        self.body = [i for i, n in enumerate(self.model.cube_bone) if n not in skip]

    # -------------------------------------------------------------------------------------------- poses

    def scene(self):
        return rg.Scene(self.model)

    def frames(self, poses):
        """World bone matrices and cube corners for each pose {bone: {channel: internal value}}."""
        sc = self.scene()
        out = []
        for p in poses:
            sc._pose = lambda tick, p=p: p
            fr = sc.evaluate(0)
            out.append(fr)
        return out


def _clip(doc, name):
    return rg.load_clip(doc, name) if name in doc["animations"] else None


def _sample(clip, tick, bones=None):
    out = {}
    if clip is None:
        return out
    for b in (bones or clip.touched()):
        d = {}
        for ch in rg.Clip.CHANNELS:
            v = clip.sample(b, ch, tick)
            if v is not None:
                d[ch] = np.array(v, float)
        if d:
            out[b] = d
    return out


def _merge(*poses, add=False):
    """Poses over one another: later ones replace, or (add) add to, what is under them."""
    out = {}
    for p in poses:
        for b, d in p.items():
            cur = dict(out.get(b, {}))
            for ch, v in d.items():
                if add and ch in cur and ch != "scale":
                    cur[ch] = cur[ch] + v
                elif add and ch not in cur:
                    cur[ch] = v
                else:
                    cur[ch] = v
            out[b] = cur
    return out


def partner(doc, name):
    """The clip that plays beside `name` in game, for the hips under it: (clip name, plays in step)."""
    A = doc["animations"]
    if name.endswith("_upper"):
        legs = name[:-6] + "_legs"
        if legs in A:
            return legs, True
        if name == "charge_run_upper":
            return "run_legs", True
        return ("idle_legs" if "idle_legs" in A else None), False
    if name + "_legs" in A:
        return name + "_legs", True
    return None, False


def base_pose(doc):
    """The guard the additive clips play over: the idle's first pose."""
    return _merge(_sample(_clip(doc, "idle_legs"), 0), _sample(_clip(doc, "idle_upper"), 0))


# ------------------------------------------------------------------------------------------------ the pendulum

def _smooth(a, loop):
    """A light [1 2 1]/4 smoothing along time (the baked keys are linear, their second difference is spiky)."""
    if loop:
        return (np.roll(a, 1, 0) + 2 * a + np.roll(a, -1, 0)) / 4
    b = np.concatenate([a[:1], a, a[-1:]])
    return (b[:-2] + 2 * b[1:-1] + b[2:]) / 4


def _rot(axis, deg):
    axis = np.asarray(axis, float)
    axis = axis / np.linalg.norm(axis)
    a = math.radians(deg)
    K = np.array([[0, -axis[2], axis[1]], [axis[2], 0, -axis[0]], [-axis[1], axis[0], 0]])
    return np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * K @ K


def droop(d, k=None):
    """Where the lantern settles when the hand holds still: its rigid hang turned DROOP of the way toward straight down."""
    down = np.array([0.0, -1.0, 0.0])
    th = math.degrees(math.acos(float(np.clip(d @ down, -1, 1))))
    ax = np.cross(d, down)
    if th < 1e-6 or np.linalg.norm(ax) < 1e-9:
        return d
    return _rot(ax, th * (DROOP if k is None else k)) @ d


def simulate(P, D, dt, loop, L, sway=None, sag=None):
    """The lantern's world direction over the samples. P: hang point (px, n x 3), D: its rigid direction (n x 3), dt: sample
    step (s), L: the hang point to the lantern's centre (px). sway: per-sample rotation matrices nudging the rigid direction (the breath), or None."""
    n = len(P)
    w0 = 2 * math.pi * F0
    if loop:
        V = (np.roll(P, -1, 0) - np.roll(P, 1, 0)) / (2 * dt)
        A = (np.roll(P, -1, 0) - 2 * P + np.roll(P, 1, 0)) / dt ** 2
    else:   # still before the clip and holding its last pose after it
        Pp = np.concatenate([P[:1], P, P[-1:]])
        A = (Pp[2:] - 2 * Pp[1:-1] + Pp[:-2]) / dt ** 2
    A = _smooth(_smooth(A, loop), loop) * GAIN
    T = np.array([droop(d, sag) for d in D])
    if sway is not None:
        T = np.einsum("nij,nj->ni", sway, T)
    reps = int(math.ceil(SETTLE / (n * dt))) + 1 if loop else 1
    x = T[0] / np.linalg.norm(T[0])
    v = np.zeros(3)
    out = np.zeros((n, 3))
    h = dt / SUBSTEPS
    for r in range(reps):
        for i in range(n):
            j = (i + 1) % n if loop else min(i + 1, n - 1)
            for s in range(SUBSTEPS):
                f = s / SUBSTEPS
                tgt = T[i] * (1 - f) + T[j] * f
                tgt = tgt / np.linalg.norm(tgt)
                acc = A[i] * (1 - f) + A[j] * f
                a = w0 * w0 * (tgt - (tgt @ x) * x) - (acc - (acc @ x) * x) / L - 2 * ZETA * w0 * v
                v = v + a * h
                v = v - (v @ x) * x
                x = x + v * h
                x = x / np.linalg.norm(x)
            if r == reps - 1:
                out[i] = x
    return out


def local_angles(R, n, down, cap):
    """The hang bone's key (Bedrock degrees x, y, z) turning its rigid hang `down` (bone frame) onto world direction n, the swing
    soft-capped at `cap` degrees. R: the rigid hang bone's world rotation."""
    nl = R.T @ n
    # the swing as a turn of `down` onto nl, soft-capped
    c = float(np.clip(down @ nl, -1, 1))
    th = math.degrees(math.acos(c))
    if th > 1e-6:
        ax = np.cross(down, nl)
        ax = ax / np.linalg.norm(ax)
        th2 = cap * math.tanh(th / cap)
        nl = _rot(ax, th2) @ down
    Rl = _align(down, nl)   # the smallest turn of the rest hang onto the swung one, about the hang point
    rx, ry, rz = _euler_zyx(Rl)
    return [-math.degrees(rx), -math.degrees(ry), math.degrees(rz)], th


def _Rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def _Ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def _Rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def _align(u, v):
    """The smallest rotation taking unit u onto unit v."""
    u = u / np.linalg.norm(u)
    v = v / np.linalg.norm(v)
    c = float(u @ v)
    ax = np.cross(u, v)
    s = np.linalg.norm(ax)
    if s < 1e-9:
        return np.eye(3) if c > 0 else _rot(np.array([1.0, 0, 0]), 180)
    return _rot(ax / s, math.degrees(math.atan2(s, c)))


def _euler_zyx(R):
    """(x, y, z) radians with R = Rz(z) Ry(y) Rx(x)."""
    y = math.asin(float(np.clip(-R[2, 0], -1, 1)))
    if abs(math.cos(y)) > 1e-6:
        x = math.atan2(R[2, 1], R[2, 2])
        z = math.atan2(R[1, 0], R[0, 0])
    else:
        x, z = math.atan2(-R[1, 2], R[1, 1]), 0.0
    return x, y, z


# ------------------------------------------------------------------------------------------------ one clip

class Keyer:
    def __init__(self, tid, doc):
        self.tr = Troop(tid)
        self.doc = doc
        self.base = base_pose(doc)
        self.report = {}

    def poses(self, name):
        """The clip's poses over its samples (with its partner's hips), and its timing."""
        doc, A = self.doc, self.doc["animations"]
        c = _clip(doc, name)
        loop = c.loop == "loop"
        n = int(round(c.length * SUB))
        ticks = [i / SUB for i in range(n if loop else n + 1)]
        pn, step = partner(doc, name)
        pc = _clip(doc, pn) if pn else None
        out = []
        for t in ticks:
            if additive(name):
                under = self.base
                p = _merge(under, _merge(_sample(c, t), _sample(pc, t) if pc is not None and t < pc.length else {}), add=True)
            else:
                legs = _sample(pc, (t if step else 0.0) if pc is not None and (not step or t <= pc.length) else 0.0) if pc else {}
                p = _merge(self.base if not pc else {}, legs, _sample(c, t))
            out.append(p)
        return out, loop, ticks

    def key(self, name):
        tr = self.tr
        poses, loop, ticks = self.poses(name)
        dt = 1.0 / (TPS * SUB)
        frs = tr.frames(poses)
        Ms = [fr.bone_M[tr.ref] for fr in frs]
        P = np.array([(M @ np.append(tr.pivot, 1.0))[:3] * 16.0 for M in Ms])
        R = [M[:3, :3] for M in Ms]
        D = np.array([r @ tr.down for r in R])
        L = len(ticks) * dt
        sway = None
        if loop and L >= SWAY_MIN_LEN:
            k = max(1, round(L * 0.45))
            m = len(ticks)
            sway = np.array([_rot([1, 0, 0], SWAY[0] * math.sin(2 * math.pi * k * i / m))
                             @ _rot([0, 0, 1], SWAY[1] * math.sin(2 * math.pi * (k + 1) * i / m + 1.1)) for i in range(m)])
        N = simulate(P, D, dt, loop, tr.L, sway, 0.0 if additive(name) else None)
        cap = CAP_ADD if additive(name) else CAP
        # the fade: additive clips end at zero; deaths let go of the lantern at the part's first drop key (or the end)
        fade = np.ones(len(ticks))
        if additive(name):
            nn = len(ticks)
            k0 = int(nn * 0.6)
            for i in range(k0, nn):
                u = (i - k0) / max(1, nn - 1 - k0)
                fade[i] = 0.5 + 0.5 * math.cos(math.pi * u)
        if name.startswith("death"):
            rel = self.release(name)
            t1 = rel if rel is not None else ticks[-1]
            t0 = max(0.0, t1 - 4.0)
            for i, t in enumerate(ticks):
                fade[i] = 1.0 if t <= t0 else 0.0 if t >= t1 else 0.5 + 0.5 * math.cos(math.pi * (t - t0) / (t1 - t0))
        keys, amps = [], []
        for i in range(len(ticks)):
            nl = R[i].T @ N[i]
            th = math.degrees(math.acos(float(np.clip(tr.down @ nl, -1, 1))))
            if fade[i] < 1:   # fade by turning back toward the rigid hang
                ax = np.cross(tr.down, nl)
                if np.linalg.norm(ax) > 1e-9:
                    nl = _rot(ax, th * fade[i] - th) @ nl
                N[i] = R[i] @ nl
            e, th = local_angles(R[i], N[i], tr.down, cap)
            keys.append(e)
            amps.append(th)
        self.cuts = (0, 0.0, 1.0)
        keys = self.clear(frs, R, P, keys, loop)
        # report
        dirs = np.array([self._dir(k) for k in keys])
        sw = [math.degrees(math.acos(float(np.clip(tr.down @ d, -1, 1)))) for d in dirs]
        mean = dirs.mean(0) / np.linalg.norm(dirs.mean(0))
        sway = max(math.degrees(math.acos(float(np.clip(mean @ d, -1, 1)))) for d in dirs)
        self.report[name] = (max(sw), sway, len(ticks), *self.cuts)
        return keys, ticks, loop

    def _dir(self, e):
        Rl = _Rz(math.radians(e[2])) @ _Ry(math.radians(-e[1])) @ _Rx(math.radians(-e[0]))
        return Rl @ self.tr.down

    def release(self, name):
        bj = self.doc["animations"][name]["bones"].get(self.tr.part_ref, {})
        for ch in ("rotation", "position"):
            ks = bj.get(ch)
            if not ks:
                continue
            for t, v in sorted(((float(k), v) for k, v in ks.items()), key=lambda x: x[0]):
                if any(abs(x) > 1e-6 for x in (v if isinstance(v, list) else v.get("post", v.get("vector", [0, 0, 0])))):
                    return max(0.0, t * TPS - 0.5)   # ticks
        return None

    # -------------------------------------------------------------------------------------------- clearance

    def depths(self, fr, E):
        """The lantern's deepest sinking (px) into the body's boxes and below the floor, swung by each key in E (k x 3)."""
        tr = self.tr
        E = np.radians(np.asarray(E, float))
        out = np.zeros(len(E))
        M = fr.bone_M[tr.ref]
        C = fr.corners[tr.body] * 16.0                         # cubes x 8 x 3
        o = C[:, 0]
        ed = C[:, [1, 2, 6]] - o[:, None, :]                   # the three edges from corner 0 (render_geo: BLB to BRB, TLB, BLF)
        ln = np.linalg.norm(ed, axis=2)
        ok = np.all(ln > 1e-6, axis=1)
        o, ed, ln = o[ok], ed[ok], ln[ok]
        U = ed / ln[:, :, None]
        for k, e in enumerate(E):
            Rl = _Rz(e[2]) @ _Ry(-e[1]) @ _Rx(-e[0])
            loc = (tr.probe - tr.pivot) @ Rl.T + tr.pivot
            w = (loc @ M[:3, :3].T + M[:3, 3]) * 16.0          # points x 3, world px
            q = np.einsum("pcj,cij->pci", w[:, None, :] - o[None], U)   # points x cubes x 3
            inside = np.all((q > 0) & (q < ln[None]), axis=2)
            d = np.where(inside, np.minimum(q, ln[None] - q).min(axis=2), 0.0)
            out[k] = max(float(d.max()), float(-w[:, 1].min()), 0.0)
        return out

    def clear(self, frs, R, P, keys, loop=False):
        """Keeps the swung lantern out of the body: a sample whose swing would sink it deeper than the rigid lantern does takes the
        nearest swing (pitch and roll offsets, or the swing scaled back) that does not; the corrections are then smoothed over
        time, and any sample the smoothing reopens is scaled back toward the rigid hang."""
        keys = np.array(keys, float)
        n = len(keys)
        rigid = np.array([self.depths(fr, [[0.0, 0.0, 0.0]])[0] for fr in frs])
        limit = rigid + MARGIN
        bad = [i for i in range(n) if self.depths(frs[i], [keys[i]])[0] > limit[i]]
        if not bad:
            return keys.tolist()
        grid = [(dx, dz) for dx in range(-24, 25, 4) for dz in range(-24, 25, 4)]
        corr = np.zeros((n, 3))
        for i in bad:
            k = keys[i]
            cands = [k + np.array([dx, 0.0, dz]) for dx, dz in grid] + [k * f for f in np.linspace(0, 1, 9)]
            dep = self.depths(frs[i], cands)
            cost = [np.hypot(*(c - k)[[0, 2]]) if d <= limit[i] else 1e9 for c, d in zip(cands, dep)]
            j = int(np.argmin(cost))
            corr[i] = cands[j] - k if cost[j] < 1e9 else -k
        # smooth the corrections over time (a [1 2 1] pass a few times), keeping each sample's own correction as a floor
        sm = corr.copy()
        for _ in range(4):
            sm = _smooth(sm, loop)
            big = np.abs(corr) > np.abs(sm)
            sm[big] = corr[big]
        out = keys + sm
        s = np.ones(n)
        for i in range(n):
            if self.depths(frs[i], [out[i]])[0] <= limit[i]:
                continue
            lo, hi = 0.0, 1.0
            for _ in range(8):
                m = (lo + hi) / 2
                if self.depths(frs[i], [out[i] * m])[0] <= limit[i]:
                    lo = m
                else:
                    hi = m
            s[i] = lo
        g = s.copy()
        for i in range(n):
            for j in range(i - 6, i + 7):
                jj = j % n if loop else j
                if 0 <= jj < n:
                    g[jj] = min(g[jj], s[i] + (1 - s[i]) * abs(j - i) / 7)
        out = out * g[:, None]
        self.cut = getattr(self, "cut", 0) + len(bad)
        self.cuts = (len(bad), float(np.abs(sm).max()), float(g.min()))
        return out.tolist()


# ------------------------------------------------------------------------------------------------ files

def _thin(ts, vals, tol, loop):
    keep, a, n = [0], 0, len(vals)
    while a < n - 1:
        b = a + 1
        while b + 1 < n and all(abs(vals[a][k] + (vals[b + 1][k] - vals[a][k]) * (m - a) / (b + 1 - a) - vals[m][k]) <= tol
                                for m in range(a + 1, b + 1) for k in range(3)):
            b += 1
        keep.append(b)
        a = b
    return keep


def _fmt_t(sec):
    return ("%.4f" % sec).rstrip("0").rstrip(".") if sec else "0.0"


def key_doc(tid, doc, only=None, log=True):
    """Keys (or re-keys) the hang bones in every clip of `doc` (a {"animations": ...} dict), in place."""
    import troop_rig
    if tid not in troop_rig.SWING:
        return {}
    k = Keyer(tid, doc)
    A = doc["animations"]
    for name in list(A):
        if only and name not in only:
            continue
        bones = A[name]["bones"]
        for h in k.tr.hangs:
            bones.pop(h, None)
        if is_legs(name):
            continue
        keys, ticks, loop = k.key(name)
        vals = [[round(v, 3) for v in e] for e in keys]
        if loop:   # close the loop on the first key
            ticks = ticks + [len(ticks) / SUB]
            vals = vals + [vals[0]]
        if max(abs(v) for e in vals for v in e) < ROT_TOL:
            continue
        idx = _thin(ticks, vals, ROT_TOL, loop)
        ch = {"rotation": {_fmt_t(ticks[i] / TPS): vals[i] for i in idx}}
        for h in k.tr.hangs:
            bones[h] = json.loads(json.dumps(ch))
    if log:
        for name, (mx, med, n, nc, dc, sc) in k.report.items():
            print("  %-26s off the rigid hang up to %5.1f deg, swings %4.1f deg about its mean" % (name, mx, med)
                  + ("   clearance: %d of %d samples moved (up to %.0f deg)%s" % (nc, n, dc, ", scaled to %.2f" % sc if sc < 1 else "")
                     if nc else ""))
        if getattr(k, "cut", 0):
            print("  clearance trimmed", k.cut, "samples")
    return k.report


def key_file(tid, path, only=None):
    path = Path(path)
    doc = json.loads(path.read_text(encoding="utf-8"))
    key_doc(tid, doc, only)
    path.write_bytes(json.dumps(doc, indent=1).encode("utf-8"))
    print("keyed", "+".join(Troop(tid).hangs), "into", path)


def main():
    tid = sys.argv[1]
    args = sys.argv[2:]
    if "--report" in args:
        doc = json.loads((ASSET_ANIMS / f"{tid}.animation.json").read_text(encoding="utf-8"))
        key_doc(tid, doc)
        return
    paths = args or [HERE.parent / tid / "out" / f"{tid}.animation.json", ASSET_ANIMS / f"{tid}.animation.json"]
    for p in paths:
        key_file(tid, p)


if __name__ == "__main__":
    main()
