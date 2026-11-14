"""Ironhold Great Keep in map coordinates, on the rock spur north of the Keep Court (C at 112, 0, court at y 76).
Spur top y 84 over x 98..146, z -84..-46. Donjon x 100..124, z -82..-58 to y 110, corner turrets, a tall north-west
beacon turret, a hall wing to the east, a terrace to the south and the great stair down to the court (x 108..116, z -45..-38).
Usage: python keep.py OUT.json [flip]"""
import sys, os
sys.path.append(os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'kingsfield', 'gen')))
from kit import Build
from gate import DEEP, FOOT, WARD

ROCK = [('stone', 32), ('deepslate', 26), ('tuff', 20), ('cobbled_deepslate', 14), ('andesite', 8)]
PAVE = [('polished_deepslate', 45), ('deepslate_tiles', 35), ('cobbled_deepslate', 20)]
TILE, TILE_SLAB = 'deepslate_tile_stairs', 'deepslate_tile_slab'
COURT, SPUR = 76, 84
SX0, SZ0, SX1, SZ1 = 98, -84, 146, -46                      # the spur top
DX0, DZ0, DX1, DZ1 = 100, -82, 124, -58                     # donjon outer faces
F1, F2, ROOF = 93, 101, 108                                 # floors above the hall, and the roof deck
HX0, HZ0, HX1, HZ1 = 125, -78, 144, -62                     # hall wing
STX0, STX1 = 108, 116                                       # great stair


def S(kind, f, top=False): return '%s[facing=%s,half=%s,shape=straight]' % (kind, f, 'top' if top else 'bottom')


def n3(x, y, z, n): return ((x * 73856093) ^ (y * 83492791) ^ (z * 19349663)) % n


def spur(b):
    for x in range(SX0 - 2, SX1 + 3):
        for z in range(SZ0 - 2, SZ1 + 1):
            out = max(SX0 - x, x - SX1, SZ0 - z, 0)                                    # rough skirt: 0 inside, 1..2 outside
            top = SPUR if out == 0 else SPUR - 3 - n3(x, 0, z, 4) if out == 1 else SPUR - 6 - n3(x, 1, z, 3)
            if out == 2 and n3(x, 2, z, 3): continue
            for y in range(COURT + 1, top + 1): b.set(x, y, z, b.pick(ROCK))
    for x in range(SX0, SX1 + 1):                                                      # terrace paving and its parapet
        for z in range(DZ1 + 1, SZ1 + 1): b.set(x, SPUR, z, b.pick(PAVE))
        if not STX0 <= x <= STX1:
            b.set(x, SPUR + 1, SZ1, b.pick(DEEP))
            if x % 2 == 0: b.set(x, SPUR + 2, SZ1, b.pick(DEEP))
    for z in range(DZ1 + 1, SZ1 + 1):
        for x in (SX0, SX1):
            b.set(x, SPUR + 1, z, b.pick(DEEP))
            if z % 2 == 0: b.set(x, SPUR + 2, z, b.pick(DEEP))
    for i, z in enumerate(range(SZ1 + 1, SZ1 + 9)):                                    # the great stair: 8 full-block steps, 9 wide
        y = SPUR - 1 - i
        for x in range(STX0 - 1, STX1 + 2):
            side = x in (STX0 - 1, STX1 + 1)
            for yy in range(COURT + 1, y + 1): b.set(x, yy, z, b.pick(DEEP) if side or yy < y else 'polished_deepslate' if (x + z) % 2 else 'deepslate_tiles')
            if side: b.set(x, y + 1, z, b.pick(DEEP)); b.set(x, y + 2, z, 'deepslate_brick_wall') if i % 2 == 0 else None
    for x in (STX0 - 1, STX1 + 1):                                                     # newel piers with lamps, top and bottom
        for z, y0 in ((SZ1, SPUR), (SZ1 + 9, COURT)):
            for y in range(y0 + 1, y0 + 4): b.set(x, y, z, 'polished_blackstone_bricks')
            b.set(x, y0 + 4, z, 'chiseled_polished_blackstone'); b.set(x, y0 + 5, z, 'lantern[hanging=false]')


def turret(b, cx, cz, top, spire=False, flag='purple'):
    r = 3
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            corner = abs(x - cx) == r and abs(z - cz) == r
            edge = abs(x - cx) == r or abs(z - cz) == r
            for y in range(SPUR + 1, top + 1):
                if corner and y > SPUR + 4: b.set(x, y, z, 'air'); continue
                b.set(x, y, z, b.pick(FOOT) if y <= SPUR + 4 else 'polished_deepslate' if edge and y in (F1, F2, ROOF, top - 1) else b.pick(DEEP))
    for y in range(F2 + 2, top - 2, 7):                                                 # loops
        for (dx, dz) in ((-r, 0), (r, 0), (0, -r), (0, r)):
            b.set(cx + dx, y, cz + dz, 'iron_bars'); b.set(cx + dx, y + 1, cz + dz, 'iron_bars')
    for x in range(cx - r - 1, cx + r + 2):                                             # corbelled top
        for z in range(cz - r - 1, cz + r + 2):
            if not (abs(x - cx) == r + 1 or abs(z - cz) == r + 1): continue
            corner = abs(x - cx) == r + 1 and abs(z - cz) == r + 1
            f = 'east' if x == cx - r - 1 else 'west' if x == cx + r + 1 else 'south' if z == cz - r - 1 else 'north'
            b.set(x, top, z, 'polished_deepslate' if corner else S('deepslate_brick_stairs', f, True)); b.set(x, top + 1, z, b.pick(DEEP))
            if (x + z) % 2 == 0 or corner: b.set(x, top + 2, z, b.pick(DEEP))
    if spire:
        for i in range(0, 3): b.box(cx - r, top + 1 + i, cz - r, cx + r, top + 1 + i, cz + r, DEEP, hollow=False) if i == 0 else None
        b.pyramid(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, top + 3, TILE, TILE_SLAB)
        for y in range(top + 7, top + 16): b.set(cx, y, cz, 'dark_oak_fence')
        b.set(cx, top + 7, cz, 'polished_blackstone'); b.set(cx, top + 16, cz, 'lightning_rod[facing=up]')
        for i in range(1, 7):
            for y in range(top + 11, top + 16):
                if not (i == 6 and y == top + 13): b.set(cx + i, y, cz, 'black_wool' if y == top + 13 else flag + '_wool')
    else:
        b.set(cx, top + 1, cz, 'polished_blackstone'); b.set(cx, top + 2, cz, 'iron_bars'); b.set(cx, top + 3, cz, 'lightning_rod[facing=up]')


def donjon(b, flag='purple'):
    for x in range(DX0, DX1 + 1):
        for z in range(DZ0, DZ1 + 1):
            ex, ez = min(x - DX0, DX1 - x), min(z - DZ0, DZ1 - z); shell = min(ex, ez) < 2; face = min(ex, ez) == 0
            for y in range(SPUR, ROOF + 1):
                if shell: b.set(x, y, z, b.pick(FOOT) if y <= SPUR + 3 and face else 'polished_deepslate' if face and y in (F1, F2, ROOF) else b.pick(DEEP))
                elif y == SPUR: b.set(x, y, z, 'purple_terracotta' if 111 <= x <= 113 and z > DZ0 + 5 else 'polished_deepslate' if (x + z) % 2 else 'deepslate_tiles')
                elif y in (F1, F2): b.set(x, y, z, 'dark_oak_planks' if (x // 3 + z) % 5 else 'spruce_planks')
                elif y == ROOF: b.set(x, y, z, b.pick(PAVE))
                else: b.set(x, y, z, 'air')
    for x in range(DX0 - 1, DX1 + 2):                                                    # battered plinth, one course out
        for z in range(DZ0 - 1, DZ1 + 2):
            if x in (DX0 - 1, DX1 + 1) or z in (DZ0 - 1, DZ1 + 1):
                if z == DZ1 + 1 and 110 <= x <= 114: continue
                b.set(x, SPUR + 1, z, b.pick(FOOT)); b.set(x, SPUR + 2, z, b.pick(FOOT))
                f = 'east' if x == DX0 - 1 else 'west' if x == DX1 + 1 else 'south' if z == DZ0 - 1 else 'north'
                b.set(x, SPUR + 3, z, S('polished_blackstone_brick_stairs', f))
    for k in range(DX0 + 6, DX1 - 5, 6):                                                 # pilaster strips and the windows between them
        for (x, z, f) in ((k, DZ0 - 1, 'south'), (k, DZ1 + 1, 'north'), (DX0 - 1, k - DX0 + DZ0, 'east'), (DX1 + 1, k - DX0 + DZ0, 'west')):
            if z == DZ1 + 1 and k == 112: continue
            for y in range(SPUR + 1, ROOF - 1): b.set(x, y, z, b.pick(FOOT) if y <= SPUR + 3 else b.pick(DEEP))
            b.set(x, ROOF - 1, z, S('deepslate_brick_stairs', f))
    for k in range(DX0 + 3, DX1 - 2, 6):
        for (x, z, x2, z2) in ((k, DZ0, k, DZ0 + 1), (k, DZ1, k, DZ1 - 1), (DX0, k - DX0 + DZ0, DX0 + 1, k - DX0 + DZ0), (DX1, k - DX0 + DZ0, DX1 - 1, k - DX0 + DZ0)):
            if x == DX1 and HZ0 <= z <= HZ1: continue                                   # the hall wing covers this face low down
            for y in (SPUR + 4, SPUR + 5, SPUR + 6, F1 + 3, F1 + 4, F2 + 2, F2 + 3, F2 + 4):
                b.set(x, y, z, 'iron_bars'); b.set(x2, y, z2, 'air')
    for x in range(DX0 - 1, DX1 + 2):                                                    # machicolated parapet
        for z in range(DZ0 - 1, DZ1 + 2):
            if not (x in (DX0 - 1, DX1 + 1) or z in (DZ0 - 1, DZ1 + 1)): continue
            f = 'east' if x == DX0 - 1 else 'west' if x == DX1 + 1 else 'south' if z == DZ0 - 1 else 'north'
            b.set(x, ROOF, z, S('deepslate_brick_stairs', f, True)); b.set(x, ROOF + 1, z, b.pick(DEEP))
            if ((x + z) // 2) % 2 == 0: b.set(x, ROOF + 2, z, b.pick(DEEP))
    b.box(DX0 + 6, ROOF + 1, DZ0 + 6, DX1 - 6, ROOF + 1, DZ1 - 6, DEEP)                  # a low tiled roof within the wall-walk
    b.pyramid(DX0 + 5, DZ0 + 5, DX1 - 5, DZ1 - 5, ROOF + 2, TILE, TILE_SLAB)
    turret(b, DX0, DZ1, ROOF + 8); turret(b, DX1, DZ1, ROOF + 8); turret(b, DX1, DZ0, ROOF + 8)
    turret(b, DX0, DZ0, ROOF + 18, spire=True, flag=flag)
    # the great door, south face
    for x in range(110, 115):
        for z in (DZ1, DZ1 - 1, DZ1 + 1):
            for y in range(SPUR + 1, SPUR + 7):
                inside = 111 <= x <= 113 and y <= SPUR + 5
                if inside: b.set(x, y, z, 'air')
                elif z >= DZ1: b.set(x, y, z, 'chiseled_polished_blackstone' if y == SPUR + 6 and x == 112 else 'polished_blackstone_bricks')
    b.set(111, SPUR + 5, DZ1, S('polished_blackstone_brick_stairs', 'east', True)); b.set(113, SPUR + 5, DZ1, S('polished_blackstone_brick_stairs', 'west', True))
    for x in (111, 112, 113): b.set(x, SPUR + 5, DZ1 - 1, 'iron_bars[east=true,west=true]')
    for x in (109, 115):
        b.set(x, SPUR + 7, DZ1 + 1, flag + '_wall_banner[facing=south]'); b.set(x, SPUR + 5, DZ1 + 1, 'air')
    for x in (108, 116):
        b.set(x, SPUR + 1, DZ1 + 3, 'polished_blackstone_brick_wall'); b.set(x, SPUR + 2, DZ1 + 3, 'polished_blackstone'); b.set(x, SPUR + 3, DZ1 + 3, 'lantern[hanging=false]')
    for x in (111, 112, 113): b.set(x, F2 + 5, DZ1, 'chiseled_deepslate')
    b.set(112, F2 + 2, DZ1 + 1, flag + '_wall_banner[facing=south]')


def hall_wing(b):
    for x in range(HX0, HX1 + 1):
        for z in range(HZ0, HZ1 + 1):
            edge = x == HX1 or z in (HZ0, HZ1)
            for y in range(SPUR, SPUR + 9):
                if edge: b.set(x, y, z, b.pick(FOOT) if y <= SPUR + 2 else 'polished_deepslate' if y == SPUR + 8 else b.pick(DEEP))
                elif y == SPUR: b.set(x, y, z, 'dark_oak_planks' if (x + z // 2) % 4 else 'spruce_planks')
                else: b.set(x, y, z, 'air')
    b.gable_x(HX0, HX1, HZ0, HZ1, SPUR + 9, TILE, TILE_SLAB, fill=DEEP, over=1)
    for x in range(HX0, HX1 + 1):                                                        # roof trusses
        if (x - HX0) % 5 == 2:
            for z in range(HZ0 + 1, HZ1): b.set(x, SPUR + 8, z, 'dark_oak_log[axis=z]')
            b.set(x, SPUR + 7, HZ0 + 1, S('dark_oak_stairs', 'south', True)); b.set(x, SPUR + 7, HZ1 - 1, S('dark_oak_stairs', 'north', True))
            b.set(x, SPUR + 7, (HZ0 + HZ1) // 2, 'chain[axis=y]'); b.set(x, SPUR + 6, (HZ0 + HZ1) // 2, 'lantern[hanging=true]')
    for x in range(HX0 + 3, HX1 - 1, 4):
        for z in (HZ0, HZ1):
            for y in (SPUR + 3, SPUR + 4, SPUR + 5): b.set(x, y, z, 'iron_bars')
    for y in (SPUR + 1, SPUR + 2, SPUR + 3):                                             # doors: from the terrace, and through into the donjon
        b.set(134, y, HZ1, 'air'); b.set(135, y, HZ1, 'air')
        for x in (DX1, DX1 - 1): b.set(x, y, -70, 'air'); b.set(x, y, -71, 'air')
    b.set(134, SPUR + 4, HZ1, S('deepslate_brick_stairs', 'east', True)); b.set(135, SPUR + 4, HZ1, S('deepslate_brick_stairs', 'west', True))
    for y in range(SPUR + 1, SPUR + 18):                                                 # the kitchen chimney on the east gable
        for x in (HX1, HX1 + 1):
            for z in (-71, -70, -69): b.set(x, y, z, b.pick(DEEP) if y < SPUR + 16 else 'polished_blackstone_bricks')
    b.set(HX1, SPUR + 17, -70, 'air'); b.set(HX1 + 1, SPUR + 17, -70, 'air')
    for y in (SPUR + 1, SPUR + 2): b.set(HX1, y, -70, 'air')
    b.set(HX1, SPUR + 1, -70, 'campfire[lit=false]'); b.set(HX1 - 1, SPUR + 3, -70, S('polished_blackstone_brick_stairs', 'east', True))
    # furniture: two long tables, benches, stores by the hearth
    for x in range(HX0 + 3, HX1 - 5):
        for z in (-73, -67):
            b.set(x, SPUR + 1, z, 'dark_oak_fence' if (x - HX0) % 3 == 0 else 'air'); b.set(x, SPUR + 1, z, 'dark_oak_slab[type=top]')
            b.set(x, SPUR + 1, z - 1, S('spruce_stairs', 'south')) if (x - HX0) % 4 else None
            b.set(x, SPUR + 1, z + 1, S('spruce_stairs', 'north')) if (x - HX0) % 4 != 2 else None
    for (x, z, blk) in ((HX1 - 1, HZ0 + 1, 'barrel[facing=up]'), (HX1 - 2, HZ0 + 1, 'barrel[facing=up]'), (HX1 - 1, HZ0 + 2, 'barrel[facing=up]'), (HX1 - 1, HZ1 - 1, 'smoker[facing=west,lit=false]'),
                        (HX1 - 2, HZ1 - 1, 'cauldron'), (HX1 - 3, HZ1 - 1, 'crafting_table'), (HX0 + 1, HZ0 + 1, 'hay_block[axis=y]'), (HX0 + 1, HZ1 - 1, 'composter')):
        b.set(x, SPUR + 1, z, blk)
    b.set(HX1 - 1, SPUR + 2, HZ0 + 1, 'barrel[facing=up]')


def inside(b, flag='purple'):
    IX0, IZ0, IX1, IZ1 = DX0 + 2, DZ0 + 2, DX1 - 2, DZ1 - 2
    # hall: pillars, dais and high seat, tables, hearth
    for (x, z) in ((106, -76), (118, -76), (106, -66), (118, -66)):
        for y in range(SPUR + 1, F1): b.set(x, y, z, 'chiseled_deepslate' if y in (SPUR + 1, F1 - 1) else 'polished_deepslate')
        for (dx, dz, f) in ((1, 0, 'west'), (-1, 0, 'east'), (0, 1, 'north'), (0, -1, 'south')): b.set(x + dx, F1 - 1, z + dz, S('deepslate_brick_stairs', f, True))
    for x in range(108, 117):
        for z in (IZ0 + 2, IZ0 + 3, IZ0 + 4): b.set(x, SPUR + 1, z, 'polished_blackstone' if (x + z) % 2 else 'polished_blackstone_bricks')
    b.set(112, SPUR + 2, IZ0 + 2, S('dark_oak_stairs', 'north')); b.set(111, SPUR + 2, IZ0 + 2, 'polished_blackstone_brick_wall'); b.set(113, SPUR + 2, IZ0 + 2, 'polished_blackstone_brick_wall')
    for x in (110, 112, 114): b.set(x, SPUR + 5, IZ0 + 2, flag + '_wall_banner[facing=south]') if False else None
    for x in (109, 115): b.set(x, SPUR + 2, IZ0 + 2, 'polished_blackstone'); b.set(x, SPUR + 3, IZ0 + 2, 'lantern[hanging=false]')
    for z in range(-73, -64):
        for x in (108, 116):
            b.set(x, SPUR + 1, z, 'dark_oak_slab[type=top]')
            if z % 3: b.set(x + (-1 if x == 108 else 1), SPUR + 1, z, S('spruce_stairs', 'east' if x == 108 else 'west'))
    for z in (-71, -70, -69):                                                            # hearth in the west wall
        for y in (SPUR + 1, SPUR + 2, SPUR + 3): b.set(IX0 - 1, y, z, 'air' if y < SPUR + 3 and z == -70 else 'polished_blackstone_bricks')
    b.set(IX0 - 1, SPUR + 1, -70, 'campfire[lit=false]'); b.set(IX0, SPUR + 4, -70, 'chiseled_polished_blackstone') if False else None
    for (x, z) in ((112, -70), (106, -71), (118, -71)): b.set(x, F1 - 1, z, 'chain[axis=y]'); b.set(x, F1 - 2, z, 'lantern[hanging=true]')
    # the ladder well, north-east: up through both floors to the roof
    lx, lz = IX1 - 5, IZ0
    for y in range(SPUR + 1, ROOF + 2): b.set(lx, y, lz, 'ladder[facing=south]')
    b.set(lx, ROOF + 1, lz, 'air'); b.set(lx, ROOF, lz, 'ladder[facing=south]')
    # first floor: guard room and armoury
    for i in range(4):
        z = IZ0 + 5 + i * 3
        b.set(IX0, F1 + 1, z, 'purple_bed[facing=west,part=head]'); b.set(IX0 + 1, F1 + 1, z, 'purple_bed[facing=west,part=foot]'); b.set(IX0, F1 + 1, z + 1, 'barrel[facing=up]')
    for (x, z, blk) in ((IX1, -75, 'anvil[facing=north]'), (IX1, -73, 'grindstone[face=floor,facing=north]'), (IX1, -71, 'smithing_table'), (IX1, -69, 'barrel[facing=up]'), (IX1, -68, 'barrel[facing=up]'),
                        (IX1 - 1, -68, 'barrel[facing=up]'), (112, IZ1, 'fletching_table'), (113, IZ1, 'barrel[facing=up]'), (110, IZ1, 'chest[facing=north]'), (IX1, IZ1 - 4, 'iron_block'), (IX1, IZ1 - 5, 'coal_block')):
        b.set(x, F1 + 1, z, blk)
    b.set(IX1, F1 + 2, -68, 'barrel[facing=up]')
    for x in range(109, 116): b.set(x, F1 + 1, -70, 'dark_oak_slab[type=top]')
    for x in (110, 112, 114): b.set(x, F1 + 1, -71, S('spruce_stairs', 'south')); b.set(x, F1 + 1, -69, S('spruce_stairs', 'north'))
    for (x, z) in ((106, -70), (118, -70)): b.set(x, F2 - 1, z, 'chain[axis=y]'); b.set(x, F2 - 2, z, 'lantern[hanging=true]')
    # second floor: the castellan's room
    b.set(IX0 + 5, F2 + 1, IZ1 - 1, 'purple_bed[facing=south,part=foot]'); b.set(IX0 + 5, F2 + 1, IZ1, 'purple_bed[facing=south,part=head]')
    for x in range(IX0 + 7, IX0 + 12):
        for y in (F2 + 1, F2 + 2, F2 + 3): b.set(x, y, IZ1, 'bookshelf' if (x + y) % 4 else 'chiseled_bookshelf[facing=north]')
    for (x, z, blk) in ((112, -70, 'cartography_table'), (113, -70, 'dark_oak_slab[type=top]'), (111, -70, 'dark_oak_slab[type=top]'), (112, -72, S('dark_oak_stairs', 'north')), (IX1, IZ1, 'chest[facing=west]'),
                        (IX1, IZ1 - 2, 'lectern[facing=west]'), (IX0, IZ1 - 6, 'barrel[facing=up]'), (IX1, -70, 'loom[facing=west]')):
        b.set(x, F2 + 1, z, blk)
    b.set(113, F2 + 2, -70, 'candle[candles=3,lit=false]')
    for (x, z) in ((108, -66), (116, -74)): b.set(x, ROOF - 1, z, 'chain[axis=y]'); b.set(x, ROOF - 2, z, 'lantern[hanging=true]')
    b.set(lx, ROOF + 1, lz + 1, 'air')
    # more light and gear upstairs (Kort, 2026-11-13)
    for (fl, top) in ((F1, F2), (F2, ROOF)):
        for (x, z) in ((106, -76), (112, -76), (106, -64), (112, -64), (118, -64), (118, -74)):
            b.set(x, top - 1, z, 'chain[axis=y]'); b.set(x, top - 2, z, 'lantern[hanging=true]')
    for x in range(105, 112):                                                            # F1: weapon rack on the north wall, drill dummies, crates
        b.set(x, F1 + 1, IZ0, 'dark_oak_fence' if x % 2 else 'iron_bars[east=true,west=true]'); b.set(x, F1 + 2, IZ0, 'dark_oak_fence' if x % 2 else 'iron_bars[east=true,west=true]')
        b.set(x, F1 + 3, IZ0, 'dark_oak_slab[type=bottom]')
    for (x, z) in ((119, -63), (116, -62)):
        b.set(x, F1 + 1, z, 'dark_oak_fence'); b.set(x, F1 + 2, z, 'hay_block[axis=y]'); b.set(x, F1 + 3, z, 'carved_pumpkin[facing=north]') if False else None
    b.set(IX1, F1 + 2, -64, 'target'); b.set(IX1, F1 + 1, -64, 'dark_oak_fence')
    for (x, z, h) in ((105, IZ1, 2), (106, IZ1, 1), (107, IZ1, 2), (108, IZ1 - 1, 1), (116, IZ1, 2), (117, IZ1, 1), (IX0, IZ1 - 3, 2), (IX0 + 1, IZ1 - 3, 1)):
        for i in range(h): b.set(x, F1 + 1 + i, z, 'barrel[facing=up]' if (x + i) % 2 else 'spruce_planks')
    for i in range(3):
        z = -76 + i * 3
        b.set(IX1 - 3, F1 + 1, z, 'purple_bed[facing=west,part=foot]') if False else None
    for x in range(109, 116): b.set(x, F1 + 2, -70, 'air')
    b.set(110, F1 + 2, -70, 'lantern[hanging=false]'); b.set(114, F1 + 2, -70, 'candle[candles=2,lit=false]')
    # F2: a timber partition makes a bedchamber (west) and a map room (east)
    for z in range(IZ0, IZ1 + 1):
        for y in range(F2 + 1, ROOF):
            gap = -71 <= z <= -69 and y <= F2 + 3
            if not gap: b.set(109, y, z, 'stripped_dark_oak_log[axis=y]' if z % 4 == 0 else 'dark_oak_planks' if y != F2 + 4 else 'stripped_dark_oak_log[axis=z]')
    for z in (IZ1 - 6, IZ1 - 5): b.set(IX0, F2 + 1, z, 'barrel[facing=up]'); b.set(IX0, F2 + 2, z, 'barrel[facing=up]')
    for z in (-71, -70, -69):
        for y in (F2 + 1, F2 + 2, F2 + 3): b.set(IX0 - 1, y, z, 'air' if y < F2 + 3 and z == -70 else 'polished_blackstone_bricks')
    b.set(IX0 - 1, F2 + 1, -70, 'campfire[lit=false]')
    b.set(IX0 + 3, F2 + 1, -73, S('dark_oak_stairs', 'west')); b.set(IX0 + 3, F2 + 1, -67, 'potted_dead_bush'); b.set(IX0 + 3, F2 + 1, -67, 'dark_oak_slab[type=top]'); b.set(IX0 + 3, F2 + 2, -67, 'candle[candles=3,lit=false]')
    b.set(IX0 + 4, F2 + 1, IZ1, 'chest[facing=north]'); b.set(IX0 + 3, F2 + 1, IZ1, 'chest[facing=north]')
    for x in range(110, 115):                                                            # the map table and its floor
        pass                                                                             # (a floor inlay here showed as a dark patch in the ceiling below; removed)
    for x in (111, 112, 113):
        for z in (-71, -70, -69): b.set(x, F2 + 1, z, 'cartography_table' if (x, z) == (112, -70) else 'dark_oak_slab[type=top]')
    b.set(112, F2 + 1, -72, 'air'); b.set(113, F2 + 2, -70, 'air'); b.set(111, F2 + 2, -71, 'candle[candles=3,lit=false]'); b.set(113, F2 + 2, -69, 'lantern[hanging=false]')
    for x in (112, 114, 116): b.set(x, F2 + 3, IZ0, flag + '_wall_banner[facing=south]')
    for x in range(111, 117):
        for y in (F2 + 1, F2 + 2, F2 + 3): b.set(x, y, IZ1, 'bookshelf' if (x * 3 + y) % 5 else 'chiseled_bookshelf[facing=north]')
    for (x, z, blk) in ((IX1, -75, 'barrel[facing=up]'), (IX1, -74, 'chest[facing=west]'), (IX1, -66, 'smithing_table'), (IX1, -65, 'barrel[facing=up]'), (118, IZ1, 'lectern[facing=north]')):
        b.set(x, F2 + 1, z, blk)


def build(b, flag='purple'):
    spur(b); donjon(b, flag); hall_wing(b); inside(b, flag)


def site(b):
    for x in range(94, 151):
        for z in range(-88, -33):
            for y in range(65, COURT): b.set(x, y, z, 'stone')
            b.set(x, COURT, z, b.pick(WARD))


if __name__ == '__main__':
    b = Build(seed=95)
    site(b); build(b)
    if len(sys.argv) > 2:
        sw = lambda v: v.replace('facing=east', '#').replace('facing=west', 'facing=east').replace('#', 'facing=west')
        b.c = {(244 - x, y, z): sw(v) for (x, y, z), v in b.c.items()}
    print(len(b.c), b.bounds())
    print(b.emit(sys.argv[1], 'ih-keep', 'Great Keep, workshop build', pad=((92, 60, -90), (152, 150, -31))), 'ops')
    if len(sys.argv) == 2:
        for (n, xa, xb) in (('w', 95, 122), ('e', 123, 149)):
            for (m, za, zb) in (('n', -87, -62), ('s', -61, -36)):
                print('ih_keep_%s%s1 %d 64 %d %d 110 %d' % (n, m, xa, za, xb, zb)); print('ih_keep_%s%s2 %d 111 %d %d 148 %d' % (n, m, xa, za, xb, zb))
