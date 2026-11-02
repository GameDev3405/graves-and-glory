"""Checks a built map (manifest + tiles) against its design. The walkability grid is rebuilt from the tiles'
top solid block, so the check covers what was actually built."""
import gzip
import hashlib
import json
import math
import pathlib
import struct
from collections import deque
from dataclasses import dataclass

import numpy as np

from mapengine.build import _BLOCK, canonical_bytes
from mapforge import nbt
from mapforge.structure import Structure, block_name

MAX_STEP = 1   # largest height change of a walkable step
MAX_WADE = 1   # deepest water that can be waded
NONE = -32768


@dataclass
class Issue:
    code: str
    object_id: str
    message: str


def _load_fast(path):
    """(palette, x, y, z, state index) for a tile written by the builder, without making a tag per block.
    The block records are cut out and read with numpy; the rest goes through mapforge. None if the file does not
    have the builder's layout (the caller then uses Structure.load)."""
    raw = pathlib.Path(path).read_bytes()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    head = b'\x09\x00\x06blocks\x0a'
    i = raw.find(head)
    if i < 0:
        return None
    n = struct.unpack_from('>i', raw, i + len(head))[0]
    start = i + len(head) + 4
    end = start + n * _BLOCK.itemsize
    if n < 0 or end > len(raw):
        return None
    rec = np.frombuffer(raw[start:end], dtype=_BLOCK)
    if n and not ((rec['t1'] == nbt.LIST).all() and (rec['n1s'] == b'pos').all() and (rec['n2s'] == b'state').all()):
        return None
    rest = raw[:i + len(head)] + struct.pack('>i', 0) + raw[end:]
    s = Structure.from_tag(nbt.decode(rest)[1])
    return s.palette, rec['x'], rec['y'], rec['z'], rec['s']


def _columns(path, shape):
    solid = np.full(shape, NONE, dtype=np.int32)
    water = solid.copy()
    fast = _load_fast(path)
    if fast is None:
        s = Structure.load(path)
        arr = np.array([(p[0], p[1], p[2], i) for p, i, _ in s.blocks], dtype=np.int64).reshape(-1, 4)
        fast = (s.palette, arr[:, 0], arr[:, 1], arr[:, 2], arr[:, 3])
    palette, xs, ys, zs, idx = fast
    wet = np.array([block_name(p) == 'minecraft:water' for p in palette] or [False])[idx]
    for a, m in ((solid, ~wet), (water, wet)):
        np.maximum.at(a, (zs[m], xs[m]), ys[m].astype(np.int32))
    return solid, water


def read_columns(out_dir, manifest, width, depth):
    """(solid top, water top) as int32 [z, x] in layers above floorY; NONE where a column has no such block."""
    solid = np.full((depth, width), NONE, dtype=np.int32)
    water = solid.copy()
    for t in manifest['tiles']:
        sol, wat = _columns(pathlib.Path(out_dir) / t['file'], (t['size'][2], t['size'][0]))
        solid[t['z']:t['z'] + sol.shape[0], t['x']:t['x'] + sol.shape[1]] = sol
        water[t['z']:t['z'] + wat.shape[0], t['x']:t['x'] + wat.shape[1]] = wat
    return solid, water


def _cell(o, width, depth):
    return min(width - 1, max(0, int(math.floor(o['x'])))), min(depth - 1, max(0, int(math.floor(o['z']))))


def _reach(solid, water, start):
    """Boolean [z, x] of cells reachable from start by 4-neighbour steps over walkable ground."""
    depth, width = solid.shape
    wade = np.where(water == NONE, 0, np.maximum(water - solid, 0))
    ok = (solid != NONE) & (wade <= MAX_WADE)
    seen = np.zeros(solid.shape, dtype=bool)
    sx, sz = start
    if not ok[sz, sx]:
        return seen
    seen[sz, sx] = True
    q = deque([(sx, sz)])
    while q:
        x, z = q.popleft()
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if 0 <= nx < width and 0 <= nz < depth and not seen[nz, nx] and ok[nz, nx] \
                    and abs(int(solid[nz, nx]) - int(solid[z, x])) <= MAX_STEP:
                seen[nz, nx] = True
                q.append((nx, nz))
    return seen


def check(d, out_dir):
    out_dir = pathlib.Path(out_dir)
    manifest = json.loads((out_dir / 'map.json').read_text(encoding='utf-8'))
    issues = []
    if manifest.get('design_sha256') != hashlib.sha256(canonical_bytes(d)).hexdigest():
        issues.append(Issue('stale', d['id'], 'the built map is older than the design: rebuild it'))
    width, depth = d['width'], d['depth']
    solid, water = read_columns(out_dir, manifest, width, depth)
    objs = d.get('objects', [])
    points = [o for o in objs if o['kind'] == 'point']
    spawns = [o for o in objs if o['kind'] == 'spawn']
    floor = manifest['floorY']
    mpoints = {p['name']: p for p in manifest['points']}
    mspawns = {s.get('id'): s for s in manifest['spawns']}

    def moved(o, m):
        cx, cz = _cell(o, width, depth)
        if m is None or m['x'] != o['x'] or m['z'] != o['z'] or m['y'] != floor + int(solid[cz, cx]) + 1:
            issues.append(Issue('moved', o['id'], f"{o['id']} in the built map is not where the design puts it"))

    for o in points:
        moved(o, mpoints.get(o['name']))
    for o in spawns:
        moved(o, mspawns.get(o['id']))
    for o in points + spawns:
        cx, cz = _cell(o, width, depth)
        if water[cz, cx] != NONE and water[cz, cx] > solid[cz, cx]:
            issues.append(Issue('in_water', o['id'], f"{o['id']} is in water"))
    for o in points:
        r = o['radius']
        if o['x'] - r < 0 or o['x'] + r > width or o['z'] - r < 0 or o['z'] + r > depth:
            issues.append(Issue('radius_clipped', o['id'], f"{o['id']}: radius {r} leaves the canvas"))
    reach = {s['id']: _reach(solid, water, _cell(s, width, depth)) for s in spawns}
    for o in points:
        cx, cz = _cell(o, width, depth)
        for s in spawns:
            if not reach[s['id']][cz, cx]:
                issues.append(Issue('unreachable', o['id'], f"{o['id']} cannot be reached from the {s['team']} spawn"))
                break
    return issues
