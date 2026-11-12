"""Supporting pieces S1, S2, S4a, S4b, S5, S7 laid side by side on one sampler pad (each has its origin at its own
north-west corner, ground block y 64). Prints one export box per piece.
Usage: python small.py OUT.json"""
import sys
from kit import Build, FOOT, MASON, DAUB, FLOOR, ROOF, ROOF_SLAB
G, B0 = 64, 65
b = Build(seed=7)


def frame(ox, oz, w, d, h, wall, stone=1, doors=(), windows=(), roof='x', roof_blocks=(ROOF, ROOF_SLAB), posts=4):
    """timber-framed house w x d, walls h high, on a stone course; doors/windows: (x, z) cells relative to the piece"""
    x0, x1, z0, z1 = ox, ox + w - 1, oz, oz + d - 1
    b.box(x0, G, z0, x1, G, z1, FLOOR)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)): continue
            post = (x in (x0, x1) and z in (z0, z1)) or (z in (z0, z1) and (x - x0) % posts == 0) or (x in (x0, x1) and (z - z0) % posts == 0)
            for y in range(B0, B0 + h):
                blk = b.pick(FOOT) if y < B0 + stone else ('oak_log[axis=y]' if post else wall)
                if y == B0 + h - 1: blk = 'stripped_oak_log[axis=%s]' % ('x' if z in (z0, z1) else 'z')
                b.set(x, y, z, blk)
    for dx, dz in doors: b.air(ox + dx, B0, oz + dz, ox + dx, B0 + 1, oz + dz)
    for dx, dz in windows: b.set(ox + dx, B0 + 2, oz + dz, 'air')
    if roof == 'x': b.gable_x(x0, x1, z0, z1, B0 + h, roof_blocks[0], roof_blocks[1], fill=[(wall, 1)])
    else: b.gable_z(x0, x1, z0, z1, B0 + h, roof_blocks[0], roof_blocks[1], fill=[(wall, 1)])


def s1_beacon(ox, oz):
    """beacon tower 7 x 7, 14 high, open fire basket on top, door on the south"""
    top = B0 + 11
    b.box(ox, G, oz, ox + 6, G, oz + 6, FOOT)
    for x in range(ox, ox + 7):
        for z in range(oz, oz + 7):
            if x in (ox, ox + 6) or z in (oz, oz + 6):
                for y in range(B0, top + 1): b.set(x, y, z, b.pick(FOOT) if y < B0 + 3 else b.pick(MASON))
            else: b.set(x, top, z, 'spruce_planks')
    b.air(ox + 3, B0, oz + 6, ox + 3, B0 + 1, oz + 6)
    for y in (B0 + 5, B0 + 9):
        b.set(ox + 3, y, oz, 'air'); b.set(ox, y, oz + 3, 'air'); b.set(ox + 6, y, oz + 3, 'air'); b.set(ox + 3, y, oz + 6, 'air')
    for x in range(ox - 1, ox + 8):                              # corbelled parapet
        for z in range(oz - 1, oz + 8):
            if x in (ox - 1, ox + 7) or z in (oz - 1, oz + 7):
                b.set(x, top, z, 'stone_brick_slab[type=top]')
                b.set(x, top + 1, z, b.pick(MASON))
                if (x + z) % 2 == 0: b.set(x, top + 2, z, 'stone_brick_wall')
    b.box(ox + 3, top + 1, oz + 3, ox + 3, top + 2, oz + 3, 'cobblestone_wall')    # fire basket
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)): b.set(ox + 3 + dx, top + 3, oz + 3 + dz, 'iron_bars')
    b.set(ox + 3, top + 3, oz + 3, 'netherrack'); b.set(ox + 3, top + 4, oz + 3, 'fire')
    for y in range(B0, top): b.set(ox + 1, y, oz + 1, 'ladder[facing=south]')
    b.set(ox + 1, top, oz + 1, 'air')
    return (ox - 1, G, oz - 1, ox + 7, top + 4, oz + 7)


def s2_lodge(ox, oz):
    """mason's lodge 12 x 9: stone-built, lean-to work shelter on the east end with a banker stone"""
    frame(ox, oz, 9, 9, 5, 'stone_bricks', stone=2, doors=((4, 8),), windows=((2, 8), (6, 8), (0, 4), (4, 0)), roof='z')
    for x in range(ox + 9, ox + 12):
        for z in range(oz + 1, oz + 8): b.set(x, B0 + 3, z, 'dark_oak_slab[type=bottom]'); b.set(x, G, z, 'gravel')
    for z in (oz + 1, oz + 7): b.box(ox + 11, B0, z, ox + 11, B0 + 2, z, 'oak_fence')
    b.set(ox + 10, B0, oz + 4, 'smooth_stone'); b.set(ox + 10, B0, oz + 3, 'stone_brick_stairs[facing=south,half=bottom,shape=straight]')
    b.set(ox + 10, B0, oz + 6, 'chiseled_stone_bricks')
    return (ox - 1, G, oz - 1, ox + 12, B0 + 11, oz + 9)


def s4a_cottage(ox, oz):
    """farm cottage 9 x 7, door south, chimney on the west gable"""
    frame(ox, oz, 9, 7, 4, DAUB, doors=((4, 6),), windows=((2, 6), (6, 6), (4, 0)), roof='x')
    b.box(ox - 1, B0, oz + 3, ox - 1, B0 + 8, oz + 3, FOOT); b.set(ox - 1, B0 + 9, oz + 3, 'cobblestone_wall')
    b.set(ox + 6, B0, oz + 7, 'barrel[facing=up]')
    return (ox - 1, G, oz - 1, ox + 9, B0 + 9, oz + 7)


def s4b_cottage(ox, oz):
    """longer cottage 9 x 11 with a byre end, ridge north-south, door east"""
    frame(ox, oz, 9, 11, 4, DAUB, doors=((8, 3),), windows=((8, 6), (0, 3), (0, 7), (4, 0)), roof='z')
    for x in range(ox + 1, ox + 8): b.set(x, B0, oz + 7, 'oak_fence')              # byre partition
    b.set(ox + 4, B0, oz + 7, 'air')
    b.air(ox + 4, B0, oz + 10, ox + 5, B0 + 1, oz + 10)                              # byre door, south
    b.set(ox + 2, B0, oz + 9, 'hay_block[axis=x]'); b.set(ox + 9, B0, oz + 5, 'composter')
    return (ox - 1, G, oz - 1, ox + 9, B0 + 10, oz + 11)


def s5_haybarn(ox, oz):
    """hay barn / cart shed 12 x 8: open on the south, boarded on three sides"""
    x0, x1, z0, z1 = ox, ox + 11, oz, oz + 7
    b.box(x0, G, z0, x1, G, z1, [('coarse_dirt', 3), ('packed_mud', 1)])
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if z == z1:
                if (x - x0) % 4 == 0 or x == x1: b.box(x, B0, z, x, B0 + 3, z, 'oak_log[axis=y]')
            elif x in (x0, x1) or z == z0:
                for y in range(B0, B0 + 4): b.set(x, y, z, 'oak_log[axis=y]' if (x in (x0, x1) and z == z0) else b.pick([('spruce_planks', 5), ('air', 1)]) if y > B0 else 'spruce_planks')
    for x in range(x0, x1 + 1): b.set(x, B0 + 3, z1, 'stripped_oak_log[axis=x]')
    b.gable_x(x0, x1, z0, z1, B0 + 4, 'spruce_stairs', 'spruce_slab')
    for x in range(x0 + 1, x0 + 5):
        for z in range(z0 + 1, z0 + 5):
            for y in range(B0, B0 + max(0, 3 - (x - x0 - 1) // 2 - (z - z0 - 1) // 2)): b.set(x, y, z, 'hay_block[axis=y]')
    for x in (x0 + 7, x0 + 8, x0 + 9): b.set(x, B0, z0 + 3, 'spruce_slab[type=top]')      # cart bed
    for x in (x0 + 7, x0 + 9):
        b.set(x, B0, z0 + 2, 'spruce_trapdoor[facing=north,half=bottom,open=true]'); b.set(x, B0, z0 + 4, 'spruce_trapdoor[facing=south,half=bottom,open=true]')
    return (ox - 1, G, oz - 1, ox + 12, B0 + 9, oz + 8)


def s7_cross(ox, oz):
    """wayside cross on a stepped plinth, 3 x 3"""
    b.box(ox, G, oz, ox + 2, G, oz + 2, FOOT)
    for x in range(ox, ox + 3):
        for z in range(oz, oz + 3): b.set(x, B0, z, 'cobblestone_slab[type=bottom]' if (x, z) != (ox + 1, oz + 1) else 'mossy_cobblestone')
    b.box(ox + 1, B0 + 1, oz + 1, ox + 1, B0 + 3, oz + 1, 'cobblestone_wall')
    b.set(ox + 1, B0 + 4, oz + 1, 'chiseled_stone_bricks')
    b.set(ox, B0 + 3, oz + 1, 'stone_brick_wall'); b.set(ox + 2, B0 + 3, oz + 1, 'stone_brick_wall')
    return (ox, G, oz, ox + 2, B0 + 4, oz + 2)


boxes = {'s1_beacon': s1_beacon(2, 2), 's2_lodge': s2_lodge(14, 2), 's4a_cottage': s4a_cottage(32, 2),
         's4b_cottage': s4b_cottage(46, 2), 's5_haybarn': s5_haybarn(2, 18), 's7_cross': s7_cross(20, 20)}
n = b.emit(sys.argv[1], 'kf-small', 'Kingsfield supporting pieces sampler', pad=((-8, 40, -8), (72, 120, 40)))
for k, v in boxes.items(): print(k, *v)
print(n, 'ops', b.bounds())
