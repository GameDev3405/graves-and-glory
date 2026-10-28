"""Generate the art sources of the three House troops (ic_hearthguard, wm_tusk, tc_tideguard) and their nine weapon tiers.

Writes art/troops/<troop>.json, the new art/parts/*.json (skin parts and the team accent parts), the skins _s1.._s3, the
_veteran trim, the _faces patches, art/weapons/<weapon>[_t2|_t3].{bbmodel,json,png,_m.png}, and regenerates
art/team/team_cloth.png and the four glyph masks (the retained team parts' pixels kept, the old pennant and standard
cleared, the House accent parts painted in). Deterministic: run it, then `python tools/build_skins.py`.

Knight-armour pass (Kort, 2026-10-28): no back flags on any troop; team colour reads through accents (tabards, crests,
kilts, headbands, wraps) in the side's cloth, the House reads through fixed accent paint (Ironclad ember, Wildmoot ochre,
Tidecallers sea blue) on bordures, straps and wraps. Plate is painted in five tones with edge highlights, rivets, seams
and scuffs over mail; dimensional boxes add crests, layered pauldrons, belts, buckles, tassets, knee cops and sabatons."""
import base64
import colorsys
import copy
import io
import json
import math
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
CD = 3           # cloth template texels a px
SIZE = (128, 64)
SLOTS = [[96, 48], [64, 48], [96, 32]]
SHADE = {'top': 1.12, 'bottom': 0.62, 'north': 1.0, 'south': 0.76, 'west': 0.88, 'east': 0.88}


def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*hs):
    return [hexc(h) for h in hs]


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


def tone(R, t):
    return R[max(0, min(len(R) - 1, int(round(t * (len(R) - 1)))))]


# ---------------------------------------------------------------- palettes (5 tones, deep to gleam)
IRON = ramp('1B1D21', '30343A', '4C5158', '6F757D', 'A3A9B0')
SCORCH = ramp('1A1714', '2D2823', '47403A', '655B51', '8C8277')
BRASS = ramp('3E2B12', '664A20', '8E6C36', 'B3905A', 'D6BE8E')
EMBER = ramp('2E0E08', '52190E', '7A2814', '9A3C20', 'B85E40')     # Ironclad accent (forge ember), dark enough to keep its hue
LEATH = ramp('20160E', '372619', '533A26', '725337', '937355')
MAIL = ramp('16181C', '2A2E34', '454A51', '686E76', '959BA3')
COPPER = ramp('35190C', '582C16', '7A4220', 'A05E2E', 'C08248')
FUR = ramp('24170E', '43291A', '673F25', '885836', 'A97650')
BONE = ramp('7E7664', 'A49C86', 'C6BEA6', 'DDD7C3', 'F0ECE0')
OCHRE = ramp('3E2208', '683E14', '8E5C20', 'B07C36', 'CC9C5E')     # Wildmoot accent
HIDE = ramp('2A1C12', '46301E', '6A4A30', '8A6646', 'A88660')
VERD = ramp('16292A', '24433F', '3A6458', '5E8A7A', '8EB2A2')       # bronze gone green
BRONZE = ramp('2E200E', '4E3818', '74562A', '98764A', 'BCA070')
SEA = ramp('0B1528', '122848', '1A3F6A', '2A5E8A', '5888AA')       # Tidecallers accent (deep sea blue)
CORAL = ramp('857A6A', 'A99F8E', 'C7BFAD', 'DCD6C6', 'EEEAE0')
NAVY = ramp('0E1320', '18202F', '232E42', '33425A', '4E6078')
SKIN_TIDE = ramp('3A4A44', '50645C', '6A8278', '84A094', 'A2BAAE')
BARN = hexc('CFCBBE')


# ---------------------------------------------------------------- the material kit

class Ctx:
    """What a material sees of one texel: its face, its place in the face (x, y texels, w, h the face's texels), the stage."""
    __slots__ = ('face', 'x', 'y', 'w', 'h', 'stage', 'rng', 'note', 'bi')


def jit(ctx, c, amt=0.04):
    return mul(c, 1 + (ctx.rng.random() - 0.5) * 2 * amt)


def side_t(ctx, base=0.55, grad=0.22):
    """The face's value before detail: top faces lit, bottom faces in shadow, side faces lighter at the top."""
    if ctx.face == 'top':
        return base + 0.25
    if ctx.face == 'bottom':
        return base - 0.35
    return base + grad / 2 - grad * ctx.y / max(1, ctx.h - 1)


def metal(R, rim=None, rivets=0, seam=0, grad=0.22, scuff=0.05, base=0.55, bevel=True):
    """Plate: five tones, the top edge catching the light, the bottom edge and the right edge in shadow, rivets (a gleam
    over a dark pit) every `rivets` texels two rows in from the top and bottom edges, lame seams every `seam` rows, scuffs."""
    def f(ctx):
        t = side_t(ctx, base, grad)
        x, y, w, h = ctx.x, ctx.y, ctx.w, ctx.h
        R2 = R
        if bevel:
            if y == 0:
                t += 0.34
            elif y == 1 and h > 4:
                t += 0.12
            if x == 0:
                t += 0.12
            if x == w - 1:
                t -= 0.16
            if y == h - 1:
                t -= 0.32
        if rim is not None and (x == 0 or y == 0 or x == w - 1 or y == h - 1):
            R2 = rim
        if seam and ctx.face not in ('top', 'bottom') and 0 < y < h - 1:
            if y % seam == seam - 1:
                t -= 0.36
            elif y % seam == 0:
                t += 0.2
        if rivets and ctx.face not in ('top', 'bottom') and h >= 6 and w >= 5 and 1 < x < w - 2:
            if (y in (2, h - 3)) and x % rivets == rivets // 2:
                return jit(ctx, tone(rim or BRASS if rim else R, 0.98), 0.02)
            if (y in (3, h - 2)) and x % rivets == rivets // 2:
                t = 0.05
        r = ctx.rng.random()
        if r < scuff:
            t += 0.28
        elif r < scuff * 2:
            t -= 0.26
        return jit(ctx, tone(R2, t), 0.03)
    return f


def rivet_metal(R, rivR, every=5, rows=None, **kw):
    """Plate with rivets in their own metal (brass on iron)."""
    inner = metal(R, **kw)

    def f(ctx):
        x, y, w, h = ctx.x, ctx.y, ctx.w, ctx.h
        rr = rows if rows is not None else (2, h - 3)
        if ctx.face not in ('top', 'bottom') and w >= 5 and 1 < x < w - 2 and x % every == every // 2:
            if y in rr:
                return jit(ctx, tone(rivR, 0.95), 0.02)
            if y - 1 in rr:
                return jit(ctx, tone(R, 0.0), 0.02)
        return inner(ctx)
    return f


def mail(R, barn=0.0):
    """Riveted mail: rows of rings two texels high, every other row offset, each ring a gleam, a mid and a dark gap."""
    def f(ctx):
        x, y = ctx.x, ctx.y
        off = (y // 2) % 2
        cx, cy = (x + off) % 2, y % 2
        t = side_t(ctx, 0.5, 0.25)
        t += {(0, 0): 0.3, (1, 0): 0.0, (0, 1): -0.12, (1, 1): -0.38}[(cx, cy)]
        if barn and ctx.rng.random() < barn:
            return jit(ctx, mix(BARN, mul(BARN, 0.75), ctx.rng.random()), 0.03)
        return jit(ctx, tone(R, t), 0.04)
    return f


def scales(R, rim=None):
    """Overlapping scales three texels a row: a lit top row, the body, a dark lower lip, the seams staggered."""
    def f(ctx):
        x, y = ctx.x, ctx.y
        row = y // 3
        cx = (x + (1 if row % 2 else 0)) % 3
        t = side_t(ctx, 0.5, 0.2) + (0.3, 0.02, -0.3)[y % 3]
        if cx == 2:
            t -= 0.18
        if y % 3 == 2 and cx != 1:
            t -= 0.12
        if rim is not None and y == ctx.h - 1:
            return jit(ctx, tone(rim, 0.6), 0.03)
        if ctx.rng.random() < 0.04:
            t += 0.25
        return jit(ctx, tone(R, t), 0.04)
    return f


def leather(R, stitch=None, straps=(), base=0.5, buckle=None):
    """Leather: a grain, darker edges, an optional stitch line one texel in, and straps (rows given as fractions)."""
    def f(ctx):
        x, y, w, h = ctx.x, ctx.y, ctx.w, ctx.h
        t = side_t(ctx, base, 0.18) + (ctx.rng.random() - 0.5) * 0.18
        if x == 0 or x == w - 1 or y == h - 1:
            t -= 0.18
        if y == 0:
            t += 0.15
        for lo, hi in straps:
            if ctx.face in ('top', 'bottom'):
                break
            if lo * h <= y < hi * h:
                t2 = 0.3 if int(lo * h) == y else (0.06 if y == int(math.ceil(hi * h)) - 1 else 0.18)
                if buckle is not None and ctx.face == 'north' and abs(x - w // 2) <= 1:
                    return jit(ctx, tone(buckle, 0.85 if y == int(lo * h) else 0.6), 0.02)
                return jit(ctx, tone(R, t2), 0.03)
        if stitch is not None and ctx.face not in ('top', 'bottom') and (x == 1 or x == w - 2) and y % 2 == 0 and 0 < y < h - 1:
            return jit(ctx, stitch, 0.03)
        return jit(ctx, tone(R, t), 0.03)
    return f


def fur(R, clumps=4):
    """Shaggy fur: vertical strands in clumps, lighter tips at each clump's lower end."""
    def f(ctx):
        x, y = ctx.x, ctx.y
        k = (x * 7 + (y // clumps) * 3) % 5
        t = side_t(ctx, 0.45, 0.2) + (k - 2) * 0.07
        if (y + x * 2) % clumps == clumps - 1:
            t += 0.22
        if ctx.rng.random() < 0.08:
            t -= 0.2
        return jit(ctx, tone(R, t), 0.04)
    return f


def cloth_skin(R, folds=6, base=0.5):
    """Cloth on the skin: vertical folds, a lit and a shaded texel each side of a crease."""
    def f(ctx):
        t = side_t(ctx, base, 0.2)
        if ctx.face not in ('top', 'bottom'):
            k = ctx.x % folds
            t += {0: -0.25, 1: 0.12}.get(k, 0)
        return jit(ctx, tone(R, t + (ctx.rng.random() - 0.5) * 0.08), 0.03)
    return f


def braid(R):
    def f(ctx):
        t = side_t(ctx, 0.5, 0.2) + (0.22 if (ctx.x + ctx.y) % 3 == 0 else (-0.22 if (ctx.x - ctx.y) % 3 == 0 else 0))
        return jit(ctx, tone(R, t), 0.03)
    return f


def horn(R, rings=4, tip_light=True):
    """Horn and tusk: lighter toward the tip (the top of the face), growth rings, a shaded side."""
    def f(ctx):
        t = 0.75 - 0.4 * ctx.y / max(1, ctx.h - 1) if tip_light else 0.55
        if ctx.face == 'top':
            t = 0.95
        if ctx.y % rings == rings - 1:
            t -= 0.2
        if ctx.x == ctx.w - 1:
            t -= 0.2
        if ctx.x == 0:
            t += 0.1
        return jit(ctx, tone(R, t), 0.03)
    return f


def coral(ctx):
    t = side_t(ctx, 0.6, 0.25)
    if (ctx.x * 2 + ctx.y * 3) % 7 == 0:
        t -= 0.4
    elif ctx.rng.random() < 0.15:
        t -= 0.2
    if ctx.y == 0:
        t += 0.2
    return jit(ctx, tone(CORAL, t), 0.03)


def shell(ctx):
    rib = (ctx.x // 2) % 2
    t = side_t(ctx, 0.6, 0.3) + (0.15 if rib else -0.18)
    if ctx.face in ('north', 'south') and ctx.y < 2 and (ctx.x < ctx.w // 4 or ctx.x >= ctx.w * 3 // 4):
        t -= 0.3
    if ctx.y == ctx.h - 1:
        t -= 0.25
    return jit(ctx, tone(CORAL, t), 0.03)


def bands(base, rows, band):
    """A material with bands (belt, wrap, cuff) over row ranges given as fractions of the face height."""
    def f(ctx):
        if ctx.face not in ('top', 'bottom'):
            t = ctx.y / ctx.h
            for lo, hi in rows:
                if lo <= t < hi:
                    return band(ctx)
        return base(ctx)
    return f


def limb(upper, lower, split):
    def f(ctx):
        if ctx.face == 'top':
            return upper(ctx)
        if ctx.face == 'bottom':
            return lower(ctx)
        return upper(ctx) if ctx.y < ctx.h * split else lower(ctx)
    return f


def by_box(*mats):
    return lambda ctx: mats[min(ctx.bi, len(mats) - 1)](ctx)


def by_note(table, default):
    def f(ctx):
        for k, m in table.items():
            if k in ctx.note:
                return m(ctx)
        return default(ctx)
    return f


def solid(R, t=0.5):
    return lambda ctx: jit(ctx, tone(R, side_t(ctx, t, 0.15)), 0.04)


def wrap(R):
    """Diagonal cloth wraps (a forearm, a haft)."""
    def f(ctx):
        t = side_t(ctx, 0.5, 0.15) + (0.25 if (ctx.x + ctx.y) % 4 == 0 else (-0.25 if (ctx.x + ctx.y) % 4 == 2 else 0))
        return jit(ctx, tone(R, t), 0.03)
    return f


# ---------------------------------------------------------------- parts

def box(origin, size, note=''):
    return {'origin': origin, 'size': size, 'note': note}


def mirror(boxes):
    """The boxes mirrored across x = 0."""
    return [box([-(b['origin'][0] + b['size'][0])] + b['origin'][1:], b['size'], b['note'].replace('right', 'left')) for b in boxes]


R_PAULDRON = [
    box([-8.5, -13.5, -3], [5, 3, 6], 'right slab'),
    box([-8.25, -10.5, -2.75], [4, 1, 5], 'right first lame'),
    box([-8, -9.5, -2.5], [3, 1, 4], 'right second lame'),
    box([-7.5, -14.5, -1], [2, 1, 2], 'right boss'),
    box([-7, -16.5, -0.5], [1, 2, 1], 'right spike'),
]
R_ARM_TIDE = [box([-3.5, 6, -2.5], [5, 3, 5], 'right bronze bracer')]

# part: (bone, helm tag, note, boxes)
PARTS = {
    # ---- Hearthguard
    'box_helm': ('head', True, 'The Hearthguard box helm: a riveted soot-iron crown with a raised comb, a back plate, two cheek plates standing '
                 '1 px off the face, a brow bar and a nasal; the eyes and the beard show through the opening.', [
        box([-5, -9.5, -5], [10, 2, 10], 'crown, 1 px proud all round'),
        box([-5, -7.5, 3], [10, 7, 2], 'back plate'),
        box([-5, -7.5, -5], [2, 7, 8], 'right cheek plate, 1 px before the face'),
        box([3, -7.5, -5], [2, 7, 8], 'left cheek plate'),
        box([-3, -7.5, -5.25], [6, 1, 1], 'brow bar'),
        box([-0.5, -6.5, -5.75], [1, 3, 1], 'nasal'),
        box([-1, -10.5, -4.5], [2, 1, 9], 'comb, front to back'),
    ]),
    'braided_beard': ('head', False, 'The Hearthguard copper beard: a square beard under the helm and two braids down the tabard edges, ringed in brass (the glyph stays clear).', [
        box([-3, -2.5, -5], [6, 4, 2], 'beard'),
        box([-3, 1.5, -5], [1, 6, 2], 'right braid'),
        box([2, 1.5, -5], [1, 6, 2], 'left braid'),
        box([-3.5, 5.5, -5.5], [2, 1, 3], 'right brass braid ring'),
        box([1.5, 5.5, -5.5], [2, 1, 3], 'left brass braid ring'),
    ]),
    'slab_pauldrons': ('body', False, 'The Hearthguard mark: layered soot-iron pauldrons, a slab wider than the arm over two lames, a brass '
                       'boss and a short spike on each.', R_PAULDRON + mirror(R_PAULDRON)),
    'ic_breastplate': ('body', False, 'The Hearthguard breastplate over the scale coat, with a fauld lame at its foot.', [
        box([-4.5, -12.5, -2.5], [9, 7, 5], 'breastplate'),
        box([-4.5, -5.5, -2.5], [9, 1, 5], 'fauld lame'),
    ]),
    'ic_belt': ('body', False, 'The Hearthguard sword belt over the tabard: a broad leather belt, a brass buckle and a belt pouch.', [
        box([-5, -1, -3], [10, 2, 6], 'belt'),
        box([-1, -1.5, -3.5], [2, 3, 1], 'brass buckle'),
        box([2.5, -0.5, -3.75], [2, 2, 1], 'pouch'),
    ]),
    'scale_coat_skirt': ('body', False, 'The knee-length skirt of the Hearthguard scale coat, with a plate tasset each side.', [
        box([-4.5, 0, -2.5], [9, 6, 5], 'skirt to the knee'),
        box([-5.5, 1, -3], [3, 4, 1], 'right tasset'),
        box([2.5, 1, -3], [3, 4, 1], 'left tasset'),
    ]),
    'ic_greave': ('right_leg', False, 'A Hearthguard knee cop and sabaton toe (the left copy is ic_greave_left).', [
        box([-2.5, 4.5, -2.75], [5, 2, 1], 'knee cop'),
        box([-2.25, 10, -3], [4, 2, 1], 'sabaton toe'),
    ]),
    'ic_greave_left': ('left_leg', False, 'The left Hearthguard knee cop and sabaton toe.', [
        box([-2.5, 4.5, -2.75], [5, 2, 1], 'knee cop'),
        box([-1.75, 10, -3], [4, 2, 1], 'sabaton toe'),
    ]),
    'helm_horns': ('head', True, 'Hearthguard stage 3: two short horns from the crown of the box helm.', [
        box([-7, -10.5, -1], [2, 2, 2], 'right horn root'),
        box([-8, -13.5, -0.5], [1, 3, 1], 'right horn tip'),
        box([5, -10.5, -1], [2, 2, 2], 'left horn root'),
        box([7, -13.5, -0.5], [1, 3, 1], 'left horn tip'),
    ]),
    # ---- Tusk Spear
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
    'leather_cap': ('head', True, 'The Tusk Spear riveted leather cap with a nose-guard bar and the boar ears through it.', [
        box([-4.5, -8.5, -4.5], [9, 2, 9], 'cap'),
        box([-5.5, -10, 0], [2, 2, 1], 'right ear'),
        box([3.5, -10, 0], [2, 2, 1], 'left ear'),
        box([-1, -9.5, -4.75], [2, 1, 9], 'iron cap bar'),
    ]),
    'hide_mantle': ('body', False, 'The Tusk Spear hide mantle: a shaggy roll over the shoulders, a bristled hump and a hide flap down the back.', [
        box([-5, -12.5, -3], [10, 3, 6], 'shoulder roll'),
        box([-1.5, -14, -0.5], [3, 2, 4], 'bristle hump'),
        box([-4.5, -9.5, 2.5], [9, 7, 1], 'back flap'),
    ]),
    'wm_pauldron': ('body', False, 'The Tusk Spear left pauldron: a boiled-leather cup over the mantle with two bone spikes.', [
        box([3.5, -13.5, -2.5], [5, 3, 5], 'leather cup'),
        box([4, -10.5, -2.25], [4, 1, 4], 'lame'),
        box([5, -15.5, -1], [1, 2, 1], 'front spike'),
        box([6.5, -15, 0.5], [1, 2, 1], 'back spike'),
    ]),
    'wm_belt': ('body', False, 'The Tusk Spear belt: a wide hide belt, a bone disc buckle and a trophy tusk.', [
        box([-4.5, -1.5, -2.5], [9, 2, 5], 'belt'),
        box([-1, -2, -3.1], [2, 3, 1], 'bone disc buckle'),
        box([2, 0, -3.25], [1, 3, 1], 'trophy tusk'),
    ]),
    'wm_shin': ('right_leg', False, 'A Tusk Spear leather shin guard (the left copy is wm_shin_left).', [
        box([-2.5, 5, -2.5], [5, 4, 5], 'shin guard'),
    ]),
    'wm_shin_left': ('left_leg', False, 'The left Tusk Spear shin guard.', [
        box([-2.5, 5, -2.5], [5, 4, 5], 'shin guard'),
    ]),
    'tusk_crown': ('head', True, 'Tusk Spear stage 3: a crown of boar tusks on the cap.', [
        box([-3.5, -11.5, -3], [1, 3, 1], 'right tusk'),
        box([-0.5, -12.5, -3.5], [1, 4, 1], 'middle tusk'),
        box([2.5, -11.5, -3], [1, 3, 1], 'left tusk'),
    ]),
    # ---- Trident Guard
    'finned_cap': ('head', True, 'The Trident Guard verdigris cap: a riveted skull cap, a brow band and two fin ear guards (the dorsal fin is '
                   'the team crest tc_crest).', [
        box([-4.5, -8.5, -4.5], [9, 3, 9], 'cap'),
        box([-5.5, -7, -1], [1, 4, 3], 'right fin guard'),
        box([4.5, -7, -1], [1, 4, 3], 'left fin guard'),
        box([-4.75, -6, -4.75], [9, 1, 1], 'brow band'),
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
    'tc_cuirass': ('body', False, 'The Trident Guard bronze scale cuirass over the mail, with a shell-clasped belt.', [
        box([-4.5, -12.5, -2.5], [9, 7, 5], 'scale cuirass'),
        box([-4.5, -1, -2.5], [9, 2, 5], 'belt'),
        box([-1, -1.5, -3], [2, 2, 1], 'shell clasp'),
    ]),
    'scallop_shell': ('body', False, 'The scallop shell on the Trident Guard back.', [
        box([-3.5, -11, 2.75], [7, 6, 1], 'shell'),
        box([-1.5, -5, 2.75], [3, 1, 1], 'hinge'),
    ]),
    'tc_bracer': ('right_arm', False, 'A Trident Guard bronze bracer (the left copy is tc_bracer_left).', R_ARM_TIDE),
    'tc_bracer_left': ('left_arm', False, 'The left Trident Guard bronze bracer.', [box([-1.5, 6, -2.5], [5, 3, 5], 'left bronze bracer')]),
    'tc_greave': ('right_leg', False, 'A Trident Guard bronze greave and knee boss (the left copy is tc_greave_left).', [
        box([-2.5, 6, -2.5], [5, 5, 5], 'greave'),
        box([-1, 4.5, -3], [2, 2, 1], 'knee boss'),
    ]),
    'tc_greave_left': ('left_leg', False, 'The left Trident Guard bronze greave and knee boss.', [
        box([-2.5, 6, -2.5], [5, 5, 5], 'greave'),
        box([-1, 4.5, -3], [2, 2, 1], 'knee boss'),
    ]),
    'coral_crown': ('head', True, 'Trident Guard stage 3: coral sprigs on the finned cap.', [
        box([-3.5, -12, -2.5], [1, 3, 1], 'right sprig'),
        box([-4.5, -13, -2.5], [1, 2, 1], 'right sprig twig'),
        box([2.5, -12, -2.5], [1, 3, 1], 'left sprig'),
        box([3.5, -13, -2.5], [1, 2, 1], 'left sprig twig'),
    ]),
}

# team accent parts, on the shared cloth sheet: part: (bone, helm tag, note, boxes, cloth painter name)
TEAM = {
    'ic_tabard': ('body', False, 'The Hearthguard tabard in the side\'s cloth: chequy with an ember bordure, the side\'s glyph on the chest '
                  'and the lower flap, front and back, behind the folded arms.', [
        box([-2.5, -11.5, -2.9], [5, 16, 1], 'front'),
        box([-2.5, -11.5, 1.9], [5, 16, 1], 'back'),
    ], 'ic_tabard'),
    'ic_crest': ('head', True, 'The Hearthguard crest in the side\'s cloth: a horsehair plume along the comb and a tail down the back.', [
        box([-1, -13.5, -4], [2, 3, 9], 'plume'),
        box([-1, -12.5, 5], [2, 6, 1], 'tail'),
    ], 'plume'),
    'wm_kilt': ('body', False, 'The Tusk Spear war kilt in the side\'s cloth: barry with an ochre-stitched hem, the glyph on the front flap.', [
        box([-3.5, 0.5, -4.4], [7, 6, 1], 'front, 1.5 px proud so the hunch does not sink it into the legs'),
        box([-3.5, 0.5, 1.9], [7, 6, 1], 'back'),
    ], 'wm_kilt'),
    'wm_headband': ('head', True, 'The Tusk Spear headband in the side\'s cloth, knotted at the back with two tails.', [
        box([-5, -7, -5], [10, 1, 10], 'band'),
        box([1.5, -6, 5], [1, 4, 1], 'tail'),
        box([2.75, -6, 5], [1, 3, 1], 'short tail'),
    ], 'band'),
    'wm_wraps': ('right_arm', False, 'Tusk Spear forearm wraps in the side\'s cloth; the left copy takes bone left_arm, offset [2,0,0].', [
        box([-3.5, 5, -2.5], [5, 2, 5], 'wrap'),
    ], 'wraps'),
    'tc_crest': ('head', True, 'The Trident Guard dorsal fin: bronze ribs over a membrane in the side\'s cloth.', [
        box([-0.5, -11.5, -3], [1, 3, 7], 'fin'),
    ], 'fin'),
    'tc_tabard': ('body', False, 'The Trident Guard tabard in the side\'s cloth: barry-wavy with a sea-blue bordure and the glyph on the chest.', [
        box([-2.5, -11.5, -2.9], [5, 14, 1], 'front'),
    ], 'tc_tabard'),
    'tc_wraps': ('right_arm', False, 'Trident Guard forearm wraps in the side\'s cloth above the bracer; the left copy takes bone left_arm, offset [2,0,0].', [
        box([-3.5, 4, -2.5], [5, 2, 5], 'wrap'),
    ], 'wraps'),
}

# ---------------------------------------------------------------- troops

TROOPS = {
    'ic_hearthguard': {
        'note': 'Ironclad Hearthguard (unit identity, Ironclad): vindicator, biped, broad and short; the riveted box helm with cheek plates '
                'and a horsehair crest, the layered slab pauldrons (the mark), arms folded at rest, a breastplate over a knee-length '
                'soot-iron scale coat, the chequy team tabard under a brass-buckled belt, tassets, knee cops and sabatons, the copper '
                'beard braided to the belt; stage 3 helm horns. Hearth axe, the shield on the left arm. No back flag.',
        'bones': {'body': {'scale': [1.2, 0.95, 1.15]}, 'right_arm': {'scale': [1.1, 0.9, 1.1]}, 'left_arm': {'scale': [1.1, 0.9, 1.1]},
                  'right_leg': {'scale': [1.1, 0.8, 1.1]}, 'left_leg': {'scale': [1.1, 0.8, 1.1]}},
        'idleArms': 'folded',
        'parts': ['box_helm', 'braided_beard', 'slab_pauldrons', 'ic_breastplate', 'ic_belt', 'scale_coat_skirt', 'ic_greave',
                  'ic_greave_left', ('helm_horns', [3])],
        'team': [{'part': 'ic_tabard'}, {'part': 'ic_crest'}],
        'weapon': 'hearth_axe', 'portrait': 'standard',
    },
    'wm_tusk': {
        'note': 'Wildmoot Tusk Spear (unit identity, Wildmoot): zombified piglin, biped, hunched; boar-kin with a snout and big white tusks, '
                'a riveted leather cap under the team headband, a shaggy hide mantle and a spiked leather pauldron over a strapped '
                'jerkin, the barry team kilt, team forearm wraps, shin guards; stage 3 tusk crown. The crossbarred boar spear stands '
                'with its head over the cap (the mark). No back flag.',
        'bones': {},
        'hunch': 20,
        'idleArms': 'hang',
        'parts': ['boar_snout', 'tusks', 'leather_cap', 'hide_mantle', 'wm_pauldron', 'wm_belt', 'wm_shin', 'wm_shin_left',
                  ('tusk_crown', [3])],
        'team': [{'part': 'wm_kilt'}, {'part': 'wm_headband'}, {'part': 'wm_wraps', 'name': 'wm_wraps_right'},
                 {'part': 'wm_wraps', 'name': 'wm_wraps_left', 'bone': 'left_arm', 'offset': [2, 0, 0]}],
        'weapon': 'boar_spear', 'portrait': 'tall 4',
    },
    'tc_tideguard': {
        'note': 'Tidecallers Trident Guard (unit identity, Tidecallers): drowned, biped, upright and long-legged; bleached coral rising '
                'from both shoulders (the mark), a bronze scale cuirass over barnacled mail, the scallop shell on the back, the '
                'verdigris finned cap with the team fin, the barry-wavy team tabard, team wraps over bronze bracers, bronze greaves; '
                'stage 3 coral crown. Coral trident. No back flag.',
        'bones': {'right_leg': {'scale': [1, 1.05, 1]}, 'left_leg': {'scale': [1, 1.05, 1]}},
        'idleArms': 'hang',
        'parts': ['finned_cap', 'coral_pauldrons', 'tc_cuirass', 'scallop_shell', 'tc_bracer', 'tc_bracer_left', 'tc_greave',
                  'tc_greave_left', ('coral_crown', [3])],
        'team': [{'part': 'tc_crest'}, {'part': 'tc_tabard'}, {'part': 'tc_wraps', 'name': 'tc_wraps_right'},
                 {'part': 'tc_wraps', 'name': 'tc_wraps_left', 'bone': 'left_arm', 'offset': [2, 0, 0]}],
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
                # only the two strips the faces use are claimed, so small boxes nest in a big box's empty top corners
                rects = footprint([u, v], size)
                if all(self.free(r) for r in rects):
                    for r in rects:
                        self.mark(r)
                    return [u, v]
        raise SystemExit('no room on the sheet for a %s box' % (size,))


def write_json(path, obj):
    path.write_text(json.dumps(obj, indent=1) + '\n', encoding='utf-8', newline='\n')


def build_sources(team_uv):
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
            try:
                boxes_uv[(n, i)] = pk.place(b['size'])
            except SystemExit:
                raise SystemExit('%s: no room on the troop sheet for %s box %d %s' % (troop, n, i, b['size']))
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
    for n, (bone, helm, note, bxs, _) in TEAM.items():
        write_json(ART / 'parts' / (n + '.json'), {
            'part': n, 'note': note, 'bone': bone, 'team': True, 'tag': 'helm' if helm else '',
            'boxes': [{'origin': b['origin'], 'size': b['size'], 'uv': team_uv[(n, i)], 'note': b['note']} for i, b in enumerate(bxs)]})
    return layout


# ---------------------------------------------------------------- the team cloth (template greys and glyph masks)

FIELD = {192: (192, 192, 192), 160: (160, 160, 160), 128: (128, 128, 128), 96: (96, 96, 96), 64: (64, 64, 64)}
FLAGS = ('pennant', 'standard')   # the retired back flags, cleared from the cloth sheet


def glyph_mask(kind, n=9):
    """The side glyph on an n x n texel grid: set of (x, y)."""
    c = (n - 1) / 2
    out = set()
    if kind == 'star':
        pts = []
        for k in range(10):
            a = -math.pi / 2 + k * math.pi / 5
            r = c + 0.5 if k % 2 == 0 else (c + 0.5) * 0.42
            pts.append((c + r * math.cos(a), c + 0.4 + r * math.sin(a)))
    for y in range(n):
        for x in range(n):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            if kind == 'sun':
                on = r <= c * 0.5 + 0.2 or (c * 0.75 <= r <= c + 0.5 and (dx == 0 or dy == 0 or abs(dx) == abs(dy)))
            elif kind == 'moon':
                on = r <= c + 0.3 and math.hypot(dx - c * 0.55, dy + c * 0.3) > c * 0.82
            elif kind == 'star':
                on = inside(pts, x, y)
            else:  # wave
                w1 = c * 0.55 + 1.1 * math.sin(x * 1.1)
                on = abs(y - w1) <= 0.6 or abs(y - (w1 + c * 0.95)) <= 0.6
            if on:
                out.add((x, y))
    return out


def inside(pts, x, y):
    n, j, on = len(pts), len(pts) - 1, False
    for i in range(n):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            on = not on
        j = i
    return on


def cl_fold(ctx, g):
    """A field grey shaded by face: the outer face as given, the top lit, the sides and the inner face a step down."""
    order = [64, 96, 128, 160, 192]
    i = order.index(g)
    i += {'top': 1, 'bottom': -2, 'west': -1, 'east': -1}.get(ctx.face, 0)
    if ctx.face == ctx.inner:
        i -= 1
    return order[max(0, min(4, i))]


def heraldic(field, border, n_glyph=9, glyph_rows=(), border_w=2):
    """A tabard face: `field(ctx)` gives the field grey, a bordure `border_w` texels wide in fixed House paint with a dark
    inner line, plaques (64 outline, 160 inside) at the given top rows carrying the glyph. The outer face is ctx.outer."""
    def f(ctx):
        x, y, w, h = ctx.x, ctx.y, ctx.w, ctx.h
        if ctx.face != ctx.outer:
            if ctx.face in ('top', 'bottom', 'west', 'east') and border is not None:
                return ('fix', tone(border, 0.5 if ctx.face != 'bottom' else 0.2))
            return cl_fold(ctx, 128)
        if x < border_w or x >= w - border_w or y >= h - border_w:
            if border is None:
                return 96
            t = 0.75 if y == 0 or x == 0 else (0.25 if x == w - 1 or y == h - 1 else 0.5)
            if (x + y) % 6 == 0 and 0 < y < h - 1:
                t = 0.95   # stitched studs
            return ('fix', tone(border, t))
        if x == border_w or x == w - border_w - 1 or y == h - border_w - 1:
            return 64
        g0 = (w - n_glyph) // 2
        for r0 in glyph_rows:
            if g0 - 1 <= x <= g0 + n_glyph and r0 - 1 <= y <= r0 + n_glyph:
                if x in (g0 - 1, g0 + n_glyph) or y in (r0 - 1, r0 + n_glyph):
                    return 64
                return ('glyph', 160, (x - g0, y - r0))
        return field(ctx)
    return f


def chequy(s=5):
    return lambda ctx: 160 if ((ctx.x // s) + (ctx.y // s)) % 2 == 0 else (128 if (ctx.x % s and ctx.y % s) else 96)


def barry(s=4, wavy=False):
    def f(ctx):
        yy = ctx.y + (int(round(1.2 * math.sin(ctx.x * 0.9))) if wavy else 0)
        k = (yy // s) % 2
        if yy % s == 0:
            return 192 if k == 0 else 96
        return 160 if k == 0 else 96 if yy % s == s - 1 else 128
    return f


def plume(ctx):
    if ctx.face == 'top':
        return 192 if ctx.x % 2 else 160
    if ctx.face == 'bottom':
        return 96
    k = (ctx.x + ctx.y // 2) % 3
    g = (192, 160, 128)[k]
    if ctx.y == ctx.h - 1:
        g = 96
    return g


def band(ctx):
    if ctx.face in ('top', 'bottom'):
        return 128 if ctx.face == 'top' else 96
    if ctx.y == 0:
        return 192
    if ctx.y == ctx.h - 1:
        return 96
    return 160 if (ctx.x // 3) % 2 == 0 else 128


def wraps(ctx):
    if ctx.face in ('top', 'bottom'):
        return 96
    k = (ctx.x + ctx.y) % 4
    return (192, 160, 128, 96)[k]


def fin(ctx):
    # bronze ribs every third texel over the team membrane
    if ctx.face in ('west', 'east') and ctx.x % 4 == 0:
        return ('fix', tone(BRONZE, 0.75 if ctx.y < 2 else 0.5))
    if ctx.face == 'top':
        return ('fix', tone(BRONZE, 0.85))
    return 160 if ctx.y < ctx.h // 2 else 128


CLOTH_PAINT = {
    'ic_tabard': heraldic(chequy(5), EMBER, 9, glyph_rows=(4,)),
    'wm_kilt': heraldic(barry(4), OCHRE, 9, glyph_rows=(4,)),
    'tc_tabard': heraldic(barry(4, wavy=True), SEA, 9, glyph_rows=(4,)),
    'plume': plume, 'band': band, 'wraps': wraps, 'fin': fin,
}


def team_footprints(exclude):
    """The cloth layout footprints of every team part in art/parts except `exclude`: {(part, i): (uv, size)}."""
    out = {}
    for f in sorted((ART / 'parts').glob('*.json')):
        p = json.loads(f.read_text(encoding='utf-8'))
        if p.get('team') and p['part'] not in exclude:
            for i, b in enumerate(p['boxes']):
                out[(p['part'], i)] = (b['uv'], b['size'])
    return out


def paint_cloth():
    """Regenerate the cloth template and the glyph masks: keep the retained team parts' pixels, clear everything else
    (the retired pennant and standard), pack and paint the House accent parts. Returns {(part, i): uv}."""
    keep = team_footprints(set(TEAM) | set(FLAGS))
    tpl = Image.open(ART / 'team/team_cloth.png').convert('RGBA')
    kinds = ('sun', 'moon', 'star', 'wave')
    masks = {k: Image.open(ART / ('team/glyph_%s.png' % k)).convert('RGBA') for k in kinds}
    W, H = tpl.size
    keepmap = Image.new('L', (W, H), 0)
    kp = keepmap.load()
    blocks = []
    for (uv, size) in keep.values():
        for r in footprint(uv, size):
            for y in range(r[1] * CD, r[3] * CD):
                for x in range(r[0] * CD, r[2] * CD):
                    kp[x, y] = 255
        blocks.append((uv[0], uv[1], uv[0] + 2 * (size[0] + size[2]), uv[1] + size[2] + size[1]))
    clear = (0, 0, 0, 0)
    for im in [tpl] + list(masks.values()):
        px = im.load()
        for y in range(H):
            for x in range(W):
                if not kp[x, y]:
                    px[x, y] = clear
    pk = Packer(blocks)
    uvs = {}
    order = sorted(((n, i, b) for n, t in TEAM.items() for i, b in enumerate(t[3])),
                   key=lambda e: -(2 * (e[2]['size'][0] + e[2]['size'][2])) * (e[2]['size'][2] + e[2]['size'][1]))
    for n, i, b in order:
        uvs[(n, i)] = pk.place(b['size'])
    gl = {k: glyph_mask(k) for k in kinds}
    tp = tpl.load()
    mp = {k: m.load() for k, m in masks.items()}
    rng = random.Random('team-cloth')
    for n, (bone, helm, note, bxs, painter) in TEAM.items():
        paint = CLOTH_PAINT[painter]
        for i, b in enumerate(bxs):
            ctx = Ctx()
            ctx.stage, ctx.rng, ctx.note, ctx.bi = 1, rng, b['note'], i
            outer = 'south' if b['note'].startswith('back') else 'north'
            for fname, (x0, y0, x1, y1) in face_rects(uvs[(n, i)], b['size'], CD).items():
                ctx.face, ctx.w, ctx.h = fname, x1 - x0, y1 - y0
                for yy in range(y0, y1):
                    for xx in range(x0, x1):
                        ctx.x, ctx.y = xx - x0, yy - y0
                        CtxX.outer, CtxX.inner = outer, ('north' if outer == 'south' else 'south')
                        v = paint(XCtx(ctx))
                        glyph = None
                        if isinstance(v, tuple) and v[0] == 'fix':
                            tp[xx, yy] = safe(v[1]) + (255,)
                            continue
                        if isinstance(v, tuple) and v[0] == 'glyph':
                            v, glyph = v[1], v[2]
                        tp[xx, yy] = FIELD[v] + (255,)
                        if glyph is not None:
                            for k in kinds:
                                if glyph in gl[k]:
                                    mp[k][xx, yy] = (255, 255, 255, 255)
    tpl.save(ART / 'team/team_cloth.png')
    for k, m in masks.items():
        m.save(ART / ('team/glyph_%s.png' % k))
    return uvs


class CtxX:
    outer = 'north'
    inner = 'south'


class XCtx:
    """A Ctx view that also answers outer and inner (which face of a one-thick flap faces out)."""
    def __init__(self, c):
        self._c = c

    def __getattr__(self, k):
        if k in ('outer', 'inner'):
            return getattr(CtxX, k)
        return getattr(self._c, k)


# ---------------------------------------------------------------- skin painting

def face_rects(uv, size, k):
    (u, v), (w, h, d) = uv, size
    return {n: tuple(k * c for c in r) for n, r in {
        'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
        'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
        'east': (u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}.items()}


def hearthguard_mats(stage):
    iron = IRON if stage >= 2 else SCORCH
    rim = BRASS if stage == 3 else None
    plate = rivet_metal(iron, BRASS if stage >= 2 else IRON, every=5, rim=rim)
    big = rivet_metal(iron, BRASS if stage >= 2 else IRON, every=6, rim=rim, seam=0)
    coat = scales(SCORCH if stage == 1 else IRON, BRASS if stage == 3 else None)
    sleeve = mail(MAIL)
    gaunt = metal(iron, seam=3, scuff=0.04)
    arm = limb(metal(iron, seam=4, rim=rim), limb(sleeve, gaunt, 0.62), 0.38)
    belt = leather(LEATH, stitch=tone(BRASS, 0.7))
    return {
        'body': coat,
        'right_arm': arm, 'left_arm': arm,
        'arms_folded': by_box(limb(metal(iron, seam=4, rim=rim), sleeve, 0.45), bands(metal(iron, seam=3), [(0.0, 0.25)],
                                                                                   leather(EMBER, base=0.5)),
                              limb(metal(iron, seam=4, rim=rim), sleeve, 0.45)),
        'right_leg': limb(mail(MAIL), metal(iron, seam=4, rim=rim), 0.5),
        'left_leg': limb(mail(MAIL), metal(iron, seam=4, rim=rim), 0.5),
        'box_helm': by_note({'comb': metal(BRASS if stage >= 2 else iron, scuff=0.03), 'brow': metal(BRASS if stage >= 2 else iron),
                             'nasal': metal(BRASS if stage >= 2 else iron)}, rivet_metal(IRON, BRASS if stage >= 2 else IRON, every=4,
                                                                                            rim=rim, base=0.6)),
        'braided_beard': by_note({'ring': metal(BRASS, bevel=True)}, braid(COPPER)),
        'slab_pauldrons': by_note({'boss': metal(BRASS, base=0.6), 'spike': horn(IRON, rings=9)},
                                  rivet_metal(IRON, BRASS if stage >= 2 else IRON, every=5, rim=BRASS if stage >= 2 else None, base=0.6)),
        'ic_breastplate': by_note({'fauld': metal(iron, rim=rim, scuff=0.06)}, big),
        'ic_belt': by_note({'buckle': metal(BRASS, base=0.65), 'pouch': leather(LEATH, base=0.45)}, belt),
        'scale_coat_skirt': by_note({'tasset': plate}, coat),
        'ic_greave': metal(iron, rim=rim, seam=0, base=0.6),
        'ic_greave_left': metal(iron, rim=rim, seam=0, base=0.6),
        'helm_horns': horn(BONE),
    }


def tusk_mats(stage):
    hide = fur(FUR)
    jerk = leather(HIDE, stitch=tone(OCHRE, 0.7), straps=[(0.15, 0.3), (0.55, 0.68)], buckle=BONE)
    bracer = leather(LEATH, straps=[(0.2, 0.35), (0.7, 0.85)]) if stage == 1 else metal(IRON if stage == 3 else SCORCH, seam=3)
    hoof = solid(ramp('1A1410', '241C16', '30261E', '3E322A', '4E4034'), 0.4)
    trousers = cloth_skin(HIDE, 5, 0.4)
    return {
        'body': jerk,
        'right_arm': limb(hide, limb(bracer, solid(FUR, 0.4), 0.8), 0.45),
        'left_arm': limb(hide, limb(bracer, solid(FUR, 0.4), 0.8), 0.45),
        'arms_folded': hide,
        'right_leg': limb(trousers, hoof, 0.82), 'left_leg': limb(trousers, hoof, 0.82),
        'boar_snout': lambda ctx: (jit(ctx, tone(ramp('2A1A14', '3A2620', '5E4034', '7E5A4A', '9C786A'),
                                                  0.0 if (ctx.note == 'nose disc' and ctx.face == 'north' and ctx.x in (ctx.w // 4, ctx.w * 3 // 4 - 1)
                                                          and ctx.h // 3 <= ctx.y < ctx.h * 2 // 3) else side_t(ctx, 0.6 if ctx.note == 'snout' else 0.5, 0.2)), 0.03)),
        'tusks': horn(BONE),
        'leather_cap': by_note({'ear': hide, 'bar': rivet_metal(IRON if stage >= 2 else SCORCH, BRASS, every=3, rows=(1,))},
                               leather(HIDE, stitch=tone(OCHRE, 0.75) if stage >= 2 else None, base=0.5)),
        'hide_mantle': fur(mix_ramp(FUR, HIDE, 0.4)),
        'wm_pauldron': by_note({'spike': horn(BONE, rings=3)}, bands(leather(LEATH, stitch=tone(OCHRE, 0.7), base=0.55),
                                                                     [(0.4, 0.6)], solid(OCHRE, 0.5))),
        'wm_belt': by_note({'buckle': horn(BONE, rings=9, tip_light=False), 'tusk': horn(BONE)}, leather(LEATH, stitch=tone(OCHRE, 0.7))),
        'wm_shin': leather(LEATH, straps=[(0.15, 0.35), (0.6, 0.8)], stitch=tone(OCHRE, 0.7)),
        'wm_shin_left': leather(LEATH, straps=[(0.15, 0.35), (0.6, 0.8)], stitch=tone(OCHRE, 0.7)),
        'tusk_crown': horn(BONE),
    }


def mix_ramp(a, b, t):
    return [mix(x, y, t) for x, y in zip(a, b)]


def tide_mats(stage):
    hauberk = mail(mix_ramp(MAIL, VERD, 0.45), 0.03 if stage == 1 else 0.06)
    bronze = VERD if stage < 3 else mix_ramp(VERD, BRONZE, 0.5)
    cuir = scales(bronze, SEA if stage >= 2 else None)
    skin = solid(SKIN_TIDE, 0.5)
    return {
        'body': hauberk,
        'right_arm': limb(hauberk, skin, 0.75), 'left_arm': limb(hauberk, skin, 0.75),
        'arms_folded': hauberk,
        'right_leg': limb(cloth_skin(NAVY, 5, 0.5), skin, 0.9), 'left_leg': limb(cloth_skin(NAVY, 5, 0.5), skin, 0.9),
        'finned_cap': by_note({'fin guard': metal(bronze, seam=3, base=0.6), 'brow': rivet_metal(BRONZE, CORAL, every=3, rows=(1,))},
                              rivet_metal(bronze, BRONZE, every=5, rim=SEA if stage >= 2 else None, base=0.6)),
        'coral_pauldrons': coral,
        'tc_cuirass': by_note({'belt': leather(mix_ramp(LEATH, SEA, 0.5), stitch=tone(CORAL, 0.7)), 'clasp': shell}, cuir),
        'scallop_shell': shell,
        'tc_bracer': metal(bronze, seam=3, rim=SEA if stage >= 2 else None),
        'tc_bracer_left': metal(bronze, seam=3, rim=SEA if stage >= 2 else None),
        'tc_greave': by_note({'boss': shell}, rivet_metal(bronze, BRONZE, every=5, rim=SEA if stage >= 2 else None, base=0.6)),
        'tc_greave_left': by_note({'boss': shell}, rivet_metal(bronze, BRONZE, every=5, rim=SEA if stage >= 2 else None, base=0.6)),
        'coral_crown': coral,
    }


MATS = {'ic_hearthguard': hearthguard_mats, 'wm_tusk': tusk_mats, 'tc_tideguard': tide_mats}


def baked_bones(troop):
    sys.path.insert(0, str(ROOT / 'tools'))
    import build_skins as B
    cat = B.load(B.ROOT)
    return B.bake(cat, troop)


def paint_skins(troop):
    model = baked_bones(troop)
    notes = {p: [b['note'] for b in v[3]] for p, v in PARTS.items()}
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
                ctx.stage, ctx.rng, ctx.bi = stage, rng, bi
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
    trim = {'ic_hearthguard': tone(BRASS, 0.9), 'wm_tusk': tone(OCHRE, 0.9), 'tc_tideguard': hexc('B7A777')}[troop]
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


# single colours for the faces and the weapons
W_SOOT, W_BRASS, W_COPPER = hexc('34373B'), hexc('7F6232'), hexc('7A4A2E')
W_RUSSET, W_BARK, W_CLAY = hexc('7A4127'), hexc('4A3626'), hexc('C8B48A')
W_VERD, W_NAVY, W_BONE, W_CORAL = hexc('4E7A6A'), hexc('1E2C44'), hexc('E4DECB'), hexc('DCD2C2')


# ---------------------------------------------------------------- faces

FACE_SETS = {
    # skin, hair/brow, eye, extra (beard / tusk-fur / gill)
    'ic_hearthguard': [(hexc('B07E62'), W_COPPER, hexc('2A3440')), (hexc('9C6A50'), hexc('4A3226'), hexc('3A2A20')),
                       (hexc('B88E74'), hexc('8E8A84'), hexc('2A3440')), (hexc('A27258'), hexc('8A4A2A'), hexc('34402A'))],
    'wm_tusk': [(W_RUSSET, W_BARK, hexc('1E1410')), (mix(W_RUSSET, W_BARK, 0.4), mul(W_BARK, 0.8), hexc('3A1E10')),
                (mix(W_RUSSET, W_CLAY, 0.2), W_BARK, hexc('1E1410')), (mul(W_RUSSET, 0.85), hexc('2E2420'), hexc('4A2A10'))],
    'tc_tideguard': [(hexc('6F8A80'), W_NAVY, hexc('C8D4C0')), (hexc('7C8E86'), hexc('2A3A3A'), hexc('101820')),
                     (hexc('64807A'), mix(W_NAVY, W_VERD, 0.4), hexc('D0C890')), (hexc('829A8C'), W_NAVY, hexc('101820'))],
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
                        c = mix(hair, W_COPPER, 0.5)
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
W_IRON, STEEL, LEATHER = hexc('3E4146'), hexc('8C9096'), hexc('3E2E22')
W_EMBER, W_OCHRE, W_SEA = tone(EMBER, 0.45), tone(OCHRE, 0.55), tone(SEA, 0.45)   # House grip wraps
GILT = hexc('A08852')


def hearth_axe(tier):
    c = [cube('haft', [-0.6, -7, -0.6], [0.6, 15, 0.6], 'wood', WOOD),
         cube('butt', [-0.9, -8, -0.9], [0.9, -7, 0.9], 'iron', W_IRON),
         cube('grip', [-0.75, -2.5, -0.75], [0.75, 2.5, 0.75], 'leather', W_EMBER),
         cube('socket', [-1.2, 11, -1], [1.2, 16, 1], 'iron', W_BRASS if tier == 3 else W_IRON),
         cube('spike', [-0.5, 16, -0.5], [0.5, 18, 0.5], 'steel', STEEL),
         cube('blade', [-6, 11.5, -0.4], [-1.2, 15.5, 0.4], 'iron', W_SOOT),
         cube('beard', [-7.5, 8, -0.35], [-5, 16, 0.35], 'iron', W_SOOT),
         cube('bit', [-8.2, 7.5, -0.25], [-7.5, 16.5, 0.25], 'steel', STEEL),
         cube('back_spike', [1.2, 12.5, -0.6], [3, 14.5, 0.6], 'iron', W_IRON)]
    if tier >= 2:
        c += [cube('band_low', [-0.8, 8.5, -0.8], [0.8, 9.5, 0.8], 'gilt', W_BRASS),
              cube('band_top', [-0.8, 3.5, -0.8], [0.8, 4.2, 0.8], 'gilt', W_BRASS),
              cube('back_spike_tip', [3, 13, -0.4], [4.2, 14, 0.4], 'steel', STEEL)]
    if tier >= 3:
        c += [cube('crown_spike', [-0.3, 18, -0.3], [0.3, 19.5, 0.3], 'gilt', GILT),
              cube('rune_plate', [-4.5, 12.5, -0.5], [-2.5, 14.5, 0.5], 'gilt', GILT),
              cube('butt_spike', [-0.4, -9.5, -0.4], [0.4, -8, 0.4], 'gilt', GILT)]
    return c


def boar_spear(tier):
    c = [cube('shaft', [-0.5, -9, -0.5], [0.5, 22, 0.5], 'wood', WOOD),
         cube('butt', [-0.7, -10, -0.7], [0.7, -9, 0.7], 'iron', W_IRON),
         cube('grip', [-0.65, -2.5, -0.65], [0.65, 2.5, 0.65], 'leather', W_OCHRE),
         cube('socket', [-0.8, 21, -0.8], [0.8, 24, 0.8], 'iron', W_IRON),
         cube('crossbar', [-3, 22, -0.4], [3, 23, 0.4], 'iron', W_IRON),
         cube('head', [-1.5, 24, -0.3], [1.5, 30, 0.3], 'steel', STEEL),
         cube('head_tip', [-0.8, 30, -0.25], [0.8, 32.5, 0.25], 'steel', STEEL),
         cube('point', [-0.3, 32.5, -0.2], [0.3, 33.5, 0.2], 'steel', STEEL),
         cube('tassel_cord', [2.4, 19, -0.2], [2.8, 22, 0.2], 'linen', W_OCHRE),
         cube('tassel', [2, 16.5, -0.4], [3.2, 19, 0.4], 'bone', W_OCHRE)]
    if tier >= 2:
        c += [cube('collar', [-0.75, 15, -0.75], [0.75, 16, 0.75], 'iron', W_IRON),
              cube('lug_r', [-4, 21.8, -0.3], [-3, 23.2, 0.3], 'iron', W_IRON),
              cube('lug_l', [3, 21.8, -0.3], [4, 23.2, 0.3], 'iron', W_IRON)]
    if tier >= 3:
        c += [cube('tusk_r', [-5.2, 22.2, -0.25], [-4, 25, 0.25], 'bone', W_BONE),
              cube('tusk_l', [4, 22.2, -0.25], [5.2, 25, 0.25], 'bone', W_BONE),
              cube('tassel_cord_2', [-2.8, 19, -0.2], [-2.4, 22, 0.2], 'linen', W_OCHRE),
              cube('tassel_2', [-3.2, 16.5, -0.4], [-2, 19, 0.4], 'bone', W_OCHRE)]
    return c


def coral_trident(tier):
    c = [cube('shaft', [-0.5, -9, -0.5], [0.5, 20, 0.5], 'wood', WOOD_DARK),
         cube('butt', [-0.8, -10, -0.8], [0.8, -9, 0.8], 'bone', W_CORAL),
         cube('grip', [-0.65, -2.5, -0.65], [0.65, 2.5, 0.65], 'leather', W_SEA),
         cube('collar', [-0.9, 20, -0.9], [0.9, 22, 0.9], 'iron', W_VERD),
         cube('crossbar', [-4, 22, -0.6], [4, 23.5, 0.6], 'bone', W_CORAL),
         cube('prong_c', [-0.5, 23.5, -0.5], [0.5, 32, 0.5], 'bone', W_CORAL),
         cube('prong_r', [-4, 23.5, -0.5], [-3, 29, 0.5], 'bone', W_CORAL),
         cube('prong_l', [3, 23.5, -0.5], [4, 29, 0.5], 'bone', W_CORAL),
         cube('tip_c', [-0.3, 32, -0.3], [0.3, 34, 0.3], 'steel', STEEL),
         cube('tip_r', [-3.8, 29, -0.35], [-3.2, 31, 0.35], 'steel', STEEL),
         cube('tip_l', [3.2, 29, -0.35], [3.8, 31, 0.35], 'steel', STEEL)]
    if tier >= 2:
        c += [cube('barb_r', [-5, 26, -0.4], [-4, 27, 0.4], 'bone', W_CORAL),
              cube('barb_l', [4, 26, -0.4], [5, 27, 0.4], 'bone', W_CORAL),
              cube('band', [-0.7, 14, -0.7], [0.7, 15, 0.7], 'iron', W_VERD)]
    if tier >= 3:
        c += [cube('pearl', [-0.7, 23.2, -0.75], [0.7, 25, 0.75], 'glass', hexc('D8DCD4')),
              cube('barb_c_r', [-1.5, 28, -0.3], [-0.5, 29, 0.3], 'bone', W_CORAL),
              cube('barb_c_l', [0.5, 28, -0.3], [1.5, 29, 0.3], 'bone', W_CORAL)]
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
    (folder / (name + '.bbmodel')).write_text(json.dumps(model, indent=1), encoding='utf-8', newline='\n')
    write_json(folder / (name + '.json'), {'weapon': name, 'thickness': thickness,
                                           'grip': {'offset': [0, 0, 0], 'rotation': rotation,
                                                    'note': 'the grip\'s centre is the model origin (the fist covers y -2..+2); ' + grip_note},
                                           'note': notes[tier - 1]})


def main():
    for wid in WEAPONS:
        for tier in (1, 2, 3):
            write_weapon(wid, tier)
    team_uv = paint_cloth()
    build_sources(team_uv)
    for troop in TROOPS:   # placeholders so the catalog loads before the first paint
        for f, size in [('%s_s%d.png' % (troop, s), (SIZE[0] * D, SIZE[1] * D)) for s in (1, 2, 3)] + [('%s_faces.png' % troop, (64, 128))]:
            if not (ART / 'troops' / f).is_file():
                Image.new('RGBA', size, (0, 0, 0, 0)).save(ART / 'troops' / f)
    for troop in TROOPS:
        model, skins = paint_skins(troop)
        paint_veteran(troop, model, skins)
        paint_faces(troop)
    print('paint_house_troops: wrote 3 troops, %d skin parts, %d team parts, 9 weapons' % (len(PARTS), len(TEAM)))


if __name__ == '__main__':
    main()
