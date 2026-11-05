"""Builds the Convoy supply wagon: the model (cubes, parts, box UVs) and its hand-ruled texture.

Writes src/main/resources/assets/gravesandglory/models/entity/supply_wagon.json, which WagonRenderer reads, and
textures/entity/supply_wagon.png. One texel per model pixel (docs/ART-STYLE-GUIDE.md, skins and relief). Model space is
vanilla entity space: y down, the ground at y 24, the wagon's front (the way it rolls) toward -z.

The look (Kort 2026-11-01, convoy spec section 4.2): a heavy Souls-like wooden supply wagon pushed by its escort, no oxen.
Plank bed with side boards, iron corner straps, a weathered canvas tilt stepped over three stained hoops, a dark opening at
each end, eight-spoke wheels with iron tyres, a push pole with a cross bar, a crate and a sack on the front deck and a keg on
the rear one. House-neutral: wood, stained wood, iron and undyed canvas only, no team colour, no gilt, no flags or pennants.

Painting follows the guide: each face at the tone its direction earns (up 4, front and lit side 3, back 2, far side 1,
underside 0), then one material pattern in the face tone +-1: wood grain and plank seams along the long axis, iron plate
edges and hammer marks on tones 1 to 3, canvas a two-texel weave check. Run: python tools/build_wagon.py
"""
import colorsys, json, pathlib
from PIL import Image

root = pathlib.Path(__file__).resolve().parents[1]
MODEL = root / 'src/main/resources/assets/gravesandglory/models/entity/supply_wagon.json'
TEXTURE = root / 'src/main/resources/assets/gravesandglory/textures/entity/supply_wagon.png'


def hexes(*h):
    return [tuple(int(x[i:i + 2], 16) for i in (1, 3, 5)) for x in h]


def ramp(base, step=0.22):
    """The guide's ramp (section 1): value +-step a tone, hue toward 268 in the darks and 52 in the lights, 6 degrees a step."""
    r, g, b = (c / 255 for c in base)
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    out = []
    for k in (-2, -1, 0, 1, 2):
        hue = h * 360
        target = 268 if k < 0 else 52
        for _ in range(abs(k)):
            d = (target - hue + 540) % 360 - 180
            hue += max(-6, min(6, d))
        val = max(0.06, min(1, v * (1 + step * k)))
        sat = max(0, s + (0.04 * -k if k < 0 else -0.07 * k))
        if val > 0.60:
            sat = min(sat, 0.44)
        rr, gg, bb = colorsys.hsv_to_rgb((hue % 360) / 360, min(1, sat), val)
        out.append((round(rr * 255), round(gg * 255), round(bb * 255)))
    return out


RAMPS = {
    'wood': hexes('#3E261F', '#563C2E', '#6E5440', '#867057', '#9E8E72'),
    'stained': hexes('#301B16', '#3E261F', '#563C2E', '#6E5440', '#867057'),
    'iron': hexes('#3A3E4B', '#5A616E', '#7C8591', '#A6ADB4', '#D6D6D7'),  # steel's ramp, iron paints on tones 1 to 3
    'canvas': ramp(hexes('#9F9278')[0], 0.16),  # undyed, weathered tilt cloth (pale cloth steps)
    'sack': ramp(hexes('#8C7B5E')[0], 0.16),
}
FACE_TONE = {'-y': 4, '-z': 3, '-x': 3, '+z': 2, '+x': 1, '+y': 0}

# ---- geometry -----------------------------------------------------------------------------------------------------------
parts = []  # {name, parent, pivot, rotation (degrees), cubes}


def part(name, parent='root', pivot=(0, 0, 0), rotation=(0, 0, 0)):
    p = {'name': name, 'parent': parent, 'pivot': list(pivot), 'rotation': list(rotation), 'cubes': []}
    parts.append(p)
    return p


def cube(p, name, frm, size, mat, faces=None, **style):
    p['cubes'].append({'name': name, 'from': list(frm), 'size': list(size), 'mat': mat, 'faces': faces or {}, 'style': style})


body = part('body')
cube(body, 'bed', (-12, 8, -22), (24, 3, 44), 'wood')
for side, x in (('l', -10), ('r', 7)):
    cube(body, 'beam_' + side, (x, 11, -23), (3, 2, 46), 'stained')
for end, z in (('f', -16), ('r', 13)):
    cube(body, 'bolster_' + end, (-11, 13, z), (22, 3, 3), 'stained')
for end, z in (('f', -15), ('r', 13)):
    cube(body, 'axle_' + end, (-13, 16, z), (26, 2, 2), 'stained')
cube(body, 'side_l', (-12, 2, -22), (2, 6, 44), 'wood')
cube(body, 'side_r', (10, 2, -22), (2, 6, 44), 'wood')
cube(body, 'board_f', (-10, 2, -22), (20, 6, 2), 'wood')
cube(body, 'board_r', (-10, 2, 20), (20, 6, 2), 'wood')
for i, z in enumerate((-21, -2, 17)):  # iron corner and middle straps, hand placed: not a fixed spacing
    cube(body, f'strap_l{i}', (-13, 2, z), (1, 9, 2), 'iron')
    cube(body, f'strap_r{i}', (12, 2, z), (1, 9, 2), 'iron')
cube(body, 'pole', (-1, 13, -38), (2, 2, 16), 'stained')
cube(body, 'crossbar', (-7, 12, -38), (14, 2, 2), 'stained', faces={'-x': 'iron', '+x': 'iron'})
cube(body, 'crate', (-8, 3, -20), (7, 5, 3), 'wood')
cube(body, 'sack', (1, 4, -20), (6, 4, 3), 'sack')
cube(body, 'keg', (-8, 1, 17), (6, 7, 3), 'wood', bands=(1, 5))

tilt = part('tilt')
TIERS = [((-11, -6), (22, 8)), ((-10, -10), (20, 4)), ((-8, -13), (16, 3)), ((-5, -15), (10, 2))]
for i, ((x, y), (w, h)) in enumerate(TIERS):
    above = TIERS[i + 1] if i + 1 < len(TIERS) else None
    span = (above[0][0] - x, above[0][0] - x + above[1][0]) if above else None
    cube(tilt, f'canvas_{i}', (x, y, -17), (w, h, 34), 'canvas', opening=span)
RIBS = [((-12, -7), (24, 9)), ((-11, -11), (22, 5)), ((-9, -14), (18, 4)), ((-6, -16), (12, 3))]
for j, z in enumerate((-11, -1, 9)):
    for i, ((x, y), (w, h)) in enumerate(RIBS):
        cube(tilt, f'rib_{j}_{i}', (x, y, z), (w, h, 2), 'stained')


def wheel(name, x, z):
    p = part(name, pivot=(x, 17, z))
    diag = part(name + '_diag', parent=name, rotation=(45, 0, 0))
    cube(p, 'hub', (-2, -2, -2), (4, 4, 4), 'iron', hub=True)
    for q in (p, diag):
        cube(q, 'spoke_v', (-1, -6, -1), (2, 12, 2), 'wood')
        cube(q, 'spoke_h', (-1, -1, -6), (2, 2, 12), 'wood')
        cube(q, 'rim_t', (-1, -7, -3), (2, 2, 6), 'stained', faces={'-y': 'iron'})
        cube(q, 'rim_b', (-1, 5, -3), (2, 2, 6), 'stained', faces={'+y': 'iron'})
        cube(q, 'rim_f', (-1, -3, -7), (2, 6, 2), 'stained', faces={'-z': 'iron'})
        cube(q, 'rim_k', (-1, -3, 5), (2, 6, 2), 'stained', faces={'+z': 'iron'})


wheel('wheel_fl', -13, -14)
wheel('wheel_fr', 13, -14)
wheel('wheel_rl', -13, 14)
wheel('wheel_rr', 13, 14)

# ---- box UV packing (shelf) ----------------------------------------------------------------------------------------------
W = 256
cubes = [c for p in parts for c in p['cubes']]
order = sorted(cubes, key=lambda c: -(c['size'][2] + c['size'][1]))
u = v = shelf = 0
for c in order:
    w, h, d = c['size']
    fw, fh = 2 * d + 2 * w, d + h
    if u + fw > W:
        u, v, shelf = 0, v + shelf, 0
    c['uv'] = [u, v]
    u += fw
    shelf = max(shelf, fh)
H = 1
while H < v + shelf:
    H *= 2

# ---- painting ------------------------------------------------------------------------------------------------------------
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
px = img.load()


def faces_of(c):
    """Each face: direction, texture origin, width and height in texels (vanilla ModelPart.Cuboid layout)."""
    (u, v), (w, h, d) = c['uv'], c['size']
    return [('-y', u + d, v, w, d), ('+y', u + d + w, v, w, d), ('-x', u, v + d, d, h),
            ('-z', u + d, v + d, w, h), ('+x', u + d + w, v + d, d, h), ('+z', u + 2 * d + w, v + d, w, h)]


def mark(seed, a, b, density):
    """A sparse, irregular hand mark: a fixed hash, so the texture rebuilds the same, never on a fixed spacing."""
    n = (seed * 73856093 ^ a * 19349663 ^ b * 83492791) & 0xffffffff
    return (n % 1000) < density


SEAMS = (3, 7, 10, 14, 19, 22)  # plank seams across a wide face, at a board's own widths
for ci, c in enumerate(cubes):
    for dirn, ox, oy, fw, fh in faces_of(c):
        mat = c['faces'].get(dirn, c['mat'])
        ramp_ = RAMPS[mat]
        t = FACE_TONE[dirn]
        if mat == 'iron':
            t = max(1, min(3, t))
        long_rows = fw >= fh  # the long axis runs along the texture's rows
        short = min(fw, fh)
        for y in range(fh):
            for x in range(fw):
                k = t
                if mat in ('wood', 'stained'):
                    across = y if long_rows else x
                    if short >= 4 and across in SEAMS:
                        k = t - 1
                    elif 2 <= short < 4 and across == short - 1:
                        k = t - 1
                    if dirn in ('-z', '-x', '+z', '+x') and fh >= 4:
                        if y == 0 and dirn in ('-z', '-x'):
                            k = t + 1  # the lit top edge, one texel
                        elif y == fh - 1:
                            k = t - 1  # road grime at the foot
                    if c['style'].get('bands') and dirn not in ('-y', '+y') and y in c['style']['bands']:
                        k = max(1, min(3, t)) - (0 if y % 2 else 1)
                        ramp_k = RAMPS['iron']
                        px[ox + x, oy + y] = ramp_k[max(0, min(4, k))] + (255,)
                        continue
                elif mat == 'iron':
                    if fw >= 3 and fh >= 3:
                        if y == 0 or x == 0:
                            k = min(3, t + 1)
                        elif y == fh - 1 or x == fw - 1:
                            k = max(1, t - 1)
                        elif mark(ci * 7 + len(dirn), x, y, 90):
                            k = max(1, t - 1)
                        if c['style'].get('hub') and dirn in ('-x', '+x') and 1 <= x <= 2 and 1 <= y <= 2:
                            k = max(1, t - 1)
                elif mat in ('canvas', 'sack'):
                    if fw >= 4 and fh >= 4 and ((x // 2) + (y // 2)) % 2:
                        k = t - 1 if t > 0 else 1
                    if dirn in ('-z', '+z') and 'opening' in c['style']:
                        span = c['style']['opening']
                        edge = x == 0 or x == fw - 1 or (y == 0 and (span is None or not (span[0] <= x < span[1])))
                        if not edge:
                            k = 0
                            ramp_ = RAMPS['stained']  # the dark inside under the tilt
                        else:
                            ramp_ = RAMPS[mat]
                    elif mat == 'sack' and mark(ci, x, y, 60):
                        k = max(0, k - 1)
                px[ox + x, oy + y] = ramp_[max(0, min(4, k))] + (255,)
                ramp_ = RAMPS[mat]

TEXTURE.parent.mkdir(parents=True, exist_ok=True)
img.save(TEXTURE)
MODEL.parent.mkdir(parents=True, exist_ok=True)
model = {'comment': 'Generated by tools/build_wagon.py; edit that, not this file.', 'texture': [W, H],
         'parts': [{'name': p['name'], 'parent': p['parent'], 'pivot': p['pivot'], 'rotation': p['rotation'],
                    'cubes': [{'name': c['name'], 'uv': c['uv'], 'from': c['from'], 'size': c['size']} for c in p['cubes']]}
                   for p in parts]}
MODEL.write_text(json.dumps(model, indent=1) + '\n', encoding='utf-8')
print(f'supply wagon: {len(cubes)} cubes in {len(parts)} parts, texture {W}x{H}')
