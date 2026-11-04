"""Candlewake Staff (mending_staff): a gnarled 2 px staff with three knots, forking into two tines that hold a small
lantern. The glass is dull at Common, glows from Uncommon, and at Legendary burns warm under a gold crown."""
from weaponlib import *
from ashen_vigil import FLAME, WARM

def make(r, side):
    F = fit_of(r)
    G = WARM if r == LEGEND else FLAME
    c = [Cube("shaft", [-1, -14, -1], [1, 13, 1], "wood", grain(OAK, 3, knot=9)),
         Cube("knot0", [-1.3, 5, -1.25], [0.6, 6.2, 1.25], "wood", grain(OAK, 2, salt=3)),
         Cube("knot1", [-0.5, -8, -1.25], [1.3, -6.8, 1.25], "wood", grain(OAK, 2, salt=5)),
         Cube("knot2", [-1.3, 9, -0.6], [1.25, 10, 1.3], "wood", grain(OAK, 2, salt=1)),
         Cube("ferrule", [-1.2, -15.5, -1.2], [1.2, -13.95, 1.2], "iron", tone(metal_of(r), 2)),
         Cube("tine_l", [-2, 12.95, -0.6], [-0.95, 17.6 if r < EPIC else 19, 0.6], "wood", grain(OAK, 3, salt=2)),
         Cube("tine_r", [0.95, 12.95, -0.6], [2, 17.6 if r < EPIC else 19, 0.6], "wood", grain(OAK, 3, salt=6)),
         Cube("collar", [-1.2, 11.5, -1.2], [1.2, 12.5, 1.2], "iron", tone(metal_of(r), 3)),
         Cube("base", [-1.5, 14, -1.5], [1.5, 15, 1.5], "iron", tone(metal_of(r), 2)),
         Cube("glass", [-1.1, 14.95, -1.1], [1.1, 17.55, 1.1], "gem", tone(G, 2 if r > COMMON else 1, lead=False), lit=W.FACES if r > COMMON else ()),
         Cube("cap", [-1.5, 17.5, -1.5], [1.5, 18.3, 1.5], "iron", tone(F, 3)),
         Cube("ring", [-0.4, 18.25, -0.3], [0.4, 19.5, 0.3], "iron", tone(F, 3, lead=False))]
    if r >= UNCOMMON: c.append(Cube("cage_a", [-0.25, 15, -1.25], [0.25, 17.55, 1.25], "iron", tone(F, 1, lead=False)))
    if r >= RARE:
        c += [Cube("band", [-1.2, -6, -1.2], [1.2, -5.2, 1.2], "iron", tone(metal_of(r), 3, lead=False)),
              Cube("cage_b", [-1.25, 15, -0.25], [1.25, 17.55, 0.25], "iron", tone(F, 1, lead=False))]
    c += bead("bead", [-0.5, 11.7, -1.4], [0.5, 12.3, 1.4], r)
    if r >= EPIC: c += bead("finial", [-0.5, 19.45, -0.5], [0.5, 20.5, 0.5], r)
    if r == LEGEND: c.append(Cube("crown", [-1.75, 18.25, -1.75], [1.75, 19.0, 1.75], "gilt", tone(GOLD, 3, lead=False)))
    return c

def weapon(): return Weapon("mending_staff", make, "staff", note="gnarled staff, two tines, a small lantern head")
