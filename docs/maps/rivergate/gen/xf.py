"""Turning and mirroring block cells with their states (facing, axis, side connections, banner rotation).
turn(cells, q): q quarter turns clockwise seen from above (north -> east). mirror(cells, 'x'|'z'): x -> -x or z -> -z.
put(dst, cells, ox, oz): copy with an offset. Cells are {(x, y, z): 'name[props]'}."""
CW = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}
MX = {'east': 'west', 'west': 'east', 'north': 'north', 'south': 'south'}
MZ = {'north': 'south', 'south': 'north', 'east': 'east', 'west': 'west'}


def _state(blk, dirmap, swap_axis, rot, flip=False):
    if '[' not in blk: return blk
    name, props = blk[:-1].split('[')
    out = []
    for kv in props.split(','):
        k, v = kv.split('=')
        if k == 'facing' and v in dirmap: v = dirmap[v]
        elif k in dirmap: k = dirmap[k]
        elif k == 'axis' and swap_axis and v in 'xz': v = 'z' if v == 'x' else 'x'
        elif k == 'rotation': v = str(rot(int(v)))
        elif flip and k in ('shape', 'hinge') and ('left' in v or 'right' in v): v = v.replace('left', '#').replace('right', 'left').replace('#', 'right')
        out.append(k + '=' + v)
    return name + '[' + ','.join(sorted(out)) + ']'


def turn(cells, q):
    q %= 4
    for _ in range(q):
        cells = {(-z, y, x): _state(b, CW, True, lambda r: (r + 4) % 16) for (x, y, z), b in cells.items()}
    return cells


def mirror(cells, axis, shift=0):
    """x -> shift - x (or z); shift -1 is the Rivergate map mirror, which keeps x -8..7 in place"""
    if axis == 'x': return {(shift - x, y, z): _state(b, MX, False, lambda r: (16 - r) % 16, True) for (x, y, z), b in cells.items()}
    return {(x, y, shift - z): _state(b, MZ, False, lambda r: (8 - r) % 16, True) for (x, y, z), b in cells.items()}


def put(dst, cells, ox=0, oz=0, oy=0):
    for (x, y, z), b in cells.items(): dst[(x + ox, y + oy, z + oz)] = b
