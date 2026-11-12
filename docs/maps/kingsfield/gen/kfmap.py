"""Kingsfield "Crown Quarry": the whole map as a function of (x, z), plus the checked pieces pasted from pieces/*.nbt.
Nothing here touches a Minecraft world: it renders pictures and writes one nbt template per 64 x 64 section.

  python kfmap.py render OUT.png top|iso [x0 z0 x1 z1]
  python kfmap.py export OUTDIR [SECTION ...]        sections are named A1..I6; none = all
  python kfmap.py check

Coordinates: x east, z south, ground y 64. The design is mirrored in x and in z (docs/maps/kingsfield/plan.md)."""
import sys, os, math, json, functools
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
from mapforge.world import World, Cell
from mapforge.structure import Structure, parse_state, state_string, normalize
from mapforge.terrain import fbm, seed_of, hash01
from trees import TREES, turned

PIECES = os.path.join(HERE, '..', 'pieces')
X0, X1, Z0, Z1 = -288, 287, -192, 191
YLO, YHI = 54, 92
SEED = seed_of(1066)
POINTS = {'A': (-128, 0, 64), 'B': (0, -114, 67), 'C': (0, 0, 56), 'D': (0, 114, 67), 'E': (128, 0, 64)}


def seg(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az
    t = max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz)))
    return math.hypot(px - ax - t * dx, pz - az - t * dz)


def ell(x, z, rx, rz):
    return math.hypot(x / rx, z / rz)


# roads in the north-west quarter (x <= 0, z <= 0): (points, half width, kind)
ROADS = [([(-288, 0), (0, 0)], 4.5, 'king'),
         ([(-215, 0), (-152, -91), (0, -114)], 3.5, 'road'),
         ([(0, -114), (0, -58)], 2.5, 'road'),
         ([(-95, -100), (-128, -30)], 2.5, 'road')]


def road(qx, qz):
    """(distance beyond the road edge, kind) of the nearest road; <= 0 means on it"""
    best = (99.0, None)
    for pts, half, kind in ROADS:
        for a, b in zip(pts, pts[1:]):
            d = seg(qx, qz, a[0], a[1], b[0], b[1]) - half
            if d < best[0]: best = (d, kind)
    return best


def ramp(ax, az):
    if az <= 7 and 44 <= ax <= 74: return 56 + round(8 * (ax - 44) / 30)
    if ax <= 5 and 31 <= az <= 58: return 56 + round(8 * (az - 31) / 27)
    return None


@functools.lru_cache(maxsize=None)
def knoll(ax, az):
    """extra height of the B / D knoll: flat top 22 around the line z 92..112, then an uneven slope of about 1 in 4"""
    d = seg(ax, az, 0, 92, 0, 112)
    if d <= 22: return 3
    d = max(22.01, d + 2.4 * fbm(SEED, ax + 300, az + 300, 9))
    return max(0, 3 - math.ceil((d - 22) / 4))


@functools.lru_cache(maxsize=None)
def bank(ax, az):
    """the edge bank: its foot wanders in and out instead of running dead straight"""
    t = max(ax - 258, az - 178)
    if t <= -3: return 0
    t += 2.6 * fbm(SEED, ax + 77, az + 33, 13)
    return 0 if t <= 0 else min(10, round(t * 10 / 12))


PONDS = [(106, 50, 5.0, 3.5), (40, 76, 4.5, 3.0), (62, 44, 4.0, 3.0)]     # (x, z, rx, rz): duck pond, mill pond, flooded digging


def pond(ax, az):
    for cx, cz, rx, rz in PONDS:
        if abs(ax - cx) < rx + 4 and abs(az - cz) < rz + 4:
            e = ell(ax - cx, az - cz, rx, rz) + 0.22 * fbm(SEED, ax * 3, az * 3, 5)
            if e < 1.6: return e
    return None


PLOTS = [(160, 101, 6, 6), (142, 75, 11, 8), (54, 131, 7, 8), (107, 137, 8, 6), (89, 109, 3, 3)]   # small buildings stand on level ground


def strata(x, y, z):
    """the banded rock of the quarry faces"""
    r = hash01(SEED, x + 31 * y, z, 11)
    if r < 0.12: return 'cobblestone'
    if r < 0.2: return 'stone'
    return {63: 'stone', 62: 'diorite', 61: 'stone', 60: 'stone', 59: 'tuff', 58: 'stone', 57: 'andesite'}.get(y, 'stone')


def in_box(ax, az, cx, cz, hx, hz, pad=0):
    return abs(ax - cx) <= hx + pad and abs(az - cz) <= hz + pad


def reserved(ax, az):
    """ground that stays dead level: bases, granges, point rings, hold plots, the pit and its rim"""
    return (in_box(ax, az, 215.5, 0, 36, 40, 6) or in_box(ax, az, 128, 0, 28, 28, 6) or ell(ax, az, 80, 64) < 1
            or in_box(ax, az, 70.5, 96.5, 12, 12, 4) or knoll(ax, az) > 0 or az < 46 or pond(ax, az) is not None
            or any(in_box(ax, az, *p) for p in PLOTS))


def height(x, z):
    ax, az = abs(x), abs(z)
    r = ramp(ax, az)
    if r is not None: return r
    if ell(ax, az, 66, 50) < 1:
        if ell(ax, az, 44, 31) <= 1: return 56
        return 58 if ell(ax, az, 52, 37.5) <= 1 else 60
    h = 64 + knoll(ax, az) + bank(ax, az)
    if bank(ax, az) == 0 and not reserved(ax, az) and road(-ax, -az)[0] > 5:
        n = fbm(SEED, x, z, 38)
        h += 1 if n > 0.2 else (-1 if n < -0.24 else 0)
    return h


CROPS = ['wheat', 'stooks', 'carrots', 'young', 'beetroots', 'fallow', 'potatoes']
WALLS = [(-170, 50, 176, 'hedge', (118, 130)), (-124, 40, 176, 'wall', (138, 150)), (-84, 44, 176, 'hedge', (150, 162)),
         (-38, 140, 176, 'wall', (153, 165))]          # (x, from |z|, to |z|, kind, gap |z| range)
STRIPS = [(176, 170, 'grass'), (170, 124, 'stubble'), (124, 84, 'grass'), (84, 38, 'stubble'), (38, 0, 'grass')]


def wall_at(ax, az):
    for wx, z0, z1, kind, gap in WALLS:
        if ax == -wx and z0 <= az <= z1 and not gap[0] <= az <= gap[1] and road(-ax, -az)[0] > 4 and knoll(ax, az) == 0:
            if not in_box(ax, az, 70.5, 96.5, 12, 12, 3): return kind
    return None


def wood(ax, az):
    return ax > 170 and az > 120 and bank(ax, az) == 0 and road(-ax, -az)[0] > 10


def column(x, z):
    """(height, surface block, under block, [blocks standing on top])"""
    ax, az = abs(x), abs(z)
    h = height(x, z)
    r1, r2, r3 = hash01(SEED, x, z, 1), hash01(SEED, x, z, 2), hash01(SEED, x, z, 3)
    rock = 'stone' if r1 < 0.55 else ('andesite' if r1 < 0.8 else ('cobblestone' if r1 < 0.92 else 'tuff'))
    top = []
    if ramp(ax, az) is not None:
        return h, ('gravel' if r1 < 0.35 else ('coarse_dirt' if r1 < 0.75 else 'cobblestone')), 'stone', top
    e = ell(ax, az, 66, 50)
    if e < 1:
        if h == 56:                                                # the floor: cart-worn earth in drifts, white chips round the Hand
            n = fbm(SEED, x + 60, z - 40, 9)
            if ell(ax, az, 27, 29) < 1 and r2 < 0.16: return h, ('calcite' if r3 < 0.6 else 'diorite'), 'stone', top
            if n > 0.22: return h, ('coarse_dirt' if r1 < 0.55 else ('packed_mud' if r1 < 0.8 else 'gravel')), 'stone', top
            if n < -0.3: return h, ('tuff' if r1 < 0.5 else ('cobbled_deepslate' if r1 < 0.7 else 'stone')), 'stone', top
            return h, ('stone' if r1 < 0.5 else ('gravel' if r1 < 0.8 else 'andesite')), 'stone', top
        if r2 < 0.05: return h, 'moss_block', 'strata', (['grass'] if r3 < 0.5 else [])       # green creeping back on the benches
        if r2 < 0.09: return h, 'mossy_cobblestone', 'strata', top
        return h, rock, 'strata', top
    p = pond(ax, az)
    if p is not None and p < 1:
        return h, 'water', 'gravel', (['lily_pad'] if r2 < 0.1 and p < 0.8 else [])
    if p is not None and p < 1.45:
        s = 'gravel' if r1 < 0.3 else ('coarse_dirt' if r1 < 0.6 else 'grass_block')
        return h, s, 'dirt', ([('fern' if r3 < 0.4 else 'grass')] if r2 < 0.45 and s != 'gravel' else [])
    if ell(ax, az, 76, 60) < 1:                                    # the bare rim
        if r2 < 0.05 and e > 1.12: return h, 'grass_block', 'strata', ['grass']
        return h, ('gravel' if r1 < 0.45 else ('stone' if r1 < 0.7 else ('coarse_dirt' if r1 < 0.9 else 'andesite'))), 'strata', top
    if knoll(ax, az) == 3:                                         # B and D: the ring let into the ground, a compass in the middle
        dd = math.hypot(ax, az - 114)
        if 15.0 <= dd < 17.0: return h, ('smooth_stone' if dd < 16 else ('polished_andesite' if (ax + az) % 5 else 'chiseled_stone_bricks')), 'dirt', top
        if dd < 3.3 or (dd < 6.5 and (ax < 1 or abs(az - 114) < 1)): return h, ('chiseled_stone_bricks' if dd < 1 else ('smooth_stone' if dd >= 3.3 else 'polished_andesite')), 'dirt', top
    d, kind = road(-ax, -az)
    b = bank(ax, az)
    if d <= 0 and b < 3:
        if kind == 'king': s = 'dirt_path' if d <= -2 else ('cobblestone' if r1 < 0.45 else ('gravel' if r1 < 0.75 else ('andesite' if r1 < 0.88 else 'coarse_dirt')))
        else: s = 'dirt_path'
        return h, s, 'dirt', top
    if d <= 1.2 and b == 0 and r1 < 0.6: return h, 'coarse_dirt', 'dirt', top
    if 0 < d <= 3.2 and b == 0 and r1 > 0.8: return h, ('gravel' if r3 < 0.35 else 'coarse_dirt'), 'dirt', top     # a ragged verge
    if b > 0:
        if fbm(SEED, ax + 40, az + 90, 8) > 0.28:                   # rock breaking through the bank
            s = 'stone' if r1 < 0.45 else ('mossy_cobblestone' if r1 < 0.75 else 'andesite')
            if r2 < 0.14 and b >= 2: top = ['mossy_cobblestone' if r3 < 0.5 else 'cobblestone']
            return h, s, 'stone', top
        s = 'grass_block' if r1 < 0.7 else ('podzol' if r1 < 0.85 else 'coarse_dirt')
        if r2 > 0.93: top = ['oak_leaves[persistent=true]']         # undergrowth
        elif r2 > 0.68: top = ['fern' if r3 < 0.5 else 'grass']
        return h, s, 'dirt', top
    w = wall_at(ax, az)
    if w == 'wall': return h, 'coarse_dirt', 'dirt', ['mossy_cobblestone' if r1 < 0.3 else 'cobblestone', 'cobblestone_wall' if r2 < 0.85 else 'mossy_cobblestone_wall']
    if w == 'hedge': return h, 'coarse_dirt', 'dirt', ['oak_leaves[persistent=true]', 'oak_leaves[persistent=true]'] + (['oak_leaves[persistent=true]'] if r2 < 0.2 else [])
    if wood(ax, az):
        s = 'grass_block' if r1 < 0.6 else ('podzol' if r1 < 0.85 else 'coarse_dirt')
        if r2 > 0.955: top = ['oak_leaves[persistent=true]']         # bushes under the trees
        elif r2 > 0.7: top = ['fern' if r3 < 0.55 else 'grass']
        elif r2 < 0.006: top = ['brown_mushroom' if r3 < 0.6 else 'red_mushroom']
        return h, s, 'dirt', top
    strip = next((k for a, c, k in STRIPS if c <= ax < a), 'grass')
    if strip == 'stubble' and az > 46 and az < 176 and not reserved(ax, az) and d > 3:
        lo, hi = (124, 170) if ax >= 124 else (38, 84)
        sid = 0 if ax >= 124 else 1
        along, off = (az - 47) // 24, (az - 47) % 24
        if off < 2 or ax - lo < 2 or hi - 1 - ax < 2:                # grass baulks between the plots, a headland by the hedge
            s = 'grass_block' if r1 < 0.8 else 'coarse_dirt'
            if s == 'grass_block': top = ['grass'] if r2 < 0.3 else (['dandelion' if r3 < 0.5 else 'oxeye_daisy'] if r2 > 0.95 else [])
            return h, s, 'dirt', top
        crop = CROPS[(along * 3 + sid * 4 + 1) % len(CROPS)]
        rows = ax if sid else az                                    # the rows turn from strip to strip
        if off == 7 and ax == (lo + hi) // 2 and crop not in ('stooks', 'fallow'):
            return h, 'coarse_dirt', 'dirt', ['oak_fence', 'hay_block[axis=x]', 'carved_pumpkin[facing=south]']   # a scarecrow
        if crop == 'stooks':                                        # cut and stooked
            s = 'rooted_dirt' if rows % 4 == 0 else ('coarse_dirt' if r1 < 0.9 else 'grass_block')
            if ax % 6 == 4 and az % 5 == 3 and r2 < 0.8: top = ['hay_block[axis=y]'] * (2 if r3 < 0.3 else 1)
            elif r2 > 0.985: top = ['dead_bush']
            return h, s, 'dirt', top
        if crop == 'fallow':
            s = 'grass_block' if r1 < 0.55 else ('coarse_dirt' if r1 < 0.85 else 'rooted_dirt')
            if s == 'grass_block': top = ['grass'] if r2 < 0.45 else (['poppy' if r3 < 0.5 else 'cornflower'] if r2 > 0.93 else [])
            return h, s, 'dirt', top
        furrow = rows % (6 if crop in ('wheat', 'young') else 3) == 0
        if furrow: return h, ('coarse_dirt' if r1 < 0.8 else 'rooted_dirt'), 'dirt', top
        plant = {'wheat': 'wheat[age=%d]' % (7 if r2 > 0.12 else 6), 'young': 'wheat[age=%d]' % (2 if r2 < 0.5 else 3),
                 'carrots': 'carrots[age=%d]' % (7 if r2 > 0.2 else 4), 'potatoes': 'potatoes[age=%d]' % (7 if r2 > 0.2 else 4),
                 'beetroots': 'beetroots[age=%d]' % (3 if r2 > 0.2 else 2)}[crop]
        return h, 'farmland[moisture=7]', 'dirt', [plant]
    n = fbm(SEED, x + 500, z - 300, 17)
    s = 'grass_block' if n < 0.3 else ('coarse_dirt' if r1 < 0.45 else ('grass_block' if r1 < 0.8 else 'rooted_dirt'))
    if s == 'grass_block' and not reserved(ax, az) and d > 2:
        f = fbm(SEED, x - 900, z + 700, 11)                         # flowers come in drifts
        if f > 0.3 and r2 < 0.3: top = [('oxeye_daisy', 'cornflower', 'poppy', 'dandelion', 'azure_bluet')[int(r3 * 5)]]
        elif f < -0.3 and r2 < 0.2: top = ['fern']
        elif r2 < 0.16: top = ['grass']
    elif s == 'grass_block' and any(in_box(ax, az, *p) for p in PLOTS) and r2 < 0.1: top = ['grass']
    return h, s, 'dirt', top


def tree(x, z, r, tall=False):
    """a marker: the trunk; leaves are added by trees()"""
    return ['@tree%d%s' % (4 + int(r * 3) + (2 if tall else 0), 's' if (tall and r < 0.5) else 'o')]


def mirror_state(state, mx, mz):
    name, props = parse_state(state)
    props = dict(props)
    for flag, a, b in ((mx, 'east', 'west'), (mz, 'north', 'south')):
        if not flag: continue
        if props.get('facing') in (a, b): props['facing'] = b if props['facing'] == a else a
        if a in props or b in props:
            va, vb = props.get(a), props.get(b)
            if vb is not None: props[a] = vb
            elif a in props: del props[a]
            if va is not None: props[b] = va
            elif b in props and vb is None: del props[b]
    if mx != mz and 'shape' in props and ('left' in props['shape'] or 'right' in props['shape']):
        props['shape'] = props['shape'].replace('left', '#').replace('right', 'left').replace('#', 'right')
    return state_string(name, props)


_cache = {}


def piece(name):
    if name not in _cache: _cache[name] = Structure.load(os.path.join(PIECES, name + '.nbt'))
    return _cache[name]


# (piece, x, y, z of the nbt corner in the north-west design, mirror x, mirror z). x/z are the lowest world corner
# AFTER mirroring is worked out by place(): give the unmirrored corner; mirrored copies are listed explicitly below.
def placements():
    out = []

    def both(name, x, y, z, dusk=None):
        """the piece on the west side and its mirror in x on the east side"""
        out.append((name, x, y, z, False, False)); out.append((dusk or name, x, y, z, True, False))

    def four(name, x, y, z):
        for mx in (False, True):
            for mz in (False, True): out.append((name, x, y, z, mx, mz))
    both('k1_base', -251, 64, -40, dusk='k1_base_dusk')
    both('k2_grange', -157, 64, -29)
    out.append(('k3_barn', -18, 67, -96, False, False))
    out.append(('k4_windmill', -1, 67, 73, False, False))
    out.append(('k5_hand', -22, 56, -24, False, False))
    out.append(('k6_crane', 10, 64, -63, False, False))
    out.append(('s9_hold', -83, 64, -108, False, False)); out.append(('s9_hold', -83, 64, -108, True, True))
    out.append(('s2_lodge', -93, 64, -25, False, False)); out.append(('s2_lodge', -93, 64, -25, True, True))
    def diag(a, b, x, y, z, bx=None, bz=None):
        """a on the north-west and south-east quarters, its variant b on the other two: the map stays the same from both ends"""
        for mx, mz in ((False, False), (True, True)): out.append((a, x, y, z, mx, mz))
        for mx, mz in ((True, False), (False, True)): out.append((b, x if bx is None else bx, y, z if bz is None else bz, mx, mz))
    diag('s1_beacon', 's1_bell', -164, 64, -105, -165, -106)
    diag('s4a_cottage', 's4a_tile', -147, 64, -79)
    diag('s4b_cottage', 's4b_board', -59, 64, -137)
    diag('s5_haybarn', 's5_cart', -113, 64, -141)
    diag('s7_cross', 's7_shrine', -90, 64, -110)
    for mx, mz in ((True, False), (False, True)):                 # the two plots without a hold: a small farmstead
        out.append(('s4a_stone', -80, 64, -105, mx, mz)); out.append(('s5_cart', -76, 64, -94, mx, mz))
    return out


def place(cells, name, x, y, z, mx, mz):
    s = piece(name)
    sx, sy, sz = s.size
    for (lx, ly, lz), state, be in s.cells():
        if state == 'minecraft:air' or (ly == 0 and state == 'minecraft:grass_block'): continue   # air, and the workshop pad
        wx = -(x + lx) if mx else x + lx
        wz = -(z + lz) if mz else z + lz
        cells[(wx, y + ly, wz)] = mirror_state(state, mx, mz) if (mx or mz) else state


class Map:
    def __init__(self, box=None):
        self.x0, self.z0, self.x1, self.z1 = box or (X0, Z0, X1, Z1)
        self.col, self.extra = {}, {}
        pad = 8
        for x in range(self.x0 - pad, self.x1 + pad + 1):
            for z in range(self.z0 - pad, self.z1 + pad + 1):
                self.col[(x, z)] = column(x, z)
        self.steps()
        for (x, z), (h, s, u, top) in self.col.items():
            for i, b in enumerate(top):
                if b.startswith('@tree'): continue
                else: self.extra[(x, h + 1 + i, z)] = b
        self.tufts()
        self.mill_yard()
        self.forest()
        self.dress()
        self.works()
        for p in placements(): place(self.extra, *p)

    def steps(self):
        """a slab on every road or ramp cell that has a road cell one higher beside it, so nothing has to jump"""
        def way(x, z):
            ax, az = abs(x), abs(z)
            if ramp(ax, az) is not None: return 'stone'
            if ell(ax, az, 76, 60) < 1 or bank(ax, az) >= 3: return None
            d, kind = road(-ax, -az)
            return ('stone' if kind == 'king' and d > -2 else 'dirt') if d <= 0 else None
        for (x, z), (h, s, u, top) in list(self.col.items()):
            k = way(x, z)
            if not k or top: continue
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = self.col.get((x + dx, z + dz))
                if n and n[0] == h + 1 and way(x + dx, z + dz):
                    self.col[(x, z)] = (h, 'cobblestone' if k == 'stone' else 'coarse_dirt', u, top)
                    self.extra[(x, h + 1, z)] = 'cobblestone_slab[type=bottom]' if k == 'stone' else 'spruce_slab[type=bottom]'
                    break

    def put(self, x, y, z, b):
        if (x, z) in self.col: self.extra[(x, y, z)] = b

    def works(self):
        """S3 quarry works on the bench, S8 wells, the orchards behind the barn and the mill; all mirrored four ways"""
        for sx in (1, -1):
            for sz in (1, -1):
                ms = lambda st: mirror_state(st, sx < 0, sz < 0)
                # timber staging against the rim face, north and south of the pit (bench y 60, rim y 64): log posts, braces, a ladder
                for x in range(18, 27):
                    for z in (45, 46, 47): self.put(sx * x, 63, sz * z, 'spruce_slab[type=top]')
                for x in (18, 22, 26):
                    for z in (45, 47):
                        for y in (61, 62, 63): self.put(sx * x, y, sz * z, 'oak_log[axis=y]')
                    self.put(sx * x, 63, sz * 46, 'stripped_oak_log[axis=z]')
                for z in (45, 47):
                    for x in (19, 23): self.put(sx * x, 62, sz * z, ms('spruce_stairs[facing=west,half=top,shape=straight]'))
                    for x in (21, 25): self.put(sx * x, 62, sz * z, ms('spruce_stairs[facing=east,half=top,shape=straight]'))
                for y in (61, 62, 63): self.put(sx * 22, y, sz * 44, ms('ladder[facing=north]'))
                self.put(sx * 25, 61, sz * 46, 'barrel[facing=up]'); self.put(sx * 24, 61, sz * 46, 'smooth_stone')
                # a tram line across the pit floor to the spoil, on sleepers
                for x in range(24, 39): self.put(sx * x, 57, sz * 10, 'rail[shape=east_west]')
                self.put(sx * 39, 57, sz * 10, 'cobblestone'); self.put(sx * 39, 58, sz * 10, 'oak_log[axis=z]')
                self.put(sx * 23, 57, sz * 10, 'cobblestone'); self.put(sx * 23, 58, sz * 10, 'oak_log[axis=z]')
                for x, z, n in ((24, 12, 2), (25, 12, 1), (24, 13, 1)):    # cut blocks waiting by the line
                    for i in range(n): self.put(sx * x, 57 + i, sz * z, 'smooth_stone' if (x + i) % 2 else 'stone_bricks')
                # the point standards of B and D, outside the ring
                for k in (-1, 1):
                    zc = 114 + 13 * k
                    for y in range(68, 78): self.put(sx * 13, y, sz * zc, 'stripped_spruce_log[axis=y]')
                    self.put(sx * 13, 78, sz * zc, 'lantern[hanging=false]')
                    for y in range(74, 78):
                        for j in (1, 2, 3): self.put(sx * 13, y, sz * (zc + j * k), 'white_wool')
                    for y in (75, 76): self.put(sx * 13, y, sz * (zc + 4 * k), 'white_wool')
                # cut stone waiting on the bench
                for x, z, n in ((40, 38, 2), (41, 38, 2), (40, 39, 1), (41, 39, 2), (48, 30, 1), (49, 30, 2), (49, 31, 1), (60, 8, 2), (60, 9, 1)):
                    for i in range(n): self.put(sx * x, 61 + i, sz * z, ('stone_bricks', 'smooth_stone', 'chiseled_stone_bricks')[(x + z + i) % 3])
                for x, z, n in ((32, 40, 2), (33, 40, 1), (32, 41, 1), (56, 20, 2), (56, 21, 2), (57, 20, 1), (12, 44, 1), (13, 44, 2)):
                    for i in range(n): self.put(sx * x, 61 + i, sz * z, 'stone_bricks' if (x + i) % 2 else 'smooth_stone')
                # spoil heaps on the pit floor, low, clear of the Hand, the ramps and the middle
                for cx, cz, r in ((30, 15, 3.2), (17, 24, 2.4), (37, 6, 0)):
                    for x in range(cx - 4, cx + 5):
                        for z in range(cz - 4, cz + 5):
                            d = math.hypot(x - cx, z - cz)
                            if d > r or ell(x, z, 44, 31) > 0.97: continue
                            hh = hash01(SEED, sx * x, sz * z, 7)
                            for i in range(2 if d < r - 1.8 else 1):
                                self.put(sx * x, 57 + i, sz * z, 'gravel' if hh < 0.4 else ('cobblestone' if hh < 0.75 else 'andesite'))
                # orchard: rows of small trees behind the knoll
                for x in range(10, 36, 6):
                    for z in range(138, 170, 7):
                        j = hash01(SEED, x, z, 51)                 # not on a ruler: rows stagger, a tree missing here and there
                        if j > 0.12: self.plant('apple', x + (3 if (z // 7) % 2 else 0) + int(j * 3) - 1, z + int(j * 7) % 3 - 1, sx, sz, (x + z) % 4)
                # well beside the first cottage
                wx, wz = sx * 133, sz * 76
                if (wx, wz) in self.col:
                    for dx in (-1, 0, 1):
                        for dz in (-1, 0, 1):
                            self.put(wx + dx, 65, wz + dz, 'water' if dx == dz == 0 else ('mossy_cobblestone' if (dx + dz) % 2 else 'cobblestone'))
                    for dx in (-1, 1):
                        for y in (66, 67): self.put(wx + dx, y, wz, 'oak_fence')
                    for dx in (-1, 0, 1): self.put(wx + dx, 68, wz, 'spruce_slab[type=bottom]')

    def tree(self, x, h, z, n, kind):
        log, leaf = ('spruce_log[axis=y]', 'spruce_leaves[persistent=true]') if kind == 's' else ('oak_log[axis=y]', 'oak_leaves[persistent=true]')
        for i in range(1, n + 1): self.extra[(x, h + i, z)] = log
        for dy, r in ((n - 2, 2), (n - 1, 2), (n, 1), (n + 1, 1)) if kind == 'o' else ((n - 3, 2), (n - 2, 1), (n - 1, 2), (n, 1), (n + 1, 0)):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) == r and abs(dz) == r and r > 1: continue
                    self.extra.setdefault((x + dx, h + dy, z + dz), leaf)

    def plant(self, name, ax, az, sx, sz, k=0):
        """one of the nine trees with its trunk at (sx*ax, sz*az), mirrored like everything else"""
        x0, z0 = sx * ax, sz * az
        if (x0, z0) not in self.col: return
        h = self.col[(x0, z0)][0]
        for (dx, dy, dz), b in turned(TREES[name], k).items():
            pos = (x0 + sx * dx, h + dy, z0 + sz * dz)
            if 'leaves' in b: self.extra.setdefault(pos, b)
            else: self.extra[pos] = b

    def forest(self):
        """the corner woods, the tree line on the edge bank and a few lone trees; the same in all four quarters"""
        WOOD = ['oak_medium', 'oak_great', 'birch_tall', 'dark_oak', 'oak_small', 'birch_pair', 'oak_medium', 'spruce_small']
        BANK = ['spruce_tall', 'spruce_small', 'oak_medium', 'birch_tall', 'spruce_tall', 'oak_small']
        spots = []
        for gx in range(174, 258, 7):
            for gz in range(124, 178, 7):
                ax, az = gx + int(hash01(SEED, gx, gz, 21) * 5) - 2, gz + int(hash01(SEED, gx, gz, 22) * 5) - 2
                if hash01(SEED, gx, gz, 23) < 0.78 and wood(ax, az) and not wall_at(ax, az):
                    spots.append((WOOD[int(hash01(SEED, gx, gz, 24) * len(WOOD))], ax, az, int(hash01(SEED, gx, gz, 25) * 4)))
        for gx in range(0, 290, 6):
            for gz in range(0, 194, 6):
                ax, az = gx + int(hash01(SEED, gx, gz, 31) * 4) - 1, gz + int(hash01(SEED, gx, gz, 32) * 4) - 1
                if bank(ax, az) >= 4 and ax <= 283 and az <= 187 and hash01(SEED, gx, gz, 33) < (0.95 if fbm(SEED, ax + 9, az + 5, 30) > -0.05 else 0.22):   # in clumps, with gaps
                    spots.append((BANK[int(hash01(SEED, gx, gz, 34) * len(BANK))], ax, az, int(hash01(SEED, gx, gz, 35) * 4)))
        spots += [('oak_great', 190, 70, 0), ('oak_great', 60, 150, 1), ('birch_pair', 140, 112, 2), ('oak_small', 196, 62, 3)]
        for name, ax, az, k in spots:
            for sx in (1, -1):
                for sz in (1, -1): self.plant(name, ax, az, sx, sz, k)

    def tufts(self):
        """long grass and fern at the foot of every one-block step in the grass, so the raw dirt face is half hidden"""
        for (x, z), (h, s, u, top) in self.col.items():
            if s != 'grass_block' or (x, h + 1, z) in self.extra: continue
            if any(self.col.get((x + dx, z + dz), (h,))[0] == h + 1 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                r = hash01(SEED, x, z, 61)
                if r < 0.7: self.extra[(x, h + 1, z)] = 'fern' if r < 0.15 else 'grass'

    def dress(self):
        """boulders in the meadows and on the bank foot, fallen logs in the woods; the same in all four quarters"""
        for gx in range(6, 262, 19):
            for gz in range(50, 180, 19):
                ax, az = gx + int(hash01(SEED, gx, gz, 41) * 13), gz + int(hash01(SEED, gx, gz, 42) * 13)
                if hash01(SEED, gx, gz, 43) > 0.6: continue
                if any(reserved(ax + i, az + j) or road(-(ax + i), -(az + j))[0] < 5 or wall_at(ax + i, az + j) for i in (-3, 0, 3) for j in (-3, 0, 3)): continue
                if ell(ax, az, 84, 68) < 1 or bank(ax, az) > 2: continue
                in_field = (124 <= ax < 170 or 38 <= ax < 84) and not wood(ax, az)
                if in_field: continue
                k = hash01(SEED, gx, gz, 44)
                for sx in (1, -1):
                    for sz in (1, -1):
                        if wood(ax, az) and k < 0.6:                 # a fallen trunk
                            n, along_x = 4 + int(k * 5), k < 0.3
                            for i in range(n):
                                x, z = (sx * (ax + i), sz * az) if along_x else (sx * ax, sz * (az + i))
                                if (x, z) in self.col: self.extra[(x, self.col[(x, z)][0] + 1, z)] = 'oak_log[axis=%s]' % ('x' if along_x else 'z')
                        else:                                        # a boulder
                            r = 1.2 + k * 1.3
                            for i in range(-2, 3):
                                for j in range(-2, 3):
                                    dd = math.hypot(i, j * 1.2)
                                    if dd > r: continue
                                    x, z = sx * (ax + i), sz * (az + j)
                                    if (x, z) not in self.col: continue
                                    hh = hash01(SEED, ax + i, az + j, 45)
                                    for y in range(2 if dd < r - 1.3 else 1):
                                        self.extra[(x, self.col[(x, z)][0] + 1 + y, z)] = 'andesite' if hh < 0.4 else ('mossy_cobblestone' if hh < 0.7 else 'stone')

    def mill_yard(self):
        """the windmill's yard wall, the mirror of the barn's cover: 34 x 22, 3 high, gates north and south on the road"""
        for x in range(-17, 18):
            for z in range(71, 97):
                if not (x in (-17, 17) or z in (71, 96)) or (abs(x) <= 3 and z in (71, 96)) or (x, z) not in self.col: continue
                r = hash01(SEED, x, z, 5)
                for y in range(self.col[(x, z)][0] + 1, 70): self.extra[(x, y, z)] = 'mossy_cobblestone' if r < 0.25 else 'cobblestone'
                self.extra[(x, 70, z)] = 'cobblestone_wall' if r < 0.8 else 'cobblestone'

    def cells(self, lo, hi, solid=True):
        """every block of the box as {pos: state}; solid fills each column down to lo.y"""
        out = {}
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h, s, u, _ = self.col[(x, z)]
                low = lo[1] if solid else min([self.col.get((x + dx, z + dz), (h,))[0] for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h]) - 1
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    out[(x, y, z)] = s if y == h else (strata(x, y, z) if u == 'strata' else (u if y > h - 4 else 'stone'))
        for (x, y, z), b in self.extra.items():
            if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]: out[(x, y, z)] = b
        return out

    def world(self, lo, hi, solid=False):
        w = World('kingsfield')
        for pos, b in self.cells(lo, hi, solid).items(): w.set(pos, Cell(normalize(b)))
        return w


def section_box(name):
    c, r = 'ABCDEFGHI'.index(name[0]), int(name[1]) - 1
    return (X0 + 64 * c, Z0 + 64 * r, X0 + 64 * c + 63, Z0 + 64 * r + 63)


def check(m):
    bad = []
    for name, (px, pz, py) in POINTS.items():
        n = [k for k in m.extra if k[1] > py and math.hypot(k[0] - px, k[2] - pz) < 17 and not (name == 'C')]
        hs = {m.col[(x, z)][0] for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17}
        print('point', name, 'blocks standing in the ring:', len(n), 'ground heights:', sorted(hs))
    return bad


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'render':
        from mapforge import render
        out, view = sys.argv[2], sys.argv[3]
        box = tuple(int(v) for v in sys.argv[4:8]) if len(sys.argv) >= 8 else (X0, Z0, X1, Z1)
        m = Map(box)
        lo, hi = (box[0], YLO, box[1]), (box[2], YHI + 8, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Kingsfield %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'export':
        outdir = sys.argv[2]; os.makedirs(outdir, exist_ok=True)
        names = sys.argv[3:] or [c + str(r) for r in range(1, 7) for c in 'ABCDEFGHI']
        for n in names:
            b = section_box(n)
            m = Map(b)
            lo, hi = (b[0], YLO, b[1]), (b[2], YHI, b[3])
            m.world(lo, hi, solid=True).export_structure(lo, hi).save(os.path.join(outdir, 'kf_%s.nbt' % n.lower()))
            print(n, b)
    elif cmd == 'check':
        check(Map())
