#!/usr/bin/env python3
"""The Tidecaller Tideguard (Tidecallers house, troop id tc_tideguard), hand-authored at one texel per model pixel.

A drowned guardian who braces a trident: bleached coral rising from both shoulders (the mark), a bronze scale cuirass over
barnacled mail, a scallop shell on his back, a verdigris finned cap with the team fin, a barry-wavy team tabard, team wraps
over bronze bracers and bronze greaves, on grey-green drowned flesh. Sea blue is the Tidecallers' team colour and lives on the
cloth only; the metal is bronze and verdigris. Tier II (the Reef Warden): a bronze breastplate rimmed in sea blue, heavier
pauldron plates, greaves and a sea-blue cap band studded in bronze, more barnacles. Tier III (the Coral Sentinel): deep
sea-blue plate chased in warm bronze with pearl rivets, a crown of coral sprigs on the cap, taller shoulder coral and a pearl
at the breast. No back flag.

  python build_tc_tideguard.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
from ironclad import scuff, rng, shade   # noqa: E402
import tidecallers as TC   # noqa: E402

SKIN = (128, 128)
CLOTH = (128, 64)

VERD = ramp("0a1426", "11264a", "1a3f6a", "2a5e8a", "5e88a6", "9db8c4")      # sea blue (Tidecallers accent, guide 10.1): the cap, patina and plate trim keep the same six tone steps as the old verdigris
BRONZE = ramp("2a1608", "553014", "86521e", "b57a30", "d9a455", "f0cf8c")
CORAL = ramp("3f3230", "6f5d57", "a08d82", "cdbcad", "e8dccd", "faf4e8")      # bleached, cooler in the pits
FLESH = ramp("16262a", "2f4a4e", "507174", "7a9c9a", "a6c4c0")                # drowned grey-green
BARN = ramp("2c3234", "52595a", "7e8582", "a9aea6", "cfd2c8")                 # barnacle crust

def bronze_scales(R=BRONZE, base=3, seed="bs"):
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
        return scuff(g, (seed, face), max(1, w * h // 14), R)
    return paint

def barnacled_mail(base=2, seed="bm"):
    """Verdigris mail crusted with grey barnacles in broken patches."""
    def paint(face, w, h):
        g = mail(base=base)(face, w, h)
        for y in range(h):
            for x in range(w):
                if g[y][x] is not None: g[y][x] = clamp(VERD, 1 + (g[y][x] in MAIL[3:]) + (1 if (x + y) % 2 == 0 else 0))
        r = rng(seed, face, w, h)
        for _ in range(max(2, w * h // 7)):
            x, y = r.randrange(w), r.randrange(h)
            g[y][x] = BARN[1 + r.randrange(4)]
        return g
    return paint

def plate_b(R=BRONZE, base=3, seed="pb", rivets=False):
    def paint(face, w, h):
        g = plate(R=R, base=base, spec=False)(face, w, h)
        if rivets and face in ("front", "back") and w >= 5:
            for x in (1, w - 2): g[0][x] = R[5]
        return scuff(g, (seed, face, w, h), max(1, w * h // 14), R)
    return paint

def cuirass():
    """A bronze breastplate of overlapping scales, a sea-green patina creeping up from the hem."""
    def paint(face, w, h):
        g = bronze_scales(BRONZE, 3, "cu")(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w):
                for y in range(h - 2, h):
                    if (x + y) % 2 == 0: g[y][x] = clamp(VERD, 3 if y == h - 2 else 2)
        return g
    return paint

def face_painter_td():
    """A drowned face under the cap: sunken pale eyes, a slack mouth, a gill slash on each cheek."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(FLESH, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0))
        if face == "front":
            for x in range(w): g[3][x] = FLESH[1]                      # the shadowed brow
            g[4][1] = g[4][2] = g[4][5] = g[4][6] = FLESH[0]           # sunken sockets
            g[4][2] = g[4][5] = VERD[5]                                # a pale drowned stare
            g[5][3] = g[5][4] = FLESH[2]
            for x in (2, 3, 4, 5): g[6][x] = FLESH[1]                  # the slack mouth
            for y in (4, 5, 6): g[y][0] = g[y][7] = FLESH[1] if y % 2 else FLESH[2]   # gill slashes
        if face in ("left", "right"):
            g[4][3] = g[5][3] = FLESH[1]
        if face == "back":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(FLESH, 2 + (x + y) % 2)
        return scuff(g, ("tdf", face), 3, FLESH)
    return paint

def cap():
    """A verdigris cap with riveted bands and a barnacle or two."""
    def paint(face, w, h):
        g = plate(R=VERD, base=3, spec=False, seam=False)(face, w, h)
        if face == "bottom": return blank(w, h)
        if face == "top":
            for y in range(h): g[y][w // 2] = VERD[1]
            return scuff(g, ("tdc", face), 5, VERD)
        for x in range(w): g[h - 1][x] = BRONZE[2 if x % 2 else 3]     # a bronze brim band
        for x in range(1, w, 3): g[1][x] = BRONZE[4]
        r = rng("tdc", face)
        for _ in range(3):
            g[r.randrange(h - 1)][r.randrange(w)] = BARN[2 + r.randrange(3)]
        return g
    return paint

def coral(seed, tip=False):
    """A bleached coral branch: pitted, ribbed along its length, the tip a shade warmer."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("coral", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + (1 if face == "top" else 0) + LIT[face] // 2 + r.choice((-1, 0, 0, 0, 1))
                if (x + y // 2) % 3 == 0: v -= 1
                g[y][x] = clamp(CORAL, v)
        for _ in range(max(1, w * h // 9)):
            g[r.randrange(h)][r.randrange(w)] = CORAL[1]              # pits
        return g
    return paint

def shell():
    """A scallop shell: radiating ribs from the hinge, a bronze-green rim."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back"):
                    v += (1 if x % 2 == 0 else -1)
                    if y == h - 1: v -= 1
                g[y][x] = clamp(CORAL, v + (0 if face != "back" else 0))
        if face in ("front", "back"):
            for x in range(w): g[0][x] = BRONZE[3 if x % 2 else 2]
        return g
    return paint

def bracer():
    return plate_b(BRONZE, 3, "br", rivets=True)

def leg_painter():
    return rows([(0, plate_b(FLESH, 2, "tdl")), (6, plate_b(BRONZE, 3, "gr", rivets=True))])

def boot():
    return plate_b(BRONZE, 2, "bt")

def belt():
    def paint(face, w, h):
        g = leather(base=1, stitch=True)(face, w, h)
        if face == "front":
            for x in range(w // 2 - 1, w // 2 + 1):
                for y in range(h): g[y][x] = BRONZE[4 if y == 0 else 3]
        return g
    return paint

def body_painter():
    return rows([(0, cuirass()), (7, barnacled_mail(2, "tdm"))])

def arm_painter():
    return rows([(0, barnacled_mail(2, "tda")), (6, plate_b(BRONZE, 3, "arm", rivets=False))])

def pauldron():
    return plate_b(VERD, 3, "tdp", rivets=True)

# ---- cloth ----
def tabard(T, M):
    """A barry-wavy tabard: team and a darker team in wavy bars, a ragged hem."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                wave = (y + (1 if (x // 2) % 2 else 0)) // 2 % 2 == 0
                v = (4 if wave else 2) + LIT[face] + (1 if y == 0 else 0)
                g[y][x] = clamp(T, v)
        r = rng("td-tab", face)
        for x in range(w):
            if r.random() < 0.4: g[h - 1][x] = None
        return g
    return paint

def wrap_cloth(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (1 if y % 2 == 0 else -1) + (lightx(face, x, w) if face not in ("top", "bottom") else 0))
        return g
    return paint

def fin(T, M):
    """The team fin along the cap's crest: dark at the root, bright at the edge, ribbed."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("left", "right"): return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 4 - (y // 2) + (1 if x % 2 == 0 else 0) - (1 if face == "left" else 0))
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face_painter_td())]))
    B.append(Bone("cap", "head", tag="helm", boxes=[Box((-4.5, -9.5, -4.5), (9, 5, 9), cap())]))
    B.append(Bone("fin", "head", team=True, tag="helm", boxes=[Box((-0.5, -13, -3), (1, 4, 7), "fin", sheet="cloth")]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms()
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm_painter(), f"{side}_forearm", ELBOW[side])
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg_painter(), f"{side}_shin", KNEE)
    B.append(Bone("belt", "body", boxes=[Box((-4, -4, -2), (8, 2, 4), belt(), dil=0.5)]))
    # the tabard's two panels hang 2 px below the hips (was 5): a thigh stepping under a longer hem cleared one leg into the
    # other at every pose change (armour_clip 1.92-1.93, model fix 2026-11-13); its top rises 1 px to keep it 7 rows long
    B.append(Bone("tabard", "body", team=True, boxes=[
        Box((-3.5, -5, -2.95), (7, 7, 0), "tabard", sheet="cloth"), Box((-3.5, -5, 2.95), (7, 7, 0), "tabard", sheet="cloth")]))
    B.append(Bone("shell", "body", (0, -8, 3.2), rot=(8, 0, 0), boxes=[Box((-2.5, -2.5, 0), (5, 5, 1), shell()), Box((-1.5, -3.5, 0.2), (3, 1, 1), shell())]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 10 * c), boxes=[
            Box((-3.5 + 0.5 * c, -2, -3), (7, 2, 6), pauldron())]))
        # coral rises from the shoulder: a trunk, two branches, bleached tips
        B.append(Bone(f"{side}_coral", "body", (4.5 * c, -12, 0), rot=(0, 0, 8 * c), boxes=[
            Box((-1.5, -6, -1.5), (3, 6, 3), coral((side, 0))),
            Box((-2.5 + 3 * (c > 0), -9, -1), (2, 4, 2), coral((side, 1))),
            Box((0.5 - 3 * (c > 0), -10, -0.5), (1, 5, 1), coral((side, 2))),
            Box((-1, -8, -1), (1, 3, 1), coral((side, 3)))]))
        B.append(Bone(f"{side}_bracer", f"{side}_forearm", boxes=[Box((-1 if side == "right" else -4, 1, -2), (4, 5, 4), bracer(), dil=0.15)]))
        B.append(Bone(f"{side}_wrap", f"{side}_arm", team=True, boxes=[Box((-2 + (0 if side == "right" else -1), 3, -2), (4, 2, 4), "wrap", dil=0.3, sheet="cloth")]))   # centred on the elbow: stays on the upper arm
        B.append(Bone(f"{side}_boot", f"{side}_shin", boxes=[Box((-2.5, 3, -3), (5, 3, 5), boot())]))
    # tier II, the Reef Warden: bronze plate rimmed in sea blue; tier III deep sea-blue plate chased in warm bronze
    B.append(Bone("breastplate", "body", stages=[2, 3], boxes=[Box((-4.5, -12.2, -2.7), (9, 6.5, 5.4), TC.P("tgbp"), dil=0.15)]))
    B.append(Bone("cap_band", "head", tag="helm", stages=[2, 3], boxes=[Box((-4.9, -5.6, -4.9), (9.8, 1.4, 9.8), TC.band("tgcb"), dil=0.05)]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_plate", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 14 * c), stages=[2, 3], boxes=[
            Box((-4 + 0.5 * c, -0.2, -3.5), (8, 3, 7), TC.P("tgpd" + side), dil=0.2)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[2, 3], boxes=[Box((-2.3, -2.5, -2.3), (4.6, 5.5, 4.6), TC.P("tggv" + side), dil=0.1)]))
        B.append(Bone(f"{side}_cuff", f"{side}_forearm", stages=[2, 3], boxes=[Box((-1.3 if side == "right" else -4.3, 5.6, -2.3), (4.6, 1, 4.6), TC.band("tgcf" + side), dil=0.1)]))
        # tier III: the shoulder coral grows a third branch
        B.append(Bone(f"{side}_coral3", "body", (4.5 * c, -12, 0), rot=(0, 0, 22 * c), stages=[3], boxes=[
            Box((-0.5 + 2 * c, -12, -0.5), (1, 6, 1), TC.tc_coral((side, "c3a"))),
            Box((-1 + 3 * c, -9, 0), (2, 1, 1), TC.tc_coral((side, "c3b")))]))
    # tier III, the Coral Sentinel: a crown of coral sprigs on the cap and a pearl set in the breastplate
    for i, (x, z, h) in enumerate([(-3.5, -3.5, 3), (-1.5, -4.2, 5), (0.5, -4.4, 4), (2.5, -4.0, 5), (3.8, -2.0, 3), (-3.9, -1.0, 3)]):
        B.append(Bone(f"crown_coral{i}", "head", tag="helm", stages=[3], boxes=[Box((x - 0.5, -9.5 - h, z - 0.5), (1, h, 1), TC.tc_coral(("cr", i)))]))
    B.append(Bone("breast_pearl", "body", stages=[3], boxes=[Box((-1, -10.5, -3.3), (2, 2, 1), TC.tc_pearl())]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard": return tabard(T, M)
    if spec == "fin": return fin(T, M)
    return wrap_cloth(T, M)

NOTE = ("Tidecaller Tideguard (unit identity, Tidecallers): drowned, biped, upright; bleached coral rising from both shoulders, a bronze scale "
        "cuirass over barnacled mail, a scallop shell on the back, a verdigris finned cap with the team fin, a barry-wavy team tabard, team "
        "wraps over bronze bracers, bronze greaves. The coral trident is braced in the right hand. Tier II bronze plate rimmed in sea blue; "
        "tier III deep sea-blue plate chased in warm bronze, pearl rivets and a crown of coral sprigs. No back flag.")
TC_TIDEGUARD = Troop("tc_tideguard", SKIN, CLOTH, bones, cloth_painter,
                     {"weapon": "coral_trident", "hand": "right", "idleArms": "hang", "portrait": "standard"})
def build(): return TC_TIDEGUARD.build()
def outputs(): return TC_TIDEGUARD.outputs()

if __name__ == "__main__":
    house_stub.main(TC_TIDEGUARD, NOTE, sys.argv)
