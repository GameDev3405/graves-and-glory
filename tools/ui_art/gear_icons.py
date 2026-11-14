"""Commander gear icons (Task 8; the baked real-model pictures Kort approved 2026-11-13): the Arsenal and Forge pictures of
every gear item, their shared rarity glows and frames, and the relic icons.

  gear/<item>_r<1-5>   each armour piece and weapon at each rarity: a smooth 144 x 144 render of its real model, no frame
                       or glow (art/hand/gear/icon_render.item_picture); drawn into 36 canvas px with linear filtering
  gear/glow_r<1-5>     the rarity's backdrop glow, 144 x 144 (icon_render.glow), drawn into 40 canvas px (96 on the card)
  gear/frame_r<1-5>    the rarity's slot frame, 44 x 44 canvas px drawn 1:1 (icon_render.frame)
  gear/frame48_r<1-5>  the Forge card's frame, 48 x 48, drawn at 2x into the 96 px picture box
  relic/<id>           each relic on its dark round plate, 32 x 32 (art/hand/gear/icons.relics), unchanged
The art is painted by art/hand/gear/icon_render.py and icons.py; this module only lists it. Each item picture is rendered
on its own when its entry is painted (build_ui_art paints entries in several processes). Registered through manifest.add
when build_ui_art imports this module (one import line there)."""
import os
import sys
from types import SimpleNamespace
import manifest

GEAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'art', 'hand', 'gear')
sys.path.insert(0, GEAR)
import gearlib
import icons
import icon_render

_relics = {}


def spr(paint):   # wrapped as a Spr is: build_ui_art reads .im, which on a bare PIL image is its C core
    return lambda: SimpleNamespace(im=paint())


def relic(rid):
    def paint():
        if not _relics: _relics.update(icons.relics())
        return _relics[rid]
    return spr(paint)


for _i in gearlib.ids()['items']:
    for _r in gearlib.RARITIES:
        manifest.add('gear/%s_r%d' % (_i['id'], _r), spr(lambda i=_i['id'], r=_r: icon_render.item_picture(i, r)))
for _r in gearlib.RARITIES:
    manifest.add('gear/glow_r%d' % _r, spr(lambda r=_r: icon_render.glow(r)))
    manifest.add('gear/frame_r%d' % _r, spr(lambda r=_r: icon_render.frame(r, icon_render.FRAME)))
    manifest.add('gear/frame48_r%d' % _r, spr(lambda r=_r: icon_render.frame(r, icon_render.FRAME_CARD)))
for _k in icons.RELICS:
    manifest.add('relic/' + _k, relic(_k))
