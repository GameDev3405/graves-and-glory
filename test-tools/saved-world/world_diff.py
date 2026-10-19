#!/usr/bin/env python
"""Read-only block diff of two saved worlds: python -I world_diff.py <worldA> <worldB> [samples]
Compares every chunk section's palette and packed data; only sections that differ are decoded. Prints the differing (A -> B) block pairs with counts and sample positions."""
import sys, os, struct, zlib, math, collections
here = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(here, 'nbt_entities.py')).read().replace('\nmain()', '\n')
ns = {}
exec(compile(src, 'nbt_entities', 'exec'), ns)
read_nbt = ns['read_nbt']
A, B = sys.argv[1], sys.argv[2]
samples = int(sys.argv[3]) if len(sys.argv) > 3 else 3

def chunks(world):
    d = os.path.join(world, 'region')
    out = {}
    for fn in sorted(os.listdir(d)):
        if not fn.endswith('.mca'): continue
        with open(os.path.join(d, fn), 'rb') as f: data = f.read()
        for i in range(1024):
            off = struct.unpack_from('>I', b'\x00' + data[i * 4:i * 4 + 3], 0)[0]
            if not off: continue
            start = off * 4096
            ln, comp = struct.unpack_from('>IB', data, start)
            raw = data[start + 5:start + 4 + ln]
            raw = zlib.decompress(raw) if comp == 2 else raw
            c = read_nbt(raw)
            out[(c['xPos'], c['zPos'])] = {s['Y']: s.get('block_states', {}) for s in c.get('sections', [])}
    return out

def decode(bs):
    pal = bs.get('palette', [])
    d = bs.get('data')
    names = [p['Name'].split(':')[1] + (('[' + ','.join('%s=%s' % kv for kv in sorted(p['Properties'].items())) + ']') if p.get('Properties') else '') for p in pal]
    if not pal: return ['air'] * 4096
    if d is None or len(pal) == 1: return [names[0]] * 4096
    bits = max(4, math.ceil(math.log2(len(pal))))
    per = 64 // bits
    mask = (1 << bits) - 1
    out = [None] * 4096
    for idx in range(4096):
        out[idx] = names[((d[idx // per] & 0xFFFFFFFFFFFFFFFF) >> ((idx % per) * bits)) & mask]
    return out

ca, cb = chunks(A), chunks(B)
pairs = collections.Counter(); where = collections.defaultdict(list)
only_a = sorted(set(ca) - set(cb)); only_b = sorted(set(cb) - set(ca))
differing_sections = 0
for key in sorted(set(ca) & set(cb)):
    sa, sb = ca[key], cb[key]
    for y in sorted(set(sa) | set(sb)):
        a, b = sa.get(y), sb.get(y)
        if a == b: continue
        differing_sections += 1
        da = decode(a) if a is not None else ['air'] * 4096
        db = decode(b) if b is not None else ['air'] * 4096
        for idx in range(4096):
            if da[idx] != db[idx]:
                pair = (da[idx], db[idx]); pairs[pair] += 1
                if len(where[pair]) < samples:
                    where[pair].append((key[0] * 16 + (idx & 15), y * 16 + (idx >> 8), key[1] * 16 + ((idx >> 4) & 15)))
print('chunks only in A: %d, only in B: %d, differing sections: %d, differing blocks: %d' % (len(only_a), len(only_b), differing_sections, sum(pairs.values())))
for (pa, pb), n in pairs.most_common(70):
    print('%7d  %s -> %s   e.g. %s' % (n, pa, pb, where[(pa, pb)]))
