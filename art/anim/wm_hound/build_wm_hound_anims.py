"""The Moot Hound's clips (animation rebuild, v2), written to out/wm_hound.animation.json (GG_HOUND_OUT, if set, redirects it).

    python art/anim/wm_hound/build_wm_hound_anims.py

hit_pulled / hit_pulled_legs (hit_v1 flag 8: dragged toward its striker) are additive like the other hits: the forequarters lurch forward,
the head and chest are yanked toward the puller, the ears flatten, the front paws reach forward to catch it, the hind paws scramble after.

A quadruped on the knight's controllers. Layers: *_legs keys body (the rump, parent of all) and the twelve leg bones; *_upper,
attack and hit clips key chest, head, jaw, ears and tail. Paired clips share length and clock, so every paw is placed by IK
(hound_ik.py) against the combined pose: the sole goes where the clip says, flat or rolled, and planted paws hold still on the
ground while the body moves over them. Deaths key every animated bone (root included) and are lifted to rest on the floor.

Ground speed: walk_ covers 2.56 px/tick (GaitController ref .16), run_ 4.16 px/tick (ref .26). Both attacks land at tick 8
(Spec family hound: cooldown 20; the longest attack is 16 ticks).
"""
import json, math, os, sys
import numpy as np
from scipy.interpolate import PchipInterpolator

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import hound_ik as IK                                        # noqa: E402

TPS, SUB = 20, 4
ROT_TOL, POS_TOL = 0.02, 0.002
H = IK.Hound()

UPPER = ["chest", "head", "jaw", "ear_right", "ear_left", "tail", "throat_skull"]
LEGB = [f"{l}_{p}" for l in IK.LEGS for p in ("leg", "shin", "foot")]
LOWER = ["body"] + LEGB
ALL = ["root"] + LOWER + UPPER

REST_SOLE = {"front_right": np.array([-2.5, 0, 4.0]), "front_left": np.array([2.5, 0, 4.0]),
             "back_right": np.array([-2.0, 0, -7.5]), "back_left": np.array([2.0, 0, -7.5])}
TOE = 2.0   # the paw's toe (and heel) from the sole centre, px


# ------------------------------------------------------------------------------------------------ curves
def K(keys):
    """Monotone cubic through (t, value) keys; value scalar or sequence. Clamped outside."""
    ts = np.array([k[0] for k in keys], float)
    vs = np.array([np.atleast_1d(np.array(k[1], float)) for k in keys])
    f = PchipInterpolator(ts, vs, axis=0, extrapolate=False)
    scalar = np.ndim(keys[0][1]) == 0

    def g(t):
        t = min(max(t, ts[0]), ts[-1])
        v = f(t)
        return float(v[0]) if scalar else v
    return g


def KL(keys, period):
    """K over a loop: the keys repeat every `period` ticks (the first key's value is used again at period)."""
    ext = [(t - period, v) for t, v in keys] + list(keys) + [(t + period, v) for t, v in keys]
    f = K(ext)
    return lambda t: f(t % period)


def ss(x):
    x = min(max(x, 0.0), 1.0)
    return x * x * (3 - 2 * x)


def env(t, a, b, c, d):
    """0 before a, eases to 1 by b, holds, eases back to 0 from c to d."""
    return ss((t - a) / max(b - a, 1e-9)) * (1 - ss((t - c) / max(d - c, 1e-9)))


def wave(t, period, phase=0.0):
    return math.sin(2 * math.pi * (t / period - phase))


def damp(t, t0, freq, decay):
    """A decaying shake starting at t0."""
    if t < t0:
        return 0.0
    x = t - t0
    return math.sin(2 * math.pi * x / freq) * math.exp(-x / decay)


# ------------------------------------------------------------------------------------------------ paws
def planted(base, pitch=0.0, about="toe"):
    """Sole centre and pitch for a paw resting at `base` (its flat sole centre) rolled by `pitch` (deg, + toe up) about its
    toe (toe stays put) or heel."""
    th = math.radians(pitch)
    if abs(th) < 1e-9:
        return np.array(base, float), 0.0
    if about == "toe":
        tip = np.array(base, float) + [0, 0, TOE]
        return tip - TOE * np.array([0, math.sin(th), math.cos(th)]), pitch
    heel = np.array(base, float) - [0, 0, TOE]
    return heel + TOE * np.array([0, math.sin(th), math.cos(th)]), pitch


def lifted(p, pitch):
    """Keeps a swinging paw's lowest corner above the floor."""
    p = np.array(p, float)
    p[1] = max(p[1], TOE * abs(math.sin(math.radians(pitch))) + 0.05)
    return p, pitch


def hermite(s, p0, p1, v0, v1):
    h00 = 2 * s ** 3 - 3 * s ** 2 + 1
    h10 = s ** 3 - 2 * s ** 2 + s
    h01 = -2 * s ** 3 + 3 * s ** 2
    h11 = s ** 3 - s ** 2
    return h00 * p0 + h10 * v0 + h01 * p1 + h11 * v1


def step(base0, base1, s, lift, fold, front, toe_up=0.0):
    """A paw stepping from `base0` to `base1` (flat sole centres), s 0..1: lifted heel-first, folded, reached, set down flat."""
    s = min(max(s, 0.0), 1.0)
    if s <= 0:
        return planted(base0)
    if s >= 1:
        return planted(base1)
    b0, b1 = np.array(base0, float), np.array(base1, float)
    p = b0 + (b1 - b0) * ss(s)
    sk = s ** 0.8
    p[1] += lift * 4 * sk * (1 - sk)
    pitch = K([(0, 0), (0.3, -fold), (0.7, -fold * 0.3), (0.86, toe_up), (1, 0)])(s)
    return lifted(p, pitch)


def gait_paw(u, D, period, v, z0, x0, lift, fold, roll, front, reach_up=0.0):
    """One paw of a looping gait at phase u (0..1, 0 = touchdown): stance for u < D (planted, moving back at v px/tick in the
    troop's frame, rolling onto the toe at the end), then the swing forward (velocity matched at both ends)."""
    L = v * period * D
    zt, zl = z0 + L / 2, z0 - L / 2
    if u < D:
        s = u / D
        toe = zt + TOE - s * L
        th = -roll * ss((s - 0.62) / 0.38)
        r = math.radians(th)
        return np.array([x0, -TOE * math.sin(r), toe - TOE * math.cos(r)]), th
    s = (u - D) / (1 - D)
    T = (1 - D) * period
    r = math.radians(-roll)
    y0, z_lo = -TOE * math.sin(r), zl + TOE - TOE * math.cos(r)
    z = hermite(s, z_lo, zt, -v * T, -v * 0.45 * T)
    sk = s ** 0.7
    y = y0 * (1 - s) ** 2 + lift * 4 * sk * (1 - sk)
    pitch = K([(0, -roll), (0.32, -fold), (0.72, -fold * 0.2), (0.88, reach_up), (1, 0)])(s)
    return lifted([x0, y, z], pitch)


# ------------------------------------------------------------------------------------------------ solve
class Solver:
    def __init__(s):
        s.guess = {}

    def legs(s, pose, feet, tag):
        """Leg bone rotations for the paws in `feet` {leg: (sole, pitch)}; FK legs (already in pose) are left as they are."""
        M = H.fk(pose, ["chest", "body"])
        worst = 0.0
        for leg, (tgt, pitch) in feet.items():
            par = "chest" if leg.startswith("front") else "body"
            key = (tag, leg)
            g = s.guess.get(key)
            if g is None:
                g = [-10, 0, 35, -25] if leg.startswith("front") else [20, 0, -40, 20]
            sol, err = H.solve_leg(pose, leg, np.asarray(tgt, float), pitch, g, M[par])
            # try the textbook bend; not in a death, where a hind leg folded to its limit under the sinking rump has no exact
            # answer and the retry flipped the paw between two misses (death_back t7.25: 44 degrees in a quarter tick):
            # there the leg keeps following its own last pose
            if err > 0.08 and not tag.startswith("death_"):
                alt = [-10, 0, 35, -25] if leg.startswith("front") else [20, 0, -40, 20]
                sol2, err2 = H.solve_leg(pose, leg, np.asarray(tgt, float), pitch, alt, M[par])
                if err2 < err:
                    sol, err = sol2, err2
            s.guess[key] = sol
            worst = max(worst, err)
            lx, lz, sx, fx = sol
            pose[leg + "_leg"] = {"rotation": [lx, 0, lz]}
            pose[leg + "_shin"] = {"rotation": [sx, 0, 0]}
            pose[leg + "_foot"] = {"rotation": [fx, 0, 0]}
        return worst


def full(pose):
    for b in ALL:
        pose.setdefault(b, {})
        pose[b].setdefault("rotation", [0, 0, 0])
    return pose


# ------------------------------------------------------------------------------------------------ bake (as art/anim/knight)
def unwrap(seq):
    out = [seq[0]]
    for a in seq[1:]:
        while a - out[-1] > 180:
            a -= 360
        while a - out[-1] < -180:
            a += 360
        out.append(a)
    return out


def thin(vals, tol):
    n = len(vals)
    keep, a = [0], 0
    while a < n - 1:
        b = a + 1
        while b + 1 < n:
            ok = True
            for m in range(a + 1, b + 1):
                f = (m - a) / (b + 1 - a)
                for ax in range(len(vals[0])):
                    if abs(vals[a][ax] + (vals[b + 1][ax] - vals[a][ax]) * f - vals[m][ax]) > tol:
                        ok = False
                        break
                if not ok:
                    break
            if not ok:
                break
            b += 1
        keep.append(b)
        a = b
    return keep


def fmt_t(sec):
    return ("%.4f" % sec).rstrip("0").rstrip(".") if sec else "0.0"


def bake(samples, ticks, loop=True, bones=None, timeline=None):
    names = bones or sorted({b for smp in samples for b in smp})
    out = {}
    for b in names:
        ch = {}
        rots = [smp[b].get("rotation", [0, 0, 0]) for smp in samples]
        rv = [[round(x, 3) for x in r] for r in zip(*[unwrap(list(ax)) for ax in zip(*rots)])]
        ch["rotation"] = {fmt_t(i / SUB / TPS): rv[i] for i in thin(rv, ROT_TOL)}
        if any("position" in smp[b] for smp in samples):
            ps = [[round(x, 4) for x in smp[b].get("position", [0, 0, 0])] for smp in samples]
            ch["position"] = {fmt_t(i / SUB / TPS): ps[i] for i in thin(ps, POS_TOL)}
        out[b] = ch
    clip = {"loop": loop, "animation_length": round(ticks / TPS, 6), "bones": out}
    if timeline:
        clip["timeline"] = timeline
    return clip


# ------------------------------------------------------------------------------------------------ the floor (deaths)
class _PoseScene(IK.R.Scene):
    def __init__(s, model):
        super().__init__(model, None, [], 0)
        s.pose = {}

    def _pose(s, tick):
        out = {}
        for b, p in s.pose.items():
            d = {}
            if "rotation" in p:
                x, y, z = p["rotation"]
                d["rotation"] = np.radians([-x, -y, z])
            if "position" in p:
                d["position"] = tuple(p["position"])
            out[b] = d
        return out


SCENE = _PoseScene(H.m)


def lowest(pose, bone=None):
    SCENE.pose = pose
    fr = SCENE.evaluate(0)
    bones = {bone} if isinstance(bone, str) else bone
    c = fr.corners if bone is None else fr.corners[[i for i, b in enumerate(H.m.cube_bone) if b in bones]]
    return float(c[:, :, 1].min()) * 16.0


HEAD_GEAR = {"head", "muzzle", "jaw", "fangs", "fangs_t2", "fangs_t3", "ear_right", "ear_left", "toggles", "skullcap",
             "antler_tuft_right", "antler_tuft_left"}
HEAD_CLEAR = 0.3


NECK_GEAR = {"collar", "tooth_collar"}


def head_clear(pose):
    """The muzzle, fangs and neck collars never go into the ground: where the authored pose would put them there, the head
    and then the chest (for what still touches, the collars) pitch up just enough, so a snap at the legs stops at the
    turf. Runs before the legs are solved; continuous in the pose."""
    _clear(pose, "head", HEAD_GEAR, 30.0)
    _clear(pose, "chest", HEAD_GEAR | NECK_GEAR, 30.0)


def _clear(pose, joint, gear, most):
    if lowest(pose, gear) >= HEAD_CLEAR:
        return
    h = pose.setdefault(joint, {}).setdefault("rotation", [0, 0, 0])
    rx0 = h[0]

    def at(d):
        h[0] = rx0 - d
        return lowest(pose, gear)
    lo, hi = 0.0, most
    if at(hi) < HEAD_CLEAR:
        return
    for _ in range(12):
        mid = (lo + hi) / 2
        if at(mid) < HEAD_CLEAR:
            lo = mid
        else:
            hi = mid
    h[0] = rx0 - hi


PENDANT_CLEAR = 0.5   # px the tier III throat skull keeps above the floor


def pendant(pose, keep=False):
    """The throat skull hangs from the collar: when the head comes low it swings back against the throat (and rides up it)
    rather than sink into the ground. Continuous in the pose, so baked keys stay smooth. keep: start from the swing hang()
    gave it and only take it further back where the floor needs it."""
    ry = rz = rx0 = 0          # ints, as before: the clips that do not swing stay byte-identical
    if keep:
        rx0, ry, rz = pose.setdefault("throat_skull", {}).get("rotation", [0, 0, 0])
    pose.setdefault("throat_skull", {})["rotation"] = [rx0, ry, rz]
    pose["throat_skull"].pop("position", None)
    if lowest(pose, "throat_skull") >= PENDANT_CLEAR:
        return

    def at(rx):
        pose["throat_skull"]["rotation"] = [rx, ry, rz]
        return lowest(pose, "throat_skull")
    lo, hi = rx0, max(rx0, 75.0)
    if at(hi) < PENDANT_CLEAR:
        need = PENDANT_CLEAR - at(hi)
        pose["throat_skull"]["position"] = [0, need, 0]
        return
    for _ in range(12):
        mid = (lo + hi) / 2
        if at(mid) < PENDANT_CLEAR:
            lo = mid
        else:
            hi = mid
    pose["throat_skull"]["rotation"] = [hi, ry, rz]


# ------------------------------------------------------------------------------------------------ hanging gear
# Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor". What HANGS from the head swings in
# the action clips (attacks, medium hits, the pull): the tier III throat skull and the tier II/III tooth collar, each a weight
# on a damped spring behind the head, hinged where the bone is (the neck joint), with a slow swing of its own so a held pose
# is not a statue. The tier III antler crown (antler_tuft_*) is bone grown from the skull: like a helm's fixed crest it rides
# the head and gets no keys (knight round 3: "remove the wobble of the red strip on his head"). The ears and tail are keyed.
#   period ticks, zeta damping, plumb how far it keeps hanging straight down as the head turns, most (x, z) degrees, sway deg
HANG = {"throat_skull": dict(period=4.2, zeta=0.32, plumb=0.55, most=(26.0, 12.0), sway=2.0, phase=0.0),
        "tooth_collar": dict(period=3.4, zeta=0.40, plumb=0.35, most=(12.0, 7.0), sway=1.4, phase=0.37)}
HANG_BONES = ["tooth_collar"]           # keyed only by the clips that swing them (throat_skull is in UPPER already)
HANG_FADE_IN, HANG_FADE_OUT = 1.0, 2.0   # ticks: every clip still starts and ends on its stance exactly


def _piece_centroid_local(name):
    """The centroid of a bone's cubes, in its own (unposed) model blocks, homogeneous."""
    idx = [i for i, b in enumerate(H.m.cube_bone) if b == name]
    c = np.concatenate([H.m.cube_list[i].corners for i in idx]).mean(axis=0)
    return np.append(c, 1.0)


def hang(samples, ticks):
    """Swings every HANG piece on a spring behind the head (world space, sub-stepped per quarter tick), then keeps each clear of
    the floor as pendant() does: the throat skull rides back up the throat, the tooth collar swings less. In place."""
    n = len(samples) - 1
    dt = 1.0 / SUB
    for name, c in HANG.items():
        if name not in H.m.bones:
            continue
        cl = _piece_centroid_local(name)
        pv = np.append(H.m.bones[name].pivot / 16.0, 1.0)
        # the spring swings about the piece's free hang; the throat skull's floor push (pendant) is laid on after it
        key0 = [[0.0, 0.0, 0.0] if name == "throat_skull" else list(smp.get(name, {}).get("rotation", [0, 0, 0]))
                for smp in samples]
        if name == "throat_skull":
            for smp in samples:
                smp[name] = {"rotation": [0.0, 0.0, 0.0]}
        Ms = [H.fk(smp, ["head"])["head"] for smp in samples]
        rigid = []
        P = []
        for M, k in zip(Ms, key0):
            Mp = M @ H.local(name, {"rotation": k})
            rigid.append((Mp @ cl)[:3] * 16.0)
            P.append((Mp @ pv)[:3] * 16.0)
        L = float(np.linalg.norm(rigid[0] - P[0]))
        if L < 0.5:
            continue
        d0 = (rigid[0] - P[0]) / L
        w = 2 * math.pi / c["period"]
        k_s, k_d = w * w, 2 * c["zeta"] * w
        X, V = rigid[0].copy(), np.zeros(3)
        out = []
        for i in range(n + 1):
            t = i * dt
            dr = (rigid[i] - P[i]) / L
            g = dr * (1 - c["plumb"]) + d0 * c["plumb"]
            sw = c["sway"] * math.radians(1) * L * math.sin(2 * math.pi * (t / (1.5 * c["period"]) + c["phase"]))
            side = np.cross(dr, [0.0, 0.0, 1.0])
            side = side / (np.linalg.norm(side) + 1e-9)
            G = P[i] + L * g / np.linalg.norm(g) + sw * side
            if i > 0:
                for _ in range(4):
                    h = dt / 4
                    A = k_s * (G - X) - k_d * V
                    V = V + A * h
                    X = X + V * h
            u = X - P[i]
            X = P[i] + L * u / (np.linalg.norm(u) + 1e-9)
            out.append(X.copy())
        need, swung = [], []
        for i, smp in enumerate(samples):
            t = i * dt
            fade = ss(min(1.0, t / HANG_FADE_IN, (ticks - t) / HANG_FADE_OUT))
            base = key0[i]
            Mh = Ms[i]

            def res(v, base=base, Mh=Mh, tgt=out[i]):
                Mp = Mh @ H.local(name, {"rotation": [base[0] + v[0], base[1], base[2] + v[1]]})
                return (Mp @ cl)[:3] * 16.0 - tgt
            from scipy.optimize import least_squares as _ls
            v = _ls(res, [0.0, 0.0], xtol=1e-6, ftol=1e-6).x
            vx = max(-c["most"][0], min(c["most"][0], float(v[0]))) * fade
            vz = max(-c["most"][1], min(c["most"][1], float(v[1]))) * fade
            ent = smp.setdefault(name, {})
            ent["rotation"] = [base[0] + vx, base[1], base[2] + vz]
            if name == "throat_skull":
                pendant(smp, keep=True)
                need.append(smp[name]["rotation"][0] - (base[0] + vx))
                swung.append(base[0] + vx)
            else:
                rest = min(HEAD_CLEAR, lowest({**smp, name: {"rotation": base}}, name))
                s_ = 1.0
                while lowest(smp, name) < rest - 1e-3 and s_ > 0:
                    s_ = max(0.0, s_ - 0.1)
                    ent["rotation"] = [base[0] + vx * s_, base[1], base[2] + vz * s_]
        if need:
            # the floor's push back against the throat comes on as a swing, not in one frame (attack_lunge t8.75 snapped 47
            # degrees): the push each sample needs, spread to its neighbours at PENDANT_RATE so the skull is already
            # swinging back as the head comes down onto the turf, then pendant() again for what is left
            rate = PENDANT_RATE / SUB
            for i, smp in enumerate(samples):
                fade = ss(min(1.0, i * dt / HANG_FADE_IN, (ticks - i * dt) / HANG_FADE_OUT))   # first and last: the stance's
                extra = max(need[i], fade * max(max(0.0, nd - rate * abs(i - j)) for j, nd in enumerate(need)))
                r = smp[name]["rotation"]
                smp[name]["rotation"] = [min(75.0, swung[i] + extra), r[1], r[2]]
                smp[name].pop("position", None)
                pendant(smp, keep=True)


PENDANT_RATE = 22.0   # deg per tick the floor may swing the throat skull back


# Review 2026-11-12 (F4): the rest of what rides the hound lags the body in EVERY clip, not only the actions: the tooth
# collar and the bone toggles hang from the neck, the sinew collar sits loose on it, the saddle cloth lies over the shoulders
# and the rump. Each is a damped spring of its centroid behind its parent bone (as hang(): plumb keeps it hanging down as the
# parent turns, sway a slow swing of its own); loops run the spring to its steady state and start at their stance's rest.
#   parent, period ticks, zeta damping, plumb, most (x, z) degrees, sway deg, phase
LAG = {"tooth_collar": dict(parent="head", period=3.4, zeta=0.40, plumb=0.35, most=(12.0, 7.0), sway=1.6, phase=0.37),
       "toggles": dict(parent="head", period=2.8, zeta=0.30, plumb=0.50, most=(16.0, 10.0), sway=3.0, phase=0.15),
       "collar": dict(parent="head", period=3.0, zeta=0.45, plumb=0.20, most=(2.5, 2.0), sway=0.6, phase=0.6),   # a ring on the neck: short lever, small
       "saddle": dict(parent="chest", period=3.6, zeta=0.40, plumb=0.15, most=(5.0, 4.0), sway=1.4, phase=0.2),
       "saddle_rump": dict(parent="body", period=3.6, zeta=0.40, plumb=0.15, most=(5.0, 4.0), sway=1.4, phase=0.7)}
GEAR_UP = ["tooth_collar", "toggles", "collar", "saddle"]   # the upper layer (children of the chest and head)
GEAR_LEGS = ["saddle_rump"]                                  # the legs layer (a child of the body)


def lag_gear(samples, ticks, loop, names, fade_out=True, centre=False):
    """Lags each named LAG piece behind its parent, in place. Every sample gets the piece's rotation (zero where it has none)."""
    n = len(samples) - 1
    dt = 1.0 / SUB
    m = n if loop else n + 1          # a loop's last sample is its first
    from scipy.optimize import least_squares as _ls
    for name in names:
        for smp in samples:
            smp.setdefault(name, {}).setdefault("rotation", [0, 0, 0])
        if name not in H.m.bones:
            continue
        c = LAG[name]
        cl = _piece_centroid_local(name)
        pv = np.append(H.m.bones[name].pivot / 16.0, 1.0)
        key0 = [list(smp[name]["rotation"]) for smp in samples]
        Ms = [H.fk(smp, [c["parent"]])[c["parent"]] for smp in samples]
        rigid, P = [], []
        for M, k in zip(Ms, key0):
            Mp = M @ H.local(name, {"rotation": k})
            rigid.append((Mp @ cl)[:3] * 16.0)
            P.append((Mp @ pv)[:3] * 16.0)
        L = float(np.linalg.norm(rigid[0] - P[0]))
        if L < 0.5:
            continue
        d0 = (rigid[0] - P[0]) / L
        w = 2 * math.pi / c["period"]
        k_s, k_d = w * w, 2 * c["zeta"] * w
        sp = 1.5 * c["period"]
        if loop:                           # a whole number of swings per loop
            sp = ticks / max(1, round(ticks / sp))
        X, V = rigid[0].copy(), np.zeros(3)
        for rep in range(3 if loop else 1):
            out = []
            for i in range(m):
                t = i * dt
                dr = (rigid[i] - P[i]) / L
                g = dr * (1 - c["plumb"]) + d0 * c["plumb"]
                sw = c["sway"] * math.radians(1) * L * math.sin(2 * math.pi * (t / sp + c["phase"]))
                side = np.cross(dr, [0.0, 0.0, 1.0])
                side = side / (np.linalg.norm(side) + 1e-9)
                G = P[i] + L * g / np.linalg.norm(g) + sw * side
                if i > 0 or rep > 0:
                    for _ in range(4):
                        h = dt / 4
                        A = k_s * (G - X) - k_d * V
                        V = V + A * h
                        X = X + V * h
                u = X - P[i]
                X = P[i] + L * u / (np.linalg.norm(u) + 1e-9)
                out.append(X.copy())
        vs = []
        for i in range(m):
            base, Mh, tgt = key0[i], Ms[i], out[i]

            def res(v, base=base, Mh=Mh, tgt=tgt):
                Mp = Mh @ H.local(name, {"rotation": [base[0] + v[0], base[1], base[2] + v[1]]})
                return (Mp @ cl)[:3] * 16.0 - tgt
            v = _ls(res, [0.0, 0.0], xtol=1e-6, ftol=1e-6).x
            vs.append([c["most"][0] * math.tanh(float(v[0]) / c["most"][0]), c["most"][1] * math.tanh(float(v[1]) / c["most"][1])])  # soft: no flat tops
        if loop:                           # a stance starts at its rest, where an attack or a hit picks it up; a gait swings about its mean
            if centre:                     # a gait's stride is short: keep only the lag's swing at the stride's own rate, smooth across the wrap
                hz = [[sum(v[j] * f(2 * math.pi * i / m) for i, v in enumerate(vs)) * 2 / m for f in (math.cos, math.sin)] for j in (0, 1)]
                vs = [[hz[j][0] * math.cos(2 * math.pi * i / m) + hz[j][1] * math.sin(2 * math.pi * i / m) for j in (0, 1)] for i in range(m)]
            else:
                v0 = list(vs[0])
                vs = [[a - v0[0], b - v0[1]] for a, b in vs]
        need, sw_ = [], []
        for i in range(m):
            t = i * dt
            fade = 1.0 if loop else ss(min(1.0, t / HANG_FADE_IN, (ticks - t) / HANG_FADE_OUT if fade_out else 1.0))
            base = key0[i]
            smp = samples[i]
            vx, vz = vs[i][0] * fade, vs[i][1] * fade
            sw_.append((vx, vz))
            smp[name] = {"rotation": [base[0] + vx, base[1], base[2] + vz]}
            rest = min(HEAD_CLEAR, lowest({**smp, name: {"rotation": base}}, name))
            s_ = 1.0
            while lowest(smp, name) < rest - 1e-3 and s_ > 0:      # never lower than it hangs unswung
                s_ = max(0.0, s_ - 0.05)
                smp[name] = {"rotation": [base[0] + vx * s_, base[1], base[2] + vz * s_]}
            need.append(s_)
        r = 2 * SUB                        # that guard as a smooth envelope: erode then average, never above any sample's need
        at = (lambda j: need[j % m]) if loop else (lambda j: need[min(max(j, 0), m - 1)])
        ero = [min(at(j) for j in range(i - r, i + r + 1)) for i in range(-r, m + r)]
        for i in range(m):
            e = sum(ero[i:i + 2 * r + 1]) / (2 * r + 1)
            base = key0[i]
            samples[i][name] = {"rotation": [base[0] + sw_[i][0] * e, base[1], base[2] + sw_[i][1] * e]}


# ------------------------------------------------------------------------------------------------ shared poses
STAND_Y = -0.5


def stand_upper(t, breath=40.0, k=1.0):
    b = wave(t, breath)
    return {"chest": {"rotation": [0.6 * b * k, 0, 0]},
            "head": {"rotation": [-0.8 * b * k, 0, 0]},
            "jaw": {"rotation": [1.5 + 1.0 * k * max(0, b), 0, 0]},
            "ear_right": {"rotation": [0, 0, 0]}, "ear_left": {"rotation": [0, 0, 0]},
            "tail": {"rotation": [6, 0, 0]}}


# ready stance: on the leash, waiting for the word
READY_P = 40.0


def ready_pose(t):
    b = wave(t, 10)                       # fast, eager breathing
    sway = wave(t, READY_P)
    trem = math.sin(2 * math.pi * t / 2.5) * 0.35   # the growl
    up = {"chest": {"rotation": [5 + 0.8 * b, 1.2 * sway, 0]},
          "head": {"rotation": [-9 - 0.8 * b + trem * 0.5, -2.5 * sway, 0]},
          "jaw": {"rotation": [5 + 1.5 * max(0, b) + trem, 0, 0]},
          "ear_right": {"rotation": [-6, 0, 4]}, "ear_left": {"rotation": [-6, 0, -4]},
          "tail": {"rotation": [24 + 1.2 * trem, 4 * wave(t, READY_P, 0.25), 0]}}
    body = {"position": [0.25 * sway, -1.5 + 0.12 * b, -0.6], "rotation": [3.0, -1.0 * sway, 1.0 * sway]}
    feet = {"front_right": planted(REST_SOLE["front_right"] + [-0.3, 0, 0.8]),
            "front_left": planted(REST_SOLE["front_left"] + [0.3, 0, 0.8]),
            "back_right": planted(REST_SOLE["back_right"] + [-0.2, 0, 0.6]),
            "back_left": planted(REST_SOLE["back_left"] + [0.2, 0, 0.6])}
    return up, body, feet


# ------------------------------------------------------------------------------------------------ clips
CLIPS = {}
SOLVER = Solver()


def sample_clip(name, ticks, fn, loop=True, tag=None, layers=("legs", "upper"), timeline=None, solve=True, hang_gear=False):
    """fn(t) -> (upper dict, body dict, feet dict, extra dict). Writes name_legs and name_upper (or the single names given).
    hang_gear: swing the head's hanging gear (hang()) once the clip is sampled."""
    tag = tag or name
    n = int(round(ticks * SUB))
    samples, worst = [], 0.0
    for i in range(n + 1):
        t = i / SUB
        if loop and i == n:
            samples.append(samples[0]); break
        up, body, feet, *extra = fn(t)
        pose = {k: dict(v) for k, v in up.items()}
        pose["body"] = dict(body)
        if extra:
            for k, v in extra[0].items():
                pose[k] = dict(v)
        head_clear(pose)
        if solve:
            worst = max(worst, SOLVER.legs(pose, feet, tag))
        pendant(pose)
        samples.append(full(pose))
    if hang_gear:
        hang(samples, ticks)
        for smp in samples:
            smp.setdefault("tooth_collar", {}).setdefault("rotation", [0, 0, 0])
    lag_gear(samples, ticks, loop, [g for g in GEAR_UP + GEAR_LEGS if not (hang_gear and g in HANG)], centre=name in ("walk", "run"))
    return samples, worst


def emit(name, ticks, samples, loop, split=True, timeline=None, legs_name=None, upper_name=None):
    if split:
        CLIPS[upper_name or name + "_upper"] = bake(samples, ticks, loop, UPPER + GEAR_UP, timeline)
        CLIPS[legs_name or name + "_legs"] = bake(samples, ticks, loop, LOWER + GEAR_LEGS)
    else:
        CLIPS[name] = bake(samples, ticks, loop, ALL, timeline)


# idle -------------------------------------------------------------------------------------------
IDLE_P = 60.0


def idle(t):
    up = stand_upper(t, 30.0)
    look = K([(0, 0), (8, 0), (13, 1), (22, 1), (27, 0), (34, 0), (38, -0.6), (44, -0.6), (48, 0), (60, 0)])(t)
    sniff = env(t, 36, 39, 44, 47)
    snf = math.sin(2 * math.pi * t / 1.5) * sniff
    up["head"]["rotation"] = [up["head"]["rotation"][0] + 14 * sniff - 2 * snf, 16 * look, 3 * look]
    up["chest"]["rotation"][1] = 3 * look
    up["chest"]["rotation"][0] += 3 * sniff
    flick_r = damp(t, 26, 2.0, 1.4) * 1.0
    flick_l = damp(t, 50, 2.0, 1.4) * 1.0
    up["ear_right"]["rotation"] = [-14 * flick_r, 0, 10 * flick_r]
    up["ear_left"]["rotation"] = [-14 * flick_l, 0, -10 * flick_l]
    up["tail"]["rotation"] = [4 + 2 * wave(t, 30), 9 * wave(t, 30, 0.1), 0]
    shift = wave(t, IDLE_P, 0.1)
    body = {"position": [0.35 * shift, STAND_Y + 0.1 * wave(t, 30), 0], "rotation": [0, 0, 1.2 * shift]}
    feet = {l: planted(REST_SOLE[l]) for l in IK.LEGS}
    # a fidget: the front left paw lifts and is set down a touch further out
    s = (t - 40) / 6
    if 0 < s < 1:
        feet["front_left"] = step(REST_SOLE["front_left"], REST_SOLE["front_left"], s, 1.2, 25, True)
    return up, body, feet


# walk: a trot, diagonal pairs ----------------------------------------------------------------------
WALK_P, WALK_V, WALK_D = 6.0, 2.56, 0.32
WALK_Y = -0.75
# Where each gait loop's seam falls (fraction of a cycle). The paw strikes are the sharpest turns of the shins; the seam sits
# between them (chosen where every bone, the shins and the swinging pendant included, turns least across the wrap) so the loop wraps
# through smooth motion. Only the clip's start moves: GaitController picks its starting phase by pose.
WALK_SEAM, RUN_SEAM = 0.08, 0.125


def walk(t):
    t = t + WALK_SEAM * WALK_P
    u0 = t / WALK_P
    ph = {"front_right": 0.0, "back_left": 0.02, "front_left": 0.5, "back_right": 0.52}
    feet = {}
    for l, p in ph.items():
        front = l.startswith("front")
        rs = REST_SOLE[l]
        feet[l] = gait_paw((u0 - p) % 1, WALK_D, WALK_P, WALK_V, rs[2] + (0.4 if front else -0.2), rs[0],
                           1.6 if front else 1.3, 45 if front else 30, 18 if front else 15, front, 5 if front else 0)
    bob = math.cos(4 * math.pi * (u0 - WALK_D / 2))   # 1 at mid-stance of each pair
    sway = wave(t, WALK_P, 0.15)
    body = {"position": [0.15 * sway, WALK_Y - 0.35 * bob, 0], "rotation": [0.4 * bob, 2.0 * sway, -1.2 * sway]}
    lag = math.cos(4 * math.pi * (u0 - WALK_D / 2 - 0.12))
    up = {"chest": {"rotation": [-0.6 * bob, -1.5 * sway, 1.0 * sway]},
          "head": {"rotation": [-3 + 0.5 * bob, -1.0 * sway, 0]},
          "jaw": {"rotation": [3 + 1.5 * max(0, lag), 0, 0]},
          "ear_right": {"rotation": [3 * lag - 2, 0, 0]}, "ear_left": {"rotation": [3 * lag - 2, 0, 0]},
          "tail": {"rotation": [14 + 3 * lag, 10 * wave(t, WALK_P, 0.35), 0]}}
    return up, body, feet


# run: a rotary gallop --------------------------------------------------------------------------------
RUN_P, RUN_V = 5.0, 4.16
RUN_Y = -0.9


def run(t):
    t = t + RUN_SEAM * RUN_P
    u0 = t / RUN_P
    ph = {"back_left": 0.0, "back_right": 0.1, "front_right": 0.45, "front_left": 0.56}
    feet = {}
    for l, p in ph.items():
        front = l.startswith("front")
        rs = REST_SOLE[l]
        D = 0.22 if front else 0.24
        feet[l] = gait_paw((u0 - p) % 1, D, RUN_P, RUN_V, rs[2] + (0.9 if front else -0.6), rs[0],
                           2.4 if front else 2.0, 60 if front else 40, 25 if front else 20, front, 8 if front else 0)
    e = math.cos(2 * math.pi * (u0 - 0.38))      # +1 extended, -1 gathered
    hgt = math.cos(2 * math.pi * (u0 - 0.92))
    body = {"position": [0, RUN_Y + 0.55 * hgt, -0.3 * e], "rotation": [3.0 * e, 0, 0]}
    lag = math.cos(2 * math.pi * (u0 - 0.38 - 0.15))
    up = {"chest": {"rotation": [-6.0 * e, 0, 0]},
          "head": {"rotation": [-2 + 3.0 * e - 1.5 * hgt, 0, 0]},          # held steady against the spine
          "jaw": {"rotation": [9 + 3 * lag, 0, 0]},
          "ear_right": {"rotation": [-22 - 4 * lag, 0, 8]}, "ear_left": {"rotation": [-22 - 4 * lag, 0, -8]},
          "tail": {"rotation": [32 + 6 * lag, 4 * wave(t, RUN_P, 0.3), 0]}}
    return up, body, feet


# stances --------------------------------------------------------------------------------------------
def ready(t):
    return ready_pose(t)


def brace(t):
    b = wave(t, 20)
    trem = math.sin(2 * math.pi * t / 2.5)
    up = {"chest": {"rotation": [7 + 0.7 * b, 0, 0]},
          "head": {"rotation": [6 - 0.6 * b + 0.4 * trem, 1.5 * wave(t, 40), 0]},
          "jaw": {"rotation": [14 + 2 * max(0, b) + 0.8 * trem, 0, 0]},
          "ear_right": {"rotation": [16, 0, 14]}, "ear_left": {"rotation": [16, 0, -14]},
          "tail": {"rotation": [-6 + 1.5 * b, 3 * wave(t, 40, 0.3), 0]}}
    body = {"position": [0, -2.0 + 0.15 * b, 0.9], "rotation": [4.5, 0, 0]}
    feet = {"front_right": planted(REST_SOLE["front_right"] + [-1.2, 0, 1.6]),
            "front_left": planted(REST_SOLE["front_left"] + [1.2, 0, 1.6]),
            "back_right": planted(REST_SOLE["back_right"] + [-0.6, 0, -0.2]),
            "back_left": planted(REST_SOLE["back_left"] + [0.6, 0, -0.2])}
    return up, body, feet


def wall(t):
    """Hold the line: a sentry in the formed line. Stiff-legged and square, chest up, head carried high and dead still,
    ears pricked forward, tail up and stiff; only a slow breath and one sharp ear twitch."""
    b = wave(t, 40)
    tw = damp(t, 24, 2.0, 1.2)
    up = {"chest": {"rotation": [-5 + 0.4 * b, 0, 0]},
          "head": {"rotation": [-15 - 0.4 * b, 0, 0]},
          "jaw": {"rotation": [0, 0, 0]},
          "ear_right": {"rotation": [-16 - 10 * tw, 0, -3 + 6 * tw]}, "ear_left": {"rotation": [-16, 0, 3]},
          "tail": {"rotation": [36, 1.5 * wave(t, 40, 0.2), 0]}}
    body = {"position": [0, -0.45 + 0.06 * b, -0.4], "rotation": [-0.5, 0, 0]}
    feet = {l: planted(REST_SOLE[l] + [(-0.6 if "right" in l else 0.6), 0, (-0.3 if l.startswith("front") else 0.2)])
            for l in IK.LEGS}
    return up, body, feet


def ward(t):
    scan = wave(t, 40)
    b = wave(t, 12)
    up = {"chest": {"rotation": [2 + 0.5 * b, 2 * scan, 0]},
          "head": {"rotation": [8 + 0.6 * b, 12 * scan, 2 * scan]},
          "jaw": {"rotation": [9 + 2 * max(0, b), 0, 0]},
          "ear_right": {"rotation": [18, 0, 12]}, "ear_left": {"rotation": [18, 0, -12]},
          "tail": {"rotation": [-14, 3 * wave(t, 40, 0.3), 0]}}
    return up, {"position": [0, WALK_Y - 0.4, 0]}, {}    # played over the walk: the pendant and muzzle clear its low body


# attacks ---------------------------------------------------------------------------------------------
def blend_ready(t, up, body, feet, w):
    """Mixes the clip's pose toward the ready stance by w (rotations and positions linearly): toward the stance's pose at its
    tick 0, which is where the clip started, so the last frame is the first (settle)."""
    ru, rb, rf = ready_pose(0)
    for b in up:
        for ch in up[b]:
            up[b][ch] = [a + (r - a) * w for a, r in zip(up[b][ch], ru[b].get(ch, [0, 0, 0]))]
    for ch in ("position", "rotation"):
        body[ch] = [a + (r - a) * w for a, r in zip(body.get(ch, [0, 0, 0]), rb.get(ch, [0, 0, 0]))]
    return up, body, feet


BITE_T, LUNGE_T, IMPACT = 16.0, 16.0, 8.0   # equal lengths: the harness expects every frame of every variant
# The neck carries the head up and out as it draws back (bite) or gathers (lunge), so the skull-mask's back edge does not
# sink into the chest (focus check armour_clip, chest into skullcap 2.0 px at bite t4.5 and lunge t3): px (up, forward -z)
NECK_LIFT = (0.4, -0.8)
LUNGE_NECK = (0.0, -0.8)   # the gather: out only; lifting it too pitched the chest down onto the front leg wraps (scorecard pen_limb)


def bite(t):
    ru, rb, rf = ready_pose(0)
    # anticipation: the haunches sink and the head draws back and up, jaw opening; strike: the back legs drive the chest
    # forward and down, the head thrusts out; shut at the impact; shake; recover
    draw = K([(0, 0), (4.5, 1), (6, 0.6), (7.2, -0.2), (8, -1), (10.5, -0.8), (13, -0.3), (16, 0)])(t)
    gape = K([(0, 0.0), (3, 0.5), (5.2, 0.95), (6, 1.0), (7.5, 1.0), (8, 0), (9.5, 0.05), (11, 0.25), (16, 0.15)])(t)
    thrust = K([(0, 0), (5.5, -0.3), (7.4, 0.8), (8, 1), (9.6, 0.8), (12.5, 0.25), (16, 0)])(t)
    shake = damp(t, 8.3, 2.2, 2.2)
    up = {"chest": {"rotation": [5 - 9 * draw, 3 * shake, -2 * shake]},
          "head": {"rotation": [-9 - 16 * draw, 14 * shake, -8 * shake],
                   "position": [0, -0.3 * thrust + NECK_LIFT[0] * max(draw, 0), -2.4 * thrust + NECK_LIFT[1] * max(draw, 0)]},
          "jaw": {"rotation": [5 + 36 * gape, 0, 0]},
          "tail": {"rotation": [24 + 8 * draw, ru["tail"]["rotation"][1] - 10 * shake, 0]}}   # from the stance's own carriage
    up["ear_right"] = {"rotation": [-6 + 14 * max(-draw, 0), 0, 4]}
    up["ear_left"] = {"rotation": [-6 + 14 * max(-draw, 0), 0, -4]}
    body = {"position": [0.3 * shake, -1.5 - 0.7 * max(draw, 0) - 0.3 * max(-draw, 0), -0.6 + 1.6 * draw],
            "rotation": [3 - 4.5 * draw, 0, 1.5 * shake]}
    feet = dict(rf)
    # the lead paw reaches out and a little wide of the dropping chest, folded less, so its shin and wrap clear the brisket
    fr0 = REST_SOLE["front_right"] + [-0.3, 0, 0.8]
    fr1 = fr0 + [-0.5, 0, 2.6]
    if t < 9.5:
        feet["front_right"] = step(fr0, fr1, (t - 4.2) / 3.3, 1.1, 18, True, 6)
    else:
        feet["front_right"] = step(fr1, fr0, (t - 11.0) / 3.5, 0.6, 12, True)
    w = ss((t - 12.0) / 3.5)
    return blend_ready(t, up, body, feet, w)


# Review 2026-11-12 (F8): it read as a crouch and a short hop. px: body lift in the air, reach (body and front paws forward),
# front paw lift, hind hop lift
LEAP = (2.0, 5.4, 4.0, 2.2)   # the extra lift comes only once the hind paws have left the ground (t7.2)


def smooth_legs(samples, legs, t0, t1, r=3):
    """Eases the named legs' bones over t0..t1 with a triangular average of +-r samples (lunge t5.25: the front shin opened 14
    degrees in a quarter tick as the paw left the ground, an IK branch bump), faded in and out over half a tick."""
    bones = [l + p for l in legs for p in ("_leg", "_shin", "_foot")]
    orig = {b: [list(smp[b]["rotation"]) for smp in samples] for b in bones}
    wts = [r + 1 - abs(j) for j in range(-r, r + 1)]
    for i, smp in enumerate(samples):
        t = i / SUB
        w = ss(min((t - t0) / 0.5, (t1 - t) / 0.5))
        if w <= 0:
            continue
        for b in bones:
            acc, tot = [0.0, 0.0, 0.0], 0.0
            for j, wt in zip(range(i - r, i + r + 1), wts):
                if 0 <= j < len(samples):
                    acc = [a + wt * v for a, v in zip(acc, orig[b][j])]
                    tot += wt
            smp[b] = {"rotation": [o + (a / tot - o) * w for o, a in zip(orig[b][i], acc)]}


def lunge(t):
    ru, rb, rf = ready_pose(0)
    gather = K([(0, 0), (2, 0.5), (4.8, 1), (5.6, 0.7), (6.6, 0), (16, 0)])(t)
    reach = K([(0, 0), (4.8, -0.1), (6, 0.45), (7.3, 0.92), (8, 1), (9, 0.9), (10.2, 0.75), (13, 0.25), (16, 0)])(t)
    air = K([(0, 0), (5.2, 0), (6.4, 0.8), (7.6, 1), (8.6, 0.6), (9.6, 0), (16, 0)])(t)
    gape = K([(0, 0.0), (4, 0.4), (6.5, 1), (7.4, 1), (8, 0), (9.5, 0.1), (12, 0.25), (16, 0.15)])(t)
    land = damp(t, 9.6, 3.0, 1.6)
    hop = K([(0, 0), (7.1, 0), (7.9, 1), (8.6, 0.75), (9.5, 0), (16, 0)])(t)
    up = {"chest": {"rotation": [5 + 4 * gather - 9 * air + 4 * land, 0, 0]},
          "head": {"rotation": [-9 + 8 * gather + 6 * air - 6 * reach * (1 - air) - 4 * land, 0, 0],
                   "position": [0, LUNGE_NECK[0] * gather, LUNGE_NECK[1] * gather]},
          "jaw": {"rotation": [5 + 40 * gape, 0, 0]},
          "ear_right": {"rotation": [-6 + 26 * reach, 0, 4]}, "ear_left": {"rotation": [-6 + 26 * reach, 0, -4]},
          "tail": {"rotation": [24 - 10 * gather + 14 * reach, ru["tail"]["rotation"][1], 0]}}
    body = {"position": [0, -1.5 - 1.6 * gather + 1.6 * air + LEAP[0] * hop - 0.5 * max(land, 0), -0.6 - LEAP[1] * reach + 0.8 * gather],
            "rotation": [3 + 2.5 * gather - 9 * air, 0, 0]}
    feet = dict(rf)
    for l in ("front_right", "front_left"):
        base = REST_SOLE[l] + [(-0.3 if "right" in l else 0.3), 0, 0.8]
        fwd = base + [0, 0, LEAP[1]]
        d = 0.0 if "right" in l else 0.4
        if t < 10:
            feet[l] = step(base, fwd, (t - 5.3 - d) / (4.3 - d), LEAP[2], 35, True, 12)
        else:
            dl = 0 if "left" in l else 1.6
            feet[l] = step(fwd, base, (t - 11.2 - dl) / 2.8, 1.3, 30, True)
    for l in ("back_right", "back_left"):    # the hind feet push, then hop up under the body
        base = REST_SOLE[l] + [(-0.2 if "right" in l else 0.2), 0, 0.6]
        mid = base + [0, 0, 3.4]
        if t < 9:
            push = -45 * env(t, 5.0, 6.8, 7.2, 8.8)
            p, pitch = planted(base, push)
            feet[l] = (p, pitch)
            if t > 7.2:   # the toe roll of the push carries into the hop and fades out (it snapped flat at t7.2: a pop)
                q, qp = step(base, mid, (t - 7.2) / 2.6, LEAP[3], 25, False)
                feet[l] = lifted(q + (p - np.array(base, float)), qp + push)
        elif t < 12.6:
            feet[l] = step(base, mid, (t - 7.2) / 2.6, LEAP[3], 25, False) if t < 9.8 else planted(mid)
        else:
            dl = 0 if "left" in l else 0.8
            feet[l] = step(mid, base, (t - 12.6 - dl) / 2.4, 1.0, 20, False)
    w = ss((t - 13.5) / 2.5)
    return blend_ready(t, up, body, feet, w)


# hits (additive over the ready stance) -----------------------------------------------------------------
# Review 2026-11-12 (F2): played over the stance as the game does, the medium hits and the pull drove the chest into the
# skullcap 2.1 to 2.6 px: head pitch, chest pitch (front), head roll (sides); the pull's trailing head and its chest dip
HIT_HEAD = (-20, -5, 6)
PULL_HEAD = (-11, 7)
HIT_NECK = (0.5, -1.1)   # px (up, forward -z) the neck carries the head out of the chest plates while the blow lands
HIT_DIRS = {"front": (0, 1), "back": (0, -1), "right": (1, 0), "left": (-1, 0)}  # push (x: +left, z: +back) in anim px


def hit(kind, d):
    """A reaction to a blow from `d`; light flinches, medium is thrown and catches itself with a step."""
    med = kind == "medium"
    L = 16.0 if med else 9.0
    px, pz = HIT_DIRS[d]     # the push in anim position units (x+ left, z+ back): a blow from the right pushes left
    k = 1.0 if med else 0.5

    def fn(t):
        ru, rb, rf = ready_pose(0)
        a = K([(0, 0), (1.0, 0.9), (2.0, 1.0), (3.5, 0.75), (L * 0.6, 0.25), (L, 0)])(t)
        rec = damp(t, 2.0, L * 0.45, L * 0.25)
        up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
        body = {ch: list(v) for ch, v in rb.items()}
        feet = dict(rf)
        if d == "front":
            up["head"]["rotation"][0] += HIT_HEAD[0] * k * a      # thrown back; less and the skullcap stays off the chest plates
            up["chest"]["rotation"][0] += HIT_HEAD[1] * k * a
            body["position"][2] += 2.8 * k * a
            body["rotation"][0] += -3 * k * a
        elif d == "back":
            up["head"]["rotation"][0] += -14 * k * a
            up["chest"]["rotation"][0] += 4 * k * a
            body["position"][2] += -1.8 * k * a
            body["rotation"][0] += -4 * k * a
            up["tail"]["rotation"][0] += -30 * k * a
        else:
            sgn = 1 if d == "right" else -1      # struck from its right: thrown to its left
            body["position"][0] += 1.6 * k * a * sgn
            body["rotation"][2] += 7 * k * a * sgn
            up["chest"]["rotation"][2] += 5 * k * a * sgn
            up["head"]["rotation"][1] += -18 * k * a * sgn + 10 * k * rec * sgn
            up["head"]["rotation"][2] += HIT_HEAD[2] * k * a * sgn
            up["tail"]["rotation"][1] += 14 * k * a * sgn
        up["head"]["rotation"][0] += 4 * k * rec
        up["head"]["position"] = [0, HIT_NECK[0] * k * a, HIT_NECK[1] * k * a]
        body["position"][1] += -0.9 * k * a
        up["jaw"]["rotation"][0] += (18 if med else 10) * K([(0, 0), (1, 1), (3, 0.6), (L * 0.6, 0), (L, 0)])(t)
        for e, sg in (("ear_right", 1), ("ear_left", -1)):
            up[e]["rotation"][0] += 20 * k * a
            up[e]["rotation"][2] += 10 * k * a * sg
        if d != "back":
            up["tail"]["rotation"][0] += -18 * k * a
        if med:   # a catching step: the paws on the push side reach out after the body
            pushv = np.array([px * 1.0, 0, -pz * 1.0])   # world: anim x = world x; anim z+ (back) = world -z
            order = {"front": ["front_right", "front_left"], "back": ["back_left", "back_right"],
                     "right": ["front_left", "back_left"], "left": ["front_right", "back_right"]}[d]
            for i, l in enumerate(order):
                b0 = rf[l][0]
                b1 = b0 + pushv * 1.8
                s0 = 2.0 + i * 1.6
                if t < 10:
                    feet[l] = step(b0, b1, (t - s0) / 2.4, 1.3, 25, l.startswith("front"))
                else:
                    feet[l] = step(b1, b0, (t - 10.5 - i * 1.4) / 2.6, 0.9, 18, l.startswith("front"))
        return up, body, feet
    return L, fn


PULL_T = 14.0


def hit_pull():
    """Dragged toward a striker in front (a harpoon, an undertow): the body lurches forward and dips, the head and chest are yanked out,
    the front paws reach forward and catch, the hind paws scramble after; everything back by PULL_T."""
    L = PULL_T

    def fn(t):
        ru, rb, rf = ready_pose(0)
        a = K([(0, 0), (1.5, 1.0), (4.0, 0.9), (L * 0.6, 0.3), (L, 0)])(t)
        rec = damp(t, 3.0, L * 0.45, L * 0.25)
        up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
        body = {ch: list(v) for ch, v in rb.items()}
        feet = dict(rf)
        body["position"][2] += -2.6 * a                # anim z+ is back: the body lurches forward
        body["position"][1] += -0.7 * a
        body["rotation"][0] += 5 * a                     # the forequarters dip
        up["chest"]["rotation"][0] += PULL_HEAD[1] * a
        up["head"]["position"] = [0, HIT_NECK[0] * a, HIT_NECK[1] * a]
        up["head"]["rotation"][0] += 12 * a + PULL_HEAD[0] * K([(0, 0), (0.8, 1), (2.5, 0), (L, 0)])(t) + 5 * rec   # the head trails, then stretches out
        up["jaw"]["rotation"][0] += 14 * K([(0, 0), (1, 1), (3, 0.6), (L * 0.6, 0), (L, 0)])(t)
        for e, sg in (("ear_right", 1), ("ear_left", -1)):
            up[e]["rotation"][0] += 26 * a
            up[e]["rotation"][2] += 8 * a * sg
        up["tail"]["rotation"][0] += 22 * a              # the tail streams back
        pushv = np.array([0.0, 0.0, 1.0])               # world forward (anim z- is world +z)
        for i, l in enumerate(["front_left", "front_right", "back_left", "back_right"]):
            b0 = rf[l][0]
            b1 = b0 + pushv * (2.2 if l.startswith("front") else 1.4)
            s0 = 1.6 + i * 1.3
            if t < 9.5:
                feet[l] = step(b0, b1, (t - s0) / 2.2, 1.2, 25, l.startswith("front"))
            else:
                feet[l] = step(b1, b0, (t - 9.8 - i * 0.6) / 2.2, 0.8, 18, l.startswith("front"))
        return up, body, feet
    return L, fn


# deaths -------------------------------------------------------------------------------------------------
LIMP = {"front": [-25, 0, 18, 30], "back": [30, 0, -24, 25]}
# per fall: how far it rolls over (1 = flat on its side), the head's final pitch (+ chin tucked), the legs' extra fold
DEATH_POSE = {"back": (1.0, -26, -12), "forward": (0.45, 22, 6), "left": (1.0, 18, 26), "right": (0.8, 0, 0)}


def death(direction):
    """Falls in `direction` ("back": haunches give first, rolls onto its right side, head thrown back and legs stretched;
    "forward": forelegs buckle, chin down, slumps half over onto its left with the chin on the ground; "left": knocked over
    onto its left side and lies curled; "right": knocked over onto its right, sprawled)."""
    side = {"back": -1, "forward": 1, "left": 1, "right": -1}[direction]   # +1 lands on its left side (root rz +)
    over, hpitch, fold = DEATH_POSE[direction]

    def fn(t):
        ru, rb, rf = ready_pose(0)
        up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
        body = {ch: list(v) for ch, v in rb.items()}
        roll = K([(0, 0), (3, 0.04), (6, 0.25), (8.6, 0.72), (10.2, 1.06), (11.2, 0.96), (12.2, 1.015), (13.4, 1), (20, 1)])(t)
        settle = damp(t, 10.2, 2.4, 1.3)
        root = {"rotation": [0, 0, 86 * over * side * roll], "position": [0, 0, 0]}
        jolt = K([(0, 0), (1.5, 1), (3.5, 0.7), (7, 0.2), (11, 0)])(t)
        sink = K([(0, 0), (2, 0.15), (5, 0.8), (7, 1), (20, 1)])(t)
        if direction == "back":
            up["head"]["rotation"][0] += -30 * jolt
            body["position"][2] += 2.0 * jolt + 0.5 * sink
            body["rotation"][0] += -16 * sink * (1 - roll) - 6 * jolt
            body["position"][1] += -2.2 * sink
        elif direction == "forward":
            up["head"]["rotation"][0] += -16 * jolt + 26 * sink * (1 - roll * 0.6)
            up["chest"]["rotation"][0] += 14 * sink * (1 - roll * 0.5)
            body["position"][2] += -1.6 * jolt
            body["rotation"][0] += 6 * jolt + 6 * sink * (1 - roll)
            body["position"][1] += -2.0 * sink
        else:
            sg = 1 if direction == "left" else -1
            body["position"][0] += 1.8 * jolt * sg
            body["rotation"][2] += 9 * jolt * sg
            up["head"]["rotation"][1] += 22 * jolt * sg
            up["head"]["rotation"][2] += -12 * jolt * sg
            body["position"][1] += -1.6 * sink
        # dying: the jaw falls slack, ears and tail go limp, the head drops to lie along the floor
        up["jaw"]["rotation"][0] = 5 + 16 * jolt + 10 * sink + 4 * settle
        for e, sg in (("ear_right", 1), ("ear_left", -1)):
            up[e]["rotation"] = [18 * jolt + 26 * sink, 0, (14 * sink + 6 * roll) * sg]
        curl = 1 if direction == "left" else 0
        up["tail"]["rotation"] = [24 - 40 * sink - 4 * settle + 30 * curl * roll, (-10 - 30 * curl) * side * roll, 0]
        up["head"]["rotation"][2] += -24 * over * side * roll
        up["head"]["rotation"][0] += (8 + hpitch) * roll + 5 * settle
        up["chest"]["rotation"][0] += 2 * settle + 0.4 * hpitch * roll
        body["rotation"][2] += 4 * settle * side
        # legs: planted (IK) while it buckles, then gone limp (FK), splayed with gravity toward the floor side
        feet = {}
        legs = {}
        lim = ss((t - 4.0) / 6.0)
        for l in IK.LEGS:
            fr = "front" if l.startswith("front") else "back"
            down = (1 if "left" in l else -1) == side      # the leg under the body
            limp = list(LIMP[fr])
            limp[1] = (6 if down else -28) * side * -1 * over     # rz: top legs fall toward the floor side, lower legs lie flat
            limp[0] += 6 * settle + (fold if fr == "front" else -fold)
            limp[2] += fold * (1 if fr == "front" else -1)
            legs[l] = limp
            feet[l] = rf[l]
        return up, body, feet, {"root": root, "_limp": legs, "_lim": lim}
    return fn


def death_clip(direction):
    fn = death(direction)
    n = int(20 * SUB)
    samples = []
    tag = "death_" + direction
    lift_prev = 0.0
    for i in range(n + 1):
        t = i / SUB
        up, body, feet, extra = fn(t)
        pose = {k: dict(v) for k, v in up.items()}
        pose["body"] = dict(body)
        pose["root"] = dict(extra["root"])
        SOLVER.legs(pose, feet, tag)
        lim = extra["_lim"]
        for l, (lx, lz, sx, fx) in extra["_limp"].items():
            a = pose[l + "_leg"]["rotation"]; b = pose[l + "_shin"]["rotation"]; c = pose[l + "_foot"]["rotation"]
            pose[l + "_leg"] = {"rotation": [a[0] + (lx - a[0]) * lim, 0, a[2] + (lz - a[2]) * lim]}
            pose[l + "_shin"] = {"rotation": [b[0] + (sx - b[0]) * lim, 0, 0]}
            pose[l + "_foot"] = {"rotation": [c[0] + (fx - c[0]) * lim, 0, 0]}
        full(pose)
        low = lowest(pose)
        # rest on the floor: lift what would sink (once falling, the body lies on the ground)
        need = -low if t > 7 else max(0.0, -low)
        need += 0.7 * max(0.0, damp(t, 10.2, 2.4, 0.9))    # the bounce as it lands
        pose["root"]["position"] = [0, need, 0]
        samples.append(pose)
    lag_gear(samples, 20, False, GEAR_UP + GEAR_LEGS, fade_out=False)   # it settles as the body comes to rest
    CLIPS[tag] = bake(samples, 20, "hold_on_last_frame", ALL + GEAR_UP + GEAR_LEGS)
    return samples


# ------------------------------------------------------------------------------------------------ main
def main():
    """GG_HOUND_ONLY=name,name: rebuild only those clips (idle, walk, run, stance_ready, stance_ward, attack_bite, hit_medium_front,
    hit_pulled, death_back ...) and keep every other clip of the existing out file byte for byte (a splice, no rebuild drift)."""
    report = {}
    path = os.environ.get("GG_HOUND_OUT") or os.path.join(HERE, "out", "wm_hound.animation.json")
    only = {x for x in os.environ.get("GG_HOUND_ONLY", "").split(",") if x}
    if only:
        CLIPS.update(json.load(open(os.path.join(HERE, "out", "wm_hound.animation.json")))["animations"])

    def want(nm):
        return not only or nm in only
    for nm, P_, fn in (("idle", IDLE_P, idle), ("walk", WALK_P, walk), ("run", RUN_P, run)):
        if want(nm):
            s, w = sample_clip(nm, P_, fn); emit(nm, P_, s, True); report[nm] = w
    for nm, fn in (("ready", ready), ("brace", brace), ("wall", wall)):
        if want("stance_" + nm):
            s, w = sample_clip("stance_" + nm, READY_P, fn); emit("stance_" + nm, READY_P, s, True); report[nm] = w
    if want("stance_ward"):
        s, _ = sample_clip("stance_ward", READY_P, ward, solve=False)
        CLIPS["stance_ward_upper"] = bake(s, READY_P, True, UPPER + GEAR_UP)
    tl = {fmt_t(IMPACT / TPS): "impact"}
    for nm, L, fn in (("attack_bite", BITE_T, bite), ("attack_lunge", LUNGE_T, lunge)):
        if not want(nm):
            continue
        s, w = sample_clip(nm, L, fn, loop=False, hang_gear=True)
        if nm == "attack_lunge":
            smooth_legs(s, ["front_right", "front_left"], 4.6, 14.2)   # lift-off, the long reach, and its way back near full stretch
            smooth_legs(s, ["back_right", "back_left"], 6.8, 10.4)     # the hop off the push
        CLIPS[nm] = bake(s, L, False, UPPER + GEAR_UP, tl)
        CLIPS[nm + "_legs"] = bake(s, L, False, LOWER + GEAR_LEGS); report[nm] = w
    ready0 = full({**{k: dict(v) for k, v in ready_pose(0)[0].items()}, "body": dict(ready_pose(0)[1])})
    head_clear(ready0)
    SOLVER.legs(ready0, ready_pose(0)[2], "ready0")
    pendant(ready0)
    for kind in ("light", "medium"):
        for d in ("front", "back", "right", "left"):
            L, fn = hit(kind, d)
            nm = f"hit_{kind}_{d}"
            if not want(nm):
                continue
            med = kind == "medium"     # the medium (and heavy, x1.55) hits swing the hanging gear; a light flinch does not
            ups = UPPER + GEAR_UP
            s, w = sample_clip(nm, L, fn, loop=False, hang_gear=med); report[nm] = w
            delta = []
            for smp in s:
                dd = {}
                for b in ups + LOWER + GEAR_LEGS:
                    r = [a - c for a, c in zip(smp[b]["rotation"], ready0.get(b, {}).get("rotation", [0, 0, 0]))]
                    dd[b] = {"rotation": r}
                    if b in ("body", "head", "throat_skull"):
                        dd[b]["position"] = [a - c for a, c in zip(smp[b].get("position", [0, 0, 0]),
                                                                    ready0[b].get("position", [0, 0, 0]))]
                delta.append(dd)
            CLIPS[nm] = bake(delta, L, False, ups)
            CLIPS[nm + "_legs"] = bake(delta, L, False, LOWER + GEAR_LEGS)
    if want("hit_pulled"):
        L, fn = hit_pull()
        s, w = sample_clip("hit_pulled", L, fn, loop=False, hang_gear=True); report["hit_pulled"] = w
        delta = []
        for smp in s:
            dd = {}
            for b in UPPER + GEAR_UP + LOWER + GEAR_LEGS:
                dd[b] = {"rotation": [a - c for a, c in zip(smp[b]["rotation"], ready0.get(b, {}).get("rotation", [0, 0, 0]))]}
                if b in ("body", "head", "throat_skull"):
                    dd[b]["position"] = [a - c for a, c in zip(smp[b].get("position", [0, 0, 0]), ready0[b].get("position", [0, 0, 0]))]
            delta.append(dd)
        CLIPS["hit_pulled"] = bake(delta, L, False, UPPER + GEAR_UP)
        CLIPS["hit_pulled_legs"] = bake(delta, L, False, LOWER + GEAR_LEGS)
    for d in ("back", "forward", "left", "right"):
        if want("death_" + d):
            death_clip(d)
    out = {"format_version": "1.8.0", "animations": CLIPS}
    json.dump(out, open(path, "w"), separators=(",", ":"))
    print("wrote", path, len(CLIPS), "clips; worst IK miss px:",
          ", ".join(f"{k} {v:.2f}" for k, v in report.items() if v > 0.05) or "none > 0.05")


if __name__ == "__main__":
    main()
