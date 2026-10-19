"""Scratch preview of a generated troop model (orthographic, z-buffered, nearest texels) for fast paint iteration.
Not evidence: grading uses the in-game harness renders. Mirrors ModelPart.Cuboid's vertex and UV layout exactly.

  python work/p2b/preview.py <troop> [--stage N] [--side gold|violet|company|linen|none] [--out file.png]
"""
import json, math, pathlib, sys
import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'


def tex_path(ident):
    ns, path = ident.split(':', 1)
    return ASSETS / path


def rot(deg):
    x, y, z = (math.radians(a) for a in deg)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx


def cuboid_quads(box, tex_w, tex_h):
    """The six quads of a ModelPart.Cuboid: (four corners in the bone frame, four (u, v) texel coords, face name)."""
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


LIGHTS = [np.array([0.2, 1.0, -0.7]), np.array([-0.2, 1.0, 0.7])]
LIGHTS = [l / np.linalg.norm(l) for l in LIGHTS]


def shade(normal_world_up):
    s = 0.4 + 0.6 * sum(max(0.0, float(normal_world_up @ l)) for l in LIGHTS)
    return min(1.0, s)


class Canvas:
    def __init__(self, w, h, bg=(150, 170, 190)):
        self.rgb = np.zeros((h, w, 3), float); self.rgb[:] = bg
        self.z = np.full((h, w), np.inf)

    def quad(self, pts, uvs, img, tw, th, light):
        """pts: four (sx, sy, depth) screen points (a parallelogram); uvs: texel coords at them; img: HxWx4 array."""
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


def render(troop, stage=1, side='gold', yaws=(0, 30, 150, 90), scale=6, folded=False, weapon=True, skin_file=None, bg=(150, 170, 190)):
    model = json.loads((ASSETS / 'models/troop' / (troop + '.json')).read_text())
    W, H = model['size']
    skin = np.array(Image.open(skin_file or tex_path(model['skins'][str(stage)])).convert('RGBA'), float)
    cloth_name = {'none': None}.get(side, side)
    cloth = np.array(Image.open(ASSETS / ('textures/entity/troop/team_cloth_%s.png' % (cloth_name or 'linen'))).convert('RGBA'), float)
    wp = None
    if weapon and model['weapon'] != 'none':
        wj = json.loads((ASSETS / 'models/troop/weapon' / (model['weapon'] + '.json')).read_text())
        wp = (wj, np.array(Image.open(tex_path(wj['texture'])).convert('RGBA'), float))
    rig = json.loads((ASSETS / 'models/troop/rig' / (model['rig'] + '.json')).read_text())
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
        if b['name'] == 'arms_folded' and not folded:
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
                for verts, uvs, name in cuboid_quads(box, W, H):
                    pts = [project(t + r @ np.array(v, float)) for v in verts]
                    e1 = np.array(verts[0]) - np.array(verts[1]); e2 = np.array(verts[2]) - np.array(verts[1])
                    nrm = ry @ (r @ np.cross(e1, e2)); ln = np.linalg.norm(nrm)
                    if ln < 1e-9 or nrm[2] <= 1e-6:
                        continue
                    nrm /= ln
                    up = np.array([nrm[0], -nrm[1], -nrm[2]])
                    canvas.quad(pts, uvs, img, W, H, shade(up))
        if wp is not None:
            wj, wimg = wp
            hand = rig['hands'][model['hand']]
            r, t = wt[hand]
            g = wj['grip']
            gr = r @ rot(g['rotation']); gt = t + r @ np.array(g['offset'], float)
            for box in wj['boxes']:
                for verts, uvs, name in cuboid_quads(box, *wj['size']):
                    pts = [project(gt + gr @ np.array(v, float)) for v in verts]
                    e1 = np.array(verts[0]) - np.array(verts[1]); e2 = np.array(verts[2]) - np.array(verts[1])
                    nrm = ry @ (gr @ np.cross(e1, e2)); ln = np.linalg.norm(nrm)
                    if ln < 1e-9 or nrm[2] <= 1e-6:
                        continue
                    nrm /= ln
                    up = np.array([nrm[0], -nrm[1], -nrm[2]])
                    canvas.quad(pts, uvs, wimg, *wj['size'], shade(up))
        views.append(Image.fromarray(np.clip(canvas.rgb, 0, 255).astype(np.uint8)))
    return views


def sheet(troop, stage=1, side='gold', out=None):
    views = render(troop, stage, side)
    w, h = views[0].size
    small = [v.resize((max(1, w * 36 // h), 36), Image.BOX) for v in views]
    im = Image.new('RGB', (w * len(views), h + 44), (40, 40, 48))
    for i, v in enumerate(views):
        im.paste(v, (i * w, 0))
        im.paste(small[i], (i * w + 4, h + 4))
    out = out or (ROOT / 'work/p2b/preview' / ('%s_s%d_%s.png' % (troop, stage, side)))
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    return out


def lineup(troops, side='gold', yaw=30, out=None, scale=4):
    """The troops side by side at one scale: colour, then the solid silhouette, then both at 36 px tall."""
    cols = []
    for t in troops:
        tr, st = (t.split(':') + ['1'])[:2]
        v = render(tr, int(st), side, yaws=(yaw,), scale=scale)[0]
        sil = render(tr, int(st), side, yaws=(yaw,), scale=scale, bg=(255, 255, 255))[0]
        a = np.array(sil); mask = (np.abs(a.astype(int) - 255).sum(axis=2) > 0)
        a[mask] = (30, 30, 34); a[~mask] = (235, 235, 235)
        cols.append((v, Image.fromarray(a)))
    w, h = cols[0][0].size
    im = Image.new('RGB', (w * len(cols), h * 2 + 60), (40, 40, 48))
    for i, (v, sil) in enumerate(cols):
        im.paste(v, (i * w, 0)); im.paste(sil, (i * w, h))
        im.paste(v.resize((w * 36 // h * 2 // 2 if False else max(1, w * 40 // h), 40), Image.BOX), (i * w + 4, 2 * h + 10))
        im.paste(sil.resize((max(1, w * 40 // h), 40), Image.BOX), (i * w + 40, 2 * h + 10))
    out = out or (ROOT / 'work/p2b/preview' / ('lineup_%s_y%d.png' % (side, yaw)))
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    return out


if __name__ == '__main__':
    args = sys.argv[1:]
    if args[0] == '--lineup':
        side = args[args.index('--side') + 1] if '--side' in args else 'gold'
        yaw = int(args[args.index('--yaw') + 1]) if '--yaw' in args else 30
        print(lineup([a for a in args[1:] if not a.startswith('--') and a not in (side, str(yaw))], side, yaw))
        sys.exit(0)
    troop = args[0]
    stage = int(args[args.index('--stage') + 1]) if '--stage' in args else 1
    side = args[args.index('--side') + 1] if '--side' in args else 'gold'
    skin = (ROOT / 'work/troops' / (troop + '_template.png')) if '--template' in args else None
    out = (ROOT / 'work/p2b/preview' / ('%s_template.png' % troop)) if skin else None
    views = render(troop, stage, side, skin_file=skin)
    w, h = views[0].size
    small = [v.resize((max(1, w * 36 // h), 36), Image.BOX) for v in views]
    im = Image.new('RGB', (w * len(views), h + 44), (40, 40, 48))
    for i, v in enumerate(views):
        im.paste(v, (i * w, 0)); im.paste(small[i], (i * w + 4, h + 4))
    out = out or (ROOT / 'work/p2b/preview' / ('%s_s%d_%s.png' % (troop, stage, side)))
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    print(out)
