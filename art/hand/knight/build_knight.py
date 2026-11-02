#!/usr/bin/env python3
"""The Order knight (Gilded house), hand-authored at one texel per model pixel.

Every box is placed by hand below and every face is painted either from an ASCII grid or from a small
material painter (plate, mail, quilted cloth, leather) whose light rules are written out here: light from
the top front and the wearer's right, a bright top rim, a dark bottom seam, rounder plate toward the edges.

  python build_knight.py preview <dir> [side]   three tier previews (Bedrock geo + one combined texture each)
  python build_knight.py install                 troop JSON, skins and cloth sheets into the mod's assets
"""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "common"))
from handlib import *   # noqa: F401,F403  ramps, painters, Box and Bone, packing, the Troop frame
from handlib import Troop

SKIN = (128, 128)      # the knight's skin sheets, one per armour tier
CLOTH = (128, 64)      # its team cloth sheets, one per side

# ---- helms ----
def coif_front(g, w, h):        # the squire's mail hood, open over eyes, nose and mouth
    for y in range(4, 8):
        for x in range(1, 8): g[y][x] = None
    return g

def great_helm(tier):
    if tier == 2:
        front = """
5556ee6555
4444dd4433
eeeeeeeedc
2kkkdckkk1
4554dc4432
4444dc4332
3444dc3k32
3344dc33k2
3333dc3k31
1222cb2210"""
        side = """
5555555554
4444444443
eddcdddcdc
3343333333
4443434433
3343333332
3333333332
3332323232
3332323322
1111111110"""
        back = """
4555555554
4444554443
cdddddddcc
3333443333
3333443332
3333443332
3332443332
3332443222
2222332222
1111111110"""
        top = """
2333333332
3444444443
3445445443
3445445443
3444ee4443
3444ee4443
3445445443
3445445443
3444444443
2333333332"""
    else:  # the captain's: gilded brow, sun on the forehead, gold breaths
        front = """
6eeffee66e
4e5dfd5e43
eedeffedcd
2kkkdckkk1
4554dc4f32
4454dc4d32
3445dc3e32
3344dc3e32
3343dc3e31
bccccccccb"""
        side = """
6666666665
5455545554
eeddcdddce
3343e33433
4443434433
3343333332
3333e33332
3332323232
3332323322
bcccccccbb"""
        back = """
5666666665
4444554443
ceedeedeec
3333dd3333
3333443332
33334c3332
33324c3332
33324c3222
2222332222
bcccccccbb"""
        top = """
2333333332
3455555543
3456656543
3455ff5543
355ffff553
355ffff553
3455ff5543
3456656543
3455555543
2333333332"""
    P = pal(STEEL)
    faces = {"front": grid(front, P), "left": grid(side, P), "back": grid(back, P), "top": grid(top, P)}
    faces["right"] = [list(reversed(r)) for r in faces["left"]]
    for r in faces["right"]:   # the lit side
        for i, c in enumerate(r):
            if c in STEEL[:-1]: r[i] = STEEL[STEEL.index(c) + 1]
    def paint(face, w, h):
        if face in faces: return [list(r) for r in faces[face]]
        return plate(base=1)(face, w, h)
    return paint

def crown(face, w, h):
    if face in ("top", "bottom"): return blank(w, h)
    return grid("""
e..e.f.e..e
ee.eefee.ee
dfedgfgdefd
bcccbcbcccb""", pal())

def halo(face, w, h):
    t = """
.....ef.....
..e..ef..e..
...e.ef.e...
....dfgd....
eee.feef.eee
ffffeggeffff
ffffeggeffff
eee.feef.eee
....dfgd....
...e.ef.e...
..e..ef..e..
.....ef....."""
    g = grid(t, pal())
    return g if face == "front" else ([list(reversed(r)) for r in g] if face == "back" else blank(w, h))

def spangen_cap(face, w, h):   # the squire's spangenhelm: steel plates in gilt bands, a brow band
    g = plate(base=3, spec=False, seam=False)(face, w, h)
    if face == "top":
        for y in range(h):
            for x in range(w):
                if x in (4, 5) or y in (4, 5): g[y][x] = GOLD[3] if (x + y) % 2 else GOLD[4]
        return g
    if face == "bottom": return blank(w, h)
    for y in range(h):
        for x in range(w):
            if x in (0, w - 1) or (face in ("front", "back") and x in (4, 5)): g[y][x] = GOLD[3] if y else GOLD[4]
            if y == h - 1: g[y][x] = GOLD[2] if x % 2 else GOLD[3]
    return g

# ---- body, cloth and limbs ----
CUIRASS = {2: """
35666653
45565443
45565443
34565432
34454332
33443322
23333221
11111111
45555443
21111111
nmnmnmnm
mnmnmnmn""", 3: """
cdeffedc
45565443
45565443
34565432
34454332
33443322
23333221
bccccccb
45555443
bbbbbbbb
nmnmnmnm
mnmnmnmn"""}

def body_base(tier):
    if tier == 1: return mail(base=3)
    sides = rows([(0, plate(base=3)), (7, plate(base=3, spec=False)), (9, plate(base=2, spec=False)), (10, mail(base=3))])
    back = """
35555553
34444443
34444433
34444432
33444332
33333322
23333221
11111111
34444433
21111111
nmnmnmnm
mnmnmnmn"""
    if tier == 3: back = back.replace("35555553", "cdddddcc", 1).replace("11111111", "bccccccb", 1).replace("21111111", "bbbbbbbb", 1)
    return with_faces(sides, front=grid(CUIRASS[tier], pal()), back=grid(back, pal()))

def gorget(tier):
    def paint(face, w, h):
        if face in ("top", "bottom"): return blank(w, h)
        g = plate(base=3, spec=False)(face, w, h)
        for x in range(w): g[h - 1][x] = (GOLD[4] if x % 2 else GOLD[3]) if tier == 3 else STEEL[1]
        return g
    return paint

def couter(tier):
    def paint(face, w, h):
        R = GOLD if tier == 3 else STEEL
        g = plate(R=R, base=4, spec=False)(face, w, h)
        if face in ("right", "left") and w >= 3: g[1][1] = R[6] if tier == 2 else STEEL[5]   # the wing's rivet
        return g
    return paint

def toe(tier):
    def paint(face, w, h):
        g = plate(base=3, spec=False, seam=False)(face, w, h)
        if tier == 3 and face == "front": g[0] = [GOLD[3]] * w
        return g
    return paint

def belt(tier):
    def paint(face, w, h):
        g = leather(base=2)(face, w, h)
        if face == "front":
            g[0][3] = g[0][4] = GOLD[4]; g[1][3] = GOLD[3]; g[1][4] = GOLD[2]
        if tier == 3 and face in ("front", "back", "left", "right"):
            for x in range(0 if face != "front" else 1, w, 3): g[0][x] = GOLD[4]
        return g
    return paint

def mantle(face, w, h):   # mail tippet over the shoulders, its lower edge in scallops
    g = mail(base=3)(face, w, h)
    if face == "bottom": return blank(w, h)
    if face != "top":
        for x in range(w):
            if x % 2: g[h - 1][x] = None
            else: g[h - 1][x] = MAIL[2]
    return g

def arm(tier):
    if tier == 1:
        return rows([(0, mail(base=3)), (7, leather(base=2)), (9, leather(base=3, stitch=True))])
    gold = tier == 3
    def couter(face, w, h):
        return [[STEEL[2] if not gold else GOLD[3]] * w]
    return rows([(0, plate(base=3)), (5, lambda f, w, h: [[(GOLD[3] if gold else STEEL[1]) for _ in range(w)]]),
                 (6, plate(base=3)), (9, gauntlet(gold))])

def gauntlet(gold):
    def paint(face, w, h):
        g = plate(base=3, spec=False)(face, w, h)
        for y in range(h):
            for x in range(w):
                if y == 1: g[y][x] = STEEL[2]
                if face == "front" and y == 2 and gold: g[y][x] = GOLD[4] if x % 2 == 0 else GOLD[2]
        return g
    return paint

def cuff(tier):
    if tier == 1: return leather(base=3, stitch=True)
    def paint(face, w, h):
        g = plate(base=4 if tier == 3 else 3)(face, w, h)
        if face not in ("top", "bottom") and tier == 3:
            for x in range(w): g[h - 1][x] = GOLD[3]
        if face in ("top", "bottom"): return blank(w, h)
        return g
    return paint

def pauldron(tier, part):
    """Spaulder plates: a domed cap ("dome") over two stepped lames ("lame1", "lame2"). Each lame rolls light
    on its upper edge and drops into a dark lower lip, so the stack reads as overlapping plates."""
    gold = tier == 3
    def paint(face, w, h):
        if face == "bottom": return plate(base=1, spec=False, rim=False)(face, w, h)
        if face == "top":
            g = blank(w, h)
            for y in range(h):
                for x in range(w):
                    dx, dy = abs(x - (w - 1) / 2), abs(y - (h - 1) / 2)
                    g[y][x] = STEEL[5] if dx + dy < 1 else (STEEL[4] if dx + dy < 2.5 else STEEL[3])
                    if part != "dome": g[y][x] = STEEL[2]
            if gold and part == "dome":
                for i in range(w): g[0][i] = g[h - 1][i] = GOLD[4]
                for i in range(h): g[i][0] = g[i][w - 1] = GOLD[3]
            return g
        base = {"dome": 4, "lame1": 3, "lame2": 3}[part] + LIT.get(face, 0)
        base = max(1, min(5, base))
        g = [[STEEL[base] for _ in range(w)] for _ in range(h)]
        for x in range(w):
            g[0][x] = STEEL[min(6, base + 1)] if part != "dome" else STEEL[min(6, base + 2)]
            g[h - 1][x] = (GOLD[3] if x % 2 else GOLD[2]) if gold else STEEL[1]
        if gold and part == "dome":
            for x in range(w): g[0][x] = GOLD[5] if x % 2 == 0 else GOLD[4]
        if face in ("front", "back", "left", "right") and part == "lame1" and w >= 5:   # rivets
            for x in (1, w - 2): g[0][x] = GOLD[5] if gold else STEEL[6]
        return g
    return paint

def leg(tier):
    if tier == 1:
        return rows([(0, mail(base=3)), (9, leather(base=2))])
    return rows([(0, plate(base=3)), (5, mail(base=2)), (6, plate(base=3)), (9, plate(base=2))])

def knee(tier):
    def paint(face, w, h):
        g = plate(base=4)(face, w, h)
        if face == "front": g[0][w // 2] = GOLD[5] if tier == 3 else STEEL[6]; g[1][w // 2] = GOLD[3] if tier == 3 else STEEL[3]
        return g
    return paint

def boot(tier):
    if tier == 1:
        def paint(face, w, h):
            g = leather(base=2)(face, w, h)
            if face not in ("top", "bottom"):
                for x in range(w): g[0][x] = LEATH[4]   # the turned-down cuff
            return g
        return paint
    def paint(face, w, h):
        g = plate(base=3)(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[1][x] = STEEL[2]
            if tier == 3 and face == "front":
                for x in range(w): g[h - 1][x] = GOLD[3]
        return g
    return paint

def shield_face(T, M, tier):
    def paint(face, w, h):
        g = blank(w, h)
        if face != "front": return g
        for y in range(h):
            for x in range(w):
                if not in_shield(x, y): continue
                v = 4 if y < 5 and x < 5 and x + y < 6 else 2 if y >= 13 else 3      # light from the upper left, the point in shade
                if not in_shield(x, y, 2):
                    v = 1                                                                 # the shadow under the rim
                g[y][x] = clamp(T, v)
        over(g, roundel(M), 2, 2)                # the sun, its boss at the centre: solid masses, so it reads at 1x
        if tier >= 2:                            # a chevron beneath it, two texels thick
            for i, xs in enumerate(((5, 6), (4, 5, 6, 7), (3, 4, 7, 8), (2, 3, 8, 9))):
                for x in xs:
                    if in_shield(x, 11 + i, 2): g[11 + i][x] = M[2] if x in (5 - i, 6 + i) else M[1]
        return g
    return paint

# ---- team cloth: tabard, its skirts, the captain's cape ----
def roundel(M):
    return grid("""
...ee...
..dffd..
.dfggfd.
efggffde
effgffde
.dfffdd.
..dddd..
...ee...""", {"d": M[1], "e": M[2], "f": M[2], "g": M[3]})

def sun(T, M, big=False):
    s = """
.e.ee.e.
..dffd..
edfggfde
edfggfde
..dffd..
.e.ee.e.""" if not big else """
..e..e..
...ee...
e.dffd.e
.dfggfd.
.dfggfd.
e.dffd.e
...ee...
..e..e.."""
    return grid(s, {"d": M[0], "e": M[1], "f": M[2], "g": M[3]})

def tabard(T, M, tier):
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back") and y >= 6 and x in (1, 6): v -= 1      # folds below the chest
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face in ("right", "left"):            # open at the arms: the hauberk or plate shows through
            for y in range(0, 7):
                for x in range(w): g[y][x] = None
            g[7] = [T[1]] * w
        if face == "front":
            over(g, sun(T, M), 0, 1)
        if face == "back" and tier >= 2:
            for y in range(1, 6): g[y][3] = g[y][4] = clamp(T, 2)
        if tier == 3 and face in ("front", "back"):
            for y in range(h): g[y][0] = g[y][w - 1] = M[1] if y % 2 else M[2]
        return g
    return paint

def flap(T, M, tier, inner_x):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y == h - 1 else 0)
                g[y][x] = clamp(T, v)
        if tier == 3:
            g[h - 1] = [M[1] if x % 2 else M[2] for x in range(w)]
        else:
            g[h - 1] = [T[1]] * w
        return g
    return paint

def crest(T, M, tail):
    """Horsehair dyed in the side's colour: strands run front to back on the crest and down the tail."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                along = y if tail else x
                v = 3 + LIT[face] + (1 if along % 3 == 0 else (-1 if along % 3 == 2 else 0))
                if face in ("front", "back", "left", "right") and not tail and y == h - 1: g[y][x] = M[0]; continue
                g[y][x] = clamp(T, v)
        if tail and face not in ("top", "bottom"):
            for x in range(w):
                if x % 2: g[h - 1][x] = None
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
            over(g, sun(T, M, big=True), 1, 3)
            for x in range(w): g[h - 1][x] = M[1] if x % 2 else M[2]
            for y in range(h): g[y][0] = g[y][w - 1] = M[1]
        if face in ("left", "right"):
            for y in range(h): g[y][0] = M[1] if y % 3 else M[2]
        return g
    return paint

def collar(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (1 if y == 0 else -1 if y == h - 1 else 0))
        if face == "front":                                   # the clasps
            for x in (0, w - 1): g[0][x] = M[3]; g[1][x] = M[1]
        return g
    return paint

def surcoat(T, M, tier, back):
    """The narrow surcoat worn over plate: steel shows at the shoulders and flanks; a small sun on the chest."""
    small = grid("""
e.ee.e
.dffd.
efggfe
efggfe
.dffd.
e.ee.e""", {"d": M[0], "e": M[1], "f": M[2], "g": M[3]})
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back") or (face == "back") != back: return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if 0 < x < w - 1 else -1)
                if y >= 7 and x in (1, w - 2): v -= 1
                if y == 0: v += 1
                g[y][x] = clamp(T, v)
        if not back: over(g, small, 0, 1)
        else:
            for y in range(1, h - 2): g[y][w // 2 - 1] = g[y][w // 2] = clamp(T, 2)
        if tier == 3:
            for y in range(h): g[y][0] = g[y][w - 1] = M[1] if y % 2 else M[2]
            g[0] = [M[2]] * w
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
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), per_tier(arm))]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), per_tier(arm))]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), per_tier(leg))]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), per_tier(leg))]))
    # helms
    B.append(Bone("helm_squire", "head", tag="helm", stages=[1], boxes=[
        Box((-4.5, -8.5, -4.5), (9, 9, 9), with_faces(mail(base=3), front=coif_front)),
        Box((-5, -9, -5), (10, 4, 10), spangen_cap),
        Box((-3, -10, -3), (6, 1, 6), plate(base=4, spec=False)),
        Box((-0.5, -11, -0.5), (1, 1, 1), plate(R=GOLD, base=4, spec=False)),
        Box((-0.5, -5, -5.5), (1, 3, 1), plate(base=4, spec=False)),
    ]))
    B.append(Bone("helm_great", "head", tag="helm", stages=[2, 3], boxes=[Box((-5, -9, -5), (10, 10, 10), per_tier(great_helm, (2, 3)))]))
    B.append(Bone("helm_crown", "head", tag="helm", stages=[3], boxes=[
        Box((-5.5, -11, -5.5), (11, 4, 11), crown),
    ]))
    B.append(Bone("crest", "head", team=True, tag="helm", stages=[3], boxes=[
        Box((-1, -12, -4), (2, 3, 9), "crest", sheet="cloth"), Box((-1, -11, 5), (2, 9, 2), "tail", sheet="cloth")]))
    # torso
    B.append(Bone("tabard", "body", team=True, stages=[1], boxes=[Box((-4, -12, -2), (8, 12, 4), "tabard1", dil=0.3, sheet="cloth")]))
    for t in (2, 3):
        B.append(Bone(f"surcoat_t{t}", "body", team=True, stages=[t], boxes=[
            Box((-3, -10, -2.3), (6, 10, 0), ("surcoat", t, False), sheet="cloth"),
            Box((-3, -10, 2.3), (6, 10, 0), ("surcoat", t, True), sheet="cloth")]))
    B.append(Bone("gorget", "body", stages=[2, 3], boxes=[Box((-4.5, -12.5, -2.5), (9, 2, 5), per_tier(gorget, (2, 3)))]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -2, -2), (8, 2, 4), per_tier(belt), dil=0.4)]))
    B.append(Bone("mantle", "body", stages=[1], boxes=[Box((-6, -12.5, -3), (12, 3, 6), mantle)]))
    B.append(Bone("cape", "body", (0, -12, 3.6), team=True, stages=[3], rot=(8, 0, 0), boxes=[
        Box((-5, 0, 0), (10, 17, 1), "cape", sheet="cloth"), Box((-4.5, -0.5, -1.5), (9, 2, 2), "collar", sheet="cloth")]))
    # the shield, on the left arm (TroopFeatures.Shield draws none of vanilla's for a model that carries its own)
    B.append(Bone("shield", "left_arm", SHIELD_PIVOT, rot=SHIELD_ROT, tag="shield", boxes=
        [Box((x, y, -1.9), (w, h, 1), wood) for (x, y), (w, h) in SHIELD_BODY]
        + [Box((x, y, -2.5), (w, h, 2), per_tier(shield_rim)) for (x, y), (w, h) in SHIELD_RIM]
        + [Box((-1, -5, -2.7), (2, 2, 1), per_tier(shield_boss)), Box((-1, -3, -0.9), (2, 5, 3), leather(base=2))]))
    for t in (1, 2, 3):
        B.append(Bone(f"shield_face_t{t}", "shield", team=True, stages=[t], boxes=[Box((-6, -10, -2.1), (12, 18, 0), ("shield", t), sheet="cloth")]))
    # arms
    for side, sx in (("right", -1), ("left", 1)):
        c = -1 if side == "right" else 1          # the arm's centre line in its own frame
        out = c                                   # outward along x, away from the body
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2, 0), stages=[2, 3], rot=(0, 0, 14 * c), boxes=[
            Box((-3.5, -1.5, -3.5), (7, 2, 7), per_tier(lambda t: pauldron(t, "dome"), (2, 3))),
            Box((-3.5 + 0.5 * out, 0.5, -3), (7, 2, 6), per_tier(lambda t: pauldron(t, "lame1"), (2, 3)), dil=0.15),
            Box((-3 + 1.0 * out, 2.5, -2.5), (6, 2, 5), per_tier(lambda t: pauldron(t, "lame2"), (2, 3)), dil=0.15),
        ]))
        B.append(Bone(f"{side}_couter", f"{side}_arm", stages=[2, 3], boxes=[
            Box((-3.6 if c < 0 else 2.6, 2, -1.5), (1, 3, 3), per_tier(couter, (2, 3)))]))
        B.append(Bone(f"{side}_toe", f"{side}_leg", stages=[2, 3], boxes=[Box((-1.5, 11, -4), (3, 1, 1), per_tier(toe, (2, 3)))]))
        B.append(Bone(f"{side}_cuff", f"{side}_arm", stages=[2, 3], boxes=[Box((c - 2.5, 5, -2.5), (5, 2, 5), per_tier(cuff, (2, 3)))]))
        B.append(Bone(f"{side}_glove", f"{side}_arm", stages=[1], boxes=[Box((c - 2, 6, -2), (4, 1, 4), leather(base=2), dil=0.3)]))
        B.append(Bone(f"{side}_boot", f"{side}_leg", boxes=[Box((-2.5, 9, -3), (5, 3, 5), per_tier(boot))]))
        B.append(Bone(f"{side}_knee", f"{side}_leg", stages=[2, 3], boxes=[Box((-2.5, 6, -3), (5, 2, 2), per_tier(knee, (2, 3)))]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, stages=[1], boxes=[
            Box((-2, 0, -2.4), (4, 6, 0), ("flap12", inner), sheet="cloth"), Box((-2, 0, 2.4), (4, 6, 0), ("flap12", 3 - inner), sheet="cloth")]))
        x0, inner3 = (-1, 2) if side == "right" else (-2, 0)   # the narrow skirts hang under the surcoat, x -3..3
        B.append(Bone(f"{side}_skirt_plate", f"{side}_leg", team=True, stages=[2], boxes=[
            Box((x0, 0, -2.4), (3, 6, 0), ("flap12", inner3), sheet="cloth"), Box((x0, 0, 2.4), (3, 6, 0), ("flap12", 2 - inner3), sheet="cloth")]))
        B.append(Bone(f"{side}_skirt_gilt", f"{side}_leg", team=True, stages=[3], boxes=[
            Box((x0, 0, -2.4), (3, 7, 0), ("flap3", inner3), sheet="cloth"), Box((x0, 0, 2.4), (3, 7, 0), ("flap3", 2 - inner3), sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard1": return tabard(T, M, 1)
    if spec == "cape": return cape(T, M)
    if spec == "collar": return collar(T, M)
    if spec[0] == "surcoat": return surcoat(T, M, spec[1], spec[2])
    if spec[0] == "shield": return shield_face(T, M, spec[1])
    if spec in ("crest", "tail"): return crest(T, M, spec == "tail")
    kind, inner = spec
    return flap(T, M, 1 if kind == "flap12" else 3, inner)

KNIGHT = Troop("knight", SKIN, CLOTH, bones, cloth_painter,
               {"weapon": "arming_sword", "hand": "right", "idleArms": "hang", "portrait": "standard", "faces": {"count": 4, "card": 0}},
               hide=lambda bn: bn.name.startswith("shield"))

def build(): return KNIGHT.build()
def outputs(): return KNIGHT.outputs()

def main():
    if len(sys.argv) > 1 and sys.argv[1] == "shield":   # the shield alone, square to the eye: face, three-quarter, back, per tier
        import render, copy
        B, skins, cloths = build()
        out = sys.argv[2]; side = sys.argv[3] if len(sys.argv) > 3 else "crimson"
        S = [copy.copy(b) for b in B if b.name in ("root",) or b.name.startswith("shield")]
        for b in S:
            if b.name == "shield": b.parent, b.pivot, b.rot = "root", [0, 0, 0], [0, 0, 0]
        tiles = []
        for t in (1, 2, 3):
            vis = lambda bn, t=t: on(bn, t)
            tiles += [render.render(S, vis, {"skin": skins[t], "cloth": cloths[side]}, y, p, px=10) for y, p in ((0, 0), (-35, 10), (180, 0))]
        render.sheet_of(tiles).save(out); print("shield sheet", out)
    else:
        KNIGHT.main(sys.argv)

if __name__ == "__main__":
    main()
