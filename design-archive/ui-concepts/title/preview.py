"""Zoomed previews for the pixel art (written to the scratchpad, never into work/).
python preview.py frames            -> 4 commander frames, day and night, at 3x on the panorama
python preview.py scene [day|night] -> the whole overlay stack (no text) at 1x, and a 2x crop round the commander"""
import os, sys, tempfile
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SP = os.environ.get('SCRATCH') or os.path.join(tempfile.gettempdir(), 'gg-title-previews')
os.makedirs(SP, exist_ok=True)
CMD_AT = (608, 110)   # where the commander sprite's top-left sits on the 960x540 GUI canvas


def sprite(name):
    return Image.open(os.path.join(HERE, 'sprites', name)).convert('RGBA')


def gui_bg(tod):
    return Image.open(os.path.join(HERE, 'bg', f'pano-{tod}.png')).convert('RGBA')


def frames():
    rows = []
    for tod in ('day', 'night'):
        bg = gui_bg(tod)
        tiles = []
        for f in range(4):
            spr = sprite(f'commander-{tod}-{f}.png')
            w, h = spr.size
            x0, y0 = CMD_AT
            crop = bg.crop((x0 * 2, y0 * 2, (x0 + w) * 2, (y0 + h) * 2))
            big = spr.resize((w * 2, h * 2), Image.NEAREST)
            crop.alpha_composite(big)
            tiles.append(crop.resize((w * 3, h * 3), Image.NEAREST))
        row = Image.new('RGBA', (sum(t.width for t in tiles) + 12 * 3, tiles[0].height), (0, 0, 0, 255))
        x = 0
        for t in tiles:
            row.paste(t, (x, 0)); x += t.width + 12
        rows.append(row)
    out = Image.new('RGBA', (rows[0].width, sum(r.height for r in rows) + 12), (0, 0, 0, 255))
    out.paste(rows[0], (0, 0)); out.paste(rows[1], (0, rows[0].height + 12))
    out.save(os.path.join(SP, 'cmd-frames.png'))
    print('cmd-frames.png', out.size)


def layer(base, name, at):
    spr = sprite(name)
    base.alpha_composite(spr.resize((spr.width * 2, spr.height * 2), Image.NEAREST), (at[0] * 2, at[1] * 2))


def scene(tod, frame=0):
    base = gui_bg(tod)
    for extra in ('volley', 'embers'):
        p = os.path.join(HERE, 'sprites', f'{extra}-{tod}.png')
        if os.path.exists(p) and extra == 'volley':
            layer(base, f'{extra}-{tod}.png', (0, 0))
    layer(base, f'hill-{tod}.png', (0, 240))
    layer(base, f'commander-{tod}-{frame}.png', CMD_AT)
    p = os.path.join(HERE, 'sprites', f'embers-{tod}.png')
    if os.path.exists(p):
        layer(base, f'embers-{tod}.png', (0, 0))
    base.save(os.path.join(SP, f'scene-{tod}.png'))
    base.crop((1000, 200, 1960, 1080)).save(os.path.join(SP, f'scene-{tod}-crop.png'))
    print('scene', tod)


if __name__ == '__main__':
    os.makedirs(SP, exist_ok=True)
    if sys.argv[1] == 'frames':
        frames()
    elif sys.argv[1] == 'scene':
        for tod in (sys.argv[2:] or ['day', 'night']):
            scene(tod)
