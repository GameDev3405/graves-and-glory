"""The authoring solvers, in game model space (px, y down, -z forward, the troop's right at -x).

Arms are aimed, not keyed: an aim is a direction for the upper arm, the forearm and (optionally) the held item, in the
frame of the bone the arm hangs from (the chest, or the body while the rig has no chest). aim_arm turns the aims into the
arm's, forearm's and hand's Euler channels: the upper arm's twist puts the elbow's hinge in the plane of the two aims
(the forearm only pitches, never past 0), a straight arm keeps the twist the hint gives (natural grip: the previous
frame's, else the parent's forward), and the hand takes the smallest turn that lays the item along its aim. Every Euler
triple is the solution nearest the previous frame's (closest_euler), so nothing flips through 360.

Legs are placed, not keyed: solve_leg finds the leg's pitch and roll and the knee (shin pitch, never below 0) that put the
sole's centre on a target in world space, by damped Gauss-Newton in the guide's bounds. Foot paths between plants use
PCHIP (Fritsch-Carlson), which never overshoots a held value, so a planted foot stays put.
"""
import math

import numpy as np

from .model import euler, rot

LEG_LO = np.array([-110.0, -40.0, 0.0])
LEG_HI = np.array([80.0, 40.0, 150.0])
NECK = (50.0, 75.0, 25.0)            # the most the head turns from its parent: pitch, yaw, roll
SOLE = 6.0                            # the sole below the shin's pivot (the shin box, MotionFeet's hull)


def unit(v):
    v = np.asarray(v, float)
    n = np.linalg.norm(v)
    return v / n if n > 1e-12 else v


def closest_euler(R, prev=None):
    """(pitch, yaw, roll) of R, the one of its two Euler solutions (each angle moved by whole turns) nearest prev."""
    a, b, c = euler(R)
    cands = [(a, b, c), (a + 180.0, 180.0 - b, c + 180.0)]
    if prev is None:
        return min(cands, key=lambda e: sum(abs(((x + 180) % 360) - 180) for x in e))
    best, cost = None, 1e18
    for e in cands:
        e = tuple(x + 360.0 * round((p - x) / 360.0) for x, p in zip(e, prev))
        d = sum((x - p) ** 2 for x, p in zip(e, prev))
        if d < cost:
            best, cost = e, d
    return best


def between(a, b):
    """The smallest rotation taking unit a to unit b."""
    a, b = unit(a), unit(b)
    v = np.cross(a, b)
    c = float(np.dot(a, b))
    if c < -0.999999:
        axis = unit(np.cross(a, [1, 0, 0]) if abs(a[0]) < 0.9 else np.cross(a, [0, 1, 0]))
        return 2 * np.outer(axis, axis) - np.eye(3)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + K + K @ K / (1 + c)


def frame(y_dir, back_hint):
    """A rotation whose +y is y_dir and whose +z lies along back_hint made square to it."""
    y = unit(y_dir)
    z = np.asarray(back_hint, float) - np.dot(back_hint, y) * y
    if np.linalg.norm(z) < 1e-6:
        alt = np.array([0.0, 0.0, 1.0]) if abs(y[2]) < 0.9 else np.array([1.0, 0.0, 0.0])
        z = alt - np.dot(alt, y) * y
    z = unit(z)
    x = np.cross(y, z)
    return np.column_stack([x, y, z])


def angle(a, b):
    return math.degrees(math.acos(max(-1.0, min(1.0, float(np.dot(unit(a), unit(b)))))))


def geodesic(Ra, Rb):
    """The true rotation between two orientations, degrees."""
    c = (np.trace(Ra.T @ Rb) - 1) / 2
    return math.degrees(math.acos(max(-1.0, min(1.0, c))))


class Arm:
    """One arm of a troop: aims in, net Euler rotations (rest + channels) of the arm, forearm and hand out.

    The forearm is a hinge (pitch only, never past 0): the upper arm's twist puts the bend in the plane of the two aims;
    a straight arm keeps the twist it had (natural grip). The hand starts from its previous rotation and takes the
    smallest turn that lays the item's tip along its aim, then twists about the tip to turn its edge toward `edge`."""

    def __init__(self, troop, side):
        self.troop, self.side = troop, side
        self.arm, self.fore, self.hand = f'{side}_arm', f'{side}_forearm', f'{side}_hand'
        self.has_fore = self.fore in troop.by
        self.has_hand = self.hand in troop.by
        self.tip = self.edge = None
        self.grip = np.zeros(3)
        ax = troop.item_axes()
        if ax is not None:
            if side == troop.hand:
                self.grip, self.tip, self.edge = ax[0], ax[1], ax[3]
            else:                                         # the off fist's axis: the held item's, mirrored
                m = np.array([-1.0, 1.0, 1.0])
                self.grip, self.tip, self.edge = ax[0] * m, ax[1] * m, ax[3] * m
        self.shoulder = troop.rest[self.arm][1]
        self.elbow = troop.rest[self.fore][1] if self.has_fore else np.zeros(3)
        self.wrist = troop.rest[self.hand][1] if self.has_hand else np.array([0.0, 10.0, 0.0])
        self.prev = {}

    def seed(self, net):
        """Start from a pose: {bone: net rotation} of this arm's bones (the stance)."""
        self.prev = {b: tuple(float(x) for x in net[b]) for b in (self.arm, self.fore, self.hand) if b in net}
        if self.arm in net:
            self.prev['hint'] = rot(net[self.arm])[:, 2]

    def state(self):
        return {k: (v.copy() if isinstance(v, np.ndarray) else v) for k, v in self.prev.items()}

    def frames(self, net):
        """(arm, arm @ forearm, arm @ forearm @ hand) rotations in the parent frame for net rotations."""
        Ra = rot(net[self.arm])
        Rf = Ra @ rot(net[self.fore]) if self.fore in net else Ra
        Rh = Rf @ rot(net[self.hand]) if self.hand in net else Rf
        return Ra, Rf, Rh

    def aims(self, net):
        """The aims a pose holds: {'upper', 'fore', 'item', 'edge', 'twist'} in the parent frame (unit vectors; twist in
        degrees, the edge's turn about the item from the natural grip)."""
        Ra, Rf, Rh = self.frames(net)
        out = {'upper': Ra[:, 1], 'fore': Rf[:, 1]}
        if self.tip is not None and self.has_hand:
            out['item'], out['edge'] = Rh @ self.tip, Rh @ self.edge
            out['twist'] = twist_of(natural(Rf, out['item']), out['edge'], out['item'])
        return out

    def hand_point(self, net):
        """The grip point (the fist's centre) in the parent frame for net rotations."""
        Ra = rot(net[self.arm])
        if not self.has_fore:
            return self.shoulder + Ra @ (self.wrist + self.grip)
        Rf = rot(net[self.fore])
        Rh = rot(net[self.hand]) if self.has_hand else np.eye(3)
        return self.shoulder + Ra @ (self.elbow + Rf @ (self.wrist + Rh @ self.grip))

    def reach(self, target, hint):
        """(upper, fore) aims putting the fist on target (parent frame), the elbow bulging toward hint; the hand keeps
        its last rotation relative to the forearm. Also the residual px when target is out of reach."""
        target = np.asarray(target, float)
        d = target - self.shoulder
        dist = float(np.linalg.norm(d))
        Rh = rot(self.prev.get(self.hand, (0, 0, 0))) if self.has_hand else np.eye(3)
        if not self.has_fore:
            v = self.wrist + Rh @ self.grip
            Ra = between(v, d)
            return Ra[:, 1], Ra[:, 1], abs(dist - float(np.linalg.norm(v)))

        def arm_vec(b):
            return self.elbow + rot((-b, 0, 0)) @ (self.wrist + Rh @ self.grip)
        lo, hi = 0.0, 145.0
        flo, fhi = np.linalg.norm(arm_vec(lo)) - dist, np.linalg.norm(arm_vec(hi)) - dist
        if flo <= 0:
            b = lo
        elif fhi >= 0:
            b = hi
        else:
            for _ in range(50):
                mid = (lo + hi) / 2
                if np.linalg.norm(arm_vec(mid)) - dist > 0:
                    lo = mid
                else:
                    hi = mid
            b = (lo + hi) / 2
        v = arm_vec(b)
        a = unit(d)
        Ra = between(v, d)
        el = Ra @ self.elbow
        e1 = el - np.dot(el, a) * a
        h1 = np.asarray(hint, float) - np.dot(hint, a) * a
        if np.linalg.norm(e1) > 1e-6 and np.linalg.norm(h1) > 1e-6:
            phi = math.degrees(math.atan2(float(np.dot(np.cross(unit(e1), unit(h1)), a)), float(np.dot(unit(e1), unit(h1)))))
            K = np.array([[0, -a[2], a[1]], [a[2], 0, -a[0]], [-a[1], a[0], 0]])
            r = math.radians(phi)
            Ra = (np.eye(3) + math.sin(r) * K + (1 - math.cos(r)) * K @ K) @ Ra
        res = abs(float(np.linalg.norm(v)) - dist)
        return Ra[:, 1], (Ra @ rot((-b, 0, 0)))[:, 1], res

    def solve(self, upper, fore=None, item=None, edge=None, hint=None, twist=None):
        """Net rotations {arm, forearm, hand} for the aims. The item's roll: `edge` (a direction), else `twist` degrees
        from the natural grip (the blade's flats square to the forearm's sides), else the smallest turn from the last
        solve."""
        u = unit(upper)
        f = unit(fore if fore is not None else upper)
        hint = self.prev.get('hint') if hint is None else hint
        if hint is None:
            hint = np.array([0.0, 0.0, 1.0])
        bend = angle(u, f) if self.has_fore else 0.0
        w = f - np.dot(f, u) * u
        Ra = frame(u, -w) if bend > 2.0 and np.linalg.norm(w) > 1e-6 else frame(u, hint)
        out = {self.arm: closest_euler(Ra, self.prev.get(self.arm))}
        Raf = Ra
        if self.has_fore:
            pf = self.prev.get(self.fore, (0.0, 0.0, 0.0))
            out[self.fore] = (-bend, pf[1], pf[2])
            Raf = Ra @ rot(out[self.fore])
        if self.has_hand:
            Rh0 = rot(self.prev.get(self.hand, self.troop.rest[self.hand][0]))
            if item is not None and self.tip is not None:
                if edge is None and twist is not None:
                    edge = turn(natural(Raf, item), item, twist)
                cur = Raf @ Rh0 @ self.tip
                Rw = between(cur, unit(item))
                if edge is not None:
                    b = unit(item)
                    have = Rw @ Raf @ Rh0 @ self.edge
                    have = have - np.dot(have, b) * b
                    want = np.asarray(edge, float) - np.dot(edge, b) * b
                    if np.linalg.norm(want) > 1e-6 and np.linalg.norm(have) > 1e-6:
                        have, want = unit(have), unit(want)
                        tw = math.atan2(float(np.dot(np.cross(have, want), b)), float(np.dot(have, want)))
                        K = np.array([[0, -b[2], b[1]], [b[2], 0, -b[0]], [-b[1], b[0], 0]])
                        Rw = (np.eye(3) + math.sin(tw) * K + (1 - math.cos(tw)) * K @ K) @ Rw
                H = Raf.T @ Rw @ Raf @ Rh0
            else:
                H = Rh0
            out[self.hand] = closest_euler(H, self.prev.get(self.hand))
        self.prev.update(out)
        self.prev['hint'] = Ra[:, 2]
        return out


def natural(Rf, item):
    """The natural grip's edge: square to the item and to the forearm's sides (its x axis)."""
    e = np.cross(Rf[:, 0], unit(item))
    return unit(e) if np.linalg.norm(e) > 1e-6 else unit(Rf[:, 2])


def turn(v, axis, deg):
    """v turned deg about axis (right hand)."""
    a = unit(axis)
    r = math.radians(deg)
    return v * math.cos(r) + np.cross(a, v) * math.sin(r) + a * np.dot(a, v) * (1 - math.cos(r))


def twist_of(base, edge, axis):
    """The signed angle (deg) about axis from base to edge, both taken square to axis."""
    a = unit(axis)
    b = base - np.dot(base, a) * a
    e = np.asarray(edge, float) - np.dot(edge, a) * a
    return math.degrees(math.atan2(float(np.dot(np.cross(b, e), a)), float(np.dot(b, e))))


def head_look(parent_R, gaze, prev=None):
    """Head channels (degrees) turning the head's -z onto a world gaze from a parent of world rotation parent_R, the
    roll kept level, each axis clamped to NECK."""
    g = parent_R.T @ unit(gaze)
    yaw = math.degrees(math.atan2(-g[0], -g[2]))
    pitch = math.degrees(math.atan2(g[1], math.hypot(g[0], g[2])))
    e = (max(-NECK[0], min(NECK[0], pitch)), max(-NECK[1], min(NECK[1], yaw)), 0.0)
    return e


def lag(values, times, delay):
    """A channel track delayed by `delay` s (secondary motion), held at its first value before."""
    v = np.asarray(values, float)
    return np.array([np.interp(t - delay, times, v[:, i] if v.ndim > 1 else v) for t in times]) if v.ndim == 1 else \
        np.stack([np.interp(np.asarray(times) - delay, times, v[:, i]) for i in range(v.shape[1])], 1)


def pchip_slopes(x, y):
    x, y = np.asarray(x, float), np.asarray(y, float)
    n = len(x)
    h = np.diff(x)
    d = np.diff(y) / h
    m = np.zeros(n)
    if n == 2:
        m[:] = d[0]
        return m
    for k in range(1, n - 1):
        if d[k - 1] * d[k] <= 0:
            m[k] = 0.0
        else:
            w1, w2 = 2 * h[k] + h[k - 1], h[k] + 2 * h[k - 1]
            m[k] = (w1 + w2) / (w1 / d[k - 1] + w2 / d[k])

    def end(h0, h1, d0, d1):
        s = ((2 * h0 + h1) * d0 - h0 * d1) / (h0 + h1)
        if np.sign(s) != np.sign(d0):
            return 0.0
        if np.sign(d0) != np.sign(d1) and abs(s) > abs(3 * d0):
            return 3 * d0
        return s
    m[0] = end(h[0], h[1], d[0], d[1])
    m[-1] = end(h[-1], h[-2], d[-1], d[-2])
    return m


def pchip(x, y, t):
    """Fritsch-Carlson monotone cubic through (x, y) at t (held flat past the ends)."""
    x, y = np.asarray(x, float), np.asarray(y, float)
    if len(x) == 1 or t <= x[0]:
        return float(y[0])
    if t >= x[-1]:
        return float(y[-1])
    m = pchip_slopes(x, y)
    k = int(np.searchsorted(x, t, side='right') - 1)
    h = x[k + 1] - x[k]
    s = (t - x[k]) / h
    h00, h10, h01, h11 = 2 * s ** 3 - 3 * s ** 2 + 1, s ** 3 - 2 * s ** 2 + s, -2 * s ** 3 + 3 * s ** 2, s ** 3 - s ** 2
    return float(h00 * y[k] + h10 * h * m[k] + h01 * y[k + 1] + h11 * h * m[k + 1])


class Leg:
    """One leg's IK on a troop: leg (hip) pitch, roll and the knee, the yaw given (toe-out)."""

    def __init__(self, troop, side):
        self.troop, self.side = troop, side
        self.leg, self.shin = f'{side}_leg', f'{side}_shin'
        self.foot = f'{side}_foot' if f'{side}_foot' in troop.by else None
        self.hip = troop.rest[self.leg][1]
        self.knee = troop.rest[self.shin][1]
        self.x = np.zeros(3)

    def sole(self, root_W, x, yaw):
        Rl = rot((x[0], yaw, x[1]))
        Rs = rot((x[2], 0, 0))
        p = self.hip + Rl @ (self.knee + Rs @ np.array([0.0, SOLE, 0.0]))
        return root_W[:3, :3] @ p + root_W[:3, 3]

    def knee_for(self, dist):
        """The knee (shin pitch >= 0) that puts the sole `dist` from the hip: the knee alone sets that distance."""
        k = self.knee
        a = float(k @ k) + SOLE * SOLE
        b = 2 * SOLE * float(k[1])
        c = (dist * dist - a) / b if abs(b) > 1e-9 else 1.0
        return math.degrees(math.acos(max(-1.0, min(1.0, c))))

    def solve(self, root_W, target, yaw=0.0, iters=30):
        """((leg pitch, yaw, roll), knee, residual px) putting the sole's centre on target (world). The knee comes from
        the hip-to-target distance (clamped to the guide's bounds); pitch and roll by damped Gauss-Newton from the last
        solve (seed() it from the stance)."""
        x = self.x.copy()
        target = np.asarray(target, float)
        hip = root_W[:3, :3] @ self.hip + root_W[:3, 3]
        x[2] = min(max(self.knee_for(float(np.linalg.norm(target - hip))), LEG_LO[2]), LEG_HI[2])
        for _ in range(iters):
            r = self.sole(root_W, x, yaw) - target
            if np.linalg.norm(r) < 1e-5:
                break
            J = np.zeros((3, 2))
            for j, i in enumerate((0, 1)):
                dx = np.zeros(3)
                dx[i] = 1e-3
                J[:, j] = (self.sole(root_W, x + dx, yaw) - self.sole(root_W, x - dx, yaw)) / 2e-3
            step = np.linalg.solve(J.T @ J + 1e-4 * np.eye(2), -J.T @ r)
            x[:2] = np.clip(x[:2] + step, LEG_LO[:2], LEG_HI[:2])
        self.x = x
        res = float(np.linalg.norm(self.sole(root_W, x, yaw) - target))
        return (float(x[0]), float(yaw), float(x[1])), float(x[2]), res

    def seed(self, net):
        """Start from a pose's net leg rotation and knee."""
        if self.leg in net and self.shin in net:
            self.x = np.array([net[self.leg][0], net[self.leg][2], net[self.shin][0]], float)
