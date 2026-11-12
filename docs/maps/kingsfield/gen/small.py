"""Supporting pieces laid side by side on one sampler pad: beacon tower (2 crowns), mason's lodge, cottage A (3 looks),
cottage B (2 looks), hay barn (2 looks), wayside cross and shrine. Every look of a piece keeps the same footprint, door
and height, so the four quarters of the map play the same. Each piece has its origin at its north-west corner, ground
block y 64. Prints one export box per piece.
Usage: python small.py OUT.json"""
import sys
from kit import Build, FOOT, MASON, DAUB, FLOOR
G, B0 = 64, 65
b = Build(seed=7)

STYLES = {
    'daub': dict(wall=DAUB, post='oak_log', beam='stripped_oak_log', roof=('dark_oak_stairs', 'dark_oak_slab'), trim='spruce', bed='red'),
    'tile': dict(wall=DAUB, post='dark_oak_log', beam='stripped_dark_oak_log', roof=('brick_stairs', 'brick_slab'), trim='dark_oak', bed='green'),
    'stone': dict(wall='stone_bricks', post='spruce_log', beam='stripped_spruce_log', roof=('cobbled_deepslate_stairs', 'cobbled_deepslate_slab'), trim='spruce', bed='blue', stone=3),
    'mud': dict(wall='mud_bricks', post='spruce_log', beam='stripped_spruce_log', roof=('spruce_stairs', 'spruce_slab'), trim='oak', bed='yellow'),
    'board': dict(wall='spruce_planks', post='oak_log', beam='stripped_oak_log', roof=('mud_brick_stairs', 'mud_brick_slab'), trim='oak', bed='white'),
}
OUT = {'n': (0, -1, 'north'), 's': (0, 1, 'south'), 'w': (-1, 0, 'west'), 'e': (1, 0, 'east')}
OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}


def side_of(dx, dz, w, d):
    return 'n' if dz == 0 else 's' if dz == d - 1 else 'w' if dx == 0 else 'e'


def frame(ox, oz, w, d, h, st, doors=(), windows=(), roof='x', posts=4, lamp=None):
    """timber-framed house w x d, walls h high on a stone course. doors/windows are (dx, dz) cells on the wall line.
    Windows get shutters, doorways a log lintel, the eaves brackets under every post. lamp: the door cell that gets a
    lantern hung from a beam end beside it."""
    x0, x1, z0, z1 = ox, ox + w - 1, oz, oz + d - 1
    stone = st.get('stone', 1)
    b.box(x0, G, z0, x1, G, z1, FLOOR)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)): continue
            post = (x in (x0, x1) and z in (z0, z1)) or (z in (z0, z1) and (x - x0) % posts == 0) or (x in (x0, x1) and (z - z0) % posts == 0)
            for y in range(B0, B0 + h):
                blk = b.pick(FOOT) if y < B0 + stone else (st['post'] + '[axis=y]' if post else st['wall'])
                if y == B0 + h - 1: blk = st['beam'] + '[axis=%s]' % ('x' if z in (z0, z1) else 'z')
                b.set(x, y, z, blk)
    stairs, slab = st['roof']
    if roof == 'x': b.gable_x(x0, x1, z0, z1, B0 + h, stairs, slab, fill=[(st['wall'], 1)])
    else: b.gable_z(x0, x1, z0, z1, B0 + h, stairs, slab, fill=[(st['wall'], 1)])
    # brackets under the eaves, at every post
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if roof == 'x' and z in (z0, z1) and (x - x0) % posts == 0:
                dz = -1 if z == z0 else 1
                b.set(x, B0 + h - 1, z + dz, '%s_stairs[facing=%s,half=top,shape=straight]' % (st['trim'], 'south' if dz < 0 else 'north'))
            if roof == 'z' and x in (x0, x1) and (z - z0) % posts == 0:
                dx = -1 if x == x0 else 1
                b.set(x + dx, B0 + h - 1, z, '%s_stairs[facing=%s,half=top,shape=straight]' % (st['trim'], 'east' if dx < 0 else 'west'))
    for dx, dz in doors:
        b.air(ox + dx, B0, oz + dz, ox + dx, B0 + 1, oz + dz)
        s = side_of(dx, dz, w, d)
        b.set(ox + dx, B0 + 2, oz + dz, st['beam'] + '[axis=%s]' % ('x' if s in 'ns' else 'z'))
        b.set(ox + dx + OUT[s][0], G, oz + dz + OUT[s][1], 'gravel')
    for dx, dz in windows:
        s = side_of(dx, dz, w, d)
        ux, uz, f = OUT[s]
        b.set(ox + dx, B0 + 2, oz + dz, 'air')
        for k in (-1, 1):                                   # a shutter each side, folded back on the wall
            sx, sz = (dx + k, dz) if s in 'ns' else (dx, dz + k)
            if (sx, sz) in doors or (sx, sz) in windows or not (0 <= sx < w and 0 <= sz < d): continue
            b.set(ox + sx + ux, B0 + 2, oz + sz + uz, '%s_trapdoor[facing=%s,half=top,open=true]' % (st['trim'], f))
    if lamp:
        dx, dz, k = lamp
        s = side_of(dx, dz, w, d)
        ux, uz, f = OUT[s]
        lx, lz = (dx + k, dz) if s in 'ns' else (dx, dz + k)
        b.set(ox + lx + ux, B0 + 2, oz + lz + uz, st['beam'] + '[axis=%s]' % ('z' if s in 'ns' else 'x'))
        b.set(ox + lx + ux, B0 + 1, oz + lz + uz, 'lantern[hanging=true]')


def chimney(x, z, side, top):
    """stone stack outside the wall cell (x, z) on `side`, with an open hearth let into the wall"""
    ux, uz, f = OUT[side]
    b.box(x + ux, G, z + uz, x + ux, top, z + uz, FOOT)
    b.box(x + ux, top - 2, z + uz, x + ux, top, z + uz, 'bricks')
    b.set(x + ux, top + 1, z + uz, 'brick_wall')
    px, pz = (1, 0) if side in 'ns' else (0, 1)
    for k in (-1, 1): b.box(x + k * px, B0, z + k * pz, x + k * px, B0 + 1, z + k * pz, 'bricks')
    b.set(x, B0 + 1, z, 'bricks'); b.set(x, B0, z, 'campfire[lit=true]')
    b.set(x - ux, G, z - uz, 'bricks')


def table(x, z, axis='x', lamp=True):
    """two upturned stairs make a trestle table, two cells long"""
    if axis == 'x':
        b.set(x, B0, z, 'spruce_stairs[facing=west,half=top,shape=straight]'); b.set(x + 1, B0, z, 'spruce_stairs[facing=east,half=top,shape=straight]')
    else:
        b.set(x, B0, z, 'spruce_stairs[facing=north,half=top,shape=straight]'); b.set(x, B0, z + 1, 'spruce_stairs[facing=south,half=top,shape=straight]')
    if lamp: b.set(x, B0 + 1, z, 'lantern[hanging=false]')


def chair(x, z, back): b.set(x, B0, z, 'spruce_stairs[facing=%s,half=bottom,shape=straight]' % back)


def bed(x, z, facing, colour):
    """foot at (x, z), head one further along `facing`"""
    dx, dz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[facing]
    b.set(x, B0, z, '%s_bed[facing=%s,part=foot]' % (colour, facing)); b.set(x + dx, B0, z + dz, '%s_bed[facing=%s,part=head]' % (colour, facing))


def garden(x0, z0, x1, z1, gap_x, crops=('carrots[age=7]', 'wheat[age=7]', 'beetroots[age=3]', 'potatoes[age=7]')):
    """fenced kitchen garden x0..x1, z0..z1 with an open way at gap_x: crop beds either side of a gravel path"""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z == z1
            if x == gap_x: b.set(x, G, z, 'gravel'); continue
            if edge:
                b.set(x, G, z, 'coarse_dirt'); b.set(x, B0, z, 'oak_fence')
            elif z > z0:
                b.set(x, G, z, 'farmland[moisture=7]'); b.set(x, B0, z, crops[(x + z) % 2 + (0 if x < gap_x else 2)])
            else: b.set(x, G, z, 'coarse_dirt')


def s1_beacon(ox, oz, crown='fire'):
    """watch tower 7 x 7, door on the south. crown 'fire': battlements and a fire bowl; 'bell': a roofed bell loft"""
    top = B0 + 11
    b.box(ox, G, oz, ox + 6, G, oz + 6, FOOT)
    for x in range(ox, ox + 7):
        for z in range(oz, oz + 7):
            if x in (ox, ox + 6) or z in (oz, oz + 6):
                for y in range(B0, top + 1): b.set(x, y, z, b.pick(FOOT) if y < B0 + 3 else b.pick(MASON))
            else:
                b.set(x, top, z, 'spruce_planks'); b.set(x, B0 + 5, z, 'spruce_planks')        # roof deck and a middle floor
    b.air(ox + 3, B0, oz + 6, ox + 3, B0 + 1, oz + 6)
    b.set(ox + 3, B0 + 2, oz + 6, 'stripped_spruce_log[axis=x]')
    for y in (B0 + 7, B0 + 9):
        b.set(ox + 3, y, oz, 'air'); b.set(ox, y, oz + 3, 'air'); b.set(ox + 6, y, oz + 3, 'air'); b.set(ox + 3, y, oz + 6, 'air')
    for y in (B0 + 3,): b.set(ox, y, oz + 3, 'air'); b.set(ox + 6, y, oz + 3, 'air')
    for x in range(ox - 1, ox + 8):                              # corbelled parapet
        for z in range(oz - 1, oz + 8):
            if x in (ox - 1, ox + 7) or z in (oz - 1, oz + 7):
                b.set(x, top - 1, z, 'stone_brick_stairs[facing=%s,half=top,shape=straight]' % (
                    'south' if z == oz - 1 else 'north' if z == oz + 7 else 'east' if x == ox - 1 else 'west'))
                b.set(x, top, z, b.pick(MASON))
                b.set(x, top + 1, z, b.pick(MASON))
                if crown == 'fire' and (x + z) % 2 == 0: b.set(x, top + 2, z, b.pick(MASON))
    for y in range(B0, top): b.set(ox + 1, y, oz + 1, 'ladder[facing=south]')
    b.set(ox + 1, top, oz + 1, 'air'); b.set(ox + 1, B0 + 5, oz + 1, 'air')
    # guard room at the foot, store on the middle floor
    b.set(ox + 5, B0, oz + 1, 'barrel[facing=up]'); b.set(ox + 5, B0, oz + 2, 'barrel[facing=up]'); b.set(ox + 5, B0 + 1, oz + 1, 'barrel[facing=up]')
    b.set(ox + 1, B0, oz + 5, 'chest[facing=east]'); b.set(ox + 5, B0, oz + 5, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
    b.set(ox + 3, B0 + 4, oz + 3, 'lantern[hanging=true]')
    for x in (ox + 4, ox + 5): b.set(x, B0 + 6, oz + 5, 'oak_log[axis=x]')              # firewood for the beacon
    b.set(ox + 5, B0 + 6, oz + 4, 'oak_log[axis=x]'); b.set(ox + 5, B0 + 7, oz + 5, 'oak_log[axis=x]')
    b.set(ox + 1, B0 + 6, oz + 5, 'hay_block[axis=y]'); b.set(ox + 3, B0 + 10, oz + 3, 'lantern[hanging=true]')
    if crown == 'fire':
        b.box(ox + 3, top + 1, oz + 3, ox + 3, top + 2, oz + 3, 'stone_bricks')         # fire bowl on a stone pedestal
        for dx, dz, f in ((1, 0, 'west'), (-1, 0, 'east'), (0, 1, 'north'), (0, -1, 'south')):
            b.set(ox + 3 + dx, top + 2, oz + 3 + dz, 'stone_brick_stairs[facing=%s,half=top,shape=straight]' % f)
        b.set(ox + 3, top + 3, oz + 3, 'netherrack'); b.set(ox + 3, top + 4, oz + 3, 'fire')
        for x, z in ((ox + 5, oz + 5), (ox + 5, oz + 4)): b.set(x, top + 1, z, 'oak_log[axis=z]')
        return (ox - 1, G, oz - 1, ox + 7, top + 4, oz + 7)
    for x, z in ((ox - 1, oz - 1), (ox + 7, oz - 1), (ox - 1, oz + 7), (ox + 7, oz + 7)):    # bell loft: four posts, a hipped cap
        b.box(x, top + 2, z, x, top + 4, z, 'spruce_log[axis=y]')
    for x in range(ox - 1, ox + 8):
        for z in (oz - 1, oz + 7): b.set(x, top + 4, z, 'stripped_spruce_log[axis=x]')
    for z in range(oz - 1, oz + 8):
        for x in (ox - 1, ox + 7): b.set(x, top + 4, z, 'stripped_spruce_log[axis=z]')
    for z in range(oz - 1, oz + 8): b.set(ox + 3, top + 4, z, 'stripped_spruce_log[axis=z]')
    b.pyramid(ox - 2, oz - 2, ox + 8, oz + 8, top + 5, 'cobbled_deepslate_stairs', 'cobbled_deepslate_slab')
    b.set(ox + 3, top + 3, oz + 3, 'bell[attachment=ceiling,facing=north]')
    return (ox - 2, G, oz - 2, ox + 8, top + 11, oz + 8)


def s2_lodge(ox, oz):
    """mason's lodge 12 x 9: stone-built with a slate roof, a lean-to work shelter on the east end"""
    st = dict(STYLES['stone'], stone=2)
    frame(ox, oz, 9, 9, 5, st, doors=((4, 8),), windows=((2, 8), (6, 8), (0, 4)), roof='z', lamp=(4, 8, 1))
    chimney(ox + 4, oz, 'n', B0 + 12)
    for x in range(ox + 9, ox + 12):                                         # lean-to: a pent roof on two posts
        for z in range(oz + 1, oz + 8):
            b.set(x, B0 + 4 - (x - ox - 9), z, 'cobbled_deepslate_stairs[facing=west,half=bottom,shape=straight]'); b.set(x, G, z, 'gravel')
    for z in (oz + 1, oz + 7): b.box(ox + 11, B0, z, ox + 11, B0 + 1, z, 'oak_log[axis=y]')
    b.set(ox + 10, B0, oz + 4, 'smooth_stone'); b.set(ox + 10, B0, oz + 3, 'stone_brick_stairs[facing=south,half=bottom,shape=straight]')
    b.set(ox + 10, B0, oz + 6, 'chiseled_stone_bricks'); b.set(ox + 9, B0, oz + 6, 'stone_bricks'); b.set(ox + 9, B0 + 1, oz + 6, 'smooth_stone')
    b.set(ox + 9, B0, oz + 5, 'smooth_stone'); b.set(ox + 9, B0, oz + 2, 'stonecutter[facing=east]'); b.set(ox + 10, B0, oz + 1, 'barrel[facing=up]')
    # inside: bench with the tools, a table, a bed, the cut stone waiting to go out
    b.set(ox + 1, B0, oz + 1, 'stonecutter[facing=south]'); b.set(ox + 2, B0, oz + 1, 'smithing_table'); b.set(ox + 7, B0, oz + 1, 'chest[facing=south]')
    b.set(ox + 6, B0, oz + 1, 'barrel[facing=up]')
    bed(ox + 7, oz + 5, 'south', 'blue')
    table(ox + 1, oz + 6); chair(ox + 2, oz + 5, 'north'); chair(ox + 1, oz + 5, 'north')
    b.set(ox + 7, B0, oz + 3, 'smooth_stone'); b.set(ox + 7, B0 + 1, oz + 3, 'chiseled_stone_bricks'); b.set(ox + 6, B0, oz + 3, 'stone_bricks')
    for x in range(ox + 1, ox + 8): b.set(x, B0 + 4, oz + 4, 'stripped_spruce_log[axis=x]')
    b.set(ox + 4, B0 + 3, oz + 4, 'lantern[hanging=true]')
    return (ox - 1, G, oz - 1, ox + 12, B0 + 13, oz + 9)


def s4a_cottage(ox, oz, look='daub', flip=False):
    """farm cottage 9 x 7, door south, hearth and stack on one gable, a fenced kitchen garden in front"""
    st = STYLES[look]
    X = lambda dx: ox + (8 - dx if flip else dx)
    E, W = ('west', 'east') if flip else ('east', 'west')
    frame(ox, oz, 9, 7, 4, st, doors=((4, 6),), windows=((2, 6), (6, 6), (2 if flip else 6, 0)), roof='x', lamp=(4, 6, -1 if flip else 1))
    chimney(X(0), oz + 3, 'e' if flip else 'w', B0 + 10)
    bed(X(6), oz + 1, E, st['bed']); b.set(X(7), B0, oz + 2, 'chest[facing=%s]' % W)
    b.set(X(3), B0, oz + 2, 'spruce_stairs[facing=%s,half=top,shape=straight]' % W); b.set(X(4), B0, oz + 2, 'spruce_stairs[facing=%s,half=top,shape=straight]' % E)
    b.set(X(3), B0 + 1, oz + 2, 'lantern[hanging=false]'); chair(X(3), oz + 3, 'south')
    b.set(X(1), B0, oz + 1, 'barrel[facing=up]'); b.set(X(2), B0, oz + 1, 'crafting_table'); b.set(X(1), B0, oz + 5, 'cauldron')
    b.set(X(7), B0, oz + 5, 'barrel[facing=up]'); b.set(X(7), B0 + 1, oz + 5, 'potted_dandelion' if look != 'tile' else 'potted_poppy')
    for dx in (5, 6): b.set(X(dx), B0, oz + 4, '%s_carpet' % st['bed'])
    for dx in range(1, 8): b.set(X(dx), B0 + 3, oz + 3, st['beam'] + '[axis=x]')
    b.set(X(4), B0 + 2, oz + 3, 'lantern[hanging=true]')
    # outside: garden in front, woodpile and water butt on the blind gable
    garden(ox - 1, oz + 7, ox + 9, oz + 11, ox + 4)
    for dz in (1, 2): b.set(X(9), B0, oz + dz, 'oak_log[axis=z]')
    b.set(X(9), B0 + 1, oz + 1, 'oak_log[axis=z]'); b.set(X(9), B0, oz + 5, 'barrel[facing=up]')
    b.set(X(2), B0, oz - 1, 'composter'); b.set(X(3), B0, oz - 1, 'hay_block[axis=x]')
    return (ox - 1, G, oz - 1, ox + 9, B0 + 11, oz + 11)


def s4b_cottage(ox, oz, look='daub'):
    """longhouse 9 x 11 with a byre end, ridge north-south, door east, hearth on the north gable"""
    st = STYLES[look]
    frame(ox, oz, 9, 11, 4, st, doors=((8, 3),), windows=((8, 6), (0, 3), (0, 9)), roof='z', lamp=(8, 3, -1))
    chimney(ox + 4, oz, 'n', B0 + 11)
    for x in range(ox + 1, ox + 8): b.set(x, B0, oz + 7, 'oak_fence')              # byre partition
    b.set(ox + 4, B0, oz + 7, 'air')
    b.air(ox + 4, B0, oz + 10, ox + 5, B0 + 1, oz + 10)                              # byre door, south
    for x in (ox + 4, ox + 5): b.set(x, B0 + 2, oz + 10, st['beam'] + '[axis=x]')
    bed(ox + 1, oz + 2, 'north', st['bed']); bed(ox + 2, oz + 5, 'west', st['bed']); b.set(ox + 1, B0, oz + 6, 'chest[facing=east]')
    table(ox + 5, oz + 5); chair(ox + 5, oz + 6, 'south'); chair(ox + 6, oz + 4, 'north')
    b.set(ox + 7, B0, oz + 1, 'barrel[facing=up]'); b.set(ox + 6, B0, oz + 1, 'crafting_table'); b.set(ox + 2, B0, oz + 1, 'cauldron')
    for z in (oz + 3, oz + 4): b.set(ox + 4, B0, z, '%s_carpet' % st['bed'])
    for x in range(ox + 1, ox + 8): b.set(x, B0 + 3, oz + 4, st['beam'] + '[axis=x]')
    b.set(ox + 4, B0 + 2, oz + 4, 'lantern[hanging=true]')
    for x, y, z in ((1, 0, 9), (2, 0, 9), (1, 1, 9), (1, 0, 8)): b.set(ox + x, B0 + y, oz + z, 'hay_block[axis=x]')
    b.set(ox + 7, B0, oz + 9, 'water_cauldron[level=3]'); b.set(ox + 7, B0, oz + 8, 'composter')
    for x in range(ox + 1, ox + 8): b.set(x, G, oz + 8, 'coarse_dirt'); b.set(x, G, oz + 9, 'packed_mud' if x % 2 else 'coarse_dirt')
    # outside: woodpile on the west wall, a bench by the door
    for z in (oz + 5, oz + 6): b.set(ox - 1, B0, z, 'oak_log[axis=z]')
    b.set(ox - 1, B0 + 1, oz + 5, 'oak_log[axis=z]'); b.set(ox + 9, B0, oz + 1, 'barrel[facing=up]')
    b.set(ox + 9, B0, oz + 5, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
    return (ox - 1, G, oz - 1, ox + 9, B0 + 12, oz + 11)


def s5_haybarn(ox, oz, look='hay'):
    """open-fronted barn 12 x 8, boarded on three sides. 'hay': a hay barn with a loft; 'cart': a thatched cart shed"""
    x0, x1, z0, z1 = ox, ox + 11, oz, oz + 7
    b.box(x0, G, z0, x1, G, z1, [('coarse_dirt', 3), ('packed_mud', 1)])
    board = 'spruce_planks' if look == 'hay' else 'oak_planks'
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if z == z1:
                if (x - x0) % 4 == 0 or x == x1: b.box(x, B0, z, x, B0 + 3, z, 'oak_log[axis=y]')
            elif x in (x0, x1) or z == z0:
                post = (x in (x0, x1) and z == z0) or (z == z0 and (x - x0) % 4 == 0) or (x in (x0, x1) and (z - z0) % 4 == 0)
                for y in range(B0, B0 + 4): b.set(x, y, z, 'oak_log[axis=y]' if post else (b.pick(FOOT) if y == B0 else board))
    for x in range(x0, x1 + 1): b.set(x, B0 + 3, z1, 'stripped_oak_log[axis=x]'); b.set(x, B0 + 3, z0, 'stripped_oak_log[axis=x]')
    for x in (x0 + 2, x0 + 6, x0 + 10): b.set(x, B0 + 2, z0, 'air')                 # vent slits in the back wall
    if look == 'hay':
        b.gable_x(x0, x1, z0, z1, B0 + 4, 'spruce_stairs', 'spruce_slab', fill=[(board, 1)])
    else:                                                                           # thatch: stepped courses of straw
        i = 0
        while z0 - 1 + i <= z1 + 1 - i:
            for x in range(x0 - 1, x1 + 2):
                for z in (z0 - 1 + i, z1 + 1 - i): b.set(x, B0 + 4 + i, z, 'hay_block[axis=x]')
            i += 1
        for x in range(x0 - 1, x1 + 2): b.set(x, B0 + 4 + i, (z0 + z1) // 2, 'oak_slab[type=bottom]'); b.set(x, B0 + 4 + i, (z0 + z1) // 2 + 1, 'oak_slab[type=bottom]')
        for x in (x0, x1):
            for k in range(0, 4):
                for z in range(z0 + k, z1 - k + 1): b.set(x, B0 + 4 + k, z, board)
    for x in range(x0, x1 + 1, 4):                                                  # brackets under the front eave
        b.set(x, B0 + 3, z1 + 1, 'oak_stairs[facing=north,half=top,shape=straight]')
    for z in (z0 + 4,):                                                             # a tie beam and a lamp over the bay
        for x in range(x0 + 1, x1): b.set(x, B0 + 3, z, 'stripped_oak_log[axis=x]')
    b.set(x0 + 6, B0 + 2, z0 + 4, 'lantern[hanging=true]')
    if look == 'hay':
        for x in range(x0 + 1, x0 + 6):
            for z in range(z0 + 1, z0 + 6):
                for y in range(B0, B0 + max(0, 3 - (x - x0 - 1) // 2 - (z - z0 - 1) // 2)): b.set(x, y, z, 'hay_block[axis=%s]' % ('y' if (x + z) % 3 else 'x'))
        for x in (x0 + 7, x0 + 8, x0 + 9): b.set(x, B0, z0 + 1, 'oak_log[axis=x]')
        b.set(x0 + 8, B0 + 1, z0 + 1, 'oak_log[axis=x]'); b.set(x0 + 10, B0, z0 + 4, 'barrel[facing=up]'); b.set(x0 + 10, B0, z0 + 5, 'barrel[facing=up]')
        b.set(x0 + 10, B0 + 1, z0 + 4, 'barrel[facing=up]'); b.set(x0 + 7, B0, z0 + 3, 'composter')
    else:
        # a wagon: a plank bed on four log wheels, shafts on the ground, loaded with sacks
        for x in range(x0 + 2, x0 + 7):
            for z in (z0 + 2, z0 + 3, z0 + 4): b.set(x, B0, z, 'spruce_slab[type=top]')
        for x in (x0 + 2, x0 + 6):
            for z in (z0 + 1, z0 + 5): b.set(x, B0, z, 'stripped_spruce_log[axis=z]')
        for x in (x0 + 3, x0 + 4): b.set(x, B0 + 1, z0 + 3, 'hay_block[axis=x]')
        b.set(x0 + 5, B0 + 1, z0 + 3, 'barrel[facing=up]'); b.set(x0 + 5, B0 + 1, z0 + 2, 'white_wool'); b.set(x0 + 3, B0 + 1, z0 + 4, 'white_wool')
        for x in (x0 + 7, x0 + 8): b.set(x, B0, z0 + 3, 'spruce_log[axis=x]')
        b.set(x0 + 10, B0, z0 + 1, 'barrel[facing=up]'); b.set(x0 + 10, B0, z0 + 2, 'barrel[facing=up]'); b.set(x0 + 10, B0 + 1, z0 + 1, 'barrel[facing=up]')
        b.set(x0 + 9, B0, z0 + 1, 'grindstone[face=floor,facing=south]'); b.set(x0 + 10, B0, z0 + 5, 'hay_block[axis=y]'); b.set(x0 + 9, B0, z0 + 5, 'hay_block[axis=x]')
        b.set(x0 + 10, B0 + 1, z0 + 5, 'hay_block[axis=x]')
    return (ox - 1, G, oz - 1, ox + 12, B0 + 10, oz + 8)


def s7_cross(ox, oz, look='cross'):
    """wayside marker on a stepped plinth, 3 x 3. 'cross': a stone cross; 'shrine': a roofed niche with a light"""
    b.box(ox, G, oz, ox + 2, G, oz + 2, FOOT)
    f = {(0, 1): 'east', (2, 1): 'west', (1, 0): 'south', (1, 2): 'north'}
    for x in range(ox, ox + 3):
        for z in range(oz, oz + 3):
            k = (x - ox, z - oz)
            if k == (1, 1): b.set(x, B0, z, 'mossy_stone_bricks')
            elif k in f: b.set(x, B0, z, 'stone_brick_stairs[facing=%s,half=bottom,shape=straight]' % f[k])
            else: b.set(x, B0, z, 'cobblestone_slab[type=bottom]')
    if look == 'cross':
        b.box(ox + 1, B0 + 1, oz + 1, ox + 1, B0 + 4, oz + 1, 'stone_brick_wall')
        b.set(ox, B0 + 3, oz + 1, 'stone_brick_wall'); b.set(ox + 2, B0 + 3, oz + 1, 'stone_brick_wall')
        return (ox, G, oz, ox + 2, B0 + 4, oz + 2)
    for x in (ox, ox + 2): b.set(x, B0, oz + 1, 'stone_bricks')
    b.set(ox + 1, B0 + 1, oz + 1, 'chiseled_stone_bricks'); b.set(ox + 1, B0 + 2, oz + 1, 'lantern[hanging=false]')
    for x in (ox, ox + 2): b.set(x, B0 + 4, oz + 1, 'cobbled_deepslate_stairs[facing=%s,half=bottom,shape=straight]' % ('east' if x == ox else 'west'))
    b.set(ox + 1, B0 + 4, oz + 1, 'cobbled_deepslate_slab[type=top]')
    for x in (ox, ox + 2): b.box(x, B0 + 1, oz + 1, x, B0 + 3, oz + 1, 'stone_brick_wall')
    return (ox, G, oz, ox + 2, B0 + 4, oz + 2)


boxes = {'s1_beacon': s1_beacon(3, 3, 'fire'), 's1_bell': s1_beacon(18, 3, 'bell'), 's2_lodge': s2_lodge(32, 3),
         's4a_cottage': s4a_cottage(50, 2, 'daub'), 's4a_tile': s4a_cottage(66, 2, 'tile', flip=True), 's4a_stone': s4a_cottage(82, 2, 'stone'),
         's4b_cottage': s4b_cottage(2, 22, 'mud'), 's4b_board': s4b_cottage(18, 22, 'board'),
         's5_haybarn': s5_haybarn(34, 22, 'hay'), 's5_cart': s5_haybarn(52, 22, 'cart'),
         's7_cross': s7_cross(70, 24, 'cross'), 's7_shrine': s7_cross(78, 24, 'shrine')}
n = b.emit(sys.argv[1], 'kf-small', 'Kingsfield supporting pieces sampler', pad=((-8, 40, -8), (104, 120, 48)))
for k, v in boxes.items(): print(k, *v)
print(n, 'ops', b.bounds())
