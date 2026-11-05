"""Wildmoot weapon-in-hand check: each troop at tiers I, II and III (3/4 and side views) with its weapon at the hand bone (the runtime weapon model, grip applied),
front, 3/4, side and back, offline at 1 texel = 1 px, scaled up.  python work/wildmoot_weapon_check.py [out.png] [scale]"""
import os, sys, json, importlib.util
from PIL import Image, ImageDraw
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import render
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory")
dst = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "work", "wildmoot-weapons-check.png")
SC = int(sys.argv[2]) if len(sys.argv) > 2 else 5
VIEWS = [(-35, 12), (-90, 0)]
rows = []
for tid in (os.environ.get("GB_ONLY") or "wm_hound,wm_oldhorn,wm_seer,wm_stalker").split(","):
    p = os.path.join(ROOT, "art", "hand", tid, "build_%s.py" % tid); sys.path.insert(0, os.path.dirname(p))
    spec = importlib.util.spec_from_file_location("b_" + tid, p); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    T = [v for v in vars(m).values() if v.__class__.__name__ == "Troop"][0]
    B, skins, cloths = T.build()
    meta = json.load(open(os.path.join(ROOT, "art", "troops", tid + ".json")))
    ims = []
    for t in (1, 2, 3):
        w = meta["weapon"] if t == 1 else "%s_t%d" % (meta["weapon"], t); extra = []
        if meta["weapon"] != "none":
            d = json.load(open(os.path.join(RES, "models", "troop", "weapon", w + ".json")))
            img = Image.open(os.path.join(RES, "textures", "entity", "troop", "weapon", w + ".png")).convert("RGBA")
            extra = [(meta["hand"] + "_hand", d["grip"]["offset"], d["grip"]["rotation"], d["frames"][0]["quads"], img)]
        sk = Image.alpha_composite(skins[t], T.lit) if T.lit else skins[t]
        ims += [render.render(B, T.visible(t), {"skin": sk, "cloth": cloths["crimson"]}, y, pt, px=1, extra=extra, bg=(150, 154, 166, 255)) for y, pt in VIEWS]
    rows.append(render.sheet_of(ims, pad=4))
rows = [r.resize((r.width * SC, r.height * SC), Image.NEAREST) for r in rows]
out = Image.new("RGBA", (max(r.width for r in rows), sum(r.height for r in rows)), (40, 42, 50, 255)); y = 0
for r in rows: out.paste(r, (0, y)); y += r.height
out.save(dst); print(dst, out.size)
