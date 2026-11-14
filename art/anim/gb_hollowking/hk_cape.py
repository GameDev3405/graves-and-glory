"""The Hollow King's cape in motion (2026-11-14, on the knee-length cape of the 2026-11-13 model).

The cape is torso > cape (container) > collar (rigid mount, the rolled fold) > cloak_a, cloak_b, cloak_c, three panels of
3x13, 4x15 and 3x12 px hinged on the collar line. Every clip keys the motion on the three panels (x: swing away from the
back or in, z: sideways); the container is held at its rest (keyed 0 in the absolute clips, so a blend never carries
an old value) and the collar is never keyed.

Each panel is a damped spring on its hinge (its own period and damping, so the three are out of phase), pulled toward
the way gravity hangs it in the collar's frame, with the hinge's own acceleration taken off gravity (a body that
lunges leaves the cloth behind, one that stops throws it on), and a head wind for the gaits (walk, run, charge: it
trails back with the speed). The stride beats on it in the gaits (out and in twice a cycle, side to side once, each panel a little
later than the one before). A slow breath of its own drives the holds (idle and stances) so it is never a still board.
Per sample the panel is kept clear of the body, the arms, the weapon, the pauldrons and the ground: where its spring
would put it inside something it is set at the nearest clear turn and carried on by what pushed it. A one-shot clip
(attack, hit, death) limits its own speed to about 20 degrees a tick unless the torso turns faster, and starts a push a
few samples early instead of flicking in one; a loop runs to its steady swing and closes.

Additive hits (HitController adds them onto the stance) are solved on the pose the game shows (the ready stance under
them) and keyed as what the stance does not already give, back to it by the end.

Called from hk_polish (stage "cape", after every body fix, so it sees the final body).
"""
from __future__ import annotations

import math

import numpy as np

PANELS = ("cloak_a", "cloak_b", "cloak_c")
CAPE_BONES = ("cape", "collar") + PANELS
SUB, TPS = 4, 20.0
Z3 = (0.0, 0.0, 0.0)
DT = 1.0 / (SUB * TPS)

G = 25.0              # blocks/s^2 the cloth falls at (gravity as the game feels, not 9.8 at this scale)
ACC_GAIN = 0.50       # of the hinge's acceleration the cloth feels (the rest is the cloth's own give)
ACC_MAX = 0.55        # the hinge's acceleration, at most this much of G (a fast lunge or a landing)
PERIOD = {"cloak_a": 0.56, "cloak_b": 0.64, "cloak_c": 0.52}     # s: longer panel, slower swing
ZETA = {"cloak_a": 0.34, "cloak_b": 0.30, "cloak_c": 0.36}
TRAIL = {"walk": 18.0, "run": 34.0, "charge": 40.0, "back": -6.0}   # degrees the head wind lays the cape back
FLAP = {"walk": 4.5, "run": 7.0, "charge": 8.0, "back": 3.0}    # degrees: the stride's own beat on the cloth (twice a
SWAY = {"walk": 3.0, "run": 4.0, "charge": 4.0, "back": 2.0}    # cycle out and in, once a cycle side to side), each panel
LAG = {"cloak_a": 0.0, "cloak_b": 0.7, "cloak_c": 1.3}          # a little later than the one before it (rad)
BREATH_X = 2.8        # degrees: the holds' own slow swing away from the back and in
BREATH_Z = 1.6        # degrees sideways
BREATH_T = 2.6        # s, about (fitted to a whole number of cycles in a loop)
PHASE = {"cloak_a": 0.0, "cloak_b": 2.2, "cloak_c": 4.1}
U_MIN, U_MAX = -20.0, 110.0      # degrees a panel swings in (toward the legs) or out from the back at most
Z_MAX = 45.0                     # degrees sideways at most
STIFF_X, STIFF_Z = 0.3, 0.6      # of the way back to its modelled hang the cloth holds (a heavy wool cape, not a sheet):
                                 # the panels keep their flare and their lie off the back unless the body tips them
REACH = 30.0          # degrees at most a panel is turned off its spring to clear something (a fall: REACH_DEATH); what
REACH_DEATH = 75.0    # is left after that is the hinge's slide's or is reported, never a flung flag
SLIDE_MAX = 2.5       # px a panel's hinge slides off a plate at most
SLIDE_RATE = 0.2      # px per sample it slides or settles at most
CLEAR = 0.25          # px a panel may lie in another piece beyond its rest fit
SPIKE, SPIKE_TOL = 6.0, 0.75    # degrees a lone quarter-tick sample may stand off both neighbours; px it may then leave
                                # (or a tenth more than its neighbours leave)
FLOOR_PAD = 0.3       # px over the ground line
FLOOR_DEATH = 1.2     # px a panel may lie in the ground in a fall (cloth lands and folds there)
RATE = 5.0            # degrees per sample (20 a tick) a panel moves at most in a one-shot, unless the torso is faster
LAND = 5.0            # degrees per sample at most a push moves it in a one-shot: it starts early instead
SHALLOW = 0.3         # px past CLEAR a graze may stay rather than turn the panel more than 10 degrees for it
CARRY = 0.2           # of what pushed it the cloth keeps when the push stops
END_EASE = 4          # ticks an additive clip eases back onto its stance


def _blur(v, loop, times=2):
    k = np.array([1.0, 4.0, 6.0, 4.0, 1.0]) / 16.0
    v = np.asarray(v, dtype=float)
    for _ in range(times):
        if loop:
            m = len(v) - 1
            p = np.concatenate([v[m - 2:m], v[:m], v[:3]])
            w = np.stack([np.convolve(p[:, j], k, mode="valid") for j in range(v.shape[1])], axis=1) if v.ndim == 2 \
                else np.convolve(p, k, mode="valid")
            v = np.concatenate([w[:m], w[:1]])
        else:
            pad = np.pad(v, ((2, 2),) + ((0, 0),) * (v.ndim - 1), mode="edge")
            v = np.stack([np.convolve(pad[:, j], k, mode="valid") for j in range(v.shape[1])], axis=1) if v.ndim == 2 \
                else np.convolve(pad, k, mode="valid")
    return v


class Cape:
    """The cape panels on the hk_polish rig: their cubes, rest fits, hinge and hem, and the clear test."""

    def __init__(self, rig):
        self.rig = rig
        m = rig.model
        self.bones = m.bones
        under = rig.subtree("cape")
        self.idx = {p: [i for i, b in enumerate(rig.bone_of) if b == p] for p in PANELS}
        self.others = [i for i, b in enumerate(rig.bone_of) if b not in under]
        low = rig.subtree("right_leg") | rig.subtree("left_leg")
        # what sits over the hinge (plates, arms, the back): only these make the hinge slide; legs and the ground turn it
        self.upper = [i for i in self.others if rig.bone_of[i] not in low
                      and not rig.bone_of[i].startswith(("rags", "hip_", "belt", "hips"))]
        M, C, R = rig.fk({})
        self.rest_fit = {}
        self.tip = {}
        for p in PANELS:
            ix = self.idx[p]
            self.rest_fit[p] = rig.fits_of(C, R, ix, C[ix], R[ix], self.others)
            c = m.cube_list[ix[0]].corners
            self.tip[p] = c[np.argsort(c[:, 1])[:4]].mean(axis=0)     # the hem's middle at rest (model space)
        self.ground = rig.ground
        # the sign that swings a hem away from his back (world -z: +z is his front)
        a = self.hem(M, "cloak_b", (10.0, 0.0))
        b = self.hem(M, "cloak_b", (-10.0, 0.0))
        self.sx = 1.0 if a[2] < b[2] else -1.0
        # the ways a panel's hinge may slide off a plate that comes back over it (a pauldron riding a raised arm sweeps
        # the panel's top, where no turn on the hinge can clear it): back off the body, and in from the arm
        self.slide = {}
        for p in PANELS:
            h0 = self.hinge_at(M, p, (0.0, 0.0, 0.0))
            zb = 1.0 if self.hinge_at(M, p, (0.0, 0.0, 1.0))[2] < h0[2] else -1.0
            back = np.array([0.0, 0.0, zb])
            dirs = [back]
            if abs(h0[0]) > 1e-3:
                xi = 1.0 if abs(self.hinge_at(M, p, (1.0, 0.0, 0.0))[0]) < abs(h0[0]) else -1.0
                inn = np.array([xi, 0.0, 0.0])
                dirs += [(back + inn) / math.sqrt(2.0), inn]
            self.slide[p] = dirs

    def mat(self, M, p, ang, off=None):
        """Panel p's matrix turned (x, z) degrees (channel signs) on its hinge, its hinge slid `off` (channel px)."""
        return M["collar"] @ self.rig.local(p, (ang[0], 0.0, ang[1]), None if off is None else tuple(off))

    def hinge_at(self, M, p, off):
        return (self.mat(M, p, (0.0, 0.0), off) @ np.append(self.bones[p].pivot / 16.0, 1.0))[:3]

    def need_slide(self, fr, p, angs, floor_tol):
        """{direction index: px} the least slide of panel p's hinge along each of its slide directions that leaves one of
        the turns `angs` clear; {} when one is clear already."""
        c0 = min(self.cost(fr, p, a, floor_tol, pool=self.upper) for a in angs)
        if c0 <= 0.0:
            return {}
        out = {}
        for k, d in enumerate(self.slide[p]):
            for x in np.arange(0.25, SLIDE_MAX + 1e-6, 0.25):
                if any(self.cost(fr, p, a, floor_tol, d * x, pool=self.upper) <= 0.0 for a in angs):
                    out[k] = float(x)
                    break
        if not out and c0 > SHALLOW:     # no slide clears it: the one that leaves least, if it helps at all
            for k, d in enumerate(self.slide[p]):
                c = min(self.cost(fr, p, a, floor_tol, d * SLIDE_MAX, pool=self.upper) for a in angs)
                if c < c0 - 0.1:
                    out[k] = SLIDE_MAX
        return out

    def hem(self, M, p, ang):
        return (self.mat(M, p, ang) @ np.append(self.tip[p], 1.0))[:3]

    def hinge(self, M, p):
        return (M["collar"] @ np.append(self.bones[p].pivot / 16.0, 1.0))[:3]

    def dir_local(self, p, ang):
        """Unit hinge-to-hem direction in the collar's frame."""
        L = self.rig.local(p, (ang[0], 0.0, ang[1]), None)
        v = (L @ np.append(self.tip[p], 1.0))[:3] - self.bones[p].pivot / 16.0
        return v / np.linalg.norm(v)

    def goal(self, M, p, g_world, guess):
        """The (x, z) turn that hangs panel p along g_world, in channel signs, from a guess (Gauss-Newton, bounded)."""
        Rp = M["collar"][:3, :3]
        d = np.linalg.solve(Rp, g_world)
        d /= np.linalg.norm(d)
        x = np.array(guess, dtype=float)
        for _ in range(6):
            f = self.dir_local(p, x) - d
            J = np.zeros((3, 2))
            for j in range(2):
                e = np.zeros(2)
                e[j] = 0.5
                J[:, j] = (self.dir_local(p, x + e) - self.dir_local(p, x - e)) / 1.0
            step = np.linalg.lstsq(J, -f, rcond=None)[0]
            step = np.clip(step, -25.0, 25.0)
            x = x + step
            x[0] = self.sx * float(np.clip(self.sx * x[0], U_MIN, U_MAX))
            x[1] = float(np.clip(x[1], -Z_MAX, Z_MAX))
            if np.abs(step).max() < 0.05:
                break
        return x

    def cost(self, fr, p, ang, floor_tol, off=None, pool=None):
        """px panel p lies in the rest of the body beyond its rest fit, or in the ground beyond floor_tol."""
        M, C, R = fr
        Mp = dict(M)
        Mp[p] = self.mat(M, p, ang, off)
        PC, PR = self.rig._cubes(Mp, self.idx[p])
        fits = self.rig.fits_of(C, R, self.idx[p], PC, PR, self.others if pool is None else pool)
        rf = self.rest_fit[p]
        gap = max([v - rf.get(b, 0.0) for b, v in fits.items()] + [0.0])
        if pool is not None:
            return max(gap - CLEAR, 0.0)
        low = self.ground + FLOOR_PAD - float(PC[:, :, 1].min()) * 16.0
        return max(gap - CLEAR, low - floor_tol, 0.0)

    def clear(self, fr, p, ang, floor_tol, off=None, reach=None):
        """The nearest clear turn to ang: ang itself when clear; else rings of growing size in eight directions
        (away from the back first), bisected. (turn, cost left)."""
        c0 = self.cost(fr, p, ang, floor_tol, off)
        if c0 <= 0.0:
            return np.array(ang, dtype=float), 0.0
        best = (c0, np.array(ang, dtype=float))
        if reach is None:
            reach = REACH
        if c0 <= SHALLOW:        # a graze: a small turn may clear it, a big one is worse than the graze
            reach = min(reach, 10.0)
        dirs = [(1, 0), (1, 1), (1, -1), (0, 1), (0, -1), (-1, 1), (-1, -1), (-1, 0)]
        prev = 0.0
        for r in (3.0, 6.0, 10.0, 15.0, 22.0, 30.0, 40.0, 55.0, 75.0):
            if r > reach:
                break
            hit = None
            for du, dz in dirs:
                n = math.hypot(du, dz)
                cand = np.array([ang[0] + self.sx * du * r / n, ang[1] + dz * r / n])
                if not (U_MIN - 1e-6 <= self.sx * cand[0] <= U_MAX + 1e-6 and abs(cand[1]) <= Z_MAX + 1e-6):
                    continue
                c = self.cost(fr, p, cand, floor_tol, off)
                if c < best[0]:
                    best = (c, cand)
                if c <= 0.0:
                    hit = (du / n, dz / n)
                    break
            if hit is not None:
                lo, hi = prev, r
                for _ in range(5):
                    mid = (lo + hi) / 2.0
                    cand = np.array([ang[0] + self.sx * hit[0] * mid, ang[1] + hit[1] * mid])
                    if self.cost(fr, p, cand, floor_tol, off) <= 0.0:
                        hi = mid
                    else:
                        lo = mid
                return np.array([ang[0] + self.sx * hit[0] * hi, ang[1] + hit[1] * hi]), 0.0
            prev = r
        if c0 <= SHALLOW:
            return np.array(ang, dtype=float), c0
        return best[1], best[0]


def _torso_rate(frames):
    """Degrees per sample the torso turns, per sample."""
    out = [0.0]
    for a, b in zip(frames, frames[1:]):
        Ra, Rb = a[0]["torso"][:3, :3], b[0]["torso"][:3, :3]
        D = Rb @ np.linalg.inv(Ra)
        c = max(-1.0, min(1.0, (np.trace(D) - 1.0) / 2.0))
        out.append(math.degrees(math.acos(c)))
    return np.array(out)


def _tent(x, rate, loop):
    """The least curve over x that rises and falls at most `rate` per sample (a loop wraps)."""
    x = np.asarray(x, dtype=float)
    n = len(x)
    if loop:
        m = n - 1
        v = np.concatenate([x[:m]] * 3)
    else:
        v = x.copy()
    for i in range(1, len(v)):
        v[i] = max(v[i], v[i - 1] - rate)
    for i in range(len(v) - 2, -1, -1):
        v[i] = max(v[i], v[i + 1] - rate)
    if loop:
        v = v[m:2 * m]
        v = np.append(v, v[0])
    return v


def solve(cape, frames, loop, x0=None, speed=None, breath=False, floor_tol=0.0, seam=None, additive=False):
    """{panel: (n+1 x 2 array of (x, z) turns, n+1 x 3 array of hinge slides in px)} for the frames (rig.fk of each
    sample, the cape stripped)."""
    n = len(frames) - 1
    length = n * DT
    wind = np.zeros(3)
    if speed:
        wind = np.array([0.0, 0.0, -1.0]) * G * math.tan(math.radians(TRAIL[speed]))
    rate = np.clip(1.2 * _torso_rate(frames), RATE, 2.0 * RATE)
    out = {}
    for p in PANELS:
        H = np.array([cape.hinge(fr[0], p) for fr in frames])
        if n >= 2:
            A = np.zeros_like(H)
            if loop:
                m = n
                for i in range(n + 1):
                    A[i] = (H[(i + 1) % m] - 2 * H[i % m] + H[(i - 1) % m]) / (DT * DT)
            else:
                A[1:-1] = (H[2:] - 2 * H[1:-1] + H[:-2]) / (DT * DT)
                A[0], A[-1] = A[1], A[-2]
            A = _blur(A, loop)
        else:
            A = np.zeros_like(H)
        A *= ACC_GAIN
        mag = np.linalg.norm(A, axis=1)
        over = mag > ACC_MAX * G
        A[over] *= (ACC_MAX * G / mag[over])[:, None]
        # the goal per sample: gravity less the hinge's acceleration, plus the head wind, plus the hold's breath
        goal = np.zeros((n + 1, 2))
        guess = np.zeros(2)
        for i, fr in enumerate(frames):
            gw = np.array([0.0, -G, 0.0]) - A[i] + wind
            guess = cape.goal(fr[0], p, gw, guess)
            goal[i] = guess * (1.0 - np.array([STIFF_X, STIFF_Z]))
        base = goal.copy()     # the slide is judged on the steady goal: the beat below is scaled up for the spring
        if speed:      # the stride beats on the cloth: the hips' bob twice a cycle, their roll once; the spring adds the lag
            ts = np.arange(n + 1) * DT
            fn = 1.0 / PERIOD[p]

            def gain(f):    # the spring's own answer to a beat of f Hz: the beat is scaled up so the cloth shows FLAP / SWAY
                r = f / fn
                return math.sqrt((1 - r * r) ** 2 + (2 * ZETA[p] * r) ** 2)
            goal[:, 0] += cape.sx * FLAP[speed] * gain(2.0 / length) * np.sin(4 * math.pi * ts / length - LAG[p])
            goal[:, 1] += SWAY[speed] * gain(1.0 / length) * np.sin(2 * math.pi * ts / length - LAG[p])
        if breath:
            k = max(1, int(round(length / BREATH_T))) if loop else 1
            T = length / k if loop else BREATH_T
            ts = np.arange(n + 1) * DT
            goal[:, 0] += cape.sx * BREATH_X * np.sin(2 * math.pi * ts / T + PHASE[p])
            goal[:, 1] += BREATH_Z * np.sin(2 * math.pi * ts / T + PHASE[p] + 1.3)
        # the hinge's slide off a plate that sweeps the panel's top (no turn clears that): one way per clip, eased
        needs = [cape.need_slide(fr, p, [base[i]], floor_tol)
                 for i, fr in enumerate(frames)]
        off = np.zeros((n + 1, 3))
        if any(needs):
            ks = range(len(cape.slide[p]))
            k = min(ks, key=lambda k: (sum(1 for nd in needs if nd and k not in nd),
                                       max(nd.get(k, 0.0) for nd in needs), k))
            E = _tent([nd.get(k, 0.0) for nd in needs], SLIDE_RATE, loop)
            if not loop:
                E = np.minimum(E, SLIDE_RATE * np.arange(n + 1))
            if additive:
                E = np.minimum(E, SLIDE_RATE * (n - np.arange(n + 1)))
            off = np.outer(E, cape.slide[p][k])
        w = 2.0 * math.pi / PERIOD[p]
        kk, damp = w * w, 2.0 * ZETA[p] * w
        x = np.array(goal[0] if x0 is None or p not in x0 else x0[p], dtype=float)
        reach = REACH_DEATH if floor_tol > 0 else REACH
        x, _ = cape.clear(frames[0], p, x, floor_tol, off[0], reach)
        v = np.zeros(2)
        cur = np.zeros((n + 1, 2))
        for _ in range(4 if loop else 1):
            for i in range(n + 1):
                if i:
                    h = DT / 4.0
                    for _ in range(4):
                        v += (kk * (goal[i] - x) - damp * v) * h
                        x = x + v * h
                    if not loop:
                        d = x - cur[i - 1]
                        lim = rate[i]
                        s = float(np.abs(d).max())
                        if s > lim:
                            x = cur[i - 1] + d * (lim / s)
                            v *= lim / s
                prev = x.copy()
                x, _ = cape.clear(frames[i], p, x, floor_tol, off[i], reach)
                if np.abs(x - prev).max() > 1e-6 and i:
                    push = (x - prev) / DT
                    v = np.where(np.sign(push) == np.sign(v), v, 0.0) + CARRY * push * (np.abs(push) > 0)
                    v = np.clip(v, -0.4 * RATE / DT, 0.4 * RATE / DT)
                cur[i] = x
        if loop:      # the residue of the wrap spread over the cycle, so the last sample is the first
            cur -= np.outer(np.arange(n + 1) / float(n), cur[n] - cur[0])
        if seam is not None:
            a, z = seam
            src = cur.copy()
            for i in range(z - a, z + 1):
                f = _smooth((i - (z - a)) / float(a))
                cur[i] = src[i] * (1.0 - f) + src[i - (z - a)] * f
            d0 = cur[z] - cur[0]
            for i in range(0, a + 1):
                cur[i] += d0 * (1.0 - _smooth(i / float(a)))
        if not loop:   # a push no faster than LAND a sample: it starts early instead
            for i in range(n - 1, -1, -1):
                d = cur[i] - cur[i + 1]
                s = float(np.abs(d).max())
                if s > max(LAND, rate[i + 1]):
                    cur[i] = cur[i + 1] + d * (max(LAND, rate[i + 1]) / s)
        out[p] = (cur, off)
    return out


def _smooth(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def final_clear(cape, frames, curves, floor_tol):
    """Re-clears every sample after the closing passes (loop residue, seam, early start), keeping the nearest turn."""
    worst = {}
    for p, (cur, off) in curves.items():
        w = 0.0
        for i, fr in enumerate(frames):
            x, c = cape.clear(fr, p, cur[i], floor_tol, off[i], REACH_DEATH if floor_tol > 0 else REACH)
            cur[i] = x
            w = max(w, c)
        for _ in range(3):     # a lone sample flung far off both neighbours (a plate met before the slide is out) is a
            for i in range(1, len(frames) - 1):     # pop: take the neighbours' mean when it is near clear
                a, b = cur[i] - cur[i - 1], cur[i] - cur[i + 1]
                if np.abs(a).max() > SPIKE and np.abs(b).max() > SPIKE and np.dot(a, b) > 0:
                    m = 0.5 * (cur[i - 1] + cur[i + 1])
                    c = cape.cost(frames[i], p, m, floor_tol, off[i])
                    near = max(cape.cost(frames[j], p, cur[j], floor_tol, off[j]) for j in (i - 1, i + 1))
                    if c <= max(SPIKE_TOL, near + 0.1):
                        cur[i] = m
                        w = max(w, c)
        worst[p] = w
    return worst
