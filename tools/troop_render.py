"""Draws a troop from its game model files, posed, for animation previews.

    python tools/troop_render.py <troop> [--tier N] [--team gold] [--out file.png]

Reads models/troop/<troop>.json (format 3: bones, boxes, faceuv, dilation, mirror, stages, variants, team cloth), its skin and
team cloth textures, and its weapon model (models/troop/weapon/<id>[_t<n>].json, gripped in the hand as TroopModels.Weapon.grip
does). Bones pose as TroopModel does: pivot plus the pose's pixels, rest rotation plus the pose's degrees, turned Z, then Y,
then X (ModelPart.rotate); boxes keep Minecraft's cuboid UV layout. The view is orthographic, the floor at y 24 (model px, y down,
-z forward). Not drawn: the outer layer twin, glow, the veteran trim and the vanilla shield item (the model's own shield
bone is drawn). Used by tools/troop_anim.py; the pose comes from the game's own engine (tools/troop_anim's MotionPreview)."""
import argparse
import json
import math
import pathlib

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'
MODELS = ASSETS / 'models/troop'
FACE_DIRS = {'down': 'bottom', 'up': 'top', 'west': 'right', 'north': 'front', 'east': 'left', 'south': 'back'}


def tex_path(ident):
    ns, path = ident.split(':', 1)
    return ROOT / 'src/main/resources/assets' / ns / path


def load_png(path):
    return np.asarray(Image.open(path).convert('RGBA'), dtype=np.uint8)


def rot(deg):
    """ModelPart's rotation: Rz . Ry . Rx (degrees)."""
    x, y, z = (math.radians(a) for a in deg)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


class Quad:
    """A textured parallelogram in its bone's frame: corner o, edges a and b; uv = (u, v at o, du and dv along a, du and dv along b)."""
    __slots__ = ('o', 'a', 'b', 'uv', 'tex')

    def __init__(self, o, a, b, uv, tex):
        self.o, self.a, self.b, self.uv, self.tex = np.asarray(o, float), np.asarray(a, float), np.asarray(b, float), uv, tex


def cuboid(box, tex, sheet):
    """Minecraft's ModelPart.Cuboid as quads (its vertex order and UV layout; mirror swaps x; faceuv moves one face's rectangle)."""
    ox, oy, oz = box['origin']
    sx, sy, sz = box['size']
    dx, dy, dz = box.get('dilation', [0, 0, 0])
    u, v = box['uv']
    x0, y0, z0, x1, y1, z1 = ox - dx, oy - dy, oz - dz, ox + sx + dx, oy + sy + dy, oz + sz + dz
    if box.get('mirror'):
        x0, x1 = x1, x0
    V = {1: (x0, y0, z0), 2: (x1, y0, z0), 3: (x1, y1, z0), 4: (x0, y1, z0), 5: (x0, y0, z1), 6: (x1, y0, z1), 7: (x1, y1, z1), 8: (x0, y1, z1)}
    j, k, l, m, n, o = u, u + sz, u + sz + sx, u + sz + sx + sx, u + sz + sx + sz, u + sz + sx + sz + sx
    p, q, r = v, v + sz, v + sz + sy
    faces = {  # Minecraft's sides: vertices, then (u1, v1, u2, v2)
        'down': ((6, 5, 1, 2), (k, p, l, q)), 'up': ((3, 4, 8, 7), (l, q, m, p)),
        'west': ((1, 5, 8, 4), (j, q, k, r)), 'north': ((2, 1, 4, 3), (k, q, l, r)),
        'east': ((6, 2, 3, 7), (l, q, n, r)), 'south': ((5, 6, 7, 8), (n, q, o, r))}
    moved = box.get('faceuv') or {}
    out = []
    for face, (vs, (u1, v1, u2, v2)) in faces.items():
        name = FACE_DIRS[face]
        if name in moved:
            fu, fv = moved[name]
            w, h = u2 - u1, v2 - v1
            u1, v1 = fu if w >= 0 else fu - w, fv if h >= 0 else fv - h
            u2, v2 = u1 + w, v1 + h
        if (abs(u2 - u1) < 1e-9 and abs(v2 - v1) < 1e-9):
            continue
        a0, a1, a2 = (np.array(V[i], float) for i in vs[:3])
        # Minecraft's Quad: vertex 0 takes (u2, v1), 1 (u1, v1), 2 (u1, v2), 3 (u2, v2) (mirror only flips the winding)
        out.append(Quad(a1, a0 - a1, a2 - a1, (u1, v1, u2 - u1, 0.0, 0.0, v2 - v1), tex))
    return out


class Bone:
    def __init__(self, b):
        self.name, self.parent = b['name'], b.get('parent', '')
        self.pivot = np.array(b.get('pivot', [0, 0, 0]), float)
        self.rest = np.array(b.get('rotation', [0, 0, 0]), float)
        self.quads = []


class Troop:
    """A troop's model at one armour tier, skin, team cloth and face, with its weapon in hand."""

    def __init__(self, troop, tier=1, skin=None, team="gold", face=0):
        self.id = troop
        j = json.loads((MODELS / (troop + '.json')).read_text(encoding='utf-8'))
        self.json = j
        self.rig = j['rig']
        skin_tex = load_png(tex_path(j["skins"][str(skin or tier)]))  # skins are per armour tier
        cloth = j.get('cloth') or ''
        cloth_tex = load_png(tex_path(cloth + team + '.png')) if cloth else None
        self.bones, self.order = {}, []
        hidden = set()
        for b in j['bones']:
            bone = Bone(b)
            stages, variants = set(b.get('stages') or []), set(b.get('variants') or [])
            arms = set(b.get('weaponTiers') or [])
            hide = bool(stages) and tier not in stages or bool(variants) and face not in variants or bool(arms) and tier not in arms
            folded = j.get('idleArms') == 'folded'
            hide |= b['name'] == 'arms_folded' if not folded else b['name'] in ('right_arm', 'left_arm')
            hide |= bone.parent in hidden
            if hide:
                hidden.add(bone.name)
            if not hide:
                tex = cloth_tex if b.get('team') else skin_tex
                for box in b.get('boxes') or []:
                    if tex is not None:
                        bone.quads += cuboid(box, tex, b.get('sheet'))
            self.bones[bone.name] = bone
            self.order.append(bone.name)
        self.hidden = hidden
        self.weapon = None
        wid = j.get('weapon') or 'none'
        if wid != 'none':
            for name in ([f'{wid}_t{tier}'] if tier > 1 else []) + [wid]:
                path = MODELS / 'weapon' / (name + '.json')
                if path.is_file():
                    self.weapon = Weapon(json.loads(path.read_text(encoding='utf-8')))
                    break
        self.hand = (j.get('hand') or 'right') + '_hand'

    def matrices(self, pose):
        """Each bone's 4x4 in model space under pose {bone: [rx, ry, rz, tx, ty, tz]} (degrees and px added to rest)."""
        out = {}
        for name in self.order:
            b = self.bones[name]
            ch = pose.get(name, (0, 0, 0, 0, 0, 0))
            M = np.eye(4)
            M[:3, :3] = rot(b.rest + np.asarray(ch[:3], float))
            M[:3, 3] = b.pivot + np.asarray(ch[3:6], float)
            out[name] = out[b.parent] @ M if b.parent else M
        return out

    def quads(self, pose, draw=None):
        """[(quad, 4x4)] for every shown box and the weapon (at the pull frame a bow's draw seconds show), posed."""
        mats = self.matrices(pose)
        items = [(q, mats[n]) for n in self.order if n not in self.hidden for q in self.bones[n].quads]
        if self.weapon is not None and self.hand in mats and self.hand not in self.hidden:
            fq = self.weapon.frames[min(self.weapon.frame_at(draw), len(self.weapon.frames) - 1)]
            items += [(q, mats[self.hand] @ self.weapon.grip) for q in fq]
        return items, mats


class Weapon:
    """A weapon model's rest frame as quads in its grip frame (TroopModels.Weapon.grip: offset, then Rz . Ry . Rx)."""

    def __init__(self, w):
        tex = load_png(tex_path(w['texture']))
        g = w.get('grip') or {}
        self.grip = np.eye(4)
        self.grip[:3, :3] = rot(g.get('rotation', [0, 0, 0]))
        self.grip[:3, 3] = g.get('offset', [0, 0, 0])
        tw, th = w['size']
        sx, sy = tex.shape[1] / tw, tex.shape[0] / th
        self.pull = w.get('pull') or []
        self.frames = []
        for fr in w['frames']:
            self.quads = []
            self._frame(fr, tex, sx, sy)
            self.frames.append(self.quads)
        self.quads = self.frames[0]

    def frame_at(self, seconds, loose=0.8):
        """TroopModels.Weapon.frameAt: the pull frame a draw time shows (0, the rest frame, before the first pull and from the loose)."""
        if not self.pull or seconds is None or seconds < self.pull[0] or seconds >= loose:
            return 0
        return 1 + max(i for i, p in enumerate(self.pull) if seconds >= p)

    def _frame(self, fr, tex, sx, sy):
        for qd in fr['quads']:
            P = [np.array(qd[i * 3:i * 3 + 3], float) for i in range(4)]
            uv = [(qd[12 + i * 2] * sx, qd[13 + i * 2] * sy) for i in range(4)]
            # TroopMesh quads: four corners with their texels; draw as the parallelogram from corner 1 (edges to 0 and 2)
            self.quads.append(Quad(P[1], P[0] - P[1], P[2] - P[1],
                                   (uv[1][0], uv[1][1], uv[0][0] - uv[1][0], uv[0][1] - uv[1][1], uv[2][0] - uv[1][0], uv[2][1] - uv[1][1]), tex))


def render(items, yaw=0.0, pitch=10.0, scale=8.0, size=(320, 400), anchor=None, bg=(0, 0, 0, 0)):
    """Orthographic, z-buffered draw of [(quad, 4x4)]. yaw 0 looks at the troop's front (from -z); positive yaw walks the camera to
    the troop's right. pitch looks down. anchor: the screen point of the model origin's floor (x, y of model point (0, 24, 0))."""
    W, H = size
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    # model space: y down, the troop faces -z. The camera stands in front (at -z) looking +z, walked round to the troop's right
    # by yaw and tilted down by pitch; screen right is cross(forward, up) with up = -y
    fwd = np.array([sy, 0.0, cy])
    right = np.array([cy, 0.0, -sy])
    down = np.array([0.0, 1.0, 0.0])
    fwd2 = fwd * cp + down * sp
    down2 = down * cp - fwd * sp
    ax, ay = anchor if anchor else (W / 2, H * 0.92)
    floor = np.array([0.0, 24.0, 0.0])
    col = np.zeros((H, W, 4), np.float32)
    col[:] = bg
    depth = np.full((H, W), np.inf)
    for q, M in items:
        R, t = M[:3, :3], M[:3, 3]
        o, a, b = R @ q.o + t, R @ q.a, R @ q.b
        n = np.cross(a, b)
        nn = np.linalg.norm(n)
        if nn < 1e-12:
            continue
        n /= nn
        P = lambda p: np.array([ax + np.dot(p - floor, right) * scale, ay + np.dot(p - floor, down2) * scale, np.dot(p, fwd2)])
        so = P(o)
        sa, sb = P(o + a) - so, P(o + b) - so
        det = sa[0] * sb[1] - sa[1] * sb[0]
        if abs(det) < 1e-9:
            continue
        xs = [so[0], so[0] + sa[0], so[0] + sb[0], so[0] + sa[0] + sb[0]]
        ys = [so[1], so[1] + sa[1], so[1] + sb[1], so[1] + sa[1] + sb[1]]
        x0, x1 = max(0, int(math.floor(min(xs)))), min(W - 1, int(math.ceil(max(xs))))
        y0, y1 = max(0, int(math.floor(min(ys)))), min(H - 1, int(math.ceil(max(ys))))
        if x1 < x0 or y1 < y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        dx, dy = gx - so[0], gy - so[1]
        s = (dx * sb[1] - dy * sb[0]) / det
        u = (sa[0] * dy - sa[1] * dx) / det
        inside = (s >= 0) & (s < 1) & (u >= 0) & (u < 1)
        if not inside.any():
            continue
        z = so[2] + s * sa[2] + u * sb[2]
        tex = q.tex
        uo, vo, dua, dva, dub, dvb = q.uv
        tu = uo + s * dua + u * dub
        tv = vo + s * dva + u * dvb
        us, vs = (uo, uo + dua, uo + dub, uo + dua + dub), (vo, vo + dva, vo + dvb, vo + dva + dvb)
        ix = np.clip(np.floor(tu).astype(int), int(math.floor(min(us) + 1e-6)), int(math.ceil(max(us) - 1e-6)) - 1).clip(0, tex.shape[1] - 1)
        iy = np.clip(np.floor(tv).astype(int), int(math.floor(min(vs) + 1e-6)), int(math.ceil(max(vs) - 1e-6)) - 1).clip(0, tex.shape[0] - 1)
        texel = tex[iy, ix]
        vis = inside & (texel[..., 3] > 8)
        sub = depth[y0:y1 + 1, x0:x1 + 1]
        vis &= z < sub - 1e-6
        if not vis.any():
            continue
        facing = n if np.dot(n, fwd2) < 0 else -n   # the side the camera sees (boxes are drawn both sides, z decides)
        k = 0.6 + 0.4 * (0.7 * facing[0] ** 2 + 0.86 * facing[2] ** 2 + (1.0 if facing[1] < 0 else 0.5) * facing[1] ** 2)
        region = col[y0:y1 + 1, x0:x1 + 1]
        region[vis, :3] = texel[..., :3][vis].astype(np.float32) * k
        region[vis, 3] = 255
        sub[vis] = z[vis]
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), 'RGBA')


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('troop')
    ap.add_argument('--tier', type=int, default=1)
    ap.add_argument('--team', default='gold')
    ap.add_argument('--out', default=None)
    a = ap.parse_args(argv)
    t = Troop(a.troop, a.tier, team=a.team)
    items, _ = t.quads({})
    views = [render(items, yaw, 12, 8, (260, 330)) for yaw in (0, 45, 90, 180)]
    sheet = Image.new('RGBA', (260 * 4, 330), (40, 44, 52, 255))
    for i, v in enumerate(views):
        sheet.alpha_composite(v, (260 * i, 0))
    out = a.out or f'{a.troop}_t{a.tier}.png'
    sheet.save(out)
    print(out)


if __name__ == '__main__':
    main()
