"""Cutaway pictures of every building's inside, one image per building: for each storey an isometric view and a plan,
both cut just under the ceiling so the furniture shows. Usage: python interiors.py OUTDIR [NAME ...]"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', '..', 'tools'))
from PIL import Image
from mapforge import render
from mapforge.world import World, Cell
from mapforge.structure import normalize
from kit import Build
import houses, hall, mill, shrine, toll, watergate

G = 64


def pieces():
    out = {}
    for n in houses.KINDS: out[n] = (houses.kind(n, 3).c, None, [G + 3 + 4 * k for k in range(houses.KINDS[n]['floors'])])
    b = Build(seed=57); hall.build(b); out['hall'] = (b.c, None, [G + 4, G + 9, G + 13])
    b = Build(seed=57); mill.build(b); out['mill'] = (b.c, (18, -113, 38, -100), [G + 4, G + 8, G + 12])
    b = Build(seed=57); shrine.build(b); out['shrine'] = (b.c, (17, 100, 38, 113), [G + 4, G + 8])
    out['toll'] = (toll.toll().c, None, [G + 4, G + 8])
    b = Build(seed=57); watergate.build(b)
    out['base-barracks-n'] = (b.c, (-249, -38, -226, -26), [G + 4])
    out['base-yard'] = (b.c, (-251, -40, -180, 39), [G + 4])
    return out


def shot(cells, box, cut, view):
    xs = [p[0] for p in cells]; zs = [p[2] for p in cells]
    x0, z0, x1, z1 = box or (min(xs), min(zs), max(xs), max(zs))
    w = World('in')
    for (x, y, z), v in cells.items():
        if x0 <= x <= x1 and z0 <= z <= z1 and G <= y <= cut and v != 'air': w.set((x, y, z), Cell(normalize(v)))
    return render.render(w, view=view, lo=(x0, G, z0), hi=(x1, cut, z1), grid=False, title='cut at y %d (floor %d)' % (cut, G))


if __name__ == '__main__':
    outdir = sys.argv[1]; os.makedirs(outdir, exist_ok=True)
    ps = pieces()
    for name in sys.argv[2:] or ps:
        cells, box, cuts = ps[name]
        ims = [shot(cells, box, c, v) for c in cuts for v in ('iso', 'top')]
        ims = [im.resize((int(im.width * 420 / im.height), 420)) if im.height > 420 else im for im in ims]
        W = sum(im.width for im in ims) + 8 * len(ims); H = max(im.height for im in ims)
        sheet = Image.new('RGB', (W, H), (24, 26, 32)); x = 0
        for im in ims: sheet.paste(im, (x, 0)); x += im.width + 8
        if sheet.width > 3000: sheet = sheet.resize((3000, int(sheet.height * 3000 / sheet.width)))
        p = os.path.join(outdir, name + '.png'); sheet.save(p); print(p, sheet.size)
