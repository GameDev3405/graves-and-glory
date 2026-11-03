"""House identity set, DRAFT (BOARD 4B.5): per House a mark (16 and 32 px), a recruiter banner cloth, a camp brazier and a
portrait card frame, all hand-designed shapes lit from the top left at 1 texel per pixel.

Writes art/house/<house>/{mark16,mark32,banner,brazier,frame}.png (source drafts, not in the jar) and, with --wire,
refreshes the one slot the code already has: textures/gui/house/<house>.png (the 16 px glyph named by design.json
houses[].glyph). The recruit banners (gui/recruit/banner-*.png, painted by build_ui_art.py), the camp tents (block
mcfunctions with vanilla banners) and the table frames (gui/table/frame-*.png) have no per-House texture slot, so
their drafts stay in art/house/ until Kort picks a direction.  --sheet PATH writes the review contact sheet.

Palettes follow docs/ART-STYLE-GUIDE.md 10.1 (Kort 2026-10-30): Graveborn dark grave violet, Tidecallers sea blue.
No flag, pennant or standard rides on any troop's back; these banners hang in the camp only.
Deterministic: python tools/paint_house_identity.py [--wire] [--sheet work/house-identity-sheet.png]"""
import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art' / 'house'
GLYPHS = ROOT / 'src/main/resources/assets/gravesandglory/textures/gui/house'
HOUSES = ['gilded', 'graveborn', 'wildmoot', 'tidecallers', 'ironclad']
SS = 4


def rgb(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def ramp(s):
    return [rgb(t) for t in s.split()]


GOLD = ramp('2C1909 583513 89581B B8852A DCB145 F2D877')
STEEL = ramp('15161F 282C3B 414A5D 617086 8A97AB B9C4D1')
LINEN = ramp('3A3024 675641 958262 BFAC86 DDCFAA F5EDD8')
IRON = ramp('121316 1B1D21 30343A 4C5158 6F757D A3A9B0')
BONE = ramp('4A4538 7E7664 A49C86 C6BEA6 DDD7C3 F0ECE0')
BRASS = ramp('2A1C0C 3E2B12 664A20 8E6C36 B3905A D6BE8E')
WOOD = ramp('1C100B 3E261F 563C2E 6E5440 867057 9E8E72')
VIOLET = ramp('1E1528 362645 54406A 7A6494 A28FBA D2C6D8')
SOUL = ramp('362645 7A6494 A28FBA D2C6D8 EAE2F2 FAF7FC')
SEA = ramp('0A1426 11264A 1A3F6A 2A5E8A 5E88A6 9DB8C4')
FOAM = ramp('2A5E8A 5E88A6 9DB8C4 CFE4E4 CFF3E4 F2FFF8')
OCHRE = ramp('3E2208 683E14 8E5C20 B07C36 CC9C5E E0BC86')
FUR = ramp('24170E 43291A 673F25 885836 A97650 C49470')
EMBER = ramp('2E0E08 52190E 7A2814 9A3C20 B85E40 D9824F')
FIRE = ramp('52190E 9A3C20 E2702A F2A24A FBD58A FFF1C8')
GOLDFIRE = ramp('89581B B8852A DCB145 F2D877 FFF0A8 FFFBE0')
SLATE = ramp('0E1320 18202F 22304A 2F4262 405A82 5E7AA2')


class Art:
    """A transparent canvas painted layer by layer. Design coordinates are in a 32-unit space scaled to the canvas."""

    def __init__(self, w, h, k=1.0, edge=None):
        self.w, self.h, self.k = w, h, k
        self.px = {}
        self.edge = edge

    # --- masks ---------------------------------------------------------------------------------------------------
    def m(self, fn):
        im = Image.new('L', (self.w * SS, self.h * SS), 0)
        d = ImageDraw.Draw(im)
        s = self.k * SS
        fn(d, lambda v: v * s)
        small = im.resize((self.w, self.h), Image.BOX)
        data = small.load()
        return {(x, y) for y in range(self.h) for x in range(self.w) if data[x, y] >= 100}

    def poly(self, pts):
        return self.m(lambda d, S: d.polygon([(S(x), S(y)) for x, y in pts], fill=255))

    def ell(self, cx, cy, rx, ry):
        return self.m(lambda d, S: d.ellipse([S(cx - rx), S(cy - ry), S(cx + rx), S(cy + ry)], fill=255))

    def rect(self, x0, y0, x1, y1):
        return self.m(lambda d, S: d.rectangle([S(x0), S(y0), S(x1) - 1, S(y1) - 1], fill=255))

    def line(self, pts, w=1.0):
        w = max(w, 1.0 / self.k)
        def f(d, S):
            d.line([(S(x), S(y)) for x, y in pts], fill=255, width=max(1, int(round(S(w)))), joint='curve')
        return self.m(f)

    # --- painting ------------------------------------------------------------------------------------------------
    def paint(self, mask, rp, base=2, lit=True, grain=0):
        for (x, y) in mask:
            t = base
            if lit:
                tl = (x - 1, y) not in mask or (x, y - 1) not in mask
                br = (x + 1, y) not in mask or (x, y + 1) not in mask
                if tl and not br:
                    t = base + 1
                elif br and not tl:
                    t = base - 1
            if grain and ((x * 7 + y * 13 + x * y) % 11) < grain:
                t += 1 if (x + y) % 2 else -1
            t = max(0, min(len(rp) - 1, t))
            self.px[(x, y)] = rp[t]
        return mask

    def outline(self, colour):
        edge = set()
        for (x, y) in self.px:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                p = (x + dx, y + dy)
                if p not in self.px and 0 <= p[0] < self.w and 0 <= p[1] < self.h:
                    edge.add(p)
        for p in edge:
            self.px[p] = colour

    def image(self):
        im = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        for p, c in self.px.items():
            im.putpixel(p, c)
        return im


def mirror_x(mask, w):
    return mask | {(w - 1 - x, y) for x, y in mask}


def grain_cloth(art, mask, rp, base, strength=2):
    """woven cloth: alternate rows and a sparse stitch grain, one tone either way"""
    for (x, y) in mask:
        c = art.px.get((x, y))
        if c is None or c not in rp:
            continue
        t = rp.index(c)
        if (x + (y % 2) * 2) % 4 == 0 and strength:
            t = min(len(rp) - 1, t + 1) if y % 3 else max(0, t - 1)
        art.px[(x, y)] = rp[t]


# ====================================================== marks (16 and 32 px, the same shapes at both sizes) =======
def mark(house, n):
    a = Art(n, n, n / 32.0)
    P, E, L, R, O = a.poly, a.ell, a.line, a.rect, a.paint
    if house == 'gilded':
        # heater shield under a radiant sun: Squire's pale and sun-white
        for ang in range(0, 360, 45):
            import math
            ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
            O(L([(16 + ca * 5.6, 8.5 + sa * 5.6), (16 + ca * 8, 8.5 + sa * 8)], 1.6), GOLD, 4, lit=False)
        O(E(16, 8.5, 4.4, 4.4), GOLD, 3)
        O(E(16, 8.5, 2.4, 2.4), GOLD, 5, lit=False)
        shield = [(6, 12), (26, 12), (26, 20), (21, 27), (16, 30), (11, 27), (6, 20)]
        O(P(shield), GOLD, 2)
        O(P([(8, 14), (24, 14), (24, 19.5), (20, 25.5), (16, 28), (12, 25.5), (8, 19.5)]), STEEL, 2)
        O(R(14, 14, 18, 27), GOLD, 3)
        O(R(8, 17, 24, 19), GOLD, 3)
        a.outline(GOLD[0])
    elif house == 'graveborn':
        # a ward-stone with a hollow crown, its eye a soul-flame
        stone = [(7, 30), (7, 14), (9, 9), (16, 6), (23, 9), (25, 14), (25, 30)]
        for x0 in (8, 13.5, 19):
            O(P([(x0, 8), (x0 + 2.5, 8), (x0 + 1.25, 1.5)]), BONE, 3)
        O(P(stone), IRON, 3)
        O(P([(10, 28), (10, 15), (11.5, 11.5), (16, 9.5), (20.5, 11.5), (22, 15), (22, 28)]), VIOLET, 1)
        O(P([(16, 12.5), (19.5, 18), (16, 25), (12.5, 18)]), SOUL, 3, lit=False)
        O(P([(16, 15.5), (17.8, 19), (16, 22.5), (14.2, 19)]), SOUL, 5, lit=False)
        O(R(5, 28, 27, 31), IRON, 2)
        a.outline(VIOLET[0])
    elif house == 'wildmoot':
        # antler crown over a boar skull with its tusks
        for sx in (1, -1):
            def X(x):
                return 16 + sx * (x - 16)
            O(L([(X(21), 15), (X(24), 11), (X(25), 6), (X(24.5), 2.5)], 2.4), OCHRE, 3)
            O(L([(X(24), 11), (X(28), 9.5), (X(29), 5)], 1.8), OCHRE, 3)
            O(L([(X(24.2), 7.5), (X(21.5), 4.5)], 1.6), OCHRE, 3)
            O(L([(X(22), 16.5), (X(27), 21), (X(25.5), 26)], 2.2), BONE, 4)
        O(P([(10, 15), (22, 15), (24, 21), (21, 29), (18, 31), (14, 31), (11, 29), (8, 21)]), BONE, 3)
        O(R(13, 25, 19, 31), BONE, 2)
        O(E(12.5, 20, 2.2, 2.4), FUR, 0, lit=False)
        O(E(19.5, 20, 2.2, 2.4), FUR, 0, lit=False)
        O(R(15, 24, 17, 29), FUR, 1, lit=False)
        a.outline(FUR[0])
    elif house == 'tidecallers':
        # a roundel of the deep, a trident rising out of the foam
        O(E(16, 16, 14, 14), SEA, 2)
        O(E(16, 16, 11.5, 11.5), SEA, 1)
        O(L([(16, 5), (16, 27)], 2.4), FOAM, 3)
        for x in (9.5, 22.5):
            O(L([(x, 6), (x, 13)], 2.2), FOAM, 3)
            O(P([(x - 2, 8), (x, 3.5), (x + 2, 8)]), FOAM, 3)
        O(P([(14, 6.5), (16, 2.5), (18, 6.5)]), FOAM, 4)
        O(L([(9.5, 12.5), (13, 15), (16, 15.8), (19, 15), (22.5, 12.5)], 2.2), FOAM, 3)
        O(P([(3, 24), (7, 21.5), (11, 23), (16, 21), (21, 23), (25, 21.5), (29, 24), (26, 28), (6, 28)]), SEA, 4)
        O(L([(6, 24.5), (10, 23), (14, 24.5), (18, 23), (22, 24.5), (26, 23)], 1.4), FOAM, 4, lit=False)
        a.outline(SEA[0])
    else:  # ironclad
        # an anvil with a spark in the ember
        O(P([(3, 9), (24, 9), (30, 12), (22, 15), (21, 19), (24, 23), (24, 27), (8, 27), (8, 23), (11, 19), (10, 15), (3, 12)]), IRON, 3)
        O(P([(5, 10.5), (23, 10.5), (26, 12), (21, 13.2), (6, 13.2)]), IRON, 5, lit=False)
        O(R(7, 27, 25, 31), BRASS, 3)
        O(R(11, 20, 21, 22), BRASS, 2, lit=False)
        O(P([(16, 1), (18, 5), (16, 8), (14, 5)]), FIRE, 3, lit=False)
        O(P([(16, 3), (17, 5.5), (16, 7.2), (15, 5.5)]), FIRE, 5, lit=False)
        a.outline(IRON[0])
    return a.image()


# ====================================================== recruiter banner (24 x 48) ==================================
BANNER = {
    # house: (cloth ramp, border ramp, hem style, trim)
    'gilded': dict(cloth=LINEN, base=4, border=GOLD, bbase=3, hem='swallow', rod=BRASS),
    'graveborn': dict(cloth=VIOLET, base=1, border=BONE, bbase=2, hem='torn', rod=IRON),
    'wildmoot': dict(cloth=FUR, base=2, border=OCHRE, bbase=3, hem='hide', rod=WOOD),
    'tidecallers': dict(cloth=SEA, base=2, border=FOAM, bbase=1, hem='frayed', rod=WOOD),
    'ironclad': dict(cloth=IRON, base=2, border=BRASS, bbase=3, hem='flat', rod=BRASS),
}


def banner(house):
    c = BANNER[house]
    a = Art(24, 48)
    P, R, L, O = a.poly, a.rect, a.line, a.paint
    top, bot = 4, 42
    hem = c['hem']
    if hem == 'swallow':
        cloth = P([(2, top), (22, top), (22, bot + 4), (17, bot), (12, bot - 3), (7, bot), (2, bot + 4)])
    elif hem == 'torn':
        cloth = P([(2, top), (22, top), (22, bot + 2), (19, bot - 2), (17, bot + 5), (14, bot), (11, bot + 3), (9, bot - 1), (6, bot + 6), (3, bot + 1), (2, bot + 2)])
    elif hem == 'hide':
        cloth = P([(2, top), (22, top), (22, bot), (20, bot + 4), (17, bot), (14, bot + 5), (11, bot), (8, bot + 4), (5, bot), (2, bot + 3)])
    elif hem == 'frayed':
        cloth = P([(2, top), (22, top), (22, bot + 1), (20, bot - 1), (19, bot + 4), (16, bot), (14, bot + 5), (11, bot + 1), (9, bot + 4), (6, bot), (4, bot + 3), (2, bot - 1)])
    else:
        cloth = P([(2, top), (22, top), (22, bot), (2, bot)])
    O(cloth, c['cloth'], c['base'])
    # border: inset ring, 2 px
    inner = {p for p in cloth if all((p[0] + dx, p[1] + dy) in cloth for dx in (-2, -1, 0, 1, 2) for dy in (-2, -1, 0, 1, 2))}
    ring = cloth - inner
    ring = {p for p in ring if p[1] < bot - 1 or hem == 'flat'}
    O(ring, c['border'], c['bbase'], lit=False)
    for p in ring:
        if (p[0] - 1, p[1]) not in cloth or (p[0], p[1] - 1) not in cloth:
            a.px[p] = c['border'][min(5, c['bbase'] + 1)]
    # folds: two darker vertical creases with a light edge
    for fx in (7, 16):
        for y in range(top + 2, bot - 1):
            p = (fx, y)
            if p in inner:
                t = c['cloth'].index(a.px[p])
                a.px[p] = c['cloth'][max(0, t - 1)]
                q = (fx + 1, y)
                if q in inner:
                    a.px[q] = c['cloth'][min(5, c['cloth'].index(a.px[q]) + 1)]
    grain_cloth(a, inner, c['cloth'], c['base'])
    # the mark, small, on the field
    mk = mark(house, 16)
    for y in range(16):
        for x in range(16):
            px = mk.getpixel((x, y))
            if px[3]:
                a.px[(4 + x, 10 + y)] = tuple(px)
    # House trim under the mark
    if house == 'gilded':
        for i in range(3):
            O(L([(6, 30 + i * 3), (12, 33 + i * 3), (18, 30 + i * 3)], 1), GOLD, 3, lit=False)
    elif house == 'graveborn':
        for x in (6, 9, 12, 15, 18):
            O(R(x, 29, x + 1, 35 if x % 2 else 33), BONE, 2, lit=False)
    elif house == 'wildmoot':
        for x in (7, 11, 15):
            O(L([(x, 29), (x + 2, 36)], 1.4), BONE, 3, lit=False)
    elif house == 'tidecallers':
        for yy in (30, 34):
            O(L([(5, yy), (8, yy - 1.5), (11, yy), (14, yy - 1.5), (17, yy), (19, yy - 1)], 1.2), FOAM, 3, lit=False)
    else:
        for x in (5, 9, 13, 17):
            O(R(x, 31, x + 2, 33), BRASS, 4, lit=False)
        O(R(4, 35, 20, 36), EMBER, 4, lit=False)
    a.outline(c['cloth'][0])
    # rod, finials, cord
    rod = R(0, 1, 24, 4)
    O(rod, c['rod'], 3)
    O(R(0, 0, 2, 5), BRASS, 4)
    O(R(22, 0, 24, 5), BRASS, 4)
    for x in (5, 12, 19):
        O(R(x, 4, x + 1, 6), c['rod'], 1, lit=False)
    return a.image()


# ====================================================== camp brazier (32 x 48) ======================================
def flame(a, cx, base_y, h, rp, w=5):
    P, O = a.poly, a.paint
    O(P([(cx - w, base_y), (cx - w * 0.9, base_y - h * 0.45), (cx - w * 0.3, base_y - h * 0.6), (cx, base_y - h),
         (cx + w * 0.3, base_y - h * 0.6), (cx + w * 0.9, base_y - h * 0.45), (cx + w, base_y)]), rp, 2, lit=False)
    O(P([(cx - w * 0.55, base_y), (cx - w * 0.45, base_y - h * 0.4), (cx, base_y - h * 0.78),
         (cx + w * 0.45, base_y - h * 0.4), (cx + w * 0.55, base_y)]), rp, 3, lit=False)
    O(P([(cx - w * 0.25, base_y), (cx, base_y - h * 0.45), (cx + w * 0.25, base_y)]), rp, 5, lit=False)


def brazier(house):
    a = Art(32, 48)
    P, R, L, E, O = a.poly, a.rect, a.line, a.ell, a.paint
    if house == 'gilded':
        # a gilt chalice-bowl on a steel column, a sun disc behind the flame
        O(E(16, 14, 9, 9), GOLD, 2)
        O(E(16, 14, 6.5, 6.5), GOLD, 1)
        flame(a, 16, 26, 17, GOLDFIRE, 5.5)
        O(P([(6, 26), (26, 26), (23, 33), (9, 33)]), GOLD, 3)
        O(R(4, 25, 28, 28), GOLD, 4)
        O(R(14, 33, 18, 40), STEEL, 3)
        O(P([(8, 47), (11, 40), (21, 40), (24, 47)]), STEEL, 2)
        O(R(12, 36, 20, 38), GOLD, 3)
        edge = GOLD[0]
    elif house == 'graveborn':
        # an iron ribbed cage on a bone-pale foot, the soul-flame pale violet
        flame(a, 16, 30, 22, SOUL, 5)
        for x in (7, 11.5, 16, 20.5, 25):
            O(L([(x, 22), (x if 7 < x < 25 else x, 36)], 1.4), IRON, 3, lit=False)
        O(L([(6, 36), (26, 36)], 2.4), IRON, 3)
        O(P([(6, 22), (26, 22), (24, 20), (8, 20)]), IRON, 3)
        O(P([(10, 36), (22, 36), (19, 41), (13, 41)]), IRON, 2)
        O(R(14, 40, 18, 44), BONE, 3)
        O(P([(8, 47), (12, 44), (20, 44), (24, 47)]), BONE, 2)
        for x, y in ((6, 8), (25, 12), (9, 4), (22, 5)):
            O(R(x, y, x + 1, y + 1), VIOLET, 4, lit=False)
        edge = VIOLET[0]
    elif house == 'wildmoot':
        # lashed timber tripod over a stone ring, the flame ochre, a boar skull on the lashing
        O(L([(4, 46), (14, 13)], 2.4), WOOD, 3)
        O(L([(28, 46), (18, 13)], 2.4), WOOD, 3)
        O(L([(16, 40), (16, 13)], 2.0), WOOD, 2)
        flame(a, 16, 38, 22, FIRE, 7.5)
        O(R(12, 15, 20, 18), FUR, 3)
        O(E(16, 10.5, 3.6, 3), BONE, 4)
        O(R(14.5, 11.5, 16, 13), FUR, 0, lit=False)
        O(R(16.5, 11.5, 18, 13), FUR, 0, lit=False)
        O(L([(13, 8.5), (10.5, 4.5)], 1.3), OCHRE, 4, lit=False)
        O(L([(19, 8.5), (21.5, 4.5)], 1.3), OCHRE, 4, lit=False)
        for x in (3, 8, 13, 18, 23):
            O(P([(x, 41), (x + 5, 41), (x + 6, 47), (x, 47)]), IRON, 2 if x % 2 else 3)
        edge = FUR[0]
    elif house == 'tidecallers':
        # a giant clam shell bowl on a driftwood post; sea-foam flame; kelp hangs from the lip
        flame(a, 16, 28, 20, FOAM, 5)
        O(P([(3, 27), (7, 24), (12, 26), (16, 23.5), (20, 26), (25, 24), (29, 27), (25, 35), (16, 38), (7, 35)]), BONE, 3)
        for x in (9, 13, 16, 19, 23):
            O(L([(x, 27), (16 + (x - 16) * 0.5, 37)], 1.1), BONE, 1, lit=False)
        O(R(14, 38, 18, 46), WOOD, 2)
        O(P([(8, 47), (12, 44), (20, 44), (24, 47)]), WOOD, 1)
        O(L([(6, 33), (5, 38), (7, 43)], 1.5), SEA, 4, lit=False)
        O(L([(26, 33), (27, 37), (25, 42)], 1.5), SEA, 4, lit=False)
        O(E(16, 30, 2, 1.6), SEA, 5, lit=False)
        edge = SEA[0]
    else:  # ironclad
        # a forge brazier: a smoke-black bowl hooped in brass on an anvil block, ember flame, a flue
        flame(a, 16, 26, 14, FIRE, 6)
        O(P([(5, 24), (27, 24), (24, 34), (8, 34)]), IRON, 2)
        for yy in (26, 31):
            O(R(6 if yy == 26 else 8, yy, 26 if yy == 26 else 24, yy + 1.5), BRASS, 3, lit=False)
        O(P([(7, 34), (25, 34), (28, 38), (4, 38)]), IRON, 3)
        O(R(9, 38, 23, 47), IRON, 2)
        O(R(12, 41, 20, 43), EMBER, 4, lit=False)
        O(R(4, 22, 28, 25), IRON, 4)
        O(R(26, 6, 29, 22), IRON, 2)
        O(R(25, 4, 30, 7), BRASS, 3)
        edge = IRON[0]
    a.outline(edge)
    return a.image()


# ====================================================== portrait card frame (48 x 48, clear centre) =================
def frame(house):
    n = 48
    a = Art(n, n)
    R, P, E, O = a.rect, a.poly, a.ell, a.paint
    box = R(0, 0, n, n) - R(5, 5, n - 5, n - 5)
    if house == 'gilded':
        body = box | R(21, 0, 27, 3) | E(5, 5, 5.2, 5.2) | E(43, 5, 5.2, 5.2) | E(5, 43, 5.2, 5.2) | E(43, 43, 5.2, 5.2)
        O(body, GOLD, 2)
        for x in (5, 43):
            for y in (5, 43):
                O(E(x, y, 2.2, 2.2), GOLD, 4)
        O(P([(24, 0.5), (26.5, 3), (24, 5.5), (21.5, 3)]), STEEL, 4, lit=False)
        for i in range(8, 40, 3):
            for (px, py) in ((i, 1), (i, 46)):
                a.px[(px, py)] = GOLD[4] if i % 6 == 2 else GOLD[2]
            for (px, py) in ((1, i), (46, i)):
                a.px[(px, py)] = GOLD[4] if i % 6 == 2 else GOLD[2]
        edge = GOLD[0]
    elif house == 'graveborn':
        body = box
        for (cx, cy, dx, dy) in ((0, 0, 1, 1), (n, 0, -1, 1), (0, n, 1, -1), (n, n, -1, -1)):
            body |= P([(cx, cy), (cx + dx * 9, cy + dy * 2), (cx + dx * 2, cy + dy * 9)]) | P([(cx + dx * 3, cy + dy * 5), (cx + dx * 11, cy + dy * 9), (cx + dx * 5, cy + dy * 11)])
        O(body, IRON, 3)
        O(R(4, 4, n - 4, 5) | R(4, n - 5, n - 4, n - 4) | R(4, 4, 5, n - 4) | R(n - 5, 4, n - 4, n - 4), VIOLET, 3, lit=False)
        O(P([(24, 0), (27, 4), (24, 9), (21, 4)]), SOUL, 3, lit=False)
        O(P([(24, 2), (25.5, 4), (24, 6.5), (22.5, 4)]), SOUL, 5, lit=False)
        for (cx, cy, dx, dy) in ((0, 0, 1, 1), (n, 0, -1, 1), (0, n, 1, -1), (n, n, -1, -1)):
            O(P([(cx, cy), (cx + dx * 9, cy + dy * 2), (cx + dx * 2, cy + dy * 9)]), BONE, 3)
        edge = VIOLET[0]
    elif house == 'wildmoot':
        body = box
        for (cx, cy, dx, dy) in ((0, 0, 1, 1), (n, 0, -1, 1), (0, n, 1, -1), (n, n, -1, -1)):
            body |= P([(cx, cy + dy * 6), (cx + dx * 5, cy + dy * 6), (cx + dx * 7, cy), (cx + dx * 9, cy + dy * 6), (cx + dx * 14, cy + dy * 4), (cx + dx * 11, cy + dy * 8), (cx + dx * 6, cy + dy * 8)])
        O(body, WOOD, 3, grain=2)
        for i in range(8, 40, 4):
            for (px, py) in ((i, 3), (i, n - 4), (3, i), (n - 4, i)):
                a.px[(px, py)] = OCHRE[3]
                a.px[(px + (1 if px not in (3, n - 4) else 0), py + (1 if py not in (3, n - 4) else 0))] = OCHRE[3]
        for (cx, cy) in ((3, 3), (n - 4, 3), (3, n - 4), (n - 4, n - 4)):
            O(E(cx + 0.5, cy + 0.5, 1.5, 1.5), BONE, 4)
        O(P([(21, 0), (27, 0), (24, 5)]), BONE, 4)
        edge = FUR[0]
    elif house == 'tidecallers':
        body = box
        for i in range(8, 40, 4):
            body |= E(i + 1, 5.5, 2.2, 2.2) | E(i + 1, n - 5.5, 2.2, 2.2) | E(5.5, i + 1, 2.2, 2.2) | E(n - 5.5, i + 1, 2.2, 2.2)
        body |= E(5, 5, 5.5, 5.5) | E(43, 5, 5.5, 5.5) | E(5, 43, 5.5, 5.5) | E(43, 43, 5.5, 5.5)
        O(body, SEA, 2)
        O(R(2, 2, n - 2, n - 2) - R(3, 3, n - 3, n - 3), SEA, 4, lit=False)
        for (x, y) in ((5, 5), (43, 5), (5, 43), (43, 43)):
            O(E(x, y, 3, 3), FOAM, 2)
            O(E(x, y, 1.3, 1.3), FOAM, 5, lit=False)
        O(E(24, 2.5, 3, 3), FOAM, 4)
        edge = SEA[0]
    else:  # ironclad
        body = box | R(0, 0, 12, 12) | R(n - 12, 0, n, 12) | R(0, n - 12, 12, n) | R(n - 12, n - 12, n, n)
        O(body, IRON, 3)
        for (x0, y0) in ((0, 0), (n - 12, 0), (0, n - 12), (n - 12, n - 12)):
            O(R(x0 + 1, y0 + 1, x0 + 11, y0 + 11), BRASS, 3)
            for (rx, ry) in ((x0 + 3, y0 + 3), (x0 + 8, y0 + 3), (x0 + 3, y0 + 8), (x0 + 8, y0 + 8)):
                a.px[(rx, ry)] = BRASS[5]
                a.px[(rx + 1, ry + 1)] = BRASS[1]
        for x in (16, 20, 24, 28, 32):
            O(R(x, 2, x + 2, 3), EMBER, 4, lit=False)
            O(R(x, n - 3, x + 2, n - 2), EMBER, 4, lit=False)
        for y in (16, 20, 24, 28, 32):
            O(R(2, y, 3, y + 2), EMBER, 4, lit=False)
            O(R(n - 3, y, n - 2, y + 2), EMBER, 4, lit=False)
        edge = IRON[0]
    a.outline(edge)
    # keep the centre clear: the outline ring may have touched it
    for y in range(7, n - 7):
        for x in range(7, n - 7):
            a.px.pop((x, y), None)
    return a.image()


# ====================================================== build, wire, sheet ==========================================
def build():
    out = {}
    for h in HOUSES:
        out[h] = dict(mark16=mark(h, 16), mark32=mark(h, 32), banner=banner(h), brazier=brazier(h), frame=frame(h))
    return out


def write(out):
    for h, assets in out.items():
        d = OUT / h
        d.mkdir(parents=True, exist_ok=True)
        for name, im in assets.items():
            im.save(d / (name + '.png'))


def wire(out):
    for h, assets in out.items():
        assets['mark16'].save(GLYPHS / (h + '.png'))


def sheet(out, path):
    def up(im, k):
        return im.resize((im.width * k, im.height * k), Image.NEAREST)
    existing = ROOT / 'src/main/resources/assets/gravesandglory/textures/gui'
    bg = (46, 42, 38, 255)
    names = ['mark16', 'mark32', 'banner', 'frame', 'brazier']
    rowh = 212
    W = 1000
    S = Image.new('RGBA', (W, 30 + rowh * len(HOUSES)), (30, 28, 26, 255))
    d = ImageDraw.Draw(S)
    d.text((8, 8), 'House identity set, DRAFT 4B.5: left, existing in main (glyph, recruit banner) and the new set at real size; right, the new set at 4x', fill=(220, 210, 190, 255))
    for i, h in enumerate(HOUSES):
        y0 = 30 + i * rowh
        d.rectangle([0, y0, W, y0 + rowh - 4], fill=bg)
        d.text((8, y0 + 4), h.upper(), fill=(230, 200, 120, 255))
        # existing
        g = Image.open(existing / 'house' / (h + '.png')) if (GLYPHS / (h + '.png')).exists() else None
        old = Image.open(ROOT / 'work/hi/old' / (h + '.png')) if (ROOT / 'work/hi/old' / (h + '.png')).exists() else None
        x = 8
        y = y0 + 22
        if old:
            S.paste(up(old, 2), (x, y), up(old, 2))
        b = Image.open(existing / 'recruit' / ('banner-' + h + '.png'))
        S.paste(b, (x, y + 40), b)
        # new set at 1x
        x = 8
        y = y0 + 132
        d.text((x, y - 12), 'real size', fill=(160, 150, 130, 255))
        for n in names:
            im = out[h][n]
            S.paste(im, (x, y), im)
            x += im.width + 6
        # new set at 4x
        x = 140
        y = y0 + 8
        for n in names:
            im = out[h][n]
            k = 4 if n != 'brazier' else 4
            big = up(im, k if im.width <= 32 and n != 'banner' else (4 if im.height * 4 <= rowh - 16 else 3))
            S.paste(big, (x, y), big)
            x += big.width + 8
    S.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--wire', action='store_true')
    ap.add_argument('--sheet')
    args = ap.parse_args()
    out = build()
    write(out)
    if args.wire:
        wire(out)
    if args.sheet:
        sheet(out, args.sheet)
    print('house identity drafts written to', OUT)


if __name__ == '__main__':
    sys.exit(main())
