"""The storybook's small furniture for scenes: the vellum caption strip a book panel's caption is typed on, vellum
tags for the controls, the use-key mouse glyph, the closed clasp of a first reading, the panel pips.

python sb_ui.py [name ...]   writes art/<name>.png"""
import math, random, sys
from sb_common import *
from v4a_paper import parchment
from v4a_furn import vellum_button

ASSETS = {}


# ---------------------------------------------------------------- the caption strip
def caption_strip(w, h, seed=3):
    """a strip of the book's own vellum laid over the foot of a panel: deckled long edges, the short ends cut
    straight (it was cut from a leaf), aged at the rim, fibres; two ruled lines in faint sepia above and below
    the text block, as a scribe rules a leaf, broken where the ruling pen lifted"""
    S, paper = parchment(w, h, kind='hi', seed=seed, shadow=(2, 4), stains=0, foxing=0.6, fibres=0.8, rim=3,
                         mottle='edge')
    rnd = random.Random(seed)
    for y in (9, h - 10):
        x = 14
        while x < w - 14:
            run = rnd.randint(30, 90)
            for i in range(run):
                if x + i < w - 14 and (x + i, y) in paper:
                    S.set(x + i, y, '#d6c69c')
            x += run + rnd.randint(2, 5)
    return S


ASSETS['caption-strip'] = lambda: caption_strip(600, 74)


# ---------------------------------------------------------------- tags and keys on vellum
for _w in (112, 124, 136, 140, 148, 196, 204, 212):
    ASSETS['tag-%dx28' % _w] = (lambda w: (lambda: vellum_button(w, 28, seed=6)))(_w)


def key_vellum(w, h=18, grey=False):
    """a key cap ruled in ink on vellum: a sepia outline with its corners cut, the far wall a shade lighter
    below and right; greyed, the outline is drawn in the faded ink of a struck line"""
    s = Spr(w, h)
    c, lo = (INK[3], VEL[4]) if not grey else (VEL[2], VEL[5])
    s.hline(1, w - 2, 0, c); s.hline(1, w - 2, h - 1, c)
    s.vline(0, 1, h - 2, c); s.vline(w - 1, 1, h - 2, c)
    s.hline(1, w - 2, h - 2, lo); s.vline(w - 2, 1, h - 2, lo)
    return s


for _w in range(24, 48):
    ASSETS['kv-%dx18' % _w] = (lambda w: (lambda: key_vellum(w)))(_w)
    ASSETS['kvg-%dx18' % _w] = (lambda w: (lambda: key_vellum(w, grey=True)))(_w)


def mouse_use():
    """the use key as the game binds it by default: a mouse in ink, its right button filled"""
    rows = ['..aaaaaaa..',
            '.a...a...a.',
            'a....a.bbba',
            'a....a.bbba',
            'a....a.bbba',
            'aaaaaaaaaaa',
            'a.........a',
            'a.........a',
            'a.........a',
            'a.........a',
            '.a.......a.',
            '.a.......a.',
            '..aaaaaaa..']
    s = Spr(11, 13)
    s.grid(0, 0, rows, {'a': INK[1], 'b': INK[1]})
    return s


ASSETS['mouse-use'] = mouse_use


def clasp_closed():
    """the first-reading mark: a book's clasp, shut. A leather strap comes in from the left over the fore-edge
    and its brass catch sits closed on the stud; the brass lit from the top left, a screw in the catch"""
    rows = ['................',
            '.........eeeeee.',
            '........edddddde',
            'llllllllebbcbbbe',
            'mmmmmmmmebcfcbbe',
            'mmmmmmmmebbcbbae',
            'nnnnnnnnebbbbbae',
            'llllllllebaaaaae',
            '........eaaaaaae',
            '.........eeeeee.',
            '................']
    s = Spr(16, 11)
    s.grid(0, 0, rows, {'l': '#2b1b10', 'm': '#5a3a22', 'n': '#3e2716', 'e': BRASS[1], 'd': BRASS[6],
                        'b': BRASS[4], 'c': BRASS[2], 'f': BRASS[7], 'a': BRASS[3]})
    return s


ASSETS['clasp-closed'] = clasp_closed


def clasp_open():
    rows = ['................',
            '.........eeeeee.',
            '........edddddde',
            '........ebbbbbbe',
            'llllll..ebbcbbbe',
            'mmmmmmm.ebbbbbae',
            'nnnnnn..ebbbbbae',
            '........ebaaaaae',
            '........eaaaaaae',
            '.........eeeeee.',
            '................']
    s = Spr(16, 11)
    s.grid(0, 0, rows, {'l': '#2b1b10', 'm': '#5a3a22', 'n': '#3e2716', 'e': BRASS[1], 'd': BRASS[6],
                        'b': BRASS[4], 'c': BRASS[2], 'f': BRASS[7], 'a': BRASS[3]})
    return s


ASSETS['clasp-open'] = clasp_open


# ---------------------------------------------------------------- pips: where a book scene stands
def pips(n, cur, gap=6):
    """n lozenges in a row: the panels read are filled brass, the current one is larger and lit, the rest are
    brass outlines; a dark rim on every lozenge so they hold on sky or field"""
    small = ['...a...',
             '..aba..',
             '.abcba.',
             'abcccba',
             '.abcba.',
             '..aba..',
             '...a...']
    hollow = ['...a...',
              '..aba..',
              '.ab.ba.',
              'ab...ba',
              '.ab.ba.',
              '..aba..',
              '...a...']
    big = ['....a....',
           '...aea...',
           '..aedea..',
           '.aedfdea.',
           'aedfffdea',
           '.aedfdea.',
           '..aedea..',
           '...aea...',
           '....a....']
    w = (n - 1) * (7 + gap) + 9
    s = Spr(w, 9)
    x = 0
    for i in range(n):
        if i == cur:
            s.grid(x, 0, big, {'a': '#0b0809', 'e': BRASS[3], 'd': BRASS[6], 'f': BRASS[7]})
            x += 9 + gap
        else:
            s.grid(x, 1, small if i < cur else hollow,
                   {'a': '#0b0809', 'b': BRASS[4] if i < cur else BRASS[5], 'c': BRASS[3]})
            x += 7 + gap
    return s


ASSETS['pips-5-2'] = lambda: pips(5, 2)


# ---------------------------------------------------------------- 04: the stage scene's dialog card
def dialog_card(w, h, seed=11):
    """the UI kit's vellum card for a line of dialog: the caption strip's vellum (deckled, aged at the rim,
    fibres), ruled round in faded sepia a little inside the edge, the ruling broken where the pen lifted and
    left open at the corners as the kit's tags are"""
    S, paper = parchment(w, h, kind='hi', seed=seed, shadow=(2, 4), stains=0, foxing=0.5, fibres=0.8, rim=3,
                         mottle='edge')
    rnd = random.Random(seed)
    c = '#d6c69c'

    def run(points):
        i = 0
        while i < len(points):
            n = rnd.randint(28, 80)
            for (x, y) in points[i:i + n]:
                if (x, y) in paper:
                    S.set(x, y, c)
            i += n + rnd.randint(2, 4)
    run([(x, 7) for x in range(12, w - 12)])
    run([(x, h - 8) for x in range(12, w - 12)])
    run([(7, y) for y in range(12, h - 12)])
    run([(w - 8, y) for y in range(12, h - 12)])
    return S


ASSETS['card-404x104'] = lambda: dialog_card(404, 104)


def mini_frame(n=60):
    """the speaker's miniature, as a scribe frames one: an ink line, a two-pixel brass fillet lit from the top
    left, a dark inner line, and a ground of dark morocco diapered in lozenges with a brass pin at each
    crossing; the portrait (n x n) is laid in the window at (3, 3)"""
    W = n + 6
    s = Spr(W, W)
    s.rect(0, 0, W, W, INK[0])
    s.rect(1, 1, W - 2, W - 2, BRASS[4])
    s.hline(1, W - 2, 1, BRASS[6]); s.vline(1, 1, W - 2, BRASS[5])
    s.hline(2, W - 3, W - 2, BRASS[2]); s.vline(W - 2, 2, W - 2, BRASS[2])
    s.hline(2, W - 3, 2, BRASS[5]); s.vline(2, 2, W - 3, BRASS[4])
    s.hline(3, W - 3, W - 3, BRASS[3]); s.vline(W - 3, 3, W - 3, BRASS[3])
    s.set(1, 1, BRASS[7])
    for y in range(n):
        for x in range(n):
            a, b = (x + y) % 10, (x - y) % 10
            c = LEA[3]
            if a == 0 or b == 0:
                c = LEA[5]
            if a == 0 and b == 0:
                c = BRASS[2]
            if y > n - 7 and (x + y) % 2 == 0:       # the ground darkens a little at the foot
                c = LEA[2] if c == LEA[3] else c
            s.set(3 + x, 3 + y, c)
    s.rect(3, 3, n, 1, INK[0]); s.rect(3, 3, 1, n, INK[0])
    return s


ASSETS['mini-66'] = lambda: mini_frame(60)


def voice_mark():
    """a voiced line's mark after the speaker's name: a mouth point and three arcs of sound spreading right,
    in the sepia the card's quieter words are written in"""
    s = Spr(12, 13)
    cx, cy = 1.0, 6.0
    for r in (3.6, 6.6, 9.6):
        for y in range(13):
            for x in range(12):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy)
                if abs(d - r) < 0.55 and dx > 0 and abs(math.degrees(math.atan2(dy, dx))) < 48:
                    s.set(x, y, INK[3])
    s.rect(0, 5, 2, 2, INK[3])
    return s


ASSETS['voice-mark'] = voice_mark


def bar(w, h, inner, seed=5):
    """a letterbox bar for a stage scene: the dark of the book's morocco with its pebbled grain barely there,
    and one tooled line in dark gilt four pixels in from the edge that meets the picture"""
    rnd = random.Random(seed)
    s = Spr(w, h, LEA[0])
    nz = Noise(seed, 5)
    for y in range(h):
        for x in range(w):
            v = nz(x, y)
            if v > 0.78:
                s.set(x, y, LEA[1])
            elif v < 0.12:
                s.set(x, y, SH)
    for _ in range(w * h // 28):
        x, y = rnd.randrange(w), rnd.randrange(h)
        s.set(x, y, LEA[1] if rnd.random() < 0.7 else LEA[2])
    ty = h - 5 if inner == 'bottom' else 4
    for x in range(w):
        s.set(x, ty, BRASS[1] if x % 9 else BRASS[2])
    ey = h - 1 if inner == 'bottom' else 0
    s.hline(0, w - 1, ey, SH)
    return s


ASSETS['bar-top'] = lambda: bar(960, 48, 'bottom')
ASSETS['bar-bot'] = lambda: bar(960, 48, 'top', seed=6)


# ---------------------------------------------------------------- 05: the march card
ASSETS['card-400x394'] = lambda: dialog_card(400, 394, seed=17)
ASSETS['tag-164x28'] = lambda: vellum_button(164, 28, seed=6)


def mini_frame_grey(n=40):
    """a miniature whose sitter is resting: the same frame with its brass gone to pewter and the diaper ground
    to cold grey, so the tile reads as out of the march before the portrait is looked at"""
    s = mini_frame(n)
    pew = {BRASS[2]: IRON[4], BRASS[3]: IRON[5], BRASS[4]: IRON[6], BRASS[5]: IRON[7], BRASS[6]: IRON[8],
           BRASS[7]: IRON[8], LEA[2]: IRON[1], LEA[3]: IRON[2], LEA[5]: IRON[3]}
    pew = {rgba(k): v for k, v in pew.items()}
    for y in range(s.h):
        for x in range(s.w):
            p = s.px[x, y]
            if p[3] and p in pew:
                s.set(x, y, pew[p])
    return s


ASSETS['mini-46'] = lambda: mini_frame(40)
ASSETS['mini-46-grey'] = lambda: mini_frame_grey(40)


def moon():
    """resting: a small crescent moon in brass with an ink rim, for the corner of a resting companion's frame"""
    s = Spr(12, 12)
    cx, cy, r = 5.5, 6.0, 5.0
    ox, oy, r2 = 8.2, 4.2, 4.3
    body = set()
    for y in range(12):
        for x in range(12):
            if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r and math.hypot(x + 0.5 - ox, y + 0.5 - oy) > r2:
                body.add((x, y))
    for (x, y) in body:
        lit = math.hypot(x + 0.5 - cx, y + 0.5 - cy) > r - 1.2 and x < 6
        s.set(x, y, BRASS[7] if lit else BRASS[5])
    for (x, y) in body:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            k = (x + dx, y + dy)
            if k not in body and 0 <= k[0] < 12 and 0 <= k[1] < 12:
                s.set(k[0], k[1], INK[0])
    return s


ASSETS['moon'] = moon


def glyph_domination():
    """the mode glyph for Domination, in ink: a pennant on a staff planted in a ring, a point held"""
    rows = ['.a...........',
            '.aaa.........',
            '.aaaaa.......',
            '.aaaaaaa.....',
            '.aaaaa.......',
            '.aaa.........',
            '.a...........',
            '.a...........',
            '.a...........',
            '.a...........',
            '.a...bbbb....',
            'bab.b....b...',
            '.bbbb.....b..']
    # the ring drawn as an ellipse round the staff's foot
    s = Spr(13, 13)
    pts = set()
    for i in range(48):
        t = i / 48.0 * 2 * math.pi
        pts.add((round(4.5 + 4.2 * math.cos(t)), round(11.0 + 1.6 * math.sin(t))))
    for (x, y) in pts:
        if 0 <= x < 13 and 0 <= y < 13:
            s.set(x, y, INK[3])
    for j, row in enumerate(rows[:10]):
        for i, ch in enumerate(row):
            if ch == 'a':
                s.set(i + 3, j, INK[1])
    s.set(4, 10, INK[1]); s.set(4, 11, INK[1])
    return s


ASSETS['glyph-dom'] = glyph_domination


def key_brass(w, h=18):
    """a key cap struck in brass for the brass button: dark rim, a lit top and left, a shaded foot"""
    s = Spr(w, h)
    s.rect(1, 1, w - 2, h - 2, BRASS[5])
    s.hline(1, w - 2, 0, BRASS[1]); s.hline(1, w - 2, h - 1, BRASS[1])
    s.vline(0, 1, h - 2, BRASS[1]); s.vline(w - 1, 1, h - 2, BRASS[1])
    s.hline(1, w - 2, 1, BRASS[7]); s.vline(1, 1, h - 2, BRASS[6])
    s.hline(2, w - 2, h - 2, BRASS[3]); s.vline(w - 2, 2, h - 2, BRASS[3])
    return s


ASSETS['kb-44x18'] = lambda: key_brass(44)


def rule_orn(w=120):
    """a scribe's line-filler under a title: a hairline in faded ink each side of a small open lozenge"""
    s = Spr(w, 7)
    c = w // 2
    for x in range(w):
        if abs(x - c) > 6 and (x < 3 or x > w - 4 or True):
            s.set(x, 3, INK[4] if abs(x - c) < w // 2 - 10 else INK[5])
    for (dx, dy) in ((0, -3), (1, -2), (2, -1), (3, 0), (2, 1), (1, 2), (0, 3), (-1, 2), (-2, 1), (-3, 0), (-2, -1),
                     (-1, -2)):
        s.set(c + dx, 3 + dy, INK[3])
    s.set(c, 3, INK[3])
    return s


ASSETS['rule-orn'] = rule_orn


# ================================================================ the open spread (02)
from PIL import Image as _Image
from v4a_furn import brass_button
from v4a_paper import wax_seal

TISSUE = '#ece2c6'
VEL_BODY = '#f1e7cb'


def _rgb(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def _thumb(name):
    return _Image.open(os.path.join(ART, name + '.png')).convert('RGB')


STILLS = os.path.join(HERE, 'stills')


def th_still(src, box):
    """a stage scene's plate picture: a 16:9 crop of the scene's in-game still (stills/, shot through the game
    camera by the staging agent), brought down to the plate's 128 x 72 with a Lanczos filter and a light unsharp
    mask so the figures keep their edges at the kit's scale. Nothing is painted on it."""
    from PIL import ImageFilter
    im = _Image.open(os.path.join(STILLS, src)).convert('RGB').crop(box)
    im = im.resize((128, 72), _Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=0.8, percent=60, threshold=2))
    s = Spr(128, 72)
    s.im.paste(im.convert('RGBA'), (0, 0))
    s.px = s.im.load()
    return s


# the read plate: Order on the Crossings, the north crossing by day (the staging's larger-soldiers take); the
# sealed plate: The North Crossing, the camp's fire at dusk, cropped round the fire so the banner and Maud show
# either side of the seal
ASSETS['th-order'] = lambda: th_still('plate-order-crossings-alt1.png', (120, 60, 1800, 1005))
ASSETS['th-north'] = lambda: th_still('north-crossing-dusk.png', (420, 250, 1500, 858))


def th_battle():
    """a battle's plate: the battle's own map (the kit's Rivergate sheet, its neatline cropped away) with the
    mode's glyph on a small vellum tab pinned in the top left corner"""
    m = _Image.open(os.path.join(KIT, 'v4', 'map-rivergate.png')).convert('RGBA')
    im = m.crop((6, 6, 134, 78))
    s = Spr(128, 72)
    s.im.paste(im, (0, 0))
    s.px = s.im.load()
    # the tab: ink rim, vellum face lit on the top left, a shadow on the map; the glyph in ink on it
    x0, y0, w, h = 3, 3, 19, 19
    for i in range(w):
        s.over(x0 + 1 + i, y0 + h, SH, 110)
    for j in range(h):
        s.over(x0 + w, y0 + 1 + j, SH, 110)
    s.rect(x0, y0, w, h, INK[1])
    s.rect(x0 + 1, y0 + 1, w - 2, h - 2, VEL[7])
    s.hline(x0 + 1, x0 + w - 2, y0 + 1, VEL[8]); s.vline(x0 + 1, y0 + 1, y0 + h - 2, VEL[8])
    s.hline(x0 + 2, x0 + w - 2, y0 + h - 2, VEL[5]); s.vline(x0 + w - 2, y0 + 2, y0 + h - 2, VEL[5])
    g = _Image.open(os.path.join(ART, 'glyph-dom.png')).convert('RGBA')
    gp = g.load()
    for y in range(13):
        for x in range(13):
            if gp[x, y][3]:
                s.set(x0 + 3 + x, y0 + 3 + y, gp[x, y])
    return s


ASSETS['th-battle'] = th_battle


def plate(name, state):
    """a plate tipped into the leaf, as a book of the Record would carry its pictures: the still (128 x 72) in a
    one-pixel ink line on a paper mount three pixels wide, lit on its top left arris and shaded on the lower
    right, with a hair of shadow on the vellum. read: the picture a little faded, as printed pictures go.
    next: the mount is a gilt fillet instead, lit top left, and the picture at full strength. sealed: a sheet
    of tissue laid over the picture (the guard a printer puts over a plate) so it shows through ghosted and
    fibred, and a wax seal pressed on the tissue"""
    W, H = 128 + 10, 72 + 10
    s = Spr(W + 2, H + 2)
    # the hair of shadow on the vellum
    for x in range(2, W + 1):
        s.over(x, H, SH, 70); s.over(x + 1, H + 1, SH, 26)
    for y in range(2, H):
        s.over(W, y, SH, 70); s.over(W + 1, y + 1, SH, 26)
    if state == 'next':
        s.rect(0, 0, W, H, BRASS[1])
        for y in range(1, H - 1):
            for x in range(1, W - 1):
                d = min(x - 1, y - 1, W - 2 - x, H - 2 - y)
                if d > 2:
                    continue
                tl = min(x - 1, y - 1) <= min(W - 2 - x, H - 2 - y)
                c = [BRASS[6], BRASS[5], BRASS[3]][d] if tl else [BRASS[3], BRASS[4], BRASS[5]][d]
                s.set(x, y, c)
        for (x, y) in ((1, 1), (2, 1), (1, 2), (W - 12, 1), (W - 11, 1), (1, H - 14)):
            s.set(x, y, BRASS[7])
    else:
        s.rect(0, 0, W, H, VT_RIM2)
        rnd = random.Random(len(name) * 7 + (3 if state == 'sealed' else 1))
        for y in range(1, H - 1):
            for x in range(1, W - 1):
                c = '#e5d7b1'
                if y == 1 or x == 1:
                    c = '#f6eedb'
                elif y == H - 2 or x == W - 2:
                    c = '#d6c69c'
                elif rnd.random() < 0.06:
                    c = '#ddcfab'
                s.set(x, y, c)
    s.frame(4, 4, 130, 74, INK[1])
    im = _thumb(name)
    p = im.load()
    for y in range(72):
        for x in range(128):
            r, g, b = p[x, y]
            if state == 'read':
                # a printed picture gone a little toward sepia: half the colour kept, then a veil of the leaf
                l = 0.30 * r + 0.59 * g + 0.11 * b
                q = '#%02x%02x%02x' % (round(r * 0.55 + l * 0.45 * 1.06), round(g * 0.55 + l * 0.45 * 0.98),
                                       round(b * 0.55 + l * 0.45 * 0.84))
                c = mix(q, VEL_BODY, 0.26)
            elif state == 'sealed':
                l = 0.30 * r + 0.59 * g + 0.11 * b
                q = '#%02x%02x%02x' % tuple(round(v * 0.4 + l * 0.6) for v in (r, g, b))
                c = mix(q, TISSUE, 0.66)
            else:
                c = (r, g, b)
            s.set(5 + x, 5 + y, c)
    if state == 'sealed':
        # the tissue: fibres, a soft crease across it, its edge a hair proud of the picture
        rnd = random.Random(5)
        for _ in range(70):
            x = rnd.randint(6, 128); y = rnd.randint(6, 74)
            for i in range(rnd.randint(2, 4)):
                if 5 <= x + i < 133:
                    s.over(x + i, y, '#fbf6e8' if rnd.random() < 0.6 else '#d8caa4', 150)
        for x in range(5, 133):
            y = round(5 + (x - 5) * 0.23 + 12)
            if 5 <= y < 77:
                s.over(x, y, '#fbf6e8', 120)
                if y + 1 < 77:
                    s.over(x, y + 1, '#c9b68a', 70)
        seal = wax_seal(14, HOLLOW_DEV, seed=13, drips=((1.9, 3), (0.8, 2)))
        sp = seal.im.load()
        ox, oy = 5 + 64 - seal.w // 2, 5 + 36 - seal.h // 2 + 2
        for y in range(seal.h):
            for x in range(seal.w):
                if sp[x, y][3]:
                    s.over(ox + x, oy + y, sp[x, y][:3], sp[x, y][3])
    return s


VT_RIM2 = '#bba67a'
HOLLOW_DEV = None


def _hollow():
    """the plate's seal is the small one (radius 14), so it takes the eleven-row mark, which sits wholly inside
    the sunk field (coordinator's ruling: nothing in the beaded border)"""
    global HOLLOW_DEV
    if HOLLOW_DEV is None:
        import sb_book
        HOLLOW_DEV = sb_book.HOLLOW_SMALL
    return HOLLOW_DEV


def _plate(name, state):
    _hollow()
    return plate(name, state)


ASSETS['plate-read'] = lambda: _plate('th-order', 'read')
ASSETS['plate-next'] = lambda: _plate('th-battle', 'next')
ASSETS['plate-sealed'] = lambda: _plate('th-north', 'sealed')


def glow_next(w=138, h=82, r=13, peak=0.44, step=0.045):
    """the next plate's light on the leaf: a warm gold breathing off the gilt in stepped rings, dithered where
    one ring gives way to the next; the plate covers the middle"""
    s = Spr(w + 2 * r, h + 2 * r)
    for y in range(h + 2 * r):
        for x in range(w + 2 * r):
            dx = max(r - x, 0, x - (r + w - 1))
            dy = max(r - y, 0, y - (r + h - 1))
            d = math.hypot(dx, dy)
            if d == 0 or d > r:
                continue
            a = (1.0 - d / r) ** 1.4 * peak
            lo = math.floor(a / step)
            q = lo + (1 if dith(x, y, a / step - lo) else 0)
            if q > 0:
                s.set(x, y, rgba('#e8b04a', round(q * step * 255)))
    return s


ASSETS['glow-next'] = glow_next


def drop_cap_t():
    """the leaf's drop capital, three lines deep: a T in burnished gold with an ink outline, lit on its top
    and left edges and turned to old gold on the lower right, set in a square of faint ruled diaper with a gilt
    dot at every crossing, inside a ruled ink frame (the scribe's box for the initial)"""
    W, H = 48, 50
    s = Spr(W, H)
    s.frame(0, 0, W, H, INK[2])
    s.frame(2, 2, W - 4, H - 4, BRASS[4])
    for y in range(3, H - 3):
        for x in range(3, W - 3):
            a, b = (x + y) % 8, (x - y) % 8
            c = None
            if a == 0 or b == 0:
                c = '#dccb9f'
            if a == 0 and b == 0:
                c = BRASS[5]
            if c:
                s.set(x, y, c)
    # the letter: a flared bar, a stem that swells to a flared foot (the face's own T, drawn at this size)
    body = set()
    for y in range(8, 15):
        for x in range(8, 40):
            body.add((x, y))
    for (x0, x1) in ((8, 11), (37, 40)):            # the bar's ends drop into serifs
        for y in range(15, 19):
            for x in range(x0, x1):
                if (x0 == 8 and x < 11 - (y - 15) // 2) or (x0 == 37 and x >= 37 + (y - 15) // 2):
                    body.add((x, y))
    for y in range(15, 42):
        half = 3.5 + (max(0, y - 36) ** 1.5) * 0.5
        for x in range(round(24 - half), round(24 + half)):
            body.add((x, y))
    for (x, y) in body:
        s.set(x, y, BRASS[6])
    for (x, y) in body:
        if (x, y - 1) not in body or (x - 1, y) not in body:
            s.set(x, y, BRASS[7])
        elif (x, y + 1) not in body or (x + 1, y) not in body:
            s.set(x, y, BRASS[3])
        elif (x + 1, y + 1) not in body:
            s.set(x, y, BRASS[4])
    for (x, y) in body:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            k = (x + dx, y + dy)
            if k not in body:
                s.set(k[0], k[1], INK[1])
    # a burnish streak down the stem
    for y in range(17, 34):
        s.set(22, y, BRASS[7])
    return s


ASSETS['cap-t'] = drop_cap_t


def quill_tick():
    """read: a tick made with the quill, thick on the down stroke and hair thin on the up stroke, a small
    blot where the pen came down"""
    rows = ['.............',
            '...........aa',
            '..........ab.',
            '.........ab..',
            '........ab...',
            '.......ab....',
            'bb....ab.....',
            'aab..ab......',
            '.aaabb.......',
            '..aaab.......',
            '...aa........',
            '.............',
            '.............']
    s = Spr(13, 13)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == 'a':
                s.set(i, j, INK[1])
            elif ch == 'b':
                s.set(i, j, INK[3])
    return s


ASSETS['quill-tick'] = quill_tick



def key_arrow(w=30, h=20, left=True):
    """the arrow key cap for the rail, the kit's key with a pixel arrow struck in it"""
    k = _Image.open(os.path.join(ART, 'key-30x20.png')).convert('RGBA')
    s = Spr(k.width, k.height)
    s.im.paste(k, (0, 0)); s.px = s.im.load()
    cx, cy = k.width // 2, 9
    pts = [(-5, 0), (-4, 0), (-3, 0), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0),
           (-4, -1), (-3, -2), (-2, -3), (-4, 1), (-3, 2), (-2, 3), (-3, -1), (-3, 1)]
    for (dx, dy) in pts:
        s.set(cx + (dx if left else -dx), cy + dy, '#f0d9a0')
    return s


ASSETS['key-left'] = key_arrow


def page_icon():
    """the rail plaque's icon for a page: a leaf with its lower corner turned, in ivory and brass"""
    rows = ['aaaaaaaaa....',
            'abbbbbbba....',
            'abccccbba....',
            'abbbbbbba....',
            'abccccbba....',
            'abbbbbbba....',
            'abcccbbba....',
            'abbbbbbba....',
            'abcccccba....',
            'abbbbbbba....',
            'abbbbbbda....',
            'abbbbbdda....',
            'aaaaaaaa.....']
    pal = {'a': BRASS[3], 'b': '#eee9df', 'c': BRASS[4], 'd': BRASS[6]}
    s = Spr(13, 13)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in pal:
                s.set(i + 2, j, pal[ch])
    return s


ASSETS['icon-page'] = page_icon


def rule_line(w, seed=3):
    """a scribe's ruling between the entries on a leaf: one hairline in faded ink, broken where the pen lifted"""
    rnd = random.Random(seed)
    s = Spr(w, 1)
    x = 0
    while x < w:
        run = rnd.randint(30, 90)
        for i in range(run):
            if x + i < w:
                s.set(x + i, 0, '#d6c69c')
        x += run + rnd.randint(2, 4)
    return s


ASSETS['rule-row'] = lambda: rule_line(324)


def brass_tab(w=112, h=28, seed=7):
    """the next scene's tab: a small brass plate riveted to the leaf at both ends, bevelled (lit top and left,
    falling away bottom and right), brushed lightly along its length, rubbed bright on the top edge and grimy in
    the lower corners; its face left clear for the key and the word"""
    rnd = random.Random(seed)
    s = Spr(w + 2, h + 3)
    for x in range(2, w + 1):
        for i, a in enumerate((0.50, 0.30, 0.12)):
            s.over(x, h + i, SH, round(a * 255))
    for y in range(2, h):
        s.over(w, y, SH, 110); s.over(w + 1, y, SH, 45)
    s.rect(0, 0, w, h, BRASS[0])
    s.rect(1, 1, w - 2, h - 2, BRASS[5])
    s.hline(1, w - 2, 1, BRASS[7]); s.hline(2, w - 3, 2, BRASS[6])
    s.vline(1, 2, h - 2, BRASS[6]); s.vline(2, 3, h - 3, BRASS[6])
    s.hline(2, w - 2, h - 2, BRASS[2]); s.hline(3, w - 3, h - 3, BRASS[3])
    s.vline(w - 2, 2, h - 2, BRASS[2]); s.vline(w - 3, 3, h - 3, BRASS[3])
    for _ in range(26):
        y = rnd.randint(4, h - 5); x = rnd.randint(4, w - 14)
        c = BRASS[6] if rnd.random() < 0.5 else BRASS[4]
        for i in range(rnd.randint(3, 9)):
            if s.get(x + i, y)[:3] == rgba(BRASS[5])[:3]:
                s.set(x + i, y, c)
    for x in range(4, w - 4):
        if rnd.random() < 0.18:
            s.set(x, 1, '#fff0c4')
    for (x, y) in ((3, h - 4), (4, h - 4), (3, h - 5), (w - 4, h - 4), (w - 5, h - 4), (w - 4, h - 5)):
        s.set(x, y, BRASS[3])
    for (cx, cy) in ((6, h // 2), (w - 7, h // 2)):
        for (dx, dy, c) in ((-1, -1, BRASS[7]), (0, -1, BRASS[7]), (1, -1, BRASS[6]), (-1, 0, BRASS[7]),
                            (0, 0, BRASS[6]), (1, 0, BRASS[3]), (-1, 1, BRASS[5]), (0, 1, BRASS[3]), (1, 1, BRASS[2])):
            s.set(cx + dx, cy + dy, c)
        for (dx, dy) in ((-2, -1), (-2, 0), (-2, 1), (2, -1), (2, 0), (2, 1), (-1, -2), (0, -2), (1, -2), (-1, 2),
                         (0, 2), (1, 2)):
            s.set(cx + dx, cy + dy, BRASS[1])
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        s.set(x, y, (0, 0, 0, 0))
    return s


ASSETS['tab-brass-112x28'] = brass_tab


if __name__ == '__main__':
    names = sys.argv[1:] or list(ASSETS)
    for n in names:
        save(ASSETS[n](), n)
