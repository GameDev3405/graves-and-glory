"""Author, review and check troop animations offline, posed exactly as the game poses them.

Review a committed clip (or, with --recipe, a recipe baked in memory):
  python tools/animate_troop.py --still knight [--clip guard] [--t 0] --out DIR
  python tools/animate_troop.py --sheet knight sword_shield_overhead [0 0.2 0.5 ...] --out DIR [--recipe]
  python tools/animate_troop.py --strip knight sword_shield_overhead 0.3 0.7 [--step 0.05] --out DIR [--recipe]
  python tools/animate_troop.py --check knight [sword_shield_overhead ...] [--recipe]
  python tools/animate_troop.py --house gilded --out DIR [--clips attacks|all]
Author (bake a recipe from tools/troop_anim/recipes.py, check it, and with --write put it in the bbmodel and run
tools/clips.py and tools/clips.py --verify; a recipe that fails a gate is never written):
  python tools/animate_troop.py --author knight sword_shield_overhead [--out DIR] [--write]
Options: --tier 1..3, --cloth crimson|teal|gold|violet|linen|company, --stance KEY (default: the clip's own).
Exit status: 0 when every check passes, 1 when one fails, 2 on a usage or pipeline error.
"""
import argparse
import pathlib
import sys
import time

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from troop_anim import author as A          # noqa: E402
from troop_anim import checks as K          # noqa: E402
from troop_anim import clearance as C       # noqa: E402
from troop_anim import model as M           # noqa: E402
from troop_anim import pose as P            # noqa: E402
from troop_anim import recipes as R         # noqa: E402
from troop_anim import render as RD         # noqa: E402

CELL = (200, 270)
SCALE = 5.0


def _font(size=12):
    from PIL import ImageFont
    for name in ('consola.ttf', 'DejaVuSansMono.ttf', 'arial.ttf'):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def clip_for(troop, name, use_recipe):
    """(parsed clip, baker or None): the committed clip, or a recipe baked in memory."""
    if not use_recipe:
        lib = P.library(troop.rig, troop.family)
        if name not in lib:
            raise SystemExit(f'{troop.id}: no clip {name!r} in {troop.rig}/{troop.family} or common')
        return lib[name], None
    rec = R.recipe(troop, name)
    if rec is None:
        raise SystemExit(f'{troop.id}: no recipe or family preset for {name!r}')
    b, _, _, _, p = A.author(troop, rec)
    return p, b


def default_times(clip):
    L = clip['length']
    imp = clip.get('impact', -1)
    ts = set(np.round(np.linspace(0, L, 7), 2))
    if imp is not None and imp >= 0:
        T = imp / 20.0
        ts |= {round(T, 2), round(max(T - 0.15, 0), 2), round(max(T - 0.3, 0), 2), round(min(T + 0.1, L), 2)}
    return sorted(float(t) for t in ts)


def frame_cells(troop, player, t, views, clipper, size=CELL, scale=SCALE, center=(0.0, 8.0)):
    """The views of one frame, and {item: (px^3 inside the body, deepest px, 'part in bone')}."""
    W = player.world(t)
    vol = clipper.volumes(W, detail=True) if clipper else {}
    return [RD.to_pil(RD.render(troop, W, v, size=size, scale=scale, center=center)) for v in views], vol


def sheet(troop, clip, times, out, tag='', views=None):
    """Columns are times, rows the three views; each column is labelled with its time and the clipping volumes."""
    from PIL import Image, ImageDraw
    player = P.Player(troop, clip)
    views = views or RD.views_for(troop)
    clipper = C.Clipper(troop)
    w, h = CELL
    head, foot = 34, 30
    img = Image.new('RGB', (w * len(times), head + h * len(views) + foot), (250, 250, 247))
    d = ImageDraw.Draw(img)
    f, fs = _font(14), _font(11)
    imp = clip.get('impact', -1)
    d.text((6, 4), f"{troop.id} t{troop.tier} {clip['clip']} {clip['length']} s{' impact ' + str(imp / 20) + ' s' if imp >= 0 else ''}"
           f"  stance {player.stance_key}  {tag}", fill=(20, 20, 20), font=f)
    for c, t in enumerate(times):
        cells, vol = frame_cells(troop, player, t, views, clipper)
        for r, cell in enumerate(cells):
            img.paste(cell, (c * w, head + r * h))
        hot = imp >= 0 and abs(t - imp / 20.0) < 1e-6
        label = f't={t:.2f}' + (' IMPACT' if hot else '')
        d.text((c * w + 6, head + len(views) * h + 2), label, fill=(170, 20, 20) if hot else (20, 20, 20), font=fs)
        vtxt = '  '.join(f'{k} {v[0]:.1f}' for k, v in vol.items())
        bad = [f'{v[2]}' for v in vol.values() if v[0] > K.GATES['clip']]
        d.text((c * w + 6, head + len(views) * h + 15), (vtxt or 'no item') + (' ' + bad[0] if bad else ''),
               fill=(190, 20, 20) if bad else (90, 90, 90), font=fs)
    for r, v in enumerate(views):
        d.text((4, head + r * h + 4), v.name, fill=(60, 60, 60), font=fs)
    out = pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    path = out / f"{troop.id}_{clip['clip']}_sheet{tag and '_' + tag}.png"
    img.save(path)
    return path


def strip(troop, clip, t0, t1, step, out, tag=''):
    """One row per view, a frame every `step` s from t0 to t1."""
    from PIL import Image, ImageDraw
    player = P.Player(troop, clip)
    views = RD.views_for(troop)[:2]
    times = [round(t, 3) for t in np.arange(t0, t1 + 1e-9, step)]
    w, h = 150, 210
    img = Image.new('RGB', (w * len(times), 22 + h * len(views) + 16), (250, 250, 247))
    d = ImageDraw.Draw(img)
    fs = _font(11)
    imp = clip.get('impact', -1)
    d.text((6, 4), f"{troop.id} {clip['clip']} {t0:.2f}..{t1:.2f} s every {step} s  {tag}", fill=(20, 20, 20), font=fs)
    for c, t in enumerate(times):
        W = player.world(t)
        for r, v in enumerate(views):
            img.paste(RD.to_pil(RD.render(troop, W, v, size=(w, h), scale=3.8, center=(0.0, 8.0))), (c * w, 22 + r * h))
        hot = imp >= 0 and abs(t - imp / 20.0) < 1e-6
        d.text((c * w + 4, 22 + len(views) * h + 2), f'{t:.2f}' + (' IMPACT' if hot else ''),
               fill=(170, 20, 20) if hot else (20, 20, 20), font=fs)
    out = pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    path = out / f"{troop.id}_{clip['clip']}_strip_{t0:.2f}-{t1:.2f}{tag and '_' + tag}.png"
    img.save(path)
    return path


def still(troop, clip_name, t, out):
    from PIL import Image
    lib = P.library(troop.rig, troop.family)
    if clip_name and clip_name not in lib:
        raise SystemExit(f'{troop.id}: no clip {clip_name!r}')
    if clip_name:
        W = P.Player(troop, lib[clip_name]).world(t)
    else:
        W = troop.fk(troop.rest_local())
    cells = [RD.to_pil(RD.render(troop, W, v, size=CELL, scale=SCALE, center=(0.0, 8.0))) for v in RD.views_for(troop)]
    img = Image.new('RGB', (CELL[0] * len(cells), CELL[1]), 'white')
    for i, c in enumerate(cells):
        img.paste(c, (i * CELL[0], 0))
    out = pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    path = out / f"{troop.id}_{clip_name or 'rest'}_{t:.2f}.png"
    img.save(path)
    return path


def house(hid, out, which='attacks', tier=3, cloth='crimson'):
    """Every troop of a House (rows) across its clips (columns), each at its impact (else mid-clip), 3/4 view."""
    from PIL import Image, ImageDraw
    ids = M.troops(hid)
    if not ids:
        raise SystemExit(f'no troops in House {hid!r}')
    rows = []
    for tid in ids:
        tr = M.Troop(tid, tier, cloth)
        lib = P.library(tr.rig, tr.family)
        names = list(P.attacks(tr.family)) if which == 'attacks' else sorted(lib)
        names = [n for n in names if n in lib] or [n for n in ('guard', 'idle') if n in lib]
        cells = []
        for n in names:
            clip = lib[n]
            imp = clip.get('impact', -1)
            t = imp / 20.0 if imp is not None and imp >= 0 else clip['length'] / 2
            W = P.Player(tr, clip).world(t)
            cells.append((n, RD.to_pil(RD.render(tr, W, RD.views_for(tr)[0], size=(170, 230), scale=4.2, center=(0, 8)))))
        rows.append((tid, tr.family, cells))
    ncol = max(len(c) for _, _, c in rows)
    w, h = 170, 230
    img = Image.new('RGB', (110 + w * ncol, (h + 16) * len(rows)), (250, 250, 247))
    d = ImageDraw.Draw(img)
    fs = _font(11)
    for r, (tid, fam, cells) in enumerate(rows):
        y = r * (h + 16)
        d.text((4, y + 4), tid, fill=(20, 20, 20), font=fs)
        d.text((4, y + 18), fam, fill=(90, 90, 90), font=fs)
        for c, (n, cell) in enumerate(cells):
            img.paste(cell, (110 + c * w, y))
            d.text((110 + c * w + 3, y + h + 1), n[:26], fill=(40, 40, 40), font=fs)
    out = pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    path = out / f'house_{hid}_{which}.png'
    img.save(path)
    return path


def check(troop, names, use_recipe):
    ok = True
    for n in names:
        clip, b = clip_for(troop, n, use_recipe)
        res = K.measure(troop, clip, reach=b.reach if b else None)
        print(K.report(res))
        ok &= not res['fails']
    return ok


def do_author(troop, name, out, write):
    rec = R.recipe(troop, name)
    if rec is None:
        raise SystemExit(f'{troop.id}: no recipe or family preset for {name!r}')
    t0 = time.time()
    b, ch, times, text, clip = A.author(troop, rec)
    print(f'baked {name} for {troop.id} in {time.time() - t0:.1f} s; clearance pushes '
          f'{ {k: round(float(v.max()), 2) for k, v in b.pushes.items()} }' + (f'; no group for {b.skipped}' if b.skipped else ''))
    res = K.measure(troop, clip, reach=b.reach)
    print(K.report(res))
    if out:
        ks = sorted({0.0} | {float(k['t']) for k in rec['keys']} | set(default_times(clip)))
        print('sheet', sheet(troop, clip, ks, out, 'recipe'))
        imp = clip.get('impact', -1)
        if imp >= 0:
            T = imp / 20.0
            print('strip', strip(troop, clip, max(0.0, round(T - 0.35, 2)), min(clip['length'], round(T + 0.2, 2)), 0.05, out, 'recipe'))
    if not write:
        return not res['fails']
    if res['fails']:
        print('not written: the recipe fails a gate')
        return False
    A.write(troop, name, ch, times, impact=rec.get('impact'), length=rec['length'])
    print('wrote', P.source(troop.rig, troop.family).relative_to(M.ROOT).as_posix())
    rc, msg = A.run_clips()
    print(msg)
    if rc:
        return False
    rc, msg = A.run_clips(verify=True)
    print(msg)
    return rc == 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument('--still', metavar='TROOP')
    g.add_argument('--sheet', nargs='+', metavar=('TROOP', 'CLIP'))
    g.add_argument('--strip', nargs=4, metavar=('TROOP', 'CLIP', 'FROM', 'TO'))
    g.add_argument('--check', nargs='+', metavar='TROOP')
    g.add_argument('--house', metavar='HOUSE')
    g.add_argument('--author', nargs=2, metavar=('TROOP', 'CLIP'))
    g.add_argument('--list', action='store_true', help='troops, their family and clips')
    ap.add_argument('--out', default=None)
    ap.add_argument('--clip', default=None)
    ap.add_argument('--clips', default='attacks', choices=('attacks', 'all'))
    ap.add_argument('--t', type=float, default=0.0)
    ap.add_argument('--step', type=float, default=0.05)
    ap.add_argument('--tier', type=int, default=3)
    ap.add_argument('--cloth', default='crimson')
    ap.add_argument('--recipe', action='store_true', help='review the recipe baked in memory, not the committed clip')
    ap.add_argument('--write', action='store_true', help='with --author: write the bbmodel and run tools/clips.py')
    a = ap.parse_args(argv)

    def troop(tid):
        return M.Troop(tid, a.tier, a.cloth)
    if a.list:
        for tid in M.troops():
            tr = troop(tid)
            print(f'{tid:16} {tr.rig:8} {tr.family:13} {tr.hand:5} attacks: {", ".join(P.attacks(tr.family))}')
        return 0
    if a.house:
        print(house(a.house, a.out or '.', a.clips, a.tier, a.cloth))
        return 0
    if a.still:
        print(still(troop(a.still), a.clip, a.t, a.out or '.'))
        return 0
    if a.sheet:
        if len(a.sheet) < 2:
            ap.error('--sheet TROOP CLIP [t ...]')
        tr = troop(a.sheet[0])
        clip, _ = clip_for(tr, a.sheet[1], a.recipe)
        ts = [float(x) for x in a.sheet[2:]] or default_times(clip)
        print(sheet(tr, clip, ts, a.out or '.', 'recipe' if a.recipe else ''))
        return 0
    if a.strip:
        tr = troop(a.strip[0])
        clip, _ = clip_for(tr, a.strip[1], a.recipe)
        print(strip(tr, clip, float(a.strip[2]), float(a.strip[3]), a.step, a.out or '.', 'recipe' if a.recipe else ''))
        return 0
    if a.check:
        tr = troop(a.check[0])
        names = a.check[1:] or list(P.attacks(tr.family))
        return 0 if check(tr, names, a.recipe) else 1
    if a.author:
        return 0 if do_author(troop(a.author[0]), a.author[1], a.out, a.write) else 1
    return 2


if __name__ == '__main__':
    sys.exit(main())
