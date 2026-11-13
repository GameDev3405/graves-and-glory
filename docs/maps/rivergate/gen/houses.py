"""Rivergate town houses. house(...) returns a Build in local coordinates: x 0..w-1, z 0..d-1, ground block y 64,
the street side is +z (south). The upper floors jetty one block over the street (z = d) and the chimney stands one
block behind (z = -1). Stone or brick ground floor, timber frame above, three roof kinds. Every room is furnished.
KINDS lists the named pieces of the plan (S1 narrow, S2 wide with a shop, S3 corner with an arch, S4 inn and guild,
S5 warehouse). As a script it lays them all in a row for pictures. Usage: python houses.py OUT.json"""
import sys
from kit import Build, MASON, FOOT

G = 64
BRICK = [('bricks', 88), ('mud_bricks', 6), ('granite', 6)]
STYLES = {
    'slate': dict(ground=MASON, post='dark_oak_log', beam='stripped_dark_oak_log', infill='white_terracotta', roof='deepslate_tile', trim='dark_oak', bed='red'),
    'tile': dict(ground=BRICK, post='spruce_log', beam='stripped_spruce_log', infill='white_terracotta', roof='brick', trim='spruce', bed='yellow'),
    'shingle': dict(ground=FOOT, post='spruce_log', beam='stripped_spruce_log', infill='mud_bricks', roof='spruce', trim='spruce', bed='green'),
    'brick': dict(ground=BRICK, post='stone_bricks', beam='stripped_dark_oak_log', infill='bricks', roof='deepslate_tile', trim='dark_oak', bed='blue'),
    'stone': dict(ground=MASON, post='chiseled_stone_bricks', beam='stripped_dark_oak_log', infill='stone_bricks', roof='brick', trim='dark_oak', bed='purple'),
}


def house(w, d, floors=2, style='slate', ridge=None, shop=False, arch=False, hoist=False, sign=False, door=None, seed=1):
    st = STYLES[style]
    b = Build(seed=seed)
    ridge = ridge or ('z' if w <= 9 else 'x')
    door = door if door is not None else (1 if w <= 9 else w // 2)
    doors = (door,) if w <= 9 or hoist else (door, door + 1)
    if hoist: doors = (door - 1, door, door + 1)
    ax0 = w - 5 if arch else None                          # arch passage x ax0+1 .. w-2
    post = lambda p: (p if p.startswith(('stone', 'chiseled')) else p + '[axis=y]')
    for x in range(w):
        for z in range(d): b.set(x, G, z, b.pick([('stone_bricks', 40), ('andesite', 30), ('cobblestone', 30)]) if (arch and x > ax0) or hoist else 'spruce_planks')
    top = G
    for k in range(floors):
        y0 = G + 1 + 4 * k
        fz = d - 1 if k == 0 else d
        if k:
            for x in range(w):
                for z in range(fz + 1):
                    edge = x in (0, w - 1) or z in (0, fz)
                    b.set(x, y0 - 1, z, (st['beam'] + '[axis=%s]' % ('x' if z in (0, fz) else 'z')) if edge else 'spruce_planks')
        for x in range(w):
            for z in range(fz + 1):
                if not (x in (0, w - 1) or z in (0, fz)): continue
                is_post = (x in (0, w - 1) and z in (0, fz)) or (z in (0, fz) and x % 3 == 0) or (x in (0, w - 1) and z % 4 == 0)
                for y in range(y0, y0 + 3):
                    if k == 0: blk = b.pick(FOOT) if y == y0 and style != 'brick' else b.pick(st['ground'])
                    else: blk = post(st['post']) if is_post else st['infill']
                    b.set(x, y, z, blk)
        # front openings
        if k == 0:
            for x in doors:
                b.air(x, y0, fz, x, y0 + 1, fz); b.set(x, y0 + 2, fz, st['beam'] + '[axis=x]')
            if hoist: b.set(doors[1], y0 + 2, fz, st['beam'] + '[axis=x]')
            xs = [x for x in range(1, w - 1) if x not in doors and x - 1 not in doors and x + 1 not in doors and not (arch and x >= ax0)]
            if shop:
                for x in xs:
                    b.set(x, y0 + 1, fz, 'air'); b.set(x, y0, fz, '%s_slab[type=top]' % st['trim'])
                    b.set(x, y0 + 2, fz + 1, '%s_trapdoor[facing=south,half=top,open=false]' % st['trim'])
            else:
                for x in xs[::2]: b.set(x, y0 + 1, fz, 'iron_bars[east=true,west=true]')
            for x in range(w):                             # brackets under the jetty
                if x % 3 == 0 or x == w - 1: b.set(x, y0 + 2, fz + 1, '%s_stairs[facing=north,half=top,shape=straight]' % st['trim'])
        else:
            wx = sorted(set([2, w - 3] + ([w // 2] if w >= 11 else []) + ([5, w - 6] if w >= 17 else [])))
            for x in wx:
                b.set(x, y0 + 1, fz, 'air')
                for s in (-1, 1):
                    if x + s not in wx: b.set(x + s, y0 + 1, fz + 1, '%s_trapdoor[facing=south,half=top,open=true]' % st['trim'])
            b.set(w // 2, y0 + 1, 0, 'air')
        zs = d // 2 if (d // 2) % 4 else d // 2 + 1               # a side window on each floor (hidden where houses share a wall)
        for x in (0, w - 1):
            if k:
                for z in (zs, zs - 1 if (zs - 1) % 4 else zs + 1): b.set(x, y0 + 1, z, 'iron_bars[north=true,south=true]')
            elif not hoist and not (arch and x == w - 1): b.set(x, y0 + 1, zs, 'iron_bars[north=true,south=true]')
        top = y0 + 3
    # arch passage through the ground floor
    if arch:
        for z in range(d):
            for x in range(ax0 + 1, w - 1):
                for y in (G + 1, G + 2): b.set(x, y, z, 'air')
                b.set(x, G + 3, z, 'air' if 0 < z < d - 1 else st['beam'] + '[axis=x]')
            for y in (G + 1, G + 2, G + 3): b.set(ax0, y, z, b.pick(st['ground']))
        for z in (0, d - 1):
            b.set(ax0 + 1, G + 2, z, '%s_stairs[facing=west,half=top,shape=straight]' % ('stone_brick'))
            b.set(w - 2, G + 2, z, '%s_stairs[facing=east,half=top,shape=straight]' % ('stone_brick'))
        b.set(ax0 + 2, G + 3, d // 2, 'lantern[hanging=true]')
    # roof
    stairs, slab = st['roof'] + '_stairs', st['roof'] + '_slab'
    if ridge == 'z': b.gable_z(0, w - 1, 0, d, top, stairs, slab, fill=[(st['infill'], 1)])
    else: b.gable_x(0, w - 1, 0, d, top, stairs, slab, fill=[(st['infill'], 1)])
    rise = ((w if ridge == 'z' else d + 1) + 2) // 2
    if ridge == 'z':
        for z in (0, d):
            for y in range(top, top + rise):
                if b.get(w // 2, y, z) == st['infill']: b.set(w // 2, y, z, post(st['post']))
            if b.get(w // 2 - 1, top + 1, z) == st['infill'] and floors < 3: b.set(w // 2 - 1, top + 1, z, 'air')
    else:                                                  # framed gable ends, a dormer to the street
        for x in (0, w - 1):
            for z in range(d + 1):
                if b.get(x, top, z) == st['infill']: b.set(x, top, z, st['beam'] + '[axis=z]')
            for y in range(top + 1, top + rise + 1):
                if b.get(x, y, d // 2) == st['infill']: b.set(x, y, d // 2, post(st['post']))
            for z in (d // 2 - 2, d // 2 + 2):
                if b.get(x, top + 1, z) == st['infill']: b.set(x, top + 1, z, 'iron_bars[north=true,south=true]')
        cx = w // 2
        for dx in (-1, 0, 1):
            b.set(cx + dx, top + 2, d - 1, 'air' if dx == 0 else post(st['post']))
            for z in (d - 2, d - 1, d): b.set(cx + dx, top + 3, z, slab + '[type=bottom]')
    # chimney and hearth at the back
    hx = 2 if not hoist else None
    if hx:
        ch = top + max(2, rise // 2)                         # a stack that clears the roof slope by two, not a spike
        b.box(hx, G, -1, hx, ch - 1, -1, FOOT); b.box(hx, ch, -1, hx, ch + 1, -1, 'bricks'); b.set(hx, ch + 2, -1, 'brick_wall')
        b.set(hx, G + 1, 0, 'campfire[lit=true]'); b.set(hx, G + 2, 0, 'bricks'); b.set(hx - 1, G + 1, 0, 'bricks'); b.set(hx + 1, G + 1, 0, 'bricks')
    # ladders up the back wall
    lx = (ax0 - 1) if arch else w - 2
    for k in range(floors - 1):
        for y in range(G + 1 + 4 * k, G + 5 + 4 * k): b.set(lx, y, 1, 'ladder[facing=south]')
    # furniture: every room has a use; things stand against the walls, the way from door to ladder stays clear
    ix1 = (ax0 - 1) if arch else w - 2                     # last interior x on the ground floor
    if hx: b.set(hx, G + 1, 1, 'iron_bars[east=true,west=true]')   # a grate: nothing can step into the fire
    guild = w >= 17 and not sign
    ground = 'store' if hoist else 'shop' if shop else 'inn' if sign else 'guild' if guild else 'kitchen'
    upper = 'loft' if hoist else 'guest' if sign else 'archive' if guild else 'bedroom'
    furnish(b, w, d, floors, st, doors, lx, ix1, ground, upper, seed)
    # extras on the street face
    if hoist:
        yb = top - 1
        for z in (d + 1, d + 2): b.set(doors[1], yb, z, st['beam'] + '[axis=z]')
        b.set(doors[1], yb - 1, d + 2, 'chain[axis=y]'); b.set(doors[1], yb - 2, d + 2, 'barrel[facing=up]')
        b.air(doors[1], top - 3, d, doors[1], top - 2, d)
    if sign:
        b.set(w - 2, G + 6, d + 1, 'spruce_fence'); b.set(w - 2, G + 5, d + 1, 'lantern[hanging=true]')
        b.set(1, G + 6, d + 1, 'spruce_fence'); b.set(1, G + 5, d + 1, 'spruce_trapdoor[facing=south,half=bottom,open=true]')
    b.set(doors[0] - 1 if doors[0] > 1 else doors[-1] + 1, G + 3, d, 'lantern[hanging=true]') if not shop and not hoist else None
    return b


BAR2 = ['barrel[facing=up]', 'barrel[facing={F}]']
BOOKS = ['bookshelf', 'bookshelf']
HAY2 = ['hay_block[axis=x]', 'hay_block[axis=z]']
LAMP = ['barrel[facing=up]', 'lantern[hanging=false]']
CUP = ['chest[facing={F}]', None, 'spruce_trapdoor[facing={F},half=top,open=true]']
POT = ['stripped_spruce_log[axis=y]', 'flower_pot']
CANDLE = ['stripped_spruce_log[axis=y]', 'candle[candles=2,lit=false]', 'spruce_slab[type=top]']
ROOMS = {
    'kitchen': dict(A=[['smoker[facing={F},lit=false]'], POT, CANDLE, ['water_cauldron[level=3]'], 'TABLE', 'TABLE', LAMP, ['chest[facing={F}]']],
                    B=[BAR2, CUP, ['bookshelf'], ['composter[level=3]'], ['loom[facing={F}]'], ['hay_block[axis=x]']], N=[['crafting_table'], ['barrel[facing=up]'], HAY2], S=[LAMP], rug='brown'),
    'shop': dict(A=[BOOKS, BAR2, CUP, LAMP, ['loom[facing={F}]'], ['composter[level=5]'], HAY2, ['barrel[facing=up]']],
                 B=[BOOKS, ['cartography_table'], POT, ['chest[facing={F}]'], BAR2, ['fletching_table'], LAMP, ['barrel[facing={F}]']],
                 N=[['crafting_table'], BAR2, ['chest[facing={F}]'], ['barrel[facing=up]'], BOOKS, HAY2], S=[], rug='white'),
    'inn': dict(A=[BAR2, ['chest[facing={F}]'], [None, 'red_wall_banner[facing={F}]'], LAMP, 'TABLE', 'TABLE', BAR2, [None, 'red_wall_banner[facing={F}]'], ['barrel[facing=up]']],
                B=[], N=[['barrel[facing={F}]', 'barrel[facing={F}]'], ['barrel[facing={F}]'], BAR2, ['water_cauldron[level=3]'], ['brewing_stand'], ['barrel[facing={F}]', 'barrel[facing={F}]'], ['barrel[facing={F}]'], POT, ['smoker[facing={F},lit=false]']],
                S=[LAMP, [None, 'yellow_wall_banner[facing={F}]'], ['barrel[facing=up]'], POT], rug=None),
    'guild': dict(A=[BOOKS, [None, 'blue_wall_banner[facing={F}]'], BOOKS, ['lectern[facing={F}]'], BOOKS, [None, 'blue_wall_banner[facing={F}]'], BOOKS, CUP, BOOKS],
                  B=[BOOKS, ['cartography_table'], CUP, ['chest[facing={F}]'], [None, 'blue_wall_banner[facing={F}]'], ['loom[facing={F}]'], BOOKS, LAMP, ['chest[facing={F}]']],
                  N=[['chest[facing={F}]'], ['chest[facing={F}]'], BAR2, ['crafting_table'], BOOKS, BOOKS, ['lectern[facing={F}]'], POT], S=[LAMP, [None, 'yellow_wall_banner[facing={F}]'], POT], rug='blue'),
    'store': dict(A=[['chest[facing={F}]'], ['composter[level=4]'], HAY2, BAR2, ['white_wool', 'white_wool'], ['barrel[facing=up]'], CUP],
                  B=[['stripped_spruce_log[axis=z]', 'stripped_spruce_log[axis=z]'], ['stripped_spruce_log[axis=z]'], HAY2, BAR2, ['white_wool'], LAMP],
                  N=[BAR2, HAY2, ['barrel[facing=up]', 'barrel[facing=up]'], ['hay_block[axis=x]'], BAR2, ['white_wool', 'white_wool'], ['barrel[facing=up]'], HAY2, BAR2, ['barrel[facing=up]'], HAY2, BAR2], S=[['barrel[facing=up]'], ['hay_block[axis=z]']], rug=None),
    'bedroom': dict(A=['BED', ['chest[facing={F}]'], [None, '{C}_wall_banner[facing={F}]'], ['bookshelf'], ['loom[facing={F}]'], 'TABLE', LAMP],
                    B=['BED', CUP, LAMP, ['bookshelf'], POT, ['barrel[facing=up]']], N=[BOOKS, ['chest[facing={F}]'], ['bookshelf']], S=[['lectern[facing={F}]'], POT, ['barrel[facing=up]']], rug='{C}'),
    'workshop': dict(A=[['crafting_table'], ['smithing_table'], ['grindstone[face=floor,facing={F}]'], BAR2, ['stripped_spruce_log[axis=z]'], LAMP],
                     B=[['cartography_table'], ['fletching_table'], HAY2, ['barrel[facing=up]'], CUP], N=[['chest[facing={F}]'], BAR2], S=[['loom[facing={F}]'], ['composter[level=2]'], POT], rug=None),
    'guest': dict(A=['BED', ['chest[facing={F}]'], [None, 'red_wall_banner[facing={F}]'], 'BED', CUP, 'BED', LAMP, 'BED', ['chest[facing={F}]']],
                  B=['BED', ['chest[facing={F}]'], [None, 'red_wall_banner[facing={F}]'], 'BED', CUP, 'BED', LAMP, 'BED', ['chest[facing={F}]']],
                  N=[BOOKS, BAR2, ['chest[facing={F}]'], POT, BOOKS, LAMP], S=[POT, ['barrel[facing=up]'], 'TABLE', LAMP, ['lectern[facing={F}]'], POT], rug='red'),
    'archive': dict(A=[BOOKS, BOOKS, ['lectern[facing={F}]'], BOOKS, BOOKS, [None, 'blue_wall_banner[facing={F}]'], BOOKS, CUP, BOOKS],
                    B=['BED', ['chest[facing={F}]'], 'BED', CUP, [None, 'blue_wall_banner[facing={F}]'], 'BED', ['chest[facing={F}]'], LAMP],
                    N=[BOOKS, BOOKS, ['cartography_table'], BOOKS, ['chest[facing={F}]'], BOOKS], S=['TABLE', POT, ['lectern[facing={F}]'], LAMP, 'TABLE', POT], rug='blue'),
    'loft': dict(A=[HAY2, BAR2, ['white_wool', 'white_wool'], ['chest[facing={F}]'], ['hay_block[axis=x]'], BAR2, ['white_wool']],
                 B=[['barrel[facing=up]'], HAY2, ['white_wool'], ['crafting_table'], ['chest[facing={F}]'], BAR2, HAY2],
                 N=[BAR2, ['barrel[facing=up]'], HAY2, ['white_wool', 'white_wool'], BAR2, ['hay_block[axis=z]'], ['barrel[facing=up]']], S=[['barrel[facing=up]'], ['white_wool']], rug=None),
}
INWARD = {'east': (1, 0), 'west': (-1, 0), 'south': (0, 1), 'north': (0, -1)}


def furnish(b, w, d, floors, st, doors, lx, ix1, ground, upper, seed):
    """fill every storey from ROOMS: side A is the long wall away from the door, B the other, N the back, S the front"""
    def free(x, y, z): return b.get(x, y, z) in (None, 'air')
    def solid(x, y, z):
        v = b.get(x, y, z)
        return v is not None and v != 'air' and not any(t in v for t in ('bars', 'pane', 'trapdoor', 'slab', 'campfire'))
    for k in range(floors):
        role = ground if k == 0 else (upper if k == 1 else 'workshop')
        R = ROOMS[role]
        y, x1, z1 = G + 1 + 4 * k, (ix1 if k == 0 else w - 2), (d - 2 if k == 0 else d - 1)
        res = {(lx, 1), (lx, 2), (lx, 3), (lx - 1, 1), (lx - 1, 2)}
        if k == 0:
            for x in doors: res |= {(x, z) for z in range(1, z1 + 1)}
            res |= {(x, 2) for x in range(min(doors[0], lx), max(doors[-1], lx) + 1)} | {(2, 1), (2, 2)}
        else:
            res |= {(x, 2) for x in range(lx - 3, lx + 1)}
        a_east = doors[0] <= (1 + x1) // 2
        walls = {'A': 'E' if a_east else 'W', 'B': 'W' if a_east else 'E', 'N': 'N', 'S': 'S'}

        def place(side, items):
            wall = walls[side]
            if wall == 'W': cells, F, wc = [(1, z) for z in range(1, z1 + 1)], 'east', (lambda x, z: (0, z))
            elif wall == 'E': cells, F, wc = [(x1, z) for z in range(1, z1 + 1)], 'west', (lambda x, z: (x1 + 1, z))
            elif wall == 'N': cells, F, wc = [(x, 1) for x in range(2, x1)], 'south', (lambda x, z: (x, 0))
            else: cells, F, wc = [(x, z1) for x in range(2, x1)], 'north', (lambda x, z: (x, z1 + 1))
            it = list(items)
            dx, dz = INWARD[F]
            for i, (x, z) in enumerate(cells):
                if not it: break
                wx, wz = wc(x, z)
                if (x, z) in res or not free(x, y, z) or not solid(wx, y, wz): continue
                item = it[0]
                if item == 'BED':
                    if wall in ('N', 'S') or (x, z + 1) in res or z + 1 > z1 or not free(x, y, z + 1): continue
                    it.pop(0)
                    b.set(x, y, z, '%s_bed[facing=north,part=head]' % st['bed']); b.set(x, y, z + 1, '%s_bed[facing=north,part=foot]' % st['bed'])
                    continue
                it.pop(0)
                if item == 'TABLE':
                    b.set(x, y, z, 'spruce_fence'); b.set(x, y + 1, z, 'spruce_pressure_plate')
                    if (x + dx, z + dz) not in res and free(x + dx, y, z + dz): b.set(x + dx, y, z + dz, 'spruce_stairs[facing=%s,half=bottom,shape=straight]' % F)
                    continue
                window = not solid(wx, y + 1, wz)
                for j, blk in enumerate(item):
                    if blk is None: continue
                    if j and (window or not free(x, y + j, z)): break
                    b.set(x, y + j, z, blk.replace('{F}', F).replace('{C}', st['bed']))
        for side in 'ABNS': place(side, R[side])
        # the middle of the room
        cz = d // 2
        if role == 'inn':                                  # trestle tables either side of a clear way two wide, the bar in the east
            for tx in (4, 7, 14):
                for z0 in (4, 8, 11) if tx < 10 else (4,):
                    for z in (z0, z0 + 1): b.set(tx, y, z, 'spruce_fence'); b.set(tx, y + 1, z, 'spruce_pressure_plate')
                    b.set(tx - 1, y, z0, 'spruce_stairs[facing=west,half=bottom,shape=straight]'); b.set(tx + 1, y, z0 + 1, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
            for z in range(8, 13):
                b.set(x1 - 3, y, z, 'spruce_slab[type=top]'); b.set(x1, y, z, 'barrel[facing=west]')
                if z % 2: b.set(x1, y + 1, z, 'barrel[facing=west]')
            b.set(x1 - 3, y + 1, 9, 'flower_pot'); b.set(x1 - 3, y + 1, 12, 'candle[candles=3,lit=false]'); b.set(x1 - 2, y, 13, 'spruce_slab[type=top]'); b.set(x1 - 1, y, 13, 'spruce_slab[type=top]')
        if role == 'guild':                                # the council table, seats down both sides
            for z in range(4, 11):
                b.set(5, y, z, 'spruce_fence'); b.set(5, y + 1, z, 'spruce_pressure_plate')
                if z % 2 == 0: b.set(4, y, z, 'spruce_stairs[facing=west,half=bottom,shape=straight]'); b.set(6, y, z, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
            b.set(5, y + 2, 4, 'candle[candles=3,lit=false]'); b.set(5, y + 2, 10, 'candle[candles=3,lit=false]')
            for z in (5, 6): b.set(14, y, z, 'spruce_fence'); b.set(14, y + 1, z, 'spruce_pressure_plate')
            b.set(13, y, 5, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
        if role == 'shop':                                 # wares in the open counters, a display table each side of the door
            for x in range(1, w - 1):
                v = b.get(x, y, d - 1)
                if v and 'slab' in v and x % 2: b.set(x, y + 1, d - 1, 'flower_pot' if x % 4 == 1 else 'candle[candles=2,lit=false]')
            for tx in (3, w - 4):
                if (tx, cz) not in res: b.set(tx, y, cz, 'spruce_fence'); b.set(tx, y + 1, cz, 'spruce_pressure_plate')
        if role in ('archive', 'guest') and w >= 17:       # a reading table / a long runner
            for x in (w // 2 - 1, w // 2):
                if role == 'archive': b.set(x, y, cz, 'spruce_fence'); b.set(x, y + 1, cz, 'spruce_pressure_plate')
            if role == 'archive': b.set(w // 2 - 2, y, cz, 'spruce_stairs[facing=west,half=bottom,shape=straight]'); b.set(w // 2 + 1, y, cz, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
            else:
                for z in range(4, z1 - 1): b.set(w // 2, y, z, 'red_carpet')
        if R['rug']:
            c = R['rug'].replace('{C}', st['bed'])
            for x in range(3, min(5, x1 - 1) + 1):
                for z in (cz, cz + 1):
                    if (x, z) not in res and free(x, y, z) and x < x1 - 1: b.set(x, y, z, c + '_carpet')
        if k >= 1 and w >= 11 and role != 'archive':             # an island so the room is not all wall furniture
            iz = cz + 3 if cz + 3 <= z1 - 2 else cz - 3
            cells = [(w // 2 + 1, iz), (w // 2 + 2, iz)]
            if all((x, z) not in res and free(x, y, z) and free(x, y + 1, z) for x, z in cells) and iz >= 3:
                (xa, _), (xb, _) = cells
                if role in ('loft', 'workshop'):
                    b.set(xa, y, iz, 'barrel[facing=up]' if role == 'loft' else 'crafting_table'); b.set(xb, y, iz, 'hay_block[axis=x]' if role == 'loft' else 'barrel[facing=up]')
                    b.set(xa, y + 1, iz, 'white_wool' if role == 'loft' else 'lantern[hanging=false]')
                else:
                    for x in (xa, xb): b.set(x, y, iz, 'spruce_fence'); b.set(x, y + 1, iz, 'spruce_pressure_plate')
                    if free(xa - 1, y, iz) and (xa - 1, iz) not in res: b.set(xa - 1, y, iz, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
                    if free(xb + 1, y, iz) and (xb + 1, iz) not in res and xb + 1 < w - 2: b.set(xb + 1, y, iz, 'spruce_stairs[facing=east,half=bottom,shape=straight]')
        # light: lanterns hang from the floor above, or from a tie beam under the roof
        xs = [x for x in range(2, (x1 if k == 0 else w - 2), 4)]
        for x in xs:
            lz = cz if (x, cz) not in res or k else cz + 1
            if k == floors - 1:
                if not free(x, y + 3, lz): continue
                for bx in range(1, w - 1):
                    if free(bx, y + 3, lz): b.set(bx, y + 3, lz, 'stripped_spruce_log[axis=x]')
            elif not solid(x, y + 3, lz): continue
            if free(x, y + 2, lz): b.set(x, y + 2, lz, 'lantern[hanging=true]')


KINDS = {
    's1a': dict(w=7, d=11, floors=3, style='slate'), 's1b': dict(w=7, d=11, floors=3, style='tile', door=5), 's1c': dict(w=7, d=11, floors=2, style='shingle', door=3),
    's2a': dict(w=11, d=11, floors=2, style='tile', shop=True), 's2b': dict(w=11, d=11, floors=2, style='slate', shop=True, ridge='z'), 's2c': dict(w=11, d=11, floors=2, style='brick', door=2),
    's3a': dict(w=13, d=13, floors=2, style='slate', arch=True, door=2), 's3b': dict(w=13, d=13, floors=2, style='shingle', arch=True, door=3, ridge='z'),
    's4inn': dict(w=20, d=16, floors=2, style='tile', sign=True), 's4guild': dict(w=20, d=16, floors=2, style='stone', door=9),
    's5a': dict(w=16, d=10, floors=2, style='shingle', hoist=True, door=8), 's5b': dict(w=16, d=10, floors=2, style='brick', hoist=True, door=5),
}


def kind(name, seed=1):
    return house(seed=seed, **KINDS[name])


if __name__ == '__main__':
    b = Build(seed=5)
    rows = [['s1a', 's1b', 's1c', 's2a', 's2b', 's2c'], ['s3a', 's3b', 's4inn', 's4guild'], ['s5a', 's5b']]
    for r, names in enumerate(rows):
        ox = 0
        for n in names:
            hb = kind(n, seed=len(n) + ox)
            for (x, y, z), v in hb.c.items(): b.set(x + ox, y, z + r * 30, v)
            ox += KINDS[n]['w'] + 4
    (x0, y0, z0), (x1, y1, z1) = b.bounds()
    for x in range(x0 - 4, x1 + 5):
        for z in range(z0 - 4, z1 + 9):
            if b.get(x, G, z) is None: b.set(x, G, z, 'cobblestone' if (z % 30) > 10 else 'grass_block')
            b.set(x, G - 1, z, 'stone')
    print(b.emit(sys.argv[1], 'rg-houses', 'Rivergate house sampler', pad=((x0 - 6, G - 2, z0 - 6), (x1 + 6, 110, z1 + 10))), 'ops', b.bounds())
    print('hs_0 %d 63 %d %d 86 %d' % (x0 - 4, z0 - 4, x0 + 43, z0 + 43))
    print('hs_1 %d 63 %d %d 86 %d' % (x0 + 44, z0 - 4, x1 + 4, z0 + 43))
    print('hs_2 %d 63 %d %d 86 %d' % (x0 - 4, z0 + 44, x0 + 43, z1 + 8))
    print('hs_3 %d 63 %d %d 86 %d' % (x0 + 44, z0 + 44, x1 + 4, z1 + 8))
