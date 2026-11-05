"""The Oathbound knight's combat pack on the articulated rig (combat_rig.py): Fresh Animations-style movement, aimed
at fighting.

  combat_idle   3.2 s loop   the guard, alive: weight rocking between the feet, breath in the chest, the point
                             hunting, the shield settling, the eyes checking the flanks
  advance       1.2 s loop   moving in guard: a step-and-drag shuffle, heel then toe, knees soft, hips swaying over
                             the planted foot, the guard held steady above it
  combo         2.1 s once   the three-hit string: a crouched, loaded wind-up, a stepping overhead cut that drives
                             off the back foot, a rising backhand, a shield bash with the elbow behind it, recovery
  block         1.0 s once   brace behind the shield, the blow lands: the shield is driven back into the shoulder,
                             the knees soak it, a half step back, then the guard again
  hit_react     0.9 s once   struck unguarded: the head snaps, the chest folds, a stumbling step back, recover

Poses are chained: each beat lists only what changes from the beat before. Timing follows what makes combat read:
anticipation held a beat, strikes of two or three frames at 20 ticks, the body breaking in order (hips, then chest,
then arm, then forearm, then the wrist, via `lags`), overshoot and settle, and weight: every strike is driven from a
planted foot, and the root drops into it. The legs, the shield's wrist, the cape and the plume are solved, not keyed.
"""
import math

import numpy as np

from . import clearance as CL
from . import combat_rig as CR
from . import rig
from .rig import Clip, sample, spring_follow

STEP = 0.025   # baked keys twice a tick: fast strikes stay on their arcs and planted feet stay put
KEYED = ['body', 'chest']
BAKED = ['right_arm', 'right_forearm', 'right_hand', 'left_arm', 'left_forearm', 'head', 'left_hand', 'right_leg', 'right_shin', 'right_foot', 'left_leg', 'left_shin', 'left_foot']

# the shield on the left hand: the board upright, its face toward -z, at rest (matches animate_unit.SHIELD_LOCAL)
SHIELD_ITEM = np.eye(4)
SHIELD_ITEM[:3, :3] = rig.ry(math.radians(180))
NECK = (50.0, 75.0, 25.0)          # how far the head may turn from the chest: pitch, yaw, roll


def seq(start, steps):
    """[(time, label, changes)] -> [(time, label, full pose)], each pose the one before with the changes applied."""
    out, cur = [], dict(start)
    for t, label, ch in steps:
        cur = dict(cur)
        cur.update(ch)
        out.append((t, label, cur))
    return out


def _lagged(keys, dt, length, loop):
    if dt == 0:
        return keys
    if not loop:
        out = [keys[0]] + [(round(k[0] + dt, 4),) + tuple(k[1:]) for k in keys[1:] if k[0] + dt < length - 1e-6]
        out.append((length,) + tuple(keys[-1][1:]))
        return out
    body = [((k[0] + dt) % length,) + tuple(k[1:]) for k in keys[:-1]]
    body.sort()
    v0 = sample(keys, (length - dt) % length)
    return [(0.0,) + tuple(v0)] + [(round(k[0], 4),) + tuple(k[1:]) for k in body if k[0] > 1e-6] + [(length,) + tuple(v0)]


def _vec_track(timeline, field, i):
    return [(t,) + tuple(float(v) for v in CR._unit(p[field][i])) for t, _, p in timeline]


def build(name, length, loop, timeline, lags=None, step=STEP, clearance=True):
    """A clip from a chained timeline. The hips and chest are hand keys (Minecraft's Catmull-Rom). Everything else is
    solved and baked every `step`: the arms from their aims (each direction interpolated with the same Catmull-Rom,
    so limbs travel in arcs; `lags` delay the forearm's and the blade's aims for overlapping action), the head (eyes on
    `look`), the shield's wrist (board on `shield`) and the legs (feet on `r_place`, `l_place`). With `clearance`, the
    arms are moved aside wherever the sword or the shield would pass through the body (clearance.py). The cape and the
    plume are simulated."""
    lags = lags or {}
    c = Clip(name, length, loop)
    c.beats = [(t, label) for t, label, _ in timeline]
    for b in ('body', 'chest'):
        c.rot[b] = [(t,) + tuple(float(v) for v in p[b]) for t, _, p in timeline]
    c.pos['root'] = [(t,) + tuple(float(v) for v in p['root']) for t, _, p in timeline]
    track = {k: [(t,) + tuple(float(v) for v in p[k]) for t, _, p in timeline]
             for k in ('r_place', 'l_place', 'shield', 'look')}
    aims = {('sword', i): _vec_track(timeline, 'sword', i) for i in range(3)}
    aims[('sword', 3)] = [(t, float(p['sword'][3]), 0.0, 0.0) for t, _, p in timeline]
    aims.update({('shield_arm', i): _vec_track(timeline, 'shield_arm', i) for i in range(2)})
    c.track, c.aims = track, aims

    def at(key, t, lag=0.0):
        t = t - lag
        t = t % length if loop else max(0.0, t)
        return np.array(sample(aims[key], t))
    n = int(round(length / step))
    times = [round(i * step, 4) for i in range(n + 1)]
    lf, lh, ls = lags.get('forearm', 0.0), lags.get('hand', 0.0), lags.get('shield_forearm', 0.0)

    def sword_aims(t):
        return (at(('sword', 0), t), at(('sword', 1), t, lf), at(('sword', 2), t, lh))

    def shield_aims(t):
        return (at(('shield_arm', 0), t), at(('shield_arm', 1), t, ls))

    # 1. the legs and the head: they do not depend on the arms
    out = {k: [] for k in BAKED}
    guess = {'right': (0.0, 0.0, 10.0), 'left': (0.0, 0.0, 10.0)}
    c.ik_error = 0.0
    for t in times:
        rot, pos = c.at(t)
        for side in ('right', 'left'):
            hip, knee, foot, err = CR.solve_leg(side, pos['root'], CR.pchip(track[side[0] + '_place'], t), guess[side])
            guess[side] = (hip[0], hip[2], knee)
            c.ik_error = max(c.ik_error, err)
            out[side + '_leg'].append((t,) + hip)
            out[side + '_shin'].append((t, knee, 0.0, 0.0))
            out[side + '_foot'].append((t,) + foot)
        pose = CR.world(rot, pos)
        head = CR.mc_euler(pose['chest'][:3, :3].T @ CR.mc_rotation(*CR.pchip(track['look'], t)))
        out['head'].append((t,) + tuple(max(-m, min(m, v)) for v, m in zip(head, NECK)))
    for k in ('right_leg', 'right_shin', 'right_foot', 'left_leg', 'left_shin', 'left_foot', 'head'):
        _unwrap(out[k])
        c.rot[k] = out[k]

    def arms(t, cs, ch, prev):
        s = CR.aim_arm(*CL.pushed(sword_aims(t), 'oathblade', cs), at(('sword', 3), t, lh)[0], prev=prev.get('right'))
        h = CR.aim_arm(*CL.pushed(shield_aims(t), 'sunward', ch), prev=prev.get('left'))
        return s, h

    def posed(t, s, h):
        rot, pos = c.at(t)
        rot.update(right_arm=s['arm'], right_forearm=s['forearm'], right_hand=s['hand'], left_arm=h['arm'],
                   left_forearm=h['forearm'])
        pose = CR.world(rot, pos)
        hand = CR.aim_hand(pose['left_forearm'], CR.pchip(track['shield'], t), SHIELD_ITEM)
        rot['left_hand'] = hand
        return CR.world(rot, pos), hand

    # 2. how far each arm must move aside, tick by tick, to keep its item out of the body
    need = {'oathblade': [], 'sunward': []}
    c.clearance_raw = {}
    if clearance:
        clip_ = CL.Clipper()
        for t in times:
            for item in need:
                best = None
                for lvl in CL.LEVELS:
                    s, h = arms(t, lvl if item == 'oathblade' else 0.0, lvl if item == 'sunward' else 0.0, {})
                    v = clip_.volumes(posed(t, s, h)[0], which=item)[item]
                    if best is None or v < best[1] - 0.25:
                        best = (lvl, v)
                    if v <= CL.TOLERANCE:
                        best = (lvl, v)
                        break
                need[item].append(best[0])
        need = {k: CL.smooth(v, step, loop=loop) for k, v in need.items()}
    else:
        need = {k: np.zeros(len(times)) for k in need}
    c.push = need

    # 3. the arms and the shield's wrist, with the pushes
    prev = {}
    for i, t in enumerate(times):
        s, h = arms(t, need['oathblade'][i], need['sunward'][i], prev)
        prev = {'right': s, 'left': h}
        _, hand = posed(t, s, h)
        for bone, v in (('right_arm', s['arm']), ('right_forearm', s['forearm']), ('right_hand', s['hand']),
                        ('left_arm', h['arm']), ('left_forearm', h['forearm']), ('left_hand', hand)):
            out[bone].append((t,) + tuple(float(x) for x in v))
    _unwrap(out['left_hand'])
    for b in ('right_arm', 'right_forearm', 'right_hand', 'left_arm', 'left_forearm', 'left_hand'):
        c.rot[b] = out[b]
    spring_follow(c, 'cape', 'chest', (1.0, 1.0), stiffness=70, damping=9, rest=(4, 0), clamp=_cape_clamp(c),
                  passes=3 if loop else 2, bones=CR.BONES)
    spring_follow(c, 'plume', 'head', (0.6, 0.6), stiffness=140, damping=10, rest=(0, 0), passes=3 if loop else 2,
                  bones=CR.BONES)
    return c


def _unwrap(keys):
    """Keeps baked angles continuous (no 359 -> -1 jumps between ticks)."""
    for i in range(1, len(keys)):
        k = list(keys[i])
        for j in (1, 2, 3):
            while k[j] - keys[i - 1][j] > 180:
                k[j] -= 360
            while k[j] - keys[i - 1][j] < -180:
                k[j] += 360
        keys[i] = tuple(k)


def _cape_clamp(clip):
    """The cape may not swing into the back, nor through a thigh swung back under it."""
    def run(t, pitch, roll):
        rot, _ = clip.at(t)
        lean = rot.get('body', (0, 0, 0))[0] + rot.get('chest', (0, 0, 0))[0]
        back = max(0.0, rot.get('right_leg', (0, 0, 0))[0], rot.get('left_leg', (0, 0, 0))[0])
        lo = max(2.0, 0.8 * back - lean + 3.0)
        return max(pitch, lo), max(-35.0, min(35.0, roll))
    return run


# ---- poses ------------------------------------------------------------------------------
# Directions are in the chest's frame: +x the troop's right (the sword side), +y up, -z forward.

NEUTRAL = dict(root=(0.0, 0.0, 0.0), body=(0.0, 0.0, 0.0), chest=(0.0, 0.0, 0.0),
               sword=((0, -1, 0), (0, -1, 0), (0, 0, -1), 0), shield_arm=((0, -1, 0), (0, -1, 0)),
               shield=(0.0, 0.0, 0.0), look=(0.0, 0.0, 0.0),
               r_place=(1.9, 0.0, 0.0, 0.0, 0.0), l_place=(1.9, 0.0, 0.0, 0.0, 0.0))


def P(base=None, **kw):
    p = dict(base or NEUTRAL)
    p.update(kw)
    return p


GUARD = P(root=(0, -1.6, 0), body=(4, 20, 0), chest=(8, -6, 0), look=(0, 0, 0),
          sword=((0.35, -1, -0.35), (0.1, -0.35, -1), (-0.15, 0.85, -0.9), 0),
          shield_arm=((-0.1, -0.8, -0.8), (0.7, -0.15, -0.8)), shield=(-4, 12, 0),
          r_place=(2.6, 3.5, 0, 35, 0), l_place=(2.4, -3.5, 0, 10, 0))


# the sword arm: (upper arm, forearm, blade, twist of the grip about the blade in degrees)
S_GUARD = GUARD['sword']
S_LOAD = ((0.88, 0.29, 0.37), (0.16, 0.96, 0.25), (0.07, 0.25, 0.97), 0)
S_WIND = ((0.65, 0.65, 0.3), (-0.35, 0.9, 0.45), (-0.4, -0.2, 1), 0)
S_CUT = ((0.55, 0.7, -0.45), (0.3, 0.75, -0.6), (0.15, 0.95, -0.3), 0)   # the arc passes outside the helm
S_CONTACT = ((0.0, -0.15, -1), (-0.3, -0.45, -1), (-0.5, -0.65, -1), 0)
S_FOLLOW = ((-0.59, -0.59, -0.55), (-0.81, -0.47, -0.35), (-0.9, -0.13, -0.41), 0)
S_SETTLE = ((-0.38, -0.51, -0.78), (-0.61, -0.35, -0.71), (-0.89, -0.46, -0.01), 0)
S_GATHER = ((-0.26, -0.16, -0.95), (-0.42, 0.08, -0.9), (-0.64, -0.43, -0.64), 0)   # elbow bent up, blade low by the wrist
S_RISE = ((0.2, -0.35, -1), (0.45, 0.15, -1), (0.55, 0.45, -1), 0)
S_RISEHIT = ((0.65, 0.35, -0.6), (0.6, 0.8, -0.35), (0.6, 0.9, 0.15), 0)
# the shoulder guard: the fist by the right ear, the blade up and back over the shoulder, clear of the helm
S_OX = ((0.97, 0.11, 0.24), (0.17, 0.97, 0.16), (0.06, 0.26, 0.96), 0)
S_OX_BACK = ((0.96, 0.09, 0.25), (0.23, 0.95, 0.21), (0.0, 0.04, 1), 0)
S_BROKEN = ((0.8, -0.4, 0.45), (0.65, 0.45, 0.25), (0.35, 0.9, 0.45), 0)
S_LOW = ((0.45, -1, 0.3), (0.2, -0.2, -1), (0.0, 0.5, -1), 0)
S_COCKED = ((0.67, -0.74, 0.09), (0.39, 0.0, -0.92), (-0.04, 0.77, -0.64), 0)

# the shield arm: (upper arm, forearm)
H_GUARD = GUARD['shield_arm']
H_REACH = ((-0.2, -0.65, -1), (0.55, 0.2, -1))
H_TUCK = ((-0.55, -0.85, 0.0), (-0.1, -0.55, -0.8))       # low and out to the left: the off-arm's counterweight
H_CHAMBER = ((-0.5, -0.82, 0.28), (0.21, -0.12, -0.97))
# the shield arm swung wide and low on the left, the off-arm's counterweight through a big cut
H_WIDE = ((-0.71, -0.66, -0.24), (-0.56, -0.01, -0.83))
H_WIDER = ((-0.9, -0.2, -0.4), (-0.45, 0.15, -0.88))
H_DRIVE = ((-0.48, -0.79, -0.39), (0.35, 0.19, -0.92))   # the bash's step: the fist driving forward
H_PUNCH = ((-0.1, -0.3, -1), (0.1, 0.1, -1))
H_BRACE = ((-0.1, -0.45, -1), (0.8, 0.3, -0.6))
H_DRIVEN = ((-0.17, -0.93, -0.34), (0.79, 0.11, -0.61))
H_FLUNG = ((-0.65, -0.7, 0.3), (-0.25, -0.55, -0.6))

R_GUARD, L_GUARD = GUARD['r_place'], GUARD['l_place']


def _blend(a, b, k):
    """An in-between arm aim, k of the way from a to b."""
    return tuple(tuple((1 - k) * x + k * y for x, y in zip(CR._unit(u), CR._unit(v))) for u, v in zip(a[:3], b[:3])) + \
        ((1 - k) * a[3] + k * b[3],)


def combo_timeline():
    return seq(GUARD, [
        (0.00, 'guard', {}),
        (0.12, 'load', dict(root=(0, -2.2, 0.8), body=(0, 34, 0), chest=(-2, 12, -4), sword=S_LOAD,
                            shield_arm=H_REACH, shield=(0, 4, 0))),
        (0.22, 'wind-up', dict(root=(0, -2.4, 1.1), body=(-3, 42, 0), chest=(-6, 16, -6), sword=S_WIND)),
        (0.26, 'wind-up hold', dict(root=(0, -2.5, 1.2), body=(-4, 43, 0), chest=(-7, 17, -6))),
        (0.32, 'cut', dict(root=(0, -2.2, -0.6), body=(6, 12, 0), chest=(6, -4, 2), sword=S_CUT, shield_arm=H_TUCK,
                           shield=(0, -10, 0), r_place=(2.6, 3.5, 0, 35, 14), l_place=(2.5, -5.2, 1.3, 9, -6))),
        (0.40, 'contact', dict(root=(0, -2.8, -2.0), body=(14, -14, 0), chest=(16, -16, 4), sword=S_CONTACT, shield_arm=H_WIDE,
                               shield=(0, -25, 0), r_place=(2.6, 3.5, 0, 35, 24), l_place=(2.6, -6.5, 0, 8, 0))),
        (0.50, 'follow-through', dict(root=(0, -3.0, -2.3), body=(16, -24, 0), chest=(18, -20, 6), sword=S_FOLLOW,
                                      shield_arm=H_WIDER,
                                      r_place=(2.6, 3.5, 0, 35, 20))),
        (0.62, 'settle', dict(root=(0, -2.6, -2.2), body=(12, -18, 0), chest=(12, -14, 4), sword=S_SETTLE,
                              r_place=(2.6, 3.5, 0, 35, 6))),
        (0.68, 'drag the back foot', dict(root=(0, -2.5, -2.7), r_place=(2.6, 2.0, 1.0, 30, 10))),
        (0.75, 'gather', dict(root=(0, -2.8, -3.0), body=(12, -26, 0), chest=(14, -18, 4), sword=S_GATHER,
                              r_place=(2.6, 0.5, 0, 35, 0))),
        (0.84, 'rising cut', dict(root=(0, -2.2, -3.0), body=(4, 6, 0), chest=(2, 10, -4), sword=S_RISE,
                                  shield_arm=H_GUARD, shield=(-4, 12, 0))),
        (0.90, 'rising contact', dict(root=(0, -1.8, -3.0), body=(-2, 26, 0), chest=(-6, 18, -6), sword=S_RISEHIT)),
        (1.02, 'high guard', dict(root=(0, -1.9, -3.0), body=(0, 30, 0), chest=(2, 6, -2), sword=S_OX)),
        (1.12, 'bash load', dict(root=(0, -2.6, -2.2), body=(2, 42, 0), chest=(-2, 14, 0), sword=S_OX_BACK,
                                 shield_arm=H_CHAMBER, shield=(0, 30, 0))),
        (1.17, 'bash step', dict(root=(0, -2.3, -3.4), body=(6, 22, 0), chest=(6, 0, 2), shield_arm=H_DRIVE,
                                 r_place=(2.6, 0.5, 0, 35, 14),
                                 l_place=(2.6, -7.5, 1.2, 8, -6))),
        (1.22, 'bash', dict(root=(0, -2.4, -4.5), body=(10, 4, 0), chest=(14, -14, 2), sword=S_OX,
                            shield_arm=H_PUNCH, shield=(-8, 0, 0), r_place=(2.6, 0.5, 0, 35, 24),
                            l_place=(2.6, -8.5, 0, 8, 0))),
        (1.32, 'bash hold', dict(root=(0, -2.6, -4.8), body=(12, 0, 0), chest=(14, -16, 2), r_place=(2.6, 0.5, 0, 35, 18))),
        (1.42, 'back foot back', dict(root=(0, -2.2, -3.8), body=(8, 10, 0), chest=(10, -8, 0), shield_arm=H_GUARD,
                                      shield=(-4, 12, 0), r_place=(2.6, 2.0, 1.2, 30, 0))),
        (1.52, 'plant', dict(root=(0, -2.0, -2.6), sword=_blend(S_OX, S_GUARD, 0.4), r_place=R_GUARD)),
        (1.60, 'lead foot back', dict(root=(0, -1.8, -1.2), body=(5, 18, 0), chest=(8, -6, 0),
                                      sword=_blend(S_OX, S_GUARD, 0.8), l_place=(2.45, -5.5, 1.0, 10, 0))),
        (1.70, 'plant', dict(root=(0, -1.7, -0.2), sword=S_GUARD, l_place=L_GUARD)),
        (1.85, 'settle', dict(root=(0, -1.5, 0.1), body=(4, 18, 0), chest=(7, -5, 0))),
        (2.10, 'guard', {k: GUARD[k] for k in ('root', 'body', 'chest', 'sword', 'shield_arm', 'shield')}),
    ])


def combo():
    return build('combat_combo', 2.10, False, combo_timeline(),
                 lags={'forearm': 0.015, 'hand': 0.03, 'shield_forearm': 0.02})


def idle_timeline():
    """3.2 s: the weight rocks onto the lead foot and back (the root sways over it, the hips roll), two breaths lift the
    chest, the point hunts in a slow circle, the shield settles, the eyes check left then right; at 2.0 s the wrist
    rolls the blade and resets the grip. It starts and ends on GUARD, the pose every strike clip returns to."""
    L, n = 3.2, 16
    steps = []
    for i in range(n + 1):
        t = round(L * i / n, 4)
        a = 2 * math.pi * i / n
        sway = math.sin(a)                              # +1: weight on the lead foot
        breath = math.sin(2 * a)                        # two breaths a loop
        circle = (0.12 * math.sin(a), 0.10 * (math.cos(a) - 1))   # starts on the guard's point
        bx, by, bz = S_GUARD[2]
        blade = (bx + circle[0], by + circle[1], bz)
        twist = 0.0
        if abs(t - 2.0) < 0.21:                         # the grip reset: the wrist rolls the blade a little and back
            twist = 35 * (1 - abs(t - 2.0) / 0.2)
        look = (2 * math.sin(a), 0.0, 0.0)
        if 0.8 <= t <= 1.4:
            look = (2, -22 * math.sin(math.pi * (t - 0.8) / 0.6), -3 * math.sin(math.pi * (t - 0.8) / 0.6))
        if 2.2 <= t <= 2.8:
            look = (1, 16 * math.sin(math.pi * (t - 2.2) / 0.6), 2 * math.sin(math.pi * (t - 2.2) / 0.6))
        steps.append((t, 'breath' if i % 4 else 'weight %s' % ('forward' if sway > 0.5 else 'back' if sway < -0.5 else 'centre'),
                      dict(root=(0.45 * sway, -1.6 - 0.12 * breath - 0.2 * abs(sway), -0.5 * sway),
                           body=(4 + 1.5 * sway, 20 + 2.5 * sway, -2.5 * sway),
                           chest=(8 - 1.5 * breath, -6 - 1.5 * sway, 1.5 * sway),
                           sword=(S_GUARD[0], S_GUARD[1], blade, twist),
                           shield_arm=((H_GUARD[0][0], H_GUARD[0][1] + 0.04 * breath, H_GUARD[0][2]), H_GUARD[1]),
                           shield=(-4 + 1.5 * math.sin(a), 12 + 3 * sway, 1.5 * math.sin(a)),
                           look=look,
                           r_place=(2.6, 3.5, 0, 35, max(0.0, 6 * sway)),
                           l_place=L_GUARD)))
    return steps and seq(GUARD, steps)


def idle():
    return build('combat_idle', 3.2, True, idle_timeline(),
                 lags={'forearm': 0.05, 'hand': 0.1, 'shield_forearm': 0.08})


ADVANCE_SPEED = 8.0     # px a second (half a block): a cautious advance in guard


def advance_timeline():
    """1.2 s, the ground sliding back under planted feet at ADVANCE_SPEED: the lead foot steps (heel strike at 0.30),
    the back foot drags up behind it (0.60 to 0.85). The root dips on each landing and rises over the planted foot,
    sways over whichever foot holds the weight, and the hips roll and turn with the stride while the guard rides steady
    above them."""
    v = ADVANCE_SPEED
    def lead(t):   # planted 0.30 -> 1.20 at -7.0 + v (t - 0.30)
        return -7.0 + v * (t - 0.30)
    def back(t):   # planted 0.85 -> 1.80 at -0.8 + v (t - 0.85)
        t = t if t >= 0.85 else t + 1.2
        return -0.8 + v * (t - 0.85)
    L_ = lambda z, lift=0.0, heel=0.0: (2.4, z, lift, 10, heel)
    R_ = lambda z, lift=0.0, heel=0.0: (2.6, z, lift, 35, heel)
    return seq(GUARD, [
        (0.00, 'lead foot lifts', dict(root=(-0.4, -1.5, 0.3), body=(4, 22, 2), chest=(8, -7, -1),
                                       l_place=L_(lead(1.2), 0, 18), r_place=R_(back(0.0)))),
        (0.15, 'lead foot swings', dict(root=(-0.5, -1.3, -0.4), body=(5, 20, 3), l_place=L_(-3.4, 1.4, 0),
                                        r_place=R_(back(0.15), 0, 8))),
        (0.30, 'heel strike', dict(root=(-0.2, -1.6, -0.6), body=(6, 18, 1), chest=(9, -5, 0),
                                   l_place=L_(lead(0.30), 0, -12), r_place=R_(back(0.30), 0, 14))),
        (0.40, 'weight forward', dict(root=(0.2, -2.1, -0.2), body=(5, 17, -1), chest=(8, -5, 1),
                                      l_place=L_(lead(0.40)), r_place=R_(back(0.40), 0, 16))),
        (0.60, 'back foot lifts', dict(root=(0.45, -1.7, 0.2), body=(4, 19, -2), chest=(8, -6, 1),
                                       l_place=L_(lead(0.60)), r_place=R_(back(0.60), 0, 24))),
        (0.72, 'back foot drags', dict(root=(0.5, -1.4, 0.4), body=(3, 21, -3), chest=(7, -7, 2),
                                       l_place=L_(lead(0.72)), r_place=R_(3.0, 0.9, 6))),
        (0.85, 'back foot plants', dict(root=(0.1, -1.8, 0.4), body=(4, 22, 0), chest=(8, -7, 0),
                                        l_place=L_(lead(0.85)), r_place=R_(back(0.85)))),
        (1.00, 'settle', dict(root=(-0.2, -1.7, 0.3), body=(4, 22, 1), chest=(8, -7, -1),
                              l_place=L_(lead(1.00)), r_place=R_(back(1.00)))),
        (1.20, 'lead foot lifts', dict(root=(-0.4, -1.5, 0.3), body=(4, 22, 2), chest=(8, -7, -1),
                                       l_place=L_(lead(1.2), 0, 18), r_place=R_(back(1.2)))),
    ])


def advance():
    return build('combat_advance', 1.2, True, advance_timeline(),
                 lags={'forearm': 0.05, 'hand': 0.09, 'shield_forearm': 0.06})


def block_timeline():
    return seq(GUARD, [
        (0.00, 'guard', {}),
        (0.10, 'brace', dict(root=(0, -2.4, 0.2), body=(6, 26, 0), chest=(14, -4, 0), shield_arm=H_BRACE,
                             shield=(-6, 6, 0), sword=S_COCKED, look=(4, 0, 0))),
        (0.18, 'braced', dict(root=(0, -2.5, 0.2), chest=(15, -4, 0))),
        (0.23, 'impact', dict(root=(0, -2.8, 1.2), body=(-2, 30, 0), chest=(4, 0, -3), shield_arm=H_DRIVEN,
                              shield=(8, 10, -4), look=(-8, 6, 4), r_place=(2.6, 3.5, 0, 35, 0),
                              l_place=(2.4, -3.5, 0, 10, -14))),
        (0.28, 'absorb', dict(root=(0, -3.1, 1.6), body=(0, 28, 0), chest=(8, -2, -2), shield=(5, 9, -2),
                              look=(-2, 3, 2))),
        (0.34, 'skid', dict(root=(0, -2.9, 1.9), l_place=(2.4, -2.6, 0.6, 10, 0))),
        (0.42, 'plant', dict(root=(0, -2.5, 1.8), body=(4, 26, 0), chest=(12, -4, 0), shield_arm=H_BRACE,
                             shield=(-4, 8, 0), look=(2, 0, 0), l_place=(2.4, -1.8, 0, 10, 0))),
        (0.52, 'shove', dict(root=(0, -2.2, 1.2), chest=(14, -6, 0), shield=(-8, 6, 0))),
        (0.64, 'step back in', dict(root=(0, -1.9, 0.6), l_place=(2.4, -2.8, 1.0, 10, 0), sword=S_GUARD,
                                    shield_arm=H_GUARD, shield=(-4, 12, 0))),
        (0.74, 'plant', dict(root=(0, -1.8, 0.1), body=(4, 21, 0), chest=(9, -6, 0), look=(0, 0, 0), l_place=L_GUARD)),
        (0.86, 'settle', dict(root=(0, -1.5, -0.1), body=(4, 19, 0), chest=(7, -6, 0))),
        (1.00, 'guard', {k: GUARD[k] for k in ('root', 'body', 'chest')}),
    ])


def block():
    return build('combat_block', 1.0, False, block_timeline(),
                 lags={'forearm': 0.02, 'hand': 0.04, 'shield_forearm': 0.01})


def hit_timeline():
    return seq(GUARD, [
        (0.00, 'guard', {}),
        (0.06, 'struck', dict(root=(0, -1.4, 1.0), body=(-6, 26, 4), chest=(-14, 10, 6), sword=S_BROKEN,
                              shield_arm=H_FLUNG, shield=(12, -25, 10), look=(-20, 14, 12))),
        (0.12, 'reel', dict(root=(0, -1.6, 1.8), body=(-8, 28, 5), chest=(-12, 12, 6), look=(-16, 18, 10),
                            r_place=(2.8, 5.0, 1.0, 35, 0))),
        (0.22, 'catch', dict(root=(0, -2.4, 2.4), body=(-2, 24, 2), chest=(-2, 8, 2), look=(-6, 8, 4),
                             r_place=(2.8, 6.5, 0, 40, 0))),
        (0.34, 'fold', dict(root=(0, -2.8, 2.3), body=(6, 22, 0), chest=(14, -2, 0), look=(10, 0, 0),
                            shield_arm=H_TUCK, shield=(4, 10, 0), sword=S_LOW)),
        (0.45, 'gather', dict(root=(0, -2.4, 2.0), body=(5, 21, 0), chest=(10, -4, 0), look=(4, 0, 0),
                              shield_arm=H_GUARD, shield=(-4, 12, 0))),
        (0.58, 'step up', dict(root=(0, -2.0, 1.2), r_place=(2.7, 5.0, 0.9, 35, 0), sword=S_GUARD, look=(0, 0, 0))),
        (0.68, 'plant', dict(root=(0, -1.8, 0.3), body=(4, 20, 0), chest=(8, -6, 0), r_place=R_GUARD)),
        (0.78, 'settle', dict(root=(0, -1.5, -0.1), body=(4, 19, 0), chest=(7, -5, 0))),
        (0.90, 'guard', {k: GUARD[k] for k in ('root', 'body', 'chest')}),
    ])


def hit_react():
    return build('combat_hit', 0.9, False, hit_timeline(),
                 lags={'forearm': 0.03, 'hand': 0.05, 'shield_forearm': 0.03})


CLIPS = [idle, advance, combo, block, hit_react]

POSES = {'neutral': NEUTRAL, 'guard': GUARD}
