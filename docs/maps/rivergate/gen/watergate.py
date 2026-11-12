"""K1 the water gate (Dawn base), in map coordinates: x -251..-180, z -40..39 (72 x 80), base centre (-215, 0).
The river (z -9..8) runs through it in a walled channel, grilled at both walls. A paved bridge court 20 wide carries
the base centre over the water. One gate on each bank in the east (field) wall, 8 wide, between twin towers 13 high.
Curtain wall 5 high with merlons and a railed wall walk. Barracks on the north bank, armoury and smithy on the south.
build(b, colour) adds it to a Build; the Dusk base is the x mirror in purple. Usage: python watergate.py OUT.json"""
import sys
from kit import Build, MASON, FOOT

G, BED, WATER = 64, 59, 62
X0, X1, Z0, Z1 = -251, -180, -40, 39
STREET = [('cobblestone', 55), ('stone', 20), ('andesite', 20), ('mossy_cobblestone', 5)]
YARD = [('cobblestone', 30), ('gravel', 25), ('coarse_dirt', 25), ('stone', 10), ('andesite', 10)]
FLAGS = [('stone_bricks', 45), ('andesite', 25), ('polished_andesite', 20), ('cobblestone', 10)]
SWAP = {'north': 'south', 'south': 'north'}


def build(b, colour='cyan'):
    def M(x, y, z, blk, both=True):
        """set on the north bank and its mirror on the south bank (z -> -1 - z), turning north/south facings"""
        b.set(x, y, z, blk)
        if both:
            for a, r in SWAP.items():
                if 'facing=' + a in blk:
                    blk = blk.replace('facing=' + a, 'facing=' + r); break
            b.set(x, y, -1 - z, blk)

    def MB(x0, y0, z0, x1, y1, z1, mix):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1): M(x, y, z, b.pick(mix))

    GATE = range(-29, -21)                                 # north-bank gate cells in the east wall (z), 8 wide
    # ground, channel and quay
    for x in range(X0, X1 + 1):
        for z in range(Z0, 0):
            court = -225 <= x <= -206
            if z >= -9:
                if court:
                    M(x, G, z, 'polished_andesite' if (x + z) % 5 == 0 else b.pick(FLAGS))
                    M(x, G - 1, z, b.pick(MASON) if x in (-225, -224, -216, -215, -207, -206) else 'stone_brick_slab[type=top]')
                    if x in (-225, -224, -216, -215, -207, -206):
                        for y in range(BED, G - 1): M(x, y, z, b.pick(FOOT))
                    else:
                        M(x, BED, z, 'gravel'); MB(x, BED + 1, z, x, WATER, z, 'water')
                else:
                    M(x, BED - 1, z, 'stone'); M(x, BED, z, b.pick([('gravel', 60), ('stone', 25), ('mud', 15)]))
                    MB(x, BED + 1, z, x, WATER, z, 'water'); MB(x, WATER + 1, z, x, G, z, 'air')
            else:
                M(x, G, z, b.pick(FLAGS) if z == -10 else b.pick(STREET) if (court or z in GATE or -223 <= x) and z > -36 else b.pick(YARD))
                if z == -10:
                    MB(x, BED, z, x, G - 1, z, MASON)
                    if not court: M(x, G + 1, z, 'stone_brick_wall')
    # bridge court edges: parapet over the water, lamp posts at the corners
    for x in (-225, -206):
        for z in range(-9, 0): M(x, G + 1, z, 'stone_brick_wall')
        M(x, G + 1, -10, 'stone_bricks'); M(x, G + 2, -10, 'stone_brick_wall'); M(x, G + 3, -10, 'lantern[hanging=false]')
    # curtain wall, merlons, wall walk with an inner rail
    def wallcell(x, z, water=False):
        lo = G + 3 if water else G + 1
        for y in range(lo, G + 6): M(x, y, z, b.pick(FOOT) if y == G + 1 else b.pick(MASON), both=False)
        if (x + z) % 2 == 0: M(x, G + 6, z, b.pick(MASON), both=False)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            river = -9 <= z <= 8
            if edge:
                if x == X1 and (z in GATE or -1 - z in GATE): continue
                wallcell(x, z, water=river)
                if river:                                  # the water grille, bed to arch
                    for y in range(BED + 1, G + 3): b.set(x, y, z, 'iron_bars[north=true,south=true]')
                    b.set(x, BED, z, 'stone_bricks')
            elif x in (X0 + 1, X0 + 2, X1 - 1, X1 - 2) or z in (Z0 + 1, Z0 + 2, Z1 - 1, Z1 - 2):
                if x >= X1 - 2 and (z in GATE or -1 - z in GATE): continue
                b.set(x, G + 4, z, 'spruce_planks')
                inner = (x in (X0 + 2, X1 - 2) and Z0 + 2 <= z <= Z1 - 2) or (z in (Z0 + 2, Z1 - 2) and X0 + 2 <= x <= X1 - 2)
                if inner:
                    b.set(x, G + 5, z, 'spruce_fence')
                    if (x + z) % 5 == 0 and not river:
                        for y in range(G + 1, G + 4): b.set(x, y, z, 'dark_oak_log[axis=y]')
    # outer faces: buttresses every 8, a barred loop and a banner between them, corners 3 higher
    for x in range(X0 + 4, X1 - 7, 8):
        for y in range(G + 1, G + 4): M(x, y, Z0 - 1, b.pick(FOOT) if y == G + 1 else b.pick(MASON))
        M(x, G + 4, Z0 - 1, 'stone_brick_stairs[facing=south,half=bottom,shape=straight]')
        for y in (G + 2, G + 3): M(x + 4, y, Z0, 'iron_bars[east=true,west=true]')
        if (x - X0) % 16 == 4: M(x + 4, G + 5, Z0 - 1, '%s_wall_banner[facing=north]' % colour)
    for z in range(Z0 + 4, -12, 8):
        for y in range(G + 1, G + 4): M(X0 - 1, y, z, b.pick(FOOT) if y == G + 1 else b.pick(MASON))
        M(X0 - 1, G + 4, z, 'stone_brick_stairs[facing=east,half=bottom,shape=straight]')
        if z + 4 < -12:
            for y in (G + 2, G + 3): M(X0, y, z + 4, 'iron_bars[north=true,south=true]')
            M(X0 - 1, G + 5, z + 4, '%s_wall_banner[facing=west]' % colour)
    for cx, sx in ((X0, 1), (X1, -1)):
        for i in range(4):
            for x, z in ((cx + sx * i, Z0), (cx, Z0 + i)):
                for y in range(G + 6, G + 9): M(x, y, z, b.pick(MASON))
                if i % 2 == 0: M(x, G + 9, z, b.pick(MASON))
    # twin towers at each gate: 6 x 6, 13 high, on the east wall
    for tz in (-35, -21):
        for x in range(X1 - 5, X1 + 1):
            for z in range(tz, tz + 6):
                edge = x in (X1 - 5, X1) or z in (tz, tz + 5)
                for y in range(G + 1, G + 14):
                    if edge or y in (G + 5, G + 10): M(x, y, z, 'spruce_planks' if not edge else b.pick(FOOT) if y == G + 1 else b.pick(MASON))
                    else: M(x, y, z, 'air')
                if edge and (x + z) % 2 == 0: M(x, G + 14, z, b.pick(MASON))
        for y in (G + 7, G + 8): M(X1, y, tz + 2, 'air'); M(X1, y, tz + 3, 'air')        # loops to the field
        M(X1 + 1, G + 11, tz + 2, '%s_wall_banner[facing=east]' % colour); M(X1 + 1, G + 11, tz + 3, '%s_wall_banner[facing=east]' % colour)
        M(X1 - 5, G + 1, tz + 2, 'air'); M(X1 - 5, G + 2, tz + 2, 'air')                   # door from the yard
        M(X1 - 5, G + 6, tz + 3, 'air'); M(X1 - 5, G + 7, tz + 3, 'air')                   # door from the wall walk level
        for y in range(G + 1, G + 11): M(X1 - 1, y, tz + 4, 'ladder[facing=west]')
        M(X1 - 1, G + 5, tz + 4, 'ladder[facing=west]'); M(X1 - 1, G + 10, tz + 4, 'ladder[facing=west]')
        M(X1 - 3, G + 1, tz + 1, 'barrel[facing=up]'); M(X1 - 2, G + 6, tz + 1, 'chest[facing=south]'); M(X1 - 3, G + 9, tz + 2, 'lantern[hanging=true]')
    # gate arch between the towers, wall over it
    for z in GATE:
        for x in (X1 - 1, X1):
            for y in range(G + 5, G + 8): M(x, y, z, b.pick(MASON))
            if (x + z) % 2 == 0 and x == X1: M(x, G + 8, z, b.pick(MASON))
        M(X1, G + 4, z, 'iron_bars[north=true,south=true]' if z not in (GATE[0], GATE[-1]) else 'stone_brick_stairs[facing=%s,half=top,shape=straight]' % ('north' if z == GATE[0] else 'south'))
        M(X1 - 2, G + 4, z, 'spruce_planks'); M(X1 - 2, G + 5, z, 'air')
        MB(X1 - 2, G, z, X1 + 1, G, z, STREET)
    for z in (GATE[2], GATE[5]): M(X1 - 1, G + 4, z, 'lantern[hanging=true]')
    # stairs to the wall walk on the north wall, climbing west
    for i, x in enumerate((-197, -198, -199, -200)):
        MB(x, G + 1, -37, x, G + i, -37, MASON) if i else None
        M(x, G + 1 + i, -37, 'stone_brick_stairs[facing=west,half=bottom,shape=straight]')
    for x in (-201, -202): M(x, G + 4, -37, 'spruce_planks'); M(x, G + 5, -38, 'air')
    # north bank: barracks 20 x 9; south bank the same shell as armoury and smithy
    bx0, bx1, bz0, bz1 = -247, -228, -36, -28
    for x in range(bx0, bx1 + 1):
        for z in range(bz0, bz1 + 1):
            edge = x in (bx0, bx1) or z in (bz0, bz1)
            M(x, G, z, 'spruce_planks')
            if edge:
                post = (x - bx0) % 4 == 3 or x in (bx0, bx1)
                for y in range(G + 1, G + 5):
                    M(x, y, z, b.pick(MASON) if y < G + 3 else ('dark_oak_log[axis=y]' if post and z in (bz0, bz1) else 'white_terracotta'))
                M(x, G + 4, z, 'stripped_dark_oak_log[axis=%s]' % ('x' if z in (bz0, bz1) else 'z'))
    for x in (-238, -237): M(x, G + 1, bz1, 'air'); M(x, G + 2, bz1, 'air')
    for x in (-244, -241, -234, -231):
        M(x, G + 3, bz1, 'air')
        for k in (-1, 1): M(x + k, G + 3, bz1 + 1, 'dark_oak_trapdoor[facing=south,half=top,open=true]')
    b.gable_x(bx0, bx1, bz0, bz1, G + 5, 'deepslate_tile_stairs', 'deepslate_tile_slab', fill=[('white_terracotta', 1)])
    b.gable_x(bx0, bx1, -1 - bz1, -1 - bz0, G + 5, 'deepslate_tile_stairs', 'deepslate_tile_slab', fill=[('white_terracotta', 1)])
    for x in (-243, -233):
        MB(x, G + 5, bz0 + 1, x, G + 5, bz1 - 1, 'dark_oak_log[axis=z]'); M(x, G + 4, -32, 'lantern[hanging=true]')
    Y = G + 1
    # barracks (north): bunks along the back wall, table, racks
    for x in range(-246, -229, 3):
        b.set(x, Y, -35, '%s_bed[facing=north,part=head]' % colour); b.set(x, Y, -34, '%s_bed[facing=north,part=foot]' % colour)
        b.set(x + 1, Y, -35, 'chest[facing=south]')
    for x in (-246, -245): b.set(x, Y, -30, 'spruce_fence'); b.set(x, Y + 1, -30, 'spruce_pressure_plate')
    b.set(-244, Y, -30, 'spruce_stairs[facing=west,half=bottom,shape=straight]'); b.set(-229, Y, -30, 'barrel[facing=up]'); b.set(-229, Y + 1, -30, 'lantern[hanging=false]')
    b.set(-229, Y, -31, 'grindstone[face=floor,facing=west]')
    # armoury and smithy (south): chests, anvil, furnaces, racks of logs and bars
    for x in range(-246, -238): b.set(x, Y, 34, 'chest[facing=north]' if x % 2 else 'barrel[facing=up]')
    b.set(-246, Y + 1, 34, 'barrel[facing=up]'); b.set(-232, Y, 34, 'blast_furnace[facing=north]'); b.set(-231, Y, 34, 'furnace[facing=north]'); b.set(-230, Y, 34, 'bricks'); b.set(-230, Y + 1, 34, 'bricks')
    b.set(-233, Y, 31, 'anvil[facing=east]'); b.set(-229, Y, 30, 'water_cauldron[level=3]'); b.set(-229, Y, 31, 'smithing_table'); b.set(-235, Y, 34, 'iron_bars[east=true,west=true]'); b.set(-236, Y, 34, 'iron_bars[east=true,west=true]')
    b.box(-231, G + 5, 36, -231, G + 12, 36, FOOT); b.set(-231, G + 13, 36, 'cobblestone_wall')
    # yards: well and drill posts on the north bank, cart and stacks on the south, team standards on the court
    for x, z in ((-212, -24), (-211, -24), (-212, -23), (-211, -23)): b.set(x, G + 1, z, 'cobblestone_wall' if (x, z) != (-212, -24) else 'cobblestone')
    b.set(-212, G + 2, -24, 'spruce_fence'); b.set(-212, G + 3, -24, 'spruce_slab[type=bottom]'); b.set(-211, G, -23, 'water')
    for x in (-222, -218, -214):
        b.set(x, G + 1, -34, 'spruce_fence'); b.set(x, G + 2, -34, 'hay_block[axis=y]')
    for x, z, blk in ((-222, 30, 'hay_block[axis=x]'), (-221, 30, 'hay_block[axis=x]'), (-222, 31, 'hay_block[axis=z]'), (-216, 33, 'barrel[facing=up]'), (-215, 33, 'barrel[facing=up]'),
                      (-215, 34, 'barrel[facing=up]'), (-210, 32, 'stripped_spruce_log[axis=x]'), (-209, 32, 'stripped_spruce_log[axis=x]'), (-209, 33, 'stripped_spruce_log[axis=x]')):
        b.set(x, G + 1, z, blk)
    b.set(-222, G + 2, 30, 'hay_block[axis=x]'); b.set(-215, G + 2, 33, 'barrel[facing=up]')
    for x in (-225, -206):
        for z in (-11, 10):
            for y in range(G + 1, G + 7): b.set(x, y, z, 'spruce_fence' if y > G + 1 else 'stone_bricks')
            b.set(x, G + 7, z, '%s_wool' % colour); b.set(x, G + 8, z, '%s_wool' % colour) if False else None
            b.set(x, G + 7, z, '%s_banner[rotation=4]' % colour)
    # tents in a row on each bank, west of the court; targets on the drill posts; spear racks by the gates
    for tx in (-246, -239, -232):
        for dx in range(5):
            for dz, y in ((0, 1), (4, 1), (1, 2), (3, 2), (2, 3)):
                M(tx + dx, G + y, -24 + dz, ('%s_wool' % colour) if dz == 2 or dx in (0, 4) and y == 1 else 'white_wool')
        for dx in (-1, 5):
            for y in (1, 2, 3): M(tx + dx, G + y, -22, 'spruce_fence')
        M(tx + 3, G + 1, -22, 'hay_block[axis=x]'); M(tx + 2, G + 1, -23, '%s_carpet' % colour); M(tx + 1, G + 1, -22, 'white_carpet')
    for x in (-222, -218, -214): b.set(x, G + 2, -34, 'target')
    for x in (-204, -203, -202, -201): M(x, G + 1, -34, 'spruce_fence' if x in (-204, -201) else 'spruce_fence_gate[facing=north,open=false]')
    M(-205, G + 1, -34, 'barrel[facing=up]'); M(-200, G + 1, -34, 'fletching_table')
    for z in (-14, 13):                                    # watch fires
        b.set(-216, G + 1, z, 'campfire[lit=false]')


if __name__ == '__main__':
    b = Build(seed=71)
    for x in range(X0 - 6, X1 + 14):
        for z in range(Z0 - 6, Z1 + 7):
            if -9 <= z <= 8:
                b.set(x, BED - 1, z, 'stone'); b.set(x, BED, z, 'gravel')
                for y in range(BED + 1, WATER + 1): b.set(x, y, z, 'water')
                b.set(x, WATER + 1, z, 'air'); b.set(x, G, z, 'air')
            else:
                for y in range(BED - 1, G): b.set(x, y, z, 'stone')
                b.set(x, G, z, 'grass_block')
    build(b)
    for p in ((-215, -4), (-170, -25)): b.box(p[0], 65, p[1], p[0], 66, p[1], 'red_wool')
    print(b.emit(sys.argv[1], 'rg-k1-watergate', 'K1 water gate, workshop build', pad=((X0 - 8, BED - 1, Z0 - 8), (X1 + 16, 110, Z1 + 8))), 'ops')
    for i, x0 in enumerate((-257, -212)):
        for j, z0 in enumerate((-46, 0)):
            print('k1_wg_%d%d %d %d %d %d %d %d' % (i, j, x0, BED - 1, z0, x0 + 44 if i == 0 else -167, 82, z0 + 45))
