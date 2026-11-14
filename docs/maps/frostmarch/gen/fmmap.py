"""Frostmarch "The Switchback": blockout of the whole map as a function of (x, z). See ../plan.md.

  python fmmap.py render OUT.png top|iso [x0 z0 x1 z1]
  python fmmap.py check

x east, z south. Camps y 64, the Cut 72, shelves 80. Half-turn symmetric: the cell (x, z) is the cell (-x, -z)."""
import sys, os, math, heapq
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
from mapforge.world import World, Cell
from mapforge.structure import normalize
from mapforge.terrain import fbm, seed_of, hash01

X0, X1, Z0, Z1 = -288, 287, -192, 191
SEED = seed_of(2411)
POINTS = {'A': (0, -112), 'B': (0, 0), 'C': (0, 112)}
SPAWN = {'dawn': (-256, 0), 'dusk': (256, 0)}
CONVOY = [('start', (-176, -44)), ('stop 1', (-100, -72)), ('stop 2', (0, 0)), ('stop 3', (100, 72)), ('end', (176, 44))]


def turn(pts): return [(-x, -z) for (x, z) in pts]


NORTH = [(-288, -120), (-250, -132), (-210, -150), (-160, -158), (-110, -160), (-64, -146), (0, -146), (64, -146), (110, -150), (160, -128), (200, -100), (240, -84), (288, -76)]   # foot of the north mountains
ROAD_W = [(-256, 0), (-214, -8), (-176, -44), (-136, -70), (-100, -72), (-70, -58), (-44, -34), (-20, -12), (0, 0)]
LOW_W = [(-232, 6), (-216, 30), (-176, 74), (-130, 104), (-92, 112)]
LINK_W = [(-100, -72), (-96, -100), (-92, -112)]
CROSS_W = [(-136, -70), (-158, -10), (-176, 74)]
ROADS = [(4, ROAD_W), (4, turn(ROAD_W)), (2, LOW_W), (2, turn(LOW_W)), (2, LINK_W), (2, turn(LINK_W)), (2, CROSS_W), (2, turn(CROSS_W))]
RIDGE_W = [(-192, 8, 13, 11), (-168, 2, 17, 13), (-142, -6, 19, 15), (-114, -8, 18, 14), (-90, -4, 15, 12), (-70, 2, 10, 9)]   # the ridge that makes the road swing north: cx, cz, rx, rz
CRAGS = RIDGE_W + [(-cx, -cz, rx, rz) for cx, cz, rx, rz in RIDGE_W]                                 # cx, cz, rx, rz
TARNS = [(-200, -120, 24, 14), (200, 120, 24, 14)]
SHELF = (58, 26)                                                                # half width, half depth, centred on A and C
RAMP = 34                                                                       # ramp length beyond the shelf side
GAPS = (-48, 48)                                                                # narrow Spine gaps, 3 wide
CROSSINGS = ('cut', 'gap n', 'gap s', 'shelf a', 'shelf c')
# blockout buildings: x0, z0, x1, z1, height, block, name (west and north ones; twins are added)
BUILD_W = [(24, -138, 48, -120, 30, 'stone_bricks', 'signal tower'), (-120, -98, -106, -88, 7, 'spruce_planks', 'waystation'),
           (-57, -122, -52, -119, 9, 'stone_bricks', 'gatehouse pier'), (-57, -105, -52, -102, 9, 'stone_bricks', 'gatehouse pier'),
           (57, -122, 52, -119, 9, 'stone_bricks', 'gatehouse pier'), (57, -105, 52, -102, 9, 'stone_bricks', 'gatehouse pier')]
BUILDINGS = BUILD_W + [(-x1, -z1, -x0, -z0, hh, b, n + ' (twin)') for x0, z0, x1, z1, hh, b, n in BUILD_W]


def lerp(pts, t):
    if t <= pts[0][0]: return pts[0][1]
    for (a, va), (b, vb) in zip(pts, pts[1:]):
        if t <= b: return va + (vb - va) * (t - a) / (b - a)
    return pts[-1][1]


def seg_d(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az; L = dx * dx + dz * dz
    t = max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / L)) if L else 0.0
    return math.hypot(px - ax - t * dx, pz - az - t * dz)


def base(x):
    """the saddle: 64 at the camps, one block up every 16 from |x| 188, 72 inside |x| 60"""
    return 64 + int(max(0, min(8, (188 - abs(x)) // 16 + 1 if abs(x) <= 188 else 0)))


class Map:
    def __init__(self, closed=()):
        self.top = {}; self.surf = {}; self.kind = {}; self.closed = closed
        road = set()
        for w, pts in ROADS:
            for (ax, az), (bx, bz) in zip(pts, pts[1:]):
                for x in range(int(min(ax, bx)) - w - 1, int(max(ax, bx)) + w + 2):
                    for z in range(int(min(az, bz)) - w - 1, int(max(az, bz)) + w + 2):
                        if seg_d(x, z, ax, az, bx, bz) <= w + 0.5: road.add((x, z))
        self.road = road
        for x in range(X0, X1 + 1):
            for z in range(Z0, Z1 + 1):
                c = (x, z) if (x < 0 or (x == 0 and z <= 0)) else (-x, -z)
                self.top[(x, z)], self.surf[(x, z)], self.kind[(x, z)] = self.cell(c[0], c[1], 1 if c == (x, z) else -1)
        for x0, z0, x1, z1, hh, b, name in BUILDINGS:
            x0, x1, z0, z1 = min(x0, x1), max(x0, x1), min(z0, z1), max(z0, z1)
            cells = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
            if any(c in road or self.kind[c] != 'ground' for c in cells) and not closed: print('building overlaps a road or rock:', name, x0, z0)
            top = max(self.top[c] for c in cells)
            for c in cells: self.top[c] = top + hh; self.surf[c] = b; self.kind[c] = 'building'
        for sx in (-1, 1):                                                       # camps: palisade with a gate toward the pass
            for x in range(234, 283):
                for z in range(-34, 35):
                    if (x in (234, 282) or abs(z) == 34) and not (x == 234 and abs(z) <= 6):
                        c = (sx * x, z); self.top[c] = 68; self.surf[c] = 'spruce_log'; self.kind[c] = 'wall'

    def cell(self, x, z, sg=1):
        """the cell at (x, z) in the west half; sg = -1 when it stands in for its twin (only the closed crossings care)"""
        g = base(x + fbm(SEED, x / 40.0, z / 40.0, 2) * 16 - 8) if abs(x) > 70 else base(x); r = hash01(SEED, x, z, 3); nz = fbm(SEED, x / 31.0, z / 31.0, 4)
        n = lerp(NORTH, x) + fbm(SEED, x / 23.0, 0.5, 3) * 10 - 5
        s = -lerp(NORTH, -x) - fbm(SEED, -x / 23.0, 0.5, 3) * 10 + 5
        out = max(n - z, z - s)
        if abs(x) <= SHELF[0] + 8 and abs(z) > 112 + SHELF[1]: out = max(out, abs(z) - 112 - SHELF[1])      # the mountain closes behind each shelf
        closed = self.closed
        for pz, name in ((-112, 'shelf a'), (112, 'shelf c')):                    # shelves and their ramps
            dz = z - pz
            if abs(x) <= SHELF[0] and abs(dz) <= SHELF[1]:
                if ('shelf a' if pz * sg < 0 else 'shelf c') in closed and abs(x) <= 2: return 92, 'stone', 'rock'    # a closed shelf: a wall across it
                return 80, 'snow_block', 'ground'
            if SHELF[0] < abs(x) <= SHELF[0] + RAMP and abs(dz) <= 5:
                return max(g, 80 - ((abs(x) - SHELF[0]) * 10 + RAMP - 1) // RAMP), 'gravel' if r < 0.5 else 'stone', 'ground'
            if abs(x) <= SHELF[0] + 2 and abs(dz) <= SHELF[1] + 2 and out <= 0 and not (abs(dz) <= 5): return 80, 'stone', 'rock'   # the retaining rim
        if out > 0:
            h = int(g + 3 + min(out, 60) * 0.9 + nz * 14)
            return h, ('snow_block' if h > 80 or nz > 0.55 else 'stone' if r < 0.6 else 'andesite'), 'rock'
        if abs(x) <= 6 and 18 <= abs(z) <= 86:                                    # the Spine
            gap = next((gz for gz in GAPS if gz - 2 <= z <= gz + 1 or -gz - 1 <= -z <= -gz + 2), None) if 46 <= abs(z) <= 49 else None
            if gap is None or ('gap n' if gap * sg < 0 else 'gap s') in closed:
                k = (abs(z) - 18) % 16
                if abs(x) <= 4 or k < 12: return g + (13 if k < 12 else 10), 'bone_block', 'bone'
        if 'cut' in closed and abs(x) <= 2 and abs(z) < 18: return g + 12, 'stone', 'rock'
        rock = max(1 - math.hypot((x - cx) / rx, (z - cz) / rz) for cx, cz, rx, rz in CRAGS)
        if True:
            if rock > 0 and (x, z) not in self.road: return int(g + 3 + rock * 16), ('snow_block' if rock > 0.45 else 'stone' if r < 0.5 else 'andesite'), 'rock'
        for cx, cz, rx, rz in TARNS:
            t = math.hypot((x - cx) / rx, (z - cz) / rz)
            if t < 1: return g - 1, 'packed_ice' if r < 0.8 else 'blue_ice', 'ice'
            if t < 1 + 3.0 / min(rx, rz): return g + 2, 'stone' if r < 0.5 else 'snow_block', 'rock'
        if (x, z) in self.road: return g, ('gravel' if r < 0.45 else 'stone' if r < 0.75 else 'andesite'), 'ground'
        return g, 'snow_block', 'ground'

    def walk(self, start):
        ok = lambda n, c: n in self.top and self.kind[n] == 'ground' and abs(self.top[n] - self.top[c]) <= 1
        d = {start: 0.0}; q = [(0.0, start)]
        while q:
            dc, c = heapq.heappop(q)
            if dc > d[c]: continue
            for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                n = (c[0] + i, c[1] + j)
                if not ok(n, c) or (i and j and not (ok((c[0] + i, c[1]), c) and ok((c[0], c[1] + j), c))): continue
                nd = dc + (1.414 if i and j else 1.0)
                if nd < d.get(n, 1e9): d[n] = nd; heapq.heappush(q, (nd, n))
        return {k: round(v) for k, v in d.items()}

    def world(self, lo, hi):
        w = World('frostmarch')
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h = self.top[(x, z)]
                low = min([self.top.get((x + i, z + j), h) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h])
                if x in (lo[0], hi[0]) or z in (lo[2], hi[2]): low = lo[1]
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    w.set((x, y, z), Cell(normalize(self.surf[(x, z)] if y == h or self.kind[(x, z)] != 'ground' else 'stone')))
        for n, (px, pz) in POINTS.items():
            for a in range(0, 360, 2):
                x, z = round(px + 17 * math.cos(math.radians(a))), round(pz + 17 * math.sin(math.radians(a)))
                if lo[0] <= x <= hi[0] and lo[2] <= z <= hi[2]: w.set((x, self.top[(x, z)], z), Cell(normalize('yellow_concrete')))
        for n, (px, pz) in CONVOY:
            for i in (-1, 0, 1):
                for j in (-1, 0, 1): w.set((px + i, self.top[(px + i, pz + j)], pz + j), Cell(normalize('red_concrete')))
        for n, (px, pz) in SPAWN.items(): w.set((px, self.top[(px, pz)] + 1, pz), Cell(normalize('lime_concrete' if n == 'dawn' else 'blue_concrete')))
        return w


def check(m):
    bad = 0
    for n, (px, pz) in POINTS.items():
        ring = [(x, z) for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17]
        hs = sorted({m.top[c] for c in ring}); ks = sorted({m.kind[c] for c in ring})
        print('point', n, 'ground heights', hs, 'kinds', ks); bad += len(hs) != 1 or ks != ['ground']
    sym = sum(1 for (x, z) in m.top if (-x, -z) in m.top and (m.top[(x, z)], m.kind[(x, z)]) != (m.top[(-x, -z)], m.kind[(-x, -z)]))
    print('cells that differ from their half-turn twin:', sym); bad += sym
    d = m.walk(SPAWN['dawn']); e = m.walk(SPAWN['dusk'])
    for n, p in list(POINTS.items()) + [('the other spawn', None)]:
        print('walk from dawn to', n, d.get(p or SPAWN['dusk']), '| from dusk', e.get(p or SPAWN['dawn'])); bad += (p or SPAWN['dusk']) not in d
    for a, b in zip(CONVOY, CONVOY[1:]):
        dd = m.walk(a[1]); print('convoy %s to %s: %s' % (a[0], b[0], dd.get(b[1])))
    esc = [c for c in d if m.kind[c] != 'ground']; print('reachable cells off the ground:', len(esc)); bad += len(esc)
    ice = [c for c in d if any(m.kind.get((c[0] + i, c[1] + j)) == 'ice' for i in (-1, 0, 1) for j in (-1, 0, 1))]
    print('walkable cells beside ice:', len(ice)); bad += len(ice)
    for name in CROSSINGS:
        dd = Map(closed=[o for o in CROSSINGS if o != name]).walk(SPAWN['dawn'])
        print('only %s open: dawn reaches the dusk spawn in' % name, dd.get(SPAWN['dusk'])); bad += SPAWN['dusk'] not in dd
    none = Map(closed=CROSSINGS).walk(SPAWN['dawn'])
    print('every crossing shut: dawn reaches the dusk spawn?', SPAWN['dusk'] in none); bad += SPAWN['dusk'] in none
    print('FAIL' if bad else 'OK')
    return bad


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'render':
        from mapforge import render
        out, view = sys.argv[2], sys.argv[3]
        box = tuple(int(v) for v in sys.argv[4:8]) if len(sys.argv) >= 8 else (X0, Z0, X1, Z1)
        m = Map(); lo, hi = (box[0], 56, box[1]), (box[2], 140, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Frostmarch blockout %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'check':
        sys.exit(1 if check(Map()) else 0)
