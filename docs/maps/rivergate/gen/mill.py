"""K5 the watermill at point A, in map coordinates: x 18..37, z -113..-100 (20 x 14), north-east of the A ring
(centre 0,-91, radius 17; nearest corner is 19.7 away). Mill house x 18..31, stone below, shingle roof (ridge y 77);
walled wheel pit x 32..37 with an undershot wheel 9 across. Door on the west, facing the square.
build(b) adds it to a Build. Usage: python mill.py OUT.json"""
import math, sys
from kit import Build, MASON, FOOT

G = 64
X0, X1, Z0, Z1 = 18, 31, -113, -100
STREET = [('cobblestone', 55), ('stone', 20), ('andesite', 20), ('mossy_cobblestone', 5)]
FLAGS = [('stone_bricks', 45), ('andesite', 25), ('polished_andesite', 20), ('cobblestone', 10)]


def build(b):
    T = G + 5                                             # loft floor
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            b.set(x, G, z, b.pick(FLAGS))
            if edge:
                corner = x in (X0, X1) and z in (Z0, Z1)
                for y in range(G + 1, T): b.set(x, y, z, b.pick(FOOT) if y == G + 1 or (corner and y < G + 4) else b.pick(MASON))
            b.set(x, T, z, ('stripped_spruce_log[axis=%s]' % ('x' if z in (Z0, Z1) else 'z')) if edge else 'spruce_planks')
    U = T + 4                                             # eaves plate over the timber upper storey
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if not (x in (X0, X1) or z in (Z0, Z1)): continue
            post = (x in (X0, X1) and z in (Z0, Z1)) or (z in (Z0, Z1) and (x - X0) % 4 == 1) or (x in (X0, X1) and (z - Z0) % 4 == 1)
            for y in range(T + 1, U): b.set(x, y, z, 'dark_oak_log[axis=y]' if post else 'white_terracotta')
            b.set(x, U, z, 'stripped_dark_oak_log[axis=%s]' % ('x' if z in (Z0, Z1) else 'z'))
    for x in (21, 25, 29):
        for z, dz, f in ((Z0, -1, 'north'), (Z1, 1, 'south')):
            if x == 25 and z == Z0: continue
            b.set(x, T + 2, z, 'air')
            for k in (-1, 1): b.set(x + k, T + 2, z + dz, 'spruce_trapdoor[facing=%s,half=top,open=true]' % f)
    for z in (-110, -103): b.set(X0, T + 2, z, 'air')
    # doors: west (2 wide) to the square, south loading door
    b.air(X0, G + 1, -107, X0, G + 2, -106); b.set(X0, G + 3, -107, 'stripped_spruce_log[axis=z]'); b.set(X0, G + 3, -106, 'stripped_spruce_log[axis=z]')
    b.air(24, G + 1, Z1, 24, G + 2, Z1); b.set(24, G + 3, Z1, 'stripped_spruce_log[axis=x]')
    # windows with shutters
    for x in (21, 28):
        for z, dz, f in ((Z0, -1, 'north'), (Z1, 1, 'south')):
            b.set(x, G + 2, z, 'air')
            for k in (-1, 1): b.set(x + k, G + 2, z + dz, 'spruce_trapdoor[facing=%s,half=top,open=true]' % f)
    for z in (-111, -102):
        b.set(X0, G + 2, z, 'air')
        for k in (-1, 1): b.set(X0 - 1, G + 2, z + k, 'spruce_trapdoor[facing=west,half=top,open=true]')
    # roof: shingle, timbered gables, a hoist beam over the loading door
    b.gable_x(X0, X1, Z0, Z1, U + 1, 'dark_oak_stairs', 'dark_oak_slab', fill=[('white_terracotta', 1)])
    for x in (X0, X1):
        for y in range(U + 1, U + 9):
            for z in range(Z0, Z1 + 1):
                if b.get(x, y, z) == 'white_terracotta' and (z in (-107, -106) or y == U + 4): b.set(x, y, z, 'dark_oak_log[axis=%s]' % ('y' if z in (-107, -106) else 'z'))
    b.set(X0, U + 2, -109, 'air'); b.set(X0, U + 2, -104, 'air')
    for x in (23, 24, 25, 26):                               # a louvred vent on the ridge
        b.set(x, U + 9, -107, 'spruce_planks'); b.set(x, U + 9, -106, 'spruce_planks'); b.set(x, U + 10, -107, 'dark_oak_slab[type=bottom]'); b.set(x, U + 10, -106, 'dark_oak_slab[type=bottom]')
    for z in (Z1 + 1, Z1 + 2): b.set(24, U, z, 'dark_oak_log[axis=z]')
    for y in range(T + 1, U): b.set(24, y, Z1 + 2, 'chain[axis=y]')
    b.set(24, T, Z1 + 2, 'barrel[facing=up]')
    for y in (T + 1, T + 2): b.set(24, y, Z1, 'air')
    # the wheel pit: bed y 60, water y 61..62, stone walls, parapet all round
    for x in range(X1 + 1, 38):
        for z in range(Z0, Z1 + 1):
            wall = x == 37 or z in (Z0, Z1)
            b.set(x, G - 5, z, 'stone')
            if wall:
                for y in range(G - 4, G + 1): b.set(x, y, z, b.pick(MASON if y > G - 2 else FOOT))
                b.set(x, G + 1, z, 'stone_brick_wall')
            else:
                b.set(x, G - 4, z, b.pick([('gravel', 60), ('mossy_cobblestone', 40)]))
                for y in (G - 3, G - 2): b.set(x, y, z, 'water')
                for y in (G - 1, G): b.set(x, y, z, 'air')
    for z in (Z0, Z1):                                    # head and tail race arches
        for x in (34, 35): b.set(x, G - 2, z, 'iron_bars[east=true,west=true]'); b.set(x, G - 3, z, 'water')
    for z in range(Z0 + 1, Z1):                            # the mill wall is stone down to the bed on the pit side
        for y in range(G - 4, G + 1): b.set(X1, y, z, b.pick(MASON if y > G - 2 else FOOT))
    # the wheel: 9 across, in the y-z plane, two rims with paddles, axle into the mill
    cy, cz = G + 3, -106.5
    for y in range(G - 3, G + 10):
        for z in range(-112, -100):
            r = math.hypot(y - cy, z - cz)
            ang = math.degrees(math.atan2(y - cy, z - cz)) % 30
            if 4.7 <= r <= 5.7:
                b.set(34, y, z, 'stripped_spruce_log[axis=x]')
                if ang < 8 or ang > 24:                      # paddles stand out each side of the rim
                    for x in (33, 35): b.set(x, y, z, 'dark_oak_planks')
            elif r < 4.7 and (abs(y - cy) < 0.6 or abs(z - cz) < 0.6):
                b.set(34, y, z, 'spruce_fence' if r > 1 else 'stripped_dark_oak_log[axis=x]')
    for x in range(28, 36):
        for z in (-107, -106): b.set(x, G + 3, z, 'stripped_dark_oak_log[axis=x]')
    # inside: pit wheel, stones, hopper, sacks, flour, miller's hearth, ladder to the loft
    Y = G + 1
    for y in (Y, Y + 1, Y + 2):
        for z in (-108, -107, -106, -105):
            if (y, z) not in ((Y, -108), (Y, -105), (Y + 2, -108), (Y + 2, -105)): b.set(27, y, z, 'spruce_planks' if (y + z) % 2 else 'dark_oak_planks')
    for x in (23, 24):
        for z in (-108, -107): b.set(x, Y, z, 'smooth_stone'); b.set(x, Y + 1, z, 'smooth_stone_slab[type=bottom]')
    b.set(25, Y, -107, 'stripped_dark_oak_log[axis=x]'); b.set(26, Y, -107, 'stripped_dark_oak_log[axis=x]')
    b.set(23, Y + 3, -108, 'hopper[facing=down]'); b.set(23, Y + 2, -108, 'chain[axis=y]')
    b.set(22, Y, -108, 'white_concrete_powder'); b.set(22, Y, -109, 'white_concrete_powder'); b.set(23, Y, -109, 'white_carpet')
    for x, z, blk in ((19, -101, 'barrel[facing=up]'), (20, -101, 'barrel[facing=up]'), (19, -102, 'hay_block[axis=z]'), (21, -101, 'composter[level=5]'),
                      (29, -101, 'chest[facing=north]'), (30, -101, 'barrel[facing=up]'), (30, -102, 'barrel[facing=up]'), (30, -112, 'crafting_table'),
                      (29, -112, 'grindstone[face=floor,facing=south]')):
        b.set(x, Y, z, blk)
    b.set(19, Y + 1, -101, 'barrel[facing=up]'); b.set(30, Y + 1, -101, 'lantern[hanging=false]')
    b.set(25, Y, Z0, 'campfire[lit=true]'); b.set(25, Y + 1, Z0, 'bricks'); b.set(24, Y, Z0, 'bricks'); b.set(26, Y, Z0, 'bricks')
    b.box(25, G, Z0 - 1, 25, U + 3, Z0 - 1, FOOT); b.box(25, U + 3, Z0 - 1, 25, U + 5, Z0 - 1, 'bricks'); b.set(25, U + 6, Z0 - 1, 'brick_wall')
    for y in range(Y, T + 1): b.set(19, y, -112, 'ladder[facing=east]')
    for x in (21, 28): b.set(x, T - 1, -106, 'lantern[hanging=true]')
    # loft: the miller's bed, sacks, hay
    L = T + 1
    b.set(29, L, -104, 'white_bed[facing=east,part=foot]'); b.set(30, L, -104, 'white_bed[facing=east,part=head]'); b.set(30, L, -105, 'chest[facing=west]')
    for x, z in ((20, -103), (21, -103), (20, -104), (27, -110), (28, -110), (28, -109)): b.set(x, L, z, 'hay_block[axis=x]')
    b.set(20, L + 1, -103, 'hay_block[axis=z]'); b.set(22, L, -109, 'barrel[facing=up]'); b.set(22, L, -110, 'barrel[facing=up]'); b.set(23, L, -110, 'composter[level=7]')
    # forecourt touches: a sack bench and a lamp by the door
    b.set(X0 - 1, G + 1, -109, 'barrel[facing=up]'); b.set(X0 - 1, G + 2, -109, 'lantern[hanging=false]'); b.set(X0 - 1, G + 1, -104, 'hay_block[axis=z]')


if __name__ == '__main__':
    b = Build(seed=57)
    for x in range(6, 46):
        for z in range(-122, -90):
            for y in range(G - 2, G): b.set(x, y, z, 'stone')
            b.set(x, G, z, b.pick(STREET) if x < 18 or z > -100 else 'grass_block')
    build(b)
    for p in ((12, -104), (26, -96)): b.box(p[0], 65, p[1], p[0], 66, p[1], 'red_wool')
    print(b.emit(sys.argv[1], 'rg-k5-mill', 'K5 watermill, workshop build', pad=((4, G - 6, -124), (48, 110, -88))), 'ops')
    print('k5_mill_ws 6 59 -122 45 84 -91')
