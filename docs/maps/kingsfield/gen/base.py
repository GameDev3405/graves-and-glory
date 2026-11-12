"""K1 the base camp (Dawn as designed, front to the east; Dusk is its mirror in x with purple cloth). 72 x 80,
local x 0..71 (71 = the front), z 0..79, ground block y 64. The muster lane (z 28..51, from the dais to the open
front) stays empty. Usage: python base.py OUT.json [cyan|purple]"""
import sys, math
from kit import Build, MASON, FOOT, DAUB, FLOOR, ROOF, ROOF_SLAB
G, B0, WT = 64, 65, 69
CLOTH = (sys.argv[2] if len(sys.argv) > 2 else 'cyan') + '_wool'
b = Build(seed=11)
LANE = range(28, 52)

# ground: trampled yard, a paved muster lane
for x in range(0, 72):
    for z in range(0, 80):
        n = math.sin(x * 0.29) * math.cos(z * 0.33 + 2) + 0.5 * math.sin((x + z) * 0.17)
        yard = 'coarse_dirt' if n > -0.1 else ('dirt_path' if n > -0.6 else 'rooted_dirt')
        if z in LANE and x >= 18: yard = 'dirt_path' if (z not in (28, 51)) else b.pick(FOOT)
        b.set(x, G, z, yard)

# wall: 2 thick, 5 high, parapet outside; the front is open 24 wide
def on_wall(x, z):
    return (x <= 1 or x >= 70 or z <= 1 or z >= 78) and not (x >= 70 and z in LANE)
for x in range(0, 72):
    for z in range(0, 80):
        if not on_wall(x, z): continue
        outer = x in (0, 71) or z in (0, 79)
        for y in range(B0, WT + 1): b.set(x, y, z, b.pick(FOOT) if y == B0 else b.pick(MASON))
        if outer:
            b.set(x, WT + 1, z, b.pick(MASON))
            if (x + z) % 2 == 0: b.set(x, WT + 2, z, b.pick(MASON))
# wall walk behind the wall on timber, so defenders can stand on it
for x in range(2, 70):
    for z in (2, 77): b.set(x, WT, z, 'spruce_slab[type=top]')
for z in range(2, 78):
    b.set(2, WT, z, 'spruce_slab[type=top]')
    if z not in LANE: b.set(69, WT, z, 'spruce_slab[type=top]')
for x in range(6, 70, 7):
    for z in (2, 77): b.box(x, B0, z, x, WT - 1, z, 'oak_log[axis=y]')
for z in range(6, 78, 7):
    b.box(2, B0, z, 2, WT - 1, z, 'oak_log[axis=y]')

def tower(x0, z0, size, top, roof=True):
    x1, z1 = x0 + size - 1, z0 + size - 1
    b.box(x0, B0, z0, x1, top, z1, MASON)
    b.box(x0, B0, z0, x1, B0 + 1, z1, FOOT)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, top + 1, z, b.pick(MASON))
                if (x + z) % 2 == 0: b.set(x, top + 2, z, b.pick(MASON))
    if roof:
        for px, pz in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)): b.box(px, top + 1, pz, px, top + 4, pz, 'oak_log[axis=y]')
        b.pyramid(x0 - 1, z0 - 1, x1 + 1, z1 + 1, top + 5, ROOF, ROOF_SLAB)
    for y in (B0 + 4, top - 2):                              # arrow loops
        b.set(x0, y, z0 + size // 2, 'air'); b.set(x1, y, z0 + size // 2, 'air')
        b.set(x0 + size // 2, y, z0, 'air'); b.set(x0 + size // 2, y, z1, 'air')

# gate towers 7 x 7, 12 high, either side of the open front; corner turrets 5 x 5, 7 high
for z0 in (21, 52):
    tower(65, z0, 7, B0 + 11)
    zc = z0 + 3
    for y in range(B0 + 5, B0 + 11):                         # long banner on the front face
        for z in (zc - 1, zc, zc + 1): b.set(72, y, z, CLOTH)
    b.box(72, B0 + 11, zc - 1, 72, B0 + 11, zc + 1, 'dark_oak_slab[type=top]')
for x0 in (0, 67):
    for z0 in (0, 75): tower(x0, z0, 5, B0 + 6, roof=False)

# command dais at the back of the lane: 12 x 14, 2 high, steps to the east, canopy and banners
dx0, dx1, dz0, dz1 = 6, 17, 33, 46
b.box(dx0, B0, dz0, dx1, B0 + 1, dz1, MASON)
b.box(dx0, B0 + 1, dz0, dx1, B0 + 1, dz1, [('spruce_planks', 4), ('stripped_spruce_log[axis=x]', 1)])
for z in range(dz0 + 2, dz1 - 1):
    b.set(dx1 + 1, B0 + 1, z, 'stone_brick_stairs[facing=west,half=bottom,shape=straight]')
    b.set(dx1 + 1, B0, z, 'stone_bricks')
    b.set(dx1 + 2, B0, z, 'stone_brick_stairs[facing=west,half=bottom,shape=straight]')
for px, pz in ((dx0, dz0), (dx0, dz1), (dx0 + 6, dz0), (dx0 + 6, dz1)):
    b.box(px, B0 + 2, pz, px, B0 + 6, pz, 'dark_oak_log[axis=y]')
for x in range(dx0 - 1, dx0 + 8):                            # canopy over the back half
    for z in range(dz0 - 1, dz1 + 2):
        edge = x in (dx0 - 1, dx0 + 7) or z in (dz0 - 1, dz1 + 1)
        b.set(x, B0 + 7, z, CLOTH if (z // 2) % 2 == 0 and not edge else ('white_wool' if not edge else 'dark_oak_slab[type=bottom]'))
b.box(dx0 + 2, B0 + 2, 39, dx0 + 3, B0 + 2, 40, 'dark_oak_planks')                       # map table
b.set(dx0 + 2, B0 + 3, 39, 'lantern[hanging=false]')
b.set(dx0 + 1, B0 + 2, 36, 'barrel[facing=up]'); b.set(dx0 + 1, B0 + 2, 43, 'barrel[facing=up]')
for pz in (dz0 + 1, dz1 - 1):                                # standards at the front corners of the dais
    b.box(dx1, B0 + 2, pz, dx1, B0 + 9, pz, 'oak_fence')
    for y in range(B0 + 6, B0 + 10): b.set(dx1, y, pz + (1 if pz < 40 else -1), CLOTH)

def hall(x0, z0, x1, z1, h, wall, door_z, doors, roof='x'):
    """timber-framed hall on a stone footing; doors: x positions on the wall line door_z"""
    b.box(x0, G, z0, x1, G, z1, FLOOR)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)): continue
            post = (x in (x0, x1) and z in (z0, z1)) or (z in (z0, z1) and (x - x0) % 4 == 0) or (x in (x0, x1) and (z - z0) % 4 == 0)
            for y in range(B0, B0 + h):
                blk = b.pick(FOOT) if y < B0 + 1 else ('oak_log[axis=y]' if post else wall)
                if y == B0 + h - 1: blk = 'stripped_oak_log[axis=%s]' % ('x' if z in (z0, z1) else 'z')
                b.set(x, y, z, blk)
    for x in doors: b.air(x, B0, door_z, x + 1, B0 + 1, door_z)
    for x in range(x0 + 2, x1 - 1, 4):
        for z in (z0, z1):
            if b.get(x, B0 + 2, z) == wall and x not in doors and x - 1 not in doors: b.set(x, B0 + 2, z, 'air')
    if roof == 'x': b.gable_x(x0, x1, z0, z1, B0 + h, ROOF, ROOF_SLAB, fill=[(wall, 1)])
    else: b.gable_z(x0, x1, z0, z1, B0 + h, ROOF, ROOF_SLAB, fill=[(wall, 1)])

# barracks: long hall on the north side, doors to the yard
hall(14, 5, 45, 14, 5, DAUB, 14, (21, 29, 37))
b.box(13, B0, 9, 13, B0 + 9, 9, FOOT); b.set(13, B0 + 10, 9, 'cobblestone_wall')
for x in (17, 41): b.box(x, B0, 16, x, B0 + 1, 16, 'oak_fence'); b.set(x, B0 + 2, 16, 'lantern[hanging=false]')   # lamp posts
# armoury: small stone store beside the barracks
hall(50, 5, 60, 13, 5, 'stone_bricks', 13, (54,))
for x, z in ((48, 16), (49, 16), (62, 15)): b.set(x, B0, z, 'barrel[facing=up]')
# weapon racks and practice posts north of the lane
for x in (24, 30, 36):                                       # archery butts: straw bales with a target on top
    b.set(x, B0, 21, 'hay_block[axis=x]'); b.set(x + 1, B0, 21, 'hay_block[axis=x]')
    b.set(x, B0 + 1, 21, 'target')
b.set(40, B0, 21, 'fletching_table'); b.set(41, B0, 21, 'barrel[facing=up]')

# stores: hall on the south side; stable: open-fronted, stalls and hay
hall(14, 65, 31, 74, 5, 'spruce_planks', 65, (20, 26))
for x, z, h in ((33, 66, 2), (34, 66, 1), (33, 67, 1), (12, 62, 1), (11, 62, 2)):
    b.box(x, B0, z, x, B0 + h - 1, z, 'barrel[facing=up]' if h == 1 else 'hay_block[axis=y]')
sx0, sx1, sz0, sz1 = 38, 61, 67, 75
for x in range(sx0, sx1 + 1):
    for z in range(sz0, sz1 + 1):
        b.set(x, G, z, b.pick([('coarse_dirt', 3), ('hay_block[axis=y]', 1)]) if z > sz0 else 'coarse_dirt')
        b.set(x, B0 + 4 + (z - sz0) // 3, z, 'dark_oak_slab[type=bottom]')          # lean-to roof rising to the wall
for x in range(sx0, sx1 + 1, 4):
    b.box(x, B0, sz0, x, B0 + 3, sz0, 'oak_log[axis=y]')
    b.box(x, B0, sz0 + 1, x, B0, sz1 - 1, 'oak_fence')
for x in (40, 48, 56): b.set(x, B0, sz1 - 1, 'hay_block[axis=x]'); b.set(x + 1, B0, sz1 - 1, 'water_cauldron[level=3]')
# tents in the north-east and south-east corners of the yard (clear of the lane)
def tent(tx, tz, door):
    """ridge tent 7 wide (x) and 8 long (z), 4 high: canvas over a ridge pole, the side's stripe along the foot,
    closed back, open front with the flaps drawn back, bedrolls and a barrel inside. door: -1 = north end, 1 = south"""
    zs = list(range(tz, tz + 8))
    front, back = (zs[0], zs[-1]) if door < 0 else (zs[-1], zs[0])
    for x in range(tx, tx + 7):
        for z in zs: b.set(x, G, z, 'coarse_dirt' if x in (tx, tx + 6) else 'spruce_planks')
    for z in zs:
        for i in range(3):
            cloth = CLOTH if i == 0 else 'white_wool'
            b.set(tx + i, B0 + i, z, cloth); b.set(tx + 6 - i, B0 + i, z, cloth)
        b.set(tx + 3, B0 + 3, z, 'stripped_spruce_log[axis=z]')                               # ridge pole
    for z in (front + door, back - door): b.set(tx + 3, B0 + 3, z, 'stripped_spruce_log[axis=z]')   # pole ends stick out
    for x in range(tx + 1, tx + 6):                               # the back wall, closed
        for y in range(B0, B0 + 3):
            if abs(x - tx - 3) + (y - B0) <= 2: b.set(x, y, back, 'white_wool')
    for x in (tx + 1, tx + 5): b.set(x, B0, front, 'white_wool')  # the front: flaps drawn to the sides, door 3 wide
    b.set(tx + 2, B0 + 1, front, 'white_wool'); b.set(tx + 4, B0 + 1, front, 'white_wool')
    inz = back + door                                             # inside, against the back wall
    for x in (tx + 1, tx + 5):
        b.set(x, B0, inz, 'hay_block[axis=z]'); b.set(x, B0, inz + door, 'hay_block[axis=z]')
    b.set(tx + 3, B0, inz, 'barrel[facing=up]')
    b.set(tx - 1, B0, front, 'barrel[facing=up]'); b.set(tx + 7, B0, front, 'spruce_planks')   # kit outside
    b.set(tx + 7, B0 + 1, front, 'lantern[hanging=false]')

tent(46, 55, -1); tent(56, 56, -1); tent(51, 17, 1)
# campfire and benches south of the lane
b.set(40, B0, 58, 'campfire[lit=true]')
for x, z in ((38, 58), (42, 58), (40, 56), (40, 60)): b.set(x, B0, z, 'spruce_slab[type=bottom]')
# well in the north-west
for ddx in (-1, 0, 1):
    for ddz in (-1, 0, 1): b.set(8 + ddx, B0, 20 + ddz, 'water' if ddx == ddz == 0 else b.pick(FOOT))

# check: the muster lane is empty from the dais steps to the front
bad = [k for k in b.c if k[1] > G and b.c[k] != 'air' and k[2] in LANE and 20 <= k[0] <= 71 and k[1] <= WT]
print('cells standing in the muster lane:', len(bad), bad[:5])
print(b.emit(sys.argv[1], 'kf-k1-base', 'K1 base camp, workshop build', pad=((-8, 40, -8), (88, 130, 96))), 'ops', b.bounds())
