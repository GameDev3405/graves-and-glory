"""Ironhold outer curtain in map coordinates: a crooked wall along OUTER, bend towers, the mine gate (north flank)
and the breach (south flank). wall(b, ...) and the towers are reused by the map generator.
Usage: python curtain.py OUT.json north|breach [flip]"""
import sys, os, math
sys.path.append(os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'kingsfield', 'gen')))
from kit import Build
from gate import DEEP, FOOT, ROAD, GLACIS, WARD

OUTER = [(-84, -138), (-78, -96), (-66, -60), (-72, -30), (-72, 30), (-64, 52), (-76, 92), (-84, 132)]
WALK, TOP = 80, 88
RUBBLE = [('cobbled_deepslate', 40), ('cracked_deepslate_bricks', 25), ('gravel', 20), ('deepslate_bricks', 15)]
MINE = (-78, -96)
BREACH_T = None


def near(line, x, z):
    """(signed distance, east positive; distance along the line) of the nearest point of a polyline that runs to +z."""
    best = None; run = 0.0
    for (ax, az), (bx, bz) in zip(line, line[1:]):
        dx, dz = bx - ax, bz - az; L = math.hypot(dx, dz)
        u = max(0.0, min(1.0, ((x - ax) * dx + (z - az) * dz) / (L * L)))
        px, pz = ax + u * dx, az + u * dz; d = math.hypot(x - px, z - pz)
        if best is None or d < best[0] - 1e-9:
            side = -(dx * (z - az) - dz * (x - ax))
            best = (d, d if side >= 0 else -d, run + u * L)
        run += L
    return best[1], best[2]


def along(line, x, z): return near(line, x, z)[1]


def noise(x, z, n): return ((x * 73856093) ^ (z * 19349663) ^ 0x5bd1e995) % n


def ground(line, x, z, lanes=()):
    sd, _ = near(line, x, z)
    for (lz, half, shift) in lanes:
        if abs(z - lz) <= half: sd += shift
    return 64 + max(0, min(6, int((sd + 16) // 2)))


def wall(b, line, x0, z0, x1, z1, g, breach=None):
    """The curtain inside a box. g(x, z) is the ground height. breach = distance along the line of a collapse, or None."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            sd, t = near(line, x, z); gy = g(x, z); ti = int(t)
            top = WALK; broken = False
            if breach is not None and abs(t - breach) <= 14:
                d = abs(t - breach)                                                    # jagged: whole courses torn off in steps, loose teeth on top
                top = gy if d <= 3.5 else min(WALK, gy + int((d - 3.5) * 1.5) + (4 if noise(ti, 5, 3) == 0 else 0) + noise(x, z, 4) + (2 if abs(sd) < 1.5 else 0))
                broken = top < WALK
            if abs(sd) <= 3.5:
                face = abs(sd) > 2.5
                for y in range(gy + 1, top + 1):
                    b.set(x, y, z, b.pick(FOOT) if y <= gy + 2 else 'polished_deepslate' if y == WALK - 3 and face else
                          'cracked_deepslate_bricks' if broken and y >= top - 1 else b.pick(DEEP))
                if broken:
                    if top == gy: b.set(x, gy, z, b.pick(RUBBLE))
                    continue
                if sd <= -2.3:
                    b.set(x, WALK + 1, z, b.pick(DEEP))
                    if (ti // 2) % 2 == 0: b.set(x, WALK + 2, z, b.pick(DEEP))
                    if ti % 8 == 4 and sd < -2.5:
                        for y in (WALK - 6, WALK - 5): b.set(x, y, z, 'iron_bars')
                elif sd >= 2.5: b.set(x, WALK + 1, z, 'deepslate_brick_wall')
            elif -4.5 <= sd < -3.5 and not broken:
                butt = ti % 8 == 0
                for y in range(gy + 1, (WALK - 4 if butt else gy + 2) + 1): b.set(x, y, z, b.pick(FOOT) if y <= gy + 3 else b.pick(DEEP))
                if butt: b.set(x, WALK - 3, z, 'polished_deepslate')
            elif breach is not None and abs(t - breach) <= 13 and abs(sd) <= 12:
                d = abs(t - breach)                                                    # the rubble heap, highest against the torn ends; the way through kept clear
                if d > 4.2:
                    h = int(max(0.0, 3.6 - (abs(sd) - 3.5) * 0.45 - max(0.0, d - 9) * 0.6) * (0.55 + noise(x, z, 10) / 10.0))
                    for y in range(gy + 1, gy + 1 + min(h, int(d - 3.2))): b.set(x, y, z, b.pick(RUBBLE))
                elif noise(x, z, 2) == 0: b.set(x, gy, z, b.pick(RUBBLE))


def point_at(line, t):
    for (ax, az), (bx, bz) in zip(line, line[1:]):
        L = math.hypot(bx - ax, bz - az)
        if t <= L: return ax + (bx - ax) * t / L, az + (bz - az) * t / L
        t -= L
    return line[-1]


def breach_extras(b, line, g, t0):
    """A shattered tower stump south of the gap, fallen beams, and lumps of wall thrown out onto the glacis."""
    cx, cz = point_at(line, t0 + 19); cx, cz = int(round(cx)), int(round(cz)); r = 5
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            edge = x in (cx - r, cx + r) or z in (cz - r, cz + r); gy = g(x, z)
            if x in (cx - r, cx + r) and z in (cz - r, cz + r): continue
            north = max(0, cz - z + 2)                                                # torn lowest on the side of the gap
            top = max(gy + 2, WALK + 3 - north * 2 - noise(x, z, 4)) if edge else gy + 1 + noise(x, z, 3)
            for y in range(gy + 1, top + 1):
                b.set(x, y, z, b.pick(FOOT) if y <= gy + 3 and edge else 'cracked_deepslate_bricks' if y >= top - 1 else b.pick(DEEP) if edge else b.pick(RUBBLE))
            if edge:
                for y in range(top + 1, WALK + 6): b.set(x, y, z, 'air')
    for y in (WALK - 6, WALK - 5): b.set(cx - r, y, cz + 2, 'iron_bars')
    px, pz = point_at(line, t0); px, pz = int(round(px)), int(round(pz))
    for (dx, dz, n, ax, rise) in ((-9, -8, 5, 'x', 0), (-7, 7, 4, 'z', 0), (5, -7, 5, 'x', 1), (6, 8, 4, 'x', 0), (-12, 6, 3, 'x', 0)):   # beams
        for i in range(n):
            x, z = (px + dx + i, pz + dz) if ax == 'x' else (px + dx, pz + dz + i)
            y = g(x, z) + 1 + (i * rise + 1) // 2
            if b.get(x, y, z) in (None, 'air') and abs(near(line, x, z)[0]) > 4: b.set(x, y, z, 'dark_oak_log[axis=%s]' % ax)
    for (dx, dz) in ((-11, -11), (-13, 11), (-8, 12)):                                     # lumps of fallen wall, still coursed
        for i in range(3):
            for j in range(2):
                x, z = px + dx + j, pz + dz + i; gy = g(x, z)
                for y in range(gy + 1, gy + 3 - (i == 2)): b.set(x, y, z, 'deepslate_bricks' if (i + j) % 2 else 'cracked_deepslate_bricks')
    for (dx, dz) in ((-6, -6), (7, 6)):                                                  # a fallen standard and a dropped brazier
        b.set(px + dx, g(px + dx, pz + dz) + 1, pz + dz, 'polished_blackstone_brick_wall')


def tower(b, line, cx, cz, g, r=6, flag='purple', tunnel=False):
    S = lambda f, top=False: 'deepslate_brick_stairs[facing=%s,half=%s,shape=straight]' % (f, 'top' if top else 'bottom')
    xa, xb, za, zb = cx - r, cx + r, cz - r, cz + r
    for x in range(xa, xb + 1):
        for z in range(za, zb + 1):
            edge = x in (xa, xb) or z in (za, zb); corner = x in (xa, xb) and z in (za, zb); gy = g(x, z)
            for y in range(gy + 1, TOP + 1):
                if corner and y > gy + 3: b.set(x, y, z, 'air'); continue
                if edge: b.set(x, y, z, b.pick(FOOT) if y <= gy + 3 else 'polished_deepslate' if y in (WALK - 3, TOP - 1) else b.pick(DEEP))
                elif y in (WALK, TOP): b.set(x, y, z, 'dark_oak_planks' if y == WALK else b.pick(DEEP))
                elif y <= 70: b.set(x, y, z, b.pick(DEEP))
                else: b.set(x, y, z, 'air')
            for y in range(TOP + 1, TOP + 4): b.set(x, y, z, 'air')
    for x in range(xa - 1, xb + 2):
        for z in range(za - 1, zb + 2):
            if not (x in (xa - 1, xb + 1) or z in (za - 1, zb + 1)): continue
            corner = x in (xa - 1, xb + 1) and z in (za - 1, zb + 1); sd, _ = near(line, x, z)
            if abs(sd) <= 3.5 and abs(z - cz) > r: continue                              # where the curtain walk meets the tower
            f = 'east' if x == xa - 1 else 'west' if x == xb + 1 else 'south' if z == za - 1 else 'north'
            b.set(x, TOP, z, 'polished_deepslate' if corner else S(f, True)); b.set(x, TOP + 1, z, b.pick(DEEP))
            if (x + z) % 2 == 0 or corner: b.set(x, TOP + 2, z, b.pick(DEEP))
            if corner: b.set(x, TOP + 3, z, 'deepslate_brick_wall'); b.set(x, TOP + 4, z, 'lightning_rod[facing=up]')
    for y in (78, 79, 84, 85):
        b.set(xa, y, cz, 'iron_bars'); b.set(xa, y, cz - 3, 'iron_bars') if y > 80 else None; b.set(xa, y, cz + 3, 'iron_bars') if y > 80 else None
    for z in (za, zb):                                                                   # doors onto the curtain walk
        for x in range(xa + 1, xb):
            sd, _ = near(line, x, z + (1 if z == zb else -1))
            if abs(sd) <= 0.8:
                for y in (WALK + 1, WALK + 2): b.set(x, y, z, 'air')
    if not tunnel:
        b.set(xb, 71, cz, 'air'); b.set(xb, 72, cz, 'air'); b.set(xb, 73, cz, S('west', True))    # door from the ward
    for y in range(71, TOP + 1): b.set(xa + 1, y, cz + 2, 'ladder[facing=east]')
    b.set(cx, TOP - 3, cz, 'lantern[hanging=true]'); b.set(cx, TOP - 2, cz, 'chain[axis=y]'); b.set(cx, TOP - 1, cz, 'chain[axis=y]')
    for y in range(TOP + 1, TOP + 9): b.set(cx, y, cz, 'dark_oak_fence')
    b.set(cx, TOP + 1, cz, 'polished_blackstone')
    for i in range(1, 5):
        for y in (TOP + 6, TOP + 7, TOP + 8):
            if not (i == 4 and y == TOP + 7): b.set(cx + i, y, cz, 'black_wool' if y == TOP + 7 else flag + '_wool')


def mine_gate(b, line, g, at=MINE, mine=True):
    """A gate tower on the north bend with an ore-cart tunnel through it: 5 wide, 5 high, floor at 70."""
    cx, cz = at; r = 7
    tower(b, line, cx, cz, g, r=r, tunnel=True)
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - 2, cz + 3):
            if cx - r <= x <= cx + r:
                b.set(x, 70, z, b.pick(ROAD))
                for y in range(71, 76): b.set(x, y, z, 'air')
                b.set(x, 76, z, b.pick(DEEP))
        if cx - r <= x <= cx + r:
            for z in (cz - 3, cz + 3):                                                   # tunnel side walls inside the hollow tower
                for y in range(71, 77): b.set(x, y, z, b.pick(DEEP))
            b.set(x, 75, cz - 2, 'deepslate_brick_stairs[facing=south,half=top,shape=straight]')
            b.set(x, 75, cz + 2, 'deepslate_brick_stairs[facing=north,half=top,shape=straight]')
            if (x - cx) % 4 == 0:                                                        # pit props
                for z in (cz - 3, cz + 3):                                               # set into the side walls, so the way stays 5 wide
                    for y in (71, 72, 73, 74): b.set(x, y, z, 'dark_oak_log[axis=y]')
                b.set(x, 75, cz - 1, 'dark_oak_slab[type=top]'); b.set(x, 75, cz, 'dark_oak_slab[type=top]'); b.set(x, 75, cz + 1, 'dark_oak_slab[type=top]')
            if (x - cx) % 4 == 2: b.set(x, 74, cz, 'lantern[hanging=true]'); b.set(x, 75, cz, 'chain[axis=y]')
    for x in (cx - r, cx + r):                                                           # the two mouths: a dark arch ring and a lifted iron grille
        for z in (cz - 3, cz + 3):
            for y in range(71, 77): b.set(x, y, z, 'polished_blackstone_bricks' if y % 2 else 'chiseled_polished_blackstone')
        for z in range(cz - 2, cz + 3): b.set(x, 76, z, 'polished_blackstone_bricks'); b.set(x, 75, z, 'iron_bars[north=true,south=true]')
    for x in range(cx - r, cx + r + 1):                                                  # the cart rail, laid against the north side of the way
        if mine and b.get(x, 71, cz - 2) in (None, 'air'): b.set(x, 71, cz - 2, 'rail[shape=east_west]')
    for (dx, dz, blk) in () if not mine else ((r + 2, -4, 'raw_iron_block'), (r + 2, -5, 'raw_iron_block'), (r + 3, -4, 'raw_iron_block'), (r + 2, 4, 'barrel[facing=up]'), (r + 3, 4, 'barrel[facing=up]'), (r + 2, 5, 'coal_block')):
        b.set(cx + dx, 71, cz + dz, blk)
    if mine: b.set(cx + r + 2, 72, cz - 4, 'raw_iron_block')
    for z in (cz - 4, cz + 4):                                                           # lamps either side of the field mouth
        gy = g(cx - r - 1, z)
        b.set(cx - r - 1, gy + 1, z, 'polished_blackstone_brick_wall'); b.set(cx - r - 1, gy + 2, z, 'polished_blackstone'); b.set(cx - r - 1, gy + 3, z, 'lantern[hanging=false]')


def site(b, line, x0, z0, x1, z1, g, lane=None):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            gy = g(x, z); sd, _ = near(line, x, z)
            for y in range(60, gy): b.set(x, y, z, 'stone')
            b.set(x, gy, z, b.pick(ROAD) if lane is not None and abs(z - lane) <= 2 else b.pick(GLACIS) if sd < -3.5 else b.pick(WARD))
            for y in range(gy + 1, 72): b.set(x, y, z, 'air')


if __name__ == '__main__':
    piece = sys.argv[2]; b = Build(seed=93)
    if piece == 'north':
        x0, z0, x1, z1 = -100, -112, -54, -42
        lanes = ((MINE[1], 3, 5),)
        g = lambda x, z: ground(OUTER, x, z, lanes)
        site(b, OUTER, x0, z0, x1, z1, g, lane=MINE[1]); wall(b, OUTER, x0, z0, x1, z1, g)
        mine_gate(b, OUTER, g); tower(b, OUTER, -66, -60, g)
        boxes = ['ih_cur_n1 -100 60 -112 -54 100 -78', 'ih_cur_n2 -100 60 -77 -54 100 -42']
    else:
        x0, z0, x1, z1 = -98, 50, -52, 96
        g = lambda x, z: ground(OUTER, x, z)
        site(b, OUTER, x0, z0, x1, z1, g); t0 = along(OUTER, -70, 73); wall(b, OUTER, x0, z0, x1, z1, g, breach=t0); breach_extras(b, OUTER, g, t0)
        bad = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) if abs(near(OUTER, x, z)[1] - t0) <= 3.5 and abs(near(OUTER, x, z)[0]) <= 14
               and any(b.get(x, y, z) not in (None, 'air') for y in range(g(x, z) + 1, g(x, z) + 4))]
        assert not bad, bad
        boxes = ['ih_breach -98 60 50 -52 100 96']
    if len(sys.argv) > 3:
        sw = lambda v: v.replace('facing=east', '#').replace('facing=west', 'facing=east').replace('#', 'facing=west')
        b.c = {(-152 - x, y, z): sw(v) for (x, y, z), v in b.c.items()}
        x0, x1 = -152 - x1, -152 - x0
    print(b.emit(sys.argv[1], 'ih-curtain-' + piece, 'outer curtain, workshop build', pad=((x0 - 2, 60, z0 - 2), (x1 + 2, 110, z1 + 2))), 'ops')
    if len(sys.argv) == 3:
        for l in boxes: print(l)
