#!/usr/bin/env python3
"""Brassjaw, the Forge Colossus (troop id ic_colossus), hand-authored at one texel per model pixel on the colossus rig.

A hulking soot-iron golem: riveted plate over a furnace chest with a lit grille, twin smokestacks on the shoulders, a
heavy brow over ember eyes and a hinged brass jaw, piston-banded arms ending in brass-knuckled fists, a team cloth apron
hung from the belt (front only) and a slag maul of oak and brass-banded iron in the right fist. Tier II (the Furnace Titan): a
riveted brow plate, cheek plates beside the grille, a third pauldron lame, knee plates and a brass-capped maul.
Tier III (the Brassjaw Unbound): bone horns on the brow, three spikes on each pauldron, ember-seamed greaves and a spiked
maul with a crown spike. No back flag.

  python build_ic_colossus.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ic_ballista"))
from siege_kit import *   # noqa: F401,F403
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import house_stub
import ironclad_tiers as IT

SKIN = (256, 128)   # tier II and III plate, horns and maul parts (128 x 128 was full)
CLOTH = (128, 64)

def hull(base=3, step=4, row=1, seed="h"):
    """Great plates: a rivet row, a seam every step*2 texels across, scuffed."""
    p = plate(IRON, base, spec=False)
    def paint(face, w, h):
        g = p(face, w, h)
        if face not in ("top", "bottom"):
            for y in range(h):
                for x in range(w):
                    if x % (step * 2) == step * 2 - 1: g[y][x] = clamp(IRON, base - 1 + LIT[face])
            for x in range(1, w - 1, step):
                if row < h: g[row][x] = IRON[5]
        return scuff(g, (seed, face, w, h), max(2, w * h // 14), IRON)
    return paint

def banded(inner, rows_at, R=BRASS):
    def paint(face, w, h):
        g = inner(face, w, h)
        if face not in ("top", "bottom"):
            for y in rows_at:
                if 0 <= y < h:
                    for x in range(w): g[y][x] = clamp(R, 3 + LIT[face] + (1 if x % 2 == 0 else 0))
        return g
    return paint

def chest(face, w, h):
    g = hull(4, 3, 1, "chest")(face, w, h)
    if face == "front":
        x0, y0 = 5, 3
        for y in range(y0 - 1, y0 + 6):
            for x in range(x0 - 1, x0 + 9): g[y][x] = BRASS[3 if (x + y) % 2 else 2]
        for y in range(y0, y0 + 5):
            for x in range(x0, x0 + 8): g[y][x] = IRON[0]
        for y in (y0 + 1, y0 + 3):
            for x in range(x0, x0 + 8): g[y][x] = EMBER[5] if x in (x0 + 3, x0 + 4) else EMBER[4]
        for y in (y0 + 2, y0 + 4):
            for x in range(x0, x0 + 8): g[y][x] = EMBER[1]
    return g

def head_painter(face, w, h):
    g = hull(4, 3, 7, "head")(face, w, h)
    if face == "front":
        for x in range(w):
            g[1][x] = IRON[5]
            g[2][x] = g[3][x] = IRON[1]
        for x in (1, 2, 5, 6): g[3][x] = EMBER[5]
    return g

def jaw(face, w, h):
    g = plate(BRASS, 4, spec=False)(face, w, h)
    if face == "front":
        for x in range(w):
            g[0][x] = BRASS[2]
            if x % 2 == 0: g[h - 1][x] = BRASS[6]
    return scuff(g, ("jaw", face, w, h), 5, BRASS)

def stack(face, w, h):
    g = banded(plate(IRON, 3, spec=False), [2, 5, 8])(face, w, h)
    if face == "top":
        for y in range(h):
            for x in range(w): g[y][x] = IRON[0] if 0 < x < w - 1 and 0 < y < h - 1 else IRON[4]
    return scuff(g, ("stack", face), 6, IRON)

def arm(face, w, h):
    return rows([(0, hull(4, 2, 1, "arm")), (14, banded(plate(IRON, 3, spec=False), [0])), (15, hull(3, 2, 2, "arm2")),
                 (27, banded(plate(IRON, 3, spec=False), [1], R=EMBER))])(face, w, h)

def fist(face, w, h):
    g = plate(IRON, 3, spec=False)(face, w, h)
    if face not in ("top", "bottom"):
        for x in range(w): g[h - 1][x] = BRASS[3 if x % 2 else 4]
        for x in range(1, w, 2): g[h - 3][x] = BRASS[5]
    return scuff(g, ("fist", face), 8, IRON)

def leg(face, w, h):
    return rows([(0, hull(4, 2, 1, "leg")), (8, banded(plate(IRON, 3, spec=False), [0])), (9, hull(3, 2, 3, "leg2"))])(face, w, h)

def boot(face, w, h):
    g = hull(3, 3, 1, "boot")(face, w, h)
    if face not in ("top", "bottom"):
        for x in range(w): g[h - 1][x] = EMBER[2]
    return g

def maul_head(face, w, h):
    g = hull(3, 3, 1, "maul")(face, w, h)
    if face in ("left", "right", "front", "back"):
        for x in range(w):
            g[h - 1][x] = BRASS[3 if x % 2 else 4]
            g[0][x] = BRASS[4]
    if face == "front":
        for x in (2, 3): g[h // 2][x] = EMBER[5]            # a vein of slag across the striking face
    return g

def maul_glow(face, w, h):
    g = blank(w, h)
    if face == "front":
        for x in (2, 3): g[h // 2][x] = EMBER[5]
    return g

def apron(T):
    pcog = {"L": BRASS[5], "m": BRASS[3], "d": IRON[0]}
    def paint(face, w, h):
        g = blank(w, h)
        if face != "front": return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (-1 if x in (0, w - 1) else 0) + (-1 if y >= h - 3 else 0))
        g[h - 1] = [EMBER[2] if x % 2 else EMBER[1] for x in range(w)]
        g[h - 2] = [EMBER[4]] * w
        g[h - 3] = [EMBER[3] if x % 2 else EMBER[2] for x in range(w)]
        over(g, grid(COG, pcog), (w - 5) // 2, 3)
        for y in range(h): g[y][0] = g[y][w - 1] = BRASS[2] if y % 2 else BRASS[3]
        return g
    return paint

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 8, 0), [Box((-9, -17, -6), (18, 12, 11), chest), Box((-4.5, -5, -3), (9, 5, 6), banded(plate(IRON, 3, spec=False), [0, 2, 4]))]))
    B.append(Bone("head", "body", (0, -15, -2), [Box((-4, -12, -5.5), (8, 10, 8), head_painter), Box((-1, -5, -7.5), (2, 4, 2), iron_plain(3, "nose", 2))]))
    B.append(Bone("right_arm", "body", (-11, -15, 0), [Box((-2, -2.5, -3), (4, 30, 6), arm)]))
    B.append(Bone("right_hand", "right_arm", (0, 25, 0)))
    B.append(Bone("left_arm", "body", (11, -15, 0), [Box((-2, -2.5, -3), (4, 30, 6), arm)]))
    B.append(Bone("left_hand", "left_arm", (0, 25, 0)))
    B.append(Bone("arms_folded", "body", (0, -12, -1)))
    B.append(Bone("right_leg", "root", (-4, 11, 0), [Box((-3.5, -3, -3), (6, 16, 5), leg)]))
    B.append(Bone("left_leg", "root", (5, 11, 0), [Box((-3.5, -3, -3), (6, 16, 5), leg)]))
    B.append(Bone("jaw", "head", boxes=[Box((-4.5, -4, -6.5), (9, 3, 2), jaw), Box((-4.5, -3, -5), (1, 3, 5), jaw), Box((3.5, -3, -5), (1, 3, 5), jaw)]))
    for side, x in (("r", -8), ("l", 4)):
        B.append(Bone(f"stack_{side}", "body", tag="smoke", boxes=[Box((x, -27, -2), (4, 11, 4), stack)]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_pauldron", "body", (11 * c, -16, 0), rot=(0, 0, 8 * c), boxes=[
            Box((-6 if c < 0 else -2, -3, -6), (8, 4, 12), hull(4, 3, 1, "pa")),
            Box((-6.5 if c < 0 else -2.5, 0.5, -6.5), (9, 3, 13), banded(plate(IRON, 3, spec=False), [1], R=EMBER))]))
        B.append(Bone(f"{side}_lame3", f"{side}_pauldron", stages=[2, 3], boxes=[Box((-6 if c < 0 else -2, 3.5, -6), (8, 2, 12), IT.P("cl" + side, 3), dil=0.05)]))
        B.append(Bone(f"{side}_spikes", f"{side}_pauldron", stages=[3], boxes=[Box((2 * c - 0.5, -6, dz - 0.5), (1, 3, 1), IT.spike(IT.IRON, 3, 3)) for dz in (-3.5, 0, 3.5)]))
        B.append(Bone(f"{side}_kneeplate", f"{side}_leg", stages=[2, 3], boxes=[Box((-3.9, 3, -3.6), (6.8, 3, 1), IT.P("ck" + side, 4), dil=0.05)]))
        B.append(Bone(f"{side}_greave", f"{side}_leg", stages=[3], boxes=[Box((-3.9, 7, -3.4), (6.8, 2, 5.8), IT.P("cg" + side, 3, ember=True), dil=0.05)]))
        B.append(Bone(f"{side}_cheek", "body", stages=[2, 3], boxes=[Box((-9.4 if c < 0 else 4.4, -16, -6.6), (5, 10, 1), IT.P("cc" + side, 3), dil=0.05)]))
        B.append(Bone(f"{side}_horn", "head", stages=[3], boxes=[Box((-6 if c < 0 else 4, -10, -2), (2, 2, 2), IT.horn()), Box((-6.5 if c < 0 else 4.5, -13, -1.5), (2, 3, 1), IT.horn())]))
        B.append(Bone(f"{side}_fist", f"{side}_arm", (0, 25, 0), boxes=[Box((-4, -4, -4), (8, 9, 8), fist)]))
        B.append(Bone(f"{side}_foot", f"{side}_leg", boxes=[Box((-4, 9, -6), (8, 4, 9), boot)]))
    def maul_boxes(t):
        out = [Box((-1, -24, -1), (2, 30, 2), hooped(grain(OAK, "y", 3, "haft", knots=1), [(6, 2), (22, 2)], along="y", R=IRON, trim=BRASS)),
               Box((-4, -33, -3.5), (8, 9, 7), maul_head)]
        if t >= 2:   # brass collars round both ends of the head
            out += [Box((-4.5, -25.5, -4), (9, 1, 8), IT.brass(3, "mc1")), Box((-4.5, -32.5, -4), (9, 1, 8), IT.brass(3, "mc2"))]
        if t >= 3:   # a crown spike, spikes on the striking face and a butt spike
            out += [Box((-1, -36, -1), (2, 3, 2), IT.spike(IT.IRON, 3, 3))]
            out += [Box((x, y, -5), (1, 1, 1.5), IT.spike(IT.IRON, 4, 2)) for x in (-2.5, 1.5) for y in (-31, -27.5)]
            out += [Box((-0.5, 6, -0.5), (1, 2, 1), IT.spike(IT.IRON, 3, 2))]
        return out
    for t in (1, 2, 3):
        B.append(Bone("maul" if t == 1 else f"maul_t{t}", "right_arm", (0, 25, 0), rot=(-18, 0, 0), tag="weapon", weapon_tiers=[t], boxes=maul_boxes(t)))
    B.append(Bone("brow", "head", stages=[2, 3], boxes=[Box((-4.5, -10, -6.5), (9, 2, 1), IT.P("cb", 4), dil=0.05)]))
    B.append(Bone("backplate", "body", boxes=[
        Box((-6.5, -16, 5), (13, 10, 1.5), banded(hull(5, 3, 1, "bp"), [3, 6], R=BRASS)),
        Box((-2, -17, 5.5), (4, 3, 1), iron_plain(5, "vent", 1))]))
    for side, x in (("r", -7), ("l", 5)):
        B.append(Bone(f"flue_{side}", "body", boxes=[Box((x, -17, 3), (2, 12, 2.5), banded(plate(IRON, 4, spec=False), [1, 4, 7, 10]))]))
    B.append(Bone("belt_banner", "body", team=True, boxes=[Box((-4, -4, -3.4), (8, 14, 0), "banner", sheet="cloth")]))
    return B

NOTE = ("Ironclad Forge Colossus (unit identity, Ironclad): a soot-iron golem with a lit furnace grille, twin smokestacks, ember eyes, "
        "a brass jaw, brass-knuckled fists, a team apron and a slag maul. Tier II brow, cheek and knee plates, a third pauldron lame and "
        "brass maul collars; tier III bone horns, pauldron spikes, ember greaves and a spiked maul. No back flag.")

def cloth_painter(spec, side): return apron(TEAM_IC[side][0])

def glow(bone, box):
    if bone.name == "body" and box.size == [18, 12, 11]:
        def paint(face, w, h):
            g = blank(w, h)
            if face == "front":
                for y in (4, 6):
                    for x in (8, 9): g[y][x] = EMBER[5]
            return g
        return paint
    if bone.name == "head" and box.size == [8, 10, 8]:
        def paint(face, w, h):
            g = blank(w, h)
            if face == "front":
                for x in (1, 2, 5, 6): g[3][x] = EMBER[5]
            return g
        return paint
    if bone.name.startswith("maul") and box.size == [8, 9, 7]: return maul_glow
    return None

IC_COLOSSUS = Troop("ic_colossus", SKIN, CLOTH, bones, cloth_painter,
                    {"rig": "colossus", "weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "tall 4"},
                    glow=glow)
def build(): return IC_COLOSSUS.build()
def outputs(): return IC_COLOSSUS.outputs()

if __name__ == "__main__":
    house_stub.main(IC_COLOSSUS, NOTE, sys.argv)
