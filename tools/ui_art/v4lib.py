"""v4 pixel painter. Small, explicit primitives for hand-shaded GUI art authored at GUI scale 1
(one pixel here = one Minecraft GUI pixel; render.py doubles the page nearest-neighbour).
Every colour comes from a named ramp, light comes from the top-left, shading is done in clusters."""
import math, random
from PIL import Image


def rgba(c, a=None):
    if c is None:
        return (0, 0, 0, 0)
    if isinstance(c, tuple):
        if len(c) == 3:
            return (c[0], c[1], c[2], 255 if a is None else a)
        return c if a is None else (c[0], c[1], c[2], a)
    c = c.lstrip('#')
    r, g, b = int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16)
    al = int(c[6:8], 16) if len(c) == 8 else 255
    return (r, g, b, al if a is None else a)


def mix(c1, c2, t):
    a, b = rgba(c1), rgba(c2)
    return '#%02x%02x%02x' % tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


BAYER4 = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def dith(x, y, t):
    """ordered dither: True on a share t (0..1) of pixels, in the Bayer 4x4 pattern"""
    return (BAYER4[y & 3][x & 3] + 0.5) / 16.0 < t


class Noise:
    """seeded 2D value noise, cosine-interpolated; returns 0..1"""
    def __init__(self, seed, cell=16, celly=None):
        self.r = random.Random(seed); self.cell = cell; self.celly = celly or cell; self.g = {}

    def _v(self, i, j):
        k = (i, j)
        if k not in self.g:
            self.g[k] = random.Random(hash((i, j, self.r.random())) & 0xffffffff).random()
        return self.g[k]

    def __call__(self, x, y):
        fx, fy = x / self.cell, y / self.celly
        i, j = math.floor(fx), math.floor(fy); tx, ty = fx - i, fy - j
        sx = (1 - math.cos(tx * math.pi)) / 2; sy = (1 - math.cos(ty * math.pi)) / 2
        a = self._v(i, j) + (self._v(i + 1, j) - self._v(i, j)) * sx
        b = self._v(i, j + 1) + (self._v(i + 1, j + 1) - self._v(i, j + 1)) * sx
        return a + (b - a) * sy


class Spr:
    def __init__(self, w, h, fill=None):
        self.w, self.h = w, h
        self.im = Image.new('RGBA', (w, h), rgba(fill) if fill else (0, 0, 0, 0))
        self.px = self.im.load()

    def inb(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def set(self, x, y, c):
        if self.inb(x, y):
            self.px[x, y] = rgba(c)

    def get(self, x, y):
        return self.px[x, y] if self.inb(x, y) else (0, 0, 0, 0)

    def opaque(self, x, y):
        return self.inb(x, y) and self.px[x, y][3] > 0

    def over(self, x, y, c, a=None):
        """composite a (translucent) colour onto the pixel"""
        if not self.inb(x, y):
            return
        s = rgba(c, a)
        d = self.px[x, y]
        sa = s[3] / 255.0; da = d[3] / 255.0
        oa = sa + da * (1 - sa)
        if oa <= 0:
            return
        out = tuple(round((s[i] * sa + d[i] * da * (1 - sa)) / oa) for i in range(3))
        self.px[x, y] = out + (round(oa * 255),)

    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c)

    def rect_over(self, x, y, w, h, c, a=None):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.over(xx, yy, c, a)

    def frame(self, x, y, w, h, c):
        self.hline(x, x + w - 1, y, c); self.hline(x, x + w - 1, y + h - 1, c)
        self.vline(x, y, y + h - 1, c); self.vline(x + w - 1, y, y + h - 1, c)

    def hline(self, x0, x1, y, c):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            self.set(x, y, c)

    def vline(self, x, y0, y1, c):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            self.set(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy; x0 += sx
            if e2 <= dx:
                err += dx; y0 += sy

    def ellipse(self, cx, cy, rx, ry, c, fill=True):
        """pixel ellipse centred on (cx, cy) (may be .5 for even sizes)"""
        for y in range(math.floor(cy - ry) - 1, math.ceil(cy + ry) + 2):
            for x in range(math.floor(cx - rx) - 1, math.ceil(cx + rx) + 2):
                d = ((x - cx) / (rx + 0.5)) ** 2 + ((y - cy) / (ry + 0.5)) ** 2
                if d <= 1.0:
                    if fill:
                        self.set(x, y, c)
                    else:
                        # edge pixel: inside, but a 4-neighbour is outside
                        out = any(((x + dx - cx) / (rx + 0.5)) ** 2 + ((y + dy - cy) / (ry + 0.5)) ** 2 > 1.0
                                  for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                        if out:
                            self.set(x, y, c)

    def poly(self, pts, c):
        """scanline fill of a polygon given in pixel-centre coordinates"""
        ys = [p[1] for p in pts]
        for y in range(math.floor(min(ys)), math.ceil(max(ys)) + 1):
            yc = y + 0.0
            xs = []
            n = len(pts)
            for i in range(n):
                (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
                if (y0 <= yc < y1) or (y1 <= yc < y0):
                    xs.append(x0 + (yc - y0) * (x1 - x0) / (y1 - y0))
            xs.sort()
            for k in range(0, len(xs) - 1, 2):
                for x in range(math.ceil(xs[k] - 0.0001), math.floor(xs[k + 1] + 0.0001) + 1):
                    self.set(x, y, c)

    def blit(self, o, x, y, alpha=1.0):
        for yy in range(o.h):
            for xx in range(o.w):
                p = o.px[xx, yy]
                if p[3]:
                    if p[3] == 255 and alpha >= 1.0:
                        self.set(x + xx, y + yy, p)
                    else:
                        self.over(x + xx, y + yy, p[:3], round(p[3] * alpha))

    def grid(self, x, y, rows, pal):
        """character grid: one letter per pixel, '.' or ' ' is empty"""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in '. ':
                    continue
                c = pal.get(ch)
                if c is not None:
                    if isinstance(c, tuple) and len(c) == 2 and isinstance(c[1], float):
                        self.over(x + i, y + j, c[0], round(c[1] * 255))
                    else:
                        self.set(x + i, y + j, c)

    def mask(self):
        return {(x, y) for y in range(self.h) for x in range(self.w) if self.px[x, y][3] > 0}

    def outline(self, c, diag=False):
        m = self.mask(); add = set()
        nb = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (1, -1), (-1, 1), (-1, -1)] if diag else [])
        for (x, y) in m:
            for dx, dy in nb:
                k = (x + dx, y + dy)
                if k not in m and self.inb(*k):
                    add.add(k)
        for k in add:
            self.set(k[0], k[1], c)

    def shadow(self, dx, dy, a=0.45, c='#000000'):
        """drop shadow of the opaque shape, offset (dx, dy), composited under the existing pixels"""
        m = self.mask()
        sh = Spr(self.w, self.h)
        for (x, y) in m:
            if (x + dx, y + dy) not in m:
                sh.set(x + dx, y + dy, rgba(c, round(a * 255)))
        sh.blit(self, 0, 0)
        self.im = sh.im; self.px = sh.px

    def crop(self, x, y, w, h):
        o = Spr(w, h)
        o.im = self.im.crop((x, y, x + w, y + h)); o.px = o.im.load()
        return o

    def save(self, path):
        self.im.save(path)


def emboss(spr, mask, ox, oy, hi, lo, base=None, rim=None):
    """press a shape (set of (x, y)) into a surface: light catches the lower-right inner edge of a recess
    when light comes from the top-left, so the top-left inner edge goes dark and the lower-right bright.
    For a raised shape pass hi and lo swapped."""
    for (x, y) in mask:
        X, Y = ox + x, oy + y
        if base:
            spr.set(X, Y, base)
        if (x - 1, y - 1) not in mask or (x - 1, y) not in mask or (x, y - 1) not in mask:
            spr.set(X, Y, lo)
        elif (x + 1, y + 1) not in mask or (x + 1, y) not in mask or (x, y + 1) not in mask:
            spr.set(X, Y, hi)
    if rim:
        for (x, y) in mask:
            for dx, dy in ((1, 0), (0, 1), (1, 1)):
                k = (x + dx, y + dy)
                if k not in mask:
                    spr.set(ox + k[0], oy + k[1], rim)


def grid_mask(rows, ch=None):
    return {(i, j) for j, r in enumerate(rows) for i, c in enumerate(r) if (c not in '. ' if ch is None else c in ch)}
