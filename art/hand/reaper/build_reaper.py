#!/usr/bin/env python3
"""The Grave Reaper (Graveborn, troop id reaper), hand-authored at one texel per model pixel.

A tall bone wraith in black: arms and legs of bare bone 2 px thick, a body 13 px tall on 14 px legs, so he stands 35 px
high before the hood's peak. A deep black hood with the skull set two px back inside it and two soul-light eyes burning
there (the glow sheet); a grave-iron mantle in two ragged tiers over a grave-iron cuirass whose lower ribs show bare; a
leather belt with a bone clasp and a violet cord; a team sash across the chest from the right shoulder to the left hip; a
long torn shroud of three strips down the back and three rag strips over the thighs, every hem bound in violet. His scythe
(weapon "scythe") raises a 10 px curved blade over the hood: the mark. Tier II (the Harvester): grave-iron pauldrons with
spikes, a breastplate, bracers and an iron-rimmed hood, piped in violet. Tier III (the Lord of Scythes): black plate chased in
funeral gilt, a gilt-ringed hood crowned with tines, longer pauldron spikes and a gilt collar. No flag on the back.

  python build_reaper.py preview <dir> [side] | contact <png> [side] | stub
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
SHROUD = ramp("08070a", "120f13", "1c181d", "272227", "35303a", "484250")   # black cloth with a cold violet-grey cast
LEAN = 7        # degrees the body leans forward; the arms stand back up, the head half way
INK_EYE = hx("07060a")

def rnd(*key): return random.Random("reaper-%s" % (key,))

def sh(R, base, face, x=0, w=0, fold=0):
    """Ramp index for a texel: lit by face and by column, an optional fold line every `fold` columns."""
    v = base + LIT[face]
    if face not in ("top", "bottom") and w: v += lightx(face, x, w)
    if fold and face not in ("top", "bottom") and x % fold == fold - 1: v -= 1
    return clamp(R, v)

def grime(g, R, key, n, step=-1, rows_=None):
    """Weathering: n texels of the sheet pushed a step along the ramp (R is the painter's own ramp)."""
    r = rnd(*key); h, w = len(g), len(g[0])
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        if g[y][x] is None or (rows_ and y not in rows_): continue
        try: i = R.index(g[y][x])
        except ValueError: continue
        g[y][x] = R[max(0, min(len(R) - 1, i + step))]
    return g

# ---- bare bone limbs with violet wraps ----
def bone_limb(joints=(), wraps=(), base=3, key="bl", ends_dark=False):
    """2 px bone: a knuckle (dark seam, light ball) at each joint row, a violet wrap on each wrap row."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                i = sh(BN, base, face, x, w)
                if face in ("top", "bottom"): g[y][x] = clamp(BN, base + (1 if face == "top" else -2)); continue
                if y in joints: i = clamp(BN, base - 2)
                elif y - 1 in joints: i = clamp(BN, base + 1)
                if y == h - 1: i = clamp(BN, base - 1)
                g[y][x] = i
        for y in wraps:
            if 0 <= y < h and face not in ("top", "bottom"):
                for x in range(w): g[y][x] = V[1 + ((x + y) % 3 == 0) + (face in ("front", "right") and (x + y) % 2 == 0)]
        return grime(g, BN, (key, face), 3)
    return paint

def arm(side):
    return bone_limb(joints=(5,), wraps=(9, 10) if side == "right" else (8, 9, 10), key="arm" + side)

def leg(side):
    return bone_limb(joints=(6,), wraps=(3, 11, 12) if side == "right" else (4, 11, 12), key="leg" + side)

def foot(face, w, h):
    g = [[clamp(BN, 2 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)) for x in range(w)] for y in range(h)]
    if face not in ("top", "bottom"):
        for x in range(w): g[0][x] = V[1 + (x % 2)]               # the ankle wrap
    return g

# ---- the skull, set deep in the hood ----
SKULL = """
dddddddd
dbbbbbbd
bbbbbbbb
BEEBBEEB
BkkBBkkB
BBBkkBBB
BtTtTtTt
.TTTTTT.
"""
def skull(face, w, h):
    p = {"d": BN[1], "b": BN[2], "B": BN[3], "t": BN[4], "T": BN[2], "k": INK_EYE, "E": SOUL[1]}
    if face == "front":
        g = grid(SKULL, p)
        for y in (6, 7):                                                  # a gaping jaw: the mouth in shadow
            for x in (2, 4, 6): g[y][x] = BN[0] if y == 6 and x == 4 else g[y][x]
        return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            i = 1 + (1 if y > 3 else 0) + (1 if face == "right" and x > w // 2 else 0)
            if face in ("top", "bottom", "back"): i = 0 + (1 if (x + y) % 3 == 0 else 0)
            g[y][x] = BN[i]
    if face in ("left", "right"): g[4][w // 2 - 1] = g[4][w // 2] = BN[0]            # the ear hole
    return g

def skull_lit(face, w, h):
    g = blank(w, h)
    if face == "front":
        for x in (1, 2, 5, 6): g[3][x] = SOUL[2]
        g[3][2] = g[3][5] = SOUL[3]
    return g

# ---- the hood: black cloth, a deep opening, a ragged lip ----
def hood(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = sh(SHROUD, 3, face, x, w, fold=3)
    if face == "front":
        for y in range(2, h):
            for x in range(2, w - 2): g[y][x] = None                       # the opening: 6 wide, down to the chin
        for x in range(1, w - 1): g[1][x] = SHROUD[1]                      # the brow's shadow line
        for y in range(2, h): g[y][1] = SHROUD[5] if y % 2 else SHROUD[4]; g[y][w - 2] = SHROUD[2]   # the lit lip on the right
        for x in range(2, w - 2): g[2][x] = SHROUD[5] if x % 2 else SHROUD[4]
        for y in range(h): g[y][0] = SHROUD[2]; g[y][w - 1] = SHROUD[1]
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face == "top":
        for x in range(w): g[h // 2][x] = SHROUD[1]
    if face in ("left", "right", "back"):
        g[h - 2] = [V[1 + (x % 2)] for x in range(w)]                       # a violet cord round the hood's base
        r = rnd("hood", face)
        g[h - 1] = [SHROUD[1] if r.random() < 0.7 else None for _ in range(w)]
    return grime(g, SHROUD, ("hood", face), 8, 1)

def peak(face, w, h):
    g = [[sh(SHROUD, 3, face, x, w, fold=2) for x in range(w)] for _ in range(h)]
    if face not in ("top", "bottom"):
        for x in range(w): g[0][x] = SHROUD[4]
    return g

def drape(face, w, h):
    g = [[sh(SHROUD, 2, face, x, w, fold=2) for x in range(w)] for _ in range(h)]
    if face not in ("top", "bottom"):
        r = rnd("drape", face)
        for x in range(w):
            g[h - 1][x] = V[2] if face == "back" else SHROUD[1]
            if r.random() < 0.4: g[h - 1][x] = None
    return g

# ---- body: the cuirass over bare ribs ----
def body(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(GI, 3 + (1 if face == "top" else -2)); continue
            plate_ = y < 9
            if plate_:
                i = 3 + LIT[face] + lightx(face, x, w) + (1 if y == 0 else 0)
                g[y][x] = clamp(GI, i)
            else:                                                         # bare ribs under the plate
                rib = (y % 2 == 0)
                g[y][x] = clamp(BN, (3 if rib else 0) + LIT[face] + (lightx(face, x, w) if face in ("front", "back") else 0))
    if face == "front":
        for y in range(1, 9):                                              # the breastbone ridge and a plate seam
            g[y][w // 2 - 1] = GI[5] if y < 4 else GI[4]; g[y][w // 2] = GI[2]
        for x in range(1, w - 1): g[4][x] = GI[2]
        for y in range(9, h): g[y][w // 2 - 1] = BN[1]                      # the sternum
    if face == "back":
        for y in range(h): g[y][w // 2 - 1] = BN[4] if y >= 9 and y % 2 == 0 else (GI[2] if y < 9 else BN[1])   # spine
    if face not in ("top", "bottom"):
        for x in range(w): g[8][x] = V[1 + (x % 2)]                          # the cuirass hem piped in violet
    return grime(g, GI, ("body", face), 10, 1)

def belt(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 2 + LIT[face] + (1 if y == 0 else 0)) if face not in ("top", "bottom") else GI[2]
    if face == "front":
        for y in range(h):
            for x in (w // 2 - 1, w // 2): g[y][x] = BN[4] if y == 0 else BN[2]   # the bone clasp
        g[0][w // 2 - 1] = g[0][w // 2] = BN[5]
    return g

def cord(face, w, h):
    g = [[V[2 if y % 2 else 1] for x in range(w)] for y in range(h)]
    g[h - 1] = [BN[4]] * w
    return g

# ---- the mantle: two tiers, the lower torn and bound in violet ----
def collar(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 4 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0))
    if face == "top":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None                      # open at the neck for the hood
    if face == "front" or face == "back":
        for x in range(w): g[h - 1][x] = V[1 + (x % 2)]
    return grime(g, GI, ("collar", face), 6, 1)

def cape(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(GI, 3 if face == "top" else 1); continue
            i = 4 + LIT[face] + (lightx(face, x, w) if face in ("front", "back") else 0)
            if (x + y) % 4 == 0: i -= 1                                    # scale-like lames
            if y % 3 == 0: i += 1
            g[y][x] = clamp(GI, i)
    if face not in ("top", "bottom"):
        r = rnd("cape", face)
        for x in range(w):
            g[h - 2][x] = V[1 + (x + 1) % 3 // 2 + 1] if face != "back" else V[2]        # the violet binding
            g[h - 1][x] = GI[1] if r.random() < 0.55 else None            # the ragged hem
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    return g

# ---- the torn shroud down the back and the rags over the thighs ----
def rag(length_cut, key, front=False):
    """A strip of black cloth: folds, a ragged bottom, a violet hem two rows up."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rnd("rag", key, face)
        for y in range(h):
            for x in range(w):
                g[y][x] = sh(SHROUD, 2 if face != "back" else 3, face, x, w, fold=2) if face not in ("top", "bottom") else SHROUD[2]
        if face in ("front", "back"):
            for x in range(w):
                g[h - 2][x] = V[2 if x % 2 else 1]
                depth = r.randrange(0, length_cut + 1)
                for y in range(h - depth, h): g[y][x] = None
                if depth == 0: g[h - 1][x] = SHROUD[1]
            for y in range(h):
                if r.random() < 0.15: g[y][0] = SHROUD[1]
        return g
    return paint

# ---- the team sash: from the right shoulder to the left hip, ends hanging ----
def sash(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("top", "bottom"):
            if face == "top":
                for x in (0, 1): g[1][x] = T[4]
            return g
        def centre(y): return 1 + y * 5.0 / 9          # front: column of the sash's centre at row y
        for y in range(h):
            for x in range(w):
                if face == "front":
                    c = centre(y); d = x - c
                elif face == "back":
                    c = (w - 1) - centre(y); d = x - c
                else:
                    d = None
                if d is not None and -1.5 <= d <= 1.5 and y <= 11:
                    i = 3 + LIT[face] + (1 if d < -0.4 else -1 if d > 0.5 else 0)
                    if abs(d) > 1.2: i = 2
                    g[y][x] = clamp(T, i)
        if face == "right":                                   # round the right side near the shoulder
            for y in range(0, 3):
                for x in range(w): g[y][x] = clamp(T, 3 + (1 if y == 0 else 0))
        if face == "left":                                    # and at the left hip
            for y in range(8, 12):
                for x in range(w): g[y][x] = clamp(T, 3 + (-1 if y == 11 else 0))
        if face == "front":
            for y in (4, 7): g[y][int(centre(y) + 0.5)] = M[2]  # a gilt thread sewn in the sash
        return g
    return paint

def sash_tail(T, M, left):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        r = rnd("tail", face, left)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + (1 if face == "front" else 0) - (1 if x == w - 1 else 0))
        for x in range(w):
            g[h - 1][x] = T[1] if r.random() < 0.5 else None
            g[h - 2][x] = M[1]
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "sash": return sash(T, M)
    return sash_tail(T, M, spec[1])

# ---- assembling ----
def glow(bone, box):
    return skull_lit if bone.name == "head" else None

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 14, 0), [Box((-4, -13, -2), (8, 13, 4), body)], rot=(LEAN, 0, 0)))
    B.append(Bone("head", "body", (0, -13, 0), [Box((-4, -8, -3), (8, 8, 8), skull)], rot=(-LEAN // 2, 0, 0)))
    # the deep hood: opening 6 wide, the skull two px behind its lip; a peak and a drape down the back
    B.append(Bone("deep_hood", "head", tag="helm", boxes=[
        Box((-5, -9, -5), (10, 10, 10), hood),
        Box((-4, -11, -3), (8, 2, 8), peak),
        Box((-3, -13, -1), (6, 2, 6), peak),
        Box((-1, -15, 2), (2, 2, 3), peak)]))
    B.append(Bone("hood_drape", "head", tag="helm", boxes=[Box((-4, 0, 3.5), (8, 6, 1), drape)]))
    for side, x in (("right", -5), ("left", 5)):   # jointed: the elbow at the knuckle (rows 5-6), the hand 4 px under it
        B += limb(f"{side}_arm", "body", (x, -11, 0), (-1, -2, -1), arm(side), f"{side}_forearm", (0, 4, 0), size=(2, 12, 2), rot=(-LEAN, 0, 0))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", (0, 4, 0)))
    B.append(Bone("arms_folded", "body", (0, -10, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -2), ("left", 2)):   # jointed: the knee at the knuckle (rows 6-7), 7 px down the 14 px leg
        B += limb(f"{side}_leg", "root", (x, 14, 0), (-1, 0, -1), leg(side), f"{side}_shin", (0, 7, 0), size=(2, 14, 2), upper_h=7)
    B.append(Bone("right_foot", "right_shin", boxes=[Box((-1.5, 5, -3), (3, 2, 4), foot)]))
    B.append(Bone("left_foot", "left_shin", boxes=[Box((-1.5, 5, -3), (3, 2, 4), foot)]))
    # the mantle: a collar, then a torn cape over the shoulders and upper arms
    B.append(Bone("mantle", "body", boxes=[
        Box((-5, -15, -3), (10, 3, 6), collar, dil=0.2),
        Box((-6, -12.5, -3.5), (12, 4, 7), cape, dil=0.3)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -4, -2), (8, 2, 4), belt, dil=0.45)]))
    B.append(Bone("cord", "body", boxes=[Box((2.5, -3, -2.9), (1, 6, 1), cord)]))
    B.append(Bone("sash", "body", team=True, boxes=[Box((-4, -13, -2), (8, 13, 4), "sash", dil=0.35, sheet="cloth")]))
    B.append(Bone("sash_tails", "body", team=True, boxes=[
        Box((2, -3, -2.8), (2, 7, 0), ("tail", True), sheet="cloth"),
        Box((3.5, -3, -2.6), (2, 5, 0), ("tail", True), sheet="cloth")]))
    # three rag strips over the thighs, three torn shroud strips down the back
    B.append(Bone("rags", "body", boxes=[
        Box((-3.5, -2, -2.5), (3, 11, 0), rag(4, "a")),
        Box((-0.5, -2, -2.7), (3, 13, 0), rag(5, "b")),
        Box((2.5, -2, -2.5), (2, 9, 0), rag(3, "c"))]))
    B.append(Bone("shroud", "body", (0, -12, 2.6), rot=(6, 0, 0), boxes=[
        Box((-5.5, 0, 0), (3, 19, 1), rag(6, "sa")),
        Box((-2, 0, 0.6), (3, 25, 1), rag(7, "sb")),
        Box((1.5, 0, 0.1), (3, 21, 1), rag(5, "sc")),
        Box((4.5, 0, 0.4), (1, 13, 1), rag(3, "sd"))]))
    # tier II, the Harvester: salvaged grave-iron plate piped in violet
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    SP = per_tier(lambda t: gb_spike(GI, 3, 3), (2, 3))
    B.append(Bone("breastplate", "body", stages=[2, 3], boxes=[Box((-3.5, -12, -2.7), (7, 6, 5.4), P2("bpl"), dil=0.15)]))
    for side, sx, rz in (("right", -1, 12), ("left", 1, -12)):
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (0, -0.3, 0), rot=(0, 0, rz), stages=[2, 3], boxes=[
            Box((-3, -3.2, -3.2), (6, 3, 6.4), P2("pd" + side), dil=0.3),
            Box((-0.5 + sx * 1.5, -7.2, -0.5), (1, 4, 1), SP)]))
        B.append(Bone(f"{side}_bracer", f"{side}_forearm", stages=[2, 3], boxes=[Box((-1.8, 0.5, -1.8), (3.6, 3.5, 3.6), P2("br" + side, 0.18), dil=0.1)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[2, 3], boxes=[Box((-1.7, -2.5, -1.7), (3.4, 3, 3.4), P2("gv" + side), dil=0.1)]))
    B.append(Bone("hood_rim", "head", stages=[2, 3], boxes=[Box((-5.3, -1.0, -5.3), (10.6, 1.2, 1.2), gb_band(V, 1, BN), dil=0.05)]))
    # tier III, the Lord of Scythes: gilt crown ring and tines, tall spikes, a gilt collar and a gilt-hemmed shroud
    B.append(Bone("hood_ring", "head", stages=[3], boxes=[Box((-5.3, -9.2, -5.3), (10.6, 1.4, 10.6), gb_gilt("hr"), dil=0.05)]))
    for i, (x, z, h) in enumerate([(-4.2, -4.0, 6), (-1.4, -4.6, 9), (1.4, -4.6, 9), (4.2, -4.0, 6)]):
        B.append(Bone(f"crown_tine{i}", "head", stages=[3], boxes=[Box((x - 0.5, -9.2 - h, z - 0.5), (1, h, 1), gb_spike(GILT, 3, 2))]))
    B.append(Bone("gilt_collar", "body", stages=[3], boxes=[Box((-3.6, -13.4, -2.8), (7.2, 1.6, 5.6), gb_gilt("gc"), dil=0.1)]))
    B.append(Bone("plate_cape", "body", (0, -12, 2.6), rot=(8, 0, 0), stages=[3], boxes=[
        Box((-4, 0.5, 1.2), (8, 4, 0.8), gb_plate(3, "pcp"), dil=0.1)]))
    return B

MAGE_NOTE = ("Graveborn Grave Reaper (unit identity, Graveborn): tall bone wraith, biped, a deep black hood with the skull deep inside and "
             "two soul-light eyes, a grave-iron mantle over a cuirass, a team sash, torn shroud and rags hemmed in violet, bare bone limbs, "
             "a scythe whose 10 px blade rises over the hood. Tier II iron plate, tier III black gilt plate and a crown of tines. No flag on the back.")

REAPER = Troop("reaper", SKIN, CLOTH, bones, cloth_painter,
               {"weapon": "scythe", "hand": "right", "idleArms": "hang", "portrait": "tall 4"}, glow=glow,
               waist={"shroud": "up"})
def build(): return REAPER.build()
def outputs(): return REAPER.outputs()

if __name__ == "__main__":
    house_stub.main(REAPER, MAGE_NOTE, sys.argv)
