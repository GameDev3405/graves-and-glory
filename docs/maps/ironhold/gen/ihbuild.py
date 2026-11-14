"""Ironhold "The Iron Pass": the whole built map. Terrain as columns, the approved pieces stamped on top. See ../plan.md.

  python ihbuild.py build CACHE.pkl                 build the map and save it (the other commands load it)
  python ihbuild.py check CACHE.pkl                 walks, rings, openings, escapes, lava, non-full blocks
  python ihbuild.py render CACHE.pkl OUT.png top|iso [x0 z0 x1 z1]
  python ihbuild.py export CACHE.pkl OUTDIR [A1 ...]   one 64 x 64 template per section
  python ihbuild.py shots CACHE.pkl OUT.txt          the in-game script that places and photographs every section

x east, z south. Valley y 64, outer ward 70, inner and upper ward 76. Dawn attacks from the west."""
import sys, os, math, pickle, heapq
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import works                                                # first: it puts the Rivergate kit on the path
import curtain, gate, keep, xf
from kit import Build
from ihmap import NORTH, SOUTH, edge, seg_d, SEED, POINTS, SPAWN, X0, X1, Z0, Z1, ROCKS
from mapforge.world import World, Cell
from mapforge.structure import normalize
from mapforge.terrain import fbm, hash01
import prop_anchor

YLO, YHI = 60, 140
G0 = works.G
OUT = [(-86, -170)] + curtain.OUTER[1:-1] + [(-86, 170)]
INN = [(30, -135), (38, -67), (46, -44), (40, -30), (40, 30), (46, 49), (36, 72), (28, 130)]
UPP = [(166, -100), (174, -44), (172, -30), (172, 30), (176, 44), (166, 100)]
MINE, NPOST, SPOST = curtain.MINE, (38, -67), (46, 49)
LANES1, LANES2 = (MINE[1],), (NPOST[1], SPOST[1])
ROADS = [(4, [(-256, 0), (-206, 0), (-176, 8), (-150, -6), (-112, 0), (-88, 0), (-60, 0), (-30, -6), (0, 0), (24, 0), (52, 0), (76, 6), (112, 0), (160, 0), (184, 0), (256, 0)]),
         (2, [(-206, 0), (-186, -52), (-146, -84), (-112, -96), (-60, -96), (-38, -92), (4, -86), (14, -67), (52, -67), (76, -40), (112, 0)]),
         (2, [(-206, 0), (-190, 70), (-150, 96), (-110, 86), (-90, 67), (-50, 79), (-34, 92), (8, 86), (20, 49), (60, 49), (80, 36), (112, 0)]),
         (2, [(-38, -92), (-38, -40), (-30, -6), (-33, 30), (-33, 70), (-34, 92)])]
VOLCANO = (252, -160)
SPUR = (96, -86, 148, -45)
# ground mixes are laid in drifts (see patch): neighbours in a list sit next to each other on the ground
VALLEY_A = [('smooth_basalt', 10), ('tuff', 26), ('gravel', 12), ('soul_soil', 20), ('rooted_dirt', 18), ('coarse_dirt', 14)]
VALLEY_G = [('coarse_dirt', 20), ('podzol', 16), ('grass_block', 64)]
VALLEY_ASH = [('blackstone', 10), ('smooth_basalt', 16), ('tuff', 36), ('gravel', 14), ('soul_soil', 24)]
GLACIS_M = [('deepslate', 22), ('cobbled_deepslate', 50), ('tuff', 28)]
WARD_M = [('deepslate_tiles', 24), ('cobbled_deepslate', 66), ('tuff', 10)]
INNER_WARD = [('deepslate_tiles', 34), ('cobbled_deepslate', 58), ('tuff', 8)]
UPPER_WARD = [('polished_deepslate', 14), ('deepslate_tiles', 36), ('cobbled_deepslate', 44), ('tuff', 6)]
ROAD_F = [('gravel', 45), ('cobblestone', 30), ('cobbled_deepslate', 25)]
ROCK_LO = [('stone', 40), ('tuff', 25), ('deepslate', 20), ('cobbled_deepslate', 15)]
ROCK_HI = [('deepslate', 44), ('tuff', 18), ('blackstone', 22), ('basalt', 16)]
BOULDER = [('cobblestone', 30), ('tuff', 30), ('stone', 25), ('andesite', 15)]


def mix(r, m):
    r *= sum(w for _, w in m)
    for b, w in m:
        r -= w
        if r <= 0: return b
    return m[-1][0]


def patch(x, z, k, m, r):
    return mix(min(0.999, max(0.0, 0.5 + fbm(SEED + k, x, z, 15, 3) * 0.95 + (r - 0.5) * 0.16)), m)


def ramp(sd): return max(0, min(6, int((sd + 16) // 2)))


def band(line, x, z, xa, xb):
    if x < xa: return -99.0
    if x > xb: return 99.0
    return curtain.near(line, x, z)[0]


class Shift:
    """A Build seen through an offset, so a piece written for the outer wall's place and height can stand elsewhere."""
    def __init__(self, b, dx=0, dy=0): self.b, self.dx, self.dy = b, dx, dy
    def set(self, x, y, z, v): self.b.set(x + self.dx, y + self.dy, z, v)
    def get(self, x, y, z): return self.b.get(x + self.dx, y + self.dy, z)
    def pick(self, m): return self.b.pick(m)


class Map:
    def __init__(self):
        self.h, self.g, self.surf, self.kind, self.sd = {}, {}, {}, {}, {}
        self.extra, self.used, self.log = {}, set(), []
        self.terrain(); self.fort(); self.town(); self.valley(); self.dress()
        self.index()

    # ---------------------------------------------------------------- terrain
    def terrain(self):
        road = {}
        for w, pts in ROADS:
            for (ax, az), (bx, bz) in zip(pts, pts[1:]):
                for x in range(int(min(ax, bx)) - w - 1, int(max(ax, bx)) + w + 2):
                    for z in range(int(min(az, bz)) - w - 1, int(max(az, bz)) + w + 2):
                        if seg_d(x, z, ax, az, bx, bz) <= w + 0.5: road[(x, z)] = True
        self.road = road
        vx, vz = VOLCANO
        for x in range(X0, X1 + 1):
            n, s = edge(x)
            for z in range(Z0, Z1 + 1):
                s1, s2, s3 = band(OUT, x, z, -116, -34), band(INN, x, z, -2, 78), band(UPP, x, z, 140, 204)
                l1 = s1 + (5 if any(abs(z - lz) <= 3 for lz in LANES1) else 0)
                l2 = s2 + (5 if any(abs(z - lz) <= 3 for lz in LANES2) else 0)
                g = 64 + ramp(l1) + ramp(l2)
                r = hash01(SEED, x, z, 3)
                out = max(n - z, z - s, -284.2 - x + fbm(SEED + 12, x, z, 7, 2) * 1.6, x - 271.5 + fbm(SEED + 12, x, z, 7, 2) * 2.5)
                if SPUR[0] <= x <= SPUR[2] and SPUR[1] <= z <= SPUR[3]: out = -1
                elif SPUR[0] - 6 <= x <= SPUR[2] + 6 and z < SPUR[1]: out = max(out, SPUR[1] - z - 0.5)
                rock = max((1 - math.hypot((x - cx) / rx, (z - cz) / rz)) for cx, cz, rx, rz in ROCKS)
                if rock > -0.3: rock += fbm(SEED + 11, x, z, 9, 2) * 0.24
                if out > 0:
                    nz = fbm(SEED + 1, x, z, 31, 4)
                    h = g + 4 + 34 * (1 - math.exp(-out / 16.0)) + (nz + 1) * 8 * min(1.0, out / 6.0) + fbm(SEED + 2, x, z, 6, 2) * 2 + fbm(SEED + 10, x, z, 3, 2) * 2.4 * min(1.0, out / 5.0) + (fbm(SEED + 8, x, z, 47, 3) + 0.35) * 16 * min(1.0, out / 24.0)
                    h = max(g + 4, min(136, int(h)))
                    p = fbm(SEED + 3, x, z, 11, 3) + (r - 0.5) * 0.35                      # stone in drifts and seams, not speckle
                    if h > 96 + nz * 6: b = 'blackstone' if p > 0.45 else 'basalt' if p > 0.22 else 'deepslate' if p > -0.3 else 'tuff'
                    else: b = 'basalt' if p > 0.45 else 'deepslate' if p > 0.0 else 'cobbled_deepslate' if p > -0.4 else 'tuff' if p > -0.62 else 'stone'
                    if out > 34 and h > 100 and abs(fbm(SEED + 6, x, z, 14, 2)) < 0.012 and math.hypot(x - vx, z - vz) < 150: b = 'magma_block'
                    d = math.hypot(x - vx, z - vz)
                    if d < 72:
                        if d < 8: h, b = 133, 'magma_block'
                        elif d <= 11: h, b = 137 - int(r * 2), 'blackstone'
                        else:
                            hv = int(137 - (d - 11) * 1.0 + fbm(SEED + 4, x, z, 7, 2) * 3)
                            if hv > h: h, b = hv, ('magma_block' if abs(fbm(SEED + 5, x, z, 8, 2)) < 0.035 and d < 46 else 'basalt' if r < 0.45 else 'blackstone' if r < 0.8 else 'smooth_basalt')
                    k = 'mtn'
                elif rock > 0 and (x, z) not in road:
                    h, b, k = int(g + 4 + abs(fbm(SEED + 13, x, z, 5, 2)) * 5 + rock ** 0.6 * 13 + fbm(SEED + 2, x, z, 5, 3) * 4 * min(1.0, rock * 4) + (3 if fbm(SEED + 9, x, z, 4, 2) > 0.4 and rock > 0.15 else 0)), patch(x, z, 11, [('basalt', 14), ('deepslate', 34), ('cobbled_deepslate', 28), ('tuff', 18), ('stone', 6)], r), 'rock'
                    h = max(h, g + 4)
                else:
                    h, k = g, 'ground'
                    if (x, z) in road: b, k = mix(r, gate.ROAD if s1 < -18 else ROAD_F), 'road'
                    elif s1 < -18:
                        p = fbm(SEED + 7, x, z, 19, 3)
                        b = patch(x, z, 12, VALLEY_G if p > 0.1 and x < -226 else VALLEY_ASH if p < -0.2 or s1 > -44 else VALLEY_A, r)
                    elif s1 < 3.5: b = patch(x, z, 13, GLACIS_M, r)
                    elif s2 < -18: b = patch(x, z, 14, WARD_M, r)
                    elif s2 < 3.5: b = patch(x, z, 13, GLACIS_M, r)
                    elif s3 < 3.5: b = patch(x, z, 15, INNER_WARD, r)
                    else: b = patch(x, z, 16, UPPER_WARD, r)
                    for pn, (px, pz) in POINTS.items():
                        d = math.hypot(x - px, z - pz)
                        if d < 17:
                            k = 'ring'
                            if d >= 15.8: b = 'cobbled_deepslate' if pn == 'A' else 'polished_deepslate'
                self.h[(x, z)] = h; self.g[(x, z)] = g; self.surf[(x, z)] = b; self.kind[(x, z)] = k; self.sd[(x, z)] = (s1, s2, s3)
        for x in range(vx - 8, vx + 9):                                                    # the crater lake
            for z in range(vz - 8, vz + 9):
                if math.hypot(x - vx, z - vz) < 8: self.extra[(x, 134, z)] = 'lava'

    # ---------------------------------------------------------------- merging
    def put(self, x, y, z, v):
        if (x, z) not in self.h: return
        if v.startswith('rail'): return                                                    # rails read as blocked to walking mobs
        if v == 'air':
            if y <= self.h[(x, z)]: self.extra[(x, y, z)] = 'air'
            else: self.extra.pop((x, y, z), None)
        else: self.extra[(x, y, z)] = v

    def merge(self, b, mark=True):
        for (x, y, z), v in b.c.items():
            if (x, z) not in self.h: continue
            if self.kind[(x, z)] == 'mtn' and (y <= self.h[(x, z)] or v == 'air'): continue
            self.put(x, y, z, v)
            if mark and v != 'air': self.used.add((x, z))

    def place(self, name, cells, x0, z0, q=0, mx=False, ok=('ground',), chk=True):
        """Stamp a workshop piece (ground at G0) with its low corner at (x0, z0), turned q quarter turns."""
        c = dict(cells)
        if mx: c = xf.mirror(c, 'x')
        c = xf.turn(c, q)
        mnx, mnz = min(k[0] for k in c), min(k[2] for k in c)
        foot = {}
        for (x, y, z) in c: foot.setdefault((x - mnx + x0, z - mnz + z0), []).append(y)
        hs = [self.h[col] for col in foot if col in self.h]
        gy = max(set(hs), key=hs.count)
        bad = [col for col in foot if col not in self.h or self.kind[col] not in ok or (chk and col in self.used)]
        xs, zs = [k[0] for k in foot], [k[1] for k in foot]
        self.log.append('%-12s x %d..%d z %d..%d ground %d%s' % (name, min(xs), max(xs), min(zs), max(zs), gy, '  CONFLICT %d cells e.g. %s %s' % (len(bad), bad[0], self.kind.get(bad[0])) if bad else ''))
        for col in foot:
            if col in self.h and self.kind[col] != 'mtn' and self.h[col] != gy: self.h[col] = gy; self.surf[col] = 'cobbled_deepslate'
        for (x, y, z), v in c.items(): self.put(x - mnx + x0, y - G0 + gy, z - mnz + z0, v)
        for (x, z) in foot:
            for i in (-1, 0, 1):
                for j in (-1, 0, 1): self.used.add((x + i, z + j))

    # ---------------------------------------------------------------- the three walls, the gates, the keep
    def fort(self):
        g = lambda x, z: self.g[(x, z)]
        g6 = lambda x, z: self.g[(x, z)] - 6
        b = Build(seed=101)                                                                # outer curtain
        t0 = curtain.along(OUT, -70, 73)
        curtain.wall(b, OUT, -112, -170, -40, -31, g); curtain.wall(b, OUT, -112, 31, -40, 170, g, breach=t0)
        curtain.breach_extras(b, OUT, g, t0)
        curtain.mine_gate(b, OUT, g); curtain.tower(b, OUT, -66, -60, g); curtain.tower(b, OUT, -64, 52, g)
        gate.build(b)
        self.merge(b)
        self.breach = t0
        b = Build(seed=103); s = Shift(b, 0, 6)                                            # inner curtain, same family, six higher
        curtain.wall(s, INN, 4, -150, 72, -31, g6); curtain.wall(s, INN, 4, 31, 72, 150, g6)
        curtain.mine_gate(s, INN, g6, at=NPOST, mine=False); curtain.mine_gate(s, INN, g6, at=SPOST, mine=False)
        curtain.tower(s, INN, 46, -44, g6); curtain.tower(s, INN, 36, 72, g6)
        self.merge(b)
        b = Build(seed=105); s = Shift(b, 0, 6)                                            # upper wall
        curtain.wall(s, UPP, 148, -120, 196, -31, g6); curtain.wall(s, UPP, 148, 31, 196, 120, g6)
        curtain.tower(s, UPP, 174, -44, g6); curtain.tower(s, UPP, 176, 44, g6)
        self.merge(b)
        for dx, flat in ((112, False), (244, True)):                                       # inner and upper gatehouses: the main one without its timber hoarding
            t = Build(seed=107 + dx); gate.build(t)
            for (x, y, z), v in list(t.c.items()):
                if v.startswith('dark_oak') and x <= gate.W0 - 1 and 81 <= y <= 84 and abs(z) <= 5:
                    if abs(z) <= 4: del t.c[(x, y, z)]
                    else: t.c[(x, y, z)] = 'deepslate_bricks'
            if flat:                                                                       # level ground on both faces: lift the field braziers out of the floor
                for z in (-6, 6):
                    x = gate.W0 - 5
                    for y in (69, 70, 71, 72): t.c.pop((x, y, z), None)
                    t.c[(x, 71, z)] = 'polished_blackstone_brick_wall'; t.c[(x, 72, z)] = 'polished_blackstone'; t.c[(x, 73, z)] = 'lantern[hanging=false]'
                for k in [k for k in t.c if k[1] < 70]: del t.c[k]
            m = Build(seed=1)
            for (x, y, z), v in t.c.items(): m.c[(x + dx, y + 6, z)] = v
            self.merge(m)
        b = Build(seed=109); keep.build(b)
        dark = {'stone': 'deepslate', 'andesite': 'tuff'}                                  # the spur in the fortress's dark stone
        for k_, v in list(b.c.items()):
            if v in dark and k_[1] <= keep.SPUR: b.c[k_] = dark[v]
        self.merge(b)

    # ---------------------------------------------------------------- buildings
    def town(self):
        H = lambda *a, **k: works.hs(*a, **k).c
        P = self.place
        P('forge', works.forge(3), -60, -83, 2); P('smelter', works.smelter(), -33, -84, 2); P('smithy', works.forge(7), -13, -107, 0)
        P('store', works.store(), -55, 34, 0); P('stables', works.stables(), -59, 61, 0)
        P('guard n', H(9, 9, floors=2, style='iron', seed=31), -56, -38, 0); P('guard s', H(9, 9, floors=2, style='soot', seed=33), -56, 18, 2)
        P('yard shop n', H(11, 9, floors=2, style='ash', shop=True, seed=35), 10, -36, 0); P('yard house s', H(9, 9, floors=2, style='iron', seed=37), 12, 26, 2)
        P('ward nw 1', H(11, 11, floors=2, style='iron', door=2, ridge='z', seed=41), 57, -87, 0); P('ward nw 2', H(11, 11, floors=2, style='ash', shop=True, seed=43), 72, -81, 0)
        P('ward sw 1', H(11, 11, floors=2, style='ash', seed=45), 56, 62, 2); P('ward sw 2', H(7, 11, floors=3, style='iron', seed=47), 70, 56, 2)
        P('ward sw 3', H(13, 13, floors=2, style='iron', arch=True, door=2, seed=49), 82, 50, 2)
        P('ward e 1', H(11, 11, floors=2, style='soot', seed=51), 136, -37, 0); P('ward e 2', H(7, 11, floors=3, style='ash', door=5, seed=53), 151, -35, 0)
        P('ward e 3', H(11, 11, floors=2, style='iron', seed=55), 136, 26, 2); P('ward e 4', H(7, 11, floors=2, style='soot', door=3, seed=57), 151, 26, 2)
        P('barracks', works.barracks_yard(), 244, -23, 0, ok=('ground', 'road'))
        P('upper store', works.store(), 196, -46, 0); P('upper stables', works.stables(), 196, 36, 2)

    def prop(self, pid, x, z, facing, name=None, ay=None, chk=True, ok=('ground',)):
        """A real prop (gg_props/<pid>.json) with its anchor cell at (x, ground + 1, z). Its solid cells are written as barriers,
        so the walk check sees them; the hidden anchor command block is written at export (docs/props/ANCHOR-NBT.md)."""
        if ay is None: ay = self.h[(x, z)] + 1
        a = prop_anchor.anchor(pid, (x, ay, z), facing, 'minecraft:stone', 1)
        cols = {(c[0], c[2]) for c in a.solid} | {(x, z)}
        bad = [c for c in cols if c not in self.h or (chk and (self.kind[c] not in ok or c in self.used or self.h[c] != ay - 1))]
        bad += [(c[0], c[2]) for c in a.solid if self.extra.get(c) not in (None, 'air')]
        xs, zs = [c[0] for c in cols], [c[1] for c in cols]
        self.log.append('%-12s x %d..%d z %d..%d ground %d  prop %s facing %d%s' % (name or pid, min(xs), max(xs), min(zs), max(zs), ay - 1, pid, facing, '  CONFLICT %d cells e.g. %s %s' % (len(bad), bad[0], self.kind.get(bad[0])) if bad else ''))
        for c in a.solid: self.extra[c] = 'barrier'
        for (cx, cz) in cols:
            for i in (-1, 0, 1):
                for j in (-1, 0, 1): self.used.add((cx + i, cz + j))
        self.__dict__.setdefault('props', []).append((pid, (x, ay, z), facing))

    def anchors(self, lo, hi):
        """{cell: (state, nbt dict)} of the prop anchors inside a box: the deepest-hidden legal cell, else straight under the floor"""
        out = {}
        for pid, (x, ay, z), facing in self.__dict__.get('props', []):
            def buried(y):
                for i, j, k in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                    b = self.blk(x + i, y + j, z + k)
                    if (x + i, z + k) not in self.h: continue                               # out of the map
                    if not (b == 'T' or (cls(b) == 1 and b.split('[')[0] not in ('barrier', 'command_block'))): return False
                return y >= YLO
            depth = next((d for d in range(2, 7) if buried(ay - d) and cls(self.blk(x, ay - 1, z)) == 1), 1)
            pos = (x, ay - depth, z)
            if not (lo[0] <= pos[0] <= hi[0] and lo[1] <= pos[1] <= hi[1] and lo[2] <= pos[2] <= hi[2]): continue
            floor = self.cells(pos, pos, solid=True).get(pos, 'stone')
            a = prop_anchor.anchor(pid, (x, ay, z), facing, floor, depth)
            out[pos] = (a.state, a.nbt, depth)
        return out

    def valley(self):
        P = self.place
        P('outwork', works.outwork(), -134, -38, 0)
        P('mine head', works.mine_head(), -160, -116, 0)
        P('slag 1', works.slag(6, 5, 1), -184, -102); P('slag 2', works.slag(4, 3, 2), -140, -120); P('slag 3', works.slag(5, 4, 3), -129, -114); P('slag 4', works.slag(4, 3, 4), -50, -110)
        pal = works.palisade(51, gate=False)                                               # the siege camp: three palisade sides and two wings, the road side open
        P('camp north', pal, -282, -35, 0); P('camp south', xf.mirror(pal, 'z'), -282, 34, 0)
        wing = works.palisade(25, gate=False)
        P('camp wing n', wing, -233, -33, 1, chk=False); P('camp wing s', wing, -233, 9, 1, chk=False)
        for i, x in enumerate((-276, -264, -252, -240)):                                   # real props from here on (gg_props): tents, pavilion, engines, wagons
            self.prop('tent_soldier_large_weathered', x, -27, 180, 'tent n%d' % i); self.prop('tent_soldier_large_weathered', x, 27, 0, 'tent s%d' % i)
        self.prop('tent_pavilion_large_dawn', -273, 0, 90, 'pavilion')
        self.prop('trebuchet', -218, -30, 90, 'trebuchet n'); self.prop('trebuchet', -218, 30, 90, 'trebuchet s')
        self.prop('supply_wagon_covered', -242, -17, 90, 'wagon camp n'); self.prop('supply_wagon_hay', -242, 17, 270, 'wagon camp s'); self.prop('supply_wagon_shot', -218, -21, 90, 'wagon shot')
        self.prop('supply_wagon_barrels', -20, 24, 0, 'wagon yard'); self.prop('supply_wagon', 124, 30, 90, 'wagon court'); self.prop('handcart_ore', -127, -83, 90, 'cart mine')
        for x in (-80, -64, 32, 48, 164, 180):                                             # the gate braziers: the block cages make way for the brazier prop
            for z in (-6, 6):
                ys = [y for y in range(self.h[(x, z)] + 1, self.h[(x, z)] + 8) if self.extra.get((x, y, z)) == 'polished_blackstone_brick_wall']
                if not ys: self.log.append('brazier %d %d: no cage found  CONFLICT' % (x, z)); continue
                for y in range(ys[0], ys[0] + 3): self.extra.pop((x, y, z), None)
                self.prop('brazier_tall', x, z, 0, 'brazier', ay=ys[0], chk=False)

    # ---------------------------------------------------------------- cover and dressing
    def free(self, x, z, r=1, kinds=('ground',)):
        return all(self.kind.get((x + i, z + j)) in kinds and (x + i, z + j) not in self.used for i in range(-r, r + 1) for j in range(-r, r + 1))

    def clear_of_road(self, x, z, r):
        return not any((x + i, z + j) in self.road for i in range(-r, r + 1) for j in range(-r, r + 1)) and all(math.hypot(x - px, z - pz) > 19 + r for px, pz in POINTS.values())

    def dress(self):
        E = self.extra
        def top(x, z): return self.h[(x, z)]
        def use(x, z, r=1):
            for i in range(-r, r + 1):
                for j in range(-r, r + 1): self.used.add((x + i, z + j))
        def boulder(x, z, k):
            big = hash01(SEED, x, z, 31) < 0.4
            for i in range(-1, 2 if big else 1):
                for j in range(-1, 2 if big else 1):
                    hh = 2 + (hash01(SEED, x + i, z + j, 32) < 0.5) + (big and i == 0 and j == 0)
                    for y in range(1, hh + 1): E[(x + i, top(x + i, z + j) + y, z + j)] = mix(hash01(SEED, x + i, z + j, 33 + y), BOULDER)
            use(x, z, 2)
        def dead_tree(x, z, k):
            hh = 6 + int(hash01(SEED, x, z, 34) * 4); y0 = top(x, z)
            for y in range(1, hh + 1): E[(x, y0 + y, z)] = 'spruce_log[axis=y]'
            dx, dz = ((1, 0), (-1, 0), (0, 1), (0, -1))[int(hash01(SEED, x, z, 35) * 4)]
            ax, bx_ = ('x' if dx else 'z'), ('z' if dx else 'x')
            for i in (1, 2): E[(x + dx * i, y0 + hh - 2 + (i - 1), z + dz * i)] = 'spruce_log[axis=%s]' % ax       # a limb that turns up
            E[(x + dx * 2, y0 + hh, z + dz * 2)] = 'spruce_log[axis=y]'
            E[(x - dx, y0 + hh - 4, z - dz)] = 'spruce_log[axis=%s]' % ax; E[(x - dx * 2, y0 + hh - 3, z - dz * 2)] = 'spruce_log[axis=y]'
            E[(x - dz, y0 + hh, z + dx)] = 'spruce_log[axis=%s]' % bx_
            use(x, z, 2)
        def spruce(x, z, k):
            hh = 7 + int(hash01(SEED, x, z, 36) * 4); y0 = top(x, z)
            for y in range(1, hh + 1): E[(x, y0 + y, z)] = 'spruce_log[axis=y]'
            for i, rr in enumerate((0, 1, 1, 2, 1, 2, 2)):
                y = y0 + hh + 1 - i
                if y < y0 + 4: break
                for a in range(-rr, rr + 1):
                    for c in range(-rr, rr + 1):
                        if abs(a) + abs(c) <= rr + (rr > 1) and (a or c or i == 0) and (x + a, z + c) in self.h: E[(x + a, y, z + c)] = 'spruce_leaves[persistent=true]'
            use(x, z, 2)
        def mantlet(x, z, k):
            for j in (-1, 0, 1):
                for y in (1, 2): E[(x, top(x, z + j) + y, z + j)] = 'spruce_planks' if (j + y) % 2 else 'stripped_spruce_log[axis=y]'
            E[(x - 1, top(x - 1, z) + 1, z)] = 'spruce_log[axis=x]'
            use(x, z, 2)
        def crates(x, z, k):
            kinds = ('barrel[facing=up]', 'hay_block[axis=y]', 'barrel[facing=up]', 'spruce_planks', 'barrel[facing=east]')
            for n, (i, j) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
                if n == 3 and hash01(SEED, x, z, 37) < 0.5: continue
                E[(x + i, top(x + i, z + j) + 1, z + j)] = kinds[int(hash01(SEED, x + i, z + j, 38) * 5)]
                if n < 2: E[(x + i, top(x + i, z + j) + 2, z + j)] = kinds[int(hash01(SEED, x + i, z + j, 39) * 5)]
            use(x, z, 2)
        def fragment(x, z, k):                                                          # a low scrap of wall, two or three high: cover, never a step
            alongx = hash01(SEED, x, z, 41) < 0.5; n = 4 + int(hash01(SEED, x, z, 42) * 4)
            for i in range(n):
                xx, zz = (x + i, z) if alongx else (x, z + i)
                if not self.free(xx, zz, 0): break
                for y in range(1, 3 + (hash01(SEED, xx, zz, 43) < 0.4)): E[(xx, top(xx, zz) + y, zz)] = mix(hash01(SEED, xx + y, zz, 44), works.RUIN if y > 1 else gate.FOOT)
                use(xx, zz, 1)
        def stakes(x, z, k):                                                            # a burnt stretch of palisade
            n = 5 + int(hash01(SEED, x, z, 45) * 5)
            for i in range(n):
                if not self.free(x, z + i, 0): break
                for y in range(1, 3 + int(hash01(SEED, x, z + i, 46) * 3)): E[(x, top(x, z + i) + y, z + i)] = 'spruce_log[axis=y]' if hash01(SEED, x, z + i, 47) < 0.7 else 'stripped_spruce_log[axis=y]'
                use(x, z + i, 1)
        def well(x, z):
            y0 = top(x, z)
            for i in range(-2, 3):
                for j in range(-2, 3):
                    edge = max(abs(i), abs(j)) == 2
                    if abs(i) == 2 and abs(j) == 2: continue
                    for y in (1, 2): E[(x + i, y0 + y, z + j)] = (mix(hash01(SEED, x + i, z + j, 48 + y), gate.FOOT) if edge else 'polished_blackstone' if y == 1 else 'iron_bars' if (i + j) % 2 else 'dark_oak_planks')
            for (i, j) in ((-2, 0), (2, 0)):
                for y in (3, 4, 5): E[(x + i, y0 + y, z + j)] = 'dark_oak_log[axis=y]'
            for i in range(-2, 3): E[(x + i, y0 + 6, z)] = 'stripped_dark_oak_log[axis=x]'
            E[(x, y0 + 5, z)] = 'chain[axis=y]'; E[(x, y0 + 4, z)] = 'chain[axis=y]'; E[(x, y0 + 3, z)] = 'cauldron'
            use(x, z, 3)
        well(-18, -30); well(84, -8); well(210, 0)
        for (x, z) in ((-262, -14), (-262, 12), (-250, -20), (-252, 14), (-270, 14), (-240, -8)): crates(x, z, 0)       # camp stockpiles
        y0 = top(-261, 0)                                                                 # a cold cook fire by the pavilion
        E[(-261, y0 + 1, 0)] = 'campfire[lit=false]'; E[(-262, y0 + 1, 1)] = 'cauldron'; E[(-261, y0 + 1, 2)] = 'spruce_log[axis=x]'; E[(-260, y0 + 1, -2)] = 'spruce_log[axis=z]'; use(-261, 0, 2)
        def scatter(step, xa, xb, pick, k, chance):
            for gx in range(xa, xb, step):
                for gz in range(-150, 151, step):
                    if hash01(SEED, gx, gz, k) > chance: continue
                    x, z = gx + int(hash01(SEED, gx, gz, k + 1) * (step - 3)), gz + int(hash01(SEED, gx, gz, k + 2) * (step - 3))
                    if not self.free(x, z, 2) or not self.clear_of_road(x, z, 3): continue
                    fn = pick(x, z, hash01(SEED, gx, gz, k + 3))
                    if fn: fn(x, z, k)
        def valley_pick(x, z, r):
            s1 = self.sd[(x, z)][0]
            if s1 > -34: return None                                                       # the last 30 blocks before the wall stay open
            if -284 <= x <= -230 and abs(z) <= 37: return None                             # the camp
            if x < -196 and r < 0.45: return spruce
            return boulder if r < 0.34 else dead_tree if r < 0.56 else stakes if r < 0.68 else fragment if r < 0.8 else mantlet if x > -215 else boulder
        scatter(10, -282, -100, valley_pick, 40, 0.62)
        scatter(13, -58, 22, lambda x, z, r: None if self.sd[(x, z)][0] < 8 or self.sd[(x, z)][1] > -18 else (crates if r < 0.45 else fragment if r < 0.85 else boulder), 50, 0.7)
        scatter(13, 52, 166, lambda x, z, r: None if self.sd[(x, z)][1] < 8 or self.sd[(x, z)][2] > -8 else (crates if r < 0.45 else fragment if r < 0.9 else boulder), 60, 0.6)
        scatter(14, 180, 270, lambda x, z, r: None if self.sd[(x, z)][2] < 8 else (crates if r < 0.6 else fragment), 70, 0.5)
        w, pts = ROADS[0]; run = 0.0; side = 1                                             # lamp posts along the Iron Road
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            L = math.hypot(bx - ax, bz - az); t = (22 - run % 22) % 22
            while t < L:
                nx, nz = -(bz - az) / L, (bx - ax) / L
                x, z = int(round(ax + (bx - ax) * t / L + nx * 6 * side)), int(round(az + (bz - az) * t / L + nz * 6 * side)); side = -side; t += 22
                if not self.free(x, z, 1) or not self.clear_of_road(x, z, 0) or min(abs(v) for v in self.sd[(x, z)]) < 14 or (-284 <= x <= -230): continue
                y = top(x, z)
                E[(x, y + 1, z)] = 'polished_blackstone_brick_wall'; E[(x, y + 2, z)] = 'polished_blackstone'; E[(x, y + 3, z)] = 'lantern[hanging=false]'; use(x, z, 1)
            run += L
        for (x, z), k in self.kind.items():                                                # dead bushes on the scorched ground
            if k == 'ground' and (x, z) not in self.used and self.sd[(x, z)][0] < -20 and self.surf[(x, z)] in ('coarse_dirt', 'rooted_dirt', 'podzol') and hash01(SEED, x, z, 80) < 0.022:
                E[(x, self.h[(x, z)] + 1, z)] = 'dead_bush'

    # ---------------------------------------------------------------- cells
    def index(self):
        self.cols = {}
        for (x, y, z) in self.extra: self.cols.setdefault((x, z), []).append(y)

    def cells(self, lo, hi, solid=True):
        out = {}
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h, s, k = self.h[(x, z)], self.surf[(x, z)], self.kind[(x, z)]
                low = lo[1] if solid else min([self.h.get((x + i, z + j), h) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h]) - 1
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    out[(x, y, z)] = s if y == h else ('deepslate' if k in ('mtn', 'rock') else 'stone') if k in ('mtn', 'rock') or y < h - 3 else 'cobbled_deepslate' if s != 'grass_block' and k != 'ground' else 'stone' if s != 'grass_block' else 'dirt'
        for (x, y, z), b in self.extra.items():
            if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]:
                if b == 'air': out.pop((x, y, z), None)
                else: out[(x, y, z)] = b
        return out

    def world(self, lo, hi, solid=False):
        w = World('ironhold')
        for pos, b in self.cells(lo, hi, solid).items(): w.set(pos, Cell(normalize(b)))
        return w

    # ---------------------------------------------------------------- walking
    def blk(self, x, y, z):
        e = self.extra.get((x, y, z))
        if e is not None: return None if e == 'air' else e
        return 'T' if y <= self.h.get((x, z), 999) else None

    def floors(self, col):
        f = self._fl.get(col)
        if f is None:
            x, z = col; f = []
            if col in self.h and col not in self.plug:
                cand = {self.h[col] + 1}
                for y in self.cols.get(col, ()): cand.add(y + 1); cand.add(y)
                for y in sorted(cand):
                    if cls(self.blk(x, y - 1, z)) in (1, 2) and cls(self.blk(x, y, z)) == 0 and cls(self.blk(x, y + 1, z)) == 0: f.append(y)
            self._fl[col] = f
        return f

    def walk(self, start, plug=()):
        self.plug = set(plug); self._fl = {}
        fs = self.floors(start)
        if not fs: return {}
        s = (start[0], fs[0], start[1]); d = {s: 0.0}; q = [(0.0, s)]
        def step(c, i, j):
            x, y, z = c; best = None
            for f in self.floors((x + i, z + j)):
                if not -3 <= f - y <= 1: continue
                if f == y + 1 and cls(self.blk(x, y + 2, z)) != 0: continue
                if f < y and any(cls(self.blk(x + i, yy, z + j)) != 0 for yy in range(f + 2, y + 2)): continue
                if best is None or abs(f - y) < abs(best - y): best = f
            return best
        while q:
            dc, c = heapq.heappop(q)
            if dc > d[c]: continue
            x, y, z = c
            for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
                f = step(c, i, j)
                if f is None: continue
                if i and j and (step(c, i, 0) != y or step(c, 0, j) != y or f != y): continue
                n = (x + i, f, z + j); nd = dc + (1.414 if i and j else 1.0)
                if nd < d.get(n, 1e9): d[n] = nd; heapq.heappush(q, (nd, n))
        return d


FREE = ('air', 'rail', 'dead_bush', 'ladder', 'candle', 'torch', 'wall_torch')
BLOCKER = ('fence', 'wall', 'iron_bars', 'chain', 'lantern', 'lightning_rod', 'trapdoor', 'door', 'grindstone', 'anvil', 'chest', 'lectern', 'campfire', 'bell', 'lava', 'fence_gate')


def cls(b):
    """0 free, 1 a full block, 2 a part block one can stand on (stairs, slab, bed), 3 in the way"""
    if b is None: return 0
    n = b.split('[')[0]
    if n in FREE or n.endswith('banner'): return 0
    if n.endswith(('_stairs', '_slab', '_bed')): return 2
    if n == 'lava' or any(n == k or n.endswith('_' + k) or n.endswith(k) for k in BLOCKER): return 3
    return 1


VIEWS = [('camp-props', (-226, 214, 2, -262, 201, -12)), ('trebuchet', (-198, 209, -10, -218, 204, -30)), ('gate-brazier', (-92, 209, -16, -80, 205, -6)), ('pavilion', (-252, 206, 10, -273, 204, 0)), ('air-camp', (-250, 262, 70, -215, 200, -10)), ('air-valley', (-190, 265, 60, -110, 200, -10)), ('air-outer-wall', (-125, 262, 70, -72, 206, 0)), ('air-outer-ward', (-30, 268, 75, -10, 206, -10)),
         ('air-inner-wall', (5, 266, 60, 42, 212, 0)), ('air-inner-ward', (100, 272, 70, 110, 212, -15)), ('air-upper', (215, 270, 60, 225, 212, -5)),
         ('camp', (-212, 212, 6, -258, 201, 0)), ('valley', (-228, 216, -12, -72, 214, 0)), ('outwork', (-94, 206, 14, -112, 202, -30)), ('main-gate', (-116, 204, 2, -72, 212, 0)),
         ('mine-gate', (-110, 204, -106, -78, 208, -96)), ('mine-head', (-128, 208, -84, -156, 206, -106)), ('breach', (-102, 206, 62, -70, 206, 73)), ('muster-yard', (-44, 218, 22, 10, 206, -20)),
         ('forges', (-36, 212, -56, -50, 207, -80)), ('stables', (-30, 212, 44, -52, 207, 62)), ('inner-gate', (-2, 208, 2, 40, 216, 0)), ('north-postern', (12, 209, -58, 38, 213, -67)),
         ('south-postern', (20, 209, 40, 46, 213, 49)), ('keep', (112, 214, 22, 114, 232, -64)), ('keep-court', (100, 218, 14, 172, 216, 0)), ('ward-houses', (100, 220, 30, 76, 212, 62)),
         ('upper-gate', (132, 214, 2, 172, 220, 0)), ('barracks', (226, 220, 2, 256, 213, 0)), ('volcano', (236, 222, 30, 252, 262, -150))]


def section_box(name):
    c, r = 'ABCDEFGHI'.index(name[0]), int(name[1]) - 1
    return (X0 + 64 * c, Z0 + 64 * r, X0 + 64 * c + 63, Z0 + 64 * r + 63)


def openings(m):
    def strip(line, cx, cz, half):
        return {(x, z) for x in range(cx - 9, cx + 10) for z in range(cz - half - 3, cz + half + 4) if abs(curtain.near(line, x, z)[0]) <= 1.5}
    bx, bz = curtain.point_at(OUT, m.breach)
    return {'outer': {'main gate': strip(OUT, -72, 0, 4), 'mine gate': strip(OUT, MINE[0], MINE[1], 3), 'breach': strip(OUT, int(bx), int(bz), 14)},
            'inner': {'inner gate': strip(INN, 40, 0, 4), 'north postern': strip(INN, NPOST[0], NPOST[1], 3), 'south postern': strip(INN, SPOST[0], SPOST[1], 3)},
            'upper': {'upper gate': strip(UPP, 172, 0, 4)}}


def check(m):
    bad = 0
    for l in m.log: print(l); bad += 'CONFLICT' in l
    d = m.walk(SPAWN['dawn']); at = {}
    for (x, y, z), v in d.items(): at.setdefault((x, z), []).append((y, v))
    e = m.walk(SPAWN['dusk']); et = {}
    for (x, y, z), v in e.items(): et.setdefault((x, z), []).append((y, v))
    print('cells Dawn can reach:', len(d), '| Dusk:', len(e))
    for n, (px, pz) in POINTS.items():
        ring = [(x, z) for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17]
        ys = sorted({y for c in ring for y, _ in at.get(c, [(None, 0)])}, key=str); stuff = [c for c in ring if m.cols.get(c)]
        print('point', n, 'standing heights', ys, 'cells with anything built:', len(stuff)); bad += len(ys) != 1 or ys[0] is None or bool(stuff)
    def dist(t, p): return round(min(v for _, v in t[p])) if p in t else None
    for n, p in list(POINTS.items()) + [('Dawn spawn', SPAWN['dawn']), ('Dusk spawn', SPAWN['dusk'])]:
        print('walk to %-10s from Dawn %s | from Dusk %s' % (n, dist(at, p), dist(et, p))); bad += p not in at or p not in et
    for who, t in (('Dawn', d), ('Dusk', e)):
        rock = [c for c in t if m.kind[(c[0], c[2])] in ('mtn', 'rock')]
        high = [c for c in t if c[1] - m.g[(c[0], c[2])] >= 5 and not (SPUR[0] - 2 <= c[0] <= SPUR[2] + 2 and SPUR[1] <= c[2] <= SPUR[3] + 8)]
        part = [c for c in t if cls(m.blk(c[0], c[1] - 1, c[2])) == 2]
        hot = [c for c in t if any(m.blk(c[0] + i, c[1] + k, c[2] + j) in ('magma_block', 'lava') or m.surf.get((c[0] + i, c[2] + j)) == 'magma_block' for i in range(-3, 4) for j in range(-3, 4) for k in (-1, 0, 1))] if who == 'Dawn' else []
        fire = [c for c in t if any('lit=true' in (m.blk(c[0] + i, c[1] + k, c[2] + j) or '') for i in (-1, 0, 1) for j in (-1, 0, 1) for k in (-1, 0, 1))]
        print(who, 'can stand on mountain or crag rock:', len(rock), rock[:6]); bad += bool(rock)
        print(who, 'can stand 5 or more above the ground (outside the keep):', len(high), sorted(high)[:8])
        print(who, 'stands on stairs, slabs or beds:', len(part), sorted(part)[:8])
        if who == 'Dawn': print('reachable cells within 3 of magma or lava:', len(hot), hot[:5]); bad += bool(hot)
        print(who, 'reachable cells beside a lit campfire:', len(fire), fire[:5]); bad += bool(fire)
    wet = [k for k, v in m.extra.items() if v.split('[')[0] == 'water' or 'waterlogged=true' in v]
    print('water blocks on the map:', len(wet)); bad += bool(wet)
    ops = openings(m)
    for wall, group in ops.items():
        for name in group:
            plug = set().union(*[v for k, v in group.items() if k != name])
            dd = m.walk(SPAWN['dawn'], plug); r = [v for (x, y, z), v in dd.items() if (x, z) == SPAWN['dusk']]
            print('only the %s open: Dawn reaches the Dusk spawn in %s' % (name, round(min(r)) if r else None)); bad += not r
        dd = m.walk(SPAWN['dawn'], set().union(*group.values())); past = [c for c in dd if (c[0], c[2]) == SPAWN['dusk']]
        print('every %s opening shut: Dawn reaches the Dusk spawn? %s' % (wall, bool(past))); bad += bool(past)
    inv = {}
    for (x, y, z), v in m.extra.items():
        n = v.split('[')[0]
        if cls(v) in (2, 3) or n in ('rail', 'ladder', 'dead_bush', 'candle') or n.endswith('banner'): inv[n] = inv.get(n, 0) + 1
    print('non-full blocks:', sum(inv.values())); print('  ' + ', '.join('%s %d' % kv for kv in sorted(inv.items(), key=lambda kv: -kv[1])))
    print('FAIL' if bad else 'OK')
    return bad


def main():
    cmd, cache = sys.argv[1], sys.argv[2]
    if cmd == 'build':
        m = Map(); pickle.dump(m, open(cache, 'wb'), protocol=4)
        for l in m.log: print(l)
        print('extra cells', len(m.extra)); sys.exit(0)
    m = pickle.load(open(cache, 'rb'))
    if cmd == 'check': sys.exit(1 if check(m) else 0)
    elif cmd == 'nonfull':                                                                 # the inventory for the review: every part block, by kind, with where it is
        d = m.walk(SPAWN['dawn']); feet = {(x, z): y for (x, y, z) in d}
        rows = {}
        for (x, y, z), v in m.extra.items():
            n = v.split('[')[0]
            if not (cls(v) in (2, 3) or n in ('rail', 'ladder', 'dead_bush', 'candle') or n.endswith('banner')): continue
            near = any(abs(feet.get((x + i, z + j), -99) - y) <= 2 for i in (-1, 0, 1) for j in (-1, 0, 1))
            r = rows.setdefault(n, [0, 0, []]); r[0] += 1; r[1] += near
            if near and len(r[2]) < 6: r[2].append((x, y + 135, z))
        L = ['# Ironhold non-full blocks', '', 'Counted from the generator. "Beside a route" = within one block sideways and two up or down of a cell a troop can stand on. Positions are test-world coordinates (map y + 135).', '',
             '| block | total | beside a route | examples |', '|---|---|---|---|']
        for n, (t, k, ex) in sorted(rows.items(), key=lambda kv: -kv[1][1]): L.append('| %s | %d | %d | %s |' % (n, t, k, ' '.join('(%d, %d, %d)' % e for e in ex)))
        open(sys.argv[3], 'w', newline=chr(10)).write(chr(10).join(L) + chr(10)); print(len(rows), 'kinds')
    elif cmd == 'render':
        from mapforge import render
        out, view = sys.argv[3], sys.argv[4]
        box = tuple(int(v) for v in sys.argv[5:9]) if len(sys.argv) >= 9 else (X0, Z0, X1, Z1)
        lo, hi = (box[0], YLO, box[1]), (box[2], YHI, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Ironhold %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'export':
        outdir = sys.argv[3]; os.makedirs(outdir, exist_ok=True)
        for n in sys.argv[4:] or [c + str(r) for r in range(1, 7) for c in 'ABCDEFGHI']:
            b = section_box(n); lo, hi = (b[0], YLO, b[1]), (b[2], YHI, b[3])
            st = m.world(lo, hi, solid=True).export_structure(lo, hi)
            anc = m.anchors(lo, hi); at = {tuple(e[0]): i for i, e in enumerate(st.blocks)}
            for pos, (state, tag, depth) in anc.items():
                rel = (pos[0] - lo[0], pos[1] - lo[1], pos[2] - lo[2]); state = normalize(state) if not state.startswith('minecraft:') else state
                if state not in st.palette: st.palette.append(state)
                st.blocks[at[rel]] = (st.blocks[at[rel]][0], st.palette.index(state), prop_anchor.nbt_tag(tag))
            st.save(os.path.join(outdir, 'ih_%s.nbt' % n.lower())); print(n, b, 'props', len(anc), 'shallow', sum(d == 1 for _, _, d in anc.values()))
    elif cmd == 'shots':
        L = ['# Ironhold: every section placed, a picture of each before the next row hides it.', 'note Ironhold sections', 'mc time set 3000']
        for r in range(1, 7):
            for c in 'ABCDEFGHI':
                b = section_box(c + str(r)); cx = (b[0] + b[2] + 1) // 2
                L += ['mc execute as @a at @s run forceload add %d %d %d %d' % b, 'mc execute as @a at @s run place template ggmaps:ih/sec/ih_%s%d %d %d %d' % (c.lower(), r, b[0], YLO + 135, b[1]),
                      'props %d %d %d %d %d %d' % (b[0], YLO + 135, b[1], b[2], YHI + 135, b[3]), 'wait 10', 'shot sec-%s%d %d %d %d %d %d %d' % (c.lower(), r, cx, 290 if r in (1, 6) else 252, b[3] + 24, cx, 206, b[1] + 26)]
        L += ['props %d %d %d %d %d %d' % (X0, YLO + 135, Z0, X1, YHI + 135, Z1), 'wait 20'] + ['shot %s %s' % (n, ' '.join(str(v) for v in c)) for n, c in VIEWS]
        open(sys.argv[3], 'w', newline='\n').write('\n'.join(L) + '\n'); print(len(L), 'lines')


if __name__ == '__main__':
    import ihbuild
    ihbuild.main()
