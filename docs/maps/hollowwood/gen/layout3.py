"""Hollowwood "The Hollow", layout sketch 3 (2026-11-14). Half-turn symmetric: (x, z) -> (-x, -z). python layout2.py OUT.png
Kort on sketch 2: 'Good, but go wilder'; bridges walkable; fallen trunks through hollows only. Kort on sketch 1: "Let's not make it seem like all the other maps, let's make it interesting and fun"."""
import sys, math, random
from PIL import Image, ImageDraw, ImageFont

X0, Z0, X1, Z1 = -288, -192, 287, 191
S = 2
POINTS = {'A': (-128, -96), 'B': (0, 0), 'C': (128, 96)}
SPAWN = {'Dawn': (-236, 124), 'Dusk': (236, -124)}
R_PIT = 60
# Dawn's half; Dusk's is this turned half way round
NEAR = [(-236, 124), (-222, 70), (-196, 20), (-170, -40), (-150, -80), (-128, -96)]            # camp to the Great Stump (A), winding
RIM = [(-196, 20), (-150, 24), (-110, 14), (-78, 6), (-60, 0)]                                 # to the west lip of the Hollow
SOUTH = [(-236, 124), (-180, 136), (-120, 130), (-60, 112), (-10, 96), (40, 100), (90, 104), (128, 96)]   # the long way round to the marsh (C)
STUMP = [(-128, -96), (-90, -84), (-56, -60), (-38, -46)]                                      # A down to the north-west lip


def turn(pts): return [(-x, -z) for (x, z) in pts]
def length(pts): return sum(math.dist(a, b) for a, b in zip(pts, pts[1:]))
def P(x, z): return ((x - X0) * S, (z - Z0) * S + 40)


def main(out):
    im = Image.new('RGB', ((X1 - X0 + 1) * S, (Z1 - Z0 + 1) * S + 40), (20, 22, 28)); d = ImageDraw.Draw(im)
    try: f = ImageFont.truetype('arialbd.ttf', 15); fs = ImageFont.truetype('arial.ttf', 12)
    except Exception: f = fs = ImageFont.load_default()
    def lab(x, z, t, c=(255, 255, 255), ff=None): d.text(P(x, z), t, fill=c, font=ff or fs)
    def line(pts, c, w): d.line([P(*p) for p in pts], fill=c, width=w, joint='curve')
    def oval(cx, cz, rx, rz, c, o=None, w=1): d.ellipse([P(cx - rx, cz - rz), P(cx + rx, cz + rz)], fill=c, outline=o, width=w)
    d.text((8, 10), 'Hollowwood "The Hollow", layout sketch 3.  576 x 384 blocks.  Same map when turned half way round.  North is up.', fill=(255, 255, 255), font=f)
    DEEP = (18, 44, 30); WOOD = (46, 84, 52); GLADE = (128, 158, 92); MARSH = (104, 120, 84); PIT = (58, 48, 36); FLOOR = (96, 112, 70)
    d.rectangle([P(X0, Z0), P(X1, Z1)], fill=DEEP)
    d.polygon([P(*p) for p in [(-280, 60), (-270, -60), (-220, -140), (-120, -176), (40, -182), (200, -180), (280, -150), (280, -60), (270, 60), (220, 140), (120, 176), (-40, 182), (-200, 180), (-280, 150)]], fill=WOOD)
    rnd = random.Random(11)
    for _ in range(260):                                                         # giant trunks (cover)
        x, z = rnd.randint(-270, 0), rnd.randint(-176, 176)
        if math.hypot(x, z) < R_PIT + 14 or math.hypot(x + 128, z + 96) < 46 or math.hypot(x - 128, z - 96) < 60 or math.hypot(x + 236, z - 124) < 44: continue
        if any(min(math.dist((x, z), p) for p in pts) < 14 for pts in (NEAR, RIM, SOUTH, STUMP, turn(SOUTH), turn(STUMP), turn(RIM))): continue
        if not (-272 < x and abs(z) < 170 + x * 0.1): continue
        r = rnd.choice((4, 5, 5, 6, 7))
        for sg in (1, -1): oval(sg * x, sg * z, r, r, (92, 64, 40), (40, 28, 18))
    # the Hollow: a sinkhole where the mother tree stood, 10 deep
    oval(0, 0, R_PIT + 5, R_PIT + 5, (70, 56, 40)); oval(0, 0, R_PIT, R_PIT, PIT); oval(0, 0, R_PIT - 16, R_PIT - 16, FLOOR)
    for sg in (1, -1):                                                           # the spiral ramps, lip to floor
        d.arc([P(-R_PIT + 7, -R_PIT + 7), P(R_PIT - 7, R_PIT - 7)], 150 if sg > 0 else 330, 250 if sg > 0 else 70, fill=(176, 140, 84), width=14)
    for sg in (1, -1):                                                           # root bridges across the Hollow, high over the floor
        line([(sg * -52, sg * -32), (sg * -20, sg * -40), (sg * 20, sg * -40), (sg * 52, sg * -32)], (120, 84, 50), 9)
    # A: the Great Stump with the stockade on it; C: the marsh stilts
    oval(-128, -96, 40, 34, (120, 90, 58), (60, 44, 28), 3); oval(-128, -96, 30, 25, (150, 116, 76))
    oval(128, 96, 54, 40, MARSH)
    for sg in (1,):
      for (x, z) in ((100, 84), (150, 80), (160, 108), (104, 116), (132, 126)): d.rectangle([P(x - 6, z - 5), P(x + 6, z + 5)], fill=(120, 96, 66), outline=(50, 40, 30))
    # the Fallen Giants: colossal trunks lying across the wood, each with a hollow you walk through
    for sg in (1, -1):
        line([(sg * -230, sg * -20), (sg * -150, sg * 50), (sg * -96, sg * 84)], (104, 74, 44), 22)
        oval(sg * -176, sg * 28, 7, 7, (30, 22, 16)); oval(sg * -118, sg * 70, 7, 7, (30, 22, 16))
    R = (176, 140, 84); R2 = (150, 128, 96)
    for pts in (NEAR, turn(NEAR)): line(pts, R, 7)
    for pts in (RIM, turn(RIM), SOUTH, turn(SOUTH), STUMP, turn(STUMP)): line(pts, R2, 5)
    HW = (222, 196, 120); GU = (60, 44, 30)
    HIGH = [(-104, -92), (-84, -70), (-64, -48), (-52, -32)]                      # the High Walk: plank decks round the giant trunks, +6
    DECK = [(-52, -32), (-62, 0), (-52, 32)]                                      # the gallery along the west lip, joining the two bridges
    UP = [(-150, 24), (-120, 2), (-92, -20), (-62, 0)]                            # a stair tree on the rim path: the way up from Dawn's side
    GULLY = [(-60, 112), (-48, 92), (-34, 70), (-24, 54)]                         # the Root Run: a sunken gully off the long road
    TUN = [(-24, 54), (-16, 38)]                                                  # ... and a root tunnel through the pit wall onto the floor
    for sg in (1, -1):
        t = lambda pts: [(sg * x, sg * z) for (x, z) in pts]
        line(t(GULLY), GU, 11); line(t(GULLY), (120, 98, 70), 5)
        for i in range(0, 5): a, b = TUN; x = a[0] + (b[0] - a[0]) * i / 4; z = a[1] + (b[1] - a[1]) * i / 4; oval(sg * x, sg * z, 3, 3, (20, 14, 10))
        for pts in (HIGH, DECK, UP):
            line(t(pts), (70, 50, 30), 8); line(t(pts), HW, 4)
            for (x, z) in pts[1:-1]: oval(sg * x, sg * z, 7, 7, (92, 64, 40), HW, 2)
        line(t([(-52, -32), (-20, -40), (20, -40), (52, -32)]), HW, 4)
        oval(sg * -150, sg * 24, 8, 8, (92, 64, 40), (255, 240, 160), 3)
        for a, b in (((100, 84), (150, 80)), ((150, 80), (160, 108)), ((160, 108), (132, 126)), ((132, 126), (104, 116)), ((104, 116), (100, 84)), ((100, 84), (52, 32))):
            line([(sg * a[0], sg * a[1]), (sg * b[0], sg * b[1])], (70, 50, 30), 8); line([(sg * a[0], sg * a[1]), (sg * b[0], sg * b[1])], HW, 4)
    lab(-250, -8, 'stair tree: the way up', c=(255, 240, 160)); lab(-120, -52, 'the High Walk (+6)', c=(255, 240, 160)); lab(-112, 84, 'the Root Run: gully and tunnel into the pit', c=(240, 220, 190))
    lab(150, 132, 'boardwalks between the huts (+6)', c=(255, 240, 160))
    for sg, n in ((1, 'DAWN CAMP'), (-1, 'DUSK CAMP')):
        d.rectangle([P(sg * -236 - 28, sg * 124 - 26), P(sg * -236 + 28, sg * 124 + 26)], outline=(150, 110, 80), width=4); lab(sg * -236 - 30, sg * 124 + (30 if sg > 0 else -44), n, ff=f)
    d.arc([P(-16, -14), P(16, 10)], 180, 360, fill=(236, 226, 196), width=7); d.rectangle([P(-4, -4), P(4, 4)], fill=(170, 170, 180))
    lab(-58, 64, 'THE HOLLOW: a pit 10 deep; shrine and the Yoke on the floor', ff=f)
    lab(-40, -70, 'root bridges cross above', c=(240, 220, 190)); lab(-200, -150, 'THE GREAT STUMP: a hollowed giant stump (+6), its bark rim is the wall', ff=f)
    lab(60, 150, 'MARSH STILTS: dry bog below, huts above', ff=f); lab(-270, -34, 'a Fallen Giant: walk round it or through its hollows', c=(240, 220, 190))
    lab(-282, -186, 'deep wood (no go)'); lab(-110, 136, 'the long road', c=(240, 220, 190)); lab(60, -150, 'the long road', c=(240, 220, 190))
    for n, (x, z) in (('start', (-128, -84)), ('stop 1', (-44, -50)), ('stop 2', (0, 0)), ('stop 3', (44, 50)), ('end', (128, 84))):
        d.rectangle([P(x - 4, z - 4), P(x + 4, z + 4)], fill=(220, 60, 60)); lab(x - 14, z + 8, n, (255, 150, 150))
    for n, (x, z) in POINTS.items():
        oval(x, z, 17, 17, None, (240, 170, 0), 3); lab(x + 20, z - 8, n, (255, 190, 40), f)
    for n, (x, z) in SPAWN.items():
        oval(x, z, 5, 5, (40, 170, 80) if n == 'Dawn' else (60, 100, 230)); lab(x - 14, z + 8, n)
    for i in range(1, 9): d.line([P(X0 + 64 * i, Z0), P(X0 + 64 * i, Z1)], fill=(90, 100, 96), width=1)
    for j in range(1, 6): d.line([P(X0, Z0 + 64 * j), P(X1, Z0 + 64 * j)], fill=(90, 100, 96), width=1)
    im.convert('RGB').save(out, quality=90); print(out, im.size)
    print('Dawn to A %.0f, to the Hollow lip %.0f (+ about 60 down the ramp to B), to C %.0f' % (length(NEAR), length(NEAR[:3]) + length(RIM), length(SOUTH)))


if __name__ == '__main__':
    main(sys.argv[1])
