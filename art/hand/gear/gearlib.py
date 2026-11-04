"""The commander gear frame (docs/superpowers/plans/2026-11-03-gear-art.md, Task 1), on the troop hand library.

A GearPiece is one worn item: bones parented to the six biped bones (head, body, right_arm, left_arm, right_leg,
left_leg), one skin sheet per rarity (1 Common to 5 Legendary, the gearRarities order), one cloth sheet per side and an
optional glow sheet. A bone's `stages` are the rarities it shows at, so a Legendary can change the silhouette. Pieces
are drawn at one texel per model pixel and shown worn on the mannequin for contact sheets.
"""
import copy, json, os, sys
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403  ramps, painters, Box and Bone, packing
from handlib import on, pack, paint_box, SIDES, TEAM
import render

RARITIES = (1, 2, 3, 4, 5)
RARITY_NAMES = ("common", "uncommon", "rare", "epic", "legendary")
BORDERS = {1: "9d9d9d", 2: "3fae49", 3: "3d7fd6", 4: "9a4fd6", 5: "e8892b"}   # gearRarities.border
BIPED = ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")

def ids():
    with open(os.path.join(HERE, "ids.json")) as f: return json.load(f)

def per_rarity(f, rarities=RARITIES): return {r: f(r) for r in rarities}

class GearPiece:
    """bones() -> [Bone] (names unique within the piece, top parents in BIPED); cloth_painter(spec, side) -> painter for
    a cloth box's paint spec; glow(bone, box, rarity) -> painter or None paints the lit texels of that rarity."""
    def __init__(s, item_id, slot, bones, cloth_painter=None, skin=(64, 64), cloth=(64, 32), glow=None):
        s.item_id, s.slot, s.bones, s.cloth_painter, s.skin, s.cloth, s.glow = item_id, slot, bones, cloth_painter, tuple(skin), tuple(cloth), glow

    def build(s):
        B = s.bones()
        sk = [b for bone in B for b in bone.boxes if b.sheet == "skin"]
        cl = [b for bone in B for b in bone.boxes if b.sheet == "cloth"]
        pack(sk, s.skin)
        if cl: pack(cl, s.cloth)
        skins = {}
        for r in RARITIES:
            img = Image.new("RGBA", s.skin, (0, 0, 0, 0))
            for bone in B:
                if not on(bone, r): continue
                for b in bone.boxes:
                    if b.sheet != "skin": continue
                    p = b.paint.get(r, b.paint.get(0))
                    if p: paint_box(img, b, p)
            skins[r] = img
        cloths = {}
        for side in SIDES:
            img = Image.new("RGBA", s.cloth, (0, 0, 0, 0))
            for b in cl: paint_box(img, b, s.cloth_painter(b.paint[0], side))
            cloths[side] = img
        glows = {}
        if s.glow:
            for r in RARITIES:
                img = Image.new("RGBA", s.skin, (0, 0, 0, 0))
                for bone in B:
                    if not on(bone, r): continue
                    for b in bone.boxes:
                        p = s.glow(bone, b, r) if b.sheet == "skin" else None
                        if p: paint_box(img, b, p)
                if img.getbbox(): glows[r] = img
        return B, skins, cloths, glows

    def model_json(s, B, glows):
        base = "gravesandglory:textures/entity/gear/%s/" % s.item_id
        out = {"format": 1, "item": s.item_id, "slot": s.slot, "density": 1, "size": list(s.skin), "cloth": base + "team_cloth_",
               "skins": {str(r): base + "%s_r%d.png" % (s.item_id, r) for r in RARITIES},
               "glow": {str(r): base + "%s_e%d.png" % (s.item_id, r) for r in glows}, "bones": []}
        for bone in B:
            out["bones"].append({"name": bone.name, "parent": bone.parent, "team": bone.team, "tag": bone.tag, "stages": bone.stages,
                                 "sheet": list(s.cloth if bone.team else s.skin), "pivot": bone.pivot, "rotation": bone.rot,
                                 "boxes": [{"origin": b.origin, "size": b.size, "uv": b.uv, "mirror": False, "dilation": [b.dil] * 3} for b in bone.boxes]})
        return out

    def outputs(s):
        B, skins, cloths, glows = s.build()
        d = f"textures/entity/gear/{s.item_id}/"
        out = {d + f"{s.item_id}_r{r}.png": skins[r] for r in RARITIES}
        if any(b.sheet == "cloth" for bone in B for b in bone.boxes):
            out.update({d + f"team_cloth_{side}.png": cloths[side] for side in SIDES})
        out.update({d + f"{s.item_id}_e{r}.png": g for r, g in glows.items()})
        out[f"models/gear/{s.item_id}.json"] = s.model_json(B, glows)
        return out

# ---- worn on the mannequin ---------------------------------------------------------------------
def worn(pieces, rarity, side, built=None):
    """(bones, visible, sheets) for render.render: the mannequin plus each piece at that rarity, the glow laid over."""
    import mannequin
    bones = mannequin.bones(); sheets = {"skin": mannequin.skin(), "cloth": mannequin.skin()}
    names = set()
    for p in pieces:
        B, skins, cloths, glows = (built or {}).get(p.item_id) or p.build()
        sk = skins[rarity]
        if rarity in glows: sk = Image.alpha_composite(sk, glows[rarity])
        sheets[p.item_id + ":skin"], sheets[p.item_id + ":cloth"] = sk, cloths[side]
        for bone in B:
            nb = copy.copy(bone); nb.boxes = []
            nb.name = p.item_id + "." + bone.name
            nb.parent = bone.parent if bone.parent in BIPED or bone.parent == "root" else p.item_id + "." + bone.parent
            for b in bone.boxes:
                c = copy.copy(b); c.sheet = p.item_id + ":" + b.sheet; nb.boxes.append(c)
            if on(bone, rarity): names.add(nb.name)
            bones.append(nb)
    base = {b.name for b in mannequin.bones()}
    return bones, (lambda bn: bn.name in base or bn.name in names), sheets

VIEWS = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
BG = (40, 42, 50, 255)

def row(pieces, rarity, side, px=6, built=None, far=True):
    bones, vis, sheets = worn(pieces, rarity, side, built)
    tiles = [render.render(bones, vis, sheets, y, p, px=px) for y, p in VIEWS]
    if far: tiles.append(render.render(bones, vis, sheets, -35, 12, px=2))
    return render.sheet_of(tiles)

def label(img, text, colour):
    from PIL import ImageDraw
    out = Image.new("RGBA", (img.width + 150, img.height), BG); out.paste(img, (150, 0))
    d = ImageDraw.Draw(out); d.rectangle([8, 12, 14, img.height - 12], fill=hx(colour)); d.text((22, img.height // 2 - 6), text, fill=(230, 226, 214, 255))
    return out

def stack(rows_):
    W = max(r.width for r in rows_); img = Image.new("RGBA", (W, sum(r.height for r in rows_)), BG); y = 0
    for r in rows_: img.paste(r, (0, y)); y += r.height
    return img

def contact(pieces, out, rarities=RARITIES, sides=("crimson", "teal"), px=6, extra_rows=()):
    """One row per rarity on the first side, the top rarity again on every other side, then extra (label, pieces, rarity) rows."""
    built = {p.item_id: p.build() for p in pieces}
    rows_ = [label(row(pieces, r, sides[0], px, built), f"{RARITY_NAMES[r - 1].title()} / {sides[0]}", BORDERS[r]) for r in rarities]
    for sd in sides[1:]:
        r = rarities[-1]; rows_.append(label(row(pieces, r, sd, px, built), f"{RARITY_NAMES[r - 1].title()} / {sd}", BORDERS[r]))
    for text, ps, r in extra_rows:
        rows_.append(label(row(ps, r, sides[0], px, {p.item_id: built.get(p.item_id) or p.build() for p in ps}), text, BORDERS[r]))
    img = stack(rows_); os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True); img.save(out); print("contact sheet", out)
    return img

def pieces_close(pieces, out, rarities, side="crimson", px=10):
    """Each piece alone on the mannequin's frame (the mannequin hidden), front / three-quarter / back, so the paint reads texel by texel."""
    import mannequin
    base = {b.name for b in mannequin.bones()}
    rows_ = []
    for r in rarities:
        tiles = []
        for p in pieces:
            bones, vis, sheets = worn([p], r, side)
            tiles += [render.render(bones, lambda bn: vis(bn) and bn.name not in base, sheets, y, pt, px=px) for y, pt in ((0, 0), (-35, 12), (180, 0))]
        rows_.append(label(render.sheet_of(tiles), RARITY_NAMES[r - 1].title(), BORDERS[r]))
    img = stack(rows_); img.save(out); print("close sheet", out)
