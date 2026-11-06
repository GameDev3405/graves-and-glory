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
import importlib, json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, "..", ".."))
for p in ("hand/common", "hand/knight", "hand/weapons"):
    sys.path.insert(0, os.path.join(ART, p))
from PIL import Image                                         # noqa: E402
import handlib as H                                           # noqa: E402
from handlib import Box, Bone, pack, paint_box, on, SIDES    # noqa: E402
import build_swords as SW                                     # noqa: E402

# The bow in the left fist (build_anims solves it, see BOW_ROT there): the grip frame's +y (top nock) along the fist's
# through-axis, its back (+x) out along the knuckles toward the target, the string toward the archer.
BOW_ROT = (-90, 0, -90)
BOW_FRAMES = ("rest", "pull_0", "pull_1", "pull_2")
# A crossbow (kind "crossbow") rides the right fist. Its string and bolt change through a shot, so they hang on three frame bones
# under the same "draw" channel the bow uses (GeoTroopRenderer shows the frame whose suffix the channel picks), keeping the bow's names:
#   rest   (channel 0, the default) loaded: the art's spanned string and the bolt on the rail
#   pull_0 (channel 1)             spent: the string slack straight across the prod, no bolt
#   pull_1 (channel 2)             spanned: the string back on the lock, no bolt yet
XBOW_FRAMES = {"rest": ("string_l", "string_r", "bolt"), "pull_0": ("string_slack",), "pull_1": ("string_l", "string_r")}
XBOW_LOOSE = {n for ns in XBOW_FRAMES.values() for n in ns}
# troop id: (art dir, module, Troop attribute, weapon module, weapon id (tiers <id>, <id>_t2, <id>_t3), weapon rotation in the fist)
TROOPS = {
    "sentinel": ("sentinel", "build_sentinel", "SENTINEL", "build_longsword", "longsword", (-90, 0, 90)),
    "gl_marshal": ("gl_marshal", "build_gl_marshal", "MARSHAL", "build_marshal_sword", "marshal_sword", (-90, 0, 90)),
    "revenant": ("revenant", "build_revenant", "REVENANT", "build_gb_notched_sword", "notched_sword", (-90, 0, 90)),
    "tc_raider": ("tc_raider", "build_tc_raider", "TC_RAIDER", "build_house_weapons", "cutlass", (-90, 0, 90)),
    # a spear (both fists on the shaft; build_spear_anims solves the left fist to it) rides the right fist like a blade
    "wm_tusk": ("wm_tusk", "build_wm_tusk", "WM_TUSK", "build_house_weapons", "boar_spear", (-90, 0, 90)),
    "tc_tideguard": ("tc_tideguard", "build_tc_tideguard", "TC_TIDEGUARD", "build_house_weapons", "coral_trident", (-90, 0, 90)),
    "tc_admiral": ("tc_admiral", "build_tc_admiral", "TC_ADMIRAL", "build_house_weapons", "admiral_trident", (-90, 0, 90)),
    # an axe rides the right fist like a sword, rolled half a turn about the haft so the bit faces the foe in guard and leads every chop
    "ic_breaker": ("ic_breaker", "build_ic_breaker", "IC_BREAKER", "build_ic_weapons", "ic_axe", (-90, 0, -90)),
    # a scythe rides the right fist like the greataxe (both fists on the haft, solved by the spear builder), rolled so the
    # blade (weapon +x) is placed as the axe's bit is
    "reaper": ("reaper", "build_reaper", "REAPER", "build_gb_scythe", "scythe", (-90, 0, -90)),
    "ic_hearthguard": ("ic_hearthguard", "build_ic_hearthguard", "IC_HEARTHGUARD", "build_house_weapons", "hearth_axe", (-90, 0, -90)),
    # a bow (kind "bow") rides the left fist; its rest and three draw frames are child bones the renderer switches between
    "archer": ("archer", "build_archer", "ARCHER", "build_bows", "order_bow", BOW_ROT, "bow"),
    "longbow": ("longbow", "build_longbow", "LONGBOW", "build_bows", "longbow", BOW_ROT, "bow"),
    "bonebow": ("bonebow", "build_bonebow", "BONEBOW", "build_gb_bone_bow", "bone_bow", BOW_ROT, "bow"),
    "stalker": ("stalker", "build_stalker", "STALKER", "build_gb_frost_bow", "frost_bow", BOW_ROT, "bow"),
    "wm_hunter": ("wm_hunter", "build_wm_hunter", "WM_HUNTER", "build_bows", "order_bow", BOW_ROT, "bow"),
    "ic_arbalist": ("ic_arbalist", "build_ic_arbalist", "IC_ARBALIST", "build_ic_weapons", "ic_crossbow", (-90, 0, 90), "crossbow"),
}
SHIELD_AT = (-1, 4, -1.5)     # the knight's grip: in the left fist by its strap, face to the front
WAIST, SPLIT = 8.5, ((12, 8), (12, 0))
DROP = {"arms_folded"}
# Per troop, body-hung gear re-hung on a leg bone: a strand that reaches the floor from the hips would sink through it
# whenever the guard bends the knees, so it rides the shin it hangs behind instead (same place in the rest pose).
HANG = {"tc_admiral": {"kelp_hem": "left_shin"}}


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


def rigify(src, weapon_rot, kind="blade", hang=None):
    """The troop's handlib bones re-hung on the knight's skeleton (pivots relative to the parent)."""
    src = [bn for bn in src if bn.name not in DROP]
    for bn in src:   # a troop's own foot (the raider's) is shin gear: it rides the rig's foot like a boot
        if bn.name in ("right_foot", "left_foot"):
            bn.name += "_gear"
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
    hang, later = hang or {}, []
    for bn in src:
        if bn.name in ("root", "body"):
            continue
        a, par = ab[bn.name], bn.parent
        if bn.name in hang:
            later.append((bn, hang[bn.name], a))
            continue
        if par == "body":
            if bn.name.startswith("cape") or bn.name == "collar":
                if cape_pivot is None:
                    cape_pivot = a
                    add(Bone("cape", "torso", rot=bn.rot, team=True), "torso", a)
                bn.rot = [0, 0, 0]
                if bn.name == "cape":   # a source bone named plain "cape" (ic_breaker) hangs under the container
                    bn.name = "cape_cloth"
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
    for bn, par, a in later:
        add(bn, par, a)
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
        if kind == "bow":   # the bow in the left fist; one child bone a draw frame, shown by the "draw" channel
            order.append(Bone(f"weapon_t{t}", "left_hand", stages=[t], tag="weapon", rot=weapon_rot))
            for f in BOW_FRAMES:
                order.append(Bone(f"bow_t{t}_{f}", f"weapon_t{t}", stages=[t], tag="draw"))
        else:
            order.append(Bone(f"weapon_t{t}", "right_hand", stages=[t], tag="weapon", rot=weapon_rot))
            if kind == "crossbow":
                for f in XBOW_FRAMES:
                    order.append(Bone(f"bow_t{t}_{f}", f"weapon_t{t}", stages=[t], tag="draw"))
    if kind in ("bow", "crossbow"):   # the draw channel: an empty bone whose position x the attack clips key to the frame index
        order.append(Bone("draw", "root"))
    del names
    return order


class Rig:
    def __init__(s, tid):
        d, mod, attr, wmod, s.wid, wrot, *kind = TROOPS[tid]
        s.kind = kind[0] if kind else "blade"
        sys.path.insert(0, os.path.join(ART, "hand", d))
        s.tid, s.troop = tid, getattr(importlib.import_module(mod), attr)
        s.weapon = importlib.import_module(wmod)
        s.skin, s.cloth = s.troop.skin, s.troop.cloth
        s.wy = s.skin[1] + s.cloth[1]
        s.B = rigify(s.troop.bones(), wrot, s.kind, HANG.get(tid))
        s.tiers = tuple(s.troop.tiers)

    def weapon_cubes(s, t):
        return s.weapon.TIERS[s.wid if t == 1 else f"{s.wid}_t{t}"][0]()

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
        s.frames, s.col = {}, {}
        for t in s.tiers:
            if s.kind == "bow":   # as build_bows.project: the rest frame's solid cubes, the arrow, one shared string column
                wid = s.wid if t == 1 else f"{s.wid}_t{t}"
                frames, _ = s.weapon.TIERS[wid][0]().frames()
                s.frames[t] = frames
                Th = importlib.import_module("build_bows").Thread
                solid = [c for c in frames["rest"] if not isinstance(c, Th)]
                names = {c.name.split("/")[1] for c in solid}
                solid += [c for c in frames["pull_2"] if not isinstance(c, Th) and c.name.split("/")[1] not in names]
                longest = max(c.n for cs in frames.values() for c in cs if isinstance(c, Th))
                col = SW.Cube("stringcol", [0, 0, 0], [1, int(math.ceil(longest)), 1], "linen", SW.flat(importlib.import_module("build_bows").STRING),
                              skip=[f for f in SW.FACES if f != "north"])
                s.cubes[t] = [SW.Cube(c.name.split("/")[1], c.lo, c.hi, c.mat, c.paint, c.skip, c.turn, c.pivot) for c in solid] + [col]
            elif s.kind == "crossbow":   # the solid stock on weapon_t<n>, the string and bolt split into the frame bones
                cubes = s.weapon_cubes(t)
                sl = next(c for c in cubes if c.name == "string_l")
                cubes.append(SW.Cube("string_slack", [-7, 14, 0.5], [7, 15, 1.5], sl.mat, sl.paint, skip=("up", "down")))
                by = {c.name: c for c in cubes}
                s.frames[t] = {f: [by[n] for n in ns] for f, ns in XBOW_FRAMES.items()}
                s.solid = {**getattr(s, "solid", {}), t: [c for c in cubes if c.name not in XBOW_LOOSE]}
                s.cubes[t] = cubes
            else:
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
            wcubes = ()
            if bn.tag == "weapon" and keep(bn) and s.kind != "bow":
                t = int(bn.name[-1]); wcubes = s.solid[t] if s.kind == "crossbow" else s.cubes[t]
            if bn.tag == "draw" and keep(bn):
                t, frame = int(bn.name[5]), bn.name[7:]
                wcubes = s.frames[t][frame]
            if wcubes:
                hx, hy, hz = j["pivot"]
                for sc in wcubes:
                    faces = {}
                    base = sc.name.split("/")[-1]
                    if isinstance(sc, getattr(s.weapon, "Thread", ())):   # the string reads the shared string column
                        ux, uy = s.uvs[t][("stringcol", "north")][:2]
                        dims = {"north": (sc.w, sc.n), "south": (sc.w, sc.n), "east": (2 * sc.hz, sc.n), "west": (2 * sc.hz, sc.n),
                                "up": (sc.w, 2 * sc.hz), "down": (sc.w, 2 * sc.hz)}
                        for f in SW.FACES:
                            faces[f] = {"uv": [ux, uy], "uv_size": [r(dims[f][0]), r(dims[f][1])]}
                    for f in (SW.FACES if not faces else ()):
                        if (base, f) not in s.uvs[t] or f in getattr(sc, "skip", ()): continue
                        x0, y0, x1, y1 = s.uvs[t][(base, f)]
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


def shows(bn, tier, face=0, shield=True, frame="rest"):
    if bn.stages and tier not in bn.stages: return False
    if bn.variants is not None and face not in bn.variants: return False
    if bn.tag == "draw" and not bn.name.endswith(frame): return False
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
        if R.kind in ("bow", "crossbow"):   # one preview per draw frame, plus every frame at once for renders that switch frames by bone scale
            for f in (BOW_FRAMES if R.kind == "bow" else XBOW_FRAMES):
                json.dump(R.geo(lambda bn, f=f: shows(bn, t, frame=f)), open(os.path.join(out, "preview", f"t{t}_{f}.geo.json"), "w"))
            json.dump(R.geo(lambda bn: shows(bn, t) or (bn.tag == "draw" and f"_t{t}_" in bn.name)),
                      open(os.path.join(out, "preview", f"t{t}_all.geo.json"), "w"))
    print(f"{tid}: {len(R.B)} bones, texture {R.skin[0]}x{R.height}, {os.path.normpath(out)}")


if __name__ == "__main__":
    main()
