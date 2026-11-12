"""Small block-building kit for the Kingsfield workshop generators. A Build is a dict of cells; emit() writes a
mapforge plan of `set` operations. Coordinates are x east, y up, z south."""
import json, random


class Build:
    def __init__(self, seed=1):
        self.c = {}
        self.rng = random.Random(seed)

    def set(self, x, y, z, b):
        self.c[(int(x), int(y), int(z))] = b

    def get(self, x, y, z):
        return self.c.get((x, y, z))

    def pick(self, mix):
        """mix: a block name or [(block, weight), ...]"""
        if isinstance(mix, str): return mix
        r = self.rng.random() * sum(w for _, w in mix)
        for b, w in mix:
            r -= w
            if r <= 0: return b
        return mix[-1][0]

    def box(self, x0, y0, z0, x1, y1, z1, mix, hollow=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if hollow and x0 < x < x1 and z0 < z < z1 and y < y1: continue
                    self.set(x, y, z, self.pick(mix))

    def air(self, x0, y0, z0, x1, y1, z1):
        self.box(x0, y0, z0, x1, y1, z1, 'air')

    def gable_x(self, x0, x1, z0, z1, y, stairs, slab, fill=None, over=1):
        """Gable roof with its ridge along x over the walls z0..z1 (top of wall at y-1). Eaves overhang by `over`."""
        za, zb = z0 - over, z1 + over
        i = 0
        while za + i < zb - i:
            for x in range(x0 - over, x1 + over + 1):
                self.set(x, y + i, za + i, f'{stairs}[facing=south,half=bottom,shape=straight]')
                self.set(x, y + i, zb - i, f'{stairs}[facing=north,half=bottom,shape=straight]')
            if fill:
                for x in (x0, x1):
                    for z in range(za + i + 1, zb - i):
                        if z0 <= z <= z1: self.set(x, y + i, z, self.pick(fill))
            i += 1
        if za + i == zb - i:
            for x in range(x0 - over, x1 + over + 1): self.set(x, y + i, za + i, f'{slab}[type=bottom]')

    def gable_z(self, x0, x1, z0, z1, y, stairs, slab, fill=None, over=1):
        xa, xb = x0 - over, x1 + over
        i = 0
        while xa + i < xb - i:
            for z in range(z0 - over, z1 + over + 1):
                self.set(xa + i, y + i, z, f'{stairs}[facing=east,half=bottom,shape=straight]')
                self.set(xb - i, y + i, z, f'{stairs}[facing=west,half=bottom,shape=straight]')
            if fill:
                for z in (z0, z1):
                    for x in range(xa + i + 1, xb - i):
                        if x0 <= x <= x1: self.set(x, y + i, z, self.pick(fill))
            i += 1
        if xa + i == xb - i:
            for z in range(z0 - over, z1 + over + 1): self.set(xa + i, y + i, z, f'{slab}[type=bottom]')

    def pyramid(self, x0, z0, x1, z1, y, stairs, slab):
        """hipped roof over the box x0..x1, z0..z1, first course at y"""
        i = 0
        while x0 + i < x1 - i and z0 + i < z1 - i:
            for x in range(x0 + i, x1 - i + 1):
                self.set(x, y + i, z0 + i, f'{stairs}[facing=south,half=bottom,shape=straight]')
                self.set(x, y + i, z1 - i, f'{stairs}[facing=north,half=bottom,shape=straight]')
            for z in range(z0 + i + 1, z1 - i):
                self.set(x0 + i, y + i, z, f'{stairs}[facing=east,half=bottom,shape=straight]')
                self.set(x1 - i, y + i, z, f'{stairs}[facing=west,half=bottom,shape=straight]')
            i += 1
        for x in range(x0 + i, x1 - i + 1):
            for z in range(z0 + i, z1 - i + 1): self.set(x, y + i, z, f'{slab}[type=bottom]')

    def mirror_x(self, axis2):
        """copy every cell to x -> axis2 - x (axis2 = 2 * mirror plane), turning east/west facings"""
        swap = {'facing=east': 'facing=west', 'facing=west': 'facing=east'}
        for (x, y, z), b in list(self.c.items()):
            for a, r in swap.items():
                if a in b:
                    b = b.replace(a, r); break
            self.c.setdefault((axis2 - x, y, z), b)

    def bounds(self):
        xs, ys, zs = zip(*self.c)
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    def emit(self, path, pid, purpose, pad=((-8, 40, -8), (80, 120, 80))):
        ops = [{'op': 'set', 'pos': list(k), 'block': v} for k, v in sorted(self.c.items())]
        plan = {'format': 'gg-mapforge-plan/1', 'id': pid, 'world': 'kf-workshop', 'author': 'maps-lead',
                'purpose': purpose, 'limits': {'maxChangedBlocks': 150000},
                'locks': [{'id': 'pad', 'mode': 'allow', 'min': list(pad[0]), 'max': list(pad[1]), 'reason': 'workshop pad'}],
                'operations': ops}
        json.dump(plan, open(path, 'w'))
        return len(ops)


# Kingsfield palette mixes
MASON = [('stone_bricks', 70), ('cracked_stone_bricks', 14), ('mossy_stone_bricks', 10), ('andesite', 6)]
FOOT = [('cobblestone', 70), ('mossy_cobblestone', 20), ('andesite', 10)]
DAUB = 'white_terracotta'
FLOOR = [('spruce_planks', 85), ('oak_planks', 15)]
YARD = [('coarse_dirt', 55), ('packed_mud', 25), ('rooted_dirt', 12), ('gravel', 8)]
ROOF, ROOF_SLAB = 'dark_oak_stairs', 'dark_oak_slab'
