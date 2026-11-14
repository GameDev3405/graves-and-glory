"""Prop anchors for map generators (docs/props/ANCHOR-NBT.md).

A generator that writes .nbt structure templates directly only has to write one block per prop: the anchor command block.
`Props.scanBox` (run by `Structures.place` and on chunk load) then restores the barrier and light cells (into air only) and
spawns the display parts. This module computes that block.

    from prop_anchor import anchor
    a = anchor('supply_wagon', (10, 4, 7), 90, 'minecraft:stone', 3)
    a.pos     # (10, 1, 7): where the command block goes (anchor cell x, y - depth, z)
    a.state   # 'minecraft:command_block[conditional=false,facing=east]'
    a.nbt     # {'id': 'minecraft:command_block', 'Command': 'gravesandglory:prop/supply_wagon depth=3 floor=minecraft:stone', 'auto': 0, ...}
    a.solid   # [(x, y, z), ...] the rotated solid cells (barrier blocks) in the same frame as anchor_cell
    a.lights  # [(x, y, z, level), ...]

Pure: JDK-free, stdlib only, no I/O when `definition` is given; otherwise it reads the def from
src/main/resources/data/gravesandglory/gg_props/<id>.json. The maths is gg.gravesandglory.PropGeometry (facing 0, 90, 180, 270
clockwise seen from above; (dx, dz) -> (-dz, dx) per 90 degrees; x east, y up, z south).
"""
import json
import pathlib
import re
from collections import namedtuple

REPO = pathlib.Path(__file__).resolve().parents[1]
DEFS = REPO / 'src' / 'main' / 'resources' / 'data' / 'gravesandglory' / 'gg_props'

ANCHOR_PREFIX = 'gravesandglory:prop/'
DIRECTIONS = {0: 'north', 90: 'east', 180: 'south', 270: 'west'}
MAX_DEPTH = 12          # Props.MAX_DEPTH
MIN_HIDDEN_DEPTH = 2    # Props.hiddenBase starts at 2; depth 1 is the fallback (the anchor shows)

Anchor = namedtuple('Anchor', 'pos state nbt solid lights')

_STATE = re.compile(r'^(?:([a-z0-9_.\-]+):)?([a-z0-9_./\-]+)(?:\[(.*)\])?$')


def load_def(prop_id):
    """The def dict of a prop id (gg_props/<id>.json)."""
    return json.loads((DEFS / (prop_id + '.json')).read_text(encoding='utf8'))


def normalize_state(text):
    """'stone_slab[waterlogged=false,type=bottom]' -> 'minecraft:stone_slab[type=bottom,waterlogged=false]': namespaced, properties
    sorted, the form the game writes into the anchor's floor= field."""
    m = _STATE.match(text.strip())
    if not m:
        raise ValueError('not a block state: %r' % text)
    props = m.group(3)
    pairs = sorted(kv.strip().split('=', 1) for kv in props.split(',') if kv.strip()) if props else []
    if any(len(p) != 2 for p in pairs):
        raise ValueError('not a block state: %r' % text)
    return '%s:%s' % (m.group(1) or 'minecraft', m.group(2)) + ('[' + ','.join('%s=%s' % (k, v) for k, v in pairs) + ']' if pairs else '')


def turn(facing, dx, dz):
    """(dx, dz) turned clockwise by facing degrees (Structures.turn)."""
    if facing == 0: return dx, dz
    if facing == 90: return -dz, dx
    if facing == 180: return -dx, -dz
    if facing == 270: return dz, -dx
    raise ValueError('facing must be 0, 90, 180 or 270, not %r' % (facing,))


def pivot(size):
    """The anchor cell of a prop in its own cells: ((sx-1)//2, 0, (sz-1)//2) (Structures.pivot, PropGeometry.cell)."""
    return (size[0] - 1) // 2, 0, (size[2] - 1) // 2


def cell(size, anchor_cell, facing, local):
    """World cell of the prop's cell `local` (x, y, z) when its anchor cell is `anchor_cell` and it faces `facing`."""
    px, _, pz = pivot(size)
    dx, dz = turn(facing, local[0] - px, local[2] - pz)
    return anchor_cell[0] + dx, anchor_cell[1] + local[1], anchor_cell[2] + dz


def anchor_command(prop_id, depth, floor_state):
    """The anchor's Command text: gravesandglory:prop/<id> depth=<n> floor=<block state>."""
    return '%s%s depth=%d floor=%s' % (ANCHOR_PREFIX, prop_id, depth, normalize_state(floor_state))


def anchor(prop_id, anchor_cell, facing, floor_state, depth, definition=None):
    """Everything a generator writes for one prop.

    prop_id      id of the def (gg_props/<id>.json)
    anchor_cell  (x, y, z) of the prop's anchor cell, the cell the prop's pivot lands on (its ground-layer cell, y of the prop's floor level)
    facing       0, 90, 180 or 270 (clockwise from above; 0 = front to the north)
    floor_state  the block state the anchor command block replaces, e.g. 'minecraft:stone' (put back when the prop is removed)
    depth        blocks under the anchor cell: 2..12 when that cell is buried (see the doc), 1 as the fallback
    definition   the def dict, if the caller has it; otherwise it is read from disk

    Returns Anchor(pos, state, nbt, solid, lights): the command block's cell, its block state string, its block-entity NBT as a plain
    dict (ints stay ints; `auto` is a byte 0), the rotated solid cells and the rotated light cells (x, y, z, level).
    """
    if facing not in DIRECTIONS:
        raise ValueError('facing must be 0, 90, 180 or 270, not %r' % (facing,))
    if not 1 <= depth <= MAX_DEPTH:
        raise ValueError('depth must be 1..%d, not %r' % (MAX_DEPTH, depth))
    d = definition or load_def(prop_id)
    if d['id'] != prop_id:
        raise ValueError('def id %r is not %r' % (d['id'], prop_id))
    ax, ay, az = anchor_cell
    solid = [cell(d['size'], anchor_cell, facing, c) for c in d['solid']]
    lights = [cell(d['size'], anchor_cell, facing, (l['x'], l['y'], l['z'])) + (l['level'],) for l in d.get('lights', [])]
    nbt = {
        'id': 'minecraft:command_block',
        'Command': anchor_command(prop_id, depth, floor_state),
        'auto': 0,                 # byte: never run
        'conditionMet': 0, 'powered': 0, 'SuccessCount': 0, 'TrackOutput': 0, 'UpdateLastExecution': 1, 'LastOutput': '',
    }
    state = 'minecraft:command_block[conditional=false,facing=%s]' % DIRECTIONS[facing]
    return Anchor((ax, ay - depth, az), state, nbt, solid, lights)


def footprint_cells(prop_id, anchor_cell, facing, definition=None):
    """Every cell of the prop's solid and light cells (the cells that must be air or barrier/light when the prop stands)."""
    a = anchor(prop_id, anchor_cell, facing, 'minecraft:stone', 1, definition)
    return a.solid + [l[:3] for l in a.lights]


# ---- structure-template output (mapforge) and the example ------------------------------------------------------------------------------

_BYTES = ('auto', 'conditionMet', 'powered', 'TrackOutput', 'UpdateLastExecution')


def nbt_tag(nbt_dict):
    """The anchor's NBT dict as a mapforge compound Tag: the idle-block flags are bytes, SuccessCount an int, the rest strings."""
    sys_path_fix()
    from mapforge import nbt
    out = {}
    for k, v in nbt_dict.items():
        out[k] = nbt.byte(v) if k in _BYTES else nbt.int_(v) if isinstance(v, int) else nbt.string(v)
    return nbt.compound(out)


def sys_path_fix():
    import sys
    t = str(pathlib.Path(__file__).resolve().parent)
    if t not in sys.path:
        sys.path.insert(0, t)


def example_structure(prop_id='supply_wagon', floor='minecraft:stone', layers=4, depth=2, spacing=8):
    """One `prop_id` at each facing (0, 90, 180, 270) in a row on a floor of `layers` full layers of `floor`, the anchors buried at `depth`.
    The template is (4*spacing) x (layers + sy) x spacing; the wagons' anchor cells are on layer `layers`, in the middle of their 8x8 bays."""
    sys_path_fix()
    from mapforge.structure import Structure
    d = load_def(prop_id)
    mid = spacing // 2
    size = (4 * spacing, layers + d['size'][1], spacing)
    palette = [floor, 'minecraft:command_block[conditional=false,facing=north]']
    blocks = {}
    for y in range(layers):
        for x in range(size[0]):
            for z in range(size[2]):
                blocks[(x, y, z)] = (0, None)
    for i, facing in enumerate((0, 90, 180, 270)):
        a = anchor(prop_id, (i * spacing + mid, layers, mid), facing, floor, depth, d)
        assert all(0 <= c[0] < size[0] and 0 <= c[2] < size[2] and c[1] < size[1] for c in a.solid), 'prop leaves its bay'
        if a.state not in palette:
            palette.append(a.state)
        blocks[a.pos] = (palette.index(a.state), nbt_tag(a.nbt))
    return Structure(size, palette, [(p, i, t) for p, (i, t) in sorted(blocks.items())])


if __name__ == '__main__':
    import sys
    if sys.argv[1:] == ['--example']:
        out = REPO / 'docs' / 'props' / 'examples' / 'prop_facings.nbt'
        out.parent.mkdir(parents=True, exist_ok=True)
        s = example_structure()
        s.save(out)
        print('wrote', out, s.size)
    else:
        sys.exit('usage: python tools/prop_anchor.py --example   (or import anchor() from a generator)')
