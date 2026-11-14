"""Frostmarch "The Switchback": the whole built map. Terrain as columns, the approved pieces stamped on top. See ../plan.md.

  python fmbuild.py build CACHE.pkl
  python fmbuild.py check CACHE.pkl
  python fmbuild.py render CACHE.pkl OUT.png top|iso [x0 z0 x1 z1]
  python fmbuild.py export CACHE.pkl OUTDIR [A1 ...]
  python fmbuild.py shots CACHE.pkl OUT.txt
  python fmbuild.py nonfull CACHE.pkl OUT.md

x east, z south. Camps y 64, the Cut 72, shelves 80. Half-turn symmetric: everything at (x, z) has its twin at (-x, -z);
only the Signal Tower (north shelf) and the Refuge (south shelf) differ, and they share one footprint."""
import sys, os, math, pickle
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, '..', '..', 'ironhold', 'gen')))
import ihbuild                                              # the shared machinery: props, anchors, the walk
from ihbuild import cls, mix
import xf, prop_anchor
sys.path.insert(0, HERE)
import spine, tower, refuge, fmpieces
from fmmap import NORTH, ROADS, ROAD_W, CRAGS, TARNS, SHELF, RAMP, POINTS, SPAWN, CONVOY, X0, X1, Z0, Z1, SEED, lerp, seg_d, base, turn
from mapforge.world import World, Cell
from mapforge.structure import normalize
from mapforge.terrain import fbm, hash01

YLO, YHI = 60, 140
TOWER, REFUGE = (36, -129), (-36, 129)
GATES = [(-54, -112), (54, -112)]                           # shelf gatehouses (the west one is the Siege gate); twins on the south shelf
WAY = (-113, -93)                                           # waystation at the first bend
HOLD = (-196, -90)                                          # the neutral hold plot on the tarn shore
ROAD_M = [('gravel', 40), ('stone', 22), ('andesite', 18), ('cobblestone', 12), ('snow_block', 8)]
PATH_M = [('gravel', 30), ('stone', 14), ('snow_block', 44), ('andesite', 12)]
SCOUR = [('stone', 30), ('andesite', 24), ('gravel', 26), ('coarse_dirt', 20)]
CAMP_M = [('coarse_dirt', 40), ('rooted_dirt', 18), ('snow_block', 30), ('gravel', 12)]
BOULDER = [('stone', 34), ('andesite', 30), ('cobblestone', 20), ('tuff', 16)]
CROSSINGS = ('cut', 'gap n', 'gap s', 'shelf a', 'shelf c')


def road_cells(w, pts):
    out = set()
    for (ax, az), (bx, bz) in zip(pts, pts[1:]):
        for x in range(int(min(ax, bx)) - w - 1, int(max(ax, bx)) + w + 2):
            for z in range(int(min(az, bz)) - w - 1, int(max(az, bz)) + w + 2):
                if seg_d(x, z, ax, az, bx, bz) <= w + 0.5: out.add((x, z))
    return out


class Map(ihbuild.Map):
    def __init__(self):
        self.h, self.g, self.surf, self.kind = {}, {}, {}, {}
        self.extra, self.used, self.log, self.props = {}, set(), [], []
        self.terrain(); self.landmarks(); self.west(); self.index()

    # ---------------------------------------------------------------- terrain
    def terrain(self):
        self.road = set().union(*[road_cells(w, pts) for w, pts in ROADS])
        self.wagon = road_cells(4, ROAD_W) | road_cells(4, turn(ROAD_W))
        for x in range(X0, X1 + 1):
            for z in range(Z0, Z1 + 1):
                c = (x, z) if (x < 0 or (x == 0 and z <= 0)) else (-x, -z)
                h, g, b, k = self.col(*c)
                self.h[(x, z)], self.g[(x, z)], self.surf[(x, z)], self.kind[(x, z)] = h, g, b, k

    def col(self, x, z):
        g = base(x + fbm(SEED + 30, x, z, 24, 2) * 12 * min(1.0, (abs(x) - 62) / 20.0)) if abs(x) > 62 else base(x); r = hash01(SEED, x, z, 3)
        if WAY[0] - 13 <= x <= WAY[0] + 12 and WAY[1] - 8 <= z <= WAY[1] + 13: g = 69                 # the wagon stop stands on a level pad
        if HOLD[0] - 14 <= x <= HOLD[0] + 15 and HOLD[1] - 10 <= z <= HOLD[1] + 12: g = 64                 # and so does the hold plot
        n = lerp(NORTH, x) + fbm(SEED, x / 23.0, 0.5, 3) * 10 - 5
        s = -lerp(NORTH, -x) - fbm(SEED, -x / 23.0, 0.5, 3) * 10 + 5
        out = max(n - z, z - s, -284.5 - x + fbm(SEED + 12, x, z, 7, 2) * 1.6)
        if abs(x) <= SHELF[0] + 8 and abs(z) > 112 + SHELF[1]: out = max(out, abs(z) - 112 - SHELF[1])
        for pz in (-112, 112):
            dz = z - pz
            if abs(x) <= SHELF[0] and abs(dz) <= SHELF[1]:
                d = math.hypot(x, dz)
                if d < 17: return 80, 80, ('polished_andesite' if d >= 15.8 else 'snow_block'), 'ring'
                p = fbm(SEED + 21, x, z, 13, 3)
                return 80, 80, (mix(r, SCOUR) if p > 0.42 else 'snow_block'), 'ground'
            if SHELF[0] < abs(x) <= SHELF[0] + RAMP and abs(dz) <= 5:
                return max(g, 80 - ((abs(x) - SHELF[0]) * 10 + RAMP - 1) // RAMP), g, mix(r, ROAD_M), 'ground'
            if abs(x) <= SHELF[0] + 2 and abs(dz) <= SHELF[1] + 2 and out <= 0 and not abs(dz) <= 5:
                return 82 + (r < 0.35), g, ('snow_block' if r < 0.5 else 'stone' if r < 0.8 else 'andesite'), 'rock'
        if out > 0:
            nz = fbm(SEED + 1, x, z, 31, 4)
            h = g + 4 + 30 * (1 - math.exp(-out / 16.0)) + (nz + 1) * 8 * min(1.0, out / 6.0) + fbm(SEED + 2, x, z, 6, 2) * 2 + (fbm(SEED + 8, x, z, 47, 3) + 0.35) * 16 * min(1.0, out / 24.0)
            h = max(g + 4, min(136, int(h)))
            if abs(x) <= SHELF[0] + 12 and abs(z) >= 112 + SHELF[1] - 2: h = max(h, 86 + int(min(out, 20) * 0.8))
            p = fbm(SEED + 3, x, z, 11, 3) + (r - 0.5) * 0.35
            if h > 88 + nz * 6: b = 'calcite' if p > 0.55 else 'diorite' if p > 0.48 else 'stone' if p < -0.62 else 'snow_block'
            else: b = 'snow_block' if p > -0.05 else 'stone' if p > -0.3 else 'andesite' if p > -0.5 else 'tuff' if p > -0.7 else 'deepslate'
            if out > 14 and abs(fbm(SEED + 6, x, z, 14, 2)) < 0.014: b = 'blue_ice' if r < 0.3 else 'packed_ice'
            return h, g, b, 'mtn'
        if (x, z) == (0, 0) or math.hypot(x, z) < 17:
            d = math.hypot(x, z)
            return g, g, ('polished_andesite' if d >= 15.8 else mix(r, ROAD_M) if (x, z) in self.road else 'snow_block'), 'ring'
        rock = max(1 - math.hypot((x - cx) / rx, (z - cz) / rz) for cx, cz, rx, rz in CRAGS)
        if rock > -0.3: rock += fbm(SEED + 11, x, z, 9, 2) * 0.24
        if rock > 0 and (x, z) not in self.road:
            h = max(g + 4, int(g + 4 + rock ** 0.6 * 11 + fbm(SEED + 2, x, z, 5, 3) * 3 + (fbm(SEED + 31, x, z, 17, 2) + 0.2) * 12 * min(1.0, rock * 3) + (7 if fbm(SEED + 32, x, z, 6, 2) > 0.45 and rock > 0.3 else 0)))
            p = fbm(SEED + 3, x, z, 11, 3)
            return h, g, ('snow_block' if rock > 0.4 or p > 0.2 else 'stone' if p > -0.2 else 'andesite' if p > -0.5 else 'tuff'), 'rock'
        for cx, cz, rx, rz in TARNS:
            t = math.hypot((x - cx) / rx, (z - cz) / rz)
            if t < 1: return 63, g, ('packed_ice' if fbm(SEED + 22, x, z, 6, 2) < 0.25 else 'blue_ice'), 'ice'
            if t < 1 + 3.0 / min(rx, rz): return 67 + (r < 0.3), g, ('stone' if r < 0.4 else 'snow_block' if r < 0.8 else 'andesite'), 'rock'
        if (x, z) in self.wagon: return g, g, mix(r, ROAD_M), 'ground'
        if (x, z) in self.road: return g, g, mix(r, PATH_M), 'ground'
        if -283 <= x <= -233 and abs(z) <= 35: return g, g, ihbuild.patch(x, z, 23, CAMP_M, r), 'ground'
        p = fbm(SEED + 21, x, z, 13, 3)
        return g, g, (mix(r, SCOUR) if p > 0.46 else 'snow_block'), 'ground'

    # ---------------------------------------------------------------- stamping
    def stamp(self, name, cells, ox, oz, q=0, G=80, ok=('ground',)):
        c = xf.turn(dict(cells), q) if q else cells
        gy = self.h[(ox, oz)]; foot = {(x + ox, z + oz) for (x, y, z), v in c.items() if v != 'air'}
        bad = [col for col in foot if col not in self.h or self.kind[col] not in ok or self.h[col] != gy or col in self.used]
        xs, zs = [k[0] for k in foot], [k[1] for k in foot]
        self.log.append('%-14s x %d..%d z %d..%d ground %d%s' % (name, min(xs), max(xs), min(zs), max(zs), gy, '  CONFLICT %d cells e.g. %s %s' % (len(bad), bad[0], self.kind.get(bad[0])) if bad else ''))
        for (x, y, z), v in c.items(): self.put(x + ox, y - G + gy, z + oz, v)
        for (x, z) in foot:
            for i in (-1, 0, 1):
                for j in (-1, 0, 1): self.used.add((x + i, z + j))

    def landmarks(self):
        for (x, y, z), v in spine.spine(72).items():                                     # the Spine; it runs in under the shelves
            if abs(z) >= 84 or (x, z) not in self.h: continue
            if y == 72:
                if self.kind[(x, z)] == 'ground' and (x, z) not in self.wagon: self.surf[(x, z)] = v
            else:
                self.put(x, y, z, v)
                if v != 'air': self.used.add((x, z))
        self.stamp('signal tower', tower.tower(80), TOWER[0], TOWER[1])
        self.stamp('refuge', refuge.refuge(80), REFUGE[0], REFUGE[1], q=2)

    def clear_of_road(self, x, z, r):
        return not any((x + i, z + j) in self.road for i in range(-r, r + 1) for j in range(-r, r + 1)) and all(math.hypot(x - px, z - pz) > 19 + r for px, pz in POINTS.values())

    # ---------------------------------------------------------------- the west half, then its twin
    def west(self):
        before = dict(self.extra); used0 = set(self.used); surf0 = dict(self.surf); E = self.extra
        def top(x, z): return self.h[(x, z)]
        def use(x, z, r=1):
            for i in range(-r, r + 1):
                for j in range(-r, r + 1): self.used.add((x + i, z + j))
        gh = fmpieces.gatehouse(80)
        for i, (x, z) in enumerate(GATES): self.stamp('gatehouse %d' % i, gh, x, z)
        self.stamp('waystation', fmpieces.waystation(64), WAY[0], WAY[1], G=64)
        hx, hz = HOLD
        self.stamp('hold hut 1', fmpieces.hut(64, 0), hx - 6, hz - 2, G=64); self.stamp('hold hut 2', fmpieces.hut(64, 3), hx + 7, hz + 3, q=1, G=64)
        for (i, j, b) in ((0, 6, 'barrel[facing=up]'), (1, 6, 'barrel[facing=up]'), (0, 7, 'spruce_log[axis=x]'), (-13, 5, 'spruce_log[axis=z]'), (-13, 6, 'spruce_log[axis=z]'), (12, -6, 'hay_block[axis=y]')):
            E[(hx + i, top(hx + i, hz + j) + 1, hz + j)] = b; use(hx + i, hz + j)
        # the camp: a palisade with the gate toward the pass, tents, pavilion, wagons (real props)
        for x in range(-282, -233):
            for z in range(-34, 35):
                if not (x in (-282, -234) or abs(z) == 34) or (x == -234 and abs(z) <= 7): continue
                if self.kind[(x, z)] != 'ground': self.log.append('palisade at %d %d on %s  CONFLICT' % (x, z, self.kind[(x, z)])); continue
                hh = 4 + ((x + z) % 3 == 0)
                for y in range(1, hh + 1): E[(x, top(x, z) + y, z)] = 'spruce_log[axis=y]' if y < hh else 'stripped_spruce_log[axis=y]'
                use(x, z, 0)
        for z in (-8, 8):                                                               # gate posts
            for y in range(1, 8): E[(-234, top(-234, z) + y, z)] = 'spruce_log[axis=y]'
            E[(-234, top(-234, z) + 8, z)] = 'snow_block'
        for i, x in enumerate((-276, -264, -252, -240)):
            self.prop('tent_soldier_large_dawn', x, -27, 180, 'tent n%d' % i); self.prop('tent_soldier_large_dawn', x, 27, 0, 'tent s%d' % i)
        self.prop('tent_pavilion_large_dawn', -273, 0, 90, 'pavilion')
        self.prop('supply_wagon_covered', -242, -17, 90, 'wagon camp n'); self.prop('supply_wagon_hay', -242, 17, 270, 'wagon camp s'); self.prop('supply_wagon_barrels', -262, -14, 0, 'wagon camp w')
        self.prop('brazier_tall', -236, -10, 0, 'brazier gate n'); self.prop('brazier_tall', -236, 10, 0, 'brazier gate s')
        for (x, z) in ((-262, 12), (-250, -20), (-252, 14), (-270, 14)): self.crates(x, z)
        y0 = top(-261, 0); E[(-261, y0 + 1, 3)] = 'hay_block[axis=x]'; E[(-262, y0 + 1, 5)] = 'spruce_log[axis=x]'; E[(-260, y0 + 1, 5)] = 'spruce_log[axis=z]'; use(-261, 4, 2)
        # the wagon stop: brazier, cart, a wagon standing off the road
        self.prop('brazier_tall', WAY[0] + 10, WAY[1] + 8, 0, 'brazier way'); self.prop('handcart_sacks', WAY[0] + 3, WAY[1] + 9, 90, 'cart way'); self.prop('supply_wagon', WAY[0] - 4, WAY[1] + 10, 90, 'wagon way')
        self.prop('handcart', hx + 2, hz - 6, 0, 'cart hold')
        self.scatter()
        new = {k: v for k, v in self.extra.items() if before.get(k) != v}               # everything above, turned half a turn
        for (x, y, z), v in xf.turn(new, 2).items():
            if (x, y, z) in self.extra and (x, y, z) not in new: self.log.append('twin cell on an old cell %s  CONFLICT' % ((x, y, z),))
            self.extra[(x, y, z)] = v
        for (x, z) in list(self.used - used0): self.used.add((-x, -z))
        for (x, z), b in list(self.surf.items()):
            if surf0[(x, z)] != b: self.surf[(-x, -z)] = b
        for pid, (x, ay, z), facing in list(self.props):
            pid2 = pid.replace('_dawn', '_dusk'); a = prop_anchor.anchor(pid2, (-x, ay, -z), (facing + 180) % 360, 'minecraft:stone', 1)
            for c in a.solid: self.extra[c] = 'barrier'
            self.props.append((pid2, (-x, ay, -z), (facing + 180) % 360))

    def crates(self, x, z):
        kinds = ('barrel[facing=up]', 'hay_block[axis=y]', 'barrel[facing=up]', 'spruce_planks', 'barrel[facing=east]')
        for n, (i, j) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
            if n == 3 and hash01(SEED, x, z, 37) < 0.5: continue
            self.extra[(x + i, self.h[(x + i, z + j)] + 1, z + j)] = kinds[int(hash01(SEED, x + i, z + j, 38) * 5)]
            if n < 2: self.extra[(x + i, self.h[(x + i, z + j)] + 2, z + j)] = kinds[int(hash01(SEED, x + i, z + j, 39) * 5)]
        for i in range(-2, 4):
            for j in range(-2, 4): self.used.add((x + i, z + j))

    def scatter(self):
        E = self.extra
        def top(x, z): return self.h[(x, z)]
        def use(x, z, r=1):
            for i in range(-r, r + 1):
                for j in range(-r, r + 1): self.used.add((x + i, z + j))
        def pine(x, z, big=None):
            hh = (9 + int(hash01(SEED, x, z, 36) * 6)) if big is None else big; y0 = top(x, z)
            for y in range(1, hh + 1): E[(x, y0 + y, z)] = 'spruce_log[axis=y]'
            rad = [0, 1, 1, 2, 1, 2, 3, 2, 3, 3, 2, 3]; i = 0; y = y0 + hh + 1
            while y >= y0 + 4 and i < len(rad):
                rr = rad[i]
                for a in range(-rr, rr + 1):
                    for c in range(-rr, rr + 1):
                        if abs(a) + abs(c) <= rr + (rr > 1) and (a or c or i == 0) and (x + a, z + c) in self.h:
                            E[(x + a, y, z + c)] = 'spruce_leaves[persistent=true]'
                            if (i == 0 or rr >= rad[i - 1]) and hash01(SEED, x + a, y, z + c, 51) < 0.55 and (x + a, y + 1, z + c) not in E: E[(x + a, y + 1, z + c)] = 'snow_block'
                y -= 1; i += 1
            use(x, z, 2)
        def boulder(x, z):
            big = hash01(SEED, x, z, 31) < 0.45
            for i in range(-1, 2 if big else 1):
                for j in range(-1, 2 if big else 1):
                    hh = 3 + (hash01(SEED, x + i, z + j, 32) < 0.5) + (big and i == 0 and j == 0)
                    for y in range(1, hh + 1): E[(x + i, top(x + i, z + j) + y, z + j)] = mix(hash01(SEED, x + i, z + j, 33 + y), BOULDER)
                    if hash01(SEED, x + i, z + j, 52) < 0.6: E[(x + i, top(x + i, z + j) + hh + 1, z + j)] = 'snow_block'
            use(x, z, 2)
        def bone(x, z):                                                                  # a knuckle of the giant, thrown clear of the Spine
            ax = 'x' if hash01(SEED, x, z, 53) < 0.5 else 'z'
            for i in range(0, 3 + int(hash01(SEED, x, z, 54) * 3)):
                xx, zz = (x + i, z) if ax == 'x' else (x, z + i)
                if not self.free(xx, zz, 0): break
                for y in (1, 2, 3): E[(xx, top(xx, zz) + y, zz)] = 'bone_block[axis=%s]' % ax if hash01(SEED, xx, y, zz, 55) < 0.8 else 'calcite'
                use(xx, zz, 1)
        def drift(x, z):                                                                 # a low snow drift: one full block up, a step and a little cover
            rr = 2 + int(hash01(SEED, x, z, 56) * 3)
            for i in range(-rr, rr + 1):
                for j in range(-rr, rr + 1):
                    if math.hypot(i, j * 1.5) + hash01(SEED, x + i, z + j, 57) * 1.2 <= rr and self.free(x + i, z + j, 0) and (x + i, z + j) not in self.road:
                        E[(x + i, top(x + i, z + j) + 1, z + j)] = 'snow_block'
            use(x, z, rr)
        def cairn(x, z):
            y = top(x, z)
            E[(x, y + 1, z)] = 'cobblestone'; E[(x, y + 2, z)] = 'mossy_cobblestone' if hash01(SEED, x, z, 58) < 0.3 else 'andesite'; E[(x, y + 3, z)] = 'cobblestone'; E[(x, y + 4, z)] = 'lantern[hanging=false]'
            for (i, j, hh) in ((1, 0, 3), (0, 1, 3), (-1, 0, 3), (1, 1, 3)):
                if hash01(SEED, x + i, z + j, 59) < 0.7 and self.free(x + i, z + j, 0) and (x + i, z + j) not in self.road:
                    for yy in range(1, hh + 1): E[(x + i, top(x + i, z + j) + yy, z + j)] = mix(hash01(SEED, x + i, yy, z + j, 33), BOULDER)
            use(x, z, 2)
        def shrine(x, z):                                                                # a road shrine: a stele with a lamp niche
            y = top(x, z)
            for i in (-1, 0, 1):
                for yy in range(1, 5 if i == 0 else 3): E[(x + i, y + yy, z)] = 'chiseled_stone_bricks' if (i == 0 and yy == 3) else 'stone_bricks' if yy > 1 else 'cobblestone'
            E[(x, y + 5, z)] = 'lantern[hanging=false]'; E[(x - 1, y + 3, z)] = 'snow_block'; E[(x + 1, y + 3, z)] = 'snow_block'; use(x, z, 2)
        for (x, z) in ((-142, -80), (-64, -70), (-30, -34), (-224, -22)):
            if self.free(x, z, 2) and self.clear_of_road(x, z, 1): shrine(x, z)
            else: self.log.append('shrine %d %d not free  CONFLICT' % (x, z))
        w, pts = 4, ROAD_W; run = 0.0; side = 1                                           # cairns mark the wagon road
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            L = math.hypot(bx - ax, bz - az); t = (24 - run % 24) % 24
            while t < L:
                nx, nz = -(bz - az) / L, (bx - ax) / L
                x, z = int(round(ax + (bx - ax) * t / L + nx * 7 * side)), int(round(az + (bz - az) * t / L + nz * 7 * side)); side = -side; t += 24
                if x < -6 and x > -230 and self.free(x, z, 1) and self.clear_of_road(x, z, 1): cairn(x, z)
            run += L
        def near(x, z, p, d): return math.hypot(x - p[0], z - p[1]) < d
        def grid(step, k, chance, pick):
            for gx in range(-284, -6, step):
                for gz in range(-188, 188, step):
                    if hash01(SEED, gx, gz, k) > chance: continue
                    x, z = gx + int(hash01(SEED, gx, gz, k + 1) * (step - 2)), gz + int(hash01(SEED, gx, gz, k + 2) * (step - 2))
                    if x > -6 or (x, z) not in self.h: continue
                    if self.kind[(x, z)] == 'mtn':
                        if self.h[(x, z)] < 104 and hash01(SEED, gx, gz, k + 4) < 0.5 and all(self.kind.get((x + i, z + j)) == 'mtn' and abs(self.h[(x + i, z + j)] - self.h[(x, z)]) <= 3 for i in (-2, 2) for j in (-2, 2)): pine(x, z)
                        continue
                    if not self.free(x, z, 2) or not self.clear_of_road(x, z, 2): continue
                    if any(self.kind.get((x + i, z + j)) not in ('ground', 'ring') for i in range(-7, 8) for j in range(-7, 8)): continue      # nothing that could be a step onto rock
                    if -286 <= x <= -230 and abs(z) <= 38: continue                    # the camp
                    fn = pick(x, z, hash01(SEED, gx, gz, k + 3))
                    if fn: fn(x, z)
        def woods(x, z, r):                                                              # pine woods in the corners and behind the camps
            if x < -150 and (z < -52 or z > 38): return pine if r < 0.8 else boulder if r < 0.9 else drift
            return None
        def cover(x, z, r):
            if abs(x) < 28 and 14 < abs(z) < 84: return bone if r < 0.5 else boulder if r < 0.8 else drift       # along the Spine
            if near(x, z, (-100, -72), 34) or near(x, z, (-176, -44), 26): return boulder if r < 0.4 else pine if r < 0.75 else drift
            if near(x, z, (0, 0), 44): return boulder if r < 0.5 else bone if r < 0.75 else drift
            if abs(z - (-112 if z < 0 else 112)) <= SHELF[1] and abs(x) <= SHELF[0]: return boulder if r < 0.35 else drift if r < 0.6 else None   # the shelves: a little cover
            return drift if r < 0.42 else boulder if r < 0.62 else pine if r < 0.8 else None
        grid(7, 60, 0.72, woods)
        grid(11, 70, 0.6, cover)

    # ---------------------------------------------------------------- cells
    def cells(self, lo, hi, solid=True):
        out = {}
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h, s, k = self.h[(x, z)], self.surf[(x, z)], self.kind[(x, z)]
                low = lo[1] if solid else min([self.h.get((x + i, z + j), h) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h]) - 1
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    out[(x, y, z)] = s if y == h else ('snow_block' if s == 'snow_block' and y >= h - 1 else 'packed_ice' if k == 'ice' else 'stone' if (x * 7 + y * 3 + z * 5) % 11 else 'andesite')
        for (x, y, z), b in self.extra.items():
            if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]:
                if b == 'air': out.pop((x, y, z), None)
                else: out[(x, y, z)] = b
        return out

    def world(self, lo, hi, solid=False):
        w = World('frostmarch')
        for pos, b in self.cells(lo, hi, solid).items(): w.set(pos, Cell(normalize(b)))
        return w


def plugs():
    return {'cut': {(x, z) for x in range(-2, 3) for z in range(-17, 18)},
            'gap n': {(x, z) for x in range(-10, 11) for z in range(-49, -45)}, 'gap s': {(x, z) for x in range(-10, 11) for z in range(46, 50)},
            'shelf a': {(x, z) for x in range(-2, 3) for z in range(-140, -84)}, 'shelf c': {(x, z) for x in range(-2, 3) for z in range(85, 141)}}


def section_box(name):
    c, r = 'ABCDEFGHI'.index(name[0]), int(name[1]) - 1
    return (X0 + 64 * c, Z0 + 64 * r, X0 + 64 * c + 63, Z0 + 64 * r + 63)


def nonfull(v):
    n = v.split('[')[0]
    return cls(v) in (2, 3) or n in ('rail', 'ladder', 'dead_bush', 'candle') or n.endswith(('banner', '_pane', 'pressure_plate', 'trapdoor', 'button', 'carpet'))


def check(m):
    bad = 0
    for l in m.log: print(l); bad += 'CONFLICT' in l
    d = m.walk(SPAWN['dawn']); at = {}
    for (x, y, z), v in d.items(): at.setdefault((x, z), []).append((y, v))
    e = m.walk(SPAWN['dusk']); et = {}
    for (x, y, z), v in e.items(): et.setdefault((x, z), []).append((y, v))
    print('cells Dawn can reach:', len(d), '| Dusk:', len(e)); bad += abs(len(d) - len(e)) > 400
    for n, (px, pz) in POINTS.items():
        ring = [(x, z) for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17]
        ys = sorted({y for c in ring for y, _ in at.get(c, [(None, 0)])}, key=str); stuff = [c for c in ring if m.cols.get(c)]
        print('point', n, 'standing heights', ys, 'cells with anything built:', len(stuff)); bad += len(ys) != 1 or ys[0] is None or bool(stuff)
    def dist(t, p): return round(min(v for _, v in t[p])) if p in t else None
    W = {}
    for n, p in list(POINTS.items()) + [('Dawn spawn', SPAWN['dawn']), ('Dusk spawn', SPAWN['dusk'])]:
        W[n] = (dist(at, p), dist(et, p)); print('walk to %-10s from Dawn %s | from Dusk %s' % (n, W[n][0], W[n][1])); bad += p not in at or p not in et
    if not bad:
        fair = abs(W['B'][0] - W['B'][1]) <= 3 and abs(W['A'][0] - W['C'][1]) <= 3 and abs(W['C'][0] - W['A'][1]) <= 3
        print('walks fair (B equal, Dawn to A = Dusk to C, Dawn to C = Dusk to A):', fair); bad += not fair
    for a, b in zip(CONVOY, CONVOY[1:]):
        dd = m.walk(a[1]); r = [v for (x, y, z), v in dd.items() if (x, z) == b[1]]; print('convoy %s to %s: %s' % (a[0], b[0], round(min(r)) if r else None)); bad += not r
    blocked = [(x, y, z) for (x, y, z), v in m.extra.items() if (x, z) in m.wagon and v != 'air' and y <= m.h[(x, z)] + 4 and abs(x) < 232]
    print('things on or low over the wagon road:', len(blocked), blocked[:6]); bad += bool(blocked)
    for who, t in (('Dawn', d), ('Dusk', e)):
        rock = [c for c in t if m.kind[(c[0], c[2])] in ('mtn', 'rock', 'ice')]
        high = [c for c in t if c[1] - m.h[(c[0], c[2])] >= 4]
        part = [c for c in t if cls(m.blk(c[0], c[1] - 1, c[2])) == 2]
        ice = [c for c in t if any('ice' in (m.blk(c[0] + i, c[1] + k, c[2] + j) or '') or m.kind.get((c[0] + i, c[2] + j)) == 'ice' for i in (-1, 0, 1) for j in (-1, 0, 1) for k in (-1, 0))]
        fire = [c for c in t if any('lit=true' in (m.blk(c[0] + i, c[1] + k, c[2] + j) or '') for i in (-1, 0, 1) for j in (-1, 0, 1) for k in (-1, 0, 1))]
        print(who, 'can stand on mountain, crag rock or ice:', len(rock), rock[:6]); bad += bool(rock)
        print(who, 'can stand 4 or more above the ground:', len(high), sorted(high)[:40]); bad += bool(high)
        print(who, 'stands on stairs, slabs or beds:', len(part), sorted(part)[:8])
        print(who, 'reachable cells beside ice:', len(ice), ice[:5]); bad += bool(ice)
        print(who, 'reachable cells beside a lit campfire:', len(fire), fire[:5]); bad += bool(fire)
    wet = [k for k, v in m.extra.items() if v.split('[')[0] in ('water', 'powder_snow', 'snow') or 'waterlogged=true' in v]
    print('water, powder snow or snow layers on the map:', len(wet)); bad += bool(wet)
    sym = sum(1 for (x, z) in m.h if (-x, -z) in m.h and m.h[(x, z)] != m.h[(-x, -z)])
    fl = sum(1 for (x, z) in at if (-x, -z) not in et)
    print('columns whose height differs from the half-turn twin:', sym, '| Dawn cells whose twin Dusk cannot reach:', fl); bad += sym > 0 or fl > 60
    P = plugs()
    for name in CROSSINGS:
        plug = set().union(*[v for k, v in P.items() if k != name])
        dd = m.walk(SPAWN['dawn'], plug); r = [v for (x, y, z), v in dd.items() if (x, z) == SPAWN['dusk']]
        print('only the %s open: Dawn reaches the Dusk spawn in %s' % (name, round(min(r)) if r else None)); bad += not r
    dd = m.walk(SPAWN['dawn'], set().union(*P.values())); past = [c for c in dd if (c[0], c[2]) == SPAWN['dusk']]
    print('every crossing shut: Dawn reaches the Dusk spawn? %s' % bool(past)); bad += bool(past)
    inv = {}
    for (x, y, z), v in m.extra.items():
        if nonfull(v): inv[v.split('[')[0]] = inv.get(v.split('[')[0], 0) + 1
    print('non-full blocks:', sum(inv.values())); print('  ' + ', '.join('%s %d' % kv for kv in sorted(inv.items(), key=lambda kv: -kv[1])))
    print('props:', len(m.props))
    print('FAIL' if bad else 'OK')
    return bad


VIEWS = [('camp-dawn', (-206, 214, 8, -258, 201, 0)), ('camp-dusk', (206, 214, -8, 258, 201, 0)), ('camp-inside', (-238, 203, 4, -272, 204, 0)), ('first-bend', (-152, 216, -46, -104, 205, -78)),
         ('waystation', (-100, 207, -72, -113, 204, -93)), ('ridge-cleft', (-146, 208, -52, -158, 203, -10)), ('ridge-air', (-130, 260, 60, -130, 205, -6)), ('hold', (-176, 210, -76, -197, 202, -92)),
         ('tarn', (-168, 224, -92, -200, 198, -120)), ('low-road', (-206, 214, 30, -130, 208, 104)), ('ramp-a-west', (-104, 212, -108, -55, 219, -112)), ('gatehouse-a', (-74, 218, -106, -55, 220, -112)),
         ('signal-shelf', (-44, 226, -92, 36, 236, -129)), ('tower', (14, 220, -100, 36, 240, -129)), ('point-a', (0, 250, -80, 0, 215, -112)), ('refuge', (-14, 220, 100, -36, 226, 129)),
         ('refuge-shelf', (44, 226, 92, -36, 226, 129)), ('cut-west', (-44, 214, -26, 0, 212, 0)), ('cut-road', (-22, 210, -13, 14, 212, 8)), ('spine-north', (-36, 218, -30, 0, 216, -60)),
         ('spine-gap-s', (-20, 210, 44, 0, 210, 48)), ('spine-shelf-end', (-30, 216, 60, 0, 216, 84)), ('second-bend', (150, 216, 46, 104, 205, 78)), ('shelf-view', (0, 218, -88, -60, 206, -40)),
         ('air-west', (-210, 290, 110, -200, 200, -20)), ('air-midwest', (-100, 290, 110, -100, 205, -20)), ('air-middle', (0, 300, 120, 0, 205, -10)), ('air-mideast', (100, 290, 110, 100, 205, -20)), ('air-east', (210, 290, 110, 200, 200, -20))]


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
        for (x, y, z), v in m.extra.items():
            if not nonfull(v): continue
            n = v.split('[')[0]
            near = any(abs(f - y) <= 2 for i in (-1, 0, 1) for j in (-1, 0, 1) for f in feet.get((x + i, z + j), ()))
            r = rows.setdefault(n, [0, 0, []]); r[0] += 1; r[1] += near
            if near and len(r[2]) < 6: r[2].append((x, y + 135, z))
        L = ['# Frostmarch non-full blocks', '', 'Counted from the generator. "Beside a route" = within one block sideways and two up or down of a cell a troop can stand on. Positions are test-world coordinates (map y + 135).', '',
             '| block | total | beside a route | examples |', '|---|---|---|---|']
        for n, (t, k, ex) in sorted(rows.items(), key=lambda kv: -kv[1][1]): L.append('| %s | %d | %d | %s |' % (n, t, k, ' '.join('(%d, %d, %d)' % q for q in ex)))
        open(sys.argv[3], 'w', newline=chr(10)).write(chr(10).join(L) + chr(10)); print(len(rows), 'kinds')
    elif cmd == 'render':
        from mapforge import render
        out, view = sys.argv[3], sys.argv[4]
        box = tuple(int(v) for v in sys.argv[5:9]) if len(sys.argv) >= 9 else (X0, Z0, X1, Z1)
        lo, hi = (box[0], YLO, box[1]), (box[2], YHI, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Frostmarch %s' % (box,))
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
            st.save(os.path.join(outdir, 'fm_%s.nbt' % n.lower())); print(n, b, 'props', len(anc), 'shallow', sum(dp == 1 for _, _, dp in anc.values()))
    elif cmd == 'shots':
        L = ['# Frostmarch: every section placed, a picture of each before the next row hides it.', 'note Frostmarch sections', 'mc time set 3000']
        for r in range(1, 7):
            for c in 'ABCDEFGHI':
                b = section_box(c + str(r)); cx = (b[0] + b[2] + 1) // 2
                L += ['mc execute as @a at @s run forceload add %d %d %d %d' % b, 'mc execute as @a at @s run place template ggmaps:fm/sec/fm_%s%d %d %d %d' % (c.lower(), r, b[0], YLO + 135, b[1]),
                      'props %d %d %d %d %d %d' % (b[0], YLO + 135, b[1], b[2], YHI + 135, b[3]), 'wait 10', 'shot sec-%s%d %d %d %d %d %d %d' % (c.lower(), r, cx, 290 if r in (1, 6) else 256, b[3] + 24, cx, 206, b[1] + 26)]
        L += ['props %d %d %d %d %d %d' % (X0, YLO + 135, Z0, X1, YHI + 135, Z1), 'wait 20'] + ['shot %s %s' % (n, ' '.join(str(v) for v in c)) for n, c in VIEWS]
        L += ['shot top-spine-n 0 292 -52 0 90', 'shot top-spine-s 0 292 52 0 90', 'shot top-cut 0 300 0 0 90']
        open(sys.argv[3], 'w', newline='\n').write('\n'.join(L) + '\n'); print(len(L), 'lines')


if __name__ == '__main__':
    import fmbuild
    fmbuild.main()
