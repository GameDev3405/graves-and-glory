"""Marchwarden, the commander set (docs/drafts/gear-catalogue-2026-11-03.md): Crown-Helm, Oathplate, Kingsfield Cuisses,
Marching Sabatons. The field marshal's harness: bright polished steel against Last Watch's blackened iron, a heraldic
tabard over the breastplate, a sun plate on the chest, a thin crown round the helm.

The side's colour is the heraldic tabard, front and back, with short sleeves over the upper arms, and the cloth edge
under the faulds, so it reads from every side. The catalogue's undyed Common tabard is not followed: Kort's ruling is
that the side colour reads on every rarity. Steel, gold and enamel are fixed paint.

Rarities: Common is plain steel. Uncommon adds green piping at the plate edges. Rare adds blue enamel panels. Epic sets
a purple gem in the brow and the sun, with gold filigree over the enamel. Legendary trims every edge in gold, lights the
chest sun and raises a tall solid crest that reads from behind.

Glow: any texel painted from a lit ramp (GEM, SUN) is copied to the glow sheet, so paint and light never drift."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from gearlib import GearPiece
from handlib import Box, Bone, ramp, blank, clamp, cut, LIT, lightx, TEAM, GOLD, mail

BRIGHT = ramp("1e2230", "343b4e", "50596e", "727d93", "9aa5b8", "c6cfdb", "eef3f8")   # polished steel
PIPE   = ramp("1d4a1b", "2c7a28", "48a83c", "78d064")
ENAMEL = ramp("0f1d44", "1b3478", "2c52aa", "4c78d0")
GEM    = ramp("4a1670", "8a3cd0", "c27af6", "e8b8ff")                                  # Epic: lit gem
SUN    = ramp("8a3a0a", "d0661a", "f59a32", "ffd068", "fff2c0")                        # Legendary: the lit sun
LEGEND, EPIC, RARE, UNCOMMON = 5, 4, 3, 2
GLOWING = {tuple(c) for c in GEM + SUN}

def side(face): return face not in ("top", "bottom")

def edge(r, face, v=0):
    """The plate's edge colour: none at Common, green piping, bright steel at Rare and Epic, gold at Legendary."""
    if r == LEGEND: return clamp(GOLD, 4 + (LIT[face] > 0) + v)
    if r >= RARE: return BRIGHT[6]
    if r == UNCOMMON: return PIPE[2 + (LIT[face] > 0)]
    return None

def steel(r, base=4, enamel=False, spec=True, pipe=False):
    """Polished plate: the bright ramp lit by face, a long specular streak, the top edge piped on rim pieces (`pipe`), and from Rare a blue enamel
    band through the middle of the side faces of `enamel` boxes, gold-filigreed from Epic."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if side(face) else 0)
                if side(face) and y == h - 1 and h > 2: v -= 1
                if spec and face == "front" and x == 1 and 0 < y < h - 1: v += 2
                g[y][x] = clamp(BRIGHT, v)
                if pipe and side(face) and y == 0:
                    e = edge(r, face)
                    if e: g[y][x] = e
        if enamel and r >= RARE and side(face) and h >= 4 and w >= 3:
            for y in range(h // 2 - 1, h // 2 + 1):
                for x in range(1, w - 1):
                    g[y][x] = ENAMEL[2 + (LIT[face] > 0) - (y > h // 2 - 1)]
                    if r >= EPIC and (x + y) % 3 == 0: g[y][x] = GOLD[4]
        return g
    return paint

def box(origin, size, art, sheet="skin"):
    return Box(origin, size, {k: art(k) for k in (1, 2, 3, 4, 5)}, sheet=sheet)

def glow_cb(bone, b, r):
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

# ---- Crown-Helm: an open-faced steel helm, enamelled cheek guards, a ridge crest, a thin crown band ----
def helm(r):
    base = steel(r, base=4)
    def paint(face, w, h):
        g = base(face, w, h)
        if side(face):
            for x in range(w): g[2][x] = clamp(GOLD, 3 + (LIT[face] > 0) + (x % 2))           # the crown band
        if face == "front":
            for y in range(3, h - 1):
                for x in range(2, w - 2): g[y][x] = None                                   # the open face
            for y in range(3, h - 1):                                                      # cheek guards
                for x in (0, 1, w - 2, w - 1):
                    g[y][x] = (ENAMEL[2 + (x in (0, w - 2))] if r >= RARE and x in (1, w - 2) and y < h - 2
                               else clamp(BRIGHT, 4 + (x == 1) - (y == h - 2)))
            g[h - 1] = [clamp(BRIGHT, 3)] * w
            e = edge(r, face)
            if e: g[h - 1] = [e] * w
            if r >= EPIC: g[2][w // 2] = (SUN[3] if r == LEGEND else GEM[2]); g[1][w // 2] = clamp(GOLD, 5)   # the brow jewel
        return g
    return paint

def crown_points(r):
    return lambda f, w, h: [[clamp(GOLD, 4 + (LIT[f] > 0) + (r == LEGEND)) for _ in range(w)] for _ in range(h)]

def helm_bones():
    B = [Bone("crown_helm", "head", tag="helm", boxes=[
        box((-4.5, -8.5, -4.5), (9, 9, 9), helm),
        box((-0.5, -9.5, -4), (1, 1, 8), lambda r: steel(r, base=5, spec=False)),                   # the ridge crest
        box((-3.5, -9.5, -4.75), (1, 1, 1), crown_points), box((2.5, -9.5, -4.75), (1, 1, 1), crown_points),
        box((-4.75, -9.5, -0.5), (1, 1, 1), crown_points), box((3.75, -9.5, -0.5), (1, 1, 1), crown_points),
        box((-3.5, -0.5, 4.5), (7, 2, 1), lambda r: steel(r, base=3, spec=False))])]                  # the neck guard
    B.append(Bone("great_crest", "head", tag="helm", stages=[LEGEND], boxes=[                        # tall, solid, reads from behind
        box((-0.5, -13.5, -3), (1, 4, 9), lambda r: steel(r, base=5, enamel=True, spec=False)),
        box((-1.5, -12.5, -5), (3, 3, 1), sun),                                                      # a sun finial above the brow
        box((-0.5, -14.5, -1), (1, 1, 6), lambda r: lambda f, w, h: [[clamp(GOLD, 4 + (LIT[f] > 0)) for _ in range(w)] for _ in range(h)])]))
    return B

# ---- Oathplate: a polished breastplate under a heraldic tabard, a sun plate on the chest, round pauldrons ----
def sun(r):
    def paint(face, w, h):
        if face != "front":
            return [[(SUN[2] if r == LEGEND else clamp(GOLD, 3 + (LIT[face] > 0))) for _ in range(w)] for _ in range(h)]
        g = blank(w, h); c = (w - 1) / 2
        for y in range(h):
            for x in range(w):
                d = abs(x - c) + abs(y - c)
                g[y][x] = BRIGHT[2 + (x < c)]                                       # the steel backing plate
                if d <= 1: g[y][x] = SUN[3] if r == LEGEND else clamp(GOLD, 5 - (d == 1))
                elif d == 2 or (x in (0, w - 1) and y == c) or (y in (0, h - 1) and x == c):
                    if (x + y) % 2 == 0 or d == 2: g[y][x] = SUN[2] if r == LEGEND else GOLD[3]
        if r == EPIC: g[int(c)][int(c)] = GEM[3]
        if r == LEGEND: g[int(c)][int(c)] = SUN[4]
        return g
    return paint

def pauldron(r):
    base = steel(r, base=4, enamel=True, pipe=True)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "top":
            for y in range(h):
                for x in range(w):
                    dx, dy = abs(x - (w - 1) / 2), abs(y - (h - 1) / 2)
                    g[y][x] = BRIGHT[6] if dx + dy < 1.5 else BRIGHT[5] if dx + dy < 3 else BRIGHT[4]
                    if r == LEGEND and (x in (0, w - 1) or y in (0, h - 1)): g[y][x] = GOLD[4]
        return g
    return paint

def chest_bones():
    B = [Bone("oathplate", "body", tag="chest", boxes=[
        box((-4.5, -12.5, -2.5), (9, 12, 5), lambda r: steel(r, base=4)),
        box((-3.5, -13.5, -3), (7, 1, 6), lambda r: steel(r, base=4, spec=False, pipe=True)),                  # the gorget
        box((-2.5, -11, -3.5), (5, 5, 1), sun),                                                     # the sun plate, over the tabard
        box((-5, -3.5, -3), (10, 2, 6), lambda r: steel(r, base=3, spec=False, pipe=True))])]                  # the waist lame
    B.append(Bone("tabard", "body", tag="chest", team=True, boxes=[
        Box((-3, -10.5, -2.75), (6, 11, 0), ("tabard", "front"), sheet="cloth"),
        Box((-3, -10.5, 2.75), (6, 11, 0), ("tabard", "back"), sheet="cloth")]))
    for s, x in (("right", -4), ("left", -1)):
        B.append(Bone(f"pauldron_{s}", f"{s}_arm", tag="chest", boxes=[
            box((x, -3, -3), (5, 4, 6), pauldron),
            box((x + 0.5, 4.5, -2.5), (5, 3, 5), lambda r: mail(base=3))]))   # mail below the sleeve
        inner, outer = (x + 4, x - 1) if s == "right" else (x, x + 5)
        B.append(Bone(f"haute_{s}", f"{s}_arm", tag="chest", stages=[LEGEND], boxes=[                 # the Legendary's raised guards
            box((inner, -6, -3), (1, 3, 6), lambda r: steel(r, base=5, spec=False)),
            box((outer, -3.5, -3.5), (1, 4, 7), lambda r: steel(r, base=4, enamel=True, spec=False))]))
        B.append(Bone(f"sleeve_{s}", f"{s}_arm", tag="chest", team=True, boxes=[
            Box((x + 0.5, 1, -2.5), (5, 3, 5), ("sleeve", "wrap"), sheet="cloth")]))                # the tabard's short sleeve
    return B

def cloth(spec, side_):
    part, which = spec
    T, trim = TEAM[side_][0], TEAM[side_][1][1]
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 - (which == "back") + (LIT.get(face, 0) if part != "tabard" else 0)
                if part == "tabard" and x in (0, w - 1): v -= 1
                if part == "tabard" and y == 0: v -= 1
                g[y][x] = clamp(T, v)
        if part == "tabard":
            for y in range(1, h - 1): g[y][0] = g[y][w - 1] = trim                   # the gold-thread border
            g[h - 1] = [trim] * w
            cut(g, [(w // 2 - 1, h - 1), (w // 2, h - 1)])                          # the split at the hem
        elif part in ("sleeve", "edge"):
            g[h - 1] = [trim if x % 2 == 0 else T[1] for x in range(w)]              # a dagged, gold-edged hem
        return g
    return paint

# ---- Kingsfield Cuisses: plate thighs over mail, faulds at the hip with a cloth edge, a knee cop ----
def legs_bones():
    B = []
    for s in ("right", "left"):
        B.append(Bone(f"cuisse_{s}", f"{s}_leg", tag="legs", boxes=[
            box((-3, -1.5, -3), (6, 2, 6), lambda r: steel(r, base=4, spec=False, pipe=True)),                   # the fauld lame
            box((-2.5, 1.5, -2.5), (5, 4, 5), lambda r: steel(r, base=4, enamel=True)),
            box((-2, 5, -3), (4, 2, 1), lambda r: steel(r, base=5, spec=False)),                      # the knee cop
            box((-2.5, 5.5, -2.5), (5, 2, 5), lambda r: mail(base=3))]))
        B.append(Bone(f"fauld_edge_{s}", f"{s}_leg", tag="legs", team=True, boxes=[
            Box((-3, 0.5, -3), (6, 1, 6), ("edge", "wrap"), sheet="cloth")]))
    return B

# ---- Marching Sabatons: clean steel boots, a pointed toe, a gold spur ----
def boots_bones():
    B = []
    for s in ("right", "left"):
        B.append(Bone(f"sabaton_{s}", f"{s}_leg", tag="boots", boxes=[
            box((-2.5, 7.75, -2.5), (5, 4, 5), lambda r: steel(r, base=4, pipe=True)),
            box((-2, 10.75, -3.5), (4, 1, 1), lambda r: steel(r, base=4, spec=False)),                 # the toe
            box((-2.5, 11.75, -3.5), (5, 1, 6), lambda r: lambda f, w, h: [[BRIGHT[1 + (LIT[f] > 0)] for _ in range(w)] for _ in range(h)]),
            box((-0.5, 10.25, 2.5), (1, 1, 2), lambda r: lambda f, w, h: [[clamp(GOLD, 4 + (LIT[f] > 0)) for _ in range(w)] for _ in range(h)])]))
    return B

def pieces():
    return [GearPiece("commander_helm", "helm", helm_bones, glow=glow_cb),
            GearPiece("commander_chest", "chest", chest_bones, cloth_painter=cloth, skin=(128, 64), cloth=(64, 32), glow=glow_cb),
            GearPiece("commander_legs", "legs", legs_bones, cloth_painter=cloth, cloth=(32, 16), glow=glow_cb),
            GearPiece("commander_boots", "boots", boots_bones, glow=glow_cb)]
