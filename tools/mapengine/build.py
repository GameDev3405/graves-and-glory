"""Builds a map engine design into structure tiles plus map.json. Deterministic: same design, same bytes."""
import gzip
import hashlib
import io
import json
import math
import pathlib
import struct

import numpy as np

from mapengine import buildings
from mapengine import design as dsn
from mapengine.terrain import NO_WATER, heightfield
from mapengine.walk import NONE, distances
from mapforge import nbt
from mapforge.structure import Structure, block_name

TILE = 64
FLOOR = -16  # lowest built layer, relative to baseY
ROLES = ('deep', 'under', 'surface', 'bed', 'water', 'road')


# Blocks the mod's read-back (EngineMaps phase 4) does not count as ground: air, barrier, sea lantern, fences, wool.
_NOT_GROUND = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:barrier', 'minecraft:sea_lantern',
               'minecraft:water', 'minecraft:structure_void'}


def ground_state(state):
    n = block_name(state)
    return n not in _NOT_GROUND and not n.endswith('_fence') and not n.endswith('_wool')


class BuildError(Exception):
    def __init__(self, problems):
        self.problems = list(problems)
        super().__init__('; '.join(self.problems))


def canonical_bytes(d):
    return (json.dumps(d, sort_keys=True, indent=1) + '\n').encode('utf-8')


def _states(zones):
    """(state strings, lookup[zone index + 1, role] -> state id); zone -1 is the default palette."""
    pals = json.loads(dsn.PALETTES_PATH.read_text(encoding='utf-8'))
    names = ['default'] + list(zones)
    states, lut = [], np.zeros((len(names), len(ROLES)), dtype=np.int16)
    for i, n in enumerate(names):
        for j, role in enumerate(ROLES):
            states.append(pals[n][role])
            lut[i, j] = len(states) - 1
    return states, lut


def _tile(f, lut, x0, z0, nx, nz, H):
    ground = np.maximum(f.height[z0:z0 + nz, x0:x0 + nx].astype(np.int32), FLOOR)
    water = f.water[z0:z0 + nz, x0:x0 + nx].astype(np.int32)
    roles = lut[f.zone[z0:z0 + nz, x0:x0 + nx].astype(np.int32) + 1]  # [z, x, role]
    road = f.road[z0:z0 + nz, x0:x0 + nx]
    h = (ground - FLOOR)[:, :, None]
    wet = (water != NO_WATER) & (water > ground)
    wtop = np.where(wet, water - FLOOR, -1)[:, :, None]
    y = np.arange(H, dtype=np.int32)[None, None, :]
    top = np.where(wet, roles[:, :, 3], np.where(road, roles[:, :, 5], roles[:, :, 2]))[:, :, None]
    out = np.full((nz, nx, H), -1, dtype=np.int16)
    out = np.where(y <= h - 4, roles[:, :, 0][:, :, None], out)
    out = np.where((y > h - 4) & (y < h), roles[:, :, 1][:, :, None], out)
    out = np.where(y == h, top, out)
    out = np.where((y > h) & (y <= wtop), roles[:, :, 4][:, :, None], out)
    return out.transpose(1, 0, 2)  # [x, z, y]: nonzero scans x outer, then z, then y ascending


_BLOCK = np.dtype([('t1', 'u1'), ('n1', '>u2'), ('n1s', 'S3'), ('e', 'u1'), ('c', '>i4'), ('x', '>i4'), ('y', '>i4'),
                   ('z', '>i4'), ('t2', 'u1'), ('n2', '>u2'), ('n2s', 'S5'), ('s', '>i4'), ('end', 'u1')])


def _write_tile(path, arr, states, H):
    """Writes the tile as a gzip structure file. Same bytes as mapforge Structure.save, but the block list is packed
    with numpy (a tile holds ~150k blocks, too many for the tag-object writer)."""
    xs, zs, ys = np.nonzero(arr >= 0)  # x outer, then z, then y ascending
    ids = arr[xs, zs, ys]
    uniq, first = np.unique(ids, return_index=True)
    order = uniq[np.argsort(first)]  # palette by first appearance
    remap = np.zeros(int(order.max()) + 1 if len(order) else 1, dtype=np.int64)
    remap[order] = np.arange(len(order))
    rec = np.zeros(len(ids), dtype=_BLOCK)
    rec['t1'], rec['n1'], rec['n1s'], rec['e'], rec['c'] = nbt.LIST, 3, b'pos', nbt.INT, 3
    rec['x'], rec['y'], rec['z'] = xs, ys, zs
    rec['t2'], rec['n2'], rec['n2s'], rec['s'] = nbt.INT, 5, b'state', remap[ids]
    s = Structure((arr.shape[0], H, arr.shape[1]), [states[int(i)] for i in order], [])
    tag = s.to_tag().value
    out = io.BytesIO()
    out.write(b'\x0a\x00\x00')
    for key in ('size', 'entities'):
        nbt._write_named(out, key, tag[key])
    out.write(struct.pack('>bH', nbt.LIST, 6) + b'blocks' + struct.pack('>bi', nbt.COMPOUND, len(rec)))
    out.write(rec.tobytes())
    for key in ('palette', 'DataVersion'):
        nbt._write_named(out, key, tag[key])
    out.write(b'\x00')
    pathlib.Path(path).write_bytes(gzip.compress(out.getvalue(), mtime=0))


def build(d, out_dir):
    problems = dsn.build_problems(d)
    if problems:
        raise BuildError(problems)
    out_dir = pathlib.Path(out_dir)
    (out_dir / 'tiles').mkdir(parents=True, exist_ok=True)
    f = heightfield(d)
    base = d['baseY']
    w, dp = d['width'], d['depth']
    top = int(f.height.max())
    wet = f.water[f.water != NO_WATER]
    if wet.size:
        top = max(top, int(wet.max()))
    states, lut = _states(f.zones)
    stamps = buildings.stamps(d, f, states)
    for _, _, ys, _ in stamps:
        if ys.size:
            top = max(top, int(ys.max()))
    top = max(top, FLOOR)
    H = top - FLOOR + 1
    is_ground = np.array([ground_state(s) for s in states] + [False])  # index -1 (no block) is the last entry
    is_water = np.array([block_name(s) == 'minecraft:water' for s in states] + [False])
    solid = np.full((dp, w), NONE, dtype=np.int32)
    water = solid.copy()
    tiles = []
    for z0 in range(0, dp, TILE):
        for x0 in range(0, w, TILE):
            nx, nz = min(TILE, w - x0), min(TILE, dp - z0)
            arr = _tile(f, lut, x0, z0, nx, nz, H)
            for xs, zs, ys, ids in stamps:  # design order: a later building overwrites an earlier one
                m = (xs >= x0) & (xs < x0 + nx) & (zs >= z0) & (zs < z0 + nz) & (ys >= FLOOR)
                arr[xs[m] - x0, zs[m] - z0, ys[m] - FLOOR] = ids[m]
            for a, mask in ((solid, is_ground[arr]), (water, is_water[arr])):  # the mod's read-back rule
                hit = mask.any(axis=2)
                y = H - 1 - np.argmax(mask[:, :, ::-1], axis=2)
                a[z0:z0 + nz, x0:x0 + nx] = np.where(hit, base + FLOOR + y, NONE).T
            rel = f'tiles/t_{x0}_{z0}.nbt'
            _write_tile(out_dir / rel, arr, states, H)
            tiles.append({'file': rel, 'x': x0, 'z': z0, 'size': [nx, H, nz]})

    def stand(o):
        cx, cz = min(w - 1, int(math.floor(o['x']))), min(dp - 1, int(math.floor(o['z'])))
        return int(solid[cz, cx]) + 1

    objs = d.get('objects', [])
    points = [{'name': o['name'], 'x': o['x'], 'y': stand(o), 'z': o['z'], 'radius': o['radius']}
              for o in objs if o['kind'] == 'point']
    spawns = [{'id': o['id'], 'team': o['team'], 'x': o['x'], 'y': stand(o), 'z': o['z']} for o in objs if o['kind'] == 'spawn']
    placed = [{'id': o['id'], 'piece': o['piece'], 'x': o['x'], 'y': base + int(f.height[o['z'], o['x']]), 'z': o['z'],
               'rotation': o['rotation'], 'box': list(buildings.box(o))} for o in buildings.of(d)]
    (out_dir / 'columns.bin').write_bytes(np.stack([solid, water]).astype('<i2').tobytes())
    routes = []
    for s in spawns:
        dist = distances(solid, water, (min(w - 1, int(math.floor(s['x']))), min(dp - 1, int(math.floor(s['z'])))))
        for p in points:
            walk = int(dist[min(dp - 1, int(math.floor(p['z']))), min(w - 1, int(math.floor(p['x'])))])
            routes.append({'spawn': s['id'], 'point': p['name'],
                           'straight': round(math.hypot(p['x'] - s['x'], p['z'] - s['z']), 2),
                           'walk': walk if walk >= 0 else None})
    roads = [{'id': o['id'], 'path': o['path'], 'width': o['width']} for o in objs if o['kind'] == 'road']
    manifest = {'format': 1, 'id': d['id'], 'width': w, 'depth': dp, 'origin': [-(w // 2), -(dp // 2)],
                'baseY': base, 'floorY': base + FLOOR, 'columns': 'columns.bin', 'tiles': tiles, 'points': points,
                'spawns': spawns, 'roads': roads, 'buildings': placed, 'routes': routes,
                'design_sha256': hashlib.sha256(canonical_bytes(d)).hexdigest()}
    (out_dir / 'map.json').write_bytes(canonical_bytes(manifest))
    return manifest
