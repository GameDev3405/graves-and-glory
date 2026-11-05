#!/usr/bin/env python3
"""Admiral Vael, the Drowned Tide (Tidecallers house, troop id tc_admiral, Legendary), hand-authored at one texel per model pixel.

The grandest Tidecaller: a once-magnificent naval coat gone to the sea. The signature is the 12 px tricorne (felt, gilt lace, a drooping
sea-blue plume, a cockade), over a pale drowned face with silver hair adrift. A deep sea-navy coat with sea-blue lapels, cuffs and collar,
double rows of gilt buttons, a sea-blue sash with a gilt buckle, gilt epaulettes with fringe, and four calf-length coat tails flaring from
the waist, hems ragged and hung with kelp, barnacles crusted on the shoulders. A tarnished anchor is slung on the back (back gear, no flag).
Gilt #96783A is trim only; the sea blue stays the larger share. Team cloth: the sash end at the hip and the cravat. Tier II (the Drowned Commodore): a bronze
half-cuirass rimmed in sea blue under the lapels, plate pauldrons under the epaulettes, vambraces and greaves, a bronze-studded
sea-blue hat band. Tier III (the Drowned Tide): deep sea-blue plate chased in warm bronze, a ring of pearls round the tricorne
crown, a pearl at the gorget and a third plume.

  python build_tc_admiral.py preview <dir> [side] | contact <png> [side] | stub | install
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "tc_tideguard"))
sys.path.insert(0, os.path.join(HERE, "..", "tc_harpooner"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import tidecallers as TC
import build_tc_tideguard as TG
import build_tc_harpooner as HP
from build_tc_tideguard import VERD as SEA, FLESH, BARN, scuff, rng

SKIN = (128, 128)
CLOTH = (128, 64)

COAT = ramp("0c1830", "16305c", "214c80", "2e6297", "6691b7", "9ab6cc")          # officer's sea blue (Kort 2026-11-02: lighter, toward SEA), lights under the tint rule
GILT = ramp("241a08", "4a3815", "6f5a26", "96783a", "b89a54", "dcc487")          # tarnished naval gilt, #96783A at the core
PALE = ramp("2c3a3c", "52666a", "7e9694", "a9bfbb", "cfdcd6", "ecf4ee")          # the drowned face
HAIR = ramp("14223a", "2a4466", "4a6a8e", "7a98b4", "b0c4d4", "e2ecf2")          # sea-silver hair, adrift (bluer, Kort 2026-11-02)
FELT = ramp("0b0f1a", "171d2c", "242d42", "34405a", "4a5a7a")                    # the tricorne's sodden felt
IRON = HP.IRON
TAR, ROPE, KELP = HP.TAR, HP.ROPE, HP.KELP
FOAM = hx("cff3e4")

def lit(face, x, w): return LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

def coat_base(base=3, seed="ac", wear=1):
    """The coat cloth: a wet vertical sheen, salt stains, barnacle specks."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("ac", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = base + lit(face, x, w)                                          # lit faces sit on the light tone 4, shaded sides on vivid tone 3
                if face not in ("top", "bottom") and x % 4 == 3 and y % 6 > 1: v -= 1   # a few long fold lines
                g[y][x] = clamp(COAT, v)
        for _ in range(w * h * wear // 80):
            g[r.randrange(h)][r.randrange(w)] = COAT[2]                             # sparse salt-dark stains
        return g
    return paint

def gilt_row(g, y, x0, x1, alt=True):
    for x in range(x0, x1):
        if 0 <= y < len(g) and g[y][x] is not None: g[y][x] = GILT[4 if (x % 2 == 0 or not alt) else 3]

def body_coat():
    """The coat body: sea-blue lapels and collar, gilt-edged, two columns of gilt buttons, medals, a pale cravat gap."""
    base = coat_base(4, "body")
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return g
        if face == "front":
            for x in range(w): g[0][x] = SEA[4 if x % 2 else 3]
            for y in range(2, 10):
                wl = 3 if y <= 4 else 2
                for x in range(wl):
                    for xx in (x, w - 1 - x):
                        g[y][xx] = SEA[1 if x else 0] if x < wl - 1 or wl == 2 else SEA[1]   # dark sea-blue facings
                ei = wl                                                             # the gilt edge along the lapel
                g[y][ei] = GILT[3] if y % 2 else SEA[4]; g[y][w - 1 - ei] = GILT[3] if y % 2 else SEA[4]
            for y in range(2, 5):
                for x in (3, 4): g[y][x] = BARN[2 if y == 2 else 3]                 # a sodden linen shirt
            for y in range(5, h):
                g[y][3] = COAT[1]; g[y][4] = COAT[1]                                 # the closed seam
            for y in (5, 7, 9):
                for x in (2, 5): g[y][x] = GILT[5]
                for x in (3, 4): g[y][x] = COAT[2]
            g[3][1] = GILT[5]; g[3][2] = GILT[4]                                     # medals on the right lapel
            g[4][6] = SEA[5]; g[5][6] = GILT[4]
            for x in range(w): g[h - 1][x] = COAT[1]
            g[10][0] = g[10][w - 1] = GILT[3]
        elif face == "back":
            for x in range(w): g[0][x] = SEA[4 if x % 2 else 3]
            for y in range(1, h): g[y][w // 2 - 1] = COAT[1]; g[y][w // 2] = COAT[1]
            for y in (1, 3): g[y][w // 2 - 1] = GILT[3]
            for x in range(w): g[2][x] = COAT[2 if x % 2 else 1]                      # yoke seam
        else:
            for x in range(w): g[0][x] = SEA[4 if x % 2 else 3]
            for y in range(1, 3):
                for x in range(w): g[y][x] = COAT[3]
        return g
    return paint

def collar():
    """The standing collar: sea-blue, gilt lace on the upper edge, a gilt anchor button at the front."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("col", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(SEA, 3 + lit(face, x, w) + r.choice((-1, 0, 0, 1)))
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = GILT[4 if x % 2 else 3]
            for x in range(w): g[h - 1][x] = SEA[1]
        if face == "front":
            g[h // 2][w // 2 - 1] = GILT[5]
        return scuff(g, ("col", face), 2, SEA)
    return paint

def sleeve_painter():
    """Arm: coat sleeve, a deep sea-blue cuff with gilt lace and three buttons, then a pale drowned hand."""
    sl = coat_base(4, "arm")
    def hand(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(PALE, 1 + lit(face, x, w) + (1 if y < 2 else 0))
        return scuff(g, ("hand", face), 2, PALE)
    def cuff(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(SEA, 3 + lit(face, x, w) + (1 if y == 1 else 0) - (1 if y == h - 1 else 0))
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = GILT[3] if x % 2 else SEA[4]
            for x in range(w): g[h - 1][x] = SEA[1]
            if face in ("front", "back"):
                g[1][w // 2] = GILT[5]
        return scuff(g, ("cuff", face), 2, SEA)
    return rows([(0, sl), (5, cuff), (9, hand)])

def breeches():
    base = coat_base(2, "br")
    return base

def boot():
    """Tall tarred sea boots: a turned sea-blue cuff with gilt edge at the top, barnacles at the foot."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("bt", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(TAR, 2 + lit(face, x, w) + r.choice((-1, 0, 0, 1)))
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = SEA[4 if x % 2 else 3]
            for x in range(w): g[1][x] = SEA[2]
            for _ in range(max(2, w * h // 8)): g[r.randrange(2, h)][r.randrange(w)] = BARN[1 + r.randrange(4)]
        return g
    return paint

def leg_painter():
    return rows([(0, coat_base(3, "lg")), (5, boot())])

def foot():
    def paint(face, w, h):
        g = TG.plate_b(TAR, 1, "adf")(face, w, h)
        if face == "front":
            for x in range(w // 2 - 1, w // 2 + 1): g[0][x] = GILT[4]            # a gilt buckle
        return g
    return paint

def sash():
    """The waist sash: sea-blue silk folds, a gilt-bordered buckle plate at the front."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(SEA, 3 + lit(face, x, w) + (1 if (x + y) % 3 == 0 else 0) - (1 if y == h - 1 else 0))
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = GILT[3 if x % 2 else 2]
        if face == "front":
            for x in (w // 2 - 1, w // 2):
                for y in range(h): g[y][x] = GILT[5 if y == 0 else 4]
            g[h - 1][w // 2 - 1] = g[h - 1][w // 2] = GILT[2]
        return g
    return paint

# ---- the coat tails ----
def tail_panel(kind, c):
    """A tail panel to the calf: coat cloth, a sea-blue facing down the edge, a gilt hem band, a ragged hem."""
    base = coat_base(4, "t" + kind, 0)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return blank(w, h)
        r = rng("tail", kind, face, w, h)
        for y in range(h):
            if face in ("front", "back"):
                # the edge facing: a sea-blue column with a gilt border, on the free edge of the panel
                if kind == "front":
                    ex = (w - 1) if c > 0 else 0
                    g[y][ex] = SEA[4 if y % 2 else 3]
                    g[y][ex - 1 if c > 0 else ex + 1] = GILT[3 if y % 2 else 2]
                elif kind == "back":
                    ex = 0 if c > 0 else (w - 1)
                    g[y][ex] = SEA[3]
                    g[y][ex + 1 if c > 0 else ex - 1] = GILT[3 if y % 2 else 2]
        if face in ("front", "back"):
            for x in range(w): g[h - 3][x] = GILT[3] if x % 2 == 0 else SEA[3]          # the hem band
            for x in range(w): g[h - 4][x] = SEA[2]
            for y in range(1, h - 4, 4): g[y][w // 2] = GILT[4]                      # buttons down the panel
        if face in ("left", "right"):
            for y in range(h - 5, h):                                                # turned-back sea-blue lining flashes
                for x in range(w): g[y][x] = SEA[2 + (1 if (x + y) % 2 == 0 else 0)]
            for x in range(w): g[h - 3][x] = SEA[4 if x % 2 else 3]
            g[h - 6][w // 2] = GILT[5]
        for x in range(w):                                                           # ragged, kelp-fouled hem
            if r.random() < 0.5: g[h - 1][x] = None
            elif g[h - 1][x] is not None and r.random() < 0.5: g[h - 1][x] = COAT[0]
        return g
    return paint

# ---- head and hair ----
def face_painter_vael():
    """A drowned admiral's face: bone-pale, hollow cheeks, deep sockets with sea-foam eyes, a stern slack mouth, a barnacled jaw."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("vf", face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(PALE, 3 + lit(face, x, w) + r.choice((-1, 0, 0, 0, 1)))
        if face in ("front", "left", "right"):
            for x in range(w): g[0][x] = HAIR[1]                                     # the hat brim's shadow and hairline
            for x in range(w): g[1][x] = PALE[1] if x % 2 else PALE[0]
        if face == "front":
            for x in range(1, 7): g[3][x] = PALE[1]                                  # heavy brow
            g[4][1] = g[4][2] = g[4][5] = g[4][6] = PALE[0]
            g[4][2] = g[4][5] = FOAM                                                 # a cold sea-foam stare
            g[5][3] = g[5][4] = PALE[2]                                              # the long nose
            g[5][0] = g[5][7] = PALE[1]; g[6][0] = g[6][7] = PALE[1]
            g[5][1] = g[5][6] = PALE[2]; g[6][1] = g[6][6] = PALE[1]                 # hollow cheeks
            for x in (2, 3, 4, 5): g[6][x] = PALE[1]                                  # the stern mouth
            g[6][3] = g[6][4] = PALE[0]
            g[7][3] = g[7][4] = PALE[2]
            g[7][1] = BARN[2]; g[7][6] = BARN[3]                                      # barnacles on the jaw
            g[2][0] = g[2][1] = g[2][6] = g[2][7] = HAIR[2]                           # temple hair
            g[3][0] = g[3][7] = HAIR[3]
        if face in ("left", "right"):
            for y in range(1, 5):
                for x in range(w): g[y][x] = clamp(HAIR, 2 + (x + y) % 2 + (1 if y < 3 else 0))   # hair over the ear
            g[5][2] = g[5][1] = PALE[1]
        if face == "back":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(HAIR, 3 + (x * 2 + y) % 3)
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(HAIR, 2 + (x + y) % 2)
        return scuff(g, ("vf", face), 3, PALE) if face in ("front",) else g
    return paint

def hair(seed="hr", fine=False):
    """Silver-blue hair drifting in the current: long strands, a few darker, tips frayed."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("hair", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] // 2 + r.choice((-1, 0, 0, 1))
                if face in ("front", "back", "left", "right") and (x + y // 3) % 3 == 0: v -= 1
                g[y][x] = clamp(HAIR, v)
        for y in range(max(0, h - 3), h):
            for x in range(w):
                if r.random() < 0.28 + 0.2 * (y - (h - 3)): g[y][x] = None
        return g
    return paint

# ---- the tricorne ----
def felt(seed="fl", band=False, lace=True):
    """Sodden black-navy felt: a nap, a damp sheen, a dotted gilt lace on the free edge."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("felt", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + lit(face, x, w) + r.choice((-1, 0, 0, 1))
                if face not in ("top", "bottom") and (x * 2 + y) % 6 == 0: v += 1
                g[y][x] = clamp(FELT, v)
        if face == "bottom": return [[FELT[0]] * w for _ in range(h)]
        if lace and face not in ("top", "bottom") and w >= 3:
            for x in range(w): g[0][x] = GILT[3 if x % 3 else 2]
        if lace and face == "top" and w >= 3 and h >= 3:
            for x in range(w): g[0][x] = GILT[3 if x % 3 else 2]; g[h - 1][x] = GILT[3 if x % 3 else 2]
            for y in range(h): g[y][0] = GILT[3 if y % 3 else 2]; g[y][w - 1] = GILT[3 if y % 3 else 2]
        if band and face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = SEA[3]
            for x in range(w): g[h - 2][x] = SEA[2]
        for _ in range(max(1, w * h // 40)): g[r.randrange(h)][r.randrange(w)] = BARN[0 + r.randrange(2)]
        return scuff(g, ("fl", seed, face), max(1, w * h // 30), FELT)
    return paint

def wing(seed):
    """An upturned brim wing: dark felt, a dotted gilt lace along its top edge."""
    return felt(seed)

def cockade():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                d = max(abs(x - (w - 1) / 2), abs(y - (h - 1) / 2))
                g[y][x] = SEA[5] if d < 0.6 else (SEA[4] if d < 1.2 else GILT[4])
        return g
    return paint

def plume(seed):
    """A drenched sea-blue plume, ribbed along the quill, edges pale."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("plume", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + (1 if y % 2 == 0 else 0) + r.choice((-1, 0, 0, 1))
                if face in ("left", "right"):
                    q = w // 2
                    if x == q: v = 5                                                 # the quill
                    elif abs(x - q) >= 2: v -= 1
                    if (x + y) % 4 == 0: v -= 1
                g[y][x] = clamp(SEA, v)
        for y in range(max(0, h - 2), h):
            for x in range(w):
                if r.random() < 0.4: g[y][x] = None
        return g
    return paint

# ---- epaulettes, anchor, kelp ----
def epaulette(seed):
    """A gilt epaulette: a sea-navy pad ringed in bullion rows, a sea-blue button, one or two barnacles."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("ep", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                if face == "top":
                    v = 3 + (1 if x % 2 == 0 else 0)
                    g[y][x] = clamp(GILT, v) if (y in (0, h - 1) or x in (0, w - 1) or (x + y) % 3 == 0) else clamp(COAT, 2 + (x + y) % 2)
                else:
                    g[y][x] = clamp(GILT, 3 + lit(face, x, w) + (1 if y % 2 == 0 else 0))
        if face == "top":
            g[h // 2][w // 2] = SEA[5]; g[h // 2][w // 2 - 1] = SEA[4]
        for _ in range(max(1, w * h // 14)): g[r.randrange(h)][r.randrange(w)] = BARN[1 + r.randrange(4)]
        return g
    return paint

def fringe(seed):
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("fr", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                if face in ("front", "back") and x % 2 == 1 and y > 0: continue    # gaps between bullion tassels
                g[y][x] = clamp(GILT, 3 + (1 if y % 2 == 0 else 0) + lit(face, x, w) // 2 + r.choice((-1, 0, 0, 1)))
        for x in range(w):
            if r.random() < 0.35: g[h - 1][x] = None
        return g
    return paint

ANCHOR = ramp("1e1408", "3a2812", "5a4020", "7a5a30", "967846", "bfa070")        # tarnished bronze, so the back reads warm (Kort 2026-11-02)

def anchor_iron(seed, bar=False):
    """The anchor: tarnished bronze, flat with a lit top-left edge, a few sea-blue patina pits."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("an", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(ANCHOR, 3 + lit(face, x, w) + (1 if y == 0 and face not in ("top", "bottom") else 0))
        if face not in ("top", "bottom"):
            for _ in range(w * h // 10): g[r.randrange(h)][r.randrange(w)] = SEA[2]
        return g
    return paint

def rope_loop(seed):
    return HP.rope(seed)

# ---- cloth ----
def sash_end(T, M):
    """The sash end hanging from the knot: team silk with gilt-lined stripes and a ragged tip."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (1 if (x + y // 3) % 3 == 0 else 0)
                g[y][x] = clamp(T, v)
        for y in range(h): g[y][0] = GILT[3 if y % 2 else 2]
        for y in range(h): g[y][w - 1] = GILT[3 if y % 2 else 2]
        r = rng("send", face)
        for x in range(w):
            if r.random() < 0.45: g[h - 1][x] = None
        return g
    return paint

def cravat(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (1 if (x + y) % 3 == 0 else 0))
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "send": return sash_end(T, M)
    return cravat(T, M)

# ---- assembling ----
def strand(name, parent, pivot, rot, boxes):
    return Bone(name, parent, pivot, [Box(o, s, HP.kelp((name, i))) for i, (o, s) in enumerate(boxes)], rot=rot)

def hair_strand(name, parent, pivot, rot, boxes):
    return Bone(name, parent, pivot, [Box(o, s, hair((name, i))) for i, (o, s) in enumerate(boxes)], rot=rot)

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), body_coat())]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face_painter_vael())]))
    # hair: a back fall down the nape, side locks over the ears and drifting strands, all upon the head
    B.append(Bone("hair_back", "head", boxes=[Box((-4.5, -7.5, 4), (9, 10, 1.6), hair("hb"))]))
    B.append(Bone("hair_lock_r", "head", boxes=[Box((-5.1, -6, -3), (1.2, 7, 3.4), hair("lr"))]))
    B.append(Bone("hair_lock_l", "head", boxes=[Box((3.9, -6, -3), (1.2, 7, 3.4), hair("ll"))]))
    B.append(hair_strand("hair_adrift_l", "head", (4.4, -2, 4.6), (-6, 0, 62), [((-1, 0, 0), (2, 8, 1))]))
    B.append(hair_strand("hair_adrift_r", "head", (-4.4, -3, 4.6), (-10, 0, -58), [((-1, 0, 0), (2, 9, 1))]))
    # the 12 px tricorne: a crown, two upturned wings, a front peak and a back corner, gilt lace, cockade, sea-blue plume
    B.append(Bone("hat_crown", "head", tag="helm", boxes=[
        Box((-4.6, -10.8, -4.6), (9.2, 3.6, 9.2), felt("cr", band=True, lace=False)),
        Box((-3.8, -11.6, -3.8), (7.6, 0.8, 7.6), felt("ct", lace=False))]))
    for c in (-1, 1):
        n = "l" if c > 0 else "r"
        B.append(Bone(f"hat_wing_{n}", "head", (c * 4.4, -7.8, 0), rot=(0, 0, c * -7), tag="helm", boxes=[
            Box((0 if c > 0 else -1.4, -4.4, -5.8), (1.4, 4.6, 11.6), wing(n))]))
    B.append(Bone("hat_peak", "head", (0, -8.2, -4.6), rot=(10, 0, 0), tag="helm", boxes=[
        Box((-3.6, -3.4, -2.2), (7.2, 3.6, 1.6), felt("pk")), Box((-3.0, -1.6, -4.4), (6.0, 1.4, 2.4), felt("pk2"))]))
    B.append(Bone("hat_back", "head", (0, -8.2, 4.6), rot=(-12, 0, 0), tag="helm", boxes=[
        Box((-3.6, -3, 0.4), (7.2, 3.4, 1.6), felt("bk"))]))
    B.append(Bone("cockade", "head", tag="helm", boxes=[Box((5.7, -10.4, -2.2), (0.8, 2.4, 2.4), cockade())]))
    B.append(Bone("plume_a", "head", (5.8, -10.4, 1.4), rot=(0, 0, -34), tag="helm", boxes=[Box((-0.6, -1, -1.6), (1, 6, 4), plume("a"))]))
    B.append(Bone("plume_b", "head", (7.6, -5.6, 1.4), rot=(24, 0, -4), tag="helm", boxes=[Box((-0.6, -0.4, -1.6), (1, 7, 4), plume("b"))]))
    B.append(Bone("collar", "body", boxes=[Box((-4.5, -13.2, -2.6), (9, 2.6, 5.2), collar())]))
    # arms
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms()
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), sleeve_painter(), f"{side}_forearm", ELBOW[side])
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg_painter(), f"{side}_shin", KNEE)
    for side in ("right", "left"):
        B.append(Bone(f"{side}_foot", f"{side}_shin", boxes=[Box((-2.5, 3, -3.2), (5, 3, 5.2), foot())]))
    # the sash and its team end, the cravat
    B.append(Bone("belt", "body", boxes=[Box((-4, -4, -2), (8, 2.4, 4), sash(), dil=0.5)]))
    B.append(Bone("sash_end", "body", (3.4, -3, -2.7), rot=(0, 0, 4), team=True, boxes=[Box((-1.6, 0, 0), (3.2, 10, 0), "send", sheet="cloth")]))
    B.append(Bone("cravat", "body", team=True, boxes=[Box((-1.6, -12.6, -2.75), (3.2, 3.6, 0.5), "cravat", sheet="cloth")]))
    # four calf-length coat tails: front and back panels and a turned-back side on each half, flaring from the waist
    for side, c in (("l", 1), ("r", -1)):
        lo = -4.2 if c > 0 else -0.4
        sx = -0.7 if c > 0 else -0.5
        B.append(Bone(f"tails_{side}", "body", (c * 4.2, -3, 0), rot=(0, 0, c * 9), boxes=[
            Box((lo + (0.5 if c > 0 else 0), -1, -2.7), (3.7, 9, 1.3), tail_panel("front", c)),
            Box((lo, -1, 1.4), (4.6, 9, 1.3), tail_panel("back", c)),
            Box((sx, -1, -2.6), (1.2, 9, 5.2), tail_panel("side", c))]))
    # epaulettes with bullion fringe, barnacles; kelp trailing
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_epaulette", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 8 * c), boxes=[
            Box((-3.2 + 0.4 * c, -2.4, -2.8), (6.2, 1.8, 5.6), epaulette(side))]))
        B.append(Bone(f"{side}_fringe", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 8 * c), boxes=[
            Box((2.4 if c > 0 else -3.8, -0.8, -2.8), (1.4, 3.2, 5.6), fringe(side))]))
    B.append(Bone("barnacles", "body", boxes=[
        Box((-4.6, -11.2, -2.3), (1.6, 1.4, 1.6), HP.barnacle()), Box((4.0, -10.8, 0.2), (1.4, 1.4, 1.4), HP.barnacle()),
        Box((-3.6, -5.2, -2.7), (1.6, 1, 1), HP.barnacle()), Box((2.6, -7.6, 2.4), (1.6, 1.2, 1), HP.barnacle()),
        Box((-1, -9.2, 2.3), (1, 1, 1.2), HP.barnacle())]))
    B.append(strand("kelp_epaulette", "body", (-6.6, -11.4, 1.5), (14, 0, -10), [((-1, 0, 0), (2, 11, 1))]))
    B.append(strand("kelp_hem", "body", (3, 4, 2.9), (10, 0, 6), [((-1, 0, 0), (2, 8, 1))]))
    B.append(strand("kelp_sash", "body", (-2.5, -3, -2.8), (-8, 0, -4), [((-1, 0, 0), (2, 11, 1))]))
    B.append(strand("kelp_hat", "head", (-5, -9, -0.5), (6, 0, -16), [((-1, 0, 0), (2, 9, 1))]))
    B.append(strand("kelp_boot", "left_shin", (1, 0, -2.4), (-14, 0, 6), [((-1, 0, 0), (2, 6, 1))]))
    # the anchor on the back, stepped like a pixel icon: ring, stock, shank, a stepped crescent of arms with flukes, a rope loop
    B.append(Bone("anchor", "body", (0, 0, 2.1), boxes=[
        Box((-0.7, -10.2, 0), (1.4, 9.0, 1.3), anchor_iron("sh")),                     # shank
        Box((-3.2, -9.4, 0.1), (6.4, 1.4, 1.1), anchor_iron("sk")),                    # stock
        Box((-1.4, -12.6, 0), (2.8, 2.6, 1.3), anchor_iron("rg")),                     # ring
        Box((-0.7, -12.2, 0.2), (1.4, 1.2, 1.1), rope_loop("rl")),                     # rope through the ring
        Box((-3.4, -2.8, 0), (6.8, 1.6, 1.3), anchor_iron("br")),                      # the crown bar of the arms
        Box((-4.4, -4.4, 0.1), (1.6, 2.0, 1.2), anchor_iron("al")), Box((2.8, -4.4, 0.1), (1.6, 2.0, 1.2), anchor_iron("ar")),
        Box((-4.8, -6.2, 0.1), (2.2, 2.0, 1.2), anchor_iron("fl")), Box((2.6, -6.2, 0.1), (2.2, 2.0, 1.2), anchor_iron("fr"))]))
    # tier II, the Drowned Commodore: a half-cuirass, plate pauldrons under the epaulettes, vambraces, greaves, a hat band
    B.append(Bone("cuirass", "body", stages=[2, 3], boxes=[Box((-4.3, -10.8, -2.7), (8.6, 5.6, 1.2), TC.P("adcu", 0.02), dil=0.1)]))
    B.append(Bone("hat_band", "head", tag="helm", stages=[2, 3], boxes=[Box((-4.9, -8.6, -4.9), (9.8, 1.4, 9.8), TC.band("adhb"), dil=0.05)]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -1.8, 0), rot=(0, 0, 16 * c), stages=[2, 3], boxes=[
            Box((-3.6 + 0.5 * c, -0.4, -3.2), (7.2, 3, 6.4), TC.P("adpd" + side, 0.03), dil=0.15)]))
        B.append(Bone(f"{side}_vambrace", f"{side}_forearm", stages=[2, 3], boxes=[Box((-1.2 if side == "right" else -4.2, 0, -2.2), (4.4, 3.5, 4.4), TC.P("advb" + side, 0.02), dil=0.1)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[2, 3], boxes=[Box((-2.3, -1.5, -2.3), (4.6, 4.5, 4.6), TC.P("adgv" + side, 0.03), dil=0.1)]))
    # tier III, the Drowned Tide: a ring of pearls round the tricorne crown, a gorget pearl, a third plume
    for i, (x, z) in enumerate([(-3.6, -4.8), (-1.2, -5.0), (1.2, -5.0), (3.6, -4.8), (-4.8, -2), (4.8, -2), (-4.8, 1), (4.8, 1)]):
        B.append(Bone(f"hat_pearl{i}", "head", tag="helm", stages=[3], boxes=[Box((x - 0.5, -12, z - 0.5 + (0.6 if abs(x) > 4 else 0)), (1, 1, 1), TC.tc_pearl())]))
    B.append(Bone("gorget_pearl", "body", stages=[3], boxes=[Box((-1, -11.2, -3.4), (2, 2, 1), TC.tc_pearl())]))
    B.append(Bone("plume_c", "head", (5.4, -11, -1.4), rot=(-20, 0, -48), tag="helm", stages=[3], boxes=[Box((-0.6, -1, -1.2), (1, 6, 3), plume("c"))]))
    return B

NOTE = ("Admiral Vael, the Drowned Tide (unit identity, Tidecallers, Legendary): drowned naval admiral, biped, upright; the 12 px tricorne in sodden felt with "
        "gilt lace, a sea-blue plume and cockade (the signature), a bone-pale face with silver hair adrift, a deep sea-navy coat with sea-blue lapels, cuffs "
        "and collar, double rows of gilt buttons, a sea-blue sash with a gilt buckle, gilt fringed epaulettes, four calf-length coat tails with ragged hems, "
        "tarred boots, kelp and barnacles, a team sash end and cravat. A tarnished anchor is slung on the back (back gear). The admiral trident is in the "
        "right hand. No coral. Tier II a bronze half-cuirass, plate pauldrons, vambraces and greaves; tier III deep sea-blue plate, "
        "a ring of pearls round the tricorne and a third plume. No back flag.")
TC_ADMIRAL = Troop("tc_admiral", SKIN, CLOTH, bones, cloth_painter,
                   {"weapon": "admiral_trident", "hand": "right", "idleArms": "hang", "portrait": "tall 4"})
def build(): return TC_ADMIRAL.build()
def outputs(): return TC_ADMIRAL.outputs()

if __name__ == "__main__":
    house_stub.main(TC_ADMIRAL, NOTE, sys.argv)
