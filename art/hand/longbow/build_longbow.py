#!/usr/bin/env python3
"""The Longbowman (Gilded house, troop id longbow), hand-authored at one texel per model pixel.

The archer's elder: a yeoman under a brimmed hat, his padded jack to the knee under a jerkin, a tall quiver hung behind
the right hip, a bracer on the bow arm. Tier I wears a felt cap with a peak and a tail; II a steel kettle hat, mail
under the jack's sleeves, a jerkin parted per pale in the team and the Order's white with the sun on it, a white sash,
steel splints on the bracer; III rims the kettle in gilt and plumes it with a long white feather, studs the jerkin in
gilt, gilds the sash, the quiver's bands and the boots' cuffs and caps the knees in steel.

No flag rides on his back (Kort, 2026-10-29): the hat and the quiver are the silhouette.

The archer's kit (bracers, gloves, belt, quiver, fletching, skirts) comes from build_archer, so the Order's bowmen read
as one company.

  python build_longbow.py preview <dir> [side] | contact <png> [side] [all] [vet] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "knight"))
sys.path.insert(0, os.path.join(HERE, "..", "archer"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
import build_knight as K
import build_archer as A

SKIN = (128, 128)   # joint overlap boxes (review 2026-11-04 1.3) filled 128 x 64
CLOTH = (128, 64)
FELT = ramp("1a1510", "2e261c", "463a2a", "5f4f39", "7a6749", "97835e")    # the felt cap, a weathered brown
WHITE = ramp("6a6458", "9a9484", "c4bfaf", "e2ddcd", "f4f1e6")             # the Order's white
PLUME = (hx("f6f3ea"), hx("dcd6c8"), hx("aaa293"))

# ---- hats ----
def felt(base=3):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face == "top" and (x + y) % 4 == 0: v -= 1
                g[y][x] = clamp(FELT, v)
        return g
    return paint

def felt_crown(face, w, h):
    g = felt(3)(face, w, h)
    if face == "bottom": return blank(w, h)
    if face in ("front", "right", "left", "back"): g[h - 1] = [LEATH[2]] * w           # the hatband
    if face == "right": g[h - 1][w // 2] = LINEN[4]                                     # a little white cockade
    return g

def felt_peak(face, w, h):
    g = felt(3)(face, w, h)
    if face == "bottom": g = felt(1)(face, w, h)
    return g

def kettle_crown(tier):
    def paint(face, w, h):
        if face == "bottom": return blank(w, h)
        g = plate(base=3, spec=False)(face, w, h)
        if face in ("front", "back"):
            for y in range(h): g[y][w // 2] = STEEL[5 if face == "front" else 4]          # the raised comb
        if face == "top":
            for y in range(h): g[y][w // 2] = STEEL[5]
        if tier == 3 and face not in ("top",):
            g[h - 1] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
        return g
    return paint

def kettle_brim(tier):
    def paint(face, w, h):
        if face == "top":
            g = [[clamp(STEEL, 4 if (x in (0, w - 1) or y in (0, h - 1)) else 3) for x in range(w)] for y in range(h)]
            if tier == 3:
                for x in range(w): g[0][x] = g[h - 1][x] = GOLD[4]
                for y in range(h): g[y][0] = g[y][w - 1] = GOLD[4]
            return g
        if face == "bottom": return [[STEEL[1]] * w for _ in range(h)]
        if tier == 3: return [[GOLD[5] if x % 2 else GOLD[4] for x in range(w)] for _ in range(h)]
        return [[clamp(STEEL, 4 + LIT[face]) for _ in range(w)] for _ in range(h)]
    return paint

def plume(face, w, h):
    """A long white feather swept back from the hat: lit along its spine, shaded toward its tip."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = PLUME[0] if face in ("top", "right") else PLUME[1] if face != "bottom" else PLUME[2]
            if face in ("right", "left") and x >= w - 2: g[y][x] = PLUME[2]
    if face in ("right", "left"):
        for x in range(0, w, 3): g[h - 1][x] = PLUME[2]
    return g

# ---- body, arms, legs ----
def body_base(face, w, h):   # the padded jack
    return quilt(A.PAD, base=3)(face, w, h)

def arm(tier):
    upper = quilt(A.PAD, base=3, every=3) if tier == 1 else mail(base=3)
    return rows([(0, upper), (4, quilt(A.PAD, base=3, every=3)), (7, A.bracer(tier)), (10, A.glove)])

def leg(tier):
    return rows([(0, quilt(A.PAD, base=2, every=4)), (5, quilt(A.HOSE, base=3, every=4)), (8, leather(base=2)), (10, leather(base=1))])

def jack_skirt(face, w, h):
    """The jack's quilted skirt below the jerkin, to the knee."""
    g = blank(w, h)
    if face not in ("front", "back"): return g
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(A.PAD, 3 + LIT[face] + (-1 if y % 2 else 0) + (-1 if y == h - 1 else 0))
    return g

# ---- the team cloth ----
def jerkin(T, M, tier):
    """I plain, laced; II per pale in the team and the Order's white, the sun on the white half, a white sash from the
    right shoulder; III studs it in the emblem metal and gilds the sash."""
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if y == 0 and face != "top": v += 1
                white = tier >= 2 and ((face == "front" and x >= w // 2) or (face == "back" and x < w // 2) or face == "left")
                g[y][x] = clamp(WHITE if white else T, v if not white else v + 0)
        if face == "top":
            for y in range(h):
                for x in range(2, w - 2): g[y][x] = None
        if face in ("right", "left"):
            for y in range(0, 5):
                for x in range(w): g[y][x] = None
            g[5] = [T[1]] * w
        if face == "front" and tier == 1:
            for y in range(1, 6): g[y][3] = T[1]; g[y][4] = LEATH[4] if y % 2 else T[1]
        if face == "front" and tier >= 2:
            over(g, grid(A.SUN, {"m": M[1], "L": M[3]}) if M is not STEEL[0:4] else grid(A.SUN, {"m": GOLD[3], "L": GOLD[5]}), 4, 2)
        if tier >= 2 and face in ("front", "back"):                       # the sash, right shoulder to left hip
            for y in range(0, h - 1):
                x = (y * (w - 1)) // (h - 2) if face == "front" else (w - 1) - (y * (w - 1)) // (h - 2)
                for dx in (0, 1):
                    if 0 <= x + dx < w:
                        g[y][x + dx] = (GOLD[5] if dx == 0 else GOLD[3]) if tier == 3 else (WHITE[4] if dx == 0 else WHITE[2])
        if tier == 3 and face in ("front", "back", "right", "left"):
            for y in range(2, h - 2, 3):
                for x in range(1, w - 1, 3):
                    c = g[y][x]
                    if c is not None and (c in T or c in WHITE): g[y][x] = GOLD[4]
        if tier >= 2 and face != "top": g[h - 1] = [M[1]] * w
        return g
    return paint

def flap(T, M, tier, inner_x, white):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        R = WHITE if white and tier >= 2 else T
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(R, 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y == h - 1 else 0))
        if tier >= 2: g[h - 1] = [M[1]] * w
        if tier == 3: g[h - 1] = [M[1] if x % 2 else M[2] for x in range(w)]
        return g
    return paint

# ---- the Mastery II trim ----
def veteran(bone, box):
    n = bone.name
    if n.endswith(("_arm", "_forearm")): return A.vet_band((6,))
    if n.endswith(("_leg", "_shin")): return A.vet_band((6,))
    return None

# ---- assembling ----
def bones():
    B = []
    B.append(Bone("root", ""))
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_base)]))
    B.append(Bone("head", "body", (0, -12, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", boxes=[Box((-4, -8, -4), (8, 8, 8), face_painter(k))], variants=[k]))
    B += arms(per_tier(arm))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B += legs(per_tier(leg))
    # the felt cap (I): a crown over the hair, a peak over the brow, a tail turned back
    B.append(Bone("felt_cap", "head", tag="helm", stages=[1], boxes=[
        Box((-4.5, -10, -4.5), (9, 3, 9), felt_crown), Box((-4.5, -8, -6.5), (9, 1, 2), felt_peak),
        Box((-1.5, -11, 1), (3, 1, 4), felt(3))]))
    # the kettle hat (II, III), plumed at III
    B.append(Bone("kettle", "head", tag="helm", stages=[2, 3], boxes=[
        Box((-4.5, -9, -4.5), (9, 2, 9), per_tier(kettle_crown, (2, 3))), Box((-3.5, -11, -3.5), (7, 2, 7), per_tier(kettle_crown, (2, 3))),
        Box((-6.5, -7, -6.5), (13, 1, 13), per_tier(kettle_brim, (2, 3)))]))
    B.append(Bone("long_feather", "head", (3.5, -10, -1), tag="helm", stages=[3], rot=(-40, 0, 0), boxes=[
        Box((0, -1, 0), (1, 2, 12), plume), Box((0, -2, 6), (1, 1, 6), plume)]))
    # the jerkin over the jack, the jack's skirts under the jerkin's
    for g in (1, 2, 3):
        B.append(Bone(f"jerkin_t{g}", "body", team=True, stages=[g], boxes=[
            Box((-4, -12, -2), (8, 12, 4), ("jerkin", g), dil=0.3, sheet="cloth")]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), per_tier(A.belt), dil=0.45)]))
    B.append(Bone("pouch", "body", boxes=[Box((1.5, -1.5, -3.3), (2, 3, 1), per_tier(A.pouch))]))
    # the tall quiver, behind the right hip, the arrows' fletching above the belt. Its mouth leans out over the STRING
    # shoulder (roll -8; was 8, 2026-11-12 bow batch review): since the GeckoLib geo writes a bow troop's quiver roll as
    # the hand model has it (troop_rig, 6935d38f), roll 8 tipped the mouth toward the bow shoulder, behind the head, and
    # the right hand's pluck dragged the arrow across the back of the head (forearm 2.3 px into the face).
    B.append(Bone("tall_quiver", "body", (-2.5, -3, 2.4), rot=(12, 0, -8), boxes=[
        Box((-1.5, -6, 0), (3, 13, 3), per_tier(A.quiver)), Box((-1, -10, 0.5), (2, 4, 2), A.fletching)]))
    for side in ("right", "left"):
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_jack", f"{side}_leg", boxes=[
            Box((-2, 0, -2.3), (4, 6, 0), jack_skirt), Box((-2, 0, 2.3), (4, 6, 0), jack_skirt)]))   # to the knee hinge
        for g in (1, 2, 3):
            B.append(Bone(f"{side}_skirt_t{g}", f"{side}_leg", team=True, stages=[g], boxes=[
                Box((-2, 0, -2.45), (4, 4, 0), ("flap", g, inner, side == "left"), sheet="cloth"),
                Box((-2, 0, 2.45), (4, 4, 0), ("flap", g, 3 - inner, side == "right"), sheet="cloth")]))
        B.append(Bone(f"{side}_knee", f"{side}_shin", stages=[3], boxes=[
            Box((-1.5, -1, -2.6), (3, 2, 1), per_tier(A.knee_cop, (3,)))]))
        B.append(Bone(f"{side}_boot_cuff", f"{side}_shin", stages=[2, 3], boxes=[
            Box((-2.5, 2, -2.5), (5, 2, 5), per_tier(A.boot_cuff, (2, 3)))]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    kind, t = spec[0], spec[1]
    if kind == "jerkin": return jerkin(T, M, t)
    return flap(T, M, t, spec[2], spec[3])

LONGBOW = Troop("longbow", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "longbow", "hand": "left", "idleArms": "hang", "portrait": "tall 2", "faces": {"count": 4, "card": 0}},
                veteran=veteran)
def build(): return LONGBOW.build()
def outputs(): return LONGBOW.outputs()

if __name__ == "__main__":
    LONGBOW.main(sys.argv)
