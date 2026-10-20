"""The War Table under the storybook, and the table things drawn large.

Kort, 2026-10-18 late (DESIGN-RULES.md): table props read far too small on every table screen; draw them at the
kit's pixel scale about 2.5 times the home screen's, the candle lighting the oak and the book and throwing
shadows; and give every page its own stretch of the table. Here:
- three stretches of one table (the closed book, the open book and the march card each lie on their own):
  different planks, knots, joints and nails from the same kit (v4art.oak_table), and no brass set into the
  planks (no butterfly keys, plates, straps or tacks: Kort, 2026-10-18);
- the back rail cut from another board (same mouldings, its own grain and wear);
- a chamberstick candle and an inkwell with its quill, painted at the kit's pixel scale (one pixel per GUI
  pixel) at about 2.5 times the home screen's (44x54 and 50x58 there); the flame bent by the wind that opens
  the book;
- the candle's light on the oak, on the book and on whatever stands near it, and the shadows they throw away
  from the flame, in stepped alpha with dithered edges.

python sb_table.py [name ...]   writes art/<name>.png"""
import math, random, sys, os
import numpy as np
from PIL import Image
from sb_common import *
from v4art import oak_table
from v4a_furn import rail, engrave, ell_in

ASSETS = {}
FLAME = ['#4a6fb5', '#e0802c', '#f2a93f', '#ffd27a', '#fff4d6']
GLASS = ['#0a0f0d', '#111915', '#18241f', '#21322b', '#2d443a', '#41604f', '#6f9381', '#b5d1c2']
WARM = '#ffb35c'


def hexrgb(c):
    return rgba(c)[:3]


# ================================================================ stretches of the table
# Nothing brass is set into the planks (Kort, 2026-10-18): no butterfly keys, mending plates, straps or tacks.
# Plank layout, knots and the kit's iron nail heads carry the variety, and each stretch is its own run of them.
def stretch(seed, avoid=()):
    return oak_table(seed=seed, avoid=avoid)


# seeds: each stretch is its own run of planks; avoid: text on bare oak and the props' feet
STRETCHES = {
    'table-sb-closed': (61, [(390, 498, 580, 534), (40, 20, 220, 64)]),
    'table-sb-open': (74, [(832, 186, 930, 232), (820, 462, 900, 504)]),
    'table-sb-march': (88, [(260, 60, 700, 520)]),
}
for _n, (_seed, _av) in STRETCHES.items():
    ASSETS[_n] = (lambda sd, av: (lambda: stretch(sd, av)))(_seed, _av)
ASSETS['rail-sb'] = lambda: rail(seed=29)


# ================================================================ the chamberstick
def candle_big(lean=1.0, tongue=True, rise=False):
    """a beeswax candle in a brass chamberstick, about 2.5 times the home screen's, seen from above and in
    front like everything on the table. The dish: a rim lit at the back left, a front wall falling into shade,
    an engraved ring in the dished floor, the flame's reflection on its front inner wall, a finger ring. The
    socket: a turned brass cup with a flared lip. The candle: wax lit on the left and glowing near the flame,
    runs of wax down its side and over the lip of the socket, a melted pool round the wick. The flame laid over
    to the left by the wind (lean 1) with a blue root, its core white, a tongue torn off the tip and a thread of
    smoke carried left"""
    W, H = 132, 176
    s = Spr(W, H)
    cx, cy = 64, 154
    RX, RY = 42, 15
    IRX, IRY = 36, 11.4
    # occlusion where the dish meets the oak
    for y in range(cy - RY, H):
        for x in range(W):
            if ell_in(x, y, cx + 2, cy + 8, RX + 3, RY + 3) and not ell_in(x, y, cx, cy + 6, RX, RY):
                d = ((x - cx - 2) / (RX + 3.0)) ** 2 + ((y - cy - 8) / (RY + 3.0)) ** 2
                s.set(x, y, rgba(SH, 150 if d < 0.93 else 80))
    # the dish's front wall
    for y in range(cy, cy + RY + 7):
        for x in range(cx - RX, cx + RX + 1):
            if ell_in(x, y, cx, cy + 6, RX, RY) and not ell_in(x, y, cx, cy, RX, RY):
                t = (x - (cx - RX)) / (2.0 * RX)
                c = BRASS[3] if t < 0.06 else (BRASS[5] if t < 0.30 else (BRASS[4] if t < 0.58 else
                                                                          (BRASS[3] if t < 0.86 else BRASS[2])))
                s.set(x, y, c)
    for x in range(cx - RX, cx + RX + 1):
        for y in range(cy + RY + 7, cy, -1):
            if ell_in(x, y, cx, cy + 6, RX, RY):
                s.set(x, y, BRASS[1])
                break
    # the rim and the floor
    for y in range(cy - RY, cy + RY + 1):
        for x in range(cx - RX, cx + RX + 1):
            if not ell_in(x, y, cx, cy, RX, RY):
                continue
            if not ell_in(x, y, cx, cy, IRX, IRY):
                ang = math.atan2((y - cy) / RY, (x - cx) / RX)
                lit = -math.cos(ang + 2.3)
                c = SPEC if lit < -0.93 else (BRASS[7] if lit < -0.6 else (BRASS[6] if lit < 0.05 else
                                                                         (BRASS[5] if lit < 0.6 else BRASS[4])))
                s.set(x, y, c)
            else:
                v = (y - cy) / IRY
                u = (x - cx) / IRX
                c = BRASS[2] if v < -0.45 else (BRASS[3] if v < 0.15 else (BRASS[4] if v < 0.55 else
                                                                         (BRASS[5] if v < 0.8 else BRASS[6])))
                if v > 0.55 and u > 0.45:
                    c = BRASS[4]
                s.set(x, y, c)
    # the rim's outer edge and the inner lip
    for y in range(cy - RY - 1, cy + RY + 2):
        for x in range(cx - RX - 1, cx + RX + 2):
            if ell_in(x, y, cx, cy, RX, RY) and not ell_in(x, y, cx, cy, RX - 1.2, RY - 0.9) and y < cy - 2:
                s.set(x, y, BRASS[6] if x < cx + 10 else BRASS[5])
            if ell_in(x, y, cx, cy, IRX + 0.9, IRY + 0.7) and not ell_in(x, y, cx, cy, IRX, IRY):
                s.set(x, y, BRASS[3] if y < cy else BRASS[7])
    # an engraved ring in the floor
    pts = []
    for k in range(220):
        a = 2 * math.pi * k / 220
        p = (round(cx + 27 * math.cos(a)), round(cy + 8.4 * math.sin(a)))
        if p not in pts and not (abs(p[0] - cx) < 15 and p[1] < cy + 2):
            pts.append(p)
    engrave(s, pts)
    # the flame's reflection on the front inner wall, and a cold window glint on the back of the rim
    for (x, y, c) in ((cx - 12, cy + 8, SPEC), (cx - 11, cy + 8, '#ffe2a6'), (cx - 13, cy + 8, '#ffe2a6'),
                      (cx - 12, cy + 9, BRASS[7]), (cx + 5, cy + 9, BRASS[7]), (cx + 6, cy + 9, '#ffe2a6'),
                      (cx - 26, cy - 11, SPEC), (cx - 25, cy - 11, SPEC), (cx - 24, cy - 12, BRASS[7])):
        s.set(x, y, c)
    # the finger ring on the right, its neck joining the rim
    fx, fy = cx + RX + 6, cy - 3
    for y in range(fy - 9, fy + 10):
        for x in range(fx - 12, fx + 12):
            if ell_in(x, y, fx, fy, 10.5, 7.8) and not ell_in(x, y, fx, fy, 6.0, 4.0):
                ang = math.atan2((y - fy) / 7.8, (x - fx) / 10.5)
                lit = -math.cos(ang + 2.3)
                inner = ell_in(x, y, fx, fy, 7.6, 5.4)
                c = (BRASS[3] if y < fy else BRASS[6]) if inner else (BRASS[7] if lit < -0.5 else
                                                                      (BRASS[5] if lit < 0.3 else BRASS[3]))
                s.set(x, y, c)
            elif ell_in(x, y, fx + 2, fy + 3, 10.5, 7.8) and not ell_in(x, y, fx, fy, 10.5, 7.8) \
                    and not ell_in(x, y, fx + 2, fy + 3, 6.0, 4.0) and y > fy:
                s.set(x, y, BRASS[2])
    for y in range(fy - 3, fy + 4):
        for x in range(cx + RX - 3, fx - 8):
            if s.get(x, y)[3] == 0 or True:
                s.set(x, y, BRASS[6] if y < fy - 1 else (BRASS[4] if y < fy + 2 else BRASS[2]))
    # the socket: a turned cup with a flared base and lip
    s0, s1, SR = cy - 26, cy - 1, 12
    for y in range(cy - 6, cy + 3):
        for x in range(cx - 18, cx + 19):
            if ell_in(x, y, cx, cy - 1, 17, 5.2):
                t = (x - (cx - 17)) / 34.0
                s.set(x, y, BRASS[6] if t < 0.3 else (BRASS[5] if t < 0.55 else (BRASS[4] if t < 0.8 else BRASS[2])))
    for y in range(s0, s1):
        for x in range(cx - SR, cx + SR + 1):
            t = (x - (cx - SR)) / (2.0 * SR)
            c = BRASS[4] if t < 0.1 else (BRASS[6] if t < 0.24 else (SPEC if t < 0.29 else (BRASS[6] if t < 0.4 else (
                BRASS[5] if t < 0.62 else (BRASS[4] if t < 0.82 else (BRASS[3] if t < 0.93 else BRASS[2]))))))
            s.set(x, y, c)
    for x in range(cx - SR, cx + SR + 1):                      # turned grooves
        for y in (s0 + 9, s0 + 17):
            s.set(x, y, BRASS[2] if x > cx else BRASS[3])
            s.set(x, y + 1, BRASS[6] if x < cx + 6 else BRASS[4])
    for y in range(s0 - 5, s0 + 4):                            # the lip
        for x in range(cx - 16, cx + 17):
            if ell_in(x, y, cx, s0, 15.5, 4.8):
                ins = ell_in(x, y, cx, s0, 11.2, 3.3)
                if ins:
                    s.set(x, y, BRASS[2])
                else:
                    ang = math.atan2((y - s0) / 4.8, (x - cx) / 15.5)
                    lit = -math.cos(ang + 2.3)
                    s.set(x, y, SPEC if lit < -0.85 else (BRASS[7] if lit < -0.3 else (BRASS[5] if lit < 0.5 else
                                                                                       BRASS[4])))
    for x in range(cx - 15, cx + 16):
        for y in range(s0 + 4, s0 - 1, -1):
            if ell_in(x, y, cx, s0 + 0.6, 15.5, 4.8):
                s.set(x, y + 1, BRASS[2])
                break
    # the candle
    c0, CR = 62, 10
    for y in range(c0, s0 + 1):
        for x in range(cx - CR, cx + CR + 1):
            t = (x - (cx - CR)) / (2.0 * CR)
            k = 6 if t < 0.1 else (8 if t < 0.32 else (7 if t < 0.56 else (6 if t < 0.8 else (5 if t < 0.93 else 4))))
            c = VEL[k]
            g = max(0.0, 1.0 - (y - c0) / 16.0)
            if g > 0:
                c = mix(c, '#ffe7b4', min(0.6, g * 0.75))
            s.set(x, y, c)
    for x in range(cx - CR, cx + CR + 1):
        for y in range(c0 - 4, c0 + 5):
            if ell_in(x, y, cx, c0, CR + 0.4, 3.6):
                s.set(x, y, VEL[8] if x < cx + 4 else VEL[7])
            if ell_in(x, y, cx, c0 + 0.3, CR - 2.5, 2.4):
                s.set(x, y, '#f6d69a' if x < cx + 2 else '#e9bf7d')
            if ell_in(x, y, cx, c0 + 0.6, CR - 5, 1.4):
                s.set(x, y, '#fbe3ad')
    # runs of wax down the side, each ending in a bead with its lit side and its shadow
    for (dx, ln, w2) in ((-8, 18, 2), (-4, 36, 2), (2, 11, 2), (6, 28, 3)):
        x0 = cx + dx
        for y in range(c0 + 1, c0 + ln):
            for i in range(w2):
                base = s.get(x0 + i, y)
                s.set(x0 + i, y, mix('#%02x%02x%02x' % base[:3], '#fff6df', 0.35) if i == 0 else
                      '#%02x%02x%02x' % base[:3])
            s.set(x0 + w2, y, VEL[4] if dx > -6 else VEL[5])
        for (bx, by, c) in ((x0 - 1, c0 + ln, VEL[8]), (x0, c0 + ln, VEL[7]), (x0 + 1, c0 + ln, VEL[7]),
                            (x0 + 2, c0 + ln, VEL[5]), (x0, c0 + ln + 1, VEL[5]), (x0 + 1, c0 + ln + 1, VEL[4])):
            s.set(bx, by, c)
    # wax over the lip of the socket
    for (dx, ln) in ((-13, 6), (-9, 9), (9, 5), (13, 3)):
        for y in range(s0 - 1, s0 + ln):
            s.set(cx + dx, y, VEL[7] if dx < 0 else VEL[6])
            s.set(cx + dx + 1, y, VEL[6] if dx < 0 else VEL[5])
        s.set(cx + dx, s0 + ln, VEL[5]); s.set(cx + dx + 1, s0 + ln, VEL[4])
    for x in range(cx - 14, cx + 15):
        if abs(x - cx) > CR - 1:
            s.set(x, s0 - 1, VEL[7] if x < cx else VEL[6])
            s.set(x, s0, VEL[6] if x < cx else VEL[5])
    # the wick, bent with the flame
    wick = [(cx, c0 - 1), (cx, c0 - 2), (cx - 1, c0 - 3), (cx - 1, c0 - 4)]
    for (x, y) in wick:
        s.set(x, y, INK[0])
    s.set(cx - 2, c0 - 5, '#ff8a3a')
    # the flame
    bx, by, FH = cx - 1.5, c0 - 4, 36
    fl = {}
    for y in range(by - FH - 2, by + 3):
        sv = (by - y) / float(FH)
        if sv < -0.06 or sv > 1.0:
            continue
        sv2 = max(0.0, sv)
        xc = bx - lean * 17.0 * sv2 ** 1.55
        hw = 7.4 * (sv2 / 0.24) ** 0.55 if sv2 < 0.24 else 7.4 * ((1.0 - sv2) / 0.76) ** 0.9
        hw = max(hw, 1.2 if sv < 0 else 0.6)
        for x in range(int(xc - hw - 2), int(xc + hw + 3)):
            d = abs(x + 0.5 - xc) / hw
            if d > 1.0:
                continue
            if sv < 0.15 and d > 0.42:
                c = FLAME[0]
            elif sv < 0.62 and d < 0.34:
                c = FLAME[4]
            elif d < 0.6:
                c = FLAME[3]
            elif d < 0.84:
                c = FLAME[2]
            else:
                c = FLAME[1]
            fl[(x, y)] = c
    # a tongue torn off the tip, and its glow
    tx, ty = int(bx - lean * 17.0) - 4, by - FH - 5
    if tongue:
        for (x, y, c) in ((tx, ty, FLAME[1]), (tx + 1, ty, FLAME[2]), (tx, ty + 1, FLAME[2]),
                          (tx + 1, ty + 1, FLAME[1]), (tx - 1, ty - 1, FLAME[1])):
            fl[(x, y)] = c
    halo = {}
    for (x, y) in fl:
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                q = (x + dx, y + dy)
                if q in fl:
                    continue
                r = max(abs(dx), abs(dy))
                a = 70 if r <= 2 else 34
                halo[q] = max(halo.get(q, 0), a)
    for (x, y), a in halo.items():
        s.over(x, y, '#ffcf7a', a)
    for (x, y), c in fl.items():
        s.set(x, y, c)
    for (x, y) in wick[2:]:
        s.set(x, y, INK[1])
    # a thread of smoke carried left and up from the tip, thinning out in steps (or, in still air, rising
    # straight from the tip and wavering as it thins)
    if rise:
        x0, y0 = int(round(bx - lean * 17.0)), by - FH - 3
        for k in range(40):
            yy = y0 - k
            xx = x0 + round(math.sin(k * 0.33) * (0.4 + k * 0.05))
            a = 120 if k < 10 else (85 if k < 20 else (55 if k < 30 else 28))
            if yy >= 0:
                s.over(xx, yy, '#cfc6b6', a)
        return s
    x, y = tx - 3, ty - 2
    for k in range(44):
        x -= 1
        if k % 3 == 0:
            y -= 1
        yy = y + round(math.sin(k * 0.42) * 1.6)
        a = 130 if k < 10 else (95 if k < 22 else (60 if k < 34 else 30))
        if x >= 0:
            s.over(x, yy, '#cfc6b6', a)
    return s


ASSETS['candle-big'] = candle_big
ASSETS['candle-calm'] = lambda: candle_big(0.12, tongue=False, rise=True)


def glow(rx, ry, peak=0.30, col=WARM, step=0.03):
    """candle light on the oak: a warm translucent pool in stepped rings, dithered where one ring gives way
    to the next"""
    s = Spr(2 * rx + 1, 2 * ry + 1)
    for y in range(2 * ry + 1):
        for x in range(2 * rx + 1):
            d = math.sqrt(((x - rx) / float(rx)) ** 2 + ((y - ry) / float(ry)) ** 2)
            if d > 1.0:
                continue
            a = (1.0 - d) ** 1.7 * peak
            lo = math.floor(a / step)
            f = a / step - lo
            q = lo + (1 if dith(x, y, f) else 0)
            if q > 0:
                s.set(x, y, rgba(col, round(q * step * 255)))
    return s


ASSETS['glow-candle'] = lambda: glow(300, 230, 0.32)


# ================================================================ the inkwell and its quill
def inkwell_big(light='left'):
    """a squat octagonal inkwell of dark green glass, about 2.5 times the home screen's, its quill standing in
    it and leaning out to the right. The glass: flat facets, lighter edges where facets meet, a cold window
    streak on the upper left, the candle's warm glints on the facets that face it, a thick base that catches
    a line of light; the ink black in the mouth with a wet highlight. The quill: a goose feather, the shaft
    bowed, the vane split in places, its barbs ticked; the bare quill amber where it goes into the ink. A few
    old drops of ink on the oak"""
    W, H = 150, 176
    s = Spr(W, H)
    cx, cy = 46, 150                 # the base's centre
    BW, BH, RY = 30, 30, 8.5         # half width, wall height, ellipse depth
    top = cy - BH
    # occlusion under the base
    for y in range(cy - 4, cy + 14):
        for x in range(cx - BW - 4, cx + BW + 6):
            if ell_in(x, y, cx + 2, cy + 3, BW + 3, RY + 3) and not ell_in(x, y, cx, cy, BW, RY):
                s.set(x, y, rgba(SH, 150))
    # facets: x edges of the octagon's front half (narrow at the sides)
    edges = [cx - BW, cx - 24, cx - 10, cx + 10, cx + 24, cx + BW]
    tones = [GLASS[4], GLASS[3], GLASS[2], GLASS[1], GLASS[0]] if light == 'left' else \
        [GLASS[2], GLASS[1], GLASS[2], GLASS[3], GLASS[2]]
    for y in range(top, cy + int(RY) + 1):
        for x in range(cx - BW, cx + BW + 1):
            inside = (top <= y <= cy) or ell_in(x, y, cx, cy, BW, RY)
            if not inside:
                continue
            f = max(i for i in range(5) if x >= edges[i])
            s.set(x, y, tones[f])
    for e in edges[1:-1]:                                     # facet edges catch light
        for y in range(top + 2, cy + 6):
            if s.get(e, y)[3]:
                s.set(e, y, GLASS[5] if e < cx else GLASS[3])
    for x in range(cx - BW, cx + BW + 1):                    # the thick base and its line of light
        for y in range(cy + int(RY) + 1, cy - 2, -1):
            if ell_in(x, y, cx, cy, BW, RY):
                s.set(x, y, GLASS[0])
                s.set(x, y - 1, GLASS[1])
                s.set(x, y - 3, GLASS[5] if x < cx + 6 else GLASS[3])
                break
    # the shoulder: a broad sloped top, lit at the back left
    for y in range(top - int(RY) - 2, top + int(RY) + 1):
        for x in range(cx - BW, cx + BW + 1):
            if ell_in(x, y, cx, top, BW, RY):
                u = (x - cx) / float(BW); v = (y - top) / RY
                k = u * 0.8 + v * 0.6
                s.set(x, y, GLASS[6] if k < -0.85 else (GLASS[5] if k < -0.35 else (GLASS[4] if k < 0.2 else
                                                                                     (GLASS[3] if k < 0.6 else GLASS[2]))))
    # the neck and the mouth with the ink in it
    ny = top - 10
    for y in range(ny, top - 1):
        for x in range(cx - 12, cx + 13):
            t = (x - (cx - 12)) / 24.0
            s.set(x, y, GLASS[5] if t < 0.2 else (GLASS[4] if t < 0.5 else (GLASS[3] if t < 0.8 else GLASS[1])))
    for y in range(ny - 5, ny + 5):
        for x in range(cx - 14, cx + 15):
            if ell_in(x, y, cx, ny, 13, 4.4):
                ins = ell_in(x, y, cx, ny + 0.4, 9.4, 3.0)
                if ins:
                    s.set(x, y, '#07060a')
                else:
                    u = (x - cx) / 13.0
                    s.set(x, y, GLASS[6] if (u < -0.4 and y <= ny) else (GLASS[5] if y <= ny else GLASS[3]))
    for (x, y, c) in ((cx - 4, ny - 1, '#3c4760'), (cx - 3, ny - 1, '#2a3244'), (cx + 5, ny + 1, '#1b2030')):
        s.set(x, y, c)
    # the window streak and the candle's glints
    for y in range(top + 6, cy - 6):
        s.set(cx - 20, y, GLASS[7] if y < top + 14 else GLASS[6])
        if y < top + 11:
            s.set(cx - 19, y, GLASS[6])
    if light == 'left':
        for (x, y, c) in ((cx - 27, top + 9, '#f7c77a'), (cx - 27, top + 10, '#f7c77a'), (cx - 27, top + 11, '#d99a4a'),
                          (cx - 26, top + 12, '#b97e3c'), (cx - 22, top - 4, '#f2c27a'), (cx - 23, top - 4, '#d99a4a')):
            s.set(x, y, c)
    # the quill: a bowed shaft from the mouth up to the right
    p0, p1, ctrl = (cx + 2, ny + 1), (cx + 100, 6), (cx + 34, ny - 70)

    def bez(t):
        return ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * ctrl[0] + t * t * p1[0],
                (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * ctrl[1] + t * t * p1[1])
    N = 300
    pts = [bez(k / float(N)) for k in range(N + 1)]
    rnd = random.Random(17)
    # the vane: barbs drawn one by one from the shaft, swept toward the tip; the upper-left vane broad and lit,
    # the lower-right narrow and in shade; every few barbs a shade darker (the vane's grain); the outer edge
    # ragged; two splits where barbs have parted
    barbs = []
    for k in range(int(N * 0.24), N + 1, 2):
        t = k / float(N)
        x, y = pts[k]
        xa, ya = pts[max(0, k - 3)]
        xb, yb = pts[min(N, k + 3)]
        dx, dy = xb - xa, yb - ya
        L = math.hypot(dx, dy) or 1
        dx, dy = dx / L, dy / L
        nx, ny_ = dy, -dx                     # the normal toward the upper left of the shaft
        u = min(1.0, (t - 0.24) / 0.76)
        prof_l = 13.5 * math.sin(math.pi * min(1.0, u * 1.04) ** 0.72) ** 0.75
        prof_r = 6.5 * math.sin(math.pi * min(1.0, u * 1.02) ** 0.85) ** 0.9
        for side, prof in ((-1, prof_r), (1, prof_l)):
            ln = prof * (1.0 + rnd.uniform(-0.12, 0.06))
            ang = math.radians(38 if side == 1 else 44)
            bx_ = side * nx * math.cos(ang) + dx * math.sin(ang)
            by_ = side * ny_ * math.cos(ang) + dy * math.sin(ang)
            barbs.append((k, side, x, y, bx_, by_, ln, u))
    gap = lambda u, side: side == 1 and ((0.40 < u < 0.43) or (0.66 < u < 0.685))
    for (k, side, x, y, bx_, by_, ln, u) in barbs:
        if gap(u, side):
            ln = min(ln, 2.5)
        grp = (k // 2) % 4 == 0
        n = int(round(ln))
        for j in range(1, n + 1):
            X, Y = round(x + bx_ * j), round(y + by_ * j)
            if side == 1:
                c = VEL[8] if j == n else (VEL[6] if grp else (VEL[7] if j < n - 2 else VEL[8]))
                if j <= 1:
                    c = VEL[6]
            else:
                c = VEL[5] if j == n else (VEL[4] if grp else VEL[5])
                if j <= 1:
                    c = VEL[4]
            s.set(X, Y, c)
    for k in range(N + 1):
        t = k / float(N)
        x, y = pts[k]
        X, Y = round(x), round(y)
        if t < 0.24:
            s.set(X, Y, '#d8bf86' if t > 0.05 else '#9c8352')
            s.set(X + 1, Y, '#a8915e' if t > 0.05 else '#6d5b3a')
        else:
            s.set(X, Y, VEL[8] if t < 0.9 else VEL[7])
            s.set(X + 1, Y + 1, VEL[3])
    # the curled tip
    for (x, y, c) in ((p1[0] + 1, p1[1] - 1, VEL[7]), (p1[0] + 2, p1[1] - 1, VEL[6]), (p1[0] + 3, p1[1], VEL[5])):
        s.set(x, y, c)
    # old drops of ink on the oak, each with a wet glint
    DROP = {3: ['.aaa.', 'aagaa', 'aaaaa', '.aaa.'], 2: ['.aa.', 'agaa', '.aa.'], 1: ['aa', 'ga']}
    for (x, y, r) in ((cx + 40, cy + 6, 3), (cx + 47, cy + 11, 1), (cx - 38, cy + 10, 2), (cx + 56, cy + 2, 1),
                      (cx + 44, cy + 9, 1)):
        s.grid(x, y, DROP[r], {'a': '#120d0c', 'g': '#4a4a5a'})
    return s


ASSETS['inkwell-big'] = inkwell_big


# ================================================================ the candle's light and the shadows it throws
CLIP_ROWS = {'inkwell-big.png': 110}          # shadows from the bottle only (the quill's would float)


SHADOW_OPEN = {'inkwell-big.png': 2}         # thin things (the quill, old ink drops) throw no shadow worth drawing


def _opened(m, r):
    """binary opening with a (2r+1) square: drops anything thinner than the square, keeps the rest's outline"""
    H, W = m.shape
    pad = np.pad(m, r, constant_values=False)
    er = np.ones_like(m)
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            er &= pad[r + dy:r + dy + H, r + dx:r + dx + W]
    pad = np.pad(er, r, constant_values=False)
    out = np.zeros_like(m)
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            out |= pad[r + dy:r + dy + H, r + dx:r + dx + W]
    return out


def alpha_mask(path, thresh=200, for_shadow=False):
    im = np.array(Image.open(path).convert('RGBA'))
    m = im[..., 3] >= thresh
    r = CLIP_ROWS.get(os.path.basename(path)) if for_shadow else None
    if r:
        m[:r] = False
    k = SHADOW_OPEN.get(os.path.basename(path)) if for_shadow else None
    if k:
        m = _opened(m, k)
    return m


def lightplay(W, H, flame, flame_h, objects, lit=None, max_len=46, seed=3):
    """two full-page overlays from one candle. shade: each object's shadow on the oak, the object's footprint
    swept away from the flame by its height over the flame's (capped), darkest at the object and lighter in
    two steps toward the far end, the last step dithered; it goes above the candle's glow and under the
    objects. lit: the candle's light on the objects named in lit, a warm wash in stepped alpha falling off
    with distance from the flame, brighter on the edges that face it; it goes above them. objects: [(path,
    x, y, height)]. Returns (shade Spr, lit Spr)"""
    fx, fy = flame
    occ = np.zeros((H, W), bool)
    masks = []
    for (path, ox, oy, hgt) in objects:
        m = alpha_mask(path, for_shadow=True)
        full = np.zeros((H, W), bool)
        h, w = m.shape
        y0, y1 = max(0, oy), min(H, oy + h)
        x0, x1 = max(0, ox), min(W, ox + w)
        full[y0:y1, x0:x1] = m[y0 - oy:y1 - oy, x0 - ox:x1 - ox]
        masks.append((full, hgt, path))
        occ |= full
    shade = np.zeros((H, W))
    steps = (0.56, 0.44, 0.32)
    for (full, hgt, path) in masks:
        ys, xs = np.nonzero(full)
        if not len(xs):
            continue
        c = np.array([xs.mean(), ys.mean()])
        v = c - np.array([fx, fy])
        dist = np.linalg.norm(v)
        L = min(max_len, dist * hgt / max(1.0, flame_h - hgt))
        u = v / max(dist, 1e-6)
        for t in range(int(L), -1, -1):
            dx, dy = int(round(u[0] * t)), int(round(u[1] * t))
            sh = np.zeros((H, W), bool)
            ya, yb = max(0, dy), min(H, H + dy)
            xa, xb = max(0, dx), min(W, W + dx)
            sh[ya:yb, xa:xb] = full[ya - dy:yb - dy, xa - dx:xb - dx]
            k = min(2, int(3 * t / max(1.0, L + 1)))
            shade = np.where(sh, np.maximum(shade, steps[k]), shade)
    shade[occ] = 0
    S = Spr(W, H)
    ys, xs = np.nonzero(shade)
    for y, x in zip(ys.tolist(), xs.tolist()):
        S.set(x, y, rgba(SH, int(round(shade[y, x] * 255))))
    Lt = Spr(W, H)
    if lit:
        rnd = random.Random(seed)
        for (full, hgt, path) in masks:
            if os.path.basename(path) not in lit:
                continue
            peak, R = lit[os.path.basename(path)]
            ys, xs = np.nonzero(full)
            d = np.hypot(xs - fx, ys - fy)
            a = peak * np.clip(1.0 - d / R, 0, 1) ** 1.4
            # edges facing the flame: an opaque pixel whose neighbour toward the flame is empty
            ux = np.sign(fx - xs).astype(int)
            uy = np.sign(fy - ys).astype(int)
            nx = np.clip(xs + ux, 0, W - 1); ny = np.clip(ys + uy, 0, H - 1)
            rim = ~occ[ys, nx] | ~occ[ny, xs]
            a = np.where(rim, a + 0.16, a)
            q = 0.03
            for y, x, av in zip(ys.tolist(), xs.tolist(), a.tolist()):
                lo = math.floor(av / q)
                f = av / q - lo
                n = lo + (1 if dith(x, y, f) else 0)
                if n > 0:
                    Lt.set(x, y, rgba(WARM, int(round(min(0.6, n * q) * 255))))
    return S, Lt




# ================================================================ page lights
def window_shadow(W, H, base, length=118, d=(0.78, 0.62), hw=(10.0, 6.0), dish=(42, 15), dish_off=(5, 5), a=0.34):
    """the candle's own shadow from the table's key light (top left): the stick laid down and to the right from
    its foot, tapering, and the dish's shadow just beside the dish. Returns an alpha array"""
    bx, by = base
    dx, dy = d
    n = math.hypot(dx, dy); dx, dy = dx / n, dy / n
    yy, xx = np.mgrid[0:H, 0:W]
    px, py = xx - bx, yy - by
    along = px * dx + py * dy
    across = -px * dy + py * dx
    t = along / float(length)
    hw_t = hw[0] + (hw[1] - hw[0]) * np.clip(t, 0, 1)
    stick = (along >= 0) & (along <= length) & (np.abs(across) <= hw_t)
    dsh = (((xx - bx - dish_off[0]) / float(dish[0])) ** 2 + ((yy - by - 4 - dish_off[1]) / float(dish[1])) ** 2) <= 1
    out = np.where(stick | dsh, a, 0.0)
    far = stick & (t > 0.8)
    th = (xx + yy) % 2
    out[far & (th == 1)] = 0
    return out


def page_light(name, flame, flame_h, objects, lit, candle=None, under=(), max_len=46):
    """write art/shade-<name>.png (shadows on the oak: under the objects) and art/over-<name>.png (the light and
    any shadow that falls on the objects named in lit: above them). candle: (sprite path, x, y, foot) puts the
    candle's own shadow from the key light on the oak and on whatever it reaches"""
    S, Lt = lightplay(960, 540, flame, flame_h, objects, lit, max_len=max_len)
    if candle:
        path, cx0, cy0, foot = candle
        ws = window_shadow(960, 540, foot)
        cm = np.zeros((540, 960), bool)
        m = alpha_mask(path, 1)
        h, w = m.shape
        cm[cy0:cy0 + h, cx0:cx0 + w] = m[:min(h, 540 - cy0), :min(w, 960 - cx0)]
        occ = np.zeros((540, 960), bool)
        for (p, ox, oy, hg) in objects:
            mm = alpha_mask(p)
            hh, ww = mm.shape
            occ[oy:oy + hh, ox:ox + ww] |= mm[:min(hh, 540 - oy), :min(ww, 960 - ox)]
        ys, xs = np.nonzero(ws > 0)
        for y, x in zip(ys.tolist(), xs.tolist()):
            if cm[y, x]:
                continue
            a = int(round(ws[y, x] * 255))
            if occ[y, x]:
                Lt.over(x, y, SH, a)
            else:
                S.over(x, y, SH, a)
    S.save(os.path.join(ART, 'shade-%s.png' % name))
    Lt.save(os.path.join(ART, 'over-%s.png' % name))
    print('  art/shade-%s.png, art/over-%s.png' % (name, name))
    return None


# 01: the candle by the book's upper left corner, the inkwell at its lower right
P01 = dict(candle=(198, 82), ink=(700, 330), book=(330, 80))
ASSETS['light-01'] = lambda: page_light(
    '01', flame=(P01['candle'][0] + 57, P01['candle'][1] + 44), flame_h=120,
    objects=[(os.path.join(ART, 'book-closed.png'), P01['book'][0], P01['book'][1], 14),
             (os.path.join(ART, 'inkwell-big.png'), P01['ink'][0], P01['ink'][1], 44)],
    lit={'book-closed.png': (0.42, 470)},
    candle=(os.path.join(ART, 'candle-big.png'), P01['candle'][0], P01['candle'][1],
            (P01['candle'][0] + 64, P01['candle'][1] + 150)))


# 02: the book open on its own stretch, the candle and the inkwell to its right; the light pools on the right leaf
P02 = dict(candle=(816, 56), ink=(798, 338), book=(36, 72))      # as placed in storybook-02-spread.html
ASSETS['light-02'] = lambda: page_light(
    '02', flame=(P02['candle'][0] + 62, P02['candle'][1] + 44), flame_h=120,
    objects=[(os.path.join(ART, 'book-open.png'), P02['book'][0], P02['book'][1], 10),
             (os.path.join(ART, 'inkwell-big.png'), P02['ink'][0], P02['ink'][1], 44)],
    lit={'book-open.png': (0.36, 560)},
    candle=(os.path.join(ART, 'candle-calm.png'), P02['candle'][0], P02['candle'][1],
            (P02['candle'][0] + 64, P02['candle'][1] + 150)))


if __name__ == '__main__':
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        out = ASSETS[n]()
        if out is not None:
            save(out, n)
