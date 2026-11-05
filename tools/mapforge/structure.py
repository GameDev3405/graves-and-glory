"""Structure template files (the vanilla structure-block format used in src/main/resources/data/gravesandglory/structures),
block state strings, and placement maths that matches gg.gravesandglory.Structures (pivot, turn, toWorld, footprint)."""
import pathlib
import re

from . import nbt

DATA_VERSION = 3465  # Minecraft 1.20.1, as tools/preflight.py STRUCTURE_DATA_VERSION
AIR = 'minecraft:air'
VOID = 'minecraft:structure_void'
REPO = pathlib.Path(__file__).resolve().parents[2]
PIECES = REPO / 'src/main/resources/data/gravesandglory/structures'

_STATE = re.compile(r'^([a-z0-9_.\-]+:)?([a-z0-9_./\-]+)(\[([a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*)?\])?$')


def parse_state(text):
    """'oak_stairs[half=bottom,facing=east]' -> ('minecraft:oak_stairs', {'facing': 'east', 'half': 'bottom'})."""
    m = _STATE.match(text.strip())
    if not m:
        raise ValueError(f'not a block state: {text!r}')
    name = (m.group(1) or 'minecraft:') + m.group(2)
    props = dict(kv.split('=') for kv in m.group(4).split(',')) if m.group(4) else {}
    return name, props


def state_string(name, props):
    return name + ('[' + ','.join(f'{k}={props[k]}' for k in sorted(props)) + ']' if props else '')


def normalize(text):
    """Canonical state string: namespaced, properties sorted."""
    return state_string(*parse_state(text))


def block_name(state):
    return state.split('[', 1)[0]


# Rotation of block states, clockwise steps of 90 degrees (BlockRotation.CLOCKWISE_90 turns north to east).
_H = ('north', 'east', 'south', 'west')
_RAIL = {'north_south': 'east_west', 'east_west': 'north_south',
         'ascending_north': 'ascending_east', 'ascending_east': 'ascending_south',
         'ascending_south': 'ascending_west', 'ascending_west': 'ascending_north',
         'north_east': 'south_east', 'south_east': 'south_west', 'south_west': 'north_west', 'north_west': 'north_east'}


def rotate_state(state, rotation):
    """The state turned by rotation degrees: facing, axis, rotation (0-15), rail shape and the four side properties
    (fences, walls, panes, redstone). Any other property is kept as it is."""
    steps = (rotation // 90) % 4
    if not steps:
        return state
    name, props = parse_state(state)
    out = dict(props)
    for _ in range(steps):
        cur = dict(out)
        if cur.get('facing') in _H:
            out['facing'] = _H[(_H.index(cur['facing']) + 1) % 4]
        if cur.get('axis') in ('x', 'z'):
            out['axis'] = 'z' if cur['axis'] == 'x' else 'x'
        if 'rotation' in cur and cur['rotation'].isdigit():
            out['rotation'] = str((int(cur['rotation']) + 4) % 16)
        if cur.get('shape') in _RAIL:
            out['shape'] = _RAIL[cur['shape']]
        sides = {side: cur[side] for side in _H if side in cur}
        for side in sides:
            del out[side]
        for side, value in sides.items():
            out[_H[(_H.index(side) + 1) % 4]] = value
    return state_string(name, out)


def pivot(size):
    """Centre block of a piece in its own cells: middle column of the footprint, ground layer (Structures.pivot)."""
    return ((size[0] - 1) // 2, 0, (size[2] - 1) // 2)


def turn(rotation, dx, dz):
    if rotation == 0:
        return dx, dz
    if rotation == 90:
        return -dz, dx
    if rotation == 180:
        return -dx, -dz
    if rotation == 270:
        return dz, -dx
    raise ValueError(f'rotation must be 0, 90, 180 or 270, not {rotation}')


def to_world(size, at, rotation, local):
    """World cell of local cell (lx,ly,lz) when the centre block sits at at=(cx,y,cz) (Structures.toWorld)."""
    p = pivot(size)
    dx, dz = turn(rotation, local[0] - p[0], local[2] - p[2])
    return (at[0] + dx, at[1] + local[1], at[2] + dz)


def footprint(size, at, rotation):
    """(min, max) world corners of the placed box (Structures.footprint)."""
    a = to_world(size, at, rotation, (0, 0, 0))
    b = to_world(size, at, rotation, (size[0] - 1, size[1] - 1, size[2] - 1))
    return tuple(map(min, a, b)), tuple(map(max, a, b))


class Structure:
    """size (x, y, z); palette: state strings; blocks: list of (pos, palette index, block-entity Tag or None).
    Cells without a block, and structure_void cells, keep what is in the world. Entities and unknown root tags are
    kept as read but are not placed by mapforge."""

    def __init__(self, size, palette=None, blocks=None, data_version=DATA_VERSION, entities=None, extra=None):
        self.size = tuple(size)
        self.palette = list(palette or [])
        self.blocks = list(blocks or [])
        self.data_version = data_version
        self.entities = entities if entities is not None else nbt.list_(nbt.END, [])
        self.extra = dict(extra or {})

    @classmethod
    def from_tag(cls, root):
        size = tuple(t.value for t in root['size'].value)
        pal_tag = root.get('palette') or (root['palettes'].value[0] if root.get('palettes') else None)
        if pal_tag is None:
            raise ValueError('structure has no palette')
        palette = []
        for entry in pal_tag.value:
            props = entry.get('Properties')
            palette.append(state_string(entry['Name'].value, {k: v.value for k, v in props.value.items()} if props else {}))
        blocks = []
        for b in root['blocks'].value:
            pos = tuple(t.value for t in b['pos'].value)
            if not all(0 <= pos[i] < size[i] for i in range(3)):
                raise ValueError(f'block at {pos} is outside the size {size}')
            blocks.append((pos, b['state'].value, b.get('nbt')))
        dv = root.get('DataVersion')
        extra = {k: v for k, v in root.value.items() if k not in ('size', 'palette', 'palettes', 'blocks', 'entities', 'DataVersion')}
        return cls(size, palette, blocks, dv.value if dv else None, root.get('entities'), extra)

    @classmethod
    def load(cls, path):
        return cls.from_tag(nbt.read_file(path)[1])

    def to_tag(self):
        palette = []
        for state in self.palette:
            name, props = parse_state(state)
            entry = {'Name': nbt.string(name)}
            if props:
                entry['Properties'] = nbt.compound({k: nbt.string(v) for k, v in sorted(props.items())})
            palette.append(nbt.compound(entry))
        blocks = []
        for pos, idx, be in self.blocks:
            entry = {'pos': nbt.list_(nbt.INT, [nbt.int_(c) for c in pos]), 'state': nbt.int_(idx)}
            if be is not None:
                entry['nbt'] = be
            blocks.append(nbt.compound(entry))
        root = {'size': nbt.list_(nbt.INT, [nbt.int_(c) for c in self.size]), 'entities': self.entities,
                'blocks': nbt.list_(nbt.COMPOUND, blocks), 'palette': nbt.list_(nbt.COMPOUND, palette)}
        root.update(self.extra)
        if self.data_version is not None:
            root['DataVersion'] = nbt.int_(self.data_version)
        return nbt.compound(root)

    def save(self, path):
        nbt.write_file(path, self.to_tag())

    def cells(self):
        """(local pos, state, block-entity Tag or None), structure_void left out."""
        for pos, idx, be in self.blocks:
            state = self.palette[idx]
            if block_name(state) != VOID:
                yield pos, state, be


def resolve_piece(piece_id):
    """Path of a piece id such as camp/archive in the mod's structure folder."""
    if not re.fullmatch(r'[a-z0-9_]+(/[a-z0-9_]+)+', piece_id):
        raise ValueError(f'piece id {piece_id!r} is not <group>/<name> in [a-z0-9_/]')
    return PIECES / (piece_id + '.nbt')
