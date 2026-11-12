"""Caster clips (unit animation rebuild, docs/anim-rebuild/STYLE.md): the spell-casting family, first the mage.

GG_CAST_TROOP=mage python art/anim/caster/build_caster_anims.py  ->  art/anim/mage/out/mage.animation.json

Built on the thrower builder (art/anim/thrower/build_thrower_anims.py): the knight's rig maths, gait legs, hit footwork and
death falls (art/anim/knight/build_anims.py, GG_ANIM_TROOP set: no shield), the left-lead footing, the gaze-held head and the
solved right fist. Each key names where the right fist sits from the right shoulder (R, troop px), which way the focus points
(d) and which way its face turns (u); the arm reaches the point and the wrist turns the focus onto (d, u). The left arm is free.

The focus's own axes differ per troop: (a, b) are the focus-local directions put onto d and u. The mage's sun staff runs along
its local +y with the disc's face along local z (a = +y, b = -z: the disc faces u); the seer's wand and the mist-caller's rod
run along local -z (a = -z, b = +y). The hexer's grave lantern hangs below a short pole that runs along local -z from the fist
(a = -z, b = +y: the lantern hangs opposite u); its body sits off the axis, so it is probed at its own points too.

The cast (attack_cast, caster_cast impactTicks 10): from the guard the caster gathers, the focus raised over the head and the
load held; then the body drives forward and the focus is thrust at the foe, the spell leaving on the server's impact tick; an
overshoot, a recovery and the settle back into guard. The hexer's cast is the lantern thrust instead: hauled in to the chest,
driven out at the foe and held while the light gathers, then shoved, the blight leaving the lantern on the impact tick
(cast_tip/hexer.json is the lantern glass).
"""
import json, math, os, sys
from pathlib import Path

TROOP = os.environ.get("GG_CAST_TROOP", "mage")
os.environ["GG_ANIM_TROOP"] = TROOP
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "knight"))
import build_anims as K  # noqa: E402
sys.path.insert(0, str(HERE.parent / "common"))
import hang_swing  # noqa: E402
import fullbody  # noqa: E402  (the batch review's soft motion: the same build_anims module as K)

SUB, TPS, Z3 = K.SUB, K.TPS, K.Z3
np = K.np
FOCI = ("weapon", "wand", "rod", "lantern")
K.HELD = K.HELD + FOCI
K.DEATH_BONES = tuple(b for b in K.DEATH_BONES if b != "cape")
_GEO = json.loads((HERE.parent / TROOP / "out" / f"{TROOP}.geo.json").read_text())["minecraft:geometry"][0]["bones"]
GEO_BONES = {b["name"] for b in _GEO}

# Per troop: W = the tier-1 focus bone (every tier shares its pivot and axes); focus = bone-name prefix of every tier's focus;
# a, b = the focus-local axes put onto d and u; gear = hanging pieces and their share of the cape lag (rule 9); soft = hanging
# pieces that never hold a falling body up.
CAST_TROOPS = {
    "mage": {"W": "weapon_t1", "focus": "weapon_", "a": (0, 1, 0), "b": (0, 0, -1),
             "gear": {"shoulder_cape": 0.5, "vestment_skirt": 0.35, "cord": 0.6},
             "soft": ("shoulder_cape", "vestment_skirt", "cord"),   # the robe panels are stiff: a fallen body rests on them
             "body": ("cowl", "face_v0", "mantle")},   # the head's own bone is empty: the cowl and face hold its boxes
    "hexer": {"W": "lantern", "focus": "lantern", "a": (0, 0, -1), "b": (0, 1, 0),
              # the vestment halves ride the robe's front and back panels (troop_rig.HANG) and flutter a little on them
              "gear": {"hood_tail": 0.4, "vestment_front": 0.2, "vestment_back": 0.2, "vestment_skirt": 0.35, "cord": 0.6},
              "soft": ("hood_tail", "vestment_front", "vestment_back", "vestment_skirt", "cord"),   # the robe panels are stiff
              # the lantern body hangs below the pole tip, off the axis: its centre, top and bottom are probed too
              "probe": ((-5.0, 10.0, -5.5), (-5.0, 12.5, -5.5), (-5.0, 7.5, -5.5), (-6.5, 10.0, -5.5), (-3.5, 10.0, -5.5)),
              "tip": (-5.0, 10.0, -5.5)},   # the blight leaves the lantern's glass, not the pole's end
    # the mist-caller's driftwood rod runs along local -z from the fist with the conch at its tip (each tier its own bone)
    "tc_mist": {"W": "rod", "focus": "rod", "a": (0, 0, -1), "b": (0, 1, 0),
                "gear": {"hood_peak": 0.3, "right_kelp_hood": 0.35, "left_kelp_hood": 0.35, "mist_back": 0.6, "sash_tail": 0.3,
                         "charm": 0.5},
                "soft": ("hood_peak", "right_kelp_hood", "left_kelp_hood", "mist_back", "right_mist", "left_mist", "sash_tail",
                         "charm")},   # the kelp, mist strips and sash hang loose; the robe skirt and hem are stiff
    # the Bloodhowl seer's bone wand runs along local -z from the fist (caster style v2: point and shoot, the hanging robe)
    "wm_seer": {"W": "wand", "focus": "wand", "a": (0, 0, -1), "b": (0, 1, 0),
                "gear": {"sash_tail": 0.3}, "soft": ("sash_tail",)},
}
ST = CAST_TROOPS[TROOP]
ROBED = TROOP in ("wm_seer", "tc_mist", "mage", "hexer")   # caster v2: the hanging robe (troop_rig.ROBE), upright footing, the gliding gait
K.SOFT = ST["soft"]
LOOKAHEAD = True
DEATH_STEP = 0.25
TOP_W, TOP_OK = 0.15, 8.0
W = ST["W"]

_HAND_TO_W = K.world({}, "right_hand")[:3, :3].T @ K.world({}, W)[:3, :3]
_PIV = {b["name"]: b["pivot"] for b in _GEO}
GX, GY, GZ = (float(v) for v in _PIV["right_hand"])
GRIP = (GX, GY, GZ)
SHOULDER = tuple(map(float, _PIV["right_arm"]))
_A = np.array(ST["a"], float)
_FC = [(c["origin"], c["size"]) for b in _GEO if b["name"].startswith(ST["focus"]) for c in b.get("cubes", [])]
_FS = [float(_A @ (np.array(o) + np.array(sz) * np.array(k) - np.array(GRIP)))
       for o, sz in _FC for k in ((0, 0, 0), (1, 1, 1))]
BUTT, TIP = min(0.0, min(_FS)), max(_FS)
QGRIP = (0.0, 0.0, 0.0)
SIDE = (1.0, 0.0, 0.0)
FWD = (0.0, 0.0, 1.0)
UP = (0.0, 1.0, 0.0)


def S(s):
    """The focus-local point s px along its axis from the grip."""
    return tuple(np.array(GRIP) + s * _A)


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def frame(d, u):
    d = unit(d)
    u = np.asarray(u, float) - (np.asarray(u, float) @ d) * d
    return d, unit(u)


def hold_wrist(pose, d, u):
    """right_hand rotation turning the focus onto (d, u): its local a onto d, b onto u."""
    d, u = frame(d, u)
    a, b = _A, np.array(ST["b"], float)
    Mw = np.column_stack([d, u, np.cross(d, u)]) @ np.column_stack([a, b, np.cross(a, b)]).T
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


LGRIP = tuple(map(float, _PIV["left_hand"]))


def reach_left(pose, ls, prior, warm=None, w=0.02, lsw=0.05):
    """The left arm (x, y, z), forearm x and the shaft point s (px from the grip) putting the left fist on the focus's axis,
    nearest the prior arm and ls; s stays 3.5 px clear of the right fist on ls's side, inside the shaft."""
    prior = np.asarray(prior[:4], float)
    lo, hi = (3.5, TIP - 5.0) if ls >= 0 else (BUTT + 1.0, -3.5)

    def f(x):
        p = K.point(K.world(_arm("left", pose, x), "left_hand"), LGRIP)
        return np.concatenate([4.0 * (p - wpoint(pose, S(x[4]))), w * (x[:4] - prior), [lsw * (x[4] - ls)]])

    x0 = np.r_[prior, min(hi, max(lo, ls))] if warm is None else warm
    x = gn(f, x0, clamp=lambda x: np.r_[x[:3], min(-2.0, max(-150.0, x[3])), min(hi, max(lo, x[4]))])
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


# ------------------------------------------------------------------------------------------------
# Body boxes: the shaft must stay out of the torso, hips, head and legs.
# ------------------------------------------------------------------------------------------------

def body_boxes(pose):
    P, owner = K.body_corners(pose)
    out = []
    for c in range(0, len(owner), 8):
        if owner[c] not in ("torso", "hips", "head", "right_leg", "left_leg", "right_shin", "left_shin") + ST.get("body", ()):
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
    for j, q in enumerate(ST.get("probe", ())):
        p = wpoint(pose, q)
        for name, o, axes in boxes:
            if all(margin < (p - o) @ a < L - margin for a, L in axes):
                hits.append(("L%d" % j, name))
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
    "brace": {"hips": ((6.0, 24.0, 0.0), (0.0, -3.4, 0.6)), "left": (3.6, 2.0, 3.4), "right": (-3.4, 2.0, -3.6), "yaw": (-10.0, 22.0)},
    "wall": {"hips": ((0.0, 14.0, 0.0), (0.0, -1.1, 0.2)), "left": (3.2, 2.0, 2.8), "right": (-3.0, 2.0, -2.4), "yaw": (-6.0, 14.0)},
}
# Caster v2 footing (Kort's rule 6, 2026-11-10): no shield crouch and the feet close under the hanging robe, the hem clear of
# the floor; each caster stands on its own feet. Per troop: the guard's hips (rot, pos) and feet ((x, y, z), yaw), and the
# brace and wall footings (StanceController plays them for Hold only on a shield or spear troop, so for a caster they are the
# caster's own version of that order, shown in the booth).
CAST_LEGS = {
    # the Sunweaver stands tall and square behind the planted staff, the weight even, the lead foot turned out a little
    "mage": {"hips": ((0.0, 12.0, 0.0), (0.0, -0.8, 0.1)),
             "left": ((3.0, 2.0, 1.3), 12.0), "right": ((-3.0, 2.0, -0.9), -8.0),
             "brace": {"hips": ((-3.0, 4.0, 0.0), (0.0, -0.7, -0.2)), "left": (3.2, 2.0, 0.5), "right": (-3.2, 2.0, -0.5),
                       "yaw": (-12.0, 12.0)},
             "wall": {"hips": ((4.0, 22.0, 0.0), (0.0, -1.1, 0.4)), "left": (3.0, 2.0, 1.9), "right": (-2.8, 2.0, -1.6),
                      "yaw": (-10.0, 16.0)}},
    # the Gravecaller hunches over the heavy lantern: the pelvis tipped in, turned further side-on, wider and lower
    "hexer": {"hips": ((4.0, 22.0, 0.0), (0.0, -1.25, -0.1)),
              "left": ((3.2, 2.0, 1.4), 16.0), "right": ((-3.1, 2.0, -1.4), -10.0),
              "brace": {"hips": ((-1.0, 12.0, 0.0), (0.0, -1.0, -0.2)), "left": (3.3, 2.0, 0.9), "right": (-3.3, 2.0, -1.0),
                        "yaw": (-14.0, 14.0)},
              "wall": {"hips": ((7.0, 14.0, 0.0), (0.0, -1.35, 0.4)), "left": (3.1, 2.0, 2.0), "right": (-3.0, 2.0, -1.6),
                       "yaw": (-12.0, 12.0)}},
    # the mist caller stands loose and open, the pelvis tipped back, the right foot level with the left and turned out (its
    # cast steps that foot forward side-on)
    "tc_mist": {"hips": ((-2.0, 6.0, 0.0), (0.0, -0.9, 0.2)),
                "left": ((2.8, 2.0, 0.6), 8.0), "right": ((-2.9, 2.0, 0.1), -16.0),
                "brace": {"hips": ((-4.0, -4.0, 0.0), (0.0, -0.8, 0.0)), "left": (2.9, 2.0, -0.3), "right": (-3.0, 2.0, 0.6),
                          "yaw": (-20.0, 6.0)},
                "wall": {"hips": ((2.0, -8.0, 0.0), (0.0, -1.0, 0.3)), "left": (2.9, 2.0, -0.2), "right": (-2.7, 2.0, 1.4),
                         "yaw": (-22.0, 4.0)}},
    # the Bloodhowl seer crouches like its pack: the hips low and tipped in, the feet wide and turned out under the robe
    "wm_seer": {"hips": ((6.0, 18.0, 0.0), (0.0, -1.35, 0.2)),
                "left": ((3.3, 2.0, 1.5), 18.0), "right": ((-3.2, 2.0, -1.2), -14.0),
                "brace": {"hips": ((2.0, 10.0, 0.0), (0.0, -1.2, 0.0)), "left": (3.4, 2.0, 0.9), "right": (-3.4, 2.0, -0.9),
                          "yaw": (-18.0, 18.0)},
                "wall": {"hips": ((10.0, 16.0, 0.0), (0.0, -1.45, 0.5)), "left": (3.2, 2.0, 2.2), "right": (-3.1, 2.0, -1.4),
                         "yaw": (-14.0, 16.0)}},
}
if ROBED:
    _cl = CAST_LEGS[TROOP]
    READY_HIPS = _cl["hips"]
    READY_FEET = {"left": _cl["left"], "right": _cl["right"]}
    K.READY_HIPS, K.READY_FEET = READY_HIPS, READY_FEET
    STANCE_LEGS = {"ready": {"hips": READY_HIPS, "left": _cl["left"][0], "right": _cl["right"][0],
                             "yaw": (_cl["right"][1], _cl["left"][1])},
                   "brace": _cl["brace"], "wall": _cl["wall"]}


def home(side, t0, t1, lift=0.8):
    """A glide step that puts the foot back on the troop's own guard footing."""
    p, y = READY_FEET[side]
    return (side, t0, t1, p, y, lift)


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
if ROBED:   # caster v2: the knees give less under the robe (a deep dip would drop the hem to the floor)
    def _shallow(spec, k=0.6):
        y0 = READY_HIPS[1][1]
        return {"hips": [(t, r, (p[0], y0 + k * (p[1] - y0), p[2])) for t, r, p in spec["hips"]], "steps": spec["steps"]}
    K.HIT_LEGS = {d: _shallow(s) for d, s in K.HIT_LEGS.items()}
    K.BLOCK_LEGS = _shallow(K.BLOCK_LEGS)


def stance_legs(spec, life=1.0, ticks=K.STANCE_TICKS, ready=False):
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


def head_local(gaze, torso, hips_rot):
    """The head holds its gaze: the key gives where the eyes look (troop frame); the hips' and torso's turn is taken out."""
    return tuple(g - t - h for g, t, h in zip(gaze, torso, hips_rot))


# ------------------------------------------------------------------------------------------------
# Keys. Each: torso (deg, on the hips), gaze (deg, troop frame), R (the fist's point from the right shoulder, troop px), d, u,
# rp (the right arm prior the solve starts from), lp (the free left arm: arm x y z, forearm x), q (0 at R, 1 at the quiver).
# ------------------------------------------------------------------------------------------------

RP = (-150.0, 0.0, 30.0, -90.0)
LP = (-55.0, 0.0, -12.0, -35.0)


def KEY(torso, gaze, R, d, u=SIDE, rp=RP, lp=LP, q=0.0, Lq=QGRIP, lh=0.0, ls=0.0, lsw=0.05):
    """lh: how far the left hand is on the focus (0 the free arm at lp, 1 the left fist solved onto the shaft); ls: the shaft
    point (px from the grip along a) the left fist prefers; lsw: how hard it holds to ls (fix round 1: the mage's grip)."""
    d, u = frame(d, u)
    return {"torso": tuple(map(float, torso)), "head": tuple(map(float, gaze)), "R": tuple(map(float, R)), "d": tuple(d),
            "u": tuple(u), "rp": tuple(map(float, rp)), "lp": tuple(map(float, lp)), "q": (float(q),), "Lq": tuple(map(float, Lq)),
            "lh": (float(lh),), "ls": (float(ls),), "lsw": (float(lsw),)}


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
ATTACK_TICKS = 36
IMPACT = 10         # caster_cast impactTicks: the spell leaves


# Fix round 1 (2026-11-12): the cast's and the mend's two-handed raise: the right fist 1.5 px lower and the left fist held
# firmly 7.5 px up the shaft (it drifted back to 5.5, where the left bell closed over the right forearm, t8.5-9)
MAGE_LS = 7.5
MAGE_DROP = 1.5


def mage_set():
    """The Sunweaver, caster style v2 (docs/anim-rebuild/CASTER-V2-DESIGN.md: the staff heal). The guard: upright, the 35 px
    sun staff held upright before the lead shoulder, gripped 40% up its shaft, the free hand relaxed at the hip. The heal: the hips
    load back first, then both hands raise the staff (the left fist solved onto the shaft above the right), the body arching back and the gaze
    following the sun up; the light is held with a tremor; the hips drive forward over the lead foot, the torso follows, the
    left hand lets go and the staff tips toward the ally, the heal leaving the sun on the release tick, the left hand open
    after it as a blessing; overshoot, recover, settle. The arm reach (8 px) against the tall cowl keeps both fists at chest
    height: the staff is tilted out to the right so the shaft rises beside the head, not through it."""
    lp_rest = (-12, 0, -12, -40)          # the off hand relaxed at the hip, by the sash
    # the staff is gripped about 40% up its shaft (troop_rig.GRIP_SLIDE), a walking staff: the fist at the belly, the staff all
    # but upright with its butt by the lead foot an ankle's height off the ground and the sun well over the cowl
    g = KEY((3, 8, 0), (4, 0, 0), (-2.0, -3.0, 6.0), aim(86, -14), u=FWD, rp=(-40, 0, 10, -40), lp=lp_rest)
    load = nudge(g, torso=(-2, 2, 0), head=(-2, -2, 0), R=(0.3, 0.6, 0.1), d=aim(84, -18))
    # the raise: both hands come onto the staff and lift it, the left fist above the right (the staff slants up across the
    # chest to the left, the only place both 8 px arms reach the shaft one over the other, clear of the cowl)
    # (batch review 2026-11-11: the left fist 5.5 px up the shaft, not 4.5: at 4.5 the left bell sleeve closed over the right
    # forearm as the light was held)
    gather = KEY((-3, 6, 0), (-4, -8, 0), (2.0, -2.5 - MAGE_DROP, 6.8), unit((0.35, 0.9, 0.2)), u=FWD, rp=(-80, 0, 10, -40),
                 lp=(-60, 0, -15, -50), lh=0.6, ls=MAGE_LS, lsw=1.0)
    up = KEY((-6, 4, 0), (-10, -16, 0), (2.4, -0.5 - MAGE_DROP, 7.0), unit((0.45, 0.85, 0.2)), u=FWD, rp=(-110, 0, 10, -40),
             lp=(-90, 0, -20, -50), lh=1, ls=MAGE_LS, lsw=1.0)
    # the light is held: a tremor, the arch deepening a little
    held = nudge(up, torso=(-1, 1, 0), head=(-2, 0, 0), R=(0.0, 0.2, 0.0))
    held2 = nudge(up, torso=(-2, 2, 0), head=(-1, 0, 0), R=(0.1, 0.3, 0.1), d=unit((0.48, 0.83, 0.24)))
    # the drive: the hips have gone first; the torso follows, the left hand comes off and the staff starts to tip
    drive = KEY((6, -4, 0), (2, -6, 0), (0.0, 1.2, 7.3), aim(56, 0), u=FWD, rp=(-100, 0, 0, -20), lp=(-90, 0, -12, -30),
                lh=0.0, ls=4.5)   # the left is off: tipped forward, the shaft above the right fist is out of its reach
    # the release: the staff tipped at the ally, the heal leaves the sun, the left hand open forward after it
    release = KEY((16, -10, 0), (6, 0, 0), (-1.0, 0.0, 7.8), aim(30, 10), u=FWD, rp=(-95, 0, 0, -10), lp=(-85, 0, 10, -10))
    over = nudge(release, torso=(3, -2, 0), head=(1, 0, 0), R=(0.0, -0.4, 0.0), d=aim(24, 12), lp=(5, 0, 2, 0))
    drift = nudge(over, torso=(2, -1, 0), head=(1, 0, 0), R=(0.0, -0.3, 0.0), d=aim(22, 12))   # the heal carries on past release
    recover = KEY((8, 2, 0), (5, 0, 0), (-2.0, -2.5, 6.0), aim(86, -14), u=FWD, rp=(-70, 0, 10, -30), lp=(-40, 0, -10, -40))
    near = nudge(g, torso=(2, -2, 0), R=(0.0, 0.3, -0.1), d=aim(82, -12))
    settle = nudge(g, R=(0.0, 0.15, -0.1), d=aim(85, -14))
    attack = [(0, g), (1.5, load), (2.8, gather), (4.5, up), (6.5, held), (8.4, held2), (9.0, drive), (IMPACT, release),
              (11.8, over), (13.6, drift), (16.0, recover), (22.0, near), (28.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    # the Knight's weight shift: the hips load back before the arms move and drive forward before the torso does
    legs = {"hips": [(0, R0, R1), (1.5, (-2.0, 10.0, 0.0), (0.0, -1.0, -0.4)), (4.5, (-2.0, 12.0, 0.0), (0.0, -1.1, -0.4)),
                     (7.6, (-3.0, 13.0, 0.0), (0.0, -1.15, -0.5)), (9.0, (6.0, 3.0, 0.0), (0.0, -1.4, 0.8)),
                     (IMPACT, (8.0, 0.0, 0.0), (0.0, -1.6, 1.2)), (11.8, (9.0, -2.0, 0.0), (0.0, -1.7, 1.4)),
                     (16.0, (4.0, 6.0, 0.0), (0.0, -1.3, 0.7)), (24.0, (1.0, 12.0, 0.0), (0.0, -1.0, 0.3)),
                     (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides under the robe: the lead foot slides forward as the hips drive, the rear foot drags after
            "steps": [("left", 7.8, 10.0, (2.9, 2.0, 2.6), 12.0, 0.8), ("right", 10.2, 13.0, (-2.8, 2.0, 0.2), -4.0, 0.8),
                      home("left", 20.0, 25.0), home("right", 23.0, 28.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the staff held crosswise as a guard bar at the brow, in front of the
        # cowl (the 8 px arms cannot lift it over the crown), the sun out to the left; no crouch (batch review 2026-11-11:
        # the right fist a px further out and lower, its bell sat in the cowl)
        "brace": KEY((4, 4, 0), (-16, 0, 0), (0.6, 2.0, 7.6), unit((1, 0.12, 0)), u=FWD, rp=(-150, 0, 10, -30),
                     lp=(-150, 0, -10, -20), lh=1, ls=9),
        # wall (Hold the line): the staff levelled at the foe in both hands like a pike, the sun head out front at chest
        # height, the butt by the right hip; the shoulders turn to bring the left hand forward on the shaft, the head stays on
        # the foe (v1's one-handed raised staff read as the guard from the front)
        "wall": KEY((8, 30, 0), (-2, -21, 0), (-1.3, -4.6, 6.0), aim(12, 15), u=FWD, rp=(-60, 0, 10, -30), lp=(-70, 0, -10, -30),
                    lh=1, ls=6),
        # ward (fall back): both hands on the staff, slanted across the body as a bar, low on the right, the sun high left
        "ward": KEY((2, 12, 0), (2, 0, 0), (1.2, -4.6, 6.3), unit((0.62, 0.56, 0.26)), u=FWD, rp=(-40, 0, 20, -40),
                    lp=(-80, 0, -10, -60), lh=1, ls=8),
        # shield_up (the caster's shield): the staff lifted high before the face in both fists and held upright, a pillar
        # between the Sunweaver and the blow, the sun over the cowl, the left fist above the right (review F3 2026-11-10: held
        # at the belly it read as the guard at 16 blocks)
        # (batch review 2026-11-11: the staff tipped 12 deg toward the foe and the fists 5.5 px apart, the left bell sat
        # on the face and on the right bell)
        # (fix round 1, 2026-11-12: the bells still met, 2.31 at t18.5: the right fist 3.5 px lower and 0.5 out, the left
        # 9.5 px up the shaft, the staff slanted 20 deg to the left so the left fist rises beside the cowl, not over the right)
        "shield_up": KEY((-2, 4, 0), (-6, 0, 0), (1.0, -2.3, 8.0), aim(76, 20), u=FWD, rp=(-120, 0, 10, -30),
                         lp=(-120, 0, -10, -30), lh=1, ls=9.5, lsw=1.0),
    }
    # the walk carries the staff higher in the fist (upper arm forward, forearm near level), so the longer butt clears the ground
    carry = {"walk": 75.0, "run": 35.0, "u": FWD, "R": (-30.0, 0.0, 6.0), "RF": -75.0}
    return g, attack, legs, stances, carry


def hexer_set():
    """The Gravecaller, caster style v2 (docs/anim-rebuild/CASTER-V2-DESIGN.md: the lantern thrust, the point and shoot made
    heavy and held). The guard: upright, the grave lantern held out before the belly on its short pole, hanging, the free hand
    relaxed at the hip. The thrust: the hips load back first, then the lantern is hauled in to the chest, the shoulders
    turning the right side away; the arm drives it out at the foe at chest height, slower than a wand, and holds it there at
    arm's length while the light gathers, a heavy tremor in the arm; the hips drive forward over the lead foot, the lantern
    heaves up a touch and is shoved at the foe, the blight leaving the lantern on the release tick; the lantern kicks up on
    its pole, the body comes back and settles into guard. The off hand never joins the cast: it hangs and swings back as the
    right side drives."""
    lp_rest = (-35, 0, -18, -60)          # the off hand clawed, held out low before the belly
    # hunched over the lantern, the head pushed forward under the hood, the lantern held out clear of the robe
    g = KEY((9, 10, 0), GAZE, (-1.8, -3.6, 6.6), aim(10, -6), u=UP, rp=(-55, 0, 20, -25), lp=lp_rest)
    # the load: the weight goes back onto the rear foot before the arm moves (the hips lead, see legs)
    load = nudge(g, torso=(-2, 4, 0), head=(-1, 0, 0), R=(-0.2, 0.3, 0.1), d=aim(16, -8))
    # the haul: the lantern drawn in to the chest, the pole tipped up, the right side turned away
    haul = KEY((-4, 10, 0), (2, 0, 0), (-2.2, -1.4, 6.2), aim(48, 8), u=UP, rp=(-80, 0, 35, -100), lp=(-18, 0, -12, -44))
    coil = nudge(haul, torso=(-1, 3, 0), R=(-0.1, 0.2, 0.1), d=aim(52, 10))
    # the push: the arm drives the lantern out at the foe, heavy, the shoulder coming round
    push = KEY((6, -10, 0), (3, 0, 0), (0.2, 0.2, 7.5), aim(14, 2), u=UP, rp=(-88, 0, 5, -15), lp=(-4, 0, -12, -38))
    # held at arm's length while the light gathers: the arm locked, a heavy tremor, the lantern swaying on its pole
    # (batch review 2026-11-11: 1.2 px out to the right, the hood sat on the cuff at full reach)
    held = KEY((7, -12, 0), (3, 0, 0), (-1.2, 0.4, 7.8), aim(9, 0), u=UP, rp=(-90, 0, 0, -5), lp=(-2, 0, -12, -36))
    held2 = nudge(held, torso=(0.5, -0.5, 0), R=(0.05, -0.12, -0.05), d=aim(7.5, 0.8))
    held3 = nudge(held, torso=(0.8, -1, 0), R=(-0.05, 0.1, -0.08), d=aim(10.5, -0.6))
    # the heave: the hips have gone forward; the shoulder holds back a beat and the lantern lifts a touch on its pole
    heave = nudge(held, torso=(-1, 2, 0), R=(0.0, 0.35, -0.5), d=aim(20, 0))
    # the release: the shoulder follows the hips through, the lantern is shoved at the foe and the blight leaves it
    release = KEY((14, -26, 0), (5, 0, 0), (0.0, 0.9, 7.9), aim(2, 0), u=UP, rp=(-90, 0, 0, -3), lp=(8, 0, -14, -30))
    # the kick: the lantern bounces up on its pole and the arm gives back through the shoulder
    kick = KEY((10, -20, 0), (4, 0, 0), (0.0, 2.2, 6.9), aim(36, 2), u=UP, rp=(-95, 0, 0, -20), lp=(5, 0, -12, -32))
    back = KEY((5, -8, 0), (4, 0, 0), (0.6, -1.6, 6.8), aim(10, -2), u=UP, rp=(-75, 0, 5, -20), lp=(-4, 0, -12, -38))
    recover = KEY((7, 6, 0), GAZE, (-1.4, -3.6, 6.6), aim(14, -5), u=UP, rp=(-55, 0, 10, -24), lp=lp_rest)
    near = nudge(g, torso=(1, -2, 0), R=(0.0, 0.2, 0.0), d=aim(13, -6))
    settle = nudge(g, R=(0.0, 0.05, 0.0))
    attack = [(0, g), (1.5, load), (3.0, haul), (4.6, coil), (6.4, push), (7.2, held), (8.0, held2), (8.8, held3),
              (9.4, heave), (IMPACT, release), (11.6, kick), (14.0, back), (17.0, recover), (22.0, near), (29.0, settle),
              (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    # the Knight's weight shift: the hips load back and turn the right side away before the arm moves, and drive forward,
    # the right side coming round over the lead foot, before the shoulder does
    legs = {"hips": [(0, R0, R1), (1.5, (-2.0, 22.0, 0.0), (0.0, -1.0, -0.4)), (4.6, (-3.0, 24.0, 0.0), (0.0, -1.1, -0.5)),
                     (6.4, (0.0, 14.0, 0.0), (0.0, -1.0, -0.1)), (8.6, (0.0, 13.0, 0.0), (0.0, -1.0, -0.1)),
                     (9.4, (6.0, 2.0, 0.0), (0.0, -1.3, 0.8)), (IMPACT, (8.0, -2.0, 0.0), (0.0, -1.5, 1.5)),
                     (11.8, (8.0, -4.0, 0.0), (0.0, -1.6, 1.6)), (16.0, (4.0, 6.0, 0.0), (0.0, -1.3, 0.7)),
                     (24.0, (1.0, 12.0, 0.0), (0.0, -1.0, 0.3)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides under the robe: the lead foot slides forward as the hips drive, the rear foot drags after
            # (batch review 2026-11-11: the drive carries 0.3 px further, the hips travelled 1.9 of the 2.0 a heavy cast needs)
            "steps": [("left", 8.2, 10.0, (2.9, 2.0, 2.6), 12.0, 0.8), ("right", 10.2, 13.0, (-2.8, 2.0, 0.2), -4.0, 0.8),
                      home("left", 20.0, 25.0), home("right", 23.0, 28.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the lantern lifted high over the head on its pole as a warding light,
        # the off hand raised flat beside it; no crouch
        "brace": KEY((-2, 4, 0), (2, 0, 0), (0.0, 5.0, 6.0), aim(30, 20), u=UP, rp=(-150, 0, 10, -30),
                     lp=(-165, 0, 5, -10)),   # batch review 2026-11-11: the raised off arm swung 25 deg off the hood (its cuff was in it)
        # wall (Hold the line): leaning in, the lantern held out at the foe at arm's length, high, the off hand pushed
        # forward open below it
        # (batch review 2026-11-11: the fist a px lower and out to the right, its cuff was in the hood)
        "wall": KEY((6, 2, 0), (0, 0, 0), (-1.4, 1.6, 7.5), aim(30, 0), u=UP, rp=(-120, 0, 5, -10), lp=(-95, 0, -20, -10)),
        # ward (fall back): drawn back, the head ducked, the lantern lifted up before the face at shoulder height to keep
        # the foe off with its light, the claw open out wide (review F1 2026-11-10: the old low ward read as the guard)
        "ward": KEY((-4, 6, 0), (6, 0, 0), (-4.4, 3.4, 5.4), aim(45, -40), u=UP, rp=(-130, 0, 30, -10),   # review: clear of the hood
                    lp=(-90, 0, -30, -50)),
        # shield_up: the body turned away, the lantern held up beside the hood on the arm raised out to the side, a light
        # to hide behind, the claw up before the face
        "shield_up": KEY((4, -10, 0), (2, 0, 0), (-4.8, 5.8, 1.0), aim(50, -90), u=UP, rp=(-150, 0, 60, -20),
                         lp=(-100, 0, -10, -10)),
    }
    carry = {"walk": 12.0, "run": 2.0, "u": UP}
    return g, attack, legs, stances, carry


def tc_mist_set():
    """The Tidecaller mist caller, caster style v2 (docs/anim-rebuild/CASTER-V2-DESIGN.md): the mist goes out through the 9 px
    driftwood rod and its conch, and the off hand never joins the cast. The guard: upright, the rod held forward and a little
    up at the foe, the free hand relaxed at the belt. Point and shoot: over ticks 0-4 the body turns side-on as the rod hand
    comes up past the chest, the conch tipping up; 4-9 the arm goes straight out at the foe, elbow locked, and the rod holds
    steady while the mist gathers; the wrist cocks, and on the release tick it flicks and the mist leaves the conch, a small
    recoil back through the shoulder; then back to the guard."""
    lp_rest = (10, 0, -40, -15)           # the off palm trailing low and back, loose
    # loose and side-on, the rod held out low before the lead hip
    g = KEY((2, -12, 0), GAZE, (-1.2, -4.5, 6.2), aim(18, -4), u=UP, rp=(-50, 0, 15, -20), lp=lp_rest)
    # the turn: the shoulders come round side-on and the rod hand rises past the chest, the conch tipping up
    turn = KEY((2, -16, 0), GAZE, (-1.0, -2.8, 6.6), aim(34, 16), u=UP, rp=(-60, 0, 20, -60), lp=(-10, 0, -36, -40))
    chest = KEY((1, -28, 0), GAZE, (-0.2, -0.6, 6.7), aim(52, 6), u=UP, rp=(-85, 0, 25, -85), lp=(-8, 0, -36, -42))
    # point: the arm straight out at the foe, the elbow locked, the rod level along it
    point = KEY((0, -32, 0), GAZE, (0.0, 0.5, 7.95), aim(2, 0), u=UP, rp=(-90, 0, 0, -5), lp=(-8, 0, -36, -42))
    # the charge: the rod holds steady, a breath of tremor only
    charge = nudge(point, R=(0.0, 0.05, -0.02), d=aim(2.5, 0.3))
    held = nudge(point, R=(0.0, 0.02, -0.03), d=aim(1.8, -0.2), lp=(-1, 0, 0, -1))
    # the wrist cocks up before it flicks (the long rod reads it at a distance: the flick spans 28 degrees)
    cock = nudge(point, R=(0.0, 0.15, -0.05), d=aim(20, 0))
    # the release: the flick down at the foe, the mist leaves the conch
    release = nudge(point, R=(0.0, 0.1, 0.0), d=aim(-8, 0))
    # the recoil back through the shoulder: the fist kicks up and back, the torso rocks back
    recoil = KEY((-2, -30, 0), GAZE, (0.0, 1.2, 6.7), aim(10, 2), u=UP, rp=(-95, 0, 0, -20), lp=(-10, 0, -36, -42))
    back = KEY((0, -24, 0), GAZE, (0.4, -0.4, 6.8), aim(6, 0), u=UP, rp=(-80, 0, 5, -25), lp=(-10, 0, -36, -42))
    recover = KEY((2, -14, 0), GAZE, (-0.6, -4.0, 6.4), aim(14, -4), u=UP, rp=(-55, 0, 10, -20), lp=lp_rest)
    near = nudge(g, torso=(0, -2, 0), R=(0.0, 0.2, 0.0), d=aim(16, -5))
    settle = nudge(g, R=(0.0, 0.05, 0.0))
    attack = [(0, g), (2.0, turn), (3.6, chest), (5.0, point), (7.0, charge), (8.6, held), (9.4, cock), (IMPACT, release),
              (11.4, recoil), (14.0, back), (19.0, recover), (24.0, near), (29.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    side_on = (0.0, -14.0, 0.0)
    # Batch review 2026-11-11 (Kort: a cast is a whole-body move): the hips load back as the body turns, carry forward over
    # the gliding right foot through the point and the charge, and drive on into the release (they travelled 0.5 px before)
    legs = {"hips": [(0, R0, R1), (2.0, (0.0, 2.0, 0.0), (0.0, -0.95, -0.5)), (4.5, side_on, (0.0, -1.05, 0.3)),
                     (9.0, (1.0, -15.0, 0.0), (0.0, -1.15, 0.9)), (IMPACT, (3.0, -15.0, 0.0), (0.0, -1.3, 1.6)),
                     (11.4, (1.0, -14.0, 0.0), (0.0, -1.2, 1.2)), (14.0, (0.0, -13.0, 0.0), (0.0, -1.05, 0.6)),
                     (22.0, (0.0, 4.0, 0.0), (0.0, -0.9, 0.1)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides: the right foot slides forward under the robe as the body turns side-on, the left eases back
            "steps": [("right", 0.8, 4.2, (-2.6, 2.0, 2.4), -24.0, 0.8), ("left", 2.0, 5.0, (2.9, 2.0, -0.4), 2.0, 0.8),
                      home("left", 18.0, 23.0), home("right", 21.0, 27.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the rod held overhead crosswise as a guard bar, the off hand
        # raised to its far end; no crouch
        # (batch review 2026-11-11: the rod fist a px out and the off arm 25 deg round, the right bell sleeve sat 2.7 px in
        # the hood; the left still touches it, see TC_MIST-REVIEW)
        "brace": KEY((-2, -6, 0), (2, 0, 0), (-1.5, 3.3, 6.2), aim(80, 0), u=FWD, rp=(-140, 0, 10, -30),
                     lp=(-110, 0, -50, -10)),
        # wall (Hold the line): side-on, the rod thrust out level at the foe, low, the off palm pressed out behind it
        "wall": KEY((6, -18, 0), (0, 0, 0), (-0.5, -1.0, 7.8), aim(-5, 0), u=UP, rp=(-90, 0, 5, -10), lp=(-60, 0, -30, -20)),
        # ward (fall back): turned away, the rod slanted across the body, the off hand up beside the face
        "ward": KEY((2, -25, 0), (0, 0, 0), (2.5, -2.0, 5.8), unit((0.7, 0.45, 0.5)), u=UP, rp=(-60, 0, 30, -70),
                    lp=(-70, 0, -18, -70)),   # (batch review 2026-11-11: the off arm 12 deg off the mantle, its bell was in it)
        # shield_up: the rod stood upright before the face, the off palm flat behind it, a veil of mist between
        "shield_up": KEY((2, -8, 0), (0, 0, 0), (1.0, 1.5, 6.5), aim(75, 0), u=FWD, rp=(-100, 0, 10, -40),
                         lp=(-95, 0, -5, -40)),
    }
    carry = {"walk": -20.0, "run": -5.0, "u": UP}
    return g, attack, legs, stances, carry


def wm_seer_set():
    """The Bloodhowl seer, caster style v2 (docs/anim-rebuild/CASTER-V2-DESIGN.md): the cast goes through the 6 px bone wand
    and the off hand never joins it. The guard: upright, the wand held low and forward at the foe, the free hand relaxed at
    the belt. Point and shoot: over ticks 0-4 the body turns side-on (the right shoulder to the foe, the right foot gliding
    forward) as the wand hand comes up past the chest; 4-9 the arm is thrust straight out at the foe, elbow locked, and the
    wand holds steady while it charges; the wrist cocks, and on the release tick it flicks and the bolt leaves the tip, a
    small recoil running back through the shoulder; then back to the guard. The totem staff stays strapped to the back."""
    lp_rest = (-25, 0, -45, -55)          # the off claw held out wide, ready to rake
    # crouched and feral, the chest over the knees, the wand held low and forward at the foe
    g = KEY((12, 6, 0), (0, 0, 0), (-1.0, -4.0, 6.5), aim(-10, -8), u=UP, rp=(-45, 0, 15, -25), lp=lp_rest)
    # the turn: the shoulders come round side-on and the wand hand rises past the chest, the wand tipping up
    turn = KEY((2, -16, 0), GAZE, (2.4, -3.2, 4.2), aim(30, 18), u=UP, rp=(-60, 0, 20, -60), lp=(-10, 0, -12, -40))
    chest = KEY((1, -28, 0), GAZE, (2.2, -0.6, 4.2), aim(58, 8), u=UP, rp=(-85, 0, 25, -85), lp=(-8, 0, -12, -42))
    # point: the arm straight out at the foe, the elbow locked, the wand level along it
    point = KEY((0, -32, 0), GAZE, (0.0, 0.5, 7.95), aim(2, 0), u=UP, rp=(-90, 0, 0, -5), lp=(-8, 0, -12, -42))
    # the charge: the wand holds steady, a breath of tremor only
    charge = nudge(point, R=(0.0, 0.05, -0.02), d=aim(2.5, 0.3))
    held = nudge(point, R=(0.0, 0.02, -0.03), d=aim(1.8, -0.2), lp=(-1, 0, 0, -1))
    # the wrist cocks up before it flicks (wide enough to read at a distance: the flick spans 30 degrees)
    cock = nudge(point, R=(0.0, 0.15, -0.05), d=aim(22, 0))
    # the release: the flick down at the foe, the bolt leaves the tip
    release = nudge(point, R=(0.0, 0.1, 0.0), d=aim(-8, 0))
    # the recoil back through the shoulder: the fist kicks up and back, the torso rocks back
    recoil = KEY((-2, -30, 0), GAZE, (0.0, 1.2, 6.7), aim(10, 2), u=UP, rp=(-95, 0, 0, -20), lp=(-10, 0, -12, -42))
    back = KEY((0, -24, 0), GAZE, (0.4, -0.4, 6.8), aim(4, 0), u=UP, rp=(-80, 0, 5, -25), lp=(-10, 0, -12, -42))
    recover = KEY((8, -2, 0), GAZE, (-0.6, -4.0, 6.5), aim(-6, -4), u=UP, rp=(-55, 0, 10, -20), lp=lp_rest)
    near = nudge(g, torso=(0, -2, 0), R=(0.0, 0.2, 0.0), d=aim(-9, -7))
    # (batch review 2026-11-11: the settle was 0.05 px off the guard and the cast froze for 5 ticks from tick 29; the body
    # now breathes down onto the guard, the wand tip dipping and coming back up)
    settle = nudge(g, torso=(2, -2, 0), R=(0.0, 0.35, -0.2), d=aim(-16, -12))
    rise = nudge(g, torso=(-1, 1, 0), R=(0.0, -0.15, 0.1), d=aim(-5, -5))   # (round 2: still 4 ticks; a breath back up)
    attack = [(0, g), (2.0, turn), (3.6, chest), (5.0, point), (7.0, charge), (8.6, held), (9.4, cock), (IMPACT, release),
              (11.4, recoil), (14.0, back), (19.0, recover), (24.0, near), (29.0, settle), (32.5, rise), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    side_on = (0.0, -14.0, 0.0)
    # Batch review 2026-11-11 (Kort: a cast is a whole-body move): the hips load back as the body turns, carry forward over
    # the gliding right foot through the point and the charge, and drive on into the release (they travelled 0.5 px before)
    legs = {"hips": [(0, R0, R1), (2.0, (0.0, 2.0, 0.0), (0.0, -0.95, -0.5)), (4.5, side_on, (0.0, -1.05, 0.3)),
                     (9.0, (1.0, -15.0, 0.0), (0.0, -1.15, 0.9)), (IMPACT, (3.0, -15.0, 0.0), (0.0, -1.3, 1.6)),
                     (11.4, (1.0, -14.0, 0.0), (0.0, -1.2, 1.2)), (14.0, (0.0, -13.0, 0.0), (0.0, -1.05, 0.6)),
                     (22.0, (0.0, 4.0, 0.0), (0.0, -0.9, 0.1)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides: the right foot slides forward under the robe as the body turns side-on, the left eases back
            "steps": [("right", 0.8, 4.2, (-2.6, 2.0, 2.4), -24.0, 0.8), ("left", 2.0, 5.0, (2.9, 2.0, -0.4), 2.0, 0.8),
                      home("left", 18.0, 23.0), home("right", 21.0, 27.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the wand held overhead crosswise as a guard bar, the off hand
        # raised flat to the other end; no crouch
        "brace": KEY((-4, 8, 0), (-6, 0, 0), (-1.5, 6.0, 3.0), aim(70, -10), u=FWD, rp=(-150, 0, 10, -30),
                     lp=(-125, 0, -10, -40)),   # (batch review 2026-11-11: the raised off arm 25 deg lower and 20 out, its cuff was in the head)
        # wall (Hold the line): crouched in, the wand thrust low at the foe's legs, the claw spread out wide beside it
        "wall": KEY((14, 4, 0), (0, 0, 0), (0.0, -2.5, 7.6), aim(-20, 0), u=UP, rp=(-80, 0, 5, -10), lp=(-50, 0, -40, -30)),
        # ward (fall back): the wand across the chest, the left forearm up across the muzzle
        # (batch review 2026-11-11: the wand fist 0.6 px further out from the chest, its cuff was in the shawl)
        "ward": KEY((6, 14, 0), (0, 0, 0), (1.2, -0.8, 7.4), unit((0.8, 0.2, 0.5)), u=UP, rp=(-60, 0, 30, -70),
                    lp=(-100, 0, -10, -90)),   # (batch review 2026-11-11: the forearm 10 deg lower, its cuff was in the head)
        # shield_up: the wand up before the face, the claw thrust out to rake whatever comes
        "shield_up": KEY((8, 6, 0), (0, 0, 0), (-0.5, 1.0, 7.2), aim(45, -10), u=FWD, rp=(-100, 0, 10, -40),
                         lp=(-85, 0, -30, -15)),
    }
    carry = {"walk": -30.0, "run": -10.0, "u": UP}
    return g, attack, legs, stances, carry


GUARD, ATTACK_KEYS, ATTACK_LEGS, STANCE_KEYS, CARRY = {"mage": mage_set, "hexer": hexer_set, "tc_mist": tc_mist_set,
                                                       "wm_seer": wm_seer_set}[TROOP]()


def attack_keys():
    return ATTACK_KEYS


def attack_legs():
    """Footwork (STYLE.md rule 4): the lead foot strides as the body coils back to gather; the hips drive and drop over the
    lead foot at the release, the rear foot drags through; both come back to the guard."""
    return K.attack_legs(ATTACK_LEGS, ticks=ATTACK_TICKS)


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
        self.lwarm = None
        self.lworst = 0.0

    def pose(self, legs, torso, head, R, d, u, rp, lp, q=0.0, Lq=QGRIP, extra=None, w=0.02, lh=0.0, ls=0.0, lsw=0.05):
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
        if lh > 1e-4:   # the left fist on the shaft (both-handed focus): solved, then blended in from the free arm by lh
            pose = fk_pose(s, legs)
            x, e = reach_left(pose, ls, lp, self.lwarm, lsw=lsw)
            self.lwarm = x
            if lh >= 0.999:
                self.lworst = max(self.lworst, e)
            lp = tuple(np.asarray(lp[:4], float) + min(1.0, lh) * (x[:4] - np.asarray(lp[:4], float)))
        else:
            self.lwarm = None
        s["left_arm"], s["left_forearm"] = (tuple(lp[:3]), None), ((lp[3], 0.0, 0.0), None)
        s["left_hand"] = (Z3, None)
        return s


def sample(keys, legs_at, sol, t, extra=None, w=0.02):
    v = {n: interp(keys, t, n) for n in ("torso", "head", "R", "d", "u", "rp", "lp", "q", "Lq", "lh", "ls", "lsw")}
    d, u = frame(v["d"], v["u"])
    head = head_local(v["head"], v["torso"], legs_at["hips"][0])
    return sol.pose(legs_at, v["torso"], head, v["R"], d, u, v["rp"], v["lp"], max(0.0, min(1.0, v["q"][0])), v["Lq"], extra, w,
                    max(0.0, min(1.0, v["lh"][0])), v["ls"][0], v["lsw"][0])


def gear(samples, legs=None):
    """Secondary motion (STYLE.md rule 9): the knight's cape lag, scaled onto each hanging piece of the troop's gear."""
    K.cape_lag(samples, legs)
    for s in samples:
        c = s.pop("cape")[0]
        for g, k in ST["gear"].items():
            s[g] = ((k * c[0], 0.0, k * c[2]), None)
    return samples


UNWIND = (22.0, 27.0, 31.0, 35.0, 0.0)   # off: the cast has no over-the-shoulder draw (kept for a troop that needs it)


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
        h = shaft_hits(fk_pose(s, legs[i]))
        if h:
            hits.append((t, h[0]))
        out.append(s)
    print("attack_cast: fist err max %.3f px (left on the shaft %.3f), samples with the focus in the body %d %s"
          % (sol.worst, sol.lworst, len(hits), hits[:8]))
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
    upper = gear(upper, legs)
    clip = K.bake(upper, ATTACK_TICKS, loop=False)
    rewind(clip)
    clip["timeline"] = {K.fmt_t(IMPACT / TPS): "impact"}
    return {"attack_cast": clip, "attack_cast_legs": K.bake(legs, ATTACK_TICKS, loop=False)}


# ------------------------------------------------------------------------------------------------
# Ability clips (work/anim-clip-spec-2026-11-07.md C3, C4, C6, C7): one per caster, named after its Spec ability id, built on the
# same keys, solver, gear lag and robe panels as the cast. Each is a once-clip from the guard back to the guard, with its own
# _legs footwork and a "timeline" impact marker on the release. The client plays it in place of attack_cast (AttackController
# .abilityClip): the server's cast event starts impactTicks (10) before the spell leaves; the clip is shifted so its impact
# marker lands on the tick the spell takes effect (the send for a mend, the windup's end for a blight or a mist). The howl has no
# cast event: the client starts it when the Seer's own howl speed modifier appears (AttackController.howl).
# ------------------------------------------------------------------------------------------------

def mend_sunweaver():
    """C3 the Sunweaver's heal, 20 ticks, release on 10 (the server sends the mend 10 ticks after the cast event's start). Calm
    and slow, unlike the attack cast's drive at the ally: a breath in, both hands lift the sun staff straight up to the sky, the
    chest opening and the head tipping back after it; at the release the left hand leaves the shaft and both arms open outward,
    the staff leaning out to the right, the light spreading; the bloom is held, then everything floats back to the guard."""
    g = GUARD
    breath = nudge(g, torso=(-1, -2, 0), head=(-2, -2, 0), R=(0.2, 0.3, 0.0), d=aim(85, -16))
    # both hands on the shaft, the left above the right (as the cast's raise), the staff slanting up across the chest
    # (batch review 2026-11-11: the fists out of the cowl and the vestment, the left 5.5 px up the shaft, clear of the right)
    gather = KEY((-3, 5, 0), (-6, -6, 0), (2.3, -2.3 - MAGE_DROP, 6.9), unit((0.35, 0.9, 0.2)), u=FWD, rp=(-80, 0, 10, -40),
                 lp=(-60, 0, -15, -50), lh=0.7, ls=MAGE_LS, lsw=1.0)
    up = KEY((-7, 2, 0), (-18, -4, 0), (2.9, 0.0 - MAGE_DROP, 6.6), unit((0.45, 0.86, 0.2)), u=FWD, rp=(-110, 0, 10, -40),
             lp=(-95, 0, -20, -50), lh=1, ls=MAGE_LS, lsw=1.0)
    peak = nudge(up, torso=(-1, 0, 0), head=(-3, 0, 0), R=(0.0, 0.3, 0.0))
    # the arms open: the right fist out to the right, the staff leaning out with it; the left arm raised wide, open
    opened = KEY((-9, 0, 0), (-20, 0, 0), (-4.2, 1.2, 5.6), unit((-0.5, 0.85, 0.0)), u=FWD, rp=(-100, 0, 30, -20),
                 lp=(-80, 0, -62, -12))
    bloom = nudge(opened, torso=(-1, 0, 0), head=(-2, 0, 0), R=(-0.2, 0.2, 0.0), lp=(-4, 0, -4, 0))
    recover = KEY((0, 6, 0), (-2, -2, 0), (-2.0, -2.5, 6.0), aim(86, -14), u=FWD, rp=(-50, 0, 10, -40), lp=(-25, 0, -20, -40))
    near = nudge(g, torso=(1, -1, 0), R=(0.0, 0.2, -0.1), d=aim(84, -14))
    keys = [(0, g), (2.0, breath), (4.6, gather), (7.4, up), (9.0, peak), (9.5, nudge(peak, lh=(0.5,))), (11.2, opened),
            (13.4, bloom), (16.0, recover), (18.4, near), (20, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (2.0, (0.0, 15.0, 0.0), (0.0, -1.05, 0.0)), (4.6, (-1.0, 13.0, 0.0), (0.0, -1.0, -0.2)),
                     (7.4, (-3.0, 11.0, 0.0), (0.0, -0.6, -0.3)), (11.2, (-3.0, 12.0, 0.0), (0.0, -0.55, -0.2)),
                     (13.4, (-2.0, 13.0, 0.0), (0.0, -0.65, -0.1)), (17.0, (0.0, 15.0, 0.0), (0.0, -0.9, 0.1)), (20, R0, R1)],
            "steps": []}
    return "mend_sunweaver", 20, 10, keys, legs


def blight_gravecaller():
    """C4 the Gravecaller's blight, 30 ticks: a 20-tick windup, the release on 20 (the server's windup end, when the blight
    strikes), a 10-tick recover. The grave lantern is hauled up overhead in the right hand while the left hand claws out at the
    foe; the body hunches and draws in, trembling, as the blight gathers; a last lift and arch, then both arms slam down, the
    lantern and the claw pointing at the foe's ground, the hips dropping; the body rebounds and settles into the guard."""
    g = GUARD
    load = nudge(g, torso=(-2, 4, 0), head=(-1, 0, 0), R=(0.0, 0.4, -0.4), d=aim(20, -8))
    claw = (-80, 0, 4, -40)        # the left arm out at the foe, the elbow bent: a claw
    # (batch review 2026-11-11: the fist rises outside the hood, the cuff went through the head on its way up)
    lift = KEY((0, 10, 0), (0, 0, 0), (-1.8, 3.0, 5.4), aim(60, 10), u=UP, rp=(-130, 0, 10, -40), lp=(-55, 0, -6, -45))
    over = KEY((4, 6, 0), (2, 0, 0), (-2.6, 7.4, 1.0), aim(50, -25), u=UP, rp=(-170, 0, 10, -30), lp=claw)
    # drawing in: the hunch deepens, the claw pulls in a little, a tremor in both arms
    hunch = KEY((10, 4, 0), (4, 0, 0), (-2.6, 7.3, 1.2), aim(48, -22), u=UP, rp=(-170, 0, 10, -30), lp=(-75, 0, 6, -55))
    hunch2 = nudge(hunch, torso=(1, -1, 0), R=(0.1, -0.15, 0.05), d=aim(46, -20), lp=(2, 0, 1, -3))
    hunch3 = nudge(hunch, torso=(2, 1, 0), R=(-0.1, 0.1, -0.05), d=aim(50, -24), lp=(-2, 0, -1, 2))
    hunch4 = nudge(hunch, torso=(3, 0, 0), R=(0.05, -0.1, 0.1), d=aim(47, -21), lp=(1, 0, 1, -5))
    # the last lift: the lantern up and back, the chest arching, the claw drawn back to strike
    # (batch review 2026-11-11: the claw drawn back 12 deg further off the hood, its cuff was 2.9 px in it)
    coil = KEY((-3, 4, 0), (-2, 0, 0), (-2.4, 7.6, 0.4), aim(62, -15), u=UP, rp=(-175, 0, 10, -30), lp=(-110, 0, 8, -60))
    # the slam: both arms down at the foe's ground, the torso driven forward
    slam = KEY((18, 0, 0), (12, 0, 0), (0.4, -2.6, 7.3), aim(-42, 0), u=UP, rp=(-75, 0, 0, -8), lp=(-55, 0, 0, -8))
    sink = nudge(slam, torso=(2, 0, 0), head=(1, 0, 0), R=(0.0, -0.3, -0.1), d=aim(-46, 0), lp=(3, 0, 0, 0))
    rebound = KEY((9, 2, 0), (6, 0, 0), (0.8, -2.0, 6.8), aim(-12, -2), u=UP, rp=(-70, 0, 5, -15), lp=(-35, 0, -8, -30))
    recover = KEY((4, 6, 0), GAZE, (1.1, -4.4, 6.1), aim(10, -5), u=UP, rp=(-50, 0, 10, -24), lp=(-15, 0, -12, -40))
    near = nudge(g, torso=(1, -1, 0), R=(0.0, 0.15, 0.0), d=aim(13, -6))
    keys = [(0, g), (2.0, load), (5.0, lift), (8.0, over), (10.5, hunch), (12.5, hunch2), (14.5, hunch3), (16.5, hunch4),
            (18.6, coil), (20, slam), (21.6, sink), (24.0, rebound), (27.0, recover), (28.8, near), (30, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (2.0, (-2.0, 20.0, 0.0), (0.0, -1.0, -0.4)), (8.0, (2.0, 18.0, 0.0), (0.0, -1.1, -0.3)),
                     (16.5, (5.0, 16.0, 0.0), (0.0, -1.35, -0.2)), (18.6, (-2.0, 18.0, 0.0), (0.0, -0.95, -0.4)),
                     (20, (8.0, 8.0, 0.0), (0.0, -1.6, 0.9)), (21.6, (9.0, 7.0, 0.0), (0.0, -1.7, 1.0)),
                     (24.0, (5.0, 10.0, 0.0), (0.0, -1.3, 0.6)), (28.0, (1.0, 15.0, 0.0), (0.0, -0.95, 0.2)), (30, R0, R1)],
            # the lead foot slides in under the slam, then back
            "steps": [("left", 18.6, 20.0, (2.9, 2.0, 2.2), 12.0, 0.3), home("left", 24.0, 28.5)]}
    return "blight_gravecaller", 30, 20, keys, legs


def mist():
    """C6 the Mist Caller's mist, 30 ticks, release on 20 (the server's windup end, when the cloud lands). Slow and tidal: the
    rod and the free hand draw up and across to the left as the hips wind that way; the body folds forward and both hands sink
    to the ground at the left, then sweep low across in a wide arc, the hips turning through to the right; at the release the
    sweep ends wide at the right, the conch flicking out, the mist laid; the body rises and drifts back to the guard."""
    g = GUARD
    draw = KEY((-2, -22, 0), (0, -10, 0), (4.6, -1.0, 5.0), aim(20, 40), u=UP, rp=(-90, 0, -10, -60), lp=(-40, 0, -45, -30))
    sink = KEY((30, -26, 0), (-12, -26, 0), (4.8, -5.2, 4.2), aim(-35, 55), u=UP, rp=(-70, 0, -15, -30), lp=(-45, 0, -55, -15))
    # (fix round 1, 2026-11-12: the sweep's bow 8 deg shallower; at 36-38 the head tipped back into the mantle to keep
    # its gaze, head and hood peak 2.0-2.1 px in the torso at ticks 11-17)
    sweep_l = KEY((28, -18, 0), (-14, -22, 0), (3.6, -6.0, 4.6), aim(-40, 40), u=UP, rp=(-60, 0, -5, -15), lp=(-55, 0, -40, -10))
    sweep_c = KEY((30, 0, 0), (-14, 0, 0), (0.6, -6.0, 4.2), aim(-40, 0), u=UP, rp=(-60, 0, 10, -10), lp=(-60, 0, -10, -10))
    sweep_r = KEY((26, 20, 0), (-12, 22, 0), (-3.6, -5.8, 4.4), aim(-36, -45), u=UP, rp=(-55, 0, 30, -10), lp=(-62, 0, 18, -12))
    # the release: the sweep ends wide at the right, the rod flicks out level, the free hand open behind it
    flick = KEY((20, 28, 0), (-6, 28, 0), (-4.8, -3.6, 4.6), aim(-8, -60), u=UP, rp=(-70, 0, 40, -8), lp=(-70, 0, 22, -8))
    drift = nudge(flick, torso=(-2, 1, 0), head=(-1, 0, 0), R=(-0.2, 0.3, 0.0), d=aim(-2, -62))
    rise = KEY((8, 12, 0), (0, 8, 0), (-1.0, -4.0, 5.8), aim(4, -25), u=UP, rp=(-55, 0, 20, -20), lp=(-30, 0, -10, -30))
    near = nudge(g, torso=(1, -2, 0), R=(0.0, 0.2, 0.0), d=aim(12, -9))
    keys = [(0, g), (4.0, draw), (8.0, sink), (11.5, sweep_l), (15.0, sweep_c), (18.0, sweep_r), (20, flick), (22.0, drift),
            (25.5, rise), (28.4, near), (30, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (4.0, (-1.0, -6.0, 0.0), (0.0, -0.85, -0.2)), (8.0, (4.0, -10.0, 0.0), (0.0, -1.5, 0.0)),
                     (11.5, (5.0, -2.0, 0.0), (0.0, -1.6, 0.2)), (15.0, (5.0, 14.0, 0.0), (0.0, -1.65, 0.3)),
                     (18.0, (4.0, 30.0, 0.0), (0.0, -1.6, 0.3)), (20, (3.0, 36.0, 0.0), (0.0, -1.45, 0.3)),
                     (22.0, (2.0, 35.0, 0.0), (0.0, -1.35, 0.25)), (25.5, (1.0, 22.0, 0.0), (0.0, -1.05, 0.15)), (30, R0, R1)],
            # the right foot glides out wide as the body folds, and back as it rises
            "steps": [("right", 5.0, 9.0, (-3.4, 2.0, -0.6), -14.0, 0.3), home("right", 23.0, 28.0)]}
    return "mist", 30, 20, keys, legs


def howl():
    """C7 the Bloodhowl Seer's howl, 25 ticks: gather 6 (a crouch and an inhale, the arms drawn in), the howl held 14 with a
    slight shake (the head thrown back, the chest pushed out and up, both arms spread wide and down, the bone wand in the right
    fist), release 5 back to the guard. The impact marker is the howl's onset (6)."""
    g = GUARD
    crouch = KEY((14, 4, 0), (10, 0, 0), (1.6, -4.0, 4.6), aim(-30, 10), u=UP, rp=(-45, 0, 15, -70), lp=(-30, 0, 5, -70))
    inhale = nudge(crouch, torso=(2, 0, 0), head=(2, 0, 0), R=(0.0, -0.2, -0.2))
    spread = KEY((-16, 0, 0), (-34, 0, 0), (-5.6, -4.2, 2.6), aim(-30, -80), u=UP, rp=(-25, 0, 55, -10), lp=(-20, 0, -58, -10))
    keys = [(0, g), (3.0, crouch), (5.0, inhale), (7.0, spread)]
    # the howl held: a slight shake (head, chest and both arms tremble on a short, uneven beat)
    shake = [((0.0, 0.0, 1.2), (0.0, 2.5, 0.0), (0.1, 0.12, 0.0)), ((0.6, 0.0, -1.0), (0.0, -2.0, 0.0), (-0.1, -0.1, 0.05)),
             ((0.0, 0.0, 0.9), (0.0, 1.8, 0.0), (0.05, 0.1, -0.05)), ((0.5, 0.0, -1.2), (0.0, -2.4, 0.0), (-0.1, -0.12, 0.0))]
    t, j = 8.2, 0
    while t < 18.5:
        tz, hd, r = shake[j % len(shake)]
        keys.append((t, nudge(spread, torso=tz, head=hd, R=r, lp=(0, 0, (1.5 if j % 2 else -1.5), 0))))
        t += 1.3
        j += 1
    # batch review 2026-11-11: the wand comes back from wide right to the guard through a key half way, aimed half way: in
    # one span the hand's turn rolled the wand 20 deg about its own length (tick 21.75)
    back = KEY((-1, 8, 0), (-15, 0, 0), (-3.3, -4.1, 4.6), aim(-20, -44), u=UP, rp=(-35, 0, 35, -17), lp=(-22, 0, -51, -32))
    keys += [(19.5, nudge(spread, torso=(2, 0, 0), head=(4, 0, 0))), (21.0, back), (22.5, nudge(g, torso=(2, 0, 0), R=(0.0, -0.3, 0.0))),
             (25, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (3.0, (5.0, 15.0, 0.0), (0.0, -1.45, -0.3)), (5.0, (6.0, 15.0, 0.0), (0.0, -1.55, -0.35)),
                     (7.0, (-4.0, 16.0, 0.0), (0.0, -0.6, 0.1)), (19.5, (-3.0, 16.0, 0.0), (0.0, -0.65, 0.1)),
                     (22.5, (0.0, 16.0, 0.0), (0.0, -0.85, 0.1)), (25, R0, R1)],
            "steps": []}
    return "howl", 25, 6, keys, legs


ABILITIES = {"mage": mend_sunweaver, "hexer": blight_gravecaller, "tc_mist": mist, "wm_seer": howl}


def ability_clips():
    name, ticks, release, keys, spec = ABILITIES[TROOP]()
    legs = K.attack_legs(spec, ticks=ticks)
    sol = Solver()
    out, hits = [], []
    for i in range(ticks * SUB + 1):
        t = i / SUB
        s = sample(keys, legs[i], sol, t)
        h = shaft_hits(fk_pose(s, legs[i]))
        if h:
            hits.append((t, h[0]))
        out.append(s)
    print("%s: fist err max %.3f px (left on the shaft %.3f), samples with the focus in the body %d %s"
          % (name, sol.worst, sol.lworst, len(hits), hits[:8]))
    if hits:
        print("  focus-in-body ticks:", sorted({(round(t), h[1]) for t, h in hits}))
    upper = gear(out, legs)
    clip = K.bake(upper, ticks, loop=False)
    rewind(clip)
    clip["timeline"] = {K.fmt_t(release / TPS): "impact"}
    return {name: clip, name + "_legs": K.bake(legs, ticks, loop=False)}


ASSET_ANIMS = HERE.parents[2] / "src/main/resources/assets/gravesandglory/animations/troop"


def splice(clips, paths):
    """Adds (or, on a re-run, replaces in place) only these clips in each animation file, after attack_cast_legs; every other
    clip stays byte for byte as it was."""
    for path in paths:
        doc = json.loads(path.read_text(encoding="utf-8"))
        old = {k: v for k, v in doc["animations"].items() if k not in clips}
        new, placed = {}, False
        for k, v in old.items():
            new[k] = v
            if k == "attack_cast_legs":
                new.update(clips)
                placed = True
        if not placed:
            new.update(clips)
        doc["animations"] = new
        hang_swing.key_doc(TROOP, doc, log=False)   # a hanging focus (the hexer's lantern): its swing re-keyed over every clip
        path.write_bytes(json.dumps(doc, indent=1).encode("utf-8"))
        print("spliced", ", ".join(clips), "into", path)


# ------------------------------------------------------------------------------------------------
# Batch review 2026-11-11 (docs/anim-rebuild/<TROOP>-REVIEW.md): the whole unit alive in every action clip, and the feet on the
# ground in the reaction layers. Kort: "i want the whole unit to be animated including pieces of its armor"; robes, hoods,
# mantles and fringes move in every action clip and never pass through the body.
#
# life_pass  every hanging piece (focus_check.ALIVE_KINDS: cape, hood tail, kelp, shawl, feather mantle, sash, fringes, the robe
#            panels, the shoulder plates) swings on fullbody.soft_motion's damped spring behind its parent, ADDED to whatever the
#            clip already keys for it (the cast's cape lag, the stun's plumb hang, the robe solve), so nothing else changes. A
#            robe panel only opens outward on its out axis (the solve's least opening keeps the legs inside) and swings freely
#            in its own plane. A loop (the stun) gets the spring's steady lap, closed on its loop tick.
# floor_pass a reaction layer (stun, knockback, pulled) whose foot dips under the ground is lifted at the hips by the dip,
#            eased in and out over a tick each side: never the whole clip.
# The builder's own clips get these in main(); the shared layers (art/anim/common/reactions.py: stun, stun_out, knockback,
# pulled) are merged into the exported file after the builder runs, so GG_CAST_REVIEW=layers post-processes them there and
# must be re-run after any reactions.py merge for this troop.
# ------------------------------------------------------------------------------------------------

DT = 1.0 / (SUB * TPS)
STUN_FLARE = 7.0      # deg the robe hem flares out on the stun's snap (its first four ticks)
LIFE_MIN = 5.0        # deg: the least a piece swings in a once-clip (focus_check.secondary_life warns under 4, fails under 2)


def _chan_at(ch, t, length=None, loop=False):
    if ch is None:
        return None
    if isinstance(ch, list):
        return [float(v) for v in ch]
    ks = sorted(ch, key=float)
    ts = [float(k) for k in ks]

    def vec(v):
        v = v["vector"] if isinstance(v, dict) else v
        return [float(x) for x in v]
    if loop and length:
        t = t % length if t < length - 1e-9 else t
    if t <= ts[0]:
        return vec(ch[ks[0]])
    if t >= ts[-1]:
        return vec(ch[ks[-1]])
    j = next(i for i in range(1, len(ts)) if t <= ts[i])
    a, b = vec(ch[ks[j - 1]]), vec(ch[ks[j]])
    f = (t - ts[j - 1]) / (ts[j] - ts[j - 1])
    return [x + f * (y - x) for x, y in zip(a, b)]


def clip_samples(clip, n, t0=0.0):
    """{bone: (rot, pos|None)} on the bake grid (SUB a tick) for n + 1 samples from t0 (a loop clip wraps)."""
    L, loop = float(clip["animation_length"]), bool(clip.get("loop"))
    out = []
    for i in range(n + 1):
        t = t0 + i * DT
        s = {}
        for b, ch in clip["bones"].items():
            r = _chan_at(ch.get("rotation"), t, L, loop)
            p = _chan_at(ch.get("position"), t, L, loop)
            s[b] = (tuple(r) if r else Z3, tuple(p) if p else None)
        out.append(s)
    return out


def write_bone(clip, bone, rots, poss):
    """Re-keys one bone of a baked clip from per-sample values (thinned as K.bake does); every other bone is untouched."""
    cols = [K.unwrap([r[i] for r in rots]) for i in range(3)]
    rv = [[round(cols[0][i], 3), round(cols[1][i], 3), round(cols[2][i], 3)] for i in range(len(rots))]
    ch = {"rotation": {K.fmt_t(i * DT): rv[i] for i in K.thin(rv, K.ROT_TOL)}}
    if any(p is not None and any(abs(v) > 1e-4 for v in p) for p in poss):
        pv = [[round(v, 4) for v in (p or Z3)] for p in poss]
        ch["position"] = {K.fmt_t(i * DT): pv[i] for i in K.thin(pv, K.POS_TOL)}
    clip["bones"][bone] = ch


def _plate_parts(model):
    """A shoulder plate hung on the torso (the hexer's) is not one fullbody.plates_for() rides on an arm: it gets a stiff
    little spring of its own (the gear kind, capped at 6 deg) so it rocks with the chest and never leaves the shoulder."""
    import focus_check as fc
    out = {}
    for n, (kind, att, tip, length) in fc.alive_pieces(model).items():
        bone = model.bones[n]
        if kind != "plate" or str(bone.parent).endswith("_arm") or n not in K.MODEL.bones:
            continue
        pbox = fc.rest_box(model, bone.parent)
        axis = (pbox.min(axis=0) + pbox.max(axis=0)) / 2.0 if len(pbox) else np.array(model.bones[bone.parent].pivot, float)
        c = dict(fullbody.SOFT_KINDS["gear"])
        c.update(bones=(n,), parent=bone.parent, pivot=(-att[0], att[1], att[2]), tip=(-tip[0], tip[1], tip[2]),
                 axis=(-axis[0], axis[1], axis[2]), kind="gear", max=6.0, gain=1.4, period=3.4, sway=2.0)
        out[n] = c
    return out


def _parts():
    model = fullbody._full_model(TROOP)
    parts = fullbody.soft_parts_for(model)
    parts.update(_plate_parts(model))
    return parts


def _sub(a, b):
    return tuple(K.wrap(x - y) for x, y in zip(a, b))


def _add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def _panel_life(P, e):
    """A robe panel's swing: free in its own plane, outward only on its out axis (the robe solve already holds the least
    opening that keeps the legs inside it)."""
    a = PANELS[P][0]
    e = list(e)
    i = 0 if a == 2 else 2                 # the out axis: x for the front and back panels, z for the sides
    if e[i] * _SIGN[P] < 0:
        e[i] = 0.0
    return tuple(e)


def _spring(upper, legs, parts):
    """fullbody.soft_motion on copies: {bone: (per-sample rotation delta, per-sample position delta)} from its first sample."""
    U = [dict(s) for s in upper]
    L = [dict(s) for s in legs]
    fullbody.soft_motion(U, L, parts, None, report=False)
    plates = fullbody.plates_for()
    if plates:
        U2 = [dict(s) for s in upper]
        fullbody.soft_motion(U2, [dict(s) for s in legs], {}, plates, report=False)
    out = {}
    for name, c in list(parts.items()) + [(p, {"bones": (p,), "clip": "upper"}) for p in plates]:
        for b in c["bones"]:
            src = (L if c.get("clip") == "legs" else (U2 if b in plates else U))
            if b not in src[0]:
                continue
            r0 = src[0][b][0]
            p0 = src[0][b][1] or Z3
            out[b] = ([_sub(s[b][0], r0) for s in src], [_sub(s[b][1] or Z3, p0) for s in src], c.get("clip") == "legs")
    return out


CLOTH_SINK = 0.5     # px a hanging piece may sink into the body it hangs on beyond what its first sample has


def _unsink(b, U, L, rots, poss):
    """Eases a hanging piece back toward its first sample's pose on every sample where it would sink into the hard boxes of
    the bone it hangs on (the cape into the chest on a yank) deeper than the first sample has it, by the least that clears."""
    model = fullbody._full_model(TROOP)
    par = model.bones[b].parent
    mine = [(model.cube_bone[ci], c) for ci, c in enumerate(model.cube_list) if model.cube_bone[ci] == b]
    body = [(model.cube_bone[ci], c) for ci, c in enumerate(model.cube_list) if model.cube_bone[ci] == par]
    if not mine or not body:
        return rots, poss

    def depth(i, r, p):
        pose = {}
        for src in (L[i] if L else {}, U[i]):
            for bn, (rr, pp) in src.items():
                if bn in K.MODEL.bones:
                    pose[bn] = (rr, pp if pp is not None else Z3)
        pose[b] = (r, p if p is not None else Z3)
        A = [fullbody._cube_world(fullbody._bone_world(pose, bn, model), c) for bn, c in mine]
        Bx = [fullbody._cube_world(fullbody._bone_world(pose, bn, model), c) for bn, c in body]
        return fullbody._sink(A, Bx)
    d0 = depth(0, rots[0], poss[0])
    out_r, out_p = list(rots), list(poss)
    for i in range(1, len(rots)):
        if depth(i, rots[i], poss[i]) - d0 <= CLOTH_SINK:
            continue
        lo, hi = 0.0, 1.0                  # the share of the way back to the first sample's pose
        for _ in range(7):
            mid = 0.5 * (lo + hi)
            r = tuple(x + mid * (y - x) for x, y in zip(rots[i], rots[0]))
            if depth(i, r, poss[i]) - d0 > CLOTH_SINK:
                lo = mid
            else:
                hi = mid
        out_r[i] = tuple(x + hi * (y - x) for x, y in zip(rots[i], rots[0]))
    # the eased samples blended into their neighbours over half a tick, so the piece is never snapped
    h = max(1, SUB // 2)
    sm = [tuple(np.mean([out_r[j][k] for j in range(max(0, i - h), min(len(out_r), i + h + 1))]) for k in range(3))
          if out_r[i] != rots[i] or any(out_r[j] != rots[j] for j in range(max(0, i - h), min(len(out_r), i + h + 1))) else out_r[i]
          for i in range(len(out_r))]
    return sm, out_p


def _swing(d):
    return max(math.sqrt(sum(v * v for v in e)) for e in d)


# The seer's feather mantle and shawl are short stiff pieces: soft_motion drives them into its clamp and they flip from one
# end of it to the other in a sample (pops of 14-21 deg in howl, the side blows and pulled). Their swing may turn at most
# this many degrees a sample, run forward and back and averaged so the series keeps its first and last value.
LIFE_RATE = {"wm_seer": 2.5}.get(TROOP)


def _rate(d, cap):
    def run(seq):
        out = [seq[0]]
        for e in seq[1:]:
            out.append(tuple(p + max(-cap, min(cap, v - p)) for p, v in zip(out[-1], e)))
        return out
    f, b = run(d), run(d[::-1])[::-1]
    return [tuple(0.5 * (x + y) for x, y in zip(u, v)) for u, v in zip(f, b)]


def life_pass(upper_clip, legs_clip, loop_from=None, report=True, name="", upper_bones=()):
    """Adds the spring swing of every hanging piece to a baked upper clip (and its legs clip). loop_from: the tick a
    looping clip's loop starts on (the stun: 4); the spring then runs three laps and the steady middle lap is used."""
    ticks = int(round(float(upper_clip["animation_length"]) * TPS))
    n = ticks * SUB
    nl = int(round(float(legs_clip["animation_length"]) * TPS)) * SUB if legs_clip else n
    N = max(n, nl)                         # hit_block: its legs run two ticks past its upper body
    if loop_from is None:
        up, lg = clip_samples(upper_clip, N), clip_samples(legs_clip, N) if legs_clip else [{} for _ in range(N + 1)]
    else:                                  # entry, then three laps: the fade-out of soft_motion lands on the third
        m = (ticks - loop_from) * SUB
        idx = list(range(n + 1)) + [loop_from * SUB + (k % m) for k in range(1, 3 * m + 1)]
        U0, L0 = clip_samples(upper_clip, n), clip_samples(legs_clip, n)
        up, lg = [U0[i] for i in idx], [L0[i] for i in idx]
    parts = _parts()

    def keyed_upper(b, in_legs):           # a piece the builder keys in the upper clip (the hexer's vestment), or that the guard
        #                                    under an additive hit keys there (tc_mist's sash tail), stays in the upper clip: a
        #                                    bone may be keyed by one of two clips played together, never both
        return in_legs and not ((b in upper_clip["bones"] or b in upper_bones) and not (legs_clip and b in legs_clip["bones"]))

    D = {b: (dr, dp, keyed_upper(b, il)) for b, (dr, dp, il) in _spring(up, lg, parts).items()}
    if loop_from is None and nl != n:      # each clip's pieces settle by its own end (soft_motion fades out on the last sample)
        Du = _spring(up[:n + 1], lg[:n + 1], parts)
        D = {b: ((Du[b][0], Du[b][1], False) if not v[2] and b in Du else v) for b, v in D.items()}
    U0 = clip_samples(upper_clip, n)
    L0 = clip_samples(legs_clip, nl) if legs_clip else None
    for b, (dr, dp, in_legs) in D.items():
        k_end = nl if in_legs else n
        if loop_from is not None:          # the steady lap (the second), closed on the loop tick; the entry blends onto it
            m = (ticks - loop_from) * SUB
            lap = [(dr[n + m + k], dp[n + m + k]) for k in range(m + 1)]
            gap_r, gap_p = _sub(lap[-1][0], lap[0][0]), _sub(lap[-1][1], lap[0][1])
            lap = [(_sub(r, tuple(g * k / m for g in gap_r)), _sub(p, tuple(g * k / m for g in gap_p)))
                   for k, (r, p) in enumerate(lap)]
            r_out, p_out = [], []
            for i in range(n + 1):
                if i >= loop_from * SUB:
                    r, p = lap[i - loop_from * SUB]
                else:
                    w = K.smooth(max(0.0, min(1.0, (i / SUB - (loop_from - 2.0)) / 2.0)))
                    lr, lp = lap[0]
                    r = tuple((1 - w) * x + w * y for x, y in zip(dr[i], lr))
                    p = tuple((1 - w) * x + w * y for x, y in zip(dp[i], lp))
                r_out.append(r)
                p_out.append(p)
            for i in range(SUB + 1):       # the clip loops end to start: its first tick eases out of the lap's last pose
                w = K.smooth(i / SUB)       # (tc_mist's kelp hood stood 23 deg apart across the wrap)
                r_out[i] = tuple((1 - w) * x + w * y for x, y in zip(lap[-1][0], r_out[i]))
                p_out[i] = tuple((1 - w) * x + w * y for x, y in zip(lap[-1][1], p_out[i]))
            dr, dp = r_out, p_out
            if b in PANELS:                # the snap that buckles the knees flares the hem out, and it falls back by the loop
                io = 0 if PANELS[b][0] == 2 else 2
                for i in range(int(loop_from * SUB) + 1):
                    e = list(dr[i])
                    e[io] += _SIGN[b] * STUN_FLARE * math.sin(math.pi * i / (loop_from * SUB))
                    dr[i] = tuple(e)
        else:
            dr, dp = dr[:k_end + 1], dp[:k_end + 1]
        if b in PANELS:
            dr = [_panel_life(b, e) for e in dr]
        if LIFE_RATE is not None:
            dr = _rate(dr, LIFE_RATE)
        sw = _swing(dr)
        if 1e-3 < sw < LIFE_MIN:           # a small blow still shows on the cloth: the same swing, scaled up to the least
            k = min(3.0, LIFE_MIN / sw)
            dr = [tuple(k * v for v in e) for e in dr]
            dp = [tuple(k * v for v in e) for e in dp]
        clip, S = (legs_clip, L0) if in_legs else (upper_clip, U0)
        if clip is None:
            continue
        rots = [_add(S[i].get(b, (Z3, None))[0], dr[i]) for i in range(k_end + 1)]
        poss = [_add(S[i].get(b, (Z3, None))[1] or Z3, dp[i]) for i in range(k_end + 1)]
        if not any(any(abs(v) > 1e-3 for v in p) for p in dp):
            poss = [S[i].get(b, (Z3, None))[1] for i in range(k_end + 1)]
        if not in_legs and b not in PANELS and parts.get(b, {}).get("kind") in ("cloth", "plume"):
            rots, poss = _unsink(b, U0, L0, rots, poss)
        write_bone(clip, b, rots, poss)
        if report:
            print("  life %-22s %-20s +%4.1f deg%s" % (name, b, _swing(dr), " (legs clip)" if in_legs else ""))
    return upper_clip, legs_clip


def _on_guard(clip, base):
    """A copy of an additive clip with the base clip's first pose added on every key (HitController adds a hit's keys
    onto the pose under it)."""
    c = json.loads(json.dumps(clip))
    for b, chans in base["bones"].items():
        for ch in ("rotation", "position"):
            if ch not in chans:
                continue
            v0 = _chan_at(chans[ch], 0.0)
            mine = c["bones"].setdefault(b, {}).get(ch)
            if mine is None:
                c["bones"][b][ch] = [round(x, 4) for x in v0]
            elif isinstance(mine, list):
                c["bones"][b][ch] = [round(x + y, 4) for x, y in zip(mine, v0)]
            else:
                for k, v in mine.items():
                    vec = v["vector"] if isinstance(v, dict) else v
                    new = [round(x + y, 4) for x, y in zip(vec, v0)]
                    if isinstance(v, dict):
                        v["vector"] = new
                    else:
                        mine[k] = new
    return c


# wm_seer: the pulled hit's left hoof sank 1.2 px with the reaction played on the guard, though not on the bind pose
FLOOR_ON_GUARD = TROOP == "wm_seer"


def _handover_legs(stun_legs, ready_legs, length, ticks=8.0):
    """A legs clip of `length` s: the stun legs giving way to the stance legs over `ticks`, as StanceController plays
    them under the stun shake-off (the spring's driver only, never written to a file)."""
    n = int(round(length * TPS)) * SUB
    S, R = clip_samples(stun_legs, n), clip_samples(ready_legs, n)
    out = {"loop": False, "animation_length": length, "bones": {}}
    for b in set(stun_legs["bones"]) | set(ready_legs["bones"]):
        rots, poss = [], []
        for i in range(n + 1):
            w = min(1.0, i / (ticks * SUB))
            rs, ps = S[i].get(b, (Z3, None))
            rr, pr = R[i].get(b, (Z3, None))
            rots.append(tuple((1 - w) * x + w * y for x, y in zip(rs, rr)))
            poss.append(tuple((1 - w) * x + w * y for x, y in zip(ps or Z3, pr or Z3)))
        write_bone(out, b, rots, poss)
    return out


def floor_lift(doc, upper, legs, margin=0.3, report=True, on_guard=False):
    """Lifts the hips of the legs clip wherever a hard corner of the pair (played the way focus_check plays it) sinks under
    the ground by more than `margin` px, by the sink, eased over a tick each side. Returns the most it lifts."""
    sys.path.insert(0, str(HERE.parents[2] / "tools" / "anim"))
    import focus_check as fc
    import render_geo as rg
    import scorecard as sc
    geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, TROOP)
    model = rg.load_geo(str(HERE.parents[2] / geo))
    rig = sc.Rig(model, sc.rig_rules_for(str(HERE.parents[2]), geo), TROOP)
    hard = fc.Parts(rig).hard()
    rest = rg.Scene(rig.model, None, [], 0.0).evaluate(0.0)
    sole = max(0.0, -float(rest.corners[hard][:, :, 1].min()) * 16.0)
    lc = doc["animations"][legs]
    ticks = int(round(float(doc["animations"][upper]["animation_length"]) * TPS))
    n = ticks * SUB
    judged = doc
    if on_guard:                           # an additive reaction is judged as the game plays it: on the guard's first pose
        A = doc["animations"]
        judged = {"animations": {legs: _on_guard(A[legs], A["stance_ready_legs"]),
                                 upper: _on_guard(A[upper], A["stance_ready_upper"])}}
    clips = [rg.load_clip(judged, legs), rg.load_clip(judged, upper)]
    scene = rg.Scene(rig.model, None, clips, 0.0)
    L = clips[-1].length                   # ticks, as focus_check plays it
    sink = np.array([max(0.0, -float(scene.evaluate(min(i / SUB, L - 1e-4)).corners[hard][:, :, 1].min()) * 16.0 - sole - margin)
                     for i in range(n + 1)])
    if sink.max() <= 0:
        return 0.0
    wide = np.array([sink[max(0, i - SUB):i + SUB + 1].max() for i in range(n + 1)])
    lift = np.array([wide[max(0, i - SUB // 2):i + SUB // 2 + 1].mean() for i in range(n + 1)])
    nl = int(round(float(lc["animation_length"]) * TPS)) * SUB
    S = clip_samples(lc, nl)
    rots = [S[i]["hips"][0] for i in range(nl + 1)]
    poss = [_add(S[i]["hips"][1] or Z3, (0.0, float(lift[min(i, n)]) if i <= n else 0.0, 0.0)) for i in range(nl + 1)]
    write_bone(lc, "hips", rots, poss)
    if report:
        k = int(np.argmax(lift))
        print("  floor %-20s hips lifted up to %.2f px (tick %.2f)" % (legs, float(lift[k]), k / SUB))
    return float(lift.max())


LAYER_LIFE = (("stun_upper", "stun_legs", 4), ("stun_out_upper", "stun_legs", None), ("hit_knockback", "hit_knockback_legs", None),
              ("hit_pulled", "hit_pulled_legs", None))
LAYER_FLOOR = (("stun_upper", "stun_legs"), ("hit_knockback", "hit_knockback_legs"), ("hit_pulled", "hit_pulled_legs"))


def action_pairs(anims):
    """(upper, legs) of every action clip the builder makes: the cast, the ability, every hit and the block."""
    return [(n, n + "_legs") for n in anims if (n == "attack_cast" or n in ABILITY_NAMES or n.startswith("hit_"))
            and not n.endswith("_legs") and n + "_legs" in anims]


ABILITY_NAMES = ("mend_sunweaver", "blight_gravecaller", "mist", "howl")


def alive_clips(anims):
    if "hit_block" in anims and "hit_light_front" in anims and anims["hit_block"] is anims["hit_light_front"]:
        anims["hit_block"] = json.loads(json.dumps(anims["hit_light_front"]))
    guard = anims.get("stance_ready_upper") or json.loads(
        (ASSET_ANIMS / f"{TROOP}.animation.json").read_text(encoding="utf-8"))["animations"]["stance_ready_upper"]
    for up, lg in action_pairs(anims):
        life_pass(anims[up], anims[lg], None, name=up, upper_bones=set(guard["bones"]) if up.startswith("hit_") else ())
    return anims


def replace_clips(clips, paths):
    """Replaces these clips in place in each animation file (a new one goes at the end); every other clip stays as it was."""
    for path in paths:
        doc = json.loads(path.read_text(encoding="utf-8"))
        A = doc["animations"]
        for k, v in clips.items():
            A[k] = v
        hang_swing.key_doc(TROOP, doc, log=False)   # a hanging focus (the hexer's lantern): its swing re-keyed over every clip
        path.write_bytes(json.dumps(doc, indent=1).encode("utf-8"))
        print("replaced", ", ".join(clips), "in", path)


# Fix round 1 (2026-11-12): per troop, builder clip: [(bone, rotation added, from tick, to tick, ramp ticks)]. Added onto
# the baked keys before the life pass (main and GG_CAST_ONLY), smoothstepped in and out over the ramp; a stance loop takes
# it on every key (None, None), so its wrap stays closed. Each entry's reason is beside it.
ALL = (None, None, 1.0)
CLIP_NUDGE = {
    # tc_mist brace: both bells sat in the hood (left 2.62 at t29.5, right 2.30 at t37): both arms 20-25 deg lower
    # shield_up: the left bell in the hood (2.44 at t3.5): the left arm 20 lower, the rod arm 12 lower and 5 out
    # mist: the right bell in the mantle on the low sweep (2.13 at t13): the rod arm 10 out over ticks 5-16; the head,
    # tipped back to keep its gaze over the bowed body, sat in the torso (2.1 at t17): 15 deg less over 8-18
    "tc_mist": {"stance_brace_upper": [("left_arm", (25, 0, 0)) + ALL, ("right_arm", (20, 0, 0)) + ALL],
                "stance_shield_up_upper": [("left_arm", (20, 0, 0)) + ALL, ("right_arm", (12, 0, 5)) + ALL],
                "mist": [("right_arm", (0, 0, 10), 5, 16, 3), ("head", (15, 0, 0), 8, 18, 3)]},
    # mage shield_up: with the staff slanted the cowl met the left bell (2.2): the head tips 8 up and turns 8 to the right
    "mage": {"stance_shield_up_upper": [("head", (-8, 8, 0)) + ALL]},
    # wm_seer cast: with the shoulder tusks sunk into the pad, the head turning to the foe met the right pad (1.96 at t4):
    # the head 5 deg up over ticks 2-6
    "wm_seer": {"attack_cast": [("head", (-5, 0, 0), 2, 6, 2)]},
}


def _ramp(tk, t0, t1, r):
    if t0 is None:
        return 1.0
    if tk <= t0 - r or tk >= t1 + r:
        return 0.0
    if t0 <= tk <= t1:
        return 1.0
    x = (tk - (t0 - r)) / r if tk < t0 else ((t1 + r) - tk) / r
    return x * x * (3 - 2 * x)


def clip_nudge(clips):
    for clip, ns in CLIP_NUDGE.get(TROOP, {}).items():
        if clip not in clips:
            continue
        for b, d, t0, t1, r in ns:
            ch = clips[clip]["bones"][b]["rotation"]
            items = ch.items() if isinstance(ch, dict) else [("0.0", ch)]
            for k, v in items:
                v = v["vector"] if isinstance(v, dict) else v
                w = _ramp(float(k) * TPS, t0, t1, r)
                for i in range(3):
                    v[i] = round(v[i] + w * d[i], 3)
    return clips


GROUPS = {"hits": lambda: hit_clips(), "attack": lambda: attack_clips(), "ability": lambda: ability_clips(),
          "stances": lambda: stance_clips(), "idle": lambda: dict(zip(("idle_legs", "idle_upper"), idle_clips()))}


# per troop, stances.py layer clip: {bone: rotation added on every key}. Mage stance_aim_up_upper: the up aim lifts both arms
# off the held light and the left sleeve bell sank 2.8 px into the cowl (tick 3.5); the left arm lowered 18 clears it.
# Hexer: rally and fallback lift the lantern beside the wide burial hood (the cuff 2.3-2.7 px in it): the arm comes 30 out
# and 15 forward; the up aim's death mask sank into the mantle (head up 8, arm out 8); the down aim's cuff sat in the mantle
# (forearm 10 more bent).
LAYER_NUDGE = {"mage": {"stance_aim_up_upper": {"left_arm": (18.0, 0.0, 0.0)}},
               "hexer": {"stance_rally_upper": {"right_arm": (15.0, 0.0, -30.0)},
                         "stance_fallback_upper": {"right_arm": (15.0, 0.0, -30.0)},
                         "stance_aim_up_upper": {"head": (-8.0, 0.0, 0.0), "right_arm": (0.0, 0.0, -8.0)},
                         "stance_aim_down_upper": {"right_forearm": (10.0, 0.0, 0.0)}},
               # tc_mist: the rally's raised rod hand sat in the mantle plate, the up aim's bracer in the hood
               # (25 forward, 5 out: further out put the forearm in the head, scorecard pen_limb 1.98 px)
               "tc_mist": {"stance_rally_upper": {"right_arm": (25.0, 0.0, -5.0)},
                           "stance_aim_up_upper": {"right_arm": (0.0, 0.0, -14.0)}},
               # wm_seer: the rally's raised wand cuff sat 3.3 px in the head; (fix round 1) with the shoulder tusks sunk into
               # the pad, the stun's left pad still put a tusk 1.97 px in the torso: the left arm 5 deg further out
               "wm_seer": {"stance_rally_upper": {"right_arm": (15.0, 0.0, -20.0)},
                           "stun_upper": {"left_arm": (0.0, 0.0, 5.0)}}}
# per troop, more stance layers whose feet the floor pass lifts (wm_seer: the fallback's right hoof 1.1 px under at tick 0)
LAYER_FLOOR_TROOP = {"wm_seer": (("stance_fallback_upper", "stance_fallback_legs"),)}


def review_layers(path):
    """GG_CAST_REVIEW=layers: the review pass over the shared reaction layers already merged into the exported file."""
    import reactions                       # read only: the layers are rebuilt fresh, so the pass never stacks on itself
    import stances
    tr = stances.Troop(TROOP, path)
    fresh = reactions.build_all(tr)
    if True:                               # the stance layers (stances.py: rally, hold, fallback, rest, aim; the aim is cut
        st = stances.build_all(tr)         # from attack_cast, so it follows a re-keyed cast), with the review's re-poses
        fresh.update(st)
    for clip, bones in LAYER_NUDGE.get(TROOP, {}).items():   # stance and reaction layers alike, before the life pass
        for b, d in bones.items():
            ch = fresh[clip]["bones"][b]["rotation"]
            for v in (ch.values() if isinstance(ch, dict) else [ch]):
                v = v["vector"] if isinstance(v, dict) else v
                for i in range(3):
                    v[i] = round(v[i] + d[i], 3)
    doc = json.loads(path.read_text(encoding="utf-8"))
    A = doc["animations"]
    for k, v in fresh.items():
        if k in A and json.dumps(A[k], sort_keys=True) != json.dumps(v, sort_keys=True):
            print("  layer %s: the file's copy differs from a fresh reactions.py build (an earlier review pass, or drift)" % k)
        A[k] = v
    stun_legs0 = json.loads(json.dumps(A["stun_legs"]))
    for up, lg, loop in LAYER_LIFE:
        if up not in A:
            continue
        if up == "stun_out_upper":         # it plays while the stun legs hand over to the stance (StanceController.STUN_OUT,
            #                                8 ticks): the spring is driven by that handover. Its upper pieces only: the
            #                                robe panels belong to the legs clips (scorecard plays the clips together and
            #                                a bone may be keyed by only one of them), so they move as the stun lap fades
            life_pass(A[up], _handover_legs(stun_legs0, A["stance_ready_legs"], float(A[up]["animation_length"])), None,
                      name=up, upper_bones=set(A["stun_upper"]["bones"]))
        else:
            life_pass(A[up], A[lg], loop, name=up,
                      upper_bones=set(A["stance_ready_upper"]["bones"]) if up.startswith("hit_") else ())
    for up, lg in LAYER_FLOOR + LAYER_FLOOR_TROOP.get(TROOP, ()):
        if up in A:
            floor_lift(doc, up, lg, on_guard=FLOOR_ON_GUARD and up.startswith("hit_"))
    hang_swing.key_doc(TROOP, doc, log=False)
    path.write_bytes(json.dumps(doc, indent=1).encode("utf-8"))
    print("review layers written to", path)


# ------------------------------------------------------------------------------------------------
# Carry: the focus held up in the right fist (per troop: CARRY pitch walking / running, u its face), the left arm free.
# ------------------------------------------------------------------------------------------------

CARRY_R = (-12.0, 0.0, 6.0)
CARRY_RF = -46.0


def carry_hand(pose, pitch, yaw=0.0):
    return hold_wrist(pose, aim(pitch, yaw), CARRY["u"])


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
    # the off forearm pumps with the swing: walking it folds on the forward half only; running it eases through a full
    # cosine (same fold at the front, open at the back) so the short run loop carries no corner
    lf = (c.fore - (0.175 * c.arm * (1.0 + sw) if run else 0.35 * c.arm * max(0.0, sw)), 0.0, 0.0)
    if run:
        ra = (-24.0 + 0.45 * c.arm * sw, 0.0, 9.0)
        rf = (-50.0 - 0.15 * c.arm * (1.0 + sw), 0.0, 0.0)   # a full cosine pump, as the off forearm
    else:
        cr, crf = CARRY.get("R", CARRY_R), CARRY.get("RF", CARRY_RF)   # per troop: the mage carries its staff higher
        ra = (cr[0] + 0.45 * c.arm * sw, 0.0, cr[2])
        rf = (crf - 0.3 * c.arm * max(0.0, -sw), 0.0, 0.0)
    s = {"torso": ((tx, ty, tz), None), "head": (head, None), "right_arm": (ra, None), "right_forearm": (rf, None),
         "left_arm": (la, None), "left_forearm": (lf, None), "left_hand": (Z3, None)}
    pose = fk_pose(s, legs)
    # the point rocks a beat behind the arm (rule 9: the weight lags)
    pitch = CARRY["run" if run else "walk"] + 3.0 * math.cos(tw * (u - c.arm_lag - 0.12))
    s["right_hand"] = (carry_hand(pose, pitch, -8.0 - 0.3 * (hy + ty)), None)
    flare, flap = c.cape
    cape = (flare + flap * math.sin(tw * 2 * (u - g.bob_phase - 0.12)), 0.0, -0.6 * tz)
    for gname, k in ST["gear"].items():
        s[gname] = ((k * cape[0], 0.0, k * cape[2]), None)
    return s


# Per troop: where in the stride the gait clips start (a fraction of the cycle, the same for the walk and the run so the
# GaitController's walk-run switch, which keeps the phase, still lands foot for foot). The 4-tick run sampled a tick at a time
# turns at its wrap exactly on the stride's extremes (the shoulders' and arms' peak swing, the planted foot's toe-off), which the
# scorecard's loop_vel reads as a kink twice the peak speed (review 2026-11-10: torso 16 vs 10.9, right foot 59.8 vs 41.0);
# starting a little over a quarter stride later puts the wrap between them. The motion itself is unchanged.
GAIT_PHASE = {"mage": 0.28, "hexer": 0.28}


def gait_u(g, u):
    ph = GAIT_PHASE.get(TROOP, 0.0)
    return u if not ph else (u + ph) % 1.0


def _legs_samples(g, n):
    return [{"hips": g.hips(gait_u(g, i / n))} for i in range(n)]


def gait_upper(g, c, run):
    n = g.ticks * SUB
    lsamp = _legs_samples(g, n)
    carry = type("C", (), {k: getattr(c, k) for k in dir(c) if not k.startswith("_")})
    carry.arm_base = -4.0 if not run else -16.0
    carry.arm_out = 6.0 if not run else 10.0
    carry.fore = -18.0 if not run else -50.0
    samples = [gait_pose(g, carry, gait_u(g, i / n), run, lsamp[i]) for i in range(n)]
    low = min(wpoint(fk_pose(s, l), S(BUTT))[1] for s, l in zip(samples, lsamp))
    hits = sum(bool(shaft_hits(fk_pose(s, l))) for s, l in zip(samples, lsamp))
    print("%s carry: butt lowest %.1f px above the ground, samples with the focus in the body %d" % (g.name, low, hits))
    samples.append(samples[0])
    return K.bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Stances and idle: all in guard (STYLE.md: the guard is the ready pose; idle stays in it). StanceController plays ready
# (fighting), brace / wall (Hold), ward (fall_back).
# ------------------------------------------------------------------------------------------------

STANCE_KEYS.setdefault("shield_up", STANCE_KEYS["ready"])
LIFE = {"ready": 3.4, "brace": 1.6, "wall": 1.4, "ward": 2.0, "shield_up": 3.4}
IDLE_LIFE = 2.2
READY_LIKE = ("ready", "shield_up")   # shield_up is a guard: both ride V2_READY_SWAY_SCALE


def stance_upper(k, life, legs_spec, ticks=K.STANCE_TICKS, ready=False):
    n = ticks * SUB
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)
    sol = Solver()
    out = []
    hips = (legs_spec or STANCE_LEGS["ready"])["hips"]
    legs = {"hips": hips}
    # two laps: the first only warms the arm solves, so the loop's first sample sits on the same IK branch as its last
    # (a cold start can land the elbow on another branch and kink the wrap)
    for i in list(range(n)) + list(range(n + 1)):
        if i == 0 and len(out) == n:
            out = []
        u = (i % n) / n
        br = a_br * math.sin(2 * math.pi * u)
        wt = a_wt * math.sin(2 * math.pi * u - K.WEIGHT)
        wl = a_wt * math.sin(2 * math.pi * u - K.WEIGHT - 0.7)
        tx, ty, tz = k["torso"]
        torso = (tx + 0.8 * br + 1.8 * wt, ty + 1.4 * wt, tz + 1.2 * wt)
        gaze = tuple(np.array(k["head"]) + np.array([-0.3 * br, 0.0, 0.0]))
        head = head_local(gaze, torso, hips[0])
        # the focus rides the breath a beat behind it, its point drifting
        R = np.array(k["R"]) + np.array([0.0, 0.12 * br + 0.15 * wl, 0.1 * wl])
        dd = np.array(k["d"]) + np.array([0.0, 0.006 * br + 0.01 * wl, 0.0])
        lp = np.array(k["lp"]) + np.array([-0.8 * wl, 0.0, 0.0, -1.2 * wl])
        d, uu = frame(dd, k["u"])
        s = sol.pose(legs, torso, head, R, d, uu, k["rp"], lp, lh=k["lh"][0], ls=k["ls"][0], lsw=k["lsw"][0])
        out.append(s)
    out = gear(out)
    out[-1] = out[0]
    return K.bake(out, ticks)


def stance_clips():
    out = {"stance_%s_upper" % k: stance_upper(v, LIFE[k], STANCE_LEGS.get(k), ready=k in READY_LIKE) for k, v in STANCE_KEYS.items()}
    out.update({"stance_%s_legs" % k: stance_legs(v, K.STANCE_LIFE.get(k, 1.0), ready=k in READY_LIKE) for k, v in STANCE_LEGS.items()})
    return out


def idle_clips():
    return stance_legs(STANCE_LEGS["ready"], IDLE_LIFE, K.IDLE_TICKS), stance_upper(GUARD, IDLE_LIFE, None, K.IDLE_TICKS)


K.idle_clips = idle_clips    # the death falls start from the guard

# ------------------------------------------------------------------------------------------------
# Hits: additive offsets. The right fist keeps the focus; the free left arm flies wide and the torso takes the blow.
# ------------------------------------------------------------------------------------------------

off = K.off
hb, hb_front = K.hb, K.hb_front   # Kort's polish: how far a blow throws the spine and head BACK (art/anim/common/feel.py)
# Batch review 2026-11-11: the front blow's left arm swings 22, not 32, toward the body (z- here is inward on the free left arm):
# at 32 the sleeve sank 1.9 px into the vestment at tick 5.5 as the game plays it (over the guard).
HIT_PEAK = {
    "front": off(torso=(hb_front(-17), -10, 0), head=(hb(-12), 3, 0), rarm=(-10, 0, 14), larm=(-19, 0, -22), lfore=-12),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 10), larm=(14, 0, -24)),
    "right": off(torso=(hb(-5), 14, 8), head=(0, 6, 3), rarm=(6, 0, 10), larm=(-12, 0, -22)),
    "left": off(torso=(hb(-5), -14, -8), head=(0, -6, -3), rarm=(-10, 0, 14), larm=(12, 0, -30)),
}


# Batch review 2026-11-11: the tc_mist and wm_seer guards already hold the free arm 40-45 deg out to the side; the blow's
# outward fling on top of that took the upper arm past 115 deg and the pauldron into the head (game: the hit adds onto the
# guard). On these two the fling outward is cut to this share; the forward and back swing is kept.
WIDE_GUARD_FLING = {"tc_mist": 0.4, "wm_seer": 0.4}.get(TROOP)


# Batch review 2026-11-11, mage: the left blow threw the upright sun staff's head into the face (1.6 px at tick 2 over the
# guard); the right arm swings 6 deg less forward with it.
if TROOP == "mage":
    HIT_PEAK["left"] = dict(HIT_PEAK["left"], right_arm=(-4, 0, 14))


def _fling(peak):
    if WIDE_GUARD_FLING is None:
        return peak
    x, y, z = peak["left_arm"]
    return dict(peak, left_arm=(x, y, z * WIDE_GUARD_FLING))


def hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for dname, peak in HIT_PEAK.items():
            peak = _fling(peak)
            keys = K.front_onset(K.hit_keys(peak, weight), dname, weight)
            out["hit_%s_%s" % (weight, dname)] = K.bake([K.sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)], ticks,
                                                       loop=False)
    out["hit_block"] = out["hit_light_front"]
    out.update(K.hit_legs_clips())
    return out


# ------------------------------------------------------------------------------------------------
# Deaths: the knight's four falls, the focus let go flat (the spearman's long-shaft solve: the wrist turns the shaft to lie on
# the ground clear of the body, tipped about the fist by the least angle that keeps it out of the floor, planned backwards).
# ------------------------------------------------------------------------------------------------

_CORNERS = K.body_corners


def is_weapon(bone):
    return any(f in bone for f in FOCI)


def body_corners_no_weapon(pose, model=K.HEAVY):
    P, owner = _CORNERS(pose, model)
    keep = [i for i, o in enumerate(owner) if not is_weapon(o)]
    return P[keep], [owner[i] for i in keep]


K.body_corners = body_corners_no_weapon

# ------------------------------------------------------------------------------------------------
# Caster v2 hanging robe (wm_seer, tc_mist, mage, hexer; troop_rig.ROBE): four skirt panels hung from the hips, the legs' own boxes empty.
# Every clip that moves the legs also keys the panels: each panel hangs plumb (or follows its thigh a little) and is
# swung out by the least angle that keeps the leg line behind its inner face and the hoof tops behind its outer face,
# with the hem off the floor. In a death the robe folds with the body (it follows the legs, no gravity or floor).
# ------------------------------------------------------------------------------------------------

# panel: (axis across its face, inward sign, inner face, outer face, axis along its width, width, legs it covers)
PANELS = {"skirt_front": (2, 1.0, -2.0, -3.0, 0, (-4.5, 4.5), ("right", "left")),
          "skirt_back": (2, -1.0, 2.0, 3.0, 0, (-4.5, 4.5), ("right", "left")),
          "skirt_right": (0, 1.0, -3.5, -4.5, 2, (-2.5, 2.5), ("right",)),
          "skirt_left": (0, -1.0, 3.5, 4.5, 2, (-2.5, 2.5), ("left",))}
HEM_Y = 2.0          # the hem's local height (bedrock px); the pivot rides the hips at 12
FEET = "_hoof"       # the bone whose boxes are the feet showing under the hem
if TROOP in ("tc_mist", "mage"):   # the bell robe steps out a px a tier (troop_rig.ROBE tiers): inner faces at the middle tier, outer at
    # the hem's; its hem hangs to 1.5 px; no hooves, the feet are the shins' bottom rows on the foot bones
    PANELS = {"skirt_front": (2, 1.0, -2.0, -3.5, 0, (-5.0, 5.0), ("right", "left")),
              "skirt_back": (2, -1.0, 2.0, 3.5, 0, (-5.0, 5.0), ("right", "left")),
              "skirt_right": (0, 1.0, -4.0, -5.5, 2, (-3.0, 3.0), ("right",)),
              "skirt_left": (0, -1.0, 4.0, 5.5, 2, (-3.0, 3.0), ("left",))}
    HEM_Y, FEET = 1.5, "_foot"
if TROOP == "hexer":   # the gravecaller's burial robe: the same bell a px wider and deeper (tiers 10/11/12 wide, 6/7/8 deep)
    PANELS = {"skirt_front": (2, 1.0, -2.5, -4.0, 0, (-5.5, 5.5), ("right", "left")),
              "skirt_back": (2, -1.0, 2.5, 4.0, 0, (-5.5, 5.5), ("right", "left")),
              "skirt_right": (0, 1.0, -4.5, -6.0, 2, (-3.5, 3.5), ("right",)),
              "skirt_left": (0, -1.0, 4.5, 6.0, 2, (-3.5, 3.5), ("left",))}
    HEM_Y, FEET = 1.5, "_foot"
LEG_R = 0.75         # the leg line stays this far behind a panel's inner face
SIDE_PITCH = 0.8     # how much of its leg's stride a side panel's hem follows
SIDE_FOLLOW = 0.5    # how much of its thigh's splay a side panel follows (front and back: 0.5)
SIDE_HEM_CLEAR = None   # px a side panel's hem keeps above the floor (None: HEM_CLEAR)
HEM_CLEAR = 0.15     # px the hem keeps above the floor while alive
if TROOP == "hexer":   # the burial robe's side panels hang straight: seen edge-on from the front a splayed side panel reads as a
    # spindly leg under the hem, so they follow the stride and the thigh less and may graze the floor at a corner (0.2 px) rather
    # than kick out to clear it
    SIDE_PITCH, SIDE_FOLLOW, SIDE_HEM_CLEAR = 0.2, 0.25, -0.2
ROBE = {"death": False}


def _hl(Mi, w):
    """World px -> hips-local homogeneous model coords."""
    return Mi @ np.array([w[0] / 16.0, w[1] / 16.0, w[2] / 16.0, 1.0])


def _bed(q):
    return np.stack([-q[..., 0] * 16.0, q[..., 1] * 16.0, q[..., 2] * 16.0], axis=-1)


def _model(b):
    return np.array([-b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, 1.0])


def _panel_rot(P, out, pitch=0.0):
    a = PANELS[P][0]
    return (_SIGN[P] * out, 0.0, 0.0) if a == 2 else (pitch * _FSIGN, 0.0, _SIGN[P] * out)


def _sign_of(P, rot):
    a, sg = PANELS[P][0], PANELS[P][1]
    piv = np.array(_PIV[P], float)
    hem = _model((piv[0], HEM_Y, piv[2]))
    return -sg * (_bed(K.local(P, rot) @ hem)[a] - _bed(hem)[a])


_SIGN, _FSIGN = {}, 1.0
if ROBED:
    _SIGN = {P: (1.0 if _sign_of(P, (5.0, 0, 0) if PANELS[P][0] == 2 else (0, 0, 5.0)) > 0 else -1.0) for P in PANELS}
    _h = _model((_PIV["skirt_right"][0], HEM_Y, 0.0))
    _FSIGN = 1.0 if _bed(K.local("skirt_right", (5.0, 0, 0)) @ _h)[2] < _bed(_h)[2] else -1.0   # + swings the hem forward
    # the hoof boxes as points every 0.25 px along their 12 edges (corners alone miss an edge across a panel's corner)
    _HOOF = {s: [tuple(o[d] + sz[d] * (e[d] if d != ax else f) for d in range(3)) for b in _GEO if b["name"] == s + FEET
                 for c in b["cubes"] for o, sz in [(c["origin"], c["size"])] for ax in range(3)
                 for e in [(i, j, k) for i in (0, 1) for j in (0, 1) for k in (0, 1)] if e[ax] == 0
                 for f in np.linspace(0.0, 1.0, max(2, int(round(sz[ax] * 4)) + 1))]
             for s in ("right", "left")}


def robe_frame(pose):
    """The legs in hips-local bedrock px: the leg line (hip-knee-ankle, every px), the hoof corners, the thighs; and the
    hips' world matrix."""
    Mh = K.world(pose, "hips")
    Mi = np.linalg.inv(Mh)
    line, hoof, thigh = [], [], {}
    for s in ("right", "left"):
        hip = K.point(K.world(pose, s + "_leg"), _PIV[s + "_leg"])
        knee = K.point(K.world(pose, s + "_shin"), _PIV[s + "_shin"])
        Mf = K.world(pose, s + "_foot")
        ankle = K.point(Mf, _PIV[s + "_foot"])
        for p0, p1, last in ((hip, knee, False), (knee, ankle, True)):
            n = max(2, int(np.linalg.norm(p1 - p0)) + 1)
            for i in range(n + (0 if last else 0)):
                f = i / n
                if last and np.linalg.norm(p1 - p0) * (1 - f) < 1.0:
                    continue
                line.append(_hl(Mi, p0 + (p1 - p0) * f))
        Mo = K.world(pose, s + FEET)
        hoof += [_hl(Mi, K.point(Mo, c)) for c in _HOOF[s]]
        thigh[s] = _bed(_hl(Mi, knee)) - _bed(_hl(Mi, hip))
        thigh[s + "_ankle"] = _bed(_hl(Mi, ankle)) - _bed(_hl(Mi, hip))
    return Mh, np.array(line), np.array(hoof), thigh


def _slack(P, rot, F, live, leg_r=None):
    """How far inside its limits the panel is at rot (negative: a leg or hoof out through it, or the hem in the floor)."""
    Mh, line, hoof, _ = F
    a, sg, inner, outer, la, (l0, l1), _legs = PANELS[P]
    L = K.local(P, rot)
    Li = np.linalg.inv(L)
    q = _bed((Li @ line.T).T)
    m = (q[:, 1] >= HEM_Y + 0.5) & (q[:, la] >= l0) & (q[:, la] <= l1)
    s = min([np.inf] + list(sg * (q[m, a] - inner) - (LEG_R if leg_r is None else leg_r)))
    q = _bed((Li @ hoof.T).T)
    m = (q[:, la] >= l0 - 0.5) & (q[:, la] <= l1 + 0.5)
    # a hoof point pokes by the least of how far it is outside the outer face and how far above the hem (one just under the hem
    # that sticks out is the foot showing below the robe, not a poke)
    s = min([s] + list(np.maximum(sg * (q[m, a] - outer), HEM_Y - q[m, 1]) - 0.05))
    if live:
        hem = []
        for lat in (l0, l1):
            for d in (inner, outer):
                b = [0.0, HEM_Y, 0.0]
                b[la], b[a] = lat, d
                hem.append((Mh @ L @ _model(b))[1] * 16.0)
        s = min(s, min(hem) - (SIDE_HEM_CLEAR if a == 0 and SIDE_HEM_CLEAR is not None else HEM_CLEAR))
    return s


def _angle(o, down):
    return math.degrees(math.atan2(o, down))


def panel_pose(pose, live=True, pitches=None, solved=None):
    """{panel: (rot, None)} for a pose that has the hips and legs. pitches: {side panel: its in-plane swing} to use instead of
    the stride's own (the gait's smoothed swing); solved: a dict that receives {panel: (out angle, swing)}."""
    F = robe_frame(pose)
    Mh, _, _, thigh = F
    g = _bed(np.linalg.inv(Mh[:3, :3]) @ np.array([0.0, -1.0, 0.0]) / 16.0)
    out = {}
    for P, (a, sg, inner, outer, la, wid, legs) in PANELS.items():
        follow = max(_angle(-sg * thigh[s][a], -thigh[s][1]) for s in legs)
        grav = _angle(-sg * g[a], -g[1])
        pref = max(grav, (SIDE_FOLLOW if a == 0 else 0.5) * follow) if live else 0.9 * follow
        pitch = 0.0
        if a == 0:   # a side panel also swings in its own plane with its leg's stride: its hem follows the hoof (hip to ankle), not
            k = thigh[legs[0] + "_ankle"]   # the knee, so a bent knee forward never leaves the heel out behind the panel
            pitch = SIDE_PITCH * _angle(-k[2], -k[1])
            if pitches is not None:
                pitch = pitches[P]
        lo = hi = pref
        if _slack(P, _panel_rot(P, pref, pitch), F, live) < 0:
            hi = pref
            while hi < 90.0:
                lo, hi = hi, min(90.0, hi + 2.0)
                if _slack(P, _panel_rot(P, hi, pitch), F, live) >= 0:
                    break
            for _ in range(4):
                mid = 0.5 * (lo + hi)
                if _slack(P, _panel_rot(P, mid, pitch), F, live) >= 0:
                    hi = mid
                else:
                    lo = mid
        if not live and TROOP in ("mage", "hexer"):   # a dead mage's robe falls shut: each panel folds in onto the legs and feet as far
            lo = hi                        # as they let it, so the hem does not gape open round empty space
            lr = 0.1 if a == 2 else None   # the legs have no boxes: front and back fold right in to the leg line and close
            while lo > -20.0 and _slack(P, _panel_rot(P, lo - 2.0, pitch), F, live, lr) >= 0:
                lo -= 2.0
            if lo > -20.0:
                bad = lo - 2.0
                for _ in range(4):
                    mid = 0.5 * (lo + bad)
                    if _slack(P, _panel_rot(P, mid, pitch), F, live, lr) >= 0:
                        lo = mid
                    else:
                        bad = mid
            hi = lo
        out[P] = (tuple(round(v, 4) for v in _panel_rot(P, hi, pitch)), None)
        if solved is not None:
            solved[P] = (hi, pitch)
    return out


def robe_metrics(pose, live=True):
    """(leg line margin behind the inner faces, hoof margin inside the outer faces, hem height above the floor) in px for
    a pose whose panels are keyed: the least over the panels. Leg >= 0, hoof >= 0 and hem > 0 (alive) pass."""
    F = robe_frame(pose)
    Mh, line, hoof, _ = F
    leg = hf = hem = np.inf
    for P, (a, sg, inner, outer, la, (l0, l1), legs) in PANELS.items():
        L = K.local(P, pose.get(P, (Z3, None))[0])
        Li = np.linalg.inv(L)
        q = _bed((Li @ line.T).T)
        m = (q[:, 1] >= HEM_Y + 0.5) & (q[:, la] >= l0) & (q[:, la] <= l1)
        leg = min([leg] + list(sg * (q[m, a] - inner)))
        q = _bed((Li @ hoof.T).T)
        m = (q[:, 1] > HEM_Y + 0.05) & (q[:, la] >= l0 - 0.5) & (q[:, la] <= l1 + 0.5)
        hf = min([hf] + list(sg * (q[m, a] - outer)))
        for lat in (l0, l1):
            for d in (inner, outer):
                b = [0.0, HEM_Y, 0.0]
                b[la], b[a] = lat, d
                hem = min(hem, (Mh @ L @ _model(b))[1] * 16.0)
    return leg, hf, hem


def add_panels(samples, live=None):
    live = (not ROBE["death"]) if live is None else live
    for s in samples:
        if "skirt_front" in s or "right_leg" not in s:
            continue
        pose = {b: (v[0], v[1] or Z3) for b, v in s.items()}
        s.update(panel_pose(pose, live))
    return samples


if ROBED:
    _bake_plain = K.bake
    K.bake = lambda samples, *a, **kw: _bake_plain(add_panels(samples), *a, **kw)

    def _legs_offset(samples):
        """K.legs_offset with the panels: both the absolute samples and the ready footing get theirs first."""
        ready = K.attack_legs({"hips": [(0, *READY_HIPS), (1, *READY_HIPS)], "steps": []}, ticks=1)[0]
        add_panels([ready], True)
        add_panels(samples, True)
        out = []
        for s in samples:
            f = {}
            for b, (rot, pos) in s.items():
                r0, p0 = ready[b]
                f[b] = (tuple(K.wrap(x - c) for x, c in zip(rot, r0)),
                        tuple(x - c for x, c in zip(pos, p0)) if pos is not None else None)
            out.append(f)
        return out

    K.legs_offset = _legs_offset
    _corners_plain = K.body_corners

    def _corners_robe(pose, model=K.HEAVY):
        """In a death the panels fold with the legs before the corners are taken, so the fall grounds the folded robe."""
        if ROBE["death"]:
            add_panels([pose], False)
        return _corners_plain(pose, model)

    K.body_corners = _corners_robe


# The robe in the gait: cloth does not follow a leg's kick on the sample; it is carried out by the stride and lets go of it a
# little later. Each panel's swing is eased round the loop (a circular box of GAIT_CLOTH samples each side) and its opening is
# the eased envelope of the least opening that clears the legs (a running maximum, then the same box), so it never closes
# through a leg and the hem rounds over the top of each kick instead of snapping back at it.
GAIT_CLOTH = 3


def _circ_box(x, r):
    x = np.asarray(x, float)
    return np.mean([np.roll(x, k) for k in range(-r, r + 1)], axis=0)


def _circ_max(x, r):
    x = np.asarray(x, float)
    return np.max([np.roll(x, k) for k in range(-r, r + 1)], axis=0)


def gait_legs_robed(g):
    n = g.ticks * SUB
    guesses = {}
    for i in range(n):            # warm the IK guesses on one silent lap
        g.pose(gait_u(g, i / n), guesses)
    samples = [g.pose(gait_u(g, i / n), guesses) for i in range(n)]
    poses = [{b: (v[0], v[1] or Z3) for b, v in s.items()} for s in samples]
    raw = []
    for p in poses:
        d = {}
        panel_pose(p, True, solved=d)
        raw.append(d)
    side = {P: _circ_box([r[P][1] for r in raw], GAIT_CLOTH) for P in PANELS if PANELS[P][0] == 0}
    solved = []
    for i, p in enumerate(poses):
        d = {}
        panel_pose(p, True, pitches={P: float(side[P][i]) for P in side}, solved=d)
        solved.append(d)
    for P in PANELS:
        opening = _circ_box(_circ_max([d[P][0] for d in solved], GAIT_CLOTH), GAIT_CLOTH)
        for i, s in enumerate(samples):
            s[P] = (tuple(round(v, 4) for v in _panel_rot(P, float(opening[i]), solved[i][P][1])), None)
    samples.append(samples[0])
    return K.bake(samples, g.ticks)


if ROBED:
    K.gait_legs = gait_legs_robed

FLAT_AT ={"death_back": (8, 12), "death_forward": (13, 16), "death_left": (9, 13), "death_right": (9, 13)}
_ARMED = ("right_arm", "right_forearm", "right_hand", "right_wrap", "right_bracer") + FOCI


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


def _slerp_q(q0, q1, k):
    if q0 @ q1 < 0:
        q1 = -q1
    ang = math.acos(min(1.0, float(q0 @ q1)))
    q = q0 if ang < 1e-6 else (math.sin((1 - k) * ang) * q0 + math.sin(k * ang) * q1) / math.sin(ang)
    return q / np.linalg.norm(q)


def slerp_rot(r0, r1, k):
    return K.decompose_zyx(_mat(_slerp_q(_quat(_rot(r0)), _quat(_rot(r1)), k)))


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
    """The right wrist rotation letting the focus drop: it lies along the ground clear of the body, as near `ref` as it can."""
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
MAX_WRIST_TURN = 26.0   # deg per tick: the focus turns in the falling fist no faster than the fall itself (spear lane: 36)


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


def flat_weapon(keys, start, t0, t1, step=None, name=None):
    """Re-key the fall every DEATH_STEP with the right wrist solved: free, then easing from t0 to the focus lying flat by t1,
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
    cap = WRIST_TURN.get((TROOP, name), MAX_WRIST_TURN) * step
    rs = [o[3] for o in out]
    for i in range(len(rs) - 2, -1, -1):
        if out[i][0] < 3.0:
            break
        turn = turn_deg(rs[i], rs[i + 1])
        if turn > cap:
            rs[i] = ground_hand(out[i][2], slerp_rot(rs[i + 1], rs[i], cap / turn), rs[i + 1])
    res, prev = [], start["right_hand"][0]
    wcap = FOCUS_WORLD_TURN.get((TROOP, name))
    if wcap is not None:
        q0 = {b: (v[0], Z3) for b, v in start.items() if b in K.DEATH_BONES}
        q0["hips"] = (start["hips"][0], start["hips"][1])
        wprev = K.world(q0, "right_hand")[:3, :3]
    for (t, f, pose, _), r in zip(out, rs):
        turn = turn_deg(prev, r)
        if turn > cap:
            r = ground_hand(pose, slerp_rot(prev, r, cap / turn), prev)
        if wcap is not None:
            r, wprev = world_capped(pose, r, prev, wprev, wcap * step)
        prev = near_euler(r, prev)
        f["right_hand"] = prev
        res.append((t, "lin", f))
    return res


# Per troop and fall (review F2 2026-11-10: these falls whipped the focus past the weapon speed limit, 83 deg/100 ms, and the
# old clips were kept instead, snapping from the new guard on tick 0). FOCUS_WORLD_TURN: the most the focus may turn in the
# world per tick (deg): where the fall's own arm swing (the roll throws the arm out past the head) would whip a focus held
# rigid in the fist, the wrist gives against the swing and the focus follows a beat behind. WRIST_TURN: MAX_WRIST_TURN for
# that fall, where the wrist's own turn to lay the focus down is the whip. The gravecaller's lantern also swings on its chain
# (hang_swing, up to ~30 deg/100 ms on top), so its pole is held slower.
FOCUS_WORLD_TURN = {("hexer", "death_right"): 18.0, ("hexer", "death_left"): 24.0, ("wm_seer", "death_right"): 30.0}
WRIST_TURN = {("hexer", "death_left"): 13.0}


def world_capped(pose, r, prev, wprev, cap):
    """The wrist rotation nearest `r` whose world turn from `wprev` is at most `cap` degrees (kept clear of the ground), and the
    world rotation it gives."""
    q = dict(pose)
    q["right_hand"] = (Z3, Z3)
    Rp = K.world(q, "right_hand")[:3, :3]
    q["right_hand"] = (r, Z3)
    Rw = K.world(q, "right_hand")[:3, :3]
    turn = math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(wprev.T @ Rw) - 1.0) / 2.0))))
    if turn > cap:
        Rt = _mat(_slerp_q(_quat(wprev), _quat(Rw), cap / turn))
        r = ground_hand(pose, K.decompose_zyx(Rp.T @ Rt), prev)
        q["right_hand"] = (r, Z3)
        Rw = K.world(q, "right_hand")[:3, :3]
    return r, Rw


# Per-troop tweaks on the knight's falls ({fall: {key tick: {bone: rot}}}), so the robed body lies on the ground and not
# propped on its bell sleeves and leg robes.
# Per-troop final-pose changes to the knight falls (tick: bone: rotation): the side falls lie on the cowl and robe
# with the arm tucked under rather than propped on the bell sleeve.
_ROBE_SIDE_FALLS = {
    "death_left": {17: {"head": (-15, 0, 30), "right_arm": (-30, 0, 20)}, 20: {"head": (-20, 0, 30), "right_arm": (-30, 0, 20)}},
    "death_right": {17: {"root": (10, 90, -88), "left_arm": (0, 0, -20)}, 20: {"root": (10, 100, -88), "left_arm": (0, 0, -20)}},
}
# the seer's robe is a stiff hanging skirt, so a fall must not land on a braced arm: on its back the arms settle out wide and
# flat, face down the wolf head turns aside onto its cheek (the snout would prop it), and in a side roll the arm the body
# rolls over lies straight out past the head on the ground instead of bent under it (grid 2026-11-06: torso gap 0.3 back,
# 1.4 face down, ~0.1 over the robe's own floor on the sides)
_SEER_FALLS = {
    # the off hand drops open on the ground (the knight's -180 flip lays its shield down; a caster has none to turn)
    "death_back": {16: {"right_arm": (-40, 0, 80), "left_arm": (-30, 0, -40), "left_forearm": (0, 0, 0),
                        "left_hand": (-20, 0, -10)},
                   20: {"right_arm": (-40, 0, 80)}},
    # struck from behind the wand arm drops to the side rather than flinging forward from the crouched guard
    "death_forward": {3: {"right_arm": (-15, 0, 20)}, 18: {"head": (-25, 60, 0)}, 20: {"head": (-20, 70, 0)}},
    "death_right_roll": {18: {"right_arm": (-180, 0, -5), "right_forearm": (-20, 0, 0)},
                         19: {"right_arm": (-180, 0, -10), "right_forearm": (0, 0, 0)},
                         20: {"right_arm": (-180, 0, -10), "right_forearm": (0, 0, 0)}},
    "death_left_roll": {18: {"left_arm": (-180, 0, -10), "left_forearm": (-30, 0, 0)},
                        19: {"left_arm": (-180, 0, 10), "left_forearm": (0, 0, 0)},
                        20: {"left_arm": (-180, 0, 10), "left_forearm": (0, 0, 0)}},
}
# the mage lies out straight-legged (the knight's tucked knees push the hanging robe out edge-on): the robe folds flat
_MAGE_FALLS = {n: {t: dict(k) for t, k in v.items()} for n, v in _SEER_FALLS.items() if n != "death_forward"}
for _n, _ticks in {"death_back": {11: {"right_leg": (-8, 0, 3), "right_shin": (12, 0, 0), "left_leg": (-14, 0, -4),
                                       "left_shin": (24, 0, 0)}, 12.5: {"left_shin": (22, 0, 0)}, 18: {"left_shin": (18, 0, 0)},
                                  20: {"left_shin": (20, 0, 0)}},
                   "death_left": {11: {"left_leg": (-12, 0, -3), "left_shin": (20, 0, 0), "right_leg": (-18, 0, 0),
                                       "right_shin": (24, 0, 0)}},
                   "death_right": {11: {"right_leg": (-12, 0, 3), "right_shin": (20, 0, 0), "left_leg": (-18, 0, 0),
                                        "left_shin": (24, 0, 0)}}}.items():
    for _t, _k in _ticks.items():
        _MAGE_FALLS.setdefault(_n, {}).setdefault(_t, {}).update(_k)
DEATH_TWEAK = {"mage": _MAGE_FALLS,
               # caster v2: the gravecaller lies out straight-legged as the sunweaver does, so its hanging robe folds flat
               "hexer": _MAGE_FALLS,
               "wm_seer": _SEER_FALLS,
               # the mist caller's robe hangs like the seer's: the same flat-armed falls (no snout, so no head turn face down)
               "tc_mist": {n: k for n, k in _SEER_FALLS.items() if n != "death_forward"}}.get(TROOP, {})


def _tweak(keys, name):
    out = []
    for t, ease, k in keys:
        k = dict(k)
        k.update(DEATH_TWEAK.get(name, {}).get(t, {}))
        out.append((t, ease, k))
    return out


def death_falls(start):
    roll = {"death_left": dict(K.ROLL_LEFT), "death_right": dict(K.ROLL_RIGHT)}
    for n, r in roll.items():
        for t, k in DEATH_TWEAK.get(n + "_roll", {}).items():
            r[t] = {**r.get(t, {}), **k}
    return {"death_back": _tweak(K.DEATH_BACK, "death_back"), "death_forward": _tweak(K.DEATH_FORWARD, "death_forward"),
            "death_left": K.roll_keys(_tweak(K.DEATH_LEFT, "death_left"), start, 13, roll["death_left"]),
            "death_right": K.roll_keys(_tweak(K.DEATH_RIGHT, "death_right"), start, 13, roll["death_right"])}


LIFT_IN = 4.0   # ticks over which a fallen body is lifted onto its robe hem


def ground_robes(samples):
    """The standing robe hem hangs below the soles, so the knight fall grounds the body on that hem and a fallen body lies
    sunk by as much. Lift each sample (eased in over the first LIFT_IN ticks of the fall) so the lowest hard corner, robe
    panels included, rests on the ground the soles stood on."""
    P, owner = K.body_corners(samples[0])
    hard = np.array([not o.startswith(K.SOFT) for o in owner])
    sole = np.array([h and "robe" not in o for h, o in zip(hard, owner)])
    ground = P[sole, 1].min()
    for i, pose in enumerate(samples):
        P, _ = K.body_corners(pose)
        w = ramp(i / SUB, 0.0, LIFT_IN)
        rot, (x, y, z) = pose["root"]
        pose["root"] = (rot, (x, y + w * max(0.0, ground - P[hard, 1].min()), z))
    return samples


def death_clips():
    start = K.idle_start()
    falls = death_falls(start)
    bake = K.bake
    K.bake = lambda samples, *a, **kw: bake(ground_robes(samples), *a, **kw)
    try:
        return {n: K.death_clip(flat_weapon(k, start, *FLAT_AT[n], name=n), start, n) for n, k in falls.items()}
    finally:
        K.bake = bake


if ROBED:
    _death_clips_plain = death_clips

    def death_clips():
        ROBE["death"] = True
        try:
            return _death_clips_plain()
        finally:
            ROBE["death"] = False


# ------------------------------------------------------------------------------------------------

def only_rig(anims):
    for clip in anims.values():
        clip["bones"] = {b: ch for b, ch in clip["bones"].items() if b in GEO_BONES}
    return anims


def dump(path, ticks):
    """Key-pose check: the throw's whole body at the given ticks (or the guard, tick -1), as still.py pose rows."""
    upper, legs = attack_clip()
    if ROBED:
        add_panels(legs)
    rows = []
    for t in ticks:
        i = int(round(t * SUB))
        p = fk_pose(upper[i], legs[i])
        rows.append({b: {"rotation": list(map(float, v[0])), "position": list(map(float, v[1]))} for b, v in p.items()})
        hits = shaft_hits(p)
        print("t%-5s focus-in-body %-3d butt y %.1f tip y %.1f" % (t, len(hits), wpoint(p, S(BUTT))[1], wpoint(p, S(TIP))[1]))
    Path(path).write_text(json.dumps(rows))
    print("dumped", len(rows), "poses to", path)


SW = K.V2_SWAY_SCALE   # Kort's polish: the pelvis sway and roll of the gait (art/anim/common/feel.py)
# caster v2 gaits (the GaitController reference speeds .16 / .26 kept): a quicker, shorter stride, the feet skimming low
# and the hips level, so the robe glides; the feet set wide enough apart that the hooves never cross
GAITS = {t: (
    K.Gait("walk", ticks=6, speed=0.16, duty=0.5, heel=4, toe_off=4, front=3.85, lift=0.9, lift_peak=0.5,
           hip_drop=0.95, bob=0.2, bob_phase=0.06, sway=0.2 * SW, yaw=3.0, roll=0.8 * SW, lane=2.5),
    K.Gait("run", ticks=5, speed=0.26, duty=0.4, heel=4, toe_off=10, front=3.4, lift=1.5, lift_peak=0.5,
           hip_drop=1.7, bob=0.3, bob_phase=0.19, sway=0.2 * SW, yaw=4.0, roll=1.0 * SW, pitch_hips=2.0, lane=2.5))
         for t in ("wm_seer", "tc_mist", "mage", "hexer")}
# the mage's run is a quicker, shorter scurry (4-tick lap, same speed): the shorter stride lets the hips stay high (less
# drop and bob), so the hem clears the floor without the side panels splaying out (run roll 26 -> 12 deg, the walk's 11)
GAITS["mage"] = (GAITS["mage"][0],
                 K.Gait("run", ticks=4, speed=0.26, duty=0.4, heel=4, toe_off=10, front=3.3, lift=1.2, lift_peak=0.5,
                        hip_drop=1.0, bob=0.15, bob_phase=0.19, sway=0.2 * SW, yaw=4.0, roll=1.0 * SW, pitch_hips=2.0, lane=2.5))
GAITS["hexer"] = (GAITS["hexer"][0], GAITS["mage"][1])   # the gravecaller's robe is the same bell: the same scurry


def tip_json():
    """Where the bolt leaves: the wand tip on the release sample, in the troop's body frame (blocks; x to its left, y up
    from the feet, z forward), for the server (Projectiles) to rotate by body yaw. One per wand tier."""
    upper, legs = attack_clip()
    i = int(round(IMPACT * SUB))
    p = fk_pose(upper[i], legs[i])
    tiers = {}
    for b in sorted(n for n in GEO_BONES if n.startswith(ST["focus"])):
        fs = [float(_A @ (np.array(c["origin"]) + np.array(c["size"]) * np.array(k) - np.array(GRIP)))
              for bb in _GEO if bb["name"] == b for c in bb.get("cubes", []) for k in ((0, 0, 0), (1, 1, 1))]
        if fs:
            w = K.point(K.world(p, b), ST.get("tip", S(max(fs))))
            tiers[b] = [round(float(w[0]) / 16.0, 3), round(float(w[1]) / 16.0, 3), round(float(w[2]) / 16.0, 3)]
    # world x is the troop's left: the standing right shoulder sits at -x
    assert K.point(K.world({}, "right_arm"), SHOULDER)[0] < 0
    return {"troop": TROOP, "release_tick": IMPACT, "frame": "body: x left, y up from the feet, z forward; blocks",
            "offset": tiers.get(ST["focus"].rstrip("_") + "_t2", tiers.get(ST["focus"])), "tiers": tiers}


def main():
    if os.environ.get("CAST_TIP"):
        t = tip_json()
        Path(os.environ["CAST_TIP"]).write_text(json.dumps(t, indent=1) + "\n")
        print("tip", t)
        return
    if os.environ.get("GG_CAST_ABILITY"):   # the ability clip only, spliced into the built and the exported clip files
        clips = only_rig(ability_clips())
        if os.environ["GG_CAST_ABILITY"] == "splice":
            splice(clips, [K.OUT, ASSET_ANIMS / f"{TROOP}.animation.json"])
        else:
            Path(os.environ["GG_CAST_ABILITY"]).write_text(json.dumps({"format_version": "1.8.0", "animations": clips}, indent=1))
            print("wrote", os.environ["GG_CAST_ABILITY"], "clips:", ", ".join(clips))
        return
    if os.environ.get("CAST_DUMP"):
        dump(os.environ["CAST_DUMP"], [float(x) for x in os.environ.get("CAST_TICKS", "0,3,5.5,8,9.3,10,12,16,22,28").split(",")])
        return
    if os.environ.get("GG_CAST_REVIEW") == "layers":   # after any reactions.py merge for this troop: see review_layers
        review_layers(Path(os.environ["GG_CAST_DRY"]) / f"{TROOP}.animation.json" if os.environ.get("GG_CAST_DRY")
                      else ASSET_ANIMS / f"{TROOP}.animation.json")
        return
    if os.environ.get("GG_CAST_ONLY"):   # rebuild only these groups (hits, attack, ability, stances, idle) in place in both files
        clips = {}
        for g in os.environ["GG_CAST_ONLY"].split(","):
            clips.update(GROUPS[g]())
        alive_clips(clip_nudge(only_rig(clips)))
        paths = [K.OUT, ASSET_ANIMS / f"{TROOP}.animation.json"]
        if os.environ.get("GG_CAST_DRY"):   # a trial: into a copy of the exported file in this folder, the real files untouched
            dry = Path(os.environ["GG_CAST_DRY"]) / f"{TROOP}.animation.json"
            if not dry.exists():
                dry.parent.mkdir(parents=True, exist_ok=True)
                dry.write_bytes(paths[1].read_bytes())
            paths = [dry]
        replace_clips(clips, paths)
        return
    idle_legs, idle_upper = idle_clips()
    walk, run = GAITS.get(TROOP, (K.WALK, K.RUN))
    anims = {"walk_legs": K.gait_legs(walk), "walk_upper": gait_upper(walk, K.WALK_CARRY, False),
             "run_legs": K.gait_legs(run), "run_upper": gait_upper(run, K.RUN_CARRY, True),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **ability_clips(), **stance_clips(), **hit_clips(),
             **death_clips()}
    only_rig(anims)
    clip_nudge(anims)
    alive_clips(anims)
    hang_swing.key_doc(TROOP, {"animations": anims})   # a hanging focus (the hexer's lantern): the swing baked over every clip
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, "clips:", ", ".join(anims))


if __name__ == "__main__":
    main()
