"""Archer clips (unit animation rebuild, docs/anim-rebuild/STYLE.md): the bow family's pilot.

python art/anim/archer/build_bow_anims.py   ->  art/anim/archer/out/archer.animation.json

Reuses the knight builder's rig maths, gait legs, stance legs, hit footwork and death falls (art/anim/knight/build_anims.py with
GG_ANIM_TROOP=archer: no shield) and keys its own upper body around the bow in the left fist.

The draw channel: the bow is four sibling bones (bow_t<n>_rest / pull_0 / pull_1 / pull_2, rig tag "draw"); the empty "draw" bone's
position x picks which one shows (0 rest, 1 pull_0 arrow nocked, 2 pull_1, 3 pull_2 full draw). GeoTroopRenderer reads it after the
controllers and hides the rest. Unkeyed it is 0: the bow at rest, no arrow.

The shot (attack_volley / attack_direct, impactTicks 16): reach back to the quiver, nock, raise and set, draw to the anchor (the
"full" marker), hold, loose on the server's impact tick, the string hand flying back past the ear, then settle to the ready guard.
AttackController holds the clip at "full" for the troop's seeded hold ticks, so a line of archers never looses in lockstep.
"""
import json, math, os, sys
from pathlib import Path

# The bow family shares this builder: GG_BOW_TROOP=longbow python art/anim/archer/build_bow_anims.py writes the longbowman's clips
# into art/anim/longbow/out from its own rig (BOW_TROOPS: its bow's nock frames, its gear bones, its own shot character).
TROOP = os.environ.get("GG_BOW_TROOP", "archer")
os.environ["GG_ANIM_TROOP"] = TROOP
# Each bow troop's shot character, so its weight reads from its weapon. The longbow's tall stave is a heavier pull: the
# volley leans further back, the hips sink deeper into the draw and rock harder at the loose, and the string hand recoils further.
CHAR = {"archer": {"lean": -16.0, "sink": 1.0, "rock": 1.5, "recoil_z": (-10.0, -16.0), "recoil_f": (95.0, 100.0), "ward_arm": 0.0,
                   "roll_left": ((-180, 0, 30), -20, (90, 0, 0))},
        "longbow": {"lean": -22.0, "sink": 1.35, "rock": 3.5, "recoil_z": (-17.0, -26.0), "recoil_f": (106.0, 112.0), "ward_arm": -7.0,
                    "roll_left": ((-160, 0, 30), -5, (90, 0, 0))},
        # The bone archer: no muscle behind the pull, so little lean and almost no sink; the loose snaps (a hard rock and a sharp,
        # wide recoil of a hand with nothing to slow it), and the bones rattle on.
        "bonebow": {"lean": -12.0, "sink": 0.5, "rock": 2.5, "recoil_z": (-14.0, -22.0), "recoil_f": (100.0, 106.0), "ward_arm": -3.0,
                    "roll_left": ((-180, 0, 30), -20, (90, 0, 0))},
        # The barrow stalker: a patient hunter's long shot, set low (the deepest sink) and steady (the least rock and recoil).
        "stalker": {"lean": -18.0, "sink": 1.6, "rock": 1.0, "recoil_z": (-8.0, -12.0), "recoil_f": (92.0, 96.0), "ward_arm": -4.0,
                    "roll_left": ((-180, 0, 30), -20, (90, 0, 0))},
        # The hide hunter: hunched and crude, a short heave from the shoulders rather than a lean back, a rough loose.
        "wm_hunter": {"lean": -10.0, "sink": 1.2, "rock": 3.0, "recoil_z": (-15.0, -24.0), "recoil_f": (102.0, 108.0), "ward_arm": -5.0,
                      "roll_left": ((-170, 0, 30), -12, (90, 0, 0))}}[TROOP]
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "knight"))
import build_anims as K  # noqa: E402

SUB, TPS, Z3 = K.SUB, K.TPS, K.Z3
K.HELD = K.HELD + ("bow", "weapon")
K.DEATH_BONES = tuple(b for b in K.DEATH_BONES if b != "cape")
GEO_BONES = {b["name"] for b in json.loads((HERE.parent / TROOP / "out" / f"{TROOP}.geo.json").read_text())["minecraft:geometry"][0]["bones"]}
DRAW = {"rest": 0.0, "pull_0": 1.0, "pull_1": 2.0, "pull_2": 3.0}

# ------------------------------------------------------------------------------------------------
# Carry: the bow hangs in the left fist by the thigh, limbs vertical, the string hand loose.
# ------------------------------------------------------------------------------------------------

CARRY = {"left_arm": (-6.0, 0.0, -7.0), "left_forearm": (-28.0, 0.0, 0.0), "left_hand": (-14.0, 0.0, 0.0),
         "right_arm": (-2.0, 0.0, 3.0), "right_forearm": (-22.0, 0.0, 0.0), "right_hand": Z3}


def gear(samples, legs=None, quiver=1.0):
    """Secondary motion (STYLE.md rule 9): the hood's short cape and the tippet trail the torso (the knight's cape lag, scaled to
    the short cloth), and the quiver rocks on its strap."""
    K.cape_lag(samples, legs)
    for s in samples:
        c = s.pop("cape")[0]
        s["hood_cape"] = ((0.6 * c[0], 0.0, 0.6 * c[2]), None)
        s["tippet"] = ((0.4 * c[0], 0.0, 0.4 * c[2]), None)
        s.setdefault("quiver", ((0.0, 0.0, 0.0), None))
    return samples


def gait_pose(g, c, u, run):
    tw = 2 * math.pi
    (hx, hy, hz), _ = g.hips(u)
    ty = (g.yaw + c.twist) * math.cos(tw * u)
    tz = -0.8 * hz - c.roll * math.sin(tw * (u - 0.03))
    tx = c.lean + 0.6 * math.cos(tw * 2 * (u - g.bob_phase))
    net_yaw, net_roll = hy + ty, hz + tz
    head = (-(hx + tx) * c.head_keep, -net_yaw * c.head_keep, -net_roll * c.head_keep)
    sw = math.cos(tw * (u - c.arm_lag))
    ra = (c.arm_base + c.arm * sw, 0.0, c.arm_out)
    rf = (c.fore - 0.35 * c.arm * max(0.0, -sw), 0.0, 0.0)
    bob = math.cos(tw * 2 * (u - g.bob_phase - 0.06))
    # the bow arm swings less than the free arm (the bow's weight), counter to it; running it pumps with the elbow bent
    if run:
        la = (-16.0 - 0.6 * c.arm * sw, 0.0, -10.0)
        lf = (-46.0 + 0.3 * c.arm * min(0.0, sw), 0.0, 0.0)
    else:
        la = (-6.0 - 0.55 * c.arm * sw, 0.0, -7.0)
        lf = (-28.0 - 0.3 * c.arm * max(0.0, sw), 0.0, 0.0)
    # the wrist keeps the bow near vertical in the world, rocking a beat behind the arm
    lh = (-14.0 - 0.5 * (la[0] - CARRY["left_arm"][0]) - 0.4 * (lf[0] - CARRY["left_forearm"][0])
          + 3.0 * math.cos(tw * (u - c.arm_lag - 0.12)), 0.0, 0.0)
    quiver = (1.5 * (2.0 if run else 1.0) * bob, 0.0, 2.0 * (2.0 if run else 1.0) * math.sin(tw * (u - 0.15)))
    flare, flap = c.cape
    cape = (flare + flap * math.sin(tw * 2 * (u - g.bob_phase - 0.12)), -0.5 * ty, -0.6 * tz)
    return {"torso": ((tx, ty, tz), None), "head": (head, None), "right_arm": (ra, None), "right_forearm": (rf, None),
            "right_hand": (Z3, None), "left_arm": (la, None), "left_forearm": (lf, None), "left_hand": (lh, None),
            "hood_cape": ((0.6 * cape[0], 0.0, 0.6 * cape[2]), None), "tippet": ((0.4 * cape[0], 0.0, 0.4 * cape[2]), None),
            "quiver": (quiver, None)}


def gait_upper(g, c, run):
    n = g.ticks * SUB
    samples = [gait_pose(g, c, i / n, run) for i in range(n)]
    samples.append(samples[0])
    return K.bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Idle: the knight's at-ease legs; the bow hangs at the thigh and drifts with the breath.
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
        upper.append({
            "torso": ((1.0 + 0.7 * br, -0.6 * sh, 0.5 * sh), None),
            "head": ((-1.0 - 0.4 * br, 0.75 * math.sin(4 * math.pi * u - 1.0), -0.3 * sh), None),
            "right_arm": ((-2.0 - 0.6 * br, 0.0, 3.0 + 0.5 * br), None),
            "right_forearm": ((-22.0 - 0.8 * lag, 0.0, 0.0), None),
            "right_hand": (Z3, None),
            "left_arm": ((-6.0 - 0.5 * br + 0.4 * lag, 0.0, -7.0 - 0.4 * br), None),
            "left_forearm": ((-28.0 - 1.0 * lag, 0.0, 0.0), None),
            "left_hand": ((-14.0 + 1.2 * lag, 0.0, 0.0), None),
            "hood_cape": ((0.6 + 0.4 * br, 0.0, 0.0), None), "tippet": ((0.4 + 0.3 * br, 0.0, 0.0), None),
            "quiver": ((0.5 * lag, 0.0, 0.0), None),
        })
    return legs, K.bake(upper, K.IDLE_TICKS)


K.idle_clips = idle_clips    # the death falls start from the archer's idle frame (kept as is: deaths are not part of v2)

# V2 feel (Kort, 2026-11-07: "the whole feel and flow of [the Knight's] movements, its weight shifts, and style"). V2 is on by
# default with the knight builder's flag (GG_V2=0 builds the old clips). The draw and the shot are untouched.
V2 = K.V2


def idle_v2_clips():
    """At ease with a real weight cycle (STYLE.md rule 2): the hips slide over one foot and back (contrapposto: the free hip
    drops, its knee softens), the chest counter-tilts so the head stays level, and both arms trail the torso a beat late, the
    bow swinging a little at the thigh. One breath and one full shift (right, centre, left, centre) per 3 s loop."""
    n = K.IDLE_TICKS * SUB
    SWAY = K.feel.sway()      # Kort's variant B: the weight shift and the arm trail at V2_SWAY_SCALE (breath is not sway)
    legs, upper, guesses = [], [], {}
    for i in range(n + 1):
        u = (i % n) / n
        br = math.sin(2 * math.pi * u)
        sh = SWAY * math.sin(2 * math.pi * u + 0.6)       # + weight over the right foot
        lag = SWAY * math.sin(2 * math.pi * u + 0.6 - 0.9)   # the arms follow the shift a few ticks late
        hips = ((0.4 * br, 2.2 * sh, 2.4 * sh), (-0.8 * sh, -0.6 - 0.14 * br - 0.16 * abs(sh), 0.0))   # +z roll lifts the right hip
        pose = {"hips": hips}
        out = {"hips": hips}
        for side, foot in (("right", (-2.4, 2.0, 0.6)), ("left", (2.4, 2.0, -0.4))):
            pose[side + "_leg"] = ((0, -hips[0][1], 0), (0, 0, 0))
            lr, sr, fr = K.leg_pose(pose, side, foot, 0.0, foot_yaw=(-6 if side == "right" else 6), guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            out[side + "_leg"], out[side + "_shin"], out[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        legs.append(out)
        upper.append({
            "torso": ((1.0 + 0.9 * br, -1.4 * sh, -2.8 * sh), None),   # shoulders tilt against the pelvis
            "head": ((-1.0 - 0.5 * br, 0.75 * math.sin(4 * math.pi * u - 1.0) - 0.6 * sh, 0.4 * sh), None),
            "right_arm": ((-2.0 - 0.6 * br + 2.2 * lag, 0.0, 3.0 + 0.5 * br + 1.2 * max(0.0, lag)), None),
            "right_forearm": ((-22.0 - 1.2 * lag, 0.0, 0.0), None),
            "right_hand": (Z3, None),
            "left_arm": ((-6.0 - 0.5 * br - 2.4 * lag, 0.0, -7.0 - 0.4 * br - 1.2 * max(0.0, -lag)), None),
            "left_forearm": ((-28.0 - 1.4 * lag, 0.0, 0.0), None),
            "left_hand": ((-14.0 + 2.0 * lag, 0.0, 0.0), None),
            "hood_cape": ((0.6 + 0.5 * br, 0.0, 0.8 * lag), None), "tippet": ((0.4 + 0.4 * br, 0.0, 0.6 * lag), None),
            "quiver": ((0.8 * lag, 0.0, 0.6 * lag), None),
        })
    return K.bake(legs, K.IDLE_TICKS), K.bake(upper, K.IDLE_TICKS)

_CORNERS = K.body_corners


def body_corners_no_bow(pose, model=K.HEAVY):
    """The death falls rest the body on its own lowest point: the long bow lies along the ground beside it rather than
    propping the corpse up on a limb tip."""
    P, owner = _CORNERS(pose, model)
    keep = [i for i, o in enumerate(owner) if not any(h in o for h in ("bow", "weapon"))]
    return P[keep], [owner[i] for i in keep]


K.body_corners = body_corners_no_bow

# ------------------------------------------------------------------------------------------------
# Key poses. World shoulder turn = hips yaw + torso yaw; the torso keys assume the ready footing's hips yaw (6), so the bow arm
# points at the foe whether the troop stands (footwork) or walks (gait hips near 0, a few degrees off).
# ------------------------------------------------------------------------------------------------

def P(torso, head, larm, lfore=0.0, lhand=0.0, rarm=Z3, rfore=Z3, rhand=Z3, quiver=Z3):
    return {"torso": tuple(torso), "head": tuple(head), "left_arm": tuple(larm), "left_forearm": (lfore, 0.0, 0.0),
            "left_hand": (lhand, 0.0, 0.0), "right_arm": tuple(rarm), "right_forearm": tuple(rfore), "right_hand": tuple(rhand),
            "quiver": tuple(quiver)}


HIPS_Y = 6.0
# ready guard: bow low and forward across the body, string hand resting on the string, left shoulder leading, eyes on the foe
READY = P((6, 19, 0), (-4, -23, 0), (-38, -18, -8), -14, -10, (-34, -10, 6), (-30, 40, 0))
# reach: the string hand over the right shoulder into the quiver, the bow dropping, the body turning back to open the reach
REACH = P((4, -2, 0), (-2, -2, 0), (-30, -10, -6), -20, -10, (-165, 0, -15), (-60, 0, 0), quiver=(0, 0, -6))
# nock: the arrow drawn down past the ear onto the string in front of the chest, the bow coming up
NOCK = P((5, 14, 0), (-3, -18, 0), (-58, -26, -4), -12, -6, (-62, -20, 4), (-24, 40, 0))
# set: bow arm up and locked at the foe, string hand on the string, shoulders turned side-on
SET = P((4, 30, 0), (-4, -34, 0), (-82, -38, 0), 0, 0, (-80, -30, 0), (0, 40, 0))
# full: drawn to the anchor at the jaw, the back squeezed, side-on
FULL = P((2, 34, 0), (-2, -38, 0), (-90, -40, 0), 0, 0, (-95, -20, 0), (0, 70, 0))
# loose: the string hand flies back past the ear, the bow arm stays on line and kicks a touch forward
LOOSE = P((2, 36, 0), (-4, -38, 0), (-88, -44, 0), 0, 0, (-100, -5, CHAR["recoil_z"][0]), (0, CHAR["recoil_f"][0], 0))
# follow-through: the release carried out, the hand open behind the head, the bow tip dipping
FOLLOW = P((0, 37, 0), (-4, -38, 0), (-84, -46, 0), 0, 4, (-104, 0, CHAR["recoil_z"][1]), (0, CHAR["recoil_f"][1], 0))


def raised(p, lean=CHAR["lean"], head=-8.0, arms=-22.0):
    """The volley: the same shot leaning back from the waist, both arms raised, so the arrow arcs over the line in front."""
    q = dict(p)
    q["torso"] = (p["torso"][0] + lean, p["torso"][1], p["torso"][2])
    q["head"] = (p["head"][0] + head, p["head"][1], p["head"][2])
    q["left_arm"] = (p["left_arm"][0] + arms, p["left_arm"][1], p["left_arm"][2])
    q["right_arm"] = (p["right_arm"][0] + arms, p["right_arm"][1], p["right_arm"][2])
    return q


def mix(a, b, k):
    return {n: tuple(x + (y - x) * k for x, y in zip(a[n], b[n])) for n in a}


ATTACK_TICKS = 26
IMPACT = 16      # bow_volley / bow_direct impactTicks: the arrow leaves
FULL_AT = 12     # the anchor is reached; AttackController holds here for the seeded hold


def shot_keys(volley, up=None):
    up = up or (raised if volley else (lambda p: p))
    tense = dict(up(FULL)); tense["torso"] = (tense["torso"][0] + 0.6, tense["torso"][1] + 1.0, 0.0)
    return [(0, READY), (2.2, mix(READY, REACH, 0.7)), (3.6, REACH), (4.6, REACH),       # reach fast, a beat in the quiver
            (6.6, NOCK), (9, up(SET)), (FULL_AT, up(FULL)), (15.4, tense),               # nock, raise, a steady draw, held
            (IMPACT, up(LOOSE)), (17.6, up(FOLLOW)), (19.5, up(mix(FOLLOW, LOOSE, 0.6))),   # loose fast, carry through
            (22.5, mix(up(SET), READY, 0.75)), (ATTACK_TICKS, READY)]                     # gather down to the guard


# The draw channel, a step per frame: rest while reaching, nocked from the string, two pull frames through the draw, rest at the loose.
DRAW_STEPS = [(0, "rest"), (5.8, "pull_0"), (10.0, "pull_1"), (11.4, "pull_2"), (IMPACT, "rest")]


def draw_at(t):
    v = 0.0
    for t0, f in DRAW_STEPS:
        if t >= t0:
            v = DRAW[f]
    return v


def sample(keys, t):
    out = {}
    for b in keys[0][1]:
        out[b] = (K.curve([(k[0], k[1][b]) for k in keys], t), None)
    return out


# Footwork (STYLE.md rule 4): the rear (left... the bow side) foot steps up to set the shot side-on as the arrow is nocked, the hips
# sink and turn into the draw, the release rocks them, and the foot steps back to the ready footing as the guard returns.
def shot_legs(volley):
    lean = -3.0 if volley else 1.0
    k = CHAR["sink"]
    return {"hips": [(0, *K.READY_HIPS), (3.6, (0.0, 4.0, 0.0), (0.0, -0.9, 0.05)), (7, (0.0, 8.0, 0.0), (0.0, -1.4 * k, 0.15)),
                     (FULL_AT, (lean, 10.0, 0.0), (0.0, -1.7 * k, 0.2)), (IMPACT, (lean, 10.0, 0.0), (0.0, -1.75 * k, 0.2)),
                     (17.4, (lean - CHAR["rock"], 12.0, 0.0), (0.0, -1.6 * k - max(0.0, k - 1), 0.05)),
                     (21, (0.0, 8.0, 0.0), (0.0, -1.3 * k, 0.1)),
                     (ATTACK_TICKS, *K.READY_HIPS)],
            "steps": [("left", 4.4, 7.4, (3.6, 2.0, -0.4), 14.0), ("left", 19.5, 23.5, (3.2, 2.0, -2.0), 7.0)]}


# ------------------------------------------------------------------------------------------------
# The bow line, solved rather than hand-keyed (STYLE.md rule 10: the shot must read from any side).
#
#  - The aim d: the troop's forward, raised by ELEV (the volley arcs over the line in front), turned a few degrees by the solve.
#  - At full draw the nock sits at the anchor A on the string-side cheek. One solve per clip moves the drawn keys' torso, head and
#    bow arm so the pull_2 nock lands there.
#  - Every sample, the bow hand turns the bow onto d (limbs upright about it) and the string hand rides the nock, whose distance
#    behind the grip follows the continuous draw amount; the draw frame shown is the nearest pull frame.
# ------------------------------------------------------------------------------------------------

np = K.np
BOW = "bow_t1_pull_2"                     # every tier's bow bones share the pivot and axes; any of them gives the frame
# Per bow troop: bow-local nock x for rest, pull_0, pull_1, pull_2 (grip x 6, nock y 16.6; build_bows.py), the aim elevations,
# and GEAR, which renames the archer's secondary-motion bones onto the troop's own (None drops the channel).
BOW_TROOPS = {
    "archer": {"nock_x": [2.68, -0.11, -2.81, -5.5], "elev": {"volley": 30.0, "direct": 3.0}, "gear": {}},
    # The longbow: a taller stave with a longer rest brace, lofted higher (it outranges the short bow); its feathered cap and tall
    # quiver take the hood cape's and quiver's lag.
    "longbow": {"nock_x": [3.076, 0.15, -2.675, -5.5], "elev": {"volley": 36.0, "direct": 4.0},
                "gear": {"hood_cape": "long_feather", "tippet": None, "quiver": "tall_quiver"}},
    # Bone, frost and hide-hunter bows share the order bow's draw (build_gb_bone_bow, build_gb_frost_bow: draw FULL_DRAW). Their
    # quivers keep still: the bands, rims and fan arrows are bones of their own that would part from a rocking quiver.
    "bonebow": {"nock_x": [2.711, -0.092, -2.796, -5.5], "elev": {"volley": 32.0, "direct": 3.5},
                "gear": {"hood_cape": "cape", "tippet": "hood_tip", "quiver": None}},
    "stalker": {"nock_x": [2.68, -0.113, -2.806, -5.5], "elev": {"volley": 35.0, "direct": 4.0},
                "gear": {"hood_cape": "pelt_tail", "tippet": "wrap_tails", "quiver": None}},
    "wm_hunter": {"nock_x": [2.68, -0.11, -2.81, -5.5], "elev": {"volley": 28.0, "direct": 3.0},
                  "gear": {"hood_cape": "cape", "tippet": None, "quiver": None}},
}
BT = BOW_TROOPS[TROOP]
NOCK_X = BT["nock_x"]
NOCK_Y = 16.6
ANCHOR = (-5.0, 26.0, -1.0)               # head-local: the string-side cheek, just outside the hood
FIST = (-6.0, 13.0, 0.0)                  # right_forearm-local: the fist that holds the string
ELEV = BT["elev"]


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
        if n < 1e-5:
            break
    return x


def aim(elev, yaw):
    """World aim direction d and the bow's upright u (world up made square to d)."""
    e, y = math.radians(elev), math.radians(yaw)
    d = np.array([math.sin(y) * math.cos(e), math.sin(e), math.cos(y) * math.cos(e)])
    u = np.array([0.0, 1.0, 0.0]) - math.sin(e) * d
    return d, u / np.linalg.norm(u)


def fk_pose(upper, legs):
    pose = {b: (v[0], v[1] or Z3) for b, v in upper.items() if b != "draw"}
    pose.update({b: (v[0], v[1] or Z3) for b, v in legs.items()})
    return pose


_HAND_TO_BOW = []


def bow_wrist(pose, d, u):
    """left_hand rotation turning the bow's arrow axis (bow-local +x) onto d with its limbs (bow-local +y) along u."""
    if not _HAND_TO_BOW:
        _HAND_TO_BOW.append(K.world({}, "left_hand")[:3, :3].T @ K.world({}, BOW)[:3, :3])
    F = np.diag([-1.0, 1.0, 1.0])              # point() reads bedrock x mirrored
    Mb = np.column_stack([d, u, np.cross(u, d)]) @ F
    Mh = Mb @ _HAND_TO_BOW[0].T
    Mf = K.world(pose, "left_forearm")[:3, :3]
    return K.decompose_zyx(Mf.T @ Mh)


def nock_x(s):
    i = max(0, min(2, int(s)))
    return NOCK_X[i] + (NOCK_X[i + 1] - NOCK_X[i]) * (s - i)


def solve_drawn(key, legs, elev):
    """The full-draw key's torso, head and bow arm, plus the aim's yaw, putting the drawn nock on the anchor."""
    x0 = np.array([*key["torso"], key["head"][0], key["head"][1], *key["left_arm"], key["left_forearm"][0], 0.0])
    w = np.array([0.02, 0.03, 0.02, 0.03, 0.03, 0.03, 0.03, 0.03, 0.06, 0.15])

    def pose_of(x):
        q = dict(key)
        q["torso"] = tuple(x[0:3])
        q["head"] = (x[3], x[4], key["head"][2])
        q["left_arm"] = tuple(x[5:8])
        q["left_forearm"] = (x[8], 0.0, 0.0)
        return fk_pose({b: (v, None) for b, v in q.items()}, legs), q

    def f(x):
        pose, _ = pose_of(x)
        d, u = aim(elev, x[9])
        pose["left_hand"] = (bow_wrist(pose, d, u), Z3)
        N = K.point(K.world(pose, BOW), (NOCK_X[3], NOCK_Y, 0.0))
        A = K.point(K.world(pose, "head"), ANCHOR)
        return np.concatenate([4.0 * (N - A), w * (x - x0)])

    x = gn(f, x0, clamp=lambda x: np.r_[x[:8], min(0.0, x[8]), x[9]])
    err = np.linalg.norm(f(x)[:3]) / 4.0
    return pose_of(x)[1], x[9], err


def string_arm(pose, target, guess):
    """right_arm (x, y, z) and right_forearm x putting the fist on the string, nearest the keyed arm."""
    g = np.array(guess, dtype=float)

    def f(x):
        p = dict(pose)
        p["right_arm"] = (tuple(x[:3]), Z3)
        p["right_forearm"] = ((x[3], 0.0, 0.0), Z3)
        F = K.point(K.world(p, "right_forearm"), FIST)
        return np.concatenate([4.0 * (F - target), 0.02 * (x - g)])

    x = gn(f, g, clamp=lambda x: np.r_[x[:3], min(0.0, max(-150.0, x[3]))])
    return tuple(x[:3]), (x[3], 0.0, 0.0), np.linalg.norm(f(x)[:3]) / 4.0


def ramp(t, a, b):
    if t <= a:
        return 0.0
    if t >= b:
        return 1.0
    k = (t - a) / (b - a)
    return k * k * (3 - 2 * k)


# The draw amount: 0 the bow at rest; 1 the arrow nocked on a resting string; 3 at the anchor. The string hand brings the arrow
# to the string, sets, then draws in one long pull that slows into the anchor; the loose snaps it back to rest.
DRAW_KEYS = [(7.8, 1.0), (9.0, 1.15), (10.6, 2.4), (FULL_AT, 3.0), (IMPACT, 3.0)]


def draw_amount(t):
    if t < DRAW_KEYS[0][0] or t >= IMPACT:
        return 0.0
    return K.curve([(a, (v,)) for a, v in DRAW_KEYS], t)[0]


AIM_IN = (4.6, 8.4)        # the bow turns onto the line as it comes up from the nock to the set
AIM_OUT = (19.0, 23.0)     # and lets go of it as the guard returns
STRING_IN = (5.0, 7.8)     # the string hand travels to the string, meeting it as the bow comes onto the line


def shot_upper(volley):
    name = "volley" if volley else "direct"
    elev = ELEV[name]
    legs = K.attack_legs(shot_legs(volley), ticks=ATTACK_TICKS)
    up = raised if volley else (lambda p: p)
    keyed_full = up(FULL)
    solved, yaw, err = solve_drawn(keyed_full, legs[FULL_AT * SUB], elev)
    if err > 0.3:
        raise ValueError("%s: anchor unreachable by %.2f px" % (name, err))
    delta = {b: tuple(a - c for a, c in zip(solved[b], keyed_full[b])) for b in ("torso", "head", "left_arm", "left_forearm")}

    def drawn(p):
        q = dict(p)
        for b, dv in delta.items():
            q[b] = tuple(a + c for a, c in zip(p[b], dv))
        return q

    keys = shot_keys(volley, lambda p: drawn(up(p)))
    d, u = aim(elev, yaw)
    out, worst = [], 0.0
    for i in range(ATTACK_TICKS * SUB + 1):
        t = i / SUB
        s = sample(keys, t)
        if FULL_AT <= t <= IMPACT:      # the held draw trembles (rule 2: alive even when still)
            k = math.sin(math.pi * (t - FULL_AT) / (IMPACT - FULL_AT))
            tr = 0.9 * k * math.sin(2 * math.pi * t / 2.3)
            for b, c in (("left_arm", (tr, 0.4 * tr, 0.0)), ("torso", (0.0, 0.5 * tr, 0.3 * tr))):
                s[b] = (tuple(a + e for a, e in zip(s[b][0], c)), None)
        pose = fk_pose(s, legs[i])
        wa = ramp(t, *AIM_IN) * (1.0 - ramp(t, *AIM_OUT))
        if wa > 0:
            want = bow_wrist(pose, d, u)
            s["left_hand"] = (tuple(a + (b - a) * wa for a, b in zip(s["left_hand"][0], want)), None)
            pose["left_hand"] = (s["left_hand"][0], Z3)
        amt = draw_amount(t)
        ws = ramp(t, *STRING_IN) if t < IMPACT else 0.0
        if ws > 0:
            N = K.point(K.world(pose, BOW), (nock_x(max(amt, 1.0)), NOCK_Y, 0.0))
            ra, rf, e = string_arm(pose, N, (*s["right_arm"][0], s["right_forearm"][0][0]))
            if ws >= 1.0:
                worst = max(worst, e)
            s["right_arm"] = (tuple(a + (b - a) * ws for a, b in zip(s["right_arm"][0], ra)), None)
            s["right_forearm"] = (tuple(a + (b - a) * ws for a, b in zip(s["right_forearm"][0], rf)), None)
        s["draw"] = (Z3, (float(amt), 0.0, 0.0))
        out.append(s)
    print("%s: aim yaw %.1f, anchor err %.3f px, string err max %.3f px, delta %s"
          % (name, yaw, err, worst, {b: tuple(round(c, 1) for c in v) for b, v in delta.items()}))
    return out, legs


def attack_clips():
    out = {}
    for name, volley in (("volley", True), ("direct", False)):
        upper, legs = shot_upper(volley)
        upper = gear(upper, legs)
        clip = K.bake(upper, ATTACK_TICKS, loop=False)
        clip["timeline"] = {K.fmt_t(FULL_AT / TPS): "full", K.fmt_t(IMPACT / TPS): "impact"}
        out["attack_" + name] = clip
        out["attack_%s_legs" % name] = K.bake(legs, ATTACK_TICKS, loop=False)
    return out


# ------------------------------------------------------------------------------------------------
# Stances: the bow at rest in the guard; StanceController plays ready (fighting), brace / wall (Hold), ward (fall_back).
# ------------------------------------------------------------------------------------------------

STANCE_UPPER = {
    "ready": READY,
    # brace: lower and wider over the braced legs, the bow held further out, ready to come up
    "brace": P((10, 16, 0), (-8, -24, 0), (-44, -16, -8), -12, -10, (-38, -10, 6), (-34, 40, 0)),
    # wall: the formed line, crouched over the deep set, bow forward and the string hand already at the quiver's mouth
    "wall": P((16, 12, 0), (-14, -24, 0), (-50, -14, -8), -10, -12, (-120, 6, -10), (-50, 0, 0), quiver=(0, 0, -4)),
    # ward: giving ground, leaning back, the bow held across the body toward the foe
    "ward": P((-6, 14, 0), (2, -18, 0), (-46 + CHAR["ward_arm"], -24, -10), -18, -8, (-22, -6, 8), (-40, 30, 0)),
}
STANCE_UPPER["shield_up"] = STANCE_UPPER["ready"]
LIFE = {"ready": 3.4, "brace": 1.6, "wall": 1.4, "ward": 2.0, "shield_up": 3.4}
READY_LIKE = ("ready", "shield_up")     # these loops run at V2_READY_SWAY_SCALE (0.25); the other stances at V2_SWAY_SCALE


def stance_upper(pose, life, ready=False):
    n = K.STANCE_TICKS * SUB
    out = []
    a_br, a_wt, a_lag = K.feel.stance_amps(life, ready)    # ready: everything at V2_READY_SWAY_SCALE; others: the rock at V2_SWAY_SCALE
    for i in range(n + 1):
        u = (i % n) / n
        br = a_br * math.sin(2 * math.pi * u)
        wt = a_wt * math.sin(2 * math.pi * u - K.WEIGHT)
        wl = a_wt * math.sin(2 * math.pi * u - K.WEIGHT - 0.7)
        f = {b: (tuple(v), None) for b, v in pose.items()}
        tx, ty, tz = pose["torso"]
        f["torso"] = ((tx + 0.8 * br + 1.8 * wt, ty - 1.4 * wt, tz - 1.2 * wt), None)
        hx, hy, hz = pose["head"]
        f["head"] = ((hx - 0.5 * br - 1.3 * wt, hy + 0.9 * wt, hz + 1.0 * wt), None)
        ax, ay, az = pose["right_arm"]
        f["right_arm"] = ((ax - 0.6 * br - 1.3 * wl, ay, az + 0.6 * wl), None)
        sx, sy, sz = pose["left_arm"]   # the bow arm steadier than the string hand: it keeps the bow on line
        f["left_arm"] = ((sx - 0.5 * br - 0.8 * wl, sy, sz), None)
        f["left_hand"] = ((pose["left_hand"][0] + 0.8 * math.sin(2 * math.pi * u - 0.6) * a_lag, 0.0, 0.0), None)
        f["quiver"] = ((0.3 * br, 0.0, pose["quiver"][2]), None)
        out.append(f)
    return K.bake(gear(out), K.STANCE_TICKS)


STANCE_LEGS = dict(K.STANCE_LEGS)
STANCE_LEGS_LIFE = dict(K.STANCE_LIFE)
if V2:
    # V2: the three commands read apart from the ready footing. brace sinks a step lower and wider (the knight's v2 brace);
    # the wall stays the deepest. The legs of the brace and the wall carry the same weight cycle as their upper bodies, so the
    # hips move with the chest instead of holding still under it.
    STANCE_LEGS["brace"] = {"hips": ((2.0, 10.0, 0.0), (0.0, -2.2, 0.5)), "right": (-3.8, 2.0, 2.8), "left": (3.6, 2.0, -2.8),
                            "yaw": (-16.0, 9.0)}
    STANCE_LEGS_LIFE.update({"brace": LIFE["brace"], "wall": LIFE["wall"]})


def stance_clips():
    out = {"stance_%s_upper" % k: stance_upper(v, LIFE[k], k in READY_LIKE) for k, v in STANCE_UPPER.items()}
    out.update({"stance_%s_legs" % k: K.stance_legs(v, STANCE_LEGS_LIFE.get(k, 1.0), k in READY_LIKE) for k, v in STANCE_LEGS.items()})
    return out


# ------------------------------------------------------------------------------------------------
# Hits: additive offsets, no shield to take the blow, so thrown hard with both arms flung (the bow rides the fist).
# ------------------------------------------------------------------------------------------------

off = K.off
HIT_PEAK = {
    "front": off(torso=(K.hb_front(-17), 10, 0), head=(K.hb(-12), -3, 0), rarm=(-20, 0, 42), larm=(-10, 0, -18), lfore=-12),
    "back": off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 20), larm=(10, 0, -14)),
    "right": off(torso=(K.hb(-5), 14, 8), head=(0, 6, 3), rarm=(10, 0, 28), larm=(-6, 0, -10)),
    "left": off(torso=(K.hb(-5), -14, -8), head=(0, -6, -3), rarm=(-6, 0, 16), larm=(10, 0, -16)),
}


def hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for d, peak in HIT_PEAK.items():
            keys = K.hit_keys(peak, weight)
            out["hit_%s_%s" % (weight, d)] = K.bake([K.sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)], ticks, loop=False)
    # no shield to block with; if the server ever reports a block, it reads as a light flinch from the front
    out["hit_block"] = out["hit_light_front"]
    out.update(K.hit_legs_clips())
    return out


# V2 hits (the knight's v2 grading, rebuilt for the bow): a light blow is a flinch at the knees; a medium blow shifts the pelvis
# well off the footing and takes a short lifted recovery step; heavy (HitController: medium x1.55) is the stagger. The body keys
# are the knight's v2 timing. Both arms ride their own tracks that open outward and never fold back across the body: the bow
# arm (the weapon arm, left) is knocked out and down, away from the face, and is back on line in about 5 ticks after a blow
# from the front; the string arm is flung out with the body and gathers without overshooting inward.
V2_LIGHT = 0.4
# Review round 1: the unshielded front throw read as a tumble at heavy (x1.55), so the medium front body throw is trimmed;
# the other directions keep the knight's v2 amplitude.
V2_BODY = {"front": 0.85, "back": 1.0, "right": 1.0, "left": 1.0}
BOW_ARM = {"front": (6, -12, -26), "back": (8, -4, -6), "right": (-4, -8, -4), "left": (8, -4, -6)}
# A blow from the right throws the body over the bow side: the longbow's tall stave then dips its lower tip under the ground,
# and the hide hunter's bow, carried low, swings into its own left leg. Those two open the bow arm wider (it lifts and stands
# the lower limb clear of the leg).
BOW_ARM_RIGHT = {"longbow": (-4, -8, -11), "wm_hunter": (-4, -8, -11)}
if TROOP in BOW_ARM_RIGHT:
    BOW_ARM["right"] = BOW_ARM_RIGHT[TROOP]
ARM_BONES = {"left": ("left_arm", "left_forearm", "left_hand"), "right": ("right_arm", "right_forearm", "right_hand")}


def v2_hit_keys(peak, weight, k=1.0):
    sc = K.scaled
    peak = sc(peak, k)
    if weight == "light":
        p = sc(peak, V2_LIGHT)
        return [(0, K.ZERO), (1.5, p), (4, sc(p, -0.12)), (6.5, sc(p, 0.04)), (9, K.ZERO)]
    return [(0, K.ZERO), (2, sc(peak, 2.3)), (4.5, sc(peak, 2.15)), (7.5, sc(peak, 0.55)), (10.5, sc(peak, -0.35)),
            (13, sc(peak, 0.08)), (16, K.ZERO)]


def bow_arm_keys(d, weight):
    a = dict(K.ZERO, left_arm=BOW_ARM[d])
    sc = K.scaled
    if d == "front":
        if weight == "light":
            return [(0, K.ZERO), (1.5, sc(a, 0.6)), (3.5, sc(a, 0.2)), (5.5, K.ZERO), (9, K.ZERO)]
        # medium: the bow is held out and down through the whole throw (the chest is pitched back until ~t8, and a bow brought
        # back on line sooner stands a hand from the face), then comes home with the gather
        return [(0, K.ZERO), (2, a), (4.5, sc(a, 0.9)), (7.5, sc(a, 0.4)), (10.5, K.ZERO), (16, K.ZERO)]
    if weight == "light":
        return [(0, K.ZERO), (1.5, sc(a, V2_LIGHT)), (4, sc(a, 0.05)), (6.5, K.ZERO), (9, K.ZERO)]
    if d == "right" and TROOP == "longbow":   # the stave tip stays off the ground while the hips are still low (to ~t11)
        return [(0, K.ZERO), (2, sc(a, 2.3)), (4.5, sc(a, 2.3)), (7.5, sc(a, 1.8)), (10.5, sc(a, 0.9)), (13, sc(a, 0.2)),
                (16, K.ZERO)]
    return [(0, K.ZERO), (2, sc(a, 2.3)), (4.5, sc(a, 2.15)), (7.5, sc(a, 0.55)), (10.5, K.ZERO), (16, K.ZERO)]


def string_arm_keys(peak, weight):
    sc = K.scaled
    a = {b: (peak[b] if b in ARM_BONES["right"] else Z3) for b in peak}
    if weight == "light":
        return [(0, K.ZERO), (1.5, sc(a, V2_LIGHT)), (4, sc(a, 0.05)), (6.5, K.ZERO), (9, K.ZERO)]
    return [(0, K.ZERO), (2, sc(a, 2.3)), (4.5, sc(a, 2.15)), (7.5, sc(a, 0.55)), (10.5, K.ZERO), (16, K.ZERO)]


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


def v2_hit_clips():
    out = {}
    for weight, ticks in K.HIT_TICKS.items():
        for d, peak in HIT_PEAK.items():
            keys, bk, sk = v2_hit_keys(peak, weight, V2_BODY[d]), bow_arm_keys(d, weight), string_arm_keys(K.scaled(peak, V2_BODY[d]), weight)
            keys, bk, sk = (K.front_onset(x, d, weight) for x in (keys, bk, sk))   # a 2-tick onset on the front medium blow (no one-tick snap)
            samples = []
            for i in range(ticks * SUB + 1):
                s, b, r = K.sample_keys(keys, i / SUB), K.sample_keys(bk, i / SUB), K.sample_keys(sk, i / SUB)
                s.update({x: b[x] for x in ARM_BONES["left"]})
                s.update({x: r[x] for x in ARM_BONES["right"]})
                samples.append(s)
            out["hit_%s_%s" % (weight, d)] = K.bake(samples, ticks, loop=False)
    # no shield to block with; if the server ever reports a block, it reads as a light flinch from the front, legs included
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
# Deaths: the knight's four falls, with the bow let go flat. From the landing on, every tick the bow hand turns the bow to lie flat
# on the ground (its limbs level, string side up or down), at whichever heading keeps it clear of the body and nearest to how it
# already lay, so it never stands through the ground or spears the corpse. FLAT_AT: (tick the wrist starts to give, tick it lies flat).
# ------------------------------------------------------------------------------------------------

# The bow arm flings out past the head as the troop rolls onto it, turned palm-up so the bow can lie flat beyond the hood
# (the knight's sweep lays the arm over the hood, where no bow heading clears both the body and the ground).
ROLL_LEFT = K._sweep("left", {14: ((-120, 0, -30), -40, (90, 0, 0)), 15: ((-150, 0, 0), -30, (90, 0, 0)),
                             **{t: CHAR["roll_left"] for t in range(16, 21)}})
FLAT_AT = {"death_back": (8, 12), "death_forward": (13, 16), "death_left": (9, 13), "death_right": (9, 13)}
_ARMED = ("left_arm", "left_forearm", "left_hand", "bow", "weapon")


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
    """Animation degrees part way (k) from r0 to r1 along the shortest turn."""
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


def flat_hand(pose, ref):
    """The bow hand rotation that lets the bow drop: it lies along the ground, flat or leaning from the fist with a limb tip on
    the ground, clear of the body, and as near `ref` as that allows."""
    q = dict(pose)
    q["left_hand"] = (ref, Z3)
    P, owner = _CORNERS(q)
    isbow = np.array(["bow" in o for o in owner])
    ground = P[~isbow, 1].min()
    boxes = _boxes(P, owner)
    L = np.linalg.inv(K.world(q, "left_hand")) @ np.c_[P[isbow] / 16.0, np.ones(int(isbow.sum()))].T
    best = None
    for phi in range(0, 360, 10):
        for tilt in range(0, 75, 5):
            c, s, ct, st = math.cos(math.radians(phi)), math.sin(math.radians(phi)), math.cos(math.radians(tilt)), math.sin(math.radians(tilt))
            u = np.array([c * ct, -st, s * ct])
            for sgn in (1.0, -1.0):
                d = sgn * np.array([-s, 0.0, c])
                h = bow_wrist(pose, d, u)
                q["left_hand"] = (h, Z3)
                W = (K.world(q, "left_hand") @ L)[:3].T * 16.0
                low = W[:, 1].min() - ground
                cost = (_inside(W, boxes) + 3.0 * max(0.0, -low) + 0.6 * abs(low - 0.2) + turn_deg(h, ref) / 45.0 + tilt / 60.0)
                if best is None or cost < best[0]:
                    best = (cost, h)
    return best[1]


def flat_bow(keys, start, t0, t1):
    end = keys[-1][0]
    overs, ref = {}, None
    for t in range(t0, end + 1):
        f = K.pose_at(keys, start, t)
        pose = {b: (r, Z3) for b, r in f.items()}
        ref = flat_hand(pose, ref or f["left_hand"])
        overs[t] = {"left_hand": slerp_rot(f["left_hand"], ref, ramp(t, t0 - 1, t1))}
    t_key = max(k[0] for k in keys if k[0] <= t0 - 1)
    return K.roll_keys(keys, start, t_key, overs)


def death_clips():
    start = K.idle_start()
    back = K.DEATH_BACK
    if TROOP == "stalker":   # the wide mantle swallows a bow laid by the hip: the bow arm flings out wide as it lands
        back = K.roll_keys(back, start, 6, {t: {"left_arm": (-30, 0, -90), "left_forearm": (0, 0, 0)} for t in range(7, 21)})
    falls = {"death_back": back, "death_forward": K.DEATH_FORWARD,
             "death_left": K.roll_keys(K.DEATH_LEFT, start, 13, ROLL_LEFT),
             "death_right": K.roll_keys(K.DEATH_RIGHT, start, 13, K.ROLL_RIGHT)}
    return {n: K.death_clip(flat_bow(k, start, *FLAT_AT[n]), start, n) for n, k in falls.items()}


# ------------------------------------------------------------------------------------------------

def only_rig(anims):
    """Renames the gear channels onto this troop's bones (BOW_TROOPS gear) and drops channels for bones its rig lacks (the knight
    builders key a cape)."""
    gear_map = BT["gear"]
    for clip in anims.values():
        bones = {}
        for b, ch in clip["bones"].items():
            b = gear_map.get(b, b)
            if b is not None and b in GEO_BONES:
                bones[b] = ch
        clip["bones"] = bones
    return anims


def main():
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
