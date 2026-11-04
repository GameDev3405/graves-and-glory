"""Commander gear Task 8 (docs/superpowers/plans/2026-11-03-gear-art.md): the grid icons of every gear item and relic.

  gear/<item>_r<1-5>   each armour piece and weapon at each rarity, 32 x 32 (art/hand/gear/icons.item_icons)
  relic/<id>           each relic on its dark round plate, 32 x 32 (art/hand/gear/icons.relics)
The art is painted by art/hand/gear/icons.py; this module only lists it. The item renders are slow, so they are made once
on the first paint and kept. Registered through manifest.add when build_ui_art imports this module (one import line there)."""
import os
import sys
from types import SimpleNamespace
import manifest

GEAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'art', 'hand', 'gear')
sys.path.insert(0, GEAR)
import gearlib
import icons

_made = {}


def made(kind):
    if kind not in _made:
        _made[kind] = icons.item_icons() if kind == 'items' else icons.relics()
    return _made[kind]


def item(iid, r):   # wrapped as a Spr is: build_ui_art reads .im, which on a bare PIL image is its C core
    return lambda: SimpleNamespace(im=made('items')[(iid, r)])


def relic(rid):
    return lambda: SimpleNamespace(im=made('relics')[rid])


for _i in gearlib.ids()['items']:
    for _r in gearlib.RARITIES:
        manifest.add('gear/%s_r%d' % (_i['id'], _r), item(_i['id'], _r))
for _k in icons.RELICS:
    manifest.add('relic/' + _k, relic(_k))
