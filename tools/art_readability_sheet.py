"""Client-free 36 px readability sheet (ART-STYLE-GUIDE 10.7: a greyed-out lineup reads as separate Houses at 36 px).
For every troop skin troop/<id>/<id>_s1.png it paints the front view (head, body, arms, legs and the stage 1 parts on them)
straight from models/troop/<id>.json: each unrotated box's front (north) face, nearest-sampled from the skin, drawn far to
near with the card face as the face. Rotated bones (shield, cape, quiver, pauldrons) and hand weapons are left out. The
figure is box-filtered to 36 px tall, and all troops go side by side, colour row then greyed row, on work/readability-36px.png.
The guide gives no numeric threshold for this check, so the printed metrics (fill, mean luma, head/body/legs luma gaps) are
informational: no pass or fail is invented. Reads art, changes none.   python tools/art_readability_sheet.py"""
import json, pathlib, sys
from PIL import Image, ImageDraw
ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'
OUT = ROOT / 'work/readability-36px.png'
H, S = 36, 8   # target height; supersample per model px before the box filter

def luma(p): return 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2]

def boxes_of(model):
    bones = {b['name']: b for b in model['bones']}
    card = model.get('faces', {}).get('card', 0)
    def world(b):
        x = y = z = 0.0; n = b
        while n:
            x += n['pivot'][0]; y += n['pivot'][1]; z += n['pivot'][2]
            n = bones.get(n['parent'])
        return x, y, z
    def ok(b):
        while b:
            if any(b['rotation']) or (b['stages'] and 1 not in b['stages']): return False
            if b.get('variants') and b['variants'] != [card]: return False
            b = bones.get(b['parent'])
        return True
    out = []
    for b in bones.values():
        if not b['boxes'] or not ok(b): continue
        wx, wy, wz = world(b)
        for bx in b['boxes']:
            out.append((b['name'], wx, wy, wz, bx, b['sheet']))
    return out

def render(tid):
    model = json.loads((ASSETS / f'models/troop/{tid}.json').read_text('utf8'))
    skin = Image.open(ASSETS / f'textures/entity/troop/{tid}/{tid}_s1.png').convert('RGBA')
    size = model['size']
    items = []
    for name, wx, wy, wz, bx, sheet in boxes_of(model):
        o, s, uv, d = bx['origin'], bx['size'], bx['uv'], bx.get('dilation', [0, 0, 0])
        x0, y0 = wx + o[0] - d[0], wy + o[1] - d[1]
        w, h = s[0] + 2 * d[0], s[1] + 2 * d[1]
        fx, fy = skin.width / size[0], skin.height / size[1]
        u0, v0 = (uv[0] + s[2]) * fx, (uv[1] + s[2]) * fy
        tile = skin.crop((round(u0), round(v0), round(u0 + s[0] * fx), round(v0 + s[1] * fy)))
        if bx.get('mirror'): tile = tile.transpose(Image.FLIP_LEFT_RIGHT)
        if tile.width and tile.height: items.append((wz + o[2], name, x0, y0, w, h, tile))
    if not items: return None, None
    items.sort(key=lambda t: -t[0])   # far (+z) first, front last
    minx, miny = min(i[2] for i in items), min(i[3] for i in items)
    maxx, maxy = max(i[2] + i[4] for i in items), max(i[3] + i[5] for i in items)
    W, Hh = round((maxx - minx) * S), round((maxy - miny) * S)
    big = Image.new('RGBA', (W, Hh), (0, 0, 0, 0))
    for _, name, x0, y0, w, h, tile in items:
        t = tile.resize((max(1, round(w * S)), max(1, round(h * S))), Image.NEAREST)
        big.alpha_composite(t, (round((x0 - minx) * S), round((y0 - miny) * S)))
    wpx = max(1, round(W * H / Hh))
    # box filter on premultiplied colour so transparent texels do not darken the edge
    a = big.getchannel('A')
    pm = Image.merge('RGBA', [Image.composite(c, Image.new('L', big.size, 0), a) for c in big.split()[:3]] + [a])
    sm = pm.resize((wpx, H), Image.BOX)
    px = sm.load()
    for yy in range(H):
        for xx in range(wpx):
            r, g, b, al = px[xx, yy]
            px[xx, yy] = (min(255, r * 255 // al), min(255, g * 255 // al), min(255, b * 255 // al), al) if al else (0, 0, 0, 0)
    metrics = {'name': tid}
    cov = [(xx, yy) for yy in range(H) for xx in range(wpx) if px[xx, yy][3] >= 128]
    metrics['fill'] = len(cov) / (wpx * H)
    band = lambda lo, hi: [luma(px[x, y]) for x, y in cov if lo <= y < hi]
    avg = lambda v: sum(v) / len(v) if v else 0
    metrics['luma'] = avg(band(0, H))
    metrics['head'], metrics['body'], metrics['legs'] = avg(band(0, 7)), avg(band(7, 21)), avg(band(21, H))
    return sm, metrics

def main():
    troops = sorted(p.name for p in (ASSETS / 'textures/entity/troop').iterdir() if (p / f'{p.name}_s1.png').exists())
    cells = []; rows = []
    for t in troops:
        im, m = render(t)
        if im is None: print('%-14s NO RENDER (no unrotated boxes)' % t); continue
        cells.append((t, im)); rows.append(m)
    pad, lab = 6, 12
    cw = max(max(i.width for _, i in cells), 72) + pad
    sheet = Image.new('RGBA', (cw * len(cells) + pad, 2 * (H + lab) + 3 * pad), (128, 128, 128, 255))
    dr = ImageDraw.Draw(sheet)
    for k, (t, im) in enumerate(cells):
        x = pad + k * cw
        grey = im.convert('LA').convert('RGBA'); grey.putalpha(im.getchannel('A'))
        sheet.alpha_composite(im, (x + (cw - pad - im.width) // 2, pad))
        sheet.alpha_composite(grey, (x + (cw - pad - im.width) // 2, 2 * pad + H + lab))
        dr.text((x, pad + H), t, fill=(255, 255, 255, 255))
    OUT.parent.mkdir(exist_ok=True)
    sheet.save(OUT)
    print('wrote %s (%d troops, %dx%d, colour row then greyed row)' % (OUT.relative_to(ROOT), len(cells), sheet.width, sheet.height))
    print('%-14s %6s %6s %6s %6s %6s   (informational: no guide threshold)' % ('troop', 'fill', 'luma', 'head', 'body', 'legs'))
    for m in rows:
        print('%-14s %5.0f%% %6.0f %6.0f %6.0f %6.0f' % (m['name'], m['fill'] * 100, m['luma'], m['head'], m['body'], m['legs']))
    return 0
if __name__ == '__main__': sys.exit(main())
