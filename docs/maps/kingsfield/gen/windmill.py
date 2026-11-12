"""K4 the Windmill: a post mill on a round stone base. Base 13 x 13 (local x 0..12, z 0..12, centre 6,6), ground
block y 64, ridge about 22 up, sails 21 across on the north face (toward the pit). Usage: python windmill.py OUT.json"""
import sys, math
from kit import Build, FOOT, MASON
G, B0 = 64, 65
b = Build(seed=44)
CX = CZ = 6

# round stone base, 5 high, door on the south, slab roof skirt
for x in range(-1, 14):
    for z in range(-1, 14):
        r = math.hypot(x - CX, z - CZ)
        if r <= 6.3:
            b.set(x, G, z, b.pick([('cobblestone', 3), ('coarse_dirt', 1)]))
            if r > 5.2:
                for y in range(B0, B0 + 5): b.set(x, y, z, b.pick(FOOT) if y < B0 + 2 else b.pick(MASON))
            b.set(x, B0 + 5, z, 'dark_oak_slab[type=bottom]' if r > 4.4 else 'dark_oak_planks')
        elif r <= 7.2:
            b.set(x, B0 + 4, z, 'dark_oak_slab[type=top]')
b.air(12, B0, 6, 12, B0 + 1, 7)                                             # base door, east
b.set(0, B0 + 2, 6, 'air')                                                  # slit window, west
b.box(CX, B0, CZ, CX, B0 + 5, CZ, 'oak_log[axis=y]')                        # the post
for dx, dz, ax in ((1, 0, 'x'), (-1, 0, 'x'), (0, 1, 'z'), (0, -1, 'z')):   # cross trees
    for k in (1, 2, 3): b.set(CX + dx * k, B0, CZ + dz * k, 'stripped_oak_log[axis=%s]' % ax)
for x, z in ((3, 4), (4, 3), (8, 9), (9, 8), (3, 8)): b.set(x, B0, z, 'barrel[facing=up]' if (x + z) % 2 else 'hay_block[axis=y]')

# the buck (mill body): 9 x 9, boarded, 9 high
x0, x1, z0, z1 = 2, 10, 2, 10
Y0, Y1 = B0 + 6, B0 + 14
for x in range(x0, x1 + 1):
    for z in range(z0, z1 + 1):
        edge = x in (x0, x1) or z in (z0, z1)
        b.set(x, Y0, z, 'spruce_planks')
        if not edge: continue
        corner = x in (x0, x1) and z in (z0, z1)
        for y in range(Y0, Y1 + 1):
            if corner: blk = 'dark_oak_log[axis=y]'
            elif y in (Y0, Y1): blk = 'stripped_dark_oak_log[axis=%s]' % ('x' if z in (z0, z1) else 'z')
            else: blk = b.pick([('spruce_planks', 8), ('stripped_spruce_log[axis=y]', 2)])
            b.set(x, y, z, blk)
for x, y, z in ((x0, Y0 + 4, 6), (x1, Y0 + 4, 6), (4, Y0 + 6, z0), (8, Y0 + 6, z0), (6, Y0 + 6, z1)):
    b.set(x, y, z, 'air')                                                  # windows
b.gable_z(x0, x1, z0, z1, Y1 + 1, 'spruce_stairs', 'dark_oak_slab', fill=[('spruce_planks', 1)])

# back door, stair ladder and tail pole to the south
b.air(6, Y0 + 1, z1, 6, Y0 + 2, z1)
for x in (5, 6, 7): b.set(x, Y0, z1 + 1, 'spruce_planks')                   # landing at the door
for i in range(7):                                                         # solid stone stair, 3 wide, stepped cheek walls
    z, y = z1 + 2 + i, Y0 - i
    for x in (4, 5, 6, 7, 8):
        b.set(x, G, z, b.pick(FOOT))
        if x in (4, 8):
            b.box(x, B0, z, x, y, z, FOOT)
        else:
            if y > B0: b.box(x, B0, z, x, y - 1, z, FOOT)
            b.set(x, y, z, 'cobblestone_stairs[facing=north,half=bottom,shape=straight]')
for x in (4, 8): b.set(x, B0 + 1, z1 + 8, 'lantern[hanging=false]')        # lamps on the foot of the cheek walls

# sails on the north face: windshaft, hub, four arms 10 long with cloth on the trailing side
HY, SZ = B0 + 13, z0 - 3
for z in range(SZ, z0 + 2): b.set(CX, HY, z, 'stripped_dark_oak_log[axis=z]')
for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
    px, py = -dy, dx                                                        # trailing side (clockwise)
    for r in range(1, 11):
        x, y = CX + dx * r, HY + dy * r
        b.set(x, y, SZ, 'stripped_spruce_log[axis=%s]' % ('x' if dx else 'y'))
        if r >= 3:
            for k in (1, 2):
                b.set(x + px * k, y + py * k, SZ, 'white_wool')
b.set(CX, HY, SZ, 'dark_oak_log[axis=z]')

# ---- inside ----------------------------------------------------------------------------------------------------
# base: sacks and barrels round the wall, two lamps under the floor, a ladder up the post to the buck
for y in range(B0 + 1, Y0 + 1): b.set(CX, y, CZ + 1, 'ladder[facing=south]')
b.set(4, B0 + 4, 4, 'lantern[hanging=true]'); b.set(8, B0 + 4, 8, 'lantern[hanging=true]')
for x, y, z in ((2, 0, 5), (2, 0, 6), (2, 1, 6), (2, 0, 7), (5, 0, 2), (6, 0, 2), (6, 1, 2), (9, 0, 3)): b.set(x, B0 + y, z, 'white_wool')
for x, y, z in ((5, 0, 10), (6, 0, 10), (7, 0, 10), (6, 1, 10), (3, 0, 9)): b.set(x, B0 + y, z, 'barrel[facing=up]')
# buck: the stones under a hopper, the upright shaft and brake wheel on the windshaft, flour sacks, a meal chest
for x in (5, 6):
    for z in (4, 5): b.set(x, Y0 + 1, z, 'smooth_stone')
b.set(5, Y0 + 2, 5, 'hopper[facing=down]')
for y in range(Y0 + 2, HY): b.set(6, y, 4, 'stripped_dark_oak_log[axis=y]')
b.set(CX, HY, 4, 'stripped_dark_oak_log[axis=z]')
for x in (5, 7): b.set(x, HY, 4, 'dark_oak_planks')
b.set(6, HY + 1, 4, 'dark_oak_planks')
for x, y, z in ((9, 1, 9), (8, 1, 9), (9, 1, 8), (9, 2, 9), (3, 1, 3)): b.set(x, Y0 + y, z, 'white_wool')
b.set(3, Y0 + 1, 9, 'barrel[facing=up]'); b.set(3, Y0 + 1, 8, 'barrel[facing=up]'); b.set(3, Y0 + 2, 9, 'barrel[facing=up]'); b.set(9, Y0 + 1, 3, 'chest[facing=west]')
for x in range(x0 + 1, x1): b.set(x, Y0 + 5, 8, 'stripped_spruce_log[axis=x]')
b.set(4, Y0 + 4, 8, 'lantern[hanging=true]'); b.set(8, Y0 + 4, 8, 'lantern[hanging=true]')

print(b.emit(sys.argv[1], 'kf-k4-windmill', 'K4 Windmill, workshop build', pad=((-12, 40, -12), (28, 130, 28))), 'ops', b.bounds())
