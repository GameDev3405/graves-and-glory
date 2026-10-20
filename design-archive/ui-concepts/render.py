"""Render a mockup: headless Chromium at 960x540 (GUI scale 2 units) with a transparent page,
then nearest-neighbour x2 and composite over the 1920x1080 world background.

Usage:
  python render.py <page.html> <background.png> <out.png>      render one page (v1 behaviour)
  python render.py --check <page.html> <background.png> <out.png>  render, then lint layout and contrast
  python render.py --all-v2                                       render and lint the nine v2 screens
  python render.py --all-v3                                       render and lint the three v3 screens (v3-check.json)
  python render.py --all-v4                                       render and lint the three v4 screens (v4-check.json)

v2 pages (file name starting "v2-") are rendered with greyscale text antialiasing: the v1 renders used
Chrome's LCD subpixel AA, which leaves red/blue fringes once the page is scaled x2.
The lint (check.js, loaded by every v2 page) reports clipped, covered, crossed and spilling text, padding
under 8 px inside panels, content within 12 px of the screen edge, overlapping boxes and the type sizes
in use; this script adds a contrast ratio per text run measured on the final composited PNG."""
import json, os, re, subprocess, sys, tempfile
from PIL import Image

CHROME = r"C:\Users\kortt\AppData\Local\ms-playwright\chromium_headless_shell-1234\chrome-headless-shell-win64\chrome-headless-shell.exe"
HERE = os.path.dirname(os.path.abspath(__file__))

V2 = [  # page, background
    ('v2-hud-B-iron-ledger', 'bg/fight-v2.png'),
    ('v2-ui-A-war-table-home', 'bg/camp-v2.png'),
    ('v2-ui-A-war-table-recruit', 'bg/camp-v2.png'),
    ('v2-ui-B-field-ledger-home', 'bg/camp-v2.png'),
    ('v2-ui-B-field-ledger-recruit', 'bg/camp-v2.png'),
    ('v2-ui-C-hall-of-banners-home', 'bg/camp-v2.png'),
    ('v2-ui-C-hall-of-banners-recruit', 'bg/camp-v2.png'),
    ('v2-hud-A-marshals-glass', 'bg/fight-v2.png'),
    ('v2-hud-C-banner-line', 'bg/fight-v2.png'),
]

V3 = [  # third pass: the chosen family only (War Table screens, Iron Ledger HUD)
    ('v3-hud-iron-ledger', 'bg/fight-v2.png'),
    ('v3-ui-war-table-home', 'bg/camp-v2.png'),
    ('v3-ui-war-table-recruit', 'bg/camp-v2.png'),
]
V4 = [  # fourth pass: the same three screens at two to three times the detail, plus the battle event feed
    ('v4-hud-iron-ledger', 'bg/fight-v2.png'),
    ('v4-ui-war-table-home', 'bg/camp-v2.png'),
    ('v4-ui-war-table-recruit', 'bg/camp-v2.png'),
]

def _url(page, query=''):
    return 'file:///' + os.path.join(HERE, page).replace('\\', '/') + query

def _flags(page):
    f = ['--headless', '--allow-file-access-from-files', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1',
         '--default-background-color=00000000', '--virtual-time-budget=4000', '--window-size=960,540']
    if os.path.basename(page).startswith(('v2-', 'v3-', 'v4-')):
        f += ['--disable-lcd-text', '--font-render-hinting=none']
    return f

def render(page, background, out):
    bg_path = os.path.join(HERE, background); out_path = os.path.join(HERE, out)
    tmp = os.path.join(tempfile.gettempdir(), 'ui-concept-overlay.png')
    subprocess.run([CHROME, *_flags(page), f'--screenshot={tmp}', _url(page)], check=True, capture_output=True)
    overlay = Image.open(tmp).convert('RGBA').resize((1920, 1080), Image.NEAREST)
    bg = Image.open(bg_path).convert('RGBA').resize((1920, 1080), Image.LANCZOS)
    bg.alpha_composite(overlay)
    bg.convert('RGB').save(out_path)
    print('wrote', out)
    return out_path

# ---------- lint ----------
def lint(page):
    r = subprocess.run([CHROME, *_flags(page), '--dump-dom', _url(page, '?check')], capture_output=True, text=True, encoding='utf-8')
    m = re.search(r'<script type="application/json" id="check-out">(.*?)</script>', r.stdout, re.S)
    if not m:
        raise RuntimeError('no check output for ' + page + ' (does the page load check.js?)')
    return json.loads(m.group(1))

def _rgb(c):
    c = c.strip()
    if c.startswith('#'):
        return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))
    m = re.match(r'rgba?\(([^)]+)\)', c)
    return tuple(int(float(v)) for v in m.group(1).split(',')[:3])

def _lum(rgb):
    def ch(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    r, g, b = (ch(v) for v in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b

def ratio(a, b):
    la, lb = _lum(a), _lum(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)

def contrast(png, texts):
    """Contrast of each text run against the median pixel of a 2 px (GUI) band around its ink box,
    measured on the final 1920x1080 image."""
    im = Image.open(png).convert('RGB'); px = im.load(); W, H = im.size
    rows = []
    for t in texts:
        col = _rgb(t['color']); x, y, w, h = (t['ink'][k] * 2 for k in ('x', 'y', 'w', 'h'))
        if w <= 0 or h <= 0:
            continue
        x0, y0, x1, y1 = int(x), int(y), int(x + w), int(y + h)
        band = []
        for yy in range(max(0, y0 - 4), min(H, y1 + 4)):
            for xx in range(max(0, x0 - 4), min(W, x1 + 4)):
                if x0 <= xx < x1 and y0 <= yy < y1:
                    continue
                band.append(px[xx, yy])
        if not band:
            continue
        band.sort(key=_lum)
        pick = band[len(band) // 2]  # the typical pixel next to the text (median luminance)
        rows.append({'text': t['text'], 'color': t['color'], 'bg': '#%02x%02x%02x' % pick, 'ratio': round(ratio(col, pick), 2), 'kind': t['kind'], 'size': t['size']})
    return rows

def report(page, background, out):
    png = render(page, background, out)
    res = lint(page)
    res['contrast'] = contrast(png, res['texts'])
    low = [c for c in res['contrast'] if c['ratio'] < 4.5]
    kinds = {}
    for i in res['issues']:
        kinds[i['type']] = kinds.get(i['type'], 0) + 1
    print(f"  words: {res.get('words')}  type sizes: {res['sizeSteps']}  {res['sizes']}")
    print(f"  issues: {kinds or 'none'}; contrast < 4.5: {len(low)}")
    for i in res['issues']:
        print('   -', json.dumps(i))
    for c in low:
        print(f"   - contrast {c['ratio']}: '{c['text'][:40]}' {c['color']} on {c['bg']}")
    res['lowContrast'] = low
    return res

if __name__ == '__main__':
    if sys.argv[1] in ('--all-v2', '--all-v3', '--all-v4'):
        only = sys.argv[2:]
        tag = sys.argv[1][-2:]
        path = os.path.join(HERE, tag + '-check.json')
        summary = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {}
        for page, bg in {'v2': V2, 'v3': V3, 'v4': V4}[tag]:
            if only and not any(o in page for o in only):
                continue
            print(page)
            r = report(page + '.html', bg, page + '.png')
            summary[page] = {'words': r.get('words'), 'sizes': r['sizeSteps'], 'issues': r['issues'], 'lowContrast': r['lowContrast']}
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=1)
    elif sys.argv[1] == '--check':
        report(*sys.argv[2:5])
    else:
        render(*sys.argv[1:4])
