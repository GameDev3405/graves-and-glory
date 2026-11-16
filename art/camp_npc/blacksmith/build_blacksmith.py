#!/usr/bin/env python3
"""The forge Blacksmith, a camp NPC (lobby NPC spec 4.1, docs/superpowers/specs/2026-11-16-lobby-npcs-design.md).

An Ironclad smith drawn by CampNpcRenderer in place of the forge villager: big and burly, chunky and exaggerated (Kort's
style call): a barrel chest, a heavy head on no neck, huge forearms, big fists and boots. A scorched leather apron over a
bare, scarred torso and arms; the left forearm is a riveted brass and redstone graft; a ginger beard with a braid tied by
an iron ring. Props: a sledge hammer (right hand), tongs (left hand), an anvil on an oak stump with a glowing billet.

One texture, one texel per model pixel, painted only from the locked Ironclad palette (art/palettes/ironclad.json).
The model is 32 px tall and the game draws it at the campNpcs row's scale (1.3), so he stands about 2.6 blocks.
Axes are the knight rig's (Bedrock geometry): y up, ground 0, front -z, his right -x.

    python art/camp_npc/blacksmith/build_blacksmith.py            writes the geo and the texture into the assets
    python art/camp_npc/blacksmith/build_blacksmith.py <dir>      writes them into <dir> instead (previews)

Writes assets/gravesandglory/geo/camp_npc/blacksmith.geo.json and textures/entity/camp_npc/blacksmith.png. Both are
generated: change this builder, never the files. The clips come from build_blacksmith_anims.py.
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
from PIL import Image                                  # noqa: E402
from handlib import Box, LIT, lightx, clamp, blank, pack, paint_box   # noqa: E402
import palette                                         # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory")
SHEET = (128, 160)

_P = palette.require("ironclad", "handlib.SKINS.fair", "build_ic_hearthguard.BEARD", "ironclad.OILED", "ironclad.FUR",
                     "handlib.LEATH", "ironclad_tiers.IRON", "ironclad_tiers.BRASS", "ironclad_tiers.EMBER", "handlib.GOLD",
                     "build_ic_ballista.OAK")
SKIN, BEARD, APRON, CLOTH = _P["handlib.SKINS.fair"], _P["build_ic_hearthguard.BEARD"], _P["ironclad.OILED"], _P["ironclad.FUR"]
LEATH, IRON, BRASS, EMBER = _P["handlib.LEATH"], _P["ironclad_tiers.IRON"], _P["ironclad_tiers.BRASS"], _P["ironclad_tiers.EMBER"]
GOLD, OAK = _P["handlib.GOLD"], _P["build_ic_ballista.OAK"]


# ------------------------------------------------------------------------------------------------ light and materials
def tone(face, x, y, w, h, base):
    """The ramp index of one texel before any detail: face light, the lit column (his right is lit) and top-to-bottom light."""
    v = base + LIT[face]
    if face in ("top", "bottom"): return v
    v += lightx(face, x, w)
    if h >= 6:
        t = y / (h - 1)
        v += 1 if t < 0.17 else (-1 if t > 0.75 else 0)
    elif h >= 3:
        if y == 0: v += 1
        elif y == h - 1: v -= 1
    return v


def mat(ramp, base, deco=None):
    """A painter of one material. deco(face, x, y, w, h, v) may return None (keep the lit tone v), an int (a tone of the same
    ramp), (ramp, tone) for another material, or "clear" for a transparent texel."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = tone(face, x, y, w, h, base)
                d = deco(face, x, y, w, h, v) if deco else None
                if d == "clear": continue
                if d is None: g[y][x] = clamp(ramp, v)
                elif isinstance(d, int): g[y][x] = clamp(ramp, d)
                else: g[y][x] = clamp(d[0], d[1])
        return g
    return paint


def side(face): return face in ("front", "back", "left", "right")


# ------------------------------------------------------------------------------------------------ the head
def head_deco(face, x, y, w, h, v):
    """A 12 x 10 x 11 head: bald on top, a ring of hair round the back, heavy brows (their own cubes) over small hard eyes."""
    hair = lambda: (BEARD, 1 + (x + y // 3) % 2)
    if face in ("front", "left", "right", "back") and y == 1:           # a sweat rag tied round the brow
        if face == "back" and x in (5, 6): return (LEATH, 1)             # its knot
        return (LEATH, 3 if (x + (face == "front")) % 4 == 0 else 2)
    if face == "front":
        if y >= 7: return (BEARD, 1)                              # under the beard
        if y == 4 and x in (4, 7): return (IRON, 0)               # pupils, close under the brows
        if y == 4 and x in (3, 8): return (GOLD, 6)               # the whites
        if y == 4: return v - 1                                   # the brow's shadow
        if y in (2, 3) and x in (5, 6): return v - 1              # the frown crease
        if y == 6 and x in (2, 3, 8, 9): return (EMBER, 4)        # forge-flushed cheeks
        if y == 0 and x in (7, 8) or y == 1 and x == 8: return v - 1   # a soot smudge
        return None
    if face in ("left", "right"):
        back = x if face == "right" else w - 1 - x                # 0 at the back of the skull
        if y >= 6: return hair()                                  # the sideburn runs into the beard
        if y >= 4 and back <= 7: return hair()
        if y == 3 and back <= 5: return (BEARD, 2)
        return None
    if face == "back":
        if y >= 4: return hair()
        if y == 3 and x not in (0, w - 1): return (BEARD, 2)
        if y == 1 and x in (3, 4, 5): return v + 1                # the bald crown shines
        return None
    if face == "top":
        if x in (2, 3, 4) and y in (6, 7): return v + 1
        if (x, y) in ((8, 3), (9, 3), (9, 4), (5, 8), (6, 9), (3, 2)): return v - 1   # soot
        if y == 0: return v - 1
    return None


def beard_deco(face, x, y, w, h, v):
    if side(face): return v + (1 if x % 2 == 0 else 0) - (1 if y == h - 1 else 0)   # strands
    return None


def moustache_deco(face, x, y, w, h, v):
    if face == "front":
        if y == 0 and 2 <= x <= 7: return v + 1                   # the moustache under the nose
        if y == 2 and x in (4, 5): return (BEARD, 0)              # the mouth, lost in it
    return beard_deco(face, x, y, w, h, v)


def braid_deco(face, x, y, w, h, v):
    if side(face): return v + (1 if (x + y) % 2 == 0 else -1)     # the plait
    return None


# ------------------------------------------------------------------------------------------------ the body
def chest_deco(face, x, y, w, h, v):
    strap = (LEATH, 3)
    if face == "front":
        if x in (2, 3) or x in (w - 4, w - 3): return strap if y < 5 else None   # apron straps over the shoulders
        if y == h - 1 and 4 <= x <= w - 5: return v - 1           # the pecs' under-shadow (mostly behind the bib)
    if face == "top":
        if x in (2, 3) or x in (w - 4, w - 3): return strap
    if face == "back":
        if x in (2, 3) and y < 2 or x in (w - 4, w - 3) and y < 2: return strap
        d = y - 1                                                 # the straps cross on the back
        if y >= 2 and (x - 3 == d or x - 4 == d or (w - 4) - x == d or (w - 5) - x == d): return strap
        if x in (8, 9) and y >= 4: return v - 1                   # the spine
        if (x, y) in ((12, 3), (13, 4), (14, 5)): return v + 1    # an old scar
    return None


def belly_deco(face, x, y, w, h, v):
    if face == "back":
        if x in (7, 8): return v - 1                              # the spine
        if y == 0 and x in (6, 9): return (LEATH, 3)              # where the straps meet
    return None


def belt_deco(face, x, y, w, h, v):
    if face == "back" and x in (7, 8): return (BRASS, 3 if y == 0 else 2)        # the apron's tie ring
    if face in ("left", "right") and x in (3, 6): return v - 1                    # tool loops
    return None


def apron_deco(skirt):
    def deco(face, x, y, w, h, v):
        if face != "front": return None
        edge = x in (0, w - 1) or (not skirt and y == 0) or (skirt and y == h - 1)
        if edge: return v - 1                                     # the turned hem
        if (x in (1, w - 2) and y % 2 == 0) or (not skirt and y == 1 and x % 2 == 0): return v + 1   # stitches
        if skirt:
            if (x, y) in ((3, 6), (4, 6), (3, 7), (9, 3), (10, 3), (10, 4), (6, 8)): return 0          # scorch marks
            if (x, y) in ((4, 7), (9, 4), (7, 8)): return (EMBER, 2)                                    # still singed at the rim
            if y == 2 and 2 <= x <= w - 3: return v - 1          # the belt's crease
            if 4 <= x <= 9 and y in (4, 5) and y == 4: return v + 1   # the pocket's lip
            if (x, y) in ((4, 5), (9, 5)): return (BRASS, 3)     # pocket rivets
        else:
            if (x, y) in ((7, 4), (8, 4), (8, 5), (3, 6)): return 0
            if (x, y) in ((7, 5),): return (EMBER, 2)
            if (x, y) in ((1, 1), (w - 2, 1)): return (BRASS, 3)  # strap rivets
        return None
    return deco


def arm_deco(right):
    def deco(face, x, y, w, h, v):
        if not side(face): return None
        if right and face == "right" and (x, y) in ((1, 2), (2, 3), (3, 4)): return v + 1     # a long scar on the outside
        if right and face == "front" and (x, y) in ((4, 5), (3, 6)): return v + 1
        if not right and face == "left" and (x, y) in ((4, 1), (5, 2)): return v + 1
        if not right and y == h - 1: return (BRASS, 2 if x % 3 else 4)                         # the graft's socket ring
        return None
    return deco


def shoulder_deco(face, x, y, w, h, v):
    if face == "top" and x in (2, 3) and y in (2, 3): return v + 1                              # a sweaty shine
    return None


def forearm_r_deco(face, x, y, w, h, v):
    if not side(face): return None
    if y >= h - 3: return (LEATH, (2 if y == h - 3 else 1) + (1 if x % 3 == 0 else 0))          # the wrist wrap
    if face == "right" and (x, y) in ((2, 1), (3, 2), (4, 3), (6, 1), (6, 2)): return v + 1     # burn scars
    if face == "front" and (x, y) in ((2, 3), (3, 4), (5, 1)): return v + 1
    if face == "back" and (x, y) in ((3, 2), (4, 3)): return v - 1                              # soot
    return None


def graft_deco(face, x, y, w, h, v):
    """The left forearm: riveted brass plates around a redstone channel that runs down its outside and front."""
    if face in ("top", "bottom"): return (IRON, 2)
    if y in (0, h - 1): return (BRASS, 4) if x % 3 != 1 else (IRON, 1)                          # rims and their rivets
    if y == 4: return v - 1                                                                     # the seam between plates
    chan = (face == "left" and x in (3, 4)) or (face == "front" and x in (5, 6))
    if chan and 1 <= y <= 6: return (EMBER, 4 if y in (2, 3) else 3)                            # the redstone core
    near = (face == "left" and x in (2, 5)) or (face == "front" and x in (4, 7))
    if near and 1 <= y <= 6: return (IRON, 1)                                                   # its iron housing
    if face == "back" and (x, y) in ((2, 2), (5, 2), (2, 6), (5, 6)): return (IRON, 1)          # bolts
    if face == "right" and (x, y) in ((3, 2), (4, 2)): return (EMBER, 3)                        # a gauge slit on the inside
    return None


def fist_deco(face, x, y, w, h, v):
    if face in ("front", "back") and y == 2 and x % 2 == 1: return v - 1                        # finger gaps
    if face == "bottom": return v
    return None


def glove_deco(face, x, y, w, h, v):
    if side(face) and y == 0: return (BRASS, 3 if x % 2 else 2)                                 # the cuff's brass edge
    if face in ("front", "back") and y == 2 and x % 2 == 1: return v - 1
    return None


def thigh_deco(face, x, y, w, h, v):
    if face == "front" and (x, y) in ((2, 3), (3, 3), (2, 4)): return v - 1                     # a knee patch
    return None


def shin_deco(face, x, y, w, h, v):
    if not side(face): return None
    if y == 0: return (LEATH, 3)                                                                # the boot's turned top
    if y == 2: return (LEATH, 1)                                                                # a strap
    if y == 2 and False: return None
    if face in ("left", "right") and y == 2 and x == 2: return (BRASS, 3)
    return None


def boot_deco(face, x, y, w, h, v):
    if side(face) and y == h - 1: return (IRON, 0)                                              # the sole
    if face == "front": return (IRON, 3 if y == 0 else 2)                                       # an iron toe cap
    if face == "top" and y >= 6: return (IRON, 4 if y == 8 else 3)
    if face in ("left", "right"):
        toe = x if face == "left" else w - 1 - x                                                # 0 at the toe
        if toe <= 2 and y < h - 1: return (IRON, 2)
    if face == "bottom": return (IRON, 0)
    return None


# ------------------------------------------------------------------------------------------------ the props
def handle_deco(face, x, y, w, h, v):
    """The hammer's short haft runs along z (10 px): a leather grip where the fist holds it."""
    if face in ("left", "right"):
        z = x if face == "left" else w - 1 - x                    # 0 at the head's end
        if 5 <= z <= 8: return (LEATH, 2 + z % 2)
    if face in ("top", "bottom"):
        z = y if face == "bottom" else h - 1 - y
        if 5 <= z <= 8: return (LEATH, 2 + z % 2)
    return None


def hammer_deco(face, x, y, w, h, v):
    if face in ("top", "bottom"): return v + 1 if 0 < x < w - 1 and 0 < y < h - 1 else v        # the worn striking faces
    if y in (2, 4): return (BRASS, 3 if face in ("front", "right") else 2)                      # two brass bands
    if y in (0, h - 1): return v + 1                                                            # mushroomed rims
    return None


def tongs_deco(face, x, y, w, h, v):
    if face in ("top", "bottom") and x == 0: return v - 1                                       # the two reins
    return None


def stump_deco(face, x, y, w, h, v):
    if face == "top":
        r = max(abs(x - 4), abs(y - 4))
        return 4 if r in (1, 3) else 3                                                          # growth rings
    if side(face):
        if y == 1: return (IRON, 2 + (1 if x % 4 == 1 else 0))                                  # an iron hoop
        return v + (1 if x % 3 == 0 else 0) - 1                                                 # bark
    return None


def anvil_deco(face, x, y, w, h, v):
    if face == "top":
        if y in (0, h - 1) or x in (0, w - 1): return v + 1                                     # the bright worn edge
        if (x, y) in ((9, 2), (10, 2)): return v - 2                                            # the hardy hole
        return v
    if side(face) and y == 0: return v + 1
    return None


def billet_deco(face, x, y, w, h, v):
    """Glowing at his right (where the hammer lands), cooling to dark iron at the tongs."""
    hot = {"top": x, "bottom": x, "front": x, "back": w - 1 - x}.get(face)
    if face == "right": return (GOLD, 6)
    if face == "left": return (IRON, 2)
    if hot is None: return None
    return [(GOLD, 6), (GOLD, 5), (GOLD, 4), (EMBER, 3), (EMBER, 2), (IRON, 2), (IRON, 2)][min(hot, 6)]


# ------------------------------------------------------------------------------------------------ the model
SK = lambda deco=None: mat(SKIN, 3, deco)


def bones():
    """[(name, parent, pivot, [cube])], cube = (origin, size, painter[, inflate[, (pivot, rotation)]]), parents first."""
    B = [("root", None, [0, 0, 0], [])]
    B.append(("hips", "root", [0, 12, 0], [
        ([-7, 9, -4], [14, 4, 8], mat(CLOTH, 3)),
        ([-8, 12, -5], [16, 2, 10], mat(LEATH, 3, belt_deco), 0.25)]))
    B.append(("apron", "hips", [0, 14, -6], [([-7, 3, -6.5], [14, 11, 1], mat(APRON, 2, apron_deco(True)))]))
    B.append(("torso", "hips", [0, 13, 0], [
        ([-8, 13, -5], [16, 5, 10], SK(belly_deco)),
        ([-9, 17, -5.5], [18, 7, 11], SK(chest_deco)),
        ([-6, 14, -6.5], [12, 9, 1], mat(APRON, 2, apron_deco(False)))]))
    B.append(("head", "torso", [0, 23, 0], [
        ([-6, 22, -6], [12, 10, 11], SK(head_deco)),
        ([-5, 28, -7], [4, 2, 1], mat(BEARD, 1), 0, ([-3, 29, -6.5], [0, 0, -12])),      # heavy brows, drawn down to the nose
        ([1, 28, -7], [4, 2, 1], mat(BEARD, 1), 0, ([3, 29, -6.5], [0, 0, 12])),
        ([-1, 25, -8], [2, 3, 2], mat(SKIN, 4)),                                          # a big nose
        ([-7, 25, -1], [1, 3, 2], SK()), ([6, 25, -1], [1, 3, 2], SK())]))                # ears
    B.append(("beard", "head", [0, 25, -6], [
        ([-5, 21, -9], [10, 4, 4], mat(BEARD, 2, moustache_deco)),
        ([-4, 18, -9], [8, 3, 3], mat(BEARD, 2, beard_deco))]))
    B.append(("braid", "beard", [0, 18, -8], [
        ([-2, 12, -9], [4, 6, 2], mat(BEARD, 2, braid_deco)),
        ([-2.5, 14, -9.5], [5, 1, 3], mat(IRON, 4))]))                                    # the iron ring
    for s, right in ((-1, True), (1, False)):
        n = "right" if right else "left"
        x = lambda lo, w: (lo if right else -lo - w)                                      # mirror an x extent to the left side
        B.append((n + "_arm", "torso", [s * 12.5, 22, 0], [
            ([x(-15.5, 6), 16, -3.5], [6, 7, 7], SK(arm_deco(right))),
            ([x(-16, 7), 20, -4], [7, 4, 8], SK(shoulder_deco))]))
        fore = SK(forearm_r_deco) if right else mat(BRASS, 2, graft_deco)
        B.append((n + "_forearm", n + "_arm", [s * 12.5, 16.5, 0], [([x(-16.5, 8), 9, -4], [8, 8, 8], fore)]))
        hand = SK(fist_deco) if right else mat(LEATH, 2, glove_deco)
        B.append((n + "_hand", n + "_forearm", [s * 12.5, 9, 0], [([x(-15.5, 6), 5, -3], [6, 4, 6], hand)]))
    B.append(("hammer", "right_hand", [-12.5, 7, 0], [
        ([-13.5, 6, -7.5], [2, 2, 10], mat(OAK, 3, handle_deco)),
        ([-15, 3.5, -10.5], [5, 7, 4], mat(IRON, 3, hammer_deco))]))
    B.append(("tongs", "left_hand", [12.5, 7, 0], [
        ([11.5, 6.5, -14], [2, 1, 13], mat(IRON, 2, tongs_deco)),
        ([11, 6, -15], [3, 2, 2], mat(IRON, 3))]))
    for s, right in ((-1, True), (1, False)):
        n = "right" if right else "left"
        x = lambda lo, w: (lo if right else -lo - w)
        B.append((n + "_leg", "hips", [s * 4, 12, 0], [([x(-7, 6), 6, -3], [6, 6, 6], mat(CLOTH, 3, thigh_deco))]))
        B.append((n + "_shin", n + "_leg", [s * 4, 7, 0], [([x(-6.5, 5), 2, -2.5], [5, 6, 5], mat(LEATH, 2, shin_deco))]))
        B.append((n + "_foot", n + "_shin", [s * 4, 2, 0], [([x(-7.5, 7), 0, -5.5], [7, 3, 9], mat(LEATH, 1, boot_deco))]))
    B.append(("anvil", "root", [0, 0, -15], [
        ([-4.5, 0, -19.5], [9, 5, 9], mat(OAK, 2, stump_deco)),
        ([-4, 5, -18.5], [8, 2, 7], mat(IRON, 2)),
        ([-2.5, 7, -17.5], [5, 2, 5], mat(IRON, 2)),
        ([-6, 9, -18], [12, 3, 6], mat(IRON, 3, anvil_deco)),
        ([6, 10, -17], [3, 2, 4], mat(IRON, 3)),                                          # the horn, at his left
        ([9, 10.5, -16], [2, 1, 2], mat(IRON, 4))]))
    B.append(("billet", "anvil", [0, 12, -15], [([-3, 12, -16], [7, 1, 2], mat(IRON, 3, billet_deco))]))
    return B


def build():
    B = bones()
    boxes, meta = [], []
    for name, parent, pivot, cubes in B:
        for c in cubes:
            b = Box(c[0], c[1], c[2], c[3] if len(c) > 3 else 0.0)
            boxes.append(b); meta.append((name, c[4] if len(c) > 4 else None))
    pack(boxes, SHEET, "blacksmith")
    img = Image.new("RGBA", SHEET, (0, 0, 0, 0))
    for b in boxes: paint_box(img, b, b.paint[0])
    out, i = [], 0
    for name, parent, pivot, cubes in B:
        js = []
        for c in cubes:
            b, (_, rot) = boxes[i], meta[i]; i += 1
            j = {"origin": list(b.origin), "size": list(b.size), "uv": list(b.uv)}
            if b.dil: j["inflate"] = b.dil
            if rot: j["pivot"], j["rotation"] = list(rot[0]), list(rot[1])
            js.append(j)
        bone = {"name": name, "pivot": list(pivot)}
        if parent: bone["parent"] = parent
        if js: bone["cubes"] = js
        out.append(bone)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
        "identifier": "geometry.gravesandglory.camp_npc_blacksmith", "texture_width": SHEET[0], "texture_height": SHEET[1],
        "visible_bounds_width": 4, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.25, 0]}, "bones": out}]}
    return geo, img


def main():
    geo, img = build()
    if len(sys.argv) > 1:
        os.makedirs(sys.argv[1], exist_ok=True)
        gp, tp = os.path.join(sys.argv[1], "blacksmith.geo.json"), os.path.join(sys.argv[1], "blacksmith.png")
    else:
        gp = os.path.join(ASSETS, "geo", "camp_npc", "blacksmith.geo.json")
        tp = os.path.join(ASSETS, "textures", "entity", "camp_npc", "blacksmith.png")
    with open(gp, "w", newline="\n") as f: f.write(json.dumps(geo, indent=1) + "\n")
    img.save(tp)
    print("wrote", gp, "and", tp)


if __name__ == "__main__":
    main()
