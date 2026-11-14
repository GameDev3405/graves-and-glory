"""Hollowwood workshop: key pieces on a mock Hollow floor, exported as 64 x 64 templates (ggmaps:hw/ws_<i><j>) with the
in-game script that places and photographs them. Nothing here goes into the map; the map builder stamps the same piece functions.
  python hwworks.py OUTDIR SHOTS.txt"""
import sys, os, math
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
from mapforge.world import World, Cell
from mapforge.structure import normalize
import yoke

G, Y = 62, 195
X0, Z0, NX, NZ = -64, -64, 2, 2


def pad():
    c = {}
    for x in range(X0, X0 + 64 * NX):
        for z in range(Z0, Z0 + 64 * NZ):
            r = math.hypot(x, z); n = yoke.h3(x, 3, z)
            if r <= 52.5: c[(x, G, z)] = 'grass_block' if n < 0.55 else 'moss_block' if n < 0.9 else 'podzol'; c[(x, G - 1, z)] = 'dirt'
            else:
                for y in range(G - 1, G + 11): c[(x, y, z)] = yoke.pick([('rooted_dirt', 4), ('coarse_dirt', 3), ('dark_oak_wood[axis=y]', 2), ('mossy_cobblestone', 2), ('moss_block', 2)], x // 2, y, z // 2)
                c[(x, G + 10, z)] = 'moss_block' if n < 0.6 else 'podzol'
    c.update(yoke.yoke(G))
    return c


if __name__ == '__main__':
    out, shots = sys.argv[1], sys.argv[2]; os.makedirs(out, exist_ok=True)
    c = pad(); w = World('hw-workshop')
    for p, v in c.items():
        if v != 'air': w.set(p, Cell(normalize(v)))
    dy = Y - G
    L = ['# Hollowwood workshop: the Yoke and shrine', 'note Hollowwood workshop']
    for i in range(NX):
        for j in range(NZ):
            lo = (X0 + 64 * i, G - 1, Z0 + 64 * j); hi = (lo[0] + 63, G + 62, lo[2] + 63)
            w.export_structure(lo, hi).save(os.path.join(out, 'ws_%d%d.nbt' % (i, j)))
            L += ['mc execute as @a at @s run forceload add %d %d %d %d' % (lo[0], lo[2], hi[0], hi[2]),
                  'mc execute as @a at @s run place template ggmaps:hw/ws_%d%d %d %d %d' % (i, j, lo[0], Y - 1, lo[2]), 'wait 10']
    def shot(n, cam, look): L.append('shot %s %d %d %d %d %d %d' % (n, cam[0], cam[1] + dy, cam[2], look[0], look[1] + dy, look[2]))
    for tag, t in (('day', 6000), ('dusk', 12800)):
        L += ['mc time set %d' % t, 'wait 20']
        shot('yoke-se-' + tag, (40, G + 16, 44), (0, G + 10, 0))
        shot('yoke-eye-' + tag, (6, G + 2, 44), (0, G + 13, 0))
        shot('yoke-foot-' + tag, (-16, G + 3, 22), (-32, G + 5, 0))
        shot('yoke-under-' + tag, (-14, G + 2, -14), (8, G + 18, 6))
        shot('yoke-rim-' + tag, (-46, G + 22, -40), (0, G + 6, 0))
    L.append('shot yoke-top 0 %d 0 0 90' % (G + 60 + dy))
    open(shots, 'w', newline='\n').write('\n'.join(L) + '\n'); print(len(L), 'lines', len(c), 'cells')
