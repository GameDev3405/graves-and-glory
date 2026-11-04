"""Barrowleaf Quiver (quiver): a hide tube at the right hip, two strap bands, a rim and a bottom cap, three arrows
showing with orange-brown feather fletching. Rare buckles the straps; Legendary gilds the rim and adds a fourth arrow."""
from weaponlib import *
from frostmarch_hunt import HIDE, FEATHER

ARROWS = ((-0.6, 0.0, 10.2), (0.6, -0.5, 9.4), (0.0, 0.7, 9.8), (-0.4, -0.8, 10.6))

def make(r, side):
    F = fit_of(r)
    c = [Cube("tube", [-1.5, -6, -1.5], [1.5, 6, 1.5], "hide", grain(HIDE, 3)),
         Cube("rim", [-1.8, 5, -1.8], [1.8, 6.2, 1.8], "gilt" if r == LEGEND else "hide", tone(HIDE if r < RARE else F, 2, lead=False)),
         Cube("cap", [-1.7, -6.8, -1.7], [1.7, -5.95, 1.7], "hide", tone(HIDE, 1, lead=False)),
         Cube("strap0", [-1.65, 2, -1.65], [1.65, 3, 1.65], "leather", tone(LEATH, 2, lead=False)),
         Cube("strap1", [-1.65, -3, -1.65], [1.65, -2, 1.65], "leather", tone(LEATH, 2, lead=False))]
    for i, (x, z, top) in enumerate(ARROWS[:4 if r == LEGEND else 3]):
        c += [Cube("shaft%d" % i, [x - 0.3, 6.1, z - 0.3], [x + 0.3, top - 1.9, z + 0.3], "wood", tone(ASHW, 2, lead=False)),
              Cube("fletch%d" % i, [x - 0.6, top - 2, z - 0.6], [x + 0.6, top, z + 0.6], "cloth", grain(FEATHER, 2, salt=i))]
    if r >= RARE:
        c += [Cube("buckle0", [-0.6, 1.8, -1.95], [0.6, 3.2, -1.6], "iron", tone(F, 3, lead=False)),
              Cube("buckle1", [-0.6, -3.2, -1.95], [0.6, -1.8, -1.6], "iron", tone(F, 3, lead=False))]
    c += bead("bead", [-0.5, 5.2, -2.05], [0.5, 6.0, -1.75], r)
    return c

def weapon(): return Weapon("quiver", make, "hip", note="hide tube at the hip, feather fletching")
