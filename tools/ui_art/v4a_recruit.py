"""v4 recruit pieces for the War Table: the five House banners hanging from a brass rod on the rail (woven cloth,
side folds, an embroidered device with raised thread, a running-stitch trim and gold fringe), the muster roll on
turned rollers, stars, the ornate reveal frame (a window for the live model render), the opened letter, the tied
bundle of sealed letters, the coin stack and the small ink icons."""
import math, random
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v4pal import *
from v4a_paper import parchment, PAPER, wax_seal, STAR, CROWN, SHADOW

ASSETS = {}

# walnut for the turned rollers (dark to light, warmer in the light)
WAL = ['#1b100c', '#2d1a12', '#432619', '#5a3420', '#73442a', '#8f5733', '#ad6d40', '#cf8c56']

HOUSES = {
    # cloth ramp dark..light; thread (lit, mid, shade) for the device and the trim
    'gilded':      (['#3d2a0e', '#5e4318', '#86642a', '#b08a3c', '#d1ab52', '#e8c97e'], ('#6e4f1c', '#4a3412', '#2e1f0c')),
    'graveborn':   (['#130a1a', '#211331', '#322047', '#422d56', '#5b4378', '#7f66aa'], (BRASS[7], BRASS[5], BRASS[3])),
    'wildmoot':    (['#24110a', '#391c0e', '#4f2714', '#65351d', '#844a29', '#a8623a'], (BRASS[7], BRASS[5], BRASS[3])),
    'ironclad':    (['#16191c', '#21262a', '#2e3439', '#3e464c', '#566066', '#77828a'], (BRASS[7], BRASS[5], BRASS[3])),
    'tidecallers': (['#0d1926', '#14263a', '#1c3550', '#264869', '#356189', '#4b80b0'], (BRASS[7], BRASS[5], BRASS[3])),
}

DEVICES = {
    # 15 x 15, one letter per stitch; the shading (lit thread, shade thread, the shadow it casts) is computed
    'gilded': ['....o..o..o....',      # a crowned sword, point down
               '....ooooooo....',
               '....ooooooo....',
               '......ooo......',
               '.......o.......',
               '..ooooooooooo..',
               '..o...ooo...o..',
               '......ooo......',
               '......ooo......',
               '......ooo......',
               '......ooo......',
               '......ooo......',
               '.......o.......',
               '.......o.......',
               '...............'],
    'graveborn': ['....ooooooo....',   # the death's head
                  '...ooooooooo...',
                  '..ooooooooooo..',
                  '..ooooooooooo..',
                  '..oo...o...oo..',
                  '..oo...o...oo..',
                  '..ooo.ooo.ooo..',
                  '...oooo.oooo...',
                  '....ooo.ooo....',
                  '....ooooooo....',
                  '....o.o.o.o....',
                  '....ooooooo....',
                  '.....ooooo.....',
                  '...............',
                  '...............'],
    'wildmoot': ['.o..o.....o..o.',    # the stag's head
                 '.o..o.....o..o.',
                 '..o.o.....o.o..',
                 '..ooo.....ooo..',
                 '....o.....o....',
                 '....ooooooo....',
                 'oo..ooooooo..oo',
                 '.ooooooooooooo.',
                 '....o.ooo.o....',
                 '.....ooooo.....',
                 '.....ooooo.....',
                 '......ooo......',
                 '......ooo......',
                 '......o.o......',
                 '...............'],
    'ironclad': ['....o.....o....',    # the anvil, struck
                 '...o.......o...',
                 '...............',
                 '...ooooooooooo.',
                 'oooooooooooooo.',
                 '.ooooooooooooo.',
                 '....oooooooooo.',
                 '......ooooooo..',
                 '.......ooooo...',
                 '.......ooooo...',
                 '......ooooooo..',
                 '.....ooooooooo.',
                 '.....ooooooooo.',
                 '...............',
                 '...............'],
    'tidecallers': ['..o....o....o..',  # the trident over the waves
                    '.oo...ooo...oo.',
                    '..o....o....o..',
                    '..o....o....o..',
                    '..oo...o...oo..',
                    '...ooooooooo...',
                    '.......o.......',
                    '.......o.......',
                    '.......o.......',
                    '......ooo......',
                    '.......o.......',
                    '.oo....o....oo.',
                    'o..oo..o..oo..o',
                    '.......o.......',
                    '...............'],
}


def embroider(S, rows, ox, oy, thread, shadow_c):
    """raised thread: lit where the stitch faces the top-left, shaded where it faces the bottom-right,
    and a one-pixel shadow cast on the cloth"""
    m = grid_mask(rows)
    lit, mid, shd = thread
    for (x, y) in m:
        if (x + 1, y + 1) not in m and S.opaque(ox + x + 1, oy + y + 1):
            S.set(ox + x + 1, oy + y + 1, shadow_c)
    for (x, y) in m:
        tl = (x - 1, y) not in m or (x, y - 1) not in m
        br = (x + 1, y) not in m or (x, y + 1) not in m
        S.set(ox + x, oy + y, lit if (tl and not br) else (shd if (br and not tl) else mid))


def banner(house, w=104, h=50, chosen=False):
    """a House banner on the rod: a sleeve, woven cloth with a fold near each side, a running-stitch trim three
    pixels in from the edge, the device embroidered at the top, a pointed hem with gold fringe"""
    ramp, thread = HOUSES[house]
    base = 4 if chosen else 3
    pt = 6
    S = Spr(w + 3, h + 9)
    cx = (w - 1) / 2

    def hem(x):
        return (h - 1 - pt) + round(pt * (1 - abs(x - cx) / cx))
    folds = [round(w * 0.12), round(w * 0.88) - 2]

    def dip(x):
        return 1 if any(abs(x - f) <= 1 for f in folds) else 0
    cloth = {(x, y) for x in range(w) for y in range(h + 1) if y <= hem(x) + dip(x)}
    for (x, y) in cloth:
        for (a, b, al) in ((2, 3, 0.42), (3, 4, 0.2)):
            k = (x + a, y + b)
            if k not in cloth and S.get(*k)[3] < al * 255:
                S.set(k[0], k[1], rgba(SHADOW, round(al * 255)))
    top = 5
    for (x, y) in cloth:
        t = base
        if y >= top:
            q = (y - top) / max(1, h - top)
            for f in folds:
                wl = 1 + int(q * 2.4)
                ws = 1 + int(q * 3.4)
                if f - wl < x <= f:
                    t = base + 1
                elif f < x <= f + ws:
                    t = base - 1
        S.set(x, y, ramp[max(0, min(5, t))])
    # the sleeve round the rod
    for x in range(w):
        S.set(x, 0, ramp[0]); S.set(x, 1, ramp[min(5, base + 1)]); S.set(x, 2, ramp[base]); S.set(x, 3, ramp[base - 1])
        S.set(x, 4, ramp[base - 2] if x % 3 else ramp[base - 1])
    # trim: a running stitch three pixels in from the sides and the hem
    lit, mid, shd = thread
    i = 0
    for y in range(top + 2, hem(3) - 2):
        if i % 3 != 2:
            S.set(3, y, mid); S.set(w - 4, y, mid)
        i += 1
    for x in range(3, w - 3):
        if (x - 3) % 3 != 2:
            S.set(x, hem(x) - 3, mid)
    for (x, y) in cloth:
        if any((x + a, y + b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            S.set(x, y, ramp[0])
    for x in range(1, w - 1):
        if x % 2 == 0:
            y = max(yy for (xx, yy) in cloth if xx == x)
            for k, c in ((1, BRASS[6]), (2, BRASS[4]), (3, BRASS[2])):
                S.set(x, y + k, c)
    embroider(S, DEVICES[house], (w - 15) // 2, 8, thread, ramp[max(0, base - 2)])
    return S


for _h in HOUSES:
    ASSETS['banner-' + _h] = (lambda hh: (lambda: banner(hh)))(_h)
    ASSETS['banner-' + _h + '-on'] = (lambda hh: (lambda: banner(hh, h=60, chosen=True)))(_h)


def rod(w=600):
    """the brass banner rod with a ball finial at each end"""
    S = Spr(w, 9)
    for x in range(6, w - 6):
        for y, c in ((1, BRASS[0]), (2, BRASS[6]), (3, BRASS[5]), (4, BRASS[3]), (5, BRASS[0])):
            S.set(x, y, c)
        if x % 11 == 0:
            S.set(x, 2, BRASS[7])
    ball_rows = ['..kkk..',
                 '.khHmk.',
                 'khHmmdk',
                 'kHmmmdk',
                 'kmmmddk',
                 '.kmddk.',
                 '..kkk..']
    for x0 in (0, w - 7):
        S.grid(x0, 0, ball_rows, {'k': BRASS[0], 'h': BRASS[7], 'H': BRASS[6], 'm': BRASS[5], 'd': BRASS[3]})
    S.shadow(1, 2, 0.45)
    return S


ASSETS['banner-rod'] = lambda: rod(568)


# ================================================================ the muster roll on its rollers
def ball(S, x0, y0, r, ramp, idx):
    """a small turned ball, light from the top-left, outlined in ramp[0]; idx maps 6 light levels to the ramp"""
    n = int(2 * r + 1)
    c = r
    m = {(x, y) for y in range(n) for x in range(n) if (x - c) ** 2 + (y - c) ** 2 <= (r + 0.3) ** 2}
    for (x, y) in m:
        nx, ny = (x - c) / (r + 0.5), (y - c) / (r + 0.5)
        nz = math.sqrt(max(0.0, 1 - nx * nx - ny * ny))
        l = -nx * 0.55 - ny * 0.6 + nz * 0.58
        k = 0 if l < -0.25 else 1 if l < 0.05 else 2 if l < 0.32 else 3 if l < 0.58 else 4 if l < 0.8 else 5
        S.set(x0 + x, y0 + y, ramp[idx[k]])
    for (x, y) in m:
        if any((x + a, y + b) not in m for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            S.set(x0 + x, y0 + y, ramp[0])


def roller(L):
    """a turned walnut roller, rod length L, with brass collars and brass ball finials"""
    S = Spr(L + 26, 14)
    x0 = 13
    for x in range(x0, x0 + L):
        for y, c in ((2, WAL[0]), (3, WAL[6]), (4, WAL[7]), (5, WAL[6]), (6, WAL[5]), (7, WAL[4]), (8, WAL[3]), (9, WAL[2]), (10, WAL[0])):
            S.set(x, y, c)
        if (x * 7) % 23 < 3:
            S.set(x, 6, WAL[4])
        if (x * 5) % 31 < 4:
            S.set(x, 8, WAL[2])
        if x % 17 == 0:
            S.set(x, 4, WAL[6])
    col = [BRASS[0], BRASS[6], BRASS[7], BRASS[6], BRASS[5], BRASS[5], BRASS[4], BRASS[4], BRASS[3], BRASS[2], BRASS[1], BRASS[0]]
    for xc in (x0 - 2, x0 + L):
        for y, c in enumerate(col):
            S.set(xc, y, c)
            S.set(xc + 1, y, c if c == BRASS[0] else BRASS[max(1, BRASS.index(c) - 1)])
    ball(S, 0, 1, 5, BRASS, idx=(1, 2, 4, 5, 6, 7))
    ball(S, x0 + L + 2, 1, 5, BRASS, idx=(1, 2, 4, 5, 6, 7))
    S.shadow(1, 2, 0.45)
    return S


def muster_roll(w=240, h=368):
    """the roll: parchment held flat between two rollers; the sheet curls under each roller.
    The paper's left edge sits at x 15 of the sprite, its top roller's rod on rows 2-10."""
    S = Spr(w + 30, h + 4)
    paper, _ = parchment(w, h - 10, kind='lo', seed=31, shadow=(2, 2), folds=(), stains=1, rim=3, mottle='edge', fibres=0.7)
    S.blit(paper, 15, 5)
    T = PAPER['lo']
    for x in range(16, w + 14):
        S.set(x, 11, T['rim2']); S.set(x, 12, T['rim1'])
        S.set(x, h - 12, T['rim2']); S.set(x, h - 13, T['rim1'])
    r = roller(w + 4)
    S.blit(r, 0, 0)
    S.blit(r, 0, h - 13)
    return S


ASSETS['muster-roll'] = muster_roll


def rule_line(w=208, seed=2):
    """a hand-ruled sepia line between roll entries, the ink thinning where the pen ran dry"""
    rnd = random.Random(seed)
    S = Spr(w, 1)
    x = 0
    while x < w:
        ln = rnd.randint(12, 40)
        for i in range(x, min(w, x + ln)):
            S.set(i, 0, VEL[3])
        x += ln
        if x < w:
            S.set(x, 0, VEL[4]); x += 1
    return S


ASSETS['rule-208'] = rule_line


def row_mark(w=232, h=56):
    """the selected roll entry: a paler wash, and a brass clip gripping the roll's left edge"""
    S = Spr(w + 2, h + 2)
    T = PAPER['hi']
    for y in range(1, h - 1):
        for x in range(6, w):
            S.set(x, y, T['body'])
    for x in range(6, w):
        S.set(x, 0, T['rim1']); S.set(x, h - 1, T['rim1'])
    for y in range(1, h - 1):
        S.set(w - 1, y, T['rim1'])
    clip = ['kkkkkkkk',
            'khHHHHmk',
            'kHmmmmdk',
            'kHmkkmdk',
            'kHmkhmdk',
            'kHmmmmdk',
            'kHmmmmdk',
            'kmddddsk',
            'kkkkkkkk']
    cy = (h - 9) // 2
    S.grid(0, cy - 8, ['kk', 'kH', 'kH', 'kH', 'kH', 'kH', 'kH', 'kH'], {'k': BRASS[0], 'H': BRASS[5]})
    S.grid(0, cy, clip, {'k': BRASS[0], 'h': BRASS[7], 'H': BRASS[6], 'm': BRASS[5], 'd': BRASS[3], 's': BRASS[2]})
    S.grid(0, cy + 9, ['kH', 'kH', 'kH', 'kH', 'kH', 'kH', 'kH', 'kk'], {'k': BRASS[0], 'H': BRASS[4]})
    return S


ASSETS['row-mark'] = row_mark


# ================================================================ stars
STAR_S = ['....k....',
          '...kHk...',
          '...kHk...',
          'kkkHHmkkk',
          'kHHHmmmdk',
          '.kHmmmdk.',
          '..kmmmk..',
          '.kmdkdmk.',
          '.kdk.kdk.',
          '.kk...kk.']
STAR_L = ['......k......',
          '.....kHk.....',
          '.....kHk.....',
          '....kHHmk....',
          'kkkkkHmmmkkkk',
          'kHHHHmmmmmmdk',
          '.kHHmmmmmmdk.',
          '..kmmmmmmdk..',
          '...kmmmmdk...',
          '..kmmmkmmdk..',
          '..kmmk.kmdk..',
          '.kmdk...kddk.',
          '.kkk.....kkk.']


def star(rows, kind='gold'):
    S = Spr(len(rows[0]) + 1, len(rows) + 1)
    if kind == 'gold':
        S.grid(0, 0, rows, {'k': BRASS[1], 'H': BRASS[7], 'm': BRASS[5], 'd': BRASS[3]})
        S.shadow(1, 1, 0.3)
    else:   # an empty star: pressed into the paper, its lower-right lip catching the light
        m = grid_mask(rows)
        S.grid(0, 0, rows, {'k': VEL[2], 'H': None, 'm': None, 'd': None})
        for (x, y) in m:
            if rows[y][x] == 'k' and (x + 1, y + 1) not in m:
                S.set(x + 1, y + 1, VEL[6])
    return S


ASSETS['star-s'] = lambda: star(STAR_S)
ASSETS['star-s-empty'] = lambda: star(STAR_S, 'empty')
ASSETS['star-l'] = lambda: star(STAR_L)
ASSETS['star-l-empty'] = lambda: star(STAR_L, 'empty')


def sparkle():
    """the glint round a new star: four inked rays and four specks, dark enough to read on the light letter"""
    rows = ['........d........',
            '........o........',
            '........o........',
            '..d...........d..',
            '...o.........o...',
            '.................',
            '.................',
            '.................',
            'doo...........ood',
            '.................',
            '.................',
            '.................',
            '...o.........o...',
            '..d...........d..',
            '........o........',
            '........o........',
            '........d........']
    S = Spr(17, 17)
    S.grid(0, 0, rows, {'o': BRASS[4], 'd': BRASS[3]})
    return S


ASSETS['sparkle'] = sparkle


# ================================================================ the reveal frame (live render window)
def frame_ornate(n=184):
    """The reveal frame: a cast brass bezel 12 px deep round a 160 px window. From the outside in: an outline,
    a bead (lit on its top-left), a fine groove, a twisted-rope band, a dark groove, an inner bevel that faces
    away from the light at the top-left, and a 1 px lip that overlaps the render. Cast corner plates carry a domed
    boss and a leaf running along each side; a diamond stud sits at the middle of every side.
    The game draws the unit in the window (drawEntity under a scissor); the bust in the mock is a placeholder."""
    S = Spr(n + 3, n + 4)
    for x in range(3, n + 3):
        for k, a in ((0, 150), (1, 80), (2, 35)):
            S.set(x, n + k, rgba(SHADOW, a))
    for y in range(3, n):
        for k, a in ((0, 130), (1, 70), (2, 30)):
            S.set(n + k, y, rgba(SHADOW, a))
    rings = {0: (BRASS[0], BRASS[0]), 1: (BRASS[7], BRASS[4]), 2: (BRASS[6], BRASS[3]), 3: (BRASS[4], BRASS[2]),
             4: (BRASS[1], BRASS[1]), 9: (BRASS[1], BRASS[1]), 10: (BRASS[3], BRASS[6]), 11: (BRASS[0], BRASS[0])}
    rope = [BRASS[6], BRASS[5], BRASS[4], BRASS[2]]
    for y in range(n):
        for x in range(n):
            d = min(x, y, n - 1 - x, n - 1 - y)
            if d > 11:
                continue
            side_tl = (y == d and x < n - 1 - d) or (x == d and y < n - 1 - d)
            S.set(x, y, (rings[d][0] if side_tl else rings[d][1]) if d in rings else rope[(x + y) % 4])
    # corner ornaments, built in screen space per corner so the light stays on the top-left
    for (fx, fy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
        def M(x, y):
            return (n - 1 - x if fx else x, n - 1 - y if fy else y)
        plate = {M(x, y) for y in range(13) for x in range(13) if x + y <= 19}
        orn = set(plate)
        for (x, y) in plate:
            tl = (x - 1, y) not in plate or (x, y - 1) not in plate
            br = (x + 1, y) not in plate or (x, y + 1) not in plate
            S.set(x, y, BRASS[6] if (tl and not br) else (BRASS[3] if br else BRASS[5]))
        bc = M(5.5, 5.5)
        boss = {(x, y) for (x, y) in plate if (x - bc[0]) ** 2 + (y - bc[1]) ** 2 <= 3.6 ** 2}
        for (x, y) in boss:
            l = -(x - bc[0]) / 4.1 - (y - bc[1]) / 4.1
            S.set(x, y, BRASS[7] if l > 0.75 else BRASS[6] if l > 0.15 else BRASS[5] if l > -0.45 else BRASS[3])
        for (x, y) in boss:
            if any((x + a, y + b) not in boss for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                S.set(x, y, BRASS[1])
        gx, gy = round(bc[0] - 1.5), round(bc[1] - 1.5)
        S.set(gx, gy, BRASS[7])
        for horiz in (True, False):
            cl = M(0, 5)[1] if horiz else M(5, 0)[0]
            for u in range(10):
                half = max(0, round(3.2 * (1 - u / 10) ** 0.8))
                for v in range(-half, half + 1):
                    X, Y = M(13 + u, 5 + v) if horiz else M(5 + v, 13 + u)
                    s = (Y - cl) if horiz else (X - cl)
                    c = BRASS[6] if s < 0 else (BRASS[5] if s == 0 else BRASS[3])
                    if s == 0 and u % 2 == 0 and half > 0:
                        c = BRASS[4]
                    S.set(X, Y, c)
                    orn.add((X, Y))
        out = {(x + a, y + b) for (x, y) in orn for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - orn
        for (x, y) in out:
            if 0 <= x < n and 0 <= y < n and min(x, y, n - 1 - x, n - 1 - y) <= 11:
                S.set(x, y, BRASS[0])
    stud = ['...k...',
            '..khk..',
            '.khHmk.',
            'khHmmdk',
            '.kmmdk.',
            '..kdk..',
            '...k...']
    pal = {'k': BRASS[0], 'h': BRASS[7], 'H': BRASS[6], 'm': BRASS[5], 'd': BRASS[3]}
    for (sx, sy) in ((n // 2 - 3, 3), (n // 2 - 3, n - 10), (3, n // 2 - 3), (n - 10, n // 2 - 3)):
        S.grid(sx, sy, stud, pal)
    return S


ASSETS['frame-ornate-184'] = lambda: frame_ornate(184)


def back_square(w, tones, seed=8, cy=0.36):
    s = Spr(w, w)
    nz = Noise(seed + w, 6)
    for y in range(w):
        for x in range(w):
            dx = (x - (w - 1) / 2) / (w * 0.55); dy = (y - w * cy) / (w * 0.62)
            r = math.sqrt(dx * dx + dy * dy)
            v = (1.0 - r) * (len(tones) - 0.4) + (nz(x, y) - 0.5) * 0.9
            s.set(x, y, tones[max(0, min(len(tones) - 1, int(v + 0.6)))])
    return s


ASSETS['frame-back-160'] = lambda: back_square(160, ['#141a1d', '#182024', '#1d262a', '#222d31', '#283439', '#2e3b40'])


# ================================================================ the opened letter (the focal sheet)
ASSETS['letter-hi'] = lambda: parchment(304, 368, kind='hi', seed=14, shadow=(4, 6), folds=(118,), stains=0, rim=3,
                                        mottle='edge', fibres=0.6)[0]


# ================================================================ the bundle of sealed letters
def envelope(w, h, seed, tone='lo'):
    """one sealed letter seen from the back: the four flaps meet in creases, the top flap's point
    a little below the middle"""
    S, paper = parchment(w, h, kind=tone, seed=seed, shadow=(1, 2), deckle=False, rim=2, stains=0, fibres=0.4, mottle='edge')
    T = PAPER[tone]
    cx = (w - 1) / 2
    vy = round(h * 0.56)
    for x in range(2, w - 2):
        t = abs(x - cx) / cx
        y = round(2 + (vy - 2) * (1 - t))
        if (x, y) in paper:
            S.set(x, y, T['edge'])
            if (x, y + 1) in paper:
                S.set(x, y + 1, T['shade'])
        yb = round(h - 3 - (h * 0.42) * (1 - t))
        if (x, yb) in paper and t > 0.12:
            S.set(x, yb, T['crease'])
    return S, vy


def letter_bundle(W=240, H=218):
    """the letters still to open: a loose heap under a bundle tied with twine, the House seal on top"""
    S = Spr(W, H)
    for (x, y, w, h, sd) in ((66, 0, 146, 94, 43), (8, 16, 150, 96, 44), (0, 104, 140, 90, 41),
                             (94, 114, 144, 96, 42), (38, 98, 150, 94, 46)):
        e, _ = envelope(w, h, sd)
        S.blit(e, x, y)
    x, y, w, h = 42, 48, 156, 100
    top, vy = envelope(w, h, 45)
    S.blit(top, x, y)
    tx, ty = x + 118, y + 36
    for i in range(w + 2):
        S.set(x - 1 + i, ty, '#8a6a3c' if i % 3 else '#6b4f2a')
        S.set(x - 1 + i, ty + 1, '#5c4225' if i % 4 == 0 else '#7a5c33')
    for j in range(h + 2):
        S.set(tx, y - 1 + j, '#8a6a3c' if j % 3 else '#6b4f2a')
        S.set(tx + 1, y - 1 + j, '#5c4225' if j % 4 == 0 else '#7a5c33')
    S.grid(tx - 2, ty - 2, ['.kkk..', 'kmmmk.', 'kmhdmk', 'kmddk.', '.kkk..'],
           {'k': '#4a331b', 'm': '#9a7a46', 'h': '#b59258', 'd': '#6b4f2a'})
    for k in range(7):
        S.set(tx + 3 + k, ty + 3 + k // 2, '#7a5c33'); S.set(tx + 4 + k, ty + 3 + k // 2, '#5c4225')
        S.set(tx - 1 - k // 2, ty + 3 + k, '#7a5c33'); S.set(tx - 2 - k // 2, ty + 3 + k, '#5c4225')
    seal = wax_seal(11, CROWN, seed=7, drips=((2.2, 3), (0.8, 2)))
    S.blit(seal, x + w // 2 - seal.w // 2, y + vy - seal.h // 2 + 1)
    return S


ASSETS['letter-bundle'] = letter_bundle


# ================================================================ coins, padlock, die, table icon
def coin_stack():
    """three gold crowns stacked a little out of true, light from the top-left; the edges are milled"""
    S = Spr(19, 17)

    def coin(ox, oy):
        face = {(x, y) for y in range(6) for x in range(13) if ((x - 6) / 6.5) ** 2 + ((y - 2.5) / 3.0) ** 2 <= 1}
        body = face | {(x, y + d) for (x, y) in face for d in (1, 2)}
        out = {(x + a, y + b) for (x, y) in body for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - body
        for (x, y) in out:
            S.set(ox + x, oy + y, BRASS[1])
        for (x, y) in body - face:
            S.set(ox + x, oy + y, BRASS[3] if x % 2 else BRASS[2])
        for (x, y) in face:
            v = (x - 6) * 0.45 + (y - 2.5) * 1.1
            S.set(ox + x, oy + y, BRASS[6] if v < -1.6 else (BRASS[4] if v > 1.9 else BRASS[5]))
        rim = {p for p in face if any((p[0] + a, p[1] + b) not in face for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
        for (x, y) in rim:
            S.set(ox + x, oy + y, BRASS[4] if y >= 3 else BRASS[6])
        S.set(ox + 4, oy + 1, BRASS[7]); S.set(ox + 5, oy + 1, BRASS[7])
    coin(3, 7)
    coin(1, 4)
    coin(4, 1)
    S.shadow(1, 1, 0.4)
    return S


ASSETS['coin-stack'] = coin_stack


def coin_small():
    rows = ['.kkk.', 'kHmdk', 'kmmdk', 'kmddk', '.kkk.']
    S = Spr(5, 5)
    S.grid(0, 0, rows, {'k': BRASS[1], 'H': BRASS[7], 'm': BRASS[5], 'd': BRASS[3]})
    return S


ASSETS['coin-s'] = coin_small


def padlock():
    rows = ['..kkk..',
            '.k...k.',
            '.k...k.',
            'kkkkkkk',
            'kmmmmdk',
            'kmmkmdk',
            'kmmkmdk',
            'kmddddk',
            'kkkkkkk']
    S = Spr(7, 9)
    S.grid(0, 0, rows, {'k': INK[2], 'm': INK[4], 'd': INK[3]})
    return S


ASSETS['padlock'] = padlock


def die():
    """the Odds tag's ink icon: a die showing five, its top face lit"""
    rows = ['..kkkkkkk',
            '.kLLLLLkk',
            'kkkkkkkdk',
            'kpppppkdk',
            'kpkpkpkdk',
            'kppkppkdk',
            'kpkpkpkk.',
            'kpppppkk.',
            'kkkkkkk..']
    S = Spr(9, 9)
    S.grid(0, 0, rows, {'k': INK[1], 'L': VEL[7], 'p': VEL[5], 'd': INK[4]})
    return S


ASSETS['die'] = die


def icon_table():
    """the War table plaque icon: the table from the front with a map spread on it"""
    rows = ['..............',
            '..............',
            '..kkkkkkkkkk..',
            '..kpppppppPk..',
            '..kpiippiPPk..',
            '..kppiipppPk..',
            'kkkkkkkkkkkkkk',
            'kwwwwwwwwwwwwk',
            'kddddddddddddk',
            '.kk.......kkk.',
            '.kw.......kwk.',
            '.kw.......kwk.',
            '.kd.......kdk.',
            '.kk.......kkk.']
    S = Spr(14, 14)
    S.grid(0, 0, rows, {'k': '#1a1009', 'p': VEL[6], 'P': VEL[4], 'i': INK[3], 'w': OAK[8], 'd': OAK[5]})
    return S


ASSETS['icon-table'] = icon_table


def padlock_brass():
    """the lock over a portrait that cannot be recruited yet: brass, so it reads on the dark window"""
    rows = ['..kkkkk..',
            '.kHk.kdk.',
            '.kH...dk.',
            '.kH...dk.',
            'kkkkkkkkk',
            'kHHHHHHmk',
            'kHmmkmmdk',
            'kHmmkmmdk',
            'kHmmmmmdk',
            'kmmmmmddk',
            'kkkkkkkkk']
    S = Spr(10, 12)
    S.grid(0, 0, rows, {'k': BRASS[0], 'H': BRASS[6], 'm': BRASS[5], 'd': BRASS[3]})
    S.shadow(1, 1, 0.5)
    return S


ASSETS['padlock-brass'] = padlock_brass

LEA = ['#1e120b', '#2f1c10', '#452a17', '#5e3a1f', '#7a4d29', '#966335', '#b47c45']


def pouch():
    """the purse: a leather pouch cinched with a drawstring, its ruffled mouth open, two crowns spilled in front"""
    S = Spr(30, 26)
    body = {(x, y) for y in range(26) for x in range(24) if ((x - 11.5) / 9.6) ** 2 + ((y - 15.5) / 7.6) ** 2 <= 1}
    neck = {(x, y) for y in range(5, 10) for x in range(8, 16)}
    ruff = set()
    for x in range(6, 18):
        top = 2 + (1 if x % 3 == 0 else 0)
        for y in range(top, 6):
            ruff.add((x, y))
    shape = body | neck | ruff
    for (x, y) in shape:
        if (x, y) in ruff:
            c = LEA[5] if y <= 3 else (LEA[4] if x < 12 else LEA[3])
        else:
            nx, ny = (x - 11.5) / 9.6, (y - 15.5) / 7.6
            l = -nx * 0.6 - ny * 0.55 + math.sqrt(max(0, 1 - nx * nx - ny * ny)) * 0.55
            c = LEA[5] if l > 0.62 else LEA[4] if l > 0.3 else LEA[3] if l > -0.05 else LEA[2]
        S.set(x, y, c)
    # gathered creases running down from the cinch
    for (x0, dx) in ((9, -0.35), (12, 0.0), (15, 0.4)):
        for k in range(6):
            X, Y = round(x0 + dx * k), 10 + k
            if (X, Y) in body:
                S.set(X, Y, LEA[2])
                if (X - 1, Y) in body:
                    S.set(X - 1, Y, LEA[5] if X < 12 else LEA[4])
    # the drawstring round the neck, tied off with two hanging ends
    for x in range(7, 17):
        S.set(x, 8, LEA[1] if x % 2 else '#7a5c33'); S.set(x, 9, '#5c4225')
    S.grid(15, 7, ['kk.', 'kmk', '.k.'], {'k': '#4a331b', 'm': '#9a7a46'})
    for k in range(5):
        S.set(17 + k // 2, 10 + k, '#7a5c33'); S.set(16 + k // 3, 10 + k, '#5c4225')
    out = {(x + a, y + b) for (x, y) in shape for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - shape
    for (x, y) in out:
        S.set(x, y, LEA[0])
    # the mouth of the pouch: dark inside, a coin rim showing
    for x in range(8, 16):
        S.set(x, 3, LEA[1]); S.set(x, 4, LEA[0] if x in (8, 15) else LEA[1])
    for x in range(10, 14):
        S.set(x, 3, BRASS[5]); 
    S.set(10, 3, BRASS[6]); S.set(13, 3, BRASS[3])

    def coin(ox, oy):
        face = {(x, y) for y in range(4) for x in range(7) if ((x - 3) / 3.6) ** 2 + ((y - 1.5) / 2.0) ** 2 <= 1}
        edge = {(x, y + 1) for (x, y) in face} - face
        for (x, y) in {(x + a, y + b) for (x, y) in face | edge for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - face - edge:
            S.set(ox + x, oy + y, BRASS[1])
        for (x, y) in edge:
            S.set(ox + x, oy + y, BRASS[2] if x % 2 else BRASS[3])
        for (x, y) in face:
            S.set(ox + x, oy + y, BRASS[6] if (x + y) < 2 else (BRASS[3] if x + y > 6 else (BRASS[4] if x + y > 4 else BRASS[5])))
    coin(19, 19)
    coin(22, 15)
    S.shadow(1, 2, 0.45)
    return S


ASSETS['pouch'] = pouch


def coin_mid():
    """the coin beside a price on a brass button: a struck crown, outlined dark so it reads on brass"""
    rows = ['..kkk..',
            '.kHHmk.',
            'kHHmmdk',
            'kHmmmdk',
            'kmmmddk',
            '.kmddk.',
            '..kkk..']
    S = Spr(7, 7)
    S.grid(0, 0, rows, {'k': BRASS[0], 'H': BRASS[7], 'm': BRASS[6], 'd': BRASS[4]})
    return S


ASSETS['coin-m'] = coin_mid
