"""Option B: a fresh hand paint of the Order squire / knight for the GeckoLib rig (art/anim/knight/build_rig.py B).

One texel per model pixel, light from the top front and the wearer's right (handlib's LIT and lightx), no dithering, no
pure black or white, hue-shifted ramps. Team colour lives only in cloth_painter; every skin-sheet painter here uses metal,
leather, wool, wood, skin or undyed cloth.

  Tier I   a poor but proud recruit: drab quilted gambeson, dark wool hose with linen leg wraps, a leather-lined mail
           coif and tippet, a dull iron spangenhelm, a team tabard with a bleached-linen cross sewn on.
  Tier II  a sworn man-at-arms: plain steel plate, a great helm with a pointed faceplate and cross-cut breaths,
           layered pauldrons, a team tabard panel bearing the Order's cross in metal.
  Tier III a champion: bright steel edged in gold, gilt pauldrons, gorget and gauntlets, the great helm gilded and
           crowned under a team plume, a fur collar and a long team cape hemmed in gold.
"""
import handlib as H
from handlib import STEEL, GOLD, MAIL, LEATH, LINEN, SKINS, HAIRS, TEAM, LIT, blank, grid, rows, ramp, hx

# ---- ramps of our own (darkest first; shadows cool, lights warm) ----
GAMB = ramp("2b2427", "483d3a", "6b5c50", "8d7a63", "ab9776", "c6b48f")   # undyed quilted fustian
WOOL = ramp("1b171b", "2d272b", "443b3b", "5d514c", "786a60")              # dark wool hose
WOOD = ramp("21160f", "3b2717", "5b3d22", "7b5530", "9a6f40", "b88b56")    # shield boards
FUR = ramp("221d1e", "3d3433", "5c514b", "807468", "a39785", "c2b8a2")     # the champion's collar


def C(R, v): return R[max(0, min(len(R) - 1, v))]


def soft(face, x, w):
    """A gentler side light for cloth: only the edge column nearest the light brightens, only the far one darkens."""
    if w < 3: return 0
    lit = {"front": 0, "back": w - 1, "right": w - 1, "left": 0}.get(face)
    if lit is None: return 0
    return 1 if x == lit else (-1 if x == w - 1 - lit else 0)


def sheet(R, base, rim=1, seam=1, light=H.lightx):
    """A plain material: face light, side light, a lit top row and a dark bottom row on the side faces."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face]
                if face == "top":
                    if y == 0: v -= 1                      # the back edge turns away
                elif face != "bottom":
                    v += light(face, x, w)
                    if y == 0: v += rim
                    if y == h - 1 and h > 2: v -= seam
                g[y][x] = C(R, v)
        return g
    return paint


def quilted(R, base, period=3, vertical=False):
    """Padded cloth: stitched channels every `period` texels (rows on the sleeves, columns on the body)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face]
                if face not in ("top", "bottom"):
                    v += H.lightx(face, x, w)
                    k = x if vertical else y
                    if k % period == period - 1: v -= 1
                    if y == 0: v += 1
                g[y][x] = C(R, v)
        return g
    return paint


def by_face(default, **faces):
    def paint(face, w, h): return faces.get(face, default)(face, w, h)
    return paint


def post(painter, fn):
    """A painter whose result fn(face, g, w, h) then edits in place."""
    def paint(face, w, h):
        g = painter(face, w, h)
        fn(face, g, w, h)
        return g
    return paint


def rivets(painter, colour, row, every=3, faces=("front", "back", "right", "left"), start=1):
    def fn(face, g, w, h):
        if face in faces and row < h:
            for x in range(start, w - 1, every): g[row][x] = colour
    return post(painter, fn)


# ---- the body: the torso keeps rows 0..9, the pelvis rows 8..12 of one 8x12x4 body ----
def body_base(tier):
    if tier == 1:   # the gambeson, quilted in vertical channels; the tabard shell covers most of it
        return rows([(0, quilted(GAMB, 4, 3, vertical=True)), (10, sheet(GAMB, 3, rim=0))])
    R, b = STEEL, (3 if tier == 2 else 4)
    def cuirass(face, w, h):
        g = sheet(R, b, rim=1, seam=0)(face, w, h)
        if face in ("front", "back") and w == 8:       # the breast's central ridge
            for y in range(1, h):
                g[y][3 if face == "front" else 4] = C(R, b + 1 + LIT[face])
        return g
    fauld_hi, fauld_lo = sheet(R, b, rim=1, seam=0), sheet(R, b - 1, rim=1, seam=0)
    mail = H.mail(base=2)
    if tier == 2:
        return rows([(0, cuirass), (8, fauld_hi), (9, fauld_lo), (10, mail)])
    gilt = sheet(GOLD, 4, rim=0, seam=0)
    return rows([(0, cuirass), (7, gilt), (8, fauld_hi), (9, sheet(GOLD, 3, rim=0, seam=0)), (10, mail)])


def belt(tier):
    def paint(face, w, h):     # 8x2x4, low on the hips
        g = sheet(LEATH, 2, rim=1, seam=0)(face, w, h)
        if face in ("top", "bottom"): return g
        if tier >= 2:          # studs (steel, then gilt plaques)
            stud = STEEL[5] if tier == 2 else GOLD[4]
            for x in range(1, w, 3):
                g[0][x] = stud
                if tier == 3: g[1][x] = GOLD[2]
        if face == "front":
            hi, lo = ((STEEL[4], STEEL[2]) if tier == 1 else (STEEL[5], STEEL[3]) if tier == 2 else (GOLD[5], GOLD[3]))
            for x in (3, 4): g[0][x], g[1][x] = hi, lo
            g[0][4] = lo if tier == 1 else hi
            g[1][3] = hi if tier == 1 else lo
        return g
    return paint


# ---- the faces: four men of the Order. Only tier I shows one, through the coif's opening (rows 4..7) ----
FACES_B = [   # S skin light, s mid, d shade, D deep; h/H/i hair dark/mid/light; b brow; w eye white; e iris; m mouth
    ("fair", "brown", False, """
hHHiiHHh
HiHHHHiH
HSSSSSsH
SbbSsbbd
SweSsewd
SSSSdssd
SSsmmsdd
sSSSssdD"""),
    ("olive", "black", True, """
hhhhhhhh
hHHHHHHh
hSSSSSsh
SbbSsbbd
SweSsewd
SSSSdssd
HHHmmHHh
hHHHHHhh"""),
    ("brown", "black", False, """
hhhhhhhh
hHhHHhHh
SSSSSSsd
SbbSsbbd
SweSsewd
SSSSdssd
SHHHHHHd
sSSmmsdD"""),
    ("pale", "blond", False, """
iHiiHiiH
HiHHiHHi
iSSSSSSH
SiiSsiid
SweSsewd
SDSSdssd
SSsmmsdd
sSSSssdD"""),
]
IRIS = {"fair": "3a5470", "olive": "3a2a1c", "brown": "2a1a12", "pale": "4c5e4c"}


def face_painter(k):
    skin, hair, bearded, front = FACES_B[k]
    S, Hh = SKINS[skin], HAIRS[hair]
    p = {"S": S[3], "s": S[2], "d": S[1], "D": S[0], "h": Hh[0], "H": Hh[1], "i": Hh[2], "b": Hh[0],
         "w": hx("e6e0d6"), "e": hx(IRIS[skin]), "m": S[0]}
    if hair == "blond": p["b"] = Hh[1]
    def paint(face, w, h):
        if face == "front": return grid(front, p)
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face == "top": g[y][x] = Hh[2] if y >= h - 3 and x < 4 else Hh[1]; continue
                if face == "bottom": g[y][x] = Hh[0] if bearded else S[1]; continue
                if face == "back": g[y][x] = (Hh[1] if y < 6 else Hh[0]) if y < 7 else S[1]; continue
                back = (w - 1 - x) if face == "right" else x       # texels from the back of the head
                lit = face == "right"
                if y < 2 or (back < 3 and y < 7): g[y][x] = Hh[1] if lit else Hh[0]
                elif bearded and y >= 5 and back >= 3: g[y][x] = Hh[1] if lit else Hh[0]
                else: g[y][x] = S[3] if lit else S[2]
                if back == 3 and y in (3, 4): g[y][x] = S[1] if y == 4 else S[2]   # the ear
        return g
    return paint


# ---- tier I helm: mail coif (handlib mail, our front), iron spangenhelm ----
def coif_front(g, w, h):
    """The coif's front face (9x9): open over brows, eyes, nose and mouth, the opening bound in leather."""
    for y in range(4, 8):
        for x in range(1, 8):
            g[y][x] = None
    for x in range(1, 8): g[3][x] = MAIL[4]               # the brow roll catches the light
    for y in range(4, 8): g[y][0], g[y][8] = MAIL[3], MAIL[1]
    return g


def spangen_cap(face, w, h):
    """10x4x10: four dull iron plates riveted to bands at the corners and the middle, a dark brow band."""
    g = blank(w, h)
    if face == "bottom":
        for y in range(h):
            for x in range(w): g[y][x] = STEEL[1]
        return g
    if face == "top":   # rows run back to front, columns from the wearer's right
        for y in range(h):
            for x in range(w):
                v = 4 + (1 if (y >= h // 2 and x < w // 2) else 0)
                if x in (4, 5) or y in (4, 5): v = 5          # the bands meeting at the crown
                if x in (0, w - 1) or y in (0, h - 1): v -= 1
                g[y][x] = C(STEEL, v)
        return g
    for y in range(h):
        for x in range(w):
            v = 3 + LIT[face] + H.lightx(face, x, w)
            if y == 0: v += 1
            if x in (4, 5): v += 1                              # the spangen band
            if y == h - 1: v = 2 + LIT[face] + (1 if H.lightx(face, x, w) > 0 else 0)   # brow band
            g[y][x] = C(STEEL, v)
    for x in (4, 5): g[1][x] = STEEL[5] if face != "left" and face != "back" else STEEL[4]   # band rivets
    for x in range(1, w - 1, 3): g[h - 1][x] = STEEL[4]
    return g


# ---- tiers II and III: the great helm and its crown ----
def great_helm(tier):
    b = 3 if tier == 2 else 4
    trim = tier == 3
    BREATH = [(2, 6), (2, 7), (2, 8)]                     # a column of breaths down each cheek

    def paint(face, w, h):    # 10x10x10
        g = blank(w, h)
        if face == "bottom":
            for y in range(h):
                for x in range(w): g[y][x] = STEEL[1]
            return g
        if face == "top":
            for y in range(h):
                for x in range(w):
                    v = b + 1 + (1 if (y >= h // 2 and x < w // 2) else 0)
                    ring = x in (0, w - 1) or y in (0, h - 1)
                    g[y][x] = (C(GOLD, 4) if trim else C(STEEL, v - 1)) if ring else C(STEEL, v)
            return g
        for y in range(h):
            for x in range(w):
                v = b + LIT[face] + H.lightx(face, x, w)
                if y == 0: v += 1
                if y == h - 1: v -= 1
                if face == "front":
                    if y == 3: v += 1                          # the brow lip over the sight
                    if y == 5: v += 1                          # the faceplate's upper edge
                    if y >= 5 and x == 4: v += 1               # the faceplate comes to a point down the middle
                    if y >= 5 and x == 5: v -= 1
                g[y][x] = C(STEEL, v)
        if face == "front":
            for x in list(range(1, 4)) + list(range(6, 9)): g[4][x] = STEEL[0]   # the sight, split by a bar
            for x, y in BREATH: g[y][x] = STEEL[1]; g[y][w - 1 - x] = STEEL[1]
        else:
            for x in range(1, w - 1, 3): g[1][x] = STEEL[5] if face in ("right",) else STEEL[4]
            if face in ("right", "left"):                      # the sight wraps a texel round the side
                fx = w - 1 if face == "right" else 0
                g[4][fx] = STEEL[0]
        if trim:
            gold = lambda d: C(GOLD, 4 + LIT[face] + d)
            for x in range(w): g[0][x] = gold(1)
            if face == "front":
                for x in range(w):
                    if g[3][x] is not None: g[3][x] = gold(0)
                g[h - 1] = [gold(-1)] * w
            else:
                for x in range(1, w - 1, 3): g[1][x] = gold(1)
                g[h - 1] = [gold(-1)] * w
        return g
    return paint


def crown(face, w, h):
    """11x4x11: an open crown of three fleurons a side over a band set with dark stones."""
    g = blank(w, h)
    if face in ("top", "bottom"):
        for y in range(h):
            for x in range(w):
                if x in (0, w - 1) or y in (0, h - 1): g[y][x] = GOLD[4] if face == "top" else GOLD[2]
        return g
    shape = ["g....g....g", "gg..ggg..gg", "gggggggggggg", "gggggggggggg"]
    for y in range(h):
        for x in range(w):
            if shape[y][x] != "g": continue
            v = 4 + LIT[face] + H.lightx(face, x, w) + (1 if y == 0 else 0) - (1 if y == 3 else 0)
            g[y][x] = C(GOLD, v)
    for x in (2, 5, 8):     # pearls set in the band, the middle one catching the light
        g[2][x] = STEEL[4] if x != 5 else STEEL[5]
    return g


def gorget(tier):
    R, b = (STEEL, 4) if tier == 2 else (GOLD, 4)
    return sheet(R, b, rim=1, seam=0)


def mantle(face, w, h):
    """12x3x6 tier I: the coif's mail tippet over the shoulders, bound in leather at its edge."""
    return rows([(0, H.mail(base=3)), (2, sheet(LEATH, 3, rim=0, seam=0))])(face, w, h)


# ---- limbs (12 rows: upper 0..6, forearm/shin 4..12) ----
def arm(tier):
    if tier == 1:
        glove = rivets(sheet(LEATH, 3, rim=1, seam=1), LEATH[1], 2, every=1, start=0)
        return rows([(0, quilted(GAMB, 4, 3)), (8, sheet(LEATH, 2, rim=1, seam=0)), (9, glove)])
    R, b = STEEL, (3 if tier == 2 else 4)
    rere = sheet(R, b, rim=1, seam=1)
    vamb = sheet(R, b, rim=1, seam=0)
    gaunt = rivets(sheet(R, b, rim=1, seam=1), C(R, b + 2), 1, every=2)
    if tier == 2:
        return rows([(0, rere), (5, H.mail(base=2)), (6, vamb), (9, gaunt)])
    gaunt = post(gaunt, lambda f, g, w, h: g.__setitem__(0, [C(GOLD, 4 + LIT[f] + H.lightx(f, x, w)) for x in range(w)]) if f not in ("top", "bottom") else None)
    return rows([(0, rere), (5, H.mail(base=2)), (6, sheet(GOLD, 4, rim=0, seam=0)), (7, vamb), (9, gaunt)])


def leg(tier):
    if tier == 1:
        wraps = post(sheet(LINEN, 2, rim=1, seam=0), lambda f, g, w, h: [g.__setitem__(y, [C(LINEN, 1 + LIT[f])] * w) for y in range(1, h, 2)] if f not in ("top", "bottom") else None)
        return rows([(0, sheet(WOOL, 3, rim=0, seam=0)), (6, wraps)])
    R, b = STEEL, (3 if tier == 2 else 4)
    cuisse = sheet(R, b, rim=1, seam=1)
    knee = by_face(sheet(R, b - 1, rim=0, seam=0), back=H.mail(base=2))
    greave = sheet(R, b, rim=1, seam=0)
    if tier == 2:
        return rows([(0, cuisse), (5, knee), (7, greave)])
    return rows([(0, sheet(GOLD, 4, rim=0, seam=0)), (1, cuisse), (5, knee), (7, sheet(GOLD, 4, rim=0, seam=0)), (8, greave)])


def boot(tier):
    if tier == 1:   # 5x3x5: a turned-down leather cuff, the boot, a dark sole
        return rows([(0, sheet(LEATH, 4, rim=0, seam=0)), (1, sheet(LEATH, 3, rim=0, seam=0)), (2, sheet(LEATH, 1, rim=0, seam=0))])
    R, b = STEEL, (3 if tier == 2 else 4)
    top = sheet(GOLD, 4, rim=0, seam=0) if tier == 3 else sheet(R, b + 1, rim=0, seam=0)
    return rows([(0, top), (1, sheet(R, b, rim=0, seam=0)), (2, sheet(LEATH, 1, rim=0, seam=0))])


def _piece(tier, rim=1, base_steel=4, base_gold=4, stud=True):
    R, b = (STEEL, base_steel) if tier == 2 else (GOLD, base_gold)
    p = sheet(R, b, rim=rim, seam=1)
    if not stud: return p
    def fn(face, g, w, h):
        if face == "front" and w >= 3 and h >= 2: g[h // 2][w // 2] = C(R, b + 2)
    return post(p, fn)


def couter(tier):
    R, b = (STEEL, 4) if tier == 2 else (GOLD, 4)
    def paint(face, w, h):     # 1x3x3, its wing on the outer face
        g = sheet(R, b, rim=1, seam=1)(face, w, h)
        if face in ("right", "left") and w >= 3 and h >= 3: g[1][1] = C(R, b + 2)
        return g
    return paint


def cuff(tier): return _piece(tier, stud=False)
def knee(tier): return _piece(tier)
def toe(tier): return sheet(STEEL if tier == 2 else GOLD, 4, rim=0, seam=0)


def pauldron(tier, part):
    if part == "dome":
        R = STEEL if tier == 2 else GOLD
        def paint(face, w, h):     # 7x2x7
            g = sheet(R, 4, rim=1, seam=0)(face, w, h)
            if face == "top":      # the dome rounds over: a lit crown toward the front and the light
                for y in range(h):
                    for x in range(w):
                        edge = x in (0, w - 1) or y in (0, h - 1)
                        g[y][x] = C(R, 4 if edge else 5 + (1 if (y >= h - 3 and x < 3) else 0))
                return g
            if face not in ("bottom",): g[0][w // 2] = C(R, 6) if face != "back" else C(R, 5)   # the boss rivet
            return g
        return paint
    # the lames: steel, their lower edge gilt at tier III
    b = 3 if tier == 2 else 4
    def paint(face, w, h):
        g = sheet(STEEL, b, rim=1, seam=0)(face, w, h)
        if face not in ("top", "bottom"):
            g[h - 1] = [C(GOLD, 3 + LIT[face] + H.lightx(face, x, w)) if tier == 3 else C(STEEL, b - 1 + LIT[face] + H.lightx(face, x, w)) for x in range(w)]
        return g
    return paint


# ---- the shield: wooden boards behind a team face, rim and boss ----
def wood(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            v = 3 + LIT[face]
            if face in ("front", "back") and x % 4 == 3: v -= 1       # board joints
            if face == "back" and h >= 12 and y in (2, 9): v = 1       # iron battens across the back
            g[y][x] = C(WOOD, v)
    return g


def shield_rim(tier):
    R, b = (LEATH, 3) if tier == 1 else (STEEL, 3) if tier == 2 else (GOLD, 3)
    nail = STEEL[4] if tier == 1 else STEEL[5] if tier == 2 else GOLD[5]
    def paint(face, w, h):
        g = sheet(R, b, rim=1 if (face == "front" and h == 1) else 0, seam=0)(face, w, h)
        if face == "front":
            if h > 1: g[0] = [C(R, b + 1)] * w
            for y in range(h):
                for x in range(w):
                    if (h > 1 and y % 3 == 2) or (h == 1 and w > 4 and x % 3 == 1): g[y][x] = nail
        return g
    return paint


def shield_boss(tier):
    R, b = (STEEL, 3) if tier == 1 else (STEEL, 4) if tier == 2 else (GOLD, 4)
    def paint(face, w, h):
        g = sheet(R, b, rim=1, seam=0)(face, w, h)
        if face == "front": g[0][0] = C(R, b + 2); g[h - 1][w - 1] = C(R, b - 2)
        return g
    return paint


# ---- team cloth ----
def metal_of(side): return STEEL[2:7] if side == "gold" else GOLD[2:7]
def pale_of(side, T): return (T[0], T[0]) if side == "linen" else (LINEN[5], LINEN[3])


def tabard1(T, pale):
    """Tier I's tabard over the whole body (8x12x4): open at the sides, a bleached linen cross sewn on the breast."""
    pl, ps = pale
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = T[3]
            return g
        base = 3 if face in ("front", "right") else 2
        for y in range(h):
            for x in range(w):
                v = base + soft(face, x, w)
                if y == 0: v += 1
                g[y][x] = C(T, v)
        if face in ("right", "left"):
            for y in range(3, 10):
                for x in range(1, w - 1): g[y][x] = None
        if face == "front":
            for y in range(3, 10): g[y][3], g[y][4] = pl, ps
            for x in range(1, 7): g[5][x] = pl
        return g
    return paint


def flap(T, M, kind, inner):
    """A leg flap continuing the tabard below the belt: 4 wide at tier I, 3 at tiers II ("flap12") and III ("flap3")."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        tier = 1 if w == 4 else (2 if kind == "flap12" else 3)
        right_leg = (inner == w - 1) if face == "front" else (inner == 0)
        outer = w - 1 - inner
        base = 3 if face == "front" else 2
        for y in range(h):
            for x in range(w):
                v = base
                if x == inner: v -= 1
                if x == outer and tier == 1: v += 1 if right_leg else -1
                g[y][x] = C(T, v)
                if tier >= 2 and x == outer: g[y][x] = T[1]
        if tier == 1:
            g[h - 1] = [T[1]] * w
            g[h - 1][outer] = None                            # a frayed corner
        elif tier == 2:
            g[h - 1] = [T[1]] * w
        else:
            g[h - 2] = [T[1]] * w
            g[h - 1] = [C(M, 2)] * w                          # a gilt hem
        return g
    return paint


def surcoat(T, M, tier, back, cross=None):
    """Tiers II and III: the open tabard panel (6x10), front with the Order's cross, back plain."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        seen_back = face == "back"
        base = 2 if back else 3
        lit = w - 2 if seen_back else 1
        for y in range(h):
            for x in range(w):
                v = base + (1 if x == lit else 0) + (1 if y == 0 else 0)
                g[y][x] = C(T, v)
                if x in (0, w - 1):
                    g[y][x] = T[1]
        if tier == 3: g[0] = [C(M, 3)] * w
        if not back and face == "front":
            hi, lo = cross or ((C(M, 2), C(M, 1)) if tier == 2 else (C(M, 3), C(M, 2)))
            bottom = 7 if tier == 2 else 8
            for y in range(1, bottom): g[y][2], g[y][3] = hi, lo
            for x in range(1, w - 1): g[3][x] = hi
            if tier == 3:   # the cross's foot flares
                g[bottom - 1][1], g[bottom - 1][4] = lo, lo
        return g
    return paint


def shield_face(T, M, pale, tier):
    """12x18 heater face: the team field and the Order's cross (bleached linen, then metal, then flared gilt)."""
    if tier == 1: hi, lo = pale
    elif tier == 2: hi, lo = C(M, 3), C(M, 2)
    else: hi, lo = C(M, 4), C(M, 3)
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                if not H.in_shield(x, y): continue
                v = 3 + (1 if x <= 1 else 0) - (1 if x >= w - 2 else 0) - (1 if y >= 13 else 0)
                g[y][x] = C(T, v)
                vert = x in (5, 6) and 1 <= y <= 15
                bar = y in (5, 6) and 1 <= x <= 10
                if vert or bar: g[y][x] = hi if (x == 5 or (bar and y == 5)) else lo
                if tier == 3 and ((y in (4, 7) and x in (1, 10)) or (x in (4, 7) and y in (1, 15))):
                    g[y][x] = lo                                # flared ends
        return g
    return paint


def crest(T, tail):
    def paint(face, w, h):
        g = blank(w, h)
        if not tail:            # 2x3x9: a plume combed along the helm's ridge
            if face == "top":
                for y in range(h):
                    for x in range(w): g[y][x] = T[4]
                return g
            if face == "bottom":
                for y in range(h):
                    for x in range(w): g[y][x] = T[1]
                return g
            d = -1 if face in ("left", "back") else 0
            for y in range(h):
                for x in range(w): g[y][x] = C(T, 4 - y + d)
            if face in ("right", "left") and w > 4:
                g[0][0] = g[0][w - 1] = None
            return g
        # tail, 2x9x2: hangs down the back of the helm, tapering
        d = -1 if face in ("left", "back") else 0
        for y in range(h):
            for x in range(w):
                g[y][x] = C(T, (3 if y < 3 else 2 if y < 7 else 1) + d)
        if face not in ("top", "bottom") and h >= 9: g[h - 1][1] = None
        return g
    return paint


def cape(T, M):
    FOLD = [1, 2, 2, 1, 2, 3, 2, 2, 3, 3]   # back face, from the wearer's left to the lit right: broad folds
    def paint(face, w, h):     # 10x17x1
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face == "back": v = FOLD[x] if w == 10 else 2
                elif face == "top": v = 2
                else: v = 1
                g[y][x] = C(T, v)
        if face == "back" or face == "front":
            g[h - 2] = [C(M, 2)] * w
            g[h - 1] = [C(M, 1)] * w
        if face in ("right", "left") and h >= 2: g[h - 2][0], g[h - 1][0] = C(M, 2), C(M, 1)
        if face == "bottom": g = [[C(M, 1)] * w for _ in range(h)]
        return g
    return paint


def collar(face, w, h):
    """9x2x2: the champion's fur collar where the cape is gathered at the shoulders."""
    g = blank(w, h)
    d = -1 if face in ("left", "back", "bottom") else 0
    for y in range(h):
        for x in range(w):
            v = (4 if face == "top" else 4 - y) + d
            if face not in ("top", "bottom") and y == h - 1 and x % 3 == 1: v -= 1   # the pelt's lower tufts
            g[y][x] = C(FUR, v)
    return g


def cloth_painter(spec, side):
    T, _ = TEAM[side]
    M = metal_of(side)
    pale = pale_of(side, T)
    if spec == "tabard1": return tabard1(T, pale)
    if spec == "cape": return cape(T, M)
    if spec == "collar": return collar
    if spec in ("crest", "tail"): return crest(T, spec == "tail")
    if spec[0] == "surcoat":
        dark = (GOLD[2], GOLD[1]) if side == "linen" else None   # gilt would vanish on undyed linen
        return surcoat(T, M, spec[1], spec[2], dark)
    if spec[0] == "shield": return shield_face(T, M, pale, spec[1])
    kind, inner = spec
    return flap(T, M, kind, inner)
