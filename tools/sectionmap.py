"""Packs a map made of exported section templates into a bundled map the mod plays in its own map slot (SectionMaps).

    python tools/sectionmap.py SETTINGS SECTIONS_DIR OUT_DIR
    python tools/sectionmap.py docs/maps/kingsfield/game.json <export dir> src/main/resources/data/gravesandglory/sectionmaps/kingsfield_cq
    python tools/sectionmap.py nav src/main/resources/data/gravesandglory/sectionmaps/kingsfield_cq    # (re)bakes nav.bin.gz from a packed map's own tiles

SETTINGS (for example docs/maps/kingsfield/game.json) names the section grid, the field (playable half sizes), the
spawns and the capture points. SECTIONS_DIR holds the section templates (Kingsfield: kfmap.py export). OUT_DIR gets:
  map.json        the EngineMap manifest (points and spawns with their standing y, walking routes, tiles) plus "field"
  columns.bin.gz  solid and water heights per column, little-endian int16, the mod's read-back rule (as mapengine build)
  nav.bin.gz      the floors of every column (floors_of), for the mod's route field (NavField): written by pack and by `python tools/sectionmap.py nav MAPDIR`
  <section>.stk   one gzip nbt per section: size, palette, and "stack" (big-endian int16 palette index per cell, -1 none,
                  cell (z*sx + x)*sy + y), the layout EngineMaps keeps in memory. A structure template of the same
                  section is ~20 times larger (a compound per block). A section whose template holds prop anchors (a command block whose
                  Command starts gravesandglory:prop/, Props.java) also gets "anchors": a list of {x, y, z (cells of the section), cmd}, which
                  EngineMaps writes back into the command blocks' block entities and then runs Props.scanBox over the map. No anchors, no key:
                  the sections of a map without props (Kingsfield, Rivergate) pack to the same bytes as before.
Deterministic: same sections and settings, same bytes.
"""
import gzip
import hashlib
import io
import json
import math
import pathlib
import struct
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from mapforge import nbt  # noqa: E402
from mapforge.structure import Structure, parse_state, block_name  # noqa: E402
from mapengine.build import ground_state, water_state  # noqa: E402
from mapengine.walk import NONE  # noqa: E402

# Walking over floors (a column can hold several: the ground under a gatehouse and its roof). A floor is a block a troop collides with, not a
# fence, wall, bar or gate (1.5 high), with two cells above it free of such blocks and the lower of them not water (a troop wades no pond: its pathfinder
# will not lead it through water, so a lake bed is no floor, and the pond on New Kingsfield at (-43, 76) was the dead pocket troops crowded at). Steps of at most 1 up or down between
# 4-neighbour columns, as mapengine.walk does over the top block only.
PASS = ('air', 'grass', 'fern', 'azure_bluet', 'cornflower', 'dandelion', 'oxeye_daisy', 'poppy', 'wheat', 'carrots', 'potatoes', 'beetroots',
        'dead_bush', 'brown_mushroom', 'red_mushroom', 'rail', 'fire', 'ladder', 'structure_void', 'cave_air', 'void_air', 'lily_pad')
TALL = ('_fence', '_wall', 'iron_bars', '_fence_gate')


MAP_FENCE = 'gravesandglory:troop_fence'
MAP_WATER = 'gravesandglory:still_water'


def kind(state):
    full = block_name(state)
    if full == MAP_FENCE:  # a wall (map block): never a floor, blocks the two cells above a floor like a fence, so the edges through it are cut
        return 3
    if full == MAP_WATER:  # no-entry water: never a floor, and a floor under it has no standing room
        return 2
    n = full.split(':')[1]
    if n == 'water':
        return 2
    if n in PASS or n.endswith('_wall_banner') or n.endswith('_carpet'):
        return 0
    if n.endswith(TALL):
        return 3
    return 1


def floors_of(stack, palette, floor):
    """{(z, x): [world y of each floor]} over one section; stack [z, x, y]."""
    k = np.array([kind(p) for p in palette] + [0])[stack]  # -1 (no block) -> last entry, air
    sz, sx, sy = k.shape
    pad = np.concatenate([k, np.zeros((sz, sx, 2), dtype=k.dtype)], axis=2)
    a1, a2 = pad[:, :, 1:sy + 1], pad[:, :, 2:sy + 2]
    stand = (k == 1) & (a1 != 1) & (a1 != 3) & (a2 != 1) & (a2 != 3) & (a1 != 2) & (a2 != 2)
    out = {}
    for z, x, y in zip(*np.nonzero(stand)):
        out.setdefault((int(z), int(x)), []).append(floor + int(y))
    return out


def floor_distances(floors, w, d, start, near_y):
    """{(cx, cz, y): steps} from the floor of start (cx, cz) nearest near_y."""
    from collections import deque
    sx, sz = start
    here = floors.get((sz, sx), [])
    if not here:
        return {}
    s = (sx, sz, min(here, key=lambda y: abs(y - near_y)))
    dist = {s: 0}
    q = deque([s])
    while q:
        x, z, y = q.popleft()
        nd = dist[(x, z, y)] + 1
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if not (0 <= nx < w and 0 <= nz < d):
                continue
            for ny in floors.get((nz, nx), ()):
                if abs(ny - y) <= 1 and (nx, nz, ny) not in dist:
                    dist[(nx, nz, ny)] = nd
                    q.append((nx, nz, ny))
    return dist


NAV = 'nav.bin.gz'


def write_nav(out, floors, w, d, floor, sha):
    """nav.bin.gz: b'NAV1', the map's design_sha256 (64 ascii bytes, so a stale file is seen), width, depth, floorY (int32 LE), then per column (z*w + x) the
    number of floors (uint8), then every floor's y minus floorY (uint8) in column order, lowest first. Deterministic."""
    counts = np.zeros(w * d, dtype=np.uint8)
    ys = []
    for (z, x), fl in sorted(floors.items()):
        counts[z * w + x] = len(fl)
        for y in sorted(fl):
            if not 0 <= y - floor < 256:
                raise SystemExit(f'floor y {y} at column {x},{z} is outside floorY {floor} + 0..255')
            ys.append(y - floor)
    body = b'NAV1' + sha.encode('ascii') + struct.pack('<iii', w, d, floor) + counts.tobytes() + bytes(ys)
    pathlib.Path(out).write_bytes(gzip.compress(body, mtime=0))


def floors_from_tiles(mapdir):
    """The floors dict of a packed map, read back from its own .stk tiles (the same floors_of the packer ran)."""
    mapdir = pathlib.Path(mapdir)
    m = json.loads((mapdir / 'map.json').read_text(encoding='utf-8'))
    floors = {}
    for t in m['tiles']:
        _, root = nbt.read_file(mapdir / t['file'])
        plain = root.value
        sx, sy, sz = [int(v.value) for v in plain['size'].value]
        palette = [e.value['Name'].value for e in plain['palette'].value]
        raw = plain['stack'].value
        stack = np.frombuffer(raw if isinstance(raw, (bytes, bytearray)) else bytes(b & 255 for b in raw), dtype='>i2').reshape(sz, sx, sy)
        for (z, x), ys in floors_of(stack, palette, m['floorY']).items():
            floors[(t['z'] + z, t['x'] + x)] = ys
    return m, floors


def bake_nav(mapdir):
    m, floors = floors_from_tiles(mapdir)
    write_nav(pathlib.Path(mapdir) / NAV, floors, m['width'], m['depth'], m['floorY'], m['design_sha256'])
    print(f"{NAV}: {len(floors)} columns with floors, {(pathlib.Path(mapdir) / NAV).stat().st_size // 1024} KiB in {mapdir}")


PROP_PREFIX = 'gravesandglory:prop/'


def anchors_of(blocks, palette):
    """[(x, y, z, Command)] of the prop anchors of a template's blocks [(pos, palette index, block entity tag)], sorted."""
    out = []
    for (x, y, z), idx, be in blocks:
        if be is None or block_name(palette[idx]) != 'minecraft:command_block':
            continue
        cmd = be.value.get('Command')
        if cmd is not None and cmd.value.startswith(PROP_PREFIX):
            out.append((x, y, z, cmd.value))
    return sorted(out)


def pack_tile(path, size, palette, stack, anchors=()):
    pal = []
    for state in palette:
        name, props = parse_state(state)
        entry = {'Name': nbt.string(name)}
        if props:
            entry['Properties'] = nbt.compound({k: nbt.string(v) for k, v in sorted(props.items())})
        pal.append(nbt.compound(entry))
    root = nbt.compound({'size': nbt.list_(nbt.INT, [nbt.int_(c) for c in size]),
                         'palette': nbt.list_(nbt.COMPOUND, pal)})
    out = io.BytesIO()
    out.write(b'\x0a\x00\x00')
    for key in ('size', 'palette'):
        nbt._write_named(out, key, root.value[key])
    if anchors:
        nbt._write_named(out, 'anchors', nbt.list_(nbt.COMPOUND, [nbt.compound({'x': nbt.int_(x), 'y': nbt.int_(y), 'z': nbt.int_(z), 'cmd': nbt.string(c)})
                                                                  for x, y, z, c in anchors]))
    raw = stack.astype('>i2').tobytes()
    out.write(struct.pack('>bH', 7, 5) + b'stack' + struct.pack('>i', len(raw)) + raw)  # TAG_Byte_Array
    out.write(b'\x00')
    pathlib.Path(path).write_bytes(gzip.compress(out.getvalue(), mtime=0))


def main(settings_path, sections_dir, out_dir):
    st = json.loads(pathlib.Path(settings_path).read_text(encoding='utf-8'))
    sec = st['sections']
    size, cols, rows = sec['size'], sec['columns'], sec['rows']
    ox, oz, floor = sec['x0'], sec['z0'], sec['y0']
    w, d = size * len(cols), size * rows
    out = pathlib.Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    solid = np.full((d, w), NONE, dtype=np.int32)
    water = solid.copy()
    tiles, digest, height, floors = [], hashlib.sha256(), None, {}
    for r in range(rows):
        for c, letter in enumerate(cols):
            name = f"{sec['prefix']}{letter}{r + 1}"
            s = Structure.load(pathlib.Path(sections_dir) / f'{name}.nbt')
            sx, sy, sz = s.size
            if (sx, sz) != (size, size):
                raise SystemExit(f'{name}: {sx} x {sz}, expected {size} x {size}')
            height = height or sy
            if sy != height:
                raise SystemExit(f'{name}: {sy} high, the others {height}')
            stack = np.full((sz, sx, sy), -1, dtype=np.int16)  # [z, x, y]
            used = {}
            for (x, y, z), idx, _ in s.blocks:
                state = s.palette[idx]
                if block_name(state) in ('minecraft:air', 'minecraft:structure_void'):
                    continue
                stack[z, x, y] = used.setdefault(state, len(used))
            palette = list(used)
            is_ground = np.array([ground_state(p) for p in palette] + [False])
            is_water = np.array([water_state(p) for p in palette] + [False])
            for grid, mask in ((solid, is_ground[stack]), (water, is_water[stack])):
                hit = mask.any(axis=2)
                top = sy - 1 - np.argmax(mask[:, :, ::-1], axis=2)
                grid[r * size:(r + 1) * size, c * size:(c + 1) * size] = np.where(hit, floor + top, NONE)
            for (z, x), ys in floors_of(stack, palette, floor).items():
                floors[(r * size + z, c * size + x)] = ys
            pack_tile(out / f'{name}.stk', (sx, sy, sz), palette, stack, anchors_of(s.blocks, s.palette))
            digest.update((out / f'{name}.stk').read_bytes())
            tiles.append({'file': f'{name}.stk', 'format': 'stack', 'x': c * size, 'z': r * size, 'size': [sx, sy, sz]})

    def cell(o):
        return min(w - 1, max(0, int(math.floor(o['x'] - ox)))), min(d - 1, max(0, int(math.floor(o['z'] - oz))))

    def stand(o):
        cx, cz = cell(o)
        return int(solid[cz, cx]) + 1

    points = [{'id': p['id'], 'name': p['name'], 'x': p['x'] - ox, 'y': stand(p), 'z': p['z'] - oz, 'radius': p['radius']}
              for p in st['points']]
    spawns = [{'id': s['id'], 'team': s['team'], 'x': s['x'] - ox, 'y': stand(s), 'z': s['z'] - oz} for s in st['spawns']]
    routes = []
    for s in st['spawns']:
        dist = floor_distances(floors, w, d, cell(s), stand(s) - 1)
        for p in st['points']:
            cx, cz = cell(p)
            walk = min([dist[(cx, cz, y)] for y in floors.get((cz, cx), []) if (cx, cz, y) in dist], default=-1)
            routes.append({'spawn': s['id'], 'point': p['name'], 'straight': round(math.hypot(p['x'] - s['x'], p['z'] - s['z']), 2),
                           'walk': walk if walk >= 0 else None})
    (out / 'columns.bin.gz').write_bytes(gzip.compress(np.stack([solid, water]).astype('<i2').tobytes(), mtime=0))
    digest.update(json.dumps(st, sort_keys=True).encode('utf-8'))
    manifest = {'format': 1, 'id': st['id'], 'width': w, 'depth': d, 'origin': [ox, oz], 'baseY': st['baseY'], 'floorY': floor,
                'columns': 'columns.bin.gz', 'field': st['field'], 'tiles': tiles, 'points': points, 'spawns': spawns,
                'roads': [], 'routes': routes, 'routesBy': 'floors', 'design_sha256': digest.hexdigest()}
    (out / 'map.json').write_bytes((json.dumps(manifest, sort_keys=True, indent=1) + '\n').encode('utf-8'))
    write_nav(out / NAV, floors, w, d, floor, manifest['design_sha256'])
    for p in points:
        print(f"point {p['id']} {p['name']}: y {p['y']}")
    for s in spawns:
        print(f"spawn {s['id']}: y {s['y']}")
    for r in routes:
        print(f"route {r['spawn']} -> {r['point']}: walk {r['walk']}, straight {r['straight']}")
    print(f'{len(tiles)} tiles, {sum(f.stat().st_size for f in out.iterdir()) // 1024} KiB in {out}')


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[1] == 'nav':
        bake_nav(sys.argv[2])
    elif len(sys.argv) != 4:
        raise SystemExit(__doc__)
    else:
        main(*sys.argv[1:])
