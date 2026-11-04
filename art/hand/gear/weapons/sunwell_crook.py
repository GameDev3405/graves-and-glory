"""Sunwell Crook (sunwell_crook): a pale ash shepherd's crook, the hook curling back over the head, a gold ring under
it at every rarity. Rare adds a second ring and a knob on the hook's end, Epic a stone there; Legendary hangs a lit sun
disc inside the curl."""
from weaponlib import *

def make(r, side):
    F = fit_of(r)
    c = [Cube("shaft", [-0.9, -15, -0.9], [0.9, 14.05, 0.9], "wood", grain(ASHW, 3)),
         Cube("ring", [-1.15, 12, -1.15], [1.15, 13, 1.15], "gilt", tone(GOLD, 4, lead=False)),
         Cube("ferrule", [-1.1, -16, -1.1], [1.1, -14.95, 1.1], "iron", tone(F, 2))]
    c += chain("hook", (0, 14), (2.4, 2.4, 2.4, 2.4, 2.0), (25, 65, 110, 150, 180), (1.6, 1.6, 1.5, 1.4, 1.3), 1.6, "wood", grain(ASHW, 3, salt=3))
    ex, ey = chain.end
    if r >= UNCOMMON: c.append(Cube("ring2", [-1.1, -6, -1.1], [1.1, -5.2, 1.1], "iron", tone(F, 3, lead=False)))
    k = 1.25 if r == LEGEND else 0.9
    if r >= RARE:
        c += [Cube("knob", [ex - k, ey - k, -0.95], [ex + k, ey + 0.6, 0.95], "wood", tone(F if r == LEGEND else ASHW, 3))]
    c += bead("bead", [-0.4, 12.2, -1.3], [0.4, 12.8, 1.3], r)
    if r >= EPIC: c.append(Cube("ring3", [-1.05, 10.6, -1.05], [1.05, 11.4, 1.05], "gilt", tone(GOLD, 3, lead=False)))
    if r >= EPIC: c += bead("hook_gem", [ex - 0.5, ey - 1.6, -0.5], [ex + 0.5, ey - 0.85, 0.5], r)
    if r == LEGEND: c.append(Cube("sun", [1.5, 14.6, -0.3], [3.6, 16.8, 0.3], "gem", stone(SUN), lit=W.FACES))
    return c

def weapon(): return Weapon("sunwell_crook", make, "pole", note="pale ash crook, a gold ring under the hook")
