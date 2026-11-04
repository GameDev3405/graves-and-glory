"""Rivergate Arbalest (crossbow): a short iron stock under a heavy steel prod across z, stepped thick limbs to capped
ends, a straight string and a foot stirrup at the front; the lock plate on top (+x). Rare lays a bolt in the groove."""
from weaponlib import *
from last_watch import IRON

def make(r, side):
    M, F = metal_of(r), fit_of(r)
    S = IRON if r == COMMON else M
    z1, z2 = (4.5, 7) if r < LEGEND else (5, 8)
    c = [Cube("stock", [-0.8, -5, -0.7], [0.8, 10, 0.7], "iron", tone(S, 3)),
         Cube("butt", [-1.5, -7.5, -1], [1.2, -4.95, 1], "iron", tone(S, 3)),
         Cube("lock", [0.75, 0, -0.5], [1.4, 4.05, 0.5], "steel", tone(F, 3)),
         Cube("trigger", [-1.6, 1, -0.3], [-0.75, 2.5, 0.3], "iron", tone(S, 3, lead=False)),
         Cube("prod", [-1, 9, -1.5], [1.2, 11, 1.5], "steel", tone(M, 3))]
    for s, n in ((1, "l"), (-1, "r")):
        zz = lambda a, b: (min(s * a, s * b), max(s * a, s * b))
        a, b = zz(1.45, z1); c.append(Cube("limb1" + n, [-0.6, 9.4, a], [0.8, 10.6, b], "steel", tone(M, 4)))
        a, b = zz(z1 - 0.05, z2); c.append(Cube("limb2" + n, [-0.5, 9.0, a], [0.7, 10.2, b], "steel", tone(M, 4)))
        a, b = zz(z2 - 0.05, z2 + 0.6); c.append(Cube("cap" + n, [-0.7, 8.6, a], [0.9 if r < EPIC else 2.2, 9.9, b], "steel", tone(F, 2)))
    c.append(Cube("string", [0.1, 8.3, -z2 - 0.3], [0.7, 8.9, z2 + 0.3], "cloth", tone(LINEN, 2, lead=False)))
    c += [Cube("stir_f", [-0.5, 13.5, -1.6], [0.5, 14.2, 1.6], "iron", tone(S, 3, lead=False)),
          Cube("stir_l", [-0.5, 10.95, 1.0], [0.5, 14.2, 1.6], "iron", tone(S, 3, lead=False)),
          Cube("stir_r", [-0.5, 10.95, -1.6], [0.5, 14.2, -1.0], "iron", tone(S, 3, lead=False))]
    if r >= UNCOMMON: c.append(Cube("bolt", [0.75, 4, -0.3], [1.35, 12, 0.3], "wood", grain(OAK, 3)))
    if r >= RARE:
        c += [Cube("bolt_head", [0.6, 11.95, -0.5], [1.5, 13.2, 0.5], "steel", tone(M, 4, lead=False)),
              Cube("brace", [-0.95, -3.2, -0.85], [0.95, -2.2, 0.85], "iron", tone(F, 3, lead=False))]
    c += bead("bead", [-0.4, 9.6, -1.7], [0.6, 10.4, 1.7], r) if r < EPIC else bead("lock_gem", [1.3, 1.5, -0.4], [1.7, 2.5, 0.4], r)
    return c

def weapon(): return Weapon("crossbow", make, "blade", note="short iron stock, thick steel limbs, stirrup at the front")
