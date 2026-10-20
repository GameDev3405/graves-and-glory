"""v4.2 Rivergate map inset: the whole field, north up, at a smaller feature scale so there is open ground
between things (v4.1 drew a close-up crowded with detail; Kort: "you can't tell what's what").

What reads first, each in dark ink with air round it: the two bases at the west and east edges (their flags
are the only team colour on the map: Dawn teal, Dusk violet), the three lanes that fan out of each gate, the
river running north to south and its three crossings (the plank bridge at A, the stone bridge under B, the
ford by C), the three wax seals in relief, and the mill hamlet at A. Everything else is quiet texture in the
ground tone, smaller and paler: the woods, the ridges and hills between the lanes, the mountains on the north
and south edges, the farm and its hedge, the shrine on its hill above C.

The layout is CampaignMaps.select(0): river x = 12 sin(0.014 z); points A (-39,-145), B (0,0), C (39,145);
crossings on the lanes at z -145, 0 and 145; gates at x -314 and 314, the lanes leaving them for the forks at
x -205 and 205; the hamlet west of A, the mill north of A's bridge, the shrine east of C. The middle of the
field is drawn at about half a pixel a block and the long approach roads are squeezed, as a campaign map
draws them; the hamlet and the shrine are nudged clear of the seals. Light comes from the top left."""
import math, random
from v4lib import Spr, rgba
from v4pal import INK, WAX, TEAL, VIOL
from v4a_paper import parchment
from v4a_ink import disc, rect

ASSETS = {}
MAP_W, MAP_H = 136, 80
X0, X1, Y0, Y1 = 4, MAP_W - 5, 4, MAP_H - 5          # the ground inside the neatline

# quiet texture, all in the ground tone: a step or two darker than the paper, never ink
Q = ['#bfae83', '#cbbb91', '#d6c79f', '#e0d3ae']      # deepest .. faintest
# the river: the paper washed with thin ink, an olive grey cooler than the ground
RIVER = ['#7e7c64', '#93917a', '#aaa88e', '#bebca3', '#d0ceb8']
STONE = ['#4a4436', '#6c6553', '#8e8771', '#b1aa92', '#d0c9b0']
WOOD = ['#3f2a1b', '#5f4029', '#7f5935', '#9f7445', '#bd9058']
LANE_BED = '#f3ead1'

# ---------------------------------------------------------------- the projection
LANE_Y = (19, 40, 61)                                 # the north, middle and south lanes (z -145, 0, 145)
KY = (LANE_Y[1] - LANE_Y[0]) / 145.0
# (world |x|, map x) on the east half; the west half is its half turn, as the field is
XS = ((0, 67.5), (39, 87.0), (205, 110.0), (290, 117.0), (314, 123.0), (350, 133.0))
KR = (XS[1][1] - XS[0][1]) / XS[1][0]                 # the scale in the middle, for the river's swing
HALF = 2.6                                            # the river's half width: its 12 blocks at that scale


def mx(xw):
    if xw < 0:
        return MAP_W - 1 - mx(-xw)
    for (a, pa), (b, pb) in zip(XS, XS[1:]):
        if xw <= b:
            return pa + (pb - pa) * (xw - a) / (b - a)
    return XS[-1][1]


def my(z):
    return LANE_Y[1] + z * KY


def river_x(y):
    """the river's centre line at row y: x = 12 sin(0.014 z), at the middle-of-the-field scale"""
    z = (y - LANE_Y[1]) / KY
    return XS[0][1] + 12 * math.sin(0.014 * z) * KR


SEAL_AT = {'A': (round(mx(-39)), LANE_Y[0]), 'B': (67, LANE_Y[1]), 'C': (round(mx(39)), LANE_Y[2])}
SEALS = {k: (x - 7, y - 7) for k, (x, y) in SEAL_AT.items()}      # top-left of each 17 x 17 seal sprite


def inside(x, y):
    return X0 <= x <= X1 and Y0 <= y <= Y1


def cells(x, y, rows):
    return {(x + i, y + j) for j, r in enumerate(rows) for i, ch in enumerate(r) if ch not in '. '}


# ---------------------------------------------------------------- frame
def neatline(S, W, H):
    """a fine outer rule and a firmer inner one (v4.1's piano-key border and compass rose are gone)"""
    for x in range(1, W - 1):
        S.set(x, 1, INK[4]); S.set(x, H - 2, INK[4])
        if 3 <= x <= W - 4:
            S.set(x, 3, INK[2]); S.set(x, H - 4, INK[2])
    for y in range(1, H - 1):
        S.set(1, y, INK[4]); S.set(W - 2, y, INK[4])
        if 3 <= y <= H - 4:
            S.set(3, y, INK[2]); S.set(W - 4, y, INK[2])


# ---------------------------------------------------------------- helpers
def dist_seg(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    L = dx * dx + dy * dy
    t = 0.0 if L == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L))
    return math.hypot(px - ax - t * dx, py - ay - t * dy)


def band(path, r):
    """the pixels within r of a polyline (pixel centres)"""
    xs = [p[0] for p in path]; ys = [p[1] for p in path]
    out = set()
    for y in range(math.floor(min(ys) - r) - 1, math.ceil(max(ys) + r) + 2):
        for x in range(math.floor(min(xs) - r) - 1, math.ceil(max(xs) + r) + 2):
            if min(dist_seg(x, y, *a, *b) for a, b in zip(path, path[1:])) <= r:
                out.add((x, y))
    return out


def stamp(S, x, y, rows, pal, taken=None, shadow=70):
    """a building: its soft shadow to the lower right, then the glyph"""
    solid = cells(0, 0, rows)
    if shadow:
        for (i, j) in solid:
            if (i + 1, j + 1) not in solid and inside(x + i + 1, y + j + 1):
                S.over(x + i + 1, y + j + 1, INK[2], shadow)
    S.grid(x, y, rows, pal)
    if taken is not None:
        taken |= cells(x, y, rows)


# ---------------------------------------------------------------- the river
def river(S):
    """an olive-grey ink wash five to six pixels wide, swinging west above B and east below it: pale shallows
    under the lit west bank, the deep channel darker toward the shaded east bank, short glints running with
    the stream; the west bank inked light, the east bank firmer"""
    water = set()
    for y in range(Y0, Y1 + 1):
        xc = river_x(y)
        for x in range(math.floor(xc - HALF) - 1, math.ceil(xc + HALF) + 2):
            if abs(x - xc) <= HALF and inside(x, y):
                water.add((x, y))
    for (x, y) in water:
        t = (x - river_x(y)) / HALF
        S.set(x, y, RIVER[3] if t < -0.55 else (RIVER[1] if t > 0.4 else RIVER[2]))
    for (x, y) in water:
        if (x - 1, y) not in water and inside(x - 1, y):
            S.set(x - 1, y, INK[3])
        if (x + 1, y) not in water and inside(x + 1, y):
            S.set(x + 1, y, INK[2])
    for y in range(Y0, Y1 + 1):
        xc = river_x(y)
        for x in (math.floor(xc - HALF) - 3, math.ceil(xc + HALF) + 3):
            if y % 4 != 3 and inside(x, y):
                S.set(x, y, Q[1])
        for (off, ph) in ((-0.9, 0), (0.6, 3)):
            k = (round(xc + off), y)
            if (y + ph) % 6 in (0, 1) and k in water:
                S.set(k[0], y, RIVER[4] if off < 0 else RIVER[3])
    return water


# ---------------------------------------------------------------- lanes
def lane_paths():
    (yn, ym, ys) = LANE_Y
    gw, gx = mx(-290), mx(290)
    fw, fe = mx(-205), mx(205)
    return [[(gw, ym), (gx, ym)],
            [(gw, ym), (fw, yn), (fe, yn), (gx, ym)],
            [(gw, ym), (fw, ys), (fe, ys), (gx, ym)]]


def lanes(S, water):
    """the three lanes: a pale bed between two ruled edges in a lighter ink than the landmarks"""
    bed, rim = set(), set()
    for p in lane_paths():
        bed |= band(p, 0.62)
        rim |= band(p, 1.62)
    rim -= bed
    for (x, y) in rim:
        if inside(x, y) and (x, y) not in water:
            S.set(x, y, INK[4])
    for (x, y) in bed:
        if inside(x, y) and (x, y) not in water:
            S.set(x, y, LANE_BED)
    return bed | rim


# ---------------------------------------------------------------- the crossings
def plank_bridge(S, y):
    """A's crossing: a plank deck on the lane, railed both sides, a post at each corner, its shadow on the
    water below"""
    xc = river_x(y)
    x0, x1 = math.floor(xc - HALF) - 1, math.ceil(xc + HALF) + 1
    for x in range(x0, x1 + 1):
        S.set(x, y - 2, INK[1]); S.set(x, y + 2, INK[1])
        for yy in (y - 1, y, y + 1):
            S.set(x, yy, WOOD[4] if yy == y - 1 else (WOOD[3] if x % 2 else WOOD[2]))
        if abs(x - xc) <= HALF:
            S.set(x, y + 3, RIVER[0])
    for x in (x0, x1):
        S.set(x, y - 3, INK[1]); S.set(x, y + 3, INK[1])


def stone_bridge(S, y, x0, x1):
    """B's crossing: a stone deck between parapets, long enough that its ends show either side of the seal"""
    for x in range(x0, x1 + 1):
        S.set(x, y - 3, INK[1]); S.set(x, y + 3, INK[1])
        S.set(x, y - 2, STONE[4]); S.set(x, y + 2, STONE[2])
        for yy in (y - 1, y, y + 1):
            S.set(x, yy, STONE[3] if (x + yy) % 4 else STONE[2])
    for x in (x0, x1):
        for yy in range(y - 3, y + 4):
            S.set(x, yy, INK[1])


def ford(S, y, water):
    """C's crossing: a causeway of set stones across the stream, water showing between the stones on its
    edges; its downstream lip in shadow"""
    xc = river_x(y)
    xs = range(math.floor(xc - HALF) - 1, math.ceil(xc + HALF) + 2)
    for x in xs:
        S.set(x, y - 2, INK[3]); S.set(x, y + 2, INK[2])
        S.set(x, y, STONE[4] if x % 3 else STONE[3])
        for yy in (y - 1, y + 1):
            S.set(x, yy, STONE[3] if (x + (yy > y)) % 2 else RIVER[1])


# ---------------------------------------------------------------- landmarks
HUT = ['..oo..', '.oLDo.', 'oLLDDo', '.owso.', '.odso.', '.oooo.']
HOUSE = ['...oo...', '..oLDo..', '.oLLDDo.', 'oLLLDDDo', '.owwwso.', '.owdwso.', '.oooooo.']
MILL = ['..oo..', '.oLDo.', 'oLLDDo', '.ownw.', '.owdw.', '.oooo.']
SHRINE = ['....o....', '..ooLoo..', '.oLLLLDo.', 'ooooooooo', '.cicicic.', '.cicicic.', 'ooooooooo']
BPAL = {'o': INK[1], 'L': WOOD[3], 'D': WOOD[1], 'w': STONE[4], 's': STONE[2], 'd': INK[1], 'n': INK[2],
        'c': STONE[4], 'i': INK[2]}
SPAL = dict(BPAL, o=INK[3], L=STONE[4], D=STONE[2], i=INK[4])   # a notch below the hamlet: a landmark, not a goal
# the base: two towers and a gated curtain, the team flag flying from the near tower across the gap
FORT = ['....pThh.......',
        '....pTTTTt.....',
        '....pTTt.......',
        '....p..........',
        'o.o.o.....o.o.o',
        'ooooo.....ooooo',
        'oWSsoo.o.ooWSso',
        'oWSsoooooooWSso',
        'oWSsoWSSSsoWSso',
        'oWSsoWogosoWSso',
        'oWssoWgggsoWsso',
        'oWssoWgggsoWsso',
        'oooooogggoooooo']


def fort(S, cx, team, taken):
    pal = {'o': INK[1], 'W': STONE[4], 'S': STONE[3], 's': STONE[2], 'p': INK[1], 'g': INK[0],
           'T': team[3], 't': team[2], 'h': team[4]}
    stamp(S, round(cx) - 7, LANE_Y[1] - 8, FORT, pal, taken=taken)


def wheel(S, cx, cy):
    """the mill wheel dipping into the stream: a rim of paddles round a hub, lit on its upper left"""
    for (x, y) in disc(cx, cy, 1.9) - disc(cx, cy, 0.6):
        S.set(x, y, WOOD[4] if (x - cx) + (y - cy) < -0.5 else (WOOD[1] if (x - cx) + (y - cy) > 0.5 else WOOD[2]))
    S.set(round(cx), round(cy), INK[1])
    for (x, y) in disc(cx, cy, 2.9) - disc(cx, cy, 1.9):
        if (x + y) % 2 == 0:
            S.set(x, y, INK[2])


def hamlet(S, taken):
    """the mill hamlet at A: four houses either side of the lane west of the seal, the mill and its wheel
    on the bank north of the bridge, the farm north of the houses inside its hedge"""
    yn = LANE_Y[0]
    for (x, y, b) in ((24, yn - 9, HOUSE), (33, yn - 8, HUT), (27, yn + 3, HUT), (33, yn + 3, HOUSE)):
        stamp(S, x, y, b, BPAL, taken=taken)
    wx = round(river_x(Y0 + 5) - HALF) - 3
    stamp(S, wx - 8, Y0 + 2, MILL, BPAL, taken=taken)
    wheel(S, wx, Y0 + 5)
    taken |= rect(wx - 3, Y0 + 1, 7, 8)
    # the farm: strips ruled in the ground tone inside a dotted hedge
    fx, fy, fw, fh = 21, Y0 + 2, 10, 3
    for y in range(fy, fy + fh):
        for x in range(fx, fx + fw):
            S.set(x, y, Q[1] if (y - fy) % 2 == 0 else Q[3])
    for x in range(fx - 1, fx + fw + 1):
        for y in (fy - 1, fy + fh):
            if x % 2 == 0:
                S.set(x, y, Q[0])
    for y in range(fy, fy + fh):
        for x in (fx - 1, fx + fw):
            if y % 2 == 0:
                S.set(x, y, Q[0])
    taken |= rect(fx - 2, fy - 2, fw + 4, fh + 4)


# ---------------------------------------------------------------- quiet ground
MOUNT = ['....q....',
         '...q.d...',
         '..q..dd..',
         '.q...ddd.',
         'q....dddd']
MOUNT_S = ['...q...',
           '..q.d..',
           '.q..dd.',
           'q...ddd']
HILL = ['...qqq...',
        '..q...dd.',
        '.q....ddd',
        'q......dd']
HILL_S = ['..qqq..',
          '.q..ddd',
          'q....dd']
TREE = ['.hb.',
        'hbbc',
        '.cc.',
        '..t.']
QPAL = {'q': Q[0], 'd': Q[1], 'h': Q[3], 'b': Q[1], 'c': Q[0], 't': Q[0]}


def shrine(S, taken):
    """the river shrine on its hill above C: the hill drawn as the quiet ones are, the shrine in ink on it"""
    cxs, cys = SEAL_AT['C']
    x, y = cxs + 11, cys - 12
    S.grid(x - 2, y + 6, ['....qqqqq....', '..qq.....ddd.', 'qq.........dd'], QPAL)
    stamp(S, x, y, SHRINE, SPAL, taken=taken)
    taken |= rect(x - 3, y + 5, 15, 5)


def ground_texture(S, keep):
    """the mountains at the north and south edges, the ridges and hills between the lanes, the woods: small,
    pale, in the ground tone, and kept a pixel clear of everything that matters (keep)"""
    def fits(x, y, rows, pad=1):
        cs = cells(x, y, rows)
        if not all(inside(*c) for c in cs):
            return False
        ring = {(cx + a, cy + b) for (cx, cy) in cs for a in range(-pad, pad + 1) for b in range(-pad, pad + 1)}
        return not (ring & keep)

    def put(x, y, rows, pad=1):
        if fits(x, y, rows, pad):
            S.grid(x, y, rows, QPAL)
            keep.update(cells(x, y, rows))
            return True
        return False
    # the north mountains show east of the river, the south ones west of it (the hamlet and the shrine
    # hold the other two corners)
    for (x, y, m) in ((75, 4, MOUNT), (84, 5, MOUNT_S), (91, 4, MOUNT), (100, 5, MOUNT_S),
                      (52, 71, MOUNT), (45, 72, MOUNT_S), (36, 71, MOUNT), (29, 72, MOUNT_S)):
        put(x, y, m)
    # the ridges north of the middle lane and the hills south of it, one pair each side of the river
    for (x, y, h) in ((30, 28, HILL), (40, 31, HILL_S), (95, 28, HILL), (88, 31, HILL_S),
                      (30, 47, HILL), (40, 50, HILL_S), (95, 45, HILL_S)):
        put(x, y, h)
    # the woods: CampaignMaps plants an oak every 13 blocks where a hash allows, away from roads, the river,
    # buildings and points; here they are gathered into copses where they stand thickest
    roads = _roads()
    trees = []
    for x in range(-325, 326, 13):
        for z in range(-225, 226, 13):
            if (x * 7343 + z * 9129) % 97 >= 22:
                continue
            if min(_rd(x, z, r) for r in roads) < 10 or abs(x - 12 * math.sin(z * .014)) < 17:
                continue
            if any(abs(x - sx) < w / 2 + 9 and abs(z - sz) < d / 2 + 9 for (sx, sz, w, d) in _STRUCT):
                continue
            if any(math.hypot(x - px, z - pz) < 30 for (px, pz) in ((-39, -145), (0, 0), (39, 145))):
                continue
            trees.append((mx(x), my(z)))
    groups = {}
    for (x, y) in trees:
        groups.setdefault((int(x // 16), int(y // 10)), []).append((x, y))
    rnd = random.Random(7)
    for (k, pts) in sorted(groups.items(), key=lambda kv: (-len(kv[1]), kv[0]))[:14]:
        x = round(sum(p[0] for p in pts) / len(pts)) - 2 + rnd.randint(-1, 1)
        y = round(sum(p[1] for p in pts) / len(pts)) - 2 + rnd.randint(-1, 1)
        n = 3 if len(pts) >= 9 else (2 if len(pts) >= 5 else 1)
        offs = rnd.choice((((0, 0), (4, 1), (2, -3)), ((0, 0), (4, -1), (1, 3)), ((0, 0), (-4, 1), (-1, -3))))
        placed = 0
        for (dx, dy) in offs + ((0, 2), (2, 0), (-2, 0)):
            if placed >= n:
                break
            if put(x + dx, y + dy, TREE, pad=0 if placed else 1):
                placed += 1


# the roads and buildings of CampaignMaps.select(0), for the woodland rule only
def _roads():
    out = []
    for i, z in enumerate((-145, 0, 145)):
        rx = int(12 * math.sin(z * .014))
        for side in (-1, 1):
            out += [(side * 290, 0, side * 205, z, 5), (side * 205, z, rx + side * 28, z, 5)]
        out.append((rx - 29, z, rx + 29, z, 6))
        if i == 0:
            for k in range(4):
                out.append((rx - 75 - (k % 2) * 26, z - 4 + (k // 2) * 31, rx - 29, z, 3))
            out += [(rx - 90, z - 25, rx - 29, z, 3), (rx - 18, z - 28, rx - 29, z, 3)]
    for side in (-1, 1):
        out += [(side * 286, -32, side * 286, 40, 5), (side * 205, -145, side * 205, 145, 4)]
        out += [(side * 180, z, side * 205, z, 3) for z in (-85, 85)]
    return out


def _rd(x, z, r):
    ax, az, bx, bz, w = r
    return dist_seg(x, z, ax, az, bx, bz) - w


_STRUCT = [(-10, -145, 44, 16), (0, 0, 44, 24), (10, 145, 44, 16), (-28, -173, 12, 12), (-100, -170, 26, 18),
           (-75, -120, 18, 8), (-85, -149, 14, 12), (-111, -149, 14, 12), (-85, -118, 14, 12), (-111, -118, 14, 12),
           (-58, 30, 18, 14), (60, -30, 24, 18), (69, 127, 22, 18)] + \
          [(s * 314, 0, 20, 72) for s in (-1, 1)] + [(s * 286, -s * 32, 28, 24) for s in (-1, 1)] + \
          [(s * 287, s * 40, 18, 16) for s in (-1, 1)] + [(s * 180, z, 11, 11) for s in (-1, 1) for z in (-85, 85)]


# ---------------------------------------------------------------- the sheet
def rivergate_map(seed=21):
    W, H = MAP_W, MAP_H
    S, paper = parchment(W, H, kind='map', seed=seed, shadow=(1, 1), deckle=False, rim=2, stains=0, fibres=0.45,
                         foxing=0.3, tears=False)
    neatline(S, W, H)
    taken = set()
    water = river(S)
    taken |= {(x + a, y) for (x, y) in water for a in (-2, -1, 0, 1, 2)}
    taken |= lanes(S, water)
    yn, ym, ys = LANE_Y
    plank_bridge(S, yn)
    stone_bridge(S, ym, SEAL_AT['B'][0] - 11, SEAL_AT['B'][0] + 11)
    ford(S, ys, water)
    for (sx, sy) in SEALS.values():
        taken |= rect(sx - 1, sy - 1, 19, 19)
    fort(S, mx(-314), TEAL, taken)
    fort(S, mx(314), VIOL, taken)
    hamlet(S, taken)
    shrine(S, taken)
    ground_texture(S, taken)
    for k, (sx, sy) in SEALS.items():
        S.blit(relief_seal(sx + sy), sx, sy)
    return S


def relief_seal(seed=1, size=15):
    """a wax seal pressed onto the map: an irregular poured lip, a raised rim ring, a sunk flat centre where
    the letter sits, lit from the top left with a glint, a soft shadow on the paper"""
    s = Spr(size + 2, size + 2)
    c = (size - 1) / 2.0

    def rr(a):
        return c + 0.3 + 0.45 * math.sin(a * 3 + seed) + 0.3 * math.sin(a * 5 + seed * 2.1)
    blob = {(x, y) for y in range(size) for x in range(size) if math.hypot(x - c, y - c) <= rr(math.atan2(y - c, x - c))}
    for (x, y) in blob:
        for (dx, dy, al) in ((1, 1, 120), (2, 2, 55), (2, 1, 55)):
            k = (x + dx, y + dy)
            if k not in blob and s.get(*k)[3] < al:
                s.set(k[0], k[1], rgba(INK[0], al))
    for (x, y) in blob:
        d = math.hypot(x - c, y - c)
        e = rr(math.atan2(y - c, x - c))
        lit = ((x - c) * -0.71 + (y - c) * -0.71) / max(d, 0.5)       # +1 faces the light
        if d > e - 1.1:
            col = WAX[3] if lit > 0.35 else (WAX[0] if lit < -0.35 else WAX[1])
        elif d > 4.9:
            col = WAX[4] if lit > 0.55 else (WAX[3] if lit > 0.1 else (WAX[2] if lit > -0.45 else WAX[1]))
        elif d > 4.0:
            col = WAX[0] if lit > 0.2 else (WAX[3] if lit < -0.35 else WAX[1])
        else:
            col = WAX[1] if (x * 7 + y * 3) % 11 else WAX[2]
        s.set(x, y, col)
    for (x, y) in ((round(c - 4), round(c - 3)), (round(c - 3), round(c - 4))):
        s.set(x, y, WAX[5])
    s.set(round(c - 4), round(c - 4), WAX[6])
    return s


ASSETS['map-rivergate'] = rivergate_map
ASSETS['map-seal'] = relief_seal
