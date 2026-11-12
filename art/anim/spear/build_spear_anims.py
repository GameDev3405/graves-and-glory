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
SPEAR_TROOPS = {"wm_tusk": {"gear": {"kilt": 0.4, "trophy": 0.7}, "fall": {"death_back": {"step": 0.25, "turn": 36.0}}},
                "tc_tideguard": {"gear": {"tabard": 0.35}, "rear_x": -0.6, "death_lookahead": True, "no_lookahead": ("death_left",)},
                "tc_admiral": {"gear": {"cape": 0.35, "tails_l": 0.7, "tails_r": 0.7}, "rear_x": -0.6, "death_lookahead": True, "death_top": 0.15, "death_step": 0.25,
                                  "soft": ("hair_adrift", "kelp_", "plume", "tails_", "sash_end")},
                # The Iron Clans breaker swings a two-handed greataxe (art/anim/greataxe): it borrows this builder's solver, footing,
                # carry, hits and falls, and keys its own swings.
                "ic_breaker": {"gear": {"cape": 0.5, "tabard": 0.3, "tabard_skirt": 0.3, "apron": 0.3}, "death_lookahead": True,
                               "death_step": 0.25, "death_top": 0.15},
                # The Graveborn reaper swings a two-handed scythe (art/anim/scythe), the same way the breaker swings its axe.
                "reaper": {"gear": {"shroud": 0.6, "plate_cape": 0.4, "mantle": 0.15, "sash_tails": 0.7, "rags": 0.6, "cord": 0.7,
                                    "hood_drape": 0.4}, "death_lookahead": True, "death_step": 0.25, "death_top": 0.15,
                           "soft": ("rags", "sash_tails", "cord", "shroud")}}
ST = SPEAR_TROOPS[TROOP]
# V2 feel (group C, 2026-11-07): the knight's v2 weight and flow on the two-handed shaft. Each blow is driven from the pelvis
# (the hips load back over the rear foot first, the arms follow, the hips drive forward over the lead foot on the impact and
# the rear foot catches up); hits are graded (a light knee flinch, a medium pelvis shift with a lifted recovery step); every
# stance rocks its weight and the stances read apart (ready tallest, brace lower and wider, the line deepest, the ward leaning
# back). Only these troops: the reaper and the rest keep the old build. GG_V2=0 builds the old style.
V2C = TROOP in ("wm_tusk", "tc_tideguard", "tc_admiral", "ic_breaker") and os.environ.get("GG_V2") != "0"
LOOKAHEAD = ST.get("death_lookahead", False)   # per troop: wrist turn planned backwards through the fall (see flat_weapon)
# per troop: falls keyed without the look-ahead. tc_tideguard's roll to the left: the planned-back wrist turn swung the spear round
# with the body's own roll at t11 (scorecard ang_speed_weapon 84, limit 83.2); the reactive turn keeps it under the cap.
NO_LOOKAHEAD = ST.get("no_lookahead", ())
RX = ST.get("rear_x", 0.0)        # per-troop shift of the thrust contact fist outward, keeping the butt out of a wider torso
K.SOFT = ST.get("soft", ())   # per troop: hanging hair and kelp never hold the falling body up (knight death_clip)
TOP_W = ST.get("death_top", 0.0)  # per troop: cost per px the dropped weapon's top stands above TOP_OK (keeps it low in a roll)
TOP_OK = 8.0
FALL = ST.get("fall", {})   # per troop and fall: its own key spacing and wrist-turn cap (wm_tusk's fall back from the wide
# guard swung the spear 97 deg/100ms at the 45 deg/tick cap, limit 83.2)
DEATH_STEP = ST.get("death_step", 0.5)  # per troop: key spacing of the solved fall; 0.25 matches the bake, so a fast wrist swing
                                        # cannot dip the weapon under the floor between keys
W = "weapon_t1"                 # every tier's spear shares the pivot and axes

# ------------------------------------------------------------------------------------------------
# The spear frame. Weapon-local px: the shaft runs along y at x -6, z 0; the right fist holds it at y 14 (the hand pivot).
# A shaft point s px from the fist is (-6, 14 + s, 0): the butt behind, the tip ahead. d = weapon +y (butt to tip),
# u = weapon -z; the blade's width runs along weapon x, so u level and square to d stands the blade's flat upright.
# ------------------------------------------------------------------------------------------------

_HAND_TO_W = K.world({}, "right_hand")[:3, :3].T @ K.world({}, W)[:3, :3]
# The fist pivots come from the rig: (-6, 14) on the stock arm, (-5, 13) on a slimmer one (the reaper's bony arms).
_PIV = {b["name"]: b["pivot"] for b in _GEO}
GX, GY = float(_PIV["right_hand"][0]), float(_PIV["right_hand"][1])
GRIP = (GX, GY, 0.0)                # right_hand-local: the hand pivot, where the fist holds the shaft
FIST_L = (float(_PIV["left_hand"][0]), float(_PIV["left_hand"][1]) - 1.5, 0.0)   # left_forearm-local: the left fist's centre
_WY = [c["origin"][1] + k * c["size"][1] for b in _GEO if b["name"].startswith("weapon_t") for c in b.get("cubes", []) for k in (0, 1)]
BUTT, TIP = min(_WY) - GY, max(_WY) - GY


def S(s):
    return (GX, GY + s, 0.0)


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
if V2C:
    # The knight's v2 footwork under blows (right lead, its own footing), mirrored like the above. A medium blow shifts the
    # pelvis well off the footing and a short lifted step catches it (the rear foot back for a blow from the front, the lead
    # foot forward from behind, the far foot out sideways), then the feet walk home; a light blow only gives at the knees.
    _KH2 = {
        "front": {"hips": [(0, *_KN_HIPS), (2, (K.hb(-6), 9, 0), (0.0, -1.9, 1.6)), (4.5, (K.hb(-5), 9, 0), (0.0, -2.1, 1.9)),
                           (7.5, (1, 5, 0), (0.0, -1.8, 1.3)), (10.5, (K.hb(-1), 7, 0), (0.0, -1.3, 0.5)), (16, *_KN_HIPS)],
                  "steps": [("left", 1.0, 3.8, (3.3, 2.0, -4.0), 9.0, 0.8), ("right", 3.5, 6.5, (-3.4, 2.0, 0.6), -13.0, 0.6),
                            ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
        "back": {"hips": [(0, *_KN_HIPS), (2, (6, 4, 0), (0.0, -1.8, -1.2)), (4.5, (5, 4, 0), (0.0, -1.9, -1.4)),
                          (7.5, (-1, 7, 0), (0.0, -1.6, -0.8)), (10.5, (0.5, 6, 0), (0.0, -1.2, -0.2)), (16, *_KN_HIPS)],
                 "steps": [("right", 1.0, 3.8, (-3.5, 2.0, 3.5), -12.0, 0.8), ("left", 3.5, 6.5, (3.2, 2.0, -0.6), 7.0, 0.6),
                           ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
        "right": {"hips": [(0, *_KN_HIPS), (2, (K.hb(-2), 14, 4), (1.0, -1.8, 0.5)), (4.5, (K.hb(-2), 13, 4), (1.2, -1.9, 0.5)),
                           (7.5, (1, 4, 0), (0.6, -1.6, 0.2)), (10.5, (0, 6.5, 0), (0.1, -1.2, 0.1)), (16, *_KN_HIPS)],
                  "steps": [("left", 1.0, 3.8, (4.9, 2.0, -2.2), 12.0, 0.8), ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8)]},
        "left": {"hips": [(0, *_KN_HIPS), (2, (K.hb(-2), -2, -4), (-1.0, -1.8, 0.5)), (4.5, (K.hb(-2), -1, -4), (-1.2, -1.9, 0.5)),
                          (7.5, (1, 8, 0), (-0.6, -1.6, 0.2)), (10.5, (0, 5.5, 0), (-0.1, -1.2, 0.1)), (16, *_KN_HIPS)],
                 "steps": [("right", 1.0, 3.8, (-5.0, 2.0, 1.6), -18.0, 0.8), ("right", 9.0, 12.5, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
    }
    K.HIT_LEGS = {"front": mirror_legs(_KH2["front"]), "back": mirror_legs(_KH2["back"]),
                  "right": mirror_legs(_KH2["left"]), "left": mirror_legs(_KH2["right"])}
    # a blow taken on the shaft: the arms take it first, the body gives a beat later and less, the feet barely skid
    K.BLOCK_LEGS = mirror_legs({"hips": [(0, *_KN_HIPS), (1.2, (-1, 7, 0), (0.0, -1.3, 0.3)), (3.0, (-3, 8, 0), (0.0, -1.9, 0.75)),
                                         (5.8, (4, 2, 0), (0.0, -1.6, -0.4)), (8, (1, 5, 0), (0.0, -1.2, 0.0)),
                                         (K.BLOCK_TICKS, *_KN_HIPS)],
                                "steps": [("right", 1.4, 3.0, (-3.4, 2.0, 1.3), -13.0, 0.0),
                                          ("right", 6.0, 9.0, (-3.4, 2.0, 2.0), -12.0, 0.6)]})
    # the commands read apart at gameplay scale: brace a step lower and wider than the guard, the line deepest and longest
    STANCE_LEGS["brace"] = {"hips": ((2.0, 24.0, 0.0), (0.0, -2.4, 0.5)), "left": (3.8, 2.0, 3.0), "right": (-3.6, 2.0, -3.0),
                            "yaw": (-10.0, 18.0)}
    STANCE_LEGS["wall"] = {"hips": ((5.0, 28.0, 0.0), (0.0, -3.7, 0.7)), "left": (3.6, 2.0, 4.6), "right": (-3.4, 2.0, -4.3),
                           "yaw": (-10.0, 22.0)}


def stance_legs(spec, life=1.0, ticks=K.STANCE_TICKS, ready=False):
    """The knight's stance legs, mirrored for the left lead: the weight rides forward over the left foot and back over the right."""
    n = ticks * SUB
    out, guesses = [], {}
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)   # ready: everything x V2_READY_SWAY_SCALE; else the excess x V2_SWAY_SCALE
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
# Forearm clearance (2026-11-09): Minecraft forearms are 4 px square and hang inward of their pivot, so a fist held before the
# belly with the elbow at the chest's side face sinks the forearm into the torso (scorecard pen_limb). The spearmen hold the
# guard wide: the rear fist out beside the hip, its arm long, the lead arm reaching forward and out so the forearm passes in
# front of the chest, the hands 10 px apart on the haft, the shaft raised 18 deg. The elbows stay open (under 23 deg), so the
# daze (reactions.py, from the ready arms) stays inside the hit limits. The arm angles are the key's own priors, solved so the
# shaft runs through both fists.
WIDE = TROOP in ("wm_tusk", "tc_tideguard", "tc_admiral")
if WIDE:
    GUARD = KEY((14, 15, 0), GAZE, (-7.43, 12.82, -0.87), aim(18.5, 20.3), Ls=10.1, rp=(-45.88, -20.69, 15.09, -2.02),
                lp=(-117.23, 26.98, -81.1, -22.28))
# Batch review 2026-11-11 (Kort, tusk thrust round 1, tick 0: "hand placement could be spaced out a tad bit more"): a two-hand
# spear guard holds the fists 8.0 px apart along the shaft. The tusk's attack_thrust is the graded demo
# (art/anim/wm_tusk/build_thrust_demo.py), which starts from this guard at 8.0, so its stance takes the same grip and the thrust
# does not slide the left fist on the way out of the stance. The tideguard and the admiral hold the same wide guard at 10.1
# (already wider than his ask), and are left as they are.
GUARD_GRIP = {"wm_tusk": 8.0}
if WIDE and TROOP in GUARD_GRIP:
    GUARD = nudge(GUARD, Ls=GUARD_GRIP[TROOP])
# Spear trio fix round 1 (2026-11-12): Kort approved the tusk's attack_thrust round 3 as it was built, on the pre-wide guard
# (R3_GUARD below). Tried as the tusk's GUARD so the stances match it: idle_upper, stance_ready_upper, stance_shield_up_upper
# and stance_rally_upper then fail scorecard pen_limb (1.92 to 1.98 px, stance limit 1.79: that guard's left forearm sits in
# the chest), so the stances keep this wide guard at 8.0 until the lead decides (a stance allowance or a re-pose); the thrust
# demo starts from R3_GUARD itself.
R3_GUARD = KEY((14, 15, 0), GAZE, (-4.5, 13.5, 2.0), aim(12, 18), Ls=8.0)
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

if V2C:
    # V2: the pelvis drives. The hips load first (by t2 they are going back and open over the rear right foot, full by t4.5,
    # held to t5.6); the arms follow them into the draw and are still drawing as the hips start forward, so the shaft leaves
    # last (the t5.9 key: no one-tick pop off the load). On the impact the hips drive over the lead foot, the torso leaning
    # and turning less than before, and the rear foot catches up behind the lunge (t8.5-11) before both feet walk home.
    # The high thrust coils down deep and springs up; the low sweep ends with the point down at the shins, not level.
    ATTACKS = {
        "thrust": [
            (0, GUARD),
            (2.5, KEY((10, 24, 0), (2, 0, 0), (-5.0, 13.8, -3.0), aim(10, 20), Ls=10.5)),
            (5.0, KEY((8, 28, 0), (2, 0, 0), (-5.4, 13.6, -4.8), aim(10, 22), Ls=12.0)),
            (5.9, KEY((9, 26, 0), (3, 0, 0), (-5.3, 13.6, -4.0), aim(10, 21), Ls=11.3)),
            (7, KEY((26, 5, 0), (7, 0, 0), (-4.0 + RX, 13.2, 10.0), aim(3, 10), Ls=2.5)),
            (9, KEY((29, 6, 0), (7, 0, 0), (-4.2 + RX, 13.4, 11.0), aim(2, 11), Ls=2.0)),
            (12, KEY((19, 10, 0), (5, 0, 0), (-3.5, 14.0, 7.0), aim(8, 14), Ls=4.5)),
            (17, nudge(GUARD, O=(0.2, 0.0, 0.6), torso=(1, -1, 0))),
            (22, GUARD)],
        "high_thrust": [
            (0, GUARD),
            (2.5, KEY((7, 27, 0), (0, 0, 0), (-5.0, 11.6, -3.2), aim(6, 21), Ls=10.5)),
            (5.0, KEY((8, 30, 0), (-3, 0, 0), (-5.2, 9.8, -4.0), aim(4, 22), Ls=11.0)),
            (5.9, KEY((9, 27, 0), (-4, 0, 0), (-5.2, 10.3, -3.4), aim(7, 21), Ls=10.5)),
            (7, KEY((18, 6, 0), (-7, 0, 0), (-4.8, 18.0, 9.6), aim(21, 10), Ls=2.5)),
            (9, KEY((20, 8, 0), (-7, 0, 0), (-5.2, 18.8, 10.4), aim(23, 12), Ls=2.0)),
            (12, KEY((13, 10, 0), (0, 0, 0), (-3.5, 16.5, 6.5), aim(16, 14), Ls=4.5)),
            (17, nudge(GUARD, O=(0.2, 0.3, 0.6), torso=(0, -1, 0))),
            (22, GUARD)],
        "low_sweep": [
            (0, GUARD),
            (3, KEY((12, 28, 0), (8, 8, 0), (-5.0, 13.0, 0.0), aim(5, 36), Ls=7.5)),
            (5.5, KEY((10, 34, 0), (10, 10, 0), (-5.5, 12.8, -1.0), aim(2, 45), Ls=8.0)),
            (6.8, KEY((17, 22, 0), (14, 5, 0), (-6.0, 12.0, 2.0), aim(-4, 30), Ls=6.0)),
            (8, KEY((26, 6, 0), (20, 0, 0), (-6.2, 11.3, 3.8), aim(-8, 10 - 6 * RX), Ls=4.0)),
            (10.5, KEY((27, 6, 0), (20, -4, 0), (-7.2, 11.3, 4.2), aim(-8, 3 - 8 * RX), Ls=4.0)),  # a wider torso turns the point left, the butt out
            (14, KEY((20, 8, 0), (10, 0, 0), (-6.0, 13.0, 3.0), aim(2, 8), Ls=5.0)),
            (18, nudge(GUARD, O=(0.2, 0.0, 0.5), torso=(1, -2, 0))),
            (22, GUARD)],
    }
    _HOME = [("left", 13.0, 16.5, (3.4, 2.0, 2.4), 14.0, 1.0), ("right", 16.0, 19.5, (-3.2, 2.0, -2.2), -6.0, 0.8)]
    ATTACK_LEGS = {
        "thrust": {"hips": [(0, *READY_HIPS), (2, (-2, 26, -0.5), (-0.3, -1.5, 0.7)), (4.5, (-4, 33, -1.5), (-0.6, -1.9, 1.4)),
                            (5.6, (-4, 33, -1.5), (-0.5, -2.0, 1.3)), (7, (7, 6, 1), (0.4, -3.0, -2.8)),
                            (9, (8, 4, 1), (0.5, -3.2, -3.1)), (12, (4, 10, 0), (0.2, -2.5, -2.2)),
                            (16.5, (1, 17, 0), (0.0, -1.6, -0.6)), (22, *READY_HIPS)],
                   "steps": [("left", 4.6, 6.8, (3.6, 2.0, 6.8), 12.0, 1.4), ("right", 8.5, 11.0, (-3.1, 2.0, 0.4), -8.0, 1.0)] + _HOME},
        "high_thrust": {"hips": [(0, *READY_HIPS), (2, (-1, 26, -0.5), (0.0, -1.8, 0.6)), (4.5, (-3, 32, -1.5), (-0.3, -3.0, 1.1)),
                                 (5.6, (-3, 32, -1.5), (-0.4, -3.4, 1.0)), (7, (1, 6, 1), (0.2, -1.7, -2.2)),
                                 (9, (2, 5, 1), (0.3, -1.6, -2.5)), (12, (1, 11, 0), (0.1, -1.5, -1.8)),
                                 (16.5, (0, 18, 0), (0.0, -1.4, -0.5)), (22, *READY_HIPS)],
                        "steps": [("left", 4.6, 6.8, (3.6, 2.0, 6.2), 12.0, 1.3), ("right", 8.5, 11.0, (-3.1, 2.0, 0.2), -8.0, 1.0)] + _HOME},
        "low_sweep": {"hips": [(0, *READY_HIPS), (2.5, (1, 27, -0.5), (-0.3, -1.8, 0.4)), (5.5, (4, 33, -1.5), (-0.4, -2.3, 0.5)),
                               (8, (10, 4, 1), (0.0, -3.1, -0.5)), (10.5, (11, 2, 1), (-0.1, -3.2, -0.6)), (14, (6, 6, 0), (0.0, -2.4, -0.3)),
                               (18, (1, 16, 0), (0.0, -1.5, 0.0)), (22, *READY_HIPS)],
                      "steps": ATTACK_LEGS["low_sweep"]["steps"]},
    }

# Spear rollout 2 (Kort, 2026-11-09: the slash recipe on the low sweep). The old sweep swung the point to the right while the
# hips and torso unwound to the left, so the wrists did twice the arc against the body. Now the point winds up out past the
# rear (right) hip and sweeps left with the body: the hips lead the unwind by about a tick, the torso follows and the
# shoulder carries the fist forward into an extended contact, the point sweeps on past the foe and the body settles back.
# The grip is planned in the chest's frame: the shaft never points right of the chest's own forward (r, degrees left of it,
# stays >= 16), and the rear fist sits out beside the hip on the load, so the 14 px butt passes outside the right hip
# instead of across the belly. The wrist rolls the blade flat (phi 90) through the sweep so its edge, not its flat, leads.
# Keys are planned as (chest pose, fist in the chest frame, world elevation, chest-relative yaw r, roll phi) and baked every
# quarter tick into the solver's world keys, so the interpolation follows the body's arc instead of cutting a chord through it.
# Impact stays on t8.
if V2C:
    def _chest(legs_i, torso):
        T = K.world(fk_pose({"torso": (tuple(torso), None)}, legs_i), "torso")
        R = T[:3, :3] @ K.world({}, "torso")[:3, :3].T
        f = R @ np.array([0.0, 0.0, 1.0])
        return T, R, math.degrees(math.atan2(f[0], f[2]))

    def _to_chest(T, O):
        v = np.linalg.inv(T) @ np.r_[np.asarray(O) / 16.0, 1.0]
        return (-v[0] * 16.0, v[1] * 16.0, -v[2] * 16.0)

    def _sweep_keys(plan, legs_spec, step=0.25):
        legs = K.attack_legs(legs_spec)
        T0, _, cy0 = _chest(legs[0], GUARD["torso"])
        d0 = np.array(GUARD["d"])
        g = {"torso": GUARD["torso"], "head": GUARD["head"], "O": _to_chest(T0, GUARD["O"]),
             "ev": (math.degrees(math.asin(d0[1])),), "r": (math.degrees(math.atan2(d0[0], d0[2])) - cy0,), "phi": (0.0,),
             "Ls": GUARD["Ls"], "rp": GUARD["rp"], "lp": GUARD["lp"]}
        rows = [(0.0, g)] + [(t, {**g, **{k: (tuple(v) if isinstance(v, (tuple, list)) else (v,)) for k, v in kv.items()}})
                             for t, kv in plan] + [(22.0, g)]
        out = []
        for t in np.arange(0.0, 22.0 + 1e-9, step):
            if 0.0 < t < plan[0][0] or plan[-1][0] < t < 22.0:
                continue                      # off the planned span: the guard ends ease in and out on their own keys
            v = {n: K.curve([(k[0], k[1][n]) for k in rows], t) for n in g}
            if SWEEP_PHI:                     # the blade's roll on its own track (batch review: never in the wind-up)
                v["phi"] = K.curve(SWEEP_PHI, t)
            out.append((float(t), _plan_key(legs[int(round(t * SUB))], v)))
        out[0], out[-1] = (0.0, GUARD), (22.0, GUARD)
        return out

    def _plan_key(legs_i, v):
        T, R, cy = _chest(legs_i, v["torso"])
        d = aim(v["ev"][0], cy + v["r"][0])
        ph = math.radians(v["phi"][0])
        up = np.array([0.0, 1.0, 0.0]) - d[1] * d
        u = math.cos(ph) * level_u(d) + math.sin(ph) * unit(up)
        o = v["O"]
        k = KEY(v["torso"], v["head"], K.point(T, (o[0], o[1], -o[2])), d, Ls=v["Ls"][0], rp=v["rp"], lp=v["lp"])
        k["u"] = tuple(unit(u))
        return k

    SWEEP_PHI = None
    # chest frame px: +x the troop's left, +y up, +z forward. r: shaft yaw left of the chest's forward. ev: world elevation.
    SWEEP_PLAN = [
        (3.0, {"torso": (12, 26, 0), "head": (8, 6, 0), "O": (-7.0, 16.0, -1.0), "ev": 2, "r": 32, "phi": 60, "Ls": 8.5}),
        (5.5, {"torso": (10, 34, 0), "head": (10, 8, 0), "O": (-6.5, 18.5, -1.0), "ev": -3, "r": 33, "phi": 90, "Ls": 8.0}),
        (6.5, {"torso": (13, 31, 0), "head": (13, 5, 0), "O": (-6.5, 16.8, -1.0), "ev": -6, "r": 33, "phi": 90, "Ls": 8.0}),
        (7.25, {"torso": (18, 24, 0), "head": (17, 4, 0), "O": (-6.6, 15.6, 2.0), "ev": -8, "r": 31, "phi": 90, "Ls": 6.5}),
        (8.0, {"torso": (24, 14, 0), "head": (20, 1, 0), "O": (-4.9, 15.0, 4.8), "ev": -9, "r": 33, "phi": 90, "Ls": 4.0}),
        (9.25, {"torso": (27, 4, 0), "head": (20, -3, 0), "O": (-4.3, 14.6, 5.5), "ev": -9, "r": 38, "phi": 90, "Ls": 5.0}),
        (10.5, {"torso": (26, -2, 0), "head": (19, -4, 0), "O": (-3.8, 14.8, 5.0), "ev": -8, "r": 42, "phi": 85, "Ls": 4.0}),
        (14.0, {"torso": (20, 6, 0), "head": (10, 0, 0), "O": (-4.0, 15.5, 3.0), "ev": 2, "r": 45, "phi": 40, "Ls": 6.0}),
        (18.0, {"torso": (15, 13, 0), "head": (5, 0, 0), "phi": 5}),
    ]
    ATTACK_LEGS["low_sweep"] = {
        "hips": [(0, *READY_HIPS), (2.5, (1, 27, -0.5), (-0.3, -1.8, 0.4)), (5.5, (4, 33, -1.5), (-0.4, -2.3, 0.5)),
                 (7.0, (7, 16, 0.5), (-0.1, -2.8, -0.2)), (8, (10, 8, 1), (0.0, -3.1, -0.5)), (10.5, (11, -1, 1), (-0.1, -3.2, -0.6)),
                 (14, (6, 6, 0), (0.0, -2.4, -0.3)), (18, (1, 16, 0), (0.0, -1.5, 0.0)), (22, *READY_HIPS)],
        "steps": ATTACK_LEGS["low_sweep"]["steps"]}
    ATTACKS["low_sweep"] = _sweep_keys(SWEEP_PLAN, ATTACK_LEGS["low_sweep"])

# The wide guard's thrusts (forearm clearance, 2026-11-09): each key's arms refitted so both forearms stay out of the chest and
# hips (the draw holds the rear fist out beside the hip, not against it; the drive carries the lead elbow out in front), the
# fists and shaft within about 2 px and 6 deg of the old keys. Timing, torso and head unchanged; t17 and t22 ride the guard.
# In-between keys (t6.4 and t6.7 on the drive, t13.5 and t15 on the way home) hold the elbows out through the blends; their torso
# and head are the curve's own values there.
if WIDE:
    def _refit(name, keys):
        ATTACKS[name] = sorted({**dict(ATTACKS[name]), **keys}.items())

    _refit("thrust", {
        2.5: KEY((10, 24, 0), (2, 0, 0), (-7.14, 13.29, -3.96), aim(10.82, 21.82), Ls=13.26, rp=(-4.35, -14.09, 12.01, -43.92), lp=(-18.8, 0, 0, -71.2)),
        5.0: KEY((8, 28, 0), (2, 0, 0), (-7.17, 13.21, -5.25), aim(11.19, 14.84), Ls=12.97, rp=(-8.48, -15.86, 13.86, -45.32), lp=(-19.68, 0, 0, -68.95)),
        5.9: KEY((9, 26, 0), (3, 0, 0), (-7.01, 12.97, -4.22), aim(8.35, 18.27), Ls=13.02, rp=(1.87, -17.51, 16.65, -53.64), lp=(-20.14, 0.01, 8.5, -67.79)),
        6.4: KEY((16.28, 16.35, 0), (4.98, 0, 0), (-7.16, 12.52, 1.92), aim(7.17, 27.19), Ls=10.05, rp=(1.09, -28.02, 19.49, -65.98), lp=(-49.05, 25.42, 0, -42.25)),
        6.7: KEY((22.05, 9.24, 0), (6.26, 0, 0), (-4.67, 12.73, 8.83), aim(11.92, 34.13), Ls=4.96, rp=(-39.06, -27.4, -0.01, -50.94), lp=(-70.62, 32.06, 2.22, -24.18)),
        7.0: KEY((26, 5, 0), (7, 0, 0), (-1.73 + RX, 12.56, 10.71), aim(5.24, 20.45), Ls=2.77, rp=(-55.81, -38.12, 0, -34.19), lp=(-84.09, 36.83, -3.64, -5.84)),
        9.0: KEY((29, 6, 0), (7, 0, 0), (-1.33 + RX, 12.26, 11.71), aim(2.91, 17.06), Ls=2.48, rp=(-63.76, -38.89, 0, -29.37), lp=(-87.85, 36.87, 0.57, -2.15)),
        12.0: KEY((19, 10, 0), (5, 0, 0), (-3.77, 14.34, 9.19), aim(10.73, 18.72), Ls=3.21, rp=(-54.84, -37.19, 0, -35.16), lp=(-85.05, 33.95, -2.33, -2.39)),
        13.5: KEY((17.24, 11.51, 0), (4.55, 0, 0), (-5.51, 14.4, 7.26), aim(17.26, 27.82), Ls=5.18, rp=(-40.96, -27.29, 0, -48.21), lp=(-86.4, 30.7, -2.36, -2.08)),
        15.0: KEY((16.05, 12.79, 0), (4.23, 0, 0), (-7.38, 12.77, 2.69), aim(19.67, 26.07), Ls=8.78, rp=(-19.84, -19.33, 13.3, -36.63), lp=(-79, 31.97, 0, -2)),
        14.25: KEY((16.59, 12.19, 0), (4.37, 0, 0), (-7.03, 13.5, 5.17), aim(20.37, 31.20), Ls=7.20, rp=(-32.98, -22.56, 14.24, -43.48), lp=(-83.79, 31.18, -1.23, -2.01)),
    })
    _refit("high_thrust", {
        2.5: KEY((7, 27, 0), (0, 0, 0), (-6.62, 12.53, -3.71), aim(8.58, 19.39), Ls=13.13, rp=(-17.73, -23.42, 14.04, -18.44), lp=(-25.94, -1.43, -1.28, -50.77)),
        5.0: KEY((8, 30, 0), (-3, 0, 0), (-6.83, 11.36, -4.5), aim(5.82, 15.17), Ls=12.59, rp=(-21.29, -24.85, 15.17, -20.59), lp=(-26.8, -6.24, 4.75, -43.37)),
        5.9: KEY((9, 27, 0), (-4, 0, 0), (-6.59, 11.12, -3.7), aim(5.77, 19.23), Ls=12.94, rp=(-15.97, -22.94, 12.85, -13.94), lp=(-28.14, -8.59, 10.6, -41.25)),
        6.4: KEY((12.93, 17.1, 0), (-5.53, 0, 0), (-7.17, 12.53, 2.4), aim(15.91, 30.33), Ls=9.13, rp=(-22.73, -25.21, 15.12, -28.94), lp=(-50.27, 22.93, 8.7, -32.28)),
        6.7: KEY((15.92, 10.11, 0), (-6.47, 0, 0), (-4.1, 15.49, 8.17), aim(21.37, 30.08), Ls=4.44, rp=(-46.76, -32.74, -0.05, -43.24), lp=(-71.4, 30.39, 13.01, -18.6)),
        7.0: KEY((18, 6, 0), (-7, 0, 0), (-1.69 + RX, 16.48, 9.85), aim(20.34, 17.40), Ls=2.61, rp=(-64.45, -39.3, 0, -29.6), lp=(-80.42, 35.91, 9.53, -9.58)),
        9.0: KEY((20, 8, 0), (-7, 0, 0), (-1.90 + RX, 17.37, 10.91), aim(22.68, 18.81), Ls=2.48, rp=(-73.4, -38.52, 0, -31.81), lp=(-90.82, 37.62, 7.42, -8.04)),
        12.0: KEY((13, 10, 0), (0, 0, 0), (-3.7, 16.64, 8.3), aim(18.11, 17.86), Ls=3.28, rp=(-57.04, -38.16, 0, -32.96), lp=(-83.96, 32.71, 0.96, -3.5)),
        13.5: KEY((13.22, 11.29, 0), (1.78, 0, 0), (-5.24, 16.01, 6.79), aim(20.65, 26.65), Ls=4.89, rp=(-43.55, -29.9, 0.01, -46.35), lp=(-84.79, 30.15, 1.17, -2.46)),
        15.0: KEY((13.61, 12.64, 0), (3.07, 0, 0), (-7.35, 13.43, 2.41), aim(21.60, 26.61), Ls=8.70, rp=(-21.23, -21.16, 14.18, -34.96), lp=(-78.13, 31.47, 0, -2)),
        14.25: KEY((13.41, 11.98, 0), (2.5, 0, 0), (-6.9, 14.6, 4.85), aim(22.93, 31.76), Ls=6.97, rp=(-35.21, -24.92, 15.54, -42.25), lp=(-83.22, 30.89, -1.83, -2.49)),
    })
    # The low sweep on the wide guard: the same chest-frame plan, each row's fists and arms refitted for forearm clearance (the
    # shaft swings a little higher, near level, so the rear forearm passes outside the hip rather than through it).
    SWEEP_PLAN = [
        (3.0, {"torso": (12, 26, 0), "head": (8, 6, 0), "O": (-6.87, 16.31, -0.74), "ev": 5.13, "r": 37.8, "phi": 60, "Ls": 11.53, "rp": (31.05, -11.02, 6.98, -83.8), "lp": (-92.23, 37.95, -0.08, -4.26)}),
        (5.5, {"torso": (10, 34, 0), "head": (10, 8, 0), "O": (-6.88, 17.64, -0.69), "ev": -1.08, "r": 38.23, "phi": 90, "Ls": 11.06, "rp": (44.9, -10.21, 9.44, -108.06), "lp": (-92.85, 39.52, -0.04, -6.89)}),
        (6.5, {"torso": (13, 31, 0), "head": (13, 5, 0), "O": (-6.85, 17.28, -0.7), "ev": -3.38, "r": 38.6, "phi": 90, "Ls": 11.18, "rp": (41.28, -9.85, 8.43, -102.2), "lp": (-92.02, 38.95, 0, -7.11)}),
        (7.25, {"torso": (18, 24, 0), "head": (17, 4, 0), "O": (-6.73, 16.78, 1.19), "ev": -1.95, "r": 43.87, "phi": 90, "Ls": 9.81, "rp": (16.29, -16.16, 11.04, -92.35), "lp": (-93.32, 39.7, 0, -6.66)}),
        (8.0, {"torso": (24, 14, 0), "head": (20, 1, 0), "O": (-5.42, 16.86, 3.66), "ev": -0.09, "r": 48.23, "phi": 90, "Ls": 7.33, "rp": (-19.21, -37.36, 21.96, -77.37), "lp": (-93.26, 39.82, 0, -6.81)}),
        (9.25, {"torso": (27, 4, 0), "head": (20, -3, 0), "O": (-5.03, 16.63, 4.28), "ev": 0.19, "r": 49.5, "phi": 90, "Ls": 6.91, "rp": (-40.47, -49.04, 39.66, -63.68), "lp": (-90.99, 38.19, 0.01, -7.33)}),
        (10.5, {"torso": (26, -2, 0), "head": (19, -4, 0), "O": (-4.51, 16.79, 4.68), "ev": 0.43, "r": 50.22, "phi": 85, "Ls": 6.28, "rp": (-58.37, -52.96, 54.84, -59.39), "lp": (-92.31, 37.21, 0.02, -2.17)}),
        (14.0, {"torso": (20, 6, 0), "head": (10, 0, 0), "O": (-4.98, 16.38, 4.44), "ev": 7.05, "r": 52.25, "phi": 40, "Ls": 7.05, "rp": (-51.5, -53.23, 49.26, -55.03), "lp": (-89.58, 35.8, 0, -4.55)}),
        (18.0, {"torso": (15, 13, 0), "head": (5, 0, 0), "O": (-4.18, 18.29, 5.75), "ev": 15.86, "r": 56.98, "phi": 5, "Ls": 7.26, "rp": (-64.83, -39.47, 52.56, -63.13), "lp": (-98.36, 26.7, 0, -12.49)}),
    ]
    # Batch review 2026-11-11 (grip_roll 24 deg/tick at t2.25 on all three: the plan rolled the blade 60 deg by t3, in the wind-up).
    # Kort: "weapons spins around in hand". The blade stays as the guard holds it through the wind-up and rolls its edge onto the
    # sweep only as the strike starts (t5 to t7.5); the rest of the roll is the plan's own.
    SWEEP_PHI = [(0.0, (0.0,)), (5.0, (0.0,)), (7.5, (90.0,)), (8.0, (90.0,)), (9.25, (90.0,)), (10.5, (85.0,)), (14.0, (40.0,)),
                 (18.0, (5.0,)), (22.0, (0.0,))]
    ATTACKS["low_sweep"] = _sweep_keys(SWEEP_PLAN, ATTACK_LEGS["low_sweep"])
    if TROOP == "tc_admiral":   # the admiral's rig (its wider cuffs) wants its own refit of the same plan
        SWEEP_PLAN = [
            (3.0, {"torso": (12, 26, 0), "head": (8, 6, 0), "O": (-7.02, 16.45, -0.65), "ev": 6.55, "r": 39.07, "phi": 60, "Ls": 12.46, "rp": (51.46, -11.98, 8.67, -85.8), "lp": (-83.77, 34.75, -15.59, -3.87)}),
            (5.5, {"torso": (10, 34, 0), "head": (10, 8, 0), "O": (-7.04, 17.41, -0.57), "ev": 0.49, "r": 40.42, "phi": 90, "Ls": 12.12, "rp": (60.95, -11.99, 10.79, -103.49), "lp": (-84.35, 35.11, -17.07, -3.68)}),
            (6.5, {"torso": (13, 31, 0), "head": (13, 5, 0), "O": (-7.03, 17.38, -0.62), "ev": -2.39, "r": 40.36, "phi": 90, "Ls": 12.15, "rp": (61.23, -11.97, 10.48, -102.96), "lp": (-85.15, 35.13, -16.34, -2.91)}),
            (7.25, {"torso": (18, 24, 0), "head": (17, 4, 0), "O": (-6.97, 17.05, 1.09), "ev": -2.67, "r": 45.23, "phi": 90, "Ls": 10.92, "rp": (38.51, -13.28, 13.38, -96.11), "lp": (-85.5, 35.66, -15.23, -3.14)}),
            (8.0, {"torso": (24, 14, 0), "head": (20, 1, 0), "O": (-6.06, 17.41, 3.5), "ev": -0.28, "r": 49.46, "phi": 90, "Ls": 8.79, "rp": (7.39, -27.79, 21.74, -87.41), "lp": (-86.58, 35.62, -8.1, -2)}),
            (9.25, {"torso": (27, 4, 0), "head": (20, -3, 0), "O": (-5.56, 17.21, 4.17), "ev": -0.67, "r": 50.39, "phi": 90, "Ls": 8.02, "rp": (-9.11, -40.23, 34.02, -76.13), "lp": (-83.64, 35.69, -11.67, -5.03)}),
            (10.5, {"torso": (26, -2, 0), "head": (19, -4, 0), "O": (-5.03, 17.37, 4.64), "ev": 0.49, "r": 50.79, "phi": 85, "Ls": 7.38, "rp": (-24.29, -45.72, 46.33, -71.88), "lp": (-83.79, 35.1, -13.57, -4.24)}),
            (14.0, {"torso": (20, 6, 0), "head": (10, 0, 0), "O": (-5.23, 16.89, 4.59), "ev": 7.7, "r": 52.49, "phi": 40, "Ls": 7.8, "rp": (-27.12, -48.83, 49.3, -63.58), "lp": (-83.84, 34.63, -17.91, -3.68)}),
            (18.0, {"torso": (15, 13, 0), "head": (5, 0, 0), "O": (-4.38, 18.25, 5.59), "ev": 15.41, "r": 56.1, "phi": 5, "Ls": 7.17, "rp": (-39.85, -39.24, 50.08, -66.67), "lp": (-87.69, 29.12, -16.3, -6.35)}),
        ]
        ATTACKS["low_sweep"] = _sweep_keys(SWEEP_PLAN, ATTACK_LEGS["low_sweep"])


def gear(samples, legs=None):
    """Secondary motion (STYLE.md rule 9): the knight's cape lag, scaled onto each hanging piece of the troop's gear."""
    K.cape_lag(samples, legs)
    for s in samples:
        c = s.pop("cape")[0]
        for g, k in ST["gear"].items():
            s[g] = ((k * c[0], 0.0, k * c[2]), None)
    return samples


# ------------------------------------------------------------------------------------------------
# Batch review 2026-11-11 (Kort's four focus points: no clipping, nothing stale, the weapon facing the right way, no limb
# glitches). For the three spear troops only (REVIEW): ic_breaker and the reaper borrow this builder and stay as they were.
#   the whole unit  every hanging piece and shoulder plate swings in the attacks and the heavier hits (fullbody.soft_motion; the
#                   tusk's kilt, trophy, hip skull, tooth necklace and pauldrons stood still; the tideguard's pauldrons and
#                   tabard; the admiral's hair, kelp, plume, tails and sash)
#   the ground      the long shaft dipped under the ground in the hits that throw the chest forward (tusk: 11 px in
#                   hit_medium_back and hit_pulled): where it would, both arms lift it about the chest's own cross axis, which
#                   keeps the shaft in both fists; a foot that sinks lifts the hips by what it sinks
# The clips the shared modules add later (stances.py, charge.py, reactions.py) get the same passes from `--post` (main()).
# ------------------------------------------------------------------------------------------------

REVIEW = WIDE
FLOOR_CLEAR = 0.6       # px the weapon keeps above the sole (focus_check fails at 1.0 under, warns at 0.5)
FEET_SINK = 0.3         # px a foot may press under the sole (a toe dug in) before the hips lift it
_FB = []


def _fullbody():
    if not _FB:
        sys.path.insert(0, str(HERE.parent / "common"))
        import fullbody   # noqa: E402  (imports this troop's knight-builder module, already loaded as K)
        _FB.append(fullbody)
    return _FB[0]


def soft_parts():
    """The kit's pieces for this rig, every one keyed in the clip it is given (the upper clip: it plays whatever the legs do)."""
    parts = _fullbody().soft_parts_for()
    for c in parts.values():
        c.pop("clip", None)
        if c.get("kind") == "plume":
            # the admiral's hair locks and plumes on the kit's lively spring snapped 16 to 23 deg/tick in the blows (focus_check
            # pop, fail 14): damped harder and swung less, they still trail the head well past secondary_life's 4 deg
            c.update(zeta=0.45, gain=1.6)
    return parts


def soft(upper, legs, label, parts=None):
    print(label + ": soft motion")
    upper, legs = _fullbody().soft_motion(upper, legs, parts=parts if parts is not None else soft_parts(), report=True)
    # The plates on one arm are one piece of armour (the tusk's pauldron and the spikes riveted to it): where the kit slides
    # one of them down the arm, off the helm, all of them slide as far, so a spike never leaves its plate.
    groups = {}
    for plate, (arm, _) in _fullbody().plates_for().items():
        groups.setdefault(arm, []).append(plate)
    for u in upper:
        for arm, plates in groups.items():
            ys = [u[p][1][1] for p in plates if p in u and u[p][1] is not None]
            if ys and len(plates) > 1:
                y = min(ys)
                for p in plates:
                    if p in u:
                        u[p] = (u[p][0], (0.0, y, 0.0))
    return upper, legs


PLATE_FIT = 1.5     # px a shoulder plate may sit deeper in the helm than the rest model has it (focus_check fails at 1.9)
PLATE_STEP = 0.375  # px between the slides tried
PLATE_REACH = 3.0   # px a plate slides down its arm at most
ARMOUR_FAIL = 1.9   # focus_check's armour_clip FAIL: a pass only touches a clip that reaches it
INHERIT = 0.15      # focus_check: an action clip within this of its first frame only inherits its stance's fit
CLOTH_FIT = 1.5     # px the hanging cloth may sit deeper in a leg than the rest model has it
CLOTH_TURN = 30     # deg the cloth turns at most to let the thigh by
_ACTX = []


def _actx():
    """The full tier-3 model's cubes by bone, which pieces are alive, for the passes below (they judge bone against bone,
    each pair against its own rest-model fit, as focus_check judges armour_clip)."""
    if not _ACTX:
        fb = _fullbody()
        import focus_check as fc
        model = fb._full_model(TROOP)
        alive = fc.alive_pieces(model)
        by = {}
        for ci, c in enumerate(model.cube_list):
            by.setdefault(model.cube_bone[ci], []).append(c)
        _ACTX.append((fb, fc, model, alive, by))
    return _ACTX[0]


def _posed(u, l):
    pose = {}
    for src in (l, u):
        for b, (r, p) in src.items():
            if b in K.MODEL.bones:
                pose[b] = (r, p if p is not None else Z3)
    return pose


def _wcubes(pose, bones):
    fb, _, model, _, by = _actx()
    return {b: [fb._cube_world(fb._bone_world(pose, b, model), c) for c in by[b]] for b in bones if b in by}


class _Fit:
    """How deep the `mine` bones sit in the `others` bones, beyond the rest model's own fit, pair by pair."""

    def __init__(self, mine, others):
        fb = _actx()[0]
        R0m, R0o = _wcubes({}, mine), _wcubes({}, others)
        self.rest = {(a, b): fb._sink(R0m[a], R0o[b]) for a in R0m for b in R0o}

    def deep(self, Wm, Wo, pairs=None):
        fb = _actx()[0]
        best, rows = 0.0, []
        for (a, b), r in self.rest.items():
            if pairs is not None and (a, b) not in pairs:
                continue
            d = fb._sink(Wm[a], Wo[b]) - r
            rows.append(((a, b), d))
            best = max(best, d)
        return best, rows


def _fit_target(deep, loop):
    """The depth a pass works down to, or None where the clip does not fail: a held loop is judged on its own; an action
    clip fails only past its first frame's fit (what it inherits from the stance) plus focus_check's allowance."""
    top = max(deep)
    if loop:
        return PLATE_FIT if top >= ARMOUR_FAIL else None
    if top < ARMOUR_FAIL or top <= deep[0] + INHERIT:
        return None
    return max(PLATE_FIT, deep[0] + 0.1)


def _ease(W, loop, absmax=False):
    """Widen each sample's need over a tick each side, smooth it, and (an action clip) fade it in over a tick and out over
    two, so the clip starts and ends exactly where its stance has it."""
    n = len(W)
    idx = (lambda j: j % (n - 1)) if loop else (lambda j: min(max(j, 0), n - 1))
    pick = (lambda xs: max(xs, key=abs)) if absmax else max
    wide = np.array([pick([W[idx(j)] for j in range(i - SUB, i + SUB + 1)]) for i in range(n)])
    out = np.array([np.mean([wide[idx(j)] for j in range(i - SUB // 2, i + SUB // 2 + 1)]) for i in range(n)])
    if not loop:
        out *= np.array([K.smooth(max(0.0, min(1.0, i / float(SUB), (n - 1 - i) / (2.0 * SUB)))) for i in range(n)])
    return out


def plate_clear(upper, legs, label, loop=False):
    """Where the helm (or a breastplate) would sink into a shoulder piece deeper than the rest model has it, that arm's
    pieces (the plate, the spikes riveted to it, an arm band) slide down the arm together until it fits. Judged pair by
    pair against the rest model (a held stance whose every frame clips is fixed too); an action clip only past what it
    inherits from its first frame. Only a clip that fails is touched."""
    upper = [dict(u) for u in upper]     # a loop's last sample may be its first one's dict
    fb, fc, model, alive, _ = _actx()
    if "head" not in model.bones:
        return upper
    hard = lambda b: b not in alive or alive[b][0] == "plate"
    arms = set(fb._under(model, "right_arm")) | set(fb._under(model, "left_arm"))
    chest = [b for b in model.order if model.bones[b].parent == "torso" and b not in arms and b != "head" and b not in alive]
    helm = [b for b in fb._under(model, "head") if b not in alive] + chest + ["torso"]
    groups = {}
    for b in model.order:     # the shoulder pieces worn on each upper arm: plates, epaulettes, arm bands
        par = model.bones[b].parent
        if par in ("right_arm", "left_arm") and (hard(b) or "fringe" in b) and b in GEO_BONES and (
                set(b.split("_")) & {"pauldron", "plate", "epaulette", "fringe"} or "arm_band" in b):
            groups.setdefault(par, []).append(b)
    n = len(upper)
    for arm, plates in groups.items():
        mine = [x for p in plates for x in fb._under(model, p)]
        F = _Fit(mine, helm)
        poses = [_posed(upper[i], legs[i]) for i in range(n)]
        Ws = [(_wcubes(p, mine), _wcubes(p, helm)) for p in poses]
        deep = [F.deep(Wm, Wo)[0] for Wm, Wo in Ws]
        fit = _fit_target(deep, loop)
        if fit is None:
            continue           # nothing fails here: the clip stays exactly as it was
        W = np.zeros(n)
        for i in range(n):
            if deep[i] <= fit:
                continue
            Wm, Wo = Ws[i]
            # every pair judged at each step: a slide down an arm held forward can carry the plate off the helm and
            # into the breastplate. The first step that fits, else the step that leaves the least (if it helps at all).
            down = fb._unit(K.world(poses[i], arm)[:3, :3] @ np.array([0.0, -1.0, 0.0]))
            best = (deep[i] - 0.05, 0.0)
            for s in np.arange(PLATE_STEP, PLATE_REACH + 1e-6, PLATE_STEP):
                moved = {b: [c + down * s / 16.0 for c in cs] for b, cs in Wm.items()}
                d = F.deep(moved, Wo)[0]
                if d <= fit:
                    best = (d, s)
                    break
                if d < best[0]:
                    best = (d, s)
            W[i] = best[1]
        if not W.any():
            continue
        sw = _ease(W, loop)
        for i in range(n):
            for p in plates:      # every sample keyed, so the slide eases in and out between keys
                r, pos = upper[i].get(p, (Z3, None))
                upper[i][p] = (r, (0.0, (pos or Z3)[1] - float(sw[i]), 0.0))   # on top of any slide the kit gave it
        k = int(np.argmax(sw))
        print("  %s: %s slide %.1f px down %s, off the helm (most at t%.2f; fit %.2f -> %.2f)"
              % (label, "+".join(plates), sw[k], arm, k / SUB, max(deep), fit))
    return upper


def cloth_clear(upper, legs, label, loop=False, into_upper=False):
    """Where a stepping thigh would pass through the cloth hanging over it (a tabard, a kilt, a sash end) deeper than the
    rest model has it, the cloth swings out of the way: the smallest turn about its own hinge (forward or back) that lets
    the leg by, eased over a tick each side. Judged like plate_clear; only a clip that fails is touched."""
    upper = [dict(u) for u in upper]     # a loop's last sample may be its first one's dict
    fb, fc, model, alive, _ = _actx()
    legset = [b for side in ("right_leg", "left_leg") if side in model.bones for b in fb._under(model, side) if b not in alive]
    cloths = [b for b in model.order if b in alive and alive[b][0] == "cloth" and b in K.MODEL.bones
              and model.bones[b].parent in ("torso", "hips")]
    n = len(upper)
    legs = [dict(l) for l in legs]
    for cl in cloths:
        mine = fb._under(model, cl)
        F = _Fit(mine, legset)
        poses = [_posed(upper[i], legs[i]) for i in range(n)]
        Wo = [_wcubes(p, legset) for p in poses]
        deep = [F.deep(_wcubes(p, mine), o)[0] for p, o in zip(poses, Wo)]
        fit = _fit_target(deep, loop)
        if fit is None:
            continue
        D = np.zeros(n)
        for i in range(n):
            if deep[i] <= fit:
                continue
            r0 = poses[i].get(cl, (Z3, Z3))
            for mag in range(2, CLOTH_TURN + 1, 2):
                ok = []
                for sgn in (1, -1):
                    q = dict(poses[i])
                    q[cl] = ((r0[0][0] + sgn * mag, r0[0][1], r0[0][2]), r0[1])
                    if F.deep(_wcubes(q, mine), Wo[i])[0] <= fit:
                        ok.append(sgn * mag)
                if ok:
                    D[i] = ok[0]
                    break
        if not D.any():
            continue
        sd = _ease(D, loop, absmax=True)
        src = legs if cl in legs[0] and cl not in upper[0] and not into_upper else upper
        for i in range(n):
            r, pos = src[i].get(cl, (Z3, None))
            src[i][cl] = ((r[0] + float(sd[i]), r[1], r[2]), pos)
        k = int(np.argmax(np.abs(sd)))
        print("  %s: %s swings %.0f deg out of the thigh's way (most at t%.2f; fit %.2f -> %.2f)"
              % (label, cl, sd[k], k / SUB, max(deep), fit))
    return upper, legs


POP_AT = 12.0       # deg: a sample's turn this much over both neighbours' (focus_check pop warns at 8, fails at 14)
POP_BONES = ("right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand")


LIMB_SLACK = 0.05   # px deeper in the torso than the solve itself that a despiked sample may put a forearm or hand
# Where the guarded despike is used (fix round 1, measured on the admiral): high_thrust spreads half way, pen_limb 1.585 ->
# 1.324 and no pop. The admiral's thrust keeps the full spread (1.405): half way read 1.441 and popped the left fringe (14.0
# at t7), and no spread at all leaves the 23.4 deg arm pop (focus FAIL) that 51afd332 had. Tusk and tideguard are unchanged.
DESPIKE_GUARDED = {("tc_admiral", "high_thrust")}
LIMB_OK = 1.45     # px a despiked sample may reach in any case (sampled before the soft and armour passes; scorecard pen_limb knight attack limit 1.391)
_LIMBFIT = []


def _limb_depth(u, l):
    """px the forearms and hands (with everything on them but the weapon) reach into the torso and hips, measured as the
    scorecard measures pen_limb: a lattice over each limb cube, depth counted from the face the limb comes in through
    (the side its elbow is on)."""
    fb, _, model, _, by = _actx()
    if not _LIMBFIT:
        sys.path.insert(0, str(HERE.parents[2] / "tools" / "anim"))
        import scorecard as sc   # noqa: E402

        def root_of(b):
            while b:
                if b in ("right_forearm", "left_forearm"):
                    return b
                b = model.bones[b].parent if b in model.bones else None
            return None
        limbs = [(b, root_of(b)) for b in by if root_of(b) and not is_weapon(b)]
        core = [b for b in by if b in ("torso", "hips", "body", "chest", "waist")]
        _LIMBFIT.append((sc, limbs, core))
    sc, limbs, core = _LIMBFIT[0]
    pose = _posed(u, l)
    pts, roots = [], []
    for b, r in limbs:
        M = fb._bone_world(pose, b, model)
        Mr = fb._bone_world(pose, r, model)
        piv = Mr[:3, :3] @ (np.asarray(model.bones[r].pivot, float) / 16.0) + Mr[:3, 3]
        for c in by[b]:
            p = sc.cube_points(fb._cube_world(M, c), sc.lattice(c))
            pts.append(p)
            roots.append(np.repeat(piv[None], len(p), axis=0))
    boxes = [fb._cube_world(fb._bone_world(pose, b, model), c) for b in core for c in by[b]]
    if not pts or not boxes:
        return 0.0
    J = np.array(boxes)
    E = np.stack([J[:, 6] - J[:, 0], J[:, 2] - J[:, 0], J[:, 1] - J[:, 0]], axis=2)   # B x 3 x 3, edges as columns
    A = E / np.maximum(np.linalg.norm(E, axis=1, keepdims=True), 1e-9)
    return sc.entry_depth(np.concatenate(pts), np.concatenate(roots), J, A) * 16.0


def despike(upper, label, legs=None):
    """Where the two-arm solve flips an elbow between one sample and the next (the refit thrusts at t6.25 to t6.5: an arm
    turned 18 to 23 deg in one quarter tick, as much again as either neighbour), the two samples either side of the flip
    are laid on the path between the samples around them, so the flip is spread over three steps. Nothing else moves.
    Fix round 1 (2026-11-12): laid on the straight Euler line, the admiral's thrusts swung a forearm through the chest at
    t6.5 (scorecard pen_limb 1.405 and 1.585, new). With the legs given (DESPIKE_GUARDED), each spread is tried on that
    line and on the shortest turn (slerp), all, three quarters and half of the way; the first that keeps the forearms and
    hands within LIMB_OK (or LIMB_SLACK of the solve) is used, and a flip no spread can take is left as solved."""
    n = len(upper)
    worst, kept = 0.0, set()
    for _ in range(8):
        hit = False
        for b in POP_BONES:
            if b not in upper[0]:
                continue
            R = [_rot_m(u[b][0]) for u in upper]
            s = [math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(R[k].T @ R[k + 1]) - 1.0) / 2.0)))) for k in range(n - 1)]
            for k in range(1, n - 2):
                spike = s[k] - max(s[k - 1], s[k + 1])
                if (b, k) in kept or not (spike > POP_AT and s[k] > 2.5 * max(s[k - 1], s[k + 1], 0.5)):
                    continue
                a, c = upper[k - 1], upper[k + 2]

                def spread(how):
                    out = {}
                    for j, f in ((k, 1.0 / 3.0), (k + 1, 2.0 / 3.0)):
                        q = dict(upper[j])
                        for bb in POP_BONES:
                            if bb in a and bb in c:
                                if how == "line":
                                    ra, rc = np.array(a[bb][0]), np.array(c[bb][0])
                                    rr = ra + np.array([K.wrap(x) for x in (rc - ra)]) * f
                                else:
                                    rr = near_euler(slerp_rot(a[bb][0], c[bb][0], f), upper[j][bb][0])
                                q[bb] = (tuple(float(x) for x in rr), upper[j][bb][1])
                        out[j] = q
                    return out
                best = None
                if legs is None:
                    best = spread("line")
                else:
                    own = max(_limb_depth(upper[j], legs[j]) for j in (k, k + 1))
                    ok = max(own + LIMB_SLACK, LIMB_OK)
                    tried = []
                    for w in (1.0, 0.75, 0.5):
                        for how in ("line", "slerp"):
                            cand = spread(how)
                            if w < 1.0:     # only part of the way: the flip spread less, the forearm kept out
                                cand = {j: dict(q, **{bb: (tuple(float(x) for x in near_euler(
                                    slerp_rot(upper[j][bb][0], q[bb][0], w), upper[j][bb][0])), q[bb][1])
                                    for bb in POP_BONES if bb in q}) for j, q in cand.items()}
                            d = max(_limb_depth(cand[j], legs[j]) for j in (k, k + 1))
                            tried.append("%s %.2f: %.2f" % (how, w, d))
                            if d <= ok:
                                best = (d, cand, "%s, %d%% of the way" % (how, round(w * 100)))
                                break   # the full line first: where it keeps the forearms out, the clip is as before
                        if best:
                            break
                    print("  %s: spreads tried at sample %d (px into the body): %s" % (label, k, ", ".join(tried)))
                    if best is None:
                        print("  %s: an arm flip of %.1f deg at sample %d left as solved (every spread sinks a forearm, %.2f px own)"
                              % (label, spike, k, own))
                        kept.add((b, k))
                        hit = True
                        break
                    print("  %s: flip at sample %d spread on the %s (limb depth %.2f px, solve %.2f)" % (label, k, best[2], best[0], own))
                    best = best[1]
                worst = max(worst, spike)
                for j, q in best.items():
                    upper[j] = q
                hit = True
                break
            if hit:
                break
        if not hit:
            break
    if worst:
        print("  %s: an arm flip of %.1f deg in one sample spread over three" % (label, worst))
    return upper


def keep_left_fist(offs, legs_offs, label, base=None):
    """An additive blow turns the two arms by keyed amounts that do not keep the two fists on one shaft: the left fist came
    off its place on the spear by up to 11 px (tusk hit_medium_front). Over the pose it is added to, the left arm and
    forearm are solved, sample by sample, to put the left wrist back where the right fist's frame carries it from the guard
    (the shaft's own place for it); the upper arm turns least where it can. Written back as the clip's offsets."""
    bu, bl = base or guard_pose()
    piv = np.r_[K.MODEL.bones["left_hand"].pivot / 16.0, 1.0]
    g = fk_pose(bu, bl)
    on_shaft = np.linalg.inv(K.world(g, "right_hand")) @ (K.world(g, "left_hand") @ piv)
    worst = [0.0, 0.0]
    orig = [dict(o) for o in offs]
    for i, o in enumerate(offs):
        up = _add(bu, o)
        lg = _add(bl, legs_offs[i]) if legs_offs else bl
        p = fk_pose(up, lg)
        want = (K.world(p, "right_hand") @ on_shaft)[:3] * 16.0      # px
        (ax, ay, az), apos = up["left_arm"]
        fx = up["left_forearm"][0][0]

        def wrist(v):
            q = dict(up)
            q["left_arm"] = ((v[0], v[3], v[1]), apos)
            q["left_forearm"] = ((v[2],) + tuple(up["left_forearm"][0][1:]), up["left_forearm"][1])
            return (K.world(fk_pose(q, lg), "left_hand") @ piv)[:3] * 16.0

        x = np.array([ax, az, fx, ay], dtype=float)
        miss0 = float(np.linalg.norm(want - wrist(x)))
        if miss0 < 0.3:
            continue
        for _ in range(30):
            a = wrist(x)
            err = want - a
            if np.linalg.norm(err) < 0.05:
                break
            J = np.zeros((3, 4))
            for j in range(4):
                d = np.zeros(4)
                d[j] = 0.05
                J[:, j] = (wrist(x + d) - a) / 0.05
            # the least turn that does it: the twist costs the most, so the elbow and shoulder do the work first
            Wd = np.diag([1.0, 1.0, 1.0, 4.0]) * 1e-3
            step = np.linalg.solve(J.T @ J + Wd, J.T @ err)
            nrm = np.linalg.norm(step)
            if nrm > 8:
                step *= 8 / nrm
            x += step
            x[2] = min(-2.0, max(-150.0, x[2]))
        miss = float(np.linalg.norm(want - wrist(x)))
        worst = [max(worst[0], miss0), max(worst[1], miss)]
        o["left_arm"] = _sub(((x[0], x[3], x[1]), apos), bu.get("left_arm"))
        o["left_forearm"] = _sub(((x[2],) + tuple(up["left_forearm"][0][1:]), up["left_forearm"][1]), bu.get("left_forearm"))
    if worst[1] > 0.5:      # out of the arm's reach somewhere (the blow turns the chest too far): left as keyed
        print("  %s: the left fist comes off the shaft by %.1f px; out of reach (%.2f), left as keyed" % (label, worst[0], worst[1]))
        return orig
    if worst[0]:
        print("  %s: the left fist kept on the shaft (it came off by %.1f px; now %.2f)" % (label, worst[0], worst[1]))
    return offs


HEAD_TURN = 16      # deg the head turns at most to let a passing forearm by


def head_clear(upper, legs, label, loop=False):
    """Where a forearm's armour (a wrap, a bracer) crosses the helm or snout deeper than the rest model has it (the front
    arm coming up past the face), the head turns out of its way: the smallest yaw or nod that fits, eased over a tick each
    side. Judged like plate_clear; only a clip that fails is touched."""
    upper = [dict(u) for u in upper]     # a loop's last sample may be its first one's dict
    fb, fc, model, alive, _ = _actx()
    if "head" not in model.bones or "head" not in K.MODEL.bones:
        return upper
    mine = [b for b in fb._under(model, "head") if b not in alive]
    arms = [b for side in ("right_arm", "left_arm") if side in model.bones for b in fb._under(model, side)
            if b not in alive and b in GEO_BONES and set(b.split("_")) & {"wrap", "bracer", "cuff", "forearm"}]
    if not mine or not arms:
        return upper
    F = _Fit(mine, arms)
    n = len(upper)
    poses = [_posed(upper[i], legs[i]) for i in range(n)]
    Wo = [_wcubes(p, arms) for p in poses]
    deep = [F.deep(_wcubes(p, mine), o)[0] for p, o in zip(poses, Wo)]
    fit = _fit_target(deep, loop)
    if fit is None:
        return upper
    turns = [(ax, s * m) for m in range(2, HEAD_TURN + 1, 2) for ax in (1, 0) for s in (1, -1)]
    Y, P = np.zeros(n), np.zeros(n)
    for i in range(n):
        if deep[i] <= fit:
            continue
        r0 = poses[i].get("head", (Z3, Z3))
        for ax, v in turns:
            q = dict(poses[i])
            r = list(r0[0]); r[ax] += v
            q["head"] = (tuple(r), r0[1])
            if F.deep(_wcubes(q, mine), Wo[i])[0] <= fit:
                (Y if ax == 1 else P)[i] = v
                break
    if not (Y.any() or P.any()):
        return upper
    sy, sp = _ease(Y, loop, absmax=True), _ease(P, loop, absmax=True)
    for i in range(n):
        r, pos = upper[i].get("head", (Z3, None))
        upper[i]["head"] = ((r[0] + float(sp[i]), r[1] + float(sy[i]), r[2]), pos)
    k = int(np.argmax(np.abs(sy) + np.abs(sp)))
    print("  %s: head turns yaw %.0f / nod %.0f deg off the forearm (most at t%.2f; fit %.2f -> %.2f)"
          % (label, sy[k], sp[k], k / SUB, max(deep), fit))
    return upper


def armour_pass(up, lg, label, loop=False):
    """The shoulder plates off the helm, the head off a passing forearm, the cloth out of the thighs' way (each only
    where the clip fails)."""
    up = plate_clear(up, lg, label, loop=loop)
    up = head_clear(up, lg, label, loop=loop)
    return cloth_clear(up, lg, label, loop=loop)


def armour_additive(offs, legs_offs, label, base=None):
    """armour_pass for an additive clip, judged over the pose it is added to; what it moves is written back as offsets.
    Returns (offs, legs_offs); the cloth of a clip with no legs clip of its own is keyed in the upper clip."""
    bu, bl = base or guard_pose()
    up = [_add(bu, o) for o in offs]
    lg = [_add(bl, o) for o in legs_offs] if legs_offs else [dict(bl) for _ in offs]
    b_up, b_lg = [dict(u) for u in up], [dict(l) for l in lg]
    up, lg = armour_pass(up, lg, label)
    # a bone the pass moved anywhere is keyed on every frame (the bake takes a missing key from frame 0)
    moved_u = {b for u, u0 in zip(up, b_up) for b, v in u.items() if v != u0.get(b)}
    moved_l = {b for l, l0 in zip(lg, b_lg) for b, v in l.items() if v != l0.get(b)}
    for o, u in zip(offs, up):
        for b in moved_u & set(u):
            o[b] = _sub(u[b], bu.get(b))
    for i, l in enumerate(lg):
        for b in moved_l & set(l):
            tgt = legs_offs[i] if legs_offs else offs[i]
            tgt[b] = _sub(l[b], bl.get(b))
    return offs, legs_offs


def armour_on_clip(A, name, legs_name=None, loop=False):
    """armour_pass over a clip of the exported file (the shared modules' stances and charge), re-baked only if it moved."""
    STN = _stances()
    clip = A[name]
    n = int(round(clip["animation_length"] * TPS)) * SUB
    ups = _frames(STN, clip, n)
    gl = guard_pose()[1]
    lgs = [{**gl, **l} for l in _frames(STN, A[legs_name], n)] if legs_name and legs_name in A else [dict(gl) for _ in range(n + 1)]
    before = [dict(u) for u in ups]
    ups, lgs2 = armour_pass(ups, lgs, name, loop=loop)
    for u, l, l0 in zip(ups, lgs2, lgs):     # cloth the legs clip would key goes in this (upper) clip: the legs clip is shared
        for b, v in l.items():
            if v != l0.get(b):
                u[b] = v
    if ups != before:
        A[name] = STN.bake(ups, n // SUB, clip.get("loop", False))


def guard_pose():
    """The stance's first frame: the solved guard over the ready legs (what stance_ready_upper / _legs start on)."""
    if not _GUARD:
        legs = K.attack_legs({"hips": [(0, *READY_HIPS), (1, *READY_HIPS)], "steps": []}, ticks=1)[0]
        _GUARD.append((sample([(0, GUARD), (1, GUARD)], legs, Solver(), 0.0), legs))
    u, l = _GUARD[0]
    return dict(u), dict(l)


_GUARD = []


def _add(base, off):
    out = {b: (tuple(r), p) for b, (r, p) in base.items()}
    for b, (r, p) in off.items():
        r0, p0 = out.get(b, (Z3, None))
        pos = p0 if p is None else tuple(a + c for a, c in zip(p0 or Z3, p))
        out[b] = (tuple(a + c for a, c in zip(r0, r)), pos)
    return out


def _sub(abs_, base):
    r0, p0 = base if base is not None else (Z3, None)
    r, p = abs_
    return (tuple(K.wrap(a - c) for a, c in zip(r, r0)),
            None if p is None else tuple(a - c for a, c in zip(p, p0 or Z3)))


def soft_additive(offs, legs_offs, label, base=None, gain=1.0):
    """soft_motion for an additive clip: played over the stance's first frame, the pieces' swing is the clip's own keys.
    gain scales every piece's swing (a light flinch moves the body too little to swing them past secondary_life's 4 deg)."""
    bu, bl = base or guard_pose()
    parts = soft_parts()
    for c in parts.values():
        c["gain"] *= gain
    plates = _fullbody().plates_for()
    pieces = {b for c in parts.values() for b in c["bones"]} | set(plates)
    up = [_add({b: v for b, v in bu.items() if b not in pieces}, o) for o in offs]
    lg = [_add(bl, o) for o in legs_offs] if legs_offs and len(legs_offs) == len(offs) else [dict(bl) for _ in offs]
    up, lg = soft(up, lg, label, parts)
    for o, u in zip(offs, up):
        for b in pieces:
            if b in u:
                o[b] = (tuple(u[b][0]), u[b][1])
    return offs


def _rot_m(r):
    a, b, c = np.radians([-r[0], -r[1], r[2]])
    return (K.rg._Rz(c) @ K.rg._Ry(b) @ K.rg._Rx(a))[:3, :3]


_WCORNERS = []


def _weapon_local():
    """The weapon's corners in the right hand's frame (all tiers), and the sole's height, from the guard."""
    if not _WCORNERS:
        s, legs = guard_pose()
        pose = fk_pose(s, legs)
        P, owner = _CORNERS(pose)
        isw = np.array([is_weapon(o) for o in owner])
        L = np.linalg.inv(K.world(pose, "right_hand")) @ np.c_[P[isw] / 16.0, np.ones(int(isw.sum()))].T
        _WCORNERS.extend([L, float(P[~isw, 1].min())])
    return _WCORNERS


def weapon_clear(pose):
    L, sole = _weapon_local()
    return float((K.world(pose, "right_hand") @ L)[1].min() * 16.0 - sole)


def body_clear(pose):
    P, owner = _CORNERS(pose)
    keep = np.array([not is_weapon(o) for o in owner])
    return float(P[keep, 1].min() - _weapon_local()[1])


def _lift_arms(pose_upper, d):
    """Both upper arms turned d degrees about the chest's cross axis (x): the hands move as one, the shaft stays in both."""
    if abs(d) < 1e-9:
        return pose_upper
    q = dict(pose_upper)
    R = _rot_m((d, 0.0, 0.0))
    for side in ("right_arm", "left_arm"):
        r, p = q[side]
        q[side] = (near_euler(K.decompose_zyx(R @ _rot_m(r)), tuple(r)), p)
    return q


def _ease_series(w):
    n = len(w)
    wide = np.array([max(w[max(0, i - SUB):i + SUB + 1], key=abs) if any(w[max(0, i - SUB):i + SUB + 1]) else 0.0 for i in range(n)])
    h = SUB
    return [float(np.mean(wide[max(0, i - h):i + h + 1])) if wide[max(0, i - h):i + h + 1].any() else 0.0 for i in range(n)]


def floor_guard(ups, legs, label, base=None, additive=False):
    """Keeps the weapon and the feet above the ground. ups/legs: samples (offsets over the stance's first frame if additive).
    Where the weapon would sink, both arms lift it (the smallest turn either way that clears, eased over a tick each side);
    where a foot would sink, the hips rise by as much. Samples that clear are left exactly as they were."""
    bu, bl = base or guard_pose()
    n = len(ups)
    absu = [_add(bu, u) if additive else u for u in ups]
    absl = [_add(bl, l) if additive else l for l in legs] if legs else [dict(bl)] * n
    lift = []
    for i in range(n):
        pose = fk_pose(absu[i], absl[i])
        if weapon_clear(pose) >= FLOOR_CLEAR:
            lift.append(0.0)
            continue
        best = 0.0
        for mag in np.arange(1.0, 46.0, 1.0):
            hit = [s * mag for s in (-1.0, 1.0) if weapon_clear(fk_pose(_lift_arms(absu[i], s * mag), absl[i])) >= FLOOR_CLEAR + 0.2]
            if hit:
                best = hit[0]
                break
        lift.append(best)
    lift = _ease_series(lift)
    rise = []
    for i in range(n):
        rise.append(max(0.0, -FEET_SINK - body_clear(fk_pose(_lift_arms(absu[i], lift[i]), absl[i]))))
    rise = _ease_series(rise) if legs else [0.0] * n
    if any(lift):
        k = int(np.argmax(np.abs(lift)))
        print("  %s: the arms lift the weapon clear of the ground, up to %.0f deg at t%.2f" % (label, lift[k], k / SUB))
    if any(rise):
        k = int(np.argmax(rise))
        print("  %s: the hips rise %.2f px at t%.2f to keep the feet out of the ground" % (label, rise[k], k / SUB))
    for i in range(n):
        if lift[i]:
            new = _lift_arms(absu[i], lift[i])
            for side in ("right_arm", "left_arm"):
                ups[i][side] = _sub(new[side], bu.get(side)) if additive else new[side]
        if rise[i]:
            r, p = legs[i]["hips"]
            legs[i]["hips"] = (r, (p[0], p[1] + rise[i], p[2]) if p is not None else (0.0, rise[i], 0.0))
    return ups, legs


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
        if REVIEW:
            upper = despike(upper, "attack_" + name, legs if (TROOP, name) in DESPIKE_GUARDED else None)
        upper = gear(upper, legs)
        if REVIEW:
            upper, legs = soft(upper, legs, "attack_" + name)
            upper, legs = armour_pass(upper, legs, "attack_" + name)
        clip = K.bake(upper, ATTACK_TICKS, loop=False)
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
    if REVIEW:
        samples, _ = cloth_clear(samples, list(lsamp) + [lsamp[0]], "%s_upper" % g.name, loop=True, into_upper=True)
        samples[-1] = samples[0]
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
if V2C:
    # V2: brace sunk a step lower over its wider feet (the fists ride down with the hips), the ward leaning back as it gives ground
    # the brace sets the point steeply, a pike grounded against a charge, so its silhouette parts from the guard
    STANCE_KEYS["brace"] = KEY((18, 16, 0), (2, 0, 0), (-5.0, 10.9, 0.5), aim(32, 14), Ls=7.0)
    STANCE_KEYS["wall"] = KEY((22, 18, 0), (6, 0, 0), (-4.0, 13.2, 3.2), aim(5, 12), Ls=5.0)
    STANCE_KEYS["ward"] = KEY((-4, 10, 0), (-2, 0, 0), (-4.5, 14.6, 1.0), aim(26, 16), Ls=6.5)
if WIDE:
    # the wide guard's stances: the same rule (fists out from the body, elbows out), each its own silhouette
    STANCE_KEYS["brace"] = KEY((18, 16, 0), (2, 0, 0), (-7.27, 12.3, 1.88), aim(28.07, 15.05), Ls=8.75, rp=(-77.28, -36.97, 29.76, -5.42),
                               lp=(-98.23, 23.21, -7.23, -27.18))
    STANCE_KEYS["wall"] = KEY((22, 18, 0), (6, 0, 0), (-8.58, 13, 1.02), aim(6, 16), Ls=9.34, rp=(-58.15, -30.01, 58, -81.92),
                              lp=(-93.42, 17.38, 8.51, -32.74))
    STANCE_KEYS["ward"] = KEY((-4, 10, 0), (-2, 0, 0), (-8.14, 16.36, -2.27), aim(25.1, 30.5), Ls=11.2, rp=(0.66, -35.1, 39.51, -85.05),
                              lp=(-112.44, 7.82, -90.92, -46.43))   # the fall back walks on it: clear of the bobbing hips too
STANCE_KEYS["shield_up"] = STANCE_KEYS["ready"]
LIFE = {"ready": 3.4, "brace": 1.6, "wall": 1.4, "ward": 2.0, "shield_up": 3.4}
IDLE_LIFE = 2.2
READY_LIKE = ("ready", "shield_up")   # shield_up is the ready pose: both ride V2_READY_SWAY_SCALE


def stance_upper(k, life, legs_spec, ticks=K.STANCE_TICKS, ready=False):
    n = ticks * SUB
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)
    hips = (legs_spec or STANCE_LEGS["ready"])["hips"]
    legs = {"hips": hips}
    rows = []
    for i in range(n + 1):
        u = (i % n) / n
        br = a_br * math.sin(2 * math.pi * u)
        wt = a_wt * math.sin(2 * math.pi * u - K.WEIGHT)
        wl = a_wt * math.sin(2 * math.pi * u - K.WEIGHT - 0.7)
        tx, ty, tz = k["torso"]
        torso = (tx + 0.8 * br + 1.8 * wt, ty + 1.4 * wt, tz + 1.2 * wt)
        gaze = tuple(np.array(k["head"]) + np.array([-0.3 * br, 0.0, 0.0]))
        head = head_local(gaze, torso, hips[0])
        # the shaft rides the breath a beat behind it, its point drifting
        O = np.array(k["O"]) + np.array([0.0, 0.12 * br + 0.15 * wl, 0.1 * wl])
        dd = np.array(k["d"]) + np.array([0.0, 0.006 * br + 0.01 * wl, 0.0])
        rows.append((legs, torso, head, O, unit(dd), k["u"], k["Ls"][0], k["rp"], k["lp"]))
    if not V2C:
        sol = Solver()
        out = [sol.pose(*r) for r in rows]
    else:
        # Loop closure: the two-arm solve has a slack direction (the elbows can swivel about the fists) that the weak arm
        # prior barely holds, so warm-starting sample after sample let the arms creep along it and the loop ended a few
        # degrees off its first frame (a kink at the wrap). Every sample starts from the same solution (the first frame's
        # solved from the priors), so each is a function of its own breath phase alone and the last frame meets the first.
        first = Solver()
        first.pose(*rows[0])
        anchor = first.warm.copy()
        sol, out = Solver(), []
        for r in rows:
            sol.warm = anchor.copy()
            out.append(sol.pose(*r))
    out = gear(out)
    out[-1] = out[0]
    if REVIEW:
        # the helm and the shoulder plates both ride above the hips: the ready legs stand in for the stance's own
        gl = guard_pose()[1]
        out = plate_clear(out, [gl] * len(out), "stance", loop=True)
        out[-1] = out[0]
    return K.bake(out, ticks)


def stance_clips():
    out = {"stance_%s_upper" % k: stance_upper(v, LIFE[k], STANCE_LEGS.get(k), ready=k in READY_LIKE) for k, v in STANCE_KEYS.items()}
    # V2: the brace and the line rock their weight with the upper body (it already does), not on stiff legs
    life = (lambda k: K.STANCE_LIFE.get(k, LIFE[k])) if V2C else (lambda k: K.STANCE_LIFE.get(k, 1.0))
    out.update({"stance_%s_legs" % k: stance_legs(v, life(k), ready=k in READY_LIKE) for k, v in STANCE_LEGS.items()})
    if REVIEW:     # the cloth out of the thighs' way over each stance's own legs (the plates are cleared in stance_upper)
        for k in STANCE_KEYS:
            lname = "stance_%s_legs" % k if "stance_%s_legs" % k in out else "stance_ready_legs"
            armour_on_clip(out, "stance_%s_upper" % k, lname, loop=True)
    return out


def idle_clips():
    return stance_legs(STANCE_LEGS["ready"], IDLE_LIFE, K.IDLE_TICKS), stance_upper(GUARD, IDLE_LIFE, None, K.IDLE_TICKS)


K.idle_clips = idle_clips    # the death falls start from the guard

# ------------------------------------------------------------------------------------------------
# Hits: additive offsets. Both fists keep the spear, so the torso and head take the blow and the arms move together.
# ------------------------------------------------------------------------------------------------

off = K.off
hb, hb_front = K.hb, K.hb_front   # Kort's polish: how far a blow throws the spine and head BACK (art/anim/common/feel.py)
HIT_PEAK = {
    "front": off(torso=(hb_front(-17), -10, 0), head=(hb(-12), 4, 0), rarm=(-8, 0, 6), larm=(-8, 0, -5), lfore=-6),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(6, 0, 4), larm=(6, 0, -4)),
    "right": off(torso=(hb(-5), 14, 8), head=(0, 6, 3), rarm=(4, 0, 8), larm=(-4, 0, -4)),
    "left": off(torso=(hb(-5), -14, -8), head=(0, -6, -3), rarm=(-4, 0, 4), larm=(4, 0, -8)),
}


hit_keys = K.hit_keys
if V2C:
    # A blow from behind throws the chest forward; the arms lift against it so the long haft stays near level and its point
    # does not drive into the ground ahead.
    HIT_PEAK["back"] = off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(-7, 0, 4), larm=(-7, 0, -4))
    # V2 grading: a light blow is a smaller flinch (0.4 of the stagger's first key), a medium one is thrown further, held a
    # beat longer and caught slower
    def hit_keys(peak, weight):
        if weight == "light":
            p = K.scaled(peak, 0.4)
            return [(0, K.ZERO), (1.5, p), (4, K.scaled(p, -0.12)), (6.5, K.scaled(p, 0.04)), (9, K.ZERO)]
        return [(0, K.ZERO), (2, K.scaled(peak, 2.3)), (4.5, K.scaled(peak, 2.15)), (7.5, K.scaled(peak, 0.55)),
                (10.5, K.scaled(peak, -0.35)), (13, K.scaled(peak, 0.08)), (16, K.ZERO)]


def hit_clips():
    out = {}
    if REVIEW:
        base = guard_pose()
        legs_of = {"medium": lambda d: K.legs_offset(K.attack_legs(K.HIT_LEGS[d], ticks=K.HIT_TICKS["medium"]))}
    for weight, ticks in K.HIT_TICKS.items():
        for dname, peak in HIT_PEAK.items():
            keys = K.front_onset(hit_keys(peak, weight), dname, weight)
            name = "hit_%s_%s" % (weight, dname)
            ups = [K.sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)]
            if REVIEW:
                # the medium blows (the heavy is the medium x1.55) and the light blow from the front, which is also the
                # block: every piece swings; the weapon and the feet stay above the ground (the light flinches do not move
                # enough to need either and stay as they were, unless the shaft dips)
                lo = legs_of["medium"](dname) if weight == "medium" else None
                if weight == "medium" or dname == "front":
                    ups = keep_left_fist(ups, lo, name, base)
                    ups = soft_additive(ups, lo, name, base, gain=1.0 if weight == "medium" else 2.5)
                ups, lo = floor_guard(ups, [dict(x) for x in lo] if lo else None, name, base, additive=True)
                ups, lo = armour_additive(ups, lo, name, base)
                if lo is not None:
                    HIT_LEGS_FIXED[name + "_legs"] = lo
            out[name] = K.bake(ups, ticks, loop=False)
    out["hit_block"] = out["hit_light_front"]
    legs = K.hit_legs_clips()
    for name, lo in HIT_LEGS_FIXED.items():
        if lo != K.legs_offset(K.attack_legs(K.HIT_LEGS[name.split("_")[2]], ticks=K.HIT_TICKS["medium"])):
            legs[name] = K.bake(lo, K.HIT_TICKS["medium"], loop=False)
    out.update(legs)
    return out


HIT_LEGS_FIXED = {}


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


def flat_weapon(keys, start, t0, t1, step=None, look=None, turn=None):
    """Re-key the fall every half tick (DEATH_STEP) with the right wrist solved: free, then easing from t0 to the spear lying
    flat by t1, always tipped clear of the ground and swinging at most MAX_WRIST_TURN per tick."""
    step = DEATH_STEP if step is None else step
    look = LOOKAHEAD if look is None else look
    turn_cap = MAX_WRIST_TURN if turn is None else turn
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
        turn, cap = turn_deg(prev, r), turn_cap * step
        if look:                                       # solve every key free first; the turn cap is applied backwards below
            out.append((t, f, pose, r))
            prev = r
            continue
        if turn > cap:                                 # the spear swings round, it does not flip in a tick
            r = ground_hand(pose, slerp_rot(prev, r, cap / turn), prev)
        prev = near_euler(r, prev)
        f["right_hand"] = prev
        out.append((t, "lin", f))
    if not look:
        return out
    # Look-ahead: walking back from the end, a key the next one is too far from turns part-way towards it, so the wrist
    # starts swinging early instead of flipping late; then a forward pass caps whatever is left.
    cap = turn_cap * step
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
    back = K.DEATH_BACK
    if V2C:
        # The knight's fall from behind, from the spear guard: the guard leans the chest 14 deg forward, so the knight's first
        # key (torso -14) whipped the spine 28 deg back in 3 ticks (scorecard ang_speed_torso 26, limit 21.7); it is thrown
        # back to -6 there and the fall carries it on. The knight's shield hand turns over to lay the shield flat (left_hand
        # -180 at t16, 159 deg/100ms); the spearman's left fist holds nothing by then and stays as it is.
        back = [(t, e, {b: ((-6, 0, 0) if (t, b) == (3, "torso") else r) for b, r in k.items() if b != "left_hand"})
                for t, e, k in K.DEATH_BACK]
    fwd = K.DEATH_FORWARD
    if REVIEW and TROOP == "wm_tusk":
        # batch review: face down, the tusk's free left arm lay up behind his back (focus_check shoulder 108 deg at t20, fail
        # 80): it lands out at his side, not thrown back (the knight's +25/+30 pitch is -10 here). The others pass: unchanged.
        fwd = [(t, e, {b: ((-10.0, r[1], r[2]) if b == "left_arm" and t in (16, 20) else r) for b, r in k.items()})
               for t, e, k in K.DEATH_FORWARD]
    falls = {"death_back": back, "death_forward": fwd,
             "death_left": K.roll_keys(K.DEATH_LEFT, start, 13, K.ROLL_LEFT),
             "death_right": K.roll_keys(K.DEATH_RIGHT, start, 13, K.ROLL_RIGHT)}
    return {n: K.death_clip(flat_weapon(k, start, *FLAT_AT[n], look=LOOKAHEAD and n not in NO_LOOKAHEAD, **FALL.get(n, {})), start, n)
            for n, k in falls.items()}


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


ASSET_ANIM = HERE.parents[2] / "src/main/resources/assets/gravesandglory/animations/troop" / f"{TROOP}.animation.json"
POST_ADDITIVE = ("hit_knockback", "hit_pulled", "stun_out_upper")


def _stances():
    sys.path.insert(0, str(HERE.parent / "common"))
    import stances   # noqa: E402  (read-only use: its clip reader and baker)
    return stances


def _frames(STN, clip, n):
    return [{b: (tuple(float(x) for x in r), None if p is None else tuple(float(x) for x in p))
             for b, (r, p) in STN.pose_at(clip, i / SUB / TPS).items()} for i in range(n + 1)]


def post_common(path=ASSET_ANIM):
    """The batch review's passes over the clips the shared modules add (reactions.py: the stun, its shake-off, the knockback
    and the pulled hit), run after them: every piece swings, the weapon and the feet stay out of the ground. The additive ones
    are judged over the stance they are added to (the guard; the shake-off over the stun's loop pose)."""
    STN = _stances()
    doc = json.loads(path.read_text(encoding="utf-8"))
    A = doc["animations"]
    guard = guard_pose()
    su = A["stun_upper"]
    n_st = int(round(su["animation_length"] * TPS)) * SUB
    ups, lgs = _frames(STN, su, n_st), _frames(STN, A["stun_legs"], n_st)
    loop0 = STUN_LOOP_START * SUB
    ext_u, ext_l = ups + ups[loop0 + 1:], lgs + lgs[loop0 + 1:]   # run the springs on into a second pass of the loop
    ext_u, ext_l = soft(ext_u, ext_l, "stun_upper")
    ups, lgs = ext_u[:n_st + 1], ext_l[:n_st + 1]
    seam = 2 * SUB          # the last 2 ticks ease the pieces onto where the loop starts again (t4), so the loop closes
    pieces = {b for c in soft_parts().values() for b in c["bones"]} | set(_fullbody().plates_for())
    for i in range(n_st - seam, n_st + 1):
        k = K.smooth((i - (n_st - seam)) / seam)
        for b in pieces:
            if b in ups[i] and b in ups[loop0]:
                (r1, p1), (r0, p0) = ups[i][b], ups[loop0][b]
                pos = None if p1 is None and p0 is None else tuple(a + (c - a) * k for a, c in zip(p1 or Z3, p0 or Z3))
                ups[i][b] = (tuple(a + (c - a) * k for a, c in zip(r1, r0)), pos)
    # Fix round 1 (2026-11-12): the game loops ticks 4..24, but the clip's first key (the stance hand-off) and its last
    # must agree too (scorecard loop_pos 1.37 tusk, 1.76 admiral: the springs had moved the pieces' positions by tick 4).
    # Each piece's position offset at tick 4 is faded out over the entry (ticks 0..4) and held off through the loop, so
    # ticks 0, 4 and 24 put every piece where the stance has it; rotations and the loop's own motion are untouched.
    ups = [dict(u) for u in ups]
    for b in pieces:
        if b not in ups[0] or b not in ups[loop0]:
            continue
        p0, p4 = ups[0][b][1], ups[loop0][b][1]
        if p0 is None and p4 is None:
            continue
        off = np.array(p4 if p4 is not None else Z3, float) - np.array(p0 if p0 is not None else Z3, float)
        if not off.any():
            continue
        for i in range(1, n_st + 1):
            k = K.smooth(min(1.0, i / float(loop0)))
            r, p = ups[i][b]
            ups[i][b] = (r, tuple(float(a - c * k) for a, c in zip(p if p is not None else Z3, off)))
    # Fix round 1: the tusk's daze sank the left forearm into the chest (scorecard pen_limb 2.785, hit limit 2.336). The
    # forearm swings out from the elbow over the first 2 ticks and stays out through the loop (ticks 4 and 24 alike).
    turn = STUN_FOREARM_OPEN.get(TROOP, 0.0)
    if turn and "left_forearm" in ups[0]:
        for i in range(1, n_st + 1):
            k = K.smooth(min(1.0, i / (2.0 * SUB)))
            r, p = ups[i]["left_forearm"]
            ups[i]["left_forearm"] = ((r[0], r[1], r[2] + turn * k), p)
    ups, lgs = floor_guard(ups, lgs, "stun_upper")
    # (no plate_clear here: on the stun it barely helped, 2.56 to 2.5, and the slide broke the loop seam: scorecard loop_pos)
    A["stun_upper"], A["stun_legs"] = STN.bake(ups, n_st // SUB, su.get("loop", False)), STN.bake(lgs, n_st // SUB, A["stun_legs"].get("loop", False))
    stun_base = ({b: v for b, v in ups[loop0].items()}, lgs[loop0])
    for name in POST_ADDITIVE:
        clip = A[name]
        n = int(round(clip["animation_length"] * TPS)) * SUB
        lname = name + "_legs" if name + "_legs" in A else None
        base = stun_base if name == "stun_out_upper" else guard
        u = _frames(STN, clip, n)
        lo = _frames(STN, A[lname], n) if lname else None
        if name != "stun_out_upper":    # the shake-off starts from the stun, where the hands have let the shaft go
            u = keep_left_fist(u, lo, name, base)
        u = soft_additive(u, lo, name, base)
        u, lo = floor_guard(u, lo, name, base, additive=True)
        u, lo = armour_additive(u, lo, name, base)
        A[name] = STN.bake(u, n // SUB, clip.get("loop", False))
        if lname:
            A[lname] = STN.bake(lo, n // SUB, False)
    # Only the fallback: on the rally and the charge run the pass measured a clip focus_check passes as failing (and its
    # cloth swing made the charge run's tabard worse), so they keep the shared modules' keys.
    for name, lname in (("stance_fallback_upper", "stance_fallback_legs"),):
        if name in A:
            armour_on_clip(A, name, lname, loop=bool(A[name].get("loop", False)))
    with open(path, "w", newline="\n") as fh:
        fh.write(json.dumps(doc, indent=1))
    print("post: re-keyed stun_upper, stun_legs, " + ", ".join(POST_ADDITIVE) + " (and their _legs) in", path)


STUN_LOOP_START = 4
STUN_FOREARM_OPEN = {"wm_tusk": -12.0}   # deg about the forearm's z: the left forearm out of the chest in the stun


def deploy(only=None):
    """The whole rebuild into the game file, in order: this builder's clips (out/) replace their copies in the exported file
    (every other clip stays where it is), the shared modules re-make theirs from the new guard, then post_common.
    (export.py copies the whole out/ file and would drop the shared modules' clips: it is for the rig and textures.)
    `only` (--deploy=a,b,...) splices just those clips, so a clip the review did not change stays byte-identical."""
    import subprocess
    new = json.loads(K.OUT.read_text(encoding="utf-8"))["animations"]
    if only:
        new = {n: v for n, v in new.items() if n in only}
    doc = json.loads(ASSET_ANIM.read_text(encoding="utf-8"))
    A = doc["animations"]
    missing = [n for n in new if n not in A]
    doc["animations"] = {n: new.get(n, v) for n, v in A.items()}
    doc["animations"].update({n: new[n] for n in missing})
    with open(ASSET_ANIM, "w", newline="\n") as fh:
        fh.write(json.dumps(doc, indent=1))
    print("spliced", len(new), "clips into", ASSET_ANIM)
    common = HERE.parent / "common"
    for script in ("stances.py", "charge.py", "reactions.py"):
        subprocess.run([sys.executable, str(common / script), TROOP], check=True)
    post_common()


def main():
    if "--post" in sys.argv:
        post_common()
        return
    dep = [a for a in sys.argv if a.startswith("--deploy")]
    if dep:
        deploy(set(dep[0].split("=", 1)[1].split(",")) if "=" in dep[0] else None)
        return
    if os.environ.get("SPEAR_DUMP"):
        dump(os.environ["SPEAR_DUMP"], os.environ.get("SPEAR_ATTACK", "thrust"),
             [float(x) for x in os.environ.get("SPEAR_TICKS", "0,2.5,5,7,9,12,17").split(",")])
        return
    idle_legs, idle_upper = idle_clips()
    anims = {"walk_legs": K.gait_legs(K.WALK), "walk_upper": gait_upper(K.WALK, K.WALK_CARRY, False),
             "run_legs": K.gait_legs(K.RUN), "run_upper": gait_upper(K.RUN, K.RUN_CARRY, True),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **stance_clips(), **hit_clips(),
             **death_clips()}
    if REVIEW and TROOP == "wm_tusk":
        # The tusk's thrust is the troop-animation skill's demo (round 3, waiting for Kort's grade): it replaces the family's
        # thrust here, as the build's last step, so a rebuild never puts the old clip back. It starts from GUARD (grip 8.0).
        sys.modules.setdefault("build_spear_anims", sys.modules[__name__])   # the demo imports this module: the same one
        sys.path.insert(0, str(HERE.parent / "wm_tusk"))
        import build_thrust_demo   # noqa: E402
        anims.update(build_thrust_demo.clips())
    only_rig(anims)
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, "clips:", ", ".join(anims))


if __name__ == "__main__":
    main()
