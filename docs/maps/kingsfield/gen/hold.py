"""S9 the neutral hold (Kingsfield: the quarrymen's steading). 24 x 24, local x 0..23, z 0..23, ground block y 64.
Gate in the south wall (z 23), yard centre (12, 15) with 6 clear around it, hall on the north side, stores shed in
the north-east. Data for the mode code: hold centre (12, 15), yard radius 6, gate cells x 10..13 z 23, garrison
spawn cells x 4..10 z 10..11. Usage: python hold.py OUT.json"""
import sys, math
from kit import Build, FOOT, MASON, DAUB, FLOOR, ROOF, ROOF_SLAB
G, B0 = 64, 65
b = Build(seed=99)
GATE = range(10, 14)

for x in range(24):
    for z in range(24):
        b.set(x, G, z, b.pick([('coarse_dirt', 5), ('packed_mud', 3), ('gravel', 2)]))
for z in range(14, 24):
    for x in GATE: b.set(x, G, z, 'dirt_path')

# drystone wall 3 high with a rough top, gate 4 wide with a timber frame
for x in range(24):
    for z in range(24):
        if not (x in (0, 23) or z in (0, 23)) or (z == 23 and x in GATE): continue
        h = 3 + (1 if b.rng.random() < 0.3 else 0)
        for y in range(B0, B0 + h): b.set(x, y, z, b.pick(FOOT))
        if h == 3 and b.rng.random() < 0.25: b.set(x, B0 + 3, z, 'cobblestone_slab[type=bottom]')
for x in (GATE[0] - 1, GATE[-1] + 1): b.box(x, B0, 23, x, B0 + 4, 23, 'oak_log[axis=y]')
b.box(GATE[0] - 1, B0 + 5, 23, GATE[-1] + 1, B0 + 5, 23, 'stripped_oak_log[axis=x]')
for x in (GATE[0] - 1, GATE[-1] + 1): b.set(x, B0 + 6, 23, 'lantern[hanging=false]')
for x in GATE: b.set(x, B0 + 4, 23, 'yellow_wool' if x in (GATE[0], GATE[-1]) else 'white_wool')   # a cloth valance right across, under the lintel

# hall: 12 x 7, stone below, daub above
hx0, hx1, hz0, hz1 = 2, 13, 2, 8
b.box(hx0, G, hz0, hx1, G, hz1, FLOOR)
for x in range(hx0, hx1 + 1):
    for z in range(hz0, hz1 + 1):
        if not (x in (hx0, hx1) or z in (hz0, hz1)): continue
        post = (x in (hx0, hx1) and z in (hz0, hz1)) or (z in (hz0, hz1) and (x - hx0) % 4 == 3)
        for y in range(B0, B0 + 5):
            blk = b.pick(MASON) if y < B0 + 2 else ('oak_log[axis=y]' if post else DAUB)
            if y == B0 + 4: blk = 'stripped_oak_log[axis=%s]' % ('x' if z in (hz0, hz1) else 'z')
            b.set(x, y, z, blk)
b.air(7, B0, hz1, 8, B0 + 1, hz1)
for x in (4, 11): b.set(x, B0 + 2, hz1, 'iron_bars[east=true,west=true]')
b.gable_x(hx0, hx1, hz0, hz1, B0 + 5, ROOF, ROOF_SLAB, fill=[(DAUB, 1)])
b.box(hx0 - 1, B0, 5, hx0 - 1, B0 + 9, 5, FOOT); b.set(hx0 - 1, B0 + 10, 5, 'cobblestone_wall')

# stores shed: 6 x 5, boarded, lean-to roof
sx0, sx1, sz0, sz1 = 16, 21, 2, 6
for x in range(sx0, sx1 + 1):
    for z in range(sz0, sz1 + 1):
        if x in (sx0, sx1) or z in (sz0, sz1):
            for y in range(B0, B0 + 3): b.set(x, y, z, 'spruce_planks' if not (x in (sx0, sx1) and z in (sz0, sz1)) else 'oak_log[axis=y]')
        b.set(x, B0 + 3 + (1 if z <= sz0 + 1 else 0), z, 'dark_oak_slab[type=%s]' % ('bottom' if z > sz0 + 1 else 'bottom'))
b.air(18, B0, sz1, 19, B0 + 1, sz1)
for x, z, blk in ((17, 3, 'barrel[facing=up]'), (20, 3, 'barrel[facing=up]'), (20, 4, 'hay_block[axis=y]')): b.set(x, B0, z, blk)

# quarrymen's clutter along the east and west walls, clear of the yard ring
for x, z, h in ((20, 10, 2), (21, 10, 1), (21, 11, 2), (20, 12, 1)): b.box(x, B0, z, x, B0 + h - 1, z, 'stone_bricks' if h == 2 else 'andesite')
for z in (18, 19, 20): b.set(21, B0, z, 'stripped_oak_log[axis=z]')
b.set(2, B0, 19, 'hay_block[axis=x]'); b.set(2, B0 + 1, 19, 'hay_block[axis=x]'); b.set(2, B0, 20, 'hay_block[axis=z]'); b.set(3, B0, 20, 'barrel[facing=up]')
b.set(18, B0, 9, 'campfire[lit=true]')
b.set(2, B0, 12, 'grindstone[face=floor,facing=east]')

# ---- furnishing and dressing ---------------------------------------------------------------------------------
# hall (inside x 3..12, z 3..7): two cots, a board with seats, the hearth under the stack
for x in (3, 12):
    b.set(x, B0, 4, 'yellow_bed[facing=north,part=foot]'); b.set(x, B0, 3, 'yellow_bed[facing=north,part=head]')
b.set(9, B0, 5, 'spruce_stairs[facing=west,half=top,shape=straight]'); b.set(10, B0, 5, 'spruce_stairs[facing=east,half=top,shape=straight]')
b.set(9, B0 + 1, 5, 'lantern[hanging=false]')
for x in (9, 10): b.set(x, B0, 4, 'spruce_stairs[facing=north,half=bottom,shape=straight]'); b.set(x, B0, 6, 'spruce_stairs[facing=south,half=bottom,shape=straight]')
for z in (4, 6): b.box(hx0, B0, z, hx0, B0 + 1, z, 'bricks')
b.set(hx0, B0 + 1, 5, 'bricks'); b.set(hx0, B0, 5, 'campfire[lit=true]'); b.set(hx0 + 1, G, 5, 'bricks')
b.set(12, B0, 7, 'chest[facing=west]'); b.set(12, B0, 6, 'barrel[facing=up]'); b.set(4, B0, 3, 'barrel[facing=up]'); b.set(3, B0, 7, 'oak_log[axis=x]')
for x in (7, 8): b.set(x, B0, 7, 'yellow_carpet'); b.set(x, B0 + 2, hz1, 'stripped_oak_log[axis=x]')
for z in range(hz0 + 1, hz1): b.set(6, B0 + 4, z, 'stripped_oak_log[axis=z]')
b.set(6, B0 + 3, 5, 'lantern[hanging=true]')
for x in (4, 11):
    for k in (-1, 1): b.set(x + k, B0 + 2, hz1 + 1, 'spruce_trapdoor[facing=south,half=top,open=true]')
for x in (5, 9, 13): b.set(x, B0 + 4, hz1 + 1, 'spruce_stairs[facing=north,half=top,shape=straight]')
# shed: more stores
b.set(17, B0 + 1, 3, 'barrel[facing=up]'); b.set(17, B0, 4, 'barrel[facing=up]'); b.set(20, B0, 5, 'hay_block[axis=x]'); b.set(20, B0 + 1, 4, 'hay_block[axis=y]')
b.set(17, B0, 5, 'chest[facing=east]')
# lookout in the south-west corner: a deck on four posts over the straw, a ladder, a rail
for x, z in ((1, 19), (4, 19), (1, 22), (4, 22)): b.box(x, B0, z, x, B0 + 3, z, 'oak_log[axis=y]')
for x in range(1, 5):
    for z in range(19, 23): b.set(x, B0 + 4, z, 'spruce_planks')
for y in range(B0, B0 + 5): b.set(4, y, 20, 'ladder[facing=south]')
for x in range(1, 5): b.set(x, B0 + 5, 19, 'oak_fence')
for z in (21, 22): b.set(4, B0 + 5, z, 'oak_fence')
b.set(1, B0 + 5, 22, 'barrel[facing=up]')
# well by the east wall
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1): b.set(21 + dx, B0, 15 + dz, 'water' if dx == dz == 0 else b.pick(FOOT))
for z in (14, 16): b.box(21, B0 + 1, z, 21, B0 + 2, z, 'oak_log[axis=y]')
for z in (14, 15, 16):
    b.set(21, B0 + 3, z, 'stripped_oak_log[axis=z]')
    b.set(20, B0 + 3, z, 'spruce_stairs[facing=east,half=bottom,shape=straight]'); b.set(22, B0 + 3, z, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
    b.set(21, B0 + 4, z, 'spruce_slab[type=bottom]')
b.set(21, B0 + 2, 15, 'chain[axis=y]')

bad = [k for k in b.c if k[1] > G and b.c[k] != 'air' and math.hypot(k[0] - 12, k[2] - 15) < 6.0]
print('cells inside the yard ring:', len(bad), bad[:5])
print(b.emit(sys.argv[1], 'kf-s9-hold', 'S9 neutral hold, workshop build', pad=((-8, 40, -8), (40, 120, 40))), 'ops', b.bounds())
