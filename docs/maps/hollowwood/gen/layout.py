"""Hollowwood "The Heartglade", layout sketch 1 (2026-11-14). Mirror-symmetric west-east and north-south. python layout.py OUT.png"""
import sys, math
from PIL import Image, ImageDraw, ImageFont

X0, Z0, X1, Z1 = -288, -192, 287, 191
S = 2
POINTS = {'A': (0, -112), 'B': (0, 0), 'C': (0, 112)}
SPAWN = {'Dawn': (-256, 0), 'Dusk': (256, 0)}
# the north-west quarter; the other three are mirrors
CAMP_ROAD = [(-256, 0), (-200, 0), (-160, 0)]                                  # camp to the west fork
RING = [(-160, 0), (-150, -40), (-120, -76), (-84, -100), (-40, -112), (0, -112)]   # the ring track, west fork to the north glade
SUNK = [(-104, -90), (-80, -78), (-49, -49), (-30, -30)]                        # the sunken path, ring track down into the Heartglade
ROOT = [(-160, 0), (-110, 0), (-46, 0)]                                         # west fork straight to the root gate (narrow)


def mir(pts, sx, sz): return [(x * sx, z * sz) for (x, z) in pts]
def length(pts): return sum(math.dist(a, b) for a, b in zip(pts, pts[1:]))
def P(x, z): return ((x - X0) * S, (z - Z0) * S + 40)


def main(out):
    im = Image.new('RGB', ((X1 - X0 + 1) * S, (Z1 - Z0 + 1) * S + 40), (20, 22, 28)); d = ImageDraw.Draw(im)
    try: f = ImageFont.truetype('arialbd.ttf', 15); fs = ImageFont.truetype('arial.ttf', 12)
    except Exception: f = fs = ImageFont.load_default()
    def lab(x, z, t, c=(255, 255, 255), ff=None): d.text(P(x, z), t, fill=c, font=ff or fs)
    def line(pts, c, w): d.line([P(*p) for p in pts], fill=c, width=w, joint='curve')
    def oval(cx, cz, rx, rz, c, o=None, w=1): d.ellipse([P(cx - rx, cz - rz), P(cx + rx, cz + rz)], fill=c, outline=o, width=w)
    d.text((8, 10), 'Hollowwood "The Heartglade", layout sketch 1.  576 x 384 blocks.  Same map mirrored left-right and top-bottom.  North is up.', fill=(255, 255, 255), font=f)
    DEEP = (18, 44, 30); WOOD = (46, 84, 52); GLADE = (128, 158, 92); MARSH = (104, 120, 84); BANK = (70, 56, 40)
    d.rectangle([P(X0, Z0), P(X1, Z1)], fill=DEEP)                               # the deep wood: no go
    oval(0, 0, 276, 180, WOOD)                                                   # the walkable wood: giant trunks as cover
    oval(0, -112, 74, 40, GLADE); oval(0, 112, 74, 40, MARSH); oval(0, 0, 50, 50, BANK); oval(0, 0, 44, 44, GLADE)
    for sx in (-1, 1): d.rectangle([P(sx * 258 - 26, -34), P(sx * 258 + 26, 34)], fill=(150, 110, 80))
    import random; rnd = random.Random(7)
    for _ in range(170):                                                         # giant trunks
        x, z = rnd.randint(8, 262), rnd.randint(8, 172)
        if (x / 270) ** 2 + (z / 176) ** 2 > 1 or math.hypot(x, z) < 60 or (x / 84) ** 2 + ((z - 112) / 50) ** 2 < 1 or x > 222 and z < 44: continue
        if min(abs(z) , abs(x - z), abs(math.hypot(x - 20, z + 10) - 150)) < 12 and x < 170: continue
        r = rnd.choice((4, 5, 5, 6, 7))
        for sx in (-1, 1):
            for sz in (-1, 1): oval(sx * x, sz * z, r, r, (92, 64, 40), (40, 28, 18))
    R = (176, 140, 84); R2 = (150, 128, 96); SK = (96, 72, 48)
    for sx in (-1, 1):
        line(mir(CAMP_ROAD, sx, 1), R, 9); line(mir(ROOT, sx, 1), R2, 3)
        for sz in (-1, 1): line(mir(RING, sx, sz), R, 7); line(mir(SUNK, sx, sz), SK, 9)
    # the stockade (north glade) and the marsh stilts (south glade)
    d.rectangle([P(-34, -138), P(34, -90)], outline=(90, 60, 30), width=4); d.rectangle([P(10, -136), P(30, -120)], fill=(110, 80, 50))
    lab(-70, -168, 'LOGGING STOCKADE (timber wall, gates west and east, sawmill)', ff=f)
    for (x, z) in ((-40, 126), (-16, 136), (20, 134), (44, 122), (-30, 96), (30, 96)): d.rectangle([P(x - 6, z - 5), P(x + 6, z + 5)], fill=(120, 96, 66), outline=(50, 40, 30))
    lab(-62, 158, 'MARSH STILTS (huts on stilts, plank walks, jetty)', ff=f)
    d.arc([P(-16, -14), P(16, 10)], 180, 360, fill=(236, 226, 196), width=7)       # the Yoke
    d.rectangle([P(-4, -4), P(4, 4)], fill=(170, 170, 180)); lab(-60, -64, "THE YOKE: a giant's collarbone arched over the shrine", ff=f)
    lab(-250, -120, 'giant-trunk wood (walkable, trunks are cover)', c=(220, 230, 210)); lab(-282, -186, 'deep wood (no go)'); lab(-40, 52, 'root bank round the glade', c=(230, 220, 200))
    lab(-112, -66, 'sunken path', c=(240, 220, 190)); lab(60, 70, 'sunken path', c=(240, 220, 190)); lab(-150, 6, 'root gate (narrow)', c=(240, 220, 190)); lab(-214, -44, 'ring track', c=(240, 220, 190))
    for n, (x, z) in (('start', (-30, -112)), ('stop 1', (-49, -49)), ('stop 2', (0, 0)), ('stop 3', (49, 49)), ('end', (0, 112))):
        d.rectangle([P(x - 4, z - 4), P(x + 4, z + 4)], fill=(220, 60, 60)); lab(x - 14, z + 8, n, (255, 150, 150))
    for n, (x, z) in POINTS.items():
        oval(x, z, 17, 17, None, (240, 170, 0), 3); lab(x + 20, z - 8, n, (255, 190, 40), f)
    for n, (x, z) in SPAWN.items():
        oval(x, z, 5, 5, (40, 170, 80) if n == 'Dawn' else (60, 100, 230)); lab(x - 14, z + 10, n)
    for (sx, sz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)): d.polygon([P(sx * 104 - 5, sz * 90), P(sx * 104 + 5, sz * 90), P(sx * 104, sz * 90 - 8)], fill=(200, 60, 200))
    lab(92, -84, 'Survival rise sites (4)', c=(240, 170, 240))
    for i in range(1, 9): d.line([P(X0 + 64 * i, Z0), P(X0 + 64 * i, Z1)], fill=(90, 100, 96), width=1)
    for j in range(1, 6): d.line([P(X0, Z0 + 64 * j), P(X1, Z0 + 64 * j)], fill=(90, 100, 96), width=1)
    im.save(out); print(out, im.size)
    a = length(CAMP_ROAD) + length(RING); b = length(CAMP_ROAD) + length(RING[:4]) + 22 + length(SUNK); g = length(CAMP_ROAD) + length(ROOT)
    print('camp to A or C by the ring %.0f; camp to B by a sunken path %.0f; by the root gate %.0f' % (a, b, g))
    print('wagon (stockade gate, NW path, shrine, SE path, jetty): %.0f' % (2 * (30 + length(RING[3:5]) * 0 + math.dist((-30, -112), (-84, -100)) + 22 + length(SUNK))))


if __name__ == '__main__':
    main(sys.argv[1])
