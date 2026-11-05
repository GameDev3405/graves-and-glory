#!/usr/bin/env python3
"""The Wildmoot Hide Hunter (Wildmoot house, troop id wm_hunter), hand-authored at one texel per model pixel.

A lean hunched boar-kin archer: floppy ears, small tusks, clay brow stripes, a leather cap with an ochre-stitched brim and a
three-feather fan bound on the wearer's left, a hide cloak and a quiver down the back, a team jerkin, an ochre-bound bracer
and the order bow in the left hand. Hide, bone and wood; team colour is only the jerkin. No back flag.

Tier I is the hide hunter. Tier II (Thornstalker) adds a bone plate over the cap's brow, a thorn-spiked hide pauldron,
a second bracer and a bone rim on the quiver. Tier III (Elder Hunter) swaps the three-feather fan for five ochre-barred
feathers, and adds a grey pelt mantle, a tusk necklace and ochre bands down the quiver.

  python build_wm_hunter.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from wildmoot import *   # noqa: F401,F403
import house_stub

SKIN = (128, 128)
CLOTH = (128, 64)
HUNCH = 20

def head_painter():
    """A boar's face as the Tusk's, with clay brow stripes and cheek dashes."""
    S = PIG
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 5 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(S, v)
        if face == "front":
            for x in range(w): g[3][x] = S[2]
            for x in (1, 2, 5, 6): g[4][x] = S[1]
            g[4][1] = g[4][6] = BONE[5]; g[4][2] = g[4][5] = INK
            g[5][3] = g[5][4] = S[2]
            for x in (0, 1, 6, 7): g[6][x] = S[2]
            for x in range(w): g[h - 1][x] = S[1]
            clay_stripes(g, [(1, 3), (2, 3), (5, 3), (6, 3), (0, 5), (1, 6), (6, 6), (7, 5)], CLAY, 3)
        if face in ("left", "right", "back"):
            for y in range(h):
                for x in range(w):
                    if y < 4 or (face == "back" and y < 7): g[y][x] = clamp(HIDE, 2 + (x + y) % 3 - 1)
            if face in ("left", "right"): clay_stripes(g, [(1, 5), (2, 5)], CLAY, 3)
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(HIDE, 2 + (x * 3 + y) % 3)
        return scuff(g, ("hhd", face), 4, S)
    return paint

def snout():
    def paint(face, w, h):
        g = plate(R=PIG, base=3, spec=False, seam=False)(face, w, h)
        if face == "front":
            for y in range(h):
                for x in range(w): g[y][x] = PIG[4 if y == 0 else 3]
            g[1][0] = PIG[0]; g[1][w - 1] = PIG[0]
            if w > 3: g[1][1] = PIG[1]; g[1][w - 2] = PIG[1]
            for x in range(w): g[h - 1][x] = PIG[1]
        return g
    return paint

def ear():
    """A floppy ear: dark rim, rosy inside."""
    def paint(face, w, h):
        g = flat(PIG, 2)(face, w, h)
        for x in range(w): g[h - 1][x] = PIG[1]
        return g
    return paint

def tuskp():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(BONE, 4 + LIT[face] + (1 if y == 0 else 0))
        return g
    return paint

def cap():
    """Leather cap with a stitched ochre brim line."""
    def paint(face, w, h):
        g = leather(base=2)(face, w, h)
        if face == "top":
            for y in range(h): g[y][w // 2] = clamp(LEATH, 1)
            return scuff(g, ("hcap", face), 4, LEATH)
        for x in range(w): g[h - 1][x] = clamp(LEATH, 1)
        for x in range(0, w, 2): g[h - 2][x] = OCHRE[3]
        return scuff(g, ("hcap", face), 3, LEATH)
    return paint

def brim():
    def paint(face, w, h):
        g = leath(base=1, seed="hbrim")(face, w, h)
        if face == "front":
            for x in range(0, w, 2): g[0][x] = OCHRE[3]
        return g
    return paint

def feather(bar=None):
    """A 2-wide vane: bone-white quill and body, dark barred tip (bar=OCHRE dips the bars in the House accent)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 4 + LIT[face] + (1 if x == 0 and face in ("front", "back") else 0) + (1 if y > 2 else 0)
                g[y][x] = clamp(BONE, v)
                if y in (0, 2) and face not in ("top", "bottom"): g[y][x] = (bar or HORN)[2 if y == 0 else 3]
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = HORN[2]
        return g
    return paint

def fletch():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BONE, 4 + LIT[face] + (1 if y > 0 else 0))
                if y == 0: g[y][x] = HORN[2]
        return g
    return paint

def jerkin():
    """The hide body under the team jerkin: bare flesh at the throat, ochre toggles."""
    def paint(face, w, h):
        g = leather(base=2)(face, w, h)
        if face in ("top", "bottom"): return g
        if face == "front":
            for y in range(2, h - 2, 3): g[y][w // 2] = OCHRE[3]
            g[0][3] = g[0][4] = PIG[2]; g[1][3] = g[1][4] = PIG[1]
        return scuff(g, ("hjk", face), 5, LEATH)
    return paint

def cloak():
    return pelt(HIDE, "wm-hcloak")

def collar():
    return pelt(HIDE, "wm-hcollar")

def quiver(tier=1):
    def paint(face, w, h):
        g = leath(base=4, seed="hq")(face, w, h)
        if face in ("front", "back", "left", "right") and h > 6:
            for x in range(w): g[h - 3][x] = OCHRE[3]                  # an ochre band
            for x in range(w): g[2][x] = clamp(LEATH, 1)
            if tier == 3:                                              # the Elder's quiver: two more ochre bands
                for x in range(w): g[h - 5][x] = OCHRE[3]; g[4][x] = OCHRE[3]
        return g
    return paint

def rim():
    return leath(base=1, seed="hqr")

def arm_bare():
    return flat(PIG, 3, "hab", 4)

def bracer():
    def paint(face, w, h):
        g = leather(base=3, stitch=True)(face, w, h)
        if face not in ("top", "bottom"):
            for y in range(1, h, 2): g[y][w // 2] = OCHRE[3]
            for x in range(0, w, 2): g[0][x] = OCHRE[3]
        return scuff(g, ("hbr", face), 2, LEATH)
    return paint

def cap_plate(tier):
    """A bone plate over the cap's brow: pale bone, a dark seam down the middle, ochre pegs at the corners; ochre dots at III."""
    def paint(face, w, h):
        g = bone_paint(4, "hcp")(face, w, h)
        if face == "front":
            for y in range(h): g[y][w // 2] = BONE[1]
            g[h - 1][0] = g[h - 1][w - 1] = OCHRE[3]
            if tier == 3: g[0] = [OCHRE[4] if x % 2 == 0 else BONE[5] for x in range(w)]
        return g
    return paint

def leg_painter():
    return rows([(0, leath(base=2, seed="hlg")), (7, tone(PIG, 3, "hlp"))])

def bind():
    return lashing()

def hoof():
    return tone(HORN, 3, "hho", 3)

def belt():
    def paint(face, w, h):
        g = leather(base=1, stitch=True)(face, w, h)
        if face == "front":
            for y in range(h): g[y][w // 2] = BONE[3 if y else 4]
        return g
    return paint

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), jerkin())], rot=(HUNCH, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), head_painter())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("snout", "head", boxes=[Box((-2.5, -3.5, -6), (5, 4, 2), snout())]))
    B.append(Bone("ear_r", "head", pivot=(-4, -6, 0), rot=(0, 0, 14), boxes=[Box((-1, 0, -1.5), (1, 5, 3), ear())]))
    B.append(Bone("ear_l", "head", pivot=(4, -6, 0), rot=(0, 0, -14), boxes=[Box((0, 0, -1.5), (1, 5, 3), ear())]))
    B.append(Bone("tusks", "head", boxes=[Box((-3.5, -2, -6), (1, 2, 1), tuskp()), Box((2.5, -2, -6), (1, 2, 1), tuskp())]))
    B.append(Bone("cap", "head", tag="helm", boxes=[Box((-4.5, -9.5, -4.5), (9, 3, 9), cap())]))
    B.append(Bone("brim", "head", tag="helm", boxes=[Box((-4, -7.5, -5.5), (8, 1, 1), brim())]))
    B.append(Bone("feather_bind", "head", boxes=[Box((1.5, -9.5, -1.5), (3, 2, 3), bind(), dil=0.2)]))
    for i, (rot, zz) in enumerate((((0, 0, -8), -1), ((-8, 0, -30), 0), ((-16, 0, -52), 1))):
        B.append(Bone("feather%d" % i, "head", pivot=(3, -9, zz), rot=rot, stages=[1, 2], boxes=[Box((-1, -7, -0.5), (2, 7, 1), feather())]))
    for i, (rot, zz) in enumerate((((0, 0, 6), -1.5), ((-4, 0, -12), -0.8), ((-8, 0, -30), 0), ((-12, 0, -48), 0.8), ((-16, 0, -66), 1.5))):
        B.append(Bone("feather_fan_5_%d" % i, "head", pivot=(3, -9, zz), rot=rot, stages=[3],
                      boxes=[Box((-1, -8 - (i % 2), -0.5), (2, 8 + (i % 2), 1), feather(OCHRE))]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms(), the arm standing back up
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm_bare(), f"{side}_forearm", ELBOW[side], rot=(-HUNCH, 0, 0))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg_painter(), f"{side}_shin", KNEE)
    B.append(Bone("collar", "body", boxes=[Box((-4.5, -13, -2.5), (9, 2, 5), collar(), dil=0.1)]))
    B.append(Bone("cloak", "body", boxes=[Box((-4.5, -12, 2), (9, 13, 1), cloak(), dil=0.1)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), belt(), dil=0.5)]))
    B.append(Bone("jerkin", "body", team=True, boxes=[Box((-4.5, -11, -2.5), (9, 7, 5), "jerkin", sheet="cloth")]))
    B.append(Bone("quiver", "body", pivot=(-1.5, -6, 3.5), rot=(0, 0, -16), boxes=[
        Box((-1.5, -4.5, -1), (3, 9, 2), per_tier(quiver, (1, 2, 3))), Box((-2, -5, -1.5), (4, 1, 3), rim()),
        Box((-1.5, -8, -0.5), (1, 3, 1), fletch()), Box((-0.5, -9, -0.5), (1, 4, 1), fletch()), Box((0.5, -8, -0.5), (1, 3, 1), fletch())]))
    B.append(Bone("left_bracer", "left_forearm", boxes=[Box((-2, 1, -2), (4, 4, 4), bracer(), dil=0.3)]))
    for side in ("right", "left"):
        B.append(Bone("%s_bind" % side, "%s_shin" % side, boxes=[Box((-2, 0, -2), (4, 1, 4), bind(), dil=0.3)]))
        B.append(Bone("%s_hoof" % side, "%s_shin" % side, boxes=[Box((-2.5, 4, -3), (5, 2, 5), hoof())]))
    # tier II, the Thornstalker: a bone plate over the cap's brow, a thorn-spiked hide pauldron, a second bracer, a bone quiver rim
    B.append(Bone("brow_plate", "head", stages=[2, 3], boxes=[Box((-3.5, -10.5, -5.2), (7, 3, 1), per_tier(cap_plate, (2, 3))),
        Box((-0.5, -12.5, -5.2), (1, 2, 1), wm_tusk())]))
    B.append(Bone("pauldron", "right_arm", stages=[2, 3], boxes=[
        Box((-3.5, -3, -2.5), (5, 3, 5), pelt(HIDE, "wm-hpd"), dil=0.1),
        Box((-3, -5, -1.5), (1, 2, 1), wm_tusk()), Box((-1.5, -6, -0.5), (1, 3, 1), wm_tusk()), Box((-3, -5, 0.5), (1, 2, 1), wm_tusk())]))
    B.append(Bone("right_bracer", "right_forearm", stages=[2, 3], boxes=[Box((-2, 1, -2), (4, 4, 4), bracer(), dil=0.3)]))
    B.append(Bone("quiver_rim", "body", pivot=(-1.5, -6, 3.5), rot=(0, 0, -16), stages=[2, 3], boxes=[
        Box((-2, -5, -1.5), (4, 1, 3), bone_paint(4, "hqb"), dil=0.15), Box((-2, 3.5, -1.5), (4, 1, 3), bone_paint(3, "hqc"), dil=0.15)]))
    # tier III, the Elder Hunter: a five-feather fan barred in ochre, a pelt mantle and a tusk necklace
    B.append(Bone("mantle", "body", stages=[3], boxes=[
        Box((-5, -13.5, -3), (10, 2, 6), pelt(GREYFUR, "wm-hmt"), dil=0.15),
        Box((-5, -10.5, -3), (2, 3, 6), pelt(GREYFUR, "wm-hmr"), dil=0.1), Box((3, -10.5, -3), (2, 3, 6), pelt(GREYFUR, "wm-hml"), dil=0.1)]))
    B.append(Bone("tusk_necklace", "body", stages=[3], boxes=[
        Box((-3, -10.5, -3.3), (6, 1, 1), lashing(OCHRE, 3), dil=0.05),
        Box((-2.5, -9.5, -3.3), (1, 2, 1), wm_tusk()), Box((-0.5, -9.5, -3.5), (1, 3, 1), wm_tusk()), Box((1.5, -9.5, -3.3), (1, 2, 1), wm_tusk())]))
    return B

def jerkin_cloth(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("top", "bottom"): return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0))
        for x in range(w): g[h - 1][x] = clamp(T, 1)
        if face == "front":
            g[0][3] = g[0][4] = clamp(T, 1)                       # the neck notch
            for y in range(1, h - 1): g[y][w // 2] = clamp(T, 2)  # centre lacing seam
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return jerkin_cloth(T, M)

NOTE = ("Wildmoot Hide Hunter (unit identity, Wildmoot): lean hunched boar-kin archer with floppy ears, small tusks, clay brow stripes, "
        "a leather cap with an ochre-stitched brim and a three-feather fan on the left, a hide cloak and quiver on the back, a team jerkin "
        "and the order bow in the left hand. Tier II: bone brow plate, spiked pauldron, second bracer, quiver rim; tier III: "
        "five-feather ochre-barred fan, pelt mantle, tusk necklace, ochre quiver bands. No back flag.")
def veteran(bone, box):
    """The Mastery II trim: ochre studs on the belt, a bone-beaded cord on each arm above the bracer and on each thigh."""
    n = bone.name
    if n == "belt": return vet_rows((0, 1), every=2, bead=True)
    if n.endswith(("_arm", "_forearm")): return vet_rows((4,))
    if n.endswith(("_leg", "_shin")): return vet_rows((3,))
    return None

WM_HUNTER = Troop("wm_hunter", SKIN, CLOTH, bones, cloth_painter,
                  {"weapon": "order_bow", "hand": "left", "idleArms": "hang", "portrait": "tall 4"}, veteran=veteran)
def build(): return WM_HUNTER.build()
def outputs(): return WM_HUNTER.outputs()

if __name__ == "__main__":
    house_stub.main(WM_HUNTER, NOTE, sys.argv)
