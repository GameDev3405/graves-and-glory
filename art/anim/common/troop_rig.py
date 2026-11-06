"""A hand-authored biped troop's GeckoLib rig, on the knight's skeleton (animation rebuild, docs/anim-rebuild/PLAN.md).

The troop's own handlib bones (art/hand/<troop>/build_<troop>.py: its art, unchanged) are re-hung on the knight's skeleton so the
knight's clips drive it: root > hips > torso > head; torso > arm > forearm > hand > weapon_t1..3; hips > leg > shin > foot.
Every bone keeps its absolute pivot, so its boxes and their painted UVs carry over as they are; only the parents change, and:

  body          split at the waist like the knight's: hips (rows 8..12) and torso (rows 0..9), one shared row
  body gear     below the waist (top at or under y 8.5) rides the hips, the rest the torso; a full-height shell (a tabard)
                splits like the body into <name> on the torso and <name>_skirt on the hips
  shins         stop 2 px above the sole; <side>_foot hangs at the ankle with the shin gear from y 3 down (boot, toe), or, with
                no boot, the shin's own bottom rows
  shield        gripped in the left fist at the knight's grip (build_anims turns the hand to keep it upright)
  cape          the capes and collar under one "cape" bone at their shared pivot, the bone the knight's clips swing
  pauldrons     undersides painted as the leather lining (they show whenever an arm lifts)
  weapon        the troop's weapon tiers (art/hand/weapons) in the right fist, Y-up grip frame like the knight's sword

The texture per tier and side is the skin sheet over the side's cloth sheet over the weapon sheets, as the knight's.

    python art/anim/common/troop_rig.py <troop> [out dir]
"""
import importlib, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, "..", ".."))
for p in ("hand/common", "hand/knight", "hand/weapons"):
    sys.path.insert(0, os.path.join(ART, p))
from PIL import Image                                         # noqa: E402
import handlib as H                                           # noqa: E402
from handlib import Box, Bone, pack, paint_box, on, SIDES    # noqa: E402
import build_swords as SW                                     # noqa: E402

# troop id: (art dir, module, Troop attribute, weapon module, weapon rotation in the fist)
TROOPS = {
    "sentinel": ("sentinel", "build_sentinel", "SENTINEL", "build_longsword", (-90, 0, 90)),
}
SHIELD_AT = (-1, 4, -1.5)     # the knight's grip: in the left fist by its strap, face to the front
WAIST, SPLIT = 8.5, ((12, 8), (12, 0))
DROP = {"arms_folded"}


def lined(paint):
    def p(face, w, h):
        return [[H.LEATH[3]] * w for _ in range(h)] if face == "bottom" else paint(face, w, h)
    return p


def absolute(B):
    a = {}
    for bn in B:
        pp = a.get(bn.parent, [0, 0, 0])
        a[bn.name] = [pp[i] + bn.pivot[i] for i in range(3)]
    return a


def rigify(src, weapon_rot):
    """The troop's handlib bones re-hung on the knight's skeleton (pivots relative to the parent)."""
    src = [bn for bn in src if bn.name not in DROP]
    ab = absolute(src)
    names = {bn.name for bn in src}
    out, at = [], {}

    def add(bn, parent, apiv):
        at[bn.name] = list(apiv)
        bn.parent = parent
        bn.pivot = [apiv[i] - at[parent][i] for i in range(3)] if parent else list(apiv)
        out.append(bn)
        return bn

    body = next(bn for bn in src if bn.name == "body")
    bb = body.boxes[0]
    root = add(Bone("root", ""), "", (0, 24, 0))
    hips = add(Bone("hips", "root", boxes=[Box((-4, -4, -2), (8, 4, 4), bb.paint, dil=0.05, cut=SPLIT[0])]), "root", (0, 12, 0))
    torso = add(Bone("torso", "hips", boxes=[Box((-4, -9, -2), (8, 9, 4), bb.paint, cut=SPLIT[1])]), "hips", (0, 9, 0))
    del root, hips, torso
    cape_pivot = None
    for bn in src:
        if bn.name in ("root", "body"):
            continue
        a, par = ab[bn.name], bn.parent
        if par == "body":
            if bn.name.startswith("cape") or bn.name == "collar":
                if cape_pivot is None:
                    cape_pivot = a
                    add(Bone("cape", "torso", rot=bn.rot, team=True), "torso", a)
                bn.rot = [0, 0, 0]
                add(bn, "cape", a)
                continue
            full = [b for b in bn.boxes if b.size[1] == 12 and b.origin[1] + a[1] == 0]
            if full and len(full) == len(bn.boxes):   # a full-height shell: split like the body
                b = full[0]
                up = Box((b.origin[0], -9, b.origin[2]), (b.size[0], 9, b.size[2]), b.paint, b.dil, b.sheet, cut=(12, 0))
                lo = Box((b.origin[0], -4, b.origin[2]), (b.size[0], 4, b.size[2]), b.paint, b.dil + 0.05, b.sheet, cut=(12, 8))
                add(Bone(bn.name, "torso", stages=bn.stages, tag=bn.tag, team=bn.team, variants=bn.variants, boxes=[up]), "torso", (0, 9, 0))
                add(Bone(bn.name + "_skirt", "hips", stages=bn.stages, tag=bn.tag, team=bn.team, variants=bn.variants, boxes=[lo]), "hips", (0, 12, 0))
                continue
            top = min(b.origin[1] for b in bn.boxes) + a[1] if bn.boxes else 0
            add(bn, "hips" if top >= WAIST else "torso", a)
            continue
        if par == "root":   # the legs
            add(bn, "hips", a)
            continue
        if bn.name == "shield":
            bn.rot = [0, 0, 0]
            add(bn, "left_hand", [ab["left_hand"][i] + SHIELD_AT[i] for i in range(3)])
            continue
        if bn.name.endswith("_pauldron") or bn.name.endswith("_spaulder"):
            for b in bn.boxes:
                b.paint = {k: lined(v) for k, v in b.paint.items()}
        add(bn, par, a)
        if bn.name.endswith("_shin"):
            side = bn.name[:-5]
            bn.boxes[0].size[1] -= 2          # the shin stops inside the boot, as the knight's
            foot = Bone(f"{side}_foot", bn.name)
            add(foot, bn.name, [a[0], a[1] + 4, a[2]])
    # shin gear from y 3 down moves to the foot (boot, toe); a troop with no boot gets the shin's own bottom rows
    for side in ("right", "left"):
        shin, foot = f"{side}_shin", f"{side}_foot"
        booted = False
        for bn in out:
            if bn.parent == shin and bn.boxes and min(b.origin[1] for b in bn.boxes) >= 3:
                bn.parent = foot
                bn.pivot = [bn.pivot[0], bn.pivot[1] - 4, bn.pivot[2]]
                at[bn.name] = [at[bn.name][0], at[bn.name][1], at[bn.name][2]]
                booted = True
        if not booted:
            sb = next(bn for bn in out if bn.name == shin).boxes[0]
            full, c0 = sb.cut
            f = next(bn for bn in out if bn.name == foot)
            f.boxes = [Box((sb.origin[0], 0, sb.origin[2]), (sb.size[0], 2, sb.size[2]), sb.paint, sb.dil, sb.sheet, cut=(full, full - 2))]
    # the order the geometry wants: a parent before its children
    order, seen = [], set()
    kids = {}
    for bn in out:
        kids.setdefault(bn.parent, []).append(bn)
    def walk(p):
        for bn in kids.get(p, []):
            if bn.name in seen: continue
            seen.add(bn.name); order.append(bn); walk(bn.name)
    walk("")
    assert len(order) == len(out), "orphans: " + ", ".join(bn.name for bn in out if bn.name not in seen)
    for t in (1, 2, 3):
        order.append(Bone(f"weapon_t{t}", "right_hand", stages=[t], tag="weapon", rot=weapon_rot))
    del names
    return order


class Rig:
    def __init__(s, tid):
        d, mod, attr, wmod, wrot = TROOPS[tid]
        sys.path.insert(0, os.path.join(ART, "hand", d))
        s.tid, s.troop = tid, getattr(importlib.import_module(mod), attr)
        s.weapon = importlib.import_module(wmod)
        s.skin, s.cloth = s.troop.skin, s.troop.cloth
        s.wy = s.skin[1] + s.cloth[1]
        s.B = rigify(s.troop.bones(), wrot)
        s.tiers = tuple(s.troop.tiers)

    def weapon_cubes(s, t):
        return list(s.weapon.TIERS.values())[t - 1][0]()

    def textures(s):
        B = s.B
        skin_boxes = [b for bn in B for b in bn.boxes if b.sheet == "skin"]
        cloth_boxes = [b for bn in B for b in bn.boxes if b.sheet == "cloth"]
        pack(skin_boxes, s.skin, s.tid + " skin"); pack(cloth_boxes, s.cloth, s.tid + " cloth")
        s.skins = {}
        for t in s.tiers:
            img = Image.new("RGBA", s.skin, (0, 0, 0, 0))
            for bn in B:
                if not on(bn, t): continue
                for b in bn.boxes:
                    if b.sheet != "skin": continue
                    p = b.paint.get(t, b.paint.get(0))
                    if p: paint_box(img, b, p)
            s.skins[t] = img
        s.cloths = {}
        for side in SIDES:
            img = Image.new("RGBA", s.cloth, (0, 0, 0, 0))
            for b in cloth_boxes: paint_box(img, b, s.troop.cloth_painter(b.paint[0], side))
            s.cloths[side] = img
        sheets, s.uvs, s.cubes = [], {}, {}
        assert SW.SHEET_W * len(s.tiers) <= s.skin[0], "weapon sheets wider than the skin"
        for t in s.tiers:
            s.cubes[t] = s.weapon_cubes(t)
            colour, _, uv = SW.pack(s.cubes[t])
            sheets.append(colour)
            dx = SW.SHEET_W * (t - 1)
            s.uvs[t] = {k: [v[0] + dx, v[1] + s.wy, v[2] + dx, v[3] + s.wy] for k, v in uv.items()}
        s.wsheet = Image.new("RGBA", (s.skin[0], max(x.height for x in sheets)), (0, 0, 0, 0))
        for i, x in enumerate(sheets): s.wsheet.paste(x, (SW.SHEET_W * i, 0))
        s.height = s.wy + s.wsheet.height

    def texture(s, tier, side):
        img = Image.new("RGBA", (s.skin[0], s.height), (0, 0, 0, 0))
        img.paste(s.skins[tier], (0, 0)); img.paste(s.cloths[side], (0, s.skin[1])); img.paste(s.wsheet, (0, s.wy))
        return img

    def geo(s, keep=lambda bn: True):
        absp, out = {}, []
        r = lambda v: round(v, 4)
        for bn in s.B:
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
                uv = list(b.uv) if b.sheet == "skin" else [b.uv[0], b.uv[1] + s.skin[1]]
                c = {"origin": [r(o[0]), r(24 - (o[1] + b.size[1])), r(o[2])], "size": list(b.size), "uv": uv}
                if b.dil: c["inflate"] = b.dil
                cs.append(c)
            if bn.tag == "weapon" and keep(bn):
                t = int(bn.name[-1]); hx, hy, hz = j["pivot"]
                for sc in s.cubes[t]:
                    faces = {}
                    for f in SW.FACES:
                        if (sc.name, f) not in s.uvs[t]: continue
                        x0, y0, x1, y1 = s.uvs[t][(sc.name, f)]
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
            "description": {"identifier": f"geometry.gravesandglory.{s.tid}", "texture_width": s.skin[0], "texture_height": s.height,
                            "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.25, 0]},
            "bones": out}]}

    def meta(s):
        return {"troop": s.tid, "tiers": list(s.tiers), "faces": s.troop.faces(), "bones": {
            bn.name: {k: v for k, v in (("stages", bn.stages), ("variants", bn.variants), ("team", bn.team), ("tag", bn.tag)) if v}
            for bn in s.B}}


def shows(bn, tier, face=0, shield=True):
    if bn.stages and tier not in bn.stages: return False
    if bn.variants is not None and face not in bn.variants: return False
    return shield or bn.tag != "shield"


def main():
    tid = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "..", tid, "out")
    os.makedirs(os.path.join(out, "preview"), exist_ok=True)
    R = Rig(tid); R.textures()
    json.dump(R.geo(), open(os.path.join(out, f"{tid}.geo.json"), "w"), indent=1)
    json.dump(R.meta(), open(os.path.join(out, f"{tid}.rig.json"), "w"), indent=1)
    for t in R.tiers:
        for side in SIDES:
            R.texture(t, side).save(os.path.join(out, f"{tid}_t{t}_{side}.png"))
        json.dump(R.geo(lambda bn: shows(bn, t)), open(os.path.join(out, "preview", f"t{t}.geo.json"), "w"))
    print(f"{tid}: {len(R.B)} bones, texture {R.skin[0]}x{R.height}, {os.path.normpath(out)}")


if __name__ == "__main__":
    main()
