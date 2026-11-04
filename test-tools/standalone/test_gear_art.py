"""Checks on the hand-authored commander gear (docs/superpowers/plans/2026-11-03-gear-art.md, Task 1)."""
import importlib, itertools, os, sys, unittest
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
GEAR = os.path.join(ROOT, "art", "hand", "gear")
sys.path.insert(0, GEAR); sys.path.insert(0, os.path.join(GEAR, "sets")); sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import gearlib, render
from handlib import on, rects, dims, SIDES

SETS = [f[:-3] for f in sorted(os.listdir(os.path.join(GEAR, "sets"))) if f.endswith(".py") and not f.startswith("_")]
_cache = {}
def pixels(img): return img.get_flattened_data() if hasattr(img, "get_flattened_data") else img.getdata()
def art_only(): return {i["id"] for i in gearlib.ids().get("art_only", [])}
def built():
    if not _cache:
        for s in SETS:
            for p in importlib.import_module(s).pieces(): _cache[p.item_id] = (s, p, p.build())
    return _cache

class GearArt(unittest.TestCase):
    def setUp(self):
        if not SETS: self.skipTest("no set modules yet")

    def test_ids_known(self):
        items = {i["id"]: i for i in gearlib.ids()["items"] + gearlib.ids().get("art_only", [])}   # art_only: House sets, pending Kort's #113
        for iid, (_, p, _) in built().items():
            self.assertIn(iid, items); self.assertEqual(p.slot, items[iid]["slot"], iid)

    def test_one_texel_per_pixel(self):
        for iid, (_, p, (B, skins, cloths, glows)) in built().items():
            self.assertEqual(p.model_json(B, glows)["density"], 1)
            for bone in B:
                for b in bone.boxes:
                    self.assertTrue(all(float(v).is_integer() for v in b.size), (iid, bone.name, b.size))
                    self.assertEqual(b.dil, 0, (iid, bone.name))
                    sheet = p.cloth if b.sheet == "cloth" else p.skin
                    for face, (x0, y0) in rects(b).items():
                        fw, fh = dims(face, *(int(v) for v in b.size))
                        self.assertLessEqual(x0 + fw, sheet[0]); self.assertLessEqual(y0 + fh, sheet[1])

    def test_body_bones_on_the_neck_frame(self):
        """Kort 11-04: a body-parented bone exports in the vanilla body part's frame (box y 0..12 from the neck), not the
        mannequin's waist frame, or the chest rides up over the head and bares the torso."""
        import json
        d = os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory", "models", "gear")
        for f in sorted(os.listdir(d)):
            if not f.endswith(".json"): continue
            with open(os.path.join(d, f)) as h: j = json.load(h)
            for b in j["bones"]:
                if b["parent"] != "body" or any(b["rotation"]): continue
                for x in b["boxes"]:
                    lo = b["pivot"][1] + x["origin"][1]
                    self.assertTrue(-8 <= lo and lo + x["size"][1] <= 26, (f, b["name"], lo))   # embers rise a little over the shoulders

    def test_every_rarity_paints(self):
        for iid, (_, p, (B, skins, _, _)) in built().items():
            for r in gearlib.RARITIES:
                for bone in B:
                    if not on(bone, r) or bone.team: continue
                    for b in bone.boxes:
                        x0, y0 = rects(b)["front"]; w, h = int(b.size[0]), int(b.size[1])
                        if not (w and h): continue
                        if iid in art_only():   # the House sets' painters cut shaped corners: any front texel will do
                            self.assertTrue(any(skins[r].getpixel((x0 + x, y0 + y))[3] == 255 for x in range(w) for y in range(h)), (iid, r, bone.name))
                        else: self.assertEqual(skins[r].getpixel((x0, y0))[3], 255, (iid, r, bone.name))

    def test_every_side_builds(self):
        for iid, (_, p, (B, _, cloths, _)) in built().items():
            self.assertEqual(set(cloths), set(SIDES))
            for bone in B:
                sheets = {b.sheet for b in bone.boxes}
                self.assertTrue(sheets <= ({"cloth"} if bone.team else {"skin"}), (iid, bone.name))

    def test_no_back_flags(self):
        for iid, (_, p, (B, _, _, _)) in built().items():
            for bone in B:
                for word in ("flag", "pennant", "standard", "banner"):
                    self.assertNotIn(word, (bone.name + bone.tag + bone.part).lower(), (iid, bone.name))

    def test_no_coplanar_faces_between_slots(self):
        import mannequin
        def world_faces(iid, B):
            frame = {bn.name: bn for bn in mannequin.bones()}
            out = []
            for bone in B:
                if any(bone.rot) or bone.parent not in gearlib.BIPED: continue
                pv = frame[bone.parent]; off = [0, 0, 0]
                while pv:   # unrotated biped frames: offsets add up
                    off = [off[i] + pv.pivot[i] for i in range(3)]; pv = frame.get(pv.parent)
                off = [off[i] + bone.pivot[i] for i in range(3)]
                for b in bone.boxes:
                    lo = [off[i] + b.origin[i] for i in range(3)]; hi = [lo[i] + b.size[i] for i in range(3)]
                    out.append((bone.parent, lo, hi, tuple(bone.stages)))
            return out
        by_set = {}
        for iid, (s, p, (B, _, _, _)) in built().items(): by_set.setdefault(s, []).append((p, world_faces(iid, B)))
        for s, ps in by_set.items():
            for (pa, fa), (pb, fb) in itertools.combinations(ps, 2):
                if pa.slot == pb.slot: continue
                for (ba, la, ha, sa), (bb, lb, hb, sb) in itertools.product(fa, fb):
                    if ba != bb or (sa and sb and not set(sa) & set(sb)): continue
                    for ax in range(3):
                        o = [i for i in range(3) if i != ax]
                        overlap = all(min(ha[i], hb[i]) - max(la[i], lb[i]) > 1e-6 for i in o)
                        if not overlap: continue
                        for pa_ in (la[ax], ha[ax]):
                            for pb_ in (lb[ax], hb[ax]):
                                self.assertFalse(abs(pa_ - pb_) < 1e-6, (pa.item_id, pb.item_id, ax, pa_))

    def test_legendary_silhouette_grows(self):
        by_set = {}
        for iid, (s, p, b) in built().items(): by_set.setdefault(s, []).append(p)
        def area(ps, r, yaw):
            bones, vis, sheets = gearlib.worn(ps, r, "crimson", {p.item_id: built()[p.item_id][2] for p in ps})
            img = render.render(bones, vis, sheets, yaw, 0, px=2)
            return sum(1 for px in pixels(img) if px != gearlib.BG)
        for s, ps in by_set.items():
            grow = 1.0 if ps[0].item_id in art_only() else 1.06   # House sets keep the shapes Kort approved on 11-04
            for yaw in (0, -90):
                self.assertGreater(area(ps, 5, yaw), grow * area(ps, 1, yaw), (s, yaw))

    def test_house_neutral(self):
        import graveborn, tidecallers, wildmoot
        banned = {tuple(c[:3]) for R in (graveborn.VIOLET, tidecallers.SEA, wildmoot.OCHRE) for c in R}
        for iid, (_, p, (_, skins, _, glows)) in built().items():
            for img in list(skins.values()) + list(glows.values()):
                used = {px[:3] for px in pixels(img) if px[3]}
                self.assertFalse(used & banned, iid)

    def test_relic_icons(self):
        """Every relic id has a 32 x 32 icon, a clear 1 px margin, no House ramp, and no two icons alike."""
        import icons, graveborn, tidecallers, wildmoot
        banned = {tuple(c[:3]) for R in (graveborn.VIOLET, tidecallers.SEA, wildmoot.OCHRE) for c in R}
        ics = icons.relics()
        self.assertEqual(set(ics), {r["id"] for r in gearlib.ids()["relics"]})
        seen = set()
        for k, im in ics.items():
            self.assertEqual(im.size, (32, 32), k)
            for x in range(32):
                for y in (0, 31):
                    self.assertEqual(im.getpixel((x, y))[3], 0, k); self.assertEqual(im.getpixel((y, x))[3], 0, k)
            px = tuple(pixels(im))
            self.assertFalse({p[:3] for p in px if p[3]} & banned, k)
            self.assertNotIn(px, seen, k); seen.add(px)

sys.path.insert(0, os.path.join(GEAR, "weapons")); sys.path.insert(0, os.path.join(ROOT, "art", "hand", "weapons"))

class GearWeapons(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import weaponlib
        cls.L = weaponlib; cls.ws = {w.item_id: w for w in weaponlib.items()}

    def test_weapon_ids(self):
        want = {i["id"]: i for i in gearlib.ids()["items"] if i["slot"] in ("main", "off")}
        self.assertEqual(set(self.ws), set(want))
        for iid, w in self.ws.items(): self.assertEqual(w.hands, want[iid].get("hands", 1), iid)

    def test_cube_caps(self):
        for iid, w in self.ws.items():
            for r in gearlib.RARITIES: self.assertLessEqual(len(w.counted(r)), self.L.CAP[r], (iid, r))

    def test_gilt_and_house_colours(self):
        import graveborn, tidecallers, wildmoot
        banned = {tuple(c[:3]) for R in (graveborn.VIOLET, tidecallers.SEA, wildmoot.OCHRE) for c in R}
        gilt = {c[:3] for c in self.L.GILT}
        for iid, w in self.ws.items():
            for r in gearlib.RARITIES:
                for side in (("crimson", "teal") if w.show_side else ("crimson",)):
                    n = g = 0
                    for c in w.cubes(r, side):
                        for f in self.L.W.FACES:
                            if f in c.skip: continue
                            fw, fh = c.size(f)
                            if not (fw and fh): continue
                            for row in c.paint(f, fw, fh):
                                for px in row:
                                    if px is None or (len(px) > 3 and not px[3]): continue
                                    n += 1; g += tuple(px[:3]) in gilt
                                    self.assertNotIn(tuple(px[:3]), banned, (iid, r, c.name))
                    self.assertLessEqual(g / n, 0.25, (iid, r, side, g / n))

    def test_tiers_change_shape(self):
        """Every step up the ladder changes the weapon's build, not only its colours: the boxes of its non-gem cubes differ
        between each pair of adjacent rarities (the weapon review, 2026-11-03)."""
        for iid, w in self.ws.items():
            shape = lambda r: {(tuple(round(v, 2) for v in c.lo), tuple(round(v, 2) for v in c.hi), c.turn)
                               for c in w.cubes(r) if c.mat != "gem"}
            for r in gearlib.RARITIES[:-1]: self.assertNotEqual(shape(r), shape(r + 1), (iid, r, r + 1))

    def test_two_handed_grips(self):
        for iid, w in self.ws.items():
            if w.hands < 2: continue
            for r in gearlib.RARITIES:
                for y in w.grips: self.assertTrue(w.haft_of(y, r), (iid, r, y))
            for clip in ("idle", "attack"):
                self.L.posed(w, clip); self.assertLess(self.L.reach.miss, 2.5, (iid, clip))

    def test_exported_models(self):
        """Each weapon's models/gear/weapon/<id>.json, as the game loads it (TroopMesh.parse): every rarity's quads are
        parallelograms with their texels on the sheet, each one's normal points out of its cube, and every texture it names is
        in the assets."""
        import json
        assets = os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory")
        tex = lambda ident: os.path.join(assets, ident.split(":", 1)[1])
        for iid, w in self.ws.items():
            with open(os.path.join(assets, "models", "gear", "weapon", iid + ".json")) as f: j = json.load(f)
            self.assertEqual((j["item"], j["hands"], j["side"]), (iid, w.hands, w.show_side))
            self.assertEqual("pose" in j, w.hands == 2, iid)
            for r in gearlib.RARITIES:
                row = j["rarities"][str(r)]; sw, sh = row["sheet"]
                names = [row["texture"] + s + ".png" for s in SIDES] if w.show_side else [row["texture"]]
                for n in names + ([row["glow"]] if "glow" in row else []): self.assertTrue(os.path.exists(tex(n)), n)
                self.assertEqual(row["quads"], self.L.model_quads(w, w.cubes(r), self.L.W.pack(w.cubes(r))[2])[1], (iid, r))
                for q in row["quads"]:
                    c = [q[3 * k:3 * k + 3] for k in range(4)]
                    for a in range(3): self.assertAlmostEqual(c[2][a], c[1][a] + c[3][a] - c[0][a], 3, (iid, r))
                    for k in range(4): self.assertTrue(0 <= q[12 + 2 * k] <= sw and 0 <= q[13 + 2 * k] <= sh, (iid, r))
            part, off = self.L.ARM[self.L.HOLDS[w.hold][0]]
            for c in w.cubes(5):   # outward: the face centre minus the cube centre runs along the normal
                R = self.L.W.rot(0, 0, c.turn)
                to = lambda p: [v + o for v, o in zip(self.L.hold_point(w.hold, [x + y for x, y in zip(self.L.W.app(R, [p[i] - c.pivot[i] for i in range(3)]), c.pivot)]), off)]
                mid = to([(c.lo[i] + c.hi[i]) / 2 for i in range(3)])
                _, quads = self.L.model_quads(w, [c], self.L.W.pack([c])[2])
                for q in quads:
                    p = [q[3 * k:3 * k + 3] for k in range(4)]
                    u = [p[1][i] - p[0][i] for i in range(3)]; v = [p[3][i] - p[0][i] for i in range(3)]
                    n = [u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]]
                    m = [sum(p[k][i] for k in range(4)) / 4 - mid[i] for i in range(3)]
                    self.assertGreaterEqual(sum(n[i] * m[i] for i in range(3)), 0, (iid, c.name))

    def test_clip_faces(self):
        """Every clip key points the weapon's tip the way the move goes (Kort 11-03), and gear_clips.json is clips.py's export."""
        import json, clips
        for item, trig in clips.ITEMS.items():
            w = clips.weapons()[item]
            for name in trig.values():
                for k in clips.CLIPS[name]["keys"]:
                    off = clips.angle(clips.tip(w, clips.pose_of(w, k)), k["face"])
                    self.assertLessEqual(off, 45, (item, name, k["t"], k["note"], round(off)))
        with open(os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory", "gear_clips.json")) as f:
            self.assertEqual(json.load(f), json.loads(json.dumps(clips.exported())), "run: python art/hand/gear/clips.py export")

    def test_held_clear_of_the_body(self):
        """Kort 11-04: a staff or banner leans out of the arm, its shaft clear of the arm from 5 px above the fist; the shield's
        top stays below the head (body y 0) at rest."""
        for iid, w in self.ws.items():
            if w.hold != "staff": continue
            part, off = self.L.ARM[self.L.HOLDS[w.hold][0]]
            for h in range(5, 21):
                p = [a + b for a, b in zip(self.L.hold_point(w.hold, (0, h, 0)), off)]
                if -2 <= p[1] <= 10: self.assertTrue(p[0] < -3.8 or p[0] > 1.8 or p[2] < -2.8 or p[2] > 2.8, (iid, h, p))
        part, off = self.L.ARM[self.L.HOLDS["shield"][0]]
        top = min(self.L.hold_point("shield", c)[1] + off[1] + 2 for c in ([x, y, z] for b in self.ws["kite_shield"].cubes(5) for x in (b.lo[0], b.hi[0]) for y in (b.lo[1], b.hi[1]) for z in (b.lo[2], b.hi[2])))
        self.assertGreaterEqual(top, 0, "the shield reaches the head")

class GearIcons(unittest.TestCase):
    def test_item_icons(self):
        """Every item id at every rarity: 32 x 32 RGBA, a clear 1 px margin, no House ramp, Common unlike Legendary."""
        import icons, graveborn, tidecallers, wildmoot
        banned = {tuple(c[:3]) for R in (graveborn.VIOLET, tidecallers.SEA, wildmoot.OCHRE) for c in R}
        ics = icons.item_icons()
        ids = [i["id"] for i in gearlib.ids()["items"]]
        self.assertEqual(set(ics), {(i, r) for i in ids for r in gearlib.RARITIES})
        for iid in ids:
            looks = {}
            for r in gearlib.RARITIES:
                im = ics[(iid, r)]
                self.assertEqual((im.mode, im.size), ("RGBA", (32, 32)), (iid, r))
                for x in range(32):
                    for y in (0, 31):
                        self.assertEqual(im.getpixel((x, y))[3], 0, (iid, r)); self.assertEqual(im.getpixel((y, x))[3], 0, (iid, r))
                px = tuple(pixels(im))
                self.assertGreater(sum(1 for p in px if p[3]), 60, (iid, r))
                self.assertFalse({p[:3] for p in px if p[3]} & banned, (iid, r))
                looks[r] = px
            self.assertNotEqual(looks[1], looks[5], iid)   # the slot border carries rarity; middle tiers may fold alike
    def test_atlas_rows(self):
        """Every item at every rarity and every relic has a fixed 32 x 32 row in the UI atlas, and its PNG is there."""
        import json, icons
        gui = os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory", "textures", "gui")
        with open(os.path.join(gui, "atlas.json"), encoding="utf-8") as f: rows = json.load(f)["textures"]
        want = ["gear/%s_r%d" % (i["id"], r) for i in gearlib.ids()["items"] for r in gearlib.RARITIES] + ["relic/" + k for k in icons.RELICS]
        for name in want:
            self.assertIn(name, rows)
            self.assertEqual((rows[name]["kind"], rows[name]["w"], rows[name]["h"]), ("fixed", 32, 32), name)
            self.assertTrue(os.path.isfile(os.path.join(gui, name + ".png")), name)

if __name__ == "__main__": unittest.main()
