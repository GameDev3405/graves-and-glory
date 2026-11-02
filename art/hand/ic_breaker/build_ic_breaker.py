#!/usr/bin/env python3
"""The Ironclad Shieldbreaker (Ironclad house, troop id ic_breaker), hand-authored at one texel per model pixel.

A bull of a man in blackened iron: a closed sallet with an ember brow band and an ember horsehair plume, a fur collar
over lamellar-lapped plate, mail showing at the elbows and knees, a long team tabard cleft by a cloven shield, and
a bearded axe inlaid in ember. Tier I only (the Ironclad troops are single-stage in the first pass).

  python build_ic_breaker.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ironclad import *   # noqa: F401,F403
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
        lift = {"back": 2, "left": 1}.get(face, 0)                # the shaded faces take a step or two up so they never read black
        g = lames([4, 5], base=3 + lift, seed="hc", wear=2, rivets=False)(face, w, h)
        if face in ("right", "left") and h >= 5:
            near = (w - 2, w - 1) if face == "right" else (0, 1)
            for x in near: g[3][x] = INK
            for x in range(w): g[2][x] = EMBER[2]                  # the brow band runs round the can
        if face == "back":
            for y in range(1, h - 1): g[y][w // 2] = IRON[3]        # the welded seam
            for x in range(w): g[2][x] = EMBER[2]
        return g
    return paint

def helm_crown():
    def paint(face, w, h):
        g = plate(R=IRON, base=4 + (1 if face in ("back", "left") else 0), spec=False, seam=False)(face, w, h)
        return scuff(g, ("crown", face), 2)
    return paint

def calm_mail(base=2):
    """Mail as one quiet mass: a lit top row and an edge shade, no weave, so it never reads as noise."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if y == 0 and face not in ("top", "bottom"): v += 1
                g[y][x] = clamp(MAIL, v)
        return g
    return paint

def plume_calm(tail=False):
    """Horsehair as clean lit bands, brightest at the crown: no per-texel noise; the tail's hem is ragged on a fixed beat."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 if face == "top" else (3 if y < max(1, h // 3) else 2) + LIT[face] // 2
                if y == h - 1 and face not in ("top", "bottom"): v -= 1
                g[y][x] = clamp(EMBER, v)
        if tail and face not in ("top", "bottom"):
            for x in range(1, w, 2): g[h - 1][x] = None
        return g
    return paint

def pelt():
    """The fur collar: three flat tones (lit crown, body, dark hem) with a hem that alternates by fixed beat."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (1 if y == 0 else 0) - (0 if y == h - 1 and face not in ("top", "bottom") else 0)
                g[y][x] = clamp(FUR, v)
        if face not in ("top", "bottom"):
            g[h - 1] = [clamp(FUR, 2)] * w                          # a solid hem: no teeth against the cloth behind
        return g
    return paint

def gorget_fur():
    return pelt()

# ---- body ----
BREAST = fold("""
.....
.4444
.3555
.3454
.2343
.2232""")  # a hammered breast ridge laid over the lames

def body_painter():
    base = lames([3, 3, 3, 3], base=3, seed="bd", wear=2, rivets=False, ember_rows=())
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for y in range(1, 7):                                  # the breastplate's ridge and the cleave scar
                g[y][w // 2 - 1] = IRON[5] if y in (2, 3) else IRON[4]
                g[y][w // 2] = IRON[2]
        if face == "back":
            for y in range(h): g[y][w // 2] = IRON[2]              # the spine seam
        return g
    return paint

def belt():
    def paint(face, w, h):
        g = leather_ic(base=1)(face, w, h)
        if face == "front":
            for y, row in enumerate(("56", "43")):                  # an iron buckle, lit top-left
                for x, c in enumerate(row): g[y][3 + x] = IRON[int(c)]
        return g
    return paint

def arm():
    """Mail sleeve, ONE long vambrace plate (lit top, dark lip), a plain leather glove: three masses, no stripes."""
    return rows([(0, calm_mail(2)), (3, lames([6], base=3, seed="arm", wear=1, rivets=False)), (9, leather_ic(base=2))])

def leg():
    return rows([(0, calm_mail(2)), (5, lames([4], base=3, seed="leg", wear=1, rivets=False)), (9, leather_ic(base=2))])

def pauldron_lame(i):
    """Three stacked plates in clear steps: bright cap, mid, dark lower lip. No rivets, no trim."""
    return lames([[2, 2, 2, 1][i]], base=[4, 3, 3, 2][i], seed=("pl", i), wear=0, rivets=False)

def knee():
    return lames([2], base=4, seed="knee", wear=0, rivets=False)

def boot():
    return rows([(0, leather_ic(base=2)), (2, lames([1], base=2, rivets=False))])

def cuff():
    """A plain flared iron cuff, the one clear lip between vambrace and glove. The arm carries no accent colour."""
    return lames([2], base=3, wear=0, rivets=False)

# ---- the cloth: tabard, side flaps ----
# a clean roundel: bright team-metal ring, dark team field, one bright cleaving slash. f field, c slash, r/R ring, s ring shade
ROUND6 = """
.rRRR.
rfffcR
RffcfR
RfcffR
RcfffR
.RssR."""

def round_pal(T, M):
    # M is the team metal (gold, or silver on the gold side): 5 tones, brightest last
    return {"r": M[-1], "R": M[-2], "s": M[-4], "c": M[-1], "f": T[1]}

def tabard(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face]
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face in ("right", "left"):
            for y in range(0, 7):
                for x in range(w): g[y][x] = None
            g[7] = [T[1]] * w
        if face in ("front", "back"):
            over(g, grid(ROUND6, round_pal(T, M)), 1, 3)           # rows 3-8: clear of the collar above and the belt below
            g[h - 1] = [EMBER[2]] * w                              # one ember hem line
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
        g[h - 1] = [EMBER[2]] * w
        return g
    return paint

def cape(T, M):
    """A short cape hung from the fur collar: a fur-dark top edge where it hangs from the shoulders, one cloth tone, a
    shaded edge column each side, the roundel and an ember hem."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = (2 if face != "back" else 3) + (-1 if face in ("left", "right") else 0)
                if face == "back" and x in (0, w - 1): v -= 1
                g[y][x] = clamp(T, v)
        if face != "bottom":
            g[0] = [clamp(FUR, 2)] * w                              # the cape is gathered into the collar
        if face == "back":
            over(g, grid(ROUND6, round_pal(T, M)), (w - 6) // 2, 2)
            g[h - 1] = [EMBER[2]] * w
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0)))
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-5, -8, -5), (10, 9, 10), helm_can()), Box((-4, -10, -4), (8, 2, 8), helm_crown())]))
    B.append(Bone("helm_plume", "head", tag="helm", boxes=[
        Box((-1, -14, -4), (2, 4, 8), plume_calm()),
        Box((-1, -12, 3.5), (2, 9, 2), plume_calm(tail=True), dil=0.1)]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm())]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm())]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg())]))
    B.append(Bone("fur_collar", "body", boxes=[Box((-5.5, -12.5, -3), (11, 3, 6), gorget_fur(), dil=0.2)]))
    B.append(Bone("tabard", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "tabard", dil=0.3, sheet="cloth")]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), belt(), dil=0.45)]))
    B.append(Bone("cape", "body", (0, -10.5, 2.4), team=True, rot=(0, 0, 0), boxes=[Box((-4, 0, 0), (8, 10, 1), "cape", sheet="cloth")]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 14 * c), boxes=[
            Box((-4, -1.5, -4), (8, 2, 8), pauldron_lame(0)),
            Box((-4 + 0.5 * c, 0.5, -3.5), (8, 2, 7), pauldron_lame(1), dil=0.15),
            Box((-3.5 + 1.0 * c, 2.5, -3), (7, 2, 6), pauldron_lame(2), dil=0.15),
            Box((-3 + 1.5 * c, 4.5, -2.5), (6, 1, 5), pauldron_lame(3), dil=0.1)]))
        B.append(Bone(f"{side}_cuff", f"{side}_arm", boxes=[Box((c - 2.5, 5, -2.5), (5, 2, 5), cuff())]))
        B.append(Bone(f"{side}_knee", f"{side}_leg", boxes=[Box((-2.5, 6, -3), (5, 2, 2), knee())]))
        B.append(Bone(f"{side}_boot", f"{side}_leg", boxes=[Box((-2.5, 9, -3), (5, 3, 5), boot())]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, boxes=[
            Box((-2, 0, -2.45), (4, 6, 0), ("flap", inner), sheet="cloth"),
            Box((-2, 0, 2.45), (4, 6, 0), ("flap", 3 - inner), sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard": return tabard(T, M)
    if spec == "cape": return cape(T, M)
    return flap(T, M, spec[1])

IC_BREAKER = Troop("ic_breaker", SKIN, CLOTH, bones, cloth_painter,
                   {"weapon": "ic_axe", "hand": "right", "idleArms": "hang", "portrait": "tall 4"}, tiers=(1,))
def build(): return IC_BREAKER.build()
def outputs(): return IC_BREAKER.outputs()

if __name__ == "__main__":
    IC_BREAKER.main(sys.argv)
