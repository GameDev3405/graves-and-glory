"""4x sheet of the boar spear tiers I, II and III side by side (front, 3/4, side views each)."""
import os, sys
from PIL import Image, ImageDraw
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "art", "hand", "weapons"))
import build_swords as W
import build_house_weapons as H

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "work", "wildmoot-boar-spear-tiers-4x.png")
LABELS = {"boar_spear": "tier I", "boar_spear_t2": "tier II", "boar_spear_t3": "tier III"}
tiles = []
for wid in LABELS:
    make = H.TIERS[wid][0]
    cubes = make()
    tiles.append([W.render(cubes, y, p) for y, p in ((0, 0), (35, 15), (90, 0))])
th = max(t.height for row in tiles for t in row)
gap, pad = 10, 14
rw = [sum(t.width for t in row) + gap * (len(row) - 1) for row in tiles]
W_, H_ = sum(rw) + pad * (len(tiles) + 1) + 40, th + 2 * pad + 14
sheet = Image.new("RGBA", (W_, H_), (40, 42, 50, 255))
d = ImageDraw.Draw(sheet); x = pad
for (wid, lab), row, w in zip(LABELS.items(), tiles, rw):
    xx = x
    for t in row: sheet.paste(t, (xx, 14 + th - t.height), t); xx += t.width + gap
    d.text((x, 2), lab, fill=(220, 220, 220, 255)); x += w + pad + 13
sheet = sheet.resize((sheet.width * 3, sheet.height * 3), Image.NEAREST)
sheet.save(OUT); print(OUT, sheet.size)
