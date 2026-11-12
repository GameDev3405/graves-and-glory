"""K2 the ribs: twelve giant ribs standing in the pool round the Eyot, six north and six south of the island, curving in
over it. ribs() returns {(x, y, z): block} in map coordinates (water surface y 62, street y 64).
Run as a script it writes a workshop plan with a stand-in island, pool and bridges for pictures.
Usage: python ribs.py OUT.json"""
import math, random, sys

BED, WATER, STREET = 59, 62, 64
# one quarter (x > 0, z < 0): x, |z| of the foot, rise above the bed, reach inward, foot radius
QUARTER = [(16, 43, 20, 15, 2.0), (28, 40, 22, 16, 2.2), (40, 34, 18, 13, 1.9)]
# broken ribs, by (sign x, sign z, index): fraction of the length that still stands
SNAP = {(-1, -1, 2): 0.66, (1, 1, 2): 0.66, (1, -1, 0): 0.86, (-1, 1, 0): 0.86}
TMAX = math.radians(102)


def noise(x, y, z):
    return math.sin(x * 0.9 + y * 0.55) * math.cos(z * 0.8 - y * 0.4) + 0.5 * math.sin((x + z + y) * 0.37)


def line(sx, sz, x, az, rise, reach):
    """centre line points (t, px, py, pz, tangent axis)"""
    pts, n = [], 90
    for i in range(n + 1):
        t = i / n
        th = t * TMAX
        py = BED - 1 + rise * math.sin(th)
        inward = reach * (1 - math.cos(th))
        px = sx * (x - 2.2 * t * t)                      # the tip leans a little toward the bridge
        pz = sz * (az - inward)
        pts.append((t, px, py, pz, 'y' if th < math.radians(52) else 'z'))
    return pts


def rib(cells, rng, sx, sz, i):
    x, az, rise, reach, r0 = QUARTER[i]
    keep = SNAP.get((sx, sz, i), 1.0)
    pts = line(sx, sz, x, az, rise, reach)
    for t, px, py, pz, ax in pts:
        if t > keep: break
        # a blade, not a tusk: broad across (x), thinner in its own plane, tapering only near the tip
        wx = max(1.6, (r0 + 0.5) * (1 - 2.2 * t)) if t < 0.86 else 1.6 - 5.0 * (t - 0.86)
        rp = r0 * (1 - 0.5 * t) + 0.2 * math.sin(t * 9.0)
        if keep < 1 and t > keep - 0.05: wx, rp = wx * (0.6 + 0.4 * ((round(py) + round(pz)) % 2)), rp * 0.8   # a jagged break
        k = int(max(wx, rp)) + 2
        for dx in range(-k, k + 1):
            for dy in range(-k, k + 1):
                for dz in range(-k, k + 1):
                    cx, cy, cz = round(px) + dx, round(py) + dy, round(pz) + dz
                    d = (abs(cx - px) / wx) ** 3 + (math.hypot(cy - py, cz - pz) / rp) ** 3
                    if d > 1 or cy < BED: continue
                    n = noise(cx, cy, cz)
                    if cy <= WATER: blk = 'mossy_cobblestone' if n > 0.5 else 'diorite'
                    elif cy == WATER + 1 and n > 0.2: blk = 'diorite'
                    elif n > 0.75: blk = 'calcite'
                    elif n < -0.95: blk = 'diorite'
                    else: blk = 'bone_block[axis=%s]' % ax
                    cells[(cx, cy, cz)] = blk
    # rubble at the foot
    fx, fz = sx * x, sz * az
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if math.hypot(dx, dz) < 3.4 and rng.random() < 0.6:
                cells.setdefault((fx + dx, BED, fz + dz), rng.choice(['diorite', 'calcite', 'gravel']))
                if math.hypot(dx, dz) < 2.6 and rng.random() < 0.4:
                    cells.setdefault((fx + dx, BED + 1, fz + dz), rng.choice(['diorite', 'calcite']))
    if keep < 0.8:
        # the fallen end leans against the foot: one end on the bed, the other clear of the water
        for j in range(12):
            cy = BED + 1 + (11 - j) * 0.55
            for dy in (0, 1):
                for w in (-1, 0, 1):
                    cells[(fx - sx * (3 + j), int(cy) + dy, fz + w)] = 'bone_block[axis=x]' if int(cy) + dy > WATER else 'diorite'


def ribs():
    cells, rng = {}, random.Random(12)
    for sx in (-1, 1):
        for sz in (-1, 1):
            for i in range(3):
                rib(cells, rng, sx, sz, i)
    return cells


if __name__ == '__main__':
    from kit import Build, MASON
    b = Build(seed=3)
    X, Z = 84, 62
    for x in range(-X, X + 1):
        for z in range(-Z, Z + 1):
            pool = (x / 72.0) ** 2 + (z / 50.0) ** 2 <= 1 or abs(z + 0.5) <= 9
            isle = (x / 50.0) ** 2 + (z / 30.0) ** 2 <= 1
            deck = -8 <= x <= 7 and 30 <= abs(z + 0.5) + 0.5 <= 50
            if isle:
                for y in range(BED, STREET + 1): b.set(x, y, z, b.pick(MASON) if y < STREET else b.pick([('cobblestone', 60), ('stone', 20), ('andesite', 20)]))
            elif pool:
                b.set(x, BED - 1, z, 'stone'); b.set(x, BED, z, b.pick([('gravel', 50), ('stone', 30), ('mud', 20)]))
                for y in range(BED + 1, WATER + 1): b.set(x, y, z, 'water')
                b.set(x, WATER + 1, z, 'air'); b.set(x, STREET, z, 'air')
                if deck:
                    b.set(x, STREET, z, b.pick(MASON)); b.set(x, STREET - 1, z, 'stone_brick_slab[type=top]' if abs(z) % 6 else b.pick(MASON))
                    if abs(z) % 6 == 0:
                        for y in range(BED, STREET - 1): b.set(x, y, z, b.pick(MASON))
                    if x in (-8, 7): b.set(x, STREET + 1, z, 'stone_brick_wall')
            else:
                for y in range(BED - 1, STREET): b.set(x, y, z, b.pick(MASON))
                b.set(x, STREET, z, 'cobblestone' if abs(x) < 60 else 'grass_block')
    for k, v in ribs().items(): b.set(*k, v)
    # two soldiers' worth of scale: a 2-high post on the island rim and on the bridge
    for p in ((0, 40), (12, -26)): b.box(p[0], 65, p[1], p[0], 66, p[1], 'red_wool')
    n = b.emit(sys.argv[1], 'rg-k2-ribs', 'K2 the ribs, workshop build', pad=((-X, BED - 1, -Z), (X, 100, Z)))
    print(n, 'ops', b.bounds())
    for i, x0 in enumerate((-84, -28, 28)):
        for j, z0 in enumerate((-62, 0)):
            print('k2_ribs_%d%d %d %d %d %d %d %d' % (i, j, x0, BED - 1, z0, x0 + (55 if i < 2 else 56), 82, z0 + (61 if j == 0 else 62)))
