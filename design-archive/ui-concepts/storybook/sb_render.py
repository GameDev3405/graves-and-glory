"""Render the storybook mockups with the house pipeline (../render.py): headless Chromium at 960x540,
nearest-neighbour x2 to 1920x1080 over each page's background (BG: the in-game still for 03 to 05), then the
check.js lint and a contrast ratio per text run measured on the PNG.
render.py turns on greyscale text antialiasing only for v2-/v3-/v4- file names; the storybook pages need it too
(LCD antialiasing leaves colour fringes once the page is doubled), so this wrapper adds the same two flags.

python sb_render.py            all five pages, results in sb-check.json
python sb_render.py 01 03      only the pages whose names hold these strings"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import render as R

_orig = R._flags


def _flags(page):
    f = _orig(page)
    if '--disable-lcd-text' not in f:
        f += ['--disable-lcd-text', '--font-render-hinting=none']
    return f


R._flags = _flags

PAGES = ['storybook-01-closed', 'storybook-02-spread', 'storybook-03-panel-scene', 'storybook-04-stage-scene',
         'storybook-05-march-card']

# What lies under each page. 01 and 02 cover the screen with their own table, so the house background never
# shows. 03, 04 and 05 are drawn over in-game footage: the page is transparent where the world shows, and the
# still (1920x1080, shot through the game camera by the staging agent, stills/STAGING.md) is the composite's
# background at its own resolution, under the kit's chrome doubled to scale 2.
BG = {
    'storybook-01-closed': 'bg/camp-v2.png',
    'storybook-02-spread': 'bg/camp-v2.png',
    'storybook-03-panel-scene': 'storybook/stills/fall-03-mound-night.png',
    'storybook-04-stage-scene': 'storybook/stills/north-crossing-dusk.png',
    'storybook-05-march-card': 'storybook/stills/training-field-day.png',
}

if __name__ == '__main__':
    only = sys.argv[1:]
    path = os.path.join(HERE, 'sb-check.json')
    summary = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {}
    for p in PAGES:
        if only and not any(o in p for o in only):
            continue
        if not os.path.exists(os.path.join(HERE, p + '.html')):
            continue
        print(p)
        bg = os.environ.get('SB_BG_' + p.split('-')[1], BG[p])
        r = R.report('storybook/' + p + '.html', bg, 'storybook/' + p + '.png')
        summary[p] = {'words': r.get('words'), 'sizes': r['sizeSteps'], 'issues': r['issues'],
                      'lowContrast': r['lowContrast']}
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(summary, f, indent=1)
