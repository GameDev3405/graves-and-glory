"""Map Forge terrain helpers: seeded heightfield noise, erosion and smoothing, surface and edge breakup, polyline
walls, paths and river channels. Each helper is a plan operation; it reads the plan's overlay (so it sees earlier
operations) and writes cells through the same path as every other operation, so region locks, protect locks, limits,
dry-run counts, apply and rollback treat it exactly like fill or terrain. Everything is deterministic: the same op on
the same world gives the same cells on every machine (integer hashing, no random module).

Shared keys (see README.md for each op):
  min, max     [x, z] inclusive clip box (required for noise, erode and breakup; optional for the polyline ops)
  exclude      list of {"min": [x, z], "max": [x, z]} boxes the op leaves alone
  only         surface block names the op may change (columns whose ground top is another block are skipped)
  scan         [ylo, yhi] range searched for a column's ground top (default [-64, 160]); air and water are not ground
  seed         int or string (default 0)
A block spec is a state string or a weighted mix [[state, weight], ...]."""
import hashlib
import math

from .structure import AIR, block_name, normalize
from .world import AIR_CELL, Cell

HELPERS = ('noise', 'erode', 'breakup', 'wall', 'path', 'river')
NOT_GROUND = {AIR, 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:water', 'minecraft:lava'}
WATER = Cell('minecraft:water')
DEFAULT_SCAN = (-64, 160)
MASK = (1 << 64) - 1

COMMON = {'op', 'note', 'min', 'max', 'exclude', 'only', 'scan', 'seed'}
KEYS = {
    'noise': COMMON | {'amplitude', 'scale', 'octaves', 'persistence', 'offset', 'falloff', 'clamp'},
    'erode': COMMON | {'mode', 'iterations', 'talus', 'rate', 'jitter', 'clamp'},
    'breakup': COMMON | {'replace', 'mix', 'density', 'scale', 'near', 'width', 'depth', 'exposed'},
    'wall': COMMON | {'points', 'thickness', 'top', 'floor', 'block', 'closed', 'merlons'},
    'path': COMMON | {'points', 'width', 'falloff', 'y', 'surface', 'under', 'edge', 'edgeDensity', 'wobble',
                      'wobbleScale'},
    'river': COMMON | {'points', 'width', 'depth', 'water', 'floor', 'bank', 'bankSurface', 'bed', 'wobble',
                       'wobbleScale', 'faces'},
}
REQUIRED = {
    'noise': ('min', 'max', 'amplitude', 'scale'),
    'erode': ('min', 'max'),
    'breakup': ('min', 'max', 'replace', 'mix'),
    'wall': ('points', 'floor', 'block'),
    'path': ('points', 'width', 'surface'),
    'river': ('points', 'width', 'water', 'floor'),
}


# ---------------------------------------------------------------- validation

def _num(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def _int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def _xz(v):
    return isinstance(v, list) and len(v) == 2 and all(_int(c) for c in v)


def _spec_errors(where, spec, errors):
    if isinstance(spec, str):
        try:
            normalize(spec)
        except (ValueError, AttributeError):
            errors.append(f'{where}: {spec!r} is not a block state')
        return
    if not isinstance(spec, list) or not spec or not all(
            isinstance(e, list) and len(e) == 2 and isinstance(e[0], str) and _num(e[1]) and e[1] > 0 for e in spec):
        errors.append(f'{where}: give a block state or a mix [[state, weight > 0], ...]')
        return
    for e in spec:
        _spec_errors(where, e[0], errors)


def validate_helper(where, op, errors):
    kind = op['op']
    for key in sorted(set(op) - KEYS[kind]):
        errors.append(f'{where}: unknown key {key!r} for {kind}')
    for key in REQUIRED[kind]:
        if key not in op:
            errors.append(f'{where}: {kind} needs {key}')
    for key in ('min', 'max'):
        if key in op and not _xz(op[key]):
            errors.append(f'{where}: {key} must be [x, z] integers')
    if ('min' in op) != ('max' in op):
        errors.append(f'{where}: give min and max together')
    elif _xz(op.get('min')) and _xz(op.get('max')) and (op['min'][0] > op['max'][0] or op['min'][1] > op['max'][1]):
        errors.append(f'{where}: min {op["min"]} is above max {op["max"]}')
    ex = op.get('exclude', [])
    if not isinstance(ex, list) or not all(isinstance(b, dict) and _xz(b.get('min')) and _xz(b.get('max')) for b in ex):
        errors.append(f'{where}: exclude must be a list of {{"min": [x, z], "max": [x, z]}} boxes')
    for key in ('only', 'replace', 'near'):
        if key in op and (not isinstance(op[key], list) or not op[key] or not all(isinstance(s, str) for s in op[key])):
            errors.append(f'{where}: {key} must be a non-empty list of block names')
    for key in ('clamp', 'scan'):
        if key in op and not (isinstance(op[key], list) and len(op[key]) == 2 and all(_int(c) for c in op[key])
                              and op[key][0] <= op[key][1]):
            errors.append(f'{where}: {key} must be [low y, high y] integers')
    if 'seed' in op and not (_int(op['seed']) or isinstance(op['seed'], str)):
        errors.append(f'{where}: seed must be an integer or a string')
    positive = {'scale', 'thickness', 'width'}
    for key in ('amplitude', 'scale', 'persistence', 'offset', 'falloff', 'talus', 'rate', 'jitter', 'density',
                'width', 'thickness', 'edgeDensity', 'wobble', 'wobbleScale', 'bank', 'depth'):
        if key in op and not _num(op[key]):
            errors.append(f'{where}: {key} must be a number')
        elif key in op and key != 'offset' and (op[key] <= 0 if key in positive else op[key] < 0):
            errors.append(f'{where}: {key} must be {"positive" if key in positive else "zero or more"}')
    for key in ('rate', 'density', 'edgeDensity', 'jitter', 'persistence'):
        if _num(op.get(key)) and op[key] > 1:
            errors.append(f'{where}: {key} must be at most 1')
    for key in ('octaves', 'iterations'):
        if key in op and not (_int(op[key]) and 1 <= op[key] <= 64):
            errors.append(f'{where}: {key} must be an integer 1..64')
    for key in ('floor', 'water', 'y', 'top'):
        if key in op and not _int(op[key]):
            errors.append(f'{where}: {key} must be an integer y')
    for key in ('closed', 'merlons', 'exposed', 'faces'):
        if key in op and not isinstance(op[key], bool):
            errors.append(f'{where}: {key} must be true or false')
    if 'mode' in op and op['mode'] not in ('thermal', 'smooth'):
        errors.append(f'{where}: mode must be thermal or smooth')
    if kind == 'breakup' and 'near' in op and not _num(op.get('width')):
        errors.append(f'{where}: near needs width (how far from those blocks the breakup reaches)')
    if kind == 'breakup' and 'depth' in op and not (_int(op['depth']) and op['depth'] >= 1):
        errors.append(f'{where}: depth must be an integer of at least 1')
    for key in ('block', 'surface', 'under', 'edge', 'bankSurface', 'bed', 'mix'):
        if key in op:
            _spec_errors(f'{where}.{key}', op[key], errors)
    if kind in ('wall', 'path', 'river'):
        pts = op.get('points')
        extra = {'wall': 'top y', 'path': 'y', 'river': 'width'}[kind]
        if not isinstance(pts, list) or len(pts) < 2 or not all(
                isinstance(p, list) and len(p) in (2, 3) and all(_num(c) for c in p) for p in pts):
            errors.append(f'{where}: points must list at least two [x, z] or [x, z, {extra}] points')
        elif kind == 'wall' and 'top' not in op and not all(len(p) == 3 for p in pts):
            errors.append(f'{where}: give top, or a top y as the third value of every point')
        elif kind == 'river' and any(len(p) == 3 and p[2] <= 0 for p in pts):
            errors.append(f'{where}: a point width must be positive')
        if kind == 'river' and _int(op.get('water')) and _int(op.get('floor')) and op['floor'] > op['water']:
            errors.append(f'{where}: floor is above water')
        if kind == 'wall' and _int(op.get('top')) and _int(op.get('floor')) and op['floor'] > op['top']:
            errors.append(f'{where}: floor is above top')


# ---------------------------------------------------------------- deterministic noise

def seed_of(seed):
    return int.from_bytes(hashlib.sha256(f'mapforge:{seed}'.encode()).digest()[:8], 'big')


def hash01(s, *ks):
    """Float in [0, 1) from a 64-bit seed and integer keys (splitmix64 rounds)."""
    h = s
    for k in ks:
        h = (h ^ (k & MASK)) & MASK
        h = (h + 0x9E3779B97F4A7C15) & MASK
        h = ((h ^ (h >> 30)) * 0xBF58476D1CE4E5B9) & MASK
        h = ((h ^ (h >> 27)) * 0x94D049BB133111EB) & MASK
        h ^= h >> 31
    return h / 2.0 ** 64


def value2(s, x, z, scale):
    """Smooth 2D value noise in [0, 1) with features about `scale` blocks across."""
    fx, fz = x / scale, z / scale
    i, j = math.floor(fx), math.floor(fz)
    tx, tz = fx - i, fz - j
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, b = hash01(s, i, j), hash01(s, i + 1, j)
    c, d = hash01(s, i, j + 1), hash01(s, i + 1, j + 1)
    return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz


def fbm(s, x, z, scale, octaves=3, persistence=0.5):
    """Layered value noise in [-1, 1]: octave k has scale / 2^k and weight persistence^k."""
    total = norm = 0.0
    amp, sc = 1.0, float(scale)
    for k in range(octaves):
        total += amp * (value2(s + k * 7919, x, z, max(sc, 1e-6)) * 2 - 1)
        norm += amp
        amp *= persistence
        sc /= 2
    return total / norm if norm else 0.0


def picker(spec, s):
    """(x, y, z) -> Cell for a block spec: one state, or a weighted mix chosen per block by hash."""
    if isinstance(spec, str):
        cell = Cell(normalize(spec))
        return lambda x, y, z: cell
    cells = [Cell(normalize(b)) for b, _ in spec]
    total = float(sum(w for _, w in spec))
    cum, acc = [], 0.0
    for _, w in spec:
        acc += w / total
        cum.append(acc)

    def pick(x, y, z):
        r = hash01(s, x, y, z, 77)
        for cell, c in zip(cells, cum):
            if r < c:
                return cell
        return cells[-1]
    return pick


# ---------------------------------------------------------------- columns

class Field:
    """Column access over the plan overlay: ground tops and column reshaping."""

    def __init__(self, op, get, put):
        self.get, self.put = get, put
        self.scan = tuple(op.get('scan', DEFAULT_SCAN))
        self.only = {normalize(n).split('[')[0] for n in op['only']} if 'only' in op else None
        self.exclude = [(b['min'][0], b['min'][1], b['max'][0], b['max'][1]) for b in op.get('exclude', [])]
        self.clip = (op['min'][0], op['min'][1], op['max'][0], op['max'][1]) if 'min' in op else None

    def allowed(self, x, z):
        if self.clip and not (self.clip[0] <= x <= self.clip[2] and self.clip[1] <= z <= self.clip[3]):
            return False
        return not any(b[0] <= x <= b[2] and b[1] <= z <= b[3] for b in self.exclude)

    def top(self, x, z):
        get = self.get
        for y in range(self.scan[1], self.scan[0] - 1, -1):
            if block_name(get((x, y, z)).state) not in NOT_GROUND:
                return y
        return None

    def usable(self, x, z, top):
        """The column may be changed: inside the clip, outside the excludes, ground found, surface in only."""
        if top is None or not self.allowed(x, z):
            return False
        return self.only is None or block_name(self.get((x, top, z)).state) in self.only

    def reshape(self, x, z, top, new, surface=None, under=None):
        """Moves the column's ground top from top to new, keeping its surface block (or `surface`) on top and
        filling a raise with the block under the old top (or `under`)."""
        get, put = self.get, self.put
        cap = surface or get((x, top, z))
        if new > top:
            below = get((x, top - 1, z))
            fill = under or (below if block_name(below.state) not in NOT_GROUND else cap)
            for y in range(top, new):
                put((x, y, z), fill)
        else:
            for y in range(new + 1, top + 1):
                put((x, y, z), AIR_CELL)
        put((x, new, z), cap)


def _box_cols(op):
    (x0, z0), (x1, z1) = op['min'], op['max']
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            yield x, z


def _clamp(v, clamp):
    return v if clamp is None else max(clamp[0], min(clamp[1], v))


# ---------------------------------------------------------------- noise

def run_noise(op, get, put):
    f = Field(op, get, put)
    s = seed_of(op.get('seed', 0))
    (x0, z0), (x1, z1) = op['min'], op['max']
    fall = op.get('falloff', 0)
    clamp = op.get('clamp')
    for x, z in _box_cols(op):
        top = f.top(x, z)
        if not f.usable(x, z, top):
            continue
        w = 1.0
        if fall > 0:
            edge = min(x - x0, x1 - x, z - z0, z1 - z) + 1
            w = min(1.0, edge / fall)
            w = w * w * (3 - 2 * w)
        n = fbm(s, x, z, op['scale'], op.get('octaves', 3), op.get('persistence', 0.5))
        new = _clamp(top + round(w * (op.get('offset', 0) + op['amplitude'] * n)), clamp)
        if new != top:
            f.reshape(x, z, top, new)


# ---------------------------------------------------------------- erode

NEIGH = ((1, 0, 1.0), (-1, 0, 1.0), (0, 1, 1.0), (0, -1, 1.0),
         (1, 1, 1.4142), (1, -1, 1.4142), (-1, 1, 1.4142), (-1, -1, 1.4142))


def run_erode(op, get, put):
    """thermal: material slides from a column to a lower neighbour while the drop is steeper than talus (blocks per
    block, jittered per column by `jitter` so slopes do not stair-step evenly); smooth: each pass moves a column
    `rate` of the way to the mean of its neighbours. Columns outside the clip, excluded or filtered by only are
    read as fixed neighbours and never changed."""
    f = Field(op, get, put)
    s = seed_of(op.get('seed', 0))
    (x0, z0), (x1, z1) = op['min'], op['max']
    tops, h, active = {}, {}, set()
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            t = f.top(x, z)
            if t is None:
                continue
            tops[(x, z)] = t
            h[(x, z)] = float(t)
            if x0 <= x <= x1 and z0 <= z <= z1 and f.usable(x, z, t):
                active.add((x, z))
    mode, rate = op.get('mode', 'thermal'), op.get('rate', 0.5)
    talus, jitter = op.get('talus', 1.0), op.get('jitter', 0.0)
    tal = {c: talus * (1 + jitter * (hash01(s, c[0], c[1]) * 2 - 1)) for c in active}
    order = sorted(active)
    for _ in range(op.get('iterations', 4)):
        delta = dict.fromkeys(order, 0.0)
        for c in order:
            hc = h[c]
            if mode == 'smooth':
                ns = [h[(c[0] + dx, c[1] + dz)] for dx, dz, _ in NEIGH[:4] if (c[0] + dx, c[1] + dz) in h]
                if ns:
                    delta[c] += rate * (sum(ns) / len(ns) - hc)
                continue
            for dx, dz, dist in NEIGH:
                n = (c[0] + dx, c[1] + dz)
                if n not in h:
                    continue
                drop = hc - h[n]
                over = drop - tal[c] * dist
                if over > 0:
                    move = rate * over / 4
                    delta[c] -= move
                    if n in delta:
                        delta[n] += move
        for c in order:
            h[c] += delta[c]
    clamp = op.get('clamp')
    for c in order:
        new = _clamp(int(math.floor(h[c] + 0.5)), clamp)
        if new != tops[c]:
            f.reshape(c[0], c[1], tops[c], new)


# ---------------------------------------------------------------- breakup

def run_breakup(op, get, put):
    """Swaps surface blocks named in replace for a weighted mix, in clumps about `scale` blocks across, at `density`.
    With near and width it only works within width blocks of a column whose surface is one of near, fading out
    with distance (a boundary blend). depth > 1 also reaches down the column; exposed limits it to blocks with air
    or water above or beside them (cliff and bank faces)."""
    f = Field(op, get, put)
    s = seed_of(op.get('seed', 0))
    replace = {normalize(n).split('[')[0] for n in op['replace']}
    pick = picker(op['mix'], s)
    density, scale = op.get('density', 0.5), op.get('scale', 3)
    depth, exposed = int(op.get('depth', 1)), op.get('exposed', False)
    near = {normalize(n).split('[')[0] for n in op.get('near', [])}
    width = op.get('width', 0)
    cols = list(_box_cols(op))
    tops = {}
    pad = int(math.ceil(width)) if near else 0
    (x0, z0), (x1, z1) = op['min'], op['max']
    offsets = sorted((math.hypot(dx, dz), dx, dz) for dx in range(-pad, pad + 1) for dz in range(-pad, pad + 1)
                     if math.hypot(dx, dz) <= width)
    if near:
        seeds = set()
        for x in range(x0 - pad, x1 + pad + 1):
            for z in range(z0 - pad, z1 + pad + 1):
                t = f.top(x, z)
                tops[(x, z)] = t
                if t is not None and block_name(get((x, t, z)).state) in near:
                    seeds.add((x, z))
                elif t is not None and t + 1 <= f.scan[1] and block_name(get((x, t + 1, z)).state) in near:
                    seeds.add((x, z))  # water over a bed counts as water
    for x, z in cols:
        top = tops.get((x, z)) if (x, z) in tops else f.top(x, z)
        if top is None or not f.allowed(x, z):
            continue
        weight = 1.0
        if near:
            d = next((dd for dd, dx, dz in offsets if (x + dx, z + dz) in seeds), None)
            if d is None:
                continue
            weight = 1.0 - d / (width + 1)
        for y in range(top, top - depth, -1):
            cur = get((x, y, z))
            if block_name(cur.state) not in replace:
                continue
            if exposed and y != top and not any(
                    block_name(get((x + dx, y, z + dz)).state) in NOT_GROUND for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                continue
            clump = value2(s + 31, x + y * 3, z - y * 2, scale) if scale > 1 else hash01(s, x, y, z, 5)
            if clump < density * weight:
                put((x, y, z), pick(x, y, z))


# ---------------------------------------------------------------- polylines

def _segments(op):
    pts = [list(map(float, p)) for p in op['points']]
    if op.get('closed') and pts[0][:2] != pts[-1][:2]:
        pts.append(pts[0])
    return pts


def nearest(pts, x, z):
    """(distance, attribute) from (x, z) to the polyline; the attribute (a point's third value) is interpolated
    along the nearest segment, or None when the points have none."""
    best = None
    for a, b in zip(pts, pts[1:]):
        ax, az, bx, bz = a[0], a[1], b[0], b[1]
        dx, dz = bx - ax, bz - az
        ll = dx * dx + dz * dz
        t = 0.0 if ll == 0 else max(0.0, min(1.0, ((x - ax) * dx + (z - az) * dz) / ll))
        px, pz = ax + t * dx, az + t * dz
        d = math.hypot(x - px, z - pz)
        if best is None or d < best[0] - 1e-9:
            attr = (a[2] + t * (b[2] - a[2])) if len(a) > 2 and len(b) > 2 else None
            best = (d, attr)
    return best


def _poly_cols(op, f, pts, reach):
    xs, zs = [p[0] for p in pts], [p[1] for p in pts]
    bx0, bx1 = int(math.floor(min(xs) - reach)), int(math.ceil(max(xs) + reach))
    bz0, bz1 = int(math.floor(min(zs) - reach)), int(math.ceil(max(zs) + reach))
    if f.clip:
        bx0, bz0, bx1, bz1 = max(bx0, f.clip[0]), max(bz0, f.clip[1]), min(bx1, f.clip[2]), min(bz1, f.clip[3])
    for z in range(bz0, bz1 + 1):
        for x in range(bx0, bx1 + 1):
            if f.allowed(x, z):
                yield x, z


def run_wall(op, get, put):
    """Columns within thickness/2 of the polyline become wall from floor to top (op top, or each point's third
    value interpolated along the line). merlons adds a block on every other column of the wall's outer edge."""
    f = Field(op, get, put)
    s = seed_of(op.get('seed', 0))
    pts = _segments(op)
    half = op.get('thickness', 3) / 2.0
    pick = picker(op['block'], s)
    for x, z in _poly_cols(op, f, pts, half + 1):
        d, attr = nearest(pts, x, z)
        if d > half:
            continue
        if f.only is not None:
            t = f.top(x, z)
            if t is None or block_name(get((x, t, z)).state) not in f.only:
                continue
        top = int(round(attr)) if 'top' not in op and attr is not None else op['top']
        for y in range(op['floor'], top + 1):
            put((x, y, z), pick(x, y, z))
        if op.get('merlons') and d > half - 1 and (x + z) % 2 == 0:
            put((x, top + 1, z), pick(x, top + 1, z))


def run_path(op, get, put):
    """A road or causeway along the polyline: columns within width/2 get `surface`; the next `falloff` blocks blend
    toward the ground beside it. With y (op y, or each point's third value) the core is graded to that top and the
    falloff band eases from it back to the ground (an embankment when y is above the ground, a cutting below);
    without y the heights stay. The falloff band is surfaced with `edge` at a density fading outward."""
    f = Field(op, get, put)
    s = seed_of(op.get('seed', 0))
    pts = _segments(op)
    half, fall = op['width'] / 2.0, op.get('falloff', 0)
    wob, wsc = op.get('wobble', 0), op.get('wobbleScale', 8)
    surf = picker(op['surface'], s)
    under = picker(op['under'], s + 1) if 'under' in op else None
    edge = picker(op['edge'], s + 2) if 'edge' in op else None
    eden = op.get('edgeDensity', 0.6)
    for x, z in _poly_cols(op, f, pts, half + fall + wob + 1):
        d, attr = nearest(pts, x, z)
        if wob:
            d += wob * fbm(s + 9, x, z, wsc, 2)
        if d > half + fall:
            continue
        top = f.top(x, z)
        if not f.usable(x, z, top):
            continue
        target = op['y'] if 'y' in op else (int(round(attr)) if attr is not None else None)
        if d <= half:
            new = top if target is None else target
            cap = surf(x, new, z)
        else:
            t = (d - half) / fall
            t = t * t * (3 - 2 * t)
            new = top if target is None else int(round(target + (top - target) * t))
            cap = edge(x, new, z) if edge and hash01(s, x, z, 3) < eden * (1 - t) else None
        f.reshape(x, z, top, new, cap, under(x, new, z) if under and new > top else None)


def run_river(op, get, put):
    """A channel along the polyline: within width/2 (op width, or each point's third value) the column is cut or
    filled to a bed `depth` below `water` at the centre line and 1 below at the edge, filled with water to `water`
    and cleared above it. The next `bank` blocks slope from the water's edge up to the ground (0 = a cut cliff);
    banks are only lowered, never raised. faces lines exposed bank and cliff cells with bankSurface."""
    f = Field(op, get, put)
    s = seed_of(op.get('seed', 0))
    pts = _segments(op)
    width = op['width']
    reach = max([width] + [p[2] for p in pts if len(p) > 2]) / 2.0
    bank, depth, water = op.get('bank', 0), op.get('depth', 2), op['water']
    wob, wsc = op.get('wobble', 0), op.get('wobbleScale', 8)
    bed = picker(op.get('bed', 'minecraft:gravel'), s)
    bsurf = picker(op['bankSurface'], s + 1) if 'bankSurface' in op else None
    cut = []
    for x, z in _poly_cols(op, f, pts, reach + bank + wob + 1):
        d, attr = nearest(pts, x, z)
        half = (attr if attr is not None else width) / 2.0
        if wob:
            d += wob * fbm(s + 9, x, z, wsc, 2)
        if d > half + bank:
            continue
        top = f.top(x, z)
        if top is not None and not f.usable(x, z, top):
            continue
        if d <= half:
            k = max(0.0, 1 - (d / half) ** 2) if half > 0 else 1.0
            bed_y = water - max(1, int(round(1 + (depth - 1) * k)))
            hi = max(top if top is not None else water, water)
            for y in range(op['floor'], bed_y + 1):
                if y >= bed_y - 1 or block_name(get((x, y, z)).state) in NOT_GROUND:
                    put((x, y, z), bed(x, y, z))
            for y in range(bed_y + 1, water + 1):
                put((x, y, z), WATER)
            for y in range(water + 1, hi + 1):
                put((x, y, z), AIR_CELL)
            cut.append((x, z))
        elif top is not None and top > water:
            t = (d - half) / bank
            t = t * t * (3 - 2 * t)
            new = min(top, int(round(water + 1 + (top - water - 1) * t)))
            if new != top or bsurf:
                f.reshape(x, z, top, new, bsurf(x, new, z) if bsurf else None)
    if op.get('faces') and bsurf:
        cutset = set(cut)
        for x, z in cut:
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in cutset or not f.allowed(*n):
                    continue
                t = f.top(*n)
                if t is None or not f.usable(n[0], n[1], t):
                    continue
                for y in range(int(water - depth), t):
                    if block_name(get((n[0], y, n[1])).state) not in NOT_GROUND:
                        put((n[0], y, n[1]), bsurf(n[0], y, n[1]))


RUN = {'noise': run_noise, 'erode': run_erode, 'breakup': run_breakup, 'wall': run_wall, 'path': run_path,
       'river': run_river}
