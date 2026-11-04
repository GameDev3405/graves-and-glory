#!/usr/bin/env python3
"""The Bone Archer (Graveborn, troop id bonebow), hand-authored at one texel per model pixel.

A gaunt skeleton, 32 px tall: bare bone arms and legs 2 px thick on a body 12 px tall (a hunch of 8 degrees), the skull bare
and grinning with two dim soul-light pinpricks in its sockets (the glow sheet), and over its crown a dark moss rag hood
that frames the face and slumps back to a torn point. A short ragged moss collar over the shoulders, a ribcage under a team
jerkin torn open at the sides and ragged at the hem, a belt of rotted cloth with a violet cord and a bone buckle, rag tatters
in team cloth and moss hanging over the hips, and a bone quiver of lashed ribs on the back, its arrows fletched in grey.
The mark: a grave-iron pauldron on the bow shoulder (the left, the bow is in the left hand) crowned with a skull. Violet
sinew lashes the wrists, ankles, hems, belt and quiver. The bow (weapon "bone_bow") is art/hand/weapons/build_gb_bone_bow.py.
Tier II (the Barrow Archer): salvaged grave-iron plate: a gorget, a spiked right pauldron, bracers and iron bands on the quiver,
all piped in violet. Tier III (the Deadeye): black plate chased in funeral gilt, a gilt-banded bone crown of tines over the
hood, a bow-arm plate with a skull and a fan of extra gilt-fletched arrows. No flag on the back.

  python build_bonebow.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
import graveborn as GB
from graveborn import VIOLET as V, BONE as BN, MOSS, ROT, GIRON as GI, SOUL, GILT, gb_plate, gb_spike, gb_gilt, gb_band
import house_stub

SKIN = (128, 64)
CLOTH = (128, 64)
LEAN = 8        # degrees the torso leans forward; the arms stand back up, the head half way
INK = hx("07060a")

def rnd(*key): return random.Random("bonebow-%s" % (key,))

def sh(R, base, face, x=0, w=0, fold=0):
    """Ramp index for a texel: lit by face and by column, an optional fold line every `fold` columns."""
    v = base + LIT[face]
    if face not in ("top", "bottom") and w: v += lightx(face, x, w)
    if fold and face not in ("top", "bottom") and x % fold == fold - 1: v -= 1
    return clamp(R, v)

def grime(g, R, key, n, step=-1):
    """Weathering: n texels of the sheet pushed a step along ramp R."""
    r = rnd(*key); h, w = len(g), len(g[0])
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        if g[y][x] is None: continue
        try: i = R.index(g[y][x])
        except ValueError: continue
        g[y][x] = R[max(0, min(len(R) - 1, i + step))]
    return g

def ragged(g, key, depth, p=0.6):
    """Cut a ragged lower edge: a share p of the columns lose 0..depth rows from the bottom."""
    r = rnd("rag", key); h, w = len(g), len(g[0])
    for x in range(w):
        d = r.randrange(0, depth + 1) if r.random() < p else 0
        for y in range(h - d, h): g[y][x] = None
    return g

# ---- bare bone limbs: knuckles, a moss bandage, violet sinew wraps ----
def bone_limb(joints=(), wraps=(), bandage=(), base=3, key="bl"):
    """2 px bone: a dark seam and a light ball at each joint row, a moss rag bandage and a violet sinew wrap on their rows."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = clamp(BN, base + (1 if face == "top" else -2)); continue
                i = sh(BN, base, face, x, w)
                if y in joints: i = clamp(BN, base - 2)
                elif y - 1 in joints: i = clamp(BN, base + 1)
                if y == h - 1: i = clamp(BN, base - 1)
                g[y][x] = i
        if face not in ("top", "bottom"):
            for y in bandage:
                if 0 <= y < h:
                    for x in range(w): g[y][x] = sh(MOSS, 2 + (y % 2), face, x, w)
            for y in wraps:
                if 0 <= y < h:
                    for x in range(w): g[y][x] = V[1 + ((x + y) % 3 == 0) + (face in ("front", "right") and (x + y) % 2 == 0)]
        return grime(g, BN, (key, face), 3)
    return paint

def arm(side):
    if side == "right": return bone_limb(joints=(5,), bandage=(7, 8), wraps=(10,), key="armr")        # the drawing arm: a guard on the fingers
    return bone_limb(joints=(5,), bandage=(1, 2), wraps=(10,), key="arml")                            # the bow arm

def leg(side):
    return bone_limb(joints=(6,), bandage=(1, 2) if side == "right" else (2, 3), wraps=(8,) if side == "right" else (), key="leg" + side)

def foot(face, w, h):
    g = [[clamp(BN, 2 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)) for x in range(w)] for y in range(h)]
    if face not in ("top", "bottom"):
        for x in range(w): g[0][x] = V[1 + (x % 2)]                                  # the ankle wrap
        if face == "front":
            for x in (0, 2): g[h - 1][x] = BN[1]                                      # toe gaps
    return g

# ---- the skull: bare, grinning ----
SKULL = """
dbbbbbbd
bbBBBBbb
bBBBBBBb
BkkBBkkB
BkkBBkkB
BBBkkBBB
BtBkkBtB
.tTtTtT.
"""
def skull(face, w, h):
    p = {"d": BN[1], "b": BN[2], "B": BN[3], "t": BN[4], "T": BN[2], "k": INK}
    if face == "front":
        g = grid(SKULL, p)
        g[1][5] = BN[1]; g[2][5] = BN[1]                                             # a crack across the brow
        g[7][3] = BN[0]; g[7][4] = BN[0]; g[6][3] = BN[2]                            # a missing tooth, the dark mouth behind
        return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            i = 2 + (1 if y > 3 else 0) + LIT[face]
            if face in ("top", "back"): i = 1 + (1 if (x + y) % 3 == 0 else 0)
            if face == "bottom": i = 0
            g[y][x] = clamp(BN, i)
    if face in ("left", "right"): g[4][w // 2 - 1] = g[4][w // 2] = BN[0]            # the ear hole
    return g

def skull_lit(face, w, h):
    g = blank(w, h)
    if face == "front": g[3][2] = g[3][5] = SOUL[1]                                  # a dim pinprick in each socket
    return g

# ---- the hood: dark moss rag, framing the face, slumping to a torn point ----
def hood(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = sh(MOSS, 3, face, x, w, fold=3)
            if rnd("hoodp", face, x // 2, y // 2).random() < 0.25: g[y][x] = clamp(ROT, 3 + LIT[face] + (1 if face == "front" else 0))   # patches of rotted cloth
    if face == "front":
        for y in range(3, h):
            for x in range(1, w - 1): g[y][x] = None                              # the opening: the whole face
        for x in range(1, w - 1):
            g[2][x] = MOSS[1]                                                     # the brow's shadow under the lip
            if x % 3 == 0: g[3][x] = MOSS[1]                                      # a ragged fringe over the brow
        for y in range(3, h - 1):
            g[y][0] = MOSS[3 if y % 2 else 2]; g[y][w - 1] = MOSS[1]              # the cheek frames, the lit one on the right
        g[h - 1][0] = g[h - 1][w - 1] = None
        g[h - 2][0] = MOSS[1]
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face == "top":
        for x in range(w): g[h // 2][x] = MOSS[1]
    if face in ("left", "right"):
        for y in range(3, h):
            for x in range(0, 3): g[y][x] = None                                   # open at the front, below the temples
        g[h - 2] = [V[1 + (x % 2)] if g[h - 2][x] is not None else None for x in range(w)]   # a violet cord round the hood's base
        ragged(g, ("hood", face), 1, 0.7)
    if face == "back":
        g[h - 2] = [V[1 + (x % 2)] for x in range(w)]
        ragged(g, ("hood", face), 2, 0.7)
    return grime(g, MOSS, ("hood", face), 8, 1)

def tip(face, w, h):
    g = [[sh(MOSS, 2, face, x, w, fold=2) for x in range(w)] for _ in range(h)]
    if face not in ("top", "bottom"):
        for x in range(w): g[0][x] = MOSS[4]
        for x in range(0, w, 2): g[h - 1][x] = ROT[3]                              # a stitched seam
    return g

def collar(face, w, h):
    """A short moss collar over the shoulders, torn in strips along the hem, a violet thread in the seam."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(MOSS, 2 + LIT[face] + (lightx(face, x, w) if face in ("front", "back") else 0) + (1 if y == 0 else 0))
            if rnd("coll", face, x, y).random() < 0.2: g[y][x] = clamp(ROT, 3 + LIT[face])
    if face in ("top", "bottom"):
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None                              # open at the neck
    else:
        for x in range(0, w, 2): g[h - 2][x] = ROT[1]
        ragged(g, ("collar", face), 1, 0.65)
    return grime(g, MOSS, ("collar", face), 6, 1)

# ---- body: a ribcage over a pelvis ----
def body(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(BN, 2 + (1 if face == "top" else -2)); continue
            lx = lightx(face, x, w) if face in ("front", "back") else 0
            if y == 0: g[y][x] = clamp(BN, 4 + LIT[face])                         # the collarbone
            elif y <= 8:
                rib = (y % 2 == 1)                                                 # rib rows against dark gaps
                g[y][x] = clamp(BN, (3 if rib else 0) + LIT[face] + lx)
            else:
                g[y][x] = clamp(BN, 2 + LIT[face] + lx)                            # the pelvis and lumbar spine
    if face == "front":
        for y in range(1, 9): g[y][w // 2 - 1] = BN[4]; g[y][w // 2] = BN[2]       # the sternum
        for y in range(9, h): g[y][w // 2 - 1] = g[y][w // 2] = BN[0]              # the pelvis's open middle
        g[11][0] = g[11][w - 1] = BN[1]
    if face == "back":
        for y in range(h): g[y][w // 2 - 1] = BN[4] if y % 2 == 0 else BN[2]; g[y][w // 2] = BN[1]   # the spine
        for y in range(2, 6):
            g[y][1] = g[y][w - 2] = BN[2]                                         # the shoulder blades
    if face in ("left", "right"):
        for y in range(1, 9):
            if y % 2 == 0:
                for x in range(w): g[y][x] = BN[0]
    return grime(g, BN, ("body", face), 7, -1)

def belt(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(ROT, 2 + LIT[face] + (1 if y == 0 else 0)) if face not in ("top", "bottom") else ROT[2]
    if face == "front":
        for y in range(h):
            for x in (w // 2, w // 2 + 1): g[y][x] = BN[3]                         # the bone buckle
        g[0][w // 2] = g[0][w // 2 + 1] = BN[5]
    return g

def cord(face, w, h):
    g = [[V[2 if y % 2 else 1] for x in range(w)] for y in range(h)]
    g[h - 1] = [BN[4]] * w
    return g

# ---- the mark: a skull on a grave-iron pauldron over the bow shoulder ----
def pad(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(GI, 4 if face == "top" else 1); continue
            g[y][x] = clamp(GI, 4 + LIT[face] + (lightx(face, x, w) if face in ("front", "back") else 0) + (1 if y == 0 else 0))
            if (x + y) % 3 == 0: g[y][x] = clamp(GI, 3 + LIT[face])                  # lames
    if face not in ("top", "bottom"):
        for x in range(w): g[h - 1][x] = V[1 + (x % 2)]                            # the rim piped in violet sinew
    return grime(g, GI, ("pad", face), 5, 1)

PSKULL = """
dbBbd
BBBBB
BkBkB
BBkBB
.tTt.
"""
def pskull(face, w, h):
    p = {"d": BN[1], "b": BN[2], "B": BN[3], "t": BN[4], "T": BN[2], "k": INK}
    if face == "front": return grid(PSKULL, p)
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(BN, 2 + LIT[face] + (1 if face == "top" else 0) + (-1 if y == h - 1 else 0))
    if face in ("left", "right"):
        g[2][1 if face == "left" else w - 2] = BN[0]                                  # an eye socket in profile
    return g

# ---- the quiver of lashed bone ribs, and the arrows ----
def quiver(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face == "top": g[y][x] = ROT[0]; continue                              # its mouth, full of shafts
            if face == "bottom": g[y][x] = GI[2]; continue
            rib = (x % 3 != 1)                                                        # ribs: pale, a dark gap between
            g[y][x] = clamp(BN, (3 if rib else 0) + LIT[face] + (1 if y == 0 else 0))
    if face not in ("top", "bottom"):
        for y in (2, h - 4):
            for x in range(w): g[y][x] = V[2 if LIT[face] >= 0 else 1]                    # the sinew lashings
        g[h - 1] = [GI[2]] * w                                                        # the iron shoe
    return grime(g, BN, ("quiver", face), 4, -1)

def arrows(face, w, h):
    """Shafts and grey feather fletching, three deep, dark nocks on top."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face == "top": g[y][x] = BN[4] if (x + y) % 2 else ROT[2]
            elif face == "bottom": g[y][x] = None
            else: g[y][x] = ROT[5] if (x + y) % 2 == 0 else (ROT[3] if y < h - 1 else ROT[2])
    if face not in ("top", "bottom"):
        for x in range(1, w, 2): g[0][x] = None                                       # ragged feather tips
    return g

# ---- rag tatters, in moss or in the team's cloth, hemmed in violet ----
def rag_skin(cut, key):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = sh(MOSS, 3, face, x, w, fold=2) if face not in ("top", "bottom") else MOSS[2]
        if face in ("front", "back"):
            for x in range(w): g[h - 2][x] = V[1 + (x % 2)]
            ragged(g, key, cut, 0.9)
        return g
    return paint

def tatter(T, key, cut):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + (1 if face == "front" else 0) - (1 if x == w - 1 else 0))
        for x in range(w): g[h - 2][x] = V[1 + (x % 2)]
        ragged(g, ("tatter", key, face), cut, 0.9)
        return g
    return paint

def jerkin(T, M):
    """A short team jerkin: torn open at the sides, a hole at the ribs, ragged at the hem, a violet cord at the chest, the quiver strap."""
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face == "top":
            for y in range(h):
                for x in range(2, w - 2): g[y][x] = None                          # the neck
        if face in ("right", "left"):                                              # open under the arms
            for y in range(2, h):
                for x in range(w): g[y][x] = None
            for x in range(w): g[1][x] = V[1 + (x % 2)]                             # the strap's edge, bound in violet
        if face in ("front", "back"):
            for x in range(w): g[h - 2][x] = V[1 + (x % 2)]                         # the hem piped in violet sinew
            ragged(g, ("jerkin", face), 2, 0.8)
        if face == "front":
            for y in (2, 3, 4): g[y][3] = g[y][4] = V[2]                               # the chest lacing: a violet cord
            g[3][3] = g[3][4] = V[3]
            for y in range(1, h):
                x = 6 - (y * 6) // 9                                                  # the quiver strap, from the left shoulder to the right hip
                if 0 <= x < w and g[y][x] is not None: g[y][x] = ROT[3]
            for y in (5, 6):
                for x in (6, 7): g[y][x] = None                                       # a tear baring the ribs
        if face == "back":
            for y in range(1, h):
                x = 1 + (y * 5) // 9
                if g[y][x] is not None: g[y][x] = ROT[3]
            for y in (4, 5):
                for x in (5, 6): g[y][x] = None
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "jerkin": return jerkin(T, M)
    return tatter(T, spec[1], spec[2])

# ---- assembling ----
def glow(bone, box):
    return skull_lit if bone.name == "head" else None

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body)], rot=(LEAN, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), skull)], rot=(-LEAN // 2, 0, 0)))
    # the rag hood: open to the whole face; a crown bump and a point slumping back
    B.append(Bone("rag_hood", "head", tag="helm", boxes=[Box((-4.5, -8.5, -4.5), (9, 9, 9), hood)]))
    B.append(Bone("hood_tip", "head", (0, -8, 2), tag="helm", rot=(-35, 0, 0), boxes=[Box((-2, -4, -2), (4, 4, 4), tip)]))
    B.append(Bone("hood_tip2", "hood_tip", (0, -4, 0), tag="helm", rot=(-35, 0, 0), boxes=[Box((-1.5, -4, -1.5), (3, 4, 3), tip)]))
    B.append(Bone("hood_tip3", "hood_tip2", (0, -4, 0), tag="helm", rot=(-35, 0, 0), boxes=[Box((-1, -3, -1), (2, 3, 2), tip)]))
    B.append(Bone("right_arm", "body", (-5, -11, 0), [Box((-1, -2, -1), (2, 12, 2), arm("right"))], rot=(-LEAN, 0, 0)))
    B.append(Bone("right_hand", "right_arm", (0, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -11, 0), [Box((-1, -2, -1), (2, 12, 2), arm("left"))], rot=(-LEAN, 0, 0)))
    B.append(Bone("left_hand", "left_arm", (0, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-2, 12, 0), [Box((-1, 0, -1), (2, 12, 2), leg("right"))]))
    B.append(Bone("left_leg", "root", (2, 12, 0), [Box((-1, 0, -1), (2, 12, 2), leg("left"))]))
    B.append(Bone("right_foot", "right_leg", boxes=[Box((-1.5, 10, -3), (3, 2, 4), foot)]))
    B.append(Bone("left_foot", "left_leg", boxes=[Box((-1.5, 10, -3), (3, 2, 4), foot)]))
    # the rag collar, the belt and its cord
    B.append(Bone("collar", "body", boxes=[Box((-5, -12.5, -3), (10, 3, 6), collar, dil=0.25)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), belt, dil=0.45)]))
    B.append(Bone("cord", "body", boxes=[Box((-3.5, -2, -2.9), (1, 5, 1), cord)]))
    B.append(Bone("jerkin", "body", team=True, boxes=[Box((-4, -12, -2), (8, 10, 4), "jerkin", dil=0.3, sheet="cloth")]))
    # rag tatters over the hips: moss on the right, team cloth on the left, one behind
    B.append(Bone("tatters", "body", boxes=[
        Box((-3.5, -2, -2.7), (3, 8, 0), rag_skin(2, "ta")), Box((0.5, -2, 2.7), (3, 6, 0), rag_skin(2, "tb"))]))
    B.append(Bone("tatters_team", "body", team=True, boxes=[
        Box((0.5, -2, -2.8), (3, 9, 0), ("tatter", "fa", 3), sheet="cloth"), Box((-3, -2, 2.8), (3, 7, 0), ("tatter", "ba", 3), sheet="cloth")]))
    # the quiver of lashed ribs slung across the back, arrows over its mouth
    B.append(Bone("bone_quiver", "body", (0, -6, 2.9), rot=(0, 0, -26), boxes=[
        Box((-1.5, -4, 0), (3, 11, 3), quiver), Box((-1.5, -8, 0.5), (3, 4, 2), arrows)]))
    # the mark: a skull on a grave-iron pauldron over the bow (left) shoulder
    B.append(Bone("skull_pauldron", "left_arm", boxes=[
        Box((-2, -3.5, -3), (5, 3, 6), pad, dil=0.2),
        Box((-2, -8, -2.5), (5, 5, 4), pskull)]))
    # tier II, the Barrow Archer: salvaged grave-iron plate piped in violet
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    B.append(Bone("plate_gorget", "body", stages=[2, 3], boxes=[Box((-5.5, -13.2, -3.4), (11, 3, 6.8), P2("gg"), dil=0.1)]))
    B.append(Bone("right_pauldron", "right_arm", (0, -3, 0), rot=(0, 0, 10), stages=[2, 3], boxes=[
        Box((-3, -2.6, -3), (5, 3, 5.6), P2("rp"), dil=0.3),
        Box((-1.5, -6.2, -0.5), (1.5, 3.6, 1.5), per_tier(lambda t: gb_spike(GI, 3, 3), (2, 3)))]))
    for side in ("right", "left"):
        B.append(Bone(f"{side}_bracer", f"{side}_arm", stages=[2, 3], boxes=[Box((-1.8, 4.5, -1.8), (3.6, 3.5, 3.6), P2("br" + side, 0.18), dil=0.1)]))
    B.append(Bone("quiver_bands", "body", (0, -6, 2.9), rot=(0, 0, -26), stages=[2, 3], boxes=[
        Box((-2, -3.2, -0.5), (4, 2, 4), P2("qb1"), dil=0.1), Box((-2, 2.6, -0.5), (4, 2, 4), P2("qb2"), dil=0.1)]))
    B.append(Bone("rib_guard", "body", stages=[2, 3], boxes=[Box((-3, -7, -2.5), (6, 1.8, 0.6), P2("rg"), dil=0.05)]))
    # tier III, the Deadeye: a gilt-banded bone crown of tines, a gilt-pinned hood, a fan of fletched arrows
    B.append(Bone("crown_band", "head", stages=[3], boxes=[Box((-4.6, -8.6, -4.6), (9.2, 1.4, 9.2), gb_gilt("cb"), dil=0.05)]))
    for i, (x, z, h) in enumerate([(-3.6, -3.6, 5), (-1.2, -4.0, 7), (1.2, -4.0, 7), (3.6, -3.6, 5)]):
        B.append(Bone(f"crown_tine{i}", "head", stages=[3], boxes=[Box((x - 0.5, -8.6 - h, z - 0.5), (1, h, 1), gb_spike(BN, 4, 3))]))
    B.append(Bone("bow_plate", "left_arm", (0, -3, 0), rot=(0, 0, -10), stages=[3], boxes=[
        Box((-2, -2.8, -3), (5, 2, 6), gb_plate(3, "bp"), dil=0.35)]))
    for i, (rz, ox) in enumerate([(-34, -1), (-30, 1)]):
        B.append(Bone(f"fan_arrow{i}", "body", (ox, -6, 2.9), rot=(0, 0, rz), stages=[3], boxes=[
            Box((-0.5, -9, 0.6), (1, 5, 1), arrows), Box((-0.5, -9, 0.6), (1, 1.2, 1), gb_gilt("fa%d" % i))]))
    B.append(Bone("hood_pin", "body", stages=[3], boxes=[Box((-1.5, -12.6, -3.2), (3, 3, 1), gb_gilt("hp"), dil=0.05)]))
    return B

NOTE = ("Graveborn Bone Archer (unit identity, Graveborn): a gaunt skeleton, biped, bare bone limbs 2 px thick, a bare skull with two "
        "dim soul-light pinpricks, a dark moss rag hood slumping back to a point, a moss collar, a ribcage under a torn team jerkin, "
        "violet sinew on hems, wrists and ankles, a bone quiver, and a skull on a grave-iron pauldron on the bow shoulder (the "
        "mark). Tier I only; no flag on the back.")

BONEBOW = Troop("bonebow", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "bone_bow", "hand": "left", "idleArms": "hang", "portrait": "standard"}, glow=glow)
def build(): return BONEBOW.build()
def outputs(): return BONEBOW.outputs()

if __name__ == "__main__":
    house_stub.main(BONEBOW, NOTE, sys.argv)
