"""Signature building library: an index of the mod's approved structure pieces (id, footprint, door side, group),
generated from the .nbt files in src/main/resources/data/gravesandglory/structures. Never hand-edit library.json;
correct a door guess in library_overrides.json and rerun `python tools/mapengine library`."""
import functools
import hashlib
import json
import pathlib
from collections import deque

from mapforge.structure import AIR, PIECES, VOID, Structure, block_name

HERE = pathlib.Path(__file__).resolve().parent
INDEX_PATH = HERE / 'library.json'
OVERRIDES_PATH = HERE / 'library_overrides.json'
SIDES = ('north', 'east', 'south', 'west')  # clockwise, as rotation turns them
DOORS = SIDES + ('open', 'none')
WALK = (1, 2)  # local layers a player stands in, above the floor layer 0
ROOF = 3       # a block at this layer or higher makes the column roofed
EAVE = 3       # roofed cells nearer the edge than this are overhangs, not an interior

# Blocks a player walks through (doors, plants, carpets, wall fittings). Anything else counts as a wall.
_PASS_SUFFIX = ('_door', '_carpet', '_sign', '_banner', 'torch', '_button', '_pressure_plate', 'rail', '_sapling',
                '_tulip', 'flower', '_mushroom', 'lantern', '_fern', 'grass', 'vine', '_coral_fan', 'cobweb')
_PASS = {AIR, VOID, 'minecraft:cave_air', 'minecraft:snow', 'minecraft:ladder', 'minecraft:dead_bush',
         'minecraft:dandelion', 'minecraft:poppy', 'minecraft:allium', 'minecraft:azure_bluet', 'minecraft:oxeye_daisy',
         'minecraft:cornflower', 'minecraft:lily_of_the_valley', 'minecraft:wither_rose', 'minecraft:blue_orchid',
         'minecraft:lever', 'minecraft:tripwire', 'minecraft:light', 'minecraft:fire', 'minecraft:water'}


def passable(state):
    n = block_name(state)
    return n in _PASS or n.endswith(_PASS_SUFFIX)


def door_of(s):
    """(door side, [lo, hi] span along that side in local cells or None). See the module doc and Plan 4."""
    sx, sy, sz = s.size
    wall = set()
    roofed = set()
    for (x, y, z), state, _ in s.cells():
        if y in WALK and not passable(state):
            wall.add((x, z))
        if y >= ROOF and block_name(state) != AIR:
            roofed.add((x, z))
    inner = [c for c in roofed if c not in wall]
    if not inner:
        return 'open', None
    doors = sorted((x, z) for (x, y, z), state, _ in s.cells() if y == WALK[0] and block_name(state).endswith('_door'))
    if doors:  # a real door: the side its cell is nearest, span over every door on that side
        def near(c):
            d = {'north': c[1], 'south': sz - 1 - c[1], 'west': c[0], 'east': sx - 1 - c[0]}
            return min(SIDES, key=lambda side: (d[side], SIDES.index(side)))
        side = near(doors[0])
        lanes = [c[0] if side in ('north', 'south') else c[1] for c in doors if near(c) == side]
        return side, [min(lanes), max(lanes)]
    side_of, lane_of, depth = {}, {}, {}
    q = deque()
    for x in range(1, sx - 1):
        for z, side in ((0, 'north'), (sz - 1, 'south')):
            if (x, z) not in wall and (x, z) not in side_of:
                side_of[(x, z)], lane_of[(x, z)], depth[(x, z)] = side, x, 0
                q.append((x, z))
    for z in range(1, sz - 1):
        for x, side in ((0, 'west'), (sx - 1, 'east')):
            if (x, z) not in wall and (x, z) not in side_of:
                side_of[(x, z)], lane_of[(x, z)], depth[(x, z)] = side, z, 0
                q.append((x, z))
    while q:
        c = q.popleft()
        for n in ((c[0] + 1, c[1]), (c[0] - 1, c[1]), (c[0], c[1] + 1), (c[0], c[1] - 1)):
            if 0 <= n[0] < sx and 0 <= n[1] < sz and n not in wall and n not in side_of:
                side_of[n], lane_of[n], depth[n] = side_of[c], lane_of[c], depth[c] + 1
                q.append(n)
    count = {side: 0 for side in SIDES}
    lanes = {side: [] for side in SIDES}
    for c in inner:
        if depth.get(c, -1) >= EAVE:  # deeper than an overhang reached from any side
            count[side_of[c]] += 1
            lanes[side_of[c]].append(lane_of[c])
    best = max(SIDES, key=lambda side: (count[side], -SIDES.index(side)))
    if not count[best]:
        return 'none', None
    return best, [min(lanes[best]), max(lanes[best])]


def scan(path, piece_id):
    raw = pathlib.Path(path).read_bytes()
    s = Structure.load(path)
    door, span = door_of(s)
    return {'id': piece_id, 'group': piece_id.split('/', 1)[0], 'size': list(s.size), 'footprint': [s.size[0], s.size[2]],
            'door': door, 'doorSpan': span, 'blocks': len(s.blocks),
            'blockEntities': sum(1 for b in s.blocks if b[2] is not None), 'sha256': hashlib.sha256(raw).hexdigest()}


def generate(pieces=PIECES, overrides_path=OVERRIDES_PATH):
    over = json.loads(pathlib.Path(overrides_path).read_text(encoding='utf-8')) if pathlib.Path(overrides_path).exists() else {}
    out = {}
    for p in sorted(pathlib.Path(pieces).rglob('*.nbt'), key=lambda p: p.relative_to(pieces).as_posix()):
        pid = p.relative_to(pieces).with_suffix('').as_posix()
        e = scan(p, pid)
        if pid in over:
            e.update({k: over[pid][k] for k in ('door', 'doorSpan') if k in over[pid]})
            e['doorFrom'] = 'override'
        out[pid] = e
    return out


def index_bytes(index):
    return (json.dumps(index, sort_keys=True, indent=1) + '\n').encode('utf-8')


@functools.lru_cache(maxsize=None)
def _load(path, mtime):
    return json.loads(pathlib.Path(path).read_text(encoding='utf-8'))


def load(path=INDEX_PATH):
    p = pathlib.Path(path)
    return _load(str(p), p.stat().st_mtime_ns)


def piece_path(piece_id, pieces=PIECES):
    return pathlib.Path(pieces) / (piece_id + '.nbt')


@functools.lru_cache(maxsize=64)
def piece(piece_id, sha=None):
    """The Structure of a library piece (cached; sha keys the cache to the file's contents)."""
    return Structure.load(piece_path(piece_id))
