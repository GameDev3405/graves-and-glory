#!/usr/bin/env python3
"""The Hollow King (Graveborn Legendary infantry, troop id gb_hollowking), hand-authored at one texel per model pixel.

The grandest of the six: 36 px to the crown's base and 41 to its spikes, broad as a doorway (the scale of the unit
identity baked into the boxes: body 10x14x5, arms 4x13x4, legs 5x14x5). Black grave-iron plate split open over a soul-fire
ribcage (the mark: the lit ribs, with the broken crown above them, on the glow sheet), a funeral-gilt crown with its band
chipped and its spikes snapped, a bare skull with its jaw bound shut in gilt wire, two soul-lit eyes, great layered
pauldrons with broken spikes, a high gorget, a violet-hemmed rag skirt over a belt with a gilt buckle, greaves slit over
bare bone, and a torn team-cloth cape (three strips under a rolled fold) to the back of the knee. His grave sword
(weapon "grave_sword") is a greatsword. Tier II (the Hollow Lord): the crown mended and raised, violet-piped salvaged plate
on the forearms, thighs and a raised gorget ring, tines on the great pauldrons. Tier III (the Hollow King crowned): a tall
gilt crown of tines, gilt rails and collar bar round the soul-fire ribs, gilt-chased black plate, long pauldron tines and a
fan of bone spikes behind the shoulders, one pale stone in the crown. No flag. The glow sheet is kept within the Legendary cap of 16
texels: 12 ribs, 2 collarbone texels and 2 eyes.

  python build_gb_hollowking.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
import graveborn as GB
from graveborn import VIOLET as V, BONE as BN, GIRON as GI, SOUL, GILT as GL, ROT, gb_plate, gb_spike, gb_gilt, gb_band
import house_stub

SKIN = (128, 128)
CLOTH = (128, 64)
INK_EYE = hx("07060a")
VOID = hx("08090c")

def rnd(*key): return random.Random("hollowking-%s" % (key,))

def sh(R, base, face, x=0, w=0):
    """Ramp index for a texel by face and by column."""
    v = base + LIT[face]
    if face not in ("top", "bottom") and w: v += lightx(face, x, w)
    return clamp(R, v)

def wear(g, R, key, n, step=-1):
    """n texels of the sheet pushed a step along ramp R (scuffs, pits)."""
    r = rnd(*key); h, w = len(g), len(g[0])
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        if g[y][x] is None: continue
        try: i = R.index(g[y][x])
        except ValueError: continue
        g[y][x] = R[max(0, min(len(R) - 1, i + step))]
    return g

def hatch(g, y, x0, x1, a, b):
    for x in range(x0, x1): g[y][x] = a if (x + y) % 2 == 0 else b

# ---- plate: grave-iron with a bright top rim, a dark bottom seam, wear, an optional trim row ----
def iron(base=3, hem=None, trim_top=None, key="iron", worn=5, scuff=1):
    """hem: 'v' (violet cord) or 'g' (gilt) on the last row; trim_top: 'g' gilt on the first row."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                i = LIT[face] + base + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face not in ("top", "bottom"):
                    if y == 0: i += 1
                    if y == h - 1 and h > 2: i -= 1
                g[y][x] = clamp(GI, i)
        if face not in ("top", "bottom"):
            if hem == "v": hatch(g, h - 1, 0, w, V[2], V[1])
            if hem == "g": g[h - 1] = [GL[2 + x % 2] for x in range(w)]
            if trim_top == "g": g[0] = [GL[3] if x % 2 else GL[2] for x in range(w)]
        r = rnd("scuff", key, face)
        for _ in range(scuff):                                        # a pale scratch, 2 texels long, on the bias
            x, y = r.randrange(max(1, w - 1)), r.randrange(max(1, h - 1))
            if g[y][x] is not None and y + 1 < h and x + 1 < w and g[y + 1][x + 1] is not None:
                g[y][x] = clamp(GI, 5); g[y + 1][x + 1] = clamp(GI, 5)
        return wear(g, GI, (key, face), worn, -1)
    return paint

# ---- the skull ----
SKULL = """
bbbbbbbb
bBBBBBBb
BBBBBBBB
BkkBBkkB
BkkBBkkB
BBBkkBBB
BtTtTtTB
dTtTtTtd
"""
def skull(face, w, h):
    p = {"b": BN[2], "B": BN[3], "t": BN[4], "T": BN[1], "d": BN[1], "k": INK_EYE}
    if face == "front":
        g = grid(SKULL, p)
        g[3][1] = g[4][1] = BN[1]; g[3][6] = g[4][6] = BN[1]              # socket rims: deeper on the outer side
        g[2][4] = BN[1]; g[1][3] = BN[4]; g[2][3] = BN[4]                  # a crack down the brow
        g[1][5] = BN[1]
        return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(BN, 2 + (1 if y < 3 else 0) - (1 if y > 5 else 0) + (1 if face == "right" and x < 2 else 0) - (1 if face in ("left", "back") else 0))
    if face in ("top", "bottom"):
        for y in range(h):
            for x in range(w): g[y][x] = clamp(BN, 2 + (1 if (x + y) % 3 == 0 else 0))
    if face in ("left", "right"): g[4][3] = g[4][4] = g[5][3] = BN[0]            # the ear hole
    return wear(g, BN, ("skull", face), 3, -1)

def skull_lit(face, w, h):
    g = blank(w, h)
    if face == "front": g[4][1] = SOUL[3]; g[4][6] = SOUL[3]                  # one soul-light texel burning in each socket
    return g

# ---- the jaw bound in gilt wire: bands over the cheek and under the chin, two verticals, the rest open on the teeth ----
def jaw_wire(face, w, h):
    g = blank(w, h)
    if face in ("top", "bottom"): return g
    for x in range(w):
        g[0][x] = GL[3] if (x + (face == "right")) % 2 == 0 else GL[2]       # the band over the upper teeth
        g[h - 1][x] = GL[2] if x % 2 == 0 else GL[1]                         # the band under the chin
    if face == "front":
        for y in range(1, h - 1): g[y][2] = GL[2]
    if face == "back":                                                       # the knot, wire ends hanging
        for y in range(h): g[y][3] = GL[2]; g[y][4] = GL[1] if y else GL[4]
        g[h - 1][2] = GL[2]; g[h - 1][5] = GL[2]
    return g

# ---- the broken crown ----
def crown_band(face, w, h):
    g = blank(w, h)
    if face == "top":
        for y in range(h):
            for x in range(w):
                if x in (0, w - 1) or y in (0, h - 1): g[y][x] = GL[4] if (x + y) % 2 == 0 else GL[3]
        return g
    if face == "bottom": return g
    for y in range(h):
        for x in range(w):
            g[y][x] = GL[[4, 3, 1][y]]
    for x in range(w):
        if x % 2 == 1: g[1][x] = GL[2]                                         # filigree beads
    if face == "front":
        g[1][4] = V[3]; g[1][3] = V[1]; g[1][5] = V[1]; g[0][4] = V[2]; g[2][4] = V[1]       # a violet stone in a setting
        g[0][7] = g[0][8] = None; g[1][8] = None                                              # a chip bitten from the right
    if face == "back": g[0][2] = g[0][3] = None; g[1][2] = None
    if face == "left": g[2][2] = None; g[2][3] = None
    return wear(g, GL, ("crown", face), 2, -1)

def spike(snapped=False):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = GL[min(4, 1 + y * 3 // max(1, h) + (1 if face in ("front", "right") else 0))] if face not in ("top", "bottom") else GL[3]
        g[0][0] = GL[4]
        if face == "top": g[0][0] = INK_EYE if snapped else GL[4]            # a snapped spike shows a dark break
        return g
    return paint

# ---- body: plate split open over the soul-fire ribcage ----
GAP = {3: (3, 6), 4: (2, 7), 5: (2, 7), 6: (2, 7), 7: (2, 7), 8: (2, 7), 9: (3, 6)}
RIB_ROWS = (4, 6, 8)

def body(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(GI, 3 + (1 if face == "top" else -2)); continue
            g[y][x] = clamp(GI, 3 + LIT[face] + lightx(face, x, w) + (1 if y == 0 else 0))
    if face == "front":
        for y, (a, b) in GAP.items():
            for x in range(a, b + 1):
                g[y][x] = VOID
        for y, (a, b) in GAP.items():                                           # the plate edges curl: a bright rim
            if a - 1 >= 0: g[y][a - 1] = GI[6] if y % 2 == 0 else GI[5]
            if b + 1 < w: g[y][b + 1] = GI[5] if y % 2 == 0 else GI[4]
        for x in range(3, 7): g[2][x] = GI[1]                                    # the gorget's shadow
        for y in range(3, 10):
            for x in (4, 5): g[y][x] = BN[3] if x == 4 else BN[2]                # the sternum, bare bone
            if y in RIB_ROWS:
                for x in (2, 3, 6, 7):
                    if GAP[y][0] <= x <= GAP[y][1]: g[y][x] = SOUL[2] if x in (3, 6) else SOUL[1]
            else:                                                                # soul-fire in the cavity between the ribs
                for x in (2, 3, 6, 7):
                    if GAP[y][0] <= x <= GAP[y][1]: g[y][x] = SOUL[0]
        g[3][3] = g[3][6] = SOUL[1]
        g[10][0] = g[10][1] = None; g[11][9] = None                              # a split plate hangs ragged
        for y in (3, 5, 7):                                                      # rivets down both plates
            g[y][0] = GL[2]; g[y][9] = GL[2]
    if face == "back":
        for y in range(h): g[y][w // 2 - 1] = GI[5] if y < 10 else GI[2]; g[y][w // 2] = GI[2]      # the spine ridge
        for y in range(3, 11): hatch(g, y, 2, 4, V[2], V[3]); hatch(g, y, 6, 8, V[3], V[2])         # lacing across the back plate
    if face in ("left", "right"):
        for y in range(3, 11): g[y][1 if face == "left" else 3] = GI[5]
    return wear(g, GI, ("body", face), 10, 1)

def belt(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 3 + LIT[face] + (1 if y == 0 else 0) + (1 if x in (0, w - 1) else 0)) if face not in ("top", "bottom") else GI[2]
    if face == "front":                                                          # a gilt buckle on a violet-corded strap
        for y in range(h): g[y][4] = GL[4] if y == 0 else GL[2]; g[y][5] = GL[3] if y == 0 else GL[1]
        g[1][4] = g[1][5] = SOUL[0]
        for x in (1, 2, 7, 8): g[1][x] = V[2 + x % 2 - (1 if x > 5 else 0)]
    if face in ("left", "right", "back"):
        for x in range(w): g[1][x] = V[1 + (x % 3 == 0) + (x % 2)]
    return g

def tasset(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y in (0, 2, 4) else 0))
    if face not in ("top", "bottom"):
        hatch(g, h - 1, 0, w, V[2], V[1])
        if face == "front": g[0][0] = g[0][w - 1] = GL[2]
    return wear(g, GI, ("tas", face, w, h), 3, 1)

# ---- the gorget and the high back collar ----
def gorget(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 4 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0))
    if face == "top":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face in ("front", "left", "right"):
        g[h - 1] = [GL[3] if x % 2 else GL[2] for x in range(w)]
    return wear(g, GI, ("gorget", face), 4, 1)

def collar_back(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0))
    if face in ("front", "back"):
        for x in range(w):
            for y in range((0, 2, 1, 3, 3, 1, 2, 0)[x % 8]): g[y][x] = None      # a jagged crest
        hatch(g, h - 1, 0, w, V[2], V[3])
    return g

def clasp(face, w, h):
    g = [[GL[3 if (x + y) % 2 else 4] for x in range(w)] for y in range(h)]
    if face == "front": g[h // 2][w // 2] = V[2]
    return g

# ---- the pauldrons: layered lames, broken spikes, a violet-corded rim, a gilt trim ----
def lame(base, trim="g", key="lame"):
    def paint(face, w, h):
        g = iron(base, hem="v" if trim == "v" else None, key=key, worn=4, scuff=2)(face, w, h)
        if face not in ("top", "bottom") and trim == "g":
            g[h - 1] = [GL[1 + (x % 2)] for x in range(w)]
        if face == "top":
            for x in range(w): g[0][x] = GI[6]; g[h - 1][x] = GI[5]
            for y in range(h): g[y][0] = GI[6]; g[y][w - 1] = GI[5]
        return g
    return paint

def shard(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 3 + LIT[face] + (1 if y < 2 else 0)) if face not in ("top", "bottom") else GI[5]
    if face not in ("top", "bottom"):
        g[0][0] = GI[6]
        if h > 3: g[h - 1][0] = GL[1]                                             # a gilt stain at the root
    return g

# ---- limbs ----
def arm(side):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = clamp(GI, 3 if face == "top" else 1); continue
                if y <= 4 or y >= 7:
                    i = 3 + LIT[face] + lightx(face, x, w) + (1 if y in (0, 7) else 0) - (1 if y in (4, 12) else 0)
                    g[y][x] = clamp(GI, i)
                else:                                                            # bare bone at the elbow
                    g[y][x] = clamp(BN, 2 + LIT[face] + lightx(face, x, w) - (2 if y == 5 else 0))
        if face not in ("top", "bottom"):
            hatch(g, 11, 0, w, V[3], V[2]); hatch(g, 12, 0, w, V[2], V[1])                 # the wrist bound in violet cord
            hatch(g, 4, 0, w, V[2], V[1])
        return wear(g, GI, ("arm", side, face), 5, -1)
    return paint

def leg(side):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = clamp(GI, 3 if face == "top" else 1); continue
                i = 3 + LIT[face] + lightx(face, x, w)
                if y == 0 or y == 8: i += 1
                if y in (5, 7, 11): i -= 1
                g[y][x] = clamp(GI, i)
                if y == 6 or (face == "front" and 9 <= y <= 11 and x == w // 2):          # bare bone at the knee, a slit in the greave
                    g[y][x] = clamp(BN, 2 + LIT[face] + lightx(face, x, w))
        if face not in ("top", "bottom"):
            hatch(g, 12, 0, w, V[3], V[2]); hatch(g, 13, 0, w, V[2], V[1])                 # the ankle bound in violet cord
        return wear(g, GI, ("leg", side, face), 3, -1)
    return paint

def knee(face, w, h):
    g = iron(4, key="knee", worn=3, scuff=1)(face, w, h)
    if face == "front": g[h // 2][w // 2] = GL[3]
    return g

def boot(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 2 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0)) if face != "bottom" else GI[0]
    return wear(g, GI, ("boot", face), 4, 1)

# ---- the rag skirt: rotted cloth in strips, each hem bound in violet ----
def rag(length_cut, key):
    def paint(face, w, h):
        g = blank(w, h)
        r = rnd("rag", key, face)
        for y in range(h):
            for x in range(w):
                g[y][x] = sh(ROT, 4 if face != "back" else 3, face, x, w) if face not in ("top", "bottom") else ROT[2]
                if face in ("front", "back") and x % 3 == 2: g[y][x] = clamp(ROT, ROT.index(g[y][x]) - 1)
        if face in ("front", "back"):
            for x in range(w):
                g[h - 2][x] = V[2 if x % 2 else 3]
                depth = r.randrange(0, length_cut + 1)
                for y in range(h - depth, h): g[y][x] = None
                if depth == 0: g[h - 1][x] = V[1]
        return g
    return paint

# ---- the cape: three team-cloth strips to the back of the knee under a rolled fold (model fix 2026-11-13, Kort's pick A) ----
def cape_strip(T, key, hem):
    """A cape strip: lit outer face, darker lining, a fold every third column, a torn hem `hem` px deep at most, moth holes."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rnd("cape", key, face)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = T[2]; continue
                i = 3 if face == "back" else 1
                if (x + (1 if key == "b" else 0)) % 3 == 2: i -= 1
                if y < 2 and face == "back": i += 1                              # light across the shoulders
                if y > h - hem - 3: i -= 1                                       # the hem darkens with grave dirt
                g[y][x] = clamp(T, i)
        if face in ("front", "back", "left", "right"):
            for x in range(w):
                cut = r.choice((0, 1, 1, 2, 3, hem))
                for y in range(h - cut, h): g[y][x] = None
            if face in ("front", "back"):
                for _ in range(max(1, w // 3)):
                    if r.random() < 0.6: g[r.randrange(h // 2, h - 2)][r.randrange(w)] = None
        return g
    return paint

def cape_roll(T, key):
    """The rolled fold across the cape's top: lit top band, a darker fold every fourth column, a few torn points."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rnd("caperoll", key, face)
        for y in range(h):
            for x in range(w):
                i = 3 if face in ("back", "top") else 2
                if face not in ("top", "bottom") and y == 0: i += 1
                if face in ("back", "front") and x % 4 == 3: i -= 1
                g[y][x] = clamp(T, i)
        if face in ("front", "back", "left", "right"):
            for x in range(w):
                cut = r.choice((0, 0, 1, 2))
                for y in range(h - cut, h): g[y][x] = None
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return cape_strip(T, spec[1], spec[2]) if spec[0] == "strip" else cape_roll(T, spec[1])

# ---- assembling ----
def glow(bone, box):
    if bone.name == "head" and box.size == [8, 8, 8]: return skull_lit
    if bone.name == "body" and box.size == [10, 14, 5]:
        def p(face, w, h):
            g = blank(w, h)
            if face == "front":
                for y in RIB_ROWS:
                    for x in (2, 3, 6, 7): g[y][x] = SOUL[2]
                g[3][3] = g[3][6] = SOUL[2]
            return g
        return p
    return None

CAPE_TILT, CAPE_B = -12, 15   # the cape hangs back off the shoulders; the centre strip is the longest

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 14, 0), [Box((-5, -14, -2.5), (10, 14, 5), body)]))
    B.append(Bone("head", "body", (0, -14, 0), [Box((-4, -8, -4), (8, 8, 8), skull)]))
    B.append(Bone("jaw_wire", "head", tag="helm", boxes=[Box((-4, -3, -4), (8, 3, 8), jaw_wire, dil=0.25)]))
    # the broken crown: a band chipped at the front, spikes of every height, one snapped, one bent over
    spikes = [(-3.5, -4.5, 5, 0), (0, -4.5, 3, 1), (2.5, -4.5, 6, 0), (3.5, -0.5, 2, 1), (-1, 3.5, 5, 0), (2.5, 3.5, 3, 0), (-4.5, 0, 4, 0)]
    crown = [Box((-4.5, -9, -4.5), (9, 3, 9), crown_band)]
    for x, z, hgt, snapped in spikes:
        crown.append(Box((x, -9 - hgt, z), (1, hgt, 1), spike(bool(snapped))))
    B.append(Bone("crown", "head", tag="helm", boxes=crown))
    B.append(Bone("crown_bent", "head", (-1, -10, -4.5), tag="helm", rot=(-50, 0, 0), boxes=[Box((-0.5, -4, -0.5), (1, 4, 1), spike())]))
    B.append(Bone("gorget", "body", boxes=[Box((-5.5, -14.5, -3.2), (11, 3, 6.4), gorget)]))
    B.append(Bone("collar_back", "body", boxes=[Box((-4, -19, 1.2), (8, 6, 1), collar_back)]))
    B.append(Bone("clasp", "body", boxes=[Box((-3.5, -13, -3.6), (2, 2, 1), clasp), Box((1.5, -13, -3.6), (2, 2, 1), clasp)]))
    # the arms: plate over bare bone, a broken spike at each elbow, great layered pauldrons
    for side, x in (("right", -7), ("left", 7)):   # jointed: the elbow at the bare-bone rows, 4 px down; the hand 5 px under it
        B += limb(f"{side}_arm", "body", (x, -12, 0), (-2, -2, -2), arm(side), f"{side}_forearm", (0, 4, 0), size=(4, 13, 4))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", (0, 5, 0)))
    B.append(Bone("arms_folded", "body", (0, -11, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_elbow", "right_forearm", boxes=[Box((-1, 0, 1.8), (2, 3, 2), shard)]))
    B.append(Bone("left_elbow", "left_forearm", boxes=[Box((-1, 0, 1.8), (2, 3, 2), shard)]))
    B.append(Bone("right_pauldron", "right_arm", (0, -2, 0), rot=(0, 0, 14), boxes=[
        Box((-5, -2, -3.8), (9, 3, 7.6), lame(4, "g", "rp0"), dil=0.2),
        Box((-4.5, 1, -3.4), (8, 2, 6.8), lame(3, "v", "rp1"), dil=0.2),
        Box((-4, -5, -1), (4, 3, 2), shard), Box((-6, -1, -0.5), (1, 3, 1), shard)]))
    B.append(Bone("left_pauldron", "left_arm", (0, -2, 0), rot=(0, 0, -14), boxes=[
        Box((-4, -2.5, -4.2), (9, 3, 8.4), lame(4, "g", "lp0"), dil=0.2),
        Box((-3.5, 0.5, -3.8), (8, 2, 7.6), lame(3, "n", "lp1"), dil=0.2),
        Box((-3.5, 2.5, -3.4), (7, 2, 6.8), lame(3, "v", "lp2"), dil=0.2),
        Box((-1.5, -9, -1.5), (2, 6, 3), shard), Box((-0.5, -12, -0.5), (1, 3, 1), shard), Box((4, -2, -1), (1, 4, 2), shard)]))
    # legs: cuisse, bare knee, slit greave, sabaton; a knee-cop with a spike
    for side, x in (("right", -2.5), ("left", 2.5)):   # jointed: the knee at mid-height, 7 px down the leg
        B += limb(f"{side}_leg", "root", (x, 14, 0), (-2.5, 0, -2.5), leg(side), f"{side}_shin", (0, 7, 0), size=(5, 14, 5), upper_h=7)
    for s in ("right", "left"):
        B.append(Bone(f"{s}_knee", f"{s}_shin", boxes=[Box((-2.5, -2.5, -3.8), (5, 4, 2), knee, dil=0.2), Box((-0.5, -1.5, -5.8), (1, 2, 2), shard)]))
        B.append(Bone(f"{s}_foot", f"{s}_shin", boxes=[Box((-3, 4.5, -4.5), (6, 3, 8), boot, dil=0.2)]))
    # belt, hip guards and the rag skirt
    B.append(Bone("belt", "body", boxes=[Box((-5, -4, -2.5), (10, 3, 5), belt, dil=0.45)]))
    B.append(Bone("hip_r", "body", (-5.5, -2, 0), rot=(0, 0, 8), boxes=[Box((-1, 0, -3), (2, 7, 6), tasset, dil=0.1)]))
    B.append(Bone("hip_l", "body", (5.5, -2, 0), rot=(0, 0, -8), boxes=[Box((-1, 0, -3), (2, 6, 6), tasset, dil=0.1)]))
    # the rag skirt hangs in three groups (model fix 2026-11-13), each hinged on the belt line at its own strip's top: rags_r
    # over the right thigh, rags_c between the thighs, rags_l over the left, so each can trail its own leg. Every strip keeps
    # its box, paint and place at rest (the boxes are the old ones, moved into their bone's frame).
    for name, x, z, ln, cut, key in (("rags_r", -3, -3.4, 12, 4, "a"), ("rags_c", 0, -3.6, 14, 5, "b"), ("rags_l", 3, -3.4, 10, 3, "c")):
        B.append(Bone(name, "body", (x, -2, z), boxes=[Box((-1.5, 0, 0), (3, ln, 0), rag(cut, key))]))
    # the cape (model fix 2026-11-13, Kort's pick A): a mount under the pauldrons, which troop_rig turns into
    # the "cape" container the idle, walk, run, attack and death clips key; three strips named cloak_a/b/c (keyed by the attack
    # and hit clips) hang from it to the back of the knee, flared a little outward. Integer widths keep every texel row.
    # The mount is named plain "collar" (troop_rig hangs it in the "cape" container just as it does cape*) so that it is a
    # rigid part of the container, not a separate piece of cloth; it carries the rolled fold across the top.
    B.append(Bone("collar", "body", (0, -10, 3.8), team=True, rot=(CAPE_TILT, 0, 0),
                  boxes=[Box((-2.5, -1.5, -0.5), (5, 2, 2), ("roll", "roll"), sheet="cloth")]))
    for key, x0, w, ln, flare in (("a", -5, 3, 13, 4), ("b", -2, 4, CAPE_B, 0), ("c", 2, 3, 12, -4)):
        B.append(Bone(f"cloak_{key}", "collar", (x0 + w / 2, 0, 0), team=True, rot=(0, 0, flare), boxes=[
            Box((-w / 2, 0, 0), (w, ln, 1), ("strip", key, 4), sheet="cloth")]))
    # tier II, the Hollow Lord: the crown mended and raised, salvaged plate piped in violet, tines on the pauldrons
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    TN = per_tier(lambda t: gb_spike(GI, 4, 3), (2, 3))
    for i, (x, z, hgt) in enumerate([(-3.5, -3.5, 8), (0.0, -4.5, 10), (3.0, -3.5, 8)]):
        B.append(Bone(f"crown_tall{i}", "head", stages=[2, 3], boxes=[Box((x - 0.5, -9 - hgt, z - 0.5), (1.4, hgt, 1.4), per_tier(lambda t: gb_spike(GL, 3, 3), (2, 3)))]))
    B.append(Bone("gorget_ring", "body", stages=[2, 3], boxes=[Box((-6, -16, -3.7), (12, 2, 7.4), P2("gr"), dil=0.3)]))
    for side in ("right", "left"):
        B.append(Bone(f"{side}_vambrace", f"{side}_forearm", stages=[2, 3], boxes=[Box((-2.5, 0.5, -2.5), (5, 4, 5), P2("vb" + side, 0.18), dil=0.3)]))
        B.append(Bone(f"{side}_cuisse", f"{side}_leg", stages=[2, 3], boxes=[Box((-2.5, 0.5, -2.5), (5, 4, 5), P2("cu" + side), dil=0.35)]))
    B.append(Bone("tine_r", "right_arm", (0, -2, 0), rot=(0, 0, 14), stages=[2, 3], boxes=[Box((-5, -9, -0.5), (1.4, 7, 1.4), TN), Box((-2.5, -7, 2), (1.4, 5, 1.4), TN)]))
    B.append(Bone("tine_l", "left_arm", (0, -2, 0), rot=(0, 0, -14), stages=[2, 3], boxes=[Box((3.6, -9, -0.5), (1.4, 7, 1.4), TN), Box((1.1, -7, 2), (1.4, 5, 1.4), TN)]))
    # tier III, the Hollow King crowned: gilt rails and a collar bar round the ribs, tall gilt tines, a spine fan, a pale stone
    B.append(Bone("crown_halo", "head", stages=[3], boxes=[Box((-5, -10.5, -5), (10, 1.5, 10), gb_gilt("ch"), dil=0.05)]))
    for i, (x, z, hgt) in enumerate([(-5.0, -1.5, 9), (4.0, -1.5, 9), (-4.5, 3.0, 11), (3.5, 3.0, 11), (-0.5, 4.5, 13)]):
        B.append(Bone(f"crown_spire{i}", "head", stages=[3], boxes=[Box((x - 0.5, -10.5 - hgt, z - 0.5), (1.4, hgt, 1.4), gb_spike(GL, 4, 3))]))
    B.append(Bone("crown_stone", "head", stages=[3], boxes=[Box((-0.5, -8.8, -5.2), (1, 1, 0.7), lambda f, w, h: [[SOUL[2]]])]))
    B.append(Bone("collar_bar", "body", stages=[3], boxes=[Box((-4.5, -13.3, -3.5), (9, 1.2, 0.8), gb_gilt("cb"), dil=0.1)]))
    for sx, nm in ((-1, "r"), (1, "l")):
        B.append(Bone(f"rib_rail_{nm}", "body", stages=[3], boxes=[Box((sx * 5.2 - 0.5, -13, -3.4), (1, 9, 1), gb_gilt("rr" + nm), dil=0.1)]))
        B.append(Bone(f"spine_fan_{nm}", "body", (sx * 2.5, -13, 3.2), rot=(10, 0, sx * -24), stages=[3], boxes=[Box((-0.7, -11, 0), (1.4, 11, 1.4), gb_spike(BN, 4, 3))]))
    B.append(Bone("spine_fan_c", "body", (0, -13, 3.2), rot=(10, 0, 0), stages=[3], boxes=[Box((-0.7, -13, 0), (1.4, 13, 1.4), gb_spike(BN, 4, 3))]))
    return B

NOTE = ("Graveborn Hollow King (unit identity, Graveborn Legendary): the grandest of the six, tall and broad (body 10x14x5, arms 4x13x4, legs "
        "5x14x5): black grave-iron plate split open over a lit soul-fire ribcage, a broken funeral-gilt crown with a violet stone, a bare "
        "skull with its jaw bound in gilt wire and soul-lit eyes, layered pauldrons with broken spikes, a high gorget, a violet-hemmed rag "
        "skirt, slit greaves over bare bone and a torn team-cloth cape to the back of the knee. Carries the grave sword. Tier II mended crown and "
        "salvaged plate, tier III tall gilt crown, gilt rails and a spine fan; no flag. Glow sheet 16 texels (ribs and eyes).")

HOLLOWKING = Troop("gb_hollowking", SKIN, CLOTH, bones, cloth_painter,
                   {"weapon": "grave_sword", "hand": "right", "idleArms": "hang", "portrait": "tall 4"}, glow=glow)
def build(): return HOLLOWKING.build()
def outputs(): return HOLLOWKING.outputs()

if __name__ == "__main__":
    house_stub.main(HOLLOWKING, NOTE, sys.argv)
