#!/usr/bin/env python3
"""Lord Marshal Aldric (Gilded house, troop id gl_marshal), hand-authored at one texel per model pixel.

One man, one stage: the Order's commander in blackened plate edged in gilt, a white surcoat with the team pale down its
middle, a fur collar, broad three-lame pauldrons, an open helm crowned with a gilt laurel over a grey-bearded, scarred
face and the heater shield with the Order's sun on the pale. No flag on his back (Kort, 2026-10-29).

  python build_gl_marshal.py preview <dir> [side] | contact <png> [side] [all] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "knight"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
import build_knight as K

SKIN = (128, 128)
CLOTH = (128, 64)
BLACK = ramp("0f0e13", "1b1a21", "28262f", "36343e", "4a4853", "625f6c", "86838f")   # blackened steel, cold highlights
FUR = ramp("1e1712", "33271d", "4b3a2b", "65513d", "806a52")
WOOD = ramp("2a1a10", "45301e", "63462c", "80603c")

def trim(R=GOLD, a=4, b=3):
    return lambda f, w, h: [[(R[a] if x % 2 else R[b]) for x in range(w)]]

# ---- the face: Aldric, grey and scarred, his beard spilling over the gorget ----
FACE = """
iiiiiiii
iHHHHHHi
HsSSSSsH
sbbSsbbs
SweSSDwS
sSSddSDs
iHHmmHHi
iiHiiHii"""

def face(face_, w, h):
    S, Hh = SKINS["pale"], HAIRS["grey"]
    p = {"S": S[4], "s": S[3], "d": S[2], "D": S[1], "H": Hh[1], "i": Hh[2], "w": hx("f2efe8"), "e": hx("4a5a6a"), "m": S[1], "b": Hh[1]}
    if face_ == "front":
        g = grid(FACE, p); g[4][2] = p["e"]; return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face_ == "top": g[y][x] = Hh[2] if (x + y) % 3 == 0 else Hh[1]; continue
            if face_ == "bottom": g[y][x] = S[1]; continue
            if face_ == "back": g[y][x] = Hh[1] if (x + y) % 3 else Hh[0]; continue
            toward_back = (w - 1 - x) if face_ == "right" else x
            if y < 2 or toward_back < 3: g[y][x] = Hh[1] if (x + y) % 3 else Hh[2]
            elif y >= 5: g[y][x] = Hh[2] if (x + y) % 2 else Hh[1]
            else: g[y][x] = S[3] if face_ == "right" else S[2]
            if toward_back == 3 and y in (3, 4): g[y][x] = S[1] if y == 4 else S[2]
    return g

def beard(face_, w, h):
    Hh = HAIRS["grey"]
    g = blank(w, h)
    if face_ == "top": return g
    for y in range(h):
        for x in range(w):
            g[y][x] = Hh[3] if (x + y) % 3 == 0 else Hh[2] if face_ in ("front", "right") else Hh[1]
    if face_ == "front":                      # forked at the point
        g[h - 1][0] = g[h - 1][w - 1] = None; g[h - 1][w // 2 - 1] = Hh[1]; g[h - 1][w // 2] = None
    return g

# ---- the open helm: a blackened skull with a gilt laurel round it, cheek plates and a neck guard ----
def skull(face_, w, h):
    if face_ == "bottom": return blank(w, h)
    g = plate(R=BLACK, base=4, spec=False)(face_, w, h)
    if face_ == "top":
        for y in range(h): g[y][w // 2] = GOLD[4] if y % 2 else GOLD[3]       # the gilt ridge
        return g
    leaf = {0: GOLD[2], 1: GOLD[4], 2: GOLD[5], 3: GOLD[3]}
    for x in range(w):                                                           # the laurel: leaves pointing to the front
        k = x if face_ in ("front", "left") else w - 1 - x
        g[h - 2][x] = leaf[k % 4]; g[h - 3][x] = GOLD[3] if k % 4 == 1 else g[h - 3][x]
    g[h - 1] = [BLACK[1]] * w
    if face_ == "front":                                                         # the laurel's two ends meet at a gilt boss
        g[h - 2][w // 2 - 1] = GOLD[5]; g[h - 2][w // 2] = GOLD[5]; g[h - 3][w // 2 - 1] = GOLD[4]; g[h - 3][w // 2] = GOLD[4]
    return g

def cheek(face_, w, h):
    if face_ in ("top", "bottom"): return plate(R=BLACK, base=3, spec=False)(face_, w, h)
    g = plate(R=BLACK, base=3, spec=False)(face_, w, h)
    for y in range(h): g[y][0] = g[y][w - 1] = GOLD[2] if y % 2 else GOLD[3]
    g[h - 1] = [GOLD[3]] * w
    return g

def neckguard(face_, w, h):
    g = plate(R=BLACK, base=3, spec=False)(face_, w, h)
    if face_ not in ("top", "bottom"): g[h - 1] = [GOLD[3] if x % 2 else GOLD[2] for x in range(w)]
    return g

# ---- the harness: blackened plate edged in gilt ----
def cuirass(face_, w, h):
    return rows([(0, plate(R=BLACK, base=2)), (7, plate(R=BLACK, base=2, spec=False)), (10, mail(base=1))])(face_, w, h)

def arm(face_, w, h):
    return rows([(0, plate(R=BLACK, base=2)), (6, plate(R=BLACK, base=2)), (10, plate(R=BLACK, base=1, spec=False))])(face_, w, h)

def leg(face_, w, h):
    return rows([(0, plate(R=BLACK, base=2)), (6, plate(R=BLACK, base=2)), (9, plate(R=BLACK, base=1))])(face_, w, h)

def pauldron(part):
    def paint(face_, w, h):
        if face_ == "bottom": return plate(R=BLACK, base=1, spec=False, rim=False)(face_, w, h)
        if face_ == "top":
            g = blank(w, h)
            for y in range(h):
                for x in range(w):
                    dx, dy = abs(x - (w - 1) / 2), abs(y - (h - 1) / 2)
                    g[y][x] = BLACK[5] if dx + dy < 1 else BLACK[4] if dx + dy < 2.5 else BLACK[3]
                    if part != "dome": g[y][x] = BLACK[2]
            return g
        base = max(1, min(5, {"dome": 3, "lame": 2}[part] + LIT.get(face_, 0)))
        g = [[BLACK[base]] * w for _ in range(h)]
        g[0] = [BLACK[min(6, base + 2)]] * w
        g[h - 1] = [BLACK[0]] * w
        if part == "dome": g[h - 1] = [GOLD[4] if x % 2 == 0 else GOLD[3] for x in range(w)]
        return g
    return paint

def plain(R, base):
    return plate(R=R, base=base, spec=False, seam=False)

def gorget(face_, w, h):
    g = plate(R=BLACK, base=3, spec=False)(face_, w, h)
    if face_ not in ("top", "bottom"): g[0] = [GOLD[3]] * w
    return g

def stud(face_, w, h):
    g = plate(R=BLACK, base=3, spec=False, seam=False)(face_, w, h)
    if face_ in ("front", "right", "left"): g[h // 2][w // 2] = GOLD[4]
    return g

def gilt_boot(face_, w, h):
    g = plate(R=BLACK, base=2)(face_, w, h)
    if face_ not in ("top", "bottom"): g[0] = [GOLD[3]] * w
    return g

def fur(face_, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(FUR, 2 + LIT[face_] + (1 if (x * 3 + y) % 4 == 0 else 0) - (1 if (x + y * 2) % 5 == 0 else 0))
    if face_ not in ("top", "bottom"):
        for x in range(w):
            if x % 2: g[h - 1][x] = None
    return g

def belt(face_, w, h):
    g = leather(base=1)(face_, w, h)
    if face_ == "front":
        for y, row in enumerate(("..fe", "..dc")):
            for x, c in enumerate(row):
                if c != ".": g[y][2 + x] = pal()[c]
    for x in range(0, w, 3): g[0][x] = GOLD[4] if g[0][x] == g[0][x] else g[0][x]
    return g

# ---- the cloth: surcoat with the pale, its skirts, the shield face ----
WHITE = ramp("6a6458", "9a9484", "c4bfaf", "e2ddcd", "f4f1e6")

def surcoat(T, M, back):
    def paint(face_, w, h):
        g = blank(w, h)
        if face_ not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                pale = 2 <= x <= 3
                v = 3 + LIT[face_] + (-1 if y >= 8 and x in (0, w - 1) else 0)
                g[y][x] = clamp(T, v + (1 if x == 2 else 0)) if pale else clamp(WHITE, v + (1 if x == 1 else 0))
        g[h - 1] = [M[1] if x % 2 else M[2] for x in range(w)]
        if not back:                                    # a small gilt sun on the pale over the heart
            for (x, y), c in {(2, 3): M[3], (3, 3): M[2], (2, 4): M[2], (3, 4): M[1]}.items(): g[y][x] = c
        return g
    return paint

def skirt(T, M, inner_x):
    def paint(face_, w, h):
        g = blank(w, h)
        if face_ not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                pale = x == inner_x
                v = 3 + LIT[face_] + (-1 if y >= h - 2 else 0)
                g[y][x] = clamp(T, v) if pale else clamp(WHITE, v + (-1 if x == 3 - inner_x else 0))
        g[h - 1] = [M[1] if x % 2 else M[2] for x in range(w)]
        return g
    return paint

def shield_face(T, M):
    def paint(face_, w, h):
        g = blank(w, h)
        if face_ != "front": return g
        for y in range(h):
            for x in range(w):
                if not in_shield(x, y): continue
                pale = 4 <= x <= 7
                v = 4 if y < 5 and x < 5 and x + y < 6 else 2 if y >= 13 else 3
                if not in_shield(x, y, 2): v = 1
                g[y][x] = clamp(WHITE, v) if pale and in_shield(x, y, 1) else clamp(T, v)
        sun = grid("""
..e..e..
...ff...
e.fggf.e
.fgggf..
.fgggf..
e.fggf.e
...ff...
..e..e..""", {"e": M[1], "f": M[2], "g": M[3]})
        over(g, sun, 2, 4)
        return g
    return paint

def gilt(face_, w, h): return plain(GOLD, 4)(face_, w, h)

# ---- the veteran trim (Mastery II): gilt filigree over the blackened plate ----
def vet_band(rows_at, cols=None):
    """Gilt on the given rows of every side face (the alternate texels a shade darker), `cols` limiting the front."""
    def paint(face_, w, h):
        g = blank(w, h)
        if face_ in ("top", "bottom"): return g
        for y in rows_at:
            if y < h:
                for x in range(w): g[y][x] = GOLD[4] if x % 2 else GOLD[3]
        if cols and face_ in ("front", "back"):
            for y in range(h):
                for x in cols: g[y][x] = GOLD[3] if y % 3 else GOLD[5]
        return g
    return paint

def veteran(bone, box):
    n = bone.name
    if n == "body": return vet_band((7,), cols=(0, 7))                     # the cuirass's waist line and its flanks
    if n.endswith(("_arm", "_forearm")): return vet_band((5,))             # a band above the elbow
    if n.endswith(("_leg", "_shin")): return vet_band((5,))                # a band at the knee
    if n == "helm" and box.size == [1, 5, 6]: return vet_band((1, 3))      # the cheek plates, banded
    return None

# ---- assembling ----
def bones():
    B = []
    B.append(Bone("root", ""))
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), cuirass)]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face), Box((-3, 0, -4.4), (6, 2, 1), beard)]))
    B += arms(arm)
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B += legs(leg)
    # the open helm
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-4.5, -9, -4.5), (9, 4, 9), skull), Box((-5, -5, -4), (1, 5, 6), cheek), Box((4, -5, -4), (1, 5, 6), cheek),
        Box((-4.5, -5, 3.5), (9, 4, 1), neckguard), Box((-0.5, -10, -3), (1, 1, 7), gilt)]))
    # torso
    B.append(Bone("gorget", "body", boxes=[Box((-4.5, -12.5, -2.5), (9, 2, 5), gorget)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), belt, dil=0.45)]))
    B.append(Bone("surcoat", "body", team=True, boxes=[
        Box((-3, -10, -2.3), (6, 10, 0), ("surcoat", False), sheet="cloth"), Box((-3, -10, 2.3), (6, 10, 0), ("surcoat", True), sheet="cloth")]))
    # the shield
    B.append(Bone("shield", "left_forearm", SHIELD_PIVOT, rot=SHIELD_ROT, tag="shield", boxes=
        [Box((x, y, -1.9), (w, h, 1), wood) for (x, y), (w, h) in SHIELD_BODY]
        + [Box((x, y, -2.5), (w, h, 2), shield_rim(3)) for (x, y), (w, h) in SHIELD_RIM]
        + [Box((-1, -3, -0.9), (2, 5, 3), leather(base=2))]))
    B.append(Bone("shield_face", "shield", team=True, tag="shield", boxes=[Box((-6, -10, -2.1), (12, 18, 0), "shield", sheet="cloth")]))
    # limbs
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 16 * c), boxes=[
            Box((-4, -1.5, -4), (8, 2, 8), pauldron("dome")),
            Box((-4 + 0.5 * c, 0.5, -3.5), (8, 2, 7), pauldron("lame"), dil=0.15),
            Box((-3.5 + 1.0 * c, 2.5, -3), (7, 2, 6), pauldron("lame"), dil=0.15)]))
        # the forearm's frame is the arm's less the elbow (c, 4, 0); the shin's is the leg's less the knee (0, 6, 0)
        B.append(Bone(f"{side}_couter", f"{side}_forearm", boxes=[Box((-2.6 if c < 0 else 1.6, -1, -1.5), (1, 3, 3), stud)]))
        B.append(Bone(f"{side}_cuff", f"{side}_forearm", boxes=[Box((-2.5, 1, -2.5), (5, 2, 5), lambda f, w, h: blank(w, h) if f in ("top", "bottom") else trim(a=5, b=4)(f, w, 1) + plain(BLACK, 4)(f, w, h - 1))]))
        B.append(Bone(f"{side}_knee", f"{side}_shin", boxes=[Box((-2.5, -1, -3), (5, 2, 2), stud)]))
        B.append(Bone(f"{side}_boot", f"{side}_shin", boxes=[Box((-2.5, 3, -3), (5, 3, 5), gilt_boot)]))
        B.append(Bone(f"{side}_toe", f"{side}_shin", boxes=[Box((-1.5, 5, -4), (3, 1, 1), plain(BLACK, 4))]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, boxes=[
            Box((-2, 0, -2.45), (4, 7, 0), ("skirt", inner), sheet="cloth"), Box((-2, 0, 2.45), (4, 7, 0), ("skirt", 3 - inner), sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "shield": return shield_face(T, M)
    if spec[0] == "surcoat": return surcoat(T, M, spec[1])
    return skirt(T, M, spec[1])

MARSHAL = Troop("gl_marshal", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "marshal_sword", "hand": "right", "idleArms": "hang", "portrait": "tall 4"},
                tiers=(1,), hide=lambda bn: bn.name.startswith("shield"), veteran=veteran)
def build(): return MARSHAL.build()
def outputs(): return MARSHAL.outputs()

if __name__ == "__main__":
    MARSHAL.main(sys.argv)
