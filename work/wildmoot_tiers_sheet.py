"""Wildmoot tiers II/III review sheet: for each troop, rows of tiers I, II and III in five views (front, 3/4, side, back,
rear 3/4), rendered offline at one texel = one pixel, scaled 4x nearest-neighbour.
python work/wildmoot_tiers_sheet.py [side] [out.png] [troop ...]"""
import os, sys, importlib.util
from PIL import Image, ImageDraw
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import render
IDS = ["wm_hound", "wm_hunter", "wm_stalker", "wm_seer", "wm_oldhorn"]
VIEWS = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
side = sys.argv[1] if len(sys.argv) > 1 else "crimson"
dst = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "work", "wildmoot-tiers-II-III-4x.png")
ids = sys.argv[3:] or IDS
rows = []
for tid in ids:
    p = os.path.join(ROOT, "art", "hand", tid, "build_%s.py" % tid)
    sys.path.insert(0, os.path.dirname(p))
    spec = importlib.util.spec_from_file_location("b_" + tid, p); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    T = [v for v in vars(m).values() if v.__class__.__name__ == "Troop"][0]
    B, skins, cloths = T.build()
    for t in T.tiers:
        sk = Image.alpha_composite(skins[t], T.lit) if T.lit else skins[t]
        ims = [render.render(B, T.visible(t), {"skin": sk, "cloth": cloths[side]}, y, pt, px=1) for y, pt in VIEWS]
        rows.append(render.sheet_of(ims, pad=4))
W = max(r.width for r in rows); out = Image.new("RGBA", (W, sum(r.height for r in rows)), (40, 42, 50, 255)); y = 0
for r in rows: out.paste(r, (0, y)); y += r.height
out = out.resize((out.width * 4, out.height * 4), Image.NEAREST)
out.save(dst); print(dst, out.size)
