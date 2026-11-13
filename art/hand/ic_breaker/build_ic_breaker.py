#!/usr/bin/env python3
"""The Ironclad Shieldbreaker (Ironclad house, troop id ic_breaker), hand-authored at one texel per model pixel.

A bull of a man in blackened iron: a closed sallet with an ember brow band and an ember horsehair plume, a fur collar
over lamellar-lapped plate, mail showing at the elbows and knees, a long team tabard cleft by a cloven shield, and
a bearded axe inlaid in ember. Tier II (the Gatebreaker): brass rivets and rims on the pauldrons and cuffs, a brass boss
on each pauldron, a riveted bevor, tassets and heavier knee cops. Tier III (the Wall Sunderer): brass rims on all plate,
a row of three iron spikes along each pauldron, a brass-crowned comb under the plume, ember-seamed tassets, greaves and a
leather forge apron over the thighs. No back flag.

  python build_ic_breaker.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ironclad import *   # noqa: F401,F403
import house_stub
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import ironclad_tiers as IT
from handlib import Troop

SKIN = (128, 128)
CLOTH = (128, 64)
P = pal_ic()

# ---- the sallet ----
HELM_FRONT = fold("""
34555
23445
qrrss
1kkkk
33445
23445
23445
13344
12233""")

def helm_can():
    def paint(face, w, h):
        if face == "front": return grid(HELM_FRONT, P)
        if face == "bottom": return blank(w, h)
        g = lames([3, 2, 2, 2], base=3, seed="hc", wear=4, rivets=False)(face, w, h)
        if face in ("right", "left") and h >= 5:
            near = (w - 2, w - 1) if face == "right" else (0, 1)
            for x in near: g[3][x] = INK
            for x in range(w):
                g[2][x] = EMBER[2 if x % 2 else 1]                 # the brow band runs round the can
        if face == "back":
            for y in range(1, h - 1): g[y][w // 2] = IRON[2]        # the welded seam
            for x in range(w): g[2][x] = EMBER[2 if x % 2 else 1]
        return g
    return paint

def helm_crown():
    def paint(face, w, h):
        g = plate(R=IRON, base=4, spec=False, seam=False)(face, w, h)
        return scuff(g, ("crown", face), 2)
    return paint

def plume():
    return ember_plume("pl")

def gorget_fur():
    return fur(seed="gorget")

# ---- body ----
BREAST = fold("""
.....
.4444
.3555
.3454
.2343
.2232""")  # a hammered breast ridge laid over the lames

def body_painter():
    base = lames([3, 3, 3, 3], base=3, seed="bd", wear=7, rivets=True, ember_rows=())
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for y in range(1, 7):                                  # the breastplate's ridge and the cleave scar
                g[y][w // 2 - 1] = IRON[5] if y in (2, 3) else IRON[4]
                g[y][w // 2] = IRON[2]
            for x in range(w): g[h - 1][x] = EMBER[1 if x % 2 else 2]
        if face == "back":
            for y in range(h): g[y][w // 2] = IRON[2]              # the spine seam
        return g
    return paint

def belt():
    def paint(face, w, h):
        g = leather_ic(base=1)(face, w, h)
        if face == "front":
            for y, row in enumerate(("..tt", "..rs")):
                for x, c in enumerate(row):
                    if c != ".": g[y][2 + x] = P[c]
        return g
    return paint

def arm():
    return rows([(0, mail_ic(3)), (3, lames([3, 3, 2, 1], base=3, seed="arm", wear=3)), (10, leather_ic(base=2, stitch=True, ember_stitch=True))])

def leg():
    return rows([(0, mail_ic(2)), (4, lames([2, 2, 2, 2], base=2, seed="leg", wear=3, rivets=False)), (8, leather_ic(base=2))])

def pauldron_lame(i):
    def paint(face, w, h):
        g = lames([h], base=3 + (1 if i == 0 else 0), seed=("pl", i), wear=3, rivets=(w >= 6))(face, w, h)
        if i == 0 and face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = EMBER[2 if x % 2 else 3]
        return g
    return paint

def knee():
    return lames([2], base=4, seed="knee", wear=1, rivets=False)

def boot():
    return rows([(0, leather_ic(base=2)), (1, leather_ic(base=1, stitch=True)), (2, lames([1], base=2, rivets=False))])

def cuff():
    return rows([(0, lames([2], base=4, rivets=False)), (1, lames([1], base=2, rivets=False))])

# ---- the cloth: tabard, side flaps ----
CLEAVE = """
.LLLLLL.
LddddeeL
LdddeddL
LddeddmL
.LdeddL.
..LedL..
...LL..."""

def cleave_pal(M): return {"e": EMBER[3], "L": IRON[5], "m": IRON[3], "d": IRON[1]}

def tabard(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back") and y >= 7 and x in (1, 6): v -= 1
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face in ("right", "left"):
            for y in range(0, 7):
                for x in range(w): g[y][x] = None
            g[7] = [T[1]] * w
        if face in ("front", "back"):
            over(g, grid(CLEAVE, cleave_pal(M)), 0, 3 if face == "front" else 2)
            for x in range(w): g[h - 1][x] = EMBER[2 if x % 2 else 3]    # the ember bordure at the hem
            for y in range(h): g[y][0] = g[y][w - 1] = EMBER[1 if y % 2 else 2]
        return g
    return paint

def flap(T, M, inner_x):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y >= h - 3 else 0)
                g[y][x] = clamp(T, v)
        g[h - 1] = [EMBER[2 if x % 2 else 3] for x in range(w)]
        g[h - 2] = [EMBER[1]] * w
        return g
    return paint

def cape(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = (2 if face != "back" else 3) + (-1 if face in ("left", "right") else 0) + (-1 if x % 3 == 2 and y > 3 else 0)
                g[y][x] = clamp(T, v)
        if face == "back":
            g[h - 1] = [EMBER[1 if x % 2 else 2] for x in range(w)]
            over(g, grid(CLEAVE, cleave_pal(M)), 1, 3)
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0)))
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-5, -8, -5), (10, 9, 10), IT.T(helm_can(), top=False)), Box((-4, -10, -4), (8, 2, 8), IT.T(helm_crown(), top=False))]))
    B.append(Bone("helm_plume", "head", tag="helm", boxes=[
        Box((-1, -14, -4), (2, 4, 8), ember_plume("pl")),
        Box((-1, -12, 3.5), (2, 9, 2), ember_plume("tail", tail=True), dil=0.1)]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms()
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm(), f"{side}_forearm", ELBOW[side])
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg(), f"{side}_shin", KNEE)
    B.append(Bone("fur_collar", "body", boxes=[Box((-5.5, -12.5, -3), (11, 3, 6), gorget_fur(), dil=0.2)]))
    B.append(Bone("tabard", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "tabard", dil=0.3, sheet="cloth")]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), belt(), dil=0.45)]))
    B.append(Bone("cape", "body", (0, -12, 3.6), team=True, rot=(8, 0, 0), boxes=[Box((-5, 0, 0), (10, 14, 1), "cape", sheet="cloth")]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 14 * c), boxes=[
            Box((-4, -1.5, -4), (8, 2, 8), IT.T(pauldron_lame(0), top=False, foot=True)),
            Box((-4 + 0.5 * c, 0.5, -3.5), (8, 2, 7), IT.T(pauldron_lame(1), top=False, foot=True), dil=0.15),
            Box((-3.5 + 1.0 * c, 2.5, -3), (7, 2, 6), IT.T(pauldron_lame(2), top=False, foot=True), dil=0.15),
            Box((-3 + 1.5 * c, 4.5, -2.5), (6, 1, 5), pauldron_lame(3), dil=0.1)]))
        B.append(Bone(f"{side}_cuff", f"{side}_forearm", boxes=[Box((-2.5, 1, -2.5), (5, 2, 5), IT.T(cuff(), top=True))]))
        B.append(Bone(f"{side}_gauntlet", f"{side}_forearm", stages=[2, 3], boxes=[Box((-2.3, 3, -2.3), (4.6, 2, 4.6), IT.P("bga" + side, 3, ember=True), dil=0.05)]))
        B.append(Bone(f"{side}_knee", f"{side}_shin", boxes=[Box((-2.5, 0, -3), (5, 2, 2), IT.T(knee(), top=False))]))
        B.append(Bone(f"{side}_knee_wing", f"{side}_shin", stages=[2, 3], boxes=[Box((-3, -1, -3.4), (6, 4, 1.4), IT.P("bkw" + side, 4), dil=0.1)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[3], boxes=[Box((-2.4, 2, -2.4), (4.8, 1.5, 4.8), IT.P("bgv" + side, 3), dil=0.1)]))
        B.append(Bone(f"{side}_boot", f"{side}_shin", boxes=[Box((-2.5, 3, -3), (5, 3, 5), IT.T(boot(), top=False))]))
        B.append(Bone(f"{side}_boss", f"{side}_pauldron", stages=[2, 3], boxes=[Box((-1.5, -2.5, -1.5), (3, 1, 3), IT.brass(3))]))
        # each row stands on the back half of its plate, rising 1-2-3 px to the back (model fix 2026-11-13): in the
        # haft jab the raised shoulder sits under the helm's front, which covers the plate's front half (helm into
        # left_spikes 3.52 -> 1.89 at the shove; the right row mirrors it, fix round 1). From the front each row still
        # shows one 3 px spike.
        rows = ((0.5, 1), (2, 2), (3.5, 3))
        B.append(Bone(f"{side}_spikes", f"{side}_pauldron", stages=[3], boxes=[Box((-0.5 + 2.5 * c, -1.5 - h, -0.5 + dz), (1, h, 1), IT.spike(IT.IRON, 3, 3)) for dz, h in rows]))
        B.append(Bone(f"{side}_tasset", "body", stages=[2, 3], boxes=[Box((-5.4 if side == "right" else 4.4, -2, -2.6), (1, 5, 5.2), IT.P("bts" + side, 3, ember=True))]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, boxes=[
            Box((-2, 0, -2.45), (4, 8, 0), ("flap", inner), sheet="cloth"),
            Box((-2, 0, 2.45), (4, 8, 0), ("flap", 3 - inner), sheet="cloth")]))
    # tier II: a riveted bevor; tier III: a brass comb under the plume, a forge apron over the thighs
    B.append(Bone("bevor", "head", tag="helm", stages=[2, 3], boxes=[Box((-5.4, -3, -5.4), (10.8, 3, 1), IT.P("bbv", 3), dil=0.05)]))
    B.append(Bone("comb", "head", tag="helm", stages=[3], boxes=[Box((-1.5, -11, -4.2), (3, 1, 8.4), IT.brass(3))]))
    B.append(Bone("apron", "body", stages=[3], boxes=[Box((-3.5, -1, -2.9), (7, 7, 0.5), leather_ic(base=2, stitch=True, ember_stitch=True))]))
    return B

NOTE = ("Ironclad Shieldbreaker (unit identity, Ironclad): biped, a closed sallet with an ember brow band and plume, fur collar, "
        "lamellar-lapped plate, a long team tabard and cape, a bearded axe. Tier II brass rims, bosses, a bevor and tassets; tier III "
        "pauldron spikes, a brass comb, greaves and a forge apron. No back flag.")

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard": return tabard(T, M)
    if spec == "cape": return cape(T, M)
    return flap(T, M, spec[1])

IC_BREAKER = Troop("ic_breaker", SKIN, CLOTH, bones, cloth_painter,
                   {"weapon": "ic_axe", "hand": "right", "idleArms": "hang", "portrait": "tall 4"})
def build(): return IC_BREAKER.build()
def outputs(): return IC_BREAKER.outputs()

if __name__ == "__main__":
    house_stub.main(IC_BREAKER, NOTE, sys.argv)
