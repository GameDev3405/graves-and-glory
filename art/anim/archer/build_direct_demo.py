"""The archer's direct shot, re-authored for the troop-animation skill's second demo (a bow, not a blade).

python art/anim/archer/build_direct_demo.py [--out file]
    merges attack_direct and attack_direct_legs into a COPY of the archer's animation file (default
    work/archer_direct/archer.animation.json, made from the exported file when missing). It never writes the exported
    file by itself: the archer is the animation lead's troop and this clip is a proposal until Kort grades it.

It stands on art/anim/archer/build_bow_anims.py (imported, not edited): the bow line, the anchor solve and the string
arm are that builder's. What changes, and why (Kort's 2026-11-11 grades of the knight demo, and focus_check on the
archer's current shot):

  footwork      the current shot shuffles one foot 1.6 px and the pelvis travels 0.9 px (whole_body FAIL). Here the
                string-side foot drops back as the hand goes to the quiver, the bow-side foot turns onto the line, and
                the pelvis turns side-on between them. "i like that the legs flow into the attack".
                Kort, round 1: "legs look abnormal". Round 1 sank the pelvis 3 px over feet 4 px apart, so both knees
                sat near 90 degrees through the hold, and both feet were in the air together on the way home. Now the
                archer stands TALL (pelvis 1.2 px under the guard's, knees 47-52 degrees, the guard's are 43-46) on a
                stance 7.2 px long, and one foot is planted before the other lifts, out and back.
  the bow       Kort, round 1: "the bows string is inside the archers arm to start" (ticks 0, 1 and 21). The lead's
                guard wrist lays the bow ALONG the forearm, so the resting string (3.8 px behind the grip) runs through
                a 4 px forearm. A hand holds a bow ACROSS the forearm, and the same way in the guard as at the aim:
                GUARD_WRIST is the aim's hold on every guard-side key, so the arm alone carries the bow up and down
                and the string never sweeps the forearm (at most 0.5 px in it on any resting-string tick; round 1's
                guard was 1.4). On the way up and down the arrow line is kept STRING_CLEAR degrees off the forearm.
                The stance clips still use the lead's wrist: see the note for the animation lead.
  set and draw  the rig's arms reach 9 px and the bow draws 11.5: the keyed set pose left the nock out of reach and the
                fist inside the chest. The nock and set poses are solved (fit_bow_arm) so the grip sits low in front
                of the chest with the arrow on, and the draw goes UP to the ear from there.
  full draw     solve_full holds the head to 16 degrees off the foe and the chest to 84 degrees side-on, so the head
                never turns more than 70 degrees on the chest (head_turn).
  the loose     the current clip switches the string arm from solved to keyed in one sample (a 124 degree pop). Here
                the fist's TARGET eases and the arm is solved on every sample: it flies back along the arrow's line
                behind the ear, then eases to the guard.
  anchor        the fist sat inside the hood (armour_clip FAIL). The anchor is outside the hood, beside the ear.
  the whole unit  hood cape, tippet, quiver, skirts and belt pouch swing on fullbody.soft_motion (the current clip only
                scales the knight's cape lag onto two of them). "i want the whole unit to be animated".

Gameplay is untouched: 26 ticks, "full" marker on tick 12 (AttackController holds there), "impact" on tick 16.
Kort, round 1: "shot seems quick". The 16 ticks to the loose are sheets/design.json's (bow_direct impactTicks), and in
game the hold at "full" adds only 0-3 ticks (MotionMath.LOOSE_MAX), so the page shows the shot about as quick as the
game plays it. A slower shot is a rate-of-fire decision, not this clip's: asked, not changed.

What the checks say (focus_check, 2026-11-11, after Kort's round 1 grades): 0 FAIL, 2 WARN (weapon_in_body 1.28 t5
bow into the face plane as it passes in front of the head, limb_in_body 1.65 t7 string forearm into the jerkin);
scorecard pass.

Known model fault, not this clip's: the exported bow geo carries the weapon builder's cube turns with the wrong sign
for a Bedrock geo (GeckoLib BakedModelFactory keeps rotation z and mirrors x), so in game every limb cube tilts the
wrong way and the drawn string opens away from the bow. art/anim/archer/fix_geo_for_render.py makes a corrected copy for renders.

Phases:
  0-1.5     anticipation  eyes on the foe, the bow dips, the weight settles
  0.6-4.6   reach         the string-side foot drops back 7.6 px; the string hand goes over the shoulder to the quiver
  3.0-6.4   bow up        the arm carries the bow up onto the line, held as it was in the guard
  4.8-7.2   nock          the bow-side foot turns onto the line; the arrow comes down onto the string, low
  7.8-12    raise, draw   one long pull up to the ear, standing tall and side-on
  12-16     hold          at the anchor; the back keeps squeezing, the bow arm trembles
  16        loose         the fist flies back behind the ear, the bow rocks forward in the hand, the pelvis rocks back
  16-19     follow        held open, everything loose still swinging
  17.8-26   recover       the bow comes down (18.6-23); the bow-side foot steps home, then the string-side foot
"""
import json
import math
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE))
import build_bow_anims as BW  # noqa: E402  (sets GG_ANIM_TROOP=archer before the knight builder loads)

sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

K, np = BW.K, BW.np
SUB, Z3 = BW.SUB, BW.Z3
T, FULL_AT, IMPACT = BW.ATTACK_TICKS, BW.FULL_AT, BW.IMPACT
ELEV = BW.ELEV["direct"]
BW.ANCHOR = (-6.5, 25.0, 0.0)       # head-local: drawn to the ear, outside the hood (was (-5, 26, -1), inside it)
ELBOW = (-6.0, 18.0, 0.0)           # right_forearm-local: the string arm's elbow
HEAD_YAW = -26.0                    # degrees the face may stay off the foe at full draw (toward the string side); -26 lets
#                                     the neck off its 70 degree limit (fix r1 F8: it sat at 69-70 through the whole hold)

# Fix r1 (F1): the quiver. The exported geo leans it over the BOW shoulder: troop_rig.geo() writes the hand model's bone
# rotation as (-rx, ry, -rz), which GeckoLib reads mirrored in x and z (the hand model and the legacy renderer put the
# fletching over the string shoulder). art/anim/archer/fix_geo_for_render.py makes the corrected copy the renders use; the reach below
# is aimed at that quiver (rotation z -28 in the file, not +28).
QUIVER_PIVOT = (0.0, 18.0, 3.0)     # the quiver bone's pivot (bedrock px, torso frame)
QUIVER_ROLL = -28.0                 # its file rotation z once corrected
FLETCH = (-1.2, 25.6, 5.1)          # quiver-local (bedrock px): the grab, in the 3x4x2 fletching cube (x -1.5..1.5, y 23..27, z 3.5..5.5), its outer back half
QUIVER_UP = (0.0, 1.0, 0.0)         # quiver-local: the arrows come out along the quiver's length
PLUCK_LIFT = 2.4                    # px the arrow is drawn up out of the quiver
VIA_UP = (-9.2, 25.0, 0.8)          # torso-local: the fist passing over the string shoulder on the way to the quiver
VIA_OUT = (-11.0, 24.0, -2.0)        # and coming back out round the shoulder with the arrow

P, mix = BW.P, BW.mix
# The keys are the bow builder's poses turned further side-on: the pelvis now carries about 30 degrees of the turn,
# so the head counters more. torso / head are (pitch, yaw, roll) relative to the pelvis / the torso.
# Kort, archer round 1 (ticks 0, 1 and 21): "the bows string is inside the archers arm to start". The bow builder's guard
# turns the wrist 10 degrees, which lays the bow ALONG the forearm: the string then runs up through the forearm and the
# upper arm (1.4 px deep). A hand holds a bow ACROSS the forearm, and it holds it the SAME way in the guard as at the
# aim: GUARD_WRIST is the aim's hold (wrist about (4, 6, -63) on the raised arm) on the guard's lowered arm, so the bow
# hangs slanted across the body, top limb in front of the chest, lower limb outside the bow-side thigh, and the arm alone
# carries it up and down. 0 px of string or limb in the arm, 0 in the body. A guard held another way (string turned to
# the outside, say) has to turn the bow over in the hand on the way, and the string sweeps through the forearm.
# The stance clips still have the old wrist: see the animation lead's note.
GUARD_WRIST = (-15.3, -2.8, -73.2)


def guard(p):
    q = dict(p)
    q["left_hand"] = GUARD_WRIST
    return q


READY = guard(BW.READY)
ANTIC = guard(P((7, 21, 0), (-5, -25, 0), (-34, -18, -9), -18, -12, (-38, -12, 8), (-34, 40, 0)))
REACH = guard(P((3, -4, 0), (5, -8, 6), (-30, -10, -6), -20, -10, (-165, 0, -15), (-60, 0, 0)))
PLUCK = guard(P((2, -7, 0), (-2, -10, 0), (-32, -11, -6), -21, -10, (-170, 0, -17), (-66, 0, 0)))
NOCK = P((6, 20, 0), (-3, -44, 0), (-58, -26, -4), -12, -6, (-66, -8, 2), (-42, 30, 0))
SET = P((4, 27, 0), (-4, -60, 0), (-88, -4, 0), -55, 0, (-80, -30, 0), (-30, 20, 0))
SET_GRIP = (5.0, 21.2, -7.0)        # torso-local (point() frame): where the bow's grip sits at the set, in front of the
#                                     chest and low, so the nocked string is in the string hand's reach and clear of the hood
NOCK_GRIP = (4.6, 20.8, -6.5)       # the same at the nock, a little lower: the bow is already in front, not out to the side
CHEST_YAW = -82.0                   # degrees side-on the chest should stop at (left to itself the solve goes past 90)
FULL = P((1, 35, 0), (-2, -70, 0), (-90, -40, 0), 0, 0, (-95, -20, 0), (0, 70, 0))
TENSE = P((0.4, 36.5, 0), (-2, -71, 0), (-90.5, -40.5, 0), 0, 0, (-95, -20, 0), (0, 70, 0))
LOOSE = P((0, 38, 0), (-4, -71, 0), (-88, -44, 0), 0, 0, (-100, -5, -10), (0, 95, 0))
FOLLOW = P((-2, 39, 0), (-4, -71, 0), (-84, -46, 0), 0, 4, (-104, 0, -16), (0, 100, 0))
DOWN = guard(P((3, 24, 0), (-4, -44, 0), (-62, -30, -3), -6, -4, (-58, -8, 11), (-16, 50, 0)))

LEGS = {
    # Kort, archer round 1: "legs look abnormal". Round 1 stood side-on on feet 3.8 px apart with the pelvis sunk 3.1 px:
    # both knees bent 90 degrees through the draw and the hold (the guard bends them 45), a squat on a tightrope. An
    # archer stands TALL on a wide base: feet a shoulder width apart along the line of the shot (8.5 px here), the pelvis
    # no lower than the guard carries it, knees 47 to 52 degrees. The pelvis travels back and turns instead of sinking.
    # (tick, (pitch, yaw, roll), (x, y, z)): the position is in the clip's own frame, +x the troop's left, +z BACK.
    "hips": [(0, *K.READY_HIPS),
             (1.5, (1.0, 8.0, 0.0), (0.6, -1.2, 0.2)),        # onto the bow-side foot
             (4.0, (0.0, 20.0, 0.0), (0.4, -1.1, 1.6)),       # back with the right foot as it drops behind
             (5.6, (0.0, 30.0, 0.0), (-0.3, -1.0, 2.6)),      # over the rear foot while the lead foot steps onto the line
             (7.4, (1.0, 40.0, 0.0), (0.1, -1.0, 2.2)),       # side-on already
             (10.0, (1.5, 45.0, 0.0), (0.2, -1.2, 2.0)),
             (FULL_AT, (2.0, 47.0, 0.0), (0.2, -1.25, 1.9)),  # between the feet, tall
             (IMPACT, (2.0, 48.0, 0.0), (0.2, -1.3, 1.8)),
             (17.3, (-0.5, 50.0, 0.0), (0.1, -1.0, 2.5)),     # the loose rocks it back and up
             (19.0, (0.0, 47.0, 0.0), (0.1, -1.0, 2.3)),
             (22.0, (0.5, 24.0, 0.0), (0.0, -1.1, 0.8)),
             (T, *K.READY_HIPS)],
    # (foot, lift tick, plant tick, (x, y, z) of the ankle, foot yaw, lift height)
    # The string-side foot drops back 7.6 px as the hand goes to the quiver; the bow-side foot then steps 3.6 px towards
    # the foe onto the line of the shot. One foot at a time, on the way back too (round 1 had both in the air at 20-21.4).
    "steps": [("right", 0.6, 4.6, (-2.2, 2.0, -5.6), -44.0, 1.0),
              ("left", 4.8, 7.2, (2.6, 2.0, 1.6), -18.0, 0.8),
              ("left", 17.8, 20.6, (3.2, 2.0, -2.0), 7.0, 0.8),
              ("right", 20.8, 24.8, (-3.4, 2.0, 2.0), -12.0, 0.9)],
}

ARROW_ON = 6.9     # fix r1 F2: the arrow is on the string as the fist arrives with it (was 7.8, in a closed fist)
DRAW_KEYS = [(ARROW_ON, 1.0), (7.8, 1.0), (9.0, 1.15), (10.6, 2.4), (FULL_AT, 3.0), (IMPACT, 3.0)]
AIM_IN, AIM_OUT = (3.0, 6.4), (18.6, 23.0)   # the bow comes onto the line as the arm rises to the nock (round 1: by 4.4,
#                                              with the arm still low, which put the resting string through the forearm)
STRING_CLEAR = 48.0                          # degrees the arrow line keeps off the forearm's line while the string is at rest
STRING_OUT = (18.4, 22.6)
# fix r1 F1/F3: the string arm's way to the quiver and back, as arm keys SOLVED onto targets (torso-local px, or the
# fletching, or the nock) on the body's own pose at that tick, then eased between as joint angles: the fist travels in
# arcs, it is exact where it must touch, and nothing chases a moving target through two answers of the shoulder.
# (tick, target, elbow up, elbow forward). Quiver to nock: 4.0 to 6.25, instead of a 0.8 tick whip.
# (tick, target, the arm's starting guess (right_arm x, y, z, right_forearm x)). The guesses hold the one natural answer:
# the upper arm raised in FRONT towards overhead, the elbow flexed, so the flexion carries the hand back behind the head
# (the solver left to itself twists the shoulder round and bends the elbow backward). Quiver to nock: 4.0 to 6.25.
REACH_KEYS = [(2.0, "via_up", (-115.0, 0.0, 0.0, -85.0)), (3.25, "grab", (-160.0, 0.0, 0.0, -110.0)),
              (3.6, "grab", (-160.0, 0.0, 0.0, -110.0)), (4.2, "pluck", (-165.0, 0.0, 0.0, -100.0)),
              (5.2, "via_out", (-125.0, 0.0, 0.0, -95.0)), (ARROW_ON, "nock", None)]
NOCK_FWD = 0.8      # the elbow comes forward at the nock (the fist is a pixel off the chest there; a tucked elbow lays
#                     the forearm along the jerkin)
REACH_FROM = 0.25   # the string hand leaves the guard (the arm keys before it are the keyed guard)

FLY = 9.0          # px the fist flies back along the arrow's line at the loose (fix r1 F3: was 4.4)
FLY_OUT = -1.5      # px it clears the hood sideways on the way
FLY_TICKS = 1.0    # was 1.5: the loose is the fastest event of the clip
DRIFT = 1.6        # px the open hand keeps drifting back through 17-19.5 (fix r1 F4)
KICK = 24.0        # degrees the bow's top limb rocks forward in the hand after the loose (was 13)
RING = 3.0         # ticks per swing of the bow's damped ring in the open hand
TREMBLE = 1.6      # degrees, the held draw's tremble (was 0.9)
BREATH = 1.0       # degrees of torso pitch, one breath across the hold


def keys(drawn, nock, set_):
    return [(0, READY), (1.5, ANTIC), (3.2, mix(ANTIC, REACH, 0.86)), (3.8, REACH), (4.8, PLUCK),
            (6.75, nock), (9.0, set_), (FULL_AT, drawn(FULL)), (15.4, drawn(TENSE)),
            (IMPACT, drawn(LOOSE)), (17.6, drawn(FOLLOW)), (19.2, drawn(mix(FOLLOW, LOOSE, 0.5))),
            (22.4, DOWN), (T, READY)]


def draw_amount(t):
    if t < DRAW_KEYS[0][0] or t >= IMPACT:
        return 0.0
    return K.curve([(a, (v,)) for a, v in DRAW_KEYS], t)[0]


def _yaw_of(pose, bone):
    R = K.world(pose, bone)[:3, :3] @ K.world({}, bone)[:3, :3].T
    return math.degrees(math.atan2(R[0, 2], R[2, 2]))


def solve_full(key, legs, elev):
    """The bow builder's solve_drawn (torso, head, bow arm and aim yaw putting the drawn nock on the anchor) with one
    more wish: the face stays on the foe. Left to itself the solve turns the head 30 degrees away to shorten the draw."""
    x0 = np.array([*key["torso"], key["head"][0], key["head"][1], *key["left_arm"], key["left_forearm"][0], 0.0])
    w = np.array([0.02, 0.02, 0.02, 0.03, 0.02, 0.03, 0.03, 0.03, 0.06, 0.12])

    def pose_of(x):
        q = dict(key)
        q["torso"], q["head"] = tuple(x[0:3]), (x[3], x[4], key["head"][2])
        q["left_arm"], q["left_forearm"] = tuple(x[5:8]), (x[8], 0.0, 0.0)
        return BW.fk_pose({b: (v, None) for b, v in q.items()}, legs), q

    def f(x):
        pose, _ = pose_of(x)
        d, u = BW.aim(elev, x[9])
        pose["left_hand"] = (BW.bow_wrist(pose, d, u), Z3)
        N = K.point(K.world(pose, BW.BOW), (BW.NOCK_X[3], BW.NOCK_Y, 0.0))
        A = K.point(K.world(pose, "head"), BW.ANCHOR)
        return np.concatenate([np.array([4.0, 1.2, 4.0]) * (N - A), w * (x - x0),
                               [0.1 * (_yaw_of(pose, "head") - HEAD_YAW), 0.1 * (_yaw_of(pose, "torso") - CHEST_YAW)]])

    x = BW.gn(f, x0, clamp=lambda x: np.r_[x[:8], min(0.0, x[8]), x[9]])
    pose, q = pose_of(x)
    print("full draw: head %.0f deg off the foe, chest %.0f deg side-on, head to chest %.0f"
          % (_yaw_of(pose, "head"), _yaw_of(pose, "torso"), _yaw_of(pose, "head") - _yaw_of(pose, "torso")))
    return q, x[9], np.linalg.norm(f(x)[:3]) / 4.0


def fit_bow_arm(key, legs, grip, d, u):
    """`key` with its bow arm re-solved so the bow's grip sits on `grip` (torso-local) with the bow already on the line."""
    x0 = np.array([*key["left_arm"], key["left_forearm"][0]])

    def pose_of(x):
        q = dict(key)
        q["left_arm"], q["left_forearm"] = tuple(x[:3]), (x[3], 0.0, 0.0)
        return BW.fk_pose({b: (v, None) for b, v in q.items()}, legs), q

    def f(x):
        pose, _ = pose_of(x)
        pose["left_hand"] = (BW.bow_wrist(pose, d, u), Z3)
        G = K.point(K.world(pose, BW.BOW), (6.0, BW.NOCK_Y, 0.0))
        return np.concatenate([4.0 * (G - K.point(K.world(pose, "torso"), grip)), 0.02 * (x - x0)])

    x = BW.gn(f, x0, clamp=lambda x: np.r_[x[:3], min(0.0, x[3])])
    err = np.linalg.norm(f(x)[:3]) / 4.0
    if err > 0.4:
        raise ValueError("the bow arm cannot reach the grip %s by %.2f px: bring it nearer the bow shoulder" % (grip, err))
    return pose_of(x)[1]


def string_arm(pose, target, guess, warm=None, up=0.35, ew=0.5, gw=0.004, fwd=0.0, damp=0.0):
    """right_arm (x, y, z) and right_forearm x putting the fist on `target`, with the elbow carried out past the string
    shoulder and up (the line of the shoulders carried on), the way a drawing arm sits: never tucked down against the
    chest. `warm` is the last sample's answer, so the elbow does not hop between two solutions. `ew` weighs the elbow wish."""
    g = np.array(guess, dtype=float)
    w0 = g if warm is None else np.array(warm, dtype=float)     # damp: the elbow's swivel stays near the last sample's
    Mt = K.world(pose, "torso")
    back = K.point(Mt, (-5.0, 22.0, 0.0)) - K.point(Mt, (5.0, 22.0, 0.0))
    front = K.point(Mt, (0.0, 22.0, -1.0)) - K.point(Mt, (0.0, 22.0, 0.0))
    back = back / np.linalg.norm(back) + np.array([0.0, up, 0.0]) + fwd * front
    back = back / np.linalg.norm(back)

    def f(x):
        p = dict(pose)
        p["right_arm"] = (tuple(x[:3]), Z3)
        p["right_forearm"] = ((x[3], 0.0, 0.0), Z3)
        M = K.world(p, "right_forearm")
        return np.concatenate([4.0 * (K.point(M, BW.FIST) - target), ew * (K.point(M, ELBOW) - (target + 5.0 * back)),
                               gw * (x - g), damp * (x - w0)])

    x = BW.gn(f, g if warm is None else np.array(warm, dtype=float), clamp=lambda x: np.r_[x[:3], min(0.0, max(-150.0, x[3]))])
    x[:3] = canon(x[:3], (g if warm is None else np.array(warm, dtype=float))[:3])   # the same pose, written nearest the last
    return tuple(x[:3]), (x[3], 0.0, 0.0), np.linalg.norm(f(x)[:3]) / 4.0


def canon(r, ref):
    """The ZYX Euler triple r written the way nearest ref: (x, y, z) and (x + 180, 180 - y, z + 180) are one rotation,
    and each angle is free by 360. Keyed angles blend sanely only between neighbours written alike."""
    best = None
    for c in (np.array(r, dtype=float), np.array([r[0] + 180.0, 180.0 - r[1], r[2] + 180.0])):
        c = np.asarray(ref, dtype=float) + (c - ref + 180.0) % 360.0 - 180.0
        if best is None or np.abs(c - ref).sum() < np.abs(best - ref).sum():
            best = c
    return best


def quiver_local():
    """The quiver's fletching grab and its arrows' direction in the torso's frame (bedrock px), with the corrected roll."""
    b = K.MODEL.bones["quiver"]
    r = np.array(b.rot, dtype=float)
    r[2] = math.radians(QUIVER_ROLL)           # the baked z is the file's z (render_geo: rotation (-rx, -ry, +rz))
    pv = b.pivot / 16.0
    L = K.rg._T(*pv) @ K.rg._Rz(r[2]) @ K.rg._Ry(r[1]) @ K.rg._Rx(r[0]) @ K.rg._T(*(-pv))

    def tl(q):
        c = L @ np.array([-q[0], q[1], q[2], 16.0]) / 16.0
        return np.array([-c[0] * 16.0, c[1] * 16.0, c[2] * 16.0])
    grab = tl(FLETCH)
    up = tl(tuple(a + c for a, c in zip(FLETCH, QUIVER_UP))) - grab
    return grab, up / np.linalg.norm(up)


def torso_pt(pose, p):
    return K.point(K.world(pose, "torso"), tuple(p))


def torso_local(pose, w):
    """The inverse of torso_pt: a world point in the torso's frame (bedrock px)."""
    v = np.linalg.solve(K.world(pose, "torso"), np.array([*(np.asarray(w) / 16.0), 1.0]))
    return np.array([-v[0] * 16.0, v[1] * 16.0, v[2] * 16.0])


def shot():
    legs = K.attack_legs(LEGS, ticks=T)
    solved, yaw, err = solve_full(FULL, legs[FULL_AT * SUB], ELEV)
    if err > 0.3:
        raise ValueError("anchor unreachable by %.2f px" % err)
    delta = {b: tuple(a - c for a, c in zip(solved[b], FULL[b])) for b in ("torso", "head", "left_arm", "left_forearm")}

    def drawn(p):
        q = dict(p)
        for b, dv in delta.items():
            q[b] = tuple(a + c for a, c in zip(p[b], dv))
        return q

    d, u = BW.aim(ELEV, yaw)
    ks = keys(drawn, fit_bow_arm(NOCK, legs[27], NOCK_GRIP, d, u), fit_bow_arm(SET, legs[9 * SUB], SET_GRIP, d, u))
    side = np.cross(u, d)
    grab, qup = quiver_local()

    last_wrist = [None]

    def body_at(i, track=False):
        """The keyed upper body at sample i with the hold's tremble and breath and the bow wrist on the aim. `track`: the
        main pass, sample after sample, so each wrist is written nearest the last one (no Euler flip between samples)."""
        t = i / SUB
        s = BW.sample(ks, t)
        if FULL_AT <= t <= IMPACT:          # the held draw trembles and breathes (fix r1 F4: 0.9 degrees and no breath)
            k = math.sin(math.pi * (t - FULL_AT) / (IMPACT - FULL_AT))
            tr = TREMBLE * k * math.sin(2 * math.pi * t / 2.3)
            br = BREATH * math.sin(2 * math.pi * (t - FULL_AT) / (IMPACT - FULL_AT))
            for b, c in (("left_arm", (tr, 0.4 * tr, 0.0)), ("torso", (br, 0.5 * tr, 0.3 * tr))):
                s[b] = (tuple(a + e for a, e in zip(s[b][0], c)), None)
        pose = BW.fk_pose(s, legs[i])
        wa = BW.ramp(t, *AIM_IN) * (1.0 - BW.ramp(t, *AIM_OUT))
        if wa > 0:
            uu, dd = u, d
            if t > IMPACT:                  # the bow kicks forward in the opened hand and rings out (fix r1 F3)
                k = (t - IMPACT)
                a = math.radians(KICK) * (1.0 - math.exp(-k / 0.1)) * math.exp(-k / 1.0) * math.cos(2 * math.pi * k / RING)
                uu = math.cos(a) * u + math.sin(a) * d
                dd = math.cos(a) * d - math.sin(a) * u
            if wa < 1.0:
                # raising and lowering: the bow's AXES go between the keyed guard's and the aim's (blending the wrist's
                # angles swings it through the arm and across the face: the guard's wrist is rolled 89 degrees)
                Rk = K.world(pose, BW.BOW)[:3, :3]
                dk, uk = -Rk[:, 0] / np.linalg.norm(Rk[:, 0]), Rk[:, 1] / np.linalg.norm(Rk[:, 1])
                dt = dd * wa + dk * (1.0 - wa)
                dt = dt / np.linalg.norm(dt)
                # The resting string sits 3.8 px behind the grip and the forearm is 4 px thick: when the arrow line comes
                # within STRING_CLEAR degrees of the forearm's own line the string cuts the forearm. Turn it off the
                # forearm by what is missing (nothing as the aim arrives; the guard's hold is the aim's, so it is clear).
                fa = K.world(pose, "left_forearm")[:3, :3] @ np.array([0.0, -1.0, 0.0])
                fa = fa / np.linalg.norm(fa)
                need = math.radians(STRING_CLEAR) * min(1.0, 3.0 * (1.0 - wa))
                if math.acos(max(-1.0, min(1.0, float(dt @ fa)))) < need:
                    side_ = dt - fa * (dt @ fa)
                    if np.linalg.norm(side_) < 1e-6:
                        side_ = dk - fa * (dk @ fa)
                    dt = math.cos(need) * fa + math.sin(need) * side_ / np.linalg.norm(side_)
                ut = uu * wa + uk * (1.0 - wa)
                ut = ut - dt * (ut @ dt)
                ut = ut / np.linalg.norm(ut)
                want = BW.bow_wrist(pose, dt, ut)
            else:
                want = BW.bow_wrist(pose, dd, uu)
            want = canon(want, last_wrist[0] if last_wrist[0] is not None else s["left_hand"][0])
            s["left_hand"] = (tuple(float(v) for v in want), None)
            pose["left_hand"] = (s["left_hand"][0], Z3)
        if track:
            last_wrist[0] = np.array(s["left_hand"][0], dtype=float)
        return s, pose

    def nock_at(pose, t):
        return K.point(K.world(pose, BW.BOW), (BW.nock_x(max(draw_amount(t), 1.0)), BW.NOCK_Y, 0.0))

    # The reach, solved key by key (each from the last, so the shoulder keeps one answer: raised in front, elbow out).
    s0 = BW.sample(ks, REACH_FROM)
    arm_keys = [(0.0, (*READY["right_arm"], READY["right_forearm"][0])),
                (REACH_FROM, (*s0["right_arm"][0], s0["right_forearm"][0][0]))]
    reach_err = []
    for tk, what, guess in REACH_KEYS:
        i = int(round(tk * SUB))
        sk, pose = body_at(i)
        if guess is None:
            guess = (*sk["right_arm"][0], sk["right_forearm"][0][0])
        target = {"via_up": lambda: torso_pt(pose, VIA_UP), "via_out": lambda: torso_pt(pose, VIA_OUT),
                  "grab": lambda: torso_pt(pose, grab), "pluck": lambda: torso_pt(pose, grab + PLUCK_LIFT * qup),
                  "nock": lambda: nock_at(pose, tk)}[what]()
        prev = np.array(arm_keys[-1][1])
        ew, up = {"via_up": (0.0, 0.35), "grab": (0.3, 0.6), "pluck": (0.3, 0.6), "via_out": (0.15, 0.3), "nock": (0.5, 0.15)}[what]
        ra, rf, e = string_arm(pose, target, guess, None, ew=ew, up=up, gw=0.03, fwd=NOCK_FWD if what == "nock" else 0.0)
        x = np.array([*canon(ra, prev[:3]), rf[0]])
        arm_keys.append((tk, tuple(x)))
        if what in ("grab", "nock"):
            reach_err.append((tk, what, e))
    print("reach keys:", ", ".join("%s %.2f at %.2f px" % (w, t, e) for t, w, e in reach_err),
          "| arm", [(t, tuple(int(round(v)) for v in x)) for t, x in arm_keys])

    def loose_target(pose, t, Fk):
        k = min(1.0, (t - IMPACT) / FLY_TICKS)
        e = k ** 3 * (10.0 - 15.0 * k + 6.0 * k * k)      # from rest and back to rest over FLY_TICKS
        A = K.point(K.world(pose, "head"), BW.ANCHOR)       # it rides with the head from here
        N = A - d * (FLY * e + DRIFT * BW.ramp(t, IMPACT + 0.8, IMPACT + 3.6))             - side * FLY_OUT * math.sin(math.pi * min(1.0, k * 1.2) / 2.0) * e - np.array([0.0, 0.6, 0.0]) * e
        return N if Fk is None else N + (Fk - N) * BW.ramp(t, *STRING_OUT)

    out, worst, warm, fly = [], 0.0, np.array(arm_keys[-1][1]), None
    for i in range(T * SUB + 1):
        t = i / SUB
        s, pose = body_at(i, track=True)
        amt = draw_amount(t)
        if t <= ARROW_ON:                   # guard, over the shoulder, the quiver, the pluck, round and onto the string
            x = K.curve(arm_keys, t)
            kf = np.array(s["right_forearm"][0]) * (1.0 - BW.ramp(t, REACH_FROM, 2.0))     # the guard's forearm twist lets go
            s["right_arm"], s["right_forearm"] = (tuple(x[:3]), None), ((x[3], kf[1], kf[2]), None)
        elif t <= STRING_OUT[1]:
            Fk = K.point(K.world(pose, "right_forearm"), BW.FIST)
            if t < IMPACT:                  # on the string through the raise, the draw and the hold
                target = nock_at(pose, t)
            else:                           # loosed: the fist flies back along the arrow's line, clear of the hood, and drifts on
                target = loose_target(pose, t, Fk)
            up = 0.15 - 0.35 * BW.ramp(t, ARROW_ON, 8.6) + 0.55 * BW.ramp(t, 8.6, 11.0) - 0.4 * BW.ramp(t, IMPACT, IMPACT + 2.0)
            if t == IMPACT:                 # the string slips off the fingers: the arm holds the anchor's answer for this sample
                ra, rf, e = tuple(warm[:3]), (warm[3], 0.0, 0.0), 0.0
            elif IMPACT < t < IMPACT + FLY_TICKS:
                # the fly itself: joint angles eased between the anchor's answer and the end of the fly's (solving every
                # sample lets the elbow swing out and back inside one sample, a pop)
                k = (t - IMPACT) / FLY_TICKS
                x = fly[0] + (fly[1] - fly[0]) * (k ** 3 * (10.0 - 15.0 * k + 6.0 * k * k))
                ra, rf, e = tuple(x[:3]), (x[3], 0.0, 0.0), 0.0
            else:
                g = (*s["right_arm"][0], s["right_forearm"][0][0])
                ra, rf, e = string_arm(pose, target, g, warm, damp=0.04, up=up,
                                       fwd=NOCK_FWD * (1.0 - BW.ramp(t, ARROW_ON + 1.0, 11.0)))
            warm = (*ra, rf[0])
            if t == IMPACT:                 # solve where the fly ends, from here
                j = i + int(round(FLY_TICKS * SUB))
                sj, pj = body_at(j)
                rj, fj, ej = string_arm(pj, loose_target(pj, j / SUB, None), (*sj["right_arm"][0], sj["right_forearm"][0][0]),
                                       warm, damp=0.04, up=-0.2 + 0.55 - 0.4 * BW.ramp(j / SUB, IMPACT, IMPACT + 2.0))
                fly = (np.array(warm), np.array([*rj, fj[0]]))
                print("loose: the fly ends %.2f px off its target" % ej)
            if t <= IMPACT:
                worst = max(worst, e)
            wi = 1.0 - BW.ramp(t, STRING_OUT[1] - 2.6, STRING_OUT[1] - 0.4)
            s["right_arm"] = (tuple(a + (b - a) * wi for a, b in zip(s["right_arm"][0], ra)), None)
            s["right_forearm"] = (tuple(a + (b - a) * wi for a, b in zip(s["right_forearm"][0], rf)), None)
        s["draw"] = (Z3, (float(amt), 0.0, 0.0))
        out.append(s)
    print("direct: aim yaw %.1f, anchor err %.3f px, string err max %.3f px, delta %s"
          % (yaw, err, worst, {b: tuple(round(c, 1) for c in v) for b, v in delta.items()}))
    return out, legs


def clips():
    upper, legs = shot()
    for s in upper:
        s.pop("quiver", None)               # soft_motion owns the quiver, the hood cape, the tippet and the skirts
    upper, legs = fullbody.soft_motion(upper, legs, report=True)
    clip = K.bake(upper, T, loop=False)
    clip["timeline"] = {K.fmt_t(FULL_AT / BW.TPS): "full", K.fmt_t(IMPACT / BW.TPS): "impact"}
    return {"attack_direct": clip, "attack_direct_legs": K.bake(legs, T, loop=False)}


def main():
    exported = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/archer.animation.json"
    out = ROOT / "work/archer_direct/archer.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    assert out.resolve() != exported.resolve(), "this demo never writes the exported file; copy its clips by hand after approval"
    if not out.is_file():
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(exported, out)
    data = json.loads(out.read_text(encoding="utf-8"))
    new = clips()
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
