"""Wildmoot Tusk tiers I-III and the six Wildmoot veteran trims (worn over tier II): five views each, one texel = one pixel,
scaled 4x nearest-neighbour, with a label and self-score per row.   python work/wildmoot_tusk_trims_sheet.py [side] [out.png]"""
import os, sys, importlib.util
from PIL import Image, ImageDraw
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "common"))
import render
IDS = ["wm_tusk", "wm_hound", "wm_hunter", "wm_stalker", "wm_seer", "wm_oldhorn"]
VIEWS = [(0, 0), (-35, 12), (-90, 0), (180, 0), (145, 12)]
# self-scores out of 10, set by the artist after reading the sheet
SCORES = {"wm_tusk tier I": "8 (as built)", "wm_tusk tier II": "8", "wm_tusk tier III": "8 (crown redone)", "wm_tusk vet (II)": "7", "wm_hound vet (II)": "7.5 (bone bands)", "wm_hunter vet (II)": "7", "wm_stalker vet (II)": "7", "wm_seer vet (II)": "7", "wm_oldhorn vet (II)": "7"}
side = sys.argv[1] if len(sys.argv) > 1 else "crimson"
dst = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "work", "wildmoot-tusk-trims-4x.png")
rows, labels = [], []
for tid in IDS:
    p = os.path.join(ROOT, "art", "hand", tid, "build_%s.py" % tid)
    sys.path.insert(0, os.path.dirname(p))
    spec = importlib.util.spec_from_file_location("b_" + tid, p); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    T = [v for v in vars(m).values() if v.__class__.__name__ == "Troop"][0]
    B, skins, cloths = T.build()
    def row(t, vet):
        sk = skins[t]
        if vet: sk = Image.alpha_composite(sk, T.trim)
        if T.lit: sk = Image.alpha_composite(sk, T.lit)
        return render.sheet_of([render.render(B, T.visible(t), {"skin": sk, "cloth": cloths[side]}, y, pt, px=1) for y, pt in VIEWS], pad=4)
    if tid == "wm_tusk":
        for t in T.tiers: rows.append(row(t, False)); labels.append("%s tier %s" % (tid, "I" * t))
    rows.append(row(2, True)); labels.append("%s vet (II)" % tid)
LW = 92
W = max(r.width for r in rows) + LW
out = Image.new("RGBA", (W, sum(r.height for r in rows)), (40, 42, 50, 255)); y = 0
dr = ImageDraw.Draw(out)
for r, lab in zip(rows, labels):
    out.paste(r, (LW, y))
    dr.text((2, y + 2), lab, fill=(220, 220, 210, 255))
    dr.text((2, y + 12), "self " + SCORES.get(lab, "-"), fill=(240, 200, 120, 255))
    y += r.height
out = out.resize((out.width * 4, out.height * 4), Image.NEAREST)
out.save(dst); print(dst, out.size)
