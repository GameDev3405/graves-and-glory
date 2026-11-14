"""Hollowwood "The Hollow": blockout of the whole map. Three levels: the wood floor (72), the Hollow (62), the High Walk (78). See ../plan.md.

  python hwmap.py render OUT.png top|iso [x0 z0 x1 z1]
  python hwmap.py check

x east, z south. Half-turn symmetric: the cell (x, z) is the cell (-x, -z). Every column is a list of slabs (y0, y1, block, walk);
a troop stands on the top of a slab whose walk is True."""
import sys, os, math, heapq, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))

X0, X1, Z0, Z1 = -288, 287, -192, 191
G, PIT, HI = 72, 62, 78
POINTS = {'A': (-128, -96), 'B': (0, 0), 'C': (128, 96)}
SPAWN = {'dawn': (-236, 124), 'dusk': (236, -124)}
ESCORT = [('start', (-100, -98)), ('stop 1', (-44, -50)), ('stop 2', (0, 0)), ('stop 3', (44, 50)), ('end', (100, 98))]
R_FLOOR, R_LEDGE = 45.5, 52.5                                                   # ramp ledges lie between; the lip starts at R_LEDGE
STUMP = (-128, -96, 40, 34)                                                     # A: cx, cz, rx, rz (C is the twin)
SPLITS = [(-22, 16), (42, -4)]                                                  # directions of the two splits in A's bark rim
NEAR = [(-236, 124), (-222, 70), (-196, 20), (-170, -40), (-150, -80), (-128, -96)]
RIM = [(-196, 20), (-150, 24), (-110, 14), (-78, 6), (-60, 0)]
SOUTH = [(-236, 124), (-180, 136), (-120, 130), (-60, 112), (-10, 96), (40, 100), (90, 104), (128, 96)]
STUMPP = [(-128, -96), (-84, -100), (-60, -70), (-38, -46)]
HIGH = [(-106, -84), (-84, -66), (-64, -46), (-52, -32)]
DECK = [(-52, -32), (-62, 0), (-52, 32)]
UP = [(-150, 8), (-120, 0), (-92, -20), (-62, 0)]
BRIDGE = [(-52, -32), (-20, -40), (20, -40), (52, -32)]
GULLY = [(-10, 96), (-34, 98), (-46, 84), (-34, 70)]                 # leaves the long road south of the Hollow, so the Root Run is no quicker than the ramps (Kort: 'Even them out')
TUN = [(-34, 70), (-18, 40)]
GIANT = [(-178, 38), (-140, 60), (-92, 86)]
HOLLOWS = [(-159, 49), (-116, 73)]
STAIR = (-166, -146, 5, 11)                                                     # x0, x1, z0, z1: rises east, 78 from x -154
CAMP = (-264, -208, 98, 150)
DECKTREES = [(-89, -60, 5), (-70, -41, 5), (-121, -9, 5), (-97, -27, 5)]            # giant trunks the High Walk wraps round: x, z, radius
STAIRTREE = (-150, -1, 5)


def turn(pts): return [(-x, -z) for (x, z) in pts]


def band(pts, w):
    """cells within w of the polyline: {(x, z): (distance, arc length at the nearest point)}"""
    out = {}; s0 = 0.0
    for (ax, az), (bx, bz) in zip(pts, pts[1:]):
        dx, dz = bx - ax, bz - az; L2 = dx * dx + dz * dz; L = math.sqrt(L2)
        for x in range(int(min(ax, bx) - w) - 1, int(max(ax, bx) + w) + 2):
            for z in range(int(min(az, bz) - w) - 1, int(max(az, bz) + w) + 2):
                t = max(0.0, min(1.0, ((x - ax) * dx + (z - az) * dz) / L2)); d = math.hypot(x - ax - t * dx, z - az - t * dz)
                if d <= w and d < out.get((x, z), (1e9,))[0]: out[(x, z)] = (d, s0 + t * L)
        s0 += L
    return out


def both(f, pts, w): a = f(pts, w); a.update({k: v for k, v in f(turn(pts), w).items() if k not in a}); return a


class Map:
    def __init__(self, closed=()):
        self.closed = set(closed); self.col = {}; self.kind = {}
        self.road = set(both(band, NEAR, 3.5)) | set(both(band, SOUTH, 3.5)) | set(both(band, RIM, 2.5)) | set(both(band, STUMPP, 2.5))
        self.gully = band(GULLY, 2.5); self.glen = sum(math.dist(a, b) for a, b in zip(GULLY, GULLY[1:])); self.tun = band(TUN, 2.5)
        self.giant = band(GIANT, 5.5)
        deck = {}
        for pts in (HIGH, DECK, UP, BRIDGE):
            for k, v in both(band, pts, 3.5).items():
                if v[0] < deck.get(k, 9): deck[k] = v[0]
        self.deck = deck
        clear = set(both(band, NEAR, 9)) | set(both(band, SOUTH, 9)) | set(both(band, RIM, 8)) | set(both(band, STUMPP, 8)) | set(both(band, GULLY, 9)) | set(both(band, TUN, 9)) | set(both(band, GIANT, 12))
        for pts in (HIGH, DECK, UP, BRIDGE): clear |= set(both(band, pts, 9))
        self.trunks = []; rnd = random.Random(2411)
        for _ in range(900):                                                      # giant trunks, in twin pairs
            x, z, r = rnd.randint(-276, 0), rnd.randint(-180, 180), rnd.choice((4, 5, 5, 6, 7))
            if any((x + i, z + j) in clear for i in (-r, 0, r) for j in (-r, 0, r)) or math.hypot(x, z) < 66 + r: continue
            if any(math.hypot((sx * x - STUMP[0]) / (STUMP[2] * 1.3 + r), (sx * z - STUMP[1]) / (STUMP[3] * 1.3 + r)) < 1 for sx in (1, -1)): continue
            if any(CAMP[0] - r - 3 <= sx * x <= CAMP[1] + r + 3 and CAMP[2] - r - 3 <= sx * z <= CAMP[3] + r + 3 for sx in (1, -1)): continue
            if STAIR[0] - r - 6 <= x <= STAIR[1] + r + 6 and STAIR[2] - r - 6 <= z <= STAIR[3] + r + 6: continue
            if abs(x / 282.0) ** 6 + abs(z / 186.0) ** 6 > 0.9: continue
            if any(math.hypot(x - tx, z - tz) < r + tr + 5 for tx, tz, tr in self.trunks) or math.hypot(2 * x, 2 * z) < 2 * r + 5: continue
            self.trunks += [(x, z, r), (-x, -z, r)]
        for tx, tz, r in DECKTREES:                                               # a round deck with a rail about each deck tree
            for sx in (1, -1):
                self.trunks.append((sx * tx, sx * tz, r))
                for x in range(tx - r - 5, tx + r + 6):
                    for z in range(tz - r - 5, tz + r + 6):
                        d = math.hypot(x - tx, z - tz)
                        if d <= r + 4.5: deck[(sx * x, sx * z)] = min(deck.get((sx * x, sx * z), 9), max(0.0, d - r - 1))
        self.trunks += [STAIRTREE, (-STAIRTREE[0], -STAIRTREE[1], STAIRTREE[2])]
        tr = {}
        for tx, tz, r in self.trunks:
            for x in range(tx - r, tx + r + 1):
                for z in range(tz - r, tz + r + 1):
                    if math.hypot(x - tx, z - tz) <= r + 0.3: tr[(x, z)] = 1
        self.tr = tr
        for x in range(X0, X1 + 1):
            for z in range(Z0, Z1 + 1):
                a = self.cell(x, z, 1); b = self.cell(-x, -z, -1)
                rank = lambda v: {'wood': 0, 'road': 1, 'pit': 2}.get(v[0], 3)
                k, slabs = a if rank(a) >= rank(b) else b                         # a feature of either half wins over plain ground
                slabs = list(slabs); top = max(s[1] for s in slabs)
                if (x, z) in deck and (k in ('wood', 'road', 'pit', 'ledge', 'gully', 'tunnel', 'ramp') and min(s[1] for s in slabs if s[3]) <= HI - 4 and top <= HI - 4):
                    if deck[(x, z)] <= 2.5: slabs.append((HI, HI, 'spruce_planks', 'bridges' not in self.closed or math.hypot(x, z) > R_LEDGE))
                    else: slabs.append((HI, HI + 1, 'stripped_spruce_log', False))
                self.col[(x, z)] = slabs; self.kind[(x, z)] = k

    def cell(self, x, z, sg):
        """the column at (x, z) as Dawn's half defines it"""
        ground = lambda h, b='moss_block': [(None, h, b, True)]
        solid = lambda h, b: [(None, h, b, False)]
        if CAMP[0] <= x <= CAMP[1] and CAMP[2] <= z <= CAMP[3]:
            edge = x in (CAMP[0], CAMP[1]) or z in (CAMP[2], CAMP[3])
            gate = (z == CAMP[2] and -234 <= x <= -224) or (x == CAMP[1] and 125 <= z <= 135)
            return ('wall', solid(G + 4, 'spruce_log')) if edge and not gate else ('road', ground(G, 'coarse_dirt'))
        if abs(x / 282.0) ** 6 + abs(z / 186.0) ** 6 > 1: return 'deep', solid(G + 24, 'dark_oak_leaves')
        r = math.hypot(x, z); th = math.degrees(math.atan2(z, x)) % 360
        if r <= R_LEDGE:
            if r > R_FLOOR and 125 <= th <= 215 and 'ramps' not in self.closed:
                return 'ledge', ground(G - min(10, int((215 - th) / 90 * 10.999)), 'rooted_dirt')
            return 'pit', ground(PIT, 'grass_block')
        if (x, z) in self.tun and 'runs' not in self.closed:
            return 'tunnel', [(None, PIT, 'rooted_dirt', True), (PIT + 5, G, 'moss_block', True)]
        if (x, z) in self.gully and 'runs' not in self.closed:
            return 'gully', ground(G - min(10, int(self.gully[(x, z)][1] / self.glen * 10.999)), 'rooted_dirt')
        cx, cz, rx, rz = STUMP; dx, dz = x - cx, z - cz; f = math.hypot(dx / rx, dz / rz)
        if f <= 1.22:
            for ux, uz in SPLITS:
                n = math.hypot(ux, uz); ux, uz = ux / n, uz / n
                if dx * ux + dz * uz > 0 and abs(dx * uz - dz * ux) <= 4.5 and f >= 0.78:
                    return 'ramp', ground(G + min(6, max(0, int((1.22 - f) / 0.44 * 7))), 'coarse_dirt')
            if f <= 0.82: return 'stump', ground(HI, 'stripped_dark_oak_wood')
            if f <= 1:
                return ('stump', ground(HI, 'stripped_dark_oak_wood')) if self.deck.get((x, z), 9) <= 2.5 else ('bark', solid(HI + 6, 'dark_oak_log'))
        if STAIR[0] <= x <= STAIR[1] and STAIR[2] <= z <= STAIR[3]: return 'ramp', ground(G + min(6, (x - STAIR[0]) // 2), 'spruce_planks')
        if (x, z) in self.giant:
            for hx, hz in HOLLOWS:
                i = min(range(len(GIANT) - 1), key=lambda i: math.dist(GIANT[i], (hx, hz)) + math.dist(GIANT[i + 1], (hx, hz)))
                (ax, az), (bx, bz) = GIANT[i], GIANT[i + 1]; n = math.dist((ax, az), (bx, bz))
                if abs((x - hx) * (bx - ax) / n + (z - hz) * (bz - az) / n) <= 2.5: return 'tunnel', [(None, G, 'rooted_dirt', True), (G + 5, G + 11, 'dark_oak_log', False)]
            return 'trunk', solid(G + 11, 'dark_oak_log')
        if (x * sg, z * sg) in self.tr: return 'trunk', solid(G + 40, 'spruce_log')
        if (x, z) in self.road or (-x, -z) in self.road: return 'road', ground(G, 'coarse_dirt')
        return 'wood', ground(G)

    def floors(self, c): return [s[1] for s in self.col.get(c, ()) if s[3]]

    def walk(self, start):
        """start is (x, z, y of the floor block)"""
        def step(c, h): return next((f for f in self.floors(c) if abs(f - h) <= 1), None)
        d = {start: 0.0}; q = [(0.0, start)]
        while q:
            dc, (x, z, h) = heapq.heappop(q)
            if dc > d[(x, z, h)]: continue
            for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                f = step((x + i, z + j), h)
                if f is None or (i and j and (step((x + i, z), h) is None or step((x, z + j), h) is None)): continue
                nd = dc + (1.414 if i and j else 1.0); n = (x + i, z + j, f)
                if nd < d.get(n, 1e9): d[n] = nd; heapq.heappush(q, (nd, n))
        return {k: round(v) for k, v in d.items()}

    def world(self, lo, hi):
        from mapforge.world import World, Cell
        from mapforge.structure import normalize
        w = World('hollowwood'); put = lambda p, b: w.set(p, Cell(normalize(b)))
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                for y0, y1, b, wk in self.col[(x, z)]:
                    if y0 is None:
                        y0 = min([min(s[1] for s in self.col.get((x + i, z + j), [(None, y1)]) if s[0] is None) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [y1])
                        if x in (lo[0], hi[0]) or z in (lo[2], hi[2]): y0 = lo[1]
                    for y in range(max(lo[1], y0), min(y1, hi[1]) + 1): put((x, y, z), b)
        for n, (px, pz) in POINTS.items():
            for a in range(0, 360, 2):
                x, z = round(px + 17 * math.cos(math.radians(a))), round(pz + 17 * math.sin(math.radians(a))); put((x, self.floors((x, z))[0], z), 'yellow_concrete')
        for n, (px, pz) in ESCORT:
            for i in (-1, 0, 1):
                for j in (-1, 0, 1): put((px + i, self.floors((px + i, pz + j))[0], pz + j), 'red_concrete')
        for n, (px, pz) in SPAWN.items():
            for i in (-1, 0, 1):
                for j in (-1, 0, 1): put((px + i, G + 1, pz + j), 'lime_concrete' if n == 'dawn' else 'blue_concrete')
        return w


def node(m, p): return (p[0], p[1], m.floors(p)[0])


def check(m):
    bad = 0
    for n, (px, pz) in POINTS.items():
        ring = [(x, z) for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17]
        hs = sorted({tuple(m.floors(c)) for c in ring}); ks = sorted({m.kind[c] for c in ring}); n2 = sorted({len(m.col[c]) for c in ring})
        print('point', n, 'floors', hs, 'kinds', ks); bad += len(hs) != 1 or n2 != [1]
    sym = sum(1 for (x, z) in m.col if (-x, -z) in m.col and m.col[(x, z)] != m.col[(-x, -z)]); print('columns that differ from their half-turn twin:', sym); bad += sym
    low = sum(1 for c, sl in m.col.items() for s in sl if s[3] and any(s[1] < t[0] <= s[1] + 3 for t in sl if t[0] is not None)); print('floors with less than 3 clear above:', low); bad += low
    d = m.walk(node(m, SPAWN['dawn'])); e = m.walk(node(m, SPAWN['dusk']))
    targets = [(n, node(m, p)) for n, p in POINTS.items()] + [('the other spawn', None), ('north bridge middle', (0, -40, HI)), ('south bridge middle', (0, 40, HI))]
    for n, p in targets:
        a, b = d.get(p or node(m, SPAWN['dusk'])), e.get(p or node(m, SPAWN['dawn'])); print('walk from dawn to %s: %s | from dusk: %s' % (n, a, b)); bad += a is None or b is None
    for a, b in zip(ESCORT, ESCORT[1:]): print('escort %s to %s: %s' % (a[0], b[0], m.walk(node(m, a[1])).get(node(m, b[1]))))
    off = sum(1 for (x, z, h) in d if m.kind[(x, z)] in ('deep', 'bark', 'trunk', 'wall')); print('reachable cells on walls, bark or trunks:', off); bad += off
    for name, closed in (('ramps only', ('runs',)), ('Root Runs only', ('ramps',)), ('neither', ('ramps', 'runs'))):
        mm = Map(closed); v = mm.walk(node(mm, SPAWN['dawn'])).get(node(mm, POINTS['B'])); print('dawn to B by %s: %s' % (name, v)); bad += (v is None) != (name == 'neither')
    mm = Map(('bridges',)); v = mm.walk(node(mm, SPAWN['dawn'])).get(node(mm, SPAWN['dusk'])); print('dawn to the dusk spawn with the bridges shut:', v)
    print('trunks:', len(m.trunks)); print('FAIL' if bad else 'OK')
    return bad


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'render':
        from mapforge import render
        out, view = sys.argv[2], sys.argv[3]
        box = tuple(int(v) for v in sys.argv[4:8]) if len(sys.argv) >= 8 else (X0, Z0, X1, Z1)
        m = Map(); lo, hi = (box[0], 56, box[1]), (box[2], 140, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Hollowwood blockout %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'check':
        sys.exit(1 if check(Map()) else 0)
