"""Heraldic units: the A/B sheet, baseline against my version.

Renders the same troop from two asset roots (the baseline and mine) at the game's
read size, so the two can be judged against each other rather than in the
abstract. 36 px is the size the rubric asks for: that is the unit as it appears
in a battle of 160.

  python work/p2b/hx_compare.py <baseline assets> <my assets> <out.png> [troop ...]
"""
import json
import pathlib
import sys

import math

import numpy as np
from PIL import Image, ImageDraw

BG = (36, 34, 42)
INK = (230, 225, 210)
MUTED = (155, 151, 142)
FIELD = (150, 170, 190)


def tex_path(root, ident):
    ns, path = ident.split(':', 1)
    return root / path


def rot(deg):
    x, y, z = (math.radians(a) for a in deg)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx


def cuboid_quads(box):
    (x, y, z), (sx, sy, sz), (u, v) = box['origin'], box['size'], box['uv']
    d = box.get('dilation', [0, 0, 0])
    f, g, h = x + sx + d[0], y + sy + d[1], z + sz + d[2]
    x, y, z = x - d[0], y - d[1], z - d[2]
    mirror = box.get('mirror', False)
    if mirror:
        x, f = f, x
    v1 = (x, y, z); v2 = (f, y, z); v3 = (f, g, z); v4 = (x, g, z)
    v5 = (x, y, h); v6 = (f, y, h); v7 = (f, g, h); v8 = (x, g, h)
    j = u; k = u + sz; l = u + sz + sx; m = u + sz + sx + sx; n = u + sz + sx + sz; o = u + sz + sx + sz + sx
    p = v; q = v + sz; r = v + sz + sy
    out = []
    for verts, u1, w1, u2, w2, name in (((v6, v5, v1, v2), k, p, l, q, 'top'), ((v3, v4, v8, v7), l, q, m, p, 'bottom'),
                                          ((v1, v5, v8, v4), j, q, k, r, 'west'), ((v2, v1, v4, v3), k, q, l, r, 'north'),
                                          ((v6, v2, v3, v7), l, q, n, r, 'east'), ((v5, v6, v7, v8), n, q, o, r, 'south')):
        uvs = [(u2, w1), (u1, w1), (u1, w2), (u2, w2)]
        if mirror:
            verts = verts[::-1]; uvs = uvs[::-1]
        out.append((verts, uvs, name))
    return out


def world(bones):
    out = {}
    for b in bones:
        r, t = out.get(b['parent'], (np.eye(3), np.zeros(3)))
        out[b['name']] = (r @ rot(b['rotation']), t + r @ np.array(b['pivot'], float))
    return out


LIGHTS = [l / np.linalg.norm(l) for l in (np.array([0.2, 1.0, -0.7]), np.array([-0.2, 1.0, 0.7]))]


def shade(normal):
    return min(1.0, 0.4 + 0.6 * sum(max(0.0, float(normal @ l)) for l in LIGHTS))


class Canvas:
    def __init__(self, w, h, bg=FIELD):
        self.rgb = np.zeros((h, w, 3), float); self.rgb[:] = bg
        self.z = np.full((h, w), np.inf)

    def quad(self, pts, uvs, img, light):
        P0 = np.array(pts[1][:2]); A = np.array(pts[0][:2]) - P0; B = np.array(pts[2][:2]) - P0
        det = A[0] * B[1] - A[1] * B[0]
        if abs(det) < 1e-9:
            return
        H, W = self.z.shape
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        x0, x1 = max(0, int(math.floor(min(xs)))), min(W, int(math.ceil(max(xs))))
        y0, y1 = max(0, int(math.floor(min(ys)))), min(H, int(math.ceil(max(ys))))
        if x0 >= x1 or y0 >= y1:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
        dx, dy = gx - P0[0], gy - P0[1]
        s = (dx * B[1] - dy * B[0]) / det
        t = (A[0] * dy - A[1] * dx) / det
        inside = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
        if not inside.any():
            return
        z0 = pts[1][2]; za = pts[0][2] - z0; zb = pts[2][2] - z0
        depth = z0 + s * za + t * zb
        uv0 = np.array(uvs[1]); ua = np.array(uvs[0]) - uv0; ub = np.array(uvs[2]) - uv0
        tu = uv0[0] + s * ua[0] + t * ub[0]; tv = uv0[1] + s * ua[1] + t * ub[1]
        iu = np.clip(np.floor(tu).astype(int), 0, img.shape[1] - 1); iv = np.clip(np.floor(tv).astype(int), 0, img.shape[0] - 1)
        texel = img[iv, iu]
        opaque = inside & (texel[..., 3] > 0)
        zs = self.z[y0:y1, x0:x1]
        win = opaque & (depth < zs)
        zs[win] = depth[win]
        self.rgb[y0:y1, x0:x1][win] = texel[..., :3][win] * light


def render(root, troop, stage=1, side='gold', yaws=(0, 30, 90, 150), scale=6, bg=FIELD):
    models = root / 'models/troop'
    model = json.loads((models / (troop + '.json')).read_text())
    skin = np.array(Image.open(tex_path(root, model['skins'][str(stage)])).convert('RGBA'), float)
    cloth_name = None if side in ('none', 'company', 'linen') else side
    cloth = np.array(Image.open(root / ('textures/entity/troop/team_cloth_%s.png' % (cloth_name or 'linen')))
                     .convert('RGBA'), float)
    wp = None
    if model['weapon'] != 'none':
        wj = json.loads((models / 'weapon' / (model['weapon'] + '.json')).read_text())
        wp = (wj, np.array(Image.open(tex_path(root, wj['texture'])).convert('RGBA'), float))
    rig = json.loads((models / 'rig' / (model['rig'] + '.json')).read_text())
    bones = model['bones']
    wt = world(bones)
    hidden = set()
    flags_on = side not in ('none', 'company', 'linen')
    for b in bones:
        if b['parent'] in hidden:
            hidden.add(b['name']); continue
        if b['stages'] and stage not in b['stages']:
            hidden.add(b['name'])
        if b['part'] in ('pennant', 'standard') and not flags_on:
            hidden.add(b['name'])
    views = []
    for yaw in yaws:
        ry = rot([0, yaw, 0])
        canvas = Canvas(48 * scale, 64 * scale, bg)
        cx, cy = 24 * scale, 56 * scale
        def project(p):
            q = ry @ p
            return (cx + q[0] * scale, cy + (q[1] - 24) * scale, q[2])
        for b in bones:
            if b['name'] in hidden or not b['boxes']:
                continue
            r, t = wt[b['name']]
            img = cloth if b['team'] else skin
            for box in b['boxes']:
                for verts, uvs, name in cuboid_quads(box):
                    pts = [project(t + r @ np.array(v, float)) for v in verts]
                    e1 = np.array(verts[0]) - np.array(verts[1]); e2 = np.array(verts[2]) - np.array(verts[1])
                    nrm = ry @ (r @ np.cross(e1, e2)); ln = np.linalg.norm(nrm)
                    if ln < 1e-9 or nrm[2] <= 1e-6:
                        continue
                    nrm /= ln
                    light = shade(np.array([nrm[0], -nrm[1], -nrm[2]]))
                    canvas.quad(pts, uvs, img, light)
        if wp is not None:
            wj, wimg = wp
            hand = rig['hands'][model['hand']]
            r, t = wt[hand]
            g = wj['grip']
            gr = r @ rot(g['rotation']); gt = t + r @ np.array(g['offset'], float)
            for box in wj['boxes']:
                for verts, uvs, name in cuboid_quads(box):
                    pts = [project(gt + gr @ np.array(v, float)) for v in verts]
                    e1 = np.array(verts[0]) - np.array(verts[1]); e2 = np.array(verts[2]) - np.array(verts[1])
                    nrm = ry @ (gr @ np.cross(e1, e2)); ln = np.linalg.norm(nrm)
                    if ln < 1e-9 or nrm[2] <= 1e-6:
                        continue
                    nrm /= ln
                    light = shade(np.array([nrm[0], -nrm[1], -nrm[2]]))
                    canvas.quad(pts, uvs, wimg, light)
        views.append(Image.fromarray(np.clip(canvas.rgb, 0, 255).astype(np.uint8)))
    return views


def main(baseline, mine, out, troops):
    ROW = 150
    W = 1500
    sheet = Image.new('RGB', (W, 40 + len(troops) * ROW), BG)
    d = ImageDraw.Draw(sheet)
    d.text((12, 12), 'BASELINE (left) against HERALDIC (right): 36 px read size, 4 yaws above', fill=INK)
    for r, t in enumerate(troops):
        y = 40 + r * ROW
        for col, root in enumerate((baseline, mine)):
            views = render(root, t)
            w, h = views[0].size
            x = 12 + col * 740
            for i, v in enumerate(views):
                small = v.resize((max(1, w * 36 // h), 36), Image.BOX)
                sheet.paste(small, (x + i * 44, y + 8))
                big = v.resize((w // 2, h // 2), Image.LANCZOS)
                sheet.paste(big, (x + i * 44, y + 50))
            d.text((x, y + 128), ('%s  %s' % ('baseline' if col == 0 else 'heraldic', t)), fill=MUTED)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    args = sys.argv[1:]
    troops = args[3:] or ['knight', 'archer', 'sentinel', 'longbow', 'mage', 'gl_marshal']
    main(pathlib.Path(args[0]), pathlib.Path(args[1]), pathlib.Path(args[2]), troops)
