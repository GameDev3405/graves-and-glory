"""Troop clip authoring kit: poses on the troop's own model, an arm solver for held weapons, clips written into the troop's
Blockbench project (art/clips/<rig>/<troop>.bbmodel, which tools/clips.py turns into clips/<rig>/<troop>/), and a lint of each
clip's net pose.

Poses are absolute: {bone: [rx, ry, rz, tx, ty, tz]} added to the bone's rest, as a clip keys a stance (ModelPart convention: y
down, -z forward; a negative arm rx swings forward, a forearm rx must be 0 or negative, a shin rx 0 or positive; root ty positive
drops). A clip names how the engine plays it, and the kit writes its keys that way:

  stance  idle, guard, carry, aim: the pose itself (the stance layer)
  offset  attacks, hits, blocks, idle_alt_<n>, walk and run: the pose minus its base stance's pose (the engine adds the clip to
          the stance playing under it; for a gait clip that is carry, or guard with an enemy near, so gait clips key rhythm)
  whole   deaths and specials (victory, war_cry, ...): the pose itself (a death fades the stance to rest; a scene pose replaces it)
  raw     walk and run: the keys are the offsets themselves (rhythm added to whichever stance plays: carry, or guard near an enemy)
"""
import json
import math
import pathlib
import sys
import uuid

import numpy as np

TOOLS = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import troop_render as R  # noqa: E402

ROOT = TOOLS.parent
DESIGN = ROOT / 'sheets/design.json'
NS = uuid.UUID('6b1e4b55-7c2a-4f0e-9d6a-0d3b5e7a1c11')
LIMITS = {'forearm': (-150, 0), 'shin': (0, 150), 'foot': (-45, 45), 'chest': (-35, 45)}
GRADED = ('root', 'body', 'chest', 'head', 'right_arm', 'left_arm', 'right_forearm', 'left_forearm', 'right_hand', 'left_hand',
          'right_leg', 'left_leg', 'right_shin', 'left_shin')
TICK = 0.05


def v(right=0.0, up=0.0, fwd=0.0):
    """A model-space vector from the troop's point of view (its right, up, forward)."""
    return np.array([-right, -up, -fwd], float)


def unit(x):
    x = np.asarray(x, float)
    return x / (np.linalg.norm(x) + 1e-12)


def P(*parts, **bones):
    """A pose: later parts and keyword bones override earlier ones, channel lists of 3 (rotation) or 6."""
    out = {}
    for part in parts + (bones,):
        for b, ch in part.items():
            ch = list(ch)
            if len(ch) == 3:
                ch = ch + list(out.get(b, [0, 0, 0, 0, 0, 0])[3:])
            out[b] = [float(c) for c in ch]
    return out


def add(pose, **bones):
    """The pose with channels added: bone=[drx, dry, drz] or 6."""
    out = {b: list(c) for b, c in pose.items()}
    for b, d in bones.items():
        cur = out.get(b, [0.0] * 6)
        d = list(d) + [0.0] * (6 - len(d))
        out[b] = [a + c for a, c in zip(cur, d)]
    return out


def mix(a, b, u):
    bones = set(a) | set(b)
    z = [0.0] * 6
    return {k: [x + (y - x) * u for x, y in zip(a.get(k, z), b.get(k, z))] for k in bones}


class Kit:
    """One troop's model for authoring: forward kinematics, its weapon's grip, and the arm solver."""

    def __init__(self, troop):
        self.id = troop
        self.t = R.Troop(troop, 1)
        self.json = self.t.json
        self.rig = self.json['rig']
        self.bones = list(self.t.order)
        design = json.loads(DESIGN.read_text(encoding='utf-8'))
        self.row = next(r for r in design['troops'] if r['id'] == troop)
        self.family = self.row['family']
        self.slots = {r['id']: r for r in design['attackVariants'] if r['family'] == self.family}
        w = self.t.weapon
        self.grip = w.grip if w is not None else np.eye(4)
        pts = np.array([q.o for q in w.quads] + [q.o + q.a + q.b for q in w.quads]) if w is not None else np.zeros((1, 3))
        self.tip_local = np.array([0, pts[:, 1].min(), 0.0])      # the weapon's far end along its -y (blade tip, staff head)
        self.butt_local = np.array([0, pts[:, 1].max(), 0.0])
        self.limits = {b['name']: tuple(b['limit']) for b in self.json['bones'] if 'limit' in b}
        g = (self.json and (w.grip[:3, :3] @ np.array([0, -1.0, 0]))) if w is not None else None
        if g is not None and g[2] < -0.5:   # a blade hung back over the forearm (sword grip [90, 45, 90])
            self.wrist0 = (90.0, 0.0, 0.0)

    def mats(self, pose):
        return self.t.matrices(pose)

    def at(self, pose, bone, local=(0, 0, 0)):
        M = self.mats(pose)[bone]
        return (M @ np.array([*local, 1.0]))[:3]

    def weapon(self, pose, side='right'):
        """(hand point, unit direction of the weapon's far end, far end point) in model space."""
        M = self.mats(pose)[side + '_hand'] @ self.grip
        hand = M[:3, 3]
        tip = (M @ np.array([*self.tip_local, 1.0]))[:3]
        return hand, unit(tip - hand), tip

    # the wrist's natural hold per weapon grip: a sword's grip [90, 45, 90] hangs the blade back over the forearm, so the hand
    # turns +90 to carry it 45 degrees forward of the forearm's line (lessons: blade = arm + 135 - wrist); upright grips hold at 0
    wrist0 = (0.0, 0.0, 0.0)
    WRIST_SPAN = 40.0

    def reach(self, pose, side, pos=None, aim=None, flat=None, hint=None, wpos=8.0, waim=6.0, wflat=2.0, wrist=0.04, iters=60, wrist0=None, hand_only=False, free=None, axis=None, twist=(0, 0, 0), axis2=None, aim2=None, waim2=4.0):
        """The pose with one arm solved so its hand reaches pos (model px) and its weapon points along aim (and its flat faces
        flat, a model-space normal for edge-leading cuts), staying near hint (default: the pose's own arm). Damped least squares
        over arm rx ry rz, forearm rx and hand rx ry rz; forearm held to [-150, -3]."""
        arm, fore, hand = side + '_arm', side + '_forearm', side + '_hand'
        base = P(pose)
        h = P(hint or pose)
        q = np.array([*base.get(arm, [0] * 6)[:3], base.get(fore, [0] * 6)[0], *base.get(hand, [0] * 6)[:3]], float)
        q0 = np.array([*h.get(arm, [0] * 6)[:3], h.get(fore, [0] * 6)[0], *h.get(hand, [0] * 6)[:3]], float)

        def put(qq):
            p = P(base)
            p[arm] = [qq[0], qq[1], qq[2]] + p.get(arm, [0] * 6)[3:]
            fr = p.get(fore, [0] * 6)
            p[fore] = [qq[3], fr[1], fr[2]] + fr[3:]
            p[hand] = [qq[4], qq[5], qq[6]] + p.get(hand, [0] * 6)[3:]
            return p

        def resid(qq):
            p = put(qq)
            if any(twist):
                p[hand] = [a + b for a, b in zip(p[hand][:3], twist)] + p[hand][3:]
            M = self.mats(p)[hand] @ self.grip
            r = []
            if pos is not None:
                r += list(wpos * (M[:3, 3] - pos) / 4.0)
            if aim is not None:
                d = M[:3, :3] @ np.asarray(axis, float) if axis is not None else (M @ np.array([*self.tip_local, 1.0]))[:3] - M[:3, 3]
                r += list(waim * (unit(d) - unit(aim)))
            if axis2 is not None and aim2 is not None:   # a second weapon axis held along aim2 (a bow's limbs upright)
                r += list(waim2 * (unit(M[:3, :3] @ np.asarray(axis2, float)) - unit(aim2)))
            if flat is not None:  # the blade's flat normal: the weapon's local x
                r += list(wflat * (abs(np.dot(unit(M[:3, 0]), unit(flat))) - 1.0) * np.ones(1))
            r += list(0.02 * (qq[:4] - q0[:4]) / 30.0)
            dev = qq[4:] - w0
            r += list(wrist * dev / 30.0)
            r += list(8.0 * np.maximum(0.0, np.abs(dev) - self.WRIST_SPAN) / 10.0)  # the ±40 bound round the hold (near hard)
            r += [3.0 * max(0.0, qq[3] + 3) / 10, 3.0 * max(0.0, -150 - qq[3]) / 10]
            return np.array(r)

        w0 = np.array(wrist0 if wrist0 is not None else (self.wrist0 if side == (self.json.get('hand') or 'right') else (0, 0, 0)), float)
        free = free if free is not None else range(4, 7) if hand_only else range(7)
        lam = 1e-2
        for _ in range(iters):
            r0 = resid(q)
            J = np.zeros((len(r0), 7))
            for i in free:
                d = np.zeros(7)
                d[i] = 0.5
                J[:, i] = (resid(q + d) - r0) / 0.5
            A = J.T @ J + lam * np.eye(7)
            step = -np.linalg.solve(A, J.T @ r0)
            if np.linalg.norm(step) > 25:
                step *= 25 / np.linalg.norm(step)
            qn = q + step
            if np.sum(resid(qn) ** 2) < np.sum(r0 ** 2):
                q, lam = qn, max(lam / 3, 1e-4)
            else:
                lam *= 4
            if np.linalg.norm(step) < 1e-3:
                break
        q[3] = min(-3, max(-150, q[3]))
        return {b: [round(c, 2) for c in ch] for b, ch in put(q).items()}

    # ---- bows: TroopMotion.twist turns the bow hand by the draw (pitch 90 d, yaw 45 (1 - d), roll 90 d) over whatever the clip keys
    def twist(self, draw):
        return (90.0 * draw, 45.0 * (1 - draw), 90.0 * draw)

    def twisted(self, pose, draw, side='left'):
        out = P(pose)
        h = out.get(side + '_hand', [0.0] * 6)
        out[side + '_hand'] = [a + b for a, b in zip(h[:3], self.twist(draw))] + h[3:]
        return out

    def nock(self, pose, draw, frame=3, side='left'):
        """The string's nock in model space with the bow drawn to frame (0 rest .. 3 full), the hand twisted by draw."""
        w = self.json['weapon']
        import json as _j
        wj = _j.loads((R.MODELS / 'weapon' / (w + '.json')).read_text(encoding='utf-8'))
        n = wj['frames'][frame]['nock']
        M = self.mats(self.twisted(pose, draw, side))[side + '_hand'] @ self.grip
        return (M @ np.array([*n, 1.0]))[:3]

    def bow_dir(self, pose, draw, side='left'):
        """Where the arrow flies (the bow's +x, from string to grip), in model space."""
        M = self.mats(self.twisted(pose, draw, side))[side + '_hand'] @ self.grip
        return unit(M[:3, 0])

    def lowest(self, pose, skip=('weapon',)):
        """The largest y (model px, y down: the lowest point) over every shown box corner of the posed model."""
        items, _ = self.t.quads(pose)
        wtex = self.t.weapon.quads[0].tex if self.t.weapon is not None else None
        y = -1e9
        for q, M in items:
            if 'weapon' in skip and q.tex is wtex:
                continue
            for c in (q.o, q.o + q.a, q.o + q.b, q.o + q.a + q.b):
                y = max(y, (M[:3, :3] @ c + M[:3, 3])[1])
        return y

    def grounded(self, pose, floor=24.0):
        """The pose with the root raised or lowered so its lowest box corner lies on the floor (a lying corpse's last key)."""
        out = P(pose)
        r = out.get('root', [0] * 6)
        out['root'] = r[:4] + [r[4] + floor - self.lowest(out), r[5]]
        return out

    def point(self, pose, side, aim, flat=None, **kw):
        """The pose with only the wrist turned (within the hold's ±40) so the weapon points along aim (model space)."""
        return self.reach(pose, side, aim=aim, flat=flat, hand_only=True, **kw)

    def report(self, pose, side='right'):
        """(arm, forearm, hand rotations, weapon direction as right/up/fwd) for a pose: to print at wind-up and impact keys."""
        h, d, tip = self.weapon(pose, side)
        return [round(x) for x in pose.get(side + '_arm', [0] * 3)[:3]], round(pose.get(side + '_forearm', [0])[0]), \
            [round(x) for x in pose.get(side + '_hand', [0] * 3)[:3]], [round(-d[0], 2), round(-d[1], 2), round(-d[2], 2)], [round(-tip[0], 1), round(24 - tip[1], 1), round(-tip[2], 1)]


# ---------------------------------------------------------------- clips

class Clip:
    """A clip as key poses: keys [(t, pose, interpolation)], how it plays (mode stance, offset or whole, and its base stance),
    markers {name: t} and loop."""

    def __init__(self, name, length, keys, mode='offset', base='guard', loop=False, markers=None, slot=None, bones=None):
        self.name, self.length, self.keys, self.mode, self.base, self.loop = name, length, keys, mode, base, loop
        self.markers = dict(markers or {})
        self.slot = slot          # the attackVariants row this troop attack fills (troopAttacks)
        self.only = bones          # if given, the bones the clip keys (others left to the layers below)


def norm_keys(keys):
    out = []
    for k in keys:
        t, pose = k[0], k[1]
        interp = k[2] if len(k) > 2 else 'linear'
        out.append((round(t / TICK) * TICK, pose, interp))
    return out


def channels(clip, stances):
    """{bone: {'rotation': [(t, xyz, interp)], 'position': [...]}} as the clip's JSON keys them (offsets already taken)."""
    keys = norm_keys(clip.keys)
    base = stances.get(clip.base, {}) if clip.mode == 'offset' else {}
    bones = sorted(set(b for _, p, _ in keys for b in p) if clip.only is None else set(clip.only))
    z = [0.0] * 6
    out = {}
    for b in bones:
        rot, pos = [], []
        for t, p, interp in keys:
            val = p.get(b, base.get(b, z) if clip.mode == 'offset' else z)
            val = [a - c for a, c in zip(val, base.get(b, z))] if clip.mode == 'offset' else val
            rot.append((t, [round(x, 2) for x in val[:3]], interp))
            pos.append((t, [round(x, 3) for x in val[3:]], interp))
        if any(any(abs(x) > 1e-6 for x in r[1]) for r in rot):
            out.setdefault(b, {})['rotation'] = rot
        if any(any(abs(x) > 1e-6 for x in r[1]) for r in pos):
            out.setdefault(b, {})['position'] = pos
        if clip.mode != 'offset' and b not in out:  # a stance or whole clip holds a keyed bone at rest explicitly
            out[b] = {'rotation': rot}
    return out


def project(kit, clips, stances):
    """The troop's Blockbench project: the troop's bone tree (no meshes; tools/anim_edit.py open adds them) and every clip."""
    def uid(*parts):
        return str(uuid.uuid5(NS, '/'.join((kit.id,) + parts)))
    groups, nodes = [], {}
    model = {'name': 'model', 'origin': [0, 0, 0], 'rotation': [180, 0, 0], 'uuid': uid('model'), 'color': 0, 'export': True, 'visibility': True}
    groups.append(model)
    outline = {'name': 'model', 'uuid': model['uuid'], 'origin': [0, 0, 0], 'rotation': [180, 0, 0], 'export': True, 'isOpen': True, 'children': []}
    world = {}
    for b in kit.json['bones']:
        name, parent = b['name'], b.get('parent', '')
        pv = np.array(b.get('pivot', [0, 0, 0]), float) + (world[parent] if parent else 0)
        world[name] = pv
        g = {'name': name, 'origin': [round(x, 4) for x in pv], 'rotation': b.get('rotation', [0, 0, 0]), 'uuid': uid('bone', name),
             'color': 0, 'export': True, 'visibility': True}
        groups.append(g)
        node = {'name': name, 'uuid': g['uuid'], 'origin': g['origin'], 'rotation': g['rotation'], 'export': True, 'isOpen': False, 'children': []}
        nodes[name] = node
        (nodes[parent]['children'] if parent else outline['children']).append(node)
    anims = []
    for c in clips:
        animators = {}
        for bone, chans in channels(c, stances).items():
            kfs = []
            for ch, ks in sorted(chans.items()):
                for t, xyz, interp in ks:
                    kfs.append({'channel': ch, 'data_points': [{'x': xyz[0], 'y': xyz[1], 'z': xyz[2]}], 'uuid': uid('k', c.name, bone, ch, f'{t:.3f}'),
                                'time': round(t, 4), 'color': -1, 'interpolation': interp})
            animators[uid('bone', bone)] = {'name': bone, 'type': 'bone', 'keyframes': kfs}
        if c.markers:
            animators['effects'] = {'name': 'Effects', 'type': 'effect', 'keyframes': [
                {'channel': 'timeline', 'data_points': [{'script': m}], 'uuid': uid('m', c.name, m), 'time': round(round(t / TICK) * TICK, 4), 'color': -1, 'interpolation': 'linear'}
                for m, t in sorted(c.markers.items(), key=lambda x: x[1])]}
        anims.append({'uuid': uid('anim', c.name), 'name': c.name, 'loop': 'loop' if c.loop else 'once', 'override': False, 'length': round(c.length, 4),
                      'snapping': 20, 'selected': False, 'anim_time_update': '', 'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators})
    return {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': True}, 'name': kit.id, 'model_identifier': kit.id,
            'visible_box': [1, 1, 0], 'resolution': {'width': 128, 'height': 128}, 'elements': [], 'groups': groups, 'outliner': [outline],
            'textures': [], 'animations': anims}


def write(kit, clips):
    """Writes art/clips/<rig>/<troop>.bbmodel and the troop's troopAttacks rows; returns the project path."""
    stances = {c.name: c.keys[0][1] for c in clips if c.mode == 'stance'}
    path = ROOT / 'art/clips' / kit.rig / (kit.id + '.bbmodel')
    path.write_text(json.dumps(project(kit, clips, stances), indent=1) + '\n', encoding='utf-8', newline='\n')
    raw = DESIGN.read_text(encoding='utf-8')
    d = json.loads(raw)
    rows = [r for r in d['troopAttacks'] if r['troop'] != kit.id]
    rows += [{'id': f'{kit.id}_{c.name}', 'troop': kit.id, 'clip': c.name, 'slot': c.slot} for c in clips if c.slot]
    order = [t['id'] for t in d['troops']]
    d['troopAttacks'] = sorted(rows, key=lambda r: (order.index(r['troop']), r['id']))
    DESIGN.write_text(json.dumps(d, indent=2, ensure_ascii=False) + ('\n' if raw.endswith('\n') else ''), encoding='utf-8', newline='\n')
    return path


# ---------------------------------------------------------------- lint

def sample(chans, t, loop, length):
    """A bone's channels at t from keys [(t, xyz, interp)], linear (catmull-rom keys sampled linearly: a lint bound, not playback)."""
    ks = chans
    if loop:
        t %= length
    if t <= ks[0][0]:
        return list(ks[0][1])
    for (t0, a, _), (t1, b, _) in zip(ks, ks[1:]):
        if t <= t1:
            u = (t - t0) / max(t1 - t0, 1e-9)
            return [x + (y - x) * u for x, y in zip(a, b)]
    return list(ks[-1][1])


def lint(kit, clips):
    """Problems in the clips' own keys and their net pose (stance plus offset): backward elbows and knees, joint limits, more
    than 60 degrees in any 100 ms on a graded bone, keys off the 0.05 s tick, loops that do not close, attack impacts off their
    slot's tick and lengths over 40 ticks."""
    stances = {c.name: c.keys[0][1] for c in clips if c.mode == 'stance'}
    out = []
    for c in clips:
        ch = channels(c, stances)
        for t, _, _ in c.keys:
            if abs(t / TICK - round(t / TICK)) > 1e-6:
                out.append(f'{c.name}: key at {t} s is off the 0.05 s tick')
        if c.slot:
            want = kit.slots[c.slot]['impactTicks']
            got = c.markers.get('impact')
            if got is None or abs(got * 20 - want) > 1e-6:
                out.append(f'{c.name}: impact {got} s, slot {c.slot} hits on tick {want} ({want / 20} s)')
            if c.length * 20 > 40 + 1e-6:
                out.append(f'{c.name}: {c.length * 20:g} ticks, over 40')
        if kit.t.weapon is not None and c.mode != 'raw' and not c.name.startswith('death'):
            for t, pose, _ in c.keys:
                y = kit.weapon(pose)[2][1]
                if y > 24.5:
                    out.append(f'{c.name}: the weapon tip is {y - 24:.1f} px under the floor at {t:.2f} s')
        base = stances.get(c.base, {}) if c.mode in ('offset', 'raw') else {}
        ts = [i * 0.01 for i in range(int(c.length / 0.01) + 1)]
        for b, chans in ch.items():
            rot = chans.get('rotation')
            if not rot:
                continue
            vals = [sample([(k[0], k[1], k[2]) for k in rot], t, c.loop, c.length) for t in ts]
            if b in GRADED:
                worst = 0
                for i in range(len(ts)):
                    j = min(len(ts) - 1, i + 10)
                    worst = max(worst, max(abs(vals[j][a] - vals[i][a]) for a in range(3)))
                if worst > 60 + 1e-6:
                    out.append(f'{c.name}: {b} turns {worst:.0f} degrees in 100 ms (cap 60)')
            kind = next((k for k in LIMITS if b.endswith(k)), None)
            lo, hi = kit.limits.get(b, LIMITS.get(kind, (-999, 999))) if kind or b in kit.limits else (-999, 999)
            for t, val in zip(ts, vals):
                net = val[0] + (base.get(b, [0] * 6)[0] if c.mode in ('offset', 'raw') else 0)
                if net < lo - 0.5 or net > hi + 0.5:
                    out.append(f'{c.name}: {b} pitch {net:.0f} at {t:.2f} s is outside its limit [{lo}, {hi}]')
                    break
            if c.loop and max(abs(a - b2) for a, b2 in zip(vals[0], vals[-1])) > 0.01:
                out.append(f'{c.name}: {b} loop does not close')
    return out
