#!/usr/bin/env python3
"""The Barrow Stalker (Graveborn, troop id stalker), hand-authored at one texel per model pixel.

A stray in a solid blue-grey wedge: a shaggy rime-furred cowl over a frost-rimed skull (two soul-light pricks in the
sockets, the glow sheet), a thick fur shoulder-cape, and under it the mark, a knee-length mantle of frost-grey felt hung in
strips (three down the back, a flap either side of the open front), every hem cut to icicle teeth over a violet binding.
The body is built wide (9x12x4, the spec's [1.1,1,1.1]) on bare bone legs 2 px thick, shins bound in violet over fur boot
cuffs. A grave-iron belt, a pelt tail down the back, a team wrap round the chest with two knotted tails, a team wrap on the
draw arm, a grave-iron bracer bound in violet on the bow arm. He carries the frost bow (weapon "frost_bow", left hand).
Tier II (the Frost Stalker): salvaged grave-iron plate over the felt: pauldrons with rime tines, a breastplate, bracers and
greaves, piped in violet. Tier III (the Wight Stalker): black plate chased in funeral gilt, longer rime tines, a gilt cowl
band and a gilt breast clasp. No flag on the back, no antlers.

  python build_stalker.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
from graveborn import VIOLET as V, BONE as BN, GIRON as GI, FROST as F, SOUL, GILT, gb_plate, gb_spike, gb_gilt, gb_band
import house_stub

SKIN = (128, 80)       # 128 x 64 was full once the limbs were jointed (the joint caps)
CLOTH = (128, 64)
LEAN = 8        # degrees the torso leans forward; the head half way back, the arms stand back up
INK_EYE = hx("07090c")
FC = ramp("24323b", "2d4757", "466a7e", "7a8e96", "93A7AE", "B8C8CC", "DCE3E2")   # FROST with cold slate-blue shadows

def rnd(*key): return random.Random("stalker-%s" % (key,))

def tone(base, face, x=0, w=0):
    """Ramp index for a texel: face light plus the lit-edge column shift on the sides."""
    v = base + LIT[face]
    if w and face not in ("top", "bottom"): v += lightx(face, x, w)
    return v

def pits(g, R, key, n, step=-1):
    """Weathering: n texels pushed a step along the ramp R."""
    r = rnd(*key); h, w = len(g), len(g[0])
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        if g[y][x] is None: continue
        try: i = R.index(g[y][x])
        except ValueError: continue
        g[y][x] = R[max(0, min(len(R) - 1, i + step))]
    return g

def bind_row(g, y, faces_ok=True, phase=0):
    """A violet binding across row y of the grid."""
    if 0 <= y < len(g):
        for x in range(len(g[y])): g[y][x] = V[1 + ((x + y + phase) % 3 == 0) + ((x + phase) % 2 == 0)]

# ---- fur: strands with pale tips and a scatter of rime ----
def fur(base, seed, rime=0.05, tiers=4):
    def paint(face, w, h):
        r = rnd("fur", seed, face, w, h)
        off = [r.randrange(tiers)]
        for _ in range(w - 1): off.append((off[-1] + r.choice((0, 1, 1, 2, tiers - 1))) % tiers)
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = tone(base, face, x, w)
                if face in ("top", "bottom"): v += (1 if (x * 3 + y * 5) % 4 == 0 else -(1 if (x + y) % 3 == 0 else 0))
                else:
                    p = (y + off[x]) % tiers
                    v += 1 if p == 0 else (0 if p == 1 else -1)
                if r.random() < rime: v += 2
                g[y][x] = clamp(FC, v)
        return g
    return paint

def fringe(g, face, seed, depth=2, tip=5):
    """Ragged fur bottom edge: each column loses 0..depth rows, the last kept texel pale."""
    if face in ("top", "bottom"): return g
    r = rnd("fringe", seed, face); h = len(g)
    for x in range(len(g[0])):
        d = r.randrange(0, depth + 1)
        for y in range(h - d, h): g[y][x] = None
        if h - d - 1 >= 0 and g[h - d - 1][x] is not None: g[h - d - 1][x] = F[tip] if r.random() < 0.6 else g[h - d - 1][x]
    return g

# ---- the skull ----
SKULL = """
RrRRRRrR
rBbBBbBr
BkkBBkkB
BkEBBEkB
bBBkkBBb
BbBBBBbB
BtktktkB
.TtTtTt.
"""
def skull(face, w, h):
    p = {"R": F[6], "r": F[5], "B": BN[4], "b": BN[3], "t": BN[5], "T": BN[3], "k": INK_EYE, "E": SOUL[1]}
    if face == "front":
        g = grid(SKULL, p)
        for y in (2, 3, 4, 5):
            if g[y][7] == BN[4]: g[y][7] = F[5]                      # rime creeping down the right cheek
        return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            i = 3 + (1 if y > 3 else 0) - (1 if face == "left" else 0)
            g[y][x] = BN[i]
            if y < 2 or face == "top": g[y][x] = F[5 - ((x + y) % 3 == 0)]            # frozen crown
            if face in ("back", "bottom"): g[y][x] = BN[1 + ((x + y) % 3 == 0)]
    if face in ("left", "right"): g[4][w // 2 - 1] = g[4][w // 2] = BN[0]            # the ear hole
    return g

def skull_lit(face, w, h):
    g = blank(w, h)
    if face == "front": g[4][2] = SOUL[3]; g[4][5] = SOUL[3]; g[4][1] = g[4][6] = None
    return g

# ---- the fur cowl: shaggy, a deep opening, a ragged lip ----
def cowl(face, w, h):
    g = fur(4, "cowl")(face, w, h)
    if face == "front":
        for y in range(2, h):
            for x in range(2, w - 2): g[y][x] = None                       # the opening: 6 wide, down to the chin
        for x in range(w): g[1][x] = F[2] if x in (0, w - 1) else g[1][x]
        for y in range(2, h): g[y][1] = F[5] if y % 2 else F[4]; g[y][w - 2] = F[3]
        for x in range(2, w - 2): g[2][x] = F[1]                           # the brow's shadow, deep in the fur
        bind_row(g, h - 1)                                                  # a violet cord to bind the cowl under the chin
        for x in range(2, w - 2): g[h - 1][x] = None
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face in ("left", "right", "back"):
        g[h - 2] = [V[1 + (x % 2)] for x in range(w)]                       # the violet cord round the cowl's base
        g = fringe(g, face, "cowl", 1)
    return g

def crest(face, w, h): return fur(5, "crest", rime=0.14)(face, w, h)

def lappet(face, w, h):
    g = fur(3, "lap")(face, w, h)
    return fringe(g, face, "lap", 2)

def pelt(face, w, h):
    g = fur(3, "pelt", rime=0.1)(face, w, h)
    for y in range(h):
        for x in range(w):
            if face in ("front", "back") and y >= h - 3 and (x + y) % 2: g[y][x] = None
    if face in ("front", "back"):
        g[h - 3][0] = g[h - 3][w - 1] = V[2]
        g[h - 4] = [V[1 + (x % 2)] for x in range(w)]
    return g

# ---- body, belt, limbs ----
def body(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = clamp(F, 2 + (1 if face == "top" else -2)); continue
            v = tone(2, face, x, w)
            if (x % 3 == 2) and y < h - 3: v -= 1                         # quilting seams
            g[y][x] = clamp(F, v)
    if face == "front":
        for y in range(1, 8):
            g[y][w // 2] = BN[4 if y % 3 == 1 else 2]                      # bone toggles down the placket
            g[y][w // 2 - 1] = F[1]
    if face in ("front", "back"):
        for x in range(w): g[0][x] = F[4]
    return pits(g, F, ("body", face), 10, 1)

def belt(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(GI, 3 + LIT[face] + (1 if y == 0 else 0)) if face not in ("top", "bottom") else GI[2]
    if face == "front":
        for y in range(h):
            for x in (w // 2 - 1, w // 2, w // 2 + 1): g[y][x] = F[5] if y == 0 else F[3]    # a rimed buckle plate
        g[0][w // 2] = F[6]
    if face in ("left", "right"): g[h - 1] = [V[2]] * w
    return g

def bone(base, face, x, w, y, h, joint=None, ball=None):
    """Bare bone texel: dark seam at the `joint` row, light ball under it."""
    if face in ("top", "bottom"): return clamp(BN, base + (1 if face == "top" else -2))
    i = tone(base, face, x, w)
    if y == joint: i = base - 2
    elif y == ball: i = base + 1
    if y == h - 1: i -= 1
    return clamp(BN, i)

def arm_bow(face, w, h):
    """The bow arm: sleeve of felt, bone forearm in a grave-iron bracer bound in violet at both ends, bare finger bones."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = F[3] if face == "top" else F[0]; continue
            if y < 3: g[y][x] = clamp(F, tone(3, face, x, w) + (1 if y == 0 else 0) - (1 if (x + y) % 3 == 0 else 0))
            elif 7 <= y <= 10: g[y][x] = clamp(GI, tone(4, face, x, w) + (1 if y == 8 else 0))
            else: g[y][x] = bone(3, face, x, w, y, h, joint=5, ball=6)
    if face not in ("top", "bottom"):
        for y in (7, 10): bind_row(g, y)
    return pits(g, BN, ("armL", face), 3)

def arm_draw(face, w, h):
    """The draw arm: sleeve, bare bone, a team wrap goes over it; fingers in a grave-iron thumb ring."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom"): g[y][x] = F[3] if face == "top" else F[0]; continue
            if y < 3: g[y][x] = clamp(F, tone(3, face, x, w) + (1 if y == 0 else 0) - (1 if (x + y) % 3 == 0 else 0))
            else: g[y][x] = bone(3, face, x, w, y, h, joint=5, ball=6)
    if face not in ("top", "bottom"):
        for y in (9, 10): bind_row(g, y, phase=1)
        for x in range(w): g[11][x] = GI[3]
    return pits(g, BN, ("armR", face), 3)

def leg(side):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = clamp(BN, 3 + (1 if face == "top" else -2)); continue
                i = tone(3, face, x, w)
                if y == 6: i = 1                                            # the knee's seam
                elif y == 7: i = 4                                          # and its ball
                elif y < 6: i = 2 + LIT[face]
                if y == h - 1: i -= 1
                g[y][x] = clamp(BN, i)
        if face not in ("top", "bottom"):
            for y in ((8, 11) if side == "right" else (7, 8, 11)): bind_row(g, y, phase=1 if side == "left" else 0)
        return pits(g, BN, ("leg" + side, face), 3)
    return paint

def cuff(face, w, h):
    g = fur(3, "cuff", rime=0.12, tiers=3)(face, w, h)
    if face not in ("top", "bottom"): bind_row(g, 0)
    return g

def boot(face, w, h):
    g = [[clamp(F, 2 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)) for x in range(w)] for y in range(h)]
    if face == "front":
        for x in range(w): g[h - 1][x] = GI[3]                              # a grave-iron toecap
    if face not in ("top", "bottom"):
        for x in range(w): g[h - 1][x] = GI[2]
    return g

# ---- the shoulder cape: fur over felt ----
def cape(face, w, h):
    g = blank(w, h)
    r = rnd("cape", face, w, h)
    furp = fur(4, "cape", rime=0.12)(face, w, h)
    for y in range(h):
        for x in range(w):
            if face in ("top", "bottom") or y < 3: g[y][x] = furp[y][x]
            else:
                v = tone(3, face, x, w) - (1 if x % 3 == 2 else 0) + (1 if r.random() < 0.07 else 0)
                g[y][x] = clamp(FC, v)
    if face not in ("top", "bottom"):
        bind_row(g, h - 1)                                                   # violet piping along the felt's edge
    if face == "top":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None                        # open at the neck for the cowl
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    return g

# ---- the knee-length mantle: felt strips with icicle hems ----
def strip(seed, outside, depth=3, base=3):
    """A strip of rimed felt: soft folds, frost drifting at the top, a violet binding, icicle teeth below it. `outside` is the
    face seen from outside (the other broad face is the lining, dark)."""
    def paint(face, w, h):
        g = blank(w, h); r = rnd("strip", seed, face)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = FC[2] if face == "bottom" else FC[4]; continue
                v = tone(base, face, x, w) + (1 if y < 3 else 0) - (1 if (x + y // 6) % 3 == 2 else 0) - (1 if y > h // 2 and x % 2 else 0)
                if y > h - 6: v += (1 if (x + y) % 3 == 0 else 0)          # frost gathering toward the hem
                if face != outside and face in ("front", "back"): v = 1 + (1 if (x + y) % 5 == 0 else 0)
                roll = r.random()
                if roll < 0.05: v += 1
                elif roll < 0.10: v -= 1
                g[y][x] = clamp(FC, v)
        if face in ("front", "back"):
            by = h - depth - 1
            for x in range(w): g[by][x] = V[1 + (x + by) % 3 // 2 + (1 if face == outside else 0)] if face == outside else V[1]
            if face == outside:
                for x in range(w): g[by - 1][x] = FC[5] if (x + by) % 2 else FC[4]
            for x in range(w):
                d = r.randrange(0, depth + 1)
                for y in range(h - d, h): g[y][x] = None
                if d < depth and g[h - d - 1][x] is not None and h - d - 1 > by: g[h - d - 1][x] = FC[6] if r.random() < 0.5 else FC[5]
        elif face in ("left", "right"):
            by = h - depth - 1
            for y in range(by, h): g[y][0] = V[1 + (y % 2)] if y == by else (F[4] if y % 2 else None)
        return g
    return paint

# ---- the team wrap: a spiral bandage round the chest, two knotted tails, a wrap on the draw arm ----
def wrap_body(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = T[2]; continue
                k = (x + 2 * y) % 5
                i = 3 + LIT[face] + (1 if k == 0 else (-1 if k in (3, 4) else 0))
                g[y][x] = clamp(T, i)
        if face == "front":
            for y in range(h):
                if (3 * y) % 5 == 1: g[y][min(w - 1, 2 * y % w)] = M[1]       # a gilt thread worked in
        return g
    return paint

def wrap_tail(T, M, cut):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        r = rnd("tail", face, cut)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + (1 if face == "front" else 0) - (1 if x == w - 1 else 0) - (1 if y % 4 == 3 else 0))
        for x in range(w):
            g[h - 2][x] = M[1]
            g[h - 1][x] = T[1] if r.random() < 0.5 else None
        return g
    return paint

def wrap_arm(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = T[2]; continue
                g[y][x] = clamp(T, 3 + LIT[face] + (1 if (x + y) % 2 == 0 else -1))
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    if spec == "wrap": return wrap_body(T, M)
    if spec == "armwrap": return wrap_arm(T, M)
    return wrap_tail(T, M, spec[1])

# ---- assembling ----
def glow(bone_, box):
    return skull_lit if bone_.name == "head" else None

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4.5, -12, -2), (9, 12, 4), body)], rot=(LEAN, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -3), (8, 8, 8), skull)], rot=(-LEAN // 2, 0, 0)))
    # the fur cowl: opening 6 wide with the skull set two px back, a crest of fur along the crown, a lappet either side
    B.append(Bone("fur_cowl", "head", tag="helm", boxes=[
        Box((-5, -9, -5), (10, 10, 10), cowl, dil=0.3),
        Box((-1, -11, -3), (2, 2, 8), crest, dil=0.2),
        Box((-6.5, -4, -3), (1, 7, 5), lappet, dil=0.2),
        Box((5.5, -4, -3), (1, 7, 5), lappet, dil=0.2)]))
    for side, x, paint in (("right", -5.5, arm_draw), ("left", 5.5, arm_bow)):   # jointed: the elbow at mid-arm, the hand 4 px under it
        B += limb(f"{side}_arm", "body", (x, -11, 0), (-1, -2, -1), paint, f"{side}_forearm", (0, 4, 0), size=(2, 12, 2), rot=(-LEAN, 0, 0))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", (0, 4, 0)))
    B.append(Bone("arms_folded", "body", (0, -10, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -2.5), ("left", 2.5)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-1, 0, -1), leg(side), f"{side}_shin", KNEE, size=(2, 12, 2))
    B.append(Bone("right_foot", "right_shin", boxes=[Box((-1.5, 4, -3), (3, 2, 4), boot), Box((-2, 2.5, -2), (4, 2, 4), cuff, dil=0.25)]))
    B.append(Bone("left_foot", "left_shin", boxes=[Box((-1.5, 4, -3), (3, 2, 4), boot), Box((-2, 2.5, -2), (4, 2, 4), cuff, dil=0.25)]))
    # the knee-length mantle: a fur-edged shoulder cape, three felt strips down the back, a flap either side of the front
    B.append(Bone("knee_mantle", "body", boxes=[Box((-6.5, -13, -3.5), (13, 5, 7), cape, dil=0.3)]))
    B.append(Bone("mantle_back", "body", (0, -9, 2.4), boxes=[Box((-3, 0, 0), (6, 15, 1), strip("bc", "back"))]))
    B.append(Bone("mantle_back_l", "body", (3, -9, 2.3), rot=(3, 0, -11), boxes=[Box((0, 0, 0), (4, 15, 1), strip("bl", "back"))]))
    B.append(Bone("mantle_back_r", "body", (-3, -9, 2.3), rot=(3, 0, 11), boxes=[Box((-4, 0, 0), (4, 15, 1), strip("br", "back"))]))
    B.append(Bone("mantle_front_l", "body", (3, -9, -2.4), rot=(-3, 0, -5), boxes=[Box((0, 0, -1), (4, 15, 1), strip("fl", "front"))]))
    B.append(Bone("mantle_front_r", "body", (-3, -9, -2.4), rot=(-3, 0, 5), boxes=[Box((-4, 0, -1), (4, 15, 1), strip("fr", "front"))]))
    B.append(Bone("belt", "body", boxes=[Box((-4.5, -4, -2), (9, 2, 4), belt, dil=0.45)]))
    B.append(Bone("pelt_tail", "body", (0, -12, 2.6), rot=(5, 0, 0), boxes=[Box((-1.5, 1, 1), (3, 11, 1), pelt)]))
    # the team wrap: a spiral bandage round the chest, two knotted tails, and a wrap on the draw arm
    B.append(Bone("wrap", "body", team=True, boxes=[Box((-4.5, -9, -2), (9, 5, 4), "wrap", dil=0.35, sheet="cloth")]))
    B.append(Bone("wrap_tails", "body", team=True, boxes=[
        Box((1.5, -4, -2.7), (2, 9, 0), ("tail", 1), sheet="cloth"),
        Box((-0.5, -4, -2.6), (2, 6, 0), ("tail", 2), sheet="cloth")]))
    B.append(Bone("arm_wrap", "right_arm", team=True, boxes=[Box((-1, 2.5, -1), (2, 3, 2), "armwrap", dil=0.3, sheet="cloth")]))
    # tier II, the Frost Stalker: salvaged grave-iron plate piped in violet, rime tines on the shoulders
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    TN = per_tier(lambda t: gb_spike(F, 4, 3), (2, 3))
    B.append(Bone("breastplate", "body", stages=[2, 3], boxes=[Box((-3.5, -12, -2.7), (7, 4, 5.4), P2("bpl"), dil=0.75)]))
    for side, sx, rz in (("right", -1, 12), ("left", 1, -12)):
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (sx * 1.2, -0.3, 0), rot=(0, 0, rz), stages=[2, 3], boxes=[
            Box((-2.7, -3.4, -3.2), (5.4, 3, 6.4), P2("pd" + side), dil=0.9),
            Box((-0.5 + sx * 1.5, -7.6, -0.5), (1, 4.4, 1), TN), Box((-0.5 + sx * 0.2, -6.4, 1.6), (1, 3.4, 1), TN)]))
        B.append(Bone(f"{side}_bracer", f"{side}_forearm", stages=[2, 3], boxes=[Box((-1.8, 0.5, -1.8), (3.6, 3.5, 3.6), P2("br" + side, 0.18), dil=0.5)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[2, 3], boxes=[Box((-1.7, -1.8, -1.7), (3.4, 3, 3.4), P2("gv" + side), dil=0.5)]))
    B.append(Bone("belt_plate", "body", stages=[2, 3], boxes=[Box((-2, -4.4, -2.7), (4, 2.6, 0.8), P2("bt"), dil=0.5)]))
    # tier III, the Wight Stalker: a gilt band round the cowl, a gilt breast clasp with one pale stone, taller rime tines
    B.append(Bone("cowl_band", "head", stages=[3], boxes=[Box((-5.2, -9.4, -5.2), (10.4, 1.3, 10.4), gb_gilt("cwb"), dil=0.05)]))
    B.append(Bone("breast_clasp", "body", stages=[3], boxes=[Box((-1.5, -9.5, -3.2), (3, 3, 0.8), gb_gilt("bcl"), dil=0.05),
                                                           Box((-0.5, -8.6, -3.4), (1, 1, 0.4), lambda f, w, h: [[SOUL[2]]])]))
    for i, (x, z, h) in enumerate([(-3.4, -3.0, 6), (3.4, -3.0, 6), (0, 0.4, 7)]):
        B.append(Bone(f"crest_tine{i}", "head", stages=[3], boxes=[Box((x - 0.5, -11.2 - h, z - 0.5), (1, h, 1), gb_spike(F, 4, 3))]))
    return B

NOTE = ("Graveborn Barrow Stalker (unit identity, Graveborn): a solid blue-grey wedge, a shaggy fur cowl over a rimed skull "
        "with two soul-light eyes, a fur shoulder-cape over a knee-length frost felt mantle hemmed in violet icicle teeth, "
        "bare bone legs, a team wrap, the frost bow. Tier II iron plate with rime tines, tier III black gilt plate; no flag on the back, no antlers.")

STALKER = Troop("stalker", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "frost_bow", "hand": "left", "idleArms": "hang", "portrait": "standard"}, glow=glow)
def build(): return STALKER.build()
def outputs(): return STALKER.outputs()

if __name__ == "__main__":
    house_stub.main(STALKER, NOTE, sys.argv)
