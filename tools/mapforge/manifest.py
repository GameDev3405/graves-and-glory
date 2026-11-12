"""Map manifests (gg-mapforge-map/1): one file per map holding its bounds, the section grid with each section's status
and reviews, the protect locks (stations, spawns, capture points, Siege sites) and the palette reference. Plans name a
section; their allow region comes from the section's bounds. The format is documented in tools/mapforge/README.md."""
import datetime
import json
import pathlib

from .world import region_digests, write_json_atomic

MAP_FORMAT = 'gg-mapforge-map/1'
PASSES = ('blockout', 'structure', 'natural', 'detail')
STATUSES = ('todo',) + PASSES + ('done',)
PROTECT_KINDS = ('station', 'spawn', 'capture', 'siege', 'other')
VERDICTS = ('pass', 'changes')


class ManifestError(Exception):
    def __init__(self, errors):
        super().__init__('; '.join(errors))
        self.errors = list(errors)


def _vec(v, n):
    return isinstance(v, list) and len(v) == n and all(isinstance(c, int) and not isinstance(c, bool) for c in v)


def _box_ok(item):
    return _vec(item.get('min'), 3) and _vec(item.get('max'), 3) and all(item['min'][i] <= item['max'][i] for i in range(3))


def _within(inner, outer):
    return all(outer['min'][i] <= inner['min'][i] and inner['max'][i] <= outer['max'][i] for i in range(3))


def _overlap(a, b):
    return all(a['min'][i] <= b['max'][i] and b['min'][i] <= a['max'][i] for i in range(3))


def validate_manifest(m):
    """Errors in a manifest (empty when valid)."""
    if not isinstance(m, dict):
        return ['the manifest must be a JSON object']
    errors = []
    if m.get('format') != MAP_FORMAT:
        errors.append(f'format must be {MAP_FORMAT!r}')
    if not isinstance(m.get('id'), str) or not m['id'] or not all(c.isalnum() or c in '-_' for c in m['id']):
        errors.append('id must be a map id of letters, digits, - and _')
    bounds = m.get('bounds')
    if not isinstance(bounds, dict) or not _box_ok(bounds):
        errors.append('bounds must be {"min": [x, y, z], "max": [x, y, z]} with min <= max')
        bounds = None
    if not isinstance(m.get('palette'), str) or not m['palette'].strip():
        errors.append('palette must name the palette reference (a palette id or file)')
    sections = m.get('sections')
    if not isinstance(sections, list) or not sections:
        errors.append('sections must be a non-empty list')
        sections = []
    seen, good = set(), []
    for i, s in enumerate(sections):
        where = f'sections[{i}]'
        if not isinstance(s, dict):
            errors.append(f'{where}: must be an object')
            continue
        sid = s.get('id')
        if not isinstance(sid, str) or not sid or sid in seen or not all(c.isalnum() or c in '-_' for c in sid):
            errors.append(f'{where}: id must be a unique string of letters, digits, - and _')
        if isinstance(sid, str):
            seen.add(sid)
        b = s.get('bounds')
        if not isinstance(b, dict) or not _box_ok(b):
            errors.append(f'{where} ({sid}): bounds must be a min/max box')
        else:
            if bounds and not _within(b, bounds):
                errors.append(f'{where} ({sid}): bounds lie outside the map bounds')
            good.append((sid, b))
        if s.get('status') not in STATUSES:
            errors.append(f'{where} ({sid}): status must be one of {", ".join(STATUSES)}')
        reviews = s.get('reviews', [])
        if not isinstance(reviews, list):
            errors.append(f'{where} ({sid}): reviews must be a list')
            reviews = []
        for j, r in enumerate(reviews):
            if not isinstance(r, dict) or r.get('pass') not in PASSES + ('done',) or r.get('verdict') not in VERDICTS \
                    or not isinstance(r.get('by'), str) or not r['by'].strip():
                errors.append(f'{where} ({sid}).reviews[{j}]: needs pass, verdict ({"/".join(VERDICTS)}) and by')
    for a in range(len(good)):
        for b in range(a + 1, len(good)):
            if _overlap(good[a][1], good[b][1]):
                errors.append(f'sections {good[a][0]} and {good[b][0]} overlap; sections tile the map')
    protect = m.get('protect', [])
    if not isinstance(protect, list):
        errors.append('protect must be a list')
        protect = []
    ids = set()
    for i, lock in enumerate(protect):
        where = f'protect[{i}]'
        if not isinstance(lock, dict):
            errors.append(f'{where}: must be an object')
            continue
        if not isinstance(lock.get('id'), str) or lock['id'] in ids:
            errors.append(f'{where}: id must be a unique string')
        if isinstance(lock.get('id'), str):
            ids.add(lock['id'])
        if lock.get('kind') not in PROTECT_KINDS:
            errors.append(f'{where}: kind must be one of {", ".join(PROTECT_KINDS)}')
        if not isinstance(lock.get('reason'), str) or not lock['reason'].strip():
            errors.append(f'{where}: reason must say what is locked and why')
        if 'center' in lock:
            c, r, y = lock.get('center'), lock.get('radius'), lock.get('y')
            if not (_vec(c, 2) and isinstance(r, (int, float)) and r > 0 and _vec(y, 2) and y[0] <= y[1]):
                errors.append(f'{where}: a cylinder lock needs center [x, z], radius > 0 and y [min, max]')
        elif not _box_ok(lock):
            errors.append(f'{where}: give min/max (a box) or center/radius/y (a cylinder)')
    return errors


def load_manifest(path):
    m = json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
    errors = validate_manifest(m)
    if errors:
        raise ManifestError([f'{path}: {e}' for e in errors])
    return m


def save_manifest(path, m):
    errors = validate_manifest(m)
    if errors:
        raise ManifestError(errors)
    write_json_atomic(path, m)


def section(m, sid):
    for s in m['sections']:
        if s['id'] == sid:
            return s
    raise ManifestError([f'map {m["id"]} has no section {sid!r}'])


def section_box(s):
    return tuple(s['bounds']['min']), tuple(s['bounds']['max'])


def protect_locks(m):
    """The manifest's protect locks as deny locks (boxes or cylinders)."""
    return tuple(dict(lock, mode='deny') for lock in m.get('protect', []))


def next_pass(status):
    i = STATUSES.index(status)
    return STATUSES[i + 1] if i + 1 < len(STATUSES) else None


def intersect(a, lo, hi):
    """Lock a cut down to the box lo..hi, or None when they do not meet."""
    mn = [max(a['min'][i], lo[i]) for i in range(3)]
    mx = [min(a['max'][i], hi[i]) for i in range(3)]
    if any(mn[i] > mx[i] for i in range(3)):
        return None
    return dict(a, min=mn, max=mx)


def plan_scope(plan, m):
    """(locks, protect, errors) for a plan that names a section of manifest m. The allow regions are the section's
    bounds, intersected with the plan's own allow locks when it has any; the plan's deny locks are kept; protect is the
    manifest's locks. Errors cover a wrong map, an unknown section, and a pass the section's status does not allow."""
    errors = []
    if plan.get('world') != m['id']:
        errors.append(f'plan is for world {plan.get("world")!r}, the manifest is map {m["id"]!r}')
    try:
        s = section(m, plan['section'])
    except ManifestError as e:
        return [], (), errors + e.errors
    lo, hi = section_box(s)
    own = [l for l in plan.get('locks', []) if l['mode'] == 'allow']
    if own:
        allows = [c for c in (intersect(l, lo, hi) for l in own) if c]
        if not allows:
            errors.append(f'none of the plan\'s allow locks meets section {s["id"]} {list(lo)}..{list(hi)}')
    else:
        allows = [{'id': f'section-{s["id"]}', 'mode': 'allow', 'min': list(lo), 'max': list(hi)}]
    status, want = s['status'], plan['pass']
    if 'reopen' not in plan and want != next_pass(status):
        errors.append(f'section {s["id"]} is at {status}; a plan may only do the next pass '
                      f'({next_pass(status) or "none, the section is done"}), not {want}, unless it gives "reopen" with a reason')
    locks = allows + [l for l in plan.get('locks', []) if l['mode'] == 'deny']
    return locks, protect_locks(m), errors


def section_digests(m, world, ids=None):
    boxes = {s['id']: section_box(s) for s in m['sections'] if ids is None or s['id'] in ids}
    return region_digests(world, boxes)


def last_pass_review(s):
    for r in reversed(s.get('reviews', [])):
        if r['verdict'] == 'pass':
            return r
    return None


def sign(m, sid, pass_, world, by, verdict='pass', notes='', images=(), plans=(), now=None):
    """Records an agent review of one pass of a section: the section digest of world, the images and plans reviewed,
    and notes. A passing review moves the status to that pass; 'changes' records the review only. The current pass can
    be reviewed again (after a plan reopened it); that keeps the status. Returns the review."""
    s = section(m, sid)
    if verdict not in VERDICTS:
        raise ManifestError([f'verdict must be one of {", ".join(VERDICTS)}'])
    want = next_pass(s['status'])
    if pass_ != want and pass_ != s['status']:  # the next pass, or a re-review of the current one after rework
        raise ManifestError([f'section {sid} is at {s["status"]}; the next review is {want or "none (done)"}, not {pass_}'])
    if not by.strip():
        raise ManifestError(['by must name the reviewer'])
    review = {'pass': pass_, 'verdict': verdict, 'by': by,
              'at': (now or datetime.datetime.now(datetime.timezone.utc)).strftime('%Y-%m-%dT%H:%M:%SZ'),
              'digest': section_digests(m, world, {sid})[sid]}
    if plans:
        review['plans'] = list(plans)
    if images:
        review['images'] = list(images)
    if notes:
        review['notes'] = notes
    s.setdefault('reviews', []).append(review)
    if verdict == 'pass':
        s['status'] = pass_
    return review


def sign_map(m, world, by, now=None):
    """Kort's sign-off of the whole finished map: every section must be done. Records the world digest."""
    open_ = [s['id'] for s in m['sections'] if s['status'] != 'done']
    if open_:
        raise ManifestError([f'{len(open_)} section(s) are not done, first {open_[0]}; the whole map is signed last'])
    m['signed'] = {'by': by, 'at': (now or datetime.datetime.now(datetime.timezone.utc)).strftime('%Y-%m-%dT%H:%M:%SZ'),
                   'world': world.digest()}
    return m['signed']


def status_rows(m, world=None):
    """One row per section: id, name, status, last review, and drift (the section changed since its last passing
    review) when a world is given."""
    digests = section_digests(m, world) if world is not None else {}
    rows = []
    for s in m['sections']:
        last = s.get('reviews', [])[-1] if s.get('reviews') else None
        good = last_pass_review(s)
        drift = None
        if world is not None and good is not None:
            drift = digests[s['id']] != good['digest']
        rows.append({'id': s['id'], 'name': s.get('name', ''), 'status': s['status'],
                     'last': f'{last["pass"]} {last["verdict"]} by {last["by"]} {last["at"]}' if last else '',
                     'drift': drift})
    return rows


def format_status(m, rows):
    counts = {}
    for r in rows:
        counts[r['status']] = counts.get(r['status'], 0) + 1
    lines = [f'map {m["id"]}: {len(rows)} sections; ' + ', '.join(f'{counts[k]} {k}' for k in STATUSES if k in counts)]
    if m.get('signed'):
        lines.append(f'whole map signed by {m["signed"]["by"]} {m["signed"]["at"]}')
    for r in rows:
        if r['status'] == 'todo' and not r['last'] and not r['drift']:
            continue
        flag = '  DRIFT since last passing review' if r['drift'] else ''
        lines.append(f'  {r["id"]:<6} {r["status"]:<9} {r["name"][:28]:<28} {r["last"]}{flag}')
    todo = sum(1 for r in rows if r['status'] == 'todo' and not r['last'])
    if todo:
        lines.append(f'  ({todo} untouched todo section(s) not listed)')
    return '\n'.join(lines)
