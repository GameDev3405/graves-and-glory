"""Checks tools/bbmodel.py (Task 5d, art pipeline v2 section 2.4): the .bbmodel reader, the weapon quads and the weapon checks,
against hand-written fixture projects. Pure: no Minecraft, no Pillow."""
import json
import math
import pathlib
import random
import sys
import tempfile
import unittest
import uuid

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
import bbmodel

FACE_ORDER = ('north', 'east', 'south', 'west', 'up', 'down')


def face_size(frm, to, face):
    w = [b - a for a, b in zip(frm, to)]
    return {'north': (w[0], w[1]), 'south': (w[0], w[1]), 'east': (w[2], w[1]), 'west': (w[2], w[1]),
            'up': (w[0], w[2]), 'down': (w[0], w[2])}[face]


def cube(name, frm, to, rot=None, origin=None, scale=2.0, uv=None, **extra):
    """A Blockbench cube element; every face gets its own patch at `scale` texels per px unless uv names the faces' rectangles."""
    e = {'name': name, 'box_uv': False, 'from': list(frm), 'to': list(to), 'autouv': 0, 'color': 0,
         'origin': list(origin if origin is not None else [(a + b) / 2 for a, b in zip(frm, to)]),
         'type': 'cube', 'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, name))}
    if rot is not None:
        e['rotation'] = list(rot)
    e['faces'] = {}
    for i, face in enumerate(FACE_ORDER):
        w, h = face_size(frm, to, face)
        rect = (uv or {}).get(face)
        if rect is None:
            rect = [2.0 * i, 0.0, 2.0 * i + scale * w, scale * h]   # side by side along the top of the sheet
        e['faces'][face] = {'uv': list(rect), 'texture': 0}
    e.update(extra)
    return e


def group(name, children, origin=(0, 0, 0), rotation=(0, 0, 0), visible=True):
    """A group: (the group object, the outliner node); children are element dicts or (group, node) pairs."""
    gid = str(uuid.uuid5(uuid.NAMESPACE_URL, 'group ' + name))
    node = {'uuid': gid, 'isOpen': False, 'children': [c['uuid'] if isinstance(c, dict) else c[1] for c in children]}
    g = {'name': name, 'uuid': gid, 'export': True, 'visibility': visible, 'origin': list(origin), 'rotation': list(rotation),
         'color': 0, 'children': []}
    return g, node


def project(frames, tex=(64, 128), uv_size=None, root=(), name='fixture', textures=None):
    """A .bbmodel dict: frames is {frame name: [cube, ...]} (a cube may be a (group, node) pair for a nested group)."""
    elements, groups, outliner = [], [], []

    def collect(item):
        if isinstance(item, dict):
            elements.append(item)
        else:
            groups.append(item[0])
            for ch in item[2:]:
                collect(ch)
    for fname, items in frames.items():
        visible = fname == 'rest'
        g, node = group(fname, items, visible=visible)
        groups.append(g)
        outliner.append(node)
        for item in items:
            collect(item)
    for e in root:
        elements.append(e)
        outliner.append(e['uuid'])
    uvw, uvh = uv_size or tex
    return {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False}, 'name': name,
            'resolution': {'width': 16, 'height': 16}, 'elements': elements, 'groups': groups, 'outliner': outliner,
            'textures': textures if textures is not None else [
                {'name': name, 'id': '0', 'width': tex[0], 'height': tex[1], 'uv_width': uvw, 'uv_height': uvh,
                 'relative_path': name + '.png', 'source': 'data:image/png;base64,AAAA'}]}


def nested(name, children, origin=(0, 0, 0), rotation=(0, 0, 0)):
    """A nested group as (group, node, *children) for project()."""
    g, node = group(name, children, origin, rotation)
    return (g, node) + tuple(children)


def write(tmp, data, name='fixture.bbmodel'):
    path = pathlib.Path(tmp) / name
    path.write_text(json.dumps(data), encoding='utf-8')
    return path


def load(data):
    with tempfile.TemporaryDirectory() as tmp:
        return bbmodel.read(write(tmp, data))


def rot_x(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def rot_y(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def rot_z(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def app(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def sub(a, b):
    return [x - y for x, y in zip(a, b)]


def unit(v):
    n = math.sqrt(sum(c * c for c in v))
    return [c / n for c in v]


BB_NORMAL = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def quad_normal(q):
    return unit(cross(sub(q[3:6], q[0:3]), sub(q[9:12], q[0:3])))


class BBModelChecks(unittest.TestCase):
    def test_unrotated_cube(self):
        """A 2 x 4 x 1 cube at the origin: six quads, north first; the north face turns to the weapon frame's south (+z), y and z flipped."""
        m = load(project({'rest': [cube('block', (0, 0, 0), (2, 4, 1), uv={'north': [10, 20, 14, 28]})]}))
        frames = bbmodel.weapon_frames(m)
        self.assertEqual(list(frames), ['rest'])
        quads = frames['rest']
        self.assertEqual(len(quads), 6)
        north = quads[0]
        self.assertEqual(len(north), 20)
        # Blockbench corners (x to, y from), (x from, y from), (x from, y to), (x to, y to) at z = 0, then (x, -y, -z)
        self.assertEqual(north[:12], [2, 0, 0, 0, 0, 0, 0, -4, 0, 2, -4, 0])
        # the face's patch [u1, v1, u2, v2] = [10, 20, 14, 28]: corner 0 is its bottom left, 1 bottom right, 2 top right, 3 top left
        self.assertEqual(north[12:], [10, 28, 14, 28, 14, 20, 10, 20])
        self.assertEqual(quad_normal(north), [0, 0, 1])

    def test_rotated_cube(self):
        """A cube turned 90 degrees about z: its up face (Blockbench +y) lies at x = -2 and faces -x in the weapon frame."""
        m = load(project({'rest': [cube('turned', (-1, -2, -0.5), (1, 2, 0.5), rot=(0, 0, 90), origin=(0, 0, 0))]}))
        up = bbmodel.weapon_frames(m)['rest'][4]
        self.assertEqual(up[:12], [-2, 1, -0.5, -2, -1, -0.5, -2, -1, 0.5, -2, 1, 0.5])
        self.assertEqual(quad_normal(up), [-1, 0, 0])

    def test_normals_and_winding(self):
        """Every face of cubes turned on every axis has its corners counter-clockwise from outside: the corner cross product is
        the cube's rotated face normal through the half turn about x, and the UV area is above zero."""
        rng = random.Random(7)
        cubes = []
        for i in range(12):
            rot = [rng.choice((0, 15, 45, 90, -30, 180)) for _ in range(3)]
            origin = [rng.uniform(-2, 2) for _ in range(3)]
            cubes.append(cube('c%d' % i, (-1, -2.5, -0.75), (1.5, 2, 0.5), rot=rot, origin=origin))
        m = load(project({'rest': cubes}))
        quads = bbmodel.weapon_frames(m)['rest']
        self.assertEqual(len(quads), 72)
        for i, e in enumerate(cubes):
            r = mul(rot_z(e['rotation'][2]), mul(rot_y(e['rotation'][1]), rot_x(e['rotation'][0])))
            for j, face in enumerate(FACE_ORDER):
                n = app(r, BB_NORMAL[face])
                want = [n[0], -n[1], -n[2]]
                got = quad_normal(quads[6 * i + j])
                for a, b in zip(want, got):
                    self.assertAlmostEqual(a, b, places=3, msg='%s %s' % (e['name'], face))
                q = quads[6 * i + j]
                area = abs((q[14] - q[12]) * (q[19] - q[13]) - (q[15] - q[13]) * (q[18] - q[12]))
                self.assertGreater(area, 0)

    def test_inflate_and_degenerate_faces(self):
        """Inflate grows a cube on every side; a flat cube (no thickness) draws only its two big faces."""
        m = load(project({'rest': [cube('puffed', (0, 0, 0), (2, 2, 2), inflate=0.5), cube('card', (0, 0, 0), (2, 2, 0))]}))
        quads = bbmodel.weapon_frames(m)['rest']
        self.assertEqual(len(quads), 6 + 2)
        xs = [q[i] for q in quads[:6] for i in (0, 3, 6, 9)]
        self.assertEqual((min(xs), max(xs)), (-0.5, 2.5))

    def test_group_composition(self):
        """A cube inside a group turned 90 degrees about z at (4, 0, 0) is carried round that pivot after its own rotation."""
        inner = cube('post', (4, 0, 0), (5, 2, 1))
        m = load(project({'rest': [nested('hand', [inner], origin=(4, 0, 0), rotation=(0, 0, 90))]}))
        quads = bbmodel.weapon_frames(m)['rest']
        # the cube's east face (x = 5) corners (Blockbench): (1,0,1)->(5,0,1), (1,0,0), (1,1,0), (1,1,1) with y to: (5,2,...)
        east = quads[1]
        # about (4, 0, 0) by 90 degrees about z: (x, y) -> (4 - y, x - 4): (5, 0) -> (4, 1), (5, 2) -> (2, 1); then (x, -y, -z)
        self.assertEqual(east[:12], [4, -1, -1, 4, -1, 0, 2, -1, 0, 2, -1, -1])

    def test_frame_groups(self):
        """rest alone, and rest with pull_0 to pull_2, hidden or visible, come out as frames in file order."""
        one = lambda n: cube(n, (0, 0, 0), (1, 1, 1))
        m = load(project({'rest': [one('a')]}))
        self.assertEqual(list(bbmodel.weapon_frames(m)), ['rest'])
        data = project({'rest': [one('a')], 'pull_0': [one('b')], 'pull_1': [one('c')], 'pull_2': [one('d')]})
        m = load(data)
        self.assertEqual(list(bbmodel.weapon_frames(m)), ['rest', 'pull_0', 'pull_1', 'pull_2'])
        self.assertEqual([m.visible[n] for n in m.frames], [True, False, False, False])
        for g in data['groups']:
            g['visibility'] = True
        m = load(data)
        self.assertEqual(list(bbmodel.weapon_frames(m)), ['rest', 'pull_0', 'pull_1', 'pull_2'])
        self.assertEqual(len(bbmodel.weapon_frames(m)['pull_2']), 6)

    def test_sword_has_66_quads(self):
        """The spike's sword is eleven cubes: 66 quads, 6 a cube."""
        cubes = [cube('part%d' % i, (0, i, 0), (1, i + 1, 1), rot=(0, 0, 10 * i)) for i in range(11)]
        m = load(project({'rest': cubes}))
        self.assertEqual(len(bbmodel.weapon_frames(m)['rest']), 66)

    def test_structure_errors(self):
        """A cube at the root, a mesh and a missing group each raise BBError naming the thing."""
        one = cube('loose', (0, 0, 0), (1, 1, 1))
        with self.assertRaises(bbmodel.BBError) as caught:
            bbmodel.weapon_frames(load(project({'rest': [cube('a', (0, 0, 0), (1, 1, 1))]}, root=[one])))
        self.assertIn('loose', str(caught.exception))
        self.assertIn('root', str(caught.exception))
        mesh = dict(cube('blob', (0, 0, 0), (1, 1, 1)), type='mesh')
        with self.assertRaises(bbmodel.BBError) as caught:
            load(project({'rest': [mesh]}))
        self.assertIn('blob', str(caught.exception))
        self.assertIn('cubes only', str(caught.exception))
        with self.assertRaises(bbmodel.BBError):
            with tempfile.TemporaryDirectory() as tmp:
                path = pathlib.Path(tmp) / 'broken.bbmodel'
                path.write_text('{"elements": [', encoding='utf-8')
                bbmodel.read(path)

    def test_texture_checks(self):
        """A texture whose UV size differs from its bitmap, two textures, and a face on a texture that is not there are listed."""
        ok = load(project({'rest': [cube('a', (0, 0, 0), (1, 1, 1))]}))
        self.assertEqual(bbmodel.check_textures(ok), [])
        wrong = load(project({'rest': [cube('a', (0, 0, 0), (1, 1, 1))]}, uv_size=(16, 16)))
        said = bbmodel.check_textures(wrong)
        self.assertEqual(len(said), 1)
        self.assertIn('UV size 16x16', said[0])
        self.assertIn('64x128', said[0])
        two = project({'rest': [cube('a', (0, 0, 0), (1, 1, 1))]})
        two['textures'].append(dict(two['textures'][0], id='1', name='other'))
        self.assertIn('one texture', bbmodel.check_textures(load(two))[0])
        off = project({'rest': [cube('a', (0, 0, 0), (1, 1, 1))]})
        off['elements'][0]['faces']['north']['texture'] = 3
        self.assertIn('texture 3', bbmodel.check_textures(load(off))[0])

    def frame_cubes(self, *cubes):
        return load(project({'rest': list(cubes)})).frames['rest']

    def test_coplanar_faces(self):
        """Two cubes with a same-way face on one plane that overlap fail; back-to-back faces and mere neighbours pass."""
        a = cube('a', (0, 0, 0), (2, 2, 1))
        stacked = cube('b', (0.5, 0, 0.5), (1.5, 2, 1))        # b's south face (z = 1) is on a's south plane, inside a's footprint
        said = bbmodel.coplanar_faces(self.frame_cubes(a, stacked))
        self.assertIn(('a', 'south', 'b', 'south'), {(s[0], s[1], s[2], s[3]) for s in said})
        # back to back: b sits behind a, its north face on a's south plane facing the other way
        behind = cube('b', (0, 0, 1), (2, 2, 2))
        self.assertEqual(bbmodel.coplanar_faces(self.frame_cubes(a, behind)), [])
        # neighbours: an end against an end, their top faces in one plane with footprints that only touch
        beside = cube('b', (2, 0, 0), (4, 2, 1))
        self.assertEqual(bbmodel.coplanar_faces(self.frame_cubes(a, beside)), [])
        # a small step (0.05 or more apart) is no z-fight
        step = cube('b', (0.5, 0.5, 0.5), (1.5, 1.5, 1.1))
        self.assertEqual(bbmodel.coplanar_faces(self.frame_cubes(a, step)), [])
        # a rotated cube's face on a plane turned with it
        turned_a = cube('a', (-1, -1, -0.5), (1, 1, 0.5), rot=(0, 0, 30), origin=(0, 0, 0))
        turned_b = cube('b', (-0.5, -1, -0.5), (0.5, 0.5, 0.5), rot=(0, 0, 30), origin=(0, 0, 0))
        self.assertTrue(bbmodel.coplanar_faces(self.frame_cubes(turned_a, turned_b)))

    def test_air_gap(self):
        """Cubes that touch or overlap are one piece; a gap makes two. Only a cube named string may hang free, by up to 0.2 px."""
        a = cube('a', (0, 0, 0), (1, 4, 1))
        b = cube('b', (1, 0, 0), (2, 4, 1))              # touches a
        pieces, free = bbmodel.joined(self.frame_cubes(a, b))
        self.assertEqual((len(pieces), free), (1, []))
        apart = cube('c', (2.5, 0, 0), (3.5, 4, 1))      # 0.5 px of air beside b
        pieces, free = bbmodel.joined(self.frame_cubes(a, b, apart))
        self.assertEqual(sorted(sorted(p) for p in pieces), [['a', 'b'], ['c']])
        near = cube('string', (2.2, 0, 0.4), (2.3, 4, 0.5))   # 0.2 from b along x
        pieces, free = bbmodel.joined(self.frame_cubes(a, b, near))
        self.assertEqual((len(pieces), free), (1, []))
        far = cube('string', (2.3, 0, 0.4), (2.4, 4, 0.5))    # 0.3 px free
        pieces, free = bbmodel.joined(self.frame_cubes(a, b, far))
        self.assertEqual(len(pieces), 1)
        self.assertEqual([f[0] for f in free], ['string'])
        turned = cube('d', (-0.5, 3.5, -0.5), (0.5, 5.5, 0.5), rot=(0, 0, 45), origin=(0, 5, 0))   # its low end swings into a
        apart_turned = cube('e', (-0.5, 4.2, -0.5), (0.5, 6, 0.5), rot=(0, 0, 45), origin=(0, 5, 0))   # but this one swings clear above it
        self.assertEqual(len(bbmodel.joined(self.frame_cubes(a, apart_turned))[0]), 2)
        pieces, free = bbmodel.joined(self.frame_cubes(a, turned))
        self.assertEqual(len(pieces), 1)

    def test_density(self):
        """A face whose UV is not exactly twice its size is listed; a rounded-up patch is not a face of its own."""
        good = cube('good', (0, 0, 0), (1, 2.2, 3))
        self.assertEqual(bbmodel.off_density(self.frame_cubes(good), 2.0), [])
        bad = cube('stretched', (0, 0, 0), (2, 2, 2), uv={'north': [0, 0, 4, 8]})
        said = bbmodel.off_density(self.frame_cubes(bad), 2.0)
        self.assertEqual([(s[0], s[1]) for s in said], [('stretched', 'north')])
        self.assertEqual(said[0][2:], (4.0, 4.0, 4.0, 8.0))
        self.assertEqual(bbmodel.off_density(self.frame_cubes(good), 1.0)[0][1], 'north')   # at 1 texel per px nothing is twice

    def test_density_three(self):
        """At 3 texels a px a face's UV is exactly three times its size: a patch made at 3 passes at 3 and is listed at 2, and the
        other way round."""
        at_three = cube('three', (0, 0, 0), (1, 2.2, 3), scale=3.0)
        self.assertEqual(bbmodel.off_density(self.frame_cubes(at_three), 3.0), [])
        listed = bbmodel.off_density(self.frame_cubes(at_three), 2.0)
        self.assertTrue(listed and all(s[0] == 'three' for s in listed))
        at_two = cube('two', (0, 0, 0), (1, 2.2, 3))
        self.assertTrue(bbmodel.off_density(self.frame_cubes(at_two), 3.0))
        bad = cube('stretched', (0, 0, 0), (2, 2, 2), scale=3.0, uv={'north': [0, 0, 6, 12]})
        said = bbmodel.off_density(self.frame_cubes(bad), 3.0)
        self.assertEqual([(s[0], s[1]) for s in said], [('stretched', 'north')])
        self.assertEqual(said[0][2:], (6.0, 6.0, 6.0, 12.0))

    def test_texture_checks_per_kind(self):
        """A weapon's UV unit is a texel, whatever its density (a 96x192 bitmap has a 96x192 UV size); a troop's UV unit is a
        layout unit, and its bitmap is the layout times the density (384x192 for a 128x64 layout at 3)."""
        one = {'rest': [cube('a', (0, 0, 0), (1, 1, 1))]}
        self.assertEqual(bbmodel.check_textures(load(project(one, tex=(96, 192)))), [])
        self.assertEqual(bbmodel.check_textures(load(project(one, tex=(96, 192))), kind='weapon', density=3), [])
        stretched = load(project(one, tex=(96, 192), uv_size=(64, 128)))
        self.assertIn('UV size 64x128', bbmodel.check_textures(stretched, kind='weapon', density=3)[0])
        troop = load(project(one, tex=(384, 192), uv_size=(128, 64)))
        self.assertEqual(bbmodel.check_textures(troop, kind='troop', density=3), [])
        self.assertEqual(bbmodel.check_textures(troop, kind='troop', density=3, layout=(128, 64)), [])
        as_weapon = bbmodel.check_textures(troop)
        self.assertEqual(len(as_weapon), 1)
        self.assertIn('384x192', as_weapon[0])
        flat = load(project(one, tex=(128, 64), uv_size=(128, 64)))
        said = bbmodel.check_textures(flat, kind='troop', density=3)
        self.assertEqual(len(said), 1)
        self.assertIn('384x192', said[0])
        self.assertIn('3 texels', said[0])
        self.assertEqual(bbmodel.check_textures(flat, kind='troop', density=1), [])
        wrong = bbmodel.check_textures(troop, kind='troop', density=3, layout=(64, 64))
        self.assertEqual(len(wrong), 1)
        self.assertIn('64x64', wrong[0])
        two = project(one, tex=(384, 192), uv_size=(128, 64))
        two['textures'].append(dict(two['textures'][0], id='1', name='other'))
        self.assertIn('one texture', bbmodel.check_textures(load(two), kind='troop', density=3)[0])
        with self.assertRaises(ValueError):
            bbmodel.check_textures(troop, kind='armour')

    def test_density_of_a_weapon_file(self):
        """tools/bbmodel.py on a file reads the density from the weapon's json beside it (2 without one), and --density wins."""
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / 'blade.bbmodel'
            self.assertEqual(bbmodel.file_density(path), 2.0)
            (pathlib.Path(tmp) / 'blade.json').write_text(json.dumps({'weapon': 'blade', 'density': 3}), encoding='utf-8')
            self.assertEqual(bbmodel.file_density(path), 3.0)
            self.assertEqual(bbmodel.file_density(path, 4), 4.0)
            (pathlib.Path(tmp) / 'blade.json').write_text(json.dumps({'weapon': 'blade'}), encoding='utf-8')
            self.assertEqual(bbmodel.file_density(path), 2.0)


if __name__ == '__main__':
    unittest.main()
