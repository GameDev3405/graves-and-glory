"""The six Phase 2 commander classes' icons and the six class actives' HUD pictures (UI follow-up to plan Task 9).

Class icons are 14 x 14 on the War Table's class plaques (table/icon-class-<id>, drawn at (8, 7) of plaque-120 by
WarTablePages.paintClass), in the manner of the three Phase 1 icons of kit_extra: the plaque's dark oak behind them, a 1 px
OAK[0] edge round the shape, the lit side on the upper left, brass and vellum for the light parts.

HUD pictures are 22 x 20 on the vellum window of hud/ability-slot (hud/ability-<active id>, drawn at (5, 6) of the slot by
BattleHud.ability, as the horn was): ink emblems, not items. The brass key plate covers the slot's upper left (slot
x 1 to 13, y 1 to 15, so picture x 0 to 8, y 0 to 9), so each emblem keeps its weight to the right and below.

Every pixel is a named ramp pixel of v4pal. A picture is a character grid: a letter is a region painted in its ramp
(lit on the upper-left edge, dark on the lower-right edge), a lower case 'k' or a digit a fixed colour; the shape then
gets its 1 px edge."""
import math

from v4lib import Spr
from v4pal import IRON, BRASS, VEL, OAK, WAX, TEAL, VIOL, INK

EDGE = OAK[0]


def paint(rows, pal, w, h, edge=EDGE, fixed=None):
    """rows: character grid; pal: letter -> (lit, base, dark) region ramp; fixed: letter -> one colour (not shaded)."""
    fixed = fixed or {}
    S = Spr(w, h)
    at = lambda x, y: rows[y][x] if 0 <= y < len(rows) and 0 <= x < len(rows[y]) else '.'
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            if ch in fixed:
                S.set(x, y, fixed[ch])
                continue
            hi, base, lo = pal[ch]
            tl = at(x - 1, y) != ch or at(x, y - 1) != ch
            br = at(x + 1, y) != ch or at(x, y + 1) != ch
            S.set(x, y, base if tl == br else (hi if tl else lo))
    if edge:
        S.outline(edge)
    return S


def layers(parts, w, h):
    """parts: (rows, pal, fixed) drawn back to front, each with its own 1 px edge, so a part in front keeps its edge over the one behind"""
    S = Spr(w, h)
    for (rows, pal, fixed) in parts:
        S.blit(paint(rows, pal, w, h, fixed=fixed), 0, 0)
    return S


STEEL = (IRON[8], IRON[7], IRON[5])
BRASSR = (BRASS[7], BRASS[6], BRASS[4])
BRASSD = (BRASS[6], BRASS[5], BRASS[3])

# ================================================================ class icons, 14 x 14
CHAMPION = ['..............',
            '......PP......',
            '......ww......',
            '..GGGGGGGGGG..',
            '..kkkkkkkkkk..',
            '..SSSSggSSSS..',
            '..SSSSggSSSS..',
            '..SSSSggSSSS..',
            '..SSSSggSSSS..',
            '...SSSggSSS...',
            '....SSggSS....',
            '.....SggS.....',
            '......gg......',
            '..............']


def class_champion():
    """the Champion: a great sword planted behind a kite shield, only its pommel and cross-guard above the rim, a brass
    pale down the steel"""
    return paint(CHAMPION, {'P': BRASSR, 'G': BRASSR, 'S': STEEL, 'g': BRASSD}, 14, 14,
                 fixed={'w': VEL[4], 'k': EDGE})


REAVER = ['......TT......',
          '......TT......',
          '.EH...TT...HE.',
          '.EHH..TT..HHE.',
          '.EHHHHTTHHHHE.',
          '.EHHHHTTHHRRE.',
          '.EHHHHTTHRRRR.',
          '.EHH..TT..RRR.',
          '.EH...TT...RR.',
          '......TT...R..',
          '......TT...R..',
          '......TT...r..',
          '......TT......',
          '......TT......']


def class_reaver():
    """the Reaver: a double-bitted axe on a long haft, one bit soaked in blood, a thread of it running down"""
    return paint(REAVER, {'H': STEEL, 'T': (OAK[8], OAK[7], OAK[5])}, 14, 14,
                 fixed={'E': IRON[8], 'R': WAX[5], 'r': WAX[3], 'Q': WAX[6]})


WARDEN = ['......bb......',
          '......bb......',
          '..............',
          '..............',
          '....VAAAAV....',
          '..VVAAhpAAVV..',
          '.VVVAAppAAVVV.',
          '.VVVAAppAAVVV.',
          '..VVAAppAAVV..',
          '....VAAAAV....',
          '..............',
          '..............',
          '......bb......',
          '......bb......']


def class_warden():
    """the Warden: a hawk's eye, amber iris and a slit pupil, a sight tick above and below it"""
    return paint(WARDEN, {'V': (VEL[8], VEL[7], VEL[5]), 'A': (BRASS[7], BRASS[6], BRASS[4]), 'b': BRASSD}, 14, 14,
                 fixed={'p': INK[0], 'h': VEL[8]})


MARSHAL_BANNER = ['............b.',
                  '......RRRRRRb.',
                  '......RRRRRRb.',
                  '........RRRRb.',
                  '......RRRRRRb.',
                  '......RRRRRRb.',
                  '............b.',
                  '............b.',
                  '............b.',
                  '............b.',
                  '............b.',
                  '............b.',
                  '............b.',
                  '............b.']
MARSHAL_HORN = ['..............',
                '..............',
                '..............',
                '..............',
                '..............',
                '..............',
                '........BB....',
                '......hBBBB...',
                '.....hhBBBDb..',
                '...hhiiibBDDb.',
                '...hijjjbbDb..',
                '..hijjj.......',
                '.hij..........',
                '.hj...........']


def class_marshal():
    """the Marshal: a war horn laid in front of a pennon on its pole"""
    banner = {'R': (WAX[6], WAX[5], WAX[3]), 'b': BRASSR}
    horn = {'B': BRASS[6], 'b': BRASS[4], 'h': VEL[7], 'i': VEL[5], 'j': VEL[3], 'D': INK[0]}
    return layers([(MARSHAL_BANNER, banner, None), (MARSHAL_HORN, {}, horn)], 14, 14)


WARLOCK = ['.....uuuu.....',
           '...uuuuuuuu...',
           '...uuUUUUuu...',
           '...uuUUUUuu...',
           '...uuuuuuuu...',
           '.....uuuu.....',
           '......tt......',
           '.....ttt.t....',
           '......tt......',
           '....vvvvvv....',
           '...vvvvvvvv...',
           '...vDDvvDDv...',
           '....vvDDvv....',
           '....vkvvkv....']


def class_warlock():
    """the Warlock: a grave bloom on a thin stem, rooted in a skull"""
    return paint(WARLOCK, {'u': (VIOL[5], VIOL[4], VIOL[2]), 'U': (VEL[8], VEL[7], VEL[5]), 't': (TEAL[5], TEAL[4], TEAL[2]),
                           'v': (VEL[7], VEL[6], VEL[3])}, 14, 14, fixed={'D': INK[0], 'k': EDGE})


QUARTERMASTER_CRATE = ['..............',
                       '..............',
                       '.LLLLLLLLLLL..',
                       '.LLLLLLLLLLL..',
                       '.ccOOOBBOOcc..',
                       '.ccOOOBbOOcc..',
                       '.ccOOOOOOOcc..',
                       '.ccssssssscc..',
                       '.ccOOOOOOOcc..',
                       '.ccOOOOOOOcc..',
                       '.ccOOOOOOOcc..',
                       '.ccOOOOOOOcc..',
                       '.LLLLLLLLLLL..',
                       '..............']
QUARTERMASTER_COIN = ['..............',
                      '..............',
                      '..............',
                      '..............',
                      '..............',
                      '..............',
                      '..............',
                      '.........bBBb.',
                      '........bBYBBb',
                      '........bBBcBb',
                      '........bBBBBb',
                      '.........bBBb.',
                      '..........bb..',
                      '..............']


def class_quartermaster():
    """the Quartermaster: a supply crate of pale planks bound in brass, a coin leaning on it"""
    crate = {'L': (BRASS[6], BRASS[5], BRASS[3]), 'c': (BRASS[5], BRASS[4], BRASS[2]), 'O': (OAK[8], OAK[8], OAK[7]),
             's': (OAK[5], OAK[5], OAK[5]), 'B': (BRASS[7], BRASS[6], BRASS[4]), 'b': (BRASS[6], BRASS[5], BRASS[3])}
    coin = {'B': BRASS[6], 'b': BRASS[4], 'Y': BRASS[7], 'c': BRASS[2]}
    return layers([(QUARTERMASTER_CRATE, crate, None), (QUARTERMASTER_COIN, {}, coin)], 14, 14)


CLASS_ICONS = {'champion': class_champion, 'reaver': class_reaver, 'warden': class_warden, 'marshal': class_marshal,
               'warlock': class_warlock, 'quartermaster': class_quartermaster}


# ================================================================ the class actives' HUD pictures, 22 x 20
AW, AH = 22, 20
LINE = INK[0]


def disc(cx, cy, rx, ry=None):
    ry = rx if ry is None else ry
    return {(x, y) for y in range(AH) for x in range(AW) if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0}


def shade(S, mask, ramp):
    """a region of one ramp (lit, base, dark): lit on the pixels whose upper or left neighbour is outside it, dark on those
    whose lower or right neighbour is, base inside"""
    for (x, y) in mask:
        tl = (x - 1, y) not in mask or (x, y - 1) not in mask
        br = (x + 1, y) not in mask or (x, y + 1) not in mask
        S.set(x, y, ramp[1] if tl == br else (ramp[0] if tl else ramp[2]))


def region(rows_by_y):
    """{y: (x0, x1)} -> the set of (x, y)"""
    return {(x, y) for y, (a, b) in rows_by_y.items() for x in range(a, b + 1)}


def emblem_stand_fast():
    """Stand Fast: a heater shield with a brass boss and a chevron, planted to its middle in a mound of earth"""
    S = Spr(AW, AH)
    cx = 15.0
    shield = set()
    for y in range(1, 18):
        hw = 6.0 if y <= 7 else 6.0 * (1 - ((y - 7) / 11.0) ** 1.3)
        for x in range(AW):
            if abs(x - cx + 0.5) <= hw:
                shield.add((x, y))
    left = {(x, y) for (x, y) in shield if x <= 14}
    right = {(x, y) for (x, y) in shield if x > 14}
    shade(S, left, (IRON[8], IRON[7], IRON[6]))
    shade(S, right, (IRON[6], IRON[5], IRON[3]))
    for (x, y) in shield:                       # the brass rim
        if any((x + a, y + b) not in shield for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            S.set(x, y, BRASS[5] if (x < 15 and y < 12) else BRASS[3])
    for k in range(4):                          # a chevron of ink under the boss
        for dy in (0, 1):
            S.set(11 + k, 12 + k + dy, INK[2])
            S.set(18 - k, 12 + k + dy, INK[2])
    boss = disc(14.5, 6.5, 2.6)
    shade(S, boss, (BRASS[7], BRASS[6], BRASS[3]))
    S.outline(LINE)
    mound = {(x, y) for (x, y) in disc(14, 21, 9.5, 6.0) if y >= 15}
    shade(S, mound, (OAK[8], OAK[7], OAK[5]))
    for (x, y) in ((8, 17), (10, 16), (18, 16), (19, 18), (13, 18)):   # clods and a pebble or two
        S.set(x, y, OAK[4])
    for (x, y) in ((9, 16), (10, 15), (17, 15)):
        S.set(x, y, OAK[8])
    S.outline(LINE)
    return S


def emblem_bloodrush():
    """Bloodrush: a drop of blood, lit on its shoulder, three streaks of speed behind it"""
    S = Spr(AW, AH)
    drop = disc(14.5, 12, 5.2, 5.4)
    for y in range(1, 8):
        hw = 4.6 * ((y - 1) / 6.0) ** 1.4
        for x in range(AW):
            if abs(x - 14.5) <= hw:
                drop.add((x, y))
    shade(S, drop, (WAX[6], WAX[4], WAX[2]))
    for (x, y, c) in ((11, 10, WAX[6]), (11, 11, WAX[6]), (12, 9, VEL[7]), (11, 12, VEL[7]), (12, 8, WAX[6])):
        S.set(x, y, c)
    S.outline(LINE)
    for (y, x0, x1) in ((11, 1, 6), (14, 3, 7), (17, 5, 9)):       # the streaks, bare ink on the vellum
        S.hline(x0, x1, y, INK[3])
    return S


def emblem_mark_prey():
    """Mark Prey: a sight ring in blood red, its cross-hairs inked through it, a bright pip at the heart"""
    S = Spr(AW, AH)
    cx, cy = 14, 10
    ring = disc(cx, cy, 6.6) - disc(cx, cy, 4.4)
    shade(S, ring, (WAX[5], WAX[4], WAX[2]))
    for x in (cx - 1, cx):                       # the cross-hairs: two px wide, out past the ring and in short of the pip
        for y in list(range(1, 6)) + list(range(15, 19)):
            S.set(x, y, INK[2])
    for y in (cy - 1, cy):
        for x in list(range(6, 10)) + list(range(18, 22)):
            S.set(x, y, INK[2])
    pip = disc(cx - 0.5, cy - 0.5, 1.7)
    shade(S, pip, (WAX[6], WAX[5], WAX[4]))
    S.outline(LINE)
    return S


def wedge(S, tip_x, tip_y, direction, depth=6, half=3, ramp=(IRON[8], IRON[6], IRON[4])):
    """a solid arrowhead pointing along x (direction 1: right, -1: left), tip at (tip_x, tip_y), depth px long, 2 * half + 1 tall at its base"""
    px = set()
    for i in range(depth):
        h = round(half * i / (depth - 1))
        for dy in range(-h, h + 1):
            px.add((tip_x - direction * i, tip_y + dy))
    shade(S, px, ramp)


def emblem_rally_horn():
    """Muster: a standard with its pennon flying, an arrowhead on each side closing in on its pole, one lower than the other"""
    S = Spr(AW, AH)
    pole = {(x, y) for y in range(1, 18) for x in (10, 11)}
    shade(S, pole, (BRASS[6], BRASS[5], BRASS[3]))
    ends = [19, 19, 18, 16, 16, 18, 19, 19]
    pen = {(x, 1 + i) for i, e in enumerate(ends) for x in range(12, e + 1)}
    shade(S, pen, (WAX[6], WAX[5], WAX[3]))
    for y in range(1, 9):
        S.set(12, y, BRASS[5])
    wedge(S, 8, 12, 1)
    wedge(S, 13, 15, -1)
    S.outline(LINE)
    return S


def emblem_grave_bloom():
    """Grave Bloom: a violet bloom opened on its stem out of a mound, a skull laid in the earth beside it"""
    S = Spr(AW, AH)
    mound = {(x, y) for (x, y) in disc(12, 22, 11.5, 6.5) if y >= 15}
    shade(S, mound, (OAK[8], OAK[7], OAK[5]))
    stem = {(x, y) for y in range(8, 17) for x in (14, 15)}
    shade(S, stem, (TEAL[4], TEAL[3], TEAL[1]))
    leaf = disc(11.5, 12, 2.4, 1.3) | disc(18, 11, 2.4, 1.3)
    shade(S, leaf, (TEAL[4], TEAL[3], TEAL[1]))
    petals = set()
    for k in range(5):
        a = math.radians(90 + 72 * k)
        petals |= disc(14.5 + 3.4 * math.cos(a), 5.5 - 3.4 * math.sin(a), 2.7)
    shade(S, petals, (VIOL[4], VIOL[3], VIOL[1]))
    heart = disc(14.5, 5.5, 2.2)
    shade(S, heart, (VEL[8], VEL[7], VEL[5]))
    skull = region({12: (3, 8), 13: (2, 9), 14: (2, 9), 15: (2, 9), 16: (3, 8), 17: (4, 7)})
    shade(S, skull, (VEL[8], VEL[7], VEL[4]))
    for (x, y) in ((3, 14), (4, 14), (3, 13), (4, 13), (7, 14), (8, 14), (7, 13), (8, 13), (5, 16), (6, 16)):
        S.set(x, y, INK[0])
    S.outline(LINE)
    return S


def emblem_forward_supply():
    """Forward Supply: a pennant on a staff planted in a brass-strapped supply crate"""
    S = Spr(AW, AH)
    crate = region({y: (7, 20) for y in range(10, 19)})
    shade(S, crate, (OAK[8], OAK[7], OAK[5]))
    for y in (13, 16):
        S.hline(8, 19, y, OAK[4])
    for x in (8, 19):
        for y in range(11, 18):
            S.set(x, y, BRASS[4])
    S.hline(7, 20, 10, BRASS[6])
    staff = {(x, y) for y in range(1, 10) for x in (10, 11)}
    shade(S, staff, (OAK[8], OAK[7], OAK[5]))
    pen = {(x, 1 + i) for i, e in enumerate([14, 16, 18, 20, 18, 16, 14]) for x in range(12, e + 1)}
    shade(S, pen, (BRASS[7], BRASS[6], BRASS[3]))
    S.outline(LINE)
    return S


ABILITY = {'stand_fast': emblem_stand_fast, 'bloodrush': emblem_bloodrush, 'mark_prey': emblem_mark_prey,
           'rally_horn': emblem_rally_horn, 'grave_bloom': emblem_grave_bloom, 'forward_supply': emblem_forward_supply}
