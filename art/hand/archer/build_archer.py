#!/usr/bin/env python3
"""The Archer (Gilded house, troop id archer), hand-authored at one texel per model pixel.

A light man of the Order who keeps his distance: a linen hood whose tail hangs down his back and whose cape lies over
his shoulders, a padded gambeson under a team jerkin that falls to mid-thigh, a quiver of white-fletched arrows slung
across his back on a strap over the chest, a leather bracer on the bow arm. Tier I is the levy's kit; II lays a mail
tippet over the hood's cape, stamps the sun on the jerkin and hems it, caps the bracers in steel and cuffs the boots;
III dyes the hood in the team and edges it in gilt over the tippet, studs the jerkin as a brigandine,
gilds the belt and the quiver's bands and hangs a signal horn at the hip.

No flag rides on his back (Kort, 2026-10-29): the quiver is the silhouette.

  python build_archer.py preview <dir> [side] | contact <png> [side] [all] [vet] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "knight"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
import build_knight as K

SKIN = (128, 64)
CLOTH = (128, 64)
HOSE = ramp("15130f", "26221b", "3a3328", "4f4636", "675c47", "84775c")    # brown-grey wool hose
PAD = ramp("2e261c", "4f4332", "776650", "9b8a6c", "bcab88", "d8caa6")     # the gambeson's padded linen, a shade under the hood
IVORY = ramp("4a3e2c", "7c6b50", "ab9a78", "d2c4a0", "eee4c8")

# ---- the hood ----
def hood(tier):
    """Linen, open over the face, the brow folded back in a lip; III gilds the lip and the hem."""
    R = LINEN
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = (3 if tier < 3 else 4) + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("right", "left", "back") and x % 3 == 2 and y > 2: v -= 1            # the cloth's folds
                if face == "top" and (x == 0 or x == w - 1 or y == 0): v -= 1
                g[y][x] = clamp(R, v)
        if face == "front":
            for y in range(2, h):
                for x in range(1, w - 1): g[y][x] = None                 # the face opening
            g[1] = [clamp(R, 5)] * w                                     # the rolled lip over the brow
            g[0] = [clamp(R, 3)] * w
            for y in range(2, h): g[y][0] = clamp(R, 2); g[y][w - 1] = clamp(R, 1)
            if tier == 3:
                g[1] = [GOLD[4] if x % 2 else GOLD[5] for x in range(w)]
                for y in range(2, h): g[y][0] = g[y][w - 1] = GOLD[3] if y % 2 else GOLD[4]
        if face in ("right", "left", "back"):
            g[h - 1] = [clamp(R, 1)] * w                                 # its shadowed hem into the cape
            if tier == 3: g[h - 1] = [GOLD[3] if x % 2 else GOLD[2] for x in range(w)]
        return g
    return paint

def team_hood(T, M):
    """III's hood, dyed in the team and edged in the emblem metal, a sun on its crown behind."""
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("right", "left", "back") and x % 3 == 2 and y > 2: v -= 1
                if face == "top" and (x == 0 or x == w - 1 or y == 0): v -= 1
                g[y][x] = clamp(T, v)
        if face == "front":
            for y in range(2, h):
                for x in range(1, w - 1): g[y][x] = None
            g[0] = [clamp(T, 3)] * w
            g[1] = [M[3] if x % 2 else M[2] for x in range(w)]
            for y in range(2, h): g[y][0] = g[y][w - 1] = M[2] if y % 2 else M[1]
        if face in ("right", "left", "back"):
            g[h - 1] = [M[2] if x % 2 else M[1] for x in range(w)]
        if face == "back": over(g, grid(SUN, {"m": M[1], "L": M[3]}), 2, 2)
        return g
    return paint

def team_liripipe(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("front", "top"): return g
        for y in range(h):
            for x in range(w): g[y][x] = clamp(T, 2 + LIT[face] + (1 if face == "back" else 0))
        if face != "bottom": g[h - 1] = [M[2]] * w
        return g
    return paint

def liripipe(tier):
    """The hood's tail, down the back of the neck; its front lies against the hood and is not drawn."""
    def paint(face, w, h):
        if face in ("front", "top"): return blank(w, h)
        g = quilt(LINEN, base=(3 if tier < 3 else 4) - (1 if face != "back" else 0), every=3)(face, w, h)
        if face != "bottom": g[h - 1] = [LINEN[1]] * w
        if tier == 3 and face == "back": g[h - 1] = [GOLD[4]] * w
        return g
    return paint

def hood_cape(face, w, h):
    """The hood's short cape over the shoulders, cut in shallow points at the hem."""
    g = blank(w, h)
    if face == "bottom": return g
    for y in range(h):
        for x in range(w):
            v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
            if face == "top" and (x in (0, w - 1) or y in (0, h - 1)): v -= 1
            g[y][x] = clamp(LINEN, v)
    if face != "top":
        for x in range(w):
            if x % 3 == 1: g[h - 1][x] = None
            else: g[h - 1][x] = LINEN[2]
    return g

# ---- body, arms, legs ----
def body_base(face, w, h):   # the gambeson, seen only at the jerkin's arm holes and its open sides
    return quilt(PAD, base=3)(face, w, h)

def belt(tier):
    def paint(face, w, h):
        g = leather(base=1)(face, w, h)
        if face == "front":
            buckle = ("ed", "dc") if tier == 3 else ("54", "43")
            for y, row in enumerate(buckle):
                for x, c in enumerate(row): g[y][3 + x] = pal()[c]
        if tier == 3 and face in ("right", "left", "back"):
            for x in range(1, w, 3): g[0][x] = GOLD[4]
        return g
    return paint

def pouch(tier):
    def paint(face, w, h):
        g = leather(base=3)(face, w, h)
        if face not in ("top", "bottom"):
            g[0] = [LEATH[4]] * w                                        # the flap
            if face == "front": g[1][w // 2] = GOLD[4] if tier == 3 else STEEL[4]
        return g
    return paint

def bracer(tier):
    """The forearm: a leather bracer laced up the inside (I), capped in steel (II), a steel vambrace edged in gilt (III)."""
    if tier == 1:
        def paint(face, w, h):
            g = leather(base=3)(face, w, h)
            if face in ("left", "right"):
                for y in range(h): g[y][w // 2] = LEATH[1] if y % 2 else LEATH[5]
            return g
        return paint
    def paint(face, w, h):
        if face in ("top", "bottom"): return plate(base=3)(face, w, h)
        g = plate(base=4 if tier == 3 else 3, spec=False)(face, w, h)
        if tier == 2:                                                    # a leather bracer under steel splints
            for y in range(h):
                for x in range(w):
                    if x % 2: g[y][x] = clamp(LEATH, 3 + LIT[face])
        if tier == 3:
            g[0] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
            g[h - 1] = [GOLD[2]] * w
        return g
    return paint

def glove(face, w, h):
    g = leather(base=2)(face, w, h)
    if face not in ("top", "bottom"): g[0] = [LEATH[4]] * w             # the cuff's turned edge
    return g

def arm(tier):
    return rows([(0, quilt(PAD, base=3, every=3)), (7, bracer(tier)), (10, glove)])

def leg(tier):
    return rows([(0, quilt(HOSE, base=3, every=4)), (7, leather(base=2)), (9, leather(base=1))])

def boot_cuff(tier):
    def paint(face, w, h):
        if face in ("top", "bottom"): return blank(w, h)
        g = leather(base=3, stitch=True)(face, w, h)
        if tier == 3: g[0] = [GOLD[4] if x % 2 else GOLD[3] for x in range(w)]
        return g
    return paint

def knee_cop(tier):
    def paint(face, w, h):
        g = plate(base=4 if tier == 3 else 3, spec=False)(face, w, h)
        if tier == 3 and face == "front": g[0][w // 2] = GOLD[5]; g[h - 1] = [GOLD[3]] * w
        return g
    return paint

# ---- the quiver, slung across the back ----
def quiver(tier):
    def paint(face, w, h):
        g = leather(base=3 if face != "back" else 2)(face, w, h)
        if face == "top": return [[LEATH[0]] * w for _ in range(h)]       # its mouth, full of shafts
        if face == "bottom": return leather(base=1)(face, w, h)
        bands = (1, h - 2) if tier == 1 else (1, h // 2, h - 2)
        for y in bands:
            c = GOLD[4] if tier == 3 else STEEL[3] if tier == 2 else LEATH[1]
            g[y] = [c if x % 2 == 0 or tier == 1 else (GOLD[3] if tier == 3 else STEEL[2]) for x in range(w)]
        if tier >= 2 and face == "back":
            for y in range(3, h - 3):
                if y not in bands: g[y][w // 2] = LEATH[1]               # the stitched seam
        return g
    return paint

def fletching(face, w, h):
    """The arrows' white fletching, three shafts deep, nocks dark on top."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face == "top": g[y][x] = LEATH[1] if (x + y) % 2 else hx("d8d2c4")
            elif face == "bottom": g[y][x] = None
            else: g[y][x] = hx("f2eee4") if (x + y) % 2 == 0 else hx("b9b2a2") if y < h - 1 else hx("8e8676")
    if face not in ("top", "bottom"):
        for x in range(1, w, 2): g[0][x] = None                            # ragged feather tips
    return g

def horn(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(IVORY, 2 + LIT[face] + (1 if y == 0 else 0))
    if face not in ("top", "bottom"):
        g[h // 2] = [GOLD[4]] * w
    return g

def horn_bell(face, w, h):
    g = plate(R=GOLD, base=3, spec=False)(face, w, h)
    if face == "front": g = [[INK if 0 < x < w - 1 and 0 < y < h - 1 else GOLD[4] for x in range(w)] for y in range(h)]
    return g

# ---- the team cloth: hood, jerkin, skirts ----
SUN = """
..m..
.mLm.
mLLLm
.mLm.
..m.."""

def jerkin(T, M, tier):
    """Sleeveless, laced at the chest; II the sun and a hem, III brigandine: studded, bordered, the sun gilt."""
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back") and y > 9 and x in (1, 6): v -= 1
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face == "top":
            for y in range(h):
                for x in range(2, w - 2): g[y][x] = None                    # the neck
        if face in ("right", "left"):                                       # open under the arms
            for y in range(0, 6):
                for x in range(w): g[y][x] = None
            g[6] = [T[1]] * w
        if face == "front":
            if tier == 1:                                                   # the lacing
                for y in range(1, 6): g[y][3] = T[1]; g[y][4] = LEATH[4] if y % 2 else T[1]
            else:
                over(g, grid(SUN, {"m": M[1], "L": M[3]}), 1, 2)
            # the quiver strap, from the left shoulder to the right hip
            for y in range(0, 10):
                x = 6 - (y * 6) // 9
                if 0 <= x < w: g[y][x] = LEATH[2] if tier < 3 else LEATH[3]
                if tier == 3 and y % 3 == 1 and 0 <= x < w: g[y][x] = GOLD[4]
        if face == "back":                                                  # the strap's other run, right shoulder down
            for y in range(0, 10):
                x = 1 + (y * 5) // 9
                g[y][x] = LEATH[2]
        if tier >= 2 and face != "top":
            g[h - 1] = [M[1]] * w
        if tier == 3 and face in ("front", "back", "right", "left"):
            for y in range(1, h - 2, 2):                                    # the brigandine's rivets
                for x in range(1 if y % 4 == 1 else 3, w - 1, 4):
                    if g[y][x] is not None and g[y][x] in T: g[y][x] = M[2]
            if face in ("front", "back"):
                for y in range(h): g[y][0] = g[y][w - 1] = M[1] if y % 2 else M[2]
        return g
    return paint

def flap(T, M, tier, inner_x):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y == h - 1 else 0))
        if tier >= 2: g[h - 1] = [M[1]] * w
        if tier == 3:
            for x in range(w): g[h - 1][x] = M[1] if x % 2 else M[2]
            for y in range(1, h - 1, 2): g[y][3 - inner_x] = M[2]
        return g
    return paint

# ---- the Mastery II trim ----
def vet_band(rows_at):
    def paint(face_, w, h):
        g = blank(w, h)
        if face_ in ("top", "bottom"): return g
        for y in rows_at:
            if y < h:
                for x in range(w): g[y][x] = GOLD[4] if x % 2 else GOLD[3]
        return g
    return paint

def veteran(bone, box):
    n = bone.name
    if n.endswith("_arm"): return vet_band((6,))                          # a gilt band above the bracer
    if n.endswith("_leg"): return vet_band((6,))                          # and one under the knee
    return None

# ---- assembling ----
def bones():
    B = []
    B.append(Bone("root", ""))
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_base)]))
    B.append(Bone("head", "body", (0, -12, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", boxes=[Box((-4, -8, -4), (8, 8, 8), face_painter(k))], variants=[k]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), per_tier(arm))]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), per_tier(arm))]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), per_tier(leg))]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), per_tier(leg))]))
    # the hood and its tail
    B.append(Bone("hood", "head", tag="helm", stages=[1, 2], boxes=[
        Box((-4.5, -8.5, -4.5), (9, 9, 9), per_tier(hood, (1, 2))), Box((-1, -6.5, 4.5), (2, 8, 1), per_tier(liripipe, (1, 2)))]))
    B.append(Bone("hood_t3", "head", team=True, tag="helm", stages=[3], boxes=[
        Box((-4.5, -8.5, -4.5), (9, 9, 9), "hood", sheet="cloth"), Box((-1, -6.5, 4.5), (2, 8, 1), "liripipe", sheet="cloth")]))
    # the shoulders: the hood's cape (I), under mail (II), under the team capelet (III)
    B.append(Bone("hood_cape", "body", stages=[1], boxes=[Box((-4.5, -12.5, -2.5), (9, 3, 5), hood_cape)]))
    B.append(Bone("tippet", "body", stages=[2, 3], boxes=[Box((-4.5, -12.5, -2.5), (9, 3, 5), K.mantle)]))
    # the jerkin and its skirts
    for st, g in (([1], 1), ([2], 2), ([3], 3)):
        B.append(Bone(f"jerkin_t{g}", "body", team=True, stages=st, boxes=[
            Box((-4, -12, -2), (8, 12, 4), ("jerkin", g), dil=0.3, sheet="cloth")]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), per_tier(belt), dil=0.45)]))
    B.append(Bone("pouch", "body", boxes=[Box((-3.5, -1.5, -3.3), (2, 3, 1), per_tier(pouch))]))
    B.append(Bone("signal_horn", "body", stages=[3], boxes=[
        Box((0.5, -1, -3.3), (3, 1, 1), horn), Box((3.5, -1.5, -3.5), (1, 2, 1), horn_bell)]))
    # the quiver, slung over the right shoulder, its mouth behind the left
    B.append(Bone("quiver", "body", (0, -6, 3), rot=(0, 0, -28), boxes=[
        Box((-2, -5, 0), (4, 12, 3), per_tier(quiver)), Box((-1.5, -9, 0.5), (3, 4, 2), fletching)]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_knee", f"{side}_leg", stages=[2, 3], boxes=[
            Box((-1.5, 5, -2.6), (3, 2, 1), per_tier(knee_cop, (2, 3)))]))
        B.append(Bone(f"{side}_boot_cuff", f"{side}_leg", stages=[2, 3], boxes=[
            Box((-2.5, 7, -2.5), (5, 2, 5), per_tier(boot_cuff, (2, 3)))]))
        inner = 3 if side == "right" else 0
        for st, g in (([1], 1), ([2], 2), ([3], 3)):
            B.append(Bone(f"{side}_skirt_t{g}", f"{side}_leg", team=True, stages=st, boxes=[
                Box((-2, 0, -2.45), (4, 4, 0), ("flap", g, inner), sheet="cloth"),
                Box((-2, 0, 2.45), (4, 4, 0), ("flap", g, 3 - inner), sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "hood": return team_hood(T, M)
    if spec == "liripipe": return team_liripipe(T, M)
    kind, t = spec[0], spec[1]
    if kind == "jerkin": return jerkin(T, M, t)
    return flap(T, M, t, spec[2])

ARCHER = Troop("archer", SKIN, CLOTH, bones, cloth_painter,
               {"weapon": "order_bow", "hand": "left", "idleArms": "hang", "portrait": "standard", "faces": {"count": 4, "card": 0}},
               veteran=veteran)
def build(): return ARCHER.build()
def outputs(): return ARCHER.outputs()

if __name__ == "__main__":
    ARCHER.main(sys.argv)
