#!/usr/bin/env python3
"""TEMPLATE builder for a NEW hand-authored troop. Copy it, never install it as it is. Recipe: docs/art/TEXTURE-RECIPE.md.

A plain biped soldier with three tiers, painted only from the locked House palette (art/palettes/<house>.json, through
palette.ramps / palette.require). Every place you are meant to change is marked "# EDIT:" with the recipe step it follows.
It is not a troop: it is not in sheets/design.json or art/troops/, so build_skins never builds it, and its `install`/`stub`
commands are refused.

  python art/hand/_template/build_template.py preview <dir> [side] [--house=<house>]   tier previews + geo (default side crimson)
  python art/hand/_template/build_template.py contact <png> [side] [--house=<house>]   flat five-view sheet, all tiers
  python tools/art/house_palette.py check --dir <dir> --house <house>                   palette check of the preview
  python tools/art/texture_audit_render.py --renderer <render_geo.py> --out <dir> --builder art/hand/_template/build_template.py --house <house>

--house picks one of the PRESETS below (default HOUSE); a real troop drops the flag and fixes HOUSE.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import Troop, Bone, Box, LIT, lightx, clamp, blank, grid, over, limb, per_tier, rows, ELBOW, HAND, KNEE   # noqa: F401
import palette

# ================================================================================================ 1. identity (recipe A1-A2)
TID = "template"          # EDIT (A2): the troop id, same as the folder art/hand/<tid>/ and the file build_<tid>.py
HOUSE = "ironclad"        # EDIT (A1): gilded, graveborn, wildmoot, ironclad or tidecallers (a new House: recipe part B)
for _a in list(sys.argv):   # template only: --house=<house> tries another preset without editing; a real troop deletes this loop
    if _a.startswith("--house="): HOUSE = _a.split("=", 1)[1]; sys.argv.remove(_a)

SKIN = (128, 128)         # skin sheet per tier; raise only if pack() says the sheet is full
CLOTH = (128, 64)         # team cloth sheet per side

# ================================================================================================ 2. materials (recipe A3)
# Each role names a ramp of the House palette (see `python tools/art/house_palette.py show <house>` or art/palettes/<house>.png)
# and a base index: the tone a flat, front-facing, mid-height texel gets. Painters move +-1..2 from it for light, so pick a
# base with at least two steps above it and one below. Head and torso sit a step lighter than legs. A dark House ramp
# (Graveborn GIRON) needs a high base or the figure is a black blob at 16 blocks (audit finding 3).
#   role: (ramp name in the palette, base index)
PRESETS = {
    "gilded": {"skin": ("handlib.SKINS.fair", 3), "hair": ("handlib.HAIRS.brown", 2), "plate": ("handlib.STEEL", 3),
               "mail": ("handlib.MAIL", 3), "padding": ("handlib.LINEN", 3), "leather": ("handlib.LEATH", 2),
               "trim": ("handlib.GOLD", 3), "accent": ("handlib.GOLD", 3)},
    "ironclad": {"skin": ("handlib.SKINS.fair", 3), "hair": ("handlib.HAIRS.black", 2), "plate": ("ironclad_tiers.IRON", 3),
                 "mail": ("handlib.MAIL", 3), "padding": ("ironclad.FUR", 3), "leather": ("ironclad.OILED", 2),
                 "trim": ("ironclad_tiers.BRASS", 3), "accent": ("ironclad_tiers.EMBER", 2)},
    "graveborn": {"skin": ("graveborn.GSKIN", 3), "hair": ("graveborn.BONE", 3), "plate": ("graveborn.GIRON", 4),
                  "mail": ("graveborn.RUST", 3), "padding": ("graveborn.ROT", 3), "leather": ("graveborn.MOSS", 3),
                  "trim": ("graveborn.GILT", 3), "accent": ("graveborn.VIOLET", 2)},
    "wildmoot": {"skin": ("wildmoot.PIG", 3), "hair": ("wildmoot.FUR", 2), "plate": ("wildmoot.BONE", 3),
                 "mail": ("wildmoot.GREYFUR", 3), "padding": ("wildmoot.HIDE", 3), "leather": ("handlib.LEATH", 2),
                 "trim": ("wildmoot.HORN", 3), "accent": ("wildmoot.OCHRE", 2)},
    "tidecallers": {"skin": ("build_tc_admiral.PALE", 3), "hair": ("build_tc_admiral.HAIR", 2), "plate": ("tidecallers.BRONZE", 3),
                    "mail": ("tidecallers.BARN", 2), "padding": ("build_tc_raider.NAVY", 3), "leather": ("handlib.LEATH", 2),
                    "trim": ("tidecallers.WARM", 3), "accent": ("tidecallers.SEA", 2)},
}
MAT = PRESETS[HOUSE]      # EDIT (A3): a real troop keeps ONE dict here, for its House, and tunes names and bases
_R = palette.require(HOUSE, *sorted({n for n, _ in MAT.values()}))
def R(role): return _R[MAT[role][0]]           # the role's ramp, dark to light, as RGBA tuples
def B(role): return MAT[role][1]               # the role's base index
_TEAM = palette.ramps("team")                  # the team cloth ramps (cloth sheet only)

# ================================================================================================ 3. light (recipe A4)
def tone(face, x, y, w, h, base):
    """The ramp index for one texel before any pattern: face light (LIT), the lit column (lightx: the wearer's right, the
    viewer's left, is lit), and top-to-bottom light on side faces, so a tall face is lighter at the top than at the foot."""
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

def fill(role, base=None, pattern=None):
    """A painter of one material: tone() everywhere, then pattern(face, x, y, w, h) -> -1/0/+1 for its structure."""
    Rm, b0 = R(role), (B(role) if base is None else base)
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(Rm, tone(face, x, y, w, h, b0) + (pattern(face, x, y, w, h) if pattern else 0))
        return g
    return paint

# ================================================================================================ 4. materials (recipe A4)
# Patterns are STRUCTURE (a mail check, quilting rows, a fold that follows the form), never random speckle (audit finding 2).
def mail_check(face, x, y, w, h): return 0 if face in ("top", "bottom") or (x + y) % 2 == 0 else -1
def quilt_rows(face, x, y, w, h): return -1 if face not in ("top", "bottom") and y % 4 == 3 else 0
def folds(face, x, y, w, h):      # two folds on front and back below the chest, the shadow side of each fold one down
    return -1 if face in ("front", "back") and y >= 3 and w >= 6 and x in (w // 3, 2 * w // 3) else 0

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

def rimmed(painter, role="trim"):
    """Tier III: a trim-metal top rim on every side face of a part (adds a bright mass, not just a stripe)."""
    def paint(face, w, h):
        g = painter(face, w, h)
        if face not in ("top", "bottom"):
            T = R(role)
            for x in range(w): g[0][x] = T[min(len(T) - 1, B(role) + 1 + (face in ("front", "right")))]
        return g
    return paint

# ================================================================================================ 5. parts (recipe A4-A6)
def head():
    """EDIT (A5, personality): the face is the first thing a player reads. Brows, a scar, a beard, warpaint, a hollow eye."""
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
                    g[y][x] = clamp(Hh, tone(face, x, y, w, h, B("hair")))
                else:
                    g[y][x] = clamp(S, tone(face, x, y, w, h, s))
                if back == 3 and y in (3, 4): g[y][x] = S[s - 1]                                            # the ear
        if face == "back":                                     # the back continues the front: the nape shows below the hair
            for x in range(w): g[h - 1][x] = S[s - 1]
        return g
    return paint

def torso(tier):
    """Padded gambeson with quilting rows at tier I; from tier II mail shows below the breastplate (its own box)."""
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
    """A padded sleeve to the wrist, a leather glove; light from the top of the limb down (the whole 12-row limb)."""
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
    """EDIT (A6): tier II an open cap with a nasal, tier III the same cap trim-rimmed. Bare head at tier I."""
    cap = plate(piping="accent")
    return cap if tier == 2 else rimmed(cap)

def pauldron(tier, part):
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
def bones():
    """Bones and boxes. Keep the biped skeleton (handlib.limb joints, hand bones, arms_folded); add parts per tier with
    stages=[...]. Every tier step must ADD MASS that reads at 16 blocks (a plate, a lame, a cape), not only a stripe."""
    Bn = [Bone("root", "")]
    Bn.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), per_tier(torso))]))
    Bn.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), head())]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):
        Bn += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm(), f"{side}_forearm", ELBOW[side])
        Bn.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    Bn.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):
        Bn += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg(side), f"{side}_shin", KNEE)
    Bn.append(Bone("belt", "body", boxes=[Box((-4, -7, -2), (8, 2, 4), belt(), dil=0.4)]))
    Bn.append(Bone("tabard", "body", team=True, boxes=[                     # the engine swings a bone named "tabard"
        Box((-3, -5, -2.6), (6, 8, 0), "tabard_front", sheet="cloth"), Box((-3, -5, 2.6), (6, 8, 0), "tabard_back", sheet="cloth")]))
    # ---- tier II: breastplate, pauldrons, open helm, vambraces, knee cops (new value masses) ----        EDIT (A6)
    Bn.append(Bone("breastplate", "body", stages=[2, 3], boxes=[
        Box((-4, -12, -2), (8, 6, 4), per_tier(lambda t: plate(piping="accent") if t == 2 else rimmed(plate(piping="accent")), (2, 3)), dil=0.3)]))
    Bn.append(Bone("helm", "head", tag="helm", stages=[2, 3], boxes=[
        Box((-4.5, -8.5, -4.5), (9, 4, 9), per_tier(helm, (2, 3))),
        Box((-0.5, -5, -4.9), (1, 3, 1), plate(base=B("plate") + 1))]))   # the nasal
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        Bn.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2, 0), stages=[2, 3], rot=(0, 0, 14 * c), boxes=[
            Box((-3.5, -1.5, -3.5), (7, 2, 7), per_tier(lambda t: pauldron(t, "dome"), (2, 3))),
            Box((-3.5 + 0.5 * c, 0.5, -3), (7, 2, 6), per_tier(lambda t: pauldron(t, "lame"), (2, 3)), dil=0.15)]))
        Bn.append(Bone(f"{side}_vambrace", f"{side}_forearm", stages=[2, 3], boxes=[   # forearm frame = arm frame less the elbow
            Box((-2.5 if c < 0 else -1.5, 0, -2.5), (4, 3, 5), per_tier(lambda t: plate() if t == 2 else rimmed(plate()), (2, 3)), dil=0.1)]))
        Bn.append(Bone(f"{side}_knee", f"{side}_shin", stages=[2, 3], boxes=[
            Box((-2.5, -1, -3), (5, 2, 2), plate(base=B("plate") + 1))]))
        # ---- tier III: a second pauldron lame, greaves, trim rims, crest and cape ----              EDIT (A6)
        Bn.append(Bone(f"{side}_lame2", f"{side}_arm", (c, -2, 0), stages=[3], rot=(0, 0, 14 * c), boxes=[
            Box((-3 + 1.0 * c, 2.5, -2.5), (6, 2, 5), pauldron(3, "lame"), dil=0.2)]))
        Bn.append(Bone(f"{side}_greave", f"{side}_shin", stages=[3], boxes=[
            Box((-2.5, 0.5, -2.5), (5, 3, 5), rimmed(plate()), dil=0.05)]))
    Bn.append(Bone("crest", "head", team=True, tag="helm", stages=[3], boxes=[Box((-1, -11, -4), (2, 2.5, 8), "crest", sheet="cloth")]))
    Bn.append(Bone("cape", "body", (0, -12, 2.9), team=True, stages=[3], rot=(8, 0, 0), boxes=[
        Box((-4, 0, 0), (8, 15, 1), "cape", sheet="cloth")]))
    return Bn

NOTE = "TEMPLATE: not a troop. EDIT (A2): one line of identity, House, mark and tier steps for art/troops/<tid>.json."
TEMPLATE = Troop(TID, SKIN, CLOTH, bones, cloth_painter,
                 {"weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "standard"})   # EDIT (A2): weapon id etc.
def build(): return TEMPLATE.build()
def outputs(): return TEMPLATE.outputs()

if __name__ == "__main__":
    if TID == "template" and len(sys.argv) > 1 and sys.argv[1] in ("install", "stub"):
        sys.exit("the template is never installed: copy it to art/hand/<tid>/build_<tid>.py first (recipe A2)")
    import house_stub   # a real troop: house_stub.main(TROOP, NOTE, sys.argv), which also writes the stub on `install`
    house_stub.main(TEMPLATE, NOTE, sys.argv)
