"""Frostmarch: the Refuge, a travellers' hospice at the back of the Refuge Shelf. Same foot as the Signal Tower
(13 x 13, a 3-wide door to +z, one ground-floor room, a lean-to on the west) so the two shelves give the same cover;
above the ground floor it is a different building: a stout stone house with a steep snow roof, a smoking chimney
and a bell. refuge(G) returns {(x, y, z): block} in local coordinates, ground block at y G.
  python refuge.py OUT.png [turn]"""
import sys, os
from tower import pick, S, h3, FOOT

WALL = [('stone_bricks', 40), ('cobblestone', 26), ('andesite', 16), ('mossy_cobblestone', 6), ('cracked_stone_bricks', 12)]
ROOF = 'deepslate_tile'


def refuge(G=80):
    c = {}
    def put(x, y, z, b): c[(x, y, z)] = b
    for y in range(G + 1, G + 8):                           # stone ground storey, battered like the tower's foot
        h = y - G
        for x in range(-6, 7):
            for z in range(-6, 7):
                if max(abs(x), abs(z)) < 6: put(x, y, z, 'air'); continue
                put(x, y, z, pick(FOOT if h <= 2 else WALL, x, y, z))
    for x in range(-5, 6):
        for z in range(-5, 6):
            put(x, G, z, pick([('spruce_planks', 6), ('stone_bricks', 2), ('cobblestone', 2)], x, G, z)); put(x, G + 7, z, 'spruce_planks')
    for y in (G + 1, G + 2, G + 3):
        for x in (-1, 0, 1): put(x, y, 6, 'air')
    for x in (-2, -1, 0, 1, 2): put(x, G + 4, 6, 'stripped_spruce_log[axis=x]')
    for (x, z) in ((-4, 6), (4, 6), (6, -3), (6, 3), (-6, 3)):
        put(x, G + 2, z, 'glass_pane'); put(x, G + 3, z, 'glass_pane')
    # timber upper storey, jettied on the long sides, and the steep roof with its ridge along x
    for y in range(G + 8, G + 12):
        for x in range(-6, 7):
            for z in range(-6, 7):
                if max(abs(x), abs(z)) < 6: put(x, y, z, 'air'); continue
                post = (abs(x) == 6 and abs(z) == 6) or (abs(x) == 6 and z % 4 == 0) or (abs(z) == 6 and x % 4 == 0)
                put(x, y, z, 'spruce_log[axis=y]' if post else ('stripped_spruce_log[axis=x]' if y == G + 8 and abs(z) == 6 else 'stripped_spruce_log[axis=z]' if y == G + 8 else 'calcite'))
    for (x, z) in ((-2, 6), (2, 6), (-2, -6), (2, -6)): put(x, G + 10, z, 'glass_pane')
    for i in range(0, 8):                                   # roof courses
        y = G + 12 + i
        for x in range(-7, 8):
            for (z, f) in ((-7 + i, 'south'), (7 - i, 'north')):
                put(x, y, z, 'snow_block' if (h3(x, i, z, 4) and i not in (0, 7)) else S(ROOF + '_stairs', f))
            if abs(x) == 6:
                for z in range(-6 + i, 7 - i): put(x, y, z, 'spruce_planks' if i < 5 else 'stripped_spruce_log[axis=z]')
            elif abs(x) < 6:
                for z in range(-6 + i, 7 - i): put(x, y, z, 'air')
    for x in range(-7, 8): put(x, G + 19, 0, 'deepslate_tile_slab[type=bottom]' if x % 3 else 'snow_block')
    # chimney on the back wall, smoking; nothing reaches the top
    for y in range(G + 1, G + 22):
        for x in (3, 4):
            for z in (-7, -6): put(x, y, z, pick([('cobblestone', 5), ('stone_bricks', 3), ('andesite', 2)], x, y, z))
    put(3, G + 22, -7, 'campfire[lit=true,signal_fire=true]'); put(4, G + 22, -6, 'cobblestone_wall'); put(4, G + 22, -7, 'cobblestone_wall'); put(3, G + 22, -6, 'cobblestone_wall'); put(3, G + 21, -7, 'hay_block[axis=y]')
    # the bell cote over the door
    for y in range(G + 12, G + 17):
        for x in (-1, 1): put(x, y, 7, 'spruce_fence' if y < G + 16 else 'spruce_planks')
    put(0, G + 16, 7, 'spruce_planks'); put(0, G + 15, 7, 'bell[attachment=ceiling,facing=north]'); put(0, G + 17, 7, 'snow_block'); put(-1, G + 17, 7, 'spruce_slab[type=bottom]'); put(1, G + 17, 7, 'spruce_slab[type=bottom]')
    put(-2, G + 5, 7, 'lantern[hanging=false]') if False else None
    for x in (-2, 2): put(x, G + 3, 7, 'lantern[hanging=true]'); put(x, G + 4, 7, 'spruce_fence')
    # the room: cold hearth, bunks, table
    put(3, G + 1, -5, 'cobblestone'); put(4, G + 1, -5, 'cobblestone')
    for z in (-4, -1, 2): put(-5, G + 1, z, 'red_wool'); put(-5, G + 1, z + 1, 'white_wool')
    put(4, G + 1, 3, 'barrel[facing=up]'); put(4, G + 1, 4, 'barrel[facing=up]'); put(0, G + 6, 0, 'lantern[hanging=true]')
    # the same lean-to as the tower's woodshed, here a sledge shelter
    for z in range(-3, 4):
        for x in (-9, -8, -7): put(x, G + 4 - (-7 - x), z, 'spruce_slab[type=bottom]' if x != -7 else 'spruce_planks')
        if abs(z) == 3:
            for y in range(G + 1, G + 3): put(-9, y, z, 'spruce_fence')
        elif z != 0: put(-8, G + 1, z, 'spruce_log[axis=z]'); put(-7, G + 1, z, 'spruce_log[axis=z]'); put(-7, G + 2, z, 'spruce_log[axis=z]')
    return c


if __name__ == '__main__':
    HERE = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
    from mapforge.world import World, Cell
    from mapforge.structure import normalize
    from mapforge import render
    G = 80; c = refuge(G); turns = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    for x in range(-14, 15):
        for z in range(-14, 15): c.setdefault((x, G, z), 'snow_block')
    for _ in range(turns): c = {(-z, y, x): v for (x, y, z), v in c.items()}
    w = World('refuge')
    for p, v in c.items():
        if v != 'air': w.set(p, Cell(normalize(v)))
    lo, hi = (-14, G, -14), (14, G + 26, 14)
    img = render.render(w, view='iso', lo=lo, hi=hi, grid=False, title='Frostmarch: the Refuge'); img.save(sys.argv[1]); print(sys.argv[1], img.size, len(c))
