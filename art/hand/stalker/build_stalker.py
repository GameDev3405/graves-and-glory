#!/usr/bin/env python3
"""The Barrow Stalker (Graveborn, troop id stalker), hand-authored at one texel per model pixel.

Rebuilt from the ground up 2026-11-13 (Kort: "just make a skeleton but with some features like ribcage showing, skull showing,
etc and then maybe put a dark cloak on it with a hood"; round 3: "Remove cape/cloak, keep hood"). A bare skeleton: a 7 px skull
with two soul-light pricks in the sockets (the glow sheet) on a bone neck, an open ribcage of separate ribs round a spine and a
sternum, the lumbar spine bare above a pelvis, bone arms and legs. A close dark hood over the skull, hemmed in violet, its back
falling over the nape. The team sash knots round the pelvis with two tails in front; a team wrap on the draw arm; a grave-iron
bracer on the bow arm. He carries the frost bow (weapon "frost_bow", left hand). Tier II (the Frost Stalker): salvaged
grave-iron, low pauldrons, bracers, greaves, a gorget and a fauld on the sash. Tier III (the Wight Stalker): black plate chased
in funeral gilt, a gilt band on the hood, a gilt medallion with a soul stone on the sternum. No cloak, no flag, no antlers.

Clearance (measured on the present clips, work/stalker_model/freespace.py): the drawing arm fills the 2 to 3 px above the neck
pivot at full draw, so the skull sits 3 px up on its neck and the hood is 8 px across and ends 4.6 px above the neck pivot.
The bones the old cloth hung on (knee_mantle, mantle_back(_l/_r), mantle_front_l/r, pelt_tail) are kept empty so every clip
loads. Bone names, parents, pivots and rest turns of every keyed bone and the arm, forearm, hand, leg and shin lengths are the
old model's.

  python build_stalker.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys, random
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
from handlib import Troop
from graveborn import VIOLET as V, BONE as BN, GIRON as GI, FROST as F, SOUL, GILT, gb_plate, gb_spike, gb_gilt
import house_stub

SKIN = (128, 80)
CLOTH = (128, 64)
LEAN = 8        # degrees the torso leans forward; the head half way back, the arms stand back up
INK = hx("07090c")
CAV = hx("0d0b10")                                                            # the dark inside the ribcage and the sockets
DK = ramp("0b0d12", "12161d", "1a1f28", "242a35", "303744", "3e4655", "525b6b")  # the hood: cold charcoal with a blue cast

def rnd(*key): return random.Random("stalker-%s" % (key,))

def tone(base, face, x=0, w=0):
    v = base + LIT[face]
    if w and face not in ("top", "bottom"): v += lightx(face, x, w)
    return v

def bind_row(g, y, phase=0):
    if 0 <= y < len(g):
        for x in range(len(g[y])): g[y][x] = V[1 + ((x + y + phase) % 3 == 0) + ((x + phase) % 2 == 0)]

def pits(g, R, key, n, step=-1):
    r = rnd(*key); h, w = len(g), len(g[0])
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        if g[y][x] is None: continue
        try: i = R.index(g[y][x])
        except ValueError: continue
        g[y][x] = R[max(0, min(len(R) - 1, i + step))]
    return g

def nothing(face, w, h): return blank(w, h)

def ring(paint):
    """A band round the hood: its sides only, so the top of the hood stays cloth."""
    return lambda face, w, h: blank(w, h) if face in ("top", "bottom") else paint(face, w, h)

# ---- the skull ----
SKULL = """
bBBBBBb
BBBBBBB
BkkBkkB
kkEBEkk
BBBkBBB
BtktktB
.TtTtT.
"""
def skull(face, w, h):
    p = {"B": BN[4], "b": BN[3], "t": BN[5], "T": BN[3], "k": INK, "E": CAV}
    if face == "front":
        g = grid(SKULL, p)
        g[1][6] = g[2][6] = BN[3]                                          # the lit cheek turns into shade
        return g
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(BN, tone(3, face, x, w) + (1 if y < 3 else 0))
            if face == "bottom": g[y][x] = BN[1 + ((x + y) % 3 == 0)]
            if face == "top": g[y][x] = BN[4 + ((x * 3 + y) % 5 == 0)]
    if face in ("left", "right"):
        g[3][w // 2] = BN[0]                                               # the ear hole
        g[5][1 if face == "right" else w - 2] = BN[1]                      # the jaw's hinge
        for x in range(w): g[h - 1][x] = BN[2] if x < w - 2 else None      # the jaw, narrower than the crown
    if face == "back":
        g[h - 1] = [BN[1]] * w
    return pits(g, BN, ("skull", face), 3)

def skull_lit(face, w, h):
    g = blank(w, h)
    if face == "front": g[3][2] = SOUL[3]; g[3][4] = SOUL[3]
    return g

def neck(face, w, h):
    """Cervical vertebrae: bone with a dark gap every other row."""
    return [[(BN[3 + (1 if face == "front" else 0) - (1 if x == w - 1 else 0)] if y % 2 == 0 else BN[1]) for x in range(w)] for y in range(h)]

# ---- the ribcage ----
def spine(face, w, h):
    g = [[BN[3 + (1 if face in ("front", "top") else 0) - (1 if face in ("left", "back") else 0)] if y % 2 == 0 else BN[1]
          for x in range(w)] for y in range(h)]
    if face == "back":
        for y in range(0, h, 2): g[y][w // 2] = BN[5]                      # the spinous knobs
    return g

def rib(face, w, h):
    """A rib hoop seen as a slab: bone round its edge, the open dark of the cage in its top and bottom."""
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(BN, tone(3, face, x, w))
    if face in ("top", "bottom"):
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = CAV
    if face == "front":
        g[0][w // 2] = BN[2]                                               # where the rib meets the sternum
    return g

def clavicle(face, w, h):
    g = [[clamp(BN, tone(4, face, x, w)) for x in range(w)] for y in range(h)]
    if face == "front":
        for y in range(h): g[y][w // 2] = BN[2]
    if face in ("top", "bottom"):
        for y in range(1, h - 1):
            for x in range(2, w - 2): g[y][x] = CAV
    return g

def sternum(face, w, h):
    g = [[clamp(BN, tone(4, face, x, w) - (1 if y % 2 else 0)) for x in range(w)] for y in range(h)]
    return g

def pelvis(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = clamp(BN, tone(3, face, x, w) + (1 if y == 0 else 0))
    if face == "front":
        for x in range(2, w - 2): g[h - 1][x] = None                        # the arch under the pubis
        g[1][1] = g[1][w - 2] = BN[1]                                       # the hip sockets' shadow
    if face == "back":
        for y in range(h): g[y][w // 2] = BN[5] if y % 2 == 0 else BN[2]    # the sacrum
    if face in ("top", "bottom"):
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = CAV
    return g

# ---- limbs ----
def bone_px(base, face, x, w, y, joint, ball):
    if face in ("top", "bottom"): return clamp(BN, base + (1 if face == "top" else -2))
    i = tone(base, face, x, w)
    if y == joint: i = base - 2
    elif y == ball: i = base + 1
    return clamp(BN, i)

def arm(bow):
    """A bone arm: a shoulder ball, the humerus, the elbow's seam and ball, the two forearm bones (a dark line between), the
    hand's knuckle row. The bow arm wears a grave-iron bracer bound in violet; the draw arm a grave-iron thumb ring."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = bone_px(3, face, x, w, y, joint=5, ball=6)
                if y == 0 and face not in ("top", "bottom"): g[y][x] = BN[4]
                if 7 <= y <= 9 and face in ("front", "back") and x == w - 1: g[y][x] = BN[1]   # radius and ulna
                if y == h - 1 and face not in ("top", "bottom"): g[y][x] = BN[4] if x % 2 == 0 else BN[2]
        if face not in ("top", "bottom"):
            if bow:
                for y in (7, 8, 9): g[y] = [clamp(GI, tone(4, face, x, w) + (1 if y == 8 else 0)) for x in range(w)]
                bind_row(g, 7); bind_row(g, 9)
            else:
                g[h - 2] = [GI[3]] * w
        return pits(g, BN, ("arm", bow, face), 2)
    return paint

def leg(side):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = bone_px(3, face, x, w, y, joint=6, ball=7)
                if y == 0 and face not in ("top", "bottom"): g[y][x] = BN[4]
                if 8 <= y <= 10 and face in ("front", "back") and x == 0: g[y][x] = BN[1]      # tibia and fibula
        if face not in ("top", "bottom") and side == "left": bind_row(g, 9, 1)                # one violet cord
        return pits(g, BN, ("leg", side, face), 2)
    return paint

def foot(face, w, h):
    """Bone foot: the toes as a row of knuckles at the front, the heel at the back."""
    g = [[clamp(BN, tone(3, face, x, w)) for x in range(w)] for y in range(h)]
    if face == "front":
        for x in range(w): g[h - 1][x] = BN[4] if x % 2 == 0 else BN[1]
    if face == "top":
        for y in range(h):
            for x in range(w): g[y][x] = BN[4] if (x % 2 == 0 or y > h - 2) else BN[2]
    if face == "bottom": g = [[BN[1]] * w for _ in range(h)]
    return g

def ankle_wrap(face, w, h):
    """A violet grave-cord bound round the ankle bone."""
    if face in ("top", "bottom"): return [[BN[1]] * w for _ in range(h)]
    g = [[None] * w for _ in range(h)]
    for y in range(h): bind_row(g, y, y)
    return g

# ---- the dark hood ----
def cloth(base, face, x, w, y, r):
    v = tone(base, face, x, w) - (1 if (x + y // 4) % 3 == 2 else 0)
    roll = r.random()
    if roll < 0.05: v += 1
    elif roll < 0.10: v -= 1
    return clamp(DK, v)

def hood(face, w, h):
    """The close hood: open at the face with a lit rim, open underneath, a violet hem round the back and sides."""
    r = rnd("hood", face); g = blank(w, h)
    for y in range(h):
        for x in range(w):
            g[y][x] = cloth(3, face, x, w, y, r)
    if face == "front":
        for y in range(1, h):
            for x in range(1, w - 1): g[y][x] = None
        for x in range(w): g[0][x] = DK[5] if x % 2 else DK[4]            # the brow's fold catches the light
        for y in range(1, h): g[y][0] = DK[4]; g[y][w - 1] = DK[3]
    if face == "bottom":
        for y in range(1, h - 1):
            for x in range(1, w - 1): g[y][x] = None
    if face in ("left", "right", "back"):
        g[h - 1] = [V[1 + (x % 2)] for x in range(w)]
    if face in ("left", "right"):
        for y in range(h): g[y][0 if face == "right" else w - 1] = DK[4]  # the rim seen from the side
    return g

def hood_inside(face, w, h):
    """The hood's back wall, behind the skull: dark, so the sky never shows through the opening."""
    return [[DK[0] if (x + y) % 5 else DK[1] for x in range(w)] for y in range(h)]

def hood_drape(face, w, h):
    """The hood's back falling over the nape onto the shoulders: open in front (the neck shows), hemmed in violet."""
    r = rnd("drape", face)
    if face in ("front", "bottom"): return blank(w, h)
    g = [[cloth(2, face, x, w, y, r) for x in range(w)] for y in range(h)]
    if face in ("left", "right", "back"): g[h - 1] = [V[1 + (x % 2)] for x in range(len(g[0]))]
    return g

def hood_point(face, w, h):
    r = rnd("hpt", face)
    return [[cloth(3, face, x, w, y, r) for x in range(w)] for y in range(h)]

# ---- the team sash, its tails and the arm wrap ----
def sash(T, M):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face in ("top", "bottom"): g[y][x] = T[2]; continue
                k = (x + 2 * y) % 5
                g[y][x] = clamp(T, 3 + LIT[face] + (1 if k == 0 else (-1 if k in (3, 4) else 0)))
        if face == "front":
            g[0][w // 2 + 1] = M[1]; g[h - 1][w // 2 + 2] = M[1]           # the knot's gilt thread
        return g
    return paint

def sash_tail(T, M, cut):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        r = rnd("stail", face, cut)
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
    if spec == "wrap": return sash(T, M)
    if spec == "armwrap": return wrap_arm(T, M)
    return sash_tail(T, M, spec[1])

# ---- assembling ----
def glow(bone_, box):
    return skull_lit if bone_.name == "head" and box.paint.get(0) is skull else None

def bones():
    B = [Bone("root", "")]
    # The body's own box is the rig's torso and hips (troop_rig re-cuts it as the knight's); here it paints nothing, so the
    # skeleton's parts below are what shows. Its size stays the old 9x12x4 so the rig and the arm sockets are unchanged.
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4.5, -12, -2), (9, 12, 4), nothing)], rot=(LEAN, 0, 0)))
    # the skull 3 px up on a bone neck (the drawing arm passes through the 2 to 3 px above the neck pivot at full draw)
    B.append(Bone("head", "body", (0, -12, 0), [Box((-3.5, -10, -3), (7, 7, 7), skull),
                                                Box((-1, -3.2, -0.2), (2, 3.2, 2), neck)], rot=(-LEAN // 2, 0, 0)))
    # the hood (keyed as fur_cowl): 8 across, 0.5 px off the skull on every side, open at the face and underneath; its point
    # behind the crown. A dark wall behind the skull's crown inside the opening.
    B.append(Bone("fur_cowl", "head", tag="helm", boxes=[
        Box((-4, -10.6, -3.6), (8, 6, 8), hood),
        Box((-3, -10.2, 4.2), (6, 5, 0), hood_inside),
        Box((-1.5, -11.6, 1.5), (3, 2, 3), hood_point),
        Box((-3, -4.2, 1.4), (6, 2, 3), hood_drape)]))
    # the ribcage: a clavicle bar, three ribs narrowing down, the sternum, the spine from the neck to the pelvis
    rib_boxes = [Box((-4, -12.4, -1.6), (8, 1, 3.4), clavicle)]
    for i, (hw, y, d) in enumerate([(3.5, -10.8, 3.6), (3.25, -9.2, 3.4), (3.0, -7.6, 3.2)]):
        rib_boxes.append(Box((-hw, y, -d / 2 + 0.2), (2 * hw, 1, d), rib))
    rib_boxes += [Box((-0.5, -11.4, -2.1), (1, 5, 0.8), sternum),
                  Box((-1, -12.2, 0.6), (2, 10, 1.4), spine)]
    B.append(Bone("ribcage", "body", boxes=rib_boxes))
    B.append(Bone("pelvis", "body", boxes=[Box((-3.5, -3.2, -1.4), (7, 3, 3.2), pelvis)]))
    for side, x, paint in (("right", -5.5, arm(False)), ("left", 5.5, arm(True))):   # jointed: the elbow at mid-arm
        B += limb(f"{side}_arm", "body", (x, -11, 0), (-1, -2, -1), paint, f"{side}_forearm", (0, 4, 0), size=(2, 12, 2), rot=(-LEAN, 0, 0))
        B.append(Bone(f"{side}_hand", f"{side}_forearm", (0, 4, 0)))
    B.append(Bone("arms_folded", "body", (0, -10, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -2.5), ("left", 2.5)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-1, 0, -1), leg(side), f"{side}_shin", KNEE, size=(2, 12, 2))
    for side in ("right", "left"):
        # the ankle wrap keeps the foot gear on the shin (troop_rig moves shin gear that starts at y 3 or lower onto the
        # rig's foot; the clips key *_foot_gear relative to the shin, as on the old model with its fur cuff)
        B.append(Bone(f"{side}_foot", f"{side}_shin", boxes=[Box((-1.5, 4, -3), (3, 2, 4), foot),
                                                            Box((-1.5, 1.5, -1.5), (3, 2, 3), ankle_wrap)]))
    # No cloak (Kort, round 3: "Remove cape/cloak, keep hood"). The bones the clips key for the old cloth stay, empty, so
    # every clip still loads: knee_mantle, mantle_back(_l/_r), mantle_front_l/r, pelt_tail.
    B.append(Bone("knee_mantle", "body"))
    B.append(Bone("mantle_back", "body", (0, -9, 2.4)))
    B.append(Bone("mantle_back_l", "body", (3, -9, 2.3), rot=(3, 0, -11)))
    B.append(Bone("mantle_back_r", "body", (-3, -9, 2.3), rot=(3, 0, 11)))
    B.append(Bone("mantle_front_l", "body", (3, -9, -2.4), rot=(-3, 0, -5)))
    B.append(Bone("mantle_front_r", "body", (-3, -9, -2.4), rot=(-3, 0, 5)))
    B.append(Bone("pelt_tail", "body", (0, -12, 2.6), rot=(5, 0, 0)))
    # the team sash knotted round the pelvis, two tails in front of it, and a wrap on the draw arm
    B.append(Bone("wrap", "body", team=True, boxes=[Box((-3.8, -3.4, -1.7), (7.6, 1.4, 3.6), "wrap", dil=0.1, sheet="cloth")]))
    B.append(Bone("wrap_tails", "body", team=True, boxes=[
        Box((0, -1.9, -2.1), (2, 8, 0), ("tail", 1), sheet="cloth"),
        Box((-2, -1.9, -2.0), (2, 5, 0), ("tail", 2), sheet="cloth")]))
    B.append(Bone("arm_wrap", "right_arm", team=True, boxes=[Box((-1, 2.5, -1), (2, 3, 2), "armwrap", dil=0.3, sheet="cloth")]))
    # tier II, the Frost Stalker: salvaged grave-iron, piped in violet
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    TN = per_tier(lambda t: gb_spike(F, 4, 3), (2, 3))
    B.append(Bone("breastplate", "body", stages=[2, 3], boxes=[Box((-2.5, -12.9, -2.4), (5, 2, 1), P2("gorget"), dil=0.2)]))
    for side, sx, rz in (("right", -1, 6), ("left", 1, -6)):
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (sx * 1.2, -0.3, 0), rot=(0, 0, rz), stages=[2, 3], boxes=[
            Box((-2.2 - sx * 0.7, -2.2, -2.2), (4.4, 2, 4.4), P2("pd" + side), dil=0.3),
            Box((-0.5 + sx * 1.0, -2.9, 0.6), (1, 1, 1), TN)]))
        B.append(Bone(f"{side}_bracer", f"{side}_forearm", stages=[2, 3], boxes=[Box((-1.5, 0, -1.5), (3, 3, 3), P2("br" + side, 0.18), dil=0.25)]))
        B.append(Bone(f"{side}_greave", f"{side}_shin", stages=[2, 3], boxes=[Box((-1.5, -1.8, -1.5), (3, 3, 3), P2("gv" + side), dil=0.35)]))
    B.append(Bone("belt_plate", "body", stages=[2, 3], boxes=[Box((-1.5, -3.6, -2.4), (3, 2.6, 0.6), P2("bt"), dil=0.1)]))
    # tier III, the Wight Stalker: a gilt band on the hood's brow, a gilt medallion with a soul stone on the sternum
    B.append(Bone("cowl_band", "head", stages=[3], boxes=[Box((-4.5, -10.95, -4.1), (9, 1, 9), ring(gb_gilt("cwb")))]))
    B.append(Bone("breast_clasp", "body", stages=[3], boxes=[Box((-1, -9.4, -2.9), (2, 2, 0.6), gb_gilt("bcl")),
                                                           Box((-0.5, -8.9, -3.1), (1, 1, 0.3), lambda f, w, h: [[SOUL[2]]])]))
    return B

NOTE = ("Graveborn Barrow Stalker (unit identity, Graveborn): a bare skeleton, open ribcage, spine and pelvis, a 7 px skull with "
        "two soul-light eyes under a dark hood hemmed in violet, a team sash on the pelvis, the frost bow. Tier II grave-iron "
        "pauldrons, bracers, greaves and gorget; tier III black gilt plate, a gilt hood band and medallion; no flag on the back, no antlers.")

STALKER = Troop("stalker", SKIN, CLOTH, bones, cloth_painter,
                {"weapon": "frost_bow", "hand": "left", "idleArms": "hang", "portrait": "standard"}, glow=glow)
def build(): return STALKER.build()
def outputs(): return STALKER.outputs()

if __name__ == "__main__":
    house_stub.main(STALKER, NOTE, sys.argv)
