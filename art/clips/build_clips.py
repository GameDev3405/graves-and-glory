"""Hand-keyed clips for the biped rig (animation rework Task 16): writes art/clips/biped/sword_shield.bbmodel,
art/clips/biped/common.bbmodel and art/clips/biped/bow.bbmodel (the interim bow set, R110), the Blockbench projects
tools/clips.py converts to the clip JSON the game loads.

Every key below is authored by hand: a time, a bone and its channels, in the rig's convention (y down, -z forward,
degrees added to the rest rotation in ZYX order). Nothing is a wave or a formula of time. The script only does the
bookkeeping a Blockbench session would: it writes the keys as keyframes, delays a bone's inner keys where a key says so
(overlap), and for a death sets the root's height so the lowest corner of the posed rig lies on the ground and its
across-the-ground offset so the body turns about the named contact point (what an animator does by eye with the root).

  python art/clips/build_clips.py            write the projects (then run python tools/clips.py)
  python art/clips/build_clips.py --check    rebuild in memory and compare with the committed projects

Stances are absolute offsets from rest and loop; attacks, hits and blocks are offsets on the stance and start and end
at zero; deaths are absolute from rest and end on the ground. The shield arm of every sword_shield stance splits the
45 degree carry between shoulder and elbow as a pure pitch, so the board keeps the carry's facing (SHIELD_ROT unchanged).
"""
import json
import math
import pathlib
import sys
import uuid

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import clips as C  # noqa: E402
from bbmodel import mat_vec, rotation  # noqa: E402

RIG = json.loads((ROOT / 'art/rigs/biped.json').read_text(encoding='utf-8'))
PIVOT = {}
for _b in RIG['bones']:
    _p = PIVOT.get(_b['parent'], (0, 0, 0)) if _b['parent'] else (0, 0, 0)
    PIVOT[_b['name']] = tuple(a + b for a, b in zip(_p, _b['pivot']))
GROUND = max(y for _, y in C.points(RIG, {}))
NS = uuid.UUID('6f1d5a52-3c1e-4b8e-9a51-16a7c11b0000')


def gid(*parts):
    return str(uuid.uuid5(NS, '/'.join(parts)))


def six(v):
    v = list(v)
    return [float(x) for x in v + [0.0] * (6 - len(v))]


class Anim:
    """One animation: per-bone keys {bone: [(t, six, interp)]}, markers and per-bone overlap delays."""

    def __init__(self, name, length, loop='once', **markers):
        self.name, self.length, self.loop = name, length, loop
        self.markers = {m: t for m, t in markers.items()}
        self.tracks = {}
        self.lags = {}

    def k(self, t, i='catmullrom', **bones):
        for b, v in bones.items():
            assert b in PIVOT, b
            track = self.tracks.setdefault(b, [])
            track[:] = [x for x in track if abs(x[0] - t) > 1e-9]
            track.append((t, six(v), i))
            track.sort(key=lambda x: x[0])
        return self

    def pose(self, t, pose, i='catmullrom'):
        return self.k(t, i, **pose)

    def lag(self, **bones):
        self.lags.update(bones)
        return self

    def keys(self):
        """{bone: [(t, six, interp)]} with the overlap delays applied to inner keys, kept in order and inside the clip."""
        out = {}
        for b, track in self.tracks.items():
            d = self.lags.get(b, 0)
            keys = []
            for n, (t, v, i) in enumerate(track):
                if d and 0 < n < len(track) - 1:
                    lo = track[n - 1][0] + 0.02
                    hi = track[n + 1][0] - 0.02
                    t = min(max(t + d, lo), hi)
                keys.append((round(t, 4), v, i))
            out[b] = keys
        return out

    def sample(self, t):
        return C.sample(C.parse(self.clip_json()), t, wrap=False)

    def clip_json(self):
        bones = {}
        for b, keys in sorted(self.keys().items()):
            ch = {}
            if any(any(abs(x) > 1e-9 for x in v[:3]) for _, v, _ in keys):
                ch['rotation'] = [{'t': t, 'i': i, 'v': [round(x, 3) for x in v[:3]]} for t, v, i in keys]
            if any(any(abs(x) > 1e-9 for x in v[3:]) for _, v, _ in keys):
                ch['position'] = [{'t': t, 'i': i, 'v': [round(x, 3) for x in v[3:]]} for t, v, i in keys]
            if not ch:
                ch['rotation'] = [{'t': t, 'i': i, 'v': [0.0, 0.0, 0.0]} for t, v, i in keys]
            bones[b] = ch
        return {'format': C.FORMAT, 'clip': self.name, 'length': self.length, 'loop': self.loop == 'loop',
                'markers': dict(self.markers), 'bones': bones}

    def animation(self, family):
        animators = {}
        cj = self.clip_json()
        for b, chans in cj['bones'].items():
            frames = []
            for ch, keys in chans.items():
                for k in keys:
                    frames.append({'channel': ch, 'data_points': [dict(zip('xyz', k['v']))],
                                   'uuid': gid(family, self.name, b, ch, str(k['t'])), 'time': k['t'], 'color': -1,
                                   'interpolation': k['i']})
            animators[gid('group', b)] = {'name': b, 'type': 'bone', 'keyframes': frames}
        if self.markers:
            animators['effects'] = {'name': 'Effects', 'type': 'effect', 'keyframes': [
                {'channel': 'timeline', 'data_points': [{'script': m}], 'uuid': gid(family, self.name, 'marker', m),
                 'time': t, 'color': -1, 'interpolation': 'linear'} for m, t in sorted(self.markers.items())]}
        return {'uuid': gid(family, 'anim', self.name), 'name': self.name, 'loop': self.loop, 'override': False,
                'length': self.length, 'snapping': 20, 'selected': False, 'anim_time_update': '', 'blend_weight': '',
                'start_delay': '', 'loop_delay': '', 'animators': animators}


# ---- grounding for deaths

def world(pose):
    """{bone: (rotation matrix, origin)} of the rig posed by {bone: six}."""
    out = {}
    for b in RIG['bones']:
        ch = pose.get(b['name'], [0.0] * 6)
        rest = b.get('rotation', [0, 0, 0])
        r = rotation([rest[i] + ch[i] for i in range(3)])
        o = [b['pivot'][i] + ch[3 + i] for i in range(3)]
        if b['parent']:
            pr, po = out[b['parent']]
            from bbmodel import mat_mul
            r, o = mat_mul(pr, r), [a + c for a, c in zip(po, mat_vec(pr, o))]
        out[b['name']] = (r, o)
    return out


def lowest(pose):
    """The lowest y of the posed rig's pivots and corners, less the bones the client hides in a death (C.HIDDEN_IN_DEATH:
    the illager fold, which otherwise held a face-down corpse 4 px off the ground on a box nobody sees)."""
    return max(y for b, y in C.points(RIG, pose) if b not in C.HIDDEN_IN_DEATH)


def grounded(pose, about=None, lift=0.0):
    """The pose with its root turned about the rig-space point `about` (the body's contact with the ground: the root's
    x and z offsets are set so that point stays put) and its root height set so the lowest corner touches the ground
    (less `lift`, a hair above it)."""
    pose = {b: six(v) for b, v in pose.items()}
    root = pose.setdefault('root', six(()))
    if about is not None:
        r = rotation(root[:3])
        p = mat_vec(r, about)
        root[3] = about[0] - p[0]
        root[5] = about[2] - p[2]
    root[4] = 0.0
    root[4] = GROUND - lift - lowest(pose)
    return pose


# ---- poses (rig channels; see the module doc)

Z = (0, 0, 0)
# Shield arm, carried: the old 45 degree shoulder pitch split 32 / 13 between shoulder and elbow, a pure pitch, so the
# forearm (and the board on it) keeps the carry's -45 and SHIELD_ROT still squares it (evidence in the Task 16 report).
L_HOLD = dict(left_arm=(-32, 0, 0), left_forearm=(-13, 0, 0))


def pose(*parts, **bones):
    out = {}
    for p in parts:
        out.update(p)
    out.update(bones)
    return {b: six(v) for b, v in out.items()}


# Sword arm stances. The hand turns 180 degrees in yaw in every stance so the arming sword's 45 degree grip points the
# blade forward from a raised fist; attacks are offsets in that frame.
# The guard, shield, wall and brace stances split the shield arm's pitch so the elbow bends 30 degrees or more (R98's
# 45 degree bends): the pitch moved from shoulder to elbow is pure, so the forearm and the board keep their facing to a
# hair, and the grip comes about 1 px back and down. The guard's root drop of 0.6 makes the plant bend both knees past
# 30 degrees (wall and brace already bend them 55 to 65).
# Wall and brace cock the sword up and back at the shoulder (Task 16 fix 3): the blade stands 63 to 72 degrees up and leans
# back, its point 6 to 8 px right of the spine and behind the neck, so no blade crosses a neighbour's board in a wall.
IDLE = pose(L_HOLD, body=(1, 3, 0), right_arm=(-20, 0, 3), right_forearm=(-10, 0, 0), right_hand=(135, 180, 0),
            right_leg=(0, 0, 3), left_leg=(0, 0, -3))
CARRY = pose(L_HOLD, body=(3, 4, 0), right_arm=(-50, 0, 2), right_forearm=(-128, 0, 0), right_hand=(178, 180, 0))
GUARD = pose(body=(3, 12, 0), head=(-2, -9, 0), right_arm=(-65, 0, 20), right_forearm=(-110, 0, 0),
             right_hand=(90, 180, 0), left_arm=(-22, 18, 0), left_forearm=(-32, 0, 0),
             right_leg=(6, 0, 4), right_shin=Z, left_leg=(-8, 0, -3), left_shin=(8, 0, 0), root=(0, 0, 0, 0, 0.6, 0))
SHIELD = pose(body=(4, 18, 0), head=(-2, -14, 0), right_arm=(-55, 0, 40), right_forearm=(-110, 0, 0),
              right_hand=(90, 180, 0), left_arm=(-28, 34, 0), left_forearm=(-30, 0, 0), root=(0, 0, 0, 0, 0.2, 0))
WALL = pose(body=(5, 20, 0), head=(-4, -16, 0), right_arm=(-110, 0, 0), right_forearm=(-70, 0, 0),
            right_hand=(160, 200, 0), left_arm=(-30, 30, 0), left_forearm=(-30, 0, 0),
            right_leg=(8, 0, 6), right_shin=(2, 0, 0), left_leg=(-12, 0, -5), left_shin=(12, 0, 0),
            root=(0, 0, 0, 0, 1.4, 0))
BRACE = pose(body=(8, 16, 0), head=(-6, -13, 0), right_arm=(-110, 0, 10), right_forearm=(-60, 0, 0),
             right_hand=(180, 200, 0), left_arm=(-24, 26, 0), left_forearm=(-32, 0, 0),
             right_leg=(12, 0, 7), right_shin=(2, 0, 0), left_leg=(-14, 0, -6), left_shin=(14, 0, 0),
             root=(0, 0, 0, 0, 1.8, 0))


def add(base, **delta):
    """base plus offsets on some bones."""
    out = {b: list(v) for b, v in base.items()}
    for b, d in delta.items():
        v = out.setdefault(b, six(()))
        out[b] = [a + c for a, c in zip(v, six(d))]
    return out


def stance(name, length, base, beats):
    """A looping stance: the base pose at 0 and at the end, and hand-set beats [(t, {bone: offset})] in between."""
    a = Anim(name, length, 'loop')
    a.pose(0, base)
    for t, delta in beats:
        a.pose(t, add(base, **delta))
    a.pose(length, base)
    return a


def stances():
    out = []
    # Idle: the sword's point rests low ahead; once a loop the wrist rolls the blade over and back and the shield arm
    # resettles a little later (the life layer already breathes and shifts the weight).
    out.append(stance('idle', 4.0, IDLE, [
        (1.3, dict(right_arm=(-2, 0, 1), right_hand=(6, 0, 5), body=(0.5, 1, 0.4))),
        (2.1, dict(right_arm=(-1, 0, 0.5), right_hand=(3, 0, 2), left_arm=(-1.5, 0, -1), left_forearm=(1, 0, 0))),
        (3.0, dict(right_arm=(1, 0, -0.5), right_hand=(-3, 0, -2), left_arm=(0.5, 0, -0.5), body=(-0.3, -1, -0.3))),
    ]).lag(right_hand=0.12, left_forearm=0.1))
    # March carry: blade sloped back over the right shoulder, shield slung; the sword settles twice a loop (a step each).
    out.append(stance('carry', 2.0, CARRY, [
        (0.5, dict(right_arm=(-3, 0, 0), right_hand=(4, 0, 0), left_arm=(2, 0, 0))),
        (1.0, dict(right_arm=(0.5, 0, 0), right_hand=(-1, 0, 0))),
        (1.5, dict(right_arm=(-3, 0, 0), right_hand=(4, 0, 0), left_arm=(-1.5, 0, 0))),
    ]).lag(right_hand=0.08, left_forearm=0.06))
    # Ready guard: point up at the enemy's face over the shield's edge, weight forward; the point stays alive.
    out.append(stance('guard', 2.4, GUARD, [
        (0.7, dict(right_arm=(-3, 0, 2), right_hand=(-6, 0, 0), body=(0, 1.5, 0))),
        (1.4, dict(right_arm=(1, 0, -1), right_hand=(4, 0, 3), left_arm=(-2, 1, 0))),
        (1.9, dict(right_arm=(-1, 0, 0.5), right_hand=(-2, 0, -1), body=(0.5, -1, 0))),
    ]).lag(right_hand=0.1, left_forearm=0.08))
    # Shield up (hold or shuffle): board square in front, body turned behind it, sword ready over the rim.
    out.append(stance('shield', 2.0, SHIELD, [
        (0.6, dict(left_arm=(-1.5, 1, 0), right_arm=(-2, 0, 1), right_hand=(-4, 0, 0))),
        (1.3, dict(left_arm=(0.5, -1, 0), right_arm=(1, 0, -1), right_hand=(3, 0, 0), body=(0.5, 1, 0))),
    ]).lag(right_hand=0.08))
    # Wall: low behind the board, its edges over the neighbours', sword cocked at the shoulder behind the shield's edge.
    out.append(stance('wall', 3.0, WALL, [
        (0.9, dict(left_arm=(-1, 2, 0), body=(1, 1, 0), right_arm=(-2, 0, 0), right_hand=(-3, 0, 0))),
        (1.8, dict(left_arm=(1, -1, 0), body=(-0.5, -0.5, 0), right_arm=(1.5, 0, 0), root=(0, 0, 0, 0, 0.2, 0))),
        (2.4, dict(left_arm=(0, 1, 0), right_hand=(2, 0, 0))),
    ]).lag(right_hand=0.1, left_forearm=0.1))
    # Brace: set to take a charge, the shield low and forward, all the weight on the back leg.
    out.append(stance('brace', 3.0, BRACE, [
        (1.0, dict(left_arm=(-1.5, 1, 0), body=(1, 0, 0), root=(0, 0, 0, 0, 0.2, 0))),
        (2.0, dict(left_arm=(1, -1, 0), body=(-0.5, 1, 0), right_hand=(-3, 0, 0))),
    ]).lag(left_forearm=0.1, right_hand=0.1))
    return out


# ---- attacks (offsets on the combat stance; zero at both ends)

def attacks():
    out = []
    # Thrust over the shield (impact tick 7): coil the sword shoulder back, the shield turning in to cover; drive off the
    # back foot, the shoulder, elbow and wrist unfolding in turn. As the body turns away behind the lunge the shield arm
    # swings across the chest against that turn, so the board stays square to the front (never further off it than the
    # guard's own 30 degrees) and low enough for the arm to reach over its rim; a short overreach, then the arm reels the
    # point back and the shield closes. 25 ticks (R97): the whole clip fits the 25-tick cooldown, so a swing every
    # cooldown never cuts the settle; mo-late-join's catch-up at impact + 12 ticks still leaves it 6 ticks to compare.
    a = Anim('sword_shield_thrust', 1.25, impact=0.35)
    a.k(0.0, root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)
    a.k(0.15, root=(0, 0, 0, 0, 0.3, 1.0), body=(-2, 14, 0), head=(0, -9, 0), right_arm=(24, 0, 6),
        right_forearm=(-12, 0, 0), right_hand=(-10, 0, 0), left_arm=(-5, 9, 0), left_forearm=(-3, 0, 0))
    a.k(0.29, i='linear', root=(0, 0, 0, 0, 0.5, -1.0), body=(3, -12, 0), head=(-2, 8, 0), right_arm=(-18, 0, -14),
        right_forearm=(62, 0, 0), right_hand=(18, 0, 0), left_arm=(2, 30, 0), left_forearm=(-5, 0, 0))
    a.k(0.35, i='linear', root=(0, 0, 0, 0, 0.7, -2.0), body=(5, -27, 0), head=(-4, 19, 0), right_arm=(-32, 0, -27),
        right_forearm=(104, 0, 0), right_hand=(34, 0, 0), left_arm=(-1, 30, 0), left_forearm=(-5, 0, 0))
    a.k(0.45, root=(0, 0, 0, 0, 0.75, -2.3), body=(6, -30, 0), head=(-4, 21, 0), right_arm=(-35, 0, -30),
        right_forearm=(108, 0, 0), right_hand=(40, 0, 0), left_arm=(5, 34, 0), left_forearm=(-10, 0, 0))
    a.k(0.68, root=(0, 0, 0, 0, 0.3, -0.7), body=(1, -8, 0), head=(-1, 6, 0), right_arm=(-6, 0, -8),
        right_forearm=(30, 0, 0), right_hand=(10, 0, 0), left_arm=(0, 14, 0), left_forearm=(2, 0, 0))
    a.k(0.98, root=(0, 0, 0, 0, -0.05, 0.15), body=(-0.6, 2, 0), head=(0, -1, 0), right_arm=(2, 0, 1.5),
        right_forearm=(-4, 0, 0), right_hand=(-3, 0, 0), left_arm=(-1, 0, 0), left_forearm=(0, 0, 0))
    a.k(1.25, root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)
    a.lag(right_hand=0.03, left_forearm=0.04, head=0.02)
    out.append(a)

    # Overhead cut (impact tick 10): a small sink, then the sword climbs over the head with the shield lifting to cover
    # the opening; a held beat at the top, then the cut falls through the body's turn and drop; the blade follows through
    # low, the body hangs over its front foot and climbs back.
    a = Anim('sword_shield_overhead', 1.2, impact=0.5)
    a.k(0.0, root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)
    a.k(0.12, root=(0, 0, 0, 0, 0.6, 0.2), body=(4, 4, 0), head=(2, -2, 0), right_arm=(12, 0, 2), right_forearm=(-6, 0, 0),
        right_hand=(-6, 0, 0))
    a.k(0.32, root=(0, 0, 0, 0, -0.2, 0.8), body=(-8, 8, 0), head=(-6, -6, 0), right_arm=(-98, 0, -30),
        right_forearm=(84, 0, 0), right_hand=(88, 0, 0), left_arm=(-8, 7, 0), left_forearm=(-4, 0, 0))
    a.k(0.41, i='linear', root=(0, 0, 0, 0, -0.25, 0.9), body=(-9, 9, 0), head=(-7, -7, 0), right_arm=(-104, 0, -31),
        right_forearm=(88, 0, 0), right_hand=(94, 0, 0))
    a.k(0.5, i='linear', root=(0, 0, 0, 0, 1.2, -1.8), body=(13, -22, 0), head=(6, 15, 0), right_arm=(-8, 0, -12),
        right_forearm=(84, 0, 0), right_hand=(18, 0, 0), left_arm=(10, -8, 0), left_forearm=(6, 0, 0))
    a.k(0.6, root=(0, 0, 0, 0, 1.45, -2.0), body=(16, -26, 0), head=(7, 18, 0), right_arm=(5, 0, -14),
        right_forearm=(78, 0, 0), right_hand=(8, 0, 0), left_arm=(12, -9, 0))
    a.k(0.82, root=(0, 0, 0, 0, 0.5, -0.6), body=(5, -8, 0), head=(2, 6, 0), right_arm=(2, 0, -4), right_forearm=(26, 0, 0),
        right_hand=(6, 0, 0), left_arm=(3, -2, 0), left_forearm=(1, 0, 0))
    a.k(1.02, root=(0, 0, 0, 0, -0.08, 0.1), body=(-1, 1.5, 0), head=(-0.5, -1, 0), right_arm=(-1.5, 0, 1),
        right_forearm=(-3, 0, 0), right_hand=(-2, 0, 0), left_arm=(-0.5, 0.5, 0))
    a.k(1.2, root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)
    a.lag(right_hand=0.03, left_forearm=0.05, head=0.025)
    out.append(a)

    # Low cut at the legs (impact tick 8): the sword drops back past the hip as the body coils behind the shield, then
    # the body unwinds and sinks and the blade sweeps across low; the shield swings open with the turn and is pulled
    # back across as the body rises.
    a = Anim('sword_shield_low_cut', 1.1, impact=0.4)
    a.k(0.0, root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)
    a.k(0.16, root=(0, 0, 0, 0, 0.4, 0.6), body=(-1, 13, 0), head=(0, -9, 0), right_arm=(-30, 0, -38),
        right_forearm=(-6, 0, 0), right_hand=(48, 0, 0), left_arm=(-4, 6, 0), left_forearm=(-2, 0, 0))
    a.k(0.33, i='linear', root=(0, 0, 0, 0, 1.2, -0.4), body=(8, -8, 0), head=(4, 6, 0), right_arm=(-2, 0, -36),
        right_forearm=(52, 0, 0), right_hand=(56, 0, 0), left_arm=(6, -5, 0))
    a.k(0.4, i='linear', root=(0, 0, 0, 0, 1.8, -1.2), body=(15, -30, 0), head=(9, 19, 0), right_arm=(20, 0, -40),
        right_forearm=(100, 0, 0), right_hand=(60, 0, 0), left_arm=(16, -14, 0), left_forearm=(8, 0, 0))
    a.k(0.5, root=(0, 0, 0, 0, 2.0, -1.4), body=(16, -36, 0), head=(10, 23, 0), right_arm=(24, 0, -46),
        right_forearm=(104, 0, 0), right_hand=(62, 0, 0), left_arm=(17, -15, 0))
    a.k(0.72, root=(0, 0, 0, 0, 0.6, -0.4), body=(5, -10, 0), head=(3, 7, 0), right_arm=(6, 0, -12),
        right_forearm=(30, 0, 0), right_hand=(18, 0, 0), left_arm=(4, -4, 0), left_forearm=(2, 0, 0))
    a.k(0.92, root=(0, 0, 0, 0, -0.06, 0.1), body=(-1, 2, 0), head=(-0.5, -1.5, 0), right_arm=(-1, 0, 1),
        right_forearm=(-3, 0, 0), right_hand=(-2, 0, 0), left_arm=(-1, 1, 0))
    a.k(1.1, root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)
    a.lag(right_hand=0.03, left_forearm=0.05, head=0.02)
    out.append(a)
    return out


# ---- wall strikes (offsets on the wall and brace stances; zero at both ends)

def wall_arm(shoulder, elbow, wrist):
    """The sword arm's absolute shoulder (pitch, roll), elbow pitch and wrist (pitch, yaw[, roll]) as offsets on the wall,
    so a key can be set where the hand and blade should be and the wall's cocked arm subtracted."""
    return dict(right_arm=(shoulder[0] - WALL['right_arm'][0], 0, shoulder[1] - WALL['right_arm'][2]),
                right_forearm=(elbow - WALL['right_forearm'][0], 0, 0),
                right_hand=(wrist[0] - WALL['right_hand'][0], wrist[1] - WALL['right_hand'][1], wrist[2] if len(wrist) > 2 else 0))


WALL_ZERO = dict(root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z, right_hand=Z, left_arm=Z, left_forearm=Z)


def wall_attacks():
    """<id>_wall: each attack as a troop in a wall or brace throws it from the cocked sword (the engine plays these in those
    stances), on the same impact tick as the row's open-order clip. No blade or arm crosses the troop's own board."""
    out = []
    # Thrust over the rim (impact tick 7): the cocked sword draws back by the ear with its point turned forward, then the
    # arm drives it out over the board's top edge, the body turning in behind it while the shield arm turns against the
    # body so the board stays square; a short overreach, and the point climbs back to the cocked guard.
    a = Anim('sword_shield_thrust_wall', 1.25, impact=0.35)
    a.k(0.0, **WALL_ZERO)
    a.k(0.15, root=(0, 0, 0, 0, 0.2, 0.8), body=(-2, 12, 0), head=(0, -8, 0), left_arm=(0, -8, 0), left_forearm=(-2, 0, 0),
        **wall_arm((-141, -6), -35, (67, 217)))
    a.k(0.29, i='linear', root=(0, 0, 0, 0, 0.4, -1.0), body=(2, -10, 0), head=(-2, 8, 0), left_arm=(0, 12, 0),
        **wall_arm((-137, -3), -18, (78, 198)))
    a.k(0.35, i='linear', root=(0, 0, 0, 0, 0.6, -1.8), body=(4, -20, 0), head=(-3, 16, 0), left_arm=(0, 20, 0),
        left_forearm=(-3, 0, 0), **wall_arm((-133, 0), -5, (88, 181)))
    a.k(0.45, root=(0, 0, 0, 0, 0.65, -2.1), body=(5, -23, 0), head=(-3, 18, 0), left_arm=(1, 22, 0), left_forearm=(-4, 0, 0),
        **wall_arm((-130, 0), -2, (92, 181)))
    a.k(0.68, root=(0, 0, 0, 0, 0.3, -0.7), body=(1, -8, 0), head=(-1, 6, 0), left_arm=(0, 8, 0), left_forearm=(-1, 0, 0),
        **wall_arm((-122, 0), -38, (128, 192)))
    a.k(0.98, root=(0, 0, 0, 0, -0.05, 0.15), body=(-0.5, 2, 0), head=(0, -1, 0), left_arm=(-0.5, -1, 0),
        **wall_arm((-108, 0), -73, (163, 201)))
    a.k(1.25, **WALL_ZERO)
    a.lag(right_hand=0.03, left_forearm=0.04, head=0.02)
    out.append(a)
    # Overhead over the rim (impact tick 10): the blade rises from the cock to lie back over the shoulder, a held beat,
    # then it chops forward and down past the board's edge as the body drops behind it; it climbs back to the cock.
    a = Anim('sword_shield_overhead_wall', 1.2, impact=0.5)
    a.k(0.0, **WALL_ZERO)
    a.k(0.12, root=(0, 0, 0, 0, 0.4, 0.2), body=(2, 3, 0), head=(1, -2, 0), **wall_arm((-116, 0), -60, (172, 203)))
    a.k(0.32, root=(0, 0, 0, 0, -0.3, 0.8), body=(-6, 8, 0), head=(-5, -5, 0), left_arm=(-3, 4, 0),
        **wall_arm((-150, 0), -32, (218, 208)))
    a.k(0.41, i='linear', root=(0, 0, 0, 0, -0.35, 0.9), body=(-7, 9, 0), head=(-6, -6, 0), left_arm=(-3, 4, 0),
        **wall_arm((-152, 0), -28, (225, 209)))
    a.k(0.5, i='linear', root=(0, 0, 0, 0, 1.0, -1.6), body=(12, -18, 0), head=(5, 13, 0), left_arm=(-4, 18, 0),
        left_forearm=(2, 0, 0), **wall_arm((-143, 0), 1, (67, 181)))
    a.k(0.6, root=(0, 0, 0, 0, 1.2, -1.8), body=(14, -21, 0), head=(6, 15, 0), left_arm=(-4, 20, 0), left_forearm=(3, 0, 0),
        **wall_arm((-136, 0), 2, (55, 181)))
    a.k(0.82, root=(0, 0, 0, 0, 0.4, -0.5), body=(4, -6, 0), head=(2, 4, 0), left_arm=(-1, 6, 0), left_forearm=(1, 0, 0),
        **wall_arm((-124, 0), -40, (122, 194)))
    a.k(1.02, root=(0, 0, 0, 0, -0.06, 0.1), body=(-1, 1.5, 0), head=(-0.5, -1, 0), **wall_arm((-108, 0), -72, (164, 201)))
    a.k(1.2, **WALL_ZERO)
    a.lag(right_hand=0.03, left_forearm=0.05, head=0.025)
    out.append(a)

    # Low cut beside the board (impact tick 8): the body coils, then sinks deep as the blade falls forward and down
    # past the board's right edge at the legs; the shield arm rolls the board a little out of the arm's road (it stays
    # within 28 degrees of the front) and the sword climbs back to the cock.
    a = Anim('sword_shield_low_cut_wall', 1.1, impact=0.4)
    a.k(0.0, **WALL_ZERO)
    a.k(0.16, root=(0, 0, 0, 0, 0.4, 0.5), body=(0, 12, 0), head=(0, -8, 0), left_arm=(-2, -4, 0),
        **wall_arm((-122, 0), -78, (188, 206)))
    a.k(0.33, i='linear', root=(0, 0, 0, 0, 1.1, -0.4), body=(8, -3, 0), head=(4, 3, 0), left_arm=(4, -6, -12),
        left_forearm=(2, 0, 0), **wall_arm((-84, 0), -66, (135, 190)))
    a.k(0.4, i='linear', root=(0, 0, 0, 0, 1.7, -1.1), body=(14, -10, 0), head=(8, 7, 0), left_arm=(6, -10, -20),
        left_forearm=(4, 0, 0), **wall_arm((-43, 0), -77, (97, 173)))
    a.k(0.5, root=(0, 0, 0, 0, 1.8, -1.2), body=(15, -11, 0), head=(8, 8, 0), left_arm=(6, -10, -21), left_forearm=(4, 0, 0),
        **wall_arm((-40, 0), -70, (88, 173)))
    a.k(0.72, root=(0, 0, 0, 0, 0.6, -0.4), body=(5, -9, 0), head=(3, 6, 0), left_arm=(2, 12, 0), left_forearm=(1, 0, 0),
        **wall_arm((-80, 0), -82, (135, 192)))
    a.k(0.92, root=(0, 0, 0, 0, -0.06, 0.1), body=(-1, 2, 0), head=(-0.5, -1.5, 0), left_arm=(-1, 1, 0),
        **wall_arm((-107, 0), -72, (162, 201)))
    a.k(1.1, **WALL_ZERO)
    a.lag(right_hand=0.03, left_forearm=0.05, head=0.02)
    out.append(a)
    return out


def blocks():
    """Blocked blows (sword_shield only: they move the shield arm). The board takes the blow, the arm gives and the body
    rocks back over the back foot, then the arm punches the board back out."""
    out = []
    a = Anim('block', 0.5)
    a.k(0.0, root=Z, body=Z, head=Z, left_arm=Z, left_forearm=Z, right_arm=Z)
    a.k(0.06, i='linear', root=(0, 0, 0, 0, 0.2, 1.2), body=(-5, 6, 0), head=(-4, 0, 0), left_arm=(9, -6, 0),
        left_forearm=(6, 0, 0), right_arm=(3, 0, 2))
    a.k(0.18, root=(0, 0, 0, 0, 0.1, 0.4), body=(-1, 2, 0), head=(-1, 0, 0), left_arm=(-3, 2, 0), left_forearm=(-2, 0, 0),
        right_arm=(1, 0, 0.5))
    a.k(0.32, root=(0, 0, 0, 0, 0, -0.1), body=(0.5, -0.5, 0), left_arm=(0.8, -0.5, 0), left_forearm=(0.5, 0, 0))
    a.k(0.5, root=Z, body=Z, head=Z, left_arm=Z, left_forearm=Z, right_arm=Z)
    a.lag(left_forearm=0.02, right_arm=0.03)
    out.append(a)
    a = Anim('block_2', 0.6)
    a.k(0.0, root=Z, body=Z, head=Z, left_arm=Z, left_forearm=Z, right_arm=Z, right_forearm=Z)
    a.k(0.07, i='linear', root=(0, 0, 0, 0, 0.4, 1.8), body=(-7, 9, -3), head=(-5, 2, 2), left_arm=(13, -10, 4),
        left_forearm=(9, 0, 0), right_arm=(6, 0, 5), right_forearm=(-6, 0, 0))
    a.k(0.22, root=(0, 0, 0, 0, 0.3, 0.9), body=(-2, 3, -1), head=(-1, 1, 0), left_arm=(-4, 3, -1),
        left_forearm=(-3, 0, 0), right_arm=(2, 0, 2), right_forearm=(-2, 0, 0))
    a.k(0.4, root=(0, 0, 0, 0, 0, -0.15), body=(0.6, -0.5, 0.3), left_arm=(1, -0.6, 0), left_forearm=(0.5, 0, 0),
        right_arm=(-0.5, 0, 0))
    a.k(0.6, root=Z, body=Z, head=Z, left_arm=Z, left_forearm=Z, right_arm=Z, right_forearm=Z)
    a.lag(left_forearm=0.02, right_arm=0.04, right_forearm=0.05)
    out.append(a)
    return out


# ---- hits (common to every biped family; offsets; never the left arm, which a shield family holds)

def hits():
    """The direction and weight come from the engine's hit pulse (which way the zone is pushed, and how hard); these add
    the body's own answer: the arms thrown out for balance, the head trailing the torso, the recovery."""
    out = []
    zero = dict(root=Z, body=Z, head=Z, right_arm=Z, right_forearm=Z)

    def hit(name, length, peak, mid, settle, lag=None):
        a = Anim(name, length)
        a.k(0.0, **zero)
        a.k(peak[0], i='linear', **peak[1])
        a.k(mid[0], **mid[1])
        a.k(settle[0], **settle[1])
        a.k(length, **zero)
        a.lag(**(lag or dict(head=0.03, right_forearm=0.04)))
        out.append(a)

    hit('hit_body', 0.55, (0.05, dict(root=(0, 0, 0, 0, 0.4, 0.5), body=(9, 0, 0), head=(6, 0, 0), right_arm=(14, 0, 8),
                                      right_forearm=(-10, 0, 0))),
        (0.2, dict(root=(0, 0, 0, 0, 0.2, 0.2), body=(4, 0, 0), head=(2, 0, 0), right_arm=(5, 0, 3), right_forearm=(-4, 0, 0))),
        (0.38, dict(body=(-0.8, 0, 0), head=(-0.5, 0, 0), right_arm=(-1, 0, -0.5))))
    hit('hit_body_2', 0.6, (0.05, dict(root=(0, 0, 0, 0, 0.5, 0.6), body=(7, -7, 4), head=(5, 4, -2), right_arm=(10, 0, 14),
                                       right_forearm=(-14, 0, 0))),
        (0.22, dict(root=(0, 0, 0, 0, 0.25, 0.25), body=(3, -3, 1.5), head=(2, 2, -1), right_arm=(4, 0, 5),
                    right_forearm=(-5, 0, 0))),
        (0.42, dict(body=(-0.6, 0.8, -0.4), head=(-0.4, -0.5, 0), right_arm=(-1, 0, -1))))
    hit('hit_body_3', 0.65, (0.06, dict(root=(0, 0, 0, 0, 0.7, 0.4), body=(11, 5, -3), head=(8, -3, 2), right_arm=(18, 0, 6),
                                        right_forearm=(-18, 0, 0))),
        (0.24, dict(root=(0, 0, 0, 0, 0.4, 0.2), body=(5, 2, -1), head=(3, -1, 1), right_arm=(6, 0, 2),
                    right_forearm=(-6, 0, 0))),
        (0.46, dict(body=(-1, -0.5, 0.3), head=(-0.6, 0.4, 0), right_arm=(-1.5, 0, 0))))
    hit('hit_head', 0.5, (0.04, dict(root=(0, 0, 0, 0, 0.1, 0.3), body=(-4, 0, 2), head=(-16, 6, 4), right_arm=(-6, 0, 10),
                                     right_forearm=(-6, 0, 0))),
        (0.16, dict(body=(-1.5, 0, 0.8), head=(-6, 2, 1.5), right_arm=(-2, 0, 3), right_forearm=(-2, 0, 0))),
        (0.32, dict(body=(0.5, 0, -0.2), head=(1.5, -0.5, -0.5))), lag=dict(body=0.02, right_forearm=0.04))
    hit('hit_head_2', 0.5, (0.04, dict(root=(0, 0, 0, 0, 0.1, 0.2), body=(-3, -4, -2), head=(-12, -9, -6), right_arm=(-4, 0, 12),
                                       right_forearm=(-8, 0, 0))),
        (0.16, dict(body=(-1, -1.5, -0.8), head=(-4, -3, -2), right_arm=(-1, 0, 4), right_forearm=(-3, 0, 0))),
        (0.32, dict(body=(0.4, 0.5, 0.2), head=(1, 1, 0.5))), lag=dict(body=0.02, right_forearm=0.04))
    hit('hit_head_3', 0.55, (0.05, dict(root=(0, 0, 0, 0, 0.3, 0.6), body=(-7, 2, 0), head=(-19, 0, 0), right_arm=(-10, 0, 6),
                                        right_forearm=(-10, 0, 0))),
        (0.18, dict(root=(0, 0, 0, 0, 0.1, 0.2), body=(-2.5, 1, 0), head=(-7, 0, 0), right_arm=(-3, 0, 2),
                    right_forearm=(-3, 0, 0))),
        (0.36, dict(body=(0.8, 0, 0), head=(2, 0, 0))), lag=dict(body=0.02, right_forearm=0.04))
    hit('hit_legs', 0.6, (0.06, dict(root=(0, 0, 0, 0, 0.9, 0), body=(6, 0, 5), head=(-3, 0, -3), right_arm=(-8, 0, 14),
                                     right_forearm=(-6, 0, 0))),
        (0.24, dict(root=(0, 0, 0, 0, 0.45, 0), body=(2.5, 0, 2), head=(-1, 0, -1), right_arm=(-3, 0, 5),
                    right_forearm=(-2, 0, 0))),
        (0.44, dict(body=(-0.5, 0, -0.4), right_arm=(0.5, 0, -1))))
    hit('hit_legs_2', 0.65, (0.06, dict(root=(0, 0, 0, 0, 1.1, 0.3), body=(8, 4, -5), head=(-4, -2, 3), right_arm=(-12, 0, 18),
                                        right_forearm=(-10, 0, 0))),
        (0.26, dict(root=(0, 0, 0, 0, 0.5, 0.1), body=(3, 1.5, -2), head=(-1.5, -1, 1), right_arm=(-4, 0, 6),
                    right_forearm=(-3, 0, 0))),
        (0.46, dict(body=(-0.6, -0.4, 0.5), right_arm=(0.5, 0, -1.5))))
    hit('hit_legs_3', 0.7, (0.07, dict(root=(0, 0, 0, 0, 1.4, -0.3), body=(10, -3, 0), head=(-5, 2, 0), right_arm=(-16, 0, 12),
                                       right_forearm=(-14, 0, 0))),
        (0.28, dict(root=(0, 0, 0, 0, 0.7, -0.1), body=(4, -1, 0), head=(-2, 1, 0), right_arm=(-5, 0, 4),
                    right_forearm=(-4, 0, 0))),
        (0.5, dict(body=(-0.8, 0.3, 0), right_arm=(1, 0, -1))))
    return out


# ---- deaths (absolute from rest; every limb keyed; the last key lies on the ground)

LIMBS = ('root', 'body', 'head', 'right_arm', 'right_forearm', 'right_hand', 'left_arm', 'left_forearm', 'left_hand',
         'right_leg', 'right_shin', 'left_leg', 'left_shin')
START = pose(left_arm=(-20, 0, 0), left_forearm=(-8, 0, 0))   # between a bare arm and the shield carry
HEELS, TOES = (0, 24, 2), (0, 24, -2)


def death(name, length, keys, settle_lift=0.0):
    """keys: [(t, pose dict, contact point or None, interp)]. A key with a contact point turns about it and lands its
    lowest corner on the ground; one without is used as written. Bones a key leaves out hold the previous key's value."""
    a = Anim(name, length, 'hold')
    held = {b: six(()) for b in LIMBS}
    for n, (t, p, about, i) in enumerate(keys):
        full = {b: list(v) for b, v in held.items()}
        full.update({b: six(v) for b, v in p.items()})
        if about is not None:
            full = grounded(full, about, lift=settle_lift if n == len(keys) - 1 else 0.0)
        held = full
        a.pose(t, full, i)
    return a


def deaths():
    out = []
    # Stab: the blade goes in, he folds round it with both hands at the wound; the legs go and he drops to his knees;
    # sways there, then pitches forward onto his face.
    out.append(death('death_stab', 1.7, [
        (0.0, START, None, 'catmullrom'),
        (0.12, pose(root=(0, 0, 0, 0, 0.6, 0.8), body=(16, 0, 0), head=(14, 0, 0), right_arm=(-40, 0, -24), right_forearm=(-70, 0, 0),
                    right_hand=(30, 0, 0), left_arm=(-40, 0, 24), left_forearm=(-70, 0, 0), right_leg=(-6, 0, 0),
                    right_shin=(10, 0, 0), left_leg=(-6, 0, 0), left_shin=(10, 0, 0)), None, 'linear'),
        (0.5, pose(body=(10, 0, 0), head=(22, 0, 3), right_arm=(-30, 0, -18), right_forearm=(-60, 0, 0),
                   left_arm=(-30, 0, 18), left_forearm=(-60, 0, 0), right_leg=(-4, 0, 3), right_shin=(92, 0, 0),
                   left_leg=(-8, 0, -3), left_shin=(96, 0, 0)), (0, 24, 2), 'catmullrom'),
        (0.62, pose(root=(4, 0, 0), body=(14, 0, 0), head=(26, 0, 4)), (0, 24, 2), 'catmullrom'),
        (0.9, pose(root=(10, 0, 3), body=(20, 0, 2), head=(30, 0, 5), right_arm=(-8, 0, -8), right_forearm=(-20, 0, 0),
                   left_arm=(-10, 0, 8), left_forearm=(-24, 0, 0)), (0, 24, 2), 'catmullrom'),
        (1.15, pose(root=(48, 0, 4), body=(16, 0, 0), head=(10, 0, 0), right_arm=(-60, 0, -10), right_forearm=(-20, 0, 0),
                    left_arm=(-62, 0, 12), left_forearm=(-24, 0, 0), right_leg=(-2, 0, 3), right_shin=(70, 0, 0),
                    left_leg=(-6, 0, -3), left_shin=(74, 0, 0)), (0, 24, 2), 'catmullrom'),
        (1.32, pose(root=(88, 0, 4), body=(-2, 0, 0), head=(-28, 18, 0), right_arm=(-150, 0, -30), right_forearm=(-24, 0, 0),
                    left_arm=(-150, -40, 34), left_forearm=(-26, 0, 0), right_leg=(4, 0, 4), right_shin=(30, 0, 0),
                    left_leg=(0, 0, -4), left_shin=(36, 0, 0)), (0, 24, 2), 'linear'),
        (1.44, pose(root=(84, 0, 4), body=(0, 0, 0), head=(-30, 20, 0), right_forearm=(-30, 0, 0), left_forearm=(-32, 0, 0),
                    right_shin=(38, 0, 0), left_shin=(42, 0, 0)), (0, 24, 2), 'catmullrom'),
        (1.7, pose(root=(86, 0, 4), body=(-4, 0, 0), head=(-34, 18, 0), right_forearm=(-34, 0, 0), left_arm=(-150, -60, 34),
                   left_forearm=(-36, 0, 0), right_shin=(34, 0, 0), left_shin=(38, 0, 0)), (0, 24, 2), 'catmullrom'),
    ]))

    def topple(name, length, axis, sign, about, extra, kind_note=''):
        """A straight fall about the feet: a stagger that buys the fall its anticipation, the tip, the impact with a
        small rebound, and the limbs' own follow-through. axis 0 pitches, 2 rolls; sign is the direction."""
        def rot(deg):
            r = [0.0, 0.0, 0.0]
            r[axis] = sign * deg
            return tuple(r)
        k = extra
        return death(name, length, [
            (0.0, START, None, 'catmullrom'),
            (0.14, pose(k['jolt'], root=rot(-5) + (0, 0.2, 0)), about, 'linear'),
            (0.38, pose(k['stagger'], root=rot(10)), about, 'catmullrom'),
            (0.62, pose(k['tip'], root=rot(42)), about, 'catmullrom'),
            (0.8, pose(k['fall'], root=rot(80)), about, 'linear'),
            (0.88, pose(k['land'], root=rot(90)), about, 'linear'),
            (0.98, pose(k['bounce'], root=rot(86)), about, 'catmullrom'),
            (1.12, pose(k['lie'], root=rot(90)), about, 'catmullrom'),
            (length, pose(k['lie'], root=rot(90)), about, 'catmullrom'),
        ])

    # Fall backwards (the blow came from the front).
    back = dict(
        jolt=pose(body=(-10, 0, 0), head=(-14, 0, 0), right_arm=(-30, 0, 20), right_forearm=(-20, 0, 0), left_arm=(-30, 0, -20),
                  left_forearm=(-14, 0, 0), right_leg=(-8, 0, 0), right_shin=(6, 0, 0)),
        stagger=pose(body=(-6, 0, 0), head=(-8, 0, 0), right_arm=(-50, 0, 30), right_forearm=(-30, 0, 0), left_arm=(-46, 0, -28),
                     left_forearm=(-20, 0, 0), right_leg=(-16, 0, 0), right_shin=(14, 0, 0), left_leg=(4, 0, 0), left_shin=(10, 0, 0)),
        tip=pose(body=(6, 0, 0), head=(12, 0, 0), right_arm=(-70, 0, 40), right_forearm=(-20, 0, 0), left_arm=(-64, 0, -36),
                 left_forearm=(-16, 0, 0), right_leg=(-26, 0, 0), right_shin=(24, 0, 0), left_leg=(-10, 0, 0), left_shin=(22, 0, 0)),
        fall=pose(body=(10, 0, 0), head=(18, 0, 0), right_arm=(-110, 0, 40), right_forearm=(-10, 0, 0), left_arm=(-100, 0, -40),
                  left_forearm=(-10, 0, 0), right_leg=(-30, 0, 0), right_shin=(30, 0, 0), left_leg=(-20, 0, 0), left_shin=(26, 0, 0)),
        land=pose(body=(4, 0, 0), head=(-6, 0, 0), right_arm=(-150, 0, 34), right_forearm=(-4, 0, 0), left_arm=(-140, 0, -34),
                  left_forearm=(-6, 0, 0), right_leg=(-24, 0, 0), right_shin=(24, 0, 0), left_leg=(-12, 0, 0), left_shin=(16, 0, 0)),
        bounce=pose(body=(6, 0, 0), head=(4, 0, 0), right_arm=(-140, 0, 36), left_arm=(-132, 0, -36),
                    right_leg=(-28, 0, 0), right_shin=(30, 0, 0)),
        lie=pose(body=(3, 0, 0), head=(36, -6, 0), right_arm=(-148, 0, 34), right_forearm=(-38, 0, 0), right_hand=Z,
                 left_arm=(-136, 0, -38), left_forearm=(-34, 0, 0), right_leg=(-22, 0, 4), right_shin=(44, 0, 0),
                 left_leg=(-16, 0, -2), left_shin=(32, 0, 0)))
    fwd = dict(
        jolt=pose(body=(14, 0, 0), head=(16, 0, 0), right_arm=(20, 0, 14), right_forearm=(-10, 0, 0), left_arm=(16, 0, -12),
                  left_forearm=(-10, 0, 0), right_leg=(8, 0, 0)),
        stagger=pose(body=(10, 0, 0), head=(6, 0, 0), right_arm=(-10, 0, 16), right_forearm=(-30, 0, 0), left_arm=(-12, 0, -14),
                     left_forearm=(-26, 0, 0), right_leg=(-18, 0, 0), right_shin=(22, 0, 0), left_leg=(8, 0, 0), left_shin=(6, 0, 0)),
        tip=pose(body=(4, 0, 0), head=(-8, 0, 0), right_arm=(-60, 0, 14), right_forearm=(-30, 0, 0), left_arm=(-58, 0, -14),
                 left_forearm=(-30, 0, 0), right_leg=(-10, 0, 0), right_shin=(12, 0, 0), left_leg=(6, 0, 0), left_shin=(10, 0, 0)),
        fall=pose(body=(0, 0, 0), head=(-20, 0, 0), right_arm=(-120, 0, 20), right_forearm=(-40, 0, 0), left_arm=(-116, 0, -18),
                  left_forearm=(-40, 0, 0), right_leg=(0, 0, 0), right_shin=(14, 0, 0), left_leg=(4, 0, 0), left_shin=(16, 0, 0)),
        land=pose(body=(-4, 0, 0), head=(-24, 0, 0), right_arm=(-150, 0, 24), right_forearm=(-20, 0, 0), left_arm=(-146, 0, -24),
                  left_forearm=(-18, 0, 0), right_shin=(20, 0, 0), left_shin=(24, 0, 0)),
        bounce=pose(body=(-2, 0, 0), head=(-18, 0, 0), right_shin=(34, 0, 0), left_shin=(30, 0, 0)),
        lie=pose(body=(-8, 0, 0), head=(-36, 14, 0), right_arm=(-160, 0, 24), right_forearm=(-34, 0, 0), right_hand=Z,
                 left_arm=(-150, -60, -26), left_forearm=(-36, 0, 0), right_leg=(2, 0, 4), right_shin=(34, 0, 0),
                 left_leg=(0, 0, -3), left_shin=(40, 0, 0)))

    def side(s):
        """Fall to the troop's right (s=1, the blow from its left) or left (s=-1). The near arm goes out to catch the fall."""
        near, far = ('right', 'left') if s > 0 else ('left', 'right')
        sz = 1 if near == 'right' else -1     # an arm's rz that lifts it out to its own side
        def arms(n, f):
            return {near + '_arm': (n[0], 0, sz * n[1]), near + '_forearm': (n[2], 0, 0),
                    far + '_arm': (f[0], 0, -sz * f[1]), far + '_forearm': (f[2], 0, 0)}
        return dict(
            jolt=pose(arms((-10, 20, -10), (-10, 14, -10)), body=(4, 0, -6 * s), head=(4, 0, -10 * s)),
            stagger=pose(arms((-20, 40, -20), (-30, 20, -30)), body=(4, 0, -4 * s), head=(2, 0, 6 * s),
                         **{near + '_leg': (0, 0, 8 * s * sz * sz), near + '_shin': (12, 0, 0)}),
            tip=pose(arms((-30, 70, -10), (-40, 30, -40)), body=(6, 0, 6 * s), head=(4, 0, 12 * s),
                     **{near + '_leg': (-10, 0, 0), near + '_shin': (24, 0, 0), far + '_leg': (0, 0, 0)}),
            fall=pose(arms((-30, 100, -6), (-50, 40, -50)), body=(6, 0, 8 * s), head=(4, 0, 18 * s)),
            land=pose(arms((-20, 110, -4), (-40, 30, -40)), body=(4, 0, 2 * s), head=(2, 0, -6 * s)),
            bounce=pose(body=(4, 0, 4 * s), head=(2, 0, 4 * s)),
            lie=pose(arms((-70, -6, -35), (-60, 10, -50)), body=(4, 0, 8 * s), head=(4, 0, -36 * s), right_hand=Z,
                     **{near + '_leg': (-12, 0, 0), near + '_shin': (34, 0, 0), far + '_leg': (-26, 0, 0), far + '_shin': (40, 0, 0)}))

    # Directions: the blow from the front throws him back (pitch -), from behind forward (pitch +), from his left onto
    # his right side (roll -), from his right onto his left (roll +).
    out.append(topple('death', 1.4, 0, -1, HEELS, back))
    out.append(topple('death_back', 1.4, 0, 1, TOES, fwd))
    out.append(topple('death_left', 1.4, 2, -1, (-2, 24, 0), side(1)))
    out.append(topple('death_right', 1.4, 2, 1, (2, 24, 0), side(-1)))
    out.append(topple('death_slash_front', 1.4, 0, -1, HEELS, back))
    out.append(topple('death_slash_back', 1.4, 0, 1, TOES, fwd))
    out.append(topple('death_slash_left', 1.4, 2, -1, (-2, 24, 0), side(1)))
    out.append(topple('death_slash_right', 1.4, 2, 1, (2, 24, 0), side(-1)))

    # K2 (R105): second variants, death_<kind>[_<dir>]_2. The engine appends each to its key's list and picks one per
    # death, so a rank that falls together does not fall in step. They reuse the grounded lies above, but get there by a
    # different road: a drop to the knees first, or a different fall.
    KNEEL = (0, 24, 2)

    def kneel_fall(name, length, axis, sign, about, jolt, lie):
        """The legs go first: a jolt, a drop to both knees, a sway there, then a fall the topple's way onto the lie."""
        def rot(deg):
            r = [0.0, 0.0, 0.0]
            r[axis] = sign * deg
            return tuple(r)
        knees = dict(right_leg=(-4, 0, 3), right_shin=(92, 0, 0), left_leg=(-8, 0, -3), left_shin=(96, 0, 0))
        return death(name, length, [
            (0.0, START, None, 'catmullrom'),
            (0.12, pose(jolt, root=(0, 0, 0, 0, 0.4, 0)), None, 'linear'),
            (0.42, pose(knees, body=(12, 0, 0), head=(18, 0, 0), right_arm=(-20, 0, 10), right_forearm=(-40, 0, 0),
                        left_arm=(-20, 0, -10), left_forearm=(-40, 0, 0)), KNEEL, 'catmullrom'),
            (0.54, pose(root=(4, 0, 0), body=(16, 0, 0), head=(24, 0, 0)), KNEEL, 'catmullrom'),
            (0.8, pose(root=rot(14), body=(10, 0, 0), head=(14, 0, 0), right_arm=(-40, 0, 20), left_arm=(-40, 0, -20)),
             KNEEL, 'catmullrom'),
            (1.02, pose(lie, root=rot(60), right_shin=(60, 0, 0), left_shin=(64, 0, 0)), about, 'linear'),
            (1.14, pose(lie, root=rot(90)), about, 'linear'),
            (1.24, pose(root=rot(86)), about, 'catmullrom'),
            (1.4, pose(lie, root=rot(90)), about, 'catmullrom'),
            (length, pose(lie, root=rot(90)), about, 'catmullrom'),
        ])

    clutch = pose(body=(24, 0, 0), head=(20, 0, 0), right_arm=(-40, 0, -24), right_forearm=(-70, 0, 0), left_arm=(-36, 0, 22),
                  left_forearm=(-64, 0, 0), right_leg=(-6, 0, 0), right_shin=(10, 0, 0))
    arch = pose(body=(-16, 0, 0), head=(-20, 0, 0), right_arm=(-30, 0, 30), right_forearm=(-20, 0, 0), left_arm=(-24, 0, -26),
                left_forearm=(-20, 0, 0))
    reel = lambda s: pose(body=(4, 0, -8 * s), head=(6, 0, -14 * s), right_arm=(-16, 0, 20), right_forearm=(-30, 0, 0),
                          left_arm=(-16, 0, -20), left_forearm=(-30, 0, 0))
    out.append(kneel_fall('death_slash_front_2', 1.6, 0, 1, TOES, clutch, fwd['lie']))
    out.append(kneel_fall('death_slash_back_2', 1.6, 0, 1, TOES, arch, fwd['lie']))
    out.append(kneel_fall('death_slash_left_2', 1.6, 2, -1, (-2, 24, 0), reel(1), side(1)['lie']))
    out.append(kneel_fall('death_slash_right_2', 1.6, 2, 1, (2, 24, 0), reel(-1), side(-1)['lie']))
    # Stab 2: the blade goes in and he staggers back off it, both hands at the wound, and goes over backwards.
    out.append(topple('death_stab_2', 1.4, 0, -1, HEELS, dict(back, jolt=clutch, stagger=pose(clutch, body=(14, 0, 0), right_leg=(-16, 0, 0),
                                                                                               right_shin=(14, 0, 0)))))
    # Blunt 2: the blow drops him to his knees, dazed; he sways and pitches forward onto his face.
    out.append(kneel_fall('death_blunt_2', 1.6, 0, 1, TOES, pose(root=(0, 0, 6), head=(-12, -10, 16), body=(-4, -4, 6),
                                                                 right_arm=(-8, 0, 18), left_arm=(-10, 0, -16)), fwd['lie']))
    # Arrow 2: the shaft punches him straight over backwards, the sword hand going to the wound as he goes.
    out.append(topple('death_arrow_2', 1.4, 0, -1, HEELS, dict(back, jolt=pose(body=(-14, -6, 0), head=(-12, 0, 0), right_arm=(-34, 0, -22),
                                                                                right_forearm=(-80, 0, 0), right_hand=(20, 0, 0),
                                                                                left_arm=(-30, 0, -20), left_forearm=(-14, 0, 0)))))
    # Magic 2: seized stiff with the arms flung wide, he topples sideways like a felled post onto his left side.
    out.append(topple('death_magic_2', 1.4, 2, 1, (2, 24, 0), dict(side(-1), jolt=pose(body=(-10, 0, 0), head=(-18, 0, 0),
                                                                                         right_arm=(-40, 0, 50), right_forearm=(-10, 0, 0),
                                                                                         left_arm=(-40, 0, -50), left_forearm=(-10, 0, 0)))))

    # Blunt: the blow rings the head; the knees unlock and he drops straight down, folding, then keels onto his side.
    out.append(death('death_blunt', 1.6, [
        (0.0, START, None, 'catmullrom'),
        (0.08, pose(root=(0, 0, -4, 0, 0.3, 0), head=(-10, 8, -14), body=(-4, 4, -4), right_arm=(-10, 0, 16),
                    left_arm=(-12, 0, -14)), None, 'linear'),
        (0.3, pose(head=(14, 4, 8), body=(10, 0, 4), right_arm=(-4, 0, 6), right_forearm=(-10, 0, 0), left_arm=(-4, 0, -6),
                   left_forearm=(-10, 0, 0), right_leg=(-30, 0, 4), right_shin=(60, 0, 0), left_leg=(-26, 0, -4),
                   left_shin=(56, 0, 0)), (0, 24, 0), 'catmullrom'),
        (0.52, pose(head=(26, 0, 10), body=(16, 0, 6), right_arm=(6, 0, 4), left_arm=(4, 0, -4), right_leg=(-50, 0, 6),
                    right_shin=(120, 0, 0), left_leg=(-46, 0, -6), left_shin=(118, 0, 0)), (0, 24, 0), 'linear'),
        (0.6, pose(root=(4, 0, 4), head=(30, 0, 12), body=(20, 0, 6)), (0, 24, 0), 'catmullrom'),
        (0.85, pose(root=(14, 0, 30), head=(24, 0, 16), body=(14, 0, 4), right_arm=(-10, 0, 20)), (2, 24, 0), 'catmullrom'),
        (1.08, pose(root=(14, 0, 86), head=(6, 0, 30), body=(10, 0, -8), right_arm=(-30, 0, 30), right_forearm=(-30, 0, 0),
                    left_arm=(-50, 0, 10), left_forearm=(-34, 0, 0), right_leg=(-60, 0, 4), right_shin=(100, 0, 0),
                    left_leg=(-52, 0, -2), left_shin=(96, 0, 0)), (2, 24, 0), 'linear'),
        (1.2, pose(root=(14, 0, 82), head=(10, 0, 46)), (2, 24, 0), 'catmullrom'),
        (1.6, pose(root=(14, 0, 88), head=(8, 0, 44)), (2, 24, 0), 'catmullrom'),
    ]))
    # Arrow: the shaft punches him back a step, his sword hand drops to the wound; he looks at it, sags to one knee and
    # then rolls forward onto his side.
    out.append(death('death_arrow', 1.8, [
        (0.0, START, None, 'catmullrom'),
        (0.06, pose(root=(-4, 0, 0, 0, 0.2, 1.4), body=(-12, -6, 0), head=(-10, 0, 0), right_arm=(-30, 0, 18),
                    right_forearm=(-30, 0, 0), left_arm=(-24, 0, -14)), None, 'linear'),
        (0.3, pose(root=(0, 0, 0, 0, 0.4, 2.0), body=(10, -4, 0), head=(26, 0, 0), right_arm=(-34, 0, -22),
                   right_forearm=(-80, 0, 0), right_hand=(20, 0, 0), left_arm=(-20, 0, -8), left_forearm=(-20, 0, 0)), None, 'catmullrom'),
        (0.7, pose(body=(14, -6, 0), head=(20, 0, 6), right_leg=(-30, 0, 4), right_shin=(30, 0, 0), left_leg=(0, 0, -2),
                   left_shin=(90, 0, 0), left_arm=(-30, 0, -20)), (0, 24, 4), 'catmullrom'),
        (0.95, pose(root=(10, 0, -6), body=(18, -6, 0), head=(26, 0, 8), left_arm=(-50, 0, -30), left_forearm=(-30, 0, 0)),
         (0, 24, 2), 'catmullrom'),
        (1.25, pose(root=(54, 0, -30), body=(10, 0, 0), head=(10, 0, 10), left_arm=(-110, 0, -40), left_forearm=(-20, 0, 0),
                    right_leg=(-20, 0, 4), right_shin=(60, 0, 0), left_leg=(-10, 0, 0), left_shin=(60, 0, 0)), (0, 24, -2), 'catmullrom'),
        (1.42, pose(root=(86, 0, -40), body=(-4, 0, -6), head=(-24, 12, -14), right_arm=(-140, 0, 30), right_forearm=(-34, 0, 0),
                    right_hand=Z, left_arm=(-150, 0, -30), left_forearm=(-34, 0, 0), right_leg=(-24, 0, 4), right_shin=(50, 0, 0),
                    left_shin=(36, 0, 0)), (0, 24, -2), 'linear'),
        (1.54, pose(root=(82, 0, -38), body=(-5, 0, -7), head=(-28, 12, -18)), (0, 24, -2), 'catmullrom'),
        (1.8, pose(root=(84, 0, -40), body=(-6, 0, -8), head=(-30, 12, -20), right_arm=(-150, 0, 30)), (0, 24, -2), 'catmullrom'),
    ]))
    # Magic: seized rigid and lifted onto his toes, arched back with the arms flung wide, a shudder, then the force lets
    # go and he collapses where he stood, onto his back.
    out.append(death('death_magic', 1.9, [
        (0.0, START, None, 'catmullrom'),
        (0.1, pose(root=(0, 0, 0, 0, -0.4, 0), body=(-10, 0, 0), head=(-20, 0, 0), right_arm=(-40, 0, 50), right_forearm=(-10, 0, 0),
                   left_arm=(-40, 0, -50), left_forearm=(-10, 0, 0), right_leg=(4, 0, 2), left_leg=(4, 0, -2)), None, 'linear'),
        (0.45, pose(root=(0, 0, 0, 0, -1.6, 0), body=(-18, 0, 0), head=(-30, 0, 0), right_arm=(-60, 0, 70), right_forearm=(-4, 0, 0),
                    left_arm=(-60, 0, -70), left_forearm=(-4, 0, 0), right_leg=(8, 0, 3), right_shin=(4, 0, 0),
                    left_leg=(8, 0, -3), left_shin=(4, 0, 0)), None, 'catmullrom'),
        (0.55, pose(root=(0, 0, 2, 0, -1.7, 0), head=(-26, 4, 4), right_arm=(-58, 0, 74), left_arm=(-62, 0, -66)), None, 'linear'),
        (0.65, pose(root=(0, 0, -2, 0, -1.7, 0), head=(-32, -4, -4), right_arm=(-62, 0, 66), left_arm=(-58, 0, -74)), None, 'linear'),
        (0.75, pose(root=(0, 0, 0, 0, -1.6, 0), head=(-30, 0, 0), right_arm=(-60, 0, 70), left_arm=(-60, 0, -70)), None, 'catmullrom'),
        (1.0, pose(root=(0, 0, 0, 0, 0, 0), body=(12, 0, 0), head=(20, 0, 0), right_arm=(-6, 0, 10), right_forearm=(-20, 0, 0),
                   left_arm=(-6, 0, -10), left_forearm=(-20, 0, 0), right_leg=(-34, 0, 4), right_shin=(70, 0, 0),
                   left_leg=(-30, 0, -4), left_shin=(66, 0, 0)), (0, 24, 0), 'catmullrom'),
        (1.3, pose(root=(-40, 0, 0), body=(6, 0, 0), head=(10, 0, 0), right_arm=(-40, 0, 24), left_arm=(-40, 0, -24),
                   right_leg=(-60, 0, 4), right_shin=(90, 0, 0), left_leg=(-54, 0, -4), left_shin=(84, 0, 0)), (0, 24, 2), 'linear'),
        (1.48, pose(root=(-84, 0, 0), body=(2, 0, 0), head=(-10, 0, 0), right_arm=(-120, 0, 40), right_forearm=(-30, 0, 0),
                    left_arm=(-120, 0, -40), left_forearm=(-30, 0, 0), right_leg=(-50, 0, 6), right_shin=(70, 0, 0),
                    left_leg=(-40, 0, -6), left_shin=(60, 0, 0)), (0, 24, 2), 'linear'),
        (1.6, pose(root=(-80, 0, 0), body=(4, 0, 0), head=(12, -6, 0), right_leg=(-44, 0, 6), right_shin=(60, 0, 0)), (0, 24, 2), 'catmullrom'),
        (1.9, pose(root=(-82, 0, 0), body=(6, 0, 0), head=(20, -10, 0), right_forearm=(-34, 0, 0), left_forearm=(-34, 0, 0),
                   right_leg=(-40, 0, 6), right_shin=(56, 0, 0), left_leg=(-30, 0, -6), left_shin=(44, 0, 0)), (0, 24, 2), 'catmullrom'),
    ]))
    return out


def scenes():
    """Scene poses: fall (a fallen troop, held) and raise (the sword held up in salute, held)."""
    out = []
    lie = death('fall', 1.2, [
        (0.0, START, None, 'catmullrom'),
        (0.5, pose(root=(-40, 0, 0), body=(8, 0, 0), right_arm=(-40, 0, 20), left_arm=(-40, 0, -20), right_leg=(-20, 0, 2),
                   right_shin=(30, 0, 0), left_leg=(-14, 0, -2), left_shin=(24, 0, 0)), HEELS, 'catmullrom'),
        (0.9, pose(root=(-88, 0, 0), body=(3, 0, 0), head=(-4, -18, 0), right_arm=(-150, 0, 30), left_arm=(-136, 0, -36),
                   right_leg=(-18, 0, 4), right_shin=(24, 0, 0), left_leg=(-4, 0, -2), left_shin=(8, 0, 0)), HEELS, 'catmullrom'),
        (1.2, pose(root=(-90, 0, 0), head=(-4, -20, 0)), HEELS, 'catmullrom'),
    ])
    lie.name = 'fall'
    out.append(lie)
    a = Anim('raise', 1.0, 'hold')
    a.k(0.0, right_arm=Z, right_forearm=Z, right_hand=Z, body=Z, head=Z)
    a.k(0.2, right_arm=(-30, 0, 6), right_forearm=(-40, 0, 0), right_hand=(20, 0, 0), body=(2, 4, 0))
    a.k(0.55, i='linear', right_arm=(-176, 0, -8), right_forearm=(-6, 0, 0), right_hand=(40, 0, 0), body=(-4, -4, 0), head=(-12, 0, 0))
    a.k(0.7, right_arm=(-170, 0, -6), right_forearm=(-4, 0, 0), right_hand=(36, 0, 0), body=(-3, -3, 0), head=(-10, 0, 0))
    a.k(1.0, right_arm=(-172, 0, -6), right_forearm=(-4, 0, 0), right_hand=(38, 0, 0), body=(-3, -3, 0), head=(-10, 0, 0))
    a.lag(right_hand=0.04)
    out.append(a)
    return out


# ---- bow (interim set, R110: the archer's aim, its two shots and the forced bow_draw; the bow sits in the left hand)

# Aim: side on to the target, the bow arm straight out with the bow upright, the string hand anchored under the chin
# on full draw's nock (frame 3; TroopMotion.twist turns the hand). Shoulders are mostly pitch, so the engine's aimPitch,
# added to both arms' pitch, swings bow and string hand together toward the sky (within 1.4 px of the nock at 45
# degrees up or 15 down, every pull frame). Keyed from a solve of the rig's FK, rounded to a tenth of a degree.
AIM = pose(body=(0, 25.8, 0), head=(-2, -22, 0), left_arm=(-101.1, 22.7, -25.0), left_forearm=(1.2, -12.5, 0),
           left_hand=(-1.6, -7.8, -50.3), right_arm=(-97.2, -31.9, 37.5), right_forearm=(-77.0, -44.2, -7.3),
           right_leg=(3, 0, 4), left_leg=(-4, 0, -4), left_shin=(4, 0, 0), root=(0, 0, 0, 0, 0.4, 0))
# The string hand on each pull frame's nock, as offsets on AIM (right_arm, right_forearm): the arrow nocked with the string
# at rest (frame 0) and frames 1 and 2 of the pull; frame 3 is AIM itself. LOOSE is the hand flicked 3 px back past the
# anchor and out, the string gone.
NOCK_AT = {0: dict(right_arm=(-10.9, -14.9, -1.7), right_forearm=(39.5, -3.8, 26.0)),
           1: dict(right_arm=(-4.8, -5.6, -0.7), right_forearm=(20.2, 1.4, 14.4)),
           2: dict(right_arm=(-1.8, -1.7, -0.5), right_forearm=(8.8, 1.7, 6.4))}
LOOSE = dict(right_arm=(3.8, 5.4, -4.0), right_forearm=(-31.6, -14.1, -24.4))


def scaled(d, s):
    return {b: tuple(x * s for x in v) for b, v in d.items()}


def bow_shot(name, lean, recoil):
    """One shot (impact tick 10, the loose; the bow's pull frames switch at 0.094, 0.2 and 0.3 s on the attack's timeline):
    a fresh arrow on the string at frame 0's nock, the draw following the frames to the anchor, a held beat, the loose
    (the string hand flicks back, the bow arm gives forward and down), and back to the anchor. Offsets on AIM.
    lean is the body's pitch held through the shot (negative leans back), recoil its kick at the loose."""
    a = Anim(name, 1.2, impact=0.5)
    a.k(0.0, body=Z, right_arm=Z, right_forearm=Z, left_arm=Z, head=Z)
    a.k(0.06, i='linear', body=(lean * 0.3, 0, 0), **NOCK_AT[0])
    a.k(0.1, i='linear', **NOCK_AT[1])
    a.k(0.2, i='linear', body=(lean * 0.8, 0, 0), **NOCK_AT[2])
    a.k(0.3, i='linear', body=(lean, 0, 0), right_arm=Z, right_forearm=Z, head=(lean * -0.3, 0, 0))
    a.k(0.42, i='linear', body=(lean * 1.05, -0.5, 0), right_arm=(0.4, 0.6, 0), right_forearm=(-1.0, 0, 0), left_arm=Z)
    a.k(0.5, i='linear', body=(lean * 1.05, -0.5, 0), right_arm=Z, right_forearm=Z, left_arm=Z)
    a.k(0.56, i='linear', body=(lean + recoil, -3, 0), left_arm=(4, -3, 0), **LOOSE)
    a.k(0.75, body=(lean * 0.6 + recoil * 0.4, -1.5, 0), left_arm=(2, -1.5, 0), head=(lean * -0.15, 0, 0), **scaled(LOOSE, 0.5))
    a.k(1.0, body=(lean * 0.1, -0.3, 0), left_arm=(0.4, -0.2, 0), head=Z, **scaled(LOOSE, 0.1))
    a.k(1.2, body=Z, right_arm=Z, right_forearm=Z, left_arm=Z, head=Z)
    return a


def bow_clips():
    out = []
    # Aim: held at full draw; the archer settles and breathes, both arms pitching together so the hand keeps the nock.
    out.append(stance('aim', 3.0, AIM, [
        (0.8, dict(left_arm=(-1.2, 0, 0), right_arm=(-1.2, 0, 0), body=(0.4, 0.6, 0), root=(0, 0, 0, 0, 0.15, 0))),
        (1.7, dict(left_arm=(0.6, 0, 0), right_arm=(0.6, 0, 0), body=(-0.3, -0.4, 0), head=(0.8, 0, 0))),
        (2.4, dict(left_arm=(-0.4, 0, 0), right_arm=(-0.4, 0, 0), body=(0.2, 0.3, 0), root=(0, 0, 0, 0, 0.1, 0))),
    ]))
    # Volley: leans back into the arc (the engine's aimPitch lifts the arms toward the sky); direct: flat, a touch forward.
    out.append(bow_shot('bow_volley', -6, 1.5))
    out.append(bow_shot('bow_direct', 1.5, -1.5))
    # Forced bow_draw (the harness's p2-depth-bow, absolute): nocked at rest, the hand following the pull frames
    # (0.15, 0.32, 0.48 s) to the anchor, held to the loose at 0.8 s, then flicked off the string.
    a = Anim('bow_draw', 1.2)
    a.pose(0.0, add(AIM, **NOCK_AT[0]), 'linear')
    a.pose(0.12, add(AIM, **NOCK_AT[0]), 'linear')
    a.pose(0.15, add(AIM, **NOCK_AT[1]), 'linear')
    a.pose(0.32, add(AIM, **NOCK_AT[2]), 'linear')
    a.pose(0.48, AIM, 'linear')
    a.pose(0.8, AIM, 'linear')
    a.pose(0.86, add(AIM, **LOOSE), 'linear')
    a.pose(1.2, add(AIM, **scaled(LOOSE, 0.3)))
    out.append(a)
    return out


# ---- project

def project(family, anims):
    groups = [{'name': 'model', 'origin': [0, 0, 0], 'rotation': [180, 0, 0], 'uuid': gid('group', 'model'),
               'color': 0, 'export': True, 'visibility': True}]
    elements, children = [], {}
    for b in RIG['bones']:
        o = [round(x, 4) for x in PIVOT[b['name']]]
        groups.append({'name': b['name'], 'origin': o, 'rotation': list(b.get('rotation', [0, 0, 0])),
                       'uuid': gid('group', b['name']), 'color': 0, 'export': True, 'visibility': True})
        for n, box in enumerate(b.get('boxes', [])):
            frm = [o[i] + box['origin'][i] for i in range(3)]
            to = [frm[i] + box['size'][i] for i in range(3)]
            e = {'name': f"{b['name']}_{n}", 'box_uv': True, 'from': frm, 'to': to, 'origin': o,
                 'rotation': list(b.get('rotation', [0, 0, 0])), 'uv_offset': [0, 0], 'faces': {},
                 'type': 'cube', 'uuid': gid('cube', b['name'], str(n))}
            elements.append(e)
            children.setdefault(b['name'], []).append(e['uuid'])

    def node(name):
        b = next(x for x in RIG['bones'] if x['name'] == name)
        kids = children.get(name, []) + [node(c['name']) for c in RIG['bones'] if c['parent'] == name]
        return {'name': name, 'uuid': gid('group', name), 'origin': [round(x, 4) for x in PIVOT[name]],
                'rotation': list(b.get('rotation', [0, 0, 0])), 'export': True, 'isOpen': True, 'children': kids}

    roots = [b['name'] for b in RIG['bones'] if not b['parent']]
    outliner = [{'name': 'model', 'uuid': gid('group', 'model'), 'origin': [0, 0, 0], 'rotation': [180, 0, 0],
                 'export': True, 'isOpen': True, 'children': [node(r) for r in roots]}]
    return {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': True},
            'name': f'{family} clips', 'model_identifier': '', 'visible_box': [1, 1, 0],
            'resolution': {'width': RIG['size'][0], 'height': RIG['size'][1]},
            'elements': elements, 'groups': groups, 'outliner': outliner, 'textures': [],
            'animations': [a.animation(family) for a in anims]}


def projects():
    return {'sword_shield': stances() + attacks() + wall_attacks() + blocks(), 'common': hits() + deaths() + scenes(),
            'bow': bow_clips()}


def dump(p):
    return json.dumps(p, indent=1) + '\n'


def main(args):
    check = args == ['--check']
    if args not in ([], ['--check']):
        print(__doc__)
        return 2
    bad = 0
    for family, anims in projects().items():
        path = ROOT / 'art/clips/biped' / f'{family}.bbmodel'
        text = dump(project(family, anims))
        if check:
            if not path.is_file() or path.read_text(encoding='utf-8') != text:
                print(f'build_clips: {path.relative_to(ROOT).as_posix()} is stale; run python art/clips/build_clips.py')
                bad = 1
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            print(f'build_clips: wrote {path.relative_to(ROOT).as_posix()} ({len(anims)} animations)')
    return bad


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
