"""Nine hand-shaped trees. Each is a dict {(dx, dy, dz): block} with the trunk foot at (0, 1, 0) (dy 1 = the first
block above the ground). TREES[name]; turned(tree, k) gives one of four quarter turns.
Usage (sampler): python trees.py OUT.json"""
import math

L = '[persistent=true]'


def _h(x, y, z, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (s * 2654435761)
    n = (n ^ (n >> 13)) * 1274126177 & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


class T:
    def __init__(self, seed): self.c, self.seed = {}, seed

    def log(self, x, y, z, kind='oak', axis='y'): self.c[(x, y, z)] = '%s_log[axis=%s]' % (kind, axis)

    def trunk(self, x, z, y0, y1, kind='oak'):
        for y in range(y0, y1 + 1): self.log(x, y, z, kind)

    def blob(self, cx, cy, cz, rx, ry, rz, leaf='oak_leaves', rough=0.28, alt=None, alt_p=0.0):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1 - rough + 2 * rough * _h(x, y, z, self.seed) and (x, y, z) not in self.c:
                        self.c[(x, y, z)] = (alt if alt and _h(x, y, z, self.seed + 5) < alt_p else leaf) + L

    def disc(self, y, r, leaf, cx=0, cz=0):
        for x in range(-4, 5):
            for z in range(-4, 5):
                d = math.hypot(x, z)
                if d <= r + 0.3 and (cx + x, y, cz + z) not in self.c:
                    if d > r - 0.5 and r >= 2 and _h(x, y, z, self.seed) < 0.07: continue
                    self.c[(cx + x, y, cz + z)] = leaf + L


def oak_small():
    t = T(1); t.trunk(0, 0, 1, 4)
    t.blob(0, 5.2, 0, 2.5, 1.9, 2.5); t.blob(1, 4, -1, 1.5, 1.1, 1.5)
    return t.c


def oak_medium():
    t = T(2); t.trunk(0, 0, 1, 4); t.trunk(1, 0, 4, 7)
    t.log(-1, 4, 0, axis='x'); t.log(-2, 5, 0, axis='x')
    t.blob(1, 8, 0, 3.3, 2.2, 3.1); t.blob(-2.5, 6, 0.5, 2.1, 1.5, 2.1); t.blob(2, 6, 2, 1.8, 1.3, 1.8)
    return t.c


def oak_great():
    t = T(3); t.trunk(0, 0, 1, 9)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)): t.c[(dx, 1, dz)] = 'oak_wood[axis=y]'      # root flare
    t.c[(1, 2, 0)] = 'oak_wood[axis=y]'; t.c[(0, 2, -1)] = 'oak_wood[axis=y]'
    for i in (1, 2, 3): t.log(i, 5 + (i > 1), 0, axis='x')
    for i in (1, 2, 3): t.log(-i, 6 + (i > 2), 0, axis='x')
    for i in (1, 2): t.log(0, 7, -i, axis='z')
    for i in (1, 2): t.log(0, 6, i, axis='z')
    t.blob(3.5, 7.5, 0, 2.8, 2.0, 2.8); t.blob(-3.5, 8.5, 0, 2.8, 2.0, 2.8); t.blob(0, 8.5, -3, 2.6, 1.9, 2.6)
    t.blob(0, 7.5, 3, 2.6, 1.8, 2.6); t.blob(0, 11, 0, 3.8, 2.4, 3.8)
    return t.c


def birch_tall():
    t = T(4); t.trunk(0, 0, 1, 9, 'birch')
    t.blob(0, 8, 0, 2.1, 2.9, 2.1, 'birch_leaves'); t.blob(0, 11, 0, 1.4, 1.6, 1.4, 'birch_leaves'); t.blob(1, 6, 0, 1.3, 1.0, 1.3, 'birch_leaves')
    return t.c


def birch_pair():
    t = T(5); t.trunk(0, 0, 1, 6, 'birch'); t.trunk(2, 1, 1, 3, 'birch'); t.trunk(3, 1, 3, 5, 'birch')
    t.blob(0, 7, 0, 1.9, 2.2, 1.9, 'birch_leaves'); t.blob(3, 6, 1, 1.7, 1.7, 1.7, 'birch_leaves')
    return t.c


def spruce_tall():
    t = T(6); t.trunk(0, 0, 1, 14, 'spruce')                         # skirts: each wide ring sits on a narrower one
    for y, r in ((3, 2.5), (4, 3.5), (5, 3), (6, 2), (7, 3), (8, 2.5), (9, 1.5), (10, 2.5), (11, 2), (12, 1), (13, 1.5), (14, 1), (15, 1), (16, 0), (17, 0)): t.disc(y, r, 'spruce_leaves')
    return t.c


def spruce_small():
    t = T(7); t.trunk(0, 0, 1, 8, 'spruce')
    for y, r in ((2, 2), (3, 2.5), (4, 2), (5, 1), (6, 2), (7, 1.5), (8, 1), (9, 1), (10, 0), (11, 0)): t.disc(y, r, 'spruce_leaves')
    return t.c


def dark_oak():
    t = T(8)
    for x in (0, 1):
        for z in (0, 1): t.trunk(x, z, 1, 5, 'dark_oak')
    t.log(-1, 4, 0, 'dark_oak', 'x'); t.log(2, 5, 1, 'dark_oak', 'x'); t.log(0, 5, 2, 'dark_oak', 'z')
    t.blob(0.5, 6.6, 0.5, 4.4, 1.9, 4.4, 'dark_oak_leaves', rough=0.2); t.blob(0.5, 8.3, 0.5, 2.6, 1.3, 2.6, 'dark_oak_leaves')
    return t.c


def apple():
    t = T(9); t.trunk(0, 0, 1, 3)
    t.log(1, 3, 0, axis='x')
    t.blob(0.3, 4.6, 0, 2.3, 1.6, 2.3, alt='flowering_azalea_leaves', alt_p=0.14)
    return t.c


TREES = {'oak_small': oak_small(), 'oak_medium': oak_medium(), 'oak_great': oak_great(), 'birch_tall': birch_tall(),
         'birch_pair': birch_pair(), 'spruce_tall': spruce_tall(), 'spruce_small': spruce_small(), 'dark_oak': dark_oak(),
         'apple': apple()}


def turned(tree, k):
    """k quarter turns about the trunk"""
    out = {}
    for (x, y, z), b in tree.items():
        for _ in range(k % 4):
            x, z = -z, x
            if 'axis=x' in b: b = b.replace('axis=x', 'axis=z')
            elif 'axis=z' in b: b = b.replace('axis=z', 'axis=x')
        out[(x, y, z)] = b
    return out


if __name__ == '__main__':
    import sys
    from kit import Build
    b = Build(seed=1)
    for i, (n, t) in enumerate(TREES.items()):
        ox, oz = 8 + (i % 5) * 13, 8 + (i // 5) * 14
        for (x, y, z), blk in t.items(): b.set(ox + x, 64 + y, oz + z, blk)
    print(b.emit(sys.argv[1], 'kf-trees', 'Kingsfield tree set', pad=((-4, 40, -4), (72, 120, 34))), 'ops', b.bounds())
