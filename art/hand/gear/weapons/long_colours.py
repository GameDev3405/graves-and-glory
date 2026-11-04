"""The Long Colours (long_colours): an old company's colours on a tall pole, the flag long and tattered with a charred
lower edge, an iron butt spike and an iron cross for a finial. Held, never on the back. Legendary gilds the cross."""
from weaponlib import *
from last_watch import IRON
from ashen_vigil import SOOT

RAG = (3, 1, 4, 2, 0, 3)

def tatter(x, y, w, h): return y >= h - RAG[x % 6] or (x == w - 1 and y % 4 == 3)

def char(x, y, w, h):
    g = h - RAG[x % 6]
    return SOOT[1] if y >= g - 1 else SOOT[3] if y >= g - 2 and x % 2 else None

def make(r, side):
    F = IRON if r == COMMON else fit_of(r)
    X = GOLD if r == LEGEND else IRON
    c = [Cube("pole", [-0.8, -16, -0.8], [0.8, 26, 0.8], "wood", grain(OAK, 3)),
         Cube("spike", [-0.6, -19, -0.6], [0.6, -15.95, 0.6], "iron", tone(IRON, 3)),
         Cube("spike_tip", [-0.25, -20.5, -0.25], [0.25, -18.95, 0.25], "iron", tone(IRON, 4, lead=False)),
         Cube("crossbar", [0.75, 23.5, -0.5], [7.5, 24.5, 0.5], "wood", grain(OAK, 3, salt=4)),
         Cube("flag", [0.85, 7, -0.3], [7, 23.55, 0.3], "cloth", cloth(side, 1, cut=tatter, device=char)),
         Cube("cross", [-0.4, 25.95, -0.4], [0.4, 30, 0.4], "iron", tone(X, 3, lead=False)),
         Cube("cross_bar", [-1.8 if r < EPIC else -2.6, 27.8, -0.45], [1.8 if r < EPIC else 2.6, 28.6, 0.45], "iron", tone(X, 3, lead=False))]
    if r >= UNCOMMON: c.append(Cube("collar", [-1, 22.4, -1], [1, 23.45, 1], "iron", tone(F, 3)))
    if r >= RARE: c += [Cube("collar_lo", [-1, -15.2, -1], [1, -14.2, 1], "iron", tone(F, 3)),
                        Cube("bar_cap", [7.45, 23.3, -0.7], [8.2, 24.7, 0.7], "iron", tone(F, 3, lead=False))]
    c += bead("bead", [-0.4, 22.6, -1.2], [0.4, 23.3, 1.2], r)
    if r >= EPIC: c += bead("cross_gem", [-0.5, 27.7, -0.55], [0.5, 28.7, 0.55], r)
    if r == LEGEND: c.append(Cube("fringe", [0.85, 23.2, -0.35], [7, 23.6, 0.35], "gilt", tone(GOLD, 4, lead=False)))
    return c

def weapon(): return Weapon("long_colours", make, "staff", show_side=True, note="tall pole, long tattered charred flag, iron cross")
