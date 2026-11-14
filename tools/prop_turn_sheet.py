"""Contact sheet of a prop's turntable shots (tools/prop_shot.py --turntable): work/props/review/<id>-turntable.jpg.

    python tools/prop_turn_sheet.py <id>[,<id>...]
Reads work/props/review/shots/<id>/p1-prop-<id>-*.png; 4 columns: 8 day angles, the two close-ups, 4 night angles, each labelled.
"""
import pathlib, sys
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parents[1]
ORDER = ['turn-%03d' % a for a in range(0, 360, 45)] + ['close-day', 'close-night'] + ['turn-night-%03d' % a for a in range(0, 360, 90)]


def sheet(pid, cols=4, w=480):
    src = ROOT / 'work' / 'props' / 'review' / 'shots' / pid
    tiles = [(n, src / ('p1-prop-%s-%s.png' % (pid, n))) for n in ORDER]
    tiles = [(n, p) for n, p in tiles if p.is_file()]
    if not tiles: sys.exit('no turntable shots for %s in %s' % (pid, src))
    first = Image.open(tiles[0][1]); h = round(w * first.height / first.width)
    rows = (len(tiles) + cols - 1) // cols
    out = Image.new('RGB', (cols * w, rows * h), (20, 20, 24)); d = ImageDraw.Draw(out)
    for i, (n, p) in enumerate(tiles):
        x, y = (i % cols) * w, (i // cols) * h
        out.paste(Image.open(p).convert('RGB').resize((w, h), Image.LANCZOS), (x, y))
        d.rectangle((x, y, x + 8 * len(n) + 12, y + 16), fill=(0, 0, 0)); d.text((x + 6, y + 2), n, fill=(255, 255, 255))
    dest = ROOT / 'work' / 'props' / 'review' / (pid + '-turntable.jpg')
    out.save(dest, quality=86); print(dest, len(tiles), 'shots')


if __name__ == '__main__':
    for pid in sys.argv[1].split(','): sheet(pid)
