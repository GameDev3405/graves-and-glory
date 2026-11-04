#!/usr/bin/env python3
"""The Gravecaller (Graveborn, troop id hexer), hand-authored at one texel per model pixel.

A hunched caster in a burial robe that falls to the ground in three ragged tiers, each hem bound in violet. A broad
grave-iron mantle sits on the shoulders with a lit candle stub on each (a tiny soul flame, the glow sheet). A deep drooping
hood with a brim shades a gilt death mask (eye slits, a nose ridge, a violet trim line). A team vestment hangs over the
robe front and back; a violet cord girds the waist. Bone arms in bell sleeves; the right hand holds out a crook with a
lit lantern at arm's length: the mark (weapon "none", the lantern is modelled into the hand; soul-light on the glow
sheet). Tier II (Gravecaller II): grave-iron plate over the mantle with a gorget, iron bell-sleeve cuffs and a girdle plate,
piped in violet; the lantern (a weaponTiers part, one bone a tier) gets a bone crook and an iron cap. Tier III (Gravecaller III): black plate chased in funeral gilt, a gilt halo ring behind the hood, gilt cuffs
and a pale stone on the mask's brow. No flag on the back.

  python build_hexer.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
import graveborn as GB
from graveborn import VIOLET as V, BONE as BN, GIRON as GI, SOUL, ROT, GILT, gb_plate, gb_spike, gb_gilt, gb_band
import house_stub

SKIN = (128, 128)
CLOTH = (128, 64)
LEAN = 9        # degrees the body stoops forward; the arms stand back up, the head half way
INK = hx("0a0809")

def rnd(*key): return random.Random("hexer-%s" % (key,))

def sh(R, base, face, x=0, w=0, fold=0):
    v = base + LIT[face]
    if face not in ("top", "bottom") and w: v += lightx(face, x, w)
    if fold and face not in ("top", "bottom") and x % fold == fold - 1: v -= 1
    return clamp(R, v)

def shi(base, face, x=0, w=0, fold=0):
    """Ramp index (unclamped) for a texel."""
    v = base + LIT[face]
    if face not in ("top", "bottom") and w: v += lightx(face, x, w)
    if fold and face not in ("top", "bottom") and x % fold == fold - 1: v -= 1
    return v

def grime(g, R, key, n, step=-1):
    r = rnd(*key); h, w = len(g), len(g[0])
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        if g[y][x] is None: continue
        try: i = R.index(g[y][x])
        except ValueError: continue
        g[y][x] = R[max(0, min(len(R) - 1, i + step))]
    return g

def vband(x, y, lo=1):
    """A violet binding texel; tones 1 to 3 so it counts as the House accent."""
    return V[lo + ((x + y) % 3 == 0) + ((x * 2 + y) % 5 == 0)]

# ---- the burial robe: dark rotted cloth, long folds, a violet-bound hem ----
def robe_cloth(base=2, fold=3, hem=False, drop=0, key="rb", tooth=0, dark=False, sparse=False):
    """Robe cloth. hem: bind the last rows in violet; tooth: ragged strips cut from the bottom."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rnd("robe", key, face)
        for y in range(h):
            for x in range(w):
                i = shi(base, face, x, w, fold)
                if face not in ("top", "bottom") and (x * 7 + y // 5) % 5 == 0: i -= 1          # long dark folds
                g[y][x] = ROT[max(0, min(len(ROT) - 1, i))]
                if dark and y % 4 == 3 and face not in ("top", "bottom") and x % 2: g[y][x] = GI[2]
        if hem and face not in ("top", "bottom"):
            for x in range(w):
                if not sparse or x % 3 == 0: g[h - 2][x] = vband(x, 0, 1)
                if drop:
                    g[h - 3][x] = ROT[1]
                g[h - 1][x] = GI[1] if r.random() > 0.3 else None
            if tooth:
                for x in range(0, w, 2):
                    if r.random() < 0.7:
                        g[h - 1][x] = None
                        if r.random() < 0.5: g[h - 2][x] = None
        if face == "bottom":
            for y in range(1, h - 1):
                for x in range(1, w - 1): g[y][x] = None
        return grime(g, ROT, ("rb", key, face), 8, 1)
    return paint

def torso(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(ROT, 3 + (1 if face == "top" else -2)); continue
            i = shi(3, face, x, w, 3)
            if (x + y // 3) % 4 == 0: i -= 1
            g[y][x] = ROT[max(0, min(len(ROT) - 1, i))]
    if face == "front":
        for y in range(1, h): g[y][w // 2 - 1] = ROT[1]; g[y][w // 2] = ROT[4]                  # the robe's placket
        for x in range(w // 2 - 2, w // 2 + 2): g[0][x] = BN[1]                                    # a v of bare throat
        g[1][w // 2 - 1] = g[1][w // 2] = BN[2]
    if face == "back":
        for y in range(h): g[y][w // 2 - 1] = ROT[1]
    return grime(g, ROT, ("torso", face), 8, 1)

# ---- the skull in the hood, and the gilt death mask over it ----
def skull(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = BN[0 + (1 if y > 3 else 0) + ((x + y) % 4 == 0)]
    return g

MASK = """
vvvvvv
ldGGdg
GkGGkG
GGllGg
gGlLGg
.gddg.
"""
def mask(face, w, h):
    p = {"v": V[3], "l": GILT[4], "G": GILT[4], "g": GILT[3], "d": GILT[1], "k": INK, "L": GILT[4]}
    if face == "front":
        g = grid(MASK, p)
        for y in range(1, 6):                                                    # a violet trim line down each cheek edge
            if g[y][0] is not None and y > 2: g[y][0] = V[2]
            if g[y][5] is not None and y > 2: g[y][5] = V[1]
        return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = clamp(GILT, 3 + LIT[face])
    return g

def mask_lit(face, w, h):
    g = blank(w, h)
    if face == "front": g[2][1] = g[2][4] = SOUL[2]                                   # soul-light behind the slits
    return g

# ---- the hood: a deep drooping cowl, brim and tail ----
def hood(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = sh(GI, 3, face, x, w, 3)
    if face == "front":
        for y in range(2, h):
            for x in range(2, w - 2): g[y][x] = None                           # the opening, 6 wide, to the chin
        for y in range(2, h):
            g[y][1] = V[1 + (y % 2)]                                           # the violet lip, both edges
            g[y][w - 2] = V[1 + (y % 2 == 0)]
        for x in range(w): g[1][x] = GI[1]                                     # shadow under the brim
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face in ("left", "right", "back"):
        for x in range(w):
            if x % 3 == 0: g[h - 2][x] = V[1 + (x % 2)]
        r = rnd("hood", face)
        g[h - 1] = [GI[1] if r.random() < 0.65 else None for _ in range(w)]
    return grime(g, GI, ("hood", face), 8, 1)

def cowl_top(face, w, h):
    g = [[sh(GI, 3, face, x, w, 2) for x in range(w)] for _ in range(h)]
    if face not in ("top", "bottom"):
        for x in range(w): g[0][x] = GI[4]
    return g

def brim(face, w, h):
    g = [[sh(GI, 4, face, x, w, 0) for x in range(w)] for _ in range(h)]
    if face == "front":
        for x in range(w): g[h - 1][x] = V[1 + (x % 2)]                           # the brim edge piped in violet
    return g

def tail(face, w, h):
    g = [[sh(GI, 2, face, x, w, 2) for x in range(w)] for _ in range(h)]
    if face in ("front", "back"):
        r = rnd("tail", face)
        for x in range(w):
            g[h - 2][x] = V[2]
            g[h - 1][x] = GI[1] if r.random() < 0.55 else None
    return g

# ---- grave-iron mantle with a violet-bound hem; candle stubs ----
def mantle(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            i = 4 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0)
            if (x + y) % 4 == 0: i -= 1
            g[y][x] = clamp(GI, i)
    if face == "top":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None                           # open for the hood
    if face in ("front", "back", "left", "right"):
        r = rnd("mantle", face)
        for x in range(w):
            if x % 2 == 0: g[h - 2][x] = V[1 + (x % 4 == 0)]
            g[h - 1][x] = GI[1] if r.random() < 0.6 else None
    return grime(g, GI, ("mantle", face), 6, 1)

def candle(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face == "bottom": g[y][x] = BN[1]; continue
            g[y][x] = clamp(BN, 4 + LIT[face] + (lightx(face, x, w) if face not in ("top",) else 0))
            if face != "top" and y == 1 and x % 2 == 0: g[y][x] = BN[3]            # wax runnels
            if face != "top" and y == h - 1: g[y][x] = BN[2]
    return g

def flame(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = SOUL[1] if y == h - 1 else SOUL[2 + (y == 0)]
    return g

def flame_lit(face, w, h):
    return flame(face, w, h)

# ---- cords, bead string, pendant ----
def cord(face, w, h):
    g = [[V[2 if (y + (face == "front")) % 2 else 1] for x in range(w)] for y in range(h)]
    g[h - 1] = [BN[4]] * w
    return g

def girdle(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = (V[1 + (x % 3 == 0)] if (x + y) % 3 == 0 else GI[2 + (x % 2)]) if face not in ("top", "bottom") else GI[2]
    if face == "front":
        g[0][w // 2 - 1] = g[0][w // 2] = GILT[3]                                  # a gilt buckle
    return g

def beads(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = BN[3 + ((x + y) % 2)] if face not in ("top", "bottom") else BN[2]
    return g

def pendant(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w): g[y][x] = GILT[2 + (y == 0) + (face == "front")]
    return g

# ---- arms and sleeves ----
def bone_arm(side):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = clamp(BN, 3 + (1 if face == "top" else -2)); continue
                i = shi(3, face, x, w)
                if y == 5: i = 1                                          # the elbow knuckle
                elif y == 6: i = 4
                g[y][x] = BN[max(0, min(len(BN) - 1, i))]
        if face not in ("top", "bottom"):
            for x in range(w): g[9][x] = vband(x, 9, 1)                              # the wrist cord
            g[h - 1] = [BN[1]] * w
        return grime(g, BN, ("arm", side, face), 3)
    return paint

def sleeve(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = sh(ROT, 3, face, x, w, 2) if face not in ("top", "bottom") else ROT[3 if face == "top" else 1]
    if face not in ("top", "bottom"):
        r = rnd("sleeve", face)
        for x in range(w):
            if x % 2 == 0: g[h - 2][x] = vband(x, 1, 1)
            g[h - 1][x] = GI[1] if r.random() < 0.55 else None
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    return grime(g, ROT, ("sleeve", face), 4, 1)

def leg_paint(face, w, h):
    g = [[sh(BN, 2, face, x, w) if face not in ("top", "bottom") else BN[1] for x in range(w)] for _ in range(h)]
    return g

# ---- the lantern: an iron crook, a hung lantern body with a soul-lit glass ----
def iron(face, w, h):
    g = [[clamp(GI, 5 + LIT[face] + (1 if (x + y) % 3 == 0 else 0)) for x in range(w)] for y in range(h)]
    return g

def lantern_cap(face, w, h):
    g = [[clamp(GI, 4 + LIT[face] + lightx(face, x, w) * (face not in ("top", "bottom"))) for x in range(w)] for y in range(h)]
    if face in ("front", "back", "left", "right"):
        for x in range(w): g[h - 1][x] = GILT[2]
    if face == "top":
        for y in range(h):
            for x in range(w): g[y][x] = GI[5] if (x, y) in ((1, 1), (2, 2)) else GI[3]
    return g

def lantern_glass(face, w, h):
    """Four faces: iron posts at the corners, a pale soul-lit glass between them."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = GI[3]; continue
            post = x == 0 or x == w - 1
            g[y][x] = GILT[2 + (face in ("front", "right"))] if post else SOUL[1 + (y in (1, 2))] if True else None
    if face not in ("top", "bottom"):
        for x in range(1, w - 1):
            g[0][x] = SOUL[1]; g[h - 1][x] = SOUL[0]                               # glass rim shadow
        g[1][1] = g[2][1] = SOUL[3]
        g[1][w - 2] = SOUL[2]; g[2][w - 2] = SOUL[2]
    return g

def lantern_glass_lit(face, w, h):
    g = blank(w, h)
    if face in ("top", "bottom"): return g
    for y in range(h):
        for x in range(1, w - 1): g[y][x] = SOUL[2 if y in (1, 2) else 1]
    g[1][1] = g[2][1] = SOUL[3]
    return g

def lantern_base(face, w, h):
    g = [[clamp(GI, 3 + LIT[face]) for x in range(w)] for y in range(h)]
    if face not in ("top", "bottom"):
        for x in range(w): g[0][x] = GILT[2]
    return g

# ---- the team vestment: a long cloth panel over the robe front and back, gilt thread, ragged hem ----
def vest(T, M, lower=False, back=False):
    def paint(face, w, h):
        g = blank(w, h)
        r = rnd("vest", lower, back, face)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = T[3]; continue
                i = 3 + LIT[face] + (1 if x == 0 else -1 if x == w - 1 else 0)
                if x == w // 2 and face in ("front", "back"): i -= 1                 # a centre fold
                g[y][x] = clamp(T, i)
        if face in ("front", "back") and w >= 4:
            for x in range(w): g[0 if not lower else 0][x] = clamp(T, 4) if not lower else g[0][x]
            for y in range(h):
                g[y][0] = M[2] if (y % 3 != 2) else g[y][0]; g[y][w - 1] = M[1] if (y % 3 != 2) else g[y][w - 1]   # gilt edge thread
            if lower:
                for x in range(w):
                    g[h - 2][x] = T[1]
                    g[h - 1][x] = T[0] if r.random() < 0.6 else None
                cx = w // 2
                g[2][cx - 1] = g[2][cx] = M[3] if len(M) > 3 else M[-1]                  # a small gilt mark
                g[3][cx - 1] = g[3][cx] = M[2]
            else:
                cx = w // 2
                g[h - 3][cx - 1] = g[h - 3][cx] = M[-1]; g[h - 2][cx - 1] = g[h - 2][cx] = M[2]
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec[0] == "up": return vest(T, M, lower=False, back=spec[1])
    return vest(T, M, lower=True, back=spec[1])

# ---- glow sheet: eyes in the slits, candle flames, the lantern glass ----
def glow(bone, box):
    if bone.name == "death_mask": return mask_lit
    if bone.name in ("candle_r", "candle_l") and box.size == [1, 2, 1]: return flame_lit
    if bone.name in ("lantern", "lantern_t2", "lantern_t3") and box.size == [4, 4, 4]: return lantern_glass_lit
    return None

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), torso)], rot=(LEAN, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -3), (8, 8, 8), skull)], rot=(-LEAN // 2 - 1, 0, 0)))
    B.append(Bone("death_mask", "head", boxes=[Box((-3, -7, -5.2), (6, 6, 1), mask)]))
    B.append(Bone("burial_hood", "head", tag="helm", boxes=[
        Box((-5, -9, -5), (10, 10, 10), hood),
        Box((-5, -9.5, -6.2), (10, 2, 2), brim, dil=0.1)]))
    B.append(Bone("hood_tail", "head", (0, -6, 4.8), rot=(-5, 0, 0), tag="helm", boxes=[Box((-3.5, 0, 0), (7, 13, 1), tail)]))
    # the arms: bare bone 2x12x2 under a bell sleeve; the right holds the lantern out
    for side, x, r in (("right", -5, -LEAN + 32), ("left", 5, -LEAN)):   # jointed: the elbow at the arm's mid-height, the hand 4 px under it
        B += limb(f"{side}_arm", "body", (x, -11, 0), (-1, -2, -1), bone_arm(side[0]), f"{side}_forearm", (0, 4, 0), size=(2, 12, 2), rot=(r, 0, 0))
        B.append(Bone(f"{side}_sleeve", f"{side}_arm", boxes=[Box((-2, -2.5, -2), (4, 7, 4), sleeve, dil=0.2)]))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", (0, 4, 0)))
    B.append(Bone("arms_folded", "body", (0, -10, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -2), ("left", 2)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-1, 0, -1), leg_paint, f"{side}_shin", KNEE, size=(2, 12, 2))
    # the burial robe, three flared ragged tiers to the ground
    B.append(Bone("burial_robe", "root", boxes=[
        Box((-5, 10, -3), (10, 7, 6), robe_cloth(4, 3, True, key="t1", sparse=True)),
        Box((-5.5, 15, -3.5), (11, 5, 7), robe_cloth(4, 3, True, key="t2", tooth=1, sparse=True)),
        Box((-6, 19, -4), (12, 5, 8), robe_cloth(3, 3, True, drop=1, key="t3", tooth=1, dark=True))]))
    # the mantle and its candle stubs
    B.append(Bone("mantle", "body", boxes=[Box((-7, -13.5, -3.5), (14, 5, 7), mantle, dil=0.2)]))
    B.append(Bone("candle_r", "body", boxes=[Box((-7, -17, -1), (2, 3, 2), candle), Box((-6.5, -19, -0.5), (1, 2, 1), flame)]))
    B.append(Bone("candle_l", "body", boxes=[Box((5, -17, -1), (2, 3, 2), candle), Box((5.5, -19, -0.5), (1, 2, 1), flame)]))
    B.append(Bone("girdle", "body", boxes=[Box((-4, -4, -2), (8, 1, 4), girdle, dil=0.45)]))
    B.append(Bone("cord", "body", boxes=[Box((2, -3, -2.8), (1, 8, 1), cord)]))
    B.append(Bone("beads", "body", boxes=[Box((-3, -11, -2.8), (6, 1, 1), beads), Box((-0.5, -10, -2.8), (1, 2, 1), pendant)]))
    # the team vestment: upper half on the body, lower half on the robe
    B.append(Bone("vestment", "body", team=True, boxes=[
        Box((-2.5, -12, -2.45), (5, 12, 1), ("up", False), dil=0.3, sheet="cloth"),
        Box((-2.5, -12, 1.45), (5, 12, 1), ("up", True), dil=0.3, sheet="cloth")]))
    B.append(Bone("vestment_front", "root", (0, 12, -3.4), rot=(-4, 0, 0), team=True,
                  boxes=[Box((-2.5, 0, -0.5), (5, 11, 1), ("lo", False), sheet="cloth")]))
    B.append(Bone("vestment_back", "root", (0, 12, 3.4), rot=(4, 0, 0), team=True,
                  boxes=[Box((-2.5, 0, -0.5), (5, 11, 1), ("lo", True), sheet="cloth")]))
    # the lantern: an iron crook off the right hand, the lantern hung from its end
    B.append(Bone("lantern", "right_hand", tag="weapon", weapon_tiers=[1], rot=(-32, 0, 0), boxes=[
        Box((-0.5, -1, -6), (1, 1, 7), iron),                                     # the crook, 7 px forward
        Box((-0.5, 0, -6), (1, 2, 1), iron),                                      # the chain
        Box((-2, 2, -7.5), (4, 1, 4), lantern_cap),                                 # the cap
        Box((-2, 3, -7.5), (4, 4, 4), lantern_glass),                               # the lit glass, 4 px
        Box((-2, 7, -7.5), (4, 1, 4), lantern_base)]))                              # the base: 6 px in all
    # the lantern's tiers (weaponTiers parts, one bone a tier; the same 6 px lantern): tier II a bone crook, the cap and
    # base salvaged iron piped in violet, a bone drop under the base; tier III black plate chased in gilt, a violet-bound bone
    # crook, a gilt chain and a gilt drop under the base
    B.append(Bone("lantern_t2", "right_hand", tag="weapon", weapon_tiers=[2], rot=(-32, 0, 0), boxes=[
        Box((-0.5, -1, -6), (1, 1, 7), gb_spike(BN, 3, 4)),
        Box((-0.5, 0, -6), (1, 2, 1), iron),
        Box((-2, 2, -7.5), (4, 1, 4), gb_plate(2, "hlc")),
        Box((-2, 3, -7.5), (4, 4, 4), lantern_glass),
        Box((-2, 7, -7.5), (4, 1, 4), gb_plate(2, "hlb")),
        Box((-0.5, 8, -6), (1, 1, 1), gb_spike(BN, 3, 4))]))
    B.append(Bone("lantern_t3", "right_hand", tag="weapon", weapon_tiers=[3], rot=(-32, 0, 0), boxes=[
        Box((-0.5, -1, -6), (1, 1, 7), gb_spike(BN, 3, 4)),
        Box((-0.7, -1.2, -3), (1.4, 1.4, 1.4), gb_band(V, 1, GILT)),
        Box((-0.5, 0, -6), (1, 2, 1), gb_gilt("hlch")),
        Box((-2, 2, -7.5), (4, 1, 4), gb_plate(3, "hlc3")),
        Box((-2, 3, -7.5), (4, 4, 4), lantern_glass),
        Box((-2, 7, -7.5), (4, 1, 4), gb_plate(3, "hlb3")),
        Box((-0.5, 8, -6), (1, 1, 1), gb_gilt("hlfn"))]))
    # tier II: salvaged grave-iron plate over the mantle, piped in violet
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    TN = per_tier(lambda t: gb_spike(GI, 3, 3), (2, 3))
    B.append(Bone("plate_gorget", "body", stages=[2, 3], boxes=[Box((-4.5, -13, -3), (9, 3, 6), P2("gg"), dil=0.55)]))
    for side, sx in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_pauldron", "body", (sx * 6.8, -11.5, 0), rot=(0, 0, sx * 14), stages=[2, 3], boxes=[
            Box((-2.5, -1.4, -3), (5, 3, 6), P2("pd" + side), dil=0.5),
            Box((-0.5, -5.4, -0.5), (1, 4, 1), TN)]))
        B.append(Bone(f"{side}_cuff", f"{side}_forearm", stages=[2, 3], boxes=[Box((-2.5, 0.3, -2.5), (5, 2.4, 5), P2("cf" + side, 0.15), dil=0.35)]))
    B.append(Bone("girdle_plate", "body", stages=[2, 3], boxes=[Box((-3, -5, -2.7), (6, 3, 0.8), P2("gp"), dil=0.55)]))
    # tier III: a gilt halo ring behind the hood, a pale stone on the mask's brow
    def halo(face, w, h):
        g = gb_gilt("hl")(face, w, h)
        if face in ("front", "back"):
            cx, cy = (w - 1) / 2, (h - 1) / 2
            for y in range(h):
                for x in range(w):
                    d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                    if d < w / 2 - 2.6 or d > w / 2 - 0.2: g[y][x] = None
        else:                                   # the rim: only where the ring touches the edge, no square frame
            for y in range(len(g)):
                for x in range(len(g[0])):
                    if not (4 <= (x if face in ("top", "bottom") else y) <= 9): g[y][x] = None
        return g
    B.append(Bone("halo", "head", (0, -8, 5.6), rot=(0, 0, 0), stages=[3], boxes=[Box((-7, -7, 0), (14, 14, 1), halo)]))
    B.append(Bone("mask_stone", "head", stages=[3], boxes=[Box((-0.5, -7.4, -5.7), (1, 1, 0.5), lambda f, w, h: [[SOUL[2]]])]))
    B.append(Bone("gilt_gorget", "body", stages=[3], boxes=[Box((-3.2, -13.3, -3.4), (6.4, 1.2, 0.6), gb_gilt("gg2"), dil=0.1)]))
    return B

NOTE = ("Graveborn Gravecaller (unit identity, Graveborn): a hunched caster in a three-tiered burial robe to the ground hemmed "
        "in violet, a grave-iron mantle with candle stubs, a deep drooping hood over a gilt death mask, a team vestment, bare "
        "bone arms in bell sleeves and a lit soul-light lantern held out on a crook. Tier II iron plate and a bone-crooked iron lantern, tier III black gilt plate, a gilt halo and a gilt-chased lantern; no flag on the back.")

HEXER = Troop("hexer", SKIN, CLOTH, bones, cloth_painter,
              {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "standard"}, glow=glow)
def build(): return HEXER.build()
def outputs(): return HEXER.outputs()

if __name__ == "__main__":
    house_stub.main(HEXER, NOTE, sys.argv)
