"""Software renderer for the real troop models: models/troop/<id>.json bones and boxes, the in-game skin, team cloth,
glow sheet and cube weapon, posed with custom bone rotations and drawn through a perspective camera into G-buffers
(albedo, normal, depth, mask, glow, specular). The title painting lights and paints over these buffers.
Model space is Minecraft's: px units, y down, the troop faces -z."""
import json, pathlib
import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'


def rot(deg):
    x, y, z = np.radians(deg)
    rx = np.array([[1, 0, 0], [0, np.cos(x), -np.sin(x)], [0, np.sin(x), np.cos(x)]])
    ry = np.array([[np.cos(y), 0, np.sin(y)], [0, 1, 0], [-np.sin(y), 0, np.cos(y)]])
    rz = np.array([[np.cos(z), -np.sin(z), 0], [np.sin(z), np.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx


def tex(path, ident=None):
    if ident:
        path = ASSETS / ident.split(':')[1]
    im = Image.open(path).convert('RGBA')
    return np.asarray(im).astype(np.float32) / 255


def box_quads(o, s, uv, mirror, dil, sheet, tsize):
    """The six faces of a Minecraft box: corners (4x3, model px) and uv corners (4x2, 0..1) in the vanilla layout."""
    x0, y0, z0 = o[0] - dil[0], o[1] - dil[1], o[2] - dil[2]
    x1, y1, z1 = o[0] + s[0] + dil[0], o[1] + s[1] + dil[1], o[2] + s[2] + dil[2]
    w, h, d = s
    u, v = uv
    fu, fv = 1 / sheet[0], 1 / sheet[1]

    def R(ua, va, ub, vb):   # rectangle top-left, top-right, bottom-right, bottom-left
        return [(ua, va), (ub, va), (ub, vb), (ua, vb)]
    faces = []
    # front (-z): left of texture is -x
    faces.append(([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], R(u + d, v + d, u + d + w, v + d + h), (0, 0, -1)))
    # back (+z): left of texture is +x
    faces.append(([(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)], R(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h), (0, 0, 1)))
    # -x side: left of texture is +z (back)
    faces.append(([(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)], R(u, v + d, u + d, v + d + h), (-1, 0, 0)))
    # +x side: left of texture is -z (front)
    faces.append(([(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], R(u + d + w, v + d, u + 2 * d + w, v + d + h), (1, 0, 0)))
    # top (-y): bottom edge of texture meets the front
    faces.append(([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], R(u + d, v, u + d + w, v + d), (0, -1, 0)))
    # bottom (+y)
    faces.append(([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], R(u + d + w, v, u + d + 2 * w, v + d), (0, 1, 0)))
    out = []
    for corners, uvs, n in faces:
        uvs = [(a * fu, b * fv) for a, b in uvs]
        if mirror:
            uvs = [uvs[1], uvs[0], uvs[3], uvs[2]]
        out.append((np.array(corners, float), np.array(uvs, float), np.array(n, float)))
    return out


class Troop:
    def __init__(self, tid, stage=3, cloth='violet', weapon_tier=''):
        self.tid = tid
        self.m = json.loads((ASSETS / f'models/troop/{tid}.json').read_text('utf8'))
        self.bones = {b['name']: b for b in self.m['bones']}
        sk = self.m['skins'][str(stage)] if str(stage) in self.m['skins'] else self.m['skins']['1']
        self.skin = tex(None, sk)
        self.stage = stage
        cl = self.m.get('cloth')
        self.cloth = None
        if cl:
            p = ASSETS / (cl.split(':')[1] + cloth + '.png')
            if p.exists():
                self.cloth = tex(p)
        g = self.m.get('glow')
        self.glow = tex(None, g) if g else None
        self.glow_from = self.m.get('glowFrom', 1)
        card = self.m.get('faces', {}).get('card', 0)
        self.card = card
        wid = self.m.get('weapon', '') + weapon_tier
        self.weapon = None
        wp = ASSETS / f'models/troop/weapon/{wid}.json'
        if self.m.get('weapon') and wp.exists():
            self.weapon = json.loads(wp.read_text('utf8'))
        self.hand = self.m.get('hand', 'right')

    def shown(self, b):
        while b:
            if b['stages'] and self.stage not in b['stages']:
                return False
            if b.get('variants') and b['variants'] != [self.card]:
                return False
            b = self.bones.get(b['parent'])
        return True

    def frames(self, pose):
        """World 4x4 of every bone: parent * translate(pivot) * rotate(rest + pose)."""
        M = {}

        def get(name):
            if name in M:
                return M[name]
            b = self.bones[name]
            P = get(b['parent']) if b['parent'] else np.eye(4)
            L = np.eye(4)
            L[:3, 3] = b['pivot']
            r = np.array(b['rotation'], float) + np.array(pose.get(name, (0, 0, 0)), float)
            L[:3, :3] = rot(r)
            M[name] = P @ L
            return M[name]
        for n in self.bones:
            get(n)
        return M

    def quads(self, pose, root=np.eye(4), skip=()):
        """Every visible face as (corners world 4x3, uv 4x2, texture id, normal world). Texture ids: 0 skin, 1 cloth, 2 weapon."""
        M = self.frames(pose)
        out = []
        for b in self.m['bones']:
            if not b['boxes'] or not self.shown(b) or b['name'] in skip:
                continue
            W = root @ M[b['name']]
            tid = (self.tid, 1 if b.get('team') and self.cloth is not None else 0)
            for bx in b['boxes']:
                for c, uv, n in box_quads(bx['origin'], bx['size'], bx['uv'], bx.get('mirror', False),
                                          bx.get('dilation', [0, 0, 0]), b['sheet'], None):
                    cw = (W[:3, :3] @ c.T).T + W[:3, 3]
                    out.append((cw, uv, tid, W[:3, :3] @ n, b['name']))
        if self.weapon:
            W = self.weapon_matrix(pose, root, M)
            ws = self.weapon['size']
            for q in self.weapon['frames'][0]['quads']:
                c = np.array(q[:12], float).reshape(4, 3)
                uv = np.array(q[12:20], float).reshape(4, 2) / np.array(ws, float)
                e1, e2 = c[1] - c[0], c[3] - c[0]
                n = np.cross(e1, e2)
                ln = np.linalg.norm(n)
                if ln < 1e-9:
                    continue
                n = n / ln
                cw = (W[:3, :3] @ c.T).T + W[:3, 3]
                out.append((cw, uv, (self.tid, 2), W[:3, :3] @ n, '_weapon'))
        return out

    def weapon_matrix(self, pose, root=np.eye(4), M=None):
        """The weapon model's world frame: hand bone, then the grip (the upright carry for a grip-less weapon), then pose['_grip']."""
        M = M or self.frames(pose)
        hand = self.hand + '_hand'
        if hand not in self.bones:
            hand = self.hand + '_arm'
        g = self.weapon['grip']
        off, gr = np.array(g['offset'], float), np.array(g['rotation'], float)
        if not off.any() and not gr.any() and not self.weapon.get('pull'):
            off, gr = np.array([0, 0, -3.0]), np.array([10.0, 0, 0])
        G = np.eye(4)
        G[:3, 3] = off
        G[:3, :3] = rot(gr)
        X = np.eye(4)
        X[:3, :3] = rot(pose.get('_grip', (0, 0, 0)))
        return root @ M[hand] @ G @ X

    def weapon_ends(self, pose, root=np.eye(4)):
        """World points of the weapon's two ends along its long (y) axis: (tip at min y, butt at max y)."""
        W = self.weapon_matrix(pose, root)
        ys = np.array([q[1::3][:4] for q in self.weapon['frames'][0]['quads']]).ravel()
        tip, butt = np.array([0, ys.min(), 0, 1.0]), np.array([0, ys.max(), 0, 1.0])
        return (W @ tip)[:3], (W @ butt)[:3]

    def weapon_tex(self):
        if not self.weapon:
            return None, None
        t = tex(None, self.weapon['texture'])
        s = tex(None, self.weapon['specular']) if self.weapon.get('specular') else None
        return t, s


class Camera:
    def __init__(self, eye, target, fov, W, H, up=(0, -1, 0)):
        self.eye = np.array(eye, float)
        f = np.array(target, float) - self.eye
        f /= np.linalg.norm(f)
        r = np.cross(f, np.array(up, float))
        r /= np.linalg.norm(r)
        u = np.cross(r, f)
        self.R = np.stack([r, u, f])   # rows: right, up, forward
        self.W, self.H = W, H
        self.fpx = 0.5 * H / np.tan(np.radians(fov) / 2)

    def project(self, p):
        q = (self.R @ (p - self.eye).T).T
        z = q[:, 2]
        x = self.W / 2 + self.fpx * q[:, 0] / z
        y = self.H / 2 - self.fpx * q[:, 1] / z
        return np.stack([x, y, z], 1)


def rasterize(items, cam, textures, ss=1):
    """items: list of (corners, uv, tex id, normal, group id). Returns G-buffers at camera size.
    textures: {tid: (albedo HxWx4, specular or None, glow or None)}."""
    W, H = cam.W, cam.H
    depth = np.full((H, W), np.inf, np.float32)
    alb = np.zeros((H, W, 3), np.float32)
    nrm = np.zeros((H, W, 3), np.float32)
    spec = np.zeros((H, W), np.float32)
    glow = np.zeros((H, W, 3), np.float32)
    gid = np.full((H, W), -1, np.int32)
    for corners, uv, tid, n, group in items:
        P = cam.project(corners)
        if (P[:, 2] <= 1).any():
            continue
        # back-face: normal pointing away from the eye
        cen = corners.mean(0)
        if np.dot(n, cam.eye - cen) <= 0:
            continue
        tA, tS, tG = textures[tid]
        for tri in ((0, 1, 2), (0, 2, 3)):
            p = P[list(tri)]
            t = uv[list(tri)]
            x0, x1 = int(max(0, np.floor(p[:, 0].min()))), int(min(W - 1, np.ceil(p[:, 0].max())))
            y0, y1 = int(max(0, np.floor(p[:, 1].min()))), int(min(H - 1, np.ceil(p[:, 1].max())))
            if x1 < x0 or y1 < y0:
                continue
            xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            (ax, ay), (bx, by), (cx, cy) = p[0, :2], p[1, :2], p[2, :2]
            den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
            if abs(den) < 1e-9:
                continue
            l0 = ((by - cy) * (xs - cx) + (cx - bx) * (ys - cy)) / den
            l1 = ((cy - ay) * (xs - cx) + (ax - cx) * (ys - cy)) / den
            l2 = 1 - l0 - l1
            inside = (l0 >= -1e-4) & (l1 >= -1e-4) & (l2 >= -1e-4)
            if not inside.any():
                continue
            iz = l0 / p[0, 2] + l1 / p[1, 2] + l2 / p[2, 2]
            z = 1 / iz
            u = (l0 * t[0, 0] / p[0, 2] + l1 * t[1, 0] / p[1, 2] + l2 * t[2, 0] / p[2, 2]) * z
            v = (l0 * t[0, 1] / p[0, 2] + l1 * t[1, 1] / p[1, 2] + l2 * t[2, 1] / p[2, 2]) * z
            th, tw = tA.shape[:2]
            ui = np.clip((u * tw).astype(int), 0, tw - 1)
            vi = np.clip((v * th).astype(int), 0, th - 1)
            a = tA[vi, ui, 3]
            sub = depth[y0:y1 + 1, x0:x1 + 1]
            ok = inside & (a > 0.5) & (z < sub)
            if not ok.any():
                continue
            sub[ok] = z[ok]
            alb[y0:y1 + 1, x0:x1 + 1][ok] = tA[vi, ui, :3][ok]
            nrm[y0:y1 + 1, x0:x1 + 1][ok] = n
            gid[y0:y1 + 1, x0:x1 + 1][ok] = group
            spec[y0:y1 + 1, x0:x1 + 1][ok] = tS[vi, ui, 0][ok] if tS is not None else 0
            if tG is not None:
                glow[y0:y1 + 1, x0:x1 + 1][ok] = (tG[vi, ui, :3] * tG[vi, ui, 3:4])[ok]
            else:
                glow[y0:y1 + 1, x0:x1 + 1][ok] = 0
    mask = np.isfinite(depth)
    return dict(depth=depth, albedo=alb, normal=nrm, spec=spec, glow=glow, mask=mask, gid=gid)


def troop_textures(t):
    wt, ws = t.weapon_tex()
    return {(t.tid, 0): (t.skin, None, t.glow), (t.tid, 1): (t.cloth if t.cloth is not None else t.skin, None, None),
            (t.tid, 2): (wt, ws, None) if wt is not None else (t.skin, None, None)}
