"""Ashen Vigil, the healer set (docs/drafts/gear-catalogue-2026-11-03.md): Lanternbearer's Hood, Vigilant Vestment,
Candlewake Skirt, Mourner's Sandals. The keepers who tend the fallen: a bone-wool cowl and capelet over an ash-brown
robe, light mail and ash-grey bracers, leather belt and straps, soot soles, a small brass lantern.

Materials are kept apart (the 2026-11-04 grade): bone-wool only on the cowl, capelet and mantle; a darker ash-brown on
the robe, sleeves and skirt; soot inside the cowl and in the scorched hems; leather for the belt, straps and footbeds;
gold only on the lantern. Cloth is lit on its top row, darkened on its bottom two, shadowed one row under every
overhang, and folded in irregular ridge/trough pairs that stop short of the hem.

The side's colour is a bordered stole down the front, a drape across the back and the skirt's front and side falls, so
it reads at a glance as Last Watch's tabard does. Everything else is fixed paint.

Rarities: Common is plain. Uncommon adds green thread at the hems. Rare adds a 3 px blue waist sash with a knot and tail,
blue thread, silver clasps and bells. Epic lights the lantern, the vials and the mask's eye holes with purple glass.
Legendary lights the eye holes and the cowl's throat warm, peaks the hood (leaning back), raises two-tier flared
pauldrons, sets four embers round the shoulders, threads the hems with dim ember and lengthens the skirt's back.

Glow: any texel painted from a lit ramp (LITGLASS, WARM) is copied to the glow sheet, so paint and light never drift."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from gearlib import GearPiece
from handlib import Box, Bone, ramp, blank, clamp, cut, LIT, lightx, LEATH, TEAM, STEEL, GOLD

WOOL   = ramp("2d2a2e", "4a4544", "6e675e", "958b7b", "b7ac96", "d6ccb4", "eee6d2")   # bone-wool: cowl, capelet, mantle
ASHB   = ramp("171214", "261e1e", "3a2e2a", "4f3f36", "665244", "7f6853", "9a8166")   # ash-brown: robe, sleeves, skirt
ASH    = ramp("141317", "232227", "35333a", "4a474b", "625e5e", "7e7973", "9b958c")   # ash-grey wraps and bracers
SOOT   = ramp("070607", "100e10", "1a1617", "25201f", "322a27", "40362f")
IRON   = ramp("121114", "201e22", "312e33", "46424a", "5e5960", "7a7478", "9a9496")   # mail, chain, buckle, plain bells
BONEM  = ramp("3d3524", "625639", "8c7d55", "b6a676", "d9cc9a", "f4ebc4")             # the ivory half mask
GREEN  = ramp("1d4a1b", "2c7a28", "48a83c", "78d064")
BLUE   = ramp("0f1b3a", "1b3266", "2b4f9e", "4677c8", "77a2e2")
PURP   = ramp("2b1640", "4c2670", "6e3c98", "9462bc")                                 # Epic thread (unlit)
FLAME  = ramp("5a3410", "8a5a1c", "b08436")                                            # unlit horn panes, Common to Rare
LITGLASS = ramp("4a1670", "7a2cb4", "a654e6", "d090fa")                                # Epic: purple glass, lit
WARM   = ramp("7a2a0a", "b84a12", "ea7a22", "fbb050", "ffe2a0")                        # Legendary: the light inside
DRAUGHT = ramp("1c3a24", "2e5e36", "4a8a4a")
LEGEND, EPIC, RARE, UNCOMMON = 5, 4, 3, 2
GLOWING = {tuple(c) for c in LITGLASS + WARM}
THREAD = {UNCOMMON: GREEN[1:3], RARE: BLUE[2:4], EPIC: PURP[2:4], LEGEND: WARM[1:3]}   # the hem thread, by rarity
FACE_SEED = {"top": 0, "bottom": 1, "right": 2, "front": 3, "left": 4, "back": 5}

def side(face): return face not in ("top", "bottom")
def flat(R, v): return lambda r: lambda f, w, h: [[clamp(R, v + LIT[f]) for _ in range(w)] for _ in range(h)]

def fold_cols(w, seed):
    """Irregular ridge/trough pairs across a face: {column: +1 ridge or -1 trough}, 3 or 4 columns apart."""
    out, x, i = {}, 1 + seed % 2, 0
    while x + 1 < w:
        out[x], out[x + 1] = 1, -1
        x += 3 + (seed + i) % 2; i += 1
    return out

def drape(R, base, r=1, seed=0, hem=0, ragged=False, shade=(), folds=True, thread=False, top_seam=False):
    """Hanging cloth. Side faces: lit top row (or a shadow row under an overhang, `shade`), the bottom two rows darker,
    ridge/trough folds that stop short of the hem, `hem` scorched rows of uneven depth with the rarity's thread above,
    a `ragged` last row. Top faces are lit, with an optional centre seam."""
    def paint(face, w, h):
        g = blank(w, h)
        sd = seed + 7 * FACE_SEED[face]
        fc = fold_cols(w, sd) if side(face) and folds and w >= 4 and h >= 4 else {}
        for y in range(h):
            for x in range(w):
                v = base + LIT[face]
                if side(face):
                    v += lightx(face, x, w)
                    if y in shade: v -= 1
                    elif y == 0: v += 1
                    if h >= 4 and y >= h - 2: v -= 1
                    if x in fc and 1 + (x + sd) % 2 <= y <= h - 3 - hem - (x * 5 + sd) % 2: v += fc[x]
                elif face == "top" and top_seam and x == w // 2: v -= 1
                g[y][x] = clamp(R, v)
        if not side(face): return g
        trow = h - hem - 1 if hem else (h - 2 if ragged and h >= 3 else h - 1)
        for x in range(w):
            d = min(hem, 1 + ((x * 5 + sd) % 3 == 0)) if hem else 0
            for y in range(h - d, h):
                g[y][x] = clamp(SOOT, 3 + (LIT[face] > 0) - (y == h - 1) + (LIT[face] < 0) * -1)
                if r == LEGEND and y == h - 1 and (x * 7 + sd) % 5 == 0: g[y][x] = WARM[1]   # embers in the char
            if thread and r in THREAD and 0 < trow < h and (hem == 0 or trow < h - d) and (r != LEGEND or (x + sd) % 3 == 0):
                g[trow][x] = THREAD[r][(x + sd) % 2]   # a broken, dim ember thread at Legendary
            if ragged and h >= 2 and (x * 3 + sd) % 4 == 0: g[h - 1][x] = None
        return g
    return paint

def wraps(base=3, strap=None):
    """Ash-grey bandage wound on a slant: one darker step on every third diagonal, a leather strap on row `strap`."""
    def paint(face, w, h):
        g = [[clamp(ASH, base + LIT[face] + (lightx(face, x, w) if side(face) else 0) - (side(face) and (x + y) % 3 == 0))
              for x in range(w)] for y in range(h)]
        if side(face) and strap is not None and strap < h: g[strap] = [clamp(LEATH, 2 + (LIT[face] > 0) + (x == 0)) for x in range(w)]
        return g
    return paint

def chain(base=3):
    """Light mail as rows of rings: one tone step, each row offset by one from the last, shadowed under the bell."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (min(0, lightx(face, x, w)) if side(face) else 0)
                if side(face):
                    v += (x + y) % 2 == 0
                    if y == 0: v -= 1
                g[y][x] = clamp(IRON, v)
        return g
    return paint

def lantern(r):
    """A brass cap and base round horn panes: a dull wick at Common to Rare, purple light at Epic, warm at Legendary."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if not side(face):
                    g[y][x] = clamp(GOLD, 3 + LIT[face] + (face == "top" and x == w // 2 and y == h // 2) * 2); continue
                if y in (0, h - 1) or x in (0, w - 1):
                    g[y][x] = clamp(GOLD, 3 + LIT[face] + (y == 0) - (y == h - 1) + (x == 0 and face == "front")); continue
                g[y][x] = (WARM[3 + (y == 1)] if r == LEGEND else LITGLASS[2 + (y == 1)] if r == EPIC else FLAME[1 + (y == 1)])
        return g
    return paint

def bell(r):
    M, b = (STEEL, 5) if r >= RARE else (IRON, 4)
    def paint(face, w, h):
        g = [[clamp(M, b + LIT[face]) for _ in range(w)] for _ in range(h)]
        if r == LEGEND and face == "front": g[0][0] = WARM[3]
        return g
    return paint

def box(origin, size, art, sheet="skin"):
    return Box(origin, size, {k: art(k) for k in (1, 2, 3, 4, 5)}, sheet=sheet)

def glow_cb(bone, b, r):
    """The lit texels of this box at rarity r, so the glow sheet copies exactly what is painted in a lit ramp."""
    if b.sheet != "skin": return None
    src = b.paint.get(r, b.paint.get(0))
    if not src: return None
    def paint(face, w, h):
        out = blank(w, h)
        for y, line in enumerate(src(face, w, h)):
            for x, c in enumerate(line):
                if c is not None and tuple(c) in GLOWING: out[y][x] = c
        return out
    return paint

# ---- Lanternbearer's Hood: a bone-wool cowl with a brim and cheek flaps, the face recessed in soot, an ivory half mask
# standing proud of it, a tapered tail down the back, a brass lantern hung forward at the left temple ----
def cowl(r):
    base = drape(WOOL, 5, r, seed=1, top_seam=True, thread=True)
    def paint(face, w, h):
        g = base(face, w, h)
        if face != "front": return g
        for y in range(h):
            for x in range(w):
                if x in (0, w - 1): g[y][x] = clamp(WOOL, 4 + (x == 0)); continue
                g[y][x] = clamp(SOOT, (y - 1) // 2 + (x in (1, w - 2)))   # the dark inside, deepest under the brim
        if r == LEGEND:
            for x in (w // 2 - 1, w // 2, w // 2 + 1): g[h - 1][x] = WARM[1 + (x == w // 2)]   # light rising from the throat
        return g
    return paint

def mask(r):
    def paint(face, w, h):
        if face != "front": return [[clamp(BONEM, 3 + LIT[face]) for _ in range(w)] for _ in range(h)]
        g = [[clamp(BONEM, (3, 4, 3)[y] - (x in (0, w - 1)) + (y == 1 and x == w // 2)) for x in range(w)] for y in range(h)]
        g[2][w // 2] = BONEM[2]                                                                    # under the nose
        eye = WARM[3] if r == LEGEND else LITGLASS[3] if r == EPIC else SOOT[0]
        g[1][1] = g[1][w - 2] = eye
        return g
    return paint

def flap(r):
    base = drape(WOOL, 5, r, seed=3, folds=False)
    def paint(face, w, h):
        g = base(face, w, h)
        if side(face) and r >= UNCOMMON: g[h - 1] = [STEEL[5] if r >= RARE else THREAD[r][0]] * w   # the throat clasp
        return g
    return paint

def helm_bones():
    B = [Bone("hood", "head", tag="helm", boxes=[
        box((-4.5, -8.5, -4.5), (9, 9, 9), cowl),
        box((-4.5, -8.5, -5.5), (9, 1, 1), lambda r: drape(WOOL, 5, r, folds=False)),        # the brim
        box((-4.5, -7.5, -5.5), (1, 6, 1), flap), box((3.5, -7.5, -5.5), (1, 6, 1), flap),                    # cheek flaps
        box((-3.5, -6.5, -5), (7, 3, 1), mask),                                                                # proud of the soot
        box((-2.5, -4, 4.5), (5, 3, 1), lambda r: drape(WOOL, 4, r, seed=2, shade=(0,))),                      # the tail, tapering
        box((-1.5, -1, 4.5), (3, 2, 1), lambda r: drape(WOOL, 4, r, seed=3, shade=(0,))),
        box((-0.5, 1, 4.5), (1, 2, 1), lambda r: drape(WOOL, 3, r, seed=4, shade=(0,)))])]
    B.append(Bone("lantern_charm", "head", tag="helm", boxes=[
        box((4.5, -8, -5.5), (3, 1, 1), flat(LEATH, 3)),                                                      # a strap off the brim
        box((6, -7, -5.5), (1, 2, 1), flat(IRON, 3)),                                                         # its chain
        box((5, -5, -6.5), (3, 4, 3), lantern)]))
    B.append(Bone("hood_peak", "head", tag="helm", stages=[LEGEND], boxes=[                                   # leans back a pixel a tier
        box((-3.5, -10.5, -3), (7, 2, 7), lambda r: drape(WOOL, 5, r, seed=5, top_seam=True)),
        box((-2.5, -12.5, -1), (5, 2, 5), lambda r: drape(WOOL, 5, r, seed=6, shade=(0,))),
        box((-1.5, -14.5, 1), (3, 2, 3), lambda r: drape(WOOL, 4, r, seed=7, shade=(0,))),
        box((-0.5, -15.5, 3), (1, 1, 2), lambda r: drape(WOOL, 4, r, seed=8))]))
    return B

# ---- Vigilant Vestment: an ash-brown robe under a ragged bone-wool capelet, bell sleeves over a little mail and
# ash-grey bracers, a leather belt of vials ----
def vial(r):
    def paint(face, w, h):
        g = [[(LITGLASS[2 + (y == 1)] if r == EPIC else WARM[2 + (y == 1)] if r == LEGEND else DRAUGHT[1 + (y == 1)])
              for _ in range(w)] for y in range(h)]
        g[0] = [LEATH[3 + (face == "top")]] * w   # the cork
        return g
    return paint

def belt(r):
    def paint(face, w, h):
        g = [[clamp(LEATH, 2 + LIT[face] + (lightx(face, x, w) if side(face) else 0) + (side(face) and y == 0)) for x in range(w)] for y in range(h)]
        if face == "front":
            for y in range(h): g[y][w // 2 - 1] = g[y][w // 2] = IRON[4 + (y == 0)]   # the buckle
            g[1][w // 2] = LEATH[1]
        return g
    return paint

def sash(r):
    def paint(face, w, h):
        g = [[clamp(BLUE, 2 + LIT[face] + (lightx(face, x, w) if side(face) else 0) + (side(face) and y == 0) - (side(face) and y == h - 1)
                    - (side(face) and h >= 3 and (x + y) % 4 == 0)) for x in range(w)] for y in range(h)]
        return g
    return paint

def capelet(r):
    base = drape(WOOL, 5, r, seed=2, ragged=True, thread=True)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front": g[1][w // 2 - 1] = g[1][w // 2] = (STEEL[5] if r >= RARE else LEATH[3])   # the clasp at the throat
        if face == "top":
            for x in range(1, w - 1): g[h // 2][x] = WOOL[4]   # the neck opening's shadow under the hood
        return g
    return paint

def chest_bones():
    B = [Bone("robe", "body", tag="chest", boxes=[
        box((-4.5, -12, -2.5), (9, 12, 5), lambda r: drape(ASHB, 3, r, seed=1, shade=(4, 9))),
        box((-5, -13.25, -3.5), (10, 5, 7), capelet),
        box((-5, -5, -3), (10, 2, 6), belt)])]
    for x in (-3.75, -2.5, -1.25):                                                                     # the belt of vials
        B[0].boxes.append(box((x, -3, -3.75), (1, 3, 1), vial))
    B.append(Bone("sash", "body", tag="chest", stages=[RARE, EPIC, LEGEND], boxes=[                    # the Rare's 3 px sash
        box((-5.5, -5.5, -3.5), (11, 3, 7), sash),
        box((1.5, -5, -4.5), (2, 2, 1), sash),                                                         # its knot
        box((1.5, -3, -4.25), (2, 5, 1), lambda r: drape(BLUE, 2, r, seed=4, ragged=True))]))
    B.append(Bone("stole", "body", tag="chest", team=True, boxes=[
        Box((-4, -9.5, -3.25), (3, 11, 0), ("stole", "front"), sheet="cloth"),
        Box((1, -9.5, -3.25), (3, 11, 0), ("stole", "front"), sheet="cloth"),
        Box((-4, -9.5, 3.25), (8, 8, 0), ("drape", "back"), sheet="cloth")]))
    for s, c in (("right", -1), ("left", 1)):
        bx = c - 3.5 if s == "right" else c - 2.5                                                      # the bell flares outward
        B.append(Bone(f"sleeve_{s}", f"{s}_arm", tag="chest", boxes=[
            box((c - 2.5, -2.5, -2.5), (5, 3, 5), lambda r: drape(ASHB, 3, r, seed=5, folds=False)),
            box((bx, 0.5, -3), (6, 3, 6), lambda r: drape(ASHB, 3, r, seed=6, shade=(0,), thread=True)),
            box((c - 2.5, 3.5, -2.5), (5, 1, 5), lambda r: chain(3)),
            box((c - 2.5, 4.5, -2.5), (5, 4, 5), lambda r: wraps(3, strap=3))]))                          # bracer: 1.5 px of hand bare
    # the Legendary: two-tier flared pauldrons and four embers over the shoulders
    B.append(Bone("mantle", "body", tag="chest", stages=[LEGEND], boxes=[
        box((-10, -13.5, -4), (6, 2, 8), lambda r: drape(WOOL, 4, r, seed=7, hem=1)),
        box((-9, -14.5, -3), (5, 1, 6), lambda r: drape(WOOL, 5, r, seed=8)),
        box((4, -13.5, -4), (6, 2, 8), lambda r: drape(WOOL, 4, r, seed=9, hem=1)),
        box((4, -14.5, -3), (5, 1, 6), lambda r: drape(WOOL, 5, r, seed=10))]))
    B.append(Bone("embers", "body", tag="chest", stages=[LEGEND], boxes=[
        box(o, (1, 1, 1), lambda r: lambda f, w, h: [[WARM[3 + (f == "front")]] * w for _ in range(h)])
        for o in ((-7.5, -17, -1), (6.5, -16, 1.5), (-5.5, -19, 2.5), (7.5, -19.5, -2))]))
    return B

def cloth(spec, side_):
    part, which = spec
    T, trim = TEAM[side_][0], TEAM[side_][1][1]
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 - (which == "back") - (1 if y >= h - 2 else 0)
                if part == "drape" and x in (2, w - 3): v -= 1
                if w >= 3 and x in (0, w - 1) or part == "fall" and which == "side" and x == 0: v = 1 - (which == "back")   # the dark border
                g[y][x] = clamp(T, v)
                if y == h - 2: g[y][x] = trim   # the gold-thread band near the hem
        if part == "stole": g[h - 1] = [T[1], None, T[1]]                                            # a fringed end
        elif part == "drape": cut(g, [(0, h - 1), (3, h - 1), (w - 1, h - 1), (w - 1, h - 2)])
        elif part == "fall": cut(g, [(0, h - 1), (w - 1, h - 1)])
        return g
    return paint

# ---- Candlewake Skirt: one long split robe of ash-brown panels standing proud, a ragged scorched hem, knee wraps ----
def legs_bones():
    B = []
    for s in ("right", "left"):
        m = -1 if s == "right" else 1                                         # toward the outside of this leg
        sx = -3.25 if s == "right" else -1.75                                 # widened inward, the two meet in a split
        B.append(Bone(f"skirt_{s}", f"{s}_leg", tag="legs", boxes=[
            box((sx, -0.5, -2.5), (5, 8, 5), lambda r: drape(ASHB, 3, r, seed=1 + (m > 0), hem=2, ragged=True, shade=(0,), thread=True)),
            box((sx + 0.5, 0.5, 2.5), (4, 7, 1), lambda r: drape(ASHB, 3, r, seed=3 + (m > 0), hem=1, ragged=True)),   # back panel
            box((-4.25 if s == "right" else 3.25, 0.5, -2), (1, 7, 4), lambda r: drape(ASHB, 3, r, seed=5, hem=1, ragged=True)),
            box((-2.5, 7.5, -2.5), (5, 1, 5), lambda r: wraps(3))]))
        B.append(Bone(f"fall_{s}", f"{s}_leg", tag="legs", team=True, boxes=[
            Box((sx + 1, 0, -3), (3, 7, 0), ("fall", "front"), sheet="cloth"),
            Box((-4.5 if s == "right" else 4.5, 0.5, -1), (0, 6, 2), ("fall", "side"), sheet="cloth")]))   # reads from the side
        B.append(Bone(f"tail_{s}", f"{s}_leg", tag="legs", stages=[LEGEND], boxes=[                        # the Legendary's longer back
            box((sx + 0.5, 7.5, 3.25), (4, 3, 1), lambda r: drape(ASHB, 3, r, seed=6, hem=2, ragged=True, shade=(0,)))]))
    return B

# ---- Mourner's Sandals: dark wraps strapped in leather on soot soles, a muted cuff, tiny bells at the outer ankle ----
def cuff(r):
    def paint(face, w, h):
        g = [[clamp(ASHB, 2 + LIT[face] + (lightx(face, x, w) if side(face) else 0)) for x in range(w)] for _ in range(h)]
        if side(face) and r in THREAD:
            for x in range(w):
                if x % (4 if r == LEGEND else 2) == 0: g[0][x] = THREAD[r][0]
        return g
    return paint

def sole(face, w, h):
    if face == "top": return [[LEATH[3] for _ in range(w)] for _ in range(h)]                           # the footbed
    return [[clamp(SOOT, 2 + LIT[face] + (side(face) and x % 3 == 0)) for x in range(w)] for _ in range(h)]

def boots_bones():
    B = []
    for s in ("right", "left"):
        bx = -3.5 if s == "right" else 2.5
        B.append(Bone(f"sandal_{s}", f"{s}_leg", tag="boots", boxes=[
            box((-3, 8.75, -3), (6, 1, 6), cuff),
            box((-2.5, 9.5, -2.5), (5, 2, 5), lambda r: wraps(2, strap=1)),
            box((-2.5, 11.5, -3), (5, 1, 6), lambda r: sole),
            box((-0.5, 9.5, -2.75), (1, 2, 1), flat(LEATH, 2)),                                             # the instep strap
            box((bx, 9.75, -1.25), (1, 1, 1), bell), box((bx, 10.25, 0.5), (1, 1, 1), bell)]))
    return B

def pieces():
    return [GearPiece("healer_helm", "helm", helm_bones, glow=glow_cb),
            GearPiece("healer_chest", "chest", chest_bones, cloth_painter=cloth, skin=(128, 64), cloth=(64, 32), glow=glow_cb),
            GearPiece("healer_legs", "legs", legs_bones, cloth_painter=cloth, cloth=(32, 16), glow=glow_cb),
            GearPiece("healer_boots", "boots", boots_bones, glow=glow_cb)]
