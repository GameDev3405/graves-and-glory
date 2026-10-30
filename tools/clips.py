"""Hand-keyed clips (animation rework Task 11): converts the Blockbench animations in art/clips/<rig>/<family>.bbmodel to
src/main/resources/assets/gravesandglory/clips/<rig>/<family>/<clip>.json, the clips ClipLibrary loads, and checks them.

A clip project is a troop project ('free' format, the [180, 0, 0] display group `model`, one group per rig bone), so a
keyframe's degrees and pixels are the bone's rig-space channels as they stand: ModelPart convention, y down, rotations
added to the rest rotation in ZYX order. Markers are lines in the Effects animator's timeline keyframes; `impact` must
sit on a tick. Sampling replicates Blockbench 5.2.2's BoneAnimator.interpolate (js/animations/timeline_animators.js) and
Keyframe.getBezierLerp / getCatmullromLerp (js/animations/keyframe.js); Clip.java samples the same way.

Checks: every animated bone is in art/rigs/<rig>.json; key and marker times lie within the clip, one key per time per
channel; each stab, slash or blunt attackVariants row of a family with a project has a clip of its id, its impact on the
row's impactTicks; a death clip does not loop, and its last frame keeps every bone pivot and box corner within 0.1 px of
the ground; a looping clip ends where it starts, within 0.01.

  python tools/clips.py            write the clip JSON (and remove clips no project holds)
  python tools/clips.py --verify   rebuild in memory and compare with the committed JSON"""
import json
import math
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from bbmodel import mat_mul, mat_vec, rotation

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = 'art/clips'
OUT = 'src/main/resources/assets/gravesandglory/clips'
RIGS = 'art/rigs'
SHEET = 'sheets/design.json'
FORMAT = 1
TICKS = 20
EPSILON = 1 / 1200          # BoneAnimator.interpolate: a key this close to the time is read, not interpolated
NAME = re.compile(r'^[a-z][a-z0-9_]*$')
MELEE = {'stab', 'slash', 'blunt'}
INTERPOLATIONS = ('linear', 'catmullrom', 'bezier', 'step')
CHANNELS = ('rotation', 'position')
HANDLES = {'lt': (-0.1,) * 3, 'lv': (0,) * 3, 'rt': (0.1,) * 3, 'rv': (0,) * 3}   # Blockbench's defaults for a key's bezier handles
GROUND_SLACK = 0.1
LOOP_SLACK = 0.01
TIME_SLACK = 1e-6          # float noise in a saved time


class ClipError(Exception):
    pass


def number(v, where):
    """A data point value as Blockbench's Molang parser reads a plain number; anything else is an expression clips refuse."""
    if isinstance(v, bool):
        raise ClipError(f'{where}: {v!r} is not a number')
    if isinstance(v, (int, float)):
        f = float(v)
    elif isinstance(v, str):
        if not v.strip():
            return 0.0
        try:
            f = float(v)
        except ValueError:
            raise ClipError(f'{where}: {v!r} is Molang; clips take plain numbers') from None
    elif v is None:
        return 0.0
    else:
        raise ClipError(f'{where}: {v!r} is not a number')
    if not math.isfinite(f):
        raise ClipError(f'{where}: {v!r} is not finite')
    return f


def triple(v, where):
    if not isinstance(v, (list, tuple)) or len(v) != 3:
        raise ClipError(f'{where}: expected [x, y, z], got {v!r}')
    return [number(a, where) for a in v]


# ---- Blockbench project -> clip JSON

def convert(project, rig, where):
    """The clips in one project as {name: clip JSON}; raises ClipError naming the first problem."""
    fmt = (project.get('meta') or {}).get('model_format')
    if fmt != 'free':
        raise ClipError(f"{where}: model_format {fmt!r}; clip projects are troop projects in the 'free' format")
    groups = {g['uuid']: g.get('name') for g in project.get('groups') or [] if isinstance(g, dict) and 'uuid' in g}
    bones = {b['name'] for b in rig['bones']}
    clips = {}
    for a in project.get('animations') or []:
        name = a.get('name')
        if not isinstance(name, str) or not NAME.match(name):
            raise ClipError(f'{where}: animation name {name!r} must be lowercase letters, digits and _')
        at = f'{where} {name}'
        if name in clips:
            raise ClipError(f'{at}: two animations share the name')
        length = number(a.get('length'), at + ' length')
        if length <= 0:
            raise ClipError(f'{at}: length {length} s; a clip needs a length')
        loop = a.get('loop', 'once')
        if loop not in ('once', 'hold', 'loop'):
            raise ClipError(f'{at}: loop mode {loop!r}')
        clip = {'format': FORMAT, 'clip': name, 'length': length, 'loop': loop == 'loop', 'markers': {}, 'bones': {}}
        for uid, an in (a.get('animators') or {}).items():
            keys = an.get('keyframes') or []
            if uid == 'effects' or an.get('type') == 'effect':
                for k in keys:
                    if k.get('channel') != 'timeline':
                        raise ClipError(f"{at}: a {k.get('channel')} keyframe; clips carry only timeline markers")
                    t = number(k.get('time'), at + ' marker time')
                    for dp in k.get('data_points') or []:
                        for line in str(dp.get('script') or '').splitlines():
                            line = line.strip()
                            if not line or line.startswith('//'):
                                continue
                            if not NAME.match(line):
                                raise ClipError(f'{at}: timeline line {line!r} at {t} s is not a marker name')
                            if line in clip['markers']:
                                raise ClipError(f'{at}: marker {line!r} appears twice')
                            clip['markers'][line] = t
                continue
            if not keys:
                continue
            if an.get('type', 'bone') != 'bone':
                raise ClipError(f"{at}: a {an.get('type')!r} animator; clips animate bones")
            if uid not in groups:
                raise ClipError(f"{at}: animator {an.get('name')!r} has no group in the project")
            bone = groups[uid]
            if bone not in bones:
                raise ClipError(f"{at}: bone {bone!r} is not in {RIGS}/{rig['rig']}.json")
            if an.get('rotation_global') or an.get('quaternion_interpolation'):
                raise ClipError(f'{at}: {bone} uses global rotation or quaternion interpolation; clips keep local Euler keys')
            out = clip['bones'].setdefault(bone, {})
            for k in keys:
                ch = k.get('channel')
                if ch not in CHANNELS:
                    raise ClipError(f'{at}: {bone} has a {ch} keyframe; clips key rotation and position only')
                out.setdefault(ch, []).append(key(k, f'{at} {bone} {ch}'))
            for ch in out:
                out[ch].sort(key=lambda k: k['t'])
        clip['bones'] = {b: clip['bones'][b] for b in sorted(clip['bones'])}
        parse(clip, at)
        clips[name] = clip
    return clips


def key(k, where):
    t = number(k.get('time'), where + ' time')
    i = k.get('interpolation', 'linear')
    if i not in INTERPOLATIONS:
        raise ClipError(f'{where}: interpolation {i!r} at {t} s')
    dps = k.get('data_points') or []
    if len(dps) not in (1, 2):
        raise ClipError(f'{where}: {len(dps)} data points at {t} s')
    pts = [[number(dp.get(a), f'{where} at {t} s') for a in 'xyz'] for dp in dps]
    out = {'t': t, 'i': i, 'v': pts[0]}
    if len(pts) == 2:
        out['post'] = pts[1]
    if i == 'bezier':
        for h, field in (('lt', 'bezier_left_time'), ('lv', 'bezier_left_value'), ('rt', 'bezier_right_time'), ('rv', 'bezier_right_value')):
            out[h] = triple(k.get(field, list(HANDLES[h])), f'{where} {field} at {t} s')
    return out


# ---- clip JSON -> sampling (Clip.java mirrors this)

def parse(clip, where=None):
    """A clip JSON checked and filled in for sampling: every key has post and handles; impact is in ticks, -1 if unmarked."""
    where = where or str(clip.get('clip'))
    if clip.get('format') != FORMAT:
        raise ClipError(f"{where}: clip format {clip.get('format')!r}, expected {FORMAT}")
    length = number(clip.get('length'), where + ' length')
    markers = {str(m): number(t, f'{where} marker {m}') for m, t in (clip.get('markers') or {}).items()}
    impact = -1
    if 'impact' in markers:
        ticks = markers['impact'] * TICKS
        if abs(ticks - round(ticks)) > 1e-3:
            raise ClipError(f"{where}: impact marker at {markers['impact']} s is not on a tick; snap the timeline to {TICKS}")
        impact = round(ticks)
    for m, t in markers.items():
        if not -TIME_SLACK <= t <= length + TIME_SLACK:
            raise ClipError(f"{where}: marker {m!r} at {t} s is outside the clip's 0 to {length} s")
    bones = {}
    for bone, chans in (clip.get('bones') or {}).items():
        bones[bone] = {}
        for ch, keys in chans.items():
            if ch not in CHANNELS:
                raise ClipError(f'{where}: {bone} channel {ch!r}')
            filled = []
            for k in keys:
                if k.get('i') not in INTERPOLATIONS:
                    raise ClipError(f"{where}: {bone} {ch} interpolation {k.get('i')!r}")
                f = {'t': number(k['t'], where), 'i': k['i'], 'v': triple(k['v'], where), 'split': 'post' in k}
                f['post'] = triple(k['post'], where) if 'post' in k else f['v']
                for h, d in HANDLES.items():
                    f[h] = triple(k[h], where) if h in k else list(d)
                filled.append(f)
            filled.sort(key=lambda k: k['t'])
            for a, b in zip(filled, filled[1:]):
                if a['t'] == b['t']:
                    raise ClipError(f"{where}: {bone} {ch} has two keys at {a['t']} s")
            for k in filled:
                if not -TIME_SLACK <= k['t'] <= length + TIME_SLACK:
                    raise ClipError(f"{where}: {bone} {ch} key at {k['t']} s is outside the clip's 0 to {length} s")
            bones[bone][ch] = filled
    return {'clip': clip.get('clip'), 'length': length, 'loop': bool(clip.get('loop')), 'markers': markers, 'impact': impact, 'bones': bones}


def clamp(v, lo, hi):   # Math.clamp: NaN goes to lo
    if v > hi:
        v = hi
    if v < lo or v != v:
        v = lo
    return v


def cubic(t, p0, p1, p2, p3):   # three.js CubicBezier, term for term
    k = 1 - t
    return k * k * k * p0 + 3 * k * k * t * p1 + 3 * (1 - t) * t * t * p2 + t * t * t * p3


def bezier_lerp(before, after, axis, alpha):
    """Keyframe.getBezierLerp: the curve as 201 points, then a lerp between the two points nearest the time."""
    vb, va = before['post'][axis], after['v'][axis]
    gap = after['t'] - before['t']
    hb = clamp(before['rt'][axis] or 0, 0, gap)
    ha = clamp(after['lt'][axis] or 0, -gap, 0)
    xs = (before['t'], before['t'] + hb, after['t'] + ha, after['t'])
    ys = (vb, vb + before['rv'][axis] or 0, va + after['lv'][axis] or 0, va)
    time = before['t'] + (after['t'] - before['t']) * alpha
    pts = [(cubic(d / 200, *xs), cubic(d / 200, *ys)) for d in range(201)]
    c = min(range(201), key=lambda i: (abs(pts[i][0] - time), i))
    s = min((i for i in range(201) if i != c), key=lambda i: (abs(pts[i][0] - time), i))
    (cx, cy), (sx, sy) = pts[c], pts[s]
    d = time - cx   # Math.getLerp divides by zero when the two points share an x: +-Infinity or NaN, which the clamp settles
    return cy + (sy - cy) * clamp(d / (sx - cx) if sx != cx else math.copysign(math.inf, d) if d else math.nan, 0, 1)


def catmullrom_lerp(bp, before, after, ap, axis, alpha):
    """Keyframe.getCatmullromLerp on three.js SplineCurve.getPoint."""
    pts = []
    if bp is not None and not before['split']:
        pts.append(bp['post'][axis])
    pts.append(before['post'][axis])
    pts.append(after['v'][axis])
    if ap is not None and not after['split']:
        pts.append(ap['v'][axis])
    n = len(pts)
    t = (alpha + (1 if bp is not None else 0)) / (n - 1)
    p = (n - 1) * t
    i = math.floor(p)
    w = p - i
    p0, p1 = pts[i if i == 0 else i - 1], pts[i]
    p2, p3 = pts[n - 1 if i > n - 2 else i + 1], pts[n - 1 if i > n - 3 else i + 2]
    v0, v1 = (p2 - p0) * 0.5, (p3 - p1) * 0.5
    t2 = w * w
    t3 = w * t2
    return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * w + p1


def channel(keys, loop, time):
    """BoneAnimator.interpolate for one channel, all three axes."""
    bi = ai = None
    for n, k in enumerate(keys):
        if k['t'] < time:
            if bi is None or k['t'] > keys[bi]['t']:
                bi = n
        elif ai is None or k['t'] < keys[ai]['t']:
            ai = n
    before = keys[bi] if bi is not None else None
    after = keys[ai] if ai is not None else None
    if before and abs(time - before['t']) < EPSILON:
        return list(before['v'])
    if after and abs(after['t'] - time) < EPSILON:
        return list(after['v'])
    if before and (before['i'] == 'step' or not after):
        return list(before['post'])
    if after and not before:
        return list(after['v'])
    if not before:
        return [0.0, 0.0, 0.0]
    alpha = (time - before['t']) / (after['t'] - before['t'])
    if before['i'] == 'linear' and after['i'] in ('linear', 'step'):
        return [b + (a - b) * alpha for b, a in zip(before['post'], after['v'])]
    if 'catmullrom' in (before['i'], after['i']):
        bp = keys[bi - 1] if bi >= 1 else None
        ap = keys[bi + 2] if bi + 2 < len(keys) else None
        if loop and len(keys) >= 3:
            bp = keys[-2] if bp is None else bp
            ap = keys[1] if ap is None else ap
        return [catmullrom_lerp(bp, before, after, ap, a, alpha) for a in range(3)]
    return [bezier_lerp(before, after, a, alpha) for a in range(3)]


def sample(clip, seconds, wrap=True):
    """{bone: [rx, ry, rz, tx, ty, tz]} at a time: a looping clip wraps (unless wrap is False), any other holds its ends."""
    length = clip['length']
    t = seconds % length if clip['loop'] and wrap else min(max(seconds, 0.0), length)
    out = {}
    for bone, chans in clip['bones'].items():
        r = channel(chans['rotation'], clip['loop'], t) if chans.get('rotation') else [0.0] * 3
        p = channel(chans['position'], clip['loop'], t) if chans.get('position') else [0.0] * 3
        out[bone] = r + p
    return out


# ---- checks

def points(rig, pose):
    """Forward kinematics: (bone, y) for every bone pivot and box corner, posed by {bone: 6 channels} on the rest rig."""
    world, out = {}, []
    for b in rig['bones']:
        ch = pose.get(b['name'], [0.0] * 6)
        rest = b.get('rotation', [0, 0, 0])
        r = rotation([rest[i] + ch[i] for i in range(3)])
        o = [b['pivot'][i] + ch[3 + i] for i in range(3)]
        if b['parent']:
            pr, po = world[b['parent']]
            r, o = mat_mul(pr, r), [a + c for a, c in zip(po, mat_vec(pr, o))]
        world[b['name']] = (r, o)
        out.append((b['name'], o[1]))
        for box in b.get('boxes', []):
            for c in range(8):
                p = [box['origin'][i] + (box['size'][i] if c >> i & 1 else 0) for i in range(3)]
                out.append((b['name'], o[1] + mat_vec(r, p)[1]))
    return out


def check(clip, parsed, rig, family, variants, at):
    name = clip['clip']
    row = variants.get((family, name)) if variants is not None else None
    if row and row.get('kind') in MELEE:
        want = row.get('impactTicks')
        if parsed['impact'] < 0:
            raise ClipError(f'{at}: melee clip has no impact marker; attackVariants {name} has impactTicks {want}')
        if parsed['impact'] != want:
            raise ClipError(f"{at}: impact {parsed['impact']} ticks, but attackVariants {name} has impactTicks {want}")
    if name.startswith('death'):
        if parsed['loop']:
            raise ClipError(f'{at}: a death clip cannot loop; set its loop mode to play once or hold')
        ground = max(y for _, y in points(rig, {}))
        bone, y = max(points(rig, sample(parsed, parsed['length'], wrap=False)), key=lambda p: p[1])
        if y > ground + GROUND_SLACK:
            raise ClipError(f'{at}: the last frame puts {bone} {y - ground:.2f} px below the ground')
    if parsed['loop']:
        first, last = sample(parsed, 0, wrap=False), sample(parsed, parsed['length'], wrap=False)
        for bone in first:
            worst = max(abs(a - b) for a, b in zip(first[bone], last[bone]))
            if worst > LOOP_SLACK:
                raise ClipError(f'{at}: a loop clip whose last frame is {worst:.3f} off its first ({bone})')


def variants(sheet):
    """{(family, id): row} from the sheet's attackVariants, or None while the sheet has no impactTicks yet."""
    rows = sheet.get('attackVariants')
    if not isinstance(rows, list) or not rows or any('impactTicks' not in r for r in rows):
        return None
    return {(r['family'], r['id']): r for r in rows}


def build(root):
    """{relative output path: clip JSON} for every project under art/clips; raises ClipError on the first problem."""
    root = pathlib.Path(root)
    sheet_path = root / SHEET
    rows = variants(json.loads(sheet_path.read_text(encoding='utf-8'))) if sheet_path.is_file() else None
    if rows is None:
        print(f'clips: {SHEET} has no attackVariants impactTicks yet; impact check skipped')
    out = {}
    for path in sorted((root / SRC).glob('*/*.bbmodel')):
        rig_name, family = path.parent.name, path.stem
        where = path.relative_to(root).as_posix()
        if not NAME.match(rig_name) or not NAME.match(family):
            raise ClipError(f'{where}: rig and family names must be lowercase letters, digits and _')
        rig_path = root / RIGS / (rig_name + '.json')
        if not rig_path.is_file():
            raise ClipError(f'{where}: no rig {RIGS}/{rig_name}.json')
        rig = json.loads(rig_path.read_text(encoding='utf-8'))
        made = convert(json.loads(path.read_text(encoding='utf-8')), rig, where)
        for (fam, row), r in sorted((rows or {}).items()):
            if fam == family and r.get('kind') in MELEE and row not in made:
                raise ClipError(f"{where}: attackVariants {row!r} ({r['kind']}) of family {family} has no clip; name an animation {row}")
        for name, clip in made.items():
            check(clip, parse(clip), rig, family, rows, f'{where} {name}')
            out[f'{rig_name}/{family}/{name}.json'] = clip
    return out


def dump(clip):
    return json.dumps(clip, indent=1) + '\n'


def main(args=None, root=ROOT):
    args = sys.argv[1:] if args is None else args
    if args not in ([], ['--verify']):
        print(__doc__)
        return 2
    root = pathlib.Path(root)
    dest = root / OUT
    try:
        fresh = build(root)
    except ClipError as e:
        print(f'clips: {e}')
        return 1
    have = {p.relative_to(dest).as_posix() for p in dest.rglob('*.json')} if dest.is_dir() else set()
    if args == ['--verify']:
        for rel in sorted(have - set(fresh)):
            print(f'clips: {OUT}/{rel} has no clip in {SRC}; run python tools/clips.py')
            return 1
        for rel, clip in sorted(fresh.items()):
            if rel not in have:
                print(f'clips: {OUT}/{rel} is missing; run python tools/clips.py')
                return 1
            if json.loads((dest / rel).read_text(encoding='utf-8')) != json.loads(dump(clip)):
                print(f'clips: {OUT}/{rel} differs from {SRC}; run python tools/clips.py')
                return 1
        print(f'clips: {len(fresh)} clips verified')
        return 0
    for rel in sorted(have - set(fresh)):
        (dest / rel).unlink()
    for rel, clip in sorted(fresh.items()):
        path = dest / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(dump(clip), encoding='utf-8', newline='\n')
    for d in sorted((p for p in dest.rglob('*') if p.is_dir()), reverse=True) if dest.is_dir() else ():
        if not any(d.iterdir()):
            d.rmdir()
    print(f'clips: wrote {len(fresh)} clips')
    return 0


if __name__ == '__main__':
    sys.exit(main())
