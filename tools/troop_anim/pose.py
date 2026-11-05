"""A clip on a troop as the game poses it: TroopMotion.applyForced("stance+clip"), step for step.

  1. channels = the stance clip at 0 (MotionEngine.stanceChain's first present) + the clip at t (Clip.add: a loop wraps, a
     once clip holds its ends), each {bone: [rx, ry, rz, tx, ty, tz]}
  2. MotionFeet.ground: the root rises by the smooth lift that takes the lower sole out of the ground
  3. apply: each bone's rotation and pivot are its rest plus the channels
  4. limits: a bone's pitch clamped to its rest + the model's "limit"
  5. carries on what neither clip keys: the shield arm 45 degrees forward, a bow's hand twist, idleArms forward
  6. rootScale: a taller troop's root offsets scaled by its height over the rig's

Clips come from the bbmodel sources through tools/clips.py (convert, parse, sample), so the pose is the committed clip's.
"""
import functools
import json
import math
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
import clips as CL                                    # noqa: E402

from . import model as M                              # noqa: E402

STANCES = ('idle', 'carry', 'guard', 'brace', 'wall', 'shield', 'aim', 'rest')
CHAINS = {'carry': ('carry', 'idle'), 'guard': ('guard', 'carry', 'idle'), 'brace': ('brace', 'guard', 'carry', 'idle'),
          'wall': ('wall', 'shield', 'brace', 'guard', 'carry', 'idle'), 'shield': ('shield', 'wall', 'guard', 'carry', 'idle'),
          'aim': ('aim', 'guard', 'carry', 'idle'), 'rest': ('rest', 'carry', 'idle'), 'dead': ()}
BOW_REST_YAW = 45.0
HIPS = ((-1.9, 12.0, 0.0), (1.9, 12.0, 0.0))
SHIN, SINK_TOL, SINK_EASE = 6.0, 0.25, 0.2
HULL = ((-2, 2, 0, 6, -2, 2), (-2.5, 2.5, 3, 6, -3, 2), (-1.5, 1.5, 5, 6, -4, -3), (-2.5, 2.5, -1, 1, -3, -1))
ZERO = (0.0,) * 6


def chain(key):
    return CHAINS.get(key, ('idle', 'carry'))


def source(rig, family):
    return CL.ROOT / CL.SRC / rig / f'{family}.bbmodel'


@functools.lru_cache(maxsize=None)
def _folder(rig, family, stamp):
    p = source(rig, family)
    if not p.is_file():
        return {}
    rigj = json.loads((CL.ROOT / CL.RIGS / f'{rig}.json').read_text(encoding='utf-8'))
    made = CL.convert(json.loads(p.read_text(encoding='utf-8')), rigj, p.name)
    return {n: CL.parse(c) for n, c in made.items()}


def folder(rig, family):
    """{clip name: parsed clip} of art/clips/<rig>/<family>.bbmodel (reloaded when the file changes)."""
    p = source(rig, family)
    return _folder(rig, family, p.stat().st_mtime_ns if p.is_file() else 0)


def library(rig, family):
    """TroopMotion.build: the rig's common folder overlaid by the family's."""
    out = dict(folder(rig, 'common'))
    out.update(folder(rig, family))
    return out


def stance_of(rig, family, key):
    lib = library(rig, family)
    for k in chain(key):
        if k in lib and k in STANCES:
            return lib[k]
    return None


def default_stance(troop, clip_name):
    """The stance an attack or action plays over: guard for a family's attacks (wall for its _wall strikes), else idle."""
    if clip_name.endswith('_wall'):
        return 'wall'
    if clip_name in STANCES:
        return None
    if attacks(troop.family).get(clip_name) or clip_name.startswith(('block', 'hit')):
        return 'guard'
    return 'idle'


def attacks(family):
    rows = (M.design().get('attackVariants') or [])
    return {r['id']: r for r in rows if r.get('family') == family}


def add(a, b):
    out = {k: list(v) for k, v in a.items()}
    for bone, ch in b.items():
        cur = out.setdefault(bone, [0.0] * 6)
        for i in range(6):
            cur[i] += ch[i]
    return out


def channels(clip, t, under=None):
    """Step 1: {bone: 6 channels} of stance(0) + clip(t)."""
    base = CL.sample(under, 0.0) if under else {}
    return add(base, CL.sample(clip, t) if clip else {})


def _turn(q, a, b, deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    qa, qb = q[a], q[b]
    q[a], q[b] = qa * c - qb * s, qa * s + qb * c


def _rotate(q, r):
    _turn(q, 1, 2, r[0])
    _turn(q, 2, 0, r[1])
    _turn(q, 0, 1, r[2])


def lowest(p, i):
    """MotionFeet.lowest: the lowest hull corner of leg i (0 right, 1 left), model y."""
    leg, shin = ('right_leg', 'right_shin') if i == 0 else ('left_leg', 'left_shin')
    L, S, R = p.get(leg, ZERO), p.get(shin, ZERO), p.get('root', ZERO)
    low = -1e9
    for box in HULL:
        for c in range(8):
            q = [box[c & 1], box[2 + ((c >> 1) & 1)], box[4 + ((c >> 2) & 1)]]
            _rotate(q, S[:3])
            q[0] += S[3]; q[1] += SHIN + S[4]; q[2] += S[5]
            _rotate(q, L[:3])
            q[0] += HIPS[i][0] + L[3]; q[1] += HIPS[i][1] + L[4]; q[2] += HIPS[i][2] + L[5]
            _rotate(q, R[:3])
            low = max(low, q[1] + R[4])
    return low


def lift(p):
    """MotionFeet.ground's lift (px, up) for channels p."""
    a, b = lowest(p, 0) - M.GROUND, lowest(p, 1) - M.GROUND
    d = a - b
    s = (a + b + math.sqrt(d * d + 0.04)) / 2 - SINK_TOL
    k = SINK_EASE
    return 0.0 if s <= -k else s if s >= k else (s + k) ** 2 / (4 * k)


def ground(p):
    up = lift(p)
    if up > 0:
        p.setdefault('root', [0.0] * 6)[4] -= up
    return up


def local(troop, p, clip=None, under=None, drawn=False, carries=True):
    """Steps 3 to 6: {bone: (rotation deg, pivot)} of every model bone."""
    out = troop.rest_local()
    for bone, ch in p.items():
        if bone in out:
            r, v = out[bone]
            out[bone] = (r + np.array(ch[:3]), v + np.array(ch[3:]))
    for bone, (lo, hi) in troop.limits.items():
        r, v = out[bone]
        d = troop.rest[bone][0][0]
        r = r.copy()
        r[0] = min(max(r[0], d + lo), d + hi)
        out[bone] = (r, v)
    if carries:
        def held(bone):
            return max(1.0 if clip and bone in clip['bones'] else 0.0, 1.0 if under and bone in under['bones'] else 0.0)
        sa = troop.shield_arm
        if sa and sa in out:
            r, v = out[sa]
            out[sa] = (r - np.array([45.0 * (1 - held(sa)), 0, 0]), v)
        if troop.bow:
            hb = troop.hand_bone
            if hb in out:
                dr = 1.0 if drawn else 0.0
                r, v = out[hb]
                out[hb] = (r + np.array([90 * dr, BOW_REST_YAW * (1 - dr), 90 * dr]), v)
        if troop.idle_arms == 'forward':
            for arm in ('right_arm', 'left_arm'):
                if arm in out:
                    r, v = out[arm]
                    out[arm] = (r - np.array([80.0 * (1 - held(arm)), 0, 0]), v)
    if troop.root_k != 1 and 'root' in out:
        r, v = out['root']
        d = troop.rest['root'][1]
        out['root'] = (r, d + (v - d) * troop.root_k)
    return out


class Player:
    """A troop playing one clip (by name, from its family's library, or a parsed clip given) over a stance."""

    def __init__(self, troop, clip, stance='default', carries=True):
        self.troop = troop
        lib = library(troop.rig, troop.family)
        if isinstance(clip, str):
            self.name = clip
            self.clip = lib.get(clip)
            if self.clip is None:
                raise KeyError(f'{troop.id}: no clip {clip!r} in {troop.rig}/{troop.family} or common')
        else:
            self.clip, self.name = clip, clip['clip']
        key = default_stance(troop, self.name) if stance == 'default' else stance
        self.stance_key = key
        self.under = stance_of(troop.rig, troop.family, key) if key else None
        self.drawn = self.name == 'bow_draw' or self.name in attacks(troop.family) or (
            self.name.endswith('_wall') and self.name[:-5] in attacks(troop.family))
        self.carries = carries

    @property
    def length(self):
        return self.clip['length']

    def channels(self, t):
        p = channels(self.clip, t, self.under)
        ground(p)
        return p

    def local(self, t):
        return local(self.troop, self.channels(t), self.clip, self.under, self.drawn, self.carries)

    def world(self, t):
        return self.troop.fk(self.local(t))
