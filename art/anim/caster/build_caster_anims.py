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


def reach_left(pose, ls, prior, warm=None, w=0.02):
    """The left arm (x, y, z), forearm x and the shaft point s (px from the grip) putting the left fist on the focus's axis,
    nearest the prior arm and ls; s stays 3.5 px clear of the right fist on ls's side, inside the shaft."""
    prior = np.asarray(prior[:4], float)
    lo, hi = (3.5, TIP - 5.0) if ls >= 0 else (BUTT + 1.0, -3.5)

    def f(x):
        p = K.point(K.world(_arm("left", pose, x), "left_hand"), LGRIP)
        return np.concatenate([4.0 * (p - wpoint(pose, S(x[4]))), w * (x[:4] - prior), [0.05 * (x[4] - ls)]])

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
if ROBED:   # caster v2: upright, the feet close under the hanging robe, the hem clear of the floor
    READY_HIPS = ((0.0, 16.0, 0.0), (0.0, -0.9, 0.1))
    READY_FEET = {"left": ((2.9, 2.0, 1.1), 10.0), "right": ((-2.8, 2.0, -1.1), -6.0)}
K.READY_HIPS, K.READY_FEET = READY_HIPS, READY_FEET
STANCE_LEGS = {
    "ready": {"hips": READY_HIPS, "left": (3.4, 2.0, 2.4), "right": (-3.2, 2.0, -2.2), "yaw": (-6.0, 14.0)},
    "brace": {"hips": ((6.0, 24.0, 0.0), (0.0, -3.4, 0.6)), "left": (3.6, 2.0, 3.4), "right": (-3.4, 2.0, -3.6), "yaw": (-10.0, 22.0)},
    "wall": {"hips": ((0.0, 14.0, 0.0), (0.0, -1.1, 0.2)), "left": (3.2, 2.0, 2.8), "right": (-3.0, 2.0, -2.4), "yaw": (-6.0, 14.0)},
}
if ROBED:   # caster v2: no shield crouch; the brace stands upright on a slightly wider footing, the wall leans in
    STANCE_LEGS = {
        "ready": {"hips": READY_HIPS, "left": (2.9, 2.0, 1.1), "right": (-2.8, 2.0, -1.1), "yaw": (-6.0, 10.0)},
        "brace": {"hips": ((-2.0, 6.0, 0.0), (0.0, -0.8, -0.1)), "left": (3.1, 2.0, 0.6), "right": (-3.1, 2.0, -0.6), "yaw": (-10.0, 10.0)},
        "wall": {"hips": ((3.0, 10.0, 0.0), (0.0, -1.0, 0.4)), "left": (2.8, 2.0, 1.6), "right": (-2.7, 2.0, -1.3), "yaw": (-6.0, 8.0)},
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


def KEY(torso, gaze, R, d, u=SIDE, rp=RP, lp=LP, q=0.0, Lq=QGRIP, lh=0.0, ls=0.0):
    """lh: how far the left hand is on the focus (0 the free arm at lp, 1 the left fist solved onto the shaft); ls: the shaft
    point (px from the grip along a) the left fist prefers."""
    d, u = frame(d, u)
    return {"torso": tuple(map(float, torso)), "head": tuple(map(float, gaze)), "R": tuple(map(float, R)), "d": tuple(d),
            "u": tuple(u), "rp": tuple(map(float, rp)), "lp": tuple(map(float, lp)), "q": (float(q),), "Lq": tuple(map(float, Lq)),
            "lh": (float(lh),), "ls": (float(ls),)}


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
    gather = KEY((-3, 6, 0), (-4, -8, 0), (2.5, -2.5, 6.5), unit((0.35, 0.9, 0.2)), u=FWD, rp=(-80, 0, 10, -40),
                 lp=(-60, 0, -15, -50), lh=0.6, ls=4.5)
    up = KEY((-6, 4, 0), (-10, -16, 0), (3.0, -0.5, 6.5), unit((0.45, 0.85, 0.2)), u=FWD, rp=(-110, 0, 10, -40),
             lp=(-90, 0, -20, -50), lh=1, ls=4.5)
    # the light is held: a tremor, the arch deepening a little
    held = nudge(up, torso=(-1, 1, 0), head=(-2, 0, 0), R=(0.0, 0.2, 0.0))
    held2 = nudge(up, torso=(-2, 2, 0), head=(-1, 0, 0), R=(0.1, 0.3, 0.1), d=unit((0.48, 0.83, 0.24)))
    # the drive: the hips have gone first; the torso follows, the left hand comes off and the staff starts to tip
    drive = KEY((6, -4, 0), (2, -6, 0), (0.0, 1.2, 7.3), aim(56, 0), u=FWD, rp=(-100, 0, 0, -20), lp=(-90, 0, 0, -30),
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
            "steps": [("left", 7.8, 10.0, (2.9, 2.0, 2.6), 12.0, 0.3), ("right", 10.2, 13.0, (-2.8, 2.0, 0.2), -4.0, 0.3),
                      ("left", 20.0, 25.0, (2.9, 2.0, 1.1), 10.0, 0.3), ("right", 23.0, 28.0, (-2.8, 2.0, -1.1), -6.0, 0.3)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the staff held crosswise as a guard bar at the brow, in front of the
        # cowl (the 8 px arms cannot lift it over the crown), the sun out to the left; no crouch
        "brace": KEY((4, 4, 0), (-16, 0, 0), (1.5, 3.5, 7.0), unit((1, 0.12, 0)), u=FWD, rp=(-150, 0, 10, -30),
                     lp=(-150, 0, -10, -20), lh=1, ls=9),
        # wall (Hold the line): the staff levelled at the foe in both hands like a pike, the sun head out front at chest
        # height, the butt by the right hip; the shoulders turn to bring the left hand forward on the shaft, the head stays on
        # the foe (v1's one-handed raised staff read as the guard from the front)
        "wall": KEY((8, 30, 0), (-2, -21, 0), (0.5, -5.0, 5.0), aim(12, 15), u=FWD, rp=(-60, 0, 10, -30), lp=(-70, 0, -10, -30),
                    lh=1, ls=6),
        # ward (fall back): both hands on the staff, slanted across the body as a bar, low on the right, the sun high left
        "ward": KEY((2, 12, 0), (2, 0, 0), (2.0, -5.5, 5.4), unit((0.62, 0.56, 0.26)), u=FWD, rp=(-40, 0, 20, -40),
                    lp=(-80, 0, -10, -60), lh=1, ls=8),
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
    lp_rest = (-12, 0, -12, -40)          # the off hand relaxed at the hip, by the cord
    g = KEY((3, 8, 0), GAZE, (1.2, -5.0, 6.0), aim(12, -6), u=UP, rp=(-45, 0, 10, -25), lp=lp_rest)
    # the load: the weight goes back onto the rear foot before the arm moves (the hips lead, see legs)
    load = nudge(g, torso=(-2, 4, 0), head=(-1, 0, 0), R=(0.0, 0.3, -0.5), d=aim(16, -8))
    # the haul: the lantern drawn in to the chest, the pole tipped up, the right side turned away
    haul = KEY((-4, 18, 0), (2, 0, 0), (1.8, -2.4, 5.9), aim(48, 8), u=UP, rp=(-80, 0, 25, -100), lp=(-18, 0, -12, -44))
    coil = nudge(haul, torso=(-1, 3, 0), R=(-0.1, 0.2, -0.2), d=aim(52, 10))
    # the push: the arm drives the lantern out at the foe, heavy, the shoulder coming round
    push = KEY((6, -10, 0), (3, 0, 0), (0.2, 0.2, 7.5), aim(14, 2), u=UP, rp=(-88, 0, 5, -15), lp=(-4, 0, -12, -38))
    # held at arm's length while the light gathers: the arm locked, a heavy tremor, the lantern swaying on its pole
    held = KEY((7, -12, 0), (3, 0, 0), (0.0, 0.4, 7.9), aim(9, 0), u=UP, rp=(-90, 0, 0, -5), lp=(-2, 0, -12, -36))
    held2 = nudge(held, torso=(0.5, -0.5, 0), R=(0.05, -0.12, -0.05), d=aim(7.5, 0.8))
    held3 = nudge(held, torso=(0.8, -1, 0), R=(-0.05, 0.1, -0.08), d=aim(10.5, -0.6))
    # the heave: the hips have gone forward; the shoulder holds back a beat and the lantern lifts a touch on its pole
    heave = nudge(held, torso=(-1, 2, 0), R=(0.0, 0.35, -0.5), d=aim(20, 0))
    # the release: the shoulder follows the hips through, the lantern is shoved at the foe and the blight leaves it
    release = KEY((14, -26, 0), (5, 0, 0), (0.0, 0.9, 7.9), aim(2, 0), u=UP, rp=(-90, 0, 0, -3), lp=(8, 0, -14, -30))
    # the kick: the lantern bounces up on its pole and the arm gives back through the shoulder
    kick = KEY((10, -20, 0), (4, 0, 0), (0.0, 2.2, 6.9), aim(36, 2), u=UP, rp=(-95, 0, 0, -20), lp=(5, 0, -12, -32))
    back = KEY((5, -8, 0), (4, 0, 0), (0.6, -1.6, 6.8), aim(10, -2), u=UP, rp=(-75, 0, 5, -20), lp=(-4, 0, -12, -38))
    recover = KEY((3, 4, 0), GAZE, (1.0, -4.4, 6.1), aim(14, -5), u=UP, rp=(-52, 0, 10, -24), lp=lp_rest)
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
                     (9.4, (6.0, 2.0, 0.0), (0.0, -1.3, 0.8)), (IMPACT, (8.0, -2.0, 0.0), (0.0, -1.5, 1.2)),
                     (11.8, (8.0, -4.0, 0.0), (0.0, -1.6, 1.3)), (16.0, (4.0, 6.0, 0.0), (0.0, -1.3, 0.7)),
                     (24.0, (1.0, 12.0, 0.0), (0.0, -1.0, 0.3)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides under the robe: the lead foot slides forward as the hips drive, the rear foot drags after
            "steps": [("left", 8.2, 10.0, (2.9, 2.0, 2.6), 12.0, 0.3), ("right", 10.2, 13.0, (-2.8, 2.0, 0.2), -4.0, 0.3),
                      ("left", 20.0, 25.0, (2.9, 2.0, 1.1), 10.0, 0.3), ("right", 23.0, 28.0, (-2.8, 2.0, -1.1), -6.0, 0.3)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the lantern lifted high over the head on its pole as a warding light,
        # the off hand raised flat beside it; no crouch
        "brace": KEY((-2, 4, 0), (2, 0, 0), (1.0, 7.6, 2.0), aim(25, 35), u=UP, rp=(-170, 0, 10, -30),
                     lp=(-165, 0, -20, -10)),
        # wall (Hold the line): leaning in, the lantern held out at the foe at arm's length, high, the off hand pushed
        # forward open below it
        "wall": KEY((6, 2, 0), (0, 0, 0), (0.5, 3.0, 7.2), aim(30, 0), u=UP, rp=(-120, 0, 5, -10), lp=(-95, 0, -20, -10)),
        # ward (fall back): shrinking away, the right side turned back, the lantern hugged in at the chest on the left, the
        # off hand up beside the face to fend
        "ward": KEY((4, 20, 0), (0, 0, 0), (3.0, -3.0, 4.0), unit((0.6, 0.35, 0.5)), u=UP, rp=(-60, 0, 30, -70),
                    lp=(-70, 0, -30, -70)),
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
    lp_rest = (-12, 0, -12, -40)          # the off hand relaxed at the hip, by the sash
    g = KEY((3, 8, 0), GAZE, (1.0, -6.0, 5.0), aim(14, -8), u=UP, rp=(-40, 0, 10, -20), lp=lp_rest)
    # the turn: the shoulders come round side-on and the rod hand rises past the chest, the conch tipping up
    turn = KEY((2, -16, 0), GAZE, (2.4, -3.2, 4.4), aim(34, 16), u=UP, rp=(-60, 0, 20, -60), lp=(-10, 0, -12, -40))
    chest = KEY((1, -28, 0), GAZE, (2.0, -0.6, 4.6), aim(52, 6), u=UP, rp=(-85, 0, 25, -85), lp=(-8, 0, -12, -42))
    # point: the arm straight out at the foe, the elbow locked, the rod level along it
    point = KEY((0, -32, 0), GAZE, (0.0, 0.5, 7.95), aim(2, 0), u=UP, rp=(-90, 0, 0, -5), lp=(-8, 0, -12, -42))
    # the charge: the rod holds steady, a breath of tremor only
    charge = nudge(point, R=(0.0, 0.05, -0.02), d=aim(2.5, 0.3))
    held = nudge(point, R=(0.0, 0.02, -0.03), d=aim(1.8, -0.2), lp=(-1, 0, 0, -1))
    # the wrist cocks up before it flicks (the long rod reads it at a distance: the flick spans 28 degrees)
    cock = nudge(point, R=(0.0, 0.15, -0.05), d=aim(20, 0))
    # the release: the flick down at the foe, the mist leaves the conch
    release = nudge(point, R=(0.0, 0.1, 0.0), d=aim(-8, 0))
    # the recoil back through the shoulder: the fist kicks up and back, the torso rocks back
    recoil = KEY((-2, -30, 0), GAZE, (0.0, 1.2, 6.7), aim(10, 2), u=UP, rp=(-95, 0, 0, -20), lp=(-10, 0, -12, -42))
    back = KEY((0, -24, 0), GAZE, (0.4, -0.4, 6.8), aim(6, 0), u=UP, rp=(-80, 0, 5, -25), lp=(-10, 0, -12, -42))
    recover = KEY((2, -6, 0), GAZE, (0.8, -4.0, 6.0), aim(8, -4), u=UP, rp=(-55, 0, 10, -20), lp=lp_rest)
    near = nudge(g, torso=(0, -2, 0), R=(0.0, 0.2, 0.0), d=aim(11, -7))
    settle = nudge(g, R=(0.0, 0.05, 0.0))
    attack = [(0, g), (2.0, turn), (3.6, chest), (5.0, point), (7.0, charge), (8.6, held), (9.4, cock), (IMPACT, release),
              (11.4, recoil), (14.0, back), (19.0, recover), (24.0, near), (29.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    side_on = (0.0, -14.0, 0.0)
    legs = {"hips": [(0, R0, R1), (2.0, (0.0, 2.0, 0.0), (0.0, -0.9, 0.0)), (4.5, side_on, (0.0, -1.0, -0.1)),
                     (9.0, (0.0, -15.0, 0.0), (0.0, -1.0, -0.1)), (IMPACT, (0.0, -15.0, 0.0), (0.0, -1.0, -0.1)),
                     (11.4, (-1.0, -14.0, 0.0), (0.0, -1.0, 0.4)), (14.0, (0.0, -13.0, 0.0), (0.0, -1.0, 0.2)),
                     (22.0, (0.0, 4.0, 0.0), (0.0, -0.9, 0.1)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides: the right foot slides forward under the robe as the body turns side-on, the left eases back
            "steps": [("right", 0.8, 4.2, (-2.6, 2.0, 1.2), -24.0, 0.3), ("left", 2.0, 5.0, (2.9, 2.0, -0.4), 2.0, 0.3),
                      ("left", 18.0, 23.0, (2.9, 2.0, 1.1), 10.0, 0.3), ("right", 21.0, 27.0, (-2.8, 2.0, -1.1), -6.0, 0.3)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the rod held overhead crosswise as a guard bar, the off hand
        # raised to its far end; no crouch
        "brace": KEY((-2, 4, 0), (2, 0, 0), (2.0, 7.5, 1.0), (1.0, 0.0, 0.0), u=FWD, rp=(-170, 0, 10, -30),
                     lp=(-165, 0, -20, -10)),
        # wall (Hold the line): leaning in, the rod raised up and out at the foe at arm's length, the conch high, the off
        # hand pushed forward open below it
        "wall": KEY((6, 2, 0), (0, 0, 0), (0.5, 3.5, 7.0), aim(50, 0), u=UP, rp=(-120, 0, 5, -10), lp=(-95, 0, -20, -10)),
        # ward (fall back): turned away, the rod slanted low across the body, the off hand up beside the face
        "ward": KEY((2, -25, 0), (0, 0, 0), (3.5, -3.5, 4.5), unit((0.7, -0.45, 0.45)), u=UP, rp=(-60, 0, 30, -70),
                    lp=(-70, 0, -30, -70)),
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
    lp_rest = (-12, 0, -12, -40)          # the off hand relaxed at the hip, by the belt
    g = KEY((3, 8, 0), GAZE, (1.0, -6.0, 5.0), aim(-6, -8), u=UP, rp=(-40, 0, 10, -20), lp=lp_rest)
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
    recover = KEY((2, -6, 0), GAZE, (0.8, -4.0, 6.0), aim(-2, -4), u=UP, rp=(-55, 0, 10, -20), lp=lp_rest)
    near = nudge(g, torso=(0, -2, 0), R=(0.0, 0.2, 0.0), d=aim(-5, -7))
    settle = nudge(g, R=(0.0, 0.05, 0.0))
    attack = [(0, g), (2.0, turn), (3.6, chest), (5.0, point), (7.0, charge), (8.6, held), (9.4, cock), (IMPACT, release),
              (11.4, recoil), (14.0, back), (19.0, recover), (24.0, near), (29.0, settle), (ATTACK_TICKS, g)]
    R0, R1 = READY_HIPS
    side_on = (0.0, -14.0, 0.0)
    legs = {"hips": [(0, R0, R1), (2.0, (0.0, 2.0, 0.0), (0.0, -0.9, 0.0)), (4.5, side_on, (0.0, -1.0, -0.1)),
                     (9.0, (0.0, -15.0, 0.0), (0.0, -1.0, -0.1)), (IMPACT, (0.0, -15.0, 0.0), (0.0, -1.0, -0.1)),
                     (11.4, (-1.0, -14.0, 0.0), (0.0, -1.0, 0.4)), (14.0, (0.0, -13.0, 0.0), (0.0, -1.0, 0.2)),
                     (22.0, (0.0, 4.0, 0.0), (0.0, -0.9, 0.1)), (30.0, R0, R1), (ATTACK_TICKS, R0, R1)],
            # short glides: the right foot slides forward under the robe as the body turns side-on, the left eases back
            "steps": [("right", 0.8, 4.2, (-2.6, 2.0, 1.2), -24.0, 0.3), ("left", 2.0, 5.0, (2.9, 2.0, -0.4), 2.0, 0.3),
                      ("left", 18.0, 23.0, (2.9, 2.0, 1.1), 10.0, 0.3), ("right", 21.0, 27.0, (-2.8, 2.0, -1.1), -6.0, 0.3)]}
    stances = {
        "ready": g,
        # brace (Hold position): upright, both arms up, the wand held overhead crosswise as a guard bar, the off hand
        # raised flat to the other end; no crouch
        "brace": KEY((-2, 4, 0), (2, 0, 0), (2.0, 7.5, 1.0), (1.0, 0.0, 0.0), u=FWD, rp=(-170, 0, 10, -30),
                     lp=(-165, 0, -20, -10)),
        # wall (Hold the line): leaning in, the wand pointed up and out at the foe at arm's length, the off hand pushed
        # forward open below it, a warding pair at chest height
        "wall": KEY((6, 2, 0), (0, 0, 0), (0.5, 3.5, 7.0), aim(45, 0), u=UP, rp=(-120, 0, 5, -10), lp=(-95, 0, -20, -10)),
        # ward (fall back): turned away, the wand slanted low across the body, the off hand up beside the face
        "ward": KEY((2, -25, 0), (0, 0, 0), (3.5, -3.5, 3.5), unit((0.7, -0.6, 0.3)), u=UP, rp=(-60, 0, 30, -70),
                    lp=(-70, 0, -30, -70)),
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

    def pose(self, legs, torso, head, R, d, u, rp, lp, q=0.0, Lq=QGRIP, extra=None, w=0.02, lh=0.0, ls=0.0):
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
            x, e = reach_left(pose, ls, lp, self.lwarm)
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
    v = {n: interp(keys, t, n) for n in ("torso", "head", "R", "d", "u", "rp", "lp", "q", "Lq", "lh", "ls")}
    d, u = frame(v["d"], v["u"])
    head = head_local(v["head"], v["torso"], legs_at["hips"][0])
    return sol.pose(legs_at, v["torso"], head, v["R"], d, u, v["rp"], v["lp"], max(0.0, min(1.0, v["q"][0])), v["Lq"], extra, w,
                    max(0.0, min(1.0, v["lh"][0])), v["ls"][0])


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
    gather = KEY((-3, 5, 0), (-6, -6, 0), (2.5, -2.5, 6.5), unit((0.35, 0.9, 0.2)), u=FWD, rp=(-80, 0, 10, -40),
                 lp=(-60, 0, -15, -50), lh=0.7, ls=4.5)
    up = KEY((-7, 2, 0), (-18, -4, 0), (3.5, 0.0, 6.0), unit((0.45, 0.86, 0.2)), u=FWD, rp=(-110, 0, 10, -40),
             lp=(-95, 0, -20, -50), lh=1, ls=4.5)
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
    lift = KEY((0, 10, 0), (0, 0, 0), (-0.5, 3.0, 5.5), aim(60, 10), u=UP, rp=(-130, 0, 10, -40), lp=(-55, 0, -6, -45))
    over = KEY((4, 6, 0), (2, 0, 0), (-2.6, 7.4, 1.0), aim(50, -25), u=UP, rp=(-170, 0, 10, -30), lp=claw)
    # drawing in: the hunch deepens, the claw pulls in a little, a tremor in both arms
    hunch = KEY((10, 4, 0), (4, 0, 0), (-2.6, 7.3, 1.2), aim(48, -22), u=UP, rp=(-170, 0, 10, -30), lp=(-75, 0, 6, -55))
    hunch2 = nudge(hunch, torso=(1, -1, 0), R=(0.1, -0.15, 0.05), d=aim(46, -20), lp=(2, 0, 1, -3))
    hunch3 = nudge(hunch, torso=(2, 1, 0), R=(-0.1, 0.1, -0.05), d=aim(50, -24), lp=(-2, 0, -1, 2))
    hunch4 = nudge(hunch, torso=(3, 0, 0), R=(0.05, -0.1, 0.1), d=aim(47, -21), lp=(1, 0, 1, -5))
    # the last lift: the lantern up and back, the chest arching, the claw drawn back to strike
    coil = KEY((-3, 4, 0), (-2, 0, 0), (-2.4, 7.6, 0.4), aim(62, -15), u=UP, rp=(-175, 0, 10, -30), lp=(-110, 0, -4, -60))
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
            "steps": [("left", 18.6, 20.0, (2.9, 2.0, 2.2), 12.0, 0.3), ("left", 24.0, 28.5, (2.9, 2.0, 1.1), 10.0, 0.3)]}
    return "blight_gravecaller", 30, 20, keys, legs


def mist():
    """C6 the Mist Caller's mist, 30 ticks, release on 20 (the server's windup end, when the cloud lands). Slow and tidal: the
    rod and the free hand draw up and across to the left as the hips wind that way; the body folds forward and both hands sink
    to the ground at the left, then sweep low across in a wide arc, the hips turning through to the right; at the release the
    sweep ends wide at the right, the conch flicking out, the mist laid; the body rises and drifts back to the guard."""
    g = GUARD
    draw = KEY((-2, -22, 0), (0, -10, 0), (4.6, -1.0, 5.0), aim(20, 40), u=UP, rp=(-90, 0, -10, -60), lp=(-40, 0, -45, -30))
    sink = KEY((30, -26, 0), (-12, -26, 0), (4.8, -5.2, 4.2), aim(-35, 55), u=UP, rp=(-70, 0, -15, -30), lp=(-45, 0, -55, -15))
    sweep_l = KEY((36, -18, 0), (-14, -22, 0), (3.6, -6.0, 4.6), aim(-40, 40), u=UP, rp=(-60, 0, -5, -15), lp=(-55, 0, -40, -10))
    sweep_c = KEY((38, 0, 0), (-14, 0, 0), (0.6, -6.0, 4.2), aim(-40, 0), u=UP, rp=(-60, 0, 10, -10), lp=(-60, 0, -10, -10))
    sweep_r = KEY((34, 20, 0), (-12, 22, 0), (-3.6, -5.8, 4.4), aim(-36, -45), u=UP, rp=(-55, 0, 30, -10), lp=(-62, 0, 18, -12))
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
            "steps": [("right", 5.0, 9.0, (-3.4, 2.0, -0.6), -14.0, 0.3), ("right", 23.0, 28.0, (-2.8, 2.0, -1.1), -6.0, 0.3)]}
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
    keys += [(19.5, nudge(spread, torso=(2, 0, 0), head=(4, 0, 0))), (22.5, nudge(g, torso=(2, 0, 0), R=(0.0, -0.3, 0.0))), (25, g)]
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
READY_LIKE = ("ready", "shield_up")   # shield_up is the ready pose: both ride V2_READY_SWAY_SCALE


def stance_upper(k, life, legs_spec, ticks=K.STANCE_TICKS, ready=False):
    n = ticks * SUB
    a_br, a_wt, _ = K.feel.stance_amps(life, ready)
    sol = Solver()
    out = []
    hips = (legs_spec or STANCE_LEGS["ready"])["hips"]
    legs = {"hips": hips}
    for i in range(n + 1):
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
        s = sol.pose(legs, torso, head, R, d, uu, k["rp"], lp, lh=k["lh"][0], ls=k["ls"][0])
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
HIT_PEAK = {
    "front": off(torso=(hb_front(-17), -10, 0), head=(hb(-12), 3, 0), rarm=(-10, 0, 14), larm=(-24, 0, -40), lfore=-14),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 10), larm=(14, 0, -24)),
    "right": off(torso=(hb(-5), 14, 8), head=(0, 6, 3), rarm=(6, 0, 10), larm=(-12, 0, -22)),
    "left": off(torso=(hb(-5), -14, -8), head=(0, -6, -3), rarm=(-10, 0, 14), larm=(12, 0, -30)),
}


def hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for dname, peak in HIT_PEAK.items():
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


def panel_pose(pose, live=True):
    """{panel: (rot, None)} for a pose that has the hips and legs."""
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
# the seer's robe is a stiff hanging skirt, so a fall must not land on a braced arm: on its back the arms settle out wide and
# flat, face down the wolf head turns aside onto its cheek (the snout would prop it), and in a side roll the arm the body
# rolls over lies straight out past the head on the ground instead of bent under it (grid 2026-11-06: torso gap 0.3 back,
# 1.4 face down, ~0.1 over the robe's own floor on the sides)
_SEER_FALLS = {
    "death_back": {16: {"right_arm": (-40, 0, 80), "left_arm": (-30, 0, -40), "left_forearm": (0, 0, 0)},
                   20: {"right_arm": (-40, 0, 80)}},
    "death_forward": {18: {"head": (-25, 60, 0)}, 20: {"head": (-20, 70, 0)}},
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
        return {n: K.death_clip(flat_weapon(k, start, *FLAT_AT[n]), start, n) for n, k in falls.items()}
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
    K.Gait("walk", ticks=6, speed=0.16, duty=0.5, heel=4, toe_off=8, front=3.85, lift=0.9, lift_peak=0.5,
           hip_drop=0.95, bob=0.2, bob_phase=0.06, sway=0.2 * SW, yaw=3.0, roll=0.8 * SW, lane=2.5),
    K.Gait("run", ticks=5, speed=0.26, duty=0.4, heel=4, toe_off=10, front=3.8, lift=1.5, lift_peak=0.5,
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
    idle_legs, idle_upper = idle_clips()
    walk, run = GAITS.get(TROOP, (K.WALK, K.RUN))
    anims = {"walk_legs": K.gait_legs(walk), "walk_upper": gait_upper(walk, K.WALK_CARRY, False),
             "run_legs": K.gait_legs(run), "run_upper": gait_upper(run, K.RUN_CARRY, True),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **ability_clips(), **stance_clips(), **hit_clips(),
             **death_clips()}
    only_rig(anims)
    hang_swing.key_doc(TROOP, {"animations": anims})   # a hanging focus (the hexer's lantern): the swing baked over every clip
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, "clips:", ", ".join(anims))


if __name__ == "__main__":
    main()
