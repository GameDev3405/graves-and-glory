"""Frostmarch workshop: the key pieces on a snow pad, exported as 64 x 64 templates (ggmaps:fm/ws_<i><j>) with the
in-game script that places and photographs them. Nothing here goes into the map; the map builder stamps the same
piece functions.
  python fmworks.py OUTDIR SHOTS.txt"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
from mapforge.world import World, Cell
from mapforge.structure import normalize
import spine, tower, refuge

G, Y = 80, 195                                              # pad ground in piece coordinates, and where it sits in the test world
X0, Z0, NX, NZ = -32, -128, 2, 4
TOWER, REFUGE = (60, -44), (60, 24)


def pad():
    c = {}
    for x in range(X0, X0 + 64 * NX):
        for z in range(Z0, Z0 + 64 * NZ):
            c[(x, G, z)] = 'gravel' if abs(z) <= 4 and x < 30 else 'snow_block'
            c[(x, G - 1, z)] = 'stone'
    c.update({(x, y, z): v for (x, y, z), v in spine.spine(G).items()})
    for (ox, oz), fn in ((TOWER, tower.tower), (REFUGE, refuge.refuge)):
        c.update({(x + ox, y, z + oz): v for (x, y, z), v in fn(G).items()})
    return c


if __name__ == '__main__':
    out, shots = sys.argv[1], sys.argv[2]; os.makedirs(out, exist_ok=True)
    c = pad(); w = World('fm-workshop')
    for p, v in c.items():
        if v != 'air': w.set(p, Cell(normalize(v)))
    dy = Y - G
    L = ['# Frostmarch workshop: the Spine, the Signal Tower, the Refuge', 'note Frostmarch workshop', 'mc time set 3000']
    for i in range(NX):
        for j in range(NZ):
            lo = (X0 + 64 * i, G - 1, Z0 + 64 * j); hi = (lo[0] + 63, G + 62, lo[2] + 63)
            w.export_structure(lo, hi).save(os.path.join(out, 'ws_%d%d.nbt' % (i, j)))
            L += ['mc execute as @a at @s run forceload add %d %d %d %d' % (lo[0], lo[2], hi[0], hi[2]),
                  'mc execute as @a at @s run place template ggmaps:fm/ws_%d%d %d %d %d' % (i, j, lo[0], Y - 1, lo[2]), 'wait 10']
    def shot(n, cam, look): L.append('shot %s %d %d %d %d %d %d' % (n, cam[0], cam[1] + dy, cam[2], look[0], look[1] + dy, look[2]))
    shot('spine-west', (-44, G + 9, 70), (0, G + 8, 30))
    shot('spine-cut', (-34, G + 3, -3), (2, G + 7, 4))
    shot('spine-gap', (-18, G + 3, 44), (0, G + 4, 48))
    shot('spine-eye', (-16, G + 2, 24), (0, G + 9, 40))
    shot('spine-air', (-50, G + 40, 0), (0, G + 4, 0))
    L.append('shot spine-top 0 %d 52 0 90' % (G + 70 + dy)); L.append('shot spine-top-n 0 %d -52 0 90' % (G + 70 + dy)); L.append('shot spine-top-close 0 %d 40 0 90' % (G + 42 + dy))
    shot('spine-side', (-40, G + 14, 52), (0, G + 10, 52)); shot('spine-end', (0, G + 8, 8), (0, G + 9, 30))
    tx, tz = TOWER; rx, rz = REFUGE
    shot('tower-se', (tx + 20, G + 12, tz + 26), (tx, G + 20, tz))
    shot('tower-door', (tx + 3, G + 3, tz + 18), (tx, G + 8, tz))
    shot('tower-top', (tx + 16, G + 44, tz + 16), (tx, G + 36, tz))
    shot('tower-in', (tx, G + 2, tz + 4), (tx - 3, G + 2, tz - 5))
    shot('refuge-se', (rx + 18, G + 10, rz + 22), (rx, G + 9, rz))
    shot('refuge-door', (rx - 4, G + 3, rz + 17), (rx, G + 6, rz))
    shot('refuge-nw', (rx - 20, G + 12, rz - 20), (rx, G + 9, rz))
    shot('refuge-in', (rx, G + 2, rz + 4), (rx - 3, G + 2, rz - 5))
    shot('both', (tx + 44, G + 18, (tz + rz) // 2), (tx, G + 14, (tz + rz) // 2))
    open(shots, 'w', newline='\n').write('\n'.join(L) + '\n'); print(len(L), 'lines', len(c), 'cells')
