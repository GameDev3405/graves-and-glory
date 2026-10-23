"""Checks tools/build_skins.py (Task 5, and Task 5d: sprite weapons, the relief layer, the turnaround sheets) against fixture
sources in a temp dir, without launching the game."""
import contextlib
import copy
import io
import json
import math
import pathlib
import re
import sys
import tempfile
import unittest
import base64

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import build_skins
import test_bbmodel as bbt
from PIL import Image

ASSETS = 'src/main/resources/assets/gravesandglory'
GOLD, VIOLET, CRIMSON, TEAL = 14922561, 11566335, 14701130, 4179904
GREY = {1.0: (160, 160, 160), 0.8: (128, 128, 128), 0.6: (96, 96, 96)}   # the template's three greys
WHITE, POLE = (243, 239, 228), (90, 70, 50)


def box(origin, size, uv, mirror=False):
    return {'origin': origin, 'size': size, 'uv': uv, 'mirror': mirror}


BIPED = {'rig': 'biped', 'size': [64, 64], 'outer': 0.25, 'hands': {'right': 'right_hand', 'left': 'left_hand'}, 'bones': [
    {'name': 'root', 'parent': '', 'pivot': [0, 0, 0], 'boxes': []},
    {'name': 'body', 'parent': 'root', 'pivot': [0, 12, 0], 'boxes': [box([-4, -12, -2], [8, 12, 4], [16, 16])]},
    {'name': 'head', 'parent': 'body', 'pivot': [0, -12, 0], 'boxes': [box([-4, -8, -4], [8, 8, 8], [0, 0])]},
    {'name': 'right_arm', 'parent': 'body', 'pivot': [-5, -10, 0], 'boxes': [box([-3, -2, -2], [4, 12, 4], [40, 16])]},
    {'name': 'right_hand', 'parent': 'right_arm', 'pivot': [-1, 10, 0], 'boxes': []},
    {'name': 'left_arm', 'parent': 'body', 'pivot': [5, -10, 0], 'boxes': [box([-1, -2, -2], [4, 12, 4], [32, 48])]},
    {'name': 'left_hand', 'parent': 'left_arm', 'pivot': [1, 10, 0], 'boxes': []},
    {'name': 'arms_folded', 'parent': 'body', 'pivot': [0, -9, -1], 'rotation': [-43, 0, 0], 'boxes': [box([-4, 2, -2], [8, 4, 4], [40, 40])]},
    {'name': 'right_leg', 'parent': 'root', 'pivot': [-1.9, 12, 0], 'boxes': [box([-2, 0, -2], [4, 12, 4], [0, 16])]},
    {'name': 'left_leg', 'parent': 'root', 'pivot': [1.9, 12, 0], 'boxes': [box([-2, 0, -2], [4, 12, 4], [16, 48])]}]}
HOUND = {'rig': 'hound', 'size': [64, 64], 'outer': 0.25, 'hands': {'right': 'jaw', 'left': 'jaw'}, 'bones': [
    {'name': 'root', 'parent': '', 'pivot': [0, 0, 0], 'boxes': []},
    {'name': 'body', 'parent': 'root', 'pivot': [0, 14, 4], 'boxes': [box([-3, -3, -4], [6, 6, 8], [0, 0])]},
    {'name': 'chest', 'parent': 'body', 'pivot': [0, 0, -4], 'boxes': [box([-3, -3, -6], [6, 6, 6], [0, 14])]},
    {'name': 'head', 'parent': 'chest', 'pivot': [0, -2, -6], 'boxes': [box([-2, -3, -4], [4, 4, 4], [28, 0])]},
    {'name': 'jaw', 'parent': 'head', 'pivot': [0, 1, -4], 'boxes': [box([-1, 0, -3], [2, 1, 3], [44, 0])]},
    {'name': 'front_left_leg', 'parent': 'chest', 'pivot': [2, 3, -3], 'boxes': [box([-1, 0, -1], [2, 7, 2], [0, 26])]},
    {'name': 'front_right_leg', 'parent': 'chest', 'pivot': [-2, 3, -3], 'boxes': [box([-1, 0, -1], [2, 7, 2], [8, 26])]},
    {'name': 'tail', 'parent': 'body', 'pivot': [0, -2, 4], 'boxes': [box([-1, 0, 0], [2, 2, 6], [16, 26])]},
    {'name': 'back_left_leg', 'parent': 'root', 'pivot': [2, 17, 7], 'boxes': [box([-1, 0, -1], [2, 7, 2], [32, 26])]},
    {'name': 'back_right_leg', 'parent': 'root', 'pivot': [-2, 17, 7], 'boxes': [box([-1, 0, -1], [2, 7, 2], [40, 26])]}]}
PARTS = {
    'cap': {'part': 'cap', 'bone': 'head', 'team': False, 'tag': 'helm', 'boxes': [box([-4, -9, -4], [8, 2, 8], [0, 0])]},
    'skirt': {'part': 'skirt', 'bone': 'body', 'team': False, 'tag': '', 'boxes': [box([-4, 0, -2], [8, 4, 4], [0, 0])]},
    'helm': {'part': 'helm', 'bone': 'head', 'team': False, 'tag': 'helm', 'boxes': [box([-4, -9, -4], [8, 3, 8], [0, 0])]},
    'pennant': {'part': 'pennant', 'bone': 'body', 'team': True, 'tag': '', 'boxes': [box([-2, -26, 3], [4, 8, 1], [0, 0])]},
}
SWORD = {'weapon': 'sword', 'thickness': 1, 'grip': {'pixel': [3, 10], 'offset': [0, 0, 0], 'rotation': [90, 45, 90]}}
TROOPS = {
    'knight': {'troop': 'knight', 'rig': 'biped', 'parts': [{'part': 'cap', 'uv': [32, 0]}, {'part': 'skirt', 'uv': [0, 32]}, {'part': 'pennant'}],
               'skins': {'1': 'knight_s1.png'}, 'weapon': 'sword', 'hand': 'right', 'portrait': 'standard'},
    'sentinel': {'troop': 'sentinel', 'rig': 'biped', 'parts': [{'part': 'helm', 'uv': [32, 0]}, {'part': 'pennant'}],
                 'skins': {'1': 'sentinel_s1.png'}, 'weapon': 'sword', 'hand': 'right', 'portrait': 'tall 2'},
    'mage': {'troop': 'mage', 'rig': 'biped', 'parts': [{'part': 'pennant'}], 'skins': {'1': 'mage_s1.png'},
             'weapon': 'none', 'hand': 'right', 'portrait': 'tall 6'},
}
SHEET = {
    'factions': [{'id': 'gold', 'color': GOLD, 'glyph': 'sun'}, {'id': 'violet', 'color': VIOLET, 'glyph': 'moon'},
                 {'id': 'crimson', 'color': CRIMSON, 'glyph': 'star'}, {'id': 'teal', 'color': TEAL, 'glyph': 'wave'}],
    'troops': [{'id': 'knight', 'entity': 'husk', 'rarity': 'common', 'evolveLine': 'knight'},
               {'id': 'sentinel', 'entity': 'husk', 'rarity': 'rare', 'evolveLine': 'sentinel'},
               {'id': 'mage', 'entity': 'husk', 'rarity': 'epic', 'evolveLine': 'none'},
               {'id': 'hound', 'entity': 'wolf', 'rarity': 'common', 'evolveLine': 'hound'}],
    'levelNames': [{'id': 'knight_2', 'troop': 'knight', 'level': 2}, {'id': 'knight_3', 'troop': 'knight', 'level': 3},
                   {'id': 'sentinel_2', 'troop': 'sentinel', 'level': 2}, {'id': 'sentinel_3', 'troop': 'sentinel', 'level': 3}],
}
GLYPH_AT = [(2, 3), (3, 3)]   # the sun mask's pixels, on the pennant field
FIELD = {(x, y): (1.0 if y < 4 else 0.8 if y < 7 else 0.6) for x in range(1, 9) for y in range(1, 9)}
CHARGE_AT, POLE_AT = (5, 5), (0, 0)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1), encoding='utf-8')


def skin(path, size=(64, 64), paint=(200, 196, 186, 255), extra=()):
    im = Image.new('RGBA', size, paint)
    for xy, colour in extra:
        im.putpixel(xy, colour)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def glow(path, lit, size=(64, 64)):
    im = Image.new('RGBA', size, (0, 0, 0, 0))
    for i in range(lit):
        im.putpixel((i % 8, i // 8), (255, 240, 200, 255))
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def fixture(root, troops=None, parts=None, rigs=None, sheet=None):
    """A minimal source tree: the sheet, two rigs, four parts, one weapon, three troops, the cloth template and glyphs."""
    root = pathlib.Path(root)
    write_json(root / 'sheets/design.json', sheet or SHEET)
    for rig in rigs or (BIPED, HOUND):
        write_json(root / 'art/rigs' / (rig['rig'] + '.json'), rig)
    for part in (parts or PARTS).values():
        write_json(root / 'art/parts' / (part['part'] + '.json'), part)
    write_json(root / 'art/weapons/sword.json', SWORD)
    skin(root / 'art/weapons/sword.png', (16, 16), (124, 133, 145, 255))
    for t in (troops or TROOPS).values():
        write_json(root / 'art/troops' / (t['troop'] + '.json'), t)
        for f in t['skins'].values():
            skin(root / 'art/troops' / f)
    cloth = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for xy, k in FIELD.items():
        cloth.putpixel(xy, GREY[k] + (255,))
    cloth.putpixel(CHARGE_AT, WHITE + (255,))
    cloth.putpixel(POLE_AT, POLE + (255,))
    (root / 'art/team').mkdir(parents=True, exist_ok=True)
    cloth.save(root / 'art/team/team_cloth.png')
    for g in ('sun', 'moon', 'star', 'wave'):
        mask = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for xy in (GLYPH_AT if g == 'sun' else [(6, 2)]):
            mask.putpixel(xy, (255, 255, 255, 255))
        mask.save(root / 'art/team' / ('glyph_%s.png' % g))
    return root


def sprite_image(size, pixels=None, colour=(124, 133, 145, 255)):
    """A sprite frame: transparent, with these opaque pixels (every pixel when None)."""
    im = Image.new('RGBA', size, (0, 0, 0, 0))
    for xy in ([(x, y) for y in range(size[1]) for x in range(size[0])] if pixels is None else pixels):
        im.putpixel(xy, colour)
    return im


def put_png(path, im):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


BOW = {'weapon': 'bow', 'thickness': 2, 'grip': {'pixel': [2, 4], 'offset': [0, 0, 0], 'rotation': [0, 0, 0]},
       'pull': [0.15, 0.32, 0.48], 'nocks': [[1.5, 4.5], [1.0, 4.5], [0.5, 4.5], [0.0, 4.5]]}
BOW_COLOURS = [(100 + 30 * i, 120, 140, 255) for i in range(4)]


def put_bow(root, bow=None, **sizes):
    """art/weapons/bow.json and its four 8x9 frames: a column of pixels (grip at (2, 4)), one colour a frame."""
    write_json(root / 'art/weapons/bow.json', bow or BOW)
    for i, name in enumerate(('bow', 'bow_pull_0', 'bow_pull_1', 'bow_pull_2')):
        size = sizes.get(name, (8, 9))
        put_png(root / 'art/weapons' / (name + '.png'), sprite_image(size, [(2, y) for y in range(min(9, size[1]))], BOW_COLOURS[i]))


# ---- Task 5d: cube weapons, made in Blockbench (art pipeline v2) ----

SHEET_2X = (64, 128)
KEYS = {'steel': (255, 0, 0), 'iron': (170, 0, 0), 'gilt': (255, 170, 0), 'wood': (0x8B, 0x45, 0x13), 'linen': (255, 255, 255),
        'leather': (0x55, 0x33, 0x22)}
PAINT = {'steel': (124, 133, 145), 'iron': (90, 96, 104), 'gilt': (150, 120, 58), 'wood': (110, 84, 64), 'linen': (191, 181, 154),
         'leather': (90, 62, 44)}
# a pike: a leather grip, a gilt guard across its top and a steel blade; the cube specs are (name, from, to, material, rotation)
PIKE = [('grip', (-0.5, -2, -0.5), (0.5, 2, 0.5), 'leather', None),
        ('guard', (-2, 2, -0.5), (2, 3, 0.5), 'gilt', None),
        ('blade', (-0.5, 3, -0.25), (0.5, 10, 0.25), 'steel', None)]
PIKE_JSON = {'weapon': 'pike', 'thickness': 1, 'grip': {'offset': [0, 0, 0], 'rotation': [0, 0, 0]}}
# a bow: a leather grip between wood risers, wood limbs with tips out to the string, and the string 0.2 px in front of the tips
BOW_NOCKS = [[-2.8, 0], [-2.9, 0], [-3.0, 0], [-3.1, 0]]


def bow_specs(shift=0.0):
    x = -2.8 - shift
    return [('grip', (-0.5, -2, -0.5), (0.5, 2, 0.5), 'leather', None),
            ('riser_hi', (-0.4, 2, -0.5), (0.4, 7, 0.5), 'wood', None), ('riser_lo', (-0.4, -7, -0.5), (0.4, -2, 0.5), 'wood', None),
            ('tip_hi', (-3.0, 7, -0.5), (0.4, 8, 0.5), 'wood', None), ('tip_lo', (-3.0, -8, -0.5), (0.4, -7, 0.5), 'wood', None),
            ('string', (x - 0.1, -7.5, -0.1), (x + 0.1, 7.5, 0.1), 'linen', None)]


BOW_FRAMES = {'rest': bow_specs(0), 'pull_0': bow_specs(0.1), 'pull_1': bow_specs(0.2), 'pull_2': bow_specs(0.3)}
BOW_JSON = {'weapon': 'recurve', 'thickness': 1, 'grip': {'offset': [0, 0, 0], 'rotation': [0, 0, 0]}, 'pull': [0.15, 0.32, 0.48],
            'nocks': BOW_NOCKS}


def data_url(im):
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def pack_frames(frames, sheet=SHEET_2X):
    """Gives every face of every cube its own patch at 2 texels per px on a shelf of the sheet, painted in the cube's material
    with its key colour in the material map. Returns ({frame: [cube element, ...]}, colour image, key image)."""
    colour, keys = Image.new('RGBA', sheet, (0, 0, 0, 0)), Image.new('RGBA', sheet, (0, 0, 0, 0))
    x = y = row = 0
    out, patches = {}, {}   # a cube of the same name and size in another frame keeps its patch (Blockbench's duplicate keeps the UVs)
    for frame, specs in frames.items():
        cubes = []
        for name, frm, to, material, rot in specs:
            uv = {}
            for face in bbt.FACE_ORDER:
                w, h = bbt.face_size(frm, to, face)
                if (name, face, round(w, 4), round(h, 4)) in patches:
                    uv[face] = patches[(name, face, round(w, 4), round(h, 4))]
                    continue
                pw, ph = 2 * w, 2 * h
                cw, ch = max(1, math.ceil(pw - 1e-6)), max(1, math.ceil(ph - 1e-6))
                if x + cw > sheet[0]:
                    x, y, row = 0, y + row, 0
                uv[face] = patches[(name, face, round(w, 4), round(h, 4))] = [x, y, x + pw, y + ph]
                for yy in range(y, y + ch):
                    for xx in range(x, x + cw):
                        colour.putpixel((xx, yy), PAINT[material] + (255,))
                        keys.putpixel((xx, yy), KEYS[material] + (255,))
                x += cw
                row = max(row, ch)
            cubes.append(bbt.cube(name, frm, to, rot=rot, uv=uv))
        out[frame] = cubes
    return out, colour, keys


def put_cube_weapon(root, wid='pike', frames=None, meta=None, tweak=None, sheet=SHEET_2X, root_cubes=()):
    """art/weapons/<wid>.bbmodel (the texture embedded), .json, .png and _m.png from cube specs; tweak(data, colour, keys, meta)
    may change anything before it is written. Returns the project dict."""
    root = pathlib.Path(root)
    cubes, colour, keys = pack_frames(frames or {'rest': PIKE}, sheet)
    meta = copy.deepcopy(meta or PIKE_JSON)
    meta['weapon'] = wid
    data = bbt.project(cubes, tex=sheet, name=wid, root=list(root_cubes))
    data['textures'][0]['source'] = data_url(colour)
    if tweak:
        tweak(data, colour, keys, meta)
    folder = root / 'art/weapons'
    folder.mkdir(parents=True, exist_ok=True)
    (folder / (wid + '.bbmodel')).write_text(json.dumps(data), encoding='utf-8')
    write_json(folder / (wid + '.json'), meta)
    colour.save(folder / (wid + '.png'))
    keys.save(folder / (wid + '_m.png'))
    return data


def cube_fixture(tmp, **kw):
    root = fixture(tmp)
    put_cube_weapon(root, **kw)
    return root


def model_of(root, wid='pike'):
    return json.loads((pathlib.Path(root) / ASSETS / ('models/troop/weapon/%s.json' % wid)).read_text(encoding='utf-8'))


def texture_of(root, name):
    with Image.open(pathlib.Path(root) / ASSETS / 'textures/entity/troop/weapon' / name) as im:
        return im.convert('RGBA')


NORMALS = ((0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0))   # Minecraft's Direction order, in model space
AXIS = (1, 1, 2, 2, 0, 0)   # the axis each Direction index lies on
MINT = (120, 100, 80, 255)   # a relief colour no rule objects to
RELIEF_PARTS = dict(PARTS,
                    panel={'part': 'panel', 'bone': 'body', 'team': True, 'tag': '', 'boxes': [box([-3, -10, -3], [6, 6, 1], [0, 9])]},
                    flap={'part': 'flap', 'bone': 'body', 'team': False, 'tag': '', 'boxes': [box([-2, 0, -2.5], [4, 4, 0], [0, 0])]},
                    plain={'part': 'plain', 'bone': 'head', 'team': False, 'tag': '', 'boxes': [box([-4, -9, -4], [8, 2, 8], [0, 0])]})


def relief_png(root, name, texels, size=(64, 64), colour=MINT):
    """art/troops/<name>: transparent, with these texels raised (a texel may carry its own (x, y, colour))."""
    im = Image.new('RGBA', size, (0, 0, 0, 0))
    for t in texels:
        im.putpixel(tuple(t[:2]), t[2] if len(t) > 2 else colour)
    put_png(root / 'art/troops' / name, im)
    return im


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def sub(a, b):
    return [x - y for x, y in zip(a, b)]


def uv_corners(q):
    """The UV of a quad's corners 0, 1, 2 and 3 (corner 2 is corner 1 + corner 3 - corner 0)."""
    u0, u1, u3 = (q[7], q[8]), (q[9], q[10]), (q[11], q[12])
    return [u0, u1, (u1[0] + u3[0] - u0[0], u1[1] + u3[1] - u0[1]), u3]


def uv_area(q):
    c = uv_corners(q)
    return abs((c[1][0] - c[0][0]) * (c[3][1] - c[0][1]) - (c[1][1] - c[0][1]) * (c[3][0] - c[0][0]))


def uv_box(q):
    c = uv_corners(q)
    return min(p[0] for p in c), min(p[1] for p in c), max(p[0] for p in c), max(p[1] for p in c)


def vanilla_uv(box, normal, point):
    """The UV (texels) that 1.20.1's ModelPart.Cuboid gives a point on the face whose geometric normal is `normal`: the
    constructor's vertices, UV rectangles and quad lists transcribed as written, the mirror swap and the flipped x of
    the normal included; the point is read as lying on that face's plane."""
    (u, v), (sx, sy, sz) = box['uv'], box['size']
    o, d = box['origin'], box['dilation']
    x, y, z = o[0] - d[0], o[1] - d[1], o[2] - d[2]
    j, k, l = o[0] + sx + d[0], o[1] + sy + d[1], o[2] + sz + d[2]
    if box['mirror']:
        x, j = j, x
    vert = {1: (x, y, z), 2: (j, y, z), 3: (j, k, z), 4: (x, k, z), 5: (x, y, l), 6: (j, y, l), 7: (j, k, l), 8: (x, k, l)}
    m, n, o2, p, q, r = u, u + sz, u + sz + sx, u + sz + sx + sx, u + sz + sx + sz, u + sz + sx + sz + sx
    s, t, w = v, v + sz, v + sz + sy
    sides = [([6, 5, 1, 2], n, s, o2, t, (0, -1, 0)), ([3, 4, 8, 7], o2, t, p, s, (0, 1, 0)), ([1, 5, 8, 4], m, t, n, w, (-1, 0, 0)),
             ([2, 1, 4, 3], n, t, o2, w, (0, 0, -1)), ([6, 2, 3, 7], o2, t, q, w, (1, 0, 0)), ([5, 6, 7, 8], q, t, r, w, (0, 0, 1))]
    for names, u1, v1, u2, v2, direction in sides:
        if box['mirror']:
            direction = (-direction[0], direction[1], direction[2])
        if tuple(direction) != tuple(normal):
            continue
        pos = [vert[i] for i in names]
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        e1, e3 = sub(pos[1], pos[0]), sub(pos[3], pos[0])
        rel = sub(point, pos[0])
        a, b = dot(rel, e1) / dot(e1, e1), dot(rel, e3) / dot(e3, e3)
        return (uvs[0][0] + a * (uvs[1][0] - uvs[0][0]) + b * (uvs[3][0] - uvs[0][0]),
                uvs[0][1] + a * (uvs[1][1] - uvs[0][1]) + b * (uvs[3][1] - uvs[0][1]))
    raise AssertionError('no face with the normal %s' % (normal,))


def mini_model(size=(4, 4, 4), origin=(-2, -2, -2), uv=(0, 0), mirror=False, dilation=(0, 0, 0), sheet=(32, 32)):
    """A model of one bone with one box, as bake gives it (what relief_mesh reads)."""
    box = {'origin': list(origin), 'size': list(size), 'uv': list(uv), 'mirror': mirror, 'dilation': list(dilation)}
    bone = {'name': 'body', 'parent': '', 'part': '', 'team': False, 'tag': '', 'stages': [], 'sheet': list(sheet),
            'pivot': [0, 0, 0], 'rotation': [0, 0, 0], 'boxes': [box]}
    return {'size': list(sheet), 'bones': [bone], 'glowFrom': 1}, box


def export_assets(root):
    return build_skins.export(pathlib.Path(root), pathlib.Path(root) / ASSETS)


def find(model, name):
    return next(b for b in model['bones'] if b['name'] == name)


def absolute_bottom(model, name):
    """The lowest y of a bone's boxes in model space; the fixture legs have no rotation on their chain."""
    bones = {b['name']: b for b in model['bones']}
    y, b = 0.0, bones[name]
    chain = b
    while chain:
        y += chain['pivot'][1]
        chain = bones.get(chain['parent'])
    return max(y + bx['origin'][1] + bx['size'][1] + bx['dilation'][1] for bx in b['boxes'])


class BuildSkinsChecks(unittest.TestCase):
    def fails(self, fn, *args):
        """Runs fn, which must exit 1 through build_skins.fail; returns what it printed."""
        out = io.StringIO()
        with contextlib.redirect_stdout(out), self.assertRaises(SystemExit) as caught:
            fn(*args)
        self.assertEqual(caught.exception.code, 1, out.getvalue())
        return out.getvalue()

    def export(self, root):
        with contextlib.redirect_stdout(io.StringIO()):
            return export_assets(root)

    def bake(self, troops):
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp, troops=troops))
            return {t: build_skins.bake(cat, t) for t in troops}

    def with_knight(self, **changes):
        troops = copy.deepcopy(TROOPS)
        troops['knight'].update(changes)
        return troops

    def test_bake_scale_about_pivot(self):
        model = self.bake(self.with_knight(bones={'body': {'scale': [1.2, 1, 1]}}))['knight']
        body = find(model, 'body')['boxes'][0]
        self.assertAlmostEqual(body['origin'][0], -4.0)
        self.assertEqual(body['size'][0], 8)
        self.assertAlmostEqual(body['dilation'][0], 0.8)
        self.assertAlmostEqual(find(model, 'right_arm')['pivot'][0], -6.0)
        self.assertAlmostEqual(find(model, 'left_arm')['pivot'][0], 6.0)
        self.assertAlmostEqual(find(model, 'head')['pivot'][0], 0.0)

    def test_feet_on_ground(self):
        legs = {'right_leg': {'scale': [1, 1.15, 1]}, 'left_leg': {'scale': [1, 1.15, 1]}}
        model = self.bake(self.with_knight(bones=legs))['knight']
        for leg in ('right_leg', 'left_leg'):
            self.assertAlmostEqual(absolute_bottom(model, leg), 24.0)
        self.assertAlmostEqual(find(model, 'root')['pivot'][1], -1.8)
        plain = self.bake(TROOPS)['knight']
        self.assertAlmostEqual(absolute_bottom(plain, 'right_leg'), 24.0)

    def test_hunch(self):
        model = self.bake(self.with_knight(hunch=20))['knight']
        self.assertAlmostEqual(find(model, 'body')['rotation'][0], 20)
        for bone in ('head', 'right_arm', 'left_arm'):
            self.assertAlmostEqual(find(model, bone)['rotation'][0], -20)

    def test_uv_overlap(self):
        troops = self.with_knight(parts=[{'part': 'cap', 'uv': [32, 0]}, {'part': 'skirt', 'uv': [32, 0]}, {'part': 'pennant'}])
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, fixture(tmp, troops=troops))
        self.assertIn('knight', said)
        self.assertIn('cap', said)
        self.assertIn('skirt', said)

    def test_bad_names(self):
        cases = {
            'quadruped': self.with_knight(rig='quadruped'),
            'tail': self.with_knight(bones={'tail': {'scale': 1.1}}),
            'wings': self.with_knight(parts=[{'part': 'wings', 'uv': [32, 0]}]),
            'hound': self.with_knight(rig='hound'),
            'head': self.with_knight(parts=[{'part': 'cap', 'name': 'head', 'uv': [32, 0]}]),
        }
        cases['skirt'] = self.with_knight(parts=[{'part': 'skirt', 'uv': [0, 32]}, {'part': 'skirt', 'uv': [0, 40]}])
        for name, troops in cases.items():
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                said = self.fails(export_assets, fixture(tmp, troops=troops))
                self.assertIn('knight', said)
                self.assertIn(name, said)

    def test_vanilla_keys(self):
        vanilla = {'parent', 'elements', 'textures', 'display', 'overrides', 'gui_light', 'ambientocclusion'}
        allowed = {'format', 'troop', 'rig', 'size', 'bones', 'skins', 'veteran', 'glow', 'glowFrom', 'outer', 'weapon',
                   'hand', 'idleArms', 'portrait'}
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            self.export(root)
            models = sorted((root / ASSETS / 'models/troop').rglob('*.json'))
            self.assertTrue(models)
            for path in models:
                data = json.loads(path.read_text(encoding='utf-8'))
                with self.subTest(path=path.name):
                    self.assertIsInstance(data, dict)
                    self.assertFalse(vanilla & set(data), sorted(vanilla & set(data)))
                    if path.parent.name == 'troop' and path.name != 'index.json':
                        self.assertLessEqual(set(data), allowed)

    def test_skin_names(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            glow(root / 'art/troops/mage_e.png', 4)
            self.export(root)
            textures = root / ASSETS / 'textures/entity/troop'
            names = [p.name for p in textures.glob('*.png') if not p.name.startswith('team_cloth_')]
            self.assertIn('mage_e.png', names)
            for name in names:
                self.assertRegex(name, r'^[a-z0-9_]+_(s\d+|veteran|e|o)\.png$')
            skin(root / 'art/troops/knight.png')
            said = self.fails(export_assets, root)
            self.assertIn('knight.png', said)

    def test_glow_cap(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            glow(root / 'art/troops/knight_e.png', 7)
            said = self.fails(export_assets, root)
            self.assertIn('knight', said)
            self.assertIn('glow', said)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            glow(root / 'art/troops/mage_e.png', 16)
            self.export(root)
            self.assertTrue((root / ASSETS / 'textures/entity/troop/mage_e.png').is_file())

    def test_tint_rule(self):
        for colour, ok in (((0xE3, 0xB3, 0x41, 255), False), ((0x96, 0x78, 0x3A, 255), True)):
            with self.subTest(colour=colour), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                skin(root / 'art/troops/knight_s1.png', extra=[((10, 10), colour)])
                if ok:
                    self.export(root)
                else:
                    said = self.fails(export_assets, root)
                    self.assertIn('knight', said)
                    self.assertIn('knight_s1.png', said)

    def test_team_cloth(self):
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp))
            gold, linen = build_skins.cloth(cat, 'gold').convert('RGBA'), build_skins.cloth(cat, None).convert('RGBA')
            company = build_skins.cloth(cat, 'company').convert('RGBA')
        rgb = ((GOLD >> 16) & 255, (GOLD >> 8) & 255, GOLD & 255)
        for xy, k in FIELD.items():
            if xy in GLYPH_AT or xy == CHARGE_AT:
                continue
            want = tuple(int(c * k + 0.5) for c in rgb)
            self.assertEqual(gold.getpixel(xy)[:3], want, (xy, k))
            self.assertEqual(linen.getpixel(xy)[:3], tuple(int(c * k + 0.5) for c in (0xCF, 0xC6, 0xB0)), (xy, k))
            self.assertEqual(company.getpixel(xy)[:3], tuple(int(c * k + 0.5) for c in (0x2A, 0x37, 0x47)), (xy, k))
        self.assertEqual(gold.getpixel((2, 2))[:3], rgb)   # grey x1.0 is the side colour itself
        for xy in GLYPH_AT:
            self.assertEqual(gold.getpixel(xy)[:3], build_skins.GLYPH)
            self.assertEqual(linen.getpixel(xy)[:3], (0xCF, 0xC6, 0xB0))
            self.assertEqual(company.getpixel(xy)[:3], (0x2A, 0x37, 0x47))   # the company's cloth carries no glyph
        self.assertEqual(gold.getpixel(CHARGE_AT)[:3], WHITE)
        self.assertEqual(gold.getpixel(POLE_AT)[:3], POLE)
        self.assertEqual(linen.getpixel(CHARGE_AT)[:3], WHITE)
        self.assertEqual(gold.getpixel((15, 15))[3], 0)

    def test_index_and_stale(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            index = self.export(root)
            on_disk = json.loads((root / ASSETS / 'models/troop/index.json').read_text(encoding='utf-8'))
            self.assertEqual(index, on_disk)
            self.assertEqual(index['troops'], ['knight', 'mage', 'sentinel'])
            self.assertEqual(index['rigs'], ['biped', 'hound'])
            self.assertEqual(index['weapons'], ['sword'])
            written = sorted(p.relative_to(root / ASSETS).as_posix() for p in (root / ASSETS).rglob('*') if p.is_file())
            self.assertEqual(sorted(index['outputs'] + ['models/troop/index.json']), written)
            for side in ('gold', 'violet', 'crimson', 'teal', 'linen', 'company'):
                self.assertIn('textures/entity/troop/team_cloth_%s.png' % side, index['outputs'])
            self.assertIn('models/troop/sentinel.json', index['outputs'])
            (root / 'art/troops/sentinel.json').unlink()
            (root / 'art/troops/sentinel_s1.png').unlink()
            index = self.export(root)
            self.assertEqual(index['troops'], ['knight', 'mage'])
            self.assertFalse((root / ASSETS / 'models/troop/sentinel.json').exists())
            self.assertFalse((root / ASSETS / 'textures/entity/troop/sentinel_s1.png').exists())
            self.assertNotIn('models/troop/sentinel.json', index['outputs'])
            self.assertTrue((root / ASSETS / 'models/troop/knight.json').is_file())
            raw = (root / ASSETS / 'models/troop/index.json').read_bytes()
            self.assertNotIn(b'\r', raw)
            self.assertTrue(raw.endswith(b'\n'))

    def test_verify(self):
        for target in ('models/troop/knight.json', 'textures/entity/troop/knight_s1.png'):
            with self.subTest(target=target), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                self.export(root)
                with contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(build_skins.verify(root), 0)
                path = root / ASSETS / target
                data = bytearray(path.read_bytes())
                data[len(data) // 2] ^= 0x01
                path.write_bytes(bytes(data))
                said = self.fails(build_skins.verify, root)
                self.assertIn(target, said)

    def test_verify_pixels(self):
        """--verify compares a PNG by its pixels: the same pixels in other bytes pass, and export leaves them alone; one
        changed pixel in a source fails, naming the output and the pixel."""
        target = 'textures/entity/troop/knight_s1.png'
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            self.export(root)
            path = root / ASSETS / target
            before = path.read_bytes()
            with Image.open(path) as im:
                same = im.copy()
            same.save(path, 'PNG', compress_level=1)
            recoded = path.read_bytes()
            self.assertNotEqual(recoded, before)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.verify(root), 0)
            self.export(root)
            self.assertEqual(path.read_bytes(), recoded)
            source = root / 'art/troops/knight_s1.png'
            with Image.open(source) as im:
                changed = im.copy()
            changed.putpixel((3, 2), (90, 90, 90, 255))
            changed.save(source)
            said = self.fails(build_skins.verify, root)
            self.assertIn(target, said)
            self.assertIn('pixel (3, 2)', said)


    def test_rig_tree(self):
        """A rig keeps its skeleton's tree, not only its bone names (finding 5): the renderer and Task 5b's clips rely on it."""
        def moved(rig, bone, parent):
            rig = copy.deepcopy(rig)
            next(b for b in rig['bones'] if b['name'] == bone)['parent'] = parent
            return rig
        for rig, bone, parent, want in ((BIPED, 'head', 'root', 'body'), (BIPED, 'right_hand', 'body', 'right_arm'),
                                        (HOUND, 'head', 'body', 'chest')):
            with self.subTest(rig=rig['rig'], bone=bone), tempfile.TemporaryDirectory() as tmp:
                rigs = [moved(rig, bone, parent) if r is rig else r for r in (BIPED, HOUND)]
                said = self.fails(export_assets, fixture(tmp, rigs=rigs))
                self.assertIn('rig %s' % rig['rig'], said)
                self.assertIn('%s has parent %s' % (bone, parent), said)
                self.assertIn('hangs it from %s' % want, said)

    def test_team_anchor(self):
        """A part hung from a team part fails: neither pass of the renderer could draw it (finding 5)."""
        parts = dict(PARTS)
        parts['badge'] = {'part': 'badge', 'bone': 'pennant', 'team': False, 'tag': '', 'boxes': [box([0, 0, 0], [1, 1, 1], [0, 0])]}
        parts['streamer'] = {'part': 'streamer', 'bone': 'pennant', 'team': True, 'tag': '',
                             'boxes': [box([0, 0, 0], [1, 2, 1], [10, 10])]}
        for extra in ({'part': 'badge', 'uv': [48, 0]}, {'part': 'streamer'}):
            troops = self.with_knight(parts=TROOPS['knight']['parts'] + [extra])
            with self.subTest(part=extra['part']), tempfile.TemporaryDirectory() as tmp:
                said = self.fails(export_assets, fixture(tmp, troops=troops, parts=parts))
                self.assertIn('knight: part %s hangs from the team part pennant' % extra['part'], said)
        with tempfile.TemporaryDirectory() as tmp:
            self.export(fixture(tmp, parts=parts))   # the parts themselves are fine while no troop hangs them there

    def test_template(self):
        """--template paints every skin box face flat: one hue per bone, the top lighter and the bottom darker than the
        front, nothing where no skin box lies and nothing for a team part, whose UVs are on the cloth sheet."""
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp))
            im = build_skins.template(cat, 'knight').convert('RGBA')
        self.assertEqual(im.size, (64, 64))
        body, head = build_skins.HUES[0], build_skins.HUES[1]   # root has no box, so body takes the first hue

        def shade(rgb, k):
            return tuple(max(0, min(255, int(c * k))) for c in rgb) + (255,)
        self.assertEqual(im.getpixel((24, 26)), shade(body, 1.0))     # body north (front), uv 16,16 size 8x12x4
        self.assertEqual(im.getpixel((24, 18)), shade(body, 1.25))    # body top
        self.assertEqual(im.getpixel((31, 18)), shade(body, 0.55))    # body bottom
        self.assertEqual(im.getpixel((38, 26)), shade(body, 0.7))     # body south (back)
        self.assertEqual(im.getpixel((12, 12)), shade(head, 1.0))     # head north
        self.assertNotEqual(shade(body, 1.0), shade(head, 1.0))
        self.assertEqual(im.getpixel((2, 0))[3], 0)    # the pennant's top face lies here on the cloth sheet: not painted
        self.assertEqual(im.getpixel((63, 63))[3], 0)  # no box
        cap = im.getpixel((44, 9))                     # the cap part at uv 32,0 (size 8x2x8): its north face, y 8..9
        self.assertEqual(cap[3], 255)
        self.assertNotIn(cap, (shade(body, 1.0), shade(head, 1.0)))

    def test_exported_pixels(self):
        """The exported skin, outer, glow and weapon PNGs carry the sources' pixels; a transparent texel leaves with no
        colour, since the glow pass adds RGB."""
        marks = [((3, 2), (90, 60, 40, 255)), ((40, 20), (20, 20, 24, 255)), ((5, 5), (210, 30, 30, 0))]
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            skin(root / 'art/troops/knight_s1.png', extra=marks)
            skin(root / 'art/troops/knight_o.png', paint=(0, 0, 0, 0), extra=[((8, 8), (120, 110, 100, 255))])
            glow(root / 'art/troops/mage_e.png', 4)
            skin(root / 'art/weapons/sword.png', (16, 16), (124, 133, 145, 255), extra=[((7, 1), (230, 230, 240, 255))])
            self.export(root)
            out = root / ASSETS / 'textures/entity/troop'
            with Image.open(out / 'knight_s1.png') as im:
                s1 = im.convert('RGBA')
            with Image.open(out / 'knight_o.png') as im:
                outer = im.convert('RGBA')
            with Image.open(out / 'mage_e.png') as im:
                lit = im.convert('RGBA')
            with Image.open(out / 'weapon/sword.png') as im:
                sword = im.convert('RGBA')
            knight = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
        self.assertEqual(s1.size, (64, 64))
        self.assertEqual(s1.getpixel((3, 2)), (90, 60, 40, 255))
        self.assertEqual(s1.getpixel((40, 20)), (20, 20, 24, 255))
        self.assertEqual(s1.getpixel((5, 5)), (0, 0, 0, 0))
        self.assertEqual(s1.getpixel((30, 30)), (200, 196, 186, 255))
        self.assertEqual(outer.getpixel((8, 8)), (120, 110, 100, 255))
        self.assertEqual(outer.getpixel((9, 8)), (0, 0, 0, 0))
        self.assertTrue(knight['outer'].endswith('/knight_o.png'), knight['outer'])
        self.assertEqual([(x, y) for y in range(64) for x in range(64) if lit.getpixel((x, y))[3]], [(0, 0), (1, 0), (2, 0), (3, 0)])
        self.assertEqual(lit.getpixel((2, 0)), (255, 240, 200, 255))
        self.assertEqual(sword.size, (16, 16))
        self.assertEqual(sword.getpixel((7, 1)), (230, 230, 240, 255))
        self.assertEqual(sword.getpixel((0, 15)), (124, 133, 145, 255))

    def test_sheet_rules(self):
        """A box outside the sheet, a skin of the wrong size and a skin for a stage the line lacks each fail, naming the troop."""
        mage = copy.deepcopy(TROOPS)
        mage['mage']['skins'] = {'1': 'mage_s1.png', '2': 'mage_s2.png'}
        cases = [
            ('knight', 'outside the 64x64 sheet',
             self.with_knight(parts=[{'part': 'cap', 'uv': [50, 0]}, {'part': 'skirt', 'uv': [0, 32]}, {'part': 'pennant'}]), False),
            ('knight', 'knight_s1.png is 32x32, not the troop size 64x64', TROOPS, True),
            ('knight', 'a skin for stage 4, which the evolution line lacks',
             self.with_knight(skins={'1': 'knight_s1.png', '4': 'knight_s4.png'}), False),
            ('mage', 'a skin for stage 2, which the evolution line lacks', mage, False),
        ]
        for troop, want, troops, small in cases:
            with self.subTest(want=want), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp, troops=troops)
                if small:
                    skin(root / 'art/troops/knight_s1.png', size=(32, 32))
                said = self.fails(export_assets, root)
                self.assertIn(want, said)
                self.assertIn(troop, said)

    def test_part_scale(self):
        """A part takes its anchor bone's scale times its own (the part file's scale, then the troop's for it)."""
        parts = copy.deepcopy(PARTS)
        parts['cap']['scale'] = [1.25, 1, 1]
        troops = self.with_knight(bones={'head': {'scale': 1.1}},
                                  parts=[{'part': 'cap', 'uv': [32, 0], 'scale': 1.2}, {'part': 'skirt', 'uv': [0, 32]}, {'part': 'pennant'}])
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp, troops=troops, parts=parts))
            model = build_skins.bake(cat, 'knight')
        cap = find(model, 'cap')['boxes'][0]
        k = (1.1 * 1.25 * 1.2, 1.1 * 1.2, 1.1 * 1.2)
        for i, (origin, size) in enumerate(zip([-4, -9, -4], [8, 2, 8])):
            grow = size * (k[i] - 1) / 2
            self.assertAlmostEqual(cap['dilation'][i], grow, places=4)
            self.assertAlmostEqual(cap['origin'][i], k[i] * origin + grow, places=4)
        self.assertEqual(cap['size'], [8, 2, 8])
        head = find(model, 'head')['boxes'][0]
        self.assertAlmostEqual(head['dilation'][0], 8 * 0.1 / 2, places=4)

    def test_bone_cap(self):
        """A troop may have 64 bones, and a 65th fails naming the troop (Task 5 review, finding 3): TroopModel records the
        bones a draw shows as the bits of one long, and the client refuses a bigger model, which then draws vanilla."""
        def flags(n):
            return self.with_knight(parts=TROOPS['knight']['parts'] + [{'part': 'pennant', 'name': 'pennant_%d' % i} for i in range(n)])
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp, troops=flags(51)))
            self.assertEqual(len(build_skins.bake(cat, 'knight')['bones']), build_skins.MAX_BONES)
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, fixture(tmp, troops=flags(52)))
        self.assertIn('knight: 65 bones, more than 64', said)

    # ---- Task 5d: sprite weapons ----

    def assert_quad(self, q, what=''):
        """A quad as the client reads it: 13 numbers, a direction, equal min and max on its axis, corners counter-clockwise
        from outside (their cross product along the normal, not just toward it), and UVs of area above 0."""
        self.assertEqual(len(q), 13, what)
        d = q[0]
        self.assertIn(d, range(6), what)
        lo, hi = q[1:4], q[4:7]
        self.assertEqual(lo[AXIS[d]], hi[AXIS[d]], what)
        for i in range(3):
            self.assertLessEqual(lo[i], hi[i], what)
        c = build_skins.quad_corners(d, lo, hi)
        n = cross(sub(c[1], c[0]), sub(c[3], c[0]))
        self.assertGreater(dot(n, NORMALS[d]), 0, what)
        self.assertAlmostEqual(dot(n, NORMALS[d]), math.sqrt(dot(n, n)), places=6, msg=what)
        self.assertGreater(uv_area(q), 0, what)

    def test_sprite_extrude(self):
        """An L of four pixels, thickness 1, gripped at its corner pixel: two big faces, then the six merged side runs."""
        im = sprite_image((4, 4), [(0, 0), (0, 1), (0, 2), (1, 2)])
        quads = build_skins.sprite_mesh(im, 1, (0, 2), 0)
        self.assertEqual(len(quads), 8)
        self.assertEqual([q[0] for q in quads[:2]], [2, 3])   # north then south
        for q in quads[:2]:
            self.assertEqual((q[1], q[2], q[4], q[5]), (-0.5, -2.5, 3.5, 1.5))   # the whole 4x4 frame about the grip pixel's centre
        self.assertEqual((quads[0][3], quads[1][3]), (-0.5, 0.5))
        runs = {(q[0], tuple(q[1:4]), tuple(q[4:7])) for q in quads[2:]}
        self.assertEqual(runs, {
            (4, (-0.5, -2.5, -0.5), (-0.5, 0.5, 0.5)),     # the left edge, rows 0 to 2
            (0, (-0.5, -2.5, -0.5), (0.5, -2.5, 0.5)),     # the top of the pixel (0, 0)
            (0, (0.5, -0.5, -0.5), (1.5, -0.5, 0.5)),      # the top of the foot
            (1, (-0.5, 0.5, -0.5), (1.5, 0.5, 0.5)),       # the bottom, two pixels in one run
            (5, (0.5, -2.5, -0.5), (0.5, -0.5, 0.5)),      # the right edge of the bar, rows 0 and 1
            (5, (1.5, -0.5, -0.5), (1.5, 0.5, 0.5))})      # the right edge of the foot
        self.assertEqual(len(runs), 6)
        for q in quads:
            self.assert_quad(q)

    def test_sprite_grip_uv(self):
        im = sprite_image((4, 4), [(0, 0), (0, 1), (0, 2), (1, 2)])
        for t in (1, 2, 3):
            with self.subTest(thickness=t):
                quads = build_skins.sprite_mesh(im, t, (0, 2), 2)   # the third column of a packed sheet: u starts at 8
                zs = sorted({q[3] for q in quads} | {q[6] for q in quads})
                self.assertEqual((zs[0], zs[-1]), (-t / 2, t / 2))
                bottom = next(q for q in quads[2:] if q[0] == 1)
                self.assertEqual((bottom[1], bottom[2], bottom[4]), (-0.5, 0.5, 1.5))   # the grip pixel's centre is (0, 0, 0)
                self.assertEqual(uv_box(quads[0]), (8, 0, 12, 4))
                self.assertEqual(uv_box(quads[1]), (8, 0, 12, 4))
                self.assertEqual(uv_box(bottom), (8, 2.25, 10, 2.75))
                self.assertEqual(uv_box(next(q for q in quads[2:] if q[0] == 4)), (8.25, 0, 8.75, 3))
                for q in quads:
                    self.assert_quad(q)
                for q in quads[2:]:
                    u0, v0, u1, v1 = uv_box(q)
                    if q[0] in (0, 1):   # a run along x: half a texel across, centred on the row; the run along
                        self.assertAlmostEqual(v1 - v0, 0.5)
                        self.assertAlmostEqual((v0 + v1) / 2 % 1, 0.5)
                        self.assertAlmostEqual(u1 - u0, q[4] - q[1])
                    else:
                        self.assertAlmostEqual(u1 - u0, 0.5)
                        self.assertAlmostEqual((u0 + u1) / 2 % 1, 0.5)
                        self.assertAlmostEqual(v1 - v0, q[5] - q[2])

    def test_pull_frames(self):
        """A bow with three pull frames packs rest, pull_0, pull_1, pull_2 left to right, with a nock for each; a frame of
        another size, a pull that does not rise or leaves 0..1.2, a glow beside it and wrong nocks each exit 1."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            put_bow(root)
            index = self.export(root)
            out = root / ASSETS
            model = json.loads((out / 'models/troop/weapon/bow.json').read_text(encoding='utf-8'))
            with Image.open(out / 'textures/entity/troop/weapon/bow.png') as im:
                tex = im.convert('RGBA')
        self.assertEqual(index['weapons'], ['bow', 'sword'])
        self.assertEqual(model['format'], 3)
        self.assertEqual((tex.size, model['size']), ((32, 9), [32, 9]))
        self.assertEqual([f['name'] for f in model['frames']], ['rest', 'pull_0', 'pull_1', 'pull_2'])
        self.assertEqual(model['pull'], [0.15, 0.32, 0.48])
        self.assertEqual([f['nock'] for f in model['frames']], [[-1, 0, 0], [-1.5, 0, 0], [-2, 0, 0], [-2.5, 0, 0]])
        self.assertEqual((model['thickness'], model['glow']), (2, ''))
        for i, f in enumerate(model['frames']):
            self.assertEqual(tex.getpixel((8 * i + 2, 4)), BOW_COLOURS[i])
            self.assertEqual(tex.getpixel((8 * i + 3, 4))[3], 0)
            self.assertTrue(f['quads'])
            for q in f['quads']:
                self.assert_quad(q)
                u0, _, u1, _ = uv_box(q)
                self.assertTrue(8 * i <= u0 and u1 <= 8 * i + 8, (f['name'], q))   # every quad reads its own column
        cases = {
            'bow_pull_1.png': lambda r: put_bow(r, bow_pull_1=(8, 10)),
            'does not rise': lambda r: put_bow(r, dict(BOW, pull=[0.3, 0.3, 0.5])),
            'between 0 and 1.2': lambda r: put_bow(r, dict(BOW, pull=[0.3, 0.5, 1.3])),
            'bow_e.png': lambda r: (put_bow(r), put_png(r / 'art/weapons/bow_e.png', sprite_image((8, 9), [(2, 3)]))),
            'nocks': lambda r: put_bow(r, dict(BOW, nocks=BOW['nocks'][:3])),
            'lacks nocks': lambda r: put_bow(r, {k: v for k, v in BOW.items() if k != 'nocks'}),
        }
        for want, make in cases.items():
            with self.subTest(want=want), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                make(root)
                said = self.fails(export_assets, root)
                self.assertIn('bow', said)
                self.assertIn(want.replace('lacks nocks', 'nocks'), said)

    def test_weapon_rules(self):
        """Each of these exits 1, naming the weapon and the frame or file."""
        def isolated(n):   # n pixels, none touching another: 4 side quads each
            return [(2 * (i % 8), 2 * (i // 8)) for i in range(n)]

        def sword(**changes):
            return lambda r: write_json(r / 'art/weapons/sword.json', dict(SWORD, **changes))

        def lone_grip(n):
            def make(r):
                write_json(r / 'art/weapons/sword.json', dict(SWORD, grip=dict(SWORD['grip'], pixel=[0, 0])))
                put_png(r / 'art/weapons/sword.png', sprite_image((16, 16), isolated(n)))
            return make

        def wide(r):
            write_json(r / 'art/weapons/sword.json', dict(SWORD, grip=dict(SWORD['grip'], pixel=[3, 5])))
            put_png(r / 'art/weapons/sword.png', sprite_image((49, 10)))
        cases = [
            ('boxes', sword(boxes=[box([0, 0, 0], [1, 1, 1], [0, 0])])),
            ('thickness', sword(thickness=4)),
            ('thickness', sword(thickness=0)),
            ('transparent', lambda r: put_png(r / 'art/weapons/sword.png', sprite_image(
                (16, 16), [(x, y) for x in range(16) for y in range(16) if (x, y) != (3, 10)]))),
            ('outside the frame', sword(grip=dict(SWORD['grip'], pixel=[16, 3]))),
            ('49x10', wide),
            ('130 quads', lone_grip(32)),
        ]
        for want, make in cases:
            with self.subTest(want=want), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                make(root)
                said = self.fails(export_assets, root)
                self.assertIn('sword', said)
                self.assertIn(want, said)
        with tempfile.TemporaryDirectory() as tmp:   # 126 quads are within the 128 a frame may have
            root = fixture(tmp)
            lone_grip(31)(root)
            self.export(root)
            frame = json.loads((root / ASSETS / 'models/troop/weapon/sword.json').read_text(encoding='utf-8'))['frames'][0]
            self.assertEqual(len(frame['quads']), 126)

    # ---- Task 5d: cube weapons from .bbmodel files ----

    def assert_cube_quad(self, q, what=''):
        """A 20-number quad as the client reads it: four corners that make a parallelogram (corner 2 is corner 1 + corner 3 -
        corner 0), a corner cross product that is not zero, and texels of area above 0."""
        self.assertEqual(len(q), 20, what)
        c = [q[0:3], q[3:6], q[6:9], q[9:12]]
        for a in range(3):
            self.assertAlmostEqual(c[2][a], c[1][a] + c[3][a] - c[0][a], places=3, msg=what)
        n = cross(sub(c[1], c[0]), sub(c[3], c[0]))
        self.assertGreater(math.sqrt(dot(n, n)), 1e-6, what)
        t = [q[12:14], q[14:16], q[16:18], q[18:20]]
        self.assertGreater(abs((t[1][0] - t[0][0]) * (t[3][1] - t[0][1]) - (t[1][1] - t[0][1]) * (t[3][0] - t[0][0])), 0, what)

    def test_bbmodel_weapon(self):
        """A weapon with a .bbmodel is built from its cubes: format 3, 20-number quads (6 a cube), the grip from the json, the
        colour sheet with the far version's strip under it, and the _n and _s maps beside it."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp)
            index = self.export(root)
            model = model_of(root)
            tex, normal, spec = (texture_of(root, n) for n in ('pike.png', 'pike_n.png', 'pike_s.png'))
            with Image.open(root / 'art/weapons/pike.png') as im:
                source = im.convert('RGBA')
        self.assertEqual(index['weapons'], ['pike', 'sword'])
        for name in ('pike.png', 'pike_n.png', 'pike_s.png'):
            self.assertIn('textures/entity/troop/weapon/' + name, index['outputs'])
        self.assertEqual((model['format'], model['kind'], model['thickness'], model['pull']), (3, 'cube', 1, []))
        self.assertEqual(model['grip'], {'offset': [0, 0, 0], 'rotation': [0, 0, 0]})
        base = 'gravesandglory:textures/entity/troop/weapon/'
        self.assertEqual((model['texture'], model['normal'], model['specular'], model['glow']),
                         (base + 'pike.png', base + 'pike_n.png', base + 'pike_s.png', ''))
        self.assertEqual([f['name'] for f in model['frames']], ['rest'])
        self.assertIsNone(model['frames'][0]['nock'])
        quads = model['frames'][0]['quads']
        self.assertEqual(len(quads), 18)
        for q in quads:
            self.assert_cube_quad(q)
        # the grip's north face (Blockbench's front) is the weapon frame's south face: y and z flip, its patch is [0, 0, 2, 8]
        self.assertEqual(quads[0], [0.5, 2, 0.5, -0.5, 2, 0.5, -0.5, -2, 0.5, 0.5, -2, 0.5, 0, 8, 2, 8, 2, 0, 0, 0])
        self.assertEqual(model['size'][0], 64)
        self.assertGreater(model['size'][1], 128)
        self.assertEqual(tex.size, tuple(model['size']))
        self.assertEqual((normal.size, spec.size), (tex.size, tex.size))
        self.assertEqual(tex.crop((0, 0, 64, 128)).tobytes(), source.tobytes())
        for q in quads:
            for u, v in zip(q[12::2], q[13::2]):
                self.assertTrue(0 <= u <= 64 and 0 <= v <= 128, q)

    def test_far_weapon(self):
        """The far version is built from the rest frame: a front view as a sprite extruded to at most 32 quads, its big faces
        first, its sprite in the strip under the colour sheet."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp)
            self.export(root)
            model = model_of(root)
        far = model['far']
        self.assertTrue(2 <= len(far) <= build_skins.FAR_CAP, len(far))
        self.assertEqual([q[0] for q in far[:2]], [2, 3])
        strip = model['size'][1] - 128
        self.assertEqual(strip, 13)   # the pike seen from the front is 13 rows tall: the blade, guard and grip
        for q in far:
            self.assert_quad(q)
            self.assertTrue(all(128 <= v <= 128 + strip for v in (q[8], q[10], q[12])), q)   # every texel in the strip
        big = far[0]
        self.assertEqual((big[1], big[4]), (-2.5, 2.5))     # five columns, the guard's width, about the grip's centre
        self.assertEqual((big[2], big[5]), (-10.5, 2.5))    # thirteen rows, the blade's tip at the top
        self.assertEqual(model['frames'][0]['quads'][0][:3], [0.5, 2, 0.5])

    def test_far_version_leaves_the_string_out(self):
        """A string is under a px. At the far version's one px to a model px it would vanish or, where it fills half a px column,
        turn into a plank, by the luck of its alignment, so the far version leaves every cube called string out."""
        laced = PIKE + [('string', (-2.4, -1, -0.1), (-1.9, 2, 0.1), 'linen', None)]   # a lanyard hanging from the guard, half a px wide
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp, wid='plain')
            put_cube_weapon(root, 'laced', frames={'rest': laced})
            self.export(root)
            plain, with_string = model_of(root, 'plain'), model_of(root, 'laced')
            sheets = [texture_of(root, name + '.png') for name in ('plain', 'laced')]
        self.assertEqual(len(with_string['frames'][0]['quads']), len(plain['frames'][0]['quads']) + 6)   # the string is still a cube of the weapon
        lit = [sum(1 for y in range(128, im.height) for x in range(im.width) if im.getpixel((x, y))[3]) for im in sheets]
        self.assertEqual(lit[0], lit[1])
        self.assertEqual(plain['far'], with_string['far'])

    def test_far_version_keeps_the_glow(self):
        """The glow sheet carries the lit texels the front view shows, in the strip under the colour sheet."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp)
            lit_sheet = Image.new('RGBA', SHEET_2X, (0, 0, 0, 0))
            for y in range(0, 8):
                for x in (4, 5):
                    lit_sheet.putpixel((x, y), (255, 240, 200, 255))   # the grip's south face (the front), its whole patch
            lit_sheet.save(root / 'art/weapons/pike_e.png')
            self.export(root)
            model = model_of(root)
            lit_out = texture_of(root, 'pike_e.png')
        self.assertEqual(model['glow'], 'gravesandglory:textures/entity/troop/weapon/pike_e.png')
        self.assertEqual(lit_out.size, tuple(model['size']))
        self.assertEqual(sum(1 for y in range(128) for x in range(64) if lit_out.getpixel((x, y))[3]), 16)
        self.assertGreater(sum(1 for y in range(128, lit_out.height) for x in range(64) if lit_out.getpixel((x, y))[3]), 0)

    def test_tiers_share_the_base_glow(self):
        """A tier model <weapon>_t2 with no glow file of its own is lit by <weapon>_e.png (the heart is the same cube at the
        same sheet spot in every tier); a glow of its own wins."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp)
            put_cube_weapon(root, 'pike_t2')
            put_cube_weapon(root, 'pike_t3')
            lit_sheet = Image.new('RGBA', SHEET_2X, (0, 0, 0, 0))
            lit_sheet.putpixel((0, 0), (255, 240, 200, 255))
            lit_sheet.save(root / 'art/weapons/pike_e.png')
            own = Image.new('RGBA', SHEET_2X, (0, 0, 0, 0))
            own.putpixel((1, 1), (255, 240, 200, 255))
            own.save(root / 'art/weapons/pike_t3_e.png')
            self.export(root)
            one, two, three = (model_of(root, w) for w in ('pike', 'pike_t2', 'pike_t3'))
            lit_two, lit_three = texture_of(root, 'pike_t2_e.png'), texture_of(root, 'pike_t3_e.png')
        self.assertTrue(one['glow'].endswith('/pike_e.png') and two['glow'].endswith('/pike_t2_e.png'))
        self.assertTrue(three['glow'].endswith('/pike_t3_e.png'))
        self.assertEqual(lit_two.getpixel((0, 0))[3], 255)
        self.assertEqual((lit_three.getpixel((0, 0))[3], lit_three.getpixel((1, 1))[3]), (0, 255))

    def test_bbmodel_pull_frames(self):
        """A bow made of frame groups: rest, pull_0, pull_1 and pull_2 (saved hidden), a nock a frame in the weapon frame's px."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp, wid='recurve', frames=BOW_FRAMES, meta=BOW_JSON)
            self.export(root)
            model = model_of(root, 'recurve')
        self.assertEqual([f['name'] for f in model['frames']], ['rest', 'pull_0', 'pull_1', 'pull_2'])
        self.assertEqual(model['pull'], [0.15, 0.32, 0.48])
        self.assertEqual([f['nock'] for f in model['frames']], [[-2.8, 0, 0], [-2.9, 0, 0], [-3, 0, 0], [-3.1, 0, 0]])
        self.assertEqual([len(f['quads']) for f in model['frames']], [36] * 4)
        self.assertEqual(model['size'][0], 64)
        self.assertLessEqual(len(model['far']), build_skins.FAR_CAP)

    def test_bbmodel_weapon_rules(self):
        """Each of these exits 1, naming the weapon and what is wrong."""
        def edit(**changes):
            return lambda data, colour, keys, m: m.update(changes)

        def pixel(data, colour, keys, m):
            m['grip']['pixel'] = [1, 1]

        def set_texture(**changes):
            return lambda data, colour, keys, m: data['textures'][0].update(changes)

        def other_texture(data, colour, keys, m):
            changed = colour.copy()
            changed.putpixel((0, 0), (1, 2, 3, 255))
            data['textures'][0]['source'] = data_url(changed)

        def not_a_png(data, colour, keys, m):
            data['textures'][0]['source'] = 'data:image/jpeg;base64,AAAA'

        def off_sheet(data, colour, keys, m):
            data['elements'][0]['faces']['north']['uv'] = [60, 0, 62.0, 8.0 + 130]

        def stretched(data, colour, keys, m):
            data['elements'][1]['faces']['north']['uv'] = [0, 100, 3, 103]

        column = [('c%d' % i, (0, i, 0), (1, i + 1, 1), 'wood', None) for i in range(22)]
        plate = ('plate', (-1, 2.2, -0.5), (1, 2.8, 0.2), 'gilt', None)
        lifted = [PIKE[0], PIKE[1], ('blade', (-0.5, 4, -0.25), (0.5, 11, 0.25), 'steel', None)]
        loose = bbt.cube('loose', (0, 0, 0), (1, 1, 1))
        without_pull_1 = {k: v for k, v in BOW_FRAMES.items() if k != 'pull_1'}
        cases = [
            ('boxes', 'pike', dict(tweak=edit(boxes=[1]))),
            ('thickness', 'pike', dict(tweak=edit(thickness=4))),
            ('thickness', 'pike', dict(tweak=edit(thickness=0))),
            ('thickness 3', 'pike', dict(tweak=edit(thickness=3))),
            ('grip pixel', 'pike', dict(tweak=pixel)),
            ('rest', 'pike', dict(frames={'stand': PIKE})),
            ('extra', 'pike', dict(frames={'rest': PIKE, 'extra': PIKE})),
            ('pull_1', 'recurve', dict(frames=without_pull_1, meta=BOW_JSON)),
            ('pull_0', 'pike', dict(frames={'rest': PIKE, 'pull_0': PIKE})),
            ('coplanar', 'pike', dict(frames={'rest': PIKE + [plate]})),
            ('air gap', 'pike', dict(frames={'rest': lifted})),
            ('twice', 'pike', dict(tweak=stretched)),
            ('root', 'pike', dict(root_cubes=[loose])),
            ('UV size', 'pike', dict(tweak=set_texture(uv_width=16, uv_height=16))),
            ('embedded', 'pike', dict(tweak=other_texture)),
            ('embedded', 'pike', dict(tweak=not_a_png)),
            ('off the 64x128 sheet', 'pike', dict(tweak=off_sheet)),
            ('132 quads', 'pike', dict(frames={'rest': column})),
        ]
        for want, wid, kw in cases:
            with self.subTest(want=want, kw=sorted(kw)), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                put_cube_weapon(root, wid, **kw)
                said = self.fails(export_assets, root)
                self.assertIn(wid, said)
                self.assertIn(want, said)

    def test_bbmodel_weapon_checks_name_the_cubes(self):
        """The coplanar, air gap and density messages name the frame and the cubes or the face."""
        def stretched(data, colour, keys, m):
            data['elements'][1]['faces']['north']['uv'] = [0, 100, 3, 103]
        plate = ('plate', (-1, 2.2, -0.5), (1, 2.8, 0.2), 'gilt', None)
        lifted = [PIKE[0], PIKE[1], ('blade', (-0.5, 4, -0.25), (0.5, 11, 0.25), 'steel', None)]
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, cube_fixture(tmp, frames={'rest': PIKE + [plate]}))
        for word in ('frame rest', 'guard', 'plate', 'north', 'art/weapons/pike.bbmodel'):
            self.assertIn(word, said)
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, cube_fixture(tmp, frames={'rest': lifted}))
        for word in ('frame rest', 'blade', 'guard', 'one piece'):
            self.assertIn(word, said)
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, cube_fixture(tmp, tweak=stretched))
        for word in ('frame rest', 'guard', 'north', 'twice its size'):
            self.assertIn(word, said)

    def test_string_may_hang_free(self):
        """A string 0.2 px from its limb tips passes (it hangs from its nocks); one 0.3 px off fails."""
        def string_at(gap):
            x = -3.1 - gap
            frames = {k: [c if c[0] != 'string' else ('string', (x - 0.1, -7.5, -0.1), (x + 0.1, 7.5, 0.1), 'linen', None) for c in v]
                      for k, v in BOW_FRAMES.items()}
            return frames, dict(BOW_JSON, nocks=[[x, 0]] * 4)
        frames, meta = string_at(0.2)
        with tempfile.TemporaryDirectory() as tmp:
            self.export(cube_fixture(tmp, wid='recurve', frames=frames, meta=meta))
        frames, meta = string_at(0.3)
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, cube_fixture(tmp, wid='recurve', frames=frames, meta=meta))
        self.assertIn('string', said)
        self.assertIn('recurve', said)

    def test_bbmodel_bow_rules(self):
        """A pull that does not rise or leaves 0..1.2, nocks that are missing, too few or off the string, and a glow beside
        pull each exit 1, naming the weapon."""
        def edit(**changes):
            return lambda data, colour, keys, m: m.update(changes)
        cases = {
            'does not rise': dict(tweak=edit(pull=[0.3, 0.3, 0.5])),
            'between 0 and 1.2': dict(tweak=edit(pull=[0.3, 0.5, 1.3])),
            'nocks must hold': dict(tweak=edit(nocks=BOW_NOCKS[:3])),
            'is not on the string': dict(tweak=edit(nocks=[[5, 0]] + BOW_NOCKS[1:])),
        }
        for want, kw in cases.items():
            with self.subTest(want=want), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                put_cube_weapon(root, 'recurve', frames=BOW_FRAMES, meta=BOW_JSON, **kw)
                said = self.fails(export_assets, root)
                self.assertIn('recurve', said)
                self.assertIn(want, said)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            put_cube_weapon(root, 'recurve', frames=BOW_FRAMES, meta=BOW_JSON)
            lit_sheet = Image.new('RGBA', SHEET_2X, (0, 0, 0, 0))
            lit_sheet.putpixel((0, 0), (255, 240, 200, 255))
            lit_sheet.save(root / 'art/weapons/recurve_e.png')
            said = self.fails(export_assets, root)
        self.assertIn('recurve_e.png', said)
        self.assertIn('pull', said)
        lacking = {k: v for k, v in BOW_JSON.items() if k != 'nocks'}
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, cube_fixture(tmp, wid='recurve', frames=BOW_FRAMES, meta=lacking))
        self.assertIn('nocks', said)

    def test_material_maps(self):
        """The _m key table gives the _s smoothness (R), F0 (G) and porosity (B) of its material, smoothness +12 a tone step
        above the patch's face tone; _n is flat with a one-texel tilt along the patch edge and a height in alpha that is
        never 0; _s alpha is 200 where the _e layer is lit and 255 elsewhere; _s G never falls in 238 to 254 (undefined
        metals)."""
        mid, hi, lo = (100, 100, 100, 255), (140, 140, 140, 255), (60, 60, 60, 255)
        colour = Image.new('RGBA', (8, 8), mid)
        keys = Image.new('RGBA', (8, 8), KEYS['steel'] + (255,))
        colour.putpixel((1, 1), hi)
        colour.putpixel((2, 2), lo)
        colour.putpixel((7, 7), (0, 0, 0, 0))
        keys.putpixel((7, 7), (0, 0, 0, 0))
        keys.putpixel((0, 0), KEYS['gilt'] + (255,))
        lit_sheet = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
        lit_sheet.putpixel((3, 0), (255, 240, 200, 255))
        normal, spec = build_skins.material_maps(colour, keys, [(0, 0, 8, 8)], lit_sheet)
        self.assertEqual(spec.getpixel((4, 4)), (185, 230, 0, 255))      # polished steel at the face tone
        self.assertEqual(spec.getpixel((1, 1)), (197, 230, 0, 255))      # one tone step lighter: +12
        self.assertEqual(spec.getpixel((2, 2)), (173, 230, 0, 255))      # one step darker: -12
        self.assertEqual(spec.getpixel((0, 0)), (200, 231, 0, 255))      # gilt: gold's F0
        self.assertEqual(spec.getpixel((3, 0)), (185, 230, 0, 200))      # lit in _e: emission 200
        self.assertEqual(spec.getpixel((7, 7)), (0, 0, 0, 255))          # no material
        self.assertEqual(normal.getpixel((7, 7)), (128, 128, 255, 255))
        self.assertEqual(normal.getpixel((5, 5)), (128, 128, 255, 128))  # flat inside a patch: height 0.5
        self.assertGreater(normal.getpixel((1, 1))[3], normal.getpixel((5, 5))[3])
        self.assertLess(normal.getpixel((2, 2))[3], normal.getpixel((5, 5))[3])
        self.assertLess(normal.getpixel((0, 5))[0], 128)                 # the left edge tilts out (-x)
        self.assertGreater(normal.getpixel((7, 5))[0], 128)              # the right edge tilts out (+x)
        self.assertLess(normal.getpixel((4, 0))[1], 128)                 # the top edge tilts up the image: DirectX, green down
        self.assertGreater(normal.getpixel((4, 7))[1], 128)
        for x in range(8):
            for y in range(8):
                self.assertGreaterEqual(normal.getpixel((x, y))[3], 1)
                self.assertEqual(normal.getpixel((x, y))[2], 255)
                self.assertFalse(238 <= spec.getpixel((x, y))[1] <= 254)
        for key, (name, smooth, f0, porosity) in build_skins.MATERIALS.items():
            self.assertFalse(238 <= f0 <= 254, name)
            self.assertTrue(0 <= smooth <= 255 and 0 <= porosity <= 255, name)
        self.assertEqual(build_skins.MATERIALS[KEYS['wood']][1:], (70, 10, 40))
        self.assertEqual(build_skins.MATERIALS[KEYS['linen']][1:], (30, 8, 60))
        self.assertEqual(set(KEYS.values()) - set(build_skins.MATERIALS), set())

    def test_map_rules(self):
        """Each of these exits 1, naming the weapon, the file and the texel: an _m of another size, an opaque texel with no key
        colour, a transparent texel with one, a key colour that is not in the table, a lit texel off the colour sheet's
        opaque texels and a missing _m."""
        def keyed(pos, value):
            def make(root):
                im = Image.open(root / 'art/weapons/pike_m.png').convert('RGBA')
                im.putpixel(pos, value)
                im.save(root / 'art/weapons/pike_m.png')
            return make

        def small(root):
            Image.new('RGBA', (32, 32), (0, 0, 0, 0)).save(root / 'art/weapons/pike_m.png')

        def lit_off_sheet(root):
            lit_sheet = Image.new('RGBA', SHEET_2X, (0, 0, 0, 0))
            lit_sheet.putpixel((63, 127), (255, 240, 200, 255))
            lit_sheet.save(root / 'art/weapons/pike_e.png')

        def small_glow(root):
            Image.new('RGBA', (32, 32), (0, 0, 0, 0)).save(root / 'art/weapons/pike_e.png')

        def no_m(root):
            (root / 'art/weapons/pike_m.png').unlink()
        cases = [
            ('pike_m.png is 32x32', 'not the colour sheet', small),
            ('pike_m.png', 'texel (0, 0) is opaque in the colour sheet but has no key colour', keyed((0, 0), (0, 0, 0, 0))),
            ('pike_m.png', 'texel (63, 127) is transparent in the colour sheet but has the key colour', keyed((63, 127), (255, 0, 0, 255))),
            ('pike_m.png', '#010203 at texel (0, 0) is not in the material table', keyed((0, 0), (1, 2, 3, 255))),
            ('pike_e.png', 'texel (63, 127)', lit_off_sheet),
            ('pike_e.png is 32x32', 'not the colour sheet', small_glow),
            ('pike_m.png', 'is missing', no_m),
        ]
        for name, want, make in cases:
            with self.subTest(want=want), tempfile.TemporaryDirectory() as tmp:
                root = cube_fixture(tmp)
                make(root)
                said = self.fails(export_assets, root)
                self.assertIn('pike', said)
                self.assertIn(name, said)
                self.assertIn(want, said)

    def test_sprite_weapon_stays_the_fallback(self):
        """A weapon with only a PNG is still extruded as a sprite, written as format 3 with 13-number quads, no maps and no far
        version, beside a cube weapon in the same catalogue."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp)
            self.export(root)
            sprite = model_of(root, 'sword')
            self.assertFalse((root / ASSETS / 'textures/entity/troop/weapon/sword_n.png').exists())
        self.assertEqual((sprite['format'], sprite['kind'], sprite['far'], sprite['normal'], sprite['specular']), (3, 'sprite', [], '', ''))
        self.assertEqual({len(q) for f in sprite['frames'] for q in f['quads']}, {13})

    def test_verify_cube_weapons(self):
        """--verify rebuilds cube weapons, their maps and far versions byte for byte (PNGs by pixels)."""
        with tempfile.TemporaryDirectory() as tmp:
            root = cube_fixture(tmp)
            self.export(root)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.verify(root), 0)
            for target in ('models/troop/weapon/pike.json', 'textures/entity/troop/weapon/pike_s.png',
                           'textures/entity/troop/weapon/pike_n.png', 'textures/entity/troop/weapon/pike.png'):
                path = root / ASSETS / target
                before = path.read_bytes()
                data = bytearray(before)
                data[len(data) // 2] ^= 0x01
                path.write_bytes(bytes(data))
                said = self.fails(build_skins.verify, root)
                self.assertIn(target, said)
                path.write_bytes(before)

    # ---- Task 5d: the relief layer ----

    def knight_with(self, parts=None, **changes):
        troops = self.with_knight(**changes)
        if parts:
            troops['knight']['parts'] = TROOPS['knight']['parts'] + parts
        return troops

    def mesh(self, model, texels, colour=MINT, key='1'):
        """relief_mesh on a hand-made one-box model: the quads of its only bone."""
        im = Image.new('RGBA', tuple(model['size']), (0, 0, 0, 0))
        for t in texels:
            im.putpixel(tuple(t[:2]), t[2] if len(t) > 2 else colour)
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp))
            return build_skins.relief_mesh(cat, model, im, 'knight', key)

    def test_relief_composite(self):
        marks = [(22, 22, (90, 60, 40, 255)), (23, 22, (90, 60, 40, 255)), (24, 28, (30, 30, 36, 255))]
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            src = relief_png(root, 'knight_s1_r.png', marks)
            self.export(root)
            index = json.loads((root / ASSETS / 'models/troop/index.json').read_text(encoding='utf-8'))
            with Image.open(root / ASSETS / 'textures/entity/troop/knight_s1.png') as im:
                shipped = im.convert('RGBA')
            on_disk = sorted(p.name for p in (root / ASSETS / 'textures/entity/troop').glob('*.png'))
            base = Image.open(root / 'art/troops/knight_s1.png').convert('RGBA')
        want = base.copy()
        for x, y, c in marks:
            want.putpixel((x, y), c)
        self.assertEqual(list(shipped.getdata()), list(want.getdata()))
        self.assertEqual(list(build_skins.composite(base, src).getdata()), list(want.getdata()))
        self.assertEqual(shipped.getpixel((22, 22)), (90, 60, 40, 255))
        self.assertEqual(shipped.getpixel((25, 22)), (200, 196, 186, 255))   # a texel the relief leaves is the skin's own
        self.assertFalse([n for n in on_disk if n.endswith('_r.png')], on_disk)
        self.assertFalse([o for o in index['outputs'] if '_r.png' in o])

    def test_relief_mesh(self):
        """A 2x1 strip on the north face of a 4x4x4 box: one top quad and four side quads, one pixel deep (1.2 px on a box
        scaled 1.2 on z); a quad's UV centre is the texel under it by the Cuboid rule, on a mirrored and a dilated box too."""
        model, box = mini_model()
        quads = self.mesh(model, [(5, 5), (6, 5)])['body']
        self.assertEqual(len(quads), 5)
        top = [q for q in quads if abs(uv_area(q) - 2) < 1e-6]   # the strip's own face covers 2 texels
        self.assertEqual(len(top), 1)
        self.assertEqual((top[0][0], top[0][1:7]), (2, [-1, -1, -3, 1, 0, -3]))
        sides = [q for q in quads if q is not top[0]]
        self.assertEqual(sorted(q[0] for q in sides), [0, 1, 4, 5])
        for q in sides:
            self.assertEqual((q[3], q[6]), (-3, -2))
        for q in quads:
            self.assert_quad(q)
        scaled, _ = mini_model(origin=(-2, -2, -2.0), dilation=(0, 0, 0.4))   # z scaled 1.2: the box spans -2.4 to 2.4
        quads = self.mesh(scaled, [(5, 5), (6, 5)])['body']
        self.assertEqual(len(quads), 5)
        for q in quads:
            self.assertAlmostEqual(q[6] - q[3], 0.0 if q[0] == 2 else 1.2)
            self.assertAlmostEqual(min(q[3], q[6]), -3.6)
        texels = {'top': (5, 1), 'bottom': (9, 1), 'west': (1, 5), 'north': (5, 5), 'east': (9, 5), 'south': (13, 5)}
        for mirror, dilation in ((False, (0, 0, 0)), (True, (0, 0, 0)), (False, (0.2, 0.1, 0.3)), (True, (0.2, 0.1, 0.3))):
            with self.subTest(mirror=mirror, dilation=dilation):
                model, box = mini_model(mirror=mirror, dilation=dilation)
                quads = self.mesh(model, list(texels.values()))['body']
                self.assertEqual(len(quads), 30)
                tops = [q for q in quads if abs(uv_area(q) - 1) < 1e-6]
                self.assertEqual(len(tops), 6)
                centres = []
                for q in tops:
                    self.assert_quad(q)
                    c = uv_corners(q)
                    uv = ((c[0][0] + c[2][0]) / 2, (c[0][1] + c[2][1]) / 2)
                    at = [(q[1 + i] + q[4 + i]) / 2 for i in range(3)]
                    want = vanilla_uv(box, NORMALS[q[0]], at)
                    self.assertAlmostEqual(uv[0], want[0], places=3)
                    self.assertAlmostEqual(uv[1], want[1], places=3)
                    centres.append((round(uv[0], 3), round(uv[1], 3)))
                self.assertEqual(sorted(centres), sorted((x + 0.5, y + 0.5) for x, y in texels.values()))

    def test_relief_corner(self):
        """Raised texels of two faces beside one box edge fill the edge cell, so a band that wraps the edge is one solid:
        no face between the voxels and the filled cell. One texel alone at the edge is a cube with 5 free faces."""
        model, box = mini_model()
        # north face column x = -2..-1 (texel 4) and west face z = -2..-1 (texel 3), both on row y = -1..0 (texel row 5)
        both = self.mesh(model, [(4, 5), (3, 5)])['body']
        # 3 cells (north voxel, west voxel, edge cell), 4 free faces each; no two of them merge
        self.assertEqual(len(both), 12)
        self.assertEqual(sorted(q[0] for q in both), [0, 0, 0, 1, 1, 1, 2, 2, 3, 4, 4, 5])
        self.assertFalse([q for q in both if q[0] in (4, 5) and q[1] == -2], 'a face between the north voxel and the edge cell')
        self.assertFalse([q for q in both if q[0] in (2, 3) and q[3] == -2], 'a face between the west voxel and the edge cell')
        for q in both:
            self.assert_quad(q)
        self.assertEqual(len(self.mesh(model, [(4, 5), (2, 5)])['body']), 10)   # the west texel one in from the edge: two cubes
        self.assertEqual(len(self.mesh(model, [(4, 5)])['body']), 5)
        # a corner of three faces: 3 voxels, 3 edge cells and the corner cell, 3 free faces each
        cluster = self.mesh(model, [(4, 4), (3, 4), (4, 3)])['body']
        self.assertEqual(len(cluster), 21)
        for q in cluster:
            self.assert_quad(q)

    def test_relief_rules(self):
        flap = {'part': 'flap', 'uv': [56, 16]}
        panel = {'part': 'panel'}
        head_spots = [(x, y) for y in (8, 10, 12, 14) for x in (8, 10, 12, 14)]   # head north: 2 apart, no two touching
        faces = [[(x + dx, y + dy) for x, y in head_spots] for dx, dy in ((0, 0), (16, 0), (-8, 0), (8, 0))]   # north, south, west, east
        faces.append([(x, y) for y in (2, 4, 6) for x in (9, 11, 13)])   # the top, not its back row (the pennant) nor its edge columns (a band would wrap)
        many = lambda n: [t for f in faces for t in f[:12]][:n]
        rows = [(x, y) for y in (20, 21, 22) for x in range(20, 28)]

        def glow_at(r):
            im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
            im.putpixel((22, 22), (255, 240, 200, 255))
            im.save(r / 'art/troops/knight_e.png')

        def outer_at(r):
            im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
            im.putpixel((22, 22), (120, 110, 100, 255))
            im.save(r / 'art/troops/knight_o.png')
        cases = [   # name, troops, parts, the relief texels, the colour, an extra setup, what the message names
            ('off every face', None, None, [(63, 63)], MINT, None, ['no skin box face', '(63, 63)']),
            ('no depth', self.knight_with([flap]), RELIEF_PARTS, [(57, 17)], MINT, None, ['no depth', '(57, 17)']),
            ('team hue', None, None, [(22, 22)], (227, 179, 65, 255), None, ['gold side colour', '(22, 22)']),
            ('glow', None, None, [(22, 22)], MINT, glow_at, ['glow', '(22, 22)']),
            ('outer', None, None, [(22, 22)], MINT, outer_at, ['outer', '(22, 22)']),
            ('arm inner face', None, None, [(49, 25)], MINT, None, ['overlaps', '(49, 25)']),
            ('under the chest panel', self.knight_with([panel]), RELIEF_PARTS, [(24, 25)], MINT, None, ['overlaps', '(24, 25)']),
            ('partial alpha', None, None, [(22, 22)], (120, 100, 80, 128), None, ['alpha', '(22, 22)']),
            ('21% of a face', None, None, rows[:21], MINT, None, ['20%']),
            ('257 quads', None, None, many(52), MINT, None, ['256 quads']),
            ('under a part that never hides', self.with_knight(parts=[{'part': 'plain', 'uv': [32, 0]}, {'part': 'pennant'}]),
             RELIEF_PARTS, [(10, 2)], MINT, None, ['overlaps', '(10, 2)']),
        ]
        for name, troops, parts, texels, colour, setup, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp, troops=troops, parts=parts)
                relief_png(root, 'knight_s1_r.png', texels, colour=colour)
                if setup:
                    setup(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                self.assertIn('knight_s1_r.png', said)
                for want in wants:
                    self.assertIn(want, said)
        with tempfile.TemporaryDirectory() as tmp:   # a second own skin without relief
            root = fixture(tmp, troops=self.with_knight(skins={'1': 'knight_s1.png', '2': 'knight_s2.png'}))
            relief_png(root, 'knight_s1_r.png', [(22, 22)])
            said = self.fails(export_assets, root)
            self.assertIn('knight', said)
            self.assertIn('knight_s2_r.png', said)
        with tempfile.TemporaryDirectory() as tmp:   # a relief of the wrong size
            root = fixture(tmp)
            relief_png(root, 'knight_s1_r.png', [(22, 22)], size=(32, 32))
            said = self.fails(export_assets, root)
            self.assertIn('knight_s1_r.png', said)
            self.assertIn('32x32', said)
        passing = [   # 19 of 96 texels, 255 quads, and a texel under a helm part (which can hide)
            ('under 20%', None, None, rows[:19]),
            ('255 quads', None, None, many(51)),
            ('under a helm', {'sentinel': TROOPS['sentinel']}, None, [(10, 2)])]
        for name, troops, parts, texels in passing:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                troop = 'sentinel' if troops else 'knight'
                root = fixture(tmp, troops=troops, parts=parts)
                relief_png(root, troop + '_s1_r.png', texels)
                self.export(root)

    def test_relief_keys(self):
        """Bones with relief carry it keyed by the skin ("1", "2"), only where they have quads; the veteran trim takes none;
        the top-level keys stay Task 5's list and format is 3."""
        troops = self.with_knight(skins={'1': 'knight_s1.png', '2': 'knight_s2.png'})
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, troops=troops)
            relief_png(root, 'knight_veteran.png', [(24, 22), (25, 22)])
            relief_png(root, 'knight_s1_r.png', [(22, 22)])
            relief_png(root, 'knight_s2_r.png', [(22, 22), (23, 22), (10, 10)])   # the head too, at stage 2
            index = self.export(root)
            model = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
        self.assertEqual(model['format'], 3)
        self.assertEqual(tuple(model), build_skins.MODEL_KEYS)
        self.assertFalse(set(model) & set(build_skins.VANILLA_KEYS))
        self.assertEqual(model['veteran'], 'gravesandglory:textures/entity/troop/knight_veteran.png')
        bones = {b['name']: b for b in model['bones']}
        self.assertEqual(list(bones['body']['relief']), ['1', '2'])
        self.assertEqual(list(bones['head']['relief']), ['2'])
        for name, bone in bones.items():
            if name not in ('body', 'head'):
                self.assertNotIn('relief', bone, name)
            for key, quads in bone.get('relief', {}).items():
                self.assertTrue(quads, (name, key))
                for q in quads:
                    self.assert_quad(q)
        self.assertEqual(len(bones['body']['relief']['1']), 5)
        self.assertEqual(index['outputs'], sorted(index['outputs']))

    def test_veteran_trim(self):
        """The veteran trim is an overlay (in-match progression design 4.4): alpha 0 or 255, on texels every skin paints, at
        most 20% of the stage 1 skin's opaque texels, no relief layer. The output keeps its pixels (nothing pasted over it)."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            trim = relief_png(root, 'knight_veteran.png', [(24, 22), (25, 22), (26, 23)])
            self.export(root)
            got = Image.open(root / ASSETS / 'textures/entity/troop/knight_veteran.png').convert('RGBA')
            self.assertEqual(got.tobytes(), trim.tobytes())
        clear = [((x, 63), (0, 0, 0, 0)) for x in range(4)]
        cases = [   # name, the trim texels, a setup, what the message names
            ('partial alpha', [(22, 22, (120, 100, 80, 128))], None, ['alpha', '(22, 22)']),
            ('over 20%', [(x, y) for x in range(64) for y in range(14)], None, ['20%']),
            ('empty', [], None, ['empty']),
            ('on a clear texel', [(1, 63)], lambda r: skin(r / 'art/troops/knight_s1.png', extra=clear), ['clear', '(1, 63)']),
            ('team hue', [(22, 22, (227, 179, 65, 255))], None, ['gold side colour']),
            ('a relief layer', [(22, 22)], lambda r: relief_png(r, 'knight_veteran_r.png', [(22, 22)]), ['knight_veteran_r.png', 'no relief']),
        ]
        for name, texels, setup, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                relief_png(root, 'knight_veteran.png', texels)
                if setup:
                    setup(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)

    def test_veteran_trim_under_relief(self):
        """Task 12 review minor 3: a trim texel under a relief voxel is hidden up close (TroopDepth.near draws the voxel over the
        flat face), so the export names how many lie under each skin's relief, and names none when no texel does."""
        def run(root):
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                export_assets(root)
            return out.getvalue()
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            relief = relief_png(root, 'knight_s1_r.png', [(22, 22), (23, 22)])
            trim = relief_png(root, 'knight_veteran.png', [(22, 22), (24, 22), (25, 22)])
            self.assertEqual(build_skins.trim_under_relief(trim, {'1': relief}), {'1': [(22, 22)]})
            said = run(root)
            self.assertIn('knight: knight_veteran.png: 1 of 3 trim texels lie under the s1 relief', said)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            relief_png(root, 'knight_s1_r.png', [(23, 22)])
            relief_png(root, 'knight_veteran.png', [(22, 22), (24, 22)])
            self.assertNotIn('under the s1 relief', run(root))

    def test_relief_template(self):
        """--template's relief sheet: allowed faces in their hue at 40%, a texel whose voxel would overlap another box
        hatched red, nothing where no skin box lies."""
        with tempfile.TemporaryDirectory() as tmp:
            cat = build_skins.load(fixture(tmp, troops=self.knight_with([{'part': 'panel'}]), parts=RELIEF_PARTS))
            im = build_skins.relief_template(cat, 'knight').convert('RGBA')
        self.assertEqual(im.size, (64, 64))
        allowed = tuple(int(c * 0.4 + 255 * 0.6) for c in build_skins.HUES[0]) + (255,)   # the body, its front
        red = build_skins.RELIEF_RED + (255,)
        self.assertEqual(im.getpixel((22, 21)), allowed)
        self.assertEqual(im.getpixel((63, 63))[3], 0)
        panel = [im.getpixel((x, y)) for x in range(21, 27) for y in range(22, 28)]   # under the chest panel
        self.assertIn(red, panel)
        self.assertTrue(all(p == red or p[3] == 255 for p in panel))
        inner = [im.getpixel((x, y)) for x in range(48, 52) for y in range(20, 32)]   # the right arm's face toward the body
        self.assertGreaterEqual(inner.count(red), 12)
        self.assertNotEqual(inner.count(red), len(inner))
        self.assertNotIn(red, [im.getpixel((x, 21)) for x in range(20, 28)])

    def test_summary_line(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            relief_png(root, 'knight_s1_r.png', [(22, 22)])
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                export_assets(root)
        lines = out.getvalue().splitlines()
        self.assertEqual(lines[0], 'build_skins: 3 troops, 2 rigs, 1 weapons, 6 cloths')
        self.assertEqual(lines[1], 'build_skins: relief 1 stages, 5 quads (most 5, knight 1); weapons 1 frames, 6 quads (most 6, sword rest)')

    def test_turn_sheets(self):
        """--turn: one sheet a troop (a row a pose in shot order, a column an angle, the missing cells grey) and turn-all."""
        with tempfile.TemporaryDirectory() as tmp:
            shots, out = pathlib.Path(tmp) / 'shots', pathlib.Path(tmp) / 'out'
            shots.mkdir()
            for a in (0, 45, 90, 135, 180, 225, 270, 315):
                Image.new('RGB', (1280, 720), (200, 40, 40)).save(shots / ('p2-turn-knight-idle-%d.png' % a))
            Image.new('RGB', (1280, 720), (40, 200, 40)).save(shots / 'p2-turn-knight-walk25-0.png')
            Image.new('RGB', (1280, 720), (40, 40, 200)).save(shots / 'p2-turn-gl_marshal-idle-0.png')
            Image.new('RGB', (1280, 720), (9, 9, 9)).save(shots / 'p2-turn-knight-hand-front.png')   # not a body angle: left out
            with contextlib.redirect_stdout(io.StringIO()):
                made = build_skins.turn_sheets(shots, out)
            self.assertEqual([p.name for p in made], ['turn-gl_marshal.png', 'turn-knight.png', 'turn-all.png'])
            sheets = {p.name: Image.open(p).convert('RGB') for p in made}
        knight = sheets['turn-knight.png']
        self.assertEqual(knight.width, 960)
        cell_w, cell_h = 120, knight.height // 2
        self.assertEqual(knight.height % 2, 0)
        self.assertEqual(knight.getpixel((cell_w // 2, cell_h // 2)), (200, 40, 40))             # idle, 0 degrees
        self.assertEqual(knight.getpixel((7 * cell_w + cell_w // 2, cell_h // 2)), (200, 40, 40))  # idle, 315
        self.assertEqual(knight.getpixel((cell_w // 2, cell_h + cell_h // 2)), (40, 200, 40))    # walk25, 0
        grey = knight.getpixel((cell_w + cell_w // 2, cell_h + cell_h // 2))                     # walk25, 45: no shot
        self.assertEqual(grey[0], grey[1])
        self.assertNotIn(grey, ((200, 40, 40), (40, 200, 40)))
        self.assertGreater(grey[0], 40)
        self.assertEqual(sheets['turn-gl_marshal.png'].width, 960)
        everyone = sheets['turn-all.png']
        self.assertEqual((everyone.width, everyone.height), (960, 2 * (knight.height // 2)))      # one row a troop, two troops
        self.assertEqual(everyone.getpixel((cell_w // 2, cell_h // 2)), (40, 40, 200))            # gl_marshal first (sorted)
        self.assertEqual(everyone.getpixel((cell_w // 2, cell_h + cell_h // 2)), (200, 40, 40))   # then the knight's idle row

    def test_contact_sheet(self):
        """--contact: a 6 x 5 grid, a House a row and a troop a column in the sheet's houses order; a picture is the troop's
        p2-tells-<troop>.png cropped to the central CONTACT_CROP square with the troop's name above it, a troop with no shot a grey picture; each
        row is headed by its House's name; margins and gaps are plain ground (nothing within 12 px of the edge, nothing touching)."""
        def band(colour, side):
            """A 1280x720 shot: colour in the central CONTACT_CROP square, the side's colour outside it, so a crop that is not that square shows it."""
            crop = build_skins.CONTACT_CROP
            im = Image.new('RGB', (1280, 720), side)
            im.paste(Image.new('RGB', (crop, crop), colour), ((1280 - crop) // 2, (720 - crop) // 2))
            return im
        with tempfile.TemporaryDirectory() as tmp:
            shots, out = pathlib.Path(tmp) / 'shots', pathlib.Path(tmp) / 'out' / 'tells-contact.png'
            shots.mkdir()
            band((200, 40, 40), (250, 250, 250)).save(shots / 'p2-tells-knight.png')
            band((40, 200, 40), (250, 250, 250)).save(shots / 'p2-tells-archer.png')
            band((40, 40, 200), (250, 250, 250)).save(shots / 'p2-tells-revenant.png')
            Image.new('RGB', (1280, 720), (9, 9, 9)).save(shots / 'p2-tells-attack-gilded.png')   # a House attack, no troop's tell: left out
            with contextlib.redirect_stdout(io.StringIO()):
                build_skins.contact(shots, out)
            sheet = Image.open(out).convert('RGB')
        cols, rows = 6, 5
        self.assertEqual(sheet.size, build_skins.contact_size(cols, rows))
        ground = build_skins.TURN_BG
        houses = build_skins.contact_houses()
        size = build_skins.CONTACT_CELL

        def picture(col, row, dx, dy):
            left, top, _, _ = build_skins.contact_picture_box(col, row)
            return sheet.getpixel((left + dx, top + dy))

        def lettered(box):
            """Whether the box holds text (near-white pixels on the dark ground)."""
            return any(min(sheet.getpixel((x, y))) > 180 for y in range(box[1], box[3]) for x in range(box[0], box[2]))
        filled = {(0, 0): (200, 40, 40), (1, 0): (40, 200, 40), (0, 1): (40, 40, 200)}   # knight, archer; revenant first of the second row
        for col in range(cols):
            for row in range(rows):
                want = filled.get((col, row), build_skins.CONTACT_GREY)
                self.assertEqual(picture(col, row, size // 2, size // 4), want, 'picture %d,%d upper middle (the "no shot" text is at the centre)' % (col, row))
                self.assertEqual(picture(col, row, 0, 0), want, 'picture %d,%d top left: a square at the centre of the shot' % (col, row))
                self.assertEqual(picture(col, row, size - 1, 0), want, 'picture %d,%d top right' % (col, row))
                self.assertEqual(picture(col, row, size - 1, size - 1), want, 'picture %d,%d bottom right' % (col, row))
                label = build_skins.contact_label_box(col, row)
                self.assertTrue(lettered(label), 'cell %d,%d has no label above its picture' % (col, row))
                self.assertEqual(label[3], build_skins.contact_picture_box(col, row)[1], 'the label sits directly above its picture')
                self.assertEqual(sheet.getpixel((label[2] - 1, label[1])), ground, 'cell %d,%d label strip is ground to its edge' % (col, row))
        self.assertNotIn(build_skins.CONTACT_GREY, list(filled.values()))
        for row in range(rows):
            header = build_skins.contact_header_box(row, cols)
            self.assertTrue(lettered(header), 'row %d has no House name' % row)
            self.assertIn(houses[row]['accent'], {c for _, c in sheet.crop(header).getcolors(maxcolors=1 << 16)}, 'row %d shows its House accent' % row)
        # nothing within 12 px of the edge; nothing touching: the gaps between pictures and between a row and the next House's header are plain ground
        self.assertGreaterEqual(build_skins.CONTACT_MARGIN, 12)
        w, h = sheet.size
        m = 12
        for edge in ((0, 0, w, m), (0, h - m, w, h), (0, 0, m, h), (w - m, 0, w, h)):
            self.assertEqual(sheet.crop(edge).getcolors(), [((edge[2] - edge[0]) * (edge[3] - edge[1]), ground)], 'edge %s is ground' % (edge,))
        for col in range(cols - 1):
            a, b = build_skins.contact_picture_box(col, 0), build_skins.contact_picture_box(col + 1, 0)
            gap = (a[2], a[1], b[0], a[3])
            self.assertGreater(gap[2] - gap[0], 0)
            self.assertEqual(sheet.crop(gap).getcolors(), [((gap[2] - gap[0]) * (gap[3] - gap[1]), ground)], 'gap after column %d' % col)
        for row in range(rows - 1):
            below = (0, build_skins.contact_picture_box(0, row)[3], w, build_skins.contact_header_box(row + 1, cols)[1])
            self.assertGreater(below[3] - below[1], 0)
            self.assertEqual(sheet.crop(below).getcolors(), [((below[2] - below[0]) * (below[3] - below[1]), ground)], 'gap under row %d' % row)

    def test_contact_contrast(self):
        """The text of the sheet reads at 4.5:1 or better (the grading rubric): the "no shot" on its grey picture, the House and troop names on the ground; measured on the
        rendered pixels, not only on the colours chosen."""
        self.assertAlmostEqual(build_skins.contrast((0, 0, 0), (255, 255, 255)), 21.0, places=6)   # the formula
        self.assertAlmostEqual(build_skins.contrast((119, 119, 119), (255, 255, 255)), 4.48, places=2)
        self.assertGreaterEqual(build_skins.contrast(build_skins.CONTACT_NO_SHOT, build_skins.CONTACT_GREY), 4.5)
        self.assertGreaterEqual(build_skins.contrast(build_skins.CONTACT_TEXT, build_skins.TURN_BG), 4.5)
        with tempfile.TemporaryDirectory() as tmp:
            shots, out = pathlib.Path(tmp) / 'shots', pathlib.Path(tmp) / 'out.png'
            shots.mkdir()
            Image.new('RGB', (1280, 720), (200, 40, 40)).save(shots / 'p2-tells-knight.png')
            with contextlib.redirect_stdout(io.StringIO()):
                build_skins.contact(shots, out)
            sheet = Image.open(out).convert('RGB')

        def brightest(box, ground):
            """The pixel of the box that contrasts most with the ground: the text at full strength."""
            return max((c for _, c in sheet.crop(box).getcolors(maxcolors=1 << 16)), key=lambda c: build_skins.contrast(c, ground))
        no_shot = brightest(build_skins.contact_picture_box(1, 0), build_skins.CONTACT_GREY)   # the second picture of the first row has no shot
        self.assertGreaterEqual(build_skins.contrast(no_shot, build_skins.CONTACT_GREY), 4.5, 'no shot text %s' % (no_shot,))
        self.assertNotEqual(no_shot, build_skins.CONTACT_GREY)
        for box in (build_skins.contact_header_box(0, 6), build_skins.contact_label_box(0, 0)):
            text = brightest(box, build_skins.TURN_BG)
            self.assertGreaterEqual(build_skins.contrast(text, build_skins.TURN_BG), 4.5, 'text %s' % (text,))

    def test_contact_order(self):
        """--contact walks sheets/design.json: the five Houses in the houses order, six troops each; a shot of the last troop of the last House lands in the last cell."""
        houses = json.loads((build_skins.ROOT / 'sheets/design.json').read_text(encoding='utf-8'))['houses']
        self.assertEqual(len(houses), 5)
        self.assertEqual([len(h['troops'].split(',')) for h in houses], [6] * 5)
        self.assertEqual(build_skins.contact_grid(), [h['troops'].split(',') for h in houses])
        self.assertEqual([h['name'] for h in build_skins.contact_houses()], [h['name'] for h in houses])
        with tempfile.TemporaryDirectory() as tmp:
            shots, out = pathlib.Path(tmp) / 'shots', pathlib.Path(tmp) / 'out.png'
            shots.mkdir()
            Image.new('RGB', (1280, 720), (200, 40, 40)).save(shots / ('p2-tells-%s.png' % houses[-1]['troops'].split(',')[-1]))
            with contextlib.redirect_stdout(io.StringIO()):
                build_skins.contact(shots, out)
                cli = pathlib.Path(tmp) / 'cli' / 'sheet.png'
                self.assertEqual(build_skins.main(['--contact', str(shots), str(cli)]), 0)   # the command line's way in
            sheet = Image.open(out).convert('RGB')
            self.assertEqual(list(Image.open(cli).convert('RGB').getdata()), list(sheet.getdata()))
        last, before = build_skins.contact_picture_box(5, 4), build_skins.contact_picture_box(4, 4)
        self.assertEqual(sheet.getpixel(((last[0] + last[2]) // 2, last[1] + build_skins.CONTACT_CELL // 4)), (200, 40, 40))
        self.assertEqual(sheet.getpixel(((before[0] + before[2]) // 2, before[1] + build_skins.CONTACT_CELL // 4)), build_skins.CONTACT_GREY)


if __name__ == '__main__':
    unittest.main(verbosity=2)
