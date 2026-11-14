"""Hollowwood workshop 2: the Great Stump and the Drowned Stump side by side (ggmaps:hw/ws2_<i><j>), with the in-game script.
  python hwworks2.py OUTDIR SHOTS.txt"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(1, os.path.normpath(os.path.join(HERE, '..', '..', '..', '..', 'tools')))
from mapforge.world import World, Cell
from mapforge.structure import normalize
import stump
from yoke import h3

G, Y = 72, 195
X0, Z0, NX, NZ = -128, -64, 4, 2
A, C = (-64, 0), (64, 0)

if __name__ == '__main__':
    out, shots = sys.argv[1], sys.argv[2]; os.makedirs(out, exist_ok=True); c = {}
    for x in range(X0, X0 + 64 * NX):
        for z in range(Z0, Z0 + 64 * NZ):
            n = h3(x, 3, z); c[(x, G - 1, z)] = 'dirt'
            c[(x, G, z)] = ('podzol' if n < 0.5 else 'moss_block' if n < 0.8 else 'coarse_dirt') if x < 0 else ('mud' if n < 0.3 else 'moss_block' if n < 0.75 else 'packed_mud')
    for (ox, oz), rot in ((A, False), (C, True)): c.update({(x + ox, y, z + oz): v for (x, y, z), v in stump.stump(G, rot).items()})
    w = World('hw-workshop2')
    for p, v in c.items(): w.set(p, Cell(normalize(v)))
    dy = Y - G; L = ['# Hollowwood workshop 2: the two stumps', 'note Hollowwood workshop 2']
    for i in range(NX):
        for j in range(NZ):
            lo = (X0 + 64 * i, G - 1, Z0 + 64 * j); hi = (lo[0] + 63, G + 62, lo[2] + 63)
            w.export_structure(lo, hi).save(os.path.join(out, 'ws2_%d%d.nbt' % (i, j)))
            L += ['mc execute as @a at @s run forceload add %d %d %d %d' % (lo[0], lo[2], hi[0], hi[2]), 'mc execute as @a at @s run place template ggmaps:hw/ws2_%d%d %d %d %d' % (i, j, lo[0], Y - 1, lo[2]), 'wait 10']
    def shot(n, cam, look): L.append('shot %s %d %d %d %d %d %d' % (n, cam[0], cam[1] + dy, cam[2], look[0], look[1] + dy, look[2]))
    for tag, t in (('day', 6000), ('dusk', 12600)):
        L += ['mc time set %d' % t, 'wait 20']
        for n, (ox, oz) in (('a', A), ('c', C)):
            shot('%s-air-%s' % (n, tag), (ox + 44, G + 40, oz + 52), (ox, G + 6, oz))
            shot('%s-gate-%s' % (n, tag), (ox + 58, G + 4, oz - 2), (ox + 30, G + 8, oz - 3))
            shot('%s-in-%s' % (n, tag), (ox + 22, G + 9, oz + 6), (ox - 26, G + 9, oz - 4))
            shot('%s-ground-%s' % (n, tag), (ox - 20, G + 3, oz + 54), (ox, G + 10, oz))
    open(shots, 'w', newline='\n').write('\n'.join(L) + '\n'); print(len(L), 'lines', len(c), 'cells')
