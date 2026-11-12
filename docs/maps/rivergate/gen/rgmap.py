"""Rivergate "The Eyot": the whole map as a function of (x, z), plus the checked pieces laid in by their generators.
Nothing here touches a Minecraft world: it renders pictures and writes one nbt template per 64 x 64 section.

  python rgmap.py render OUT.png top|iso [x0 z0 x1 z1]
  python rgmap.py export OUTDIR [SECTION ...]        sections are named A1..I6; none = all
  python rgmap.py check

Coordinates: x east, z south, street y 64, water y 62. The map mirrors as x -> -1 - x and z -> -1 - z, so the bridge
(x -8..7) and the river (z -9..8) sit on the mirror lines. Terrain is written in quarter coordinates (ax, az), both
>= 1, counted outward from those lines: the north-west quarter is x = -ax, z = -az. See ../plan.md."""
import sys, os, math, functools
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
sys.path.append(os.path.normpath(os.path.join(HERE, '..', '..', 'kingsfield', 'gen')))
from mapforge.world import World, Cell
from mapforge.structure import normalize
from mapforge.terrain import fbm, seed_of, hash01
from trees import TREES, turned
from kit import Build
import xf, ribs as ribs_mod, bridge, hall, mill, shrine, watergate, houses, toll

X0, X1, Z0, Z1 = -288, 287, -192, 191
YLO, YHI = 56, 93
SEED = seed_of(1187)
POINTS = {'A': (0, -91), 'B': (0, 0), 'C': (0, 91)}
PASS = ('air', 'grass', 'fern', 'tall_grass', 'dandelion', 'poppy', 'oxeye_daisy', 'cornflower', 'azure_bluet', 'allium', 'wheat', 'carrots',
        'potatoes', 'beetroots', 'lily_pad', 'ladder', 'brown_mushroom', 'red_mushroom', 'dead_bush', 'water')


def Q(x, z):
    return (-x if x < 0 else x + 1), (-z if z < 0 else z + 1)


def W(ax, az, sx, sz):
    """world cell of quarter cell (ax, az) in the quarter (sx, sz); (-1, -1) is the north-west"""
    return (-ax if sx < 0 else ax - 1), (-az if sz < 0 else az - 1)


def seg(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az
    t = max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz)))
    return math.hypot(px - ax - t * dx, pz - az - t * dz)


def ell(ax, az, rx, rz):
    return math.hypot((ax - 0.5) / rx, (az - 0.5) / rz)


def isle(ax, az): return ell(ax, az, 50, 30) <= 1
def deck(ax, az): return (ax <= 8 and 31 <= az <= 50) or (100 <= ax <= 113 and az <= 9)
def base(ax, az): return 180 <= ax <= 251 and az <= 40
def hold(ax, az): return 95 <= ax <= 118 and 101 <= az <= 124


def wet(ax, az):
    return ((az <= 9 and ax <= 268) or ell(ax, az, 72, 50) <= 1) and not isle(ax, az)


@functools.lru_cache(maxsize=None)
def bank(ax, az):
    """the edge bank, as on Kingsfield: its foot wanders in and out"""
    t = max(ax - 258, az - 178)
    if t <= -3: return 0
    t += 2.6 * fbm(SEED, ax + 77, az + 33, 13)
    return 0 if t <= 0 else min(10, round(t * 10 / 12))


PATHS = [([(98, 91), (152, 76), (180, 26)], 3.5), ([(113, 15), (178, 24)], 2.0)]      # the bank road and the riverside path
APRONS = [(36, 46, 49, 54), (60, 66, 35, 41), (77, 102, 19, 19), (67, 78, 17, 35)]                       # paving up to house fronts


def path(ax, az):
    return min(seg(ax, az, a[0], a[1], b[0], b[1]) - half for pts, half in PATHS for a, b in zip(pts, pts[1:]))


def pave(ax, az):
    """the kind of made ground at a quarter cell, or None"""
    if isle(ax, az): return 'isle'
    if ax <= 22 and 51 <= az <= 113: return 'plaza'
    if ell(ax, az, 81, 59) <= 1 or (72 <= ax <= 113 and 10 <= az <= 18): return 'quay'
    if any(a <= ax <= b and c <= az <= d for a, b, c, d in APRONS): return 'quay'
    if 23 <= ax <= 98 and 85 <= az <= 98: return 'street'
    if 47 <= ax <= 59 and 40 <= az <= 84: return 'street'
    if 103 <= ax <= 109 and 10 <= az <= 100: return 'ford'
    if (az <= 129 and 127 <= az and ax <= 90) or (85 <= ax <= 86 and 99 <= az <= 129) or (51 <= ax <= 55 and 99 <= az <= 126) or (ax <= 3 and 114 <= az <= 126): return 'lane'
    if path(ax, az) <= 0: return 'path'
    if (23 <= ax <= 46 or 60 <= ax <= 85) and 60 <= az <= 72: return 'yard'
    if 60 <= ax <= 86 and 36 <= az <= 59 and fbm(SEED, ax, az, 9) > -0.12: return 'yard'
    return None


GARDEN_X = (39, 45, 60, 67, 74, 84)


def garden(ax, az):
    """back gardens behind the north row: 'wall', or the strip number"""
    if not (112 <= az <= 126 and (39 <= ax <= 45 or 60 <= ax <= 84)): return None
    if ax in GARDEN_X or az == 126: return None if (az == 126 and ax in (42, 63, 70, 79)) else 'wall'
    return sum(1 for g in GARDEN_X if g < ax)


def plots(ax, az):
    """the field strips between the river meadow and the bank road: 'fence', 'baulk' or a crop"""
    if not (116 <= ax <= 172 and 36 <= az <= 70) or path(ax, az) < 5: return None
    if ax in (116, 172) or az == 36: return None if (ax + az) % 23 < 3 else 'fence'
    k = (ax - 117) // 14
    if (ax - 117) % 14 < 2: return 'baulk'
    return ('wheat', 'carrots', 'fallow', 'beetroots')[k % 4]


def wood(ax, az):
    """the corner woods, and copses on the open ground north of each base"""
    if bank(ax, az) > 0 or path(ax, az) < 10: return False
    return (ax > 170 and az > 120) or (ax > 176 and az > 48 and fbm(SEED, ax, az, 23) > 0.1)


HEDGES = [('z', 131, 4, 170, 'hedge'), ('x', 30, 132, 177, 'hedge'), ('x', 62, 132, 177, 'wall'), ('x', 96, 132, 177, 'hedge'), ('x', 134, 124, 177, 'hedge'),
          ('z', 154, 31, 133, 'wall'), ('x', 174, 50, 119, 'hedge'), ('z', 78, 120, 173, 'hedge') if False else ('z', 121, 119, 133, 'hedge')]


def hedge(ax, az):
    """field boundaries out of town: 'hedge', 'wall' or None; each has gaps to walk through"""
    for axis, at, lo, hi, kind in HEDGES:
        a, t = (az, ax) if axis == 'z' else (ax, az)
        if a == at and lo <= t <= hi and not 11 <= (t + at) % 29 <= 14 and path(ax, az) > 4: return kind
    return None


def blend(x, z, r, table):
    """like mix, but in drifts: neighbouring cells mostly agree, so made ground does not look like noise"""
    return mix(max(0.0, min(0.999, 0.5 + 0.95 * fbm(SEED, x + 211, z - 97, 7) + 0.22 * (r - 0.5))), table)


def mix(r, table):
    t = sum(w for _, w in table) * r
    for b, w in table:
        t -= w
        if t < 0: return b
    return table[-1][0]


STREET = [('cobblestone', 50), ('stone', 20), ('andesite', 18), ('gravel', 7), ('mossy_cobblestone', 5)]
QUAY = [('stone_bricks', 35), ('cobblestone', 30), ('andesite', 15), ('mossy_cobblestone', 12), ('cracked_stone_bricks', 8)]
PLAZA = [('cobblestone', 45), ('stone', 20), ('andesite', 20), ('stone_bricks', 10), ('mossy_cobblestone', 5)]
FORD = [('cobblestone', 40), ('gravel', 30), ('coarse_dirt', 18), ('packed_mud', 12)]
LANE = [('dirt_path', 90), ('coarse_dirt', 10)]
YARD = [('coarse_dirt', 40), ('gravel', 25), ('cobblestone', 20), ('packed_mud', 15)]
MASON = [('stone_bricks', 70), ('cracked_stone_bricks', 14), ('mossy_stone_bricks', 10), ('andesite', 6)]
FLOWERS = ('oxeye_daisy', 'cornflower', 'poppy', 'dandelion', 'azure_bluet', 'allium')


def ring(x, z, r1):
    """the capture ring let into the paving, and a compass at its centre; None off the pattern"""
    for px, pz in POINTS.values():
        dd = math.hypot(x - px, z - pz)
        if 14.0 <= dd < 17.0: return 'smooth_stone' if dd < 16 else ('polished_andesite' if (x + z) % 5 else 'chiseled_stone_bricks')
        if 23.0 <= dd < 24.0 and (px, pz) == (0, 0): return 'polished_andesite' if (x + z) % 7 else 'chiseled_stone_bricks'
        if dd < 3.3 or (dd < 6.5 and (x == px or z == pz)): return 'chiseled_stone_bricks' if dd < 1 else ('smooth_stone' if dd >= 3.3 else 'polished_andesite')
    return None


def column(x, z):
    """(height, surface block, under block, [blocks standing on top])"""
    ax, az = Q(x, z)
    r1, r2, r3 = hash01(SEED, x, z, 1), hash01(SEED, x, z, 2), hash01(SEED, x, z, 3)
    top = []
    if wet(ax, az):
        if 100 <= ax <= 113 and az <= 9 and az not in (1, 5, 6): return 63, mix(r1, QUAY), 'mason', top     # the causeway between its culverts
        e = ell(ax, az, 72, 50)
        lily = az > 9 and 0.9 < e <= 1 and r2 < 0.1 and not deck(ax, az)
        return 62, 'water', 'water', (['lily_pad'] if lily else [])
    if base(ax, az): return 64, 'grass_block', 'dirt', top
    b = bank(ax, az)
    if b > 0:
        if 258 <= ax <= 268 and az <= 9: pass
        if fbm(SEED, ax + 40, az + 90, 8) > 0.28:
            s = 'stone' if r1 < 0.45 else ('mossy_cobblestone' if r1 < 0.75 else 'andesite')
            if r2 < 0.14 and b >= 2: top = ['mossy_cobblestone' if r3 < 0.5 else 'cobblestone']
            return 64 + b, s, 'stone', top
        s = 'grass_block' if r1 < 0.7 else ('podzol' if r1 < 0.85 else 'coarse_dirt')
        if r2 > 0.93: top = ['oak_leaves[persistent=true]']
        elif r2 > 0.68: top = ['fern' if r3 < 0.5 else 'grass']
        return 64 + b, s, 'dirt', top
    k = pave(ax, az)
    edge = any(wet(ax + i, az + j) for i in (-1, 0, 1) for j in (-1, 0, 1))
    under = 'mason' if (edge and ax <= 113) else ('stone' if k in ('isle', 'plaza', 'quay', 'street') else 'dirt')
    if k in ('isle', 'plaza'):
        kerb = 'stone_bricks' if x in (-6, 5) and r1 < 0.85 else None          # the bridge street's kerb lines, A to C
        return 64, ring(x, z, r1) or kerb or mix(r1, PLAZA), under, top
    if k == 'quay': return 64, mix(r1, QUAY), under, top
    if k == 'street': return 64, mix(r1, STREET), under, top
    if k == 'ford': return 64, blend(x, z, r1, FORD), under, top
    if k == 'lane': return 64, mix(r1, LANE), under, top
    if k == 'yard': return 64, blend(x, z, r1, YARD), under, top
    if k == 'path': return 64, ('dirt_path' if path(ax, az) < -0.8 or r1 < 0.5 else 'coarse_dirt'), under, top
    if hold(ax, az): return 64, 'grass_block', 'dirt', (['grass'] if r2 < 0.1 else [])
    g = garden(ax, az)
    if g == 'wall': return 64, 'coarse_dirt', 'dirt', ['mossy_cobblestone' if r1 < 0.3 else 'cobblestone', 'cobblestone_wall' if r2 < 0.85 else 'mossy_cobblestone_wall']
    if g is not None:
        kind = (g * 3 + (1 if az > 0 else 0)) % 4
        if kind == 0:
            if az % 3 == 0: return 64, 'coarse_dirt', 'dirt', top
            return 64, 'farmland[moisture=7]', 'dirt', ['carrots[age=%d]' % (7 if r2 > 0.2 else 4) if ax % 2 else 'wheat[age=%d]' % (7 if r2 > 0.15 else 5)]
        if kind == 2:
            if ax % 3 == 0: return 64, 'coarse_dirt', 'dirt', top
            return 64, 'farmland[moisture=7]', 'dirt', ['beetroots[age=%d]' % (3 if r2 > 0.2 else 2) if az < 119 else 'potatoes[age=%d]' % (7 if r2 > 0.2 else 4)]
        s = 'grass_block' if r1 < 0.85 else 'coarse_dirt'
        if s == 'grass_block': top = [FLOWERS[int(r3 * 6)]] if r2 < (0.3 if kind == 1 else 0.06) else (['grass'] if r2 > 0.7 else [])
        return 64, s, 'dirt', top
    p = plots(ax, az)
    if p == 'fence': return 64, 'coarse_dirt', 'dirt', ['spruce_fence']
    if p == 'baulk' or p == 'fallow':
        s = 'grass_block' if r1 < 0.7 else 'coarse_dirt'
        return 64, s, 'dirt', ((['grass'] if r2 < 0.4 else ([FLOWERS[int(r3 * 4)]] if r2 > 0.93 else [])) if s == 'grass_block' else [])
    if p is not None:
        if az % (6 if p == 'wheat' else 3) == 0: return 64, ('coarse_dirt' if r1 < 0.8 else 'rooted_dirt'), 'dirt', top
        plant = {'wheat': 'wheat[age=%d]' % (7 if r2 > 0.12 else 6), 'carrots': 'carrots[age=%d]' % (7 if r2 > 0.2 else 4), 'beetroots': 'beetroots[age=%d]' % (3 if r2 > 0.2 else 2)}[p]
        return 64, 'farmland[moisture=7]', 'dirt', [plant]
    d = path(ax, az)
    if d <= 1.2 and r1 < 0.6: return 64, 'coarse_dirt', 'dirt', top
    w = hedge(ax, az)
    if w == 'wall': return 64, 'coarse_dirt', 'dirt', ['mossy_cobblestone' if r1 < 0.3 else 'cobblestone', 'cobblestone_wall' if r2 < 0.85 else 'mossy_cobblestone_wall']
    if w == 'hedge': return 64, 'coarse_dirt', 'dirt', ['oak_leaves[persistent=true]', 'oak_leaves[persistent=true]'] + (['oak_leaves[persistent=true]'] if r2 < 0.2 else [])
    if wood(ax, az):
        s = 'grass_block' if r1 < 0.6 else ('podzol' if r1 < 0.85 else 'coarse_dirt')
        if r2 > 0.955: top = ['oak_leaves[persistent=true]']
        elif r2 > 0.7: top = ['fern' if r3 < 0.55 else 'grass']
        elif r2 < 0.006: top = ['brown_mushroom' if r3 < 0.6 else 'red_mushroom']
        return 64, s, 'dirt', top
    if edge:                                                       # the river bank out of town: mud and gravel at the water
        return 64, ('grass_block' if r1 < 0.5 else ('coarse_dirt' if r1 < 0.75 else ('mud' if r1 < 0.9 else 'gravel'))), 'dirt', top
    if az > 131 and ax < 170:                                       # the pastures behind the town, field by field
        f = (sum(1 for g in (30, 62, 96, 134) if g < ax) * 2 + (1 if az > 154 else 0)) % 5
        if f == 1:                                                 # mown and stooked
            s = 'grass_block' if r1 < 0.5 else ('coarse_dirt' if r1 < 0.85 else 'rooted_dirt')
            if ax % 6 == 4 and az % 5 == 3 and r2 < 0.8: top = ['hay_block[axis=y]'] * (2 if r3 < 0.3 else 1)
            return 64, s, 'dirt', top
        if f == 3:                                                 # a flower meadow
            return 64, 'grass_block', 'dirt', ([FLOWERS[int(r3 * 6)]] if r2 < 0.3 else (['grass'] if r2 < 0.55 else []))
        if f == 4:                                                 # grazed hard
            s = 'grass_block' if r1 < 0.75 else ('coarse_dirt' if r1 < 0.9 else 'podzol')
            return 64, s, 'dirt', (['grass'] if r2 < 0.05 and s == 'grass_block' else [])
    town = ax <= 96 and az <= 130
    if ax <= 120 and az <= 132 and r1 < 0.5 and any(pave(ax + i, az + j) in ('street', 'quay', 'plaza', 'ford') for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))):
        return 64, ('gravel' if r2 < 0.4 else ('coarse_dirt' if r2 < 0.75 else 'mossy_cobblestone')), 'dirt', top      # a ragged verge
    meadow = az <= 34 and ax > 113
    n = fbm(SEED, x + 500, z - 300, 17)
    s = 'grass_block' if n < 0.32 or meadow else ('coarse_dirt' if r1 < 0.45 else ('grass_block' if r1 < 0.8 else 'rooted_dirt'))
    if s == 'grass_block' and d > 2:
        f = fbm(SEED, x - 900, z + 700, 11)
        if meadow and r2 < 0.42: top = [FLOWERS[int(r3 * 6)]] if r2 < 0.2 else (['fern'] if r2 < 0.26 else ['grass'])
        elif town: top = ['grass'] if r2 < 0.08 else []
        elif f > 0.3 and r2 < 0.3: top = [FLOWERS[int(r3 * 5)]]
        elif f < -0.3 and r2 < 0.2: top = ['fern']
        elif r2 < 0.16: top = ['grass']
    return 64, s, 'dirt', top


# houses of the north-west quarter: (x of the west wall, z of the jetty, front 's' or 'n', kinds for NW, NE, SW, SE)
HOUSES = [(-84, -99, 's', ('s2a', 's2b', 's2b', 's2a')), (-73, -99, 's', ('s1a', 's1b', 's1b', 's1a')), (-66, -99, 's', ('s1b', 's1c', 's1c', 's1b')),
          (-45, -99, 's', ('s1c', 's1a', 's1a', 's1c')), (-38, -99, 's', ('s4inn', None, None, 's4guild')),
          (-85, -84, 'n', ('s2c', 's2a', 's2a', 's2c')), (-72, -84, 'n', ('s3a', 's3b', 's3b', 's3a')),
          (-46, -84, 'n', ('s2b', 's2c', 's2c', 's2b')), (-35, -84, 'n', ('s3b', 's3a', 's3a', 's3b')),
          (-66, -41, 's', ('s1c', 's1a', 's1a', 's1c')), (-46, -54, 's', ('s2a', 's2c', 's2c', 's2a')),
          (-93, -19, 's', ('s5a', 's5b', 's5b', 's5a')), (-101, -19, 's', ('s1a', 's1c', 's1c', 's1a')),
          (-112, -103, 's', (None, 's2c', 's2c', None)),
          (-93, -99, 's', ('s1b', 's1a', 's1a', 's1b')), (-85, -48, 's', ('s1a', 's1c', 's1c', 's1a')), (-97, -84, 'n', ('s2b', 's2c', 's2c', 's2b')), (-77, -35, 's', ('s2a', 's2b', 's2b', 's2a'))]


def house_cells(name, x0, zf, face, seed):
    hb = houses.kind(name, seed)
    d = houses.KINDS[name]['d']
    if face == 's': return {(x0 + x, y, zf - d + z): v for (x, y, z), v in hb.c.items()}
    return {(x0 + x, y, zf + d + z): v for (x, y, z), v in xf.mirror(hb.c, 'z').items()}


def stall(colour, k=0):
    """a market stall, 5 x 3: four posts, a wool awning, a counter"""
    c = {}
    for x in (0, 4):
        for z in (0, 2):
            for y in (65, 66): c[(x, y, z)] = 'spruce_fence'
    for x in range(5):
        for z in range(3): c[(x, 67, z)] = ('%s_wool' % colour) if (x + k) % 2 else 'white_wool'
        c[(x, 67, 2)] = 'spruce_slab[type=bottom]'
    for x in (1, 2, 3): c[(x, 65, 0)] = ('barrel[facing=up]', 'spruce_slab[type=top]', 'hay_block[axis=x]', 'composter[level=6]')[(x + k) % 4]
    c[(2, 66, 2)] = 'lantern[hanging=true]'
    return c


def shed(k=0):
    """a lean-to shed, 5 x 3, open to the south"""
    c = {}
    for x in range(5):
        for z in range(3):
            if x in (0, 4) or z == 0:
                for y in (65, 66): c[(x, y, z)] = 'spruce_planks' if not (x in (0, 4) and z == 2) else 'spruce_fence'
            c[(x, 67, z)] = 'spruce_slab[type=%s]' % ('top' if z == 0 else 'bottom')
    for x, blk in ((1, 'barrel[facing=up]'), (2, 'hay_block[axis=x]'), (3, 'composter[level=4]')): c[(x, 65, 1)] = blk if (x + k) % 3 else 'chest[facing=south]'
    return c


def pavilion():
    """the butter market west of the ring: the hall's mass in open timber, x -42..-21, z -6..5, with the market cross under it"""
    b = Build(seed=91)
    for x in (-42, -35, -28, -21):
        for z in (-6, 5):
            b.set(x, 65, z, 'stone_bricks')
            for y in range(66, 70): b.set(x, y, z, 'spruce_log[axis=y]')
            for dx, f in ((-1, 'east'), (1, 'west')):
                if -42 <= x + dx <= -21: b.set(x + dx, 69, z, 'spruce_stairs[facing=%s,half=top,shape=straight]' % f)
    for x in range(-42, -20):
        for z in (-6, 5): b.set(x, 70, z, 'stripped_spruce_log[axis=x]')
    for x in (-42, -35, -28, -21):
        for z in range(-5, 5): b.set(x, 70, z, 'stripped_spruce_log[axis=z]')
    b.gable_x(-42, -21, -6, 5, 71, 'spruce_stairs', 'spruce_slab', fill=[('spruce_planks', 1)])
    for x in (-38, -31, -25):
        b.set(x, 69, -1, 'chain[axis=y]'); b.set(x, 68, -1, 'lantern[hanging=true]')
    for x, z, blk in ((-40, -3, 'barrel[facing=up]'), (-39, -3, 'spruce_slab[type=top]'), (-38, -3, 'spruce_slab[type=top]'), (-37, -3, 'hay_block[axis=x]'),
                      (-26, 2, 'barrel[facing=up]'), (-25, 2, 'spruce_slab[type=top]'), (-24, 2, 'spruce_slab[type=top]'), (-23, 2, 'composter[level=6]'),
                      (-39, 2, 'spruce_slab[type=top]'), (-38, 2, 'barrel[facing=up]'), (-25, -3, 'spruce_slab[type=top]'), (-24, -3, 'barrel[facing=up]')):
        b.set(x, 65, z, blk)
    b.set(-40, 66, -3, 'barrel[facing=up]')
    return dict(b.c)


def well():
    c = {}
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1): c[(dx, 65, dz)] = 'water_cauldron[level=3]' if dx == dz == 0 else ('mossy_cobblestone' if (dx + dz) % 2 else 'cobblestone')
    for dx in (-1, 1):
        for y in (66, 67): c[(dx, y, 0)] = 'oak_fence'
    for dx in (-1, 0, 1): c[(dx, 68, 0)] = 'spruce_slab[type=bottom]'
    c[(0, 67, 0)] = 'chain[axis=y]'
    return c


def standard(colour='white'):
    c = {(0, y, 0): 'stripped_spruce_log[axis=y]' for y in range(65, 75)}
    c[(0, 75, 0)] = 'lantern[hanging=false]'
    for y in range(71, 75):
        for j in (1, 2): c[(0, y, -j)] = '%s_wool' % colour
    return c


def quarter(i):
    """everything that stands in the north-west quarter, in north-west world cells; i picks the variant (0 NW, 1 NE, 2 SW, 3 SE)"""
    c = {}
    for n, (x0, zf, face, kinds) in enumerate(HOUSES):
        if kinds[i]: xf.put(c, house_cells(kinds[i], x0, zf, face, 11 + 7 * n + i))
    if i in (0, 3): xf.put(c, toll.toll().c, -118, -124)
    else:                                                          # the plot without a hold: a farmstead yard
        for x in range(-117, -96):
            for z in (-124, -101):
                if not -109 <= x <= -103: c[(x, 65, z)] = 'spruce_fence'
        for z in range(-124, -100): c[(-118, 65, z)] = 'spruce_fence'; c[(-95, 65, z)] = 'spruce_fence'
        for x, z, blk in ((-100, -106, 'hay_block[axis=x]'), (-100, -107, 'hay_block[axis=z]'), (-99, -106, 'hay_block[axis=x]'), (-98, -120, 'composter[level=5]'), (-99, -120, 'barrel[facing=up]')): c[(x, 65, z)] = blk
        c[(-100, 66, -106)] = 'hay_block[axis=x]'
    v = i in (1, 2)
    xf.put(c, stall('red' if not v else 'blue', 0), -30, -16); xf.put(c, stall('yellow' if not v else 'green', 1), -40, -12)
    xf.put(c, stall('green' if not v else 'red', 2), -19, -67)
    xf.put(c, well(), -14, -63); xf.put(c, well(), -18, -109)
    for k, (x, z) in enumerate(((-82, -64), (-44, -68), (-31, -65), (-70, -57))): xf.put(c, shed(k + i), x, z)
    for x, z, y, blk in ((-20, -76, 65, 'cauldron'), (-21, -105, 65, 'barrel[facing=up]'), (-21, -104, 65, 'barrel[facing=up]'), (-21, -105, 66, 'barrel[facing=up]'), (-20, -103, 65, 'hay_block[axis=z]'),
                         (-16, -70, 65, 'spruce_fence'), (-16, -70, 66, 'lantern[hanging=false]'), (16, -70, 65, 'spruce_fence') if False else (-18, -84, 65, 'composter[level=2]')):
        c[(x, y, z)] = blk
    xf.put(c, standard(), -15, -15); xf.put(c, standard(), -21, -78)
    # the cart by the bridgehead well, crates on the plaza edge and in the yards
    for x, z, y, blk in ((-9, -66, 65, 'spruce_slab[type=top]'), (-8, -66, 65, 'spruce_slab[type=top]'), (-7, -66, 65, 'spruce_fence'), (-9, -67, 65, 'spruce_trapdoor[facing=north,half=bottom,open=true]'),
                         (-8, -66, 66, 'barrel[facing=up]'), (-21, -70, 65, 'barrel[facing=up]'), (-21, -71, 65, 'barrel[facing=up]'), (-21, -70, 66, 'barrel[facing=up]'),
                         (-30, -66, 65, 'hay_block[axis=x]'), (-31, -66, 65, 'barrel[facing=up]'), (-64, -66, 65, 'composter[level=3]'), (-70, -64, 65, 'barrel[facing=up]'),
                         (-70, -65, 65, 'barrel[facing=up]'), (-78, -68, 65, 'hay_block[axis=z]'), (-28, -62, 65, 'grindstone[face=floor,facing=east]')):
        c[(x, y, z)] = blk
    # quay crane before the warehouse: a post, a jib over the water, a keg on the chain
    for y in range(65, 70): c[(-84, y, -11)] = 'spruce_log[axis=y]'
    for z in (-12, -10, -9, -8): c[(-84, 69, z)] = 'stripped_spruce_log[axis=z]'
    c[(-84, 68, -12)] = 'spruce_stairs[facing=south,half=top,shape=straight]'
    for y in (68, 67): c[(-84, y, -8)] = 'chain[axis=y]'
    c[(-84, 66, -8)] = 'barrel[facing=up]'
    if not v:                                                      # a moored barge
        for x in range(-97, -86):
            for z in range(-9, -5):
                rim = x in (-97, -87) or z in (-9, -6)
                c[(x, 62, z)] = 'dark_oak_planks'
                if rim: c[(x, 63, z)] = 'spruce_trapdoor[facing=%s,half=bottom,open=true]' % ('west' if x == -97 else ('east' if x == -87 else ('north' if z == -9 else 'south')))
        for x, blk in ((-95, 'barrel[facing=up]'), (-94, 'barrel[facing=up]'), (-93, 'hay_block[axis=x]'), (-90, 'barrel[facing=up]')): c[(x, 63, -8)] = blk
        c[(-94, 64, -8)] = 'barrel[facing=up]'; c[(-91, 63, -7)] = 'white_wool'; c[(-92, 63, -7)] = 'white_wool'
    else:                                                          # two rowing boats
        for x0 in (-96, -89):
            for x in range(x0, x0 + 4):
                c[(x, 62, -9)] = 'spruce_planks'
                c[(x, 63, -9)] = 'spruce_trapdoor[facing=%s,half=bottom,open=true]' % ('west' if x == x0 else 'east') if x in (x0, x0 + 3) else 'spruce_slab[type=bottom]'
    return c


def singles():
    """pieces that are not mirrored four ways: (cells, clear box or None)"""
    out = []
    out.append(ribs_mod.ribs())
    b = Build(seed=41); bridge.bridge(b); bridge.gatehouse(b)
    out.append(dict(b.c)); out.append(xf.mirror(b.c, 'z', -1))
    b = Build(seed=23); hall.build(b); out.append(dict(b.c))
    b = Build(seed=57); mill.build(b); out.append(dict(b.c))
    b = Build(seed=58); shrine.build(b)                            # built east of C at z 100..113; it stands west, one block nearer
    out.append(xf.mirror({(x, y, z - 1): v for (x, y, z), v in b.c.items()}, 'x', -1))
    b = Build(seed=71); watergate.build(b, 'cyan'); out.append(dict(b.c))
    b = Build(seed=71); watergate.build(b, 'purple'); out.append(xf.mirror(b.c, 'x', -1))
    cross = {}                                                     # the market cross west of the ring
    for x in range(-33, -29):
        for z in range(-2, 2):
            inner = x in (-32, -31) and z in (-1, 0)
            cross[(x, 65, z)] = 'stone_bricks' if inner else 'stone_brick_slab[type=bottom]'
    for y in range(66, 71): cross[(-32, y, -1)] = 'stone_brick_wall' if y < 70 else 'chiseled_stone_bricks'
    cross[(-32, 71, -1)] = 'stone_brick_wall'; cross[(-33, 70, -1)] = 'stone_brick_wall'; cross[(-31, 70, -1)] = 'stone_brick_wall'
    for x, z in ((-31, -1), (-32, 0), (-31, 0)): cross[(x, 66, z)] = 'stone_brick_slab[type=bottom]'
    for x, z, y, blk in ((-37, 3, 65, 'barrel[facing=up]'), (-37, 4, 65, 'barrel[facing=up]'), (-37, 3, 66, 'barrel[facing=up]'), (-36, -5, 65, 'hay_block[axis=x]'), (-36, -6, 65, 'composter[level=4]')):
        cross[(x, y, z)] = blk
    out.append(pavilion()); out.append(cross)
    return out


class Map:
    def __init__(self, box=None):
        self.x0, self.z0, self.x1, self.z1 = box or (X0, Z0, X1, Z1)
        self.col, self.extra = {}, {}
        pad = 8
        for x in range(self.x0 - pad, self.x1 + pad + 1):
            for z in range(self.z0 - pad, self.z1 + pad + 1):
                self.col[(x, z)] = column(x, z)
        for (x, z), (h, s, u, top) in self.col.items():
            for i, b in enumerate(top): self.extra[(x, h + 1 + i, z)] = b
        self.forest()
        self.guard()
        self.works()
        self.fold()
        qs = [quarter(i) for i in range(4)]
        qs[1] = xf.mirror(qs[1], 'x', -1); qs[2] = xf.mirror(qs[2], 'z', -1); qs[3] = xf.mirror(xf.mirror(qs[3], 'x', -1), 'z', -1)
        for cells in qs + singles():
            xs = {}
            for (x, y, z) in cells: xs.setdefault((x, z), []).append(y)
            for (x, z), ys in xs.items():                          # a piece owns its columns: nothing of the terrain's stays standing in them
                if max(ys) < 65 or min(ys) > 65: continue           # overhangs (jetties, jibs, rib tips) clear nothing below
                for y in range(65, 93): self.extra.pop((x, y, z), None)
            for pos, b in cells.items():
                if (pos[0], pos[2]) in self.col: self.extra[pos] = b

    def put(self, x, y, z, b):
        if (x, z) in self.col: self.extra[(x, y, z)] = b

    def guard(self):
        """the water guard: a parapet (in town) or a rail (out of town) on every land cell that touches open water"""
        for (x, z), (h, s, u, top) in self.col.items():
            ax, az = Q(x, z)
            if s == 'water' or base(ax, az): continue
            if h == 63:                                            # the causeway deck: parapets on its sides, a slab step at each end
                if ax in (100, 113): self.extra[(x, 64, z)] = 'stone_brick_wall'
                elif az == 9: self.extra[(x, 64, z)] = 'stone_brick_slab[type=bottom]'
                continue
            if not any(wet(ax + i, az + j) and not deck(ax + i, az + j) for i in (-1, 0, 1) for j in (-1, 0, 1)): continue
            r = hash01(SEED, x, z, 71)
            if 59 <= ax <= 63 and 20 <= az <= 45: g = 'spruce_fence_gate[facing=south,open=false]'      # the gated landings
            elif ax <= 113: g = 'mossy_stone_brick_wall' if r < 0.15 else 'stone_brick_wall'
            else: g = 'spruce_fence'
            self.extra[(x, h + 1, z)] = g
            if ax <= 113 and h == 64 and (ax * 7 + az * 3) % 13 == 0: self.extra[(x, h + 2, z)] = 'lantern[hanging=false]'
        for (x, z), (h, s, u, top) in self.col.items():            # culvert decks, mooring posts, the arch where the river leaves
            ax, az = Q(x, z)
            if s != 'water': continue
            if 100 <= ax <= 113:
                self.extra[(x, 63, z)] = mix(hash01(SEED, x, z, 72), QUAY)
                self.extra[(x, 64, z)] = 'stone_brick_wall' if ax in (100, 113) else ('stone_brick_slab[type=bottom]' if az == 9 else 'air')
                if self.extra[(x, 64, z)] == 'air': del self.extra[(x, 64, z)]
            elif ax >= 258:
                for y in range(65, 65 + bank(ax, 12)): self.extra[(x, y, z)] = mix(hash01(SEED, x + y, z, 73), MASON)
            elif ax <= 113 and not deck(ax, az) and not base(ax, az) and (ax * 5 + az * 11) % 17 == 0:
                if any(not wet(ax + i, az + j) for i, j in ((1, 0), (-1, 0), (0, 1), (0, -1))) and not isle(ax + 1, az) and not isle(ax, az - 1):
                    for y in range(60, 64): self.extra[(x, y, z)] = 'spruce_log[axis=y]'

    def works(self):
        """orchards, garden trees and willows; the same in all four quarters"""
        for sx in (1, -1):
            for sz in (1, -1):
                for ax in range(122, 168, 6):
                    for az in range(99, 120, 7):
                        j = hash01(SEED, ax, az, 51)
                        if j > 0.12: self.plant('apple', ax + (3 if (az // 7) % 2 else 0) + int(j * 3) - 1, az + int(j * 7) % 3 - 1, sx, sz, (ax + az) % 4)
                for ax, az in ((42, 120), (80, 118), (30, 121), (14, 120), (70, 54), (80, 50), (76, 60), (64, 58), (92, 60), (96, 44), (90, 36)): self.plant('apple', ax, az, sx, sz, ax % 4)
                for ax, az, name in ((126, 22, 'oak_medium'), (150, 30, 'oak_medium'), (140, 30, 'birch_pair'), (121, 31, 'oak_small'), (260, 20, 'oak_medium'),
                                     (100, 110, 'oak_great') if False else (93, 112, 'oak_medium'), (70, 140, 'oak_great'), (20, 142, 'birch_pair'), (46, 160, 'oak_medium'), (110, 146, 'oak_great'), (150, 150, 'oak_medium'), (12, 166, 'oak_small'), (84, 168, 'birch_tall'), (120, 170, 'oak_small'), (160, 136, 'birch_pair'), (8, 136, 'oak_medium'), (110, 60, 'oak_small'), (160, 110, 'oak_small')):
                    self.plant(name, ax, az, sx, sz, (ax + az) % 4)

    def fold(self):
        """a stone sheepfold and a hay rick in the pastures; the same in all four quarters"""
        for sx in (1, -1):
            for sz in (1, -1):
                for ax in range(40, 51):
                    for az in range(160, 168):
                        if not (ax in (40, 50) or az in (160, 167)) or (az == 160 and ax in (44, 45)): continue
                        x, z = W(ax, az, sx, sz)
                        if (x, z) in self.col: self.extra[(x, 65, z)] = 'cobblestone'; self.extra[(x, 66, z)] = 'cobblestone_wall'
                for ax, az, n in ((47, 164, 2), (48, 164, 1), (47, 165, 1), (110, 160, 3), (111, 160, 2), (110, 161, 2), (111, 161, 1), (109, 160, 1)):
                    x, z = W(ax, az, sx, sz)
                    if (x, z) in self.col:
                        for i in range(n): self.extra[(x, 65 + i, z)] = 'hay_block[axis=y]'

    def plant(self, name, ax, az, sx, sz, k=0):
        x0, z0 = W(ax, az, sx, sz)
        if (x0, z0) not in self.col: return
        h = self.col[(x0, z0)][0]
        for (dx, dy, dz), b in turned(TREES[name], k).items():
            pos = (x0 - sx * dx, h + dy, z0 - sz * dz)
            if 'leaves' in b: self.extra.setdefault(pos, b)
            else: self.extra[pos] = b

    def forest(self):
        WOOD = ['oak_medium', 'oak_great', 'birch_tall', 'dark_oak', 'oak_small', 'birch_pair', 'oak_medium', 'spruce_small']
        BANK = ['spruce_tall', 'spruce_small', 'oak_medium', 'birch_tall', 'spruce_tall', 'oak_small']
        spots = []
        for gx in range(174, 258, 7):
            for gz in range(52, 178, 7):
                ax, az = gx + int(hash01(SEED, gx, gz, 21) * 5) - 2, gz + int(hash01(SEED, gx, gz, 22) * 5) - 2
                if hash01(SEED, gx, gz, 23) < 0.78 and wood(ax, az):
                    spots.append((WOOD[int(hash01(SEED, gx, gz, 24) * len(WOOD))], ax, az, int(hash01(SEED, gx, gz, 25) * 4)))
        for gx in range(0, 290, 6):
            for gz in range(0, 194, 6):
                ax, az = gx + int(hash01(SEED, gx, gz, 31) * 4) - 1, gz + int(hash01(SEED, gx, gz, 32) * 4) - 1
                if az <= 16 and ax <= 270: continue                # nothing over the river's way out
                if bank(ax, az) >= 4 and ax <= 283 and az <= 187 and hash01(SEED, gx, gz, 33) < (0.95 if fbm(SEED, ax + 9, az + 5, 30) > -0.05 else 0.22):
                    spots.append((BANK[int(hash01(SEED, gx, gz, 34) * len(BANK))], ax, az, int(hash01(SEED, gx, gz, 35) * 4)))
        for name, ax, az, k in spots:
            for sx in (1, -1):
                for sz in (1, -1): self.plant(name, ax, az, sx, sz, k)

    def cells(self, lo, hi, solid=True):
        out = {}
        for x in range(lo[0], hi[0] + 1):
            for z in range(lo[2], hi[2] + 1):
                h, s, u, _ = self.col[(x, z)]
                low = lo[1] if solid else min([self.col.get((x + dx, z + dz), (h,))[0] for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))] + [h]) - 1
                for y in range(max(lo[1], low), min(h, hi[1]) + 1):
                    if y == h: b = s
                    elif u == 'water': b = 'water' if y >= 60 else (mix(hash01(SEED, x, z, 9), [('gravel', 50), ('stone', 30), ('mud', 20)]) if y == 59 else 'stone')
                    elif u == 'mason': b = mix(hash01(SEED, x + 13 * y, z, 8), MASON) if y > 58 else 'stone'
                    else: b = u if y > h - 4 else 'stone'
                    out[(x, y, z)] = b
        for (x, y, z), b in self.extra.items():
            if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]: out[(x, y, z)] = b
        return out

    def world(self, lo, hi, solid=False):
        w = World('rivergate')
        for pos, b in self.cells(lo, hi, solid).items(): w.set(pos, Cell(normalize(b)))
        return w


def section_box(name):
    c, r = 'ABCDEFGHI'.index(name[0]), int(name[1]) - 1
    return (X0 + 64 * c, Z0 + 64 * r, X0 + 64 * c + 63, Z0 + 64 * r + 63)


def free(b):
    return b is None or b.split('[')[0] in PASS or b.endswith('_carpet') or b.endswith('pressure_plate')


def check(m):
    bad = 0
    for name, (px, pz) in POINTS.items():
        n = [k for k, b in m.extra.items() if k[1] > 64 and math.hypot(k[0] - px, k[2] - pz) < 17 and b != 'air']
        hs = {m.col[(x, z)][0] for x in range(px - 17, px + 18) for z in range(pz - 17, pz + 18) if math.hypot(x - px, z - pz) < 17}
        print('point', name, 'blocks standing in the ring:', len(n), sorted(n)[:6], 'ground heights:', sorted(hs))
        bad += len(n) + (hs != {64})
    # the water guard: no cell a soldier can stand on may touch open water
    def blk(x, y, z):
        h, s, u, _ = m.col[(x, z)]
        return m.extra.get((x, y, z), s if y == h else (('water' if u == 'water' else 'stone') if y < h else None))

    def stand(x, z):
        """the y a walker stands at in this column near street level, or None"""
        for y in (67, 66, 65, 64, 63):
            b = blk(x, y, z)
            if b in ('water', None, 'air') or free(b) or b.startswith('lantern'): continue
            if y == 66 and not free(blk(x, 65, z)): return None      # two high from the street: nobody steps up that
            if y == 67 or 'wall' in b or 'fence' in b or 'iron_bars' in b or 'trapdoor' in b: return None
            return y if free(blk(x, y + 1, z)) and free(blk(x, y + 2, z)) else None
        return None
    gaps = []
    for (x, z), (h, s, u, _) in m.col.items():
        if not (X0 <= x <= X1 and Z0 <= z <= Z1) or s != 'water': continue
        if any(m.extra.get((x, y, z)) not in (None, 'air', 'lily_pad') for y in (62, 63, 64)): continue      # decked or built over
        for i in (-1, 0, 1):
            for j in (-1, 0, 1):
                n = (x + i, z + j)
                if n not in m.col or (m.col[n][1] == 'water' and all(m.extra.get((n[0], y, n[1])) in (None, 'air', 'lily_pad') for y in (62, 63, 64))): continue
                if abs(n[0]) > 270 or abs(n[1]) > 190: continue
                if m.col[n][1] == 'water' and not deck(*Q(*n)): continue      # boats and posts: not reachable from the land
                if stand(*n) is not None: gaps.append(n)
    gaps = sorted(set(gaps))
    print('water guard gaps:', len(gaps), gaps[:60])
    return bad + len(gaps)


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'render':
        from mapforge import render
        out, view = sys.argv[2], sys.argv[3]
        box = tuple(int(v) for v in sys.argv[4:8]) if len(sys.argv) >= 8 else (X0, Z0, X1, Z1)
        m = Map(box)
        lo, hi = (box[0], YLO, box[1]), (box[2], YHI + 8, box[3])
        img = render.render(m.world(lo, hi), view=view, lo=lo, hi=hi, grid=False, title='Rivergate %s' % (box,))
        img.save(out); print(out, img.size)
    elif cmd == 'export':
        outdir = sys.argv[2]; os.makedirs(outdir, exist_ok=True)
        names = sys.argv[3:] or [c + str(r) for r in range(1, 7) for c in 'ABCDEFGHI']
        for n in names:
            b = section_box(n)
            m = Map(b)
            lo, hi = (b[0], YLO, b[1]), (b[2], YHI, b[3])
            m.world(lo, hi, solid=True).export_structure(lo, hi).save(os.path.join(outdir, 'rg_%s.nbt' % n.lower()))
            print(n, b)
    elif cmd == 'check':
        sys.exit(1 if check(Map()) else 0)
