#!/usr/bin/env python3
"""The Tidecaller Drowned Raider (Tidecallers house, troop id tc_raider), hand-authored at one texel per model pixel.

A drowned light skirmisher who leans into the charge: a green turtle-shell helmet (the signature) edged in sea blue, a bloated
grey-green body with white blind eyes, a short sea-blue oilskin coat hanging open on a swollen belly, tattered navy trousers,
barnacles crusting shoulder and hip, kelp streaming back from helm, shoulders, arms, belt and shin, a rolled net on the back.
Team cloth is the neck scarf, a hip sash and the wrist wraps. No coral (the Trident Guard's). Tier II (the Reef Reaver): a bronze pauldron rimmed in sea blue on the sword
shoulder, bracers, greaves and a sea-blue gorget band. Tier III (the Wreck Captain): deep sea-blue plate chased in warm bronze
on both shoulders, shell barnacles crusting helm and shoulders, a pearl-studded helm rim. No back flag.
The cutlass rides in the right hand.

  python build_tc_raider.py preview <dir> [side] | contact <png> [side] | stub | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "tc_tideguard"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import tidecallers as TC
import build_tc_tideguard as TG
from build_tc_tideguard import VERD as SEA, BRONZE, FLESH, BARN, barnacled_mail, scuff, rng

SKIN = (128, 128)
CLOTH = (128, 64)
HUNCH = 16     # the raider leans into the charge

SHELL = ramp("0d1a10", "1c3322", "325238", "4d7448", "6f9560", "9bb883")        # a green turtle's carapace, lights eased under the tint rule
SCUTE = ramp("2a2a16", "4b4a28", "6e6a3c", "958e58", "b9b07a")                   # the pale horn between the plates
KELP = ramp("0e170a", "1d2f12", "2f4a1c", "4a6a2a", "6a8a40", "8fa56d")
NAVY = ramp("0e1320", "18202f", "232e42", "33425a", "4e6078")
ROPE = ramp("2a2218", "4a3d2b", "6e5d44", "8f7d5e", "b09d78")
BLOAT = ramp("16262a", "2a4246", "456466", "6a8a86", "93b0a8", "bcd0c8")         # bloated, waterlogged flesh: paler than the Guard's

def oilskin(base=2, seed="os"):
    """A sea-blue oilskin: wet diagonal sheen, vertical seams, a sea-blue edge, a barnacle crust."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face not in ("top", "bottom"):
                    if x % 4 == 3 and y % 5 > 1: v -= 1                        # a long seam, broken
                    if y == 0: v += 1
                g[y][x] = clamp(SEA, v)
        r = rng(seed, face, w, h)
        for _ in range(w * h // 70):
            g[r.randrange(h)][r.randrange(w)] = BARN[1 + r.randrange(3)]
        return g
    return paint

def coat():
    """The coat on the body: oilskin hanging open on a swollen belly, a sea-blue collar and a sea-blue stitched front edge."""
    base = oilskin(2, "rc")
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for y in range(3, h):
                for x in range(w // 2 - 1, w // 2 + 2): g[y][x] = clamp(FLESH, 2 + (1 if (x + y) % 3 == 0 else 0))   # the opening on the belly
            for y in range(3, h):
                g[y][w // 2 - 2] = SEA[5 if y % 2 else 4]; g[y][w // 2 + 2] = SEA[5 if y % 2 else 4]
        if face in ("front", "back", "left", "right"):
            for x in range(w): g[0][x] = SEA[5 if x % 2 else 4]               # the collar edge
            g[1][0] = g[1][w - 1] = SEA[3]
        return g
    return paint

def sleeve():
    return rows([(0, oilskin(2, "rs")), (5, TG.plate_b(BLOAT, 3, "rw")), ])

def arm_painter():
    def paint(face, w, h):
        g = rows([(0, oilskin(2, "ra")), (8, TG.plate_b(FLESH, 2, "rf"))])(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[7][x] = SEA[4]                                  # the sea-blue cuff
        return g
    return paint

def skirt():
    """The coat's tails: oilskin to the thigh, a ragged hem with a sea-blue edge."""
    base = oilskin(2, "rk")
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return blank(w, h) if face == "bottom" else g
        for x in range(w): g[h - 2][x] = SEA[4 if x % 2 else 3]
        r = rng("rk-hem", face, w, h)
        for x in range(w):
            if r.random() < 0.45: g[h - 1][x] = None
            if r.random() < 0.15: g[h - 2][x] = None
        return g
    return paint

def trousers():
    """Tattered navy cloth, patched and sea-stained."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("rt", face, w, h)
        for y in range(h):
            for x in range(w):
                v = 2 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(NAVY, v)
        if face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[1 if x % 2 else 2]            # a frayed, stained hem
        return g
    return paint

def leg_painter():
    return rows([(0, trousers()), (7, TG.plate_b(FLESH, 2, "rlg"))])

def foot():
    return TG.plate_b(FLESH, 1, "rft")

def belt():
    """A cord-wound belt with a worn bronze buckle (the lights eased under the tint rule)."""
    def paint(face, w, h):
        g = leather(base=1, stitch=True)(face, w, h)
        if face == "front":
            for x in range(w // 2 - 1, w // 2 + 1):
                for y in range(h): g[y][x] = hx("a38a5e") if y == 0 else hx("8f6530")
        return g
    return paint

def belly():
    """A bloated, waterlogged belly: pale, mottled, stretched lines."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("belly", face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face == "front" and x == w // 2 and 0 < y < h - 1: v -= 1   # the line down the belly
                g[y][x] = clamp(BLOAT, v)
        return g
    return paint

def face_painter_raider():
    """A bloated drowned face: puffed cheeks, blind white eyes under a heavy brow, a slack gaping mouth, gill slashes."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("rfc", face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + r.choice((-1, 0, 0, 0, 0, 1))
                g[y][x] = clamp(BLOAT, v - 1)
        if face == "front":
            for x in range(w): g[2][x] = BLOAT[1]                                # the shadowed brow
            for x in (1, 2, 6, 7): g[3][x] = BLOAT[0]                            # sockets
            for x in (1, 2, 6, 7): g[4][x] = hx("e8f0ec")                        # white eyes
            g[4][2] = g[4][6] = hx("c4d2cf")                                     # a milky glint
            for y in (5, 6): g[y][0] = g[y][w - 1] = BLOAT[2]                    # puffed cheeks
            g[5][3] = g[5][5] = BLOAT[2]                                         # nose wings
            for x in range(2, 7): g[6][x] = BLOAT[0]                             # the gaping mouth
            for x in (3, 5): g[6][x] = hx("c9d0c6")                              # broken teeth
            for x in range(3, 6): g[7][x] = BLOAT[1]
        if face in ("left", "right"):
            for y in (4, 5, 6): g[y][3] = BLOAT[1] if y % 2 else BLOAT[0]        # gill slashes
        return scuff(g, ("rfc", face), 3, BLOAT)
    return paint

def scutes(seed="sh", top_ridge=False, rim=False):
    """A turtle's carapace: staggered hexagonal plates, each a green dome lit at its upper-left, dark seams between."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("shell", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                if face == "bottom": g[y][x] = SHELL[0]; continue
                if face == "top":
                    cx, cy = abs(x - (w - 1) / 2), abs(y - (h - 1) / 2)
                    ring = max(cx, cy)
                    g[y][x] = SHELL[0] if ring > (w - 1) / 2 - 0.5 else SCUTE[3] if (ring < 0.6 and top_ridge) else clamp(SHELL, 3 + (1 if (x + y) % 5 == 0 else 0) - (1 if ring > (w - 1) / 2 - 1.5 else 0))
                    continue
                row, off = y % 4, (3 if (y // 4) % 2 else 0)
                cx = (x + off) % 6
                if cx == 5 or row == 3: g[y][x] = SHELL[0]; continue          # the seam
                v = 3 + (1 if (cx <= 1 and row == 0) else 0) - (1 if (cx >= 3 or row == 2) else 0) + (LIT[face] if face in ("left", "back") else 0)
                if cx == 0 and row == 1 and face == "front": v += 1
                g[y][x] = clamp(SHELL, v)
        if rim and face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[4 if x % 2 else 3]            # the sea-blue rolled edge
        if face in ("front", "back") and w >= 8:
            for y in range(h - (1 if rim else 0)): g[y][w // 2] = SCUTE[3 if y % 4 < 3 else 1]     # the vertebral ridge
        if face == "front" and h > 3:
            for _ in range(2): g[r.randrange(h - 1)][r.randrange(w)] = BARN[3]
        return g
    return paint

def kelp(seed, hem=2):
    """A kelp ribbon: a dark vein, frilled edges, the tip shredded."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("kelp", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] // 2
                if face in ("front", "back") and (x == w // 2 or w == 1): v -= 1
                if y % 3 == 2 and face in ("front", "back") and x != w // 2: v += 1
                g[y][x] = clamp(KELP, v)
        for y in range(h - hem, h):
            for x in range(w):
                if r.random() < 0.3 + 0.3 * (y - (h - hem)): g[y][x] = None
        return g
    return paint

def barnacle():
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("bc", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BARN, 2 + LIT[face] + r.choice((-1, 0, 1)))
        return g
    return paint

def net():
    """A rolled fishing net: rope diamonds with gaps."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom") or (x + y) % 2 == 0 or y == 0 or y == h - 1:
                    g[y][x] = clamp(ROPE, 2 + LIT[face] + (1 if (x // 2 + y) % 3 == 0 else 0))
        return g
    return paint

def kelp_wrap():
    def paint(face, w, h):
        g = kelp(("wr", face), 0)(face, w, h)
        for y in range(h):
            for x in range(w):
                if g[y][x] is not None and (x + y) % 3 == 0: g[y][x] = clamp(KELP, 2)
        return g
    return paint

# ---- cloth ----
def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tail": return TG.tabard(T, M)
    if spec == "scarf": return TG.wrap_cloth(T, M)
    return TG.wrap_cloth(T, M)

# ---- assembling ----
def strand(name, parent, pivot, rot, boxes):
    return Bone(name, parent, pivot, [Box(o, s, kelp((name, i))) for i, (o, s) in enumerate(boxes)], rot=rot)

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4.5, -12, -2.5), (9, 12, 5), coat())], rot=(HUNCH, 0, 0)))
    B.append(Bone("belly", "body", boxes=[Box((-2, -8, -3.4), (4, 5, 0.9), belly())]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4.5, -8, -4), (9, 8, 8), face_painter_raider())], rot=(-HUNCH * 0.6, 0, 0)))
    # the turtle-shell helmet: a broad dome in three stepped plates and a flared back guard over the nape
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-5, -10, -5), (10, 4, 10), scutes("base", rim=True)),
        Box((-4, -12.5, -4), (8, 2.5, 8), scutes("mid")),
        Box((-2.5, -13.5, -2.5), (5, 1, 5), scutes("crown", True))]))
    B.append(Bone("helm_back", "head", (0, -6, 4.5), rot=(-14, 0, 0), tag="helm", boxes=[Box((-4.5, 0, 0), (9, 5, 2), scutes("back", rim=True))]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms(), the arm standing back up
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm_painter(), f"{side}_forearm", ELBOW[side], rot=(-HUNCH, 0, 0))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg_painter(), f"{side}_shin", KNEE)
    B.append(Bone("skirt", "body", boxes=[Box((-5, -3, -3), (10, 6, 6), skirt(), dil=0.1)]))
    B.append(Bone("belt", "body", boxes=[Box((-4.5, -4, -2.5), (9, 2, 5), belt(), dil=0.5)]))
    B.append(Bone("barnacles", "body", boxes=[
        Box((-5.2, -11.5, -1), (1, 2, 2), barnacle()), Box((4.6, -5, 0), (1, 1, 2), barnacle())]))
    B.append(Bone("net", "body", boxes=[Box((-3, -11, 2.6), (6, 4, 2), net())]))
    B.append(Bone("scarf", "body", team=True, boxes=[Box((-4.7, -12.5, -2.7), (9.4, 2, 5.4), "scarf", sheet="cloth")]))
    B.append(Bone("tail", "body", team=True, boxes=[
        Box((2.5, -3.5, -3.3), (2, 8, 0), "tail", sheet="cloth"), Box((2.5, -3.5, 3.3), (2, 8, 0), "tail", sheet="cloth")]))
    # kelp streaming back: helm, both shoulders, both forearms, belt
    B.append(strand("kelp_helm", "head", (0, -4, 4.5), (30, 0, 0), [((-2.5, 0, 0), (2, 7, 1)), ((1, 0, 0.4), (2, 9, 1))]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        if side == "left": B.append(strand(f"kelp_{side}_shoulder", "body", (5 * c, -10.5, 0.5), (26, 0, 8 * c), [((-1, 0, 0), (2, 7, 1))]))
        B.append(Bone(f"{side}_wrap", f"{side}_forearm", team=True, boxes=[Box((-1 if side == "right" else -4, 3, -2), (4, 2, 4), "wrap", dil=0.3, sheet="cloth")]))
        B.append(Bone(f"{side}_foot", f"{side}_shin", boxes=[Box((-2.5, 3, -3.5), (5, 3, 5.5), foot())]))
    # tier II, the Reef Reaver: one bronze pauldron on the sword shoulder, bracers, greaves, a gorget band
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_bracer", f"{side}_forearm", stages=[2, 3], boxes=[Box((-1.2 if side == "right" else -4.2, -0.5, -2.2), (4.4, 3.5, 4.4), TC.P("rdbr" + side, 0.03), dil=0.1)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[2, 3], boxes=[Box((-2.3, -2, -2.3), (4.6, 4.5, 4.6), TC.P("rdgv" + side), dil=0.1)]))
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 14 * c), stages=[2, 3] if side == "right" else [3], boxes=[
            Box((-3.5 + 0.5 * c, -0.5, -3), (7, 3, 6), TC.P("rdpd" + side), dil=0.2),
            Box((-2 + 0.5 * c, -1.5, -2), (4, 1, 4), TC.P("rdpt" + side), dil=0.1)]))
    B.append(Bone("gorget", "body", stages=[2, 3], boxes=[Box((-4, -12.5, -3), (8, 1.5, 6), TC.band("rdg"), dil=0.1)]))
    # tier III, the Wreck Captain: shell barnacles on helm and shoulders, a pearl-studded helm rim
    B.append(Bone("helm_rim", "head", tag="helm", stages=[3], boxes=[Box((-5.2, -5.2, -4.8), (10.4, 1.2, 9.6), TC.band("rdhr"), dil=0.05)]))
    for i, (x, y, z, w) in enumerate([(-3, -11, -2, 2), (1.5, -11.5, 0, 2), (-1, -11.2, 2.5, 3), (3.2, -10.5, -3, 1)]):
        B.append(Bone(f"shell_barn{i}", "head", tag="helm", stages=[3], boxes=[Box((x, y, z), (w, 1.5, w), TC.tc_barnacles(("rdh", i)))]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_shell_barn", f"{side}_arm", (c, -2.5, 0), stages=[3], boxes=[
            Box((-1.5 + c, -2.5, -1.5), (2, 1.5, 2), TC.tc_barnacles((side, "a"))), Box((0.5 + c, -2.2, 0.5), (1, 1, 1), TC.tc_barnacles((side, "b")))]))
    return B

NOTE = ("Tidecaller Drowned Raider (unit identity, Tidecallers): drowned light skirmisher, biped, leaning forward (hunch baked into the bones); "
        "a green turtle-shell helmet with a sea-blue rolled edge (the signature), a bloated grey-green body with white eyes, a sea-blue oilskin "
        "coat open on a swollen belly, tattered navy trousers, barnacles, a rolled net on the back, kelp streaming from helm, shoulders, "
        "forearms, belt and shin, a team neck scarf, hip sash and wrist wraps. The cutlass is in the right hand. No coral. Tier II a bronze sword-shoulder "
        "pauldron, bracers and greaves; tier III deep sea-blue plate on both shoulders and shell barnacles on helm and shoulders. No back flag.")
TC_RAIDER = Troop("tc_raider", SKIN, CLOTH, bones, cloth_painter,
                  {"weapon": "cutlass", "hand": "right", "idleArms": "hang", "portrait": "standard"})
def build(): return TC_RAIDER.build()
def outputs(): return TC_RAIDER.outputs()

if __name__ == "__main__":
    house_stub.main(TC_RAIDER, NOTE, sys.argv)
