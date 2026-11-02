#!/usr/bin/env python3
"""The Tidecaller Harpooner (Tidecallers house, troop id tc_harpooner), hand-authored at one texel per model pixel.

A drowned whaler who stands tall and throws: a weathered oilskin sou'wester (the signature) with a short brim all round and a long
tail hanging down the back of the neck, a banded sea-blue hatband, a long navy oilskin coat edged in sea blue over navy trousers
and tarred wader boots, a coil of rope slung over one shoulder, a quiver of spare harpoon heads on the back on a sea-blue baldric,
barnacles and kelp, on grey-green drowned flesh with gill slits. Team cloth is the neck scarf, a hip sash and the wrist wraps.
No coral (the Trident Guard's). Tier I only. No back flag. The harpoon rides in the right hand.

  python build_tc_harpooner.py preview <dir> [side] | contact <png> [side] | stub | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "tc_tideguard"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
import build_tc_tideguard as TG
from build_tc_tideguard import VERD as SEA, BRONZE, FLESH, BARN, scuff, rng

SKIN = (128, 128)
CLOTH = (128, 64)

NAVY = ramp("0e1320", "18202f", "232e42", "33425a", "4e6078")
OIL = ramp("1c1608", "382b10", "5a4818", "80692a", "a38a3e", "c4ad62")          # tarred, weathered yellow oilskin
ROPE = ramp("2a2218", "4a3d2b", "6e5d44", "8f7d5e", "b09d78")
KELP = ramp("0e170a", "1d2f12", "2f4a1c", "4a6a2a", "6a8a40", "8fa56d")
TAR = ramp("0c0a0a", "1c1612", "30261c", "483a2a", "62503a")                     # tarred leather and wood
IRON = ramp("12141a", "262a33", "3f4550", "5d6672", "808a96", "a9b2bb")          # spare harpoon heads, salt-pitted

def lit(face, x, w): return LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

def navy_oil(base=2, seed="no", trim=False):
    """Navy oilskin: a wet diagonal sheen, vertical seams, barnacle specks."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + lit(face, x, w)
                if face not in ("top", "bottom"):
                    if (x * 2 + y) % 7 == 0: v += 1
                    if x % 5 == 4: v -= 1
                g[y][x] = clamp(NAVY, v)
        r = rng(seed, face, w, h)
        for _ in range(max(1, w * h // 26)):
            g[r.randrange(h)][r.randrange(w)] = BARN[1 + r.randrange(3)]
        return g
    return paint

def coat():
    """The long coat on the body: buttoned front with a sea-blue edge and sea-blue buttons, a high sea-blue collar, a sea-blue yoke."""
    base = navy_oil(3, "hc")
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("front", "back", "left", "right"):
            for x in range(w): g[0][x] = SEA[5 if x % 2 else 4]               # the collar edge
            for x in range(w): g[1][x] = SEA[3 if x % 2 else 2]
        if face == "front":
            for y in range(2, h):
                g[y][w // 2 - 1] = SEA[3]; g[y][w // 2] = SEA[4]               # the placket
            for y in range(3, h, 3): g[y][w // 2 - 1] = BARN[3]                # buttons
            for x in range(w): g[h - 1][x] = SEA[2]
        if face == "back":
            for x in range(w): g[3][x] = SEA[2 if x % 2 else 1]                # a yoke seam
        return g
    return paint

def arm_painter():
    def paint(face, w, h):
        g = rows([(0, navy_oil(3, "ha")), (6, TG.plate_b(FLESH, 2, "hf"))])(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[5][x] = SEA[4 if x % 2 else 5]                  # the sea-blue cuff
            for x in range(w): g[4][x] = SEA[2]
        return g
    return paint

def skirt():
    """The coat's long skirts: oilskin to the knee, a sea-blue hem band, a ragged edge, a back vent."""
    base = navy_oil(3, "hk")
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "top": return blank(w, h)
        if face == "bottom": return blank(w, h)
        for x in range(w): g[h - 2][x] = SEA[3 if x % 2 else 4]
        r = rng("hk-hem", face, w, h)
        for x in range(w):
            if r.random() < 0.45: g[h - 1][x] = None
            elif g[h - 1][x] is not None: g[h - 1][x] = SEA[1]
        if face == "back":
            for y in range(h): g[y][w // 2] = NAVY[0]                            # the vent
        return g
    return paint

def trousers():
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("ht", face, w, h)
        for y in range(h):
            for x in range(w):
                v = 2 + lit(face, x, w)
                if (x + y) % 4 == 0: v += 1
                if r.random() < 0.08: v -= 1
                g[y][x] = clamp(NAVY, v)
        return g
    return paint

def wader():
    """Tarred wader boots: a sea-blue turned cuff, then wet leather crusted with barnacles."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("hw", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(TAR, 2 + lit(face, x, w) + r.choice((-1, 0, 0, 1)))
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = SEA[4 if x % 2 else 3]
            for x in range(w): g[1][x] = SEA[2]
            for _ in range(max(2, w * h // 9)): g[r.randrange(2, h)][r.randrange(w)] = BARN[1 + r.randrange(4)]
        return g
    return paint

def leg_painter():
    return rows([(0, trousers()), (7, wader())])

def foot():
    return TG.plate_b(TAR, 1, "hft")

def belt():
    """A thick rope-cord belt with a sea-blue knot at the front."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(ROPE, 2 + LIT[face] + (1 if (x + y) % 3 == 0 else -1 if (x + y) % 3 == 1 else 0))
        if face == "front":
            for x in range(w // 2 - 1, w // 2 + 1):
                for y in range(h): g[y][x] = SEA[4 if y == 0 else 3]
        return g
    return paint

def face_painter_harp():
    """A drowned face under the brim: heavy brim shadow, sunken pale eyes, slack mouth, gill slits on both cheeks, a barnacled jaw."""
    FL = FLESH
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("hfc", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(FL, 3 + lit(face, x, w) + r.choice((-1, 0, 0, 0, 1)))
        if face in ("front", "left", "right"):
            for x in range(w): g[0][x] = FL[0]
            for x in range(w): g[1][x] = FL[1] if x % 2 else FL[0]                      # the brim shadow
        if face == "front":
            for x in range(w): g[3][x] = FL[1]
            g[4][1] = g[4][2] = g[4][5] = g[4][6] = FL[0]
            g[4][2] = g[4][5] = hx("cff3e4")                                            # the pale sea-foam stare
            g[5][3] = g[5][4] = FL[2]
            for x in (2, 3, 4, 5): g[6][x] = FL[1]
            g[6][3] = g[6][4] = FL[0]
            for y in (4, 5, 6): g[y][0] = g[y][7] = FL[1] if y % 2 else FL[0]            # gill slits
            for x in (1, 6): g[7][x] = BARN[2]
        if face in ("left", "right"):
            for y in (4, 5, 6): g[y][3] = FL[0] if y % 2 else FL[1]
            g[5][2] = g[5][4] = FL[1]
        if face == "back":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(FL, 2 + (x + y) % 2)
        return scuff(g, ("hfc", face), 3, FL)
    return paint

# ---- the sou'wester ----
def hat(seed="hc", crown=False, band=False):
    """Oilskin hat: stitched panel lines, a wet sheen, a worn crease. band puts the sea-blue hatband on the lowest rows."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("hat", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + lit(face, x, w) + r.choice((0, 0, 0, 0, 1))
                if face not in ("top", "bottom"):
                    if (x * 2 + y) % 6 == 0: v += 1
                    if x % 4 == 3: v -= 1
                if face == "top":
                    ring = max(abs(x - (w - 1) / 2), abs(y - (h - 1) / 2))
                    v = 3 + (1 if ring < 1.5 else 0) - (1 if ring > (w - 1) / 2 - 1 else 0)
                g[y][x] = clamp(OIL, v)
        if face == "bottom": return [[OIL[0]] * w for _ in range(h)]
        if band and face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[4 if x % 2 else 3]
            for x in range(w): g[h - 2][x] = SEA[2]
        if face in ("front", "back") and w >= 8 and not crown:
            for y in range(h): g[y][w // 2] = OIL[1]                                    # the centre seam
        return scuff(g, ("hat", seed, face), max(1, w * h // 24), OIL)
    return paint

def brim(seed="br"):
    """The brim all round: bright on top, the sea-blue edge, a dark underside."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("brim", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                if face == "bottom": g[y][x] = OIL[0]; continue
                v = 3 + (1 if face == "top" else lit(face, x, w) - 1) + r.choice((-1, 0, 0, 1))
                g[y][x] = clamp(OIL, v)
        if face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[3 if x % 2 else 4]
            for x in range(w): g[h - 2][x] = clamp(OIL, 2 + (1 if x % 3 == 0 else 0))
        return g
    return paint

def tail(seed="tl"):
    """The long back brim: a spoon of oilskin hanging past the nape, ribbed, hem frayed and edged in sea blue."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("tail", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = (3 if face == "back" else 2) + (1 if y < 2 else 0) + r.choice((-1, 0, 0, 1))
                if face in ("front", "back") and x % 3 == 1: v -= 1                     # ribs running down the tail
                if face in ("front", "back") and (x * 2 + y) % 7 == 0: v += 1
                g[y][x] = clamp(OIL, v)
        if face in ("front", "back"):
            for x in range(w): g[h - 2][x] = SEA[3 if x % 2 else 4]
            for x in range(w):
                if r.random() < 0.45: g[h - 1][x] = None
                elif r.random() < 0.7: g[h - 1][x] = SEA[1]
            for _ in range(2): g[r.randrange(h - 2)][r.randrange(w)] = BARN[2 + r.randrange(3)]
        return g
    return paint

# ---- rope, quiver, kelp, barnacles ----
def rope(seed="rp"):
    """A coil of rope: diagonal twisted strands, dark between the turns."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("rope", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + (1 if (x + y) % 3 == 0 else (-1 if (x + y) % 3 == 1 else 0)) + (LIT[face] if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(ROPE, v + r.choice((0, 0, 0, 1)))
        if face in ("front", "back") and h >= 6:
            for y in (2, h - 3):
                for x in range(w): g[y][x] = ROPE[0]                                     # the whipped bindings
        return g
    return paint

def kelp(seed, hem=2):
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("kelp", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] // 2 + r.choice((-1, 0, 0, 1))
                if face in ("front", "back") and (x == w // 2 or w == 1): v -= 1
                if y % 3 == 2 and face in ("front", "back") and x != w // 2: v += 1
                g[y][x] = clamp(KELP, v)
        for y in range(h - hem, h):
            for x in range(w):
                if r.random() < 0.3 + 0.3 * (y - (h - hem)): g[y][x] = None
        return g
    return paint

def quiver():
    """A tarred leather quiver bound with a sea-blue strap, rope whipping at the mouth."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("qv", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(TAR, 3 + lit(face, x, w) + r.choice((-1, 0, 0, 1)))
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = ROPE[3 if x % 2 else 2]
            for x in range(w): g[1][x] = ROPE[1]
            for x in range(w): g[h // 2][x] = SEA[3 if x % 2 else 4]
            for _ in range(2): g[r.randrange(2, h)][r.randrange(w)] = BARN[2 + r.randrange(2)]
        return g
    return paint

def head_iron(seed="hi"):
    """A spare harpoon head: pitted iron, bright at the edge."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("hi", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(IRON, 4 + lit(face, x, w) + r.choice((-1, 0, 0, 1)) + (1 if y == 0 else 0))
        return g
    return paint

def baldric():
    """The sea-blue baldric across the chest: a woven strap with pale stitches."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (1 if y % 2 == 0 else 0) - (1 if x == 0 else 0)
                g[y][x] = clamp(SEA, v)
        if face in ("front", "back"):
            for y in range(1, h, 3):
                g[y][w // 2] = SEA[5]
        return g
    return paint

def barnacle():
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("hbc", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BARN, 2 + LIT[face] + r.choice((-1, 0, 1)))
        return g
    return paint

def shoulder_pad():
    """An oilskin shoulder cape-piece with a sea-blue edge and a barnacle crust."""
    base = navy_oil(2, "hp")
    def paint(face, w, h):
        g = base(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[3 if x % 2 else 4]
        else:
            for x in range(w): g[0][x] = SEA[3]
        return g
    return paint

# ---- cloth ----
def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec in ("tail", "sash"): return TG.tabard(T, M)
    return TG.wrap_cloth(T, M)

# ---- assembling ----
def strand(name, parent, pivot, rot, boxes):
    return Bone(name, parent, pivot, [Box(o, s, kelp((name, i))) for i, (o, s) in enumerate(boxes)], rot=rot)

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), coat())]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face_painter_harp())]))
    # the sou'wester: a high rounded crown with a sea-blue band, a short brim all round, and the long tail down the back
    B.append(Bone("hat", "head", tag="helm", boxes=[
        Box((-4.6, -9.6, -4.6), (9.2, 4.6, 9.2), hat("lo", band=True)),
        Box((-3.8, -11.4, -3.8), (7.6, 2, 7.6), hat("hi", crown=True)),
        Box((-2.4, -12.2, -2.4), (4.8, 1, 4.8), hat("top", crown=True))]))
    B.append(Bone("brim", "head", tag="helm", boxes=[Box((-5.6, -6.2, -6.2), (11.2, 2.2, 4.4), brim("f")),
                                                       Box((-5.6, -6.2, -1.8), (1.6, 2.2, 6.8), brim("l")),
                                                       Box((4, -6.2, -1.8), (1.6, 2.2, 6.8), brim("r"))]))
    B.append(Bone("brim_tail", "head", (0, -6.2, 4.4), rot=(-20, 0, 0), tag="helm", boxes=[Box((-5.2, 0, 0), (10.4, 9, 1.6), tail())]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm_painter())]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm_painter())]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg_painter())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg_painter())]))
    B.append(Bone("skirt", "body", boxes=[Box((-4.6, -3, -2.6), (9.2, 10, 5.2), skirt())]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -4, -2), (8, 2, 4), belt(), dil=0.5)]))
    B.append(Bone("sash", "body", team=True, boxes=[Box((-3, -3.5, -2.9), (2, 8, 0), "sash", sheet="cloth")]))
    B.append(Bone("scarf", "body", team=True, boxes=[Box((-4.4, -12.7, -2.4), (8.8, 2, 4.8), "scarf", sheet="cloth")]))
    # rope coil over the left shoulder: a bridge across the shoulder, a front and a back drape slanting to the right hip
    B.append(Bone("rope_bridge", "body", boxes=[Box((2.2, -14, -3.2), (5.6, 2.4, 6.4), rope("br"))]))
    B.append(Bone("rope_front", "body", (5, -12, -3.2), rot=(0, 0, 32), boxes=[Box((-1.5, 0, -0.6), (3.2, 10, 1.6), rope("fr"))]))
    B.append(Bone("rope_back", "body", (5, -12, 2.4), rot=(0, 0, 32), boxes=[Box((-1.5, 0, -0.6), (3.2, 10, 1.6), rope("bk"))]))
    # the sea-blue baldric and the quiver of spare heads on the back
    B.append(Bone("baldric", "body", (-4, -12, 0), rot=(0, 0, -36), boxes=[Box((-1, 0, -2.3), (2, 14, 0.4), baldric()), Box((-1, 0, 1.9), (2, 14, 0.4), baldric())]))
    B.append(Bone("quiver", "body", (-1, -2, 2.4), rot=(0, 0, -18), boxes=[
        Box((-1.5, -9, 0), (3, 10, 2.4), quiver()),
        Box((-1.2, -11.5, 0.4), (1, 2.6, 1), head_iron("a")), Box((-1.7, -10.7, 0.4), (2, 1, 1), head_iron("b")),
        Box((0.6, -12.5, 0.8), (1, 3.4, 1), head_iron("c")), Box((0.1, -11.5, 0.8), (2, 1, 1), head_iron("d"))]))
    B.append(Bone("barnacles", "body", boxes=[
        Box((1, -9.5, -2.7), (2, 1, 1), barnacle()), Box((-3.4, -6.5, -2.7), (1, 1, 1), barnacle()),
        Box((-5.1, -11.5, -1), (1, 2, 2), barnacle()), Box((4.4, -4.5, 0), (1, 1, 2), barnacle()), Box((-3, -5, 2.7), (2, 1, 1), barnacle())]))
    B.append(Bone("pad_right", "body", boxes=[Box((-8, -12.5, -2.9), (4.4, 1.8, 5.8), shoulder_pad())]))
    B.append(strand("kelp_pad", "body", (-7, -10.5, 2.5), (22, 0, -8), [((-1, 0, 0), (2, 8, 1))]))
    B.append(strand("kelp_belt", "body", (3, -3, 2.5), (20, 0, 5), [((-1, 0, 0), (2, 9, 1))]))
    B.append(strand("kelp_hat", "head", (-4.5, -6, 2), (10, 0, -14), [((-1, 0, 0), (2, 9, 1))]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_wrap", f"{side}_arm", team=True, boxes=[Box((-2 + (0 if side == "right" else -1), 7, -2), (4, 2, 4), "wrap", dil=0.3, sheet="cloth")]))
        B.append(Bone(f"{side}_foot", f"{side}_leg", boxes=[Box((-2.5, 9, -3.2), (5, 3, 5.2), foot())]))
    B.append(strand("kelp_shin", "left_leg", (1, 6, 2.2), (14, 0, 6), [((-1, 0, 0), (2, 6, 1))]))
    return B

NOTE = ("Tidecaller Harpooner (unit identity, Tidecallers): drowned whaler, biped, upright; an oilskin sou'wester with a banded sea-blue hat "
        "and a long brim tail down the back (the signature), a long navy oilskin coat edged in sea blue, navy trousers, tarred barnacled waders, "
        "a rope coil over the left shoulder, a quiver of spare harpoon heads on a sea-blue baldric on the back, kelp and barnacles, a team "
        "neck scarf, hip sash and wrist wraps, grey-green drowned flesh with gill slits. The harpoon is in the right hand. No coral. Tier I only. "
        "No back flag.")
TC_HARPOONER = Troop("tc_harpooner", SKIN, CLOTH, bones, cloth_painter,
                     {"weapon": "harpoon", "hand": "right", "idleArms": "hang", "portrait": "tall 2"}, tiers=(1,))
def build(): return TC_HARPOONER.build()
def outputs(): return TC_HARPOONER.outputs()

if __name__ == "__main__":
    house_stub.main(TC_HARPOONER, NOTE, sys.argv)
