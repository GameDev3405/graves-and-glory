"""Hollowwood "The Hollow": the whole built map. The blockout (hwmap.py) is the spec for every floor a troop may stand on;
this dresses it: giant trunks and canopy, the two stumps, the Fallen Giants, the High Walk, the Root Runs, camps, the Yoke. See ../plan.md.

  python hwbuild.py build CACHE.pkl
  python hwbuild.py check CACHE.pkl
  python hwbuild.py render CACHE.pkl OUT.png top|iso [x0 z0 x1 z1]
  python hwbuild.py export CACHE.pkl OUTDIR [A1 ...]
  python hwbuild.py shots CACHE.pkl OUT.txt
  python hwbuild.py nonfull CACHE.pkl OUT.md

x east, z south. Wood floor y 72, the Hollow 62, the High Walk 78. Half-turn symmetric: Dawn's half is built and turned.
Only the dressing of the two stumps (A the loggers' stockade, C rotted in the bog), the bog pools and the Yoke differ."""
import sys, os, math, pickle
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, '..', '..', 'ironhold', 'gen')))
import ihbuild                                              # the shared machinery: props, anchors, the walk
from ihbuild import cls, mix
import xf, prop_anchor
sys.path.insert(0, HERE)
import hwmap, stump, yoke
from hwmap import POINTS, SPAWN, ESCORT, X0, X1, Z0, Z1, G, PIT, HI, CAMP, STAIR, DECKTREES, STAIRTREE, R_LEDGE
from mapforge.world import World, Cell
from mapforge.structure import normalize
from mapforge.terrain import fbm, hash01, seed_of

YLO, YHI = 60, 140
SEED = seed_of(2412)
A = (hwmap.STUMP[0], hwmap.STUMP[1]); C = (-A[0], -A[1])
WOOD_M = [('podzol', 46), ('coarse_dirt', 16), ('rooted_dirt', 16), ('moss_block', 22)]
MOSS_M = [('moss_block', 60), ('podzol', 22), ('rooted_dirt', 18)]
ROAD_M = [('coarse_dirt', 46), ('packed_mud', 24), ('rooted_dirt', 16), ('podzol', 14)]
PIT_M = [('grass_block', 48), ('moss_block', 34), ('podzol', 18)]
RAMP_M = [('rooted_dirt', 40), ('coarse_dirt', 34), ('packed_mud', 26)]
BOG_M = [('packed_mud', 34), ('moss_block', 38), ('rooted_dirt', 16), ('podzol', 12)]
BOULDER = [('mossy_cobblestone', 40), ('cobblestone', 20), ('moss_block', 24), ('andesite', 16)]
MOUNDS = [(-8, -54, 6), (34, -50, 5), (-62, -18, 6)]                              # round A: brush mounds; their twins round C are bog pools behind hummocks


def patch(x, z, m, k=0):
    """a ground mix in soft patches, not salt and pepper"""
    return mix(min(0.999, max(0.0, (fbm(SEED + 11 + k, x, z, 6, 2) + 1) / 2 * 0.72 + hash01(SEED, x, z, 12 + k) * 0.28)), m)


def dawn(x, z): return x < 0 or (x == 0 and z < 0)
def sf(x, z, c): return math.hypot((x - c[0]) / 40.0, (z - c[1]) / 34.0)


class Map(ihbuild.Map):
    def __init__(self):
        self.h, self.g, self.surf, self.kind = {}, {}, {}, {}
        self.extra, self.used, self.log, self.props = {}, set(), [], []
        self.bm = hwmap.Map(); self.road = set(self.bm.road) | set(hwmap.both(hwmap.band, hwmap.GULLY, 4)) | set(hwmap.both(hwmap.band, hwmap.TUN, 4))
        self.terrain(); before = dict(self.extra)
        self.trunks(); self.giant(); self.stumps(); self.decks(); self.camp(); self.mounds(); self.scatter(); self.twin(before)
        self.drowned(); self.shrine(); self.index()

    # ---------------------------------------------------------------- terrain
    def terrain(self):
        bm = self.bm
        for x in range(X0, X1 + 1):
            for z in range(Z0, Z1 + 1):
                sx, sz = (x, z) if dawn(x, z) else (-x, -z); r = hash01(SEED, sx, sz, 3); k = bm.kind[(x, z)]; gt = next(s[1] for s in bm.col[(x, z)] if s[0] is None)
                fa = min(sf(x, z, A), sf(x, z, C))
                if fa <= 1.25: h, kind, b = G, 'stump', mix(r, WOOD_M)
                elif k == 'deep':
                    out = abs(x / 282.0) ** 6 + abs(z / 186.0) ** 6 - 1
                    h, kind, b = G + 4 + min(12, int(out * 30 + (fbm(SEED + 4, sx, sz, 9, 2) + 1) * 2)), 'deep', mix(r, MOSS_M)
                elif k in ('trunk', 'bark', 'wall'): h, kind, b = G, k, mix(r, WOOD_M)
                elif k == 'tunnel': h, kind, b = G, 'ground', mix(r, ROAD_M if len(bm.col[(x, z)]) > 1 and bm.col[(x, z)][1][0] != PIT + 5 else WOOD_M)
                elif k == 'pit': h, kind, b = gt, ('ring' if math.hypot(x, z) < 17 else 'ground'), patch(sx, sz, PIT_M, 3)
                elif k in ('ledge', 'gully'): h, kind, b = gt, 'ground', mix(r, RAMP_M)
                elif k == 'ramp': h, kind, b = gt, 'ground', 'spruce_planks'
                elif k == 'road': h, kind, b = gt, 'ground', patch(sx, sz, ROAD_M, 5)
                else: h, kind, b = gt, 'ground', patch(sx, sz, MOSS_M if fbm(SEED + 1, sx, sz, 15, 3) > 0.25 else WOOD_M)
                self.h[(x, z)], self.g[(x, z)], self.surf[(x, z)], self.kind[(x, z)] = h, h, b, kind
                if k == 'tunnel' and bm.col[(x, z)][1][0] == PIT + 5 and dawn(x, z):          # a Root Run tunnel: 4 clear under the lip
                    for y in range(PIT + 1, PIT + 5): self.extra[(x, y, z)] = 'air'
                    if bm.tun.get((x, z), (9,))[0] < 0.6 and int(bm.tun[(x, z)][1]) % 9 == 4: self.extra[(x, PIT + 4, z)] = 'lantern[hanging=true]'

    def use(self, x, z, r=1):
        for i in range(-r, r + 1):
            for j in range(-r, r + 1): self.used.add((x + i, z + j))

    # ---------------------------------------------------------------- the wood
    def trunks(self):
        bm = self.bm; E = self.extra; plain = {(t[0], t[1]) for t in DECKTREES} | {(STAIRTREE[0], STAIRTREE[1])}
        noleaf = set(bm.road) | set(hwmap.both(hwmap.band, hwmap.GULLY, 5))
        for tx, tz, r in bm.trunks:
            if not dawn(tx, tz): continue
            hh = hash01(SEED, tx, tz, 5); oak = hh < 0.6; T = 116 + int(hash01(SEED, tx, tz, 6) * 12)
            log, wood, leaf = ('dark_oak_log[axis=y]', 'dark_oak_wood[axis=y]', 'dark_oak_leaves[persistent=true]') if oak else ('spruce_log[axis=y]', 'spruce_wood[axis=y]', 'spruce_leaves[persistent=true]')
            for x in range(tx - r - 1, tx + r + 2):
                for z in range(tz - r - 1, tz + r + 2):
                    d = math.hypot(x - tx, z - tz)
                    if d > r + 0.3: continue
                    self.used.add((x, z))
                    for y in range(G + 1, T + 1):
                        if d > r - 1.6 or y >= T - 1 or y == G + 1: E[(x, y, z)] = wood if hash01(SEED, x * 3 + y // 5, z, 7) < 0.2 else 'moss_block' if y < G + 8 and hash01(SEED, x, z + y // 2, 8) < 0.22 else log
            if (tx, tz) not in plain:                                                  # buttress roots: fins 9, 6 and 3 high, never a step
                n = 5 + int(hash01(SEED, tx, tz, 9) * 3)
                for i in range(n):
                    a = (i + hash01(SEED, tx, tz, 10 + i) * 0.6) * 2 * math.pi / n
                    for out in (1, 2, 3):
                        x, z = round(tx + (r + out) * math.cos(a)), round(tz + (r + out) * math.sin(a))
                        if self.kind.get((x, z)) != 'ground' or self.h[(x, z)] != G or (x, z) in self.road or (x, z) in bm.deck: continue
                        for y in range(G + 1, G + 13 - 3 * out): E[(x, y, z)] = wood
                        self.used.add((x, z))
            def leafok(x, z):
                return (x, z) in self.h and (x, z) not in noleaf and math.hypot(x, z) >= 58 and min(sf(x, z, A), sf(x, z, C)) >= 1.05 and not (CAMP[0] - 2 <= x <= CAMP[1] + 2 and CAMP[2] - 2 <= z <= CAMP[3] + 2)
            R = r + 9 + int(hash01(SEED, tx, tz, 20) * 5); ph = hash01(SEED, tx, tz, 19) * 6.283
            for x in range(tx - R, tx + R + 1):
                for z in range(tz - R, tz + R + 1):
                    d = math.hypot(x - tx, z - tz); core = d <= r + 0.3; an = math.atan2(z - tz, x - tx)
                    if d > R * (0.82 + 0.18 * math.sin(an * 3 + ph)) + fbm(SEED + 8, x, z, 5, 2) * 2 or (x, z) not in self.h or (not core and not leafok(x, z)): continue
                    if d > R - 2 and hash01(SEED, x, z, 21) < 0.4: continue
                    top = T + 2 + int(5 * (1 - (d / R) ** 2) + fbm(SEED + 9, x, z, 7, 2) * 2.2 + 1.5 * math.sin(an * 2 + ph)); bot = T + 1 if core else T - 1 - (2 if d > R * 0.6 and hash01(SEED, x // 2, z // 2, 23) < 0.5 else 0)
                    for y in range(bot, top + 1):
                        if y >= top - 1 or y <= bot + 1 or d > R - 3: E.setdefault((x, y, z), 'flowering_azalea_leaves[persistent=true]' if hash01(SEED, x, y, z * 3 + 22) < 0.03 else leaf)
            for i in range(4):                                                          # limbs, far above any deck, each with a clump of leaf
                a = hash01(SEED, tx, tz, 24 + i) * 6.283; by = T - 7 - i * 6; L = 5 + int(hash01(SEED, tx, tz, 28 + i) * 6)
                ex, ez = round(tx + (r + L) * math.cos(a)), round(tz + (r + L) * math.sin(a))
                if not all(leafok(ex + p, ez + q) for p in (-4, 0, 4) for q in (-4, 0, 4)): continue
                for k in range(L * 2 + 1):
                    x, z = round(tx + (r + k / 2) * math.cos(a)), round(tz + (r + k / 2) * math.sin(a)); E[(x, by + k // 4, z)] = wood; E[(x, by + k // 4 - 1, z)] = wood
                ey = by + L // 2
                for x in range(ex - 4, ex + 5):
                    for z in range(ez - 4, ez + 5):
                        for y in range(ey - 1, ey + 4):
                            if math.hypot(x - ex, z - ez, (y - ey - 1) * 1.6) <= 4.2 - hash01(SEED, x, y, z + 29) * 0.8: E.setdefault((x, y, z), leaf)
            for i in range(3):
                a = hash01(SEED, tx, tz, 30 + i) * 6.283; x, z = round(tx + (r + 1.6) * math.cos(a)), round(tz + (r + 1.6) * math.sin(a))
                if (x, z) in self.h: E[(x, T - 2, z)] = 'shroomlight'
        for x in range(X0, X1 + 1):                                                     # the deep wood: a wall of smaller trees on the bank
            for z in range(Z0, Z1 + 1):
                if not dawn(x, z) or self.kind[(x, z)] != 'deep': continue
                h = self.h[(x, z)]
                if x % 5 == 2 and z % 5 == 2 and hash01(SEED, x, z, 40) < 0.75:
                    T = min(YHI - 6, h + 22 + int(hash01(SEED, x, z, 41) * 14)); lg = 'dark_oak_log[axis=y]' if hash01(SEED, x, z, 42) < 0.6 else 'spruce_log[axis=y]'
                    for i in (0, 1):
                        for j in (0, 1):
                            for y in range(h + 1, T + 1): E[(x + i, y, z + j)] = lg
                    for i in range(-4, 6):
                        for j in range(-4, 6):
                            for y in range(T - 2, T + 3):
                                if math.hypot(i - 0.5, j - 0.5, (y - T) * 1.4) <= 4.6 and self.kind.get((x + i, z + j)) == 'deep': E.setdefault((x + i, y, z + j), 'dark_oak_leaves[persistent=true]' if lg.startswith('dark') else 'spruce_leaves[persistent=true]')
                elif hash01(SEED, x, z, 43) < 0.5:
                    for y in range(h + 1, h + 2 + int(hash01(SEED, x, z, 44) * 3)): E.setdefault((x, y, z), 'dark_oak_leaves[persistent=true]')

    def giant(self):
        bm = self.bm; E = self.extra; L = sum(math.dist(a, b) for a, b in zip(hwmap.GIANT, hwmap.GIANT[1:]))
        for (x, z), (d, s) in bm.giant.items():
            if bm.kind[(x, z)] not in ('trunk', 'tunnel'): continue
            top = G + 6 + int(round(math.sqrt(max(0.0, 30.25 - d * d)))); end = s < 1.2 or s > L - 1.2
            for y in range(G + 5 if bm.kind[(x, z)] == 'tunnel' else G + 1, top + 1):
                E[(x, y, z)] = 'stripped_dark_oak_wood[axis=x]' if end else 'moss_block' if y == top and fbm(SEED + 5, x, z, 6, 2) > 0.1 else 'dark_oak_wood[axis=x]' if hash01(SEED, x + y, z, 50) < 0.25 else 'dark_oak_log[axis=x]'
            self.used.add((x, z))
        (ax, az), (bx, bz) = hwmap.GIANT[0], hwmap.GIANT[1]; n = math.dist((ax, az), (bx, bz)); ux, uz = (ax - bx) / n, (az - bz) / n       # the root plate at the west end
        for i in range(-9, 10):
            for y in range(G + 1, G + 17):
                if math.hypot(i, y - G - 7) > 9.4 - hash01(SEED, i, y, 51) * 2: continue
                for t in (1, 2):
                    x, z = round(ax + ux * t - uz * i), round(az + uz * t + ux * i)
                    if self.kind.get((x, z)) in ('ground', 'trunk') and (x, z) not in self.road: E[(x, y, z)] = 'rooted_dirt' if hash01(SEED, i, y, 52 + t) < 0.5 else 'dark_oak_wood[axis=y]'; self.used.add((x, z))

    def stamp_stump(self, c, rotted, q):
        bm = self.bm; keys = []
        for (x, y, z), v in (xf.turn(stump.stump(G, rotted), q) if q else stump.stump(G, rotted)).items():
            X, Z = x + c[0], z + c[1]; lx, lz = (x, z) if not q else (-x, -z); kind = stump.shape(lx, lz)[0]
            if (X, Z) not in self.h: continue
            if kind == 'bark' and bm.kind[(X, Z)] == 'stump':                           # the High Walk's door through the rim
                if y > G + 6: continue
                if y == G + 6: v = 'spruce_planks'
            if kind in ('root', None) and (X, Z) in self.neardeck: continue             # no root beside a deck: it would be a way off it
            self.extra[(X, y, Z)] = v; keys.append((X, y, Z)); self.used.add((X, Z))
        return keys

    def stumps(self):
        self.neardeck = {(x + i, z + j) for (x, z) in self.bm.deck for i in range(-3, 4) for j in range(-3, 4)}
        self.akeys = self.stamp_stump(A, False, 0)

    def drowned(self):
        """C: take off the turned stockade and put the rotted stump on the same shape; the ground round it turns to bog"""
        for (x, y, z) in self.akeys: self.extra.pop((-x, y, -z), None)
        self.stamp_stump(C, True, 2)
        for x in range(C[0] - 90, C[0] + 91):
            for z in range(C[1] - 80, C[1] + 81):
                if (x, z) in self.h and self.kind[(x, z)] in ('ground', 'stump') and self.h[(x, z)] == G and (x, z) not in self.bm.road and sf(x, z, C) < 2.1 + fbm(SEED + 6, x, z, 11, 2) * 0.3:
                    self.surf[(x, z)] = patch(x, z, BOG_M, 7)
        for mx, mz, r in MOUNDS:                                                        # the pools: water inside the hummock ring, out of reach
            cx, cz = -(A[0] + mx), -(A[1] + mz)
            for x in range(cx - r, cx + r + 1):
                for z in range(cz - r, cz + r + 1):
                    if math.hypot(x - cx, z - cz) <= r - 2.2 and (x, z) in self.pool:
                        for y in range(G + 1, G + 6): self.extra.pop((x, y, z), None)
                        self.surf[(x, z)] = 'water'

    def mounds(self):
        self.pool = set()
        for mx, mz, r in MOUNDS:
            cx, cz = A[0] + mx, A[1] + mz
            for x in range(cx - r, cx + r + 1):
                for z in range(cz - r, cz + r + 1):
                    d = math.hypot(x - cx, z - cz) + (hash01(SEED, x, z, 61) - 0.5)
                    if d > r or self.kind.get((x, z)) != 'ground' or (x, z) in self.used or (x, z) in self.road or (x, z) in self.bm.deck: continue
                    rim = math.hypot(x - cx, z - cz) > r - 2.2
                    for y in range(G + 1, G + 4 + (1 if hash01(SEED, x, z, 62) < 0.3 else 0)):
                        self.extra[(x, y, z)] = mix(hash01(SEED, x, y, z + 63), [('mossy_cobblestone', 20), ('moss_block', 30), ('rooted_dirt', 25), ('dark_oak_wood[axis=y]', 25)]) if rim else ('dark_oak_log[axis=x]' if (y + z) % 2 else 'spruce_log[axis=z]')
                    self.used.add((x, z)); self.pool.add((-x, -z))

    # ---------------------------------------------------------------- the High Walk
    def decks(self):
        bm = self.bm; E = self.extra
        for (x, z), slabs in bm.col.items():
            if not dawn(x, z): continue
            r = math.hypot(x, z); over = r <= 58
            for y0, y1, b, wk in slabs:
                if y0 != HI: continue
                if y1 == HI: E[(x, HI, z)] = 'dark_oak_planks' if over or hash01(SEED, x // 2, z // 2, 55) < 0.14 else 'spruce_planks'
                else:
                    E[(x, HI, z)] = E[(x, HI + 1, z)] = 'dark_oak_planks' if over else 'spruce_planks'; E[(x, HI + 2, z)] = 'stripped_dark_oak_log[axis=x]' if over else 'stripped_spruce_log[axis=x]'      # rails two above the deck
                if over:
                    for y in range(HI - int(4 * (abs(x) / 52.0) ** 2), HI): E[(x, y, z)] = 'dark_oak_wood[axis=x]'
                elif y1 != HI and bm.kind[(x, z)] == 'wood' and r > 64 and (x * 3 + z * 5) % 9 == 0 and (x, z) not in self.road:
                    for y in range(G + 1, HI): E[(x, y, z)] = 'spruce_log[axis=y]'
                    self.used.add((x, z))
                elif y1 == HI and bm.deck[(x, z)] < 0.6 and (x + 2 * z) % 13 == 0 and self.h[(x, z)] <= G: E[(x, HI - 1, z)] = 'lantern[hanging=true]'

    # ---------------------------------------------------------------- the camp and the small things
    def camp(self):
        E = self.extra; x0, x1, z0, z1 = CAMP
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if self.bm.kind[(x, z)] != 'wall': continue
                hh = 4 + ((x + z) % 3 == 0)
                for y in range(1, hh + 1): E[(x, G + y, z)] = 'spruce_log[axis=y]' if y < hh else 'stripped_spruce_log[axis=y]'
                self.used.add((x, z))
        for (x, z) in ((-235, z0), (-223, z0), (x1, 124), (x1, 136)):                    # gate posts
            for y in range(1, 8): E[(x, G + y, z)] = 'dark_oak_log[axis=y]'
        for i, x in enumerate((-256, -244, -232, -220)): self.prop('tent_soldier_large_dawn', x, z1 - 7, 0, 'tent s%d' % i)
        self.prop('tent_pavilion_large_dawn', x0 + 9, 112, 90, 'pavilion')
        self.prop('supply_wagon_covered', -244, 104, 90, 'wagon camp n'); self.prop('supply_wagon_hay', -216, 112, 0, 'wagon camp e'); self.prop('supply_wagon_barrels', -252, 128, 0, 'wagon camp w')
        self.prop('brazier_tall', -238, z0 + 2, 0, 'brazier gate n'); self.prop('brazier_tall', x1 - 2, 121, 0, 'brazier gate e')
        for (x, z) in ((-216, 102), (-258, 104), (-224, 132)):
            for n, (i, j) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
                for y in (1, 2, 3): E[(x + i, G + y, z + j)] = ('barrel[facing=up]', 'hay_block[axis=y]', 'spruce_planks')[(n + y) % 3]
            self.use(x, z, 2)

    def scatter(self):
        E = self.extra; bm = self.bm
        def ok(x, z, r):
            return self.free(x, z, r) and self.clear_of_road(x, z, r + 1) and all((x + i, z + j) not in bm.deck and self.h.get((x + i, z + j)) == self.h[(x, z)] for i in range(-r - 1, r + 2) for j in range(-r - 1, r + 2))
        def boulder(x, z):
            for i in range(-1, 2):
                for j in range(-1, 2):
                    if abs(i) + abs(j) == 2 and hash01(SEED, x + i, z + j, 70) < 0.5: continue
                    for y in range(1, 4 + (1 if i == 0 and j == 0 else 0)): E[(x + i, self.h[(x, z)] + y, z + j)] = mix(hash01(SEED, x + i, y, z + j + 71), BOULDER)
            self.use(x, z, 2)
        def shroom(x, z):
            red = hash01(SEED, x, z, 72) < 0.4; hh = 5 + int(hash01(SEED, x, z, 73) * 3); y0 = self.h[(x, z)]
            for y in range(1, hh): E[(x, y0 + y, z)] = 'mushroom_stem'
            for i in range(-2, 3):
                for j in range(-2, 3):
                    if abs(i) == 2 and abs(j) == 2: continue
                    E[(x + i, y0 + hh, z + j)] = 'red_mushroom_block' if red else 'brown_mushroom_block'
                    if red and max(abs(i), abs(j)) == 2: E[(x + i, y0 + hh - 1, z + j)] = 'red_mushroom_block'
            self.use(x, z, 1)
        def bush(x, z):
            for i in (0, 1):
                for j in (0, 1):
                    for y in range(1, 4): E[(x + i, self.h[(x, z)] + y, z + j)] = 'azalea_leaves[persistent=true]' if hash01(SEED, x + i, y, z + j + 74) < 0.7 else 'flowering_azalea_leaves[persistent=true]'
            self.use(x, z, 2)
        def logs(x, z):
            ax = 'x' if hash01(SEED, x, z, 75) < 0.5 else 'z'
            for i in range(-2, 3):
                for w in (0, 1):
                    for y in (1, 2, 3):
                        cx, cz = (x + i, z + w) if ax == 'x' else (x + w, z + i); E[(cx, self.h[(x, z)] + y, cz)] = 'spruce_log[axis=%s]' % ax if (y + w) % 2 else 'dark_oak_log[axis=%s]' % ax
            self.use(x, z, 3)
        def young(x, z):
            hh = 8 + int(hash01(SEED, x, z, 76) * 7); y0 = self.h[(x, z)]; sp = hash01(SEED, x, z, 77) < 0.45
            for y in range(1, hh + 1): E[(x, y0 + y, z)] = 'spruce_log[axis=y]' if sp else 'dark_oak_log[axis=y]'
            for i in range(-3, 4):
                for j in range(-3, 4):
                    for y in range(hh - 3, hh + 3):
                        if math.hypot(i, j, (y - hh + 1) * 1.3) <= 3.4 - hash01(SEED, x + i, y, z + j + 78) and (x + i, z + j) in self.h: E.setdefault((x + i, y0 + y, z + j), 'spruce_leaves[persistent=true]' if sp else 'dark_oak_leaves[persistent=true]')
            self.use(x, z, 1)
        for gx in range(X0 + 5, 4, 9):
            for gz in range(Z0 + 5, Z1 - 4, 9):
                if hash01(SEED, gx, gz, 80) > 0.8: continue
                x, z = gx + int(hash01(SEED, gx, gz, 81) * 7), gz + int(hash01(SEED, gx, gz, 82) * 7); r = hash01(SEED, gx, gz, 83)
                if not dawn(x, z) or (x, z) not in self.h or math.hypot(x, z) < 68 or min(sf(x, z, A), sf(x, z, C)) < 1.5: continue
                if CAMP[0] - 6 <= x <= CAMP[1] + 6 and CAMP[2] - 6 <= z <= CAMP[3] + 6: continue
                if not ok(x, z, 3): continue
                (boulder if r < 0.18 else shroom if r < 0.33 else bush if r < 0.55 else logs if r < 0.65 else young)(x, z)
        for th, rr in ((150, 36), (205, 30), (255, 36), (118, 27), (172, 24), (232, 23), (285, 30), (300, 22)):                                # cover on the Hollow floor
            x, z = round(rr * math.cos(math.radians(th))), round(rr * math.sin(math.radians(th)))
            if dawn(x, z) and ok(x, z, 2): (boulder if th % 2 else logs if th % 3 else shroom)(x, z)

    def twin(self, before):
        new = {k: v for k, v in self.extra.items() if before.get(k) != v}
        for (x, y, z), v in xf.turn(new, 2).items():
            if (x, y, z) in self.extra and (x, y, z) not in new and self.extra[(x, y, z)] != v and 'leaves' not in v and 'leaves' not in self.extra[(x, y, z)]: self.log.append('twin cell on an old cell %s  CONFLICT' % ((x, y, z),))
            self.extra.setdefault((x, y, z), v) if 'leaves' in v else self.extra.__setitem__((x, y, z), v)
        for (x, y, z), v in xf.turn(before, 2).items(): self.extra[(x, y, z)] = v             # the tunnels' air
        for (x, z) in list(self.used): self.used.add((-x, -z))
        for pid, (x, ay, z), facing in list(self.props):
            pid2 = pid.replace('_dawn', '_dusk'); a = prop_anchor.anchor(pid2, (-x, ay, -z), (facing + 180) % 360, 'minecraft:stone', 1)
            for c in a.solid: self.extra[c] = 'barrier'
            self.props.append((pid2, (-x, ay, -z), (facing + 180) % 360))

    def shrine(self):
        for (x, y, z), v in yoke.yoke(PIT).items():
            if y == PIT: self.surf[(x, z)] = v
            else: self.extra[(x, y, z)] = v
            if y > PIT: self.used.add((x, z))

    # ---------------------------------------------------------------- cells
    def cells(self, lo, hi, solid=True):
        out = {}; st = STAIR
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h, s = self.h[(x, z)], self.surf[(x, z)]
                low = lo[1] if solid else min([self.h.get((x + i, z + j), h) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h, PIT if self.bm.kind[(x, z)] == 'tunnel' else h]) - 1
                plank = s == 'spruce_planks'
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    k = (x * 7 + y * 3 + z * 5) % 11
                    out[(x, y, z)] = s if y == h else 'spruce_planks' if plank else 'dirt' if s == 'water' else 'dark_oak_wood[axis=y]' if hash01(SEED, x, z, 90) < 0.22 and y > h - 3 - int(hash01(SEED, x, z, 91) * 9) else 'moss_block' if fbm(SEED + 13, x + z, y * 2, 5, 2) > 0.25 and y > h - 5 else 'mossy_cobblestone' if fbm(SEED + 14, x - z, y * 2, 6, 2) > 0.3 else 'rooted_dirt' if k < 5 else 'coarse_dirt' if k < 8 else 'dirt'
        for (x, y, z), b in self.extra.items():
            if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]:
                if b == 'air': out.pop((x, y, z), None)
                else: out[(x, y, z)] = b
        return out

    def world(self, lo, hi, solid=False):
        w = World('hollowwood')
        for pos, b in self.cells(lo, hi, solid).items(): w.set(pos, Cell(normalize(b)))
        return w


def plugs(m):
    bm = m.bm
    return {'ramps': {c for c, k in bm.kind.items() if k == 'ledge'}, 'runs': {c for c, k in bm.kind.items() if k == 'tunnel' and bm.col[c][1][0] == PIT + 5},
            'bridges': {c for c in bm.deck if math.hypot(*c) < R_LEDGE}}


def section_box(name):
    c, r = 'ABCDEFGHI'.index(name[0]), int(name[1]) - 1
    return (X0 + 64 * c, Z0 + 64 * r, X0 + 64 * c + 63, Z0 + 64 * r + 63)


def nonfull(v):
    n = v.split('[')[0]
    return cls(v) in (2, 3) or n in ('rail', 'ladder', 'dead_bush', 'candle', 'water') or n.endswith(('banner', '_pane', 'pressure_plate', 'trapdoor', 'button', 'carpet'))


def check(m):
    bad = 0; bm = m.bm
    for l in m.log: print(l); bad += 'CONFLICT' in l
    d = m.walk(SPAWN['dawn']); at = {}
    for (x, y, z), v in d.items(): at.setdefault((x, z), []).append((y, v))
    e = m.walk(SPAWN['dusk']); et = {}
    for (x, y, z), v in e.items(): et.setdefault((x, z), []).append((y, v))
    print('cells Dawn can reach:', len(d), '| Dusk:', len(e)); bad += abs(len(d) - len(e)) > 400
    for n, (px, pz) in POINTS.items():
        ring = [(x, z) for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17]
        ys = sorted({y for c in ring for y, _ in at.get(c, [(None, 0)])}, key=str)
        stuff = [c for c in ring if ys[0] is not None and any(ys[0] <= y <= ys[0] + 4 for y in m.cols.get(c, ()) if m.extra[(c[0], y, c[1])] != 'air')]
        print('point', n, 'standing heights', ys, 'cells with anything standing in the ring:', len(stuff)); bad += len(ys) != 1 or ys[0] is None or bool(stuff)
    def dist(t, p): return round(min(v for _, v in t[p])) if p in t else None
    W = {}
    for n, p in list(POINTS.items()) + [('Dawn spawn', SPAWN['dawn']), ('Dusk spawn', SPAWN['dusk']), ('north bridge', (0, -40)), ('south bridge', (0, 40))]:
        W[n] = (dist(at, p), dist(et, p)); print('walk to %-12s from Dawn %s | from Dusk %s' % (n, W[n][0], W[n][1])); bad += p not in at or p not in et
    if not bad:
        fair = abs(W['B'][0] - W['B'][1]) <= 3 and abs(W['A'][0] - W['C'][1]) <= 3 and abs(W['C'][0] - W['A'][1]) <= 3
        print('walks fair (B equal, Dawn to A = Dusk to C, Dawn to C = Dusk to A):', fair); bad += not fair
    for a, b in zip(ESCORT, ESCORT[1:]):
        dd = m.walk(a[1]); r = [v for (x, y, z), v in dd.items() if (x, z) == b[1]]; print('escort %s to %s: %s' % (a[0], b[0], round(min(r)) if r else None)); bad += not r
    spec = {c: {s[1] for s in sl if s[3]} for c, sl in bm.col.items()}
    for who, t in (('Dawn', d), ('Dusk', e)):
        off = [c for c in t if c[1] - 1 not in spec[(c[0], c[2])]]
        prop = [c for c in off if m.blk(c[0], c[1] - 1, c[2]) == 'barrier']; flat = [c for c in off if c[1] == G + 1 and m.blk(c[0], G, c[2]) == 'T']
        print(who, 'can stand on a prop (wagon beds, tent floors):', len(prop), '| on plain wood floor the blockout drew as bark:', len(flat)); off = [c for c in off if c not in set(prop) | set(flat)]
        wet = [c for c in t if any(m.surf.get((c[0] + i, c[2] + j)) == 'water' for i in (-1, 0, 1) for j in (-1, 0, 1))]
        fire = [c for c in t if any('lit=true' in (m.blk(c[0] + i, c[1] + k, c[2] + j) or '') for i in (-1, 0, 1) for j in (-1, 0, 1) for k in (-1, 0, 1))]
        part = [c for c in t if cls(m.blk(c[0], c[1] - 1, c[2])) == 2]
        print(who, 'stands somewhere the blockout has no floor (on a trunk, roof, rock, prop...):', len(off), sorted(off)[:30]); bad += bool(off)
        print(who, 'reachable cells beside water:', len(wet), wet[:5]); bad += bool(wet)
        print(who, 'reachable cells beside a lit fire:', len(fire), fire[:5]); bad += bool(fire)
        print(who, 'stands on stairs, slabs or beds:', len(part), sorted(part)[:8]); bad += bool(part)
    bd = bm.walk(hwmap.node(bm, SPAWN['dawn'])); miss = [k for k in bd if (k[0], k[2] + 1, k[1]) not in d]
    print('blockout floors Dawn can no longer reach (under trunks roots, boulders, sheds, the Yoke feet...):', len(miss), 'of', len(bd))
    sym = sum(1 for (x, z) in m.h if (-x, -z) in m.h and m.h[(x, z)] != m.h[(-x, -z)])
    fl = sum(1 for (x, y, z) in d if (-x, y, -z) not in e)
    print('columns whose height differs from the half-turn twin:', sym, '| Dawn cells whose twin Dusk cannot reach:', fl); bad += sym > 0 or fl > 60
    P = plugs(m)
    for name, shut in (('ramps only', ('runs',)), ('Root Runs only', ('ramps',)), ('neither', ('ramps', 'runs'))):
        dd = m.walk(SPAWN['dawn'], set().union(*[P[k] for k in shut])); r = [v for (x, y, z), v in dd.items() if (x, z) == POINTS['B']]
        print('Dawn to B by %s: %s' % (name, round(min(r)) if r else None)); bad += bool(r) == (name == 'neither')
    dd = m.walk(SPAWN['dawn'], P['bridges']); r = [v for (x, y, z), v in dd.items() if (x, z) == SPAWN['dusk']]; print('Dawn to the Dusk spawn with the bridges shut:', round(min(r)) if r else None); bad += not r
    inv = {}
    for (x, y, z), v in m.extra.items():
        if nonfull(v): inv[v.split('[')[0]] = inv.get(v.split('[')[0], 0) + 1
    print('non-full blocks:', sum(inv.values())); print('  ' + ', '.join('%s %d' % kv for kv in sorted(inv.items(), key=lambda kv: -kv[1])))
    print('props:', len(m.props), '| extra cells:', len(m.extra))
    print('FAIL' if bad else 'OK')
    return bad


VIEWS = [('camp-dawn', (-200, 222, 96, -236, 208, 124)), ('camp-inside', (-214, 210, 140, -250, 209, 118)), ('near-road', (-214, 211, 60, -190, 212, 0)), ('giant-west', (-204, 216, 26, -172, 212, 42)),
         ('giant-hollow', (-152, 209, 37, -159, 209, 49)), ('stair-tree', (-178, 211, 24, -150, 213, 6)), ('highwalk-up', (-146, 216, 8, -100, 214, -12)), ('highwalk-a', (-68, 216, -50, -106, 214, -84)),
         ('stump-a-gate', (-178, 210, -60, -150, 212, -82)), ('stump-a-east', (-66, 211, -106, -96, 214, -99)), ('stump-a-top', (-106, 218, -90, -150, 214, -98)), ('point-a-air', (-128, 262, -56, -128, 213, -96)),
         ('stump-path', (-72, 210, -84, -40, 208, -48)), ('pit-nw-lip', (-54, 213, -60, 0, 200, 0)), ('pit-ramp', (-48, 210, -26, -30, 200, 30)), ('pit-floor', (-30, 199, 34, 0, 206, 0)),
         ('yoke', (10, 199, 40, 0, 210, 0)), ('bridge-n', (-50, 216, -33, 40, 214, -40)), ('bridge-below', (0, 199, -18, 0, 214, -42)), ('gallery-west', (-60, 216, -22, -58, 214, 30)),
         ('root-run', (-10, 210, 100, -40, 204, 88)), ('tunnel-mouth', (-45, 203, 82, -32, 199, 66)), ('tunnel-exit', (-2, 201, 20, -20, 199, 44)), ('long-road', (-150, 211, 136, -70, 209, 114)),
         ('wood-floor', (-196, 210, 12, -170, 212, -20)), ('stump-c-gate', (178, 210, 60, 150, 212, 82)), ('stump-c-top', (106, 218, 90, 150, 214, 98)), ('point-c-air', (128, 262, 136, 128, 213, 96)),
         ('bog', (66, 211, 106, 96, 214, 99)), ('pool', (86, 222, 160, 93, 207, 147)), ('camp-dusk', (200, 222, -96, 236, 208, -124)),
         ('air-nw', (-150, 300, 40, -140, 207, -70)), ('air-sw', (-170, 300, 196, -170, 207, 70)), ('air-mid', (0, 310, 110, 0, 200, -5)), ('air-ne', (150, 300, 60, 140, 207, -60)), ('air-se', (170, 300, 196, 150, 207, 90))]
DUSK = ['yoke', 'pit-nw-lip', 'stump-a-top', 'highwalk-up', 'bridge-n', 'wood-floor', 'stump-c-top', 'camp-dawn']


def main():
    cmd, cache = sys.argv[1], sys.argv[2]
    if cmd == 'build':
        m = Map(); pickle.dump(m, open(cache, 'wb'), protocol=4)
        for l in m.log: print(l)
        print('extra cells', len(m.extra), 'props', len(m.props)); sys.exit(0)
    m = pickle.load(open(cache, 'rb'))
    if cmd == 'check': sys.exit(1 if check(m) else 0)
    elif cmd == 'nonfull':
        d = m.walk(SPAWN['dawn']); feet = {}
        for (x, y, z) in d: feet.setdefault((x, z), []).append(y)
        rows = {}
        for (x, y, z), v in list(m.extra.items()) + [((x, m.h[(x, z)], z), 'water') for (x, z), b in m.surf.items() if b == 'water']:
            if not nonfull(v): continue
            n = v.split('[')[0]
            near = any(abs(f - y) <= 2 for i in (-1, 0, 1) for j in (-1, 0, 1) for f in feet.get((x + i, z + j), ()))
            r = rows.setdefault(n, [0, 0, []]); r[0] += 1; r[1] += near
            if near and len(r[2]) < 6: r[2].append((x, y + 135, z))
        L = ['# Hollowwood non-full blocks', '', 'Counted from the generator. "Beside a route" = within one block sideways and two up or down of a cell a troop can stand on. Positions are test-world coordinates (map y + 135).', '',
             '| block | total | beside a route | examples |', '|---|---|---|---|']
        for n, (t, k, ex) in sorted(rows.items(), key=lambda kv: -kv[1][1]): L.append('| %s | %d | %d | %s |' % (n, t, k, ' '.join('(%d, %d, %d)' % q for q in ex)))
        open(sys.argv[3], 'w', newline=chr(10)).write(chr(10).join(L) + chr(10)); print(len(rows), 'kinds')
    elif cmd == 'render':
        from mapforge import render
        out, view = sys.argv[3], sys.argv[4]
        box = tuple(int(v) for v in sys.argv[5:9]) if len(sys.argv) >= 9 else (X0, Z0, X1, Z1)
        lo, hi = (box[0], YLO, box[1]), (box[2], YHI, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Hollowwood %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'export':
        outdir = sys.argv[3]; os.makedirs(outdir, exist_ok=True)
        for n in sys.argv[4:] or [c + str(r) for r in range(1, 7) for c in 'ABCDEFGHI']:
            b = section_box(n); lo, hi = (b[0], YLO, b[1]), (b[2], YHI, b[3])
            st = m.world(lo, hi, solid=True).export_structure(lo, hi)
            anc = m.anchors(lo, hi); at = {tuple(q[0]): i for i, q in enumerate(st.blocks)}
            for pos, (state, tag, depth) in anc.items():
                rel = (pos[0] - lo[0], pos[1] - lo[1], pos[2] - lo[2]); state = normalize(state) if not state.startswith('minecraft:') else state
                if state not in st.palette: st.palette.append(state)
                st.blocks[at[rel]] = (st.blocks[at[rel]][0], st.palette.index(state), prop_anchor.nbt_tag(tag))
            st.save(os.path.join(outdir, 'hw_%s.nbt' % n.lower())); print(n, b, 'props', len(anc), 'shallow', sum(dp == 1 for _, _, dp in anc.values()))
    elif cmd == 'shots':
        L = ['# Hollowwood: every section placed, a picture of each before the next row hides it.', 'note Hollowwood sections', 'mc time set 6000']
        for r in range(1, 7):
            for c in 'ABCDEFGHI':
                b = section_box(c + str(r)); cx = (b[0] + b[2] + 1) // 2
                L += ['mc execute as @a at @s run forceload add %d %d %d %d' % b, 'mc execute as @a at @s run place template ggmaps:hw/sec/hw_%s%d %d %d %d' % (c.lower(), r, b[0], YLO + 135, b[1]),
                      'props %d %d %d %d %d %d' % (b[0], YLO + 135, b[1], b[2], YHI + 135, b[3]), 'wait 10', 'shot sec-%s%d %d %d %d %d %d %d' % (c.lower(), r, cx, 304, b[3] + 24, cx, 207, b[1] + 26)]
        L += ['props %d %d %d %d %d %d' % (X0, YLO + 135, Z0, X1, YHI + 135, Z1), 'wait 20'] + ['shot %s %s' % (n, ' '.join(str(v) for v in c)) for n, c in VIEWS]
        L += ['shot top-pit 0 300 0 0 90', 'mc time set 12700', 'wait 20'] + ['shot %s-dusk %s' % (n, ' '.join(str(v) for v in c)) for n, c in VIEWS if n in DUSK]
        open(sys.argv[3], 'w', newline='\n').write('\n'.join(L) + '\n'); print(len(L), 'lines')


if __name__ == '__main__':
    import hwbuild
    hwbuild.main()
