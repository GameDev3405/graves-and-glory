"""Map Forge plans: validation, compilation to block changes, region locks, dry-run summary, apply with an undo
record, and rollback. The format is documented in tools/mapforge/README.md."""
import collections
import datetime
import hashlib
import json
import pathlib

from .structure import AIR, Structure, block_name, normalize, resolve_piece, rotate_state, to_world
from . import terrain
from .world import AIR_CELL, Cell, write_json_atomic

PLAN_FORMAT = 'gg-mapforge-plan/1'
UNDO_FORMAT = 'gg-mapforge-undo/1'
OPS = ('set', 'fill', 'clear', 'structure', 'terrain') + terrain.HELPERS
DEFAULT_MAX_CHANGED = 65536
MAX_OP_VOLUME = 2_000_000
Y_RANGE = (-64, 319)  # 1.20.1 overworld build height


class PlanError(Exception):
    def __init__(self, errors):
        super().__init__('; '.join(errors))
        self.errors = list(errors)


def plan_sha(plan):
    return hashlib.sha256(json.dumps(plan, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def load_plan(path):
    return json.loads(pathlib.Path(path).read_text(encoding='utf-8'))


# ---------------------------------------------------------------- validation

def _vec(v, n):
    return isinstance(v, list) and len(v) == n and all(isinstance(c, int) and not isinstance(c, bool) for c in v)


def _box(where, item, errors, limit=True):
    if not (_vec(item.get('min'), 3) and _vec(item.get('max'), 3)):
        errors.append(f'{where}: min and max must be [x, y, z] integers')
        return None
    lo, hi = item['min'], item['max']
    if any(lo[i] > hi[i] for i in range(3)):
        errors.append(f'{where}: min {lo} is above max {hi} on some axis')
        return None
    if lo[1] < Y_RANGE[0] or hi[1] > Y_RANGE[1]:
        errors.append(f'{where}: y outside {Y_RANGE[0]}..{Y_RANGE[1]}')
    vol = (hi[0] - lo[0] + 1) * (hi[1] - lo[1] + 1) * (hi[2] - lo[2] + 1)
    if limit and vol > MAX_OP_VOLUME:
        errors.append(f'{where}: box of {vol} blocks is over {MAX_OP_VOLUME}')
    return tuple(lo), tuple(hi)


def _state(where, value, errors):
    try:
        return normalize(value)
    except (ValueError, AttributeError):
        errors.append(f'{where}: {value!r} is not a block state')
        return None


def validate(plan, plan_dir=None):
    """Errors in a plan (empty when valid). Structure files are checked to exist."""
    errors = []
    if not isinstance(plan, dict):
        return ['the plan must be a JSON object']
    if plan.get('format') != PLAN_FORMAT:
        errors.append(f'format must be {PLAN_FORMAT!r}')
    for key in ('id', 'world', 'author', 'purpose'):
        if not isinstance(plan.get(key), str) or not plan[key].strip():
            errors.append(f'{key} must be a non-empty string')
    if isinstance(plan.get('id'), str) and not all(c.isalnum() or c in '-_' for c in plan['id']):
        errors.append('id may hold only letters, digits, - and _')
    if 'expectWorld' in plan:
        errors.append('expectWorld is gone; name a section and give expectSection, the digest of that section '
                      '(mapforge status MAP --world WORLD --digests prints them)')
    sectioned = 'section' in plan
    if sectioned:
        from .manifest import PASSES
        if not isinstance(plan['section'], str) or not plan['section'].strip():
            errors.append('section must name a section of the map manifest')
        if plan.get('pass') not in PASSES:
            errors.append(f'pass must be one of {", ".join(PASSES)} when the plan names a section')
        if 'reopen' in plan and (not isinstance(plan['reopen'], str) or not plan['reopen'].strip()):
            errors.append('reopen must give the reason for redoing an earlier pass')
    elif 'pass' in plan or 'reopen' in plan:
        errors.append('pass and reopen belong to plans that name a section')
    if 'expectSection' in plan and not (sectioned and isinstance(plan['expectSection'], str)
                                        and len(plan['expectSection']) == 64):
        errors.append("expectSection must be the sha256 digest of the plan's section (and needs section)")
    limits = plan.get('limits', {})
    if not isinstance(limits, dict) or not isinstance(limits.get('maxChangedBlocks', 1), int) or limits.get('maxChangedBlocks', 1) <= 0:
        errors.append('limits.maxChangedBlocks must be a positive integer')
    locks = plan.get('locks')
    if locks is None and sectioned:
        locks = plan['locks'] = []
    if not isinstance(locks, list) or not (sectioned or any(isinstance(l, dict) and l.get('mode') == 'allow' for l in locks)):
        errors.append('locks must list at least one allow region (every change has to fall inside one), '
                      'or the plan must name a section, whose bounds are then the allow region')
        locks = locks if isinstance(locks, list) else []
    lock_ids = set()
    for i, lock in enumerate(locks):
        where = f'locks[{i}]'
        if not isinstance(lock, dict):
            errors.append(f'{where}: must be an object')
            continue
        if lock.get('mode') not in ('allow', 'deny'):
            errors.append(f'{where}: mode must be allow or deny')
        if not isinstance(lock.get('id'), str) or lock.get('id') in lock_ids:
            errors.append(f'{where}: id must be a unique string')
        lock_ids.add(lock.get('id'))
        _box(where, lock, errors, limit=False)
    ops = plan.get('operations')
    if not isinstance(ops, list) or not ops:
        errors.append('operations must be a non-empty list')
        ops = []
    for i, op in enumerate(ops):
        _validate_op(f'operations[{i}]', op, plan_dir, errors)
    return errors


def _validate_op(where, op, plan_dir, errors):
    if not isinstance(op, dict) or op.get('op') not in OPS:
        errors.append(f'{where}: op must be one of {", ".join(OPS)}')
        return
    kind = op['op']
    if kind in terrain.HELPERS:
        terrain.validate_helper(where, op, errors)
        return
    known = {'op', 'note'} | {
        'set': {'pos', 'block'},
        'fill': {'min', 'max', 'block', 'replace', 'shape'},
        'clear': {'min', 'max'},
        'structure': {'piece', 'file', 'at', 'rotation', 'includeAir'},
        'terrain': {'origin', 'base', 'heights', 'floor', 'surface', 'under', 'underDepth', 'core', 'clearTo'},
    }[kind]
    for key in sorted(set(op) - known):
        errors.append(f'{where}: unknown key {key!r} for {kind}')
    if kind == 'set':
        if not _vec(op.get('pos'), 3):
            errors.append(f'{where}: pos must be [x, y, z] integers')
        _state(where, op.get('block'), errors)
    elif kind in ('fill', 'clear'):
        _box(where, op, errors)
        if kind == 'fill':
            _state(where, op.get('block'), errors)
            if op.get('shape', 'solid') not in ('solid', 'hollow', 'outline'):
                errors.append(f'{where}: shape must be solid, hollow or outline')
            if 'replace' in op:
                if not isinstance(op['replace'], list) or not op['replace']:
                    errors.append(f'{where}: replace must be a non-empty list of block names or states')
                else:
                    for r in op['replace']:
                        _state(where + '.replace', r, errors)
    elif kind == 'structure':
        if ('piece' in op) == ('file' in op):
            errors.append(f'{where}: give exactly one of piece or file')
        else:
            try:
                path = _structure_path(op, plan_dir)
                if not path.is_file():
                    errors.append(f'{where}: no structure file {path}')
            except ValueError as e:
                errors.append(f'{where}: {e}')
        if not _vec(op.get('at'), 3):
            errors.append(f'{where}: at must be [x, y, z] integers (centre block, ground layer)')
        if op.get('rotation', 0) not in (0, 90, 180, 270):
            errors.append(f'{where}: rotation must be 0, 90, 180 or 270')
        if not isinstance(op.get('includeAir', True), bool):
            errors.append(f'{where}: includeAir must be true or false')
    elif kind == 'terrain':
        if not _vec(op.get('origin'), 2):
            errors.append(f'{where}: origin must be [x, z] integers')
        for key in ('base', 'floor'):
            if not isinstance(op.get(key), int):
                errors.append(f'{where}: {key} must be an integer y')
        h = op.get('heights')
        if not isinstance(h, list) or not h or not all(isinstance(r, list) for r in h):
            errors.append(f'{where}: heights must be a list of rows (one per z), each a list of offsets (one per x) or null')
        elif not all(v is None or (isinstance(v, int) and not isinstance(v, bool)) for r in h for v in r):
            errors.append(f'{where}: every height is an integer offset from base, or null to skip the column')
        for key in ('surface', 'under', 'core'):
            if key in op or key == 'surface':
                _state(f'{where}.{key}', op.get(key), errors)
        if not isinstance(op.get('underDepth', 3), int) or op.get('underDepth', 3) < 0:
            errors.append(f'{where}: underDepth must be a non-negative integer')
        if 'clearTo' in op and not isinstance(op['clearTo'], int):
            errors.append(f'{where}: clearTo must be an integer y')
        if isinstance(op.get('base'), int) and isinstance(op.get('floor'), int) and op['floor'] > op['base']:
            errors.append(f'{where}: floor is above base')


def _structure_path(op, plan_dir):
    if 'piece' in op:
        return resolve_piece(op['piece'])
    p = pathlib.Path(op['file'])
    return p if p.is_absolute() else pathlib.Path(plan_dir or '.') / p


# ---------------------------------------------------------------- compilation

def _matches(state, patterns):
    for pat in patterns:
        if '[' in pat:
            if state == pat:
                return True
        elif block_name(state) == pat:
            return True
    return False


def compile_plan(plan, world, plan_dir=None):
    """Runs the operations in order against an overlay of world (later operations see earlier ones) and returns
    (changes, per-op counts). changes: dict pos -> (before Cell, after Cell, index of the last op that wrote it),
    only for cells whose final value differs from the world."""
    errors = validate(plan, plan_dir)
    if errors:
        raise PlanError(errors)
    overlay, writer = {}, {}

    def get(pos):
        return overlay.get(pos, world.get(pos))

    def put(i, pos, cell):
        overlay[pos] = cell
        writer[pos] = i

    for i, op in enumerate(plan['operations']):
        kind = op['op']
        if kind == 'set':
            put(i, tuple(op['pos']), Cell(normalize(op['block'])))
        elif kind in ('fill', 'clear'):
            cell = Cell(normalize(op['block'])) if kind == 'fill' else AIR_CELL
            shape = op.get('shape', 'solid')
            patterns = [normalize(r) for r in op.get('replace', [])]
            lo, hi = op['min'], op['max']
            for x in range(lo[0], hi[0] + 1):
                for y in range(lo[1], hi[1] + 1):
                    for z in range(lo[2], hi[2] + 1):
                        edge = x in (lo[0], hi[0]) or y in (lo[1], hi[1]) or z in (lo[2], hi[2])
                        if shape == 'outline' and not edge:
                            continue
                        target = AIR_CELL if shape == 'hollow' and not edge else cell
                        pos = (x, y, z)
                        if patterns and not _matches(get(pos).state, patterns):
                            continue
                        put(i, pos, target)
        elif kind == 'structure':
            struct = Structure.load(_structure_path(op, plan_dir))
            rot = op.get('rotation', 0)
            include_air = op.get('includeAir', True)
            at = tuple(op['at'])
            for local, state, be in struct.cells():
                if not include_air and block_name(state) == AIR:
                    continue
                put(i, to_world(struct.size, at, rot, local), Cell.from_tag(rotate_state(state, rot), be))
        elif kind in terrain.HELPERS:
            terrain.RUN[kind](op, get, lambda pos, cell, i=i: put(i, pos, cell))
        elif kind == 'terrain':
            surface = Cell(normalize(op['surface']))
            under = Cell(normalize(op.get('under', op['surface'])))
            core = Cell(normalize(op.get('core', op.get('under', op['surface']))))
            depth = op.get('underDepth', 3)
            ox, oz = op['origin']
            for dz, row in enumerate(op['heights']):
                for dx, h in enumerate(row):
                    if h is None:
                        continue
                    top = op['base'] + h
                    for y in range(op['floor'], top + 1):
                        put(i, (ox + dx, y, oz + dz), surface if y == top else under if y >= top - depth else core)
                    for y in range(max(top + 1, op['floor']), op.get('clearTo', top) + 1):
                        put(i, (ox + dx, y, oz + dz), AIR_CELL)
    changes = {}
    for pos, after in overlay.items():
        before = world.get(pos)
        if after != before:
            changes[pos] = (before, after, writer[pos])
    return changes


# ---------------------------------------------------------------- locks

def _inside(pos, lock):
    if 'center' in lock:
        (cx, cz), r, (y0, y1) = lock['center'], lock['radius'], lock['y']
        return y0 <= pos[1] <= y1 and (pos[0] - cx) ** 2 + (pos[2] - cz) ** 2 <= r * r
    return all(lock['min'][i] <= pos[i] <= lock['max'][i] for i in range(3))


def scope(plan, world, manifest=None):
    """(locks, protect, problems) that govern a plan. A plan naming a section needs the map manifest: its allow
    region is the section's bounds cut down by the plan's own allow locks, the manifest's protect locks are added as
    denies, the pass must be the section's next one, and expectSection must match the section's digest in world.
    A plan without a section keeps its own locks, plus the manifest's protect locks when one is given."""
    from .manifest import plan_scope, protect_locks, section_digests
    if 'section' not in plan:
        return plan['locks'], (protect_locks(manifest) if manifest else ()), []
    if manifest is None:
        return plan['locks'], (), [f'plan names section {plan["section"]}; give the map manifest (--map) so its '
                                   'bounds and protect locks apply']
    locks, protect, problems = plan_scope(plan, manifest)
    if 'expectSection' in plan and not problems:
        now = section_digests(manifest, world, {plan['section']})[plan['section']]
        if now != plan['expectSection']:
            problems.append(f'section {plan["section"]} digest {now[:12]} is not the expected '
                            f'{plan["expectSection"][:12]}; the section changed since the plan was made, re-plan it')
    return locks, protect, problems


def lock_violations(plan, changes, protect=(), locks=None):
    """Changes outside every allow region, or inside any deny region of the plan or of protect (map-owned deny
    regions such as stations, spawns and capture points; boxes or cylinders). locks overrides plan['locks'] (see
    scope). Returns a list of messages, at most one per lock plus one for the allow check."""
    locks = plan['locks'] if locks is None else locks
    allows = [l for l in locks if l['mode'] == 'allow']
    denies = [l for l in locks if l['mode'] == 'deny'] + [dict(l, mode='deny') for l in protect]
    out = []
    outside = [p for p in changes if not any(_inside(p, l) for l in allows)]
    if outside:
        out.append(f'{len(outside)} changed block(s) outside every allow region, first {min(outside)}')
    for lock in denies:
        hit = [p for p in changes if _inside(p, lock)]
        if hit:
            reason = f' ({lock["reason"]})' if lock.get('reason') else ''
            out.append(f'{len(hit)} changed block(s) inside deny region {lock["id"]}{reason}, first {min(hit)}')
    limit = plan.get('limits', {}).get('maxChangedBlocks', DEFAULT_MAX_CHANGED)
    if len(changes) > limit:
        out.append(f'{len(changes)} changed blocks is over limits.maxChangedBlocks {limit}')
    return out


# ---------------------------------------------------------------- dry-run summary

def summarize(plan, world, changes, violations):
    per_op = collections.Counter(i for _, _, i in changes.values())
    per_cols = collections.defaultdict(set)
    for pos, (_, _, i) in changes.items():
        per_cols[i].add((pos[0], pos[2]))
    placed, removed = collections.Counter(), collections.Counter()
    for before, after, _ in changes.values():
        if after != AIR_CELL:
            placed[block_name(after.state)] += 1
        if before != AIR_CELL:
            removed[block_name(before.state)] += 1
    predicted = world.copy()
    for pos, (_, after, _) in changes.items():
        predicted.set(pos, after)
    box = None
    if changes:
        xs, ys, zs = zip(*changes)
        box = [[min(xs), min(ys), min(zs)], [max(xs), max(ys), max(zs)]]
    return {
        'plan': plan['id'], 'planSha256': plan_sha(plan), 'world': world.name,
        'worldBefore': world.digest(), 'worldAfter': predicted.digest(),
        'changed': len(changes), 'box': box,
        'operations': [{'index': i, 'op': op['op'], 'note': op.get('note', ''), 'changed': per_op.get(i, 0),
                        'columns': len(per_cols.get(i, ()))}
                       for i, op in enumerate(plan['operations'])],
        'placed': dict(sorted(placed.items(), key=lambda kv: (-kv[1], kv[0]))),
        'removed': dict(sorted(removed.items(), key=lambda kv: (-kv[1], kv[0]))),
        'violations': violations, 'ok': not violations,
    }


def format_summary(s):
    lines = [f'plan {s["plan"]} ({s["planSha256"][:12]}) on world {s["world"]}',
             f'world {s["worldBefore"][:12]} -> {s["worldAfter"][:12]}; {s["changed"]} block(s) change'
             + (f' in box {s["box"][0]}..{s["box"][1]}' if s['box'] else '')]
    for op in s['operations']:
        lines.append(f'  [{op["index"]}] {op["op"]:<9} {op["changed"]:>7} changed in {op.get("columns", 0):>5} columns  {op["note"]}')
    for title, key, sign in (('placed', 'placed', '+'), ('removed', 'removed', '-')):
        if s[key]:
            lines.append(f'  {title}: ' + ', '.join(f'{sign}{n} {name}' for name, n in s[key].items()))
    if s['violations']:
        lines.append('REFUSED:')
        lines += [f'  {v}' for v in s['violations']]
    else:
        lines.append('locks ok')
    return '\n'.join(lines)


# ---------------------------------------------------------------- dry-run, apply, rollback

def _checked(plan, world, plan_dir, manifest):
    locks, protect, problems = scope(plan, world, manifest)
    changes = compile_plan(plan, world, plan_dir)
    summary = summarize(plan, world, changes, problems + lock_violations(plan, changes, protect, locks))
    if 'section' in plan:
        summary['section'], summary['pass'] = plan['section'], plan['pass']
    return changes, summary


def dry_run(plan, world, plan_dir=None, manifest=None):
    """Summary of what the plan would change; the world is not modified. manifest is the map manifest: needed when
    the plan names a section, and its protect locks apply to every plan."""
    return _checked(plan, world, plan_dir, manifest)[1]


def apply(plan, world, plan_dir=None, manifest=None, now=None):
    """Applies the plan to world in place and returns (summary, undo record). Raises PlanError and leaves the world
    untouched on a lock violation, a limit, a pass out of order, or a section that no longer matches expectSection."""
    changes, summary = _checked(plan, world, plan_dir, manifest)
    if not summary['ok']:
        raise PlanError(summary['violations'])
    if 'section' in plan:
        from .manifest import section_digests
        section_before = section_digests(manifest, world, {plan['section']})[plan['section']]
    for pos, (_, after, _) in changes.items():
        world.set(pos, after)
    assert world.digest() == summary['worldAfter']
    if 'section' in plan:
        extra = {'section': plan['section'], 'pass': plan['pass'], 'sectionBefore': section_before,
                 'sectionAfter': section_digests(manifest, world, {plan['section']})[plan['section']]}
    else:
        extra = {}
    undo = {
        'format': UNDO_FORMAT, 'plan': plan['id'], 'planSha256': summary['planSha256'], 'author': plan['author'],
        'world': world.name, 'worldBefore': summary['worldBefore'], 'worldAfter': summary['worldAfter'],
        'appliedAt': (now or datetime.datetime.now(datetime.timezone.utc)).strftime('%Y-%m-%dT%H:%M:%SZ'),
        **extra,
        'changes': [[*pos, before.to_json(), after.to_json()] for pos, (before, after, _) in sorted(changes.items())],
    }
    return summary, undo


def rollback(undo, world):
    """Restores every cell of the undo record. Refuses, leaving the world untouched, when any of those cells no
    longer holds what the plan wrote (someone edited it since); edits elsewhere are kept. Returns the count."""
    if undo.get('format') != UNDO_FORMAT:
        raise PlanError([f'undo format {undo.get("format")!r}, expected {UNDO_FORMAT}'])
    rows = [(tuple(r[:3]), Cell.from_json(r[3]), Cell.from_json(r[4])) for r in undo['changes']]
    conflicts = [pos for pos, _, after in rows if world.get(pos) != after]
    if conflicts:
        raise PlanError([f'{len(conflicts)} block(s) changed since plan {undo["plan"]} was applied, first {min(conflicts)}; '
                         'rollback refused'])
    for pos, before, _ in rows:
        world.set(pos, before)
    return len(rows)


def write_undo(path, undo):
    path = pathlib.Path(path)
    if path.exists():
        raise PlanError([f'undo file {path} already exists; refusing to overwrite an undo record'])
    write_json_atomic(path, undo)
