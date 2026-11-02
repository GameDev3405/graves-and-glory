#!/usr/bin/env python3
"""The Tidecaller Mist Caller (Tidecallers house, troop id tc_mist), hand-authored at one texel per model pixel.

A drowned priest of the fog: a deep sea-blue robe to the ground in three bell tiers, the hem ringed by a pale barnacle rune
band (the signature), a barnacle-crusted shoulder mantle, a deep hood framing a grey face with sea-foam glow eyes, wide bell
sleeves, kelp trailing from the hood, cuffs, belt and hem, a rope cord with a conch charm, and a driftwood rod topped with a
conch in the right hand (the caster carries no weapon). Sea blue is fixed paint on the robe and trim; the team cloth is a
diagonal sash with a barry-wavy tail. Rare, so the eyes and three hem runes glow. Tier I only. No back flag.

  python build_tc_mist.py preview <dir> [side] | contact <png> [side] | stub | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "tc_tideguard"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
import build_tc_tideguard as TG
from build_tc_tideguard import VERD as SEA, BARN, scuff, rng

SKIN = (128, 128)
CLOTH = (128, 64)

GREY = ramp("14191c", "2b3538", "4b5b5f", "72868a", "9db0b2")                  # a fog-grey drowned face
KELP = ramp("0e170a", "1d2f12", "2f4a1c", "4a6a2a", "6a8a40", "8fa56d")
DRIFT = ramp("1a1612", "322a22", "524738", "75664f", "9a8a6c")                 # bleached driftwood
ROPE = ramp("2a2218", "4a3d2b", "6e5d44", "8f7d5e", "b09d78")
FOAM = hx("cff3e4")                                                             # sea-foam glow

def lit(face, x, w): return LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

# ---- the robe ----
def robe(base=2, seed="mr", folds=3):
    """Sea-blue robe cloth: vertical fold lines, a wet sheen, a few barnacle specks."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = base + lit(face, x, w) + r.choice((0, 0, 0, 0, 0, 0, 1))
                if face not in ("top", "bottom"):
                    if x % folds == folds - 1: v -= 1
                    elif x % folds == 0 and y % 5 == 0: v += 1
                g[y][x] = clamp(SEA, v)
        for _ in range(w * h // 90):
            g[r.randrange(h)][r.randrange(w)] = BARN[1 + r.randrange(3)]
        return g
    return paint

def torso():
    """The robe over the chest: a lighter sea-blue placket, a pale shell-bead necklace, a belt cord at the waist."""
    base = robe(3, "mt", 4)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return g
        for x in range(w): g[0][x] = SEA[1]                                           # the collar's shadow
        if face == "front":
            for y in range(1, h):
                g[y][w // 2 - 1] = SEA[3]; g[y][w // 2] = SEA[4]                      # the placket
            for y in range(2, h, 3): g[y][w // 2 - 1] = SEA[5]
            for x in range(1, w - 1): g[1][x] = BARN[3 if x % 2 else 4]               # shell-bead necklace
            g[2][w // 2 - 1] = g[2][w // 2] = BARN[4]                                 # its pendant
            for x in range(w): g[h - 2][x] = SEA[4 if x % 2 else 3]                   # sea-blue waist band
            for x in range(w): g[h - 3][x] = SEA[1]
        if face == "back":
            for x in range(w): g[3][x] = SEA[4 if x % 2 else 3]                       # a pale yoke seam
        return g
    return paint

def sleeve():
    """A wide sea-blue sleeve to the wrist, a pale hem line, a grey drowned hand below."""
    base = robe(3, "ms", 3)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return g
        for y in range(9, h):
            for x in range(w): g[y][x] = clamp(GREY, 3 + lit(face, x, w) + (1 if (x + y) % 3 == 0 else 0))
        for x in range(w): g[8][x] = SEA[1]
        return scuff(g, ("msc", face), 2, GREY)
    return paint

def bell():
    """The bell cuff over the sleeve: a deeper sea blue, edged in pale sea blue."""
    base = robe(2, "mb", 2)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return blank(w, h)
        for x in range(w): g[h - 1][x] = SEA[5 if x % 2 else 4]
        for x in range(w): g[h - 2][x] = SEA[2]
        return g
    return paint

def panel(base):
    """A lighter sea-blue centre panel down the front and back of the skirt, carrying the placket to the hem."""
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("front", "back"):
            for y in range(h):
                g[y][w // 2 - 1] = SEA[3]; g[y][w // 2] = SEA[4]
        return g
    return paint

# ---- the hem: the signature rune band ----
GLYPHS = [("X.X", "XXX"), ("XX.", ".XX"), (".X.", "XXX"), ("X..", "X.X")]
LIT_GLYPH = ((1, 0), (2, 1), (0, 2))   # none used: the glow sheet picks the front runes itself

def hem(tier_rows, band_rows, seed="mh", glyph_row=1, ragged=True):
    """A robe tier whose lowest rows carry the pale barnacle rune band: a dark seam, a crust of pale barnacles, rows of
    carved runes, a crenulated bottom edge. band_rows = how many of the last rows are band."""
    def paint(face, w, h):
        g = panel(robe(3, seed, 3))(face, w, h)
        if face in ("top", "bottom"): return blank(w, h) if face == "bottom" else g
        if band_rows <= 0: return g
        r = rng(seed + "-band", face, w, h)
        top = h - band_rows
        for x in range(w): g[top - 1][x] = SEA[0] if top > 0 else g[top - 1][x]    # the dark seam above the band
        for y in range(top, h):
            for x in range(w):
                g[y][x] = clamp(BARN, 3 + (1 if (x + y) % 3 == 0 else 0) - (1 if (x * 2 + y) % 5 == 0 else 0) + r.choice((0, 0, 1)) + (LIT[face] // 2))
        if face in ("front", "back", "left", "right") and band_rows >= 3:
            gy = top + 1
            for k, x0 in enumerate(range(1, w - 1, 4)):
                gl = GLYPHS[(x0 // 4 + (1 if face == "back" else 0)) % len(GLYPHS)]
                for dy, row in enumerate(gl):
                    for dx, c in enumerate(row):
                        yy, xx = gy + dy, x0 + dx
                        if c == "X" and top <= yy < h and 0 <= xx < w: g[yy][xx] = BARN[1]
        if ragged:
            for x in range(w):                                                      # crenulated barnacle edge
                if r.random() < 0.3: g[h - 1][x] = None
                elif r.random() < 0.4: g[h - 1][x] = BARN[2]
        return g
    return paint

def glow_cells():
    """Three hem runes on the front lit sea-foam (texels in the lowest tier's front face)."""
    return ((2, 3), (6, 3), (10, 3))

# ---- the head ----
def face_painter_mc():
    """A fog-grey drowned face under a deep hood: a shadowed brow, sunken sockets with sea-foam eyes, a slack mouth, gill slits."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("mcf", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(GREY, 4 + lit(face, x, w) + r.choice((-1, 0, 0, 0, 1)))
        if face == "front":
            for x in range(w): g[0][x] = GREY[0]
            for x in range(w): g[1][x] = GREY[1]
            for x in range(w): g[2][x] = GREY[2]                                    # the deep hood shadow
            for x in range(w): g[3][x] = GREY[2] if x % 2 else GREY[3]
            g[4][1] = g[4][2] = g[4][5] = g[4][6] = GREY[0]                         # sunken sockets
            g[4][2] = g[4][5] = FOAM                                                # the sea-foam stare
            g[5][3] = g[5][4] = GREY[2]
            for x in (2, 3, 4, 5): g[6][x] = GREY[1]                                # the slack mouth
            g[6][3] = g[6][4] = GREY[0]
            for y in (5, 6, 7): g[y][0] = g[y][7] = GREY[1] if y % 2 else GREY[0]   # gill slits
            g[7][2] = g[7][5] = BARN[2]                                             # a barnacle on the jaw
        if face in ("left", "right"):
            for y in range(0, 3):
                for x in range(w): g[y][x] = GREY[0]
            for y in (5, 6): g[y][3] = GREY[0] if y % 2 else GREY[1]
        if face == "back":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(SEA, 1 + (x + y) % 2)
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(SEA, 1 + (x + y) % 2)
        return scuff(g, ("mcf", face), 3, GREY)
    return paint

def hood():
    """The hood: a sea-blue cowl round the head with its front open, pale stitched rim, wet sheen."""
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("front", "bottom"): return g
        r = rng("mhd", face, w, h)
        for y in range(h):
            for x in range(w):
                v = (3 if face in ("back", "left") else 2) + lit(face, x, w) + r.choice((0, 0, 0, 0, 0, 1))
                if face not in ("top",):
                    if x % 3 == 2: v -= 1
                    elif (x * 2 + y) % 7 == 0: v += 1
                g[y][x] = clamp(SEA, v)
        if face in ("left", "right"):
            for y in range(h): g[y][w - 1] = SEA[4] if y % 2 else SEA[3]            # the pale front edge (w-1 is toward -z or +z by face)
            for y in range(h): g[y][0] = SEA[1]
        if face == "top":
            for x in range(w): g[h - 1][x] = SEA[4 if x % 2 else 3]
        if face == "back":
            for x in range(w): g[h - 1][x] = SEA[1]
            for y in range(h): g[y][w // 2] = SEA[1]
        for _ in range(max(1, w * h // 36)):
            g[r.randrange(h)][r.randrange(w)] = BARN[1 + r.randrange(3)]
        return g
    return paint

def brow():
    """The hood's overhanging brow, a dark slab with a pale rim."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(SEA, 1 + (LIT[face] if face not in ("top", "bottom") else 0) + (1 if face == "top" else 0))
        if face == "front":
            for x in range(w): g[0][x] = SEA[4 if x % 2 else 3]
            for x in range(w): g[h - 1][x] = SEA[0]
        return g
    return paint

def peak():
    """A drooping cowl tip at the back of the hood."""
    base = robe(1, "mp", 2)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("front", "back"):
            for x in range(w): g[h - 1][x] = SEA[4 if x % 2 else 3]
        return g
    return paint

# ---- mantle, kelp, charms ----
def mantle():
    """A shoulder mantle crusted with pale barnacle clusters over deep sea blue."""
    def paint(face, w, h):
        g = robe(2, "mm", 4)(face, w, h)
        r = rng("mmc", face, w, h)
        for _ in range(max(3, w * h // 5)):
            cx, cy = r.randrange(w), r.randrange(h)
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                if 0 <= cx + dx < w and 0 <= cy + dy < h: g[cy + dy][cx + dx] = clamp(BARN, 2 + r.randrange(3) + (1 if face == "top" else 0))
        if face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[4 if x % 2 else 3]
        return g
    return paint

def kelp(seed, hem=2):
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("mkelp", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 4 + LIT[face] // 2 + r.choice((-1, 0, 0, 1))
                if face in ("front", "back") and (x == w // 2 and w > 1): v -= 1
                if y % 3 == 2 and face in ("front", "back") and x != w // 2: v += 1
                g[y][x] = clamp(KELP, v)
        for y in range(max(0, h - hem), h):
            for x in range(w):
                if r.random() < 0.3 + 0.3 * (y - (h - hem)): g[y][x] = None
        return g
    return paint

def cord():
    """The rope cord belt knotted at the front with a sea-blue knot."""
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

def conch():
    """A pale barnacle-crusted conch: banded whorls, a dark mouth."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("mconch", face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + (1 if face == "top" else 0) + LIT[face] // 2 + r.choice((-1, 0, 0, 1))
                if y % 2 == 1 and face not in ("top", "bottom"): v -= 1
                g[y][x] = clamp(BARN, v)
        if face == "front" and w >= 2 and h >= 2: g[h - 1][w // 2] = SEA[0]
        return g
    return paint

def rod():
    """The caster's driftwood rod: bleached, ringed with pale lashings."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("mrod", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(DRIFT, 3 + LIT[face] // 2 + r.choice((-1, 0, 0, 1)))
        for y in range(1, h, 3):
            for x in range(w): g[y][x] = SEA[3]
        return g
    return paint

# ---- cloth ----
def sash_cloth(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (1 if y == 0 else 0))
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "tail": return TG.tabard(T, M)
    return sash_cloth(T, M)

def glow_fn(bone, box):
    if bone.name == "head" and box.size == [8, 8, 8]:
        def p(face, w, h):
            g = blank(w, h)
            if face == "front": g[4][2] = g[4][5] = FOAM
            return g
        return p
    if bone.name == "skirt_hem" and box.size[0] == 11:
        def p(face, w, h):
            g = blank(w, h)
            if face == "front":
                for x, y in ((2, 3), (6, 3), (10, 3)):
                    if y < h and x < w: g[y][x] = FOAM
            return g
        return p
    return None

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), torso())]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face_painter_mc())]))
    B.append(Bone("hood", "head", boxes=[Box((-4.5, -8.5, -5.5), (9, 9, 10), hood()), Box((-4.5, -9.5, -5.5), (9, 1, 1), brow())]))
    B.append(Bone("hood_peak", "head", (0, -4, 4.5), rot=(18, 0, 0), boxes=[Box((-2, 0, 0), (4, 6, 2), peak())]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), sleeve())]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("rod", "right_hand", tag="weapon", boxes=[
        Box((-0.5, -0.5, -7), (1, 1, 7), rod()), Box((-1, -1, -9), (2, 2, 2), conch()), Box((-0.5, -2, -8.5), (1, 1, 1), conch())]))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), sleeve())]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), robe(1, "ml", 3))]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), robe(1, "ml", 3))]))
    # the robe to the ground in three bell tiers; the hem tier carries the rune band
    B.append(Bone("skirt", "root", boxes=[Box((-4.5, 11.5, -2.5), (9, 4, 5), panel(robe(3, "m1", 3))),
                                          Box((-5, 15.5, -3), (10, 3, 6), panel(robe(3, "m2", 3)))]))
    B.append(Bone("skirt_hem", "root", boxes=[Box((-5.5, 18.5, -3.5), (11, 5, 7), hem(5, 4, "m3"))]))
    B.append(Bone("mantle", "body", boxes=[Box((-5, -13.5, -3), (10, 4, 6), mantle(), dil=0.2)]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 1, 4), cord(), dil=0.5)]))
    B.append(Bone("charm", "body", boxes=[Box((2.2, -2, -2.8), (1, 1, 1), cord()), Box((1.7, -1, -3), (2, 3, 1), conch())]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_bell", f"{side}_arm", boxes=[Box((-2 + (0 if side == "right" else -1), 3, -2), (4, 5, 4), bell(), dil=0.8)]))
        B.append(Bone(f"{side}_kelp_cuff", f"{side}_arm", boxes=[Box((-0.5 + (0 if side == "right" else -0.5) + 0.5 * (c > 0) * 0, 8, 1.9), (1, 6, 1), kelp((side, "cuff"), 2))]))
        B.append(Bone(f"{side}_kelp_hood", "head", boxes=[Box((4.6 * c - 0.5, -2, -2.5), (1, 8, 1), kelp((side, "hood"), 3)),
                                                         Box((4.6 * c - 0.5, -2, 1.5), (1, 6, 1), kelp((side, "hood2"), 2))]))
    B.append(Bone("kelp_belt", "body", boxes=[Box((-3.5, -2, -2.7), (1, 9, 1), kelp("belt", 3)), Box((3.2, -2, 2.3), (1, 8, 1), kelp("belt2", 3))]))
    B.append(Bone("kelp_hem", "root", boxes=[Box((-3.5, 22, -4), (1, 3, 1), kelp("hem", 2)), Box((2.5, 22, -4), (1, 2, 1), kelp("hem2", 1))]))
    # team cloth: a diagonal sash over the chest and a barry-wavy tail at the hip
    B.append(Bone("sash", "body", team=True, pivot=(0, -7, 0), rot=(0, 0, 40), boxes=[Box((-4, -1, -2.5), (8, 2, 5), "sash", sheet="cloth")]))
    B.append(Bone("sash_tail", "body", team=True, boxes=[
        Box((-4, -1, -2.95), (3, 8, 0), "tail", sheet="cloth"), Box((-4, -1, 2.95), (3, 8, 0), "tail", sheet="cloth")]))
    return B

NOTE = ("Tidecaller Mist Caller (unit identity, Tidecallers): a drowned priest of the fog in a deep sea-blue robe to the ground, a pale barnacle "
        "rune band round the hem (the signature), a barnacle-crusted shoulder mantle, a deep hood over a grey face with sea-foam glow eyes, "
        "wide bell sleeves, kelp trailing from the hood, cuffs, belt and hem, a rope cord with a conch charm, a driftwood rod with a conch in "
        "the right hand, a team sash with a barry-wavy tail. Tier I only. No back flag.")
TC_MIST = Troop("tc_mist", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "standard"}, tiers=(1,), glow=glow_fn)
def build(): return TC_MIST.build()
def outputs(): return TC_MIST.outputs()

if __name__ == "__main__":
    house_stub.main(TC_MIST, NOTE, sys.argv)
