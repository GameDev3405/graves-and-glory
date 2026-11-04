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
def built():
    if not _cache:
        for s in SETS:
            for p in importlib.import_module(s).pieces(): _cache[p.item_id] = (s, p, p.build())
    return _cache

class GearArt(unittest.TestCase):
    def setUp(self):
        if not SETS: self.skipTest("no set modules yet")

    def test_ids_known(self):
        items = {i["id"]: i for i in gearlib.ids()["items"]}
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

    def test_every_rarity_paints(self):
        for iid, (_, p, (B, skins, _, _)) in built().items():
            for r in gearlib.RARITIES:
                for bone in B:
                    if not on(bone, r) or bone.team: continue
                    for b in bone.boxes:
                        x0, y0 = rects(b)["front"]; w, h = int(b.size[0]), int(b.size[1])
                        if w and h: self.assertEqual(skins[r].getpixel((x0, y0))[3], 255, (iid, r, bone.name))

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
            for yaw in (0, -90):
                self.assertGreaterEqual(area(ps, 5, yaw), 1.06 * area(ps, 1, yaw), (s, yaw))

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

if __name__ == "__main__": unittest.main()
