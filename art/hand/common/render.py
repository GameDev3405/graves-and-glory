"""A small orthographic renderer for hand-authored troops: Java ModelPart box UVs, bone pivots and rotations,
flat entity-style face light. It draws every texel as its own quad, far to near, so pixels stay crisp."""
import math
from PIL import Image, ImageDraw

def rot(rx, ry, rz):
    """rotationZYX(rz, ry, rx) as a 3x3 matrix, angles in degrees (Rz * Ry * Rx)."""
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    Rx = [[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]]
    Ry = [[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]]
    Rz = [[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]]
    return mul(Rz, mul(Ry, Rx))

def mul(A, B): return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def app(M, p): return [sum(M[i][k] * p[k] for k in range(3)) for i in range(3)]

def faces_of(b):
    """(face, corner(u,v) -> point, texel w, texel h, uv origin) for each face of a box, in its bone's frame."""
    w, h, d = (int(round(v)) for v in b.size)
    dl = b.dil if isinstance(b.dil, (list, tuple)) else [b.dil] * 3
    x0, y0, z0 = (b.origin[i] - dl[i] for i in range(3))
    x1, y1, z1 = (b.origin[i] + b.size[i] + dl[i] for i in range(3))
    u, v = b.uv
    L = lambda a0, a1, t: a0 + (a1 - a0) * t
    out = []
    if w and h: out.append(("front", lambda s, t: (L(x0, x1, s), L(y0, y1, t), z0), w, h, (u + d, v + d), (0, 0, -1)))
    if w and h: out.append(("back", lambda s, t: (L(x1, x0, s), L(y0, y1, t), z1), w, h, (u + 2 * d + w, v + d), (0, 0, 1)))
    if d and h: out.append(("right", lambda s, t: (x0, L(y0, y1, t), L(z1, z0, s)), d, h, (u, v + d), (-1, 0, 0)))
    if d and h: out.append(("left", lambda s, t: (x1, L(y0, y1, t), L(z0, z1, s)), d, h, (u + d + w, v + d), (1, 0, 0)))
    if w and d: out.append(("top", lambda s, t: (L(x0, x1, s), y0, L(z1, z0, t)), w, d, (u + d, v), (0, -1, 0)))
    if w and d: out.append(("bottom", lambda s, t: (L(x0, x1, s), y1, L(z0, z1, t)), w, d, (u + d + w, v), (0, 1, 0)))
    return out

def render(bones, visible, sheets, yaw=0, pitch=0, px=8, bg=(40, 42, 50, 255), extra=()):
    """bones: Bone list; visible(bone)->bool; sheets: {'skin': img, 'cloth': img}. Returns an RGBA image."""
    mats, offs = {}, {}
    for bn in bones:
        pm, po = mats.get(bn.parent, rot(0, 0, 0)), offs.get(bn.parent, [0, 0, 0])
        o = [po[i] + app(pm, bn.pivot)[i] for i in range(3)]
        mats[bn.name], offs[bn.name] = mul(pm, rot(*bn.rot)), o
    cam = mul(rot(pitch, 0, 0), rot(0, yaw, 0))
    light = [0.35, -0.85, -0.45]
    quads = []
    for bn in bones:
        if not visible(bn): continue
        M, O = mats[bn.name], offs[bn.name]
        for b in bn.boxes:
            sheet = sheets[b.sheet]
            for face, at, tw, th, (u0, v0), n in faces_of(b):
                nw = app(M, n)
                shade = 0.62 + 0.38 * max(0.0, sum(nw[i] * light[i] for i in range(3)))
                if face == "top" and abs(nw[1]) > 0.9: shade = max(shade, 0.98)
                for ty in range(th):
                    for tx in range(tw):
                        c = sheet.getpixel((u0 + tx, v0 + ty))
                        if c[3] < 128: continue
                        pts = []
                        for s, t in ((tx / tw, ty / th), ((tx + 1) / tw, ty / th), ((tx + 1) / tw, (ty + 1) / th), (tx / tw, (ty + 1) / th)):
                            p = at(s, t)
                            q = app(cam, [O[i] + app(M, p)[i] for i in range(3)])
                            pts.append(q)
                        depth = sum(p[2] for p in pts) / 4
                        col = tuple(int(c[i] * shade) for i in range(3)) + (255,)
                        quads.append((depth, [(p[0], p[1]) for p in pts], col))
    # extra: (bone name, grip offset px, grip rotation deg, weapon quads (20 numbers), weapon sheet) drawn in the bone's frame
    for bname, goff, grot, wq, wimg in extra:
        M, O = mul(mats[bname], rot(*grot)), [offs[bname][i] + app(mats[bname], goff)[i] for i in range(3)]
        for q in wq:
            if len(q) != 20: continue
            P = [q[3 * k:3 * k + 3] for k in range(4)]; T = [q[12 + 2 * k:14 + 2 * k] for k in range(4)]
            nu = max(1, int(math.ceil(math.hypot(T[1][0] - T[0][0], T[1][1] - T[0][1]) - 1e-6)))
            nv = max(1, int(math.ceil(math.hypot(T[3][0] - T[0][0], T[3][1] - T[0][1]) - 1e-6)))
            for j in range(nv):
                for i in range(nu):
                    cs = (i + .5) / nu; ct = (j + .5) / nv
                    tu = T[0][0] + cs * (T[1][0] - T[0][0]) + ct * (T[3][0] - T[0][0]); tv = T[0][1] + cs * (T[1][1] - T[0][1]) + ct * (T[3][1] - T[0][1])
                    c = wimg.getpixel((min(wimg.width - 1, int(tu)), min(wimg.height - 1, int(tv))))
                    if c[3] < 128: continue
                    pts = []
                    for s_, t_ in ((i / nu, j / nv), ((i + 1) / nu, j / nv), ((i + 1) / nu, (j + 1) / nv), (i / nu, (j + 1) / nv)):
                        pp = [P[0][a] + s_ * (P[1][a] - P[0][a]) + t_ * (P[3][a] - P[0][a]) for a in range(3)]
                        pts.append(app(cam, [O[a] + app(M, pp)[a] for a in range(3)]))
                    col = tuple(int(c[a] * 0.85) for a in range(3)) + (255,)
                    quads.append((sum(p[2] for p in pts) / 4 - 0.01, [(p[0], p[1]) for p in pts], col))
    xs = [x for _, ps, _ in quads for x, _ in ps]; ys = [y for _, ps, _ in quads for _, y in ps]
    mx, my = min(xs) - 2, min(ys) - 2
    W, H = int((max(xs) - mx + 2) * px), int((max(ys) - my + 2) * px)
    img = Image.new("RGBA", (W, H), bg); dr = ImageDraw.Draw(img)
    for depth, ps, col in sorted(quads, key=lambda q: -q[0]):
        dr.polygon([((x - mx) * px, (y - my) * px) for x, y in ps], fill=col, outline=col)
    return img

def sheet_of(images, pad=12, bg=(40, 42, 50, 255)):
    h = max(i.height for i in images); w = sum(i.width for i in images) + pad * (len(images) + 1)
    out = Image.new("RGBA", (w, h + 2 * pad), bg); x = pad
    for i in images: out.paste(i, (x, pad + h - i.height)); x += i.width + pad
    return out
