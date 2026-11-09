"""Shared ground for the hand-authored troops (art/hand/<troop>/build_<troop>.py), drawn at one texel per model pixel.

Ramps, face light rules and material painters (plate, mail, quilted cloth, leather), ASCII grids, the Box and Bone
model, UV shelf packing, the troop JSON, Bedrock geometry for Blockbench and contact sheets. Light comes from the top
front and the wearer's right, a bright top rim, a dark bottom seam, rounder plate toward the edges.

A troop module fills a Troop with its bones and its cloth painter; outputs() and the preview/contact/install commands
come from here.
"""
import json, os, sys
from PIL import Image

SIDES = ["gold", "violet", "crimson", "teal", "company", "linen"]

def hx(s): s = s.lstrip("#"); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)
def ramp(*cs): return [hx(c) for c in cs]

# hue-shifted ramps, darkest first: shadows lean cool or purple, lights lean warm
STEEL = ramp("15161f", "282c3b", "414a5d", "617086", "8a97ab", "b9c4d1", "eef3f6")
GOLD  = ramp("2c1909", "583513", "89581b", "b8852a", "dcb145", "f2d877", "fff5c4")
MAIL  = ramp("121319", "23262f", "373c4a", "525a6b", "737d90", "9ba5b5")
LEATH = ramp("1c100b", "362114", "54341e", "764b2d", "9a6a42", "c09068")
LINEN = ramp("3a3024", "675641", "958262", "bfac86", "ddcfaa", "f5edd8")
INK   = hx("0e0c12")
TEAM = {  # per side: five shades plus the emblem's metal (gold reads on every side but gold, which takes silver)
    "gold":    (ramp("3b2207", "6c430f", "a06c18", "cf9a2a", "efc75a"), STEEL[0:4]),
    "violet":  (ramp("1a1029", "33204f", "513585", "7655b4", "a185da"), GOLD[2:]),
    "crimson": (ramp("23080c", "4e1018", "821a22", "b22d2e", "d95c4f"), GOLD[2:]),
    "teal":    (ramp("06201f", "0d4341", "166a64", "23978b", "5cc5b4"), GOLD[2:]),
    "company": (ramp("0b0d14", "171c29", "25304a", "374767", "51658a"), GOLD[2:]),
    "linen":   (ramp("3a3024", "675641", "958262", "c4b28d", "e6dab8"), GOLD[2:]),
}

# --- faces of a box ------------------------------------------------------------------------------
FACES = ("top", "bottom", "right", "front", "left", "back")   # right = the wearer's right (-x)
LIT = {"top": 1, "front": 0, "right": 0, "left": -1, "back": -1, "bottom": -2}

def dims(face, w, h, d):
    return {"top": (w, d), "bottom": (w, d), "right": (d, h), "front": (w, h), "left": (d, h), "back": (w, h)}[face]

def blank(w, h): return [[None] * w for _ in range(h)]

def lightx(face, x, w):
    """How lit column x of a side face is, +1 at the edge nearest the light to -1 at the far one."""
    if w < 2: return 0
    t = x / (w - 1)
    if face in ("front",): t = t                 # wearer's right is column 0
    elif face == "back": t = 1 - t               # seen from behind, the wearer's right is the last column
    elif face == "right": t = 1 - t              # its last column meets the front
    elif face == "left": t = t                   # its first column meets the front
    else: return 0
    return 1 if t < 0.25 else (-1 if t > 0.75 else 0)

def clamp(r, i): return r[max(0, min(len(r) - 1, i))]

def plate(R=STEEL, base=3, spec=True, rim=True, seam=True):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face]
                if face in ("top", "bottom"):
                    if face == "top":
                        edge = x in (0, w - 1) or y == 0
                        v += -1 if edge else 0
                        if y == h - 1: v += 1
                    g[y][x] = clamp(R, v); continue
                v += lightx(face, x, w)
                if rim and y == 0: v += 1
                if seam and y == h - 1 and h > 2: v -= 1
                if spec and face == "front" and w >= 5 and x == 1 and 0 < y < h - 2: v += 1
                g[y][x] = clamp(R, v)
        return g
    return paint

def mail(base=3, rim=True):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + ((1 if x % 2 == 0 else 0) if y % 2 == 0 else (-1 if x % 2 else 0))
                if face not in ("top", "bottom"): v += lightx(face, x, w) if lightx(face, x, w) < 0 else 0
                if rim and y == 0 and face not in ("top", "bottom"): v += 1
                g[y][x] = clamp(MAIL, v)
        return g
    return paint

def quilt(R=LINEN, base=3, every=2):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face not in ("top", "bottom") and x % every == every - 1: v -= 1
                if y == 0: v += 1
                g[y][x] = clamp(R, v)
        return g
    return paint

def leather(base=3, stitch=False):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if y == 0 and face not in ("top", "bottom"): v += 1
                if y == h - 1 and h > 2 and face not in ("top", "bottom"): v -= 1
                if stitch and y == 1 and x % 2 == 0 and face not in ("top", "bottom"): v += 1
                g[y][x] = clamp(LEATH, v)
        return g
    return paint

def none(face, w, h): return blank(w, h)

def rows(painters):
    """Stack painters down the side faces: [(first_row, painter), ...]; the top face takes the first, the bottom the last."""
    def paint(face, w, h):
        if face == "top": return painters[0][1](face, w, h)
        if face == "bottom": return painters[-1][1](face, w, h)
        g = blank(w, h)
        for i, (r0, p) in enumerate(painters):
            r1 = painters[i + 1][0] if i + 1 < len(painters) else h
            if r1 <= r0: continue
            part = p(face, w, r1 - r0)
            for y in range(r0, r1): g[y] = part[y - r0]
        return g
    return paint

def grid(text, pal):
    out = []
    for line in text.strip("\n").splitlines():
        line = line.split("|")[0].replace(" ", "")
        out.append([None if ch == "." else pal[ch] for ch in line])
    return out

def over(base, top, x0=0, y0=0):
    for y, row in enumerate(top):
        for x, c in enumerate(row):
            if c is not None and 0 <= y0 + y < len(base) and 0 <= x0 + x < len(base[0]): base[y0 + y][x0 + x] = c
    return base

def cut(g, cells):
    for x, y in cells:
        if 0 <= y < len(g) and 0 <= x < len(g[0]): g[y][x] = None
    return g

def pal(main=STEEL, **extra):
    p = {str(i): c for i, c in enumerate(main)}
    p.update({ch: GOLD[i] for i, ch in enumerate("abcdefg")})
    p.update({"k": INK, "m": MAIL[1], "n": MAIL[3], "o": MAIL[4]})
    p.update({ch: LEATH[i] for i, ch in enumerate("ABCDEF")})
    p.update(extra)
    return p

def with_faces(painter, **faces):
    """A painter whose named faces are replaced or stamped: face=grid, or face=(painter_result_fn)."""
    def paint(face, w, h):
        g = painter(face, w, h)
        if face in faces:
            f = faces[face]
            g = f(g, w, h) if callable(f) else over(g, f)
        return g
    return paint

# --- the model -----------------------------------------------------------------------------------
class Box:
    """cut=(full_h, y0): this box is rows y0..y0+h of a limb full_h tall (a jointed limb's upper or lower half, limb()): its painter
    paints the whole limb's sides and the box keeps its rows, so the two halves at rest look as the one-piece limb did. The limb's
    top shows on the box at y0 0 and its bottom on the box that ends it; the faces at the joint are caps (paint_box)."""
    def __init__(s, origin, size, paint, dil=0.0, sheet="skin", cut=None):
        s.origin, s.size, s.dil, s.sheet, s.cut = list(origin), list(size), dil, sheet, cut
        s.paint = paint if isinstance(paint, dict) else {0: paint}   # {tier or 0 for every tier: painter}
        s.uv = None

class Bone:
    """limit: the joint's pitch range in degrees, [min, max], or None (no limit); the troop file keeps it as the bone's "limit"
    and the client reads it through TroopModel.jointLimit (a mage's rigid robe holds its knees to [0, 10])."""
    def __init__(s, name, parent, pivot=(0, 0, 0), boxes=(), stages=(), tag="", team=False, rot=(0, 0, 0), variants=None, part="", limit=None, weapon_tiers=()):
        s.weapon_tiers = list(weapon_tiers)   # a weapon part of a troop with no held weapon: the weapon tiers it shows at
        s.name, s.parent, s.pivot, s.boxes, s.stages, s.tag, s.team, s.rot, s.variants = name, parent, list(pivot), list(boxes), list(stages), tag, team, list(rot), variants
        s.part = part   # "pennant" or "standard": a flag bone, drawn only on the squad's bearer
        s.limit = list(limit) if limit is not None else None

# The biped's jointed limbs (animation rework Task 9, the rig contract): each 4x12x4 limb is an upper and a lower 4x6x4 box, the
# lower on a bone at the joint, 6 px down the limb: the forearm at the elbow ([-1, 4, 0] in the right arm's frame, [1, 4, 0] in the
# left's), the shin at the knee ([0, 6, 0]); a hand hangs from its forearm at [0, 4, 0], where the one-piece arm held it.
ELBOW = {"right": (-1, 4, 0), "left": (1, 4, 0)}
KNEE = (0, 6, 0)
HAND = (0, 4, 0)
# Default hinge ranges (pitch, degrees from the file pose) for a limb's lower bone: TroopMotion.limits clamps them, so a
# positive forearm rx (a backwards elbow) or a negative shin rx (a backwards knee) can never reach the screen.
JOINT_LIMITS = {"_forearm": (-150, 0), "_shin": (0, 150)}
JOINT_OVERLAP = 2  # px a limb's lower box reaches past its hinge into the upper box (review 2026-11-04 section 1.3)
# px the lower box of a jointed limb is shrunk on every side (a negative inflate) where a rig opts in (limb inset=JOINT_INSET): the
# lower box shares the upper's cross-section through the overlap, so a bent knee or elbow left their side faces coplanar and the two
# textures z-fought in stripes. Inset, the upper box's faces always win; 0.1 px is under a texel yet resolves in depth to ~70 blocks.
JOINT_INSET = 0.1

def limb(name, parent, pivot, origin, paint, lower, joint, size=(4, 12, 4), dil=0.0, sheet="skin", limit=None, upper_h=6, rot=(0, 0, 0), inset=0.0):
    """A jointed limb as two bones: `name` at pivot with the upper box at origin, and `lower` at joint (in the upper bone's frame)
    with the lower box under it, both painted from the one painter of the whole limb (Box cut). Returns [upper bone, lower bone].
    rot: the upper bone's rest rotation (a leaning troop's arms stand back up); the lower bone has none of its own.
    inset: px the lower box is shrunk on every side (JOINT_INSET), so the joint's coplanar side faces never z-fight; UVs unchanged."""
    w, h, d = size
    if limit is None:  # a hinge bends one way: an elbow flexes (rx <= 0), a knee folds back (rx >= 0); see JOINT_LIMITS
        limit = next((v for k, v in JOINT_LIMITS.items() if lower.endswith(k)), None)
    # the lower box starts JOINT_OVERLAP rows above the hinge, painted with the limb's same rows as the upper box there, so a bent
    # elbow or knee shows the limb's own texels across the outside of the bend instead of a gap
    o = min(JOINT_OVERLAP, upper_h)
    lo = [origin[0] - joint[0], origin[1] + upper_h - o - joint[1], origin[2] - joint[2]]
    return [Bone(name, parent, pivot, [Box(origin, (w, upper_h, d), paint, dil, sheet, cut=(h, 0))], rot=rot),
            Bone(lower, name, joint, [Box(lo, (w, h - upper_h + o, d), paint, dil - inset, sheet, cut=(h, upper_h - o))], limit=limit)]

def arms(painter, right=None):
    """The biped's two jointed arms with their hands (right painter, then left; one painter serves both when right is None)."""
    out = []
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):
        p = painter if right is None or side == "left" else right
        out += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), p, f"{side}_forearm", ELBOW[side])
        out.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    return out

def legs(painter, limit=None):
    """The biped's two jointed legs (thigh and shin)."""
    out = []
    for side, x in (("right", -1.9), ("left", 1.9)):
        out += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), painter, f"{side}_shin", KNEE, limit=limit)
    return out

def on(bone, tier): return not bone.stages or tier in bone.stages
def shown(bone, tier): return on(bone, tier) and (not bone.weapon_tiers or tier in bone.weapon_tiers)   # weapon tier = worn tier on the sheets

# ---- skin and hair ramps ----
SKINS = {
    "fair":  ramp("4a2a20", "8a5340", "c08068", "e2aa8c", "f4ccb0"),
    "olive": ramp("3c2618", "6e4630", "a06c4c", "c8926c", "e0b48e"),
    "brown": ramp("24140e", "4a2c1c", "6e4430", "905c40", "b07a58"),
    "pale":  ramp("4c3430", "8c6458", "bc9282", "dcb6a4", "f0d6c8"),
}
HAIRS = {
    "brown": ramp("1c120c", "3a2618", "5a3c24", "7c5634"),
    "black": ramp("0c0a0c", "1a161a", "2c262a", "423a3c"),
    "blond": ramp("4a3418", "8a6430", "c09a4c", "e2c278"),
    "grey":  ramp("3a3a40", "66666e", "9a9aa2", "c8c8cc"),
}

# ---- faces: four men of the Order, hair carried round the head ----
FACE_ART = [  # S skin light, s skin, d shade, D deep, h/H/i hair dark/mid/light, w eye white, e iris, m mouth, b brow
    ("fair", "brown", """
hhHHHHhh
hHHiiHHh
HsSSSSsH
sbbSsbbs
SweSSews
sSSddSSs
dsSmmSsd
DdssssdD"""),
    ("olive", "black", """
hhhhhhhh
hHHHHHHh
hsSSSSsh
sbbSsbbs
sweSSews
sSSddSSs
hHHmmHHh
hhHHHHhh"""),
    ("brown", "black", """
hhhhhhhh
hHhHHhHh
dsSSSSsd
sbbsSbbs
sweSSews
sSsddsSs
dsDmmDsd
DddssddD"""),
    ("pale", "grey", """
iHiHHiHi
HiHiiHiH
HsSSSSsH
sHHSsHHs
SweSSewS
sSSddSSs
dHHHHHHd
DdsmmsdD"""),
]

def face_painter(k):
    skin_name, hair_name, front = FACE_ART[k]
    S, Hh = SKINS[skin_name], HAIRS[hair_name]
    eye = {"fair": "3c5a78", "olive": "3a2a1c", "brown": "2a1a10", "pale": "5a6a5a"}[skin_name]
    p = {"S": S[4], "s": S[3], "d": S[2], "D": S[1], "h": Hh[0], "H": Hh[1], "i": Hh[2], "w": hx("f2efe8"),
         "e": hx(eye), "m": S[1], "b": Hh[0]}
    bearded = k in (1, 3)
    def paint(face, w, h):
        if face == "front": return grid(front, p)
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face == "top": g[y][x] = Hh[2] if (x + y) % 3 == 0 else Hh[1]; continue
                if face == "bottom": g[y][x] = S[1]; continue
                if face == "back":
                    g[y][x] = (Hh[1] if (x + y) % 3 else Hh[0]) if y < 7 else S[2]; continue
                # sides: hair over the top and back, an ear, a beard along the jaw
                toward_back = (w - 1 - x) if face == "right" else x
                hair = y < 2 or toward_back < 3 and y < 7
                if hair: g[y][x] = Hh[1] if (x + y) % 3 else Hh[2]
                elif bearded and y >= 6: g[y][x] = Hh[1] if (x + y) % 2 else Hh[0]
                else: g[y][x] = S[3] if face == "right" else S[2]
                if toward_back == 3 and y in (3, 4): g[y][x] = S[1] if y == 4 else S[2]   # ear
        return g
    return paint


# ---- the heater shield: a wooden board in a raised metal rim, its face in the team cloth ----
# Its frame is vanilla's shield model (face outward along -z, y down, the grip at the origin). Vanilla's held pose ran the
# forearm through the board, so it hangs on the forearm instead: TroopAnimator carries the shield arm 45 degrees forward,
# and SHIELD_ROT is Rx(45)*Ry(-60), which undoes that carry and turns the face out and forward, so the board stands upright
# beside the forearm, its top at the shoulder, clear of the pauldron, the enarmes reaching back to the arm. Its bone is the
# left_forearm's child: SHIELD_PIVOT is the old arm-frame grip (2.7, 10, -1.5) less the elbow (1, 4, 0), the same point at rest.
SHIELD_PIVOT, SHIELD_ROT = (1.7, 6.0, -1.5), (63.435, -37.761, -50.768)
SHIELD_W = [12] * 12 + [10, 10, 8, 8, 6, 4]           # row widths, top to bottom: 18 rows, y -10..8
SHIELD_BODY = [((-6, -10), (12, 12)), ((-5, 2), (10, 2)), ((-4, 4), (8, 2)), ((-3, 6), (6, 1)), ((-2, 7), (4, 1))]
SHIELD_RIM = [((-6, -10), (12, 1)), ((-6, -9), (1, 11)), ((5, -9), (1, 11)), ((-5, 2), (1, 2)), ((4, 2), (1, 2)),
              ((-4, 4), (1, 2)), ((3, 4), (1, 2)), ((-3, 6), (1, 1)), ((2, 6), (1, 1)), ((-2, 7), (4, 1))]

def in_shield(x, y, inset=0):
    """Whether column x (0..11) of row y (0..17) lies on the board, inset rings in from its edge."""
    if not inset <= y < len(SHIELD_W) - inset: return False
    def inside(xx, yy): return 0 <= yy < len(SHIELD_W) and abs(xx + 0.5 - 6) <= SHIELD_W[yy] / 2
    return all(inside(x + dx, y + dy) for dx in range(-inset, inset + 1) for dy in range(-inset, inset + 1))

def wood(face, w, h):
    g = blank(w, h)
    for y in range(h):
        for x in range(w):
            v = 3 + LIT[face] + (-1 if face in ("front", "back") and x % 3 == 2 else 0)
            if face == "back" and h >= 12 and y in (3, 8): v = 1 + (x % 2)            # the leather straps
            g[y][x] = clamp(LEATH, v)
    return g

def shield_rim(tier):
    R, base = (STEEL, 2) if tier == 1 else (STEEL, 3) if tier == 2 else (GOLD, 3)
    def paint(face, w, h):
        g = plate(R=R, base=base, spec=False, seam=False, rim=(face == "front" and h == 1))(face, w, h)
        if face == "front" and tier >= 2:
            for y in range(h):
                for x in range(w):
                    if (h > 1 and y % 3 == 1) or (h == 1 and w > 4 and x % 3 == 1): g[y][x] = R[6] if tier == 3 else STEEL[5]   # rivets
        if face == "front" and h > 1: g[0] = [clamp(R, base + 1)] * w                # each step catches light on its top
        return g
    return paint

def shield_boss(tier):
    R = GOLD if tier == 3 else STEEL
    def paint(face, w, h):
        g = plate(R=R, base=4 if tier > 1 else 3, spec=False, seam=False)(face, w, h)
        if face == "front": g[0][0] = R[6]; g[h - 1][w - 1] = R[2]
        return g
    return paint


def per_tier(f, tiers=(1, 2, 3)): return {t: f(t) for t in tiers}

# ---- UV packing and painting ----
def footprint(b):
    w, h, d = (int(round(v)) for v in b.size)
    return 2 * (w + d), h + d

def pack(boxes, size, who=""):
    """Shelf-pack box footprints (tallest first) onto a sheet."""
    order = sorted(boxes, key=lambda b: (-footprint(b)[1], -footprint(b)[0]))
    x = y = shelf = 0
    for b in order:
        fw, fh = footprint(b)
        if x + fw > size[0]: x, y, shelf = 0, y + shelf, 0
        if y + fh > size[1]: raise SystemExit(f"{who} sheet {size} is full")
        b.uv = [x, y]; x += fw; shelf = max(shelf, fh)

def rects(b):
    w, h, d = (int(round(v)) for v in b.size)
    u, v = b.uv
    return {"top": (u + d, v), "bottom": (u + d + w, v), "right": (u, v + d), "front": (u + d, v + d),
            "left": (u + d + w, v + d), "back": (u + 2 * d + w, v + d)}

SIDE_FACES = ("front", "back", "right", "left")

def joint_cap(painter, face, w, d, full, row):
    """A cut limb's face at its joint (the upper box's bottom, the lower box's top): each texel takes the colour of the nearest side
    face's texel in the joint row, so a bent joint shows the limb's own skin, cloth or armour across the cut, never a hole.
    The top face's rows run back to front and the bottom's front to back (render.faces_of, ModelPart's box UV)."""
    sides = {f: painter(f, *dims(f, w, full, d)) for f in SIDE_FACES}
    def px(f, col):
        g = sides[f]
        return g[row][col] if row < len(g) and col < len(g[row]) else None
    g = blank(w, d)
    for r in range(d):
        k = r if face == "bottom" else d - 1 - r      # depth from the front
        for i in range(w):                            # columns run from the wearer's right (x min)
            near = sorted([(k, 0, "front", i), (d - 1 - k, 1, "back", w - 1 - i), (i, 2, "right", d - 1 - k), (w - 1 - i, 3, "left", k)])
            for _, _, f, col in near:
                c = px(f, col)
                if c is not None: g[r][i] = c; break
    return g

def paint_box(img, b, painter, caps=True):
    """Paints a box's faces from a painter. A cut box (Box cut) paints its rows of the whole limb's sides; its joint faces are caps
    (joint_cap), which the trim and glow overlays leave blank (caps=False)."""
    w, h, d = (int(round(v)) for v in b.size)
    full, c0 = b.cut if b.cut else (h, 0)
    for face, (x0, y0) in rects(b).items():
        fw, fh = dims(face, w, h, d)
        if fw == 0 or fh == 0: continue
        if b.cut and face in SIDE_FACES:
            g = painter(face, fw, full)[c0:c0 + h]
        elif b.cut and (face == "top" and c0 > 0 or face == "bottom" and c0 + h < full):
            if not caps: continue
            g = joint_cap(painter, face, w, d, full, c0 if face == "top" else c0 + h - 1)
        else:
            g = painter(face, fw, fh)
        for y in range(fh):
            for x in range(fw):
                c = g[y][x] if y < len(g) and x < len(g[y]) else None
                if c is not None: img.putpixel((x0 + x, y0 + y), c)

# ---- a troop: build, files and commands ----
class Troop:
    """One hand-authored troop. bones() -> [Bone]; cloth(spec, side) -> painter for a team box's paint spec; meta holds
    the troop JSON's own fields (weapon, hand, idleArms, portrait, faces, glow...). veteran(bone, box) -> painter or None
    paints the Mastery II trim, an overlay on the skin sheet kept to texels every tier paints (build_skins' trim rules)."""
    def __init__(s, tid, skin, cloth, bones, cloth_painter, meta, tiers=(1, 2, 3), hide=lambda bone: False, veteran=None, glow=None):
        s.tid, s.skin, s.cloth, s.bones, s.cloth_painter, s.meta, s.tiers, s.hide = tid, tuple(skin), tuple(cloth), bones, cloth_painter, meta, tiers, hide
        s.veteran, s.glow = veteran, glow   # glow(bone, box) -> painter or None: the _e sheet, lit only where the skin is opaque

    def faces(s): return s.meta.get("faces", {}).get("count", 1)

    def build(s):
        B = s.bones()
        skin_boxes = [b for bone in B for b in bone.boxes if b.sheet == "skin"]
        cloth_boxes = [b for bone in B for b in bone.boxes if b.sheet == "cloth"]
        pack(skin_boxes, s.skin, s.tid + " skin"); pack(cloth_boxes, s.cloth, s.tid + " cloth")
        skins = {}
        for t in s.tiers:
            img = Image.new("RGBA", s.skin, (0, 0, 0, 0))
            for bone in B:
                if not on(bone, t): continue
                for b in bone.boxes:
                    if b.sheet != "skin": continue
                    p = b.paint.get(t, b.paint.get(0))
                    if p: paint_box(img, b, p)
            skins[t] = img
        cloths = {}
        for side in SIDES:
            img = Image.new("RGBA", s.cloth, (0, 0, 0, 0))
            for b in cloth_boxes: paint_box(img, b, s.cloth_painter(b.paint[0], side))
            cloths[side] = img
        s.trim = s.build_trim(B, skins) if s.veteran else None
        s.lit = s.build_glow(B, skins) if s.glow else None
        return B, skins, cloths

    def build_glow(s, B, skins):
        img = Image.new("RGBA", s.skin, (0, 0, 0, 0))
        for bone in B:
            for b in bone.boxes:
                p = s.glow(bone, b) if b.sheet == "skin" else None
                if p: paint_box(img, b, p, caps=False)
        px, sp = img.load(), skins[s.meta.get("glowFrom", s.tiers[0])].load()
        for y in range(s.skin[1]):
            for x in range(s.skin[0]):
                if not px[x, y][3] or not sp[x, y][3]: px[x, y] = (0, 0, 0, 0)
                else: px[x, y] = px[x, y][:3] + (255,)
        return img

    TRIM_SHARE = 0.20   # build_skins' cap on the trim's share of the stage 1 skin's opaque texels

    def build_trim(s, B, skins):
        img = Image.new("RGBA", s.skin, (0, 0, 0, 0))
        for bone in B:
            for b in bone.boxes:
                p = s.veteran(bone, b) if b.sheet == "skin" else None
                if p: paint_box(img, b, p, caps=False)
        px, sp = img.load(), [skins[t].load() for t in s.tiers]
        n = 0
        for y in range(s.skin[1]):
            for x in range(s.skin[0]):
                if px[x, y][3] and any(k[x, y][3] == 0 for k in sp): px[x, y] = (0, 0, 0, 0)
                elif px[x, y][3]: px[x, y] = px[x, y][:3] + (255,); n += 1
        base = sum(1 for y in range(s.skin[1]) for x in range(s.skin[0]) if sp[0][x, y][3])
        assert 0 < n <= s.TRIM_SHARE * base, f"{s.tid}: veteran trim covers {n} texels of {base}"
        return img

    def troop_json(s, B):
        base = "gravesandglory:textures/entity/troop/%s/" % s.tid
        out = {"format": 3, "troop": s.tid, "rig": "biped", "size": list(s.skin), "density": 1, "cloth": base + "team_cloth_",
               "skins": {str(t): base + "%s_s%d.png" % (s.tid, t) for t in s.tiers},
               "veteran": base + "%s_veteran.png" % s.tid if s.veteran else "", "glow": base + "%s_e.png" % s.tid if s.glow else "", "glowFrom": 1, "outer": "", "weapon": "none", "hand": "right",
               "idleArms": "hang", "portrait": "standard", "faces": {"count": 1, "card": 0}}
        out.update(s.meta)
        if out["faces"].get("count", 1) < 2: del out["faces"]   # one face: the loader wants no "faces" at all
        out["bones"] = []
        for bone in B:
            j = {"name": bone.name, "parent": bone.parent, "part": bone.part, "team": bone.team, "tag": bone.tag, "stages": bone.stages}
            if bone.weapon_tiers: j["weaponTiers"] = bone.weapon_tiers
            if bone.variants is not None: j["variants"] = bone.variants
            if bone.limit is not None: j["limit"] = bone.limit
            j.update({"sheet": list(s.cloth if bone.team else s.skin), "pivot": bone.pivot, "rotation": bone.rot,
                      "boxes": [{"origin": b.origin, "size": b.size, "uv": b.uv, "mirror": False, "dilation": [b.dil] * 3} for b in bone.boxes]})
            out["bones"].append(j)
        if len(out["bones"]) > 64:   # TroopModel.MAX_BONES: the client keeps a draw's bones as the bits of one long and refuses a bigger model
            raise SystemExit("%s: %d bones, more than 64" % (s.tid, len(out["bones"])))
        return out

    def geo(s, B, tier, face, name):
        """Bedrock geometry of one tier for Blockbench, the cloth sheet stacked under the skin sheet."""
        absp, bones_out = {}, []
        for bone in B:
            pp = absp.get(bone.parent, [0, 0, 0])
            ap = [pp[i] + bone.pivot[i] for i in range(3)]
            absp[bone.name] = ap
            if not shown(bone, tier) or (bone.variants is not None and face not in bone.variants): continue
            cubes = []
            for b in bone.boxes:
                o = [ap[i] + b.origin[i] for i in range(3)]
                uv = list(b.uv) if b.sheet == "skin" else [b.uv[0], b.uv[1] + s.skin[1]]
                cubes.append({"origin": [o[0], 24 - (o[1] + b.size[1]), o[2]], "size": b.size, "uv": uv, "inflate": b.dil})
            j = {"name": bone.name, "pivot": [ap[0], 24 - ap[1], ap[2]], "rotation": [-bone.rot[0], bone.rot[1], -bone.rot[2]], "cubes": cubes}
            if bone.parent: j["parent"] = bone.parent
            bones_out.append(j)
        return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": f"geometry.{name}",
                "texture_width": s.skin[0], "texture_height": s.skin[1] + s.cloth[1]}, "bones": bones_out}]}

    def outputs(s):
        """Every file this troop ships, for tools/build_skins.py: {path under the assets root: PIL image or JSON object}."""
        B, skins, cloths = s.build()
        out = {f"textures/entity/troop/{s.tid}/{s.tid}_s{t}.png": skins[t] for t in s.tiers}
        out.update({f"textures/entity/troop/{s.tid}/team_cloth_{side}.png": cloths[side] for side in SIDES})
        if s.trim: out[f"textures/entity/troop/{s.tid}/{s.tid}_veteran.png"] = s.trim
        if s.lit: out[f"textures/entity/troop/{s.tid}/{s.tid}_e.png"] = s.lit
        out[f"models/troop/{s.tid}.json"] = s.troop_json(B)
        return out

    def visible(s, t, show_hidden=False):
        n = s.faces()
        return lambda bn: shown(bn, t) and (bn.variants is None or ((t - 1) % n) in bn.variants) and (show_hidden or not s.hide(bn))

    def contact(s, out, side="crimson", show_hidden=False, px=6, vet=False):
        import render
        B, skins, cloths = s.build()
        if vet and s.trim:
            for t in s.tiers: skins[t] = Image.alpha_composite(skins[t], s.trim)
        views = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
        rows_ = [render.sheet_of([render.render(B, s.visible(t, show_hidden), {"skin": skins[t], "cloth": cloths[side]}, y, p, px=px)
                                  for y, p in views]) for t in s.tiers]
        W = max(r.width for r in rows_); img = Image.new("RGBA", (W, sum(r.height for r in rows_)), (40, 42, 50, 255)); y = 0
        for r in rows_: img.paste(r, (0, y)); y += r.height
        img.save(out); print("contact sheet", out)

    def main(s, argv):
        cmd = argv[1] if len(argv) > 1 else ""
        if cmd == "preview":
            B, skins, cloths = s.build()
            out = argv[2]; side = argv[3] if len(argv) > 3 else "crimson"
            os.makedirs(out, exist_ok=True)
            for t in s.tiers:
                img = Image.new("RGBA", (s.skin[0], s.skin[1] + s.cloth[1]), (0, 0, 0, 0))
                img.paste(skins[t], (0, 0)); img.paste(cloths[side], (0, s.skin[1]))
                img.save(os.path.join(out, f"{s.tid}_t{t}.png"))
                with open(os.path.join(out, f"{s.tid}_t{t}.geo.json"), "w") as f:
                    json.dump(s.geo(B, t, (t - 1) % s.faces(), f"{s.tid}_t{t}"), f)
                skins[t].save(os.path.join(out, f"skin_s{t}.png"))
            for side_ in SIDES: cloths[side_].save(os.path.join(out, f"cloth_{side_}.png"))
            print("preview written to", out)
        elif cmd == "contact":
            s.contact(argv[2], argv[3] if len(argv) > 3 else "crimson", show_hidden="all" in argv[4:], vet="vet" in argv[4:])
        elif cmd == "install":   # build_skins builds hand-authored troops from outputs() and keeps --verify honest
            import subprocess
            repo = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..")
            sys.exit(subprocess.call([sys.executable, os.path.join(repo, "tools", "build_skins.py")], cwd=repo))
        else:
            print("commands: preview <dir> [side] | contact <png> [side] [all] [vet] | install")
