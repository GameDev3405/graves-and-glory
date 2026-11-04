"""Ashen Vigil, the healer set (docs/drafts/gear-catalogue-2026-11-03.md): Lanternbearer's Hood, Vigilant Vestment,
Candlewake Skirt, Mourner's Sandals. The keepers who tend the fallen: pale wool over light mail, ash-grey wraps, soot.
The side's colour is a long stole down the front, a drape across the back and the skirt's front falls, so it reads at a
glance as Last Watch's tabard does. The wool, wraps and metal are fixed paint.

Rarities: Common is plain grey-white wool. Uncommon adds green thread at the hems. Rare adds a blue waist sash and
silver clasps and bells. Epic puts purple glass in the lantern and the vials, softly lit. Legendary lights the cowl from
inside with warm orange, sets embers drifting round the shoulders and in the scorched hems, peaks the hood, flares the
shoulders and lengthens the skirt.

Glow: any texel painted from a lit ramp (LITGLASS, WARM) is copied to the glow sheet, so paint and light never drift."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from gearlib import GearPiece
from handlib import Box, Bone, ramp, blank, clamp, cut, LIT, lightx, LEATH, TEAM, STEEL, GOLD, mail

WOOL   = ramp("2f2d2b", "4b4844", "6e6a63", "938d84", "b7b0a4", "d7d1c5", "efebe2")
ASH    = ramp("1d1c1b", "33312e", "4b4844", "67635c", "86817a")
SOOT   = ramp("0b0a0a", "171514", "24211f", "322e2b")
BONEM  = ramp("4a4436", "7a715c", "a89e84", "d0c7aa", "efe8d0")        # the half mask
GREEN  = ramp("1d4a1b", "2c7a28", "48a83c", "78d064")
BLUE   = ramp("0f1b3a", "1b3266", "2b4f9e", "4677c8", "77a2e2")
FLAME  = ramp("5a3410", "8a5a1c", "b08436")                            # the unlit wick and dull glass of Common to Rare
LITGLASS = ramp("4a1670", "7a2cb4", "a654e6", "d090fa")                # Epic: purple glass, lit
WARM   = ramp("7a2a0a", "b84a12", "ea7a22", "fbb050", "ffe2a0")        # Legendary: the light inside
DRAUGHT = ramp("1c3a24", "2e5e36", "4a8a4a")
LEGEND, EPIC, RARE, UNCOMMON = 5, 4, 3, 2
GLOWING = {tuple(c) for c in LITGLASS + WARM}

def side(face): return face not in ("top", "bottom")

def wool(r, base=4, fold=3, hem=0, R=WOOL):
    """Hanging wool: folds every `fold` columns, a lit top row, and `hem` scorched rows with the rarity's thread above."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if side(face) else 0)
                if side(face) and x % fold == fold - 1: v -= 1
                if side(face) and y == 0: v += 1
                g[y][x] = clamp(R, v)
                if side(face) and hem:
                    if y >= h - hem:
                        g[y][x] = SOOT[1 + (x + y) % 2 + (LIT[face] > 0)]
                        if r == LEGEND and y == h - 1 and x % 3 == 1: g[y][x] = WARM[2 + (x % 2)]   # embers in the char
                    elif y == h - hem - 1 and r >= UNCOMMON:
                        g[y][x] = (WARM[3] if r == LEGEND else GREEN[1 + (LIT[face] > 0) + (x % 2)])
        return g
    return paint

def wraps(r, base=3):
    """Ash-grey bandage wraps, wound on a slant."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if side(face) else 0)
                if side(face) and (x + 2 * y) % 4 == 0: v -= 1
                if side(face) and (x + 2 * y) % 4 == 1: v += 1
                g[y][x] = clamp(ASH, v)
        if side(face) and r >= UNCOMMON and h >= 2:
            for x in range(w): g[0][x] = GREEN[1 + (x % 2)] if r < LEGEND else WARM[3]
        return g
    return paint

def lantern(r):
    """A brass frame round small glass panes: a dull wick at Common to Rare, purple light at Epic, warm at Legendary."""
    G = GOLD
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                frame = not side(face) or x in (0, w - 1) or y in (0, h - 1)
                if frame: g[y][x] = clamp(G, 3 + LIT[face]); continue
                g[y][x] = (WARM[3 + (y == 1)] if r == LEGEND else LITGLASS[2 + (y == 1)] if r == EPIC else FLAME[1 + (y == 1)])
        return g
    return paint

def bell(r):
    M = STEEL if r >= RARE else GOLD
    def paint(face, w, h):
        g = [[clamp(M, 4 + LIT[face]) for _ in range(w)] for _ in range(h)]
        if r == LEGEND and face == "front": g[0][0] = WARM[4]
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

# ---- Lanternbearer's Hood: a pale cowl over a bone half mask, a small lantern charm at the left temple ----
def hood(r):
    base = wool(r, base=4)
    def paint(face, w, h):
        g = base(face, w, h)
        if face != "front": return g
        for y in range(2, h):
            for x in range(2, w - 2):
                if 3 <= y <= 5:   # the half mask: brow ridge, two eye holes, a cheek line
                    v = 3 if y == 3 else 2
                    g[y][x] = BONEM[v + (x < w // 2)]
                    if y == 4 and x in (3, w - 4): g[y][x] = WARM[3] if r == LEGEND else SOOT[0]
                    if y == 5 and x == w // 2: g[y][x] = BONEM[1]
                else:             # the dark inside of the cowl; lit warm from within at Legendary
                    g[y][x] = (WARM[1 + (y == 2) + (x == w // 2)] if r == LEGEND else SOOT[1 + (y == 2)])
        for y in range(1, h): g[y][1] = WOOL[2]; g[y][w - 2] = WOOL[3]   # the cowl's turned edge
        if r >= RARE: g[h - 1][1] = STEEL[5]; g[h - 1][w - 2] = STEEL[5]   # silver clasps at the throat
        return g
    return paint

def helm_bones():
    B = [Bone("hood", "head", tag="helm", boxes=[
        box((-4.5, -8.5, -4.5), (9, 9, 9), hood),
        box((-2, -7, 4.5), (4, 5, 1), lambda r: wool(r, base=3, fold=2))])]                  # the cowl's fall at the back
    B.append(Bone("lantern_charm", "head", tag="helm", boxes=[
        box((4.5, -6, 0), (1, 2, 1), lambda r: lambda f, w, h: [[GOLD[2]] * w for _ in range(h)]),   # its chain
        box((4.5, -4, -0.5), (2, 3, 2), lantern)]))
    B.append(Bone("hood_peak", "head", tag="helm", stages=[LEGEND], boxes=[                   # the Legendary's peaked cowl
        box((-3.5, -10.5, -3), (7, 2, 7), lambda r: wool(r, base=4)),
        box((-2, -12.5, -1), (4, 2, 5), lambda r: wool(r, base=3, fold=2)),
        box((-1, -13.5, 1), (2, 1, 3), lambda r: wool(r, base=3, fold=2))]))
    return B

# ---- Vigilant Vestment: a layered robe over light mail, a capelet, a belt of small vials ----
def vial(r):
    def paint(face, w, h):
        g = [[(LITGLASS[2 + (y == 0)] if r == EPIC else WARM[2 + (y == 0)] if r == LEGEND else DRAUGHT[1 + (y == 0)])
              for _ in range(w)] for y in range(h)]
        g[0] = [LEATH[3]] * w   # the cork
        return g
    return paint

def belt(r):
    def paint(face, w, h):
        R = BLUE if r >= RARE else LEATH
        g = [[clamp(R, 2 + LIT[face] + (lightx(face, x, w) if side(face) else 0) + (y == 0)) for x in range(w)] for y in range(h)]
        if face == "front":
            g[0][w // 2] = STEEL[5] if r >= RARE else GOLD[3]; g[-1][w // 2] = STEEL[3] if r >= RARE else GOLD[2]   # the clasp
        return g
    return paint

def sleeve(r):
    cape, chain = wool(r, base=4, fold=2), mail(base=3)
    def paint(face, w, h):
        g = chain(face, w, h)
        top = cape(face, w, 3)
        for y in range(min(3, h)): g[y] = top[y]
        return g
    return paint

def chest_bones():
    B = [Bone("robe", "body", tag="chest", boxes=[
        box((-4.5, -12.5, -2.5), (9, 12, 5), lambda r: wool(r, base=4, hem=1)),
        box((-5, -12.75, -3), (10, 3, 6), lambda r: wool(r, base=5, fold=2, hem=0)),             # the capelet layer
        box((-5, -4.5, -3), (10, 2, 6), belt)])]
    for i, x in enumerate((-3, -1, 2)):                                                       # the belt of vials
        B[0].boxes.append(box((x, -3.5, -4), (1, 2, 1), vial))
    B.append(Bone("sash_tail", "body", tag="chest", stages=[RARE, EPIC, LEGEND], boxes=[             # the Rare's blue sash end
        box((1.5, -2.5, -3.5), (2, 4, 1), lambda r: lambda f, w, h: [[clamp(BLUE, 2 + LIT[f] + (y == h - 1) - (x == 0)) for x in range(w)] for y in range(h)])]))
    B.append(Bone("stole", "body", tag="chest", team=True, boxes=[
        Box((-3, -9.5, -3.25), (2, 11, 0), ("stole", "front"), sheet="cloth"),
        Box((1, -9.5, -3.25), (2, 11, 0), ("stole", "front"), sheet="cloth"),
        Box((-4, -9.5, 3.25), (8, 8, 0), ("drape", "back"), sheet="cloth")]))
    for s, c, x in (("right", -1, -3.5), ("left", 1, -1.5)):
        B.append(Bone(f"sleeve_{s}", f"{s}_arm", tag="chest", boxes=[box((x, -2.5, -2.5), (5, 8, 5), sleeve)]))
    # the Legendary: the shoulders flare into a raised mantle and embers drift round them
    B.append(Bone("mantle_l", "body", tag="chest", stages=[LEGEND], boxes=[
        box((-8, -13.75, -3.5), (4, 2, 7), lambda r: wool(r, base=5, fold=2)),
        box((4, -13.75, -3.5), (4, 2, 7), lambda r: wool(r, base=5, fold=2))]))
    B.append(Bone("embers", "body", tag="chest", stages=[LEGEND], boxes=[
        box(o, (1, 1, 1), lambda r: lambda f, w, h: [[WARM[3 + (f == "front")]] * w for _ in range(h)])
        for o in ((-7.5, -16, -1), (6.5, -15, 1.5), (-6, -18, 2.5), (7, -18.5, -2), (-8.5, -14.5, 3))]))
    return B

def cloth(spec, side_):
    part, which = spec
    T, trim = TEAM[side_][0], TEAM[side_][1][1]
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 - (which == "back") - (1 if y >= h - 2 else 0) + (1 if x == 0 and part != "drape" else 0)
                if part == "drape" and x in (2, w - 3): v -= 1
                g[y][x] = clamp(T, v)
                if y == h - 2: g[y][x] = trim   # the gold-thread band near the hem
        if part == "stole": g[h - 1] = [T[1], None][:w] if w == 2 else g[h - 1]   # a fringed end
        elif part == "drape": cut(g, [(0, h - 1), (3, h - 1), (7, h - 1), (7, h - 2)])
        elif part == "fall": cut(g, [(0, h - 1), (w - 1, h - 1)])
        return g
    return paint

# ---- Candlewake Skirt: a long split robe, ash-grey wraps at the knee, a scorched hem ----
def legs_bones():
    B = []
    for s in ("right", "left"):
        B.append(Bone(f"skirt_{s}", f"{s}_leg", tag="legs", boxes=[
            box((-2.5, -0.5, -2.5), (5, 8, 5), lambda r: wool(r, base=3, fold=2, hem=2)),
            box((-2.5, 7.5, -2.5), (5, 1, 5), wraps)]))
        B.append(Bone(f"fall_{s}", f"{s}_leg", tag="legs", team=True, boxes=[
            Box((-1.5, -0.5, -2.75), (3, 7, 0), ("fall", "front"), sheet="cloth"),
            Box((-2.75 if s == "right" else 2.75, -0.5, -1.5), (0, 7, 3), ("fall", "side"), sheet="cloth")]))   # reads from the side
        B.append(Bone(f"tail_{s}", f"{s}_leg", tag="legs", stages=[LEGEND], boxes=[                # the Legendary's long tails
            box((-2, 7.5, 2.75), (4, 3, 1), lambda r: wool(r, base=3, fold=2, hem=2))]))
    return B

# ---- Mourner's Sandals: wrapped boots on soot-black soles, tiny bells at the ankle ----
def boots_bones():
    B = []
    for s in ("right", "left"):
        B.append(Bone(f"sandal_{s}", f"{s}_leg", tag="boots", boxes=[
            box((-2.5, 8.75, -2.5), (5, 3, 5), lambda r: wraps(r, base=2)),
            box((-2.5, 11.5, -3), (5, 1, 6), lambda r: lambda f, w, h: [[SOOT[1 + (LIT[f] > 0) + (x % 3 == 0)] for x in range(w)] for _ in range(h)]),
            box((-2, 9, -3.5), (1, 1, 1), bell), box((1, 9.5, -3.5), (1, 1, 1), bell)]))
        B.append(Bone(f"cuff_{s}", f"{s}_leg", tag="boots", stages=[LEGEND], boxes=[                # a flared wool cuff
            box((-3, 7.75, -3), (6, 1, 6), lambda r: wool(r, base=4, hem=0))]))
    return B

def pieces():
    return [GearPiece("healer_helm", "helm", helm_bones, glow=glow_cb),
            GearPiece("healer_chest", "chest", chest_bones, cloth_painter=cloth, skin=(128, 64), cloth=(64, 32), glow=glow_cb),
            GearPiece("healer_legs", "legs", legs_bones, cloth_painter=cloth, cloth=(32, 16), glow=glow_cb),
            GearPiece("healer_boots", "boots", boots_bones, glow=glow_cb)]
