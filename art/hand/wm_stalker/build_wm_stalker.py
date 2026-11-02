#!/usr/bin/env python3
"""The Wildmoot Web Stalker (Wildmoot house, troop id wm_stalker), hand-authored at one texel per model pixel.

A wide, low, long-legged spider of the Wildmoot hunt: a dark green-black chitin body with a lit edge, jointed legs banded
pale at the knees and thick with bristles, and a carved white bone mask lashed over the eyes. Silk sacs cluster on the
abdomen, clay warpaint is smeared across it, and a team cloth is tied across the abdomen with ochre cords. No back flag.

Tier I is the Web Stalker. Tier II (Broodstalker) carries more silk sacs, bone ridge plates on the thorax and bone
beads strung on the cords. Tier III (Widowmaker) grows great bone mandibles and a bone crest on the mask, and bears a
ochre hourglass edged in dried blood on the rear of the abdomen's back.

  python build_wm_stalker.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from wildmoot import *   # noqa: F401,F403
import house_stub

SKIN = (128, 128)
CLOTH = (128, 64)
Y0 = 13.8            # thorax pivot height (the vanilla spider's is 15); the longer legs are tuned to reach y = 24

# ---- painters ----
def chitin(base=2, seed="st"):
    """Dark green-black chitin with a lit top and rim, sparse paler mottle and sunk dark pits."""
    def paint(face, w, h):
        g = plate(R=CHITIN, base=base, spec=False)(face, w, h)
        r = rng("st-ch", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                q = r.random()
                if q < 0.07: g[y][x] = clamp(CHITIN, base + LIT[face] + 1)
                elif q < 0.12: g[y][x] = clamp(CHITIN, base + LIT[face] - 1)
        return g
    return paint

def head_painter():
    """The carapace head: chitin with a ridge of lit scutes on top, a shadowed face behind the mask."""
    base = chitin(2, "hd")
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "top":
            for x in range(w): g[h // 2][x] = CHITIN[3]
            for y in range(h): g[y][w // 2 - 1] = g[y][w // 2] = CHITIN[4]
        if face == "front":
            for y in range(h):
                for x in range(w): g[y][x] = CHITIN[1 if (x + y) % 3 else 0]
        return g
    return paint

def thorax_painter():
    base = chitin(2, "th")
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "top":
            for y in range(h):
                g[y][w // 2 - 1] = g[y][w // 2] = CHITIN[4]                       # a lit dorsal ridge
                if y % 2 == 0:
                    for x in (1, w - 2): g[y][x] = CHITIN[3]
        return g
    return paint

def abdomen_painter(tier=1):
    """Segment ridges (a dark groove then a lit lip), clay warpaint smeared across the rear, a dark belly."""
    base = chitin(2, "ab")
    def paint(face, w, h):
        g = base(face, w, h)
        r = rng("st-ab", face, w, h)
        if face == "top":      # w = 12 across, h = 12 deep; row 0 is the rear (the cloth and cords cover rows 5 to 11)
            for x in range(w):
                g[2][x] = CHITIN[0]                                             # a dark groove
                g[3][x] = CHITIN[4 if x not in (0, w - 1) else 3]               # and a lit lip
            for y0 in (0, 4):      # clay bars across the rear back, broken by wear
                for y in (y0, y0 + 1) if y0 == 0 else (y0,):
                    for x in range(1, w - 1):
                        if r.random() < 0.8: g[y][x] = CLAY[3 if y == y0 else 2]
            for x, y in ((2, 3), (3, 3), (8, 3), (9, 3)): g[y][x] = CLAY[2]     # a handprint of dots
            if tier == 3:      # the Widowmaker's mark: an ochre hourglass edged in dried blood across the rear back
                for y, (a, b) in enumerate(((3, 9), (4, 8), (5, 7), (4, 8), (3, 9))):
                    for x in range(a, b): g[y][x] = OCHRE[4 if y in (0, 4) else 3]
                    g[y][a - 1] = BLOOD[1]; g[y][b] = BLOOD[1]
        elif face in ("left", "right"):     # w = 12 deep, h = 7; the rear end is column 0 on the right face, the last on the left
            rear = (lambda x: x) if face == "right" else (lambda x: w - 1 - x)
            for c in (2,):
                for y in range(h): g[y][rear(c)] = CHITIN[0]
            for k in range(2):     # slanting clay slashes down the rear flank
                for t in range(4):
                    g[t][rear(0 + k * 2 + (t + 1) // 2)] = CLAY[3 if t % 2 == 0 else 2]
        elif face == "back":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(CHITIN, 2 + (1 if (x + y) % 5 == 0 else 0) - (1 if y > h - 3 else 0))
            for x in range(3, 9): g[2][x] = CLAY[2]
            g[3][5] = g[3][6] = CLAY[3]
        elif face == "bottom":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(CHITIN, 1 + (1 if (x + y) % 4 == 0 else 0))
        return g
    return paint

def leg_painter(kind, seed):
    """Femur/tibia: chitin, a lit ridge along the top."""
    base = chitin(2 if kind == "femur" else 1, seed)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "top":
            for x in range(w): g[h // 2][x] = clamp(CHITIN, 4 if kind == "femur" else 3)
        return g
    return paint

def band():
    """A pale bone band at a joint."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BONE, 2 + LIT[face] + (1 if y == 0 and face == 'front' else 0))
        return g
    return paint

def claw():
    """The tarsus tip: dark horn."""
    return tone(HORN, 3, "stc", 1)

def bristle():
    """A stiff hair: dark root, bone-tan tip."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BONE, 2) if face in ("top", "bottom") else clamp(BONE, 0 if y >= h - 1 else (2 if y == 0 else 1))
        return g
    return paint

def mask_painter():
    """A carved white bone skull-plate: a brow ridge, four dark eye slits, a nasal notch, a toothy lower edge and two
    ochre-painted lines. The front face is 7 wide x 5 tall."""
    base = bone_paint(4, "mask")
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front" and w == 7:
            for x in range(w): g[0][x] = BONE[5]                                 # lit brow
            for x in (1, 2, 4, 5):                                               # the eye slits
                g[1][x] = INK; g[2][x] = INK if x in (2, 4) else BONE[1]
            g[1][0] = g[1][6] = BONE[3]
            g[2][3] = g[3][3] = BONE[0]                                           # nasal notch
            for x in range(w): g[3][x] = BONE[0] if x == 3 else BONE[4]
            for x in range(w): g[4][x] = BONE[0] if x % 2 == 0 else BONE[3]      # teeth and gaps
            g[2][0] = g[2][6] = BONE[2]
            for y in range(0, 3): g[y][3] = OCHRE[2] if y == 1 else OCHRE[3]     # painted forehead stripe
            g[3][1] = g[3][5] = OCHRE[3]                                          # a smear under each eye
        if face == "top":
            for x in range(w): g[0][x] = BONE[5]
        return g
    return paint

def fang():
    """Venom fang: bone at the root, dark horn at the tip."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = BONE[3] if face in ("top", "bottom") else clamp(BONE, 4 + LIT[face] - (y * 3 // max(1, h)))
        if face not in ("top", "bottom"): g[h - 1][w // 2] = HORN[2]
        return g
    return paint

def palp():
    return tone(CHITIN, 3, "pl", 1)

def sac(seed):
    """A silk egg-sac: bound in thread, a pale lit side, dark pinched pits."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if (x + y) % 3 == 0: v -= 1
                if y == 0 and face not in ("top", "bottom"): v += 1
                g[y][x] = clamp(SILK, v)
        r = rng("st-sac", seed, face, w, h)
        for _ in range(max(1, w * h // 9)):
            g[r.randrange(h)][r.randrange(w)] = SILK[0]
        if face not in ("top", "bottom") and h > 2:
            for x in range(w): g[h // 2][x] = SILK[1]                             # the binding thread
        return g
    return paint

def cord():
    """An ochre cord lashing (skin sheet): bands with a darker twist."""
    base = lashing(OCHRE, 2)
    def paint(face, w, h):
        g = base(face, w, h)
        for y in range(h):
            for x in range(w):
                if (x + 2 * y) % 4 == 0: g[y][x] = OCHRE[1]
        return g
    return paint

def thread():
    """A hanging silk strand."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(SILK, 2 + (1 if y % 2 == 0 else 0) - (1 if y == h - 1 else 0))
        return g
    return paint

def knot():
    return tone(OCHRE, 3, "kn", 1)

# ---- cloth ----
def backcloth(T, M):
    """A heavy team cloth tied across the abdomen: a lit field, vertical folds."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("st-cloth", face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if x % 4 == 2: v -= 1
                if r.random() < 0.06: v -= 1
                g[y][x] = clamp(T, v)
        return g
    return paint

# ---- assembling ----
LEG_ROT = {"left": [(0, 45, 45), (0, 22.5, 33.3), (0, -22.5, 33.3), (0, -45, 45)],
           "right": [(0, -45, -45), (0, -22.5, -33.3), (0, 22.5, -33.3), (0, 45, -45)]}
BEND = 6   # the tibia folds this much further down than the femur

def leg_bones(side, i):
    s = 1 if side == "left" else -1
    n = f"{side}_leg_{i}"
    piv = (4 * s, 0, -1 + (i - 1))
    def X(x0, w):    # box x origin for a stretch [x0, x0+w) measured outward along the leg
        return x0 if s == 1 else -x0 - w
    femur = [Box((X(-1, 8), -1.5, -1.5), (8, 3, 3), leg_painter("femur", f"{n}f")),
             Box((X(6, 1), -2, -2), (1, 4, 4), band())]
    for bx in (1, 4):
        femur.append(Box((X(bx, 1), -3.5, -0.5), (1, 2, 1), bristle()))
    low = [Box((X(0, 8), -1, -1), (8, 2, 2), leg_painter("tibia", f"{n}t")),
           Box((X(5, 1), -1.5, -1.5), (1, 3, 3), band()),
           Box((X(8, 3), -0.5, -0.5), (3, 1, 1), claw()),
           Box((X(3, 1), -2.5, -0.5), (1, 2, 1), bristle())]
    return [Bone(n, "thorax", piv, femur, rot=LEG_ROT[side][i - 1]),
            Bone(n + "_low", n, (7 * s, 0, 0), low, rot=(0, 0, BEND * s))]

def bones():
    B = [Bone("root", "")]
    B.append(Bone("thorax", "root", (0, Y0, 0), [Box((-5, -3, -3), (10, 6, 6), thorax_painter())]))
    B.append(Bone("head", "thorax", (0, 0.5, -3), [Box((-4, -3, -7), (8, 6, 7), head_painter())]))
    B.append(Bone("mask", "head", boxes=[
        Box((-3.5, -2.5, -8), (7, 5, 1), mask_painter()),
        Box((-4, -3.5, -8.5), (8, 1, 2), bone_paint(4, "brow")),
        Box((-4.5, 0, -8), (1, 3, 1), bone_paint(3, "ck")), Box((3.5, 0, -8), (1, 3, 1), bone_paint(3, "ck2"))]))
    B.append(Bone("palps", "head", boxes=[Box((-5, 1, -6), (1, 3, 3), palp()), Box((4, 1, -6), (1, 3, 3), palp())]))
    B.append(Bone("fangs", "head", (0, 2.5, -7), [Box((-2, 0, -1), (4, 1, 1), bone_paint(3, "fb")),
                                                   Box((-2, 1, -1), (1, 3, 1), fang()), Box((1, 1, -1), (1, 3, 1), fang())]))
    B.append(Bone("abdomen", "thorax", (0, 0.5, 2), [Box((-6, -4, 0), (12, 7, 12), per_tier(abdomen_painter, (1, 2, 3)))], rot=(5, 0, 0)))
    B.append(Bone("sacs", "abdomen", boxes=[
        Box((-3, 0, 10), (4, 4, 4), sac("a")), Box((1, -1, 9), (4, 4, 4), sac("b")),
        Box((5, 1, 8), (3, 3, 3), sac("c")), Box((-8, 1, 9), (3, 3, 3), sac("d")),
        Box((-1, 3, 5), (3, 3, 4), sac("e")), Box((-5, 3, 9), (3, 3, 3), sac("f")),
        Box((-2, 4, 12), (1, 5, 1), thread()), Box((2, 3, 12), (1, 6, 1), thread())]))
    B.append(Bone("cords", "abdomen", boxes=[
        Box((-6, -4, 0), (12, 7, 1), cord(), dil=0.5), Box((-6, -4, 6), (12, 7, 1), cord(), dil=0.5),
        Box((-1, -5.5, 5), (2, 2, 2), knot()), Box((-7.5, 0, 1.5), (1, 3, 1), cord()), Box((6.5, 1, 5.5), (1, 3, 1), cord())]))
    B.append(Bone("backcloth", "abdomen", team=True, boxes=[Box((-6, -4, 1), (12, 7, 5), "cloth", dil=0.3, sheet="cloth")]))
    for side in ("left", "right"):
        for i in (1, 2, 3, 4):
            B.extend(leg_bones(side, i))
    # tier II, the Broodstalker: more silk sacs, bone ridge plates on the thorax, bone beads strung on the cords
    B.append(Bone("sacs_ii", "abdomen", stages=[2, 3], boxes=[
        Box((-7.5, -3, 3), (3, 3, 3), sac("g")), Box((4.5, -3, 2), (3, 3, 3), sac("h")), Box((-4, -6, 8), (3, 3, 3), sac("i")),
        Box((3, -5, 9), (3, 3, 3), sac("j")), Box((-5, 2, 12), (1, 4, 1), thread()), Box((4, 3, 11), (1, 4, 1), thread())]))
    B.append(Bone("ridge_plates", "thorax", stages=[2, 3], boxes=[
        Box((-4, -4, -2.5), (2, 1, 5), wm_plate(2, "srp1")), Box((-1, -4.5, -2.5), (2, 2, 5), wm_plate(2, "srp2")),
        Box((2, -4, -2.5), (2, 1, 5), wm_plate(2, "srp3"))]))
    B.append(Bone("cord_beads", "abdomen", stages=[2, 3], boxes=[
        Box((-5, -5, 0), (1, 1, 1), bone_paint(4, "cb1")), Box((-2.5, -5, 0), (1, 1, 1), bone_paint(4, "cb2")),
        Box((1.5, -5, 0), (1, 1, 1), bone_paint(4, "cb3")), Box((4, -5, 0), (1, 1, 1), bone_paint(4, "cb4")),
        Box((-5, -5, 6), (1, 1, 1), bone_paint(4, "cb5")), Box((4, -5, 6), (1, 1, 1), bone_paint(4, "cb6"))]))
    # tier III, the Widowmaker: great bone mandibles, a bone crest on the mask, the blood hourglass on the rear
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"mandible_{side}", "head", (c * 4.5, 2, -7), stages=[3], rot=(18, 0, 0), boxes=[
            Box((-1, -1, -1), (2, 3, 2), bone_paint(3, "mdb" + side)),
            Box((-1, 1, -6), (2, 2, 6), wm_tusk()), Box((-0.5 - c * 1, 1.5, -8), (1, 2, 2), wm_tusk()),
            Box((-0.5 - c * 2, 2, -9), (1, 2, 1), wm_tusk())]))
    B.append(Bone("crest", "head", stages=[3], boxes=[
        Box((-0.5, -9.5, -8.5), (1, 6, 1), wm_tusk()), Box((-2.5, -7.5, -8.3), (1, 4, 1), wm_tusk()), Box((1.5, -7.5, -8.3), (1, 4, 1), wm_tusk()),
        Box((-4.5, -6, -8.1), (1, 2, 1), wm_tusk()), Box((3.5, -6, -8.1), (1, 2, 1), wm_tusk()),
        Box((-4, -4.5, -8.6), (8, 1, 1), lashing(OCHRE, 3), dil=0.05)]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return backcloth(T, M)

NOTE = ("Wildmoot Web Stalker (unit identity, Wildmoot): Rare spider beast. A wide low dark chitin body with a lit edge, long banded "
        "bristled legs, a carved white bone mask lashed over the eyes, silk sacs and clay warpaint on the abdomen, a team cloth tied "
        "across the abdomen with ochre cords. Tier II: more sacs, thorax ridge "
        "plates, cord beads; tier III: bone mandibles, mask crest, ochre hourglass. No back flag.")
WM_STALKER = Troop("wm_stalker", SKIN, CLOTH, bones, cloth_painter,
                   {"rig": "spider", "weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "beast 18 yaw 20"})
def build(): return WM_STALKER.build()
def outputs(): return WM_STALKER.outputs()

if __name__ == "__main__":
    house_stub.main(WM_STALKER, NOTE, sys.argv)
