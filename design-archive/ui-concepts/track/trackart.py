"""The Chapter plaque (the Founders reward track): sprites painted at GUI scale 1 into track/art/.
Run from work/ui-concepts:  python track/trackart.py [name ...]
Built on the accepted v4 painters (v4lib, v4pal, v4a_furn, v4a_paper, v4a_recruit, v4a_portrait): the same
ramps, light from the top left, clustered shading, outlines and drop shadows. New here: the hanging tier
plaques and their states (lit, raised, dull under an iron hasp, sepia for an Oath not sworn), the chains,
the tier tags on the rod, the wax stamps (the Hollow's mark, the tier seal), the chapter's XP rail, the
reward icons, the landscape hero letter and the pennant on its pole."""
import math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, ROOT)
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v4pal import *
from v4a_furn import plaque, brass_button, key_engraved, screw, engrave, brushed, tarnish, arc, SPEC, TARN, SH
from v4a_paper import parchment, wax_seal, ribbon, CROWN, SHADOW, PAPER
from v4a_recruit import HOUSES, DEVICES, embroider, frame_ornate
import v4a_portrait

OUT = os.path.join(HERE, 'art')
os.makedirs(OUT, exist_ok=True)
ASSETS = {}


# ================================================================ ramps
# tarnished brass for anything not yet reached: the brass ramp pulled halfway to iron and down a step
DULL = [mix(mix(BRASS[i], IRON[min(8, i + 1)], 0.55), '#000000', 0.10) for i in range(8)]
# sepia: the Oath row for a player sworn to no House, drawn as an old brown print
SEP = ['#17120d', '#221a12', '#2f2419', '#3e3022', '#4f3e2c', '#615038', '#776446', '#8c7853', '#a08c64']
RUSTW = ['#3b2214', '#4f2c18', '#63391f']


def sepia(src, lo=0.0, hi=1.0):
    """map every pixel's luma onto the sepia ramp, squeezed into [lo, hi] of it; alpha kept"""
    o = Spr(src.w, src.h)
    for y in range(src.h):
        for x in range(src.w):
            p = src.px[x, y]
            if p[3] == 0:
                continue
            l = (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255.0
            t = lo + (hi - lo) * l
            c = SEP[max(0, min(len(SEP) - 1, int(t * (len(SEP) - 0.01))))]
            o.px[x, y] = rgba(c)[:3] + (p[3],)
    return o


def soft_shadow(S, steps):
    """a stepped shadow under an opaque shape: steps = [(dx, dy, alpha), ...] nearest first"""
    m = {(x, y) for y in range(S.h) for x in range(S.w) if S.px[x, y][3] > 200}
    sh = Spr(S.w, S.h)
    for (dx, dy, a) in steps:
        for (x, y) in m:
            k = (x + dx, y + dy)
            if k not in m and sh.get(*k)[3] < a * 255:
                sh.set(k[0], k[1], rgba(SH, round(a * 255)))
    sh.blit(S, 0, 0)
    S.im = sh.im; S.px = sh.px
    return S


def pad(S, l=0, t=0, r=0, b=0):
    o = Spr(S.w + l + r, S.h + t + b)
    o.blit(S, l, t)
    return o


# ================================================================ the tier plaque (hangs from the rod)
def tier_plaque(w=68, h=80, seed=1, kind='lit'):
    """a brass-framed oak plaque, made as the rail's nav plaques are, taller: a 4 px frame (outline, lit
    outer bevel, brushed face, inner bevel falling into the panel), a slotted screw in each corner, grime
    by the screws, the recessed quarter-sawn panel. kind: lit (reached), raised (the current tier: the
    brass polished, a deeper shadow), dull (not reached: the brass tarnished toward iron, the oak darker)"""
    rnd = random.Random(seed * 31 + w + h)
    M = DULL if kind == 'dull' else (BRASS[1:] + [SPEC] if kind == 'raised' else BRASS)
    s = Spr(w, h)
    s.rect(0, 0, w, h, M[0])
    s.rect(1, 1, w - 2, h - 2, M[5])
    brushed(s, 2, 2, w - 2, 4, rnd, 2.0, m=M)
    brushed(s, 2, h - 4, w - 2, h - 2, rnd, 2.0, m=M)
    for y in range(4, h - 4):
        for x in (2, w - 3):
            if rnd.random() < 0.3:
                s.set(x, y, M[6] if x == 2 else M[4])
    s.hline(1, w - 2, 1, M[6]); s.vline(1, 1, h - 2, M[6])
    s.hline(1, w - 2, h - 2, M[3]); s.vline(w - 2, 1, h - 2, M[3])
    s.set(w - 2, 1, M[5]); s.set(1, h - 2, M[4])
    glint = SPEC if kind == 'raised' else M[7]
    for x in range(3, w - 3):
        if rnd.random() < (0.22 if kind == 'raised' else 0.12):
            s.set(x, 1, M[7])
    for k in range(8 if kind == 'raised' else 6):
        s.set(2 + k, 1, glint if k < 4 else M[7])
    s.set(1, 2, M[7]); s.set(1, 3, M[7])
    if kind == 'raised':
        for k in range(5):
            s.set(1, 4 + k, M[7] if k < 3 else M[6])
    for x in range(2, w - 2):
        if s.get(x, 2)[:3] == rgba(M[5])[:3] and rnd.random() < 0.25:
            s.set(x, 2, M[6])
    s.hline(3, w - 4, 3, M[3]); s.vline(3, 3, h - 4, M[3])
    s.hline(3, w - 4, h - 4, M[6]); s.vline(w - 4, 3, h - 4, M[6])
    s.set(3, 3, M[2]); s.set(w - 4, h - 4, M[7])
    # the oak panel, quarter-sawn: straight grain in runs, pale ray flecks across it
    base = 1 if kind == 'dull' else (3 if kind == 'raised' else 2)
    for y in range(4, h - 4):
        for x in range(4, w - 4):
            s.set(x, y, OAK[base])
    for k in range(int((h - 8) / 2.0) + 1):
        yb = 4.6 + k * 2.0 + rnd.random() * 0.8
        amp = rnd.uniform(0.2, 0.7); L = rnd.uniform(50, 130); ph = rnd.uniform(0, 6.28)
        tone = rnd.choice([base - 1, base - 1, base + 1, base + 1, base + 1])
        x = rnd.randint(-20, 4); on = True
        while x < w:
            run = rnd.randint(10, 40) if on else rnd.randint(3, 12)
            if on:
                for xx in range(max(5, x), min(w - 5, x + run)):
                    Y = round(yb + amp * math.sin(6.283 * xx / L + ph))
                    if 5 <= Y <= h - 6:
                        s.set(xx, Y, OAK[max(0, tone)])
            on = not on; x += run
    for _ in range(int(w * h / 260)):
        x = rnd.randint(6, w - 9); y = rnd.randint(6, h - 7)
        for i in range(rnd.randint(2, 3)):
            s.set(x + i, y, OAK[base + 2] if i else OAK[base + 1])
    s.hline(4, w - 5, 4, OAK[0]); s.vline(4, 4, h - 5, OAK[0])
    s.hline(5, w - 5, 5, OAK[max(0, base - 1)]); s.vline(5, 5, h - 6, OAK[max(0, base - 1)])
    s.hline(5, w - 5, h - 5, OAK[base + 1])
    slots = rnd.sample(['-', '|', '/', 'x'], 4)
    for i, (cx, cy) in enumerate(((1, 1), (w - 6, 1), (1, h - 6), (w - 6, h - 6))):
        tarnish(s, cx + 2, cy + 2, 2.2, rnd, 5, keep=lambda X, Y: 4 <= X < w - 4 and 4 <= Y < h - 4)
        screw(s, cx, cy, slots[i], metal=M)
    if kind == 'dull':
        # verdigris and grime along the frame, where the damp sits
        for _ in range(18):
            X = rnd.randint(6, w - 7); Y = rnd.choice([h - 3, h - 2, 2])
            s.set(X, Y, TARN[rnd.randint(0, 2)])
    if kind == 'raised':
        return soft_shadow(pad(s, 0, 0, 4, 5), [(1, 1, 0.62), (2, 2, 0.46), (3, 3, 0.3), (4, 5, 0.14)])
    return soft_shadow(pad(s, 0, 0, 2, 3), [(1, 1, 0.55), (2, 3, 0.25)])


ASSETS['plaque-tier'] = lambda: tier_plaque(seed=1)
ASSETS['plaque-tier-b'] = lambda: tier_plaque(seed=2)
ASSETS['plaque-tier-raised'] = lambda: tier_plaque(68, 88, seed=3, kind='raised')
ASSETS['plaque-tier-dull'] = lambda: tier_plaque(seed=4, kind='dull')
ASSETS['plaque-tier-dull-b'] = lambda: tier_plaque(seed=5, kind='dull')
ASSETS['plaque-tier-sepia'] = lambda: sepia(tier_plaque(seed=6), 0.04, 0.82)


# ================================================================ chains and the rod
def chain(n=18, metal=BRASS):
    """a hanging chain: links alternate face-on (an oval ring, lit down its left side) and edge-on (a
    bar, lit on its left), each link through the last; a ring at the foot that sits on the plaque's frame"""
    m = metal
    S = Spr(6, n + 3)
    face = ['.hm.', 'h..d', 'h..d', 'm..d', '.dd.']
    edge = ['hk', 'mk', 'mk', 'dk']
    y = 0; i = 0
    while y < n - 1:
        if i % 2 == 0:
            S.grid(0, y, face, {'h': m[7], 'm': m[5], 'd': m[3]})
            y += 4
        else:
            S.grid(1, y, edge, {'h': m[7], 'm': m[5], 'd': m[3], 'k': m[1]})
            y += 3
        i += 1
    S.grid(0, n - 1, ['.hm.', 'h..d', '.dd.'], {'h': m[6], 'm': m[5], 'd': m[3]})
    for yy in range(S.h):
        for xx in range(S.w):
            if yy > n + 1:
                S.set(xx, yy, None)
    return soft_shadow(S, [(1, 1, 0.5)])


# rod to Roll: 18 links, 10 for the lifted current tier; Roll to Oath: 10, 6 for the current tier
for _n in (6, 10, 18):
    ASSETS['chain-%d' % _n] = (lambda n: (lambda: chain(n)))(_n)
for _n in (10, 18):
    ASSETS['chain-dull-%d' % _n] = (lambda n: (lambda: chain(n, DULL)))(_n)
ASSETS['chain-sepia-10'] = lambda: sepia(chain(10), 0.04, 0.82)


def track_rod(w=784):
    """the brass rod the tiers hang from: six tones round its curve, glints along the lit line, a turned
    collar at each end; the ends sit in the arrow bosses"""
    S = Spr(w, 11)
    rows = ((1, BRASS[0]), (2, BRASS[6]), (3, BRASS[7]), (4, BRASS[5]), (5, BRASS[4]), (6, BRASS[3]),
            (7, BRASS[2]), (8, BRASS[0]))
    rnd = random.Random(5)
    for x in range(w):
        for y, c in rows:
            S.set(x, y, c)
        if x % 9 == 0:
            S.set(x, 3, SPEC)
        if x % 13 == 5:
            S.set(x, 5, BRASS[3])
        if rnd.random() < 0.05:
            S.set(x, 6, TARN[1])
    for xc in (4, w - 6):
        for y, c in ((0, BRASS[0]), (1, BRASS[6]), (2, BRASS[7]), (3, BRASS[6]), (4, BRASS[5]), (5, BRASS[4]),
                     (6, BRASS[3]), (7, BRASS[2]), (8, BRASS[1]), (9, BRASS[0])):
            S.set(xc, y, c); S.set(xc + 1, y, BRASS[max(1, BRASS.index(c) - 1)] if c != BRASS[0] else c)
    for x in range(w):
        S.set(x, 9, rgba(SH, 120)); S.set(x, 10, rgba(SH, 50))
    return S


ASSETS['track-rod'] = lambda: track_rod(760)   # 130..890: the right boss ends on the page's right edge, 904


def tier_tag(w=21, h=15, metal=BRASS):
    """the tier's number tag: a brass sleeve clamped round the rod, wider than the rod so it shows above
    and below it; shaded as a cylinder (lit band on top, flat face for the number, a dark lower band),
    its ends turned down, a seam at each end"""
    m = metal
    S = Spr(w, h)
    bands = [m[0], m[7], m[6], m[5], m[5], m[5], m[5], m[5], m[5], m[5], m[5], m[4], m[3], m[2], m[0]]
    for y in range(h):
        for x in range(w):
            S.set(x, y, bands[min(len(bands) - 1, y)])
    for y in range(1, h - 1):
        S.set(0, y, m[0]); S.set(w - 1, y, m[0])
        S.set(1, y, m[3] if y > 2 else m[6]); S.set(w - 2, y, m[2] if y > 2 else m[4])
        S.set(2, y, m[4] if 2 < y < h - 3 else S.get(2, y)); S.set(w - 3, y, m[4] if 2 < y < h - 3 else S.get(w - 3, y))
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        S.set(x, y, None)
    S.set(3, 1, SPEC); S.set(4, 1, SPEC); S.set(5, 1, m[7])
    for x in range(6, w - 4, 4):
        S.set(x, 2, m[7])
    return soft_shadow(pad(S, 0, 0, 2, 2), [(1, 1, 0.55), (2, 2, 0.22)])


ASSETS['tag-tier'] = tier_tag


def arrow_stud(direction=1, dim=False):
    """a domed brass boss at the rod's end with a chevron cut into it: the way to the tiers off the table"""
    m = DULL if dim else BRASS
    S = Spr(15, 15)
    c = 7.0
    for y in range(15):
        for x in range(15):
            d = math.hypot(x - c, y - c)
            if d <= 7.2:
                l = -(x - c) / 7.0 - (y - c) / 7.0
                col = m[7] if l > 0.95 else m[6] if l > 0.45 else m[5] if l > -0.2 else m[4] if l > -0.75 else m[3]
                if d > 6.2:
                    col = m[0] if l < 0.4 else m[2]
                S.set(x, y, col)
    S.set(4, 4, SPEC); S.set(5, 4, m[7]); S.set(4, 5, m[7])
    chev = [(0, 0), (1, 1), (2, 2), (3, 3), (2, 4), (1, 5), (0, 6)]
    if direction < 0:
        chev = [(3 - x, y) for (x, y) in chev]
    pts = [(5 + x, 4 + y) for (x, y) in chev]
    pts += [(x + 1, y) for (x, y) in pts]
    engrave(S, pts, m)
    return soft_shadow(pad(S, 0, 0, 2, 2), [(1, 1, 0.55), (2, 2, 0.22)])


ASSETS['arrow-left'] = lambda: arrow_stud(-1)
ASSETS['arrow-right'] = lambda: arrow_stud(1)

