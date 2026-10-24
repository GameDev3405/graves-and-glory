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


def upscale(path, k):
    """Nearest-neighbour enlargement of a PNG to k times its size, in place (the fixture's density-k sources)."""
    with Image.open(path) as im:
        big = im.convert('RGBA').resize((im.width * k, im.height * k), Image.NEAREST)
    big.save(path)


def densify(root, troop='knight', density=3, cloth=False):
    """Makes a fixture troop a density-D troop: "density" in its json and every PNG it owns (skins, glow, outer, trim, relief)
    enlarged to D times its size; with cloth=True the team cloth template and the glyph masks too, and art/team/team_cloth.json
    names their density."""
    root = pathlib.Path(root)
    path = root / 'art/troops' / (troop + '.json')
    data = json.loads(path.read_text(encoding='utf-8'))
    data['density'] = density
    write_json(path, data)
    for png in sorted((root / 'art/troops').glob(troop + '_*.png')):
        upscale(png, density)
    if cloth:
        for png in [root / 'art/team/team_cloth.png'] + sorted((root / 'art/team').glob('glyph_*.png')):
            upscale(png, density)
        write_json(root / 'art/team/team_cloth.json', {'density': density})


FACE_SLOTS = [[0, 64], [32, 64], [0, 80]]   # three free 32x16 rectangles under the fixture's 64x64 sheet, which faced() grows to 64x96
HEAD_FACES = [(8, 0, 24, 8), (0, 8, 32, 16)]   # the faces of an 8x8x8 box at uv (0, 0) inside its 32x16 rectangle (build_skins.footprint)


def patch_colour(k, x, y):
    """Texel (x, y) of face patch k: low in saturation (the tint rule objects to nothing here), different per patch and per position."""
    return (180 + 10 * k, 170 + x % 16, 160 + y % 16, 255)


def face_patches(density=1, count=4):
    """art/troops/<troop>_faces.png: `count` patches of 32x16 layout px stacked, the head faces of each painted by patch_colour."""
    im = Image.new('RGBA', (32 * density, 16 * density * count), (0, 0, 0, 0))
    for k in range(count):
        for x0, y0, x1, y1 in HEAD_FACES:
            for y in range(y0 * density, y1 * density):
                for x in range(x0 * density, x1 * density):
                    im.putpixel((x, k * 16 * density + y), patch_colour(k, x, y))
    return im


BEARD_SLOTS = [[32, 80], [46, 80], [32, 85]]   # three free 14x5 rectangles in the 64x96 sheet (the head slots take (0, 64), (32, 64) and (0, 80))
BEARD_FACES = [(1, 0, 13, 1), (0, 1, 14, 5)]   # the six faces of a 6x4x1 box at uv (0, 0) inside its 14x5 rectangle


def beard_colour(k, x, y):
    """Layout px (x, y) of beard patch k: a cut-out (alpha 0) where (x + y + k) is a multiple of 4, else low in saturation."""
    return (0, 0, 0, 0) if (x + y + k) % 4 == 0 else (170 + 10 * k, 160 + x % 16, 150 + y % 16, 255)


def beard_patches(density=1, count=4):
    """art/troops/<troop>_beards.png: `count` patches of 14x5 layout px stacked at `density` texels a px (a px is one block of one colour), the six faces of the beard box painted, with cut-outs."""
    im = Image.new('RGBA', (14 * density, 5 * density * count), (0, 0, 0, 0))
    for k in range(count):
        for x0, y0, x1, y1 in BEARD_FACES:
            for y in range(y0 * density, y1 * density):
                for x in range(x0 * density, x1 * density):
                    im.putpixel((x, k * 5 * density + y), beard_colour(k, x // density, y // density))
    return im


def face_skin(path, density=1, size=(64, 96), slots=FACE_SLOTS, paint=(200, 196, 186, 255), rects=()):
    """A skin of the faces fixture: painted all over except the slots' 32x16 rectangles (free rows for the faces) and any `rects` (x0, y0, x1, y1 in layout px)."""
    im = Image.new('RGBA', (size[0] * density, size[1] * density), paint)
    for x0, y0, x1, y1 in [(u, v, u + 32, v + 16) for u, v in slots] + list(rects):
        for y in range(y0 * density, y1 * density):
            for x in range(x0 * density, x1 * density):
                im.putpixel((x, y), (0, 0, 0, 0))
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    return im


def faces_fixture(tmp, density=1, troops=None, patches=True):
    """The fixture with the knight a faces troop (troops from BuildSkinsChecks.faces_troops): each of its own skins 64x96 at `density`
    (painted but for the slots), and art/troops/knight_faces.png with four distinct patches."""
    root = fixture(tmp, troops=troops)
    for name, paint in (('knight_s1.png', (200, 196, 186, 255)), ('knight_s2.png', (190, 186, 176, 255))):
        if (root / 'art/troops' / name).is_file():
            face_skin(root / 'art/troops' / name, density, paint=paint)
    if patches:
        put_png(root / 'art/troops/knight_faces.png', face_patches(density))
    return root


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
SHEET_3X = (96, 192)
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


def pack_frames(frames, sheet=SHEET_2X, density=2):
    """Gives every face of every cube its own patch at `density` texels per px on a shelf of the sheet, painted in the cube's
    material with its key colour in the material map. Returns ({frame: [cube element, ...]}, colour image, key image)."""
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
                pw, ph = density * w, density * h
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


def put_cube_weapon(root, wid='pike', frames=None, meta=None, tweak=None, sheet=SHEET_2X, root_cubes=(), density=2):
    """art/weapons/<wid>.bbmodel (the texture embedded), .json, .png and _m.png from cube specs; tweak(data, colour, keys, meta)
    may change anything before it is written. Patches are made at `density` texels a px (the json is not told: pass
    meta=dict(PIKE_JSON, density=3) for a weapon that says so). Returns the project dict."""
    root = pathlib.Path(root)
    cubes, colour, keys = pack_frames(frames or {'rest': PIKE}, sheet, density)
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


def mini_model(size=(4, 4, 4), origin=(-2, -2, -2), uv=(0, 0), mirror=False, dilation=(0, 0, 0), sheet=(32, 32), density=1):
    """A model of one bone with one box, as bake gives it (what relief_mesh reads); the sheet is in layout units, and a model
    of density D carries the key as bake writes it and is relieved by a layer D times the sheet."""
    box = {'origin': list(origin), 'size': list(size), 'uv': list(uv), 'mirror': mirror, 'dilation': list(dilation)}
    bone = {'name': 'body', 'parent': '', 'part': '', 'team': False, 'tag': '', 'stages': [], 'sheet': list(sheet),
            'pivot': [0, 0, 0], 'rotation': [0, 0, 0], 'boxes': [box]}
    model = {'size': list(sheet), 'bones': [bone], 'glowFrom': 1}
    if density != 1:
        model['density'] = density
    return model, box


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
        allowed = {'format', 'troop', 'rig', 'size', 'density', 'bones', 'skins', 'veteran', 'glow', 'glowFrom', 'outer', 'weapon',
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
        """A box outside the sheet, a skin of the wrong size and a skin for an armour tier no gear tier has each fail, naming the troop."""
        mage = copy.deepcopy(TROOPS)
        mage['mage']['skins'] = {'1': 'mage_s1.png', '5': 'mage_s5.png'}
        cases = [
            ('knight', 'outside the 64x64 sheet',
             self.with_knight(parts=[{'part': 'cap', 'uv': [50, 0]}, {'part': 'skirt', 'uv': [0, 32]}, {'part': 'pennant'}]), False),
            ('knight', 'knight_s1.png is 32x32, not the troop size 64x64', TROOPS, True),
            ('knight', 'a skin for armour tier 4, which no gear tier has (armour tiers [1, 2, 3])',
             self.with_knight(skins={'1': 'knight_s1.png', '4': 'knight_s4.png'}), False),
            ('mage', 'a skin for armour tier 5, which no gear tier has', mage, False),
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
        k = model.get('density', 1)
        im = Image.new('RGBA', (model['size'][0] * k, model['size'][1] * k), (0, 0, 0, 0))
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

    # ---- the texel-3 trial: a density a troop and a weapon may name ----

    def outputs(self, root):
        """{path under the assets root: bytes} of everything a build wrote."""
        base = pathlib.Path(root) / ASSETS
        return {p.relative_to(base).as_posix(): p.read_bytes() for p in sorted(base.rglob('*')) if p.is_file()}

    def raised(self, texels, k=3):
        """The texels of a density 1 relief as a layer drawn at k paints them: each becomes a k x k block."""
        return [(k * x + i, k * y + j) for x, y in texels for j in range(k) for i in range(k)]

    def test_density_defaults_change_nothing(self):
        """A density key that names the default (1 for a troop, 2 for a weapon) builds the same files as no key at all, and a model
        built with none carries no density key: the 1x and 2x outputs stay byte for byte what they were."""
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            plain = cube_fixture(a)
            named = fixture(b)
            put_cube_weapon(named, meta=dict(PIKE_JSON, density=2))
            data = json.loads((named / 'art/troops/knight.json').read_text(encoding='utf-8'))
            data['density'] = 1
            write_json(named / 'art/troops/knight.json', data)
            self.export(plain)
            self.export(named)
            first, second = self.outputs(plain), self.outputs(named)
        self.assertEqual(sorted(first), sorted(second))
        for name in first:
            self.assertEqual(first[name], second[name], name)
        for name in ('models/troop/knight.json', 'models/troop/weapon/pike.json'):
            self.assertNotIn('density', json.loads(first[name]), name)
        self.assertEqual(tuple(json.loads(first['models/troop/knight.json'])), build_skins.MODEL_KEYS)
        self.assertEqual(build_skins.model_keys(1), build_skins.MODEL_KEYS)
        self.assertFalse([n for n in first if n.endswith('_far.png') and '/weapon/' not in n], 'a density 1 troop has no far twin')

    def test_troop_density_three(self):
        """A troop of density 3 keeps its layout (the model's size, its boxes' uv and sheet are in layout units, which is what the
        client divides by) and ships its PNGs at 3 times it; the model says density 3 right after its size."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root)
            self.export(root)
            base = root / ASSETS
            model = json.loads((base / 'models/troop/knight.json').read_text(encoding='utf-8'))
            sentinel = json.loads((base / 'models/troop/sentinel.json').read_text(encoding='utf-8'))
            with Image.open(base / 'textures/entity/troop/knight_s1.png') as im:
                shipped = im.size
        self.assertEqual(model['size'], [64, 64])
        self.assertEqual(model['density'], 3)
        self.assertEqual(tuple(model), build_skins.model_keys(3))
        self.assertEqual(list(model)[:6], ['format', 'troop', 'rig', 'size', 'density', 'bones'])
        self.assertNotIn('density', sentinel)
        self.assertEqual(shipped, (192, 192))
        body = find(model, 'body')
        self.assertEqual((body['sheet'], body['boxes'][0]['uv'], body['boxes'][0]['size']), ([64, 64], [16, 16], [8, 12, 4]))

    def test_cloth_layout_is_not_the_png_size(self):
        """The cloth template drawn at 3 is 48x48 for a 16x16 layout, and the team bones' sheet stays 16x16: taken from the PNG it
        would be 48, and the client's sheet/size scale of 3 would sample the top-left third of the cloth."""
        rgb = ((GOLD >> 16) & 255, (GOLD >> 8) & 255, GOLD & 255)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root, cloth=True)
            self.export(root)
            base = root / ASSETS
            model = json.loads((base / 'models/troop/knight.json').read_text(encoding='utf-8'))
            with Image.open(base / 'textures/entity/troop/team_cloth_gold.png') as im:
                gold = im.convert('RGBA')
        pennant = find(model, 'pennant')
        self.assertTrue(pennant['team'])
        self.assertEqual(pennant['sheet'], [16, 16])
        self.assertEqual(gold.size, (48, 48))
        self.assertEqual(gold.getpixel((2 * 3 + 1, 2 * 3 + 1))[:3], rgb)   # the x1.0 field, enlarged
        self.assertEqual(gold.getpixel((47, 47))[3], 0)
        with tempfile.TemporaryDirectory() as tmp:   # a cloth at density 1 is as it was
            root = fixture(tmp)
            self.export(root)
            model = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
        self.assertEqual(find(model, 'pennant')['sheet'], [16, 16])

    def test_cloth_layout_checks(self):
        """A team part is checked against the cloth's layout, not its PNG: a box that fits 48x48 but not 16x16 fails; a template the
        density does not divide fails naming team_cloth.json; so does a density that is not 1 to 4."""
        parts = dict(PARTS, pennant=dict(PARTS['pennant'], boxes=[box([-2, -26, 3], [4, 8, 1], [12, 0])]))
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, parts=parts)
            densify(root, cloth=True)
            said = self.fails(export_assets, root)
        self.assertIn('pennant', said)
        self.assertIn('outside the 16x16 sheet', said)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            Image.new('RGBA', (50, 50), (0, 0, 0, 0)).save(root / 'art/team/team_cloth.png')
            write_json(root / 'art/team/team_cloth.json', {'density': 3})
            said = self.fails(export_assets, root)
        for want in ('team_cloth.json', '50x50', 'density 3'):
            self.assertIn(want, said)
        for value in (0, 5, 2.5, True, '3'):
            with self.subTest(value=value), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                write_json(root / 'art/team/team_cloth.json', {'density': value})
                said = self.fails(export_assets, root)
                self.assertIn('team_cloth.json', said)
                self.assertIn('density', said)

    def test_density_key_rules(self):
        """A density is a whole number from 1 to 4; a sprite weapon (one texel a px) takes none."""
        for value in (0, 5, 2.5, True, '3'):
            with self.subTest(troop=value), tempfile.TemporaryDirectory() as tmp:
                said = self.fails(export_assets, fixture(tmp, troops=self.with_knight(density=value)))
                self.assertIn('knight', said)
                self.assertIn('density', said)
        for value in (0, 5, 2.5, False):
            with self.subTest(weapon=value), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                put_cube_weapon(root, meta=dict(PIKE_JSON, density=value))
                said = self.fails(export_assets, root)
                self.assertIn('pike', said)
                self.assertIn('density', said)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            write_json(root / 'art/weapons/sword.json', dict(SWORD, density=1))
            said = self.fails(export_assets, root)
        self.assertIn('sword', said)
        self.assertIn('sprite', said)

    def test_wrong_size_names_the_density(self):
        """A skin that is the layout size on a density 3 troop fails saying what the troop's size is at 3 texels a px."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root)
            skin(root / 'art/troops/knight_s1.png', size=(64, 64))
            said = self.fails(export_assets, root)
        self.assertIn('knight_s1.png is 64x64, not the troop size 64x64 at 3 texels a px (192x192)', said)

    def test_relief_density_three_is_the_same_shape(self):
        """A relief layer drawn at 3 whose blocks are the 1x layer's texels (the nearest-upscaled placeholder) gives the quads of
        the 1x layer: the same directions and corners and, for a top quad, the same UV rectangle, on a plain, a mirrored and a
        dilated box, for a strip, a wrapped edge, a three-face corner and one texel on each face."""
        every = [(5, 1), (9, 1), (1, 5), (5, 5), (9, 5), (13, 5)]
        patterns = {'strip': [(5, 5), (6, 5)], 'edge': [(4, 5), (3, 5)], 'corner': [(4, 4), (3, 4), (4, 3)], 'every face': every}

        def is_top(q):
            x0, y0, x1, y1 = uv_box(q)
            return min(x1 - x0, y1 - y0) >= 0.99

        def tops(quads):
            return sorted(tuple(round(v, 3) for v in uv_box(q)) for q in quads if is_top(q))
        for mirror, dilation in ((False, (0, 0, 0)), (True, (0, 0, 0)), (False, (0.2, 0.1, 0.3)), (True, (0.2, 0.1, 0.3))):
            for name, texels in patterns.items():
                with self.subTest(name=name, mirror=mirror, dilation=dilation):
                    one, _ = mini_model(mirror=mirror, dilation=dilation)
                    three, _ = mini_model(mirror=mirror, dilation=dilation, density=3)
                    a = self.mesh(one, texels)['body']
                    b = self.mesh(three, self.raised(texels))['body']
                    self.assertEqual(len(a), len(b))
                    self.assertEqual(sorted([round(v, 3) for v in q[:7]] for q in a), sorted([round(v, 3) for v in q[:7]] for q in b))
                    self.assertEqual(tops(a), tops(b))
                    self.assertTrue(tops(b))
                    for q in b:
                        self.assert_quad(q)
                    self.assert_side_uvs_in_source_texel(a, b)

    def assert_side_uvs_in_source_texel(self, ones, threes):
        """Every side and corner quad of a density 3 mesh reads the source texel of its 1x twin: its UV box lies inside that texel's
        cell (layout units), is as long along its run as the 1x quad and a third as wide across (2 x SLIVER over the density),
        so a side quad whose UV is not divided by the density (or divided twice) leaves the cell and fails."""
        def key(q):
            return tuple(round(v, 3) for v in q[:7])
        by_key = {}
        for q in ones:
            by_key.setdefault(key(q), []).append(q)
        sides = 0
        for q in threes:
            x0, y0, x1, y1 = uv_box(q)
            if min(x1 - x0, y1 - y0) >= 0.99:
                continue   # a top quad, pinned by its UV rectangle
            sides += 1
            fits = []
            for a in by_key[key(q)]:
                u0, v0, u1, v1 = uv_box(a)
                cell = (math.floor(u0 + 1e-6), math.floor(v0 + 1e-6), math.ceil(u1 - 1e-6), math.ceil(v1 - 1e-6))
                inside = x0 >= cell[0] - 1e-6 and y0 >= cell[1] - 1e-6 and x1 <= cell[2] + 1e-6 and y1 <= cell[3] + 1e-6
                extents = sorted((x1 - x0, y1 - y0))
                want = sorted((u1 - u0 if u1 - u0 > 0.6 or u1 - u0 < 0.1 else 2 * build_skins.SLIVER / 3,
                               v1 - v0 if v1 - v0 > 0.6 or v1 - v0 < 0.1 else 2 * build_skins.SLIVER / 3))
                fits.append(inside and all(abs(g - w) < 1e-4 for g, w in zip(extents, want)))
            self.assertTrue(any(fits), 'a side quad %s reads outside its source texel: UV box %s' % (q, (x0, y0, x1, y1)))
        self.assertGreater(sides, 0)

    def test_relief_density_three_fine_detail(self):
        """One raised texel at 3 is a third of a px across and a whole px proud: the voxel's depth along the normal is the box's
        px (1.2 on a box scaled 1.2), not the texel's. Its top quad is the texel itself in layout units, in thirds of four decimals."""
        model, box = mini_model(density=3)
        quads = self.mesh(model, [(15, 15)])['body']   # the north face's texels run 12 to 23 across and down
        self.assertEqual(len(quads), 5)
        tops = [q for q in quads if q[0] == 2]
        self.assertEqual(len(tops), 1)
        top = tops[0]
        self.assertEqual(top[1:7], [-1, -1, -3, -0.6667, -0.6667, -3])
        for got, want in zip(uv_box(top), (5, 5, 5.3333, 5.3333)):
            self.assertAlmostEqual(got, want, places=4)
        c = uv_corners(top)
        centre = ((c[0][0] + c[2][0]) / 2, (c[0][1] + c[2][1]) / 2)
        at = [(top[1 + i] + top[4 + i]) / 2 for i in range(3)]
        want = vanilla_uv(box, NORMALS[top[0]], at)
        self.assertAlmostEqual(centre[0], 5.1667, places=3)
        self.assertAlmostEqual(centre[1], 5.1667, places=3)
        self.assertAlmostEqual(centre[0], want[0], places=3)
        self.assertAlmostEqual(centre[1], want[1], places=3)
        centre_uv = 15.5 / 3
        across = {0: 0, 1: 0}   # which texture axis a side quad's sliver runs over
        for q in quads:
            self.assert_quad(q)
            if q is not top:
                self.assertEqual((q[3], q[6]), (-3, -2))
                # a side quad is the texel's centre band: its UV box is centred on the texel (5.1667, 5.1667), a third of a layout px
                # long along its run and 2 x SLIVER / 3 across, so its corners sit at the centre plus or minus SLIVER / 3
                x0, y0, x1, y1 = uv_box(q)
                self.assertAlmostEqual((x0 + x1) / 2, centre_uv, places=4)
                self.assertAlmostEqual((y0 + y1) / 2, centre_uv, places=4)
                narrow = [i for i, w in enumerate((x1 - x0, y1 - y0)) if abs(w - 2 * build_skins.SLIVER / 3) < 1e-4]
                self.assertEqual(len(narrow), 1, 'one UV axis is the sliver')
                across[narrow[0]] += 1
                long_axis = (x1 - x0, y1 - y0)[1 - narrow[0]]
                self.assertAlmostEqual(long_axis, 1 / 3, places=4)
                bounds = (x0, x1) if narrow[0] == 0 else (y0, y1)
                self.assertAlmostEqual(bounds[0], centre_uv - build_skins.SLIVER / 3, places=4)
                self.assertAlmostEqual(bounds[1], centre_uv + build_skins.SLIVER / 3, places=4)
        self.assertEqual(across, {0: 2, 1: 2})   # two side quads slivered over u, two over v
        scaled, _ = mini_model(origin=(-2, -2, -2.0), dilation=(0, 0, 0.4), density=3)   # z scaled 1.2
        for q in self.mesh(scaled, [(15, 15)])['body']:
            self.assertAlmostEqual(q[6] - q[3], 0.0 if q[0] == 2 else 1.2)

    def test_relief_density_rules(self):
        """The relief rules count in texels of the troop's density: the layer must be D times the layout, a texel off every face is
        named in layer texels, and a face may have 20% of its texels raised (19 of 96 at 1x are 171 of 864 at 3)."""
        rows = [(x, y) for y in (20, 21, 22) for x in range(20, 28)]
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root)
            relief_png(root, 'knight_s1_r.png', self.raised(rows[:19]), size=(192, 192))
            self.export(root)
        cases = [('21 of 96', self.raised(rows[:21]), (192, 192), ['20%']),
                 ('off every face', [(189, 189)], (192, 192), ['no skin box face', '(189, 189)']),
                 ('a layer at 1x', [(22, 22)], (64, 64), ['knight_s1_r.png is 64x64', '192x192'])]
        for name, texels, size, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                densify(root)
                relief_png(root, 'knight_s1_r.png', texels, size=size)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)

    def test_relief_density_three_in_a_troop(self):
        """A density 3 troop with a nearest-upscaled relief gives the model the quads of the 1x troop, in layout units."""
        marks = [(22, 22), (23, 22), (24, 28)]
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            relief_png(root, 'knight_s1_r.png', marks)
            self.export(root)
            one = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            relief_png(root, 'knight_s1_r.png', marks)
            densify(root)
            self.export(root)
            three = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
        a, b = find(one, 'body')['relief']['1'], find(three, 'body')['relief']['1']
        self.assertEqual(len(a), len(b))
        self.assertEqual(sorted(q[:7] for q in a), sorted(q[:7] for q in b))

    def test_templates_at_density(self):
        """--template draws the sheets at the troop's density: the flat template and the relief template are 192x192 for a 64x64
        layout and are the 1x template enlarged, texel for texel."""
        with tempfile.TemporaryDirectory() as tmp:
            troops = self.knight_with([{'part': 'panel'}])
            cat = build_skins.load(fixture(tmp, troops=troops, parts=RELIEF_PARTS))
            flat_one, relief_one = build_skins.template(cat, 'knight').convert('RGBA'), build_skins.relief_template(cat, 'knight').convert('RGBA')
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, troops=troops, parts=RELIEF_PARTS)
            densify(root)
            cat = build_skins.load(root)
            flat, relief = build_skins.template(cat, 'knight').convert('RGBA'), build_skins.relief_template(cat, 'knight').convert('RGBA')
        self.assertEqual((flat.size, relief.size), ((192, 192), (192, 192)))
        red = build_skins.RELIEF_RED + (255,)
        self.assertEqual(flat.tobytes(), flat_one.resize((192, 192), Image.NEAREST).tobytes())
        self.assertEqual(relief.tobytes(), relief_one.resize((192, 192), Image.NEAREST).tobytes())
        self.assertIn(red, [relief.getpixel((x, y)) for x in range(21 * 3, 27 * 3) for y in range(22 * 3, 28 * 3)])

    def test_glow_cap_counts_px(self):
        """The glow cap counts a troop's lit texels at its density (54 lit texels at 3 a px are 6 px, the common cap; 63 are 7). A
        cube weapon's lit texels keep the unit they always had: at its default 2 a px every texel counts 1, as before the texel-3
        work (a 2x weapon's cap is no looser than it was), and at 3 a px a texel counts 1 / 2.25, the same lit area for the same count."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root)
            glow(root / 'art/troops/knight_e.png', 54, size=(192, 192))
            self.export(root)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root)
            glow(root / 'art/troops/knight_e.png', 63, size=(192, 192))
            said = self.fails(export_assets, root)
        for want in ('knight', 'glow', '7 lit', 'cap of 6'):
            self.assertIn(want, said)

        def lit_pike(root, knight_lit, pike_lit, sheet=SHEET_2X):
            glow(root / 'art/troops/knight_e.png', knight_lit)
            colour = Image.open(root / 'art/weapons/pike.png').convert('RGBA')
            painted = [(x, y) for y in range(colour.height) for x in range(colour.width) if colour.getpixel((x, y))[3]]
            lit_sheet = Image.new('RGBA', sheet, (0, 0, 0, 0))
            for xy in painted[:pike_lit]:
                lit_sheet.putpixel(xy, (255, 240, 200, 255))
            lit_sheet.save(root / 'art/weapons/pike_e.png')

        def pike_build(knight_lit, pike_lit, density=2):
            with tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp, troops=self.with_knight(weapon='pike'))
                if density == 2:
                    put_cube_weapon(root)
                else:
                    put_cube_weapon(root, meta=dict(PIKE_JSON, density=density), sheet=SHEET_3X, density=density)
                lit_pike(root, knight_lit, pike_lit, SHEET_2X if density == 2 else SHEET_3X)
                return self.fails(export_assets, root) if knight_lit + pike_lit * (4 / density ** 2) > 6 else self.export(root)
        pike_build(0, 6)                  # a 2x weapon's texels count 1 each, as before: 6 is the common cap
        said = pike_build(1, 6)           # 1 + 6 = 7
        self.assertIn('knight', said)
        self.assertIn('7 lit', said)
        said = pike_build(0, 16)          # 16 lit texels at 2 a px are 16, not the 4 px of area they cover: the cap of an old weapon is not loosened
        self.assertIn('16 lit', said)
        self.assertIn('cap of 6', said)
        self.assertIn('13 lit', pike_build(0, 13))   # the same 13 texels fail at 2 a px ...
        pike_build(0, 13, density=3)      # ... and pass at 3 a px: 13 / 2.25 = 5.78
        said = pike_build(0, 14, density=3)   # 14 / 2.25 = 6.22
        self.assertIn('6.22 lit', said)

    def test_weapon_density_three(self):
        """A weapon that names density 3 has its patches at 3 texels a px on a 96x192 sheet: the model says so right after its size,
        the quads read texels of that sheet, and the far version stays one texel to a px in the strip under it."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            put_cube_weapon(root, meta=dict(PIKE_JSON, density=3), sheet=SHEET_3X, density=3)
            self.export(root)
            model = model_of(root)
            tex, normal, spec = (texture_of(root, n) for n in ('pike.png', 'pike_n.png', 'pike_s.png'))
        self.assertEqual(model['density'], 3)
        self.assertEqual(list(model)[:6], ['format', 'weapon', 'kind', 'size', 'density', 'thickness'])
        self.assertEqual(model['size'][0], 96)
        self.assertEqual(model['size'][1] - 192, 13)   # the same 13 rows of far version, one texel to a px, under the bigger sheet
        self.assertEqual(tex.size, tuple(model['size']))
        self.assertEqual((normal.size, spec.size), (tex.size, tex.size))
        quads = model['frames'][0]['quads']
        self.assertEqual(quads[0], [0.5, 2, 0.5, -0.5, 2, 0.5, -0.5, -2, 0.5, 0.5, -2, 0.5, 0, 12, 3, 12, 3, 0, 0, 0])
        for q in quads:
            for u, v in zip(q[12::2], q[13::2]):
                self.assertTrue(0 <= u <= 96 and 0 <= v <= 192, q)
        for q in model['far']:
            self.assertTrue(all(192 <= v <= 192 + 13 for v in (q[8], q[10], q[12])), q)
        self.assertLessEqual(len(model['far']), build_skins.FAR_CAP)

    def test_weapon_density_mismatch(self):
        """A weapon whose patches are not at the density its json names (or the default 2) fails, saying how many times its size a
        face should be."""
        cases = [('three times its size', dict(meta=dict(PIKE_JSON, density=3))),   # patches at 2, the json says 3
                 ('twice its size', dict(sheet=SHEET_3X, density=3))]               # patches at 3, the json is silent
        for want, kw in cases:
            with self.subTest(want=want), tempfile.TemporaryDirectory() as tmp:
                root = fixture(tmp)
                put_cube_weapon(root, **kw)
                said = self.fails(export_assets, root)
                for word in ('pike', 'texels a px', want):
                    self.assertIn(word, said)

    def test_material_maps_tilt_follows_the_density(self):
        """A patch edge tilts the _n map by the drop to the next texel, and a texel is 1/D px: EDGE_TILT is scaled by 2/D, so a 3x
        weapon's bevel leans as a 2x weapon's does."""
        colour = Image.new('RGBA', (5, 1), (120, 120, 120, 255))
        keys = Image.new('RGBA', (5, 1), KEYS['steel'] + (255,))

        def left_edge(tilt):
            nx = -0.5 * tilt
            return int(round(128 + nx / math.sqrt(nx * nx + 1.0) * 127))
        for d, tilt in ((2, build_skins.EDGE_TILT), (3, build_skins.EDGE_TILT * 2 / 3), (4, build_skins.EDGE_TILT / 2)):
            normal, _ = build_skins.material_maps(colour, keys, [(0, 0, 5, 1)], None, d)
            self.assertEqual(normal.getpixel((0, 0))[0], left_edge(tilt), d)
        normal, _ = build_skins.material_maps(colour, keys, [(0, 0, 5, 1)])   # the default is the weapons' 2
        self.assertEqual(normal.getpixel((0, 0))[0], left_edge(build_skins.EDGE_TILT))

    def test_far_skin_twin(self):
        """A density 3 troop ships a twin of each skin at one texel to a px (<name>_far.png), made as the weapons' far version is:
        a block of 3x3 texels takes its most common colour and is clear unless at least half of it is painted. The trim, the glow
        and the outer layer get twins too; a density 1 troop gets none."""
        paint, other = (90, 60, 40, 255), (30, 30, 36, 255)
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root)
            src = root / 'art/troops/knight_s1.png'
            with Image.open(src) as im:
                skin_im = im.convert('RGBA')
            for i in range(9):   # block (5, 5): six texels of paint and three of another colour
                skin_im.putpixel((15 + i % 3, 15 + i // 3), paint if i < 6 else other)
            for i in range(9):   # block (7, 7): four painted, five clear: clear
                skin_im.putpixel((21 + i % 3, 21 + i // 3), paint if i < 4 else (0, 0, 0, 0))
            for i in range(9):   # block (8, 8): five painted, four clear: painted
                skin_im.putpixel((24 + i % 3, 24 + i // 3), paint if i < 5 else (0, 0, 0, 0))
            skin_im.save(src)
            relief_png(root, 'knight_veteran.png', self.raised([(10, 10)]), size=(192, 192))
            lit_im = Image.new('RGBA', (192, 192), (0, 0, 0, 0))
            for i in range(5):
                lit_im.putpixel((6 + i % 3, 6 + i // 3), (255, 240, 200, 255))   # block (2, 2): five lit
            lit_im.save(root / 'art/troops/knight_e.png')
            skin(root / 'art/troops/knight_o.png', size=(192, 192), paint=(0, 0, 0, 0),
                 extra=[((60 + i % 3, 60 + i // 3), (120, 110, 100, 255)) for i in range(9)])
            index = self.export(root)
            base = root / ASSETS / 'textures/entity/troop'
            names = ('knight_s1_far.png', 'knight_veteran_far.png', 'knight_e_far.png', 'knight_o_far.png')
            twins = {n: Image.open(base / n).convert('RGBA') for n in names}
            on_disk = sorted(p.name for p in base.glob('*_far.png'))
        self.assertEqual(on_disk, sorted(twins))
        for name in twins:
            self.assertIn('textures/entity/troop/' + name, index['outputs'])
            self.assertEqual(twins[name].size, (64, 64), name)
        far = twins['knight_s1_far.png']
        self.assertEqual(far.getpixel((5, 5)), paint)
        self.assertEqual(far.getpixel((7, 7))[3], 0)
        self.assertEqual(far.getpixel((8, 8)), paint)
        self.assertEqual(far.getpixel((0, 0)), (200, 196, 186, 255))
        self.assertEqual(twins['knight_veteran_far.png'].getpixel((10, 10)), MINT)
        self.assertEqual(twins['knight_veteran_far.png'].getpixel((11, 10))[3], 0)
        self.assertEqual(twins['knight_e_far.png'].getpixel((2, 2)), (255, 240, 200, 255))
        self.assertEqual(twins['knight_o_far.png'].getpixel((20, 20)), (120, 110, 100, 255))

    def test_far_twin_function(self):
        """far_twin on its own: density 1 is the image itself; a tie goes to the colour met first in reading order; a block with
        fewer than half its texels painted is clear."""
        im = Image.new('RGBA', (6, 3), (0, 0, 0, 0))
        a, b = (10, 20, 30, 255), (40, 50, 60, 255)
        for i in range(9):   # a 3x3 block of four a, four b and one clear: a was met first
            im.putpixel((i % 3, i // 3), a if i in (0, 1, 3, 4) else b if i in (2, 5, 6, 7) else (0, 0, 0, 0))
        for x in range(3, 6):
            for y in range(3):
                im.putpixel((x, y), b)
        twin = build_skins.far_twin(im, 3)
        self.assertEqual(twin.size, (2, 1))
        self.assertEqual((twin.getpixel((0, 0)), twin.getpixel((1, 0))), (a, b))
        self.assertIs(build_skins.far_twin(im, 1), im)
        sparse = Image.new('RGBA', (3, 3), (0, 0, 0, 0))
        for i in range(4):
            sparse.putpixel((i % 3, i // 3), a)
        self.assertEqual(build_skins.far_twin(sparse, 3).getpixel((0, 0))[3], 0)

    def test_far_twin_even_density(self):
        """The half rule lands exactly on a texel count at an even density (the weapons' far version keeps a px at half its samples
        too): at 2 a block with 2 of its 4 texels painted is kept and one with 1 is clear; a 2-2 colour tie, and a 1-1 tie, go to the
        colour met first in reading order; at 4, 8 of 16 are kept and 7 are clear."""
        a, b, clear = (10, 20, 30, 255), (40, 50, 60, 255), (0, 0, 0, 0)
        # each block lists its texels in reading order: (0,0) (1,0) (0,1) (1,1)
        blocks = [('two of four, one colour', [a, clear, clear, a], a), ('one of four', [clear, clear, a, clear], clear),
                  ('three of four, two against one', [b, a, b, clear], b), ('a 2-2 tie, a met first', [a, b, b, a], a),
                  ('a 2-2 tie, b met first', [b, a, a, b], b), ('a 1-1 tie, kept', [clear, b, a, clear], b),
                  ('none', [clear] * 4, clear)]
        im = Image.new('RGBA', (2 * len(blocks), 2), clear)
        for n, (_, texels, _) in enumerate(blocks):
            for i, c in enumerate(texels):
                im.putpixel((2 * n + i % 2, i // 2), c)
        twin = build_skins.far_twin(im, 2)
        self.assertEqual(twin.size, (len(blocks), 1))
        for n, (name, _, want) in enumerate(blocks):
            with self.subTest(name=name):
                self.assertEqual(twin.getpixel((n, 0)), want)
        for painted, kept in ((8, True), (7, False)):
            with self.subTest(painted=painted, density=4):
                big = Image.new('RGBA', (4, 4), clear)
                for i in range(painted):
                    big.putpixel((i % 4, i // 4), a)
                self.assertEqual(build_skins.far_twin(big, 4).getpixel((0, 0)), a if kept else clear)

    def test_verify_density_three(self):
        """--verify rebuilds a density 3 troop, its far twins and the cloth at 3 (PNGs by pixels, the model byte for byte)."""
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            densify(root, cloth=True)
            self.export(root)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.verify(root), 0)
            for target in ('models/troop/knight.json', 'textures/entity/troop/knight_s1_far.png', 'textures/entity/troop/knight_s1.png',
                           'textures/entity/troop/team_cloth_gold.png'):
                path = root / ASSETS / target
                before = path.read_bytes()
                data = bytearray(before)
                data[len(data) // 2] ^= 0x01
                path.write_bytes(bytes(data))
                said = self.fails(build_skins.verify, root)
                self.assertIn(target, said)
                path.write_bytes(before)

    # ---- the troop faces (face variants: head boxes in the sheet, picked by the client from the entity's UUID) ----

    def faces_troops(self, density=None, fdensity=None, **faces):
        """TROOPS with the knight a four-faces troop: a 64x96 sheet (the fixture's 64x64 and free rows under it) and a faces block
        (`fdensity` is the block's own "density": the texels a px the patch files are drawn at)."""
        block = {'count': 4, 'slots': FACE_SLOTS}
        if fdensity is not None:
            block['density'] = fdensity
        block.update(faces)
        extra = {'size': [64, 96], 'faces': block}
        if density:
            extra['density'] = density
        return self.with_knight(**extra)

    def bake_fails(self, troops, setup=None):
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, troops=troops)
            if setup:
                setup(root)
            cat = build_skins.load(root)
            return self.fails(build_skins.bake, cat, 'knight')

    def test_faces_model(self):
        """A troop with faces keeps `head` as the pivot (the clips, helm bones and helm_off hang from it) and gets one face bone a
        variant under it, in order, straight after it: face_v0 is the rig's head box exactly where it was, face_v1 to 3 the same box
        with its uv at a slot. Each says which variant shows it; the model says the count and the card face. A troop without faces
        is what it was: no faces key, no variants."""
        model = self.bake(self.faces_troops())['knight']
        names = [b['name'] for b in model['bones']]
        at = names.index('head')
        self.assertEqual(names[at + 1:at + 5], ['face_v0', 'face_v1', 'face_v2', 'face_v3'])
        self.assertEqual(find(model, 'head')['boxes'], [])
        uvs = [[0, 0]] + FACE_SLOTS
        for k in range(4):
            b = find(model, 'face_v%d' % k)
            self.assertEqual(list(b), ['name', 'parent', 'part', 'team', 'tag', 'stages', 'variants', 'sheet', 'pivot', 'rotation', 'boxes'])
            self.assertEqual((b['parent'], b['part'], b['team'], b['tag'], b['stages'], b['variants']), ('head', '', False, '', [], [k]))
            self.assertEqual((b['sheet'], b['pivot'], b['rotation']), ([64, 96], [0, 0, 0], [0, 0, 0]))
            self.assertEqual(b['boxes'], [{'origin': [-4, -8, -4], 'size': [8, 8, 8], 'uv': uvs[k], 'mirror': False, 'dilation': [0, 0, 0]}])
        self.assertEqual(model['faces'], {'count': 4, 'card': 0})
        self.assertEqual(tuple(model), build_skins.model_keys(1, True))
        self.assertEqual(list(model)[-2:], ['portrait', 'faces'])
        plain = self.bake(TROOPS)['knight']
        self.assertEqual(tuple(plain), build_skins.MODEL_KEYS)
        self.assertFalse([b['name'] for b in plain['bones'] if 'variants' in b])
        self.assertEqual([b['name'] for b in plain['bones']], [n for n in names if not n.startswith('face_v')])
        dense = self.bake(self.faces_troops(density=3))['knight']
        self.assertEqual(tuple(dense), build_skins.model_keys(3, True))
        self.assertEqual(find(dense, 'face_v1')['boxes'][0]['uv'], FACE_SLOTS[0])   # slots stay in layout units

    def test_faces_head_boxes(self):
        """The first box of `head` becomes the faces; any other box of it (the mage's and the Marshal's beard volumes) stays on `head`
        and shows in every variant. A bone scale reaches the face boxes as it reached the head box."""
        beard = box([-3, -3, -4.9], [6, 4, 1], [48, 56])
        main = box([-4, -8, -4], [8, 8, 8], [0, 0])
        troops = self.faces_troops()
        troops['knight']['bones'] = {'head': {'boxes': [main, beard]}}
        model = self.bake(troops)['knight']
        head = find(model, 'head')
        self.assertEqual(len(head['boxes']), 1)
        self.assertEqual((head['boxes'][0]['size'], head['boxes'][0]['uv']), ([6, 4, 1], [48, 56]))
        for k in range(4):
            self.assertEqual(len(find(model, 'face_v%d' % k)['boxes']), 1)
        scaled = self.faces_troops()
        scaled['knight']['bones'] = {'head': {'scale': 1.25}}
        plain = copy.deepcopy(TROOPS)
        plain['knight'].update(size=[64, 96], bones={'head': {'scale': 1.25}})
        flat = find(self.bake(plain)['knight'], 'head')['boxes'][0]
        baked = self.bake(scaled)['knight']
        for k in range(4):
            got = find(baked, 'face_v%d' % k)['boxes'][0]
            self.assertEqual({key: v for key, v in got.items() if key != 'uv'}, {key: v for key, v in flat.items() if key != 'uv'})

    def test_faces_card_and_fixed(self):
        """card is the face a card and a reveal portrait wear (0 unless it says; a pinned troop's own face unless it says), fixed pins
        a singular character (the Marshal) to one face whatever its UUID; both ride in the model."""
        self.assertEqual(self.bake(self.faces_troops(card=2))['knight']['faces'], {'count': 4, 'card': 2})
        self.assertEqual(self.bake(self.faces_troops(fixed=1))['knight']['faces'], {'count': 4, 'card': 1, 'fixed': 1})
        self.assertEqual(self.bake(self.faces_troops(fixed=1, card=3))['knight']['faces'], {'count': 4, 'card': 3, 'fixed': 1})
        self.assertEqual(self.bake(self.faces_troops(count=2, slots=FACE_SLOTS[:1]))['knight']['faces'], {'count': 2, 'card': 0})

    def test_faces_key_rules(self):
        cases = [   # name, the faces keys that change, what the message names
            ('no face', dict(count=0, slots=[]), ['faces', 'count']),
            ('nine faces', dict(count=9, slots=FACE_SLOTS * 3), ['faces', 'count']),
            ('count as text', dict(count='4'), ['faces', 'count']),
            ('too few slots', dict(slots=FACE_SLOTS[:2]), ['faces', 'slots', '3']),
            ('a slot that is no pair', dict(slots=[[0, 64], [32], [0, 80]]), ['slot 2']),
            ('a negative slot', dict(slots=[[0, 64], [-32, 64], [0, 80]]), ['slot 2']),
            ('a slot off the sheet', dict(slots=[[0, 64], [32, 64], [0, 90]]), ['slot 3', 'sheet']),
            ('a slot on the cap', dict(slots=[[32, 0], [32, 64], [0, 80]]), ['slot 1', 'overlaps', 'cap']),
            ('a slot on the head', dict(slots=[[0, 64], [32, 64], [8, 4]]), ['slot 3', 'overlaps', 'head']),
            ('slots on each other', dict(slots=[[0, 64], [16, 64], [0, 80]]), ['slot 2', 'overlaps', 'slot 1']),
            ('card out of range', dict(card=4), ['faces', 'card']),
            ('negative card', dict(card=-1), ['faces', 'card']),
            ('fixed out of range', dict(fixed=7), ['faces', 'fixed']),
            ('unknown key', dict(mood=1), ['faces', 'unknown key', 'mood']),
        ]
        for name, change, wants in cases:
            with self.subTest(name=name):
                said = self.bake_fails(self.faces_troops(**change))
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='faces is no object'):
            said = self.bake_fails(self.with_knight(size=[64, 96], faces=[4]))
            self.assertIn('faces', said)

        def outer(root):
            skin(root / 'art/troops/knight_o.png', paint=(0, 0, 0, 0))
        with self.subTest(name='an outer layer'):
            said = self.bake_fails(self.faces_troops(), outer)
            for want in ('faces', 'outer'):
                self.assertIn(want, said)

    def test_faces_patch_rules(self):
        def edit(f):
            def go(root):
                path = root / 'art/troops/knight_faces.png'
                im = Image.open(path).convert('RGBA')
                f(im)
                im.save(path)
            return go
        cases = [   # name, a change to art/troops/knight_faces.png, what the message names
            ('missing', lambda root: (root / 'art/troops/knight_faces.png').unlink(), ['knight_faces.png', 'missing']),
            ('half alpha', edit(lambda im: im.putpixel((9, 3), (200, 190, 180, 128))), ['knight_faces.png', 'alpha 128', '(9, 3)']),
            ('a hole in a face', edit(lambda im: im.putpixel((9, 3), (0, 0, 0, 0))), ['knight_faces.png', 'alpha 0', '(9, 3)']),
            ('paint off the faces', edit(lambda im: im.putpixel((1, 2 * 16 + 1), (200, 190, 180, 255))),
             ['knight_faces.png', 'patch 2', 'outside', '(1, 33)']),
            ('a side colour', edit(lambda im: im.putpixel((9, 3), (227, 179, 65, 255))), ['knight_faces.png', 'gold side colour']),
        ]
        for name, change, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = faces_fixture(tmp, troops=self.faces_troops())
                change(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='wrong size'), tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, troops=self.faces_troops())
            put_png(root / 'art/troops/knight_faces.png', face_patches().crop((0, 0, 32, 48)))
            said = self.fails(export_assets, root)
            for want in ('knight_faces.png', '32x48', '32x64'):
                self.assertIn(want, said)
        with self.subTest(name='wrong size at density 3'), tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, density=3, troops=self.faces_troops(density=3))
            put_png(root / 'art/troops/knight_faces.png', face_patches(1))
            said = self.fails(export_assets, root)
            for want in ('knight_faces.png', '96x192'):
                self.assertIn(want, said)
        with self.subTest(name='a faces file with no faces block'), tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp)
            put_png(root / 'art/troops/knight_faces.png', face_patches())
            said = self.fails(export_assets, root)
            for want in ('knight_faces.png', 'faces'):
                self.assertIn(want, said)

    def test_faces_composite(self):
        """The patches go into every own skin: patch 0 over the head's faces, patches 1 to 3 at their slots, only on the texels of the
        six faces (a patch's corners are left alone), at the troop's density; nothing else of the skin changes, the veteran trim
        and the glow are shipped as they are, and the far twin is made from the composited skin."""
        for density in (1, 3):
            with self.subTest(density=density), tempfile.TemporaryDirectory() as tmp:
                troops = self.faces_troops(density=density if density > 1 else None)
                troops['knight']['skins'] = {'1': 'knight_s1.png', '2': 'knight_s2.png'}
                root = faces_fixture(tmp, density, troops)
                size = (64 * density, 96 * density)
                trim = relief_png(root, 'knight_veteran.png', self.raised([(24, 22), (25, 22)], density), size=size)
                lit = Image.new('RGBA', size, (0, 0, 0, 0))
                lit.putpixel((24 * density, 22 * density), (255, 240, 200, 255))
                lit.save(root / 'art/troops/knight_e.png')
                patches = face_patches(density)
                index = self.export(root)
                base = root / ASSETS / 'textures/entity/troop'
                for name in ('knight_s1.png', 'knight_s2.png'):
                    want = Image.open(root / 'art/troops' / name).convert('RGBA')
                    for k, (u, v) in enumerate([(0, 0)] + FACE_SLOTS):
                        for x0, y0, x1, y1 in HEAD_FACES:
                            for y in range(y0 * density, y1 * density):
                                for x in range(x0 * density, x1 * density):
                                    want.putpixel((u * density + x, v * density + y), patches.getpixel((x, k * 16 * density + y)))
                    got = Image.open(base / name).convert('RGBA')
                    self.assertEqual(got.size, size)
                    self.assertEqual(got.tobytes(), want.tobytes(), name)
                    self.assertNotEqual(got.tobytes(), Image.open(root / 'art/troops' / name).convert('RGBA').tobytes(), name)
                    self.assertEqual(got.getpixel((0, 64 * density))[3], 0)   # a slot's corner (outside the six faces) stays clear
                self.assertEqual(Image.open(base / 'knight_veteran.png').convert('RGBA').tobytes(), trim.tobytes())
                self.assertEqual(Image.open(base / 'knight_e.png').convert('RGBA').tobytes(), lit.tobytes())
                if density > 1:
                    s1 = Image.open(base / 'knight_s1.png').convert('RGBA')
                    twin = Image.open(base / 'knight_s1_far.png').convert('RGBA')
                    self.assertEqual(twin.size, (64, 96))
                    self.assertEqual(twin.tobytes(), build_skins.far_twin(s1, density).tobytes())
                    self.assertGreater(twin.getpixel((12, 76))[3], 0)   # slot 1's front survives in the twin
                self.assertFalse([n for n in index['outputs'] if 'faces' in n], 'the patch file is a source, not an output')

    def test_faces_free_rows(self):
        """The sheet around a slot is free: no skin may paint a slot's 32x16 rectangle, and the veteran trim and the glow must not
        light it (a face on a slot would be painted over by them in every variant but 0)."""
        def paint_at(xy):
            def go(root):
                im = Image.open(root / 'art/troops/knight_s1.png').convert('RGBA')
                im.putpixel(xy, (200, 196, 186, 255))
                im.save(root / 'art/troops/knight_s1.png')
            return go

        def trim_in_slot(root):
            relief_png(root, 'knight_veteran.png', [(10, 74)], size=(64, 96))

        def glow_in_slot(root):
            lit = Image.new('RGBA', (64, 96), (0, 0, 0, 0))
            lit.putpixel((10, 74), (255, 240, 200, 255))
            lit.save(root / 'art/troops/knight_e.png')
        cases = [
            ('a skin paints slot 1', paint_at((3, 70)), ['knight_s1.png', 'slot 1', '(3, 70)']),
            ('a skin paints slot 3', paint_at((5, 85)), ['knight_s1.png', 'slot 3', '(5, 85)']),
            ('the trim reaches a slot', trim_in_slot, ['knight_veteran.png', 'slot 1', '(10, 74)']),
            ('the glow lights a slot', glow_in_slot, ['knight_e.png', 'slot 1', '(10, 74)']),
        ]
        for name, setup, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = faces_fixture(tmp, troops=self.faces_troops())
                setup(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)

    def test_faces_overlays_keep_off_variant_zero(self):
        """The veteran trim and the glow are drawn over whichever face shows, so they may touch variant 0's head and beard rectangles as
        little as the slots: a mark there would show on a quarter of the faces only. A skin may paint them (patch 0 replaces it), and a
        troop of count 1 has no uneven faces, so its trim may lie on its head."""
        def trim_at(xy):
            return lambda root: relief_png(root, 'knight_veteran.png', [xy], size=(64, 96))

        def glow_at(xy):
            def go(root):
                lit = Image.new('RGBA', (64, 96), (0, 0, 0, 0))
                lit.putpixel(xy, (255, 240, 200, 255))
                lit.save(root / 'art/troops/knight_e.png')
            return go
        cases = [
            ('the trim on the head rect', False, trim_at((10, 5)), ['knight_veteran.png', 'canonical face', '(10, 5)']),
            ('the glow on the head rect', False, glow_at((10, 5)), ['knight_e.png', 'canonical face', '(10, 5)']),
            ('the trim on the beard rect', True, trim_at((50, 58)), ['knight_veteran.png', 'canonical beard', '(50, 58)']),
            ('the glow on the beard rect', True, glow_at((50, 58)), ['knight_e.png', 'canonical beard', '(50, 58)']),
        ]
        for name, beard, setup, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = self.beard_root(tmp) if beard else faces_fixture(tmp, troops=self.faces_troops())
                setup(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='count 1 trims its head'), tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, troops=self.with_knight(size=[64, 96], faces={'count': 1}), patches=False)
            put_png(root / 'art/troops/knight_faces.png', face_patches(1, 1))
            trim_at((10, 5))(root)
            self.export(root)

    # ---- faces at their own density: the faces of a density 3 sheet drawn at 2 texels a px (Kort's ruling, 2026-10-24) ----

    def dense_troops(self, **faces):
        """faces_troops of a density 3 knight whose faces are drawn at 2 texels a px."""
        return self.faces_troops(density=3, fdensity=2, **faces)

    def dense_root(self, tmp, troops=None, rects=(), patches=True):
        """The density 3 fixture with 2x patches: its skins painted all over but the compact slot rectangles (22x11 layout px) and `rects`."""
        troops = troops or self.dense_troops()
        troops['knight']['skins'] = {'1': 'knight_s1.png', '2': 'knight_s2.png'}
        root = faces_fixture(tmp, 3, troops, patches=False)
        for name, paint in (('knight_s1.png', (200, 196, 186, 255)), ('knight_s2.png', (190, 186, 176, 255))):
            face_skin(root / 'art/troops' / name, 3, slots=[], paint=paint, rects=[(u, v, u + 22, v + 11) for u, v in FACE_SLOTS] + list(rects))
        if patches:
            put_png(root / 'art/troops/knight_faces.png', face_patches(2))
        return root

    def test_faces_dense_model(self):
        """A faces block with "density": 2 on a density 3 troop draws its face bones at 2 texels a px: each face bone has its own sheet,
        the sheet in face px (the PNG's 192x288 texels over 2 = 96x144), and its boxes' uv in face px (a slot's texel origin over 2), so
        the client's one unit (a box px of the bone) is two texels and the face's rectangle is 64x32 texels, not 96x48. The other bones
        keep the layout sheet; the model keeps its size, its density and its faces key."""
        model = self.bake(self.dense_troops())['knight']
        self.assertEqual(model['size'], [64, 96])
        self.assertEqual(model['density'], 3)
        self.assertEqual(tuple(model), build_skins.model_keys(3, True))
        self.assertEqual(model['faces'], {'count': 4, 'card': 0})
        for k, (x, y) in enumerate([[0, 0]] + FACE_SLOTS):
            b = find(model, 'face_v%d' % k)
            self.assertEqual(b['sheet'], [96, 144], b['name'])
            self.assertEqual(b['boxes'][0]['uv'], [x * 3 // 2, y * 3 // 2], b['name'])
            self.assertEqual(b['boxes'][0]['size'], [8, 8, 8])
        self.assertEqual(find(model, 'body')['sheet'], [64, 96])
        self.assertEqual(find(model, 'body')['boxes'][0]['uv'], [16, 16])
        same = self.bake(self.faces_troops(density=3))['knight']   # no key: the faces at the troop's density as before
        self.assertEqual(find(same, 'face_v1')['sheet'], [64, 96])
        self.assertEqual(find(same, 'face_v1')['boxes'][0]['uv'], FACE_SLOTS[0])

    def test_faces_dense_uv_lands_on_texels(self):
        """Every corner of a dense face bone's six faces, as the client computes the UV (the bone's uv plus the box px over its sheet),
        falls on a whole texel of the 192x288 PNG: no UV is inside a texel, so no patch edge bleeds into its neighbour, and the
        rectangle of face k is exactly the 64x32 texels pasted at its slot."""
        model = self.bake(self.dense_troops())['knight']
        for k, (x, y) in enumerate([[0, 0]] + FACE_SLOTS):
            b = find(model, 'face_v%d' % k)
            u, v = b['boxes'][0]['uv']
            sw, sh = b['sheet']
            corners = [(u + du, v + dv) for du in (0, 8, 16, 24, 32) for dv in (0, 8, 16)]
            for cu, cv in corners:
                tx, ty = cu * 192 / sw, cv * 288 / sh
                self.assertAlmostEqual(tx, round(tx), places=9, msg=(k, cu, cv))
                self.assertAlmostEqual(ty, round(ty), places=9, msg=(k, cu, cv))
            self.assertEqual((u * 192 / sw, v * 288 / sh, (u + 32) * 192 / sw, (v + 16) * 288 / sh), (3 * x, 3 * y, 3 * x + 64, 3 * y + 32))

    def test_faces_dense_composite(self):
        """The 2x patches go into a 3x skin texel for texel: patch k at slot k's texel origin (3 x the slot), 64x32 texels over the
        six faces; variant 0's old head rectangle (96x48 texels) is cleared but for patch 0 (the stale 3x head is unused); nothing else
        changes. The far twin is made from the composited skin and keeps the patch's last texel column and row (the block that
        straddles the patch's edge is judged by the patch's texels in it)."""
        with tempfile.TemporaryDirectory() as tmp:
            root = self.dense_root(tmp)
            self.export(root)
            base = root / ASSETS / 'textures/entity/troop'
            patches = face_patches(2)
            for name in ('knight_s1.png', 'knight_s2.png'):
                want = Image.open(root / 'art/troops' / name).convert('RGBA')
                for y in range(48):
                    for x in range(96):
                        want.putpixel((x, y), (0, 0, 0, 0))
                for k, (u, v) in enumerate([(0, 0)] + FACE_SLOTS):
                    for x0, y0, x1, y1 in HEAD_FACES:
                        for y in range(y0 * 2, y1 * 2):
                            for x in range(x0 * 2, x1 * 2):
                                want.putpixel((u * 3 + x, v * 3 + y), patches.getpixel((x, k * 32 + y)))
                got = Image.open(base / name).convert('RGBA')
                self.assertEqual(got.size, (192, 288))
                self.assertEqual(got.tobytes(), want.tobytes(), name)
            s1 = Image.open(base / 'knight_s1.png').convert('RGBA')
            twin = Image.open(base / 'knight_s1_far.png').convert('RGBA')
            islands = [(u * 3, v * 3, u * 3 + 64, v * 3 + 32) for u, v in [(0, 0)] + FACE_SLOTS]
            self.assertEqual(twin.tobytes(), build_skins.far_twin(s1, 3, islands).tobytes())
            for u, v in [(0, 0)] + FACE_SLOTS:   # the last column (texels 63 to 65 of the patch's row 18 to 20) and the last row
                self.assertGreater(twin.getpixel((u + 21, v + 6))[3], 0, (u, v))
                self.assertGreater(twin.getpixel((u + 8, v + 10))[3], 0, (u, v))
            plain = build_skins.far_twin(s1, 3)
            self.assertEqual(plain.getpixel((21, 6))[3], 0)   # what the plain 3x3 rule does with the edge block: a hole in the back of the head

    def test_faces_dense_rules(self):
        """The block's density is a whole number from 1 to 4; above the troop's it must be a whole multiple (it is reduced by that);
        below it the faces need a face bone (count above 1), a sheet of a whole number of face px, and every uv a whole number of them."""
        for value in (0, 5, 2.5, True, '2'):
            with self.subTest(value=value):
                said = self.bake_fails(self.faces_troops(fdensity=value))
                for want in ('knight', 'faces', 'density'):
                    self.assertIn(want, said)
        with self.subTest(name='4 over 3'):
            said = self.bake_fails(self.faces_troops(density=3, fdensity=4))
            for want in ('knight', 'faces', 'density 4', 'multiple', '3'):
                self.assertIn(want, said)
        with self.subTest(name='count 1 below the density'):
            said = self.bake_fails(self.with_knight(size=[64, 96], density=3, faces={'count': 1, 'density': 2}))
            for want in ('knight', 'count 1', 'density'):
                self.assertIn(want, said)
        with self.subTest(name='an odd slot'):
            said = self.bake_fails(self.dense_troops(slots=[[1, 64], [32, 64], [0, 80]]))
            for want in ('knight', 'slot 1', '(1, 64)', 'multiple of 2'):
                self.assertIn(want, said)
        with self.subTest(name='an odd sheet'):
            said = self.bake_fails(self.with_knight(size=[65, 96], density=3, faces={'count': 4, 'density': 2, 'slots': FACE_SLOTS}))
            for want in ('knight', 'sheet', 'multiple of 2'):
                self.assertIn(want, said)
        with self.subTest(name='the unit of the key'):
            model = self.bake(self.faces_troops(density=3, fdensity=3))['knight']   # the troop's own density: as no key
            self.assertEqual(find(model, 'face_v1')['sheet'], [64, 96])

    def test_faces_dense_slots_collide_at_their_own_size(self):
        """A dense slot is the rectangle it needs (a 64x32 texel patch is 22x11 layout px on a 3x sheet, not 32x16): two fit side by
        side where full-size ones would overlap, the one slot that leaves the sheet is the one whose compact rectangle leaves it."""
        troops = self.dense_troops(slots=[[0, 64], [22, 64], [44, 76]])   # 22 wide each: the sheet is 64 wide, the third ends at 66
        said = self.bake_fails(troops)
        for want in ('knight', 'slot 3', 'leaves', '64x96', '22x11'):
            self.assertIn(want, said)
        self.bake(self.dense_troops(slots=[[0, 64], [22, 64], [42, 76]]))   # fits: the third ends at 64
        said = self.bake_fails(self.dense_troops(slots=[[0, 64], [20, 64], [42, 76]]))
        for want in ('knight', 'slot 2', 'overlaps', 'slot 1'):
            self.assertIn(want, said)
        self.bake(self.dense_troops(slots=[[0, 64], [22, 64], [0, 84]]))   # 84 + 11 = 95 (an odd y cannot be a whole number of face px)
        said = self.bake_fails(self.dense_troops(slots=[[0, 64], [22, 64], [0, 86]]))
        for want in ('knight', 'slot 3', 'leaves'):
            self.assertIn(want, said)
        said = self.bake_fails(self.faces_troops(density=3, slots=[[0, 64], [22, 64], [42, 64]]))   # no key: full-size 32x16 rectangles overlap
        for want in ('knight', 'slot 2', 'overlaps'):
            self.assertIn(want, said)

    def test_faces_dense_overlays_and_relief(self):
        """The trim and the glow keep off the whole reserved rectangle of a dense slot (its texels rounded up to layout px) and off
        variant 0's whole old head rectangle; a skin may paint the old rectangle (patch 0 replaces what it covers and the rest is
        cleared); relief on the patch is refused naming the face bone, and relief on the stale rest of the old rectangle lies on no face."""
        def trim_at(xy):
            return lambda root: relief_png(root, 'knight_veteran.png', [xy], size=(192, 288))

        def glow_at(xy):
            def go(root):
                lit = Image.new('RGBA', (192, 288), (0, 0, 0, 0))
                lit.putpixel(xy, (255, 240, 200, 255))
                lit.save(root / 'art/troops/knight_e.png')
            return go
        cases = [
            ('the trim at the corner of slot 1\'s reserved rectangle', trim_at((65, 224)), ['knight_veteran.png', 'slot 1', '(65, 224)']),
            ('the glow at the end of slot 2', glow_at((96 + 65, 224)), ['knight_e.png', 'slot 2', '(161, 224)']),
            ('the trim in the old head rectangle beyond the patch', trim_at((80, 10)), ['knight_veteran.png', 'canonical face', '(80, 10)']),
        ]
        for name, setup, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = self.dense_root(tmp)
                setup(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='the trim just outside slot 1'), tempfile.TemporaryDirectory() as tmp:
            root = self.dense_root(tmp)
            trim_at((66, 224))(root)
            self.export(root)
        for name, texel, wants in (('relief on the patch', (20, 20), ['face bone', 'face_v0', '(20, 20)']),
                                   ('relief on the stale head', (80, 10), ['no skin box face', '(80, 10)'])):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = self.dense_root(tmp)
                for r in ('knight_s1_r.png', 'knight_s2_r.png'):
                    relief_png(root, r, [texel], size=(192, 288))
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)

    def test_faces_one_x_troops_reduce_two_to_one(self):
        """A density 1 troop whose faces block says density 2 takes patches drawn at 2 texels a px and reduces them by the far twin's
        majority rule, 2 to 1 (the 8x8 head of the default look); a file of any other size is refused naming the density; a density 3
        troop whose block says 2 refuses a 3x file."""
        with tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, 1, self.faces_troops(fdensity=2), patches=False)
            put_png(root / 'art/troops/knight_faces.png', self.scaled_patches(2))
            self.export(root)
            got = Image.open(root / ASSETS / 'textures/entity/troop/knight_s1.png').convert('RGBA')
            want = Image.open(root / 'art/troops/knight_s1.png').convert('RGBA')
            patches = face_patches(1)
            for k, (u, v) in enumerate([(0, 0)] + FACE_SLOTS):
                for x0, y0, x1, y1 in HEAD_FACES:
                    for y in range(y0, y1):
                        for x in range(x0, x1):
                            want.putpixel((u + x, v + y), patches.getpixel((x, k * 16 + y)))
            self.assertEqual(got.tobytes(), want.tobytes())
        for density, scale in ((1, 1), (1, 3), (3, 3), (3, 1)):
            with self.subTest(density=density, scale=scale), tempfile.TemporaryDirectory() as tmp:
                root = faces_fixture(tmp, density, self.faces_troops(density=density if density > 1 else None, fdensity=2), patches=False)
                put_png(root / 'art/troops/knight_faces.png', self.scaled_patches(scale))
                said = self.fails(export_assets, root)
                for want in ('knight', 'knight_faces.png', '64x128', '2 texels a px'):
                    self.assertIn(want, said)

    def test_faces_dense_verify_and_templates(self):
        """--verify rebuilds a dense-faces troop (a changed texel in its skin or its far twin is named) and the templates draw each face
        bone at its own density: slot 1's front is the 64x32 texel patch's, not a 96x48 one."""
        with tempfile.TemporaryDirectory() as tmp:
            root = self.dense_root(tmp)
            self.export(root)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.verify(root), 0)
            for target in ('textures/entity/troop/knight_s1.png', 'textures/entity/troop/knight_s1_far.png', 'models/troop/knight.json'):
                path = root / ASSETS / target
                before = path.read_bytes()
                data = bytearray(before)
                data[len(data) // 2] ^= 0x01
                path.write_bytes(bytes(data))
                said = self.fails(build_skins.verify, root)
                self.assertIn(target, said)
                path.write_bytes(before)
            cat = build_skins.load(root)
            flat, relief = build_skins.template(cat, 'knight').convert('RGBA'), build_skins.relief_template(cat, 'knight').convert('RGBA')
        self.assertEqual(flat.size, (192, 288))
        self.assertGreater(flat.getpixel((24, 215))[3], 0)    # slot 1 (texel origin (0, 192)): its front is texels (16..32, 208..224)
        self.assertGreater(flat.getpixel((63, 223))[3], 0)    # the back's last texel, the patch's 64th column
        self.assertEqual(flat.getpixel((64, 223))[3], 0)      # beyond the 64 texel patch (a 96 texel one would paint it)
        self.assertEqual(flat.getpixel((8, 200))[3], 0)       # the corner of the patch rectangle, outside the six faces
        self.assertEqual(relief.size, (192, 288))
        self.assertEqual(relief.getpixel((64, 223))[3], 0)

    def test_faces_carry_no_relief(self):
        """Faces are flat paint: a relief texel on the head's faces or on a slot's is refused, naming the face bone; relief elsewhere
        (the body) still builds, and no face bone carries any."""
        for name, texel, wants in (('the head front', (12, 12), ['face bone', 'face_v0', '(12, 12)']),
                                   ('slot 2', (44, 76), ['face bone', 'face_v2', '(44, 76)'])):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = faces_fixture(tmp, troops=self.faces_troops())
                relief_png(root, 'knight_s1_r.png', [texel], size=(64, 96))
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, troops=self.faces_troops())
            relief_png(root, 'knight_s1_r.png', [(22, 22)], size=(64, 96))
            self.export(root)
            model = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
        self.assertIn('relief', find(model, 'body'))
        self.assertFalse([b['name'] for b in model['bones'] if b['name'].startswith('face_v') and 'relief' in b])

    def test_faces_verify_and_templates(self):
        """--verify rebuilds a faces troop (a changed slot texel is named), and the --template sheets show the slots: a flat
        colour on the six faces of each, the relief template hatching them as ground where relief may not stand."""
        with tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, troops=self.faces_troops())
            self.export(root)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.verify(root), 0)
            path = root / ASSETS / 'textures/entity/troop/knight_s1.png'
            before = path.read_bytes()
            im = Image.open(path).convert('RGBA')
            im.putpixel((12, 76), (1, 2, 3, 255))
            im.save(path)
            said = self.fails(build_skins.verify, root)
            self.assertIn('knight_s1.png', said)
            self.assertIn('(12, 76)', said)
            path.write_bytes(before)
            cat = build_skins.load(root)
            flat, relief = build_skins.template(cat, 'knight').convert('RGBA'), build_skins.relief_template(cat, 'knight').convert('RGBA')
        self.assertEqual(flat.size, (64, 96))
        self.assertGreater(flat.getpixel((12, 76))[3], 0)     # slot 1's front
        self.assertEqual(flat.getpixel((2, 66))[3], 0)        # its corner, outside the six faces
        red = build_skins.RELIEF_RED + (255,)
        self.assertIn(red, [relief.getpixel((x, y)) for x in range(8, 16) for y in range(72, 80)])

    # ---- the faces' source resolution, the banked patches, the beard boxes ----

    def scaled_patches(self, scale, count=4):
        """face_patches(1) drawn at `scale` texels a px: every px a block of one colour but its first texel, which is noise, so the
        majority of a block is the px's colour and a reduction by the block must give face_patches(1) back."""
        flat = face_patches(1, count)
        im = Image.new('RGBA', (flat.width * scale, flat.height * scale), (0, 0, 0, 0))
        for y in range(flat.height):
            for x in range(flat.width):
                c = flat.getpixel((x, y))
                if not c[3]:
                    continue
                for j in range(scale):
                    for i in range(scale):
                        im.putpixel((x * scale + i, y * scale + j), (230, 230, 226, 255) if (i, j) == (0, 0) and scale > 1 else c)
        return im

    def test_faces_source_resolution(self):
        """Face art is drawn at 3 texels a px whatever the troop's sheet: patches at a whole multiple of the troop's density are taken,
        and reduced to the density by the same majority rule as the far twin (a block takes its most common colour). A troop at density
        1 builds the same sheet from 1x, 2x and 3x patches; a patch at a lower density or at a fraction of the density is refused."""
        for density, scale in ((1, 1), (1, 2), (1, 3), (3, 3), (3, 6)):
            with self.subTest(density=density, scale=scale), tempfile.TemporaryDirectory() as tmp:
                root = faces_fixture(tmp, density, self.faces_troops(density=density if density > 1 else None), patches=False)
                put_png(root / 'art/troops/knight_faces.png', self.scaled_patches(scale))
                self.export(root)
                got = Image.open(root / ASSETS / 'textures/entity/troop/knight_s1.png').convert('RGBA')
                patches = self.scaled_patches(scale) if scale == density else face_patches(1).resize((32 * density, 64 * density), Image.NEAREST)
                for k, (u, v) in enumerate([(0, 0)] + FACE_SLOTS):
                    for x0, y0, x1, y1 in HEAD_FACES:
                        for y in range(y0 * density, y1 * density):
                            for x in range(x0 * density, x1 * density):
                                self.assertEqual(got.getpixel((u * density + x, v * density + y)), patches.getpixel((x, k * 16 * density + y)), (k, x, y))
        for name, density, make, wants in (
                ('a lower density than the troop', 3, lambda: self.scaled_patches(1), ['knight_faces.png', '32x64', '96x192']),
                ('a fraction of the density', 3, lambda: self.scaled_patches(2), ['knight_faces.png', '64x128', '96x192']),
                ('a width that is no whole scale', 1, lambda: self.scaled_patches(3).crop((0, 0, 94, 192)), ['knight_faces.png', '94x192']),
                ('a height that is no whole patch', 1, lambda: self.scaled_patches(3).crop((0, 0, 96, 190)), ['knight_faces.png', '96x190']),
                ('fewer patches than the count', 1, lambda: self.scaled_patches(3).crop((0, 0, 96, 144)), ['knight_faces.png', '96x144', '3 patches']),
        ):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = faces_fixture(tmp, density, self.faces_troops(density=density if density > 1 else None), patches=False)
                put_png(root / 'art/troops/knight_faces.png', make())
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='paint off the faces at 3x'), tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, 1, self.faces_troops(), patches=False)
            im = self.scaled_patches(3)
            im.putpixel((2, 2 * 48 + 2), (200, 190, 180, 255))
            put_png(root / 'art/troops/knight_faces.png', im)
            said = self.fails(export_assets, root)
            for want in ('knight_faces.png', 'patch 2', 'outside', '(2, 98)'):
                self.assertIn(want, said)

    def test_faces_banked_patches(self):
        """A faces block of count 1 is a troop that wears its canonical face only (the Templar, whose great helm hides it): no face bones,
        no faces key in the model, no slots; patch 0 is pasted over the head's faces in every own skin. The patch file may hold more
        patches than the count (banked art, to wire later), and a file with fewer is refused."""
        for extra in (0, 3):
            with self.subTest(banked=extra), tempfile.TemporaryDirectory() as tmp:
                troops = self.with_knight(size=[64, 96], faces={'count': 1})
                root = faces_fixture(tmp, troops=troops, patches=False)
                put_png(root / 'art/troops/knight_faces.png', face_patches(1, 1 + extra))
                before = Image.open(root / 'art/troops/knight_s1.png').convert('RGBA')
                self.export(root)
                model = json.loads((root / ASSETS / 'models/troop/knight.json').read_text(encoding='utf-8'))
                self.assertNotIn('faces', model)
                self.assertEqual(tuple(model), build_skins.MODEL_KEYS)
                self.assertFalse([b['name'] for b in model['bones'] if b['name'].startswith('face_v') or 'variants' in b])
                self.assertEqual(len(find(model, 'head')['boxes']), 1)
                got = Image.open(root / ASSETS / 'textures/entity/troop/knight_s1.png').convert('RGBA')
                want = before.copy()
                patches = face_patches(1, 1)
                for x0, y0, x1, y1 in HEAD_FACES:
                    for y in range(y0, y1):
                        for x in range(x0, x1):
                            want.putpixel((x, y), patches.getpixel((x, y)))
                self.assertEqual(got.tobytes(), want.tobytes())
        for name, change, wants in (
                ('count 1 with slots', dict(count=1, slots=FACE_SLOTS[:1]), ['faces', 'count 1', 'slots']),
                ('count 1 fixed to 1', dict(count=1, fixed=1), ['faces', 'fixed']),
                ('count 0', dict(count=0, slots=[]), ['faces', 'count'])):
            with self.subTest(name=name):
                said = self.bake_fails(self.with_knight(size=[64, 96], faces=change))
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='fewer patches than the count'), tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, troops=self.faces_troops(), patches=False)
            put_png(root / 'art/troops/knight_faces.png', face_patches(1, 3))
            said = self.fails(export_assets, root)
            for want in ('knight_faces.png', '32x48', '32x64'):
                self.assertIn(want, said)

    # the beard box: the head's second box (the mage's and the Marshal's beard volume), whose variants are drawn by <troop>_beards.png

    def beard_troops(self, density=None, **faces):
        """faces_troops with a beard volume (box [6, 4, 1] at uv (48, 56)) as the head's second box and three beard slots."""
        troops = self.faces_troops(density=density, **dict({'beard': {'slots': BEARD_SLOTS}}, **faces))
        troops['knight']['bones'] = {'head': {'boxes': [box([-4, -8, -4], [8, 8, 8], [0, 0]), box([-3, -3, -4.9], [6, 4, 1], [48, 56])]}}
        return troops

    def test_faces_beard_model(self):
        """With a beard block the head's second box goes into the face bones with the first: face_v0 holds both where they were, face_v1
        to 3 the head box at its slot and the beard box at its beard slot; `head` keeps no box. Without the block the beard stays on
        `head` and shows in every variant, as it did."""
        model = self.bake(self.beard_troops())['knight']
        self.assertEqual(find(model, 'head')['boxes'], [])
        for k in range(4):
            b = find(model, 'face_v%d' % k)
            self.assertEqual(len(b['boxes']), 2)
            self.assertEqual(b['boxes'][0]['uv'], [0, 0] if k == 0 else FACE_SLOTS[k - 1])
            self.assertEqual(b['boxes'][1]['uv'], [48, 56] if k == 0 else BEARD_SLOTS[k - 1])
            self.assertEqual((b['boxes'][1]['size'], b['boxes'][1]['origin']), ([6, 4, 1], [-3, -3, -4.9]))
            self.assertEqual(b['variants'], [k])
        self.assertEqual(self.bake(self.beard_troops())['knight']['faces'], {'count': 4, 'card': 0})
        troops = self.faces_troops()
        troops['knight']['bones'] = {'head': {'boxes': [box([-4, -8, -4], [8, 8, 8], [0, 0]), box([-3, -3, -4.9], [6, 4, 1], [48, 56])]}}
        plain = self.bake(troops)['knight']
        self.assertEqual(len(find(plain, 'head')['boxes']), 1)
        self.assertEqual([len(find(plain, 'face_v%d' % k)['boxes']) for k in range(4)], [1, 1, 1, 1])

    def test_faces_beard_rules(self):
        cases = [   # name, the faces keys that change, what the message names
            ('too few beard slots', dict(beard={'slots': BEARD_SLOTS[:2]}), ['faces', 'beard', 'slots', '3']),
            ('a beard slot off the sheet', dict(beard={'slots': [[32, 80], [46, 80], [60, 95]]}), ['beard slot 3', 'sheet']),
            ('a beard slot on a head slot', dict(beard={'slots': [[32, 80], [46, 80], [2, 66]]}), ['beard slot 3', 'overlaps', 'slot 1']),
            ('a beard slot on the head', dict(beard={'slots': [[32, 80], [46, 80], [4, 4]]}), ['beard slot 3', 'overlaps', 'head']),
            ('beard slots on each other', dict(beard={'slots': [[32, 80], [40, 80], [32, 85]]}), ['beard slot 2', 'overlaps', 'beard slot 1']),
            ('a head slot on a beard slot', dict(slots=[[32, 64], [32, 80], [0, 80]]), ['slot 2', 'overlaps', 'beard slot']),
            ('an unknown beard key', dict(beard={'slots': BEARD_SLOTS, 'mood': 1}), ['beard', 'unknown key', 'mood']),
            ('a beard that is no object', dict(beard=[1]), ['faces', 'beard']),
        ]
        for name, change, wants in cases:
            with self.subTest(name=name):
                said = self.bake_fails(self.beard_troops(**change))
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='a beard block and no second head box'):
            said = self.bake_fails(self.faces_troops(beard={'slots': BEARD_SLOTS}))
            for want in ('knight', 'beard', 'second box'):
                self.assertIn(want, said)

    def beard_root(self, tmp, density=1, troops=None):
        """The beard fixture: skins painted but for the head slots and the beard slots, art/troops/knight_faces.png and knight_beards.png."""
        root = faces_fixture(tmp, density, troops or self.beard_troops(density=density if density > 1 else None))
        for name, paint in (('knight_s1.png', (200, 196, 186, 255)), ('knight_s2.png', (190, 186, 176, 255))):
            if (root / 'art/troops' / name).is_file():
                face_skin(root / 'art/troops' / name, density, paint=paint, rects=[(u, v, u + 14, v + 5) for u, v in BEARD_SLOTS])
        put_png(root / 'art/troops/knight_beards.png', beard_patches(density))
        return root

    def test_faces_beard_composite(self):
        """Beard patch k replaces the texels of the beard box's six faces, at the box's own rectangle for patch 0 and at beard slot k for
        the rest, cut-out texels (alpha 0) included: the cut-out is the beard's silhouette, and the head patch of the same variant shows
        through it. Texels of the rectangle outside the six faces stay as they were."""
        for density in (1, 3):
            with self.subTest(density=density), tempfile.TemporaryDirectory() as tmp:
                troops = self.beard_troops(density=density if density > 1 else None)
                troops['knight']['skins'] = {'1': 'knight_s1.png', '2': 'knight_s2.png'}
                root = self.beard_root(tmp, density, troops)
                self.export(root)
                beards, heads = beard_patches(density), face_patches(density)
                got = Image.open(root / ASSETS / 'textures/entity/troop/knight_s1.png').convert('RGBA')
                want = Image.open(root / 'art/troops/knight_s1.png').convert('RGBA')
                for k in range(4):
                    for (u, v), src, parts, h in (((0, 0) if k == 0 else tuple(FACE_SLOTS[k - 1]), heads, HEAD_FACES, 16),
                                                  ((48, 56) if k == 0 else tuple(BEARD_SLOTS[k - 1]), beards, BEARD_FACES, 5)):
                        for x0, y0, x1, y1 in parts:
                            for y in range(y0 * density, y1 * density):
                                for x in range(x0 * density, x1 * density):
                                    want.putpixel((u * density + x, v * density + y), src.getpixel((x, k * h * density + y)))
                self.assertEqual(got.tobytes(), want.tobytes())
                cut = [(x, y) for x0, y0, x1, y1 in BEARD_FACES for x in range(x0 * density, x1 * density) for y in range(y0 * density, y1 * density)
                       if beards.getpixel((x, y))[3] == 0]
                self.assertTrue(cut)
                for x, y in cut:
                    self.assertEqual(got.getpixel((48 * density + x, 56 * density + y))[3], 0)   # a cut-out texel is clear in the sheet
                if density > 1:
                    twin = Image.open(root / ASSETS / 'textures/entity/troop/knight_s1_far.png').convert('RGBA')
                    self.assertEqual(twin.tobytes(), build_skins.far_twin(got, density).tobytes())

    def test_faces_beard_patch_rules(self):
        def edit(name, f):
            def go(root):
                path = root / 'art/troops' / name
                im = Image.open(path).convert('RGBA')
                f(im)
                im.save(path)
            return go
        cases = [   # name, a change to the fixture, what the message names
            ('missing', lambda root: (root / 'art/troops/knight_beards.png').unlink(), ['knight_beards.png', 'missing']),
            ('half alpha', edit('knight_beards.png', lambda im: im.putpixel((5, 2), (200, 190, 180, 128))), ['knight_beards.png', 'alpha 128', '(5, 2)']),
            ('paint off the faces', edit('knight_beards.png', lambda im: im.putpixel((0, 5 + 0), (200, 190, 180, 255))), ['knight_beards.png', 'patch 1', 'outside', '(0, 5)']),
            ('a side colour', edit('knight_beards.png', lambda im: im.putpixel((5, 2), (227, 179, 65, 255))), ['knight_beards.png', 'gold side colour']),
            ('wrong size', lambda root: put_png(root / 'art/troops/knight_beards.png', beard_patches(1).crop((0, 0, 14, 15))), ['knight_beards.png', '14x15', '14x20']),
            ('a skin paints beard slot 2', edit('knight_s1.png', lambda im: im.putpixel((47, 82), (200, 196, 186, 255))), ['knight_s1.png', 'beard slot 2', '(47, 82)']),
        ]
        for name, change, wants in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = self.beard_root(tmp)
                change(root)
                said = self.fails(export_assets, root)
                self.assertIn('knight', said)
                for want in wants:
                    self.assertIn(want, said)
        with self.subTest(name='a beards file and no beard block'), tempfile.TemporaryDirectory() as tmp:
            root = faces_fixture(tmp, troops=self.faces_troops())
            put_png(root / 'art/troops/knight_beards.png', beard_patches(1))
            said = self.fails(export_assets, root)
            for want in ('knight_beards.png', 'beard'):
                self.assertIn(want, said)
        with self.subTest(name='a beard patch at 3 texels a px for a density 1 troop'), tempfile.TemporaryDirectory() as tmp:
            root = self.beard_root(tmp)
            put_png(root / 'art/troops/knight_beards.png', beard_patches(3))
            self.export(root)
            got = Image.open(root / ASSETS / 'textures/entity/troop/knight_s1.png').convert('RGBA')
            self.assertEqual(got.size, (64, 96))
            self.assertEqual(got.getpixel((48 + 4, 56 + 2))[3], 255)

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

    # ---- in-match progression Task 9: armour tiers, weapon tiers, the stopgap list, the chevron ----

    def tiers_root(self, tmp, troops=None, weapons=(), chevron=None):
        """The fixture with these extra weapon sources (sword_t2, ...) copied from the sword, and a chevron source when given."""
        root = fixture(tmp, troops=troops)
        for wid in weapons:
            write_json(root / 'art/weapons' / (wid + '.json'), dict(SWORD, weapon=wid))
            skin(root / 'art/weapons' / (wid + '.png'), (16, 16), (150, 150, 160, 255))
        if chevron is not None:
            put_png(root / 'art/marks/chevron.png', chevron)
        return root

    def test_armour_tiers_every_troop(self):
        """Armour tiers are the gearTiers rows (in-match progression design 4.4): an Epic, which has no level line, takes a
        tier II and III skin; a sheet with gearTiers rows 1 and 2 refuses a tier 3 skin."""
        troops = copy.deepcopy(TROOPS)
        troops['mage']['skins'] = {'1': 'mage_s1.png', '2': 'mage_s2.png', '3': 'mage_s3.png'}
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, troops=troops)
            self.export(root)
            model = json.loads((root / ASSETS / 'models/troop/mage.json').read_text(encoding='utf-8'))
        self.assertEqual(list(model['skins']), ['1', '2', '3'])
        sheet = copy.deepcopy(SHEET)
        sheet['gearTiers'] = [{'id': 't1', 'tier': 1}, {'id': 't2', 'tier': 2}]
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, troops=troops, sheet=sheet)
            said = self.fails(export_assets, root)
        self.assertIn('mage: a skin for armour tier 3, which no gear tier has (armour tiers [1, 2])', said)

    def test_own_skins_only(self):
        """The model's skins name only the troop's own files: a missing tier is the client's stopgap, never a copied key."""
        model = self.bake(TROOPS)['knight']
        self.assertEqual(model['skins'], {'1': 'gravesandglory:textures/entity/troop/knight_s1.png'})
        model = self.bake(self.with_knight(skins={'1': 'knight_s1.png', '3': 'knight_s3.png'}))['knight']
        self.assertEqual(list(model['skins']), ['1', '3'])

    def test_weapon_tier_parts(self):
        """The weaponTiers part key (design 4.4): the bone carries the weapon tiers it shows at; an entry without it carries
        none (the key is left out of the bone, so models without weapon parts keep their bytes); a tier no gear tier has fails."""
        parts = TROOPS['mage']['parts'] + [{'part': 'cap', 'uv': [32, 0], 'weaponTiers': [2, 3]}]
        troops = copy.deepcopy(TROOPS)
        troops['mage']['parts'] = parts
        model = self.bake(troops)['mage']
        self.assertEqual(find(model, 'cap')['weaponTiers'], [2, 3])
        self.assertNotIn('weaponTiers', find(model, 'pennant'))
        self.assertNotIn('weaponTiers', find(self.bake(TROOPS)['knight'], 'cap'))
        for bad, want in (([4], 'shows at weapon tiers [4]'), (2, 'weaponTiers must list the weapon tiers it shows at, not 2'), ([], 'weaponTiers must list')):
            troops['mage']['parts'] = TROOPS['mage']['parts'] + [{'part': 'cap', 'uv': [32, 0], 'weaponTiers': bad}]
            with self.subTest(bad=bad), tempfile.TemporaryDirectory() as tmp:
                said = self.fails(export_assets, fixture(tmp, troops=troops))
                self.assertIn('mage', said)
                self.assertIn(want, said)

    def test_weapon_tier_models(self):
        """art/weapons/<weapon>_t2 and _t3 build as their own weapons; one without its tier I weapon fails, and so does a troop
        that names a tier weapon instead of its tier I weapon."""
        with tempfile.TemporaryDirectory() as tmp:
            root = self.tiers_root(tmp, weapons=('sword_t2',))
            index = self.export(root)
            self.assertIn('sword_t2', index['weapons'])
            self.assertTrue((root / ASSETS / 'models/troop/weapon/sword_t2.json').is_file())
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, self.tiers_root(tmp, weapons=('axe_t2',)))
            self.assertIn('weapon axe_t2: a weapon tier of axe, which art/weapons lacks', said)
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, self.tiers_root(tmp, troops=self.with_knight(weapon='sword_t2'), weapons=('sword_t2',)))
            self.assertIn('knight: weapon sword_t2 is a weapon tier; name its tier I weapon sword', said)
        with tempfile.TemporaryDirectory() as tmp:
            said = self.fails(export_assets, self.tiers_root(tmp, weapons=('sword_t4',)))
            self.assertIn('weapon sword_t4: tier 4 is no weapon tier', said)

    def test_tiers_list(self):
        """--tiers (tier_table and stopgaps): per troop, each armour and weapon tier and the tier it draws; a troop with every
        tier of its own is no stopgap, a missing _s3 or weapon tier and a troop with no model at all are."""
        troops = copy.deepcopy(TROOPS)
        troops['knight']['skins'] = {'1': 'knight_s1.png', '2': 'knight_s2.png'}
        troops['sentinel']['skins'] = {'1': 'sentinel_s1.png', '2': 'sentinel_s2.png', '3': 'sentinel_s3.png'}
        troops['mage']['parts'] = TROOPS['mage']['parts'] + [{'part': 'cap', 'uv': [32, 0], 'weaponTiers': [3]}]
        troops['mage']['skins'] = {'1': 'mage_s1.png', '2': 'mage_s2.png', '3': 'mage_s3.png'}
        with tempfile.TemporaryDirectory() as tmp:
            root = self.tiers_root(tmp, troops=troops, weapons=('sword_t2', 'sword_t3'))
            cat = build_skins.load(root)
            rows = {r['troop']: r for r in build_skins.tier_table(cat)}
            gaps = build_skins.stopgaps(cat)
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                code = build_skins.tiers(root)
        self.assertEqual(rows['knight']['armour'], {1: 1, 2: 2, 3: 2})
        self.assertEqual(rows['knight']['weapon'], {1: 1, 2: 2, 3: 3})
        self.assertEqual(rows['sentinel']['armour'], {1: 1, 2: 2, 3: 3})
        self.assertEqual(rows['mage']['weapon'], {1: 1, 2: 1, 3: 3})
        self.assertIsNone(rows['hound']['armour'])
        self.assertEqual([g[0] for g in gaps], ['hound', 'knight', 'mage'])
        self.assertEqual(dict(gaps)['knight'], 'armour III draws II')
        self.assertEqual(dict(gaps)['mage'], 'weapon II draws I')
        self.assertEqual(dict(gaps)['hound'], 'no model')
        lines = out.getvalue().splitlines()
        self.assertEqual(code, 0)
        self.assertIn('build_skins: tiers knight: armour I II III->II; weapon sword I II III', lines)
        self.assertIn('build_skins: tiers mage: armour I II III; weapon parts I II->I III', lines)
        self.assertIn('build_skins: tiers sentinel: armour I II III; weapon sword I II III', lines)
        self.assertIn('build_skins: tiers hound: no model (draws its vanilla mob)', lines)
        self.assertEqual(lines[-1], 'build_skins: stopgap 3 troops: hound (no model), knight (armour III draws II), mage (weapon II draws I)')

    def test_partial_weapon_parts(self):
        """A weapon tier of weaponTiers parts has its own art only when every bone that carries a tier I weapon part carries one
        of that tier too (Task 9 review M7): a set with a tier II cap but no tier II guard draws tier I, and fails the gate."""
        troops = copy.deepcopy(TROOPS)
        troops['mage']['skins'] = {'1': 'mage_s1.png', '2': 'mage_s2.png', '3': 'mage_s3.png'}
        base = TROOPS['mage']['parts']
        partial = [{'part': 'cap', 'name': 'cap_1', 'uv': [32, 0], 'weaponTiers': [1]},
                   {'part': 'cap', 'name': 'cap_2', 'uv': [32, 0], 'weaponTiers': [2, 3]},
                   {'part': 'cap', 'name': 'guard_1', 'bone': 'body', 'uv': [32, 0], 'weaponTiers': [1]}]
        whole = partial + [{'part': 'cap', 'name': 'guard_2', 'bone': 'body', 'uv': [32, 0], 'weaponTiers': [2, 3]}]
        for parts, want, gap in ((partial, {1: 1, 2: 1, 3: 1}, 'weapon II draws I, weapon III draws I'), (whole, {1: 1, 2: 2, 3: 3}, None)):
            troops['mage']['parts'] = base + parts
            with self.subTest(parts=len(parts)), tempfile.TemporaryDirectory() as tmp:
                cat = build_skins.load(self.tiers_root(tmp, troops=troops))
                rows = {r['troop']: r for r in build_skins.tier_table(cat)}
                self.assertEqual(rows['mage']['weapon'], want)
                self.assertEqual(dict(build_skins.stopgaps(cat)).get('mage'), gap)

    def test_release_names(self):
        """The release files' names carry the gate's mark (Task 9 review I2), and an exporter takes only the player archive
        its own mark names: a -stopgap package never feeds an unmarked .mrpack or server kit, nor an unmarked one a marked."""
        self.assertEqual(build_skins.release_name('0.5.0', '', '.zip'), 'graves-and-glory-0.5.0.zip')
        self.assertEqual(build_skins.release_name('0.5.0', '-stopgap', '.mrpack'), 'graves-and-glory-0.5.0-stopgap.mrpack')
        self.assertEqual(build_skins.release_name('0.5.0', '-stopgap', '-server.zip'), 'graves-and-glory-0.5.0-stopgap-server.zip')
        self.assertEqual(build_skins.release_archive({'fileName': 'graves-and-glory-0.5.0.zip'}, '0.5.0', ''), 'graves-and-glory-0.5.0.zip')
        for name, mark in (('graves-and-glory-0.5.0-stopgap.zip', ''), ('graves-and-glory-0.5.0.zip', '-stopgap'), ('graves-and-glory-0.4.0.zip', '')):
            with self.subTest(name=name, mark=mark), self.assertRaises(SystemExit) as caught, contextlib.redirect_stdout(io.StringIO()) as out:
                build_skins.release_archive({'fileName': name}, '0.5.0', mark, 'export_modrinth')
            self.assertEqual(caught.exception.code, 1)
            self.assertIn('export_modrinth: refused: dist/package-manifest.json names %s' % name, out.getvalue())

    def test_exporters_refuse(self):
        """tools/export_modrinth.py and tools/export_server.py run the same tier gate as package.py (Task 9 review I2): on this
        checkout, while a troop draws a stopgap tier, each refuses before it reads a jar or writes into dist/."""
        import subprocess
        tools = pathlib.Path(build_skins.__file__).resolve().parent
        if not build_skins.stopgaps(build_skins.load(tools.parent)):
            self.skipTest('every troop has its own tiers: the exporters have nothing to refuse')
        for tool in ('export_modrinth', 'export_server'):
            with self.subTest(tool=tool):
                done = subprocess.run([sys.executable, str(tools / (tool + '.py'))], capture_output=True, text=True, cwd=tools.parent)
                self.assertEqual(done.returncode, 1, done.stdout + done.stderr)
                self.assertIn('%s: refused:' % tool, done.stdout)
                self.assertIn('python tools/%s.py --allow-stopgap' % tool, done.stdout)

    def test_release_gate(self):
        """package.py's check (round 34 question 6): with a stopgap it refuses, printing the list, unless --allow-stopgap is
        given, which marks the file name; with none it passes unmarked."""
        with tempfile.TemporaryDirectory() as tmp:
            root = self.tiers_root(tmp)
            out = io.StringIO()
            with contextlib.redirect_stdout(out), self.assertRaises(SystemExit) as caught:
                build_skins.release_gate(root, [])
            self.assertEqual(caught.exception.code, 1)
            self.assertIn('package: refused: 4 troops still draw a stopgap tier', out.getvalue())
            self.assertIn('hound (no model)', out.getvalue())
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.release_gate(root, ['--allow-stopgap']), '-stopgap')
        sheet = copy.deepcopy(SHEET)
        sheet['troops'] = [t for t in SHEET['troops'] if t['id'] == 'sentinel']
        troops = {'sentinel': dict(TROOPS['sentinel'], skins={'1': 'sentinel_s1.png', '2': 'sentinel_s2.png', '3': 'sentinel_s3.png'})}
        with tempfile.TemporaryDirectory() as tmp:
            root = fixture(tmp, troops=troops, sheet=sheet)
            for wid in ('sword_t2', 'sword_t3'):
                write_json(root / 'art/weapons' / (wid + '.json'), dict(SWORD, weapon=wid))
                skin(root / 'art/weapons' / (wid + '.png'), (16, 16), (150, 150, 160, 255))
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(build_skins.release_gate(root, []), '')
                self.assertEqual(build_skins.release_gate(root, ['--allow-stopgap']), '')

    def test_package_refuses(self):
        """tools/package.py on this checkout, while its troops are not all tier-complete, refuses before it reads a jar."""
        import subprocess
        tools = pathlib.Path(build_skins.__file__).resolve().parent
        if not build_skins.stopgaps(build_skins.load(tools.parent)):
            self.skipTest('every troop has its own tiers: package.py has nothing to refuse')
        done = subprocess.run([sys.executable, str(tools / 'package.py')], capture_output=True, text=True, cwd=tools.parent)
        self.assertEqual(done.returncode, 1, done.stdout + done.stderr)
        self.assertIn('package: refused:', done.stdout)
        self.assertIn('--allow-stopgap', done.stdout)

    def test_chevron(self):
        """art/marks/chevron.png builds to textures/entity/troop/chevron.png (the rank chevron, tinted by the side colour on the
        client): every opaque pixel grey, alpha 0 or 255, at most 32 px a side."""
        good = Image.new('RGBA', (11, 7), (0, 0, 0, 0))
        for x in range(11):
            good.putpixel((x, 3), (230, 230, 230, 255))
        good.putpixel((5, 0), (28, 28, 28, 255))
        with tempfile.TemporaryDirectory() as tmp:
            root = self.tiers_root(tmp, chevron=good)
            index = self.export(root)
            self.assertIn('textures/entity/troop/chevron.png', index['outputs'])
            built = Image.open(root / ASSETS / 'textures/entity/troop/chevron.png').convert('RGBA')
            self.assertEqual(list(built.getdata()), list(good.getdata()))
        for name, edit in (('is not grey', lambda im: im.putpixel((0, 3), (230, 200, 40, 255))),
                           ('alpha 128', lambda im: im.putpixel((1, 3), (230, 230, 230, 128))),
                           ('33x7', None)):
            im = good.copy() if edit else Image.new('RGBA', (33, 7), (0, 0, 0, 0))
            if edit:
                edit(im)
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                said = self.fails(export_assets, self.tiers_root(tmp, chevron=im))
                self.assertIn('chevron.png', said)
                self.assertIn(name, said)


if __name__ == '__main__':
    unittest.main(verbosity=2)
