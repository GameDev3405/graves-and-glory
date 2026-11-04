"""Hollowfell Greatsword (greatsword): a long, heavy blackened blade, notched along one edge where the bright edge
breaks in five lengths; a long two-hand grip and a broad cross. Two hands, the second fist 4.5 px below the first."""
from weaponlib import *
from last_watch import BLACK

def make(r, side):
    F = BLACK if r == COMMON else fit_of(r)
    E = metal_of(r)
    c = [Cube("pommel", [-1.3, -9.5, -1.3], [1.3, -6.95, 1.3], "iron", tone(BLACK if r == COMMON else metal_of(r), 3)),
         Cube("grip", [-0.7, -7.05, -0.7], [0.7, 2.05, 0.7], "leather", wound(LEATH, 1, rub=1)),
         Cube("guard", [-4.5, 2, -1], [4.5, 3.5, 1], "iron", tone(F, 3)),
         Cube("blade", [-1.5, 3.45, -0.5], [1, 24.05, 0.5], "iron", blade(BLACK, 3, fuller=True)),
         Cube("point", [-1.1, 24, -0.4], [1.1, 25.6, 0.4], "iron", blade(BLACK, 3)),
         Cube("tip", [-0.5, 25.55, -0.3], [0.5, 27, 0.3], "steel", tone(E, 4, lead=False))]
    for i, a in enumerate((4, 8.3, 12.3, 16.3, 20.3)):
        c.append(Cube("edge%d" % i, [0.95, a, -0.35], [1.6, a + 3.2, 0.35], "steel", tone(E, 4, lead=False)))
    if r >= RARE:
        c += [Cube("end_l", [-5.1, 1.7, -1.2], [-4.45, 3.8, 1.2], "iron", tone(F, 2)),
              Cube("end_r", [4.45, 1.7, -1.2], [5.1, 3.8, 1.2], "iron", tone(F, 2))]
    c += bead("bead", [-0.5, 2.3, -1.2], [0.5, 3.2, 1.2], r)
    if r >= EPIC: c += bead("pommel_gem", [-0.5, -8.8, -1.45], [0.5, -7.8, 1.45], r)
    if r == LEGEND: c.append(Cube("inlay", [-0.75, 6, -0.6], [0.05, 20, 0.6], "gem", tone(SUN, 2, lead=False), lit=W.FACES))
    return c

def weapon(): return Weapon("greatsword", make, "blade", grips=(0, -4.5), note="long black blade notched along one edge")
