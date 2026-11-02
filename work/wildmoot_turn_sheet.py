"""Wildmoot tier I turn sheet: each troop's five views (front, 3/4, side, back, rear 3/4) rendered offline at one
texel = one pixel, then scaled 4x nearest-neighbour, one row per troop. python work/wildmoot_turn_sheet.py [side]"""
import os, sys, importlib.util
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import render
IDS = ["wm_tusk", "wm_hound", "wm_hunter", "wm_stalker", "wm_seer", "wm_oldhorn"]
VIEWS = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
side = sys.argv[1] if len(sys.argv) > 1 else "crimson"
rows = []
for tid in IDS:
    p = os.path.join(ROOT, "art", "hand", tid, "build_%s.py" % tid)
    sys.path.insert(0, os.path.dirname(p))
    spec = importlib.util.spec_from_file_location("b_" + tid, p); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    T = [v for v in vars(m).values() if v.__class__.__name__ == "Troop"][0]
    B, skins, cloths = T.build()
    if T.lit: skins[1] = Image.alpha_composite(skins[1], T.lit)
    ims = [render.render(B, T.visible(1), {"skin": skins[1], "cloth": cloths[side]}, y, pt, px=1) for y, pt in VIEWS]
    rows.append(render.sheet_of(ims, pad=4))
W = max(r.width for r in rows); out = Image.new("RGBA", (W, sum(r.height for r in rows)), (40, 42, 50, 255)); y = 0
for r in rows: out.paste(r, (0, y)); y += r.height
out = out.resize((out.width * 4, out.height * 4), Image.NEAREST)
dst = os.path.join(ROOT, "work", "wildmoot-t1-turn-4x-%s.png" % side); out.save(dst); print(dst, out.size)
