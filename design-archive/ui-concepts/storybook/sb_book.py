"""The book itself: the closed cover on the War Table (storybook-01) and the pieces the open spread reuses.
Painted at GUI scale 1 with the v4 rules: named hue-shifted ramps, light from the top left, shading in clusters,
dithering only for large falloffs; brass gets an outline, a lit bevel, a brushed face, engraved lines (the cut
dark, its far wall lit), domed rivets and tarnish in the recesses; gilt is gold leaf pressed into the leather
(the leaf flat and bright, worn off in places, the cut wall dark above and to the left).

python sb_book.py [name ...]   writes art/<name>.png"""
import math, random, sys
from sb_common import *
from v4a_furn import screw, engrave, arc, brushed, tarnish, plaque, key_engraved, brass_button, vellum_button
from v4a_paper import wax_seal, parchment

ASSETS = {}

# ================================================================ the Hollow mark
# The company standard as the game hangs a banner: a crossbar with a finial, the cloth hanging from it with a
# swallowtail, the pole below; through the cloth a round hollow, the empty place where a crown goes. Kort
# approved it as the company's look (2026-10-18); the pole stands on the centre line, so the mark is symmetric
# (coordinator's ruling).
HOLLOW = ['.......x.......',
          '......xxx......',
          'xxxxxxxxxxxxxxx',
          '.xxxxxxxxxxxxx.',
          '.xxxxxxxxxxxxx.',
          '.xxxxx...xxxxx.',
          '.xxxx.....xxxx.',
          '.xxxx.....xxxx.',
          '.xxxx.....xxxx.',
          '.xxxxx...xxxxx.',
          '.xxxxxxxxxxxxx.',
          '.xxxxxxxxxxxxx.',
          '.xxxxxxxxxxxxx.',
          '.xxxxxx.xxxxxx.',
          '.xxxxx.x.xxxxx.',
          '.xxxx..x..xxxx.',
          '.xxx...x...xxx.',
          '.xx....x....xx.',
          '.......x.......',
          '......xxx......']

# The same mark cut down to eleven rows for the small seal (radius 14), so that all of it and its cast shadow
# sit inside the sunk field and nothing reaches the beaded border: the finial one pixel, the crossbar, one row of
# cloth over the hollow, the hollow three rows, one under it, the notch, two rows of tails, the pole's foot.
HOLLOW_SMALL = ['.....x.....',
                'xxxxxxxxxxx',
                '.xxxxxxxxx.',
                '.xxx...xxx.',
                '.xx.....xx.',
                '.xxx...xxx.',
                '.xxxxxxxxx.',
                '.xxxx.xxxx.',
                '.xxx.x.xxx.',
                '.xx..x..xx.',
                '....xxx....']


# ================================================================ gilt and blind tooling
def gilt(S, pts, rnd, worn=0.08, glint=0.10):
    """gold leaf pressed into leather: the leaf bright and flat, a dark cut wall above and to the left of it,
    a warm lit wall below and to the right; rubbed off in places (the leather shows through)"""
    ps = set(pts)
    for (x, y) in ps:
        for (dx, dy, c) in ((-1, -1, LEA[0]), (0, -1, LEA[1]), (-1, 0, LEA[1])):
            if (x + dx, y + dy) not in ps and S.get(x + dx, y + dy)[3]:
                S.set(x + dx, y + dy, c)
    for (x, y) in ps:
        if (x + 1, y + 1) not in ps and S.get(x + 1, y + 1)[3]:
            S.set(x + 1, y + 1, LEA[6])
    for (x, y) in ps:
        r = rnd.random()
        if r < worn:
            S.set(x, y, LEA[5] if rnd.random() < 0.5 else BRASS[2])
        elif r < worn + glint:
            S.set(x, y, BRASS[7])
        else:
            S.set(x, y, BRASS[5] if (x + y) % 5 else BRASS[6])


def blind(S, pts, deep=False):
    """blind tooling: a line pressed into the leather without gold; the cut dark, the far wall lit"""
    ps = set(pts)
    for (x, y) in ps:
        S.set(x, y, LEA[0] if deep else LEA[1])
    for (x, y) in ps:
        for (dx, dy) in ((1, 1), (0, 1), (1, 0)):
            if (x + dx, y + dy) not in ps:
                S.set(x + dx, y + dy, LEA[6] if (dx, dy) == (1, 1) else LEA[5])
                break


def rect_pts(x0, y0, x1, y1, notch=0):
    """the outline of a rectangle; notch cuts each corner in a quarter circle of that radius (turned inward)"""
    pts = []
    for x in range(x0 + notch, x1 - notch + 1):
        pts += [(x, y0), (x, y1)]
    for y in range(y0 + notch, y1 - notch + 1):
        pts += [(x0, y), (x1, y)]
    if notch:
        for (cx, cy, a0) in ((x0, y0, 0.0), (x1, y0, math.pi / 2), (x1, y1, math.pi), (x0, y1, 1.5 * math.pi)):
            pts += arc(cx, cy, notch, a0, a0 + math.pi / 2)
    return pts


# a corner tool: a small gilt fleuron, a stem and two leaves curling out of a bud (top-left orientation)
FLEURON = ['xx........',
           'x.x.......',
           '.x.xx.....',
           '..x..x....',
           '..x...x...',
           '...x...xx.',
           '...x......',
           '....xx....',
           '......x...',
           '.......x..']
# the centre tool of a line: a lozenge between two dots
LOZENGE = ['....x....',
           '...x.x...',
           'x.x...x.x',
           '...x.x...',
           '....x....']
# a sprig, the tail ornament under the title
SPRIG = ['.....x.....x.....',
         '...xx.x...x.xx...',
         '..x....x.x....x..',
         'xx......x......xx',
         '..x....x.x....x..',
         '...xx.x...x.xx...',
         '.....x.....x.....']


def grid_pts(rows, ox, oy, flipx=False, flipy=False):
    h = len(rows); w = max(len(r) for r in rows)
    out = []
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch == 'x':
                X = (w - 1 - i) if flipx else i
                Y = (h - 1 - j) if flipy else j
                out.append((ox + X, oy + Y))
    return out


# ================================================================ leather
def leather(S, mask, seed, base=3, light=None):
    """morocco on a board: a base tone, broad mottle where it has darkened with handling, a pebbled grain (each
    pebble lit on its upper left), the edges rubbed paler, scratches with a dark side; light(x, y) adds tones"""
    rnd = random.Random(seed)
    big = Noise(seed + 1, 38); mid = Noise(seed + 2, 9); fine = Noise(seed + 3, 2)
    xs = [p[0] for p in mask]; ys = [p[1] for p in mask]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    idx = {}
    for (x, y) in mask:
        t = base
        v = big(x, y)
        if v > 0.70:
            t -= 1
        elif v < 0.24:
            t += 1
        if mid(x, y) > 0.78:
            t -= 1
        d = min(x - x0, x1 - x, y - y0, y1 - y)
        if d < 7 and mid(x + 40, y) > 0.35 + d * 0.07:
            t += 1                    # rubbed edges
        if light:
            t += light(x, y)
        idx[(x, y)] = t
    # pebble grain
    for _ in range(int(len(mask) / 7)):
        x = rnd.randint(x0, x1); y = rnd.randint(y0, y1)
        if (x, y) in idx and (x + 1, y + 1) in idx:
            if fine(x, y) > 0.45:
                idx[(x, y)] += 1; idx[(x + 1, y + 1)] -= 1
            else:
                idx[(x, y)] -= 1
    for (x, y), t in idx.items():
        S.set(x, y, LEA[max(0, min(9, t))])
    # scratches: a light line with its dark side below
    for _ in range(int(len(mask) / 2600)):
        x = rnd.randint(x0 + 10, x1 - 10); y = rnd.randint(y0 + 10, y1 - 10)
        ang = rnd.uniform(-0.6, 0.6) + (math.pi / 2 if rnd.random() < 0.3 else 0)
        for i in range(rnd.randint(4, 13)):
            X = round(x + i * math.cos(ang)); Y = round(y + i * math.sin(ang))
            if (X, Y) in idx and (X, Y + 1) in idx:
                S.set(X, Y, LEA[min(9, idx[(X, Y)] + 2)]); S.set(X, Y + 1, LEA[max(0, idx[(X, Y + 1)] - 1)])
    return idx


# ================================================================ brass corner pieces
def corner_mask(n=38):
    """a corner protector for the top-left corner: the two arms run n px along the edges; its inner edge is an
    ogee (a convex lobe, a concave cove, then the arm), as bookbinders cut them"""
    m = set()
    for y in range(n):
        for x in range(n):
            if (x < 9 and y < n) or (y < 9 and x < n):
                m.add((x, y))
                continue
            if math.hypot(x, y) <= 21.5 and not (math.hypot(x - 21, y - 21) < 8.5):
                m.add((x, y))
    out = set()
    for (x, y) in m:
        if x >= n - 1 and y > 6:
            continue
        if y >= n - 1 and x > 6:
            continue
        out.add((x, y))
    return out


def brass_corner(S, ox, oy, flipx, flipy, rnd, n=38):
    """paint a corner piece with (ox, oy) as the top left of its box, mirrored as told; light from the top left
    whatever the mirror, so the bevels are worked out after mirroring"""
    base = corner_mask(n)
    m = set()
    for (x, y) in base:
        X = (n - 1 - x) if flipx else x
        Y = (n - 1 - y) if flipy else y
        m.add((X, Y))
    for (x, y) in m:
        for (dx, dy, a) in ((1, 1, 150), (2, 2, 80)):
            k = (x + dx, y + dy)
            if k not in m:
                S.over(ox + k[0], oy + k[1], SH, a)
    for (x, y) in m:
        up = (x, y - 1) not in m; lf = (x - 1, y) not in m
        dn = (x, y + 1) not in m; rt = (x + 1, y) not in m
        if up or lf:
            c = BRASS[7] if (up and lf) else BRASS[6]
        elif dn or rt:
            c = BRASS[2]
        else:
            up2 = (x, y - 2) not in m; lf2 = (x - 2, y) not in m
            dn2 = (x, y + 2) not in m; rt2 = (x + 2, y) not in m
            if up2 or lf2:
                c = BRASS[6]
            elif dn2 or rt2:
                c = BRASS[3]
            else:
                c = BRASS[4]
        S.set(ox + x, oy + y, c)
    for (x, y) in m:
        for (dx, dy) in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            k = (x + dx, y + dy)
            if k not in m and 0 <= k[0] < n and 0 <= k[1] < n:
                S.set(ox + k[0], oy + k[1], BRASS[0])
    ms = sorted(m)
    for _ in range(int(len(m) / 6)):
        x, y = rnd.choice(ms)
        if S.get(ox + x, oy + y)[:3] == rgba(BRASS[4])[:3]:
            S.set(ox + x, oy + y, BRASS[5] if rnd.random() < 0.6 else BRASS[3])
    # an engraved line following the inner edge, three px in
    keep = []
    for (x, y) in m:
        near = any((x + dx, y + dy) not in m for dx in range(-3, 4) for dy in range(-3, 4) if abs(dx) + abs(dy) <= 3)
        edge2 = any((x + dx, y + dy) not in m for dx in range(-2, 3) for dy in range(-2, 3) if abs(dx) + abs(dy) <= 2)
        if near and not edge2:
            bx = x if not flipx else n - 1 - x
            by = y if not flipy else n - 1 - y
            if bx > 3 and by > 3:
                keep.append((ox + x, oy + y))
    engrave(S, keep)
    for (x, y) in ((2, 2), (2, 26), (26, 2)):
        X = (n - 5 - x) if flipx else x
        Y = (n - 5 - y) if flipy else y
        screw(S, ox + X, oy + Y, rnd.choice(['-', '|', '/', 'x']))
    for (x, y) in ((8, 8), (15, 3), (3, 15)):
        X = (n - 1 - x) if flipx else x
        Y = (n - 1 - y) if flipy else y
        tarnish(S, ox + X, oy + Y, 2.4, rnd, 6, keep=lambda X2, Y2: (X2 - ox, Y2 - oy) not in m)


# ================================================================ the closed book
COVER_W, COVER_H, EDGE_T = 300, 390, 18
BOOK_AT = (330, 80)          # where the (0, 0) of the sprite sits on the page
COVER_CX = 156               # the centre line of the tooling, in cover px (the joint takes the left 15)
MEDAL_Y = 184                # the centre of the medallion and the seal
CART = (244, 312)            # the cartouche, top and bottom
TITLE_RULES = (42, 82)

ROSETTE = ['.x.',
           'x.x',
           '.x.']
CORNER_TOOL = ['xxx...........',
               'x..x..........',
               'x.x.xx........',
               '.x...x.x......',
               '..xx..x.x.....',
               '....x..x......',
               '.....xx..xx...',
               '...x...x...x..',
               '..x.x...x...x.',
               '...x.....x.x..',
               '..........x...',
               '...........x..',
               '............x.',
               '.............x']


def lift_at(x, y, W, H, L=7.0):
    """how far the wind has lifted the board at (x, y): nothing along the spine and the head, rising to L at
    the lower fore-edge corner (the board flexes a little before it goes)"""
    s = max(0.0, min(1.0, (x - (W - 120)) / 120.0)) ** 2
    t = max(0.0, min(1.0, (y - (H - 190)) / 190.0)) ** 1.5
    return L * s * t


def cover_top(seed=5):
    """the front board as seen from above, plus its front edge (3 px) below it"""
    W, H = COVER_W, COVER_H
    rnd = random.Random(seed)
    S = Spr(W, H + 3)
    cx = COVER_CX
    # the edge of the board along the tail, under the brass at both ends
    for x in range(W):
        S.set(x, H, LEA[2]); S.set(x, H + 1, LEA[1]); S.set(x, H + 2, LEA[1])

    def lightfn(x, y):
        d = math.hypot(x + 50, (y + 30) / 1.25)
        return 2 if d < 110 else (1 if d < 230 else 0)
    cover = {(x, y) for y in range(H) for x in range(W)}
    for (x, y) in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
        cover.discard((x, y))
    idx = leather(S, cover, seed, base=3, light=lightfn)
    for x in range(1, W - 1):
        S.set(x, 0, LEA[8] if x < W * 0.45 else LEA[7]); S.set(x, 1, LEA[5])
        S.set(x, H - 1, LEA[1]); S.set(x, H - 2, LEA[2])
    for y in range(1, H - 1):
        S.set(0, y, LEA[7] if y < H * 0.5 else LEA[6]); S.set(W - 1, y, LEA[1]); S.set(W - 2, y, LEA[2])
    # the spine side: the rounded back rolling away to the left, raised bands, the joint groove
    for y in range(2, H - 2):
        for x in range(0, 12):
            t = [5, 7, 8, 8, 7, 6, 6, 5, 5, 4, 4, 3][x]
            if idx.get((x, y), 3) < 3:
                t -= 1
            if y > H * 0.6:
                t -= 1
            S.set(x, y, LEA[max(0, t)])
    for by in (44, 118, 196, 274, 348):
        for x in range(0, 11):
            S.set(x, by - 1, LEA[9] if x in (2, 3, 4) else LEA[8] if x < 7 else LEA[6])
            S.set(x, by, LEA[8] if x in (2, 3) else LEA[7] if x < 7 else LEA[5])
            S.set(x, by + 1, LEA[5])
            S.set(x, by + 2, LEA[2])
            S.set(x, by + 3, LEA[1] if x < 9 else LEA[2])
    for y in range(1, H - 1):
        S.set(12, y, LEA[0]); S.set(13, y, LEA[1]); S.set(14, y, LEA[7] if y % 9 else LEA[6])
    # ---- blind tooling round the board: a double fillet with a dotted roll between
    blind(S, rect_pts(22, 10, W - 11, H - 11))
    blind(S, rect_pts(26, 14, W - 15, H - 15))
    for x in range(28, W - 16, 2):
        S.set(x, 12, LEA[1]); S.set(x, H - 13, LEA[1])
    for y in range(16, H - 16, 2):
        S.set(24, y, LEA[1]); S.set(W - 13, y, LEA[1])
    gx0, gy0, gx1, gy1 = 36, 24, W - 25, H - 25

    # ---- a blind diaper inside the gilt panel: a lattice of diagonal fillets with a rosette in every lozenge,
    # kept off the title, the medallion and the cartouche
    def clear(x, y):
        if TITLE_RULES[0] - 8 <= y <= TITLE_RULES[1] + 8:
            return False
        if math.hypot(x - cx, y - MEDAL_Y) < 58:
            return False
        if CART[0] - 8 <= y <= CART[1] + 22 and cx - 84 <= x <= cx + 84:
            return False
        return gx0 + 9 <= x <= gx1 - 9 and gy0 + 9 <= y <= gy1 - 9
    step = 24
    lat = []
    for y in range(gy0 + 9, gy1 - 8):
        for x in range(gx0 + 9, gx1 - 8):
            u, v = (x - cx) + (y - MEDAL_Y), (x - cx) - (y - MEDAL_Y)
            if (u % step == 0 or v % step == 0) and clear(x, y):
                lat.append((x, y))
    blind(S, lat)
    for y in range(gy0 + 12, gy1 - 11):
        for x in range(gx0 + 12, gx1 - 11):
            u, v = (x - cx) + (y - MEDAL_Y), (x - cx) - (y - MEDAL_Y)
            if u % step == step // 2 and v % step == step // 2 and clear(x - 2, y - 2) and clear(x + 2, y + 2):
                blind(S, grid_pts(ROSETTE, x - 1, y - 1))
    # ---- the gilt panel: two fillets with notched corners and a gilt dot roll between, a tool in each corner
    gilt(S, rect_pts(gx0, gy0, gx1, gy1, notch=5), rnd)
    gilt(S, rect_pts(gx0 + 4, gy0 + 4, gx1 - 4, gy1 - 4, notch=4), rnd, worn=0.12)
    dots = [(x, gy0 + 2) for x in range(gx0 + 8, gx1 - 7, 3)] + [(x, gy1 - 2) for x in range(gx0 + 8, gx1 - 7, 3)]
    dots += [(gx0 + 2, y) for y in range(gy0 + 8, gy1 - 7, 3)] + [(gx1 - 2, y) for y in range(gy0 + 8, gy1 - 7, 3)]
    gilt(S, dots, rnd, worn=0.1)
    # a quarter sunburst in each corner of the panel, echoing the medallion: two arcs and three rays
    for (fx, fy, sx, sy) in ((gx0 + 6, gy0 + 6, 1, 1), (gx1 - 6, gy0 + 6, -1, 1),
                             (gx0 + 6, gy1 - 6, 1, -1), (gx1 - 6, gy1 - 6, -1, -1)):
        fan = []
        for r in (6, 12):
            for (px, py) in arc(0, 0, r, 0.12, math.pi / 2 - 0.12):
                fan.append((fx + sx * px, fy + sy * py))
        for a in (0.42, math.pi / 4, math.pi / 2 - 0.42):
            for r in (8, 9, 10):
                fan.append((fx + sx * round(r * math.cos(a)), fy + sy * round(r * math.sin(a))))
        fan.append((fx + sx * round(15 * math.cos(math.pi / 4)), fy + sy * round(15 * math.sin(math.pi / 4))))
        fan.append((fx + sx * 2, fy + sy * 2))
        gilt(S, fan, rnd, worn=0.04)
    # ---- the title band: a rule over and under it, a lozenge at the centre of each, short rules at the ends
    for ry in TITLE_RULES:
        gilt(S, [(x, ry) for x in range(cx - 98, cx - 6)] + [(x, ry) for x in range(cx + 7, cx + 99)], rnd)
        gilt(S, grid_pts(LOZENGE, cx - 4, ry - 2), rnd, worn=0.0)
        gilt(S, [(cx - 102, ry), (cx + 102, ry)], rnd, worn=0.0)
    # ---- the medallion round the seal: a gilt ring, a finer ring, a ring of dots, rays between
    my = MEDAL_Y
    gilt(S, arc(cx, my, 40, 0, 2 * math.pi, 340), rnd, worn=0.05)
    gilt(S, arc(cx, my, 36, 0, 2 * math.pi, 320), rnd, worn=0.18)
    for k in range(36):
        a = k / 36 * 2 * math.pi + 0.04
        gilt(S, [(round(cx + 45 * math.cos(a)), round(my + 45 * math.sin(a)))], rnd, worn=0.0)
        if k % 2 == 0:
            gilt(S, [(round(cx + r * math.cos(a)), round(my + r * math.sin(a))) for r in (49, 50, 51, 52)], rnd,
                 worn=0.08)
        else:
            gilt(S, [(round(cx + 49 * math.cos(a)), round(my + 49 * math.sin(a)))], rnd, worn=0.0)
    # ---- the cartouche for the numeral and the name of this part
    c0, c1 = CART
    gilt(S, rect_pts(cx - 78, c0, cx + 78, c1, notch=7), rnd)
    gilt(S, rect_pts(cx - 75, c0 + 3, cx + 75, c1 - 3, notch=5), rnd, worn=0.2)
    gilt(S, [(x, (c0 + c1) // 2 - 1) for x in range(cx - 42, cx + 43) if abs(x - cx) > 4], rnd, worn=0.1)
    gilt(S, grid_pts(LOZENGE, cx - 4, (c0 + c1) // 2 - 3), rnd, worn=0.0)
    gilt(S, grid_pts(SPRIG, cx - 8, c1 + 11), rnd, worn=0.05)
    # a water stain, faint, low on the board
    for k in range(80):
        a = k / 80 * 2 * math.pi
        if 2.0 < a < 2.9:
            continue
        X, Y = round(226 + 16 * math.cos(a)), round(356 + 10 * math.sin(a))
        if (X, Y) in cover:
            S.set(X, Y, LEA[2])
    # ---- the brass corners
    n = 38
    brass_corner(S, 0, 0, False, False, rnd, n)
    brass_corner(S, W - n, 0, True, False, rnd, n)
    brass_corner(S, 0, H - n, False, True, rnd, n)
    brass_corner(S, W - n, H - n, True, True, rnd, n)
    for x in list(range(0, 9)) + list(range(W - 9, W)):
        S.set(x, H, BRASS[3]); S.set(x, H + 1, BRASS[2]); S.set(x, H + 2, BRASS[1])
    return S


def book_closed(seed=5):
    """the closed book seen from above and a little in front, as everything on the table is: the board, the tail
    (front board edge, the leaves edge-on, back board edge), the spine cap with its headband; the wind has the
    lower fore-edge corner of the board up a few px, so a sliver of the first leaf shows under it in shadow"""
    W, H, T = COVER_W, COVER_H, EDGE_T
    S = Spr(W + 16, H + T + 16)
    top = cover_top(seed)
    lift = {}
    for y in range(H + 3):
        for x in range(W):
            lift[(x, y)] = round(lift_at(x, min(y, H - 1), W, H))
    # ---- the shadow on the oak: stepped, a little longer under the lifted corner
    sil = {(x, y) for y in range(H + T) for x in range(W)}
    for (x, y) in sil:
        L = lift.get((x, min(y, H - 1)), 0)
        for (dx, dy, a) in ((4, 5, 125), (6, 7, 75), (8, 9, 38)):
            k = (x + dx + L, y + dy + L // 2)
            if k not in sil and S.get(*k)[3] < a:
                S.set(k[0], k[1], rgba(SH, a))
    # ---- the tail: the leaves edge-on between the boards, slightly cockled, grimy toward the ends
    nz = Noise(seed + 7, 7, 1); wav = Noise(seed + 8, 23, 4)
    PB = ['#6b5a3a', '#8f7a52', '#a8946a', '#b8a57c', '#cdbb92', '#dccb9f', '#e8dab4']
    for x in range(W):
        for y in range(H + 3, H + T - 3):
            S.set(x, y, LEA[0])
        for y in range(H + T - 3, H + T):
            S.set(x, y, LEA[2] if y == H + T - 3 else LEA[1])
    for y in range(H + 3, H + T - 3):
        k = y - (H + 3)
        n = T - 6
        for x in range(17, W - 3):
            off = 1 if wav(x, y) > 0.62 else 0
            kk = k - off
            if kk < 0:
                c = PB[0]
            elif kk == 0:
                c = PB[1]                      # in the shadow of the front board
            elif kk >= n - 1:
                c = PB[1]
            else:
                c = PB[5] if kk % 2 else PB[4]
                if kk == 1:
                    c = PB[3]
            if x > W - 14 or x < 24:
                c = PB[max(0, PB.index(c) - 2)] if c in PB else c
            if nz(x, y) > 0.82 and c in (PB[4], PB[5]):
                c = PB[3]
            S.set(x, y, c)
    # the tail of the spine: a rounded cap, the headband (red and cream stitches) tucked under it
    for y in range(H, H + T):
        for x in range(0, 17):
            S.set(x, y, LEA[3] if x < 3 else LEA[4] if x < 8 else LEA[3] if x < 13 else LEA[2])
    for x in range(4, 16):
        for y in range(H + 5, H + 11):
            S.set(x, y, WAX[2] if (x + y) % 3 else VEL[5])
        S.set(x, H + 5, WAX[3] if x % 2 else VEL[6])
        S.set(x, H + 11, LEA[1])
    for y in range(H, H + T):
        S.set(0, y, LEA[1]); S.set(16, y, LEA[1])
    # ---- under the lifted corner: the first leaf, seen from above, in the shadow of the board
    for x in range(W):
        L = lift[(x, H - 1)]
        for k in range(L):
            y = H + 2 - k
            # nearest the board is darkest
            c = VEL[2] if k == L - 1 else (VEL[3] if k >= L - 3 else VEL[5])
            if k == 0:
                c = VEL[6]                     # the very edge of the leaf catches the light
            S.set(x, y, c)
    # ---- the board, warped by the lift
    for y in range(H + 3):
        for x in range(W):
            p = top.px[x, y]
            if p[3]:
                S.set(x, y - lift[(x, y)], p)
    # the wind has got under the fore-edge: the first leaves puff out past the board in the lower half,
    # each a cream strip with a shadowed line under it, the gap under the board dark
    pf = Noise(seed + 21, 1, 9)
    for y in range(int(H * 0.38), H + 2):
        p = int(pf(0, y) * 3.2 * min(1.0, (y - H * 0.38) / 60.0) + 0.3)
        if p <= 0:
            continue
        for k in range(p):
            X = W + k
            c = VEL[6] if k == p - 1 else VEL[4]
            if (y + k) % 4 == 0:
                c = VEL[3]
            S.set(X, y, c)
        S.set(W + p, y, rgba(SH, 150))
        S.set(W + p + 1, y + 1, rgba(SH, 70))
    # the lifted edge shows a hair of the board underside where it rises
    for x in range(W - 120, W):
        L = lift[(x, H - 1)]
        if L >= 2:
            S.set(x, H + 3 - L, LEA[3])
    return S


ASSETS['book-closed'] = book_closed


def seal_hollow(r0=20, seed=11):
    return wax_seal(r0, HOLLOW, seed=seed, drips=((1.9, 4), (2.5, 2), (0.9, 2)))


ASSETS['seal-hollow'] = seal_hollow


# ================================================================ the open book (storybook-02)
OPEN_W, OPEN_H = 776, 440
OPEN_AT = (148, 72)                  # where the sprite's (0, 0) sits on the page
LEAF_Y0, LEAF_Y1 = 8, 428            # both leaves, head to tail (sprite px, end exclusive)
LEAF_L = (10, 384)                   # the left leaf, x from and to (exclusive)
LEAF_R = (388, 762)                  # the right leaf
VT = dict(edge='#86704a', rim2='#bba67a', rim1='#d6c69c', shade='#e8dbbb', body='#f1e7cb', light='#f6eedb',
          hi='#fcf8ec', fibre='#ddcfab', fibre_l='#fbf4e4', fox1='#e0c79a', fox2='#c7a573', back='#e5d7b1',
          fold='#a08a5d', deep='#6f5a38', rule='#e0d0a6')
PB = ['#6b5a3a', '#8f7a52', '#a8946a', '#b8a57c', '#cdbb92', '#dccb9f', '#e8dab4']


def open_leaf(S, x0, x1, y0, y1, side, seed):
    """one leaf of worn vellum: the body in broad lighter and darker patches, aged in lobes from its three free
    edges, falling away into the fold in dithered steps on the gutter side, grubby at the lower outer corner
    where the leaves are turned; fibres, a little foxing, and the scribe's ruled frame for the text block in
    the faintest ink, broken where the ruling pen lifted"""
    rnd = random.Random(seed)
    big = Noise(seed + 1, 46); mid = Noise(seed + 2, 13); fine = Noise(seed + 3, 3)
    body = {VT['body'], VT['light'], VT['shade']}
    for y in range(y0, y1):
        for x in range(x0, x1):
            u = x - x0
            dg, dout = ((x1 - 1 - x), u) if side == 'L' else (u, (x1 - 1 - x))
            de = min(dout, y - y0, y1 - 1 - y)
            b = big(x, y)
            pl = min(0.46, max(0.0, (b - 0.56) * 1.5))      # paler where the skin was scraped thinner
            ps = min(0.26, max(0.0, (0.34 - b) * 1.1))      # a touch darker where it is thicker
            r = rnd.random()
            c = VT['light'] if r < pl else (VT['shade'] if r < pl + ps else VT['body'])
            lob = mid(x + 70, y)
            band = 4 + lob * 13.0
            if de == 0:
                c = VT['rim2']
            elif de == 1:
                c = VT['rim1']
            elif de < 2 + lob * 4.0:
                c = VT['rim1'] if rnd.random() < 0.85 else VT['shade']
            elif de < band:
                c = VT['shade'] if rnd.random() < 0.9 else VT['rim1']
            elif de < band + 7 and rnd.random() < 0.6 * (1 - (de - band) / 7.0):
                c = VT['shade']                            # the toning breaks up into grain, no hard line
            elif rnd.random() < 0.025:
                c = VT['shade']
            cw = math.hypot(dout, y1 - 1 - y)
            if cw < 40 and fine(x, y) > 0.30 + cw / 70.0:
                c = VT['rim1'] if cw < 20 else VT['shade']
            if dg < 24:
                k = (24 - dg) / 24.0 * 4.0
                lo = int(k)
                if dith(x, y, k - lo):
                    lo += 1
                c = [c, VT['shade'], VT['rim1'], VT['rim2'], VT['fold']][min(4, lo)]
                if dg == 0:
                    c = VT['deep']
            S.set(x, y, c)
    for _ in range((x1 - x0) * (y1 - y0) // 300):
        x = rnd.randint(x0 + 4, x1 - 8); y = rnd.randint(y0 + 4, y1 - 5)
        cc = VT['fibre'] if rnd.random() < 0.7 else VT['fibre_l']
        for i in range(rnd.randint(2, 4)):
            p = S.get(x + i, y)
            if '#%02x%02x%02x' % p[:3] in body:
                S.set(x + i, y, cc)
    for _ in range(4):
        fx_ = rnd.randint(x0 + 30, x1 - 30); fy_ = rnd.randint(y0 + 40, y1 - 30)
        for _k in range(rnd.randint(3, 8)):
            X = fx_ + round(rnd.gauss(0, 2.4)); Y = fy_ + round(rnd.gauss(0, 1.7))
            S.set(X, Y, VT['fox2'] if rnd.random() < 0.4 else VT['fox1'])
    # the ruled frame of the text block, inside the margins and under the running head
    # 14 px in from the outer edge and 20 from the gutter: wide enough that the company row (46 px frame and a
    # want up to 277 px) stands clear of both rules (6 px on the left), aligned with the passage
    fx0, fx1 = (x0 + 14, x1 - 20) if side == 'L' else (x0 + 20, x1 - 14)
    fy0, fy1 = y0 + 30, y1 - 14

    def run(points):
        i = 0
        while i < len(points):
            n = rnd.randint(40, 120)
            for (x, y) in points[i:i + n]:
                if '#%02x%02x%02x' % S.get(x, y)[:3] in body:
                    S.set(x, y, VT['rule'])
            i += n + rnd.randint(2, 4)
    run([(x, fy0) for x in range(fx0 - 4, fx1 + 5)])
    run([(x, fy1) for x in range(fx0 - 4, fx1 + 5)])
    run([(fx0, y) for y in range(fy0 - 4, fy1 + 5)])
    run([(fx1, y) for y in range(fy0 - 4, fy1 + 5)])


def page_band(S, xs, ys, n, horizontal, seed):
    """a block of leaves seen on its edge: n lines of page edges, alternately lit and in shadow, a little
    cockled, the line nearest the board darkest"""
    wav = Noise(seed, 19, 3)
    for i in range(n):
        for t in (xs if horizontal else ys):
            off = 1 if wav(t, i * 7) > 0.66 else 0
            k = i + off
            if k >= n:
                continue
            c = PB[5] if k % 2 == 0 else PB[3]
            if k == n - 1:
                c = PB[1]
            if k == 0:
                c = PB[6]
            if horizontal:
                S.set(t, ys[0] + k, c)
            else:
                S.set(xs[0] + k, t, c)


def small_corner(S, x0, y0, fx, fy, rnd, n=16, a=4):
    """a small brass cap on a board's corner where the leather is turned in: an L of brass, bevelled from the
    top left, outlined, a rivet at the angle"""
    m = set()
    for i in range(n):
        for j in range(a):
            m.add((i, j)); m.add((j, i))

    def at(i, j):
        return (x0 + ((n - 1 - i) if fx else i), y0 + ((n - 1 - j) if fy else j))
    for (i, j) in m:
        X, Y = at(i, j)
        up = (i, j - 1) not in m
        lf = (i - 1, j) not in m
        dn = (i, j + 1) not in m
        rt = (i + 1, j) not in m
        if fy:
            up, dn = dn, up
        if fx:
            lf, rt = rt, lf
        c = BRASS[6] if (up or lf) else (BRASS[2] if (dn or rt) else BRASS[4])
        S.set(X, Y, c)
    for (i, j) in m:
        for (di, dj) in ((1, 0), (0, 1)):
            k = (i + di, j + dj)
            if k not in m and k[0] < n and k[1] < n:
                S.set(*at(*k), BRASS[0])
    X, Y = at(1, 1)
    S.set(X, Y, BRASS[7])


def book_open(seed=9):
    """the book open at page 2, seen from above and a little in front like everything on the table: the boards'
    turned-in leather round the leaves with a gilt fillet and brass corner caps; the two leaves of worn vellum
    falling into the fold; the leaves before this page a thin block on the left, the rest of the chapter and
    the sealed chapters a thick block on the right, its back section tied round with a cord; head and tail
    bands at the spine; the lower outer corner of the left leaf curled up to be turned back"""
    W, H = OPEN_W, OPEN_H
    rnd = random.Random(seed)
    S = Spr(W + 14, H + 14)
    # the shadow on the oak, stepped
    for (dx, dy, a) in ((4, 5, 125), (6, 7, 75), (8, 9, 38)):
        for y in range(dy, H + 4 + dy):
            for x in range(dx, W + dx):
                if (x >= W or y >= H + 4) and S.get(x, y)[3] < a:
                    S.set(x, y, rgba(SH, a))
    # the boards' turned-in leather, only where it shows
    lx0, lx1 = LEAF_L[0] - 6, LEAF_R[1] + 9

    def shows(x, y):
        return not (lx0 + 2 <= x < lx1 - 2 and LEAF_Y0 + 2 <= y < LEAF_Y1 + 6)
    mask = {(x, y) for y in range(H) for x in range(W) if shows(x, y)}

    def lightfn(x, y):
        d = math.hypot(x + 60, (y + 20) / 1.2)
        return 2 if d < 160 else (1 if d < 360 else 0)
    leather(S, mask, seed, base=3, light=lightfn)
    for x in range(1, W - 1):
        S.set(x, 0, LEA[8] if x < W * 0.4 else LEA[7]); S.set(x, H - 1, LEA[1])
    for y in range(1, H - 1):
        S.set(0, y, LEA[7]); S.set(W - 1, y, LEA[1])
    gilt(S, [p for p in rect_pts(4, 4, W - 5, H - 5) if p in mask], rnd, worn=0.12)
    # the board edges along the tail, seen from the front
    for x in range(W):
        S.set(x, H, LEA[3]); S.set(x, H + 1, LEA[2]); S.set(x, H + 2, LEA[1]); S.set(x, H + 3, LEA[1])
    # the spine between the boards at head and tail: the hollow back, the head and tail bands
    g0, g1 = LEAF_L[1], LEAF_R[0]
    for y in list(range(0, LEAF_Y0 + 1)) + list(range(LEAF_Y1 - 1, H + 4)):
        for x in range(g0 - 3, g1 + 3):
            S.set(x, y, LEA[1] if x in (g0 - 3, g1 + 2) else LEA[0])
    for x in range(g0 - 2, g1 + 2):
        for y in (LEAF_Y0 - 4, LEAF_Y0 - 3, LEAF_Y0 - 2):
            S.set(x, y, WAX[3] if (x + y) % 3 else VEL[5])
        for y in (LEAF_Y1 + 1, LEAF_Y1 + 2, LEAF_Y1 + 3):
            S.set(x, y, WAX[2] if (x + y) % 3 else VEL[4])
    # the blocks of leaves on their edges: thin on the left (the pages read), thick on the right
    page_band(S, list(range(LEAF_L[0] - 4, LEAF_L[0])), list(range(LEAF_Y0 + 2, LEAF_Y1 + 4)), 4, False, seed + 4)
    page_band(S, list(range(LEAF_L[0] - 4, LEAF_L[1])), list(range(LEAF_Y1, LEAF_Y1 + 4)), 4, True, seed + 5)
    page_band(S, list(range(LEAF_R[1], LEAF_R[1] + 8)), list(range(LEAF_Y0 + 2, LEAF_Y1 + 8)), 8, False, seed + 6)
    page_band(S, list(range(LEAF_R[0], LEAF_R[1] + 8)), list(range(LEAF_Y1, LEAF_Y1 + 8)), 8, True, seed + 7)
    # the leaves
    open_leaf(S, LEAF_L[0], LEAF_L[1], LEAF_Y0, LEAF_Y1, 'L', seed + 10)
    open_leaf(S, LEAF_R[0], LEAF_R[1], LEAF_Y0, LEAF_Y1, 'R', seed + 20)
    # the fold itself, dark between the leaves
    for y in range(LEAF_Y0, LEAF_Y1):
        S.set(LEAF_L[1], y, VT['deep']); S.set(LEAF_L[1] + 1, y, LEA[1]); S.set(LEAF_L[1] + 2, y, LEA[1])
        S.set(LEAF_R[0] - 1, y, VT['deep'])
    # the sealed back of the book: a cord round the deepest leaves of the right block, across their edges and
    # off the board, where a small seal holds its ends
    cy = 150
    for x in range(LEAF_R[1] + 2, W + 2):
        S.set(x, cy, WAX[4] if x % 3 else WAX[3]); S.set(x, cy + 1, WAX[2]); S.set(x, cy + 2, rgba(SH, 90))
    for (x, y) in ((LEAF_R[1] + 2, cy - 1), (LEAF_R[1] + 2, cy + 2)):
        S.set(x, y, WAX[2])
    tie = wax_seal(5, ['.x.', 'xxx', '.x.'], seed=21, drips=((1.6, 2),))
    tp = tie.im.load()
    for y in range(tie.h):
        for x in range(tie.w):
            if tp[x, y][3]:
                S.over(W - 7 + x, cy - tie.h // 2 + 1 + y, tp[x, y][:3], tp[x, y][3])
    # the curl: the lower outer corner of the left leaf lifted to be turned back. The right leaf's corner lies
    # flat: the pages past this one are sealed. Under the lifted corner the leaf beneath shows in the flap's
    # shadow; the flap shows its underside, lit along the curl, foreshortened as it turns toward us
    n = 19
    cx, by = LEAF_L[0], LEAF_Y1 - 1
    smax = 0.72 * (n - 1)
    flap = {}
    for j in range(n):
        for i in range(n):
            sd = i + j - (n - 1)
            if sd > 0 and sd <= smax and abs(i - j) <= (n - 1) * (1 - sd / smax):
                flap[(i, j)] = sd
    for j in range(n):
        for i in range(n - j):
            x, y = cx + i, by - j
            if i == 0 or j == 0:
                c = VT['rim2']
            elif i + j >= n - 3:
                c = VT['rim1']
            else:
                c = VT['shade'] if (x + y) % 2 else VT['rim1']
            S.set(x, y, c)
    for (i, j), sd in flap.items():
        S.over(cx + i + 1, by - j + 1, SH, 70)
    for (i, j), sd in flap.items():
        edge = abs(i - j) >= (n - 1) * (1 - sd / smax) - 1.0 or sd > smax - 1.0
        if sd <= 1.5:
            c = VT['hi']
        elif edge:
            c = VT['rim2']
        elif sd / smax > 0.62:
            c = '#ddcfab'
        else:
            c = VT['back']
        S.set(cx + i, by - j, c)
    # brass caps on the board's four outer corners
    small_corner(S, 0, 0, False, False, rnd)
    small_corner(S, W - 16, 0, True, False, rnd)
    small_corner(S, 0, H - 16, False, True, rnd)
    small_corner(S, W - 16, H - 16, True, True, rnd)
    for x in list(range(0, 4)) + list(range(W - 4, W)):
        S.set(x, H, BRASS[3]); S.set(x, H + 1, BRASS[2]); S.set(x, H + 2, BRASS[1])
    return S


ASSETS['book-open'] = book_open


if __name__ == '__main__':
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        save(ASSETS[n](), n)
