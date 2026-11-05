"""Graveborn tier I turn sheet: each troop's five views (front, 3/4, side, back, rear 3/4) rendered offline at one
texel = one pixel, then scaled 4x nearest-neighbour, one row per troop. python work/graveborn_turn_sheet.py [side]"""
import os, sys, importlib.util
from PIL import Image, ImageDraw, ImageFont
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import render
IDS = [("revenant", "Risen"), ("bonebow", "Bone Archer"), ("reaper", "Grave Reaper"), ("stalker", "Barrow Stalker"),
       ("hexer", "Gravecaller"), ("gb_hollowking", "The Hollow King")]
IDS = [i for i in IDS if (len(sys.argv) < 3 or i[0] in sys.argv[2:]) and os.path.exists(os.path.join(ROOT, "art", "hand", i[0]))]
VIEWS = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
side = sys.argv[1] if len(sys.argv) > 1 else "crimson"
rows = []
for tid, name in IDS:
    p = os.path.join(ROOT, "art", "hand", tid, "build_%s.py" % tid)
    sys.path.insert(0, os.path.dirname(p))
    spec = importlib.util.spec_from_file_location("b_" + tid, p); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    T = [v for v in vars(m).values() if v.__class__.__name__ == "Troop"][0]
    B, skins, cloths = T.build()
    if T.lit: skins[1] = Image.alpha_composite(skins[1], T.lit)
    ims = [render.render(B, T.visible(1), {"skin": skins[1], "cloth": cloths[side]}, y, pt, px=1) for y, pt in VIEWS]
    rows.append((name, render.sheet_of(ims, pad=4)))
LABEL = 290   # px of label column at 4x
rows = [(n, r.resize((r.width * 4, r.height * 4), Image.NEAREST)) for n, r in rows]
W = LABEL + max(r.width for _, r in rows); out = Image.new("RGBA", (W, sum(r.height for _, r in rows)), (40, 42, 50, 255)); y = 0
try: font = ImageFont.truetype("arialbd.ttf", 22)
except OSError: font = ImageFont.load_default()
d = ImageDraw.Draw(out)
for n, r in rows:
    out.paste(r, (LABEL, y)); d.text((12, y + r.height // 2 - 12), n, fill=(235, 235, 225, 255), font=font)
    d.line([(0, y), (W, y)], fill=(70, 74, 86, 255)); y += r.height
dst = os.environ.get("GB_OUT") or os.path.join(ROOT, "work", "graveborn-t1-turn-4x-%s.png" % side); out.save(dst); print(dst, out.size)
