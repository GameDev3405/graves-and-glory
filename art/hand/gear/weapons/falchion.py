"""Notched Falchion (falchion): a short heavy single-edged blade that widens to a clipped point, the edge hacked into
three notched lengths, a short guard and a disc pommel. House-neutral steel, unlike the troops' violet notched sword."""
from weaponlib import *

def make(r, side):
    M, F = metal_of(r), fit_of(r)
    g = 3.4 if r == LEGEND else 2.6
    c = [Cube("pommel", [-1.4, -4.2, -0.5], [1.4, -2.45, 0.5], "steel", tone(M, 3)),
         Cube("grip", [-0.6, -2.5, -0.6], [0.6, 2.05, 0.6], "leather", wound(LEATH, 2, rub=1)),
         Cube("guard", [-g, 2, -0.8], [g, 3, 0.8], "steel", tone(F, 3)),
         Cube("blade_low", [-1, 2.95, -0.4], [1.2, 10.05, 0.4], "steel", blade(M, 4, dark_edge=True)),
         Cube("blade_up", [-1, 9.95, -0.4], [1.8, 15.05, 0.4], "steel", blade(M, 4, dark_edge=True)),
         Cube("point", [0.0, 15, -0.35], [1.8, 16.6, 0.35], "steel", blade(M, 4)),
         Cube("tip", [0.9, 16.55, -0.25], [1.8, 17.4, 0.25], "steel", tone(M, 5, lead=False))]
    for i, (a, b) in enumerate(((10.2, 11.6), (12.4, 13.7), (14.4, 15.1))):
        c.append(Cube("edge%d" % i, [1.75, a, -0.3], [2.35, b, 0.3], "steel", tone(M, 5, lead=False)))
    if r >= RARE:
        c += [Cube("quillon_l", [-g - 0.05, 1.0, -0.6], [-g + 0.85, 2.05, 0.6], "steel", tone(F, 2)),
              Cube("quillon_r", [g - 0.85, 1.0, -0.6], [g + 0.05, 2.05, 0.6], "steel", tone(F, 2))]
    c += bead("bead", [-0.5, 2.2, -0.9], [0.5, 2.8, 0.9], r)
    if r >= EPIC: c += bead("pommel_gem", [-0.5, -3.8, -0.65], [0.5, -2.8, 0.65], r)
    if r == LEGEND: c.append(Cube("spine", [-1.6, 11, -0.3], [-0.95, 14.2, 0.3], "steel", tone(M, 4, lead=False)))
    return c

def weapon(): return Weapon("falchion", make, "blade", note="single edge widening to a clipped point, three notched edge lengths")
