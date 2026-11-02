#!/usr/bin/env python3
"""The Ironclad Shieldbreaker (Ironclad house, troop id ic_breaker), hand-authored at one texel per model pixel.

A bull of a man in blackened iron: a closed sallet with an ember brow band and an ember horsehair plume, a fur collar
over lamellar-lapped plate, mail showing at the elbows and knees, a long team tabard cleft by a cloven shield, and
a bearded axe inlaid in ember. Tier I only (the Ironclad troops are single-stage in the first pass).

  python build_ic_breaker.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ironclad import *   # noqa: F401,F403
from handlib import Troop

SKIN = (128, 128)
CLOTH = (128, 64)
P = pal_ic()

# ---- the sallet ----
HELM_FRONT = fold("""
34555
23445
qrrss
1kkkk
33445
23445
23445
13344
12233""")

def helm_can():
    def paint(face, w, h):
        if face == "front": return grid(HELM_FRONT, P)
        if face == "bottom": return blank(w, h)
        g = lames([3, 2, 2, 2], base=3, seed="hc", wear=4, rivets=False)(face, w, h)
        if face in ("right", "left") and h >= 5:
            near = (w - 2, w - 1) if face == "right" else (0, 1)
            for x in near: g[3][x] = INK
            for x in range(w):
                g[2][x] = EMBER[2 if x % 2 else 1]                 # the brow band runs round the can
        if face == "back":
            for y in range(1, h - 1): g[y][w // 2] = IRON[2]        # the welded seam
            for x in range(w): g[2][x] = EMBER[2 if x % 2 else 1]
        return g
    return paint

def helm_crown():
    def paint(face, w, h):
        g = plate(R=IRON, base=4, spec=False, seam=False)(face, w, h)
        return scuff(g, ("crown", face), 2)
    return paint

def plume():
    return ember_plume("pl")

def gorget_fur():
    return fur(seed="gorget")

# ---- body ----
BREAST = fold("""
.....
.4444
.3555
.3454
.2343
.2232""")  # a hammered breast ridge laid over the lames

def body_painter():
    base = lames([3, 3, 3, 3], base=3, seed="bd", wear=7, rivets=True, ember_rows=())
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for y in range(1, 7):                                  # the breastplate's ridge and the cleave scar
                g[y][w // 2 - 1] = IRON[5] if y in (2, 3) else IRON[4]
                g[y][w // 2] = IRON[2]
            for x in range(w): g[h - 1][x] = EMBER[1 if x % 2 else 2]
        if face == "back":
            for y in range(h): g[y][w // 2] = IRON[2]              # the spine seam
        return g
    return paint

def belt():
    def paint(face, w, h):
        g = leather_ic(base=1)(face, w, h)
        if face == "front":
            for y, row in enumerate(("..tt", "..rs")):
                for x, c in enumerate(row):
                    if c != ".": g[y][2 + x] = P[c]
        return g
    return paint

def arm():
    return rows([(0, mail_ic(3)), (3, lames([3, 3, 2, 1], base=3, seed="arm", wear=3)), (10, leather_ic(base=2, stitch=True, ember_stitch=True))])

def leg():
    return rows([(0, mail_ic(2)), (4, lames([2, 2, 2, 2], base=2, seed="leg", wear=3, rivets=False)), (8, leather_ic(base=2))])

def pauldron_lame(i):
    def paint(face, w, h):
        g = lames([h], base=3 + (1 if i == 0 else 0), seed=("pl", i), wear=3, rivets=(w >= 6))(face, w, h)
        if i == 0 and face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = EMBER[2 if x % 2 else 3]
        return g
    return paint

def knee():
    return lames([2], base=4, seed="knee", wear=1, rivets=False)

def boot():
    return rows([(0, leather_ic(base=2)), (1, leather_ic(base=1, stitch=True)), (2, lames([1], base=2, rivets=False))])

def cuff():
    return rows([(0, lames([2], base=4, rivets=False)), (1, lames([1], base=2, rivets=False))])

# ---- the cloth: tabard, side flaps ----
CLEAVE = """
.LLLLLL.
LddddeeL
LdddeddL
LddeddmL
.LdeddL.
..LedL..
...LL..."""

def cleave_pal(M): return {"e": EMBER[3], "L": IRON[5], "m": IRON[3], "d": IRON[1]}

def tabard(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back") and y >= 7 and x in (1, 6): v -= 1
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face in ("right", "left"):
            for y in range(0, 7):
                for x in range(w): g[y][x] = None
            g[7] = [T[1]] * w
        if face in ("front", "back"):
            over(g, grid(CLEAVE, cleave_pal(M)), 0, 3 if face == "front" else 2)
            for x in range(w): g[h - 1][x] = EMBER[2 if x % 2 else 3]    # the ember bordure at the hem
            for y in range(h): g[y][0] = g[y][w - 1] = EMBER[1 if y % 2 else 2]
        return g
    return paint

def flap(T, M, inner_x):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y >= h - 3 else 0)
                g[y][x] = clamp(T, v)
        g[h - 1] = [EMBER[2 if x % 2 else 3] for x in range(w)]
        g[h - 2] = [EMBER[1]] * w
        return g
    return paint

def cape(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = (2 if face != "back" else 3) + (-1 if face in ("left", "right") else 0) + (-1 if x % 3 == 2 and y > 3 else 0)
                g[y][x] = clamp(T, v)
        if face == "back":
            g[h - 1] = [EMBER[1 if x % 2 else 2] for x in range(w)]
            over(g, grid(CLEAVE, cleave_pal(M)), 1, 3)
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0)))
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-5, -8, -5), (10, 9, 10), helm_can()), Box((-4, -10, -4), (8, 2, 8), helm_crown())]))
    B.append(Bone("helm_plume", "head", tag="helm", boxes=[
        Box((-1, -14, -4), (2, 4, 8), ember_plume("pl")),
        Box((-1, -12, 3.5), (2, 9, 2), ember_plume("tail", tail=True), dil=0.1)]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm())]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm())]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg())]))
    B.append(Bone("fur_collar", "body", boxes=[Box((-5.5, -12.5, -3), (11, 3, 6), gorget_fur(), dil=0.2)]))
    B.append(Bone("tabard", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "tabard", dil=0.3, sheet="cloth")]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), belt(), dil=0.45)]))
    B.append(Bone("cape", "body", (0, -12, 3.6), team=True, rot=(8, 0, 0), boxes=[Box((-5, 0, 0), (10, 14, 1), "cape", sheet="cloth")]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 14 * c), boxes=[
            Box((-4, -1.5, -4), (8, 2, 8), pauldron_lame(0)),
            Box((-4 + 0.5 * c, 0.5, -3.5), (8, 2, 7), pauldron_lame(1), dil=0.15),
            Box((-3.5 + 1.0 * c, 2.5, -3), (7, 2, 6), pauldron_lame(2), dil=0.15),
            Box((-3 + 1.5 * c, 4.5, -2.5), (6, 1, 5), pauldron_lame(3), dil=0.1)]))
        B.append(Bone(f"{side}_cuff", f"{side}_arm", boxes=[Box((c - 2.5, 5, -2.5), (5, 2, 5), cuff())]))
        B.append(Bone(f"{side}_knee", f"{side}_leg", boxes=[Box((-2.5, 6, -3), (5, 2, 2), knee())]))
        B.append(Bone(f"{side}_boot", f"{side}_leg", boxes=[Box((-2.5, 9, -3), (5, 3, 5), boot())]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, boxes=[
            Box((-2, 0, -2.45), (4, 8, 0), ("flap", inner), sheet="cloth"),
            Box((-2, 0, 2.45), (4, 8, 0), ("flap", 3 - inner), sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard": return tabard(T, M)
    if spec == "cape": return cape(T, M)
    return flap(T, M, spec[1])

IC_BREAKER = Troop("ic_breaker", SKIN, CLOTH, bones, cloth_painter,
                   {"weapon": "ic_axe", "hand": "right", "idleArms": "hang", "portrait": "tall 4"}, tiers=(1,))
def build(): return IC_BREAKER.build()
def outputs(): return IC_BREAKER.outputs()

if __name__ == "__main__":
    IC_BREAKER.main(sys.argv)
