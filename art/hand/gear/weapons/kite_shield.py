"""Last Watch Tower Shield (kite_shield): a tall iron slab with a dented proud rim, a boss and a strap behind; worn
before the left forearm with the north face out. Iron at Common, blackened from Uncommon; Legendary gilds the cross of spine and bar, lights
a sun boss and raises three crest spikes."""
from weaponlib import *
from last_watch import IRON, BLACK

def dented(R, base):
    def paint(face, w, h):
        g = tone(R, base)(face, w, h)
        for y in range(h):
            for x in range(w):
                if (x * 7 + y * 3) % 17 == 0: g[y][x] = clamp(R, base + LITW[face] - 1)
                elif (x * 5 + y * 9) % 29 == 0: g[y][x] = clamp(R, base + LITW[face] + 1)
        return g
    return paint

def make(r, side):
    S = IRON if r == COMMON else BLACK
    F = IRON if r == COMMON else metal_of(r)
    G = fit_of(r) if r > COMMON else IRON
    c = [Cube("slab", [-4.5, -8, -1], [4.5, 8, 0], "iron", dented(S, 3)),
         Cube("rim_t", [-4.8, 7.2, -1.3], [4.8, 8.3, 0.1], "iron", dented(F, 3)),
         Cube("rim_b", [-4.8, -8.3, -1.3], [4.8, -7.2, 0.1], "iron", dented(F, 3)),
         Cube("rim_l", [-4.8, -7.25, -1.3], [-3.8, 7.25, 0.1], "iron", dented(F, 3)),
         Cube("rim_r", [3.8, -7.25, -1.3], [4.8, 7.25, 0.1], "iron", dented(F, 3)),
         Cube("boss", [-1.5, 0, -1.9], [1.5, 3, -0.95], "iron", W.ring(G[1], G[3], G[5]) if r < LEGEND else stone(SUN), lit=W.FACES if r == LEGEND else ()),
         Cube("strap", [-2.5, -1, -0.05], [2.5, 1, 0.6], "leather", wound(LEATH, 2))]
    if r >= UNCOMMON: c.append(Cube("plate", [-2.2, -0.7, -1.2], [2.2, 3.7, -0.95], "iron", tone(F, 2, lead=False)))
    if r >= RARE:
        c += [Cube("spine", [-0.5, -7.25, -1.25], [0.5, 7.25, -0.95], "iron", tone(G, 2, lead=False)),
              Cube("bar", [-3.85, 1.0, -1.2], [3.85, 2.0, -0.95], "iron", tone(G, 2, lead=False))]
    if r >= EPIC:
        c += [Cube("stud_l", [-3.6, 0.9, -1.6], [-2.6, 2.1, -1.15], "iron", tone(G, 4, lead=False)),
              Cube("stud_r", [2.6, 0.9, -1.6], [3.6, 2.1, -1.15], "iron", tone(G, 4, lead=False))]
    c += bead("bead", [-0.5, 3.0, -1.6], [0.5, 3.9, -1.2], r)
    if r == LEGEND:
        c += [Cube("crest_c", [-0.6, 8.25, -1.0], [0.6, 11, -0.2], "gilt", tone(GOLD, 4, lead=False)),
              Cube("crest_l", [-3.4, 8.25, -1.0], [-2.4, 9.8, -0.2], "gilt", tone(GOLD, 4, lead=False)),
              Cube("crest_r", [2.4, 8.25, -1.0], [3.4, 9.8, -0.2], "gilt", tone(GOLD, 4, lead=False))]
    return c

def weapon(): return Weapon("kite_shield", make, "shield", note="tall iron slab, dented proud rim, boss")
