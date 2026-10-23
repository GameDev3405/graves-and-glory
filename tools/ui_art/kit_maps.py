"""The War Table map cards (UI plan Task 7): one painted card for each map, 136 x 80, in the style and at the size of
the Rivergate card (v4a_map.py), each painted from its layout in CampaignMaps.select.

Every card keeps the Rivergate projection: the three lanes on rows 19, 40 and 61 whatever the lane spacing of the map,
the same squeezed x scale (v4a_map.mx), the river from the riverX of the map, the crossings by kind (planks on the north
lane, stone on the middle one, a ford on the south one), the points as relief wax seals lettered in the game font (the
Minecraft glyphs in CHIP_TEXT ivory, baked in so the home card and the Field page draw one sprite per map), the
landmarks of the map nudged clear of the seals as Rivergate nudges its hamlet and its shrine, and the quiet ground in
the paper tone: the mountains, the ridges and hills of the map, and its woods by the woodland rule of CampaignMaps.

What each map adds to the Rivergate kit:
  Ironhold Siege: a straight river; the curtain walls standing between the lanes on both flanks, the breached wall
    across the middle lane west of the Iron Gate, the keep round B, the supply barn and its hay, the garrison by C.
  Frostmarch Pass: a frozen gorge (an ice wash between hachured cliffs), snow-capped mountains on both edges and
    ridges between the lanes, the caravan lodge and the pass refuge on their cart tracks, the signal tower by C,
    sparse spruce.
  Hollowwood Reach: a wide slow river with reed beds at the crossings, the sawmill and its log pile, the broken
    chapel and the moon shrine by C, cart tracks, a dense spruce wood.
  Kingsfield Crossroads: five seals, the north and south crossings and the Kings Bridge under A, C and E, the raised
    outposts B and D on their mounds with the roads that climb to them, the hamlet with its mill and farm, the
    customs house and the warehouse, the shrine by E.

Rivergate is v4a_map.rivergate_map() unchanged, its letters where the home screen drew them."""
import math, random
import v4a_map
from v4pal import INK, TEAL, VIOL
from v4a_paper import parchment
from v4a_ink import disc, rect
from v4a_map import (MAP_W, MAP_H, X0, X1, Y0, Y1, Q, RIVER, STONE, WOOD, LANE_BED, LANE_Y, XS, KR, mx, neatline,
                     band, dist_seg, stamp, cells, inside, HUT, HOUSE, MILL, SHRINE, BPAL, SPAL, MOUNT, MOUNT_S, HILL,
                     HILL_S, TREE, QPAL, fort, wheel, relief_seal, stone_bridge, lane_paths)

# ---------------------------------------------------------------- the letters
CHIP = '#f1e7cb'          # DarkUi.CHIP_TEXT, as the home screen lettered the Rivergate seals
LETTERS = {               # the Minecraft font (ascii.png), 5 x 7
    'A': ['.xxx.', 'x...x', 'xxxxx', 'x...x', 'x...x', 'x...x', 'x...x'],
    'B': ['xxxx.', 'x...x', 'xxxx.', 'x...x', 'x...x', 'x...x', 'xxxx.'],
    'C': ['.xxx.', 'x...x', 'x....', 'x....', 'x....', 'x...x', '.xxx.'],
    'D': ['xxxx.', 'x...x', 'x...x', 'x...x', 'x...x', 'x...x', 'xxxx.'],
    'E': ['xxxxx', 'x....', 'xxx..', 'x....', 'x....', 'x....', 'xxxxx'],
}


def letter(S, ch, x, y):
    S.grid(x, y, LETTERS[ch], {'x': CHIP})


# ---------------------------------------------------------------- one map, as CampaignMaps.select builds it
class Field:
    """the points, roads and buildings of CampaignMaps.select(index), in blocks, and the card projection: x by
    v4a_map.mx, z so that the three lanes land on LANE_Y"""

    def __init__(self, index):
        self.index = index
        flanks = index == 4
        self.zs = zs = {1: 130, 2: 155, 4: 160}.get(index, 145)
        self.rf = {1: lambda z: 0.0, 2: lambda z: 9 * math.sin(z * .013),
                   3: lambda z: 18 * math.sin(z * .011)}.get(index, lambda z: 12 * math.sin(z * .014))
        self.ky = (LANE_Y[1] - LANE_Y[0]) / float(zs)
        roads, structs, points = [], [], {}

        def road(a, b, c, d, w):
            roads.append((a, b, c, d, w))

        def s(kind, x, z, w, d):
            structs.append((kind, x, z, w, d))
        for i, z in enumerate((-zs, 0, zs)):
            rx = int(self.rf(z))
            px = rx + (0 if flanks else (-29 if i == 0 else (29 if i == 2 else 0)))
            points['ACE'[i] if flanks else 'ABC'[i]] = (px, z)
            s('bridge', rx, z, 44, 24 if i == 1 else 16)
            for side in (-1, 1):
                road(side * 290, 0, side * 205, z, 5)
                road(side * 205, z, rx + side * 28, z, 5)
            road(rx - 29, z, rx + 29, z, 6)
            if i == 0:
                s('mill', rx - 18, z - 28, 12, 12)
                s('farm', rx - 90, z - 25, 26, 18)
                s('market', rx - 65, z + 25, 18, 8)
                for k in range(4):
                    hx, hz = rx - 75 - (k % 2) * 26, z - 4 + (k // 2) * 31
                    s('house', hx, hz, 14, 12)
                    road(hx, hz, rx - 29, z, 3)
                road(rx - 90, z - 25, rx - 29, z, 3)
                road(rx - 18, z - 28, rx - 29, z, 3)
            elif i == 1:
                if index == 1:
                    s('ruin', 0, 27, 58, 42)
                    s('breach', -37, 0, 6, 50)
                else:
                    s('house', rx - 58, 30, 18, 14)
                    s('barracks', rx + 60, -30, 24, 18)
            else:
                s('shrine', rx + 59, z - 18, 22, 18)
        for side in (-1, 1):
            s('base', side * 314, 0, 20, 72)
            s('barracks', side * 286, -side * 32, 28, 24)
            s('house', side * 287, side * 40, 18, 16)
            road(side * 286, -32, side * 286, 40, 5)
            road(side * 205, -zs, side * 205, zs, 4)
            for z in (-85, 85):
                s('watchtower', side * 180, z, 11, 11)
                road(side * 180, z, side * 205, z, 3)
        if index == 1:
            structs[:] = [t for t in structs if t[0] not in ('farm', 'mill', 'shrine')]
            s('house', -105, -160, 24, 18)
            s('barracks', 65, 135, 30, 22)
            for side in (-1, 1):
                for z in (-62, 62):
                    s('breach', side * 72, z, 5, 68)
        elif index == 2:
            structs[:] = [t for t in structs if t[0] not in ('farm', 'mill', 'market')]
            s('house', -80, -178, 26, 18)
            s('house', 62, 34, 20, 16)
            s('watchtower', 57, 166, 14, 14)
            road(-80, -178, -20, -155, 4)
            road(62, 34, 25, 0, 4)
        elif flanks:
            points['B'] = (-130, 100)
            points['D'] = (130, -100)
            for side in (-1, 1):
                road(side * 205, -side * 100, side * 130, -side * 100, 4)
                road(side * 130, -side * 100, side * 28, 0, 4)
        elif index == 3:
            structs[:] = [t for t in structs if t[0] not in ('farm', 'mill')]
            s('house', -85, -176, 24, 18)
            s('ruin', 70, 100, 28, 22)
            road(-85, -176, -35, -145, 4)
            road(70, 100, 40, 145, 3)
        self.roads, self.structs, self.points = roads, structs, points
        # the seals: each point at its card pixel, a half pixel rounded down as Rivergate rounds B
        self.seal_at = {k: (self.px(mx(x)), self.px(self.my(z))) for k, (x, z) in sorted(points.items())}
        self.seals = {k: (x - 7, y - 7) for k, (x, y) in self.seal_at.items()}

    @staticmethod
    def px(v):
        return math.floor(v + 0.5 - 1e-6)

    def my(self, z):
        return LANE_Y[1] + z * self.ky

    def at(self, x, z):
        return (mx(x), self.my(z))

    def river_x(self, y):
        return XS[0][1] + self.rf((y - LANE_Y[1]) / self.ky) * KR

    def trees(self):
        """the woodland of CampaignMaps.select: a tree every 13 blocks where the hash of the map allows, clear of the
        roads, the river, the buildings and the points; returned at their card pixels"""
        limit = {2: 13, 3: 55}.get(self.index, 22)
        out = []
        for x in range(-325, 326, 13):
            for z in range(-225, 226, 13):
                if (x * 7343 + z * 9129) % 97 >= limit:
                    continue
                if min(dist_seg(x, z, a, b, c, d) - w for (a, b, c, d, w) in self.roads) < 10:
                    continue
                if abs(x - self.rf(z)) < 17:
                    continue
                if any(abs(x - sx) < w // 2 + 9 and abs(z - sz) < d // 2 + 9 for (_, sx, sz, w, d) in self.structs):
                    continue
                if any(math.hypot(x - px, z - pz) < 30 for (px, pz) in self.points.values()):
                    continue
                out.append(self.at(x, z))
        return out


# ---------------------------------------------------------------- the river
ICE = ['#7f8a8c', '#98a3a3', '#b3bbb8', '#cad0cb', '#e2e4dc']     # the frozen gorge: a cool pale wash, cracked


def river(S, F, half, ice=False):
    """the Rivergate river at the half width of this map (v4a_map.river with the riverX of the map); frozen, the wash
    is the pale ice ramp, the glints become cracks and the banks are cliffs, short hachures falling to the ice"""
    pal = ICE if ice else RIVER
    water = set()
    for y in range(Y0, Y1 + 1):
        xc = F.river_x(y)
        for x in range(math.floor(xc - half) - 1, math.ceil(xc + half) + 2):
            if abs(x - xc) <= half and inside(x, y):
                water.add((x, y))
    for (x, y) in water:
        t = (x - F.river_x(y)) / half
        S.set(x, y, pal[3] if t < -0.55 else (pal[1] if t > 0.4 else pal[2]))
    for (x, y) in water:
        if (x - 1, y) not in water and inside(x - 1, y):
            S.set(x - 1, y, INK[3])
        if (x + 1, y) not in water and inside(x + 1, y):
            S.set(x + 1, y, INK[2])
    for y in range(Y0, Y1 + 1):
        xc = F.river_x(y)
        lo, hi = math.floor(xc - half), math.ceil(xc + half)
        if ice:
            # the gorge: a hachure every other row falling from each rim to the ice
            if y % 2 == 0:
                for x in (lo - 3, lo - 2):
                    if inside(x, y) and (x, y) not in water:
                        S.set(x, y, INK[4] if x == lo - 2 else Q[0])
                for x in (hi + 2, hi + 3):
                    if inside(x, y) and (x, y) not in water:
                        S.set(x, y, INK[4] if x == hi + 2 else Q[0])
            # cracks across the ice and a bright sheen under the lit bank
            if y % 7 == 2:
                for i in range(3):
                    k = (round(xc - 1 + i), y + (i % 2))
                    if k in water:
                        S.set(k[0], k[1], pal[0])
            k = (round(xc - half * 0.55), y)
            if (y % 5) in (0, 1, 2) and k in water:
                S.set(k[0], y, pal[4])
            continue
        for x in (lo - 3, hi + 3):
            if y % 4 != 3 and inside(x, y):
                S.set(x, y, Q[1])
        for (off, ph) in ((-0.9, 0), (0.6, 3)):
            k = (round(xc + off), y)
            if (y + ph) % 6 in (0, 1) and k in water:
                S.set(k[0], y, pal[4] if off < 0 else pal[3])
    return water


def plank_bridge(S, F, y, half, x0=None, x1=None, pal=RIVER):
    """v4a_map.plank_bridge on this river: a plank deck railed both sides, a post at each corner, its shadow on the
    water below; x0 and x1 run it on past the banks where a seal stands on the bridge"""
    xc = F.river_x(y)
    if x0 is None:
        x0, x1 = math.floor(xc - half) - 1, math.ceil(xc + half) + 1
    for x in range(x0, x1 + 1):
        S.set(x, y - 2, INK[1]); S.set(x, y + 2, INK[1])
        for yy in (y - 1, y, y + 1):
            S.set(x, yy, WOOD[4] if yy == y - 1 else (WOOD[3] if x % 2 else WOOD[2]))
        if abs(x - xc) <= half:
            S.set(x, y + 3, pal[0])
    for x in (x0, x1):
        S.set(x, y - 3, INK[1]); S.set(x, y + 3, INK[1])


def ford(S, F, y, half, water, x0=None, x1=None, pal=RIVER):
    """v4a_map.ford on this river: set stones across the stream, water between the stones on its edges, the
    downstream lip in shadow; run on past the banks, the stones pave the approach"""
    xc = F.river_x(y)
    if x0 is None:
        x0, x1 = math.floor(xc - half) - 1, math.ceil(xc + half) + 1
    for x in range(x0, x1 + 1):
        S.set(x, y - 2, INK[3]); S.set(x, y + 2, INK[2])
        S.set(x, y, STONE[4] if x % 3 else STONE[3])
        for yy in (y - 1, y + 1):
            gap = pal[1] if (x, yy) in water or (x, y) in water else Q[1]
            S.set(x, yy, STONE[3] if (x + (yy > y)) % 2 else gap)


# ---------------------------------------------------------------- roads
def lanes(S, paths, water):
    """v4a_map.lanes for any set of paths: a pale bed between two ruled edges"""
    bed, rim = set(), set()
    for p in paths:
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


def track(S, path, avoid):
    """a cart track: a dotted line in the light lane ink, quieter than a lane"""
    pts = []
    for (ax, ay), (bx, by) in zip(path, path[1:]):
        n = max(1, round(max(abs(bx - ax), abs(by - ay))))
        for i in range(n + 1):
            p = (round(ax + (bx - ax) * i / n), round(ay + (by - ay) * i / n))
            if p not in pts:
                pts.append(p)
    for i, (x, y) in enumerate(pts):
        if i % 2 == 0 and inside(x, y) and (x, y) not in avoid:
            S.set(x, y, INK[4])
    return {(x + a, y + b) for (x, y) in pts for a in (-1, 0, 1) for b in (-1, 0, 1)}


# ---------------------------------------------------------------- buildings
KPAL = {'o': INK[1], 'W': STONE[4], 'S': STONE[3], 's': STONE[2], 'g': INK[0], 'L': WOOD[3], 'D': WOOD[1]}
BARRACKS = ['o.o.o.o.o',
            'ooooooooo',
            'oWWWWWWso',
            'oWgWWWgso',
            'oWgWWWgso',
            'ooooooooo']
LONG = ['..oooooooo..',
        '.oLLLLLLLLo.',
        'oDDDDDDDDDDo',
        '.owwwwdwwso.',
        '.owwwwdwwso.',
        '.oooooooooo.']
TOWER = ['o.o.o',
         'ooooo',
         'oWSso',
         '.oSo.',
         '.oWo.',
         '.oSo.',
         'oWSso',
         'ooooo']
RUIN = ['..o......',
        '.oWo.....',
        'oWWSo..o.',
        'oWgSo.oSo',
        'oWgSoooSo',
        'oWWSWWsSo',
        'ooooooooo']
HAY = ['.oo.', 'oLLo', 'oDDo']
LOGS = ['.o.o.o.', 'oLoLoLo', '.oLoLo.', '..o.o..']
REED = ['x.x', 'x.x', '.x.']


def walls(S, x, y0, y1, cracks=(), gate=None, taken=None, west=True):
    """a curtain wall running north to south, seen from above: a stone walk two wide (lit on the west, shaded on the
    east) between ink lines as the forts and the keep are drawn, its merlons in a softer ink on the side the attack
    comes from, its ends squared off. A crack is a breach a row wide with rubble spilt either side; a gate is the gap
    the lane goes through, rubble beside it"""
    for y in range(y0, y1 + 1):
        if y in cracks or (gate and gate[0] <= y <= gate[1]):
            continue
        S.set(x - 1, y, INK[1]); S.set(x, y, STONE[4] if y % 3 else STONE[3]); S.set(x + 1, y, STONE[2])
        S.set(x + 2, y, INK[1])
        if y % 2 == 0:
            S.set(x - 2 if west else x + 3, y, INK[3])
    ends = [y0 - 1, y1 + 1] + ([gate[0] - 1, gate[1] + 1] if gate else []) + [c + d for c in cracks for d in (-1, 1)]
    for y in ends:
        if y0 - 1 <= y <= y1 + 1:
            for i in (-1, 0, 1, 2):
                S.set(x + i, y, INK[1])
    for y in cracks:
        S.set(x - 2, y, STONE[3]); S.set(x + 3, y, STONE[2])
    if gate:
        for (i, j, c) in ((-3, gate[0], STONE[3]), (3, gate[0] + 1, STONE[2]), (-2, gate[1], STONE[2]),
                          (4, gate[1] - 1, STONE[3])):
            S.set(x + i, j, c)
    if taken is not None:
        taken |= rect(x - 3, y0 - 2, 8, y1 - y0 + 5)


def keep(S, x0, y0, x1, y1, taken, gaps=()):
    """the Ironhold keep south of the Iron Gate: a tower at its two south corners, the curtain between them broken
    where the river runs through (gaps: x ranges), its two side walls running up to the lane"""
    for x in range(x0, x1 + 1):
        if not any(a <= x <= b for (a, b) in gaps):
            S.set(x, y1 - 1, INK[1]); S.set(x, y1, STONE[3]); S.set(x, y1 + 1, INK[1])
            if x % 2 == 0:
                S.set(x, y1 + 2, INK[1])
    for (a, b) in gaps:
        for y in (y1 - 1, y1, y1 + 1):
            S.set(a - 1, y, INK[1]); S.set(b + 1, y, INK[1])
    for y in range(y0, y1 + 1):
        for (x, c) in ((x0, STONE[4]), (x1, STONE[3])):
            S.set(x - 1, y, INK[1]); S.set(x, y, c); S.set(x + 1, y, INK[1])
    for x in (x0, x1):
        for i in (-1, 0, 1):
            S.set(x + i, y0 - 1, INK[1])
    for cx in (x0, x1):
        stamp(S, cx - 2, y1 - 3, ['o.o.o', 'ooooo', 'oWWSo', 'oWSso', 'oSsso', 'ooooo'], KPAL)
    taken |= rect(x0 - 3, y0 - 2, x1 - x0 + 7, y1 - y0 + 6)


def mound(S, cx, cy, taken, r0=9.0, r1=13.0, n=40):
    """the raised ground of an outpost: radial hachures falling outward all round its seal, firm on the shaded
    lower right, faint on the lit upper left; drawn under the lanes, which cross it"""
    for k in range(n):
        a = (k + 0.5) * 2 * math.pi / n
        dx, dy = math.cos(a), math.sin(a) * 0.8
        lit = -dx - dy
        c = Q[1] if lit > 0.6 else (Q[0] if lit > -0.2 else INK[5])
        r = r0 + (k % 2) * 0.8
        while r <= r1 - (k % 2) * 1.2:
            x, y = round(cx + dx * r), round(cy + dy * r)
            if inside(x, y) and (x, y) not in taken:
                S.set(x, y, c)
            r += 0.7


def hamlet(S, taken, dx):
    """the four houses either side of the north lane west of A, as Rivergate draws them, moved with the seal"""
    yn = LANE_Y[0]
    for (x, y, b) in ((24, yn - 9, HOUSE), (33, yn - 8, HUT), (27, yn + 3, HUT), (33, yn + 3, HOUSE)):
        stamp(S, x + dx, y, b, BPAL, taken=taken)


def mill_farm(S, F, half, taken, dx, wy):
    """the mill on the bank north of the plank bridge with its wheel in the stream, the farm strips in their hedge"""
    wx = round(F.river_x(wy) - half) - 3
    stamp(S, wx - 8, wy - 3, MILL, BPAL, taken=taken)
    wheel(S, wx, wy)
    taken |= rect(wx - 3, wy - 4, 7, 8)
    fx, fy, fw, fh = 21 + dx, Y0 + 2, 10, 3
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


def shrine(S, x, y, taken):
    """v4a_map.shrine at (x, y): the hill in the ground tone, the shrine in ink on it"""
    S.grid(x - 2, y + 6, ['....qqqqq....', '..qq.....ddd.', 'qq.........dd'], QPAL)
    stamp(S, x, y, SHRINE, SPAL, taken=taken)
    taken |= rect(x - 3, y + 5, 15, 5)


def reeds(S, F, half, taken, reach=11, step=3):
    """the reed beds of a marsh crossing: tufts along both banks within reach rows of each lane, wherever one fits"""
    for yl in LANE_Y:
        for y in range(yl - reach, yl + reach + 1, step):
            xc = F.river_x(y)
            for x in (math.floor(xc - half) - 6 - (y % 2), math.ceil(xc + half) + 4 + (y % 2)):
                cs = cells(x, y, REED)
                ring = {(cx + a, cy + b) for (cx, cy) in cs for a in (-1, 0, 1) for b in (-1, 0, 1)}
                if not (ring & taken) and all(inside(*c) for c in cs):
                    S.grid(x, y, REED, {'x': Q[0]})
                    taken |= cs


# ---------------------------------------------------------------- the quiet ground
MOUNT_L = ['......q......',
           '.....q.d.....',
           '....q..dd....',
           '...q...ddd...',
           '..q....dddd..',
           '.q.....ddddd.',
           'q......dddddd']
SNOW = ['.......q.......',
        '......q.q......',
        '.....q...q.....',
        '....q..d.dd....',
        '...q...ddddd...',
        '..q....dddddd..',
        '.q.....ddddddd.',
        'q......dddddddd']
PEAKS = dict(QPAL, q=INK[5], d=Q[0])      # the pass: its peaks and ridges a notch firmer than the quiet ground
SNOW_S = ['....q....',
          '...q.q...',
          '..q..dd..',
          '.q...ddd.',
          'q....dddd']
SPRUCE = ['..b..',
          '.bbc.',
          '..bc.',
          '.bbcc',
          'bbbcc',
          '..c..']


def ground(S, keep, mountains=(), hills=(), trees=(), copses=14, tree=TREE, big=9, mid=5, seed=7, relief=QPAL):
    """v4a_map.ground_texture with the ground of this map: its mountains and hills where they fit, then its woods
    gathered into copses where they stand thickest, all kept a pixel clear of everything that matters (keep)"""
    def fits(x, y, rows, pad=1):
        cs = cells(x, y, rows)
        if not all(inside(*c) for c in cs):
            return False
        ring = {(cx + a, cy + b) for (cx, cy) in cs for a in range(-pad, pad + 1) for b in range(-pad, pad + 1)}
        return not (ring & keep)

    def put(x, y, rows, pad=1, pal=QPAL):
        if fits(x, y, rows, pad):
            S.grid(x, y, rows, pal)
            keep.update(cells(x, y, rows))
            return True
        return False
    for (x, y, m) in mountains:
        put(x, y, m, pal=relief)
    for (x, y, h) in hills:
        put(x, y, h, pal=relief)
    groups = {}
    for (x, y) in trees:
        groups.setdefault((int(x // 16), int(y // 10)), []).append((x, y))
    rnd = random.Random(seed)
    for (k, pts) in sorted(groups.items(), key=lambda kv: (-len(kv[1]), kv[0]))[:copses]:
        x = round(sum(p[0] for p in pts) / len(pts)) - 2 + rnd.randint(-1, 1)
        y = round(sum(p[1] for p in pts) / len(pts)) - 2 + rnd.randint(-1, 1)
        n = 3 if len(pts) >= big else (2 if len(pts) >= mid else 1)
        offs = rnd.choice((((0, 0), (4, 1), (2, -3)), ((0, 0), (4, -1), (1, 3)), ((0, 0), (-4, 1), (-1, -3))))
        placed = 0
        for (dx, dy) in offs + ((0, 2), (2, 0), (-2, 0)):
            if placed >= n:
                break
            if put(x + dx, y + dy, tree, pad=0 if placed else 1):
                placed += 1


# ---------------------------------------------------------------- the cards
def sheet(F, seed, half, ice=False):
    S, paper = parchment(MAP_W, MAP_H, kind='map', seed=seed, shadow=(1, 1), deckle=False, rim=2, stains=0,
                         fibres=0.45, foxing=0.3, tears=False)
    neatline(S, MAP_W, MAP_H)
    water = river(S, F, half, ice)
    taken = {(x + a, y) for (x, y) in water for a in (-2, -1, 0, 1, 2)}
    if ice:
        taken |= {(x + a, y) for (x, y) in water for a in (-3, 3)}
    return S, taken, water


def seals(S, F, taken):
    for (sx, sy) in F.seals.values():
        taken |= rect(sx - 1, sy - 1, 19, 19)


def press(S, F):
    for k, (sx, sy) in F.seals.items():
        S.blit(relief_seal(sx + sy), sx, sy)
    for k, (cx, cy) in F.seal_at.items():
        letter(S, k, cx - 2, cy - 3)
    return S


def map_rivergate():
    S = v4a_map.rivergate_map()
    for (k, x, y) in (('A', 47, 16), ('B', 65, 37), ('C', 84, 58)):
        letter(S, k, x, y)
    return S


def map_ironhold():
    F = Field(1)
    half = 2.6
    S, taken, water = sheet(F, 22, half)
    taken |= lanes(S, lane_paths(), water)
    yn, ym, ys = LANE_Y
    plank_bridge(S, F, yn, half)
    stone_bridge(S, ym, F.seal_at['B'][0] - 11, F.seal_at['B'][0] + 11)
    ford(S, F, ys, half, water)
    seals(S, F, taken)
    fort(S, mx(-314), TEAL, taken)
    fort(S, mx(314), VIOL, taken)
    # the curtains between the lanes on both flanks, the gate road open between them; the wall breached across the
    # middle lane west of the Iron Gate; the keep south of B
    xw, xe = round(mx(-72)), round(mx(72))
    for (y0, y1) in ((24, 35), (45, 56)):
        walls(S, xw, y0, y1, taken=taken, west=True)
        walls(S, xe, y0, y1, taken=taken, west=False)
    walls(S, round(mx(-37)) - 1, 32, 48, gate=(38, 42), taken=taken, west=True)
    bx = F.seal_at['B'][0]
    rv = (math.floor(F.river_x(52) - half) - 1, math.ceil(F.river_x(52) + half) + 1)
    keep(S, bx - 12, ym + 5, bx + 12, ym + 12, taken, gaps=(rv,))
    hamlet(S, taken, -1)
    # the supply yard: the barn and its hay north of the hamlet
    stamp(S, 22, Y0 + 2, LONG, dict(BPAL, w=STONE[4], s=STONE[2], d=INK[1]), taken=taken)
    for (x, y) in ((36, Y0 + 3), (41, Y0 + 3)):
        stamp(S, x, y, HAY, BPAL, taken=taken)
    # the garrison east of C, below the south lane
    cx, cy = F.seal_at['C']
    stamp(S, cx + 12, cy + 3, BARRACKS, KPAL, taken=taken)
    ground(S, taken,
           mountains=((75, 4, MOUNT), (84, 5, MOUNT_S), (91, 4, MOUNT), (100, 5, MOUNT_S),
                      (52, 71, MOUNT), (45, 72, MOUNT_S), (36, 71, MOUNT), (29, 72, MOUNT_S)),
           hills=((28, 27, HILL), (100, 27, HILL), (28, 49, HILL_S), (100, 47, HILL)),
           trees=F.trees(), copses=12)
    return press(S, F)


def map_frostmarch():
    F = Field(2)
    half = 2.6
    S, taken, water = sheet(F, 23, half, ice=True)
    taken |= lanes(S, lane_paths(), water)
    yn, ym, ys = LANE_Y
    plank_bridge(S, F, yn, half, pal=ICE)
    stone_bridge(S, ym, F.seal_at['B'][0] - 11, F.seal_at['B'][0] + 11)
    ford(S, F, ys, half, water, pal=ICE)
    seals(S, F, taken)
    fort(S, mx(-314), TEAL, taken)
    fort(S, mx(314), VIOL, taken)
    hamlet(S, taken, 0)
    ax, ay = F.seal_at['A']
    cx, cy = F.seal_at['C']
    # the caravan lodge north of the hamlet, its track down to the north crossing
    taken |= track(S, [(34, 9), (44, 11), (57, 16)], taken)
    stamp(S, 21, Y0 + 2, LONG, dict(BPAL, w=STONE[4], s=STONE[2], d=INK[1]), taken=taken)
    # the pass refuge south of the middle lane east of B, its track to the lane
    rx, ry = round(mx(62)), round(F.my(34))
    taken |= track(S, [(rx, ry + 2), (rx - 6, ym + 3), (round(mx(25)), ym + 2)], taken)
    stamp(S, rx - 1, ry + 1, HOUSE, BPAL, taken=taken)
    # the signal tower below the south lane east of C, the shrine above it
    stamp(S, cx + 13, cy + 2, TOWER, KPAL, taken=taken)
    shrine(S, cx + 12, cy - 13, taken)
    ground(S, taken,
           mountains=((74, 4, SNOW), (86, 5, SNOW_S), (95, 4, SNOW), (108, 5, SNOW_S), (47, 4, SNOW_S),
                      (33, 68, SNOW), (46, 70, SNOW_S), (21, 70, SNOW_S), (55, 68, SNOW), (86, 70, SNOW_S),
                      (97, 69, SNOW_S)),
           hills=((27, 25, SNOW_S), (36, 28, MOUNT_S), (99, 25, SNOW_S), (91, 28, MOUNT_S),
                  (27, 47, MOUNT_S), (36, 50, SNOW_S), (101, 49, MOUNT_S)),
           trees=F.trees(), copses=10, tree=SPRUCE, relief=PEAKS)
    return press(S, F)


def map_hollowwood():
    F = Field(3)
    half = 3.9
    S, taken, water = sheet(F, 24, half)
    taken |= lanes(S, lane_paths(), water)
    yn, ym, ys = LANE_Y
    plank_bridge(S, F, yn, half)
    stone_bridge(S, ym, F.seal_at['B'][0] - 12, F.seal_at['B'][0] + 12)
    ford(S, F, ys, half, water)
    seals(S, F, taken)
    fort(S, mx(-314), TEAL, taken)
    fort(S, mx(314), VIOL, taken)
    hamlet(S, taken, -1)
    cx, cy = F.seal_at['C']
    # the sawmill and its log pile north of the hamlet, its track to the north crossing
    taken |= track(S, [(33, 9), (44, 12), (52, 15)], taken)
    stamp(S, 21, Y0 + 2, LONG, dict(BPAL, w=STONE[4], s=STONE[2], d=INK[1]), taken=taken)
    stamp(S, 34, Y0 + 3, LOGS, BPAL, taken=taken)
    # the broken chapel north of C, its track down to the ford; the moon shrine east of it, above the south lane
    taken |= track(S, [(cx + 4, cy - 10), (cx + 2, cy - 7)], taken)
    stamp(S, cx + 1, cy - 17, RUIN, KPAL, taken=taken)
    shrine(S, cx + 12, cy - 13, taken)
    # the reed beds of the marsh crossings
    reeds(S, F, half, taken)
    ground(S, taken,
           mountains=((75, 4, MOUNT), (84, 5, MOUNT_S), (52, 71, MOUNT), (45, 72, MOUNT_S)),
           hills=((30, 28, HILL_S), (97, 28, HILL_S), (30, 48, HILL_S)),
           trees=F.trees(), copses=44, tree=SPRUCE, big=5, mid=3)
    return press(S, F)


def map_kingsfield():
    F = Field(4)
    half = 2.6
    S, taken, water = sheet(F, 25, half)
    ax, ay = F.seal_at['A']
    bx, by = F.seal_at['B']
    cx, cy = F.seal_at['C']
    dx, dy = F.seal_at['D']
    ex, ey = F.seal_at['E']
    # the roads that climb to the outposts leave the fork roads (drawn here from the flank lanes)
    west = [(22.6, by), (bx, by), (mx(-28), LANE_Y[1])]
    east = [(MAP_W - 1 - 22.6, dy), (dx, dy), (mx(28), LANE_Y[1])]
    mound(S, bx, by, taken)
    mound(S, dx, dy, taken)
    taken |= lanes(S, lane_paths() + [west, east], water)
    yn, ym, ys = LANE_Y
    plank_bridge(S, F, yn, half, ax - 11, ax + 11)
    stone_bridge(S, ym, cx - 11, cx + 11)
    ford(S, F, ys, half, water, ex - 11, ex + 11)
    seals(S, F, taken)
    for (x, y) in (F.seal_at['B'], F.seal_at['D']):
        taken |= {(x + a, y + b) for a in range(-13, 14) for b in range(-11, 12) if (a / 13.0) ** 2 + (b / 11.0) ** 2 <= 1}
    fort(S, mx(-314), TEAL, taken)
    fort(S, mx(314), VIOL, taken)
    hamlet(S, taken, 8)
    mill_farm(S, F, half, taken, 8, Y0 + 4)
    stamp(S, cx - 19, cy + 7, HOUSE, BPAL, taken=taken)
    stamp(S, cx + 12, cy - 13, BARRACKS, KPAL, taken=taken)
    shrine(S, ex + 11, ey - 12, taken)
    ground(S, taken,
           mountains=((75, 4, MOUNT), (84, 5, MOUNT_S), (91, 4, MOUNT), (100, 5, MOUNT_S),
                      (52, 71, MOUNT), (45, 72, MOUNT_S), (36, 71, MOUNT), (29, 72, MOUNT_S)),
           hills=((30, 27, HILL), (97, 49, HILL)),
           trees=F.trees(), copses=14, tree=SPRUCE)
    return press(S, F)


MAPS = {
    'table/map-rivergate': map_rivergate,
    'table/map-ironhold': map_ironhold,
    'table/map-frostmarch': map_frostmarch,
    'table/map-hollowwood': map_hollowwood,
    'table/map-kingsfield': map_kingsfield,
}
