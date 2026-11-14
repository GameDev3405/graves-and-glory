"""Frostmarch "The Switchback", layout sketch 1 (2026-11-14). Half-turn symmetric: (x, z) -> (-x, -z). python layout.py OUT.png"""
import sys, math
from PIL import Image, ImageDraw, ImageFont

X0, Z0, X1, Z1 = -288, -192, 287, 191
S = 2
POINTS = {'A': (0, -112), 'B': (0, 0), 'C': (0, 112)}
SPAWN = {'Dawn': (-256, 0), 'Dusk': (256, 0)}
# west half; the east half is this turned half way round
ROAD_W = [(-256, 0), (-214, -8), (-176, -44), (-136, -70), (-100, -72), (-70, -58), (-44, -34), (-20, -12), (0, 0)]          # the S road, Dawn camp to the Cut
LOW_W = [(-256, 0), (-216, 30), (-176, 74), (-130, 104), (-92, 112)]                                                       # the low road, Dawn camp to the west foot of the Refuge Shelf
LINK_W = [(-100, -72), (-96, -100), (-92, -112)]                                                                           # first bend up to the west foot of the Signal Shelf
CROSS_W = [(-136, -70), (-150, -10), (-176, 74)]                                                                           # a cross path between the two roads
NORTH = [(-288, -120), (-250, -132), (-210, -150), (-160, -158), (-110, -164), (-60, -160), (0, -166), (60, -160), (110, -150), (160, -128), (200, -100), (240, -84), (287, -76)]   # foot of the north mountains


def turn(pts): return [(-x, -z) for (x, z) in pts]
def length(pts): return sum(math.dist(a, b) for a, b in zip(pts, pts[1:]))
def P(x, z): return ((x - X0) * S, (z - Z0) * S + 40)


def main(out):
    im = Image.new('RGB', ((X1 - X0 + 1) * S, (Z1 - Z0 + 1) * S + 40), (20, 22, 28)); d = ImageDraw.Draw(im)
    try: f = ImageFont.truetype('arialbd.ttf', 15); fs = ImageFont.truetype('arial.ttf', 12)
    except Exception: f = fs = ImageFont.load_default()
    def box(x0, z0, x1, z1, c, o=None): d.rectangle([P(x0, z0), P(x1, z1)], fill=c, outline=o)
    def poly(pts, c, o=None): d.polygon([P(*p) for p in pts], fill=c, outline=o)
    def line(pts, c, w): d.line([P(*p) for p in pts], fill=c, width=w, joint='curve')
    def lab(x, z, t, c=(255, 255, 255), ff=None): d.text(P(x, z), t, fill=c, font=ff or fs)
    def both(fn, *a):                                       # draw a thing and its half-turn twin
        fn(*a)
    d.text((8, 10), 'Frostmarch "The Switchback", layout sketch 1.  576 x 384 blocks.  Same map when turned half way round.  North is up.', fill=(255, 255, 255), font=f)
    ROCK = (84, 92, 108); SNOW = (226, 232, 238); HIGH = (200, 210, 224); SHELF = (176, 190, 210)
    box(X0, Z0, X1, Z1, ROCK)
    poly(NORTH + turn(NORTH), SNOW)                                                   # the pass floor, y 64 at the camps
    poly([(-120, -150), (-60, -100), (-40, -40), (-60, 40), (-90, 100), (-60, 150), (60, 150), (120, 150), (60, 100), (40, 40), (60, -40), (90, -100), (60, -150)], HIGH)    # the saddle, rising to y 72 at the Cut
    for sgn in (1, -1):                                                               # shelves, y 80
        z = -112 * sgn
        box(-58, z - 26, 58, z + 26, SHELF, (120, 130, 150))
        for sx in (-1, 1): poly([(sx * 58, z - 5), (sx * 92, z - 5), (sx * 92, z + 5), (sx * 58, z + 5)], (190, 170, 130))           # ramps, 10 wide
    # frozen tarns and falls: scenery only
    for sgn in (1, -1):
        d.ellipse([P(sgn * 196 - 26, sgn * 118 - 16), P(sgn * 196 + 26, sgn * 118 + 16)], fill=(150, 200, 235))
        lab(sgn * 196 - 24, sgn * 118 - 6, 'frozen tarn', (30, 60, 90))
        poly([(sgn * -150, sgn * -30), (sgn * -128, sgn * -44), (sgn * -116, sgn * -20), (sgn * -134, sgn * 4), (sgn * -156, sgn * -6)], ROCK)    # a crag between the two roads
    # the Spine
    B = (236, 226, 196)
    for z in range(-84, 85, 12):
        if abs(z) < 14: continue
        d.ellipse([P(-7, z - 5), P(7, z + 5)], fill=B, outline=(120, 110, 90))
    lab(10, -78, "THE SPINE: a giant's backbone", ff=f, c=(40, 40, 40)); lab(10, -62, 'a wall of bone, cut by the road', c=(40, 40, 40))
    R = (176, 140, 84); R2 = (150, 128, 96)
    for pts in (ROAD_W, turn(ROAD_W)): line(pts, R, 9)
    for pts in (LOW_W, turn(LOW_W), LINK_W, turn(LINK_W), CROSS_W, turn(CROSS_W)): line(pts, R2, 4)
    # camps, tower, refuge
    for sgn, n in ((1, 'DAWN CAMP'), (-1, 'DUSK CAMP')):
        box(sgn * -282 if sgn > 0 else 234, -34, -234 if sgn > 0 else 282, 34, (150, 110, 80)); lab(-280 if sgn > 0 else 216, -52, n, ff=f, c=(40, 40, 40))
    box(28, -138, 52, -118, (70, 74, 90)); lab(-52, -160, 'SIGNAL TOWER (beacon)', ff=f, c=(40, 40, 40))
    box(-52, 118, -28, 138, (90, 76, 66)); lab(-50, 142, 'REFUGE (hospice in the rock)', ff=f, c=(40, 40, 40))
    lab(-24, -108, 'Signal Shelf, +8', c=(40, 40, 40)); lab(-24, 92, 'Refuge Shelf, +8', c=(40, 40, 40))
    lab(-246, -112, 'snow slopes, pines', c=(60, 70, 80)); lab(150, 100, 'snow slopes, pines', c=(60, 70, 80)); lab(-280, -186, 'mountain (no go)'); lab(180, 172, 'mountain (no go)')
    lab(-128, -92, 'first bend', c=(60, 40, 20)); lab(84, 78, 'second bend', c=(60, 40, 20)); lab(-196, 82, 'low road', c=(60, 40, 20)); lab(150, -96, 'low road', c=(60, 40, 20))
    lab(14, -8, 'the Cut (top of the pass, +8)', c=(60, 40, 20))
    # convoy stops
    for n, (x, z) in (('start', (-222, -4)), ('stop 1', (-100, -72)), ('stop 2', (0, 0)), ('stop 3', (100, 72)), ('end', (222, 4))):
        d.rectangle([P(x - 4, z - 4), P(x + 4, z + 4)], fill=(220, 60, 60)); lab(x - 14, z + 8, n, (170, 20, 20))
    for n, (x, z) in POINTS.items():
        d.ellipse([P(x - 17, z - 17), P(x + 17, z + 17)], outline=(230, 150, 0), width=3); lab(x - 30, z - 12, n, (200, 120, 0), f)
    for n, (x, z) in SPAWN.items():
        d.ellipse([P(x - 5, z - 5), P(x + 5, z + 5)], fill=(40, 170, 80) if n == 'Dawn' else (60, 100, 230)); lab(x - 14, z + 10, n, (40, 40, 40))
    for i in range(1, 9): d.line([P(X0 + 64 * i, Z0), P(X0 + 64 * i, Z1)], fill=(150, 150, 160), width=1)
    for j in range(1, 6): d.line([P(X0, Z0 + 64 * j), P(X1, Z0 + 64 * j)], fill=(150, 150, 160), width=1)
    for i in range(9): lab(X0 + 64 * i + 3, Z0 + 2, 'ABCDEFGHI'[i], (210, 210, 210))
    for j in range(6): lab(X0 + 3, Z0 + 64 * j + 16, str(j + 1), (210, 210, 210))
    im.save(out); print(out, im.size)
    rd = length(ROAD_W); print('road camp to Cut %.0f, whole S %.0f' % (rd, 2 * rd))
    a_dawn = length(ROAD_W[:5]) + length(LINK_W) + 34 + 58; a_dusk = length(turn(LOW_W)) + 34 + 58
    print('to A: Dawn %.0f (road, first bend, west ramp), Dusk %.0f (low road, east ramp)' % (a_dawn, a_dusk))


if __name__ == '__main__':
    main(sys.argv[1])
