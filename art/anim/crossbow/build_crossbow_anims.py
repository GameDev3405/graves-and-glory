"""Crossbow clips (unit animation rebuild, docs/anim-rebuild/STYLE.md): the crossbow family, first the Iron Crown arbalist.

GG_XBOW_TROOP=ic_arbalist python art/anim/crossbow/build_crossbow_anims.py  ->  art/anim/ic_arbalist/out/ic_arbalist.animation.json

Reuses the knight builder's rig maths, gait legs, stance legs, hit footwork and death falls (art/anim/knight/build_anims.py with
GG_ANIM_TROOP set: no shield) and keys its own upper body around the crossbow in the right fist.

The crossbow is solved, not hand-keyed: every key names where the grip sits (O, troop px), which way the stock points (d, butt to
prod) and which way its top faces (u), and where the left fist goes (a point on the crossbow, or on the body for the quiver). Each
sample the right arm reaches the grip, the wrist turns the crossbow onto (d, u), and the left arm reaches its point.

The draw channel: the crossbow is three sibling bones (weapon_t<n>_rest / pull_0 / pull_1, rig tag "draw"); the empty "draw"
bone's position x picks one (0 rest: spanned with a bolt on the rail, 1 pull_0: the string forward, spent, 2 pull_1: spanned, no
bolt). GeoTroopRenderer reads it after the controllers.

The shot (attack_loose, impactTicks 4): the crossbow snaps from the low guard to the shoulder and looses on the server's impact
tick; the kick throws it up off the cheek. Then the reload: the stock swings across the body, prod high at the left, the left fist
takes the string and hauls it down into the nut with the hips sunk and the back twisted into it, reaches over the left shoulder
to the quiver, lays the bolt on the rail, and both hands bring the crossbow back down to the guard.
"""
import json, math, os, sys
from pathlib import Path

TROOP = os.environ.get("GG_XBOW_TROOP", "ic_arbalist")
os.environ["GG_ANIM_TROOP"] = TROOP
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "knight"))
import build_anims as K  # noqa: E402

SUB, TPS, Z3 = K.SUB, K.TPS, K.Z3
np = K.np
K.HELD = K.HELD + ("weapon",)
K.DEATH_BONES = tuple(b for b in K.DEATH_BONES if b != "cape")
GEO_BONES = {b["name"] for b in json.loads((HERE.parent / TROOP / "out" / f"{TROOP}.geo.json").read_text())["minecraft:geometry"][0]["bones"]}
DRAW = {"rest": 0.0, "pull_0": 1.0, "pull_1": 2.0}
XBOW_TROOPS = {"ic_arbalist": {"gear": {"hood_cape": "halfcape", "tippet": None, "quiver": "quiver"}}}
XT = XBOW_TROOPS[TROOP]
W = "weapon_t1"                 # every tier's crossbow bones share the pivot and axes

# ------------------------------------------------------------------------------------------------
# The crossbow frame. Weapon-local px: butt y7, grip y14 (the fist), rail y20-27 on top (-z), prod y27-29, limbs x -14..2,
# string spent y28 / spanned y26. d = weapon +y (butt to prod), u = weapon -z (the stock's top).
# ------------------------------------------------------------------------------------------------

_HAND_TO_W = K.world({}, "right_hand")[:3, :3].T @ K.world({}, W)[:3, :3]
GRIP = (-6.0, 14.0, 0.0)            # right_hand-local: the hand pivot, where the fist holds the stock
FIST_L = (6.0, 12.5, 0.0)           # left_forearm-local: the centre of the left fist
FORE_STOCK = (-6.0, 20.0, 1.8)      # weapon-local: under the fore-stock, the left fist's guard hold
STRING_SIDE = -9.0                  # weapon-local x: the left fist takes the string beside the rail (the troop's left side)
STRING_SPENT = (STRING_SIDE, 28.0, 1.2)
STRING_SPAN = (STRING_SIDE, 26.0, 1.2)
RAIL = (-6.0, 23.0, -2.6)           # on the rail's top: the fist lays the bolt
QUIVER = (2.5, 24.5, 3.8)           # quiver-local: the bolt heads above the quiver's mouth, behind the left shoulder


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def frame(d, u):
    """d, u made square: the stock's line and its top."""
    d = unit(d)
    u = np.asarray(u, float) - (np.asarray(u, float) @ d) * d
    return d, unit(u)


def hold_wrist(pose, d, u):
    """right_hand rotation turning the crossbow onto (d, u)."""
    d, u = frame(d, u)
    Mw = np.column_stack([np.cross(d, -u), d, -u])
    return K.decompose_zyx(K.world(pose, "right_forearm")[:3, :3].T @ (Mw @ _HAND_TO_W.T))


def gn(f, x0, iters=60, lam=0.02, clamp=None):
    """Damped Gauss-Newton on the residual vector f(x)."""
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


def reach(side, pose, target, prior, warm=None, w=0.02):
    """The arm (x, y, z) and forearm x putting the fist on target, nearest the prior arm."""
    prior = np.asarray(prior, float)
    tip = (lambda p: K.point(K.world(p, "right_hand"), GRIP)) if side == "right" else \
        (lambda p: K.point(K.world(p, "left_forearm"), FIST_L))

    def f(x):
        return np.concatenate([4.0 * (tip(_arm(side, pose, x)) - target), w * (x - prior)])

    x = gn(f, prior if warm is None else warm, clamp=lambda x: np.r_[x[:3], min(-2.0, max(-150.0, x[3]))])
    return x, np.linalg.norm(f(x)[:3]) / 4.0


def wpoint(pose, a):
    return K.point(K.world(pose, W), a)


# ------------------------------------------------------------------------------------------------
# Keys. Each: torso, head (deg), O grip (troop px), d, u, Lw (weapon-local left target), Lq (quiver-local), m (0 crossbow, 1
# quiver), and the arm priors (right 4, left 4) the solve starts from.
# ------------------------------------------------------------------------------------------------

def aim(elev, yaw):
    e, y = math.radians(elev), math.radians(yaw)
    d = np.array([math.sin(y) * math.cos(e), math.sin(e), math.cos(y) * math.cos(e)])
    return frame(d, (0.0, 1.0, 0.0))


def KEY(torso, head, O, d, u=(0, 1, 0), Lw=FORE_STOCK, Lq=QUIVER, m=0.0, rp=(-40, 0, 10, -40), lp=(-50, -20, 0, -40), lm=1.0, pw=0.02):
    """lm: how far the left fist goes to its point on the crossbow (1 holding it, 0 free at its prior lp). pw: how hard the
    two-handed solve holds the arms to their priors rp / lp (the elbows' swivel), 0.02 a light pull."""
    d, u = frame(d, u)
    return {"torso": tuple(map(float, torso)), "head": tuple(map(float, head)), "O": tuple(map(float, O)), "d": tuple(d),
            "u": tuple(u), "Lw": tuple(map(float, Lw)), "Lq": tuple(map(float, Lq)), "m": (float(m),),
            "rp": tuple(map(float, rp)), "lp": tuple(map(float, lp)), "lm": (float(lm),), "pw": (float(pw),)}


# The shouldered aim. The butt goes on the OUTER shoulder, over the joint (torso-local x -7.3, outside the chest's side at
# x -4), not into the chest: with this rig's short arms a butt in front of the chest leaves the grip out of the right arm's
# reach, and the solve then drags the stock (and the t3 butt crossbar) 2 px into the chest. The torso turns well side-on
# (yaw 51, the left shoulder leading), the stock levelled straight down the line, the left fist under the stock just ahead of
# the lock, the head turned down the stock with the cheek dropped to it. Searched with the scorecard's own pen_weapon /
# pen_limb (scratch shoulder.py): pen_weapon 1.0 px, pen_limb 1.27 px, grip error 0.24 px.
SHOULDER = {"torso": (5.7, 51.2, 0.0), "butt": (-7.27, 22.57, -3.73), "yaw": 4.5, "elev": 0.0, "left_y": 16.67}


def shoulder_key(spec=SHOULDER, head=None):
    torso = spec["torso"]
    T = K.world({"hips": K.READY_HIPS, "torso": (torso, Z3)}, "torso")
    d, u = aim(spec["elev"], spec["yaw"])
    O = K.point(T, spec["butt"]) + 7.0 * d        # the grip is 7 px up the stock from the butt (weapon y7 -> y14)
    head = head or (6.0, -torso[1] - K.READY_HIPS[0][1] + spec["yaw"], -8.0)
    return KEY(torso, head, O, d, u, Lw=(FORE_STOCK[0], spec["left_y"], FORE_STOCK[2]),
               rp=(-50.65, -56.63, -41.18, -2.0), lp=(-55.37, -1.89, 7.85, -39.67))


AIM = shoulder_key()


def nudge(k, **kw):
    q = dict(k)
    for n, v in kw.items():
        if n in ("O",):
            q[n] = tuple(a + b for a, b in zip(k[n], v))
        elif n in ("torso", "head"):
            q[n] = tuple(a + b for a, b in zip(k[n], v))
        elif n in ("lm", "pw"):
            q[n] = (float(v),)
        else:
            q[n] = v
    return q


def pitched(k, deg):
    """The same key with the stock pitched up by deg about the troop's side axis through the grip."""
    d, u = np.array(k["d"]), np.array(k["u"])
    ax = unit(np.cross(d, (0, 1, 0)))
    a = math.radians(-deg)
    rot = lambda v: v * math.cos(a) + np.cross(ax, v) * math.sin(a) + ax * (ax @ v) * (1 - math.cos(a))
    d2, u2 = frame(rot(d), rot(u))
    q = dict(k)
    q["d"], q["u"] = tuple(d2), tuple(u2)
    return q


def _axis_rot(ax, deg):
    ax, a = unit(ax), math.radians(deg)
    return lambda v: v * math.cos(a) + np.cross(ax, v) * math.sin(a) + ax * (ax @ v) * (1 - math.cos(a))


def about_butt(k, down, inward=0.0, torso=Z3, head=Z3, shift=Z3, **kw):
    """The key's crossbow swung about its butt (weapon y7, 7 px behind the grip): down tips the prod toward the ground, inward
    swings it across the body toward the troop's left. The butt stays in the shoulder's pocket, so the grip stays in reach."""
    d, u, O = np.array(k["d"]), np.array(k["u"]), np.array(k["O"])
    B = O - 7.0 * d
    r1 = _axis_rot(np.cross(d, (0, 1, 0)), -down)
    r2 = _axis_rot((0, 1, 0), inward)
    rot = lambda v: r2(r1(v))
    d2, u2 = frame(rot(d), rot(u))
    q = nudge(k, torso=torso, head=head, **kw)
    q["O"] = tuple(B + 7.0 * d2 + np.asarray(shift, float))
    q["d"], q["u"] = tuple(d2), tuple(u2)
    return q


# The low guard: the shouldered aim with the prod tipped 30 degrees down and swung a touch across the body about the butt,
# which stays near the shoulder (a low ready), so the snap to the cheek is short and the left fist never leaves the fore-stock.
# (The numbers of READY, brace and wall were searched with the scorecard's pen_weapon / pen_limb, scratch abopt.py.)
READY = about_butt(AIM, 12.0, 6.5, torso=(0.2, 5.1, 0.0), head=(6.4, 10.0, 6.0), shift=(-1.41, 1.5, -1.06),
                   Lw=(FORE_STOCK[0], 18.8, FORE_STOCK[2]))
# The span: the stock swung across the body, butt at the right hip, prod up at the left front; the left fist on the spent string.
SPAN = KEY((4, -12, 0), (10, 22, 0), (-4.0, 12.0, 4.0), (10.0, 6.0, 7.0), u=(0, 0.3, -1), Lw=STRING_SPENT,
           rp=(-20, 0, -10, -30), lp=(-73, 34, 5, -24))
# The haul, half way: the hips sink, the back twists down into the pull, the right fist shoves the stock up and away.
HAUL_A = KEY((7, -18, -3), (12, 26, 0), (-4.5, 12.6, 5.0), (9.0, 8.0, 7.0), u=(0, 0.3, -1), Lw=(STRING_SIDE, 27.0, 1.2),
             rp=(-24, 0, -12, -26), lp=(-72, 34, 5, -26))
# The string drops into the nut: the hardest moment of the pull, the torso folded over the crossbow.
HAUL_B = KEY((11, -22, -6), (14, 28, 2), (-5.0, 13.4, 6.0), (8.0, 10.0, 6.0), u=(0, 0.3, -1), Lw=STRING_SPAN,
             rp=(-28, 0, -14, -22), lp=(-70, 34, 5, -30))
# The bolt: the crossbow tipped prod-down in the right fist alone, the left fist reaching back over the left shoulder, the head
# turned to it.
QUIVER_K = about_butt(AIM, 40.0, 0.0, torso=(-2.0, 0.0, 2.0), head=(4.0, 46.0, 16.0), Lq=QUIVER, m=(1.0,),
                      lp=(-165, -10, -10, -120))
# Laying the bolt on the rail: the low guard drawn in a little, eyes down on the rail, the left fist on the rail's top.
LOAD = about_butt(AIM, 30.0, 20.0, torso=(6.0, -6.0, 0.0), head=(20.0, 16.0, 10.0), Lw=RAIL)
LOAD_IN = nudge(LOAD, Lw=(RAIL[0], RAIL[1] + 2.5, RAIL[2]), O=(0.0, 0.0, 0.4))

ATTACK_TICKS = 50
IMPACT = 4          # crossbow_loose impactTicks: the bolt leaves


def blend(a, b, k):
    """A key part way from a to b, every channel linear (the stock's line and top squared again)."""
    q = {n: tuple(np.asarray(a[n], float) + (np.asarray(b[n], float) - np.asarray(a[n], float)) * k) for n in a}
    q["d"], q["u"] = map(tuple, frame(q["d"], q["u"]))
    return q


def attack_keys():
    """The shot and the reload. The span, haul and load keys carry the stock a little out from the hips and the chest, and the
    torso folds a touch further over it, so the right forearm passes outside the body (pen_limb, scratch atkopt.py)."""
    tense = nudge(AIM, torso=(0.5, 1.0, 0.0), head=(1.0, 0.0, 0.0))
    kick = nudge(pitched(AIM, 9.0), O=(-0.4, 1.6, -1.6), torso=(-5.0, 2.0, 0.0), head=(-4.0, 2.0, 0.0))
    kick2 = nudge(pitched(AIM, 5.0), O=(-0.2, 0.5, -0.6), torso=(-2.0, 1.0, 0.0), head=(-1.0, 1.0, 0.0))
    lower = about_butt(AIM, 20.0, 10.0, torso=(-1.2, -1.3, 0.0), head=(6.0, 14.0, 6.0), shift=(-1.37, 3.27, 0.1),
                       Lw=(FORE_STOCK[0], AIM["Lw"][1] + 1.8, FORE_STOCK[2]))
    span = nudge(SPAN, O=(-1.5, 0.75, -2.6), torso=(1.9, 3.0, 0.0))
    haul_a = nudge(HAUL_A, O=(-0.44, -0.74, -2.0), torso=(4.0, 2.0, 0.0))
    haul_b = nudge(HAUL_B, O=(-0.3, -0.5, -1.5), torso=(3.0, 1.5, 0.0))
    # the fist takes the bolt by its head, high over the quiver's mouth, so the elbow does not fold past its limit
    reach_q = nudge(QUIVER_K, Lq=(QUIVER[0], QUIVER[1] + 3.0, QUIVER[2] + 0.5))
    take_q = nudge(QUIVER_K, Lq=(QUIVER[0], QUIVER[1] + 2.0, QUIVER[2]))
    draw_q = nudge(QUIVER_K, torso=(1.0, 4.0, 0.0), Lq=(QUIVER[0] + 0.5, QUIVER[1] + 5.0, QUIVER[2] - 1.0))
    # laying the bolt: the right elbow held out to the side (its prior pulled harder) so the forearm stays off the chest
    out_r = dict(rp=(-5.0, -15.0, 8.0, -105.0), pw=0.25)
    load = nudge(LOAD, O=(-0.96, 0.0, -1.28), torso=(-0.66, -4.84, 0.0), Lw=(RAIL[0], RAIL[1] + 1.7, RAIL[2]), **out_r)
    load_in = nudge(LOAD_IN, O=(-0.45, -1.1, -2.85), **out_r)
    home = nudge(READY, O=(-2.0, 1.8, -1.5), torso=(-1.0, 1.0, 0.0))      # the stock swung back out round the hip to the guard
    settle = nudge(READY, O=(0.0, 0.4, 0.6), torso=(1.0, 2.0, 0.0))
    # the swings between the side-on shoulder and the square-on haul: the grip is carried out round the front on the way, so the
    # right arm stays reaching instead of folding in through the chest
    swing_in = nudge(blend(lower, span, 0.5), O=(-1.5, 0.0, 2.0), lm=0.0, lp=(-60, -10, 0, -50))
    swing_in["d"], swing_in["u"] = blend(lower, span, 0.6)["d"], blend(lower, span, 0.6)["u"]   # the stock turns early
    swing_out = nudge(blend(nudge(haul_b, torso=(-1, 1, 1)), reach_q, 0.5), O=(-1.5, 0.0, 2.0))
    return [(0, READY), (2.6, AIM), (IMPACT, tense),                                  # snap to the shoulder, loose
            (5.2, kick), (7.0, kick2), (9.6, lower), (11.8, swing_in),               # the kick off the cheek, the stock lowered
            (14.0, span), (16.0, nudge(span, Lw=(STRING_SIDE, 28.0, 0.6))),           # swung across, the fist hooks the string
            (19.0, haul_a), (21.0, haul_b), (22.5, nudge(haul_b, torso=(-1, 1, 1))),  # the haul; the string drops into the nut
            (24.25, swing_out), (26.0, reach_q), (28.0, take_q), (30.0, draw_q),                        # over the shoulder, a bolt drawn up
            (31.8, nudge(blend(draw_q, load, 0.5), rp=(-25.0, -20.0, 0.0, -80.0), pw=0.15)),
            (33.5, load), (35.0, load_in), (36.0, nudge(blend(load_in, home, 0.5), rp=(-35.0, -25.0, 0.0, -65.0), pw=0.15)),
            (37.0, home),                              # laid on the rail, pressed home
            (40.0, settle), (44.0, nudge(READY, O=(0.0, -0.3, -0.2))), (ATTACK_TICKS, READY)]


# The draw frame: loaded until the loose; spent through the span; spanned once the string drops into the nut; loaded again
# when the bolt is pressed home.
DRAW_STEPS = [(0, "rest"), (IMPACT, "pull_0"), (21.0, "pull_1"), (34.6, "rest")]


def draw_at(t):
    v = 0.0
    for t0, f in DRAW_STEPS:
        if t >= t0:
            v = DRAW[f]
    return v


def attack_legs():
    """Footwork (STYLE.md rule 4): a short settle into the shot, the rock of the kick, then for the haul the left foot steps out
    wide to brace, the hips sink and twist into the pull, and the foot comes back as the guard returns."""
    R0, R1 = K.READY_HIPS
    spec = {"hips": [(0, R0, R1), (2.6, (0.0, 9.0, 0.0), (0.0, -1.3, 0.1)), (IMPACT, (0.0, 9.0, 0.0), (0.0, -1.35, 0.1)),
                     (5.4, (-2.5, 10.0, 0.0), (0.0, -1.1, -0.2)), (8.0, (0.0, 8.0, 0.0), (0.0, -1.2, 0.0)),
                     (14.0, (2.0, -6.0, 0.0), (0.0, -1.6, -0.2)), (19.0, (4.0, -9.0, -1.0), (0.0, -2.3, -0.4)),
                     (21.0, (5.0, -11.0, -1.5), (0.0, -2.6, -0.5)), (23.0, (3.0, -9.0, -1.0), (0.0, -2.2, -0.4)),
                     (28.0, (1.0, 2.0, 0.0), (0.0, -1.4, 0.0)), (34.0, (2.0, 4.0, 0.0), (0.0, -1.3, 0.1)),
                     (42.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # the lead (left) foot steps out wide for the haul (it already leads, so the step is mostly sideways), then home
            "steps": [("left", 10.5, 14.0, (5.0, 2.0, 2.4), 18.0)] + rehome([("left", 37.0, 41.0, (3.2, 2.0, -2.0), 7.0)])}
    return K.attack_legs(spec, ticks=ATTACK_TICKS)


def fk_pose(s, legs):
    pose = {b: (v[0], v[1] or Z3) for b, v in s.items() if b != "draw"}
    pose.update({b: (v[0], v[1] or Z3) for b, v in legs.items()})
    return pose


def interp(keys, t, name):
    return K.curve([(k[0], k[1][name]) for k in keys], t)


class Solver:
    """Solves sample after sample along one clip, each arm warm-started from the last sample so the elbows never flip."""

    def __init__(self):
        self.warm = {"right": None, "left": None}
        self.worst = {"right": 0.0, "left": 0.0}

    def pose(self, legs, torso, head, O, d, u, left, rp, lp, lm=1.0, extra=None, quiver=None, m=0.0, pw=0.02):
        """left: a world point, or None for a free left arm keyed by lp. lm (0-1): how far the left fist goes to it.
        quiver (with m, 0-1): the second contact, the bolt in the quiver. Between the two the arms blend joint angle by joint
        angle (the fist arcs up over the shoulder) rather than the fist's target sliding through the body."""
        s = {"torso": (tuple(torso), None), "head": (tuple(head), None)}
        if extra:
            s.update(extra)
        pose = fk_pose(s, legs)
        lp = np.asarray(lp, float)
        if left is not None and lm >= 1.0:
            # both hands on the crossbow: the left fist's contact is held hard, the grip point is a soft target, so a
            # crossbow out of the left arm's reach slides back toward the body instead of the fist floating free of it
            x, y, eo, el = self.joint(pose, np.asarray(O), d, u, left, np.asarray(rp, float), lp, pw=pw)
            if m > 0.0:
                xs, e = reach("right", pose, np.asarray(O), rp, self.warm.get("rs"))
                self.warm["rs"] = xs
                pr = _arm("right", pose, xs)
                qp = np.asarray(QUIVER_K["lp"], float)
                yq, eq = reach("left", pr, quiver(pr), qp, self.warm.get("q"))
                self.warm["q"] = yq
                x, y = x + (xs - x) * m, y + (yq - y) * m
                eo = eo if m < 1.0 else e
                el = el if m <= 0.0 else (eq if m >= 1.0 else 0.0)
            else:
                self.warm["rs"] = self.warm["q"] = None
            self.worst["right"] = max(self.worst["right"], eo)
            self.worst["left"] = max(self.worst["left"], el)
            for side, v in (("right", x), ("left", y)):
                s[side + "_arm"], s[side + "_forearm"] = (tuple(v[:3]), None), ((v[3], 0.0, 0.0), None)
            pose = fk_pose(s, legs)
            s["right_hand"] = (hold_wrist(pose, d, u), None)
            s["left_hand"] = (Z3, None)
            return s
        x, e = reach("right", pose, np.asarray(O), rp, self.warm["right"])
        self.warm["right"] = x
        self.worst["right"] = max(self.worst["right"], e)
        s["right_arm"], s["right_forearm"] = (tuple(x[:3]), None), ((x[3], 0.0, 0.0), None)
        pose = fk_pose(s, legs)
        s["right_hand"] = (hold_wrist(pose, d, u), None)
        pose["right_hand"] = (s["right_hand"][0], Z3)
        if left is not None and lm > 0:
            y, e = reach("left", pose, left(pose), lp, self.warm["left"])
            self.warm["left"] = y
            if lm >= 1.0:
                self.worst["left"] = max(self.worst["left"], e)
            y = lp + (y - lp) * lm
        else:
            y = lp
            self.warm["left"] = None
        s["left_arm"], s["left_forearm"] = (tuple(y[:3]), None), ((y[3], 0.0, 0.0), None)
        s["left_hand"] = (Z3, None)
        return s

    def joint(self, pose, O, d, u, left, rp, lp, wo=1.0, pw=0.02):
        prior = np.r_[rp, lp]
        warm = prior if self.warm["right"] is None or self.warm["left"] is None else np.r_[self.warm["right"], self.warm["left"]]

        def build(z):
            p = _arm("left", _arm("right", pose, z[:4]), z[4:])
            p["right_hand"] = (hold_wrist(p, d, u), Z3)
            return p

        def f(z):
            p = build(z)
            g = K.point(K.world(p, "right_hand"), GRIP)
            fl = K.point(K.world(p, "left_forearm"), FIST_L)
            return np.concatenate([wo * (g - O), 4.0 * (fl - left(p)), pw * (z - prior)])

        clamp = lambda z: np.r_[z[:3], min(-2.0, max(-150.0, z[3])), z[4:7], min(-2.0, max(-150.0, z[7]))]
        z = gn(f, warm, clamp=clamp)
        r = f(z)
        self.warm["right"], self.warm["left"] = z[:4], z[4:]
        return z[:4], z[4:], np.linalg.norm(r[:3]) / wo, np.linalg.norm(r[3:6]) / 4.0


def left_target(Lw, Lq=None, m=0.0):
    return lambda pose: wpoint(pose, Lw)


def quiver_target(Lq):
    return lambda pose: K.point(K.world(pose, "quiver"), Lq)


def solve_keys(keys, legs_at, t):
    """One sample of a keyed clip."""
    v = {n: interp(keys, t, n) for n in ("torso", "head", "O", "d", "u", "Lw", "Lq", "m", "rp", "lp", "lm", "pw")}
    v["mq"] = max(0.0, min(1.0, v["m"][0]))
    return v, left_target(v["Lw"])


def attack_clip():
    keys = attack_keys()
    legs = attack_legs()
    sol = Solver()
    out = []
    for i in range(ATTACK_TICKS * SUB + 1):
        t = i / SUB
        v, left = solve_keys(keys, legs, t)
        s = sol.pose(legs[i], v["torso"], v["head"], v["O"], v["d"], v["u"], left, v["rp"][:4], v["lp"][:4],
                     lm=min(1.0, max(0.0, v["lm"][0])), quiver=quiver_target(v["Lq"]), m=v["mq"], pw=v["pw"][0])
        s["draw"] = (Z3, (draw_at(t), 0.0, 0.0))
        out.append(s)
    print("attack_loose: grip err max %.3f px, left fist err max %.3f px" % (sol.worst["right"], sol.worst["left"]))
    return out, legs


def gear(samples, legs=None):
    """Secondary motion (STYLE.md rule 9): the half cape trails the torso (the knight's cape lag, scaled to the short cloth) and
    the quiver rocks on its strap."""
    K.cape_lag(samples, legs)
    for s in samples:
        c = s.pop("cape")[0]
        s["hood_cape"] = ((0.6 * c[0], 0.0, 0.6 * c[2]), None)
        s.setdefault("quiver", (Z3, None))
    return samples


def attack_clips():
    upper, legs = attack_clip()
    # the quiver jolts at the kick and as the bolt is pulled from it
    for i, s in enumerate(upper):
        t = i / SUB
        j = 3.0 * math.exp(-max(0.0, t - IMPACT) / 1.5) * (t >= IMPACT) + 4.0 * math.sin(math.pi * K.np.clip((t - 27.5) / 4.0, 0, 1))
        s["quiver"] = ((j, 0.0, 0.4 * j), None)
    upper = gear(upper, legs)
    clip = K.bake(upper, ATTACK_TICKS, loop=False)
    clip["timeline"] = {K.fmt_t(IMPACT / TPS): "impact"}
    return {"attack_loose": clip, "attack_loose_legs": K.bake(legs, ATTACK_TICKS, loop=False)}


# ------------------------------------------------------------------------------------------------
# Carry: the crossbow hangs by its grip in the right fist at the thigh, stock level and pointing ahead, the left arm free.
# ------------------------------------------------------------------------------------------------

CARRY_R = (-8.0, 0.0, 4.0)
CARRY_RF = -38.0


def carry_hand(pose, pitch, yaw=0.0):
    """The crossbow's line in the world: ahead, pitched (deg, + nose up), turned a little; its top up."""
    e, y = math.radians(pitch), math.radians(yaw)
    d = np.array([math.sin(y) * math.cos(e), math.sin(e), math.cos(y) * math.cos(e)])
    return hold_wrist(pose, d, (0.0, 1.0, 0.0))


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
    # the free left arm swings counter to the right leg; the crossbow arm swings less (its weight), the elbow carrying it ahead
    la = (c.arm_base - c.arm * sw, 0.0, -c.arm_out)
    lf = (c.fore - 0.35 * c.arm * max(0.0, sw), 0.0, 0.0)
    if run:
        ra = (-22.0 + 0.55 * c.arm * sw, 0.0, 8.0)
        rf = (-55.0 + 0.3 * c.arm * min(0.0, -sw), 0.0, 0.0)
    else:
        ra = (CARRY_R[0] + 0.5 * c.arm * sw, 0.0, CARRY_R[2])
        rf = (CARRY_RF - 0.3 * c.arm * max(0.0, -sw), 0.0, 0.0)
    s = {"torso": ((tx, ty, tz), None), "head": (head, None), "right_arm": (ra, None), "right_forearm": (rf, None),
         "left_arm": (la, None), "left_forearm": (lf, None), "left_hand": (Z3, None)}
    pose = fk_pose(s, legs)
    # the stock rides level, its nose bobbing a beat behind the arm (rule 9: the weight lags)
    pitch = (-6.0 if run else -10.0) + 4.0 * math.cos(tw * (u - c.arm_lag - 0.12))
    s["right_hand"] = (carry_hand(pose, pitch, -0.3 * (hy + ty)), None)
    flare, flap = c.cape
    cape = (flare + flap * math.sin(tw * 2 * (u - g.bob_phase - 0.12)), -0.5 * ty, -0.6 * tz)
    s["hood_cape"] = ((0.6 * cape[0], 0.0, 0.6 * cape[2]), None)
    s["quiver"] = ((1.5 * (2.0 if run else 1.0) * bob, 0.0, 2.0 * (2.0 if run else 1.0) * math.sin(tw * (u - 0.15))), None)
    return s


class GaitArm:
    """The free arm's numbers: the knight's carry, its arm base and swing reused for the left arm."""


def gait_upper(g, c, run):
    legs = K.gait_legs(g)
    n = g.ticks * SUB
    lsamp = _legs_samples(g, n)
    carry = type("C", (), {k: getattr(c, k) for k in dir(c) if not k.startswith("_")})
    carry.arm_base = -4.0 if not run else -16.0
    carry.arm_out = 6.0 if not run else 10.0
    carry.fore = -18.0 if not run else -50.0
    samples = [gait_pose(g, carry, i / n, run, lsamp[i]) for i in range(n)]
    samples.append(samples[0])
    del legs
    return K.bake(samples, g.ticks)


def _legs_samples(g, n):
    """The gait's hips per sample, for the crossbow's world line (legs below the hips do not move the arms)."""
    out = []
    for i in range(n):
        out.append({"hips": (g.hips(i / n)[0], g.hips(i / n)[1])})
    return out


# ------------------------------------------------------------------------------------------------
# Idle: the knight's at-ease legs; the crossbow hangs at the thigh, its nose dipping with the breath.
# ------------------------------------------------------------------------------------------------

_KNIGHT_IDLE = K.idle_clips


def idle_clips():
    legs, _ = _KNIGHT_IDLE()
    n = K.IDLE_TICKS * SUB
    upper = []
    for i in range(n + 1):
        u = (i % n) / n
        br = math.sin(2 * math.pi * u)
        sh = math.sin(2 * math.pi * u + 0.6)
        lag = math.sin(2 * math.pi * u - 0.5)
        s = {"torso": ((1.0 + 0.7 * br, -0.6 * sh, 0.5 * sh), None),
             "head": ((-1.0 - 0.4 * br, 0.75 * math.sin(4 * math.pi * u - 1.0), -0.3 * sh), None),
             "right_arm": ((CARRY_R[0] - 0.6 * br, 0.0, CARRY_R[2] + 0.5 * br), None),
             "right_forearm": ((CARRY_RF - 0.8 * lag, 0.0, 0.0), None),
             "left_arm": ((-3.0 - 0.5 * br + 0.4 * lag, 0.0, -5.0 - 0.4 * br), None),
             "left_forearm": ((-16.0 - 1.0 * lag, 0.0, 0.0), None), "left_hand": (Z3, None),
             "hood_cape": ((0.6 + 0.4 * br, 0.0, 0.0), None), "quiver": ((0.5 * lag, 0.0, 0.0), None)}
        pose = fk_pose(s, {"hips": (Z3, Z3)})
        s["right_hand"] = (carry_hand(pose, -10.0 + 1.5 * lag), None)
        upper.append(s)
    return legs, K.bake(upper, K.IDLE_TICKS)


K.idle_clips = idle_clips    # the death falls start from the arbalist's idle frame (kept as is: deaths are not part of v2)

# V2 feel (Kort, 2026-11-07: "the whole feel and flow of [the Knight's] movements, its weight shifts, and style"). V2 is on by
# default with the knight builder's flag (GG_V2=0 builds the old clips). The shot and the haul are untouched.
V2 = K.V2


IDLE_OUT = 2.5     # at ease the crossbow arm hangs a little wider than in the walk, so the stock clears the hip as the shoulders roll


def idle_v2_clips():
    """At ease with a real weight cycle (STYLE.md rule 2): the hips slide over one foot and back (contrapposto: the free hip
    drops, its knee softens), the chest counter-tilts so the head stays level, and both arms trail the torso a beat late, the
    crossbow swinging a little at the thigh. One breath and one full shift (right, centre, left, centre) per 3 s loop."""
    n = K.IDLE_TICKS * SUB
    SWAY = K.feel.sway()      # Kort's variant B: the weight shift and the arm trail at V2_SWAY_SCALE (breath is not sway)
    legs, upper, guesses = [], [], {}
    for i in range(n + 1):
        u = (i % n) / n
        br = math.sin(2 * math.pi * u)
        sh = SWAY * math.sin(2 * math.pi * u + 0.6)       # + weight over the right foot
        lag = SWAY * math.sin(2 * math.pi * u + 0.6 - 0.9)   # the arms follow the shift a few ticks late
        hips = ((0.5 * br, 4.5 * sh, 5.0 * sh), (-2.0 * sh, -0.8 - 0.14 * br - 0.3 * abs(sh), 0.0))   # +z roll lifts the right hip
        pose = {"hips": hips}
        out = {"hips": hips}
        for side, foot in (("right", (-2.4, 2.0, 0.6)), ("left", (2.4, 2.0, -0.4))):
            pose[side + "_leg"] = ((0, -hips[0][1], 0), (0, 0, 0))
            lr, sr, fr = K.leg_pose(pose, side, foot, 0.0, foot_yaw=(-6 if side == "right" else 6), guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            out[side + "_leg"], out[side + "_shin"], out[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        legs.append(out)
        s = {"torso": ((1.0 + 0.9 * br, -1.6 * sh, -2.6 * sh), None),   # shoulders tilt against the pelvis
             "head": ((-1.0 - 0.5 * br, 0.75 * math.sin(4 * math.pi * u - 1.0) - 0.6 * sh, -1.8 * sh), None),
             "right_arm": ((CARRY_R[0] - 0.6 * br + 2.0 * lag, 0.0, CARRY_R[2] + IDLE_OUT + 0.5 * br + 1.0 * max(0.0, lag)), None),
             "right_forearm": ((CARRY_RF - 1.2 * lag, 0.0, 0.0), None),
             "left_arm": ((-3.0 - 0.5 * br - 2.4 * lag, 0.0, -5.0 - 0.4 * br - 1.2 * max(0.0, -lag)), None),
             "left_forearm": ((-16.0 - 1.4 * lag, 0.0, 0.0), None), "left_hand": ((2.0 * lag, 0.0, 0.0), None),
             "hood_cape": ((0.6 + 0.5 * br, 0.0, 0.8 * lag), None), "quiver": ((0.8 * lag, 0.0, 0.6 * lag), None)}
        pose = fk_pose(s, {"hips": hips})
        s["right_hand"] = (carry_hand(pose, -10.0 + 1.5 * lag), None)   # world line: the stock stays level through the sway
        upper.append(s)
    return K.bake(legs, K.IDLE_TICKS), K.bake(upper, K.IDLE_TICKS)


_CORNERS = K.body_corners


def is_weapon(bone):
    """The crossbow: its holder bone and the draw-frame meshes under it (bow_tN_rest/pull_0/pull_1)."""
    return "weapon" in bone or bone.startswith("bow_")


def body_corners_no_weapon(pose, model=K.HEAVY):
    """The death falls rest the body on its own lowest point: the crossbow lies beside it rather than propping it up."""
    P, owner = _CORNERS(pose, model)
    keep = [i for i, o in enumerate(owner) if not is_weapon(o)]
    return P[keep], [owner[i] for i in keep]


K.body_corners = body_corners_no_weapon

# ------------------------------------------------------------------------------------------------
# Stances: the low guard, solved; StanceController plays ready (fighting), brace / wall (Hold), ward (fall_back).
# ------------------------------------------------------------------------------------------------

# The arbalist's own footing: the shooter's stance, left foot leading and the rear (right) foot turned out under the shouldered
# stock, the hips of the knight's guard (the shot and the haul start from them). brace sinks lower and wider, side-on; the wall
# is the deepest crouch, the weight forward over the lead knee.
READY_FEET = {"left": ((3.6, 2.0, 2.0), 10.0), "right": ((-3.4, 2.0, -2.6), -24.0)}
_KN_FEET = dict(K.READY_FEET)
K.READY_FEET = READY_FEET


def rehome(steps):
    """Footwork keyed from the knight's footing moved onto the arbalist's: each step lands as far from this troop's home foot as
    it did from the knight's (and turned as much), so a step home lands home."""
    out = []
    for side, t0, t1, to, yaw, *rest in steps:
        (kp, ky), (hp, hy) = _KN_FEET[side], READY_FEET[side]
        out.append((side, t0, t1, tuple(a - b + c for a, b, c in zip(to, kp, hp)), yaw - ky + hy, *rest))
    return out
STANCE_LEGS = {
    "ready": {"hips": K.READY_HIPS, "left": READY_FEET["left"][0], "right": READY_FEET["right"][0], "yaw": (-24.0, 10.0)},
    "brace": {"hips": ((3.0, 16.0, 0.0), (0.0, -2.4, 0.0)), "left": (4.2, 2.0, 3.0), "right": (-3.8, 2.0, -3.4), "yaw": (-30.0, 8.0)},
    "wall": {"hips": ((7.0, 22.0, 0.0), (0.0, -3.6, -0.2)), "left": (3.8, 2.0, 4.2), "right": (-3.6, 2.0, -4.6), "yaw": (-34.0, 4.0)},
}
STANCE_LEGS_LIFE = dict(K.STANCE_LIFE)


def _onto_hips(k, hips):
    """A key solved over the ready hips moved onto other hips: the torso keeps its world yaw, the grip drops with the hips."""
    (r0, p0), (r1, p1) = K.READY_HIPS, hips
    return nudge(k, torso=(0.0, r0[1] - r1[1], 0.0), O=(0.0, p1[1] - p0[1], p1[2] - p0[2]))


STANCE_KEYS = {
    "ready": READY,
    # brace: the stock shouldered on the outer shoulder and the cheek on it, levelled a touch low at the charge's chest, side-on
    # over the braced legs
    "brace": _onto_hips(about_butt(pitched(AIM, -6.0), -1.5, 1.8, torso=(3.8, 3.0, 0.0), head=(3.7, 10.0, 6.0),
                                   shift=(-0.88, -0.76, 0.49), Lw=(FORE_STOCK[0], 17.15, FORE_STOCK[2])), STANCE_LEGS["brace"]["hips"]),
    # wall: the line crouched behind its front, leaning into it; the stock still at the shoulder, the prod dipped at the ground
    # a few strides ahead
    "wall": _onto_hips(about_butt(AIM, 16.0, 2.2, torso=(4.5, 6.0, 0.0), head=(7.2, 10.0, 6.0), shift=(-1.24, 0.57, 1.22),
                                  Lw=(FORE_STOCK[0], 18.5, FORE_STOCK[2])), STANCE_LEGS["wall"]["hips"]),
    # ward: giving ground, leaning back, the crossbow raised flat across the chest as a bar, prod out to the left, top forward
    # (the chest square to the threat, not turned: turned 10 degrees the left fist sat on the left shoulder, the elbow at 107)
    "ward": KEY((-6, -5, 0), (4, 3, 0), (-4.5, 18.0, 7.5), (1.0, 0.12, 0.2), u=(0.0, 0.0, 1.0), rp=(-70, 0, 20, -60),
                lp=(-60, -10, -10, -50)),
    # shield_up: moving with the guard up: the crossbow held upright before the face, the prod a cover across it
    "shield_up": KEY((2, 6, 0), (-2, -6, 0), (-3.5, 16.5, 7.0), (0.25, 1.0, 0.15), u=(0.0, 0.0, 1.0), rp=(-80, 0, 15, -60),
                     lp=(-80, -10, -10, -60)),
}
LIFE = {"ready": 3.4, "brace": 1.6, "wall": 1.4, "ward": 2.0, "shield_up": 3.4}
STANCE_LEGS_LIFE.update({"brace": LIFE["brace"], "wall": LIFE["wall"]})
READY_LIKE = ("ready", "shield_up")     # these loops run at V2_READY_SWAY_SCALE (0.25); the other stances at V2_SWAY_SCALE
# warm the solve up on the loop's own last sample, so the first sample lands on the same arm branch as the rest and the loop
# closes without a jump
PREWARM = ("ready", "brace", "wall", "ward", "shield_up")


def stance_upper(k, life, hips, ready=False, prewarm=False):
    n = K.STANCE_TICKS * SUB
    sol = Solver()
    out = []
    legs = {"hips": hips}
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)    # ready: everything at V2_READY_SWAY_SCALE; others: the rock at V2_SWAY_SCALE
    for i in ([n - 1, n - 1] if prewarm else []) + list(range(n + 1)):
        u = (i % n) / n
        br = a_br * math.sin(2 * math.pi * u)
        wt = a_wt * math.sin(2 * math.pi * u - K.WEIGHT)
        wl = a_wt * math.sin(2 * math.pi * u - K.WEIGHT - 0.7)
        tx, ty, tz = k["torso"]
        hx, hy, hz = k["head"]
        torso = (tx + 0.8 * br + 1.8 * wt, ty - 1.4 * wt, tz - 1.2 * wt)
        head = (hx - 0.5 * br - 1.3 * wt, hy + 0.9 * wt, hz + 1.0 * wt)
        # the crossbow rides the breath, a beat behind it, and its nose drifts
        O = np.array(k["O"]) + np.array([0.0, 0.12 * br + 0.15 * wl, 0.1 * wl])
        dd = np.array(k["d"]) + np.array([0.0, 0.006 * br + 0.01 * wl, 0.0])
        s = sol.pose(legs, torso, head, O, dd, k["u"], left_target(k["Lw"], k["Lq"], 0.0), k["rp"], k["lp"])
        s["quiver"] = ((0.3 * br, 0.0, 0.0), None)
        out.append(s)
    out = out[2:] if prewarm else out
    out = gear(out)
    out[-1] = out[0]
    return K.bake(out, K.STANCE_TICKS)


def stance_clips():
    out = {}
    for k, v in STANCE_KEYS.items():
        hips = STANCE_LEGS.get(k, STANCE_LEGS["ready"])["hips"]
        out["stance_%s_upper" % k] = stance_upper(v, LIFE[k], hips, k in READY_LIKE, k in PREWARM)
    out.update({"stance_%s_legs" % k: K.stance_legs(v, STANCE_LEGS_LIFE.get(k, 1.0), k in READY_LIKE) for k, v in STANCE_LEGS.items()})
    out.update(order_clips())
    return out


# ------------------------------------------------------------------------------------------------
# The order stances (rally, hold, fall back, rest, aim). common/stances.py makes these for every other troop by turning the
# ready pose's joints; for the crossbow those turns drove the stock into the chest, so the arbalist's are solved here, each
# its own crossbowman's posture, and stances.py leaves them alone (its BUILDER_OWNS). Their legs stay stances.py's.
# ------------------------------------------------------------------------------------------------

# stances.py's hold legs: hips between the ready and the brace hips (0.55 of the way), sat 0.35 lower
HOLD_HIPS = tuple(tuple(a + 0.55 * (b - a) for a, b in zip(r, q)) for r, q in zip(K.READY_HIPS, STANCE_LEGS["brace"]["hips"]))
HOLD_HIPS = (HOLD_HIPS[0], (HOLD_HIPS[1][0], HOLD_HIPS[1][1] - 0.35, HOLD_HIPS[1][2]))
# hold: the stock still at the shoulder and the prod up on the threat (6 degrees down, against the ready's 12), the weight low
HOLD = _onto_hips(about_butt(AIM, 6.0, 4.0, torso=(1.5, 3.5, 0.0), head=(5.0, 8.0, 5.0), shift=(-1.2, 1.0, -0.5),
                             Lw=(FORE_STOCK[0], 18.0, FORE_STOCK[2])), HOLD_HIPS)
# aim at the troop's look pitched up (-45, the stance's up) and down (+30): the same outer-shoulder mount, re-searched per
# pitch (scratch elevopt.py): leaning back under a high shot, folded over a low one
AIM_UP = shoulder_key({"torso": (-18.2, 60.95, 0.0), "butt": (-6.29, 16.66, -4.0), "yaw": 0.32, "elev": 45.0, "left_y": 16.5},
                      head=(6.0 - 0.6 * 45.0, -60.95 - 6.0 + 0.32, -8.0))
AIM_DOWN = shoulder_key({"torso": (16.78, 61.19, 0.0), "butt": (-7.09, 25.16, -4.06), "yaw": 6.31, "elev": -30.0, "left_y": 17.4},
                        head=(6.0 + 0.6 * 30.0, -61.19 - 6.0 + 6.31, -8.0))


def stance_loop(k, life, hips, ticks, ready=False, sway=1.0):
    """stance_upper over `ticks` (its loops are K.STANCE_TICKS long), the solve warmed on its own last sample."""
    old = K.STANCE_TICKS
    K.STANCE_TICKS = ticks
    try:
        return stance_upper(k, life * sway, hips, ready, True)
    finally:
        K.STANCE_TICKS = old


def one_hand(torso, head, ra, rf, la, lf, pitch, hips, quiver=0.0):
    """A one-handed carry: the right fist holds the stock, the crossbow's line pitched in the world (+ up), aimed ahead."""
    s = {"torso": (tuple(torso), None), "head": (tuple(head), None), "right_arm": (tuple(ra), None),
         "right_forearm": ((rf, 0.0, 0.0), None), "left_arm": (tuple(la), None), "left_forearm": ((lf, 0.0, 0.0), None),
         "left_hand": (Z3, None)}
    s["right_hand"] = (carry_hand(fk_pose(s, {"hips": hips}), pitch), None)
    s["quiver"] = ((quiver, 0.0, 0.0), None)
    return s


def fk_loop(fn, ticks):
    n = ticks * SUB
    out = gear([fn(i / n) for i in range(n + 1)])
    out[-1] = out[0]
    return K.bake(out, ticks)


def fallback_upper():
    """Backing off, a step every walk cycle: the crossbow levelled one-handed from the hip at the threat (the right elbow out
    from the ribs, the stock clear of the flank), the left fist up as a guard; the chest bobs with the step and the bow stays
    on target. stances.py's fall-back legs (walk_legs backwards, hips 0.6 side-on and dropped) carry it."""
    hips = ((0.0, 0.6 * K.READY_HIPS[0][1], 0.0), (0.0, -1.6, 0.0))

    def fn(u):
        ph = 2 * math.pi * u
        tx, ty = -3.0 + 0.9 * math.cos(2 * ph), 1.6 * math.sin(ph)
        return one_hand((tx, ty, 0.0), (-tx + 2.0, -ty - hips[0][1], 0.0), (-28.0 + 1.2 * math.cos(2 * ph), 0.0, 16.0), -40.0,
                        (-12.0, 0.0, -14.0), -28.0, -4.0 - 1.5 * math.cos(2 * ph), hips, 0.8 * math.cos(2 * ph))
    return fk_loop(fn, K.WALK.ticks)


def rest_upper():
    """At ease between volleys: the crossbow hangs from the right fist by the thigh, prod down, the left arm loose; the chest
    sinks and the head takes a slow look round."""
    hips = K.READY_HIPS

    def fn(u):
        ph = 2 * math.pi * u
        br = math.sin(ph)
        tx, ty = -3.0 + 0.9 * br, -6.0 + 1.5 * math.sin(ph + 0.9)
        return one_hand((tx, ty, 0.0), (1.0 - 0.6 * br, 8.0 - ty - hips[0][1] + 5.0 * math.sin(ph), 0.0),
                        (-6.0 + 0.6 * br, 0.0, 9.0), -22.0, (-2.0 - 0.6 * br, 0.0, -6.0), -14.0, -38.0 + 1.0 * br, hips,
                        0.4 * br)
    return fk_loop(fn, 48)


RALLY_TICKS = 20
RALLY_HOLD_FROM = 13.0      # the last 7 ticks settle into the hold pose, so the clip ends exactly on it (stances.py's END_ON_HOLD)


def rally_upper(hold_clip):
    """The rally: the chest rises and opens, the head comes up with the shout, and the crossbow goes up two-handed, the prod
    at the sky (30 degrees up) and the grip lifted to the chin, shaken once at the top, then down into the hold. (A one-handed
    thrust overhead flipped the solve and swung the stock past the stance's 43.5 deg / 100 ms; this rig's arms only lift the
    grip 4 px with both hands on, searched in scratch rally.py: pen_weapon 0.56 px, pen_limb 1.03 px.)"""
    yaw = math.degrees(math.atan2(READY["d"][0], READY["d"][2]))
    d_up, u_up = aim(30.0, yaw)
    up = KEY((READY["torso"][0] - 4.0, READY["torso"][1], 0.0), (READY["head"][0] - 15.0, READY["head"][1], READY["head"][2]),
             np.array(READY["O"]) + np.array([0.0, 4.0, -1.0]), d_up, u_up, Lw=(FORE_STOCK[0], 18.0, FORE_STOCK[2]),
             rp=READY["rp"], lp=READY["lp"])
    shake = nudge(up, O=(0.0, 0.5, 0.2), torso=(-0.8, 0.0, 0.0))
    keys = [(0.0, READY), (5.0, up), (7.5, shake), (10.0, up), (RALLY_HOLD_FROM, up), (RALLY_TICKS, READY)]
    sol = Solver()
    legs = {"hips": K.READY_HIPS}
    out = []
    for i in range(RALLY_TICKS * SUB + 1):
        t = i / SUB
        v, left = solve_keys(keys, None, t)
        out.append(sol.pose(legs, v["torso"], v["head"], v["O"], v["d"], v["u"], left, v["rp"][:4], v["lp"][:4],
                            lm=min(1.0, max(0.0, v["lm"][0])), pw=v["pw"][0]))
    out = gear(out)
    # settle onto the hold clip's first frame, joint by joint
    h0 = {b: np.array(ch["rotation"]["0.0"] if "0.0" in ch["rotation"] else list(ch["rotation"].values())[0], float)
          for b, ch in hold_clip["bones"].items() if "rotation" in ch}
    for i, s in enumerate(out):
        k = max(0.0, min(1.0, (i / SUB - RALLY_HOLD_FROM) / (RALLY_TICKS - RALLY_HOLD_FROM)))
        k = k * k * (3 - 2 * k)
        for b, r in h0.items():
            if b in s and k > 0:
                s[b] = (tuple(np.array(s[b][0], float) + (r - np.array(s[b][0], float)) * k), s[b][1])
    return K.bake(out, RALLY_TICKS, loop=False)


def order_clips():
    out = {"stance_hold_upper": stance_loop(HOLD, 2.0, HOLD_HIPS, 48),
           "stance_fallback_upper": fallback_upper(),
           "stance_rest_upper": rest_upper(),
           "stance_aim_upper": stance_loop(nudge(AIM, torso=(0.5, 1.0, 0.0), head=(1.0, 0.0, 0.0)), 1.0, K.READY_HIPS, 40, sway=0.6),
           "stance_aim_up_upper": stance_loop(AIM_UP, 1.0, K.READY_HIPS, 40, sway=0.6),
           "stance_aim_down_upper": stance_loop(AIM_DOWN, 1.0, K.READY_HIPS, 40, sway=0.6)}
    out["stance_rally_upper"] = rally_upper(out["stance_hold_upper"])
    return out


# ------------------------------------------------------------------------------------------------
# Hits: additive offsets; both hands keep the crossbow, so the free left arm and the torso take the blow.
# ------------------------------------------------------------------------------------------------

off = K.off
HIT_PEAK = {
    "front": off(torso=(K.hb_front(-17), -10, 0), head=(K.hb(-12), 3, 0), rarm=(-10, 0, 18), larm=(-19, 0, -40), lfore=-12),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 14), larm=(10, 0, -20)),
    "right": off(torso=(K.hb(-5), 14, 8), head=(0, 6, 3), rarm=(6, 0, 10), larm=(-10, 0, -16)),
    "left": off(torso=(K.hb(-5), -14, -8), head=(0, -6, -3), rarm=(-10, 0, 16), larm=(10, 0, -28)),
}


def hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for d, peak in HIT_PEAK.items():
            keys = K.hit_keys(peak, weight)
            out["hit_%s_%s" % (weight, d)] = K.bake([K.sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)], ticks, loop=False)
    out["hit_block"] = out["hit_light_front"]
    out.update(K.hit_legs_clips())
    return out


# V2 hits (the knight's v2 grading, rebuilt for the crossbow): a light blow is a flinch at the knees; a medium blow shifts the
# pelvis well off the footing and takes a short lifted recovery step; heavy (HitController: medium x1.55) is the stagger. The
# body keys are the knight's v2 timing. Both arms ride their own tracks that open outward and never fold back across the body:
# the crossbow arm (right) is held out through a blow from the front, then comes home with the gather; the free arm is flung
# out with the body and gathers without overshooting inward.
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


V2_HIT_LEGS = {   # the knight's v2 hit footwork, from the same ready footing (READY_HIPS, feet as STANCE_LEGS["ready"])
    "front": {"hips": [(0, *K.READY_HIPS), (2, (K.hb(-6), 9, 0), (0.0, -1.9, 1.6)), (4.5, (K.hb(-5), 9, 0), (0.0, -2.1, 1.9)),
                       (7.5, (1, 5, 0), (0.0, -1.8, 1.3)), (10.5, (K.hb(-1), 7, 0), (0.0, -1.3, 0.5)), (16, *K.READY_HIPS)],
              "steps": [("left", 1.0, 3.8, (3.3, 2.0, -4.0), 9.0, 0.8), ("right", 3.5, 6.5, (-3.4, 2.0, 0.6), -13.0, 0.6),
                        ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
    "back": {"hips": [(0, *K.READY_HIPS), (2, (6, 4, 0), (0.0, -1.8, -1.2)), (4.5, (5, 4, 0), (0.0, -1.9, -1.4)),
                      (7.5, (K.hb(-1), 7, 0), (0.0, -1.6, -0.8)), (10.5, (0.5, 6, 0), (0.0, -1.2, -0.2)), (16, *K.READY_HIPS)],
             "steps": [("right", 1.0, 3.8, (-3.5, 2.0, 3.5), -12.0, 0.8), ("left", 3.5, 6.5, (3.2, 2.0, -0.6), 7.0, 0.6),
                       ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
    "right": {"hips": [(0, *K.READY_HIPS), (2, (K.hb(-2), 14, 4), (1.0, -1.8, 0.5)), (4.5, (K.hb(-2), 13, 4), (1.2, -1.9, 0.5)),
                       (7.5, (1, 4, 0), (0.6, -1.6, 0.2)), (10.5, (0, 6.5, 0), (0.1, -1.2, 0.1)), (16, *K.READY_HIPS)],
              "steps": [("left", 1.0, 3.8, (4.9, 2.0, -2.2), 12.0, 0.8), ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8)]},
    "left": {"hips": [(0, *K.READY_HIPS), (2, (K.hb(-2), -2, -4), (-1.0, -1.8, 0.5)), (4.5, (K.hb(-2), -1, -4), (-1.2, -1.9, 0.5)),
                      (7.5, (1, 8, 0), (-0.6, -1.6, 0.2)), (10.5, (0, 5.5, 0), (-0.1, -1.2, 0.1)), (16, *K.READY_HIPS)],
             "steps": [("right", 1.0, 3.8, (-5.0, 2.0, 1.6), -18.0, 0.8), ("right", 9.0, 12.5, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
}


for _spec in V2_HIT_LEGS.values():
    _spec["steps"] = rehome(_spec["steps"])


def v2_hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for d, peak in HIT_PEAK.items():
            pk = K.scaled(peak, V2_BODY[d])
            armed = ARM_BONES["left"] + ARM_BONES["right"]
            body = only(pk, [b for b in pk if b not in armed])
            tracks = [(K.front_onset(v2_keys(body, weight), d, weight), None),    # a 2-tick onset on the front medium blow
                      (K.front_onset(v2_keys(only(pk, ARM_BONES["right"]), weight, hold=d == "front"), d, weight), "right"),
                      (K.front_onset(v2_keys(only(pk, ARM_BONES["left"]), weight), d, weight), "left")]
            samples = []
            for i in range(ticks * SUB + 1):
                s = {}
                for keys, side in tracks:
                    v = K.sample_keys(keys, i / SUB)
                    s.update(v if side is None else {x: v[x] for x in ARM_BONES[side]})
                samples.append(s)
            out["hit_%s_%s" % (weight, d)] = K.bake(samples, ticks, loop=False)
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
# Deaths: the knight's four falls, the crossbow let go flat. From the landing on, every tick the right wrist turns the crossbow
# to lie on the ground (stock and limbs level), at whichever heading keeps it clear of the body and nearest how it already lay.
# ------------------------------------------------------------------------------------------------

FLAT_AT = {"death_back": (8, 12), "death_forward": (13, 16), "death_left": (9, 13), "death_right": (9, 13)}
_ARMED = ("right_arm", "right_forearm", "right_hand", "weapon", "bow_")


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


FLAT_TURN = 20.0     # degrees a tick the dropped crossbow may turn in the fist (a bigger jump read as a flip: ang_speed)


def flat_hand(pose, ref):
    """The right wrist rotation letting the crossbow drop: it lies along the ground, flat or tipped from the fist with its butt or
    prod on the ground, clear of the body, and as near `ref` as that allows (never more than FLAT_TURN from it: the crossbow
    settles tick by tick, it does not flip over to a better lie)."""
    q = dict(pose)
    q["right_hand"] = (ref, Z3)
    P, owner = _CORNERS(q)
    isw = np.array([is_weapon(o) for o in owner])
    ground = P[~isw, 1].min()
    boxes = _boxes(P, owner)
    L = np.linalg.inv(K.world(q, "right_hand")) @ np.c_[P[isw] / 16.0, np.ones(int(isw.sum()))].T
    def score(h, tilt, sgn):
        q["right_hand"] = (h, Z3)
        Wp = (K.world(q, "right_hand") @ L)[:3].T * 16.0
        low = Wp[:, 1].min() - ground
        return (_inside(Wp, boxes) + 3.0 * max(0.0, -low) + 0.6 * abs(low - 0.2) + turn_deg(h, ref) / 45.0
                + abs(tilt) / 60.0 + (0.5 if sgn < 0 else 0.0))
    best = (score(ref, 0, 1.0), ref)
    for phi in range(0, 360, 10):
        for tilt in range(-60, 65, 5):
            c, s = math.cos(math.radians(phi)), math.sin(math.radians(phi))
            d = np.array([c * math.cos(math.radians(tilt)), math.sin(math.radians(tilt)), s * math.cos(math.radians(tilt))])
            for sgn in (1.0, -1.0):
                h = hold_wrist(pose, d, (0.0, sgn, 0.0))
                if turn_deg(h, ref) > FLAT_TURN:
                    continue
                cost = score(h, tilt, sgn)
                if cost < best[0]:
                    best = (cost, h)
    return best[1]


def euler_near(r, prev):
    """The rotation r written as the euler triple nearest `prev` (each zyx rotation has two, each angle mod 360), so the keys
    between ticks interpolate the short way round instead of through a flip."""
    r = np.asarray(r, float)
    cands = [r, np.array([r[0] - 180.0, -180.0 - r[1], r[2] + 180.0])]
    cands = [prev + (c - prev + 180.0) % 360.0 - 180.0 for c in cands]
    return min(cands, key=lambda c: float(np.abs(c - prev).sum()))


def flat_weapon(keys, start, t0, t1):
    end = keys[-1][0]
    overs, ref = {}, None
    prev = np.asarray(K.pose_at(keys, start, t0 - 1)["right_hand"], float)
    for t in range(t0, end + 1):
        f = K.pose_at(keys, start, t)
        pose = {b: (r, Z3) for b, r in f.items()}
        ref = flat_hand(pose, ref or f["right_hand"])
        prev = euler_near(slerp_rot(f["right_hand"], ref, ramp(t, t0 - 1, t1)), prev)
        overs[t] = {"right_hand": tuple(prev)}
    t_key = max(k[0] for k in keys if k[0] <= t0 - 1)
    return K.roll_keys(keys, start, t_key, overs)


# the knight's roll onto the right side, its sword arm's sweep up past the head eased in over one more tick: with the crossbow
# in the fist the 40 degree first tick swung the stock at 90 degrees / 100 ms (ang_speed_weapon)
ROLL_RIGHT = {t: dict(v) for t, v in K.ROLL_RIGHT.items()}
ROLL_RIGHT[14]["right_arm"], ROLL_RIGHT[15]["right_arm"] = (-102.0, 0.0, 0.0), (-128.0, 0.0, 0.0)


def death_clips():
    # the falls start from the idle frame (the knight's at-ease feet), not from the shooter's footing: their footwork keeps the
    # knight's home feet, so they stay the approved falls
    saved = K.READY_FEET
    K.READY_FEET = _KN_FEET
    try:
        start = K.idle_start()
        falls = {"death_back": K.DEATH_BACK, "death_forward": K.DEATH_FORWARD,
                 "death_left": K.roll_keys(K.DEATH_LEFT, start, 13, K.ROLL_LEFT),
                 "death_right": K.roll_keys(K.DEATH_RIGHT, start, 13, ROLL_RIGHT)}
        return {n: K.death_clip(flat_weapon(k, start, *FLAT_AT[n]), start, n) for n, k in falls.items()}
    finally:
        K.READY_FEET = saved


# ------------------------------------------------------------------------------------------------

def only_rig(anims):
    gear_map = XT["gear"]
    for clip in anims.values():
        bones = {}
        for b, ch in clip["bones"].items():
            b = gear_map.get(b, b)
            if b is not None and b in GEO_BONES:
                bones[b] = ch
        clip["bones"] = bones
    return anims


def dump(path, ticks):
    """Key-pose check: the attack's upper body and legs at the given ticks, as still.py pose rows."""
    upper, legs = attack_clip()
    rows = []
    for t in ticks:
        i = int(round(t * SUB))
        p = fk_pose(upper[i], legs[i])
        rows.append({b: {"rotation": list(map(float, v[0])), "position": list(map(float, v[1]))} for b, v in p.items()})
        rows[-1]["draw"] = {"rotation": [0, 0, 0], "position": [float(upper[i]["draw"][1][0]), 0, 0]}
    Path(path).write_text(json.dumps(rows))
    print("dumped", len(rows), "poses to", path)


def main():
    if os.environ.get("XBOW_DUMP"):
        dump(os.environ["XBOW_DUMP"], [float(x) for x in os.environ.get("XBOW_TICKS", "0,2.6,5.2,10,14,19,21,28,33.5,40").split(",")])
        return
    idle_legs, idle_upper = idle_v2_clips() if V2 else idle_clips()
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
