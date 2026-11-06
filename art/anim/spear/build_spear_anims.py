"""Spear clips (unit animation rebuild, docs/anim-rebuild/STYLE.md): the spear family, first the Warmaw tusk.

GG_SPEAR_TROOP=wm_tusk python art/anim/spear/build_spear_anims.py  ->  art/anim/wm_tusk/out/wm_tusk.animation.json

Reuses the knight builder's rig maths, gait legs, leg IK and death falls (art/anim/knight/build_anims.py with GG_ANIM_TROOP set:
no shield) and keys its own upper body around the spear in the right fist.

The guard is a left-lead underarm hold: the right (rear) fist at the right hip holds the shaft at the hand pivot, the left fist
rides the shaft ahead of it, the point at the foe's chest; the left foot leads and the hips and torso turn side-on behind it.
A spear is solved, not hand-keyed: every key names where the right fist sits (O, troop px), which way the shaft points (d,
butt to tip), and how far up the shaft the left fist holds (Ls px ahead of the right fist). Each sample both arms are solved
together so the left fist stays on the shaft, and the right wrist turns the spear onto d with the blade's flat upright.

The thrusts slide the shaft through the left fist: the rear hand drives, the leading hand guides.
"""
import json, math, os, sys
from pathlib import Path

TROOP = os.environ.get("GG_SPEAR_TROOP", "wm_tusk")
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
# Secondary cloth (STYLE.md rule 9): the knight's cape lag, given to each troop's hanging gear at a scale.
SPEAR_TROOPS = {"wm_tusk": {"gear": {"kilt": 0.4, "trophy": 0.7}},
                "tc_tideguard": {"gear": {"tabard": 0.35}, "rear_x": -0.6, "death_lookahead": True},
                "tc_admiral": {"gear": {"cape": 0.35, "tails_l": 0.7, "tails_r": 0.7}, "rear_x": -0.6, "death_lookahead": True, "death_top": 0.15, "death_step": 0.25,
                                  "soft": ("hair_adrift", "kelp_", "plume", "tails_", "sash_end")},
                # The Iron Clans breaker swings a two-handed greataxe (art/anim/greataxe): it borrows this builder's solver, footing,
                # carry, hits and falls, and keys its own swings.
                "ic_breaker": {"gear": {"cape": 0.5, "tabard": 0.3, "tabard_skirt": 0.3, "apron": 0.3}, "death_lookahead": True,
                               "death_step": 0.25, "death_top": 0.15}}
ST = SPEAR_TROOPS[TROOP]
LOOKAHEAD = ST.get("death_lookahead", False)   # per troop: wrist turn planned backwards through the fall (see flat_weapon)
RX = ST.get("rear_x", 0.0)        # per-troop shift of the thrust contact fist outward, keeping the butt out of a wider torso
K.SOFT = ST.get("soft", ())   # per troop: hanging hair and kelp never hold the falling body up (knight death_clip)
TOP_W = ST.get("death_top", 0.0)  # per troop: cost per px the dropped weapon's top stands above TOP_OK (keeps it low in a roll)
TOP_OK = 8.0
DEATH_STEP = ST.get("death_step", 0.5)  # per troop: key spacing of the solved fall; 0.25 matches the bake, so a fast wrist swing
                                        # cannot dip the weapon under the floor between keys
W = "weapon_t1"                 # every tier's spear shares the pivot and axes

# ------------------------------------------------------------------------------------------------
# The spear frame. Weapon-local px: the shaft runs along y at x -6, z 0; the right fist holds it at y 14 (the hand pivot).
# A shaft point s px from the fist is (-6, 14 + s, 0): the butt behind, the tip ahead. d = weapon +y (butt to tip),
# u = weapon -z; the blade's width runs along weapon x, so u level and square to d stands the blade's flat upright.
# ------------------------------------------------------------------------------------------------

_HAND_TO_W = K.world({}, "right_hand")[:3, :3].T @ K.world({}, W)[:3, :3]
GRIP = (-6.0, 14.0, 0.0)            # right_hand-local: the hand pivot, where the fist holds the shaft
FIST_L = (6.0, 12.5, 0.0)           # left_forearm-local: the centre of the left fist
_WY = [c["origin"][1] + k * c["size"][1] for b in _GEO if b["name"].startswith("weapon_t") for c in b.get("cubes", []) for k in (0, 1)]
BUTT, TIP = min(_WY) - 14.0, max(_WY) - 14.0


def S(s):
    return (-6.0, 14.0 + s, 0.0)


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def frame(d, u):
    d = unit(d)
    u = np.asarray(u, float) - (np.asarray(u, float) @ d) * d
    return d, unit(u)


def level_u(d, sgn=1.0):
    """The blade's flat upright: u level and square to the shaft."""
    d = unit(d)
    u = np.cross((0.0, 1.0, 0.0), d)
    if np.linalg.norm(u) < 1e-3:
        u = np.array([1.0, 0.0, 0.0])
    return sgn * unit(u)


def hold_wrist(pose, d, u):
    """right_hand rotation turning the spear onto (d, u)."""
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


def wpoint(pose, a):
    return K.point(K.world(pose, W), a)


def aim(elev, yaw):
    """A shaft direction: elev degrees up, yaw degrees toward the troop's left (+x)."""
    e, y = math.radians(elev), math.radians(yaw)
    return np.array([math.sin(y) * math.cos(e), math.sin(e), math.cos(y) * math.cos(e)])


# ------------------------------------------------------------------------------------------------
# Keys. Each: torso, head (deg, on top of the hips), O right fist (troop px), d shaft, Ls left fist (px up the shaft from the
# right fist), and the arm priors (right 4, left 4) the solve starts from.
# ------------------------------------------------------------------------------------------------

def KEY(torso, head, O, d, Ls=7.0, rp=(-20, 0, 10, -60), lp=(-60, -30, 0, -40), sgn=1.0):
    d = unit(d)
    return {"torso": tuple(map(float, torso)), "head": tuple(map(float, head)), "O": tuple(map(float, O)), "d": tuple(d),
            "u": tuple(level_u(d, sgn)), "Ls": (float(Ls),), "rp": tuple(map(float, rp)), "lp": tuple(map(float, lp))}


def nudge(k, **kw):
    q = dict(k)
    for n, v in kw.items():
        if n in ("O", "torso", "head"):
            q[n] = tuple(a + b for a, b in zip(k[n], v))
        elif n == "d":
            q["d"] = tuple(unit(v))
            q["u"] = tuple(level_u(v, 1.0 if np.dot(level_u(v), k["u"]) >= 0 else -1.0))
        elif n == "Ls":
            q["Ls"] = (float(v),)
        else:
            q[n] = v
    return q


def fk_pose(s, legs):
    pose = {b: (v[0], v[1] or Z3) for b, v in s.items()}
    pose.update({b: (v[0], v[1] or Z3) for b, v in legs.items()})
    return pose


def interp(keys, t, name):
    return K.curve([(k[0], k[1][name]) for k in keys], t)


class Solver:
    """Solves sample after sample along one clip, both arms warm-started from the last sample so the elbows never flip. The
    left fist's contact on the shaft is held hard, the right fist's place is a softer target, so a shaft out of the left arm's
    reach slides back toward the body instead of the fist floating free of it."""

    def __init__(self, wo=1.0):
        self.warm = None
        self.wo = wo
        self.worst = {"right": 0.0, "left": 0.0}

    def pose(self, legs, torso, head, O, d, u, Ls, rp, lp, extra=None):
        s = {"torso": (tuple(torso), None), "head": (tuple(head), None)}
        if extra:
            s.update(extra)
        pose = fk_pose(s, legs)
        O, rp, lp = np.asarray(O, float), np.asarray(rp, float), np.asarray(lp, float)
        prior = np.r_[rp, lp]
        warm = prior if self.warm is None else self.warm

        def build(z):
            p = _arm("left", _arm("right", pose, z[:4]), z[4:])
            p["right_hand"] = (hold_wrist(p, d, u), Z3)
            return p

        def f(z):
            p = build(z)
            g = K.point(K.world(p, "right_hand"), GRIP)
            fl = K.point(K.world(p, "left_forearm"), FIST_L)
            return np.concatenate([self.wo * (g - O), 4.0 * (fl - wpoint(p, S(Ls))), 0.02 * (z - prior)])

        clamp = lambda z: np.r_[z[:3], min(-2.0, max(-150.0, z[3])), z[4:7], min(-2.0, max(-150.0, z[7]))]
        z = gn(f, warm, clamp=clamp)
        r = f(z)
        self.warm = z
        self.worst["right"] = max(self.worst["right"], np.linalg.norm(r[:3]) / self.wo)
        self.worst["left"] = max(self.worst["left"], np.linalg.norm(r[3:6]) / 4.0)
        for side, v in (("right", z[:4]), ("left", z[4:])):
            s[side + "_arm"], s[side + "_forearm"] = (tuple(v[:3]), None), ((v[3], 0.0, 0.0), None)
        pose = fk_pose(s, legs)
        s["right_hand"] = (hold_wrist(pose, d, u), None)
        s["left_hand"] = (Z3, None)
        return s


def sample(keys, legs_at, sol, t):
    v = {n: interp(keys, t, n) for n in ("torso", "head", "O", "d", "u", "Ls", "rp", "lp")}
    d, u = frame(v["d"], v["u"])
    return sol.pose(legs_at, v["torso"], v["head"], v["O"], d, u, v["Ls"][0], v["rp"], v["lp"])


# ------------------------------------------------------------------------------------------------
# Body clearance: the shaft must pass beside the body, never through it.
# ------------------------------------------------------------------------------------------------

_ARMED = ("right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand", "weapon", "wrap")


def body_boxes(pose):
    P, owner = K.body_corners(pose)
    out = []
    for c in range(0, len(owner), 8):
        if any(a in owner[c] for a in _ARMED) or owner[c] not in ("torso", "hips", "head", "right_leg", "left_leg",
                                                                   "right_shin", "left_shin"):
            continue
        out.append((owner[c], P[c], [(ax / np.linalg.norm(ax), np.linalg.norm(ax)) for ax in (P[c + 1] - P[c], P[c + 2] - P[c], P[c + 6] - P[c])
                                     if np.linalg.norm(ax) > 1e-6]))
    return out


def shaft_hits(pose, margin=0.3):
    """Shaft samples (s, bone) inside the body's main boxes."""
    boxes = body_boxes(pose)
    hits = []
    for s in np.arange(BUTT, TIP + 0.01, 1.0):
        p = wpoint(pose, S(s))
        for name, o, axes in boxes:
            if all(margin < (p - o) @ a < L - margin for a, L in axes):
                hits.append((round(float(s), 1), name))
    return hits


# ------------------------------------------------------------------------------------------------
# Left-lead footing. The spearman stands left foot forward, hips turned 20 deg side-on behind it; the knight's attack, stance,
# hit and block legs are all built from READY_FEET / READY_HIPS, so they follow. The knight's (right-lead) hit and block steps
# are mirrored onto this footing: each hips key keeps its offset from the ready hips, each step its offset from the ready foot.
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


def stance_legs(spec, life=1.0, ticks=K.STANCE_TICKS):
    """The knight's stance legs, mirrored for the left lead: the weight rides forward over the left foot and back over the right."""
    n = ticks * SUB
    out, guesses = [], {}
    for i in range(n + 1):
        u = (i % n) / n
        br = life * math.sin(2 * math.pi * u)
        wt = (life - 1.0) * math.sin(2 * math.pi * u - K.WEIGHT)
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


# ------------------------------------------------------------------------------------------------
# The head holds its gaze: keys give where the eyes look (troop frame); the hips' and torso's turn is taken back out.
# ------------------------------------------------------------------------------------------------

def head_local(gaze, torso, hips_rot):
    return tuple(g - t - h for g, t, h in zip(gaze, torso, hips_rot))


def sample(keys, legs_at, sol, t):  # noqa: F811  (replaces the core's: the head key is a gaze)
    v = {n: interp(keys, t, n) for n in ("torso", "head", "O", "d", "u", "Ls", "rp", "lp")}
    d, u = frame(v["d"], v["u"])
    head = head_local(v["head"], v["torso"], legs_at["hips"][0])
    return sol.pose(legs_at, v["torso"], head, v["O"], d, u, v["Ls"][0], v["rp"], v["lp"])


# ------------------------------------------------------------------------------------------------
# Guard and attacks. The guard: right fist at the right hip, the shaft rising 12 deg to the foe's chest, the left fist 6 px up it.
# The thrust draws the shaft back through the left fist (held), then drives the rear fist up to the front one, the body
# lunging behind it off a step of the lead foot. The high thrust dips first and drives up at the face. The low sweep winds the
# point out past the lead side and swings the shaft flat across the shins, the hips dropping and turning through it.
# ------------------------------------------------------------------------------------------------

GAZE = (4.0, 0.0, 0.0)
GUARD = KEY((14, 15, 0), GAZE, (-4.5, 13.5, 2.0), aim(12, 18), Ls=6.0)
ATTACK_TICKS = K.ATTACK_TICKS     # 22
IMPACT = {"thrust": 7, "high_thrust": 7, "low_sweep": 8}

ATTACKS = {
    "thrust": [
        (0, GUARD),
        (2.5, KEY((8, 28, 0), (2, 0, 0), (-5.2, 13.8, -4.0), aim(10, 21), Ls=11.5)),
        (5.5, KEY((7, 32, 0), (2, 0, 0), (-5.4, 13.6, -4.8), aim(10, 22), Ls=12.0)),
        (7, KEY((28, 4, 0), (7, 0, 0), (-4.0 + RX, 13.2, 10.0), aim(3, 10), Ls=2.5)),
        (9, KEY((31, 6, 0), (7, 0, 0), (-4.2 + RX, 13.4, 11.0), aim(2, 11), Ls=2.0)),
        (12, KEY((20, 10, 0), (5, 0, 0), (-3.5, 14.0, 7.0), aim(8, 14), Ls=4.5)),
        (17, nudge(GUARD, O=(0.2, 0.0, 0.6), torso=(1, -1, 0))),
        (22, GUARD)],
    "high_thrust": [
        (0, GUARD),
        (2.5, KEY((6, 27, 0), (0, 0, 0), (-5.0, 11.2, -3.2), aim(6, 21), Ls=10.5)),
        (5.5, KEY((5, 31, 0), (-2, 0, 0), (-5.2, 10.8, -4.0), aim(5, 22), Ls=11.0)),
        (7, KEY((18, 5, 0), (-7, 0, 0), (-4.8, 18.0, 10.0), aim(21, 10), Ls=2.5)),
        (9, KEY((20, 7, 0), (-7, 0, 0), (-5.2, 18.8, 10.8), aim(23, 12), Ls=2.0)),
        (12, KEY((14, 10, 0), (0, 0, 0), (-3.5, 16.5, 6.5), aim(16, 14), Ls=4.5)),
        (17, nudge(GUARD, O=(0.2, 0.3, 0.6), torso=(0, -1, 0))),
        (22, GUARD)],
    "low_sweep": [
        (0, GUARD),
        (3, KEY((12, 30, 0), (8, 8, 0), (-5.0, 13.0, 0.0), aim(5, 36), Ls=7.5)),
        (5.5, KEY((10, 38, 0), (10, 10, 0), (-5.5, 12.8, -1.0), aim(2, 45), Ls=8.0)),
        (8, KEY((28, 6, 0), (20, 0, 0), (-6.5, 11.0, 4.5), aim(-5, 10), Ls=4.0)),
        (10.5, KEY((28, 6, 0), (20, -4, 0), (-8.0, 11.0, 4.5), aim(-5, 4), Ls=4.0)),
        (14, KEY((22, 8, 0), (10, 0, 0), (-6.0, 13.0, 3.0), aim(4, 8), Ls=5.0)),
        (18, nudge(GUARD, O=(0.2, 0.0, 0.5), torso=(1, -2, 0))),
        (22, GUARD)],
}

ATTACK_LEGS = {
    "thrust": {"hips": [(0, *READY_HIPS), (2.5, (-4, 30, 0), (0.2, -1.7, 1.0)), (5.5, (-5, 32, 0), (0.2, -1.8, 1.2)),
                        (7, (7, 4, 0), (-0.2, -3.0, -2.6)), (9, (8, 3, 0), (-0.3, -3.2, -2.9)), (13, (4, 10, 0), (0.0, -2.4, -1.6)),
                        (17, (0, 18, 0), (0.0, -1.5, -0.3)), (22, *READY_HIPS)],
               "steps": [("left", 5.0, 6.8, (3.6, 2.0, 6.8), 12.0, 1.4), ("left", 13.0, 17.0, (3.4, 2.0, 2.4), 14.0, 1.0)]},
    "high_thrust": {"hips": [(0, *READY_HIPS), (2.5, (-4, 29, 0), (0.2, -2.2, 0.9)), (5.5, (-5, 31, 0), (0.2, -2.5, 1.1)),
                             (7, (1, 5, 0), (-0.2, -1.5, -2.4)), (9, (2, 4, 0), (-0.3, -1.4, -2.7)), (13, (1, 11, 0), (0.0, -1.6, -1.5)),
                             (17, (0, 18, 0), (0.0, -1.4, -0.3)), (22, *READY_HIPS)],
                    "steps": [("left", 5.0, 6.8, (3.6, 2.0, 6.6), 12.0, 1.4), ("left", 13.0, 17.0, (3.4, 2.0, 2.4), 14.0, 1.0)]},
    "low_sweep": {"hips": [(0, *READY_HIPS), (5.5, (4, 32, 0), (0.3, -2.2, 0.5)), (8, (10, 4, 0), (-0.2, -3.0, -0.5)),
                           (10.5, (11, 2, 0), (-0.4, -3.1, -0.6)), (14, (6, 6, 0), (-0.2, -2.4, -0.3)),
                           (18, (1, 16, 0), (0.0, -1.5, 0.0)), (22, *READY_HIPS)],
                  "steps": [("left", 1.5, 4.5, (4.4, 2.0, 3.0), 22.0, 1.2), ("right", 6.0, 9.0, (-2.8, 2.0, -1.4), -24.0, 0.0),
                            ("left", 13.0, 17.0, (3.4, 2.0, 2.4), 14.0, 1.0), ("right", 15.0, 19.0, (-3.2, 2.0, -2.2), -6.0, 0.6)]},
}


def gear(samples, legs=None):
    """Secondary motion (STYLE.md rule 9): the knight's cape lag, scaled onto each hanging piece of the troop's gear."""
    K.cape_lag(samples, legs)
    for s in samples:
        c = s.pop("cape")[0]
        for g, k in ST["gear"].items():
            s[g] = ((k * c[0], 0.0, k * c[2]), None)
    return samples


def attack_clip(name):
    keys, legs = ATTACKS[name], K.attack_legs(ATTACK_LEGS[name])
    sol = Solver()
    out, hits = [], 0
    for i in range(ATTACK_TICKS * SUB + 1):
        s = sample(keys, legs[i], sol, i / SUB)
        hits += bool(shaft_hits(fk_pose(s, legs[i])))
        out.append(s)
    print("attack_%s: grip err max %.3f px, left fist err max %.3f px, samples with the shaft in the body %d"
          % (name, sol.worst["right"], sol.worst["left"], hits))
    return out, legs


def attack_clips():
    out = {}
    for name in ATTACKS:
        upper, legs = attack_clip(name)
        clip = K.bake(gear(upper, legs), ATTACK_TICKS, loop=False)
        clip["timeline"] = {K.fmt_t(IMPACT[name] / TPS): "impact"}
        out["attack_" + name] = clip
        out["attack_%s_legs" % name] = K.bake(legs, ATTACK_TICKS, loop=False)
    return out


# ------------------------------------------------------------------------------------------------
# Carry: walking, the spear rides at the slope in the right fist, point up and forward and turned a little out; running, it
# drops to the trail, point forward. The left arm swings free.
# ------------------------------------------------------------------------------------------------

CARRY_R = (-12.0, 0.0, 6.0)
CARRY_RF = -46.0


def carry_hand(pose, pitch, yaw=0.0):
    return hold_wrist(pose, aim(pitch, yaw), level_u(aim(pitch, yaw)))


def gait_pose(g, c, u, run, legs):
    tw = 2 * math.pi
    (hx, hy, hz), _ = g.hips(u)
    ty = (g.yaw + c.twist) * math.cos(tw * u)
    tz = -0.8 * hz - c.roll * math.sin(tw * (u - 0.03))
    tx = c.lean + 0.6 * math.cos(tw * 2 * (u - g.bob_phase))
    net_yaw, net_roll = hy + ty, hz + tz
    head = (-(hx + tx) * c.head_keep, -net_yaw * c.head_keep, -net_roll * c.head_keep)
    sw = math.cos(tw * (u - c.arm_lag))
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
    print("%s carry: butt lowest %.1f px above the ground, samples with the shaft in the body %d" % (g.name, low, hits))
    samples.append(samples[0])
    return K.bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Stances and idle: all in guard (STYLE.md: the guard is the ready pose; idle stays in it). StanceController plays ready
# (fighting), brace / wall (Hold), ward (fall_back).
# ------------------------------------------------------------------------------------------------

STANCE_KEYS = {
    "ready": GUARD,
    # brace: sunk over the braced legs, the butt low, the point up at a rider's chest
    "brace": KEY((18, 16, 0), (2, 0, 0), (-5.0, 11.8, 0.5), aim(20, 14), Ls=7.0),
    # wall: the formed line, crouched, the shaft level and long over the lead foot
    "wall": KEY((22, 18, 0), (6, 0, 0), (-4.0, 13.5, 3.0), aim(5, 12), Ls=5.0),
    # ward: giving ground, upright, the point raised to fend
    "ward": KEY((4, 12, 0), (0, 0, 0), (-4.5, 14.5, 1.5), aim(24, 16), Ls=6.5),
}
STANCE_KEYS["shield_up"] = STANCE_KEYS["ready"]
LIFE = {"ready": 3.4, "brace": 1.6, "wall": 1.4, "ward": 2.0, "shield_up": 3.4}
IDLE_LIFE = 2.2


def stance_upper(k, life, legs_spec, ticks=K.STANCE_TICKS):
    n = ticks * SUB
    sol = Solver()
    out = []
    hips = (legs_spec or STANCE_LEGS["ready"])["hips"]
    legs = {"hips": hips}
    for i in range(n + 1):
        u = (i % n) / n
        br = life * math.sin(2 * math.pi * u)
        wt = (life - 1.0) * math.sin(2 * math.pi * u - K.WEIGHT)
        wl = (life - 1.0) * math.sin(2 * math.pi * u - K.WEIGHT - 0.7)
        tx, ty, tz = k["torso"]
        torso = (tx + 0.8 * br + 1.8 * wt, ty + 1.4 * wt, tz + 1.2 * wt)
        gaze = tuple(np.array(k["head"]) + np.array([-0.3 * br, 0.0, 0.0]))
        head = head_local(gaze, torso, hips[0])
        # the shaft rides the breath a beat behind it, its point drifting
        O = np.array(k["O"]) + np.array([0.0, 0.12 * br + 0.15 * wl, 0.1 * wl])
        dd = np.array(k["d"]) + np.array([0.0, 0.006 * br + 0.01 * wl, 0.0])
        out.append(sol.pose(legs, torso, head, O, unit(dd), k["u"], k["Ls"][0], k["rp"], k["lp"]))
    out = gear(out)
    out[-1] = out[0]
    return K.bake(out, ticks)


def stance_clips():
    out = {"stance_%s_upper" % k: stance_upper(v, LIFE[k], STANCE_LEGS.get(k)) for k, v in STANCE_KEYS.items()}
    out.update({"stance_%s_legs" % k: stance_legs(v, K.STANCE_LIFE.get(k, 1.0)) for k, v in STANCE_LEGS.items()})
    return out


def idle_clips():
    return stance_legs(STANCE_LEGS["ready"], IDLE_LIFE, K.IDLE_TICKS), stance_upper(GUARD, IDLE_LIFE, None, K.IDLE_TICKS)


K.idle_clips = idle_clips    # the death falls start from the guard

# ------------------------------------------------------------------------------------------------
# Hits: additive offsets. Both fists keep the spear, so the torso and head take the blow and the arms move together.
# ------------------------------------------------------------------------------------------------

off = K.off
HIT_PEAK = {
    "front": off(torso=(-17, -10, 0), head=(-12, 4, 0), rarm=(-8, 0, 6), larm=(-8, 0, -5), lfore=-6),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(6, 0, 4), larm=(6, 0, -4)),
    "right": off(torso=(-5, 14, 8), head=(0, 6, 3), rarm=(4, 0, 8), larm=(-4, 0, -4)),
    "left": off(torso=(-5, -14, -8), head=(0, -6, -3), rarm=(-4, 0, 4), larm=(4, 0, -8)),
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


# ------------------------------------------------------------------------------------------------
# Deaths: the knight's four falls, the spear let go flat. From the landing on, every tick the right wrist turns the spear to lie
# on the ground, at whichever heading keeps it clear of the body and nearest how it already lay.
# ------------------------------------------------------------------------------------------------

_CORNERS = K.body_corners


def is_weapon(bone):
    return "weapon" in bone


def body_corners_no_weapon(pose, model=K.HEAVY):
    """The death falls rest the body on its own lowest point: the spear lies beside it rather than propping it up."""
    P, owner = _CORNERS(pose, model)
    keep = [i for i, o in enumerate(owner) if not is_weapon(o)]
    return P[keep], [owner[i] for i in keep]


K.body_corners = body_corners_no_weapon

FLAT_AT = {"death_back": (8, 12), "death_forward": (13, 16), "death_left": (9, 13), "death_right": (9, 13)}
_ARMED_D = ("right_arm", "right_forearm", "right_hand", "weapon")


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
        if any(a in owner[c] for a in _ARMED_D):
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
    """The floor the dropped weapon must clear: the body's lowest point, not counting soft hanging strands, which hang
    below it until death_clip settles them on the ground."""
    hard = ~isw & np.array([not any(o.startswith(h) for h in K.SOFT) for o in owner], dtype=bool)
    return P[hard, 1].min()


def flat_hand(pose, ref):
    """The right wrist rotation letting the spear drop: it lies along the ground, flat or tipped from the fist with its butt or
    point on the ground, clear of the body, and as near `ref` as that allows."""
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


def _axis_rot(axis, deg):
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
    """The wrist rotation nearest `rot` (and the last tick's, `prev`) keeping the whole spear at or above the ground (the
    body's lowest point) and out of the body: the spear tips about its own fist by the smallest angle that clears, so it
    never stabs the floor. The left arm is left out of the body here: its fist lets go of the shaft as the troop falls."""
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
    _, _, vt = np.linalg.svd(pts - pts.mean(0))        # the spear's direction: the widest spread of its corners
    axes = []
    tilt = np.cross(vt[0], (0.0, 1.0, 0.0))
    if np.linalg.norm(tilt) > 1e-6:
        axes += [tilt, -tilt]
    axes += [np.array([math.cos(a), 0.0, math.sin(a)]) for a in np.radians(np.arange(0, 360, 30))]
    best = None
    for ax in axes:
        for deg in np.arange(1.0, 151.0, 1.0):
            r = K.decompose_zyx(Rp.T @ _axis_rot(ax, deg) @ Rh)
            q["right_hand"] = (r, Z3)
            Wp = (K.world(q, "right_hand") @ L)[:3].T * 16.0
            low = Wp[:, 1].min() - ground
            if low < GROUND_CLEAR - 0.05:
                continue
            cost = deg / 20.0 + 0.4 * _inside(Wp, boxes) + (turn_deg(r, prev) / 15.0 if prev is not None else 0.0)
            if best is None or cost < best[0]:
                best = (cost, r)
            break                                      # the smallest clearing angle about this axis
    return rot if best is None else best[1]


def near_euler(r, prev):
    """The Euler triple for rotation `r` closest to `prev`, so per-tick keys interpolate the short way."""
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
    """Re-key the fall every half tick (DEATH_STEP) with the right wrist solved: free, then easing from t0 to the spear lying
    flat by t1, always tipped clear of the ground and swinging at most MAX_WRIST_TURN per tick."""
    step = DEATH_STEP if step is None else step
    end = keys[-1][0]
    out, ref, prev = [], None, start["right_hand"][0]
    for t in np.arange(step, end + step / 2, step):
        t = float(t)
        f = K.pose_at(keys, start, t)
        pose = {b: (r, Z3) for b, r in f.items()}
        pose["hips"] = (f["hips"], start["hips"][1])     # as death_clip poses it: the hips sit at the idle height
        hand = f["right_hand"]
        if t >= t0:
            ref = flat_hand(pose, ref or prev)
            hand = slerp_rot(hand, ref, ramp(t, t0 - 1, t1))
        r = ground_hand(pose, hand, prev)
        turn, cap = turn_deg(prev, r), MAX_WRIST_TURN * step
        if LOOKAHEAD:                                  # solve every key free first; the turn cap is applied backwards below
            out.append((t, f, pose, r))
            prev = r
            continue
        if turn > cap:                                 # the spear swings round, it does not flip in a tick
            r = ground_hand(pose, slerp_rot(prev, r, cap / turn), prev)
        prev = near_euler(r, prev)
        f["right_hand"] = prev
        out.append((t, "lin", f))
    if not LOOKAHEAD:
        return out
    # Look-ahead: walking back from the end, a key the next one is too far from turns part-way towards it, so the wrist
    # starts swinging early instead of flipping late; then a forward pass caps whatever is left.
    cap = MAX_WRIST_TURN * step
    rs = [o[3] for o in out]
    for i in range(len(rs) - 2, -1, -1):
        if out[i][0] < 3.0:                            # the first ticks keep the forward solve: the arm drops the spear
            break                                      # into the floor at once there, and only the reactive turn clears it
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


def death_clips():
    start = K.idle_start()
    falls = {"death_back": K.DEATH_BACK, "death_forward": K.DEATH_FORWARD,
             "death_left": K.roll_keys(K.DEATH_LEFT, start, 13, K.ROLL_LEFT),
             "death_right": K.roll_keys(K.DEATH_RIGHT, start, 13, K.ROLL_RIGHT)}
    return {n: K.death_clip(flat_weapon(k, start, *FLAT_AT[n]), start, n) for n, k in falls.items()}


# ------------------------------------------------------------------------------------------------

def only_rig(anims):
    for clip in anims.values():
        clip["bones"] = {b: ch for b, ch in clip["bones"].items() if b in GEO_BONES}
    return anims


def dump(path, name, ticks):
    """Key-pose check: an attack's (or the guard's) whole body at the given ticks, as still.py pose rows."""
    if name == "guard":
        legs = K.attack_legs({"hips": [(0, *READY_HIPS), (1, *READY_HIPS)], "steps": []}, ticks=1)
        sol = Solver()
        poses = [fk_pose(sample([(0, GUARD), (1, GUARD)], legs[0], sol, 0.0), legs[0])]
        print("guard: grip err %.3f, left err %.3f, hits %s" % (sol.worst["right"], sol.worst["left"], shaft_hits(poses[0])))
    else:
        upper, legs = attack_clip(name)
        poses = [fk_pose(upper[int(round(t * SUB))], legs[int(round(t * SUB))]) for t in ticks]
    rows = [{b: {"rotation": list(map(float, v[0])), "position": list(map(float, v[1]))} for b, v in p.items()} for p in poses]
    Path(path).write_text(json.dumps(rows))
    print("dumped", len(rows), "poses to", path)


def main():
    if os.environ.get("SPEAR_DUMP"):
        dump(os.environ["SPEAR_DUMP"], os.environ.get("SPEAR_ATTACK", "thrust"),
             [float(x) for x in os.environ.get("SPEAR_TICKS", "0,2.5,5,7,9,12,17").split(",")])
        return
    idle_legs, idle_upper = idle_clips()
    anims = {"walk_legs": K.gait_legs(K.WALK), "walk_upper": gait_upper(K.WALK, K.WALK_CARRY, False),
             "run_legs": K.gait_legs(K.RUN), "run_upper": gait_upper(K.RUN, K.RUN_CARRY, True),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **stance_clips(), **hit_clips(),
             **death_clips()}
    only_rig(anims)
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, "clips:", ", ".join(anims))


if __name__ == "__main__":
    main()
