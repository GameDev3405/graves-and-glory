"""Frostmarch Hunt, the ranged set (docs/drafts/gear-catalogue-2026-11-03.md): Stalker's Maskhood, Pass-Warden Jerkin,
Snowshod Leggings, Silent Hunter's Boots. Mountain stalkers: layered hide, fur ruffs, lashed bone, frost on the boots.
The side's colour is a wound scarf round the neck with tails front and back, and a cloth band bound round each thigh,
so it reads from every side. Hide, fur and bone are fixed paint.

Rarities: Common is dull brown hide. Uncommon adds green moss at the fur trims. Rare turns the fur blue-grey and carves
the bone. Epic scratches purple-lit runes into the mask, jerkin and guards. Legendary adds an orange feather plume,
a pale glowing eye in the mask, longer antlers and feathered shoulders.

Glow: any texel painted from a lit ramp (RUNE, EYE) is copied to the glow sheet, so paint and light never drift."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from gearlib import GearPiece
from handlib import Box, Bone, ramp, blank, clamp, cut, LIT, lightx, TEAM

HIDE   = ramp("24170e", "3a2616", "54381f", "6e4a2a", "8a6038", "a67a4a")
FURB   = ramp("2a1f16", "45362a", "65523f", "887158", "a89272")              # brown fur, Common and Uncommon
FURG   = ramp("232a33", "3a4552", "5a6878", "7f8e9e", "a9b6c2")              # blue-grey fur, Rare and up
BONEM  = ramp("4a4436", "7a715c", "a89e84", "d0c7aa", "efe8d0")
MOSS   = ramp("1f3a16", "33602a", "4f8a3c", "78b05a")
FROST  = ramp("6f8796", "a4c0cf", "d8eaf2")
RUNE   = ramp("4a1670", "8a3cd0", "c27af6")                                  # Epic: lit rune scratches
EYE    = ramp("9fd8e6", "d6f4fa", "f6feff")                                  # Legendary: the pale eye
FEATHER = ramp("5a2208", "9a3e10", "d8661c", "f59a3e", "fcc77a")
LEGEND, EPIC, RARE, UNCOMMON = 5, 4, 3, 2
GLOWING = {tuple(c) for c in RUNE + EYE}

def side(face): return face not in ("top", "bottom")
def shade(face, x, w): return LIT[face] + (lightx(face, x, w) if side(face) else 0)

def runes(g, face, r, at):
    """Scratch the Epic's lit runes (and keep them at Legendary) into a front face at the given texels."""
    if r >= EPIC and face == "front":
        for x, y in at:
            if 0 <= y < len(g) and 0 <= x < len(g[0]): g[y][x] = RUNE[1 + (y % 2)]
    return g

def hide(r, base=3, layers=0, rune_at=()):
    """Hide leather: scuffs, and with `layers` an overlapping plate every that many rows (a dark lip, a lit edge)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + shade(face, x, w)
                if side(face) and layers and y % layers == layers - 1: v -= 2
                elif side(face) and layers and y % layers == 0: v += 1
                if (x * 7 + y * 3) % 11 == 0: v -= 1
                g[y][x] = clamp(HIDE, v)
        return runes(g, face, r, rune_at)
    return paint

def fur(r, base=2):
    """A fur ruff: tufted strands, brown until Rare, then blue-grey; moss in the tips from Uncommon."""
    F = FURG if r >= RARE else FURB
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + shade(face, x, w) + (1 if (x + y) % 3 == 0 else 0) - (1 if x % 2 and y == h - 1 else 0)
                g[y][x] = clamp(F, v)
                if r >= UNCOMMON and side(face) and ((y == h - 1 and x % 3 != 2) or (y == h - 2 and x % 4 == 1)):
                    g[y][x] = MOSS[1 + (r >= RARE) + (x % 2 and LIT[face] >= 0)]
        return g
    return paint

def bone(r, base=2, rune_at=()):
    """Bone: plain at Common and Uncommon, carved with dark grooves from Rare, rune-lit from Epic."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + shade(face, x, w) + (y == 0)
                if r >= RARE and side(face) and (x + y) % 3 == 0 and 0 < y < h - 1: v -= 2
                g[y][x] = clamp(BONEM, v)
        return runes(g, face, r, rune_at)
    return paint

def lashing(r, base):
    """Bone with two rawhide lashings across it."""
    b = bone(r, base, rune_at=((1, 1),))
    def paint(face, w, h):
        g = b(face, w, h)
        if side(face):
            for y in (0, h - 1):
                for x in range(w): g[y][x] = HIDE[1 + (x % 2)]
        return g
    return paint

def flat(R, v): return lambda r: lambda f, w, h: [[clamp(R, v + LIT[f]) for _ in range(w)] for _ in range(h)]

def box(origin, size, art, sheet="skin"):
    return Box(origin, size, {k: art(k) for k in (1, 2, 3, 4, 5)}, sheet=sheet)

def glow_cb(bone_, b, r):
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

# ---- Stalker's Maskhood: a hide hood with a fur rim, a bone half mask over the eyes, antler nubs ----
def hood(r):
    base, rim = hide(r, base=3), fur(r, base=2)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            f = rim(face, w, h)
            for y in range(h):
                for x in range(w):
                    if x in (0, w - 1) or y == 0: g[y][x] = f[y][x]                 # the fur rim round the opening
                    elif y >= 2: g[y][x] = HIDE[0] if y >= 6 else g[y][x]           # the shadowed jaw
            for y in range(2, 6):                                                   # the bone half mask
                for x in range(1, w - 1):
                    v = 3 - (y == 5) + (x < w // 2)
                    if r >= RARE and y == 4 and x in (2, w - 3): v -= 2             # carved cheek grooves
                    g[y][x] = clamp(BONEM, v)
            for x in (2, w - 3):                                                    # the eye slits
                g[3][x] = EYE[1 + (x == 2)] if r == LEGEND else HIDE[0]
                g[3][x + (1 if x == 2 else -1)] = EYE[0] if r == LEGEND else HIDE[0]
            g[2][w // 2] = BONEM[1]
            runes(g, face, r, ((w // 2, 4), (w // 2, 5), (1, 2), (w - 2, 2)))
        elif face == "top" or side(face):
            for x in range(w): g[h - 1][x] = clamp(FURG if r >= RARE else FURB, 2 + LIT[face] + (x % 2))   # the fur rim's underside
        return g
    return paint

def helm_bones():
    B = [Bone("maskhood", "head", tag="helm", boxes=[
        box((-4.5, -8.5, -4.5), (9, 9, 9), hood),
        box((-3, -6, 4.5), (6, 6, 1), lambda r: hide(r, base=2, layers=3))])]                     # the hood's back fall
    B.append(Bone("antler_r", "head", tag="helm", pivot=(-2.5, -8.5, -1), rot=(0, 0, 20), boxes=[
        box((-0.5, -2, -0.5), (1, 2, 1), lambda r: bone(r, 2)), box((-1.5, -3, -0.5), (1, 1, 1), lambda r: bone(r, 3))]))
    B.append(Bone("antler_l", "head", tag="helm", pivot=(2.5, -8.5, -1), rot=(0, 0, -20), boxes=[
        box((-0.5, -2, -0.5), (1, 2, 1), lambda r: bone(r, 2)), box((0.5, -3, -0.5), (1, 1, 1), lambda r: bone(r, 3))]))
    B.append(Bone("antler_tines", "head", tag="helm", stages=[LEGEND], boxes=[                    # the Legendary's long tines
        box((-5.5, -12.5, -1.5), (1, 2, 1), lambda r: bone(r, 3)), box((4.5, -12.5, -1.5), (1, 2, 1), lambda r: bone(r, 3)),
        box((-4.5, -13.5, -0.5), (1, 1, 1), lambda r: bone(r, 4)), box((3.5, -13.5, -0.5), (1, 1, 1), lambda r: bone(r, 4))]))
    B.append(Bone("plume", "head", tag="helm", stages=[LEGEND], pivot=(0, -8.5, 3), rot=(-30, 0, 0), boxes=[  # orange feathers, back of the crown
        box((-0.5, -6, -1), (1, 6, 2), lambda r: lambda f, w, h: [[FEATHER[min(4, 1 + y // 2 + (x % 2))] for x in range(w)] for y in range(h)]),
        box((-1.5, -4, 0), (1, 4, 1), lambda r: lambda f, w, h: [[FEATHER[2 + (y < 2)] for _ in range(w)] for y in range(h)]),
        box((0.5, -5, 0), (1, 5, 1), lambda r: lambda f, w, h: [[FEATHER[1 + (y < 3)] for _ in range(w)] for y in range(h)])]))
    return B

# ---- Pass-Warden Jerkin: layered hide, a fur ruff, the quiver harness across the chest, a wound scarf ----
def harness(r):
    def paint(face, w, h):
        g = [[clamp(HIDE, 1 + LIT[face] + (y == 0)) for x in range(w)] for y in range(h)]
        if face == "front":
            for x in range(1, w, 4): g[h // 2][x] = BONEM[3]                     # bone toggles
        return g
    return paint

def chest_bones():
    B = [Bone("jerkin", "body", tag="chest", boxes=[
        box((-4.5, -12.5, -2.5), (9, 12, 5), lambda r: hide(r, base=3, layers=3, rune_at=((2, 4), (3, 5), (6, 7), (5, 8)))),
        box((-5.5, -13, -3), (11, 2, 6), lambda r: fur(r, base=2)),                              # the ruff over the shoulders
        box((-5, -2.5, -3), (10, 2, 6), lambda r: hide(r, base=1)),                              # the belt
        box((2, -3, -4), (2, 3, 1), lambda r: bone(r, 3, rune_at=((0, 1),)))])]                  # the bone buckle
    B.append(Bone("harness", "body", tag="chest", pivot=(0, -6, 0), rot=(0, 0, 40), boxes=[        # strap, right hip to left shoulder
        box((-6.5, -1, -3.25), (13, 2, 1), harness),
        box((-6.5, -1, 2.25), (13, 2, 1), harness)]))
    B.append(Bone("knife", "body", tag="chest", boxes=[
        box((-4, -5, -3.75), (1, 4, 1), lambda r: hide(r, base=1)), box((-4, -6, -3.75), (1, 1, 1), lambda r: bone(r, 3))]))
    B.append(Bone("scarf", "body", tag="chest", team=True, boxes=[
        Box((-5, -11.25, -3.5), (10, 2, 7), ("scarf", "wrap"), sheet="cloth"),
        Box((-3.5, -9.25, -3.5), (2, 6, 0), ("scarf", "tail"), sheet="cloth"),
        Box((0.5, -9.25, 3.5), (3, 7, 0), ("scarf", "tail"), sheet="cloth")]))
    for s, x in (("right", -3.5), ("left", -1.5)):
        B.append(Bone(f"sleeve_{s}", f"{s}_arm", tag="chest", boxes=[
            box((x, -2.5, -2.5), (5, 6, 5), lambda r: hide(r, base=3, layers=3)),
            box((x, 5.5, -2.5), (5, 2, 5), lambda r: hide(r, base=2))]))                          # the bracer
    B.append(Bone("feathered", "body", tag="chest", stages=[LEGEND], boxes=[                       # the Legendary's feathered shoulders
        box((-7.5, -14, -2.5), (2, 1, 5), lambda r: lambda f, w, h: [[FEATHER[2 + (x % 2) + (f == "top")] for x in range(w)] for _ in range(h)]),
        box((5.5, -14, -2.5), (2, 1, 5), lambda r: lambda f, w, h: [[FEATHER[2 + (x % 2) + (f == "top")] for x in range(w)] for _ in range(h)])]))
    return B

def cloth(spec, side_):
    part, which = spec
    T, trim = TEAM[side_][0], TEAM[side_][1][1]
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT.get(face, 0) // 2 - (1 if (x + y) % 4 == 0 else 0)
                if part == "band" and y == 0: v += 1
                g[y][x] = clamp(T, v)
        if part == "wrap":
            for x in range(w):
                if x % 4 == 1: g[h - 1][x] = trim                                 # the stitched hem of the wound scarf
        elif part == "tail":
            g[h - 2] = [trim] * w
            cut(g, [(0, h - 1)] if w == 2 else [(0, h - 1), (2, h - 1)])
        elif part == "band":
            for y in range(h): g[y][0] = T[1]                                    # the knot's shadow
        return g
    return paint

# ---- Snowshod Leggings: hide legs, a fur rim at the hip, lashed bone guards, a cloth band round the thigh ----
def legs_bones():
    B = []
    for s in ("right", "left"):
        B.append(Bone(f"legging_{s}", f"{s}_leg", tag="legs", boxes=[
            box((-2.5, -0.5, -2.5), (5, 7, 5), lambda r: hide(r, base=2, layers=4)),
            box((-1.5, 2, -3), (3, 4, 1), lambda r: lashing(r, 2))]))
        B.append(Bone(f"band_{s}", f"{s}_leg", tag="legs", team=True, boxes=[
            Box((-3, 0, -3.25), (6, 2, 6), ("band", "wrap"), sheet="cloth")]))
    return B

# ---- Silent Hunter's Boots: soft tall boots, a fur cuff, a bone buckle, frost on the toes ----
def boot(r):
    base = hide(r, base=2)
    def paint(face, w, h):
        g = base(face, w, h)
        if side(face):
            for y in range(h - 2, h):
                for x in range(w):
                    if (x * 5 + y * 3) % 7 in (0, 3): g[y][x] = FROST[(LIT[face] > 0) + (x % 3 == 0)]   # frost stains on the foot
        return g
    return paint

def boots_bones():
    B = []
    for s in ("right", "left"):
        B.append(Bone(f"boot_{s}", f"{s}_leg", tag="boots", boxes=[
            box((-2.5, 6.75, -2.5), (5, 5, 5), boot),
            box((-3, 6.25, -3), (6, 1, 6), fur),
            box((-2.5, 11.5, -3), (5, 1, 6), lambda r: lambda f, w, h: [[HIDE[0 + (LIT[f] > 0)] for _ in range(w)] for _ in range(h)]),
            box((-1, 8.25, -3), (2, 1, 1), lambda r: bone(r, 3))]))
    return B

def pieces():
    return [GearPiece("ranged_helm", "helm", helm_bones, glow=glow_cb),
            GearPiece("ranged_chest", "chest", chest_bones, cloth_painter=cloth, skin=(128, 64), cloth=(64, 32), glow=glow_cb),
            GearPiece("ranged_legs", "legs", legs_bones, cloth_painter=cloth, cloth=(32, 16), glow=glow_cb),
            GearPiece("ranged_boots", "boots", boots_bones, glow=glow_cb)]
