"""Painters for the UI kit textures no mockup drew (UI plan Task 1; spec section 7 leaves their pixels to the
builder). Each follows the rule of its mock siblings: the order glyphs move, charge and retreat and the formation
glyphs wedge and loose are 9 x 9 ink grids in the GLYPHS style (with the mock's -faded rule); the neutral point
seal is the contested seal's square plate without the swords; the Dusk health fill is hp_fill in the Dusk ramp;
the crescent feed glyph is an FG grid inked as the notices are; the iron Enlist plate is the brass plate in iron;
the recruit page's Odds note is a light parchment pinned by two tacks (UI plan Task 6)."""
import math
from v4lib import Spr, rgba
from v4pal import IRON, BRASS, VIOL, OAK, VEL, WAX, TEAL
from v4a_hud import glyph, INK, PAPER, FEEDV_GLYPH, SHADOW, iron_shade, ledger_card, BRASSR, notice_slip, feed_slip, hp_channel, stamp
from v4a_paper import parchment, tack

# 'k' ink, 'L' the light vellum inside a shape, as in v4a_hud.GLYPHS
GLYPHS = {
    # move: travel to a place, an arrow with a shaft (outlined like the sword and shield)
    'g-move':    ['.........', '.....k...', '.....kk..', 'kkkkkkLk.', 'kLLLLLLLk', 'kkkkkkLk.', '.....kk..', '.....k...', '.........'],
    # charge: commit at speed, a solid head with speed lines behind it
    'g-charge':  ['....k....', '....kk...', 'kk..kkk..', '....kkkk.', 'kkk.kkkkk', '....kkkk.', 'kk..kkk..', '....kk...', '....k....'],
    # retreat: turn back, a U-turn arrow
    'g-retreat': ['...kkkk..', '..k....k.', '..k.....k', '..k.....k', 'kkkkk...k', '.kkk....k', '..k.....k', '........k', '........k'],
    # wedge: the compact glyph's squares as a point, one, two, three
    'g-wedge':   ['...kk....', '...kk....', '.........', '.kk..kk..', '.kk..kk..', '.........', 'kk.kk.kk.', 'kk.kk.kk.', '.........'],
    # loose: the compact glyph's squares spread out
    'g-loose':   ['kk....kk.', 'kk....kk.', '.........', '...kk....', '...kk....', '.........', 'kk....kk.', 'kk....kk.', '.........'],
}


def glyph_ink(name):
    return glyph(GLYPHS[name])


def glyph_faded(name):
    """the mock's -faded rule: ink INK[3], the light parts in the grey (routing) card's light"""
    return glyph(GLYPHS[name], INK[3], PAPER['grey']['light'])


# Mirrors v4a_hud.py seal('B') (lines 396-469): the size and offset (400-402), B's square plate and field (411-412),
# the shadow (431-435), iron_shade (436) and B's bead ring (441-444). Left out: the crossed swords (413-430), the teal
# and violet field (445-460), the split (461-464) and the glint (465-468); the field is bare IRON[4] instead.
def seal_neutral():
    """the neutral point: seal('B')'s square iron plate, its bead ring and its shadow, without the crossed swords,
    the field bare iron in IRON[4] (a square with nothing on it reads as nobody's, without colour)"""
    size, o = 19, 4
    S = Spr(27, 27)
    outer = {(x, y) for y in range(1, size - 1) for x in range(1, size - 1)}
    inner = {(x, y) for y in range(4, size - 4) for x in range(4, size - 4)}
    for (x, y) in outer:
        k = (x + 1 + o, y + 2 + o)
        if (x + 1, y + 2) not in outer and S.get(*k)[3] < 0.5 * 255:
            S.set(k[0], k[1], rgba(SHADOW, round(0.5 * 255)))
    iron_shade(S, outer, o, o, seed=51 + ord('B'), scratches=False)
    for t in range(3, size - 3, 3):
        for (x, y) in ((t, 2), (t, size - 3), (2, t), (size - 3, t)):
            S.set(o + x, o + y, IRON[7]); S.set(o + x + 1, o + y + 1, IRON[2])
    for (x, y) in inner:
        S.set(o + x, o + y, IRON[4])
    return S


# Mirrors v4a_hud.py hp_fill (lines 561-575): three rows of one ramp, light to dark (563-564, 569-571), then the dark
# edge in the last column (572-574), with VIOL in place of TEAL.
def hp_full_dusk(n=102):
    """v4a_hud.hp_fill's rule in the Dusk ramp: rows VIOL[4], VIOL[3], VIOL[2], the dark edge VIOL[1] in the last column"""
    S = Spr(n, 3)
    for x in range(n):
        for y, c in enumerate((VIOL[4], VIOL[3], VIOL[2])):
            S.set(x, y, c)
    for y in range(3):
        S.set(n - 1, y, VIOL[1])
    return S


# an 8 x 8 crescent in the FG style: 'x' body, 'h' the lit pixels on the upper left, 'd' the shaded side
FG_MOON = ['..hxx...', '.hxx....', 'hxx.....', 'hxx.....', 'xxd.....', 'xxdd....', '.xxddd..', '..xxx...']


def fgv_moon():
    """the Dusk emblem for the feed's deploy and notice lines, inked as v4a_hud.feed_glyph_ink inks 'ivory'"""
    d, m, l = FEEDV_GLYPH['ivory']
    S = Spr(8, 8)
    S.grid(0, 0, FG_MOON, {'x': m, 'h': l, 'd': d})
    return S


def btn_iron(brass):
    """the iron Enlist plate (short of gold): the brass plate with every BRASS[i] pixel replaced by IRON[max(0, i-3)],
    so its face BRASS[5] becomes IRON[2] #1f2427; the shadow, the specular and the tarnish stay as painted"""
    lut = {rgba(c)[:3]: rgba(IRON[max(0, i - 3)])[:3] for i, c in enumerate(BRASS)}
    px = brass.px
    for y in range(brass.h):
        for x in range(brass.w):
            p = px[x, y]
            if p[:3] in lut:
                px[x, y] = lut[p[:3]] + (p[3],)
    return brass


# ================================================================ recruit/
def odds_note():
    """recruit/odds-note (UI plan Task 6): the note the recruit page lays over its letter when Odds is pressed,
    parchment(240, 160, kind='hi') with its shadow, pinned at its top corners by two tacks placed as the field card's
    are (4 px down, 6 px in from the left edge and 13 px in from the right, so a tack's 6 px and its shadow sit on the sheet)"""
    note, _ = parchment(240, 160, kind='hi')
    pin = tack()
    note.blit(pin, 6, 4)
    note.blit(pin, 240 - 13, 4)
    return note


# ================================================================ table/ pages (UI plan Task 7)
# the sheets and notes the War Table pages lay on the oak, each parchment(w, h, kind='lo') pinned at its top corners
# as the field card is; by name: (width, height, seed). The three contract notes are three sheets, so the row of
# them does not repeat one stain three times
SHEETS = {
    'table/sheet-banner': (400, 368, 3),
    'table/note-contract': (200, 96, 5),
    'table/note-contract-b': (200, 96, 6),
    'table/note-contract-c': (200, 96, 8),
    'table/note-weekly': (304, 96, 7),
    'table/note-friends': (360, 300, 9),
    'table/note-room': (228, 368, 11),
    'table/note-room-short': (228, 300, 12),
    'table/sheet-field': (520, 320, 13),
}


def pinned(w, h, seed):
    """a War Table page's sheet: parchment(w, h, kind='lo') with its shadow, two tacks placed as the field card's. Its
    ageing creeps in from the rim and leaves the middle clean (mottle='edge', fibres 0.7, as the muster roll's paper):
    the default spotted mottle lays broad dark blotches across a sheet this size, under the text"""
    note, _ = parchment(w, h, kind='lo', seed=seed, mottle='edge', fibres=0.7)
    pin = tack()
    note.blit(pin, 6, 4)
    note.blit(pin, w - 13, 4)
    return note


def _line(S, x0, y0, x1, y1, c):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        S.set(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)


def class_vanguard():
    """the Vanguard on its plaque, 14 x 14 in the rail icons' manner: a heater shield, its face lit on the left,
    a brass boss in the middle"""
    S = Spr(14, 14)
    face = {(x, y) for y in range(1, 13) for x in range(1, 13)
            if abs(x - 6.5) <= 5.5 - max(0, y - 6) * 0.95}
    for (x, y) in face:
        edge = any((x + a, y + b) not in face for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            S.set(x, y, OAK[0])
        else:
            S.set(x, y, VEL[7] if x < 5 else (VEL[6] if x < 7 else (VEL[4] if x < 10 else VEL[3])))
    for (x, y, c) in ((6, 5, BRASS[7]), (7, 5, BRASS[5]), (6, 6, BRASS[5]), (7, 6, BRASS[2]), (5, 5, BRASS[3]),
                      (8, 6, BRASS[2]), (6, 4, BRASS[4]), (7, 7, BRASS[2]), (6, 7, BRASS[3]), (5, 6, BRASS[4]),
                      (8, 5, BRASS[3]), (7, 4, BRASS[4])):
        S.set(x, y, c)
    return S


def class_marksman():
    """the Marksman: a bow drawn, its arc bulging to the upper left, the string across, an arrow on it pointing out
    past the arc, its fletching red"""
    S = Spr(14, 14)
    cx, cy, r = 12.0, 12.0, 10.0
    arc = set()
    for k in range(0, 91):
        a = math.radians(180 + k)
        arc.add((round(cx + r * math.cos(a)), round(cy + r * math.sin(a))))
    for (x, y) in arc:
        for (a, b) in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            if (x + a, y + b) not in arc and 0 <= x + a < 14 and 0 <= y + b < 14:
                S.set(x + a, y + b, OAK[0])
    for (x, y) in arc:
        S.set(x, y, VEL[6] if x + y < 10 else (VEL[4] if x + y < 16 else VEL[3]))
    _line(S, 2, 12, 12, 2, VEL[2])
    _line(S, 11, 11, 3, 3, BRASS[4])
    for (x, y, c) in ((2, 2, IRON[7]), (3, 2, IRON[5]), (2, 3, IRON[5]), (1, 1, IRON[8]),
                      (11, 12, WAX[4]), (12, 11, WAX[4]), (12, 12, WAX[2]), (10, 11, WAX[5]), (11, 10, WAX[5])):
        S.set(x, y, c)
    return S


ARCANIST = ['.........kkk..',
            '........kThTk.',
            '.......kThtTTk',
            '.......kTtTTsk',
            '.......kkTTssk',
            '......kWkkssk.',
            '.....kWDk.kk..',
            '....kWDk......',
            '...kWDk.......',
            '..kWDk........',
            '.kWDk.........',
            'kWDk..........',
            'kDk...........',
            '.k............']


def class_arcanist():
    """the Arcanist: a staff held up at a slant, a teal light in its head"""
    S = Spr(14, 14)
    S.grid(0, 0, ARCANIST, {'k': OAK[0], 'W': VEL[5], 'D': VEL[3], 'T': TEAL[3], 't': TEAL[4], 'h': TEAL[5],
                            's': TEAL[1]})
    return S


CLASS_ICONS = {'vanguard': class_vanguard, 'marksman': class_marksman, 'arcanist': class_arcanist}


# ================================================================ hud/ the lighter battle HUD (UI Task 8c)
# Kort, 2026-10-22: too much of the screen was blocked. A group that is not selected hangs on the spine as a chip one line
# high; the whole army selected shows its chips in brass over one key strip; the tutorial's lesson is one short note. Each
# is a plate of the cards' own ledger (v4a_hud.ledger_card's vellum, inked edge rule, loss-flash rule and shadow), bound
# at its corners by small caps: the cards' book-corner brackets need 13 px of plate a side, more than an 18 px chip has, so
# these are v4a_hud.corner_caps made small, a right-angled plate a pixel proud of the vellum, one engraved groove for its
# two and a rivet head. Shaded in screen space, so the light stays on the top left whichever corner a cap sits on.
def small_caps(S, w, h, metal=IRON, size=6, seed=5, ox=0, oy=0, rust=0.0, corners=(0, 1, 2, 3)):
    """v4a_hud.corner_caps (lines 92-113) at a chip's size: the same right-angled plate over each corner standing a pixel
    proud, one engraved groove parallel to the cut edge (lit on its far side, as there) in place of two, and a 2 x 2
    rivet head (its specular on the upper left) in place of the 4 x 4 dome. (ox, oy) is where the plate's own top-left
    pixel lies inside S."""
    base = {(x, y) for y in range(-1, size) for x in range(-1, size) if x + y <= size - 1}
    groove = {(x, y) for (x, y) in base if x + y == size - 3 and x >= 0 and y >= 0}
    for k in corners:
        fx, fy = k in (1, 3), k in (2, 3)
        M = lambda x, y: (ox + ((w - 1 - x) if fx else x), oy + ((h - 1 - y) if fy else y))
        m = {M(x, y) for (x, y) in base}
        iron_shade(S, m, 0, 0, ramp=metal, seed=seed + k, rust=rust, scratches=False)
        g = [M(x, y) for (x, y) in groove]
        for (X, Y) in g:
            S.set(X, Y, metal[2])
        for (X, Y) in g:
            for (a, b) in ((1, 0), (0, 1)):
                q = (X + a, Y + b)
                if q in m and q not in g and S.get(*q)[:3] != rgba(metal[0])[:3]:
                    S.set(q[0], q[1], metal[6])
        rx = min(M(0, 0)[0], M(1, 1)[0]); ry = min(M(0, 0)[1], M(1, 1)[1])
        S.grid(rx, ry, ['Hb', 'bd'], {'H': metal[-1], 'b': metal[5], 'd': metal[2]})


# a chip is 176 x 18, the cards' width; the PNG puts the box at (1, 1), (2, 2) with the flash rule
CHIP_W, CHIP_H = 176, 18


def chip(kind='lo', metal=IRON, seed=41, rust=0.0, flash=False):
    """a group's chip: ledger_card's plate at 176 x 18 without its brackets, bound by small caps (iron; brass while the
    whole army is selected); flash, the loss flash's alarm-red rule round it; the grey vellum and rust of a routed or
    fallen group's card"""
    S = ledger_card(CHIP_W, CHIP_H, kind, metal, seed=seed, rust=rust, flash=flash, caps=False)
    pad = 2 if flash else 1
    small_caps(S, CHIP_W, CHIP_H, metal, 6, seed, pad, pad, rust=rust)
    return S


def keys_strip():
    """the key strip under the bottom chip while the whole army is selected: the order keys' row of the selected card on a
    brass-capped plate of its own, 176 x 16"""
    S = ledger_card(CHIP_W, 16, 'lo', BRASSR, seed=45, caps=False)
    small_caps(S, CHIP_W, 16, BRASSR, 6, 45, 1, 1)
    return S


# the lesson note: 280 x 33 for two lines of copy, a three-v whose middle band (PNG rows 12 to 24) is the plate's plain
# side edges round bare vellum, so a third line (a long key name) grows it between its caps
LESSON_W, LESSON_H, LESSON_T, LESSON_B = 280, 33, 12, 14


def lesson_note():
    """the tutorial's lesson as one short note: ledger_card(280, 33)'s plate with small caps of 8 px, iron"""
    S = ledger_card(LESSON_W, LESSON_H, 'lo', IRON, seed=46, caps=False)
    small_caps(S, LESSON_W, LESSON_H, IRON, 8, 46, 1, 1)
    im = S.im
    w, h = im.size
    # the middle band holds no cap: only the inked edge rule and the shadow at its sides, vellum between
    for y in range(LESSON_T, h - LESSON_B):
        for x in range(2, w - 4):
            r, g, b, a = im.getpixel((x, y))
            assert a == 255 and r - b >= 30, ('hud/lesson-note band', x, y, (r, g, b, a))
    return S


# the hint slip, narrower: v4a_hud.notice_slip at 164 x 40; its rows run 142 px from x 10 (BattleHud.HINT_ROW_W), so the
# longest short-name row (PgUp PgDn Turn LAlt Hints, 141 px) still ends 12 px inside the box, as the 176 px slip's did
HINT_W = 164


def hint_slip():
    return notice_slip(HINT_W, 40)


# a chip's strength channel and rout stamp: the card's channel (v4a_hud.hp_channel) and stamp at the chip's 66 px state slot
def chip_channel():
    return hp_channel(66)


def chip_stamp():
    return stamp(66, 14)


def feed_leaf_3():
    """the feed's leaf at the Compact size: v4a_hud.feed_slip ruled for three entries on a 12 px pitch from row 7, 166 x 44
    (the five-entry leaf is 172 x 74); its text runs 134 px from x 20, 12 px short of the binding, as the five-entry leaf's"""
    return feed_slip(w=166, h=44, rows=3, top=7, pitch=12)
