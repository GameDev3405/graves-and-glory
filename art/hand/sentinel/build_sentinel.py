#!/usr/bin/env python3
"""The Templar (Gilded house, troop id sentinel), hand-authored at one texel per model pixel.

A heavier man of the Order than the knight: a stepped great helm with a cross of slit and bar, a long team tabard
with the cross front and back that falls to the knee, plate over dark padded sleeves, broad pauldrons, a heater shield
with the cross. Tier I is the initiate in mail and plate; II the full harness with a comb, a visor snout and a cape;
III gilds it all, crowns the helm with a gilt cross over team mantling and borders the cloth in filigree.

The plate pieces it shares with the knight (pauldrons, couters, cuffs, knees, sabatons) come from build_knight, so the
Order's harness reads as one smithy.

  python build_sentinel.py preview <dir> [side] | contact <png> [side] [all] | shield <png> [side] | install
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
DARK = ramp("121118", "1e1c25", "2b2833", "3a3644", "4d4858", "625c70")   # the padded sleeves and hose, blue-black wool

# ---- the great helm ----
CAN = {1: """
4555555554
3444554433
1kkk54kkk1
3444543332
3414543132
3444543332
3414543132
3344543322
1222332221""", 2: """
5666666665
4565665654
1kkk65kkk1
3444654332
3414654132
3444654332
3414654132
3344654322
2333443332""", 3: """
6666666665
efeeffeedc
1kkkfekkk1
3444fe4332
3414fe4132
3444fe4332
3414fe4132
3344fe4322
bccccccccb"""}

def helm_can(tier):
    P = pal(STEEL)
    def paint(face, w, h):
        if face == "front": return grid(CAN[tier], P)
        if face == "bottom": return blank(w, h)
        g = plate(base=4 if face in ("back", "left") else 3, spec=False)(face, w, h)
        if face in ("right", "left"):
            near = (w - 2, w - 1) if face == "right" else (0, 1)     # the slit wraps a little round the cheek
            for x in near: g[2][x] = INK
            for x in range(w):
                if tier >= 2 and x % 3 == 1: g[h - 2][x] = STEEL[5]      # a row of rivets over the lower band
        if face == "back":
            for y in range(1, h - 1): g[y][w // 2] = STEEL[2]           # the welded seam
        if tier == 3 and face != "top":
            g[h - 1] = [GOLD[2] if x % 2 else GOLD[3] for x in range(w)]
            if face in ("right", "left"): g[1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
            if face == "back": g[1] = [GOLD[3]] * w
        return g
    return paint

def helm_step(tier):
    def paint(face, w, h):
        if face == "bottom": return blank(w, h)
        g = plate(base=4 if tier > 1 else 3, spec=False, seam=False)(face, w, h)
        if tier == 3 and face != "top": g[h - 1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
        return g
    return paint

def snout(tier):
    front = {2: ["65", "54", "54", "54", "43"], 3: ["fe", "ed", "fe", "ed", "dc"]}[tier]
    P = pal(STEEL)
    def paint(face, w, h):
        if face == "front": return [[P[c] for c in r] for r in front]
        if tier == 3: return plate(R=GOLD, base=3, spec=False)(face, w, h)
        return plate(base=3, spec=False)(face, w, h)
    return paint

def comb(tier):
    R = GOLD if tier == 3 else STEEL
    def paint(face, w, h):
        g = plate(R=R, base=4, spec=False, seam=False)(face, w, h)
        if face in ("right", "left"):
            for x in range(0, w, 2): g[0][x] = R[6]
        return g
    return paint

def mantling(T, M):
    """Team cloth hung from the helm's crown down its back, cut into leaves at the hem; lined in the emblem metal."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                v = (3 if face == "back" else 1) + (-1 if x % 3 == 1 and y > 2 else 0) + (1 if y == 0 else 0)
                g[y][x] = clamp(T, v)
        if face == "front":                       # the lining, seen past the helm
            for y in range(h): g[y][0] = g[y][w - 1] = M[1]
        for x in range(w):                        # the leaves: alternate long and short tongues
            if x % 3 == 1: g[h - 1][x] = None; g[h - 2][x] = None
            elif x % 3 == 2: g[h - 1][x] = None
            elif face == "back": g[h - 1][x] = M[2]
        return g
    return paint

# ---- body ----
def body_base(tier):
    if tier == 1: return rows([(0, mail(base=3)), (10, leather(base=2))])
    return rows([(0, plate(base=3)), (7, plate(base=3, spec=False)), (9, plate(base=2, spec=False)), (10, mail(base=3))])

def belt(tier):
    def paint(face, w, h):
        g = leather(base=1)(face, w, h)
        if face == "front":
            for y, row in enumerate(("..ed", "..cb")):
                for x, c in enumerate(row):
                    if c != ".": g[y][2 + x] = pal()[c]
        if tier == 3 and face in ("front", "back", "left", "right"):
            for x in range(0 if face != "front" else 1, w, 3): g[0][x] = GOLD[4]
        return g
    return paint

def arm(tier):
    if tier == 1:
        return rows([(0, mail(base=3)), (6, plate(base=3)), (10, leather(base=2, stitch=True))])
    gold = tier == 3
    trim = lambda f, w, h: [[(GOLD[3] if gold else STEEL[1]) for _ in range(w)]]
    return rows([(0, quilt(DARK, base=3)), (6, trim), (7, plate(base=3)), (10, K.gauntlet(gold))])

def leg(tier):
    if tier == 1:
        return rows([(0, quilt(DARK, base=3)), (6, plate(base=3, spec=False)), (9, plate(base=2, spec=False))])
    gold = tier == 3
    return rows([(0, plate(base=3)), (5, lambda f, w, h: [[(GOLD[3] if gold else STEEL[1]) for _ in range(w)]]),
                 (6, plate(base=3)), (9, plate(base=2))])

def spaulder(tier, part):
    """The initiate's single spaulder: the knight's dome over one lame, in plain steel."""
    return K.pauldron(2, part) if tier == 1 else K.pauldron(tier, part)

# ---- the cross: tabard, cape, shield ----
CROSS = """
........
...Lm...
...Lm...
.LLLLLm.
.dmLmmd.
...Lm...
...Lm...
...Lm...
...Lm...
...dd..."""

def cross_pal(M): return {"L": M[3], "m": M[2], "d": M[1]}

def tabard(T, M, tier):
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back") and y >= 7 and x in (1, 6): v -= 1      # folds under the belt
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face in ("right", "left"):            # open at the arms: the hauberk or plate shows through
            for y in range(0, 7):
                for x in range(w): g[y][x] = None
            g[7] = [T[1]] * w
        if face in ("front", "back"):
            over(g, grid(CROSS, cross_pal(M)), 0, 0)
        if tier == 3 and face in ("front", "back"):
            for y in range(h): g[y][0] = g[y][w - 1] = M[1] if y % 2 else M[2]
            g[0] = [M[2]] * w
        return g
    return paint

def flap(T, M, tier, inner_x):
    """The tabard's long skirts, front and back of each leg, to below the knee."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y >= h - 2 else 0)
                g[y][x] = clamp(T, v)
        if tier == 3:
            g[h - 1] = [M[1] if x % 2 else M[2] for x in range(w)]
            for y in range(h - 1): g[y][3 - inner_x] = M[1] if y % 2 else M[2]
        else:
            g[h - 1] = [T[1]] * w
        return g
    return paint

def cape(T, M, tier):
    big = """
.....Lm.....
.....Lm.....
.....Lm.....
..LLLLLLLm..
..dmmmLmmmd.
.....Lm.....
.....Lm.....
.....Lm.....
.....Lm.....
.....Lm.....
.....dd....."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = (2 if face != "back" else 3) + (-1 if face in ("left", "right") else 0) + (-1 if x % 3 == 2 and y > 3 else 0)
                g[y][x] = clamp(T, v)
        if face == "back":
            over(g, grid(big, cross_pal(M)), -1, 2)
            if tier == 3:
                for x in range(w): g[h - 1][x] = M[1] if x % 2 else M[2]
                for y in range(h): g[y][0] = g[y][w - 1] = M[1]
            else:
                g[h - 1] = [T[1]] * w
        if face in ("left", "right") and tier == 3:
            for y in range(h): g[y][0] = M[1] if y % 3 else M[2]
        return g
    return paint

def shield_face(T, M, tier):
    def paint(face, w, h):
        g = blank(w, h)
        if face != "front": return g
        for y in range(h):
            for x in range(w):
                if not in_shield(x, y): continue
                v = 4 if y < 5 and x < 5 and x + y < 6 else 2 if y >= 13 else 3
                if not in_shield(x, y, 2): v = 1
                g[y][x] = clamp(T, v)
        for y in range(2, 15):                        # the cross, two texels thick, lit on its left and top
            for x in (5, 6):
                if in_shield(x, y, 1): g[y][x] = M[3] if x == 5 else M[2]
        for x in range(2, 10):
            for y in (5, 6):
                if in_shield(x, y, 1): g[y][x] = M[3] if y == 5 else M[1] if x not in (5, 6) else g[y][x]
        if tier == 3:                                  # a gilt lozenge where the arms meet, and gilt tips
            for x, y in ((5, 2), (6, 2), (2, 5), (2, 6), (9, 5), (9, 6), (5, 14), (6, 14)): g[y][x] = M[3]
        return g
    return paint

# ---- assembling ----
def bones():
    B = []
    B.append(Bone("root", ""))
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), per_tier(body_base))]))
    B.append(Bone("head", "body", (0, -12, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", boxes=[Box((-4, -8, -4), (8, 8, 8), face_painter(k))], variants=[k]))
    B += arms(per_tier(arm))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B += legs(per_tier(leg))
    # the great helm: a can, a step on its crown, a comb and a visor snout from tier II, the gilt cross and mantling at III
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-5, -8, -5), (10, 9, 10), per_tier(helm_can)), Box((-4, -10, -4), (8, 2, 8), per_tier(helm_step))]))
    B.append(Bone("helm_snout", "head", tag="helm", stages=[2, 3], boxes=[Box((-1, -4, -6), (2, 5, 1), per_tier(snout, (2, 3)))]))
    B.append(Bone("helm_comb", "head", tag="helm", stages=[2], boxes=[Box((-0.5, -12, -4), (1, 2, 8), comb(2))]))
    B.append(Bone("helm_cross", "head", tag="helm", stages=[3], boxes=[
        Box((-0.5, -11, -4), (1, 1, 8), comb(3)), Box((-0.5, -15, -0.5), (1, 4, 1), plate(R=GOLD, base=4, spec=False)),
        Box((-1.5, -14, -0.5), (3, 1, 1), plate(R=GOLD, base=4, spec=False))]))
    B.append(Bone("mantling", "head", team=True, tag="helm", stages=[3], boxes=[Box((-4.5, -10, 5.2), (9, 12, 0), "mantling", sheet="cloth")]))
    # torso
    for st, g in (([1, 2], 1), ([3], 3)):
        B.append(Bone(f"tabard_t{g}", "body", team=True, stages=st, boxes=[Box((-4, -12, -2), (8, 12, 4), ("tabard", g), dil=0.3, sheet="cloth")]))
    B.append(Bone("gorget", "body", stages=[2, 3], boxes=[Box((-4.5, -12.5, -2.5), (9, 2, 5), per_tier(K.gorget, (2, 3)))]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), per_tier(belt), dil=0.45)]))
    for t in (2, 3):
        B.append(Bone(f"cape_t{t}", "body", (0, -12, 3.6), team=True, stages=[t], rot=(8, 0, 0), boxes=[
            Box((-5, 0, 0), (10, 18, 1), ("cape", t), sheet="cloth")]))
    B.append(Bone("collar", "body", (0, -12, 3.6), team=True, stages=[2, 3], rot=(8, 0, 0), boxes=[
        Box((-4.5, -0.5, -1.5), (9, 2, 2), "collar", sheet="cloth")]))
    # the shield, on the left forearm
    B.append(Bone("shield", "left_forearm", SHIELD_PIVOT, rot=SHIELD_ROT, tag="shield", boxes=
        shield_boxes(wood, per_tier(shield_rim))
        + [Box((-1, -5, -2.7), (2, 2, 1), per_tier(shield_boss)), Box((-1, -3, -0.9), (2, 5, 3), leather(base=2))]))
    for st, g in (([1, 2], 1), ([3], 3)):
        B.append(Bone(f"shield_face_t{g}", "shield", team=True, stages=st, boxes=[Box((-6, -10, -2.1), (12, 18, 0), ("shield", g), sheet="cloth")]))
    # arms and legs
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_spaulder", f"{side}_arm", (c, -2, 0), stages=[1], rot=(0, 0, 10 * c), boxes=[
            Box((-3.5, -1.5, -3.5), (7, 2, 7), spaulder(1, "dome")),
            Box((-3.5 + 0.5 * c, 0.5, -3), (7, 2, 6), spaulder(1, "lame1"), dil=0.15)]))
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), stages=[2, 3], rot=(0, 0, 16 * c), boxes=[
            Box((-4, -1.5, -4), (8, 2, 8), per_tier(lambda t: K.pauldron(t, "dome"), (2, 3))),
            Box((-4 + 0.5 * c, 0.5, -3.5), (8, 2, 7), per_tier(lambda t: K.pauldron(t, "lame1"), (2, 3)), dil=0.15),
            Box((-3.5 + 1.0 * c, 2.5, -3), (7, 2, 6), per_tier(lambda t: K.pauldron(2, "lame2"), (2, 3)), dil=0.15),
            Box((-3 + 1.5 * c, 4.5, -2.5), (6, 1, 5), per_tier(lambda t: K.pauldron(2, "lame2"), (2, 3)), dil=0.1)]))
        # the forearm's frame is the arm's less the elbow (c, 4, 0); the shin's is the leg's less the knee (0, 6, 0)
        B.append(Bone(f"{side}_couter", f"{side}_forearm", stages=[2, 3], boxes=[
            Box((-2.6 if c < 0 else 1.6, -1, -1.5), (1, 3, 3), per_tier(K.couter, (2, 3)))]))
        B.append(Bone(f"{side}_cuff", f"{side}_forearm", boxes=[Box((-2.5, 1, -2.5), (5, 2, 5), per_tier(lambda t: K.cuff(max(t, 2))))]))
        B.append(Bone(f"{side}_knee", f"{side}_shin", boxes=[Box((-2.5, -1, -3), (5, 2, 2), per_tier(lambda t: K.knee(max(t, 2))))]))
        B.append(Bone(f"{side}_boot", f"{side}_shin", boxes=[Box((-2.5, 3, -3), (5, 3, 5), per_tier(lambda t: K.boot(t if t > 1 else 1)))]))
        B.append(Bone(f"{side}_toe", f"{side}_shin", stages=[2, 3], boxes=[Box((-1.5, 5, -4), (3, 1, 1), per_tier(K.toe, (2, 3)))]))
        inner = 3 if side == "right" else 0
        for st, g in (([1, 2], 1), ([3], 3)):
            B.append(Bone(f"{side}_skirt_t{g}", f"{side}_leg", team=True, stages=st, boxes=[
                Box((-2, 0, -2.45), (4, 6, 0), ("flap", g, inner), sheet="cloth"),
                Box((-2, 0, 2.45), (4, 6, 0), ("flap", g, 3 - inner), sheet="cloth")]))   # both flaps end at the knee hinge (y6)
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "collar": return K.collar(T, M)
    if spec == "mantling": return mantling(T, M)
    kind, t = spec[0], spec[1]
    if kind == "tabard": return tabard(T, M, t)
    if kind == "cape": return cape(T, M, t)
    if kind == "shield": return shield_face(T, M, t)
    return flap(T, M, t, spec[2])

SENTINEL = Troop("sentinel", SKIN, CLOTH, bones, cloth_painter,
                 {"weapon": "longsword", "hand": "right", "idleArms": "hang", "portrait": "tall 4", "faces": {"count": 4, "card": 0}},
                 hide=lambda bn: bn.name.startswith("shield"), veteran=K.veteran)
def build(): return SENTINEL.build()
def outputs(): return SENTINEL.outputs()

if __name__ == "__main__":
    SENTINEL.main(sys.argv)
