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
overshoot, a recovery and the settle back into guard.
"""
import json, math, os, sys
from pathlib import Path

TROOP = os.environ.get("GG_CAST_TROOP", "mage")
os.environ["GG_ANIM_TROOP"] = TROOP
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "knight"))
import build_anims as K  # noqa: E402

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
             "soft": ("shoulder_cape", "vestment_skirt", "cord")},   # the robe panels are stiff: a fallen body rests on them
    "hexer": {"W": "lantern", "focus": "lantern", "a": (0, 0, -1), "b": (0, 1, 0),
              "gear": {"hood_tail": 0.4, "vestment_front": 0.3, "vestment_back": 0.3, "vestment_skirt": 0.35, "cord": 0.6},
              "soft": ("hood_tail", "vestment_front", "vestment_back", "vestment_skirt", "cord"),   # the burial robe is stiff
              # the lantern body hangs below the pole tip, off the axis: its centre, top and bottom are probed too
              "probe": ((-5.0, 10.0, -5.5), (-5.0, 12.5, -5.5), (-5.0, 7.5, -5.5), (-6.5, 10.0, -5.5), (-3.5, 10.0, -5.5))},
    # the mist-caller's driftwood rod runs along local -z from the fist with the conch at its tip (each tier its own bone)
    "tc_mist": {"W": "rod", "focus": "rod", "a": (0, 0, -1), "b": (0, 1, 0),
                "gear": {"hood_peak": 0.3, "right_kelp_hood": 0.35, "left_kelp_hood": 0.35, "mist_back": 0.6, "sash_tail": 0.3,
                         "charm": 0.5},
                "soft": ("hood_peak", "right_kelp_hood", "left_kelp_hood", "mist_back", "right_mist", "left_mist", "sash_tail",
                         "charm")},   # the kelp, mist strips and sash hang loose; the robe skirt and hem are stiff
}
ST = CAST_TROOPS[TROOP]
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
ATTACK_TICKS = 36
IMPACT = 10         # caster_cast impactTicks: the spell leaves


def mage_set():
    """The sun mage. The guard: the staff held up before the lead shoulder, the sun disc high and facing the foe, the free hand
    open at the belly. The cast: the body coils back over the rear foot as the staff is swept up overhead and both arms rise
    to gather the light; the load is held; the body drives forward over the lead foot and the staff is thrust at the foe, the
    disc presented, the off hand flung after it; overshoot, recover, settle."""
    g = KEY((4, 10, 0), GAZE, (-1.5, -5.0, 6.5), aim(78, -12), u=FWD, lp=(-60, 0, -10, -45))
    # the gather keeps the staff a hand forward of the cowl and out to the right, so in side view it rises clear of the body
    gather = KEY((-5, 20, 0), (2, 0, 0), (-2.5, -2.5, 6.6), aim(86, -6), u=FWD, lp=(-95, 0, -25, -40))
    up = KEY((-11, 25, 0), (-8, 0, 0), (-2.4, 2.6, 7.1), aim(89, -8), u=FWD, lp=(-160, 0, -30, -20))
    # the load: the body keeps coiling back and round over the rear foot while the staff is held up
    held = nudge(up, torso=(-4, 5, 0), head=(-2, 0, 0), R=(0.0, 0.3, 0.0), d=aim(92, -8), lp=(-8, 0, -4, 0))
    drive = KEY((8, -8, 0), (2, 0, 0), (0.8, 2.4, 6.6), aim(48, 14), u=FWD, lp=(-120, 0, 0, -30))
    # the release: the hips and chest drive and twist through, the arm thrown out and the staff levelled at the foe
    release = KEY((26, -20, 0), (8, 0, 0), (1.3, 0.4, 7.7), aim(26, 9), u=FWD, lp=(-75, 0, 12, -5))
    over = KEY((30, -24, -2), (9, 0, 0), (1.5, -0.4, 7.8), aim(17, 11), u=FWD, lp=(-65, 0, 16, -6))
    recover = KEY((12, -4, 0), (5, 0, 0), (0.0, -3.5, 7.0), aim(62, -6), u=FWD, lp=(-65, 0, 0, -40))
    near = nudge(g, torso=(3, -3, 0), R=(0.0, 0.3, -0.1), d=aim(72, -10))
    settle = nudge(g, R=(0.0, 0.15, -0.1), d=aim(79, -12))
    attack = [(0, g), (2.5, gather), (4.5, up), (8.4, held), (9.3, drive), (IMPACT, release), (11.8, over), (16.0, recover),
              (22.0, near), (28.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (4.0, (-4.0, 30.0, 0.0), (0.0, -1.4, -0.7)), (8.4, (-6.0, 34.0, 0.0), (0.0, -1.7, -1.0)),
                     (IMPACT, (10.0, 0.0, 0.0), (0.0, -3.5, 1.8)), (11.8, (12.0, -4.0, -2.0), (0.0, -3.8, 2.1)),
                     (16.0, (5.0, 10.0, 0.0), (0.0, -2.4, 1.0)), (24.0, (1.0, 18.0, 0.0), (0.0, -1.5, 0.3)),
                     (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            "steps": [("left", 6.8, 9.8, (3.7, 2.0, 5.0), 18.0), ("right", 10.4, 14.0, (-3.0, 2.0, 0.0), -4.0, 0.6),
                      ("left", 20.0, 26.0, (3.4, 2.0, 2.4), 14.0), ("right", 24.0, 30.0, (-3.2, 2.0, -2.2), -6.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): a deep crouch, hunched over the staff planted upright beside the lead foot, the off
        # hand low on the knee
        "brace": KEY((16, 6, 0), (2, 0, 0), (-2.2, -4.6, 5.6), aim(90, -4), u=FWD, lp=(-55, 0, -10, -45)),
        # wall (Hold the line): standing tall, the staff pushed out in front and leaned forward as a barrier, the off
        # hand raised flat behind it
        "wall": KEY((4, 6, 0), (0, 0, 0), (1.0, -3.5, 7.1), aim(68, 8), u=FWD, lp=(-100, 0, -15, -15)),
        # ward (fall back): the staff slanted across the body as a bar, the off hand up to fend
        "ward": KEY((2, 8, 0), (0, 0, 0), (2.5, -5.0, 5.0), unit((0.6, 0.75, 0.25)), u=FWD, lp=(-70, 0, -30, -70)),
    }
    carry = {"walk": 75.0, "run": 35.0, "u": FWD}
    return g, attack, legs, stances, carry


def hexer_set():
    """The gravecaller. The guard: the lantern held out before the body on its short pole, the light hanging at the belly, the
    free hand open low. The cast is an underhand swing (a censer's, not the mage's overhead gather): the body coils back and
    sinks over the rear foot as the lantern is swept down and back past the right hip, the free hand reaching out to mark the
    foe; the load is held low; the hips drive forward over the lead foot and the lantern swings up through the front and is
    thrust at the foe at chest height, the curse leaving on the impact tick, the off hand flung after it; overshoot, recover,
    settle."""
    g = KEY((6, 10, 0), (6, 0, 0), (1.5, -4.6, 6.2), aim(14, -6), u=UP, lp=(-45, 0, -12, -40))
    gather = KEY((-2, 22, 0), (6, 0, 0), (-1.2, -7.0, 3.2), aim(-18, -18), u=UP, lp=(-70, 0, -8, -30))
    # the load: the lantern swept low and back past the right hip, the body coiled round over the rear foot, the off hand
    # stretched out at the foe
    low = KEY((-6, 26, 0), (8, 0, 0), (-2.4, -6.6, -2.0), aim(-32, -26), u=UP, lp=(-88, 0, -14, -10))
    # the coil keeps winding through the held load (sinking and turning further), so it builds rather than parks
    held = nudge(low, torso=(-6, 12, 0), head=(2, 0, 0), R=(-0.6, -0.1, -1.0), d=aim(-42, -32))
    # the swing comes through wide of the right hip, not across the body
    drive = KEY((10, -2, 0), (6, 0, 0), (-2.8, -5.8, 4.2), aim(4, -14), u=UP, lp=(-70, 0, -4, -30))
    # the release: the hips and chest drive through, the arm swung up and out, the lantern thrust at the foe
    release = KEY((29, -18, 0), (2, 0, 0), (0.2, -1.0, 7.7), aim(24, 6), u=UP, lp=(-85, 0, 10, -8))
    over = KEY((32, -22, -2), (3, 0, 0), (0.6, -0.2, 7.6), aim(30, 9), u=UP, lp=(-78, 0, 14, -10))
    recover = KEY((12, 0, 0), (6, 0, 0), (0.4, -3.6, 6.8), aim(20, -2), u=UP, lp=(-55, 0, 0, -35))
    near = nudge(g, torso=(3, -3, 0), R=(0.0, 0.3, 0.1), d=aim(17, -5))
    settle = nudge(g, R=(0.0, 0.15, 0.05), d=aim(15, -6))
    attack = [(0, g), (2.5, gather), (4.5, low), (8.4, held), (9.3, drive), (IMPACT, release), (11.8, over), (16.0, recover),
              (22.0, near), (28.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (4.0, (-3.0, 32.0, 0.0), (0.0, -1.9, -0.8)), (8.4, (-5.0, 38.0, 0.0), (0.0, -2.5, -1.15)),
                     (IMPACT, (11.0, 2.0, 0.0), (0.0, -3.4, 2.0)), (11.8, (12.0, -3.0, -2.0), (0.0, -3.7, 2.2)),
                     (16.0, (5.0, 10.0, 0.0), (0.0, -2.4, 1.0)), (24.0, (1.0, 18.0, 0.0), (0.0, -1.5, 0.3)),
                     (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            "steps": [("left", 6.8, 9.8, (3.7, 2.0, 5.0), 18.0), ("right", 10.4, 14.0, (-3.0, 2.0, 0.0), -4.0, 0.6),
                      ("left", 20.0, 26.0, (3.4, 2.0, 2.4), 14.0), ("right", 24.0, 30.0, (-3.2, 2.0, -2.2), -6.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): a deep crouch, the lantern held low and close before the lead knee, shielded, the off hand
        # low on the knee
        "brace": KEY((26, 8, 0), (10, 0, 0), (0.0, -7.4, 3.4), aim(-16, -4), u=UP, lp=(-40, 0, -10, -60)),
        # wall (Hold the line): standing tall, the lantern raised high at arm's length as a warding light, the off hand
        # raised flat beside it
        "wall": KEY((0, 6, 0), (-6, 0, 0), (0.4, 4.2, 6.5), aim(38, 6), u=UP, lp=(-120, 0, -12, -10)),
        # ward (fall back): the lantern pulled in to the chest, the pole up across the body, the off hand up to fend
        "ward": KEY((2, 8, 0), (2, 0, 0), (1.4, -3.8, 5.8), unit((0.45, 0.7, 0.55)), u=UP, lp=(-70, 0, -30, -70)),
    }
    carry = {"walk": 12.0, "run": 2.0, "u": UP}
    return g, attack, legs, stances, carry


def tc_mist_set():
    """The mist caller. The guard: the driftwood rod held up and forward before the lead shoulder, the conch tilted at the
    foe, the free hand open low. The cast is a backhand fan sweep (not the mage's overhead thrust or the hexer's underhand
    swing) that spreads the fog: the right shoulder turns forward and the body sinks as the rod is drawn across the chest
    and back past the left side, the free hand swung back; the load winds further; the hips open and the rod sweeps flat
    out through the front, levelled at the foe on the impact tick as the free hand is flung wide the other way (the arms
    part the mist); the sweep carries on out to the right, recover, settle."""
    g = KEY((5, 10, 0), (4, 0, 0), (1.5, -4.5, 6.4), aim(48, -16), u=UP, lp=(-30, 0, -14, -30))
    gather = KEY((3, -18, 0), (5, 0, 0), (4.0, -5.5, 5.0), aim(22, 62), u=UP, lp=(-15, 0, -18, -30))
    # the load: the right shoulder turned forward and the rod drawn across the chest and back past the left side, the conch tipped up to show past the shoulder,
    # the off hand swung back and out
    load = KEY((8, -34, 0), (6, 0, 0), (5.0, -5.0, 3.2), aim(18, 105), u=UP, lp=(30, 0, -25, -15))
    # the wind keeps tightening through the held load (sinking and turning further), so it builds rather than parks
    held = nudge(load, torso=(2, -4, 0), head=(1, 0, 0), R=(0.2, -0.3, -0.2), d=aim(14, 112), lp=(5, 0, -3, 0))
    # the sweep comes out flat: the fist leads across the front, the rod trailing behind it
    drive = KEY((10, -16, 0), (5, 0, 0), (3.2, -4.6, 5.8), aim(2, 48), u=UP, lp=(-30, 0, -45, -10))
    # the release: the hips open and the arm sweeps out, the rod levelled at the foe, the off hand flung wide to the left
    release = KEY((20, -12, 0), (6, 0, 0), (0.4, -2.6, 7.6), aim(-5, -2), u=UP, lp=(-45, 0, -75, -10))
    over = KEY((22, -6, -2), (6, 0, 0), (-2.4, -2.8, 7.0), aim(-4, -38), u=UP, lp=(-40, 0, -80, -10))
    recover = KEY((12, 2, 0), (5, 0, 0), (-1.2, -4.6, 6.2), aim(16, -30), u=UP, lp=(-40, 0, -40, -30))
    near = nudge(g, torso=(3, -3, 0), R=(-0.2, 0.2, 0.0), d=aim(32, -14))
    settle = nudge(g, R=(0.0, 0.1, 0.0), d=aim(36, -11))
    attack = [(0, g), (2.5, gather), (4.5, load), (8.4, held), (9.1, drive), (IMPACT, release), (11.8, over), (16.0, recover),
              (22.0, near), (28.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    legs = {"hips": [(0, R0, R1), (1.8, (2.0, 2.0, 0.0), (0.0, -1.6, -0.4)), (4.0, (4.0, 6.0, 0.0), (0.0, -2.0, -0.8)), (8.4, (5.0, 2.0, 0.0), (0.0, -2.5, -1.1)),
                     (IMPACT, (12.0, 22.0, 0.0), (0.0, -3.4, 2.0)), (11.8, (13.0, 30.0, -2.0), (0.0, -3.7, 2.2)),
                     (16.0, (5.0, 24.0, 0.0), (0.0, -2.4, 1.0)), (24.0, (1.0, 18.0, 0.0), (0.0, -1.5, 0.3)),
                     (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            "steps": [("left", 6.8, 9.8, (3.7, 2.0, 5.0), 18.0), ("right", 10.4, 14.0, (-3.0, 2.0, 0.0), -4.0, 0.6),
                      ("left", 20.0, 26.0, (3.4, 2.0, 2.4), 14.0), ("right", 24.0, 30.0, (-3.2, 2.0, -2.2), -6.0)]}
    stances = {
        "ready": g,
        # brace (Hold position): a deep crouch, the rod held low and level over the ground before the lead knee, the
        # conch at the foe, the off hand low and palm down
        "brace": KEY((24, 8, 0), (10, 0, 0), (0.6, -7.0, 3.8), aim(-22, -4), u=UP, lp=(-40, 0, -14, -55)),
        # wall (Hold the line): standing tall, the rod raised high overhead with the conch to the sky, the off hand
        # raised beside it
        "wall": KEY((0, 6, 0), (-6, 0, 0), (0.0, 6.6, 4.4), aim(68, 0), u=FWD, lp=(-100, 0, -25, -20)),
        # ward (fall back): the rod held crosswise before the chest as a bar, the off hand up to fend
        "ward": KEY((2, 8, 0), (2, 0, 0), (1.5, -4.0, 6.5), unit((1.0, 0.05, 0.15)), u=UP, lp=(-80, 0, -25, -75)),
    }
    carry = {"walk": 45.0, "run": 20.0, "u": UP}
    return g, attack, legs, stances, carry


GUARD, ATTACK_KEYS, ATTACK_LEGS, STANCE_KEYS, CARRY = {"mage": mage_set, "hexer": hexer_set, "tc_mist": tc_mist_set}[TROOP]()


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
    print("attack_cast: fist err max %.3f px, samples with the focus in the body %d %s" % (sol.worst, len(hits), hits[:8]))
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
    pitch = CARRY["run" if run else "walk"] + 3.0 * math.cos(tw * (u - c.arm_lag - 0.12))
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
    print("%s carry: butt lowest %.1f px above the ground, samples with the focus in the body %d" % (g.name, low, hits))
    samples.append(samples[0])
    return K.bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Stances and idle: all in guard (STYLE.md: the guard is the ready pose; idle stays in it). StanceController plays ready
# (fighting), brace / wall (Hold), ward (fall_back).
# ------------------------------------------------------------------------------------------------

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
        # the focus rides the breath a beat behind it, its point drifting
        R = np.array(k["R"]) + np.array([0.0, 0.12 * br + 0.15 * wl, 0.1 * wl])
        dd = np.array(k["d"]) + np.array([0.0, 0.006 * br + 0.01 * wl, 0.0])
        lp = np.array(k["lp"]) + np.array([-0.8 * wl, 0.0, 0.0, -1.2 * wl])
        d, uu = frame(dd, k["u"])
        s = sol.pose(legs, torso, head, R, d, uu, k["rp"], lp)
        out.append(s)
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
# Hits: additive offsets. The right fist keeps the focus; the free left arm flies wide and the torso takes the blow.
# ------------------------------------------------------------------------------------------------

off = K.off
HIT_PEAK = {
    "front": off(torso=(-17, -10, 0), head=(-12, 3, 0), rarm=(-10, 0, 14), larm=(-24, 0, -40), lfore=-14),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 10), larm=(14, 0, -24)),
    "right": off(torso=(-5, 14, 8), head=(0, 6, 3), rarm=(6, 0, 10), larm=(-12, 0, -22)),
    "left": off(torso=(-5, -14, -8), head=(0, -6, -3), rarm=(-10, 0, 14), larm=(12, 0, -30)),
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

FLAT_AT = {"death_back": (8, 12), "death_forward": (13, 16), "death_left": (9, 13), "death_right": (9, 13)}
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


# Per-troop tweaks on the knight's falls ({fall: {key tick: {bone: rot}}}), so the robed body lies on the ground and not
# propped on its bell sleeves and leg robes.
# Per-troop final-pose changes to the knight falls (tick: bone: rotation): the side falls lie on the cowl and robe
# with the arm tucked under rather than propped on the bell sleeve.
_ROBE_SIDE_FALLS = {
    "death_left": {17: {"head": (-15, 0, 30), "right_arm": (-30, 0, 20)}, 20: {"head": (-20, 0, 30), "right_arm": (-30, 0, 20)}},
    "death_right": {17: {"root": (10, 90, -88), "left_arm": (0, 0, -20)}, 20: {"root": (10, 100, -88), "left_arm": (0, 0, -20)}},
}
# the hexer takes only the left-fall tuck: its right fall already lies lower untweaked (probe 2026-11-06)
DEATH_TWEAK = {"mage": _ROBE_SIDE_FALLS, "hexer": {"death_left": _ROBE_SIDE_FALLS["death_left"]}}.get(TROOP, {})


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
        return {n: K.death_clip(flat_weapon(k, start, *FLAT_AT[n]), start, n) for n, k in falls.items()}
    finally:
        K.bake = bake


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
        hits = shaft_hits(p)
        print("t%-5s focus-in-body %-3d butt y %.1f tip y %.1f" % (t, len(hits), wpoint(p, S(BUTT))[1], wpoint(p, S(TIP))[1]))
    Path(path).write_text(json.dumps(rows))
    print("dumped", len(rows), "poses to", path)


def main():
    if os.environ.get("CAST_DUMP"):
        dump(os.environ["CAST_DUMP"], [float(x) for x in os.environ.get("CAST_TICKS", "0,3,5.5,8,9.3,10,12,16,22,28").split(",")])
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
