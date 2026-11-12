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
        b.box(hx, G, -1, hx, top + rise - 1, -1, FOOT); b.box(hx, top + rise - 1, -1, hx, top + rise + 1, -1, 'bricks'); b.set(hx, top + rise + 2, -1, 'brick_wall')
        b.set(hx, G + 1, 0, 'campfire[lit=true]'); b.set(hx, G + 2, 0, 'bricks'); b.set(hx - 1, G + 1, 0, 'bricks'); b.set(hx + 1, G + 1, 0, 'bricks')
    # ladders up the back wall
    lx = (ax0 - 1) if arch else w - 2
    for k in range(floors - 1):
        for y in range(G + 1 + 4 * k, G + 5 + 4 * k): b.set(lx, y, 1, 'ladder[facing=south]')
    # furniture
    ix1 = (ax0 - 1) if arch else w - 2                     # last interior x on the ground floor
    Y = G + 1
    if hoist:
        for x in range(1, w - 1):
            for z in (1, 2):
                if x == lx and z == 1: continue
                h = (x * 7 + z * 3 + seed) % 3
                for y in range(h): b.set(x, Y + y, z, 'barrel[facing=up]' if (x + y + z) % 3 else 'hay_block[axis=x]')
        b.set(1, Y, d - 2, 'chest[facing=east]'); b.set(1, Y, d - 3, 'composter[level=4]'); b.set(w - 2, Y, d - 2, 'stripped_spruce_log[axis=z]'); b.set(w - 2, Y, d - 3, 'stripped_spruce_log[axis=z]')
        b.set(w // 2, Y + 2, d // 2, 'lantern[hanging=true]')
    else:
        b.set(1, Y, d // 2, 'spruce_fence'); b.set(1, Y + 1, d // 2, 'spruce_pressure_plate'); b.set(1, Y, d // 2 + 1, 'spruce_stairs[facing=north,half=bottom,shape=straight]')
        b.set(ix1, Y, d - 2, 'barrel[facing=up]') if ix1 not in doors else None
        b.set(ix1, Y, d // 2, 'chest[facing=west]'); b.set(ix1, Y + 1, d - 2, 'lantern[hanging=false]') if ix1 not in doors else None
        b.set(min(4, ix1), Y, 1, 'crafting_table' if not shop else 'barrel[facing=up]')
        b.set((1 + ix1) // 2 + 1, Y + 2, d // 2, 'lantern[hanging=true]')
        if sign or w >= 17:                                # a common room: trestle tables down the middle
            for z0 in [3] + ([d - 7] if d >= 14 else []):
                for x in range(4, ix1 - 1, 4):
                    if x in doors or x + 1 in doors and z0 > 3: continue
                    for z in (z0, z0 + 1): b.set(x, Y, z, 'spruce_fence'); b.set(x, Y + 1, z, 'spruce_pressure_plate')
                    b.set(x - 1, Y, z0, 'spruce_stairs[facing=east,half=bottom,shape=straight]'); b.set(x + 1, Y, z0 + 1, 'spruce_stairs[facing=west,half=bottom,shape=straight]')
            for z in range(d - 6, d - 2):                  # the bar along the east wall, kegs behind it
                b.set(ix1 - 2, Y, z, 'spruce_slab[type=top]'); b.set(ix1, Y, z, 'barrel[facing=west]')
                if z % 2: b.set(ix1, Y + 1, z, 'barrel[facing=west]')
            b.set(ix1 - 2, Y + 1, d - 5, 'flower_pot'); b.set(ix1 - 1, Y + 2, d - 4, 'lantern[hanging=true]')
            for x in (5, 9, 13):
                if x < ix1 - 3: b.set(x, Y + 2, d // 2, 'lantern[hanging=true]')
            for x in range(w - 6, w - 2): b.set(x, Y, 1, 'barrel[facing=south]'); b.set(x, Y + 1, 1, 'barrel[facing=south]') if x % 2 else None
    for k in range(1, floors):
        y = G + 1 + 4 * k
        b.set(1, y, 2, '%s_bed[facing=north,part=head]' % st['bed']); b.set(1, y, 3, '%s_bed[facing=north,part=foot]' % st['bed']); b.set(1, y, 4, 'chest[facing=east]')
        b.set(w - 2, y, d - 1, 'bookshelf' if k == 1 else 'barrel[facing=up]'); b.set(w - 2, y, d - 2, 'loom[facing=west]' if k == 1 else 'hay_block[axis=z]')
        b.set(w // 2, y + 2, d // 2, 'lantern[hanging=true]')
        b.set(2, y, d - 1, 'spruce_fence'); b.set(2, y + 1, d - 1, 'spruce_pressure_plate')
        if w >= 11:
            b.set(w - 2, y, 3, '%s_bed[facing=north,part=foot]' % st['bed']); b.set(w - 2, y, 2, '%s_bed[facing=north,part=head]' % st['bed'])
            for x in range(4, w - 4): b.set(x, y, d // 2, '%s_carpet' % st['bed'])
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
