"""The War Table's props and its per-page oak (UI plan Task 5; Global Constraints, Kort 2026-10-18 late): props draw at
the kit's pixel scale (one pixel here is one canvas pixel, two screen pixels at 1080p) at about 2.5 times the round 4
home mock's size (its candle 44 x 54, its inkwell 50 x 58), with the candle's light and shadow on the oak; every War
Table page lies on its own stretch of the table: its own plank layout, knots and nail heads from the same kit, never
the home page's backdrop again. No table carries a brass fitting of any kind, no butterfly key and no mending plate
(Kort, 2026-10-18): a page's variety is its plank layout, knots and nail heads only.

- candle(): the chamberstick, its candle burned low, as a sheet of four frames whose flame flickers (the game shows
  each for 6 ticks); its contact shadow and the shadow it throws from the table's key light (top left) are part of
  each frame. Burned low, it stands under a full party of four with the 12 px edge margin kept.
- glow(): the candle's warm pool on the oak, drawn under the props and the content (the game makes it breathe).
- inkwell(): the inkwell with its quill, a sheet of three frames: at rest, the tip twitched, and half back.
- plate_glint(): the streak of light that crosses the brass Ride plate once when it gains the focus.
- table(page): the 1280 x 720 oak for a page, from oak_table with the page's seed.

The candle and the inkwell follow the storybook artist's 2.5x props (work/ui-concepts/storybook/sb_table.py,
candle_big and inkwell_big, 2026-10-18), redrawn upright, a little smaller and framed for sheets."""
import math, random
from v4lib import Spr, dith, rgba, mix
from v4pal import BRASS, VEL, INK
from v4a_furn import ell_in, engrave, SPEC, SH
import v4art

FLAME = ['#4a6fb5', '#e0802c', '#f2a93f', '#ffd27a', '#fff4d6']
WARM = '#ffb35c'
GLASS = ['#0a0f0d', '#111915', '#18241f', '#21322b', '#2d443a', '#41604f', '#6f9381', '#b5d1c2']

# ================================================================ the oak, page by page
# Each page's stretch of the table: its seed (the plank layout, knots, joints and nail heads oak_table cuts from it) and
# the boxes where no knot or joint may sit (bare oak under text, in 1280 x 720 table pixels: the 960 x 540 page sits at
# (160, 90)). Home and recruit are painted now; the rest are reserved for UI Tasks 6 and 7 (their textures will be
# table/oak-table-<page>). Variety comes from the plank layout, knots and nail heads only: no brass fitting of any kind,
# no butterfly key and no mending plate (Kort, 2026-10-18: banned from every table background).
TABLE_SEEDS = {'home': 11, 'recruit': 23, 'banner': 37, 'contracts': 43, 'chapter': 53, 'friends': 67, 'field': 79,
               'results': 97}
TABLE_AVOID = {
    'home': [(200, 270, 520, 462)],          # the party, as the round 4 mock kept it (home's planks are the mock's)
    'recruit': [(824, 434, 1064, 586)],      # the ladder under the letters
}


def table(page):
    """a page's 1280 x 720 stretch of the War Table"""
    return v4art.oak_table(W=1280, H=720, seed=TABLE_SEEDS[page], avoid=TABLE_AVOID.get(page, []))


# ================================================================ the chamberstick
CW, CH = 124, 128                     # one frame
CX, CY = 50, 84                       # the stick's axis, the dish's centre
RX, RY, IRX, IRY = 42, 15, 36, 11.4
S0, SR = CY - 20, 11                  # the socket's lip and radius
C0, CR = S0 - 22, 9                   # the candle's top and radius: burned low, the council has sat late


def _stick_shadow(s):
    """the candle's own shadow from the table's key light (top left): the stick laid down and to the right from its
    foot, tapering, the far end dithered out; and the dish's shadow beside the dish. Painted first, under the rest."""
    bx, by = CX, CY + 2
    dx, dy = 0.8, 0.6
    length, h0, h1 = 50.0, 10.0, 5.0
    for y in range(CH):
        for x in range(CW):
            px, py = x - bx, y - by
            along = px * dx + py * dy
            across = -px * dy + py * dx
            t = along / length
            a = 0
            if 0 <= along <= length and abs(across) <= h0 + (h1 - h0) * t:
                a = 96 if not (t > 0.78 and (x + y) % 2) else 0
            if ell_in(x, y, CX + 5, CY + 6, RX + 1, RY + 1):
                a = max(a, 104)
            if a:
                s.set(x, y, rgba(SH, a))


def _body(s):
    """the dish, the finger ring, the socket and the candle (adapted from sb_table.candle_big)"""
    cx, cy = CX, CY
    # occlusion where the dish meets the oak
    for y in range(cy - RY, CH):
        for x in range(CW):
            if ell_in(x, y, cx + 2, cy + 8, RX + 3, RY + 3) and not ell_in(x, y, cx, cy + 6, RX, RY):
                d = ((x - cx - 2) / (RX + 3.0)) ** 2 + ((y - cy - 8) / (RY + 3.0)) ** 2
                s.over(x, y, SH, 150 if d < 0.93 else 80)
    # the dish's front wall
    for y in range(cy, cy + RY + 7):
        for x in range(cx - RX, cx + RX + 1):
            if ell_in(x, y, cx, cy + 6, RX, RY) and not ell_in(x, y, cx, cy, RX, RY):
                t = (x - (cx - RX)) / (2.0 * RX)
                s.set(x, y, BRASS[3] if t < 0.06 else (BRASS[5] if t < 0.30 else (BRASS[4] if t < 0.58 else
                                                                                 (BRASS[3] if t < 0.86 else BRASS[2]))))
    for x in range(cx - RX, cx + RX + 1):
        for y in range(cy + RY + 7, cy, -1):
            if ell_in(x, y, cx, cy + 6, RX, RY):
                s.set(x, y, BRASS[1])
                break
    # the rim and the dished floor
    for y in range(cy - RY, cy + RY + 1):
        for x in range(cx - RX, cx + RX + 1):
            if not ell_in(x, y, cx, cy, RX, RY):
                continue
            if not ell_in(x, y, cx, cy, IRX, IRY):
                ang = math.atan2((y - cy) / RY, (x - cx) / RX)
                lit = -math.cos(ang + 2.3)
                s.set(x, y, SPEC if lit < -0.93 else (BRASS[7] if lit < -0.6 else (BRASS[6] if lit < 0.05 else
                                                                                  (BRASS[5] if lit < 0.6 else BRASS[4]))))
            else:
                v = (y - cy) / IRY
                u = (x - cx) / IRX
                c = BRASS[2] if v < -0.45 else (BRASS[3] if v < 0.15 else (BRASS[4] if v < 0.55 else
                                                                         (BRASS[5] if v < 0.8 else BRASS[6])))
                if v > 0.55 and u > 0.45:
                    c = BRASS[4]
                s.set(x, y, c)
    for y in range(cy - RY - 1, cy + RY + 2):
        for x in range(cx - RX - 1, cx + RX + 2):
            if ell_in(x, y, cx, cy, RX, RY) and not ell_in(x, y, cx, cy, RX - 1.2, RY - 0.9) and y < cy - 2:
                s.set(x, y, BRASS[6] if x < cx + 10 else BRASS[5])
            if ell_in(x, y, cx, cy, IRX + 0.9, IRY + 0.7) and not ell_in(x, y, cx, cy, IRX, IRY):
                s.set(x, y, BRASS[3] if y < cy else BRASS[7])
    # an engraved ring in the floor, broken where the socket stands
    pts = []
    for k in range(220):
        a = 2 * math.pi * k / 220
        p = (round(cx + 27 * math.cos(a)), round(cy + 8.4 * math.sin(a)))
        if p not in pts and not (abs(p[0] - cx) < 15 and p[1] < cy + 2):
            pts.append(p)
    engrave(s, pts)
    # the flame's reflection on the front inner wall, a glint on the back of the rim
    for (x, y, c) in ((cx - 12, cy + 8, SPEC), (cx - 11, cy + 8, '#ffe2a6'), (cx - 13, cy + 8, '#ffe2a6'),
                      (cx - 12, cy + 9, BRASS[7]), (cx + 5, cy + 9, BRASS[7]), (cx + 6, cy + 9, '#ffe2a6'),
                      (cx - 26, cy - 11, SPEC), (cx - 25, cy - 11, SPEC), (cx - 24, cy - 12, BRASS[7])):
        s.set(x, y, c)
    # the finger ring on the right, its neck joining the rim
    fx, fy = cx + RX + 6, cy - 3
    for y in range(fy - 9, fy + 10):
        for x in range(fx - 12, fx + 12):
            if ell_in(x, y, fx, fy, 10.5, 7.8) and not ell_in(x, y, fx, fy, 6.0, 4.0):
                ang = math.atan2((y - fy) / 7.8, (x - fx) / 10.5)
                lit = -math.cos(ang + 2.3)
                inner = ell_in(x, y, fx, fy, 7.6, 5.4)
                s.set(x, y, (BRASS[3] if y < fy else BRASS[6]) if inner else (BRASS[7] if lit < -0.5 else
                                                                             (BRASS[5] if lit < 0.3 else BRASS[3])))
            elif ell_in(x, y, fx + 2, fy + 3, 10.5, 7.8) and not ell_in(x, y, fx, fy, 10.5, 7.8) \
                    and not ell_in(x, y, fx + 2, fy + 3, 6.0, 4.0) and y > fy:
                s.set(x, y, BRASS[2])
    for y in range(fy - 3, fy + 4):
        for x in range(cx + RX - 3, fx - 8):
            s.set(x, y, BRASS[6] if y < fy - 1 else (BRASS[4] if y < fy + 2 else BRASS[2]))
    # the socket: a turned cup with a flared base and lip
    for y in range(cy - 6, cy + 3):
        for x in range(cx - 18, cx + 19):
            if ell_in(x, y, cx, cy - 1, 16, 5.0):
                t = (x - (cx - 16)) / 32.0
                s.set(x, y, BRASS[6] if t < 0.3 else (BRASS[5] if t < 0.55 else (BRASS[4] if t < 0.8 else BRASS[2])))
    for y in range(S0, cy - 1):
        for x in range(cx - SR, cx + SR + 1):
            t = (x - (cx - SR)) / (2.0 * SR)
            s.set(x, y, BRASS[4] if t < 0.1 else (BRASS[6] if t < 0.24 else (SPEC if t < 0.29 else (BRASS[6] if t < 0.4 else (
                BRASS[5] if t < 0.62 else (BRASS[4] if t < 0.82 else (BRASS[3] if t < 0.93 else BRASS[2])))))))
    for x in range(cx - SR, cx + SR + 1):
        for y in (S0 + 6, S0 + 12):
            s.set(x, y, BRASS[2] if x > cx else BRASS[3])
            s.set(x, y + 1, BRASS[6] if x < cx + 6 else BRASS[4])
    for y in range(S0 - 5, S0 + 4):
        for x in range(cx - 15, cx + 16):
            if ell_in(x, y, cx, S0, 14.5, 4.5):
                if ell_in(x, y, cx, S0, 10.4, 3.1):
                    s.set(x, y, BRASS[2])
                else:
                    ang = math.atan2((y - S0) / 4.5, (x - cx) / 14.5)
                    lit = -math.cos(ang + 2.3)
                    s.set(x, y, SPEC if lit < -0.85 else (BRASS[7] if lit < -0.3 else (BRASS[5] if lit < 0.5 else BRASS[4])))
    for x in range(cx - 14, cx + 15):
        for y in range(S0 + 4, S0 - 1, -1):
            if ell_in(x, y, cx, S0 + 0.6, 14.5, 4.5):
                s.set(x, y + 1, BRASS[2])
                break
    # the candle: wax lit on the left, glowing near the flame
    for y in range(C0, S0 + 1):
        for x in range(cx - CR, cx + CR + 1):
            t = (x - (cx - CR)) / (2.0 * CR)
            k = 6 if t < 0.1 else (8 if t < 0.32 else (7 if t < 0.56 else (6 if t < 0.8 else (5 if t < 0.93 else 4))))
            c = VEL[k]
            g = max(0.0, 1.0 - (y - C0) / 14.0)
            if g > 0:
                c = mix(c, '#ffe7b4', min(0.6, g * 0.75))
            s.set(x, y, c)
    for x in range(cx - CR, cx + CR + 1):
        for y in range(C0 - 4, C0 + 5):
            if ell_in(x, y, cx, C0, CR + 0.4, 3.3):
                s.set(x, y, VEL[8] if x < cx + 3 else VEL[7])
            if ell_in(x, y, cx, C0 + 0.3, CR - 2.5, 2.2):
                s.set(x, y, '#f6d69a' if x < cx + 2 else '#e9bf7d')
            if ell_in(x, y, cx, C0 + 0.6, CR - 5, 1.3):
                s.set(x, y, '#fbe3ad')
    # runs of wax down the side, each ending in a bead
    for (dx, ln, w2) in ((-7, 9, 2), (-3, 19, 2), (2, 6, 2), (5, 14, 2)):
        x0 = cx + dx
        for y in range(C0 + 1, C0 + ln):
            for i in range(w2):
                base = s.get(x0 + i, y)
                s.set(x0 + i, y, mix('#%02x%02x%02x' % base[:3], '#fff6df', 0.35) if i == 0 else '#%02x%02x%02x' % base[:3])
            s.set(x0 + w2, y, VEL[4] if dx > -5 else VEL[5])
        for (bx, by, c) in ((x0 - 1, C0 + ln, VEL[8]), (x0, C0 + ln, VEL[7]), (x0 + 1, C0 + ln, VEL[7]),
                            (x0 + 2, C0 + ln, VEL[5]), (x0, C0 + ln + 1, VEL[5]), (x0 + 1, C0 + ln + 1, VEL[4])):
            s.set(bx, by, c)
    # wax over the lip of the socket
    for (dx, ln) in ((-12, 5), (-8, 8), (8, 4), (12, 3)):
        for y in range(S0 - 1, S0 + ln):
            s.set(cx + dx, y, VEL[7] if dx < 0 else VEL[6])
            s.set(cx + dx + 1, y, VEL[6] if dx < 0 else VEL[5])
        s.set(cx + dx, S0 + ln, VEL[5]); s.set(cx + dx + 1, S0 + ln, VEL[4])
    for x in range(cx - 13, cx + 14):
        if abs(x - cx) > CR - 1:
            s.set(x, S0 - 1, VEL[7] if x < cx else VEL[6])
            s.set(x, S0, VEL[6] if x < cx else VEL[5])


# the flame's four frames: height, half width, the tip's lean (px at the top) and the core's share
FLAMES = [(27, 6.2, 0.0, 0.34), (29, 5.8, 1.2, 0.30), (25, 6.6, -1.0, 0.38), (28, 6.0, 0.6, 0.32)]


def _flame(s, frame):
    """an upright flame with a blue root and a white core, its tip swaying a pixel either way from frame to frame, a
    warm halo round it in two steps, the wick black in its foot"""
    FH, HW, lean, core = FLAMES[frame]
    bx, by = CX + 0.5, C0 - 3
    for (x, y) in ((CX, C0 - 1), (CX, C0 - 2)):
        s.set(x, y, INK[0])
    fl = {}
    for y in range(by - FH - 2, by + 3):
        sv = (by - y) / float(FH)
        if sv < -0.06 or sv > 1.0:
            continue
        sv2 = max(0.0, sv)
        xc = bx + lean * sv2 ** 1.6
        hw = HW * (sv2 / 0.26) ** 0.55 if sv2 < 0.26 else HW * ((1.0 - sv2) / 0.74) ** 0.95
        hw = max(hw, 1.2 if sv < 0 else 0.6)
        for x in range(int(xc - hw - 2), int(xc + hw + 3)):
            d = abs(x + 0.5 - xc) / hw
            if d > 1.0:
                continue
            if sv < 0.15 and d > 0.42:
                c = FLAME[0]
            elif sv < 0.62 and d < core:
                c = FLAME[4]
            elif d < 0.6:
                c = FLAME[3]
            elif d < 0.84:
                c = FLAME[2]
            else:
                c = FLAME[1]
            fl[(x, y)] = c
    halo = {}
    for (x, y) in fl:
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                q = (x + dx, y + dy)
                if q in fl:
                    continue
                r = max(abs(dx), abs(dy))
                halo[q] = max(halo.get(q, 0), 64 if r <= 2 else 30)
    for (x, y), a in sorted(halo.items()):
        s.over(x, y, '#ffcf7a', a)
    for (x, y), c in fl.items():
        s.set(x, y, c)
    s.set(CX, C0 - 2, INK[1])


def candle():
    """the chamberstick as a sheet of four frames, CW x CH each"""
    sheet = Spr(CW * 4, CH)
    base = Spr(CW, CH)
    _stick_shadow(base)
    _body(base)
    for f in range(4):
        s = Spr(CW, CH)
        s.blit(base, 0, 0)
        _flame(s, f)
        sheet.blit(s, CW * f, 0)
    return sheet


# where the flame stands in a frame, for the game: its light and halo are centred here
FLAME_AT = (CX, C0 - 16)


def glow(rx=190, ry=150, peak=0.40, step=0.03):
    """the candle's light on the oak: a warm translucent pool in stepped rings, dithered where one ring gives way to
    the next (sb_table.glow at the home page's size)"""
    s = Spr(2 * rx + 1, 2 * ry + 1)
    for y in range(2 * ry + 1):
        for x in range(2 * rx + 1):
            d = math.sqrt(((x - rx) / float(rx)) ** 2 + ((y - ry) / float(ry)) ** 2)
            if d > 1.0:
                continue
            a = (1.0 - d) ** 1.7 * peak
            lo = math.floor(a / step)
            f = a / step - lo
            q = lo + (1 if dith(x, y, f) else 0)
            if q > 0:
                s.set(x, y, rgba(WARM, round(q * step * 255)))
    return s


# ================================================================ the inkwell and its quill
IW, IH = 132, 152                     # one frame
ICX, ICY = 42, 128                    # the base's centre
BW, BH, BRY = 26, 26, 7.5             # half width, wall height, ellipse depth
QUILL = [((ICX + 82, 6), (ICX + 28, None)),      # at rest: the tip, the bow's control point (y from the mouth)
         ((ICX + 79, 8), (ICX + 27, None)),      # twitched: the tip dips and swings in
         ((ICX + 81, 7), (ICX + 28, None))]      # half back


def _jar(s):
    """the squat octagonal bottle of dark green glass (adapted from sb_table.inkwell_big): flat facets, lighter edges
    where they meet, a cold window streak, a thick base with a line of light, the ink black in the mouth"""
    cx, cy = ICX, ICY
    top = cy - BH
    for y in range(cy - 4, cy + 14):
        for x in range(cx - BW - 4, cx + BW + 6):
            if ell_in(x, y, cx + 2, cy + 3, BW + 3, BRY + 3) and not ell_in(x, y, cx, cy, BW, BRY):
                s.over(x, y, SH, 150)
    for y in range(cy - 2, cy + 18):                       # its shadow from the key light, down and right
        for x in range(cx - BW, cx + BW + 16):
            if ell_in(x, y, cx + 9, cy + 6, BW + 2, BRY + 2) and not s.get(x, y)[3] and (y < cy + 10 or (x + y) % 2):
                s.set(x, y, rgba(SH, 70))
    edges = [cx - BW, cx - 21, cx - 9, cx + 9, cx + 21, cx + BW]
    tones = [GLASS[4], GLASS[3], GLASS[2], GLASS[1], GLASS[0]]
    for y in range(top, cy + int(BRY) + 1):
        for x in range(cx - BW, cx + BW + 1):
            if not ((top <= y <= cy) or ell_in(x, y, cx, cy, BW, BRY)):
                continue
            f = max(i for i in range(5) if x >= edges[i])
            s.set(x, y, tones[f])
    for e in edges[1:-1]:
        for y in range(top + 2, cy + 5):
            if s.get(e, y)[3] == 255:
                s.set(e, y, GLASS[5] if e < cx else GLASS[3])
    for x in range(cx - BW, cx + BW + 1):
        for y in range(cy + int(BRY) + 1, cy - 2, -1):
            if ell_in(x, y, cx, cy, BW, BRY):
                s.set(x, y, GLASS[0]); s.set(x, y - 1, GLASS[1])
                s.set(x, y - 3, GLASS[5] if x < cx + 6 else GLASS[3])
                break
    for y in range(top - int(BRY) - 2, top + int(BRY) + 1):
        for x in range(cx - BW, cx + BW + 1):
            if ell_in(x, y, cx, top, BW, BRY):
                u = (x - cx) / float(BW); v = (y - top) / BRY
                k = u * 0.8 + v * 0.6
                s.set(x, y, GLASS[6] if k < -0.85 else (GLASS[5] if k < -0.35 else (GLASS[4] if k < 0.2 else
                                                                                     (GLASS[3] if k < 0.6 else GLASS[2]))))
    ny = top - 9
    for y in range(ny, top - 1):
        for x in range(cx - 11, cx + 12):
            t = (x - (cx - 11)) / 22.0
            s.set(x, y, GLASS[5] if t < 0.2 else (GLASS[4] if t < 0.5 else (GLASS[3] if t < 0.8 else GLASS[1])))
    for y in range(ny - 5, ny + 5):
        for x in range(cx - 13, cx + 14):
            if ell_in(x, y, cx, ny, 12, 4.0):
                if ell_in(x, y, cx, ny + 0.4, 8.6, 2.7):
                    s.set(x, y, '#07060a')
                else:
                    u = (x - cx) / 12.0
                    s.set(x, y, GLASS[6] if (u < -0.4 and y <= ny) else (GLASS[5] if y <= ny else GLASS[3]))
    for (x, y, c) in ((cx - 4, ny - 1, '#3c4760'), (cx - 3, ny - 1, '#2a3244'), (cx + 5, ny + 1, '#1b2030')):
        s.set(x, y, c)
    for y in range(top + 6, cy - 6):
        s.set(cx - 18, y, GLASS[7] if y < top + 13 else GLASS[6])
        if y < top + 10:
            s.set(cx - 17, y, GLASS[6])
    # old drops of ink on the oak, each with a wet glint
    DROP = {3: ['.aaa.', 'aagaa', 'aaaaa', '.aaa.'], 2: ['.aa.', 'agaa', '.aa.'], 1: ['aa', 'ga']}
    for (x, y, r) in ((cx + 36, cy + 7, 3), (cx + 43, cy + 12, 1), (cx - 34, cy + 11, 2), (cx + 51, cy + 3, 1)):
        s.grid(x, y, DROP[r], {'a': '#120d0c', 'g': '#4a4a5a'})
    return ny


def _quill(s, ny, frame):
    """a goose feather standing in the ink and leaning out to the right: the shaft bowed, the vane broad and lit on
    the upper left, narrow and shaded on the lower right, split in two places, its barbs ticked; the bare quill amber
    where it goes into the ink (adapted from sb_table.inkwell_big)"""
    (tip, (ctrlx, _)) = QUILL[frame]
    p0, p1, ctrl = (ICX + 2, ny + 1), tip, (ctrlx, ny - 60)

    def bez(t):
        return ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * ctrl[0] + t * t * p1[0],
                (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * ctrl[1] + t * t * p1[1])
    N = 260
    pts = [bez(k / float(N)) for k in range(N + 1)]
    rnd = random.Random(17)
    barbs = []
    for k in range(int(N * 0.24), N + 1, 2):
        t = k / float(N)
        x, y = pts[k]
        xa, ya = pts[max(0, k - 3)]
        xb, yb = pts[min(N, k + 3)]
        dx, dy = xb - xa, yb - ya
        L = math.hypot(dx, dy) or 1
        dx, dy = dx / L, dy / L
        nx, ny_ = dy, -dx
        u = max(0.0, min(1.0, (t - 0.24) / 0.76))
        prof_l = 11.5 * math.sin(math.pi * min(1.0, u * 1.04) ** 0.72) ** 0.75
        prof_r = 5.5 * math.sin(math.pi * min(1.0, u * 1.02) ** 0.85) ** 0.9
        for side, prof in ((-1, prof_r), (1, prof_l)):
            ln = prof * (1.0 + rnd.uniform(-0.12, 0.06))
            ang = math.radians(38 if side == 1 else 44)
            bx_ = side * nx * math.cos(ang) + dx * math.sin(ang)
            by_ = side * ny_ * math.cos(ang) + dy * math.sin(ang)
            barbs.append((k, side, x, y, bx_, by_, ln, u))
    for (k, side, x, y, bx_, by_, ln, u) in barbs:
        if side == 1 and ((0.40 < u < 0.43) or (0.66 < u < 0.685)):
            ln = min(ln, 2.5)
        grp = (k // 2) % 4 == 0
        n = int(round(ln))
        for j in range(1, n + 1):
            X, Y = round(x + bx_ * j), round(y + by_ * j)
            if side == 1:
                c = VEL[8] if j == n else (VEL[6] if grp else (VEL[7] if j < n - 2 else VEL[8]))
                if j <= 1:
                    c = VEL[6]
            else:
                c = VEL[5] if j == n else (VEL[4] if grp else VEL[5])
                if j <= 1:
                    c = VEL[4]
            s.set(X, Y, c)
    for k in range(N + 1):
        t = k / float(N)
        x, y = pts[k]
        X, Y = round(x), round(y)
        if t < 0.24:
            s.set(X, Y, '#d8bf86' if t > 0.05 else '#9c8352')
            s.set(X + 1, Y, '#a8915e' if t > 0.05 else '#6d5b3a')
        else:
            s.set(X, Y, VEL[8] if t < 0.9 else VEL[7])
            s.set(X + 1, Y + 1, VEL[3])
    for (x, y, c) in ((p1[0] + 1, p1[1] - 1, VEL[7]), (p1[0] + 2, p1[1] - 1, VEL[6]), (p1[0] + 3, p1[1], VEL[5])):
        s.set(x, y, c)


def inkwell():
    """the inkwell and its quill as a sheet of three frames, IW x IH each: at rest, the tip twitched, half back"""
    sheet = Spr(IW * 3, IH)
    for f in range(3):
        s = Spr(IW, IH)
        ny = _jar(s)
        _quill(s, ny, f)
        sheet.blit(s, IW * f, 0)
    return sheet


# ================================================================ the Ride plate's glint
def plate_glint():
    """a streak of light leaning to the right, a broad band and a thin one beside it, in the brass's warm specular;
    the game sweeps it once across the plate's face, clipped to the face"""
    W, H = 22, 36
    s = Spr(W, H)
    for y in range(H):
        x0 = round((H - 1 - y) * 0.42)
        fade = 1.0 - abs(y - H / 2.0) / (H / 2.0 + 2)
        for i in range(5):
            s.set(x0 + i, y, rgba(SPEC, round((150 if 0 < i < 4 else 96) * fade)))
        for i in range(2):
            s.set(x0 + 8 + i, y, rgba(SPEC, round(84 * fade)))
    return s
