"""K6 the quarry crane: a treadwheel crane on a stone plinth, 9 x 9 (local x 0..8, z 0..8), ground block y 64,
about 17 high, jib 12 out to the south (+z) over the pit. Usage: python crane.py OUT.json"""
import sys, math
from kit import Build, FOOT, MASON
G, B0 = 64, 65
b = Build(seed=66)

# plinth, 2 high, steps on the north
b.box(0, G, 0, 8, G, 8, FOOT)
b.box(0, B0, 0, 8, B0, 8, FOOT)
b.box(0, B0 + 1, 0, 8, B0 + 1, 8, MASON)
for x in (3, 4, 5):
    b.set(x, B0, -2, 'cobblestone_stairs[facing=south,half=bottom,shape=straight]')
    b.set(x, B0, -1, 'cobblestone')
    b.set(x, B0 + 1, -1, 'cobblestone_stairs[facing=south,half=bottom,shape=straight]')

# tower frame: four posts, ties at mid height and top, diagonal braces
T0, TM, TT = B0 + 2, B0 + 7, B0 + 12
for x in (2, 6):
    for z in (2, 6): b.box(x, T0, z, x, TT, z, 'oak_log[axis=y]')
for y in (TM, TT):
    for k in range(2, 7):
        for z in (2, 6): b.set(k, y, z, 'stripped_oak_log[axis=x]')
        for x in (2, 6): b.set(x, y, k, 'stripped_oak_log[axis=z]')
for x in (2, 6):
    for z in (2, 6): b.set(x, TM, z, 'oak_log[axis=y]'); b.set(x, TT, z, 'oak_log[axis=y]')
for k in (1, 2):                                             # braces under the top tie
    for z in (2, 6):
        b.set(2 + k, TT - k, z, 'oak_fence'); b.set(6 - k, TT - k, z, 'oak_fence')

# treadwheel: a 7-wide ring in the y-z plane, axle along x
WY, WZ = B0 + 5, 4
for y in range(WY - 3, WY + 4):
    for z in range(WZ - 3, WZ + 4):
        r = math.hypot(y - WY, z - WZ)
        if 2.5 <= r <= 3.6:
            for x in (3, 4, 5): b.set(x, y, z, 'spruce_planks' if x != 4 else 'spruce_slab[type=%s]' % ('bottom' if y <= WY else 'top'))
        elif r > 0 and (y == WY or z == WZ): b.set(4, y, z, 'oak_fence')
for x in range(2, 7): b.set(x, WY, WZ, 'stripped_dark_oak_log[axis=x]')

# cap, jib and back stay
b.pyramid(1, 1, 7, 7, TT + 1, 'dark_oak_stairs', 'dark_oak_slab')
tip = None
for z in range(6, 19):
    y = TT - 2 + round((z - 6) * 7 / 12)
    b.set(4, y, z, 'stripped_spruce_log[axis=z]')
    if z > 6: b.set(4, y - 1, z - 1, b.get(4, y - 1, z - 1) or 'oak_fence')
    tip = (4, y, z)
for i, z in enumerate(range(1, -4, -1)):                     # back stay down to an anchor stone
    b.set(4, TT - 1 - i * 2, z, 'oak_fence'); b.set(4, TT - 2 - i * 2, z, 'oak_fence')
for y in range(tip[1] - 4, tip[1]): b.set(4, y, tip[2], 'chain[axis=y]')
b.box(3, tip[1] - 6, tip[2], 4, tip[1] - 5, tip[2] + 1, 'smooth_stone')      # the block on the hook
b.set(1, B0 + 2, 7, 'barrel[facing=up]')
b.set(7, B0 + 2, 1, 'lantern[hanging=false]')

print(b.emit(sys.argv[1], 'kf-k6-crane', 'K6 quarry crane, workshop build', pad=((-12, 40, -12), (28, 130, 32))), 'ops', b.bounds())
