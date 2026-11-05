"""The quality gates of docs/ANIMATING-TROOPS.md, measured on a clip as the game plays it (pose.Player, 60 fps).

  reach     IK residual of an authored bake (legs to their plants, an off hand to a haft)    < 0.3 px
  sink      the lower sole below the ground after MotionFeet lifts the root, past its 0.25 px tolerance   <= 0.15 px
  hover     the lower sole above the ground (the game lifts, never lowers)                   <= 0.15 px
  skate     a planted sole's ground speed (planted: within 0.3 px of its stance height)        <= 8 px/s
  pop       true (geodesic) rotation of a bone between 60 fps frames                          <= 50 deg
  jerk      change of that per-frame rotation (as a rotation vector)                          <= 30 deg
  clip      Clipper volume per item per frame                                                  <= 1.5 px^3
  loop      a loop's last pose against its first                                               < 0.5 deg, root 0.01 px
  ends      a once clip's first and last poses against its stance alone                        < 1 deg
  sign      net forearm pitch > 0 (backwards elbow) or net shin pitch < 0                      none
  rate      peak per-axis change over any 100 ms window, linear between the clip's keys        <= 60 deg
  wrist     the hand's turn relative to the forearm through the strike (impact - 0.15 s to impact), forearm rigs only  <= 40 deg
  ticks     key times off the 0.05 s tick                                                      none
  timing    impact marker and length against sheets/design.json attackVariants                 equal

The stance's own feet are reported apart ('stance hover'): a once clip starts and ends on it, so the clip cannot fix it.
"""
import math

import numpy as np

from . import clearance as C
from . import model as M
from . import pose as P
from . import solve as S

FPS = 60
GATES = {'reach': 0.3, 'sink': 0.15, 'hover': 0.15, 'skate': 8.0, 'pop': 50.0, 'jerk': 30.0, 'clip': 1.5,
         'loop': 0.5, 'loop_root': 0.01, 'ends': 1.0, 'rate': 60.0, 'wrist': 40.0}
PLANTED = 0.3
STRIKE = 0.15


def _logvec(R):
    """The rotation vector (axis * angle, degrees) of R."""
    c = max(-1.0, min(1.0, (np.trace(R) - 1) / 2))
    a = math.acos(c)
    if a < 1e-9:
        return np.zeros(3)
    v = np.array([R[2, 1] - R[1, 2], R[0, 2] - R[2, 0], R[1, 0] - R[0, 1]])
    s = math.sin(a)
    if abs(s) < 1e-6:                                   # 180 degrees: the axis from the diagonal
        ax = np.sqrt(np.maximum((np.diag(R) + 1) / 2, 0))
        return math.degrees(a) * ax
    return math.degrees(a) * v / (2 * s)


def graded(troop, bone):
    b = troop.by.get(bone)
    return b is not None and not C.is_soft(b) and bone in troop.shown


class Run:
    """A clip played as the game plays it, sampled at FPS (and at its ends)."""

    def __init__(self, troop, clip, stance='default', fps=FPS):
        self.troop = troop
        self.player = P.Player(troop, clip, stance)
        self.name = self.player.name
        L = self.player.length
        n = max(2, int(round(L * fps)) + 1)
        self.times = np.linspace(0.0, L, n)
        self.ch, self.loc, self.W = [], [], []
        for t in self.times:
            ch = self.player.channels(float(t))
            loc = P.local(troop, ch, self.player.clip, self.player.under, self.player.drawn, self.player.carries)
            self.ch.append(ch)
            self.loc.append(loc)
            self.W.append(troop.fk(loc))
        self.loop = bool(self.player.clip.get('loop'))
        self.under = self.player.under

    def stance_pose(self):
        """The stance alone, as the game shows it when no action plays."""
        if self.under is None:
            return None
        ch = P.channels(None, 0.0, self.under)
        P.ground(ch)
        return P.local(self.troop, ch, None, self.under, False, True)


def feet(run):
    out = {'sink': 0.0, 'hover': 0.0, 'skate': 0.0, 'stance_hover': 0.0}
    tr = run.troop
    if not ('right_shin' in tr.by and 'left_shin' in tr.by):
        return out
    st = run.stance_pose()
    base = None
    if st is not None:
        ch = P.channels(None, 0.0, run.under)
        P.ground(ch)
        base = (P.lowest(ch, 0), P.lowest(ch, 1))
        out['stance_hover'] = max(0.0, M.GROUND - max(base))
        out['stance_gap'] = (round(M.GROUND - base[0], 3), round(M.GROUND - base[1], 3))
    soles = []
    lows = []
    for ch, W in zip(run.ch, run.W):
        low = (P.lowest(ch, 0), P.lowest(ch, 1))
        lows.append(low)
        sup = max(low)
        out['sink'] = max(out['sink'], sup - M.GROUND - P.SINK_TOL)
        out['hover'] = max(out['hover'], M.GROUND - sup)
        soles.append([(W[f'{s}_shin'] @ np.array([0.0, S.SOLE, 0.0, 1.0]))[:3] for s in ('right', 'left')])
    lows = np.array(lows)
    soles = np.array(soles)
    ref = base if base is not None else lows.max(0)
    dt = np.diff(run.times)
    for i in range(2):
        planted = (lows[:, i] >= ref[i] - PLANTED) | (lows[:, i] >= M.GROUND - PLANTED)
        v = np.linalg.norm(np.diff(soles[:, i, [0, 2]], axis=0), axis=1) / np.maximum(dt, 1e-9)
        both = planted[1:] & planted[:-1]
        if both.any():
            out['skate'] = max(out['skate'], float(v[both].max()))
    return out


def smoothness(run):
    """{'pop': (deg, bone, t), 'jerk': (deg, bone, t)} over the graded bones' local rotations."""
    tr = run.troop
    pop, jerk = (0.0, None, 0.0), (0.0, None, 0.0)
    for b in run.loc[0]:
        if not graded(tr, b):
            continue
        Rs = [M.rot(loc[b][0]) for loc in run.loc]
        prev = None
        for i in range(1, len(Rs)):
            d = _logvec(Rs[i - 1].T @ Rs[i])
            a = float(np.linalg.norm(d))
            if a > pop[0]:
                pop = (a, b, float(run.times[i]))
            if prev is not None:
                j = float(np.linalg.norm(d - prev))
                if j > jerk[0]:
                    jerk = (j, b, float(run.times[i]))
            prev = d
    return {'pop': pop, 'jerk': jerk}


def clipping(run, clipper=None):
    cl = clipper or C.Clipper(run.troop)
    worst = {}
    for t, W in zip(run.times, run.W):
        for item, v in cl.volumes(W).items():
            if v > worst.get(item, (0.0, 0.0))[0]:
                worst[item] = (float(v), float(t))
    return worst


def ends(run):
    """Loops: (deg, root px) last against first. Once clips: deg of the first and last poses against the stance."""
    tr = run.troop
    if run.loop:
        a, b = run.loc[0], run.loc[-1]
        deg = max((S.geodesic(M.rot(a[k][0]), M.rot(b[k][0])) for k in a if graded(tr, k)), default=0.0)
        root = float(np.linalg.norm(a['root'][1] - b['root'][1])) if 'root' in a else 0.0
        return {'loop': deg, 'loop_root': root}
    st = run.stance_pose()
    if st is None:
        return {}
    out = {}
    for name, loc in (('start', run.loc[0]), ('end', run.loc[-1])):
        deg = max((S.geodesic(M.rot(st[k][0]), M.rot(loc[k][0])) for k in st if graded(tr, k)), default=0.0)
        pos = max((float(np.linalg.norm(st[k][1] - loc[k][1])) for k in st if graded(tr, k)), default=0.0)
        out[name] = (deg, pos)
    return {'ends': max(out['start'][0], out['end'][0]), 'ends_px': max(out['start'][1], out['end'][1])}


def signs(run):
    """{bone: (deg past the hinge, t)} of backwards elbows and knees in the authored net pitch.

    The pitch is rest + stance + clip before the game's joint limits clamp it: a model with limits
    hides a backwards key as a frozen elbow, which is still an authoring fault."""
    bad = {}
    rest = run.troop.rest
    for t, ch in zip(run.times, run.ch):
        for b, c in ch.items():
            if b not in rest:
                continue
            x = float(rest[b][0][0] + c[0])
            if b.endswith('_forearm') and x > 0.01 and x > bad.get(b, (0, 0))[0]:
                bad[b] = (x, float(t))
            if b.endswith('_shin') and x < -0.01 and -x > bad.get(b, (0, 0))[0]:
                bad[b] = (-x, float(t))
    return {b: (round(v, 2), round(t, 3)) for b, (v, t) in bad.items()}


def rate(clip, troop, window=0.1, step=0.005):
    """{bone: (peak deg per window on one axis, t)} from the clip's keys, linear between them."""
    out = {}
    L = clip['length']
    ts = np.arange(0.0, max(L - window, 0.0) + 1e-9, step)
    for b, chans in clip['bones'].items():
        keys = chans.get('rotation')
        if not keys or not graded(troop, b):
            continue
        kt = np.array([k['t'] for k in keys])
        kv = np.array([k['v'] for k in keys], float)
        if len(kt) == 1:
            continue
        best = (0.0, 0.0)
        for ax in range(3):
            v0 = np.interp(ts, kt, kv[:, ax])
            v1 = np.interp(ts + window, kt, kv[:, ax])
            d = np.abs(v1 - v0)
            i = int(d.argmax())
            if d[i] > best[0]:
                best = (float(d[i]), float(ts[i]))
        out[b] = best
    return out


def wrist(run, troop):
    """Max turn (deg) of each hand relative to its forearm over the strike window, on rigs with forearms."""
    imp = run.player.clip.get('impact', -1)
    if imp is None or imp < 0:
        return {}
    t1 = imp / 20.0
    t0 = max(0.0, t1 - STRIKE)
    out = {}
    for side in ('right', 'left'):
        h, f = f'{side}_hand', f'{side}_forearm'
        if h not in troop.by or f not in troop.by:
            continue
        idx = [i for i, t in enumerate(run.times) if t0 - 1e-9 <= t <= t1 + 1e-9]
        if not idx:
            continue
        R0 = M.rot(run.loc[idx[0]][h][0])
        out[h] = max(S.geodesic(R0, M.rot(run.loc[i][h][0])) for i in idx)
    return out


def ticks(clip):
    off = []
    for b, chans in clip['bones'].items():
        for ch, keys in chans.items():
            for k in keys:
                if abs(k['t'] * 20 - round(k['t'] * 20)) > 1e-3:
                    off.append((b, ch, k['t']))
    return off


def timing(troop, clip):
    row = P.attacks(troop.family).get(clip['clip'][:-5] if clip['clip'].endswith('_wall') else clip['clip'])
    if not row:
        return None
    return {'impact': clip.get('impact'), 'want': row.get('impactTicks'), 'length': clip['length']}


def measure(troop, clip, stance='default', reach=None, clipper=None):
    """Every gate for one clip; returns {metric: value} and a list of failures."""
    run = Run(troop, clip, stance)
    res = {'clip': run.name, 'length': run.player.length, 'loop': run.loop}
    res.update(feet(run))
    sm = smoothness(run)
    res['pop'], res['pop_at'] = round(sm['pop'][0], 2), (sm['pop'][1], round(sm['pop'][2], 3))
    res['jerk'], res['jerk_at'] = round(sm['jerk'][0], 2), (sm['jerk'][1], round(sm['jerk'][2], 3))
    cl = clipping(run, clipper)
    res['clipping'] = {k: (round(v, 2), round(t, 3)) for k, (v, t) in cl.items()}
    res.update(ends(run))
    res['signs'] = signs(run)
    rt = rate(run.player.clip, troop)
    worst = max(rt.items(), key=lambda kv: kv[1][0], default=(None, (0.0, 0.0)))
    res['rate'], res['rate_at'] = round(worst[1][0], 2), (worst[0], round(worst[1][1], 3))
    res['rate_over'] = {b: round(v, 1) for b, (v, t) in rt.items() if v > GATES['rate']}
    res['wrist'] = {b: round(v, 1) for b, v in wrist(run, troop).items()}
    res['ticks_off'] = ticks(run.player.clip)
    res['timing'] = timing(troop, run.player.clip)
    if reach:
        res['reach'] = round(max((max(r.values(), default=0.0) for r in reach), default=0.0), 4)
    fails = []

    def gate(name, value, limit, strict=False):
        if value is not None and (value >= limit if strict else value > limit):
            fails.append(f'{name} {value:.2f} > {limit}')
    gate('reach', res.get('reach'), GATES['reach'], True)
    for k in ('sink', 'hover', 'skate', 'pop', 'jerk'):
        gate(k, res[k], GATES[k])
    for item, (v, t) in res['clipping'].items():
        gate(f'clip {item}@{t}', v, GATES['clip'])
    if run.loop:
        gate('loop', res.get('loop'), GATES['loop'], True)
        gate('loop root', res.get('loop_root'), GATES['loop_root'], True)
    elif 'ends' in res:
        gate('ends', res['ends'], GATES['ends'], True)
    for b, (v, t) in res['signs'].items():
        fails.append(f'sign {b} {v:.1f} at {t:.3f}')
    for b, v in res['rate_over'].items():
        fails.append(f'rate {b} {v} deg/100ms')
    for b, v in res['wrist'].items():
        gate(f'wrist {b}', v, GATES['wrist'])
    if res['ticks_off']:
        fails.append(f'{len(res["ticks_off"])} keys off the tick')
    tm = res['timing']
    if tm and tm['want'] is not None and tm['impact'] != tm['want']:
        fails.append(f"impact tick {tm['impact']} != design {tm['want']}")
    res['fails'] = fails
    return res


def report(res):
    lines = [f"{res['clip']}: {res['length']} s {'loop' if res['loop'] else 'once'}"]
    if 'reach' in res:
        lines.append(f"  reach      {res['reach']:.3f} px")
    lines.append(f"  feet       sink {res['sink']:.2f}  hover {res['hover']:.2f}  skate {res['skate']:.1f} px/s"
                 + (f"  (stance gap R/L {res['stance_gap']})" if 'stance_gap' in res else ''))
    lines.append(f"  smooth     pop {res['pop']:.1f} deg {res['pop_at']}  jerk {res['jerk']:.1f} deg {res['jerk_at']}")
    lines.append(f"  clipping   {res['clipping'] or 'none'}")
    if res['loop']:
        lines.append(f"  loop       {res.get('loop', 0):.2f} deg, root {res.get('loop_root', 0):.3f} px")
    elif 'ends' in res:
        lines.append(f"  ends       {res['ends']:.2f} deg, {res['ends_px']:.3f} px from the stance")
    lines.append(f"  rate       {res['rate']:.1f} deg/100ms {res['rate_at']}" + (f"  over: {res['rate_over']}" if res['rate_over'] else ''))
    lines.append(f"  wrist      {res['wrist'] or 'n/a'}")
    lines.append(f"  signs      {res['signs'] or 'ok'}   ticks off {len(res['ticks_off'])}   timing {res['timing']}")
    lines.append('  PASS' if not res['fails'] else '  FAIL: ' + '; '.join(res['fails']))
    return '\n'.join(lines)
