#!/usr/bin/env python3
"""The Wildmoot Bloodhowl Seer (Wildmoot house, troop id wm_seer), hand-authored at one texel per model pixel.

A stooped boar-kin elder: grey-furred muzzle, clay-glow eyes, an antler crown of two branching tines, a hide robe to the
shins daubed with blood handprints, bone and ochre trinkets, a team sash across the chest and a bone wand in the right
hand. A totem staff (wood pole, bone skull and antler top, ochre lashing) is strapped diagonally on the back. Epic, so the
eyes and a few staff runes glow. No back flag.

Tier I is the Seer. Tier II adds a third tine to each antler, a necklace of bone plaques, a mantle of dark feathers and
ochre bands down the wand. Tier III raises a taller crown of tines, hangs two trophy skulls on the staff, sets a bone and
tooth collar at the throat and an ochre fringe at the robe's hem. The glow texels are unchanged at every tier.

  python build_wm_seer.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from wildmoot import *   # noqa: F401,F403
import house_stub

SKIN = (256, 128)   # tier II and III bone and the wand tiers
CLOTH = (128, 64)
HUNCH = 30

PRINT = ["X.X", "XXX", "XXX", ".X."]

def handprint(g, x0, y0):
    for dy, row in enumerate(PRINT):
        for dx, c in enumerate(row):
            y, x = y0 + dy, x0 + dx
            if c == "X" and 0 <= y < len(g) and 0 <= x < len(g[0]): g[y][x] = BLOOD[2 if (dx + dy) % 3 else 3]
    return g

def head_painter():
    """Flesh above, a grey-furred muzzle below, sunk brow, clay eyes (lit by the glow sheet)."""
    S = PIG
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 4 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(S, v)
        if face == "front":
            for x in range(w): g[3][x] = S[1]                                        # a heavy, sunk brow
            for x in (1, 2, 5, 6): g[4][x] = S[0]
            g[4][2] = g[4][5] = CLAY[3]                                              # the clay eyes
            for y in range(5, h):
                for x in range(w): g[y][x] = clamp(GREYFUR, 3 + (x + y) % 2 + (1 if y == 5 else 0))   # grey muzzle fur
            for x in (0, 7): g[3][x] = GREYFUR[2]
        if face in ("left", "right", "back"):
            for y in range(h):
                for x in range(w):
                    if y < 5 or face == "back": g[y][x] = clamp(GREYFUR, 2 + (x + y) % 3)   # a grey mane
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(GREYFUR, 2 + (x * 3 + y) % 3)
        return scuff(g, ("shd", face), 4, S)
    return paint

def snout():
    def paint(face, w, h):
        g = flat(GREYFUR, 4)(face, w, h)
        if face == "front":
            for x in range(w): g[0][x] = GREYFUR[5]
            g[1][0] = g[1][w - 1] = PIG[0]                                            # the nostrils
            if w > 3: g[1][1] = g[1][w - 2] = PIG[1]
            for x in range(w): g[h - 1][x] = GREYFUR[2]
        return g
    return paint

def ear():
    return flat(GREYFUR, 3, "sear", 2)

def antlerp():
    return antler(3, "sant")

def robe():
    """The hide robe over the torso: dark hide, fold lines, a V of bare throat, blood handprints."""
    def paint(face, w, h):
        g = flat(HIDE, 2, "srb", 5)(face, w, h)
        if face in ("top", "bottom"): return g
        for x in range(1, w, 3):
            for y in range(h): g[y][x] = clamp(HIDE, 1)
        if face == "front":
            g[0][3] = g[0][4] = PIG[2]; g[1][3] = g[1][4] = PIG[1]
            handprint(g, 0, 7); handprint(g, 5, 9)
        if face == "back": handprint(g, 1, 6)
        return g
    return paint

def skirt():
    """The robe skirt to the shins: hide with a ragged ochre-stitched hem and handprints."""
    def paint(face, w, h):
        g = flat(HIDE, 2, "ssk", 6)(face, w, h)
        if face in ("top", "bottom"): return g
        for x in range(1, w, 3):
            for y in range(h): g[y][x] = clamp(HIDE, 1)
        for x in range(w): g[h - 1][x] = clamp(HIDE, 1)
        for x in range(0, w, 2): g[h - 2][x] = OCHRE[3]
        if face == "front": handprint(g, 1, 2); handprint(g, 5, 3)
        if face == "back": handprint(g, 3, 2)
        return g
    return paint

def arm():
    """Hide sleeve to the elbow, bare grey-furred forearm below."""
    return rows([(0, flat(HIDE, 2, "ssl", 4)), (8, flat(PIG, 3, "sab", 3))])

def cuff():
    def paint(face, w, h):
        g = flat(BONE, 3)(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(0, w, 2): g[0][x] = OCHRE[3]
        return g
    return paint

def shaw():
    return pelt(GREYFUR, "wm-sshawl")

def leg_painter():
    return rows([(0, flat(HIDE, 2, "slg", 3)), (9, flat(PIG, 3, "slp", 3))])

def hoof():
    return tone(HORN, 3, "sho", 3)

def belt():
    def paint(face, w, h):
        g = lashing(OCHRE, 2)(face, w, h)
        return g
    return paint

def tooth():
    return tone(BONE, 4, "stt", 0)

def bead():
    return lashing(OCHRE, 4)

def skull():
    """The staff's bone skull: dark sockets on the back face (lit)."""
    def paint(face, w, h):
        g = bone_paint(4, "ssk")(face, w, h)
        if face == "back":
            g[1][0] = g[1][w - 1] = CLAY[3]; g[2][w // 2] = HORN[1]
        return g
    return paint

def pole():
    """Wood pole with notches and, near the top, three clay rune texels on the back face."""
    def paint(face, w, h):
        g = flat(WOOD, 3, "spl", 2)(face, w, h)
        for y in range(4, h - 2, 5): g[y][0] = clamp(WOOD, 1)
        if face == "back":
            for y in (3, 5, 7): g[y][0] = CLAY[3]
        return g
    return paint

def lash():
    return lashing()

def wand():
    return bone_paint(4, "swd")

def plume():
    """A hanging black-brown feather of the mantle, a pale bone-white tip (the bottom row)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(HORN, 2 + LIT[face] + (1 if y == 0 else 0))
        if face not in ("top", "bottom"): g[h - 1] = [BONE[4]] * w
        return g
    return paint

def sash_tail():
    return None

def glow_fn(bone, box):
    if bone.name == "head" and box.size == [8, 8, 8]:
        def p(face, w, h):
            g = blank(w, h)
            if face == "front": g[4][2] = g[4][5] = GLOWCLAY[2]
            return g
        return p
    if bone.name == "staff" and box.size == [3, 3, 3]:
        def p(face, w, h):
            g = blank(w, h)
            if face == "back": g[1][0] = g[1][w - 1] = GLOWCLAY[1]
            return g
        return p
    if bone.name == "staff" and box.size[1] > 20:
        def p(face, w, h):
            g = blank(w, h)
            if face == "back":
                for y in (3, 5, 7): g[y][0] = GLOWCLAY[1]
            return g
        return p
    return None

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), robe())], rot=(HUNCH, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), head_painter())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("snout", "head", boxes=[Box((-2.5, -3.5, -6), (5, 4, 2), snout())]))
    B.append(Bone("ear_r", "head", pivot=(-4, -6, 0), rot=(0, 0, 18), boxes=[Box((-1, 0, -1.5), (1, 4, 3), ear())]))
    B.append(Bone("ear_l", "head", pivot=(4, -6, 0), rot=(0, 0, -18), boxes=[Box((0, 0, -1.5), (1, 4, 3), ear())]))
    for name, s in (("antler_r", -1), ("antler_l", 1)):
        B.append(Bone(name, "head", pivot=(s * 2.5, -8, 0), rot=(0, 0, s * 14), boxes=[
            Box((-0.5, -6, -0.5), (1, 6, 1), antlerp()),                                   # the main beam, 6 px
            Box((s * 1 - 0.5, -5, -0.5), (1, 3, 1), antlerp()),                            # an outer tine
            Box((-0.5 - s * 1.0, -5, -0.5), (1, 2, 1), antlerp()),                         # an inner tine
            Box((-1, -1.5, -1), (2, 1, 2), lash(), dil=0.1)]))                            # ochre lashing at the base
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    # the weapon, the bone wand (weaponTiers): I a bone rod with an ochre grip; II longer, ochre-banded, a fang at the head;
    # III a trophy skull on the head between two antler tines, the shaft bound in ochre
    for t in (1, 2, 3):
        bx = [Box((-0.5, -0.5, -5), (1, 1, 5), wand()), Box((-1, -1, -6), (2, 2, 1), wand()), Box((-1, -1, -1.5), (2, 2, 1), lash(), dil=0.1)]
        if t >= 2:
            bx = [Box((-0.5, -0.5, -7), (1, 1, 7), wand()), Box((-1, -1, -1.5), (2, 2, 1), lash(), dil=0.1),
                  Box((-0.5, -0.5, -5), (1, 1, 1), lash(), dil=0.15), Box((-0.5, -0.5, -3.5), (1, 1, 1), lash(), dil=0.15)]
            bx += [Box((-1, -1, -8), (2, 2, 1), wand()), Box((-0.5, 1, -8), (1, 2, 1), wm_tusk())] if t == 2 else [
                   Box((-1.5, -1.5, -10), (3, 3, 3), wm_skull("swk")), Box((-2.5, -3.5, -9), (1, 3, 1), antler(4, "swa")),
                   Box((1.5, -3.5, -9), (1, 3, 1), antler(4, "swb")), Box((-1, -1, -7), (2, 2, 1), lash(), dil=0.1)]
        B.append(Bone("wand" if t == 1 else f"wand_t{t}", "right_hand", tag="weapon", weapon_tiers=[t], boxes=bx))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm())], rot=(-HUNCH, 0, 0)))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg_painter())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg_painter())]))
    B.append(Bone("skirt", "root", boxes=[Box((-4.5, 11.5, -2.5), (9, 9, 5), skirt())]))
    for side in ("right", "left"):
        B.append(Bone("%s_hoof" % side, "%s_leg" % side, boxes=[Box((-2.5, 10, -3), (5, 2, 5), hoof())]))
        B.append(Bone("%s_cuff" % side, "%s_arm" % side, boxes=[Box((-2 + (0 if side == "right" else -1), 7, -2), (4, 1, 4), cuff(), dil=0.3)]))
    B.append(Bone("shawl", "body", boxes=[Box((-5, -13.5, -3), (10, 4, 6), shaw(), dil=0.2)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 1, 4), belt(), dil=0.5)]))
    B.append(Bone("trinkets", "body", boxes=[
        Box((-2.5, -2, -2.7), (1, 3, 1), tooth()), Box((0.5, -2, -2.7), (1, 4, 1), tooth()), Box((2.5, -2, -2.7), (1, 2, 1), tooth()),
        Box((-1, -2, -2.7), (1, 1, 1), bead()), Box((1.5, -2, -2.7), (1, 1, 1), bead())]))
    B.append(Bone("necklace", "body", boxes=[Box((-2.5, -12, -2.7), (5, 1, 1), tooth()), Box((-0.5, -11, -2.7), (1, 2, 1), bead())]))
    B.append(Bone("sash", "body", team=True, pivot=(0, -7, 0), rot=(0, 0, 40), boxes=[Box((-4, -1, -2.5), (8, 2, 5), "sash", sheet="cloth")]))
    B.append(Bone("sash_tail", "body", team=True, boxes=[Box((-4, -1, -2.5), (3, 5, 1), "tail", sheet="cloth")]))
    B.append(Bone("staff", "body", pivot=(0, -6, 3), rot=(0, 0, 22), boxes=[
        Box((-0.5, -12, -0.5), (1, 24, 1), pole()),
        Box((-1.5, -15, -1.5), (3, 3, 3), skull()),
        Box((-2.5, -17, -0.5), (1, 3, 1), antlerp()), Box((1.5, -17, -0.5), (1, 3, 1), antlerp()),
        Box((-1, -9, -1), (2, 1, 2), lash(), dil=0.1), Box((-1, 1, -1), (2, 1, 2), lash(), dil=0.1)]))
    # tier II: a third tine on each antler, a necklace of bone plaques, a feather mantle, ochre bands down the wand
    for name, sg in (("antler_r", -1), ("antler_l", 1)):
        B.append(Bone(name + "_tine3", name, stages=[2, 3], boxes=[
            Box((sg * 1 - 0.5, -8, -0.5), (1, 3, 1), antler(4, "st3" + name)), Box((sg * 2 - 0.5, -4, -0.5), (1, 2, 1), antler(4, "st4" + name))]))
    B.append(Bone("plaques", "body", stages=[2, 3], boxes=[
        Box((-4.5, -11, -3.8), (2, 2, 1), bone_paint(4, "spq1")), Box((-1, -10.5, -3.8), (2, 3, 1), bone_paint(4, "spq2")),
        Box((2.5, -11, -3.8), (2, 2, 1), bone_paint(4, "spq3")), Box((-5, -12, -3.6), (10, 1, 1), lash(), dil=0.05)]))
    B.append(Bone("feather_mantle", "body", stages=[2, 3], boxes=[
        Box((-5.5, -10, -3.2 + i * 1.5), (1, 3 + i % 2, 1), plume()) for i in range(5)] + [
        Box((4.5, -10, -3.2 + i * 1.5), (1, 3 + i % 2, 1), plume()) for i in range(5)] + [
        Box((-4.5 + i * 2, -10, 3.2), (1, 3 + (i + 1) % 2, 1), plume()) for i in range(5)]))
    # and heavy bone shoulder plates lashed over the shawl, a bone breast plate, bone bracers
    for side, x0 in (("right", -3.5), ("left", -1.5)):
        B.append(Bone(f"{side}_shoulder", f"{side}_arm", stages=[2, 3], boxes=[
            Box((x0, -3, -2.6), (5, 2, 5.2), per_tier(lambda t, sd=side: wm_plate(t, "ssh" + sd), (2, 3)), dil=0.1),
            Box((x0 + 0.5, -1, -2.4), (4, 2, 4.8), per_tier(lambda t, sd=side: wm_plate(t, "ssi" + sd), (2, 3)), dil=0.05)]))
        B.append(Bone(f"{side}_bracer", f"{side}_arm", stages=[2, 3], boxes=[
            Box((x0 + 0.5, 4, -2.3), (4, 3, 4.6), per_tier(lambda t, sd=side: wm_plate(t, "sbr" + sd), (2, 3)), dil=0.05)]))
    B.append(Bone("breastplate", "body", stages=[2, 3], boxes=[
        Box((-3, -9, -2.9), (6, 5, 1), per_tier(lambda t: wm_plate(t, "sbp"), (2, 3)), dil=0.05)]))
    # tier III: a taller crown of more tines, two trophy skulls on the staff, a bone and tooth collar, an ochre fringe at the hem
    for name, sg in (("antler_r", -1), ("antler_l", 1)):
        B.append(Bone(name + "_crown", name, (0, -6, 0), stages=[3], rot=(0, 0, sg * 10), boxes=[
            Box((-0.5, -4, -0.5), (1, 4, 1), antler(4, "scr" + name)), Box((sg * 1 - 0.5, -3, -0.5), (1, 2, 1), antler(4, "sct" + name)),
            Box((-0.5 - sg * 1, -2, -0.5), (1, 1, 1), antler(4, "scu" + name)), Box((-0.5, -4.5, -0.5), (1, 1, 1), lash())]))
    B.append(Bone("staff_skulls", "body", pivot=(0, -6, 3), rot=(0, 0, 22), stages=[3], boxes=[
        Box((-1.5, -6, -0.5), (2, 2, 2), wm_skull("ss1")), Box((0.5, -2, -0.5), (2, 2, 2), wm_skull("ss2")),
        Box((-1, -6.5, -1), (2, 1, 2), lash(), dil=0.1)]))
    B.append(Bone("bone_collar", "body", stages=[3], boxes=[
        Box((-4.5, -13, -3.4), (9, 1, 1), bone_paint(4, "sbc")),
        Box((-3.5, -12, -3.4), (1, 2, 1), wm_tusk()), Box((-1.5, -12, -3.4), (1, 2, 1), wm_tusk()),
        Box((0.5, -12, -3.4), (1, 2, 1), wm_tusk()), Box((2.5, -12, -3.4), (1, 2, 1), wm_tusk())]))
    B.append(Bone("hem_fringe", "root", stages=[3], boxes=[Box((-4.5, 19.5, -2.5), (9, 1, 5), lashing(OCHRE, 3), dil=0.35)]))
    B.append(Bone("chest_skull", "body", stages=[3], boxes=[
        Box((-1.5, -8.5, -4.2), (3, 3, 2), wm_skull("scs")), Box((-3.5, -9, -3.6), (1, 3, 1), wm_tusk()), Box((2.5, -9, -3.6), (1, 3, 1), wm_tusk())]))
    for side, x0 in (("right", -3.5), ("left", -1.5)):
        B.append(Bone(f"{side}_shoulder_tusks", f"{side}_shoulder", stages=[3], boxes=[
            Box((x0 + 0.5, -6, -0.5), (1, 3, 1), wm_tusk()), Box((x0 + 3.5, -5, -0.5), (1, 2, 1), wm_tusk()),
            Box((x0 + 2, -5, 1.5), (1, 2, 1), wm_tusk())]))
    return B

def sash_cloth(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0))
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tail": return cloth_band(T, base=3, ragged=True, seed="wm-stail")
    return sash_cloth(T, M)

NOTE = ("Wildmoot Bloodhowl Seer (unit identity, Wildmoot): a stooped boar-kin elder with a grey-furred muzzle, clay-glow eyes, an antler "
        "crown of two branching tines, a hide robe to the shins with blood handprints, bone and ochre trinkets, a team sash across the chest "
        "and a bone wand in the right hand. A totem staff (bone skull and antler top) is strapped diagonally on the back. Tier II: third tines, "
        "bone plaques, feather mantle, shoulder plates, breast plate, bracers, a longer banded wand; tier III: taller crown, staff skulls, tooth collar, ochre hem, a chest skull, shoulder tusks, a skull-headed wand. No back flag.")
def veteran(bone, box):
    """The Mastery II trim: a bone-beaded ochre baldric, studs on the belt, a cord above each cuff and a band on the skirt."""
    n = bone.name
    if n == "body": return vet_baldric()
    if n == "belt": return vet_rows((0,), every=2, bead=True)
    if n.endswith("_arm"): return vet_rows((4,))
    if n.endswith("_leg"): return vet_rows((3,))
    if n == "skirt": return vet_rows((6,), bead=True)
    return None

WM_SEER = Troop("wm_seer", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "tall 6"}, glow=glow_fn, veteran=veteran)
def build(): return WM_SEER.build()
def outputs(): return WM_SEER.outputs()

if __name__ == "__main__":
    house_stub.main(WM_SEER, NOTE, sys.argv)
