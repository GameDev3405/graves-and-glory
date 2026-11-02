#!/usr/bin/env python3
"""The Sunweaver (Gilded house, troop id mage), hand-authored at one texel per model pixel.

An old sun priest of the Order: a deep white cowl over a grey-bearded face, a white mantle on the shoulders pinned by
a gilt sunburst brooch that burns (the _e sheet), a white robe falling to the feet in three flared tiers each hemmed in
gilt, wide bell sleeves, a rope cincture, and over it all a stole in the team colour, the sun worked on its breast and
on its back. Mastery II gilds the robe's hems.

Tier I is the priest in wool and rope. Tier II (sun cleric) adds steel spaulders edged in gilt, a short white shoulder
cape worked with the sun, a gilt lip on the cowl, deep gilt cuffs on the bell sleeves and a leather belt set with gilt
plaques. Tier III (hierophant) spends the gilt: the spaulders gilded, a sunburst crown of rays rising from a circlet
over the cowl's brow, gilt filigree on every robe tier and a gilt border round the shoulder cape.

No flag rides on his back (Kort, 2026-10-29).

  python build_mage.py preview <dir> [side] | contact <png> [side] [all] [vet] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "knight"))
sys.path.insert(0, os.path.join(HERE, "..", "archer"))
from handlib import *   # noqa: F401,F403
from handlib import Troop, FACE_ART, SKINS, HAIRS
import build_archer as A

SKIN = (128, 80)
CLOTH = (128, 64)
WHITE = ramp("5e5a52", "8e897c", "b9b4a4", "d9d5c6", "efece0", "fbf9f1")   # the robe's white, shadowed warm
ROPE = ramp("6a5a3a", "9c8858", "c8b57e", "e6d8a6")
SAGE_HAIR = ramp("6a6a70", "9c9ca4", "c8c8ce", "e8e8ea")

def cloth_v(base=3, fold=3):
    """White wool: lit by face, a fold every `fold` columns."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face not in ("top", "bottom") and x % fold == fold - 1: v -= 1
                g[y][x] = clamp(WHITE, v)
        return g
    return paint

# ---- the face: four old men of the Order, every head gone grey, a long beard on the chin ----
def sage_face(k):
    skin_name, _, front = FACE_ART[k]
    S, Hh = SKINS[skin_name], SAGE_HAIR
    eye = {"fair": "3c5a78", "olive": "3a2a1c", "brown": "2a1a10", "pale": "5a6a5a"}[skin_name]
    p = {"S": S[4], "s": S[3], "d": S[2], "D": S[1], "h": Hh[0], "H": Hh[1], "i": Hh[2], "w": hx("f2efe8"),
         "e": hx(eye), "m": S[1], "b": Hh[2]}
    rows_ = [r for r in front.strip().splitlines()]
    rows_[6] = "dHHmmHHd"; rows_[7] = "HHiiiiHH"                 # moustache, and the beard's root along the jaw
    def paint(face, w, h):
        if face == "front": return grid("\n".join(rows_), p)
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face == "top": g[y][x] = Hh[2] if (x + y) % 3 == 0 else Hh[1]; continue
                if face == "bottom": g[y][x] = S[1]; continue
                if face == "back": g[y][x] = Hh[1] if (x + y) % 3 else Hh[0]; continue
                toward_back = (w - 1 - x) if face == "right" else x
                if y < 2 or toward_back < 3 or y >= 6: g[y][x] = Hh[1] if (x + y) % 3 else Hh[2]
                else: g[y][x] = S[3] if face == "right" else S[2]
        return g
    return paint

def beard(face, w, h):
    """The beard falls from the chin over the breast, combed into strands, its tip forked."""
    g = blank(w, h)
    H = SAGE_HAIR
    for y in range(h):
        for x in range(w):
            g[y][x] = H[2 + (1 if face == "front" and x % 2 == 0 else 0) - (1 if y == h - 1 else 0)] if face != "bottom" else H[0]
    if face == "front":
        g[h - 1][0] = g[h - 1][w - 1] = None
        g[h - 1][w // 2 - 1 + (w % 2)] = None if w > 4 else g[h - 1][w // 2]
    return g

# ---- the cowl and mantle ----
def cowl(tier):
  def paint(face, w, h):
    g = cloth_v(3)(face, w, h)
    if face == "front":                          # the deep opening: shadow round the face, the face set back in it
        for y in range(h):
            for x in range(w):
                if y >= 2 and 1 <= x <= w - 2: g[y][x] = None
        for x in range(1, w - 1): g[1][x] = WHITE[5] if x % 2 else WHITE[4]   # the turned lip catches the light
        if tier >= 2:                                                             # the lip edged in gilt
            for x in range(1, w - 1): g[1][x] = GOLD[4] if x % 2 else GOLD[3]
            for y in range(2, h): g[y][0] = g[y][w - 1] = GOLD[3] if y % 2 else GOLD[2]
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face == "top":
        for x in range(w): g[h // 2][x] = (GOLD[3] if x % 2 else GOLD[2]) if tier == 3 else WHITE[2]   # the seam over the crown
    return g
  return paint

def cowl_shadow(face, w, h):
    """A sleeve of shadow inside the cowl's mouth, so the face sits deep."""
    g = blank(w, h)
    if face in ("right", "left", "top"):
        for y in range(h):
            for x in range(w): g[y][x] = WHITE[0] if (x + y) % 2 else hx("3e3a34")
    return g

def peak(face, w, h): return cloth_v(4)(face, w, h)

def mantle(face, w, h):
    g = cloth_v(3, fold=2)(face, w, h)
    if face == "bottom": return blank(w, h)
    if face == "top":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
        return g
    g[h - 1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]          # the gilt hem
    return g

def brooch(face, w, h):
    if face == "front":
        return grid("""
g.G.g
.GHG.
GHCHG
.GHG.
g.G.g""", {"g": GOLD[3], "G": GOLD[4], "H": GOLD[5], "C": hx("fff8dc")})
    return [[GOLD[2]] * w for _ in range(h)]

def brooch_lit(face, w, h):
    g = blank(w, h)
    if face == "front":
        for (x, y) in ((2, 2), (1, 2), (3, 2), (2, 1), (2, 3)): g[y][x] = hx("ffe9a0")
        g[2][2] = hx("fff8dc")
    return g

# ---- robe, sleeves, legs ----
def robe_body(face, w, h): return cloth_v(3)(face, w, h)

def sleeve(face, w, h):
    return rows([(0, cloth_v(3)), (9, rows([(0, cloth_v(2))])), (10, hand)])(face, w, h)

def hand(face, w, h):
    S = SKINS["fair"]
    return [[S[3] if face in ("front", "right") else S[2]] * w for _ in range(h)]

def bell(tier):
  def paint(face, w, h):
    g = cloth_v(4, fold=2)(face, w, h)
    if face == "top": return blank(w, h)
    if face == "bottom":
        g = [[WHITE[0]] * w for _ in range(h)]
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
        return g
    g[h - 1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
    g[h - 2] = [WHITE[2]] * w if tier == 1 else [GOLD[2] if x % 2 else GOLD[3] for x in range(w)]   # a deep gilt cuff
    if tier == 3: g[h - 3] = [GOLD[4] if x % 3 == 1 else clamp(WHITE, 3 + LIT[face]) for x in range(w)]
    return g
  return paint

def leg(face, w, h):
    return rows([(0, cloth_v(2)), (11, leather(base=1))])(face, w, h)

def robe_tier(tier):
  def paint(face, w, h):
    """One tier of the robe's skirt: lit wool, darker toward its hem, the hem in gilt; at III a gilt scroll above it."""
    g = cloth_v(3)(face, w, h)
    if face == "top": return blank(w, h)
    if face == "bottom":
        g = [[WHITE[1]] * w for _ in range(h)]
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
        return g
    if h > 2: g[h - 2] = [clamp(WHITE, 2 + LIT[face])] * w
    if tier == 3 and h > 2:
        g[h - 2] = [GOLD[3] if x % 3 == 1 else clamp(WHITE, 2 + LIT[face]) for x in range(w)]
    g[h - 1] = [GOLD[3] if x % 2 else GOLD[2] for x in range(w)]
    return g
  return paint

def cincture(tier):
  def paint(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = ROPE[2 + ((x + y) % 2)] if face not in ("bottom",) else ROPE[0]
    if tier >= 2 and face not in ("top", "bottom"):                          # a leather belt set with gilt plaques
        g = [[(GOLD[4] if tier == 3 else GOLD[3]) if x % 3 == 1 else LEATH[2 + (face == "front")] for x in range(w)] for _ in range(h)]
    return g
  return paint

# ---- tiers II and III: the spaulders, the shoulder cape and the sunburst crown ----
def spaulder(tier, part):
    """A small domed shoulder plate over the mantle: steel edged in gilt at II, gilt over it all at III."""
    R = GOLD if tier == 3 else STEEL
    def paint(face, w, h):
        if face == "bottom": return blank(w, h)
        if face == "top":
            g = blank(w, h)
            for y in range(h):
                for x in range(w):
                    d = abs(x - (w - 1) / 2) + abs(y - (h - 1) / 2)
                    g[y][x] = R[5] if d < 1.2 else R[4] if d < 3 else R[3]
            return g
        base = max(1, min(5, (3 if part == "dome" else 2) + LIT.get(face, 0)))
        g = [[R[base]] * w for _ in range(h)]
        g[0] = [R[min(6, base + 2)]] * w
        g[h - 1] = [GOLD[4] if x % 2 == 0 else GOLD[3] for x in range(w)] if part == "dome" else [R[1]] * w
        return g
    return paint

SUN8 = """
..e..e..
...ff...
e.fggf.e
.fgggf..
.fgggf..
e.fggf.e
...ff...
..e..e.."""

def shoulder_cape(tier):
    """A short white cape from the shoulders to the small of the back, the sun worked on it in gilt."""
    sun = grid(SUN8, {"e": GOLD[3], "f": GOLD[4], "g": GOLD[5]})
    def paint(face, w, h):
        if face in ("top", "front"): return blank(w, h)
        if face == "bottom": return [[WHITE[1]] * w for _ in range(h)]
        g = cloth_v(3)(face, w, h)
        if face == "back": over(g, sun, (w - 8) // 2, 1)
        g[h - 1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
        if tier == 3:
            if face == "back":
                for y in range(h): g[y][0] = g[y][w - 1] = GOLD[3] if y % 2 else GOLD[4]
            g[h - 2] = [GOLD[2] if x % 2 else g[h - 2][x] for x in range(w)]
        return g
    return paint

def gilt_ray(face, w, h):
    """One ray of the sunburst crown: bright on its lit faces, a shade down on the shadowed, a pale tip."""
    v = {"top": 5, "front": 4, "right": 4, "left": 3, "back": 3, "bottom": 2}[face]
    g = [[GOLD[v]] * w for _ in range(h)]
    if face in ("front", "right", "left", "back") and h > 1: g[0] = [GOLD[min(6, v + 1)]] * w
    return g

def circlet(face, w, h):
    if face in ("top", "bottom"): return blank(w, h)
    g = [[GOLD[3 + LIT[face]]] * w for _ in range(h)]
    if face == "front":
        g[0] = [GOLD[5] if x % 2 else GOLD[4] for x in range(w)]
        g[0][w // 2 - 1] = g[0][w // 2] = GOLD[6]                             # the sun at the brow
    return g

def cord(face, w, h):
    g = [[ROPE[2 if (y % 2) else 1]] * w for y in range(h)]
    g[h - 1] = [GOLD[4]] * w                                                   # a gilt aglet
    return g

# ---- the team cloth: the stole ----
def stole(T, M):
    """Over the shoulders and down the breast and back in the team colour, edged in the emblem metal, the sun on it."""
    sun = grid(A.SUN, {"m": M[1], "L": M[3]}) if M is not STEEL[0:4] else grid(A.SUN, {"m": GOLD[3], "L": GOLD[5]})
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        # the front: the stole's two ends falling from the shoulders; the back: a broad yoke with the sun on it
        cols = (1, 2, w - 3, w - 2) if face == "front" else tuple(range(1, w - 1))
        for y in range(h):
            for x in range(w):
                if face in ("front", "back") and x in cols:
                    g[y][x] = clamp(T, 3 + LIT[face] + (1 if x in (1, w - 3) else 0))
                if face == "top" and (x in (1, 2) or x in (w - 3, w - 2)): g[y][x] = clamp(T, 4)
        if face == "front":
            for x in cols: g[h - 1][x] = M[2]; g[h - 2][x] = M[1]
            for x in (1, w - 2): g[8][x] = g[9][x + (1 if x == 1 else -1)] = M[3]   # a worked cross on each end
        if face == "back":
            over(g, sun, (w - 5) // 2, 2)
            g[h - 1] = [None] + [M[2] if x % 2 else M[1] for x in range(1, w - 1)] + [None]
        return g
    return paint

# ---- the Mastery II trim: the robe's hems in bright gilt ----
def veteran(bone, box):
    if bone.name.endswith("_robe"):
        def paint(face, w, h):
            g = blank(w, h)
            if face in ("top", "bottom"): return g
            g[h - 1] = [GOLD[5] if x % 2 else GOLD[4] for x in range(w)]
            return g
        return paint
    return None

def glow(bone, box):
    return brooch_lit if bone.name == "brooch" else None

# ---- assembling ----
def bones():
    B = []
    B.append(Bone("root", ""))
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), robe_body)]))
    B.append(Bone("head", "body", (0, -12, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", variants=[k], boxes=[
            Box((-4, -8, -4), (8, 8, 8), sage_face(k)), Box((-3, -1, -4.4), (6, 4, 1), beard)]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), sleeve)]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), sleeve)]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg)]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg)]))
    # the bell sleeves
    B.append(Bone("right_bell", "right_arm", boxes=[Box((-3.5, 4, -2.5), (5, 5, 5), per_tier(bell))]))
    B.append(Bone("left_bell", "left_arm", boxes=[Box((-1.5, 4, -2.5), (5, 5, 5), per_tier(bell))]))
    # the cowl, deep, a little peak over the brow; the mantle on the shoulders
    B.append(Bone("cowl", "head", tag="helm", boxes=[
        Box((-5, -9, -5), (10, 10, 10), per_tier(cowl))]))
    # tier III: the sunburst crown, a gilt circlet round the cowl's crown with five rays rising from its front
    B.append(Bone("sun_crown", "head", tag="helm", stages=[3], boxes=[
        Box((-5, -8, -5), (10, 1, 10), circlet, dil=0.3),
        Box((-0.5, -13, -5.4), (1, 5, 1), gilt_ray), Box((-2.5, -11, -5.4), (1, 3, 1), gilt_ray),
        Box((1.5, -11, -5.4), (1, 3, 1), gilt_ray), Box((-4.5, -10, -5.2), (1, 2, 1), gilt_ray),
        Box((3.5, -10, -5.2), (1, 2, 1), gilt_ray)]))
    B.append(Bone("mantle", "body", boxes=[Box((-5, -12, -3), (10, 4, 6), mantle)]))
    B.append(Bone("brooch", "body", boxes=[Box((-2.5, -8, -3.4), (5, 5, 1), brooch)]))
    # the stole over the robe, the cincture and its cord
    B.append(Bone("vestment", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "stole", dil=0.3, sheet="cloth")]))
    B.append(Bone("cincture", "body", boxes=[Box((-4, -4, -2), (8, 1, 4), per_tier(cincture), dil=0.5)]))
    # tiers II and III: the shoulder cape behind, the spaulders over the mantle
    B.append(Bone("shoulder_cape", "body", (0, -12, 2.6), stages=[2, 3], rot=(4, 0, 0), boxes=[
        Box((-5, 0, 0), (10, 10, 1), per_tier(shoulder_cape, (2, 3)))]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_spaulder", f"{side}_arm", (c, -2.5, 0), stages=[2, 3], rot=(0, 0, 14 * c), boxes=[
            Box((-3, -1.5, -3), (6, 2, 6), per_tier(lambda t: spaulder(t, "dome"), (2, 3))),
            Box((-3 + 0.5 * c, 0.5, -2.5), (6, 1, 5), per_tier(lambda t: spaulder(t, "lame"), (2, 3)), dil=0.12)]))
    B.append(Bone("cord", "body", boxes=[Box((2, -3.5, -2.8), (1, 6, 1), cord)]))
    # the robe in three flared tiers round each leg
    for side in ("right", "left"):
        B.append(Bone(f"{side}_robe", f"{side}_leg", boxes=[
            Box((-2, 0, -2), (4, 4, 4), per_tier(robe_tier), dil=0.25), Box((-2, 4, -2), (4, 4, 4), per_tier(robe_tier), dil=0.55),
            Box((-2, 8, -2), (4, 3, 4), per_tier(robe_tier), dil=0.85)]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return stole(T, M)

MAGE = Troop("mage", SKIN, CLOTH, bones, cloth_painter,
             {"weapon": "sun_staff", "hand": "right", "idleArms": "hang", "portrait": "tall 6", "faces": {"count": 4, "card": 0}},
             veteran=veteran, glow=glow)
def build(): return MAGE.build()
def outputs(): return MAGE.outputs()

if __name__ == "__main__":
    MAGE.main(sys.argv)
