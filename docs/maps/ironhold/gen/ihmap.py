"""Ironhold "The Iron Pass": blockout of the whole map as a function of (x, z). See ../plan.md.

  python ihmap.py render OUT.png top|iso [x0 z0 x1 z1]
  python ihmap.py check

x east, z south. Valley y 64, outer ward 70, inner and upper ward 76. Attackers (Dawn) west, defenders (Dusk) east."""
import sys, os, math
from collections import deque
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
from mapforge.world import World, Cell
from mapforge.structure import normalize
from mapforge.terrain import fbm, seed_of, hash01

X0, X1, Z0, Z1 = -288, 287, -192, 191
SEED = seed_of(2311)
POINTS = {'A': (-112, 0), 'B': (0, 0), 'C': (112, 0)}
SPAWN = {'dawn': (-256, 0), 'dusk': (256, 0)}
NORTH = [(-288, -150), (-230, -158), (-180, -140), (-150, -150), (-110, -128), (-80, -138), (-40, -112), (0, -122), (30, -96), (70, -104), (100, -80), (140, -88), (170, -62), (210, -72), (250, -56), (288, -60)]
SOUTH = [(-288, 140), (-240, 152), (-190, 132), (-150, 146), (-120, 120), (-84, 132), (-50, 104), (-10, 116), (26, 90), (66, 100), (104, 74), (136, 84), (168, 58), (206, 66), (252, 50), (288, 56)]
# the three walls as (z, x) bends, north cliff to south cliff
OUTER = [(-150, -86), (-96, -78), (-60, -66), (-14, -72), (14, -72), (52, -64), (92, -76), (150, -86)]
INNER = [(-120, 28), (-66, 38), (-34, 46), (-12, 40), (12, 40), (36, 48), (64, 36), (120, 24)]
UPPER = [(-90, 168), (-30, 176), (-6, 172), (6, 172), (30, 178), (90, 166)]
GATES = {'outer': [(0, 4, 'main gate'), (-95, 2, 'mine gate'), (73, 3, 'breach')], 'inner': [(0, 4, 'inner gate'), (-67, 2, 'north postern'), (49, 2, 'south postern')], 'upper': [(0, 4, 'upper gate')]}
ROCKS = [(-158, -42, 16, 20), (-150, 46, 16, 20), (-8, -62, 18, 15), (-10, 62, 18, 15)]           # the crag and the two knuckles: cx, cz, rx, rz
ROADS = [(4, [(-256, 0), (-206, 0), (-176, 8), (-150, -6), (-112, 0), (-72, 0), (-30, -8), (0, 0), (40, 0), (76, 8), (112, 0), (172, 0), (256, 0)]),
         (2, [(-206, 0), (-186, -52), (-146, -84), (-112, -96), (-77, -95), (-36, -92), (4, -86), (39, -67), (76, -40), (112, 0)]),
         (2, [(-206, 0), (-190, 70), (-150, 96), (-110, 86), (-68, 73), (-34, 92), (8, 86), (45, 49), (80, 36), (112, 0)])]
# blockout buildings: x0, z0, x1, z1, height above ground, block, name
BUILDINGS = [(-60, -86, -46, -72, 8, 'deepslate_bricks', 'forge'), (-40, -110, -24, -98, 8, 'deepslate_bricks', 'smelter'), (-22, -104, -10, -95, 7, 'deepslate_bricks', 'smithy'),
             (-62, 88, -48, 98, 7, 'dark_oak_planks', 'stables'), (-8, 95, 4, 103, 6, 'dark_oak_planks', 'stores'), (-52, 26, -40, 38, 6, 'dark_oak_planks', 'guardhouse'),
             (52, -90, 64, -78, 8, 'dark_oak_planks', 'ward house n'), (60, 52, 72, 64, 8, 'dark_oak_planks', 'ward house'), (88, 44, 100, 56, 8, 'dark_oak_planks', 'ward house'),
             (136, 30, 150, 44, 8, 'dark_oak_planks', 'ward house'), (140, -30, 154, -22, 7, 'dark_oak_planks', 'ward house'),
             (204, -36, 264, -12, 9, 'deepslate_bricks', 'barracks north'), (204, 12, 264, 36, 9, 'deepslate_bricks', 'barracks south'),
             (-164, -110, -150, -98, 10, 'spruce_planks', 'mine head'), (-138, -30, -131, -23, 5, 'cobblestone', 'outwork ruin'), (-96, 20, -90, 27, 4, 'cobblestone', 'outwork wall')]
KEEP = (98, -84, 146, -42)                                  # the spur: rock to y 84, the keep on it to y 110


def lerp(pts, t):
    if t <= pts[0][0]: return pts[0][1]
    for (a, va), (b, vb) in zip(pts, pts[1:]):
        if t <= b: return va + (vb - va) * (t - a) / (b - a)
    return pts[-1][1]


def edge(x):
    n = fbm(SEED, x / 23.0, 0.5, 3) * 10 - 5
    return lerp(NORTH, x) + n, lerp(SOUTH, x) - fbm(SEED, x / 23.0, 9.5, 3) * 10 + 5


def wallx(name, z): return lerp({'outer': OUTER, 'inner': INNER, 'upper': UPPER}[name], z)


def seg_d(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az; L = dx * dx + dz * dz
    t = max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / L)) if L else 0.0
    return math.hypot(px - ax - t * dx, pz - az - t * dz)


class Map:
    def __init__(self, closed=()):
        self.top = {}; self.surf = {}; self.kind = {}
        road = {}
        for w, pts in ROADS:
            for (ax, az), (bx, bz) in zip(pts, pts[1:]):
                for x in range(int(min(ax, bx)) - w - 1, int(max(ax, bx)) + w + 2):
                    for z in range(int(min(az, bz)) - w - 1, int(max(az, bz)) + w + 2):
                        if seg_d(x, z, ax, az, bx, bz) <= w + 0.5: road[(x, z)] = True
        for x in range(X0, X1 + 1):
            n, s = edge(x)
            for z in range(Z0, Z1 + 1):
                t1, t2, t3 = x - wallx('outer', z), x - wallx('inner', z), x - wallx('upper', z)
                g = 64 + max(0, min(6, (t1 + 16) // 2)) + max(0, min(6, (t2 + 16) // 2))      # glacis ramps: one block up every two, ending before each wall
                g = int(g)
                r = hash01(SEED, x, z, 3); nz = fbm(SEED, x / 31.0, z / 31.0, 4)
                out = max(n - z, z - s)                                                  # > 0: in the mountain
                rock = max((1 - math.hypot((x - cx) / rx, (z - cz) / rz)) for cx, cz, rx, rz in ROCKS)
                kx0, kz0, kx1, kz1 = KEEP
                if kx0 - 3 <= x <= kx1 + 3 and kz0 - 3 <= z <= kz1 + 3:
                    h, b, k = 84, 'deepslate', 'rock'
                    if kx0 <= x <= kx1 and kz0 <= z <= kz1: h, b, k = (110 if (x - kx0 < 16 and z - kz0 < 18) else 96), 'deepslate_tiles', 'keep'
                elif out > 0:
                    h = int(g + 3 + min(out, 60) * 0.9 + nz * 14); k = 'rock'
                    b = 'magma_block' if (out > 26 and fbm(SEED, x / 9.0, z / 9.0, 2) > 0.72) else ('blackstone' if nz > 0.62 else 'basalt' if nz > 0.5 else 'deepslate' if h > 92 else 'tuff' if r < 0.3 else 'stone')
                elif rock > 0 and (x, z) not in road:
                    h, b, k = int(g + 3 + rock * 14), ('tuff' if r < 0.4 else 'stone'), 'rock'
                else:
                    h, k = g, 'ground'
                    b = ('gravel' if r < 0.5 else 'coarse_dirt') if (x, z) in road else (('grass_block' if r < 0.8 else 'coarse_dirt') if t1 < -18 else ('gravel' if r < 0.25 else 'cobblestone' if r < 0.6 else 'stone') if t1 < 4 or t2 >= -18 else ('cobblestone' if r < 0.5 else 'gravel' if r < 0.8 else 'coarse_dirt'))
                    for name, t in (('outer', t1), ('inner', t2), ('upper', t3)):
                        if abs(t) > 9: continue
                        gate = next((gz for gz, gw, gn in GATES[name] if abs(z - gz) <= gw and gn not in closed), None)
                        tower = any(abs(z - gz) in range(gw + 1, gw + 8) for gz, gw, gn in GATES[name] if gw == 4) or any(abs(z - bz) <= 5 for bz, _ in {'outer': OUTER, 'inner': INNER, 'upper': UPPER}[name][1:-1] if abs(bz) > 20)
                        if gate is not None: b = 'polished_deepslate' if abs(t) <= 3 else b; continue
                        if abs(t) <= 3 or (tower and abs(t) <= 6): h, b, k = g + (15 if tower else 10), ('polished_deepslate' if tower else 'deepslate_bricks'), 'wall'
                self.top[(x, z)] = h; self.surf[(x, z)] = b; self.kind[(x, z)] = k
        for x0, z0, x1, z1, hh, b, name in BUILDINGS:
            x0, x1, z0, z1 = min(x0, x1), max(x0, x1), min(z0, z1), max(z0, z1)
            cells = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
            if any(c in road or self.kind[c] != 'ground' for c in cells) and not closed: print('building overlaps a road, rock or wall:', name, x0, z0)
            base = max(self.top[c] for c in cells)
            for c in cells: self.top[c] = base + hh; self.surf[c] = b; self.kind[c] = 'building'
        for x in range(-282, -233):                               # the siege camp: palisade with an east opening, tents
            for z in range(-38, 39):
                if (x in (-282, -234) or z in (-38, 38)) and not (x == -234 and abs(z) <= 5): self.top[(x, z)] = 68; self.surf[(x, z)] = 'spruce_log'; self.kind[(x, z)] = 'wall'
        for tx in (-276, -266):
            for tz in (-30, -18, 14, 26):
                for x in range(tx, tx + 6):
                    for z in range(tz, tz + 6): self.top[(x, z)] = 67; self.surf[(x, z)] = 'white_wool'; self.kind[(x, z)] = 'building'
        self.road = road

    def walk(self, start):
        """breadth-first walk on 'ground' cells, one block up or down a step; returns distances"""
        import heapq
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
        w = World('ironhold')
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h = self.top[(x, z)]
                low = min([self.top.get((x + i, z + j), h) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h])
                if x in (lo[0], hi[0]) or z in (lo[2], hi[2]): low = lo[1]
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    w.set((x, y, z), Cell(normalize(self.surf[(x, z)] if y == h or self.kind[(x, z)] != 'ground' else 'stone')))
        for n, (px, pz) in POINTS.items():                         # ring markers, picture only
            for a in range(0, 360, 2):
                x, z = round(px + 17 * math.cos(math.radians(a))), round(pz + 17 * math.sin(math.radians(a)))
                if lo[0] <= x <= hi[0] and lo[2] <= z <= hi[2]: w.set((x, self.top[(x, z)], z), Cell(normalize('yellow_concrete')))
        for n, (px, pz) in SPAWN.items(): w.set((px, self.top[(px, pz)] + 1, pz), Cell(normalize('lime_concrete' if n == 'dawn' else 'blue_concrete')))
        return w


def check(m):
    bad = 0
    for n, (px, pz) in POINTS.items():
        ring = [(x, z) for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17]
        hs = sorted({m.top[c] for c in ring}); ks = sorted({m.kind[c] for c in ring})
        print('point', n, 'ground heights', hs, 'kinds', ks); bad += len(hs) != 1 or ks != ['ground']
    d = m.walk(SPAWN['dawn']); e = m.walk(SPAWN['dusk'])
    for n, p in list(POINTS.items()) + [('dusk spawn', SPAWN['dusk'])]:
        print('walk from dawn to', n, d.get(p), '| from dusk', e.get(p)); bad += p not in d
    esc = [c for c in d if m.kind[c] != 'ground']; print('reachable cells off the ground:', len(esc))
    lava = [c for c in d if any(m.surf.get((c[0] + i, c[1] + j)) == 'magma_block' for i in range(-3, 4) for j in range(-3, 4))]
    print('walkable cells within 3 of lava rock:', len(lava)); bad += len(lava)
    for wall in GATES:                                             # every opening must work alone
        for gz, gw, gn in GATES[wall]:
            others = [o[2] for o in GATES[wall] if o[2] != gn]
            dd = Map(closed=others).walk(SPAWN['dawn'])
            print('only the %s open: dawn reaches dusk spawn in' % gn, dd.get(SPAWN['dusk'])); bad += SPAWN['dusk'] not in dd
    none = Map(closed=[o[2] for o in GATES['outer']]).walk(SPAWN['dawn'])
    print('all outer openings shut: dawn reaches B?', POINTS['B'] in none); bad += POINTS['B'] in none
    print('FAIL' if bad else 'OK')
    return bad


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'render':
        from mapforge import render
        out, view = sys.argv[2], sys.argv[3]
        box = tuple(int(v) for v in sys.argv[4:8]) if len(sys.argv) >= 8 else (X0, Z0, X1, Z1)
        m = Map(); lo, hi = (box[0], 56, box[1]), (box[2], 134, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Ironhold blockout %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'check':
        sys.exit(1 if check(Map()) else 0)
