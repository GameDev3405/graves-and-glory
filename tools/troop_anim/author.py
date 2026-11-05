"""Chained-pose timelines, solved per tick, baked to tick keys and written into art/clips/<rig>/<family>.bbmodel.

A recipe (recipes.py) is a list of keys, each a time and a partial pose; whatever a key leaves out it keeps from the key
before (the first key starts from the stance). The pose is hand-agnostic: 'weapon' and 'off' name the arms and feet by
what they hold, and every sideways quantity is mirrored for a left-handed troop.

  root   (side, down, forward) px offset of the root from the stance (side + toward the weapon side; down + lowers)
  hips   (pitch, yaw, roll) deg on the stance root; body and chest likewise on the stance body and chest (the chest folds
         into the body while the rig has no chest bone). Pitch + leans forward; yaw + turns the weapon shoulder forward;
         roll + drops the weapon shoulder... all mirrored for a left-handed troop.
  weapon, off   arm aims: 'upper', 'fore', 'item' as (elevation, azimuth) deg in the arm's parent frame (the chest, else
         the body): elevation + up, azimuth 0 forward and + outward on that arm's side; 'twist' deg turns the item about
         itself from the natural grip (flats square to the forearm's sides); 'on' (along, side, back) px and 'grab' 0..1
         put the fist on the other hand's item (a two-handed haft, a bowstring) instead; 'stance' resets to the stance.
  look   (elevation, azimuth) of the gaze in model space (azimuth + toward the weapon side), or 'stance' (the head keeps
         the stance's gaze while the body turns under it)
  feet   {'weapon'|'off': (out, forward, lift, toe)} px and deg from the stance's plant (out + away from the centre)
  extra  {bone: (rx, ry, rz)} raw channel offsets added last (cloth, tails, a non-biped's parts)
  interp 'pchip' (default; monotone, never overshoots a hold) or 'linear', for the segment from this key to the next
  stance True: this key is the stance exactly (the clip stores the stance's zero there)

Per tick: interpolate, then root/hips/body/chest -> forward kinematics -> legs by IK keeping each sole's lowest corner
where the stance has it (less `lift`) -> the weapon arm, then the off arm, by aim or reach with clearance pushes -> head
look -> lags -> net - stance(0), which is what the clip stores (the game adds the stance back).
"""
import copy
import json
import math
import subprocess
import sys
import uuid

import numpy as np

from . import clearance as C
from . import model as M
from . import pose as P
from . import solve as S

TICK = 0.05
ROT_ROUND, POS_ROUND = 2, 3
NS = uuid.UUID('6f0a6d8e-4b1e-4c55-9a51-9f7f1c0d2b17')
AIMS = ('upper', 'fore', 'item')


def sph(el, az, out):
    """A unit vector: elevation el (+ up), azimuth az (0 forward, + toward the side whose x sign is out)."""
    e, a = math.radians(el), math.radians(az)
    return np.array([out * math.cos(e) * math.sin(a), -math.sin(e), -math.cos(e) * math.cos(a)])


def unsph(v, out):
    v = S.unit(v)
    el = math.degrees(math.asin(max(-1.0, min(1.0, -v[1]))))
    az = math.degrees(math.atan2(out * v[0], -v[2]))
    return el, az


def slerp(a, b, k):
    a, b = S.unit(a), S.unit(b)
    w = math.acos(max(-1.0, min(1.0, float(np.dot(a, b)))))
    if w < 1e-6:
        return a
    return (math.sin((1 - k) * w) * a + math.sin(k * w) * b) / math.sin(w)


class Rig:
    """A troop's authoring view: which bones exist, the sides, the stance under the clip and its pose."""

    def __init__(self, troop, clip, stance='default'):
        self.troop = t = troop
        self.clip = clip
        key = P.default_stance(t, clip) if stance == 'default' else stance
        self.stance_key = key
        self.under = P.stance_of(t.rig, t.family, key) if key else None
        self.stance_ch = {b: list(v) for b, v in P.channels(None, 0.0, self.under).items()}
        self.w = -1.0 if t.hand == 'right' else 1.0                 # the weapon side's x sign
        self.m = 1.0 if t.hand == 'right' else -1.0                 # yaw and roll mirror
        self.sides = {'weapon': t.hand, 'off': t.off_hand}
        self.chest = 'chest' if 'chest' in t.by else 'body'
        # the stance as the clip sees it: un-grounded (the game grounds stance + clip together), with carries
        self.stance_local = P.local(t, self.stance_ch, None, self.under, False, True)
        self.stance_W = t.fk(self.stance_local)
        self.stance_net = {b: np.array(r, float) for b, (r, v) in self.stance_local.items()}
        self.zero = {}                                               # the clip value that reproduces the stance
        for b, (r, v) in self.stance_local.items():
            z = r - t.rest[b][0] - np.array(self.stance_ch.get(b, [0.0] * 6)[:3])
            if np.any(np.abs(z) > 1e-9):
                self.zero[b] = np.concatenate([z, np.zeros(3)])
        self.arms = {}
        for s in ('right', 'left'):
            if f'{s}_arm' in t.by:
                self.arms[s] = S.Arm(t, s)
        self.legs = {s: S.Leg(t, s) for s in ('right', 'left') if f'{s}_leg' in t.by and f'{s}_shin' in t.by}
        self.plants = {s: self.sole(self.stance_W, s) for s in self.legs}
        self.low = {s: P.lowest(self.stance_ch, 0 if s == 'right' else 1) for s in self.legs}
        self.head = 'head' if 'head' in t.by else None
        self.gaze = self.stance_W[self.head][:3, :3] @ np.array([0.0, 0.0, -1.0]) if self.head else None
        self.reset()

    def reset(self):
        for a in self.arms.values():
            a.seed(self.stance_net)
        for leg in self.legs.values():
            leg.seed(self.stance_net)

    def out(self, side):
        return -1.0 if side == 'right' else 1.0

    def sole(self, W, side):
        return (W[f'{side}_shin'] @ np.array([0.0, S.SOLE, 0.0, 1.0]))[:3]

    def stance_aims(self, which):
        side = self.sides[which]
        a = self.arms.get(side)
        if a is None:
            return {}
        am = a.aims(self.stance_net)
        out = {k: unsph(am[k], self.out(side)) for k in AIMS if k in am}
        if 'twist' in am:
            out['twist'] = am['twist']
        out['on'], out['grab'] = (0.0, 0.0, 0.0), 0.0
        return out


def _chain(rig, keys):
    """Every key filled forward to a full pose."""
    full = []
    cur = {'root': (0.0, 0.0, 0.0), 'hips': (0.0, 0.0, 0.0), 'body': (0.0, 0.0, 0.0), 'chest': (0.0, 0.0, 0.0),
           'look': 'stance', 'extra': {}, 'feet': {'weapon': (0.0, 0.0, 0.0, 0.0), 'off': (0.0, 0.0, 0.0, 0.0)},
           'weapon': rig.stance_aims('weapon'), 'off': rig.stance_aims('off')}
    first = copy.deepcopy(cur)
    for k in keys:
        if k.get('stance'):
            nxt = copy.deepcopy(first)
        else:
            nxt = copy.deepcopy(cur)
        for f, v in (k.get('pose') or {}).items():
            if f in ('weapon', 'off'):
                if v == 'stance':
                    nxt[f] = dict(first[f])
                else:
                    for a, x in v.items():
                        nxt[f][a] = float(x) if a in ('twist', 'grab') else tuple(float(y) for y in x)
            elif f == 'feet':
                nxt['feet'] = dict(nxt['feet'])
                nxt['feet'].update({s: tuple(float(y) for y in x) for s, x in v.items()})
            elif f == 'extra':
                nxt['extra'] = {b: tuple(float(y) for y in x) for b, x in v.items()}
            elif f == 'look':
                nxt['look'] = v if v == 'stance' else tuple(float(y) for y in v)
            else:
                nxt[f] = tuple(float(y) for y in v)
        full.append((float(k['t']), nxt, k.get('interp', 'pchip'), bool(k.get('stance'))))
        cur = nxt
    if not full or full[0][0] > 0:
        full.insert(0, (0.0, copy.deepcopy(first), 'pchip', True))
    return full


def _interp(times, values, interps, t, slopes=None):
    """PCHIP through the keys (or linear on a segment its key marks linear), held past the ends. slopes: the PCHIP
    slopes to use (Timeline zeroes them at a once clip's ends, so the motion eases out of and into the stance)."""
    if t <= times[0]:
        return float(values[0])
    if t >= times[-1]:
        return float(values[-1])
    k = int(np.searchsorted(times, t, side='right') - 1)
    h = times[k + 1] - times[k]
    s = (t - times[k]) / h
    if interps[k] == 'linear':
        return float(values[k] + (values[k + 1] - values[k]) * s)
    m = S.pchip_slopes(times, values) if slopes is None else slopes
    h00, h10, h01, h11 = 2 * s ** 3 - 3 * s ** 2 + 1, s ** 3 - 2 * s ** 2 + s, -2 * s ** 3 + 3 * s ** 2, s ** 3 - s ** 2
    return float(h00 * values[k] + h10 * h * m[k] + h01 * values[k + 1] + h11 * h * m[k + 1])


def _slerp_R(A, B, w):
    """The rotation w of the way from A to B (shortest arc)."""
    D = A.T @ B
    c = max(-1.0, min(1.0, (np.trace(D) - 1) / 2))
    ang = math.acos(c)
    if ang < 1e-9:
        return A
    v = np.array([D[2, 1] - D[1, 2], D[0, 2] - D[2, 0], D[1, 0] - D[0, 1]])
    if np.linalg.norm(v) < 1e-9:                      # a half turn: any axis of D's +1 eigenvector
        val, vec = np.linalg.eigh((D + D.T) / 2)
        v = vec[:, int(val.argmax())]
    v = v / np.linalg.norm(v)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    a = ang * w
    return A @ (np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * K @ K)


def _unwrap(vals):
    out = [float(vals[0])]
    for v in vals[1:]:
        out.append(v + 360.0 * round((out[-1] - v) / 360.0))
    return np.array(out)


def _free_poles(full, which, aim):
    """An aim straight up or down has no azimuth: give it its neighbour's, so the arm rises without a spiral."""
    vals = [p[which][aim] for _, p, _, _ in full]
    pole = [abs(abs(el) - 90.0) < 0.5 for el, _ in vals]
    for k, is_pole in enumerate(pole):
        if not is_pole:
            continue
        nb = next((vals[j] for j in range(k + 1, len(vals)) if not pole[j]), None) or next(
            (vals[j] for j in range(k - 1, -1, -1) if not pole[j]), None)
        if nb is not None:
            full[k][1][which][aim] = (vals[k][0], nb[1])


class Timeline:
    """A recipe's keys as continuous tracks."""

    def __init__(self, rig, keys, ease_ends=True):
        self.rig = rig
        self.full = f = _chain(rig, keys)
        self.slopes = {}
        self.times = np.array([t for t, _, _, _ in f])
        self.interps = [i for _, _, i, _ in f]
        self.tracks = {}

        def add(name, get, wrap=False):
            v = np.array([float(get(p)) for _, p, _, _ in f])
            v = _unwrap(v) if wrap else v
            self.tracks[name] = v
            m = S.pchip_slopes(self.times, v) if len(v) > 1 else np.zeros(len(v))
            if ease_ends and len(v) > 1:
                m[0] = m[-1] = 0.0
            for k in range(len(v)):                     # a stance key is a rest: the motion settles there
                if f[k][3]:
                    m[k] = 0.0
            self.slopes[name] = m
        for g in ('root', 'hips', 'body', 'chest'):
            for i in range(3):
                add(f'{g}.{i}', lambda p, g=g, i=i: p[g][i])
        for which in ('weapon', 'off'):
            have = f[0][1][which]
            for aim in AIMS:
                if aim in have:
                    _free_poles(f, which, aim)
                    add(f'{which}.{aim}.el', lambda p, w=which, a=aim: p[w][a][0])
                    add(f'{which}.{aim}.az', lambda p, w=which, a=aim: p[w][a][1], wrap=True)
            if 'twist' in have:
                add(f'{which}.twist', lambda p, w=which: p[w]['twist'], wrap=True)
            if have:
                add(f'{which}.grab', lambda p, w=which: p[w]['grab'])
                for i in range(3):
                    add(f'{which}.on.{i}', lambda p, w=which, i=i: p[w]['on'][i])
        for s in ('weapon', 'off'):
            for i in range(4):
                add(f'feet.{s}.{i}', lambda p, s=s, i=i: p['feet'][s][i])
        self.look_stance = all(p['look'] == 'stance' for _, p, _, _ in f)
        if not self.look_stance and rig.gaze is not None:
            g0 = unsph(rig.gaze, rig.w)
            add('look.el', lambda p: (g0 if p['look'] == 'stance' else p['look'])[0])
            add('look.az', lambda p: (g0 if p['look'] == 'stance' else p['look'])[1], wrap=True)
        self.extra_bones = sorted({b for _, p, _, _ in f for b in p['extra']})
        for b in self.extra_bones:
            for i in range(3):
                add(f'extra.{b}.{i}', lambda p, b=b, i=i: p['extra'].get(b, (0.0, 0.0, 0.0))[i])

    def has(self, name):
        return name in self.tracks

    def at(self, name, t):
        return _interp(self.times, self.tracks[name], self.interps, t, self.slopes[name])

    def vec(self, name, t, n=3):
        return np.array([self.at(f'{name}.{i}', t) for i in range(n)])

    def aims(self, which, t):
        side = self.rig.sides[which]
        out = {}
        for aim in AIMS:
            if self.has(f'{which}.{aim}.el'):
                out[aim] = sph(self.at(f'{which}.{aim}.el', t), self.at(f'{which}.{aim}.az', t), self.rig.out(side))
        if self.has(f'{which}.twist'):
            out['twist'] = self.at(f'{which}.twist', t)
        return out

    def stance_times(self):
        return [kt for kt, _, _, st in self.full if st]


class Baker:
    """Solves a recipe per tick into net channels, then into the clip's channels."""

    def __init__(self, troop, recipe, step=TICK):
        self.troop, self.recipe = troop, recipe
        self.rig = Rig(troop, recipe['clip'], recipe.get('stance', 'default'))
        self.tl = Timeline(self.rig, recipe['keys'], ease_ends=not recipe.get('loop'))
        self.length = float(recipe['length'])
        self.loop = bool(recipe.get('loop'))
        self.step = step
        self.n = int(round(self.length / step)) + 1
        self.times = np.round(np.arange(self.n) * step, 6)
        self.clipper = C.Clipper(troop) if recipe.get('push', True) else None
        self.reach_now = {}

    # ---- one tick
    def _upper(self, t):
        r = self.rig
        ch = {b: list(v) for b, v in r.stance_ch.items()}
        root = self.tl.vec('root', t)
        d = (r.w * root[0], root[1], -root[2])
        mir = np.array([1.0, r.m, r.m])
        rc = ch.setdefault('root', [0.0] * 6)
        hips, body, chest = (self.tl.vec(g, t) * mir for g in ('hips', 'body', 'chest'))
        for i in range(3):
            rc[i] += hips[i]
            rc[3 + i] += d[i]
        bc = ch.setdefault('body', [0.0] * 6)
        for i in range(3):
            bc[i] += body[i] + (0.0 if r.chest == 'chest' else chest[i])
        if r.chest == 'chest':
            cc = ch.setdefault('chest', [0.0] * 6)
            for i in range(3):
                cc[i] += chest[i]
        return ch

    def _local(self, ch):
        return P.local(self.troop, ch, None, None, False, False)

    def _set(self, ch, bone, net):
        cc = ch.setdefault(bone, [0.0] * 6)
        rest = self.troop.rest[bone][0]
        for i in range(3):
            cc[i] = float(net[i]) - float(rest[i])

    def tick(self, t, push=None, state=None):
        """{bone: 6 channels} (stance + clip, before the game grounds it) at t; push {'weapon'|'shield': c}."""
        r, tr = self.rig, self.troop
        if state:
            for s, st in state['arms'].items():
                r.arms[s].prev = copy.deepcopy(st)
            for s, x in state['legs'].items():
                r.legs[s].x = x.copy()
        ch = self._upper(t)
        W = tr.fk(self._local(ch))
        reach = {}
        for which, side in r.sides.items():                      # legs
            leg = r.legs.get(side)
            if leg is None:
                continue
            o, fwd, lift, toe = self.tl.vec(f'feet.{which}', t, 4)
            target = r.plants[side] + np.array([r.out(side) * o, 0.0, -fwd])
            yaw = r.stance_net[leg.leg][1] - r.out(side) * toe
            want = r.low[side] - lift
            for _ in range(6):
                (lp, ly, lr), knee, res = leg.solve(W['root'], target, yaw)
                self._set(ch, leg.leg, (lp, ly, lr))
                self._set(ch, leg.shin, (knee, r.stance_net[leg.shin][1], r.stance_net[leg.shin][2]))
                low = P.lowest(ch, 0 if side == 'right' else 1)
                if abs(low - want) < 1e-3:
                    break
                target[1] -= low - want
            reach[f'{side}_leg'] = res
            if leg.foot:                                        # a foot bone keeps the sole level
                fc = ch.setdefault(leg.foot, [0.0] * 6)
                fc[0] = -(lp + knee) - tr.rest[leg.foot][0][0] + r.stance_net[leg.foot][0] + (
                    r.stance_net[leg.leg][0] + r.stance_net[leg.shin][0])
        W = None
        for which in ('weapon', 'off'):                          # arms: the weapon's first, the off hand may reach it
            side = r.sides[which]
            arm = r.arms.get(side)
            if arm is None or not self.tl.has(f'{which}.upper.el'):
                continue
            aims = self.tl.aims(which, t)
            grab = self.tl.at(f'{which}.grab', t) if self.tl.has(f'{which}.grab') else 0.0
            if grab > 1e-4:
                other = r.arms.get(r.sides['weapon' if which == 'off' else 'off'])
                W = tr.fk(self._local(ch))
                on = self.tl.vec(f'{which}.on', t) * np.array([1.0, r.m, 1.0])
                hand = other.hand if other.has_hand else other.arm
                if other.tip is not None:
                    n = np.cross(other.tip, other.edge)
                    p_hand = other.grip + on[0] * other.tip + on[1] * other.edge + on[2] * n
                else:
                    p_hand = other.grip + on
                g = W[hand] @ np.append(p_hand, 1.0)
                par = tr.parent[arm.arm]
                tgt = (np.linalg.inv(W[par]) @ g)[:3]
                hint = np.array([r.out(side), 0.4, 0.6])
                u, f, res = arm.reach(tgt, hint)
                reach[arm.arm] = res * grab
                aims['upper'] = slerp(aims['upper'], u, grab)
                aims['fore'] = slerp(aims['fore'], f, grab)
                if other.tip is not None and 'item' in aims:
                    held = W[hand][:3, :3] @ other.tip
                    aims['item'] = slerp(aims['item'], np.linalg.inv(W[par])[:3, :3] @ held, grab)
            item = 'weapon' if which == 'weapon' else 'shield'
            c = (push or {}).get(item, 0.0)
            if c:
                aims = dict(aims)
                tw = aims.pop('twist', None)
                aims = C.pushed(aims, C.push(tr.family, item, side), c)
                if tw is not None:
                    aims['twist'] = tw
            net = arm.solve(aims['upper'], aims.get('fore'), aims.get('item'), twist=aims.get('twist'))
            for b, e in net.items():
                self._set(ch, b, e)
        if r.head and r.gaze is not None:                          # head
            g = r.gaze if self.tl.look_stance else sph(self.tl.at('look.el', t), self.tl.at('look.az', t), r.w)
            W = tr.fk(self._local(ch))
            hp = S.head_look(W[tr.parent[r.head]][:3, :3], g)
            self._set(ch, r.head, (hp[0], hp[1], r.stance_net[r.head][2]))
        for b in self.tl.extra_bones:
            if b in tr.by:
                cc = ch.setdefault(b, [0.0] * 6)
                e = self.tl.vec(f'extra.{b}', t)
                cc[0] += e[0]
                cc[1] += e[1] * r.m
                cc[2] += e[2] * r.m
        self.reach_now = reach
        return ch

    def state(self):
        return {'arms': {s: copy.deepcopy(a.prev) for s, a in self.rig.arms.items()},
                'legs': {s: leg.x.copy() for s, leg in self.rig.legs.items()}}

    def world(self, ch):
        p = {k: list(v) for k, v in ch.items()}
        P.ground(p)
        return self.troop.fk(self._local(p))

    # ---- the whole clip
    def run(self, pushes=None):
        self.rig.reset()
        nets, states, reach = [], [], []
        for i, t in enumerate(self.times):
            states.append(self.state())
            push = {k: float(v[i]) for k, v in (pushes or {}).items()}
            nets.append(self.tick(t, push))
            reach.append(dict(self.reach_now))
        return nets, states, reach

    def _volumes(self, ch, prev):
        """Clipper volumes of a tick's pose and of the half tick before it (the clip's keys are linear, so the game
        plays the channels' midpoint there; a path can cut through the body between two clear ticks)."""
        v = self.clipper.volumes(self.world(ch))
        if prev is None:
            return v
        mid = {b: [(x + y) / 2 for x, y in zip(prev.get(b, [0.0] * 6), ch.get(b, [0.0] * 6))] for b in set(prev) | set(ch)}
        m = self.clipper.volumes(self.world(mid))
        return {k: max(v.get(k, 0.0), m.get(k, 0.0)) for k in set(v) | set(m)}

    def solve_pushes(self, nets, states):
        """Per item, the smallest push level per tick that brings it (and the half tick before it) under TOLERANCE,
        smoothed over time."""
        if self.clipper is None:
            return {}
        need = {'weapon': np.zeros(self.n), 'shield': np.zeros(self.n)}
        # what the stance itself has inside the body is the floor: the clip starts and ends there, so it cannot do better
        base = self.clipper.volumes(self.world(self.rig.stance_ch))
        tol = {k: max(C.TOLERANCE, base.get(k, 0.0) + 0.25) for k in need}
        for i, t in enumerate(self.times):
            prev = nets[i - 1] if i else None
            vol = self._volumes(nets[i], prev)
            for item, v in vol.items():
                if item not in need or v <= tol[item]:
                    continue
                best = C.LEVELS[-1]
                for c in C.LEVELS[1:]:
                    ch = self.tick(t, {item: c}, states[i])
                    if self._volumes(ch, prev).get(item, 0.0) <= tol[item]:
                        best = c
                        break
                need[item][i] = best
        return {k: C.smooth(v, self.step, loop=self.loop) for k, v in need.items() if v.any()}

    def _ends(self):
        """Tick indices the clip pins to the stance."""
        ends = {int(round(st / self.step)) for st in self.tl.stance_times()}
        if not self.loop:
            ends |= {0, self.n - 1}
        return {i for i in ends if 0 <= i < self.n}

    def _settle(self, nets, ends):
        """Land every free bone on the stance's own rotation and numbers at the pinned ticks.

        1. A straight arm's roll is free (the hand carries the item's turn), so the solver may come home with the
           upper arm rolled and the hand counter-rolled. Each segment that closes on a pinned tick eases the arm and
           hand rotations onto the stance's (smoothstep, slerp), so the pin is not a snap.
        2. A rotation has two ZYX Euler forms, each moved by whole turns; the game interpolates the numbers, so each
           tick takes the form that moves the channels least (dynamic programming), pinned ticks fixed to the stance."""
        tr, r = self.troop, self.rig
        keys = sorted(set(float(x) for x in self.tl.times))
        st_net = r.stance_net
        arm_bones = {b for a in r.arms.values() for b in (a.arm, a.hand) if b in tr.rest}
        nets = [{b: list(v) for b, v in ch.items()} for ch in nets]

        def net_r(ch, b):
            return np.array(tr.rest[b][0], float) + np.array(ch[b][:3], float)

        def put(ch, b, e):
            for k in range(3):
                ch[b][k] = float(e[k]) - float(tr.rest[b][0][k])

        for i in sorted(ends):
            ts = self.times[i]
            prev = [k for k in keys if k < ts - 1e-9]
            if not prev or i == 0:
                continue
            tp = prev[-1]
            for j in range(self.n):
                t = self.times[j]
                if not tp < t < ts:
                    continue
                x = (t - tp) / (ts - tp)
                w = x * x * (3 - 2 * x)
                for b in arm_bones:
                    if b in nets[j] and b in st_net:
                        Rs, Rt = M.rot(st_net[b]), M.rot(net_r(nets[j], b))
                        put(nets[j], b, S.closest_euler(_slerp_R(Rt, Rs, w), net_r(nets[j], b)))
        shifts = np.array([(i, j, k) for i in (-1, 0, 1) for j in (-1, 0, 1) for k in (-1, 0, 1)], float) * 360.0
        for b in sorted({b for ch in nets for b in ch}):
            if b in tr.limits or b not in tr.rest or b not in st_net or any(b not in ch for ch in nets):
                continue
            cands = []
            for j, ch in enumerate(nets):
                if j in ends:
                    cands.append(np.array([st_net[b]], float))
                    continue
                e = np.array(net_r(ch, b), float)
                alt = np.array([e[0] + 180.0, 180.0 - e[1], e[2] + 180.0])
                alt = alt + 360.0 * np.round((e - alt) / 360.0)
                cands.append(np.concatenate([e + shifts, alt + shifts]))
            cost = np.zeros(len(cands[0]))
            back = []
            for j in range(1, len(cands)):
                d = ((cands[j][:, None, :] - cands[j - 1][None, :, :]) ** 2).sum(-1) + cost[None, :]
                arg = d.argmin(1)
                back.append(arg)
                cost = d[np.arange(len(arg)), arg]
            pick = [int(cost.argmin())]
            for arg in reversed(back):
                pick.append(int(arg[pick[-1]]))
            pick.reverse()
            for j, ch in enumerate(nets):
                put(ch, b, cands[j][pick[j]])
        return nets

    def bake(self):
        """{bone: n x 6 clip channels}; also sets .nets, .pushes, .reach."""
        nets, states, reach = self.run()
        pushes = self.solve_pushes(nets, states)
        if pushes:
            nets, states, reach = self.run(pushes)
        nets = self._settle(nets, self._ends())
        bones = sorted({b for ch in nets for b in ch})
        clip = {}
        for b in bones:
            base = np.array(self.rig.stance_ch.get(b, [0.0] * 6), float)
            clip[b] = np.array([np.array(ch.get(b, base), float) - base for ch in nets])
        for group, delay in (self.recipe.get('lag') or {}).items():   # overlap: a bone group trails
            for b in self._group(group):
                if b in clip:
                    tt = self.times - delay
                    tt = tt % self.length if self.loop else tt
                    clip[b] = np.stack([np.interp(tt, self.times, clip[b][:, k]) for k in range(6)], 1)
        for i in self._ends():
            for b in clip:
                clip[b][i] = self.rig.zero.get(b, np.zeros(6))
        if self.loop:
            for b in clip:
                clip[b][-1] = clip[b][0]
        for b in list(clip):
            z = self.rig.zero.get(b)
            if not np.any(np.abs(clip[b]) > 1e-6) and z is None:
                del clip[b]
        self.nets, self.pushes, self.reach = nets, pushes, reach
        return clip

    def _group(self, g):
        side = self.rig.sides.get(g)
        if side:
            return [f'{side}_arm', f'{side}_forearm', f'{side}_hand']
        if g.endswith('_hand') and g[:-5] in self.rig.sides:
            return [f'{self.rig.sides[g[:-5]]}_hand']
        if g.endswith('_forearm') and g[:-8] in self.rig.sides:
            s = self.rig.sides[g[:-8]]
            return [f'{s}_forearm', f'{s}_hand']
        return [g]


def resample(clip, times, step):
    """A clip baked at a fine step, read on the 0.05 s tick."""
    if abs(step - TICK) < 1e-9:
        return clip, times
    n = int(round(times[-1] / TICK)) + 1
    tt = np.round(np.arange(n) * TICK, 6)
    idx = np.round(tt / step).astype(int)
    return {b: v[idx] for b, v in clip.items()}, tt


# ---- the bbmodel

def keyframes(clip_uuid, bone, track, times):
    """Linear tick keys for one bone: rotation always, position when it ever moves."""
    out = []
    for chan, sl, nd in (('rotation', slice(0, 3), ROT_ROUND), ('position', slice(3, 6), POS_ROUND)):
        v = np.round(track[:, sl], nd) + 0.0
        if chan == 'position' and not np.any(np.abs(v) > 0):
            continue
        for t, x in zip(times, v):
            out.append({'channel': chan, 'data_points': [{'x': float(x[0]), 'y': float(x[1]), 'z': float(x[2])}],
                        'uuid': str(uuid.uuid5(NS, f'{clip_uuid}/{bone}/{chan}/{t:.2f}')), 'time': float(round(t, 2)),
                        'color': -1, 'interpolation': 'linear'})
    return out


def impact_marks(anim):
    out = []
    for an in (anim.get('animators') or {}).values():
        if an.get('type') == 'effect':
            for k in an.get('keyframes', []):
                if any('impact' in str(d.get('script', '')) for d in k.get('data_points', [])):
                    out.append(float(k['time']))
    return out


def write(troop, clip_name, clip, times, impact=None, length=None, dry=False):
    """Replaces one animation's bone keys in the family's bbmodel; the rest of the file is untouched. The animation must
    exist (made in Blockbench with its id, length, loop and impact marker): this never changes timing. Returns
    (new text, bones skipped because the project has no group for them)."""
    path = P.source(troop.rig, troop.family)
    raw = path.read_text(encoding='utf-8')
    proj = json.loads(raw)
    anim = next((a for a in proj.get('animations', []) if a.get('name') == clip_name), None)
    if anim is None:
        raise KeyError(f'{path.name} has no animation {clip_name!r}; add it in Blockbench first (id, length, impact)')
    if length is not None and abs(float(anim.get('length', 0)) - length) > 1e-6:
        raise ValueError(f'{clip_name}: the project says {anim.get("length")} s, the recipe {length} s; timing is gameplay')
    if impact is not None:
        marks = impact_marks(anim)
        if not marks or any(abs(m - impact) > 1e-6 for m in marks):
            raise ValueError(f'{clip_name}: impact marker {marks} is not at {impact} s; timing is gameplay, ask Kort')
    groups = {}
    for g in proj.get('groups', []):
        groups.setdefault(g['name'], g['uuid'])
    old = anim.get('animators') or {}
    new = {u: an for u, an in old.items() if an.get('type') == 'effect'}
    skipped = []
    for bone in sorted(clip):
        if bone not in groups:
            if np.any(np.abs(clip[bone]) > 1e-6):
                skipped.append(bone)
            continue
        new[groups[bone]] = {'name': bone, 'type': 'bone', 'keyframes': keyframes(anim['uuid'], bone, clip[bone], times)}
    order = [u for u in old if u in new] + [u for u in new if u not in old]
    anim['animators'] = {u: new[u] for u in order}
    text = _dump_like(raw, proj)
    if not dry:
        path.write_text(text, encoding='utf-8', newline='\n')
    return text, skipped


def _dump_like(raw, proj):
    """JSON in the source file's own layout (indent and trailing newline), so other animations stay byte-identical."""
    indent = None
    for line in raw.splitlines()[1:3]:
        stripped = line.lstrip(' \t')
        if stripped and len(stripped) < len(line):
            ws = line[:len(line) - len(stripped)]
            indent = '\t' if ws.startswith('\t') else len(ws)
            break
    text = json.dumps(proj, indent=indent, ensure_ascii=False) if indent is not None else json.dumps(
        proj, separators=(',', ':'), ensure_ascii=False)
    return text + ('\n' if raw.endswith('\n') else '')


def run_clips(verify=False):
    args = [sys.executable, str(M.ROOT / 'tools/clips.py')] + (['--verify'] if verify else [])
    r = subprocess.run(args, cwd=str(M.ROOT), capture_output=True, text=True)
    return r.returncode, (r.stdout + r.stderr).strip()


def parsed(troop, clip_name, text):
    """(clip JSON, parsed clip) of one animation from a project's text, through clips.py's own convert, check and
    parse: what the game would load if the text were written."""
    rig = json.loads((M.ROOT / P.CL.RIGS / (troop.rig + '.json')).read_text(encoding='utf-8'))
    where = f'{troop.rig}/{troop.family}.bbmodel'
    made = P.CL.convert(json.loads(text), rig, where)
    clip = made[clip_name]
    rows = P.CL.variants(json.loads((M.ROOT / P.CL.SHEET).read_text(encoding='utf-8')))
    p = P.CL.parse(clip)
    P.CL.check(clip, p, rig, troop.family, rows, f'{where} {clip_name}')
    return clip, p


def author(troop, rec, step=TICK):
    """Bake a recipe and pass it through the bbmodel writer in memory: (baker, clip channels, times, project text,
    parsed clip). Nothing is written."""
    b = Baker(troop, rec, step)
    clip = b.bake()
    clip, times = resample(clip, b.times, step)
    text, skipped = write(troop, rec['clip'], clip, times, impact=rec.get('impact'), length=rec['length'], dry=True)
    _, p = parsed(troop, rec['clip'], text)
    b.skipped = skipped
    return b, clip, times, text, p
