"""Surface check for surface-only maps: finds the cells where a hollow column would show from outside.

A column's ground top is its highest block that is not air, water or lava. For each of its four neighbours with a
lower ground top, every cell of the column between the neighbour's top and its own top must hold a block (water
counts: it is seen, not seen through to the void). An air cell there is a gap: the side of the column shows a hole
under the cap. Neighbours with no ground at all (outside the built area) are ignored."""
from .terrain import NOT_GROUND
from .structure import block_name


def ground_tops(world, lo=None, hi=None):
    """{(x, z): highest ground y} over the world, or over the [x, z] box lo..hi."""
    tops = {}
    for (x, y, z), cell in world.blocks.items():
        if lo and not (lo[0] <= x <= hi[0] and lo[1] <= z <= hi[1]):
            continue
        if block_name(cell.state) in NOT_GROUND:
            continue
        if tops.get((x, z), -10 ** 6) < y:
            tops[(x, z)] = y
    return tops


TERRAIN = ('grass_block', 'dirt', 'coarse_dirt', 'podzol', 'mud', 'gravel', 'sand', 'stone', 'andesite', 'granite', 'diorite',
           'deepslate', 'tuff', 'dirt_path', 'snow_block', 'moss_block')


def find_gaps(world, lo=None, hi=None, terrain_only=False):
    """Sorted list of (x, y, z) gap cells in the [x, z] box lo..hi (default the whole world). terrain_only keeps the columns
    whose top block is natural ground: openings under a built top (gate arches, doors, trebuchet frames) are meant."""
    tops = ground_tops(world, (lo[0] - 1, lo[1] - 1) if lo else None, (hi[0] + 1, hi[1] + 1) if hi else None)
    blocks, gaps = world.blocks, []
    for (x, z), t in tops.items():
        if lo and not (lo[0] <= x <= hi[0] and lo[1] <= z <= hi[1]):
            continue
        low = min((n for n in (tops.get((x + 1, z)), tops.get((x - 1, z)), tops.get((x, z + 1)), tops.get((x, z - 1)))
                   if n is not None), default=t)
        if terrain_only and block_name(blocks[(x, t, z)].state).split(':')[-1] not in TERRAIN:
            continue
        for y in range(low + 1, t):
            if (x, y, z) not in blocks:
                gaps.append((x, y, z))
    return sorted(gaps)
