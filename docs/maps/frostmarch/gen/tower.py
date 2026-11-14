"""Frostmarch: the Signal Tower, the map's landmark. A grey stone beacon tower, 13 x 13 at the foot, 40 tall to the
fire. tower(G) returns {(x, y, z): block} in local coordinates: centre (0, 0), ground block at y G, door to +z (south).
Troops can use the ground-floor room as cover; nothing above it can be reached (no stairs, no ladders), so the lit
beacon is safe.
  python tower.py OUT.png [turn]"""
import math, sys, os

WALL = [('stone_bricks', 62), ('cracked_stone_bricks', 12), ('andesite', 12), ('cobblestone', 8), ('polished_andesite', 6)]
FOOT = [('cobblestone', 50), ('andesite', 30), ('stone', 20)]


def h3(x, y, z, n): return ((x * 73856093) ^ (y * 83492791) ^ (z * 19349663) ^ 0x51f3) % n


def pick(mix, x, y, z):
    r = h3(x, y, z, sum(w for _, w in mix))
    for b, w in mix:
        r -= w
        if r < 0: return b
    return mix[-1][0]


def S(kind, f, top=False): return '%s[facing=%s,half=%s,shape=straight]' % (kind, f, 'top' if top else 'bottom')


def tower(G=80):
    c = {}
    def put(x, y, z, b): c[(x, y, z)] = b
    SH = 32                                                 # shaft height above the ground
    for y in range(G + 1, G + SH + 1):
        h = y - G
        r = 6 if h <= 6 else 5                              # a battered foot, then the shaft
        for x in range(-r, r + 1):
            for z in range(-r, r + 1):
                edge = max(abs(x), abs(z)) == r
                if not edge: put(x, y, z, 'air'); continue
                corner = abs(x) == r and abs(z) == r
                b = pick(FOOT if h <= 2 else WALL, x, y, z)
                if h in (7, 17): b = 'polished_andesite'                             # string courses
                if corner and h > 6: b = 'stone_bricks' if h % 4 else 'chiseled_stone_bricks'
                put(x, y, z, b)
        if h == 6:                                          # the weathering where the foot meets the shaft
            for i in range(-6, 7):
                for (x, z, f) in ((i, -6, 'south'), (i, 6, 'north'), (-6, i, 'east'), (6, i, 'west')):
                    if abs(i) < 6: put(x, y + 1, z, S('stone_brick_stairs', f))
            for x in (-6, 6):
                for z in (-6, 6): put(x, y + 1, z, 'stone_brick_slab[type=bottom]')
    # floors: the ground-floor ceiling is solid, so nothing above is reachable
    for x in range(-5, 6):
        for z in range(-5, 6):
            put(x, G, z, pick([('stone_bricks', 5), ('andesite', 3), ('cobblestone', 2)], x, G, z))
            put(x, G + 6, z, 'spruce_planks')
            if max(abs(x), abs(z)) <= 4: put(x, G + 17, z, 'spruce_planks')
    # the door (south) and arrow slits
    for y in (G + 1, G + 2, G + 3):
        for x in (-1, 0, 1): put(x, y, 6, 'air')
    for x in (-1, 0, 1): put(x, G + 4, 6, 'polished_andesite')
    put(-2, G + 4, 6, 'chiseled_stone_bricks'); put(2, G + 4, 6, 'chiseled_stone_bricks')
    for h in (10, 11, 21, 22):
        for (x, z) in ((0, -5), (0, 5), (-5, 0), (5, 0)): put(x, G + h, z, 'air')
    for h in (13, 25):
        for (x, z) in ((-3, 5), (3, 5), (5, -3), (5, 3), (-5, -3), (-5, 3), (-3, -5), (3, -5)): put(x, G + h, z, 'air')
    # ground-floor room: hearth (cold), bunks, stores
    put(-4, G + 1, -4, 'barrel[facing=up]'); put(-3, G + 1, -4, 'barrel[facing=up]'); put(-4, G + 2, -4, 'barrel[facing=up]'); put(4, G + 1, -4, 'spruce_planks'); put(4, G + 1, -3, 'spruce_planks')
    put(0, G + 1, -5, 'cobblestone'); put(-4, G + 1, 3, 'hay_block[axis=x]'); put(4, G + 1, 4, 'barrel[facing=up]'); put(0, G + 5, 0, 'lantern[hanging=true]')
    # the head: corbelled out one block over the shaft, a snowed-up fighting deck, battlements
    HY = G + SH - 4
    for x in range(-6, 7):
        for z in range(-6, 7):
            m = max(abs(x), abs(z))
            if m == 6:
                f = 'south' if z == -6 else 'north' if z == 6 else 'east' if x == -6 else 'west'
                put(x, HY, z, S('stone_brick_stairs', f, True) if not (abs(x) == 6 and abs(z) == 6) else 'chiseled_stone_bricks')
                for y in range(HY + 1, HY + 5): put(x, y, z, 'polished_andesite' if y == HY + 1 else pick(WALL, x, y, z))
                if (x + z) % 3 == 0 and abs(x) != abs(z): put(x, HY + 3, z, 'air')                                  # crenel-height loops
    TY = G + SH + 1
    for x in range(-6, 7):
        for z in range(-6, 7):
            m = max(abs(x), abs(z))
            put(x, TY - 1, z, 'stone_bricks')
            if m == 6:
                if (x + z) % 2 == 0 or abs(x) == abs(z): put(x, TY, z, pick(WALL, x, TY, z)); put(x, TY + 1, z, 'stone_brick_wall' if abs(x) != abs(z) else 'stone_bricks'); put(x, TY + 2 if abs(x) == abs(z) else TY + 1, z, 'snow_block' if abs(x) == abs(z) else c[(x, TY + 1, z)])
                else: put(x, TY, z, 'stone_brick_slab[type=bottom]')
            elif m >= 3: put(x, TY, z, 'snow_block' if h3(x, 1, z, 4) else 'andesite')
    for x in range(-2, 3):                                  # the fire table
        for z in range(-2, 3):
            m = max(abs(x), abs(z))
            for y in range(TY + 1, TY + 5): put(x, y, z, 'polished_deepslate' if m == 2 and abs(x) == abs(z) else ('deepslate_bricks' if m < 2 else 'air'))
            if m < 2: put(x, TY + 5, z, 'shroomlight'); put(x, TY + 6, z, 'hay_block[axis=y]'); put(x, TY + 7, z, 'campfire[lit=true,signal_fire=true]')
            elif abs(x) == abs(z):
                for y in range(TY + 5, TY + 11): put(x, y, z, 'iron_bars' if y < TY + 10 else 'polished_deepslate')
            else:
                put(x, TY + 5, z, 'polished_deepslate'); put(x, TY + 6, z, 'iron_bars')
    for x in range(-3, 4):                                  # the iron canopy over the fire, open in the middle for the smoke
        for z in range(-3, 4):
            m = max(abs(x), abs(z))
            if m == 3: put(x, TY + 10, z, 'deepslate_tile_slab[type=bottom]')
            elif m == 2 and abs(x) != abs(z): put(x, TY + 10, z, 'deepslate_tile_slab[type=top]')
    for (x, z) in ((-2, -2), (2, -2), (-2, 2), (2, 2)): put(x, TY + 11, z, 'lightning_rod[facing=up]')
    # lean-to woodshed on the west side, and snow drifts that do not make steps
    for z in range(-3, 4):
        for x in (-9, -8, -7):
            put(x, G + 4 - (-7 - x), z, 'spruce_slab[type=bottom]' if x != -7 else 'spruce_planks')
        if abs(z) == 3:
            for y in range(G + 1, G + 3): put(-9, y, z, 'spruce_fence')
        elif z != 0: put(-8, G + 1, z, 'spruce_log[axis=z]'); put(-7, G + 1, z, 'spruce_log[axis=z]'); put(-7, G + 2, z, 'spruce_log[axis=z]')
    return c


if __name__ == '__main__':
    HERE = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
    from mapforge.world import World, Cell
    from mapforge.structure import normalize
    from mapforge import render
    G = 80; c = tower(G); turns = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    for x in range(-14, 15):
        for z in range(-14, 15): c.setdefault((x, G, z), 'snow_block')
    for _ in range(turns): c = {(-z, y, x): v for (x, y, z), v in c.items()}
    w = World('tower')
    for p, v in c.items():
        if v != 'air': w.set(p, Cell(normalize(v)))
    lo, hi = (-14, G, -14), (14, G + 48, 14)
    img = render.render(w, view='iso', lo=lo, hi=hi, grid=False, title='Frostmarch: the Signal Tower'); img.save(sys.argv[1]); print(sys.argv[1], img.size, len(c))
