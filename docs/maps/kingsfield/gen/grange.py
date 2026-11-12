"""K2 the Grange (West Grange as designed; the East Grange is its mirror in x). 56 x 56, local x 0..55, z 0..55,
ground block y 64. Point A stands at the centre (27.5, 27.5); nothing may stand within 17 of it.
Usage: python grange.py OUT.json"""
import sys, math
from kit import Build, MASON, FOOT, DAUB, FLOOR, YARD, ROOF, ROOF_SLAB
G, B0, WT = 64, 65, 69          # ground block, first built layer, wall top block (walk at y 70)
b = Build(seed=22)
S = 'stone_brick_stairs'

# ground
for x in range(0, 56):
    for z in range(0, 56):
        n = math.sin(x * 0.37 + 1) * math.cos(z * 0.31) + 0.6 * math.sin((x - z) * 0.21)
        yard = 'coarse_dirt' if n > -0.2 else ('rooted_dirt' if n > -0.7 else 'gravel')
        if b.rng.random() < 0.06: yard = 'packed_mud'
        b.set(x, G, z, 'dirt_path' if 25 <= z <= 30 else yard)

# curtain wall, 3 thick, with parapet and merlons on the outer edge
for x in range(0, 56):
    for z in range(0, 56):
        if not (x <= 2 or x >= 53 or z <= 2 or z >= 53): continue
        outer = x in (0, 55) or z in (0, 55)
        for y in range(B0, WT + 1):
            b.set(x, y, z, b.pick(FOOT) if (y == B0 and outer) else b.pick(MASON))
        if outer:
            b.set(x, WT + 1, z, b.pick(MASON))
            if (x + z) % 2 == 0 and b.rng.random() > 0.08: b.set(x, WT + 2, z, b.pick(MASON))

# corner turrets: 7 x 7, the walk runs through them, parapet one higher
for cx, cz in ((0, 0), (49, 0), (0, 49), (49, 49)):
    b.box(cx, B0, cz, cx + 6, WT, cz + 6, MASON)
    for x in range(cx, cx + 7):
        for z in range(cz, cz + 7):
            if x in (0, 55) or z in (0, 55):
                b.box(x, WT + 1, z, x, WT + 2, z, MASON)
                if (x + z) % 2 == 0: b.set(x, WT + 3, z, b.pick(MASON))
    for px, pz in ((cx, cz), (cx + 6, cz), (cx, cz + 6), (cx + 6, cz + 6)):     # roof posts; the walk stays open below
        b.box(px, WT + 1, pz, px, WT + 4, pz, 'oak_log[axis=y]')
    b.pyramid(cx - 1, cz - 1, cx + 7, cz + 7, WT + 5, ROOF, ROOF_SLAB)

# west gatehouse (the east one is mirrored below): 6 deep, 14 wide, 9 high, passage 6 wide and 5 high
GT = B0 + 8
b.box(0, B0, 21, 5, GT, 34, MASON)
b.box(0, B0, 21, 0, B0, 34, FOOT)
for x in range(0, 6):
    for z in range(21, 35):
        if x in (0, 5) or z in (21, 34):
            b.set(x, GT + 1, z, b.pick(MASON))
            if (x + z) % 2 == 0: b.set(x, GT + 2, z, b.pick(MASON))
b.air(0, B0, 25, 5, B0 + 4, 30)
for x in range(0, 6):                                   # arch shoulders
    b.set(x, B0 + 4, 25, S + '[facing=north,half=top,shape=straight]')
    b.set(x, B0 + 4, 30, S + '[facing=south,half=top,shape=straight]')
for z in range(26, 30): b.set(1, B0 + 4, z, 'iron_bars[north=true,south=true]')      # raised portcullis
for z in (23, 32):                                       # arrow loops
    b.set(0, B0 + 6, z, 'air')
    b.set(5, B0 + 6, z, 'air')
for px, pz in ((0, 21), (5, 21), (0, 34), (5, 34)): b.box(px, GT + 1, pz, px, GT + 3, pz, 'oak_log[axis=y]')
b.gable_z(0, 5, 21, 34, GT + 4, ROOF, ROOF_SLAB, over=1)
for z in (24, 31):
    b.box(6, B0, z, 6, B0 + 2, z, 'oak_fence')
    b.set(6, B0 + 3, z, 'lantern[hanging=false]')

# stairs to the wall walk, north of the gatehouse, 2 wide, straight
for i in range(5):
    z = 20 - i
    for x in (3, 4):
        if i: b.box(x, B0, z, x, B0 + i - 1, z, MASON)
        b.set(x, B0 + i, z, S + '[facing=north,half=bottom,shape=straight]')
b.box(3, B0, 13, 4, WT, 15, MASON)                       # landing joining the walk

b.mirror_x(55)

# side doors north and south, 3 wide, 3 high
for z0 in (0, 53):
    b.air(27, B0, z0, 29, B0 + 2, z0 + 2)
    for z in range(z0, z0 + 3):
        b.set(27, B0 + 2, z, S + '[facing=west,half=top,shape=straight]')
        b.set(29, B0 + 2, z, S + '[facing=east,half=top,shape=straight]')
        for x in (27, 28, 29): b.set(x, G, z, 'dirt_path')

# hall (the Siege keep): 12 x 9 in the north-west corner, stone below, timber frame above
hx0, hx1, hz0, hz1 = 6, 17, 5, 12
b.box(hx0, G, hz0, hx1, G, hz1, FLOOR)
for x in range(hx0, hx1 + 1):
    for z in range(hz0, hz1 + 1):
        if x in (hx0, hx1) or z in (hz0, hz1):
            post = (x in (hx0, hx1) and z in (hz0, hz1)) or (z in (hz0, hz1) and (x - hx0) % 4 == 0)
            for y in range(B0, B0 + 5):
                blk = b.pick(MASON) if y < B0 + 2 else ('oak_log[axis=y]' if post else DAUB)
                if y == B0 + 4: blk = 'stripped_oak_log[axis=%s]' % ('x' if z in (hz0, hz1) else 'z')
                b.set(x, y, z, blk)
b.air(11, B0, hz1, 12, B0 + 1, hz1)                      # door to the yard
for x in (8, 15): b.set(x, B0 + 2, hz1, 'iron_bars[east=true,west=true]')
for x in (8, 12, 15): b.set(x, B0 + 2, hz0, 'iron_bars[east=true,west=true]')
b.gable_x(hx0, hx1, hz0, hz1, B0 + 5, ROOF, ROOF_SLAB, fill=[(DAUB, 1)])
b.box(hx0 - 1, B0, 8, hx0 - 1, B0 + 10, 8, FOOT)         # chimney
b.set(hx0 - 1, B0 + 11, 8, 'cobblestone_wall')
b.set(10, B0, hz1 + 1, 'barrel[facing=up]')
b.set(13, B0, hz1 + 1, 'barrel[facing=up]')
b.set(13, B0 + 1, hz1 + 1, 'lantern[hanging=false]')

# granary: 7 x 7, tall, south-west corner
gx0, gx1, gz0, gz1 = 6, 12, 44, 50
b.box(gx0, G, gz0, gx1, G, gz1, FLOOR)
for x in range(gx0, gx1 + 1):
    for z in range(gz0, gz1 + 1):
        if x in (gx0, gx1) or z in (gz0, gz1):
            corner = x in (gx0, gx1) and z in (gz0, gz1)
            for y in range(B0, B0 + 7):
                b.set(x, y, z, b.pick(FOOT) if y < B0 + 3 else ('oak_log[axis=y]' if corner else 'spruce_planks'))
b.air(9, B0, gz0, 9, B0 + 1, gz0)
b.set(9, B0 + 5, gz0, 'air')                             # loft hatch
b.gable_z(gx0, gx1, gz0, gz1, B0 + 7, 'spruce_stairs', 'spruce_slab', fill=[('spruce_planks', 1)])

# cart shed: open lean-to against the south wall, south-east corner
for x in range(37, 51):
    for z in range(47, 53):
        b.set(x, B0 + 3, z, 'dark_oak_slab[type=bottom]' if z == 47 else 'dark_oak_slab[type=top]')
for x in (37, 41, 46, 50): b.box(x, B0, 48, x, B0 + 2, 48, 'oak_log[axis=y]')
for x, z, h in ((39, 51, 2), (40, 51, 1), (39, 50, 1), (48, 51, 2), (47, 52, 1)):
    b.box(x, B0, z, x, B0 + h - 1, z, 'hay_block[axis=y]')
b.set(43, B0, 51, 'composter')
b.set(44, B0, 52, 'barrel[facing=up]')
for x in (43, 44, 45): b.set(x, B0, 50, 'oak_log[axis=x]')            # a log pile
b.set(44, B0 + 1, 50, 'oak_log[axis=x]')

# well, north-east corner
wx, wz = 45, 9
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        b.set(wx + dx, B0, wz + dz, 'water' if dx == dz == 0 else b.pick(FOOT))
for dx, dz in ((-1, -1), (1, 1), (-1, 1), (1, -1)):
    b.box(wx + dx, B0 + 1, wz + dz, wx + dx, B0 + 2, wz + dz, 'oak_fence')
b.box(wx - 1, B0 + 3, wz - 1, wx + 1, B0 + 3, wz + 1, 'dark_oak_slab[type=bottom]')
b.set(41, B0, 6, 'cauldron')

# pig pen, north-east corner
for x in range(42, 51):
    for z in (13, 17): b.set(x, B0, z, 'oak_fence')
for z in range(13, 18):
    for x in (42, 50): b.set(x, B0, z, 'oak_fence')
b.set(46, B0, 17, 'oak_fence_gate[facing=south,open=false]')
for x in range(43, 50):
    for z in range(14, 17): b.set(x, G, z, b.pick([('mud', 3), ('coarse_dirt', 2)]))
b.box(48, B0, 14, 49, B0, 14, 'hay_block[axis=x]')
# woodpile by the hall, sacks by the granary
for x in (19, 20, 21): b.box(x, B0, 5, x, B0 + (1 if x < 21 else 0), 5, 'oak_log[axis=z]')
b.set(14, B0, 46, 'barrel[facing=up]')
b.set(14, B0, 47, 'hay_block[axis=y]')

# ---- furnishing and dressing ---------------------------------------------------------------------------------
def table(x, z):
    b.set(x, B0, z, 'spruce_stairs[facing=west,half=top,shape=straight]'); b.set(x + 1, B0, z, 'spruce_stairs[facing=east,half=top,shape=straight]')
# hall (inside x 7..16, z 6..11): a long board with benches, the hearth under the stack, a bed, chests
for x in (10, 12):
    table(x, 8)
    for k in (0, 1):
        b.set(x + k, B0, 7, 'spruce_stairs[facing=north,half=bottom,shape=straight]'); b.set(x + k, B0, 9, 'spruce_stairs[facing=south,half=bottom,shape=straight]')
b.set(11, B0 + 1, 8, 'lantern[hanging=false]')
for z in (7, 9): b.box(hx0, B0, z, hx0, B0 + 1, z, 'bricks')
b.set(hx0, B0 + 1, 8, 'bricks'); b.set(hx0, B0, 8, 'campfire[lit=true]'); b.set(hx0 + 1, G, 8, 'bricks')
b.set(7, B0, 10, 'red_bed[facing=south,part=foot]'); b.set(7, B0, 11, 'red_bed[facing=south,part=head]')
b.set(16, B0, 6, 'chest[facing=west]'); b.set(16, B0, 7, 'barrel[facing=up]'); b.set(16, B0, 10, 'barrel[facing=up]'); b.set(16, B0, 11, 'barrel[facing=up]')
b.set(16, B0 + 1, 11, 'barrel[facing=up]'); b.set(7, B0, 6, 'oak_log[axis=x]'); b.set(8, B0, 6, 'oak_log[axis=x]')
for x in (11, 12):
    for z in (10, 11): b.set(x, B0, z, 'red_carpet')
for z in range(hz0 + 1, hz1): b.set(14, B0 + 4, z, 'stripped_oak_log[axis=z]')
b.set(14, B0 + 3, 9, 'lantern[hanging=true]'); b.set(12, B0 + 3, hz0 + 1, 'white_wall_banner[facing=south]')
for x in (8, 15):                                           # shutters by the yard windows, a lintel over the door
    for k in (-1, 1): b.set(x + k, B0 + 2, hz1 + 1, 'spruce_trapdoor[facing=south,half=top,open=true]')
for x in (11, 12): b.set(x, B0 + 2, hz1, 'stripped_oak_log[axis=x]')
for x in range(hx0, hx1 + 1, 4):
    b.set(x, B0 + 4, hz1 + 1, 'spruce_stairs[facing=north,half=top,shape=straight]')
# granary (inside x 7..11, z 45..49): barrels, sacks and straw below, a loft reached by a ladder, a hoist over the door
for z in range(45, 50):
    b.set(7, B0, z, 'barrel[facing=up]')
    if z % 2: b.set(7, B0 + 1, z, 'barrel[facing=up]')
for z in (47, 48, 49): b.set(11, B0, z, 'hay_block[axis=z]')
b.set(11, B0 + 1, 49, 'hay_block[axis=z]'); b.set(11, B0 + 1, 48, 'hay_block[axis=x]')
for x in (8, 9, 10): b.set(x, B0, 49, 'white_wool')
b.set(9, B0 + 1, 49, 'white_wool')
for x in range(7, 12):
    for z in range(45, 50): b.set(x, B0 + 4, z, 'spruce_planks')
for y in range(B0, B0 + 5): b.set(11, y, 45, 'ladder[facing=west]')
b.set(8, B0 + 5, 48, 'hay_block[axis=x]'); b.set(8, B0 + 5, 49, 'hay_block[axis=x]'); b.set(7, B0 + 5, 49, 'white_wool'); b.set(10, B0 + 5, 49, 'barrel[facing=up]')
b.set(9, B0 + 2, gz0, 'stripped_spruce_log[axis=x]')
b.set(9, B0 + 6, gz0 - 1, 'stripped_spruce_log[axis=z]'); b.set(9, B0 + 5, gz0 - 1, 'chain[axis=y]'); b.set(9, B0 + 4, gz0 - 1, 'chain[axis=y]')
# smithy: a lean-to against the north wall, east of the side door
for x in range(31, 39):
    for z in range(3, 8):
        b.set(x, B0 + 3, z, 'dark_oak_slab[type=bottom]' if z == 7 else 'dark_oak_slab[type=top]'); b.set(x, G, z, 'gravel')
for x in (31, 38): b.box(x, B0, 7, x, B0 + 2, 7, 'oak_log[axis=y]')
b.set(32, B0, 3, 'water_cauldron[level=3]'); b.set(34, B0, 3, 'blast_furnace[facing=south]'); b.set(35, B0, 3, 'coal_block')
b.set(37, B0, 3, 'smithing_table'); b.set(34, B0, 5, 'anvil[facing=east]'); b.set(37, B0, 5, 'grindstone[face=floor,facing=south]')
b.set(36, B0, 3, 'barrel[facing=up]'); b.set(34, B0 + 2, 6, 'lantern[hanging=true]')
# a round hay rick by the cart shed, a trough in the pen
for x in range(33, 38):
    for z in range(45, 50):
        d = math.hypot(x - 35, z - 47)
        for y in range(B0, B0 + (3 if d < 1.2 else 2 if d < 2.3 else 0)): b.set(x, y, z, 'hay_block[axis=y]')
b.set(35, B0 + 3, 47, "hay_block[axis=y]")
b.set(43, B0, 14, 'water_cauldron[level=3]')
# buttresses on the outer face of the curtain
for k in (10, 18, 37, 45):
    for x, f in ((-1, 'east'), (56, 'west')):
        b.box(x, B0, k, x, B0 + 2, k, FOOT); b.set(x, B0 + 3, k, 'stone_brick_stairs[facing=%s,half=bottom,shape=straight]' % f)
    for z, f in ((-1, 'south'), (56, 'north')):
        b.box(k, B0, z, k, B0 + 2, z, FOOT); b.set(k, B0 + 3, z, 'stone_brick_stairs[facing=%s,half=bottom,shape=straight]' % f)
# the point: a stone ring let into the yard on the capture line, a compass in the middle, four white standards outside it
for x in range(8, 48):
    for z in range(8, 48):
        d = math.hypot(x - 27.5, z - 27.5)
        if 15.0 <= d < 17.0: b.set(x, G, z, 'smooth_stone' if d < 16 else ('polished_andesite' if (x + z) % 5 else 'chiseled_stone_bricks'))
        elif d < 3.3 or (d < 6.5 and (abs(x - 27.5) < 1 or abs(z - 27.5) < 1)): b.set(x, G, z, 'chiseled_stone_bricks' if d < 1 else ('smooth_stone' if d >= 3.3 else 'polished_andesite'))
for x, z, k in ((14, 14, -1), (41, 14, -1), (14, 41, 1), (41, 41, 1)):
    b.box(x, B0, z, x, B0 + 9, z, 'stripped_spruce_log[axis=y]'); b.set(x, B0 + 10, z, 'lantern[hanging=false]')
    for y in range(B0 + 6, B0 + 10):
        for j in (1, 2, 3): b.set(x, y, z + j * k, 'white_wool')
    for y in (B0 + 7, B0 + 8): b.set(x, y, z + 4 * k, 'white_wool')

# check: nothing above ground within 17.5 of the point
bad = [k for k in b.c if k[1] > G and b.c[k] != 'air' and math.hypot(k[0] - 27.5, k[2] - 27.5) < 17.0]
print('cells inside the capture ring:', len(bad), bad[:5])
print(b.emit(sys.argv[1], 'kf-k2-grange', 'K2 West Grange, workshop build'), 'ops', b.bounds())
