"""Keeping the held items out of the body.

`Clipper` measures it: points every 0.5 px through the sword and the shield (less their grips, which belong in the
fist), tested against every body and armour box of the knight on the articulated rig, wherever the pose puts them.

`clear` fixes it while a clip is baked (combat_clips.build). For every tick it finds the smallest push that takes each
item out of the body: the shield arm reaches further forward and out, the sword arm carries the blade out and away.
The pushes are spread over time (held a little before and after they are needed, then eased) so the arm moves aside
smoothly instead of popping, and the arm is solved again with them.
"""
import numpy as np

from . import combat_rig as CR

HELD_IN_HAND = {'oathblade': {'grip'}, 'sunward': {'grip', 'bracket_t', 'bracket_b'}}
SOFT = ('cape', 'plume')            # cloth and horsehair: an item may brush them
_GEOMETRY = None


def _box_matrix(box):
    M = np.eye(4)
    if any(box.rot):
        R = box.matrix()
        o = np.array(box.origin, float)
        M[:3, :3] = R
        M[:3, 3] = o - R @ o
    return M


def geometry():
    """The knight's boxes on the articulated rig, untextured: [(box, bone, item local matrix or None)]."""
    global _GEOMETRY
    if _GEOMETRY is None:
        import animate_unit as AU                       # tools/ is on the path wherever this runs
        from build_player_armor import BODY
        from player_armor import oathbound as ARMOUR
        from player_weapons import oathbound as WEAPONS
        parts = [(b, {}, AU._bone_of(b), None) for b in ARMOUR.build().boxes_for('base')]
        parts += [(b, {}, b.bone, None) for b in BODY]
        ws = {w.id: w for w in WEAPONS.weapons()}
        for wid, bone, local in (('oathblade', 'right_hand', AU.SWORD_LOCAL), ('sunward', 'left_hand', AU.SHIELD_LOCAL)):
            parts += [(b, {}, bone, local) for b in ws[wid].boxes('base', 'rest')]
        _GEOMETRY = [(b, bone, local) for b, f, bone, local in CR.split_parts(parts)]
    return _GEOMETRY


KNIGHT_EXCLUDE = (('oathblade', 'pommel', 'right_forearm'),    # the pommel rides just under the fist
                  ('sunward', None, 'left_forearm'))           # the forearm is strapped behind the board


class Clipper:
    """parts: [(box, bone, item local matrix or None, item name or None)] (the knight's by default; with three-tuples
    the item is named by its hand). held: {item: grip boxes that belong in the fist}. exclude: (item, box prefix or
    None, body bone) pairs that may touch. soft: name prefixes of cloth that items may brush."""

    def __init__(self, parts=None, step=0.5, held=None, exclude=KNIGHT_EXCLUDE, soft=SOFT):
        held = HELD_IN_HAND if held is None else held
        self.items, self.body, self.exclude = [], [], exclude
        for entry in parts or geometry():
            box, bone, local = entry[:3]
            item = entry[3] if len(entry) > 3 else ('oathblade' if bone == 'right_hand' else 'sunward')
            if local is not None:
                if box.name in held.get(item, ()):
                    continue
                lo, hi = box.lo_hi()
                axes = [np.arange(lo[i] + step / 2, hi[i], step) for i in range(3)]
                g = np.stack(np.meshgrid(*axes, indexing='ij'), -1).reshape(-1, 3)
                if not len(g):
                    continue
                M = local @ _box_matrix(box)
                self.items.append((item, box.name, bone, (M[:3, :3] @ g.T).T + M[:3, 3]))
            elif not box.name.startswith(soft) and bone not in soft:
                lo, hi = box.lo_hi()
                self.body.append((box.name, bone, np.linalg.inv(_box_matrix(box)), np.array(lo) + 0.05,
                                  np.array(hi) - 0.05))
        self.cell = step ** 3
        self.names = sorted({i for i, *_ in self.items})

    def _excluded(self, item, name, bbone):
        return any(i == item and (pre is None or name.startswith(pre)) and b == bbone for i, pre, b in self.exclude)

    def volumes(self, pose, which=None, detail=False):
        """{item: px^3 of it inside the body} (with detail: also the deepest point and where)."""
        out = {n: ((0.0, 0.0, '') if detail else 0.0) for n in self.names if not which or n == which}
        inv = {}
        for item, name, bone, pts in self.items:
            if which and item != which:
                continue
            W = pose[bone]
            wp = (W[:3, :3] @ pts.T).T + W[:3, 3]
            inside = np.zeros(len(wp), bool)
            depth = np.zeros(len(wp))
            where = np.empty(len(wp), object)
            for bname, bbone, Binv, lo, hi in self.body:
                if self._excluded(item, name, bbone):
                    continue
                if bbone not in inv:
                    inv[bbone] = np.linalg.inv(pose[bbone])
                M = Binv @ inv[bbone]
                lp = (M[:3, :3] @ wp.T).T + M[:3, 3]
                ins = np.all((lp > lo) & (lp < hi), axis=1)
                if ins.any():
                    inside |= ins
                    if detail:
                        d = np.minimum(lp - lo, hi - lp).min(axis=1)
                        deeper = ins & (d > depth)
                        depth[deeper] = d[deeper]
                        where[deeper] = bname
            if detail:
                vol, dm, part = out[item]
                vol += inside.sum() * self.cell
                if inside.any() and depth.max() > dm:
                    dm, part = float(depth.max()), '%s in %s' % (name, where[int(depth.argmax())])
                out[item] = (vol, dm, part)
            else:
                out[item] += inside.sum() * self.cell
        return out


# the pushes, in the chest's frame (+x the troop's right, +y up, -z forward), added to the aims at strength c
PUSH = {
    'sunward': {'upper': (-0.35, 0.2, -1.1), 'fore': (-0.25, -0.1, -0.7)},
    'oathblade': {'upper': (0.55, 0.0, -0.45), 'fore': (0.35, 0.0, -0.3), 'blade': (0.5, 0.0, -0.2)},
}
LEVELS = (0.0, 0.15, 0.3, 0.45, 0.6, 0.8, 1.0, 1.25, 1.5)
TOLERANCE = 1.0          # px^3 an item may touch the body (a brush, not a pass-through)


def pushed(aims, item, c):
    """The arm's aims with a push of strength c."""
    p = PUSH[item]
    out = list(aims)
    for i, k in enumerate(('upper', 'fore', 'blade')):
        if k in p and i < len(out) and c:
            out[i] = CR._unit(np.asarray(CR._unit(out[i])) + c * np.asarray(p[k]))
    return tuple(out)


def smooth(values, step, hold=0.15, ease=0.05, loop=False):
    """Spreads per-tick pushes over time: each is held `hold` s either side (so the arm is already aside when the item
    arrives) and then eased with a Gaussian of `ease` s (three of which fit inside the hold, so a peak keeps its full
    height)."""
    v = np.asarray(values, float)
    n = len(v)
    h = int(round(hold / step))
    held = np.array([max(v[(j % n) if loop else min(max(j, 0), n - 1)] for j in range(i - h, i + h + 1))
                     for i in range(n)])
    s = ease / step
    k = np.arange(-int(3 * s), int(3 * s) + 1)
    w = np.exp(-0.5 * (k / s) ** 2)
    w /= w.sum()
    out = np.zeros(n)
    for i in range(n):
        idx = [(i + j) % n if loop else min(max(i + j, 0), n - 1) for j in k]
        out[i] = float(np.dot(w, held[idx]))
    out = np.maximum(out, v)               # never less than what the tick itself needs
    if not loop:                           # a once clip starts and ends on the guard, which needs no push
        ease_ = lambda r: 3 * r ** 2 - 2 * r ** 3
        i = np.arange(n)
        out = np.minimum(out, out.max() * ease_(np.clip((n - 1 - i) / round(0.2 / step), 0, 1)))
        out = np.minimum(out, out.max() * ease_(np.clip(i / round(0.06 / step), 0, 1)))
    return out


LIMBS = ('right_forearm', 'left_forearm')
TRUNK = ('chest', 'body', 'head')


class LimbClipper:
    """How far the forearms pass into the torso or the head: the volume of forearm deeper than `min_depth` px inside
    them. The upper arms are left out (they meet the torso at the shoulder by design), and so is the outer skin: the
    armour is modelled with arms that overlap a wide jerkin or belt by a px at rest. parts: [(box, bone)]."""

    def __init__(self, parts, step=0.5, soft=SOFT, min_depth=1.1):
        self.min_depth = min_depth
        self.limbs, self.trunk = [], []
        for box, bone in parts:
            if box.name.startswith(soft) or box.name.startswith('p_'):
                continue
            lo, hi = box.lo_hi()
            if bone in LIMBS:
                axes = [np.arange(lo[i] + step / 2, hi[i], step) for i in range(3)]
                g = np.stack(np.meshgrid(*axes, indexing='ij'), -1).reshape(-1, 3)
                M = _box_matrix(box)
                self.limbs.append((box.name, bone, (M[:3, :3] @ g.T).T + M[:3, 3]))
            elif bone in TRUNK:
                self.trunk.append((box.name, bone, np.linalg.inv(_box_matrix(box)), np.array(lo) + 0.05,
                                   np.array(hi) - 0.05))
        self.cell = step ** 3

    def volume(self, pose):
        total = 0.0
        inv = {b: np.linalg.inv(pose[b]) for b in TRUNK}
        for name, bone, pts in self.limbs:
            W = pose[bone]
            wp = (W[:3, :3] @ pts.T).T + W[:3, 3]
            inside = np.zeros(len(wp), bool)
            for bname, bbone, Binv, lo, hi in self.trunk:
                M = Binv @ inv[bbone]
                lp = (M[:3, :3] @ wp.T).T + M[:3, 3]
                inside |= np.all((lp > lo + self.min_depth) & (lp < hi - self.min_depth), axis=1)
            total += inside.sum() * self.cell
        return total
