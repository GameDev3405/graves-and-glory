"""Ironhold buildings and field works, each in local coordinates (ground block at y 64, front to +z) as a dict of cells.
PIECES maps a name to a function that returns the cells. As a script it lays every piece out on a workshop pad,
prints the export boxes and writes the in-game shot script.
Usage: python works.py OUT.json [SHOTS.txt]"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, '..', '..', 'rivergate', 'gen')))
from kit import Build
import houses, xf
from gate import DEEP, FOOT, WARD

G = 64
houses.STYLES['iron'] = dict(ground=DEEP, post='dark_oak_log', beam='stripped_dark_oak_log', infill='light_gray_concrete', roof='deepslate_tile', trim='dark_oak', bed='purple')
houses.STYLES['ash'] = dict(ground=FOOT, post='spruce_log', beam='stripped_spruce_log', infill='cyan_terracotta', roof='deepslate_tile', trim='spruce', bed='gray')
houses.STYLES['soot'] = dict(ground=DEEP, post='dark_oak_log', beam='stripped_dark_oak_log', infill='deepslate_bricks', roof='dark_oak', trim='dark_oak', bed='purple')
SLAG = [('gravel', 30), ('tuff', 22), ('basalt[axis=y]', 16), ('cobbled_deepslate', 18), ('raw_iron_block', 5), ('coal_block', 4), ('smooth_basalt', 5)]
RUIN = [('cobbled_deepslate', 35), ('cracked_deepslate_bricks', 30), ('deepslate_bricks', 25), ('mossy_cobblestone', 10)]
CAMP = 'cyan'


def S(kind, f, top=False): return '%s[facing=%s,half=%s,shape=straight]' % (kind, f, 'top' if top else 'bottom')


def nz(x, z, n, k=0): return ((x * 73856093) ^ (z * 19349663) ^ (k * 83492791) ^ 0x2545f491) % n


class C(dict):
    def set(self, x, y, z, b): self[(x, y, z)] = b
    def box(self, x0, y0, z0, x1, y1, z1, blk):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1): self[(x, y, z)] = blk(x, y, z) if callable(blk) else blk


def stack(c, x0, z0, x1, z1, top, core='polished_blackstone_bricks', smoke=True):
    """A chimney stack from the ground to `top`, with a hidden fire for smoke that nobody can reach."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(G + 1, top + 1): c.set(x, y, z, core if (y - G) % 5 else 'chiseled_polished_blackstone')
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    if smoke and x1 - x0 >= 2:
        c.set(cx, top, cz, 'campfire[lit=true,signal_fire=true]'); c.set(cx, top - 1, cz, 'hay_block[axis=y]')
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) != (cx, cz) or not smoke: c.set(x, top + 1, z, 'polished_blackstone_brick_wall' if (x in (x0, x1) and z in (z0, z1)) else 'air')


SWAP = {'bricks': 'polished_blackstone_bricks', 'cobblestone': 'cobbled_deepslate', 'mossy_cobblestone': 'cobbled_deepslate', 'stone_bricks': 'deepslate_bricks', 'andesite': 'tuff',
        'mossy_stone_bricks': 'cracked_deepslate_bricks', 'cracked_stone_bricks': 'cracked_deepslate_bricks', 'granite': 'blackstone', 'mud_bricks': 'deepslate_tiles'}


def hs(*a, **k):
    """A Rivergate-kit house with every town-coloured block turned to Ironhold stone."""
    out = {}
    for key, v in houses.house(*a, **k).c.items():
        n = v.split('[')[0]
        for old in ('brick_stairs', 'brick_slab', 'brick_wall', 'cobblestone_stairs', 'cobblestone_slab', 'cobblestone_wall', 'stone_brick_stairs', 'stone_brick_slab', 'stone_brick_wall'):
            if n == old: v = 'deepslate_brick_' + old.rsplit('_', 1)[1] + v[len(n):]
        out[key] = SWAP.get(n, n) + v[len(n):] if n in SWAP else v
        if v == 'campfire[lit=true]': out[key] = 'campfire[lit=false]'            # house hearths stay cold: troops walk through these rooms
    return type('H', (), {'c': out})


def forge(seed=3):
    c = C(hs(13, 9, floors=1, style='soot', hoist=True, door=6, seed=seed).c)
    stack(c, 13, 2, 15, 4, G + 13)
    for x in range(-1, 14):                                                               # the open smithy under a lean-to, on the street side
        for z in range(9, 13): c.set(x, G + 5 - (z - 9 + 1) // 2, z, 'dark_oak_slab[type=%s]' % ('top' if (z - 9) % 2 == 0 else 'bottom')) if not 5 <= x <= 7 or z > 9 else None
    for x in (-1, 4, 8, 13):
        for y in range(G + 1, G + 4): c.set(x, y, 12, 'dark_oak_fence')
    for (x, z, blk) in ((0, 10, 'anvil[facing=east]'), (1, 11, 'grindstone[face=floor,facing=north]'), (3, 10, 'blast_furnace[facing=south,lit=false]'), (2, 10, 'polished_blackstone'),
                        (9, 10, 'smithing_table'), (10, 10, 'cauldron'), (12, 10, 'iron_block'), (12, 11, 'iron_block'), (11, 10, 'barrel[facing=up]'), (9, 11, 'anvil[facing=north]')):
        c.set(x, G + 1, z, blk)
    c.set(12, G + 2, 10, 'iron_block'); c.set(2, G + 2, 10, 'lantern[hanging=false]')
    return c


def smelter():
    c = C()
    for x in range(0, 11):
        for z in range(0, 11):
            edge = x in (0, 10) or z in (0, 10); corner = x in (0, 10) and z in (0, 10)
            c.set(x, G, z, 'polished_blackstone' if (x + z) % 2 else 'blackstone')
            for y in range(G + 1, G + 8):
                if corner: continue
                if edge: c.set(x, y, z, 'polished_blackstone_bricks' if y <= G + 2 else 'polished_deepslate' if y == G + 7 else ('deepslate_bricks', 'deepslate_tiles', 'cracked_deepslate_bricks')[nz(x, z, 3, y)])
                elif y == G + 7: c.set(x, y, z, 'deepslate_tiles')
    for y in (G + 1, G + 2, G + 3):
        for x in (4, 5, 6): c.set(x, y, 10, 'air')
    c.set(4, G + 4, 10, S('polished_blackstone_brick_stairs', 'east', True)); c.set(6, G + 4, 10, S('polished_blackstone_brick_stairs', 'west', True))
    for x in (2, 8):
        for y in (G + 3, G + 4): c.set(x, y, 10, 'iron_bars'); c.set(0, y, x + 1, 'iron_bars'); c.set(10, y, x + 1, 'iron_bars')
    for x in range(3, 8):                                                                 # the furnace block against the back wall, and its great stack
        for z in (1, 2, 3):
            for y in range(G + 1, G + 7): c.set(x, y, z, 'polished_blackstone_bricks')
    for x in (4, 5, 6): c.set(x, G + 1, 3, 'blast_furnace[facing=south,lit=false]'); c.set(x, G + 2, 3, 'iron_bars')
    for y in range(G + 8, G + 19):
        r = 2 if y < G + 12 else 1
        for x in range(5 - r, 5 + r + 1):
            for z in range(2 - r + 1, 2 + r + 2):
                if r == 2 and abs(x - 5) == 2 and z in (1, 5): continue
                c.set(x, y, z, 'polished_basalt[axis=y]' if (y - G) % 4 else 'polished_blackstone')
    c.set(5, G + 18, 3, 'campfire[lit=true,signal_fire=true]'); c.set(5, G + 17, 3, 'hay_block[axis=y]')
    for (x, z, blk) in ((1, 1, 'coal_block'), (1, 2, 'coal_block'), (2, 1, 'coal_block'), (9, 1, 'raw_iron_block'), (9, 2, 'raw_iron_block'), (8, 1, 'raw_iron_block'), (1, 8, 'cauldron'), (9, 8, 'anvil[facing=east]'), (9, 6, 'barrel[facing=up]')):
        c.set(x, G + 1, z, blk)
    c.set(1, G + 2, 1, 'coal_block'); c.set(9, G + 2, 1, 'raw_iron_block')
    for (x, z) in ((2, 6), (8, 6)): c.set(x, G + 6, z, 'chain[axis=y]'); c.set(x, G + 5, z, 'lantern[hanging=true]')
    return c


def stables():
    c = C(); b = Build(seed=7)
    for x in range(0, 19):
        for z in range(0, 8):
            c.set(x, G, z, 'coarse_dirt' if z < 5 and 0 < x < 18 and nz(x, z, 3) else 'cobbled_deepslate')
            back = z == 0 or x in (0, 18)
            if back and (z < 6):
                for y in range(G + 1, G + 5): c.set(x, y, z, b.pick(FOOT) if y <= G + 1 else b.pick(DEEP))
    for x in range(0, 19, 3):
        for y in range(G + 1, G + 5): c.set(x, y, 5, 'dark_oak_log[axis=y]')
        if 0 < x < 18:
            for z in (1, 2, 3, 4): c.set(x, G + 1, z, 'dark_oak_fence'); c.set(x, G + 2, z, 'dark_oak_fence') if z < 3 else None
    for x in range(1, 18):
        if x % 3: c.set(x, G + 1, 1, 'hay_block[axis=x]' if x % 6 < 3 else 'cauldron')
        c.set(x, G + 4, 5, 'stripped_dark_oak_log[axis=x]') if x % 3 else None
    b2 = Build(seed=8); b2.gable_x(0, 18, 0, 6, G + 5, 'deepslate_tile_stairs', 'deepslate_tile_slab', fill=DEEP, over=1)
    c.update(b2.c)
    for (x, z, blk) in ((2, 7, 'hay_block[axis=y]'), (3, 7, 'hay_block[axis=x]'), (16, 7, 'barrel[facing=up]'), (10, 7, 'cauldron')): c.set(x, G + 1, z, blk)
    c.set(2, G + 2, 7, 'hay_block[axis=x]')
    for x in (6, 12): c.set(x, G + 3, 6, 'lantern[hanging=true]'); c.set(x, G + 4, 6, 'dark_oak_fence')
    return c


def store(): return C(hs(16, 10, floors=2, style='ash', hoist=True, door=8, seed=11).c)


def barrack(seed=5): return C(hs(20, 10, floors=2, style='iron', door=9, seed=seed).c)


def barracks_yard(flag='purple'):
    """Two barrack blocks face each other across a drill yard 22 deep; the defenders start in the yard."""
    c = C(); a = barrack(5); bb = xf.turn(barrack(9), 2)
    xf.put(c, a, 0, 0); xf.put(c, bb, 19, 44)                                             # the second block turned to face north
    for x in range(-2, 22):
        for z in range(11, 34):
            if (x, G, z) not in c: c.set(x, G, z, ('cobbled_deepslate', 'gravel', 'cobblestone', 'tuff')[nz(x, z, 4)])
    for (x, z) in ((2, 15), (2, 29), (17, 15), (17, 29)):                                 # drill posts
        c.set(x, G + 1, z, 'dark_oak_fence'); c.set(x, G + 2, z, 'hay_block[axis=y]'); c.set(x, G + 3, z, 'dark_oak_fence')
    for x in (6, 7, 8, 11, 12, 13):                                                       # weapon racks along the east side
        c.set(21, G + 1, 5 + x, 'dark_oak_fence' if x % 2 else 'iron_bars[north=true,south=true]'); c.set(21, G + 2, 5 + x, 'dark_oak_slab[type=bottom]')
    for y in range(G + 1, G + 12): c.set(-2, y, 13, 'dark_oak_fence')
    c.set(-2, G + 1, 13, 'polished_blackstone')
    for i in range(1, 6):
        for y in (G + 9, G + 10, G + 11):
            if not (i == 5 and y == G + 10): c.set(-2 + i, y, 13, 'black_wool' if y == G + 10 else flag + '_wool')
    for (x, z, blk) in ((0, 12, 'barrel[facing=up]'), (1, 12, 'barrel[facing=up]'), (19, 32, 'barrel[facing=up]'), (18, 32, 'smithing_table'), (0, 32, 'grindstone[face=floor,facing=north]'), (19, 12, 'anvil[facing=east]')):
        c.set(x, G + 1, z, blk)
    for (x, z) in ((5, 12), (14, 12), (5, 32), (14, 32)):
        c.set(x, G + 1, z, 'polished_blackstone_brick_wall'); c.set(x, G + 2, z, 'polished_blackstone'); c.set(x, G + 3, z, 'lantern[hanging=false]')
    return c


def tent(l=7, stripe='white'):
    c = C()
    for z in range(l):
        col = (CAMP + '_wool') if z % 3 != 1 else stripe + '_wool'
        for (x, y) in ((0, 1), (6, 1), (0, 2), (6, 2), (1, 3), (5, 3), (2, 4), (4, 4), (3, 5)): c.set(x, G + y, z, col)
        if z == 0:
            for x in range(1, 6):
                for y in range(1, 5 - abs(x - 3) + (1 if abs(x - 3) == 2 else 0)): c.set(x, G + y, z, col)
    for y in range(1, 5): c.set(3, G + y, l - 1, 'spruce_fence')
    c.set(3, G + 6, l - 1, 'spruce_fence'); c.set(3, G + 7, l - 1, CAMP + '_banner[rotation=0]')
    for z in (1, 2): c.set(1, G + 1, z, 'hay_block[axis=z]'); c.set(5, G + 1, z, 'hay_block[axis=z]')
    c.set(3, G + 1, 1, 'barrel[facing=up]'); c.set(5, G + 1, 4, 'chest[facing=west]'); c.set(1, G + 1, 4, 'barrel[facing=up]')
    return c


def pavilion():
    c = C()
    for x in range(0, 9):
        for z in range(0, 9):
            c.set(x, G, z, 'spruce_planks' if (x + z) % 3 else 'dark_oak_planks')
            edge = x in (0, 8) or z in (0, 8)
            if edge and not (z == 8 and 2 <= x <= 6):
                for y in range(G + 1, G + 4): c.set(x, y, z, (CAMP if (x + z) % 4 < 2 else 'white') + '_wool')
    for i in range(5):
        for x in range(-1 + i, 10 - i):
            for z in range(-1 + i, 10 - i):
                if x in (-1 + i, 9 - i) or z in (-1 + i, 9 - i): c.set(x, G + 4 + i, z, (CAMP if i % 2 == 0 else 'white') + '_wool')
    for (x, z) in ((0, 0), (8, 0), (0, 8), (8, 8)):
        for y in range(G + 1, G + 4): c.set(x, y, z, 'spruce_log[axis=y]')
    for y in range(G + 9, G + 13): c.set(4, y, 4, 'spruce_fence')
    for i in (1, 2, 3):
        for y in (G + 11, G + 12): c.set(4 + i, y, 4, CAMP + '_wool')
    for x in (3, 4, 5):
        for z in (3, 4): c.set(x, G + 1, z, 'cartography_table' if (x, z) == (4, 3) else 'spruce_slab[type=top]')
    for (x, z, blk) in ((4, 2, S('spruce_stairs', 'north')), (2, 4, S('spruce_stairs', 'west')), (6, 4, S('spruce_stairs', 'east')), (1, 1, 'chest[facing=south]'), (7, 1, 'barrel[facing=up]'), (1, 6, CAMP + '_bed[facing=north,part=head]'), (1, 7, CAMP + '_bed[facing=north,part=foot]'), (7, 7, 'lectern[facing=west]')):
        c.set(x, G + 1, z, blk)
    c.set(5, G + 2, 4, 'candle[candles=3,lit=false]'); c.set(4, G + 7, 4, 'chain[axis=y]'); c.set(4, G + 6, 4, 'lantern[hanging=true]')
    for x in (1, 7): c.set(x, G + 1, 9, 'spruce_fence'); c.set(x, G + 2, 9, 'spruce_fence'); c.set(x, G + 3, 9, CAMP + '_banner[rotation=0]')
    return c


def trebuchet():
    """Cocked: the long arm drawn down to the rear (+x), the weight box high at the front. 13 long, 5 wide, 13 tall."""
    c = C()
    for x in range(0, 13):
        for z in (0, 4): c.set(x, G + 1, z, 'dark_oak_log[axis=x]')
    for x in (0, 6, 12):
        for z in (1, 2, 3): c.set(x, G + 1, z, 'dark_oak_log[axis=z]')
    for z in (0, 4):                                                                      # the two A-frames
        for y in range(G + 2, G + 9): c.set(5, y, z, 'stripped_spruce_log[axis=y]')
        for i in range(1, 5):
            c.set(5 - i, G + 8 - 2 * i + 1, z, 'spruce_fence'); c.set(5 - i, G + 8 - 2 * i, z, 'spruce_fence'); c.set(5 + i, G + 8 - 2 * i + 1, z, 'spruce_fence'); c.set(5 + i, G + 8 - 2 * i, z, 'spruce_fence')
    for z in range(0, 5): c.set(5, G + 9, z, 'stripped_dark_oak_log[axis=z]')              # axle
    for i in range(1, 8): c.set(5 + i, G + 9 - i, 2, 'stripped_spruce_log[axis=x]')       # long arm, down to the rear
    for i in range(1, 4): c.set(5 - i, G + 9 + i, 2, 'stripped_spruce_log[axis=x]')       # short arm, up to the front
    c.set(2, G + 11, 2, 'chain[axis=y]'); c.set(2, G + 10, 2, 'chain[axis=y]')
    for x in (1, 2, 3):
        for z in (1, 2, 3):
            for y in (G + 7, G + 8, G + 9):
                if not (y == G + 9 and (x, z) != (2, 2)): c.set(x, y, z, 'iron_block' if (x, z) == (2, 2) and y == G + 8 else ('cobbled_deepslate', 'deepslate_bricks', 'tuff')[nz(x + y, z, 3)])
    for z in (1, 3):
        for x in (1, 3): c.set(x, G + 9, z, 'dark_oak_slab[type=bottom]')
    c.set(12, G + 2, 2, 'chain[axis=x]'); c.set(11, G + 2, 2, 'cobbled_deepslate')        # the sling and its stone
    for z in (1, 2, 3): c.set(9, G + 2, z, 'stripped_dark_oak_log[axis=z]')                # the winch drum, its handles and rope
    for z in (0, 4): c.set(9, G + 2, z, 'dark_oak_fence'); c.set(9, G + 3, z, 'grindstone[face=wall,facing=%s]' % ('north' if z == 0 else 'south'))
    c.set(10, G + 3, 2, 'chain[axis=x]'); c.set(11, G + 3, 2, 'chain[axis=x]')
    for y in range(G + 2, G + 9): c.set(4, y, 4, 'ladder[facing=west]')                    # a ladder up the south frame
    for x in (0, 12):                                                                      # wheels
        for z in (-1, 5): c.set(x, G + 1, z, 'dark_oak_trapdoor[facing=%s,half=bottom,open=true]' % ('north' if z < 0 else 'south'))
    for x in (1, 3):                                                                       # iron straps on the weight box
        for z in (1, 3):
            for y in (G + 7, G + 8): c.set(x, y, z, 'dark_oak_planks')
    for x in range(15, 18):                                                                # a proper pile of shot
        for z in range(1, 4): c.set(x, G + 1, z, 'cobbled_deepslate' if (x + z) % 2 else 'tuff')
    for x in (15, 16):
        for z in (1, 2): c.set(x, G + 2, z, 'tuff' if (x + z) % 2 else 'cobbled_deepslate')
    c.set(15, G + 3, 1, 'cobbled_deepslate')
    return c


def wagon(load=0):
    c = C()
    for x in range(0, 5):
        for z in (0, 1, 2): c.set(x, G + 1, z, 'spruce_slab[type=top]')
        c.set(x, G + 2, -1 + 0, 'air')
    for x in (0, 4):
        for z in (-1, 3): c.set(x, G + 1, z, 'dark_oak_trapdoor[facing=%s,half=bottom,open=true]' % ('north' if z < 0 else 'south'))
    for x in range(0, 5):
        for z in (0, 2): c.set(x, G + 2, z, 'spruce_trapdoor[facing=%s,half=bottom,open=true]' % ('south' if z == 0 else 'north')) if load != 2 else None
    for (x, blk) in ((0, 'barrel[facing=up]'), (1, 'hay_block[axis=x]'), (2, 'hay_block[axis=x]'), (3, 'barrel[facing=east]'), (4, 'chest[facing=east]')) if load == 0 else ((0, 'cobbled_deepslate'), (1, 'tuff'), (2, 'cobbled_deepslate'), (3, 'cobbled_deepslate'), (4, 'barrel[facing=up]')):
        c.set(x, G + 2, 1, blk)
    c.set(5, G + 1, 1, 'spruce_fence'); c.set(6, G + 1, 1, 'spruce_fence')
    return c


def palisade(n=25, gate=True):
    c = C()
    for x in range(n):
        mid = gate and abs(x - n // 2) <= 2
        if mid: continue
        h = 4 + nz(x, 0, 2) + (2 if gate and abs(x - n // 2) == 3 else 0)
        for y in range(G + 1, G + h + 1): c.set(x, y, 0, 'spruce_log[axis=y]' if nz(x, 1, 4) else 'stripped_spruce_log[axis=y]')
        if nz(x, 2, 3) == 0: c.set(x, G + h + 1, 0, 'spruce_fence')
        if x % 6 == 2: c.set(x, G + 1, 1, 'spruce_log[axis=y]'); c.set(x, G + 2, 1, S('spruce_stairs', 'north'))
    if gate:
        for x in (n // 2 - 3, n // 2 + 3): c.set(x, G + 5, -1, CAMP + '_wall_banner[facing=north]')
        for x in range(n // 2 - 2, n // 2 + 3): c.set(x, G + 6, 0, 'stripped_spruce_log[axis=x]')
    return c


def mine_head():
    c = C(); b = Build(seed=13)
    for x in range(0, 7):                                                                 # the shaft collar, too high to climb, lidded
        for z in range(0, 7):
            edge = x in (0, 6) or z in (0, 6)
            for y in (G + 1, G + 2): c.set(x, y, z, b.pick(FOOT) if edge else 'dark_oak_planks' if y == G + 2 else 'stone')
            if not edge and (x, z) != (3, 3): c.set(x, G + 2, z, 'iron_bars' if (x + z) % 2 else 'dark_oak_planks')
    for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6)):                                       # the winding frame
        for y in range(G + 3, G + 12): c.set(x, y, z, 'dark_oak_log[axis=y]')
    for y in (G + 6, G + 11):
        for k in range(1, 6):
            c.set(k, y, 0, 'stripped_dark_oak_log[axis=x]'); c.set(k, y, 6, 'stripped_dark_oak_log[axis=x]'); c.set(0, y, k, 'stripped_dark_oak_log[axis=z]'); c.set(6, y, k, 'stripped_dark_oak_log[axis=z]')
    for k in range(1, 5):
        for z in (0, 6): c.set(k, G + 6 + k, z, 'dark_oak_fence'); c.set(6 - k, G + 6 + k, z, 'dark_oak_fence') if k != 3 else None
    for z in range(0, 7): c.set(3, G + 12, z, 'dark_oak_log[axis=z]')
    for (dx, dy) in ((-1, 0), (1, 0), (0, 1), (0, -1), (-1, 1), (1, 1), (-1, -1), (1, -1)):   # the sheave wheel
        c.set(3 + dx, G + 14 + dy, 3, 'stripped_dark_oak_log[axis=z]' if dx and dy else 'dark_oak_planks')
    c.set(3, G + 14, 3, 'iron_block'); c.set(3, G + 12, 3, 'dark_oak_log[axis=z]')
    for y in range(G + 3, G + 12): c.set(3, y, 3, 'chain[axis=y]')
    c.set(3, G + 2, 3, 'iron_block')
    for x in range(7, 22): c.set(x, G + 1, 3, 'rail[shape=east_west]')                    # the tip road: a rail and its tubs
    for (x, blk) in ((9, 'composter'), (10, 'composter'), (15, 'composter')): c.set(x, G + 1, 3, blk)
    c.set(9, G + 2, 3, 'raw_iron_block'); c.set(15, G + 2, 3, 'coal_block')
    hut = hs(7, 7, floors=1, style='ash', door=3, seed=17).c                   # the winch hut
    xf.put(c, hut, 9, -12)
    for i in range(1, 6): c.set(6 + i, G + 11 - i, 0 - i, 'chain[axis=x]') if i < 5 else None
    return c


def slag(r=6, h=5, seed=1):
    c = C(); b = Build(seed=seed)
    for x in range(-r, r + 1):
        for z in range(-r, r + 1):
            d = (x * x + z * z) ** 0.5
            top = int(round(h * (1 - d / (r + 0.5)) + nz(x, z, 3, seed) * 0.4))
            for y in range(G + 1, G + 1 + max(0, top)): c.set(x, y, z, b.pick(SLAG))
    return c


def outwork():
    """The Outwork ruin by point A: a broken watch tower, 15 across, torn down to the south-east, with the stub of its
    upper floor, a fallen lump of its top, and long scraps of yard wall for cover."""
    c = C(); b = Build(seed=19); R = 7
    for x in range(-R, R + 1):
        for z in range(-R, R + 1):
            d = max(abs(x), abs(z))
            if abs(x) == R and abs(z) == R: continue
            tall = max(0, (-x - z) + 6)                                                   # highest at the north-west corner
            if d >= R - 1:
                if abs(x) <= 1 and z >= R - 1: continue                                    # the doorway, south
                top = min(14, 2 + tall + nz(x, z, 3)) if d == R else min(12, tall - 2 + nz(x, z, 2))
                for y in range(G + 1, G + 1 + max(0, top)):
                    c.set(x, y, z, b.pick(FOOT) if y <= G + 3 and d == R else 'polished_deepslate' if y == G + 7 and d == R else b.pick(RUIN))
            else:
                c.set(x, G, z, b.pick(RUIN))
                if tall >= 9 and d >= 2: c.set(x, G + 7, z, 'dark_oak_planks' if nz(x, z, 4) else 'spruce_planks')       # what is left of the floor
                if nz(x, z, 6) == 0 and abs(x) > 1 and tall < 9: c.set(x, G + 1, z, b.pick(RUIN))
    for (x, z) in ((-R, -2), (-2, -R), (-R, 2)):
        for y in (G + 4, G + 5, G + 10, G + 11): c.set(x, y, z, 'iron_bars') if c.get((x, y, z)) else None
    for y in range(G + 1, G + 8): c.set(-R + 2, y, -R + 2, 'dark_oak_log[axis=y]')
    for i in range(4):                                                                    # a fallen lump of the top, lying south-east
        for j in range(3):
            for k in range(2 - (i == 3)): c.set(10 + i, G + 1 + k, 4 + j, 'deepslate_bricks' if (i + j + k) % 2 else 'cracked_deepslate_bricks')
    for (x0, z0, dx, dz, n) in ((11, -8, 1, 0, 14), (11, -7, 0, 1, 7), (-22, 9, 1, 0, 13), (-22, 2, 0, 1, 7), (9, 12, 1, 0, 12), (-10, 13, 1, 0, 6)):   # yard walls
        for i in range(n):
            if i in (5, 6) and n > 10: continue                                           # a gap to pass through
            x, z = x0 + dx * i, z0 + dz * i; top = 2 + (nz(x, z, 3) > 0) + (nz(x, z, 4) == 0) - (i in (0, n - 1))
            for y in range(G + 1, G + 1 + top): c.set(x, y, z, b.pick(FOOT) if y == G + 1 else b.pick(RUIN))
    for (x, z, blk) in ((-18, 11, 'barrel[facing=east]'), (16, -5, 'dead_bush'), (-3, 11, 'cobbled_deepslate'), (14, 14, 'hay_block[axis=x]'), (-19, 11, 'barrel[facing=up]'), (13, -6, 'cobbled_deepslate')): c.set(x, G + 1, z, blk)
    c.set(16, G, -5, 'coarse_dirt')
    return c


def house_row():
    c = C(); ox = 0
    for (w, d, f, st, kw) in ((7, 11, 3, 'iron', {}), (11, 11, 2, 'ash', dict(shop=True)), (7, 11, 2, 'soot', dict(door=3)), (13, 13, 2, 'iron', dict(arch=True, door=2)), (11, 11, 2, 'iron', dict(door=2, ridge='z')), (7, 11, 3, 'ash', dict(door=5))):
        xf.put(c, hs(w, d, floors=f, style=st, seed=ox + 3, **kw).c, ox, 13 - d); ox += w + 3
    return c


ALL = None
PIECES = [('houses', house_row), ('forge', forge), ('smelter', smelter), ('stables', stables), ('store', store), ('barracks', barracks_yard),
          ('tents', lambda: {**tent(), **{(x + 10, y, z): v for (x, y, z), v in tent(9, 'light_gray').items()}, **{(x + 21, y, z): v for (x, y, z), v in pavilion().items()}}),
          ('trebuchet', trebuchet), ('wagons', lambda: {**wagon(0), **{(x + 10, y, z): v for (x, y, z), v in wagon(1).items()}}), ('palisade', palisade),
          ('minehead', mine_head), ('slag', lambda: {**slag(6, 5, 1), **{(x + 16, y, z): v for (x, y, z), v in slag(4, 3, 2).items()}}), ('outwork', outwork)]


if __name__ == '__main__':
    b = Build(seed=21); ox = oz = 0; rowd = 0; shots = []; spans = []
    only = os.environ.get('IH_ONLY'); PIECES = [p for p in PIECES if not only or p[0] in only.split(',')]
    for name, fn in PIECES:
        cells = fn(); xs = [k[0] for k in cells]; ys = [k[1] for k in cells]; zs = [k[2] for k in cells]
        w, d = max(xs) - min(xs) + 1, max(zs) - min(zs) + 1
        if ox and ox + w > 2000: ox = 0; oz += rowd + 12; rowd = 0
        dx, dz = ox - min(xs), oz - min(zs)
        for (x, y, z), v in cells.items(): b.set(x + dx, y, z + dz, v)
        cx, cz, top = ox + w / 2.0, oz + d / 2.0, max(ys)
        r = max(w, d) * 0.55 + 7
        shots.append('shot wk-%s %.1f %.1f %.1f %.1f %.1f %.1f' % (name, cx - r * 0.45, G + 135 + 4 + (top - G) * 0.6, oz + d + r, cx, G + 135 + (top - G) * 0.4, cz))
        if name in ('forge', 'smelter', 'barracks', 'tents', 'minehead', 'houses'):
            shots.append('shot wk-%s-b %.1f %.1f %.1f %.1f %.1f %.1f' % (name, cx + w * 0.3, G + 135 + 2.6, oz + d + 5, cx - w * 0.1, G + 135 + 2 + (top - G) * 0.2, cz))
        spans.append((name, ox, oz, w, d)); ox += w + 10; rowd = max(rowd, d)
    (x0, y0, z0), (x1, y1, z1) = b.bounds()
    for x in range(x0 - 5, x1 + 6):
        for z in range(z0 - 5, z1 + 6):
            if b.get(x, G, z) is None: b.set(x, G, z, b.pick(WARD))
            b.set(x, G - 1, z, 'stone')
    print('SPANS', spans); print(b.bounds())
    print(b.emit(sys.argv[1], 'ih-works', 'Ironhold buildings and field works, workshop sampler', pad=((x0 - 7, G - 2, z0 - 7), (x1 + 7, 110, z1 + 7))), 'ops')
    L = ['# Ironhold works sampler', 'note Ironhold works', 'mc time set 3000', 'mc execute as @a at @s run forceload add %d %d %d %d' % (x0 - 8, z0 - 8, x1 + 8, z1 + 8)]
    i = 0
    for xa in range(x0 - 5, x1 + 6, 47):
        for za in range(z0 - 5, z1 + 6, 47):
            n = 'ih_wk_%d' % i; i += 1
            print('%s %d %d %d %d %d %d' % (n, xa, G - 1, za, min(xa + 46, x1 + 5), min(G + 40, y1 + 1), min(za + 46, z1 + 5)))
            L.append('mc execute as @a at @s run place template ggmaps:rg/%s %d %d %d' % (n, xa, G - 1 + 135, za))
    if len(sys.argv) > 2: open(sys.argv[2], 'w', newline='\n').write('\n'.join(L + ['wait 10'] + shots) + '\n')
