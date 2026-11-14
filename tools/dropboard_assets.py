"""Copies the drop board's altar and coin art into the mod's own assets. Stdlib only.

The altar and the coin are modelled with the prop workflow (art/props/drop_altar, art/props/drop_coin; model.py, card), but the prop
resource pack only reaches clients of the builder server, so the camp's altar is drawn by the client from the mod's assets
(DropAltar.java). This writes, byte for byte the same on every run:
  src/main/resources/assets/gravesandglory/models/dropboard/<part>.json    each part, its texture pointed at the item atlas
  src/main/resources/assets/gravesandglory/textures/item/dropboard/<id>.png
  src/main/resources/assets/gravesandglory/dropboard/altar.json            the parts: model, glow, offset (model px, prop frame)
Run after the art changes: python tools/dropboard_assets.py   (--check: exit 1 when the assets are stale)
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ART = ROOT / 'art' / 'props'
ASSETS = ROOT / 'src' / 'main' / 'resources' / 'assets' / 'gravesandglory'
IDS = ('drop_altar', 'drop_coin')


def outputs():
    out = {}
    for pid in IDS:
        d = json.loads((ART / pid / 'def.json').read_text(encoding='utf-8'))
        out[ASSETS / 'textures' / 'item' / 'dropboard' / (pid + '.png')] = (ART / pid / (pid + '.png')).read_bytes()
        for part in d['parts']:
            m = json.loads((ART / pid / (part['model'] + '.json')).read_text(encoding='utf-8'))
            m['textures'] = {k: 'gravesandglory:item/dropboard/' + pid for k in m.get('textures', {})}
            m.pop('display', None)
            out[ASSETS / 'models' / 'dropboard' / (part['model'] + '.json')] = (json.dumps(m, separators=(',', ':')) + '\n').encode()
        if pid == 'drop_altar':
            parts = [{'model': p['model'], 'glow': bool(p.get('glow')), 'offset': p.get('offset', [0, 0, 0])} for p in d['parts']]
            out[ASSETS / 'dropboard' / 'altar.json'] = (json.dumps({'size': d['size'], 'parts': parts}, indent=1) + '\n').encode()
    return out


def main():
    check = '--check' in sys.argv
    stale = 0
    for path, data in outputs().items():
        if path.exists() and path.read_bytes() == data:
            continue
        stale += 1
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    print('dropboard assets: %d file(s) %s' % (stale, 'stale' if check else 'written'))
    return 1 if check and stale else 0


if __name__ == '__main__':
    sys.exit(main())
