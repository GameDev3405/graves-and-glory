#!/usr/bin/env python
"""Entity census of a saved world: parses <world>/entities/r.*.mca (1.20.1 entity storage) and prints counts by entity id, item ids, tags and age.
usage: python -I nbt_entities.py <world dir> [--items] [--where]
Read only. Chunk data is zlib (2), gzip (1), none (3); lz4 (4) is skipped and counted.
"""
import sys, os, struct, zlib, gzip, glob
from collections import Counter, defaultdict

def read_nbt(buf):
    pos = 0
    def u(fmt, n):
        nonlocal pos
        v = struct.unpack_from(fmt, buf, pos)[0]
        pos += n
        return v
    def string():
        nonlocal pos
        ln = u('>H', 2)
        s = buf[pos:pos + ln].decode('utf-8', 'replace')
        pos += ln
        return s
    def payload(t):
        nonlocal pos
        if t == 1: return u('>b', 1)
        if t == 2: return u('>h', 2)
        if t == 3: return u('>i', 4)
        if t == 4: return u('>q', 8)
        if t == 5: return u('>f', 4)
        if t == 6: return u('>d', 8)
        if t == 7:
            ln = u('>i', 4); pos += ln; return None
        if t == 8: return string()
        if t == 9:
            et = u('>b', 1); ln = u('>i', 4)
            return [payload(et) for _ in range(ln)]
        if t == 10:
            d = {}
            while True:
                tt = u('>b', 1)
                if tt == 0: return d
                name = string()
                d[name] = payload(tt)
        if t == 11:
            ln = u('>i', 4); vals = list(struct.unpack_from('>%di' % ln, buf, pos)); pos += 4 * ln; return vals
        if t == 12:
            ln = u('>i', 4); vals = list(struct.unpack_from('>%dq' % ln, buf, pos)); pos += 8 * ln; return vals
        raise ValueError('bad tag %d' % t)
    t = u('>b', 1)
    name = string()
    return payload(t)

def chunks(path):
    with open(path, 'rb') as f:
        data = f.read()
    if len(data) < 8192:
        return
    for i in range(1024):
        off = struct.unpack_from('>I', b'\x00' + data[i * 4:i * 4 + 3], 0)[0]
        cnt = data[i * 4 + 3]
        if off == 0 or cnt == 0:
            continue
        start = off * 4096
        ln, comp = struct.unpack_from('>IB', data, start)
        raw = data[start + 5:start + 4 + ln]
        try:
            if comp == 2: raw = zlib.decompress(raw)
            elif comp == 1: raw = gzip.decompress(raw)
            elif comp == 3: pass
            else:
                yield i, None
                continue
            yield i, read_nbt(raw)
        except Exception as e:
            yield i, {'_error': str(e)}

def main():
    world = sys.argv[1]
    show_items = '--items' in sys.argv
    show_where = '--where' in sys.argv
    ids = Counter(); items = Counter(); tags = Counter(); ages = Counter(); skipped = 0; errors = 0
    where = defaultdict(Counter)
    total_chunks = 0; nonempty = 0
    files = sorted(glob.glob(os.path.join(world, 'entities', 'r.*.mca')))
    for fpath in files:
        base = os.path.basename(fpath)
        for idx, nbt in chunks(fpath):
            total_chunks += 1
            if nbt is None: skipped += 1; continue
            if '_error' in nbt: errors += 1; continue
            ents = nbt.get('Entities') or []
            if ents: nonempty += 1
            for e in ents:
                eid = e.get('id', '?')
                ids[eid] += 1
                where[base][eid] += 1
                for tg in e.get('Tags', []) or []:
                    tags[tg.split(':')[0] if tg.startswith('camp') else tg] += 1
                if eid == 'minecraft:item':
                    it = e.get('Item', {})
                    items[it.get('id', '?')] += it.get('Count', 1) if show_items else 1
                    a = e.get('Age', 0)
                    ages['<1200' if a < 1200 else '<3000' if a < 3000 else '<6000' if a < 6000 else '>=6000'] += 1
    print('files', len(files), 'chunks', total_chunks, 'with entities', nonempty, 'skipped(lz4/other)', skipped, 'errors', errors)
    print('TOTAL entities', sum(ids.values()))
    for k, v in ids.most_common():
        print('  %-40s %d' % (k, v))
    print('tags:', dict(tags.most_common(12)))
    if items:
        print('item stacks by item id:')
        for k, v in items.most_common(15):
            print('  %-40s %d' % (k, v))
        print('item ages (ticks):', dict(ages))
    if show_where:
        for b, c in where.items():
            print(b, dict(c.most_common(6)))

main()
