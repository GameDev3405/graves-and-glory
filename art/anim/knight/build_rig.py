"""The knight's GeckoLib rig (animation rebuild, docs/anim-rebuild/PLAN.md).

One geometry holds every bone of every tier, face and gear piece; the client hides what a troop does not wear (GeoBone
setHidden) using knight.rig.json. Bones are authored in handlib's Java frame (Y down, 24 = ground, -x = wearer's right,
-z = front) so the hand-painted art and its box UVs carry over, then written as Bedrock geometry (Y up, ground 0).

Skeleton: root (ground) > hips > torso > head; torso > arm > forearm > hand > sword_t1..3; hips > leg > shin > foot.
The body is split at the waist so the chest can twist and lean over planted legs, and the feet are their own bones so a
planted foot can stay flat while the shin swings.

Texture per tier and side: the tier's skin sheet (128x128) over the side's cloth sheet (128x64) over the swords' sheets.

    python art/anim/knight/build_rig.py A|B [out dir]
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, "..", ".."))
for p in ("hand/common", "hand/knight", "hand/weapons", "anim/knight"):
    sys.path.insert(0, os.path.join(ART, p))
from PIL import Image                                         # noqa: E402
import handlib as H                                           # noqa: E402
from handlib import Box, Bone, limb, pack, paint_box, on, SIDES, ELBOW, KNEE, HAND, per_tier  # noqa: E402
import build_knight as K                                      # noqa: E402
import build_swords as SW                                     # noqa: E402

SKIN, CLOTH = (128, 128), (128, 64)
SWORD_Y = SKIN[1] + CLOTH[1]                                  # the sword sheets start under the cloth
TIERS = (1, 2, 3)
SWORDS = {1: SW.tier1, 2: SW.tier2, 3: SW.tier3}
SWORD_ROT = (-90, 0, 90)      # Java-frame degrees: blade forward out of a hanging fist, edges up and down (checked on render_geo)
# gripped in the left fist by its strap, face to the front, boss over the knuckles and a little inside them (the hand bone is
# turned by build_anims so the shield stays upright whatever the arm does)
SHIELD_AT, SHIELD_ROT = (-1, 4, -1.5), (0, 0, 0)


def lined(paint):
    """A plate painter whose underside is its leather lining."""
    def p(face, w, h):
        return [[H.LEATH[3]] * w for _ in range(h)] if face == "bottom" else paint(face, w, h)
    return p


def bones(P):
    """The rig in the Java frame (pivots relative to the parent). P supplies the painters (A: build_knight's, B: paint_b's)."""
    g = lambda n: getattr(P, n)
    B = [Bone("root", "", (0, 24, 0))]                        # on the ground under the feet
    # the body split at the waist: the pelvis is the body's rows 8..12 and the chest its rows 0..9 (one row of overlap so a
    # leaning chest never opens a gap; the pelvis stands 0.05 proud so the shared row is never coplanar)
    B.append(Bone("hips", "root", (0, -12, 0), [Box((-4, -4, -2), (8, 4, 4), per_tier(g("body_base")), dil=0.05, cut=(12, 8))]))
    B.append(Bone("belt", "hips", boxes=[Box((-4, -2, -2), (8, 2, 4), per_tier(g("belt")), dil=0.4)]))
    B.append(Bone("torso", "hips", (0, -3, 0), [Box((-4, -9, -2), (8, 9, 4), per_tier(g("body_base")), cut=(12, 0))]))
    B.append(Bone("head", "torso", (0, -9, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", boxes=[Box((-4, -8, -4), (8, 8, 8), g("face_painter")(k))], variants=[k]))
    B.append(Bone("helm_squire", "head", tag="helm", stages=[1], boxes=[
        Box((-4.5, -8.5, -4.5), (9, 9, 9), H.with_faces(H.mail(base=3), front=g("coif_front"))),
        Box((-5, -9, -5), (10, 4, 10), g("spangen_cap")),
        Box((-3, -10, -3), (6, 1, 6), H.plate(base=4, spec=False)),
        Box((-0.5, -11, -0.5), (1, 1, 1), H.plate(R=H.GOLD, base=4, spec=False)),
        Box((-0.5, -5, -5.5), (1, 3, 1), H.plate(base=4, spec=False))]))
    B.append(Bone("helm_great", "head", tag="helm", stages=[2, 3], boxes=[Box((-5, -9, -5), (10, 10, 10), per_tier(g("great_helm"), (2, 3)))]))
    B.append(Bone("helm_crown", "head", tag="helm", stages=[3], boxes=[Box((-5.5, -11, -5.5), (11, 4, 11), g("crown"))]))
    B.append(Bone("crest", "head", team=True, tag="helm", stages=[3], boxes=[
        Box((-1, -12, -4), (2, 3, 9), "crest", sheet="cloth"), Box((-1, -11, 5), (2, 9, 2), "tail", sheet="cloth")]))
    # torso gear: tier I's surcoat is a shell split like the body; tiers II and III hang open panels from the chest
    B.append(Bone("surcoat_t1", "torso", team=True, stages=[1], boxes=[Box((-4, -9, -2), (8, 9, 4), "tabard1", dil=0.3, sheet="cloth", cut=(12, 0))]))
    B.append(Bone("surcoat_t1_skirt", "hips", team=True, stages=[1], boxes=[Box((-4, -4, -2), (8, 4, 4), "tabard1", dil=0.35, sheet="cloth", cut=(12, 8))]))
    for t in (2, 3):
        B.append(Bone(f"surcoat_t{t}", "torso", team=True, stages=[t], boxes=[
            Box((-3, -7, -2.3), (6, 10, 0), ("surcoat", t, False), sheet="cloth"),
            Box((-3, -7, 2.3), (6, 10, 0), ("surcoat", t, True), sheet="cloth")]))
    B.append(Bone("gorget", "torso", stages=[2, 3], boxes=[Box((-4.5, -9.5, -2.5), (9, 2, 5), per_tier(g("gorget"), (2, 3)))]))
    B.append(Bone("mantle", "torso", stages=[1], boxes=[Box((-6, -9.5, -3), (12, 3, 6), g("mantle"))]))
    B.append(Bone("cape", "torso", (0, -9, 3.6), team=True, stages=[3], rot=(8, 0, 0), boxes=[
        Box((-5, 0, 0), (10, 17, 1), "cape", sheet="cloth"), Box((-4.5, -0.5, -1.5), (9, 2, 2), "collar", sheet="cloth")]))
    # arms (shoulder, elbow, hand: the hand bone is the grip point) and legs (hip, knee, ankle)
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):
        B += limb(f"{side}_arm", "torso", (x, -7, 0), (ox, -2, -2), per_tier(g("arm")), f"{side}_forearm", ELBOW[side])
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    for side, x in (("right", -1.9), ("left", 1.9)):
        leg = limb(f"{side}_leg", "hips", (x, 0, 0), (-2, 0, -2), per_tier(g("leg")), f"{side}_shin", KNEE)
        # The shin stops 2 px above the sole, inside the boot (which covers the bottom 3 px): ending at the sole, its back corner
        # sank 0.75 px into the ground whenever the knee bent. The box keeps its top rows, so the texture is unchanged above the boot.
        leg[1].boxes[0].size[1] -= 2
        B += leg
        B.append(Bone(f"{side}_foot", f"{side}_shin", (0, 4, 0), [Box((-2.5, -1, -3), (5, 3, 5), per_tier(g("boot")))]))
    B.append(Bone("shield", "left_hand", SHIELD_AT, rot=SHIELD_ROT, tag="shield", boxes=
        [Box((x, y, -1.9), (w, h, 1), g("wood")) for (x, y), (w, h) in H.SHIELD_BODY]
        + [Box((x, y, -2.5), (w, h, 2), per_tier(g("shield_rim")), dil=0.01) for (x, y), (w, h) in H.SHIELD_RIM]
        + [Box((-1, -5, -2.7), (2, 2, 1), per_tier(g("shield_boss"))), Box((-1, -3, -0.9), (2, 5, 3), H.leather(base=2))]))
    for t in TIERS:
        B.append(Bone(f"shield_face_t{t}", "shield", team=True, stages=[t], tag="shield",
                      boxes=[Box((-6, -10, -2.1), (12, 18, 0), ("shield", t), sheet="cloth")]))
    for side in ("right", "left"):
        c = -1 if side == "right" else 1
        # The undersides show whenever the arm lifts (every attack): paint them as the plate's leather lining, not the
        # darkest steel, which the game's down-face shading turned into a black block.
        pa = lambda part: per_tier(lambda t: lined(g("pauldron")(t, part)), (2, 3))
        B.append(Bone(f"{side}_pauldron", f"{side}_arm", (c, -2, 0), stages=[2, 3], rot=(0, 0, 14 * c), boxes=[
            Box((-3.5, -1.5, -3.5), (7, 2, 7), pa("dome")),
            Box((-3.5 + 0.5 * c, 0.5, -3), (7, 2, 6), pa("lame1"), dil=0.15),
            Box((-3 + 1.0 * c, 2.5, -2.5), (6, 2, 5), pa("lame2"), dil=0.15)]))
        B.append(Bone(f"{side}_couter", f"{side}_forearm", stages=[2, 3], boxes=[
            Box((-2.6 if c < 0 else 1.6, -2, -1.5), (1, 3, 3), per_tier(g("couter"), (2, 3)))]))
        B.append(Bone(f"{side}_cuff", f"{side}_forearm", stages=[2, 3], boxes=[Box((-2.5, 1, -2.5), (5, 2, 5), per_tier(g("cuff"), (2, 3)))]))
        B.append(Bone(f"{side}_glove", f"{side}_forearm", stages=[1], boxes=[Box((-2, 2, -2), (4, 1, 4), H.leather(base=2), dil=0.3)]))
        B.append(Bone(f"{side}_knee", f"{side}_shin", stages=[2, 3], boxes=[Box((-2.5, -1, -3), (5, 2, 2), per_tier(g("knee"), (2, 3)))]))
        B.append(Bone(f"{side}_toe", f"{side}_foot", stages=[2, 3], boxes=[Box((-1.5, 1, -4), (3, 1, 1), per_tier(g("toe"), (2, 3)))]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, stages=[1], boxes=[
            Box((-2, 0, -2.4), (4, 6, 0), ("flap12", inner), sheet="cloth"), Box((-2, 0, 2.4), (4, 6, 0), ("flap12", 3 - inner), sheet="cloth")]))
        x0, inner3 = (-1, 2) if side == "right" else (-2, 0)
        for name, st, spec in (("skirt_plate", 2, "flap12"), ("skirt_gilt", 3, "flap3")):
            B.append(Bone(f"{side}_{name}", f"{side}_leg", team=True, stages=[st], boxes=[
                Box((x0, 0, -2.4), (3, 6, 0), (spec, inner3), sheet="cloth"), Box((x0, 0, 2.4), (3, 6, 0), (spec, 2 - inner3), sheet="cloth")]))
    for t in TIERS:   # the swords' cubes come from build_swords (geo())
        B.append(Bone(f"sword_t{t}", "right_hand", stages=[t], tag="weapon", rot=SWORD_ROT))
    return B


def textures(P, B):
    """Paints and packs. Returns ({tier: skin}, {side: cloth}, sword sheet, {tier: {(cube, face): uv}}, {tier: cubes})."""
    skin_boxes = [b for bn in B for b in bn.boxes if b.sheet == "skin"]
    cloth_boxes = [b for bn in B for b in bn.boxes if b.sheet == "cloth"]
    pack(skin_boxes, SKIN, "knight skin"); pack(cloth_boxes, CLOTH, "knight cloth")
    skins = {}
    for t in TIERS:
        img = Image.new("RGBA", SKIN, (0, 0, 0, 0))
        for bn in B:
            if not on(bn, t): continue
            for b in bn.boxes:
                if b.sheet != "skin": continue
                p = b.paint.get(t, b.paint.get(0))
                if p: paint_box(img, b, p)
        skins[t] = img
    cloths = {}
    for side in SIDES:
        img = Image.new("RGBA", CLOTH, (0, 0, 0, 0))
        for b in cloth_boxes: paint_box(img, b, P.cloth_painter(b.paint[0], side))
        cloths[side] = img
    sheets, uvs, cubes = [], {}, {}
    for t in TIERS:
        cubes[t] = SWORDS[t]()
        colour, _, uv = SW.pack(cubes[t])
        sheets.append(colour)
        dx = SW.SHEET_W * (t - 1)
        uvs[t] = {k: [v[0] + dx, v[1] + SWORD_Y, v[2] + dx, v[3] + SWORD_Y] for k, v in uv.items()}
    sword = Image.new("RGBA", (SKIN[0], max(s.height for s in sheets)), (0, 0, 0, 0))
    for i, s in enumerate(sheets): sword.paste(s, (SW.SHEET_W * i, 0))
    return skins, cloths, sword, uvs, cubes


def texture(skins, cloths, sword, tier, side):
    img = Image.new("RGBA", (SKIN[0], SWORD_Y + sword.height), (0, 0, 0, 0))
    img.paste(skins[tier], (0, 0)); img.paste(cloths[side], (0, SKIN[1])); img.paste(sword, (0, SWORD_Y))
    return img


def geo(B, uvs, cubes, height, keep=lambda bn: True):
    """Bedrock 1.12 geometry (Y up, ground 0). keep(bone) False writes the bone without cubes (previews of one tier/face)."""
    absp, out = {}, []
    r = lambda v: round(v, 4)
    for bn in B:
        pp = absp.get(bn.parent, [0, 0, 0])
        ap = [pp[i] + bn.pivot[i] for i in range(3)]
        absp[bn.name] = ap
        j = {"name": bn.name}
        if bn.parent: j["parent"] = bn.parent
        j["pivot"] = [r(ap[0]), r(24 - ap[1]), r(ap[2])]
        if any(bn.rot): j["rotation"] = [r(-bn.rot[0]), r(bn.rot[1]), r(-bn.rot[2])]
        cs = []
        for b in (bn.boxes if keep(bn) else ()):
            o = [ap[i] + b.origin[i] for i in range(3)]
            uv = list(b.uv) if b.sheet == "skin" else [b.uv[0], b.uv[1] + SKIN[1]]
            c = {"origin": [r(o[0]), r(24 - (o[1] + b.size[1])), r(o[2])], "size": list(b.size), "uv": uv}
            if b.dil: c["inflate"] = b.dil
            cs.append(c)
        if bn.name.startswith("sword_t") and keep(bn):   # sword frame (Y up, grip at the origin) placed at the fist
            t = int(bn.name[-1]); hx, hy, hz = j["pivot"]
            for sc in cubes[t]:
                faces = {}
                for f in SW.FACES:
                    if (sc.name, f) not in uvs[t]: continue
                    x0, y0, x1, y1 = uvs[t][(sc.name, f)]
                    faces[f] = ({"uv": [x0, y1], "uv_size": [x1 - x0, y0 - y1]} if f == "down"
                                else {"uv": [x0, y0], "uv_size": [x1 - x0, y1 - y0]})
                c = {"origin": [r(hx + sc.lo[0]), r(hy + sc.lo[1]), r(hz + sc.lo[2])],
                     "size": [r(sc.hi[i] - sc.lo[i]) for i in range(3)], "uv": faces}
                if sc.turn:
                    pv = sc.pivot if sc.pivot is not None else [(sc.lo[i] + sc.hi[i]) / 2 for i in range(3)]
                    c["pivot"], c["rotation"] = [r(hx + pv[0]), r(hy + pv[1]), r(hz + pv[2])], [0, 0, sc.turn]
                cs.append(c)
        if cs: j["cubes"] = cs
        out.append(j)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.gravesandglory.knight", "texture_width": SKIN[0], "texture_height": height,
                        "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.25, 0]},
        "bones": out}]}


def shows(bn, tier, face=0, shield=True):
    """Whether a troop of this tier and face wears the bone (the rule the client applies from knight.rig.json)."""
    if bn.stages and tier not in bn.stages: return False
    if bn.variants is not None and face not in bn.variants: return False
    return shield or bn.tag != "shield"


def rig_meta(B):
    return {"troop": "knight", "tiers": list(TIERS), "faces": 4, "bones": {
        bn.name: {k: v for k, v in (("stages", bn.stages), ("variants", bn.variants), ("team", bn.team), ("tag", bn.tag)) if v}
        for bn in B}}


def build(option):
    P = K if option == "A" else __import__("paint_b")
    B = bones(P)
    return (B,) + textures(P, B)


def main():
    option = sys.argv[1] if len(sys.argv) > 1 else "A"
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "out", option)
    os.makedirs(os.path.join(out, "preview"), exist_ok=True)
    B, skins, cloths, sword, uvs, cubes = build(option)
    h = SWORD_Y + sword.height
    json.dump(geo(B, uvs, cubes, h), open(os.path.join(out, "knight.geo.json"), "w"), indent=1)
    json.dump(rig_meta(B), open(os.path.join(out, "knight.rig.json"), "w"), indent=1)
    for t in TIERS:
        for side in SIDES:
            texture(skins, cloths, sword, t, side).save(os.path.join(out, f"knight_t{t}_{side}.png"))
        json.dump(geo(B, uvs, cubes, h, lambda bn: shows(bn, t)), open(os.path.join(out, "preview", f"t{t}.geo.json"), "w"))
    print(f"option {option}: {len(B)} bones, texture 128x{h}, {out}")


if __name__ == "__main__":
    main()
