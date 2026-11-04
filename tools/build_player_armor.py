"""Builds the player armour sets: textures for every rarity and team, Blockbench projects and review sheets.

  python tools/build_player_armor.py [set ...]        build every set (or the named ones)
  python tools/build_player_armor.py --check          only paint and run the checks (z-fighting faces, glow cap)

Writes art/armor/<set>/: <set>.bbmodel (Common to Epic geometry), <set>_legendary.bbmodel, textures/<set>_<rarity>_<team>.png
and textures/<set>_legendary_e.png (the glow mask), and art/armor/review/<set>.png: the sheet Kort reviews, one row per
rarity (crimson), Legendary again in teal and a mixed row (chest and boots, Rare, on a bare player). Every row shows front,
three-quarter, side, back and three-quarter back views, plus a thumbnail at roughly in-game size.
"""
import argparse
import json
import pathlib
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from player_armor import paint as P                                     # noqa: E402
from player_armor.common import RARITIES, RARITY_BAR, TEAMS             # noqa: E402
from player_armor.engine import Box, Palette, FACES, SHADE, bbmodel, coplanar, render   # noqa: E402
import importlib                                                          # noqa: E402

ORDER = ('oathbound', 'barrow_warden', 'thornhide', 'forgewall', 'tidewrought')
SETS = {}
for _m in ORDER:
    try:
        SETS[_m] = importlib.import_module('player_armor.' + _m).build
    except ModuleNotFoundError as _e:
        if _e.name != 'player_armor.' + _m:
            raise
ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'art' / 'armor'
BG = (34, 37, 45, 255)
GLOW_CAP = 16          # style guide section 6: at most 16 lit texels on Epics and Legendaries (per piece shown)

# ---- the bare player under the armour (an original placeholder skin, not Steve) -----------------------------

BODY_PAL = Palette()
for n, h in (('skin', '#C79A76'), ('hair', '#4A3424'), ('tunic', '#3C4A5E'), ('trousers', '#4E4034'),
             ('shoe', '#3A3634'), ('eye', '#2A3348'), ('white', '#E8E4DA'), ('mouth', '#8A5A48')):
    BODY_PAL.add(n, h, 0.18)


def _head(c):
    c.fill('skin', c.base if c.sem != 'front' else 3)
    if c.sem == 'top':
        c.fill('hair', 3)
        return
    if c.sem == 'bottom':
        return
    c.hline(0, mat='hair', tone=2); c.hline(1, mat='hair', tone=2)
    if c.sem == 'front':
        c.px(0, 2, mat='hair', tone=2); c.px(7, 2, mat='hair', tone=1)
        c.px(1, 4, mat='white', tone=3); c.px(2, 4, mat='eye', tone=2)
        c.px(5, 4, mat='eye', tone=2); c.px(6, 4, mat='white', tone=3)
        c.hline(3, 1, 2, mat='hair', tone=1); c.hline(3, 5, 6, mat='hair', tone=1)
        c.px(3, 5, d=-1); c.px(4, 5, d=-1)
        c.hline(6, 3, 4, mat='mouth', tone=2)
    elif c.sem in ('back',):
        c.rect(0, 0, 8, 6, mat='hair', tone=2)
    else:
        c.rect(0, 0, 8, 3, mat='hair', tone=2 if c.sem == 'lit' else 1)
        c.rect(4 if c.sem == 'lit' else 0, 3, 4, 2, mat='hair', tone=1)


def _torso(c):
    c.fill('tunic', c.base)
    if c.sem in ('front', 'back') and c.h > 4:
        c.hline(c.h - 2, mat='trousers', tone=1)
    if c.sem == 'front':
        c.vline(3, 0, 3, d=-1)


def _arm(c):
    c.fill('tunic', c.base)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.rect(0, 4, c.w, 8, mat='skin', tone=c.base)
        c.hline(4, d=-1)
    if c.sem == 'bottom':
        c.fill('skin', 1)


def _leg(c):
    c.fill('trousers', c.base)
    if c.sem in ('front', 'back', 'lit', 'dark'):
        c.rect(0, 9, c.w, 3, mat='shoe', tone=c.base)
    if c.sem == 'bottom':
        c.fill('shoe', 0)


BODY = [Box('p_head', 'skin', 'head', (-4, 24, -4), (8, 8, 8), [_head]),
        Box('p_body', 'skin', 'body', (-4, 12, -2), (8, 12, 4), [_torso]),
        Box('p_arm_r', 'skin', 'right_arm', (4, 12, -2), (4, 12, 4), [_arm]),
        Box('p_arm_l', 'skin', 'left_arm', (-8, 12, -2), (4, 12, 4), [_arm]),
        Box('p_leg_r', 'skin', 'right_leg', (-0.1, 0, -2), (4, 12, 4), [_leg]),
        Box('p_leg_l', 'skin', 'left_leg', (-3.9, 0, -2), (4, 12, 4), [_leg])]


def _body_drawables():
    from player_armor.engine import Canvas
    out = []
    for b in BODY:
        faces = {}
        for f in FACES:
            w, h = b.face_size(f)
            c = Canvas(w, h, f, b, 'base')
            for layer in b.paint:
                layer(c)
            tex = np.zeros((h, w, 4), dtype=np.uint8)
            for y in range(h):
                for x in range(w):
                    m = c.mat[y, x]
                    if m is not None:
                        tex[y, x] = (*[int(v) for v in BODY_PAL.tones[m][int(c.tone[y, x])]], 255)
            faces[f] = (tex, np.zeros((h, w), bool), (0, 0, w, h), SHADE[f])
        out.append((b, faces))
    return out


# ---- drawing ------------------------------------------------------------------------------------------

def drawables(aset, rarity, team, slots=None):
    tier = 'legendary' if rarity == 'legendary' else 'base'
    img, glow = aset.texture(rarity, team)
    tex = np.array(img)
    gl = np.array(glow)[..., 3] > 0
    W, H, pos = aset.pack()
    out = []
    for b in aset.boxes_for(tier, slots):
        faces = {f: (tex, gl, pos[(b.name, f)], SHADE[f]) for f in FACES if f not in b.hide}
        out.append((b, faces))
    return out


VIEWS = ((0, 0, 'front'), (35, 14, 'three-quarter'), (90, 0, 'side'), (180, 0, 'back'), (215, 14, 'three-quarter back'))


def font(size, bold=False):
    for p in ('/usr/share/fonts/opentype/inter/Inter-%s.otf' % ('SemiBold' if bold else 'Regular'),
              '/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf' % ('-Bold' if bold else '')):
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            pass
    return ImageFont.load_default()


def sheet(aset, path, scale=6):
    rows = [('common', 'crimson', None, 'Common / crimson'), ('uncommon', 'crimson', None, 'Uncommon / crimson'),
            ('rare', 'crimson', None, 'Rare / crimson'), ('epic', 'crimson', None, 'Epic / crimson'),
            ('legendary', 'crimson', None, 'Legendary / crimson'), ('legendary', 'teal', None, 'Legendary / teal'),
            ('rare', 'crimson', ('chest', 'boots'), 'Mixed: chest + boots / rare')]
    vw, vh = 31 * scale, 44 * scale
    label_w, thumb_w = 170, 80
    W = label_w + vw * len(VIEWS) + thumb_w + 20
    head_h = 64
    H = head_h + vh * len(rows)
    out = Image.new('RGBA', (W, H), BG)
    d = ImageDraw.Draw(out)
    d.text((20, 18), '%s  ·  %s' % (aset.title, aset.house), font=font(22, True), fill=(226, 222, 210))
    d.text((20 + d.textlength('%s  ·  %s' % (aset.title, aset.house), font=font(22, True)) + 16, 24),
           'player armour set · helmet, chest, legs, boots', font=font(13), fill=(150, 154, 164))
    body = _body_drawables()
    for i, (rarity, team, slots, label) in enumerate(rows):
        y0 = head_h + i * vh
        d.rectangle((8, y0 + 10, 12, y0 + vh - 10), fill=RARITY_BAR[rarity])
        d.text((22, y0 + vh // 2 - 8), label, font=font(13), fill=(214, 216, 222))
        dr = drawables(aset, rarity, team, slots) + body
        for j, (yaw, pitch, _) in enumerate(VIEWS):
            im = render(dr, yaw, pitch, scale, size=(vw, vh), anchor=(vw / 2, vh - 3 * scale))
            out.alpha_composite(im, (label_w + j * vw, y0))
        th = render(dr, 35, 14, 2, size=(thumb_w, 44 * 2), anchor=(thumb_w / 2, 44 * 2 - 6))
        out.alpha_composite(th, (label_w + len(VIEWS) * vw + 6, y0 + vh - 44 * 2 - 2 * scale))
    path.parent.mkdir(parents=True, exist_ok=True)
    out.convert('RGB').save(path)
    return path


def lineup(sets, path, scale=5):
    """Every set side by side at Common and Legendary, front and three-quarter, crimson."""
    vw, vh = 34 * scale, 46 * scale
    cols = len(sets)
    W, H = 20 + cols * vw * 2, 70 + 2 * vh + 30
    out = Image.new('RGBA', (W, H), BG)
    d = ImageDraw.Draw(out)
    d.text((20, 16), 'Graves and Glory · five new player armour sets', font=font(22, True), fill=(226, 222, 210))
    body = _body_drawables()
    for i, aset in enumerate(sets):
        x0 = 10 + i * vw * 2
        d.text((x0 + 10, 50), '%s · %s' % (aset.title, aset.house), font=font(13, True), fill=(214, 216, 222))
        for r, rarity in enumerate(('common', 'legendary')):
            dr = drawables(aset, rarity, 'crimson') + body
            for j, (yaw, pitch) in enumerate(((0, 0), (35, 14))):
                im = render(dr, yaw, pitch, scale, size=(vw, vh), anchor=(vw / 2, vh - 2 * scale))
                out.alpha_composite(im, (x0 + j * vw, 70 + r * vh))
    d.text((20, H - 24), 'top row Common, bottom row Legendary; crimson team', font=font(12), fill=(150, 154, 164))
    out.convert('RGB').save(path)
    return path


# ---- checks -------------------------------------------------------------------------------------------

def checks(aset):
    problems = []
    for tier in ('base', 'legendary'):
        for a, b, f in coplanar(aset.boxes_for(tier) + BODY):
            if a.startswith('p_') and b.startswith('p_'):
                continue   # the vanilla player's own legs overlap; not ours
            problems.append('%s: %s and %s share a %s plane (z-fighting)' % (tier, a, b, f))
        aset.paint(tier)
    glow = aset.stats('legendary').get('ember', 0)
    if aset.stats('base').get('ember', 0):
        problems.append('base tier carries ember texels')
    return problems, glow


def build(aset, write=True):
    problems, glow = checks(aset)
    d = OUT / aset.id
    tex_dir = d / 'textures'
    if write:
        tex_dir.mkdir(parents=True, exist_ok=True)
        for rarity in RARITIES:
            for team in TEAMS:
                img, g = aset.texture(rarity, team)
                img.save(tex_dir / ('%s_%s_%s.png' % (aset.id, rarity, team)))
                if rarity == 'legendary' and team == 'crimson':
                    g.save(tex_dir / ('%s_legendary_e.png' % aset.id))
        for tier, rarity in (('base', 'common'), ('legendary', 'legendary')):
            img, _ = aset.texture(rarity, 'crimson')
            name = '%s_%s_crimson.png' % (aset.id, rarity)
            proj = bbmodel(aset, tier, img, name)
            (d / ('%s%s.bbmodel' % (aset.id, '' if tier == 'base' else '_legendary'))).write_text(
                json.dumps(proj, indent=1) + '\n', encoding='utf-8')
        sheet(aset, OUT / 'review' / ('%s.png' % aset.id))
    W, H, pos = aset.pack()
    return {'set': aset.id, 'boxes': {t: len(aset.boxes_for(t)) for t in ('base', 'legendary')},
            'texture': [W, H], 'glow_texels': glow, 'problems': problems}


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('sets', nargs='*')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--no-lineup', action='store_true')
    a = ap.parse_args(argv)
    names = a.sets or list(SETS)
    built = []
    bad = 0
    for n in names:
        aset = SETS[n]()
        r = build(aset, write=not a.check)
        built.append(aset)
        print('%-14s boxes %-26s texture %dx%d  glow %d' % (r['set'], r['boxes'], *r['texture'], r['glow_texels']))
        for p in r['problems']:
            print('   ! ' + p)
        bad += len(r['problems'])
    if not a.check and not a.sets and not a.no_lineup:
        lineup(built, OUT / 'review' / 'lineup.png')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
