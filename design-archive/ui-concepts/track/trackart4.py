"""The Chapter plaque, round 2: this page's own stretch of the table (its planks, knots and nails from the
kit's oak with another seed, and no brass fittings: Kort allows only plank layout, knots and nail heads on
table wood), the plain wax pin that marks the current
tier on the rod, and the Oath's House rewards (the player's portrait frame, tabard, cloak, pennant patterns,
banner cloth, double shards)."""
import math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
sys.path.insert(0, HERE)
from v4lib import Spr, Noise, dith, rgba, mix
from v4pal import *
from v4a_furn import SPEC
from v4a_recruit import HOUSES, DEVICES
from trackart import sepia, soft_shadow, pad
from trackart2 import outline, K, SHARD_GILDED, SHARD_PLAIN, shard_cluster, mini_bust, _blob

ASSETS = {}
GOLD = HOUSES['gilded'][0] + ['#f6dfa0']      # the Gilded Order's gold, dark to light, and its glint


# ================================================================ the table for this page
TABLE_SEED = 53     # home and recruit use 11; this stretch of the table has its own planks, knots and nails


def table_r2():
    """the kit's oak with this page's seed and nothing let into it: no brass fittings on table wood (Kort)"""
    from v4art import oak_table
    return oak_table(seed=TABLE_SEED, avoid=[(476, 72, 904, 200), (52, 292, 124, 468), (132, 494, 560, 520)])


ASSETS['oak-table-track'] = table_r2


# ================================================================ the plain pin on the rod
def pin_plain(r=6, seed=7):
    """a drop of red wax pressed onto the rod over the current tier: domed, lit on the top left with a
    glint, its point run down toward the plaques below; no number (the chapter's seal carries it)"""
    shape, c, R, size = _blob(r, seed, ((1.5708, 3, 2),))
    S = Spr(size, size)
    L = (-0.55, -0.62, 0.56)
    for (x, y) in shape:
        dx, dy = (x - c) / (r + 0.5), (y - c) / (r + 0.5)
        d2 = dx * dx + dy * dy
        z = math.sqrt(max(0.0, 1.0 - min(1.0, d2)))
        n = (dx, dy, z)
        v = n[0] * L[0] + n[1] * L[1] + n[2] * L[2]
        col = WAX[6] if v > 0.86 else WAX[5] if v > 0.62 else WAX[4] if v > 0.3 else WAX[3] if v > 0.0 else WAX[2]
        if d2 > 1.0:                                    # the run below the dome
            col = WAX[3] if (x - c) < 0 else WAX[2]
        S.set(x, y, col)
    S.set(round(c - r * 0.45), round(c - r * 0.5), SPEC)
    S.set(round(c - r * 0.45) + 1, round(c - r * 0.5), WAX[6])
    out = {(x + a, y + b) for (x, y) in shape for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))} - shape
    for p in out:
        S.set(*p, WAX[0])
    return soft_shadow(pad(S, 0, 0, 2, 3), [(1, 1, 0.5), (1, 2, 0.4), (2, 3, 0.18)])


ASSETS['pin-tier'] = pin_plain


# ================================================================ the Oath's rewards: the House's, worn by the player
SWORD = ['.x.x.x.',          # the Gilded Order's crowned sword, cut down for an icon
         '.xxxxx.',
         '...x...',
         'xxxxxxx',
         '...x...',
         '...x...',
         '...x...',
         '...x...',
         '...x...',
         '...x...']


def device(S, ox, oy, lit, mid, dark):
    """the crowned sword stitched in: each stitch lit on its upper left, the shadow it throws on the cloth"""
    m = {(ox + i, oy + j) for j, row in enumerate(SWORD) for i, ch in enumerate(row) if ch == 'x'}
    for (x, y) in m:
        if (x + 1, y + 1) not in m:
            S.set(x + 1, y + 1, dark)
    for (x, y) in m:
        S.set(x, y, lit if ((x - 1, y) not in m or (x, y - 1) not in m) else mid)


def house_frame():
    """the House's portrait frame for the player's own portrait: gilt, beaded, a rosette at each corner and
    the Order's crown cast on top, the player's face in the window (not a troop's: the mastery frames are
    brass and per troop)"""
    W, H, N, top = 32, 38, 30, 7
    S = Spr(W, H)
    ox, oy = 1, top
    rings = {0: (GOLD[0], GOLD[0]), 1: (GOLD[5], GOLD[2]), 2: (GOLD[4], GOLD[3]), 3: (GOLD[1], GOLD[4])}
    for y in range(N):
        for x in range(N):
            d = min(x, y, N - 1 - x, N - 1 - y)
            if d in rings:
                tl = (y == d and x < N - 1 - d) or (x == d and y < N - 1 - d)
                S.set(ox + x, oy + y, rings[d][0 if tl else 1])
    for k in range(5, N - 5, 3):                        # the beading on the frame's face
        S.set(ox + k, oy + 2, GOLD[6]); S.set(ox + k, oy + N - 3, GOLD[5])
        S.set(ox + 2, oy + k, GOLD[6]); S.set(ox + N - 3, oy + k, GOLD[5])
    tones = ['#141a1d', '#182024', '#1d262a', '#222d31', '#283439']
    for y in range(4, N - 4):
        for x in range(4, N - 4):
            dd = math.hypot((x - 14.5) / 12, (y - 10) / 13)
            S.set(ox + x, oy + y, tones[max(0, min(4, int((1.1 - dd) * 4.0)))])
    S.blit(mini_bust('kort', 22), ox + 4, oy + 4)
    ros = ['.kk.', 'khmk', 'kmdk', '.kk.']
    for (x, y) in ((0, 0), (N - 4, 0), (0, N - 4), (N - 4, N - 4)):
        S.grid(ox + x, oy + y, ros, {'k': GOLD[0], 'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    crown = ['.x...x...x.',
             '.xx..x..xx.',
             '.xxxxxxxxx.',
             'xxxxxxxxxxx',
             '..xxxxxxx..',
             '...xxxxx...',
             '....xxx....']
    cx0 = ox + N // 2 - 5
    m = {(cx0 + i, j + 1) for j, row in enumerate(crown) for i, ch in enumerate(row) if ch == 'x'}
    for (x, y) in m:
        up, lf = (x, y - 1) not in m, (x - 1, y) not in m
        S.set(x, y, GOLD[6] if (up and lf) else GOLD[5] if up else GOLD[2] if (x + 1, y) not in m else GOLD[4])
    S.set(cx0 + 5, 1, SPEC)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.5)])


def tabard():
    """the commander's tabard in the Order's colours: ivory wool with a gilt border, short flaps over the
    shoulders, the crowned sword on the chest, a belt at the waist; lit from the top left, folds falling"""
    W, H = 30, 34
    S = Spr(W, H)
    inside = set()
    for y in range(1, 33):
        for x in range(W):
            if y < 9:                                   # the shoulder flaps, rounded at their ends
                ok = 2 <= x <= 27 and not (y == 1 and (x < 4 or x > 25))
                if y >= 7 and (x < 5 or x > 24):
                    ok = False
            else:
                f = (y - 9) / 23.0
                ok = round(6 - 1.5 * f) <= x <= round(23 + 1.5 * f)
            if ok:
                inside.add((x, y))
    for (x, y) in inside:
        t = x / (W - 1)
        c = VEL[7] if t < 0.3 else VEL[6] if t < 0.64 else VEL[5]
        if y > 21 and x in (10, 19):                    # the skirt's folds under the belt
            c = VEL[5] if x == 10 else VEL[4]
        S.set(x, y, c)
    for (x, y) in inside:                               # the gilt border round every edge
        nb = [(x + a, y + b) not in inside for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        if any(nb):
            S.set(x, y, GOLD[5] if (nb[1] or nb[3]) else GOLD[3])
    for x in range(12, 18):                             # the neck, open to the dark inside
        S.set(x, 1, '#2a1d12')
        if 13 <= x <= 16:
            S.set(x, 2, '#2a1d12')
    device(S, 11, 8, GOLD[4], GOLD[3], VEL[3])
    for x in range(6, 24):                              # the belt and its buckle
        if (x, 20) in inside:
            S.set(x, 20, INK[3] if x < 15 else INK[2]); S.set(x, 21, INK[1])
    S.grid(13, 19, ['hmmd', 'm..d', 'hmmd'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def cloak():
    """the commander's cloak in the Order's gold, hanging empty: the hood up and dark inside, a gilt clasp at
    the throat, the wool falling from the shoulders in folds that open toward a wavy hem, the fronts parted
    on the dark inside with an ivory facing on each edge; lit from the top left"""
    W, H = 34, 36
    S = Spr(W, H)
    cx = 16.5

    def hw(y):
        if y < 11:
            return 2.6 + 4.9 * min(1.0, (y - 1) / 5.0) ** 0.6
        sh = min(1.0, (y - 10) / 4.0)
        return 7.5 + 4.3 * (3 * sh * sh - 2 * sh * sh * sh) + 3.6 * max(0, y - 14) / 19.0

    def hem(x):
        return 33 - (1 if math.sin((x - cx) * 0.78 + 0.4) > 0.35 else 0)

    body = {(x, y) for y in range(1, 34) for x in range(W) if abs(x + 0.5 - cx - 0.5) <= hw(y) and y <= hem(x)}
    WOOL = [GOLD[1], GOLD[2], GOLD[3], GOLD[4], GOLD[5]]
    for (x, y) in body:
        u = x - cx
        a = u / max(1.0, hw(y))
        fold = math.sin(a * 5.2 + 0.5) if y > 11 else 0.0
        v = 2.1 + fold * 0.9 * min(1.0, (y - 10) / 8.0 if y > 10 else 0) - a * 0.8
        S.set(x, y, WOOL[max(0, min(4, int(round(v))))])
    void = set()
    for (x, y) in body:                                 # the hood's dark inside
        if ((x - cx) / 4.3) ** 2 + ((y - 8.3) / 4.6) ** 2 <= 1.0 and y <= 11:
            void.add((x, y))
    for (x, y) in void:
        S.set(x, y, '#1e140c' if y > 5 else '#2a1d12')
    for (x, y) in void:                                 # the hood's gilt rim, brighter on the lit side
        for (a2, b2) in ((-1, 0), (1, 0), (0, -1)):
            q = (x + a2, y + b2)
            if q in body and q not in void:
                S.set(*q, GOLD[5] if q[0] < cx else GOLD[3])
    for (x, y) in body:                                 # the fronts parting below the clasp
        if y < 14:
            continue
        g = 0.5 + (y - 14) * 0.24
        u = x + 0.5 - cx - 0.5
        if abs(u) < g - 1.0:
            S.set(x, y, '#1e140c' if y < 30 else '#2a1d12')
        elif abs(u) < g:
            S.set(x, y, VEL[6] if u < 0 else VEL[4])    # the ivory facing on each front
    for x in range(W):                                  # the gilt hem along the wave
        y = hem(x)
        if (x, y) in body and S.get(x, y) not in (rgba('#1e140c'), rgba('#2a1d12')):
            S.set(x, y, GOLD[5] if x < 18 else GOLD[4])
    S.grid(15, 12, ['.hm.', 'hmmd', 'mmdd', '.dd.'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    S.set(16, 12, SPEC)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def pennant(kind='pale'):
    """a pennant pattern of the House on a short staff: gold cloth cut in a swallowtail, the pattern in
    ivory (pale: a stripe down the middle; bend: a band from the hoist's top to the fly's foot; cross: a
    stripe down and a band across), the cloth waving, lit along its upper edge; an ash staff with a gilt
    finial"""
    W, H = 35, 30
    S = Spr(W, H)
    for y in range(2, H):                               # the staff
        S.set(3, y, BRASS[3] if y > 3 else GOLD[5]); S.set(4, y, VEL[2]); S.set(5, y, VEL[1])
    S.grid(2, 0, ['.h.', 'hmd', '.d.'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    cloth = {}
    for x in range(6, 34):
        t = (x - 6) / 27.0
        wave = 1.6 * math.sin(t * 5.2)
        y0 = 4 + t * 4 + wave
        y1 = 17 - t * 4 + wave
        for y in range(H):
            if y0 <= y <= y1:
                if x > 27:                              # the swallowtail
                    mid = (y0 + y1) / 2
                    if abs(y - mid) < (x - 27) * 0.75:
                        continue
                cloth[(x, y)] = (t, (y - y0) / max(1.0, y1 - y0), math.cos(t * 5.2))
    for (x, y), (t, v, slope) in cloth.items():
        if kind == 'pale':
            ivory = 13 <= x <= 17
        elif kind == 'cross':
            ivory = 13 <= x <= 15 or 0.38 <= v <= 0.6
        else:
            ivory = abs((v * 13) - (t * 13) + 0.0) < 2.2
        shade = 1 if slope > 0.3 else (0 if slope > -0.4 else -1)
        if ivory:
            c = [VEL[4], VEL[6], VEL[7]][shade + 1]
        else:
            c = [GOLD[2], GOLD[3], GOLD[4]][shade + 1]
        if v < 0.12:
            c = VEL[8] if ivory else GOLD[5]
        S.set(x, y, c)
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def banner_cloth(kind='ivory'):
    """a banner cloth of the House hung from a gilt crossbar, cut at the foot into three points, the folds
    falling straight. ivory: Order white, the cloth ivory with the crowned sword and border in gold (tier
    15); gold: the cloth of gold with the sword and border in ivory (tier 21, drawn large for the hero)"""
    if kind == 'ivory':
        C, B, D = [VEL[5], VEL[6], VEL[7], VEL[8]], (GOLD[5], GOLD[3]), (GOLD[4], GOLD[3], GOLD[1])
    else:
        C, B, D = [GOLD[2], GOLD[3], GOLD[4], GOLD[5]], (VEL[6], VEL[4]), (VEL[8], VEL[6], GOLD[1])
    W, H = 30, 37
    S = Spr(W, H)
    cloth = set()
    for y in range(5, 36):
        for x in range(4, 26):
            if y < 28:
                cloth.add((x, y))
            else:
                k = (x - 4) / 21.0 * 3.0
                tooth = abs((k % 1.0) - 0.5) * 2.0      # 1 at the notch, 0 at the point
                if (y - 28) <= (1 - tooth) * 7.5:
                    cloth.add((x, y))
    for (x, y) in cloth:
        u = (x - 4) / 21.0
        fold = math.sin(u * 3.14159 * 3)
        c = C[2] if fold > 0.45 else C[1] if fold > -0.35 else C[0]
        if u < 0.12:
            c = C[3] if fold > -0.35 else C[2]
        S.set(x, y, c)
    for (x, y) in cloth:
        edge = [(x + a, y + b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        inner = [(x + 2 * a, y + 2 * b) not in cloth for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        if not any(edge) and any(inner):
            S.set(x, y, B[0] if (x < 15) else B[1])
    device(S, 12, 10, *D)
    for x in range(1, 29):                              # the crossbar, its knobs, the rings
        S.set(x, 3, GOLD[5]); S.set(x, 4, GOLD[3])
    for xk in (0, 28):
        S.grid(xk, 2, ['hm', 'md', 'dd'], {'h': GOLD[6], 'm': GOLD[4], 'd': GOLD[2]})
    for xr in (6, 14, 22):
        S.set(xr, 5, GOLD[1])
    outline(S)
    return soft_shadow(pad(S, 0, 0, 1, 1), [(1, 1, 0.45)])


def shards_double(ramp):
    """the Oath doubles the shards: two clusters, the second behind and to the right"""
    a = shard_cluster(ramp)
    S = Spr(a.w + 8, a.h + 2)
    S.blit(a, 8, 0)
    S.blit(a, 0, 2)
    return S


ASSETS['ic-o-frame'] = house_frame
ASSETS['ic-o-tabard'] = tabard
ASSETS['ic-o-cloak'] = cloak
ASSETS['ic-o-pennant-pale'] = lambda: pennant('pale')
ASSETS['ic-o-pennant-bend'] = lambda: pennant('bend')
ASSETS['ic-o-cloth'] = lambda: banner_cloth('ivory')
ASSETS['ic-o-pennant-cross'] = lambda: pennant('cross')
ASSETS['ic-o-shards'] = lambda: shards_double(SHARD_GILDED)
for _n, _f in (('frame', house_frame), ('tabard', tabard), ('cloak', cloak), ('cloth', lambda: banner_cloth('ivory'))):
    ASSETS['ic-o-%s-sepia' % _n] = (lambda f: (lambda: sepia(f(), 0.08, 0.92)))(_f)
ASSETS['ic-o-pennant-pale-sepia'] = lambda: sepia(pennant('pale'), 0.08, 0.92)
ASSETS['ic-o-pennant-bend-sepia'] = lambda: sepia(pennant('bend'), 0.08, 0.92)
ASSETS['ic-o-pennant-cross-sepia'] = lambda: sepia(pennant('cross'), 0.08, 0.92)
ASSETS['ic-o-shards-sepia'] = lambda: sepia(shards_double(SHARD_GILDED), 0.08, 0.92)
