"""v4 ink illustrations for the War Table mode cards, drawn as engravings at GUI scale. Outline in the darkest
ink, weighted on the shadow side; hatching that follows the form; cross-hatching in the core shadows; a thin
ink wash under the shadow side; the paper left bare where the light falls from the top left."""
import math, random
from v4lib import Spr, rgba
from v4pal import INK

ASSETS = {}
K, D, M, L, F = INK[0], INK[1], INK[2], INK[3], INK[4]   # outline, deep, hatch, light hatch, faint
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


# ---------------------------------------------------------------- shapes (sets of pixels)
def disc(cx, cy, r):
    return {(x, y) for y in range(math.floor(cy - r) - 1, math.ceil(cy + r) + 2)
            for x in range(math.floor(cx - r) - 1, math.ceil(cx + r) + 2) if (x - cx) ** 2 + (y - cy) ** 2 <= (r + 0.4) ** 2}


def ell(cx, cy, rx, ry):
    return {(x, y) for y in range(math.floor(cy - ry) - 1, math.ceil(cy + ry) + 2)
            for x in range(math.floor(cx - rx) - 1, math.ceil(cx + rx) + 2)
            if ((x - cx) / (rx + 0.4)) ** 2 + ((y - cy) / (ry + 0.4)) ** 2 <= 1.0}


def rect(x, y, w, h):
    return {(xx, yy) for yy in range(y, y + h) for xx in range(x, x + w)}


def poly(pts):
    t = Spr(int(max(p[0] for p in pts)) + 3, int(max(p[1] for p in pts)) + 3)
    t.poly(pts, '#ffffff')
    return t.mask()


def seg(x0, y0, x1, y1):
    """the pixels of a one-pixel line, in order"""
    x0, y0, x1, y1 = round(x0), round(y0), round(x1), round(y1)
    out = []
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        out.append((x0, y0))
        if x0 == x1 and y0 == y1:
            return out
        e2 = 2 * err
        if e2 >= dy:
            err += dy; x0 += sx
        if e2 <= dx:
            err += dx; y0 += sy


def thick(x0, y0, x1, y1, r):
    out = set()
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        out |= disc(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r)
    return out


def edge(mask):
    return {(x, y) for (x, y) in mask if any((x + a, y + b) not in mask for a, b in N4)}


def depth(mask):
    """4-neighbour distance from the outside: 1 on the boundary, 2 just inside it, and so on"""
    d = {}
    front = [p for p in mask if any((p[0] + a, p[1] + b) not in mask for a, b in N4)]
    for p in front:
        d[p] = 1
    k = 1
    while front:
        nxt = []
        for (x, y) in front:
            for a, b in N4:
                q = (x + a, y + b)
                if q in mask and q not in d:
                    d[q] = k + 1; nxt.append(q)
        front = nxt; k += 1
    return d


def away(cx, cy, r):
    """a field over a round body: 0 at its middle, 1 at the edge turned from the light (top left)"""
    return lambda x, y: ((x - cx) * 0.71 + (y - cy) * 0.71) / r


# ---------------------------------------------------------------- the pen
class Ink:
    def __init__(self, w, h, seed=1):
        self.s = Spr(w, h); self.w, self.h = w, h; self.r = random.Random(seed)

    def put(self, x, y, c):
        self.s.set(x, y, c)

    def pts(self, pts, c):
        for (x, y) in pts:
            self.s.set(x, y, c)

    def knock(self, mask):
        """clear whatever lies behind a solid front object"""
        for (x, y) in mask:
            self.s.set(x, y, None)

    def wash(self, mask, a=0.2):
        """a thin ink wash, translucent, laid only where the paper is still bare"""
        col = rgba(INK[3], round(a * 255))
        for (x, y) in mask:
            if not self.s.opaque(x, y):
                self.s.set(x, y, col)

    def hatch(self, mask, c, step=3, ang='d', ph=0, field=None, t=0.0, jit=0.0):
        """parallel strokes across a mask ('d' rising, 'a' falling, 'h' rows, 'v' columns). With a field a stroke only runs
        where field > t plus its own jitter, so the hatching ends raggedly toward the light, as a pen stroke does."""
        jits = {}
        for (x, y) in sorted(mask):
            v = {'d': x + y, 'a': x - y, 'h': y, 'v': x}[ang] + ph
            if v % step:
                continue
            if field is not None:
                k = v // step
                if k not in jits:
                    jits[k] = self.r.uniform(-jit, jit)
                if field(x, y) <= t + jits[k]:
                    continue
            self.s.set(x, y, c)

    def line(self, x0, y0, x1, y1, c=K, dash=0, gap=1):
        for i, (x, y) in enumerate(seg(x0, y0, x1, y1)):
            if dash and i % (dash + gap) >= dash:
                continue
            self.s.set(x, y, c)

    def outline(self, mask, c=K, heavy=None):
        """outline a mask; heavy = the colour of a second line inside the right and lower edges (the shadow side)"""
        e = edge(mask)
        for (x, y) in e:
            self.s.set(x, y, c)
        if heavy:
            for (x, y) in e:
                if (x + 1, y) not in mask and (x - 1, y) in mask and (x - 1, y) not in e:
                    self.s.set(x - 1, y, heavy)
                if (x, y + 1) not in mask and (x, y - 1) in mask and (x, y - 1) not in e:
                    self.s.set(x, y - 1, heavy)

    def contour(self, mask, field, c, rings=(3, 5), t=0.2, jit=0.12):
        """form-following hatching: strokes parallel to the outline, at the given depths, on the shadow side"""
        d = depth(mask)
        js = {k: self.r.uniform(-jit, jit) for k in rings}
        for (x, y) in sorted(mask):
            k = d.get((x, y))
            if k in rings and field(x, y) > t + js[k] + 0.12 * rings.index(k):
                self.s.set(x, y, c)

    def stipple(self, mask, c, p, field=None, t=0.0):
        for (x, y) in sorted(mask):
            if (field is None or field(x, y) > t) and self.r.random() < p:
                self.s.set(x, y, c)

    def rope(self, x0, x1, y):
        """a twisted cord two pixels tall, the strands slanting, the upper row catching the light"""
        for x in range(x0, x1 + 1):
            k = (x - x0) % 3
            self.s.set(x, y, K if k == 0 else (L if k == 1 else M))
            self.s.set(x, y + 1, K if k == 1 else (M if k == 2 else D))


# ---------------------------------------------------------------- practice: the training dummy
def practice_drawing():
    """a training dummy: a dented kettle hat over a burlap head with stitched eyes, a rough crossbar lashed to
    the post, a stuffed sack body with a painted target and an arrow in the outer ring, tied at the waist over
    a skirt of straw; a wooden practice sword driven into the ground beside it; tufts on a low mound and the
    cast shadow in ruled lines to the lower right"""
    W, H = 68, 72
    p = Ink(W, H, seed=3)
    cx = 34

    def gy(x):
        return 65 + round(2.0 * ((x - cx) / 22.0) ** 2)
    # ---- the ground
    sh = {(x, y) for (x, y) in ell(48, 68, 15, 2.6) if y > gy(x)}
    p.wash(sh, 0.16)
    p.hatch(sh, M, 2, 'h', 0, field=lambda x, y: 1 - abs(x - 48) / 15.0, t=0.1, jit=0.2)
    for x in range(9, 61):
        if x % 13 != 6:
            p.put(x, gy(x), M if 13 < x < 56 else L)
    for (x0, x1, y) in ((7, 16, 70), (21, 31, 70), (57, 64, 70)):
        p.line(x0, y, x1, y, L, dash=5, gap=2)
    for gx in (11, 24, 44, 58):
        y = gy(gx)
        p.put(gx, y - 1, K); p.put(gx - 1, y - 2, M); p.put(gx + 1, y - 3, M); p.put(gx + 2, y - 2, L)
    for (sx, sy, dx, dy) in ((21, 67, 2, -1), (40, 68, 3, -1), (54, 71, 2, 0)):
        p.line(sx, sy, sx + dx, sy + dy, L)
    # ---- the post, its front face lit and its right face in shadow
    pb = gy(cx)
    post = rect(31, 24, 6, pb - 24)
    p.knock(post); p.outline(post, K)
    for y in range(25, pb):
        p.put(35, y, M if y % 3 else D)
        if y % 5 == 1:
            p.put(33, y, L)
    p.put(30, pb - 1, K); p.put(37, pb - 1, K); p.put(38, pb, M)
    # ---- the wooden practice sword, driven into the ground beside the dummy
    by = gy(15)
    blade = rect(13, 46, 4, by - 46)
    p.knock(blade); p.outline(blade, K)
    for y in range(47, by):
        p.put(15, y, L if y % 4 else M)
    for x in (13, 14, 15, 16):
        p.put(x, by - 1, M)
    p.put(12, by - 1, K); p.put(17, by - 1, K)
    p.line(18, by, 22, by, M, dash=2, gap=1)
    guard = rect(9, 43, 12, 3)
    p.knock(guard); p.outline(guard, K)
    for x in range(15, 20):
        p.put(x, 44, M)
    grip = rect(13, 37, 4, 6)
    p.knock(grip); p.outline(grip, K)
    for y in range(38, 42):
        p.put(14, y, D if y % 2 else None); p.put(15, y, M if y % 2 else D)
    pom = disc(14.5, 34.5, 2.0)
    p.knock(pom); p.outline(pom, K)
    p.put(15, 35, D); p.put(14, 35, M)
    # ---- the body: a stuffed sack hung from the crossbar
    def bhw(y):
        return 8.0 + 3.6 * math.sin(math.pi * (y - 31) / 25.0) - (2 if y == 32 else (1 if y == 33 else 0))
    body = {(x, y) for y in range(32, 54) for x in range(cx - 13, cx + 14) if abs(x - cx + 0.5) <= bhw(y)}
    p.knock(body)
    fb = away(cx - 3, 39, 13)
    p.wash({q for q in body if fb(*q) > 0.3}, 0.18)
    p.contour(body, fb, M, rings=(3, 5), t=0.3)
    p.hatch(body, D, 3, 'd', field=fb, t=0.8, jit=0.08)
    p.outline(body, K, heavy=D)
    dd = depth(body)
    for (x, y) in sorted(body):
        if dd[(x, y)] == 4 and x < cx - 5 and y % 2 == 0:
            p.put(x, y, L)
    # a tear on the upper right with straw poking out
    p.put(40, 35, K); p.put(41, 36, K); p.put(40, 36, D)
    for (sx, sy, ex, ey) in ((42, 36, 45, 34), (42, 37, 46, 37)):
        p.line(sx, sy, ex, ey, M)
    # the painted target: a ruled red ring, a bare ring, the bull
    tx, ty = cx - 0.5, 42.5
    outer, mid, bull = disc(tx, ty, 5.6), disc(tx, ty, 3.6), disc(tx, ty, 1.4)
    p.knock(outer)
    for (x, y) in outer - mid:
        if x % 2 == 0:
            p.put(x, y, M)
    p.wash({q for q in outer if fb(*q) > 0.3}, 0.18)
    p.outline(outer, K); p.outline(mid, K); p.pts(bull, K)
    # an arrow in the outer ring, its fletching out to the right
    shaft = seg(tx + 4, ty + 2, tx + 18, ty + 8)
    p.pts(shaft, K)
    for (x, y) in shaft[-5:-1]:
        p.put(x - 1, y - 1, M); p.put(x, y + 1, M)
    p.put(shaft[-1][0] + 1, shaft[-1][1], K)
    # the waist tie, knotted on the left, with its two ends hanging
    p.rope(cx - 8, cx + 8, 52)
    for (x, y, c) in ((cx - 8, 54, K), (cx - 9, 55, M), (cx - 9, 56, K), (cx - 7, 55, K), (cx - 7, 56, M), (cx - 6, 57, K)):
        p.put(x, y, c)
    # straw bursting out below the tie, single strands with air between them
    for i, x in enumerate(range(cx - 7, cx + 8, 2)):
        ln = 4 + (i * 5) % 3
        sl = (x - cx) / 5.0
        p.line(x, 54, round(x + sl * ln / 2.0), 54 + ln, M if i % 3 else K)
    for i, x in enumerate(range(cx - 6, cx + 7, 2)):
        p.line(x, 54, round(x + (x - cx) / 6.0), 56 + i % 2, L)
    # ---- the crossbar, rough timber lashed across the post
    bar = rect(5, 27, 59, 5)
    p.knock(bar); p.outline(bar, K)
    for x in range(6, 63):
        p.put(x, 30, M if (x % 2 == 0 or x > cx + 4) else L)
    for (x0, x1, y) in ((8, 15, 28), (19, 27, 29), (41, 49, 28), (52, 60, 29), (11, 16, 29)):
        p.line(x0, y, x1, y, L, dash=3, gap=1)
    p.put(5, 27, None); p.put(63, 31, None); p.put(6, 28, K); p.put(62, 30, K)
    for x in range(cx - 9, cx + 10):
        if (x, 32) in body and x % 2:
            p.put(x, 32, M)
    # the lashing: three turns of rope slanting over the joint
    for k in range(3):
        x0 = cx - 3 + k * 3
        p.line(x0, 26, x0 + 2, 32, M); p.put(x0, 26, K); p.put(x0 + 2, 32, K)
    # ---- the head: a burlap sack, X-stitched eyes and a sewn mouth, tied off at the neck
    head = ell(cx, 17, 7.5, 7.5)
    p.knock(head)
    fh = away(cx - 2, 15, 9)
    p.wash({q for q in head if fh(*q) > 0.35}, 0.18)
    p.contour(head, fh, M, rings=(3,), t=0.5)
    p.hatch(head, D, 3, 'd', field=fh, t=0.8, jit=0.08)
    p.outline(head, K, heavy=D)
    for (ex, ey) in ((cx - 3, 17), (cx + 3, 17)):
        for (a, b) in ((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)):
            p.put(ex + a, ey + b, K)
    for x in range(cx - 2, cx + 3):
        p.put(x, 21, K if x % 2 == 0 else M)
    for y in (14, 16, 18, 20):
        p.put(cx - 6, y, L)
    p.rope(cx - 3, cx + 3, 24)
    # ---- the kettle hat: a dented iron dome with a raised comb, rivets round the band, a wide brim
    brim = ell(cx, 10, 11, 2.2)
    p.knock(brim)
    p.hatch(brim, M, 3, 'd', field=lambda x, y: (x - cx) / 11.0, t=0.3, jit=0.1)
    p.outline(brim, K, heavy=D)
    for x in range(cx - 6, cx + 7):
        if (x, 13) in head:
            p.put(x, 13, M if x % 2 else D)
    dome = {q for q in ell(cx, 9, 7, 7) if q[1] <= 9}
    p.knock(dome)
    fd = away(cx - 3, 5, 8)
    p.wash({q for q in dome if fd(*q) > 0.2}, 0.22)
    p.contour(dome, fd, M, rings=(3, 5), t=0.05)
    p.hatch(dome, D, 3, 'a', 0, field=fd, t=0.7, jit=0.1)
    p.outline(dome, K, heavy=D)
    for y in range(3, 9):
        p.put(cx, y, K); p.put(cx - 1, y, None)
    for x in (cx - 5, cx - 2, cx + 2, cx + 5):
        p.put(x, 8, K)
    p.put(cx - 4, 5, M); p.put(cx - 5, 6, M); p.put(cx - 4, 6, None)
    for (sx, sy, ex, ey) in ((cx - 7, 12, cx - 9, 15), (cx - 7, 14, cx - 8, 17), (cx + 7, 12, cx + 9, 15), (cx + 7, 14, cx + 8, 17)):
        p.line(sx, sy, ex, ey, L)
    return p.s


ASSETS['ink-practice'] = practice_drawing


# ---------------------------------------------------------------- skirmish: crossed swords behind a heater shield
def longsword(W, H, gx, gy, tip_t, seed=7):
    """a longsword at 45 degrees on its own sheet, guard centred on (gx, gy), the blade rising to the upper
    right. t runs along the blade, u across it (u grows toward the lower right, the shadow side)."""
    a = Ink(W, H, seed)
    taper = 16

    def tu(x, y):
        return (x - gx) - (y - gy), (x - gx) + (y - gy)

    def bw(t):
        return 3 if t <= tip_t - taper else max(0, round(3.4 * (tip_t - t) / taper))
    allpx = [(x, y) for y in range(H) for x in range(W)]
    blade = {k for k in allpx if 2 <= tu(*k)[0] <= tip_t and abs(tu(*k)[1]) <= bw(tu(*k)[0])}
    for (x, y) in blade:
        t, u = tu(x, y)
        w = bw(t)
        if abs(u) == w:
            c = K
        elif u < 0:
            c = L if (u == -2 and t % 14 == 5) else None          # the lit bevel, a glint of line here and there
        elif u == 0:
            c = M if t < tip_t * 0.68 else (L if t % 4 == 0 else None)   # the fuller, fading toward the point
        else:
            c = M if (u == 2 and t % 6 == 1) else L               # the shaded bevel
        a.put(x, y, c)
    # the guard: a bar across the blade swelling at the ends
    guard = {k for k in allpx if -1 <= tu(*k)[0] <= 1 and abs(tu(*k)[1]) <= 11}
    for s in (-1, 1):
        guard |= disc(gx + s * 6, gy + s * 6, 1.8)
    a.knock(guard); a.outline(guard, K)
    ge = edge(guard)
    for k in guard:
        if k not in ge and tu(*k)[1] > 0:
            a.put(k[0], k[1], M)
    # the grip, wrapped in cord; the pommel, a wheel with a boss
    grip = {k for k in allpx if -11 <= tu(*k)[0] <= -2 and abs(tu(*k)[1]) <= 2}
    a.knock(grip); a.outline(grip, K)
    gre = edge(grip)
    for k in grip:
        t, u = tu(*k)
        if k not in gre:
            a.put(k[0], k[1], D if (t + 1) % 3 == 0 else (M if u > 0 else None))
    pc = (gx - 7.5, gy + 7.5)
    pom = disc(pc[0], pc[1], 3.0)
    a.knock(pom); a.outline(pom, K)
    pe = edge(pom)
    for (x, y) in pom:
        if (x, y) not in pe and (x - pc[0]) + (y - pc[1]) > 0.8:
            a.put(x, y, D if (x + y) % 2 else M)
    for (x, y) in ((round(pc[0] - 0.5), round(pc[1] - 0.5)), (round(pc[0] + 0.5), round(pc[1] - 0.5)), (round(pc[0] - 0.5), round(pc[1] + 0.5))):
        a.put(x, y, K)
    return a


def skirmish_drawing():
    """two longswords crossed behind a heater shield. The shield is rimmed in riveted iron and quartered, the
    red quarters ruled in the heraldic way with upright lines; its right side turns from the light in wash and
    hatching; a nick bitten from the rim and a gouge across the first quarter. The swords show their points
    above and their hilts below: fullers, a lit and a shaded bevel, swelling guards, corded grips, wheel pommels."""
    from PIL import Image
    W = H = 76
    p = Ink(W, H, seed=5)
    a = longsword(W, H, 16, 61, 110)
    p.s.im.alpha_composite(a.s.im.transpose(Image.FLIP_LEFT_RIGHT))
    p.s.im.alpha_composite(a.s.im)
    p.s.px = p.s.im.load()
    cx = 37.5

    def top(x):
        return 14 + round(((x - cx) / 18.5) ** 2 * 1.6)

    def hw(y):
        return 18.5 if y <= 36 else 18.5 * max(0.0, 1 - ((y - 36) / 31.0) ** 1.7)
    shield = {(x, y) for y in range(12, 68) for x in range(16, 60) if y >= top(x) and abs(x - cx) <= hw(y)}
    halo = {(x + a_, y + b) for (x, y) in shield for a_, b in N4 + ((0, 0),)}
    p.knock(halo)
    dep = depth(shield)
    fs = lambda x, y: (x - cx) / 18.5 * 0.9 + (y - 30) / 60.0 * 0.3
    field = {k for k, d in dep.items() if d >= 6}
    # quartered: the first and fourth quarters bare (argent), the second and third ruled upright (gules)
    for (x, y) in field:
        if (x > 37) != (y > 33) and x % 2 == 0:
            p.put(x, y, M)
    p.wash({k for k in field if fs(*k) > 0.32}, 0.18)
    p.hatch(field, D, 3, 'd', field=fs, t=0.6, jit=0.1)
    p.hatch(field, K, 3, 'a', 1, field=fs, t=0.9, jit=0.06)
    for (x, y) in field:
        if x == 37 or y == 33:
            p.put(x, y, K)
    # the iron rim: outer and inner lines, the band between lit on the upper left, darker round the right
    for (x, y), d in dep.items():
        f = fs(x, y)
        if d in (1, 5):
            p.put(x, y, K)
        elif d == 2:
            p.put(x, y, None if f < 0.3 else (L if f < 0.6 else M))
        elif d == 3:
            p.put(x, y, (F if (x + y) % 3 == 0 else None) if f < 0.3 else (L if f < 0.6 else M))
        elif d == 4:
            p.put(x, y, L if f < 0.3 else (M if f < 0.6 else D))
    # rivets round the rim, every few pixels along it
    ring3 = sorted((k for k, d in dep.items() if d == 3), key=lambda k: math.atan2(k[1] - 36, k[0] - cx))
    for i, (x, y) in enumerate(ring3):
        if i % 9 == 4:
            f = fs(x, y)
            p.put(x, y, K if f < 0.6 else None)
    # the shadow side: a second line inside the right and lower edges
    for (x, y), d in dep.items():
        if d == 1 and fs(x, y) > 0.45:
            for a_, b in ((-1, 0), (0, -1)):
                if dep.get((x + a_, y + b)) == 2:
                    p.put(x + a_, y + b, D)
    # nicks bitten from the rim at the upper right corner and on the left side
    for (x, y) in ((51, 14), (52, 14), (53, 14), (52, 15)):
        p.put(x, y, None)
    for (x, y) in ((50, 14), (51, 15), (52, 16), (53, 15), (54, 14)):
        p.put(x, y, K)
    for (x, y) in ((19, 41), (19, 42), (20, 42)):
        p.put(x, y, None)
    for (x, y) in ((19, 40), (20, 41), (21, 42), (20, 43), (19, 43)):
        p.put(x, y, K)
    return p.s


ASSETS['ink-skirmish'] = skirmish_drawing


# ---------------------------------------------------------------- campaign: the keep on its hill
def campaign_drawing():
    """a keep on its hill: a square tower in coursed stone with arrow loops, a corbelled parapet and a gate with
    its portcullis down, the right face in cross-hatched shadow; curtain walls running out to two round towers
    with shingled conical roofs; a pennant from the keep; the hill in contour hatching with a road winding up
    to the gate, trees, a rock, grass; two birds in the sky"""
    W, H = 80, 66
    p = Ink(W, H, seed=9)

    def hy(x):
        return 51.0 - 10.0 * math.exp(-((x - 40) / 30.0) ** 4)

    def base(x):
        return round(hy(x)) + 1
    # ---- the sky: two birds
    for (bx, by) in ((24, 9), (29, 5)):
        p.put(bx - 2, by - 1, M); p.put(bx - 1, by, K); p.put(bx, by + 1, K); p.put(bx + 1, by, K); p.put(bx + 2, by - 1, M)
    # ---- the hill: the crest line, contour hatching round its shadow side, grass on the lit side
    for x in range(W):
        p.put(x, round(hy(x)), K if 6 < x < W - 6 else M)
    for k in range(1, 6):
        for x in range(46, W):
            y = round(hy(x) + 2.4 * k + (x - 44) * 0.04 * k)
            if y < H and x > 46 + k * 4 and (x * 7 + k * 3) % 11 != 0:
                p.put(x, y, M if k < 4 else L)
    hill = {(x, y) for x in range(W) for y in range(H) if y > hy(x) + 1}
    p.hatch({(x, y) for (x, y) in hill if x > 62 and y > hy(x) + 10}, L, 3, 'd', field=lambda x, y: (x - 62) / 18.0, t=0.15, jit=0.25)
    for (gx, gy_) in ((5, 55), (14, 57), (3, 61), (17, 63), (31, 63), (37, 57), (9, 64), (45, 63), (30, 47), (12, 51)):
        p.put(gx, gy_, M); p.put(gx + 1, gy_ - 1, L); p.put(gx - 1, gy_ - 1, L)
    # ---- the road winding up to the gate: two ruled edges, ruts down the middle, a few stones
    path = [(39, 43), (36, 46), (32, 49), (29, 52), (28, 55), (25, 58), (19, 61), (11, 64), (5, 66)]
    pts = []
    for (x0, y0), (x1, y1) in zip(path, path[1:]):
        pts += seg(x0, y0, x1, y1)[:-1]
    for i, (x, y) in enumerate(pts):
        hw_ = 2 + (y - 43) // 7
        p.put(x - hw_, y, M)
        p.put(x + hw_, y, M if i % 7 else L)
        if i % 3 != 2 and y > 47:
            p.put(x - 1, y, F); p.put(x + 1, y, F)
    for (sx, sy) in ((35, 48), (29, 54), (22, 60), (14, 63)):
        p.put(sx, sy, L)
    # ---- firs in tiers: the lit half lightly ruled, the shadow half in close rows, a stub of trunk
    for (fx, top, h) in ((8, 45, 13), (14, 50, 9), (56, 51, 8), (70, 47, 14), (75, 53, 8)):
        fir = {(x, y) for y in range(top, top + h) for x in range(fx - 7, fx + 8)
               if abs(x - fx) <= (y - top) * 0.42 + (1 if (y - top) % 4 == 3 else 0)}
        p.knock({(x + a_, y + b) for (x, y) in fir for a_, b in N4})
        p.hatch({q for q in fir if q[0] > fx}, D, 2, 'h', top % 2)
        p.hatch({q for q in fir if q[0] < fx}, L, 4, 'a', 0)
        p.outline(fir, K)
        p.pts({(fx, y) for y in range(top + h, top + h + 2)}, K)
    # a rock on the slope, lit on top, its underside hatched
    rock = poly([(18, 50), (20, 47), (24, 47), (26, 50)])
    p.knock(rock); p.hatch({q for q in rock if q[1] >= 49}, M, 2, 'v', 0); p.outline(rock, K)
    # ---- the curtain walls: coursed stone under a crenellated walk, hatched toward their feet
    for (x0, x1) in ((16, 32), (52, 64)):
        wall = {(x, y) for x in range(x0, x1) for y in range(31, base(x) + 1)}
        merl = set()
        for x in range(x0 + 1, x1 - 1, 4):
            merl |= rect(x, 28, 2, 3)
        p.knock(wall | merl)
        for y in range(34, 50, 3):
            p.line(x0 + 1, y, x1 - 2, y, L, dash=4, gap=1)
        for (x, y) in wall:
            if (y - 34) % 3 == 1 and (x + ((y - 34) // 3) * 3) % 6 == 0 and y < base(x) - 1:
                p.put(x, y, L)
        p.hatch({q for q in wall if q[1] > base(q[0]) - 4}, M, 3, 'd')
        p.outline(wall | merl, K)
        for x in range(x0 + 1, x1 - 1):
            p.put(x, 32, D if x % 2 else M)
    # ---- the round towers: drums shaded across their curve, courses bowing with it, shingled cones
    for tcx in (12, 68):
        body = {(x, y) for x in range(tcx - 5, tcx + 6) for y in range(24, base(x) + 1)}
        p.knock(body)
        for (x, y) in body:
            f = (x - tcx) / 5.5
            c = D if f > 0.7 else ((M if y % 3 else None) if f > 0.4 else ((L if y % 2 else None) if f > 0.1 else None))
            if c:
                p.put(x, y, c)
        p.wash({q for q in body if (q[0] - tcx) / 5.5 > 0.3}, 0.2)
        for y0 in range(28, 48, 4):
            for x in range(tcx - 4, tcx + 2):
                p.put(x, y0 + round(0.8 * (1 - ((x - tcx) / 5.5) ** 2)), L)
        p.outline(body, K, heavy=D)
        p.line(tcx - 1, 31, tcx - 1, 35, K); p.put(tcx - 2, 33, K); p.put(tcx, 33, K)
        roof = poly([(tcx - 7, 24), (tcx, 9), (tcx + 7, 24)])
        p.knock(roof)
        for (x, y) in roof:
            if (y - 9) % 3 == 0 and (x + y // 3) % 2 == 0:
                p.put(x, y, L)
        p.wash({q for q in roof if q[0] > tcx}, 0.22)
        p.hatch({q for q in roof if q[0] > tcx}, D, 3, 'd', field=lambda x, y, tcx=tcx: (x - tcx) / 7.0, t=0.15, jit=0.1)
        p.outline(roof, K, heavy=D)
        for x in range(tcx - 5, tcx + 6):
            p.put(x, 25, D if x % 2 else M)
        p.line(tcx, 9, tcx, 4, K)
        p.pts([(tcx + 1, 4), (tcx + 2, 4), (tcx + 3, 5), (tcx + 1, 5), (tcx + 2, 5), (tcx + 1, 6)], M)
    # ---- the keep
    side = {(x, y) for x in range(48, 53) for y in range(16 - (x - 47) // 2, base(x) + 1 - (x - 47) // 2)}
    smerl = rect(49, 12, 2, 3) | rect(52, 11, 1, 3)
    p.knock(side | smerl)
    p.wash(side, 0.3)
    p.hatch(side, M, 2, 'v', 1)
    p.hatch(side, D, 3, 'd', 0)
    p.outline(side | smerl, K)
    parapet = rect(30, 16, 18, 4)
    merl = set()
    for x in range(30, 48, 4):
        merl |= rect(x, 12, 2, 4)
    wall = {(x, y) for x in range(31, 47) for y in range(20, base(x) + 1)}
    p.knock(parapet | merl | wall)
    p.outline(parapet | merl, K)
    for x in range(31, 47):
        p.put(x, 17, L if x % 4 == 1 else None)
        p.put(x, 20, K if x % 3 == 0 else D)
        p.put(x, 21, K if x % 3 == 0 else (M if x % 3 == 1 else None))
    for y in range(24, 44, 3):
        p.line(32, y, 45, y, L, dash=4, gap=1)
        for x in range(32, 46):
            if (x + ((y - 24) // 3) * 3) % 6 == 0 and y + 1 < base(x):
                p.put(x, y + 1, L)
    p.hatch({q for q in wall if q[1] > base(q[0]) - 4}, M, 3, 'd')
    p.wash({q for q in wall if q[0] > 43}, 0.14)
    p.outline(wall, K)
    for x in range(31, 47):
        p.put(x, 19, K)
    # arrow loops, cross-shaped
    for lx in (33, 44):
        p.line(lx, 23, lx, 28, K); p.put(lx - 1, 25, K); p.put(lx + 1, 25, K)
    # an arched window with its sill
    win = rect(37, 26, 4, 5) | {(38, 25), (39, 25)}
    p.pts(win, D); p.outline(win, K)
    p.line(36, 31, 41, 31, L)
    # the gate, its portcullis down
    gate = {(x, y) for x in range(35, 43) for y in range(34, base(x) + 1)} - {(35, 34), (42, 34)}
    p.pts(gate, D)
    for (x, y) in gate:
        if x in (36, 38, 40) or y in (37, 40):
            p.put(x, y, K)
    p.outline(gate, K)
    p.line(34, 33, 43, 33, M, dash=2, gap=1)
    # the pennant, ruled upright like the shields
    p.line(38, 11, 38, 2, K)
    pen = poly([(39, 2), (50, 3), (46, 5), (50, 7), (39, 6)])
    p.knock(pen)
    p.hatch(pen, M, 2, 'v', 0)
    p.outline(pen, K)
    return p.s


ASSETS['ink-campaign'] = campaign_drawing
