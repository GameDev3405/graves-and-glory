"""Zoomed previews of the UI sprites (written to the scratchpad, never into work/).
python preview_ui.py -> ui-sheet.png: the lockup at 2x over the day sky, the marker and horn at 8x"""
import os
from PIL import Image
from preview import SP, HERE


def main():
    bg = Image.open(os.path.join(HERE, 'bg', 'pano-day.png')).convert('RGBA')
    lk = Image.open(os.path.join(HERE, 'sprites', 'lockup.png')).convert('RGBA')
    at = ((960 - lk.width) // 2, 18)
    sky = bg.crop((at[0] * 2, at[1] * 2, (at[0] + lk.width) * 2, (at[1] + lk.height) * 2))
    sky.alpha_composite(lk.resize((lk.width * 2, lk.height * 2), Image.NEAREST))
    tiles = [sky]
    for n in ('marker', 'horn'):
        s = Image.open(os.path.join(HERE, 'sprites', n + '.png')).convert('RGBA')
        t = Image.new('RGBA', (s.width * 8 + 16, s.height * 8 + 16), (26, 22, 30, 255))
        t.alpha_composite(s.resize((s.width * 8, s.height * 8), Image.NEAREST), (8, 8))
        tiles.append(t)
    W = max(t.width for t in tiles); H = sum(t.height + 8 for t in tiles)
    out = Image.new('RGBA', (W, H), (0, 0, 0, 255))
    y = 0
    for t in tiles:
        out.paste(t, (0, y)); y += t.height + 8
    out.save(os.path.join(SP, 'ui-sheet.png'))
    print('ui-sheet.png', out.size)


if __name__ == '__main__':
    main()
