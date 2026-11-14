"""Hollowwood: the giant stumps. A, the Great Stump (the loggers' stockade), and C, the Drowned Stump (rotted, in the bog, stilt huts on its rim).
Both come from one shape, so the two points play the same; only blocks and out-of-reach dressing differ.
stump(G, rotted) returns {(x, y, z): block}, local coordinates, forest floor block at y G, centre (0, 0), top floor at G + 6.
Shape as in hwmap.py: ellipse 40 x 34; inside 0.82 the flat top; 0.82..1 the bark rim; two splits with ramps (directions SPLITS).
The map builder cuts the High Walk's door through the rim.
  python stump.py OUT.png [iso|top] [rotted]"""
import sys, os, math
from yoke import h3, pick

RX, RZ, TOP = 40, 34, 6
SPLITS = [(-22, 16), (42, -4)]
SHED = (-30, -22, -5, 5)                                                        # x0, x1, z0, z1 on the top: sawmill / store hut, open to +x
PILE = (-3, 3, -24, -22)                                                        # a stack 3 high on the top
PAL = {
    False: dict(bark=[('dark_oak_log[axis=y]', 6), ('dark_oak_wood[axis=y]', 3), ('spruce_log[axis=y]', 2)], crown=[('stripped_dark_oak_wood[axis=y]', 3), ('dark_oak_log[axis=y]', 2), ('stripped_spruce_wood[axis=y]', 2)],
                inner=[('stripped_spruce_wood[axis=y]', 5), ('stripped_dark_oak_wood[axis=y]', 3), ('spruce_planks', 1)], ring=['stripped_spruce_log[axis=y]', 'stripped_spruce_log[axis=y]', 'stripped_oak_log[axis=y]', 'stripped_spruce_log[axis=y]', 'stripped_dark_oak_log[axis=y]'],
                patch=None, ramp=[('coarse_dirt', 4), ('packed_mud', 3), ('spruce_planks', 2)], post='spruce_log[axis=y]', beam='stripped_spruce_log', wall=[('spruce_planks', 6), ('stripped_spruce_wood[axis=y]', 2)], roof='dark_oak_planks',
                pile='spruce_log[axis=x]', root=[('dark_oak_wood[axis=y]', 5), ('dark_oak_log[axis=y]', 3), ('rooted_dirt', 1)]),
    True: dict(bark=[('dark_oak_wood[axis=y]', 5), ('spruce_wood[axis=y]', 2), ('moss_block', 4), ('rooted_dirt', 1)], crown=[('moss_block', 4), ('stripped_dark_oak_wood[axis=y]', 2), ('dark_oak_wood[axis=y]', 2)],
               inner=[('stripped_dark_oak_wood[axis=y]', 4), ('moss_block', 4), ('rooted_dirt', 1)], ring=['stripped_dark_oak_log[axis=y]', 'stripped_dark_oak_log[axis=y]', 'stripped_spruce_log[axis=y]', 'stripped_dark_oak_log[axis=y]', 'stripped_dark_oak_log[axis=y]'],
               patch=[('moss_block', 1)], ramp=[('packed_mud', 5), ('moss_block', 3), ('spruce_planks', 2)], post='dark_oak_log[axis=y]', beam='stripped_dark_oak_log', wall=[('spruce_planks', 5), ('dark_oak_planks', 3), ('moss_block', 1)],
               roof='spruce_planks', pile='dark_oak_log[axis=x]', root=[('dark_oak_wood[axis=y]', 4), ('moss_block', 4), ('rooted_dirt', 2)]),
}


def noise(a, k, n=3):
    """smooth noise round the rim: a in radians"""
    return sum(math.sin(a * f + h3(f, k, 0) * 6.283) / (i + 1) for i, f in enumerate((5, 11, 23)[:n])) / 1.8


def shape(x, z):
    """('top' | 'bark' | 'ramp' | 'root' | None, height above G)"""
    f = math.hypot(x / RX, z / RZ); a = math.atan2(z, x)
    if f <= 1.22:
        for ux, uz in SPLITS:
            n = math.hypot(ux, uz); ux, uz = ux / n, uz / n
            if x * ux + z * uz > 0 and abs(x * uz - z * ux) <= 4.5 and f >= 0.78: return 'ramp', min(TOP, max(0, int((1.22 - f) / 0.44 * 7)))
    if f <= 0.82: return 'top', TOP
    edge = 1 + 0.03 * math.sin(a * 17) + 0.02 * noise(a, 1)                       # ridged bark
    if f <= edge: return 'bark', TOP + 6 + int(2.5 + 2.5 * noise(a, 2)) + (1 if h3(x, 4, z) < 0.3 else 0)
    k = round(a / (2 * math.pi) * 11)                                              # eleven buttress roots
    ra = k * 2 * math.pi / 11 + 0.12 * math.sin(k * 5.1); lat = math.sin(a - ra) * f * 37; out = (f - 1) * 37
    if any(x * ux + z * uz > 0 and abs(x * uz - z * ux) <= 9 * math.hypot(ux, uz) for ux, uz in SPLITS): return None, 0
    if abs(lat) <= 1.6 - out * 0.06 and out <= 11 + 3 * math.sin(k * 2.3): return 'root', max(3, int(9 - out * 0.55))
    return None, 0


def stump(G=72, rotted=False):
    P = PAL[rotted]; c = {}
    for x in range(-RX - 16, RX + 17):
        for z in range(-RZ - 16, RZ + 17):
            kind, hh = shape(x, z); f = math.hypot(x / RX, z / RZ)
            if kind == 'top':
                b = P['ring'][int(f * 30 + 0.9 * h3(x // 3, 6, z // 3)) % len(P['ring'])]
                if P['patch'] and (h3(x // 7, 7, z // 7) * 2 + h3(x // 3, 8, z // 3)) / 3 < 0.36: b = pick(P['patch'], x, 0, z)
                for y in range(1, TOP): c[(x, G + y, z)] = 'dark_oak_wood[axis=y]'
                c[(x, G + TOP, z)] = b
            elif kind == 'bark':
                for y in range(1, hh + 1):
                    m = P['crown'] if y >= hh - 1 else P['inner'] if f < 0.87 and y > TOP else P['bark']
                    c[(x, G + y, z)] = pick(m, x if abs(x) > abs(z) else x // 2, y // 3, z if abs(z) >= abs(x) else z // 2)
            elif kind == 'ramp':
                for y in range(1, hh + 1): c[(x, G + y, z)] = 'dark_oak_wood[axis=y]'
                c[(x, G + hh, z)] = pick(P['ramp'], x, 0, z)
            elif kind == 'root':
                for y in range(1, hh + 1): c[(x, G + y, z)] = pick(P['root'], x, y // 2, z)
    for ux, uz in SPLITS:                                                          # a gate frame in each split: posts in the bark, a beam across, a lantern under it
        n = math.hypot(ux, uz); ux, uz = ux / n, uz / n; R = 1 / math.hypot(ux / RX, uz / RZ)
        for lat in range(-6, 7):
            for dp in (0, 1):
                x, z = round(ux * (R * 0.9 + dp) - uz * lat), round(uz * (R * 0.9 + dp) + ux * lat)
                for y in ((TOP + 6, TOP + 7) if abs(lat) < 6 else range(TOP + 1, TOP + 9)): c[(x, G + y, z)] = P['beam'] + ('[axis=x]' if abs(uz) > abs(ux) else '[axis=z]') if abs(lat) < 6 else P['post']
        x, z = round(ux * R * 0.9), round(uz * R * 0.9); c[(x, G + TOP + 5, z)] = 'lantern[hanging=true]'
    x0, x1, z0, z1 = SHED                                                          # the shed: back and side walls, open front, a roof with one block of eave
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wall = x == x0 or z in (z0, z1); post = (x in (x0, x1)) and z in (z0, 0, z1)
            for y in range(TOP + 1, TOP + 5):
                if post: c[(x, G + y, z)] = P['post']
                elif wall: c[(x, G + y, z)] = pick(P['wall'], x, y, z)
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2): c[(x, G + TOP + 5 + (x1 + 1 - x) // 4, z)] = P['roof']
    for (x, z, y) in [(x, z, y) for (x, z) in ((x0 + 1, z0 + 1), (x0 + 1, z0 + 2), (x0 + 2, z1 - 1), (x0 + 1, z1 - 1)) for y in (1, 2, 3)]: c[(x, G + TOP + y, z)] = 'barrel[facing=up]' if not rotted else 'hay_block[axis=y]'
    c[(x1 - 1, G + TOP + 4, 0)] = 'lantern[hanging=true]'
    x0, x1, z0, z1 = PILE
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in (1, 2, 3): c[(x, G + TOP + y, z)] = P['pile']
    crown = {}
    for (x, y, z), b in c.items():
        if shape(x, z)[0] == 'bark': crown[(x, z)] = max(crown.get((x, z), 0), y)
    for (x, z), y in crown.items():                                                # dressing on the rim top, out of reach
        f = math.hypot(x / RX, z / RZ); a = math.atan2(z, x)
        if not rotted:
            if 0.9 <= f <= 0.95 and (x + z) % 2 == 0: c[(x, y + 1, z)] = 'spruce_log[axis=y]'; c[(x, y + 2, z)] = 'spruce_log[axis=y]' if h3(x, 11, z) < 0.6 else c.get((x, y + 2, z), 'air')
        else:
            if h3(x // 3, 12, z // 3) < 0.3: c[(x, y + 1, z)] = 'moss_block' if h3(x, 13, z) < 0.5 else 'azalea_leaves[persistent=true]'
    if rotted:                                                                     # stilt huts perched on the rim; bracket fungi on the bark
        for a in (0.9, 2.6, 4.2, 5.5):
            R = 1 / math.hypot(math.cos(a) / RX, math.sin(a) / RZ) * 0.92; cx, cz = round(R * math.cos(a)), round(R * math.sin(a)); base = max(crown.get((cx + i, cz + j), 0) for i in (-2, 0, 2) for j in (-2, 0, 2)) + 3
            for i in range(-3, 4):
                for j in range(-3, 4):
                    m = max(abs(i), abs(j))
                    if m == 2 and abs(i) == abs(j):
                        for y in range(crown.get((cx + i, cz + j), base - 3) + 1, base): c[(cx + i, y, cz + j)] = 'dark_oak_log[axis=y]'
                    c[(cx + i, base, cz + j)] = 'spruce_planks'
                    if m == 2:
                        for y in (1, 2, 3): c[(cx + i, base + y, cz + j)] = 'spruce_planks' if not (y == 2 and (i == 0 or j == 0)) else 'air'
                    for k in range(0, 4):
                        if m == 3 - k: c[(cx + i, base + 4 + k, cz + j)] = 'dark_oak_planks' if h3(cx + i, k, cz + j) < 0.7 else 'moss_block'
            c[(cx, base + 3, cz)] = 'lantern[hanging=true]'
        for i in range(60):
            a = h3(i, 20, 0) * 6.283; R = 1 / math.hypot(math.cos(a) / RX, math.sin(a) / RZ) * 1.05; x, z = round(R * math.cos(a)), round(R * math.sin(a)); y = G + 5 + int(h3(i, 21, 0) * 7)
            if shape(x, z)[0] is None and not any(x * ux + z * uz > 0 and abs(x * uz - z * ux) <= 10 * math.hypot(ux, uz) for ux, uz in SPLITS):
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        if shape(x + dx, z + dz)[0] is None and h3(x + dx, i, z + dz) < 0.75: c[(x + dx, y, z + dz)] = 'brown_mushroom_block'
    return {k: v for k, v in c.items() if v != 'air'}


if __name__ == '__main__':
    HERE = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
    from mapforge.world import World, Cell
    from mapforge.structure import normalize
    from mapforge import render
    G = 72; view = sys.argv[2] if len(sys.argv) > 2 else 'iso'; rot = len(sys.argv) > 3; c = stump(G, rot)
    for x in range(-58, 59):
        for z in range(-52, 53): c.setdefault((x, G, z), ('mud' if h3(x, 3, z) < 0.4 else 'moss_block') if rot else ('podzol' if h3(x, 3, z) < 0.6 else 'moss_block'))
    w = World('piece')
    for p, v in c.items(): w.set(p, Cell(normalize(v)))
    lo, hi = (-58, G, -52), (58, G + 30, 52)
    img = render.render(w, view=view, lo=lo, hi=hi, grid=False, title='Hollowwood: the %s Stump' % ('Drowned' if rot else 'Great')); img.save(sys.argv[1]); print(sys.argv[1], img.size, len(c))
