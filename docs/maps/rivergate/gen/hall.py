"""K3 the market hall on the Eyot, in map coordinates: x 20..41, z -7..6, east of the B ring (radius 17, centre 0,0).
Open arcaded ground floor 4 high (troops walk through), council chamber above reached by a stone stair on the east
end, tile roof with a bell turret (ridge y 80). The Siege keep. build(b) adds it to a Build.
Usage: python hall.py OUT.json"""
import sys
from kit import Build, MASON, FOOT

G = 64
X0, X1, Z0, Z1 = 20, 41, -7, 6
PX = (20, 24, 28, 33, 37, 41)
PZ = (-7, -3, 2, 6)
STREET = [('cobblestone', 55), ('stone', 20), ('andesite', 20), ('mossy_cobblestone', 5)]
FLAGS = [('stone_bricks', 45), ('andesite', 25), ('polished_andesite', 20), ('cobblestone', 10)]


def build(b):
    F = G + 5                                             # chamber floor block
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            pier = edge and (x in PX if z in (Z0, Z1) else z in PZ)
            b.set(x, G, z, 'polished_andesite' if (x in PX or z in PZ) else b.pick(FLAGS))
            if pier:
                b.set(x, G + 1, z, b.pick(FOOT))
                for y in range(G + 2, F): b.set(x, y, z, b.pick(MASON))
            elif edge:                                    # arch springers beside each pier
                if z in (Z0, Z1):
                    if x - 1 in PX: b.set(x, F - 1, z, 'stone_brick_stairs[facing=west,half=top,shape=straight]')
                    if x + 1 in PX: b.set(x, F - 1, z, 'stone_brick_stairs[facing=east,half=top,shape=straight]')
                else:
                    if z - 1 in PZ: b.set(x, F - 1, z, 'stone_brick_stairs[facing=north,half=top,shape=straight]')
                    if z + 1 in PZ: b.set(x, F - 1, z, 'stone_brick_stairs[facing=south,half=top,shape=straight]')
            # inner posts under the floor
            if not edge and x in PX and z in (-3, 2):
                b.set(x, G + 1, z, 'stone_bricks')
                for y in range(G + 2, F): b.set(x, y, z, 'dark_oak_log[axis=y]')
            # floor: beams over the pier lines, boards between
            beam = x in PX or z in PZ
            b.set(x, F, z, ('stone_bricks' if edge and not pier else 'chiseled_stone_bricks' if pier else
                            'stripped_dark_oak_log[axis=%s]' % ('z' if x in PX else 'x') if beam else 'spruce_planks'))
            # chamber walls
            if edge:
                post = pier
                for y in range(F + 1, F + 4):
                    blk = 'dark_oak_log[axis=y]' if post else 'white_terracotta'
                    if y == F + 3: blk = 'stripped_dark_oak_log[axis=%s]' % ('x' if z in (Z0, Z1) else 'z')
                    b.set(x, y, z, blk)
    # brackets under the floor outside each pier, lamps under the beams
    for x in PX:
        b.set(x, F - 1, Z0 - 1, 'dark_oak_stairs[facing=south,half=top,shape=straight]')
        b.set(x, F - 1, Z1 + 1, 'dark_oak_stairs[facing=north,half=top,shape=straight]')
    for x in (26, 35):
        for z in (-5, 4): b.set(x, F - 1, z, 'lantern[hanging=true]')
    b.set(30, F - 1, 0, 'lantern[hanging=true]'); b.set(31, F - 1, -1, 'lantern[hanging=true]')
    # ground floor: the weigh beam and stores tucked against the east piers, the rest clear
    for x, z, blk in ((40, -6, 'barrel[facing=up]'), (40, -5, 'barrel[facing=up]'), (39, -6, 'barrel[facing=up]'), (40, 5, 'hay_block[axis=x]'),
                      (40, 4, 'hay_block[axis=z]'), (39, 5, 'composter[level=3]')):
        b.set(x, G + 1, z, blk)
    b.set(40, G + 2, -6, 'barrel[facing=up]'); b.set(40, G + 2, 5, 'hay_block[axis=x]')
    b.set(37, F - 1, 0, 'chain[axis=y]'); b.set(37, F - 2, 0, 'lantern[hanging=true]')
    # windows with shutters: two to a bay on the long sides, one each side of centre on the ends
    W = F + 2
    for x in (22, 26, 30, 31, 35, 39):
        for z, dz, f in ((Z0, -1, 'north'), (Z1, 1, 'south')):
            b.set(x, W, z, 'air')
            for k in (-1, 1):
                if x + k in (22, 26, 30, 31, 35, 39): continue
                b.set(x + k, W, z + dz, 'dark_oak_trapdoor[facing=%s,half=top,open=true]' % f)
    for z in (-5, 0, -1, 4):
        b.set(X0, W, z, 'air')
    for z in (-6, -4, 3, 5): b.set(X0 - 1, W, z, 'dark_oak_trapdoor[facing=west,half=top,open=true]')
    # roof with timbered gables
    b.gable_x(X0, X1, Z0, Z1, F + 4, 'brick_stairs', 'brick_slab', fill=[('white_terracotta', 1)])
    for x in (X0, X1):
        for y in range(F + 4, F + 11):
            for z in (-1, 0):
                if b.get(x, y, z) == 'white_terracotta': b.set(x, y, z, 'dark_oak_log[axis=y]')
        for z in range(Z0, Z1 + 1):
            if b.get(x, F + 7, z) == 'white_terracotta': b.set(x, F + 7, z, 'stripped_dark_oak_log[axis=z]')
        b.set(x, F + 5, -3, 'air'); b.set(x, F + 5, 2, 'air')
    for x in PX[1:-1]:
        b.box(x, F + 4, Z0 + 1, x, F + 4, Z1 - 1, 'dark_oak_log[axis=z]')
        b.set(x, F + 3, -1, 'lantern[hanging=true]')
    # dormers, two a side, to break the big roof
    for x in (24, 35):
        for zf, zs in ((Z0 + 1, (Z0, Z0 + 1, Z0 + 2)), (Z1 - 1, (Z1 - 2, Z1 - 1, Z1))):
            for dx in (0, 1, 2):
                b.set(x + dx, F + 7, zf, 'air' if dx == 1 else 'dark_oak_log[axis=y]')
                for z in zs: b.set(x + dx, F + 8, z, 'brick_slab[type=bottom]')
    # bell turret on the ridge
    R = F + 11
    for x in (30, 31):
        for z in (-1, 0):
            b.set(x, R, z, 'bricks')
            for y in (R + 1, R + 2): b.set(x, y, z, 'spruce_fence')
    for x in range(29, 33):
        for z in range(-2, 2): b.set(x, R + 3, z, 'brick_slab[type=bottom]')
    b.set(30, R + 3, -1, 'bricks'); b.set(31, R + 3, 0, 'bricks'); b.set(30, R + 3, 0, 'bricks'); b.set(31, R + 3, -1, 'bricks')
    b.set(30, R + 4, -1, 'brick_slab[type=bottom]'); b.set(31, R + 4, 0, 'brick_slab[type=bottom]'); b.set(30, R + 4, 0, 'brick_slab[type=bottom]'); b.set(31, R + 4, -1, 'brick_slab[type=bottom]')
    # chimney on the north side
    b.box(30, G, Z0 - 1, 31, F + 7, Z0 - 1, FOOT); b.box(30, F + 7, Z0 - 1, 31, F + 9, Z0 - 1, 'bricks'); b.set(30, F + 10, Z0 - 1, 'brick_wall'); b.set(31, F + 10, Z0 - 1, 'brick_wall')
    b.set(30, F + 1, Z0, 'campfire[lit=true]'); b.set(30, F + 1, Z0 + 1, 'iron_bars[east=true,west=true]'); b.set(30, F + 2, Z0, 'bricks'); b.set(29, F + 1, Z0, 'bricks'); b.set(31, F + 1, Z0, 'bricks'); b.set(31, F + 2, Z0, 'bricks')
    # outside stair on the east end, climbing north to a landing and a door
    for i, z in enumerate((5, 4, 3, 2, 1)):
        for x in (X1 + 1, X1 + 2):
            for y in range(G + 1, G + 1 + i): b.set(x, y, z, b.pick(MASON))
            b.set(x, G + 1 + i, z, 'stone_brick_stairs[facing=north,half=bottom,shape=straight]')
        b.set(X1 + 3, G + 2 + i, z, 'stone_brick_wall'); b.set(X1 + 3, G + 1 + i, z, b.pick(MASON))
        for y in range(G + 1, G + 1 + i): b.set(X1 + 3, y, z, b.pick(MASON))
    for z in (0, -1, -2):
        for x in (X1 + 1, X1 + 2, X1 + 3):
            for y in range(G + 1, F + 1): b.set(x, y, z, b.pick(MASON))
        b.set(X1 + 3, F + 1, z, 'stone_brick_wall')
    for x in (X1 + 1, X1 + 2, X1 + 3): b.set(x, F + 1, -3, 'stone_brick_wall')
    for x in (X1 + 1, X1 + 2, X1 + 3):
        for y in range(G + 1, F + 1): b.set(x, y, -3, b.pick(MASON))
    b.air(X1 + 1, F + 1, -2, X1 + 2, F + 2, 0)
    b.air(X1, F + 1, -1, X1, F + 2, 0)
    b.set(X1 + 3, F + 2, 0, 'lantern[hanging=false]')
    # the chamber: council table, benches, tally desk, chests, shelves
    C = F + 1
    for x in range(26, 36):
        b.set(x, C, -1, 'spruce_fence'); b.set(x, C + 1, -1, 'spruce_pressure_plate')
        if x % 2 == 0:
            b.set(x, C, -3, 'spruce_stairs[facing=north,half=bottom,shape=straight]'); b.set(x, C, 1, 'spruce_stairs[facing=south,half=bottom,shape=straight]')
    b.set(25, C, -1, 'dark_oak_stairs[facing=west,half=bottom,shape=straight]')        # the reeve's chair, back to the market
    for z in (-6, -5, 4, 5): b.set(21, C, z, 'bookshelf'); b.set(21, C + 1, z, 'bookshelf')
    b.set(22, C, 5, 'lectern[facing=north]'); b.set(22, C, -6, 'cartography_table')
    b.set(40, C, -6, 'chest[facing=west]'); b.set(40, C, -5, 'chest[facing=west]'); b.set(40, C, 5, 'barrel[facing=up]'); b.set(39, C, 5, 'barrel[facing=up]'); b.set(40, C + 1, 5, 'barrel[facing=up]')
    for x in (27, 34): b.set(x, C, 5, 'loom[facing=north]')
    for x in range(24, 38):
        for z in range(-4, 3):
            if b.get(x, C, z) is None and (x + z) % 2 == 0 and x in (24, 25, 36, 37): b.set(x, C, z, 'red_carpet')


if __name__ == '__main__':
    b = Build(seed=23)
    for x in range(8, 54):
        for z in range(-20, 20):
            for y in range(G - 2, G): b.set(x, y, z, 'stone')
            r = (x * x + z * z) ** 0.5
            b.set(x, G, z, 'smooth_stone' if 15 <= r <= 17 else b.pick(STREET))
    build(b)
    for p in ((14, 0), (30, 10)): b.box(p[0], 65, p[1], p[0], 66, p[1], 'red_wool')
    print(b.emit(sys.argv[1], 'rg-k3-hall', 'K3 market hall, workshop build', pad=((6, G - 2, -22), (56, 110, 22))), 'ops')
    print('k3_hall_ws 8 62 -20 53 86 19')
