"""The table things around the book: the back plaque and key caps sized for 13 px type, the candle with its flame
bent by the wind and its light on the oak, dust motes the wind carries, the ribbon marker.

python sb_props.py [name ...]   writes art/<name>.png"""
import math, random, sys
from sb_common import *
from v4a_furn import candle, plaque, key_engraved, brass_button, vellum_button, screw, engrave, tarnish, brushed
from v4a_paper import ribbon

ASSETS = {}

# ---------------------------------------------------------------- plaques and keys for 13 px type
for _w in (144, 152, 168):
    ASSETS['plaque-%dx32' % _w] = (lambda w: (lambda: plaque(w, 32, seed=3)))(_w)
for _w in range(24, 56):
    ASSETS['key-%dx20' % _w] = (lambda w: (lambda: key_engraved(w, 20)))(_w)


def key_vellum(w, h=20):
    """a key cap ruled in ink on vellum: a sepia outline with rounded corners, the inner edge a shade lighter
    below and right, the face left clear"""
    s = Spr(w, h)
    for x in range(1, w - 1):
        s.set(x, 0, INK[3]); s.set(x, h - 1, INK[3])
    for y in range(1, h - 1):
        s.set(0, y, INK[3]); s.set(w - 1, y, INK[3])
    for x in range(1, w - 1):
        s.set(x, h - 2, VEL[4])
    for y in range(1, h - 1):
        s.set(w - 2, y, VEL[4])
    return s


for _w in range(24, 56):
    ASSETS['keyv-%dx20' % _w] = (lambda w: (lambda: key_vellum(w, 20)))(_w)


# ---------------------------------------------------------------- the candle in the wind
FLAME = ['#4a6fb5', '#e0802c', '#f2a93f', '#ffd27a', '#fff4d6']


def candle_wind():
    """the War Table candle with its flame laid over to the left by the wind coming off the book; a thread of
    smoke torn off the tip and carried left"""
    base = candle()
    W, H = base.w + 16, base.h + 10
    s = Spr(W, H)
    s.blit(base, 16, 10)
    cx, top = 19 + 16, 12 + 10
    # clear the upright flame
    for y in range(top - 12, top - 2):
        for x in range(cx - 3, cx + 4):
            s.set(x, y, None)
    s.set(cx, top - 2, INK[0])
    FL = ['d.........',
          '.dd.......',
          '..ddcd....',
          '...dccbd..',
          '....dcbbd.',
          '....dcbabd',
          '.....dbabd',
          '......dbd.',
          '........e.']
    s.grid(cx - 8, top - 11, FL, {'a': FLAME[4], 'b': FLAME[3], 'c': FLAME[2], 'd': FLAME[1], 'e': FLAME[0]})
    # smoke: a thin thread from the tip, drifting left and climbing, thinning out (alpha steps, no blur)
    pts = []
    x, y = cx - 9, top - 12
    for k in range(26):
        x -= 1
        if k % 3 == 0:
            y -= 1
        y2 = y + round(math.sin(k * 0.55) * 1.2)
        pts.append((x, y2, max(20, 120 - k * 4)))
    for (x, y, a) in pts:
        s.over(x, y, '#c9c2b4', a)
    return s


ASSETS['candle-wind'] = candle_wind


def glow(rx, ry, peak=0.30):
    """candle light on the oak: a warm translucent pool in stepped rings, dithered where it fades"""
    s = Spr(2 * rx + 1, 2 * ry + 1)
    for y in range(2 * ry + 1):
        for x in range(2 * rx + 1):
            d = math.sqrt(((x - rx) / rx) ** 2 + ((y - ry) / ry) ** 2)
            if d > 1.0:
                continue
            a = (1.0 - d) ** 1.5 * peak
            step = round(a / 0.035) * 0.035
            if step <= 0.0 and dith(x, y, a / 0.035):
                step = 0.035
            if step > 0:
                s.set(x, y, rgba('#ffb35c', round(step * 255)))
    return s


ASSETS['glow-big'] = lambda: glow(150, 120, 0.28)


# ---------------------------------------------------------------- dust the wind carries
def motes(spots, seed=3, W=960, H=540):
    """specks of dust and ash carried right to left: a bright head and a short fading tail behind it"""
    rnd = random.Random(seed)
    s = Spr(W, H)
    for (x, y, ln) in spots:
        dy = rnd.choice([0, 0, 1, -1])
        for i in range(ln):
            a = max(0, 210 - i * (190 // max(1, ln)))
            yy = y + (dy if i > ln // 2 else 0)
            s.over(x + i, yy, '#f1e7cb' if i == 0 else '#e8dab4', a)
        if ln > 3:
            s.over(x, y - 1, '#f1e7cb', 70)
    return s


ASSETS['motes-01'] = lambda: motes([(660, 214, 6), (702, 288, 4), (744, 160, 7), (652, 372, 5), (688, 430, 3),
                                    (772, 330, 5), (618, 136, 4), (812, 236, 3), (726, 398, 6), (598, 468, 4),
                                    (836, 404, 4), (636, 300, 3)])


# ---------------------------------------------------------------- the ribbon marker
def marker(path, w=6, seed=2, lift=()):
    """a silk ribbon marker laid along a path of points: a six px band, the lit edge on the upper left, the
    weave shown as a fine diagonal, a swallowtail cut at the end; lift is the set of path indices raised off
    the surface (their shadow falls further)"""
    rnd = random.Random(seed)
    R = ['#2e0b08', '#4a120c', '#5e1810', '#76231a', '#8f3224', '#a8453a']
    xs = [p[0] for p in path]; ys = [p[1] for p in path]
    ox, oy = min(xs) - w - 4, min(ys) - w - 4
    S = Spr(max(xs) - ox + w + 8, max(ys) - oy + w + 8)
    body = {}
    n = len(path)
    for i in range(n - 1):
        (x0, y0), (x1, y1) = path[i], path[i + 1]
        L = max(1, round(math.hypot(x1 - x0, y1 - y0)))
        for k in range(L):
            t = k / L
            x = x0 + (x1 - x0) * t; y = y0 + (y1 - y0) * t
            dx, dy = (x1 - x0) / L, (y1 - y0) / L
            nx, ny = -dy, dx
            for j in range(w):
                o = j - (w - 1) / 2.0
                X, Y = round(x + nx * o - ox), round(y + ny * o - oy)
                body[(X, Y)] = (j, i, k)
    # swallowtail: cut a notch into the last few px
    ex, ey = path[-1]; px, py = path[-2]
    L = math.hypot(ex - px, ey - py); dx, dy = (ex - px) / L, (ey - py) / L
    for (X, Y) in list(body):
        wx, wy = X + ox - ex, Y + oy - ey
        along = wx * dx + wy * dy
        across = abs(-wx * dy + wy * dx)
        if along > -4 + across * 1.2 and along > -5:
            del body[(X, Y)]
    for (X, Y), (j, i, k) in body.items():
        for (sx, sy, a) in (((2, 3, 110) if i not in lift else (5, 7, 80)),):
            q = (X + sx, Y + sy)
            if q not in body:
                S.over(q[0], q[1], SH, a)
    for (X, Y), (j, i, k) in body.items():
        c = R[3]
        if j == 0:
            c = R[5]
        elif j == 1:
            c = R[4]
        elif j == w - 1:
            c = R[1]
        elif j == w - 2:
            c = R[2]
        if (X + Y) % 4 == 0 and 0 < j < w - 1:
            c = R[2] if j > 2 else R[3]
        S.set(X, Y, c)
    return S, (ox, oy)


def save_marker(name, path, **kw):
    S, (ox, oy) = marker(path, **kw)
    save(S, name)
    print('    origin', ox, oy)




# ---------------------------------------------------------------- a ribbon marker in the wind
RIB = ['#2e0b08', '#4a120c', '#5e1810', '#76231a', '#8f3224', '#a8453a', '#c46552']


def ribbon_wind(path, widths, air, shadow_off=(3, 8)):
    """a silk ribbon marker along a centre line (a list of points), its width given per point (a pinch where it
    twists), and air[i] (0..1) how high it flies there (its shadow falls further). Each point of the band is
    lit by which side of the centre line it is on: the upper edge catches the light, the lower edge turns
    away; past a pinch the ribbon shows its back, a step darker."""
    pts = []
    for i in range(len(path) - 1):
        (x0, y0), (x1, y1) = path[i], path[i + 1]
        L = max(1, int(math.hypot(x1 - x0, y1 - y0) * 3))
        for k in range(L):
            t = k / L
            pts.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, widths[i] + (widths[i + 1] - widths[i]) * t,
                        air[i] + (air[i + 1] - air[i]) * t, i, (x1 - x0, y1 - y0)))
    xs = [p[0] for p in path]; ys = [p[1] for p in path]
    ox, oy = int(min(xs)) - 10, int(min(ys)) - 10
    S = Spr(int(max(xs)) - ox + 20, int(max(ys)) - oy + 24)
    band = {}
    flip = False
    lastw = None
    for (x, y, w, a, i, (dx, dy)) in pts:
        L = math.hypot(dx, dy) or 1
        nx, ny = -dy / L, dx / L
        if ny > 0:
            nx, ny = -nx, -ny                  # n points up
        if lastw is not None and w < 2.2 <= lastw:
            flip = not flip
        lastw = w
        half = w / 2.0
        steps = max(2, int(w * 3))
        for s in range(steps + 1):
            o = -half + w * s / steps
            X, Y = round(x + nx * o - ox), round(y + ny * o - oy)
            side = o / max(0.5, half)          # +1 is the upper edge
            band[(X, Y)] = (side, a, flip)
    for (X, Y), (side, a, flip) in band.items():
        sx = round(shadow_off[0] * a) + 1; sy = round(shadow_off[1] * a) + 1
        q = (X + sx, Y + sy)
        if q not in band:
            S.over(q[0], q[1], SH, 120 if a < 0.3 else 85)
    for (X, Y), (side, a, flip) in band.items():
        if side > 0.6:
            c = RIB[6] if not flip else RIB[4]
        elif side > 0.1:
            c = RIB[5] if not flip else RIB[3]
        elif side > -0.5:
            c = RIB[4] if not flip else RIB[2]
        else:
            c = RIB[2] if not flip else RIB[1]
        S.set(X, Y, c)
    # outline the band where it meets the table or the air (one dark pixel under the lower edge)
    for (X, Y), (side, a, flip) in list(band.items()):
        if (X, Y + 1) not in band and side < -0.3:
            S.set(X, Y + 1, RIB[0])
    return S, (ox, oy)


# ribbon-01: the marker from the book's tail, blown left and up off the table; its origin is (min x - 10, min y - 10)
RIB01 = dict(path=[(597, 474), (597, 489), (592, 495), (580, 497), (566, 494), (552, 489), (538, 487), (523, 489),
                   (509, 486), (496, 480), (484, 478)],
             widths=[6, 6, 6, 6, 5.5, 4.5, 2.0, 4.5, 5.5, 6, 6],
             air=[0, 0, 0.05, 0.25, 0.45, 0.65, 0.8, 0.9, 1.0, 1.0, 1.0])
ASSETS['ribbon-01'] = lambda: ribbon_wind(RIB01['path'], RIB01['widths'], RIB01['air'], shadow_off=(4, 12))[0]


# ribbon-02: the marker lying in the fold of the open spread, head to tail, then out over the tail of the
# spine and onto the oak in a lazy S. Page coordinates; the sprite's origin is RIB02_AT
RIB02 = dict(gutter=(421, 80, 500),
             path=[(422, 498), (422, 507), (423, 512), (427, 518), (436, 522), (448, 523), (459, 521), (468, 519),
                   (477, 521)],
             widths=[3, 4.5, 6, 6, 6, 6, 6, 6, 6], air=[0.2, 0.15, 0.05, 0, 0, 0, 0, 0, 0])
RIB02_AT = (411, 70)


def ribbon_02():
    tail, (ox, oy) = ribbon_wind(RIB02['path'], RIB02['widths'], RIB02['air'], shadow_off=(3, 5))
    ax, ay = RIB02_AT
    W = max(tail.w + ox - ax, 40)
    H = tail.h + oy - ay
    S = Spr(W, H)
    gx, gy0, gy1 = RIB02['gutter']
    # in the fold: two px of silk sunk in the fold's shadow, dark wine for most of its length and only coming up
    # into the light over the last stretch, where it rises out of the fold toward the tail
    for y in range(gy0, gy1):
        t = (y - gy0) / float(gy1 - gy0)
        if t < 0.82:
            pair = (RIB[1], RIB[0]) if (y // 2) % 9 else (RIB[2], RIB[1])
        elif t < 0.92:
            pair = (RIB[2], RIB[1])
        else:
            pair = (RIB[3], RIB[2])
        for i, c in enumerate(pair):
            S.set(gx + i - ax, y - ay, c)
    tp = tail.im.load()
    for y in range(tail.h):
        for x in range(tail.w):
            if tp[x, y][3]:
                S.over(x + ox - ax, y + oy - ay, tp[x, y][:3], tp[x, y][3])
    # the swallowtail cut at the end, and its shadow with it
    (px, py), (ex, ey) = RIB02['path'][-2], RIB02['path'][-1]
    L = math.hypot(ex - px, ey - py); dx, dy = (ex - px) / L, (ey - py) / L
    for Y in range(S.h):
        for X in range(S.w):
            wx, wy = X + ax - ex, Y + ay - ey
            along = wx * dx + wy * dy
            across = abs(-wx * dy + wy * dx)
            if along > -3.5 + across * 1.1 and along > -5 and across < 6:
                S.set(X, Y, (0, 0, 0, 0))
    # crop to the silk and its shadow, and say where the sprite goes on the page
    bb = S.im.getbbox()
    out = Spr(bb[2] - bb[0], bb[3] - bb[1])
    out.im.paste(S.im.crop(bb), (0, 0)); out.px = out.im.load()
    print('    ribbon-02 at', ax + bb[0], ay + bb[1], 'size', out.w, out.h)
    return out


ASSETS['ribbon-02'] = ribbon_02


# ---------------------------------------------------------------- leaves the wind brings in
LEAF_PAL = {'a': '#4e3118', 'b': '#8e6030', 'B': '#b07a3e', 'h': '#c89452', 'c': '#5e3c1e', 'd': '#3e2612',
            'u': '#9a7a4e', 'U': '#7e6240'}


OAK_LOBES = dict(upper=[(12, 5.0, 3.4), (21, 6.6, 3.9), (29.5, 6.0, 3.7), (36.5, 4.6, 3.0), (41, 3.2, 2.2)],
                 lower=[(15, 5.2, 3.4), (24.5, 6.4, 3.8), (33, 5.4, 3.4), (39, 3.8, 2.6), (43, 2.6, 1.9)])


def leaf_shape(squash=1.0):
    """an English oak leaf along a midrib from the tip (x 0) to the stem (x 44): a rounded tip lobe and five
    lobes a side, each a long oval swept forward toward the tip from where it leaves the rib (the lower side a
    half lobe behind the upper), on a narrow body, so the sinuses cut back toward the rib. Returns pixels ->
    (u, v, lobe), u tip to base, v signed across (upper negative)"""
    px = {}
    lobes = []
    for side, key in ((-1, 'upper'), (1, 'lower')):
        for k, (bx, la, lb) in enumerate(OAK_LOBES[key]):
            ax, ay = -0.42, 0.91 * side
            n = math.hypot(ax, ay); ax, ay = ax / n, ay / n
            cx, cy = bx + ax * (la - 0.6), side * 1.4 + ay * (la - 0.6)
            lobes.append((cx, cy, ax, ay, la, lb, len(lobes)))
    for x in range(0, 47):
        for y in range(-14, 15):
            yy = y / max(0.2, squash)
            hit = -1
            if ((x - 5.0) / 5.2) ** 2 + (yy / 4.3) ** 2 <= 1:
                hit = 99
            for (cx, cy, ax, ay, la, lb, k) in lobes:
                dx, dy = x - cx, yy - cy
                u = dx * ax + dy * ay
                v = -dx * ay + dy * ax
                if (u / la) ** 2 + (v / lb) ** 2 <= 1:
                    hit = k
                    break
            body = 3 <= x <= 44 and abs(yy) <= 2.7 - 1.3 * max(0, x - 32) / 12.0
            if hit != -1 or body:
                px[(x, y)] = (x / 44.0, max(-1.0, min(1.0, yy / 8.0)), hit)
    return px


def paint_leaf(S, ox, oy, px, flip=False, under=False, rnd=None, shadow=None):
    """paint a leaf's pixels at (ox, oy) (the midrib on row oy): the upper half lit, the lower half in shade
    (or the paler underside when it turns over), side veins every lobe running out toward the tip, the midrib
    and the stem, a dark rim, a nibbled hole"""
    rnd = rnd or random.Random(5)
    P = LEAF_PAL
    keys = set(px)
    if shadow:
        sx, sy, a = shadow
        for (i, j) in keys:
            S.over(ox + i + sx, oy + j + sy, SH, a)
    for (i, j), (u, v, *_r) in px.items():
        if under and v > 0:
            c = P['u'] if v < 0.7 else P['U']
        elif v < -0.55:
            c = P['h'] if (i + j) % 5 else P['B']
        elif v < 0:
            c = P['B']
        else:
            c = P['b']
        S.set(ox + i, oy + j, c)
    L = max(i for (i, j) in keys)
    # side veins: from the rib out along each lobe's axis
    sq = max(abs(j) for (i, j) in keys) / 13.0 if keys else 1.0
    for side, key in ((-1, 'upper'), (1, 'lower')):
        for (bx, la, lb) in OAK_LOBES[key]:
            ax, ay = -0.42, 0.91 * side
            n_ = math.hypot(ax, ay); ax, ay = ax / n_, ay / n_
            for t in range(1, int(la * 1.4)):
                q = (int(round(bx + ax * t)), int(round((side * 1.0 + ay * t) * sq)))
                if q in keys and q[1] != 0:
                    S.set(ox + q[0], oy + q[1], P['c'] if not (under and side > 0) else P['U'])
    for i in range(L + 1):
        if (i, 0) in keys:
            S.set(ox + i, oy, P['c'])
    for k in range(1, 6):
        S.set(ox + L + k, oy + (k // 3), P['d'])
    # the rim
    for (i, j) in keys:
        for (di, dj) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (i + di, j + dj) not in keys:
                S.set(ox + i + di, oy + j + dj, P['a'])
    # a nibbled hole
    hi, hj = int(L * 0.55), -4
    for (di, dj) in ((0, 0), (1, 0), (0, 1)):
        if (hi + di, hj + dj) in keys:
            S.set(ox + hi + di, oy + hj + dj, None)
            if shadow:
                S.over(ox + hi + di, oy + hj + dj, SH, 60)


def dry_leaf(S, ox, oy, L=40, W=9.0, squash=1.0, turned=False, shadow=None, seed=4):
    """a dry leaf, beech-like: an almond blade with a fine toothed edge, a midrib and straight side veins
    running forward to the teeth, a short stalk; the upper half lit, the lower in shade; one edge near the tip
    curled up so its paler underside shows; turned (in the air): the whole lower half shows its underside"""
    rnd = random.Random(seed)
    P = LEAF_PAL
    px = {}
    for i in range(L + 1):
        u = i / float(L)
        hw = W * math.sin(math.pi * min(1.0, u ** 0.75)) ** 0.85
        if i % 4 == 0 and 0.1 < u < 0.92:
            hw -= 0.8
        for j in range(-int(round(hw * squash)), int(round(hw * squash)) + 1):
            px[(i, j)] = j / max(1.0, hw * squash)
    if shadow:
        sx, sy, a = shadow
        for (i, j) in px:
            S.over(ox + i + sx, oy + j + sy, SH, a)
    for (i, j), v in px.items():
        u = i / float(L)
        curl = v < -0.55 and u < 0.42
        if curl or (turned and v > 0):
            c = P['u'] if v > -0.85 else P['U']
        elif v < -0.4:
            c = P['h']
        elif v < 0:
            c = P['B']
        else:
            c = P['b'] if v < 0.75 else P['c']
        S.set(ox + i, oy + j, c)
    # side veins: straight, forward to the edge, every few px
    for i0 in range(6, L - 3, 5):
        for side in (-1, 1):
            for t in range(1, 12):
                q = (i0 - int(t * 0.8), side * t)
                if q in px and abs(px[q]) < 0.9:
                    S.set(ox + q[0], oy + q[1], P['c'] if not (turned and side > 0) else P['U'])
    for i in range(1, L):
        S.set(ox + i, oy, P['c'])
    for k in range(1, 6):
        S.set(ox + L + k, oy + (1 if k > 3 else 0), P['d'])
    for (i, j) in px:                               # the rim, and the curl's fold line
        for (di, dj) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (i + di, j + dj) not in px:
                S.set(ox + i + di, oy + j + dj, P['a'])
        if px[(i, j)] >= -0.55 and px.get((i, j - 1), 0) < -0.55 and i / float(L) < 0.42:
            S.set(ox + i, oy + j - 1, P['a'])


def leaf_flat():
    """a dry leaf lying where the wind dropped it, its tip pointing the way the wind goes"""
    s = Spr(54, 28)
    dry_leaf(s, 3, 12, shadow=(2, 3, 90))
    return s


def leaf_tumbling():
    """a second leaf in the air, turning over: foreshortened, its pale underside showing, its shadow on the oak
    well below it (it flies)"""
    s = Spr(70, 46)
    tmp = Spr(70, 46)
    dry_leaf(tmp, 3, 8, L=34, W=8.0, squash=0.55, turned=True, seed=9)
    for y in range(tmp.h):
        for x in range(tmp.w):
            if tmp.get(x, y)[3] and 0 <= x + 18 < 70 and 0 <= y + 24 < 46:
                s.over(x + 18, y + 24, SH, 58)
    s.blit(tmp, 0, 0)
    return s


ASSETS['leaf-flat'] = leaf_flat
ASSETS['leaf-tumbling'] = leaf_tumbling
ASSETS['motes-01'] = lambda: motes([(660, 214, 9), (702, 288, 6), (744, 160, 12), (652, 372, 8), (688, 430, 5),
                                    (772, 330, 9), (618, 136, 6), (812, 236, 5), (726, 398, 10), (598, 468, 6),
                                    (836, 404, 7), (636, 300, 5), (868, 180, 14), (890, 300, 11), (900, 446, 8),
                                    (790, 120, 7), (846, 270, 6), (760, 470, 9), (610, 420, 7), (575, 445, 5),
                                    (930, 380, 10), (700, 140, 6)])


if __name__ == '__main__':
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        save(ASSETS[n](), n)
