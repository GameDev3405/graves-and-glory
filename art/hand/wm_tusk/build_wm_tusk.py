#!/usr/bin/env python3
"""The Wildmoot Tusk (Wildmoot house, troop id wm_tusk), hand-authored at one texel per model pixel.

A hunched boar-kin spearman, a zombified piglin of the Wildmoot: a boar snout and big white tusks, a riveted leather cap
under the team headband, a shaggy hide mantle, a spiked leather pauldron over a strapped jerkin, a barry team kilt, team
forearm wraps, bone-and-hide shin guards and dark hooves. He stands bent forward over a crossbarred boar spear. Hide, bone
and wood throughout; the team colour is only the cloth. Tier I only. No back flag.

  python build_wm_tusk.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
from ironclad import scuff, rng, fur, shade   # noqa: E402

SKIN = (128, 128)
CLOTH = (128, 64)
HUNCH = 20   # degrees the body leans forward; the head and arms stand back up (what build_skins bakes for a generated troop)

PIG = ramp("2c1612", "5e332a", "8c5a48", "b3806a", "d6a994")      # zombified piglin: a bruised rosy flesh
HIDE = ramp("1b120c", "3a2619", "5c4029", "82603c", "a98258", "cfae80")
BONE = ramp("33291f", "675b47", "9a8d71", "c9bd9f", "e6dfc8", "f7f3e4")
HORN = ramp("0f0b09", "231915", "3b2c24", "58443a", "7a6252")
WOOD = ramp("1a120c", "36241a", "563a26", "7a5436", "9c7248")

def tone(R, base=3, seed=None, wear=2):
    def paint(face, w, h):
        g = plate(R=R, base=base, spec=False)(face, w, h)
        return scuff(g, (seed, face, w, h), wear, R) if seed else g
    return paint

def leath(base=2, stitch=False, seed="tl"):
    def paint(face, w, h):
        g = leather(base=base, stitch=stitch)(face, w, h)
        return scuff(g, (seed, face, w, h), max(1, w * h // 14), LEATH)
    return paint

def head_painter():
    """A boar's face: heavy brow, small hard eyes, a rosy flesh, cheek stubble, bristle down the back of the skull."""
    S = PIG
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 5 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(S, v)
        if face == "front":
            for x in range(w): g[3][x] = S[2]                                     # the brow ridge
            for x in (1, 2, 5, 6): g[4][x] = S[1]                                # sunk sockets
            g[4][1] = g[4][6] = BONE[5]; g[4][2] = g[4][5] = INK                 # small eyes, a glint at the outside
            g[5][3] = g[5][4] = S[2]
            for x in (0, 1, 6, 7): g[6][x] = S[2]
            for x in range(w): g[h - 1][x] = S[1]
        if face in ("left", "right", "back"):
            for y in range(h):
                for x in range(w):
                    if y < 4 or (face == "back" and y < 7): g[y][x] = clamp(HIDE, 2 + (x + y) % 3 - 1)
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(HIDE, 2 + (x * 3 + y) % 3)
        return scuff(g, ("hd", face), 4, S)
    return paint

def snout():
    def paint(face, w, h):
        g = plate(R=PIG, base=3, spec=False, seam=False)(face, w, h)
        if face == "front":
            for y in range(h):
                for x in range(w): g[y][x] = PIG[4 if (y == 0) else 3]
            g[1][0 + 0] = PIG[0]; g[1][w - 1] = PIG[0]                            # the nostrils
            g[1][1] = PIG[1] if w > 3 else g[1][1]; g[1][w - 2] = PIG[1] if w > 3 else g[1][w - 2]
            for x in range(w): g[h - 1][x] = PIG[1]
        return g
    return paint

def ear():
    def paint(face, w, h):
        g = plate(R=PIG, base=2, spec=False, seam=False)(face, w, h)
        if face in ("front", "back"):
            for y in range(h):
                for x in range(w): g[y][x] = PIG[1 if y == 0 else 2]
        return g
    return paint

def tusk():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 4 + LIT[face] + (1 if y == 0 else 0)
                g[y][x] = clamp(BONE, v + (1 if face in ("top",) else 0))
        for x in range(w): g[h - 1][x] = BONE[3]
        return g
    return paint

def cap():
    """A riveted leather cap: panels with a stitched seam and bone rivets, bound at the brim."""
    def paint(face, w, h):
        g = leather(base=2)(face, w, h)
        if face == "top":
            for y in range(h): g[y][w // 2] = clamp(LEATH, 1)
            return scuff(g, ("cap", face), 4, LEATH)
        for x in range(1, w, 3): g[1][x] = BONE[3]                                    # bone rivets
        for x in range(w): g[h - 1][x] = clamp(LEATH, 1)
        return scuff(g, ("cap", face), 4, LEATH)
    return paint

def mantle():
    return fur(R=HIDE, seed="wm-mantle")

def jerkin():
    """A strapped leather jerkin: dark straps crossing the chest, bone toggles, bare flesh at the throat."""
    def paint(face, w, h):
        g = leather(base=2)(face, w, h)
        if face in ("top", "bottom"): return g
        for y in range(h):
            for x in range(w):
                if face in ("front", "back") and ((x == y // 2 + 1) or (x == w - 2 - y // 2)) and y < 8:
                    g[y][x] = clamp(LEATH, 0 if y % 2 else 1)                          # the crossing straps
        if face == "front":
            for y in range(2, h - 2, 3): g[y][w // 2] = BONE[3]                        # toggles
            g[0][3] = g[0][4] = PIG[2]                                                 # throat flesh
        return scuff(g, ("jk", face), 5, LEATH)
    return paint

def arm_bare():
    """A thick bare arm: rosy flesh, bristle at the elbow."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(PIG, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0))
        return scuff(g, ("ab", face, w), 4, PIG)
    return paint

def pauldron(spiked=True):
    def paint(face, w, h):
        g = leather(base=3)(face, w, h)
        if face in ("front", "back") and w >= 5:
            for x in (1, w - 2): g[0][x] = BONE[3]
        return scuff(g, ("pd", face, w), 3, LEATH)
    return paint

def spike():
    return tone(BONE, 4)

def leg_painter():
    return rows([(0, leath(base=2, seed="tlg")), (7, tone(PIG, 3, "tlp"))])

def shin():
    """A shin guard of hide laced over bone splints."""
    def paint(face, w, h):
        g = leather(base=3)(face, w, h)
        if face in ("front", "back"):
            for y in range(h):
                if y % 2 == 0: g[y][w // 2] = BONE[3]
            for x in range(w): g[h - 1][x] = clamp(LEATH, 1)
        return scuff(g, ("sh", face, w), 3, LEATH)
    return paint

def hoof():
    return tone(HORN, 3, "ho", 3)

def belt():
    def paint(face, w, h):
        g = leather(base=1, stitch=True)(face, w, h)
        if face == "front":
            for y in range(h):
                for x in range(w // 2 - 1, w // 2 + 1): g[y][x] = BONE[3 if y else 4]   # a bone disc buckle
        return g
    return paint

def trophy():
    return tone(BONE, 4)

# ---- cloth ----
def kilt(T, M):
    """A barry kilt: team stripes of two tones, a ragged hem."""
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("top", "bottom"): return g
        r = rng("wm-kilt", face)
        for y in range(h):
            for x in range(w):
                v = (3 if (y // 2) % 2 == 0 else 1) + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(T, v)
        for x in range(w):
            if r.random() < 0.4: g[h - 1][x] = None
        return g
    return paint

def headband(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0))
        if face == "back":
            for x in range(w): g[h - 1][x] = clamp(T, 2)
        return g
    return paint

def wrap_cloth(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (1 if y % 2 == 0 else -1) + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(T, v)
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), jerkin())], rot=(HUNCH, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), head_painter())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("snout", "head", boxes=[Box((-2.5, -3.5, -6), (5, 4, 2), snout())]))
    B.append(Bone("ears", "head", boxes=[Box((-5.5, -8, -1), (1, 3, 2), ear()), Box((4.5, -8, -1), (1, 3, 2), ear())]))
    B.append(Bone("tusks", "head", boxes=[
        Box((-4, -5, -6), (2, 4, 1), tusk()), Box((-4, -7, -6.5), (2, 2, 1), tusk()),
        Box((2, -5, -6), (2, 4, 1), tusk()), Box((2, -7, -6.5), (2, 2, 1), tusk())]))
    B.append(Bone("cap", "head", tag="helm", boxes=[Box((-4.5, -9.5, -4.5), (9, 3, 9), cap())]))
    B.append(Bone("headband", "head", team=True, tag="helm", boxes=[Box((-4.5, -6.5, -4.5), (9, 1, 9), "band", sheet="cloth")]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm_bare())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm_bare())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg_painter())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg_painter())]))
    B.append(Bone("mantle", "body", boxes=[Box((-5, -13.5, -3), (10, 3, 6), mantle(), dil=0.2)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -4, -2), (8, 2, 4), belt(), dil=0.5)]))
    B.append(Bone("trophy", "body", boxes=[Box((1.5, -3, -3), (1, 3, 1), trophy())]))
    B.append(Bone("kilt", "body", team=True, boxes=[Box((-4.5, -3, -2.5), (9, 6, 5), "kilt", sheet="cloth")]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_wrap", f"{side}_arm", team=True, boxes=[Box((-2 + (0 if side == "right" else -1), 5, -2), (4, 4, 4), "wrap", dil=0.3, sheet="cloth")]))
        B.append(Bone(f"{side}_shin", f"{side}_leg", boxes=[Box((-2, 6, -2), (4, 4, 4), shin(), dil=0.3)]))
        B.append(Bone(f"{side}_hoof", f"{side}_leg", boxes=[Box((-2.5, 10, -3), (5, 2, 5), hoof())]))
    B.append(Bone("pauldron", "right_arm", (-1, -2.5, 0), rot=(0, 0, -12), boxes=[Box((-3.5, -2.5, -3.5), (7, 3, 7), pauldron())]))
    B.append(Bone("pauldron_spikes", "right_arm", (-1, -2.5, 0), rot=(0, 0, -12), boxes=[
        Box((-3, -5.5, -0.5), (1, 3, 1), spike()), Box((0, -5.5, -0.5), (1, 3, 1), spike())]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "kilt": return kilt(T, M)
    if spec == "band": return headband(T, M)
    return wrap_cloth(T, M)

NOTE = ("Wildmoot Tusk (unit identity, Wildmoot): zombified piglin, biped, hunched; boar-kin with a snout and big white tusks, a riveted "
        "leather cap under the team headband, a shaggy hide mantle, a spiked leather pauldron over a strapped jerkin, a barry team kilt, "
        "team forearm wraps, shin guards and hooves. The crossbarred boar spear stands over the cap. Tier I only in the first pass. No back flag.")
WM_TUSK = Troop("wm_tusk", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "boar_spear", "hand": "right", "idleArms": "hang", "portrait": "tall 4"}, tiers=(1,))
def build(): return WM_TUSK.build()
def outputs(): return WM_TUSK.outputs()

if __name__ == "__main__":
    house_stub.main(WM_TUSK, NOTE, sys.argv)
