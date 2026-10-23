#!/usr/bin/env python
"""Read-only census of the loose item entities saved in a world: python -I items_census.py <world dir> [half_x half_z]
Counts minecraft:item entities in <world>/entities/r.*.mca, inside and outside the built area (|x|<=half_x, |z|<=half_z; default 366, 266), by item id."""
import sys, os, glob
from collections import Counter
ns = {}
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'nbt_entities.py')).read().replace('\nmain()', '\n')
exec(compile(src, 'nbt_entities', 'exec'), ns)
chunks = ns['chunks']
world = sys.argv[1]
hx = int(sys.argv[2]) if len(sys.argv) > 2 else 366
hz = int(sys.argv[3]) if len(sys.argv) > 3 else 266
inside, outside = Counter(), Counter()
other = Counter()
for path in sorted(glob.glob(os.path.join(world, 'entities', 'r.*.mca'))):
    for idx, nbt in chunks(path):
        if not nbt or '_error' in nbt: continue
        for e in nbt.get('Entities') or []:
            eid = e.get('id', '?')
            if eid != 'minecraft:item':
                other[eid] += 1
                continue
            x, y, z = e.get('Pos', [0, 0, 0])
            it = e.get('Item', {})
            name = it.get('id', '?').replace('minecraft:', '')
            n = it.get('Count', 1)
            (inside if abs(x) <= hx + 1 and abs(z) <= hz + 1 else outside)[name] += n
print('world', world)
print('items inside the built area (|x|<=%d, |z|<=%d): %d stacks-by-count total %d' % (hx, hz, sum(1 for _ in inside.elements()) if False else len(inside), sum(inside.values())))
print('  ', dict(inside.most_common(12)))
print('items outside: total %d' % sum(outside.values()), dict(outside.most_common(8)))
print('other entities:', dict(other.most_common(6)))
