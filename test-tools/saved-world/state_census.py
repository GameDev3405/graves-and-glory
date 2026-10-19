#!/usr/bin/env python
"""Read-only census of block states in a saved world: python -I state_census.py <world dir> name[,name...]
Counts every block of the named kinds by its property combination, over all region files (the first chunk section palette entry counts, decoded exactly)."""
import sys, os, struct, zlib, math, collections
here = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(here, 'nbt_entities.py')).read().replace('\nmain()', '\n')
ns = {}
exec(compile(src, 'nbt_entities', 'exec'), ns)
read_nbt = ns['read_nbt']
world = sys.argv[1]
names = set('minecraft:' + n for n in sys.argv[2].split(','))
counts = collections.defaultdict(collections.Counter)
region_dir = os.path.join(world, 'region')
for fn in sorted(os.listdir(region_dir)):
    if not fn.endswith('.mca'): continue
    with open(os.path.join(region_dir, fn), 'rb') as f: data = f.read()
    for i in range(1024):
        off = struct.unpack_from('>I', b'\x00' + data[i * 4:i * 4 + 3], 0)[0]
        if not off: continue
        start = off * 4096
        ln, comp = struct.unpack_from('>IB', data, start)
        raw = data[start + 5:start + 4 + ln]
        raw = zlib.decompress(raw) if comp == 2 else raw
        c = read_nbt(raw)
        for s in c.get('sections', []):
            bs = s.get('block_states', {})
            pal = bs.get('palette', [])
            if not any(p['Name'] in names for p in pal): continue
            d = bs.get('data')
            if d is None or len(pal) == 1:
                tallies = {0: 4096}
            else:
                bits = max(4, math.ceil(math.log2(len(pal))))
                per = 64 // bits
                tallies = collections.Counter()
                for idx in range(4096):
                    word = d[idx // per] & 0xFFFFFFFFFFFFFFFF
                    tallies[(word >> ((idx % per) * bits)) & ((1 << bits) - 1)] += 1
            for k, n in tallies.items():
                p = pal[k]
                if p['Name'] in names:
                    props = ','.join('%s=%s' % (a, b) for a, b in sorted((p.get('Properties') or {}).items()))
                    counts[p['Name'].split(':')[1]][props] += n
for name in sorted(counts):
    print(name, sum(counts[name].values()))
    for props, n in counts[name].most_common(): print('   %6d  %s' % (n, props))
