"""Keeping the held items out of the body, for any troop.

Clipper measures it: points every 0.5 px through each held item, tested against every rigid box of the troop wherever
the pose puts it, as px^3 inside. The items and what each may touch come from the model, not from a list per troop:

  weapon  the held weapon mesh (voxelised by winding number in its own frame), or the model's own tag-"weapon" bones
  shield  the model's tag-"shield" bones, or vanilla's shield item on the off hand (its handle is in the fist)

What belongs in the fist is left out: weapon points within FIST px of the grip, and the pommel side of the grip, never
count against the holding forearm and hand; weapon points within FIST px of the off hand's wrist never count against the
off forearm and hand (a second hand on the haft); a shield never counts against the arm it is strapped to. Cloth and
hair (SOFT names, team panels) may be brushed.

PUSH and pushed() take an item out while a clip is baked: the arm reaches further out and forward with a push of strength
c, the smallest of LEVELS that brings the item under TOLERANCE, spread over time by smooth() so the arm moves aside
before the item arrives instead of popping.
"""
import numpy as np

from .model import grip_matrix, mesh_quads, shield_item_matrix, SHIELD_BOXES

STEP = 0.5
FIST = 4.0
SOFT = ('cape', 'cloak', 'plume', 'feather', 'tabard', 'robe', 'skirt', 'kilt', 'sash', 'scarf', 'tatter', 'rags',
        'hair', 'tail', 'train', 'fringe', 'kelp', 'cord', 'beads', 'mist', 'tent', 'shroud', 'apron', 'hem', 'tippet',
        'trinket', 'troph', 'charm', 'necklace', 'pelt', 'smoke', 'sling', 'strap', 'chain', 'quill')
SOFT_TAGS = ('smoke', 'strap', 'apron')
TOLERANCE = 1.0                      # px^3 an item may touch the body: a brush, not a pass-through
LEVELS = (0.0, 0.15, 0.3, 0.45, 0.6, 0.8, 1.0, 1.25, 1.5)

# Pushes as (out, up, forward) per aim, out being away from the body on the arm's own side; to_game() turns them into
# the chest frame for the arm. Families override the defaults (a two-handed haft goes forward, not out).
PUSH_DEFAULT = {
    'shield': {'upper': (0.35, 0.2, 1.1), 'fore': (0.25, -0.1, 0.7)},
    'weapon': {'upper': (0.55, 0.0, 0.45), 'fore': (0.35, 0.0, 0.3), 'item': (0.5, 0.0, 0.2)},
}
PUSH_FAMILY = {
    'greatsword': {'weapon': {'upper': (0.2, 0.0, 0.6), 'fore': (0.15, 0.0, 0.5), 'item': (0.3, 0.0, 0.4)}},
    'polearm': {'weapon': {'upper': (0.15, 0.0, 0.6), 'fore': (0.1, 0.0, 0.5), 'item': (0.2, 0.0, 0.5)}},
    'spear': {'weapon': {'upper': (0.15, 0.0, 0.6), 'fore': (0.1, 0.0, 0.5), 'item': (0.2, 0.0, 0.5)}},
    'bow': {'weapon': {'upper': (0.3, 0.0, 0.6), 'fore': (0.2, 0.0, 0.5), 'item': (0.2, 0.0, 0.3)}},
}


def is_soft(bone):
    n = bone['name']
    return bool(bone.get('team')) or bone.get('tag') in SOFT_TAGS or any(s in n for s in SOFT)


def push(family, item, side):
    """{aim: unit-less push vector in the parent frame} for an item held on side ('right' or 'left')."""
    p = PUSH_FAMILY.get(family, {}).get(item) or PUSH_DEFAULT[item]
    s = 1.0 if side == 'left' else -1.0                  # the troop's left is +x
    return {k: np.array([s * o, -u, -f]) for k, (o, u, f) in p.items()}


def pushed(aims, vec, c):
    """aims {'upper', 'fore', 'item'} with push vectors vec at strength c (each renormalised)."""
    if not c:
        return dict(aims)
    out = dict(aims)
    for k, v in vec.items():
        if out.get(k) is not None:
            a = np.asarray(out[k], float)
            a = a / max(np.linalg.norm(a), 1e-9)
            b = a + c * v
            out[k] = b / max(np.linalg.norm(b), 1e-9)
    return out


def _box_grid(lo, hi, step=STEP):
    axes = [np.arange(lo[i] + step / 2, hi[i] - 1e-9, step) for i in range(3)]
    if any(len(a) == 0 for a in axes):
        return np.zeros((0, 3))
    return np.stack(np.meshgrid(*axes, indexing='ij'), -1).reshape(-1, 3)


def _box_lohi(x):
    o, s = np.array(x['origin'], float), np.array(x['size'], float)
    d = np.array(x.get('dilation', [0, 0, 0]), float)
    return o - d, o + s + d


def voxelise(corners, step=STEP):
    """Points every `step` px inside a closed mesh of quads (corners Nx4x3), by winding number of a +x ray."""
    tris = []
    for c in corners:
        tris.append((c[0], c[1], c[2]))
        tris.append((c[0], c[2], c[3]))
    if not tris:
        return np.zeros((0, 3))
    T = np.array(tris)                                   # M x 3 x 3
    lo, hi = T.reshape(-1, 3).min(0), T.reshape(-1, 3).max(0)
    g = _box_grid(lo, hi, step)
    if len(g) == 0:
        return g
    a, b, c = T[:, 0], T[:, 1], T[:, 2]
    n = np.cross(b - a, c - a)
    keep = np.abs(n[:, 0]) > 1e-9                        # faces parallel to the ray never cross it
    a, b, c, n = a[keep], b[keep], c[keep], n[keep]
    inside = np.zeros(len(g), bool)
    for i0 in range(0, len(g), 4096):
        P = g[i0:i0 + 4096][:, None, :]                  # K x 1 x 3
        # the ray's (y, z) in each triangle, by 2D barycentrics
        def cross2(u, v):
            return u[..., 1] * v[..., 2] - u[..., 2] * v[..., 1]
        w0 = cross2(b - a, P - a)
        w1 = cross2(c - b, P - b)
        w2 = cross2(a - c, P - c)
        hit = ((w0 >= 0) & (w1 >= 0) & (w2 >= 0)) | ((w0 <= 0) & (w1 <= 0) & (w2 <= 0))
        # where the ray meets the plane: x = a.x - (n.y (y - a.y) + n.z (z - a.z)) / n.x
        x = a[:, 0] - (n[:, 1] * (P[..., 1] - a[:, 1]) + n[:, 2] * (P[..., 2] - a[:, 2])) / n[:, 0]
        hit &= x > P[..., 0]
        wind = (hit * np.sign(n[:, 0])).sum(1)
        inside[i0:i0 + 4096] = wind != 0
    return g[inside]


def _surface(corners, step=STEP):
    pts = []
    for c in corners:
        u, v = c[1] - c[0], c[3] - c[0]
        nu, nv = max(1, int(np.linalg.norm(u) / step)), max(1, int(np.linalg.norm(v) / step))
        for i in range(nu):
            for j in range(nv):
                pts.append(c[0] + u * (i + 0.5) / nu + v * (j + 0.5) / nv)
    return np.array(pts) if pts else np.zeros((0, 3))


class Clipper:
    """Item points and body boxes of one troop, and their overlap in a pose."""

    def __init__(self, troop, step=STEP):
        self.troop, self.cell = troop, step ** 3
        t = troop
        self.items = []                                  # (item, part, bone, points in bone frame, grip, tip)
        item_bones = set()
        weapon_bones = {b['name'] for b in t.bones if b.get('tag') == 'weapon'}
        sub = lambda tops: {b['name'] for b in t.bones if any(a in tops for a in t.path(b['name']))}
        weapon_bones = sub(weapon_bones)
        shield_bones = t.shield_bones() if t.own_shield else set()
        item_bones = weapon_bones | shield_bones
        if t.weapon and t.path_shown(t.hand_bone):
            w = t.weapon
            corners = [c for c, uv in mesh_quads(w['frames'][0]['quads'], *w['size'])]
            pts = voxelise(corners, step)
            if len(pts) == 0:
                pts = _surface(corners, step)
            G = grip_matrix(w)
            pts = (G[:3, :3] @ pts.T).T + G[:3, 3]
            ax = t.weapon_axis()
            self.items.append(('weapon', 'weapon', t.hand_bone, pts, ax[0], ax[1]))
        for n in sorted(weapon_bones):
            if n in t.shown:
                pts = np.concatenate([_box_grid(*_box_lohi(x), step) for x in t.by[n]['boxes']] or [np.zeros((0, 3))])
                if len(pts):
                    self.items.append(('weapon', n, n, pts, None, None))   # the grip at the bone's own pivot
        for n in sorted(shield_bones):
            if n in t.shown:
                pts = np.concatenate([_box_grid(*_box_lohi(x), step) for x in t.by[n]['boxes']] or [np.zeros((0, 3))])
                if len(pts):
                    self.items.append(('shield', n, n, pts, None, None))
        if t.shield_row and not t.own_shield and t.path_shown(t.off_hand_bone):
            M = shield_item_matrix(-1 if t.off_hand == 'left' else 1)
            for name, o, s in SHIELD_BOXES:
                if name == 'handle':
                    continue                             # in the fist
                g = _box_grid(np.array(o, float), np.array(o, float) + s, step)
                self.items.append(('shield', 'shield_' + name, t.off_hand_bone, (M[:3, :3] @ g.T).T + M[:3, 3], None, None))
        self.body = {}                                   # bone: (lo K x 3, hi K x 3)
        for b in t.bones:
            n = b['name']
            if n not in t.shown or n in item_bones or is_soft(b):
                continue
            los, his = [], []
            for x in b.get('boxes', []):
                lo, hi = _box_lohi(x)
                if np.any(hi - lo < 0.2):
                    continue                             # a plane has no inside
                los.append(lo + 0.05)
                his.append(hi - 0.05)
            if los:
                self.body[n] = (np.array(los), np.array(his))
        def fist(hand):                                  # the hand, its forearm and what they carry (cuffs, couters)
            tops = {hand, t.parent.get(hand)} - {None, 'body', 'root', 'chest'}
            return {b['name'] for b in t.bones if any(a in tops for a in t.path(b['name']))}
        self.hold = fist(t.hand_bone)
        self.off_hold = fist(t.off_hand_bone)
        if t.own_shield:
            tops = {t.parent.get(n) for n in shield_bones} - shield_bones - {None}
            self.strap = {b['name'] for b in t.bones if any(a in tops for a in t.path(b['name']))} - shield_bones
        else:
            self.strap = set(self.off_hold)

    def volumes(self, W, detail=False):
        """{item: px^3 of it inside the body} for world matrices W (with detail: (volume, deepest px, 'part in bone'))."""
        out = {}
        inv = {}
        off = None
        if self.troop.off_hand_bone in W:
            off = W[self.troop.off_hand_bone][:3, 3]
        for item, part, bone, pts, grip, tip in self.items:
            if bone not in W or not len(pts):
                continue
            M = W[bone]
            wp = (M[:3, :3] @ pts.T).T + M[:3, 3]
            near = np.zeros(len(pts), bool)
            if item == 'weapon':
                g = np.zeros(3) if grip is None else grip
                d = pts - g
                near = np.linalg.norm(d, axis=1) < FIST
                if tip is not None:
                    near |= d @ tip < 0
                near_off = np.linalg.norm(wp - off, axis=1) < FIST if off is not None else np.zeros(len(pts), bool)
            inside = np.zeros(len(pts), bool)
            depth = np.zeros(len(pts))
            where = np.empty(len(pts), object)
            for bb, (lo, hi) in self.body.items():
                if bb not in W:
                    continue
                if item == 'shield' and bb in self.strap:
                    continue
                mask = np.ones(len(pts), bool)
                if item == 'weapon':
                    if bb in self.hold:
                        mask &= ~near
                    if bb in self.off_hold:
                        mask &= ~near_off
                if not mask.any():
                    continue
                if bb not in inv:
                    inv[bb] = np.linalg.inv(W[bb])
                Bi = inv[bb]
                lp = (Bi[:3, :3] @ wp.T).T + Bi[:3, 3]           # N x 3
                ins = np.all((lp[:, None, :] > lo[None]) & (lp[:, None, :] < hi[None]), axis=2)   # N x K
                hit = ins.any(1) & mask
                if hit.any():
                    inside |= hit
                    if detail:
                        dd = np.where(ins, np.minimum(lp[:, None, :] - lo[None], hi[None] - lp[:, None, :]).min(2), 0).max(1)
                        deeper = hit & (dd > depth)
                        depth[deeper] = dd[deeper]
                        where[deeper] = bb
            vol = float(inside.sum() * self.cell)
            if detail:
                v, dm, pp = out.get(item, (0.0, 0.0, ''))
                v += vol
                if inside.any() and depth.max() > dm:
                    dm, pp = float(depth.max()), f'{part} in {where[int(depth.argmax())]}'
                out[item] = (v, dm, pp)
            else:
                out[item] = out.get(item, 0.0) + vol
        return out


def below(clipper, W, ground=24.0):
    """{item: px its lowest point sits under the ground plane (model y = MotionFeet.GROUND, y down)}."""
    out = {}
    for item, part, bone, pts, grip, tip in clipper.items:
        if bone not in W or not len(pts):
            continue
        M = W[bone]
        y = (M[1, :3] @ pts.T) + M[1, 3]
        out[item] = max(out.get(item, 0.0), float(max(0.0, y.max() - ground)))
    return out


def smooth(values, step, hold=0.15, ease=0.05, loop=False):
    """Spreads per-tick pushes over time: each held `hold` s either side, eased by a Gaussian of `ease` s, never below
    what the tick itself needs; a once clip eases to 0 over its first 0.06 s and last 0.2 s (it starts and ends on its
    stance, which needs no push)."""
    v = np.asarray(values, float)
    n = len(v)
    if n == 0 or not v.any():
        return v.copy()
    h = int(round(hold / step))
    idx = lambda j: (j % n) if loop else min(max(j, 0), n - 1)
    held = np.array([max(v[idx(j)] for j in range(i - h, i + h + 1)) for i in range(n)])
    s = ease / step
    k = np.arange(-int(3 * s), int(3 * s) + 1)
    w = np.exp(-0.5 * (k / s) ** 2)
    w /= w.sum()
    out = np.array([float(np.dot(w, held[[idx(i + j) for j in k]])) for i in range(n)])
    out = np.maximum(out, v)
    if not loop:
        e = lambda r: 3 * r ** 2 - 2 * r ** 3
        i = np.arange(n)
        out = np.minimum(out, out.max() * e(np.clip((n - 1 - i) / max(1, round(0.2 / step)), 0, 1)))
        out = np.minimum(out, out.max() * e(np.clip(i / max(1, round(0.06 / step)), 0, 1)))
    return out


def spring_follow(values, step, k=60.0, damping=0.7):
    """A damped spring chasing a channel track (cloth lag), per sample of `step` s."""
    v = np.asarray(values, float)
    out = np.zeros_like(v)
    x, vel = v[0].copy() if v.ndim > 1 else float(v[0]), 0.0 * v[0]
    c = 2 * damping * np.sqrt(k)
    for i in range(len(v)):
        acc = k * (v[i] - x) - c * vel
        vel = vel + acc * step
        x = x + vel * step
        out[i] = x
    return out
