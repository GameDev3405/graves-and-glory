"""K3 the Tithe Barn: 34 x 16 (plus a 3-deep wagon porch on each long side), local x 0..33, z 0..15, ground block y 64. Great doors in both long sides so the
escort wagon drives through. Usage: python barn.py OUT.json"""
import sys
from kit import Build, FOOT
G, B0 = 64, 65
EAVE = B0 + 5                    # top wall course (6 high)
b = Build(seed=33)
X0, X1, Z0, Z1 = 0, 33, 0, 15
DOOR = range(14, 20)             # 6 wide

# floor: beaten earth, a cart lane through the doors
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        b.set(x, G, z, 'dirt_path' if x in DOOR else b.pick([('coarse_dirt', 4), ('rooted_dirt', 1)]))

# walls: stone footing, boarded timber frame, top plate
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        if not (x in (X0, X1) or z in (Z0, Z1)): continue
        long_side = z in (Z0, Z1)
        post = (x in (X0, X1) and z in (Z0, Z1)) or (long_side and x % 5 in (0, 3) and x % 5 == (0 if x < 14 else 3)) \
            or (not long_side and z in (5, 10))
        for y in range(B0, EAVE + 1):
            if y < B0 + 2: blk = b.pick(FOOT)
            elif y == EAVE: blk = 'stripped_oak_log[axis=%s]' % ('x' if long_side else 'z')
            else: blk = 'oak_log[axis=y]' if post else b.pick([('spruce_planks', 9), ('stripped_spruce_log[axis=y]', 1)])
            b.set(x, y, z, blk)

# great doors, 6 wide and 5 high, with stone jambs and an oak lintel
for z in (Z0, Z1):
    b.air(DOOR[0], B0, z, DOOR[-1], B0 + 4, z)
    for x in (DOOR[0] - 1, DOOR[-1] + 1):
        b.box(x, B0, z, x, B0 + 4, z, FOOT)
# small doors in the gable ends, and loft openings
for x in (X0, X1):
    b.air(x, B0, 7, x, B0 + 1, 8)

# aisle posts and tie beams
for x in range(4, X1, 5):
    if x in DOOR: continue
    for z in (4, 11):
        b.box(x, B0, z, x, EAVE, z, 'oak_log[axis=y]')
    b.box(x, EAVE, 1, x, EAVE, 14, 'stripped_oak_log[axis=z]')

# roof: spruce, ridge along x, dark ridge line, a few tiles gone
b.gable_x(X0, X1, Z0, Z1, EAVE + 1, 'spruce_stairs', 'dark_oak_slab', fill=[('spruce_planks', 1)])
top = max(k[1] for k in b.c)
for (x, y, z) in list(b.c):
    if y == top - 0 and 'stairs' in b.c[(x, y, z)]: b.c[(x, y, z)] = b.c[(x, y, z)].replace('spruce_stairs', 'dark_oak_stairs')
for x, i in ((7, 3), (8, 3), (24, 5), (27, 2), (28, 2), (28, 3)):   # holes on the south slope
    b.c.pop((x, EAVE + 1 + i, Z1 + 1 - i), None)
# wagon porches over both great doors: 3 deep, the swung-open door leaves are their sides, cross gable into the main roof
for side in (-1, 1):
    zw = Z0 if side < 0 else Z1                       # wall line
    for d in (1, 2, 3):
        z = zw + side * d
        for x in range(DOOR[0], DOOR[-1] + 1): b.set(x, G, z, 'dirt_path')
        for x in (DOOR[0] - 1, DOOR[-1] + 1):
            b.set(x, G, z, b.pick(FOOT))
            for y in range(B0, B0 + 5):
                b.set(x, y, z, 'oak_log[axis=y]' if d == 3 else ('dark_oak_planks' if y < B0 + 4 else 'stripped_oak_log[axis=z]'))
    for x in range(DOOR[0], DOOR[-1] + 1): b.set(x, B0 + 5, zw + side * 3, 'stripped_oak_log[axis=x]')
    xa, xb = DOOR[0] - 2, DOOR[-1] + 2
    for d in range(-8, 5):                            # d: distance out from the wall line (negative = over the barn)
        z = zw + side * d
        main_y = EAVE + 1 + (-d + 1) if d <= 1 else -99    # height of the main roof at this z
        i = 0
        while xa + i < xb - i:
            y = B0 + 5 + i
            if y > main_y:
                b.set(xa + i, y, z, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
                b.set(xb - i, y, z, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
                if d == 3:
                    for x in range(xa + i + 1, xb - i): b.set(x, y, z, 'spruce_planks' if y > B0 + 5 else b.get(x, y, z) or 'spruce_planks')
            i += 1
        if xa + i == xb - i and B0 + 5 + i > main_y: b.set(xa + i, B0 + 5 + i, z, 'dark_oak_slab[type=bottom]')
    b.set(DOOR[0] + 2, B0 + 4, zw + side * 3, 'lantern[hanging=true]')
    b.set(DOOR[0] + 3, B0 + 4, zw + side * 3, 'lantern[hanging=true]')
# loft windows in the gables
for x in (X0, X1):
    b.set(x, EAVE + 3, 7, 'air')
    b.set(x, EAVE + 3, 8, 'air')

# stores: hay in the end bays, sacks and barrels, clear of the cart lane
for x0, x1 in ((1, 6), (27, 32)):
    for x in range(x0, x1 + 1):
        for z in range(1, 15):
            if z in (7, 8): continue
            h = max(0, 3 - abs(x - (x0 + x1) // 2) - (1 if z in (6, 9) else 0) - b.rng.randint(0, 1))
            for y in range(B0, B0 + h): b.set(x, y, z, 'hay_block[axis=%s]' % b.rng.choice('xz'))
for x, z in ((11, 1), (12, 1), (11, 2), (22, 14), (21, 14)):
    b.set(x, B0, z, 'barrel[facing=up]')
b.set(11, B0 + 1, 1, 'barrel[facing=up]')
for x in (9, 24):
    b.set(x, EAVE - 1, 4, 'lantern[hanging=true]')
    b.set(x, EAVE - 1, 11, 'lantern[hanging=true]')

print(b.emit(sys.argv[1], 'kf-k3-barn', 'K3 Tithe Barn, workshop build', pad=((-8, 40, -8), (48, 120, 32))), 'ops', b.bounds())
