"""Thrower clips (unit animation rebuild, docs/anim-rebuild/STYLE.md): the thrown-weapon family, first the Tidecaller harpooner.

GG_THROW_TROOP=tc_harpooner python art/anim/thrower/build_thrower_anims.py  ->  art/anim/tc_harpooner/out/tc_harpooner.animation.json

Reuses the knight builder's rig maths, gait legs, hit footwork and death falls (art/anim/knight/build_anims.py with GG_ANIM_TROOP
set: no shield), the spearman's left-lead footing and gaze-held head, and the crossbow builder's draw channel.

The harpoon is solved, not hand-keyed: every key names where the right fist sits relative to the right shoulder (R, troop px),
or how far it has gone to the quiver (q), which way the shaft points (d, foot to tip) and which way its side faces (u). Each
sample the right arm reaches the fist's point and the wrist turns the harpoon onto (d, u). The left arm is free and keyed by
its joint angles: it points the throw, then yanks in to the chest to whip the shoulders round.

The draw channel: the harpoon is two sibling bones (weapon_t<n>_rest / pull_0, rig kind "thrower"); the empty "draw" bone's
position x picks one (0 rest: the harpoon in the fist, 1 pull_0: thrown, the fist empty). GeoTroopRenderer reads it.

The throw (attack_throw, thrower_throw impactTicks 8): from the cocked guard the body coils back over the rear foot as the lead
foot strides, the left arm pointing at the foe; the load is held; then the hips and shoulders unwind, the left arm yanks in and
the right fist drives over the shoulder and through, the harpoon leaving on the server's impact tick. The fist follows through
low across the body; the right hand reaches back over the shoulder, draws the next harpoon up out of the quiver and swings it
over into the cocked guard.
"""
import json, math, os, sys
from pathlib import Path

TROOP = os.environ.get("GG_THROW_TROOP", "tc_harpooner")
os.environ["GG_ANIM_TROOP"] = TROOP
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "knight"))
import build_anims as K  # noqa: E402

SUB, TPS, Z3 = K.SUB, K.TPS, K.Z3
np = K.np
K.HELD = K.HELD + ("weapon",)
K.DEATH_BONES = tuple(b for b in K.DEATH_BONES if b != "cape")
_GEO = json.loads((HERE.parent / TROOP / "out" / f"{TROOP}.geo.json").read_text())["minecraft:geometry"][0]["bones"]
GEO_BONES = {b["name"] for b in _GEO}
DRAW = {"rest": 0.0, "pull_0": 1.0}

# Per troop: gear = hanging pieces and how much of the knight's cape lag each takes (rule 9); quiver_grip = quiver-local point
# the fist closes on to draw the next weapon; soft = hanging pieces that never hold a falling body up.
THROW_TROOPS = {
    "tc_harpooner": {"gear": {"rope_back": 0.5, "rope_front": 0.35, "brim_tail": 0.4, "kelp_pad": 0.5, "kelp_belt": 0.6,
                              "kelp_hat": 0.35},
                     "quiver_grip": (-1.0, 23.8, 3.6), "soft": ("kelp_", "brim_tail", "rope_")},
}
ST = THROW_TROOPS[TROOP]
K.SOFT = ST["soft"]
LOOKAHEAD = True          # the long shaft: the wrist turn through a fall is planned backwards (see flat_weapon)
DEATH_STEP = 0.25
TOP_W, TOP_OK = 0.15, 8.0
W = "weapon_t1"           # every tier's harpoon bones share the pivot and axes

# ------------------------------------------------------------------------------------------------
# The harpoon frame. Weapon-local px: the shaft runs along y at x -6, z 0; the foot at y 0, the fist at y 14 (the hand pivot),
# the barbed head y 34-47. d = weapon +y (foot to tip), u = weapon -z; the barbs spread along weapon x.
# ------------------------------------------------------------------------------------------------

_HAND_TO_W = K.world({}, "right_hand")[:3, :3].T @ K.world({}, W)[:3, :3]
_PIV = {b["name"]: b["pivot"] for b in _GEO}
GX, GY = float(_PIV["right_hand"][0]), float(_PIV["right_hand"][1])
GRIP = (GX, GY, 0.0)
SHOULDER = tuple(map(float, _PIV["right_arm"]))
_WY = [c["origin"][1] + k * c["size"][1] for b in _GEO if b["name"].startswith("bow_t1_rest") for c in b.get("cubes", [])
       for k in (0, 1)]
BUTT, TIP = min(_WY) - GY, max(_WY) - GY
QGRIP = tuple(map(float, ST["quiver_grip"]))
SIDE = (1.0, 0.0, 0.0)    # the default u: the shaft's side toward the troop's left, so the barbs stand upright


def S(s):
    return (GX, GY + s, 0.0)


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def frame(d, u):
    d = unit(d)
    u = np.asarray(u, float) - (np.asarray(u, float) @ d) * d
    return d, unit(u)


def hold_wrist(pose, d, u):
    """right_hand rotation turning the harpoon onto (d, u)."""
    d, u = frame(d, u)
    Mw = np.column_stack([np.cross(d, -u), d, -u])
    return K.decompose_zyx(K.world(pose, "right_forearm")[:3, :3].T @ (Mw @ _HAND_TO_W.T))


def gn(f, x0, iters=60, lam=0.02, clamp=None):
    x = np.array(x0, dtype=float)
    for _ in range(iters):
        r = f(x)
        J = np.zeros((len(r), len(x)))
        for i in range(len(x)):
            dx = np.zeros(len(x))
            dx[i] = 1e-3
            J[:, i] = (f(x + dx) - r) / 1e-3
        step = np.linalg.solve(J.T @ J + lam * np.eye(len(x)), -J.T @ r)
        n = np.linalg.norm(step)
        if n > 8:
            step *= 8 / n
        x += step
        if clamp:
            x = clamp(x)
        if n < 1e-4:
            break
    return x


def _arm(side, pose, x):
    p = dict(pose)
    p[side + "_arm"] = (tuple(x[:3]), Z3)
    p[side + "_forearm"] = ((x[3], 0.0, 0.0), Z3)
    return p


def reach(pose, target, prior, warm=None, w=0.02):
    """The right arm (x, y, z) and forearm x putting the fist on target, nearest the prior arm."""
    prior = np.asarray(prior, float)

    def f(x):
        return np.concatenate([4.0 * (K.point(K.world(_arm("right", pose, x), "right_hand"), GRIP) - target), w * (x - prior)])

    x = gn(f, prior if warm is None else warm, clamp=lambda x: np.r_[x[:3], min(-2.0, max(-150.0, x[3]))])
    return x, np.linalg.norm(f(x)[:3]) / 4.0


def wpoint(pose, a):
    return K.point(K.world(pose, W), a)


def aim(elev, yaw):
    """A world direction: elev up from level, yaw toward the troop's left (+x)."""
    e, y = math.radians(elev), math.radians(yaw)
    return np.array([math.sin(y) * math.cos(e), math.sin(e), math.cos(y) * math.cos(e)])


def shoulder(pose):
    return K.point(K.world(pose, "right_arm"), SHOULDER)


def quiver_point(pose, Lq):
    return K.point(K.world(pose, "quiver"), Lq)


QUIVER_AXIS = unit(quiver_point({}, (QGRIP[0], QGRIP[1] + 4.0, QGRIP[2])) - quiver_point({}, QGRIP))


# ------------------------------------------------------------------------------------------------
# Body boxes: the shaft must stay out of the torso, hips, head and legs.
# ------------------------------------------------------------------------------------------------

def body_boxes(pose):
    P, owner = K.body_corners(pose)
    out = []
    for c in range(0, len(owner), 8):
        if owner[c] not in ("torso", "hips", "head", "right_leg", "left_leg", "right_shin", "left_shin"):
            continue
        out.append((owner[c], P[c], [(ax / np.linalg.norm(ax), np.linalg.norm(ax)) for ax in (P[c + 1] - P[c], P[c + 2] - P[c], P[c + 6] - P[c])
                                     if np.linalg.norm(ax) > 1e-6]))
    return out


def shaft_hits(pose, margin=0.3):
    boxes = body_boxes(pose)
    hits = []
    for s in np.arange(BUTT, TIP + 0.01, 1.0):
        p = wpoint(pose, S(s))
        for name, o, axes in boxes:
            if all(margin < (p - o) @ a < L - margin for a, L in axes):
                hits.append((round(float(s), 1), name))
    return hits


# ------------------------------------------------------------------------------------------------
# Left-lead footing (the spearman's): left foot forward, hips turned 20 deg side-on behind it. The knight's right-lead hit and
# block steps are mirrored onto it.
# ------------------------------------------------------------------------------------------------

_KN_HIPS, _KN_FEET = K.READY_HIPS, dict(K.READY_FEET)
READY_HIPS = ((0.0, 20.0, 0.0), (0.0, -1.3, 0.1))
READY_FEET = {"left": ((3.4, 2.0, 2.4), 14.0), "right": ((-3.2, 2.0, -2.2), -6.0)}
K.READY_HIPS, K.READY_FEET = READY_HIPS, READY_FEET
STANCE_LEGS = {
    "ready": {"hips": READY_HIPS, "left": (3.4, 2.0, 2.4), "right": (-3.2, 2.0, -2.2), "yaw": (-6.0, 14.0)},
    "brace": {"hips": ((0.0, 24.0, 0.0), (0.0, -1.5, 0.2)), "left": (3.0, 2.0, 3.0), "right": (-2.8, 2.0, -2.8), "yaw": (-8.0, 16.0)},
    "wall": {"hips": ((4.0, 28.0, 0.0), (0.0, -3.4, 0.6)), "left": (3.4, 2.0, 4.2), "right": (-3.2, 2.0, -4.0), "yaw": (-10.0, 22.0)},
}


def _mirror_hips(rot, pos):
    dr = [a - b for a, b in zip(rot, _KN_HIPS[0])]
    dp = [a - b for a, b in zip(pos, _KN_HIPS[1])]
    return (tuple(a + b for a, b in zip(READY_HIPS[0], (dr[0], -dr[1], -dr[2]))),
            tuple(a + b for a, b in zip(READY_HIPS[1], (-dp[0], dp[1], dp[2]))))


def mirror_legs(spec):
    hips = [(t, *_mirror_hips(r, p)) for t, r, p in spec["hips"]]
    steps = []
    for side, t0, t1, to, yaw, *lift in spec["steps"]:
        new = "left" if side == "right" else "right"
        (kp, ky), (mp, my) = _KN_FEET[side], READY_FEET[new]
        steps.append((new, t0, t1, (-to[0] + mp[0] + kp[0], to[1] + mp[1] - kp[1], to[2] + mp[2] - kp[2]), -yaw + my + ky, *lift))
    return {"hips": hips, "steps": steps}


_KH = K.HIT_LEGS
K.HIT_LEGS = {"front": mirror_legs(_KH["front"]), "back": mirror_legs(_KH["back"]),
              "right": mirror_legs(_KH["left"]), "left": mirror_legs(_KH["right"])}
K.BLOCK_LEGS = mirror_legs(K.BLOCK_LEGS)


def stance_legs(spec, life=1.0, ticks=K.STANCE_TICKS, ready=False):
    n = ticks * SUB
    out, guesses = [], {}
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)    # ready: everything at V2_READY_SWAY_SCALE; others: the rock at V2_SWAY_SCALE
    for i in range(n + 1):
        u = (i % n) / n
        br = a_br * math.sin(2 * math.pi * u)
        wt = a_wt * math.sin(2 * math.pi * u - K.WEIGHT)
        (rx, ry, rz), (px, py, pz) = spec["hips"]
        hips = ((rx + 1.5 * wt, ry + 2.2 * wt, rz - 1.6 * wt),
                (px + 0.35 * wt, py - 0.1 * br - 0.12 * abs(wt) - 0.22 * max(0.0, wt), pz - 0.6 * wt))
        pose = {"hips": hips}
        f = {"hips": hips}
        for side in ("right", "left"):
            pose[side + "_leg"] = ((0, -ry, 0), (0, 0, 0))
            yaw = spec["yaw"][0 if side == "right" else 1]
            lr, sr, fr = K.leg_pose(pose, side, spec[side], 0.0, foot_yaw=yaw, guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            f[side + "_leg"], f[side + "_shin"], f[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        out.append(f)
    out[-1] = out[0]
    return K.bake(out, ticks)


def head_local(gaze, torso, hips_rot):
    """The head holds its gaze: the key gives where the eyes look (troop frame); the hips' and torso's turn is taken out."""
    return tuple(g - t - h for g, t, h in zip(gaze, torso, hips_rot))


# ------------------------------------------------------------------------------------------------
# Keys. Each: torso (deg, on the hips), gaze (deg, troop frame), R (the fist's point from the right shoulder, troop px), d, u,
# rp (the right arm prior the solve starts from), lp (the free left arm: arm x y z, forearm x), q (0 at R, 1 at the quiver).
# ------------------------------------------------------------------------------------------------

RP = (-150.0, 0.0, 30.0, -90.0)
LP = (-55.0, 0.0, -12.0, -35.0)


def KEY(torso, gaze, R, d, u=SIDE, rp=RP, lp=LP, q=0.0, Lq=QGRIP):
    d, u = frame(d, u)
    return {"torso": tuple(map(float, torso)), "head": tuple(map(float, gaze)), "R": tuple(map(float, R)), "d": tuple(d),
            "u": tuple(u), "rp": tuple(map(float, rp)), "lp": tuple(map(float, lp)), "q": (float(q),), "Lq": tuple(map(float, Lq))}


def nudge(k, **kw):
    q = dict(k)
    for n, v in kw.items():
        if n in ("R", "torso", "head", "lp"):
            q[n] = tuple(a + b for a, b in zip(k[n], v))
        elif n == "d":
            q["d"], q["u"] = map(tuple, frame(v, k["u"]))
        else:
            q[n] = v
    return q


GAZE = (4.0, 0.0, 0.0)
# The guard: the harpoon cocked overhand, the fist up beside the right ear, the point level at the foe; side-on behind the
# lead foot, the free left arm half raised before the chest. From here the throw is a coil and a whip.
GUARD = KEY((4, 10, 0), GAZE, (-1.0, 3.0, -2.5), aim(8, -4))

ATTACK_TICKS = 36
IMPACT = 8          # thrower_throw impactTicks: the harpoon leaves


def attack_keys():
    coil_a = KEY((-5, 18, 0), (3, 0, 0), (-0.6, 3.4, -5.8), aim(20, -4), lp=(-80, 0, -9, -12))
    coil = KEY((-10, 26, 0), (2, 0, 0), (-0.3, 3.8, -6.6), aim(28, -4), lp=(-95, 0, -6, -4))
    held = nudge(coil, torso=(-1.5, 2, 0), R=(0.0, 0.1, -0.3), lp=(-2, 0, 0, 0))
    drive = KEY((4, -6, 0), (2, 0, 0), (0.0, 6.5, 0.5), aim(16, -2), lp=(-40, 0, -4, -78))
    # the left arm yanks in to the chest with the elbow kept out at the side, so the bracer clears the ribs
    release = KEY((14, -22, 0), (6, 0, 0), (1.5, 1.0, 6.8), aim(-5, 0), lp=(-25, 0, -26, -90))
    over = KEY((20, -26, -4), (10, 0, 0), (4.0, -5.0, 4.0), aim(-35, 15), lp=(-10, 0, -30, -80))
    recover = KEY((10, -12, 0), (6, 0, 0), (1.0, -4.0, 3.5), aim(-20, 5), lp=(-30, 0, -20, -60))
    # the fist goes back over the right shoulder behind the ear and closes on the next harpoon; drawn up out of the quiver it
    # stands angled up and back past the head, the foot clear outside the right hip, then the fist carries it over
    BACK = unit((0.3, 0.85, -0.4))
    reach_q = KEY((2, 6, 0), (6, 10, 2), (-0.5, 4.0, -4.6), BACK, lp=(-40, 0, -15, -40))
    grab = KEY((0, 8, 0), (6, 14, 3), (-0.6, 3.6, -4.9), BACK, lp=(-42, 0, -15, -42))
    draw_out = KEY((0, 8, 0), (6, 12, 2), (-0.6, 6.0, -4.2), unit((0.25, 0.9, -0.35)), lp=(-44, 0, -15, -41))
    lift = KEY((0, 8, 0), (5, 6, 1), (-0.3, 6.6, -2.6), unit((0.1, 1.0, -0.15)), lp=(-46, 0, -14, -40))
    swing = KEY((2, 10, 0), (4, 2, 0), (-1.0, 4.5, -1.5), aim(50, -4), lp=(-52, 0, -12, -36))
    over_g = nudge(GUARD, R=(0.0, -0.4, 0.3), d=aim(3, -4), torso=(1, -1, 0))
    settle = nudge(GUARD, R=(0.0, 0.15, -0.1), d=aim(9, -4))
    # on the way up to the quiver the empty fist swings out wide of the right shoulder (the elbow opens instead of folding
    # shut against the head)
    wide = KEY((6, -3, 0), (6, 5, 1), (-5.5, 1.0, 0.0), unit((-0.6, 0.5, -0.3)), lp=(-35, 0, -10, -50))
    return [(0, GUARD), (3.0, coil_a), (5.0, coil), (6.6, held),            # coil back over the rear foot; the load held
            (7.4, drive), (IMPACT, release),                                  # unwind and whip: the harpoon leaves
            (10.0, over), (13.0, recover),                                    # the follow-through low across the body
            (14.8, wide), (16.5, reach_q), (18.5, grab), (20.0, draw_out), (22.0, lift),                      # over the shoulder, the next harpoon drawn up
            (25.0, swing), (29.0, over_g), (33.0, settle), (ATTACK_TICKS, GUARD)]


# The draw frame: the harpoon in the fist until the release; thrown (the fist empty) until it closes on the next one.
DRAW_STEPS = [(0, "rest"), (IMPACT, "pull_0"), (18.5, "rest")]


def draw_at(t):
    v = 0.0
    for t0, f in DRAW_STEPS:
        if t >= t0:
            v = DRAW[f]
    return v


def attack_legs():
    """Footwork (STYLE.md rule 4): the lead foot strides as the body coils back over the rear foot; the hips unwind and drop
    over the lead foot at the release, the rear foot drags through; both come back to the guard while the next harpoon is drawn."""
    R0, R1 = READY_HIPS
    spec = {"hips": [(0, R0, R1), (3.0, (-2.0, 30.0, 0.0), (0.0, -1.5, -0.4)), (5.0, (-3.0, 34.0, 0.0), (0.0, -1.6, -0.6)),
                     (6.6, (-3.0, 35.0, 0.0), (0.0, -1.7, -0.6)), (7.4, (2.0, 22.0, 0.0), (0.0, -1.9, 0.4)),
                     (IMPACT, (5.0, 8.0, 0.0), (0.0, -2.4, 1.0)), (10.0, (7.0, 2.0, -2.0), (0.0, -2.7, 1.4)),
                     (13.0, (4.0, 10.0, 0.0), (0.0, -2.1, 0.8)), (17.0, (1.0, 18.0, 0.0), (0.0, -1.5, 0.3)),
                     (24.0, (0.0, 20.0, 0.0), (0.0, -1.4, 0.2)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            "steps": [("left", 2.0, 6.0, (3.8, 2.0, 4.4), 20.0), ("right", 9.0, 13.0, (-3.0, 2.0, -0.6), -2.0),
                      ("left", 22.0, 27.0, (3.4, 2.0, 2.4), 14.0), ("right", 27.0, 31.0, (-3.2, 2.0, -2.2), -6.0)]}
    return K.attack_legs(spec, ticks=ATTACK_TICKS)


def fk_pose(s, legs):
    pose = {b: (v[0], v[1] or Z3) for b, v in s.items() if b != "draw"}
    pose.update({b: (v[0], v[1] or Z3) for b, v in legs.items()})
    return pose


def interp(keys, t, name):
    return K.curve([(k[0], k[1][name]) for k in keys], t)


class Solver:
    """Solves sample after sample along one clip, the arm warm-started from the last sample so the elbow never flips."""

    def __init__(self):
        self.warm = None
        self.worst = 0.0

    def pose(self, legs, torso, head, R, d, u, rp, lp, q=0.0, Lq=QGRIP, extra=None, w=0.02):
        s = {"torso": (tuple(torso), None), "head": (tuple(head), None)}
        if extra:
            s.update(extra)
        pose = fk_pose(s, legs)
        O = shoulder(pose) + np.asarray(R, float)
        if q > 0.0:
            O = (1.0 - q) * O + q * quiver_point(pose, Lq)
        x, e = reach(pose, O, rp, self.warm, w)
        self.warm = x
        self.worst = max(self.worst, e)
        s["right_arm"], s["right_forearm"] = (tuple(x[:3]), None), ((x[3], 0.0, 0.0), None)
        pose = fk_pose(s, legs)
        s["right_hand"] = (hold_wrist(pose, d, u), None)
        s["left_arm"], s["left_forearm"] = (tuple(lp[:3]), None), ((lp[3], 0.0, 0.0), None)
        s["left_hand"] = (Z3, None)
        return s


def sample(keys, legs_at, sol, t, extra=None, w=0.02):
    v = {n: interp(keys, t, n) for n in ("torso", "head", "R", "d", "u", "rp", "lp", "q", "Lq")}
    d, u = frame(v["d"], v["u"])
    head = head_local(v["head"], v["torso"], legs_at["hips"][0])
    return sol.pose(legs_at, v["torso"], head, v["R"], d, u, v["rp"], v["lp"], max(0.0, min(1.0, v["q"][0])), v["Lq"], extra, w)


def gear(samples, legs=None):
    """Secondary motion (STYLE.md rule 9): the knight's cape lag, scaled onto each hanging piece of the troop's gear."""
    K.cape_lag(samples, legs)
    for s in samples:
        c = s.pop("cape")[0]
        for g, k in ST["gear"].items():
            s[g] = ((k * c[0], 0.0, k * c[2]), None)
        s.setdefault("quiver", (Z3, None))
    return samples


UNWIND = (22.0, 27.0, 31.0, 35.0, 0.3)   # the prior pull ramps up over 22-27, holds, and fades by 35


def unwind_w(t):
    """The over-the-shoulder draw leaves the arm on a twisted IK branch (elbow up, the wrist wound round) that matches the
    guard's fist and harpoon but not its joints, so blending into idle spun the arm a full turn. Pulling harder toward the
    prior over the swing back to guard slides the elbow down along its swivel onto the guard's own solution."""
    a, b, c, e, k = UNWIND
    return 0.02 + k * (K.smooth((t - a) / (b - a)) if t < c else K.smooth((e - t) / (e - c)))


def attack_clip():
    keys, legs = attack_keys(), attack_legs()
    sol = Solver()
    out, hits = [], []
    for i in range(ATTACK_TICKS * SUB + 1):
        t = i / SUB
        s = sample(keys, legs[i], sol, t, w=unwind_w(t))
        s["draw"] = (Z3, (draw_at(t), 0.0, 0.0))
        if draw_at(t) == 0.0:
            h = shaft_hits(fk_pose(s, legs[i]))
            if h:
                hits.append((t, h[0]))
        out.append(s)
    print("attack_throw: fist err max %.3f px, samples with the shaft in the body %d %s" % (sol.worst, len(hits), hits))
    return out, legs


def rewind(clip):
    """The wrist rolls fast twice in the throw (the release flip and the unwind), and the bake's unwrap carries each roll on
    as whole turns, so the hand ends a turn away from where it began (x -360, z +360: the same rotation). The renderer blends
    the clip's eulers linearly into the guard, which would spin the hand a full turn. Take the whole turns back off from the
    largest step in the clip's second half, where the wrist already flips, so the clip ends on the guard's own numbers."""
    for b, ch in clip["bones"].items():
        rot = ch.get("rotation")
        if not rot:
            continue
        ks = sorted(rot, key=float)
        off = [360.0 * round((rot[ks[-1]][i] - rot[ks[0]][i]) / 360.0) for i in range(3)]
        if not any(off):
            continue
        half = [k for k in ks if float(k) >= float(ks[-1]) / 2]
        cut = max(range(1, len(half)), key=lambda j: sum(abs(rot[half[j]][i] - rot[half[j - 1]][i]) for i in range(3)))
        for k in half[cut:]:
            rot[k] = [round(rot[k][i] - off[i], 3) for i in range(3)]
        print("rewind %s: %s from %ss" % (b, off, half[cut]))


def attack_clips():
    upper, legs = attack_clip()
    # the quiver jolts as the body whips round and as the harpoon is drawn out of it
    for i, s in enumerate(upper):
        t = i / SUB
        j = 4.0 * math.exp(-max(0.0, t - IMPACT) / 1.5) * (t >= IMPACT) + 5.0 * math.sin(math.pi * np.clip((t - 18.5) / 3.5, 0, 1))
        s["quiver"] = ((j, 0.0, 0.4 * j), None)
    upper = gear(upper, legs)
    clip = K.bake(upper, ATTACK_TICKS, loop=False)
    rewind(clip)
    clip["timeline"] = {K.fmt_t(IMPACT / TPS): "impact"}
    return {"attack_throw": clip, "attack_throw_legs": K.bake(legs, ATTACK_TICKS, loop=False)}


# ------------------------------------------------------------------------------------------------
# Carry: the harpoon at the slope in the right fist, point up and ahead, the left arm free (the spearman's carry).
# ------------------------------------------------------------------------------------------------

CARRY_R = (-12.0, 0.0, 6.0)
CARRY_RF = -46.0


def carry_hand(pose, pitch, yaw=0.0):
    return hold_wrist(pose, aim(pitch, yaw), SIDE)


def gait_pose(g, c, u, run, legs):
    tw = 2 * math.pi
    (hx, hy, hz), _ = g.hips(u)
    ty = (g.yaw + c.twist) * math.cos(tw * u)
    tz = -0.8 * hz - c.roll * math.sin(tw * (u - 0.03))
    tx = c.lean + 0.6 * math.cos(tw * 2 * (u - g.bob_phase))
    net_yaw, net_roll = hy + ty, hz + tz
    head = (-(hx + tx) * c.head_keep, -net_yaw * c.head_keep, -net_roll * c.head_keep)
    sw = math.cos(tw * (u - c.arm_lag))
    bob = math.cos(tw * 2 * (u - g.bob_phase - 0.06))
    la = (c.arm_base - c.arm * sw, 0.0, -c.arm_out)
    lf = (c.fore - 0.35 * c.arm * max(0.0, sw), 0.0, 0.0)
    if run:
        ra = (-24.0 + 0.45 * c.arm * sw, 0.0, 9.0)
        rf = (-50.0 + 0.3 * c.arm * min(0.0, -sw), 0.0, 0.0)
    else:
        ra = (CARRY_R[0] + 0.45 * c.arm * sw, 0.0, CARRY_R[2])
        rf = (CARRY_RF - 0.3 * c.arm * max(0.0, -sw), 0.0, 0.0)
    s = {"torso": ((tx, ty, tz), None), "head": (head, None), "right_arm": (ra, None), "right_forearm": (rf, None),
         "left_arm": (la, None), "left_forearm": (lf, None), "left_hand": (Z3, None)}
    pose = fk_pose(s, legs)
    # the point rocks a beat behind the arm (rule 9: the weight lags)
    pitch = (26.0 if run else 56.0) + 3.0 * math.cos(tw * (u - c.arm_lag - 0.12))
    s["right_hand"] = (carry_hand(pose, pitch, -8.0 - 0.3 * (hy + ty)), None)
    flare, flap = c.cape
    cape = (flare + flap * math.sin(tw * 2 * (u - g.bob_phase - 0.12)), 0.0, -0.6 * tz)
    for gname, k in ST["gear"].items():
        s[gname] = ((k * cape[0], 0.0, k * cape[2]), None)
    s["quiver"] = ((1.5 * (2.0 if run else 1.0) * bob, 0.0, 2.0 * (2.0 if run else 1.0) * math.sin(tw * (u - 0.15))), None)
    return s


def _legs_samples(g, n):
    return [{"hips": (g.hips(i / n)[0], g.hips(i / n)[1])} for i in range(n)]


def gait_upper(g, c, run):
    n = g.ticks * SUB
    lsamp = _legs_samples(g, n)
    carry = type("C", (), {k: getattr(c, k) for k in dir(c) if not k.startswith("_")})
    carry.arm_base = -4.0 if not run else -16.0
    carry.arm_out = 6.0 if not run else 10.0
    carry.fore = -18.0 if not run else -50.0
    samples = [gait_pose(g, carry, i / n, run, lsamp[i]) for i in range(n)]
    low = min(wpoint(fk_pose(s, l), S(BUTT))[1] for s, l in zip(samples, lsamp))
    hits = sum(bool(shaft_hits(fk_pose(s, l))) for s, l in zip(samples, lsamp))
    print("%s carry: foot lowest %.1f px above the ground, samples with the shaft in the body %d" % (g.name, low, hits))
    samples.append(samples[0])
    return K.bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Stances and idle: all in guard (STYLE.md: the guard is the ready pose; idle stays in it). StanceController plays ready
# (fighting), brace / wall (Hold), ward (fall_back).
# ------------------------------------------------------------------------------------------------

STANCE_KEYS = {
    "ready": GUARD,
    # brace: sunk over the braced legs, the harpoon levelled at the chest like a short spear, ready to jab or loose
    "brace": KEY((16, 10, 0), (2, 0, 0), (-0.5, -1.0, 1.5), aim(-6, -4), lp=(-60, 0, -14, -40)),   # set low, point down at the charge
    # wall: the formed line, crouched, the harpoon cocked high over the shoulder, the left arm out to sight
    "wall": KEY((-6, 20, 0), (3, 0, 0), (-0.4, 3.6, -6.2), aim(24, -4), lp=(-85, 0, -8, -8)),   # cocked to throw, the line poised
    # ward: giving ground, the harpoon dropped to an underhand fend before the body, the barbs raised at the foe's face
    "ward": KEY((-2, 8, 0), (0, 0, 0), (-3.0, -7.0, 4.0), aim(30, -4), rp=(-45, 0, 15, -45), lp=(-40, 0, -18, -55)),
    # shield_up (the harpooner has no shield; StanceController plays this when the line moves under missiles): head ducked
    # behind the left forearm raised across the brow, the harpoon kept cocked low by the hip, point at the foe
    "shield_up": KEY((10, 10, 0), (10, 0, 0), (-2.5, -6.0, -1.0), aim(10, -4), rp=(-25, 0, 15, -40), lp=(-140, 0, -25, -95)),
}
LIFE = {"ready": 3.4, "brace": 1.6, "wall": 1.4, "ward": 2.0, "shield_up": 3.4}
IDLE_LIFE = 2.2
READY_LIKE = ("ready", "shield_up")     # these loops run at V2_READY_SWAY_SCALE (0.25); the other stances (and the idle) at V2_SWAY_SCALE
IDLE_LIFE_V2 = IDLE_LIFE

# V2 feel (Kort, 2026-11-07: "the whole feel and flow of [the Knight's] movements, its weight shifts, and style"). V2 is on by
# default with the knight builder's flag (GG_V2=0 builds the old clips). The throw is untouched.
V2 = K.V2
STANCE_LEGS_LIFE = dict(K.STANCE_LIFE)
if V2:
    # V2: the three commands read apart from the ready footing. brace sinks a step lower and wider, mirrored from the knight's v2
    # brace (hips down, feet out); the wall stays the deepest. The legs of the brace and the wall carry the same weight cycle as
    # their upper bodies, so the hips move with the chest instead of holding still under it.
    STANCE_LEGS["brace"] = {"hips": ((5.0, 28.0, 0.0), (0.0, -3.4, 0.8)), "left": (4.4, 2.0, 4.0), "right": (-4.2, 2.0, -4.0),
                            "yaw": (-12.0, 20.0)}
    # brace: sunk on the braced legs, the harpoon cocked back at the shoulder with the barbs dipped at the charge's chest, the
    # left arm out low pointing the line
    STANCE_KEYS["brace"] = KEY((18, 14, 0), (2, 0, 0), (-1.0, 2.5, -4.0), aim(-8, -4), lp=(-70, 0, -14, -20))
    IDLE_LIFE_V2 = 3.4       # the idle weight cycle: a clear hip shift over the guard, not just a breath
    STANCE_LEGS_LIFE.update({"brace": LIFE["brace"], "wall": LIFE["wall"]})


# Stances whose solve is warmed up on their own first sample: a cold first solve can land on another arm branch than the
# warm-started samples after it, and the loop then closes on that odd sample (a wrist flip at the seam). The ready guard and the
# wall are kept as they were (every derived stance clip starts from the ready).
PREWARM = ("brace", "ward", "shield_up")


def stance_upper(k, life, legs_spec, ticks=K.STANCE_TICKS, ready=False, prewarm=False):
    n = ticks * SUB
    sol = Solver()
    out = []
    hips = (legs_spec or STANCE_LEGS["ready"])["hips"]
    legs = {"hips": hips}
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)
    for i in ([n - 1, n - 1] if prewarm else []) + list(range(n + 1)):
        u = (i % n) / n
        br = a_br * math.sin(2 * math.pi * u)
        wt = a_wt * math.sin(2 * math.pi * u - K.WEIGHT)
        wl = a_wt * math.sin(2 * math.pi * u - K.WEIGHT - 0.7)
        tx, ty, tz = k["torso"]
        torso = (tx + 0.8 * br + 1.8 * wt, ty + 1.4 * wt, tz + 1.2 * wt)
        gaze = tuple(np.array(k["head"]) + np.array([-0.3 * br, 0.0, 0.0]))
        head = head_local(gaze, torso, hips[0])
        # the harpoon rides the breath a beat behind it, its point drifting
        R = np.array(k["R"]) + np.array([0.0, 0.12 * br + 0.15 * wl, 0.1 * wl])
        dd = np.array(k["d"]) + np.array([0.0, 0.006 * br + 0.01 * wl, 0.0])
        lp = np.array(k["lp"]) + np.array([-0.8 * wl, 0.0, 0.0, -1.2 * wl])
        d, uu = frame(dd, k["u"])
        s = sol.pose(legs, torso, head, R, d, uu, k["rp"], lp)
        s["quiver"] = ((0.3 * br, 0.0, 0.0), None)
        out.append(s)
    out = out[2:] if prewarm else out
    out = gear(out)
    out[-1] = out[0]
    return K.bake(out, ticks)


def stance_clips():
    out = {"stance_%s_upper" % k: stance_upper(v, LIFE[k], STANCE_LEGS.get(k), ready=k in READY_LIKE, prewarm=k in PREWARM)
           for k, v in STANCE_KEYS.items()}
    out.update({"stance_%s_legs" % k: stance_legs(v, STANCE_LEGS_LIFE.get(k, 1.0), ready=k in READY_LIKE) for k, v in STANCE_LEGS.items()})
    return out


def idle_clips(life=None):
    life = IDLE_LIFE if life is None else life
    return stance_legs(STANCE_LEGS["ready"], life, K.IDLE_TICKS), stance_upper(GUARD, life, None, K.IDLE_TICKS)


K.idle_clips = idle_clips    # the death falls start from the guard

# ------------------------------------------------------------------------------------------------
# Hits: additive offsets. The right fist keeps the harpoon; the free left arm flies wide and the torso takes the blow.
# ------------------------------------------------------------------------------------------------

off = K.off
HIT_PEAK = {
    "front": off(torso=(K.hb_front(-17), -10, 0), head=(K.hb(-12), 3, 0), rarm=(-10, 0, 14), larm=(-23, 0, -38), lfore=-14),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 10), larm=(14, 0, -24)),
    "right": off(torso=(K.hb(-5), 14, 8), head=(0, 6, 3), rarm=(6, 0, 10), larm=(-12, 0, -22)),
    "left": off(torso=(K.hb(-5), -14, -8), head=(0, -6, -3), rarm=(-10, 0, 14), larm=(12, 0, -30)),
}


def hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for dname, peak in HIT_PEAK.items():
            keys = K.hit_keys(peak, weight)
            out["hit_%s_%s" % (weight, dname)] = K.bake([K.sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)], ticks,
                                                       loop=False)
    out["hit_block"] = out["hit_light_front"]
    out.update(K.hit_legs_clips())
    return out


# V2 hits (the knight's v2 grading, rebuilt for the harpoon): a light blow is a flinch at the knees; a medium blow shifts the
# pelvis well off the footing and takes a short lifted recovery step; heavy (HitController: medium x1.55) is the stagger. The
# body keys are the knight's v2 timing. Both arms ride their own tracks that open outward and never fold back across the body:
# the harpoon arm (right) is held out through a blow from the front, then comes home with the gather; the free arm is flung
# out with the body and gathers without overshooting inward. The footwork is the knight's v2 table, mirrored onto this guard.
V2_LIGHT = 0.4
V2_BODY = {"front": 0.85, "back": 1.0, "right": 1.0, "left": 1.0}   # the unshielded front throw read as a tumble at heavy
ARM_BONES = {"left": ("left_arm", "left_forearm", "left_hand"), "right": ("right_arm", "right_forearm", "right_hand")}


def v2_keys(peak, weight, hold=False):
    sc = K.scaled
    if weight == "light":
        p = sc(peak, V2_LIGHT)
        return [(0, K.ZERO), (1.5, p), (4, sc(p, -0.12)), (6.5, sc(p, 0.04)), (9, K.ZERO)]
    if hold:    # held out and down through the whole throw, then home with the gather
        return [(0, K.ZERO), (2, sc(peak, 2.3)), (4.5, sc(peak, 2.0)), (7.5, sc(peak, 0.9)), (10.5, sc(peak, 0.2)), (16, K.ZERO)]
    return [(0, K.ZERO), (2, sc(peak, 2.3)), (4.5, sc(peak, 2.15)), (7.5, sc(peak, 0.55)), (10.5, sc(peak, -0.35)),
            (13, sc(peak, 0.08)), (16, K.ZERO)]


def only(peak, bones):
    return {b: (peak[b] if b in bones else Z3) for b in peak}


_R = _KN_HIPS     # the knight's ready hips: the table is the knight's v2 footwork, mirrored below onto this guard
_V2_KNIGHT_LEGS = {
    "front": {"hips": [(0, *_R), (2, (K.hb(-6), 9, 0), (0.0, -1.9, 1.6)), (4.5, (K.hb(-5), 9, 0), (0.0, -2.1, 1.9)),
                       (7.5, (1, 5, 0), (0.0, -1.8, 1.3)), (10.5, (K.hb(-1), 7, 0), (0.0, -1.3, 0.5)), (16, *_R)],
              "steps": [("left", 1.0, 3.8, (3.3, 2.0, -4.0), 9.0, 0.8), ("right", 3.5, 6.5, (-3.4, 2.0, 0.6), -13.0, 0.6),
                        ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
    "back": {"hips": [(0, *_R), (2, (6, 4, 0), (0.0, -1.8, -1.2)), (4.5, (5, 4, 0), (0.0, -1.9, -1.4)),
                      (7.5, (K.hb(-1), 7, 0), (0.0, -1.6, -0.8)), (10.5, (0.5, 6, 0), (0.0, -1.2, -0.2)), (16, *_R)],
             "steps": [("right", 1.0, 3.8, (-3.5, 2.0, 3.5), -12.0, 0.8), ("left", 3.5, 6.5, (3.2, 2.0, -0.6), 7.0, 0.6),
                       ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
    "right": {"hips": [(0, *_R), (2, (K.hb(-2), 14, 4), (1.0, -1.8, 0.5)), (4.5, (K.hb(-2), 13, 4), (1.2, -1.9, 0.5)),
                       (7.5, (1, 4, 0), (0.6, -1.6, 0.2)), (10.5, (0, 6.5, 0), (0.1, -1.2, 0.1)), (16, *_R)],
              "steps": [("left", 1.0, 3.8, (4.9, 2.0, -2.2), 12.0, 0.8), ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8)]},
    "left": {"hips": [(0, *_R), (2, (K.hb(-2), -2, -4), (-1.0, -1.8, 0.5)), (4.5, (K.hb(-2), -1, -4), (-1.2, -1.9, 0.5)),
                      (7.5, (1, 8, 0), (-0.6, -1.6, 0.2)), (10.5, (0, 5.5, 0), (-0.1, -1.2, 0.1)), (16, *_R)],
             "steps": [("right", 1.0, 3.8, (-5.0, 2.0, 1.6), -18.0, 0.8), ("right", 9.0, 12.5, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
}
# the same direction swap as the old table above (a blow from the knight's right lands on this guard's left)
V2_HIT_LEGS = {"front": mirror_legs(_V2_KNIGHT_LEGS["front"]), "back": mirror_legs(_V2_KNIGHT_LEGS["back"]),
               "right": mirror_legs(_V2_KNIGHT_LEGS["left"]), "left": mirror_legs(_V2_KNIGHT_LEGS["right"])}


def v2_hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for dname, peak in HIT_PEAK.items():
            pk = K.scaled(peak, V2_BODY[dname])
            armed = ARM_BONES["left"] + ARM_BONES["right"]
            body = only(pk, [b for b in pk if b not in armed])
            tracks = [(K.front_onset(v2_keys(body, weight), dname, weight), None),   # a 2-tick onset on the front medium blow
                      (K.front_onset(v2_keys(only(pk, ARM_BONES["right"]), weight, hold=dname == "front"), dname, weight), "right"),
                      (K.front_onset(v2_keys(only(pk, ARM_BONES["left"]), weight), dname, weight), "left")]
            samples = []
            for i in range(ticks * SUB + 1):
                smp = {}
                for keys, side in tracks:
                    v = K.sample_keys(keys, i / SUB)
                    smp.update(v if side is None else {x: v[x] for x in ARM_BONES[side]})
                samples.append(smp)
            out["hit_%s_%s" % (weight, dname)] = K.bake(samples, ticks, loop=False)
    out["hit_block"] = out["hit_light_front"]
    saved = K.HIT_LEGS
    K.HIT_LEGS = V2_HIT_LEGS     # K.hit_legs_clips reads the module global; restored at once (nothing else reads it here)
    try:
        legs = K.hit_legs_clips()
    finally:
        K.HIT_LEGS = saved
    legs["hit_block_legs"] = legs["hit_light_front_legs"]
    out.update(legs)
    return out


# ------------------------------------------------------------------------------------------------
# Deaths: the knight's four falls, the harpoon let go flat (the spearman's long-shaft solve: the wrist turns the shaft to lie on
# the ground clear of the body, tipped about the fist by the least angle that keeps it out of the floor, planned backwards).
# ------------------------------------------------------------------------------------------------

_CORNERS = K.body_corners


def is_weapon(bone):
    return "weapon" in bone or bone.startswith("bow_")


def body_corners_no_weapon(pose, model=K.HEAVY):
    P, owner = _CORNERS(pose, model)
    keep = [i for i, o in enumerate(owner) if not is_weapon(o)]
    return P[keep], [owner[i] for i in keep]


K.body_corners = body_corners_no_weapon

FLAT_AT = {"death_back": (8, 12), "death_forward": (13, 16), "death_left": (9, 13), "death_right": (9, 13)}
_ARMED = ("right_arm", "right_forearm", "right_hand", "right_wrap", "right_bracer", "weapon", "bow_")


def _rot(r):
    a, b, c = np.radians([-r[0], -r[1], r[2]])
    return (K.rg._Rz(c) @ K.rg._Ry(b) @ K.rg._Rx(a))[:3, :3]


def _quat(R):
    w = math.sqrt(max(0.0, 1.0 + R[0, 0] + R[1, 1] + R[2, 2])) / 2.0
    x = math.copysign(math.sqrt(max(0.0, 1.0 + R[0, 0] - R[1, 1] - R[2, 2])) / 2.0, R[2, 1] - R[1, 2])
    y = math.copysign(math.sqrt(max(0.0, 1.0 - R[0, 0] + R[1, 1] - R[2, 2])) / 2.0, R[0, 2] - R[2, 0])
    z = math.copysign(math.sqrt(max(0.0, 1.0 - R[0, 0] - R[1, 1] + R[2, 2])) / 2.0, R[1, 0] - R[0, 1])
    q = np.array([w, x, y, z])
    return q / np.linalg.norm(q)


def _mat(q):
    w, x, y, z = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


def slerp_rot(r0, r1, k):
    q0, q1 = _quat(_rot(r0)), _quat(_rot(r1))
    if q0 @ q1 < 0:
        q1 = -q1
    ang = math.acos(min(1.0, float(q0 @ q1)))
    q = q0 if ang < 1e-6 else (math.sin((1 - k) * ang) * q0 + math.sin(k * ang) * q1) / math.sin(ang)
    return K.decompose_zyx(_mat(q / np.linalg.norm(q)))


def turn_deg(r0, r1):
    return math.degrees(2 * math.acos(min(1.0, abs(float(_quat(_rot(r0)) @ _quat(_rot(r1)))))))


def _boxes(P, owner):
    out = []
    for c in range(0, len(owner), 8):
        if any(a in owner[c] for a in _ARMED):
            continue
        out.append((P[c], [(ax / np.linalg.norm(ax), np.linalg.norm(ax)) for ax in (P[c + 1] - P[c], P[c + 2] - P[c], P[c + 6] - P[c])
                           if np.linalg.norm(ax) > 1e-6]))
    return out


def _inside(pts, boxes):
    n = 0
    for o, axes in boxes:
        inside = np.ones(len(pts), bool)
        for a, L in axes:
            s = (pts - o) @ a
            inside &= (s > 0.2) & (s < L - 0.2)
        n += int(inside.sum())
    return n


def ramp(t, a, b):
    if t <= a:
        return 0.0
    if t >= b:
        return 1.0
    k = (t - a) / (b - a)
    return k * k * (3 - 2 * k)


def body_ground(P, owner, isw):
    hard = ~isw & np.array([not any(o.startswith(h) for h in K.SOFT) for o in owner], dtype=bool)
    return P[hard, 1].min()


def flat_hand(pose, ref):
    """The right wrist rotation letting the harpoon drop: it lies along the ground clear of the body, as near `ref` as it can."""
    q = dict(pose)
    q["right_hand"] = (ref, Z3)
    P, owner = _CORNERS(q)
    isw = np.array([is_weapon(o) for o in owner])
    ground = body_ground(P, owner, isw)
    boxes = _boxes(P, owner)
    L = np.linalg.inv(K.world(q, "right_hand")) @ np.c_[P[isw] / 16.0, np.ones(int(isw.sum()))].T
    best = None
    for phi in range(0, 360, 10):
        for tilt in range(-60, 65, 5):
            c, s = math.cos(math.radians(phi)), math.sin(math.radians(phi))
            d = np.array([c * math.cos(math.radians(tilt)), math.sin(math.radians(tilt)), s * math.cos(math.radians(tilt))])
            for sgn in (1.0, -1.0):
                h = hold_wrist(pose, d, (0.0, sgn, 0.0))
                q["right_hand"] = (h, Z3)
                Wp = (K.world(q, "right_hand") @ L)[:3].T * 16.0
                low = Wp[:, 1].min() - ground
                cost = (_inside(Wp, boxes) + 3.0 * max(0.0, -low) + 0.6 * abs(low - 0.2) + turn_deg(h, ref) / 45.0
                        + abs(tilt) / 60.0 + (0.5 if sgn < 0 else 0.0)
                        + TOP_W * max(0.0, Wp[:, 1].max() - ground - TOP_OK))
                if best is None or cost < best[0]:
                    best = (cost, h)
    return best[1]


def _axis_mat(axis, deg):
    a = axis / np.linalg.norm(axis)
    x, y, z = a
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    C = 1 - c
    return np.array([[c + x * x * C, x * y * C - z * s, x * z * C + y * s],
                     [y * x * C + z * s, c + y * y * C, y * z * C - x * s],
                     [z * x * C - y * s, z * y * C + x * s, c + z * z * C]])


GROUND_CLEAR = 1.0
MAX_WRIST_TURN = 45.0


def ground_hand(pose, rot, prev):
    """The wrist rotation nearest `rot` (and the last key's, `prev`) keeping the whole harpoon at or above the ground and out of
    the body: it tips about the fist by the smallest angle that clears, so it never stabs the floor."""
    q = dict(pose)
    q["right_hand"] = (rot, Z3)
    P, owner = _CORNERS(q)
    isw = np.array([is_weapon(o) for o in owner])
    ground = body_ground(P, owner, isw)
    if P[isw, 1].min() - ground >= GROUND_CLEAR:
        return rot
    keep = np.array([not any(a in o for a in ("left_arm", "left_forearm", "left_hand")) for o in owner])
    boxes = _boxes(P[keep], [o for o, k in zip(owner, keep) if k])
    Wh = K.world(q, "right_hand")
    L = np.linalg.inv(Wh) @ np.c_[P[isw] / 16.0, np.ones(int(isw.sum()))].T
    pts = (Wh @ L)[:3].T
    q["right_hand"] = (Z3, Z3)
    Rp = K.world(q, "right_hand")[:3, :3]
    Rh = Wh[:3, :3]
    _, _, vt = np.linalg.svd(pts - pts.mean(0))
    axes = []
    tilt = np.cross(vt[0], (0.0, 1.0, 0.0))
    if np.linalg.norm(tilt) > 1e-6:
        axes += [tilt, -tilt]
    axes += [np.array([math.cos(a), 0.0, math.sin(a)]) for a in np.radians(np.arange(0, 360, 30))]
    best = None
    for ax in axes:
        for deg in np.arange(1.0, 151.0, 1.0):
            r = K.decompose_zyx(Rp.T @ _axis_mat(ax, deg) @ Rh)
            q["right_hand"] = (r, Z3)
            Wp = (K.world(q, "right_hand") @ L)[:3].T * 16.0
            low = Wp[:, 1].min() - ground
            if low < GROUND_CLEAR - 0.05:
                continue
            cost = deg / 20.0 + 0.4 * _inside(Wp, boxes) + (turn_deg(r, prev) / 15.0 if prev is not None else 0.0)
            if best is None or cost < best[0]:
                best = (cost, r)
            break
    return rot if best is None else best[1]


def near_euler(r, prev):
    if prev is None:
        return r
    a, b, c = r
    cands = []
    for base in ((a, b, c), (a + 180.0, 180.0 - b, c + 180.0), (a + 180.0, -180.0 - b, c + 180.0),
                 (a - 180.0, 180.0 - b, c - 180.0), (a - 180.0, -180.0 - b, c - 180.0)):
        for da in (-360.0, 0.0, 360.0):
            for db in (-360.0, 0.0, 360.0):
                for dc in (-360.0, 0.0, 360.0):
                    e = (base[0] + da, base[1] + db, base[2] + dc)
                    if np.abs(_rot(e) - _rot(r)).max() < 1e-6:
                        cands.append(e)
    return min(cands, key=lambda e: sum((e[i] - prev[i]) ** 2 for i in range(3)))


def flat_weapon(keys, start, t0, t1, step=None):
    """Re-key the fall every DEATH_STEP with the right wrist solved: free, then easing from t0 to the harpoon lying flat by t1,
    always tipped clear of the ground, swinging at most MAX_WRIST_TURN per tick (planned backwards, then capped forwards)."""
    step = DEATH_STEP if step is None else step
    end = keys[-1][0]
    out, ref, prev = [], None, start["right_hand"][0]
    for t in np.arange(step, end + step / 2, step):
        t = float(t)
        f = K.pose_at(keys, start, t)
        pose = {b: (r, Z3) for b, r in f.items()}
        pose["hips"] = (f["hips"], start["hips"][1])
        hand = f["right_hand"]
        if t >= t0:
            ref = flat_hand(pose, ref or prev)
            hand = slerp_rot(hand, ref, ramp(t, t0 - 1, t1))
        r = ground_hand(pose, hand, prev)
        out.append((t, f, pose, r))
        prev = r
    cap = MAX_WRIST_TURN * step
    rs = [o[3] for o in out]
    for i in range(len(rs) - 2, -1, -1):
        if out[i][0] < 3.0:
            break
        turn = turn_deg(rs[i], rs[i + 1])
        if turn > cap:
            rs[i] = ground_hand(out[i][2], slerp_rot(rs[i + 1], rs[i], cap / turn), rs[i + 1])
    res, prev = [], start["right_hand"][0]
    for (t, f, pose, _), r in zip(out, rs):
        turn = turn_deg(prev, r)
        if turn > cap:
            r = ground_hand(pose, slerp_rot(prev, r, cap / turn), prev)
        prev = near_euler(r, prev)
        f["right_hand"] = prev
        res.append((t, "lin", f))
    return res


# The knight's falls start from a sword guard with the arm low; the harpooner's arm starts cocked above the shoulder, so the
# knight's first arm key (3 ticks in, eased out) flung it down 170 deg at once. The first key takes the throwing arm only part
# of the way (ARM_IN of it, the next key finishes the drop), and the empty left hand never turns over as the knight's shield hand
# does to lay the shield flat: it stays open.
ARM_IN = 0.5


def harpoon_fall(keys, start):
    out, first = [], True
    for t, ease, k in keys:
        k = dict(k)
        if "left_hand" in k:
            k["left_hand"] = Z3
        if first:
            for b in ("right_arm", "right_forearm"):
                if b in k:
                    k[b] = tuple(a + ARM_IN * (v - a) for a, v in zip(start[b][0], k[b]))
            first = False
        out.append((t, ease, k))
    return out


def death_clips():
    start = K.idle_start()
    falls = {"death_back": K.DEATH_BACK, "death_forward": K.DEATH_FORWARD,
             "death_left": K.roll_keys(K.DEATH_LEFT, start, 13, K.ROLL_LEFT),
             "death_right": K.roll_keys(K.DEATH_RIGHT, start, 13, K.ROLL_RIGHT)}
    return {n: K.death_clip(flat_weapon(harpoon_fall(k, start), start, *FLAT_AT[n]), start, n) for n, k in falls.items()}


# ------------------------------------------------------------------------------------------------

def only_rig(anims):
    for clip in anims.values():
        clip["bones"] = {b: ch for b, ch in clip["bones"].items() if b in GEO_BONES}
    return anims


def dump(path, ticks):
    """Key-pose check: the throw's whole body at the given ticks (or the guard, tick -1), as still.py pose rows."""
    upper, legs = attack_clip()
    rows = []
    for t in ticks:
        i = int(round(t * SUB))
        p = fk_pose(upper[i], legs[i])
        rows.append({b: {"rotation": list(map(float, v[0])), "position": list(map(float, v[1]))} for b, v in p.items()})
        rows[-1]["draw"] = {"rotation": [0, 0, 0], "position": [float(upper[i]["draw"][1][0]), 0, 0]}
        hits = shaft_hits(p) if draw_at(t) == 0.0 else []
        foot = wpoint(p, S(BUTT))
        print("t%-5s shaft-in-body %-3d foot y %.1f" % (t, len(hits), foot[1]))
    Path(path).write_text(json.dumps(rows))
    print("dumped", len(rows), "poses to", path)


def main():
    if os.environ.get("THROW_DUMP"):
        dump(os.environ["THROW_DUMP"], [float(x) for x in os.environ.get("THROW_TICKS", "0,3,5,7.4,8,10,13,16.5,18.5,21,25,29").split(",")])
        return
    idle_legs, idle_upper = idle_clips(IDLE_LIFE_V2 if V2 else None)   # the death falls keep the old guard (K.idle_clips)
    anims = {"walk_legs": K.gait_legs(K.WALK), "walk_upper": gait_upper(K.WALK, K.WALK_CARRY, False),
             "run_legs": K.gait_legs(K.RUN), "run_upper": gait_upper(K.RUN, K.RUN_CARRY, True),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **stance_clips(), **(v2_hit_clips() if V2 else hit_clips()),
             **death_clips()}
    only_rig(anims)
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, "clips:", ", ".join(anims))


if __name__ == "__main__":
    main()
