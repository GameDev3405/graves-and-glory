"""v4-detail-crops.png: four crops taken 1:1 from the 1920 x 1080 renders (GUI scale 2), so the detail can
be judged at the size the game shows it. Run after render.py --all-v4."""
import os
from PIL import Image, ImageDraw, ImageFont
HERE = os.path.dirname(os.path.abspath(__file__))
CROPS = [
    ('v4-ui-war-table-home.png', (1472, 368, 360, 400), 'The Rivergate map card', 'home, the field card'),
    ('v4-ui-war-table-home.png', (896, 320, 136, 132), 'A wax seal in relief', 'home, the Skirmish order'),
    ('v4-hud-iron-ledger.png', (20, 672, 212, 132), 'An iron corner', 'HUD, the Infantry card'),
    ('v4-ui-war-table-home.png', (868, 436, 184, 172), 'A mode drawing', 'home, Skirmish'),
]
PAD, GAP, TOP, COL = 32, 32, 96, 236
W = PAD * 2 + sum(max(COL, c[1][2]) for c in CROPS) + GAP * (len(CROPS) - 1)
H = TOP + max(c[1][3] for c in CROPS) + 72
out = Image.new('RGB', (W, H), '#17110d')
d = ImageDraw.Draw(out)
title = ImageFont.truetype(os.path.join(HERE, 'fonts', 'marcellus.ttf'), 28)
lab = ImageFont.truetype(os.path.join(HERE, 'fonts', 'marcellus.ttf'), 20)
small = ImageFont.truetype(os.path.join(HERE, 'fonts', 'lato.ttf'), 16)
d.text((PAD, 24), 'v4 detail at native size', font=title, fill='#eee9df')
d.text((PAD + 330, 34), 'each crop is 1:1 from the 1920 x 1080 render (GUI scale 2): one GUI pixel is 2 x 2 screen pixels',
       font=small, fill='#afb9bc')
x = PAD
for (src, (cx, cy, cw, ch), name, where) in CROPS:
    im = Image.open(os.path.join(HERE, src)).convert('RGB').crop((cx, cy, cx + cw, cy + ch))
    out.paste(im, (x, TOP))
    d.rectangle((x - 1, TOP - 1, x + cw, TOP + ch), outline='#5c4a32')
    d.text((x, TOP + ch + 12), name, font=lab, fill='#eee9df')
    d.text((x, TOP + ch + 38), where, font=small, fill='#afb9bc')
    x += max(COL, cw) + GAP
out.save(os.path.join(HERE, 'v4-detail-crops.png'))
print('v4-detail-crops.png', W, H)
