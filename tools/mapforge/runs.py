"""Column-run files (`gg-mapforge-runs/1`): the one format of map adoption (plan tasks 2A.4 and 2A.5).

A runs file lists cells of a world as runs along y, one line per column, gzip-compressed with mtime 0 so the same
cells always give the same bytes. The game reads and writes the same files (test-tools MapForge.java, the shipped
MapAdoption.java and MapRuns.java), so this module and those classes must agree byte for byte.

Kinds:
  capture  every cell of a box, air included (an export of a world; used as the full-snapshot form too)
  overlay  only the cells where a final world differs from the generated base, with their final states;
           baseDigest is the digest of the base at exactly those cells, so a changed generator is refused
  batch    the cells an agent's build plan writes (compiled offline, applied in game by /mapforge apply)
  undo     the cells an apply changed, with their states before it; afterDigest is what the apply left

Text, UTF-8, '\\n' line ends:
  line 1   header JSON (sorted keys, no spaces)
  P i S    palette entry i is the block state S (indices in order of first use)
  C x z y n p [y n p ...]   runs in one column: n cells from y up hold palette state p; ascending, not overlapping,
           and adjacent runs never share a state (maximal runs), so equal cells always give equal lines
  E x y z B   block entity of a listed cell: base64 of its uncompressed NBT compound
C lines are sorted by (x, z), E lines by (x, y, z).

Digest (`digest` and `baseDigest`): sha256 over 'x,z,y,n|state\\n' for every maximal run, in file order. A digest
of other cells over the same ranges (the base under an overlay) splits each range into that world's own maximal
runs. Block entities are not in digests. `body` is the sha256 of the text after the header line.
"""
import base64
import gzip
import hashlib
import json
import os
import pathlib

FORMAT = 'gg-mapforge-runs/1'
KINDS = ('capture', 'overlay', 'batch', 'undo')
AIR = 'minecraft:air'
Y_MIN, Y_MAX = -64, 319


SETTLES = {'minecraft:grass_block', 'minecraft:dirt', 'minecraft:mycelium', 'minecraft:farmland'}
FLUIDS = {'minecraft:water', 'minecraft:lava'}


def settle(state):
    """World-comparing digests ignore what the game changes on its own (MapRuns.settle): soil that random ticks
    turn into each other counts as dirt, and flowing (non-source) water or lava counts as air."""
    base = state.split('[', 1)[0]
    if base in SETTLES:
        return 'minecraft:dirt'
    if base in FLUIDS and 'level=' in state and 'level=0' not in state:
        return 'minecraft:air'
    return state


class RunsError(ValueError):
    pass


class Runs:
    def __init__(self, kind, header=None):
        if kind not in KINDS:
            raise RunsError(f'unknown kind {kind}')
        self.header = dict(header or {})
        self.header['format'] = FORMAT
        self.header['kind'] = kind
        self.columns = {}   # (x, z) -> [[y, n, state], ...] ascending, maximal
        self.be = {}        # (x, y, z) -> base64 string

    @property
    def kind(self):
        return self.header['kind']

    # ---- building ----
    def add(self, x, z, y, n, state):
        """Appends a run above the column's last one, merging with it when it continues the same state."""
        if n <= 0:
            return
        col = self.columns.setdefault((x, z), [])
        if col:
            last = col[-1]
            end = last[0] + last[1]
            if y < end:
                raise RunsError(f'run at {x},{y},{z} overlaps the run below it')
            if y == end and last[2] == state:
                last[1] += n
                return
        col.append([y, n, state])

    def cells(self):
        return sum(n for col in self.columns.values() for _, n, _ in col)

    def get(self, x, y, z):
        for y0, n, s in self.columns.get((x, z), ()):
            if y0 <= y < y0 + n:
                return s
        return None

    # ---- digests ----
    def digest(self):
        h = hashlib.sha256()
        for (x, z) in sorted(self.columns):
            for y, n, s in self.columns[(x, z)]:
                h.update(f'{x},{z},{y},{n}|{s}\n'.encode())
        return h.hexdigest()

    def digest_over(self, ranges):
        """Digest of this file's cells over another file's ranges (the base under an overlay), soil settled
        (settle) and runs merged within each range, as MapRuns.WorldDigest reads a live world. A range this file
        does not cover counts as air."""
        h = hashlib.sha256()
        for (x, z) in sorted(ranges.columns):
            mine = self.columns.get((x, z), [])
            for y, n, _ in ranges.columns[(x, z)]:
                run = None
                for sy, sn, s in split(mine, y, y + n):
                    s = settle(s)
                    if run and run[2] == s:
                        run[1] += sn
                        continue
                    if run:
                        h.update(f'{x},{z},{run[0]},{run[1]}|{run[2]}\n'.encode())
                    run = [sy, sn, s]
                if run:
                    h.update(f'{x},{z},{run[0]},{run[1]}|{run[2]}\n'.encode())
        return h.hexdigest()

    # ---- text ----
    def body(self):
        palette, lines = {}, []
        for (x, z) in sorted(self.columns):
            parts = [f'C {x} {z}']
            for y, n, s in self.columns[(x, z)]:
                p = palette.get(s)
                if p is None:
                    p = palette[s] = len(palette)
                parts.append(f'{y} {n} {p}')
            lines.append(' '.join(parts))
        for (x, y, z) in sorted(self.be):
            lines.append(f'E {x} {y} {z} {self.be[(x, y, z)]}')
        head = [f'P {i} {s}' for s, i in palette.items()]
        return '\n'.join(head + lines) + '\n'

    def to_bytes(self):
        body = self.body()
        self.header['cells'] = self.cells()
        self.header['digest'] = self.digest()
        self.header['body'] = hashlib.sha256(body.encode()).hexdigest()
        text = json.dumps(self.header, sort_keys=True, separators=(',', ':')) + '\n' + body
        return gzip.compress(text.encode(), mtime=0)

    def write(self, path):
        path = pathlib.Path(path)
        data = self.to_bytes()
        tmp = path.with_name(path.name + '.part')
        tmp.write_bytes(data)
        os.replace(tmp, path)
        return len(data)

    @classmethod
    def from_bytes(cls, data, check=True):
        text = gzip.decompress(data).decode()
        first, _, body = text.partition('\n')
        header = json.loads(first)
        if header.get('format') != FORMAT:
            raise RunsError(f'not a {FORMAT} file: format {header.get("format")!r}')
        if check and hashlib.sha256(body.encode()).hexdigest() != header.get('body'):
            raise RunsError('body hash differs from the header: the file was changed or truncated')
        r = cls(header.get('kind'), header)
        palette = {}
        for line in body.splitlines():
            if not line:
                continue
            tag = line[0]
            if tag == 'P':
                _, i, s = line.split(' ', 2)
                palette[int(i)] = s
            elif tag == 'C':
                v = line.split(' ')
                x, z = int(v[1]), int(v[2])
                if (x, z) in r.columns:
                    raise RunsError(f'column {x},{z} listed twice')
                for k in range(3, len(v), 3):
                    y, n, p = int(v[k]), int(v[k + 1]), int(v[k + 2])
                    if p not in palette:
                        raise RunsError(f'column {x},{z}: palette index {p} undefined')
                    col = r.columns.setdefault((x, z), [])
                    if col and (y < col[-1][0] + col[-1][1] or (y == col[-1][0] + col[-1][1] and col[-1][2] == palette[p])):
                        raise RunsError(f'column {x},{z}: runs overlap or are not maximal at y {y}')
                    if n <= 0 or y < Y_MIN or y + n - 1 > Y_MAX:
                        raise RunsError(f'column {x},{z}: run {y}+{n} outside y {Y_MIN}..{Y_MAX}')
                    col.append([y, n, palette[p]])
            elif tag == 'E':
                _, x, y, z, b = line.split(' ', 4)
                r.be[(int(x), int(y), int(z))] = b
            else:
                raise RunsError(f'unknown line {line[:20]!r}')
        if check:
            if r.cells() != header.get('cells'):
                raise RunsError('cell count differs from the header')
            if r.digest() != header.get('digest'):
                raise RunsError('digest differs from the header')
            for (x, y, z) in r.be:
                if r.get(x, y, z) is None:
                    raise RunsError(f'block entity at {x},{y},{z} is not on a listed cell')
        return r

    @classmethod
    def read(cls, path, check=True):
        return cls.from_bytes(pathlib.Path(path).read_bytes(), check)


def split(col, lo, hi):
    """The maximal runs of a column inside [lo, hi), air where the column lists nothing."""
    out = []

    def put(y, n, s):
        if out and out[-1][2] == s and out[-1][0] + out[-1][1] == y:
            out[-1][1] += n
        else:
            out.append([y, n, s])
    y = lo
    for y0, n, s in col:
        a, b = max(y0, lo), min(y0 + n, hi)
        if a >= b:
            continue
        if a > y:
            put(y, a - y, AIR)
        put(a, b - a, s)
        y = b
    if y < hi:
        put(y, hi - y, AIR)
    return out


def diff(base, final, header=None):
    """Overlay of final over base: the cells of final whose state (or block entity) differs from base, with final's
    states. Both are captures; only columns and heights both cover are compared."""
    if base.kind != 'capture' or final.kind != 'capture':
        raise RunsError('diff needs two captures')
    out = Runs('overlay', header)
    for key in sorted(final.columns):
        b = base.columns.get(key)
        if b is None:
            continue
        x, z = key
        lo, hi = b[0][0], b[-1][0] + b[-1][1]
        for y, n, s in final.columns[key]:
            for sy, sn, bs in split(b, max(y, lo), min(y + n, hi)) if max(y, lo) < min(y + n, hi) else ():
                if bs == s:
                    # Same state: a changed block entity still counts, cell by cell.
                    for yy in range(sy, sy + sn):
                        if final.be.get((x, yy, z)) != base.be.get((x, yy, z)):
                            out.add(x, z, yy, 1, s)
                    continue
                out.add(x, z, sy, sn, s)
    for (x, y, z), b64 in final.be.items():
        if out.get(x, y, z) is not None:
            out.be[(x, y, z)] = b64
    out.header['baseDigest'] = base.digest_over(out)
    return out


def apply(patch, capture):
    """A new capture: capture with patch's cells written over it (the offline twin of MapAdoption and /mapforge
    apply, for tests and the spike)."""
    out = Runs('capture', capture.header)
    for key, col in capture.columns.items():
        p = patch.columns.get(key)
        if not p:
            out.columns[key] = [list(r) for r in col]
            continue
        lo, hi = col[0][0], col[-1][0] + col[-1][1]
        cells = {}
        for y, n, s in p:
            for yy in range(max(y, lo), min(y + n, hi)):
                cells[yy] = s
        x, z = key
        for y, n, s in col:
            for yy in range(y, y + n):
                out.add(x, z, yy, 1, cells.get(yy, s))
    out.be = {k: v for k, v in capture.be.items() if patch.get(*k) is None}
    for k, v in patch.be.items():
        if capture.get(*k) is not None:
            out.be[k] = v
    return out


def from_world(world, kind, header=None, box=None, top=None, keep=None):
    """Runs from an offline mapforge world. capture: every cell of box (x, y, z min and max), air included.
    batch: each column of the world from its lowest cell up to `top` (air above and between its cells), so the
    surface replaces whatever the live world holds there and keeps only what lies under it. keep(x, y, z) True
    leaves a cell out (protected cells keep the live world)."""
    out = Runs(kind, header)
    cols = {}
    for (x, y, z), cell in world.blocks.items():
        if box and not (box[0][0] <= x <= box[1][0] and box[0][1] <= y <= box[1][1] and box[0][2] <= z <= box[1][2]):
            continue
        cols.setdefault((x, z), {})[y] = cell
    if kind == 'capture':
        keys = [(x, z) for x in range(box[0][0], box[1][0] + 1) for z in range(box[0][2], box[1][2] + 1)]
    else:
        keys = sorted(cols)
    for (x, z) in keys:
        c = cols.get((x, z), {})
        lo = box[0][1] if kind == 'capture' else min(c)
        hi = box[1][1] if kind == 'capture' else (top if top is not None else max(c))
        for y in range(lo, hi + 1):
            if keep and keep(x, y, z):
                continue
            cell = c.get(y)
            out.add(x, z, y, 1, cell.state if cell else AIR)
            if cell is not None and cell.be is not None:
                out.be[(x, y, z)] = base64.b64encode(cell.be).decode('ascii')
    return out


def to_world(runs, world_cls):
    """An offline world holding a capture's cells (air left out)."""
    from .world import Cell
    w = world_cls(runs.header.get('map', 'offline'))
    for (x, z), col in runs.columns.items():
        for y, n, s in col:
            if s == AIR:
                continue
            for yy in range(y, y + n):
                b = runs.be.get((x, yy, z))
                w.set((x, yy, z), Cell(s, base64.b64decode(b) if b else None))
    return w


def validate(path):
    """Problems with a runs file, as a list of strings (empty when it is sound)."""
    try:
        r = Runs.read(path)
    except (RunsError, OSError, ValueError, KeyError) as e:
        return [str(e)], None
    errors = []
    h = r.header
    for key in ('map', 'stamp'):
        if not isinstance(h.get(key), str) or not h.get(key):
            errors.append(f'header has no {key}')
    if r.kind == 'overlay' and not isinstance(h.get('baseDigest'), str):
        errors.append('overlay header has no baseDigest')
    if r.kind == 'undo' and not isinstance(h.get('afterDigest'), str):
        errors.append('undo header has no afterDigest')
    box = h.get('box')
    if box:
        (x0, y0, z0), (x1, y1, z1) = box['min'], box['max']
        for (x, z), col in r.columns.items():
            if not (x0 <= x <= x1 and z0 <= z <= z1) or col[0][0] < y0 or col[-1][0] + col[-1][1] - 1 > y1:
                errors.append(f'column {x},{z} lies outside the header box')
                break
    return errors, r
