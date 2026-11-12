"""Writes the in-game script that places the Kingsfield sections one at a time, 135 above their real height
(ground y 199), with a picture of each section before the next goes in, then eye-level pictures of the key places.
Usage: python secshots.py OUT.txt [SECTION ...]"""
import sys, math
from kfmap import section_box, YLO
UP = 135
names = sys.argv[2:] or [c + str(r) for r in range(1, 7) for c in 'ABCDEFGHI']


def mid(n):
    b = section_box(n)
    return (b[0] + b[2]) / 2, (b[1] + b[3]) / 2


names.sort(key=lambda n: (round(math.hypot(*mid(n))), n))
out = ['# Kingsfield: sections placed one at a time, a picture of each before the next.', 'note Kingsfield sections', 'mc time set 3000']
for n in names:
    b = section_box(n)
    cx, cz = mid(n)
    out.append('mc execute as @a at @s run forceload add %d %d %d %d' % (b[0], b[1], b[2], b[3]))
    out.append('mc execute as @a at @s run place template ggmaps:kf/sec/kf_%s %d %d %d' % (n.lower(), b[0], YLO + UP, b[1]))
    out.append('wait 10')
    out.append('shot sec-%s %d %d %d %d %d %d' % (n.lower(), cx, 199 + 44, b[3] + 20, cx, 199, cz + 4))
if len(sys.argv) <= 2:
    G = 199
    views = [('view-dawn-gate', -170, G + 2.7, 0, -215, G + 4, 0), ('view-kings-road-east', -60, G + 2.7, 0, 0, G - 4, 0),
             ('view-pit-from-west-rim', -68, G + 2.7, 6, 0, G - 8, 0), ('view-pit-floor', -30, G - 5.3, 20, 0, G - 6, -6),
             ('view-point-a', -128, G + 14, 34, -128, G, 0), ('view-point-b', 0, G + 16, -150, 0, G + 3, -110),
             ('view-barn-road', 0, G + 2.7, -56, 0, G + 6, -90), ('view-mill', 0, G + 2.7, 60, 4, G + 12, 84),
             ('view-hold-nw', -70, G + 2.7, -70, -71, G + 3, -93), ('view-north-road', -180, G + 2.7, -50, -152, G + 4, -95),
             ('view-crane', 30, G + 2.7, -70, 14, G + 8, -56), ('view-dusk-gate', 170, G + 2.7, 0, 215, G + 4, 0),
             ('view-high-nw', -20, G + 95, 40, -120, G, -80), ('view-high-ne', 20, G + 95, 40, 120, G, -80),
             ('view-high-sw', -20, G + 95, -40, -120, G, 80), ('view-high-se', 20, G + 95, -40, 120, G, 80),
             ('view-high-pit', 0, G + 80, 95, 0, G - 8, 0), ('view-north-knoll-steps', -34, G + 2.7, -116, 0, G + 4, -114)]
    for v in views: out.append('shot %s %s %s %s %s %s %s' % v)
open(sys.argv[1], 'w').write('\n'.join(out) + '\n')
print(len(names), 'sections,', len(out), 'lines')
