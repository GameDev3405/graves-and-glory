"""Hollowwood: the Yoke, a giant's collarbone arched over the shrine on the Hollow floor. No face.
yoke(G) returns {(x, y, z): block}, local coordinates, the floor block at y G, the shrine centre at (0, 0).
The capture ring (radius 17) keeps a flat floor with nothing standing in it: the shrine is an inlay, the feet and stones stand outside.
  python yoke.py OUT.png [iso|top]"""
import sys, os, math

BONE = [('bone_block[axis=x]', 6), ('bone_block[axis=y]', 2), ('calcite', 3), ('smooth_quartz', 2), ('diorite', 1)]
OLD = [('calcite', 3), ('diorite', 3), ('bone_block[axis=y]', 2), ('mossy_cobblestone', 2), ('moss_block', 2)]
STONE = [('mossy_cobblestone', 4), ('cobblestone', 3), ('mossy_stone_bricks', 2), ('andesite', 2)]
SPAN, RISE = 30, 24                                                             # Kort on draft 1: 'Make it bigger and grander'; shrine: 'Add a glowing centre'


def h3(x, y, z, k=0):
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (k * 2654435761); n = (n ^ (n >> 13)) * 1274126177 & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65536.0


def pick(m, x, y, z, k=0):
    t = h3(x, y, z, k) * sum(w for _, w in m)
    for b, w in m:
        t -= w
        if t < 0: return b
    return m[-1][0]


def curve(t):
    """the bone's centre line: a collarbone's shallow S in plan, arched in height"""
    return -SPAN + 2 * SPAN * t, 1.5 + RISE * math.sin(math.pi * t) ** 0.75, 6.5 * math.sin(2 * math.pi * t)


def yoke(G=62):
    c = {}
    def put(x, y, z, b):
        if y > G: c[(x, y, z)] = b
    def ball(cx, cy, cz, r, flat=1.0, m=BONE):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for y in range(int(cy - r) - 1, int(cy + r) + 2):
                for z in range(int(cz - r) - 1, int(cz + r) + 2):
                    if math.hypot(x - cx, (y - cy) / flat, z - cz) <= r:
                        lowm = OLD if y - cy < -r * 0.2 and cy < 7 else m
                        put(x, G + y, z, pick(lowm, x // 2, y, z // 2))
    n = 400
    for i in range(n + 1):
        t = i / n; x, y, z = curve(t); e = abs(2 * t - 1)
        ball(x, y, z, 2.6 + 2.0 * e ** 2.2)                                        # thin in the shaft, thick toward the ends
    for sg, r in ((-1, 5.2), (1, 5.2)):                                            # the two joint ends, half sunk in the floor
        x, y, z = curve(0 if sg < 0 else 1)
        ball(x + sg * 1.5, 1.5, z, r, 0.9); ball(x + sg * 0.5, 4.0, z - sg * 2.5, r - 1.4)
    for sg in (0, 1):                                                              # roots creeping up both feet
        for k in range(4):
            for i in range(0, 120):
                t = i / 120 * (0.2 + 0.05 * k); t = t if sg == 0 else 1 - t; x, y, z = curve(t); e = abs(2 * t - 1); r = 2.6 + 2.0 * e ** 2.2 + 0.5
                ph = k * 1.6 + i * 0.045 * (1 if k % 2 else -1)
                put(round(x), G + round(y + r * math.cos(ph)), round(z + r * math.sin(ph)), 'dark_oak_wood[axis=x]' if h3(i, k, sg) < 0.8 else 'rooted_dirt')
    tops = {}
    for (x, y, z) in c: tops[(x, z)] = max(tops.get((x, z), 0), y)
    for (x, z), y in tops.items():                                                 # moss and leaf on the bone's back
        if c[(x, y, z)].startswith('dark_oak'): continue
        nz = (h3(x // 4, 7, z // 4) + h3(x // 2, 8, z // 2)) / 2
        if nz < 0.42: c[(x, y, z)] = 'moss_block'
        if nz < 0.2 and y > G + 8: c[(x, y + 1, z)] = 'flowering_azalea_leaves[persistent=true]' if h3(x, 9, z) < 0.25 else 'azalea_leaves[persistent=true]'
    for (x, y, z) in [k for k in c]:                                               # no undercut at the feet: fill below the bone down to the floor
        if y <= G + 6 and abs(x) >= SPAN - 7:
            for yy in range(G + 1, y):
                if (x, yy, z) not in c: c[(x, yy, z)] = pick(OLD, x // 2, yy - G, z // 2)
    tops = {}
    for (x, y, z) in c: tops[(x, z)] = max(tops.get((x, z), 0), y)
    for (x, z), y in tops.items():                                                 # sheer feet: nothing a troop can step up onto (no top lower than +4)
        for yy in range(G + 1, G + 5):
            if y < G + 4 and (x, yy, z) not in c: c[(x, yy, z)] = pick(OLD, x // 2, yy - G, z // 2)
    for x in range(-19, 20):                                                       # the shrine: a flat inlay in the floor, flush with the ground
        for z in range(-19, 20):
            d = math.hypot(x, z)
            if d > 18.6: continue
            a = math.atan2(z, x)
            if d <= 2.2: b = 'chiseled_stone_bricks' if d < 1 else 'polished_andesite'
            elif d <= 6.5: b = 'mossy_stone_bricks' if h3(x, 0, z) < 0.5 else 'stone_bricks'
            elif abs(d - 9) <= 0.6 or abs(d - 14) <= 0.6: b = 'polished_andesite'
            elif d < 14 and abs(math.sin(a * 4)) < 0.16: b = 'stone_bricks'
            elif d > 16.2: b = pick(STONE, x, 0, z)
            else: b = 'moss_block' if h3(x, 1, z) < 0.6 else 'grass_block' if h3(x, 2, z) < 0.5 else 'mossy_cobblestone'
            c[(x, G, z)] = b
            if d <= 1.5 or (abs(d - 9) <= 0.6 and abs(math.sin(a * 4)) < 0.16): c[(x, G, z)] = 'orange_stained_glass'; c[(x, G - 1, z)] = 'shroomlight'   # the glowing centre and eight lit stones
    for i in range(10):                                                            # offering stones, outside the ring, clear of the feet
        a = math.radians(18 + i * 36); x, z = round(24 * math.cos(a)), round(24 * math.sin(a))
        if abs(x) > 12 and abs(z) < 14: continue
        hgt = 4 + int(h3(i, 5, 0) * 3)
        for dx in (0, 1):
            for dz in (0, 1):
                for y in range(1, hgt + (1 if dx + dz == 0 else 0)): c[(x + dx, G + y, z + dz)] = pick(STONE, x + dx, y, z + dz)
    for t in (0.36, 0.5, 0.64):                                                    # lanterns hung under the arch, out of reach
        x, y, z = curve(t); x, z = round(x), round(z); y = min(yy for (xx, yy, zz) in c if xx == x and zz == z and yy > G + 6)
        c[(x, y - 1, z)] = 'chain[axis=y]'; c[(x, y - 2, z)] = 'chain[axis=y]'; c[(x, y - 3, z)] = 'lantern[hanging=true]'
    tops = {}
    for (x, y, z) in c:
        if not c[(x, y, z)].startswith(('lantern', 'chain')): tops[(x, z)] = max(tops.get((x, z), 0), y)
    low = [k for k, y in tops.items() if G < y < G + 3]
    assert not low, low[:5]                                                        # nothing a troop can step up onto: every top is the floor or at least 3 up
    return c


if __name__ == '__main__':
    HERE = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
    from mapforge.world import World, Cell
    from mapforge.structure import normalize
    from mapforge import render
    G = 62; c = yoke(G); view = sys.argv[2] if len(sys.argv) > 2 else 'iso'
    for x in range(-44, 45):
        for z in range(-44, 45):
            if math.hypot(x, z) <= 44: c.setdefault((x, G, z), 'grass_block' if h3(x, 3, z) < 0.6 else 'moss_block')
    ring = [(x, z) for x in range(-17, 18) for z in range(-17, 18) if math.hypot(x, z) < 17]
    print('blocks standing in the ring:', sum(1 for (x, y, z) in c if (x, z) in ring and G < y <= G + 4), '| lowest bone over the ring: +%d' % min(y - G for (x, y, z) in c if (x, z) in ring and y > G))
    w = World('piece')
    for p, v in c.items(): w.set(p, Cell(normalize(v)))
    lo, hi = (-44, G - 1, -44), (44, G + 34, 44)
    img = render.render(w, view=view, lo=lo, hi=hi, grid=False, title='Hollowwood: the Yoke'); img.save(sys.argv[1]); print(sys.argv[1], img.size, len(c))
