"""Graveborn tiers II/III review sheet: for each troop, rows of tiers I, II and III in five views (front, 3/4, side, back,
rear 3/4), rendered offline at one texel = one pixel, scaled 4x nearest-neighbour, with a label column.
python work/graveborn_tiers_sheet.py [side] [out.png] [troop ...]     (GB_SCORES="revenant=7,reaper=6" prints scores in the labels)"""
import os, sys, importlib.util
from PIL import Image, ImageDraw, ImageFont
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import render
NAMES = {"revenant": "Risen / Revenant / Grave Knight", "bonebow": "Bone Archer / Barrow Archer / Deadeye",
         "reaper": "Grave Reaper / Harvester / Lord of Scythes", "stalker": "Barrow / Frost / Wight Stalker",
         "hexer": "Gravecaller I / II / III", "gb_hollowking": "Hollow King I / II / III"}
VIEWS = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
side = sys.argv[1] if len(sys.argv) > 1 else "crimson"
dst = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "work", "graveborn-tiers-II-III-4x.png")
ids = sys.argv[3:] or list(NAMES)
scores = dict(kv.split("=") for kv in os.environ.get("GB_SCORES", "").split(",") if "=" in kv)
try: font = ImageFont.truetype("arialbd.ttf", 18)
except OSError: font = ImageFont.load_default()
LABEL = 250
SC = int(os.environ.get("GB_SCALE", "4"))
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
        rows.append((tid, t, render.sheet_of(ims, pad=4)))
rows = [(a, t, r.resize((r.width * SC, r.height * SC), Image.NEAREST)) for a, t, r in rows]
W = LABEL + max(r.width for _, _, r in rows)
out = Image.new("RGBA", (W, sum(r.height for _, _, r in rows)), (40, 42, 50, 255)); d = ImageDraw.Draw(out); y = 0
for tid, t, r in rows:
    out.paste(r, (LABEL, y))
    d.line([(0, y), (W, y)], fill=(70, 74, 86, 255) if t > 1 else (130, 134, 150, 255), width=1 if t > 1 else 3)
    if t == 1: d.text((10, y + 8), NAMES[tid], fill=(235, 235, 225, 255), font=font)
    d.text((10, y + r.height // 2 - 10), "Tier " + "I" * t, fill=(200, 190, 230, 255), font=font)
    if t == 3 and tid in scores: d.text((10, y + r.height - 30), "self-score %s/10" % scores[tid], fill=(235, 225, 160, 255), font=font)
    y += r.height
out.save(dst); print(dst, out.size)
