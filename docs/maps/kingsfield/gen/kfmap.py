"""Kingsfield "Crown Quarry": the whole map as a function of (x, z), plus the checked pieces pasted from pieces/*.nbt.
Nothing here touches a Minecraft world: it renders pictures and writes one nbt template per 64 x 64 section.

  python kfmap.py render OUT.png top|iso [x0 z0 x1 z1]
  python kfmap.py export OUTDIR [SECTION ...]        sections are named A1..I6; none = all
  python kfmap.py check

Coordinates: x east, z south, ground y 64. The design is mirrored in x and in z (docs/maps/kingsfield/plan.md)."""
import sys, os, math, json
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


def knoll(ax, az):
    """extra height of the B / D knoll: flat top 22 around the line z 92..112, slope 1 in 4"""
    d = seg(ax, az, 0, 92, 0, 112)
    return 3 if d <= 22 else max(0, 3 - math.ceil((d - 22) / 4))


def bank(ax, az):
    t = max(ax - 258, az - 178)
    return 0 if t <= 0 else min(10, round(t * 10 / 12))


def in_box(ax, az, cx, cz, hx, hz, pad=0):
    return abs(ax - cx) <= hx + pad and abs(az - cz) <= hz + pad


def reserved(ax, az):
    """ground that stays dead level: bases, granges, point rings, hold plots, the pit and its rim"""
    return (in_box(ax, az, 215.5, 0, 36, 40, 6) or in_box(ax, az, 128, 0, 28, 28, 6) or ell(ax, az, 80, 64) < 1
            or in_box(ax, az, 70.5, 96.5, 12, 12, 4) or knoll(ax, az) > 0 or az < 46)


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
        if h == 56: return h, ('stone' if r1 < 0.5 else ('gravel' if r1 < 0.8 else 'andesite')), 'stone', top
        return h, rock, 'stone', top
    if ell(ax, az, 76, 60) < 1:                                    # the bare rim
        return h, ('gravel' if r1 < 0.45 else ('stone' if r1 < 0.7 else ('coarse_dirt' if r1 < 0.9 else 'andesite'))), 'stone', top
    d, kind = road(-ax, -az)
    b = bank(ax, az)
    if d <= 0 and b < 3:
        if kind == 'king': s = 'dirt_path' if d <= -2 else ('cobblestone' if r1 < 0.45 else ('gravel' if r1 < 0.75 else ('andesite' if r1 < 0.88 else 'coarse_dirt')))
        else: s = 'dirt_path'
        return h, s, 'dirt', top
    if d <= 1.2 and b == 0 and r1 < 0.6: return h, 'coarse_dirt', 'dirt', top
    if b > 0:
        s = 'grass_block' if r1 < 0.7 else ('podzol' if r1 < 0.85 else 'coarse_dirt')
        if b >= 4 and r2 < 0.085: top = tree(x, z, r3, tall=True)
        elif r2 > 0.9: top = ['fern' if r3 < 0.5 else 'grass']
        return h, s, 'dirt', top
    w = wall_at(ax, az)
    if w == 'wall': return h, 'coarse_dirt', 'dirt', ['mossy_cobblestone' if r1 < 0.3 else 'cobblestone', 'cobblestone_wall' if r2 < 0.85 else 'mossy_cobblestone_wall']
    if w == 'hedge': return h, 'coarse_dirt', 'dirt', ['oak_leaves[persistent=true]', 'oak_leaves[persistent=true]'] + (['oak_leaves[persistent=true]'] if r2 < 0.2 else [])
    if wood(ax, az):
        s = 'grass_block' if r1 < 0.6 else ('podzol' if r1 < 0.85 else 'coarse_dirt')
        if hash01(SEED, x // 1, z // 1, 9) < 0.016: top = tree(x, z, r3)
        elif r2 > 0.85: top = ['fern' if r3 < 0.4 else 'grass']
        return h, s, 'dirt', top
    strip = next((k for a, c, k in STRIPS if c <= ax < a), 'grass')
    if strip == 'stubble' and az > 46 and az < 176 and not reserved(ax, az) and d > 3:
        rows = ax if (ax // 46) % 2 else az                         # the rows turn from strip to strip
        s = 'rooted_dirt' if rows % 4 == 0 else ('coarse_dirt' if r1 < 0.9 else 'grass_block')
        if ax % 9 == 4 and az % 7 == 3 and r2 < 0.75: top = ['hay_block[axis=y]'] * (2 if r3 < 0.3 else 1)
        elif r2 > 0.985: top = ['dead_bush']
        return h, s, 'dirt', top
    n = fbm(SEED, x + 500, z - 300, 17)
    s = 'grass_block' if n < 0.22 else ('coarse_dirt' if r1 < 0.6 else ('podzol' if r1 < 0.8 else 'rooted_dirt'))
    if s == 'grass_block' and not reserved(ax, az) and d > 2:
        if r2 < 0.07: top = ['grass']
        elif r2 > 0.996: top = ['poppy']
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
    four('s1_beacon', -164, 64, -105)
    four('s4a_cottage', -147, 64, -79)
    four('s4b_cottage', -59, 64, -137)
    four('s5_haybarn', -113, 64, -141)
    four('s7_cross', -90, 64, -110)
    for mx, mz in ((True, False), (False, True)):                 # the two plots without a hold: a small farmstead
        out.append(('s4a_cottage', -80, 64, -105, mx, mz)); out.append(('s5_haybarn', -76, 64, -94, mx, mz))
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
        self.mill_yard()
        self.forest()
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
                # timber scaffold against the rim face, north and south of the pit (bench y 60, rim y 64)
                for x in range(18, 27):
                    if x % 4 == 2:
                        for y in (61, 62): self.put(sx * x, y, sz * 47, 'oak_fence'); self.put(sx * x, y, sz * 45, 'oak_fence')
                    for z in (45, 46, 47): self.put(sx * x, 63, sz * z, 'spruce_slab[type=top]')
                # cut stone waiting on the bench
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
                        self.plant('apple', x, z, sx, sz, (x + z) % 4)
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
                if bank(ax, az) >= 4 and hash01(SEED, gx, gz, 33) < 0.72:
                    spots.append((BANK[int(hash01(SEED, gx, gz, 34) * len(BANK))], ax, az, int(hash01(SEED, gx, gz, 35) * 4)))
        spots += [('oak_great', 190, 70, 0), ('oak_great', 60, 150, 1), ('birch_pair', 140, 112, 2), ('oak_small', 196, 62, 3)]
        for name, ax, az, k in spots:
            for sx in (1, -1):
                for sz in (1, -1): self.plant(name, ax, az, sx, sz, k)

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
                    out[(x, y, z)] = s if y == h else (u if y > h - 4 else 'stone')
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
