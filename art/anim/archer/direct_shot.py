"""The archer's direct shot: Kort's approved round 3 (2026-11-11, "i aprove of archer animation"), ported onto the bow
builder's 22-tick loose. The archer's attack_direct and attack_direct_legs come from here.

    build_bow_anims.py (GG_BOW_TROOP=archer, the default) calls clips() for the archer's attack_direct pair; every other
    bow troop, and the archer's volley, stance and aim clips, stay the builder's own (the aim stances still take their
    full-draw frame from the builder's own direct shot, which it computes for that and no longer writes).
    python art/anim/archer/build_direct_demo.py [--out file] is a thin runner: it writes the pair into a COPY of the
    exported file (default work/archer_direct/archer.animation.json) to look at it without a full rebuild.

Timing comes from the builder (B below): the keys are written in the 26-tick clock Kort graded (loose on 16, full on 12,
B.IMPACT0), and w() carries them onto B's clock through the same points the approved clip used: B.at(12) = B.FULL_AT
(16.5), B.at(16) = B.IMPACT (22), B.at(26) = B.ATTACK_TICKS (32). Two pacing points inside the shot are the approved
clip's own and are kept (the nock at 8.75, the set at 11.5: B.at's plain 22/16 stretch would put them 0.7 and 0.9 ticks
later and shorten the pull Kort graded good). So the one timing difference from the approved clip is the full draw half
a tick earlier (16.5, was 17): the pull is half a tick shorter, the hold half a tick longer.

It stands on art/anim/archer/build_bow_anims.py (B): the bow line, the aim, the nock frames and the string's fist are
that builder's; the guard wrist is B.GUARD_WRIST, the stance roll every bow-troop stance holds. What the round-3 clip
changed from the builder's shot, and why (Kort's 2026-11-11 grades of the knight demo, and focus_check on the archer's
earlier shot):

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
                The bow-troop stances hold the bow the same way (B.GUARD_WRIST).
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

Timing: B.ATTACK_TICKS (32) ticks, "full" marker on B.FULL_AT (16.5; AttackController holds there), "impact" on
B.IMPACT (22), as sheets/design.json bow_direct impactTicks. Kort, round 1: "shot seems quick"; he set 22 ticks to the
loose, same damage per arrow (2026-11-11). The approved clip (round 3) was built at full 17 before the builder took 22.

Phases:
  0-2       anticipation  eyes on the foe, the bow dips, the weight settles
  0.8-6     reach         the string-side foot drops back 7.6 px; the string hand goes over the shoulder to the quiver
  4.1-4.6   in the quiver a beat on the fletching, then the arrow is drawn up and out (5.3)
  4-8       bow up        the arm carries the bow up onto the line, held as it was in the guard
  6.1-9.1   nock          the bow-side foot turns onto the line; the arrow comes down onto the string (8.75), low
  10-17     raise, draw   one long pull up to the ear, standing tall and side-on
  17-22     hold          at the anchor; the back keeps squeezing, the bow arm trembles
  22        loose         the fist flies back behind the ear, the bow rocks forward in the hand, the pelvis rocks back
  22-25     follow        held open, everything loose still swinging
  23.8-32   recover       the bow comes down (24.6-29); the bow-side foot steps home, then the string-side foot
"""
import math
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
# The builder that is running: build_bow_anims.py registers itself under this name before it imports this module, so a
# build run as a script shares its one module (and its knight builder) with this file; the runner imports it plainly.
import build_bow_anims as BW  # noqa: E402

sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

K, np = BW.K, BW.np
SUB, Z3 = BW.SUB, BW.Z3
# Batch review 2026-11-12: the approved shot is the standard for every bow troop (lead's brief: "the bar for every shot is
# the approved archer attack_direct r3 ... each fitted to that troop's own bow and build"). FIT holds what differs per
# troop: its quiver (bone, file roll, the fletching it grabs, quiver-local bedrock px), the anchor beside its own head gear,
# and the volley's lean. The archer's values are the approved clip's; its attack_direct is byte-identical to round 3.
FIT = {
    "archer": {"quiver": "quiver", "roll": -28.0, "fletch": (-1.2, 25.6, 5.1), "anchor": (-6.5, 25.0, 0.0)},
    # the tall hip quiver: fletching cube x -3.5..-1.5, y 21..25, z 2.9..4.9. Its mouth is lower and further back than the
    # archer's: the grab is the fletching's outer, front, top corner, so the elbow stays out to the side (in the cube's
    # middle the upper arm went up behind the back, shoulder 162). Under the hat brim (y 30) the archer's anchor.
    # The stave is 4.3 px longer each way (y -4.5..32.5): the bow arm is carried 20 degrees higher through the reach, and the
    # bow is nocked canted 30 degrees and comes upright with the draw, so the lower limb clears the ground (3.3 px under).
    "longbow": {"quiver": "tall_quiver", "roll": -8.0, "fletch": (-3.4, 24.9, 3.0), "anchor": (-6.5, 25.0, 0.0),
                "cant": -30.0, "bow_raise": 20.0},
    # the bone quiver: fletching cube x -1.5..1.5, y 22..26, z 3.4..5.4 (the archer's, a pixel lower). The skeleton's
    # shoulders sit a pixel higher (y 23): the anchor a pixel higher with them, and the direct shot wishes the chest upright
    # (the solve leaned it 23 degrees sideways to reach the cheek). The bow comes up as the hunter's does (canted, higher,
    # the face off the top limb: it went 2.3 px into the skull), and the nock grip a pixel higher and further out (the
    # string bracer lay 2.3 px in the jerkin).
    "bonebow": {"quiver": "bone_quiver", "roll": -26.0, "fletch": (-1.2, 24.6, 4.9), "anchor": (-6.5, 26.0, 0.0),
                "chest_roll_w": 0.2, "cant": -30.0, "cant_out": (7.5, 11.5), "bow_raise": 20.0, "nock_head": (-10, -30, 0),
                "nock_grip": (5.0, 22.2, -8.6), "set_grip": (5.0, 22.0, -7.6)},  # nock grip 1 px further forward (z -8.6): the string bracer cleared the jerkin (scorecard pen_limb 1.68 -> 1.35)
    # the hide quiver: arrow shafts x -3..0, y 23..27, z 3..4, grabbed on its outer (string-side) shafts, clear of the
    # collar; the anchor clears the boar ear (x 4..5, turned 14 out). The snout and the bulky jerkin: the bow is nocked
    # canted 30 degrees (upright again by the set) and carried higher through the reach, so its top limb passes in front of
    # the snout, not through it; the grips sit a pixel higher and further out from the jerkin; the face turns further off
    # the bow arm, at the nock and at full draw (the snout lay on the bow arm, 1.94 px).
    "wm_hunter": {"quiver": "quiver", "roll": -16.0, "fletch": (-2.6, 25.4, 3.5), "anchor": (-7.5, 24.5, 0.0),
                  "cant": -30.0, "cant_out": (7.5, 11.5), "bow_raise": 20.0, "nock_head": (-10, -30, 0), "head_yaw": -34.0,
                  "nock_grip": (4.8, 21.6, -8.2), "set_grip": (5.0, 22.0, -7.6)},  # nock grip 1 px further forward (z -8.2): the string forearm cleared the jerkin (scorecard pen_limb 1.52 -> 1.07)
}[BW.TROOP]
# The clock: B's. The keys are written in the 26-tick shot Kort graded (loose on B.IMPACT0 = 16, full on 12).
OLD_FULL, OLD_TICKS = 12.0, 26.0
T, FULL_AT, IMPACT = BW.ATTACK_TICKS, BW.FULL_AT, BW.IMPACT
assert (BW.at(OLD_FULL), BW.at(BW.IMPACT0), BW.at(OLD_TICKS)) == (FULL_AT, IMPACT, T)
# The approved clip's own pacing inside the shot (Kort graded it good): the nock and the set. B.at alone would put them at
# 9.49 and 12.38 and leave a 4.1-tick pull (the approved one is 5.5).
APPROVED_PACE = [(6.9, 8.75), (9.0, 11.5)]
RETIME = [(0.0, 0.0), *APPROVED_PACE, (OLD_FULL, FULL_AT), (BW.IMPACT0, IMPACT), (OLD_TICKS, float(T))]
assert all(b < e for (a, b), (c, e) in zip(RETIME, RETIME[1:])) and APPROVED_PACE[-1][1] < FULL_AT


def w(t):
    """A tick of the 26-tick shot (loose on 16) on the builder's clock (loose on B.IMPACT); after the loose it is B.at."""
    for (a, b), (c, e) in zip(RETIME, RETIME[1:]):
        if t <= c:
            return b + (e - b) * (t - a) / (c - a)
    return RETIME[-1][1]


ELEV = BW.ELEV["direct"]
ANCHOR = FIT["anchor"]              # head-local (archer (-6.5, 25, 0)): drawn to the ear, outside the hood (B.ANCHOR (-5, 26, -1) is inside it)
ELBOW = (-6.0, 18.0, 0.0)           # right_forearm-local: the string arm's elbow
HEAD_YAW = FIT.get("head_yaw", -26.0)                    # degrees the face may stay off the foe at full draw (toward the string side); -26 lets
#                                     the neck off its 70 degree limit (fix r1 F8: it sat at 69-70 through the whole hold)

# Fix r1 (F1): the quiver sits over the STRING shoulder, as the hand model has it. Until 2026-11-12 the exported geo leaned
# it over the bow shoulder in game (troop_rig.geo() wrote the bow troops' quiver roll mirrored); troop_rig now writes it as
# rotation z -28, and the reach below is aimed at that quiver, read from the model.
QUIVER_PIVOT = (0.0, 18.0, 3.0)     # the quiver bone's pivot (bedrock px, torso frame)
QUIVER_ROLL = FIT["roll"]           # its file rotation z (checked against the model in quiver_local); archer -28
FLETCH = FIT["fletch"]              # archer (-1.2, 25.6, 5.1): quiver-local (bedrock px): the grab, in the 3x4x2 fletching cube (x -1.5..1.5, y 23..27, z 3.5..5.5), its outer back half
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
# The bow-troop stances hold the same wrist (build_bow_anims.py BOW_STANCES), so the shot leaves and meets them on it.
GUARD_WRIST = BW.GUARD_WRIST       # (-15.3, -2.8, -73.2): the stances' roll, so the shot starts and ends on it


def guard(p):
    q = dict(p)
    q["left_hand"] = GUARD_WRIST
    return q


READY = guard(BW.READY)
ANTIC = guard(P((7, 21, 0), (-5, -25, 0), (-34, -18, -9), -18, -12, (-38, -12, 8), (-34, 40, 0)))


def lifted(p):
    """FIT "bow_raise": degrees the bow arm is carried further forward and up while the other hand is at the quiver (a
    longer stave's lower limb clears the ground as the bow turns onto the line). 0 for the archer."""
    q = dict(p)
    a = q["left_arm"]
    q["left_arm"] = (a[0] - FIT.get("bow_raise", 0.0), a[1], a[2])
    return q


REACH = lifted(guard(P((3, -4, 0), (5, -8, 6), (-30, -10, -6), -20, -10, (-165, 0, -15), (-60, 0, 0))))
PLUCK = lifted(guard(P((2, -7, 0), (-2, -10, 0), (-32, -11, -6), -21, -10, (-170, 0, -17), (-66, 0, 0))))
NOCK = P((6, 20, 0), FIT.get("nock_head", (-3, -44, 0)), (-58, -26, -4), -12, -6, (-66, -8, 2), (-42, 30, 0))
SET = P((4, 27, 0), (-4, -60, 0), (-88, -4, 0), -55, 0, (-80, -30, 0), (-30, 20, 0))
SET_GRIP = FIT.get("set_grip", (5.0, 21.2, -7.0))        # torso-local (point() frame): where the bow's grip sits at the set, in front of the
#                                     chest and low, so the nocked string is in the string hand's reach and clear of the hood
NOCK_GRIP = FIT.get("nock_grip", (4.6, 20.8, -6.5))       # the same at the nock, a little lower: the bow is already in front, not out to the side
CHEST_YAW = -82.0                   # degrees side-on the chest should stop at (left to itself the solve goes past 90)
FULL = P((1, 35, 0), (-2, -70, 0), (-90, -40, 0), 0, 0, (-95, -20, 0), (0, 70, 0))
TENSE = P((0.4, 36.5, 0), (-2, -71, 0), (-90.5, -40.5, 0), 0, 0, (-95, -20, 0), (0, 70, 0))
LOOSE = P((0, 38, 0), (-4, -71, 0), (-88, -44, 0), 0, 0, (-100, -5, -10), (0, 95, 0))
FOLLOW = P((-2, 39, 0), (-4, -71, 0), (-84, -46, 0), 0, 4, (-104, 0, -16), (0, 100, 0))
DOWN = guard(P((3, 24, 0), (-4, -44, 0), (-62, -30, -3), -6, -4, (-58, -8, 11), (-16, 50, 0)))

SHOT_FEET = {"right": (-2.2, 2.0, -5.6), "left": (2.6, 2.0, 1.6)}     # the ankles in the shooting stance
LEGS = {
    # Kort, archer round 1: "legs look abnormal". Round 1 stood side-on on feet 3.8 px apart with the pelvis sunk 3.1 px:
    # both knees bent 90 degrees through the draw and the hold (the guard bends them 45), a squat on a tightrope. An
    # archer stands TALL on a wide base: feet a shoulder width apart along the line of the shot (8.5 px here), the pelvis
    # no lower than the guard carries it, knees 47 to 52 degrees. The pelvis travels back and turns instead of sinking.
    # (tick, (pitch, yaw, roll), (x, y, z)): the position is in the clip's own frame, +x the troop's left, +z BACK.
    "hips": [(0, *K.READY_HIPS),
             (w(1.5), (1.0, 8.0, 0.0), (0.6, -1.2, 0.2)),        # onto the bow-side foot
             (w(4.0), (0.0, 20.0, 0.0), (0.4, -1.1, 1.6)),       # back with the right foot as it drops behind
             (w(5.6), (0.0, 30.0, 0.0), (-0.3, -1.0, 2.6)),      # over the rear foot while the lead foot steps onto the line
             (w(7.4), (1.0, 40.0, 0.0), (0.1, -1.0, 2.2)),       # side-on already
             (w(10.0), (1.5, 45.0, 0.0), (0.2, -1.0, 2.0)),         # and RISES into the draw (round 3): the knees
             #                                                    open from 45 to 33 degrees, the shins stand up off the feet
             (FULL_AT, (2.0, 47.0, 0.0), (0.2, -0.75, 1.9)),  # between the feet, tall
             (IMPACT, (2.0, 48.0, 0.0), (0.2, -0.8, 1.8)),
             (w(17.3), (-0.5, 50.0, 0.0), (0.1, -0.7, 2.5)),     # the loose rocks it back and up
             (w(19.0), (0.0, 47.0, 0.0), (0.1, -1.0, 2.3)),
             (w(22.0), (0.5, 24.0, 0.0), (0.0, -1.1, 0.8)),
             (T, *K.READY_HIPS)],
    # (foot, lift tick, plant tick, (x, y, z) of the ankle, foot yaw, lift height)
    # The string-side foot drops back 7.6 px as the hand goes to the quiver; the bow-side foot then steps 3.6 px towards
    # the foe onto the line of the shot. One foot at a time, on the way back too (round 1 had both in the air at 20-21.4).
    "steps": [("right", w(0.6), w(4.6), SHOT_FEET["right"], -36.0, 1.0),      # yaws: see KNEE_TURN
              ("left", w(4.8), w(7.2), SHOT_FEET["left"], 6.0, 0.8),
              ("left", w(17.8), w(20.6), (3.2, 2.0, -2.0), 7.0, 0.8),
              ("right", w(20.8), w(24.8), (-3.4, 2.0, 2.0), -12.0, 0.9)],
}

ARROW_ON = w(6.9)  # fix r1 F2: the arrow is on the string as the fist arrives with it (was 7.8, in a closed fist)
DRAW_KEYS = [(ARROW_ON, 1.0), (w(7.8), 1.0), (w(9.0), 1.15), (w(10.6), 2.4), (FULL_AT, 3.0), (IMPACT, 3.0)]
AIM_IN, AIM_OUT = (w(3.0), w(6.4)), (w(18.6), w(23.0))   # the bow comes onto the line as the arm rises to the nock (round 1: by 4.4,
#                                              with the arm still low, which put the resting string through the forearm)
STRING_CLEAR = 48.0                          # degrees the arrow line keeps off the forearm's line while the string is at rest
STRING_OUT = (w(18.4), w(22.6))
# fix r1 F1/F3: the string arm's way to the quiver and back, as arm keys SOLVED onto targets (torso-local px, or the
# fletching, or the nock) on the body's own pose at that tick, then eased between as joint angles: the fist travels in
# arcs, it is exact where it must touch, and nothing chases a moving target through two answers of the shoulder.
# (tick, target, elbow up, elbow forward). Quiver to nock: 4.0 to 6.25, instead of a 0.8 tick whip.
# (tick, target, the arm's starting guess (right_arm x, y, z, right_forearm x)). The guesses hold the one natural answer:
# the upper arm raised in FRONT towards overhead, the elbow flexed, so the flexion carries the hand back behind the head
# (the solver left to itself twists the shoulder round and bends the elbow backward). Quiver to nock: 4.0 to 6.25.
REACH_KEYS = [(w(1.8), "via_up", (-115.0, 0.0, 0.0, -85.0)), (w(3.25), "grab", (-160.0, 0.0, 0.0, -110.0)),
              (w(3.6), "grab", (-160.0, 0.0, 0.0, -110.0)), (w(4.2), "pluck", (-165.0, 0.0, 0.0, -100.0)),
              (w(5.2), "via_out", (-125.0, 0.0, 0.0, -95.0)), (ARROW_ON, "nock", None)]
NOCK_FWD = FIT.get("nock_fwd", 0.8)      # the elbow comes forward at the nock (the fist is a pixel off the chest there; a tucked elbow lays
#                     the forearm along the jerkin)
REACH_FROM = w(0.25)   # the string hand leaves the guard (the arm keys before it are the keyed guard)

FLY = 9.0          # px the fist flies back along the arrow's line at the loose (fix r1 F3: was 4.4)
FLY_OUT = -1.5      # px it clears the hood sideways on the way
FLY_TICKS = 1.0    # was 1.5: the loose is the fastest event of the clip
DRIFT = 1.6        # px the open hand keeps drifting back through 17-19.5 (fix r1 F4)
KICK = 24.0        # degrees the bow's top limb rocks forward in the hand after the loose (was 13)
RING = 3.0         # ticks per swing of the bow's damped ring in the open hand
TREMBLE = 1.6      # degrees, the held draw's tremble (was 0.9)
BREATH = 1.0       # degrees of torso pitch, one breath across the hold


FULL_FRAME = None   # set by clips(): the direct shot's full-draw sample (upper body), the aim stances' base
NOCK_AT, SET_AT = round(w(6.75) * SUB) / SUB, w(9.0)     # on the sample grid: their bow arms are solved on that sample's legs


def keys(drawn, nock, set_):
    return [(0, READY), (w(1.5), ANTIC), (w(3.2), mix(ANTIC, REACH, 0.86)), (w(3.8), REACH), (w(4.8), PLUCK),
            (NOCK_AT, nock), (SET_AT, set_), (FULL_AT, drawn(FULL)), (w(15.4), drawn(TENSE)),
            (IMPACT, drawn(LOOSE)), (w(17.6), drawn(FOLLOW)), (w(19.2), drawn(mix(FOLLOW, LOOSE, 0.5))),
            (w(22.4), DOWN), (T, READY)]


def draw_amount(t):
    if t < DRAW_KEYS[0][0] or t >= IMPACT:
        return 0.0
    return K.curve([(a, (v,)) for a, v in DRAW_KEYS], t)[0]


def _yaw_of(pose, bone):
    R = K.world(pose, bone)[:3, :3] @ K.world({}, bone)[:3, :3].T
    return math.degrees(math.atan2(R[0, 2], R[2, 2]))


def solve_full(key, legs, elev, roll_w=0.02):
    """The bow builder's solve_drawn (torso, head, bow arm and aim yaw putting the drawn nock on the anchor) with one
    more wish: the face stays on the foe. Left to itself the solve turns the head 30 degrees away to shorten the draw."""
    x0 = np.array([*key["torso"], key["head"][0], key["head"][1], *key["left_arm"], key["left_forearm"][0], 0.0])
    w = np.array([0.02, 0.02, roll_w, 0.03, 0.02, 0.03, 0.03, 0.03, 0.06, 0.12])

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
        A = K.point(K.world(pose, "head"), ANCHOR)
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
    """The quiver's fletching grab and its arrows' direction in the torso's frame (bedrock px), as the model has it."""
    b = K.MODEL.bones[FIT["quiver"]]
    r = np.array(b.rot, dtype=float)           # the baked z is the file's z (render_geo: rotation (-rx, -ry, +rz))
    assert abs(r[2] - math.radians(QUIVER_ROLL)) < 1e-6, "the %s geo's quiver roll is not %s: re-export it" % (BW.TROOP, QUIVER_ROLL)
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


# Kort, archer round 2: "the bottom flat piece is flush with the ground but then its lower leg is at an angle and it makes
# a weird crease". K.attack_legs keeps every knee facing the troop's front whatever the foot does: side-on, on feet turned
# 32 and 25 degrees from the guard, that twisted the right ankle 26 degrees and rolled it 38 (left: twisted 19). The knee
# tracks over the toes: each thigh turns with its own foot, and the feet point where their legs can follow (the rear foot
# across the line, the lead foot at the foe, an open stance). Swept at full draw (work/ar_ankle2.py): these leave each
# ankle within 10 degrees of twist and of roll, what the guard itself has. Degrees in the world, the foot yaw's sense.
KNEE_TURN = {"right": -37.0, "left": 6.0}


def shot_legs(turn=None):
    """K.attack_legs with each thigh turned KNEE_TURN in the shooting stance, eased in and out with the foot's own step
    (by how far the ankle is from the guard's footing), so the clip still starts and ends on the guard's legs."""
    turn = KNEE_TURN if turn is None else turn
    hips_rot = [(k[0], k[1]) for k in LEGS["hips"]]
    hips_pos = [(k[0], k[2]) for k in LEGS["hips"]]
    out, guesses = [], {}
    for i in range(T * SUB + 1):
        t = i / SUB
        hips = (K.curve(hips_rot, t), K.curve(hips_pos, t))
        pose, f = {"hips": hips}, {"hips": hips}
        feet = K.feet_at(LEGS["steps"], t)
        for side in ("right", "left"):
            at, yaw, pitch = feet[side]
            home, there = np.array(K.READY_FEET[side][0]), np.array(SHOT_FEET[side])
            u = min(1.0, float(np.linalg.norm((np.array(at) - home)[[0, 2]]) / np.linalg.norm((there - home)[[0, 2]])))
            pose[side + "_leg"] = ((0, -hips[0][1] - turn[side] * u, 0), (0, 0, 0))
            lr, sr, fr = K.leg_pose(pose, side, at, pitch, foot_yaw=yaw, guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            f[side + "_leg"], f[side + "_shin"], f[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        out.append(f)
    return out


def shot(volley=False):
    """The shot's samples. volley: the same shot lofted (the builder's raised(): the chest leans back CHAR["lean"], the
    head and both arms lift) onto ELEV["volley"]; the string is still taken with the bow near level at the set, and the
    loft comes in with the draw (set to full), as the builder's own volley does, so the upper limb never lies back into
    the head gear while the hand is at the cheek."""
    legs = shot_legs()
    elev = BW.ELEV["volley"] if volley else ELEV
    up = BW.raised if volley else (lambda p: p)
    # FIT "chest_roll_w": the direct shot's wish that the chest stays upright (the bone archer's higher shoulders let the
    # solve reach the anchor by leaning the chest 23 degrees sideways). The volley's lean back IS a roll of the side-on chest.
    solved, yaw, err = solve_full(up(FULL), legs[int(round(FULL_AT * SUB))], elev,
                                  0.02 if volley else FIT.get("chest_roll_w", 0.02))
    if err > 0.3:
        raise ValueError("anchor unreachable by %.2f px" % err)
    delta = {b: tuple(a - c for a, c in zip(solved[b], up(FULL)[b])) for b in ("torso", "head", "left_arm", "left_forearm")}

    def drawn(p):
        p = up(p)
        q = dict(p)
        for b, dv in delta.items():
            q[b] = tuple(a + c for a, c in zip(p[b], dv))
        return q

    d, u = BW.aim(elev, yaw)
    d0, u0 = BW.aim(ELEV, yaw) if volley else (d, u)
    cant = math.radians(FIT.get("cant", 0.0))

    def canted(dd, uu, t):
        """FIT "cant": the bow is nocked canted, its top limb tipped this far out to the bow side about the arrow's line,
        and comes upright with the draw (set to full). A longer stave's lower limb then clears the ground. 0: the archer."""
        c = cant * (1.0 - BW.ramp(t, *FIT.get("cant_out", (NOCK_AT, FULL_AT))))
        if c == 0.0:
            return dd, uu
        sd = np.cross(uu, dd)
        return dd, math.cos(c) * uu + math.sin(c) * sd / np.linalg.norm(sd)

    def aim_at(t):
        if not volley:
            return canted(d, u, t)
        return canted(*BW.aim(ELEV + (elev - ELEV) * BW.ramp(t, SET_AT, FULL_AT), yaw), t)

    ks = keys(drawn, fit_bow_arm(NOCK, legs[int(round(NOCK_AT * SUB))], NOCK_GRIP, *canted(d0, u0, NOCK_AT)),
              fit_bow_arm(SET, legs[int(round(SET_AT * SUB))], SET_GRIP, *canted(d0, u0, SET_AT)))
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
            dd, uu = aim_at(t)
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
        gu = FIT.get("grab_up", 0.6)       # how far up the elbow is wished at the quiver (archer 0.6)
        ew, up = {"via_up": (0.0, 0.35), "grab": (0.3, gu), "pluck": (0.3, gu), "via_out": (0.15, 0.3), "nock": (0.5, 0.15)}[what]
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
        A = K.point(K.world(pose, "head"), ANCHOR)       # it rides with the head from here
        N = A - d * (FLY * e + DRIFT * BW.ramp(t, IMPACT + 0.8, IMPACT + 3.6))             - side * FLY_OUT * math.sin(math.pi * min(1.0, k * 1.2) / 2.0) * e - np.array([0.0, 0.6, 0.0]) * e
        return N if Fk is None else N + (Fk - N) * BW.ramp(t, *STRING_OUT)

    out, worst, warm, fly = [], 0.0, np.array(arm_keys[-1][1]), None
    for i in range(T * SUB + 1):
        t = i / SUB
        s, pose = body_at(i, track=True)
        amt = draw_amount(t)
        if t <= ARROW_ON:                   # guard, over the shoulder, the quiver, the pluck, round and onto the string
            x = K.curve(arm_keys, t)
            kf = np.array(s["right_forearm"][0]) * (1.0 - BW.ramp(t, REACH_FROM, w(2.0)))     # the guard's forearm twist lets go
            s["right_arm"], s["right_forearm"] = (tuple(x[:3]), None), ((x[3], kf[1], kf[2]), None)
        elif t <= STRING_OUT[1]:
            Fk = K.point(K.world(pose, "right_forearm"), BW.FIST)
            if t < IMPACT:                  # on the string through the raise, the draw and the hold
                target = nock_at(pose, t)
            else:                           # loosed: the fist flies back along the arrow's line, clear of the hood, and drifts on
                target = loose_target(pose, t, Fk)
            up = 0.15 - 0.35 * BW.ramp(t, ARROW_ON, w(8.6)) + 0.55 * BW.ramp(t, w(8.6), w(11.0)) - 0.4 * BW.ramp(t, IMPACT, IMPACT + 2.0)
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
                                       fwd=NOCK_FWD * (1.0 - BW.ramp(t, w(7.9), w(11.0))))
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
    print("%s %s: aim yaw %.1f, anchor err %.3f px, string err max %.3f px, delta %s"
          % (BW.TROOP, "volley" if volley else "direct", yaw, err, worst, {b: tuple(round(c, 1) for c in v) for b, v in delta.items()}))
    return out, legs


def clips(volley=False):
    """{"attack_direct": ..., "attack_direct_legs": ...} on the builder's clock (B.ATTACK_TICKS, markers B.FULL_AT and B.IMPACT);
    volley=True: the same pair as attack_volley / attack_volley_legs."""
    upper, legs = shot(volley)
    if not volley:                          # the aim stances' source (build_bow_anims.aim_upper): the held full draw, body only
        global FULL_FRAME
        FULL_FRAME = {b: v for b, v in upper[int(round(FULL_AT * SUB))].items() if b != "quiver"}
    for s in upper:
        s.pop("quiver", None)               # soft_motion owns the quiver, the hood cape, the tippet and the skirts
    upper, legs = fullbody.soft_motion(upper, legs, report=True)
    clip = K.bake(upper, T, loop=False)
    clip["timeline"] = {K.fmt_t(FULL_AT / BW.TPS): "full", K.fmt_t(IMPACT / BW.TPS): "impact"}
    name = "attack_volley" if volley else "attack_direct"
    legs = K.bake(legs, T, loop=False)
    clip["_own_bones"] = legs["_own_bones"] = True      # only_rig keeps their bone names (they are the geo's own)
    return {name: clip, name + "_legs": legs}

