"""Blockbench projects (.bbmodel, Generic Model, art pipeline v2) to the quads of a weapon (Task 5d). Pure Python: no
Minecraft and no Pillow, so tools/build_skins.py, its tests and the art checks can all import it.

A weapon project holds one top-level group per frame (`rest`, and `pull_0` to `pull_2` of a draw weapon), cubes only, one
texture. Blockbench's space is y up with the grip at the origin; `weapon_frames` turns it by a half turn about x
((x, y, z) -> (x, -y, -z), a proper rotation, so the winding is kept) into the weapon frame of build_skins.py (x right,
y down, z thickness; Blockbench's south, the front the style guide names, is the frame's north). A quad is 20 numbers: the
four corners in model px, counter-clockwise seen from outside, then the texel of each corner; its normal is the cross
product of (corner 1 - corner 0) and (corner 3 - corner 0), as the client's TroopMesh reads it.

  python tools/bbmodel.py [--density N] <file.bbmodel> ...      print the frames, cubes and quads of each project and the weapon checks

The density (texels a px) is 2 for a weapon unless art/weapons/<name>.json says "density" (3 for the texel-3 trial) or --density does.

Reading never looks at visibility: a hidden group or cube is still a frame or a cube (only `rest` is saved visible, so
Blockbench shows one weapon and not four laid over each other).

Troops (stage 2, art pipeline v2 sections 2.3 and 2.4) go both ways: `write` saves a project build_skins.py builds from
the JSON, and `troop_bones` reads a troop project's bone groups back against its rig, checking the part groups against the
part library. Troops are the only thing converted to a .bbmodel; weapons are authored in Blockbench and only ever read.
A troop project keeps the rig's y-down coordinates and puts everything under one display group turned 180 degrees about
x, so Blockbench (y up) shows the model upright without changing a cube or a UV. That group is not model data.
"""
import itertools
import json
import math
import pathlib
import sys

FACES = ('north', 'east', 'south', 'west', 'up', 'down')
# The corners of each face of a Blockbench cube as picks (0 = from, 1 = to) on x, y, z, counter-clockwise seen from
# outside; corner 0 is the face's bottom left as painted (its patch's (u1, v2)), 1 its bottom right, 2 its top right, 3 its
# top left. Up and down are painted with north at the top of the patch and south at the top of the down face's patch.
CORNERS = {
    'north': ((1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)),
    'east': ((1, 0, 1), (1, 0, 0), (1, 1, 0), (1, 1, 1)),
    'south': ((0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)),
    'west': ((0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)),
    'up': ((0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)),
    'down': ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)),
}
UV_PICK = ((0, 3), (2, 3), (2, 1), (0, 1))   # corners 0 to 3 read (u1, v2), (u2, v2), (u2, v1), (u1, v1) of a face's [u1, v1, u2, v2]
LOCAL_NORMAL = {'north': (0, 0, -1), 'east': (1, 0, 0), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
FACE_AXES = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}   # the two axes a face spans
FLAT = 1e-9   # a face with less area than this draws nothing
WEAPON_DENSITY = 2.0   # texels a px of a weapon sheet unless its json says density (tools/build_skins.py WEAPON_DENSITY is the same number)


class BBError(Exception):
    """A project that cannot be read as a weapon; the message names the file, cube or face."""


# ---- vectors and matrices ----

def sub(a, b):
    return [x - y for x, y in zip(a, b)]


def add(a, b):
    return [x + y for x, y in zip(a, b)]


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def length(a):
    return math.sqrt(dot(a, a))


def unit(a):
    n = length(a)
    return [c / n for c in a] if n > 0 else [0.0, 0.0, 0.0]


def mat_vec(m, v):
    return [m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2] for i in range(3)]


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


IDENTITY = [[1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]]


def rotation(deg):
    """Blockbench turns an element or a group by Euler degrees [x, y, z] in three.js order ZYX: about x first, then y, then z."""
    ax, ay, az = (math.radians(a) for a in deg)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    x = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mat_mul(z, mat_mul(y, x))


def num(v):
    """A number as the model files keep it: four decimals, an integer when it is one, never -0."""
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


# ---- the project ----

class Texture:
    def __init__(self, index, t):
        self.index = index
        self.name = str(t.get('name', index))
        self.width, self.height = int(t.get('width', 0)), int(t.get('height', 0))
        self.uv_width, self.uv_height = int(t.get('uv_width', self.width)), int(t.get('uv_height', self.height))
        self.path = t.get('relative_path') or t.get('path') or ''
        self.source = t.get('source') or ''   # the embedded copy, a data URL


class Cube:
    """One cube: its extent, its own turn about its origin, and the groups above it. `corner` gives a face's corners in
    Blockbench's space after every turn; R, center and half describe the same cube as an oriented box for the checks."""

    def __init__(self, e, frame, chain, visible):
        self.name = str(e.get('name', e.get('uuid', 'cube')))
        self.frame = frame
        self.visible = visible and e.get('visibility', True) is not False
        grow = float(e.get('inflate', 0) or 0)
        self.lo = [float(c) - grow for c in e['from']]
        self.hi = [float(c) + grow for c in e['to']]
        self.origin = [float(c) for c in e.get('origin', [(a + b) / 2 for a, b in zip(self.lo, self.hi)])]
        rot = e.get('rotation') or [0, 0, 0]
        self.steps = []   # (pivot, matrix) from the cube's own turn out to the outermost group
        if any(rot):
            self.steps.append((self.origin, rotation(rot)))
        self.steps += chain
        self.R = IDENTITY
        for _, m in self.steps:
            self.R = mat_mul(m, self.R)
        mid = [(a + b) / 2 for a, b in zip(self.lo, self.hi)]
        self.center = self.world(mid)
        self.half = [(b - a) / 2 for a, b in zip(self.lo, self.hi)]
        self.faces = {}
        for f, spec in (e.get('faces') or {}).items():
            if f not in LOCAL_NORMAL:
                raise BBError('cube %s: no face called %s' % (self.name, f))
            tex = spec.get('texture')
            uv = spec.get('uv')
            if tex is None or tex is False or uv is None:
                continue   # a face with no texture is not drawn
            self.faces[f] = (int(tex), [float(c) for c in uv])

    def world(self, p):
        for pivot, m in self.steps:
            p = add(mat_vec(m, sub(p, pivot)), pivot)
        return p

    def corners(self, face):
        picks = CORNERS[face]
        return [self.world([(self.hi if pick[a] else self.lo)[a] for a in range(3)]) for pick in picks]

    def normal(self, face):
        return mat_vec(self.R, list(LOCAL_NORMAL[face]))

    def size(self, face):
        a, b = FACE_AXES[face]
        return self.hi[a] - self.lo[a], self.hi[b] - self.lo[b]


class Model:
    """frames: top-level group name -> its cubes (depth first, in file order); visible: group name -> saved visible; root:
    the cubes that sit at the root, which a weapon may not have."""

    def __init__(self, name, textures):
        self.name = name
        self.textures = textures
        self.frames, self.visible, self.root = {}, {}, []

    def cubes(self):
        return [c for cs in self.frames.values() for c in cs] + self.root


def read(path) -> Model:
    """Reads a .bbmodel. Raises BBError for a file that is not JSON, an element that is not a cube, or a duplicate frame group."""
    try:
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
    except (OSError, ValueError) as e:
        raise BBError('%s: %s' % (path, e))
    if not isinstance(data, dict) or not isinstance(data.get('elements'), list) or not isinstance(data.get('outliner'), list):
        raise BBError('%s: not a Blockbench project (no elements or outliner)' % path)
    model = Model(str(data.get('name', '')), [Texture(i, t) for i, t in enumerate(data.get('textures') or [])])
    model.data = data   # the raw project, for troop_bones (read's frames flatten the groups a troop needs)
    elements = {}
    for e in data['elements']:
        kind = e.get('type', 'cube')
        if kind != 'cube':
            raise BBError('%s: element %s is a %s; weapons are cubes only' % (path, e.get('name', e.get('uuid')), kind))
        elements[e['uuid']] = e
    groups = {g['uuid']: g for g in data.get('groups') or []}
    placed = set()

    def make(uid, frame, chain, visible):
        e = elements.get(uid)
        if e is None:
            raise BBError('%s: the outliner names %s, which is no element' % (path, uid))
        placed.add(uid)
        try:
            return Cube(e, frame, chain, visible)
        except (KeyError, TypeError, ValueError) as err:
            raise BBError('%s: cube %s is malformed (%s)' % (path, e.get('name', uid), err))

    def walk(node, frame, chain, visible, out):
        if isinstance(node, str):
            out.append(make(node, frame, chain, visible))
            return
        g = groups.get(node.get('uuid'), {})
        here = list(chain)
        origin, rot = [float(c) for c in g.get('origin') or node.get('origin') or [0, 0, 0]], g.get('rotation') or node.get('rotation') or [0, 0, 0]
        if any(rot):
            here = [(origin, rotation(rot))] + here   # a group turns what its children already turned, so it goes outside
        for ch in node.get('children', []):
            walk(ch, frame, here, visible and g.get('visibility', True) is not False, out)

    for node in data['outliner']:
        if isinstance(node, str):
            model.root.append(make(node, None, [], True))
            continue
        g = groups.get(node.get('uuid'), {})
        name = node.get('name') or g.get('name')
        if not name:
            raise BBError('%s: a top-level group has no name' % path)
        if name in model.frames:
            raise BBError('%s: two frame groups called %s' % (path, name))
        cubes = []
        walk(node, name, [], True, cubes)
        model.frames[name] = cubes
        model.visible[name] = g.get('visibility', True) is not False
    for uid, e in elements.items():
        if uid not in placed:
            model.root.append(Cube(e, None, [], True))
    return model


def write(project, path):
    """Writes a project dict as a .bbmodel (indent 1, a final newline); makes the folder. Returns the path written."""
    path = pathlib.Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(project, indent=1) + '\n', encoding='utf-8')
    return path


def _same(a, b, eps=1e-6):
    return len(a) == len(b) and all(abs(float(x) - float(y)) <= eps for x, y in zip(a, b))


def _vec3(v, what):
    if not isinstance(v, (list, tuple)) or len(v) != 3:
        raise BBError('%s must be three numbers, not %r' % (what, v))
    try:
        return [float(c) for c in v]
    except (TypeError, ValueError):
        raise BBError('%s must be three numbers, not %r' % (what, v))


def _troop_cube(e, base, what):
    """One skin cube of a bone group as bake_bones keeps a box: origin from the group origin, size, uv, no dilation."""
    if e.get('type', 'cube') != 'cube':
        raise BBError('%s: element %s is a %s; a troop is cubes only' % (what, e.get('name', e.get('uuid')), e.get('type')))
    rot = e.get('rotation') or [0, 0, 0]
    if any(abs(float(c)) > 1e-9 for c in rot):
        raise BBError('%s: cube %s is turned %s; turn its bone group, not the cube' % (what, e.get('name', e.get('uuid')), rot))
    if not e.get('box_uv', False):
        raise BBError('%s: cube %s is not box UV; a skin cube is box UV on the troop sheet' % (what, e.get('name', e.get('uuid'))))
    uv = e.get('uv_offset')
    if not isinstance(uv, (list, tuple)) or len(uv) != 2:
        raise BBError('%s: cube %s needs a uv_offset (its box on the troop sheet)' % (what, e.get('name', e.get('uuid'))))
    frm, to = _vec3(e.get('from'), 'cube %s from' % what), _vec3(e.get('to'), 'cube %s to' % what)
    size = [to[i] - frm[i] for i in range(3)]
    if any(s < -1e-9 or abs(s - round(s)) > 1e-6 for s in size):
        raise BBError('%s: cube %s is %s px across; a troop box is a whole number of px' % (what, e.get('name', e.get('uuid')), size))
    if e.get('mirror', False) or e.get('mirror_uv', False):
        raise BBError('%s: cube %s is mirrored; troop .bbmodel cubes are unmirrored in this version (the rig has one '
                      'mirrored arms_folded cube, which a folded troop cannot round-trip yet)' % (what, e.get('name', e.get('uuid'))))
    return {'origin': [frm[i] - base[i] for i in range(3)], 'size': [int(round(s)) for s in size],
            'uv': [int(uv[0]), int(uv[1])], 'mirror': False, 'dilation': [0.0, 0.0, 0.0]}


def troop_bones(model, rig, parts=()):
    """A troop project's rig bone groups as bake_bones takes them (before scale, hunch and the stand-on-the-ground shift).

    The one top-level group (`model`, turned 180, 0, 0 so Blockbench's y-up view shows the rig's y-down model upright) is a
    display group, not data. Each rig bone is a group named as the rig names it, at the bone's model-space pivot, with the
    rig's rest rotation; its cubes are the bone's boxes (box UV on the troop sheet). The part groups (`part:<part>[:<name>]`,
    `team:<part>`) are checked against art/parts/<part>.json: the library stays the source of part boxes, so a part group's
    cubes must be the library's boxes at the group's origin, and the group must hang from the entry's anchor. Part bones are
    still built from the JSON by build_skins.py (only their groups are checked here). Returns name -> bone, rig order.
    Raises BBError.
    """
    data = getattr(model, 'data', model)
    groups = {g['uuid']: g for g in data.get('groups') or [] if 'uuid' in g}
    elements = {e['uuid']: e for e in data.get('elements') or [] if 'uuid' in e}
    parents, owner = {}, {}

    def walk(node, parent):
        uid = node.get('uuid') if isinstance(node, dict) else node
        if uid in elements:
            if parent is None:
                raise BBError('cube %s sits at the root; every cube belongs to a group' % elements[uid].get('name', uid))
            owner[uid] = parent
            return
        if uid not in groups:
            raise BBError('the outliner names %s, which is no group or cube' % uid)
        if uid in parents:
            raise BBError('group %s is in the outliner twice' % groups[uid].get('name', uid))
        if not isinstance(node, dict):
            raise BBError('group %s appears as a bare id, without its children' % groups[uid].get('name', uid))
        parents[uid] = parent
        for child in node.get('children') or []:
            walk(child, uid)

    for node in data.get('outliner') or []:
        walk(node, None)
    outside = set(groups) - set(parents)
    if outside:
        raise BBError('groups outside the outliner: %s' % ', '.join(str(groups[u].get('name', u)) for u in sorted(outside)))
    tops = [u for u in groups if parents[u] is None]
    if len(tops) != 1:
        raise BBError('%d top-level groups; a troop has one display group called model' % len(tops))
    display = groups[tops[0]]
    if display.get('name') != 'model':
        raise BBError('the top-level group is %s; a troop\'s display group is called model' % display.get('name'))
    if not _same([float(c) for c in display.get('rotation') or [0, 0, 0]], [180.0, 0.0, 0.0]):
        raise BBError('the model display group is turned %s; it must be 180, 0, 0 (Blockbench y is up, the rig y down)'
                      % (display.get('rotation') or [0, 0, 0]))
    if not _same(_vec3(display.get('origin') or [0, 0, 0], 'the model display group origin'), [0.0, 0.0, 0.0]):
        raise BBError('the model display group is at %s; it must sit at the origin' % (display.get('origin') or [0, 0, 0]))

    byname = {}
    for uid, g in groups.items():
        name = g.get('name')
        if name in byname:
            raise BBError('two groups called %s' % name)
        byname[name] = uid
    rig_names = [b['name'] for b in rig['bones']]
    for name in rig_names:
        if name not in byname:
            raise BBError('the project has no group for the %s bone %s' % (rig['rig'], name))
    bones, origins = {}, {}
    for b in rig['bones']:
        uid = byname[b['name']]
        g = groups[uid]
        origin = _vec3(g.get('origin'), 'group %s origin' % b['name'])
        parent = b['parent']
        want_parent = byname.get(parent) if parent else tops[0]
        got_parent = parents[uid]
        if got_parent != want_parent:
            raise BBError('group %s hangs from %s, not %s' % (b['name'], groups.get(got_parent, {}).get('name') if got_parent else 'the top level', parent or 'model'))
        base = origins.get(parent, [0.0, 0.0, 0.0])
        rot = [float(c) for c in g.get('rotation') or [0, 0, 0]]
        want = [float(c) for c in b.get('rotation', [0, 0, 0])]
        if not _same(rot, want):
            raise BBError('group %s is turned %s; the rig says %s' % (b['name'], rot, want))
        boxes = [_troop_cube(e, origin, 'group ' + b['name']) for euid, e in elements.items() if owner.get(euid) == uid]
        bones[b['name']] = {'name': b['name'], 'parent': parent, 'part': '', 'team': False, 'tag': '', 'stages': [],
                            'weaponTiers': [], 'pivot': [origin[i] - base[i] for i in range(3)], 'rotation': rot, 'boxes': boxes}
        origins[b['name']] = origin

    entries = {entry.get('name', part['part']): (entry, part) for entry, part in parts}
    seen_parts = set()
    for uid, g in groups.items():
        name = g.get('name') or ''
        if not (name.startswith('part:') or name.startswith('team:')):
            continue
        team = name.startswith('team:')
        bits = name[5:].split(':')
        if len(bits) > 2:
            raise BBError('group %s: a part group is part:<part>[:<name>] or team:<part>[:<name>]' % name)
        pid, pname = bits[0], (bits[1] if len(bits) > 1 else bits[0])
        if pname not in entries:
            raise BBError('group %s: no part entry called %s in the troop json' % (name, pname))
        entry, part = entries[pname]
        if part['part'] != pid:
            raise BBError('group %s: part %s is not %s' % (name, pid, part['part']))
        if team != part['team']:
            raise BBError('group %s: part %s is %s team part' % (name, pid, 'a' if part['team'] else 'no'))
        anchor = entry.get('bone', part['bone'])
        if anchor not in origins:
            raise BBError('group %s hangs from %s, which is no bone or earlier part' % (name, anchor))
        if parents[uid] != byname.get(anchor):
            raise BBError('group %s is not under its anchor bone %s' % (name, anchor))
        pivot = [a + b2 for a, b2 in zip(_vec3(part.get('pivot', [0, 0, 0]), 'part ' + pid + ' pivot'),
                                         _vec3(entry.get('offset', [0, 0, 0]), 'part ' + pname + ' offset'))]
        origin = _vec3(g.get('origin'), 'group %s origin' % name)
        want = [origins[anchor][i] + pivot[i] for i in range(3)]
        if not _same(origin, want):
            raise BBError('group %s is at %s; the troop json puts it at %s' % (name, origin, want))
        rot = [float(c) for c in g.get('rotation') or [0, 0, 0]]
        want = [float(c) for c in entry.get('rotation', part.get('rotation', [0, 0, 0]))]
        if not _same(rot, want):
            raise BBError('group %s is turned %s; the troop json says %s' % (name, rot, want))
        cubes = [elements[euid] for euid in elements if owner.get(euid) == uid]
        if len(cubes) != len(part['boxes']):
            raise BBError('group %s has %d cubes; part %s has %d boxes' % (name, len(cubes), pid, len(part['boxes'])))
        for i, (e, box) in enumerate(zip(cubes, part['boxes'])):
            frm, to = _vec3(e.get('from'), 'cube from'), _vec3(e.get('to'), 'cube to')
            want_from = [origin[k] + box['origin'][k] for k in range(3)]
            want_to = [want_from[k] + box['size'][k] for k in range(3)]
            if not _same(frm, want_from) or not _same(to, want_to):
                raise BBError('group %s cube %d is %s..%s; the part library puts it at %s..%s'
                              % (name, i, frm, to, want_from, want_to))
        origins[pname] = origin
        seen_parts.add(pname)
    missing = set(entries) - seen_parts
    if missing:
        raise BBError('the troop json lists parts with no group: %s' % ', '.join(sorted(missing)))
    known = {byname[n] for n in rig_names} | {u for u, g in groups.items() if (g.get('name') or '').startswith(('part:', 'team:'))}
    for euid in elements:
        if owner.get(euid) not in known:
            raise BBError('cube %s sits under %s, not a bone or part group' % (elements[euid].get('name', euid), groups.get(owner.get(euid), {}).get('name', 'the root')))
    return bones


# ---- the weapon quads ----

class Quad:
    """One drawn face: its cube and face name, its four corners in the weapon frame and the texel of each."""

    def __init__(self, cube, face, corners, texels):
        self.cube, self.face, self.corners, self.texels = cube, face, corners, texels

    def numbers(self):
        return [num(c) for p in self.corners for c in p] + [num(t) for uv in self.texels for t in uv]


def to_weapon_frame(p):
    """Blockbench (y up) to the weapon frame (y down): a half turn about x keeps the handedness."""
    return [p[0], -p[1], -p[2]]


def cube_quads(model, cube):
    """The quads of one cube in face order (north, east, south, west, up, down), the faces with no area left out."""
    out = []
    for face in FACES:
        if face not in cube.faces:
            continue
        tex, uv = cube.faces[face]
        if tex < 0 or tex >= len(model.textures):
            raise BBError('cube %s face %s: texture %d is not in the project' % (cube.name, face, tex))
        t = model.textures[tex]
        w, h = cube.size(face)
        if w * h <= FLAT:
            continue
        ku = t.width / t.uv_width if t.uv_width else 1.0
        kv = t.height / t.uv_height if t.uv_height else 1.0
        corners = [to_weapon_frame(p) for p in cube.corners(face)]
        texels = [(uv[i] * ku, uv[j] * kv) for i, j in UV_PICK]
        out.append(Quad(cube, face, corners, texels))
    return out


def frame_quads(model):
    """Frame name -> its Quad objects. A cube at the root raises BBError."""
    if model.root:
        raise BBError('cube %s sits at the root; every cube belongs to a frame group (rest, pull_0 to pull_2)' % model.root[0].name)
    return {name: [q for c in cubes for q in cube_quads(model, c)] for name, cubes in model.frames.items()}


def weapon_frames(model):
    """Frame name -> its quads as 20 numbers each, frames in file order."""
    return {name: [q.numbers() for q in quads] for name, quads in frame_quads(model).items()}


def bounds(quads):
    """(lo, hi) over every corner of the Quad objects, in the weapon frame."""
    pts = [p for q in quads for p in q.corners]
    return [min(p[a] for p in pts) for a in range(3)], [max(p[a] for p in pts) for a in range(3)]


# ---- the checks (art pipeline v2 section 2.4); each returns what is wrong, empty when nothing is ----

def check_textures(model, kind='weapon', density=2, layout=None):
    """What is wrong with the project's texture. kind 'weapon': its UV unit is a texel, so the UV size must equal the bitmap,
    whatever the weapon's density. kind 'troop': its UV unit is a layout unit (a px of the model), so the bitmap must be the
    UV size, or `layout` when given, times `density` texels a px (a troop of density 3 on a 128x64 layout has a 384x192 bitmap)."""
    if kind not in ('weapon', 'troop'):
        raise ValueError('kind must be weapon or troop, not %r' % (kind,))
    said = []
    if len(model.textures) != 1:
        said.append('the project has %d textures; a %s has one texture' % (len(model.textures), kind))
    for t in model.textures:
        if kind == 'weapon':
            if (t.uv_width, t.uv_height) != (t.width, t.height):
                said.append('texture %s has UV size %dx%d but its bitmap is %dx%d (a UV unit must be a texel)'
                            % (t.name, t.uv_width, t.uv_height, t.width, t.height))
        elif layout is not None and (t.uv_width, t.uv_height) != tuple(layout):
            said.append('texture %s has UV size %dx%d, not the troop layout %dx%d (a UV unit is a px of the model)'
                        % (t.name, t.uv_width, t.uv_height, layout[0], layout[1]))
        else:
            want = (round(t.uv_width * density), round(t.uv_height * density))
            if (t.width, t.height) != want:
                said.append('texture %s is %dx%d, but the UV size %dx%d at %d texels a px is %dx%d'
                            % (t.name, t.width, t.height, t.uv_width, t.uv_height, density, want[0], want[1]))
    for c in model.cubes():
        for face, (tex, _) in sorted(c.faces.items()):
            if tex < 0 or tex >= len(model.textures):
                said.append('cube %s face %s uses texture %d, which the project lacks' % (c.name, face, tex))
    return said


def file_density(path, override=None):
    """The texels a px of a weapon project: --density when given, else the "density" of the weapon's json beside it
    (art/weapons/<stem>.json), else 2 (the weapons' default)."""
    if override is not None:
        return float(override)
    side = pathlib.Path(path).with_suffix('.json')
    try:
        data = json.loads(side.read_text(encoding='utf-8'))
    except (OSError, ValueError):
        return WEAPON_DENSITY
    value = data.get('density') if isinstance(data, dict) else None
    return float(value) if isinstance(value, (int, float)) and not isinstance(value, bool) and value > 0 else WEAPON_DENSITY


def off_density(cubes, scale, tol=1e-3):
    """Faces whose UV is not exactly `scale` times their size: (cube, face, want w, want h, got w, got h)."""
    out = []
    for c in cubes:
        for face in FACES:
            if face not in c.faces:
                continue
            w, h = c.size(face)
            u1, v1, u2, v2 = c.faces[face][1]
            gw, gh = abs(u2 - u1), abs(v2 - v1)
            if abs(gw - scale * w) > tol or abs(gh - scale * h) > tol:
                out.append((c.name, face, round(scale * w, 4), round(scale * h, 4), round(gw, 4), round(gh, 4)))
    return out


def gap(a, b):
    """The largest separation of two cubes along any of the 15 separating axes; negative when they overlap."""
    axes = [[a.R[i][k] for i in range(3)] for k in range(3)] + [[b.R[i][k] for i in range(3)] for k in range(3)]
    for u, v in itertools.product(axes[:3], axes[3:]):
        x = cross(u, v)
        if length(x) > 1e-6:
            axes.append(unit(x))
    d = sub(b.center, a.center)
    best = -1e9
    for ax in axes:
        ra = sum(a.half[i] * abs(dot([a.R[r][i] for r in range(3)], ax)) for i in range(3))
        rb = sum(b.half[i] * abs(dot([b.R[r][i] for r in range(3)], ax)) for i in range(3))
        best = max(best, abs(dot(d, ax)) - ra - rb)
    return best


def clip(poly, clipper):
    """Sutherland-Hodgman: a convex polygon clipped by a convex counter-clockwise one."""
    out = poly
    for i in range(len(clipper)):
        a, b = clipper[i], clipper[(i + 1) % len(clipper)]
        inp, out = out, []
        if not inp:
            break
        for j in range(len(inp)):
            p, q = inp[j], inp[(j + 1) % len(inp)]
            sp = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
            sq = (b[0] - a[0]) * (q[1] - a[1]) - (b[1] - a[1]) * (q[0] - a[0])
            if sp >= 0:
                out.append(p)
            if (sp >= 0) != (sq >= 0):
                t = sp / (sp - sq)
                out.append((p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])))
    return out


def area(poly):
    return abs(sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))) / 2


def counter_clockwise(pts):
    s = sum(pts[i][0] * pts[(i + 1) % len(pts)][1] - pts[(i + 1) % len(pts)][0] * pts[i][1] for i in range(len(pts)))
    return pts if s >= 0 else pts[::-1]


def coplanar_faces(cubes, margin=0.05, overlap=0.02):
    """Two faces of different cubes on one plane (closer than `margin` px), facing the same way, whose footprints overlap
    by more than `overlap` px squared z-fight: (cube a, face, cube b, face, distance, overlap). Back-to-back faces are never
    drawn against each other and pass, rotated cubes included."""
    bad = []
    for a, b in itertools.combinations(cubes, 2):
        if gap(a, b) > margin + 0.01:
            continue
        for fa, fb in itertools.product(a.faces, b.faces):
            na, nb = a.normal(fa), b.normal(fb)
            if dot(na, nb) < 0.99999:
                continue
            pa, pb = a.corners(fa), b.corners(fb)
            dist = abs(dot(na, pa[0]) - dot(na, pb[0]))
            if dist >= margin - 1e-6:
                continue
            u = unit(cross(na, [1, 0, 0] if abs(na[0]) < 0.9 else [0, 1, 0]))
            v = cross(na, u)
            fa2 = counter_clockwise([(dot(p, u), dot(p, v)) for p in pa])
            fb2 = counter_clockwise([(dot(p, u), dot(p, v)) for p in pb])
            both = clip(fa2, fb2)
            ov = area(both) if len(both) >= 3 else 0.0
            if ov > overlap:
                bad.append((a.name, fa, b.name, fb, round(dist, 4), round(ov, 4)))
    return bad


def is_string(cube):
    return 'string' in cube.name.lower()


def joined(cubes, slack=0.02, string_slack=0.2):
    """(pieces, free): the cubes that touch or overlap (within `slack` px) grouped into pieces as lists of names, strings
    left out; and the strings that hang more than `string_slack` px from every other cube, as (name, gap). A weapon frame is
    one piece, and only the string may hang free, by up to 0.2 px of its nocks."""
    solid = [c for c in cubes if not is_string(c)]
    parent = list(range(len(solid)))

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i
    for i, j in itertools.combinations(range(len(solid)), 2):
        if gap(solid[i], solid[j]) <= slack:
            parent[find(i)] = find(j)
    pieces = {}
    for i, c in enumerate(solid):
        pieces.setdefault(find(i), []).append(c.name)
    free = []
    for s in cubes:
        if is_string(s) and solid:
            g = min(gap(s, o) for o in solid)
            if g > string_slack + 1e-6:
                free.append((s.name, round(g, 3)))
    return list(pieces.values()), free


def face_rects(model):
    """The distinct texel rectangles the faces of a project read, as whole texels (x0, y0, x1, y1): the low corner floored and
    the high corner ceiled (a patch is rounded up to whole texels on the sheet), in the texture's bitmap texels, sorted."""
    out = set()
    for c in model.cubes():
        for face, (tex, uv) in c.faces.items():
            if tex < 0 or tex >= len(model.textures):
                continue
            t = model.textures[tex]
            ku = t.width / t.uv_width if t.uv_width else 1.0
            kv = t.height / t.uv_height if t.uv_height else 1.0
            xs, ys = sorted((uv[0] * ku, uv[2] * ku)), sorted((uv[1] * kv, uv[3] * kv))
            out.add((math.floor(xs[0] + 1e-6), math.floor(ys[0] + 1e-6), math.ceil(xs[1] - 1e-6), math.ceil(ys[1] - 1e-6)))
    return sorted(out)


def distance(cube, p):
    """How far a point (in Blockbench's space) lies from a cube, 0 inside it: the cube is an oriented box."""
    d = sub(p, cube.center)
    local = [sum(cube.R[r][i] * d[r] for r in range(3)) for i in range(3)]
    return length([max(abs(local[i]) - cube.half[i], 0.0) for i in range(3)])


def main(args):
    args = list(args)
    override = None
    if '--density' in args:
        at = args.index('--density')
        try:
            override = float(args[at + 1])
        except (IndexError, ValueError):
            print('bbmodel: --density needs a number of texels a px')
            return 1
        del args[at:at + 2]
    for path in args:
        model = read(path)
        density = file_density(path, override)
        print('%s (%s texels a px)' % (path, int(density) if density == int(density) else density))
        for problem in check_textures(model):
            print('  texture: ' + problem)
        for name, cubes in model.frames.items():
            quads = len(cubes) * 6
            pieces, free = joined(cubes)
            print('  %-7s cubes %2d quads %3d pieces %d coplanar %d off-density %d free strings %d%s' % (
                name, len(cubes), quads, len(pieces), len(coplanar_faces(cubes)), len(off_density(cubes, density)), len(free),
                '' if model.visible[name] else ' (saved hidden)'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
