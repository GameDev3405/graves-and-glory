#!/usr/bin/env python3
"""TEMPLATE builder for a NEW hand-authored troop. Copy it, never install it as it is. Recipe: docs/art/TEXTURE-RECIPE.md.

A plain biped soldier with three tiers, painted only from the locked House palette (art/palettes/<house>.json, through
palette.ramps / palette.require). Every place you are meant to change is marked "# EDIT:" with the recipe step it follows.
It is not a troop: it is not in sheets/design.json or art/troops/, so build_skins never builds it, and its `install`/`stub`
commands are refused.

  python art/hand/_template/build_template.py preview <dir> [side] [--house=<house>]   tier previews + geo, then the gate
  python art/hand/_template/build_template.py gate [--house=<house>]                   the gate alone (exit 1 on a problem)
  python art/hand/_template/build_template.py contact <png> [side] [--house=<house>]   flat five-view sheet, all tiers
  python tools/art/house_palette.py check --dir <dir> --house <house>                   palette check of the preview
  python tools/art/texture_audit_render.py --renderer <render_geo.py> --out <dir> --builder art/hand/_template/build_template.py --house <house>

The gate (section 8) is cheap tool enforcement of recipe rules: primary-ramp share, trim/accent share, top-to-bottom light
on every tall face, and how many texels the painters clamped at a ramp end. It is not the grade (recipe A8).
--house picks one of the PRESETS below (default HOUSE); a real troop drops the flag and fixes HOUSE.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import Troop, Bone, Box, LIT, lightx, clamp, blank, grid, over, limb, per_tier, rows, rects, on, ELBOW, HAND, KNEE   # noqa: F401
import palette

# ================================================================================================ 1. identity (recipe A1-A2)
TID = "template"          # EDIT (A2): the troop id, same as the folder art/hand/<tid>/ and the file build_<tid>.py
HOUSE = "ironclad"        # EDIT (A1): the House named in your brief (a new House: recipe part B)
for _a in list(sys.argv):   # template only: --house=<house> tries another preset without editing; a real troop deletes this loop
    if _a.startswith("--house="): HOUSE = _a.split("=", 1)[1]; sys.argv.remove(_a)

SKIN = (128, 128)         # skin sheet per tier; raise only if pack() says the sheet is full
CLOTH = (128, 64)         # team cloth sheet per side

# ================================================================================================ 2. materials (recipe A3)
# Each role names a ramp of the House palette (`python tools/art/house_palette.py show <house> <png>`, or art/palettes/<house>.png)
# and a base index: the tone of a flat, front-facing, mid-height texel. Base by ramp length (recipe A3 table):
# 4 tones -> 1, 5 -> 2, 6 -> 2 or 3, 7 -> 3 or 4. Ramps from other troops' builders (build_tc_raider.NAVY) are fine if they
# are in this House's palette. To ADD A ROLE (rope, shell, oilskin...): add "role": ("ramp name", base) to the dict, then
# paint with R("role") / B("role") / fill("role", ...); list it in PRIMARY_ROLES or TRIM_ROLES if it is one of those.
#   role: (ramp name in the palette, base index)
PRESETS = {
    "gilded": {"skin": ("handlib.SKINS.fair", 3), "hair": ("handlib.HAIRS.brown", 2), "plate": ("handlib.STEEL", 3),
               "mail": ("handlib.MAIL", 3), "padding": ("handlib.LINEN", 3), "leather": ("handlib.LEATH", 2),
               "trim": ("handlib.GOLD", 3), "accent": ("handlib.GOLD", 3)},
    "ironclad": {"skin": ("handlib.SKINS.fair", 3), "hair": ("handlib.HAIRS.black", 2), "plate": ("ironclad_tiers.IRON", 3),
                 "mail": ("handlib.MAIL", 3), "padding": ("ironclad.OILED", 2), "leather": ("handlib.LEATH", 2),
                 "trim": ("ironclad_tiers.BRASS", 3), "accent": ("ironclad_tiers.EMBER", 2)},
    "graveborn": {"skin": ("graveborn.GSKIN", 3), "hair": ("graveborn.BONE", 3), "plate": ("graveborn.GIRON", 4),
                  "mail": ("graveborn.RUST", 3), "padding": ("graveborn.ROT", 3), "leather": ("graveborn.MOSS", 3),
                  "trim": ("graveborn.GILT", 2), "accent": ("graveborn.VIOLET", 2)},
    "wildmoot": {"skin": ("wildmoot.PIG", 3), "hair": ("wildmoot.FUR", 2), "plate": ("wildmoot.BONE", 3),
                 "mail": ("wildmoot.GREYFUR", 3), "padding": ("wildmoot.HIDE", 3), "leather": ("handlib.LEATH", 2),
                 "trim": ("wildmoot.HORN", 2), "accent": ("wildmoot.OCHRE", 2)},
    "tidecallers": {"skin": ("build_tc_admiral.PALE", 3), "hair": ("build_tc_admiral.HAIR", 2), "plate": ("tidecallers.BRONZE", 3),
                    "mail": ("tidecallers.BARN", 2), "padding": ("build_tc_raider.NAVY", 2), "leather": ("handlib.LEATH", 2),
                    "trim": ("tidecallers.WARM", 3), "accent": ("tidecallers.SEA", 2)},
}
MAT = PRESETS[HOUSE]      # EDIT (A3): a real troop keeps ONE dict here, for its House, and tunes names and bases
PRIMARY_ROLES = ("plate", "padding", "mail")   # EDIT (A3): the House's primary armour/cloth ramps (recipe A3 table): the big masses
TRIM_ROLES = ("trim", "accent")                # trim metal and House accent together stay under TRIM_MAX of the skin
_R = palette.require(HOUSE, *sorted({n for n, _ in MAT.values()}))
def R(role): return _R[MAT[role][0]]           # the role's ramp, dark to light, as RGBA tuples
def B(role): return MAT[role][1]               # the role's base index
_TEAM = palette.ramps("team")                  # the team cloth ramps (cloth sheet only)

# ================================================================================================ 3. light (recipe A4)
_CLAMP = [0, 0]   # [texels painted by fill(), texels whose tone ran off a ramp end]: the gate reports the share

def tone(face, x, y, w, h, base):
    """The ramp index for one texel before any pattern: face light (LIT), the lit column (lightx: the wearer's right, the
    viewer's left, is lit), and top-to-bottom light on side faces, so a tall face is lighter at the top than at the foot.
    Limbs are painted as one 12-row face (Box cut), so the gradient runs shoulder to wrist and hip to sole."""
    v = base + LIT[face]
    if face in ("top", "bottom"): return v
    v += lightx(face, x, w)
    if h >= 6:                                   # tall face: a light top band and a dark foot band (audit finding 1)
        t = y / (h - 1)
        v += 1 if t < 0.17 else (-1 if t > 0.75 else 0)
    else:                                        # short face: a bright rim on top, a dark seam at the foot
        if y == 0: v += 1
        elif y == h - 1 and h > 2: v -= 1
    return v

def put(Rm, v):
    """clamp() that counts how often a tone ran off the ramp (a flattened highlight or shadow is lost form)."""
    _CLAMP[0] += 1
    if v < 0 or v >= len(Rm): _CLAMP[1] += 1
    return clamp(Rm, v)

def fill(role, base=None, pattern=None):
    """A painter of one material: tone() everywhere, then pattern(face, x, y, w, h) -> -1/0/+1 for its structure.
    ALWAYS build a painter on fill()/tone(): a painter that writes colours directly loses the light (the gate catches it)."""
    Rm, b0 = R(role), (B(role) if base is None else base)
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = put(Rm, tone(face, x, y, w, h, b0) + (pattern(face, x, y, w, h) if pattern else 0))
        return g
    return paint

# ================================================================================================ 4. materials (recipe A4, cue table)
# Patterns are STRUCTURE, never random speckle (audit finding 2). Each returns -1/0/+1 to add to tone().
# A painter may return None for a texel: it is TRANSPARENT (a cut-out: a face window, a ragged hem). Use it only on purpose.
def mail_check(face, x, y, w, h): return 0 if face in ("top", "bottom") or (x + y) % 2 == 0 else -1
def quilt_rows(face, x, y, w, h): return -1 if face not in ("top", "bottom") and y % 4 == 3 else 0
def folds(face, x, y, w, h):      # two folds on front and back below the chest, the shadow side of each fold one down
    return -1 if face in ("front", "back") and y >= 3 and w >= 6 and x in (w // 3, 2 * w // 3) else 0
def rope_twist(face, x, y, w, h):  # rope: diagonal twists, a light strand then a dark groove, running down the rope
    return 0 if face in ("top", "bottom") else (1 if (x + y) % 3 == 0 else (-1 if (x + y) % 3 == 2 else 0))
def gloss_fold(face, x, y, w, h):  # oilskin: dark matte field, one bright gloss streak along each fold's lit edge
    if face in ("top", "bottom") or w < 4: return 0
    return 2 if x in (1, w // 2 + 1) and 0 < y < h - 1 and y % 5 != 4 else (-1 if x in (2, w // 2 + 2) else 0)
def scale_rows(face, x, y, w, h):  # scales: rows offset by one, each scale lit on its upper edge
    if face in ("top", "bottom"): return 0
    return 1 if y % 2 == 0 and (x + (y // 2) % 2) % 2 == 0 else (-1 if y % 2 == 1 else 0)

def shell(role, base=None):
    """Shell: ridged fan (ridges radiating from the bottom centre, alternating light and dark) with a bright rim on top; the
    top face carries the ribs too, brightest along its front edge. Use the House TRIM or PLATE ramp (recipe A3), never a
    vivid off-tone colour."""
    b0 = B(role) if base is None else base
    def ridge(face, x, y, w, h):
        if face in ("top", "bottom"): return 0
        return 1 if ((x - w // 2) * 2 + (h - y)) % 3 == 0 else (-1 if ((x - w // 2) * 2 + (h - y)) % 3 == 2 else 0)
    body = fill(role, b0, ridge)
    def paint(face, w, h):
        g = body(face, w, h)
        if face not in ("top", "bottom"): g[0] = [put(R(role), b0 + 2)] * w        # the bright rim
        elif face == "top":                                                       # top: ribs fanning back from the lit front edge
            for y in range(h):
                for x in range(w): g[y][x] = put(R(role), b0 + 1 + (1 if y == h - 1 else (0 if x % 2 == 0 else -1)))
        return g
    return paint

def plate(role="plate", base=None, piping=None):
    """Plate: tone() plus a specular column near the lit edge of the front; piping=a role whose ramp edges the foot row."""
    def spec(face, x, y, w, h): return 1 if face == "front" and w >= 5 and x == 1 and 0 < y < h - 1 else 0
    body = fill(role, base, spec)
    def paint(face, w, h):
        g = body(face, w, h)
        if piping and face not in ("top", "bottom") and h >= 3:
            A = R(piping)
            for x in range(w): g[h - 1][x] = A[B(piping) + (1 if x % 2 == 0 and face in ("front", "right") else 0)]
        return g
    return paint

def rimmed(painter, role="trim", depth=1):
    """Tier III trim: a trim-metal band `depth` rows deep along the top of every side face, so it runs unbroken round the
    part. depth=2 on a big part (the breastplate) is the continuous trim structure the recipe asks of tier III (A6)."""
    def paint(face, w, h):
        g = painter(face, w, h)
        if face not in ("top", "bottom"):
            T = R(role)
            for y in range(min(depth, h)):
                for x in range(w): g[y][x] = T[min(len(T) - 1, B(role) + 1 - y + (face in ("front", "right")))]
        return g
    return paint

# ================================================================================================ 5. parts (recipe A4-A6)
def head():
    """EDIT (A5, personality): the face is the first thing a player reads. Brows, a scar, a beard, warpaint, a hollow eye.
    Rows 0..1 are under the hood's brim (tier I) and rows 0..3 under the helm (tiers II-III): paint cues on rows 4..7, or
    on row 3 only if no tier covers it. Bare skin sits at most one tone above the clothing beside it (recipe A3.5)."""
    S, Hh = R("skin"), R("hair")
    s = B("skin")
    pal = {"S": S[s + 1], "s": S[s], "d": S[s - 1], "D": S[s - 2], "w": S[-1], "e": Hh[0], "b": Hh[max(0, B("hair") - 1)],
           "h": Hh[B("hair")], "H": Hh[min(len(Hh) - 1, B("hair") + 1)], "m": S[s - 2]}
    front = grid("""
hHHHHHHh
hhHHHHhh
dsSSSSsd
sbbSsbbs
SweSSewS
sSSddSSs
dsSmmSsd
DdssssdD""", pal)
    def paint(face, w, h):
        if face == "front": return [list(r) for r in front]
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if face == "top": g[y][x] = pal["H"] if (y < 2 or x in (0, w - 1)) else pal["h"]; continue   # lit crown
                if face == "bottom": g[y][x] = S[s - 2]; continue
                back = x if face == "right" else (w - 1 - x) if face == "left" else None   # columns from the back edge (lightx: right's last column meets the front)
                if face == "back" or y < 2 or (back is not None and back < 3 and y < 6):                    # hair wraps round
                    g[y][x] = put(Hh, tone(face, x, y, w, h, B("hair")))
                else:
                    g[y][x] = put(S, tone(face, x, y, w, h, s))
                if back == 3 and y in (3, 4): g[y][x] = S[s - 1]                                            # the ear
        return g
    return paint

def hood():
    """Tier I hood in the primary cloth: tone() on every face, so each side face spans 3+ tones (light crown, mid, dark hem);
    a lit rim round the face window; the back has two structural features (a centre seam and two folds, recipe A7.1).
    The hood's brim hides head rows 0..1 and the tier II/III helm rows 0..3: keep the eyes (row 4) and the face cues below
    the lowest brim of any tier (recipe A5)."""
    body = fill("padding", B("padding") + 1, folds)
    Rp = R("padding")
    def paint(face, w, h):
        g = body(face, w, h)
        if face == "bottom": return blank(w, h)
        if face == "front":
            for y in range(2, h):
                for x in range(1, w - 1): g[y][x] = None                     # the face window
            for x in range(1, w - 1): g[1][x] = put(Rp, B("padding") + 3)    # the lit rim over the window (the brim's edge)
            for y in range(2, h - 1): g[y][0] = put(Rp, B("padding") + 2)    # its lit side (the wearer's right)
        if face == "back":
            for y in range(1, h - 1): g[y][w // 2] = put(Rp, tone(face, w // 2, y, w, h, B("padding")) - 1)          # seam
            for y in range(1, h - 1): g[y][w // 2 + 1] = put(Rp, tone(face, w // 2 + 1, y, w, h, B("padding")) + 1)  # its lit lip
        return g
    return paint

def torso(tier):
    """Primary cloth with quilting rows at tier I; from tier II mail shows below the breastplate (its own box)."""
    pad = fill("padding", pattern=quilt_rows)
    return pad if tier == 1 else rows([(0, pad), (6, fill("mail", pattern=mail_check))])

def belt():
    """Leather belt with an accent buckle: one of the accent's reliable homes (recipe A7)."""
    base = fill("leather")
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("front", "back", "right", "left"):
            A = R("accent")
            for y in range(h):
                for x in range(w):
                    if face == "front" and x in (w // 2 - 1, w // 2): g[y][x] = A[B("accent") + (1 if y == 0 else 0)]
                    elif y == h - 1: g[y][x] = A[max(0, B("accent") - 1)]    # an accent stitch line along the foot
        return g
    return paint

def arm():
    """Primary-cloth sleeve to the wrist, a leather glove. Painted as the WHOLE 12-row limb, so tone() lights it from the
    shoulder down on front, back and sides (the limb's two boxes each keep their rows: Box cut)."""
    sleeve, glove = fill("padding", pattern=quilt_rows), fill("leather")
    def paint(face, w, h):
        g, gl = sleeve(face, w, h), glove(face, w, h)
        for y in range(9, h): g[y] = gl[y]
        if face not in ("top", "bottom") and h > 9:
            A = R("accent")
            for x in range(w): g[9][x] = A[B("accent")]          # accent wrist wrap
        return g
    return paint

def leg(side):
    """Hose over the thigh, boots below; a SHADOW BAND above the knee and on the inner leg, a dark sole (audit finding 4)."""
    hose, boot = fill("padding", B("padding") - 1), fill("leather")
    inner_face = "left" if side == "right" else "right"          # the face toward the other leg: always in shadow
    def paint(face, w, h):
        g, bt = hose(face, w, h), boot(face, w, h)
        if face in ("top", "bottom"): return bt if face == "bottom" else g
        for y in range(8, h): g[y] = bt[y]
        Rp, Rl = R("padding"), R("leather")
        for x in range(w):
            g[5][x] = clamp(Rp, B("padding") - 2 + LIT[face])                 # the knee crease
            g[8][x] = clamp(Rl, B("leather") + 1)                             # the boot's turned-down cuff catches light
            g[h - 1][x] = Rl[0]                                               # the sole
        if face in ("front", "back"):                                         # inner column toward the other leg
            inner = (w - 1 if side == "right" else 0) if face == "front" else (0 if side == "right" else w - 1)
            for y in range(1, h - 1): g[y][inner] = clamp(Rp if y < 8 else Rl, (B("padding") if y < 8 else B("leather")) - 2)
        if face == inner_face:
            for y in range(h):
                for x in range(w):
                    c = g[y][x]
                    for Rm in (Rp, Rl):
                        if c in Rm: g[y][x] = Rm[max(0, Rm.index(c) - 1)]; break
        return g
    return paint

def helm(tier):
    """EDIT (A6): tier II an open cap with a nasal, tier III the same cap trim-rimmed."""
    cap = plate(piping="accent")
    return cap if tier == 2 else rimmed(cap)

def pauldron(tier, part):
    """Tier II mass from the House PLATE ramp (recipe A6): a domed cap and a lame, piped in the accent."""
    p = plate(base=B("plate") + (1 if part == "dome" else 0), piping="accent" if part != "dome" else None)
    return rimmed(p) if tier == 3 else p

# ================================================================================================ 6. team cloth (recipe A7)
def cloth_painter(spec, side):
    """Cloth sheet only, from the team palette: T the side's cloth ramp, M the emblem metal (silver on gold, else gold)."""
    T = _TEAM["handlib.TEAM.%s.cloth" % side]
    M = _TEAM["handlib.STEEL"][1:5] if side == "gold" else _TEAM["handlib.GOLD"][2:6]
    def tabard(face, w, h, want):
        g = blank(w, h)
        if face != want: return g          # a flat (0-deep) panel paints only its outward face, or the two z-fight
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, tone(face, x, y, w, h, 3) + folds(face, x, y, w, h))
        for x in range(w): g[h - 1][x] = M[1] if x % 2 else M[2]                  # a hem in the emblem metal
        if face == "front":                                                       # EDIT (A5): the troop's own device
            over(g, grid("""
.e.
eFe
.e.""", {"e": M[1], "F": M[3]}), w // 2 - 1, 1)
        return g                                                                  # the back keeps the folds and the hem
    def cape(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, tone(face, x, y, w, h, 3 if face == "back" else 2) - (1 if face == "back" and x % 3 == 2 and y > 2 else 0))
        if face == "back":
            for x in range(w): g[h - 1][x] = M[2]
        return g
    def crest(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(T, 3 + LIT[face] + (1 if x % 3 == 0 else 0))
        return g
    if spec.startswith("tabard_"): return lambda face, w, h: tabard(face, w, h, spec[len("tabard_"):])
    return {"cape": cape, "crest": crest}[spec]

# ================================================================================================ 7. model (recipe A6)
# Frames: a forearm box spans x -2..2, z -2..2 in the forearm's frame (the arm frame less the elbow), y -2..6; a shin box
# x -2..2, z -2..2, y -2..6. Put gear CENTRED on those numbers (x -2, z -2, width 4) and stand it proud with dil, not offsets.
# Layering in front: the body's front face is z -2. The team `tabard` bone (the engine swings it) hangs at z -2.6, so gear that
# must show IN FRONT of it needs its front face nearer than -2.65 (a box at z -2 with dil >= 0.7), or keep the tabard below
# the chest gear, as here (tabard y -5..3, breastplate y -12..-6).
def bones():
    """Bones and boxes. Keep the biped skeleton (handlib.limb joints, hand bones, arms_folded); add parts per tier with
    stages=[...]. Every tier step must ADD MASS that reads at 16 blocks (a plate, a lame, a cape), not only a stripe."""
    TEAM_EXTRAS = True                                               # EDIT (A6): False drops the tier III crest and cape
    Bn = [Bone("root", "")]
    Bn.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), per_tier(torso))]))
    Bn.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), head())]))
    Bn.append(Bone("hood", "head", tag="helm", stages=[1], boxes=[Box((-4.5, -8.5, -4.5), (9, 9, 9), hood())]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):
        Bn += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm(), f"{side}_forearm", ELBOW[side])
        Bn.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    Bn.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):
        Bn += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg(side), f"{side}_shin", KNEE)
    Bn.append(Bone("belt", "body", boxes=[Box((-4, -7, -2), (8, 2, 4), belt(), dil=0.4)]))
    Bn.append(Bone("tabard", "body", team=True, boxes=[                     # the engine swings a bone named "tabard"
        Box((-3, -5, -2.6), (6, 8, 0), "tabard_front", sheet="cloth"), Box((-3, -5, 2.6), (6, 8, 0), "tabard_back", sheet="cloth")]))
    # ---- tier II: breastplate, pauldrons, open helm, vambraces, knee cops: new masses in the House PLATE ramp -- EDIT (A6)
    Bn.append(Bone("breastplate", "body", stages=[2, 3], boxes=[
        Box((-4, -12, -2), (8, 6, 4), per_tier(lambda t: plate(piping="accent") if t == 2 else rimmed(plate(piping="accent"), depth=2), (2, 3)), dil=0.3)]))
    Bn.append(Bone("helm", "head", tag="helm", stages=[2, 3], boxes=[
        Box((-4.5, -8.5, -4.5), (9, 4, 9), per_tier(helm, (2, 3))),
        Box((-0.5, -5, -4.9), (1, 3, 1), plate(base=B("plate") + 1))]))   # the nasal
    ROLL = -14                                                       # pauldron roll in degrees, times c (see below)
    for side in ("right", "left"):
        # c is the side's sign: -1 on the wearer's right (-x), +1 on the left. Tie EVERY mirrored number to c: the x offset
        # outward (0.5 * c) and the z roll (ROLL * c). ROLL is NEGATIVE: -14 * c drops the outer edge on both shoulders
        # (verified in the front render, 2026-11-09). +14 * c lifts it and the pauldron reads as a wing.
        # ASYMMETRIC gear (one shell shoulder, one bare arm): wrap that bone in `if side == "left":` (or build it once outside
        # the loop with its own c); keep the other side's tier mass so the tier still reads. See recipe A2.5.
        c = -1 if side == "right" else 1
        Bn.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2, 0), stages=[2, 3], rot=(0, 0, ROLL * c), boxes=[
            Box((-3.5, -1.5, -3.5), (7, 2, 7), per_tier(lambda t: pauldron(t, "dome"), (2, 3))),
            Box((-3.5 + 0.5 * c, 0.5, -3), (7, 2, 6), per_tier(lambda t: pauldron(t, "lame"), (2, 3)), dil=0.15)]))
        Bn.append(Bone(f"{side}_vambrace", f"{side}_forearm", stages=[2, 3], boxes=[   # centred on the forearm box (-2..2)
            Box((-2, 0, -2), (4, 3, 4), per_tier(lambda t: plate() if t == 2 else rimmed(plate()), (2, 3)), dil=0.4)]))
        Bn.append(Bone(f"{side}_knee", f"{side}_shin", stages=[2, 3], boxes=[
            Box((-2.5, -1, -3), (5, 2, 2), plate(base=B("plate") + 1))]))
        # ---- tier III: a second pauldron lame, greaves, trim rims, crest and cape ----              EDIT (A6)
        Bn.append(Bone(f"{side}_lame2", f"{side}_arm", (c, -2, 0), stages=[3], rot=(0, 0, ROLL * c), boxes=[
            Box((-3 + 1.0 * c, 2.5, -2.5), (6, 2, 5), pauldron(3, "lame"), dil=0.2)]))
        Bn.append(Bone(f"{side}_greave", f"{side}_shin", stages=[3], boxes=[
            Box((-2, 0.5, -2), (4, 3, 4), rimmed(plate()), dil=0.4)]))
    # OPTIONAL: crest and cape are team cloth for tier III. Set TEAM_EXTRAS = False (top of bones) or delete these two bones if
    # the brief has no cape or crest; then the tier III step must come from the trim band and plate (recipe A6).
    if TEAM_EXTRAS: Bn.append(Bone("crest", "head", team=True, tag="helm", stages=[3], boxes=[Box((-1, -11, -4), (2, 2.5, 8), "crest", sheet="cloth")]))
    if TEAM_EXTRAS: Bn.append(Bone("cape", "body", (0, -12, 2.9), team=True, stages=[3], rot=(8, 0, 0), boxes=[
        Box((-4, 0, 0), (8, 15, 1), "cape", sheet="cloth")]))
    return Bn

# ================================================================================================ 8. the gate (recipe A8)
PRIMARY_MIN = 0.35     # share of opaque skin texels in PRIMARY_ROLES ramps, per tier (the House reads from its main masses)
TRIM_MAX = 0.20        # share in TRIM_ROLES ramps (that are not also primary), per tier
CLAMP_MAX = 0.12       # share of fill() texels whose tone ran off a ramp end: pick bases with headroom (recipe A3)
GRADIENT_TONES = 3     # lit faces (front, right): row mean tones, top to bottom, take at least this many distinct values
GRADIENT_SKIP = ("head",)   # bones whose own boxes are exempt from the top-to-bottom test (the face grid: hair over skin)

def _lum(c): return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]

def gate(troop):
    """Print the gate's numbers; return the list of problems (empty = pass). Shares are of SHEET texels, which include faces
    the model hides, so they are approximate: the render and the reviewer have the last word."""
    _CLAMP[0] = _CLAMP[1] = 0
    Bn, skins, _ = troop.build()
    problems = []
    cols = lambda roles: {c[:3] for r in roles if r in MAT for c in R(r)}
    prim = cols(PRIMARY_ROLES); trim = cols(TRIM_ROLES) - prim
    for t in troop.tiers:
        px = [p[:3] for p in skins[t].get_flattened_data() if p[3]]
        sp, st = sum(c in prim for c in px) / len(px), sum(c in trim for c in px) / len(px)
        print("gate tier %d: primary %2d%% (min %d%%), trim+accent %2d%% (max %d%%)" % (t, sp * 100, PRIMARY_MIN * 100, st * 100, TRIM_MAX * 100))
        if sp < PRIMARY_MIN: problems.append("tier %d: primary ramps cover only %d%%; paint the main masses from %s" % (t, sp * 100, PRIMARY_ROLES))
        if st > TRIM_MAX: problems.append("tier %d: trim+accent cover %d%%; trim is trim" % (t, st * 100))
    # every tall side face is lighter (a higher ramp index) at its top than at its foot. Tested on the painter's own output
    # for the whole face (a jointed limb as its full 12 rows), by ramp INDEX so a change of material (hose to boot) does not
    # count, and ignoring trim and accent texels (a piped foot or a rimmed top is trim, not light).
    index = {}
    for r in MAT:
        if r in TRIM_ROLES: continue
        for i, c in enumerate(R(r)): index.setdefault(c, i)
    for bone in Bn:
        if bone.name in GRADIENT_SKIP: continue
        for b in bone.boxes:
            if b.sheet != "skin" or (b.cut and b.cut[1] > 0): continue      # a limb is tested once, from its upper box
            w, h, d = (int(round(v)) for v in b.size)
            full = b.cut[0] if b.cut else h
            if full < 6: continue
            for t in troop.tiers:
                p = b.paint.get(t, b.paint.get(0))
                if not on(bone, t) or not p: continue
                for face in ("front", "back", "right", "left"):
                    g = p(face, w if face in ("front", "back") else d, full)
                    def mean(ys):
                        v = [index[c] for y in ys for c in g[y] if c in index]
                        return sum(v) / len(v) if v else None
                    top, foot = mean(range(max(1, -(-full // 6)))), mean(range(full - max(1, full // 4), full))
                    if top is None or foot is None: continue
                    if top <= foot:
                        problems.append("tier %d %s %s: %s face is flat or lit from below (mean tone top %.1f, foot %.1f)" % (t, bone.name, list(b.size), face, top, foot))
                    elif face in ("front", "right"):
                        span = {round(m) for m in (mean([y]) for y in range(full)) if m is not None}
                        if len(span) < GRADIENT_TONES:
                            problems.append("tier %d %s %s: %s face spans only %d tones top to bottom (need %d)" % (t, bone.name, list(b.size), face, len(span), GRADIENT_TONES))
    share = _CLAMP[1] / max(1, _CLAMP[0])
    print("gate: %d%% of painted texels clamped at a ramp end (max %d%%)" % (share * 100, CLAMP_MAX * 100))
    if share > CLAMP_MAX: problems.append("bases too close to a ramp end: %d%% of texels clamped" % (share * 100))
    for p in problems: print("GATE:", p)
    print("gate", "PASS" if not problems else "FAIL (%d)" % len(problems))
    return problems

NOTE = "TEMPLATE: not a troop. EDIT (A2): one line of identity, House, mark and tier steps for art/troops/<tid>.json."
TEMPLATE = Troop(TID, SKIN, CLOTH, bones, cloth_painter,
                 {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "standard"})   # EDIT (A2): weapon id (recipe A2)
def build(): return TEMPLATE.build()
def outputs(): return TEMPLATE.outputs()

if __name__ == "__main__":
    if TID == "template" and len(sys.argv) > 1 and sys.argv[1] in ("install", "stub"):
        sys.exit("the template is never installed: copy it to art/hand/<tid>/build_<tid>.py first (recipe A2)")
    if len(sys.argv) > 1 and sys.argv[1] == "gate": sys.exit(1 if gate(TEMPLATE) else 0)
    import house_stub   # a real troop: house_stub.main(TROOP, NOTE, sys.argv), which also writes the stub on `install`
    house_stub.main(TEMPLATE, NOTE, sys.argv)
    if len(sys.argv) > 1 and sys.argv[1] == "preview": gate(TEMPLATE)   # reported here; the `gate` command is the exit-1 check
