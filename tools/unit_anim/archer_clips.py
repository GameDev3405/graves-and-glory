"""The Thornhide archer on the articulated rig: the Antlerbow in the left hand, an arrow in the right, a hip quiver.

  archer_idle   loop   low ready: an arrow nocked, the bow canted low across the hips, the weight rocking, the eyes
                       hunting left and right, the cloak, feathers and loincloth swaying
  archer_shoot  once   raise, draw to the anchor, settle and aim, loose (the string snaps and shivers, the arrow flies,
                       the draw hand follows through past the jaw), then reach to the quiver, draw an arrow, nock it,
                       back to low ready
  archer_kite   loop   backpedalling at low ready, the back foot reaching back first, the eyes on the threat
  archer_hit    once   struck: the head snaps, the bow flies wide, a stumble back, the arrow kept nocked, recover

The hands are *placed*, not aimed. The bow's grip sits at `bow_at` (the root's frame), turned by `aim` (yaw,
elevation, cant); poses are written by where the nock should be (`bow_for`). The draw hand holds the arrow's nock on the string (`on_string` 1) or goes where the clip sends it
(`hand_at`, in the root's frame: the follow-through, the quiver, the nocking). Both arms reach their hands with an
elbow pulled toward a pole (`r_pole`, `l_pole`, chest frame), and both wrists are solved so the bow and the arrow face
where they should. The string's position (`draw`) bends the bow; after the loose a damped vibration takes over, and
the arrow leaves the hand and flies.
"""
import math

import numpy as np

from . import combat_rig as CR
from . import rig
from .combat_clips import NECK, _cape_clamp, _unwrap, bake_legs_and_head, seq
from .rig import Clip, spring_follow
from . import accessories as AC
from . import pose_search as PS

STEP = 0.025
POSES_FILE = str(__import__('pathlib').Path(__file__).resolve().parent / 'archer_poses.json')
ROOT_REF = np.array([0.0, -1.0, 0.0])     # bow_at, hand_at and nocks are written for the root here (low ready)

# the follow bones: the cloak from the chest, the feather fan from the head, the loincloth from the hips
BONES = CR.bones_with({'cape': ('chest', (0, 21, 3.6)), 'plume': ('head', (-5.5, 28, 1.5)),
                       'loin': ('body', (0, 10.5, -3.3))})

# where the items sit in the fists (hand space) and on the hips
GRIP_R, GRIP_L = np.array([6.0, 12.5, -1.0]), np.array([-6.0, 12.5, -1.0])


def _m(R, t):
    M = np.eye(4)
    M[:3, :3] = R
    M[:3, 3] = t
    return M


# A bow is gripped across the fist: its arrow side (bow x) runs on down the forearm, its upper limb (bow y) out past the
# knuckles. The arrow leaves the draw fingers along the forearm. So with a straight wrist the forearm points down the
# shot.
BOW_LOCAL = _m(np.column_stack([(0, -1, 0), (0, 0, -1), (1, 0, 0)]).astype(float), GRIP_L)
ARROW_LOCAL = _m(rig.rx(math.radians(180)), GRIP_R)
# tilted back and out, as a hip quiver hangs, so an arrow drawn out along it rises up and away from the head
QUIVER_LOCAL = _m(rig.rz(math.radians(-24)) @ rig.rx(math.radians(20)), np.array([6.6, 7.8, 1.2]))

NOCK = np.array([0.0, 0.25, -0.1])       # the nock on the string, bow space (x set by the draw)
REST = np.array([1.2, 0.25, -1.6])       # where the shaft lies on the bow, beside the grip
FLIGHT_SPEED = 160.0
DRAW_FORE = 0.45                         # how hard the draw forearm is pulled in line with the nocked arrow (searched)                     # px a second for the flying arrow (it is off the frame in a few frames)


def unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def aim_dir(yaw, elev):
    """A direction from yaw (degrees, positive toward the troop's right, +x) and elevation (degrees up)."""
    y, e = math.radians(yaw), math.radians(elev)
    return np.array([math.sin(y) * math.cos(e), math.sin(e), -math.cos(y) * math.cos(e)])


def bow_frame(yaw, elev, cant):
    """The bow's rotation in the world: its x along the aim, its y (the upper limb) up, canted `cant` degrees (the
    upper limb toward the troop's right)."""
    x = aim_dir(yaw, elev)
    up = np.array([0.0, 1.0, 0.0])
    y0 = unit(up - np.dot(up, x) * x)
    c, s = math.cos(math.radians(cant)), math.sin(math.radians(cant))
    y = c * y0 - s * np.cross(x, y0)
    return np.column_stack([x, y, np.cross(x, y)])


def vibrate(t):
    """The string after the loose, t seconds on: it snaps past brace and shivers back."""
    return AC.BRACE + 0.75 * math.exp(-t * 9.0) * math.sin(2 * math.pi * 11 * t + 0.4) * min(1.0, t / 0.012)


def build(name, length, loop, timeline, loose=None, ground_speed=0.0, step=STEP):
    c = Clip(name, length, loop)
    c.bones, c.ground_speed, c.loose = BONES, ground_speed, loose
    c.beats = [(t, label) for t, label, _ in timeline]
    for b in ('body', 'chest'):
        c.rot[b] = [(t,) + tuple(float(v) for v in p[b]) for t, _, p in timeline]
    c.pos['root'] = [(t,) + tuple(float(v) for v in p['root']) for t, _, p in timeline]
    keys = ('r_place', 'l_place', 'look', 'bow_at', 'aim', 'draw', 'on_string', 'hand_at', 'arrow_dir', 'r_pole', 'l_pole')
    track = {k: [(t,) + tuple(float(v) for v in np.atleast_1d(p[k])) for t, _, p in timeline] for k in keys}
    c.track = track
    c.arrow_keys = [(t, bool(p['arrow'])) for t, _, p in timeline]
    n = int(round(length / step))
    times = [round(i * step, 4) for i in range(n + 1)]
    bake_legs_and_head(c, track, times, BONES)

    def tr(k, t):
        v = CR.pchip(track[k], t % length if loop else t)
        return v if len(v) > 1 else v[0]
    guess = {'right': ((0.2, -1, -0.3), (0.0, 0.2, -1)), 'left': ((-0.2, -1, -0.3), (0.0, 0.2, -1))}
    prev = {'right': None, 'left': None}
    for pass_ in range(2 if loop else 1):      # a loop solves twice, the second pass starting where the first ended
        out = {k: [] for k in ('right_arm', 'right_forearm', 'right_hand', 'left_arm', 'left_forearm', 'left_hand')}
        c.draws, c.reach_error, c.items = [], 0.0, []
        c.side_error = {'left': 0.0, 'right': 0.0}
        c.misses = []
        if pass_ == 0:          # settle the solver on the first pose before baking anything
            for _ in range(12):
                _solve_tick(c, times[0], tr, loose, {k: [] for k in out}, guess, {'right': None, 'left': None})
            c.draws, c.reach_error, c.items, c.misses = [], 0.0, [], []
            c.side_error = {'left': 0.0, 'right': 0.0}
        for t in times:
            _solve_tick(c, t, tr, loose, out, guess, prev)
    for k, v in out.items():
        _unwrap(v)
        c.rot[k] = v
    spring_follow(c, 'cape', 'chest', (1.0, 1.0), stiffness=60, damping=8, rest=(3, 0), clamp=_cape_clamp(c),
                  passes=3 if loop else 2, bones=BONES)
    spring_follow(c, 'plume', 'head', (0.8, 0.8), stiffness=110, damping=8, rest=(0, 0), passes=3 if loop else 2,
                  bones=BONES)
    spring_follow(c, 'loin', 'body', (1.0, 1.0), stiffness=80, damping=9, rest=(0, 0), clamp=_loin_clamp(c),
                  passes=3 if loop else 2, bones=BONES)
    c.extra_pose = lambda pose, t: extra_pose(c, pose, t)
    c.report = lambda: ['hands on their targets: worst %.3f px; string draw %.2f to %.2f px' %
                        (c.reach_error, min(d for _, d in c.draws), max(d for _, d in c.draws))]
    return c



def _solve_tick(c, t, tr, loose, out, guess, prev):
    """One tick of the arms: place the bow and the draw hand, reach both, solve both wrists."""
    rot, pos = c.at(t)
    pose = CR.world(rot, pos, BONES)
    chest = pose['chest']
    # the string
    d = vibrate(t - loose) if loose is not None and t >= loose and tr('on_string', t) < 0.5 else tr('draw', t)
    # the bow (positions ride with the root: they are written for the root at ROOT_REF)
    shift = np.array([-pos['root'][0], pos['root'][1], pos['root'][2]]) - ROOT_REF
    D = bow_frame(*tr('aim', t))
    P = np.array(tr('bow_at', t)) + shift
    nock = P + D @ (NOCK + np.array([d, 0, 0]))
    rest = P + D @ REST
    # the draw hand: on the nock, or where the clip sends it
    w = float(np.clip(tr('on_string', t), 0, 1))
    free = np.array(tr('hand_at', t)) + shift
    Pa = w * nock + (1 - w) * free
    ya = unit(w * unit(rest - nock) + (1 - w) * aim_dir(*tr('arrow_dir', t)))
    za = unit(D[:, 2] - np.dot(D[:, 2], ya) * ya)
    A = np.column_stack([np.cross(ya, za), ya, za])
    c.items.append((t, d, P, D, Pa, A))
    c.draws.append((t, d))
    # reach both hands: the wrist pivots where the items need them
    for side, item_P, item_R, local in (('left', P, D, BOW_LOCAL), ('right', Pa, A, ARROW_LOCAL)):
        H = item_R @ local[:3, :3].T
        piv = np.array(BONES[side + '_hand'][1], float)
        target = item_P - H @ (local[:3, 3] - piv)
        pole = chest[:3, :3] @ np.asarray(tr(side[0] + '_pole', t), float)
        # the bow arm's forearm points down the shot; the draw arm's lies along the arrow as far as it is nocked
        # (the pull fades with the hand's hold on the string, so nothing switches)
        hint, weight = (item_R[:, 0], 0.6) if side == 'left' else (item_R[:, 1], DRAW_FORE * w)
        u, f, err, swivel = CR.reach_arm(side, chest, target, pole, guess[side], BONES,
                                         fore=hint if weight > 1e-3 else None, fore_weight=weight,
                                         prev_swivel=guess.get(side + '_swivel'))
        guess[side], guess[side + '_swivel'] = (u, f), swivel
        if side == 'right' and w > 0.98 or side == 'left':
            c.reach_error = max(c.reach_error, err)
            c.side_error[side] = max(c.side_error[side], err)
            if err > 0.1:
                c.misses.append((t, side, round(err, 2)))
        a = CR.aim_arm(u, f, prev=prev[side])
        prev[side] = a
        _, F = CR._chain(side, chest, a['arm'], a['forearm'][0], BONES)
        hand = CR.closest_euler(F[:3, :3].T @ H, out[side + '_hand'][-1][1:] if out[side + '_hand'] else None)
        for bone, v in ((side + '_arm', a['arm']), (side + '_forearm', a['forearm']), (side + '_hand', hand)):
            out[bone].append((t,) + tuple(float(x) for x in v))


def _loin_clamp(clip):
    """The loincloth hangs in front of the thighs: it may swing forward, never back into a thigh."""
    def run(t, pitch, roll):
        rot, _ = clip.at(t)
        body = rot.get('body', (0, 0, 0))[0]
        thigh = min(0.0, rot.get('right_leg', (0, 0, 0))[0], rot.get('left_leg', (0, 0, 0))[0])
        return min(pitch, 0.9 * thigh - 2.0 - body), max(-25.0, min(25.0, roll))
    return run


def arrow_visible(clip, t):
    tt = t % clip.length if clip.loop else t
    v = clip.arrow_keys[0][1]
    for kt, kv in clip.arrow_keys:
        if kt <= tt + 1e-9:
            v = kv
    return v


def item_state(clip, t):
    """(draw, bow grip, bow rotation, arrow nock, arrow rotation) at t, between the baked ticks."""
    tt = t % clip.length if clip.loop else min(t, clip.length)
    i = min(len(clip.items) - 2, int(tt / STEP))
    a, b = clip.items[i], clip.items[i + 1]
    k = (tt - a[0]) / (b[0] - a[0])
    lerp = lambda x, y: (1 - k) * x + k * y
    return lerp(a[1], b[1]), lerp(a[2], b[2]), a[3] if k < 0.5 else b[3], lerp(a[4], b[4]), a[5] if k < 0.5 else b[5]


def flight(clip, t):
    """The loosed arrow's world matrix while it is in the air (None otherwise)."""
    if clip.loose is None or t < clip.loose or t > clip.loose + 0.35:
        return None
    _, P, D, _, _ = item_state(clip, clip.loose)
    nock0 = P + D @ (NOCK + np.array([AC.FULL, 0, 0]))
    dirn = unit(P + D @ REST - nock0)
    za = unit(D[:, 2] - np.dot(D[:, 2], dirn) * dirn)
    M = np.eye(4)
    M[:3, :3] = np.column_stack([np.cross(dirn, za), dirn, za])
    M[:3, 3] = nock0 + dirn * FLIGHT_SPEED * (t - clip.loose)
    return M


def extra_pose(clip, pose, t):
    """Pseudo-bones for the items, so the clipping check sees where they really are: 'bow' and 'arrow' (the arrow far
    away while it is not in the hand), 'quiver' on the hips."""
    pose = dict(pose)
    pose['bow'] = pose['left_hand']
    pose['arrow'] = pose['right_hand'] if arrow_visible(clip, t) else np.diag([1, 1, 1, 1.0]) @ _far()
    pose['quiver'] = pose['body']
    return pose


def _far():
    M = np.eye(4)
    M[:3, 3] = (0, -1000, 0)
    return M


# ---- poses ------------------------------------------------------------------------------------
# Positions: bow_at and hand_at in the root's frame (Blockbench axes, the feet at y 0). Directions: aim = (yaw, elevation, cant), arrow_dir = (yaw, elevation) in degrees; poles in the chest's
# frame.

STANCE = dict(root=(0.0, -1.0, 0.0), r_place=(2.2, 3.0, 0, 55, 0), l_place=(2.0, -3.0, 0, 20, 0))

# Ready: the bow carried at the side by its grip, the limbs fore and aft beside the lead leg, the draw hand resting by
# the quiver, no arrow nocked. Neither arm crosses the body: at Minecraft proportions (a 10 px head, a 12 px fur collar,
# 8 px arms) a nocked arrow held low across the belly puts the draw forearm through the chest.
READY = dict(
    STANCE, body=(3, 18, 0), chest=(2, 4, 0), look=(4, 0, 0),
    bow_at=(-5.71, 12.0, -5.0), aim=(1.8, -77.3, -5.9), draw=AC.BRACE, on_string=0.0, hand_at=(7.85, 12.72, -0.29),
    arrow_dir=(0, -80), arrow=0, r_pole=(1.0, -0.64, 0.31), l_pole=(-0.67, -0.6, 0.3))     # searched: clear

# Nocking: the bow held out in front and canted, the arrow laid on it and drawn back to the string.
NOCK_AIM, NOCK_AT = (-25, -25, 55), (2.5, 18.5, -7.5)

# Full draw (found by search, see docs/ANIMATING-TROOPS.md): side-on, the bow arm out at the target, the string at the
# right cheek, the draw elbow out to the side.
SHOT_AIM, SHOT_NOCK = (-16.7, -2.8, 28), (6.5, 21.5, -3.6)
SHOT_TORSO = dict(body=(3, 57, 0), chest=(3, 18, 0), look=(2.8, -16.7, 0), l_pole=(-1.3, 0.28, 0.17),
                  r_pole=(0.19, -0.35, 0.47))


def bow_for(nock, aim, draw):
    """The bow grip that puts the string's nock at `nock` (root frame) with the bow turned by `aim` and the string at
    `draw`."""
    return tuple(np.asarray(nock, float) - bow_frame(*aim) @ (NOCK + np.array([draw, 0.0, 0.0])))


SHOT_BOW = bow_for(SHOT_NOCK, SHOT_AIM, AC.FULL)
NOCK_BOW = bow_for(NOCK_AT, NOCK_AIM, AC.BRACE)


def P(base=None, **kw):
    p = dict(base or READY)
    p.update(kw)
    return p


def _add(a, b):
    return tuple(float(x) + float(y) for x, y in zip(a, b))


_QUIVER = None


def _quiver():
    global _QUIVER
    if _QUIVER is None:
        c = build('probe', 0.05, False, [(0, 'p', READY), (0.05, 'p', READY)])
        M = CR.world(*c.at(0), BONES)['body'] @ QUIVER_LOCAL
        _QUIVER = (tuple(rig.apply(M, (0, 10.0, 0))), tuple(unit(M[:3, 1])))
    return _QUIVER


def quiver_mouth():
    """Where the quiver's fletchings stand in the root's frame at low ready (the hips barely move from it)."""
    return _quiver()[0]


def _down_axis():
    """(yaw, elevation) of a direction pointing back down the quiver: an arrow being drawn out hangs that way."""
    d = -np.array(quiver_axis())
    return (math.degrees(math.atan2(d[0], -d[2])), math.degrees(math.asin(d[1])))


def quiver_axis():
    """The quiver's long axis in the root's frame (out of the mouth): an arrow is drawn out along it."""
    return _quiver()[1]


# ---- the clips ------------------------------------------------------------------------------------

def idle_timeline():
    """3.0 s: two breaths, the weight rocking to the lead foot and back, the carried bow swaying a little, the draw
    hand drifting by the quiver, the eyes checking left (0.9 s) then right (2.0 s)."""
    L, n = 3.0, 15
    steps = []
    for i in range(n + 1):
        t = round(L * i / n, 4)
        a = 2 * math.pi * i / n
        sway, breath = math.sin(a), math.sin(2 * a)
        look = (4 + 2 * math.sin(a), 0.0, 0.0)
        if 0.8 <= t <= 1.4:
            k = math.sin(math.pi * (t - 0.8) / 0.6)
            look = (3, -30 * k, -4 * k)
        if 1.9 <= t <= 2.5:
            k = math.sin(math.pi * (t - 1.9) / 0.6)
            look = (3, 26 * k, 3 * k)
        steps.append((t, 'breath' if i % 5 else 'weight %s' % ('forward' if sway > 0.5 else 'back' if sway < -0.5 else 'centre'),
                      dict(look=look, root=(0.4 * sway, -1.0 - 0.1 * breath - 0.15 * abs(sway), -0.4 * sway),
                           body=(3 + 1.0 * sway, 18 + 2.0 * sway, -2.0 * sway),
                           chest=(2 - 1.2 * breath, 4 - 1.0 * sway, 1.0 * sway),
                           aim=_add(READY['aim'], (2 * math.sin(a + 0.6), 3 * math.sin(a), 0)),
                           bow_at=_add(READY['bow_at'], (0.0, 0.15 * breath, 0.3 * math.sin(a + 0.6))),
                           hand_at=_add(READY['hand_at'], (0.0, 0.2 * breath, -0.3 * math.sin(a + 1.2))),
                           r_place=(2.2, 3.0, 0, 55, max(0.0, 5 * sway)))))
    return seq(READY, steps)


def idle():
    return build('archer_idle', 3.0, True, timeline('archer_idle'))


LOOSE = 1.55


def shoot_timeline():
    """From ready: reach to the quiver, draw an arrow clear, bring it to the bow held out in front, nock it, raise,
    draw to the cheek, settle and aim, loose at 1.55 s (the string snaps, the arrow flies), follow through, lower to
    ready."""
    qm = quiver_mouth()
    shot = dict(SHOT_TORSO, aim=SHOT_AIM, bow_at=SHOT_BOW)
    lift_aim, lift_nock = (-20, -12, 40), (4.5, 20.5, -6.0)
    return seq(READY, [
        (0.00, 'ready', {}),
        (0.12, 'to the quiver', dict(hand_at=_add(qm, (0.5, 3.5, -1.0)), r_pole=(1, 0.3, 0.6), look=(8, 6, 0),
                                     bow_at=(-6.6, 12.5, -3.0), aim=(-10, -60, 20))),
        (0.22, 'grab', dict(hand_at=_add(qm, (0.0, 3.2, -0.3)), arrow=1, arrow_dir=_down_axis())),
        (0.38, 'pull clear', dict(hand_at=_add(_add(qm, 11.5 * np.array(quiver_axis())), (1.0, 0, 0)),
                                  r_pole=(1, 0.6, 0.6), look=(6, 0, 0))),
        (0.54, 'bring to the bow', dict(hand_at=_add(NOCK_AT, (2.0, 2.0, -1.0)), arrow_dir=NOCK_AIM[:2],
                                        aim=NOCK_AIM, bow_at=NOCK_BOW, body=(3, 32, 0), chest=(3, 10, 0),
                                        r_pole=(1, 0.2, 0.5), l_pole=(-1, -0.4, 0.2), look=(10, -10, 0))),
        (0.66, 'nock', dict(on_string=1.0, hand_at=NOCK_AT)),
        (0.80, 'raise', dict(aim=lift_aim, bow_at=bow_for(lift_nock, lift_aim, -3.3), draw=-3.3, hand_at=lift_nock,
                             body=(3, 45, 0), chest=(3, 14, 0), look=(3, -12, 0), l_pole=(-1, -0.2, 0.2),
                             r_pole=(0.6, -0.3, 0.5))),
        (0.98, 'set and draw', dict(shot, draw=-4.2, hand_at=SHOT_NOCK, body=(3, 52, 0), chest=(3, 16, 0))),
        (1.22, 'anchor', dict(draw=AC.FULL, body=SHOT_TORSO['body'], chest=(3, 19, 0))),
        (1.32, 'settle', dict(root=(0, -1.12, 0), chest=SHOT_TORSO['chest'])),
        (1.52, 'aim', dict(draw=-6.1, chest=(3, 18.5, 0))),
        (1.549, 'loose', dict(on_string=1.0, hand_at=SHOT_NOCK)),
        (1.55, 'loose', dict(on_string=0.0, arrow=0)),
        (1.63, 'follow-through', dict(hand_at=_add(SHOT_NOCK, (2.0, 0.6, 2.5)), aim=_add(SHOT_AIM, (3, -2, 2)),
                                      bow_at=_add(SHOT_BOW, (0.2, -0.6, 0.2)), chest=(3, 20, 0))),
        (1.85, 'hold the finish', dict(hand_at=_add(SHOT_NOCK, (2.3, 0.4, 2.8)), chest=(3, 17, 0))),
        (2.15, 'lower', dict(bow_at=_add(READY['bow_at'], (0.0, 1.0, -1.5)), aim=(-5, -70, 10), hand_at=READY['hand_at'],
                             root=(0, -1.05, 0), body=(3, 22, 0), chest=(2, 6, 0), look=(5, 0, 0),
                             r_pole=READY['r_pole'], l_pole=READY['l_pole'])),
        (2.50, 'ready', {k: READY[k] for k in ('root', 'body', 'chest', 'look', 'bow_at', 'aim')}),
    ])


def shoot():
    return build('archer_shoot', 2.5, False, timeline('archer_shoot'), loose=LOOSE)


KITE_SPEED = -4.5       # px a second: the ground slides forward under planted feet as the archer backs away


def kite_timeline():
    """1.2 s: the back foot reaches back (0.0 to 0.3), the lead foot follows (0.6 to 0.85), the bow carried at the
    side and the draw hand swinging a little, the eyes on the threat; the root dips as each foot lands and sways over
    the one holding the weight."""
    v = KITE_SPEED

    def back(t):            # the right foot, planted 0.3 to 1.2
        return 4.2 + v * (t - 0.3)

    def lead(t):            # the left foot, planted 0.85 to 1.8 (wrapping)
        t = t if t >= 0.85 else t + 1.2
        return -0.4 + v * (t - 0.85)
    R = lambda z, lift=0.0, heel=0.0: (2.2, z, lift, 55, heel)
    Lf = lambda z, lift=0.0, heel=0.0: (2.0, z, lift, 20, heel)
    hand = lambda dz: _add(READY['hand_at'], (0, 0, dz))
    bow = lambda dz: _add(READY['bow_at'], (0, 0, dz))
    return seq(READY, [
        (0.00, 'back foot lifts', dict(root=(-0.3, -1.5, 0.2), body=(3, 19, 1), hand_at=hand(-0.6), bow_at=bow(0.4),
                                       r_place=R(back(1.2), 0, 10), l_place=Lf(lead(0.0)))),
        (0.15, 'reach back', dict(root=(-0.4, -1.3, 0.5), body=(2, 20, 2), hand_at=hand(0.2), bow_at=bow(-0.2),
                                  r_place=R(2.2, 1.0, 0), l_place=Lf(lead(0.15)))),
        (0.30, 'toe down', dict(root=(-0.2, -1.7, 0.9), body=(3, 19, 1), hand_at=hand(0.7), bow_at=bow(-0.6),
                                r_place=R(back(0.30), 0, -8), l_place=Lf(lead(0.30)))),
        (0.42, 'weight back', dict(root=(0.1, -1.85, 0.7), body=(4, 18, -1), hand_at=hand(0.5), bow_at=bow(-0.4),
                                   r_place=R(back(0.42)), l_place=Lf(lead(0.42), 0, 6))),
        (0.60, 'lead foot lifts', dict(root=(0.4, -1.55, 0.4), body=(3, 17, -2), hand_at=hand(-0.2), bow_at=bow(0.2),
                                       r_place=R(back(0.60)), l_place=Lf(lead(0.60), 0, 14))),
        (0.72, 'drag', dict(root=(0.45, -1.35, 0.2), body=(2, 17, -2), hand_at=hand(-0.6), bow_at=bow(0.5),
                            r_place=R(back(0.72)), l_place=Lf(-2.6, 0.8, 4))),
        (0.85, 'lead foot plants', dict(root=(0.1, -1.7, 0.1), body=(3, 18, 0), hand_at=hand(-0.7), bow_at=bow(0.6),
                                        r_place=R(back(0.85)), l_place=Lf(lead(0.85)))),
        (1.00, 'settle', dict(root=(-0.2, -1.55, 0.1), body=(3, 18, 1), hand_at=hand(-0.7), bow_at=bow(0.5),
                              r_place=R(back(1.00)), l_place=Lf(lead(1.00)))),
        (1.20, 'back foot lifts', dict(root=(-0.3, -1.5, 0.2), body=(3, 19, 1), hand_at=hand(-0.6), bow_at=bow(0.4),
                                       r_place=R(back(1.2), 0, 10), l_place=Lf(lead(1.2)))),
    ])


def kite():
    return build('archer_kite', 1.2, True, timeline('archer_kite'), ground_speed=KITE_SPEED)


def hit_timeline():
    """0.9 s: struck, the head snaps back and both arms fly wide, a stumbling step back, a crouch to catch it, back up
    to ready."""
    return seq(READY, [
        (0.00, 'ready', {}),
        (0.07, 'struck', dict(root=(0, -1.0, 0.9), body=(-5, 24, 4), chest=(-12, 12, 6), look=(-18, 14, 10),
                              bow_at=(-9.0, 14.5, 1.0), aim=(-35, -45, 35), hand_at=(8.0, 16.0, 2.0),
                              l_pole=(-1, 0.2, 0.2), r_pole=(1, 0.2, 0.4))),
        (0.14, 'reel', dict(root=(0, -1.2, 1.3), body=(-6, 25, 5), chest=(-10, 14, 6), look=(-14, 18, 8),
                            bow_at=(-9.5, 14.0, 1.5), hand_at=(8.5, 16.5, 2.5), r_place=(2.4, 4.4, 1.0, 55, 0))),
        (0.24, 'catch', dict(root=(0, -1.6, 1.4), body=(-1, 22, 2), chest=(-2, 8, 2), look=(-4, 8, 3),
                             bow_at=(-8.0, 12.5, 0.0), hand_at=(7.5, 14.5, 1.5), r_place=(2.4, 5.0, 0, 55, 0))),
        (0.36, 'crouch', dict(root=(0, -1.9, 1.4), body=(6, 19, 0), chest=(8, 3, 0), look=(10, 0, 0),
                              bow_at=_add(READY['bow_at'], (-0.5, -0.5, -0.5)), aim=READY['aim'],
                              hand_at=_add(READY['hand_at'], (0.3, -0.3, -0.3)),
                              l_pole=READY['l_pole'], r_pole=READY['r_pole'], r_place=(2.4, 5.0, 0, 55, 0))),
        (0.50, 'gather', dict(root=(0, -1.5, 1.3), body=(4, 18, 0), chest=(4, 4, 0), look=(4, 0, 0),
                              bow_at=READY['bow_at'], hand_at=READY['hand_at'], r_place=(2.4, 5.0, 0, 55, 0))),
        (0.60, 'step up', dict(root=(0, -1.2, 1.0), r_place=(2.3, 4.4, 0.9, 55, 0))),
        (0.70, 'plant', dict(root=(0, -1.1, 0.2), r_place=STANCE['r_place'])),
        (0.80, 'settle', dict(root=(0, -0.95, -0.05), body=(3, 18, 0), chest=(2, 4, 0))),
        (0.90, 'ready', {k: READY[k] for k in ('root', 'body', 'chest', 'look')}),
    ])


def hit_react():
    return build('archer_hit', 0.9, False, timeline('archer_hit'))


CLIPS = [idle, kite, shoot, hit_react]
TIMELINES = {'archer_idle': idle_timeline, 'archer_kite': kite_timeline, 'archer_shoot': shoot_timeline,
             'archer_hit': hit_timeline}
LOOPS = {'archer_idle': 3.0, 'archer_kite': 1.2}


def timeline(name):
    """A clip's written timeline with the searched fixes (archer_poses.json, from pose_search.py) laid over it."""
    return PS.apply(TIMELINES[name](), PS.load(POSES_FILE).get(name, {}))


def still(pose):
    """A pose held still (for judging one beat)."""
    return build('still', 0.05, False, [(0, 'still', pose), (0.05, 'still', pose)])
