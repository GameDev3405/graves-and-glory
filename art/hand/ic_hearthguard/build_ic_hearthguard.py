#!/usr/bin/env python3
"""The Ironclad Hearthguard (Ironclad house, troop id ic_hearthguard), hand-authored at one texel per model pixel.

A short, broad vindicator who holds the line: a riveted box helm with cheek plates and an ember horsehair crest over a
copper beard braided down to his belt, layered slab pauldrons (the mark), a breastplate over a knee-length soot-iron scale
coat under a chequy team tabard, a brass-buckled belt, knee cops and sabatons, and a shield on the left arm. Tier II (the
Hearth Warden): clean iron with brass rivets, a brass comb, brow and nasal, brass-rimmed pauldrons with a fourth lame and a
brass boss, a gorget and heavier knee cops. Tier III (the Forge Sworn): brass rims on all plate, brass-edged scales, two short
bone helm horns, a spike on each boss, ember-seamed tassets over the hips and greaves. No back flag.

  python build_ic_hearthguard.py preview <dir> [side] | contact <png> [side] [all] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
from ironclad import *   # noqa: F401,F403
from handlib import Troop
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import ironclad_tiers as IT
import house_stub

SKIN = (128, 128)
CLOTH = (128, 64)
P = pal_ic()
FACE = SKINS["fair"]
BEARD = ramp("2a0f08", "5a2410", "8a3c18", "b8642a", "d98f4e")   # copper, darkest first

# ---- materials ----
def scales(R=IRON, base=2, seed="sc"):
    """Soot-iron scale coat: rows of overlapping scales, each catching light on its upper edge."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face not in ("top", "bottom"):
                    off = (y // 2) % 2
                    if y % 2 == 0 and (x + off) % 2 == 0: v += 1
                    elif y % 2 == 1: v -= 1
                g[y][x] = clamp(R, v)
        return scuff(g, (seed, face), max(1, w * h // 12), R)
    return paint

def slab(base=3, ember=False, seed="sl", rivets=True):
    """A thick slab pauldron: bright hammered top, a dark lip under, rivets, an optional ember edge."""
    def paint(face, w, h):
        g = plate(R=IRON, base=base, spec=False)(face, w, h)
        if face in ("top", "bottom"): return scuff(g, (seed, face), 3)
        if rivets and w >= 5 and h >= 2:
            for x in (1, w - 2): rivet(g, x, 0)
        if ember:
            for x in range(w): g[h - 1][x] = EMBER[2 if x % 2 else 3]
        return scuff(g, (seed, face), 3)
    return paint

def helm_box():
    def paint(face, w, h):
        g = plate(R=IRON, base=3, spec=False, seam=False)(face, w, h)
        if face == "bottom": return blank(w, h)
        if face == "top":
            for x in range(1, w - 1):
                g[w // 2][x] = IRON[2] if x != w // 2 else IRON[1]   # the forged ridge
            for y in range(h): g[y][w // 2] = IRON[5 if y % 2 else 4]
            return scuff(g, ("hb", face), 5)
        for x in range(w): g[h - 1][x] = IRON[1]                   # the dark brim lip
        for x in range(w): g[0][x] = IRON[5]
        for x in (1, w - 2):
            if w > 5: rivet(g, x, 1)
        if face == "front":
            for x in range(w): g[h - 2][x] = IRON[2]
        return scuff(g, ("hb", face), 4)
    return paint

def cheek():
    def paint(face, w, h):
        g = plate(R=IRON, base=3, spec=False)(face, w, h)
        if face in ("front", "back") and h > 2:
            rivet(g, 0, 1)
        return scuff(g, ("ck", face, w), 2)
    return paint

def nasal():
    return plate(R=IRON, base=4, spec=False, seam=False)

def face_head():
    """The face below the brim: eyes in the helm's shadow, a nose and a copper beard, ruddy cheeks."""
    s = FACE
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face == "top" or face == "bottom": g[y][x] = IRON[1]; continue
                g[y][x] = BEARD[2 if (x + y) % 3 else 1] if y >= 5 else (s[3] if face != "back" else IRON[2])
                if face == "back": g[y][x] = IRON[2 + (x + y) % 2]
        if face == "front":
            for x in range(w): g[4][x] = s[2]
            g[4][2] = g[4][5] = INK                       # the eyes, hard under the brim
            g[5][3] = g[5][4] = s[2]                      # the nose, then the beard closes in
            for y in range(5, h):
                for x in range(w):
                    g[y][x] = BEARD[3 if (x + y) % 3 == 0 else 2]
            g[5][3] = g[5][4] = s[3]
            g[5][0] = g[5][7] = s[2]
        if face in ("left", "right"):
            for y in range(h):
                for x in range(w): g[y][x] = IRON[2] if y < 5 else BEARD[2 if (x + y) % 3 else 1]
        return g
    return paint

def crest():
    return ember_plume("hg-crest")

def crest_tail():
    return ember_plume("hg-tail", tail=True)

def braid():
    """A copper beard plaited down the chest: a rope of diagonal twists tied with an iron ring."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = BEARD[1 + ((x + y) % 3 == 0) + (face in ("front",) and (x + y) % 2)]
        if face in ("top", "bottom") or h < 4: return g
        for x in range(w): g[h - 1][x] = IRON[3]
        for x in range(w): g[h - 3][x] = IRON[4] if face == "front" else IRON[3]
        return g
    return paint

# ---- body, arms, legs ----
def body_painter():
    return rows([(0, lames([3, 3], base=3, seed="hbk", wear=2, rivets=False)), (6, scales(IRON, 2, "hbs"))])

def breastplate():
    def paint(face, w, h):
        if face in ("top", "bottom"): return plate(R=IRON, base=4, spec=False)(face, w, h)
        g = lames([3, 3], base=4, seed="hbp", wear=3, rivets=True)(face, w, h)
        if face == "front":
            for y in range(h): g[y][w // 2] = IRON[5] if y % 2 else IRON[3]    # the forged keel
            for x in range(w): g[0][x] = IRON[5]
        return g
    return paint

def coat():
    def paint(face, w, h):
        if face in ("top", "bottom"): return scales(IRON, 2, "hct")(face, w, h)
        g = scales(IRON, 2, "hco")(face, w, h)
        for x in range(w): g[h - 1][x] = EMBER[1 if x % 2 else 2]               # a rusted hem
        return g
    return paint

def brass_scales(p):
    """Tier III: every other scale row edged in brass."""
    def paint(face, w, h):
        g = [list(r) for r in p(face, w, h)]
        if face in ("top", "bottom"): return g
        for y in range(0, h - 1, 4):
            for x in range(w):
                if (x + (y // 2) % 2) % 2 == 0 and g[y][x] is not None: g[y][x] = IT.BRASS[2 + (x % 4 == 0)]
        return g
    return paint

def belt():
    def paint(face, w, h):
        g = leather_ic(base=1, stitch=True)(face, w, h)
        if face == "front":
            for y, row in enumerate((GOLD[3:6], (GOLD[4], GOLD[2], GOLD[4]))):
                for x, c in enumerate(row): g[y][w // 2 - 1 + x] = c          # the brass buckle
        return g
    return paint

def sleeve():
    return rows([(0, mail_ic(2)), (5, lames([2, 2, 2], base=3, seed="hvb", wear=2, rivets=False)), (11, leather_ic(base=1))])

def leg():
    return rows([(0, scales(IRON, 2, "hlg")), (4, mail_ic(2)), (7, lames([2, 2, 1], base=3, seed="hgr", wear=2, rivets=False))])

def pauldron(i, left):
    return slab(base=3 + (1 if i == 0 else 0), ember=(i == 2), seed=("hp", i, left))

def knee():
    return lames([2], base=4, seed="hk", wear=1, rivets=True)

def boot():
    return rows([(0, lames([2], base=3, seed="hsb", wear=1, rivets=False)), (2, leather_ic(base=1, stitch=True))])

# ---- the shield: an iron-rimmed board, its face in the team colour with a dark bordure and a pale chief ----
def shield_rim_ic():
    def paint(face, w, h):
        g = plate(R=IRON, base=3, spec=False, seam=False, rim=(face == "front" and h == 1))(face, w, h)
        if face == "front":
            if h > 1: g[0] = [clamp(IRON, 4)] * w
            for y in range(h):
                for x in range(w):
                    if (h > 1 and y % 3 == 1) or (h == 1 and w > 4 and x % 3 == 1): g[y][x] = IRON[5]    # rivets
        return scuff(g, ("hsr", face, w, h), max(1, w * h // 20))
    return paint

def shield_boss_ic():
    def paint(face, w, h):
        g = plate(R=IRON, base=4, spec=False, seam=False)(face, w, h)
        if face == "front": g[0][0] = IRON[6]; g[h - 1][w - 1] = IRON[2]
        return g
    return paint

def shield_face(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face != "front": return g
        for y in range(h):
            for x in range(w):
                if not in_shield(x, y): continue
                v = 4 if y < 5 and x < 5 and x + y < 6 else 2 if y >= 13 else 3
                if not in_shield(x, y, 2): v = 1
                g[y][x] = clamp(T, v)
        for y in range(h):
            for x in range(w):
                if in_shield(x, y) and not in_shield(x, y, 1) and in_shield(x, y, 0): g[y][x] = IRON[1]    # the black bordure
        for y in range(2, 5):
            for x in range(w):
                if in_shield(x, y, 2): g[y][x] = clamp(IRON, 5 if y < 4 else 4)                   # a pale chief
        return g
    return paint

# ---- cloth ----
def tabard(T, M):
    """A chequy tabard hanging from the belt: team and a darker team, two-by-two, a dark foot."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                chk = ((x // 2) + (y // 2)) % 2 == 0
                v = (3 if chk else 1) + LIT[face] + (1 if y == 0 else 0)
                g[y][x] = clamp(T, v)
        for x in range(w): g[h - 1][x] = EMBER[1 if x % 2 else 2]
        return g
    return paint

def bordure(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w): g[y][x] = clamp(T, 3 + LIT[face] - (1 if y >= h - 2 else 0))
        for x in range(w): g[0][x] = EMBER[3]
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face_head())]))
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-4.5, -9.5, -4.5), (9, 5, 9), IT.T(helm_box(), top=False, every=True)),
        Box((-5, -5, -3.5), (1, 4, 6), IT.T(cheek(), top=False, every=True)), Box((4, -5, -3.5), (1, 4, 6), IT.T(cheek(), top=False, every=True)),
        Box((-1, -5, -4.9), (2, 3, 1), {1: nasal(), 2: IT.brass(3), 3: IT.brass(3)})]))
    B.append(Bone("crest", "head", tag="helm", boxes=[
        Box((-1, -12.5, -3), (2, 3, 7), crest(), dil=0.1),
        Box((-1, -10.5, 3.5), (2, 6, 1), crest_tail(), dil=0.1)]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms()
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), sleeve(), f"{side}_forearm", ELBOW[side])
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg(), f"{side}_shin", KNEE)
    B.append(Bone("breastplate", "body", boxes=[Box((-4.5, -12, -2.5), (9, 6, 5), IT.T(breastplate(), top=False, every=True), dil=0.1)]))
    B.append(Bone("coat", "body", boxes=[Box((-4.5, -6, -2.5), (9, 8, 5), {1: coat(), 2: coat(), 3: brass_scales(coat())}, dil=0.05)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -5, -2), (8, 2, 4), belt(), dil=0.6)]))
    B.append(Bone("beard_braid", "body", boxes=[Box((-1.5, -9, -3.5), (3, 8, 1), braid())]))
    B.append(Bone("tabard", "body", team=True, boxes=[
        Box((-3, -6, -2.95), (6, 8, 0), "tabard", sheet="cloth"), Box((-3, -6, 2.95), (6, 8, 0), "tabard", sheet="cloth")]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 10 * c), boxes=[
            Box((-4 + 0.5 * c, -2.5, -3.5), (8, 2, 7), IT.T(pauldron(0, side), top=False, foot=True)),
            Box((-4 + 1.0 * c, -0.5, -3.5), (8, 2, 7), IT.T(pauldron(1, side), top=False, foot=True), dil=0.1),
            Box((-4 + 1.5 * c, 1.5, -3), (8, 2, 6), IT.T(pauldron(2, side), top=False, foot=True), dil=0.15)]))
        # tier II: a fourth lame under the slab and a brass boss on top; tier III a spike on the boss
        B.append(Bone(f"{side}_lame4", f"{side}_pauldron", stages=[2, 3], boxes=[Box((-4 + 2.0 * c, 3.5, -3), (8, 2, 6), IT.P("hgl4" + side, 3, ember=True), dil=0.2)]))
        B.append(Bone(f"{side}_boss", f"{side}_pauldron", stages=[2, 3], boxes=[Box((0.5 * c - 1.5, -3.5, -1.5), (3, 1, 3), IT.brass(3))]))
        B.append(Bone(f"{side}_boss_spike", f"{side}_pauldron", stages=[3], boxes=[Box((0.5 * c - 0.5, -6, -0.5), (1, 2.5, 1), IT.spike(IT.IRON, 3, 3))]))
        B.append(Bone(f"{side}_knee", f"{side}_shin", boxes=[Box((-2.5, -1, -3), (5, 2, 2), IT.T(knee(), top=False, every=True))]))
        B.append(Bone(f"{side}_knee_wing", f"{side}_shin", stages=[2, 3], boxes=[Box((-3, -2, -3.4), (6, 4, 1.4), IT.P("hgkw" + side, 4), dil=0.1)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[3], boxes=[Box((-2.4, 1.5, -2.4), (4.8, 2, 4.8), IT.P("hggv" + side, 3), dil=0.1)]))
        B.append(Bone(f"{side}_boot", f"{side}_shin", boxes=[Box((-2.5, 3, -3), (5, 3, 5), IT.T(boot(), top=False, every=True))]))
        B.append(Bone(f"{side}_tasset", "body", stages=[3], boxes=[Box((-5.4 if side == "right" else 4.4, -5, -2.6), (1, 5, 5.2), IT.P("hgts" + side, 3, ember=True))]))
        B.append(Bone(f"{side}_horn", "head", tag="helm", stages=[3], boxes=[
            Box((-6.5 if side == "right" else 4.5, -9, -1), (2, 1.5, 2), IT.horn()), Box((-7.5 if side == "right" else 5.5, -12.5, -0.75), (2, 3.5, 1.5), IT.horn())]))
    B.append(Bone("comb", "head", tag="helm", stages=[2, 3], boxes=[Box((-1.5, -10.5, -4.2), (3, 1, 8.4), IT.brass(3))]))
    B.append(Bone("brow", "head", tag="helm", stages=[2, 3], boxes=[Box((-4.7, -6.5, -4.9), (9.4, 1, 1), IT.brass(3))]))
    B.append(Bone("gorget", "body", stages=[2, 3], boxes=[Box((-3.5, -12.8, -2.9), (7, 1.6, 5.8), IT.P("hggt", 3), dil=0.1)]))
    # the shield, on the left arm (TroopFeatures.Shield draws none of vanilla's for a model that carries its own)
    B.append(Bone("shield", "left_forearm", SHIELD_PIVOT, rot=SHIELD_ROT, tag="shield", boxes=   # the old arm-frame grip less the elbow
        shield_boxes(wood, shield_rim_ic())
        + [Box((-1, -5, -2.7), (2, 2, 1), shield_boss_ic()), Box((-1, -3, -0.9), (2, 5, 3), leather_ic(base=2))]))
    B.append(Bone("shield_face", "shield", team=True, boxes=[Box((-6, -10, -2.1), (12, 18, 0), "shield", sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard": return tabard(T, M)
    return shield_face(T, M)

NOTE = ("Ironclad Hearthguard (unit identity, Ironclad): vindicator, biped, broad and short; a riveted box helm with cheek plates and an "
        "ember crest over a copper beard braided to the belt, layered slab pauldrons, a breastplate over a soot-iron scale coat, a chequy "
        "team tabard, a brass-buckled belt, knee cops and sabatons; the shield on the left arm. Tier II brass rivets, comb, brow, nasal, rims, boss and a fourth pauldron lame; tier III brass rims on all plate, brass-edged scales, bone helm horns, boss spikes and ember-seamed tassets. No back flag.")
IC_HEARTHGUARD = Troop("ic_hearthguard", SKIN, CLOTH, bones, cloth_painter,
                       {"weapon": "hearth_axe", "hand": "right", "idleArms": "hang", "portrait": "standard"},
                       hide=lambda bn: bn.name.startswith("shield"))
def build(): return IC_HEARTHGUARD.build()
def outputs(): return IC_HEARTHGUARD.outputs()

if __name__ == "__main__":
    house_stub.main(IC_HEARTHGUARD, NOTE, sys.argv)
