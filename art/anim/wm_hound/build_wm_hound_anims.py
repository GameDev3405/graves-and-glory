"""The Moot Hound's clips (animation rebuild, v2), written to out/wm_hound.animation.json.

    python art/anim/wm_hound/build_wm_hound_anims.py

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
            if err > 0.08:   # try the textbook bend
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


def pendant(pose):
    """The throat skull hangs from the collar: when the head comes low it swings back against the throat (and rides up it)
    rather than sink into the ground. Continuous in the pose, so baked keys stay smooth."""
    pose.setdefault("throat_skull", {})["rotation"] = [0, 0, 0]
    pose["throat_skull"].pop("position", None)
    if lowest(pose, "throat_skull") >= PENDANT_CLEAR:
        return

    def at(rx):
        pose["throat_skull"]["rotation"] = [rx, 0, 0]
        return lowest(pose, "throat_skull")
    lo, hi = 0.0, 75.0
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
    pose["throat_skull"]["rotation"] = [hi, 0, 0]


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


def sample_clip(name, ticks, fn, loop=True, tag=None, layers=("legs", "upper"), timeline=None, solve=True):
    """fn(t) -> (upper dict, body dict, feet dict, extra dict). Writes name_legs and name_upper (or the single names given)."""
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
    return samples, worst


def emit(name, ticks, samples, loop, split=True, timeline=None, legs_name=None, upper_name=None):
    if split:
        CLIPS[upper_name or name + "_upper"] = bake(samples, ticks, loop, UPPER, timeline)
        CLIPS[legs_name or name + "_legs"] = bake(samples, ticks, loop, LOWER)
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


def walk(t):
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
    """Mixes the clip's pose toward the ready stance by w (rotations and positions linearly)."""
    ru, rb, rf = ready_pose(t)
    for b in up:
        for ch in up[b]:
            up[b][ch] = [a + (r - a) * w for a, r in zip(up[b][ch], ru[b].get(ch, [0, 0, 0]))]
    for ch in ("position", "rotation"):
        body[ch] = [a + (r - a) * w for a, r in zip(body.get(ch, [0, 0, 0]), rb.get(ch, [0, 0, 0]))]
    return up, body, feet


BITE_T, LUNGE_T, IMPACT = 16.0, 16.0, 8.0   # equal lengths: the harness expects every frame of every variant


def bite(t):
    ru, rb, rf = ready_pose(0)
    # anticipation: the haunches sink and the head draws back and up, jaw opening; strike: the back legs drive the chest
    # forward and down, the head thrusts out; shut at the impact; shake; recover
    draw = K([(0, 0), (4.5, 1), (6, 0.6), (7.2, -0.2), (8, -1), (10.5, -0.8), (13, -0.3), (16, 0)])(t)
    gape = K([(0, 0.15), (3, 0.5), (5.2, 0.95), (6, 1.0), (7.5, 1.0), (8, 0), (9.5, 0.05), (11, 0.25), (16, 0.15)])(t)
    thrust = K([(0, 0), (5.5, -0.3), (7.4, 0.8), (8, 1), (9.6, 0.8), (12.5, 0.25), (16, 0)])(t)
    shake = damp(t, 8.3, 2.2, 2.2)
    up = {"chest": {"rotation": [5 - 9 * draw, 3 * shake, -2 * shake]},
          "head": {"rotation": [-9 - 16 * draw, 14 * shake, -8 * shake], "position": [0, -0.3 * thrust, -2.4 * thrust]},
          "jaw": {"rotation": [5 + 36 * gape, 0, 0]},
          "tail": {"rotation": [24 + 8 * draw, -10 * shake, 0]}}
    up["ear_right"] = {"rotation": [-6 + 14 * max(-draw, 0), 0, 4]}
    up["ear_left"] = {"rotation": [-6 + 14 * max(-draw, 0), 0, -4]}
    body = {"position": [0.3 * shake, -1.5 - 0.7 * max(draw, 0) - 0.3 * max(-draw, 0), -0.6 + 1.6 * draw],
            "rotation": [3 - 4.5 * draw, 0, 1.5 * shake]}
    feet = dict(rf)
    fr0 = REST_SOLE["front_right"] + [-0.3, 0, 0.8]
    fr1 = fr0 + [0, 0, 2.6]
    if t < 9.5:
        feet["front_right"] = step(fr0, fr1, (t - 4.2) / 3.3, 1.8, 30, True, 6)
    else:
        feet["front_right"] = step(fr1, fr0, (t - 11.0) / 3.5, 1.2, 25, True)
    w = ss((t - 12.0) / 3.5)
    return blend_ready(t, up, body, feet, w)


def lunge(t):
    ru, rb, rf = ready_pose(0)
    gather = K([(0, 0), (2, 0.5), (4.8, 1), (5.6, 0.7), (6.6, 0), (16, 0)])(t)
    reach = K([(0, 0), (4.8, -0.1), (6, 0.45), (7.3, 0.92), (8, 1), (9, 0.9), (10.2, 0.75), (13, 0.25), (16, 0)])(t)
    air = K([(0, 0), (5.2, 0), (6.4, 0.8), (7.6, 1), (8.6, 0.6), (9.6, 0), (16, 0)])(t)
    gape = K([(0, 0.1), (4, 0.4), (6.5, 1), (7.4, 1), (8, 0), (9.5, 0.1), (12, 0.25), (16, 0.15)])(t)
    land = damp(t, 9.6, 3.0, 1.6)
    up = {"chest": {"rotation": [5 + 4 * gather - 9 * air + 4 * land, 0, 0]},
          "head": {"rotation": [-9 + 8 * gather + 6 * air - 6 * reach * (1 - air) - 4 * land, 0, 0]},
          "jaw": {"rotation": [5 + 40 * gape, 0, 0]},
          "ear_right": {"rotation": [-6 + 26 * reach, 0, 4]}, "ear_left": {"rotation": [-6 + 26 * reach, 0, -4]},
          "tail": {"rotation": [24 - 10 * gather + 14 * reach, 0, 0]}}
    body = {"position": [0, -1.5 - 1.6 * gather + 1.6 * air - 0.5 * max(land, 0), -0.6 - 5.0 * reach + 0.8 * gather],
            "rotation": [3 + 2.5 * gather - 9 * air, 0, 0]}
    feet = dict(rf)
    for l in ("front_right", "front_left"):
        base = REST_SOLE[l] + [(-0.3 if "right" in l else 0.3), 0, 0.8]
        fwd = base + [0, 0, 5.0]
        d = 0.0 if "right" in l else 0.4
        if t < 10:
            feet[l] = step(base, fwd, (t - 5.3 - d) / (4.3 - d), 3.2, 35, True, 12)
        else:
            dl = 0 if "left" in l else 1.6
            feet[l] = step(fwd, base, (t - 11.2 - dl) / 2.8, 1.3, 30, True)
    for l in ("back_right", "back_left"):    # the hind feet push, then hop up under the body
        base = REST_SOLE[l] + [(-0.2 if "right" in l else 0.2), 0, 0.6]
        mid = base + [0, 0, 3.4]
        if t < 9:
            p, pitch = planted(base, -45 * env(t, 5.0, 6.8, 7.2, 8.8))
            feet[l] = (p, pitch)
            if t > 7.2:
                feet[l] = step(base, mid, (t - 7.2) / 2.6, 1.4, 25, False)
        elif t < 12.6:
            feet[l] = step(base, mid, (t - 7.2) / 2.6, 1.4, 25, False) if t < 9.8 else planted(mid)
        else:
            dl = 0 if "left" in l else 0.8
            feet[l] = step(mid, base, (t - 12.6 - dl) / 2.4, 1.0, 20, False)
    w = ss((t - 13.5) / 2.5)
    return blend_ready(t, up, body, feet, w)


# hits (additive over the ready stance) -----------------------------------------------------------------
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
            up["head"]["rotation"][0] += -26 * k * a
            up["chest"]["rotation"][0] += -8 * k * a
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
            up["head"]["rotation"][2] += 10 * k * a * sgn
            up["tail"]["rotation"][1] += 14 * k * a * sgn
        up["head"]["rotation"][0] += 4 * k * rec
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
    CLIPS[tag] = bake(samples, 20, "hold_on_last_frame", ALL)
    return samples


# ------------------------------------------------------------------------------------------------ main
def main():
    report = {}
    s, w = sample_clip("idle", IDLE_P, idle); emit("idle", IDLE_P, s, True); report["idle"] = w
    s, w = sample_clip("walk", WALK_P, walk); emit("walk", WALK_P, s, True); report["walk"] = w
    s, w = sample_clip("run", RUN_P, run); emit("run", RUN_P, s, True); report["run"] = w
    for nm, fn in (("ready", ready), ("brace", brace), ("wall", wall)):
        s, w = sample_clip("stance_" + nm, READY_P, fn); emit("stance_" + nm, READY_P, s, True); report[nm] = w
    s, _ = sample_clip("stance_ward", READY_P, ward, solve=False)
    CLIPS["stance_ward_upper"] = bake(s, READY_P, True, UPPER)
    tl = {fmt_t(IMPACT / TPS): "impact"}
    for nm, L, fn in (("attack_bite", BITE_T, bite), ("attack_lunge", LUNGE_T, lunge)):
        s, w = sample_clip(nm, L, fn, loop=False)
        emit(nm, L, s, False, upper_name=nm, legs_name=nm + "_legs", timeline=tl); report[nm] = w
    ready0 = full({**{k: dict(v) for k, v in ready_pose(0)[0].items()}, "body": dict(ready_pose(0)[1])})
    head_clear(ready0)
    SOLVER.legs(ready0, ready_pose(0)[2], "ready0")
    pendant(ready0)
    for kind in ("light", "medium"):
        for d in ("front", "back", "right", "left"):
            L, fn = hit(kind, d)
            nm = f"hit_{kind}_{d}"
            s, w = sample_clip(nm, L, fn, loop=False); report[nm] = w
            delta = []
            for smp in s:
                dd = {}
                for b in UPPER + LOWER:
                    r = [a - c for a, c in zip(smp[b]["rotation"], ready0[b]["rotation"])]
                    dd[b] = {"rotation": r}
                    if b in ("body", "throat_skull"):
                        dd[b]["position"] = [a - c for a, c in zip(smp[b].get("position", [0, 0, 0]),
                                                                    ready0[b].get("position", [0, 0, 0]))]
                delta.append(dd)
            CLIPS[nm] = bake(delta, L, False, UPPER)
            CLIPS[nm + "_legs"] = bake(delta, L, False, LOWER)
    for d in ("back", "forward", "left", "right"):
        death_clip(d)
    out = {"format_version": "1.8.0", "animations": CLIPS}
    path = os.path.join(HERE, "out", "wm_hound.animation.json")
    json.dump(out, open(path, "w"), separators=(",", ":"))
    print("wrote", path, len(CLIPS), "clips; worst IK miss px:",
          ", ".join(f"{k} {v:.2f}" for k, v in report.items() if v > 0.05) or "none > 0.05")


if __name__ == "__main__":
    main()
