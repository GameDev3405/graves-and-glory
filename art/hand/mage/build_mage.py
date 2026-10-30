#!/usr/bin/env python3
"""The Sunweaver (Gilded house, troop id mage), hand-authored at one texel per model pixel.

An old sun priest of the Order: a deep white cowl over a grey-bearded face, a white mantle on the shoulders pinned by
a gilt sunburst brooch that burns (the _e sheet), a white robe falling to the feet in three flared tiers each hemmed in
gilt, wide bell sleeves, a rope cincture, and over it all a stole in the team colour, the sun worked on its breast and
on its back. One armour tier; Mastery II gilds the robe's hems.

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
def cowl(face, w, h):
    g = cloth_v(3)(face, w, h)
    if face == "front":                          # the deep opening: shadow round the face, the face set back in it
        for y in range(h):
            for x in range(w):
                if y >= 2 and 1 <= x <= w - 2: g[y][x] = None
        for x in range(1, w - 1): g[1][x] = WHITE[5] if x % 2 else WHITE[4]   # the turned lip catches the light
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face == "top":
        for x in range(w): g[h // 2][x] = WHITE[2]   # the seam over the crown
    return g

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

def bell(face, w, h):
    g = cloth_v(4, fold=2)(face, w, h)
    if face == "top": return blank(w, h)
    if face == "bottom":
        g = [[WHITE[0]] * w for _ in range(h)]
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
        return g
    g[h - 1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
    g[h - 2] = [WHITE[2]] * w
    return g

def leg(face, w, h):
    return rows([(0, cloth_v(2)), (11, leather(base=1))])(face, w, h)

def robe_tier(face, w, h):
    """One tier of the robe's skirt: lit wool, darker toward its hem, the hem in gilt."""
    g = cloth_v(3)(face, w, h)
    if face == "top": return blank(w, h)
    if face == "bottom":
        g = [[WHITE[1]] * w for _ in range(h)]
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
        return g
    if h > 2: g[h - 2] = [clamp(WHITE, 2 + LIT[face])] * w
    g[h - 1] = [GOLD[3] if x % 2 else GOLD[2] for x in range(w)]
    return g

def cincture(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = ROPE[2 + ((x + y) % 2)] if face not in ("bottom",) else ROPE[0]
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
# The robe hangs rigid from the thigh to the shin's hem, so the knee may bend only a little before the shin would leave it:
# the shins carry a joint limit of 0..10 degrees ("limit" in the troop file, TroopModel.jointLimit), and the walk glides.
SHIN_LIMIT = (0, 10)

def bones():
    B = []
    B.append(Bone("root", ""))
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), robe_body)]))
    B.append(Bone("head", "body", (0, -12, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", variants=[k], boxes=[
            Box((-4, -8, -4), (8, 8, 8), sage_face(k)), Box((-3, -1, -4.4), (6, 4, 1), beard)]))
    B += arms(sleeve)
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B += legs(leg, limit=SHIN_LIMIT)
    # the bell sleeves
    B.append(Bone("right_bell", "right_forearm", boxes=[Box((-2.5, 0, -2.5), (5, 5, 5), bell)]))
    B.append(Bone("left_bell", "left_forearm", boxes=[Box((-2.5, 0, -2.5), (5, 5, 5), bell)]))
    # the cowl, deep, a little peak over the brow; the mantle on the shoulders
    B.append(Bone("cowl", "head", tag="helm", boxes=[
        Box((-5, -9, -5), (10, 10, 10), cowl)]))
    B.append(Bone("mantle", "body", boxes=[Box((-5, -12, -3), (10, 4, 6), mantle)]))
    B.append(Bone("brooch", "body", boxes=[Box((-2.5, -8, -3.4), (5, 5, 1), brooch)]))
    # the stole over the robe, the cincture and its cord
    B.append(Bone("vestment", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "stole", dil=0.3, sheet="cloth")]))
    B.append(Bone("cincture", "body", boxes=[Box((-4, -4, -2), (8, 1, 4), cincture, dil=0.5)]))
    B.append(Bone("cord", "body", boxes=[Box((2, -3.5, -2.8), (1, 6, 1), cord)]))
    # the robe in three flared tiers round each leg
    for side in ("right", "left"):
        B.append(Bone(f"{side}_robe", f"{side}_leg", boxes=[
            Box((-2, 0, -2), (4, 4, 4), robe_tier, dil=0.25), Box((-2, 4, -2), (4, 4, 4), robe_tier, dil=0.55),
            Box((-2, 8, -2), (4, 3, 4), robe_tier, dil=0.85)]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return stole(T, M)

MAGE = Troop("mage", SKIN, CLOTH, bones, cloth_painter,
             {"weapon": "sun_staff", "hand": "right", "idleArms": "hang", "portrait": "tall 6", "faces": {"count": 4, "card": 0}},
             tiers=(1,), veteran=veteran, glow=glow)
def build(): return MAGE.build()
def outputs(): return MAGE.outputs()

if __name__ == "__main__":
    MAGE.main(sys.argv)
