"""The barrow stalker's clips, his own from the first frame to the last (stalker_core.py is the solver).

    python art/anim/stalker/build_stalker_anims.py                  ->  art/anim/stalker/out/stalker.animation.json  (all clips)
    python art/anim/stalker/build_stalker_anims.py --sheet <name> <ticks a:b:step> <png>      a review sheet of one animation
    python art/anim/common/export.py stalker                         ->  the mod's assets

Who he is: a patient barrow hunter. He never stands square: the bow side leads, the weight sits low between a pointing lead foot and
a turned-out back foot, and an arrow is always on the string, both hands joined at the bow. He has no quiver (his arrows ride the
belt behind the right hip), so every shot ends with the string hand dipping to the belt and bringing the next arrow to the string.
The head leads everything: it finds the target first and stays on it while the body coils and uncoils underneath.

Gameplay timing is unchanged from the shipped stalker: both shots 26 ticks, "full" at 12, "impact" at 16; every clip keeps its
length and its name.
"""
import json, math, sys

import stalker_core as C
from stalker_core import K, B, np, SUB, TPS, Z3, run, cloth, upper_of, legs_of, delta, lerp, add, ramp

TAU = 2 * math.pi
FULL = {}     # name -> the composed full-body samples of each animation, for review sheets


def sin(t, period, phase=0.0):
    return math.sin(TAU * (t / period - phase))


# ------------------------------------------------------------------------------------------------
# The poses everything is built from
# ------------------------------------------------------------------------------------------------

LH = (-35.0, 0.0, -8.0, -45.0)      # the bow arm's elbow: low and a little out
RH = (-20.0, 0.0, 12.0, -75.0)      # the string arm's: folded, the elbow back
FREE = (-6.0, 0.0, 10.0, -30.0)     # the string arm hanging free

# Fighting ready: coiled over the lead leg, the bow canted across the body with the arrow down the line of the lead foot.
READY = dict(hips_r=(2, 10, 0), hips_p=(0.1, -2.0, 0.3), torso=(9, 34, 0), head=(-6, -40, 0),
             grip=(7.0, 18.0, -4.6), lhint=LH, bow=(-18, 24, -55), rh=(-3, 16, -5), rhint=RH, string=1.0, draw=1.0, rhand=Z3)
# Standing watch: taller and looser, the bow carried flat and low across the hips in the bow hand alone, no arrow on the string
# yet; the string hand hangs free by the arrows at the belt. (Joined hands that low would put the string arm through his chest.)
WATCH = dict(hips_r=(0, 8, 0), hips_p=(0.0, -1.2, 0.0), torso=(3, 22, 0), head=(-1, -28, 0),
             grip=(6.0, 17.0, -4.5), lhint=LH, bow=(-8, 8, -78), rh=(-6.0, 14.6, -2.0), rhint=FREE, string=0.0, draw=0.0, rhand=Z3)


def G(base, **over):
    g = dict(base)
    g.update(over)
    return g


def breathe(amp=1.0, period=40.0):
    """The chest fills and empties, the bow rides it, the head stays level."""
    def mod(t, g):
        b = amp * sin(t, period)
        g["torso"] = add(g["torso"], (0.9 * b, 0.0, 0.0))
        g["head"] = add(g["head"], (-0.8 * b, 0.0, 0.0))
        g["hips_p"] = add(g["hips_p"], (0.0, 0.12 * b, 0.0))
        g["grip"] = add(g["grip"], (0.0, 0.25 * amp * sin(t, period, 0.12), 0.0))
    return mod


def both(*mods):
    def mod(t, g):
        for m in mods:
            m(t, g)
    return mod


def emit(out, name, samples, ticks, loop, legs=True, cloth_kw=None, upper_name=None, legs_name=None):
    cloth(samples, loop=loop is True, **(cloth_kw or {}))
    FULL[name] = samples
    out[upper_name or name] = K.bake(upper_of(samples), ticks, loop=loop)
    if legs:
        out[legs_name or name + "_legs"] = K.bake(legs_of(samples), ticks, loop=loop)


# ------------------------------------------------------------------------------------------------
# The shot. 0-5 the bow comes up onto the line as the body turns side-on; 5-12 one long draw that slows into the anchor ("full");
# 12-16 the held aim (AttackController stretches it for the seeded hold); 16 the loose ("impact"): the string hand flies back
# along the line, the bow tips forward out of the open hand's push; 18-25 the hand drops to the belt, takes an arrow and brings
# it to the string as the body settles back over the lead leg.
# ------------------------------------------------------------------------------------------------

ATTACK_TICKS, FULL_AT, IMPACT = 26, 12, 16
ANCHOR = (-7.4, 24.6, 1.5)         # head-local: beside the fur cowl at the jaw, drawn to the ear, where the string hand can be seen
ELEV = {"volley": 35.0, "direct": 4.0}


def anchor_solve(g, elev, draw=3.0, anchor=ANCHOR, feet=C.FEET):
    """The bow arm and the aim's yaw that put the drawn nock on the anchor; returns the goals with grip, lhint and bow set."""
    g = dict(g)
    sv = C.Solver()
    full = C.fill([(0, g)])[0][1]
    legs = sv.legs(full, C.feet_at((), 0, feet))
    pose = {b: (v[0], v[1] or Z3) for b, v in legs.items()}
    x0 = np.array([-80.0, -30.0, 0.0, -12.0, g["bow"][1], 0.0, 0.0])
    cant = g["bow"][2]

    def nock(x):
        p = dict(pose)
        p["torso"] = (add(g["torso"], (x[6], x[5], 0.0)), Z3)        # the chest may turn and lean a little to reach
        p["head"] = (add(g["head"], (-x[6], -x[5], 0.0)), Z3)        # under a head that stays on the target
        p["left_arm"], p["left_forearm"] = ((x[0], x[1], x[2]), Z3), ((x[3], 0.0, 0.0), Z3)
        d, u = C.bow_frame(elev, x[4], cant)
        p["left_hand"] = (B.bow_wrist(p, d, u), Z3)
        A = K.point(K.world(p, "head"), anchor)
        return K.point(K.world(p, B.BOW), (B.nock_x(draw), C.NOCK_Y, 0.0)), A, p

    def f(x):
        N, A, _ = nock(x)
        return np.concatenate([4.0 * (N - A), 0.02 * (x - x0) * np.array([1, 1, 1, 1, 40, 0.5, 8])])

    x = B.gn(f, x0, clamp=lambda x: np.r_[x[:3], min(-4.0, x[3]), x[4:]])
    N, A, p = nock(x)
    err = float(np.linalg.norm(N - A))
    g["torso"], g["head"] = p["torso"][0], p["head"][0]
    g["grip"] = C.to_chest(p, K.point(K.world(p, "left_forearm"), C.WRIST_L))
    g["lhint"] = tuple(x[:4])
    g["bow"] = (elev, float(x[4]), cant)
    g["rh"] = C.to_chest(p, N)
    return g, err


def shot(volley):
    name = "volley" if volley else "direct"
    elev = ELEV[name]
    lean = -15.0 if volley else 1.0
    drawn, err = anchor_solve(G(READY, hips_r=(-2 if volley else 1, 16, 0), hips_p=(-0.5, -2.7 if volley else -2.4, -0.9 if volley else -0.3),
                                torso=(lean, 78, 0), head=(-lean * 0.55 - (10 if volley else 0), -94, 0), bow=(elev, 0, -6),
                                rhint=(-70.0, -30.0, 20.0, -130.0)), elev)
    if err > 0.35:
        raise ValueError("%s: the anchor is out of reach by %.2f px" % (name, err))
    e, yaw, cant = drawn["bow"]
    # the set: the bow already on the line, the string still at rest, the body half way round
    setp = G(drawn, hips_r=lerp(READY["hips_r"], drawn["hips_r"], 0.6), hips_p=lerp(READY["hips_p"], drawn["hips_p"], 0.5),
             torso=lerp(READY["torso"], drawn["torso"], 0.72), head=lerp(READY["head"], drawn["head"], 0.72),
             grip=add(drawn["grip"], (0.0, -0.6, 0.6)), bow=(e - 5, yaw, cant - 10), rhint=(-50.0, -20.0, 14.0, -100.0))
    # the loose: the bow hand pushes through and the limbs tip forward; the string hand is gone back past the ear
    loose = G(drawn, torso=add(drawn["torso"], (1.5, 5, 0)), hips_p=add(drawn["hips_p"], (0.0, 0.5, 0.5)),
              grip=add(drawn["grip"], (0.3, -0.2, -0.5)), bow=(e - 9, yaw - 1, cant - 16),
              rh=add(drawn["rh"], (-3.2, 0.9, 4.2)), rhint=(-95.0, -10.0, 34.0, -95.0), rhand=(0, 0, -25))
    follow = G(loose, torso=add(drawn["torso"], (0.5, 7, 0)), bow=(e - 16, yaw - 2, cant - 26),
               rh=add(drawn["rh"], (-4.6, 0.4, 6.0)), rhand=(0, 0, -35))
    # the belt: the string hand drops behind the right hip for the next arrow while the bow sinks toward the guard
    belt = G(READY, hips_r=lerp(READY["hips_r"], drawn["hips_r"], 0.35), hips_p=add(READY["hips_p"], (0.2, 0.3, 0.0)),
             torso=(12, 20, 0), head=(-8, -27, 0), grip=add(READY["grip"], (0.4, 1.2, -0.4)), bow=(-6, 20, -34),
             rh=(-5.6, 14.2, 1.6), rhint=(14.0, 0.0, 10.0, -42.0), rhand=(0, 0, 0))
    keys = [(0, READY),
            (1.6, G(READY, hips_p=add(READY["hips_p"], (0.1, -0.5, 0.2)), torso=add(READY["torso"], (3, 6, 0)),
                    grip=add(READY["grip"], (0, -0.5, 0.2)))),                       # a breath down before he rises
            (5.2, setp), (FULL_AT, drawn), (IMPACT, drawn), (IMPACT + 0.75, loose), (18.6, follow), (22.0, belt),
            (ATTACK_TICKS, READY)]
    steps = ([("right", 2.0, 5.0, (-3.9, 2.0, -4.3), -48.0, 1.0), ("right", 21.0, 24.5, C.FEET["right"][0], C.FEET["right"][1], 0.9)]
             if volley else
             [("left", 2.0, 5.0, (2.6, 2.0, 4.8), 4.0, 1.0), ("left", 20.5, 24.0, C.FEET["left"][0], C.FEET["left"][1], 0.9)])

    def mod(t, g):
        # the draw: an arrow on the string, one pull that slows into the anchor, gone at the loose, the next arrow at 23
        if t < IMPACT:
            amt = K.curve([(5.2, (1.0,)), (6.4, (1.2,)), (9.6, (2.5,)), (FULL_AT, (3.0,)), (IMPACT, (3.0,))], t)[0] if t > 5.2 else 1.0
            g["string"] = (1.0,)
        else:
            amt = 0.0 if t < 23.5 else 1.0
            g["string"] = (ramp(t, 22.2, 24.6),)
        g["draw"] = (amt,)
        if FULL_AT <= t <= IMPACT:      # the held draw is alive: the bow arm fights the string
            k = math.sin(math.pi * (t - FULL_AT) / (IMPACT - FULL_AT))
            tr = k * math.sin(TAU * t / 2.3)
            g["grip"] = add(g["grip"], (0.0, 0.16 * tr, 0.0))
            g["torso"] = add(g["torso"], (0.3 * tr, 0.5 * tr, 0.0))
            g["hips_p"] = add(g["hips_p"], (0.0, -0.25 * k, 0.0))
        if IMPACT < t < IMPACT + 5:      # the loose rocks him: a short kick through the hips that rings out
            a = t - IMPACT
            kick = math.exp(-a / 1.6) * math.sin(TAU * a / 4.2)
            g["hips_p"] = add(g["hips_p"], (0.0, 0.2 * kick, -0.7 * kick))
            g["head"] = add(g["head"], (-2.5 * kick, 0.0, 0.0))

    samples = run(keys, ATTACK_TICKS, steps=steps, mod=mod, report="attack_" + name)
    return samples, yaw, err


def attack_clips(out):
    for name in ("volley", "direct"):
        samples, yaw, err = shot(name == "volley")
        emit(out, "attack_" + name, samples, ATTACK_TICKS, False)
        out["attack_" + name]["timeline"] = {K.fmt_t(FULL_AT / TPS): "full", K.fmt_t(IMPACT / TPS): "impact"}
        print("  attack_%s: aim yaw %.1f, anchor miss %.3f px" % (name, yaw, err))


# ------------------------------------------------------------------------------------------------
# Idle: standing watch. The weight drifts from one leg to the other, the head quarters the ground ahead in two slow looks with a
# pause between, the bow rides the breath.
# ------------------------------------------------------------------------------------------------

IDLE_TICKS = 60


def idle_mod(t, g):
    breathe(1.0, 30.0)(t, g)
    w = sin(t, 60.0)                                     # the weight: over the back leg, then the lead
    g["hips_p"] = add(g["hips_p"], (-0.45 * w, -0.2 * abs(w), -0.35 * w))
    g["hips_r"] = add(g["hips_r"], (0.0, 1.2 * w, 0.9 * w))
    g["torso"] = add(g["torso"], (0.0, -1.5 * w, -1.1 * w))
    look = K.curve([(0, (0.0, 0.0)), (8, (0.0, 0.0)), (15, (14.0, 2.0)), (27, (15.0, 2.5)), (36, (-12.0, -1.0)), (47, (-13.0, -1.5)),
                    (56, (0.0, 0.0)), (60, (0.0, 0.0))], t)
    g["head"] = add(g["head"], (look[1], look[0], 0.0))
    g["torso"] = add(g["torso"], (0.0, 0.22 * look[0], 0.0))
    g["bow"] = add(g["bow"], (0.8 * sin(t, 30.0, 0.2), 0.12 * look[0], 1.5 * w))


def idle_clips(out):
    samples = run([(0, WATCH), (IDLE_TICKS, WATCH)], IDLE_TICKS, mod=idle_mod, loop=True, report="idle")
    emit(out, "idle", samples, IDLE_TICKS, True, upper_name="idle_upper", legs_name="idle_legs")


# ------------------------------------------------------------------------------------------------
# World-frame chest and face. A side-on archer cannot lean back from his target with one torso angle (the torso's own x axis
# points across the line once he has turned), so the chest and the head are given as WORLD yaw / lean / roll and turned into bone
# rotations under whatever the hips are doing.
# ------------------------------------------------------------------------------------------------

def _under(parents, yaw, lean, roll):
    R = np.eye(3)
    for r in parents:
        R = R @ B._rot(r)
    want = B._rot((0, 0, roll)) @ B._rot((lean, 0, 0)) @ B._rot((0, yaw, 0))
    return tuple(float(v) for v in K.decompose_zyx(R.T @ want))


def chest(hips_r, yaw, lean=0.0, roll=0.0):
    """Torso rotation for a chest turned `yaw` to its right, leaned `lean` toward the target and rolled `roll`."""
    return _under([hips_r], yaw, lean, roll)


def face(hips_r, torso, yaw=0.0, pitch=0.0, roll=0.0):
    """Head rotation for a face turned `yaw` off the target, looking `pitch` down."""
    return _under([hips_r, torso], yaw, pitch, roll)


def body(base, yaw, lean=0.0, roll=0.0, look=(0.0, 0.0, 0.0), **over):
    g = G(base, **over)
    g["torso"] = chest(g["hips_r"], yaw, lean, roll)
    g["head"] = face(g["hips_r"], g["torso"], *look)
    return g


def hold(out, name, goal, ticks, mod, legs, feet=C.FEET):
    """A stance loop: one pose kept alive by `mod`."""
    samples = run(lambda t: goal, ticks, mod=mod, feet=feet, loop=True, report=name)
    emit(out, name, samples, ticks, True, legs=legs, upper_name=name + "_upper", legs_name=name + "_legs")
    return samples


def life(period, breath=1.0, look=0.0, sway=0.0, tremble=0.0):
    """What keeps a held pose alive: breath, a slow look from side to side, a drift of the weight, a tremble in the bow arm."""
    def mod(t, g):
        breathe(breath, period / 2.0)(t, g)
        if look:
            y = look * sin(t, period)
            g["head"] = add(g["head"], (0.0, y, 0.0))
            g["torso"] = add(g["torso"], (0.0, 0.2 * y, 0.0))
        if sway:
            w = sway * sin(t, period, 0.15)
            g["hips_p"] = add(g["hips_p"], (-0.4 * w, 0.0, -0.2 * w))
            g["hips_r"] = add(g["hips_r"], (0.0, 0.0, 0.8 * w))
            g["torso"] = add(g["torso"], (0.0, 0.0, -0.9 * w))
        if tremble:
            tr = tremble * math.sin(TAU * t / 2.5) * (0.6 + 0.4 * sin(t, period))
            g["grip"] = add(g["grip"], (0.0, 0.14 * tr, 0.0))
            g["torso"] = add(g["torso"], (0.0, 0.4 * tr, 0.0))
    return mod


# ------------------------------------------------------------------------------------------------
# Stances. Each has its own bow, so they read apart from across a field:
#   ready      coiled over the lead leg, bow canted across the body, arrow down the line of the lead foot
#   brace      the deepest crouch, half drawn already: an ambush about to spring
#   wall       squared up and tight, the bow upright in front of the chest
#   ward       leaning away behind a bow thrust out level like a bar
#   shield_up  ducked low under the bow arm, the bow held level across the brow, the string hand up guarding the face
#   hold       tall and planted, the bow stood upright at the lead foot like a staff, the head sweeping
#   rest       at ease: the bow trailed at the thigh in a loose hand, the string hand hooked in the belt
#   rally      the bow punched up over the head
#   fall_back  backing away, chest still to the foe, the arrow half raised
#   aim        on the line at half draw, level, high and low
# ------------------------------------------------------------------------------------------------

STANCE_TICKS = 40
FEET_WALL = {"right": ((-2.7, 2.0, -1.7), -18.0), "left": ((2.4, 2.0, 1.9), 6.0)}
FEET_BRACE = {"right": ((-3.7, 2.0, -4.2), -50.0), "left": ((2.6, 2.0, 4.0), 6.0)}
FEET_HOLD = {"right": ((-3.0, 2.0, -2.3), -30.0), "left": ((2.3, 2.0, 2.5), 4.0)}
PRE_ANCHOR = (ANCHOR[0], ANCHOR[1], ANCHOR[2] - 2.7)     # half draw: the string hand a hand's width short of the ear

BRACE = body(READY, 52, 15, look=(0, 2), hips_r=(3, 12, 0), hips_p=(0.1, -3.6, 0.2), grip=(6.6, 18.6, -5.2),
             bow=(2, 6, -40), draw=2.0, rhint=(-45.0, -10.0, 16.0, -100.0))
WALL = body(READY, 20, 4, look=(0, 5), hips_r=(0, 4, 0), hips_p=(0.0, -1.5, 0.0), grip=(3.4, 20.6, -7.0), bow=(0, 0, 2),
            lhint=(-50.0, 20.0, -4.0, -60.0))
WARD = body(WATCH, 34, -9, look=(4, 9), grip=(4.6, 21.2, -6.0), bow=(4, 2, -86), lhint=(-70.0, 10.0, -10.0, -30.0),
            rh=(-3, 15, -5), rhint=RH, string=1.0, draw=1.0)
SHIELD_UP = body(WATCH, 40, 27, look=(0, 16), grip=(4.0, 26.6, -6.6), bow=(4, 10, -88), lhint=(-150.0, 0.0, -20.0, -60.0),
                 rh=(-2.6, 20.6, -5.6), rhint=(-45.0, 0.0, -8.0, -125.0), string=0.0, draw=0.0)
HOLD = body(WATCH, 26, 1, look=(0, 0), hips_r=(0, 6, 0), hips_p=(0.0, -0.8, 0.0), grip=(8.4, 18.0, -4.4), bow=(2, 0, 4),
            lhint=(-25.0, 0.0, -12.0, -50.0), rh=(-5.8, 14.4, -1.6), rhint=(-8.0, 0.0, 14.0, -50.0))
REST = body(WATCH, 14, -3, look=(6, 3), grip=(7.2, 15.4, -1.4), bow=(-66, 16, -8), lhint=(-4.0, 0.0, -10.0, -24.0),
            rh=(-4.3, 15.2, -2.7), rhint=(-8.0, 0.0, 16.0, -70.0), string=0.0, draw=0.0)
FALLBACK = body(WATCH, 44, -7, look=(0, 3), hips_r=(0, 0, 0), grip=(5.4, 20.2, -5.6), bow=(-4, 0, -30), draw=1.0, string=1.0,
                rh=(-3, 15, -5), rhint=RH,
                lhint=(-60.0, 0.0, -10.0, -40.0))


def aim_pose(elev):
    lean, nod = {0.0: (1.0, 0.0), 45.0: (-15.0, -12.0), -30.0: (12.0, 9.0)}[elev]
    g, err = anchor_solve(G(WATCH, torso=(lean, 78, 0), head=(-lean * 0.55 + nod, -94, 0), bow=(elev, 0, -6), draw=2.0, string=1.0,
                            rhint=(-70.0, -30.0, 20.0, -130.0)), elev, draw=2.0, anchor=PRE_ANCHOR)
    print("  aim %+.0f: anchor miss %.3f px" % (elev, err))
    return g


def stance_clips(out, only=None):
    want = lambda n: only is None or only == n
    if want("stance_ready"):
        hold(out, "stance_ready", READY, STANCE_TICKS, life(40, 0.9, look=3.0, sway=0.35), True)
    if want("stance_brace"):
        hold(out, "stance_brace", BRACE, STANCE_TICKS, life(40, 0.7, tremble=0.6), True, feet=FEET_BRACE)
    if want("stance_wall"):
        hold(out, "stance_wall", WALL, STANCE_TICKS, life(40, 0.7, look=2.0), True, feet=FEET_WALL)
    if want("stance_ward"):
        hold(out, "stance_ward", WARD, STANCE_TICKS, life(40, 1.0, look=4.0, sway=0.3), False)
    if want("stance_shield_up"):
        hold(out, "stance_shield_up", SHIELD_UP, STANCE_TICKS, life(40, 1.0, tremble=0.4), False)
    if want("stance_hold"):
        hold(out, "stance_hold", HOLD, 48, life(48, 0.8, look=17.0, sway=0.25), True, feet=FEET_HOLD)
    if want("stance_rest"):
        def rest_mod(t, g):
            life(48, 1.2, look=11.0, sway=0.6)(t, g)
            g["bow"] = add(g["bow"], (3.0 * sin(t, 48, 0.1), 0.0, 4.0 * sin(t, 24)))      # the bow swings idly in the loose hand
        hold(out, "stance_rest", REST, 48, rest_mod, False)
    for n, elev in (("stance_aim", 0.0), ("stance_aim_up", 45.0), ("stance_aim_down", -30.0)):
        if want(n):
            hold(out, n, aim_pose(elev), STANCE_TICKS, life(40, 0.5, tremble=1.0), False)
    if want("stance_rally"):
        # down into the legs, then the bow punched up over the head; it overshoots, rings and is held aloft
        up = body(WATCH, 20, -3, look=(0, -14), hips_p=(0.0, -1.3, 0.0), grip=(6.4, 30.6, -1.2), bow=(0, 0, 0),
                  lhint=(-175.0, 0.0, -8.0, -6.0), rh=(-5.6, 18.4, -3.6), rhint=(-30.0, 0.0, 10.0, -110.0), string=0.0, draw=0.0)
        dip = body(WATCH, 24, 7, look=(0, 4), hips_p=(0.0, -2.4, 0.2), grip=(6.4, 16.4, -4.6), bow=(-14, 8, -72),
                   string=0.0, draw=0.0)
        keys = [(0, WATCH), (3.0, dip), (10.0, G(up, grip=(6.4, 31.2, -1.0), hips_p=(0.0, -1.0, 0.0))),
                (12.5, G(up, grip=(6.4, 30.0, -1.4), hips_p=(0.0, -1.6, 0.0))), (15.0, up), (20, up)]

        def mod(t, g):
            if t > 10:       # the bow is shaken at the sky, dying away
                k = math.exp(-(t - 10) / 6.0) * math.sin(TAU * (t - 10) / 3.6)
                g["bow"] = add(g["bow"], (0.0, 0.0, 6.0 * k))
                g["rh"] = add(g["rh"], (0.0, 0.5 * k, 0.0))
        emit(out, "stance_rally", run(keys, 20, mod=mod, report="stance_rally"), 20, False, legs=False, upper_name="stance_rally_upper")
    if want("stance_fallback"):
        def back_goal(u, hr):
            s2 = math.sin(2 * TAU * u)
            return body(FALLBACK, 44 - 4.0 * math.cos(TAU * u), -7 + 1.2 * s2, look=(0, 3), hips_r=hr,
                        grip=add(FALLBACK["grip"], (0, 0.3 * s2, 0)))
        gait_run(out, "stance_fallback", BACK, back_goal, reverse=True, wind=-3.0)


# ------------------------------------------------------------------------------------------------
# Gaits. A prowl, not a march: the hips ride low and level, the chest counter-turns over them, the bow is carried flat and
# still while the head never leaves the ground ahead. The run folds lower and tucks the bow along the lead side. The charge is
# the same run with the bow already up and half drawn: he arrives shooting. Falling back is the prowl walked backwards.
# ------------------------------------------------------------------------------------------------

WALK = K.Gait("walk", ticks=8, speed=0.16, duty=0.6, heel=12, toe_off=22, front=4.7, lift=2.3, lift_peak=0.4, hip_drop=1.9,
              bob=0.4, bob_phase=0.06, sway=0.3, yaw=5.0, roll=1.0, pitch_hips=2.0, lane=2.0)
RUN = K.Gait("run", ticks=7, speed=0.26, duty=0.37, heel=6, toe_off=34, front=4.5, lift=3.4, lift_peak=0.45, hip_drop=2.2,
             bob=0.75, bob_phase=0.19, sway=0.2, yaw=7.0, roll=1.4, pitch_hips=5.0, lane=2.0)
BACK = K.Gait("back", ticks=8, speed=0.12, duty=0.62, heel=8, toe_off=10, front=3.4, lift=1.5, lift_peak=0.5, hip_drop=1.4,
              bob=0.3, bob_phase=0.06, sway=0.25, yaw=3.0, roll=0.8, pitch_hips=-2.0, lane=2.2)
CARRY = G(WATCH, grip=(6.0, 17.2, -4.6), bow=(-10, 10, -76))
TUCK = G(WATCH, grip=(6.6, 17.8, -4.4), bow=(-26, 18, -60))


def walk_goal(u, hr):
    c, s2 = math.cos(TAU * u), math.sin(2 * TAU * u)
    return body(CARRY, 24 + 7.0 * c, 8 + 1.2 * s2, look=(0, 2), hips_r=hr,
                grip=add(CARRY["grip"], (0, 0.3 * s2, 0)), bow=add(CARRY["bow"], (1.5 * s2, 0, 2.5 * c)),
                rh=(-6.2, 14.8, -1.2 + 2.0 * c), rhint=(-6.0, 0.0, 10.0, -34.0))


def run_goal(u, hr):
    c, s2 = math.cos(TAU * u), math.sin(2 * TAU * (u - 0.08))
    return body(TUCK, 26 + 6.0 * c, 19 + 1.5 * s2, look=(0, -3), hips_r=hr,
                grip=add(TUCK["grip"], (0, 0.5 * s2, 0.2 * c)), bow=add(TUCK["bow"], (3.0 * s2, 0, 4.0 * c)),
                rh=(-6.4, 16.4, -2.2 + 3.0 * c), rhint=(-10.0, 0.0, 10.0, -85.0))


def charge_goal(u, hr):
    c, s2 = math.cos(TAU * u), math.sin(2 * TAU * (u - 0.08))
    return body(TUCK, 58 + 3.0 * c, 13 + 1.0 * s2, look=(0, 0), hips_r=hr, grip=(2.4, 21.2, -7.0 + 0.2 * s2),
                bow=(1 + 1.0 * s2, 0, -55), draw=2.0, string=1.0, rh=(-3, 16, -5), lhint=(-80.0, -20.0, 0.0, -20.0), rhint=(-60.0, -20.0, 16.0, -120.0))


def gait_run(out, name, g, goal, reverse=False, wind=0.0, flutter=0.0, legs=True):
    n = g.ticks * SUB
    guesses = {}
    for i in range(n):
        g.pose(i / n, guesses)
    sv = C.Solver()
    samples = []
    for lap in (0, 1):
        samples = []
        for i in range(n + 1):
            u = ((n - i) % n if reverse else i % n) / n
            lg = g.pose(u, guesses)
            f = dict(lg)
            f.update(sv.upper(C.fill([(0, goal(u, lg["hips"][0]))])[0][1], lg))
            samples.append(f)
        if lap == 0:
            sv.err = {"left": 0.0, "right": 0.0}
    samples[-1] = samples[0]
    print("  %-22s reach miss: bow hand %.2f px, string hand %.2f px" % (name, sv.err["left"], sv.err["right"]))
    emit(out, name, samples, g.ticks, True, legs=legs, cloth_kw=dict(wind=wind, flutter=flutter, period=g.ticks),
         upper_name=name + "_upper", legs_name=name + "_legs")


def gait_clips(out, only=None):
    if only in (None, "walk"):
        gait_run(out, "walk", WALK, walk_goal, wind=5.0, flutter=2.0)
    if only in (None, "run"):
        gait_run(out, "run", RUN, run_goal, wind=13.0, flutter=4.0)
    if only in (None, "charge_run"):
        gait_run(out, "charge_run", RUN, charge_goal, wind=13.0, flutter=4.0, legs=False)


# ------------------------------------------------------------------------------------------------
# Hits: ADDITIVE over whatever he is doing (HitController), so each is authored as a whole-body move out of and back into the
# ready pose and stored as its difference from it. A blow shoves him along its own line: the chest goes first, the head is left
# behind and then whips past, the hips give under it, and a stagger catches itself with a step. The bow and the string hand go
# with the chest, still joined: the arrow never leaves the string for a flinch.
#   push (x, z): where the blow drives him in the world: from the front (0, -1), from behind (0, 1), from his right (1, 0).
# ------------------------------------------------------------------------------------------------

READY_YAW, READY_LEAN = 44.0, 9.0
PUSH = {"front": (0.0, -1.0), "back": (0.0, 1.0), "right": (1.0, 0.0), "left": (-1.0, 0.0)}
TWIST = {"front": -0.8, "back": 0.5, "right": 0.5, "left": -0.5}          # how the blow turns the shoulders, per degree of lean
BOW_KICK = {"front": (18, 4, 22), "back": (-24, -4, -10), "right": (4, 13, -8), "left": (3, -8, 11)}
STEP_FOOT = {"front": "right", "back": "left", "right": "left", "left": "right"}
ROLL_SIGN = -1.0      # a positive torso z tips him to his right


def struck(d, amt, head=0.0, drop=0.0, kick=1.0, **over):
    """The ready pose shoved `amt` degrees along a blow's line; `head` is how far the head has followed (0 left behind, 1 with the
    chest, more whipped past)."""
    px, pz = PUSH[d]
    k = kick * amt / 20.0
    kw = dict(hips_r=add(READY["hips_r"], (0.25 * amt * pz, 0.3 * TWIST[d] * amt, 0.25 * amt * px * ROLL_SIGN)),
              hips_p=add(READY["hips_p"], (0.075 * amt * px, -drop, -0.075 * amt * pz)),      # hips position: +z is BACK
              bow=add(READY["bow"], tuple(k * v for v in BOW_KICK[d])),
              grip=add(READY["grip"], (0.0, 0.05 * abs(amt), 0.04 * abs(amt))))
    kw.update(over)
    return body(READY, READY_YAW + TWIST[d] * amt, READY_LEAN + amt * pz, amt * px * ROLL_SIGN,
                look=(-0.3 * TWIST[d] * amt * (head - 1), 4 + 0.9 * amt * pz * (head - 0.35), 0.9 * amt * px * ROLL_SIGN * (head - 0.35)),
                **kw)


def additive(out, name, samples, ticks, ref):
    cloth(samples)
    FULL[name] = samples
    out[name] = K.bake(delta(samples, ref, C.UPPER + C.CLOTH), ticks, loop=False)
    legs = delta(samples, ref, C.LEGS)
    for f in legs:
        for b in ("right_shin", "left_shin"):
            f[b] = ((max(-9.0, f[b][0][0]),) + tuple(f[b][0][1:]), f[b][1])
    out[name + "_legs"] = K.bake(legs, ticks, loop=False)


def stepped(d, t0, t1, back0, back1, reach, lift=1.2):
    side = STEP_FOOT[d]
    p, yaw = C.FEET[side]
    to = (p[0] + reach * PUSH[d][0], p[1], p[2] + reach * PUSH[d][1])
    return [(side, t0, t1, to, yaw, lift), (side, back0, back1, p, yaw, 0.8)]


READY_B = body(READY, READY_YAW, READY_LEAN, look=(0, 4))       # the ready pose the flinches leave from and come back to


def hit_ref():
    return cloth([dict(C.static(READY_B)) for _ in range(40)])[-1]


def hit_clips(out, only=None):
    ref = hit_ref()
    R0 = READY_B
    for d in PUSH:
        if only in (None, "hit_light_" + d):
            keys = [(0, R0), (1.5, struck(d, 11, head=0.1, drop=0.5)), (3.5, struck(d, 5, head=1.5, drop=0.7)),
                    (6.0, struck(d, -1.5, head=1.0, drop=0.1)), (9, R0)]
            additive(out, "hit_light_" + d, run(keys, 9, report="hit_light_" + d), 9, ref)
        if only in (None, "hit_medium_" + d):
            keys = [(0, R0), (2.4, struck(d, 20, head=0.0, drop=0.6)), (4.8, struck(d, 16, head=1.6, drop=1.3)),
                    (8.0, struck(d, 6, head=1.1, drop=1.0)), (12.0, struck(d, -2.5, head=1.0, drop=0.2)), (16, R0)]
            additive(out, "hit_medium_" + d, run(keys, 16, steps=stepped(d, 1.5, 4.5, 9.0, 13.0, 2.6), report="hit_medium_" + d), 16, ref)
    if only in (None, "hit_block"):
        # the bow snapped up across the blow like a bar, the body hunched behind it; it gives, then shoves back
        bar = dict(bow=(-4, 16, -76), lhint=(-60.0, 5.0, -9.0, -42.0))
        gr = lambda *o: add(READY["grip"], o)
        hp = lambda *o: add(READY["hips_p"], o)
        keys = [(0, R0), (2.2, body(READY, 40, 4, look=(0, 12), hips_p=hp(0, -0.6, 0), grip=gr(-0.8, 2.2, -0.9), **bar)),
                (4.0, body(READY, 38, -5, look=(0, 14), hips_p=hp(0, -1.0, 0.9), grip=gr(-0.8, 1.9, -0.2), **bar)),
                (6.2, body(READY, 45, 12, look=(0, 6), hips_p=hp(0, -0.5, -0.5), grip=gr(-0.7, 2.0, -1.4), **bar)), (9, R0)]
        additive(out, "hit_block", run(keys, 9, report="hit_block"), 9, ref)
    if only in (None, "hit_knockback"):
        # thrown: the chest goes over backwards, the string hand is torn off the string and both arms fly; two stumbling steps
        # catch him low, and he gathers the bow back in as he comes up over the lead leg again
        air = struck("front", 32, head=0.0, drop=0.3, kick=1.1, grip=(7.8, 20.4, -3.8), rh=(-7.4, 18.6, -3.2),
                     rhint=(-40.0, 0.0, 40.0, -60.0), lhint=(-60.0, 0.0, -30.0, -35.0), hips_p=add(READY["hips_p"], (0, -0.3, 3.3)))
        low = struck("front", 20, head=1.7, drop=2.2, kick=0.8, grip=(7.6, 18.8, -4.0), rh=(-7.2, 16.0, -3.2),
                     rhint=(-30.0, 0.0, 36.0, -60.0), hips_p=add(READY["hips_p"], (0, -2.2, 3.0)))
        up = struck("front", -6, head=1.0, drop=0.9, kick=0.5)
        keys = [(0, R0), (3.0, air), (7.5, low), (11.0, G(low, hips_p=add(low["hips_p"], (0, 0.5, -0.3)))), (15.0, up), (20, R0)]
        fr, fl = C.FEET["right"], C.FEET["left"]
        steps = [("right", 1.0, 5.0, add(fr[0], (-0.3, 0, -3.0)), fr[1], 1.3), ("left", 5.0, 9.0, add(fl[0], (0.2, 0, -2.8)), fl[1], 1.1),
                 ("left", 10.5, 14.5, fl[0], fl[1], 0.9), ("right", 14.5, 18.5, fr[0], fr[1], 0.9)]

        def mod(t, g):
            g["string"] = (1.0 - ramp(t, 0.6, 2.6) + ramp(t, 11.0, 15.0),)
        additive(out, "hit_knockback", run(keys, 20, steps=steps, mod=mod, report="hit_knockback"), 20, ref)
    if only in (None, "hit_pulled"):
        # yanked: the chest is dragged out ahead of the feet, the head left behind looking at the sky, the bow arm hauled
        # forward; the lead foot lunges to catch him, the back foot drags in after it
        yank = struck("back", 28, head=0.25, drop=0.6, kick=1.2, grip=(6.0, 19.0, -6.2))
        catch = struck("back", 17, head=1.5, drop=1.8, kick=0.8)
        keys = [(0, R0), (2.4, yank), (5.8, catch), (9.5, struck("back", -4, head=1.0, drop=0.4)), (14, R0)]
        fr, fl = C.FEET["right"], C.FEET["left"]
        steps = [("left", 2.0, 5.0, add(fl[0], (0, 0, 3.0)), fl[1], 1.2), ("right", 4.5, 7.5, add(fr[0], (0, 0, 2.2)), fr[1], 0.3),
                 ("left", 8.0, 11.0, fl[0], fl[1], 0.9), ("right", 10.5, 13.5, fr[0], fr[1], 0.9)]
        additive(out, "hit_pulled", run(keys, 14, steps=steps, report="hit_pulled"), 14, ref)


# ------------------------------------------------------------------------------------------------
# Stun: 0-4 the jolt (the chest and head thrown up out of the slump and dropped back into it), then 4-24 the daze, a loop
# (StanceController plays the first 4 once and loops the rest, so the pose at 24 is the pose at 4, and at 0 too): he hangs over his knees with the bow trailing from a slack hand, no arrow, the head rolling in a
# slow circle, the whole body swaying after it.
# The shake-off (stun_out_upper, additive, rotations only): the head shaken clear, the shoulders shrugged up and dropped.
# ------------------------------------------------------------------------------------------------

def daze(ph, jolt=0.0):
    a = TAU * ph
    return body(READY, 30 + 4 * math.sin(a), 19 + 4 * math.sin(2 * a) - 17 * jolt, 6 * math.sin(a + 0.6),
                look=(8 + 15 * math.cos(a), 24 + 9 * math.sin(a) - 16 * jolt, 13 * math.sin(a + 0.4)),
                hips_r=(4, 8 + 2 * math.sin(a), 2.5 * math.sin(a)), hips_p=(0.5 * math.sin(a), -2.7 - 0.3 * math.cos(2 * a), 0.3 * math.cos(a)),
                grip=(6.8 + 0.8 * jolt, 15.4 + 0.3 * math.sin(2 * a) + 1.6 * jolt, -2.4), bow=(-58 + 5 * math.sin(a), 30, -30 + 8 * math.sin(a + 1)),
                lhint=(-6.0, 0.0, -12.0, -16.0), rh=(-6.6, 13.6, -1.4 + 0.8 * math.sin(a + 2)), rhint=(-4.0, 0.0, 10.0, -14.0),
                string=0.0, draw=0.0)


def stun_clips(out, only=None):
    samples = run(lambda t: daze((t - 4.0) / 20.0) if t >= 4 else daze(0.0, math.sin(math.pi * t / 4.0) ** 2 * (1.0 - t / 8.0)), 24, report="stun")
    cloth(samples)
    samples[24 * SUB] = samples[4 * SUB]
    FULL["stun"] = samples
    out["stun_upper"] = K.bake(upper_of(samples), 24, loop=True)
    out["stun_legs"] = K.bake(legs_of(samples), 24, loop=True)
    shake = []
    for i in range(6 * SUB + 1):
        e = i / (6.0 * SUB)
        env = math.sin(math.pi * e)
        hs = 17.0 * math.sin(TAU * 1.5 * e) * (1 - e) * min(1.0, 6 * e)
        shr = math.sin(math.pi * min(1.0, e / 0.6)) ** 2
        shake.append({"head": ((-7.0 * env, hs, 0.0), None), "torso": ((-6.0 * env, -0.25 * hs, 0.0), None),
                      "right_arm": ((0.0, 0.0, 7.0 * shr), None), "left_arm": ((0.0, 0.0, -7.0 * shr), None),
                      "right_forearm": ((-9.0 * shr, 0.0, 0.0), None), "left_forearm": ((-9.0 * shr, 0.0, 0.0), None),
                      "pelt_tail": ((4.0 * env, 0.0, -0.4 * hs), None)})
    out["stun_out_upper"] = K.bake(shake, 6, loop=False)
    base = hit_ref()        # shown over the ready pose he comes back to
    FULL["stun_out"] = [{b: (add(base[b][0], s[b][0]) if b in s else base[b][0], base[b][1]) for b in base} for s in shake]


# ------------------------------------------------------------------------------------------------
# Deaths (DeathController picks by the killing blow's bearing; 20 ticks, the last frame held). The falls are keyed as rotations
# and grounded by the knight builder's fall solver (the root is solved so the lowest point of the body stays on the ground and
# he rolls about whatever touches it); the bow is let go and laid flat by the bow family's flat-bow search.
#   death_back     shot through the chest from the front: the bow arm is thrown up, he totters two beats on his heels and goes
#                  over flat on his back, one knee up and sliding down, the head rolling aside
#   death_forward  struck from behind: both knees go, he kneels a beat with the head hanging, looks up once and pitches on his face
#   death_left / death_right   struck from the side: the near knee folds and he spins down over it onto his side and rolls
# ------------------------------------------------------------------------------------------------

DEATH_BACK = [
    (3, "out", {"root": (-4, 0, 0), "torso": (-13, 16, 0), "head": (18, -20, 0), "left_arm": (-70, 20, -30), "left_forearm": (-30, 0, 0),
                "right_arm": (-35, 0, 40), "right_forearm": (-60, 0, 0), "right_leg": (4, 0, 4), "right_shin": (14, 0, 0)}),
    (6, "io", {"root": (-15, 0, 0), "torso": (-25, 6, 0), "head": (-24, -10, 0), "left_arm": (-125, 0, -40), "left_forearm": (-10, 0, 0),
               "right_arm": (-105, 0, 35), "right_forearm": (-25, 0, 0), "right_leg": (14, 0, 5), "right_shin": (24, 0, 0),
               "left_leg": (-32, 0, -3), "left_shin": (44, 0, 0)}),
    (12, "in", {"root": (-88, 0, 0), "hips": (0, 0, 0), "torso": (-4, 0, 0), "head": (8, 0, 0), "right_arm": (-25, 0, 84),
                "right_forearm": (-20, 0, 0), "left_arm": (-35, 0, -88), "left_forearm": (-5, 0, 0), "right_leg": (-22, 0, 6),
                "right_shin": (30, 0, 0), "left_leg": (-58, 0, -8), "left_shin": (88, 0, 0), "right_foot": (20, 0, 0),
                "left_foot": (20, 0, 0)}),
    (14, "out", {"root": (-80, 0, 0), "head": (22, -8, 0), "left_shin": (78, 0, 0), "right_arm": (-32, 0, 78)}),
    (17, "io", {"root": (-84, 0, 0), "head": (36, -26, 0), "right_arm": (12, 0, 80), "right_forearm": (-25, 0, 0),
                "left_arm": (-22, 0, -88), "left_leg": (-36, 0, -10), "left_shin": (58, 0, 0)}),
    (20, "io", {"root": (-83, 0, 0), "head": (40, -32, 0), "left_leg": (-22, 0, -12), "left_shin": (38, 0, 0)}),
]
DEATH_FORWARD = [
    (3, "out", {"root": (9, 0, 0), "torso": (15, 10, 0), "head": (-24, 0, 0), "right_arm": (28, 0, 24), "left_arm": (-20, 20, -20),
                "right_leg": (-16, 0, 4), "right_shin": (60, 0, 0), "left_leg": (-34, 0, -4), "left_shin": (62, 0, 0)}),
    (7, "in", {"root": (0, 0, 0), "hips": (0, 0, 0), "torso": (12, 2, 0), "head": (28, 0, 0), "right_arm": (-5, 0, 14),
               "right_forearm": (-20, 0, 0), "left_arm": (-8, 20, -22), "left_forearm": (-20, 0, 0), "right_leg": (4, 0, 4),
               "left_leg": (-6, 0, -4), "right_shin": (95, 0, 0), "left_shin": (92, 0, 0), "right_foot": (30, 0, 0), "left_foot": (30, 0, 0)}),
    (10, "io", {"root": (-8, 0, 0), "torso": (4, 0, 0), "head": (-16, 0, 0)}),
    (13, "in", {"root": (30, 0, 0), "torso": (14, 0, 0), "head": (20, 15, 0), "left_arm": (-40, 10, -50), "right_arm": (-20, 0, 30),
                "right_shin": (72, 0, 0), "left_shin": (72, 0, 0)}),
    (16, "in", {"root": (85, -14, 0), "torso": (6, 0, 0), "head": (-18, 30, 0), "right_arm": (0, 0, -25), "right_forearm": (-40, 0, 0),
                "right_hand": (0, -40, -80), "left_arm": (25, 0, -85), "left_forearm": (-40, 0, 0), "left_hand": (0, 0, 0),
                "right_leg": (0, 0, 4), "left_leg": (0, 0, -4), "right_shin": (10, 0, 0), "left_shin": (12, 0, 0)}),
    (18, "out", {"root": (77, -15, 0), "head": (-26, 30, 0), "right_shin": (20, 0, 0), "left_shin": (24, 0, 0)}),
    (20, "io", {"root": (81, -16, 0), "torso": (8, 0, 0), "head": (-20, 34, 0), "left_arm": (30, 0, -90), "right_shin": (12, 0, 0),
                "left_shin": (16, 0, 0)}),
]


FLAT = {"death_back": (4, 12), "death_forward": (8, 16), "death_left": (5, 13), "death_right": (5, 13)}   # the bow let go, slowly


MAX_HAND_TURN = 32.0     # degrees a tick


def rest_hand(pose, ref):
    """The bow family's flat-hand search with the stalker's taste: his long bow must end lying down, so what it pays for is
    height off the ground, not distance from the grip it had."""
    q = dict(pose)
    q["left_hand"] = (ref, Z3)
    P, owner = B._CORNERS(q)
    isbow = np.array(["bow" in o for o in owner])
    ground = P[~isbow, 1].min()
    boxes = B._boxes(P, owner)
    L = np.linalg.inv(K.world(q, "left_hand")) @ np.c_[P[isbow] / 16.0, np.ones(int(isbow.sum()))].T
    best = None
    for phi in range(0, 360, 10):
        for tilt in range(-30, 75, 5):
            c, s, ct, st = (math.cos(math.radians(phi)), math.sin(math.radians(phi)), math.cos(math.radians(tilt)),
                            math.sin(math.radians(tilt)))
            u = np.array([c * ct, -st, s * ct])
            for sgn in (1.0, -1.0):
                d = sgn * np.array([-s, 0.0, c])
                h = B.bow_wrist(pose, d, u)
                q["left_hand"] = (h, Z3)
                W = (K.world(q, "left_hand") @ L)[:3].T * 16.0
                low, high = W[:, 1].min() - ground, W[:, 1].max() - ground
                cost = B._inside(W, boxes) + 3.0 * max(0.0, -low) + 0.5 * max(0.0, high - 4.0) + B.turn_deg(h, ref) / 90.0
                if best is None or cost < best[0]:
                    best = (cost, h)
    return best[1]


def lay_bow(keys, start, t0, t1):
    """The bow let go of and laid flat (the bow family's flat-hand search), found backwards from where it comes to rest so the
    hand turns one way into that rest instead of hunting between the search's answers on the way down."""
    end = keys[-1][0]
    flats, ref = {}, None
    for t in range(end, t0 - 1, -1):
        f = K.pose_at(keys, start, t)
        ref = rest_hand({b: (r, Z3) for b, r in f.items()}, ref or f["left_hand"])
        flats[t] = ref
    overs, prev = {}, K.pose_at(keys, start, t0 - 1)["left_hand"]
    for t in range(t0, end + 1):
        want = B.slerp_rot(K.pose_at(keys, start, t)["left_hand"], flats[t], B.ramp(t, t0 - 1, t1))
        turn = B.turn_deg(prev, want)
        prev = B.slerp_rot(prev, want, min(1.0, MAX_HAND_TURN / max(turn, 1e-6)))     # the wrist never snaps to a new answer
        overs[t] = {"left_hand": prev}
    return K.roll_keys(keys, start, max(k[0] for k in keys if k[0] <= t0 - 1), overs)


def _near(e, prev):
    """The Euler form of rotation e nearest prev (whole turns, and the mirrored form of the same rotation)."""
    best = None
    for c in (tuple(e), (e[0] + 180.0, 180.0 - e[1], e[2] + 180.0)):
        c = tuple(v + 360.0 * round((p - v) / 360.0) for v, p in zip(c, prev))
        d = sum((v - p) ** 2 for v, p in zip(c, prev))
        if best is None or d < best[0]:
            best = (d, c)
    return best[1]


def smooth_hand(clip):
    """The bow hand's tick keys can be whole turns apart in Euler form, and the game blends Euler angles: key every sample along
    the short turn and keep each key's Euler form next to the one before, so the hand never spins between ticks."""
    ch = clip["bones"]["left_hand"]["rotation"]
    ticks = sorted((round(float(k) * TPS), k) for k in ch)
    keys = {}
    for (ta, ka), (tb, kb) in zip(ticks, ticks[1:]):
        n = (tb - ta) * SUB
        for i in range(n + 1):
            keys[ta * SUB + i] = B.slerp_rot(ch[ka], ch[kb], i / n)
    prev, out = None, {}
    for i in sorted(keys):
        e = tuple(float(v) for v in keys[i])
        prev = e if prev is None else _near(e, prev)
        out[K.fmt_t(i / (TPS * SUB))] = [round(v, 3) for v in prev]
    clip["bones"]["left_hand"]["rotation"] = out


def death_clips(out, only=None):
    if "idle" not in FULL:
        idle_clips({})
    first = FULL["idle"][0]
    start = {b: (tuple(float(v) for v in first[b][0]), tuple(first[b][1]) if first[b][1] is not None else None)
             for b in K.DEATH_BONES if b in first}
    start = {b: (tuple(float(v) for v in K.decompose_zyx(B._rot(r))), pos) for b, (r, pos) in start.items()}   # one Euler form
    start["root"] = (Z3, None)
    # the side falls open his own way (the head and chest knocked across, the bow arm thrown wide) and then roll on the family's
    # grounded roll, which is already solved to keep the body, not the bow, on the ground
    left = [(3, "out", dict(K.DEATH_LEFT[0][2], torso=(4, 20, 14), head=(6, -24, 18), left_arm=(-60, 0, -50)))] + K.DEATH_LEFT[1:]
    right = [(3, "out", dict(K.DEATH_RIGHT[0][2], torso=(4, 30, -14), head=(6, -20, -18), right_arm=(-50, 0, 50)))] + K.DEATH_RIGHT[1:]
    falls = {"death_back": DEATH_BACK, "death_forward": DEATH_FORWARD,
             "death_left": K.roll_keys(left, start, 13, B.ROLL_LEFT), "death_right": K.roll_keys(right, start, 13, K.ROLL_RIGHT)}
    for n, keys in falls.items():
        if only not in (None, n):
            continue
        clip = B.only_rig({n: K.death_clip(lay_bow(keys, start, *FLAT[n]), start, n)})[n]
        smooth_hand(clip)
        out[n] = clip
        FULL[n] = clip


# ------------------------------------------------------------------------------------------------

GROUPS = {"idle": idle_clips, "attack": attack_clips, "stance": stance_clips, "walk": gait_clips, "run": gait_clips,
          "charge": gait_clips, "hit": hit_clips, "stun": stun_clips, "death": death_clips}


def build(only=None):
    out = {}
    done = set()
    for prefix, fn in GROUPS.items():
        if fn in done or (only is not None and not only.startswith(prefix)):
            continue
        done.add(fn)
        if fn in (idle_clips, attack_clips):
            fn(out)
        else:
            fn(out, only)
    return out


def main():
    only = sys.argv[2] if len(sys.argv) > 1 and sys.argv[1] == "--sheet" else None
    anims = build(only)
    if len(sys.argv) > 1 and sys.argv[1] == "--sheet":
        if ":" in sys.argv[3]:
            a, b, st = (float(v) for v in sys.argv[3].split(":"))
            ticks = [a + i * st for i in range(int(round((b - a) / st)) + 1)]
        else:
            ticks = [float(v) for v in sys.argv[3].split(",")]
        kw = dict(scale=int(sys.argv[6])) if len(sys.argv) > 6 else {}
        if len(sys.argv) > 5:
            kw["views"] = tuple(sys.argv[5].split(","))
        C.sheet(FULL[only], ticks, sys.argv[4], label=only, **kw)
        return
    if len(sys.argv) > 2 and sys.argv[1] == "--sheets":      # the whole set built once, one review sheet an animation
        for name, smp in FULL.items():
            n = int(round(smp["animation_length"] * TPS)) if isinstance(smp, dict) else (len(smp) - 1) // SUB
            k = 7 if n > 9 else 5
            C.sheet(smp, [round(n * i / (k - 1), 2) for i in range(k)], "%s/%s.png" % (sys.argv[2], name), label=name,
                    views=("front", "left34"), scale=int(sys.argv[3]) if len(sys.argv) > 3 else 95)
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, len(anims), "clips")


if __name__ == "__main__":
    main()
