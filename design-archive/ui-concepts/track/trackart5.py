"""The Chapter plaque, round 2, part two: the table props at the kit's pixel scale (a brass chamberstick with
its candle lit, the light it throws on the oak, a glass ink pot with its quill), and the tier 21 hero: the
Gilded Order's cloth of gold on its pole, and the wax seal with the crowned sword."""
import math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
sys.path.insert(0, HERE)
from v4lib import Spr, Noise, dith, rgba, mix
from v4pal import *
from v4a_furn import FLAME, GLASS, ell_in, SPEC, SH
from v4a_paper import wax_seal
from trackart4 import GOLD, SWORD

ASSETS = {}
WARM = '#ffb35c'          # the candle's light on the oak, the kit's glow colour
CORE = '#fff4d6'
KD = '#140b06'


# ================================================================ the candle, 2.5 times the kit's
def candle_big():
    """a beeswax candle in a brass chamberstick at the kit's 2x pixel scale: the dish with its rolled rim lit
    on the back left and the flame caught on its front, the dished floor turned in rings, the stick's shadow
    across it, a finger ring on the left with a thumb rest, the socket with its lip, the candle with its
    melted crater, wax runs and a pool on the lip, a long run down the socket to a puddle on the dish, the
    wick bent in the flame, the flame (orange tip, yellow body, white core, blue at the wick) and its halo"""
    oy = 12                                             # headroom for the halo
    W, H = 112, 142 + oy
    S = Spr(W, H)
    cx, cy, RX, RY = 60, 118 + oy, 38, 14
    top = 36 + oy                                       # the candle's crater

    def halo(x, y):
        d = math.hypot(x + 0.5 - (cx + 1.0), (y - 21 - oy) / 1.3)
        a = 0.30 if d < 11 else 0.19 if d < 17 else 0.10 if d < 24 else (0.05 if d < 30 and dith(x, y, 0.5) else 0)
        return rgba(FLAME[3], round(a * 255)) if a else None

    # the halo round the flame (translucent; the candle and flame are painted over it)
    for y in range(0, 62 + oy):
        for x in range(W):
            h = halo(x, y)
            if h:
                S.set(x, y, h)
    # the shadow the chamberstick throws on the oak, away from the window
    for y in range(60 + oy, H):
        for x in range(W):
            if ell_in(x, y, cx + 7, cy + 7, RX + 3, RY + 3.5):
                d = ((x - cx - 7) / (RX + 3)) ** 2 + ((y - cy - 7) / (RY + 3.5)) ** 2
                S.set(x, y, rgba(SH, 165 if d < 0.7 else 110))
    # the finger ring on the left, and its thumb rest
    rx0, ry0 = cx - RX - 4, cy - 1
    for y in range(H):
        for x in range(W):
            if ell_in(x + 0.5, y + 0.5, rx0, ry0, 10, 7.2) and not ell_in(x + 0.5, y + 0.5, rx0, ry0, 5.8, 3.6):
                up, lf = y + 0.5 < ry0, x + 0.5 < rx0
                c = (BRASS[7] if lf else BRASS[5]) if up else (BRASS[4] if lf else BRASS[2])
                if up and abs(x + 0.5 - (rx0 - 4)) < 1 and y + 0.5 < ry0 - 4:
                    c = SPEC
                S.set(x, y, c)
    S.grid(round(rx0) - 4, ry0 - 10, ['.hhhhhh.', 'hmmmmmmd', '.dddddd.'], {'h': BRASS[7], 'm': BRASS[5], 'd': BRASS[2]})
    # the dish: the front wall under the rim, then the rim and the dished floor
    for y in range(cy - RY - 1, cy + RY + 7):
        for x in range(W):
            xx, yy = x + 0.5, y + 0.5
            inner = ell_in(xx, yy, cx + 0.5, cy, RX, RY)
            if not inner and ell_in(xx, yy, cx + 0.5, cy + 5, RX, RY) and yy > cy:
                t = (xx - (cx - RX)) / (2 * RX)
                c = BRASS[5] if t < 0.14 else BRASS[4] if t < 0.4 else BRASS[3] if t < 0.75 else BRASS[2]
                if not ell_in(xx, yy + 1, cx + 0.5, cy + 5, RX, RY):
                    c = BRASS[1]
                elif abs(xx - cx) < 9 and not ell_in(xx, yy - 1, cx + 0.5, cy, RX, RY) and yy < cy + RY + 2:
                    c = BRASS[6]                        # the flame caught on the front of the wall
                S.set(x, y, c)
            if inner:
                ang = math.atan2((yy - cy) / RY, (xx - cx - 0.5) / RX)
                d = math.hypot((xx - cx - 0.5) / RX, (yy - cy) / RY)
                L = math.cos(ang + 2.4)                 # 1 at the back left, where the window falls
                if d > 0.88:
                    c = SPEC if L > 0.95 else BRASS[7] if L > 0.62 else BRASS[6] if L > 0.05 else BRASS[5] if L > -0.55 else BRASS[4]
                    if yy > cy and abs(xx - cx - 0.5) < 7:
                        c = SPEC if abs(xx - cx - 0.5) < 3 else BRASS[7]     # the flame on the rim's front
                elif d > 0.82:
                    c = BRASS[3] if L > 0 else BRASS[2]                     # the rim's inner edge
                else:
                    v = (yy - cy) / (RY * 0.82)
                    k = 2 if v < -0.4 else 3 if v < 0.25 else 4 if v < 0.62 else 5
                    f = (v + 0.4) / 0.65 if v < 0.25 else (v - 0.25) / 0.37 if v < 0.62 else 0
                    if round(d * RX) % 6 == 0 and d > 0.4:
                        k = max(1, k - 1)               # turned rings
                    c = BRASS[k]
                S.set(x, y, c)
    # the stick's shadow on the floor, down and right from the window
    for y in range(cy - 2, cy + 12):
        for x in range(cx, cx + 36):
            t = (x - cx) / 36.0
            ycen = cy + 1 + t * 9
            if abs(y - ycen) < 3.2 - t * 1.5 and ell_in(x + 0.5, y + 0.5, cx + 0.5, cy, RX * 0.8, RY * 0.8):
                S.set(x, y, mix(S.get(x, y), BRASS[1], 0.45))
    # a few spots of tarnish on the floor
    rnd = random.Random(5)
    for _ in range(14):
        a, r = rnd.uniform(0, 6.28), rnd.uniform(0.35, 0.78)
        x, y = round(cx + RX * r * math.cos(a)), round(cy + RY * r * math.sin(a))
        if abs(x - cx) > 15:
            S.set(x, y, BRASS[2])
    # the socket: its fillet on the floor, the brass cylinder, the bead at its foot, the rolled lip
    for y in range(cy - 2, cy + 6):
        for x in range(W):
            if ell_in(x + 0.5, y + 0.5, cx + 0.5, cy + 1, 15, 4.6) and not ell_in(x + 0.5, y + 0.5, cx + 0.5, cy, 13, 3.6):
                S.set(x, y, BRASS[2] if x > cx - 6 else BRASS[3])
    for y in range(cy - 17, cy + 1):
        for x in range(cx - 12, cx + 13):
            t = (x - (cx - 12)) / 24.0
            c = BRASS[5] if t < 0.1 else BRASS[7] if t < 0.24 else BRASS[6] if t < 0.38 else BRASS[5] if t < 0.58 else BRASS[4] if t < 0.78 else BRASS[3] if t < 0.92 else BRASS[2]
            if y in (cy - 2, cy - 1):
                c = BRASS[6] if t < 0.3 else BRASS[4] if t < 0.7 else BRASS[2]
            if y == cy:
                c = BRASS[2]
            S.set(x, y, c)
    for y in range(cy - 14, cy - 4):
        S.set(cx - 8, y, SPEC if y < cy - 9 else BRASS[7])
    for y in range(cy - 23, cy - 12):
        for x in range(cx - 15, cx + 17):
            xx, yy = x + 0.5, y + 0.5
            if ell_in(xx, yy, cx + 0.5, cy - 17, 14.5, 4.3) and not ell_in(xx, yy, cx + 0.5, cy - 17.5, 11.5, 2.6):
                ang = math.atan2(yy - (cy - 17), xx - cx - 0.5)
                L = math.cos(ang + 2.4)
                c = BRASS[7] if L > 0.5 else BRASS[6] if L > -0.1 else BRASS[4]
                if yy > cy - 17 and not ell_in(xx, yy + 1, cx + 0.5, cy - 17, 14.5, 4.3):
                    c = BRASS[2]
                S.set(x, y, c)
    # the candle: beeswax, lit on the left, glowing through near the flame
    for y in range(top, cy - 17):
        for x in range(cx - 9, cx + 10):
            t = (x - (cx - 9)) / 18.0
            c = VEL[7] if t < 0.08 else VEL[8] if t < 0.3 else VEL[7] if t < 0.56 else VEL[6] if t < 0.78 else VEL[5] if t < 0.92 else VEL[4]
            g = max(0.0, 1.0 - (y - top) / 14.0)
            if g > 0:
                c = mix(c, CORE, g * 0.45)
            S.set(x, y, c)
    for x in range(cx - 9, cx + 10):                    # the crater: a raised rim round a melted pool
        for y in range(top - 3, top + 3):
            xx, yy = x + 0.5, y + 0.5
            if ell_in(xx, yy, cx + 0.5, top, 9.5, 3.0):
                inner = ell_in(xx, yy, cx + 0.5, top + 0.3, 7.5, 1.9)
                S.set(x, y, (VEL[6] if xx < cx + 3 else VEL[5]) if inner else (CORE if xx < cx + 2 else VEL[7]))
    S.set(cx - 3, top, CORE); S.set(cx - 2, top, CORE)   # the flame in the pool
    for x in range(cx + 6, cx + 10):                     # the rim melted low on the right
        for y in (top - 3, top - 2):
            S.set(x, y, halo(x, y))
    # the runs of wax down the side, each standing proud: lit edge, shadow on its right, a bulb at its foot
    for (dx, y0, ln, w) in ((-6, top + 2, 26, 2), (7, top - 1, 41, 3), (3, top + 2, 13, 2), (-1, top + 3, 8, 2)):
        for y in range(y0, y0 + ln):
            for i in range(w):
                S.set(cx + dx + i, y, VEL[8] if i == 0 else VEL[7])
            if cx + dx + w <= cx + 9:
                S.set(cx + dx + w, y, VEL[5])
        for i in range(-1, w + 1):
            S.set(cx + dx + i, y0 + ln, VEL[7] if i < w // 2 + 1 else VEL[6])
        S.set(cx + dx, y0 + ln + 1, VEL[6]); S.set(cx + dx + 1, y0 + ln + 1, VEL[5])
    # the wax pooled on the lip, and the long run down the socket to a puddle on the dish
    for x in range(cx - 13, cx + 15):
        k = abs(math.sin(x * 1.7)) + abs(math.sin(x * 0.6))
        if k > 0.6:
            S.set(x, cy - 18, VEL[7] if x < cx else VEL[6])
            if k > 1.3:
                S.set(x, cy - 17, VEL[6] if x < cx else VEL[5])
    for y in range(cy - 17, cy + 1):
        S.set(cx - 9, y, VEL[8]); S.set(cx - 8, y, VEL[6]); S.set(cx - 7, y, BRASS[2])
    for y in range(cy, cy + 5):
        for x in range(cx - 17, cx - 5):
            if ell_in(x + 0.5, y + 0.5, cx - 11, cy + 2.4, 6, 2.2):
                S.set(x, y, VEL[7] if y < cy + 2 else VEL[6])
    S.set(cx - 13, cy + 1, CORE)
    # the wick, bent over in the flame, its tip an ember
    for y in range(top - 6, top):
        S.set(cx, y, INK[0]); S.set(cx + 1, y, INK[1] if y > top - 4 else INK[0])
    S.set(cx + 1, top - 7, INK[0]); S.set(cx + 2, top - 7, FLAME[1]); S.set(cx + 2, top - 8, FLAME[2])
    # the flame: leaning a little, an orange tip, yellow body, white core, blue at the wick
    y0, y1 = 5 + oy, top - 3
    for y in range(y0, y1 + 1):
        t = (y - y0) / (y1 - y0)
        w = 6.9 * math.sin(math.pi / 2 * min(1.0, t / 0.7)) ** 1.25 if t < 0.7 else 6.9 * math.sqrt(max(0.0, 1 - ((t - 0.7) / 0.3) ** 2))
        xc = cx + 0.5 + 2.0 * (1 - t) ** 2
        for x in range(cx - 10, cx + 12):
            f = abs(x + 0.5 - xc) / max(0.6, w)
            if f > 1.0:
                continue
            if t < 0.13:
                c = FLAME[1]
            elif f > 0.74:
                c = FLAME[0] if t > 0.84 else FLAME[1]
            elif f > 0.48:
                c = FLAME[0] if t > 0.9 else FLAME[2]
            elif f > 0.22 or t < 0.36:
                c = FLAME[3] if t > 0.2 else FLAME[2]
            else:
                c = FLAME[4]
            if t > 0.8 and abs(x - cx) <= 1 and f <= 0.48:
                c = FLAME[2]                            # the dark cone round the wick
            S.set(x, y, c)
    # a dark line where the brass meets the oak, so the dish sits on the table
    m = {(x, y) for y in range(cy - 25, H) for x in range(W) if S.get(x, y)[3] == 255}
    for (x, y) in list(m):
        for (a, b) in ((0, 1), (1, 0), (-1, 0)):
            q = (x + a, y + b)
            if q not in m and y >= cy - 3:
                S.over(q[0], q[1], KD, 200)
    return S


ASSETS['prop-candle-2x'] = candle_big


def glow_big(rx=176, ry=140, peak=0.44):
    """the candle's light on the oak, the kit's stepped warm pool at the big candle's reach, dithered where it
    fades out"""
    s = Spr(2 * rx + 1, 2 * ry + 1)
    for y in range(2 * ry + 1):
        for x in range(2 * rx + 1):
            d = math.sqrt(((x - rx) / rx) ** 2 + ((y - ry) / ry) ** 2)
            if d > 1.0:
                continue
            a = (1.0 - d) ** 1.35 * peak
            step = round(a / 0.03) * 0.03
            if step <= 0.0 and dith(x, y, a / 0.03):
                step = 0.03
            if step > 0:
                s.set(x, y, rgba(WARM, round(step * 255)))
    return s


ASSETS['prop-glow-2x'] = glow_big


# ================================================================ the ink pot and quill, 2.5 times the kit's
def inkwell_big():
    """a squat inkwell of dark green glass at the kit's 2x pixel scale: the thick foot, the ink line seen
    through the glass, a cold window sliver on the left, the candle across the table caught small on the
    right, the shoulder, the neck and its rolled lip, the ink in the mouth with a glint; the goose quill
    standing in it and leaning into the page, its wide vane on the lit side, barbs ticked and split in two
    places, the shaft stained with ink where it dips"""
    W, H = 72, 150
    S = Spr(W, H)
    cx, cy, RX = 32, 136, 25
    top = cy - 24
    for y in range(top - 6, H):
        for x in range(W):
            if ell_in(x, y, cx + 6, cy + 4, RX + 2, 8):
                S.set(x, y, rgba(SH, 125))

    G2 = []                                             # the glass ramp in half steps, for a round body
    for i in range(len(GLASS)):
        G2.append(GLASS[i])
        if i + 1 < len(GLASS):
            G2.append(mix(GLASS[i], GLASS[i + 1], 0.5))

    def body_col(x, y):
        u = (x + 0.5 - (cx + 0.5)) / RX
        v = 8.6 - 4.6 * (u + 0.52) ** 2                 # brightest a third in from the window side
        if u < -0.86:
            v -= 3.5                                    # the far wall of the glass, turned away
        if y > top + 7:
            v -= 2                                      # the ink inside, below its line
        if y >= cy - 3 and -0.8 < u < 0.5:
            v += 2                                      # the thick glass of the foot
        return G2[max(0, min(len(G2) - 1, int(round(v))))]
    for y in range(top, cy + 8):
        for x in range(W):
            xx, yy = x + 0.5, y + 0.5
            ins = (abs(xx - cx - 0.5) <= RX and y <= cy) or ell_in(xx, yy, cx + 0.5, cy, RX, 7)
            if ins:
                S.set(x, y, body_col(x, y))
                if not ell_in(xx, yy + 1, cx + 0.5, cy, RX, 7) and y >= cy:
                    S.set(x, y, GLASS[0])
    for y in range(top + 4, cy - 5):                    # the window, a cold sliver on the left
        S.set(cx - 14, y, GLASS[5] if y < top + 13 else GLASS[4])
        if y < top + 9:
            S.set(cx - 13, y, GLASS[4])
    for y in range(top + 5, top + 12):                  # the candle across the table, small and warm
        S.set(cx + 18, y, '#e0a060' if y < top + 9 else '#9a6534')
    S.set(cx + 18, top + 6, '#ffd27a')
    for y in range(top - 9, top + 9):                   # the shoulder, domed and lit at the back left
        for x in range(W):
            xx, yy = x + 0.5, y + 0.5
            if ell_in(xx, yy, cx + 0.5, top, RX, 8.5):
                v = (xx - cx - 0.5) / RX + (yy - top) / 8.5 * 0.9
                S.set(x, y, GLASS[5] if v < -1.05 else GLASS[4] if v < -0.45 else GLASS[3] if v < 0.3 else GLASS[2])
    for y in range(top - 14, top - 3):                  # the neck
        for x in range(cx - 10, cx + 12):
            t = (x - (cx - 10)) / 21.0
            S.set(x, y, GLASS[4] if t < 0.2 else GLASS[3] if t < 0.5 else GLASS[2] if t < 0.8 else GLASS[1])
    for y in range(top - 19, top - 9):                  # the rolled lip, the ink in the mouth
        for x in range(cx - 13, cx + 15):
            xx, yy = x + 0.5, y + 0.5
            if ell_in(xx, yy, cx + 0.5, top - 14, 12.5, 4.2):
                ang = math.atan2(yy - (top - 14), xx - cx - 0.5)
                L = math.cos(ang + 2.4)
                S.set(x, y, GLASS[5] if L > 0.7 else GLASS[4] if L > 0 else GLASS[3])
                if ell_in(xx, yy, cx + 0.5, top - 14.3, 8.6, 2.5):
                    S.set(x, y, '#08070a')
    S.set(cx - 3, top - 15, '#3d4658'); S.set(cx - 2, top - 15, '#2a3040')
    for y in range(top - 12, top + 6):                  # a dribble of ink down the shoulder
        S.set(cx + 7, y, '#0b0a0d' if y < top + 3 else '#141a17')
    # the quill
    x0, y0, x1, y1 = cx + 2, top - 15, cx + 14, 3
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        t = i / n
        x, y = round(x0 + (x1 - x0) * t - 2.2 * math.sin(math.pi * t)), round(y0 + (y1 - y0) * t)   # the shaft bows
        if t > 0.2:
            s = min(1.0, (t - 0.2) / 0.8)
            wl = round(9.5 * math.sin(math.pi * s ** 0.75) + (1.5 if s > 0.9 else 0))
            wr = max(1, round(wl * 0.42))
            split = (0.50 < t < 0.53) or (0.69 < t < 0.715)
            for k in range(1, wl + 1):
                if split and k > wl * 0.35:
                    continue
                c = VEL[8] if k == wl else VEL[6] if k == 1 else (VEL[5] if (i + k) % 3 == 0 else VEL[7])
                S.set(x - k, y, c)
            for k in range(1, wr + 1):
                S.set(x + k, y, VEL[4] if (i + k) % 3 == 0 else VEL[5])
            if t < 0.27 and i % 2 == 0:
                S.set(x - wl - 1, y + 1, VEL[6]); S.set(x + wr + 1, y + 1, VEL[4])   # the down at its foot
        c = INK[1] if t < 0.07 else INK[3] if t < 0.2 else (VEL[5] if t < 0.97 else VEL[8])
        S.set(x, y, c)
    S.set(x1, y1 - 1, VEL[8]); S.set(x1 - 1, y1 - 1, VEL[7])
    # the dark edge where the glass meets the oak
    m = {(x, y) for y in range(top - 20, H) for x in range(W) if S.get(x, y)[3] == 255}
    for (x, y) in list(m):
        for (a, b) in ((0, 1), (1, 0)):
            q = (x + a, y + b)
            if q not in m and y > top:
                S.over(q[0], q[1], KD, 190)
    return S


ASSETS['prop-inkwell-2x'] = inkwell_big


# ================================================================ the tier 21 hero: the cloth of gold
def cloth_of_gold(W=160, seed=21):
    """the Gilded Order banner cloth as the banner builder will show it: a gonfalon of cloth of gold hung by
    its sleeve from a gilt crossbar, the bar slung from the pole top on two cords, tassels at its ends; the
    cloth woven in a lozenge diaper with a bright knot at each crossing, hanging in folds that deepen toward
    the foot, cut into three points with a tassel at each; an ivory border inside a gilt edge; the crowned
    sword in ivory, couched in dark gold thread; an ash pole behind with a gilt orb and spike on top"""
    S = Spr(W, W)
    ASH = ['#2a1d12', '#4a3420', '#6b4d30', '#8c6a44', '#ad8a5e', '#c9a97a']
    pc = 80
    x0, x1, y0 = 33, 127, 30
    pts = [x0 + (x1 - x0) * k / 6.0 for k in (1, 3, 5)]

    def fold(x):
        return math.sin((x - x0) / (x1 - x0) * 2 * math.pi * 2.5 + 0.6)

    def foot(x):
        k = (x - x0) / (x1 - x0) * 3.0
        tooth = abs((k % 1.0) - 0.5) * 2.0
        return 126 + (1 - tooth) * 20 + 1.2 * fold(x)
    cloth = {(x, y) for x in range(x0, x1 + 1) for y in range(y0, 152) if y <= foot(x)}
    # the cloth shadow on the backdrop
    for (x, y) in cloth:
        S.set(x + 4, y + 4, rgba(SH, 110))
    # the pole, behind the cloth
    for y in range(12, W):
        for i, c in enumerate((ASH[0], ASH[2], ASH[5], ASH[4], ASH[3], ASH[3], ASH[2], ASH[1], ASH[0])):
            S.set(pc - 4 + i, y, c)
    for yy in (150, 156):
        for i, c in enumerate((KD, GOLD[5], GOLD[6], GOLD[5], GOLD[4], GOLD[3], GOLD[3], GOLD[2], KD)):
            S.set(pc - 4 + i, yy, c); S.set(pc - 4 + i, yy + 1, GOLD[1] if c != KD else KD)
    # the orb and spike on the pole top
    for y in range(0, 14):
        for x in range(pc - 6, pc + 7):
            xx, yy = x + 0.5, y + 0.5
            if (xx - pc - 0.5) ** 2 + (yy - 9) ** 2 <= 4.6 ** 2:
                v = (xx - pc - 0.5) * 0.6 + (yy - 9) * 0.8
                S.set(x, y, GOLD[6] if v < -2.4 else GOLD[5] if v < -0.6 else GOLD[4] if v < 1.2 else GOLD[2])
            elif abs(xx - pc - 0.5) < max(0, (5 - yy) * 0.45) and yy < 5:
                S.set(x, y, GOLD[6] if xx < pc + 0.5 else GOLD[3])
    S.set(pc - 1, 7, SPEC)
    for i, c in enumerate((KD, GOLD[5], GOLD[6], GOLD[5], GOLD[4], GOLD[3], GOLD[2], GOLD[2], KD)):
        S.set(pc - 4 + i, 14, c); S.set(pc - 4 + i, 15, GOLD[1] if c != KD else KD)
    # the cords from the pole top to the ends of the bar
    for (xa, ya, xb, yb) in ((pc - 2, 16, x0 - 4, y0 + 1), (pc + 3, 16, x1 + 4, y0 + 1)):
        n = max(abs(xb - xa), abs(yb - ya))
        for i in range(n + 1):
            t = i / n
            x, y = round(xa + (xb - xa) * t), round(ya + (yb - ya) * t + 2.5 * math.sin(math.pi * t))
            S.set(x, y, GOLD[4] if i % 3 else GOLD[5]); S.set(x, y + 1, GOLD[1])
    # the crossbar, its knobs and the tassels under them
    for x in range(x0 - 8, x1 + 9):
        for j, c in enumerate((KD, GOLD[6], GOLD[5], GOLD[3], GOLD[1], KD)):
            S.set(x, y0 - 1 + j, c)
    for kx in (x0 - 10, x1 + 10):
        for y in range(y0 - 4, y0 + 6):
            for x in range(kx - 5, kx + 6):
                xx, yy = x + 0.5, y + 0.5
                d = math.hypot(xx - kx - 0.5, yy - y0 - 1.5)
                if d <= 4.2:
                    v = (xx - kx - 0.5) * 0.6 + (yy - y0 - 1.5) * 0.8
                    S.set(x, y, GOLD[6] if v < -1.8 else GOLD[5] if v < -0.3 else GOLD[3] if v < 1.5 else GOLD[1])
                elif d <= 5.2:
                    S.set(x, y, KD)
        for y in range(y0 + 6, y0 + 20):
            for i in range(-1, 2):
                c = GOLD[5] if i < 0 else GOLD[4] if i == 0 else GOLD[2]
                if y < y0 + 9:
                    c = GOLD[6] if i < 1 else GOLD[3]
                if y > y0 + 16 and (y + i) % 2:
                    continue
                S.set(kx + i, y, c)
    # the cloth: folds deepening toward the foot, lit from the top left, darker down and to the right
    depth = {}
    frontier = [p for p in cloth if any((p[0] + a, p[1] + b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    for p in frontier:
        depth[p] = 1
    k = 1
    while frontier and k < 9:
        nxt = []
        for (x, y) in frontier:
            for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + a, y + b)
                if q in cloth and q not in depth:
                    depth[q] = k + 1
                    nxt.append(q)
        frontier, k = nxt, k + 1
    for (x, y) in cloth:
        u = (x - x0) / (x1 - x0)
        amp = 0.45 + 0.55 * (y - y0) / 116.0
        f = fold(x) * amp
        v = 3.4 + 2.3 * f - 0.8 * u - 0.9 * (y - y0) / 116.0
        if (x + y) % 14 == 0 or (x - y) % 14 == 0:
            v -= 0.55
            if (x + y) % 14 == 0 and (x - y) % 14 == 0:
                v += 2.4
        elif ((x + y) % 14 in (1, 13)) and ((x - y) % 14 in (1, 13)):
            v += 1.0
        i = int(math.floor(v))
        if dith(x, y, v - i):
            i += 1
        S.set(x, y, GOLD[max(0, min(6, i))])
        dd = depth.get((x, y), 9)
        if dd == 1:
            top_or_left = (x, y - 1) not in cloth or (x - 1, y) not in cloth
            S.set(x, y, GOLD[6] if top_or_left else GOLD[2])
        elif dd in (5, 6) and y > y0 + 7:
            S.set(x, y, VEL[8] if v > 4.0 else VEL[7] if v > 3.0 else VEL[6] if v > 2.0 else VEL[5])
    for x in range(x0, x1 + 1):                         # the sleeve over the bar, and its seam
        for y in range(y0, y0 + 6):
            if (x, y) in cloth and depth.get((x, y), 9) > 1:
                S.set(x, y, GOLD[5] if y == y0 + 1 else GOLD[4] if y < y0 + 4 else GOLD[3])
        if x % 3 and (x, y0 + 6) in cloth:
            S.set(x, y0 + 6, GOLD[1])
    for (x, y) in {(x + a, y + b) for (x, y) in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - cloth:
        if y > y0 + 1:
            S.set(x, y, KD)
    # the crowned sword in ivory, couched in dark gold thread
    dev = set()
    for y in range(42, 57):                             # the crown: three points with pearls, a jewelled band
        for x in range(66, 95):
            xx = x + 0.5
            band = 51 <= y <= 56 and 67 <= x <= 93
            pt = False
            for (px_, ty, hw) in ((70.5, 46, 3.5), (80.5, 43, 4.0), (90.5, 46, 3.5)):
                if ty <= y <= 51 and abs(xx - px_) <= hw * (y - ty + 1) / (52 - ty):
                    pt = True
            if band or pt:
                dev.add((x, y))
    for (px_, ty) in ((70, 43), (80, 40), (90, 43)):
        for (a, b) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            dev.add((px_ + a, ty + b))
    for y in range(57, 62):                             # the pommel
        for x in range(75, 86):
            if ((x + 0.5 - 80.5) / 5.2) ** 2 + ((y + 0.5 - 59.5) / 2.8) ** 2 <= 1:
                dev.add((x, y))
    for y in range(62, 72):                             # the grip
        for x in range(78, 83):
            dev.add((x, y))
    for y in range(71, 77):                             # the guard, its quillons flared
        for x in range(58, 103):
            end = x < 61 or x > 99
            if 72 <= y <= 75 or end:
                dev.add((x, y))
    for y in range(77, 121):                            # the blade, tapering to its point
        hw = 5.4 - 5.0 * (y - 77) / 43.0
        for x in range(73, 89):
            if abs(x + 0.5 - 80.5) <= hw:
                dev.add((x, y))
    for (x, y) in dev:
        S.set(x + 1, y + 1, GOLD[1])
    for (x, y) in dev:
        u = x + 0.5 - 80.5
        c = VEL[8] if u < -2 else VEL[7] if u < 1 else VEL[6] if u < 3.5 else VEL[5]
        if 62 <= y < 72 and (x + y) % 3 == 0:
            c = VEL[4]                                  # the grip binding
        if 79 <= y <= 110 and x == 80:
            c = VEL[4]                                  # the fuller
        if 52 <= y <= 54 and x in (71, 72, 80, 81, 89, 90):
            c = WAX[4] if y == 53 else WAX[3]           # garnets in the band
        S.set(x, y, c)
    for (x, y) in {(x + a, y + b) for (x, y) in dev for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - dev:
        if (x, y) in cloth:
            S.set(x, y, GOLD[0])
    # the tassels at the three points
    for px_ in pts:
        bx = round(px_)
        by = round(foot(px_))
        for y in range(by + 1, by + 9):
            for i in range(-1, 2):
                if y > by + 6 and (y + i) % 2:
                    continue
                c = GOLD[6] if y < by + 3 and i < 1 else GOLD[5] if i < 0 else GOLD[4] if i == 0 else GOLD[2]
                S.set(bx + i, y, c)
        S.set(bx - 1, by + 1, KD); S.set(bx + 1, by + 1, KD)
    return S


ASSETS['cloth-gold-160'] = cloth_of_gold
ASSETS['seal-sword'] = lambda: wax_seal(11, SWORD, seed=17, drips=((0.7, 2),))
