"""Head clearance for the House sets (Kort: nothing clips the head). Vanilla armour already meets the helm at the shoulder:
the helmet hangs to 1 px under the neck and the chestplate's arms rise 1 px over the shoulder, so the two cross in that
band. What must not happen is shoulder or body gear rising past that band (above 1 px over the shoulder) into the helm or
bare head, at rest or with the head turned up to 50 degrees either way (the body turns with it past that). Pitch is
left out: the vanilla head already sinks into any chestplate looking down."""
import itertools, math, sys, os
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import houselib

NECK = np.array([0.0, 24.0, 0.0])
YAWS, PITCHES = range(-50, 51, 5), (0,)                  # head relative to the body (the body follows past ~50 deg)
BAND = 25.0                                              # vanilla chestplate top over the shoulder (y up)

def obb(b, above=None):
    """(centre, axes 3x3 rows, half extents) of an engine Box in its rest pose; `above` keeps only the part over that
    height (unrotated boxes), None when nothing is left."""
    lo, hi = (np.array(v, dtype=float) for v in b.lo_hi())
    if above is not None and not any(b.rot):
        if hi[1] <= above: return None
        lo[1] = max(lo[1], above)
    c, h, ax = (lo + hi) / 2, (hi - lo) / 2, np.eye(3)
    if any(b.rot):
        m = b.matrix(); o = np.array(b.origin, dtype=float); c = m @ (c - o) + o; ax = (m @ ax.T).T
    return c, ax, h

def turn(o, yaw, pitch):
    y, p = math.radians(yaw), math.radians(pitch)
    my = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    mx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)], [0, math.sin(p), math.cos(p)]])
    m = my @ mx; c, ax, h = o
    return m @ (c - NECK) + NECK, (m @ ax.T).T, h

def depth(a, b):
    """Overlap depth (px) of two oriented boxes by the separating axis test; 0 when apart."""
    (ca, A, ha), (cb, B, hb) = a, b
    axes = list(A) + list(B) + [np.cross(u, v) for u in A for v in B]
    d, best = cb - ca, float("inf")
    for L in axes:
        n = np.linalg.norm(L)
        if n < 1e-9: continue
        L = L / n
        ra = sum(ha[i] * abs(A[i] @ L) for i in range(3)); rb = sum(hb[i] * abs(B[i] @ L) for i in range(3))
        best = min(best, ra + rb - abs(d @ L))
        if best <= 0: return 0.0
    return best

def clashes(name, tol=0.05):
    """[(tier, head box, other box, worst depth, yaw, pitch)] where the turned head cuts into armour deeper than tol px."""
    s, out = houselib.armour_set(name), []
    for tier in ("base", "legendary"):
        bx = s.boxes_for(tier)
        head = [b for b in bx if b.bone == "head"] + [houselib.BODY_BY["p_head"]]
        rest = [b for b in bx if b.bone != "head"]
        for h, o in itertools.product(head, rest):
            oh, oo, worst = obb(h), obb(o, BAND), (0.0, 0, 0)
            if oo is None: continue
            for yaw, pitch in itertools.product(YAWS, PITCHES):
                d = depth(turn(oh, yaw, pitch), oo)
                if d > worst[0]: worst = (d, yaw, pitch)
            if worst[0] > tol: out.append((tier, h.name, o.name, round(worst[0], 2), worst[1], worst[2]))
    return out

if __name__ == "__main__":
    for n in sys.argv[1:] or houselib.ORDER:
        c = clashes(n)
        print(n, len(c))
        for x in c: print("   ", x)
