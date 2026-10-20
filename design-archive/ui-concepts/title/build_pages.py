"""Write the three title mockup pages. The version line is read from the real build identity, as the game
draws it ("v" + version + " / " + the first 8 characters of the source hash).

python build_pages.py -> title-hill-day.html, title-hill-night.html, title-hill-hover.html"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..', '..'))
BUILD = json.load(open(os.path.join(ROOT, 'build', 'resources', 'main', 'gravesandglory-build.json'), encoding='utf-8'))
VERSION = 'v%s / %s' % (BUILD['version'], BUILD['sourceSha256'][:8])

# GUI px. The menu column sits on the crag face in shadow; every line is a control with 4 px of air round its ink (6 below, for descenders).
MENU = [('ride', 'Ride to camp', 't16', 396), ('friends', 'Play with friends', 'h11', 424),
        ('settings', 'Settings', 'h11', 448), ('credits', 'Credits', 'h11', 472), ('quit', 'Quit', 'h11', 496)]
MARK_Y = {'ride': 404, 'friends': 430}          # centred on each line's capitals

PAGE = """<!doctype html>
<html class="v2"><head><meta charset="utf-8"><title>Title: The Hill ({label})</title>
<link rel="stylesheet" href="title.css">
<style>
/* The Hill. Behind the page: one real battlefield frame (shader-on, {light}) as the static panorama.
   On the page, back to front: the volley, the hill (crag, plateau, tents, tree, path), the commander, the embers (pixel sprites at GUI
   scale 1, each its own parallax layer), then the lockup, the tagline and the menu, which is plain text
   on the shadowed crag face with a brass spearhead on the line under the cursor (Battle Brothers fashion). */
img.s{{position:absolute;image-rendering:pixelated}}
.tag{{position:absolute;top:88px;color:var(--ivory);text-shadow:1px 1px 0 #120d16}}
.mi{{position:absolute;left:36px;padding:4px 4px 6px;color:var(--ivory)}}
.mi.on{{color:var(--brass-hi)}}
</style></head>
<body>
<img class="s" src="sprites/volley-{tod}.png" style="left:0;top:0" data-bleed alt="">
<img class="s" src="sprites/hill-{tod}.png" style="left:0;top:240px" data-bleed alt="">
<img class="s" src="sprites/commander-{tod}-0.png" style="left:{cx}px;top:{cy}px" data-bleed alt="">
<img class="s" src="sprites/embers-{tod}.png" style="left:0;top:0" data-bleed alt="">

<img class="s" src="sprites/lockup.png" style="left:240px;top:24px" alt="Graves and Glory">
<div class="tag h11" data-cx="480">Collect an army. Command the battlefield.</div>

<img class="s" src="sprites/marker.png" style="left:20px;top:{mark_y}px" alt="">
{menu}
{horn}
<mc-text class="a" data-rx="944" style="top:504px" color="#afb9bc">{version}</mc-text>
<script src="../mc.js"></script><script src="../v2.js"></script><script src="../check.js"></script>
</body></html>
"""

HORN = """<img class="s" id="horn" src="sprites/horn.png" style="left:0;top:426px" alt="">
<script>window.v2after=[function(){var r=document.querySelector('[data-name=friends]').getBoundingClientRect();
document.getElementById('horn').style.left=Math.round(r.right+4)+'px';}];</script>"""


def page(name, tod, label, light, hovered):
    menu = '\n'.join(
        f'<div class="mi {cls}{" on" if key == hovered else ""}" data-ctl data-name="{key}" style="top:{top}px">{text}</div>'
        for key, text, cls, top in MENU)
    cx, cy = json.load(open(os.path.join(HERE, 'sprites', 'commander-at.json')))   # where title_commander placed him
    html = PAGE.format(label=label, light=light, tod=tod, mark_y=MARK_Y[hovered], menu=menu,
                       horn=HORN if hovered == 'friends' else '', version=VERSION, cx=cx, cy=cy)
    with open(os.path.join(HERE, name), 'w', encoding='utf-8', newline='\n') as f:
        f.write(html)
    print('wrote', name)


if __name__ == '__main__':
    page('title-hill-day.html', 'day', 'golden hour', 'golden hour', 'ride')
    page('title-hill-night.html', 'night', 'Dusk, night', 'night, torches lit', 'ride')
    page('title-hill-hover.html', 'day', 'golden hour, Play with friends hovered', 'golden hour', 'friends')
