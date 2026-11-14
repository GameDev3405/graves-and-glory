"""Frostmarch: the small pieces. Each returns {(x, y, z): block} in local coordinates with the ground block at y G.
gatehouse: the arch at a shelf ramp head (the road runs along x, the opening is 11 wide and 5 clear).
waystation: an open-fronted stable and store at a wagon stop (front to +z).
hut: an ice-fisher's hut on the tarn shore (door to +z).
  python fmpieces.py OUT.png gatehouse|waystation|hut [turn]"""
import sys, os
from tower import pick, S, h3, WALL, FOOT

TIMBER = [('spruce_planks', 6), ('stripped_spruce_log[axis=y]', 2), ('dark_oak_planks', 2)]


def gatehouse(G=80):
    c = {}
    def put(x, y, z, b): c[(x, y, z)] = b
    for sz in (-1, 1):
        for x in range(-3, 4):
            for dz in range(6, 10):
                z = sz * dz
                for y in range(G + 1, G + 11):
                    b = pick(FOOT if y <= G + 2 else WALL, x, y, z)
                    if y == G + 7: b = 'polished_andesite'
                    if abs(x) == 3 and dz in (6, 9) and y > G + 2: b = 'stone_bricks' if y % 4 else 'chiseled_stone_bricks'
                    put(x, y, z, b)
                put(x, G + 11, z, pick(WALL, x, G + 11, z) if (x + dz) % 2 == 0 or (abs(x) == 3 and dz in (6, 9)) else 'snow_block' if abs(x) < 3 and dz in (7, 8) else 'air')
            for y in (G + 4, G + 5): put(x, y, sz * 9, 'air') if x == 0 else None        # an arrow loop outward
        for x in (-4, 4):                                                               # a battered foot
            for dz in range(6, 10):
                for y in (G + 1, G + 2): put(x, y, sz * dz, pick(FOOT, x, y, sz * dz))
    for x in range(-2, 3):                                                              # the bridge over the road
        for z in range(-5, 6):
            for y in (G + 7, G + 8, G + 9): put(x, y, z, 'polished_andesite' if y == G + 7 else pick(WALL, x, y, z))
            if abs(z) == 5: put(x, G + 6, z, pick(WALL, x, G + 6, z))
            if abs(x) == 2:
                put(x, G + 10, z, pick(WALL, x, G + 10, z))
                if z % 2 == 0: put(x, G + 11, z, pick(WALL, x, G + 11, z))
            else: put(x, G + 10, z, 'snow_block' if h3(x, 2, z, 3) else 'air')
    for z in (-3, 3): put(0, G + 6, z, 'lantern[hanging=true]')
    return c


def waystation(G=64):
    c = {}
    def put(x, y, z, b): c[(x, y, z)] = b
    def roof(z): return G + 5 + (z + 6) // 4
    for x in range(-7, 8):
        for z in range(-5, 6):
            put(x, G, z, pick([('coarse_dirt', 5), ('spruce_planks', 3), ('gravel', 2)], x, G, z))
            wall = z == -5 or abs(x) == 7
            post = (abs(x) == 7 and z in (-5, 0, 5)) or (z == -5 and x % 7 == 0) or (z == 5 and x in (-7, -3, 3, 7))
            for y in range(G + 1, roof(z)):
                if post: put(x, y, z, 'spruce_log[axis=y]')
                elif wall: put(x, y, z, pick(FOOT, x, y, z) if y == G + 1 else pick(TIMBER, x, y, z))
    for x in range(-7, 8): put(x, G + 4, 5, 'stripped_spruce_log[axis=x]'); put(x, G + 5, 5, 'spruce_planks'); put(x, G + 6, 5, 'spruce_planks')
    for x in range(-8, 9):                                                              # a shed roof, low at the back, under snow
        for z in range(-6, 7):
            y = roof(z); put(x, y, z, 'spruce_planks' if abs(x) < 8 else 'stripped_spruce_log[axis=z]')
            if h3(x, 5, z, 5): put(x, y + 1, z, 'snow_block')
    for z in (-4, -3, -2):                                                              # stall walls one block high, hay and stores
        put(-3, G + 1, z, 'spruce_log[axis=z]'); put(1, G + 1, z, 'spruce_log[axis=z]')
    for (x, z, b) in ((-6, -4, 'hay_block[axis=y]'), (-5, -4, 'hay_block[axis=x]'), (-6, -3, 'hay_block[axis=y]'), (-1, -4, 'hay_block[axis=z]'), (3, -4, 'barrel[facing=up]'), (4, -4, 'barrel[facing=up]'),
                      (5, -4, 'barrel[facing=up]'), (6, -4, 'spruce_planks'), (6, -3, 'barrel[facing=up]'), (6, -2, 'hay_block[axis=y]')): put(x, G + 1, z, b)
    put(-6, G + 2, -4, 'hay_block[axis=x]'); put(4, G + 2, -4, 'barrel[facing=up]'); put(6, G + 2, -4, 'barrel[facing=south]')
    for x in (-5, 0, 5): put(x, G + 3, 5, 'lantern[hanging=true]')
    for z in range(-3, 2):                                                              # the woodpile against the west wall
        for y in (G + 1, G + 2):
            put(-9, y, z, 'spruce_log[axis=z]')
        put(-10, G + 1, z, 'spruce_log[axis=z]')
    return c


def hut(G=64, k=0):
    c = {}
    def put(x, y, z, b): c[(x, y, z)] = b
    for x in range(-3, 4):
        for z in range(-3, 4):
            m = max(abs(x), abs(z)); put(x, G, z, 'spruce_planks')
            for y in range(G + 1, G + 5):
                if m < 3: put(x, y, z, 'air')
                elif abs(x) == 3 and abs(z) == 3: put(x, y, z, 'spruce_log[axis=y]')
                else: put(x, y, z, pick(FOOT, x, y, z) if y == G + 1 else pick(TIMBER, x + k, y, z))
    put(0, G + 1, 3, 'air'); put(0, G + 2, 3, 'air'); put(0, G + 3, 3, 'stripped_spruce_log[axis=x]')
    put(3, G + 2, 0, 'glass_pane'); put(-3, G + 2, 0, 'glass_pane')
    for i in range(0, 4):                                                               # a snowed pyramid roof
        y = G + 5 + i; rr = 4 - i
        for x in range(-rr, rr + 1):
            for z in range(-rr, rr + 1):
                if max(abs(x), abs(z)) == rr or i == 3: put(x, y, z, 'snow_block' if h3(x + k, i, z, 4) and i else 'spruce_planks')
    put(0, G + 9, 0, 'snow_block')
    put(-2, G + 1, -2, 'barrel[facing=up]'); put(2, G + 1, -2, 'barrel[facing=up]'); put(-2, G + 1, 1, 'hay_block[axis=x]'); put(0, G + 4, 0, 'lantern[hanging=true]')
    return c


if __name__ == '__main__':
    HERE = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
    from mapforge.world import World, Cell
    from mapforge.structure import normalize
    from mapforge import render
    G = 80; c = {'gatehouse': gatehouse, 'waystation': waystation, 'hut': hut}[sys.argv[2]](G); turns = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    for x in range(-14, 15):
        for z in range(-14, 15): c.setdefault((x, G, z), 'snow_block')
    for _ in range(turns): c = {(-z, y, x): v for (x, y, z), v in c.items()}
    w = World('piece')
    for p, v in c.items():
        if v != 'air': w.set(p, Cell(normalize(v)))
    lo, hi = (-14, G, -14), (14, G + 16, 14)
    img = render.render(w, view='iso', lo=lo, hi=hi, grid=False, title='Frostmarch: ' + sys.argv[2]); img.save(sys.argv[1]); print(sys.argv[1], img.size, len(c))
