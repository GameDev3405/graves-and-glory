"""Builds the troops' own models and skins (Phase 2 Task 5) from art/ into src/main/resources/assets/gravesandglory/:
models/troop/<troop>.json, rig/<rig>.json, weapon/<id>.json and index.json (every troop, rig, weapon and output), and
textures/entity/troop/<troop>_s<stage>.png, _veteran, _e (glow), _o (outer layer), weapon/<id>.png (and _e), and one
team_cloth_<side>.png per factions row plus team_cloth_linen.png (no side) and team_cloth_company.png (the company's navy,
which portraits wear: unit identity rule 8 as Kort ruled it, Needs Kort 11).

  python tools/build_skins.py                     export everything, and remove the outputs the previous index.json
                                                  listed that are no longer built (never a file no index listed)
  python tools/build_skins.py --verify            rebuild into a temp dir and compare: a PNG by its size and RGBA
                                                  pixels (another Pillow or zlib may encode the same pixels in other
                                                  bytes), anything else byte for byte; exit 1 naming the first
                                                  difference
  python tools/build_skins.py --template <troop>  write work/troops/<troop>_template.png: every skin box face of the
                                                  troop in a flat colour, the sheet to paint over

Sources (sizes in model pixels, y down, the ground at y 24, angles in degrees; a "note" key is allowed anywhere):
  art/rigs/<rig>.json      rig, size [w,h], outer (the outer twin's dilation), hands {right, left} (bone names), bones[]
                           of {name, parent, pivot, rotation?, boxes[{origin, size, uv, mirror?}]}, parents first. A pivot
                           is relative to the parent's pivot, a box origin to its own bone's pivot (the ModelPart way).
  art/parts/<part>.json    part, bone (the default anchor), team, tag ("" or "helm"), pivot? and rotation? (in the
                           anchor's frame), scale? (the part's own, times the troop's for it), boxes[]. Box UVs are
                           relative to the troop's uv for the part, except that a team part's UVs are absolute on the
                           cloth sheet.
  art/weapons/<id>.json    weapon, size, boxes[], grip {offset, rotation} (the weapon's place in the hand bone's frame),
                           with <id>.png and, for a lit weapon, <id>_e.png.
  art/troops/<troop>.json  troop, rig, size?, bones {<name>: {pivot?, boxes?, scale?}}, hunch? (biped), idleArms?,
                           parts[] of {part, name?, bone?, uv, offset?, rotation?, scale?, stages?}, skins {<stage>: file},
                           veteran?, glow?, glowFrom?, outer?, weapon (an id or none), hand, portrait.
  art/team/team_cloth.png  the greyscale cloth template; its three greys take the side colour x1.0, x0.8 and x0.6, every
                           other pixel keeps its paint. art/team/glyph_<glyph>.png: full-sheet masks, stamped in white.

Proportions are baked here, never scaled at runtime: a bone scale k scales the bone's boxes about its pivot (origin
k*origin + size*(k-1)/2, dilation size*(k-1)/2, the UV size stays the integer size) and moves its children's pivots by
k; a part takes its anchor bone's scale times its own; root then shifts so the lowest leg box bottom sits at y 24.

Deterministic: every collection is walked in file or sorted order. Like build_ui_art.py it re-executes itself with
PYTHONHASHSEED=0 and runs under Python 3.12. Pillow is a dev tool only."""
import io
import json
import math
import os
import re
import subprocess
import sys
import tempfile
import colorsys
from pathlib import Path

sys.dont_write_bytecode = True
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = 'src/main/resources/assets/gravesandglory'
MODELS = 'models/troop'
TEXTURES = 'textures/entity/troop'
NAMESPACE = 'gravesandglory'
FORMAT = 1
# The rig of each sheet entity (TroopModels.RIGS in the client).
RIGS = {'husk': 'biped', 'zombie': 'biped', 'skeleton': 'biped', 'stray': 'biped', 'wither_skeleton': 'biped',
        'drowned': 'biped', 'zombified_piglin': 'biped', 'vindicator': 'biped', 'pillager': 'biped', 'evoker': 'biped',
        'wolf': 'hound', 'polar_bear': 'hound', 'ravager': 'hound', 'spider': 'spider', 'iron_golem': 'colossus'}
BIPED_BONES = ('root', 'body', 'head', 'right_arm', 'right_hand', 'left_arm', 'left_hand', 'arms_folded', 'right_leg', 'left_leg')
RIG_BONES = {
    'biped': BIPED_BONES,
    'hound': ('root', 'body', 'chest', 'head', 'jaw', 'front_left_leg', 'front_right_leg', 'tail', 'back_left_leg', 'back_right_leg'),
    'spider': ('root', 'thorax', 'head', 'fangs', 'abdomen') + tuple('%s_leg_%d' % (s, n) for s in ('left', 'right') for n in (1, 2, 3, 4)),
    'colossus': BIPED_BONES,
}
LEGS = {'right_leg', 'left_leg', 'front_left_leg', 'front_right_leg', 'back_left_leg', 'back_right_leg'} | \
       {'%s_leg_%d' % (s, n) for s in ('left', 'right') for n in (1, 2, 3, 4)}
MODEL_KEYS = ('format', 'troop', 'rig', 'size', 'bones', 'skins', 'veteran', 'glow', 'glowFrom', 'outer', 'weapon',
              'hand', 'idleArms', 'portrait')
VANILLA_KEYS = ('parent', 'elements', 'textures', 'display', 'overrides', 'gui_light', 'ambientocclusion')
TROOP_KEYS = {'troop', 'rig', 'size', 'bones', 'hunch', 'idleArms', 'parts', 'skins', 'veteran', 'glow', 'glowFrom',
              'outer', 'weapon', 'hand', 'portrait', 'note'}
ENTRY_KEYS = {'part', 'name', 'bone', 'uv', 'offset', 'rotation', 'scale', 'stages', 'note'}
PART_KEYS = {'part', 'bone', 'team', 'tag', 'pivot', 'rotation', 'scale', 'boxes', 'note'}
WEAPON_KEYS = {'weapon', 'size', 'boxes', 'grip', 'note'}
RIG_KEYS = {'rig', 'size', 'outer', 'hands', 'bones', 'note'}
IDLE_ARMS = ('hang', 'forward', 'folded')
PORTRAIT = re.compile(r'(standard|tall \d+|wide \d+|beast \d+( yaw -?\d+)?)')
GREYS = {(160, 160, 160): 1.0, (128, 128, 128): 0.8, (96, 96, 96): 0.6}   # the cloth template's field greys
LINEN = (0xCF, 0xC6, 0xB0)
COMPANY = (0x2A, 0x37, 0x47)   # the Hollow's navy (work/ui-concepts/storybook/NOTES.md), the company's cloth
GLYPH = (0xF3, 0xEF, 0xE4)   # the pennant glyph's white, the white of the charges
GLOW_CAP = {'epic': 16, 'legendary': 16}   # any other rarity: 6
TINT_SATURATION, TINT_VALUE, TINT_HUE = 0.45, 0.60, 45.0
ONE = (1.0, 1.0, 1.0)


def fail(why):
    print('build_skins: FAIL ' + why)
    sys.exit(1)


def num(v):
    """A number as the JSON keeps it: four decimals, an integer when it is one, never -0."""
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def is_num(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v)


def vec(v, n, what, ints=False, minimum=None):
    if not (isinstance(v, list) and len(v) == n and all(is_num(c) and (not ints or float(c).is_integer()) for c in v)):
        fail('%s must be a list of %d %s' % (what, n, 'whole numbers' if ints else 'numbers'))
    if minimum is not None and any(c < minimum for c in v):
        fail('%s must not go below %s' % (what, minimum))
    return [int(c) for c in v] if ints else [float(c) for c in v]


def scale3(v, what):
    if is_num(v):
        v = [v, v, v]
    k = vec(v, 3, what + ' scale')
    if any(c <= 0 for c in k):
        fail(what + ' scale must be above 0')
    return tuple(k)


def keys(d, allowed, what):
    if not isinstance(d, dict):
        fail(what + ' must be a JSON object')
    extra = sorted(set(d) - allowed)
    if extra:
        fail('%s has unknown key %s' % (what, extra[0]))


def read_json(path):
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except (OSError, ValueError) as e:
        fail('%s: %s' % (path.as_posix(), e))


def read_png(path, what):
    try:
        with Image.open(path) as im:
            return im.convert('RGBA')
    except OSError as e:
        fail('%s: %s (%s)' % (what, path.name, e))


class Catalog:
    """Every source under art/ plus the sheet rows the build reads (troops, factions, evolutions)."""

    def __init__(self, root):
        self.root = root
        self.rigs, self.parts, self.weapons, self.troops = {}, {}, {}, {}
        self.sheet, self.factions, self.stages = {}, {}, {}
        self.template, self.glyphs = None, {}


def sources(folder, key):
    out = {}
    for path in sorted(folder.glob('*.json')):
        data = read_json(path)
        if not isinstance(data, dict) or data.get(key) != path.stem:
            fail('%s: "%s" must be %s, the file name' % (path.as_posix(), key, path.stem))
        out[path.stem] = data
    return out


def load(root: Path) -> Catalog:
    root = Path(root)
    cat = Catalog(root)
    design = read_json(root / 'sheets/design.json')
    cat.sheet = {r['id']: r for r in design.get('troops', [])}
    cat.factions = {f['id']: f for f in design.get('factions', [])}
    for reserved in ('linen', 'company'):
        if reserved in cat.factions:
            fail('factions row %s: linen and company name the cloths with no side' % reserved)
    lines = {}
    for e in design.get('evolutions', []):
        lines.setdefault(e['troop'], set()).add(int(e['stage']))
    for tid, row in cat.sheet.items():
        cat.stages[tid] = sorted({1} | lines.get(row.get('evolveLine'), set()))
    art = root / 'art'
    cat.rigs = sources(art / 'rigs', 'rig')
    cat.parts = sources(art / 'parts', 'part')
    cat.weapons = sources(art / 'weapons', 'weapon')
    cat.troops = sources(art / 'troops', 'troop')
    if (art / 'team/team_cloth.png').is_file():
        cat.template = read_png(art / 'team/team_cloth.png', 'team cloth template')
        for path in sorted((art / 'team').glob('glyph_*.png')):
            cat.glyphs[path.stem[len('glyph_'):]] = read_png(path, 'glyph')
    for name in cat.rigs:
        check_rig(cat, name)
    for name in cat.parts:
        check_part(cat, name)
    team = [(pid, b['uv'], b['size']) for pid in sorted(cat.parts) if cat.parts[pid]['team']
            for b in check_boxes(cat.parts[pid]['boxes'], 'part ' + pid)]
    if team:
        check_layout(team, cat.template.size, 'the cloth sheet (every team part)')
    return cat


def check_boxes(boxes, what, sheet=None):
    if not isinstance(boxes, list):
        fail(what + ' boxes must be a list')
    out = []
    for i, b in enumerate(boxes):
        keys(b, {'origin', 'size', 'uv', 'mirror', 'note'}, '%s box %d' % (what, i))
        mirror = b.get('mirror', False)
        if not isinstance(mirror, bool):
            fail('%s box %d mirror must be true or false' % (what, i))
        out.append({'origin': vec(b.get('origin'), 3, '%s box %d origin' % (what, i)),
                    'size': vec(b.get('size'), 3, '%s box %d size' % (what, i), ints=True, minimum=0),
                    'uv': vec(b.get('uv'), 2, '%s box %d uv' % (what, i), ints=True, minimum=0),
                    'mirror': mirror, 'dilation': [0.0, 0.0, 0.0]})
    return out


def footprint(uv, size):
    """The texture rectangles a box's six faces read, as (x0, y0, x1, y1): the top row holds the top and bottom faces,
    the second row the four sides (ModelPart.Cuboid's layout)."""
    (u, v), (w, h, d) = uv, size
    return [r for r in ((u + d, v, u + d + 2 * w, v + d), (u, v + d, u + 2 * d + 2 * w, v + d + h)) if r[0] < r[2] and r[1] < r[3]]


def check_layout(owned, sheet, what):
    """owned: (owner, uv, size) per box. Every face inside the sheet, no two boxes on one texel."""
    w, h = sheet
    rects = []
    for owner, uv, size in owned:
        for r in footprint(uv, size):
            if r[0] < 0 or r[1] < 0 or r[2] > w or r[3] > h:
                fail('%s: %s has a box outside the %dx%d sheet (uv %d, %d)' % (what, owner, w, h, uv[0], uv[1]))
            rects.append((owner, r))
    for i, (a, ra) in enumerate(rects):
        for b, rb in rects[i + 1:]:
            if ra[0] < rb[2] and rb[0] < ra[2] and ra[1] < rb[3] and rb[1] < ra[3]:
                if a == b:
                    fail('%s: two boxes of %s overlap in UV at (%d, %d)' % (what, a, max(ra[0], rb[0]), max(ra[1], rb[1])))
                fail('%s: %s and %s overlap in UV at (%d, %d)' % (what, a, b, max(ra[0], rb[0]), max(ra[1], rb[1])))


def check_rig(cat, name):
    rig = cat.rigs[name]
    what = 'rig ' + name
    keys(rig, RIG_KEYS, what)
    if name not in RIG_BONES:
        fail('%s: no such skeleton; the rigs are %s' % (what, ', '.join(sorted(RIG_BONES))))
    vec(rig.get('size'), 2, what + ' size', ints=True, minimum=1)
    if not is_num(rig.get('outer')):
        fail(what + ': outer must be a number')
    bones = rig.get('bones')
    if not isinstance(bones, list):
        fail(what + ': bones must be a list')
    seen = []
    for b in bones:
        keys(b, {'name', 'parent', 'pivot', 'rotation', 'boxes', 'note'}, what + ' bone')
        n = b.get('name')
        if n in seen:
            fail('%s: duplicate bone %s' % (what, n))
        parent = b.get('parent')
        if not seen and parent != '':
            fail('%s: %s: the first bone is root, with parent ""' % (what, n))
        if seen and parent not in seen:
            fail('%s: %s has parent %s, which is not a bone listed before it' % (what, n, parent))
        vec(b.get('pivot'), 3, '%s %s pivot' % (what, n))
        if 'rotation' in b:
            vec(b['rotation'], 3, '%s %s rotation' % (what, n))
        check_boxes(b.get('boxes', []), '%s %s' % (what, n))
        seen.append(n)
    if sorted(seen) != sorted(RIG_BONES[name]) or seen[0] != 'root':
        fail('%s: the bones must be %s, root first' % (what, ', '.join(RIG_BONES[name])))
    hands = rig.get('hands')
    if not isinstance(hands, dict) or set(hands) != {'right', 'left'} or any(hands[k] not in seen for k in hands):
        fail(what + ': hands must name a bone for right and for left')
    check_layout([(b['name'], x['uv'], x['size']) for b in bones for x in check_boxes(b.get('boxes', []), what)],
                 rig['size'], what)


def check_part(cat, name):
    p = cat.parts[name]
    what = 'part ' + name
    keys(p, PART_KEYS, what)
    if not isinstance(p.get('bone'), str) or not isinstance(p.get('team'), bool) or p.get('tag') not in ('', 'helm'):
        fail(what + ': needs bone (a bone name), team (true or false) and tag ("" or "helm")')
    for k in ('pivot', 'rotation'):
        if k in p:
            vec(p[k], 3, '%s %s' % (what, k))
    scale3(p.get('scale', 1), what)
    boxes = check_boxes(p.get('boxes'), what)
    if p['team']:
        if cat.template is None:
            fail(what + ': a team part needs art/team/team_cloth.png')
        check_layout([(name, b['uv'], b['size']) for b in boxes], cat.template.size, what + ' on the cloth sheet')


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def rotation(deg):
    """The ModelPart rotation (pitch, yaw, roll in degrees) as a matrix: Z then Y then X, as Quaternionf.rotationZYX."""
    x, y, z = (math.radians(a) for a in deg)
    rx = [[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]]
    ry = [[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]]
    rz = [[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]]
    return mat_mul(mat_mul(rz, ry), rx)


def world(bones):
    """Each bone's rest transform in model space: (rotation matrix, translation)."""
    out = {}
    for b in bones:
        r, t = out.get(b['parent'], ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], [0.0, 0.0, 0.0]))
        p = b['pivot']
        t2 = [t[i] + sum(r[i][k] * p[k] for k in range(3)) for i in range(3)]
        out[b['name']] = (mat_mul(r, rotation(b['rotation'])), t2)
    return out


def corners(box):
    lo = [box['origin'][i] - box['dilation'][i] for i in range(3)]
    hi = [box['origin'][i] + box['size'][i] + box['dilation'][i] for i in range(3)]
    return [(x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]


def bounds(bones, include=lambda b: True):
    """(min, max) corners of the chosen bones' boxes in model space, rest pose."""
    w = world(bones)
    pts = []
    for b in bones:
        if not include(b):
            continue
        r, t = w[b['name']]
        for box in b['boxes']:
            for c in corners(box):
                pts.append([t[i] + sum(r[i][k] * c[k] for k in range(3)) for i in range(3)])
    if not pts:
        return None
    return [min(p[i] for p in pts) for i in range(3)], [max(p[i] for p in pts) for i in range(3)]


def bake_bones(rig, what, overrides=None, parts=(), hunch=0.0, folded=True, stages=(1,)):
    """The rig's bones with the troop's overrides and parts, scaled, hunched and set on the ground; parts first checked.
    parts: (entry, part) pairs. Returns the bone dicts, parents first."""
    bones, scale = {}, {}
    for b in rig['bones']:
        bones[b['name']] = {'name': b['name'], 'parent': b['parent'], 'part': '', 'team': False, 'tag': '', 'stages': [],
                            'pivot': [float(c) for c in b['pivot']], 'rotation': [float(c) for c in b.get('rotation', [0, 0, 0])],
                            'boxes': check_boxes(b.get('boxes', []), what)}
    for name, ov in (overrides or {}).items():
        if name not in bones:
            fail('%s: unknown bone %s (bones names a bone the %s rig lacks)' % (what, name, rig['rig']))
        keys(ov, {'pivot', 'boxes', 'scale', 'note'}, '%s bones %s' % (what, name))
        if 'pivot' in ov:
            bones[name]['pivot'] = vec(ov['pivot'], 3, '%s bones %s pivot' % (what, name))
        if 'boxes' in ov:
            bones[name]['boxes'] = check_boxes(ov['boxes'], '%s bones %s' % (what, name))
        if 'scale' in ov:
            scale[name] = scale3(ov['scale'], '%s bones %s' % (what, name))
    if not folded and 'arms_folded' in bones:
        bones['arms_folded']['boxes'] = []
    for entry, part in parts:
        pid = part['part']
        name = entry.get('name', pid)
        if not isinstance(name, str) or not re.fullmatch(r'[a-z0-9_]+', name):
            fail('%s: part %s needs a lower-case name' % (what, pid))
        if name in bones:
            fail('%s: duplicate bone %s (a part\'s bone is its name, which defaults to the part id)' % (what, name))
        anchor = entry.get('bone', part['bone'])
        if anchor not in bones:
            fail('%s: part %s hangs from unknown bone %s' % (what, name, anchor))
        if part['team']:
            origin_uv = [0, 0]
        else:
            if 'uv' not in entry:
                fail('%s: part %s needs a uv on the troop sheet' % (what, name))
            origin_uv = vec(entry['uv'], 2, '%s part %s uv' % (what, name), ints=True, minimum=0)
        want = entry.get('stages', [])
        if not isinstance(want, list) or any(s not in stages for s in want):
            fail('%s: part %s shows at stages %s, but the troop has stages %s' % (what, name, want, list(stages)))
        pivot = [a + b for a, b in zip(vec(part.get('pivot', [0, 0, 0]), 3, 'pivot'),
                                       vec(entry.get('offset', [0, 0, 0]), 3, '%s part %s offset' % (what, name)))]
        rot = vec(entry.get('rotation', part.get('rotation', [0, 0, 0])), 3, '%s part %s rotation' % (what, name))
        boxes = check_boxes(part['boxes'], 'part ' + pid)
        for b in boxes:
            b['uv'] = [origin_uv[0] + b['uv'][0], origin_uv[1] + b['uv'][1]]
        bones[name] = {'name': name, 'parent': anchor, 'part': pid, 'team': part['team'], 'tag': part['tag'],
                       'stages': sorted(want), 'pivot': pivot, 'rotation': rot, 'boxes': boxes}
        own = scale3(entry.get('scale', 1), '%s part %s' % (what, name))
        base = scale3(part.get('scale', 1), 'part ' + pid)   # the part's own scale, then the troop's for it
        k = scale.get(anchor, ONE)
        scale[name] = tuple(k[i] * base[i] * own[i] for i in range(3))
    for b in bones.values():
        k = scale.get(b['name'], ONE)
        if k == ONE:
            continue
        for box in b['boxes']:
            for i in range(3):
                grow = box['size'][i] * (k[i] - 1) / 2
                box['origin'][i] = k[i] * box['origin'][i] + grow
                box['dilation'][i] = grow
        for child in bones.values():
            if child['parent'] == b['name']:
                child['pivot'] = [child['pivot'][i] * k[i] for i in range(3)]
    if hunch:
        bones['body']['rotation'][0] += hunch
        for n in ('head', 'right_arm', 'left_arm'):
            bones[n]['rotation'][0] -= hunch
    order = list(bones.values())
    feet = bounds(order, lambda b: b['name'] in LEGS)
    if feet is None:
        fail('%s: no leg box to stand on' % what)
    bones['root']['pivot'][1] += 24 - feet[1][1]
    return order


def clean_bone(b, sheet):
    return {'name': b['name'], 'parent': b['parent'], 'part': b['part'], 'team': b['team'], 'tag': b['tag'],
            'stages': b['stages'], 'sheet': list(sheet), 'pivot': [num(c) for c in b['pivot']],
            'rotation': [num(c) for c in b['rotation']],
            'boxes': [{'origin': [num(c) for c in x['origin']], 'size': x['size'], 'uv': x['uv'], 'mirror': x['mirror'],
                       'dilation': [num(c) for c in x['dilation']]} for x in b['boxes']]}


def texture_id(name):
    return '%s:%s/%s' % (NAMESPACE, TEXTURES, name)


def troop_files(cat, troop):
    """The troop's skins by stage, and its veteran, glow and outer files ('' for none), checked against the naming rules."""
    src = cat.troops[troop]
    folder = cat.root / 'art/troops'
    if (folder / (troop + '.png')).exists():
        fail('%s: art/troops/%s.png: skins end _s<stage> or _veteran, never <troop>.png (ETF, when a player has it, '
             'pairs <name>_e.png with <name>.png)' % (troop, troop))
    stages = cat.stages[troop]
    skins = src.get('skins')
    if not isinstance(skins, dict) or '1' not in skins:
        fail('%s: skins must map stage numbers to files, with a stage 1 skin' % troop)
    own = {}
    for key, f in skins.items():
        if not key.isdigit() or int(key) not in stages:
            fail('%s: a skin for stage %s, which the evolution line lacks (stages %s)' % (troop, key, stages))
        if f != '%s_s%s.png' % (troop, key):
            fail('%s: the stage %s skin must be %s_s%s.png, not %s' % (troop, key, troop, key, f))
        if not (folder / f).is_file():
            fail('%s: %s is missing from art/troops' % (troop, f))
        own[int(key)] = f
    extra = {}
    for kind, suffix in (('veteran', '_veteran'), ('glow', '_e'), ('outer', '_o')):
        want = troop + suffix + '.png'
        f = src.get(kind, want if (folder / want).is_file() else '')
        if f and f != want:
            fail('%s: the %s file must be %s, not %s' % (troop, kind, want, f))
        if f and not (folder / f).is_file():
            fail('%s: %s is missing from art/troops' % (troop, f))
        extra[kind] = f
    return own, extra


def tint(im, what, factions):
    """Team tint rule 1: no opaque pixel with saturation over 0.45 and value over 0.60 within 45 degrees of hue of a side."""
    sides = []
    for f in factions.values():
        c = f['color']
        h, s, v = colorsys.rgb_to_hsv(((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255)
        sides.append((f['id'], h * 360))
    seen = {}
    w, h = im.size
    data = im.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = data[x, y]
            if a == 0 or (r, g, b) in seen:
                continue
            hue, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            bad = None
            if s > TINT_SATURATION and v > TINT_VALUE:
                for side, sh in sides:
                    if min(abs(hue * 360 - sh), 360 - abs(hue * 360 - sh)) < TINT_HUE:
                        bad = side
                        break
            seen[(r, g, b)] = bad
            if bad:
                fail('%s pixel (%d, %d) #%02X%02X%02X is within %d degrees of the %s side colour (saturation %.2f, value '
                     '%.2f); team colour belongs on the cloth sheet' % (what, x, y, r, g, b, TINT_HUE, bad, s, v))


def lit(im):
    data = im.convert('RGBA').tobytes()
    return sum(1 for i in range(3, len(data), 4) if data[i])


def png_bytes(im):
    """The PNG of an image as RGBA, every transparent texel (0, 0, 0, 0): it carries no colour, as the glow pass adds RGB."""
    data = bytearray(im.convert('RGBA').tobytes())
    for i in range(0, len(data), 4):
        if not data[i + 3]:
            data[i:i + 3] = b'\x00\x00\x00'
    buf = io.BytesIO()
    Image.frombytes('RGBA', im.size, bytes(data)).save(buf, 'PNG')
    return buf.getvalue()


def dumps(obj):
    """Fixed formatting: one top-level key per line and one list element per line for lists of objects; LF."""
    def one(v):
        return json.dumps(v, separators=(',', ':'), ensure_ascii=True)
    lines = []
    for k, v in obj.items():
        if isinstance(v, list) and v and all(isinstance(x, (dict, str)) for x in v):
            lines.append('%s:[\n%s\n]' % (one(k), ',\n'.join(one(x) for x in v)))
        else:
            lines.append('%s:%s' % (one(k), one(v)))
    return ('{\n' + ',\n'.join(lines) + '\n}\n').encode('utf-8')


def weapon_outputs(cat, wid):
    w = cat.weapons[wid]
    what = 'weapon ' + wid
    keys(w, WEAPON_KEYS, what)
    size = vec(w.get('size'), 2, what + ' size', ints=True, minimum=1)
    boxes = check_boxes(w.get('boxes'), what)
    if not boxes:
        fail(what + ': no boxes')
    check_layout([(wid, b['uv'], b['size']) for b in boxes], size, what)
    grip = w.get('grip')
    keys(grip, {'offset', 'rotation', 'note'}, what + ' grip')
    folder = cat.root / 'art/weapons'
    if not (folder / (wid + '.png')).is_file():
        fail('%s: %s.png is missing from art/weapons' % (what, wid))
    tex = read_png(folder / (wid + '.png'), what)
    if list(tex.size) != size:
        fail('%s: %s.png is %dx%d, not the weapon size %dx%d' % (what, wid, tex.size[0], tex.size[1], size[0], size[1]))
    tint(tex, '%s: %s.png' % (what, wid), cat.factions)
    glow = None
    if (folder / (wid + '_e.png')).is_file():
        glow = read_png(folder / (wid + '_e.png'), what)
        if glow.size != tex.size:
            fail('%s: %s_e.png is not the weapon size' % (what, wid))
    model = {'format': FORMAT, 'weapon': wid, 'size': size,
             'grip': {'offset': [num(c) for c in vec(grip.get('offset'), 3, what + ' grip offset')],
                      'rotation': [num(c) for c in vec(grip.get('rotation'), 3, what + ' grip rotation')]},
             'boxes': [{'origin': [num(c) for c in b['origin']], 'size': b['size'], 'uv': b['uv'], 'mirror': b['mirror'],
                        'dilation': [0, 0, 0]} for b in boxes],
             'texture': texture_id('weapon/%s.png' % wid), 'glow': texture_id('weapon/%s_e.png' % wid) if glow else ''}
    out = {'%s/weapon/%s.json' % (MODELS, wid): dumps(model), '%s/weapon/%s.png' % (TEXTURES, wid): png_bytes(tex)}
    if glow:
        out['%s/weapon/%s_e.png' % (TEXTURES, wid)] = png_bytes(glow)
    return out, (lit(glow) if glow else 0)


def bake(cat: Catalog, troop: str) -> dict:
    """The generated model of one troop (models/troop/<troop>.json), every rule checked."""
    src = cat.troops.get(troop)
    if src is None:
        fail('%s: no art/troops/%s.json' % (troop, troop))
    keys(src, TROOP_KEYS, troop)
    row = cat.sheet.get(troop)
    if row is None:
        fail('%s: no troops row in sheets/design.json has this id' % troop)
    rig_name = src.get('rig')
    rig = cat.rigs.get(rig_name)
    if rig is None:
        fail('%s: unknown rig %s' % (troop, rig_name))
    want = RIGS.get(row['entity'])
    if rig_name != want:
        fail('%s: rig %s is not the %s rig (%s)' % (troop, rig_name, row['entity'], want))
    size = vec(src.get('size', rig['size']), 2, troop + ' size', ints=True, minimum=1)
    idle = src.get('idleArms', 'hang')
    if idle not in IDLE_ARMS:
        fail('%s: idleArms %s is not one of %s' % (troop, idle, ', '.join(IDLE_ARMS)))
    hunch = src.get('hunch', 0)
    if not is_num(hunch) or (hunch and rig_name != 'biped'):
        fail('%s: hunch is a number of degrees, and only a biped hunches' % troop)
    parts = []
    entries = src.get('parts', [])
    if not isinstance(entries, list):
        fail(troop + ': parts must be a list')
    for e in entries:
        keys(e, ENTRY_KEYS, troop + ' part entry')
        p = cat.parts.get(e.get('part'))
        if p is None:
            fail('%s: unknown part %s' % (troop, e.get('part')))
        parts.append((e, p))
    stages = cat.stages[troop]
    bones = bake_bones(rig, troop, src.get('bones'), parts, float(hunch), idle == 'folded', stages)
    skin_boxes = [(b['name'], x['uv'], x['size']) for b in bones if not b['team'] for x in b['boxes']]
    check_layout(skin_boxes, size, troop)
    team_boxes = [(b['part'], x['uv'], x['size']) for b in bones if b['team'] for x in b['boxes']]
    if team_boxes:
        check_layout(sorted(set((o, tuple(uv), tuple(s)) for o, uv, s in team_boxes), key=str), cat.template.size,
                     troop + ' on the cloth sheet')
    weapon = src.get('weapon')
    if weapon != 'none' and weapon not in cat.weapons:
        fail('%s: unknown weapon %s' % (troop, weapon))
    hand = src.get('hand')
    if hand not in ('right', 'left'):
        fail('%s: hand must be right or left' % troop)
    portrait = src.get('portrait')
    if not isinstance(portrait, str) or not PORTRAIT.fullmatch(portrait):
        fail('%s: portrait must be standard, tall N, wide N or beast N [yaw D], not %s' % (troop, portrait))
    glow_from = src.get('glowFrom', 1)
    if glow_from not in stages:
        fail('%s: glowFrom %s is not one of the stages %s' % (troop, glow_from, stages))
    own, extra = troop_files(cat, troop)
    skins, last = {}, None
    for s in stages:
        last = own.get(s, last)
        skins[str(s)] = texture_id(last)
    model = {'format': FORMAT, 'troop': troop, 'rig': rig_name, 'size': size,
             'bones': [clean_bone(b, cat.template.size if b['team'] else size) for b in bones], 'skins': skins,
             'veteran': texture_id(extra['veteran']) if extra['veteran'] else '',
             'glow': texture_id(extra['glow']) if extra['glow'] else '', 'glowFrom': glow_from,
             'outer': texture_id(extra['outer']) if extra['outer'] else '',
             'weapon': weapon, 'hand': hand, 'idleArms': idle, 'portrait': portrait}
    assert tuple(model) == MODEL_KEYS and not set(model) & set(VANILLA_KEYS)
    return model


def troop_outputs(cat, troop, weapon_lit):
    model = bake(cat, troop)
    own, extra = troop_files(cat, troop)
    folder = cat.root / 'art/troops'
    size = model['size']
    out = {'%s/%s.json' % (MODELS, troop): dumps(model)}
    for f in [own[s] for s in sorted(own)] + [extra['veteran'], extra['outer'], extra['glow']]:
        if not f:
            continue
        im = read_png(folder / f, troop)
        if list(im.size) != size:
            fail('%s: %s is %dx%d, not the troop size %dx%d' % (troop, f, im.size[0], im.size[1], size[0], size[1]))
        if f != extra['glow']:
            tint(im, '%s: %s' % (troop, f), cat.factions)
        out['%s/%s' % (TEXTURES, f)] = png_bytes(im)
    cap = GLOW_CAP.get(cat.sheet[troop]['rarity'], 6)
    n = (lit(read_png(folder / extra['glow'], troop)) if extra['glow'] else 0) + weapon_lit.get(model['weapon'], 0)
    if n > cap:
        fail('%s: %d lit pixels in its glow and its weapon\'s glow together, over the %s cap of %d'
             % (troop, n, cat.sheet[troop]['rarity'], cap))
    return out


def cloth(cat: Catalog, side) -> Image.Image:
    """The team cloth sheet of a side (a factions id), linen for None, or the company's navy for 'company': the
    template's three greys take the colour x1.0, x0.8 and x0.6, every other pixel keeps its paint, and a side's glyph mask
    goes on in white (linen and the company carry none)."""
    if cat.template is None:
        fail('art/team/team_cloth.png is missing')
    if side is None:
        rgb, glyph = LINEN, None
    elif side == 'company':
        rgb, glyph = COMPANY, None
    else:
        row = cat.factions.get(side)
        if row is None:
            fail('cloth: no factions row %s' % side)
        c = row['color']
        rgb = ((c >> 16) & 255, (c >> 8) & 255, c & 255)
        glyph = cat.glyphs.get(row['glyph'])
        if glyph is None:
            fail('cloth %s: art/team/glyph_%s.png is missing' % (side, row['glyph']))
    out = cat.template.copy()
    px = out.load()
    w, h = out.size
    if glyph is not None and glyph.size != out.size:
        fail('glyph_%s.png is %dx%d, not the cloth template size %dx%d' % (row['glyph'], glyph.size[0], glyph.size[1], w, h))
    mask = glyph.load() if glyph is not None else None
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            k = GREYS.get((r, g, b)) if a else None
            if mask is not None and mask[x, y][3]:
                if k is None:
                    fail('glyph_%s.png: pixel (%d, %d) is not on a field grey of team_cloth.png' % (row['glyph'], x, y))
                px[x, y] = GLYPH + (255,)
            elif k is not None:
                px[x, y] = tuple(int(c * k + 0.5) for c in rgb) + (a,)
    return out


def build(cat):
    """Every output as {path under the assets root: bytes}, plus the index."""
    files = {}
    weapon_lit = {}
    for wid in sorted(cat.weapons):
        out, n = weapon_outputs(cat, wid)
        files.update(out)
        weapon_lit[wid] = n
    for rid in sorted(cat.rigs):
        rig = cat.rigs[rid]
        bones = bake_bones(rig, 'rig ' + rid)
        files['%s/rig/%s.json' % (MODELS, rid)] = dumps(
            {'format': FORMAT, 'rig': rid, 'size': rig['size'], 'outer': num(rig['outer']), 'hands': rig['hands'],
             'bones': [clean_bone(b, rig['size']) for b in bones]})
    for tid in sorted(cat.troops):
        files.update(troop_outputs(cat, tid, weapon_lit))
    sides = list(cat.factions)
    for side in sides + [None, 'company']:
        files['%s/team_cloth_%s.png' % (TEXTURES, side or 'linen')] = png_bytes(cloth(cat, side))
    index = {'format': FORMAT, 'troops': sorted(cat.troops), 'rigs': sorted(cat.rigs), 'weapons': sorted(cat.weapons),
             'outputs': sorted(files)}
    files['%s/index.json' % MODELS] = dumps(index)
    return files, index, len(sides) + 2


def listed(index_file):
    try:
        return set(json.loads(index_file.read_text(encoding='utf-8')).get('outputs', []))
    except (OSError, ValueError, AttributeError):
        return set()


def owned_files(out):
    found = []
    for folder in (MODELS, TEXTURES):
        if (out / folder).is_dir():
            found += [p.relative_to(out).as_posix() for p in (out / folder).rglob('*') if p.is_file()]
    return sorted(found)


def export(root: Path, out: Path) -> dict:
    """Builds every output into out (the assets root), removes what the previous index listed and this build no longer
    makes, and returns the new index."""
    root, out = Path(root), Path(out)
    cat = load(root)
    files, index, cloths = build(cat)
    before = listed(out / MODELS / 'index.json')
    for rel, data in files.items():
        path = out / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.is_file() or differs(rel, path.read_bytes(), data):
            path.write_bytes(data)
    for gone in sorted(before - set(index['outputs'])):
        path = out / gone
        if path.is_file():
            path.unlink()
            print('build_skins: removed ' + gone)
    strays = sorted(set(owned_files(out)) - set(files))
    if strays:
        print('build_skins: left %d file(s) that no index listed, first %s; --verify fails until they go' % (len(strays), strays[0]))
    print('build_skins: %d troops, %d rigs, %d weapons, %d cloths' % (len(cat.troops), len(cat.rigs), len(cat.weapons), cloths))
    try:
        shown = out.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        shown = out.as_posix()
    print('build_skins: %d outputs in %s' % (len(files), shown))
    return index


def pixels(data):
    """A PNG's size and its pixels as RGBA bytes."""
    with Image.open(io.BytesIO(data)) as im:
        return im.size, im.convert('RGBA').tobytes()


def differs(rel, a, b):
    """How a committed output differs from its rebuild, or None when they match: a PNG by its size and RGBA pixels (the
    same pixels may be encoded in other bytes by another Pillow or zlib build, and that is no change), anything else byte
    for byte."""
    if a == b:
        return None
    if rel.endswith('.png'):
        try:
            (sa, pa), (sb, pb) = pixels(a), pixels(b)
        except Exception as e:  # Pillow raises OSError, SyntaxError or zlib.error on a broken file
            return 'is not a PNG its rebuild can be compared with (%s)' % e
        if sa != sb:
            return 'is %dx%d, its rebuild %dx%d' % (sa + sb)
        if pa == pb:
            return None
        at = next(i for i in range(len(pa)) if pa[i] != pb[i]) // 4
        return 'differs from its rebuild at pixel (%d, %d): #%s, rebuilt #%s' % (
            at % sa[0], at // sa[0], pa[at * 4:at * 4 + 4].hex().upper(), pb[at * 4:at * 4 + 4].hex().upper())
    at = next((i for i in range(min(len(a), len(b))) if a[i] != b[i]), min(len(a), len(b)))
    return 'differs from its rebuild at byte %d' % at


def verify(root: Path) -> int:
    """Rebuilds into a temp dir and compares every output with the committed one (differs: PNGs by pixels, the rest byte
    for byte); exits 1 naming the first difference, a missing output or a file the index does not list."""
    root = Path(root)
    out = root / ASSETS
    if not (out / MODELS / 'index.json').is_file():
        fail('%s/index.json is missing; run python tools/build_skins.py' % MODELS)
    with tempfile.TemporaryDirectory() as tmp:
        quiet = io.StringIO()
        real, sys.stdout = sys.stdout, quiet
        try:
            index = export(root, Path(tmp))
        except SystemExit:
            sys.stdout = real
            print(quiet.getvalue().rstrip())
            raise
        finally:
            sys.stdout = real
        fresh = sorted(index['outputs'] + ['%s/index.json' % MODELS])
        for rel in fresh:
            a, b = out / rel, Path(tmp) / rel
            if not a.is_file():
                fail('%s is missing; run python tools/build_skins.py' % rel)
            why = differs(rel, a.read_bytes(), b.read_bytes())
            if why:
                fail('%s %s; run python tools/build_skins.py' % (rel, why))
        extra = sorted(set(owned_files(out)) - set(fresh))
        if extra:
            fail('%s is not in %s/index.json' % (extra[0], MODELS))
    print('build_skins: %d outputs verified' % len(fresh))
    return 0


FACE_SHADE = {'top': 1.25, 'bottom': 0.55, 'north': 1.0, 'south': 0.7, 'west': 0.85, 'east': 0.85}
HUES = [(196, 92, 84), (84, 140, 196), (104, 180, 96), (200, 164, 72), (150, 100, 190), (70, 170, 170), (210, 120, 50),
        (120, 120, 200), (180, 90, 150), (110, 160, 60), (90, 110, 130), (200, 200, 110)]


def faces(uv, size):
    """The six face rectangles of a box on its sheet, by the side they show (north is the front, west the model's right)."""
    (u, v), (w, h, d) = uv, size
    return {'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
            'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
            'east': (u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def template(cat: Catalog, troop: str) -> Image.Image:
    """Every skin box face of the troop in a flat colour (one hue per bone; top light, bottom dark, the front plain)."""
    model = bake(cat, troop)
    im = Image.new('RGBA', tuple(model['size']), (0, 0, 0, 0))
    px = im.load()
    for i, b in enumerate(x for x in model['bones'] if not x['team'] and x['boxes']):
        base = HUES[i % len(HUES)]
        for box in b['boxes']:
            for face, (x0, y0, x1, y1) in faces(box['uv'], box['size']).items():
                k = FACE_SHADE[face]
                colour = tuple(max(0, min(255, int(c * k))) for c in base) + (255,)
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        px[x, y] = colour
    return im


def main(args):
    if args == ['--verify']:
        return verify(ROOT)
    if len(args) == 2 and args[0] == '--template':
        cat = load(ROOT)
        im = template(cat, args[1])
        dest = ROOT / 'work/troops' / (args[1] + '_template.png')
        dest.parent.mkdir(parents=True, exist_ok=True)
        im.save(dest)
        print('build_skins: template %s' % dest.relative_to(ROOT).as_posix())
        return 0
    if not args:
        export(ROOT, ROOT / ASSETS)
        return 0
    print(__doc__)
    return 2


if __name__ == '__main__':
    if sys.version_info[:2] != (3, 12):
        sys.exit('build_skins: needs Python 3.12, not %d.%d' % sys.version_info[:2])
    if os.environ.get('PYTHONHASHSEED') != '0':
        os.environ['PYTHONHASHSEED'] = '0'
        sys.exit(subprocess.call([sys.executable] + sys.argv))
    sys.exit(main(sys.argv[1:]))
