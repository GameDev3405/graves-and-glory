#!/usr/bin/env python
"""Read-only block map of a saved world: python -I blocks_dump.py <world dir> <y> <x0> <x1> <z0> <z1>  (prints one character per block at height y)."""
import sys, os, struct, zlib, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import importlib.util
spec = importlib.util.spec_from_file_location('ne', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'nbt_entities.py'))
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'nbt_entities.py')).read().replace('\nmain()', '\n')
ns = {}
exec(compile(src, 'nbt_entities', 'exec'), ns)
read_nbt = ns['read_nbt']

world, y, x0, x1, z0, z1 = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5]), int(sys.argv[6])
cache = {}
def chunk(cx, cz):
    key = (cx, cz)
    if key in cache: return cache[key]
    rx, rz = cx >> 5, cz >> 5
    path = os.path.join(world, 'region', 'r.%d.%d.mca' % (rx, rz))
    res = None
    if os.path.exists(path):
        with open(path, 'rb') as f: data = f.read()
        i = (cx & 31) + (cz & 31) * 32
        off = struct.unpack_from('>I', b'\x00' + data[i * 4:i * 4 + 3], 0)[0]
        if off:
            start = off * 4096
            ln, comp = struct.unpack_from('>IB', data, start)
            raw = data[start + 5:start + 4 + ln]
            raw = zlib.decompress(raw) if comp == 2 else raw
            res = read_nbt(raw)
    cache[key] = res
    return res

def block(x, yy, z):
    c = chunk(x >> 4, z >> 4)
    if not c: return None
    sy = yy >> 4
    for s in c.get('sections', []):
        if s.get('Y') == sy:
            bs = s.get('block_states', {})
            pal = bs.get('palette', [])
            data = bs.get('data')
            if not pal: return None
            if data is None or len(pal) == 1:
                return pal[0]['Name']
            bits = max(4, math.ceil(math.log2(len(pal))))
            per = 64 // bits
            idx = ((yy & 15) << 8) | ((z & 15) << 4) | (x & 15)
            word = data[idx // per] & 0xFFFFFFFFFFFFFFFF
            val = (word >> ((idx % per) * bits)) & ((1 << bits) - 1)
            return pal[val]['Name']
    return 'minecraft:air'

SYM = {'spruce_planks': 'P', 'spruce_fence': 'F', 'spruce_log': 'L', 'spruce_slab': 's', 'water': '~', 'air': '.', 'grass_block': '"', 'stone_bricks': '#',
       'smooth_stone': '=', 'chiseled_stone_bricks': 'C', 'oak_fence': 'f', 'dirt': 'd', 'coarse_dirt': 'd', 'stone': 'S', 'mossy_stone_bricks': 'm', 'stone_brick_wall': 'W',
       'lantern': 'l', 'andesite': 'a', 'mossy_cobblestone': 'o', 'gravel': 'g', 'dirt_path': 'p', 'oak_planks': 'O', 'cyan_wool': 'w', 'white_wool': 'w', 'sea_lantern': 'L'}
print('y=%d  x %d..%d (left to right), z %d..%d (top to bottom)' % (y, x0, x1, z0, z1))
for z in range(z0, z1 + 1):
    row = []
    for x in range(x0, x1 + 1):
        b = block(x, y, z)
        n = b.split(':')[-1] if b else None
        row.append('?' if n is None else SYM.get(n, '*'))
    print('%5d %s' % (z, ''.join(row)))
