"""Frostmarch: the Spine. A giant's backbone lying face down along x = 0 from z -93 to 93, cut by the road at the Cut
(|z| < 18). spine(G) returns {(x, y, z): block} in map coordinates with the ground block at y G. No faces.
Each vertebra is drawn as the real bone: a spool-shaped body with end plates, a darker disc between bodies, the arch
over the cord (an open canal you can see into at the broken ends), a spinous process leaning tailward on top, a
side process out to each side with a knob, and small joints that lap over the next vertebra. The outer ones carry
broken rib stumps. A wall troops cannot climb: the body is widest above the ground, and everything else is overhead.
One disc is missing on each side (|z| 46..49): the narrow Spine gaps.
  python spine.py OUT.png [turn] [iso|top]     an offline picture (turn = quarter turns of the view)"""
import math, sys, os

PERIOD, BODY = 16, 12                                      # a vertebra 12 long, a disc 4 long
START, END = 18, 86
GAP = (46, 49)                                             # the missing disc on each side: |z| 46..49
R = 6.0                                                    # body radius; its axis is 5 above the ground
DISC = [('dead_brain_coral_block', 5), ('dead_bubble_coral_block', 3), ('andesite', 2)]
SCREE = [('stone', 4), ('gravel', 3), ('andesite', 2), ('snow_block', 3)]


def noise(x, y, z): return math.sin(x * 0.9 + y * 0.55) * math.cos(z * 0.8 - y * 0.4) + 0.5 * math.sin((x + z + y) * 0.37)


def h3(x, y, z, n): return ((x * 73856093) ^ (y * 83492791) ^ (z * 19349663) ^ 0x2f1d) % n


def mix(m, x, y, z):
    r = h3(x, y, z, sum(w for _, w in m))
    for b, w in m:
        r -= w
        if r < 0: return b
    return m[-1][0]


def bone(x, y, z, ax='z'):
    n = noise(x * 0.3, y * 0.3, z * 0.3)                                            # broad weathering, not speckle
    return 'calcite' if n > 1.05 else 'smooth_quartz' if n < -1.1 else 'bone_block[axis=%s]' % ax


def vertebra(c, G, z0, i, snapped=False, joint=True, ribs=False):
    """one vertebra, body from z0 to z0 + 11 (north to south); snapped = its north end was hacked off for the road"""
    cy = G + 5; s = round(1.3 * math.sin(i * 0.9))                                  # s: the column wanders a little
    def put(x, y, z, b): c[(x + s, y, z)] = b
    for dz in range(BODY):
        z = z0 + dz
        rr = R + 0.7 if dz in (0, BODY - 1) else R if dz in (1, BODY - 2) else R - 0.9   # a spool: flared end plates, a waist
        for x in range(-8, 9):
            for y in range(G + 1, G + 13):
                d = math.hypot(x / rr, (y - cy) / rr)
                if d > 1: continue
                if snapped and dz < 4 and y > G + 5 and noise((x + s) * 1.3, y * 1.1, z * 1.9) > -0.75 + 0.45 * dz: continue      # a jagged break
                plate = dz in (0, BODY - 1)
                put(x, y, z, ('smooth_quartz' if d > 0.72 else 'bone_block[axis=z]') if plate else bone(x + s, y, z, 'z'))
    a0 = 4 if snapped else 2
    for dz in range(a0, 10):                                                        # the arch over the cord: two legs and a roof, the canal open between
        z = z0 + dz
        for sx in (-1, 1):
            for x in (3, 4):
                for y in range(G + 10, G + 16): put(sx * x, y, z, bone(sx * x + s, y, z, 'y'))
        for x in range(-4, 5):
            put(x, G + 15, z, bone(x + s, G + 15, z, 'x'))
            if abs(x) <= 3: put(x, G + 16, z, bone(x + s, G + 16, z, 'x'))
        for x in range(-2, 3):
            for y in range(G + 12, G + 15): put(x, y, z, 'air')
    for h in range(0, 10):                                                          # the spinous process: a blade on the midline, leaning tailward (south)
        y = G + 17 + h; lean = int(h * 0.6); ln = 5 - h // 3
        for dz in range(4, 4 + ln):
            for x in ((-1, 0, 1) if h < 4 else (0,)):
                put(x, y, z0 + dz + lean, bone(x + s, y, z0 + dz + lean, 'y'))
    for k in (0, 1):                                                                # its knobbed tip
        for x in (-1, 0, 1): put(x, G + 26, z0 + 9 + k, bone(x + s, G + 26, z0 + 9 + k, 'z'))
    for sx in (-1, 1):                                                              # the side processes, out and a little up, with an end knob
        for k in range(0, 7):
            x = sx * (5 + k); y = G + 13 + k // 3
            for dz in (5, 6):
                for yy in (y, y + 1): put(x, yy, z0 + dz, bone(x + s, yy, z0 + dz, 'x'))
        for x in (11, 12):
            for dz in (4, 5, 6, 7):
                for yy in (G + 14, G + 15, G + 16): put(sx * x, yy, z0 + dz, bone(sx * x + s, yy, z0 + dz, 'x'))
        if ribs:                                                                    # a broken rib stump, curving out and down, ending well overhead
            n = 6 + min(i, 3) * 2                                                   # longer towards the middle of the back
            for k in range(1, n + 1):
                x = sx * (12 + k); y = G + 15 - int(k * k * 0.07); back = k // 4       # sweeping out, down and tailward
                for dz in (5 + back, 6 + back):
                    for yy in (y, y + 1): put(x, yy, z0 + dz, bone(x + s, yy, z0 + dz, 'x'))
        if joint:                                                                   # the joints that lap over the next vertebra
            for dz in (10, 11, 12, 13):
                for yy in (G + 16, G + 17): put(sx * 3, yy, z0 + dz, bone(sx * 3 + s, yy, z0 + dz, 'z'))
            put(sx * 3, G + 18, z0 + 13, bone(sx * 3 + s, G + 18, z0 + 13, 'y'))


def disc(c, G, z0, i):
    cy = G + 5; s = round(1.3 * math.sin((i + 0.5) * 0.9))
    for z in range(z0, z0 + PERIOD - BODY):
        for x in range(-6, 7):
            for y in range(G + 1, G + 11):
                if math.hypot(x / 4.6, (y - cy) / 4.6) <= 1: c[(x + s, y, z)] = mix(DISC, x, y, z)


def half(G):
    """the south half (z > 0)"""
    c = {}; i = 0; z = START
    while z <= END:
        nxt = z + BODY                                                              # where the next disc would start
        gap = nxt == GAP[0]; last = nxt > END
        vertebra(c, G, z, i, snapped=(i == 0), joint=not gap and not last, ribs=i >= 1)
        if not gap and not last: disc(c, G, nxt, i)
        z += PERIOD; i += 1
    for (x, zz, h) in ((-10, 21, 2), (11, 24, 1), (-13, 27, 1), (9, 19, 2), (14, 30, 1), (-16, 23, 1)):     # fragments the road builders threw aside: low cover
        for y in range(G + 1, G + 1 + h): c[(x, y, zz)] = bone(x, y, zz, 'y'); c[(x + 1, y, zz)] = bone(x + 1, y, zz, 'x')
    tops = {}
    for (x, y, zz), v in c.items():
        if v != 'air': tops[(x, zz)] = max(tops.get((x, zz), 0), y)
    for (x, zz), y in tops.items():
        if y >= G + 14 and noise(x * 0.5, 3, zz * 0.5) > 0.55: c[(x, y + 1, zz)] = 'snow_block'    # a little snow on the high faces only: the bone must read from above
    for zz in range(START - 2, END + 8):                                            # wind-scoured ground along the bone, so it stands out of the snow
        for x in range(-17, 18):
            if abs(x) + h3(x, 3, zz, 5) <= 16: c.setdefault((x, G, zz), mix(SCREE, x, 0, zz) if abs(x) > 8 else mix(SCREE[:3], x, 0, zz))
    return c


def spine(G=72):
    c = half(G)
    c.update({(-x, y, -z): v for (x, y, z), v in list(c.items())})
    return c


if __name__ == '__main__':
    HERE = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
    from mapforge.world import World, Cell
    from mapforge.structure import normalize
    from mapforge import render
    G = 72; c = spine(G); turns = int(sys.argv[2]) if len(sys.argv) > 2 else 0; view = sys.argv[3] if len(sys.argv) > 3 else 'iso'
    for x in range(-30, 31):
        for z in range(-100, 101): c.setdefault((x, G, z), 'snow_block' if abs(z) > 5 else 'gravel')
    for _ in range(turns): c = {(-z, y, x): v.replace('axis=x', 'axis=#').replace('axis=z', 'axis=x').replace('axis=#', 'axis=z') for (x, y, z), v in c.items()}
    w = World('spine')
    for p, v in c.items():
        if v != 'air': w.set(p, Cell(normalize(v)))
    xs = [p[0] for p in c]; zs = [p[2] for p in c]
    lo, hi = (min(xs), G, min(zs)), (max(xs), G + 30, max(zs))
    img = render.render(w, view=view, lo=lo, hi=hi, grid=False, title='Frostmarch: the Spine'); img.save(sys.argv[1]); print(sys.argv[1], img.size, len(c))
