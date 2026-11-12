"""K5 the Hand: writes a mapforge plan for the workshop pad (pit floor at y 56).
Usage: python hand.py OUT.json    Hand centre (0,0), wrist south, fingers north, 44 x 30, at most 2 high."""
import json, math, random, sys
FLOOR = 56
CURL = len(sys.argv) > 2 and sys.argv[2] == 'curled'   # fingers bend up off the floor, tips 4 high
rng = random.Random(5)

def cap(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az
    t = max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz)))
    return math.hypot(px - ax - t * dx, pz - az - t * dz), t

FINGERS = [((-6.6, -6), (-9.6, -19.5), 1.9), ((-2.2, -7), (-3.0, -22), 1.9),
           ((2.2, -7), (3.6, -21.0), 1.9), ((6.5, -6), (10.0, -16.5), 1.7)]
BROKEN = 3   # the little finger is snapped past its first joint; the tip lies apart
THUMB = ((-8.0, 7.0), (-19.0, -7.0), 2.5)

def palm(x, z):
    # rounded slab x -9..9, z -7..12, narrowing to the wrist
    half = 9.6 - max(0.0, (z - 3) * 0.33) - max(0.0, (-3 - z) * 0.12)
    if z < -7.5 or z > 13.5 or abs(x - 0.2 * max(0, z - 3) * 0) > half: return False
    cx, cz = max(abs(x) - (half - 3.5), 0), max(-4 - z, 0)
    return math.hypot(cx, cz) <= 3.6

def cell(x, z):
    """returns (height above floor, block) or None. height 0 = flush inlay."""
    best = None
    for i, (a, b, r) in enumerate(FINGERS + [THUMB]):
        if CURL:
            k = 0.74 if i < 4 else 0.82
            b = (a[0] + k * (b[0] - a[0]), a[1] + k * (b[1] - a[1]))
        d, t = cap(x, z, *a, *b)
        if i == BROKEN and 0.58 < t < 0.74: continue
        rr = r * (1.0 - 0.18 * t)
        if d <= rr:
            thumb = (a, b, r) == THUMB
            # joints: two knuckle bands rise to 2
            joint = any(abs(t - k) < 0.07 for k in ((0.45, 0.78) if not thumb else (0.6,)))
            h = 2 if (joint and d < rr - 0.6) else 1
            if t > 0.9: h = 0.5                      # fingertip: a slab
            if CURL:
                h = 1 + int((3.4 if not thumb else 2.4) * t ** 1.5)
                if i == BROKEN and t >= 0.74: h = 1
            blk = None
            if any(abs(t - k - 0.09) < 0.035 for k in (0.45, 0.78)) and not thumb: h, blk = 1, 'andesite'
            if best is None or h > best[0]: best = (h, blk)
    if best: return best
    if palm(x, z):
        # heel of the hand and thumb mound stand 1 high; the hollow of the palm is flush with the floor
        if z >= 9.5 or (x <= -5.5 and z >= 0) or (z <= -5.5): return (1, None)
        crease = abs((z - 1.5) - 0.35 * x) < 0.5 or abs((z + 2.5) + 0.2 * x) < 0.5 or abs(x + 2 - 0.5 * (z - 2)) < 0.5 and z > 2
        return (0, 'andesite' if crease else None)
    wh = 6.2 - 0.9 * math.sin((z - 13.5) / 8.5 * math.pi)   # wrist narrows then flares into the forearm
    if abs(x) <= wh and 13.5 < z <= 22:        # wrist, sinking under scree at the south end
        if z > 18.5 + 2.5 * abs(math.sin(x * 1.3)) : return ('scree', None)
        return (2 if abs(x) < wh - 1.5 else 1, None)
    return None

def skin(x=0, z=0):
    n = math.sin(x * 0.55 + 1.3) * math.cos(z * 0.47 - 0.4) + 0.5 * math.sin((x + z) * 0.31)
    return 'diorite' if n > 0.55 or (n > 0.3 and rng.random() < 0.3) else 'calcite'

ops = []
for z in range(-26, 27):
    for x in range(-22, 16):
        c = cell(x + 0.0, z + 0.0)
        if c is None: continue
        h, blk = c
        if h == 'scree':
            ops.append({'op': 'set', 'pos': [x, FLOOR + 1, z], 'block': rng.choice(['gravel', 'cobblestone', 'andesite'])})
            continue
        if h == 0:
            ops.append({'op': 'set', 'pos': [x, FLOOR, z], 'block': blk or skin(x, z)})
        elif h == 0.5:
            ops.append({'op': 'set', 'pos': [x, FLOOR, z], 'block': 'diorite'})
            ops.append({'op': 'set', 'pos': [x, FLOOR + 1, z], 'block': 'diorite_slab[type=bottom]'})
        else:
            ops.append({'op': 'set', 'pos': [x, FLOOR, z], 'block': 'diorite'})
            for y in range(1, int(h) + 1):
                ops.append({'op': 'set', 'pos': [x, FLOOR + y, z], 'block': (blk if y == h and blk else skin(x, z))})
plan = {'format': 'gg-mapforge-plan/1', 'id': 'kf-k5-hand' + ('-curled' if CURL else ''), 'world': 'kf-workshop', 'author': 'maps-lead',
        'purpose': 'K5 the Hand, workshop build', 'limits': {'maxChangedBlocks': 20000},
        'locks': [{'id': 'pad', 'mode': 'allow', 'min': [-32, 50, -32], 'max': [31, 80, 31], 'reason': 'workshop pad'}],
        'operations': ops}
json.dump(plan, open(sys.argv[1], 'w'))
print(len(ops), 'ops')
