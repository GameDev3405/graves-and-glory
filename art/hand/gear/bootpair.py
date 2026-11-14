"""The boots' UI picture (Kort approved 2026-11-13): a pair of low, Minecraft Dungeons-style ankle boots, built from the
set's own boot. Used by icon_render for the boots slot only; the worn boots do not change.

How one boot is made (no shear, every box stays a box): the set's right boot boxes are cut at a sole line 2 texels above
the main box's bottom. Below the cut, the front half is stretched forward into a fat foot; its top texel band takes a
shorter stretch, so the toe steps round. Above the cut everything is stretched upward into a short shaft, keeping the
top texel band (cuff or trim) at its own height. Every new face is resampled from the same face of the real boot, so the
colours, trims and materials are the set's own, and the open shaft top is lined dark.

How it is drawn: the Dungeons item-icon light, from a high front three-quarter (30 deg above, 35 deg off the toe): a key
from the upper left on each face plus ambient, the top brightest, a vertical darkening toward the ground, ambient
occlusion in the crease where the shaft meets the foot, a light bevel on the lit edges, the foot drawn 1.6x taller. The
second boot stands to the viewer's left and a step back, a touch darker."""
import math, os, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "common"))
import gearlib, icons, render
from handlib import Box

HI = 12   # render px per model texel (icon_render.HI)
AX = {  # face -> (s axis, s reversed, t axis, t reversed), from render.faces_of
    "front": (0, False, 1, False), "back": (0, True, 1, False), "right": (2, True, 1, False),
    "left": (2, False, 1, False), "top": (0, False, 2, True), "bottom": (0, False, 2, False)}
SHAPE = dict(foot=2.0, up=0.68, toe=1.85, toe_top=1.25)   # the low ankle boot
OVERRIDE = {"healer_boots": dict(up=0.55)}                # the healer's sandals: a lower shaft
FOOT_Y = 1.6
LIGHT = (-0.55, -0.70, -0.45)   # camera space: from upper left, toward the viewer


def _src_boxes(p, r, built):
    bones, vis, sheets = gearlib.worn([p], r, icons.ICON_SIDE, built)
    out = []
    for bn in bones:
        if bn.parent == "right_leg" and bn.name.startswith(p.item_id + ".") and vis(bn):
            for b in bn.boxes: out.append((b, sheets[b.sheet]))
    return out


def _ext(b):
    return [b.origin[i] for i in range(3)], [b.origin[i] + b.size[i] for i in range(3)]


def _face_tex(b):
    return {f: (uv, tw, th) for f, at, tw, th, uv, n in render.faces_of(b)}


class _Shape:
    """The boot's geometry maps: fy for the shaft (upper), fz for the foot (lower, and its top band 'mid')."""
    def __init__(s, boxes, foot, up, toe, toe_top):
        main = max(boxes, key=lambda bs: bs[0].size[0] * bs[0].size[1] * bs[0].size[2])[0]
        lo, hi = _ext(main)
        s.cut = hi[1] - foot
        cx, cz = (lo[0] + hi[0]) / 2, (lo[2] + hi[2]) / 2
        body = [b for b, _ in boxes if b.size[0] >= 3 and _ext(b)[0][0] <= cx <= _ext(b)[1][0] and _ext(b)[0][2] <= cz <= _ext(b)[1][2]]
        s.ytop = min(_ext(b)[0][1] for b in body)   # the boot body's top (stacked boxes over the main one); fins above ride up unstretched
        s.body = body
        s.yb = s.ytop + 1.0 if s.cut - s.ytop >= 1.75 else s.ytop   # keep the top band unless the shaft is too short to spare it
        s.zm = (lo[2] + hi[2]) / 2; s.toe = toe; s.toe_top = toe_top
        s.mid = s.cut + 1.0 if toe_top != toe else None   # the foot's top band: its own (shorter) toe, a rounded step
        zf = min(_ext(b)[0][2] for b, _ in boxes); zb = max(hi[2], s.zm)
        flen = (zb - s.zm) + (s.zm - zf) * toe                   # the foot's length after the toe stretch
        want = up * flen - foot - 1.0                            # the boot stands about `up` x the foot's length
        s.up = max(1.0, want / max(0.5, s.cut - s.yb))
        s.main = main

    def fy(s, y):
        if y >= s.cut: return y
        if y >= s.yb: return s.cut - (s.cut - y) * s.up
        return s.cut - (s.cut - s.yb) * s.up - (s.yb - y)

    def iy(s, y):
        if y >= s.cut: return y
        y_b = s.cut - (s.cut - s.yb) * s.up
        if y >= y_b: return s.cut - (s.cut - y) / s.up
        return s.yb - (y_b - y)

    def fz(s, z, reg="low"):
        k = s.toe_top if reg == "mid" else s.toe
        return z if z >= s.zm else s.zm + (z - s.zm) * k

    def iz(s, z, reg="low"):
        k = s.toe_top if reg == "mid" else s.toe
        return z if z >= s.zm else s.zm + (z - s.zm) / k


def _pieces(boxes, sh):
    """Split each real box at the cut (and the foot's top band): (old box, sheet, region, new lo, new hi, old lo, old hi, (y0, y1))."""
    out = []
    for b, sheet in boxes:
        lo, hi = _ext(b)
        parts = []
        if hi[1] <= sh.cut + 1e-6: parts.append(("up", lo[1], hi[1]))
        elif lo[1] >= sh.cut - 1e-6: parts.append(("low", lo[1], hi[1]))
        else: parts += [("up", lo[1], sh.cut), ("low", sh.cut, hi[1])]
        if sh.mid is not None:
            sp = []
            for reg, y0, y1 in parts:
                if reg != "low": sp.append((reg, y0, y1))
                elif y1 <= sh.mid + 1e-6: sp.append(("mid", y0, y1))
                elif y0 >= sh.mid - 1e-6: sp.append(("low", y0, y1))
                else: sp += [("mid", y0, sh.mid), ("low", sh.mid, y1)]
            parts = sp
        for reg, y0, y1 in parts:
            if reg == "up": nl, nh = [lo[0], sh.fy(y0), lo[2]], [hi[0], sh.fy(y1), hi[2]]
            else: nl, nh = [lo[0], y0, sh.fz(lo[2], reg)], [hi[0], y1, sh.fz(hi[2], reg)]
            out.append((b, sheet, reg, nl, nh, lo, hi, (y0, y1)))
    return out


def _sample(sheet, ft, face, reg, sh, nl, nh, lo, hi, i, j, tw, th, cutcap, caprow=None):
    sa, sr, ta, tr = AX[face]
    def coord(axis, rev, f):
        f = 1 - f if rev else f
        return nl[axis] + (nh[axis] - nl[axis]) * f
    def inv(axis, c):
        if axis == 1 and reg == "up": return sh.iy(c)
        if axis == 2 and reg in ("low", "mid"): return sh.iz(c, reg)
        return c
    def frac(axis, rev, c):
        f = (c - lo[axis]) / max(1e-6, hi[axis] - lo[axis]); f = min(0.999, max(0.0, f))
        return 1 - f if rev else f
    cs = inv(sa, coord(sa, sr, (i + .5) / tw)); ct = inv(ta, coord(ta, tr, (j + .5) / th))
    if cutcap:   # a face made by the cut (the instep): the front face's row at the cut
        uv, w2, h2 = ft["front"]
        si = int(frac(0, False, cs) * w2); ti = int(frac(1, False, (sh.cut if caprow is None else caprow) + 0.5) * h2)
        return sheet.getpixel((uv[0] + si, uv[1] + ti))
    uv, w2, h2 = ft[face]
    si = int(frac(sa, sr, cs) * w2); ti = int(frac(ta, tr, ct) * h2)
    return sheet.getpixel((uv[0] + si, uv[1] + ti))


def build_boot(p, r, built):
    """The reshaped boot: ([Box] packed into one new sheet 'nb', that sheet, the _Shape)."""
    src = _src_boxes(p, r, built)
    sh = _Shape(src, **dict(SHAPE, **OVERRIDE.get(p.item_id, {})))
    items = []
    for b, sheet, reg, nl, nh, lo, hi, (y0, y1) in _pieces(src, sh):
        nb = Box(nl, [nh[k] - nl[k] for k in range(3)], None, dil=b.dil); nb.uv = (0, 0); ft = _face_tex(b)
        tex = {}
        for face, at, tw, th, uv, n in render.faces_of(nb):
            if face not in ft: continue
            img = Image.new("RGBA", (tw, th))
            cap = (face == "top" and reg in ("low", "mid") and y0 > lo[1] + 1e-6) or (face == "bottom" and reg == "up" and y1 < hi[1] - 1e-6)
            for j in range(th):
                for i in range(tw):
                    img.putpixel((i, j), _sample(sheet, ft, face, reg, sh, nl, nh, lo, hi, i, j, tw, th, cap, y0 if face == "top" else None))
            if face == "top" and reg == "up" and any(b is c for c in sh.body) and abs(lo[1] - sh.ytop) < 0.6:   # the open shaft top: dark lining
                px = img.load()
                for jj in range(th):
                    for ii in range(tw):
                        c = px[ii, jj]
                        edge = ii == 0 or jj == 0 or ii == tw - 1 or jj == th - 1
                        if c[3] and not edge: px[ii, jj] = (int(c[0] * .3), int(c[1] * .3), int(c[2] * .3), 255)
            tex[face] = img
        items.append((nb, tex))
    W = max(2 * (int(round(b.size[0])) + int(round(b.size[2]))) for b, _ in items) + 2
    H = sum(int(round(b.size[2])) + int(round(b.size[1])) + 1 for b, _ in items) + 2
    sheet = Image.new("RGBA", (W, H), (0, 0, 0, 0)); v = 0
    for nb, tex in items:
        nb.uv = (0, v); nb.sheet = "nb"
        for face, at, tw, th, uv, n in render.faces_of(nb):
            if face in tex: sheet.paste(tex[face], uv)
        v += int(round(nb.size[2])) + int(round(nb.size[1])) + 1
    return [nb for nb, _ in items], sheet, sh


def _norm(v):
    l = math.sqrt(sum(c * c for c in v)); return [c / l for c in v]


def render_d(boxes, sheet, sh, yaw=35, pitch=30, px=HI, ao=0.32, grad=0.16, bevel=0.12, foot_y=FOOT_Y):
    """Texel-quad renderer like render.render, with the Dungeons light (see the module note). RGBA, clear background."""
    cam = render.mul(render.rot(pitch, 0, 0), render.rot(0, yaw, 0))
    L = _norm(LIGHT)
    ys = [b.origin[1] for b in boxes] + [b.origin[1] + b.size[1] for b in boxes]
    ytop, ybot = min(ys), max(ys)
    zfront_shaft = sh.main.origin[2]   # the shaft's front, where the instep crease runs
    quads = []
    for b in boxes:
        for face, at, tw, th, (u0, v0), n in render.faces_of(b):
            nc = render.app(cam, n)
            key = max(0.0, -sum(nc[i] * L[i] for i in range(3)))
            base = 0.50 + 0.55 * key
            if face == "top": base = max(base, 1.0)
            if face == "bottom": base = 0.35
            for ty in range(th):
                for tx in range(tw):
                    c = sheet.getpixel((u0 + tx, v0 + ty))
                    if c[3] < 128: continue
                    P = [at(s, t) for s, t in ((tx / tw, ty / th), ((tx + 1) / tw, ty / th), ((tx + 1) / tw, (ty + 1) / th), (tx / tw, (ty + 1) / th))]
                    mid = [sum(p[i] for p in P) / 4 for i in range(3)]
                    k = base * getattr(b, "tint", 1.0)
                    if face not in ("top", "bottom"):   # darker toward the ground
                        k *= 1 - grad * (mid[1] - ytop) / max(1e-6, ybot - ytop)
                    # crease AO: shaft fronts just above the foot, foot tops just in front of the shaft
                    if face in ("front", "left", "right") and mid[1] < sh.cut and mid[2] < zfront_shaft + 0.6:
                        d = sh.cut - mid[1]; k *= 1 - ao * max(0.0, 1 - d / 1.6) if face == "front" else 1
                    if face == "top" and abs(mid[1] - sh.cut) < 0.05:
                        d = zfront_shaft - mid[2]; k *= 1 - ao * max(0.0, 1 - d / 1.6)
                    edge = (face == "top" and (ty == th - 1 or tx == 0)) or (face != "top" and face != "bottom" and ty == 0)
                    if edge: k *= 1 + bevel   # bevel: the first texel row/column on edges facing the key
                    q = [render.app(cam, [p[0], p[1] if p[1] <= sh.cut else sh.cut + (p[1] - sh.cut) * foot_y, p[2]]) for p in P]
                    col = tuple(min(255, int(c[i] * k)) for i in range(3)) + (255,)
                    quads.append((sum(p[2] for p in q) / 4, [(p[0], p[1]) for p in q], col))
    xs = [x for _, ps, _ in quads for x, _ in ps]; ys2 = [y for _, ps, _ in quads for _, y in ps]
    mx, my = min(xs) - 1, min(ys2) - 1
    W, H = int((max(xs) - mx + 1) * px), int((max(ys2) - my + 1) * px)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0)); dr = ImageDraw.Draw(img)
    for depth, ps, col in sorted(quads, key=lambda q: -q[0]):
        dr.polygon([((x - mx) * px, (y - my) * px) for x, y in ps], fill=col, outline=col)
    return img


def boot_pair(p, r, built, yaw=35, pitch=30, gap=1.0, back=2.2, far_tint=0.86):
    """The set's boots at rarity r as the approved pair, RGBA on clear: the near boot, the far one to its left and back."""
    boxes, sheet, sh = build_boot(p, r, built)
    xs = [b.origin[0] for b in boxes] + [b.origin[0] + b.size[0] for b in boxes]
    w = max(xs) - min(xs)
    far = []
    for b in boxes:
        c = Box([b.origin[0] - w - gap, b.origin[1], b.origin[2] + back], b.size, None, dil=b.dil); c.uv = b.uv; c.tint = far_tint
        far.append(c)
    return render_d(far + boxes, sheet, sh, yaw, pitch)
