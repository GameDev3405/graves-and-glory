"""Command UI Task 8: the battle command panel's art (spec 2026-11-04-command-ui-design, Panel; mockup variation 2,
"Pennants down the left edge"). CommandPanel (Task 7) draws, at GUI scale 1:
  command/pole           the pole on the left edge, 8 wide: a gold finial, a dark oak shaft that tiles, a gold butt cap
                         (three-v: the shaft's middle rows repeat, so the pole runs the panel's height)
  command/pennant        one order's swallowtail pennant, 64 x 24, hoist at the left: the body in greys only, so the
                         panel tints it with the House colour and draws it about 55% opaque
  command/pennant_trim   the gold trim on the same 64 x 24 frame, drawn over the tinted body at the same spot
  command/icon_<order>   16 x 16 order pictures in steel and gold, inked round so they read on any House colour:
                         attack (a sword point-first up and forward), charge (a lance level with speed streaks),
                         follow (footprints walking on), retreat (an arrow turning back), formation (ranks in brackets)
  command/pill           a glass group pill (nine-slice), its label drawn over it
  command/tray           the glass Formation tray (nine-slice), gold corners
  command/shape_<id>     24 x 24 formation shapes for the tray, front at the top: line (two ranks), block (Shield wall:
                         a row of kite shields with a rank behind), wedge (a point, 1-2-3-4), loose (wide, uneven spacing)
Registered through manifest.add when build_ui_art imports this module (one import line there), as wt_claims is.
Every pixel is painted at GUI scale 1: nothing is resized."""
from v4lib import Spr, rgba
from v4pal import IRON, BRASS, OAK, INK, VEL, WAX
import manifest

INKO = INK[0]          # the outline every icon carries, so it reads on a light or a dark House colour
STEEL = (IRON[8], IRON[7], IRON[5], IRON[3])   # light, mid, shade, deep
GOLD = (BRASS[7], BRASS[5], BRASS[3], BRASS[1])


def g(v):
    return (v, v, v, 255)


# ================================================================ pennant
PW, PH = 64, 24
NOTCH = 47             # the swallowtail's notch starts here; the tails run to x 63


def pennant_mask():
    """the swallowtail: a cloth sagging one pixel between x 22 and 40 on its top edge, a V notch cut into the fly end"""
    m = set()
    for x in range(PW):
        top = 1 if 22 <= x <= 40 else 0
        bottom = PH - 1 - (1 if 24 <= x <= 42 else 0)
        for y in range(top, bottom + 1):
            if x > NOTCH:
                half = (x - NOTCH) * 0.66          # the notch opens toward the fly
                if abs(y - 11.5) < half:
                    continue
                tail = (x - NOTCH) * 0.2            # the tails taper on their outer edges
                if y < top + tail or y > bottom - tail:
                    continue
            m.add((x, y))
    return m


def edge(m, x, y):
    return any((x + dx, y + dy) not in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def pennant():
    """greys only: a light cloth with three soft folds, a darker sleeve at the hoist where it wraps the pole"""
    m = pennant_mask()
    S = Spr(PW, PH)
    folds = {0: 228, 1: 214, 2: 200}
    for (x, y) in m:
        band = (x // 7 + (1 if x % 7 >= 4 else 0)) % 4   # broad vertical folds, clustered
        v = folds[{0: 0, 1: 1, 2: 2, 3: 1}[band]]
        if x <= 2:
            v = 150 if x < 2 else 176                     # the sleeve round the pole
            if y % 4 == 1:
                v = 196                                   # its stitches
        elif y >= PH - 3 and v > 200:
            v -= 14                                       # the lower hem in shade
        if edge(m, x, y):
            v = 168 if x > 2 else v
        S.set(x, y, g(v))
    return S


def pennant_trim():
    """the gold edge on the pennant's own outline, lit from the top-left: bright on top, dark on the lower edges,
    and a gold seam where the sleeve meets the cloth"""
    m = pennant_mask()
    S = Spr(PW, PH)
    for (x, y) in m:
        if x <= 2:
            continue
        if edge(m, x, y):
            upper = y < 12
            S.set(x, y, GOLD[1] if upper else GOLD[2])
            if upper and (x - 1, y) in m and (x, y + 1) in m and y <= 1:
                S.set(x, y, GOLD[0] if x % 6 == 3 else GOLD[1])
    for y in range(PH):
        if (3, y) in m:
            S.set(3, y, GOLD[2] if y % 3 else GOLD[1])  # the seam
    return S


# ================================================================ pole
POLE_W, POLE_H = 8, 32
POLE_TOP, POLE_BOTTOM = 10, 4


def pole():
    """an oak pole with a gold spear finial and a gold butt cap; the shaft rows 10..27 repeat when the pole tiles"""
    S = Spr(POLE_W, POLE_H)
    finial = ['...o....', '..oGo...', '..oGo...', '.oGgGo..', '.oGggo..', 'ooGggoo.', 'oGGgggo.', 'oggggdo.',
              '.oo.oo..', '.oWwdo..']
    S.grid(0, 0, finial, {'o': INKO, 'G': GOLD[0], 'g': GOLD[1], 'd': GOLD[2], 'W': OAK[8], 'w': OAK[6]})
    for y in range(POLE_TOP, POLE_H - POLE_BOTTOM):
        S.set(1, y, INKO); S.set(2, y, OAK[8]); S.set(3, y, OAK[6]); S.set(4, y, OAK[4]); S.set(5, y, INKO)
        if y % 6 == 3:
            S.set(3, y, OAK[5])                     # grain
    cap = ['oGggdo..', 'oggddo..', '.oddo...', '..oo....']
    S.grid(0, POLE_H - POLE_BOTTOM, cap, {'o': INKO, 'G': GOLD[0], 'g': GOLD[1], 'd': GOLD[2]})
    return S


# ================================================================ glass
GLASS = rgba(IRON[1], 150)
RIM_HI = rgba(IRON[8], 170)
RIM_LO = rgba(IRON[0], 210)
SHEEN = rgba('#ffffff', 46)


def glass(w, h, r):
    """a dark see-through pane with rounded corners of radius r, a pale rim lit from the top-left, a sheen row"""
    S = Spr(w, h)

    def inside(x, y):
        cx = min(max(x, r), w - 1 - r)
        cy = min(max(y, r), h - 1 - r)
        return (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6

    m = {(x, y) for y in range(h) for x in range(w) if inside(x, y)}
    for (x, y) in m:
        S.set(x, y, GLASS)
    for (x, y) in m:
        if edge(m, x, y):
            lit = (x - 1, y) not in m or (x, y - 1) not in m
            low = (x + 1, y) not in m or (x, y + 1) not in m
            S.set(x, y, RIM_LO if low and not (y < h // 2 and lit) else RIM_HI)
        elif y == 2 or (y == 1 and (x, 0) not in m and (x, 1) in m and not edge(m, x, y)):
            S.over(x, y, SHEEN)
    return S


def pill():
    """the group pill: 24 x 12 glass with round ends (nine-slice, the round ends stay whole)"""
    return glass(24, 12, 5)


def tray():
    """the Formation tray: 32 x 32 glass, gold corner brackets inside the rim (nine-slice keeps them whole)"""
    S = glass(32, 32, 3)
    for (cx, cy, sx, sy) in ((2, 2, 1, 1), (29, 2, -1, 1), (2, 29, 1, -1), (29, 29, -1, -1)):
        for i in range(4):
            S.set(cx + sx * i, cy, GOLD[1] if sy > 0 else GOLD[2])
            S.set(cx, cy + sy * i, GOLD[1] if sx > 0 else GOLD[2])
        S.set(cx, cy, GOLD[0])
    return S


# ================================================================ order icons, 16 x 16
def inked(S):
    """the ink outline round the whole picture (four-neighbour), as the HUD glyphs are inked"""
    S.outline(INKO)
    return S


def icon_attack():
    """a sword point-first up and to the right: the move is forward, so the blade leads"""
    S = Spr(16, 16)
    for t in range(9):                      # the blade, from the point at (14, 1) down-left
        x, y = 14 - t, 1 + t
        S.set(x, y, STEEL[0])
        if t:
            S.set(x + 1, y, STEEL[1]); S.set(x, y + 1, STEEL[2])
    S.set(14, 1, STEEL[0])
    for i in range(-2, 3):                   # the crossguard, square to the blade
        S.set(5 + i, 10 + i, GOLD[1] if i < 0 else GOLD[2])
    S.set(5, 10, GOLD[0])
    S.line(4, 11, 2, 13, OAK[6])                          # the grip
    S.set(3, 12, OAK[5])
    S.rect(1, 13, 2, 2, GOLD[1]); S.set(1, 13, GOLD[0])   # the pommel
    return inked(S)


def icon_charge():
    """a couched lance driving right: the long shaft, the gold cone of its vamplate, a narrow steel point, and two
    short speed streaks trailing behind"""
    S = Spr(16, 16)
    rows = ['................',
            '................',
            '................',
            '.WWW............',
            '................',
            '......Gg........',
            '.....GGg..Ww....',
            '.bbbGGGgbbWWWwws',
            '.dddGGGddbWWwwss',
            '.....Ggd..ws....',
            '......d.........',
            '................',
            '.WWW............']
    S.grid(0, 0, rows, {'W': STEEL[0], 'w': STEEL[1], 's': STEEL[2], 'G': GOLD[0], 'g': GOLD[1], 'd': GOLD[2],
                        'b': OAK[7]})
    S.hline(1, 3, 8, OAK[5]); S.hline(8, 9, 8, OAK[5])
    return inked(S)


def foot(S, x, y):
    """one boot print, toe up: a sole of 4 x 5 and a heel of 3 x 2 under it, a gap between"""
    S.rect(x, y, 3, 1, VEL[7]); S.rect(x, y + 1, 4, 3, VEL[6]); S.rect(x + 1, y + 1, 2, 1, VEL[8])
    S.rect(x + 1, y + 4, 3, 1, VEL[5])
    S.rect(x + 1, y + 6, 3, 2, VEL[6])


def icon_follow():
    """footprints walking on, up the icon: one boot behind, one ahead, the march to follow"""
    S = Spr(16, 16)
    foot(S, 2, 7)
    foot(S, 9, 1)
    return inked(S)


def icon_retreat():
    """an arrow that turns back on itself: up from the right, over the top, down to a red head pointing back and down"""
    S = Spr(16, 16)
    arc = ['....WWWW.....',
           '..WWwwwwWW...',
           '.Wws....swW..',
           '.Ws......sW..',
           '.Ws......sW..',
           '.........sW..',
           '.........sW..',
           '.........sW..',
           '.........sW..',
           '.........sW..',
           '.........sW..']
    S.grid(2, 2, arc, {'W': STEEL[0], 'w': STEEL[1], 's': STEEL[2]})
    head = ['RRRRRRR', '.RRRRr.', '..RRr..', '...r...']
    S.grid(0, 7, head, {'R': WAX[5], 'r': WAX[3]})
    return inked(S)


def icon_formation():
    """two ranks of three soldiers inside four gold corner brackets: placing a formation on the ground"""
    S = Spr(16, 16)
    for (cx, cy, sx, sy) in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
        for i in range(3):
            S.set(cx + sx * i, cy, GOLD[1]); S.set(cx, cy + sy * i, GOLD[1])
        S.set(cx, cy, GOLD[0])
    for y in (5, 9):
        for x in (3, 7, 11):
            S.set(x, y, STEEL[0]); S.set(x + 1, y, STEEL[1]); S.set(x, y + 1, STEEL[1]); S.set(x + 1, y + 1, STEEL[2])
    return inked(S)


# ================================================================ formation shapes, 24 x 24, front at the top
def pip(S, cx, cy):
    """one soldier seen from above: a 3 x 3 helm, lit from the top-left"""
    S.set(cx - 1, cy - 1, STEEL[0]); S.set(cx, cy - 1, STEEL[0]); S.set(cx + 1, cy - 1, STEEL[1])
    S.set(cx - 1, cy, STEEL[0]); S.set(cx, cy, STEEL[1]); S.set(cx + 1, cy, STEEL[2])
    S.set(cx - 1, cy + 1, STEEL[1]); S.set(cx, cy + 1, STEEL[2]); S.set(cx + 1, cy + 1, STEEL[3])


def shape_line():
    """two ranks of four, side by side"""
    S = Spr(24, 24)
    for cy in (8, 15):
        for cx in (3, 9, 15, 21):
            pip(S, cx, cy)
    return inked(S)


def shape_block():
    """Shield wall: four steel kite shields edge to edge across the front, a rank of four behind them"""
    S = Spr(24, 24)
    kite = ['gGGGg', 'GWWWs', 'GWbws', 'GWwws', '.Gws.', '.Gws.', '..s..']
    for x in (0, 6, 12, 18):
        S.grid(x + 1, 3, kite, {'G': GOLD[1], 'g': GOLD[0], 's': GOLD[2], 'W': STEEL[0], 'w': STEEL[1], 'b': GOLD[0]})
    for cx in (4, 10, 16, 22):
        pip(S, cx - 1, 16)
    return inked(S)


def shape_wedge():
    """a point at the front: ranks of one, two, three and four"""
    S = Spr(24, 24)
    for r, cy in enumerate((3, 8, 13, 18)):
        for k in range(r + 1):
            pip(S, 12 - 3 * r + 6 * k if r < 3 else 3 + 6 * k, cy)
    return inked(S)


def shape_loose():
    """wide, uneven spacing: seven soldiers spread over the ground"""
    S = Spr(24, 24)
    for (cx, cy) in ((3, 4), (13, 2), (21, 6), (7, 13), (18, 15), (4, 21), (13, 21)):
        pip(S, cx, cy)
    return inked(S)


ICONS = {'attack': icon_attack, 'charge': icon_charge, 'follow': icon_follow, 'retreat': icon_retreat,
         'formation': icon_formation}
SHAPES = {'line': shape_line, 'block': shape_block, 'wedge': shape_wedge, 'loose': shape_loose}

manifest.add('command/pole', pole, kind='three-v', t=POLE_TOP, b=POLE_BOTTOM)
manifest.add('command/pennant', pennant)
manifest.add('command/pennant_trim', pennant_trim)
for _n, _paint in ICONS.items():
    manifest.add('command/icon_' + _n, _paint)
manifest.add('command/pill', pill, kind='nine', l=6, t=4, r=6, b=4)
manifest.add('command/tray', tray, kind='nine', l=7, t=7, r=7, b=7)
for _n, _paint in SHAPES.items():
    manifest.add('command/shape_' + _n, _paint)
