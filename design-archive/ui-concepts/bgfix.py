"""Clean copies of the two real screenshots used by the v2 mockups (the originals stay untouched).

bg/fight-v2.png  from bg/fight-clean.png:
  - lifts the vanilla chat backing (five half-black bars, y 857-901) by the measured per-row factor
  - removes the old score strip's bottom edge (y 119-134, x 430-1495) where it crosses open sky
  - re-tiles the floor right of the patch seam (x 1572, y 770 down), which cut a lantern in half
bg/camp-v2.png   from bg/camp-clean.png:
  - replaces the brown and moss smears (x 570-1360, y 930-980) with the brick course above them
"""
from PIL import Image
import statistics

def lum(p):
    return (p[0] * 299 + p[1] * 587 + p[2] * 114) / 1000

def fight():
    im = Image.open('bg/fight-clean.png').convert('RGB'); px = im.load(); W, H = im.size
    # 1. chat backing: find the dark bars by column, then lift each row by gap/bar brightness
    ratio = []
    for x in range(W):
        band = statistics.mean(lum(px[x, y]) for y in range(864, 895))
        out = statistics.mean([lum(px[x, y]) for y in range(820, 850)] + [lum(px[x, y]) for y in range(908, 938)])
        ratio.append(band / out)
    bar = [r < 0.8 for r in ratio]
    core = bar
    gaps = [x for x in range(W) if not bar[x] and 80 < x < 1560]
    cores = [x for x in range(W) if core[x]]
    for y in range(855, 903):
        g = statistics.mean(lum(px[x, y]) for x in gaps); c = statistics.mean(lum(px[x, y]) for x in cores)
        f = max(1.0, min(2.6, g / max(c, 1)))
        for x in range(W):
            if not bar[x]:
                continue
            p = px[x, y]; px[x, y] = tuple(min(255, int(v * f + 0.5)) for v in p)
        # soften the one-pixel step where a lifted bar meets the untouched floor
        for x in range(1, W - 1):
            if bar[x] != bar[x + 1]:
                for c in (x, x + 1):
                    l, r = px[c - 1, y], px[c + 1, y]
                    px[c, y] = tuple((l[i] + r[i]) // 2 for i in range(3))
    # 2. score strip edge: rebuild sky between the clean rows above and below, keep tree pixels
    def sky(p):
        return p[2] > 200 and p[2] - p[0] > 30
    def tree(p):
        return p[1] > p[0] + 12 and p[1] >= p[2]
    for x in range(426, 1502):
        top, bot = px[x, 118], px[x, 135]
        if not (sky(top) or sky(bot)):
            continue
        top = top if sky(top) else bot
        bot = bot if sky(bot) else top
        for y in range(119, 135):
            p = px[x, y]
            if tree(p):
                continue
            t = (y - 118) / 17
            px[x, y] = tuple(int(top[i] * (1 - t) + bot[i] * t + 0.5) for i in range(3))
    # 3. patch seam (x 1572, y 770 down): tile the floor from the left, two bricks at a time.
    #    The brick period grows with depth (71 px at y 780 to 103 px at y 1060, measured by autocorrelation).
    orig = im.copy(); op = orig.load()
    for y in range(770, H):
        s = round(2 * (71 + (y - 780) * 0.115))
        for x in range(1572, W):
            px[x, y] = px[x - s, y]
        if y < 782:  # fade in over the first 12 rows so no horizontal edge appears
            t = (y - 770) / 12
            for x in range(1572, W):
                a, b = op[x, y], px[x, y]
                px[x, y] = tuple(int(a[i] * (1 - t) + b[i] * t + 0.5) for i in range(3))
    # 4. remove isolated specks the tiling carried along (pixels far from their 3x3 median)
    ref = im.copy(); rp = ref.load()
    for y in range(841, H - 1):
        for x in range(1301, W - 1):
            nb = sorted((rp[x + dx, y + dy] for dx in (-1, 0, 1) for dy in (-1, 0, 1)), key=lum)
            med = nb[4]
            if abs(lum(rp[x, y]) - lum(med)) > 40:
                px[x, y] = med
    im.save('bg/fight-v2.png'); print('wrote bg/fight-v2.png')

def camp():
    im = Image.open('bg/camp-clean.png').convert('RGB'); px = im.load()
    src = im.copy(); sp = src.load()
    DY = 44
    for y in range(928, 982):
        for x in range(566, 1364):
            px[x, y] = sp[x, y - DY]
    im.save('bg/camp-v2.png'); print('wrote bg/camp-v2.png')

if __name__ == '__main__':
    fight(); camp()
