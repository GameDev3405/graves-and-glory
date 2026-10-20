"""Shared pixel-art helpers for the title sprites. Everything is authored at GUI scale 1: one pixel here is one GUI
pixel, and the page doubles it nearest-neighbour, so each lands on screen as a 2x2 block.

A sprite is a code map: each pixel holds a (material, tone) code such as "S1" plus a part id. Palettes turn codes
into colours, so the day and night sprites are one drawing with two palettes."""
import math, os
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "sprites")


def hexrgb(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.code = np.full((h, w), "", dtype=object)
        self.part = np.zeros((h, w), dtype=np.int16)

    def put(self, x, y, code, part):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.code[y, x] = code
            self.part[y, x] = part

    def get(self, x, y):
        return self.code[y, x] if (0 <= x < self.w and 0 <= y < self.h) else ""

    def pid(self, x, y):
        return self.part[y, x] if (0 <= x < self.w and 0 <= y < self.h) else 0

    def fill(self, pts, code, part):
        """fill a polygon given by hand-placed vertices"""
        for (x, y) in poly_mask(pts):
            self.put(x, y, code, part)

    def fill_mask(self, mask, code, part):
        for (x, y) in mask:
            self.put(x, y, code, part)

    def line(self, pts, code, part, only=None):
        """a 1 px polyline through hand-placed points; with only=a set of part ids, it paints only over those"""
        for (x, y) in polyline_px(pts):
            if only is None or self.pid(x, y) in only:
                self.put(x, y, code, part if only is None else self.pid(x, y))

    def grid(self, x0, y0, rows, key, part, only=None):
        """paint a hand-typed grid; key maps characters to codes; '.' and ' ' are transparent"""
        for j, r in enumerate(rows):
            for i, ch in enumerate(r):
                if ch in key and (only is None or self.pid(x0 + i, y0 + j) in only):
                    self.put(x0 + i, y0 + j, key[ch], part if only is None else self.pid(x0 + i, y0 + j))

    def render(self, pal):
        im = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        px = im.load()
        for y in range(self.h):
            for x in range(self.w):
                c = self.code[y, x]
                if c:
                    px[x, y] = hexrgb(pal[c]) + (255,)
        return im


def poly_mask(pts):
    """pixels whose centres fall inside the polygon (even-odd)"""
    ys = [p[1] for p in pts]
    out = set()
    for y in range(int(math.floor(min(ys))), int(math.ceil(max(ys))) + 1):
        yc = y + 0.5
        cut = []
        n = len(pts)
        for i in range(n):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
            if (y0 <= yc < y1) or (y1 <= yc < y0):
                cut.append(x0 + (yc - y0) * (x1 - x0) / (y1 - y0))
        cut.sort()
        for k in range(0, len(cut) - 1, 2):
            for x in range(int(math.ceil(cut[k] - 0.5)), int(math.floor(cut[k + 1] - 0.5)) + 1):
                out.add((x, y))
    return out


def polyline_px(pts):
    out = []
    for (a, b) in zip(pts, pts[1:]):
        n = max(abs(b[0] - a[0]), abs(b[1] - a[1])) or 1
        for i in range(n + (1 if b == pts[-1] else 0)):
            p = (round(a[0] + (b[0] - a[0]) * i / n), round(a[1] + (b[1] - a[1]) * i / n))
            if not out or out[-1] != p:
                out.append(p)
    return out


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name))
