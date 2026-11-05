"""Building placements: footprints, door fronts, the levelled pad, design checks and the block stamp.

A building object is {kind: building, piece: <library id>, x, z, rotation}. (x, z) is the piece's centre block
(Structures.pivot) and rotation turns it clockwise about that block, as Structures.place does in the mod, so the
maths here is mapforge.structure's, which mirrors the mod."""
import numpy as np

from mapengine import library
from mapforge.structure import footprint as _footprint
from mapforge.structure import rotate_state, to_world

NO_WATER = -32768  # as terrain.NO_WATER
MAX_STEP = 1       # as walk.MAX_STEP: a door front this far above or below the floor is still walkable


def of(d):
    return [o for o in d.get('objects', []) if o.get('kind') == 'building']


def entry(o, lib=None):
    return (lib or library.load())[o['piece']]


def box(o, lib=None):
    """(x0, z0, x1, z1), inclusive canvas cells under the placed piece."""
    size = entry(o, lib)['size']
    lo, hi = _footprint(size, (o['x'], 0, o['z']), o['rotation'])
    return lo[0], lo[2], hi[0], hi[2]


def door(o, lib=None):
    """(side after rotation, or 'open' / 'none'; canvas cells just outside the door opening)."""
    e = entry(o, lib)
    side = e['door']
    if side not in library.SIDES or not e.get('doorSpan'):
        return side, []
    sx, _, sz = e['size']
    lo, hi = e['doorSpan']
    local = {'north': [(i, -1) for i in range(lo, hi + 1)], 'south': [(i, sz) for i in range(lo, hi + 1)],
             'west': [(-1, i) for i in range(lo, hi + 1)], 'east': [(sx, i) for i in range(lo, hi + 1)]}[side]
    cells = []
    for lx, lz in local:
        wx, _, wz = to_world(e['size'], (o['x'], 0, o['z']), o['rotation'], (lx, 0, lz))
        cells.append((wx, wz))
    turned = library.SIDES[(library.SIDES.index(side) + o['rotation'] // 90) % 4]
    return turned, cells


def _clip(b, width, depth):
    x0, z0, x1, z1 = b
    return max(0, x0), max(0, z0), min(width - 1, x1), min(depth - 1, z1)


def pad(d, h, water):
    """Levels each footprint, in design order, to the ground under its centre block and drains water there.
    h and water are the heightfield's [z, x] arrays, changed in place."""
    width, depth = d['width'], d['depth']
    for o in of(d):
        try:
            b = box(o)
        except KeyError:
            continue  # unknown piece: validate reports it
        x0, z0, x1, z1 = _clip(b, width, depth)
        if x1 < x0 or z1 < z0:
            continue
        h[z0:z1 + 1, x0:x1 + 1] = h[o['z'], o['x']]
        water[z0:z1 + 1, x0:x1 + 1] = NO_WATER


def issues(d, f):
    """[(code, object id, message)] for buildings against the design and its (padded) heightfield f."""
    width, depth = d['width'], d['depth']
    lib = library.load()
    bs = [o for o in of(d) if o.get('piece') in lib]
    boxes = {o['id']: box(o, lib) for o in bs}
    out = []
    for o in bs:
        x0, z0, x1, z1 = boxes[o['id']]
        if x0 < 0 or z0 < 0 or x1 >= width or z1 >= depth:
            out.append(('building_bounds', o['id'], f"{o['id']}: {o['piece']} reaches x {x0}..{x1}, z {z0}..{z1}, "
                        f"off the {width} by {depth} canvas"))
    for i, a in enumerate(bs):
        for b in bs[:i]:
            p, q = boxes[a['id']], boxes[b['id']]
            if p[0] <= q[2] and q[0] <= p[2] and p[1] <= q[3] and q[1] <= p[3]:
                out.append(('building_overlap', a['id'], f"{a['id']}: {a['piece']} overlaps {b['id']} ({b['piece']})"))

    def inside_any(x, z, skip=None):
        return any(k != skip and p[0] <= x <= p[2] and p[1] <= z <= p[3] for k, p in boxes.items())

    for o in bs:
        side, cells = door(o, lib)
        if not cells:
            continue
        floor = int(f.height[o['z'], o['x']])
        ok = 0
        for x, z in cells:
            if not (0 <= x < width and 0 <= z < depth) or inside_any(x, z):
                continue
            wet = f.water[z, x] != NO_WATER and int(f.water[z, x]) > int(f.height[z, x])
            if not wet and abs(int(f.height[z, x]) - floor) <= MAX_STEP:
                ok += 1
        if not ok:
            out.append(('door_blocked', o['id'], f"{o['id']}: the {side} door of {o['piece']} opens onto no walkable "
                        f"ground (off the canvas, water, another building or a step of more than {MAX_STEP})"))
    for o in d.get('objects', []):
        if o.get('kind') in ('point', 'spawn'):
            x, z = int(o['x']), int(o['z'])
            for k, p in boxes.items():
                if p[0] <= x <= p[2] and p[1] <= z <= p[3]:
                    out.append(('building_point', o['id'], f"{o['id']} stands inside the footprint of {k}"))
    return out


def stamps(d, f, states):
    """[(xs, zs, layer, state ids)] per building in design order: canvas cells (clipped to the canvas), height
    relative to baseY, and indices into states (extended in place with the rotated piece states). Air and structure
    void are left out, so the terrain stays there. Layer 0 of the piece replaces the pad's surface block."""
    width, depth = d['width'], d['depth']
    lib = library.load()
    index = {s: i for i, s in enumerate(states)}
    out = []
    for o in of(d):
        e = lib[o['piece']]
        s = library.piece(o['piece'], e['sha256'])
        floor = int(f.height[o['z'], o['x']])
        xs, zs, ys, ids = [], [], [], []
        for (lx, ly, lz), state, _ in s.cells():
            if state == 'minecraft:air':
                continue
            wx, _, wz = to_world(s.size, (o['x'], 0, o['z']), o['rotation'], (lx, 0, lz))
            if not (0 <= wx < width and 0 <= wz < depth):
                continue
            st = rotate_state(state, o['rotation'])
            if st not in index:
                index[st] = len(states)
                states.append(st)
            xs.append(wx)
            zs.append(wz)
            ys.append(floor + ly)
            ids.append(index[st])
        out.append((np.array(xs, dtype=np.int32), np.array(zs, dtype=np.int32), np.array(ys, dtype=np.int32),
                    np.array(ids, dtype=np.int16)))
    return out
