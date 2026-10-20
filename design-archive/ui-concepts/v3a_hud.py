"""v3 Iron Ledger HUD: one material for every piece. Vellum ledger cards bound with hammered iron corner caps
and rivets: the score bar is a long ledger card between two enamel bosses (Dawn round, Dusk diamond); the three
point seals hang from it on iron chains; the group cards hang on an iron spine; the notice is a ledger slip
clasped to the top of the spine; the ability is an iron-framed slot with a brass horn and a radial cooldown."""
import math, random
from v3lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v3pal import *
from v3a_paper import parchment, PAPER, SHADOW

ASSETS = {}

RUST = ['#3a2418', '#5a3a24', '#7a4f2e']
ALARM = ['#5e140b', '#8a2a1a', '#b8402c', '#d9634c', '#e47a64', '#f2a48f']
STAMP = '#6a1a10'
PAPER['grey'] = dict(edge='#5e574b', rim2='#8a8273', rim1='#a0988a', body='#b5ad9c', shade='#ada594', light='#bdb6a6',
                     fibre='#a39b8b', crease='#a0988a', ridge='#c4bdae', back='#c4bdae')


# ================================================================ iron, the shared material
def iron_shade(S, mask, ox=0, oy=0, ramp=IRON, seed=3, rust=0.0):
    """shade any shape as a hammered iron plate: dark outline; the top and left inner edge catch the light,
    the bottom and right inner edge turn away; the face is hammered in small facets and pits (clusters, not
    noise); a little rust gathers in the lower-right edge where water would sit"""
    nz = Noise(seed, 3)
    rnd = random.Random(seed)
    for (x, y) in mask:
        out = lambda a, b: (x + a, y + b) not in mask
        if out(1, 0) or out(-1, 0) or out(0, 1) or out(0, -1):
            c = ramp[0]
        elif out(0, -2) or out(-2, 0):
            c = ramp[7] if out(0, -2) else ramp[6]
        elif out(0, 2) or out(2, 0):
            c = ramp[2]
            if rust and rnd.random() < rust:
                c = RUST[1]
        else:
            v = nz(x * 1.0, y * 1.0)
            c = ramp[3] if v > 0.74 else (ramp[5] if v < 0.2 else ramp[4])
        S.set(ox + x, oy + y, c)


def rivet(S, x, y, metal=IRON):
    """a domed 3x3 rivet head, lit on its top-left"""
    S.grid(x, y, ['hm.', 'mmd', '.dd'], {'h': metal[-1], 'm': metal[5], 'd': metal[2]})


def corner_caps(S, w, h, metal=IRON, size=15, rust=0.0, seed=5, corners=(0, 1, 2, 3), ox=0, oy=0):
    """metal corner protectors, as on a ledger's boards: a right-angled plate over each corner whose outer edges
    stand a pixel proud of the vellum, an engraved line parallel to the cut edge, and a domed rivet.
    Shaded in screen space so the light stays on the top-left whichever corner the plate sits on.
    (ox, oy) is where the card's own top-left pixel lies inside S."""
    base = {(x, y) for y in range(-1, size) for x in range(-1, size) if x + y <= size - 1}
    groove = {(x, y) for (x, y) in base if x + y == size - 5 and x >= 1 and y >= 1}
    for k in corners:
        fx, fy = k in (1, 3), k in (2, 3)
        M = lambda x, y: (ox + ((w - 1 - x) if fx else x), oy + ((h - 1 - y) if fy else y))
        m = {M(x, y) for (x, y) in base}
        iron_shade(S, m, 0, 0, ramp=metal, seed=seed + k, rust=rust)
        g = [M(x, y) for (x, y) in groove]
        for (X, Y) in g:
            S.set(X, Y, metal[2])
            # the groove's far wall catches the light on the side away from the top-left
            nx, ny = X + 1, Y + 1
            for (a, b) in ((1, 0), (0, 1)):
                q = (X + a, Y + b)
                if q in m and q not in g and S.get(*q)[:3] != rgba(metal[0])[:3]:
                    S.set(q[0], q[1], metal[6])
        rx, ry = M(3, 3)
        rivet(S, rx - (2 if fx else 0), ry - (2 if fy else 0), metal)


BRASSR = [BRASS[0], BRASS[1], BRASS[2], BRASS[3], BRASS[4], BRASS[5], BRASS[6], BRASS[7], BRASS[7]]


def ledger_card(w, h, kind='lo', metal=IRON, seed=1, rust=0.0, flash=False, caps=True):
    """a vellum ledger card bound with metal corner caps; a soft shadow below and to the right.
    flash: the loss flash, an alarm-red rule round the card (with a lighter halo outside it)."""
    pad = 2 if flash else 1
    S = Spr(w + 3 + 2 * pad, h + 4 + 2 * pad)
    paper, _ = parchment(w, h, kind=kind, seed=seed, shadow=(2, 3), deckle=False, rim=2, stains=0, fibres=0.5, mottle='edge')
    S.blit(paper, pad, pad)
    T = PAPER[kind]
    # the card's edge: an inked rule, as on a ledger page
    for x in range(pad, pad + w):
        S.set(x, pad, T['edge']); S.set(x, pad + h - 1, T['edge'])
    for y in range(pad, pad + h):
        S.set(pad, y, T['edge']); S.set(pad + w - 1, y, T['edge'])
    if flash:
        for x in range(0, w + 4):
            S.set(x, 0, ALARM[4]); S.set(x, h + 3, ALARM[4])
            S.set(x, 1, ALARM[2]); S.set(x, h + 2, ALARM[2])
        for y in range(0, h + 4):
            S.set(0, y, ALARM[4]); S.set(w + 3, y, ALARM[4])
            S.set(1, y, ALARM[2]); S.set(w + 2, y, ALARM[2])
    if caps:
        corner_caps(S, w, h, metal=metal, rust=rust, seed=seed, ox=pad, oy=pad)
    return S


ASSETS['card-plain'] = lambda: ledger_card(176, 48, seed=11)
ASSETS['card-flash'] = lambda: ledger_card(176, 48, seed=12, flash=True)
ASSETS['card-sel'] = lambda: ledger_card(176, 62, metal=BRASSR, seed=13)
ASSETS['card-rout'] = lambda: ledger_card(176, 48, kind='grey', seed=14, rust=0.35)
def score_card(w=256, h=24, band=8, inset=12):
    """the score bar: a ledger card whose two ends are clamped by riveted iron bands standing a pixel proud;
    the enamel bosses sit on the ends, outboard of the bands"""
    S = ledger_card(w, h, seed=15, caps=False)
    for bx in (inset, w - inset - band):
        m = {(x, y) for y in range(-1, h + 1) for x in range(bx, bx + band)}
        iron_shade(S, m, 1, 1, seed=16 + bx, rust=0.06)
        for ry in (1, h - 4):
            rivet(S, 1 + bx + 2, 1 + ry)
        for y in range(4, h - 4):               # an engraved line down the band, lit on its far side
            S.set(1 + bx + 5, 1 + y, IRON[2])
    return S


ASSETS['score-card'] = score_card


def notice_slip(w=176, h=40):
    """the notice: a ledger slip clasped to the top of the spine by its two upper corners"""
    S = Spr(w + 5, h + 6)
    paper, _ = parchment(w, h, kind='hi', seed=21, shadow=(2, 3), deckle=True, rim=2, stains=0, fibres=0.4, mottle='edge')
    S.blit(paper, 1, 1)
    corner_caps(S, w, h, size=12, corners=(0, 1), seed=21, ox=1, oy=1)
    return S


ASSETS['notice-slip'] = notice_slip


# ================================================================ the spine and its hooks
def spine(h=228):
    """the iron strap the ledger hangs from, riveted every 24 px"""
    S = Spr(9, h + 2)
    m = {(x, y) for y in range(h) for x in range(7)}
    iron_shade(S, m, seed=31, rust=0.08)
    for y in range(6, h - 6, 24):
        rivet(S, 2, y)
    for y in range(h):
        S.set(7, y, rgba(SHADOW, 110)); S.set(8, y, rgba(SHADOW, 50))
    for x in range(1, 8):
        S.set(x, h, rgba(SHADOW, 110))
    return S


ASSETS['spine'] = spine


def hook(metal=IRON):
    """a hook from the spine through the card's corner cap"""
    rows = ['kkkkkkk.',
            'khhhhmdk',
            'kmmmmmdk',
            'kddddkdk',
            '.....kdk',
            '.....kdk',
            '.....kkk']
    S = Spr(8, 7)
    S.grid(0, 0, rows, {'k': metal[0], 'h': metal[7], 'm': metal[5], 'd': metal[3]})
    return S


ASSETS['hook'] = hook
ASSETS['hook-brass'] = lambda: hook(BRASSR)


# ================================================================ the score bar: enamel bosses, tracks, seals
def enamel_boss(shape='round', enamel=TEAL, device='sun', size=25):
    """an iron boss holding an enamel field with a raised metal device: Dawn is round with a gold sun, Dusk
    is a diamond with a silver crescent (shape and device carry the team, not colour alone)"""
    S = Spr(size + 2, size + 3)
    c = (size - 1) / 2
    if shape == 'round':
        outer = {(x, y) for y in range(size) for x in range(size) if (x - c) ** 2 + (y - c) ** 2 <= (c + 0.4) ** 2}
        inner = {(x, y) for (x, y) in outer if (x - c) ** 2 + (y - c) ** 2 <= (c - 3.6) ** 2}
    else:
        outer = {(x, y) for y in range(size) for x in range(size) if abs(x - c) + abs(y - c) <= c + 0.5}
        inner = {(x, y) for (x, y) in outer if abs(x - c) + abs(y - c) <= c - 4.5}
    for (x, y) in outer:
        for (a, b, al) in ((1, 2, 0.5), (2, 3, 0.22)):
            k = (x + a, y + b)
            if k not in outer and S.get(*k)[3] < al * 255:
                S.set(k[0], k[1], rgba(SHADOW, round(al * 255)))
    iron_shade(S, outer, seed=41)
    # the enamel: a cool field, darker at the top-left under the iron lip, a glint on the lower right of the glaze
    for (x, y) in inner:
        d = (x - c) * 0.7 + (y - c) * 0.7
        S.set(x, y, enamel[2] if d < -2.5 else (enamel[3] if d < 3.5 else enamel[4]))
    for (x, y) in inner:
        if any((x + a, y + b) not in inner for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            S.set(x, y, enamel[1] if (x - c) + (y - c) < 0 else enamel[2])
    if device == 'sun':
        dev = ['....m....',
               '.m..m..m.',
               '..mHHHm..',
               '..HhhhHd.',
               'mmHhhhHdd',
               '..HhhhHd.',
               '..mdddm..',
               '.m..d..m.',
               '....d....']
        pal = {'H': BRASS[6], 'h': BRASS[7], 'm': BRASS[5], 'd': BRASS[3]}
    else:
        dev = ['...hhh...',
               '..hHk....',
               '.hHk.....',
               '.hH......',
               '.hm......',
               '.hm......',
               '..mm.....',
               '...mmd...',
               '....dd...']
        pal = {'h': IRON[8], 'H': IRON[7], 'm': IRON[6], 'd': IRON[5], 'k': IRON[3]}
    ox, oy = round(c - 4), round(c - 4)
    m = grid_mask(dev)
    for (x, y) in m:
        if (x + 1, y + 1) not in m:
            S.set(ox + x + 1, oy + y + 1, enamel[0])
    S.grid(ox, oy, dev, pal)
    return S


ASSETS['boss-dawn'] = lambda: enamel_boss('round', TEAL, 'sun')
ASSETS['boss-dusk'] = lambda: enamel_boss('diamond', VIOL, 'moon')


def track(w=64, fill=0, team='dawn'):
    """a score track inked into the vellum: a recessed groove with tick marks at 100 and 200;
    the team's fill runs in from the outer end (Dawn from the left, Dusk from the right)"""
    S = Spr(w, 8)
    ramp = TEAL if team == 'dawn' else VIOL
    for x in range(w):
        S.set(x, 1, INK[2]); S.set(x, 6, INK[2])
        for y in (2, 3, 4, 5):
            S.set(x, y, VEL[3] if y == 2 else VEL[4])
    for y in range(1, 7):
        S.set(0, y, INK[2]); S.set(w - 1, y, INK[2])
    n = round((w - 2) * fill)
    xs = range(1, 1 + n) if team == 'dawn' else range(w - 1 - n, w - 1)
    for x in xs:
        S.set(x, 2, ramp[4]); S.set(x, 3, ramp[3]); S.set(x, 4, ramp[3]); S.set(x, 5, ramp[2])
    if n:
        edge = (n) if team == 'dawn' else (w - 1 - n)
        for y in (2, 3, 4, 5):
            S.set(edge, y, ramp[1])
    for t in (1 / 3, 2 / 3):
        x = round((w - 1) * t) if team == 'dawn' else round((w - 1) * (1 - t))
        S.set(x, 0, INK[3]); S.set(x, 7, INK[3])
    return S


ASSETS['track-dawn'] = lambda: track(56, 148 / 300, 'dawn')
ASSETS['track-dusk'] = lambda: track(56, 121 / 300, 'dusk')


def chain(links=2):
    """iron chain: links alternate face-on and edge-on"""
    rows = []
    for i in range(links):
        rows += ['.kk.', 'k.hk', 'k.mk', '.kk.'] if i % 2 == 0 else ['.kk.', '.hk.', '.mk.', '.kk.']
    S = Spr(4, len(rows))
    S.grid(0, 0, rows, {'k': IRON[1], 'h': IRON[7], 'm': IRON[5]})
    return S


ASSETS['chain'] = lambda: chain(2)


def seal(kind):
    """a point seal hung from the bar. Dawn holds A: round, teal. Dusk holds C: a diamond, violet.
    B is contested: a square split teal and violet with two swords crossed behind it, their points and
    hilts standing out at the corners (the X reads as a fight without the colours)"""
    size = 19
    S = Spr(27, 27)
    o = 4
    c = (size - 1) / 2
    if kind == 'A':
        outer = {(x, y) for y in range(size) for x in range(size) if (x - c) ** 2 + (y - c) ** 2 <= (c + 0.3) ** 2}
        inner = {(x, y) for (x, y) in outer if (x - c) ** 2 + (y - c) ** 2 <= (c - 2.6) ** 2}
    elif kind == 'C':
        outer = {(x, y) for y in range(size) for x in range(size) if abs(x - c) + abs(y - c) <= c + 0.5}
        inner = {(x, y) for (x, y) in outer if abs(x - c) + abs(y - c) <= c - 3.2}
    else:
        outer = {(x, y) for y in range(1, size - 1) for x in range(1, size - 1)}
        inner = {(x, y) for y in range(4, size - 4) for x in range(4, size - 4)}
        # the crossed swords behind the square: blades from bottom corners to top corners
        sw = Spr(27, 27)
        for (sx, d) in ((0, 1), (26, -1)):
            for t in range(27):
                X, Y = sx + d * t, 26 - t
                if t < 6:      # hilt and guard at the bottom
                    continue
                sw.set(X, Y, IRON[8] if d == 1 else IRON[7]); sw.set(X + d, Y, IRON[6] if d == 1 else IRON[5])
            for t in (6, 7):
                X, Y = sx + d * t, 26 - t
            gx, gy = sx + d * 6, 20
            for u in range(-3, 4):
                sw.set(gx + d * u, gy + u, BRASS[5])           # cross-guard
            for t in range(1, 6):
                sw.set(sx + d * t, 26 - t, OAK[5] if t % 2 else OAK[7])   # wrapped grip
            sw.set(sx, 26, BRASS[4])                            # pommel
        sw.outline(IRON[0])
        S.blit(sw, 0, 0)
    for (x, y) in outer:
        for (a, b, al) in ((1, 2, 0.5),):
            k = (x + a + o, y + b + o)
            if (x + a, y + b) not in outer and S.get(*k)[3] < al * 255:
                S.set(k[0], k[1], rgba(SHADOW, round(al * 255)))
    iron_shade(S, outer, o, o, seed=51 + ord(kind))
    for (x, y) in inner:
        if kind == 'A':
            ramp = TEAL
        elif kind == 'C':
            ramp = VIOL
        else:
            ramp = TEAL if (x - 4) + (y - 4) < (size - 9) else VIOL
        d = (x - c) * 0.7 + (y - c) * 0.7
        rim = any((x + a, y + b) not in inner for a, b in ((-1, 0), (0, -1)))
        if kind == 'A':
            body, lit = TEAL[2], TEAL[3]
        elif kind == 'C':
            body, lit = VIOL[2], VIOL[3]
        else:
            body, lit = (TEAL[2], TEAL[3]) if ramp is TEAL else (VIOL[2], VIOL[3])
        S.set(o + x, o + y, ramp[1] if rim else (lit if d > 3.5 else body))
    if kind == 'B':
        for (x, y) in inner:
            if (x - 4) + (y - 4) == size - 9:
                S.set(o + x, o + y, IRON[1])                     # the split
    gx, gy = min(inner, key=lambda p: (p[0] + p[1], p[1]))
    for (a, b) in ((2, 2), (3, 2), (2, 3)):
        if (gx + a, gy + b) in inner:
            S.set(o + gx + a, o + gy + b, (TEAL if kind != 'C' else VIOL)[4])
    return S


for _k in 'ABC':
    ASSETS['seal-' + _k] = (lambda k: (lambda: seal(k)))(_k)


# ================================================================ glyphs (ink on vellum, 9 x 9)
GLYPHS = {
    'g-attack':     ['......kkk', '.....kLLk', '....kLLk.', '.k.kLLk..', '.kkLLk...', '..kkk....', '.kkkkk...', 'kkk.kk...', 'kk.......'],
    'g-hold':       ['kkkkkkkkk', 'kLLLkLLLk', 'kLLLkLLLk', 'kkkkkkkkk', 'kLLLkLLLk', '.kLLkLLk.', '..kLkLk..', '...kkk...', '....k....'],
    'g-follow':     ['k...k....', 'kk..kk...', '.kk..kk..', '..kk..kk.', '...kk..kk', '..kk..kk.', '.kk..kk..', 'kk..kk...', 'k...k....'],
    'g-shieldwall': ['.k..k..k.', '.k..k..k.', 'kkkkkkkkk', 'kLkkLkkLk', 'kLkkLkkLk', 'kLkkLkkLk', '.k..k..k.', '.........', '.........'],
    'g-line':       ['.........', '.........', '.k.k.k.k.', 'kkkkkkkkk', '.k.k.k.k.', '.k.k.k.k.', '.........', '.........', '.........'],
    'g-compact':    ['kk.kk.kk.', 'kk.kk.kk.', '.........', 'kk.kk.kk.', 'kk.kk.kk.', '.........', 'kk.kk.kk.', 'kk.kk.kk.', '.........'],
    'g-fight':      ['k.......k', 'kk.....kk', '.kk...kk.', '..kk.kk..', '...kkk...', '..kk.kk..', 'kkk...kkk', '.k.....k.', 'k.......k'],
    'g-cycle':      ['..kkkk...', '.k....k..', 'k......k.', 'k.....kkk', 'k......k.', 'k........', '.k.......', '..kkk....', '.........'],
}


def glyph(rows, ink=INK[1], light=VEL[7]):
    S = Spr(9, 9)
    S.grid(0, 0, rows, {'k': ink, 'L': light})
    return S


for _g, _rows in GLYPHS.items():
    ASSETS[_g] = (lambda r: (lambda: glyph(r)))(_rows)
    ASSETS[_g + '-faded'] = (lambda r: (lambda: glyph(r, INK[3], PAPER['grey']['light'])))(_rows)
ASSETS['g-fight-red'] = lambda: glyph(GLYPHS['g-fight'], ALARM[1])


def key_ink(w=9, h=11):
    """a key cap ruled in ink on the vellum, its lower-right lip catching the light"""
    S = Spr(w + 1, h + 1)
    S.frame(0, 0, w, h, INK[2])
    for x in range(1, w + 1):
        S.set(x, h, VEL[8])
    for y in range(1, h + 1):
        S.set(w, y, VEL[8])
    return S


ASSETS['key-ink'] = key_ink


def key_chip(w=9, h=11):
    """a key cap printed in solid ink, the letter left in the vellum; corners clipped, a lighter top edge"""
    S = Spr(w + 1, h + 1)
    for y in range(h):
        for x in range(w):
            S.set(x, y, INK[1])
    for x in range(1, w - 1):
        S.set(x, 0, INK[3]); S.set(x, h - 1, INK[0])
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        S.set(x, y, (0, 0, 0, 0))
    for x in range(1, w + 1):
        S.set(x, h, rgba(SHADOW, 60))
    for y in range(1, h):
        S.set(w, y, rgba(SHADOW, 45))
    return S


ASSETS['key-chip'] = key_chip


def sun_ink():
    """the Dawn sun, inked in the team's teal (the notice's mark)"""
    rows = ['....k....', '.k..k..k.', '..kkkkk..', '..kLLLk..', 'kkkLLLkkk', '..kLLLk..', '..kkkkk..', '.k..k..k.', '....k....']
    S = Spr(9, 9)
    S.grid(0, 0, rows, {'k': TEAL[1], 'L': TEAL[4]})
    return S


ASSETS['sun-ink'] = sun_ink


# ================================================================ health bars
def hp_channel(w=104):
    S = Spr(w, 5)
    for x in range(w):
        S.set(x, 0, INK[2]); S.set(x, 4, INK[2])
        S.set(x, 1, VEL[3]); S.set(x, 2, VEL[4]); S.set(x, 3, VEL[4])
    for y in range(5):
        S.set(0, y, INK[2]); S.set(w - 1, y, INK[2])
    return S


ASSETS['hp-channel'] = hp_channel
ASSETS['hp-channel-grey'] = lambda: (lambda s: s)(hp_channel())


def hp_fill(n, kind='dawn'):
    S = Spr(n, 3)
    if kind == 'dawn':
        cols = (TEAL[4], TEAL[3], TEAL[2])
    elif kind == 'lost':
        cols = (ALARM[4], ALARM[3], ALARM[2])
    else:
        cols = (VEL[2], VEL[1], VEL[1])
    for x in range(n):
        for y in range(3):
            S.set(x, y, cols[y])
    if kind != 'lost':
        for y in range(3):
            S.set(n - 1, y, TEAL[1] if kind == 'dawn' else VEL[0])
    return S


ASSETS['hp-inf'] = lambda: hp_fill(68)
ASSETS['hp-inf-lost'] = lambda: hp_fill(9, 'lost')
ASSETS['hp-ran'] = lambda: hp_fill(102)
ASSETS['hp-sup'] = lambda: hp_fill(30, 'rout')


def stamp(w=54, h=16, seed=4):
    """the ROUTING stamp: a double-ruled box in red ink, worn where the stamp did not bite"""
    rnd = random.Random(seed)
    S = Spr(w, h)
    for (i, ok) in ((0, 0.92), (2, 0.82)):
        for x in range(i, w - i):
            for y in (i, h - 1 - i):
                if rnd.random() < ok:
                    S.set(x, y, STAMP)
        for y in range(i, h - i):
            for x in (i, w - 1 - i):
                if rnd.random() < ok:
                    S.set(x, y, STAMP)
    return S


ASSETS['stamp-rout'] = stamp


# ================================================================ the ability slot
HORNR = ['#1c0f0a', '#2e1a10', '#472918', '#633b22', '#85552f', '#a8764a']


def horn(w=22, h=20):
    """the Rally Horn: an ox horn curving up from the mouthpiece (bottom left) to the bell (top right), banded
    in brass; lit from the top left, the bell's mouth dark inside a bright brass rim; a leather strap hangs below"""
    S = Spr(w, h)
    P0, P1, P2 = (2.6, 16.6), (5.0, 7.5), (15.0, 5.8)
    pts = []
    for i in range(121):
        t = i / 120
        x = (1 - t) ** 2 * P0[0] + 2 * (1 - t) * t * P1[0] + t * t * P2[0]
        y = (1 - t) ** 2 * P0[1] + 2 * (1 - t) * t * P1[1] + t * t * P2[1]
        pts.append((t, x, y, 1.3 + 3.9 * t ** 1.5))
    body = {}
    for y in range(h):
        for x in range(w):
            best = None
            for (t, cx, cy, r) in pts:
                d = math.hypot(x - cx, y - cy)
                if d <= r and (best is None or d / r < best[0]):
                    best = (d / r, t, (x - cx) / max(d, 1e-6), (y - cy) / max(d, 1e-6))
            if best:
                body[(x, y)] = best
    # shade: light from the top left on the curved surface, one streak of lighter horn along the top
    for (x, y), (f, t, nx, ny) in body.items():
        lit = -(nx * 0.7 + ny * 0.7) * f
        if t < 0.07:
            ramp = BRASSR
            c = ramp[6] if lit > 0.3 else (ramp[4] if lit > -0.3 else ramp[2])
        else:
            c = HORNR[5] if lit > 0.55 else (HORNR[4] if lit > 0.15 else (HORNR[3] if lit > -0.35 else HORNR[2]))
            if abs(t - 0.36) < 0.045 or abs(t - 0.66) < 0.04:
                c = BRASS[6] if lit > 0.2 else (BRASS[4] if lit > -0.35 else BRASS[2])
        S.set(x, y, c)
    # the bell: an oval mouth across the end of the horn, seen at an angle
    ex, ey = 17.2, 4.8
    ux, uy = 0.86, -0.5          # the horn's direction at the bell
    vx, vy = 0.5, 0.86           # across it
    for y in range(h):
        for x in range(w):
            a = (x - ex) * ux + (y - ey) * uy
            b = (x - ex) * vx + (y - ey) * vy
            q = (a / 2.3) ** 2 + (b / 5.3) ** 2
            if q <= 1.0:
                S.set(x, y, BRASS[6] if (b < 0 and q > 0.45) else (BRASS[4] if q > 0.45 else '#1a0e08'))
            if 0.45 >= q > 0.0 and b > 1.8 and a < 0:
                S.set(x, y, '#3a2414')
    S.outline(HORNR[0])
    # the strap: a leather loop from the first band to the second, hanging below the horn
    for (x, y) in ((6, 15), (7, 16), (8, 17), (9, 17), (10, 17), (11, 16), (12, 15), (13, 14)):
        if S.get(x, y)[3] == 0:
            S.set(x, y, OAK[6] if y < 17 else OAK[5])
    return S


ASSETS['horn'] = horn


def ability_slot(size=32, tab_w=22, tab_h=16):
    """an iron frame 4 px deep round a vellum window (the ability's card), riveted at three corners; an iron
    tab riveted under the right side carries the seconds. The window is light so the dark sweep reads."""
    W = size + tab_w - 4
    S = Spr(W + 2, size + 3)
    ty = (size - tab_h) // 2
    outer = {(x, y) for y in range(size) for x in range(size)}
    tab = {(x, y) for y in range(ty, ty + tab_h) for x in range(size - 4, W)}
    allm = outer | tab
    for (x, y) in allm:
        for (a, b, al) in ((1, 2, 0.5), (2, 3, 0.22)):
            k = (x + a, y + b)
            if k not in allm and S.get(*k)[3] < al * 255:
                S.set(k[0], k[1], rgba(SHADOW, round(al * 255)))
    iron_shade(S, tab, seed=62, rust=0.1)
    iron_shade(S, outer, seed=61)
    nz = Noise(63, 4)
    for y in range(4, size - 4):
        for x in range(4, size - 4):
            v = nz(x * 1.0, y * 1.0)
            c = VEL[5] if v < 0.62 else VEL[4]
            if x <= 5 or y <= 5:
                c = VEL[3] if (x == 4 or y == 4) else VEL[4]
            S.set(x, y, c)
    for (rx, ry) in ((size - 4, size - 4), (size - 4, 0), (0, size - 4)):
        rivet(S, rx + 1, ry + 1)
    return S


ASSETS['ability-slot'] = ability_slot


def cooldown(size=24, remain=48 / 90):
    """the cooldown sweep over the window: a dark veil over the part of the turn still to come (clockwise from
    twelve), its leading edge a bright line, so the time left reads as an angle before the number is read"""
    S = Spr(size, size)
    c = (size - 1) / 2
    start = 2 * math.pi * (1 - remain)
    ex, ey = math.sin(start), -math.cos(start)
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            ang = math.atan2(dx, -dy) % (2 * math.pi)
            if ang >= start:
                S.set(x, y, rgba(IRON[1], 205))
            # the leading edge: pixels on the ray at `start`
            along = dx * ex + dy * ey
            across = abs(dx * ey - dy * ex)
            if along > 0.5 and across < 0.62:
                S.set(x, y, rgba(BRASS[6], 230))
    for y in range(size):          # the start line at twelve o'clock
        if y <= c:
            S.set(round(c), y, rgba(BRASS[4], 200)) if S.get(round(c), y)[3] else None
    return S


ASSETS['cooldown'] = cooldown


def key_brass_plate(w=12, h=14):
    """the ability's key, a small brass plate riveted over the frame's top-left corner"""
    S = Spr(w + 1, h + 1)
    m = {(x, y) for y in range(h) for x in range(w)}
    for x in range(1, w + 1):
        S.set(x, h, rgba(SHADOW, 140))
    for y in range(1, h + 1):
        S.set(w, y, rgba(SHADOW, 120))
    iron_shade(S, m, ramp=BRASSR, seed=71)
    for (x, y) in m:
        if 2 <= x < w - 2 and 2 <= y < h - 2:
            S.set(x, y, BRASS[5])
    return S


ASSETS['key-brass-plate'] = key_brass_plate
