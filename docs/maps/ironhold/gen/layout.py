"""Ironhold fresh layout, sketch 2 (2026-11-13): walls run cliff to cliff across a mountain pass. python layout.py OUT.png"""
import sys
from PIL import Image, ImageDraw, ImageFont

X0, Z0, X1, Z1 = -288, -192, 287, 191
S = 2
POINTS = {'A': (-112, 0), 'B': (0, 0), 'C': (112, 0)}
SPAWN = {'Dawn (attack)': (-256, 0), 'Dusk (defend)': (256, 0)}
# the pass floor, west (wide valley) to east (narrow, high): north edge then south edge, as (x, z)
NORTH = [(-288, -150), (-230, -158), (-180, -140), (-150, -150), (-110, -128), (-80, -138), (-40, -112), (0, -122), (30, -96), (70, -104), (100, -80), (140, -88), (170, -62), (210, -72), (250, -56), (287, -60)]
SOUTH = [(-288, 140), (-240, 152), (-190, 132), (-150, 146), (-120, 120), (-84, 132), (-50, 104), (-10, 116), (26, 90), (66, 100), (104, 74), (136, 84), (168, 58), (206, 66), (252, 50), (287, 56)]
OUTER = [(-84, -138), (-78, -96), (-66, -60), (-72, -14), (-72, 14), (-64, 52), (-76, 92), (-84, 132)]       # outer curtain, cliff to cliff
INNER = [(30, -96), (38, -66), (46, -34), (40, -12), (40, 12), (48, 36), (36, 64), (26, 90)]                 # inner curtain
UPPER = [(170, -62), (176, -30), (172, -6), (172, 6), (178, 30), (168, 58)]                                  # upper wall


def P(x, z): return ((x - X0) * S, (z - Z0) * S + 40)


def main(out):
    im = Image.new('RGB', ((X1 - X0 + 1) * S, (Z1 - Z0 + 1) * S + 40), (20, 22, 28)); d = ImageDraw.Draw(im)
    try: f = ImageFont.truetype('arialbd.ttf', 15); fs = ImageFont.truetype('arial.ttf', 12)
    except Exception: f = fs = ImageFont.load_default()
    def box(x0, z0, x1, z1, c, o=None): d.rectangle([P(x0, z0), P(x1, z1)], fill=c, outline=o)
    def poly(pts, c): d.polygon([P(*p) for p in pts], fill=c)
    def line(pts, c, w): d.line([P(*p) for p in pts], fill=c, width=w, joint='curve')
    def lab(x, z, t, c=(255, 255, 255), ff=None): d.text(P(x, z), t, fill=c, font=ff or fs)
    def band(xa, xb, c):                                    # the pass floor between two wall lines
        n = [p for p in NORTH if xa <= p[0] <= xb]; s = [p for p in SOUTH if xa <= p[0] <= xb]
        poly(n + s[::-1], c)
    d.text((8, 10), 'Ironhold, layout sketch 2.  576 x 384 blocks.  A mountain pass: west is low, east is high.  North is up.', fill=(255, 255, 255), font=f)
    box(X0, Z0, X1, Z1, (78, 86, 96))                                 # mountain rock, no go
    poly(NORTH + SOUTH[::-1], (92, 122, 74))                          # valley floor y 64
    poly([p for p in NORTH if p[0] >= -84] + [p for p in SOUTH if p[0] >= -84][::-1], (124, 124, 100))    # outer ward y 70
    poly([p for p in NORTH if p[0] >= 30] + [p for p in SOUTH if p[0] >= 26][::-1], (140, 134, 116))      # inner ward y 76
    poly([p for p in NORTH if p[0] >= 170] + [p for p in SOUTH if p[0] >= 168][::-1], (122, 118, 112))    # upper ward y 76
    poly([(-176, -20), (-150, -34), (-136, -8), (-150, 30), (-172, 18)], (78, 86, 96)); lab(-170, -6, 'crag')                    # a crag that splits the approach
    poly([(-30, -70), (-6, -84), (14, -62), (-4, -44)], (78, 86, 96)); poly([(-34, 58), (-10, 44), (12, 62), (-8, 84)], (78, 86, 96))   # rock knuckles in the outer ward
    W = (44, 46, 54)
    for wall in (OUTER, INNER, UPPER):
        line(wall, W, 9)
        for (x, z) in wall[1:-1]:
            if abs(z) > 14: box(x - 6, z - 6, x + 6, z + 6, (30, 30, 36))                        # towers at the bends
    G = (176, 120, 60)
    for x in (-72, 40): box(x - 9, -14, x + 9, 14, (30, 30, 36)); box(x - 4, -5, x + 4, 5, G)      # main and inner gatehouses
    box(168, -5, 176, 5, G)
    box(-82, -100, -72, -90, G); lab(-68, -106, 'mine gate (north flank)')
    box(-74, 66, -62, 80, (150, 90, 70)); lab(-58, 70, 'breach (south flank)')
    box(34, -72, 44, -62, G); box(40, 44, 50, 54, G)
    poly([(92, -84), (128, -92), (150, -66), (140, -40), (100, -36)], (36, 36, 44)); lab(98, -70, 'GREAT KEEP', ff=f); lab(98, -54, 'on a rock spur')
    box(206, -28, 262, 28, (70, 70, 80)); lab(210, -24, 'barracks')
    R = (190, 162, 104)
    line([(-256, 0), (-206, 0), (-180, 34), (-150, 44), (-128, 20), (-112, 0), (-72, 0), (-30, -8), (0, 0), (40, 0), (76, 8), (112, 0), (172, 0), (256, 0)], R, 7)
    line([(-206, 0), (-186, -52), (-146, -84), (-112, -96), (-77, -95), (-36, -92), (4, -86), (36, -67), (76, -40), (112, 0)], R, 4)
    line([(-206, 0), (-190, 70), (-150, 96), (-110, 86), (-68, 73), (-34, 92), (8, 86), (45, 49), (80, 36), (112, 0)], R, 4)
    box(-282, -38, -234, 38, (130, 96, 70)); lab(-280, -54, 'SIEGE CAMP', ff=f)
    lab(-170, -126, 'slag heaps, ore carts'); lab(-170, 108, 'scree, dead pines'); lab(-56, -118, 'forges, smelters'); lab(-20, 96, 'stables')
    lab(-58, 18, 'OUTER WARD', ff=f); lab(60, 20, 'INNER WARD', ff=f); lab(184, -52, 'UPPER WARD', ff=f); lab(-270, -180, 'mountain (no go)'); lab(120, 150, 'mountain (no go)')
    lab(-260, 120, 'valley, height 64'); lab(-60, 34, 'height 70'); lab(64, 36, 'height 76')
    for n, (x, z) in POINTS.items():
        d.ellipse([P(x - 17, z - 17), P(x + 17, z + 17)], outline=(255, 208, 40), width=3); lab(x - 5, z - 36, n, (255, 208, 40), f)
    for n, (x, z) in SPAWN.items():
        d.ellipse([P(x - 5, z - 5), P(x + 5, z + 5)], fill=(60, 200, 90) if 'Dawn' in n else (70, 110, 230)); lab(x - 30, z + 10, n)
    for i in range(1, 9): d.line([P(X0 + 64 * i, Z0), P(X0 + 64 * i, Z1)], fill=(150, 150, 160), width=1)
    for j in range(1, 6): d.line([P(X0, Z0 + 64 * j), P(X1, Z0 + 64 * j)], fill=(150, 150, 160), width=1)
    for i in range(9): lab(X0 + 64 * i + 3, Z0 + 2, 'ABCDEFGHI'[i], (210, 210, 210))
    for j in range(6): lab(X0 + 3, Z0 + 64 * j + 16, str(j + 1), (210, 210, 210))
    im.save(out); print(out, im.size)


if __name__ == '__main__':
    main(sys.argv[1])
