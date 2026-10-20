"""The Chapter plaque, part three: the hero letter and its seals, the pennant on its pole, the chapter's XP
rail, the Claim plate gone dull, the rail's Chapter plaque marked as the page you are on."""
import math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
sys.path.insert(0, HERE)
from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v4pal import *
from v4a_furn import plaque, brass_button, key_engraved, screw, engrave, brushed, tarnish, SPEC, TARN, SH
from v4a_paper import parchment, wax_seal, ribbon, SHADOW
import v4a_portrait
from trackart import DULL, sepia, soft_shadow, pad

ASSETS = {}


# ================================================================ the hero: the next big reward
# the letter lies landscape: the live window on the left, the reward's words on the right
ASSETS['hero-letter'] = lambda: parchment(400, 200, kind='hi', seed=19, shadow=(4, 6), folds=(140,), stains=0,
                                          rim=3, mottle='edge', fibres=0.6)[0]
ASSETS['pt-longbow-160'] = lambda: v4a_portrait.portrait('longbow', 160)

LOZENGE = ['...x...',      # Rare: a lozenge, as the recruit table marks a Rare
           '..xxx..',
           '.xxxxx.',
           'xxxxxxx',
           '.xxxxx.',
           '..xxx..',
           '...x...']
CHEVRON = ['....x....',    # a banner cosmetic: the chevron the pennant carries
           '...xxx...',
           '..xx.xx..',
           '.xx...xx.',
           'xx.....xx']
ASSETS['seal-rare'] = lambda: wax_seal(11, LOZENGE, seed=12, drips=((2.1, 2),))
ASSETS['seal-chevron'] = lambda: wax_seal(11, CHEVRON, seed=15, drips=((1.0, 2),))


def pennant_on_pole(W=160, seed=4):
    """the tier 21 reward as the banner builder will show it: an ash pole with brass ferrules and a leaf
    point, a long swallowtail pennant flying from three brass rings. The cloth is ivory linen, woven (a
    twill of half-steps, a lit weft every fourth row), shaded by its wave (lit where it turns up to the
    light, dark in the troughs), edged in gold thread, and carries the pattern the tier gives: two brass
    chevrons. Brass and ivory, never a team colour: the side colour changes every match (Kort, round 1
    ruling 5). The window's backdrop is drawn by frame-back-160 under it."""
    rnd = random.Random(seed)
    S = Spr(W, W)
    T = [VEL[0], VEL[2], VEL[4], VEL[5], VEL[6], VEL[7]]
    px0 = 40                      # the pole's left edge
    hoist0, hoist1 = 30, 74       # where the cloth meets the pole
    x0, x1 = px0 + 6, 150
    L = x1 - x0
    cloth = {}

    def geo(x):
        u = (x - x0) / L
        hh = (hoist1 - hoist0) / 2 * (1 - 0.55 * u)
        yc = (hoist0 + hoist1) / 2 + 9 * u * u + 5.5 * math.sin(2 * math.pi * (u * 1.25) + 0.4) * min(1, u * 2.2)
        slope = 5.5 * math.cos(2 * math.pi * (u * 1.25) + 0.4) * 2 * math.pi * 1.25 / L * min(1, u * 2.2) + 18 * u / L
        return u, hh, yc, slope
    for x in range(x0, x1 + 1):
        u, hh, yc, slope = geo(x)
        for y in range(int(yc - hh) - 1, int(yc + hh) + 2):
            if abs(y + 0.5 - yc) > hh:
                continue
            if u > 0.78 and abs(y + 0.5 - yc) < (u - 0.78) / 0.22 * hh * 0.95:
                continue
            cloth[(x, y)] = (u, yc, hh, slope)
    # shading: light from the top left; the cloth faces up where the wave rises toward the fly
    for (x, y), (u, yc, hh, slope) in cloth.items():
        t = 3 - slope * 9.0
        t += -0.35 if y < yc - hh * 0.55 else (0.35 if y > yc + hh * 0.55 else 0)
        t = max(1, min(5, round(t)))
        c = T[t]
        if (x + 2 * y) % 5 == 0:
            c = mix(T[t], T[max(0, t - 1)], 0.45)
        elif y % 4 == 1 and (x * 3 + y) % 7 != 0:
            c = mix(T[t], T[min(5, t + 1)], 0.3)
        S.set(x, y, c)
    # the pattern: two brass chevrons pointing to the fly, following the cloth
    IV = [BRASS[2], BRASS[4], BRASS[5], BRASS[6]]
    for (u0, wd) in ((0.22, 0.075), (0.47, 0.075)):
        for (x, y), (u, yc, hh, slope) in cloth.items():
            off = abs(y + 0.5 - yc) / max(1.0, hh) * 0.16
            if u0 - off <= u <= u0 - off + wd:
                t = 3 - slope * 9.0 + (-0.35 if y < yc - hh * 0.55 else 0.35 if y > yc + hh * 0.55 else 0)
                S.set(x, y, IV[max(0, min(3, round(t) - 1))])
    # gold thread along the edges, the outline
    m = set(cloth)
    for (x, y) in m:
        if (x, y - 1) not in m and x > x0:
            S.set(x, y, BRASS[6] if x % 3 else BRASS[5])
        elif (x, y + 1) not in m and x > x0:
            S.set(x, y, BRASS[4] if x % 3 else BRASS[3])
    out = {(x + a, y + b) for (x, y) in m for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - m
    for (x, y) in out:
        S.set(x, y, T[0])
    # the pole: ash, lit on its left, grain in short runs
    ASH = ['#2a1d12', '#4a3420', '#6b4d30', '#8c6a44', '#ad8a5e', '#c9a97a']
    for y in range(14, W):
        for i, c in enumerate((ASH[1], ASH[5], ASH[4], ASH[3], ASH[3], ASH[2], ASH[1])):
            S.set(px0 - 1 + i, y, c)
        if rnd.random() < 0.18:
            S.set(px0 + 2 + rnd.randint(0, 2), y, ASH[2])
    for y in range(14, W):
        S.set(px0 - 2, y, ASH[0]); S.set(px0 + 6, y, ASH[0])
    # the brass rings holding the hoist, and two ferrules
    for yy in (hoist0 + 2, (hoist0 + hoist1) // 2, hoist1 - 3):
        for i, c in enumerate((BRASS[0], BRASS[6], BRASS[7], BRASS[5], BRASS[5], BRASS[4], BRASS[3], BRASS[2], BRASS[0])):
            S.set(px0 - 2 + i, yy, c)
            S.set(px0 - 2 + i, yy + 1, BRASS[0] if c == BRASS[0] else mix(c, BRASS[2], 0.4))
    for yy in (96, 140):
        for k in range(4):
            for i, c in enumerate((BRASS[0], BRASS[6], BRASS[7], BRASS[6], BRASS[5], BRASS[4], BRASS[3], BRASS[2], BRASS[0])):
                S.set(px0 - 2 + i, yy + k, c if k not in (0, 3) else (BRASS[0] if c == BRASS[0] else BRASS[3]))
    # the leaf point
    tip = ['....k....',
           '...khk...',
           '...khk...',
           '..khmdk..',
           '..khmdk..',
           '.khhmmdk.',
           '.khmmmdk.',
           '.khmmmdk.',
           '..khmdk..',
           '...kdk...',
           '..kkkkk..',
           '.kHmmmdk.',
           '..kmmdk..']
    S.grid(px0 - 2, 1, tip, {'k': BRASS[0], 'h': BRASS[7], 'H': SPEC, 'm': BRASS[5], 'd': BRASS[3]})
    S.set(px0 + 2, 3, SPEC)
    # the cloth's shadow on the pole side where it wraps
    for y in range(hoist0, hoist1):
        if S.get(px0 + 6, y)[3]:
            S.set(px0 + 7, y, T[0])
    return S


ASSETS['pennant-160'] = pennant_on_pole


# ================================================================ the chapter's XP rail
def xp_rail(w=368, fill=138, tiers=30):
    """the chapter as one brass rail screwed to the table: a frame lit on its top and left, a broad top band
    engraved with the 30 tier marks (every fifth longer), a channel cut along the middle, a bottom band,
    a slotted screw at each end of each band, and the brass poured into the channel as far as the XP goes,
    polished, its head rounded"""
    H = 20
    S = Spr(w, H)
    rnd = random.Random(3)
    S.rect(0, 0, w, H, BRASS[0])
    S.rect(1, 1, w - 2, H - 2, BRASS[5])
    brushed(S, 2, 2, w - 2, 6, rnd, 1.8)
    brushed(S, 2, 14, w - 2, 18, rnd, 1.8)
    S.hline(1, w - 2, 1, BRASS[7]); S.vline(1, 1, H - 2, BRASS[6])
    S.hline(1, w - 2, H - 2, BRASS[3]); S.vline(w - 2, 1, H - 2, BRASS[3])
    for x in range(2, 14):
        S.set(x, 1, SPEC if x < 8 else BRASS[7])
    c0, c1 = 4, w - 5               # the channel, 360 px: 12 px a tier
    S.hline(c0 - 1, c1 + 1, 6, BRASS[2]); S.vline(c0 - 1, 6, 13, BRASS[2])
    S.hline(c0 - 1, c1 + 1, 13, BRASS[6]); S.vline(c1 + 1, 6, 13, BRASS[6])
    for x in range(c0, c1 + 1):
        S.set(x, 7, OAK[0])
        for y in (8, 9, 10, 11):
            S.set(x, y, OAK[1])
        S.set(x, 12, OAK[2])
    step = (c1 - c0 + 1) / tiers
    for k in range(1, tiers):
        x = c0 + round(k * step) - 1
        long = k % 5 == 0
        for y in ((2, 3, 4, 5) if long else (3, 4, 5)):
            S.set(x, y, BRASS[2])
        for y in ((3, 4, 5) if long else (4, 5)):
            S.set(x + 1, y, BRASS[6])
        S.set(x, 12, OAK[0])
    # the fill
    for x in range(c0, c0 + fill):
        S.set(x, 7, BRASS[6]); S.set(x, 8, SPEC if x % 7 == 3 else BRASS[7]); S.set(x, 9, BRASS[6])
        S.set(x, 10, BRASS[5]); S.set(x, 11, BRASS[4]); S.set(x, 12, BRASS[3])
        if x % 11 == 6:
            S.set(x, 10, BRASS[4])
    hx = c0 + fill - 1
    for y, c in ((7, BRASS[4]), (8, BRASS[6]), (9, BRASS[5]), (10, BRASS[4]), (11, BRASS[3]), (12, BRASS[2])):
        S.set(hx, y, c)
    for y in (8, 9, 10, 11):
        S.set(hx + 1, y, BRASS[2])
    # the screws at the ends of each band
    for (x, y, sl) in ((1, 1, '/'), (w - 6, 1, '-'), (1, H - 6, '|'), (w - 6, H - 6, 'x')):
        screw(S, x, y, sl)
    for (dx, dy) in ((0, 0), (w - 1, 0), (0, H - 1), (w - 1, H - 1)):
        S.set(dx, dy, None)
    return soft_shadow(pad(S, 0, 0, 2, 3), [(1, 1, 0.55), (1, 2, 0.4), (2, 3, 0.16)])


ASSETS['xp-rail'] = xp_rail


# ================================================================ Claim gone dull, and the rail's plaque for this page
DARK = ['#100e0b', '#1b1813', '#26221b', '#302b22', '#3a342a', '#443d31', '#5a5141', '#6f6552']


def dull_plate():
    """the Claim plate when there is nothing to claim: the same plate gone dark with tarnish, its shine gone"""
    s = brass_button(240, 40)
    table = {rgba(a)[:3]: rgba(b)[:3] for a, b in zip(BRASS + [SPEC], DARK + [DARK[7]])}
    for k, c in zip(TARN, ('#2a261d', '#353024', '#3d3a2c')):
        table[rgba(k)[:3]] = rgba(c)[:3]
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if p[3] and p[:3] in table:
                s.px[x, y] = table[p[:3]] + (p[3],)
    return s


def dull_key(w=37):
    s = key_engraved(w)
    table = {rgba(a)[:3]: rgba(b)[:3] for a, b in zip(BRASS, DARK)}
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if p[3] and p[:3] in table:
                s.px[x, y] = table[p[:3]] + (p[3],)
    return s


ASSETS['btn-dull-240x40'] = dull_plate
ASSETS['key-dull-37'] = dull_key


def plaque_here(w=112):
    """the rail's Chapter plaque while the Chapter page is open: the brass polished a step brighter and the
    oak panel lit warm, as if drawn toward the lamp"""
    s = plaque(w)
    up = {rgba(OAK[i])[:3]: rgba(OAK[min(8, i + 3)])[:3] for i in range(9)}
    br = {rgba(BRASS[i])[:3]: rgba(BRASS[i + 1] if i < 7 else SPEC)[:3] for i in range(8)}
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if not p[3]:
                continue
            if 4 <= x < w - 4 and 4 <= y < 24 and p[:3] in up:
                s.px[x, y] = up[p[:3]] + (p[3],)
            elif p[:3] in br and p[3] == 255:
                s.px[x, y] = br[p[:3]] + (p[3],)
    return s


ASSETS['plaque-112-here'] = plaque_here
