"""A troop as the game draws it, from its model JSON (TroopModelLoader, TroopModel.prepare, TroopFeatures, TroopModels).

Troop(id, tier, cloth) loads models/troop/<id>.json and its row in sheets/design.json: every bone with its rest pivot and
rotation, its joint limit, whether this tier shows it (stages, weaponTiers, the face variant, the flags, team bones in
the cloth pass), its boxes as vanilla ModelPart.Cuboid quads (faceuv included) on the skin or cloth sheet, the held weapon
(TroopModels.weapon's tier fallback, the grip or the upright carry, on handPath) and the item shield (a shield row on a
model without its own shield bone). Nothing here is hard-wired to one troop: the hand, the shield arm, the item parts
held in the fist (HELD_IN_HAND for clearance) and the push directions come from the model and the family.
"""
import functools
import json
import math
import pathlib

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets'
MODELS = ASSETS / 'gravesandglory/models/troop'
WEAPONS = MODELS / 'weapon'
SHEET = ROOT / 'sheets/design.json'
RIGS = ROOT / 'art/rigs'
CLOTHS = ('crimson', 'teal', 'gold', 'violet', 'linen', 'company')
MAX_TIER = 3
FLAGS = ('pennant', 'standard')
CARRY_FORWARD, CARRY_TILT = 3.0, 10.0          # TroopModels.Weapon.carry
GROUND = 24.0                                  # MotionFeet.GROUND: the soles' y at rest


def rot(deg):
    """ModelPart's rotationZYX(roll, yaw, pitch) for (pitch, yaw, roll) degrees: Rz @ Ry @ Rx."""
    ax, ay, az = (math.radians(float(a)) for a in deg)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    return np.array([[cy * cz, sx * sy * cz - cx * sz, cx * sy * cz + sx * sz],
                     [cy * sz, sx * sy * sz + cx * cz, cx * sy * sz - sx * cz],
                     [-sy, sx * cy, cx * cy]])


def euler(R):
    """The inverse of rot: (pitch, yaw, roll) degrees, yaw in [-90, 90]."""
    b = math.asin(max(-1.0, min(1.0, -R[2, 0])))
    return (math.degrees(math.atan2(R[2, 1], R[2, 2])), math.degrees(b), math.degrees(math.atan2(R[1, 0], R[0, 0])))


def mat(R=None, t=(0, 0, 0)):
    M = np.eye(4)
    if R is not None:
        M[:3, :3] = R
    M[:3, 3] = t
    return M


def res(rid):
    ns, path = rid.split(':', 1)
    return ASSETS / ns / path


@functools.lru_cache(maxsize=None)
def design():
    return json.loads(SHEET.read_text(encoding='utf-8'))


def row(tid):
    for r in design()['troops']:
        if r.get('id') == tid:
            return r
    raise KeyError(f'no troop {tid!r} in {SHEET.name}')


def troops(house=None):
    """Every troop id with a model JSON (of one House)."""
    out = []
    for r in design()['troops']:
        if (house is None or r.get('house') == house) and (MODELS / f"{r['id']}.json").is_file():
            out.append(r['id'])
    return out


@functools.lru_cache(maxsize=None)
def texture(rid):
    """RGBA float array [h, w, 4] of a resource, or None when it is missing."""
    p = res(rid)
    if not p.is_file():
        return None
    from PIL import Image
    return np.asarray(Image.open(p).convert('RGBA'), np.float32) / 255.0


class Quad:
    """One textured face: corners in its bone's frame (4x3, model px), uv in sheet units (4x2), the texture key."""
    __slots__ = ('bone', 'part', 'item', 'tex', 'p', 'uv', 'local')

    def __init__(self, bone, part, item, tex, p, uv, local=None):
        self.bone, self.part, self.item, self.tex = bone, part, item, tex
        self.p, self.uv, self.local = np.asarray(p, float), np.asarray(uv, float), local


def cuboid(u, v, o, s, d, mirror, tw, th, sides=None):
    """vanilla ModelPart.Cuboid: the quads (corners, uv) of a box, as the 1.20.1 constructor lays them."""
    x, y, z = o[0] - d[0], o[1] - d[1], o[2] - d[2]
    f, g, h = o[0] + s[0] + d[0], o[1] + s[1] + d[1], o[2] + s[2] + d[2]
    if mirror:
        x, f = f, x
    v1, v2, v3, v4 = (x, y, z), (f, y, z), (f, g, z), (x, g, z)
    v5, v6, v7, v8 = (x, y, h), (f, y, h), (f, g, h), (x, g, h)
    sx, sy, sz = s
    j, k, l, m = u, u + sz, u + sz + sx, u + sz + sx + sx
    n, oo = u + sz + sx + sz, u + sz + sx + sz + sx
    p, q, r = v, v + sz, v + sz + sy
    faces = {'down': ((v6, v5, v1, v2), k, p, l, q), 'up': ((v3, v4, v8, v7), l, q, m, p),
             'west': ((v1, v5, v8, v4), j, q, k, r), 'north': ((v2, v1, v4, v3), k, q, l, r),
             'east': ((v6, v2, v3, v7), l, q, n, r), 'south': ((v5, v6, v7, v8), n, q, oo, r)}
    out = []
    for name, (vs, u1, va, u2, vb) in faces.items():
        if sides is not None and name not in sides:
            continue
        uv = [(u2 / tw, va / th), (u1 / tw, va / th), (u1 / tw, vb / th), (u2 / tw, vb / th)]
        out.append((name, vs, uv))
    return out


FACEUV = {'top': 'down', 'bottom': 'up', 'right': 'west', 'front': 'north', 'left': 'east', 'back': 'south'}


def face_uv(face, fu, fv, w, d):
    """TroopModelLoader.faceUv."""
    return {'top': (fu - d, fv), 'bottom': (fu - d - w, fv), 'right': (fu, fv - d), 'front': (fu - d, fv - d),
            'left': (fu - d - w, fv - d), 'back': (fu - 2 * d - w, fv - d)}[face]


AXIS = (1, 1, 2, 2, 0, 0)
NORMALS = ((0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0))


def mesh_quads(quads, w, h):
    """TroopMesh.parse: [(corners 4x3, uv 4x2)] from 13- or 20-number quads."""
    out = []
    for v in quads:
        if len(v) == 13:
            dr, lo, hi = int(v[0]), v[1:4], v[4:7]
            a = AXIS[dr]
            p, q = (a + 1) % 3, (a + 2) % 3
            pq = ([(lo[p], lo[q]), (hi[p], lo[q]), (hi[p], hi[q]), (lo[p], hi[q])] if NORMALS[dr][a] > 0
                  else [(lo[p], lo[q]), (lo[p], hi[q]), (hi[p], hi[q]), (hi[p], lo[q])])
            c = np.zeros((4, 3))
            for k in range(4):
                c[k, a], c[k, p], c[k, q] = lo[a], pq[k][0], pq[k][1]
            tex = [v[7], v[8], v[9], v[10], v[9] + v[11] - v[7], v[10] + v[12] - v[8], v[11], v[12]]
        elif len(v) == 20:
            c = np.array(v[:12], float).reshape(4, 3)
            tex = v[12:20]
        else:
            raise ValueError(f'a quad of {len(v)} numbers')
        uv = np.array(tex, float).reshape(4, 2) / (w, h)
        out.append((c, uv))
    return out


def weapon_json(wid, tier):
    """TroopModels.weapon(id, tier): <id>_t<n> from min(tier, 3) down to 2, then <id>; None without one."""
    if not wid:
        return None
    for t in range(min(tier, MAX_TIER), 1, -1):
        p = WEAPONS / f'{wid}_t{t}.json'
        if p.is_file():
            return json.loads(p.read_text(encoding='utf-8'))
    p = WEAPONS / f'{wid}.json'
    return json.loads(p.read_text(encoding='utf-8')) if p.is_file() else None


def grip_matrix(w):
    """The weapon's grip in the hand bone's frame (TroopModels.Weapon.grip, with carry for an upright weapon)."""
    g = w['grip']
    off, r = list(g.get('offset', [0, 0, 0])), list(g.get('rotation', [0, 0, 0]))
    if not any(off) and not any(r) and not w.get('pull'):
        off, r = [0, 0, -CARRY_FORWARD], [CARRY_TILT, 0, 0]
    return mat(rot(r), off)


# vanilla's shield (ShieldEntityModel) in the item frame, px: the plate and the handle
SHIELD_BOXES = (('plate', (-6, -11, -2), (12, 22, 1)), ('handle', (-1, -3, -1), (2, 6, 6)))


def shield_item_matrix(side):
    """TroopFeatures.Shield after the hand path, then ItemRenderer's -0.5 centring and the shield model's scale(1,-1,-1), px."""
    def rx(a):
        return mat(rot((a, 0, 0)))
    M = rx(-90) @ mat(rot((0, 180, 0))) @ mat(None, (0, 2, -2)) @ mat(None, (side * 13.51, 3, 5))
    a, b = math.radians(45), math.radians(side * 135)
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    M = M @ mat(Rx @ Ry)                       # Quaternionf.rotationXYZ: x first in the product
    M = M @ mat(None, (-8, -8, -8)) @ mat(np.diag([1.0, -1.0, -1.0]))
    return M


class Troop:
    """One troop at one tier, cloth side and face, as TroopModel.prepare shows it."""

    def __init__(self, tid, tier=3, cloth='crimson', face=None, helm=True, folded=False):
        self.id, self.tier, self.cloth = tid, int(tier), cloth
        self.row = row(tid)
        path = MODELS / f'{tid}.json'
        self.model = json.loads(path.read_text(encoding='utf-8'))
        m = self.model
        self.rig = m.get('rig', 'biped')
        self.rig_json = json.loads((RIGS / f'{self.rig}.json').read_text(encoding='utf-8'))
        self.family = self.row.get('family')
        self.hand = m.get('hand', 'right') or 'right'
        self.idle_arms = m.get('idleArms', 'hang')
        self.size = m.get('size', [64, 64])
        self.bones = m['bones']
        self.by = {b['name']: b for b in self.bones}
        self.parent = {b['name']: (b.get('parent') or None) for b in self.bones}
        self.rest = {b['name']: (np.array(b.get('rotation', [0, 0, 0]), float), np.array(b.get('pivot', [0, 0, 0]), float))
                     for b in self.bones}
        self.limits = {b['name']: tuple(b['limit']) for b in self.bones if 'limit' in b}
        skins = {int(k): v for k, v in (m.get('skins') or {}).items()}
        self.stage = self._skin_stage(skins, self.tier)
        self.skin_id = skins.get(self.stage)
        self.weapon_tier = self.tier
        faces = m.get('faces') or {}
        self.face_count = int(faces.get('count', 0) or 0)
        self.face = int(faces.get('card', 0) or 0) if face is None else face
        self.shield_row = (self.row.get('shield') or 'none') != 'none'
        self.own_shield = any(b.get('tag') == 'shield' for b in self.bones)
        self.folded = folded and 'arms_folded' in self.by
        self.flies = bool(self.row.get('pennant'))
        self.hand_bone = self._hand_bone(self.hand)
        self.off_hand = 'left' if self.hand == 'right' else 'right'
        self.off_hand_bone = self._hand_bone(self.off_hand)
        self.shield_arm = (self.off_hand + '_arm') if self.shield_row else None
        self.bow = self.family == 'bow'
        root_rig = next((b for b in self.rig_json['bones'] if b['name'] == 'root'), None)
        rp = self.rest['root'][1][1] if 'root' in self.rest else 0.0
        rr = root_rig['pivot'][1] if root_rig else 0.0
        self.root_k = (GROUND - rp) / (GROUND - rr) if GROUND != rr else 1.0
        self.shown = self._visibility(helm)
        self.weapon = weapon_json(m.get('weapon'), self.weapon_tier)
        self.textures = {'skin': texture(self.skin_id) if self.skin_id else None,
                         'cloth': texture(m['cloth'] + cloth + '.png') if m.get('cloth') else None}
        self.quads = self._quads()

    # ---- what shows
    @staticmethod
    def _skin_stage(skins, stage):
        out = None
        for k in sorted(skins):
            if k <= stage or out is None:
                out = k
        return 1 if out is None else out

    def _hand_bone(self, side):
        for n in (f'{side}_hand', f'{side}_forearm', f'{side}_arm'):
            if n in self.by:
                return n
        return None

    def path(self, bone):
        out = []
        while bone:
            out.append(bone)
            bone = self.parent.get(bone)
        return out[::-1]

    def _on(self, b):
        stages, wt, var = b.get('stages') or [], b.get('weaponTiers') or [], b.get('variants') or []
        on = (not stages or self.stage in stages) and (not wt or self.weapon_tier in wt)
        if var:
            on &= (0 if self.face_count <= 0 else self.face % self.face_count) in var
        return on

    def _visibility(self, helm):
        vis = {}
        for b in self.bones:
            on = self._on(b) and (helm or b.get('tag') != 'helm')
            vis[b['name']] = on and not b.get('team')
        if 'arms_folded' in self.by:
            vis['arms_folded'] = self.folded
            for a in ('right_arm', 'left_arm'):
                if a in vis:
                    vis[a] = vis[a] and not self.folded
        shown = set()
        for b in self.bones:
            n = b['name']
            if b.get('team'):
                on = self._on(b) and (b.get('part') not in FLAGS or self.flies) and (helm or b.get('tag') != 'helm')
            else:
                on = vis[n]
            p = self.path(n)
            for k in p[1:-1]:
                on = on and vis[k]
            if on:
                shown.add(n)
        self.visible = vis
        return shown

    def path_shown(self, bone):
        return bone is not None and all(self.visible.get(k, False) for k in self.path(bone)[1:])

    # ---- geometry
    def _quads(self):
        out = []
        sw, sh = self.size
        for b in self.bones:
            n = b['name']
            if n not in self.shown:
                continue
            tw, th = b.get('sheet', [sw, sh])
            tex = 'cloth' if b.get('team') else 'skin'
            for x in b.get('boxes', []):
                o, s = x['origin'], x['size']
                d = x.get('dilation', [0, 0, 0])
                mirror = bool(x.get('mirror'))
                rest = set(('down', 'up', 'west', 'north', 'east', 'south'))
                moved = []
                for face, (fu, fv) in (x.get('faceuv') or {}).items():
                    at = face_uv(face, fu, fv, s[0], s[2])
                    rest.discard(FACEUV[face])
                    moved.append((at, {FACEUV[face]}))
                for (u, v), sides in [((x['uv'][0], x['uv'][1]), rest)] + moved:
                    for name, vs, uv in cuboid(u, v, o, s, d, mirror, tw, th, sides):
                        out.append(Quad(n, n, None, tex, vs, uv))
        if self.weapon and self.path_shown(self.hand_bone):
            w = self.weapon
            ww, wh = w['size']
            G = grip_matrix(w)
            key = 'weapon'
            self.textures[key] = texture(w['texture']) if w.get('texture') else None
            for c, uv in mesh_quads(w['frames'][0]['quads'], ww, wh):
                p = (G[:3, :3] @ c.T).T + G[:3, 3]
                out.append(Quad(self.hand_bone, 'weapon', 'weapon', key, p, uv, local=c))
        if self.shield_row and not self.own_shield and self.path_shown(self.off_hand_bone):
            side = -1 if self.off_hand == 'left' else 1
            M = shield_item_matrix(side)
            for name, o, s in SHIELD_BOXES:
                for fname, vs, uv in cuboid(0, 0, o, s, (0, 0, 0), False, 64, 64):
                    p = (M[:3, :3] @ np.array(vs, float).T).T + M[:3, 3]
                    out.append(Quad(self.off_hand_bone, 'shield_' + name, 'shield', 'item_shield', p, uv, local=np.array(vs, float)))
            self.textures['item_shield'] = None
        return out

    def weapon_axis(self):
        """In the hand bone's frame: (grip point, unit tip direction, reach px) of the held weapon (see item_axes)."""
        ax = self.item_axes()
        return None if ax is None else ax[:3]

    def item_axes(self):
        """In the hand bone's frame: (grip, tip, reach, edge) of what the weapon hand holds, from the model itself: the
        weapon mesh (its grip the TroopModels grip) or the own tag-"weapon" bones under the hand (the grip their pivot).
        tip points from the grip at the farthest point; edge is the item's wide axis square to it (a blade's edge, a
        bow's string side), from the points' principal axes; None when the hand holds nothing."""
        pts, grip = [], None
        if self.weapon:
            pts = [q.p for q in self.quads if q.item == 'weapon']
            grip = grip_matrix(self.weapon)[:3, 3]
        if not pts and self.hand_bone:
            tops = [b['name'] for b in self.bones if b.get('tag') == 'weapon' and b['name'] in self.shown
                    and self.hand_bone in self.path(b['name'])]
            for n in tops:
                M = np.eye(4)
                for k in self.path(n)[self.path(n).index(self.hand_bone) + 1:]:
                    r, v = self.rest[k]
                    M = M @ mat(rot(r), v)
                for x in self.by[n].get('boxes', []):
                    o, s = np.array(x['origin'], float), np.array(x['size'], float)
                    c = np.array([[o[0] + s[0] * (i & 1), o[1] + s[1] * (i >> 1 & 1), o[2] + s[2] * (i >> 2 & 1)] for i in range(8)])
                    pts.append((M[:3, :3] @ c.T).T + M[:3, 3])
                if grip is None:
                    grip = M[:3, 3]
        if not pts:
            return None
        pts = np.concatenate(pts)
        d = pts - grip
        far = d[np.argmax(np.linalg.norm(d, axis=1))]
        tip = far / max(np.linalg.norm(far), 1e-9)
        q = d - np.outer(d @ tip, tip)
        w, v = np.linalg.eigh(q.T @ q)
        edge = v[:, int(np.argmax(w))]
        edge = edge - (edge @ tip) * tip
        edge /= max(np.linalg.norm(edge), 1e-9)
        if edge[np.argmax(np.abs(edge))] < 0:
            edge = -edge
        return grip, tip, float(np.linalg.norm(far)), edge

    def item_items(self):
        """The held items clearance measures: 'weapon' and 'shield' (the own shield bone's subtree or the item)."""
        items = []
        if self.weapon and self.path_shown(self.hand_bone):
            items.append('weapon')
        if self.shield_row and self.path_shown(self.off_hand_bone):
            items.append('shield')
        return items

    def shield_bones(self):
        """The own shield's bones: the tag-shield bone and its descendants."""
        top = [b['name'] for b in self.bones if b.get('tag') == 'shield']
        out = set(top)
        for b in self.bones:
            if any(a in out for a in self.path(b['name'])[:-1]):
                out.add(b['name'])
        return out

    # ---- kinematics
    def fk(self, local):
        """{bone: 4x4} in model space from {bone: (rotation deg, pivot)} (missing bones at rest)."""
        W = {}
        for b in self.bones:
            n = b['name']
            r, p = local.get(n, self.rest[n])
            L = mat(rot(r), p)
            par = self.parent[n]
            W[n] = W[par] @ L if par else L
        return W

    def rest_local(self):
        return {n: (r.copy(), p.copy()) for n, (r, p) in self.rest.items()}
