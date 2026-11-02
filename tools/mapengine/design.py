"""Map engine design file: schema, validation, load/save. Pure data, no Minecraft."""
import json
import os
import pathlib
import tempfile

FORMAT = 1
KINDS = ('hill', 'river', 'road', 'point', 'spawn', 'zone', 'note')
TEAMS = ('dawn', 'dusk')
BY = ('kort', 'ai')
STATES = ('accepted', 'proposed')
MAX_BRIEF = 8000
MAX_NOTE = 2000
PALETTES_PATH = pathlib.Path(__file__).resolve().parent / 'palettes.json'


class DesignError(Exception):
    def __init__(self, problems):
        self.problems = list(problems)
        super().__init__('; '.join(self.problems))


def palette_keys():
    return set(json.loads(PALETTES_PATH.read_text(encoding='utf-8')))


def new_design(id, name, width=700, depth=500):
    return {'format': FORMAT, 'id': id, 'name': name, 'width': width, 'depth': depth, 'baseY': 64, 'objects': []}


def _num(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def _int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def _pts_ok(pts, n):
    return (isinstance(pts, list) and len(pts) >= n
            and all(isinstance(p, (list, tuple)) and len(p) == 2 and _num(p[0]) and _num(p[1]) for p in pts))


def _range(o, key, lo, hi, out, integer=False):
    v = o.get(key)
    ok = _int(v) if integer else _num(v)
    if not ok or not lo <= v <= hi:
        out.append(f"{o['id']}: {key} must be {'an integer' if integer else 'a number'} from {lo} to {hi}")


def validate(d):
    if not isinstance(d, dict):
        return ['design: not an object']
    out = []
    if d.get('format') != FORMAT:
        out.append(f'format must be {FORMAT}')
    w, dp = d.get('width'), d.get('depth')
    if not (_int(w) and w > 0 and _int(dp) and dp > 0):
        out.append('width and depth must be positive integers')
        w = dp = 0
    brief = d.get('brief', '')
    if not isinstance(brief, str) or len(brief) > MAX_BRIEF:
        out.append(f'brief must be text of at most {MAX_BRIEF} characters')
    review = d.get('review', [])
    if not isinstance(review, list) or not all(isinstance(r, str) for r in review):
        out.append('review must be a list of strings')
    seen = set()
    names = set()
    palettes = palette_keys()
    objects = d.get('objects', [])
    if not isinstance(objects, list):
        out.append('objects: not a list')
        objects = []
    for i, o in enumerate(objects):
        if not isinstance(o, dict):
            out.append(f'object #{i}: not an object')
            continue
        oid = o.get('id') if isinstance(o.get('id'), str) else f'object #{i}'
        o = dict(o, id=oid)
        if oid in seen:
            out.append(f'{oid}: duplicate id')
        seen.add(oid)
        kind = o.get('kind')
        if kind not in KINDS:
            out.append(f'{oid}: unknown kind {kind!r}')
            continue
        if o.get('by') not in BY:
            out.append(f'{oid}: by must be one of {", ".join(BY)}')
        if o.get('state') not in STATES:
            out.append(f'{oid}: state must be one of {", ".join(STATES)}')
        if kind in ('hill', 'zone') and not _pts_ok(o.get('outline'), 3):
            out.append(f'{oid}: outline needs at least 3 vertices')
        if kind in ('river', 'road') and not _pts_ok(o.get('path'), 2):
            out.append(f'{oid}: path needs at least 2 vertices')
        if kind == 'hill':
            _range(o, 'height', -40, 120, out, integer=True)
            _range(o, 'steepness', 0, 1, out)
        elif kind == 'river':
            _range(o, 'width', 1, 64, out)
            _range(o, 'depth', 1, 12, out)
        elif kind == 'road':
            _range(o, 'width', 1, 16, out)
        elif kind == 'point':
            _range(o, 'radius', 2, 32, out)
            name = o.get('name')
            if not isinstance(name, str) or not name.strip():
                out.append(f'{oid}: name must be a non-empty string')
            elif name in names:
                out.append(f'{oid}: duplicate point name {name!r}')
            else:
                names.add(name)
        elif kind == 'note':
            _range(o, 'radius', 4, 200, out)
            text = o.get('text')
            if not isinstance(text, str) or not text.strip() or len(text) > MAX_NOTE:
                out.append(f'{oid}: text must be 1 to {MAX_NOTE} characters')
        elif kind == 'zone' and o.get('palette') not in palettes:
            out.append(f'{oid}: palette must be one of {", ".join(sorted(palettes))}')
        if kind == 'spawn' and o.get('team') not in TEAMS:
            out.append(f'{oid}: team must be dawn or dusk')
        if kind in ('point', 'spawn', 'note'):
            x, z = o.get('x'), o.get('z')
            if not (_num(x) and _num(z) and 0 <= x < w and 0 <= z < dp):
                out.append(f'{oid}: x and z must be inside the canvas (0..{w} by 0..{dp})')
    return out


def locked(o):
    """Kort placed, edited or accepted it: an AI draft may not change or delete it."""
    return o.get('by') == 'kort' or o.get('state') == 'accepted'


def build_problems(d):
    out = []
    objs = d.get('objects', [])
    waiting = sum(1 for o in objs if o.get('state') == 'proposed')
    if waiting:
        out.append(f'{waiting} AI proposal{"s" if waiting != 1 else ""} not reviewed: accept or reject them before building')
    for team in TEAMS:
        if not any(o.get('kind') == 'spawn' and o.get('team') == team for o in objs):
            out.append(f'no {team} spawn: add a spawn for {team}')
    if not any(o.get('kind') == 'point' for o in objs):
        out.append('no point: add at least one capture point')
    return out


def load(path):
    d = json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
    problems = validate(d)
    if problems:
        raise DesignError(problems)
    return d


def write_atomic(path, data):
    """Write bytes to path via a temp file in the same directory, so readers never see a partial file."""
    p = pathlib.Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=p.parent, prefix='.tmp-', suffix=p.suffix)
    try:
        with os.fdopen(fd, 'wb') as f:
            f.write(data)
        os.replace(tmp, p)
    except BaseException:
        try:
            os.unlink(tmp)
        except OSError:
            pass
        raise


def save(path, d):
    write_atomic(path, (json.dumps(d, sort_keys=True, indent=1) + '\n').encode('utf-8'))


def next_id(d, kind):
    n = 0
    prefix = kind + '-'
    for o in d.get('objects', []):
        i = o.get('id', '')
        if i.startswith(prefix) and i[len(prefix):].isdigit():
            n = max(n, int(i[len(prefix):]))
    return f'{prefix}{n + 1}'
