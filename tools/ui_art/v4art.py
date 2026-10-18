"""v4 art for the chosen family (War Table screens, Iron Ledger HUD). Run: python v4art.py [name ...]
Writes PNG sprites into v4/ at GUI scale 1. Each function paints one sprite by hand-placed rules:
named hue-shifted ramps, light from the top-left, shading in clusters, dithering only for large falloffs."""
import math, os, random, sys
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'v4')
os.makedirs(OUT, exist_ok=True)

from v4pal import *


def save(spr, name):
    spr.save(os.path.join(OUT, name + '.png'))
    print('  v4/' + name + '.png', spr.w, 'x', spr.h)


# ================================================================ the oak table (full screen)
RUSTW = ['#3b2214', '#4f2c18', '#63391f']     # rust bleeding into oak, dark to light


def oak_table(W=960, H=540, seed=11, avoid=()):
    """flat-sawn oak planks at full pixel density. Per plank: a base tone, cathedral figures (nested arches
    whose arms run out into straight grain), straight late-wood lines broken into runs, early-wood pores,
    knots with rings, a check crack and a lit far wall, the grain bending round them. Between planks: a dark
    gap, the lower plank's top arris lit and worn, the upper plank's bottom arris turning away. Butt joints
    with end grain and domed nail heads that bleed rust and sit in a hammer dent. Scratches, dents and rubbed
    patches where hands rest; a stepped vignette that keeps the eye in the middle."""
    rnd = random.Random(seed)
    # tools/ui_art edit, UI Task 1 fix round 1: the light and the scratches round it sit where the mock put them on its
    # 960x540 table, (480, 300), 30 px below the middle, and keep that place relative to the middle of a bigger table,
    # so the 1280x720 exports are lit from (640, 390) and not from up and left. The 960x540 table paints as before.
    lx, ly = 480 + (W - 960) / 2.0, 300 + (H - 540) / 2.0

    def clear(x, y0, y1, pad=10):
        return not any(ax0 - pad <= x <= ax1 + pad and y0 <= ay1 and y1 >= ay0 for (ax0, ay0, ax1, ay1) in avoid)
    idx = [[4] * W for _ in range(H)]
    lock = [[False] * W for _ in range(H)]
    planks = []
    y = -6
    while y < H:
        h = rnd.choice([30, 32, 34, 36, 38])
        planks.append((y, h)); y += h
    wob = Noise(seed + 9, 40, 6)
    pn = Noise(seed + 10, 9, 2)
    nails = []
    prev = []

    def put(x, y, t, y0, h, force=False):
        if 0 <= x < W and 0 <= y < H and y0 + 2 <= y <= y0 + h - 3 and (force or not lock[y][x]):
            idx[y][x] = t
    for (y0, h) in planks:
        base = rnd.choice([3, 4, 4, 4])
        for yy in range(max(0, y0), min(H, y0 + h)):
            row = idx[yy]
            for xx in range(W):
                row[xx] = base
        dark, darker, light = base - 1, base - 2, base + 1
        yc = y0 + h / 2.0
        half = h / 2.0 - 3.0
        # knots first, so the grain can bend round them
        knots = []
        for _ in range(2):
            if rnd.random() < 0.5:
                kx = rnd.randint(30, W - 30)
                if clear(kx, y0, y0 + h, 16):
                    knots.append((kx, y0 + rnd.randint(10, h - 11), rnd.randint(4, 8), rnd.choice([2, 3, 3])))

        def disp(x, yb, knots=knots):
            d = 0.0
            for (kx, ky, rx, ry) in knots:
                dy = yb - ky
                reach = ry + 8
                if abs(dy) < reach:
                    push = (1 if dy >= 0 else -1) * (reach - abs(dy)) * 0.8
                    d += push * math.exp(-((x - kx) / (rx * 2.6)) ** 2)
            return d
        # cathedral figures: nested arches opening along the plank, their arms flattening into straight grain
        figs = []
        for _ in range(rnd.choice([1, 2, 2])):
            figs.append(dict(ax=rnd.randint(60, W + 60), N=rnd.randint(5, 8), d=rnd.uniform(6.5, 11.0),
                             p=rnd.uniform(0.8, 1.9), yc=yc + rnd.uniform(-2.0, 2.0), dir=rnd.choice([1, -1]),
                             L=rnd.uniform(160, 420)))

        def inside(x, y, figs=figs, half=half):
            for f in figs:
                dist = (f['ax'] - x) * f['dir']
                if 0 <= dist <= f['L']:
                    Y = min(half - 0.5, math.sqrt(f['p'] * dist))
                    if abs(y - f['yc']) < Y - 0.5:
                        return True
            return False
        # straight late-wood lines, broken into runs, kept out of the figures
        n = max(7, int(h / 2.4))
        for k in range(n):
            yb = y0 + 3 + (h - 6) * (k + rnd.random() * 0.7) / n
            amp = rnd.uniform(0.2, 1.1); Lw = rnd.uniform(120, 320); ph = rnd.uniform(0, 6.283)
            tone = rnd.choices([dark, darker, light], [50, 30, 20])[0]
            x = rnd.randint(-80, 0); on = True
            while x < W:
                run = rnd.randint(40, 200) if on else rnd.randint(6, 40)
                if on:
                    for xx in range(max(0, x), min(W, x + run)):
                        Y = round(yb + amp * math.sin(6.283 * xx / Lw + ph) + disp(xx, yb))
                        if not inside(xx, Y):
                            put(xx, Y, tone, y0, h)
                on = not on; x += run
        # the arches
        for f in figs:
            for j in range(f['N']):
                apex = f['ax'] - f['dir'] * j * f['d']
                ymax = half * (1.0 - j / (f['N'] + 0.6))
                lj = f['L'] * rnd.uniform(0.55, 1.0)
                tone = darker
                for side in (-1, 1):
                    last = None
                    for step in range(int(lj)):
                        xx = round(apex - f['dir'] * step)
                        Y = min(ymax, math.sqrt(f['p'] * step))
                        yy = round(f['yc'] + side * Y + (wob(xx, y0) - 0.5) * 1.4 + disp(xx, f['yc'] + side * Y))
                        if last is not None and abs(yy - last) > 1:
                            for t in range(min(yy, last) + 1, max(yy, last)):
                                put(xx, t, tone, y0, h)
                        put(xx, yy, tone, y0, h)
                        # early wood: a lighter band on the outside of each arch, in clusters
                        if pn(xx, yy) > 0.35:
                            put(xx, yy + side, light, y0, h)
                        last = yy
                        if step > 30 and rnd.random() < 0.004:
                            break
        # pores: short dark dashes along the grain in the lighter wood
        for _ in range(int(W * h / 70)):
            xx = rnd.randint(0, W - 4); yy = rnd.randint(y0 + 3, y0 + h - 4)
            if 0 <= yy < H and idx[yy][xx] >= base:
                for t in range(rnd.choice([2, 2, 3])):
                    put(xx + t, yy, dark, y0, h)
        # knots: rings round a dark core, a check crack, the lit far wall of the hollow
        for (kx, ky, rx, ry) in knots:
            for yy in range(ky - ry - 3, ky + ry + 4):
                for xx in range(kx - rx - 3, kx + rx + 4):
                    if not (0 <= yy < H and 0 <= xx < W) or yy <= y0 + 1 or yy >= y0 + h - 2:
                        continue
                    d = ((xx - kx) / (rx + 0.5)) ** 2 + ((yy - ky) / (ry + 0.5)) ** 2
                    dd = ((xx - kx) / (rx + 2.0)) ** 2 + ((yy - ky) / (ry + 1.5)) ** 2
                    if d <= 0.1:
                        t = 0
                    elif d <= 0.3:
                        t = 1
                    elif d <= 0.5:
                        t = 2
                    elif d <= 0.68:
                        t = 3
                    elif d <= 0.84:
                        t = 2
                    elif d <= 1.0:
                        t = light
                    elif dd <= 1.0:
                        t = dark
                    else:
                        continue
                    idx[yy][xx] = t; lock[yy][xx] = True
            for (dx, dy) in ((rx - 1, 1), (rx - 2, 1), (rx - 2, 2), (rx - 3, 2)):
                X, Y = kx + dx, ky + dy
                if 0 <= Y < H and 0 <= X < W:
                    idx[Y][X] = 6
            cdir = rnd.choice([-1, 1])
            for t in range(rx + 2, rx + 2 + rnd.randint(4, 10)):
                X = kx + cdir * t; Y = ky + round(math.sin(t * 0.7) * 0.6)
                if 0 <= Y < H and 0 <= X < W:
                    idx[Y][X] = 1; lock[Y][X] = True
        # seams and arrises
        en = Noise(seed + y0 + 40, 7, 3)
        for xx in range(W):
            rows = ((y0, 0), (y0 + 1, 6), (y0 + h - 1, 1), (y0 + h - 2, 2))
            for (yy, t) in rows:
                if 0 <= yy < H:
                    if yy == y0 + 1:
                        v = en(xx, yy)
                        t = 7 if v > 0.72 else (6 if v > 0.28 else 5)
                    if yy == y0 + h - 2 and en(xx, yy + 50) > 0.55:
                        t = dark
                    idx[yy][xx] = t; lock[yy][xx] = True
            # wear: rubbed lighter patches just under the lit arris
            yy = y0 + 2
            if 0 <= yy < H and en(xx, yy + 90) > 0.66:
                idx[yy][xx] = light + 1 if en(xx, yy + 91) > 0.82 else light
                lock[yy][xx] = True
        # butt joints with end grain, staggered against the plank above
        joints = []
        for _ in range(rnd.choice([1, 2, 2])):
            for _try in range(40):
                jx = rnd.randint(70, W - 70)
                if all(abs(jx - q) > 90 for q in prev + joints) and clear(jx, y0, y0 + h):
                    joints.append(jx); break
        for jx in joints:
            for yy in range(y0 + 1, y0 + h):
                if 0 <= yy < H:
                    idx[yy][jx] = 0; idx[yy][jx - 1] = 2; idx[yy][jx + 1] = 6
                    idx[yy][jx + 2] = light if (yy * 3) % 7 else base
                    for q in (jx - 1, jx, jx + 1, jx + 2):
                        lock[yy][q] = True
            for nx in (jx - 7, jx + 4):
                for ny in (y0 + 6, y0 + h - 9):
                    nails.append((nx, ny, base))
        prev = joints
    # scratches, dents and rubbed patches where hands and tokens rest
    for _ in range(140):
        cx = rnd.gauss(lx, 200); cy = rnd.gauss(ly, 120)
        if not (20 < cx < W - 20 and 60 < cy < H - 20):
            continue
        ln = rnd.randint(4, 14); ang = rnd.uniform(-0.5, 0.5) + (math.pi / 2 if rnd.random() < 0.15 else 0)
        for i in range(ln):
            X = round(cx + i * math.cos(ang)); Y = round(cy + i * math.sin(ang))
            if 0 <= Y < H and 0 <= X < W and not lock[Y][X] and clear(X, Y, Y, 2):
                idx[Y][X] = 6 if i % 5 else 5
    for _ in range(50):
        X = rnd.randint(30, W - 30); Y = rnd.randint(60, H - 20)
        if all(not lock[Y][X + t] for t in range(3)):
            idx[Y][X] = 2; idx[Y][X + 1] = 2
            if Y + 1 < H:
                idx[Y + 1][X + 1] = 6
    # stepped vignette following the grain
    nz = Noise(seed + 5, 30, 3)
    for yy in range(H):
        for xx in range(W):
            dx = (xx - lx) / 600.0; dy = (yy - ly) / 380.0
            r = math.sqrt(dx * dx + dy * dy)
            dk = max(0.0, min(1.0, (r - 0.5) / 0.55)) * 1.9
            steps = int(dk + (nz(xx, yy) - 0.5) * 0.9 + 0.5) if dk > 0 else 0
            if steps and idx[yy][xx] > 0:
                idx[yy][xx] = max(0, idx[yy][xx] - steps)
    s = Spr(W, H)
    for yy in range(H):
        for xx in range(W):
            s.set(xx, yy, OAK[idx[yy][xx]])
    # nail heads: domed, lit top-left, sitting in a hammer dent, rust bleeding down the grain
    NAIL = ['.kk.', 'kHmk', 'kmdk', '.kk.']
    for (nx, ny, base) in nails:
        if not (2 <= ny < H - 6 and 2 <= nx < W - 6):
            continue
        for (dx, dy) in ((-1, 0), (0, -1), (-1, 1), (1, -1)):
            s.set(nx + dx, ny + dy, OAK[max(0, base - 2)])
        for (dx, dy) in ((4, 2), (2, 4), (4, 3), (3, 4)):
            s.set(nx + dx, ny + dy, OAK[min(8, base + 2)])
        for (dx, dy, c) in ((4, 1, RUSTW[1]), (5, 2, RUSTW[0]), (1, 4, RUSTW[1]), (6, 2, RUSTW[0]), (2, 5, RUSTW[0])):
            if rnd.random() < 0.8:
                s.set(nx + dx, ny + dy, c)
        s.grid(nx, ny, NAIL, {'k': IRON[1], 'H': IRON[7], 'm': IRON[5], 'd': IRON[3]})
    return s


ASSETS = {
    'oak-table': lambda: oak_table(avoid=[(40, 180, 360, 372)]),   # home: the party names sit on bare oak
    'oak-table-recruit': lambda: oak_table(avoid=[(664, 344, 904, 496)]),   # recruit: the ladder under the letters
}

if __name__ == '__main__':
    import v4a_table, v4a_paper, v4a_hud, v4a_recruit, v4a_ink, v4a_map, v4a_furn, v4a_portrait, v4a_recruit2  # each registers its sprites
    for mod in (v4a_table, v4a_paper, v4a_hud, v4a_recruit, v4a_ink, v4a_map, v4a_furn, v4a_portrait, v4a_recruit2):
        ASSETS.update(mod.ASSETS)
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        save(ASSETS[n](), n)
