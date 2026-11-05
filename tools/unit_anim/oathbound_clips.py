"""The Oathbound knight's clips: `oath_guard`, an idle loop in a sword-and-shield guard, and `oath_combo`, the signature
attack: a wound-up overhead diagonal cut with a lunge, a rising backhand, then a shield bash, back to guard.

Poses are keyed in the game's convention (rig.py) and timed on the principles that make a swing read: anticipation (the
wind-up holds a beat and the body loads opposite to the cut), a fast strike (two to four frames at 20 ticks), overshoot
and settle on the follow-through, arcs (the sword travels around the shoulder, not in straight lines), the hips leading
the shoulders and the shoulders leading the blade (offset keys), and weight (the feet step into each strike and the root
drops into the lunge). The cape and the plume are not keyed: they are a spring simulation of the body's motion, baked.
The root's height is not keyed either: a solver keeps the lowest point of either sabaton on the ground.
"""
from .rig import Clip, ground_root_y, sample, spring_follow

STEP = 0.05   # baked keys every game tick


GUARD = {
    'body': (6, 18, 0),           # leaning into it, turned so the shield shoulder leads
    'head': (-2, -16, 0),         # eyes on the target straight ahead
    'right_arm': (-38, 16, 12),   # sword arm low and back, blade forward
    'right_hand': (-32, 0, 0),    # the point up, at the enemy's throat
    'left_arm': (-48, 14, -4),    # the shield up across the chest
    'right_leg': (14, 0, 2),      # back foot
    'left_leg': (-16, 0, -2),     # lead foot
    'root_pos': (0, 0, 0),
}


def pose(**kw):
    p = dict(GUARD)
    p.update(kw)
    return p


# (time, label, pose). Labels name the beats on the review sheets.
COMBO = [
    (0.00, 'guard', GUARD),
    (0.10, 'load', pose(body=(2, 26, 0), head=(-4, -22, 0), right_arm=(-100, 24, 22), right_hand=(-30, 0, 0),
                        left_arm=(-60, 16, -6), right_leg=(17, 0, 2), left_leg=(-18, 0, -2), root_pos=(0, 0, 0.6))),
    (0.20, 'wind-up', pose(body=(-5, 34, 0), head=(-6, -26, 0), right_arm=(-168, 32, 26), right_hand=(-48, 0, 0),
                           left_arm=(-74, 20, -8), right_leg=(19, 0, 2), left_leg=(-20, 0, -2), root_pos=(0, 0, 0.9))),
    (0.25, 'wind-up hold', pose(body=(-6, 35, 0), head=(-6, -27, 0), right_arm=(-172, 33, 27), right_hand=(-50, 0, 0),
                                left_arm=(-75, 20, -8), right_leg=(19, 0, 2), left_leg=(-20, 0, -2), root_pos=(0, 0, 1.0))),
    (0.30, 'cut', pose(body=(10, 10, 0), head=(0, -8, 0), right_arm=(-130, -5, 12), right_hand=(5, 0, 0),
                       left_arm=(-42, 4, -14), right_leg=(-6, 0, 2), left_leg=(-4, 0, -2), root_pos=(0, 0, -0.6))),
    (0.35, 'contact', pose(body=(18, -18, 0), head=(6, 10, 0), right_arm=(-72, -34, -8), right_hand=(52, 0, 0),
                           left_arm=(-26, -14, -18), right_leg=(-30, 0, 3), left_leg=(22, 0, -2), root_pos=(0, 0, -1.8))),
    (0.44, 'follow-through', pose(body=(22, -30, 0), head=(8, 18, 0), right_arm=(-26, -54, -20), right_hand=(72, 0, 0),
                                  left_arm=(-20, -24, -22), right_leg=(-33, 0, 3), left_leg=(24, 0, -2),
                                  root_pos=(0, 0, -2.1))),
    (0.56, 'settle', pose(body=(16, -24, 0), head=(4, 14, 0), right_arm=(-34, -46, -16), right_hand=(46, 0, 0),
                          left_arm=(-34, -10, -16), right_leg=(-28, 0, 3), left_leg=(20, 0, -2), root_pos=(0, 0, -1.9))),
    (0.70, 'gather', pose(body=(14, -30, 0), head=(4, 18, 0), right_arm=(-14, -64, -28), right_hand=(24, 0, 0),
                          left_arm=(-44, -6, -12), right_leg=(-24, 0, 3), left_leg=(18, 0, -2), root_pos=(0, 0, -1.7))),
    (0.80, 'rising cut', pose(body=(6, 4, 0), head=(0, -2, 0), right_arm=(-96, -8, 10), right_hand=(-18, 0, 0),
                              left_arm=(-50, 6, -10), right_leg=(-14, 0, 2), left_leg=(6, 0, -2), root_pos=(0, 0, -1.6))),
    (0.86, 'rising contact', pose(body=(2, 24, 0), head=(-4, -16, 0), right_arm=(-138, 30, 30), right_hand=(-44, 0, 0),
                                  left_arm=(-54, 14, -8), right_leg=(-6, 0, 2), left_leg=(-2, 0, -2),
                                  root_pos=(0, 0, -1.5))),
    (0.97, 'high guard', pose(body=(0, 32, 0), head=(-6, -22, 0), right_arm=(-150, 40, 34), right_hand=(-56, 0, 0),
                              left_arm=(-56, 18, -8), right_leg=(4, 0, 2), left_leg=(-8, 0, -2), root_pos=(0, 0, -1.4))),
    (1.10, 'bash load', pose(body=(2, 40, 0), head=(-4, -30, 0), right_arm=(-146, 42, 34), right_hand=(-56, 0, 0),
                             left_arm=(-22, -26, -16), right_leg=(14, 0, 2), left_leg=(-14, 0, -2),
                             root_pos=(0, 0, -0.6))),
    (1.20, 'bash', pose(body=(16, -6, 0), head=(6, 4, 0), right_arm=(-90, 30, 30), right_hand=(-30, 0, 0),
                        left_arm=(-92, 6, -4), right_leg=(22, 0, 2), left_leg=(-34, 0, -2), root_pos=(0, 0, -2.6))),
    (1.30, 'bash hold', pose(body=(14, -2, 0), head=(4, 2, 0), right_arm=(-80, 28, 28), right_hand=(-30, 0, 0),
                             left_arm=(-86, 4, -4), right_leg=(22, 0, 2), left_leg=(-32, 0, -2), root_pos=(0, 0, -2.4))),
    (1.55, 'recover', pose(body=(8, 14, 0), head=(-1, -12, 0), right_arm=(-46, 18, 14), right_hand=(-28, 0, 0),
                           left_arm=(-52, 12, -4), right_leg=(16, 0, 2), left_leg=(-18, 0, -2), root_pos=(0, 0, -0.4))),
    (1.90, 'guard', GUARD),
]
COMBO_LENGTH = 1.90

BEATS = [(t, label) for t, label, _ in COMBO]


def keyed(clip, timeline):
    """Writes a timeline of whole poses as per-bone keys (every bone keyed at every pose, so the hips, shoulders and blade
    stay in step unless a pose offsets them on purpose)."""
    bones = sorted({b for _, _, p in timeline for b in p if b != 'root_pos'})
    for b in bones:
        clip.rot[b] = [(t,) + tuple(float(v) for v in p.get(b, GUARD.get(b, (0, 0, 0)))) for t, _, p in timeline]
    clip.pos['root'] = [(t,) + tuple(float(v) for v in p.get('root_pos', (0, 0, 0))) for t, _, p in timeline]


def shield_hand(clip):
    """The shield's hand turns against its arm's pitch so the board faces the enemy wherever the arm is (keys at the
    arm's own times)."""
    clip.rot['left_hand'] = [(t, -0.85 * p, 0.0, 0.0) for t, p, y, r in clip.rot['left_arm']]


def bake_ground(clip):
    """Root keys every tick: x and z as keyed, y from the ground solver (the sabatons stay on the floor)."""
    n = int(round(clip.length / STEP))
    keys = []
    for i in range(n + 1):
        t = round(i * STEP, 4)
        rot, pos = clip.at(t)
        x, _, z = pos.get('root', (0, 0, 0))
        keys.append((t, float(x), float(ground_root_y(rot, {'root': (x, 0, z)})), float(z)))
    clip.pos['root'] = keys


def cape_clamp(clip):
    """The cape may not swing into the body, nor through a leg swung back under it."""
    def run(t, pitch, roll):
        rot, _ = clip.at(t)
        body = rot.get('body', (0, 0, 0))[0]
        back = max(0.0, rot.get('right_leg', (0, 0, 0))[0], rot.get('left_leg', (0, 0, 0))[0])
        lo = max(2.0, 0.8 * back - body + 3.0)
        return max(pitch, lo), max(-35.0, min(35.0, roll))
    return run


def combo():
    c = Clip('oath_combo', COMBO_LENGTH, loop=False)
    keyed(c, COMBO)
    shield_hand(c)
    bake_ground(c)
    spring_follow(c, 'cape', 'body', (1.0, 1.0), stiffness=70, damping=9, rest=(4, 0), clamp=cape_clamp(c))
    spring_follow(c, 'plume', 'head', (0.6, 0.6), stiffness=140, damping=10, rest=(0, 0))
    return c


def guard():
    """A 2.4 s breathing loop in the guard: the chest rises on the in-breath (the body pitches back a degree and a half),
    the sword's point drifts in a small circle, the shield settles, the head checks left then right."""
    import math
    L, n = 2.4, 12
    c = Clip('oath_guard', L, loop=True)
    tl = []
    for i in range(n + 1):
        t = round(L * i / n, 4)
        a = 2 * math.pi * i / n
        g = dict(GUARD)
        g['body'] = (GUARD['body'][0] - 1.5 * math.sin(a), GUARD['body'][1] + 1.0 * math.sin(a + 0.6), 0)
        g['head'] = (GUARD['head'][0] + 1.0 * math.sin(a + 1.0), GUARD['head'][1] + 3.0 * math.sin(a / 1 + 2.2), 0)
        g['right_arm'] = (GUARD['right_arm'][0] + 2.5 * math.sin(a), GUARD['right_arm'][1] + 2.0 * math.cos(a),
                          GUARD['right_arm'][2])
        g['right_hand'] = (GUARD['right_hand'][0] + 3.0 * math.sin(a + 0.8), 0, 0)
        g['left_arm'] = (GUARD['left_arm'][0] + 1.5 * math.sin(a + 0.4), GUARD['left_arm'][1], GUARD['left_arm'][2])
        tl.append((t, 'breath', g))
    keyed(c, tl)
    shield_hand(c)
    bake_ground(c)
    spring_follow(c, 'cape', 'body', (1.0, 1.0), stiffness=70, damping=9, rest=(4, 0), clamp=cape_clamp(c), passes=3)
    spring_follow(c, 'plume', 'head', (0.6, 0.6), stiffness=140, damping=10, rest=(0, 0), passes=3)
    return c
