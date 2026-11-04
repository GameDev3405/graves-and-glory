"""Oathkeeper's Longsword (arming_sword): the knight's straight double-edged sword, a long cross guard and a wheel
pommel; 24.5 px from pommel to point. Common is a nicked worn blade on a plain iron cross; Legendary grows the guard and
gilds the pommel and the guard ends round a lit sun stone."""
from weaponlib import *

def make(r, side):
    M, F = metal_of(r), fit_of(r)
    g = 5 if r == LEGEND else 4
    nicks = (3, 8) if r == COMMON else ()
    c = [Cube("pommel", [-1.2, -4.5, -1.2], [1.2, -2.45, 1.2], "steel", tone(F, 3)),
         Cube("grip", [-0.6, -2.5, -0.6], [0.6, 2.05, 0.6], "leather", wound(LEATH, 2, rub=1)),
         Cube("guard", [-g, 2, -0.9], [g, 3, 0.9], "steel", tone(M, 3)),
         Cube("seat", [-1.3, 2.95, -0.7], [1.3, 4.55, 0.7], "steel", tone(M, 2)),
         Cube("blade", [-1, 4.5, -0.45], [1, 17.55, 0.45], "steel", blade(M, 4, fuller=r >= RARE, nicks=nicks)),
         Cube("point", [-0.6, 17.5, -0.35], [0.6, 19.05, 0.35], "steel", blade(M, 4)),
         Cube("tip", [-0.2, 19, -0.2], [0.2, 20, 0.2], "steel", tone(M, 5, lead=False))]
    if r >= RARE:
        c += [Cube("end_l", [-g - 0.6, 1.8, -1.1], [-g + 0.05, 3.2, 1.1], "steel", tone(F, 2)),
              Cube("end_r", [g - 0.05, 1.8, -1.1], [g + 0.6, 3.2, 1.1], "steel", tone(F, 2))]
    c += bead("bead", [-0.5, 3.2, -0.8], [0.5, 4.2, 0.8], r)
    if r >= EPIC: c += bead("pommel_gem", [-0.5, -4.0, -1.3], [0.5, -3.0, 1.3], r)
    return c

def weapon(): return Weapon("arming_sword", make, "blade", note="straight double edge, long cross, wheel pommel")
