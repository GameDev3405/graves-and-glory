"""The Web Stalker's clips (animation rebuild, v2, group F: weight, anticipation, graded reactions), written to
out/wm_stalker.animation.json.

    python art/anim/wm_stalker/build_wm_stalker_anims.py

An eight-legged spider on the knight's controllers. Layers: *_legs keys `body` (height, pitch, roll) and the sixteen leg bones;
*_upper, attack and hit clips key the thorax, head, fangs, palps, mandibles, abdomen and silk sacs. Paired clips share length
and clock, so every claw tip is placed by IK (stalker_ik.py) against the combined pose: planted claws hold still on the floor
while the body moves over them, lifted ones follow the clip's arcs. Deaths key every animated bone (root included) and are
lifted to rest on the floor.

Gaits are the alternating tetrapod (L1 R2 L3 R4, then R1 L2 R3 L4), each set staggered a touch front to back. Ground speed:
walk_ covers 2.56 px/tick (GaitController ref .16), run_ 4.16 px/tick (ref .26). Both attacks and the ensnare land at tick 8
(Spec family spider: spider_bite and spider_lunge, delay 8; cooldown 25).

hit_pulled / hit_pulled_legs (14 ticks, additive, hit_v1 flag 8: harpooned toward a puller in front) yank the body forward off its
claws, the forelegs reaching to dig in while the six others scrape behind and then scrabble back under it. `--pulled` splices
just those two into the existing file (as --deaths, --fallback and --ensnare do).
"""
import json, math, os, sys
import numpy as np
from scipy.interpolate import PchipInterpolator

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import stalker_ik as IK                                      # noqa: E402

TPS, SUB = 20, 4
ROT_TOL, POS_TOL = 0.02, 0.002
S = IK.Spider()
BODY0 = float(S.m.bones["body"].pivot[1])                    # the body pivot's rest height, px (10.2)

FANGS = ["fangs", "fangs_t2", "fangs_t3"]
UPPER = ["thorax", "head", "abdomen", "palps"] + FANGS + ["mandible_left", "mandible_right", "sacs", "sacs_ii"]
LEGB = [l + sfx for l in IK.LEGS for sfx in ("", "_low")]
LOWER = ["body"] + LEGB
ALL = ["root"] + LOWER + UPPER

TIP_Y = 0.5          # the claw tip's centre on the floor: its lowest corner just touches
REST = {1: (11.5, 12.5), 2: (14.5, 5.0), 3: (14.5, -5.5), 4: (12.0, -12.5)}   # claw tips at rest: out, forward (px)
STAND_H = 8.0        # thorax centre height standing (the hand art's is 10.2: the body sits low and the knees rise over it)


def sg(leg):
    return IK.side(leg)


def idx(leg):
    return int(leg[-1])


def rest_tip(leg, dx=0.0, dy=0.0, dz=0.0):
    x, z = REST[idx(leg)]
    return np.array([sg(leg) * (x + dx), TIP_Y + dy, z + dz])


def by(h):
    """The body bone's y position key for a thorax centre at height h."""
    return h - BODY0


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
        return float(v[0]) if scalar else np.array(v)
    return g


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


def hermite(s, p0, p1, v0, v1):
    h00 = 2 * s ** 3 - 3 * s ** 2 + 1
    h10 = s ** 3 - 2 * s ** 2 + s
    h01 = -2 * s ** 3 + 3 * s ** 2
    h11 = s ** 3 - s ** 2
    return h00 * p0 + h10 * v0 + h01 * p1 + h11 * v1


# ------------------------------------------------------------------------------------------------ claws
def step(p0, p1, s, lift):
    """A claw stepping from p0 to p1 (world px), s 0..1: lifts, arcs, sets down."""
    s = min(max(s, 0.0), 1.0)
    p0, p1 = np.array(p0, float), np.array(p1, float)
    if s <= 0:
        return p0
    if s >= 1:
        return p1
    p = p0 + (p1 - p0) * ss(s)
    sk = s ** 0.85
    p[1] += lift * 4 * sk * (1 - sk)
    p[1] = max(p[1], TIP_Y)
    return p


def gait_tip(leg, u, D, period, v, lift, base):
    """One claw of a looping gait at phase u (0..1, 0 = touchdown): planted for u < D (moving back at v px/tick in the troop's
    frame), then the swing forward, leaving and arriving at the ground's speed so the claw never skids."""
    L = v * period * D
    x0, z0 = base[0], base[2]
    zf, zb = z0 + L / 2, z0 - L / 2
    if u < D:
        return np.array([x0, TIP_Y, zf - (u / D) * L])
    s = (u - D) / (1 - D)
    T = (1 - D) * period
    z = hermite(s, zb, zf, -v * T * 0.6, -v * T * 0.35)
    sk = s ** 0.8
    y = TIP_Y + lift * 4 * sk * (1 - sk)
    x = x0 + sg(leg) * 0.8 * math.sin(math.pi * s)    # the leg swings out a little as it reaches
    return np.array([x, y, z])


# ------------------------------------------------------------------------------------------------ poses
def U(thorax=(0, 0, 0), head=(0, 0, 0), abd=(0, 0, 0), palps=0.0, fang=0.0, mand=0.0, sacs=(0, 0, 0), head_pos=None,
      thorax_pos=None):
    """An upper-body pose. fang: + tucks the fangs back, - swings them forward and open; mand: the tier III mandibles' spread."""
    up = {"thorax": {"rotation": list(thorax)}, "head": {"rotation": list(head)}, "abdomen": {"rotation": list(abd)},
          "palps": {"rotation": [palps, 0, 0]}, "mandible_left": {"rotation": [0, -mand, 0]},
          "mandible_right": {"rotation": [0, mand, 0]}, "sacs": {"rotation": list(sacs)},
          "sacs_ii": {"rotation": [sacs[0] * 0.7, sacs[1] * 0.7, sacs[2] * 0.7]}}
    for f in FANGS:
        up[f] = {"rotation": [fang, 0, 0]}
    if head_pos is not None:
        up["head"]["position"] = list(head_pos)
    if thorax_pos is not None:
        up["thorax"]["position"] = list(thorax_pos)
    return up


def B(h, pos=(0, 0, 0), rot=(0, 0, 0)):
    return {"position": [pos[0], by(h) + pos[1], pos[2]], "rotation": list(rot)}


def add_rot(d, bone, v):
    r = d[bone].setdefault("rotation", [0, 0, 0])
    d[bone]["rotation"] = [r[i] + v[i] for i in range(3)]


def add_pos(d, bone, v):
    p = d[bone].setdefault("position", [0, 0, 0])
    d[bone]["position"] = [p[i] + v[i] for i in range(3)]


# ------------------------------------------------------------------------------------------------ the floor
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


SCENE = _PoseScene(S.m)
LEG_SET = set(LEGB) | {f"{l}_guard" for l in IK.LEGS} | {f"{l}_spur" for l in IK.LEGS}


def lowest(pose, bones=None, exclude=None):
    SCENE.pose = pose
    fr = SCENE.evaluate(0)
    sel = [i for i, b in enumerate(S.m.cube_bone) if (bones is None or b in bones) and (exclude is None or b not in exclude)]
    if not sel:
        return 99.0
    return float(fr.corners[sel][:, :, 1].min()) * 16.0


HEAD_GEAR = {"head", "mask", "palps", "fangs", "fangs_t2", "fangs_t3", "mandible_left", "mandible_right", "crest"}
ABD_GEAR = {"abdomen", "sacs", "sacs_ii", "cords", "backcloth", "abdomen_plates", "cord_beads", "trophy"}
CLEAR = 0.3


def _clear(pose, joint, gear, most, sign):
    """Turns `joint` (pitch, by sign: -1 lifts a forward part, +1 a rearward one) just enough that `gear` stays CLEAR px above
    the floor. Continuous in the pose, so baked keys stay smooth."""
    if lowest(pose, gear) >= CLEAR:
        return
    h = pose.setdefault(joint, {}).setdefault("rotation", [0, 0, 0])
    rx0 = h[0]

    def at(d):
        h[0] = rx0 + sign * d
        return lowest(pose, gear)
    lo, hi = 0.0, most
    if at(hi) < CLEAR:
        return
    for _ in range(12):
        mid = (lo + hi) / 2
        if at(mid) < CLEAR:
            lo = mid
        else:
            hi = mid
    h[0] = rx0 + sign * hi


def floor_clear(pose):
    """The fangs, mandibles and mask never dig into the turf (the head, then the thorax, pitch up just enough), nor does the
    abdomen or its sacs (the abdomen pitches up)."""
    _clear(pose, "head", HEAD_GEAR, 35.0, -1)
    _clear(pose, "thorax", HEAD_GEAR, 25.0, -1)
    _clear(pose, "abdomen", ABD_GEAR, 60.0, +1)


# Batch review 2026-11-11: in the medium hits and the knockback the abdomen whips while the thorax pitches, and the front lip
# of its backcloth folded into the thorax's ridge plates (focus_check armour_clip 2.0-2.2, tick 2.5-3, played over ready as
# the game plays it). As in the ensnare, the pedicel stretches: the abdomen rides back off the thorax just as far as it needs
# (at most 1.6 px), so the cloth stays within PEDICEL_SLACK px of its rest fit.
PEDICEL = {"hit_medium_front", "hit_medium_back", "hit_medium_right", "hit_medium_left", "hit_knockback"}
PEDICEL_SLACK = 1.2
_PED = {}


def _cloth_depth(pose):
    if not _PED:
        _PED["cubes"] = (S.cubes_of({"backcloth"}), S.cubes_of({"ridge_plates"}))
        _PED["rest"] = 0.0
        _PED["rest"] = _cloth_depth({})
    a, b = _PED["cubes"]
    M = S.fk(pose, ["backcloth", "ridge_plates"])
    return max(S.pair_depth(M[ab], ac, ah, ap, M[bb], bc, bh, bp) for ab, ac, ah, ap in a for bb, bc, bh, bp in b) - _PED["rest"]


def pedicel_clear(pose):
    if _cloth_depth(pose) <= PEDICEL_SLACK:
        return
    ab = pose.setdefault("abdomen", {"rotation": [0, 0, 0]})
    p0 = list(ab.get("position", [0.0, 0.0, 0.0]))

    def at(z):
        ab["position"] = [p0[0], p0[1], p0[2] + z]
        return _cloth_depth(pose)
    lo, hi = 0.0, 1.6
    if at(hi) > PEDICEL_SLACK:
        return
    for _ in range(10):
        mid = (lo + hi) / 2
        if at(mid) > PEDICEL_SLACK:
            lo = mid
        else:
            hi = mid
    at(hi)


# ------------------------------------------------------------------------------------------------ the trophy
# The skull trophy lashed on top of the abdomen (tier III) stood dead still in every attack, hit and stun (focus_check
# secondary_life; Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor"). It now rocks on
# its lashing: a damped spring lags the abdomen's pitch and roll and the body's heave, and the bone turns about the trophy's
# own base (its pivot is the abdomen's, 10 px away, so a position key holds the base in place).
TROPHY_CUBE = max((ci for ci, b in enumerate(S.m.cube_bone) if b == "trophy"), key=lambda ci: np.prod(S.m.cube_list[ci].size))
_tc = S.m.cube_list[TROPHY_CUBE].corners
TROPHY_BASE = np.array([(_tc[:, 0].min() + _tc[:, 0].max()) / 2, _tc[:, 1].min(), (_tc[:, 2].min() + _tc[:, 2].max()) / 2, 1.0])
UPPER_T = UPPER + ["trophy"]


def trophy_key(rx, rz):
    """The trophy's rotation and the position that keeps its base where it sits."""
    A = S.local("trophy", {"rotation": [rx, 0.0, rz]})
    d = (TROPHY_BASE - A @ TROPHY_BASE)[:3] * 16.0
    return {"rotation": [rx, 0.0, rz], "position": [-float(d[0]), float(d[1]), float(d[2])]}


def swing_trophy(samples, period=3.2, zeta=0.3, gain=1.0, most=12.0):
    """Adds the trophy's rock to absolute samples (in place): zero on the first sample, eased back to zero over the last two
    ticks, so the clip still starts and ends on its stance."""
    def drive(p):
        r = [p[b]["rotation"] for b in ("body", "thorax", "abdomen")]
        y = p["body"].get("position", [0, 0, 0])[1]
        return np.array([sum(v[0] for v in r) - 6.0 * y, sum(v[2] for v in r)])
    dt = 1.0 / SUB
    w = 2 * math.pi / period
    th = drive(samples[0]).astype(float)
    v = np.zeros(2)
    n = len(samples) - 1
    for i, p in enumerate(samples):
        d = drive(p)
        if i:
            for _ in range(4):
                a = w * w * (d - th) - 2 * zeta * w * v
                v = v + a * dt / 4
                th = th + v * dt / 4
        fade = ss((n - i) / (2.0 * SUB))
        rx, rz = np.clip((th - d) * gain, -most, most) * fade
        p["trophy"] = trophy_key(float(rx), float(rz))
    return samples
# Batch review 2026-11-11. The clips named here solve their legs with two extra terms (stalker_ik.solve_leg): `keep` holds
# each leg's spare degree of freedom near the previous sample's answer (the femur used to flick 8-14 deg between samples
# while the claw stayed put: focus_check jitter), and `avoid` keeps the fore legs out of the head and the hind legs out of
# the abdomen and its sacs (focus_check armour_clip). Clips not named keep the solve they were built and reviewed with.
# run is left out: with `keep` its spare freedom drifted over the stride and did not come back at the wrap (scorecard loop_vel
# 28.9 vs 21.8 on run_upper); it keeps its committed solve and its armour and jitter rows stay open.
STEADY = {"walk", "idle", "stance_fallback", "attack_bite", "attack_lunge", "ensnare", "stun",
          "hit_medium_front", "hit_medium_back", "hit_medium_right", "hit_medium_left", "hit_light_front",
          "hit_light_right", "hit_light_left", "hit_knockback", "hit_pulled"}
# death_back's left hind leg flipped solution branch at tick 5.25 (femur 16, 30 deg to -13, -12 in a quarter tick: focus_check
# pop 32); the deaths named here keep `keep` alone (no avoid: a dying spider's legs fold into it)
KEEP_ONLY = {"death_back"}
KEEP = 0.04          # per deg of change from the previous sample (a claw miss costs 4 per px)
AVOID_BONES = {1: ("head", "mask"), 2: ("head", "mask"), 3: ("abdomen", "sacs", "sacs_ii"), 4: ("abdomen", "sacs", "sacs_ii")}
AVOID_SLACK = 1.4    # px a leg may sink into those cubes beyond its rest fit (focus_check warns at 1.7, fails at 1.9)
# Fix round 1 (2026-11-12): the game adds a hit onto stance_ready at the same tick, and the ready stance breathes, so a hit
# shows a little deeper than the solve saw (hit_medium_front abdomen into left_leg_4_low 2.38 at t11.5, hit_knockback
# right_leg_3_guard into sacs_ii 1.94 at t15). The thrown hits keep a wider margin.
# hit_medium_front: as the abdomen swings back (t8-15) it needs 0.5 px; across the whole clip so tight a margin made the
# legs jump (pop 14.5 at t0.25), so it ramps in and out there (SLACK_WINDOW, quantized to 0.1 px).
SLACK_WINDOW = {"hit_medium_front": (0.5, 8.0, 10.0, 13.0, 15.0)}
HIT_SLACK = {"hit_medium_front": 0.9, "hit_medium_back": 0.9, "hit_medium_right": 0.9, "hit_knockback": 0.9}
# Thrown in the medium hits a fore leg's guard sank into the next leg's spur (left_leg_1_guard into left_leg_2_spur:
# hit_medium_back 1.96 at t1.5, hit_medium_right 1.99 at t3.5). In these clips each leg also keeps out of the leg before it
# on its side, which is already solved.
NEIGHBOUR = {"hit_medium_front", "hit_medium_back", "hit_medium_right"}   # (medium_left: 1.85 WARN without; with, a pop)
# run keeps its committed solve (the steady solve drifted over the stride and broke the loop seam) except right_leg_4 from
# t2 to t4, where it flicked 13.7 deg and its claw went 2.01 px into the abdomen (t3): keep and avoid there only, so the
# wrap (t0 = t5) is untouched.
# The stride is mirrored half a period later: left_leg_4 does the same at t0.5, so it gets a window too, starting after
# the t0 sample (the wrap's own frame stays the committed solve).
PART_STEADY = {"run": ({"right_leg_4": (2.0, 4.0), "left_leg_4": (0.25, 1.5)})}
# Additive hits steer every leg home to the ready stance's own answer over their last ticks (the guess and `keep` pull
# toward it), so the delta comes back to zero on the same answer and the end fade (fade_legs_end) has nothing to blend:
# blending two answers for one claw took hit_medium_front's hind claw 2.3 px into the abdomen at t14.5. {tag: length}
HOME = {}
HOME_TICKS = {"hit_medium_front": 9.0}   # its hind leg ends 17 deg off the ready answer: a long, gentle way home
READY_SOL = {}
_ALLOW = {}


def avoid_for(leg, M, slack=AVOID_SLACK):
    """The avoid terms for one leg in a pose whose bone matrices are M: the body cubes near it, and how deep each (leg
    bone, body bone) pair may sit: its rest fit (what focus_check subtracts) plus `slack`."""
    i = idx(leg)
    if i not in _ALLOW:
        _ALLOW[i] = S.cubes_of(set(AVOID_BONES[i]))
    key = leg if slack == AVOID_SLACK else (leg, slack)
    if key not in _ALLOW:
        M0 = S.fk({})
        allow = {"_": slack}
        for lb, lc, lh, lp in S.leg_cubes(leg):
            ml = M0[lb if lb == leg + "_low" else leg]
            for bb, bc, bh, bp in _ALLOW[i]:
                k = (lb, bb)
                allow[k] = max(allow.get(k, slack), max(0.0, S.pair_depth(ml, lc, lh, lp, M0[bb], bc, bh, bp)) + slack)
        _ALLOW[key] = allow
    body = [(bb, M[bb], bc, bh, bp, (M[bb] @ np.append(bc, 1.0))[:3], float(np.linalg.norm(bh))) for bb, bc, bh, bp in _ALLOW[i]]
    return body, _ALLOW[key]


def neighbour_avoid(leg, M, body, allow, slack=AVOID_SLACK):
    """Adds the leg before `leg` on its side (its posed matrices in M) to an avoid set."""
    i = idx(leg)
    if i == 1:
        return body, allow
    nb = leg[:-1] + str(i - 1)
    key = ("nb", leg, slack)
    if key not in _ALLOW:
        M0 = S.fk({})
        al = {}
        for lb, lc, lh, lp in S.leg_cubes(leg):
            ml = M0[lb if lb == leg + "_low" else leg]
            for bb, bc, bh, bp in S.leg_cubes(nb):
                mb = M0[bb if bb == nb + "_low" else nb]
                al[(lb, bb)] = max(0.0, S.pair_depth(ml, lc, lh, lp, mb, bc, bh, bp)) + slack
        _ALLOW[key] = al
    extra = []
    for bb, bc, bh, bp in S.leg_cubes(nb):
        mb = M[bb if bb == nb + "_low" else nb]
        extra.append((bb, mb, bc, bh, bp, (mb @ np.append(bc, 1.0))[:3], float(np.linalg.norm(bh))))
    return body + extra, {**allow, **_ALLOW[key]}


class Solver:
    def __init__(s):
        s.guess = {}
        s.t = None       # the sample's time, set by pose_of (PART_STEADY windows)

    def legs(s, pose, feet, tag):
        """Leg rotations putting each claw tip in `feet` {leg: world px}; legs not in feet keep what the pose gives them."""
        steady = tag in STEADY
        part = PART_STEADY.get(tag, {})
        part_on = {l for l, (lo, hi) in part.items() if s.t is not None and lo <= s.t <= hi}
        M = S.fk(pose) if (steady or part_on) else None
        base = M["body"] if M is not None else S.fk(pose, ["body"])["body"]
        slack = HIT_SLACK.get(tag, AVOID_SLACK)
        if tag in SLACK_WINDOW and s.t is not None:
            lo_s, a0, a1, b0, b1 = SLACK_WINDOW[tag]
            e = min(ss((s.t - a0) / (a1 - a0)), 1 - ss((s.t - b0) / (b1 - b0)))
            slack = round((slack - (slack - lo_s) * e) * 10) / 10
        worst = 0.0
        for leg, tgt in feet.items():
            key = (tag, leg)
            lsteady = steady or leg in part_on
            held = lsteady or tag in KEEP_ONLY
            kw = dict(keep=KEEP, avoid=avoid_for(leg, M, slack)) if lsteady else (dict(keep=KEEP) if held else {})
            if tag in NEIGHBOUR:
                kw["avoid"] = neighbour_avoid(leg, M, *kw["avoid"], slack=slack)
            if held and key not in s.guess:
                kw["keep"] = 0.0
            g = s.guess.get(key)
            if tag in HOME and g is not None and leg in READY_SOL and s.t is not None:
                hw = HOME_TICKS.get(tag, 4.0)
                w = ss((s.t - (HOME[tag] - hw)) / (hw - 1.0))
                if w > 0:
                    g = list((1 - w) * np.asarray(g) + w * np.asarray(READY_SOL[leg]))
                    kw["keep"] = KEEP * (1 + 9 * w)
            sol, err, ML = S.solve_leg(leg, np.asarray(tgt, float), base, g, **kw)
            if err > 0.08 and not (held and key in s.guess):   # try from the standing solution
                sol2, err2, ML2 = S.solve_leg(leg, np.asarray(tgt, float), base, STAND_GUESS.get(leg), **dict(kw, keep=0.0))
                if err2 < err:
                    sol, err, ML = sol2, err2, ML2
            if M is not None:
                M.update(ML)      # the next leg along sees this one where it now is (NEIGHBOUR)
            s.guess[key] = sol
            worst = max(worst, err)
            fx, fy, fz, tz = sol
            pose[leg] = {"rotation": [fx, fy, fz]}
            pose[leg + "_low"] = {"rotation": [0, 0, tz]}
        return worst


STAND_GUESS = {}


def full(pose):
    for b in ALL:
        pose.setdefault(b, {})
        pose[b].setdefault("rotation", [0, 0, 0])
    return pose


# ------------------------------------------------------------------------------------------------ bake
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


# ------------------------------------------------------------------------------------------------ sampling
CLIPS = {}
SOLVER = Solver()
REPORT = {}


def pose_of(fn, t, tag, solve=True):
    up, body, feet, *extra = fn(t)
    pose = {k: {c: list(v) for c, v in d.items()} for k, d in up.items()}
    pose["body"] = {c: list(v) for c, v in body.items()}
    if extra:
        for k, v in extra[0].items():
            pose[k] = dict(v)
    floor_clear(pose)
    if tag in PEDICEL:
        pedicel_clear(pose)
    SOLVER.t = t
    err = SOLVER.legs(pose, feet, tag) if solve else 0.0
    return full(pose), err


def sample_clip(name, ticks, fn, loop=True, solve=True):
    n = int(round(ticks * SUB))
    samples, worst = [], 0.0
    for i in range(n + 1):
        t = i / SUB
        if loop and i == n:
            samples.append(samples[0]); break
        pose, err = pose_of(fn, t, name, solve)
        worst = max(worst, err)
        samples.append(pose)
    low_body = min(lowest(p, exclude=LEG_SET) for p in samples[::2])
    low_leg = min(lowest(p, LEG_SET) for p in samples[::2])
    REPORT[name] = (worst, low_body, low_leg)
    return samples


def emit(name, ticks, samples, loop, timeline=None, legs_name=None, upper_name=None):
    CLIPS[upper_name or name + "_upper"] = bake(samples, ticks, loop, UPPER_T if "trophy" in samples[0] else UPPER, timeline)
    CLIPS[legs_name or name + "_legs"] = bake(samples, ticks, loop, LOWER)


# ================================================================================================ clips
# idle: alive but still -- a slow breath through the abdomen, palps and fangs working, a look about, a foreleg tests the ground
IDLE_P = 60.0


def idle(t):
    b = wave(t, 30)
    look = K([(0, 0), (9, 0), (13, 1), (21, 1), (25, 0), (37, 0), (40, -0.7), (47, -0.7), (51, 0), (60, 0)])(t)
    click = damp(t, 17, 1.4, 0.9) + damp(t, 44, 1.4, 0.9) + 0.6 * damp(t, 45.4, 1.4, 0.9)
    palp = damp(t, 6, 1.6, 1.0) - damp(t, 31, 1.6, 1.0) + 0.7 * damp(t, 52, 1.6, 1.0)
    up = U(thorax=(0.5 * b, 2.5 * look, 0), head=(-1.0 * b - 3 * abs(click), 9 * look, 2 * look),
           abd=(1.6 * wave(t, 30, 0.2), 2.0 * wave(t, 60, 0.1), 0.6 * wave(t, 60, 0.35)),
           palps=-6 * palp - 4 * max(0, b), fang=-3 - 9 * abs(click), mand=4 + 10 * abs(click),
           sacs=(-1.6 * wave(t, 30, 0.35), -1.5 * wave(t, 60, 0.3), 0))
    body = B(STAND_H, (0.25 * wave(t, 60, 0.1), 0.15 * b, 0), (0, 0, 0.6 * wave(t, 60, 0.1)))
    feet = {l: rest_tip(l) for l in IK.LEGS}
    # the left foreleg tests the ground: lifts, taps twice, sets down a touch further out; later the right second leg shifts
    if 24 <= t <= 33:
        a = env(t, 24, 25.6, 31.4, 33)
        tap = max(0.0, math.sin(2 * math.pi * (t - 25.6) / 2.2)) if 25.6 <= t <= 30 else 0.0
        p = rest_tip("left_leg_1") + np.array([0.3, 0, 0.9]) * a
        p[1] = TIP_Y + a * (2.4 - 1.9 * tap)
        feet["left_leg_1"] = p
    feet["right_leg_2"] = step(rest_tip("right_leg_2"), rest_tip("right_leg_2"), (t - 50) / 3.0, 1.6)
    return up, body, feet


# walk: the alternating tetrapod, quick and low ---------------------------------------------------------------
WALK_P, WALK_V, WALK_D = 6.0, 2.56, 0.52
WALK_H = 7.8
# Where the looping clip starts in the stride: a third of a cycle in, so the clip's wrap falls between the touchdowns and
# lift-offs (a plant or lift exactly on the seam reads as a pop at the loop point: scorecard loop_vel).
WALK_T0 = 2.0


def tetrapod(t, period, v, D, lift_f, lift_b, stagger=0.03, base=None):
    u0 = t / period
    feet = {}
    for l in IK.LEGS:
        ph = (0.0 if l in IK.SET_A else 0.5) + stagger * (idx(l) - 1)
        lift = lift_f if idx(l) <= 2 else lift_b
        feet[l] = gait_tip(l, (u0 - ph) % 1, D, period, v, lift, base(l) if base else rest_tip(l))
    return feet


def walk(t):
    t = t + WALK_T0
    u0 = t / WALK_P
    feet = tetrapod(t, WALK_P, WALK_V, WALK_D, 2.4, 2.0,
                    base=lambda l: rest_tip(l, 0, 0, (-1.5 if idx(l) == 1 else 1.0 if idx(l) == 4 else 0)))
    stp = math.cos(4 * math.pi * (u0 - 0.1))          # +1 as each set plants and takes the weight
    sway = wave(t, WALK_P, 0.1)                         # the body weaves a little as the sets swap
    lag = math.cos(4 * math.pi * (u0 - 0.24))
    up = U(thorax=(0, -0.8 * sway, 0), head=(-2 - 0.6 * stp, -1.4 * sway, 0),
           abd=(1.5 + 2.6 * lag, -3.2 * wave(t, WALK_P, 0.3), 1.2 * wave(t, WALK_P, 0.35)),
           palps=-4 + 7 * wave(t, WALK_P / 2, 0.2), fang=-2, mand=3,
           sacs=(-2.5 * math.cos(4 * math.pi * (u0 - 0.36)), -2.2 * wave(t, WALK_P, 0.45), 0))
    body = B(WALK_H, (0.2 * sway, -0.18 * stp, 0), (0.4 * stp, 1.6 * sway, -0.7 * sway))
    return up, body, feet


# run: the same tetrapod at a scuttle, lower, the abdomen bouncing behind ----------------------------------------
RUN_P, RUN_V, RUN_D = 5.0, 4.16, 0.42
RUN_H = 7.3
RUN_T0 = 1.0         # as WALK_T0: the wrap sits clear of the plants and lift-offs


def run(t):
    t = t + RUN_T0
    u0 = t / RUN_P
    feet = tetrapod(t, RUN_P, RUN_V, RUN_D, 3.0, 2.4, 0.04,
                    base=lambda l: rest_tip(l, 0.4, 0, (-1.0 if idx(l) == 1 else 0.6 if idx(l) == 4 else 0)))
    stp = math.cos(4 * math.pi * (u0 - 0.12))
    sway = wave(t, RUN_P, 0.1)
    lag = math.cos(4 * math.pi * (u0 - 0.3))
    up = U(thorax=(1.5, -0.6 * sway, 0), head=(-4 - 0.8 * stp, -1.0 * sway, 0),
           abd=(3 + 4.5 * lag, -4.0 * wave(t, RUN_P, 0.32), 1.5 * wave(t, RUN_P, 0.4)),
           palps=-12 + 5 * lag, fang=-4, mand=4,
           sacs=(-4 * math.cos(4 * math.pi * (u0 - 0.36)), -3 * wave(t, RUN_P, 0.5), 0))
    body = B(RUN_H, (0.15 * sway, -0.3 * stp, 0), (2.0 + 0.6 * stp, 1.2 * sway, -0.6 * sway))
    return up, body, feet


# stances ---------------------------------------------------------------------------------------------------------
READY_P = 40.0
READY_H = 7.6


def ready_pose(t):
    """Fighting: low, front up a touch, the forelegs raised off the turf and feeling the air, fangs working."""
    b = wave(t, 16)
    sway = wave(t, READY_P)
    hov = wave(t, 10)
    up = U(thorax=(-4 + 0.5 * b, 1.5 * sway, 0), head=(-2, -2.5 * sway, 0),
           abd=(7 + 1.0 * b, 1.6 * wave(t, READY_P, 0.25), 0),
           palps=-12 + 6 * wave(t, 5), fang=-8 - 3 * max(0.0, wave(t, 8)), mand=7 + 4 * wave(t, 8),
           sacs=(-1.2 * wave(t, 16, 0.25), -1.2 * wave(t, READY_P, 0.4), 0))
    body = B(READY_H, (0.25 * sway, 0.1 * b, 0.4), (-2.0, -1.0 * sway, 0.8 * sway))
    feet = {"left_leg_1": np.array([10.0, 5.0 + 0.6 * hov, 15.0]), "right_leg_1": np.array([-10.0, 5.0 - 0.6 * hov, 15.0])}
    for l in IK.LEGS:
        if idx(l) == 2:
            feet[l] = rest_tip(l, 0.8, 0, 0.6)
        elif idx(l) >= 3:
            feet[l] = rest_tip(l, 0.5, 0, -0.3)
    return up, body, feet


def ready(t):
    return ready_pose(t)


def brace(t):
    """Brace: hunkered flat and wide, every claw dug in, the forelegs planted far forward, fangs bared, a low tremble."""
    b = wave(t, 20)
    trem = math.sin(2 * math.pi * t / 2.5)
    up = U(thorax=(3 + 0.5 * b, 0, 0), head=(4 + 0.4 * trem, 1.5 * wave(t, 40), 0),
           abd=(-1 + 0.8 * b, 1.0 * wave(t, 40, 0.3), 0), palps=-18 + 1.5 * trem, fang=-16 - 1.5 * max(0, b), mand=12,
           sacs=(-0.8 * wave(t, 20, 0.3), 0, 0))
    body = B(6.4, (0, 0.12 * b, -0.3), (2.5, 0, 0))
    feet = {}
    for l in IK.LEGS:
        i = idx(l)
        feet[l] = rest_tip(l, 1.8, 0, {1: 2.6, 2: 0.6, 3: -0.4, 4: -1.2}[i])
    return up, body, feet


def wall(t):
    """Hold the line: a sentry. Up tall and square on straightened legs, the abdomen raised, dead still; only a slow breath, the
    palps lifting now and then, and one sharp foreleg twitch."""
    b = wave(t, 40)
    tw = damp(t, 24, 2.0, 1.2) * ss((t - 24) / 0.5)   # sharp, but eased in over half a tick (it popped 14 deg in one sample)
    up = U(thorax=(-3 + 0.3 * b, 0, 0), head=(-5 - 0.3 * b, 0, 0), abd=(12 + 0.6 * b, 0, 0),
           palps=-20 - 8 * max(0.0, damp(t, 8, 3.0, 2.0)), fang=-4, mand=3, sacs=(-0.6 * b, 0, 0))
    body = B(9.2, (0, 0.06 * b, 0.2), (-1.0, 0, 0))
    feet = {l: rest_tip(l, -1.2, 0, {1: 1.2, 2: 0.4, 3: -0.4, 4: -1.0}[idx(l)]) for l in IK.LEGS}
    feet["right_leg_1"] = feet["right_leg_1"] + np.array([0, 1.8 * max(0.0, tw), 0.4 * tw])
    return up, body, feet


def ward(t):
    """Falling back (played over the backward walk): front up on guard, the head scanning, fangs out, palps raised."""
    scan = wave(t, 40)
    b = wave(t, 12)
    up = U(thorax=(-6 + 0.5 * b, 2 * scan, 0), head=(-2 + 0.6 * b, 12 * scan, 2 * scan), abd=(4, -3 * scan, 0),
           palps=-22, fang=-12 - 2 * max(0, b), mand=10, sacs=(-1 * b, 0, 0))
    return up, B(WALK_H), {}


def fallback(t):
    """Falling back (stance_fallback_*: the controller plays these while the troop backs off, on the world clock, unscaled by standing):
    the ward's guard, bobbing with the step, over the tetrapod walked backwards at 0.7 of the walk's stride, same 6-tick cycle (the
    shared humanoid fall-back's rule: walk_legs reversed at 0.7)."""
    u0 = t / WALK_P
    feet = tetrapod(t, WALK_P, -0.7 * WALK_V, WALK_D, 2.0, 1.8,
                    base=lambda l: rest_tip(l, 0, 0, (-1.0 if idx(l) == 1 else 1.0 if idx(l) == 4 else 0)))
    stp = math.cos(4 * math.pi * (u0 - 0.1))
    sway = wave(t, WALK_P, 0.1)
    up = U(thorax=(-6 + 0.6 * stp, 1.5 * sway, 0), head=(-2 + 0.8 * stp, -3 * sway, 0), abd=(5 + 2.0 * math.cos(4 * math.pi * (u0 - 0.25)), 2.5 * sway, 0),
           palps=-22 + 3 * stp, fang=-12 - 2 * max(0.0, stp), mand=10, sacs=(-2 * stp, -1.5 * sway, 0))
    body = B(WALK_H, (0.2 * sway, -0.15 * stp, 0), (-2.0 + 0.4 * stp, -1.2 * sway, 0.6 * sway))
    return up, body, feet


# attacks ---------------------------------------------------------------------------------------------------------
ATK_T, IMPACT = 18.0, 8.0   # equal lengths: the harness expects every frame of every variant


def ends_ready(t, t_out):
    """The weight toward ready for an attack that starts AND ends exactly on ready_pose(0) (batch review 2026-11-11: the bite
    and the lunge ended 2.8 deg off their first frame, focus_check settle): in over the first 1.2 ticks, back over 4."""
    return max(ss((t - t_out) / 4.0), 1.0 - ss(t / 1.2))


def blend_ready(t, up, body, feet, w):
    """Mixes the clip's pose toward the ready stance by w (rotations and positions linearly; the claws are the clip's own)."""
    ru, rb, rf = ready_pose(t)
    for b in up:
        for ch in up[b]:
            up[b][ch] = [a + (r - a) * w for a, r in zip(up[b][ch], ru[b].get(ch, [0, 0, 0]))]
    for ch in ("position", "rotation"):
        body[ch] = [a + (r - a) * w for a, r in zip(body.get(ch, [0, 0, 0]), rb.get(ch, [0, 0, 0]))]
    return up, body, feet


def bite(t):
    """The bite: rears up, forelegs high and spread (the threat), holds it trembling; then drives down and forward, the forelegs
    slamming down to pin either side of the prey as the fangs strike shut at the impact; a shake, and back to guard."""
    ru, rb, rf = ready_pose(0)
    rear = K([(0, 0), (2, 0.35), (4.4, 1), (6.2, 1.05), (7.2, 0.25), (8, -0.6), (9.6, -0.5), (12, -0.15), (18, 0)])(t)
    thrust = K([(0, 0), (5.6, -0.35), (7.3, 0.7), (8, 1), (9.4, 0.8), (12.5, 0.2), (18, 0)])(t)
    gape = K([(0, 0.2), (3, 0.6), (5.4, 1.0), (7.4, 1.0), (8, 0), (9.6, 0.05), (11, 0.2), (18, 0.25)])(t)
    trem = math.sin(2 * math.pi * t / 1.2) * env(t, 3.6, 4.6, 6.0, 6.6)
    shake = damp(t, 8.3, 2.0, 1.8)
    up_r, dn_r = max(rear, 0), max(-rear, 0)
    up = U(thorax=(-4 - 12 * up_r + 6 * dn_r + 1.2 * trem, 3 * shake, -2 * shake),
           head=(-2 + 8 * up_r - 6 * thrust, 10 * shake, -5 * shake), head_pos=(0, 0, -1.8 * max(thrust, 0)),
           abd=(7 + 16 * up_r - 8 * dn_r + 3 * shake, -4 * shake, 0),
           palps=-12 - 22 * up_r + 10 * max(thrust, 0), fang=-36 * gape + 10 * (1 - gape) * max(thrust, 0),
           mand=7 + 26 * gape, sacs=(-5 * shake + 3 * up_r, 3 * shake, 0))
    body = B(READY_H, (0.3 * shake, 1.3 * up_r - 0.9 * dn_r, 0.4 + 1.2 * up_r - 2.6 * thrust),
             (-2 - 16 * up_r + 8 * dn_r + 0.8 * trem, 0, 1.2 * shake))
    feet = dict(rf)
    for l in ("left_leg_1", "right_leg_1"):
        s = sg(l)
        hi = np.array([s * 9.5, 11.0, 15.0])
        pin = np.array([s * 7.5, TIP_Y, 16.5])
        feet[l] = K([(0, rf[l]), (2.2, rf[l] + [s * 0.5, 2.5, -0.8]), (4.4, hi), (6.3, hi + [0, 0.5, 0]),
                     (7.4, pin + [0, 2.0, 0.4]), (8.1, pin), (11.0, pin), (13.6, (pin + rf[l]) / 2 + [0, 2.2, 0]),
                     (15.8, rf[l]), (18, rf[l])])(t)
        feet[l][1] = max(feet[l][1], TIP_Y)
    for l in ("left_leg_2", "right_leg_2"):   # the second pair shuffles forward under the drive, then back
        s = sg(l)
        a, b2 = rf[l], rf[l] + [0, 0, 2.2]
        feet[l] = step(a, b2, (t - 6.6) / 2.0, 1.6) if t < 11 else step(b2, a, (t - 12.2 - (0.8 if s < 0 else 0)) / 2.6, 1.3)
    return blend_ready(0.0, up, body, feet, ends_ready(t, 13.5))


def lunge(t):
    """The lunge: gathers low on bunched legs, springs, flies at the prey with every foreleg reaching, slams in at the impact,
    rides the landing down, then backs off a step at a time."""
    ru, rb, rf = ready_pose(0)
    gather = K([(0, 0), (2.5, 0.5), (5.2, 1), (6.0, 0.8), (6.8, 0), (18, 0)])(t)
    reach = K([(0, 0), (5.4, -0.15), (6.6, 0.5), (7.6, 0.92), (8, 1), (9, 0.96), (10.5, 0.75), (14, 0.2), (18, 0)])(t)
    air = K([(0, 0), (5.8, 0), (6.7, 0.9), (7.4, 1), (8.0, 0.25), (8.5, 0), (18, 0)])(t)
    gape = K([(0, 0.2), (4, 0.5), (6.4, 1), (7.5, 1), (8, 0), (9.5, 0.1), (12, 0.25), (18, 0.25)])(t)
    land = damp(t, 8.1, 2.6, 1.4)
    up = U(thorax=(-4 + 4 * gather - 8 * air + 5 * land, 0, 0), head=(-2 + 6 * air - 5 * land, 0, 0),
           head_pos=(0, 0, -1.2 * reach * (1 - air)),
           abd=(7 - 8 * gather + 14 * air - 9 * land, 0, 0), palps=-12 - 18 * reach,
           fang=-36 * gape, mand=7 + 26 * gape, sacs=(-6 * land + 4 * air, 0, 0))
    body = B(READY_H, (0, -2.0 * gather + 2.4 * air - 0.7 * max(land, 0), 0.4 + 0.8 * gather - 6.5 * reach),
             (-2 + 3 * gather - 8 * air + 6 * land, 0, 0))
    feet = dict(rf)
    D = 5.5
    for l in IK.LEGS:
        s, i = sg(l), idx(l)
        base = rf[l]
        if i <= 2:   # the forelegs reach out in the air and land ahead
            fwd = rest_tip(l, -0.5 if i == 1 else 0, 0, D + (0.3 if i == 1 else 0.5))
            d0 = 0.0 if i == 1 else 0.3
            if t < 10.5:
                feet[l] = step(base, fwd, (t - 5.6 - d0) / (2.6 - d0), 3.4 if i == 1 else 2.6)
            else:
                dl = (0.0 if s > 0 else 0.7) + (0.0 if i == 1 else 1.4)
                feet[l] = step(fwd, base, (t - 11.0 - dl) / 2.4, 1.4)
        else:        # the hind legs push off, then hop up under the body
            mid = base + [0, 0, D - 1.0]
            if t < 6.8:
                feet[l] = base
            elif t < 12.4:
                feet[l] = step(base, mid, (t - 6.8 - (0.3 if i == 4 else 0)) / 2.4, 1.6)
            else:
                dl = (0.0 if s > 0 else 0.6) + (0.0 if i == 3 else 1.2)
                feet[l] = step(mid, base, (t - 12.4 - dl) / 2.2, 1.2)
    return blend_ready(0.0, up, body, feet, ends_ready(t, 14.0))


# where the forelegs' claws land at the cast, anim px (x: out, y: up, z: forward). Batch review 2026-11-11: was (7.5, 4, 17),
# past the leg's reach (IK miss 1.05 px at tick 8): the foreleg sat at full stretch and the reel's half-pixel tugs swung its
# femur 25-45 deg a quarter tick (focus_check pop 15-35, ticks 7.75-9.25). One px higher and two nearer it is in reach.
ENSNARE_FLING = (7.0, 6.0, 15.0)
ENSNARE_CURL = [(0, 0), (2.6, 0.4), (4.8, 1.0), (7.0, 1.12), (7.5, 0.6), (8.25, -0.45), (10.8, -0.35), (13, 0.05), (18, 0)]


def ensnare(t):
    """Ensnare, built to read apart from the bite at battle distance: it rears higher than for the bite with BOTH front pairs up
    and spread wide (the bite raises only the forelegs, forward), the abdomen curling right under to aim the spinnerets; it
    holds that coil, trembling, for two ticks (the spit beat); at the release (the impact, tick 8) the forelegs fling forward and
    wide like a cast net while the second pair slams out sideways and the abdomen whips; it holds the cast, reeling on the line
    with small tugs, then drops back to guard."""
    ru, rb, rf = ready_pose(0)
    rear = K([(0, 0), (2.2, 0.5), (4.6, 1.2), (7.2, 1.3), (7.7, 0.85), (8, 0.5), (11, 0.45), (13.5, 0.12), (18, 0)])(t)
    # Batch review 2026-11-11: the curl unwinds from 7.0 and lands a quarter tick after the impact (was 7.2 / 7.7 / 8.0): resting
    # on the floor (floor_clear) the abdomen dipped as the front came down, then whipped 29 deg in one quarter tick (focus_check
    # pop 23, tick 7.75); now the dip is a wind-up and the whip spreads over three quarter ticks
    curl = K(ENSNARE_CURL)(t)
    cast = K([(0, 0), (4.4, -0.25), (7.2, -0.4), (7.6, 0.55), (8, 1), (11, 0.95), (12.6, 0.6), (15, 0.1), (18, 0)])(t)
    trem = math.sin(2 * math.pi * t / 0.9) * env(t, 4.8, 5.4, 6.9, 7.3)
    reel = math.sin(2 * math.pi * (t - 8.5) / 1.6) * env(t, 8.6, 9.3, 11.2, 13.0)
    gape = K([(0, 0.2), (4, 0.8), (7, 1.0), (8, 0.3), (11, 0.4), (18, 0.25)])(t)
    up = U(thorax=(-4 - 16 * rear + 5 * max(cast, 0), 0, 0), head=(-2 + 12 * rear - 5 * cast + 2 * reel + 1.5 * trem, 0, 0),
           abd=(7 + 22 * rear - 62 * curl, 0, 0), palps=-12 - 22 * rear + 14 * max(cast, 0),
           fang=-32 * gape, mand=7 + 22 * gape, sacs=(-8 * curl - 3 * reel, 0, 0))
    # as the abdomen whips up at the cast its front lip folds into the thorax's ridge plates (focus_check armour_clip 2.15,
    # backcloth into ridge_plates, tick 8): the pedicel stretches, the abdomen riding back off the thorax by up to 1.2 px
    up["abdomen"]["position"] = [0.0, 0.0, 1.2 * min(1.0, max(0.0, -curl) / 0.45)]
    body = B(READY_H, (0, 5.5 * rear - 0.4 * reel + 0.25 * trem, 0.4 + 2.4 * rear - 1.4 * cast),
             (-2 - 13 * rear + 5 * max(cast, 0), 0, 0))
    feet = dict(rf)
    for l in ("left_leg_1", "right_leg_1"):
        s = sg(l)
        hi = np.array([s * 9.5, 15.0, 12.5])
        fling = np.array([s * ENSNARE_FLING[0], ENSNARE_FLING[1], ENSNARE_FLING[2]])
        feet[l] = K([(0, rf[l]), (2.2, rf[l] + [0, 3.0, -1.0]), (4.6, hi), (7.2, hi + [s * 0.8, 0.6, -0.8]),
                     (7.7, (hi + fling) / 2 + [0, 1.5, 0]), (8.0, fling), (11.0, fling + [0, 0.6, -1.2]),
                     (13.6, (fling + rf[l]) / 2 + [0, 1.4, 0]), (16, rf[l]), (18, rf[l])])(t)
        feet[l] = feet[l] + np.array([0, 0.4 * reel + 0.35 * trem, -0.6 * reel])
    for l in ("left_leg_2", "right_leg_2"):   # the second pair spreads up wide, then slams out sideways and braces there
        s = sg(l)
        a = rf[l]
        hi2 = np.array([s * 14.0, 8.0, 8.0])
        out2 = np.array([s * 16.0, TIP_Y, 7.0])
        if t < 13.0:
            feet[l] = K([(0, a), (1.4, a), (3.0, a + [s * 1.0, 4.0, 1.0]), (4.8, hi2), (7.2, hi2 + [s * 0.5, 0.5, 0]),
                         (7.7, hi2 + [s * 1.5, -2.5, 0]), (8.3, out2), (13.0, out2)])(t)
            feet[l] = feet[l] + np.array([0, 0.3 * trem * (1 if t < 7.5 else 0), 0])
        else:
            feet[l] = step(out2, a, (t - 13.0 - (0.6 if s < 0 else 0)) / 2.6, 1.4)
    w = ss((t - 14.0) / 4.0)
    return blend_ready(t, up, body, feet, w)


# hits (additive over the ready stance) ---------------------------------------------------------------------------
HIT_DIRS = {"front": (0, 1), "back": (0, -1), "right": (1, 0), "left": (-1, 0)}  # the push in anim px (x: +left, z: +back)


def hit(kind, d):
    """A blow from `d`. Light: a flinch, the legs stay put. Medium: thrown, the forelegs fly up, the abdomen whips, and the legs on
    the push side catch it with a step. (Heavy is medium x1.55 in the controller.)"""
    med = kind == "medium"
    L = 16.0 if med else 9.0
    px, pz = HIT_DIRS[d]
    k = 1.0 if med else 0.45

    def fn(t):
        ru, rb, rf = ready_pose(0)
        a = K([(0, 0), (1.0, 0.9), (2.0, 1.0), (3.5, 0.75), (L * 0.6, 0.25), (L, 0)])(t)
        wh = K([(0, 0), (1.4, 0), (2.6, 1), (4.2, -0.5), (6.0, 0.2), (L * 0.7, 0), (L, 0)])(t)    # the abdomen's lagging whip
        rec = damp(t, 2.0, L * 0.45, L * 0.25)
        up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
        body = {ch: list(v) for ch, v in rb.items()}
        feet = {l: np.array(v) for l, v in rf.items()}
        if d == "front":
            add_rot(up, "head", [-24 * k * a, 0, 0])
            add_rot(up, "thorax", [-8 * k * a, 0, 0])
            add_rot(up, "abdomen", [-14 * k * wh, 0, 0])
            body["position"][2] += 2.8 * k * a
            body["rotation"][0] += -6 * k * a
        elif d == "back":
            add_rot(up, "head", [-12 * k * a, 0, 0])
            add_rot(up, "thorax", [5 * k * a, 0, 0])
            add_rot(up, "abdomen", [24 * k * a - 10 * k * wh, 0, 0])
            body["position"][2] += -2.2 * k * a
            body["rotation"][0] += 5 * k * a
        else:
            sgn = 1 if d == "right" else -1      # struck from its right: thrown to its left
            body["position"][0] += 2.0 * k * a * sgn
            body["rotation"][2] += 9 * k * a * sgn
            add_rot(up, "thorax", [0, 0, 4 * k * a * sgn])
            add_rot(up, "head", [0, -18 * k * a * sgn + 9 * k * rec * sgn, 9 * k * a * sgn])
            add_rot(up, "abdomen", [0, 22 * k * wh * sgn, 6 * k * a * sgn])
        add_rot(up, "head", [4 * k * rec, 0, 0])
        body["position"][1] += -0.9 * k * a
        add_rot(up, "palps", [24 * k * a, 0, 0])
        for f in FANGS:
            add_rot(up, f, [-(18 if med else 10) * K([(0, 0), (1, 1), (3, 0.5), (L * 0.6, 0), (L, 0)])(t), 0, 0])
        add_rot(up, "mandible_left", [0, -14 * k * a, 0]); add_rot(up, "mandible_right", [0, 14 * k * a, 0])
        add_rot(up, "sacs", [-10 * k * wh, 6 * k * wh * px, 0])
        # the raised forelegs flinch up and back (medium: flung, then a re-reach)
        fl = K([(0, 0), (1.2, 1), (3.0, 0.8), (L * 0.55, 0.15), (L, 0)])(t)
        for l in ("left_leg_1", "right_leg_1"):
            feet[l] = feet[l] + np.array([sg(l) * 1.2 * k * fl, 3.0 * k * fl, -2.0 * k * fl])
        if med:   # a catching step: the planted legs on the push side reach out after the body
            push = np.array([px * 1.0, 0, -pz * 1.0])
            order = {"front": ["left_leg_4", "right_leg_4", "left_leg_3"], "back": ["left_leg_2", "right_leg_2"],
                     "right": ["left_leg_2", "left_leg_3", "left_leg_4"], "left": ["right_leg_2", "right_leg_3", "right_leg_4"]}[d]
            for j, l in enumerate(order):
                b0 = feet[l]
                b1 = b0 + push * 2.0
                s0 = 1.8 + j * 1.0
                if t < 9.5:
                    feet[l] = step(b0, b1, (t - s0) / 2.0, 1.6)
                else:
                    feet[l] = step(b1, b0, (t - 10.0 - j * 1.1) / 2.4, 1.2)
        return up, body, feet
    return L, fn


def knockback(t):
    """Thrown: the whole body flung back and up, forelegs flailing, the abdomen whipping down then up; it skids, scrabbles its legs
    under it in two quick tetrapod steps and recovers. Additive (the server moves the body)."""
    ru, rb, rf = ready_pose(0)
    L = 20.0
    a = K([(0, 0), (1.2, 1.0), (3.0, 0.9), (6.0, 0.55), (11, 0.2), (L, 0)])(t)
    wh = K([(0, 0), (1.5, 0), (3.0, 1), (5.0, -0.6), (7.5, 0.25), (11, 0), (L, 0)])(t)
    up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
    body = {ch: list(v) for ch, v in rb.items()}
    feet = {l: np.array(v) for l, v in rf.items()}
    add_rot(up, "head", [-30 * a, 0, 0]); add_rot(up, "thorax", [-12 * a, 0, 0])
    add_rot(up, "abdomen", [-22 * wh, 0, 0]); add_rot(up, "sacs", [-14 * wh, 0, 0])
    add_rot(up, "palps", [30 * a, 0, 0])
    for f in FANGS:
        add_rot(up, f, [-24 * a, 0, 0])
    add_rot(up, "mandible_left", [0, -18 * a, 0]); add_rot(up, "mandible_right", [0, 18 * a, 0])
    body["position"][2] += 4.0 * a
    body["position"][1] += 2.6 * K([(0, 0), (1.2, 1), (3, 0.5), (5, -0.4), (8, -0.15), (L, 0)])(t)
    body["rotation"][0] += -10 * a
    for l in ("left_leg_1", "right_leg_1"):
        feet[l] = feet[l] + np.array([sg(l) * 2.0 * a, 4.5 * a, -2.5 * a])
    # scrabble: set A then set B step back under the body as it lands
    for l in IK.LEGS:
        if idx(l) == 1:
            continue
        b0 = feet[l]
        b1 = b0 + np.array([0, 0, -2.6])
        s0 = 2.6 if l in IK.SET_A else 4.0
        if t < 10:
            feet[l] = step(b0, b1, (t - s0) / 1.8, 1.8)
        else:
            feet[l] = step(b1, b0, (t - (11.5 if l in IK.SET_A else 13.0)) / 2.4, 1.2)
    return up, body, feet


PULL_T = 14.0   # hit_pulled's length, as every troop's (art/anim/common/reactions.py)


def pulled(t):
    """Harpooned (hit_v1 flag 8: dragged toward a puller in front; the server moves the body): the whole spider yanked forward
    off its claws, the front pitching down toward the line, the head lagging then lifting to the puller, the abdomen trailing
    low behind it and bobbing up as it settles;
    the forelegs flung forward, flailing, then hooked down to dig in; the other six claws scrape along the turf behind the body,
    then scrabble back under it, set A then set B, and it settles. Additive, zero at both ends."""
    ru, rb, rf = ready_pose(0)
    L = PULL_T
    a = K([(0, 0), (1.0, 1.0), (2.5, 0.95), (5.0, 0.6), (9.0, 0.2), (L, 0)])(t)
    lag = K([(0, 0), (0.8, 0), (2.0, 1), (3.5, 0.5), (5.5, -0.35), (8.0, 0), (L, 0)])(t)
    fade = 1 - ss((t - 9.0) / 4.0)
    up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
    body = {ch: list(v) for ch, v in rb.items()}
    feet = {l: np.array(v) for l, v in rf.items()}
    body["position"][2] += -3.6 * a                      # yanked forward
    body["position"][1] += -0.7 * a
    body["rotation"][0] += 9 * a                         # the front pitched down toward the puller
    body["rotation"][2] += 2.2 * damp(t, 3.0, 2.4, 2.5) * fade   # a scrabbling rock
    add_rot(up, "thorax", [5 * a, 0, 0])
    add_rot(up, "head", [14 * lag - 6 * a * (1 - ss(t / 3.0)) - 10 * a * ss((t - 3.0) / 2.0), 0, 0])
    # the abdomen trails, dragged: the front is hauled down 14 deg (body 9 + thorax 5) at once, the abdomen's mass follows
    # late (a_lag), so at first it hangs back near level and low behind (-12 deg off its parent at t1), then catches up,
    # overshoots into a small bob (+4 around t4-5) and settles. Fix round 1: it had followed the body within 3 deg.
    a_lag = K([(0, 0), (1.0, 0.15), (2.5, 0.75), (4.0, 1.05), (5.5, 0.75), (9.0, 0.2), (L, 0)])(t)
    add_rot(up, "abdomen", [14 * (a_lag - a), 0, 0])
    add_rot(up, "sacs", [-12 * lag, 0, 0])
    add_rot(up, "palps", [22 * a, 0, 0])
    for f in FANGS:
        add_rot(up, f, [-18 * a, 0, 0])
    add_rot(up, "mandible_left", [0, -16 * a, 0]); add_rot(up, "mandible_right", [0, 16 * a, 0])
    reach = K([(0, 0), (1.2, 1), (3.5, 1), (5.5, 0.6), (9.0, 0.2), (L, 0)])(t)
    dig = K([(0, 0), (2.0, 0), (3.5, 1), (6.0, 1), (9.5, 0), (L, 0)])(t)
    for l in ("left_leg_1", "right_leg_1"):
        fl = damp(t, 0.5, 2.5, 2.0) * fade * (1 if l == "left_leg_1" else -1)
        feet[l] = feet[l] + np.array([sg(l) * (0.8 * reach + 0.9 * fl), 1.5 * reach - 3.5 * dig + 0.6 * fl, 3.0 * reach])
    for l in IK.LEGS:
        i = idx(l)
        if i == 1:
            continue
        p0 = feet[l]
        tr = p0 + np.array([-sg(l) * 0.3 * (i - 1), 0, -{2: 1.6, 3: 1.4, 4: 0.6}[i]])   # the rear stretch least: its reach is shortest
        ca = p0 + np.array([0, 0, 0.6])
        s0 = 3.0 if l in IK.SET_A else 4.2
        if t < s0:
            feet[l] = p0 + (tr - p0) * ss(t / 2.0)       # dragged: the claw scrapes behind the body
        elif t < s0 + 6.0:
            feet[l] = step(tr, ca, (t - s0) / 1.8, 1.8)  # the scrabble back under it
        else:
            feet[l] = step(ca, p0, (t - s0 - 6.0) / 2.2, 0.8)
    return up, body, feet


# stun (absolute, like a stance): a 4-tick snap, then a 20-tick dazed loop whose tick 24 is its tick 4 ----------------
STUN_ENTRY, STUN_LOOP = 4, 20


def stun(t):
    ph = (max(t, STUN_ENTRY) - STUN_ENTRY) / STUN_LOOP          # 0..1 over the loop
    sn = K([(0, 0.75), (1.2, 1), (2.5, 0.7), (4, 0)])(t) if t < 4 else 0.0   # the snap entry, already landing on frame 0
    # (as the knight's: the controller loops ticks 4..24, so tick 0 is only ever played once, straight off the hit)
    wob = math.sin(2 * math.pi * ph)
    wob2 = math.cos(2 * math.pi * ph)
    up = U(thorax=(3 + 2 * wob2 - 10 * sn, 0, 3 * wob), head=(8 + 5 * wob2 - 18 * sn, 7 * wob, 6 * wob2),
           abd=(-5 + 2.5 * math.sin(2 * math.pi * (ph - 0.2)) + 10 * sn, 3 * wob, 0),
           palps=14 + 4 * wob - 20 * sn, fang=6 - 20 * sn, mand=2 + 14 * sn, sacs=(-2 * wob2, 2 * wob, 0))
    body = B(6.6, (0.5 * wob, 0.3 * wob2 - 0.8 * sn, 0.5), (1.5 + 1.0 * wob2 - 6 * sn, 2 * wob, 3.5 * wob))
    feet = {}
    for l in IK.LEGS:
        i, s = idx(l), sg(l)
        p = rest_tip(l, 1.6, 0, {1: 1.2, 2: 0.3, 3: -0.3, 4: -0.8}[i])
        # the legs splay out at the snap, and one at a time twitches up, curling a little
        p = p + np.array([s * 1.2 * sn, 0, 0])
        k = (IK.LEGS.index(l) * 3) % 8
        tw = max(0.0, math.sin(2 * math.pi * (ph * 2 - k / 8.0))) ** 6
        p = p + np.array([-s * 1.5 * tw, 2.2 * tw, 0])
        feet[l] = p
    # the trophy lolls with the daze, a beat behind the body's wobble (periodic, so the 4..24 loop has no seam)
    up["trophy"] = trophy_key(5.0 * math.sin(2 * math.pi * (ph - 0.2)) - 6.0 * sn, 4.5 * math.cos(2 * math.pi * (ph - 0.15)) * (1 - sn))
    return up, body, feet


def stun_out(t):
    """The shake-off, additive over 6 ticks: the head shakes, the body shudders, the palps flick."""
    e = math.sin(math.pi * min(max(t / 6.0, 0), 1))
    sh = math.sin(2 * math.pi * t / 2.0) * e
    return {"thorax": {"rotation": [0, 3 * sh, 4 * sh]}, "head": {"rotation": [-6 * e, 14 * sh, 6 * sh]},
            "abdomen": {"rotation": [4 * e, -6 * sh, 0]}, "palps": {"rotation": [-16 * e, 0, 0]},
            "sacs": {"rotation": [-4 * sh, 4 * sh, 0]},
            "trophy": trophy_key(5 * math.sin(2 * math.pi * (t - 0.4) / 2.0) * e, -6 * math.sin(2 * math.pi * (t - 0.5) / 2.0) * e)}


# deaths -------------------------------------------------------------------------------------------------------------
def curl_pose(l, amt, splay=0.0):
    """A dead spider's leg (femur yaw, roll, tibia bend; clip keys): folded in over the belly by amt (0..1)."""
    s, i = sg(l), idx(l)
    yaw = {1: 10, 2: 4, 3: -4, 4: -10}[i] * s * (1 - 0.5 * amt)
    roll = s * ((20 + 55 * amt) * (1 - splay) - 32 * splay)   # + s: the femur turns down past the belly; - s: flat out sideways
    bend = s * ((40 + 75 * amt) * (1 - splay) + 14 * splay)
    return [yaw, roll, bend]


DEATH = {   # root turn (deg) by the end: x (+ tips the front down), z (+ onto its left side); curl of the legs; body sag
    "back": {"rx": -180, "rz": 0, "curl": 1.0, "splay": 0.0},
    "forward": {"rx": 6, "rz": 0, "curl": 0.3, "splay": 1.0},
    "left": {"rx": 0, "rz": 98, "curl": 0.8, "splay": 0.2},
    "right": {"rx": 0, "rz": -96, "curl": 1.0, "splay": 0.0},
}


def death(direction):
    """back: rears from the blow and flips over backward onto its back, the legs curling in over the belly. forward: the legs give
    and it slumps flat on its belly, legs splayed and half-curled, fangs in the dirt. left: knocked onto its left side, legs
    drawn in. right: rolls right over onto its back. Each lands, bounces and settles; a last leg twitch."""
    P = DEATH[direction]

    def fn(t):
        ru, rb, rf = ready_pose(0)
        up = {b: {ch: list(v) for ch, v in ru[b].items()} for b in ru}
        body = {ch: list(v) for ch, v in rb.items()}
        feet = dict(rf)
        jolt = K([(0, 0), (1.2, 1), (3.2, 0.7), (6.5, 0.2), (10, 0)])(t)
        sink = K([(0, 0), (1.5, 0.2), (4.0, 0.8), (6.0, 1), (20, 1)])(t)
        over = K([(0, 0), (2.5, 0.02), (4.5, 0.22), (7.4, 0.72), (9.0, 1.06), (10.0, 0.97), (11.0, 1.012), (12.2, 1), (20, 1)])(t)
        settle = damp(t, 9.0, 2.2, 1.2)
        root = {"rotation": [P["rx"] * over, 0, P["rz"] * over], "position": [0, 0, 0]}
        if direction == "back":
            add_rot(up, "head", [-26 * jolt, 0, 0]); add_rot(up, "thorax", [-8 * jolt, 0, 0])
            body["position"][2] += 2.0 * jolt
            body["rotation"][0] += -14 * jolt
        elif direction == "forward":
            # the legs give: the body drops onto its belly while every claw skids out along the turf (IK keeps them planted
            # on the floor), so it ends sprawled flat with the knees splayed out, not standing on its legs
            add_rot(up, "head", [-12 * jolt + 22 * sink, 0, 0])
            body["position"][2] += -1.4 * jolt
            body["rotation"][0] += 6 * jolt + 4 * sink
            body["position"][1] += -1.6 * sink
            drop = ss((t - 1.0) / 3.0)
            for l in IK.LEGS:
                i = idx(l)
                sprawl = rest_tip(l, 3.4 * sink, 0, {1: 2.0, 2: 0.8, 3: -0.8, 4: -1.8}[i] * sink)
                if i == 1:      # the raised forelegs come down first, then skid out
                    sprawl = feet[l] + (sprawl - feet[l]) * drop
                feet[l] = sprawl
            feet["left_leg_2"] = feet["left_leg_2"] + np.array([0, 1.2 * max(0.0, damp(t, 14.0, 1.6, 1.1)), 0])  # a last twitch
        else:
            g = 1 if direction == "left" else -1
            body["position"][0] += 2.0 * jolt * g
            body["rotation"][2] += 10 * jolt * g
            add_rot(up, "head", [0, 20 * jolt * g, -10 * jolt * g])
        body["position"][1] += -1.8 * sink
        add_rot(up, "abdomen", [-10 * sink + 6 * settle, 8 * jolt * (1 if direction != "right" else -1), 0])
        add_rot(up, "sacs", [-8 * settle, 0, 0])
        for f in FANGS:
            up[f]["rotation"] = [-24 * jolt + 10 * sink, 0, 0]
        up["mandible_left"]["rotation"] = [0, -18 * jolt - 4 * sink, 0]
        up["mandible_right"]["rotation"] = [0, 18 * jolt + 4 * sink, 0]
        up["palps"]["rotation"] = [20 * jolt + 22 * sink, 0, 0]
        add_rot(up, "head", [4 * settle, 0, 0])
        curl = {}
        twitch = damp(t, 14.0, 1.6, 1.1)
        for l in IK.LEGS:
            c = curl_pose(l, P["curl"], P["splay"])
            if l == "left_leg_2" or l == "right_leg_3":
                c[2] += sg(l) * 18 * twitch
            c[1] += sg(l) * 6 * settle
            curl[l] = c
        lim = 0.0 if direction == "forward" else ss((t - 2.0) / 5.5)
        return up, body, feet, {"root": root, "_curl": curl, "_lim": lim}
    return fn


# The floor under a corpse. One clip serves every tier, and the tier gear (fangs and mandibles, the crest, the trophy, the leg
# spurs, the plates and guards) shows on some tiers only, so a corpse rests on the parts every tier draws (CORE): resting it on
# tier III gear left tier I floating 5-6 px over its shadow. The head and abdomen turn just enough to keep their tier gear off
# the floor (FOLD_*), the armour plates of the higher tiers may settle up to SINK px into the dirt, and the spurs (thin spikes)
# may dig in.
RIG = json.load(open(os.path.join(HERE, "out", "wm_stalker.rig.json")))["bones"]
CORE = {b for b, r in RIG.items() if not r.get("stages")}
SPURS = {f"{l}_spur" for l in IK.LEGS}
FOLD_HEAD = {"fangs", "fangs_t2", "fangs_t3", "mandible_left", "mandible_right", "crest"}
FOLD_ABD = {"trophy"}
ARMOUR = set(RIG) - CORE - SPURS - FOLD_HEAD - FOLD_ABD
SINK = 1.0


def tier_bones(t):
    return {b for b, r in RIG.items() if not r.get("stages") or t in r["stages"]}


def _fold_angle(pose, joint, gear, most, sign):
    """The least turn of `joint` (by sign) that lifts `gear` CLEAR px off the floor, or None when `most` is not enough."""
    h = pose.setdefault(joint, {}).setdefault("rotation", [0, 0, 0])
    rx0 = h[0]
    if lowest(pose, gear) >= CLEAR:
        return 0.0
    h[0] = rx0 + sign * most
    ok = lowest(pose, gear) >= CLEAR
    h[0] = rx0
    if not ok:
        return None
    lo, hi = 0.0, most
    for _ in range(12):
        mid = (lo + hi) / 2
        h[0] = rx0 + sign * mid
        if lowest(pose, gear) < CLEAR:
            lo = mid
        else:
            hi = mid
    h[0] = rx0
    return hi


def rest_height(pose):
    """The root lift (absolute) that lays the corpse on its CORE parts, the higher tiers' armour sinking at most SINK px."""
    keep = pose["root"].get("position", [0, 0, 0])
    set_lift(pose, 0.0)
    n = max(-lowest(pose, CORE), -lowest(pose, CORE | ARMOUR) - SINK)
    pose["root"]["position"] = keep
    return n


def set_lift(pose, n):
    p = pose["root"].get("position", [0, 0, 0])
    pose["root"]["position"] = [p[0], n, p[2]]


BODY_CORE = None


def centre(pose):
    """The x, z centre of the body's CORE boxes (legs left out), px."""
    global BODY_CORE
    if BODY_CORE is None:
        BODY_CORE = [i for i, b in enumerate(S.m.cube_bone) if b in CORE and b not in LEG_SET]
    SCENE.pose = pose
    c = SCENE.evaluate(0).corners[BODY_CORE].reshape(-1, 3) * 16.0
    return np.array([(c[:, 0].min() + c[:, 0].max()) / 2, (c[:, 2].min() + c[:, 2].max()) / 2])


def death_clip(direction):
    fn = death(direction)
    n = int(20 * SUB)
    samples = []
    tag = "death_" + direction
    signs = None
    poses = []
    for i in range(n + 1):
        t = i / SUB
        up, body, feet, extra = fn(t)
        pose = {k: {c: list(v) for c, v in d.items()} for k, d in up.items()}
        pose["body"] = {c: list(v) for c, v in body.items()}
        pose["root"] = dict(extra["root"])
        SOLVER.legs(pose, feet, tag)
        lim = extra["_lim"]
        for l, (fy, fz, tz) in extra["_curl"].items():
            a = pose[l]["rotation"]; b = pose[l + "_low"]["rotation"]
            pose[l] = {"rotation": [a[0] * (1 - lim), a[1] + (fy - a[1]) * lim, a[2] + (fz - a[2]) * lim]}
            pose[l + "_low"] = {"rotation": [0, 0, b[2] + (tz - b[2]) * lim]}
        if direction == "forward":   # flat on its belly: the fangs and mandibles splay forward off the turf, the head and the
            for f, most in [(f, 70.0) for f in FANGS] + [("mandible_left", 40.0), ("mandible_right", 40.0), ("palps", 60.0)]:
                _clear(pose, f, {f}, most, -1)                             # abdomen lift only what still digs in
            _clear(pose, "head", {"head", "mask"}, 30.0, -1)
            _clear(pose, "abdomen", {"abdomen", "sacs", "cords"}, 30.0, +1)
        full(pose)
        poses.append((t, pose))
    # which way the head and abdomen fold their gear clear: decided once, on the final pose, so the keys stay continuous
    t_end, last = poses[-1]
    last = {k: {c: list(v) for c, v in d.items()} for k, d in last.items()}
    set_lift(last, rest_height(last))
    signs = {}
    for joint, gear in (("head", FOLD_HEAD), ("abdomen", FOLD_ABD)):
        a = [_fold_angle(last, joint, gear, 75.0, s) for s in (-1, 1)]
        signs[joint] = -1 if a[1] is None or (a[0] is not None and a[0] <= a[1]) else 1
    c0 = None
    lifts = []
    for t, pose in poses:
        free = max(0.0, -lowest(pose))                      # standing and falling: nothing digs in
        set_lift(pose, free)
        rest = free
        rx0 = {j: pose[j]["rotation"][0] for j in ("head", "abdomen")}
        for _ in range(3):                                  # lying: on the CORE, the tier gear folded clear
            rest = rest_height(pose)
            set_lift(pose, rest)
            for joint, gear in (("head", FOLD_HEAD), ("abdomen", FOLD_ABD)):
                _clear(pose, joint, gear, 75.0, signs[joint])
        lifts.append((free, rx0, {j: pose[j]["rotation"][0] - rx0[j] for j in rx0}))
    if direction in FOLD_EASE:
        # Batch review 2026-11-11: on its back the trophy meets the floor within a quarter tick and the abdomen folded
        # 72 deg in one sample (focus_check pop 70). The fold now starts early and eases in: each sample takes the
        # largest fold of the next FOLD_EASE ticks, scaled down by how far ahead it is, so it is never less than the
        # floor needs.
        n_ahead = int(FOLD_EASE[direction] * SUB)
        for j in ("head", "abdomen"):
            raw = [f[2][j] for f in lifts]
            for i, (t, pose) in enumerate(poses):
                v = max((raw[k] * ss(1.0 - (k - i) / n_ahead) for k in range(i, min(len(raw), i + n_ahead + 1))),
                        key=abs)
                pose[j]["rotation"][0] = lifts[i][1][j] + v
    for (t, pose), (free, _, _) in zip(poses, lifts):
        w = ss((t - 3.0) / 5.0)
        lift = free * (1 - w) + rest_height(pose) * w
        # the corpse lies over the spot it stood on (its shadow): a roll turns about the root on the floor and would carry the
        # body 3 px sideways; a fall on the back may keep 2 px of its backward travel
        if c0 is None:
            c0 = centre(pose)
        dx, dz = centre(pose) - c0
        dz = dz - max(-2.0, min(2.0, dz))
        pose["root"]["position"] = [-dx * w, 0, dz * w]       # anim z runs back, the scene z forward
        lift += 0.8 * max(0.0, damp(t, 9.0, 2.2, 0.8))    # the bounce as it lands
        set_lift(pose, lift)
        samples.append(pose)
    CLIPS[tag] = bake(samples, 20, "hold_on_last_frame", ALL)
    fin = samples[-1]
    REPORT[tag] = (0.0, min(lowest(p, CORE) for p in samples[::4]), 0.0)
    TIER_REPORT[tag] = [(lowest(fin, tier_bones(k) - SPURS), lowest(fin, tier_bones(k))) for k in (1, 2, 3)]
    return samples


TIER_REPORT = {}
FOLD_EASE = {"back": 2.0}    # deaths whose head and abdomen folds ease in (ticks of look-ahead)


# ------------------------------------------------------------------------------------------------ main
def delta_of(samples, ref):
    out = []
    for smp in samples:
        dd = {}
        for b in UPPER + LOWER + (["trophy"] if "trophy" in smp else []):
            rr = ref.get(b, {}).get("rotation", [0, 0, 0])
            dd[b] = {"rotation": [a - c for a, c in zip(smp[b]["rotation"], rr)]}
            if b in ("body", "head", "thorax", "trophy") or (b == "abdomen" and "position" in smp[b]):
                dd[b]["position"] = [a - c for a, c in zip(smp[b].get("position", [0, 0, 0]), ref.get(b, {}).get("position", [0, 0, 0]))]
        out.append(dd)
    return out


LEG_BONES = set(IK.LEGS) | {l + "_low" for l in IK.LEGS}


def fade_legs_end(dl, L, ticks=2.0):
    """Fix round 1: with `keep` the steady solve can finish a hit on another of the leg's equal answers for the same claw
    (hit_medium_front left_leg_4 17 deg off the ready solve at t16), so the delta was not zero at the end and the legs
    would snap back when the clip stops. The leg deltas fade to zero over the last `ticks`, the claw staying planted."""
    for i, dd in enumerate(dl):
        w = ss((i / SUB - (L - ticks)) / ticks)
        if w <= 0:
            continue
        for b in LEG_BONES & set(dd):
            dd[b]["rotation"] = [v * (1 - w) for v in dd[b]["rotation"]]
    return dl


def pulled_clip(ready0):
    dl = fade_legs_end(delta_of(swing_trophy(sample_clip("hit_pulled", PULL_T, pulled, loop=False)), ready0), PULL_T)
    CLIPS["hit_pulled"] = bake(dl, PULL_T, False, UPPER_T)
    CLIPS["hit_pulled_legs"] = bake(dl, PULL_T, False, LOWER)


def build_all(only=None):
    def want(nm):
        return only is None or nm in only
    if want("idle"):
        emit("idle", IDLE_P, sample_clip("idle", IDLE_P, idle), True)
    if want("walk"):
        emit("walk", WALK_P, sample_clip("walk", WALK_P, walk), True)
    if want("run"):
        emit("run", RUN_P, sample_clip("run", RUN_P, run), True)
    for nm, fn in (("ready", ready), ("brace", brace), ("wall", wall)):
        if want("stance_" + nm):
            emit("stance_" + nm, READY_P, sample_clip("stance_" + nm, READY_P, fn), True)
    if want("stance_fallback"):
        emit("stance_fallback", WALK_P, sample_clip("stance_fallback", WALK_P, fallback), True)
    if want("stance_ward"):
        CLIPS["stance_ward_upper"] = bake(sample_clip("stance_ward", READY_P, ward, solve=False), READY_P, True, UPPER)
    tl = {fmt_t(IMPACT / TPS): "impact"}
    for nm, fn in (("attack_bite", bite), ("attack_lunge", lunge), ("ensnare", ensnare)):
        if want(nm):
            emit(nm, ATK_T, swing_trophy(sample_clip(nm, ATK_T, fn, loop=False)), False, upper_name=nm,
                 legs_name=nm + "_legs", timeline=tl)
    ready0, _ = pose_of(ready_pose, 0, "ready0")
    READY_SOL.update({l: list(ready0[l]["rotation"]) + [ready0[l + "_low"]["rotation"][2]] for l in IK.LEGS})
    HOME.update({f"hit_{k}_{d}": hit(k, d)[0] for k in ("light", "medium") for d in ("front", "right", "left", "back")
                 if f"hit_{k}_{d}" in STEADY})
    HOME.update(hit_knockback=20, hit_pulled=PULL_T)
    for kind in ("light", "medium"):
        for d in ("front", "back", "right", "left"):
            nm = f"hit_{kind}_{d}"
            if not want(nm):
                continue
            L, fn = hit(kind, d)
            s = sample_clip(nm, L, fn, loop=False)
            if nm in STEADY:
                s = swing_trophy(s)
            dl = delta_of(s, ready0)
            if nm in STEADY:
                dl = fade_legs_end(dl, L)
            CLIPS[nm] = bake(dl, L, False, UPPER_T if nm in STEADY else UPPER)
            CLIPS[nm + "_legs"] = bake(dl, L, False, LOWER)
    if want("hit_knockback"):
        dl = fade_legs_end(delta_of(swing_trophy(sample_clip("hit_knockback", 20, knockback, loop=False)), ready0), 20)
        CLIPS["hit_knockback"] = bake(dl, 20, False, UPPER_T)
        CLIPS["hit_knockback_legs"] = bake(dl, 20, False, LOWER)
    if want("hit_pulled"):
        pulled_clip(ready0)
    if want("stun"):
        s = sample_clip("stun", STUN_ENTRY + STUN_LOOP, stun, loop=False)
        CLIPS["stun_upper"] = bake(s, STUN_ENTRY + STUN_LOOP, True, UPPER_T)
        CLIPS["stun_legs"] = bake(s, STUN_ENTRY + STUN_LOOP, True, LOWER)
    if want("stun_out"):
        so = []
        for i in range(6 * SUB + 1):
            d = stun_out(i / SUB)
            so.append({b: d.get(b, {"rotation": [0, 0, 0]}) for b in UPPER_T})
        CLIPS["stun_out_upper"] = bake(so, 6, False, UPPER_T)
    for d in ("back", "forward", "left", "right"):
        if want("death_" + d):
            death_clip(d)


def main():
    global STAND_GUESS
    # the standing solution, the fallback guess for every leg
    p0, _ = pose_of(idle, 0, "stand0")
    STAND_GUESS = {l: list(p0[l]["rotation"]) + [p0[l + "_low"]["rotation"][2]] for l in IK.LEGS}
    path = os.path.join(HERE, "out", "wm_stalker.animation.json")
    if any(f in sys.argv for f in ("--deaths", "--fallback", "--ensnare", "--pulled")):   # rebuild only these, spliced in
        out = json.load(open(path))
        if "--pulled" in sys.argv:
            pulled_clip(pose_of(ready_pose, 0, "ready0")[0])
        if "--ensnare" in sys.argv:
            emit("ensnare", ATK_T, swing_trophy(sample_clip("ensnare", ATK_T, ensnare, loop=False)), False, upper_name="ensnare",
                 legs_name="ensnare_legs", timeline={fmt_t(IMPACT / TPS): "impact"})
        if "--fallback" in sys.argv:
            emit("stance_fallback", WALK_P, sample_clip("stance_fallback", WALK_P, fallback), True)
        if "--deaths" in sys.argv:
            for d in ("back", "forward", "left", "right"):
                death_clip(d)
        out["animations"].update(CLIPS)
        json.dump(out, open(path, "w"), separators=(",", ":"))
        print("spliced", sorted(CLIPS))
        for k, (w, lb, ll) in REPORT.items():
            print(f"  {k:24s} {w:6.3f} {lb:7.2f} {ll:7.2f}")
        for k, v in TIER_REPORT.items():
            print(f"  {k:24s} final lowest by tier (no spurs / with spurs): " + "  ".join(f"t{i+1} {a:5.2f}/{b:5.2f}" for i, (a, b) in enumerate(v)))
        return
    only = None
    if "--only" in sys.argv:     # rebuild just these clips (stems: idle, walk, stance_wall, attack_bite, hit_medium_left,
        only = set(sys.argv[sys.argv.index("--only") + 1].split(","))   # stun, stun_out, death_back, ...), spliced in
    build_all(only)
    if "--part" in sys.argv:     # write just the rebuilt clips to this file (parallel partial builds, merged afterwards)
        json.dump({"animations": CLIPS}, open(sys.argv[sys.argv.index("--part") + 1], "w"), separators=(",", ":"))
        print("part", sorted(CLIPS))
        for k, (w, lb, ll) in REPORT.items():
            print(f"  {k:24s} {w:6.3f} {lb:7.2f} {ll:7.2f}")
        return
    if only:
        out = json.load(open(path))
        out["animations"].update(CLIPS)
    else:
        # keep the committed file's clip order (hit_pulled was spliced in last), so a rebuild with no change is byte-identical
        prev = list(json.load(open(path))["animations"]) if os.path.exists(path) else []
        order = [k for k in prev if k in CLIPS] + [k for k in CLIPS if k not in prev]
        out = {"format_version": "1.8.0", "animations": {k: CLIPS[k] for k in order}}
    json.dump(out, open(path, "w"), separators=(",", ":"))
    print("wrote", path, len(CLIPS), "clips")
    print("clip: worst IK miss px / lowest body px / lowest leg px")
    for k, (w, lb, ll) in REPORT.items():
        print(f"  {k:24s} {w:6.3f} {lb:7.2f} {ll:7.2f}")
    for k, v in TIER_REPORT.items():
        print(f"  {k:24s} final lowest by tier (no spurs / with spurs): " + "  ".join(f"t{i+1} {a:5.2f}/{b:5.2f}" for i, (a, b) in enumerate(v)))


if __name__ == "__main__":
    main()
