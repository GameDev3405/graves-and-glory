"""v3 art for the chosen family (War Table screens, Iron Ledger HUD). Run: python v3art.py [name ...]
Writes PNG sprites into v3/ at GUI scale 1. Each function paints one sprite by hand-placed rules:
named hue-shifted ramps, light from the top-left, shading in clusters, dithering only for large falloffs."""
import math, os, random, sys
from v3lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'v3')
os.makedirs(OUT, exist_ok=True)

from v3pal import *


def save(spr, name):
    spr.save(os.path.join(OUT, name + '.png'))
    print('  v3/' + name + '.png', spr.w, 'x', spr.h)


# ================================================================ the oak table (full screen)
def oak_table(W=960, H=540, seed=11, avoid=()):
    """flat-sawn oak planks running across the screen: broken grain lines that bend around knots,
    a dark seam with a lit lower lip between planks, staggered butt joints with iron nails,
    light scuffs where hands rest, and a stepped vignette that pulls the eye to the middle."""
    rnd = random.Random(seed)

    def clear(x, y0, y1, pad=10):
        """no joint, nail or knot where text sits on the bare table"""
        return not any(ax0 - pad <= x <= ax1 + pad and y0 <= ay1 and y1 >= ay0 for (ax0, ay0, ax1, ay1) in avoid)
    idx = [[4] * W for _ in range(H)]
    lock = [[False] * W for _ in range(H)]
    planks = []
    y = -6
    while y < H:
        h = rnd.choice([30, 32, 34, 36, 38])
        planks.append((y, h)); y += h
    nails = []
    prev = []
    for (y0, h) in planks:
        knots = []
        for _ in range(2):
            if rnd.random() < 0.45:
                kx = rnd.randint(30, W - 30)
                if clear(kx, y0, y0 + h, 14):
                    knots.append((kx, y0 + rnd.randint(9, h - 10), rnd.randint(4, 8), rnd.choice([2, 2, 3])))

        def disp(x, yb):
            d = 0.0
            for (kx, ky, rx, ry) in knots:
                dy = yb - ky
                reach = ry + 7
                if abs(dy) < reach:
                    push = (1 if dy >= 0 else -1) * (reach - abs(dy)) * 0.75
                    d += push * math.exp(-((x - kx) / (rx * 2.4)) ** 2)
            return d
        n = max(6, int(h / 2.7))
        for k in range(n):
            yb = y0 + 2.5 + (h - 5) * (k + rnd.random() * 0.7) / n
            amp = rnd.uniform(0.3, 1.3); L = rnd.uniform(110, 300); ph = rnd.uniform(0, 6.283)
            tone = rnd.choices([3, 2, 5, 6], [52, 14, 24, 10])[0]
            x = rnd.randint(-80, 0); on = True
            while x < W:
                run = rnd.randint(40, 190) if on else rnd.randint(8, 46)
                if on:
                    for xx in range(max(0, x), min(W, x + run)):
                        Y = round(yb + amp * math.sin(6.283 * xx / L + ph) + disp(xx, yb))
                        if y0 + 2 <= Y <= y0 + h - 3 and 0 <= Y < H:
                            idx[Y][xx] = tone
                            # late-wood bands are two pixels thick in places
                            if tone == 2 and 0 <= Y + 1 < H and Y + 1 <= y0 + h - 3 and (xx // 23) % 3 == 0:
                                idx[Y + 1][xx] = 3
                on = not on; x += run
        for (kx, ky, rx, ry) in knots:
            for yy in range(ky - ry - 2, ky + ry + 3):
                for xx in range(kx - rx - 2, kx + rx + 3):
                    if not (0 <= yy < H and 0 <= xx < W) or yy <= y0 + 1 or yy >= y0 + h - 2:
                        continue
                    d = ((xx - kx) / (rx + 0.5)) ** 2 + ((yy - ky) / (ry + 0.5)) ** 2
                    dd = ((xx - kx) / (rx + 1.8)) ** 2 + ((yy - ky) / (ry + 1.4)) ** 2
                    if d <= 0.12:
                        idx[yy][xx] = 1
                    elif d <= 0.45:
                        idx[yy][xx] = 2
                    elif d <= 0.7:
                        idx[yy][xx] = 3
                    elif d <= 1.0:
                        idx[yy][xx] = 2
                    elif dd <= 1.0:
                        idx[yy][xx] = 3
                    else:
                        continue
                    lock[yy][xx] = True
            # the lit rim on the knot's lower right (it is a hollow: light from the top-left hits the far wall)
            for (dx, dy) in ((rx - 1, 1), (rx - 2, 1), (rx - 3, 2) if ry > 2 else (rx - 3, 1)):
                X, Y = kx + dx, ky + dy
                if 0 <= Y < H and 0 <= X < W:
                    idx[Y][X] = 5
        # seams: a dark gap, the lower plank's top lip catches the light, its bottom lip turns away
        for xx in range(W):
            for (yy, t) in ((y0, 0), (y0 + 1, 6), (y0 + h - 1, 2)):
                if 0 <= yy < H:
                    if t == 6 and (xx * 7 + y0) % 41 < 3:
                        t = 5
                    idx[yy][xx] = t; lock[yy][xx] = True
        # butt joints, staggered against the plank above
        joints = []
        for _ in range(rnd.choice([1, 2, 2])):
            for _try in range(40):
                jx = rnd.randint(70, W - 70)
                if all(abs(jx - p) > 90 for p in prev + joints) and clear(jx, y0, y0 + h):
                    joints.append(jx); break
        for jx in joints:
            for yy in range(y0 + 1, y0 + h):
                if 0 <= yy < H:
                    idx[yy][jx] = 0; idx[yy][jx - 1] = 3; idx[yy][jx + 1] = 6
                    lock[yy][jx] = lock[yy][jx - 1] = lock[yy][jx + 1] = True
            for nx in (jx - 6, jx + 4):
                for ny in (y0 + 6, y0 + h - 8):
                    nails.append((nx, ny))
        prev = joints
    # scuffs and dents where hands and tokens rest (the middle of the table)
    for _ in range(170):
        cx = rnd.gauss(480, 190); cy = rnd.gauss(300, 110)
        if not (20 < cx < W - 20 and 60 < cy < H - 20):
            continue
        ln = rnd.randint(3, 11); ang = rnd.uniform(-0.25, 0.25)
        t = rnd.choice([5, 5, 6])
        for i in range(ln):
            X = round(cx + i * math.cos(ang)); Y = round(cy + i * math.sin(ang))
            if 0 <= Y < H and 0 <= X < W and not lock[Y][X] and clear(X, Y, Y, 2):
                idx[Y][X] = t
    for _ in range(60):
        X = rnd.randint(30, W - 30); Y = rnd.randint(60, H - 20)
        if not lock[Y][X]:
            idx[Y][X] = 2
            if X + 1 < W and not lock[Y][X + 1]:
                idx[Y][X + 1] = 3
    # stepped vignette; the step edges follow the grain (stretched noise), so they form clusters, not a checker
    nz = Noise(seed + 5, 30, 3)
    for yy in range(H):
        for xx in range(W):
            dx = (xx - 480) / 600.0; dy = (yy - 300) / 380.0
            r = math.sqrt(dx * dx + dy * dy)
            dark = max(0.0, min(1.0, (r - 0.5) / 0.55)) * 1.9
            steps = int(dark + (nz(xx, yy) - 0.5) * 0.9 + 0.5) if dark > 0 else 0
            if steps and idx[yy][xx] > 0:
                idx[yy][xx] = max(0, idx[yy][xx] - steps)
    s = Spr(W, H)
    for yy in range(H):
        for xx in range(W):
            s.set(xx, yy, OAK[idx[yy][xx]])
    for (nx, ny) in nails:
        if 0 <= ny < H - 2 and 0 <= nx < W - 2:
            s.set(nx, ny, IRON[6]); s.set(nx + 1, ny, IRON[4])
            s.set(nx, ny + 1, IRON[4]); s.set(nx + 1, ny + 1, IRON[2])
            s.set(nx + 2, ny + 1, OAK[2]); s.set(nx + 1, ny + 2, OAK[2]); s.set(nx + 2, ny + 2, OAK[2])
    return s


ASSETS = {
    'oak-table': lambda: oak_table(avoid=[(40, 180, 360, 372)]),   # home: the party names sit on bare oak
    'oak-table-recruit': lambda: oak_table(avoid=[(664, 300, 904, 470)]),   # recruit: purse and Epic counter
}

if __name__ == '__main__':
    import v3a_table, v3a_paper, v3a_hud, v3a_recruit  # each registers its sprites into ASSETS
    for mod in (v3a_table, v3a_paper, v3a_hud, v3a_recruit):
        ASSETS.update(mod.ASSETS)
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        save(ASSETS[n](), n)
