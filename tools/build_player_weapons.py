"""Builds the player weapons: textures per rarity, glow and material maps, Blockbench projects and review sheets.

  python tools/build_player_weapons.py [weapon ...]     build every weapon (or the named ones)
  python tools/build_player_weapons.py --check          only build in memory and run the checks

Writes art/player_weapons/<weapon>/: <weapon>.bbmodel (Common to Epic), <weapon>_legendary.bbmodel, <weapon>.json (the
sidecar), textures/<weapon>_<rarity>.png, textures/<weapon>_legendary_e.png (glow), textures/<weapon>_m.png and
<weapon>_legendary_m.png (material maps in the key colours of tools/build_skins.py), and art/player_weapons/review/.

The checks are the weapon standard's (docs/ART-STYLE-GUIDE.md 7.1), run with the repo's own reader (tools/bbmodel.py) on
the written projects: every frame one connected piece, no coplanar faces, every face's UV exactly 2 texels a px, one
64x128 texture, at most 16 cubes a frame (18 on Legendary) not counting strings, and no glow below Legendary.
"""
import argparse
import importlib
import json
import pathlib
import sys
import tempfile

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import bbmodel as BB                                                    # noqa: E402
from build_player_armor import BG, font                                 # noqa: E402
from player_armor.common import RARITIES, RARITY_BAR                    # noqa: E402
from player_armor.engine import Canvas, FACES, SHADE, Palette, render   # noqa: E402
from player_weapons.wengine import CAP, WBox, bbmodel, drawables        # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'art' / 'player_weapons'
SWORDS = ('longsword', 'cutlass')     # Kort's ruling (round 28): a sword is at most 24.5 px
SETS = ('oathbound', 'barrow_warden', 'thornhide', 'forgewall', 'tidewrought')


def load():
    out = {}
    for s in SETS:
        try:
            mod = importlib.import_module('player_weapons.' + s)
        except ModuleNotFoundError as e:
            if e.name != 'player_weapons.' + s:
                raise
            continue
        for wp in mod.weapons():
            out[wp.id] = wp
    return out


# ---- the fist (a 4 px box at the grip, at skin density, from a player's hand) ----

FIST_PAL = Palette().add('skin', '#C79A76', 0.18)


def _fist():
    from player_armor.engine import Box
    b = Box('fist', 'skin', 'hand', (-2, -2, -2), (4, 4, 4), [lambda c: c.fill('skin', {'up': 3, 'down': 0, 'north': 2,
                                                                                         'south': 3, 'east': 1, 'west': 3}[c.face])])
    faces = {}
    for f in FACES:
        c = Canvas(4, 4, f, b, 'base')
        b.paint[0](c)
        if f in ('south', 'north'):
            for x in range(4):
                c.px(x, 1 if x % 2 else 2, d=-1)      # knuckle creases
        tex = np.zeros((4, 4, 4), np.uint8)
        for y in range(4):
            for x in range(4):
                tex[y, x] = (*[int(v) for v in FIST_PAL.tones['skin'][int(c.tone[y, x])]], 255)
        faces[f] = (tex, np.zeros((4, 4), bool), (0, 0, 4, 4), SHADE[f])
    return [(b, faces)]


FIST = _fist()

# camera yaw: 180 looks at the front (south); 225 is the style guide's 45-degree view (lit west side showing)
VIEWS = ((180, 0, 'front'), (225, 15, '45 degrees'), (270, 0, 'edge'), (0, 0, 'back'), (225, 15, 'in the fist'))


def _render_all(wp, scale):
    rows = [(r, 'rest', '%s' % r.capitalize()) for r in RARITIES]
    if 'pull_0' in wp.frames:
        rows.append(('common', None, 'Draw: rest, pull 0-2'))
    big = 80 * scale
    cells = []
    for rarity, frame, label in rows:
        row = []
        if frame is None:
            for fr in wp.frames:
                row.append(render(drawables(wp, rarity, fr), 180, 0, scale, size=(big, big), anchor=(big / 2, big / 2)))
            row.append(render(drawables(wp, 'legendary', 'pull_2'), 225, 15, scale, size=(big, big),
                              anchor=(big / 2, big / 2)))
        else:
            for yaw, pitch, name in VIEWS:
                dr = drawables(wp, rarity, frame) + (FIST if name == 'in the fist' else [])
                row.append(render(dr, yaw, pitch, scale, size=(big, big), anchor=(big / 2, big / 2)))
        cells.append((rarity, label, row))
    return cells


def _bbox(images):
    box = None
    for im in images:
        b = im.getbbox()
        if b:
            box = b if box is None else (min(box[0], b[0]), min(box[1], b[1]), max(box[2], b[2]), max(box[3], b[3]))
    return box


def sheet(wp, path, scale=6):
    cells = _render_all(wp, scale)
    box = _bbox([im for _, _, row in cells for im in row])
    pad = 14
    cw, ch = box[2] - box[0] + 2 * pad, box[3] - box[1] + 2 * pad
    label_w, thumb_w = 190, 90
    ncol = max(len(r) for _, _, r in cells)
    Wd = label_w + ncol * cw + thumb_w + 10
    head = 70
    Ht = head + len(cells) * ch
    out = Image.new('RGBA', (Wd, Ht), BG)
    d = ImageDraw.Draw(out)
    t = '%s  ·  %s' % (wp.title, wp.set_title)
    d.text((20, 16), t, font=font(22, True), fill=(226, 222, 210))
    d.text((20, 46), '%s · %s · player weapon · columns: front, 45 degrees, edge, back, in the fist, small'
           % (wp.house, wp.kind), font=font(13), fill=(150, 154, 164))
    for i, (rarity, label, row) in enumerate(cells):
        y0 = head + i * ch
        d.rectangle((8, y0 + 8, 12, y0 + ch - 8), fill=RARITY_BAR[rarity])
        d.text((22, y0 + ch // 2 - 8), label, font=font(13), fill=(214, 216, 222))
        for j, im in enumerate(row):
            out.alpha_composite(im.crop((box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad)), (label_w + j * cw, y0))
        if label.startswith('Draw'):
            continue
        small = render(drawables(wp, rarity) + FIST, 225, 15, 2, size=(160, 160), anchor=(80, 80))
        sb = small.getbbox()
        if sb:
            sm = small.crop(sb)
            out.alpha_composite(sm, (label_w + ncol * cw + 10, y0 + (ch - sm.height) // 2))
    path.parent.mkdir(parents=True, exist_ok=True)
    out.convert('RGB').save(path)
    return path


def lineup(weapons, path, scale=5):
    imgs = []
    for wp in weapons:
        for rarity in ('common', 'legendary'):
            imgs.append(render(drawables(wp, rarity), 225, 15, scale, size=(80 * scale, 80 * scale),
                               anchor=(40 * scale, 40 * scale)))
    box = _bbox(imgs)
    pad = 8
    cw, ch = box[2] - box[0] + 2 * pad, box[3] - box[1] + 2 * pad
    cols = len(weapons)
    out = Image.new('RGBA', (20 + cols * cw, 80 + 2 * ch + 30), BG)
    d = ImageDraw.Draw(out)
    d.text((20, 14), 'Graves and Glory · ten player weapons, two per armour set', font=font(22, True), fill=(226, 222, 210))
    for i, wp in enumerate(weapons):
        x0 = 10 + i * cw
        d.text((x0 + 6, 46), wp.title, font=font(13, True), fill=(214, 216, 222))
        d.text((x0 + 6, 62), wp.set_title, font=font(11), fill=(150, 154, 164))
        for r in range(2):
            im = imgs[i * 2 + r]
            out.alpha_composite(im.crop((box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad)), (x0, 80 + r * ch))
    d.text((20, out.height - 24), 'top row Common, bottom row Legendary; the 45-degree view', font=font(12),
           fill=(150, 154, 164))
    out.convert('RGB').save(path)


# ---- checks -----------------------------------------------------------------------------

def check(wp, folder):
    problems = []
    if wp.stats('base').get('ember'):
        problems.append('glow below Legendary')
    for tier in ('base', 'legendary'):
        for frame in wp.frames:
            n = len([b for b in wp.boxes(tier, frame) if 'string' not in b.name])
            if n > CAP[tier]:
                problems.append('%s/%s: %d cubes, over the %d cap' % (tier, frame, n, CAP[tier]))
        name = wp.id + ('_legendary' if tier == 'legendary' else '')
        img, _, _ = wp.texture('legendary' if tier == 'legendary' else 'common')
        path = folder / (name + '.bbmodel')
        path.write_text(json.dumps(bbmodel(wp, tier, img), indent=1) + '\n', encoding='utf-8')
        m = BB.read(path)
        problems += ['%s: %s' % (name, p) for p in BB.check_textures(m)]
        quads = BB.frame_quads(m)
        for frame, cubes in m.frames.items():
            lo, hi = BB.bounds(quads[frame])
            extent = [round(hi[i] - lo[i], 2) for i in range(3)]
            if wp.kind in SWORDS and max(extent) > 24.5 + 1e-6:
                problems.append('%s/%s: %.2f px long, over the 24.5 px sword limit' % (name, frame, max(extent)))
            wp.extent = extent
            pieces, free = BB.joined(cubes)
            if len(pieces) != 1:
                problems.append('%s/%s: %d separate pieces %s' % (name, frame, len(pieces), pieces))
            if free:
                problems.append('%s/%s: free strings %s' % (name, frame, free))
            for cp in BB.coplanar_faces(cubes):
                problems.append('%s/%s: coplanar %s' % (name, frame, cp))
            for od in BB.off_density(cubes, 2):
                problems.append('%s/%s: off density %s' % (name, frame, od))
    return problems


def build(wp, write=True):
    folder = OUT / wp.id
    if write:
        (folder / 'textures').mkdir(parents=True, exist_ok=True)
        problems = check(wp, folder)
        for rarity in RARITIES:
            img, glow, keys = wp.texture(rarity)
            img.save(folder / 'textures' / ('%s_%s.png' % (wp.id, rarity)))
            if rarity == 'legendary':
                glow.save(folder / 'textures' / ('%s_legendary_e.png' % wp.id))
                keys.save(folder / 'textures' / ('%s_legendary_m.png' % wp.id))
            if rarity == 'common':
                keys.save(folder / 'textures' / ('%s_m.png' % wp.id))
        side = {'weapon': wp.id, 'title': wp.title, 'set': wp.set_title, 'house': wp.house, 'kind': wp.kind,
                'density': 2, 'frames': list(wp.frames),
                'grip': {'offset': [0, 0, 0], 'rotation': [0, 0, 0],
                         'note': "the grip's centre is the model origin (the fist box covers y -2..+2); y up, big faces "
                                 "across z, Blockbench's south the front"}}
        side.update(wp.sidecar)
        (folder / (wp.id + '.json')).write_text(json.dumps(side, indent=1) + '\n', encoding='utf-8')
        sheet(wp, OUT / 'review' / ('%s.png' % wp.id))
    else:
        with tempfile.TemporaryDirectory() as tmp:
            problems = check(wp, pathlib.Path(tmp))
    counts = {t: max(len([b for b in wp.boxes(t, f) if 'string' not in b.name]) for f in wp.frames)
              for t in ('base', 'legendary')}
    return problems, counts, wp.stats('legendary').get('ember', 0)


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('weapons', nargs='*')
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args(argv)
    allw = load()
    names = a.weapons or list(allw)
    bad = 0
    for n in names:
        wp = allw[n]
        problems, counts, glow = build(wp, write=not a.check)
        print('%-14s cubes %-30s glow %3d  size %s px' % (n, counts, glow, getattr(wp, 'extent', '?')))
        for p in problems:
            print('   ! ' + p)
        bad += len(problems)
    if not a.check and not a.weapons:
        lineup([allw[n] for n in names], OUT / 'review' / 'lineup.png')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
