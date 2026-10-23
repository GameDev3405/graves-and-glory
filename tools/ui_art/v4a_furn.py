"""v4 War Table furniture at full density: the back rail, the brass nav plaques, the Ride and Enlist plates,
key caps, the parchment tags, the wax count badge, the portrait frames and their backdrops, and the quiet
things that give the empty oak a reason (a candle, an inkwell and quill, coins, a map slid under the rail).
Brass gets six tones plus a warm specular: an outline, a lit outer bevel, a brushed face with wear where
hands rub, an engraved line (the cut dark, its far wall lit), slotted screw heads turned every which way,
and tarnish gathering in the corners. Overrides the v4a_table sprites of the same name."""
import math, random
from v4lib import Spr, Noise, dith, rgba, mix
from v4pal import *

ASSETS = {}
SPEC = '#fff0c4'                      # the warm specular on brass
TARN = ['#3a3220', '#4d4529', '#5d5a3a']   # tarnish and a hint of verdigris, darkest first
SH = '#0a0505'


def screw(S, x, y, slot='-', metal=None):
    """a 5 x 5 domed slotted screw head sitting in its countersink, lit from the top left"""
    m = metal or BRASS
    rows = ['.abb.',
            'abbcc',
            'bbccd',
            'bccdd',
            '.cdd.']
    S.grid(x, y, rows, {'a': m[7], 'b': m[6], 'c': m[4], 'd': m[2]})
    for (dx, dy) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        S.set(x + dx, y + dy, m[1])
    cut = {'-': ((1, 2), (2, 2), (3, 2)), '|': ((2, 1), (2, 2), (2, 3)),
           '/': ((3, 1), (2, 2), (1, 3)), 'x': ((1, 1), (2, 2), (3, 3))}[slot]
    for (dx, dy) in cut:
        S.set(x + dx, y + dy, m[0])


def engrave(S, pts, m=None, lit=True):
    """an engraved line: each cut pixel dark; the pixel to its lower right, when not cut, lit"""
    m = m or BRASS
    ps = set(pts)
    for (x, y) in ps:
        S.set(x, y, m[2])
    if lit:
        for (x, y) in ps:
            for (dx, dy) in ((1, 1), (0, 1), (1, 0)):
                if (x + dx, y + dy) not in ps:
                    S.set(x + dx, y + dy, m[7] if (dx, dy) == (1, 1) else m[6])
                    break


def arc(cx, cy, r, a0, a1, n=None):
    n = n or max(8, int(abs(a1 - a0) * r * 1.6))
    out = []
    for k in range(n + 1):
        a = a0 + (a1 - a0) * k / n
        p = (round(cx + r * math.cos(a)), round(cy + r * math.sin(a)))
        if not out or out[-1] != p:
            out.append(p)
    return out


def brushed(S, x0, y0, x1, y1, rnd, density=1.0, keep=None, m=None):
    """long faint horizontal brushing on a brass face; keep = (ya, yb) rows left clean for text"""
    m = m or BRASS
    w, h = x1 - x0, y1 - y0
    for _ in range(int(w * h / 60 * density)):
        y = rnd.randint(y0, y1 - 1)
        if keep and keep[0] <= y <= keep[1]:
            continue
        xa = rnd.randint(x0, max(x0, x1 - 6)); ln = rnd.randint(4, 26)
        t = rnd.choice([m[6], m[6], m[4], m[4], m[4]])
        for x in range(xa, min(x1, xa + ln)):
            if rnd.random() < 0.85:
                S.set(x, y, t)


def tarnish(S, cx, cy, r, rnd, n=8, keep=None):
    for _ in range(n):
        X = cx + round(rnd.gauss(0, r)); Y = cy + round(rnd.gauss(0, r * 0.7))
        if keep and keep(X, Y):
            continue
        if S.get(X, Y)[3] == 255:
            S.set(X, Y, TARN[rnd.randint(0, 2)] if rnd.random() < 0.3 else BRASS[3])


# ================================================================ nav plaques on the rail
def plaque(w, h=28, seed=1):
    """a brass-framed oak plaque screwed to the rail: a 4 px frame (outline, lit outer bevel, brushed face,
    inner bevel falling into the panel), a slotted screw at each corner turned its own way, tarnish by the
    screws, wear on the top edge; the recessed oak panel with fine grain and ray flecks"""
    rnd = random.Random(seed * 31 + w)
    s = Spr(w + 1, h + 2)
    for x in range(1, w + 1):
        s.set(x, h, rgba(SH, 150)); s.set(x, h + 1, rgba(SH, 70))
    for y in range(1, h):
        s.set(w, y, rgba(SH, 120))
    s.rect(0, 0, w, h, BRASS[0])
    s.rect(1, 1, w - 2, h - 2, BRASS[5])
    brushed(s, 2, 2, w - 2, 4, rnd, 2.0)
    brushed(s, 2, h - 4, w - 2, h - 2, rnd, 2.0)
    # outer bevel: lit top and left with specular glints, the bottom and right turned away
    s.hline(1, w - 2, 1, BRASS[6]); s.vline(1, 1, h - 2, BRASS[6])
    s.hline(1, w - 2, h - 2, BRASS[3]); s.vline(w - 2, 1, h - 2, BRASS[3])
    s.set(w - 2, 1, BRASS[5]); s.set(1, h - 2, BRASS[4])
    for x in range(3, w - 3):
        if rnd.random() < 0.12:
            s.set(x, 1, BRASS[7])
    for k in range(6):
        s.set(2 + k, 1, BRASS[7] if k < 4 else BRASS[6])
    s.set(1, 2, BRASS[7]); s.set(1, 3, BRASS[7])
    # the brushed face (rows 2 and h-3), worn bright along the top where thumbs press
    for x in range(2, w - 2):
        if s.get(x, 2)[:3] == rgba(BRASS[5])[:3] and rnd.random() < 0.25:
            s.set(x, 2, BRASS[6])
    # inner bevel into the panel: its top and left face away (dark), its bottom and right are lit
    s.hline(3, w - 4, 3, BRASS[3]); s.vline(3, 3, h - 4, BRASS[3])
    s.hline(3, w - 4, h - 4, BRASS[6]); s.vline(w - 4, 3, h - 4, BRASS[6])
    s.set(3, 3, BRASS[2]); s.set(w - 4, h - 4, BRASS[7])
    # the oak panel
    for y in range(4, h - 4):
        for x in range(4, w - 4):
            s.set(x, y, OAK[2])
    for k in range(int((h - 8) / 2.0) + 1):
        yb = 4.6 + k * 2.0 + rnd.random() * 0.8
        amp = rnd.uniform(0.2, 0.7); L = rnd.uniform(50, 130); ph = rnd.uniform(0, 6.28)
        tone = rnd.choice([1, 1, 3, 3, 3])
        x = rnd.randint(-20, 4); on = True
        while x < w:
            run = rnd.randint(10, 50) if on else rnd.randint(3, 14)
            if on:
                for xx in range(max(5, x), min(w - 5, x + run)):
                    Y = round(yb + amp * math.sin(6.283 * xx / L + ph))
                    if 5 <= Y <= h - 6:
                        s.set(xx, Y, OAK[tone])
            on = not on; x += run
    for _ in range(int(w / 14)):
        # ray flecks: short pale flakes across the grain
        x = rnd.randint(6, w - 9); y = rnd.randint(6, h - 7)
        for i in range(rnd.randint(2, 3)):
            s.set(x + i, y, OAK[4] if i else OAK[3])
    s.hline(4, w - 5, 4, OAK[0]); s.vline(4, 4, h - 5, OAK[0])
    s.hline(5, w - 5, 5, OAK[1]); s.vline(5, 5, h - 6, OAK[1])
    s.hline(5, w - 5, h - 5, OAK[3])
    # the screws, each slot turned its own way, with grime gathered round them
    slots = rnd.sample(['-', '|', '/', 'x'], 4)
    for i, (cx, cy) in enumerate(((1, 1), (w - 6, 1), (1, h - 6), (w - 6, h - 6))):
        tarnish(s, cx + 2, cy + 2, 2.2, rnd, 5, keep=lambda X, Y: 4 <= X < w - 4 and 4 <= Y < h - 4)
        screw(s, cx, cy, slots[i])
    return s


for _w in (80, 88, 96, 104, 112, 120):
    ASSETS['plaque-%d' % _w] = (lambda w: (lambda: plaque(w)))(_w)


# ================================================================ the back rail
def rail(W=960, H=66, seed=3):
    """the table back rail: quarter-sawn oak (fine straight grain in five tones, the pale ray flecks
    quarter-sawn oak shows, a lit top arris), an ogee moulding (bead, fillet, cove, lip), a brass inlay
    engraved with a twisted cable, rosette studs every 48 px, and the stepped shadow it casts on the table"""
    rnd = random.Random(seed)
    s = Spr(W, H)
    body = 52
    idx = [[3] * W for _ in range(body)]
    for k in range(22):
        yb = 3 + k * 1.75 + rnd.random() * 0.6
        amp = rnd.uniform(0.15, 0.6); L = rnd.uniform(180, 380); ph = rnd.uniform(0, 6.28)
        tone = rnd.choices([2, 4, 1, 5], [52, 30, 12, 6])[0]
        x = rnd.randint(-60, 0); on = True
        while x < W:
            run = rnd.randint(50, 220) if on else rnd.randint(8, 40)
            if on:
                for xx in range(max(0, x), min(W, x + run)):
                    Y = round(yb + amp * math.sin(6.283 * xx / L + ph))
                    if 3 <= Y <= 40:
                        idx[Y][xx] = tone
            on = not on; x += run
    # ray flecks: pale flakes lying across the straight grain, each with a darker lower edge
    for _ in range(170):
        x = rnd.randint(0, W - 8); y = rnd.randint(5, 38); ln = rnd.randint(2, 6)
        for i in range(ln):
            if 0 <= x + i < W:
                idx[y][x + i] = 5 if i not in (0, ln - 1) else 4
                if y + 1 <= 40 and rnd.random() < 0.5:
                    idx[y + 1][x + i] = 2
    for xx in range(W):
        idx[0][xx] = 1                      # the back edge
        idx[1][xx] = 5 if (xx * 7) % 41 > 3 else 6   # the lit top arris, worn brighter in places
        idx[2][xx] = 4
        idx[41][xx] = 2
        idx[42][xx] = 5                     # the bead
        idx[43][xx] = 6 if (xx % 37) > 2 and (xx * 11) % 23 > 2 else 5
        idx[44][xx] = 3                     # fillet
        idx[45][xx] = 1                     # the cove, deep in shadow
        idx[46][xx] = 0
        idx[47][xx] = 4                     # the lip below catches the light
        for yy in range(48, body):
            idx[yy][xx] = 2 if yy < 50 else 1
        if (xx * 13) % 29 < 3:
            idx[48][xx] = 1
        if (xx * 7) % 23 < 2:
            idx[50][xx] = 0
    for yy in range(body):
        for xx in range(W):
            s.set(xx, yy, OAK[idx[yy][xx]])
    # the brass inlay: a lit top edge, three rows of twisted cable, a dark lower edge
    cable = [BRASS[7], BRASS[5], BRASS[4], BRASS[2]]
    for xx in range(W):
        s.set(xx, body, BRASS[6])
        for r in range(3):
            s.set(xx, body + 1 + r, cable[(xx + r) % 4])
        s.set(xx, body + 4, BRASS[1])
    for xx in range(0, W, 5):
        if rnd.random() < 0.3:
            s.set(xx, body, BRASS[7])
    # rosette studs every 48 px: a domed boss with four petals round it
    ROS = ['..hH..',
           '.hHmd.',
           'hHSmmd',
           'Hmmmdd',
           '.mmdd.',
           '..dd..']
    for cx in range(24, W, 48):
        s.grid(cx - 3, body - 1, ROS, {'h': BRASS[6], 'H': BRASS[7], 'S': SPEC, 'm': BRASS[4], 'd': BRASS[2]})
        s.set(cx - 4, body + 1, BRASS[1]); s.set(cx + 3, body + 1, BRASS[1])
    # the cast shadow on the table
    for i, a in enumerate((0.62, 0.48, 0.34, 0.22, 0.13, 0.06)):
        for xx in range(W):
            s.set(xx, body + 5 + i, rgba(SH, round(a * 255)))
    return s


ASSETS['rail'] = rail


# ================================================================ the primary action plate
def brass_button(w, h, seed=2):
    """the primary action: a brushed brass plate. A two-step outer bevel lit top and left; a slotted screw
    in each corner; an engraved border that sweeps round each screw in a quarter circle; brushing kept off
    the text band; the edges rubbed bright on top, grime gathered in the lower corners; a shadow on the table"""
    rnd = random.Random(seed + w * 3 + h)
    s = Spr(w + 2, h + 3)
    for x in range(2, w + 2):
        for i, a in enumerate((0.58, 0.36, 0.17)):
            s.set(x, h + i, rgba(SH, round(a * 255)))
    for y in range(2, h):
        s.set(w, y, rgba(SH, 130)); s.set(w + 1, y, rgba(SH, 60))
    s.rect(0, 0, w, h, BRASS[0])
    s.rect(1, 1, w - 2, h - 2, BRASS[5])
    mid = (h // 2 - 7, h // 2 + 7)
    brushed(s, 3, 3, w - 3, h - 3, rnd, 1.4, keep=mid)
    # two-step outer bevel
    s.hline(1, w - 2, 1, BRASS[7]); s.vline(1, 1, h - 2, BRASS[6])
    s.hline(2, w - 3, 2, BRASS[6]); s.vline(2, 2, h - 3, BRASS[6])
    s.hline(1, w - 2, h - 2, BRASS[2]); s.vline(w - 2, 1, h - 2, BRASS[2])
    s.hline(2, w - 3, h - 3, BRASS[3]); s.vline(w - 3, 2, h - 3, BRASS[3])
    for x in range(4, w - 4):
        if rnd.random() < 0.18:
            s.set(x, 1, SPEC)
        if rnd.random() < 0.10:
            s.set(x, h - 2, BRASS[1])
    for k in range(10):
        s.set(2 + k, 1, SPEC if k < 6 else BRASS[7])
    # the engraved border: straight runs between the screws, a quarter circle round each screw
    e = 6
    pts = [(x, e) for x in range(e + 6, w - e - 6)] + [(x, h - 1 - e) for x in range(e + 6, w - e - 6)]
    pts += [(e, y) for y in range(e + 6, h - e - 6)] + [(w - 1 - e, y) for y in range(e + 6, h - e - 6)]
    for (cx, cy, a0) in ((e, e, 0.0), (w - 1 - e, e, math.pi / 2), (w - 1 - e, h - 1 - e, math.pi),
                         (e, h - 1 - e, 3 * math.pi / 2)):
        pts += arc(cx, cy, 6, a0, a0 + math.pi / 2)
    engrave(s, pts)
    # a second, finer line inside on the long sides only, with punched dots between
    for x in range(e + 10, w - e - 10):
        if x % 3 == 0:
            s.set(x, e + 3, BRASS[3]); s.set(x, h - 1 - e - 3, BRASS[3])
    # grime in the lower corners, wear on the upper edge
    tarnish(s, 10, h - 8, 3.0, rnd, 10)
    tarnish(s, w - 10, h - 8, 3.0, rnd, 10)
    slots = ['/', '-', 'x', '|']
    for i, (cx, cy) in enumerate(((e - 2, e - 2), (w - e - 3, e - 2), (e - 2, h - e - 3), (w - e - 3, h - e - 3))):
        screw(s, cx, cy, slots[i])
    return s


ASSETS['btn-brass-240x40'] = lambda: brass_button(240, 40)


def key_engraved(w, h=14):
    """a key cap cut into brass: a rounded outline, the cut dark on top and left, its far wall lit below and
    right; the face inside left clear so the cap works on oak and on brass"""
    s = Spr(w, h)
    for x in range(1, w - 1):
        s.set(x, 0, BRASS[2]); s.set(x, h - 1, BRASS[2])
    for y in range(1, h - 1):
        s.set(0, y, BRASS[2]); s.set(w - 1, y, BRASS[2])
    for x in range(1, w - 1):
        s.set(x, h - 2, BRASS[6])
    for y in range(1, h - 1):
        s.set(w - 2, y, BRASS[6])
    for x in range(2, w - 2):
        s.set(x, 1, BRASS[4])
    s.set(w - 2, 1, BRASS[5]); s.set(1, h - 2, BRASS[5]); s.set(w - 2, h - 2, BRASS[7])
    return s


for _w in range(13, 49):
    ASSETS['key-brass-%d' % _w] = (lambda w: (lambda: key_engraved(w)))(_w)


def vellum_button(w, h=20, seed=4):
    """the secondary action: a cut parchment tag raised off the sheet; fibres in two tones, a ruled ink
    border with corners left open, foxing at one end, the lit top-left arris and the shaded lower edge"""
    rnd = random.Random(seed + w)
    s = Spr(w + 1, h + 2)
    for x in range(1, w + 1):
        s.set(x, h, rgba('#2a1d10', 120)); s.set(x, h + 1, rgba('#2a1d10', 50))
    for y in range(1, h):
        s.set(w, y, rgba('#2a1d10', 90))
    s.rect(0, 0, w, h, VEL[1])
    s.rect(1, 1, w - 2, h - 2, VEL[7])
    nz = Noise(seed + w, 6)
    for y in range(2, h - 2):
        for x in range(2, w - 2):
            v = nz(x, y)
            if v > 0.72:
                s.set(x, y, VEL[6])
            elif v < 0.16:
                s.set(x, y, VEL[8])
    for _ in range(int(w * h / 40)):
        x = rnd.randint(3, w - 6); y = rnd.randint(3, h - 4); ln = rnd.randint(2, 4)
        c = VEL[6] if rnd.random() < 0.6 else VEL[8]
        for i in range(ln):
            s.set(x + i, y, c)
    s.hline(1, w - 2, 1, VEL[8]); s.vline(1, 1, h - 2, VEL[8])
    s.hline(1, w - 2, h - 2, VEL[4]); s.vline(w - 2, 2, h - 2, VEL[4])
    s.hline(2, w - 3, h - 3, VEL[5])
    # the ruled border, broken at the corners
    for x in range(5, w - 5):
        if x % 7 != 3:
            s.set(x, 3, VEL[4])
    for x in range(5, w - 5):
        s.set(x, h - 4, VEL[4])
    # foxing at the right end
    for _ in range(4):
        X = w - 6 + round(rnd.gauss(0, 1.5)); Y = rnd.randint(5, h - 6)
        s.set(X, Y, VEL[4] if rnd.random() < 0.5 else VEL[5])
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        s.set(x, y, None)
    s.set(1, 1, VEL[6])
    return s


for _w in (64, 72, 80, 88):
    ASSETS['btn-vellum-%dx24' % _w] = (lambda w: (lambda: vellum_button(w, 24)))(_w)


def wax_badge():
    """a drop of red wax with a raised lip and a flat field for one digit; a glint on the lip"""
    rows = ['...kkkkk...',
            '..kGHhhfk..',
            '.kHhffffsk.',
            'kHhfffffssk',
            'khffffffssk',
            'kffffffffsk',
            'kffffffffsk',
            'kfffffffssk',
            '.ksffffssk.',
            '..kssssdk..',
            '...kkdkk...']
    s = Spr(11, 12)
    s.grid(0, 0, rows, {'k': WAX[0], 'G': '#f4c2a8', 'H': WAX[6], 'h': WAX[5], 'f': WAX[3], 's': WAX[2],
                        'd': WAX[1]})
    s.set(5, 11, WAX[1]); s.set(6, 11, rgba(SH, 90))
    return s


ASSETS['wax-badge'] = wax_badge


# ================================================================ portrait frames for the live model render
def frame_rim(size, seed=7):
    """a brass bezel round a portrait window (size - 8 square, clear here; the game draws the unit there with
    drawEntity under a scissor, and the rim overlaps the window by one pixel so the scissored edge never
    shows). Outline, lit outer bevel with glints, a beaded face, the inner bevel falling to the window,
    a domed boss at each corner, tarnish by the bosses"""
    rnd = random.Random(seed + size)
    n = size
    s = Spr(n + 1, n + 2)
    for x in range(1, n + 1):
        s.set(x, n, rgba(SH, 140)); s.set(x, n + 1, rgba(SH, 60))
    for y in range(1, n):
        s.set(n, y, rgba(SH, 110))
    s.frame(0, 0, n, n, BRASS[0])
    s.hline(1, n - 2, 1, BRASS[6]); s.vline(1, 1, n - 2, BRASS[6])
    s.hline(1, n - 2, n - 2, BRASS[3]); s.vline(n - 2, 1, n - 2, BRASS[3])
    for k in range(2, n - 2):
        if rnd.random() < 0.15:
            s.set(k, 1, BRASS[7])
        if rnd.random() < 0.15:
            s.set(1, k, BRASS[7])
    # the beaded face: alternate light and dark beads, lit on the top and left runs, dimmer on the others
    for k in range(2, n - 2):
        top = BRASS[6] if k % 2 == 0 else BRASS[4]
        bot = BRASS[5] if k % 2 == 0 else BRASS[3]
        s.set(k, 2, top); s.set(2, k, top)
        s.set(k, n - 3, bot); s.set(n - 3, k, bot)
    # inner bevel: slopes toward the window, so its top and left are dark and its bottom and right lit
    s.hline(3, n - 4, 3, BRASS[2]); s.vline(3, 3, n - 4, BRASS[2])
    s.hline(3, n - 4, n - 4, BRASS[6]); s.vline(n - 4, 3, n - 4, BRASS[6])
    s.frame(4, 4, n - 8, n - 8, BRASS[0])
    s.set(1, 1, SPEC); s.set(2, 1, BRASS[7]); s.set(1, 2, BRASS[7])
    BOSS = ['.hHm..',
            'hHSmd.',
            'HSmmdd',
            'mmmdd.',
            '.ddd..',
            '......']
    for (cx, cy) in ((0, 0), (n - 5, 0), (0, n - 5), (n - 5, n - 5)):
        tarnish(s, cx + 2, cy + 2, 2.0, rnd, 4, keep=lambda X, Y: 4 <= X < n - 4 and 4 <= Y < n - 4)
        s.grid(cx, cy, BOSS, {'h': BRASS[6], 'H': BRASS[7], 'S': SPEC, 'm': BRASS[4], 'd': BRASS[2]})
    return s


def frame_back(size, empty=False, seed=5):
    """the backdrop behind the live render: deep slate, a soft light behind the head (upper middle) in six
    steps with ragged edges so it never reads as rings, darker toward the shoulders and corners"""
    w = size - 8
    s = Spr(w, w)
    nz = Noise(seed + size, 5); nz2 = Noise(seed + size + 1, 2)
    tones = ['#11171a', '#151c20', '#1a2226', '#1f292d', '#253135', '#2c393e']
    if empty:
        tones = ['#100b09', '#140e0b', '#18110d', '#1c140f', '#201711', '#251b13']
    for y in range(w):
        for x in range(w):
            dx = (x - (w - 1) / 2) / (w * 0.55); dy = (y - w * 0.34) / (w * 0.62)
            r = math.sqrt(dx * dx + dy * dy)
            v = (1.0 - r) * 4.6 + (nz(x, y) - 0.5) * 1.0 + (nz2(x, y) - 0.5) * 0.35
            t = max(0, min(5, int(v + 0.4)))
            s.set(x, y, tones[t])
    return s


for _sz in (40, 44, 48, 168):
    ASSETS['frame-rim-%d' % _sz] = (lambda z: (lambda: frame_rim(z)))(_sz)
    ASSETS['frame-back-%d' % _sz] = (lambda z: (lambda: frame_back(z)))(_sz)
ASSETS['frame-back-empty-44'] = lambda: frame_back(44, empty=True)
ASSETS['frame-back-empty-48'] = lambda: frame_back(48, empty=True)
ASSETS['frame-back-160'] = lambda: frame_back(168, seed=9)


def invite_plus():
    """an engraved brass plus for the empty party seat: the cut dark, its far walls lit"""
    s = Spr(9, 9)
    pts = [(4, y) for y in range(0, 8)] + [(x, 4) for x in range(0, 8)]
    for (x, y) in pts:
        s.set(x, y, BRASS[5])
    for (x, y) in pts:
        if (x + 1, y) not in pts:
            s.set(x + 1, y, BRASS[2])
        if (x, y + 1) not in pts:
            s.set(x, y + 1, BRASS[2])
    s.set(4, 0, BRASS[7]); s.set(0, 4, BRASS[7]); s.set(4, 3, BRASS[6]); s.set(3, 4, BRASS[6])
    return s


ASSETS['invite-plus'] = invite_plus


# ================================================================ the quiet things on the table
FLAME = ['#4a6fb5', '#e0802c', '#f2a93f', '#ffd27a', '#fff4d6']


def ell_in(x, y, cx, cy, rx, ry):
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0


def candle():
    """a beeswax candle in a brass chamberstick, seen from above and in front like everything on the table:
    the dish with its lit back rim and shaded front wall, a finger ring, the socket, the candle with wax
    run down its side and pooled in the socket, a small flame"""
    W, H = 44, 54
    s = Spr(W, H)
    cx, cy = 19, 44
    # the shadow it casts away from the window light (down and right)
    for y in range(H):
        for x in range(W):
            if ell_in(x, y, cx + 3, cy + 3, 18, 7.5):
                s.set(x, y, rgba(SH, 110))
    # the dish: its front wall, then the rim, then the dished floor
    for y in range(H):
        for x in range(W):
            if ell_in(x, y, cx, cy + 2, 17, 6.5) and not ell_in(x, y, cx, cy, 17, 6.5):
                s.set(x, y, BRASS[3] if x < cx + 6 else BRASS[2])
            if ell_in(x, y, cx, cy, 17, 6.5):
                ang = math.atan2((y - cy) / 6.5, (x - cx) / 17)
                outer = not ell_in(x, y, cx, cy, 15, 5.2)
                if outer:
                    # the rim: lit at the back left, turned away at the front right
                    lit = -math.cos(ang - (-2.4))
                    s.set(x, y, SPEC if lit < -0.92 else (BRASS[7] if lit < -0.55 else (BRASS[6] if lit < 0.1 else
                          (BRASS[5] if lit < 0.6 else BRASS[4]))))
                else:
                    # the dished floor: dark at the back (the rim shades it), lit on the front inner wall
                    v = (y - cy) / 5.2
                    s.set(x, y, BRASS[2] if v < -0.45 else (BRASS[3] if v < 0.2 else (BRASS[4] if v < 0.6 else BRASS[6])))
    for x in range(W):
        for y in range(H):
            if ell_in(x, y, cx, cy + 1, 17.4, 7.4) and not ell_in(x, y, cx, cy + 1, 16.6, 6.6) and y > cy + 3:
                s.set(x, y, BRASS[1])
    # the finger ring on the right
    for y in range(H):
        for x in range(W):
            if ell_in(x, y, 37, cy - 1, 4.6, 3.4) and not ell_in(x, y, 37, cy - 1, 2.6, 1.6):
                s.set(x, y, BRASS[6] if y < cy - 1 else BRASS[3])
    # the socket: a short brass cylinder
    for y in range(cy - 6, cy + 1):
        for x in range(cx - 5, cx + 6):
            t = (x - (cx - 5)) / 10.0
            s.set(x, y, BRASS[6] if t < 0.25 else (BRASS[5] if t < 0.5 else (BRASS[4] if t < 0.8 else BRASS[2])))
    for x in range(cx - 5, cx + 6):
        s.set(x, cy - 7, BRASS[7] if x < cx else BRASS[5])
        s.set(x, cy + 1, BRASS[2])
    # the candle
    top, bot = 12, cy - 7
    for y in range(top, bot + 1):
        for x in range(cx - 3, cx + 4):
            t = (x - (cx - 3)) / 6.0
            s.set(x, y, VEL[8] if t < 0.17 else (VEL[7] if t < 0.5 else (VEL[6] if t < 0.84 else VEL[4])))
    for x in range(cx - 3, cx + 4):
        s.set(x, top, VEL[6]); s.set(x, top - 1, VEL[5] if abs(x - cx) < 3 else None)
    s.set(cx, top, INK[1]); s.set(cx, top - 1, VEL[4])
    # wax run down the side and pooled in the socket
    for (x, y0, ln) in ((cx - 2, top + 1, 9), (cx + 2, top + 1, 14), (cx + 3, top + 2, 5)):
        for y in range(y0, y0 + ln):
            s.set(x, y, VEL[8] if x < cx else VEL[7])
        s.set(x, y0 + ln, VEL[6])
    for x in range(cx - 6, cx + 7):
        if abs(x - cx) > 3:
            s.set(x, cy - 7, VEL[7] if x < cx else VEL[6])
    s.set(cx + 6, cy - 6, VEL[6]); s.set(cx - 6, cy - 6, VEL[7])
    # the wick and the flame
    s.set(cx, top - 2, INK[0]); s.set(cx, top - 3, INK[1])
    FL = ['..d..',
          '..d..',
          '.dcd.',
          '.dbd.',
          'dcbcd',
          'dbabd',
          'dbabd',
          '.dbd.',
          '..e..']
    s.grid(cx - 2, top - 11, FL, {'a': FLAME[4], 'b': FLAME[3], 'c': FLAME[2], 'd': FLAME[1], 'e': FLAME[0]})
    return s


ASSETS['prop-candle'] = candle


def glow(rx=66, ry=56):
    """the candle light on the oak: a warm translucent pool in stepped rings, dithered where it fades"""
    s = Spr(2 * rx + 1, 2 * ry + 1)
    for y in range(2 * ry + 1):
        for x in range(2 * rx + 1):
            d = math.sqrt(((x - rx) / rx) ** 2 + ((y - ry) / ry) ** 2)
            if d > 1.0:
                continue
            a = (1.0 - d) ** 1.6 * 0.26
            step = round(a / 0.035) * 0.035
            if step <= 0.0 and dith(x, y, a / 0.035):
                step = 0.035
            if step > 0:
                s.set(x, y, rgba('#ffb35c', round(step * 255)))
    return s


ASSETS['prop-glow'] = glow


GLASS = ['#0f1412', '#18211d', '#22302a', '#33473e', '#557466', '#a9c4b5']


def inkwell():
    """a squat glass inkwell, the quill standing in it and leaning out to the right; the glass dark green
    with a cold window highlight on its upper left"""
    W, H = 50, 58
    s = Spr(W, H)
    cx, cy = 16, 48
    for y in range(H):
        for x in range(W):
            if ell_in(x, y, cx + 3, cy + 3, 12, 5):
                s.set(x, y, rgba(SH, 115))
    # the body: a short wide jar, its front wall and the round shoulder
    for y in range(cy - 12, cy + 4):
        for x in range(cx - 11, cx + 12):
            inside = ell_in(x, y, cx, cy, 11, 4) or (cy - 9 <= y <= cy and cx - 11 <= x <= cx + 11)
            if not inside:
                continue
            t = (x - (cx - 11)) / 22.0
            c = GLASS[3] if t < 0.18 else (GLASS[2] if t < 0.62 else (GLASS[1] if t < 0.9 else GLASS[0]))
            s.set(x, y, c)
    # the shoulder: an ellipse on top of the jar, lit at the back left
    for y in range(cy - 14, cy - 6):
        for x in range(cx - 11, cx + 12):
            if ell_in(x, y, cx, cy - 10, 11, 4):
                v = (x - cx) / 11.0 + (y - (cy - 10)) / 4.0
                s.set(x, y, GLASS[4] if v < -0.9 else (GLASS[3] if v < 0.1 else GLASS[2]))
    # the neck and the dark mouth with the ink inside
    for y in range(cy - 15, cy - 9):
        for x in range(cx - 5, cx + 6):
            if ell_in(x, y, cx, cy - 12, 5, 2.6):
                s.set(x, y, GLASS[4] if x < cx - 2 else GLASS[3])
            if ell_in(x, y, cx, cy - 12, 3.4, 1.6):
                s.set(x, y, '#08070a')
    s.set(cx - 2, cy - 13, '#3d4658')
    # the window highlight, a cold sliver on the upper left of the jar
    for y in range(cy - 7, cy - 1):
        s.set(cx - 8, y, GLASS[5] if y < cy - 4 else GLASS[4])
    s.set(cx - 7, cy - 7, GLASS[5])
    for x in range(cx - 10, cx + 11):
        s.set(x, cy + 4 if abs(x - cx) < 8 else cy + 3, GLASS[0])
    # the quill: shaft from the mouth up and to the right, vane on its upper side, barbs ticked
    x0, y0, x1, y1 = cx + 1, cy - 13, cx + 30, 1
    n = max(abs(x1 - x0), abs(y1 - y0))
    shaft = []
    for k in range(n + 1):
        t = k / n
        shaft.append((round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t)))
    for i, (x, y) in enumerate(shaft):
        t = i / len(shaft)
        if t > 0.22:
            wv = round(4.2 * math.sin(math.pi * min(1.0, (t - 0.22) / 0.78) ** 0.8))
            for k in range(1, wv + 1):
                c = VEL[8] if k == wv else (VEL[6] if (i + k) % 3 else VEL[5])
                s.set(x - k, y, c)
            for k in range(1, max(1, wv - 2) + 1):
                s.set(x + k, y + 1, VEL[5] if (i + k) % 2 else VEL[4])
        s.set(x, y, VEL[3] if t > 0.22 else INK[3])
    s.set(x1, y1, VEL[8])
    return s


ASSETS['prop-inkwell'] = inkwell


def coins():
    """three coins on the table: a pair stacked, one lying apart; each a flat disc seen from above and in
    front, lit on its upper rim, its milled edge showing below"""
    s = Spr(28, 13)
    TOP = ['..hHHm..',
           '.hHmmmmd',
           'hHmMmmdd',
           'mmmmmddd',
           '.mddddd.']
    EDGE = ['.kekeke.', '..kkkk..']
    for (x, y) in ((2, 6), (3, 3), (17, 6)):
        for (dx, dy) in ((2, 6), (3, 6), (4, 6), (5, 6), (6, 5), (7, 5), (8, 4)):
            s.set(x + dx, y + dy, rgba(SH, 110))
        s.grid(x, y + 4, EDGE, {'k': BRASS[1], 'e': BRASS[3]})
        s.grid(x, y, TOP, {'h': BRASS[6], 'H': BRASS[7], 'm': BRASS[5], 'M': BRASS[4], 'd': BRASS[3]})
    return s


ASSETS['prop-coins'] = coins


def map_sheet(w=236, h=92, seed=21):
    """the corner of the campaign map slid under the rail: a coast with its water ruled in short strokes,
    hatched hills, a road dotted between two towns, the edge of a compass rose; the whole sheet sits in the
    table shadow so it stays quiet"""
    from v4a_paper import parchment
    S, paper = parchment(w, h, 'lo', seed, (2, 3), folds=(52,), rim=3, stains=1, fibres=0.8, mottle='edge',
                         foxing=0.8)
    rnd = random.Random(seed)
    K, D, M = INK[1], INK[2], INK[3]

    def ink(x, y, c):
        if (x, y) in paper:
            S.set(x, y, c)
    # the coast: a ragged line from the left edge down to the lower middle; the sea below and left of it
    coast = []
    y = 30.0
    for x in range(4, 150):
        y += rnd.uniform(-0.9, 1.05) + (0.32 if x > 60 else 0.0)
        y = max(24.0, min(h - 8.0, y))
        coast.append((x, round(y)))
    for (x, y) in coast:
        ink(x, y, D)
        ink(x, y + 1, M)
    for (x, y) in coast:
        for k in range(3, 40, 4):
            Y = y + k
            if Y < h - 5 and (x + k * 3) % 9 < 4:
                ink(x, Y, INK[4])
    # hills above the coast, drawn as a mapmaker draws them: two flanks meeting at a peak, the right flank
    # shaded with short strokes falling from it, a ground line at the foot
    for (hx, hy, hh) in ((30, 12, 7), (46, 9, 9), (62, 15, 6), (94, 24, 7), (110, 18, 9), (126, 28, 6),
                         (172, 32, 7), (190, 24, 9), (206, 34, 6)):
        for k in range(hh + 1):
            ink(hx - k, hy + k, D if k < hh - 1 else M)
            ink(hx + k, hy + k, K if k < hh - 1 else M)
        for k in range(2, hh, 2):
            for j in range(1, min(4, hh - k + 1)):
                ink(hx + k, hy + k + j, M)
        ink(hx, hy - 1, M)
    # a stand of trees between the hills and the road
    for (tx, ty) in ((140, 40), (146, 38), (152, 41), (143, 45), (158, 44)):
        ink(tx, ty, D); ink(tx - 1, ty + 1, D); ink(tx + 1, ty + 1, K); ink(tx - 1, ty + 2, D)
        ink(tx + 1, ty + 2, K); ink(tx, ty + 3, M)
    # two towns and the road between them
    for (tx, ty) in ((150, 58), (214, 66)):
        for (dx, dy) in ((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (0, 2), (1, 2), (2, 2)):
            ink(tx + dx, ty + dy, K)
        ink(tx + 1, ty - 1, K)
    for i in range(0, 64, 3):
        t = i / 64.0
        ink(round(153 + 60 * t), round(60 + 6 * t + 3 * math.sin(t * 6)), M)
    # the edge of a compass rose in the lower right
    rcx, rcy = 220, 88
    for a in range(0, 360, 5):
        r = 14
        ink(round(rcx + r * math.cos(math.radians(a))), round(rcy + r * math.sin(math.radians(a))), M)
    for a in range(0, 360, 45):
        L = 20 if a % 90 == 0 else 13
        for k in range(4, L):
            ink(round(rcx + k * math.cos(math.radians(a))), round(rcy + k * math.sin(math.radians(a))), D)
    # the table shadow over the sheet: deeper toward the rail and toward the left edge
    for (x, y) in paper:
        a = 0.40 + 0.22 * max(0.0, 1.0 - y / 40.0) + 0.12 * max(0.0, 1.0 - x / 80.0)
        S.over(x, y, '#1a0f08', round(min(0.66, a) * 255))
    return S


ASSETS['prop-mapsheet'] = map_sheet


# ---------------------------------------------------------------- the two plain orders, wider for the subtitle
from v4a_paper import _card
ASSETS['order-lo'] = lambda: _card(152, 136, 'lo', 3, (2, 3), folds=(58,), mottle='edge')
ASSETS['order-lo2'] = lambda: _card(152, 136, 'lo', 8, (2, 3), folds=(61,), mottle='edge')


# ---------------------------------------------------------------- v4.1: the map, rolled and tied
def _v(*a):
    return a


def _dot(p, q):
    return p[0] * q[0] + p[1] * q[1] + p[2] * q[2]


def _add(p, q, s=1.0):
    return (p[0] + q[0] * s, p[1] + q[1] * s, p[2] + q[2] * s)


def _hit_cyl(O, v, A, a, R, s0, s1):
    """the nearest-to-camera entry of the ray O + t v (t grows toward the camera) into the finite cylinder
    round the axis A + s a, s0 <= s <= s1: (t, part, s) or None"""
    w = _add(O, A, -1.0)
    wa, va = _dot(w, a), _dot(v, a)
    wp, vp = _add(w, a, -wa), _add(v, a, -va)
    qa, qb, qc = _dot(vp, vp), 2.0 * _dot(wp, vp), _dot(wp, wp) - R * R
    best = None
    disc = qb * qb - 4.0 * qa * qc
    if disc >= 0.0 and qa > 1e-9:
        sq = math.sqrt(disc)
        for t in ((-qb + sq) / (2.0 * qa), (-qb - sq) / (2.0 * qa)):
            s = wa + t * va
            if s0 <= s <= s1 and (best is None or t > best[0]):
                best = (t, 'body', s)
    if abs(va) > 1e-9:
        for sc, nm in ((s0, 'cap0'), (s1, 'cap1')):
            t = (sc - wa) / va
            q = _add(_add(w, v, t), a, -sc)
            if _dot(q, q) <= R * R and (best is None or t > best[0]):
                best = (t, nm, sc)
    return best


MAPROLL_AT = (148, 48)                   # where the sprite sits on the page; the rail covers rows above 57


def map_roll(seed=23):
    """the campaign map, rolled and tied, lying on the oak with its far end pushed under the rail. Traced as a
    cylinder in the props’ oblique view (the table’s depth drawn at 0.42, heights at 0.92, as the inkwell and
    candle are): warm vellum turning from a lit top to a shaded underside with the oak’s brown bounced into it,
    a faint grain along its length and a few foxing spots; the outer sheet’s edge running down the roll as a lit
    lip over a dark line; at the near end the curl, the sheet wound three times round a hollow core; a faded red
    ribbon tied round it near that end in a small bow, its two tails fallen onto the table; its shadow on the
    oak to the lower right."""
    PX, PY = MAPROLL_AT
    W, H = 156, 80
    S = Spr(W, H)
    c, k = 0.42, 0.92
    r, L = 11.0, 160.0
    beta = math.atan(0.5 / c)
    a = (-math.cos(beta), math.sin(beta), 0.0)          # from the far end (under the rail) to the near end
    uh = (math.sin(beta), math.cos(beta), 0.0)          # across the roll, toward the viewer
    uz = (0.0, 0.0, 1.0)
    Xb, Yb = 178.0, 94.0                                # the near end centre on the page
    B = (Xb, (Yb + k * r) / c, r)
    A = _add(B, a, -L)
    sr0, sr1 = L - 36.0, L - 31.0                       # the ribbon band
    Ld = (-0.574, 0.242, 0.782)
    Ls = (-0.40, -0.55, 1.0)
    n_ = math.sqrt(_dot(Ls, Ls)); Ls = (Ls[0] / n_, Ls[1] / n_, Ls[2] / n_)
    v = (0.0, k, c)
    the = 0.78                                          # where the outer sheet ends, round the roll
    grain = Noise(seed, 2, 9)
    rnd = random.Random(seed)
    fox = [(rnd.uniform(40, L - 8), rnd.uniform(-0.5, 1.6)) for _ in range(7)]
    RAMP = [VEL[0], VEL[1], VEL[2], VEL[3], VEL[4], VEL[5]]
    RIB = [WAX[0], WAX[1], WAX[2], WAX[3], WAX[4], WAX[5]]

    def proj(P):
        return (P[0] - PX, c * P[1] - k * P[2] - PY)

    def ramp(pal, I, x, y):
        f = max(0.0, min(len(pal) - 1.0, I * (len(pal) - 1)))
        i = int(f); fr = f - i
        if i < len(pal) - 1 and dith(x, y, max(0.0, min(1.0, (fr - 0.3) / 0.4))):
            i += 1
        return pal[i]

    Imap, cap = {}, set()
    for y in range(H):
        for x in range(W):
            X, Y = PX + x + 0.5, PY + y + 0.5
            O = (X, Y / c, 0.0)
            h1 = _hit_cyl(O, v, A, a, r, 0.0, L)
            h2 = _hit_cyl(O, v, A, a, r + 0.8, sr0, sr1)
            h = h1 if (h2 is None or (h1 is not None and h1[0] >= h2[0])) else h2
            if h is None:
                # the table: the shadow of the roll, soft at its edge
                w0 = _add(O, A, -1.0)
                bb, dd, ee = _dot(Ls, a), _dot(Ls, w0), _dot(a, w0)
                den = 1.0 - bb * bb
                u = (bb * ee - dd) / den; s = (ee - bb * dd) / den
                if u > 0 and -2.0 < s < L + 2.0:
                    p = _add(_add(w0, Ls, u), a, -s)
                    dist = math.sqrt(_dot(p, p))
                    al = max(0.0, min(1.0, (r + 0.8 - dist) / 2.6))
                    if s > L:
                        al *= max(0.0, 1.0 - (s - L) / 2.0)
                    if al > 0:
                        S.set(x, y, rgba(SH, round(36 + 84 * al)))
                continue
            t, part, s = h
            p = _add(O, v, t)
            ribbon = h is h2
            if part == 'body':
                q = _add(_add(p, A, -1.0), a, -s)
                R = math.sqrt(_dot(q, q)) or 1.0
                n = (q[0] / R, q[1] / R, q[2] / R)
                th = math.atan2(_dot(n, uh), _dot(n, uz))
                I = 0.20 + 0.80 * max(0.0, _dot(n, Ld)) + 0.12 * max(0.0, -n[2])
                Imap[(x, y)] = I
                if ribbon:
                    I = 0.15 + 0.85 * max(0.0, _dot(n, Ld)) + 0.08 * max(0.0, -n[2])
                    if s - sr0 < 0.9 or sr1 - s < 0.9:
                        I -= 0.14
                    S.set(x, y, ramp(RIB, I, x, y))
                    continue
                I += (grain(s * 1.0, th * r) - 0.5) * 0.10
                e = the + 0.06 * math.sin(s * 0.21) + 0.03 * math.sin(s * 0.53)
                if e - 0.12 <= th < e:
                    I += 0.12
                elif e <= th < e + 0.20:
                    I -= 0.30
                col = ramp(RAMP, I, x, y)
                for (fs, ft) in fox:
                    if abs(s - fs) < 1.6 and abs(th - ft) * r < 1.2:
                        col = mix(col, VEL[1], 0.45)
                S.set(x, y, col)
            else:
                if ribbon:
                    S.set(x, y, RIB[1]); continue
                # the near end: the cut edges of the wound sheet, lit from the upper left
                cen = _add(A, a, s)
                q = _add(p, cen, -1.0)
                rho = math.sqrt(_dot(q, q)) / r
                side = (_dot(q, uh) * 0.55 - _dot(q, uz) * 0.85) / r     # +1 toward the lower right
                I = 0.66 - 0.16 * max(0.0, side) + 0.06 * max(0.0, -side)
                Imap[(x, y)] = I
                cap.add((x, y))
                if rho > 0.92:
                    I -= 0.10 if side < 0 else 0.22
                S.set(x, y, ramp(RAMP, I, x, y))
    # the curl: the edge of the sheet wound inward from where the outer sheet ends, one unbroken line
    cen = B
    turns, rho_core = 2.75, 0.24
    prev = None
    for i in range(2400):
        psi = i / 2400.0 * turns * 2 * math.pi
        rho = 1.0 - (1.0 - rho_core) * psi / (turns * 2 * math.pi)
        th = the + psi
        P = _add(cen, _add(_add((0, 0, 0), uz, math.cos(th)), uh, math.sin(th)), r * rho)
        X, Y = proj(P)
        px, py = int(math.floor(X)), int(math.floor(Y))
        if (px, py) != prev and (px, py) in cap:
            side = (math.sin(th) * 0.55 - math.cos(th) * 0.85)
            S.set(px, py, VEL[1] if side > -0.2 else VEL[2])
            prev = (px, py)
    # the hollow core
    for (x, y) in cap:
        P0 = (PX + x + 0.5, (PY + y + 0.5) / c, 0.0)
        hh = _hit_cyl(P0, v, A, a, r, 0.0, L)
        if hh is None:
            continue
        q = _add(_add(P0, v, hh[0]), B, -1.0)
        rho = math.sqrt(_dot(q, q)) / r
        if rho < rho_core - 0.06:
            S.set(x, y, '#2b2117' if rho < rho_core - 0.12 else VEL[1])
    # the knot, on the front of the band where it turns under the roll, and its two tails fallen to the table
    sm = (sr0 + sr1) / 2.0
    thk = 1.85
    Pk = _add(_add(A, a, sm), _add(_add((0, 0, 0), uz, math.cos(thk)), uh, math.sin(thk)), r + 0.8)
    kx, ky = proj(Pk)
    kx, ky = int(round(kx)), int(round(ky))

    def rib_px(x, y, I):
        S.set(x, y, ramp(RIB, I, x, y))

    # each tail: (drift per row, rows, start column offset); flat on the oak, lit on its left edge, a shadow
    # to its right, cut in a swallowtail at the end
    for (dx, ln, ox, ph) in ((-0.62, 13, -1, 0.0), (0.46, 16, 1, 1.3)):
        cells = []
        for j in range(ln):
            fx = kx + ox + dx * j + 0.35 * math.sin(j * 0.55 + ph)
            cells.append((int(round(fx)), ky + 1 + j))
        for j, (xi, yi) in enumerate(cells):
            last = j >= ln - 1
            for cc in (-1, 0, 1):
                if last and cc == 0:
                    continue
                x, y = xi + cc, yi
                if S.get(x + 1, y + 1)[3] < 140 and (x + 1, y + 1) not in Imap:
                    S.set(x + 1, y + 1, rgba(SH, 120))
        for j, (xi, yi) in enumerate(cells):
            last = j >= ln - 1
            twist = 0.10 * math.sin(j * 0.6 + ph)
            for cc in (-1, 0, 1):
                if last and cc == 0:
                    continue
                I = 0.56 + twist + (0.14 if cc < 0 else (-0.16 if cc > 0 else 0.0)) - 0.012 * j
                rib_px(xi + cc, yi, I)
    # the knot itself: a small lump, lit on its upper left
    for (x, y, I) in ((kx - 1, ky - 1, 0.62), (kx, ky - 1, 0.70), (kx + 1, ky - 1, 0.50),
                      (kx - 1, ky, 0.50), (kx, ky, 0.56), (kx + 1, ky, 0.34),
                      (kx - 1, ky + 1, 0.30), (kx, ky + 1, 0.22), (kx + 1, ky + 1, 0.18)):
        rib_px(x, y, I)
    S.set(kx + 2, ky, RIB[0]); S.set(kx + 2, ky + 1, rgba(SH, 120))
    return S


ASSETS['prop-maproll'] = map_roll
