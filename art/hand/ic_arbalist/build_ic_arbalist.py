#!/usr/bin/env python3
"""The Ironclad Arbalist (Ironclad house, troop id ic_arbalist), hand-authored at one texel per model pixel.

A lighter man than the Shieldbreaker, built to stand behind him: an ember-banded kettle hat over a mail aventail with a
slit of face showing, a riveted brigandine of dark leather and iron studs, a short team surcoat with crossed bolts, a
cross-body strap, fauld and mail legs, and a quiver of ember-fletched bolts on his back. Tier I only.

  python build_ic_arbalist.py preview <dir> [side] | contact <png> [side] | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
from ironclad import *   # noqa: F401,F403
from handlib import Troop

SKIN = (128, 128)
CLOTH = (128, 64)
P = pal_ic()
FACE = SKINS["olive"]

# ---- the kettle hat and aventail ----
HAT_FRONT = fold("""
34555
23445
qrrss
12233""")

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

def hat_crown():
    def paint(face, w, h):
        if face == "front": return grid(HAT_FRONT, P)
        if face == "bottom": return blank(w, h)
        g = lames([2, 2], base=3, seed="kc", wear=0, rivets=False)(face, w, h)
        if face != "top":
            for x in range(w): g[2][x] = EMBER[2]               # one solid ember hatband
        return g
    return paint

def hat_brim():
    def paint(face, w, h):
        g = plate(R=IRON, base=3, spec=False, seam=False)(face, w, h)
        if face == "bottom": g = [[IRON[1]] * w for _ in range(h)]
        return g
    return paint

def aventail():
    """Mail round the lower head with a slit of olive face between the brim and the mail."""
    def paint(face, w, h):
        g = calm_mail(3)(face, w, h)
        if face == "front":
            for x in range(1, w - 1): g[0][x] = FACE[3] if x not in (3, 4) else FACE[2]
            for x in (2, 5): g[0][x] = INK            # the eyes
            for x in range(1, w - 1): g[1][x] = FACE[2]
        if face == "bottom": return blank(w, h)
        return g
    return paint

# ---- body ----
def body_painter():
    def paint(face, w, h):
        g = leather_ic(base=2)(face, w, h) if face in ("top", "bottom") else blank(w, h)
        if face in ("top", "bottom"): return g
        for y in range(h):
            for x in range(w):
                v = 2 + LIT[face] + lightx(face, x, w) + (1 if y == 0 else 0) + (-1 if y == h - 1 else 0)
                g[y][x] = clamp(OILED, v)
        for y in (2, 5):                                    # two rows of iron studs, the only detail
            for x in range(1, w - 1, 2): g[y][x] = IRON[4]
        return g
    return paint

def fauld():
    """The belt (two leather rows with an iron buckle) over two plates of fauld, all in one box."""
    plates = lames([2], base=3, seed="fa", wear=0, rivets=False)
    strap = leather_ic(base=1)
    def paint(face, w, h):
        if face in ("top", "bottom"): return plates(face, w, h)
        g = blank(w, h)
        belt, pl = strap(face, w, 2), plates(face, w, 2)
        for x in range(w): g[0][x], g[1][x] = belt[0][x], belt[1][x]
        for y in range(2, h):
            for x in range(w): g[y][x] = pl[y - 2][x]
        if face == "front":
            for y, row in enumerate(("56", "43")):          # an iron buckle, lit top-left
                for x, c in enumerate(row): g[y][3 + x] = IRON[int(c)]
        return g
    return paint

def arm():
    """Mail sleeve, ONE vambrace plate (lit top, dark lip), a plain leather glove: three masses, no stripes."""
    return rows([(0, calm_mail(2)), (4, lames([5], base=3, seed="aarm", wear=1, rivets=False)), (9, leather_ic(base=2))])

def leg():
    return rows([(0, calm_mail(2)), (5, lames([4], base=3, seed="aleg", wear=1, rivets=False)), (9, leather_ic(base=2))])

def pauldron(i):
    return lames([2], base=4 if i == 0 else 3, seed=("ap", i), wear=0, rivets=False)

def quiver():
    """A leather tube: a darker lip, one iron band, a dark mouth on top; the fletched bolts stand above it."""
    def paint(face, w, h):
        g = leather_ic(base=3)(face, w, h)
        if face == "top": return [[IRON[0]] * w for _ in range(h)]
        if face == "bottom": return g
        g[0] = [clamp(OILED, 2)] * w
        g[3] = [IRON[5 if face in ("front", "right") else 4]] * w      # the single band
        g[h - 1] = [clamp(OILED, 1)] * w
        return g
    return paint

def fletch(tone):
    """One bolt's fletching, a lit tip and a darker body."""
    def paint(face, w, h):
        return [[(EMBER[5 if tone else 4] if y == 0 else (EMBER[3] if y < h - 1 else EMBER[2])) for x in range(w)] for y in range(h)]
    return paint

# ---- cloth ----
# crossed bolts straight on the team field: bright shafts, ember fletching at the top ends. c shaft, e fletch
CROSS6 = """
.ffff.
fcffcf
ffccff
ffccff
fcffcf
.ffff."""

def cross_pal(T, M):
    # M is the team metal (gold, or silver on the gold side): its brightest tone reads on every side
    return {"c": M[-1], "f": T[1]}

def surcoat(T, M):
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
            # the baldric first (right shoulder to left hip in front; on the back it climbs from the quiver's mouth to the
            # right shoulder), then the badge over it on the front, so the strap passes behind the badge
            strap = [(i, i) for i in range(8)] if face == "front" else [(7 - i, i) for i in range(5)]
            for x, y in strap:
                g[y][x] = clamp(OILED, 4)
                if y + 1 < 8 and (face == "back" or x + 1 < w): g[y + 1][x] = clamp(OILED, 2)
            if face == "front": over(g, grid(CROSS6, cross_pal(T, M)), 1, 2)    # rows 2-7: clear of the mantle above and the fauld below
            for y in range(8, h):                           # rows 8-11 sit under the fauld: left empty, not hidden by luck
                g[y] = [None] * w
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

def halfcape(T, M):
    """A plain half cape hung from the right shoulder: one tone, a shaded edge, an ember hem."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = (2 if face != "back" else 3) + (-1 if face in ("left", "right") else 0)
                g[y][x] = clamp(T, v)
        g[h - 1] = [EMBER[2]] * w
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0)))
    B.append(Bone("helm", "head", tag="helm", boxes=[
        Box((-4.5, -8.5, -4.5), (9, 4, 9), hat_crown()),
        Box((-5.5, -5, -5.5), (11, 1, 11), hat_brim()),
        Box((-4, -4, -4), (8, 5, 8), aventail(), dil=0.3)]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm())]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm())]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg())]))
    B.append(Bone("fur_mantle", "body", boxes=[Box((-4.5, -13, -2.5), (9, 2, 5), fur(seed="am"), dil=0.2)]))
    B.append(Bone("tabard", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "tabard", dil=0.3, sheet="cloth")]))
    B.append(Bone("fauld", "body", boxes=[Box((-4.5, -4, -2.5), (9, 4, 5), fauld(), dil=0.1)]))
    # the quiver sits straight and flush on the back, inside the torso's width so a swinging arm (x beyond +-4) never meets it
    B.append(Bone("quiver", "body", (2.1, -8, 2.7), boxes=[
        Box((-1.5, 0, 0), (3, 8, 2), quiver()),
        Box((-1.2, -2.4, 0.0), (1, 2, 1), fletch(1)), Box((0.2, -1.8, 0.0), (1, 2, 1), fletch(0)),
        Box((-0.5, -2.2, 1.0), (1, 2, 1), fletch(0)), Box((0.5, -1.6, 1.0), (1, 2, 1), fletch(1))]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 12 * c), boxes=[
            Box((-3.5, -1.5, -3.5), (7, 2, 7), pauldron(0)),
            Box((-3.5 + 0.5 * c, 0.5, -3), (7, 2, 6), pauldron(1), dil=0.15)]))
        B.append(Bone(f"{side}_knee", f"{side}_leg", boxes=[Box((-2.5, 6, -3), (5, 2, 2), lames([2], base=4, seed="ak", wear=0, rivets=False))]))
        B.append(Bone(f"{side}_boot", f"{side}_leg", boxes=[Box((-2.5, 9, -3), (5, 3, 5), rows([(0, leather_ic(base=2)), (2, leather_ic(base=1, stitch=True))]))]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, boxes=[
            Box((-2, 0, -2.45), (4, 6, 0), ("flap", inner), sheet="cloth"),
            Box((-2, 0, 2.45), (4, 6, 0), ("flap", 3 - inner), sheet="cloth")]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tabard": return surcoat(T, M)
    if spec == "halfcape": return halfcape(T, M)
    return flap(T, M, spec[1])

IC_ARBALIST = Troop("ic_arbalist", SKIN, CLOTH, bones, cloth_painter,
                    {"weapon": "ic_crossbow", "hand": "right", "idleArms": "hang", "portrait": "tall 4"}, tiers=(1,))
def build(): return IC_ARBALIST.build()
def outputs(): return IC_ARBALIST.outputs()

if __name__ == "__main__":
    IC_ARBALIST.main(sys.argv)
