"""Generate the art sources of the three House troops (ic_hearthguard, wm_tusk, tc_tideguard) and their nine weapon tiers.

Writes art/troops/<troop>.json, the new art/parts/*.json, the skins _s1.._s3, the _veteran trim and the _faces patches, and
art/weapons/<weapon>[_t2|_t3].{bbmodel,json,png,_m.png}. Deterministic: run it, then `python tools/build_skins.py`.
The painting is procedural (flat Minecraft-style colour, shaded by face, small per-material patterns); the shapes follow
docs/superpowers/specs/2026-10-17-unit-identity.md (Hearthguard, Tusk Spear, Trident Guard)."""
import base64
import colorsys
import copy
import io
import json
import random
import sys
import uuid
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / 'art'
D = 3            # troop texels a px
FD = 2           # face patch texels a px
WD = 2           # weapon texels a px
SIZE = (128, 64)
SLOTS = [[96, 48], [64, 48], [96, 32]]
SHADE = {'top': 1.18, 'bottom': 0.6, 'north': 1.0, 'south': 0.72, 'west': 0.86, 'east': 0.86}


def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def safe(c):
    """Clamp a colour under the team tint rule (no saturation over 0.44 with value over 0.6)."""
    r, g, b = (max(0, min(255, int(round(x)))) for x in c)
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if v > 0.6 and s > 0.42:
        s = 0.42
        r, g, b = (int(round(x * 255)) for x in colorsys.hsv_to_rgb(h, s, v))
    return (r, g, b)


def mul(c, k):
    return tuple(x * k for x in c)


def mix(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


# ---------------------------------------------------------------- parts

def box(origin, size, note=''):
    return {'origin': origin, 'size': size, 'note': note}


PARTS = {
    # Hearthguard
    'box_helm': ('head', True, 'The Hearthguard box helm: a square soot-iron crown, a back plate and two cheek plates standing 1 px off the face, '
                 'a brow bar and a short nasal; the eyes and the beard show through the opening.', [
        box([-5, -9.5, -5], [10, 2, 10], 'crown, 1 px proud all round'),
        box([-5, -7.5, 3], [10, 7, 2], 'back plate'),
        box([-5, -7.5, -5], [2, 7, 8], 'right cheek plate, 1 px before the face'),
        box([3, -7.5, -5], [2, 7, 8], 'left cheek plate'),
        box([-3, -7.5, -5], [6, 1, 1], 'brow bar'),
        box([-0.5, -6.5, -5.5], [1, 3, 1], 'nasal'),
    ]),
    'braided_beard': ('head', False, 'The Hearthguard copper beard: a square beard under the helm and one thick braid to the belt, ringed in brass.', [
        box([-3, -2.5, -5], [6, 4, 2], 'beard'),
        box([-1, 1.5, -5], [2, 8, 2], 'the braid, before the chest'),
        box([-1.5, 6, -5.5], [3, 1, 3], 'brass braid ring'),
    ]),
    'slab_pauldrons': ('body', False, 'The Hearthguard mark: two flat soot-iron slabs on the shoulders, wider than the arms, a lame under each.', [
        box([-8.5, -13.5, -3], [5, 3, 6], 'right slab'),
        box([3.5, -13.5, -3], [5, 3, 6], 'left slab'),
        box([-8, -10.5, -2.5], [3, 2, 5], 'right lame'),
        box([5, -10.5, -2.5], [3, 2, 5], 'left lame'),
    ]),
    'scale_coat_skirt': ('body', False, 'The knee-length skirt of the Hearthguard scale coat.', [
        box([-4.5, 0, -2.5], [9, 6, 5], 'skirt to the knee'),
    ]),
    'helm_horns': ('head', True, 'Hearthguard stage 3: two short horns from the crown of the box helm.', [
        box([-7, -10.5, -1], [2, 2, 2], 'right horn root'),
        box([-8, -13.5, -0.5], [1, 3, 1], 'right horn tip'),
        box([5, -10.5, -1], [2, 2, 2], 'left horn root'),
        box([7, -13.5, -0.5], [1, 3, 1], 'left horn tip'),
    ]),
    # Tusk Spear
    'boar_snout': ('head', False, 'The Tusk Spear boar snout before the face, with the nose disc.', [
        box([-2.5, -4, -6], [5, 4, 2], 'snout'),
        box([-1.5, -3.5, -7], [3, 2, 1], 'nose disc'),
    ]),
    'tusks': ('head', False, 'The big white tusks rising beside the snout.', [
        box([-4, -5.5, -6.5], [1, 4, 1], 'right tusk'),
        box([-4.5, -6.5, -7], [1, 1, 1], 'right tusk tip'),
        box([3, -5.5, -6.5], [1, 4, 1], 'left tusk'),
        box([3.5, -6.5, -7], [1, 1, 1], 'left tusk tip'),
    ]),
    'leather_cap': ('head', True, 'The Tusk Spear leather cap and the boar ears through it.', [
        box([-4.5, -8.5, -4.5], [9, 2, 9], 'cap'),
        box([-5.5, -10, 0], [2, 2, 1], 'right ear'),
        box([3.5, -10, 0], [2, 2, 1], 'left ear'),
    ]),
    'hide_mantle': ('body', False, 'The Tusk Spear hide mantle: a shaggy roll over the shoulders, a bristled hump and a hide flap down the back.', [
        box([-5, -12.5, -3], [10, 3, 6], 'shoulder roll'),
        box([-1.5, -14, -0.5], [3, 2, 4], 'bristle hump'),
        box([-4.5, -9.5, 2.5], [9, 7, 1], 'back flap'),
    ]),
    'tusk_crown': ('head', True, 'Tusk Spear stage 3: a crown of boar tusks on the cap.', [
        box([-3.5, -11.5, -3], [1, 3, 1], 'right tusk'),
        box([-0.5, -12.5, -3.5], [1, 4, 1], 'middle tusk'),
        box([2.5, -11.5, -3], [1, 3, 1], 'left tusk'),
    ]),
    # Trident Guard
    'finned_cap': ('head', True, 'The Trident Guard finned iron cap: a skull cap, a dorsal fin and two fin ear guards.', [
        box([-4.5, -8.5, -4.5], [9, 3, 9], 'cap'),
        box([-0.5, -11.5, -3], [1, 3, 7], 'dorsal fin'),
        box([-5.5, -7, -1], [1, 4, 3], 'right fin guard'),
        box([4.5, -7, -1], [1, 4, 3], 'left fin guard'),
    ]),
    'coral_pauldrons': ('body', False, 'The Trident Guard mark: bleached coral rising from both shoulders.', [
        box([-8, -13, -2], [4, 1, 4], 'right coral seat'),
        box([-7, -18, -0.5], [1, 5, 1], 'right trunk'),
        box([-9, -16, -0.5], [2, 1, 1], 'right twig'),
        box([-9, -18, -0.5], [1, 2, 1], 'right twig tip'),
        box([-5.5, -16, 0.5], [1, 3, 1], 'right second branch'),
        box([4, -13, -2], [4, 1, 4], 'left coral seat'),
        box([6, -18, -0.5], [1, 5, 1], 'left trunk'),
        box([7, -16, -0.5], [2, 1, 1], 'left twig'),
        box([8, -18, -0.5], [1, 2, 1], 'left twig tip'),
        box([4.5, -16, 0.5], [1, 3, 1], 'left second branch'),
    ]),
    'scallop_shell': ('body', False, 'The scallop shell on the Trident Guard back.', [
        box([-3.5, -11, 2], [7, 6, 1], 'shell'),
        box([-1.5, -5, 2], [3, 1, 1], 'hinge'),
    ]),
    'coral_crown': ('head', True, 'Trident Guard stage 3: coral sprigs on the finned cap.', [
        box([-3.5, -12, -2.5], [1, 3, 1], 'right sprig'),
        box([-4.5, -13, -2.5], [1, 2, 1], 'right sprig twig'),
        box([2.5, -12, -2.5], [1, 3, 1], 'left sprig'),
        box([3.5, -13, -2.5], [1, 2, 1], 'left sprig twig'),
    ]),
}

# ---------------------------------------------------------------- troops

TROOPS = {
    'ic_hearthguard': {
        'note': 'Ironclad Hearthguard (unit identity, Ironclad): vindicator, biped, broad and short; the square box helm with cheek plates, '
                'the slab pauldrons (the mark), arms folded at rest, a knee-length soot-iron scale coat under the team pale, the copper '
                'beard braided to the belt; stage 3 helm horns. Hearth axe, the shield on the left arm.',
        'bones': {'body': {'scale': [1.2, 0.95, 1.15]}, 'right_arm': {'scale': [1.1, 0.9, 1.1]}, 'left_arm': {'scale': [1.1, 0.9, 1.1]},
                  'right_leg': {'scale': [1.1, 0.8, 1.1]}, 'left_leg': {'scale': [1.1, 0.8, 1.1]}},
        'idleArms': 'folded',
        'parts': ['box_helm', 'braided_beard', 'slab_pauldrons', 'scale_coat_skirt', ('helm_horns', [3])],
        'team': [{'part': 'chest_panel'}, {'part': 'pennant', 'scale': [0.833333, 1.052632, 0.869565], 'offset': [0, 0.6316, 0.8696]}],
        'weapon': 'hearth_axe', 'portrait': 'standard',
    },
    'wm_tusk': {
        'note': 'Wildmoot Tusk Spear (unit identity, Wildmoot): zombified piglin, biped, hunched; boar-kin with a snout and big white tusks, '
                'a leather cap, a shaggy hide mantle over the team jerkin, the pennant; stage 3 tusk crown. The crossbarred boar spear '
                'stands with its head over the cap (the mark).',
        'bones': {},
        'hunch': 20,
        'idleArms': 'hang',
        'parts': ['boar_snout', 'tusks', 'leather_cap', 'hide_mantle', ('tusk_crown', [3])],
        'team': [{'part': 'jerkin'}, {'part': 'pennant'}],
        'weapon': 'boar_spear', 'portrait': 'tall 4',
    },
    'tc_tideguard': {
        'note': 'Tidecallers Trident Guard (unit identity, Tidecallers): drowned, biped, upright and long-legged; bleached coral rising '
                'from both shoulders (the mark), barnacled mail, the scallop shell on the back, the finned iron cap, the team pale as a '
                'sash, the pennant; stage 3 coral crown. Coral trident.',
        'bones': {'right_leg': {'scale': [1, 1.05, 1]}, 'left_leg': {'scale': [1, 1.05, 1]}},
        'idleArms': 'hang',
        'parts': ['finned_cap', 'coral_pauldrons', 'scallop_shell', ('coral_crown', [3])],
        'team': [{'part': 'chest_panel'}, {'part': 'pennant', 'offset': [0, 0, 0.5]}],
        'weapon': 'coral_trident', 'portrait': 'standard',
    },
}


def rig_rects():
    rig = json.loads((ART / 'rigs/biped.json').read_text(encoding='utf-8'))
    out = []
    for b in rig['bones']:
        for x in b['boxes']:
            out += footprint(x['uv'], x['size'])
    return out


def footprint(uv, size):
    (u, v), (w, h, d) = uv, size
    return [r for r in ((u + d, v, u + d + 2 * w, v + d), (u, v + d, u + 2 * d + 2 * w, v + d + h)) if r[0] < r[2] and r[1] < r[3]]


class Packer:
    def __init__(self, rects):
        self.grid = [[False] * SIZE[0] for _ in range(SIZE[1])]
        for r in rects:
            self.mark(r)

    def mark(self, r):
        x0, y0, x1, y1 = r
        for y in range(max(0, y0), min(SIZE[1], y1)):
            for x in range(max(0, x0), min(SIZE[0], x1)):
                self.grid[y][x] = True

    def free(self, r):
        x0, y0, x1, y1 = r
        if x0 < 0 or y0 < 0 or x1 > SIZE[0] or y1 > SIZE[1]:
            return False
        return not any(self.grid[y][x] for y in range(y0, y1) for x in range(x0, x1))

    def place(self, size):
        w, h, d = size
        for v in range(0, SIZE[1]):
            for u in range(0, SIZE[0]):
                rs = footprint((u, v), size)
                # the whole block (u..u+2d+2w, v..v+d+h) kept free so neighbours never touch a face
                block = (u, v, u + 2 * (d + w), v + d + h)
                if self.free(block):
                    self.mark(block)
                    return [u, v]
        raise SystemExit('no room on the sheet for a %s box' % (size,))


def write_json(path, obj):
    path.write_text(json.dumps(obj, indent=1) + '\n', encoding='utf-8')


def build_sources():
    """The troop and part jsons, with each part's boxes packed on the troop sheet. Returns {troop: {(part, i): uv}}."""
    layout = {}
    for troop, t in TROOPS.items():
        reserve = rig_rects() + [(0, 0, 32, 16)] + [(x, y, x + 22, y + 11) for x, y in SLOTS]
        pk = Packer(reserve)
        entries = []
        boxes_uv = {}
        names = [p if isinstance(p, str) else p[0] for p in t['parts']]
        order = sorted(((n, i, b) for n in names for i, b in enumerate(PARTS[n][3])),
                       key=lambda e: -(2 * (e[2]['size'][0] + e[2]['size'][2])) * (e[2]['size'][2] + e[2]['size'][1]))
        for n, i, b in order:
            boxes_uv[(n, i)] = pk.place(b['size'])
        for p in t['parts']:
            n, stages = (p, None) if isinstance(p, str) else p
            bone, helm, note, bxs = PARTS[n]
            uvs = [boxes_uv[(n, i)] for i in range(len(bxs))]
            u0, v0 = min(u for u, _ in uvs), min(v for _, v in uvs)
            write_json(ART / 'parts' / (n + '.json'), {
                'part': n, 'note': note, 'bone': bone, 'team': False, 'tag': 'helm' if helm else '',
                'boxes': [{'origin': b['origin'], 'size': b['size'], 'uv': [u - u0, v - v0], 'note': b['note']} for b, (u, v) in zip(bxs, uvs)]})
            e = {'part': n, 'uv': [u0, v0]}
            if stages:
                e['stages'] = stages
            entries.append(e)
        src = {'troop': troop, 'note': t['note'], 'rig': 'biped', 'size': list(SIZE), 'density': D}
        if t['bones']:
            src['bones'] = t['bones']
        if 'hunch' in t:
            src['hunch'] = t['hunch']
        src.update({'idleArms': t['idleArms'], 'parts': entries + t['team'],
                    'skins': {str(s): '%s_s%d.png' % (troop, s) for s in (1, 2, 3)},
                    'weapon': t['weapon'], 'hand': 'right', 'portrait': t['portrait'],
                    'faces': {'density': FD, 'count': 4, 'slots': SLOTS}})
        write_json(ART / 'troops' / (troop + '.json'), src)
        layout[troop] = boxes_uv
    return layout


# ---------------------------------------------------------------- skin painting

def face_rects(uv, size, k):
    (u, v), (w, h, d) = uv, size
    return {n: tuple(k * c for c in r) for n, r in {
        'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
        'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
        'east': (u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}.items()}


class Ctx:
    """What a material sees of one texel: its face, its place in the face (x, y texels, w, h the face's texels), the stage."""
    __slots__ = ('face', 'x', 'y', 'w', 'h', 'stage', 'rng', 'note')


def noise(ctx, base, amt=0.06):
    return mul(base, 1 + (ctx.rng.random() - 0.5) * 2 * amt)


def edge(ctx, n=1):
    return ctx.x < n or ctx.y < n or ctx.x >= ctx.w - n or ctx.y >= ctx.h - n


# palettes
SOOT, SOOT_HI = hexc('34373B'), hexc('5A5E63')
BRASS = hexc('7F6232')
BRASS_HI = hexc('A88A55')
COPPER = hexc('7A4A2E')
SCORCH = hexc('4B3A2C')
RUSSET, BARK, CLAY = hexc('7A4127'), hexc('4A3626'), hexc('C8B48A')
VERD, BARN, NAVY = hexc('4E7A6A'), hexc('CFCBBE'), hexc('1E2C44')
BONE = hexc('E4DECB')
CORAL = hexc('DCD2C2')


def m_scales(base, hi, rim=None):
    def f(ctx):
        row = ctx.y // 3
        off = 1 if row % 2 else 0
        cx = (ctx.x + off) % 3
        c = hi if ctx.y % 3 == 0 else base
        if ctx.y % 3 == 2 or cx == 2:
            c = mul(base, 0.72)
        if rim and edge(ctx):
            c = rim
        return noise(ctx, c, 0.05)
    return f


def m_plate(base, rim=None, rivets=None):
    def f(ctx):
        c = base
        if edge(ctx):
            c = rim if rim else mul(base, 0.75)
        elif rivets and ctx.y in (2,) and ctx.x % 5 == 2:
            c = rivets
        elif ctx.y == 1:
            c = mul(base, 1.2)
        return noise(ctx, c, 0.04)
    return f


def m_mail(base, barn=0.0):
    def f(ctx):
        c = base if (ctx.x + ctx.y) % 2 else mul(base, 1.25)
        if ctx.y % 3 == 2:
            c = mul(base, 0.7)
        if barn and ctx.rng.random() < barn:
            c = mix(BARN, mul(BARN, 0.8), ctx.rng.random())
        return noise(ctx, c, 0.05)
    return f


def m_cloth(base, stitch=None):
    def f(ctx):
        c = base
        if stitch and (ctx.x == 1 or ctx.x == ctx.w - 2) and ctx.y % 2 == 0:
            c = stitch
        return noise(ctx, c, 0.08)
    return f


def m_fur(base, dark):
    def f(ctx):
        k = (ctx.x * 7 + (ctx.y // 2) * 3) % 5
        c = mix(base, dark, 0.15 * k)
        if ctx.rng.random() < 0.12:
            c = mul(c, 1.18)
        return noise(ctx, c, 0.05)
    return f


def m_hair(base):
    def f(ctx):
        c = base if (ctx.x + ctx.y // 2) % 3 else mul(base, 0.7)
        if (ctx.x + ctx.y) % 4 == 0:
            c = mul(base, 1.2)
        return noise(ctx, c, 0.04)
    return f


def m_bone(base, tip=None, tipfrom=0.0):
    def f(ctx):
        c = base
        if tip is not None and ctx.y < ctx.h * tipfrom:
            c = tip
        if ctx.x == 0:
            c = mul(c, 0.85)
        return noise(ctx, c, 0.04)
    return f


def m_coral(ctx):
    c = CORAL
    if ctx.rng.random() < 0.25:
        c = mul(CORAL, 0.82)
    if (ctx.x + 2 * ctx.y) % 5 == 0:
        c = mul(CORAL, 0.9)
    return noise(ctx, c, 0.03)


def m_shell(ctx):
    rib = (ctx.x * 2 // max(1, ctx.w // 7)) % 2
    c = BARN if rib else mul(BARN, 0.82)
    if ctx.face == 'north' or ctx.face == 'south':
        if ctx.y < 2 and (ctx.x < ctx.w // 4 or ctx.x >= ctx.w * 3 // 4):
            c = mul(c, 0.8)
    return noise(ctx, c, 0.04)


def m_band(top_m, rows, band_c, fill_m=None):
    """A material with a band (belt, boot) over a row range given as fractions of the face height."""
    lo, hi = rows

    def f(ctx):
        if ctx.face in ('top', 'bottom'):
            return top_m(ctx)
        t = ctx.y / ctx.h
        if lo <= t < hi:
            c = band_c(ctx) if callable(band_c) else band_c
            return noise(ctx, c, 0.04)
        return top_m(ctx)
    return f


def m_limb(upper, lower, split):
    def f(ctx):
        if ctx.face == 'top':
            return upper(ctx)
        if ctx.face == 'bottom':
            return lower(ctx)
        return upper(ctx) if ctx.y < ctx.h * split else lower(ctx)
    return f


def solid(c):
    return lambda ctx: noise(ctx, c, 0.05)


def hearthguard_mats(stage):
    coat = m_scales(SCORCH, mul(SCORCH, 1.3)) if stage == 1 else m_scales(SOOT, SOOT_HI, BRASS if stage == 3 else None)
    helm_rim = BRASS if stage >= 2 else None
    belt = lambda ctx: BRASS_HI if ctx.face == 'north' and abs(ctx.x - ctx.w // 2) <= 1 else SCORCH
    arm_up = m_plate(SOOT if stage >= 2 else SCORCH, BRASS if stage == 3 else None)
    glove = m_cloth(mul(SCORCH, 0.85))
    sleeve = m_mail(SOOT) if stage >= 2 else m_cloth(mul(SCORCH, 1.1))
    return {
        'body': m_band(coat, (0.8, 0.92), belt),
        'right_arm': m_limb(arm_up, m_limb(sleeve, glove, 0.6), 0.33),
        'left_arm': m_limb(arm_up, m_limb(sleeve, glove, 0.6), 0.33),
        'arms_folded': m_limb(sleeve, glove, 0.5),
        'right_leg': m_limb(m_cloth(mul(SCORCH, 0.9)), m_plate(SOOT, BRASS if stage == 3 else None), 0.55),
        'left_leg': m_limb(m_cloth(mul(SCORCH, 0.9)), m_plate(SOOT, BRASS if stage == 3 else None), 0.55),
        'box_helm': m_plate(SOOT, helm_rim, BRASS if stage >= 2 else mul(SOOT, 1.4)),
        'braided_beard': lambda ctx: (noise(ctx, BRASS_HI, 0.03) if ctx.note == 'brass braid ring' else m_hair(COPPER)(ctx)),
        'slab_pauldrons': m_plate(SOOT, BRASS if stage >= 2 else mul(SOOT, 0.7), BRASS_HI if stage >= 2 else SOOT_HI),
        'scale_coat_skirt': coat,
        'helm_horns': m_bone(hexc('BDB29A'), hexc('E8E2CF'), 0.4),
    }


def tusk_mats(stage):
    fur = m_fur(RUSSET, BARK)
    wrap = m_cloth(BARK, mul(CLAY, 0.8))
    bracer = wrap if stage == 1 else (m_cloth(mul(BARK, 0.8), CLAY) if stage == 2 else m_plate(hexc('4C4A48'), BARK))
    hoof = solid(hexc('2E2420'))
    return {
        'body': m_band(fur, (0.82, 0.94), lambda ctx: CLAY if ctx.x % 4 == 0 else BARK),
        'right_arm': m_limb(fur, m_limb(bracer, fur, 0.75), 0.5),
        'left_arm': m_limb(fur, m_limb(bracer, fur, 0.75), 0.5),
        'arms_folded': fur,
        'right_leg': m_limb(m_cloth(mul(BARK, 1.1), None), m_limb(fur, hoof, 0.75), 0.45),
        'left_leg': m_limb(m_cloth(mul(BARK, 1.1), None), m_limb(fur, hoof, 0.75), 0.45),
        'boar_snout': lambda ctx: (noise(ctx, hexc('3A2620') if ctx.note == 'nose disc' and ctx.face == 'north' and
                                         (ctx.x in (ctx.w // 4, ctx.w * 3 // 4 - 1)) and ctx.h // 3 <= ctx.y < ctx.h * 2 // 3
                                         else (hexc('7E5A4A') if ctx.note == 'nose disc' else hexc('8A5E46')), 0.04)),
        'tusks': m_bone(BONE),
        'leather_cap': lambda ctx: (fur(ctx) if 'ear' in ctx.note else
                                    (noise(ctx, hexc('5E5A55'), 0.03) if stage == 3 and ctx.face != 'bottom' and ctx.x % 5 == 2 and ctx.y % 4 == 1
                                     else m_cloth(BARK, CLAY if stage >= 2 else None)(ctx))),
        'hide_mantle': m_fur(mix(RUSSET, BARK, 0.6), mul(BARK, 0.7)),
        'tusk_crown': m_bone(BONE, mul(BONE, 0.85), 0.25),
    }


def tide_mats(stage):
    mail = m_cloth(mix(NAVY, VERD, 0.2), None) if stage == 1 else m_mail(mix(NAVY, VERD, 0.35), 0.06 if stage == 2 else 0.1)
    bronze = m_plate(VERD, mul(VERD, 0.7) if stage < 3 else BARN)
    skin = solid(hexc('6F8A80'))
    return {
        'body': m_band(mail, (0.82, 0.94), lambda ctx: VERD if ctx.face != 'north' or abs(ctx.x - ctx.w // 2) > 1 else BARN),
        'right_arm': m_limb(bronze if stage >= 2 else mail, m_limb(mail, skin, 0.75), 0.33),
        'left_arm': m_limb(bronze if stage >= 2 else mail, m_limb(mail, skin, 0.75), 0.33),
        'arms_folded': mail,
        'right_leg': m_limb(m_cloth(NAVY), bronze if stage >= 2 else m_cloth(mul(NAVY, 0.8)), 0.5),
        'left_leg': m_limb(m_cloth(NAVY), bronze if stage >= 2 else m_cloth(mul(NAVY, 0.8)), 0.5),
        'finned_cap': lambda ctx: (m_fin(ctx) if 'fin' in ctx.note else m_plate(VERD, mul(VERD, 0.7), BARN if stage >= 2 else None)(ctx)),
        'coral_pauldrons': m_coral,
        'scallop_shell': m_shell,
        'coral_crown': m_coral,
    }


def m_fin(ctx):
    c = mix(VERD, NAVY, 0.5) if (ctx.x // 2) % 2 else mix(VERD, BARN, 0.25)
    return noise(ctx, c, 0.04)


MATS = {'ic_hearthguard': hearthguard_mats, 'wm_tusk': tusk_mats, 'tc_tideguard': tide_mats}


def baked_bones(troop):
    sys.path.insert(0, str(ROOT / 'tools'))
    import build_skins as B
    cat = B.load(B.ROOT)
    return B.bake(cat, troop)


def paint_skins(troop):
    model = baked_bones(troop)
    notes = {}
    for p, (bone, helm, note, bxs) in PARTS.items():
        notes[p] = [b['note'] for b in bxs]
    skins = []
    for stage in (1, 2, 3):
        mats = MATS[troop](stage)
        im = Image.new('RGBA', (SIZE[0] * D, SIZE[1] * D), (0, 0, 0, 0))
        px = im.load()
        rng = random.Random('%s-%d' % (troop, stage))
        for b in model['bones']:
            if b['team'] or not b['boxes'] or b.get('variants'):
                continue
            key = b['part'] or b['name']
            mat = mats.get(key)
            if mat is None:
                raise SystemExit('%s: no material for bone %s' % (troop, key))
            for bi, x in enumerate(b['boxes']):
                ctx = Ctx()
                ctx.stage, ctx.rng = stage, rng
                ctx.note = notes.get(b['part'], [''] * 99)[bi] if b['part'] else ''
                for fname, (x0, y0, x1, y1) in face_rects(x['uv'], x['size'], D).items():
                    ctx.face, ctx.w, ctx.h = fname, x1 - x0, y1 - y0
                    for yy in range(y0, y1):
                        for xx in range(x0, x1):
                            ctx.x, ctx.y = xx - x0, yy - y0
                            c = mat(ctx)
                            px[xx, yy] = safe(mul(c, SHADE[fname])) + (255,)
        im.save(ART / 'troops' / ('%s_s%d.png' % (troop, stage)))
        skins.append(im)
    return model, skins


def paint_veteran(troop, model, skins):
    """Brass trim on the rims of the helm and the mark part's front faces, over texels every skin paints."""
    keys = {'ic_hearthguard': ('slab_pauldrons', 'box_helm'), 'wm_tusk': ('leather_cap', 'hide_mantle'),
            'tc_tideguard': ('finned_cap', 'scallop_shell')}[troop]
    trim = {'ic_hearthguard': BRASS_HI, 'wm_tusk': CLAY, 'tc_tideguard': hexc('B7A777')}[troop]
    im = Image.new('RGBA', skins[0].size, (0, 0, 0, 0))
    px = im.load()
    for b in model['bones']:
        if b['part'] not in keys:
            continue
        for x in b['boxes']:
            for fname, (x0, y0, x1, y1) in face_rects(x['uv'], x['size'], D).items():
                if fname not in ('north', 'west', 'east'):
                    continue
                for xx in range(x0, x1):
                    for yy in (y0,):
                        px[xx, yy] = safe(mul(trim, SHADE[fname])) + (255,)
    opaque = [s.getchannel('A') for s in skins]
    for y in range(im.height):
        for xx in range(im.width):
            if px[xx, y][3] and not all(o.getpixel((xx, y)) for o in opaque):
                px[xx, y] = (0, 0, 0, 0)
    im.save(ART / 'troops' / ('%s_veteran.png' % troop))


# ---------------------------------------------------------------- faces

FACE_SETS = {
    # skin, hair/brow, eye, extra (beard / tusk-fur / gill)
    'ic_hearthguard': [(hexc('B07E62'), COPPER, hexc('2A3440')), (hexc('9C6A50'), hexc('4A3226'), hexc('3A2A20')),
                       (hexc('B88E74'), hexc('8E8A84'), hexc('2A3440')), (hexc('A27258'), hexc('8A4A2A'), hexc('34402A'))],
    'wm_tusk': [(RUSSET, BARK, hexc('1E1410')), (mix(RUSSET, BARK, 0.4), mul(BARK, 0.8), hexc('3A1E10')),
                (mix(RUSSET, CLAY, 0.2), BARK, hexc('1E1410')), (mul(RUSSET, 0.85), hexc('2E2420'), hexc('4A2A10'))],
    'tc_tideguard': [(hexc('6F8A80'), NAVY, hexc('C8D4C0')), (hexc('7C8E86'), hexc('2A3A3A'), hexc('101820')),
                     (hexc('64807A'), mix(NAVY, VERD, 0.4), hexc('D0C890')), (hexc('829A8C'), NAVY, hexc('101820'))],
}


def paint_faces(troop):
    im = Image.new('RGBA', (8 * 4 * FD // 1 * 1, 4 * 16 * FD), (0, 0, 0, 0))   # 64 x 128
    px = im.load()
    for n, (skin, hair, eye) in enumerate(FACE_SETS[troop]):
        rng = random.Random('%s-face-%d' % (troop, n))
        oy = n * 16 * FD
        for fname, (x0, y0, x1, y1) in face_rects((0, 0), (8, 8, 8), FD).items():
            w, h = x1 - x0, y1 - y0
            for yy in range(y0, y1):
                for xx in range(x0, x1):
                    fx, fy = (xx - x0) // FD, (yy - y0) // FD   # face px 0..7
                    c = skin
                    if fname == 'top' or (fname in ('south',) and fy < 6) or (fname in ('west', 'east') and fy < 2):
                        c = hair
                    if fname == 'north':
                        c = face_px(troop, n, fx, fy, skin, hair, eye)
                    if fname in ('west', 'east') and troop == 'tc_tideguard' and fy in (4, 5) and fx in (2, 4):
                        c = mul(skin, 0.6)   # gill slits
                    if fname in ('west', 'east') and troop == 'ic_hearthguard' and fy >= 5 and fx <= 3:
                        c = mix(hair, COPPER, 0.5)
                    if fname == 'bottom' and troop == 'ic_hearthguard':
                        c = hair
                    c = mul(c, 1 + (rng.random() - 0.5) * 0.08)
                    px[xx, yy + oy] = safe(mul(c, SHADE[fname])) + (255,)
    im.save(ART / 'troops' / ('%s_faces.png' % troop))


def face_px(troop, n, x, y, skin, hair, eye):
    """The front face, 8x8 face px (y 0 the brow)."""
    if troop == 'ic_hearthguard':
        if y == 2 and 1 <= x <= 6:
            return mul(hair, 0.9)                      # heavy brows
        if y == 3 and x in (2, 5):
            return eye
        if y == 3 and x in (1, 6):
            return (235, 230, 220)
        if y == 4 and x in (3, 4):
            return mul(skin, 0.85)                     # nose
        if y >= 5 or (y == 4 and x in (0, 1, 6, 7)):
            return hair if (x + y) % 3 else mul(hair, 0.75)   # beard
        if y < 2:
            return mul(hair, 0.85) if n != 2 else skin
        return skin
    if troop == 'wm_tusk':
        if y < 2:
            return mul(hair, 0.9) if (x + n) % 2 else hair  # bristled brow
        if y == 2 and x in (1, 6):
            return mul(hair, 0.8)
        if y == 3 and x in (1, 6):
            return eye
        if y == 3 and x in (2, 5):
            return mul(skin, 0.7)
        if y >= 6:
            return mul(skin, 0.8)
        return skin if (x + y) % 3 else mul(skin, 0.9)
    # tideguard
    if y < 1:
        return hair
    if y == 3 and x in (1, 2, 5, 6):
        return eye if x in (2, 5) else mul(skin, 0.55)
    if y == 2 and x in (1, 2, 5, 6):
        return mul(skin, 0.8)
    if y == 5 and x in (2, 3, 4, 5):
        return mul(skin, 0.6)
    if y == 6 and x in (1, 6) and n % 2:
        return mul(skin, 0.75)
    return skin


# ---------------------------------------------------------------- weapons

MAT_KEY = {'steel': (255, 0, 0), 'iron': (170, 0, 0), 'gilt': (255, 170, 0), 'wood': (0x8B, 0x45, 0x13),
           'linen': (255, 255, 255), 'leather': (0x55, 0x33, 0x22), 'bone': (0xEE, 0xEE, 0xDD), 'glass': (0, 255, 0)}


def cube(name, frm, to, mat, colour):
    return {'name': name, 'from': frm, 'to': to, 'mat': mat, 'colour': colour}


WOOD_DARK, WOOD = hexc('4E3A28'), hexc('6A4E34')
IRON, STEEL, LEATHER = hexc('3E4146'), hexc('8C9096'), hexc('3E2E22')
GILT = hexc('A08852')


def hearth_axe(tier):
    c = [cube('haft', [-0.6, -7, -0.6], [0.6, 15, 0.6], 'wood', WOOD),
         cube('butt', [-0.9, -8, -0.9], [0.9, -7, 0.9], 'iron', IRON),
         cube('grip', [-0.75, -2.5, -0.75], [0.75, 2.5, 0.75], 'leather', LEATHER),
         cube('socket', [-1.2, 11, -1], [1.2, 16, 1], 'iron', BRASS if tier == 3 else IRON),
         cube('spike', [-0.5, 16, -0.5], [0.5, 18, 0.5], 'steel', STEEL),
         cube('blade', [-6, 11.5, -0.4], [-1.2, 15.5, 0.4], 'iron', SOOT),
         cube('beard', [-7.5, 8, -0.35], [-5, 16, 0.35], 'iron', SOOT),
         cube('bit', [-8.2, 7.5, -0.25], [-7.5, 16.5, 0.25], 'steel', STEEL),
         cube('back_spike', [1.2, 12.5, -0.6], [3, 14.5, 0.6], 'iron', IRON)]
    if tier >= 2:
        c += [cube('band_low', [-0.8, 8.5, -0.8], [0.8, 9.5, 0.8], 'gilt', BRASS),
              cube('band_top', [-0.8, 3.5, -0.8], [0.8, 4.2, 0.8], 'gilt', BRASS),
              cube('back_spike_tip', [3, 13, -0.4], [4.2, 14, 0.4], 'steel', STEEL)]
    if tier >= 3:
        c += [cube('crown_spike', [-0.3, 18, -0.3], [0.3, 19.5, 0.3], 'gilt', GILT),
              cube('rune_plate', [-4.5, 12.5, -0.5], [-2.5, 14.5, 0.5], 'gilt', GILT),
              cube('butt_spike', [-0.4, -9.5, -0.4], [0.4, -8, 0.4], 'gilt', GILT)]
    return c


def boar_spear(tier):
    c = [cube('shaft', [-0.5, -9, -0.5], [0.5, 22, 0.5], 'wood', WOOD),
         cube('butt', [-0.7, -10, -0.7], [0.7, -9, 0.7], 'iron', IRON),
         cube('grip', [-0.65, -2.5, -0.65], [0.65, 2.5, 0.65], 'leather', LEATHER),
         cube('socket', [-0.8, 21, -0.8], [0.8, 24, 0.8], 'iron', IRON),
         cube('crossbar', [-3, 22, -0.4], [3, 23, 0.4], 'iron', IRON),
         cube('head', [-1.5, 24, -0.3], [1.5, 30, 0.3], 'steel', STEEL),
         cube('head_tip', [-0.8, 30, -0.25], [0.8, 32.5, 0.25], 'steel', STEEL),
         cube('point', [-0.3, 32.5, -0.2], [0.3, 33.5, 0.2], 'steel', STEEL),
         cube('tassel_cord', [2.4, 19, -0.2], [2.8, 22, 0.2], 'linen', BARK),
         cube('tassel', [2, 16.5, -0.4], [3.2, 19, 0.4], 'bone', CLAY)]
    if tier >= 2:
        c += [cube('collar', [-0.75, 15, -0.75], [0.75, 16, 0.75], 'iron', IRON),
              cube('lug_r', [-4, 21.8, -0.3], [-3, 23.2, 0.3], 'iron', IRON),
              cube('lug_l', [3, 21.8, -0.3], [4, 23.2, 0.3], 'iron', IRON)]
    if tier >= 3:
        c += [cube('tusk_r', [-5.2, 22.2, -0.25], [-4, 25, 0.25], 'bone', BONE),
              cube('tusk_l', [4, 22.2, -0.25], [5.2, 25, 0.25], 'bone', BONE),
              cube('tassel_cord_2', [-2.8, 19, -0.2], [-2.4, 22, 0.2], 'linen', BARK),
              cube('tassel_2', [-3.2, 16.5, -0.4], [-2, 19, 0.4], 'bone', CLAY)]
    return c


def coral_trident(tier):
    c = [cube('shaft', [-0.5, -9, -0.5], [0.5, 20, 0.5], 'wood', WOOD_DARK),
         cube('butt', [-0.8, -10, -0.8], [0.8, -9, 0.8], 'bone', CORAL),
         cube('grip', [-0.65, -2.5, -0.65], [0.65, 2.5, 0.65], 'leather', mix(NAVY, LEATHER, 0.5)),
         cube('collar', [-0.9, 20, -0.9], [0.9, 22, 0.9], 'iron', VERD),
         cube('crossbar', [-4, 22, -0.6], [4, 23.5, 0.6], 'bone', CORAL),
         cube('prong_c', [-0.5, 23.5, -0.5], [0.5, 32, 0.5], 'bone', CORAL),
         cube('prong_r', [-4, 23.5, -0.5], [-3, 29, 0.5], 'bone', CORAL),
         cube('prong_l', [3, 23.5, -0.5], [4, 29, 0.5], 'bone', CORAL),
         cube('tip_c', [-0.3, 32, -0.3], [0.3, 34, 0.3], 'steel', STEEL),
         cube('tip_r', [-3.8, 29, -0.35], [-3.2, 31, 0.35], 'steel', STEEL),
         cube('tip_l', [3.2, 29, -0.35], [3.8, 31, 0.35], 'steel', STEEL)]
    if tier >= 2:
        c += [cube('barb_r', [-5, 26, -0.4], [-4, 27, 0.4], 'bone', CORAL),
              cube('barb_l', [4, 26, -0.4], [5, 27, 0.4], 'bone', CORAL),
              cube('band', [-0.7, 14, -0.7], [0.7, 15, 0.7], 'iron', VERD)]
    if tier >= 3:
        c += [cube('pearl', [-0.7, 23.2, -0.75], [0.7, 25, 0.75], 'glass', hexc('D8DCD4')),
              cube('barb_c_r', [-1.5, 28, -0.3], [-0.5, 29, 0.3], 'bone', CORAL),
              cube('barb_c_l', [0.5, 28, -0.3], [1.5, 29, 0.3], 'bone', CORAL)]
    return c


WEAPONS = {
    'hearth_axe': (hearth_axe, 2, [90, 45, 90], 'the grip held like the swords: turned flat to the side, tilted 45 degrees forward',
                   ['The Hearthguard hearth axe, tier I: an ash haft, a leather grip, a soot-iron bearded blade with a bright bit and a back spike.',
                    'tier II: brass bands on the haft and a longer back spike.',
                    'tier III: a brass socket, a gilt rune plate on the blade, a crown spike and a butt spike.']),
    'boar_spear': (boar_spear, 2, [0, 0, 0], 'held upright like the staff, so the crossbarred head stands over the cap',
                   ['The Tusk Spear boar spear, tier I: an ash shaft, a leaf head over an iron crossbar and a clay tassel; one head, never three.',
                    'tier II: a collar and longer lugs on the crossbar.',
                    'tier III: tusk lugs and a second clay tassel.']),
    'coral_trident': (coral_trident, 2, [0, 0, 0], 'held upright like the staff',
                      ['The Trident Guard coral trident, tier I: a driftwood shaft, a verdigris collar and three bleached coral prongs with steel tips.',
                       'tier II: coral barbs on the side prongs and a verdigris band.',
                       'tier III: a pearl at the fork and barbs on the middle prong.']),
}

FACE_DIRS = ('north', 'east', 'south', 'west', 'up', 'down')


def face_size(c, f):
    dx, dy, dz = (c['to'][i] - c['from'][i] for i in range(3))
    return {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}[f]


def ceil(v):
    i = int(v)
    return i if i == v else i + 1


def paint_weapon(wid, tier, cubes):
    W, H = 64, 128
    colour = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    keys = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    cp, kp = colour.load(), keys.load()
    rng = random.Random('%s-%d' % (wid, tier))
    x, y, row = 0, 0, 0
    elements = []
    wshade = {'north': 1.0, 'south': 0.75, 'east': 0.85, 'west': 0.85, 'up': 1.15, 'down': 0.6}
    for c in cubes:
        faces = {}
        for f in FACE_DIRS:
            fw, fh = face_size(c, f)
            tw, th = fw * WD, fh * WD
            cw, ch = ceil(round(tw, 4)), ceil(round(th, 4))
            if x + cw > W:
                x, y, row = 0, y + row + 1, 0
            if y + ch > H:
                raise SystemExit('%s: weapon sheet full' % wid)
            faces[f] = {'uv': [x, y, round(x + tw, 4), round(y + th, 4)], 'texture': 0}
            base = c['colour']
            for yy in range(y, y + ch):
                for xx in range(x, x + cw):
                    u, v = xx - x, yy - y
                    k = wshade[f]
                    col = base
                    if c['mat'] == 'wood' and (u + 3 * (v // 3)) % 4 == 0:
                        col = mul(base, 0.82)
                    if c['mat'] == 'steel' and (u == 0 or v == 0):
                        col = mul(base, 1.18)
                    if c['mat'] in ('iron', 'gilt') and (u == cw - 1 or v == ch - 1) and cw > 2 and ch > 2:
                        col = mul(base, 0.8)
                    if c['mat'] == 'leather' and v % 2 == 0:
                        col = mul(base, 0.8)
                    col = mul(col, k * (1 + (rng.random() - 0.5) * 0.08))
                    cp[xx, yy] = safe(col) + (255,)
                    kp[xx, yy] = MAT_KEY[c['mat']] + (255,)
            x += cw + 1
            row = max(row, ch)
        elements.append({'name': c['name'], 'from': c['from'], 'to': c['to'], 'faces': faces})
    return colour, keys, elements


def write_weapon(wid, tier):
    fn, thickness, rotation, grip_note, notes = WEAPONS[wid]
    name = wid if tier == 1 else '%s_t%d' % (wid, tier)
    cubes = fn(tier)
    colour, keys, elements = paint_weapon(wid, tier, cubes)
    folder = ART / 'weapons'
    colour.save(folder / (name + '.png'))
    keys.save(folder / (name + '_m.png'))
    buf = io.BytesIO()
    Image.open(folder / (name + '.png')).save(buf, 'PNG')
    template = json.loads((folder / 'longsword.bbmodel').read_text(encoding='utf-8'))
    proto_el = template['elements'][0]
    rnd = random.Random(name)
    def uid():
        return str(uuid.UUID(int=rnd.getrandbits(128)))
    els = []
    for e in elements:
        el = copy.deepcopy(proto_el)
        el['name'] = e['name']
        el['from'], el['to'] = e['from'], e['to']
        el['origin'] = [round((a + b) / 2, 4) for a, b in zip(e['from'], e['to'])]
        el['faces'] = e['faces']
        el['uuid'] = uid()
        el.pop('rotation', None)
        els.append(el)
    model = copy.deepcopy(template)
    model['name'] = name
    model['elements'] = els
    gid = uid()
    model['groups'][0]['uuid'] = gid
    model['groups'] = model['groups'][:1]
    model['outliner'] = [{'uuid': gid, 'isOpen': False, 'children': [e['uuid'] for e in els]}]
    t = model['textures'][0]
    t.update({'name': name, 'uuid': uid(), 'width': 64, 'height': 128, 'uv_width': 64, 'uv_height': 128,
              'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii')})
    model['textures'] = [t]
    (folder / (name + '.bbmodel')).write_text(json.dumps(model, indent=1), encoding='utf-8')
    write_json(folder / (name + '.json'), {'weapon': name, 'thickness': thickness,
                                           'grip': {'offset': [0, 0, 0], 'rotation': rotation,
                                                    'note': 'the grip\'s centre is the model origin (the fist covers y -2..+2); ' + grip_note},
                                           'note': notes[tier - 1]})


def main():
    for wid in WEAPONS:
        for tier in (1, 2, 3):
            write_weapon(wid, tier)
    build_sources()
    for troop in TROOPS:   # placeholders so the catalog loads before the first paint
        for f, size in [('%s_s%d.png' % (troop, s), (SIZE[0] * D, SIZE[1] * D)) for s in (1, 2, 3)] + [('%s_faces.png' % troop, (64, 128))]:
            if not (ART / 'troops' / f).is_file():
                Image.new('RGBA', size, (0, 0, 0, 0)).save(ART / 'troops' / f)
    for troop in TROOPS:
        model, skins = paint_skins(troop)
        paint_veteran(troop, model, skins)
        paint_faces(troop)
    print('paint_house_troops: wrote 3 troops, %d parts, 9 weapons' % sum(len(t['parts']) for t in TROOPS.values()))


if __name__ == '__main__':
    main()
