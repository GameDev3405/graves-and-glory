"""S9 the toll house (Rivergate's neutral hold). 24 x 24, local x 0..23, z 0..23, ground block y 64.
Gate in the south wall (x 10..13, z 23), yard centre (12, 15) with 6 clear around it, toll house on the north side,
stable lean-to in the north-east. Same data as the Kingsfield hold: hold centre (12, 15), yard radius 6, gate cells
x 10..13 z 23, garrison spawn cells x 4..10 z 11..12. toll() returns a Build. Usage: python toll.py OUT.json"""
import sys
from kit import Build, MASON, FOOT
from houses import house

G, B0 = 64, 65
GATE = range(10, 14)


def toll():
    b = Build(seed=88)
    for x in range(24):
        for z in range(24):
            b.set(x, G, z, b.pick([('cobblestone', 40), ('gravel', 25), ('coarse_dirt', 20), ('stone', 15)]))
            if not (x in (0, 23) or z in (0, 23)) or (z == 23 and x in GATE): continue
            for y in range(B0, B0 + 3): b.set(x, y, z, b.pick(FOOT) if y == B0 else b.pick(MASON))
            b.set(x, B0 + 3, z, 'stone_brick_slab[type=bottom]' if (x + z) % 4 else 'stone_bricks')
    for x in (GATE[0] - 1, GATE[-1] + 1):
        b.box(x, B0, 23, x, B0 + 4, 23, MASON); b.set(x, B0 + 5, 23, 'stone_brick_wall'); b.set(x, B0 + 6, 23, 'lantern[hanging=false]')
    b.box(GATE[0] - 1, B0 + 4, 23, GATE[-1] + 1, B0 + 4, 23, 'stripped_dark_oak_log[axis=x]')
    for x in (GATE[0] - 1, GATE[-1] + 1): b.set(x, B0 + 4, 23, 'chiseled_stone_bricks')
    # the toll bar, swung open against the wall inside
    for z in (22, 21, 20, 19): b.set(GATE[0] - 1, B0 + 1, z, 'spruce_fence') if z < 22 else b.set(GATE[0] - 1, B0, z, 'spruce_fence')
    b.set(GATE[0] - 1, B0 + 1, 22, 'spruce_fence')
    # toll house: brick, two floors, front to the yard
    hb = house(11, 8, floors=2, style='brick', door=5, seed=3)
    for (x, y, z), v in hb.c.items(): b.set(x + 2, y, z + 2, v)
    # stable lean-to in the north-east
    for x in range(16, 22):
        for z in range(2, 7):
            edge = x in (16, 21) or z == 2
            if edge and not (x in (16, 21) and z > 4):
                for y in range(B0, B0 + 3): b.set(x, y, z, 'spruce_planks' if not (x in (16, 21) and z == 2) else 'spruce_log[axis=y]')
            b.set(x, B0 + 3 if z < 5 else B0 + 2, z, 'spruce_slab[type=%s]' % ('bottom' if z < 5 else 'top'))
    for x in (16, 21): b.box(x, B0, 6, x, B0 + 1, 6, 'spruce_fence')
    for x, blk in ((17, 'hay_block[axis=x]'), (18, 'hay_block[axis=z]'), (20, 'water_cauldron[level=3]')): b.set(x, B0, 3, blk)
    b.set(17, B0 + 1, 3, 'hay_block[axis=x]'); b.set(19, B0 + 2, 4, 'lantern[hanging=true]')
    # the collector's table by the gate, strongbox, stacks along the walls, clear of the yard ring
    b.set(15, B0, 21, 'spruce_fence'); b.set(15, B0 + 1, 21, 'spruce_pressure_plate'); b.set(16, B0, 21, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
    b.set(15, B0, 22, 'chest[facing=north]'); b.set(16, B0, 22, 'barrel[facing=up]')
    for x, z, h in ((20, 10, 2), (21, 10, 1), (21, 11, 2), (21, 12, 1)):
        for y in range(h): b.set(x, B0 + y, z, 'barrel[facing=up]')
    for z in (18, 19, 20): b.set(21, B0, z, 'stripped_spruce_log[axis=z]')
    b.set(2, B0, 19, 'hay_block[axis=x]'); b.set(2, B0 + 1, 19, 'hay_block[axis=x]'); b.set(2, B0, 20, 'hay_block[axis=z]'); b.set(3, B0, 20, 'barrel[facing=up]')
    b.set(2, B0, 13, 'grindstone[face=floor,facing=east]'); b.set(19, B0, 16, 'campfire[lit=false]')
    return b


if __name__ == '__main__':
    b = toll()
    for x in range(-6, 30):
        for z in range(-6, 32):
            if b.get(x, G, z) is None: b.set(x, G, z, 'grass_block' if not (9 <= x <= 14 and z > 23) else 'dirt_path')
            b.set(x, G - 1, z, 'dirt')
    print(b.emit(sys.argv[1], 'rg-s9-toll', 'S9 toll house, workshop build', pad=((-8, G - 2, -8), (32, 100, 34))), 'ops')
    print('s9_toll_ws -6 63 -6 29 84 31')
