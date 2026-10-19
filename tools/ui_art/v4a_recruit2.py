"""v4 recruit sprites at full density, overriding v4a_recruit where they share a name: the House banners
woven (a twill in half-steps of the cloth ramp, a lit weft every fourth row, a sheen on each fold, the
fringe in two lengths with loose threads), the rod in six tones with specular glints, the bundle twine
twisted and its envelopes creased with a lit ridge and a shaded facet."""
import math, random
from v4lib import Spr, Noise, rgba, mix, grid_mask
from v4pal import *
from v4a_paper import parchment, PAPER, wax_seal, CROWN, SHADOW
from v4a_recruit import HOUSES, DEVICES, embroider

ASSETS = {}
SPEC = '#fff0c4'


def banner(house, w=104, h=50, chosen=False, seed=3):
    """a House banner on the rod: a sleeve with its stitched hem, woven cloth with folds near each side,
    a running-stitch trim, the device embroidered at the top, a pointed hem with a gold fringe"""
    ramp, thread = HOUSES[house]
    rnd = random.Random(seed + len(house) * 7 + (1 if chosen else 0))
    base = 4 if chosen else 3
    pt = 6
    S = Spr(w + 3, h + 10)
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
    tone = {}
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
        tone[(x, y)] = max(0, min(5, t))
    for (x, y), t in tone.items():
        c = ramp[t]
        if y >= top:
            # the weave: a twill of half-step darker picks, and a lit weft every fourth row
            if (x + 2 * y) % 5 == 0:
                c = mix(ramp[t], ramp[max(0, t - 1)], 0.45)
            elif y % 4 == 1 and (x * 3 + y) % 7 != 0:
                c = mix(ramp[t], ramp[min(5, t + 1)], 0.3)
        S.set(x, y, c)
    # a sheen on the lit side of each fold
    for f in folds:
        for y in range(top + 3, h - 6):
            if (f, y) in cloth and tone.get((f, y)) == min(5, base + 1):
                S.set(f, y, mix(ramp[min(5, base + 1)], ramp[5], 0.6))
    # the sleeve round the rod, its hem stitched
    lit, mid, shd = thread
    for x in range(w):
        S.set(x, 0, ramp[0]); S.set(x, 1, ramp[min(5, base + 1)]); S.set(x, 2, ramp[base]); S.set(x, 3, ramp[base - 1])
        S.set(x, 4, ramp[base - 2] if x % 3 else ramp[base - 1])
        if x % 4 == 1:
            S.set(x, 1, mix(ramp[min(5, base + 1)], ramp[5], 0.5))
        if 2 <= x < w - 2 and x % 3 != 2:
            S.set(x, 3, shd)
    # trim: a running stitch three pixels in from the sides and the hem, each stitch lit on its top
    i = 0
    for y in range(top + 2, hem(3) - 2):
        if i % 3 != 2:
            S.set(3, y, lit if i % 3 == 0 else mid); S.set(w - 4, y, lit if i % 3 == 0 else mid)
        i += 1
    for x in range(3, w - 3):
        if (x - 3) % 3 != 2:
            S.set(x, hem(x) - 3, lit if (x - 3) % 3 == 0 else mid)
    for (x, y) in cloth:
        if any((x + a, y + b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            S.set(x, y, ramp[0])
    # the fringe: tassels in two lengths, lit at the top, a few loose threads
    for x in range(1, w - 1):
        if x % 2 == 0:
            y = max(yy for (xx, yy) in cloth if xx == x)
            ln = 3 if (x // 2) % 2 else 4
            cols = (BRASS[6], BRASS[5], BRASS[3], BRASS[2])
            for k in range(1, ln + 1):
                S.set(x, y + k, cols[min(3, k - 1)])
            if rnd.random() < 0.12:
                S.set(x + 1, y + ln, BRASS[3])
    embroider(S, DEVICES[house], (w - 15) // 2, 8, thread, ramp[max(0, base - 2)])
    return S


for _h in HOUSES:
    ASSETS['banner-' + _h] = (lambda hh: (lambda: banner(hh)))(_h)
    ASSETS['banner-' + _h + '-on'] = (lambda hh: (lambda: banner(hh, h=60, chosen=True)))(_h)


def rod(w=600):
    """the brass banner rod: six tones round its curve, specular glints along the lit line, a turned collar
    inside each finial, ball finials with a specular point"""
    S = Spr(w, 10)
    rows = ((1, BRASS[0]), (2, BRASS[6]), (3, BRASS[7]), (4, BRASS[5]), (5, BRASS[4]), (6, BRASS[2]), (7, BRASS[0]))
    for x in range(6, w - 6):
        for y, c in rows:
            S.set(x, y, c)
        if x % 9 == 0:
            S.set(x, 3, SPEC)
        if x % 13 == 5:
            S.set(x, 5, BRASS[3])
    for xc in (7, w - 9):
        for y, c in ((0, BRASS[0]), (1, BRASS[6]), (2, BRASS[7]), (3, BRASS[6]), (4, BRASS[5]), (5, BRASS[4]),
                     (6, BRASS[3]), (7, BRASS[2]), (8, BRASS[0])):
            S.set(xc, y, c); S.set(xc + 1, y, c if c == BRASS[0] else BRASS[max(1, BRASS.index(c) - 1)])
    ball_rows = ['..kkk..',
                 '.khSmk.',
                 'khHmmdk',
                 'kHmmmdk',
                 'kmmmddk',
                 '.kmddk.',
                 '..kkk..']
    for x0 in (0, w - 7):
        S.grid(x0, 1, ball_rows, {'k': BRASS[0], 'h': BRASS[7], 'H': BRASS[6], 'S': SPEC, 'm': BRASS[5],
                                  'd': BRASS[3]})
    S.shadow(1, 2, 0.45)
    return S


ASSETS['banner-rod'] = lambda: rod(568)


# ================================================================ the bundle of sealed letters
TW = ['#4a331b', '#5c4225', '#7a5c33', '#9a7a46', '#b59258']    # twine, dark to light


def envelope(w, h, seed, tone='lo'):
    """one sealed letter from the back: four flaps meeting in creases; each crease a dark line with a lit
    ridge on the side toward the light and a shaded facet on the other; the top flap point below the middle"""
    S, paper = parchment(w, h, kind=tone, seed=seed, shadow=(1, 2), deckle=False, rim=2, stains=0, fibres=0.5,
                         mottle='edge', foxing=0.5)
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
            if (x, y + 2) in paper and x % 2 == 0:
                S.set(x, y + 2, T['shade'])
            if (x, y - 1) in paper and x < cx:
                S.set(x, y - 1, T['ridge'])
        yb = round(h - 3 - (h * 0.42) * (1 - t))
        if (x, yb) in paper and t > 0.12:
            S.set(x, yb, T['crease'])
            if (x, yb - 1) in paper and x % 3:
                S.set(x, yb - 1, T['ridge'])
    return S, vy


def twine_h(S, x0, x1, y):
    """two-ply twine laid horizontally: the plies twist, so the light catches every third pixel"""
    for i, x in enumerate(range(x0, x1)):
        k = i % 4
        S.set(x, y, (TW[4], TW[3], TW[2], TW[3])[k])
        S.set(x, y + 1, (TW[2], TW[1], TW[2], TW[0])[k])


def twine_v(S, x, y0, y1):
    for i, y in enumerate(range(y0, y1)):
        k = i % 4
        S.set(x, y, (TW[4], TW[3], TW[2], TW[3])[k])
        S.set(x + 1, y, (TW[2], TW[1], TW[2], TW[0])[k])


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
    # the twine casts a one-pixel shadow on the envelope
    for i in range(w + 2):
        S.over(x - 1 + i, ty + 2, SHADOW, 70)
    for j in range(h + 2):
        S.over(tx + 2, y - 1 + j, SHADOW, 70)
    twine_h(S, x - 1, x + w + 1, ty)
    twine_v(S, tx, y - 1, y + h + 1)
    # the knot and its two loose ends
    S.grid(tx - 3, ty - 3, ['..kkk...', '.kmhmk..', 'kmhHdmk.', 'kmddmdk.', '.kmdmk..', '..kkk...'],
           {'k': TW[0], 'm': TW[2], 'h': TW[3], 'H': TW[4], 'd': TW[1]})
    for k in range(8):
        S.set(tx + 3 + k, ty + 3 + k // 2, TW[3] if k % 3 else TW[2]); S.set(tx + 4 + k, ty + 3 + k // 2, TW[1])
        S.set(tx - 1 - k // 2, ty + 3 + k, TW[3] if k % 3 else TW[2]); S.set(tx - 2 - k // 2, ty + 3 + k, TW[1])
    S.set(tx + 11, ty + 7, TW[4]); S.set(tx - 5, ty + 11, TW[4])
    seal = wax_seal(11, CROWN, seed=7, drips=((2.2, 3), (0.8, 2)))
    S.blit(seal, x + w // 2 - seal.w // 2, y + vy - seal.h // 2 + 1)
    return S


ASSETS['letter-bundle'] = letter_bundle
