"""v4 paper goods for the War Table: parchment sheets (deckled, aged at the rim, folded), ink drawings with
hatching, wax seals (raised rim, pressed field, embossed device, drips), and the inked Rivergate map."""
import math, random
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v4pal import *

ASSETS = {}

SHADOW = '#0a0505'


# ================================================================ parchment
# Paper tone families, six to eight steps each: the torn edge, two aged rims, the shaded mottle, the body,
# a clean light, a highlight for the lit arris; dark and light fibres; two foxing browns; fold crease and ridge.
PAPER = {
    'lo': dict(edge='#6f5a38', rim2='#a08a5d', rim1='#bda97c', shade='#d0bd90', body='#dccb9f', light='#e5d6ad',
               hi='#efe4c4', fibre='#c2ae80', fibre_l='#e9dcb6', fox1='#c7a674', fox2='#a9824f',
               crease='#bfac7f', ridge='#ece0bd', back='#e9dcb7'),
    'hi': dict(edge='#86704a', rim2='#bba67a', rim1='#d6c69c', shade='#e8dbbb', body='#f1e7cb', light='#f6eedb',
               hi='#fcf8ec', fibre='#ddcfab', fibre_l='#fbf4e4', fox1='#e0c79a', fox2='#c7a573',
               crease='#d9caa5', ridge='#fdf8ec', back='#e5d7b1'),
    'map': dict(edge='#7a6440', rim2='#bba67a', rim1='#d6c69c', shade='#e4d6b3', body='#ece0c0', light='#f2e9cf',
                hi='#f8f2e0', fibre='#d9cba6', fibre_l='#f6efdc', fox1='#dcc293', fox2='#c3a06c',
                crease='#d6c7a2', ridge='#f8f2e2', back='#e5d7b1'),
}


def parchment(w, h, kind='lo', seed=1, shadow=(2, 3), folds=(), dogear=0, deckle=True, rim=3, stains=1, fibres=1.0,
              mottle='spots', foxing=1.0, tears=True):
    """a sheet of parchment w x h plus its shadow; returns (sprite, set of paper pixels).
    Torn-edge pass: a deckled outline with V-shaped nicks and loose fibres standing off the edge. Ageing creeps
    in from the rim in broad lobes (two rim tones, then the shaded mottle), the middle stays clean with a few
    lighter patches; the top and left arris catch the light. Fibres in two tones run with the sheet; foxing is
    clusters of brown spots with a pale halo; folds are a crease with a lit ridge on the side toward the light
    and a shaded facet on the other."""
    T = PAPER[kind]
    for k, v in (('hi', T['light']), ('fibre_l', T['light']), ('fox1', T['rim1']), ('fox2', T['rim2'])):
        T.setdefault(k, v)
    rnd = random.Random(seed * 7919 + w * 31 + h)
    dx, dy = shadow
    S = Spr(w + dx + 2, h + dy + 2)

    def runs(n, p1=0.36, p2=0.05):
        out = []
        while len(out) < n:
            r = rnd.random()
            v = 2 if r < p2 else (1 if r < p1 else 0)
            ln = rnd.randint(1, 2) if v == 2 else rnd.randint(3, 12)
            out += [v] * ln
        return out[:n]
    if deckle:
        top, bot, lef, rig = runs(w), runs(w), runs(h), runs(h)
        if tears:
            # V-shaped nicks torn into the edge, two to four per side
            for arr, n in ((top, w), (bot, w), (lef, h), (rig, h)):
                for _ in range(rnd.randint(2, 4)):
                    c = rnd.randint(10, n - 11); dep = rnd.randint(3, 5)
                    for k in range(-dep, dep + 1):
                        arr[c + k] = max(arr[c + k], dep - abs(k))
    else:
        top, bot, lef, rig = [0] * w, [0] * w, [0] * h, [0] * h

    def inside(x, y):
        if not (0 <= x < w and 0 <= y < h):
            return False
        if y < top[x] or y > h - 1 - bot[x] or x < lef[y] or x > w - 1 - rig[y]:
            return False
        if x + y < 2 or (w - 1 - x) + y < 2 or x + (h - 1 - y) < 2 or (w - 1 - x) + (h - 1 - y) < 2:
            return False
        if dogear and (x - (w - dogear)) > y:
            return False
        return True

    def dist(x, y):
        return min(x - lef[y], w - 1 - rig[y] - x, y - top[x], h - 1 - bot[x] - y)
    paper = {(x, y) for y in range(h) for x in range(w) if inside(x, y)}
    for (x, y) in paper:
        for (ox, oy, a) in ((dx, dy, 0.42), (dx + 1, dy + 1, 0.18)):
            k = (x + ox, y + oy)
            if k not in paper and S.get(*k)[3] < a * 255:
                S.set(k[0], k[1], rgba(SHADOW, round(a * 255)))
    big = Noise(seed + 1, 30); mid = Noise(seed + 2, 7); fine = Noise(seed + 3, 3); clean = Noise(seed + 4, 22)
    for (x, y) in paper:
        d = dist(x, y)
        if d <= 0:
            c = T['edge']
        elif d == 1:
            c = T['rim2'] if fine(x, y) > 0.3 else T['rim1']
            if (x - lef[y] == 1 or y - top[x] == 1) and fine(x, y) > 0.55:
                c = T['hi']                      # the top and left arris catch the light
        elif d <= rim:
            c = T['rim1'] if mid(x, y) > 0.38 + 0.12 * (d - 2) else T['shade']
        elif d <= rim + 3 and mid(x, y) > 0.74:
            c = T['rim1']
        elif mottle == 'edge':
            e = max(0.0, 1.0 - (d - rim) / 22.0)
            a = e * 0.8 + big(x, y) * 0.42
            c = T['shade'] if a > 0.66 else T['body']
            if a > 0.9 and mid(x, y) > 0.6:
                c = T['rim1']
            elif a < 0.22 and clean(x, y) > 0.74:
                c = T['light']
        else:
            v = big(x, y)
            c = T['shade'] if v > 0.64 else (T['light'] if v < 0.24 else T['body'])
        S.set(x, y, c)
    # loose fibres standing off the torn edge
    if deckle and tears:
        for _ in range(int((w + h) / 9)):
            side = rnd.randint(0, 3)
            if side == 0:
                x = rnd.randint(3, w - 4); y = top[x] - 1; dd = (0, -1)
            elif side == 1:
                x = rnd.randint(3, w - 4); y = h - bot[x]; dd = (0, 1)
            elif side == 2:
                y = rnd.randint(3, h - 4); x = lef[y] - 1; dd = (-1, 0)
            else:
                y = rnd.randint(3, h - 4); x = w - rig[y]; dd = (1, 0)
            if (x - dd[0], y - dd[1]) in paper and 0 <= x < w and 0 <= y < h:
                S.set(x, y, T['rim2'])
    # faint stains: a broken ring where a cup or an inkwell stood
    for _ in range(stains):
        cx, cy = rnd.randint(16, w - 17), rnd.randint(16, h - 17)
        r = rnd.uniform(4.5, 7.5); gap = rnd.uniform(0, 6.28)
        for k in range(int(r * 7)):
            a = k / (r * 7) * 6.283
            if abs(((a - gap + 3.14) % 6.283) - 3.14) < 0.7:
                continue
            X, Y = round(cx + r * math.cos(a)), round(cy + r * math.sin(a) * 0.9)
            if (X, Y) in paper and dist(X, Y) > rim:
                S.set(X, Y, T['shade'] if k % 5 else T['fibre'])
    # fibres in two tones, running mostly with the sheet
    for _ in range(int(w * h / 150 * fibres)):
        x0, y0 = rnd.randint(4, w - 6), rnd.randint(4, h - 6)
        ang = rnd.gauss(0, 0.35) + (math.pi / 2 if rnd.random() < 0.2 else 0)
        ln = rnd.randint(2, 6)
        c = T['fibre'] if rnd.random() < 0.6 else T['fibre_l']
        for i in range(ln):
            X, Y = round(x0 + i * math.cos(ang)), round(y0 + i * math.sin(ang))
            if (X, Y) in paper and dist(X, Y) > 1:
                S.set(X, Y, c)
    # foxing: clusters of brown spots, each with a pale halo on its upper left
    for _ in range(int(max(0, foxing) * (1 + w * h / 9000))):
        if rnd.random() < 0.65:
            # most foxing gathers toward the rim
            side = rnd.randint(0, 3)
            fx = rnd.randint(6, w - 7) if side < 2 else (rnd.randint(4, 18) if side == 2 else rnd.randint(w - 19, w - 5))
            fy = rnd.randint(6, h - 7) if side >= 2 else (rnd.randint(4, 18) if side == 0 else rnd.randint(h - 19, h - 5))
        else:
            fx, fy = rnd.randint(8, w - 9), rnd.randint(8, h - 9)
        for _k in range(rnd.randint(2, 6)):
            X = fx + round(rnd.gauss(0, 2.2)); Y = fy + round(rnd.gauss(0, 1.6))
            if (X, Y) in paper and dist(X, Y) > 1:
                S.set(X, Y, T['fox2'] if rnd.random() < 0.35 else T['fox1'])
                if (X - 1, Y - 1) in paper and rnd.random() < 0.5:
                    S.set(X - 1, Y - 1, T['light'])
                if rnd.random() < 0.3 and (X + 1, Y) in paper:
                    S.set(X + 1, Y, T['fox1'])
    for fy in folds:
        gap = rnd.randint(22, 40)
        for x in range(1, w - 1):
            if (x + seed) % gap == 0:
                continue
            if (x, fy) in paper and dist(x, fy) > 0:
                S.set(x, fy, T['crease'])
            if (x, fy + 1) in paper and dist(x, fy + 1) > 0:
                S.set(x, fy + 1, T['ridge'])
            # the facet below the fold turns slightly away from the light
            for o in (2, 3):
                if (x, fy + o) in paper and dist(x, fy + o) > 1 and (x * 5 + o * 3 + seed) % 4 != 0:
                    if S.get(x, fy + o)[:3] == rgba(T['body'])[:3] or S.get(x, fy + o)[:3] == rgba(T['light'])[:3]:
                        S.set(x, fy + o, T['shade'])
            if (x, fy - 1) in paper and dist(x, fy - 1) > 1 and (x + seed) % 3 == 0:
                S.set(x, fy - 1, T['light'])
        # the crease nicks both edges of the sheet
        for X in (lef[fy], w - 1 - rig[fy]):
            S.set(X, fy, T['edge'])
    if dogear:
        x0 = w - dogear
        for y in range(dogear):
            for x in range(x0, w):
                u = x - x0
                if u > y:
                    continue
                if u == y:
                    S.set(x, y, T['crease'])          # the hinge
                elif u == 0 or y == dogear - 1:
                    S.set(x, y, T['edge'])
                else:
                    S.set(x, y, T['back'] if (x + y) % 5 else T['shade'])
        for y in range(1, dogear + 1):
            S.set(x0 - 1, y, T['rim2'])
        for x in range(x0, w - 1):
            S.set(x, dogear, T['rim2'])
    return S, paper


def _card(w, h, kind, seed, shadow, **kw):
    s, _ = parchment(w, h, kind=kind, seed=seed, shadow=shadow, **kw)
    return s


ASSETS['order-lo'] = lambda: _card(144, 136, 'lo', 3, (2, 3), folds=(58,), mottle='edge')
ASSETS['order-lo2'] = lambda: _card(144, 136, 'lo', 8, (2, 3), folds=(61,), mottle='edge')
ASSETS['order-hi'] = lambda: _card(176, 160, 'hi', 5, (4, 6), folds=(52, 106), dogear=9, stains=0, mottle='edge')
ASSETS['field-card'] = lambda: _card(168, 188, 'lo', 9, (2, 3), stains=1, mottle='edge')


# ================================================================ ink drawing helpers
class Pen:
    """ink on a transparent layer, with a knock-out halo so front strokes cut through what is behind"""
    def __init__(self, w, h):
        self.s = Spr(w, h)

    def put(self, x, y, c):
        self.s.set(x, y, c)

    def layer_over(self, other, halo=True, solid=()):
        """draw `other` (a Pen) on top of this one, clearing a 1 px ring around its strokes first;
        `solid` is a set of pixels the front object covers completely (its body hides what is behind)"""
        for (x, y) in solid:
            self.s.set(x, y, None)
        m = other.s.mask()
        if halo:
            for (x, y) in m:
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    k = (x + ox, y + oy)
                    if k not in m:
                        self.s.set(k[0], k[1], None)
        self.s.blit(other.s, 0, 0)


def disc_mask(cx, cy, r):
    return {(x, y) for y in range(math.floor(cy - r) - 1, math.ceil(cy + r) + 2) for x in range(math.floor(cx - r) - 1, math.ceil(cx + r) + 2)
            if (x - cx) ** 2 + (y - cy) ** 2 <= (r + 0.5) ** 2}


def sword(pen, x0, y0, tg, tt, ink, wash, flip=False, W=64):
    """a longsword on the 45 degree diagonal, pommel at (x0, y0) heading up-right; tg = guard step, tt = tip step.
    Drawn as an engraving: a 7 px blade with a fuller, its shadow edge hatched, a solid flared guard,
    a wrapped grip and a round pommel."""
    def P(x, y, c):
        pen.put((W - 1 - x) if flip else x, y, c)
    for t in range(tg + 1, tt + 1):
        cx, cy = x0 + t, y0 - t
        half = min(3, tt - t)
        if half == 0:
            P(cx, cy, ink); continue
        P(cx - half, cy, ink); P(cx + half, cy, ink)
        if half == 3:
            if t < tg + (tt - tg) * 0.66:
                P(cx, cy, wash)
            if t % 2 == 0:
                P(cx + 2, cy, wash)
        elif half == 2 and t % 2 == 0:
            P(cx + 1, cy, wash)
    gx, gy = x0 + tg, y0 - tg
    for u in range(-7, 8):
        for o in (0, 1, -1):
            if abs(u) == 7 and o == -1:
                continue
            P(gx + u + o, gy + u, ink)
    P(gx - 9, gy - 7, ink); P(gx - 8, gy - 8, ink); P(gx + 9, gy + 7, ink); P(gx + 9, gy + 8, ink)
    for t in range(tg - 7, tg):
        cx, cy = x0 + t, y0 - t
        P(cx - 2, cy, ink); P(cx + 2, cy, ink)
        P(cx - 1, cy, ink if t % 2 == 0 else wash); P(cx, cy, wash if t % 2 == 0 else ink); P(cx + 1, cy, ink if t % 2 == 0 else wash)
    t = tg - 10
    cx, cy = x0 + t, y0 - t
    for (ox, oy) in disc_mask(0, 0, 2.6):
        P(cx + ox, cy + oy, ink)
    P(cx - 1, cy - 1, wash)


def hatch(pen, mask, step, ink, mode='diag'):
    for (x, y) in mask:
        if mode == 'diag' and (x + y) % step == 0:
            pen.put(x, y, ink)
        elif mode == 'vert' and x % step == 0:
            pen.put(x, y, ink)
        elif mode == 'horiz' and y % step == 0:
            pen.put(x, y, ink)


def ring(mask):
    return {(x, y) for (x, y) in mask if any((x + a, y + b) not in mask for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def skirmish_drawing(strong=True):
    """crossed longswords over a round shield"""
    W = 64
    ink = INK[1] if strong else INK[3]
    mid = INK[3] if strong else INK[4]
    wash = INK[4] if strong else INK[5]
    back = Pen(W, W)
    # round shield: planked, rimmed, its lower right hatched
    cx = cy = 31.5
    outer = disc_mask(cx, cy, 18)
    inner = disc_mask(cx, cy, 15)
    for p in ring(outer):
        back.put(*p, ink)
    for p in ring(inner):
        back.put(*p, mid)
    for (x, y) in inner:
        if (x - 3) % 7 == 0 and (x, y) not in ring(inner):
            back.put(x, y, wash)
    rim_band = outer - inner
    for (x, y) in rim_band:
        if (x, y) not in ring(outer) and (x, y) not in ring(inner) and (x - cx) + (y - cy) > 6 and (x + y) % 2 == 0:
            back.put(x, y, wash)
    shade = {(x, y) for (x, y) in inner if (x - cx) * 0.7 + (y - cy) > 4 and (x, y) not in ring(inner)}
    hatch(back, shade, 3, wash)
    # rim studs
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        X, Y = round(cx + 16.5 * math.cos(a)), round(cy + 16.5 * math.sin(a))
        back.put(X, Y, ink)
    s2 = Pen(W, W)
    sword(s2, 8, 56, 12, 49, ink, wash, flip=True, W=W)
    s1 = Pen(W, W)
    sword(s1, 8, 56, 12, 49, ink, wash, W=W)
    back.layer_over(s2)
    back.layer_over(s1)
    return back.s


def practice_drawing(strong=False):
    """a straw training dummy on a post, a target painted on its chest, a wooden sword in the ground"""
    W, H = 56, 56
    ink = INK[1] if strong else INK[3]
    mid = INK[3] if strong else INK[4]
    wash = INK[4] if strong else INK[5]
    p = Pen(W, H)
    # ground mound with grass tufts
    for x in range(8, 48):
        y = round(50 - 2.2 * math.exp(-((x - 28) / 11) ** 2))
        p.put(x, y, ink)
        if x > 30 and x % 2 == 0:
            p.put(x, y + 2, wash)
    for (gx, gy) in ((12, 49), (19, 48), (40, 49), (45, 49)):
        p.put(gx, gy, mid); p.put(gx - 1, gy - 1, mid); p.put(gx + 1, gy - 2, mid)
    # post
    for y in range(17, 49):
        p.put(26, y, ink); p.put(30, y, ink)
        if y % 2 == 0:
            p.put(29, y, wash)
    # cross arm with straw tufts
    for x in range(8, 49):
        p.put(x, 20, ink); p.put(x, 23, ink)
        if x % 2 == 1 and x > 30:
            p.put(x, 22, wash)
    for (tx, d) in ((8, -1), (48, 1)):
        for k in range(4):
            p.put(tx + d * (1 + k // 2), 19 - k + 3, mid)
        p.put(tx + d * 2, 21, mid); p.put(tx + d * 3, 22, mid); p.put(tx + d * 2, 24, mid); p.put(tx + d * 3, 25, mid)
    # sack head, tied off
    head = disc_mask(28, 10.5, 6)
    for q in ring(head):
        p.put(*q, ink)
    hatch(p, {(x, y) for (x, y) in head if x > 29 and y > 9 and (x, y) not in ring(head)}, 2, wash)
    for (tx, ty) in ((27, 3), (29, 3), (26, 2), (30, 2), (28, 2)):
        p.put(tx, ty, mid)                          # straw poking out of the tied top
    for x in range(25, 32):
        p.put(x, 17, ink)                           # the cord at the neck
    # straw torso (a stuffed sack) with a painted target
    body = {(x, y) for y in range(24, 44) for x in range(19, 38)
            if ((x - 28) / 9.5) ** 2 + ((y - 33) / 10.5) ** 2 <= 1.0}
    bodyp = Pen(W, H)
    for q in ring(body):
        bodyp.put(*q, ink)
    tgt = disc_mask(27.5, 32.5, 5)
    for q in ring(tgt):
        bodyp.put(*q, ink)
    for q in ring(disc_mask(27.5, 32.5, 2)):
        bodyp.put(*q, mid)
    hatch(bodyp, {(x, y) for (x, y) in body if (x - 28) * 0.6 + (y - 33) > 4 and (x, y) not in ring(body)}, 2, wash, 'diag')
    for (sx, sy) in ((21, 27), (35, 29), (22, 39), (34, 40)):
        bodyp.put(sx, sy, mid); bodyp.put(sx + 1, sy + 1, mid)
    p.layer_over(bodyp, solid=body)
    # wooden practice sword stuck in the ground, leaning
    sw = Pen(W, H)
    for k in range(18):
        x, y = 44 - k // 3, 49 - k
        sw.put(x, y, ink); sw.put(x + 1, y, ink if k > 14 else wash)
    for u in range(-3, 4):
        sw.put(39 + u, 32, ink)
    for k in range(4):
        sw.put(38 - k // 3, 31 - k, ink)
    p.layer_over(sw)
    return p.s


def campaign_drawing(strong=False):
    """a keep on its hill: crenellated towers, a gate, arrow slits, a banner, a road winding up"""
    W, H = 60, 54
    ink = INK[1] if strong else INK[3]
    mid = INK[3] if strong else INK[4]
    wash = INK[4] if strong else INK[5]
    p = Pen(W, H)
    # hill with hatching on its right flank and a winding road
    hill = set()
    for x in range(2, 58):
        top = round(40 - 9 * math.exp(-((x - 30) / 16) ** 2))
        p.put(x, top, ink)
        for y in range(top + 1, 52):
            hill.add((x, y))
    hatch(p, {(x, y) for (x, y) in hill if x > 34 and y < 50}, 3, wash, 'diag')
    for k in range(10):
        p.put(30 + round(4 * math.sin(k / 2.0)), 42 + k, mid)
        p.put(33 + round(4 * math.sin(k / 2.0)), 42 + k, mid)
    # keep: central tower, two side towers, curtain wall
    k = Pen(W, H)

    def tower(x0, x1, ytop, ybot, merlons=True):
        for y in range(ytop, ybot + 1):
            k.put(x0, y, ink); k.put(x1, y, ink)
        for x in range(x0, x1 + 1):
            k.put(x, ytop, ink)
        if merlons:
            for x in range(x0, x1 + 1):
                if (x - x0) % 4 in (0, 1):
                    k.put(x, ytop - 1, ink); k.put(x, ytop - 2, ink)
                elif (x - x0) % 4 == 2:
                    k.put(x, ytop - 2, ink)
            for x in range(x0, x1 + 1):
                if (x - x0) % 4 == 3:
                    k.put(x, ytop - 2, ink)
        # right face in shadow: vertical hatching
        for y in range(ytop + 2, ybot):
            for x in range(x1 - 2, x1):
                if x % 2 == 0:
                    k.put(x, y, wash)
    tower(14, 22, 18, 34)
    tower(37, 45, 18, 34)
    tower(23, 36, 8, 34)
    for x in range(14, 46):
        k.put(x, 34, ink)
    # gate: an arch with a portcullis
    for y in range(26, 34):
        k.put(27, y, ink); k.put(32, y, ink)
    for x in range(28, 32):
        k.put(x, 25, ink)
    k.put(27, 26, None); k.put(32, 26, None); k.put(28, 25, None); k.put(31, 25, None)
    k.put(27, 27, ink); k.put(32, 27, ink); k.put(28, 26, ink); k.put(31, 26, ink)
    for y in range(27, 34):
        for x in range(28, 32):
            if x % 2 == 1 or y % 2 == 0:
                k.put(x, y, mid)
    # arrow slits and a window
    for (sx, sy) in ((18, 23), (41, 23), (29, 13), (30, 13), (26, 18), (33, 18)):
        k.put(sx, sy, ink); k.put(sx, sy + 1, ink)
    # stone courses
    for y in (12, 22, 30):
        for x in range(24, 36):
            if (x + y) % 5 != 0:
                k.put(x, y, wash)
    # banner on the keep
    for y in range(0, 6):
        k.put(29, y, ink)
    for (bx, by) in ((30, 0), (31, 0), (32, 0), (33, 1), (30, 1), (31, 1), (32, 1), (30, 2), (31, 2), (32, 2), (33, 2), (30, 3), (31, 3)):
        k.put(bx, by, mid if (bx, by) not in ((30, 0), (30, 1), (30, 2), (30, 3)) else ink)
    p.layer_over(k)
    return p.s


ASSETS['ink-skirmish'] = lambda: skirmish_drawing(True)
ASSETS['ink-practice'] = lambda: practice_drawing(False)
ASSETS['ink-campaign'] = lambda: campaign_drawing(False)


# ================================================================ wax seals
CROWN = ['x....x....x',
         'xx..xxx..xx',
         'xxx.xxx.xxx',
         'xxxxxxxxxxx',
         'xxxxxxxxxxx',
         '...........',
         'xxxxxxxxxxx']
STAR = ['....x....',
        '....x....',
        '...xxx...',
        'xxxxxxxxx',
        '.xxxxxxx.',
        '..xxxxx..',
        '..xx.xx..',
        '.xx...xx.',
        '.x.....x.']


def wax_seal(r0=13, device=CROWN, seed=4, drips=((2.3, 3), (1.2, 2)), shadow=True, broken=False):
    """a stamped wax seal in relief, six tones of wax. From the outside in: the poured lip, its slope lit on
    the top left; the torus squeezed out by the stamp, its crest catching the light; the stamp's beaded border;
    the sunk field's inner wall, dark where it faces away from the light; the field, with the device standing
    proud of it, edges lit top left and shadowed bottom right. A glint on the crest, a few bubbles in the lip,
    drips running off the edge, a soft shadow under it all."""
    rnd = random.Random(seed)
    ph1, ph2 = rnd.uniform(0, 6.28), rnd.uniform(0, 6.28)
    size = 2 * r0 + 9
    c = size / 2 - 0.5
    S = Spr(size, size)
    lump = Noise(seed + 50, 3)

    def R(th):
        return r0 + 0.7 * math.sin(3 * th + ph1) + 0.5 * math.sin(5 * th + ph2)
    shape = set()
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            if math.hypot(dx, dy) <= R(math.atan2(dy, dx)):
                shape.add((x, y))
    driptips = []
    for (ang, ln) in drips:
        for k in range(ln + 3):
            rr = r0 - 1 + k
            X, Y = round(c + rr * math.cos(ang)), round(c + rr * math.sin(ang))
            wdt = 1 if k > ln else 2
            for a in range(-wdt + 1, wdt):
                for b in range(-wdt + 1, wdt):
                    shape.add((X + a, Y + b))
        driptips.append((round(c + (r0 + ln) * math.cos(ang)), round(c + (r0 + ln) * math.sin(ang))))
    ri = r0 - 5.2
    lx, ly = -0.7071, -0.7071
    for (x, y) in shape:
        dx, dy = x - c, y - c
        d = math.hypot(dx, dy) + 1e-6
        facing = (dx / d) * lx + (dy / d) * ly
        Rt = R(math.atan2(dy, dx))
        if d > Rt - 1.5:
            col = WAX[4] if facing > 0.45 else (WAX[3] if facing > -0.2 else (WAX[2] if facing > -0.7 else WAX[1]))
        elif d > ri + 2.3:
            crest = (ri + 2.3 + Rt - 1.5) / 2
            # the torus: its inner slope faces the centre, its outer slope faces out; light from the top left
            slope = (-1.0 if d < crest - 0.5 else (1.0 if d > crest + 0.5 else 0.0))
            up = facing * (0.55 + 0.45 * slope) + (lump(x, y) - 0.5) * 0.22
            col = WAX[6] if (up > 0.7 and abs(d - crest) < 0.9) else (WAX[5] if up > 0.32 else (WAX[4] if up > -0.12 else (WAX[3] if up > -0.55 else WAX[2])))
        elif d > ri + 1.1:
            a = math.atan2(dy, dx)
            bead = (round(a / (2 * math.pi) * 2 * math.pi * (ri + 1.7) / 2.6)) % 2 == 0
            col = (WAX[5] if facing > -0.2 else WAX[4]) if bead else WAX[2]
        elif d > ri:
            col = WAX[1] if facing > 0.1 else (WAX[4] if facing < -0.4 else WAX[2])
        else:
            col = WAX[3] if (x * 5 + y * 3) % 13 else WAX[2]
        S.set(x, y, col)
    # the device, standing proud of the field
    dev = grid_mask(device)
    dw = max(len(r) for r in device); dh = len(device)
    ox, oy = round(c - (dw - 1) / 2), round(c - (dh - 1) / 2)
    for (x, y) in dev:
        for (a, b) in ((1, 0), (0, 1), (1, 1)):
            if (x + a, y + b) not in dev:
                S.set(ox + x + a, oy + y + b, WAX[1])
    for (x, y) in dev:
        X, Y = ox + x, oy + y
        S.set(X, Y, WAX[4])
        if (x - 1, y) not in dev or (x, y - 1) not in dev:
            S.set(X, Y, WAX[5])
        elif (x + 1, y) not in dev or (x, y + 1) not in dev:
            S.set(X, Y, WAX[3])
    top = min(dev, key=lambda q: (q[0] + q[1], q[1]))
    S.set(ox + top[0], oy + top[1], WAX[6])
    # bubbles in the lip: a dark pit with a lit rim above it
    for _ in range(4):
        a = rnd.uniform(0, 2 * math.pi)
        X, Y = round(c + (R(a) - 0.8) * math.cos(a)), round(c + (R(a) - 0.8) * math.sin(a))
        if (X, Y) in shape and (X, Y - 1) in shape:
            S.set(X, Y, WAX[1]); S.set(X, Y - 1, WAX[5])
    # outline, the drips' lit edges, the glint on the crest
    outline = {(x + a, y + b) for (x, y) in shape for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - shape
    for p in outline:
        S.set(*p, WAX[0])
    for (X, Y) in driptips:
        if (X - 1, Y - 1) in shape:
            S.set(X - 1, Y - 1, WAX[5])
    gx, gy = round(c - r0 * 0.6), round(c - r0 * 0.6)
    for (a, b) in ((0, 0), (1, 0), (0, 1), (2, -1), (-1, 2)):
        if (gx + a, gy + b) in shape:
            S.set(gx + a, gy + b, WAX[6])
    if broken:
        # a crack across the field where the letter was opened, its lower lip catching the light
        y = round(c) - 4
        x = round(c - r0) + 1
        while x < c + r0:
            if (x, y) in shape:
                S.set(x, y, WAX[0])
                if (x, y + 1) in shape:
                    S.set(x, y + 1, WAX[5])
                if (x, y - 1) in shape and rnd.random() < 0.5:
                    S.set(x, y - 1, WAX[1])
            x += 1
            if rnd.random() < 0.45:
                y += rnd.choice([-1, 1, 1])
    if shadow:
        sh = Spr(size + 2, size + 3)
        for (x, y) in shape | outline:
            for (a, b, al) in ((1, 2, 0.45), (2, 3, 0.2)):
                k = (x + a, y + b)
                if k not in shape and k not in outline and sh.get(*k)[3] < al * 255:
                    sh.set(k[0], k[1], rgba(SHADOW, round(al * 255)))
        sh.blit(S, 0, 0)
        return sh
    return S


ASSETS['wax-crown'] = lambda: wax_seal(13, CROWN, seed=4)
ASSETS['wax-star'] = lambda: wax_seal(12, STAR, seed=9, drips=((0.6, 2), (2.6, 3)))
ASSETS['wax-broken'] = lambda: wax_seal(12, CROWN, seed=6, drips=((2.0, 2),), broken=True)


def ribbon(color=TEAL, ln=18):
    """two ribbon tails hanging from under a seal, cut in swallowtails"""
    W = 20
    S = Spr(W, ln + 2)
    for (x0, lean) in ((4, -1), (11, 1)):
        for y in range(ln):
            off = round(lean * y / 7.0)
            for i in range(5):
                X = x0 + off + i
                col = color[3] if i in (1, 2) else (color[4] if i == 0 else color[2])
                if y > ln - 4 and i == 2 and y >= ln - 3 + (0 if y < ln - 1 else 0):
                    continue
                if y == ln - 1 and i in (1, 2, 3):
                    continue
                S.set(X, y, col)
            S.set(x0 + off - 1, y, color[0]); S.set(x0 + off + 5, y, color[0])
        S.set(x0 + round(lean * (ln - 1) / 7.0) + 2, ln - 2, None)
    S.shadow(1, 1, 0.35)
    return S


ASSETS['ribbon-teal'] = lambda: ribbon(TEAL, 26)


# ================================================================ brass tack (pins a sheet to the table)
def tack():
    rows = ['.kkkk.',
            'khHmdk',
            'kHmmdk',
            'kmmddk',
            'kddddk',
            '.kkkk.']
    s = Spr(6, 6)
    s.grid(0, 0, rows, {'k': BRASS[0], 'h': BRASS[7], 'H': BRASS[6], 'm': BRASS[4], 'd': BRASS[2]})
    s.shadow(1, 1, 0.45)
    return s


ASSETS['tack'] = tack


# ================================================================ the Rivergate map, inked and washed
def rivergate_map(W=136, H=80, seed=21):
    """A working river town: the mill (A, Mill Hamlet) on its stream among strip fields, the trade crossing
    (B) where the road bridges the river, the shrine (C) on its hill. Ink line work, a muted water wash,
    hatched hills, tree symbols, a dotted road, a compass. Point discs carry the letters (drawn as text)."""
    rnd = random.Random(seed)
    S, paper = parchment(W, H, kind='map', seed=seed, shadow=(1, 1), deckle=False, rim=2, stains=0, fibres=0.5)
    ink, mid, wash = INK[2], INK[3], INK[5]
    WATER, WATER2, WATER3 = '#9db6b1', '#7a9d9b', '#c4d6cd'
    # double rule
    for x in range(2, W - 2):
        S.set(x, 2, ink); S.set(x, H - 3, ink)
        if 4 <= x <= W - 5:
            S.set(x, 4, wash); S.set(x, H - 5, wash)
    for y in range(2, H - 2):
        S.set(2, y, ink); S.set(W - 3, y, ink)
        if 4 <= y <= H - 5:
            S.set(4, y, wash); S.set(W - 5, y, wash)
    X0, X1, Y0, Y1 = 6, W - 7, 6, H - 7

    def inside(x, y):
        return X0 <= x <= X1 and Y0 <= y <= Y1
    # river: centre line through control points, 4 px wide wash with inked banks
    pts = [(X0, 47), (18, 49), (34, 55), (52, 58), (66, 57), (82, 51), (98, 43), (114, 39), (X1, 35)]

    def river_y(x):
        for (a, b), (c, d) in zip(pts, pts[1:]):
            if a <= x <= c:
                t = (x - a) / (c - a); t = (1 - math.cos(t * math.pi)) / 2
                return b + (d - b) * t
        return pts[-1][1]
    water = set()
    for x in range(X0, X1 + 1):
        yc = river_y(x)
        half = 2.4 + 0.6 * math.sin(x / 9.0)
        for y in range(math.floor(yc - half), math.ceil(yc + half) + 1):
            if abs(y - yc) <= half and inside(x, y):
                water.add((x, y))
    # the mill stream from the north
    for y in range(20, 56):
        xc = 26 + 2.0 * math.sin(y / 5.0)
        for x in range(math.floor(xc - 1), math.ceil(xc + 1) + 1):
            if abs(x - xc) <= 1.1 and inside(x, y) and y < river_y(x) + 1:
                water.add((x, y))
    for (x, y) in water:
        S.set(x, y, WATER)
    for (x, y) in water:
        if (x, y - 1) not in water and inside(x, y - 1):
            S.set(x, y - 1, ink)
        if (x, y + 1) not in water and inside(x, y + 1):
            S.set(x, y + 1, ink)
        if (x - 1, y) not in water and (x, y - 1) in water and inside(x - 1, y):
            S.set(x - 1, y, ink)
        if (x + 1, y) not in water and (x, y - 1) in water and inside(x + 1, y):
            S.set(x + 1, y, ink)
    for (x, y) in water:
        if (x, y + 1) not in water:
            S.set(x, y, WATER2)          # deeper water along the south bank
    for x in range(X0 + 3, X1 - 3, 7):
        y = round(river_y(x) - 0.5)
        if (x, y) in water and (x + 1, y) in water:
            S.set(x, y, WATER3); S.set(x + 1, y, WATER3)
    # hills with hachures (top right, around the shrine) and a ridge along the north
    def hill(cx, base, wdt, ht):
        for x in range(cx - wdt, cx + wdt + 1):
            t = (x - cx) / wdt
            top = round(base - ht * (1 - t * t))
            if inside(x, top):
                S.set(x, top, ink)
            if x > cx and (x - cx) % 2 == 0:
                for y in range(top + 2, min(base, top + 2 + round(ht * 0.6))):
                    if inside(x, y) and (x, y) not in water:
                        S.set(x, y, wash)
    hill(112, 31, 16, 11)
    hill(88, 22, 9, 6)
    hill(126, 24, 6, 5)
    # the shrine on its hill: two columns, a roof, a step
    shrine = ['..kkkkk..',
              '.kkkkkkk.',
              'kkkkkkkkk',
              '.k.k.k.k.',
              '.k.k.k.k.',
              '.k.k.k.k.',
              'kkkkkkkkk']
    S.grid(108, 13, shrine, {'k': ink})
    # the mill: a house with a water wheel on the stream, and the hamlet's roofs
    mill = ['...kk.....',
            '..kmmk....',
            '.kmmmmk...',
            'kkkkkkkk..',
            '.k....k...',
            '.k.kk.k...',
            '.kkkkkk...']
    S.grid(13, 21, mill, {'k': ink, 'm': mid})
    wheel = disc_mask(25.5, 27.5, 3)
    for q in ring(wheel):
        S.set(*q, ink)
    S.set(25, 27, ink); S.set(26, 28, ink); S.set(23, 27, mid); S.set(28, 28, mid)
    for (hx, hy) in ((33, 16), (38, 20), (32, 23)):
        S.grid(hx, hy, ['.kk.', 'kmmk', 'kkkk', 'k..k', 'kkkk'], {'k': ink, 'm': mid})
    # strip fields north of the river
    for (fx, fy, fw, fh) in ((44, 13, 14, 8), (44, 23, 14, 7), (8, 33, 12, 7)):
        S.frame(fx, fy, fw, fh, mid)
        for y in range(fy + 2, fy + fh - 1, 2):
            for x in range(fx + 2, fx + fw - 2):
                if (x + y) % 3:
                    S.set(x, y, '#a7a271')
    # trees: lollipop symbols, canopy lit top-left
    tree = ['.kkk.', 'kggGk', 'kgGGk', '.kkk.', '..k..']
    for (tx, ty) in ((90, 56), (96, 60), (103, 57), (109, 62), (116, 58), (122, 63), (98, 66), (112, 50), (120, 52), (60, 30), (66, 34), (72, 29), (8, 54), (14, 58)):
        if all(inside(tx + a, ty + b) and (tx + a, ty + b) not in water for a in range(5) for b in range(5)):
            S.grid(tx, ty, tree, {'k': ink, 'g': '#b3b07c', 'G': '#8d8a5a'})
    # the road: dotted, from the south edge over the bridge to the mill and up to the shrine
    road = []
    for y in range(Y1, 40, -1):
        road.append((66 + round(1.5 * math.sin(y / 4.0)), y))
    for k in range(0, 34):
        road.append((66 - k, 41 - round(k * 0.55)))
    for k in range(0, 44):
        road.append((66 + k, 41 - round(k * 0.5)))
    for i, (x, y) in enumerate(road):
        if i % 3 != 2 and inside(x, y) and (x, y) not in water:
            S.set(x, y, mid)
    # the bridge at the crossing: two rails and planks across the water
    by = round(river_y(66))
    for y in range(by - 4, by + 5):
        S.set(64, y, ink); S.set(69, y, ink)
        for x in range(65, 69):
            S.set(x, y, '#b08a5a' if y % 2 else '#8c6a40')
    # compass in the south-west corner
    comp = ['...k...',
            '..kkk..',
            '...k...',
            'k.kkk.k',
            '.kkkkk.',
            'k.kkk.k',
            '...k...',
            '...k...']
    S.grid(8, 61, comp, {'k': mid})
    S.set(11, 59, ink); S.set(10, 60, ink); S.set(12, 60, ink)
    for (px, py) in MAP_DISCS.values():
        S.blit(point_disc(), px, py)
    return S


MAP_DISCS = {'A': (9, 6), 'B': (72, 61), 'C': (92, 5)}   # top-left of each 9x9 disc inside the map


ASSETS['map-rivergate'] = rivergate_map


def point_disc():
    """an inked disc for a point letter on the map (the letter is text, drawn over it), lit on its top-left"""
    rows = ['....kkkkk....',
            '..kkmmmmmkk..',
            '.kmmmmmmmmmk.',
            '.kmmmmmmmmmk.',
            'kmmmmmmmmmmmk',
            'kmmmmmmmmmmmk',
            'kmmmmmmmmmmmk',
            'kmmmmmmmmmmmk',
            'kmmmmmmmmmmmk',
            '.kmmmmmmmmmk.',
            '.kmmmmmmmmmk.',
            '..kkmmmmmkk..',
            '....kkkkk....']
    s = Spr(13, 13)
    s.grid(0, 0, rows, {'k': INK[0], 'm': INK[2]})
    s.set(3, 3, INK[3]); s.set(4, 2, INK[3]); s.set(2, 4, INK[3])
    return s


ASSETS['map-disc'] = point_disc
