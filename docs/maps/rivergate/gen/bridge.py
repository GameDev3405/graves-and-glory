"""K4 the north bridge and its gatehouse, in map coordinates (the south one is the z mirror).
Bridge: x -8..7, z -50..-31, deck y 64, 14 clear between parapets. Gatehouse on the bank end: x -10..9, z -58..-51,
open passage x -4..3 (8 wide, 4 high), guardroom each side, one long chamber above, slate roof (ridge y 77).
build() returns the Build. As a script: writes a workshop plan with stand-in water and banks.
Usage: python bridge.py OUT.json"""
import sys
from kit import Build, MASON, FOOT

G, BED, WATER = 64, 59, 62
PIERS = (-45, -44, -38, -37)
FLAGS = [('stone_bricks', 50), ('andesite', 25), ('polished_andesite', 15), ('cobblestone', 10)]
STREET = [('cobblestone', 55), ('stone', 20), ('andesite', 20), ('mossy_cobblestone', 5)]


def bridge(b):
    for z in range(-50, -30):
        pier = z in PIERS
        end = z in (-50, -31)
        for x in range(-8, 8):
            b.set(x, G, z, 'cobblestone' if x in (-3, 2) else b.pick(MASON if abs(x + 0.5) > 5 else STREET))
            if pier or end:
                for y in range(BED, G): b.set(x, y, z, b.pick(MASON if y > WATER else FOOT))
            elif z + 1 in PIERS or z + 1 == -31:
                b.set(x, G - 1, z, 'stone_brick_stairs[facing=south,half=top,shape=straight]')
            elif z - 1 in PIERS or z - 1 == -50:
                b.set(x, G - 1, z, 'stone_brick_stairs[facing=north,half=top,shape=straight]')
            else:
                b.set(x, G - 1, z, 'stone_brick_slab[type=top]')
        for x, sx in ((-8, -1), (7, 1)):
            post = z in (-50, -44, -38, -31)
            b.set(x, G + 1, z, b.pick(MASON) if post else 'stone_brick_wall')
            if post:
                b.set(x, G + 2, z, 'stone_brick_wall' if z in (-44, -38) else 'stone_brick_slab[type=bottom]')
                if z in (-44, -38): b.set(x, G + 3, z, 'lantern[hanging=false]')
            if pier:                                      # cutwaters, pointed up and down stream
                for y in range(BED, G): b.set(x + sx, y, z, b.pick(MASON if y > WATER else FOOT))
                b.set(x + sx, G, z, 'stone_brick_slab[type=bottom]')
                for y in range(BED, G - 1): b.set(x + 2 * sx, y, z, b.pick(FOOT))
                b.set(x + 2 * sx, G - 1, z, 'cobblestone_slab[type=bottom]')
    for z in (-47, -41, -34):                              # keystones on both faces
        for x in (-8, 7): b.set(x, G, z, 'chiseled_stone_bricks')


def gatehouse(b):
    x0, x1, z0, z1 = -10, 9, -58, -51
    P0, P1 = -4, 3                                         # passage
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, b.pick(STREET) if P0 <= x <= P1 else b.pick(FLAGS))
            wall = (x in (x0, x1) or z in (z0, z1) or x in (P0 - 1, P1 + 1)) and not P0 <= x <= P1
            if wall:
                for y in range(G + 1, G + 5): b.set(x, y, z, b.pick(FOOT) if y == G + 1 else b.pick(MASON))
            # floor of the chamber, stone string course outside
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, G + 5, z, ('chiseled_stone_bricks' if (x - x0) % 4 == 1 else 'stone_bricks') if edge else 'spruce_planks')
            # timber chamber
            if edge:
                post = (x in (x0, x1) and z in (z0, z1)) or (z in (z0, z1) and (x - x0) % 3 == 0) or (x in (x0, x1) and (z - z0) in (3, 4))
                for y in range(G + 6, G + 9):
                    blk = 'dark_oak_log[axis=y]' if post else 'white_terracotta'
                    if y == G + 8: blk = 'stripped_dark_oak_log[axis=%s]' % ('x' if z in (z0, z1) else 'z')
                    b.set(x, y, z, blk)
    # arch ring on both faces, raised portcullis on the field face
    for z in (z0, z1):
        b.set(P0, G + 4, z, 'stone_brick_stairs[facing=west,half=top,shape=straight]')
        b.set(P1, G + 4, z, 'stone_brick_stairs[facing=east,half=top,shape=straight]')
    for x in range(P0 + 1, P1): b.set(x, G + 4, z0, 'iron_bars[east=true,west=true]')
    for x in range(P0, P1 + 1):
        for z in (z0, z1): b.set(x, G + 5, z, 'chiseled_stone_bricks' if x in (-1, 0) else 'stone_bricks')
    # buttresses beside the passage on both faces
    for x in (P0 - 1, P1 + 1):
        for z, f in ((z0 - 1, 'south'), (z1 + 1, 'north')):
            for y in range(G + 1, G + 4): b.set(x, y, z, b.pick(MASON))
            b.set(x, G + 4, z, 'stone_brick_stairs[facing=%s,half=bottom,shape=straight]' % f)
    # arrow slits, guardroom doors, passage lamps
    for x in (-8, 7):
        for z in (z0, z1): b.air(x, G + 2, z, x, G + 3, z)
    b.air(P0 - 1, G + 1, -55, P0 - 1, G + 2, -55)
    b.air(P1 + 1, G + 1, -54, P1 + 1, G + 2, -54)
    for x, z in ((-1, -56), (0, -53)): b.set(x, G + 4, z, 'lantern[hanging=true]')
    # west guardroom: bunk and stores
    Y = G + 1
    b.set(-9, Y, -53, 'red_bed[facing=north,part=foot]'); b.set(-9, Y, -54, 'red_bed[facing=north,part=head]')
    b.set(-9, Y, -57, 'barrel[facing=up]'); b.set(-8, Y, -57, 'barrel[facing=up]'); b.set(-9, Y + 1, -57, 'lantern[hanging=false]')
    b.set(-9, Y, -56, 'chest[facing=east]'); b.set(-6, Y, -57, 'grindstone[face=floor,facing=north]')
    b.set(-6, Y, -52, 'spruce_fence'); b.set(-6, Y + 1, -52, 'spruce_pressure_plate'); b.set(-7, Y, -52, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
    # east guardroom: ladder to the chamber, table, stores
    for y in range(Y, G + 6): b.set(8, y, -57, 'ladder[facing=west]')
    b.set(8, Y, -52, 'chest[facing=west]'); b.set(8, Y, -53, 'barrel[facing=up]'); b.set(8, Y + 1, -53, 'barrel[facing=up]')
    b.set(6, Y, -52, 'spruce_fence'); b.set(6, Y + 1, -52, 'spruce_pressure_plate'); b.set(5, Y, -52, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
    b.set(5, Y, -57, 'hay_block[axis=x]')
    # chamber windows with shutters
    C = G + 6
    for x in (-7, -2, 1, 6):
        for z, dz, f in ((z0, -1, 'north'), (z1, 1, 'south')):
            b.set(x, C + 1, z, 'air')
            for k in (-1, 1): b.set(x + k, C + 1, z + dz, 'dark_oak_trapdoor[facing=%s,half=top,open=true]' % f)
    # roof, tie beams with lamps
    b.gable_x(x0, x1, z0, z1, G + 9, 'deepslate_tile_stairs', 'deepslate_tile_slab', fill=[('white_terracotta', 1)])
    for x in (-4, 3):
        b.box(x, G + 9, z0 + 1, x, G + 9, z1 - 1, 'dark_oak_log[axis=z]')
        b.set(x, G + 8, -55, 'lantern[hanging=true]')
    # cross gable over the passage, jettied one block out on both faces and carried on the buttresses
    for x in (P0 - 1, P1 + 1):
        for z in (z0 - 1, z1 + 1): b.set(x, G + 4, z, b.pick(MASON))
    for z, zw in ((z0 - 1, z0), (z1 + 1, z1)):
        for x in range(P0 - 1, P1 + 2):
            b.set(x, G + 5, z, 'stripped_dark_oak_log[axis=x]')
            post = x in (P0 - 1, -2, 1, P1 + 1)
            for y in range(C, C + 3):
                b.set(x, y, z, 'stripped_dark_oak_log[axis=x]' if y == C + 2 else 'dark_oak_log[axis=y]' if post else 'white_terracotta')
                if y < C + 2 and P0 <= x <= P1: b.set(x, y, zw, 'air')
        for x in (-1, 0): b.set(x, C + 1, z, 'air')
    b.gable_z(P0 - 1, P1 + 1, z0 - 1, z1 + 1, G + 9, 'deepslate_tile_stairs', 'deepslate_tile_slab', fill=[('white_terracotta', 1)])
    for z in (z0 - 1, z1 + 1): b.set(0, G + 11, z, 'air'); b.set(-1, G + 11, z, 'air')      # attic lights in the gables
    # chimney on the west end with an open hearth
    b.box(x0 - 1, G, -55, x0 - 1, G + 12, -55, FOOT); b.box(x0 - 1, G + 12, -55, x0 - 1, G + 14, -55, 'bricks'); b.set(x0 - 1, G + 15, -55, 'brick_wall')
    b.set(x0, C, -55, 'campfire[lit=true]'); b.set(x0, C + 1, -55, 'bricks'); b.set(x0, C, -56, 'bricks'); b.set(x0, C, -54, 'bricks')
    # chamber furniture
    b.set(0, C, -57, 'red_bed[facing=east,part=foot]'); b.set(1, C, -57, 'red_bed[facing=east,part=head]')
    b.set(3, C, -57, 'red_bed[facing=east,part=foot]'); b.set(4, C, -57, 'red_bed[facing=east,part=head]')
    b.set(-6, C, -52, 'spruce_fence'); b.set(-6, C + 1, -52, 'spruce_pressure_plate'); b.set(-5, C, -52, 'spruce_fence'); b.set(-5, C + 1, -52, 'spruce_pressure_plate')
    b.set(-7, C, -52, 'spruce_stairs[facing=east,half=bottom,shape=straight]'); b.set(-4, C, -52, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
    b.set(6, C, -52, 'chest[facing=north]'); b.set(7, C, -52, 'barrel[facing=up]'); b.set(7, C + 1, -52, 'barrel[facing=up]'); b.set(8, C, -52, 'barrel[facing=up]')
    b.set(-1, C, -57, 'grindstone[face=floor,facing=north]')
    b.set(-8, C, -57, 'hay_block[axis=x]'); b.set(-9, C, -57, 'hay_block[axis=z]')
    b.set(8, G + 5, -57, 'ladder[facing=west]')


def build():
    b = Build(seed=41)
    bridge(b); gatehouse(b)
    return b


if __name__ == '__main__':
    b = Build(seed=41)
    for x in range(-22, 22):
        for z in range(-68, -22):
            if -50 <= z <= -31:
                b.set(x, BED - 1, z, 'stone'); b.set(x, BED, z, b.pick([('gravel', 50), ('stone', 30), ('mud', 20)]))
                for y in range(BED + 1, WATER + 1): b.set(x, y, z, 'water')
                b.set(x, WATER + 1, z, 'air'); b.set(x, G, z, 'air')
            else:
                for y in range(BED - 1, G): b.set(x, y, z, b.pick(MASON))
                b.set(x, G, z, b.pick(STREET))
    bridge(b); gatehouse(b)
    for p in ((0, -40), (-2, -63)): b.box(p[0], 65, p[1], p[0], 66, p[1], 'red_wool')
    print(b.emit(sys.argv[1], 'rg-k4-bridge', 'K4 bridge and gatehouse, workshop build', pad=((-24, BED - 1, -70), (23, 100, -20))), 'ops')
    print('k4_bridge_ws -22 58 -68 21 82 -23')
