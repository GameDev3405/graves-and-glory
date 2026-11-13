"""K6 the river shrine at point C, in map coordinates: x 18..37, z 100..113 (20 x 14), the z mirror of the watermill's
plot, so both points have the same cover. Chapel x 18..31 (stone, slate roof, ridge y 77), door on the west facing the
square; bell tower x 32..37, z 104..109 (top y 84) where the mill has its wheel; closed graveyard strips beside it.
build(b) adds it to a Build. Usage: python shrine.py OUT.json"""
import sys
from kit import Build, MASON, FOOT

G = 64
X0, X1, Z0, Z1 = 18, 31, 100, 113
STREET = [('cobblestone', 55), ('stone', 20), ('andesite', 20), ('mossy_cobblestone', 5)]
FLAGS = [('stone_bricks', 45), ('andesite', 25), ('polished_andesite', 20), ('cobblestone', 10)]


def build(b):
    T = G + 7                                             # wall head
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            aisle = 105 <= z <= 108
            b.set(x, G, z, 'polished_andesite' if aisle and (x + z) % 2 else b.pick(FLAGS))
            if edge:
                for y in range(G + 1, T): b.set(x, y, z, b.pick(FOOT) if y == G + 1 else b.pick(MASON))
                b.set(x, T, z, 'chiseled_stone_bricks' if (x + z) % 3 == 0 else 'stone_bricks')
    # doors: west (2 wide) to the square, a side door on the north
    b.air(X0, G + 1, 106, X0, G + 2, 107)
    for z in (106, 107): b.set(X0, G + 3, z, 'chiseled_stone_bricks')
    b.set(X0 - 1, G + 3, 105, 'stone_brick_stairs[facing=south,half=top,shape=straight]'); b.set(X0 - 1, G + 3, 108, 'stone_brick_stairs[facing=north,half=top,shape=straight]')
    b.air(24, G + 1, Z0, 24, G + 2, Z0)
    # lancet windows and buttresses on the long sides
    for x in (21, 26, 27):
        for z in (Z0, Z1):
            for y in range(G + 2, G + 7): b.set(x, y, z, 'yellow_stained_glass_pane[east=true,west=true]')
    for z in (104, 109):                                   # the east end: tall lights either side of the altar wall
        for y in range(G + 2, G + 7): b.set(X1, y, z, 'yellow_stained_glass_pane[north=true,south=true]') if z < 104 or z > 109 else None
    for z in (102, 111):
        for y in range(G + 2, G + 7): b.set(X1, y, z, 'yellow_stained_glass_pane[north=true,south=true]')
    for x in (19, 23, 29):
        for z, f in ((Z0 - 1, 'south'), (Z1 + 1, 'north')):
            if x == 23 and z == Z0 - 1: continue
            for y in range(G + 1, G + 5): b.set(x, y, z, b.pick(MASON))
            b.set(x, G + 5, z, 'stone_brick_stairs[facing=%s,half=bottom,shape=straight]' % f)
    # roof: slate, stone gables, a round light in the west gable
    b.gable_x(X0, X1, Z0, Z1, T + 1, 'deepslate_tile_stairs', 'deepslate_tile_slab', fill=MASON)
    for z in (106, 107):
        for y in (T + 3, T + 4): b.set(X0, y, z, 'yellow_stained_glass_pane[north=true,south=true]')
    for z in (105, 106, 107, 108):
        for y in (T + 2, T + 3, T + 4, T + 5):
            if (z in (105, 108)) != (y in (T + 2, T + 5)) and b.get(X0, y, z) not in (None, 'air'): b.set(X0, y, z, 'chiseled_stone_bricks')
    b.set(X0, T + 9, 106, 'stone_brick_wall'); b.set(X0, T + 9, 107, 'stone_brick_wall')
    for x in (22, 27):
        b.box(x, T + 1, Z0 + 1, x, T + 1, Z1 - 1, 'dark_oak_log[axis=z]')
        for z in (105, 108): b.set(x, T, z, 'lantern[hanging=true]')
    # inside: altar with the relic bone, pews, font, lectern
    Y = G + 1
    for z in (106, 107):
        b.set(29, Y, z, 'chiseled_stone_bricks'); b.set(30, Y, z, 'stone_bricks'); b.set(30, Y + 1, z, 'bone_block[axis=z]')
    b.set(29, Y + 1, 106, 'candle[candles=3,lit=true]'); b.set(29, Y + 1, 107, 'candle[candles=2,lit=true]')
    for z in (105, 108): b.set(30, Y, z, 'stone_brick_wall'); b.set(30, Y + 1, z, 'lantern[hanging=false]')
    for x in range(28, 31):
        for z in range(104, 110):
            if b.get(x, Y, z) is None and x == 28: b.set(x, Y, z, 'red_carpet')
    for x in (20, 22, 24, 26):
        for z in (102, 103, 104, 109, 110, 111):
            if x == 24 and z == 102: continue
            b.set(x, Y, z, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
    b.set(19, Y, 111, 'water_cauldron[level=3]'); b.set(19, Y, 102, 'lectern[facing=east]')
    b.set(30, Y, 101, 'bookshelf'); b.set(30, Y + 1, 101, 'bookshelf'); b.set(30, Y, 112, 'chest[facing=west]'); b.set(30, Y + 1, 112, 'candle[candles=1,lit=true]')
    # bell tower
    tx0, tx1, tz0, tz1 = 32, 37, 104, 109
    for x in range(tx0, tx1 + 1):
        for z in range(tz0, tz1 + 1):
            edge = x in (tx0, tx1) or z in (tz0, tz1)
            b.set(x, G, z, b.pick(FLAGS))
            if edge:
                for y in range(G + 1, G + 21):
                    open_ = y in (G + 18, G + 19) and ((x in (34, 35)) or (z in (106, 107)))
                    b.set(x, y, z, 'air' if open_ else b.pick(FOOT) if y == G + 1 else b.pick(MASON))
                if (x in (34, 35)) or (z in (106, 107)): b.set(x, G + 17, z, 'chiseled_stone_bricks')
            else:
                b.set(x, G + 17, z, 'spruce_planks')
    for i in range(4):                                         # a steep spire: each ring two courses high
        a0, a1, c0, c1 = tx0 - 1 + i, tx1 + 1 - i, tz0 - 1 + i, tz1 + 1 - i
        for x in range(a0, a1 + 1):
            for z in range(c0, c1 + 1):
                if not (x in (a0, a1) or z in (c0, c1)): continue
                corner = x in (a0, a1) and z in (c0, c1)
                y = G + 21 + 2 * i
                f = 'east' if x == a0 else 'west' if x == a1 else 'south' if z == c0 else 'north'
                if i == 0:
                    b.set(x, y, z, 'deepslate_tile_slab[type=bottom]' if corner else 'deepslate_tile_stairs[facing=%s,half=bottom,shape=straight]' % f)
                    continue
                b.set(x, y - 1, z, 'deepslate_tiles')
                b.set(x, y, z, 'deepslate_tile_slab[type=bottom]' if corner else 'deepslate_tile_stairs[facing=%s,half=bottom,shape=straight]' % f)
    for x in (34, 35):
        for z in (106, 107):
            b.set(x, G + 27, z, 'deepslate_tiles')
            f = {(34, 106): 'east', (35, 106): 'south', (35, 107): 'west', (34, 107): 'north'}[(x, z)]
            b.set(x, G + 28, z, 'deepslate_tile_stairs[facing=%s,half=bottom,shape=outer_right]' % f)
    for x, z in ((tx0, tz0), (tx0, tz1), (tx1, tz0), (tx1, tz1)):  # corner pinnacles
        b.set(x, G + 22, z, 'polished_andesite'); b.set(x, G + 23, z, 'stone_brick_wall'); b.set(x, G + 24, z, 'stone_brick_wall'); b.set(x, G + 25, z, 'deepslate_tile_wall')
    b.box(34, G + 20, 105, 35, G + 20, 108, 'dark_oak_log[axis=z]'); b.set(34, G + 19, 106, 'bell[attachment=ceiling,facing=north]')
    b.air(X1, G + 1, 106, tx0, G + 2, 106)                     # vestry door from the chapel
    for y in range(G + 1, G + 18): b.set(33, y, 105, 'ladder[facing=east]')
    b.set(36, G + 1, 108, 'chest[facing=west]'); b.set(36, G + 1, 105, 'barrel[facing=up]'); b.set(36, G + 2, 105, 'candle[candles=2,lit=true]')
    for y in (G + 4, G + 5, G + 9, G + 10, G + 14, G + 15):
        b.set(tx1, y, 106, 'iron_bars[north=true,south=true]'); b.set(34, y, tz0, 'iron_bars[east=true,west=true]'); b.set(34, y, tz1, 'iron_bars[east=true,west=true]')
    for x in (tx0, tx1):                                       # pale stone: quoins up the corners, two string courses, a spire finial
        for z in (tz0, tz1):
            for y in range(G + 1, G + 21):
                b.set(x, y, z, 'polished_andesite' if y % 2 else 'stone_bricks')
    for x in range(tx0, tx1 + 1):
        for z in range(tz0, tz1 + 1):
            if (x in (tx0, tx1) or z in (tz0, tz1)) and b.get(x, G + 11, z) not in (None, 'air'): b.set(x, G + 11, z, 'polished_andesite')
            if (x in (tx0, tx1) or z in (tz0, tz1)) and b.get(x, G + 20, z) not in (None, 'air'): b.set(x, G + 20, z, 'polished_andesite' if (x + z) % 2 else 'chiseled_stone_bricks')
    # closed graveyard strips north and south of the tower
    for x in range(X1 + 1, 38):
        for z in list(range(Z0, tz0)) + list(range(tz1 + 1, Z1 + 1)):
            wall = x == 37 or z in (Z0, Z1)
            b.set(x, G, z, b.pick(MASON) if wall else b.pick([('grass_block', 60), ('podzol', 25), ('coarse_dirt', 15)]))
            if wall: b.set(x, G + 1, z, 'stone_brick_wall' if (x + z) % 5 else 'mossy_stone_brick_wall')
    for x, z, s in ((33, 101, 'cobblestone_wall'), (35, 102, 'mossy_cobblestone_wall'), (34, 111, 'cobblestone_wall'), (36, 111, 'stone_brick_wall'), (33, 112, 'mossy_cobblestone_wall')):
        b.set(x, G + 1, z, s); b.set(x, G, z - 1 if z < 106 else z, 'coarse_dirt')
    for x, z, f in ((34, 102, 'white_tulip'), (36, 101, 'lily_of_the_valley'), (35, 111, 'poppy'), (33, 110, 'fern'), (36, 102, 'fern')):
        b.set(x, G + 1, z, f)
    for x, z in ((36, 102), (36, 111)):                        # yews
        b.set(x, G + 1, z, 'spruce_log[axis=y]'); b.set(x, G + 2, z, 'spruce_log[axis=y]')
        for dx, dy, dz in ((0, 3, 0), (0, 4, 0), (-1, 3, 0), (0, 3, -1 if z < 106 else 1), (-1, 2, 0), (0, 2, -1 if z < 106 else 1)): b.set(x + dx, G + dy, z + dz, 'spruce_leaves[persistent=true]')
    # by the west door: lamps on plinths
    for z in (104, 109): b.set(X0 - 1, G + 1, z, 'stone_brick_wall'); b.set(X0 - 1, G + 2, z, 'lantern[hanging=false]')


if __name__ == '__main__':
    b = Build(seed=58)
    for x in range(6, 46):
        for z in range(91, 123):
            for y in range(G - 2, G): b.set(x, y, z, 'stone')
            b.set(x, G, z, b.pick(STREET) if x < 18 or z < 100 else 'grass_block')
    build(b)
    for p in ((12, 104), (26, 96)): b.box(p[0], 65, p[1], p[0], 66, p[1], 'red_wool')
    print(b.emit(sys.argv[1], 'rg-k6-shrine', 'K6 river shrine, workshop build', pad=((4, G - 3, 89), (48, 110, 125))), 'ops')
    print('k6_shrine_ws 6 62 91 45 86 122')
