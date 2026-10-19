"""Heraldic units: the final A/B sheet, one troop per block, baseline against mine.

Each block: the baseline's four yaws and 36 px strip on the left, mine on the
right. This is the sheet the grade is read off.

  python work/p2b/hx_ab.py <baseline assets> <my assets> <out.png> [troop ...]
"""
import pathlib
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from hx_compare import render  # noqa: E402

BG = (36, 34, 42)
INK = (230, 225, 210)
MUTED = (160, 156, 146)
BLOCK = 330
W = 1180


def main(baseline, mine, out, troops):
    rows = (len(troops) + 1) // 2
    sheet = Image.new('RGB', (W, 40 + rows * BLOCK), BG)
    d = ImageDraw.Draw(sheet)
    d.text((12, 12), 'BASELINE (left)   vs   HERALDIC (right)   -   four yaws, 36 px read strip above',
           fill=INK)
    for i, t in enumerate(troops):
        col, row = i % 2, i // 2
        x0 = 12 + col * 580
        y0 = 34 + row * BLOCK
        d.text((x0, y0), t, fill=INK)
        for c, root in enumerate((baseline, mine)):
            views = render(root, t)
            w, h = views[0].size
            x = x0 + c * 280
            for j, v in enumerate(views):
                small = v.resize((max(1, w * 36 // h), 36), Image.BOX)
                sheet.paste(small, (x + j * 26, y0 + 16))
                sheet.paste(v.resize((w // 3, h // 3), Image.LANCZOS), (x + j * 66, y0 + 56))
            d.text((x, y0 + 300), 'baseline' if c == 0 else 'heraldic', fill=MUTED)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(out, sheet.size)


if __name__ == '__main__':
    args = sys.argv[1:]
    troops = args[3:] or ['knight', 'archer', 'sentinel', 'longbow', 'mage', 'gl_marshal']
    main(pathlib.Path(args[0]), pathlib.Path(args[1]), pathlib.Path(args[2]), troops)
