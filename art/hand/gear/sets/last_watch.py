"""Last Watch, the bruiser set (docs/drafts/gear-catalogue-2026-11-03.md): Watchman's Greathelm, Wallbreaker Cuirass,
Gatepost Greaves, Cairn-Treads. Old gate-guard iron, heavy and battered. Common is bare grey iron with plain rivets; the
Legendary is blackened iron with ember light in its cracks, a horned crest, and extra stacked plates on the pauldron,
knees and boots. Every box has whole-texel sizes and no dilation, so one texel is one model pixel.

Uncommon dyes the leather straps green; Rare is blued steel; Epic is dark gunmetal with purple inlay lines whose seams
glow faintly (the glow sheet lights the inlay texels)."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from gearlib import GearPiece
from handlib import Box, Bone, ramp, blank, clamp, grid, over, cut, LIT, lightx, LEATH, TEAM

IRON  = ramp("16171c", "2a2b31", "43444b", "5f5f66", "80807f", "a5a39c", "cfccc2")
BLACK = ramp("0e0d10", "1c1a1e", "2c292c", "403a3a", "5a514c", "786c62", "9a8c7c")
EMBER = ramp("5a1a08", "9a3510", "d4601a", "f39a3a", "ffd27a")
MUD   = ramp("1e1812", "2e251b", "413426", "58472f", "6e5a3c")
INK   = BLACK[0]
BLUED = ramp("10141f", "1c2436", "2b3a56", "3f557a", "5c779e", "84a0c4", "b8cce4")
GUN   = ramp("121218", "1e1e28", "2e2d3c", "434055", "5d5872", "7f7894", "aaa2bc")
INLAY = ramp("3a0f52", "6a1c9c", "9a38d6", "c470f2")   # gear-rarity purple, kept clear of the Graveborn ramp
GREEN_LEATH = ramp("0f1a0c", "1c3016", "2c4a20", "3e6a2c", "56903a", "7cb456")
LEGEND, EPIC = 5, 4

def metal(r): return {3: BLUED, 4: GUN, 5: BLACK}.get(r, IRON)
def leath(r): return GREEN_LEATH if r == 2 else LEATH

def P(r):
    """Grid palette: 0-6 the rarity's metal, k ink, A-F leather, m-p mud, e-i ember (Legendary only; iron dark otherwise)."""
    M = metal(r)
    p = {str(i): c for i, c in enumerate(M)}
    p.update({"k": INK}); p.update({ch: leath(r)[i] for i, ch in enumerate("ABCDEF")}); p.update({ch: MUD[i] for i, ch in enumerate("mnopq")})
    lit = r == LEGEND
    p.update({ch: (EMBER[i] if lit else M[max(0, i - 1)]) for i, ch in enumerate("efghi")})
    return p

GLOW = set("efghi")

def iron(r, base=3, rim=True, seam=True, spec=True):
    """Hammered plate: the metal ramp lit by face, a bright rim, a dark lower seam, and a scatter of dents."""
    M = metal(r)
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face]
                if face in ("top", "bottom"):
                    if face == "top" and (x in (0, w - 1) or y in (0, h - 1)): v -= 1
                    g[y][x] = clamp(M, v + (1 if face == "top" and (x * 7 + y * 3) % 11 == 0 else 0)); continue
                v += lightx(face, x, w)
                if rim and y == 0: v += 1
                if seam and y == h - 1 and h > 2: v -= 1
                if spec and face == "front" and w >= 5 and x == 1 and 0 < y < h - 2: v += 1
                if (x * 5 + y * 3) % 13 == 0 and 0 < y < h - 1: v -= 1   # dents
                g[y][x] = clamp(M, v)
                if r == EPIC and h >= 3 and y == 1 and (x + (face == "back")) % 4 != 3:   # an inlay line under the rim
                    g[y][x] = INLAY[2 + (LIT[face] > 0)]
        return g
    return paint

def stamped(painter, r, faces):
    """painter with face grids stamped over it; '.' keeps the base."""
    def paint(face, w, h):
        g = painter(face, w, h)
        if face in faces: g = over(g, grid(faces[face], P(r)))
        return g
    return paint

def ember_of(r, faces):
    """The glow sheet's painter for a stamped box: only the ember letters, at the Legendary."""
    if r != LEGEND: return None
    def paint(face, w, h):
        g = blank(w, h)
        if face in faces:
            for y, line in enumerate(faces[face].strip("\n").splitlines()):
                for x, ch in enumerate(line.split("|")[0].replace(" ", "")):
                    if ch in GLOW and y < h and x < w: g[y][x] = EMBER[min(4, "efghi".index(ch) + 1)]
        return g
    return paint

def box(origin, size, art, glow=None, sheet="skin"):
    """A Box painted per rarity by art(r) -> painter; glow(r) -> painter or None for the glow sheet."""
    b = Box(origin, size, {r: art(r) for r in (1, 2, 3, 4, 5)}, sheet=sheet)
    b.glow = {r: glow(r) for r in (1, 2, 3, 4, 5)} if glow else {}
    return b

def glow_cb(bone, b, r):
    g = b.glow.get(r) if getattr(b, "glow", None) else None
    if g or r != EPIC: return g
    src = b.paint.get(EPIC, b.paint.get(0)); inlay = set(INLAY)
    def paint(face, w, h):   # the Epic's faint seam glow: its inlay texels, lit
        out = blank(w, h)
        for y, line in enumerate(src(face, w, h)):
            for x, c in enumerate(line):
                if c in inlay: out[y][x] = c
        return out
    return paint

# ---- Watchman's Greathelm: a squat iron bucket, one narrow eye slit, a cracked brow plate ----
HELM_C = {
"front": """
..........
.5......5.
..4....4..
1kkk55kkk1
....44....
....33.k..
.2..33.k..
..2.32....
.5......5.""",
"right": """
..........
.5......5.
3333333333
..........
..........
..k.k.....
...k.k....
..........
..........""",
"left": """
..........
.5......5.
3333333333
..........
..........
.....k.k..
....k.k...
..........
..........""",
"back": """
..........
.5......5.
3333333333
....22....
....22....
....22....
....22....
....22....
.5......5.""",
}
HELM_L = {   # the Legendary: blackened, one ember crack down the brow and a dim light behind the slit
"front": """
..........
.5....f.5.
..4..g.4..
1kkfh5kkk1
....4h....
....3g.k..
....f3.k..
....33....
.5......5.""",
"right": HELM_C["right"].replace("..k.k.....", "..k.k..f..").replace("...k.k....", "...k.k.e.."),
"left": HELM_C["left"], "back": HELM_C["back"],
}

def helm_bones():
    B = [Bone("helm", "head", tag="helm", boxes=[box((-5, -9, -5), (10, 9, 10),
            lambda r: stamped(iron(r, base=3), r, HELM_L if r == LEGEND else HELM_C),
            lambda r: ember_of(r, HELM_L))])]
    # the Legendary's crest: a notched iron ridge front to back and two horns swept up and back from the temples
    ridge = {"right": "4.4.4.4.\n33333333", "left": "4.4.4.4.\n33333333"}
    B.append(Bone("helm_crest", "head", tag="helm", stages=[LEGEND], boxes=[
        box((-1, -11, -4), (2, 2, 8), lambda r: stamped(iron(r, base=3, seam=False), r, ridge))]))
    for side, c in (("right", -1), ("left", 1)):
        horn = [((5, -7, -1), (2, 3, 3)), ((6, -9, 0), (2, 2, 2)), ((7, -11, 1), (1, 2, 2)), ((7, -12, 2), (1, 1, 1))]
        boxes = []
        for (x, y, z), (w, h, d) in horn:
            ox = x if c > 0 else -x - w
            boxes.append(box((ox, y, z), (w, h, d), lambda r: iron(r, base=3, seam=False, spec=False)))
        B.append(Bone(f"helm_horn_{side}", "head", tag="helm", stages=[LEGEND], boxes=boxes))
    B.append(Bone("helm_plume", "head", tag="helm", team=True, stages=[LEGEND], boxes=[   # horsehair in the side's colour along the crest
        Box((-0.5, -13, -4.5), (1, 2, 8), ("plume", "top"), sheet="cloth")]))
    return B

# ---- Wallbreaker Cuirass: a slab breastplate in riveted bands, one huge pauldron on the left, a tabard strip ----
CUIRASS = {
"front": """
.5.....5.
.........
3333333331
.6.....6.
.........
2222222221
.6.....6.
.........
.........""",
"back": """
.5.....5.
....3....
2222222221
.5.....5.
....3....
2222222221
.5.....5.
....3....
.........""",
}
CUIRASS_L = {
"front": """
.5.....5.
...f.....
3333g33331
.6..h..6.
....e....
2222222221
.6..f..6.
.....g...
.........""",
"back": CUIRASS["back"],
}

def strap(r, base=2):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face not in ("top", "bottom") and y == 0: v += 1
                g[y][x] = clamp(leath(r), v)
        if face == "front" and w >= 3 and h >= 2:   # an iron buckle
            M = metal(r); g[0][w // 2] = M[5]; g[h - 1][w // 2] = M[3]
        return g
    return paint

def fauld(r):
    def paint(face, w, h):
        g = iron(r, base=2, spec=False)(face, w, h)
        if face in ("front", "back", "left", "right"):
            for x in range(0, w, 3): g[0][x] = metal(r)[5]   # rivets along the lame
        return g
    return paint

def pauldron(r, part):
    M = metal(r)
    def paint(face, w, h):
        if face == "bottom": return iron(r, base=1, rim=False, spec=False)(face, w, h)
        if face == "top":
            g = blank(w, h)
            for y in range(h):
                for x in range(w):
                    dx, dy = abs(x - (w - 1) / 2), abs(y - (h - 1) / 2)
                    g[y][x] = M[5] if dx + dy < 1.5 else (M[4] if dx + dy < 3.5 else M[3])
                    if part != "dome": g[y][x] = M[2]
                    if (x * 5 + y * 7) % 13 == 0: g[y][x] = M[2]
            return g
        base = max(1, min(5, (4 if part == "dome" else 3) + LIT[face]))
        g = [[M[base] for _ in range(w)] for _ in range(h)]
        for x in range(w):
            g[0][x] = M[min(6, base + (2 if part == "dome" else 1))]
            g[h - 1][x] = M[1]
            if x % 3 == 1 and part != "dome": g[0][x] = M[6]   # rivets
            if r == EPIC and h >= 3 and x % 4 != 3: g[1][x] = INLAY[2 + (LIT[face] > 0)]
        return g
    return paint

PAULDRON_L = {"front": "........\n..f..g..\n........", "left": "........\n.e....f.\n........"}

def chest_bones():
    B = [Bone("cuirass", "body", tag="chest", boxes=[
        box((-4.5, -12.5, -2.5), (9, 9, 5), lambda r: stamped(iron(r, base=3), r, CUIRASS_L if r == LEGEND else CUIRASS),
            lambda r: ember_of(r, CUIRASS_L)),
        box((-4.5, -3.5, -2.5), (9, 2, 5), lambda r: strap(r)),
        box((-5, -1.5, -3), (10, 2, 6), fauld)])]
    # the side's colour, readable at a glance from any angle: a broad tabard front and back, and a short mantle over
    # the shoulders behind (back gear, not a standard) that ends at the belt so the tabard shows below it
    B.append(Bone("tabard", "body", tag="chest", team=True, boxes=[
        Box((-3, -1.5, -3.25), (6, 10, 0), ("tabard", "front"), sheet="cloth"),
        Box((-3, -1.5, 3.25), (6, 10, 0), ("tabard", "back"), sheet="cloth"),
        Box((-4.5, -12.5, 3.75), (9, 10, 0), ("mantle", "back"), sheet="cloth")]))
    # a sash slung from the great pauldron across the breastplate to the right hip, so the front reads as well
    B.append(Bone("sash", "body", (0, -8, 0), rot=(0, 0, -35), tag="chest", team=True, boxes=[
        Box((-5.5, -1.5, -2.75), (11, 3, 0), ("sash", "front"), sheet="cloth")]))
    # the great pauldron on the wearer's left: a broad dome over two lames, rolled outward from the shoulder
    B.append(Bone("pauldron_left", "left_arm", (1, -2, 0), rot=(0, 0, 14), tag="chest", boxes=[
        box((-4, -2, -4), (8, 3, 8), lambda r: stamped(pauldron(r, "dome"), r, PAULDRON_L if r == LEGEND else {}),
            lambda r: ember_of(r, PAULDRON_L)),
        box((-3.5, 1, -3.5), (8, 2, 7), lambda r: pauldron(r, "lame1")),
        box((-3, 2.5, -3), (8, 2, 6), lambda r: pauldron(r, "lame2"))]))
    B.append(Bone("pauldron_left_l", "pauldron_left", tag="chest", stages=[LEGEND], boxes=[
        box((-3, -3, -3), (6, 1, 6), lambda r: pauldron(r, "dome")),               # a second plate stacked on the dome
        box((-4.5, -5, -4.5), (1, 3, 9), lambda r: iron(r, base=4, seam=False)),   # the raised guard at the neck
        box((-2.5, 4, -2.5), (8, 2, 5), lambda r: pauldron(r, "lame2"))]))         # a third lame
    B.append(Bone("pauldron_right", "right_arm", (-1, -2, 0), rot=(0, 0, -14), tag="chest", boxes=[
        box((-3, -1.5, -3), (6, 2, 6), lambda r: pauldron(r, "dome")),
        box((-3.5, 0.5, -2.5), (6, 2, 5), lambda r: pauldron(r, "lame1"))]))
    B.append(Bone("pauldron_right_l", "pauldron_right", tag="chest", stages=[LEGEND], boxes=[
        box((-4, 2, -2), (6, 2, 4), lambda r: pauldron(r, "lame2"))]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"gauntlet_{side}", f"{side}_arm", tag="chest", boxes=[
            box((c - 2.5, 4, -2.5), (5, 3, 5), lambda r: iron(r, base=3)),
            box((c - 2.5, 7.5, -2.5), (5, 3, 5), lambda r: stamped(iron(r, base=2, rim=False), r,
                {"front": "4.4.4\n.....\n3333."})),
        ]))
    return B

def tabard(spec, side):
    part, which = spec
    T, trim = TEAM[side][0], TEAM[side][1][1]
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if part == "plume":   # horsehair: dark-rooted strands, the crown catching light
                    g[y][x] = clamp(T, 3 - y + (1 if face in ("right", "top") else 0) - (x % 2 if face in ("right", "left") else 0)); continue
                v = 2 + (1 if 0 < x < w - 1 and x < w // 2 else 0) - (1 if y >= h - 3 else 0)
                if which == "back": v -= 1
                if part == "mantle" and x in (2, w - 3): v -= 1                          # two heavy folds
                if part == "tabard" and y == 0: v = 1                                      # the fold under the belt
                g[y][x] = clamp(T, v)
                if part == "sash": g[y][x] = clamp(T, 3 - y + (1 if x % 4 == 0 and y == 1 else 0))   # lit top edge, a stitched seam
                elif x in (0, w - 1) and part != "plume": g[y][x] = T[0]   # a dark selvedge
                if y == h - 2 and part in ("tabard", "mantle"): g[y][x] = trim   # the gold-thread hem
        if part == "tabard":   # a torn, uneven hem
            cut(g, [(0, h - 1), (4, h - 1), (5, h - 1), (5, h - 2), (1, h - 1)] if which == "front" else [(0, h - 1), (3, h - 1), (5, h - 1)])
        elif part == "mantle":
            cut(g, [(0, h - 1), (1, h - 1), (4, h - 1), (7, h - 1), (8, h - 1), (8, h - 2)])
        return g
    return paint

# ---- Gatepost Greaves: thick plated thighs, leather straps, scuffed knee guards ----
def thigh(r):
    def paint(face, w, h):
        g = iron(r, base=3)(face, w, h)
        if face in ("front", "back", "left", "right"):
            for x in range(w):
                g[2][x] = leath(r)[2 + LIT[face]]; g[4][x] = leath(r)[2 + LIT[face]]   # straps
            if face == "front": g[2][w // 2] = metal(r)[5]; g[4][w // 2] = metal(r)[5]
        return g
    return paint

KNEE_C = {"front": "..4..\n.3.k."}
KNEE_L = {"front": "..5..\n.3fk."}

def legs_bones():
    B = []
    for side in ("right", "left"):
        B.append(Bone(f"greave_{side}", f"{side}_leg", tag="legs", boxes=[
            box((-2.5, -0.5, -2.5), (5, 6, 5), thigh),
            box((-2.5, 5, -3), (5, 2, 1), lambda r: stamped(iron(r, base=4, seam=False), r, KNEE_L if r == LEGEND else KNEE_C),
                lambda r: ember_of(r, KNEE_L))]))
        B.append(Bone(f"greave_{side}_l", f"{side}_leg", tag="legs", stages=[LEGEND], boxes=[
            box((-3, 4.5, -4), (6, 3, 1), lambda r: stamped(iron(r, base=4), r, {"front": "......\n..ff..\n......"}),
                lambda r: ember_of(r, {"front": "......\n..ff..\n......"})),
            box((-2, 3.5, -4.5), (4, 1, 1), lambda r: iron(r, base=5, seam=False))]))
    return B

# ---- Cairn-Treads: sabatons like stacked stones, mud-dark ----
def stone(r, base, mud_rows=1):
    M = metal(r)
    def paint(face, w, h):
        g = iron(r, base=base, spec=False)(face, w, h)
        if face in ("front", "back", "left", "right"):
            for y in range(max(0, h - mud_rows), h):
                for x in range(w):
                    if (x + y) % 3: g[y][x] = MUD[1 + (1 if (x * 3 + y) % 4 == 0 else 0) + (LIT[face] > 0)]
        if face == "bottom":
            for y in range(h):
                for x in range(w): g[y][x] = MUD[0]
        return g
    return paint

def boots_bones():
    B = []
    for side in ("right", "left"):
        B.append(Bone(f"tread_{side}", f"{side}_leg", tag="boots", boxes=[
            box((-2.5, 7.5, -2.5), (5, 2, 5), lambda r: stone(r, 3, 0)),
            box((-3, 9.5, -3), (6, 2, 6), lambda r: stone(r, 3, 1)),
            box((-3, 11.5, -4), (6, 1, 7), lambda r: stone(r, 2, 1))]))
        B.append(Bone(f"tread_{side}_l", f"{side}_leg", tag="boots", stages=[LEGEND], boxes=[
            box((-2, 6.5, -3.5), (4, 2, 1), lambda r: stamped(iron(r, base=4, seam=False), r, {"front": ".f..\n...."}),
                lambda r: ember_of(r, {"front": ".f..\n...."})),
            box((-1.5, 10.5, -4.5), (3, 1, 2), lambda r: iron(r, base=4, seam=False))]))
    return B

def pieces():
    return [GearPiece("bruiser_helm", "helm", helm_bones, cloth_painter=tabard, cloth=(32, 16), glow=glow_cb),
            GearPiece("bruiser_chest", "chest", chest_bones, cloth_painter=tabard, skin=(128, 64), cloth=(64, 32), glow=glow_cb),
            GearPiece("bruiser_legs", "legs", legs_bones, glow=glow_cb),
            GearPiece("bruiser_boots", "boots", boots_bones, glow=glow_cb)]
