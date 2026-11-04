"""Ironhold Cleaver (great_axe): a broad bearded iron head on an oak haft bound in brass, a poll behind and a short top
spike; two hands, the second fist 5 px down the haft. Legendary grows a horn and a beard on the edge."""
from weaponlib import *

def make(r, side):
    M, F = metal_of(r), fit_of(r)
    c = [Cube("haft", [-0.9, -13, -0.9], [0.9, 15, 0.9], "wood", grain(OAK, 3, knot=20)),
         Cube("band0", [-1.1, 2.5, -1.1], [1.1, 3.5, 1.1], "brass", tone(BRASS, 3, lead=False)),
         Cube("band1", [-1.1, -8.5, -1.1], [1.1, -7.5, 1.1], "brass", tone(BRASS, 3, lead=False)),
         Cube("band2", [-1.1, 8, -1.1], [1.1, 9, 1.1], "brass", tone(BRASS, 3, lead=False)),
         Cube("butt", [-1.2, -14.5, -1.2], [1.2, -12.95, 1.2], "iron", tone(F, 2)),
         Cube("spike", [-0.5, -16, -0.5], [0.5, -14.45, 0.5], "iron", tone(F, 3, lead=False)),
         Cube("socket", [-1.6, 9.5, -1.3], [1.6, 15.5, 1.3], "iron", tone(M, 2)),
         Cube("cheek", [1.55, 10, -0.6], [5, 15, 0.6], "iron", tone(M, 3)),
         Cube("head", [4.95, 8, -0.45], [7.5, 17, 0.45], "steel", blade(M, 3)),
         Cube("edge", [7.45, 8.5, -0.3], [8.1, 16.5, 0.3], "steel", tone(M, 5, lead=False)),
         Cube("poll", [-3.5, 11, -0.8], [-1.55, 14, 0.8], "iron", tone(M, 2)),
         Cube("top", [-0.5, 15.45, -0.5], [0.5, 17.5, 0.5], "iron", tone(M, 4, lead=False))]
    if r >= RARE: c.append(Cube("langets", [-1.05, 6.5, -1.05], [1.05, 9.55, 1.05], "iron", tone(F, 2)))
    c += bead("bead", [-0.5, 12, -1.5], [0.5, 13, 1.5], r)
    if r >= EPIC: c += bead("butt_gem", [-0.5, -14, -1.35], [0.5, -13.2, 1.35], r)
    if r == LEGEND:
        c += [Cube("horn", [7.45, 16.45, -0.3], [8.6, 18.6, 0.3], "gilt", tone(GOLD, 4, lead=False)),
              Cube("beard", [7.45, 5.9, -0.3], [8.6, 8.55, 0.3], "gilt", tone(GOLD, 4, lead=False))]
    return c

def weapon(): return Weapon("great_axe", make, "blade", grips=(0, -5), note="broad bearded head, brass-banded oak haft")
