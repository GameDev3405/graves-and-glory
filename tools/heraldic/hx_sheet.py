"""Heraldic-units contact sheet builder (my own grading sheet, Kort's 9/10 bar).

Renders troops through the same offline orthographic renderer the project uses
(work/p2b/preview.py) at four yaws, and lays them out as:
  - the colour render at 4x (the paint check),
  - the 36 px silhouette (the game's read size, the rubric's size),
  - the 36 px colour (how it actually looks in the field).

  python work/p2b/hx_sheet.py <out.png> [troop[:stage] ...]

The default set is the six Gilded Order troops, stage 1.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import numpy as np
from PIL import Image, ImageDraw

import preview as PV

BG = (36, 34, 42)
INK = (230, 225, 210)
MUTED = (155, 151, 142)
FIELD_H = 40
BAND = 22


def silhouette(view, bg=(255, 255, 255)):
    """The troop's mask: every pixel that is not the flat background."""
    a = np.array(view).astype(int)
    mask = (np.abs(a - np.array(bg)).sum(axis=2) > 0)
    out = np.full(a.shape, 235, dtype=np.uint8)
    out[mask] = np.array((28, 28, 32), dtype=np.uint8)
    return Image.fromarray(out)


def panel(troop, stage, side, yaw=30, scale=5, zoom=36):
    """One troop: the 4-view colour render on top, the 36 px colour and silhouette below."""
    views = PV.render(troop, stage, side, yaws=(0, 30, 90, 150), scale=scale)
    w, h = views[0].size
    strip = Image.new('RGB', (w * len(views), h), BG)
    for i, v in enumerate(views):
        strip.paste(v, (i * w, 0))
    small = [v.resize((max(1, w * zoom // h), zoom), Image.BOX) for v in views]
    sils = [silhouette(v) for v in views]
    sw = small[0].width
    low = Image.new('RGB', (w * len(views), zoom + 4), BG)
    for i, v in enumerate(small):
        low.paste(v, (i * w + (w - sw) // 2, 2))
    low2 = Image.new('RGB', (w * len(views), zoom + 4), BG)
    for i, v in enumerate(sils):
        low2.paste(v, (i * w + (w - sw) // 2, 2))
    body = Image.new('RGB', (w * len(views), h + zoom * 2 + 12), BG)
    body.paste(strip, (0, 0))
    body.paste(low, (0, h + 4))
    body.paste(low2, (0, h + 8 + zoom))
    return body


def main(out, troops):
    panels = []
    for t in troops:
        name, _, st = t.partition(':')
        panels.append((t, panel(name, int(st or 1), 'gold')))
    pw, ph = panels[0][1].size
    cols = min(3, len(panels))
    rows = (len(panels) + cols - 1) // cols
    W = cols * (pw + 10) + 10
    H = rows * (ph + BAND + 10) + 30
    sheet = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), 'Heraldic units: colour render (4 yaws), 36 px colour, 36 px silhouette', fill=INK)
    for i, (label, body) in enumerate(panels):
        x = 10 + (i % cols) * (pw + 10)
        y = 26 + (i // cols) * (ph + BAND + 10)
        d.text((x, y), label, fill=MUTED)
        sheet.paste(body, (x, y + BAND))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    out = pathlib.Path(args[0])
    troops = args[1:] or ['knight', 'archer', 'sentinel', 'longbow', 'mage', 'gl_marshal']
    main(out, troops)
