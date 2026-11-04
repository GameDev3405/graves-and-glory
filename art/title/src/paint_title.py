"""Painted title scene: Tidecallers meet Graveborn at stormy dusk (board 4F.3).

Kort's brief (2026-11-03): full painterly key art, Graveborn vs Tidecallers, the lines colliding, Hollow King vs
Admiral Vael at the point of impact, Tidecallers from the left and Graveborn from the right, stormy dusk, logo top and
menu bottom-left, Dark Souls / Darkest Dungeon / Total War references, figures with a hint of blocks, 2560x1440.

Two passes:
  1. scene(): a flat composite (sky, ground, armies, heroes, light) authored in 2560x1440 px;
  2. paint(): a stroke-based repaint of that composite (coarse to fine brushes that follow the image's
     structure), then canvas grain, a soft impasto light and a final grade.

Usage: python paint_title.py VARIANT OUT.png [--base BASE.png] [--fast]
"""
import math
import sys

import cv2
import numpy as np

W, H = 2560, 1440


def hexc(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) / 255 for i in (0, 2, 4)], np.float32)


def mix(a, b, t):
    return a + (b - a) * t


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def fbm(h, w, seed, octaves=6, base=3, persist=0.55, aspect=1.0):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        gh = max(2, int(base * 2 ** o))
        gw = max(2, int(base * 2 ** o * w / h / aspect))
        g = rng.random((gh, gw)).astype(np.float32)
        out += amp * cv2.resize(g, (w, h), interpolation=cv2.INTER_CUBIC)
        tot += amp
        amp *= persist
    return out / tot


# ---------------------------------------------------------------- palettes (ART-STYLE-GUIDE 10.1, 10.5, 10.6)
VIOLET = [hexc(c) for c in ('#1E1528', '#362645', '#54406A', '#7A6494', '#A28FBA', '#D2C6D8')]
SEA = [hexc(c) for c in ('#0A1426', '#11264A', '#1A3F6A', '#2A5E8A', '#5E88A6', '#9DB8C4')]
NAVY = [hexc(c) for c in ('#0E1320', '#18202F', '#232E42', '#33425A', '#4E6078')]
VERD = [hexc(c) for c in ('#16292A', '#24433F', '#3A6458', '#5E8A7A', '#8EB2A2')]
CORAL = [hexc(c) for c in ('#857A6A', '#A99F8E', '#C7BFAD', '#DCD6C6')]
SKIN_TIDE = [hexc(c) for c in ('#3A4A44', '#50645C', '#6A8278', '#84A094', '#A2BAAE')]
GILT = hexc('#96783A')
GILT_HI = hexc('#D8B266')
BONE = hexc('#C9C1A8')
GRAVE_IRON = hexc('#2B2826')
RUST = hexc('#6B4A33')
SOUL = hexc('#CFEFFF')
KELP = hexc('#2E4630')

VARIANTS = {
    # A: the sun sits low behind the clash; the weapons cross over the bright slit of sky.
    'A': dict(sun=(1440, 770), hor=812, clash=1415, storm=0.0, rain=False, warm=1.0),
    # B: the sun breaks through upper right; lightning far left over the Tidecallers' rear; rain curtains.
    'B': dict(sun=(1980, 640), hor=800, clash=1360, storm=1.0, rain=True, warm=0.85),
    # C: colder: the sun is almost gone, a thin ember line on the horizon; soul-light and sparks carry the warmth.
    'C': dict(sun=(1300, 815), hor=815, clash=1400, storm=0.5, rain=True, warm=0.55),
}


class Scene:
    def __init__(self, v):
        self.v = v
        self.sx, self.sy = v['sun']
        self.hor = v['hor']
        self.img = np.zeros((H, W, 3), np.float32)
        self.glow = np.zeros((H, W, 3), np.float32)
        self.rng = np.random.default_rng(1103)

    # ---------------------------------------------------------------- light helpers
    def sun_dir(self, x, y):
        dx, dy = self.sx - x, self.sy - y
        n = math.hypot(dx, dy) + 1e-6
        return dx / n, dy / n

    def sun_strength(self, x, y):
        d = math.hypot(self.sx - x, (self.sy - y) * 1.6)
        return self.v['warm'] * (0.35 + 0.95 * math.exp(-d / 900))

    def haze(self, x, y):
        t = min(1.0, abs(x - self.sx) / 1300)
        near = mix(hexc('#E08850'), hexc('#B46A48'), 1 - self.v['warm'])
        return mix(near, hexc('#5A3A44'), t)

    # ---------------------------------------------------------------- sky
    def sky(self):
        v = self.v
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        t = np.clip(yy / self.hor, 0, 1.2)
        stops = [(0.0, '#0D0A14'), (0.4, '#241A2A'), (0.7, '#4E2E36'), (0.88, '#A2553A'), (1.0, '#F0A458')]
        sky = np.zeros((H, W, 3), np.float32)
        for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
            m = ((t >= t0) & (t <= t1))[..., None]
            k = ((t - t0) / (t1 - t0))[..., None]
            sky = np.where(m, mix(hexc(c0), hexc(c1), np.clip(k, 0, 1)), sky)
        sky = np.where((t > 1.0)[..., None], hexc(stops[-1][1]), sky)
        # away from the sun the low sky cools toward bruised violet
        away = np.clip(np.abs(xx - self.sx) / 1500, 0, 1)[..., None] * smooth(0.45, 1.0, t)[..., None]
        sky = mix(sky, sky * hexc('#7A5A78') * 1.1, away * 0.85)
        sky = mix(sky * hexc('#7A6A88'), sky, v['warm'])
        d = np.hypot(xx - self.sx, (yy - self.sy) * 1.5)
        sky += (hexc('#FFD290') * np.exp(-d / 60)[..., None] * 1.4 +
                hexc('#FF9850') * np.exp(-d / 260)[..., None] * 0.55 +
                hexc('#B05030') * np.exp(-d / 900)[..., None] * 0.35) * v['warm']

        # clouds: heavy banks above, a clear slit just over the horizon where the sun shows
        n = fbm(H, W, 7, octaves=7, base=2, aspect=3.2)
        n2 = fbm(H, W, 11, octaves=5, base=4, aspect=2.0)
        gap = np.exp(-((yy - (self.hor - 70)) / 55) ** 2)
        alt = 0.32 * (1 - t) - 0.55 * gap + 0.08 * v['storm']
        dens = smooth(0.42, 0.66, n + alt)
        dens *= smooth(1.02, 0.9, t)
        dens = cv2.GaussianBlur(dens, (0, 0), 2.0)
        cloud = mix(hexc('#120D16'), hexc('#2E2230'), n2[..., None])
        # lit undersides and edges facing the sun
        ux, uy = self.sun_dir(W / 2, 200)
        M = np.float32([[1, 0, ux * 28], [0, 1, uy * 28]])
        shifted = cv2.warpAffine(dens, M, (W, H), borderMode=cv2.BORDER_REPLICATE)
        edge = np.clip(dens - shifted, 0, 1)
        edge = cv2.GaussianBlur(edge, (0, 0), 3)
        near = np.exp(-d / 950)[..., None]
        cloud += hexc('#FF9A58') * edge[..., None] * near * 3.2 * v['warm']
        cloud += hexc('#6A3036') * smooth(0.5, 1.0, t)[..., None] * 0.6
        sky = mix(sky, cloud, dens[..., None])

        # light shafts: radial blur of the open bright sky from the sun
        bright = np.clip(sky - 0.45, 0, None) * (1 - dens[..., None])
        small = cv2.resize(bright, (W // 4, H // 4), interpolation=cv2.INTER_AREA)
        acc = np.zeros_like(small)
        cx, cy = self.sx / 4, self.sy / 4
        for i in range(28):
            s = 1 + i * 0.045
            M = np.float32([[s, 0, cx - s * cx], [0, s, cy - s * cy]])
            acc += cv2.warpAffine(small, M, (W // 4, H // 4))
        rays = cv2.resize(acc / 28, (W, H), interpolation=cv2.INTER_CUBIC)
        sky += rays * 0.9 * v['warm']

        if v['storm'] > 0.5:  # lightning far left
            self.lightning(sky, 300, 140, self.hor - 40)
        self.img[:] = sky
        self.cloud_dens = dens

    def lightning(self, sky, x0, y0, y1):
        rng = np.random.default_rng(42)
        pts = [(x0, y0)]
        x, y = x0, y0
        while y < y1:
            x += rng.uniform(-28, 22)
            y += rng.uniform(18, 40)
            pts.append((x, y))
        bolt = np.zeros((H, W), np.float32)
        cv2.polylines(bolt, [np.int32(pts)], False, 1.0, 3, cv2.LINE_AA)
        br = [p for p in pts[4:6]]
        bx, by = br[0]
        bp = [(bx, by)]
        for _ in range(5):
            bx += rng.uniform(-40, -10)
            by += rng.uniform(15, 30)
            bp.append((bx, by))
        cv2.polylines(bolt, [np.int32(bp)], False, 0.7, 2, cv2.LINE_AA)
        glow = cv2.GaussianBlur(bolt, (0, 0), 30)
        sky += hexc('#E8E0FF') * bolt[..., None] * 1.2 + hexc('#8070B0') * glow[..., None] * 3.0

    # ---------------------------------------------------------------- ground
    def ground(self):
        hor = self.hor
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        img = self.img
        # far ridges, two layers
        for i, (amp, off, col, k) in enumerate([(26, -6, '#7A4A44', 0.55), (42, 10, '#4A2E34', 0.35)]):
            n = fbm(1, W, 30 + i, octaves=6, base=3)[0]
            ridge = hor + off - amp * n - 10 * np.sin(np.arange(W) / 310 + i)
            m = (yy > ridge[None, :])
            c = mix(hexc(col), self.haze_arr(xx), k)
            img[:] = np.where(m[..., None], c, img)
        # the plain: hazy at the horizon, dark at our feet
        g = np.clip((yy - hor) / (H - hor), 0, 1)
        plain = mix(self.haze_arr(xx) * 0.85, hexc('#161012'), smooth(0.0, 0.55, g)[..., None])
        mud = fbm(H, W, 51, octaves=6, base=3, aspect=5)
        plain *= (0.8 + 0.4 * mud)[..., None]
        # wet ground: puddles catch the sky under the sun
        pud = smooth(0.56, 0.62, fbm(H, W, 52, octaves=5, base=5, aspect=7))
        spread = np.maximum(90 + (yy - hor) * 0.9, 20)
        refl = np.exp(-((xx - self.sx) / spread) ** 2) * (yy > hor + 8) * np.exp(-np.maximum(yy - hor, 0) / 260)
        plain += hexc('#FFB070') * (pud * refl * 1.1 * self.v['warm'])[..., None] * (1 - g * 0.6)[..., None]
        plain += hexc('#FF9050') * (refl * 0.18 * self.v['warm'])[..., None] * (1 - g)[..., None]
        m = (yy > hor + 20 - 12 * fbm(1, W, 33, octaves=4, base=4)[0][None, :])
        img[:] = np.where(m[..., None], plain, img)

    def haze_arr(self, xx):
        t = np.clip(np.abs(xx - self.sx) / 1300, 0, 1)[..., None]
        near = mix(hexc('#E08850'), hexc('#B46A48'), 1 - self.v['warm'])
        return mix(near, hexc('#5A3A44'), t)

    def dust(self):
        """Warm dust and smoke over the clash, thickest at ground level behind the heroes."""
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        n = fbm(H, W, 77, octaves=6, base=3, aspect=1.8)
        band = np.exp(-((yy - (self.hor + 60)) / 170) ** 2)
        centre = np.exp(-((xx - self.v['clash']) / 900) ** 2)
        a = np.clip(band * (0.25 + 0.75 * centre) * smooth(0.35, 0.75, n) * 0.55, 0, 0.6)
        col = self.haze_arr(xx) * 1.05
        self.img[:] = mix(self.img, col, a[..., None])
        # smoke columns, darker, rising from the far field
        for (x, s) in [(560, 1), (2150, 2), (900, 3)]:
            sm = fbm(H, W, 90 + s, octaves=5, base=4, aspect=0.6)
            col_m = np.exp(-((xx - x - (self.hor - yy) * 0.35) / np.maximum(60 + (self.hor - yy) * 0.25, 20)) ** 2)
            col_m *= (yy < self.hor + 20) * smooth(0, 220, self.hor - yy + 200)
            a = np.clip(col_m * smooth(0.4, 0.7, sm) * 0.7, 0, 0.7)
            self.img[:] = mix(self.img, hexc('#1A1418'), a[..., None])

    # ---------------------------------------------------------------- drawing
    def to8(self):
        return np.clip(self.img * 255, 0, 255).astype(np.uint8)

    def from8(self, a8):
        self.img[:] = a8.astype(np.float32) / 255

    @staticmethod
    def col8(c):
        c = np.clip(c, 0, 1) * 255
        return (float(c[0]), float(c[1]), float(c[2]))

    def draw_parts(self, canvas, parts, x, y, h, fog, rim_k=1.0, rim_w=None, dim=1.0, mask=None):
        """parts: (polygon Nx2, colour) in screen px. A rim pass offset toward the sun, then the body."""
        ux, uy = self.sun_dir(x, y - 2 * h)
        rw = rim_w if rim_w is not None else max(1.0, h * 0.10)
        hz = self.haze(x, y)
        st = self.sun_strength(x, y)
        rim = np.clip(hexc('#FFB468') * (0.55 + 0.9 * st) * rim_k, 0, 1)
        rim = mix(rim, hz, fog * 0.4)
        off = np.float32([ux * rw, uy * rw])
        for poly, c, kind in parts:
            if kind != 'b':
                continue
            p = np.int32(np.round((poly + off) * 4))
            cv2.fillPoly(canvas, [p], self.col8(rim), cv2.LINE_AA, shift=2)
        for poly, c, kind in parts:
            if kind == 'glow':
                p = np.int32(np.round(poly * 4))
                cv2.fillPoly(canvas, [p], self.col8(mix(c, hz, fog * 0.5)), cv2.LINE_AA, shift=2)
                cx, cy = poly.mean(axis=0)
                r = max(1, int(h * 0.12))
                cv2.circle(self.glow, (int(cx), int(cy)), r, tuple(float(v) for v in c * (1 - fog) * 0.3), -1)
                continue
            cc = mix(c * dim, hz, fog)
            p = np.int32(np.round(poly * 4))
            cv2.fillPoly(canvas, [p], self.col8(cc), cv2.LINE_AA, shift=2)
            if mask is not None:
                cv2.fillPoly(mask, [p], 1.0, cv2.LINE_AA, shift=2)

    # ---------------------------------------------------------------- armies
    def armies(self):
        canvas = self.to8()
        rng = self.rng
        hor = self.hor
        rows = 22
        figs = []
        for r in range(rows):
            d = (r + 0.5) / rows
            fy = hor + 14 + (1150 - hor) * d ** 1.6
            h = 3.2 + 62 * d ** 1.9
            front = self.v['clash'] + (d - 0.6) * 180
            spacing = h * 1.25
            x = -60 + rng.uniform(0, spacing)
            while x < W + 60:
                side = 'tide' if x < front else 'grave'
                gap = abs(x - front)
                # thin the rear into clusters
                rear = gap / 1500
                if rng.random() < 0.18 + rear * 0.55 * (1 - d):
                    x += spacing * rng.uniform(0.6, 1.4)
                    continue
                y = fy + rng.uniform(-0.4, 0.4) * h
                figs.append((y, x, h, side, d, gap))
                x += spacing * rng.uniform(0.55, 1.25)
        figs.sort()
        for y, x, h, side, d, gap in figs:
            fog = min(0.92, (1 - d) ** 1.4 * 0.95)
            face = 1 if side == 'tide' else -1
            fighting = gap < h * 2.2
            parts = soldier(x, y, h, face, side, rng, fighting)
            rk = 0.12 + 0.75 * math.exp(-abs(x - self.sx) / 480)
            self.draw_parts(canvas, parts, x, y, h, fog, rim_k=rk, dim=mix(0.9, 0.42, d))
        self.from8(canvas)

    def heroes(self):
        canvas = self.to8()
        h = 150
        ax, ay = self.v['clash'] - 250, 1255
        kx, ky = self.v['clash'] + 260, 1268
        Scene.weapon_contact = (self.v['clash'] + 20, self.sy - 95)
        for fx, fy, w in ((ax - 40, ay, 190), (kx + 10, ky, 230)):  # long shadows toward us
            sh = np.float32([(fx - w / 2, fy - 8), (fx + w / 2, fy - 8), (fx + w * 0.9, H + 40), (fx - w * 0.9, H + 40)])
            m = np.zeros((H, W), np.uint8)
            cv2.fillPoly(m, [np.int32(sh)], 255, cv2.LINE_AA)
            m = cv2.GaussianBlur(m.astype(np.float32) / 255, (0, 0), 18)
            canvas = (canvas.astype(np.float32) * (1 - 0.55 * m[..., None])).astype(np.uint8)
        mask = np.zeros((H, W), np.float32)
        self.draw_parts(canvas, admiral(ax, ay, 140), ax, ay, 140, 0.0, rim_k=1.1, rim_w=4, dim=0.62, mask=mask)
        self.draw_parts(canvas, hollow_king(kx, ky, h), kx, ky, h, 0.0, rim_k=1.1, rim_w=4, dim=0.62, mask=mask)
        self.from8(canvas)
        # form: the figures darken toward the ground and take a mottled, worn surface
        yy = np.arange(H, dtype=np.float32)[:, None]
        shade = 0.55 + 0.6 * smooth(1300, 650, yy)
        mott = 0.88 + 0.24 * fbm(H, W, 12, octaves=6, base=20)
        k = mask[..., None]
        self.img[:] = self.img * (1 - k) + self.img * k * (shade * mott)[..., None]
        # soul-light: the Hollow King's ribcage and eyes, the Admiral's sea-foam eyes
        self.add_glow((kx - 0.15 * h, ky - 2.45 * h), 22, SOUL * 0.7 + VIOLET[4] * 0.4, 0.8)
        self.add_glow((kx - 0.15 * h, ky - 2.45 * h), 120, VIOLET[4], 0.55)
        for ex in (-0.42, -0.05):
            self.add_glow((kx + ex * h * 0.9, ky - 3.62 * h), 5, SOUL, 1.4)
        # sparks where the trident catches the greatsword
        self.sparks(self.weapon_contact)

    def add_glow(self, c, r, col, k):
        g = np.zeros((H, W), np.float32)
        cv2.circle(g, (int(c[0]), int(c[1])), max(1, int(r * 0.4)), 1.0, -1, cv2.LINE_AA)
        g = cv2.GaussianBlur(g, (0, 0), r * 0.6)
        g /= max(g.max(), 1e-6)
        self.glow += (g * k)[..., None] * col

    def sparks(self, c):
        rng = np.random.default_rng(9)
        cx, cy = c
        layer = np.zeros((H, W), np.float32)
        for i in range(70):
            a = rng.uniform(-math.pi, math.pi * 0.15)
            ln = rng.uniform(20, 210) * rng.random() ** 0.5
            r0 = rng.uniform(4, 30)
            p0 = (cx + math.cos(a) * r0, cy + math.sin(a) * r0)
            p1 = (cx + math.cos(a) * (r0 + ln), cy + math.sin(a) * (r0 + ln) + ln * ln * 0.0015)
            cv2.line(layer, (int(p0[0] * 4), int(p0[1] * 4)), (int(p1[0] * 4), int(p1[1] * 4)),
                     rng.uniform(0.5, 1.0), 1 + int(rng.random() < 0.3), cv2.LINE_AA, shift=2)
        self.glow += layer[..., None] * hexc('#FFD890') * 1.4
        self.glow += cv2.GaussianBlur(layer, (0, 0), 6)[..., None] * hexc('#FF8030') * 2.5
        self.add_glow(c, 60, hexc('#FFE0A0'), 1.3)

    def embers(self):
        rng = np.random.default_rng(5)
        layer = np.zeros((H, W), np.float32)
        for i in range(260):
            x = rng.normal(self.v['clash'], 520)
            y = rng.uniform(380, self.hor + 300)
            # keep the lockup and the menu clear
            if 560 < x < 2000 and y < 340:
                continue
            if x < 700 and y > 980:
                continue
            ln = rng.uniform(3, 14)
            a = -math.pi / 2 + rng.uniform(0.2, 0.9)
            p1 = (x + math.cos(a) * ln, y + math.sin(a) * ln)
            cv2.line(layer, (int(x * 4), int(y * 4)), (int(p1[0] * 4), int(p1[1] * 4)),
                     rng.uniform(0.3, 1.0), 2, cv2.LINE_AA, shift=2)
        self.glow += layer[..., None] * hexc('#FFA850') * 0.9
        self.glow += cv2.GaussianBlur(layer, (0, 0), 4)[..., None] * hexc('#FF6020') * 1.2

    def rain(self):
        rng = np.random.default_rng(3)
        layer = np.zeros((H, W), np.float32)
        for i in range(4200):
            x, y = rng.uniform(-200, W), rng.uniform(0, H)
            ln = rng.uniform(18, 46)
            cv2.line(layer, (int(x), int(y)), (int(x + ln * 0.28), int(y + ln)), rng.uniform(0.2, 0.6), 1,
                     cv2.LINE_AA)
        layer = cv2.GaussianBlur(layer, (0, 0), 0.7)
        self.img[:] = mix(self.img, hexc('#9A90A8'), (layer * 0.22)[..., None])

    def foreground(self):
        """Dark ground under the menu (bottom-left) and broken ground bottom-right, both calm."""
        canvas = self.to8()
        n = fbm(1, W, 61, octaves=6, base=4)[0]
        xs = np.arange(0, 900, 6)
        top = 1010 + (xs / 900) ** 1.8 * 520 - 40 * n[xs] + 14 * np.sin(xs / 40)
        poly = np.concatenate([np.stack([xs, top], 1), [[900, H], [0, H]]]).astype(np.float32)
        lip = poly.copy()
        lip[:len(xs), 1] -= 4
        cv2.fillPoly(canvas, [np.int32(lip[:len(xs) + 2])], self.col8(hexc('#6A3A2E')), cv2.LINE_AA)
        cv2.fillPoly(canvas, [np.int32(poly)], self.col8(hexc('#0B080B')), cv2.LINE_AA)
        xs = np.arange(1900, W + 6, 6)
        top = 1375 - ((xs - 1900) / 660) ** 1.5 * 150 - 25 * n[np.clip(xs, 0, W - 1)]
        poly = np.concatenate([np.stack([xs, top], 1), [[W, H], [1900, H]]]).astype(np.float32)
        lip = poly.copy()
        lip[:len(xs), 1] -= 4
        lip[:len(xs), 0] -= 3
        cv2.fillPoly(canvas, [np.int32(lip)], self.col8(hexc('#7A4630')), cv2.LINE_AA)
        cv2.fillPoly(canvas, [np.int32(poly)], self.col8(hexc('#0C090C')), cv2.LINE_AA)
        # a broken trident and a fallen grave-sword stuck in the bottom-right rise
        for parts in debris():
            for poly, c, _ in parts:
                cv2.fillPoly(canvas, [np.int32(poly * 4)], self.col8(c), cv2.LINE_AA, shift=2)
        self.from8(canvas)

    def grade(self):
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        img = self.img + self.glow
        # vignette, heavier at the bottom corners
        d = np.hypot((xx - W * 0.55) / (W * 0.62), (yy - H * 0.52) / (H * 0.62))
        img *= (1 - 0.55 * smooth(0.55, 1.25, d))[..., None]
        # bloom
        b = np.clip(img - 0.75, 0, None)
        img += cv2.GaussianBlur(b, (0, 0), 18) * 0.6 + cv2.GaussianBlur(b, (0, 0), 60) * 0.4
        # split tone: violet shadows, warm highlights
        lum = (img @ np.float32([0.299, 0.587, 0.114]))[..., None]
        img = img + (hexc('#2A1A3C') - 0.15) * 0.18 * (1 - smooth(0.0, 0.35, lum))
        img = img / (1 + img * 0.18)  # soft shoulder
        self.img = np.clip(img * 1.12, 0, 1)

    def build(self):
        self.sky()
        self.ground()
        self.dust()
        self.armies()
        self.heroes()
        if self.v['rain']:
            self.rain()
        self.foreground()
        self.embers()
        return self


# ================================================================ figures
def limb(p0, p1, w0, w1=None):
    w1 = w0 if w1 is None else w1
    p0, p1 = np.float32(p0), np.float32(p1)
    d = p1 - p0
    n = np.float32([-d[1], d[0]]) / (np.hypot(*d) + 1e-6)
    return np.stack([p0 + n * w0 / 2, p1 + n * w1 / 2, p1 - n * w1 / 2, p0 - n * w0 / 2])


def box(c, w, hgt, ang=0.0):
    cx, cy = c
    ca, sa = math.cos(ang), math.sin(ang)
    pts = [(-w / 2, -hgt / 2), (w / 2, -hgt / 2), (w / 2, hgt / 2), (-w / 2, hgt / 2)]
    return np.float32([(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in pts])


def ragged(p_left, p_right, depth, n, rng):
    """A ragged hem from p_left to p_right hanging down by up to depth."""
    pts = []
    for i in range(n + 1):
        t = i / n
        x = p_left[0] + (p_right[0] - p_left[0]) * t
        y = p_left[1] + (p_right[1] - p_left[1]) * t
        pts.append((x, y + (depth * rng.uniform(0.3, 1.0) if i % 2 else depth * 0.15)))
    return pts


def soldier(x, y, h, face, side, rng, fighting):
    """A crowd soldier, Minecraft proportions (head h, body 1.5h, legs 1.5h), sheared by its lean."""
    lean = rng.uniform(0.08, 0.32) + (0.15 if fighting else 0)

    def P(lx, ly):  # lx forward, ly up, in heads
        return (x + face * (lx + ly * lean) * h, y - ly * h)

    parts = []
    stride = rng.uniform(0.25, 0.7)
    tide = side == 'tide'
    if tide:
        kind = rng.choice(['guard', 'raider', 'harp'], p=[0.5, 0.35, 0.15])
        armour = VERD[1] if kind == 'guard' else NAVY[2]
        cloth = SEA[3] if rng.random() < 0.7 else SEA[2]
        skin = SKIN_TIDE[2]
    else:
        kind = rng.choice(['risen', 'reaper', 'bow'], p=[0.55, 0.3, 0.15])
        armour = GRAVE_IRON if kind != 'risen' else hexc('#3A3430')
        cloth = VIOLET[2] if rng.random() < 0.6 else VIOLET[1]
        skin = BONE * 0.8 if kind != 'risen' else hexc('#5E6A55')
    dark = armour * 0.7
    # legs
    parts.append((limb(P(-0.05, 1.5), P(-stride, 0), 0.48 * h), dark, 'b'))
    parts.append((limb(P(0.1, 1.5), P(stride * 0.8, 0), 0.48 * h), armour, 'b'))
    # back arm
    if fighting or rng.random() < 0.5:
        hand_b = P(0.9, 2.5)
    else:
        hand_b = P(0.2, 1.7)
    parts.append((limb(P(-0.15, 2.9), hand_b, 0.42 * h), dark, 'b'))
    # torso
    parts.append((np.float32([P(-0.5, 1.45), P(0.5, 1.45), P(0.5, 3.0), P(-0.5, 3.0)]), armour, 'b'))
    if tide:
        parts.append((np.float32([P(-0.42, 1.2), P(0.42, 1.2), P(0.44, 2.4), P(-0.44, 2.4)]), cloth, 'b'))
    else:
        hem = ragged(P(-0.48, 1.4), P(0.48, 1.4), 0.5 * h, 6, rng)
        parts.append((np.float32([P(-0.48, 2.7), P(0.48, 2.7)] + hem[::-1]), cloth, 'b'))
    # head
    hc = P(0.08, 3.5)
    parts.append((box(hc, h * 0.95, h * 0.95, face * lean * 0.6), skin, 'b'))
    if tide:
        if kind == 'raider':
            parts.append((np.float32([P(-0.55, 3.8), P(-0.3, 4.3), P(0.4, 4.3), P(0.65, 3.8)]), VERD[2], 'b'))
        elif kind == 'harp':
            parts.append((np.float32([P(-0.9, 3.7), P(-0.4, 4.2), P(0.5, 4.2), P(0.6, 3.85)]), hexc('#5A4A28'), 'b'))
        else:
            parts.append((np.float32([P(-0.5, 3.9), P(-0.45, 4.15), P(0.5, 4.15), P(0.55, 3.9)]), VERD[2], 'b'))
            for s in (-0.6, 0.55):  # coral rising from the shoulders
                parts.append((limb(P(s, 3.0), P(s * 1.25, 3.6), 0.14 * h, 0.06 * h), CORAL[2], 'b'))
    else:
        if kind in ('reaper', 'bow'):  # pointed hood, eyes lit
            parts.append((np.float32([P(-0.6, 3.0), P(-0.6, 3.95), P(-0.1, 4.55), P(0.6, 4.0), P(0.6, 3.0)]),
                          VIOLET[0], 'b'))
            for ex in (0.12, 0.38):
                parts.append((box(P(ex, 3.55), h * 0.12, h * 0.1), SOUL, 'glow'))
        else:
            parts.append((box(P(0.25, 3.45), h * 0.12, h * 0.1), hexc('#E8F4E0'), 'b'))
    # front arm and weapon
    if tide:
        if kind == 'raider':
            hand = P(0.7, 3.4) if fighting else P(0.6, 2.4)
            tip = (hand[0] + face * 1.1 * h, hand[1] - (0.9 if fighting else 0.3) * h)
            parts.append((limb(hand, tip, 0.16 * h, 0.08 * h), hexc('#B8B8B0'), 'w'))
        else:
            ang = rng.uniform(0.1, 0.45) if fighting else (rng.uniform(0.35, 0.8) if rng.random() < 0.7 else rng.uniform(1.2, 1.45))
            hand = P(0.75, 2.5)
            ln = 4.6 * h if kind == 'guard' else 3.6 * h
            back = (hand[0] - face * math.cos(ang) * ln * 0.35, hand[1] + math.sin(ang) * ln * 0.35)
            tip = (hand[0] + face * math.cos(ang) * ln * 0.65, hand[1] - math.sin(ang) * ln * 0.65)
            parts.append((limb(back, tip, 0.12 * h), hexc('#5A4630'), 'w'))
            if kind == 'guard':  # three prongs
                for o in (-0.22, 0, 0.22):
                    nx, ny = -math.sin(ang) * o * h, -math.cos(ang) * o * h * face
                    p0 = (tip[0] + nx * face, tip[1] + ny * face)
                    p1 = (p0[0] + face * math.cos(ang) * 0.7 * h, p0[1] - math.sin(ang) * 0.7 * h)
                    parts.append((limb(p0, p1, 0.09 * h, 0.04 * h), CORAL[1], 'w'))
                parts.append((limb((tip[0] - face * 0.0, tip[1]), (tip[0] + face * 0.05 * h, tip[1]), 0.5 * h), CORAL[1], 'b'))
        parts.append((limb(P(0.1, 2.9), hand, 0.42 * h), armour * 1.15, 'b'))
    else:
        if kind == 'reaper':
            hand = P(0.5, 3.6) if fighting else P(0.6, 2.6)
            top = (hand[0] - face * 0.8 * h, hand[1] - 1.6 * h)
            bot = (hand[0] + face * 0.6 * h, hand[1] + 1.5 * h)
            parts.append((limb(bot, top, 0.12 * h), hexc('#3A2C22'), 'w'))
            blade = [top, (top[0] + face * 0.7 * h, top[1] - 0.25 * h), (top[0] + face * 1.6 * h, top[1] + 0.35 * h),
                     (top[0] + face * 0.8 * h, top[1] + 0.05 * h), (top[0], top[1] + 0.2 * h)]
            parts.append((np.float32(blade), hexc('#7A7A80'), 'b'))
        elif kind == 'bow':
            hand = P(1.0, 2.9)
            parts.append((np.float32([(hand[0] - face * 0.1 * h, hand[1] - 1.3 * h),
                                      (hand[0] + face * 0.35 * h, hand[1]),
                                      (hand[0] - face * 0.1 * h, hand[1] + 1.3 * h),
                                      (hand[0] + face * 0.15 * h, hand[1])]), BONE * 0.9, 'b'))
        else:
            hand = P(0.8, 3.3) if fighting else P(0.7, 2.3)
            tip = (hand[0] + face * 0.9 * h, hand[1] - 1.0 * h)
            parts.append((limb(hand, tip, 0.2 * h, 0.12 * h), hexc('#6A625A'), 'b'))
        parts.append((limb(P(0.1, 2.9), hand, 0.4 * h), (skin if kind == 'risen' else armour) * 1.1, 'b'))
    return parts


def admiral(x, y, h):
    """Admiral Vael, the Drowned Tide: tricorne, calf-length coat, gilt trim, thrusting the admiral's trident up
    and right into the Hollow King's falling greatsword. Faces right. Feet centre at (x, y), head h px."""
    rng = np.random.default_rng(21)
    parts = []

    def P(lx, ly):
        return (x + lx * h, y - ly * h)

    coat, coat_d, trim = NAVY[2], NAVY[1], GILT
    skin = SKIN_TIDE[3]
    # back leg, extended behind (left), and front leg bent forward
    parts.append((limb(P(-0.15, 1.6), P(-0.95, 0.75), 0.55 * h, 0.5 * h), NAVY[0], 'b'))
    parts.append((limb(P(-0.95, 0.75), P(-1.55, 0.05), 0.5 * h, 0.46 * h), NAVY[0], 'b'))
    parts.append((box(P(-1.62, 0.12), 0.62 * h, 0.26 * h, 0.35), hexc('#140E0C'), 'b'))
    # coat tails flaring back in the wind, with a kelp-ragged hem
    tail = [P(-0.45, 2.4), P(-1.25, 1.6), P(-2.25, 0.9), P(-2.55, 0.55)]
    hem = [P(-2.4, 0.5), P(-1.9, 0.55), P(-1.5, 0.75), P(-0.9, 0.85), P(-0.3, 1.0), P(0.25, 1.3)]
    parts.append((np.float32(tail + hem + [P(0.3, 2.3)]), coat_d, 'b'))
    parts.append((np.float32([(px, py - 6) for px, py in hem[:-1]] + [(px, py) for px, py in hem[-2::-1]]), trim * 0.8, 'b'))
    parts.append((limb(P(0.15, 1.55), P(0.85, 0.85), 0.58 * h, 0.52 * h), NAVY[1], 'b'))
    parts.append((limb(P(0.85, 0.85), P(0.95, 0.05), 0.52 * h, 0.5 * h), NAVY[1], 'b'))
    parts.append((box(P(1.08, 0.12), 0.66 * h, 0.26 * h), hexc('#140E0C'), 'b'))
    # back arm (left hand low on the shaft)
    parts.append((limb(P(-0.1, 3.05), P(0.55, 2.35), 0.46 * h), coat_d, 'b'))
    # torso: coat, sea-blue sash, gilt facings
    torso = [P(-0.6, 1.55), P(0.55, 1.6), P(0.72, 3.1), P(-0.42, 3.15)]
    parts.append((np.float32(torso), coat, 'b'))
    parts.append((np.float32([P(-0.55, 1.95), P(0.6, 2.0), P(0.62, 2.3), P(-0.5, 2.25)]), SEA[4], 'b'))
    parts.append((np.float32([P(0.2, 1.6), P(0.36, 1.6), P(0.62, 3.1), P(0.46, 3.1)]), trim, 'b'))
    parts.append((np.float32([P(-0.62, 2.95), P(0.75, 3.0), P(0.8, 3.25), P(-0.55, 3.25)]), trim * 0.9, 'b'))
    # head: pale drowned skin, hair adrift behind, tricorne
    for i in range(4):
        a = rng.uniform(0, 1)
        parts.append((limb(P(-0.25, 3.9 - a * 0.5), P(-1.1 - a * 0.6, 3.75 - a * 0.8 + rng.uniform(-0.2, 0.2)),
                           0.12 * h, 0.03 * h), hexc('#3E4A50'), 'b'))
    parts.append((box(P(0.12, 3.62), 0.95 * h, 0.95 * h, 0.08), skin, 'b'))
    parts.append((box(P(0.47, 3.66), 0.12 * h, 0.1 * h), hexc('#CFF3E4'), 'glow'))
    parts.append((np.float32([P(0.25, 3.3), P(0.6, 3.25), P(0.6, 3.38), P(0.25, 3.42)]), SKIN_TIDE[1], 'b'))
    tric = [P(-0.95, 4.0), P(-0.5, 4.5), P(-0.15, 4.85), P(0.35, 4.7), P(0.95, 4.55), P(0.85, 4.15),
            P(0.3, 4.12), P(-0.4, 4.08)]
    parts.append((np.float32(tric), hexc('#12121A'), 'b'))
    parts.append((np.float32([P(-0.95, 4.0), P(-0.4, 4.08), P(0.3, 4.12), P(0.85, 4.15), P(0.85, 4.22),
                              P(0.3, 4.2), P(-0.4, 4.16), P(-0.92, 4.1)]), GILT_HI, 'b'))
    # admiral_trident: bronze shaft, gilt collar, three barbed prongs; thrust up and right
    hand = P(0.95, 2.7)
    tx, ty = Scene.weapon_contact
    ang = math.atan2(hand[1] - ty, tx - hand[0])
    reach = math.hypot(tx - hand[0], ty - hand[1]) - 0.75 * h
    back = (hand[0] - math.cos(ang) * 1.6 * h, hand[1] + math.sin(ang) * 1.6 * h)
    tip = (hand[0] + math.cos(ang) * reach, hand[1] - math.sin(ang) * reach)
    parts.append((limb(back, tip, 0.15 * h), hexc('#4E3818'), 'b'))
    cx, cy = tip
    dx, dy = math.cos(ang), -math.sin(ang)
    nx, ny = -dy, dx
    parts.append((limb((cx - dx * 0.1 * h, cy - dy * 0.1 * h), (cx + dx * 0.15 * h, cy + dy * 0.15 * h), 0.75 * h), GILT, 'b'))
    for o in (-0.32, 0, 0.32):
        p0 = (cx + nx * o * h, cy + ny * o * h)
        p1 = (p0[0] + dx * (1.0 if o == 0 else 0.8) * h, p0[1] + dy * (1.0 if o == 0 else 0.8) * h)
        parts.append((limb(p0, p1, 0.13 * h, 0.03 * h), hexc('#C8C8C0'), 'b'))
        bx = (p0[0] + dx * 0.55 * h, p0[1] + dy * 0.55 * h)
        parts.append((np.float32([bx, (bx[0] + nx * 0.1 * h * (1 if o >= 0 else -1) - dx * 0.15 * h,
                                       bx[1] + ny * 0.1 * h * (1 if o >= 0 else -1) - dy * 0.15 * h),
                                  (bx[0] - dx * 0.08 * h, bx[1] - dy * 0.08 * h)]), hexc('#C8C8C0'), 'b'))
    # front arm on the shaft, gilt cuff
    parts.append((limb(P(0.35, 3.0), (hand[0] + dx * 0.9 * h, hand[1] + dy * 0.9 * h), 0.46 * h, 0.42 * h), coat, 'b'))
    cuff = (hand[0] + dx * 0.75 * h, hand[1] + dy * 0.75 * h)
    parts.append((box(cuff, 0.25 * h, 0.5 * h, -ang), trim, 'b'))
    # kelp trailing from the arms and coat
    for (px, py) in (P(-0.5, 2.9), P(0.4, 2.6), P(-1.6, 1.1)):
        k = [(px, py)]
        for i in range(5):
            k.append((k[-1][0] - rng.uniform(12, 22), k[-1][1] + rng.uniform(4, 14)))
        for a0, a1 in zip(k, k[1:]):
            parts.append((limb(a0, a1, 7, 4), KELP, 'b'))
    return parts


def hollow_king(x, y, h):
    """The Hollow King: tall, broad, black grave-iron plate, broken gilt crown, jaw bound in gilt wire, a lit
    ribcage, a dragging ragged violet cloak; brings the grave_sword down overhead. Faces left."""
    rng = np.random.default_rng(31)
    parts = []

    def P(lx, ly):  # lx forward (to the left on screen)
        return (x - lx * h, y - ly * h)

    iron, iron_d = hexc('#34302E'), hexc('#1C1A1A')
    # cloak dragging behind (right), ragged violet hem
    cloak_top = [P(-0.6, 3.35), P(0.5, 3.4)]
    hem = ragged(P(0.6, 0.05), P(-2.6, 0.0), 0.12 * h, 16, rng)
    parts.append((np.float32([P(-0.8, 3.3), P(-1.8, 2.0), P(-2.7, 0.6)] + hem[::-1][:-1] + [P(0.6, 0.6), P(0.6, 3.0)]),
                  VIOLET[1], 'b'))
    inner = [P(-0.75, 3.1), P(-1.5, 1.9), P(-2.2, 0.8), P(-1.8, 0.4), P(-0.9, 0.5), P(-0.5, 1.6)]
    parts.append((np.float32(inner), VIOLET[2], 'b'))
    # legs, wide stance
    parts.append((limb(P(-0.3, 1.7), P(-0.95, 0.05), 0.6 * h, 0.55 * h), iron_d, 'b'))
    parts.append((limb(P(0.3, 1.7), P(0.95, 0.85), 0.62 * h), iron, 'b'))
    parts.append((limb(P(0.95, 0.85), P(1.05, 0.05), 0.6 * h, 0.55 * h), iron, 'b'))
    parts.append((box(P(1.2, 0.12), 0.7 * h, 0.28 * h), iron_d, 'b'))
    parts.append((box(P(-1.0, 0.1), 0.7 * h, 0.26 * h), iron_d, 'b'))
    # violet tabard in strips
    tab = ragged(P(-0.55, 1.0), P(0.6, 1.0), 0.5 * h, 7, rng)
    parts.append((np.float32([P(-0.6, 2.0), P(0.6, 2.0)] + tab[::-1]), VIOLET[3], 'b'))
    # torso: broad plate, the ribcage open on a soul-lit core
    torso = [P(-0.75, 1.75), P(0.7, 1.75), P(0.85, 3.45), P(-0.85, 3.45)]
    parts.append((np.float32(torso), iron, 'b'))
    core = box(P(0.1, 2.48), 0.8 * h, 0.75 * h)
    parts.append((core, hexc('#5A6090'), 'glow'))
    for i in range(4):  # ribs over the light
        yy = 2.15 + i * 0.22
        parts.append((limb(P(-0.4, yy), P(0.6, yy + 0.05), 0.08 * h), BONE * 0.75, 'b'))
    parts.append((limb(P(0.1, 2.0), P(0.1, 2.95), 0.12 * h), BONE * 0.7, 'b'))
    # pauldrons, black plate with gilt edge
    for sx in (-0.75, 0.75):
        parts.append((box(P(sx, 3.35), 0.8 * h, 0.5 * h, -0.15 if sx > 0 else 0.15), iron_d, 'b'))
        parts.append((box(P(sx, 3.12), 0.82 * h, 0.08 * h, -0.15 if sx > 0 else 0.15), GILT * 0.8, 'b'))
    # the grave_sword: a notched greatsword brought down from overhead into the trident's prongs
    cx, cy = Scene.weapon_contact
    grip = P(-0.95, 4.75)
    d = np.float32([cx - grip[0], cy - grip[1]])
    ln = np.hypot(*d)
    d /= ln
    n = np.float32([-d[1], d[0]])
    g = np.float32(grip)
    pommel = g - d * 0.55 * h
    tip = g + d * (ln + 0.9 * h)
    parts.append((limb(pommel, g + d * 0.2 * h, 0.17 * h), hexc('#2A1C14'), 'b'))
    parts.append((box(tuple(g + d * 0.25 * h), 1.1 * h, 0.16 * h, math.atan2(n[1], n[0])), GILT * 0.85, 'b'))
    base = g + d * 0.33 * h
    blade = [base + n * 0.22 * h, tip - d * 0.6 * h + n * 0.2 * h, tip, tip - d * 0.6 * h - n * 0.2 * h,
             base - n * 0.22 * h]
    parts.append((np.float32(blade), hexc('#5A5A64'), 'b'))
    parts.append((np.float32([base, tip - d * 0.4 * h, tip - d * 0.6 * h - n * 0.2 * h, base - n * 0.22 * h]),
                  hexc('#3A3A44'), 'b'))
    for t in (0.35, 0.55, 0.7):  # notches
        p = base + (tip - base) * t + n * 0.23 * h
        parts.append((np.float32([p, p - n * 0.1 * h + d * 0.06 * h, p + d * 0.14 * h]), hexc('#1A1416'), 'b'))
    # sword arm raised behind (screen right), front hand clawing at the Admiral
    parts.append((limb(P(-0.6, 3.35), (grip[0] + 0.1 * h, grip[1] + 0.15 * h), 0.52 * h, 0.46 * h), iron, 'b'))
    parts.append((box(grip, 0.44 * h, 0.44 * h, 0.4), iron_d, 'b'))
    claw = P(1.55, 3.05)
    parts.append((limb(P(0.65, 3.3), P(1.2, 3.1), 0.5 * h, 0.44 * h), iron_d, 'b'))
    parts.append((limb(P(1.2, 3.1), claw, 0.4 * h, 0.36 * h), iron_d, 'b'))
    for a in (-0.35, 0.0, 0.35):
        f0 = claw
        f1 = (claw[0] - math.cos(a) * 0.45 * h, claw[1] + math.sin(a) * 0.45 * h - 0.05 * h)
        parts.append((limb(f0, f1, 0.1 * h, 0.04 * h), BONE * 0.8, 'b'))
    # skull head, jaw bound in gilt wire, broken crown
    parts.append((box(P(0.12, 3.95), 1.0 * h, 1.0 * h, 0.05), BONE * 0.85, 'b'))
    parts.append((box(P(0.3, 3.95), 0.25 * h, 0.2 * h), hexc('#120E14'), 'b'))
    parts.append((box(P(-0.05, 3.95), 0.25 * h, 0.2 * h), hexc('#120E14'), 'b'))
    parts.append((box(P(0.12, 3.92), 0.1 * h, 0.08 * h), SOUL, 'glow'))
    for i in range(3):
        parts.append((box(P(0.12, 3.62 - i * 0.07), 0.95 * h, 0.035 * h), GILT_HI, 'b'))
    crown = [P(-0.42, 4.42), P(-0.42, 4.75), P(-0.3, 4.6), P(-0.15, 4.9), P(0.0, 4.6), P(0.12, 4.7), P(0.25, 4.55),
             P(0.38, 4.85), P(0.5, 4.6), P(0.62, 4.62), P(0.66, 4.42)]
    parts.append((np.float32(crown), GILT, 'b'))
    return parts


def debris():
    out = []
    # broken trident shaft in the bottom-right rise
    p = [((2280, 1330), (2380, 1150), 9, hexc('#2A1E14')), ((2390, 1290), (2470, 1250), 7, hexc('#1E1610'))]
    for a, b, w, c in p:
        out.append([(limb(a, b, w), c, 'b')])
    # a grave-sword stuck point-down, tilted
    out.append([(limb((2130, 1380), (2085, 1205), 13, 4), hexc('#25232A'), 'b'),
                (box((2083, 1197), 54, 9, -0.25), hexc('#3A2E1A'), 'b'),
                (limb((2083, 1195), (2075, 1160), 8), hexc('#1A1210'), 'b')])
    return out


# ================================================================ the paint pass
def structure_dir(lum):
    gx = cv2.Sobel(lum, cv2.CV_32F, 1, 0, ksize=3)
    gy = cv2.Sobel(lum, cv2.CV_32F, 0, 1, ksize=3)
    jxx = cv2.GaussianBlur(gx * gx, (0, 0), 6)
    jyy = cv2.GaussianBlur(gy * gy, (0, 0), 6)
    jxy = cv2.GaussianBlur(gx * gy, (0, 0), 6)
    ang = 0.5 * np.arctan2(2 * jxy, jxx - jyy) + np.pi / 2  # strokes run along edges
    strength = np.sqrt((jxx - jyy) ** 2 + 4 * jxy ** 2)
    k = smooth(0.0004, 0.004, strength)
    vx = np.cos(ang) * k + (1 - k) * np.cos(ang * 0.15)
    vy = np.sin(ang) * k + (1 - k) * np.sin(ang * 0.15)
    return np.arctan2(vy, vx), strength


def paint(src, fast=False, seed=2):
    """Stroke-based repaint: brushes coarse to fine; a stroke is laid where the canvas differs from the source,
    follows the edge direction for a few steps, and takes the source colour at its start with a small jitter."""
    rng = np.random.default_rng(seed)
    radii = [24, 12, 6, 3] if not fast else [24, 12, 6]
    src8 = np.clip(src * 255, 0, 255).astype(np.uint8)
    canvas = cv2.GaussianBlur(src8, (0, 0), 24)
    lum_full = cv2.cvtColor(src8, cv2.COLOR_RGB2GRAY).astype(np.float32) / 255
    ang_full, strength = structure_dir(lum_full)
    for R in radii:
        ref = cv2.GaussianBlur(src8, (0, 0), R * 0.5)
        reff = ref.astype(np.float32)
        diff = np.abs(canvas.astype(np.float32) - reff).sum(axis=2)
        T = 28 if R > 3 else 34
        strokes = []
        step = max(2, R)
        for gy in range(0, H, step):
            for gx in range(0, W, step):
                cell = diff[gy:gy + step, gx:gx + step]
                if cell.mean() < T and R != radii[0]:
                    continue
                j = np.unravel_index(np.argmax(cell), cell.shape)
                strokes.append((gx + j[1], gy + j[0]))
        order = rng.permutation(len(strokes))
        lenmax = {24: 5, 12: 5, 6: 5, 3: 4}[R]
        thick = max(1, int(R * 0.9))
        for i in order:
            x, y = strokes[i]
            c = reff[y, x]
            jit = rng.normal(0, 5, 3)
            col = tuple(float(v) for v in np.clip(c + jit, 0, 255))
            pts = [(x, y)]
            px, py = float(x), float(y)
            last = None
            for k in range(lenmax):
                a = ang_full[int(py), int(px)]
                dx, dy = math.cos(a), math.sin(a)
                if last is not None and dx * last[0] + dy * last[1] < 0:
                    dx, dy = -dx, -dy
                last = (dx, dy)
                px += dx * R
                py += dy * R
                if not (0 <= px < W and 0 <= py < H):
                    break
                if np.abs(reff[int(py), int(px)] - c).sum() > 60:
                    break
                pts.append((px, py))
            if len(pts) == 1:
                cv2.circle(canvas, (x * 4, y * 4), thick * 2, col, -1, cv2.LINE_AA, shift=2)
            else:
                cv2.polylines(canvas, [np.int32(np.array(pts) * 4)], False, col, thick, cv2.LINE_AA, shift=2)
    out = canvas.astype(np.float32) / 255
    # impasto: a soft emboss of the paint's own luminance, lit from the upper right
    lum = cv2.cvtColor(canvas, cv2.COLOR_RGB2GRAY).astype(np.float32) / 255
    hl = cv2.GaussianBlur(lum, (0, 0), 1.2)
    emb = cv2.filter2D(hl, -1, np.float32([[0, 0, -1], [0, 0, 0], [1, 0, 0]])) * 0.6
    out = out * (1 + emb[..., None] * 0.5)
    # canvas weave
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    weave = (np.sin(xx * 1.9) * np.sin(yy * 1.9)) * 0.012 + np.random.default_rng(4).normal(0, 0.012, (H, W))
    out = out * (1 + weave[..., None])
    return np.clip(out, 0, 1)


def main():
    v = sys.argv[1]
    out = sys.argv[2]
    fast = '--fast' in sys.argv
    s = Scene(VARIANTS[v]).build()
    s.grade()
    base = s.img
    if '--base' in sys.argv:
        bp = sys.argv[sys.argv.index('--base') + 1]
        cv2.imwrite(bp, cv2.cvtColor((base * 255).astype(np.uint8), cv2.COLOR_RGB2BGR))
    painted = paint(base, fast=fast)
    # keep the glows crisp over the paint
    painted = np.clip(painted + np.clip(s.glow, 0, 1) * 0.25, 0, 1)
    cv2.imwrite(out, cv2.cvtColor((painted * 255).astype(np.uint8), cv2.COLOR_RGB2BGR))


if __name__ == '__main__':
    main()
