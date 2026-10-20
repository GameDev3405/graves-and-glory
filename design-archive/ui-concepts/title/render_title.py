"""Render and lint the title mockups with the shared pipeline (../render.py, ../check.js), then build the
banner wind strip.

render.py only switches Chromium to greyscale text antialiasing for pages named v2-/v3-/v4-; these pages need
the same (LCD subpixel AA leaves colour fringes once the page is doubled), so this wrapper adds the flags.

python render_title.py          -> title-hill-day.png, title-hill-night.png, title-hill-hover.png, title-check.json,
                                   title-banner-frames.png"""
import json, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import render  # noqa: E402  (the shared ui-concepts pipeline)

_flags = render._flags


def flags(page):
    f = _flags(page)
    return f if '--disable-lcd-text' in f else f + ['--disable-lcd-text', '--font-render-hinting=none']


render._flags = flags

PAGES = [('title-hill-day', 'bg/pano-day.png'), ('title-hill-night', 'bg/pano-night.png'),
         ('title-hill-hover', 'bg/pano-day.png')]


def banner_strip():
    """the 4 wind frames of the commander (a fold runs down the banner, the tails follow a frame behind), at 2x,
    over the panorama crop they stand on, with the frame number under each"""
    tiles = []
    bg = Image.open(os.path.join(HERE, 'bg', 'pano-day.png')).convert('RGBA')
    x0, y0 = json.load(open(os.path.join(HERE, 'sprites', 'commander-at.json')))   # where title_commander placed him
    boxes = [Image.open(os.path.join(HERE, 'sprites', f'commander-day-{f}.png')).getbbox() for f in range(4)]
    box = (min(b[0] for b in boxes) - 6, min(b[1] for b in boxes) - 6, max(b[2] for b in boxes) + 6,
           max(b[3] for b in boxes) + 4)                     # the whole figure, every frame
    for f in range(4):
        spr = Image.open(os.path.join(HERE, 'sprites', f'commander-day-{f}.png')).convert('RGBA')
        crop = bg.crop((x0 * 2, y0 * 2, (x0 + spr.width) * 2, (y0 + spr.height) * 2))
        crop.alpha_composite(spr.resize((spr.width * 2, spr.height * 2), Image.NEAREST))
        tiles.append(crop.crop((box[0] * 2, box[1] * 2, box[2] * 2, box[3] * 2)))
    pad = 16
    w = sum(t.width for t in tiles) + pad * 5
    h = tiles[0].height + pad * 2 + 20
    out = Image.new('RGBA', (w, h), (26, 33, 37, 255))           # charcoal token
    digits = digits_font()
    x = pad
    for i, t in enumerate(tiles):
        out.paste(t, (x, pad))
        draw_digits(out, digits, str(i), x + t.width // 2, pad + t.height + 6)
        x += t.width + pad
    out.convert('RGB').save(os.path.join(HERE, 'title-banner-frames.png'))
    print('wrote title-banner-frames.png', out.size)


def digits_font():
    """the frame numbers in Minecraft's own glyphs, cut from the real ascii.png (16 x 16 cells of 8 px)"""
    sheet = Image.open(os.path.join(HERE, '..', 'assets', 'ascii.png')).convert('RGBA')
    out = {}
    for d in '0123':
        c = ord(d)
        cell = sheet.crop(((c % 16) * 8, (c // 16) * 8, (c % 16) * 8 + 8, (c // 16) * 8 + 8))
        px = cell.load()
        out[d] = [''.join('#' if px[i, j][3] > 128 else '.' for i in range(8)) for j in range(8)]
    return out


def draw_digits(im, font, s, cx, top):
    """one Minecraft glyph at GUI scale 2, with the vanilla drop shadow one GUI pixel down and right"""
    px = im.load()
    rows = font[s]
    x0 = cx - 8
    for colour, off in (((62, 52, 34, 255), 2), ((200, 165, 107, 255), 0)):
        for j, r in enumerate(rows):
            for i, ch in enumerate(r):
                if ch == '#':
                    for dy in (0, 1):
                        for dx in (0, 1):
                            px[x0 + i * 2 + dx + off, top + j * 2 + dy + off] = colour


if __name__ == '__main__':
    summary = {}
    for page, bg in PAGES:
        print(page)
        r = render.report(f'title/{page}.html', f'title/{bg}', f'title/{page}.png')
        summary[page] = {'words': r.get('words'), 'sizes': r['sizeSteps'], 'issues': r['issues'],
                         'lowContrast': r['lowContrast'],
                         'minContrast': min((c['ratio'] for c in r['contrast']), default=None),
                         'contrast': r['contrast']}
        low = min(r['contrast'], key=lambda c: c['ratio'], default=None)
        if low:
            print(f"  lowest contrast {low['ratio']}: '{low['text'][:40]}' {low['color']} on {low['bg']}")
    with open(os.path.join(HERE, 'title-check.json'), 'w', encoding='utf-8') as f:
        json.dump(summary, f, indent=1)
    banner_strip()
