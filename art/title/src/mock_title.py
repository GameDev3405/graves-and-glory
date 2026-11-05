"""Quick mock of the title menu over the key art: the lockup, tagline, menu column, marker and version placed as in
design-archive/ui-concepts/title/TITLE.md (960x540 GUI px, drawn here at 2560x1440, a scale of 8/3).

Usage: python mock_title.py KEY_ART.png OUT.png
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
SPRITES = ROOT / 'design-archive/ui-concepts/title/sprites'
MARCELLUS = ROOT / 'src/main/resources/assets/gravesandglory/font/marcellus.ttf'
S = 2560 / 960
IVORY, GREY, SHADOW = (237, 227, 207), (150, 144, 138), (10, 8, 10)


def gui(v):
    return int(round(v * S))


def main(src, out):
    im = Image.open(src).convert('RGBA')
    d = ImageDraw.Draw(im)
    lock = Image.open(SPRITES / 'lockup.png').convert('RGBA')
    lock = lock.resize((gui(lock.width), gui(lock.height)), Image.NEAREST)
    im.alpha_composite(lock, (gui(240), gui(24)))
    big, small = ImageFont.truetype(str(MARCELLUS), gui(16)), ImageFont.truetype(str(MARCELLUS), gui(11))

    def text(xy, s, f, col, anchor='la'):
        d.text((xy[0] + 3, xy[1] + 3), s, font=f, fill=SHADOW, anchor=anchor)
        d.text(xy, s, font=f, fill=col, anchor=anchor)

    text((gui(480), gui(88)), 'Collect an army. Command the battlefield.', small, IVORY, 'ma')
    lines = [('Ride to camp', 396, big), ('Play with friends', 424, small), ('Settings', 448, small),
             ('Credits', 472, small), ('Quit', 496, small)]
    for s, y, f in lines:
        text((gui(40), gui(y)), s, f, IVORY)
    mk = Image.open(SPRITES / 'marker.png').convert('RGBA')
    mk = mk.resize((gui(mk.width), gui(mk.height)), Image.NEAREST)
    im.alpha_composite(mk, (gui(20), gui(396) + gui(8) - mk.height // 2))
    text((gui(944), gui(496) + gui(11)), '1.0.0', small, GREY, 'rs')
    im.convert('RGB').save(out)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
