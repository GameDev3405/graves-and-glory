"""Shared setup for the storybook painters: paths into the v4 kit and the extra ramps the book needs.
Everything is painted at GUI scale 1 (one pixel here is one GUI pixel; the page is doubled nearest-neighbour)."""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
KIT = os.path.dirname(HERE)
for p in (KIT, os.path.join(KIT, 'title')):
    if p not in sys.path:
        sys.path.insert(0, p)
ART = os.path.join(HERE, 'art')
os.makedirs(ART, exist_ok=True)

from v4lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask   # noqa: E402
from v4pal import *                                                    # noqa: E402,F401

# The binding: a near-black umber morocco, cooler and less red than the oak it lies on, so the book separates
# from the table by value and by grain (pebbled, not linear) and the brass and gilt carry the light.
LEA = ['#0b0809', '#120d0e', '#1a1413', '#221a17', '#2b211c', '#352922', '#413228', '#4f3d30', '#614b3a', '#755c47']
SH = '#0a0505'
SPEC = '#fff0c4'
TARN = ['#3a3220', '#4d4529', '#5d5a3a']


def save(spr, name):
    spr.save(os.path.join(ART, name + '.png'))
    print('  art/' + name + '.png', spr.w, 'x', spr.h)
